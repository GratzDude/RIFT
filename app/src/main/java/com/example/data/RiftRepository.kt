package com.example.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.MemoryCacheSettings
import com.google.firebase.firestore.snapshots
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import org.json.JSONArray

object NetworkConnectivityMonitor {
  fun isCurrentlyOnline(context: Context): Boolean {
    return try {
      val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
      val activeNet = cm.activeNetwork ?: return false
      val caps = cm.getNetworkCapabilities(activeNet) ?: return false
      caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    } catch (_: Exception) {
      false
    }
  }

  fun observeOnlineState(context: Context): Flow<Boolean> =
    callbackFlow {
        val appContext = context.applicationContext
        val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        if (cm == null) {
          trySend(false)
          awaitClose {}
          return@callbackFlow
        }
        trySend(isCurrentlyOnline(appContext))
        val callback =
          object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
              trySend(isCurrentlyOnline(appContext))
            }

            override fun onLost(network: Network) {
              trySend(isCurrentlyOnline(appContext))
            }

            override fun onCapabilitiesChanged(
              network: Network,
              networkCapabilities: NetworkCapabilities,
            ) {
              val hasNet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
              trySend(hasNet || isCurrentlyOnline(appContext))
            }
          }
        val registered =
          try {
            val req =
              NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            cm.registerNetworkCallback(req, callback)
            true
          } catch (_: Exception) {
            false
          }
        awaitClose {
          if (registered) {
            runCatching { cm.unregisterNetworkCallback(callback) }
          }
        }
      }
      .distinctUntilChanged()
}

typealias LiveCalibrationBroadcast = RiftRepository.LiveCalibrationBroadcast

class RiftRepository(private val dbProvider: () -> FirebaseFirestore) {
  private val localSeenMsByUid = ConcurrentHashMap<String, Long>()
  private val knownExistingPilotDocUids = ConcurrentHashMap.newKeySet<String>()
  private val _cloudSyncHealth = MutableStateFlow(CloudSyncHealth())
  val cloudSyncHealth: StateFlow<CloudSyncHealth> = _cloudSyncHealth.asStateFlow()

  data class LiveCalibrationBroadcast(
    val customGameId: String = "",
    val authorCallsign: String = "",
    val payloadJson: String = "",
    val updatedAtMs: Long = 0L,
  )

  constructor() : this({ FirebaseFirestore.getInstance() })

  constructor(db: FirebaseFirestore) : this({ db })

  constructor(context: Context) : this({
    val appContext = context.applicationContext
    val db =
      FirebaseFirestore.getInstance(
        appContext.getString(R.string.firestore_database_id)
      )
    try {
      db.firestoreSettings =
        FirebaseFirestoreSettings.Builder()
          .setLocalCacheSettings(MemoryCacheSettings.newBuilder().build())
          .build()
    } catch (_: Exception) {
      // Settings already applied on this instance
    }
    db
  })

  private val db: FirebaseFirestore by lazy { dbProvider() }

  private val firestore: FirebaseFirestore?
    get() = runCatching { db }.getOrNull()

  private val auth = Firebase.auth

  private fun markHeartbeatSuccess() {
    val now = System.currentTimeMillis()
    val prev = _cloudSyncHealth.value
    _cloudSyncHealth.value =
      prev.copy(
        isConnected = true,
        lastHeartbeatOkMs = now,
        statusLabel = "LIVE SYNC OK",
        lastError = null,
      )
  }

  private fun markSnapshotSuccess() {
    val now = System.currentTimeMillis()
    val prev = _cloudSyncHealth.value
    _cloudSyncHealth.value =
      prev.copy(
        isConnected = true,
        lastSnapshotOkMs = now,
        statusLabel = "LIVE SYNC OK",
        lastError = null,
      )
  }

  private fun markSyncError(error: Throwable) {
    val prev = _cloudSyncHealth.value
    val msg = (error.message ?: error.javaClass.simpleName).take(72)
    _cloudSyncHealth.value =
      prev.copy(
        isConnected = false,
        statusLabel = "SYNC RETRYING",
        lastError = msg,
      )
  }

  private fun requireUserId(): String {
    val uid = auth.currentUser?.uid?.trim()
    if (uid.isNullOrEmpty() || uid.startsWith("dev_")) {
      throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }
    return uid
  }

  private fun isAuthenticatedNonGuest(): Boolean {
    val uid = auth.currentUser?.uid?.trim()
    return !uid.isNullOrEmpty() && !uid.startsWith("dev_")
  }

  private fun sanitizeCallsign(raw: String): String {
    val cleaned = raw.trim().replace(Regex("[^a-zA-Z0-9_\\- ]"), "")
    return when {
      cleaned.length < 2 -> "PILOT_01"
      cleaned.length > 24 -> cleaned.take(24)
      else -> cleaned
    }
  }

  private fun sanitizeSyndicateTag(raw: String): String {
    return raw.trim().uppercase(Locale.US).replace(Regex("[^A-Z0-9_\\-]"), "").take(4)
  }

  private fun sanitizeArmadaTag(raw: String): String {
    return raw.trim().uppercase(Locale.US).replace(Regex("[^A-Z0-9_\\-]"), "").take(14)
  }

  private fun sanitizeFoundry(raw: String): String {
    val clean = raw.trim()
    return if (clean.isEmpty()) "Independent" else clean.take(32)
  }

  fun sanitizeSectorMode(raw: String): String {
    val trimmed = raw.trim()
    val withoutLegacyOne =
      when (trimmed.uppercase(Locale.US)) {
        "SECTOR_CONVERGENCE_1" -> "SECTOR_CONVERGENCE"
        "FLUX_CORE_1" -> "FLUX_CORE"
        "RIFT_SKIRMISH_1" -> "RIFT_SKIRMISH"
        "STASIS_BREACH_1" -> "STASIS_BREACH"
        "RIFT_CONTROL_1" -> "RIFT_CONTROL"
        "SYNDICATE_WARS_1" -> "SYNDICATE_WARS"
        else -> trimmed
      }
    val allowed =
      setOf(
        "FFA",
        "SECTOR_CONVERGENCE",
        "SECTOR_CONVERGENCE_2",
        "RIFT_SKIRMISH",
        "RIFT_CONTROL",
        "FLUX_CORE",
        "STASIS_BREACH",
        "SYNDICATE_WARS",
        "NEXUS_LOBBY",
        "warzone_10v10",
        "ranked_3v3",
        "ranked_5v5",
      )
    return when {
      withoutLegacyOne in allowed -> withoutLegacyOne
      withoutLegacyOne.uppercase(Locale.US) in allowed -> withoutLegacyOne.uppercase(Locale.US)
      withoutLegacyOne.startsWith("CUSTOM_") &&
        withoutLegacyOne.length <= 30 &&
        withoutLegacyOne.matches(Regex("^CUSTOM_[A-Za-z0-9_\\-]+$")) -> withoutLegacyOne
      else -> "SECTOR_CONVERGENCE"
    }
  }

  private fun sanitizeMode(raw: String): String = sanitizeSectorMode(raw)

  suspend fun getPilotDossier(userId: String = requireUserId()): Result<PilotDossier?> = runCatching {
    val callerUid = requireUserId()
    val resolvedUid = userId.trim().ifEmpty { callerUid }
    if (resolvedUid.startsWith("dev_")) {
      return@runCatching null
    }
    val path = "users/$resolvedUid"
    try {
      val snapshot = db.collection("users").document(resolvedUid).get().await()
      markSnapshotSuccess()
      if (snapshot.exists()) {
        snapshot.toObject(PilotDossier::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
      } else {
        null
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      if (isAuthenticatedNonGuest()) {
        markSyncError(e)
        handleFirestoreError(e, OperationType.GET, path)
      }
      throw e
    }
  }

  fun observePilotDossier(userId: String): Flow<PilotDossier?> {
    val path = "users/$userId"
    return db.collection("users")
      .document(userId)
      .snapshots()
      .map { snapshot ->
        markSnapshotSuccess()
        if (snapshot.exists()) {
          snapshot.toObject(PilotDossier::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        } else {
          null
        }
      }
      .catch { error ->
        val firestoreError =
          generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && isAuthenticatedNonGuest() && auth.currentUser?.uid == userId) {
          markSyncError(exceptionToLog)
          handleFirestoreError(exceptionToLog, OperationType.GET, path)
        }
        throw error
      }
  }

  suspend fun savePilotDossier(dossier: PilotDossier): Result<Unit> = runCatching {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid)
    val safeCallsign = sanitizeCallsign(dossier.callsign)
    val safeSynTag = sanitizeSyndicateTag(dossier.syndicateTag)
    val safeSynName = dossier.syndicateName.trim().take(48)
    val safeShipId = dossier.preferredShipId.coerceIn(1L, 10L)
    val safeFoundry = sanitizeFoundry(dossier.preferredFoundry)
    val safeMode = sanitizeMode(dossier.preferredMode)

    try {
      val existingSnap = docRef.get().await()
      if (!existingSnap.exists() || existingSnap.getTimestamp("createdAt") == null) {
        val createPayload =
          mapOf(
            "userId" to uid,
            "callsign" to safeCallsign,
            "syndicateTag" to safeSynTag,
            "syndicateName" to safeSynName,
            "preferredShipId" to safeShipId,
            "preferredFoundry" to safeFoundry,
            "preferredMode" to safeMode,
            "pvpKills" to dossier.pvpKills.coerceAtLeast(0L),
            "pvpDeaths" to dossier.pvpDeaths.coerceAtLeast(0L),
            "pvpBountyPeak" to dossier.pvpBountyPeak.coerceAtLeast(0L),
            "allTimeKills" to dossier.allTimeKills.coerceAtLeast(0L),
            "allTimeDeaths" to dossier.allTimeDeaths.coerceAtLeast(0L),
            "allTimeBountyPeak" to dossier.allTimeBountyPeak.coerceAtLeast(0L),
            "convergencesWon" to dossier.convergencesWon.coerceAtLeast(0L),
            "createdAt" to (existingSnap.getTimestamp("createdAt") ?: FieldValue.serverTimestamp()),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        docRef.set(createPayload).await()
      } else {
        val existingObj =
          existingSnap.toObject(PilotDossier::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        val mergedCallsign =
          if (safeCallsign == "COMMANDER" || safeCallsign == "PILOT_01") {
            existingObj?.callsign?.takeIf { it.isNotBlank() && it != "COMMANDER" } ?: safeCallsign
          } else {
            safeCallsign
          }
        val mergedSynTag =
          if (safeSynTag.isNotEmpty() && safeSynTag !in setOf("GUEST", "RFT", "BOT")) safeSynTag
          else existingObj?.syndicateTag?.let { sanitizeSyndicateTag(it) }.orEmpty()
        val mergedSynName =
          if (safeSynName.isNotEmpty() && !safeSynName.contains("Guest", ignoreCase = true)) safeSynName
          else existingObj?.syndicateName?.take(48).orEmpty()
        val updatePayload =
          mapOf(
            "callsign" to mergedCallsign,
            "syndicateTag" to mergedSynTag,
            "syndicateName" to mergedSynName,
            "preferredShipId" to safeShipId,
            "preferredFoundry" to safeFoundry,
            "preferredMode" to safeMode,
            "pvpKills" to kotlin.math.max(existingObj?.pvpKills ?: 0L, dossier.pvpKills.coerceAtLeast(0L)),
            "pvpDeaths" to kotlin.math.max(existingObj?.pvpDeaths ?: 0L, dossier.pvpDeaths.coerceAtLeast(0L)),
            "pvpBountyPeak" to kotlin.math.max(existingObj?.pvpBountyPeak ?: 100L, dossier.pvpBountyPeak.coerceAtLeast(0L)),
            "allTimeKills" to kotlin.math.max(existingObj?.allTimeKills ?: 0L, dossier.allTimeKills.coerceAtLeast(0L)),
            "allTimeDeaths" to kotlin.math.max(existingObj?.allTimeDeaths ?: 0L, dossier.allTimeDeaths.coerceAtLeast(0L)),
            "allTimeBountyPeak" to kotlin.math.max(existingObj?.allTimeBountyPeak ?: 100L, dossier.allTimeBountyPeak.coerceAtLeast(0L)),
            "convergencesWon" to kotlin.math.max(existingObj?.convergencesWon ?: 0L, dossier.convergencesWon.coerceAtLeast(0L)),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        docRef.update(updatePayload).await()
      }
      markHeartbeatSuccess()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      if (isAuthenticatedNonGuest()) {
        markSyncError(e)
        handleFirestoreError(e, OperationType.WRITE, docRef.path)
      }
      throw e
    }
  }

  suspend fun saveLeaderboardEntry(entry: LeaderboardEntry): Result<Unit> = runCatching {
    val uid = requireUserId()
    val safeSyndicateTag = sanitizeSyndicateTag(entry.syndicateTag)
    if (safeSyndicateTag == "RFT" || safeSyndicateTag == "BOT" || entry.callsign.contains("[BOT]", ignoreCase = true)) {
      return@runCatching
    }
    val docRef = db.collection("leaderboard").document(uid)
    val safeCallsign = sanitizeCallsign(entry.callsign)
    val safeShipId = entry.shipId.coerceIn(1L, 10L)
    val safeFoundry = sanitizeFoundry(entry.foundry)
    val safeArmadaTag = sanitizeArmadaTag(entry.armadaTag).take(12).ifEmpty { "SOLO" }
    val safeArmadaTierSize = entry.armadaTierSize.coerceIn(1, 5)

    try {
      val existingSnap = docRef.get().await()
      if (!existingSnap.exists() || existingSnap.getTimestamp("createdAt") == null) {
        val createPayload =
          mapOf(
            "userId" to uid,
            "visibility" to "public",
            "callsign" to safeCallsign,
            "shipId" to safeShipId,
            "syndicateTag" to safeSyndicateTag,
            "armadaTag" to safeArmadaTag,
            "armadaTierSize" to safeArmadaTierSize,
            "foundry" to safeFoundry,
            "pvpKills" to entry.pvpKills.coerceAtLeast(0L),
            "pvpDeaths" to entry.pvpDeaths.coerceAtLeast(0L),
            "pvpBounty" to entry.pvpBounty.coerceAtLeast(0L),
            "allTimeKills" to entry.allTimeKills.coerceAtLeast(0L),
            "allTimeDeaths" to entry.allTimeDeaths.coerceAtLeast(0L),
            "allTimeBounty" to entry.allTimeBounty.coerceAtLeast(0L),
            "convergencesWon" to entry.convergencesWon.coerceAtLeast(0L),
            "createdAt" to (existingSnap.getTimestamp("createdAt") ?: FieldValue.serverTimestamp()),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        docRef.set(createPayload).await()
      } else {
        val existingObj =
          existingSnap.toObject(LeaderboardEntry::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        val mergedCallsign =
          if (safeCallsign == "COMMANDER" || safeCallsign == "PILOT_01") {
            existingObj?.callsign?.takeIf { it.isNotBlank() && it != "COMMANDER" } ?: safeCallsign
          } else {
            safeCallsign
          }
        val updatePayload =
          mapOf(
            "callsign" to mergedCallsign,
            "shipId" to safeShipId,
            "syndicateTag" to safeSyndicateTag,
            "armadaTag" to safeArmadaTag,
            "armadaTierSize" to safeArmadaTierSize,
            "foundry" to safeFoundry,
            "pvpKills" to kotlin.math.max(existingObj?.pvpKills ?: 0L, entry.pvpKills.coerceAtLeast(0L)),
            "pvpDeaths" to kotlin.math.max(existingObj?.pvpDeaths ?: 0L, entry.pvpDeaths.coerceAtLeast(0L)),
            "pvpBounty" to kotlin.math.max(existingObj?.pvpBounty ?: 100L, entry.pvpBounty.coerceAtLeast(0L)),
            "allTimeKills" to kotlin.math.max(existingObj?.allTimeKills ?: 0L, entry.allTimeKills.coerceAtLeast(0L)),
            "allTimeDeaths" to kotlin.math.max(existingObj?.allTimeDeaths ?: 0L, entry.allTimeDeaths.coerceAtLeast(0L)),
            "allTimeBounty" to kotlin.math.max(existingObj?.allTimeBounty ?: 100L, entry.allTimeBounty.coerceAtLeast(0L)),
            "convergencesWon" to kotlin.math.max(existingObj?.convergencesWon ?: 0L, entry.convergencesWon.coerceAtLeast(0L)),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        docRef.update(updatePayload).await()
      }
      markHeartbeatSuccess()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      if (isAuthenticatedNonGuest()) {
        markSyncError(e)
        handleFirestoreError(e, OperationType.WRITE, docRef.path)
      }
      throw e
    }
  }

  fun observeLeaderboard(): Flow<List<LeaderboardEntry>> {
    val path = "leaderboard"
    return db.collection(path)
      .whereEqualTo("visibility", "public")
      .snapshots()
      .map { snapshot ->
        markSnapshotSuccess()
        snapshot
          .toObjects(
            LeaderboardEntry::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
          )
          .filter { entry ->
            val cleanUid = entry.userId.ifBlank { entry.id }.trim()
            val cleanTag = entry.syndicateTag.trim().uppercase(Locale.US)
            cleanUid.isNotEmpty() &&
              !cleanUid.startsWith("dev_") &&
              !cleanUid.startsWith("drifter_") &&
              cleanTag != "BOT" &&
              cleanTag != "RFT" &&
              !entry.callsign.contains("[BOT]", ignoreCase = true)
          }
      }
      .catch { error ->
        val firestoreError =
          generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && isAuthenticatedNonGuest()) {
          markSyncError(exceptionToLog)
          handleFirestoreError(exceptionToLog, OperationType.LIST, path)
        }
        throw error
      }
  }

  suspend fun saveLivePilotState(state: LivePilotState): Result<Unit> = runCatching {
    val uid = requireUserId()
    val docRef = db.collection("sector_pilots").document(uid)
    val safeCallsign = sanitizeCallsign(state.callsign)
    val safeSyndicateTag = sanitizeSyndicateTag(state.syndicateTag)
    val safeArmadaName = sanitizeArmadaTag(state.armadaName).take(12)
    val safeSector = sanitizeMode(state.sectorId)
    val safeShip = state.shipId.coerceIn(1L, 10L)
    val safeFreq = state.freq.coerceIn(0L, 99L)
    val safeSecondaryType =
      state.secondaryType
        .trim()
        .uppercase(Locale.US)
        .replace(Regex("[^A-Z0-9_\\-]"), "")
        .take(28)
        .ifEmpty { "NONE" }
    val safePing =
      if (state.lastPingType in listOf("NONE", "ATTACK", "DEFEND", "CONVERGENCE")) {
        state.lastPingType
      } else {
        "NONE"
      }

    val updatePayload =
      mapOf(
        "visibility" to "public",
        "sectorId" to safeSector,
        "callsign" to safeCallsign,
        "syndicateTag" to safeSyndicateTag,
        "armadaName" to safeArmadaName,
        "shipId" to safeShip,
        "freq" to safeFreq,
        "x" to state.x.coerceIn(-100000.0, 100000.0),
        "y" to state.y.coerceIn(-100000.0, 100000.0),
        "vx" to state.vx.coerceIn(-5000.0, 5000.0),
        "vy" to state.vy.coerceIn(-5000.0, 5000.0),
        "angle" to state.angle.coerceIn(-100.0, 100.0),
        "weaponAngle" to state.weaponAngle.coerceIn(-100.0, 100.0),
        "isFiringPrimary" to state.isFiringPrimary,
        "isAfterburning" to state.isAfterburning,
        "isCloaked" to state.isCloaked,
        "isRespawning" to state.isRespawning,
        "isInStasisBrig" to state.isInStasisBrig,
        "secondarySeq" to state.secondarySeq.coerceIn(0L, 100_000_000L),
        "secondaryType" to safeSecondaryType,
        "secondaryAim" to state.secondaryAim.coerceIn(-100.0, 100.0),
        "energy" to state.energy.coerceIn(0L, 10000L),
        "maxEnergy" to state.maxEnergy.coerceIn(100L, 10000L),
        "bounty" to state.bounty.coerceIn(0L, 100000000L),
        "liveResonance" to state.liveResonance.coerceIn(0L, 1_000_000L),
        "lastPingType" to safePing,
        "lastPingX" to state.lastPingX.coerceIn(-100000.0, 100000.0),
        "lastPingY" to state.lastPingY.coerceIn(-100000.0, 100000.0),
        "updatedAt" to FieldValue.serverTimestamp(),
      )

    suspend fun initializeOrRepairPilotDoc() {
      val existingSnap = runCatching { docRef.get().await() }.getOrNull()
      val preservedCreatedAt =
        if (existingSnap != null && existingSnap.exists()) {
          existingSnap.getTimestamp("createdAt") ?: FieldValue.serverTimestamp()
        } else {
          FieldValue.serverTimestamp()
        }
      val fullPayload =
        updatePayload +
          mapOf(
            "userId" to uid,
            "createdAt" to preservedCreatedAt,
          )
      docRef.set(fullPayload).await()
      knownExistingPilotDocUids.add(uid)
    }

    try {
      if (uid in knownExistingPilotDocUids) {
        try {
          // Single-round-trip fast path for combat & lobby heartbeats
          docRef.update(updatePayload).await()
        } catch (updateErr: Exception) {
          if (updateErr is CancellationException) throw updateErr
          knownExistingPilotDocUids.remove(uid)
          initializeOrRepairPilotDoc()
        }
      } else {
        initializeOrRepairPilotDoc()
      }
      localSeenMsByUid[uid] = System.currentTimeMillis()
      markHeartbeatSuccess()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      if (isAuthenticatedNonGuest()) {
        markSyncError(e)
        handleFirestoreError(e, OperationType.WRITE, docRef.path)
      }
      throw e
    }
  }

  private fun isPilotFreshEnough(
    uid: String,
    updatedAtMs: Long?,
    nowMs: Long,
    initialWindowMs: Long = 90_000L,
  ): Boolean {
    val lastSeenLocal = if (uid.isNotEmpty()) localSeenMsByUid[uid] else null
    if (lastSeenLocal != null) {
      return (nowMs - lastSeenLocal) <= 45_000L
    }
    if (updatedAtMs == null) return true
    val skewSafeDelta = kotlin.math.abs(nowMs - updatedAtMs)
    return skewSafeDelta <= initialWindowMs
  }

  fun observeSectorPilots(sectorId: String): Flow<List<LivePilotState>> {
    val path = "sector_pilots"
    val safeSector = sanitizeMode(sectorId)
    return db.collection(path)
      .whereEqualTo("visibility", "public")
      .whereEqualTo("sectorId", safeSector)
      .snapshots()
      .map { snapshot ->
        markSnapshotSuccess()
        val nowMs = System.currentTimeMillis()
        for (change in snapshot.documentChanges) {
          val docId = change.document.id.trim()
          if (docId.isEmpty()) continue
          when (change.type) {
            DocumentChange.Type.ADDED -> {
              val serverMs = change.document.getTimestamp("updatedAt")?.toDate()?.time
              if (serverMs == null || kotlin.math.abs(nowMs - serverMs) <= 90_000L) {
                localSeenMsByUid[docId] = nowMs
              }
            }
            DocumentChange.Type.MODIFIED -> {
              localSeenMsByUid[docId] = nowMs
            }
            DocumentChange.Type.REMOVED -> {
              localSeenMsByUid.remove(docId)
            }
          }
        }
        snapshot
          .toObjects(
            LivePilotState::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
          )
          .filter { state ->
            val cleanUid = state.userId.ifBlank { state.id }.trim()
            val updatedAtMs = state.updatedAt?.toDate()?.time
            isPilotFreshEnough(cleanUid, updatedAtMs, nowMs, initialWindowMs = 90_000L)
          }
          .sortedWith(
            compareByDescending<LivePilotState> { it.liveResonance }
              .thenByDescending { it.bounty }
          )
      }
      .catch { error ->
        val firestoreError =
          generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && isAuthenticatedNonGuest()) {
          markSyncError(exceptionToLog)
          handleFirestoreError(exceptionToLog, OperationType.LIST, path)
        }
        throw error
      }
  }

  fun observeAllOnlineHumanPilots(): Flow<List<LivePilotState>> {
    val path = "sector_pilots"
    return db.collection(path)
      .whereEqualTo("visibility", "public")
      .snapshots()
      .map { snapshot ->
        markSnapshotSuccess()
        val nowMs = System.currentTimeMillis()
        for (change in snapshot.documentChanges) {
          val docId = change.document.id.trim()
          if (docId.isEmpty()) continue
          when (change.type) {
            DocumentChange.Type.ADDED -> {
              val serverMs = change.document.getTimestamp("updatedAt")?.toDate()?.time
              if (serverMs == null || kotlin.math.abs(nowMs - serverMs) <= 90_000L) {
                localSeenMsByUid[docId] = nowMs
              }
            }
            DocumentChange.Type.MODIFIED -> {
              localSeenMsByUid[docId] = nowMs
            }
            DocumentChange.Type.REMOVED -> {
              localSeenMsByUid.remove(docId)
            }
          }
        }
        snapshot
          .toObjects(
            LivePilotState::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
          )
          .filter { state ->
            val cleanUid = state.userId.ifBlank { state.id }.trim()
            val cleanTag = state.syndicateTag.trim().uppercase(Locale.US)
            val updatedAtMs = state.updatedAt?.toDate()?.time
            cleanUid.isNotEmpty() &&
              !cleanUid.startsWith("dev_", ignoreCase = true) &&
              !cleanUid.startsWith("bot_", ignoreCase = true) &&
              cleanTag != "BOT" &&
              !state.callsign.contains("[BOT]", ignoreCase = true) &&
              isPilotFreshEnough(cleanUid, updatedAtMs, nowMs, initialWindowMs = 90_000L)
          }
          .sortedWith(
            compareByDescending<LivePilotState> { it.liveResonance }
              .thenByDescending { it.bounty }
          )
      }
      .catch { error ->
        val firestoreError =
          generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && isAuthenticatedNonGuest()) {
          markSyncError(exceptionToLog)
          handleFirestoreError(exceptionToLog, OperationType.LIST, path)
        }
        throw error
      }
  }

  suspend fun postSectorCombatEvent(event: SectorCombatEvent): Result<Unit> = runCatching {
    val uid = requireUserId()
    val targetClean = event.targetUid.trim().take(128)
    if (targetClean.isEmpty() || targetClean == uid) return@runCatching
    val now = event.createdAtMs.takeIf { it > 0L } ?: System.currentTimeMillis()
    val rawId = event.id.trim().replace(Regex("[^a-zA-Z0-9_\\-]"), "")
    val docId = rawId.ifEmpty { "ce_${uid.take(10)}_${now}_${(100..999).random()}" }
    val docRef = db.collection("sector_combat_events").document(docId)
    val safeType = if (event.eventType == "ELIMINATED") "ELIMINATED" else "HIT"
    val payload =
      mapOf(
        "senderUid" to uid,
        "targetUid" to targetClean,
        "sectorId" to sanitizeMode(event.sectorId),
        "eventType" to safeType,
        "damage" to event.damage.coerceIn(0L, 25000L),
        "bountyAward" to event.bountyAward.coerceIn(0L, 100_000_000L),
        "weaponLabel" to event.weaponLabel.trim().take(48),
        "senderCallsign" to sanitizeCallsign(event.senderCallsign),
        "senderTag" to sanitizeArmadaTag(event.senderTag).take(12),
        "senderShipId" to event.senderShipId.coerceIn(1L, 10L),
        "createdAtMs" to now,
        "createdAt" to FieldValue.serverTimestamp(),
      )
    try {
      docRef.set(payload).await()
      markHeartbeatSuccess()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      if (isAuthenticatedNonGuest()) {
        markSyncError(e)
        handleFirestoreError(e, OperationType.WRITE, docRef.path)
      }
      throw e
    }
  }

  fun observeSectorCombatEvents(sectorId: String): Flow<List<SectorCombatEvent>> {
    val path = "sector_combat_events"
    val safeSector = sanitizeMode(sectorId)
    val subscribedLocalMs = System.currentTimeMillis()
    val seenFreshEventIds = ConcurrentHashMap.newKeySet<String>()
    return db.collection(path)
      .whereEqualTo("sectorId", safeSector)
      .snapshots()
      .map { snapshot ->
        markSnapshotSuccess()
        val nowMs = System.currentTimeMillis()
        for (change in snapshot.documentChanges) {
          val id = change.document.id
          if (change.type == DocumentChange.Type.ADDED) {
            val serverMs = change.document.getTimestamp("createdAt")?.toDate()?.time
            val clientMs = change.document.getLong("createdAtMs") ?: nowMs
            val freshByServer = serverMs == null || (nowMs - serverMs) <= 10_000L
            val freshBySession = clientMs >= (subscribedLocalMs - 6_000L) || kotlin.math.abs(nowMs - clientMs) <= 10_000L
            if (freshByServer && freshBySession) {
              seenFreshEventIds.add(id)
            }
          }
        }
        snapshot
          .toObjects(
            SectorCombatEvent::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
          )
          .filter { evt -> evt.id in seenFreshEventIds }
          .sortedBy { it.createdAt?.toDate()?.time ?: it.createdAtMs }
          .takeLast(48)
      }
      .catch { error ->
        val firestoreError =
          generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && isAuthenticatedNonGuest()) {
          markSyncError(exceptionToLog)
          handleFirestoreError(exceptionToLog, OperationType.LIST, path)
        }
        throw error
      }
  }

  suspend fun saveSectorInstanceState(state: SectorInstanceState): Result<Unit> = runCatching {
    val uid = requireUserId()
    val safeSector = sanitizeMode(state.sectorId)
    if (safeSector == "NEXUS_LOBBY") return@runCatching
    val docRef = db.collection("sector_instances").document(safeSector)
    val payload =
      mapOf(
        "sectorId" to safeSector,
        "hostUid" to uid,
        "fluxCarrierUid" to state.fluxCarrierUid.trim().take(128),
        "fluxOrbX" to state.fluxOrbX.coerceIn(-100000.0, 100000.0),
        "fluxOrbY" to state.fluxOrbY.coerceIn(-100000.0, 100000.0),
        "fluxOrbVx" to state.fluxOrbVx.coerceIn(-5000.0, 5000.0),
        "fluxOrbVy" to state.fluxOrbVy.coerceIn(-5000.0, 5000.0),
        "fluxTier" to state.fluxTier.coerceIn(0L, 10L),
        "fluxHoldSec" to state.fluxHoldSec.coerceIn(0.0, 3600.0),
        "rcWestScore" to state.rcWestScore.coerceIn(0.0, 1000.0),
        "rcEastScore" to state.rcEastScore.coerceIn(0.0, 1000.0),
        "rcTimerSec" to state.rcTimerSec.coerceIn(0.0, 600.0),
        "rcAlphaCtrl" to state.rcAlphaCtrl.coerceIn(-30.0, 30.0),
        "rcAlphaLock" to state.rcAlphaLock.coerceIn(-1L, 2L),
        "rcOmegaCtrl" to state.rcOmegaCtrl.coerceIn(-30.0, 30.0),
        "rcOmegaLock" to state.rcOmegaLock.coerceIn(-1L, 2L),
        "sbTimerSec" to state.sbTimerSec.coerceIn(0.0, 600.0),
        "sbWestCaps" to state.sbWestCaps.coerceIn(0L, 10000L),
        "sbEastCaps" to state.sbEastCaps.coerceIn(0L, 10000L),
        "sbWestCoreHp" to state.sbWestCoreHp.coerceIn(0.0, 250000.0),
        "sbEastCoreHp" to state.sbEastCoreHp.coerceIn(0.0, 250000.0),
        "jailedUidsCsv" to state.jailedUidsCsv.take(640),
        "updatedAtMs" to (state.updatedAtMs.takeIf { it > 0L } ?: System.currentTimeMillis()),
        "updatedAt" to FieldValue.serverTimestamp(),
      )
    try {
      docRef.set(payload).await()
      markHeartbeatSuccess()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      if (isAuthenticatedNonGuest()) {
        markSyncError(e)
        handleFirestoreError(e, OperationType.WRITE, docRef.path)
      }
      throw e
    }
  }

  fun observeSectorInstanceState(sectorId: String): Flow<SectorInstanceState?> {
    val safeSector = sanitizeMode(sectorId)
    val path = "sector_instances/$safeSector"
    return db.collection("sector_instances")
      .document(safeSector)
      .snapshots()
      .map { snapshot ->
        markSnapshotSuccess()
        if (snapshot.exists()) {
          snapshot.toObject(
            SectorInstanceState::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
          )
        } else {
          null
        }
      }
      .catch { error ->
        val firestoreError =
          generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && isAuthenticatedNonGuest()) {
          markSyncError(exceptionToLog)
          handleFirestoreError(exceptionToLog, OperationType.GET, path)
        }
        throw error
      }
  }

  suspend fun findRealSwornPilotByCallsign(
    rawCallsign: String,
    excludeUid: String = "",
  ): FriendPilotEntry? {
    if (!isAuthenticatedNonGuest()) return null
    val targetClean =
      rawCallsign
        .trim()
        .uppercase(Locale.US)
        .replace(Regex("[^A-Z0-9_\\-]"), "")
        .take(14)
    if (targetClean.length < 2 || UgcModeration.containsProfanity(targetClean)) return null
    val nowMs = System.currentTimeMillis()

    try {
      val liveSnap = db.collection("sector_pilots").whereEqualTo("visibility", "public").get().await()
      val liveMatch =
        liveSnap
          .toObjects(LivePilotState::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          .firstOrNull { st ->
            val uid = st.userId.ifBlank { st.id }.trim()
            val tag = st.syndicateTag.trim().uppercase(Locale.US)
            uid.isNotEmpty() &&
              uid != excludeUid &&
              !uid.startsWith("dev_", ignoreCase = true) &&
              !uid.startsWith("drifter_", ignoreCase = true) &&
              !uid.startsWith("bot_", ignoreCase = true) &&
              tag != "BOT" &&
              tag != "RFT" &&
              !st.callsign.contains("[BOT]", ignoreCase = true) &&
              st.callsign.trim().equals(targetClean, ignoreCase = true)
          }

      if (liveMatch != null) {
        val uid = liveMatch.userId.ifBlank { liveMatch.id }.trim()
        val updatedMs = liveMatch.updatedAt?.toDate()?.time
        val isOnlineNow =
          (localSeenMsByUid[uid]?.let { (nowMs - it) <= 45_000L } == true) ||
            updatedMs == null ||
            kotlin.math.abs(nowMs - updatedMs) <= 45_000L
        val sectorTitle =
          when {
            liveMatch.sectorId == "SECTOR_CONVERGENCE" || liveMatch.sectorId == "FFA" -> "Sector Convergence"
            liveMatch.sectorId == "SECTOR_CONVERGENCE_2" -> "Sector Convergence #07"
            liveMatch.sectorId == "RIFT_SKIRMISH" -> "Rift Skirmish"
            liveMatch.sectorId == "RIFT_CONTROL" -> "Rift Control"
            liveMatch.sectorId == "FLUX_CORE" -> "Flux Core"
            liveMatch.sectorId == "STASIS_BREACH" -> "Stasis Breach"
            liveMatch.sectorId == "SYNDICATE_WARS" -> "Syndicate Wars"
            liveMatch.sectorId == "NEXUS_LOBBY" -> "NEXUS Command"
            liveMatch.sectorId.startsWith("CUSTOM_") -> "Custom Rift"
            else -> "Sector Orbit"
          }
        return FriendPilotEntry(
          uid = uid,
          callsign = liveMatch.callsign.trim().uppercase(Locale.US),
          syndicateTag = sanitizeSyndicateTag(liveMatch.syndicateTag),
          shipId = liveMatch.shipId.toInt().coerceIn(1, 10),
          statusText = if (isOnlineNow) "Online in $sectorTitle" else "Last seen in $sectorTitle",
          isOnline = isOnlineNow,
          currentArmadaName = sanitizeArmadaTag(liveMatch.armadaName),
          currentArmadaCount = if (liveMatch.armadaName.isNotBlank()) 2 else 1,
          bounty = liveMatch.bounty.toInt().coerceAtLeast(100),
        )
      }
    } catch (e: CancellationException) {
      throw e
    } catch (_: Exception) {}

    try {
      val lbSnap =
        db.collection("leaderboard")
          .whereEqualTo("visibility", "public")
          .get()
          .await()
      val lbMatch =
        lbSnap
          .toObjects(LeaderboardEntry::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          .firstOrNull { entry ->
            val uid = entry.userId.ifBlank { entry.id }.trim()
            val tag = entry.syndicateTag.trim().uppercase(Locale.US)
            uid.isNotEmpty() &&
              uid != excludeUid &&
              !uid.startsWith("dev_", ignoreCase = true) &&
              !uid.startsWith("drifter_", ignoreCase = true) &&
              !uid.startsWith("bot_", ignoreCase = true) &&
              tag != "BOT" &&
              tag != "RFT" &&
              !entry.callsign.contains("[BOT]", ignoreCase = true) &&
              entry.callsign.trim().equals(targetClean, ignoreCase = true)
          }

      if (lbMatch != null) {
        val uid = lbMatch.userId.ifBlank { lbMatch.id }.trim()
        val armada = sanitizeArmadaTag(lbMatch.armadaTag).let { if (it == "SOLO") "" else it }
        return FriendPilotEntry(
          uid = uid,
          callsign = lbMatch.callsign.trim().uppercase(Locale.US),
          syndicateTag = sanitizeSyndicateTag(lbMatch.syndicateTag),
          shipId = lbMatch.shipId.toInt().coerceIn(1, 10),
          statusText = "Sworn Pilot Archive • Peak ${lbMatch.allTimeBounty}",
          isOnline = false,
          currentArmadaName = armada,
          currentArmadaCount = lbMatch.armadaTierSize.coerceIn(1, 5),
          bounty = lbMatch.allTimeBounty.toInt().coerceAtLeast(100),
        )
      }
    } catch (e: CancellationException) {
      throw e
    } catch (_: Exception) {}

    return null
  }

  fun observeCloudSyndicates(): Flow<List<SyndicateInfo>> {
    val path = "syndicates"
    return db.collection(path)
      .whereEqualTo("isCustom", true)
      .snapshots()
      .map { snapshot ->
        markSnapshotSuccess()
        snapshot
          .toObjects(
            SyndicateInfo::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
          )
          .filter { it.isRealPlayerFoundedSyndicate() }
      }
      .catch { error ->
        val firestoreError =
          generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && isAuthenticatedNonGuest()) {
          markSyncError(exceptionToLog)
          handleFirestoreError(exceptionToLog, OperationType.LIST, path)
        }
        throw error
      }
  }

  suspend fun saveCloudSyndicate(syndicate: SyndicateInfo): Result<Unit> = runCatching {
    val uid = requireUserId()
    val cleanTag = sanitizeSyndicateTag(syndicate.tag)
    if (cleanTag.length < 2) {
      throw IllegalArgumentException("Syndicate tag must be at least 2 characters.")
    }
    val docRef = db.collection("syndicates").document(cleanTag)
    try {
      val existingSnap = docRef.get().await()
      if (!existingSnap.exists()) {
        val createPayload =
          mapOf(
            "ownerId" to uid,
            "tag" to cleanTag,
            "name" to syndicate.name.trim().take(36).ifEmpty { "$cleanTag Syndicate" },
            "foundryOrigin" to sanitizeFoundry(syndicate.foundryOrigin),
            "motto" to syndicate.motto.trim().take(64).ifEmpty { "Independent Rift Syndicate" },
            "memberCount" to syndicate.memberCount.coerceAtLeast(1),
            "combinedBounty" to syndicate.combinedBounty.coerceAtLeast(100L),
            "isCustom" to true,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        docRef.set(createPayload).await()
      } else {
        val updatePayload =
          mapOf(
            "name" to syndicate.name.trim().take(36).ifEmpty { "$cleanTag Syndicate" },
            "foundryOrigin" to sanitizeFoundry(syndicate.foundryOrigin),
            "motto" to syndicate.motto.trim().take(64).ifEmpty { "Independent Rift Syndicate" },
            "memberCount" to syndicate.memberCount.coerceAtLeast(1),
            "combinedBounty" to syndicate.combinedBounty.coerceAtLeast(100L),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        docRef.update(updatePayload).await()
      }
      markHeartbeatSuccess()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      if (isAuthenticatedNonGuest()) {
        markSyncError(e)
        handleFirestoreError(e, OperationType.WRITE, docRef.path)
      }
      throw e
    }
  }

  suspend fun saveCloudSyndicateMeta(meta: CloudSyndicateMeta): Boolean {
    if (!isAuthenticatedNonGuest()) return false
    val fs = firestore ?: return false
    val cleanTag = sanitizeSyndicateTag(meta.tag).take(4)
    if (cleanTag.isEmpty() || cleanTag in setOf("RFT", "BOT", "SOLO", "GUEST", "FREE")) return false
    val uid = auth.currentUser?.uid?.trim().orEmpty()
    val payload =
      mapOf(
        "tag" to cleanTag,
        "ownerId" to meta.ownerId.ifBlank { uid },
        "crestJson" to meta.crestJson.take(4096),
        "customChannelsJson" to JSONArray(meta.customChannels.take(5)).toString(),
        "invitedGuestsCsv" to meta.invitedGuests.take(25).joinToString(","),
        "updatedAtMs" to System.currentTimeMillis(),
      )
    return try {
      fs.collection("global_state")
        .document("syndicate_meta_$cleanTag")
        .set(payload)
        .await()
      true
    } catch (e: CancellationException) {
      throw e
    } catch (_: Exception) {
      false
    }
  }

  fun observeCloudSyndicateMeta(tag: String): Flow<CloudSyndicateMeta?> = callbackFlow {
    val cleanTag = sanitizeSyndicateTag(tag).take(4)
    val fs = firestore
    if (fs == null || cleanTag.isEmpty() || cleanTag in setOf("RFT", "BOT", "SOLO", "GUEST", "FREE")) {
      trySend(null)
      close()
      return@callbackFlow
    }
    val reg =
      fs.collection("global_state")
        .document("syndicate_meta_$cleanTag")
        .addSnapshotListener { snapshot, error ->
          if (error != null) {
            return@addSnapshotListener
          }
          if (snapshot != null && snapshot.exists()) {
            val channelsRaw = snapshot.getString("customChannelsJson") ?: "[]"
            val parsedChannels = mutableListOf<String>()
            try {
              val arr = JSONArray(channelsRaw)
              for (i in 0 until arr.length()) {
                val ch = arr.optString(i, "").trim()
                if (ch.isNotEmpty()) parsedChannels.add(ch)
              }
            } catch (_: Exception) {}
            val guestsCsv = snapshot.getString("invitedGuestsCsv") ?: ""
            val guests =
              guestsCsv
                .split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .toSet()
            trySend(
              CloudSyndicateMeta(
                tag = snapshot.getString("tag") ?: cleanTag,
                ownerId = snapshot.getString("ownerId") ?: "",
                crestJson = snapshot.getString("crestJson") ?: "",
                customChannels = parsedChannels.ifEmpty { listOf("#general") },
                invitedGuests = guests,
                updatedAtMs = snapshot.getLong("updatedAtMs") ?: 0L,
              )
            )
          } else {
            trySend(null)
          }
        }
    awaitClose { reg.remove() }
  }

  fun observeSectorComms(): Flow<List<SyndicateChatMessage>> {
    val path = "sector_comms"
    return db.collection(path)
      .whereEqualTo("visibility", "public")
      .snapshots()
      .map { snapshot ->
        markSnapshotSuccess()
        snapshot
          .toObjects(
            SyndicateChatMessage::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
          )
          .sortedBy { it.createdAt?.toDate()?.time ?: it.createdAtMs }
          .takeLast(160)
      }
      .catch { error ->
        val firestoreError =
          generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && isAuthenticatedNonGuest()) {
          markSyncError(exceptionToLog)
          handleFirestoreError(exceptionToLog, OperationType.LIST, path)
        }
        throw error
      }
  }

  suspend fun postSectorCommMessage(msg: SyndicateChatMessage): Result<Unit> = runCatching {
    val uid = requireUserId()
    val rawId = msg.id.trim().replace(Regex("[^a-zA-Z0-9_\\-]"), "")
    val docId = rawId.ifEmpty { "comm_${System.currentTimeMillis()}" }
    val docRef = db.collection("sector_comms").document(docId)
    val safeLabel = msg.timestampLabel.trim().uppercase(Locale.US).take(24).ifEmpty { "#SECTOR-LOUNGE" }
    val safeObjective =
      (if (
        msg.armadaObjective.startsWith("BUILD|", ignoreCase = true) ||
          msg.armadaObjective.startsWith("ROOM|", ignoreCase = true) ||
          msg.armadaObjective.startsWith("BUG|", ignoreCase = true)
      ) {
        msg.armadaObjective.trim()
      } else {
        UgcModeration.sanitizeUgcText(msg.armadaObjective)
      }).take(120)

    val payload =
      mapOf(
        "senderUid" to uid,
        "visibility" to "public",
        "senderCallsign" to sanitizeCallsign(msg.senderCallsign),
        "senderSyndicateTag" to sanitizeSyndicateTag(msg.senderSyndicateTag),
        "timestampLabel" to safeLabel,
        "messageText" to UgcModeration.sanitizeUgcText(msg.messageText).take(240),
        "isArmadaRecruitBanner" to msg.isArmadaRecruitBanner,
        "armadaName" to sanitizeArmadaTag(msg.armadaName).take(12),
        "armadaCurrentSize" to msg.armadaCurrentSize.coerceIn(1, 5),
        "armadaMaxSize" to msg.armadaMaxSize.coerceIn(1, 5),
        "armadaObjective" to safeObjective,
        "requestSent" to msg.requestSent,
        "createdAtMs" to msg.createdAtMs.coerceAtLeast(0L),
        "createdAt" to FieldValue.serverTimestamp(),
      )
    try {
      docRef.set(payload).await()
      markHeartbeatSuccess()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      if (isAuthenticatedNonGuest()) {
        markSyncError(e)
        handleFirestoreError(e, OperationType.WRITE, docRef.path)
      }
      throw e
    }
  }

  suspend fun submitBugReportToCloud(report: SectorBugReport): Result<Unit> = runCatching {
    val now = report.createdAtMs.takeIf { it > 0L } ?: System.currentTimeMillis()
    val rawId = report.id.trim().replace(Regex("[^a-zA-Z0-9_\\-]"), "")
    val docId = rawId.ifEmpty { "bug_$now" }
    val safeDesc = UgcModeration.sanitizeUgcText(report.description).trim().take(220)
    if (safeDesc.isEmpty()) return@runCatching
    val safeCat = report.category.trim().uppercase(Locale.US).take(24).ifEmpty { "PHYSICS / COLLISION" }

    val fs = firestore
    if (fs != null && isAuthenticatedNonGuest()) {
      val uid = requireUserId()
      val reportPayload =
        mapOf(
          "reporterUid" to uid,
          "reporterCallsign" to sanitizeCallsign(report.reporterCallsign),
          "syndicateTag" to sanitizeSyndicateTag(report.syndicateTag),
          "category" to safeCat,
          "description" to safeDesc,
          "buildVersion" to report.buildVersion,
          "activeMode" to report.activeMode.take(32),
          "activeShipId" to report.activeShipId.coerceIn(1, 10),
          "screenDpSummary" to report.screenDpSummary.take(40),
          "createdAtMs" to now,
          "createdAt" to FieldValue.serverTimestamp(),
        )
      try {
        fs.collection("sector_bug_reports").document(docId).set(reportPayload).await()
      } catch (e: CancellationException) {
        throw e
      } catch (_: Exception) {}
    }

    if (isAuthenticatedNonGuest()) {
      val commCard =
        SyndicateChatMessage(
          id = docId,
          senderUid = requireUserId(),
          senderCallsign = report.reporterCallsign,
          senderSyndicateTag = report.syndicateTag,
          timestampLabel = "#BUG-REPORTS",
          messageText = "[$safeCat] $safeDesc",
          isArmadaRecruitBanner = false,
          armadaName = "",
          armadaCurrentSize = 1,
          armadaMaxSize = 5,
          armadaObjective = "BUG|$safeCat|v${report.buildVersion}|${report.activeMode.take(24)}".take(120),
          createdAtMs = now,
        )
      postSectorCommMessage(commCard).getOrThrow()
    }
  }

  suspend fun broadcastCustomGameCalibration(
    customGameId: String,
    authorCallsign: String,
    payloadJson: String,
  ): Boolean {
    if (!isAuthenticatedNonGuest()) return false
    val fs = firestore ?: return false
    val safeGameId = customGameId.trim().take(40).ifEmpty { "custom" }
    val safeAuthor = sanitizeCallsign(authorCallsign)
    val payload =
      mapOf(
        "customGameId" to safeGameId,
        "authorCallsign" to safeAuthor,
        "payloadJson" to payloadJson.take(16384),
        "updatedAtMs" to System.currentTimeMillis(),
      )
    return try {
      fs.collection("global_state")
        .document("custom_game_calibration")
        .set(payload)
        .await()
      true
    } catch (e: CancellationException) {
      throw e
    } catch (_: Exception) {
      false
    }
  }

  fun observeCustomGameCalibration(): Flow<LiveCalibrationBroadcast?> = callbackFlow {
    val fs = firestore
    if (fs == null || !isAuthenticatedNonGuest()) {
      trySend(null)
      close()
      return@callbackFlow
    }
    val reg =
      fs.collection("global_state")
        .document("custom_game_calibration")
        .addSnapshotListener { snapshot, error ->
          if (error != null) {
            return@addSnapshotListener
          }
          if (snapshot != null && snapshot.exists()) {
            val broadcast =
              LiveCalibrationBroadcast(
                customGameId = snapshot.getString("customGameId") ?: "",
                authorCallsign = snapshot.getString("authorCallsign") ?: "",
                payloadJson = snapshot.getString("payloadJson") ?: "",
                updatedAtMs = snapshot.getLong("updatedAtMs") ?: 0L,
              )
            trySend(broadcast)
          } else {
            trySend(null)
          }
        }
    awaitClose { reg.remove() }
  }
}
