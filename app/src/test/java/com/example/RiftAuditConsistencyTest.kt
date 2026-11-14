package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.LocalPilotPrefs
import com.example.data.UgcModeration
import com.example.engine.AnomalyType
import com.example.engine.RiftCalibrationState
import com.example.engine.RiftCodex
import com.example.engine.RiftGameEngine
import com.example.engine.RiftSoundSynthesizer
import com.example.engine.SpatialAnomalyEntity
import com.example.engine.WallRect
import kotlin.math.hypot
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RiftAuditConsistencyTest {

  private lateinit var context: Context
  private lateinit var engine: RiftGameEngine

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    RiftCalibrationState.resetAll(context = null, engine = null)
    engine = RiftGameEngine(sfx = RiftSoundSynthesizer())
    engine.rebuildRosterForMode("FFA")
    engine.anomalies.clear()
  }

  @After
  fun tearDown() {
    RiftCalibrationState.resetAll(context = null, engine = null)
  }

  @Test
  fun `zero friction coasting persists when left stick is released and only brakes via manual button`() {
    engine.switchLocalPlayerShip(1, forceFromHangar = true)
    val player = engine.localPlayer
    player.x = 2000f
    player.y = 2000f
    player.vx = 420f
    player.vy = -160f

    // Ensure no stick drag and no manual brake: velocity must NOT decay over multiple updates
    engine.leftStickActive = false
    engine.leftStickMag = 0f
    repeat(10) { engine.stepSimulation(0.016f) }
    assertFalse(engine.autoBraking)
    assertEquals(420f, player.vx, 1.0f)
    assertEquals(-160f, player.vy, 1.0f)

    // Engaging manual [BRAKE] slows velocity down cleanly
    engine.engageManualBrake()
    assertTrue(engine.autoBraking)
    repeat(15) { engine.stepSimulation(0.016f) }
    assertTrue(hypot(player.vx, player.vy) < hypot(420f, -160f) * 0.7f)
  }

  @Test
  fun `viper phase dash cancels brake raises speed cap and primes overcharge`() {
    engine.switchLocalPlayerShip(1, forceFromHangar = true)
    val player = engine.localPlayer
    player.x = 2200f
    player.y = 2200f
    player.vx = 250f
    player.vy = 0f
    player.angle = 0f
    player.bombTimer = 0f
    player.flux = player.maxFlux
    engine.engageManualBrake()
    assertTrue(engine.autoBraking)

    engine.fireSecondaryBomb(player, 0f)

    assertFalse(engine.autoBraking)
    assertTrue(player.phaseDashTimer > 0.30f)
    assertTrue(player.phaseShieldTimer > 0.50f)
    assertTrue(player.sniperOverchargeReady)
    val speedAfter = hypot(player.vx, player.vy)
    assertTrue("Phase Dash speed should exceed normal topSpeed", speedAfter > player.spec.topSpeed)

    // Advance physics a few frames while Phase Dash window is active: burst speed must not be clamped to topSpeed
    repeat(5) { engine.stepSimulation(0.016f) }
    assertTrue(hypot(player.vx, player.vy) > player.spec.topSpeed)
  }

  @Test
  fun `disruptor cloak and zero-flux decloak emp work on button tap and primary fire pull`() {
    engine.anomalies.clear()
    engine.switchLocalPlayerShip(6, forceFromHangar = true)
    val player = engine.localPlayer
    player.x = 2000f
    player.y = 2000f
    player.flux = 400f
    assertFalse(player.isCloaked)

    // Enemy within 220px radius
    val enemy = engine.ships.first { !it.isLocalPlayer }
    engine.ships.retainAll { it === player || it === enemy }
    enemy.respawnTimer = 0f
    enemy.x = 2120f
    enemy.y = 2000f
    enemy.flux = enemy.maxFlux
    val enemyStartFlux = enemy.flux

    // Tap to cloak
    engine.fireSecondaryBomb(player, 0f)
    assertTrue(player.isCloaked)

    // Drop Flux below 350 (above 150 floor) and pull Primary Fire trigger -> must still detonate EMP at 0 Flux cost
    player.flux = 240f
    player.gunTimer = 0f
    val shockwavesBefore = engine.shockwaves.size
    engine.firePrimaryGun(player)

    assertFalse(player.isCloaked)
    // Only primary gun cost is deducted; EMP Pulse itself costs 0⚡
    assertEquals(240f - player.spec.primaryCost, player.flux, 0.5f)
    assertTrue(engine.shockwaves.size > shockwavesBefore)
    assertTrue("Enemy Flux should drop by 35% Max Flux", enemy.flux < enemyStartFlux)
    assertTrue("Enemy should receive 2.5s EMP recharge lock", enemy.empTimer >= 2.4f)
  }

  @Test
  fun `disruptor 150 flux safety floor breaks cloak without firing emp`() {
    engine.anomalies.clear()
    engine.switchLocalPlayerShip(6, forceFromHangar = true)
    val player = engine.localPlayer
    player.x = 2000f
    player.y = 2000f
    player.isCloaked = true
    player.flux = 152f

    // Isolate target enemy from unrelated AI combat and set Flux below max to verify recharge stays intact
    val enemy = engine.ships.first { !it.isLocalPlayer }
    engine.ships.retainAll { it === player || it === enemy }
    enemy.respawnTimer = 0f
    enemy.x = 2100f
    enemy.y = 2000f
    enemy.empTimer = 0f
    enemy.gunTimer = 5f
    enemy.bombTimer = 5f
    enemy.flux = enemy.maxFlux * 0.5f
    val enemyStartFlux = enemy.flux

    // Tick enough cloak drain to cross <= 150⚡ threshold
    engine.stepSimulation(0.05f)
    assertFalse(player.isCloaked)
    assertTrue("Forced uncloak must not drain enemy Flux with EMP", enemy.flux >= enemyStartFlux)
    assertTrue("Forced uncloak must not apply EMP recharge lock", enemy.empTimer <= 0.01f)
  }

  @Test
  fun `dynamic buff cap scales by mode override`() {
    RiftCalibrationState.buffTargetCountOverride = null
    RiftCalibrationState.buffModeOverride = "3V3"
    assertEquals(12, engine.getEffectiveBuffCap())
    RiftCalibrationState.buffModeOverride = "5V5"
    assertEquals(10, engine.getEffectiveBuffCap())
    RiftCalibrationState.buffModeOverride = "10V10"
    assertEquals(20, engine.getEffectiveBuffCap())
    RiftCalibrationState.buffModeOverride = "FFA"
    assertEquals(40, engine.getEffectiveBuffCap())
  }

  @Test
  fun `phase rift pair warps aim bounce raycast trajectories between Node A and Node B`() {
    engine.walls.clear()
    engine.walls.add(WallRect(0f, 0f, 8000f, 16f))
    engine.anomalies.clear()
    val nodeA =
      SpatialAnomalyEntity(
        id = "anom_test_a",
        type = AnomalyType.PHASE_RIFT_A,
        x = 2250f,
        y = 2000f,
        physicalRadius = 180f,
        partnerId = "anom_test_b",
      )
    val nodeB =
      SpatialAnomalyEntity(
        id = "anom_test_b",
        type = AnomalyType.PHASE_RIFT_B,
        x = 6000f,
        y = 5500f,
        physicalRadius = 180f,
        partnerId = "anom_test_a",
      )
    engine.anomalies.add(nodeA)
    engine.anomalies.add(nodeB)

    val player = engine.localPlayer
    player.x = 2000f
    player.y = 2000f
    val pts = engine.computeBounceTrajectory(player, angle = 0f, maxBounces = 2, segmentMaxLen = 540f)

    assertTrue(pts.any { it.isWarp && !it.isBounce })
    assertTrue(pts.any { it.isWarp && it.isBounce && it.x == 6000f && it.y == 5500f })
    assertTrue(pts.last().x > 6100f)
  }

  @Test
  fun `ship names codex and ugc moderation adhere to clean rift rules`() {
    val allShips = RiftCodex.getAllTenShips()
    assertEquals(10, allShips.size)
    val legacyPrefix = Regex("^[0-9]+-")
    val forbiddenWords = listOf("Energy", "NRG", "Thor", "Wormhole", "Shotgun", "Missile", "Rocket", "Decoy", "Brick")

    for (s in allShips) {
      assertFalse("Ship ${s.name} must not have numeric prefix", legacyPrefix.containsMatchIn(s.name))
      for (bad in forbiddenWords) {
        assertFalse("Ship title contains $bad", s.primaryTitle.contains(bad, ignoreCase = true))
        assertFalse("Ship secondary contains $bad", s.secondaryTitle.contains(bad, ignoreCase = true))
      }
    }

    assertTrue(UgcModeration.containsProfanity("badshitname"))
    assertFalse(UgcModeration.containsProfanity("APEX_ACE"))
    val masked = UgcModeration.sanitizeUgcText("Good luck fuck that")
    assertFalse(masked.contains("fuck", ignoreCase = true))

    val prefs = LocalPilotPrefs(context)
    prefs.blockPilot("toxic_1")
    assertTrue("toxic_1" in prefs.getBlockedPilotUids())
    assertEquals(1, prefs.reportPilot("toxic_1", "TOXIC_PILOT", "Abusive Comms"))
  }

  @Test
  fun `interdictor ship 9 has 100 point parity and deploys two-way containment bubble`() {
    val interdictor = RiftCodex.getShipById(9)
    assertEquals("Interdictor", interdictor.name)
    assertEquals("INTERDICTOR_DOME", interdictor.bombType)
    assertEquals(
      100,
      interdictor.speedPts +
        interdictor.thrustPts +
        interdictor.turnPts +
        interdictor.maxFluxPts +
        interdictor.rechargePts +
        interdictor.primaryPts +
        interdictor.secondaryPts,
    )
    assertEquals(100, interdictor.totalAllocatedPoints)

    val riftFrame = RiftCodex.getShipById(10)
    assertEquals(10, riftFrame.id)

    engine.walls.clear()
    engine.anomalies.clear()
    engine.interdictorBubbles.clear()
    engine.switchLocalPlayerShip(9, forceFromHangar = true)
    val player = engine.localPlayer
    val enemy = engine.ships.first { !it.isLocalPlayer }
    engine.ships.retainAll { it === player || it === enemy }

    player.x = 2000f
    player.y = 2000f
    player.vx = 0f
    player.vy = 0f
    player.flux = player.maxFlux
    player.bombTimer = 0f

    // Deploy Interdictor Stasis Bubble at (2000, 2000)
    engine.fireSecondaryBomb(player, 0f)
    assertEquals(1, engine.interdictorBubbles.size)
    val bubble = engine.interdictorBubbles.first()
    assertEquals(290f, bubble.radius, 0.1f)
    assertEquals(15.0f, bubble.life, 0.1f)

    // 1. Inside ship attempting to cross outward beyond (radius - 36f = 254f) is clamped inside and reflected
    player.x = 2250f
    player.y = 2000f
    player.vx = 400f
    player.vy = 0f
    engine.stepSimulation(0.05f)
    val distInsideAfter = hypot(player.x - bubble.x, player.y - bubble.y)
    assertTrue("Ship inside dome must not escape perimeter", distInsideAfter <= bubble.radius - 35.5f)
    assertTrue("Ship velocity must reflect inward", player.vx < 0f)

    // 2. Outside ship attempting to enter inward within (radius + 36f = 326f) is clamped outside and reflected
    enemy.respawnTimer = 0f
    enemy.x = 2330f
    enemy.y = 2000f
    enemy.vx = -400f
    enemy.vy = 0f
    engine.stepSimulation(0.05f)
    val distOutsideAfter = hypot(enemy.x - bubble.x, enemy.y - bubble.y)
    assertTrue("Ship outside dome must not breach perimeter", distOutsideAfter >= bubble.radius + 35.5f)
    assertTrue("Outside ship velocity must reflect outward", enemy.vx > 0f)
  }

  @Test
  fun `flux core mode overcharges carrier and ejects at 1650 px per sec`() {
    engine.deployToRankedMode(com.example.engine.RankedGameMode.FLUX_CORE)
    val player = engine.localPlayer
    engine.ships.retainAll { it === player }

    // Position player outside the Safe Zone right on top of the Flux Core Orb
    player.respawnTimer = 0f
    player.x = engine.fluxCoreOrb.x
    player.y = engine.fluxCoreOrb.y
    player.vx = 0f
    player.vy = 0f
    engine.fluxCoreOrb.pickupImmunitySec = 0f

    // Step simulation so player picks up the Flux Core Orb
    engine.stepSimulation(0.05f)
    assertEquals(player.id, engine.fluxCoreCarrierEntityId)
    assertEquals(1, engine.fluxCoreOrb.overchargeTier)

    // Advance holdDurationSec to 25.0s -> Tier 5 (up to Tier 10 at 50s)
    engine.fluxCoreOrb.holdDurationSec = 25.0f
    engine.stepSimulation(0.05f)
    assertEquals(5, engine.fluxCoreOrb.overchargeTier)

    engine.fluxCoreOrb.holdDurationSec = 52.0f
    engine.stepSimulation(0.05f)
    assertEquals(10, engine.fluxCoreOrb.overchargeTier)

    // Entering the Safe Zone ejects the Flux Core at 1650 px/s with a 30.0s deceleration timer
    player.x = engine.safeZone.centerX()
    player.y = engine.safeZone.centerY()
    engine.stepSimulation(0.01f)

    assertEquals("", engine.fluxCoreCarrierEntityId)
    assertEquals(-1, engine.fluxCoreOrb.carrierShipId)
    assertTrue("Eject deceleration timer must be ~30.0s", engine.fluxCoreOrb.ejectDecelTimerSec >= 29.9f)
    val ejectSpeed = hypot(engine.fluxCoreOrb.vx, engine.fluxCoreOrb.vy)
    assertEquals(1650f, ejectSpeed, 25f)
  }

  @Test
  fun `stasis breach captures eliminated ships and reactor core breakout releases all prisoners`() {
    engine.deployToRankedMode(com.example.engine.RankedGameMode.STASIS_BREACH, teamSizeBracket = 10)
    assertEquals(8000f, engine.westBrigGeneratorHp, 0.1f)
    assertEquals(8000f, engine.eastBrigGeneratorHp, 0.1f)

    val player = engine.localPlayer // freq = 0 (West Horizon)
    val eastVictim = engine.ships.first { it.freq == 1 }
    val eastRescuer = engine.ships.last { it.freq == 1 && it !== eastVictim }

    // Eliminate eastVictim outside the Safe Zone using a bullet from player
    eastVictim.respawnTimer = 0f
    eastVictim.phaseShieldTimer = 0f
    eastVictim.isInStasisBrig = false
    eastVictim.x = 2000f
    eastVictim.y = 2000f
    eastVictim.flux = 10f

    engine.bullets.clear()
    engine.bullets.add(
      com.example.engine.BulletEntity(
        ownerId = player.id,
        ownerCallsign = player.callsign,
        ownerSyndicateTag = player.syndicateTag,
        ownerArmadaName = player.armadaName,
        ownerIsHuman = true,
        freq = player.freq,
        x = 2000f,
        y = 2000f,
        vx = 0f,
        vy = 0f,
        damage = 250f,
        bouncesLeft = 0,
        life = 1.0f,
        color = 0xFF00F5D4.toInt(),
      )
    )
    engine.stepSimulation(0.02f)

    assertTrue("Eliminated ship in Stasis Breach must be captured in Stasis Brig", eastVictim.isInStasisBrig)

    // Free teammate (eastRescuer) destroys the West Brig Reactor Core (8000f HP) holding East (freq=1) prisoners
    eastRescuer.isInStasisBrig = false
    engine.damageBrigReactorCore(targetBrigFreq = 0, attacker = eastRescuer, rawDamage = 8000f)

    assertFalse("Destroyed Brig Reactor Core must release captured prisoner", eastVictim.isInStasisBrig)
    assertEquals("Brig Reactor Core HP must reset to 8000f after breach", 8000f, engine.westBrigGeneratorHp, 0.1f)
  }

  @Test
  fun `canQueueSyndicateWars enforces non guest syndicate membership and same syndicate armada`() {
    engine.localPlayer.syndicateTag = "GUEST"
    engine.activeArmadaSlots.clear()
    assertFalse("GUEST pilot must not be eligible for Syndicate Wars", engine.canQueueSyndicateWars().first)

    engine.localPlayer.syndicateTag = "AEX"
    engine.activeArmadaSlots.clear()
    engine.activeArmadaSlots.add(
      com.example.data.ArmadaMemberSlot(
        uid = "pilot_1",
        callsign = "PILOT_1",
        syndicateTag = "AEX",
        shipId = 1,
        isLeader = true,
      )
    )
    engine.activeArmadaSlots.add(
      com.example.data.ArmadaMemberSlot(
        uid = "pilot_2",
        callsign = "WINGMAN_AEX",
        syndicateTag = "AEX",
        shipId = 3,
        isLeader = false,
      )
    )
    assertTrue("All-AEX Armada must be eligible for Syndicate Wars", engine.canQueueSyndicateWars().first)

    // Add a mixed-Syndicate wingman -> must reject
    engine.activeArmadaSlots.add(
      com.example.data.ArmadaMemberSlot(
        uid = "pilot_3",
        callsign = "RIVAL_VTX",
        syndicateTag = "VTX",
        shipId = 5,
        isLeader = false,
      )
    )
    assertFalse("Mixed-Syndicate Armada must not be eligible for Syndicate Wars", engine.canQueueSyndicateWars().first)
  }

  @Test
  fun `performMasterFreshStartWipe resets kills bounties armadas and syndicate tag back to GUEST`() {
    val prefs = LocalPilotPrefs(context)
    prefs.savePilotProgression(
      uid = "test_uid",
      xpFromBuffs = 5000L,
      xpFromDamage = 8000L,
      xpFromAccuracy = 4000L,
      xpFromBountyEarned = 6000L,
      xpFromBountyKilled = 7000L,
      riftShards = 4200L,
      highestMilestoneLevelClaimed = 15,
    )
    prefs.recordSyndicateWarOutcome(tag = "AEX", won = true, trophyDelta = 35)

    engine.allTimeKills = 42L
    engine.allTimeBountyPeak = 9500L
    engine.localPlayer.bounty = 1800
    engine.localPlayer.syndicateTag = "AEX"
    engine.activeArmadaName = "APEX STRIKE"

    engine.performMasterFreshStartWipe(localPrefs = prefs, uid = "test_uid")

    assertEquals(0L, engine.allTimeKills)
    assertEquals(100L, engine.allTimeBountyPeak)
    assertEquals(100, engine.localPlayer.bounty)
    assertEquals("GUEST", engine.localPlayer.syndicateTag)
    assertEquals("", engine.activeArmadaName)
    assertEquals(150L, engine.riftShards)
    assertEquals(0L, prefs.getAllTimeKills())
    assertEquals(150L, prefs.getRiftShards("test_uid"))
  }
}

