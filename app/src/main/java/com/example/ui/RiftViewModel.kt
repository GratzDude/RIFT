package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.LeaderboardEntry
import com.example.data.LivePilotState
import com.example.data.PilotDossier
import com.example.data.RiftRepository
import com.example.data.SyndicateChatMessage
import com.example.data.SyndicateInfo
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

sealed interface UiState<out T> {
  data object Loading : UiState<Nothing>
  data class Success<T>(val data: T) : UiState<T>
  data class Error(val message: String) : UiState<Nothing>
}

class RiftViewModel(
  private val repository: RiftRepository,
  val currentUserId: String,
  val initialCallsign: String,
) : ViewModel() {

  private val _activeSectorId = MutableStateFlow("FFA")
  val activeSectorId: StateFlow<String> = _activeSectorId.asStateFlow()

  val dossierState: StateFlow<UiState<PilotDossier?>> =
    repository
      .observePilotDossier(currentUserId)
      .map<PilotDossier?, UiState<PilotDossier?>> { UiState.Success(it) }
      .catch { error ->
        val firestoreError =
          generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val rootError = firestoreError ?: error
        Log.w(TAG, "Error observing pilot dossier", rootError)
        emit(UiState.Error(rootError.message ?: "Failed to load pilot dossier"))
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading,
      )

  val leaderboardState: StateFlow<UiState<List<LeaderboardEntry>>> =
    repository
      .observeLeaderboard()
      .map<List<LeaderboardEntry>, UiState<List<LeaderboardEntry>>> { UiState.Success(it) }
      .catch { error ->
        val firestoreError =
          generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val rootError = firestoreError ?: error
        Log.w(TAG, "Error observing leaderboard", rootError)
        emit(UiState.Error(rootError.message ?: "Failed to load leaderboard"))
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading,
      )

  @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
  val sectorPilotsState: StateFlow<UiState<List<LivePilotState>>> =
    _activeSectorId
      .flatMapLatest { sectorId -> repository.observeSectorPilots(sectorId) }
      .map<List<LivePilotState>, UiState<List<LivePilotState>>> { UiState.Success(it) }
      .catch { error ->
        val firestoreError =
          generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val rootError = firestoreError ?: error
        Log.w(TAG, "Error observing sector pilots", rootError)
        emit(UiState.Error(rootError.message ?: "Failed to observe sector pilots"))
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading,
      )

  val cloudSyndicatesState: StateFlow<UiState<List<SyndicateInfo>>> =
    repository
      .observeCloudSyndicates()
      .map<List<SyndicateInfo>, UiState<List<SyndicateInfo>>> { UiState.Success(it) }
      .catch { error ->
        val firestoreError =
          generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val rootError = firestoreError ?: error
        Log.w(TAG, "Error observing cloud syndicates", rootError)
        emit(UiState.Error(rootError.message ?: "Failed to observe cloud syndicates"))
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading,
      )

  val sectorCommsState: StateFlow<UiState<List<SyndicateChatMessage>>> =
    repository
      .observeSectorComms()
      .map<List<SyndicateChatMessage>, UiState<List<SyndicateChatMessage>>> { UiState.Success(it) }
      .catch { error ->
        val firestoreError =
          generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val rootError = firestoreError ?: error
        Log.w(TAG, "Error observing sector comms", rootError)
        emit(UiState.Error(rootError.message ?: "Failed to observe sector comms"))
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading,
      )

  init {
    ensureInitialProfile()
  }

  private fun ensureInitialProfile() {
    viewModelScope.launch {
      try {
        val existing = repository.getPilotDossier(currentUserId).getOrNull()
        if (existing == null) {
          val defaultDossier =
            PilotDossier(
              userId = currentUserId,
              callsign = initialCallsign,
              preferredShipId = 1L,
              preferredFoundry = "Independent",
              preferredMode = "FFA",
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
              userId = currentUserId,
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

  fun createCloudSyndicate(syndicate: SyndicateInfo) {
    viewModelScope.launch {
      try {
        repository.saveCloudSyndicate(syndicate)
      } catch (e: Exception) {
        Log.w(TAG, "Failed to save cloud syndicate", e)
      }
    }
  }

  fun sendSectorComm(msg: SyndicateChatMessage) {
    viewModelScope.launch {
      try {
        repository.postSectorCommMessage(msg)
      } catch (e: Exception) {
        Log.w(TAG, "Failed to post sector comm message", e)
      }
    }
  }

  fun syncFromEngineJson(payloadJson: String) {
    viewModelScope.launch {
      try {
        val obj = JSONObject(payloadJson)
        val callsign = obj.optString("callsign", initialCallsign)
        val shipId = obj.optLong("shipId", 1L)
        val foundry = obj.optString("foundry", "Independent")
        val mode = "FFA"
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
    viewModelScope.launch {
      try {
        val obj = JSONObject(payloadJson)
        val sectorId = "FFA"
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
    armadaTag: String = "SOLO",
    armadaTierSize: Int = 1,
  ) {
    if (_activeSectorId.value != "FFA") {
      _activeSectorId.value = "FFA"
    }
    viewModelScope.launch {
      try {
        repository.savePilotDossier(
          PilotDossier(
            userId = currentUserId,
            callsign = callsign,
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
    flux: Float,
    maxFlux: Float,
    bounty: Int,
  ) {
    viewModelScope.launch {
      try {
        repository.saveLivePilotState(
          LivePilotState(
            userId = currentUserId,
            sectorId = sectorId,
            callsign = callsign,
            syndicateTag = syndicateTag,
            armadaName = armadaName,
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
            energy = flux.toLong().coerceIn(0L, 10000L),
            maxEnergy = maxFlux.toLong().coerceIn(100L, 10000L),
            bounty = bounty.toLong(),
            lastPingType = "NONE",
            lastPingX = 0.0,
            lastPingY = 0.0,
          )
        )
      } catch (e: Exception) {
        Log.w(TAG, "Failed to sync native live state", e)
      }
    }
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
    val nowMs = System.currentTimeMillis()
    pilotsList.forEach { p ->
      val updatedMs = p.updatedAt?.toDate()?.time ?: nowMs
      // Include active human pilots seen within the last 60 seconds
      if (p.userId != currentUserId && (nowMs - updatedMs) <= 60_000L) {
        pilotsArray.put(
          JSONObject().apply {
            put("userId", p.userId.ifEmpty { p.id })
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
