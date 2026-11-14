package com.example.data

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

@Config(sdk = [36])
@LooperMode(LooperMode.Mode.INSTRUMENTATION_TEST)
class RiftRepositoryRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun saveAndObservePilotDossier_authenticatedOwner_succeeds() = runBlocking {
    val uid = signInTestUser(ALICE_EMAIL)
    val repo = RiftRepository(firestore)

    val saveResult =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        repo.savePilotDossier(
          PilotDossier(
            userId = uid,
            callsign = "VIPER_ACE",
            preferredShipId = 2L,
            preferredFoundry = "Independent",
            preferredMode = "FFA",
            pvpKills = 8L,
            pvpDeaths = 2L,
            pvpBountyPeak = 640L,
            allTimeKills = 25L,
            allTimeDeaths = 5L,
            allTimeBountyPeak = 1100L,
            convergencesWon = 2L,
          )
        )
      }
    assertTrue("Expected dossier save to succeed: ${saveResult.exceptionOrNull()}", saveResult.isSuccess)

    val emitted =
      withTimeout(FLOW_TIMEOUT_MS) {
        repo.observePilotDossier(uid).first { it != null && it.callsign == "VIPER_ACE" }
      }
    assertNotNull(emitted)
    assertEquals(8L, emitted?.pvpKills)
  }

  @Test
  fun getPilotDossier_crossUserAccess_failsWithPermissionDenied() = runBlocking {
    val aliceUid = signInTestUser(ALICE_EMAIL)
    val aliceRepo = RiftRepository(firestore)
    withTimeout(DEFAULT_TIMEOUT_MS) {
      aliceRepo.savePilotDossier(PilotDossier(userId = aliceUid, callsign = "ALICE_ACE")).getOrThrow()
    }

    signInTestUser(BOB_EMAIL)
    val bobRepo = RiftRepository(firestore)
    val result = withTimeout(DEFAULT_TIMEOUT_MS) { bobRepo.getPilotDossier(aliceUid) }
    assertTrue("Expected cross-user read to fail", result.isFailure)
    val ex = result.exceptionOrNull()?.unwrapFirestoreException()
    assertNotNull("Expected FirebaseFirestoreException", ex)
    assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, ex?.code)
  }

  @Test
  fun saveAndObserveLeaderboard_authenticatedUsers_succeeds() = runBlocking {
    val aliceUid = signInTestUser(ALICE_EMAIL)
    val repo = RiftRepository(firestore)

    val saveResult =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        repo.saveLeaderboardEntry(
          LeaderboardEntry(
            userId = aliceUid,
            callsign = "ALICE_TOP",
            shipId = 4L,
            foundry = "Independent",
            pvpKills = 12L,
            pvpDeaths = 3L,
            pvpBounty = 950L,
            allTimeKills = 40L,
            allTimeDeaths = 8L,
            allTimeBounty = 1800L,
            convergencesWon = 3L,
          )
        )
      }
    assertTrue("Expected leaderboard save to succeed: ${saveResult.exceptionOrNull()}", saveResult.isSuccess)

    val entries =
      withTimeout(FLOW_TIMEOUT_MS) {
        repo.observeLeaderboard().first { list -> list.any { it.id == aliceUid } }
      }
    assertTrue(entries.any { it.callsign == "ALICE_TOP" && it.pvpKills == 12L })
  }

  @Test
  fun observeLeaderboard_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
    auth.signOut()
    val repo = RiftRepository(firestore)

    try {
      withTimeout(FLOW_TIMEOUT_MS) { repo.observeLeaderboard().first() }
      fail("Expected unauthenticated leaderboard query to fail")
    } catch (e: Exception) {
      val firestoreEx = e.unwrapFirestoreException()
      assertNotNull("Expected nested FirebaseFirestoreException, got $e", firestoreEx)
      assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)
    }
  }

  @Test
  fun saveAndObserveLivePilotState_authenticatedOwner_succeeds() = runBlocking {
    val aliceUid = signInTestUser(ALICE_EMAIL)
    val repo = RiftRepository(firestore)

    val saveResult =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        repo.saveLivePilotState(
          LivePilotState(
            userId = aliceUid,
            sectorId = "SECTOR_CONVERGENCE",
            callsign = "ALICE_ACE",
            syndicateTag = "AEX",
            armadaName = "SOLO",
            shipId = 9L,
            freq = 0L,
            x = 2400.0,
            y = 2100.0,
            vx = 180.0,
            vy = -60.0,
            angle = 1.2,
            weaponAngle = 1.2,
            isFiringPrimary = true,
            isAfterburning = false,
            isCloaked = false,
            energy = 2400L,
            maxEnergy = 2400L,
            bounty = 350L,
          )
        )
      }
    assertTrue("Expected live pilot state save to succeed: ${saveResult.exceptionOrNull()}", saveResult.isSuccess)

    val pilots =
      withTimeout(FLOW_TIMEOUT_MS) {
        repo.observeSectorPilots("SECTOR_CONVERGENCE").first { list -> list.any { it.id == aliceUid } }
      }
    assertTrue(pilots.any { it.callsign == "ALICE_ACE" && it.shipId == 9L })
  }

  private companion object {
    const val ALICE_EMAIL = "alice_rift@test.com"
    const val BOB_EMAIL = "bob_rift@test.com"
    const val DEFAULT_TIMEOUT_MS = 15000L
    const val FLOW_TIMEOUT_MS = 10000L
  }
}
