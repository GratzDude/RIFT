package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.CloudCustomGameRoom
import com.example.data.CloudSyncHealth
import com.example.data.CloudSyndicateMeta
import com.example.data.FriendPilotEntry
import com.example.data.LeaderboardEntry
import com.example.data.LivePilotState
import com.example.data.LocalPilotPrefs
import com.example.data.PilotDossier
import com.example.data.RiftRepository
import com.example.data.SectorBugReport
import com.example.data.SectorCombatEvent
import com.example.data.SectorInstanceState
import com.example.data.SyndicateChatMessage
import com.example.data.SyndicateInfo
import com.example.data.UgcModeration
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray
import org.json.JSONObject

sealed interface UiState<out T> {
  data object Loading : UiState<Nothing>
  data class Success<T>(val data: T) : UiState<T>
  data class Error(val message: String) : UiState<Nothing>
}

private fun <T> Flow<T>.retryWithExponentialBackoff(tagLabel: String): Flow<T> =
  this.retryWhen { cause, attempt ->
    if (cause is CancellationException) {
      false
    } else {
      val backoffMs = minOf(8_000L, 900L * (1L shl attempt.coerceAtMost(3L).toInt()))
      Log.w("RiftViewModel", "Retrying $tagLabel stream in ${backoffMs}ms after transient error: ${cause.message}")
      delay(backoffMs)
      true
    }
  }

@OptIn(ExperimentalCoroutinesApi::class)
class RiftViewModel(
  private val repository: RiftRepository,
  val currentUserId: String,
  val initialCallsign: String,
  val isGoogleAuthenticated: Boolean = Firebase.auth.currentUser != null,
) : ViewModel() {

  private val _activeSectorId = MutableStateFlow("NEXUS_LOBBY")
  val activeSectorId: StateFlow<String> = _activeSectorId.asStateFlow()

  fun setActiveSector(sectorId: String) {
    val cleanSector = repository.sanitizeSectorMode(sectorId.ifBlank { "NEXUS_LOBBY" })
    if (_activeSectorId.value != cleanSector) {
      _activeSectorId.value = cleanSector
    }
  }

  val cloudSyncHealth: StateFlow<CloudSyncHealth> = repository.cloudSyncHealth

  private val cloudReadyFlow: StateFlow<Boolean> =
    callbackFlow {
        val auth = Firebase.auth
        fun checkReady(): Boolean {
          val uid = auth.currentUser?.uid?.trim()
          return isGoogleAuthenticated && !uid.isNullOrEmpty() && !uid.startsWith("dev_")
        }
        trySend(checkReady())
        val listener = FirebaseAuth.AuthStateListener { trySend(checkReady()) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
      }
      .distinctUntilChanged()
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = isGoogleAuthenticated && Firebase.auth.currentUser != null,
      )

  private fun isCloudSessionReady(): Boolean {
    val uid = Firebase.auth.currentUser?.uid?.trim()
    return isGoogleAuthenticated && !uid.isNullOrEmpty() && !uid.startsWith("dev_")
  }

  val dossierState: StateFlow<UiState<PilotDossier?>> =
    cloudReadyFlow
      .flatMapLatest { ready ->
        if (!ready) {
          flowOf(UiState.Success(null))
        } else {
          val resolvedUid = Firebase.auth.currentUser?.uid?.trim()?.ifEmpty { currentUserId } ?: currentUserId
          repository
            .observePilotDossier(resolvedUid)
            .map<PilotDossier?, UiState<PilotDossier?>> { UiState.Success(it) }
            .retryWithExponentialBackoff("pilotDossier")
            .catch { error ->
              if (error is CancellationException) throw error
              emit(UiState.Error(error.message ?: "Failed to load pilot dossier"))
            }
        }
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = if (isCloudSessionReady()) UiState.Loading else UiState.Success(null),
      )

  val cloudProgressionState: StateFlow<String?> =
    cloudReadyFlow
      .flatMapLatest { ready ->
        if (!ready) {
          flowOf(null)
        } else {
          val resolvedUid = Firebase.auth.currentUser?.uid?.trim()?.ifEmpty { currentUserId } ?: currentUserId
          repository
            .observeCloudPilotProgression(resolvedUid)
            .distinctUntilChanged()
            .retryWithExponentialBackoff("pilotProgression")
            .catch { error ->
              if (error is CancellationException) throw error
              emit(null)
            }
        }
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = null,
      )

  val leaderboardState: StateFlow<UiState<List<LeaderboardEntry>>> =
    cloudReadyFlow
      .flatMapLatest { ready ->
        if (!ready) {
          flowOf(UiState.Success(emptyList()))
        } else {
          repository
            .observeLeaderboard()
            .map<List<LeaderboardEntry>, UiState<List<LeaderboardEntry>>> { UiState.Success(it) }
            .retryWithExponentialBackoff("leaderboard")
            .catch { error ->
              if (error is CancellationException) throw error
              emit(UiState.Error(error.message ?: "Failed to load leaderboard"))
            }
        }
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = if (isCloudSessionReady()) UiState.Loading else UiState.Success(emptyList()),
      )

  val sectorPilotsState: StateFlow<UiState<List<LivePilotState>>> =
    cloudReadyFlow
      .flatMapLatest { ready ->
        if (!ready) {
          flowOf(UiState.Success(emptyList()))
        } else {
          _activeSectorId
            .flatMapLatest { sectorId ->
              repository
                .observeSectorPilots(sectorId)
                .map<List<LivePilotState>, UiState<List<LivePilotState>>> { UiState.Success(it) }
                .retryWithExponentialBackoff("sectorPilots:$sectorId")
                .catch { error ->
                  if (error is CancellationException) throw error
                  emit(UiState.Error(error.message ?: "Failed to observe sector pilots"))
                }
            }
        }
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = if (isCloudSessionReady()) UiState.Loading else UiState.Success(emptyList()),
      )

  val allOnlineHumansState: StateFlow<List<LivePilotState>> =
    cloudReadyFlow
      .flatMapLatest { ready ->
        if (!ready) {
          flowOf(emptyList())
        } else {
          repository
            .observeAllOnlineHumanPilots()
            .retryWithExponentialBackoff("allOnlineHumanPilots")
            .catch { error ->
              if (error is CancellationException) throw error
              emit(emptyList())
            }
        }
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList(),
      )

  val sectorCombatEventsState: StateFlow<List<SectorCombatEvent>> =
    cloudReadyFlow
      .flatMapLatest { ready ->
        if (!ready) {
          flowOf(emptyList())
        } else {
          _activeSectorId.flatMapLatest { sectorId ->
            if (sectorId == "NEXUS_LOBBY") {
              flowOf(emptyList())
            } else {
              repository
                .observeSectorCombatEvents(sectorId)
                .retryWithExponentialBackoff("combatEvents:$sectorId")
                .catch { error ->
                  if (error is CancellationException) throw error
                  emit(emptyList())
                }
            }
          }
        }
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList(),
      )

  val sectorInstanceState: StateFlow<SectorInstanceState?> =
    cloudReadyFlow
      .flatMapLatest { ready ->
        if (!ready) {
          flowOf(null)
        } else {
          _activeSectorId.flatMapLatest { sectorId ->
            if (sectorId == "NEXUS_LOBBY") {
              flowOf(null)
            } else {
              repository
                .observeSectorInstanceState(sectorId)
                .retryWithExponentialBackoff("sectorInstance:$sectorId")
                .catch { error ->
                  if (error is CancellationException) throw error
                  emit(null)
                }
            }
          }
        }
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = null,
      )

  val cloudSyndicatesState: StateFlow<UiState<List<SyndicateInfo>>> =
    cloudReadyFlow
      .flatMapLatest { ready ->
        if (!ready) {
          flowOf(UiState.Success(emptyList()))
        } else {
          repository
            .observeCloudSyndicates()
            .map<List<SyndicateInfo>, UiState<List<SyndicateInfo>>> { UiState.Success(it) }
            .retryWithExponentialBackoff("cloudSyndicates")
            .catch { error ->
              if (error is CancellationException) throw error
              emit(UiState.Error(error.message ?: "Failed to observe cloud syndicates"))
            }
        }
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = if (isCloudSessionReady()) UiState.Loading else UiState.Success(emptyList()),
      )

  val sectorCommsState: StateFlow<UiState<List<SyndicateChatMessage>>> =
    cloudReadyFlow
      .flatMapLatest { ready ->
        if (!ready) {
          flowOf(UiState.Success(emptyList()))
        } else {
          repository
            .observeSectorComms()
            .map<List<SyndicateChatMessage>, UiState<List<SyndicateChatMessage>>> { UiState.Success(it) }
            .retryWithExponentialBackoff("sectorComms")
            .catch { error ->
              if (error is CancellationException) throw error
              emit(UiState.Error(error.message ?: "Failed to observe sector comms"))
            }
        }
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = if (isCloudSessionReady()) UiState.Loading else UiState.Success(emptyList()),
      )

  val latestCalibrationBroadcast: StateFlow<RiftRepository.LiveCalibrationBroadcast?> =
    cloudReadyFlow
      .flatMapLatest { ready ->
        if (!ready) {
          flowOf(null)
        } else {
          repository
            .observeCustomGameCalibration()
            .retryWithExponentialBackoff("customCalibration")
            .catch { error ->
              if (error is CancellationException) throw error
              emit(null)
            }
        }
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = null,
      )

  val publicCustomGamesState: StateFlow<List<CloudCustomGameRoom>> =
    cloudReadyFlow
      .flatMapLatest { ready ->
        if (!ready) {
          flowOf(emptyList())
        } else {
          repository
            .observePublicCustomGames()
            .retryWithExponentialBackoff("publicCustomGames")
            .catch { error ->
              if (error is CancellationException) throw error
              emit(emptyList())
            }
        }
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList(),
      )

  private val pendingLivePilotState = MutableStateFlow<LivePilotState?>(null)
  private val liveStateRevision = AtomicLong(0L)
  private val liveStateSignal = Channel<Unit>(Channel.CONFLATED)
  private val pendingCloudProgressionJson = MutableStateFlow<String?>(null)
  private val cloudProgressionSignal = Channel<Unit>(Channel.CONFLATED)

  init {
    viewModelScope.launch {
      cloudReadyFlow.collect { ready ->
        if (ready) {
          ensureInitialProfile()
          liveStateSignal.trySend(Unit)
          cloudProgressionSignal.trySend(Unit)
        }
      }
    }
    // Single-flight conflated cloud progression writer paced at >= 1,200ms:
    // Skips consecutive identical JSON payloads while guaranteeing the latest state flushes cleanly.
    viewModelScope.launch {
      var lastWrittenJson: String? = null
      while (true) {
        val nextJson = pendingCloudProgressionJson.value
        if (!isCloudSessionReady() || nextJson.isNullOrBlank() || nextJson == lastWrittenJson) {
          cloudProgressionSignal.receive()
          continue
        }
        val ok =
          try {
            repository.saveCloudPilotProgression(nextJson)
          } catch (e: CancellationException) {
            throw e
          } catch (e: Exception) {
            Log.w(TAG, "Transient cloud progression sync error", e)
            false
          }
        if (ok) {
          lastWrittenJson = nextJson
        }
        delay(if (ok) 1_200L else 4_000L)
      }
    }
    // Single-flight conflated presence/telemetry writer:
    // Prevents concurrent Firestore .update() calls from piling up and stalling after initial spawn.
    viewModelScope.launch {
      var lastSentRevision = 0L
      var lastSentSector = ""
      var lastSentFiring = false
      var lastSentSecondarySeq = 0L
      var lastSentRespawning = false
      while (true) {
        val currentRev = liveStateRevision.get()
        val snapshot = pendingLivePilotState.value
        if (!isCloudSessionReady() || snapshot == null || currentRev <= lastSentRevision) {
          liveStateSignal.receive()
          continue
        }
        lastSentRevision = currentRev
        lastSentSector = snapshot.sectorId
        lastSentFiring = snapshot.isFiringPrimary
        lastSentSecondarySeq = snapshot.secondarySeq
        lastSentRespawning = snapshot.isRespawning
        try {
          repository.saveLivePilotState(snapshot)
        } catch (e: CancellationException) {
          throw e
        } catch (e: Exception) {
          Log.w(TAG, "Transient live state sync error", e)
        }
        // Pace steady position updates at ~115ms while waking immediately on urgent combat/sector changes
        withTimeoutOrNull(115L) {
          while (true) {
            liveStateSignal.receive()
            val next = pendingLivePilotState.value
            if (
              next != null &&
                (next.sectorId != lastSentSector ||
                  next.isFiringPrimary != lastSentFiring ||
                  next.secondarySeq != lastSentSecondarySeq ||
                  next.isRespawning != lastSentRespawning)
            ) {
              break
            }
          }
        }
      }
    }
  }

  private fun ensureInitialProfile() {
    if (!isCloudSessionReady()) return
    viewModelScope.launch {
      try {
        val resolvedUid = Firebase.auth.currentUser?.uid?.trim()?.ifEmpty { currentUserId } ?: currentUserId
        val existingResult = repository.getPilotDossier(resolvedUid)
        if (existingResult.isFailure) return@launch
        val existing = existingResult.getOrNull()
        if (existing == null) {
          val defaultDossier =
            PilotDossier(
              userId = resolvedUid,
              callsign = initialCallsign,
              syndicateTag = "",
              syndicateName = "Unaffiliated",
              preferredShipId = 1L,
              preferredFoundry = "Independent",
              preferredMode = "SECTOR_CONVERGENCE",
              pvpKills = 0L,
              pvpDeaths = 0L,
              pvpBountyPeak = 100L,
              allTimeKills = 0L,
              allTimeDeaths = 0L,
              allTimeBountyPeak = 100L,
              convergencesWon = 0L,
            )
          repository.savePilotDossier(defaultDossier)
          repository.saveLeaderboardEntry(
            LeaderboardEntry(
              userId = resolvedUid,
              callsign = initialCallsign,
              shipId = 1L,
              syndicateTag = "",
              armadaTag = "SOLO",
              armadaTierSize = 1,
              foundry = "Independent",
              pvpKills = 0L,
              pvpDeaths = 0L,
              pvpBounty = 100L,
              allTimeKills = 0L,
              allTimeDeaths = 0L,
              allTimeBounty = 100L,
              convergencesWon = 0L,
            )
          )
        }
      } catch (e: Exception) {
        Log.w(TAG, "Initial profile check skipped or offline", e)
      }
    }
  }

  fun syncCloudPilotProgression(payloadJson: String) {
    val cleanJson = payloadJson.trim()
    if (cleanJson.isEmpty() || !isCloudSessionReady()) return
    pendingCloudProgressionJson.value = cleanJson
    cloudProgressionSignal.trySend(Unit)
  }

  fun observeSyndicateMeta(tag: String): Flow<CloudSyndicateMeta?> {
    val cleanTag = tag.trim().uppercase(Locale.US)
    if (!isCloudSessionReady() || cleanTag.isEmpty() || cleanTag in LocalPilotPrefs.NON_PLAYER_SYNDICATE_TAGS) {
      return flowOf(null)
    }
    return repository
      .observeCloudSyndicateMeta(cleanTag)
      .retryWithExponentialBackoff("syndicateMeta:$cleanTag")
      .catch { error ->
        if (error is CancellationException) throw error
        Log.w(TAG, "Error observing syndicate meta for $cleanTag", error)
        emit(null)
      }
  }

  fun syncSyndicateMetaToCloud(
    tag: String,
    crestJson: String,
    channels: List<String>,
    invitedGuests: Set<String>,
    inviteOnly: Boolean = false,
    officerCallsigns: Set<String> = emptySet(),
  ) {
    val cleanTag = tag.trim().uppercase(Locale.US)
    if (!isCloudSessionReady() || cleanTag.isEmpty() || cleanTag in LocalPilotPrefs.NON_PLAYER_SYNDICATE_TAGS) {
      return
    }
    viewModelScope.launch {
      try {
        repository.saveCloudSyndicateMeta(
          CloudSyndicateMeta(
            tag = cleanTag,
            ownerId = currentUserId,
            crestJson = crestJson,
            customChannels = channels,
            invitedGuests = invitedGuests,
            inviteOnly = inviteOnly,
            officerCallsigns = officerCallsigns,
            updatedAtMs = System.currentTimeMillis(),
          )
        )
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        Log.w(TAG, "Failed to sync cloud syndicate meta", e)
      }
    }
  }

  fun createCloudSyndicate(syndicate: SyndicateInfo) {
    if (!isCloudSessionReady()) return
    viewModelScope.launch {
      try {
        repository.saveCloudSyndicate(syndicate)
      } catch (e: Exception) {
        Log.w(TAG, "Failed to save cloud syndicate", e)
      }
    }
  }

  fun sendSectorComm(msg: SyndicateChatMessage) {
    if (!isCloudSessionReady()) return
    viewModelScope.launch {
      try {
        repository.postSectorCommMessage(msg)
      } catch (e: Exception) {
        Log.w(TAG, "Failed to post sector comm message", e)
      }
    }
  }

  fun emitCombatEvent(event: SectorCombatEvent) {
    if (!isCloudSessionReady()) return
    viewModelScope.launch {
      try {
        repository.postSectorCombatEvent(event)
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        Log.w(TAG, "Failed to emit combat event", e)
      }
    }
  }

  fun publishSectorInstanceState(state: SectorInstanceState) {
    if (!isCloudSessionReady()) return
    viewModelScope.launch {
      try {
        repository.saveSectorInstanceState(state)
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        Log.w(TAG, "Failed to publish sector instance state", e)
      }
    }
  }

  fun postStructuredChannelMessage(
    senderCallsign: String,
    senderSyndicateTag: String,
    channelKey: String,
    messageText: String,
    armadaName: String = "",
    armadaCurrentSize: Int = 1,
    armadaMaxSize: Int = 5,
    armadaObjective: String = "",
    isArmadaRecruitBanner: Boolean = false,
  ): SyndicateChatMessage {
    val now = System.currentTimeMillis()
    val safeChannelLabel = channelKey.trim().uppercase(Locale.US).take(24).ifEmpty { "#SECTOR-LOUNGE" }
    val safeMessage = UgcModeration.sanitizeUgcText(messageText).take(240)
    val safeObjective =
      (if (
        armadaObjective.startsWith("BUILD|", ignoreCase = true) ||
          armadaObjective.startsWith("ROOM|", ignoreCase = true) ||
          armadaObjective.startsWith("BUG|", ignoreCase = true)
      ) {
        armadaObjective.trim()
      } else {
        UgcModeration.sanitizeUgcText(armadaObjective)
      }).take(120)

    val msg =
      SyndicateChatMessage(
        id = "comm_${now}_${(100..999).random()}",
        senderUid = currentUserId,
        senderCallsign = senderCallsign,
        senderSyndicateTag = senderSyndicateTag,
        timestampLabel = safeChannelLabel,
        messageText = safeMessage,
        isArmadaRecruitBanner = isArmadaRecruitBanner,
        armadaName = armadaName.trim().uppercase(Locale.US).take(14),
        armadaCurrentSize = armadaCurrentSize.coerceIn(1, 5),
        armadaMaxSize = armadaMaxSize.coerceIn(1, 5),
        armadaObjective = safeObjective,
        requestSent = false,
        createdAtMs = now,
      )

    if (isCloudSessionReady()) {
      viewModelScope.launch {
        try {
          repository.postSectorCommMessage(msg)
        } catch (e: CancellationException) {
          throw e
        } catch (e: Exception) {
          Log.w(TAG, "Failed to post structured channel message", e)
        }
      }
    }
    return msg
  }

  fun submitCloudBugReport(
    reporterCallsign: String,
    syndicateTag: String,
    category: String,
    description: String,
    buildVersion: Int,
    activeMode: String,
    activeShipId: Int,
    screenDpSummary: String,
    onResult: (Boolean) -> Unit = {},
  ) {
    val now = System.currentTimeMillis()
    val report =
      SectorBugReport(
        id = "bug_${now}_${(100..999).random()}",
        reporterUid = currentUserId,
        reporterCallsign = reporterCallsign,
        syndicateTag = syndicateTag,
        category = category,
        description = description,
        buildVersion = buildVersion,
        activeMode = activeMode,
        activeShipId = activeShipId,
        screenDpSummary = screenDpSummary,
        createdAtMs = now,
      )
    viewModelScope.launch {
      val ok =
        try {
          repository.submitBugReportToCloud(report).isSuccess
        } catch (e: CancellationException) {
          throw e
        } catch (e: Exception) {
          Log.w(TAG, "Failed to submit cloud bug report", e)
          false
        }
      onResult(ok)
    }
  }

  fun lookupAndAddRealFriendByCallsign(
    rawCallsign: String,
    localPrefs: LocalPilotPrefs,
    onResult: (FriendPilotEntry?, String) -> Unit,
  ) {
    val cleanTarget =
      rawCallsign
        .trim()
        .uppercase(Locale.US)
        .replace(Regex("[^A-Z0-9_\\-]"), "")
        .take(14)
    if (cleanTarget.length < 2 || UgcModeration.containsProfanity(cleanTarget)) {
      onResult(null, "INVALID OR RESTRICTED CALLSIGN.")
      return
    }
    if (cleanTarget.equals(initialCallsign, ignoreCase = true)) {
      onResult(null, "CANNOT ADD YOUR OWN CALLSIGN TO STRIKE WING ROSTER.")
      return
    }
    val existingLocal = localPrefs.addFriendByCallsign(cleanTarget)
    if (existingLocal != null) {
      onResult(
        existingLocal,
        "VERIFIED PILOT [${existingLocal.syndicateTag.ifEmpty { "SOLO" }}] ${existingLocal.callsign} ALREADY IN ROSTER.",
      )
      return
    }
    if (!isCloudSessionReady()) {
      onResult(null, "LINK GOOGLE ACCOUNT IN HELM SYSTEMS TO SEARCH LIVE SWORN PILOTS.")
      return
    }
    viewModelScope.launch {
      val found =
        try {
          repository.findRealSwornPilotByCallsign(cleanTarget, excludeUid = currentUserId)
        } catch (e: CancellationException) {
          throw e
        } catch (e: Exception) {
          Log.w(TAG, "Failed to lookup sworn pilot by callsign", e)
          null
        }
      if (found != null) {
        localPrefs.saveVerifiedFriendPilot(found)
        val badge = found.syndicateTag.ifEmpty { "SOLO" }
        onResult(found, "VERIFIED SWORN PILOT [$badge] ${found.callsign} ADDED TO ROSTER.")
      } else {
        onResult(null, "NO VERIFIED SWORN PILOT FOUND FOR CALLSIGN $cleanTarget.")
      }
    }
  }

  fun broadcastCalibrationToCustomGame(
    customGameId: String,
    authorCallsign: String,
    payloadJson: String,
    onResult: (Boolean) -> Unit = {},
  ) {
    viewModelScope.launch {
      val ok =
        try {
          repository.broadcastCustomGameCalibration(customGameId, authorCallsign, payloadJson)
        } catch (e: CancellationException) {
          throw e
        } catch (e: Exception) {
          Log.w(TAG, "Failed to broadcast custom game calibration", e)
          false
        }
      onResult(ok)
    }
  }

  fun publishCustomGameRoom(
    room: CloudCustomGameRoom,
    onResult: (Boolean) -> Unit = {},
  ) {
    if (!isCloudSessionReady()) {
      onResult(false)
      return
    }
    viewModelScope.launch {
      val ok =
        try {
          repository.saveCustomGameRoom(room).isSuccess
        } catch (e: CancellationException) {
          throw e
        } catch (e: Exception) {
          Log.w(TAG, "Failed to publish custom game room", e)
          false
        }
      onResult(ok)
    }
  }

  fun lookupCustomGameByCode(
    rawCode: String,
    onResult: (CloudCustomGameRoom?, String) -> Unit,
  ) {
    val cleanCode = rawCode.trim().uppercase(Locale.US).replace(Regex("[^A-Z0-9]"), "").take(8)
    if (cleanCode.length !in 4..8) {
      onResult(null, "ENTER A VALID 6-CHARACTER JOIN CODE.")
      return
    }
    if (!isCloudSessionReady()) {
      onResult(null, "SIGN IN WITH GOOGLE TO JOIN REMOTE CUSTOM GAMES BY CODE.")
      return
    }
    viewModelScope.launch {
      val result =
        try {
          repository.getCustomGameByJoinCode(cleanCode)
        } catch (e: CancellationException) {
          throw e
        } catch (e: Exception) {
          Result.failure(e)
        }
      val found = result.getOrNull()
      if (found != null) {
        onResult(found, "JOINING CUSTOM GAME [${found.roomCode}] // ${found.title}")
      } else if (result.isFailure) {
        onResult(null, "UNABLE TO REACH CLOUD DIRECTORY FOR CODE [$cleanCode].")
      } else {
        onResult(null, "NO ACTIVE CUSTOM GAME FOUND FOR CODE [$cleanCode].")
      }
    }
  }

  fun syncFromEngineJson(payloadJson: String) {
    if (!isCloudSessionReady()) return
    viewModelScope.launch {
      try {
        val obj = JSONObject(payloadJson)
        val callsign = obj.optString("callsign", initialCallsign)
        val shipId = obj.optLong("shipId", 1L)
        val foundry = obj.optString("foundry", "Independent")
        val mode = repository.sanitizeSectorMode(obj.optString("mode", "SECTOR_CONVERGENCE"))
        val pvpKills = obj.optLong("pvpKills", 0L)
        val pvpDeaths = obj.optLong("pvpDeaths", 0L)
        val pvpBountyPeak = obj.optLong("pvpBountyPeak", 100L)
        val allTimeKills = obj.optLong("allTimeKills", 0L)
        val allTimeDeaths = obj.optLong("allTimeDeaths", 0L)
        val allTimeBountyPeak = obj.optLong("allTimeBountyPeak", 100L)
        val convergencesWon = obj.optLong("convergencesWon", 0L)

        if (_activeSectorId.value != mode) {
          _activeSectorId.value = mode
        }

        repository.savePilotDossier(
          PilotDossier(
            userId = currentUserId,
            callsign = callsign,
            syndicateTag = "",
            syndicateName = "Unaffiliated",
            preferredShipId = shipId,
            preferredFoundry = foundry,
            preferredMode = mode,
            pvpKills = pvpKills,
            pvpDeaths = pvpDeaths,
            pvpBountyPeak = pvpBountyPeak,
            allTimeKills = allTimeKills,
            allTimeDeaths = allTimeDeaths,
            allTimeBountyPeak = allTimeBountyPeak,
            convergencesWon = convergencesWon,
          )
        )
        repository.saveLeaderboardEntry(
          LeaderboardEntry(
            userId = currentUserId,
            callsign = callsign,
            shipId = shipId,
            syndicateTag = "",
            foundry = foundry,
            pvpKills = pvpKills,
            pvpDeaths = pvpDeaths,
            pvpBounty = pvpBountyPeak,
            allTimeKills = allTimeKills,
            allTimeDeaths = allTimeDeaths,
            allTimeBounty = allTimeBountyPeak,
            convergencesWon = convergencesWon,
          )
        )
      } catch (e: Exception) {
        Log.w(TAG, "Failed to parse engine stats JSON", e)
      }
    }
  }

  fun syncTelemetryFromEngineJson(payloadJson: String) {
    if (!isCloudSessionReady()) return
    viewModelScope.launch {
      try {
        val obj = JSONObject(payloadJson)
        val sectorId = repository.sanitizeSectorMode(obj.optString("sectorId", "SECTOR_CONVERGENCE"))
        if (_activeSectorId.value != sectorId) {
          _activeSectorId.value = sectorId
        }
        val state =
          LivePilotState(
            userId = currentUserId,
            sectorId = sectorId,
            callsign = obj.optString("callsign", initialCallsign),
            shipId = obj.optLong("shipId", 1L),
            freq = obj.optLong("freq", 0L),
            x = obj.optDouble("x", 2000.0),
            y = obj.optDouble("y", 2000.0),
            vx = obj.optDouble("vx", 0.0),
            vy = obj.optDouble("vy", 0.0),
            angle = obj.optDouble("angle", 0.0),
            energy = obj.optLong("energy", 1200L),
            maxEnergy = obj.optLong("maxEnergy", 1200L),
            bounty = obj.optLong("bounty", 100L),
            lastPingType = obj.optString("lastPingType", "NONE"),
            lastPingX = obj.optDouble("lastPingX", 0.0),
            lastPingY = obj.optDouble("lastPingY", 0.0),
          )
        repository.saveLivePilotState(state)
      } catch (e: Exception) {
        Log.w(TAG, "Failed to parse telemetry JSON", e)
      }
    }
  }

  fun syncNativePilotStats(
    callsign: String,
    shipId: Int,
    foundry: String,
    mode: String,
    pvpKills: Long,
    pvpDeaths: Long,
    pvpBountyPeak: Long,
    allTimeKills: Long,
    allTimeDeaths: Long,
    allTimeBountyPeak: Long,
    convergencesWon: Long,
    syndicateTag: String = "",
    syndicateName: String = "",
    armadaTag: String = "SOLO",
    armadaTierSize: Int = 1,
  ) {
    if (!isCloudSessionReady()) return
    viewModelScope.launch {
      try {
        repository.savePilotDossier(
          PilotDossier(
            userId = currentUserId,
            callsign = callsign,
            syndicateTag = syndicateTag,
            syndicateName = syndicateName,
            preferredShipId = shipId.toLong(),
            preferredFoundry = foundry,
            preferredMode = mode,
            pvpKills = pvpKills,
            pvpDeaths = pvpDeaths,
            pvpBountyPeak = pvpBountyPeak,
            allTimeKills = allTimeKills,
            allTimeDeaths = allTimeDeaths,
            allTimeBountyPeak = allTimeBountyPeak,
            convergencesWon = convergencesWon,
          )
        )
        repository.saveLeaderboardEntry(
          LeaderboardEntry(
            userId = currentUserId,
            callsign = callsign,
            shipId = shipId.toLong(),
            syndicateTag = syndicateTag,
            armadaTag = armadaTag.ifBlank { "SOLO" },
            armadaTierSize = armadaTierSize.coerceIn(1, 5),
            foundry = foundry,
            pvpKills = pvpKills,
            pvpDeaths = pvpDeaths,
            pvpBounty = pvpBountyPeak,
            allTimeKills = allTimeKills,
            allTimeDeaths = allTimeDeaths,
            allTimeBounty = allTimeBountyPeak,
            convergencesWon = convergencesWon,
          )
        )
      } catch (e: Exception) {
        Log.w(TAG, "Failed to sync native pilot stats", e)
      }
    }
  }

  fun syncNativeLiveState(
    sectorId: String,
    callsign: String,
    syndicateTag: String = "",
    armadaName: String = "",
    shipId: Int,
    freq: Int,
    x: Float,
    y: Float,
    vx: Float,
    vy: Float,
    angle: Float,
    weaponAngle: Float = angle,
    isFiringPrimary: Boolean = false,
    isAfterburning: Boolean = false,
    isCloaked: Boolean = false,
    isRespawning: Boolean = false,
    isInStasisBrig: Boolean = false,
    secondarySeq: Long = 0L,
    secondaryType: String = "NONE",
    secondaryAim: Float = angle,
    flux: Float,
    maxFlux: Float,
    bounty: Int,
    liveResonance: Int = 0,
  ) {
    val cleanSector = repository.sanitizeSectorMode(sectorId.ifBlank { "NEXUS_LOBBY" })
    if (_activeSectorId.value != cleanSector) {
      _activeSectorId.value = cleanSector
    }
    if (!isCloudSessionReady()) return
    pendingLivePilotState.value =
      LivePilotState(
        userId = currentUserId,
        sectorId = cleanSector,
        callsign = callsign,
        syndicateTag = syndicateTag,
        armadaName = armadaName.take(14),
        shipId = shipId.toLong(),
        freq = freq.toLong(),
        x = x.toDouble(),
        y = y.toDouble(),
        vx = vx.toDouble(),
        vy = vy.toDouble(),
        angle = angle.toDouble(),
        weaponAngle = weaponAngle.toDouble(),
        isFiringPrimary = isFiringPrimary,
        isAfterburning = isAfterburning,
        isCloaked = isCloaked,
        isRespawning = isRespawning,
        isInStasisBrig = isInStasisBrig,
        secondarySeq = secondarySeq,
        secondaryType = secondaryType,
        secondaryAim = secondaryAim.toDouble(),
        energy = flux.toLong().coerceIn(0L, 10000L),
        maxEnergy = maxFlux.toLong().coerceIn(100L, 10000L),
        bounty = bounty.toLong(),
        liveResonance = liveResonance.toLong(),
        lastPingType = "NONE",
        lastPingX = 0.0,
        lastPingY = 0.0,
      )
    liveStateRevision.incrementAndGet()
    liveStateSignal.trySend(Unit)
  }

  fun buildCloudSnapshotJson(): String {
    val root = JSONObject()
    root.put("userId", currentUserId)
    root.put("defaultCallsign", initialCallsign)

    val dossier = (dossierState.value as? UiState.Success)?.data
    if (dossier != null) {
      val dObj =
        JSONObject().apply {
          put("callsign", dossier.callsign)
          put("preferredShipId", dossier.preferredShipId)
          put("preferredFoundry", dossier.preferredFoundry)
          put("preferredMode", dossier.preferredMode)
          put("pvpKills", dossier.pvpKills)
          put("pvpDeaths", dossier.pvpDeaths)
          put("pvpBountyPeak", dossier.pvpBountyPeak)
          put("allTimeKills", dossier.allTimeKills)
          put("allTimeDeaths", dossier.allTimeDeaths)
          put("allTimeBountyPeak", dossier.allTimeBountyPeak)
          put("convergencesWon", dossier.convergencesWon)
        }
      root.put("dossier", dObj)
    }

    val lbList = (leaderboardState.value as? UiState.Success)?.data ?: emptyList()
    val lbArray = JSONArray()
    lbList.forEach { entry ->
      lbArray.put(
        JSONObject().apply {
          put("userId", entry.userId.ifEmpty { entry.id })
          put("callsign", entry.callsign)
          put("shipId", entry.shipId)
          put("foundry", entry.foundry)
          put("pvpKills", entry.pvpKills)
          put("pvpDeaths", entry.pvpDeaths)
          put("pvpBounty", entry.pvpBounty)
          put("allTimeKills", entry.allTimeKills)
          put("allTimeDeaths", entry.allTimeDeaths)
          put("allTimeBounty", entry.allTimeBounty)
          put("convergencesWon", entry.convergencesWon)
          put("isHuman", true)
        }
      )
    }
    root.put("leaderboard", lbArray)

    val pilotsList = (sectorPilotsState.value as? UiState.Success)?.data ?: emptyList()
    val pilotsArray = JSONArray()
    pilotsList.forEach { p ->
      val pUid = p.userId.ifEmpty { p.id }.trim()
      if (pUid.isNotEmpty() && pUid != currentUserId) {
        pilotsArray.put(
          JSONObject().apply {
            put("userId", pUid)
            put("sectorId", p.sectorId)
            put("callsign", p.callsign)
            put("shipId", p.shipId)
            put("freq", p.freq)
            put("x", p.x)
            put("y", p.y)
            put("vx", p.vx)
            put("vy", p.vy)
            put("angle", p.angle)
            put("energy", p.energy)
            put("maxEnergy", p.maxEnergy)
            put("bounty", p.bounty)
            put("liveResonance", p.liveResonance)
            put("lastPingType", p.lastPingType)
            put("lastPingX", p.lastPingX)
            put("lastPingY", p.lastPingY)
          }
        )
      }
    }
    root.put("remotePilots", pilotsArray)

    return root.toString()
  }

  private companion object {
    const val TAG = "RiftViewModel"
    const val STOP_TIMEOUT_MILLIS = 5000L
  }
}
