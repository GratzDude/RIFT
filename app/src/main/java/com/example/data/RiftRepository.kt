package com.example.data

import android.content.Context
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class RiftRepository(private val db: FirebaseFirestore) {

  constructor(context: Context) : this(
    FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    )
  )

  private val auth = Firebase.auth

  private fun requireUserId(): String {
    return auth.currentUser?.uid
      ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
  }

  private fun resolveUserId(explicitUserId: String = ""): String {
    return auth.currentUser?.uid
      ?: explicitUserId.trim().takeIf { it.isNotEmpty() }
      ?: requireUserId()
  }

  private fun sanitizeCallsign(raw: String): String {
    val cleaned = raw.trim().replace(Regex("[^a-zA-Z0-9_\\- ]"), "")
    return when {
      cleaned.length < 2 -> "PILOT_01"
      cleaned.length > 24 -> cleaned.take(24)
      else -> cleaned
    }
  }

  private fun sanitizeTag(raw: String): String {
    return raw.trim().uppercase().replace(Regex("[^A-Z0-9_]"), "").take(12)
  }

  private fun sanitizeFoundry(raw: String): String {
    val clean = raw.trim()
    return if (clean.isEmpty()) "Independent" else clean.take(32)
  }

  private fun sanitizeMode(raw: String): String {
    return if (raw in listOf("FFA", "warzone_10v10", "ranked_3v3", "ranked_5v5")) raw else "FFA"
  }

  suspend fun getPilotDossier(userId: String = requireUserId()): Result<PilotDossier?> = runCatching {
    val resolvedUid = resolveUserId(userId)
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
      handleFirestoreError(e, OperationType.GET, path)
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
        if (exceptionToLog != null && auth.currentUser?.uid == userId) {
          handleFirestoreError(exceptionToLog, OperationType.GET, path)
        }
        throw error
      }
  }

  suspend fun savePilotDossier(dossier: PilotDossier): Result<Unit> = runCatching {
    val uid = resolveUserId(dossier.userId)
    val docRef = db.collection("users").document(uid)
    val safeCallsign = sanitizeCallsign(dossier.callsign)
    val safeShipId = dossier.preferredShipId.coerceIn(1L, 9L)
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
        val updatePayload =
          mapOf(
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
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        docRef.update(updatePayload).await()
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docRef.path)
      throw e
    }
  }

  suspend fun saveLeaderboardEntry(entry: LeaderboardEntry): Result<Unit> = runCatching {
    val uid = resolveUserId(entry.userId)
    val docRef = db.collection("leaderboard").document(uid)
    val safeCallsign = sanitizeCallsign(entry.callsign)
    val safeShipId = entry.shipId.coerceIn(1L, 9L)
    val safeFoundry = sanitizeFoundry(entry.foundry)
    val safeSyndicateTag = sanitizeTag(entry.syndicateTag)
    val safeArmadaTag = sanitizeTag(entry.armadaTag).ifEmpty { "SOLO" }
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
        val updatePayload =
          mapOf(
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
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        docRef.update(updatePayload).await()
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docRef.path)
      throw e
    }
  }

  fun observeLeaderboard(): Flow<List<LeaderboardEntry>> {
    val path = "leaderboard"
    return db.collection(path)
      .whereEqualTo("visibility", "public")
      .snapshots()
      .map { snapshot ->
        snapshot.toObjects(
          LeaderboardEntry::class.java,
          DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
        )
      }
      .catch { error ->
        val firestoreError =
          generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser != null) {
          handleFirestoreError(exceptionToLog, OperationType.LIST, path)
        }
        throw error
      }
  }

  suspend fun saveLivePilotState(state: LivePilotState): Result<Unit> = runCatching {
    val uid = resolveUserId(state.userId)
    val docRef = db.collection("sector_pilots").document(uid)
    val safeCallsign = sanitizeCallsign(state.callsign)
    val safeSyndicateTag = sanitizeTag(state.syndicateTag)
    val safeArmadaName = sanitizeTag(state.armadaName)
    val safeSector = sanitizeMode(state.sectorId)
    val safeShip = state.shipId.coerceIn(1L, 9L)
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
      handleFirestoreError(e, OperationType.WRITE, docRef.path)
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
        if (exceptionToLog != null && auth.currentUser != null) {
          handleFirestoreError(exceptionToLog, OperationType.LIST, path)
        }
        throw error
      }
  }

  fun observeCloudSyndicates(): Flow<List<SyndicateInfo>> {
    val path = "syndicates"
    return db.collection(path)
      .snapshots()
      .map { snapshot ->
        snapshot.toObjects(
          SyndicateInfo::class.java,
          DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
        )
      }
      .catch { error ->
        val firestoreError =
          generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser != null) {
          handleFirestoreError(exceptionToLog, OperationType.LIST, path)
        }
        throw error
      }
  }

  suspend fun saveCloudSyndicate(syndicate: SyndicateInfo): Result<Unit> = runCatching {
    val cleanTag = sanitizeTag(syndicate.tag).take(4)
    if (cleanTag.length < 2) {
      throw IllegalArgumentException("Syndicate tag must be at least 2 characters.")
    }
    val docRef = db.collection("syndicates").document(cleanTag)
    val payload =
      mapOf(
        "tag" to cleanTag,
        "name" to syndicate.name.trim().take(36).ifEmpty { "$cleanTag Syndicate" },
        "foundryOrigin" to sanitizeFoundry(syndicate.foundryOrigin),
        "motto" to syndicate.motto.trim().take(64).ifEmpty { "Independent Rift Syndicate" },
        "memberCount" to syndicate.memberCount.coerceAtLeast(1),
        "combinedBounty" to syndicate.combinedBounty.coerceAtLeast(100L),
        "isCustom" to true,
        "updatedAt" to FieldValue.serverTimestamp(),
      )
    try {
      docRef.set(payload).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docRef.path)
      throw e
    }
  }

  fun observeSectorComms(): Flow<List<SyndicateChatMessage>> {
    val path = "sector_comms"
    return db.collection(path)
      .snapshots()
      .map { snapshot ->
        snapshot
          .toObjects(
            SyndicateChatMessage::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
          )
          .sortedBy { it.createdAt?.toDate()?.time ?: it.createdAtMs }
          .takeLast(40)
      }
      .catch { error ->
        val firestoreError =
          generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser != null) {
          handleFirestoreError(exceptionToLog, OperationType.LIST, path)
        }
        throw error
      }
  }

  suspend fun postSectorCommMessage(msg: SyndicateChatMessage): Result<Unit> = runCatching {
    val docId = msg.id.trim().ifEmpty { "comm_${System.currentTimeMillis()}" }
    val docRef = db.collection("sector_comms").document(docId)
    val payload =
      mapOf(
        "senderUid" to msg.senderUid.trim().ifEmpty { resolveUserId("") },
        "senderCallsign" to sanitizeCallsign(msg.senderCallsign),
        "senderSyndicateTag" to sanitizeTag(msg.senderSyndicateTag),
        "timestampLabel" to msg.timestampLabel.trim().take(24).ifEmpty { "JUST NOW" },
        "messageText" to UgcModeration.sanitizeUgcText(msg.messageText).take(240),
        "isArmadaRecruitBanner" to msg.isArmadaRecruitBanner,
        "armadaName" to sanitizeTag(msg.armadaName),
        "armadaCurrentSize" to msg.armadaCurrentSize.coerceIn(1, 5),
        "armadaMaxSize" to msg.armadaMaxSize.coerceIn(1, 5),
        "armadaObjective" to UgcModeration.sanitizeUgcText(msg.armadaObjective).take(120),
        "requestSent" to msg.requestSent,
        "createdAtMs" to msg.createdAtMs,
        "createdAt" to FieldValue.serverTimestamp(),
      )
    try {
      docRef.set(payload).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docRef.path)
      throw e
    }
  }
}
