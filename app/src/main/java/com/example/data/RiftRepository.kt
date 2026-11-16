package com.example.data

import android.content.Context
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.MemoryCacheSettings
import com.google.firebase.firestore.snapshots
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import org.json.JSONArray

typealias LiveCalibrationBroadcast = RiftRepository.LiveCalibrationBroadcast

class RiftRepository(private val dbProvider: () -> FirebaseFirestore) {

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

  private fun sanitizeMode(raw: String): String {
    val allowed =
      setOf(
        "FFA",
        "SECTOR_CONVERGENCE",
        "RIFT_SKIRMISH",
        "FLUX_CORE",
        "STASIS_BREACH",
        "SYNDICATE_WARS",
        "warzone_10v10",
        "ranked_3v3",
        "ranked_5v5",
      )
    return if (raw in allowed) raw else "FFA"
  }

  suspend fun getPilotDossier(userId: String = requireUserId()): Result<PilotDossier?> = runCatching {
    val callerUid = requireUserId()
    val resolvedUid = userId.trim().ifEmpty { callerUid }
    if (resolvedUid.startsWith("dev_")) {
      return@runCatching null
    }
    val path = "users/$resolvedUid"
    try {
      val snapshot = db.collection("users").document(resolvedUid).get().await()
      if (snapshot.exists()) {
        snapshot.toObject(PilotDossier::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
      } else {
        null
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      if (isAuthenticatedNonGuest()) {
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
          handleFirestoreError(exceptionToLog, OperationType.GET, path)
        }
        throw error
      }
  }

  suspend fun savePilotDossier(dossier: PilotDossier): Result<Unit> = runCatching {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid)
    val safeCallsign = sanitizeCallsign(dossier.callsign)
    val safeShipId = dossier.preferredShipId.coerceIn(1L, 10L)
    val safeFoundry = sanitizeFoundry(dossier.preferredFoundry)
    val safeMode = sanitizeMode(dossier.preferredMode)

    try {
      val existingSnap = docRef.get().await()
      if (!existingSnap.exists()) {
        val createPayload =
          mapOf(
            "userId" to uid,
            "callsign" to safeCallsign,
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
            "createdAt" to FieldValue.serverTimestamp(),
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
        val updatePayload =
          mapOf(
            "callsign" to mergedCallsign,
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
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      if (isAuthenticatedNonGuest()) {
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
      if (!existingSnap.exists()) {
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
            "createdAt" to FieldValue.serverTimestamp(),
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
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      if (isAuthenticatedNonGuest()) {
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
    val safePing =
      if (state.lastPingType in listOf("NONE", "ATTACK", "DEFEND", "CONVERGENCE")) {
        state.lastPingType
      } else {
        "NONE"
      }

    try {
      val existingSnap = docRef.get().await()
      if (!existingSnap.exists()) {
        val createPayload =
          mapOf(
            "userId" to uid,
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
            "energy" to state.energy.coerceIn(0L, 10000L),
            "maxEnergy" to state.maxEnergy.coerceIn(100L, 10000L),
            "bounty" to state.bounty.coerceIn(0L, 100000000L),
            "lastPingType" to safePing,
            "lastPingX" to state.lastPingX.coerceIn(-100000.0, 100000.0),
            "lastPingY" to state.lastPingY.coerceIn(-100000.0, 100000.0),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        docRef.set(createPayload).await()
      } else {
        val updatePayload =
          mapOf(
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
            "energy" to state.energy.coerceIn(0L, 10000L),
            "maxEnergy" to state.maxEnergy.coerceIn(100L, 10000L),
            "bounty" to state.bounty.coerceIn(0L, 100000000L),
            "lastPingType" to safePing,
            "lastPingX" to state.lastPingX.coerceIn(-100000.0, 100000.0),
            "lastPingY" to state.lastPingY.coerceIn(-100000.0, 100000.0),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        docRef.update(updatePayload).await()
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      if (isAuthenticatedNonGuest()) {
        handleFirestoreError(e, OperationType.WRITE, docRef.path)
      }
      throw e
    }
  }

  fun observeSectorPilots(sectorId: String): Flow<List<LivePilotState>> {
    val path = "sector_pilots"
    val safeSector = sanitizeMode(sectorId)
    return db.collection(path)
      .whereEqualTo("sectorId", safeSector)
      .snapshots()
      .map { snapshot ->
        val nowMs = System.currentTimeMillis()
        snapshot
          .toObjects(
            LivePilotState::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
          )
          .filter { state ->
            val updatedAtMs = state.updatedAt?.toDate()?.time
            updatedAtMs == null || (nowMs - updatedAtMs) <= 60_000L
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
          handleFirestoreError(exceptionToLog, OperationType.LIST, path)
        }
        throw error
      }
  }

  fun observeAllOnlineHumanPilots(): Flow<List<LivePilotState>> {
    val path = "sector_pilots"
    return db.collection(path)
      .snapshots()
      .map { snapshot ->
        val nowMs = System.currentTimeMillis()
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
              (updatedAtMs == null || (nowMs - updatedAtMs) <= 45_000L)
          }
          .sortedByDescending { it.bounty }
      }
      .catch { error ->
        val firestoreError =
          generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && isAuthenticatedNonGuest()) {
          handleFirestoreError(exceptionToLog, OperationType.LIST, path)
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
      val liveSnap = db.collection("sector_pilots").get().await()
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
        val isOnlineNow = updatedMs == null || (nowMs - updatedMs) <= 45_000L
        val sectorTitle =
          when (liveMatch.sectorId) {
            "SECTOR_CONVERGENCE", "FFA" -> "Sector Convergence"
            "RIFT_SKIRMISH" -> "Rift Skirmish"
            "FLUX_CORE" -> "Flux Core"
            "STASIS_BREACH" -> "Stasis Breach"
            "SYNDICATE_WARS" -> "Syndicate Wars"
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
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      if (isAuthenticatedNonGuest()) {
        handleFirestoreError(e, OperationType.WRITE, docRef.path)
      }
      throw e
    }
  }

  suspend fun saveCloudSyndicateMeta(meta: CloudSyndicateMeta): Boolean {
    if (!isAuthenticatedNonGuest()) return false
    val fs = firestore ?: return false
    val cleanTag = sanitizeSyndicateTag(meta.tag).take(3)
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
    val cleanTag = sanitizeSyndicateTag(tag).take(3)
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
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      if (isAuthenticatedNonGuest()) {
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
    if (fs != null) {
      val reportPayload =
        mapOf(
          "reporterUid" to report.reporterUid.take(64),
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

      try {
        fs.collection("global_state").document("latest_bug_$docId").set(reportPayload).await()
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
    val fs = firestore ?: return false
    val safeGameId = customGameId.trim().ifEmpty { "custom" }
    val safeAuthor = sanitizeCallsign(authorCallsign)
    val payload =
      mapOf(
        "customGameId" to safeGameId,
        "authorCallsign" to safeAuthor,
        "payloadJson" to payloadJson,
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
    if (fs == null) {
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
