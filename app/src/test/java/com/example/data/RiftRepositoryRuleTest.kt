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
            callsign = "KESTREL_ACE",
            preferredShipId = 2L,
            preferredFoundry = "Kestrel",
            preferredMode = "warzone_10v10",
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
        repo.observePilotDossier(uid).first { it != null && it.callsign == "KESTREL_ACE" }
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
            foundry = "Aurelia",
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

  private companion object {
    const val ALICE_EMAIL = "alice_rift@test.com"
    const val BOB_EMAIL = "bob_rift@test.com"
    const val DEFAULT_TIMEOUT_MS = 15000L
    const val FLOW_TIMEOUT_MS = 10000L
  }
}
