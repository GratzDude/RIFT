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

  private fun sanitizeCallsign(raw: String): String {
    val cleaned = raw.trim().replace(Regex("[^a-zA-Z0-9_\\- ]"), "")
    return when {
      cleaned.length < 2 -> "PILOT_01"
      cleaned.length > 24 -> cleaned.take(24)
      else -> cleaned
    }
  }

  private fun sanitizeFoundry(raw: String): String {
    val clean = raw.trim()
    return if (clean.isEmpty()) "Independent" else clean.take(32)
  }

  private fun sanitizeMode(raw: String): String {
    return if (raw in listOf("FFA", "warzone_10v10", "ranked_3v3", "ranked_5v5")) raw else "FFA"
  }

  suspend fun getPilotDossier(userId: String = requireUserId()): Result<PilotDossier?> = runCatching {
    val path = "users/$userId"
    try {
      val snapshot = db.collection("users").document(userId).get().await()
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
    val uid = requireUserId()
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
    val uid = requireUserId()
    val docRef = db.collection("leaderboard").document(uid)
    val safeCallsign = sanitizeCallsign(entry.callsign)
    val safeShipId = entry.shipId.coerceIn(1L, 9L)
    val safeFoundry = sanitizeFoundry(entry.foundry)

    try {
      val existingSnap = docRef.get().await()
      if (!existingSnap.exists()) {
        val createPayload =
          mapOf(
            "userId" to uid,
            "visibility" to "public",
            "callsign" to safeCallsign,
            "shipId" to safeShipId,
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
    val uid = requireUserId()
    val docRef = db.collection("sector_pilots").document(uid)
    val safeCallsign = sanitizeCallsign(state.callsign)
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
            "shipId" to safeShip,
            "freq" to safeFreq,
            "x" to state.x.coerceIn(-100000.0, 100000.0),
            "y" to state.y.coerceIn(-100000.0, 100000.0),
            "vx" to state.vx.coerceIn(-5000.0, 5000.0),
            "vy" to state.vy.coerceIn(-5000.0, 5000.0),
            "angle" to state.angle.coerceIn(-100.0, 100.0),
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
            "shipId" to safeShip,
            "freq" to safeFreq,
            "x" to state.x.coerceIn(-100000.0, 100000.0),
            "y" to state.y.coerceIn(-100000.0, 100000.0),
            "vx" to state.vx.coerceIn(-5000.0, 5000.0),
            "vy" to state.vy.coerceIn(-5000.0, 5000.0),
            "angle" to state.angle.coerceIn(-100.0, 100.0),
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
        snapshot.toObjects(
          LivePilotState::class.java,
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
}
