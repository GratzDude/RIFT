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
    engine.walls.clear()
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
    // Only primary gun cost is deducted; EMP Pulse itself costs 0 Flux
    assertEquals(240f - player.spec.primaryCost, player.flux, 0.5f)
    assertTrue(engine.shockwaves.size > shockwavesBefore)
    assertTrue("Enemy Flux should drop by 25% Max Flux", enemy.flux < enemyStartFlux)
    assertTrue("Enemy should receive 2.0s EMP recharge lock", enemy.empTimer >= 1.9f)
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

    // Tick enough 36 Flux/s cloak drain (3.6 Flux over 0.10s) to cross <= 150 Flux threshold
    engine.stepSimulation(0.10f)
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
  fun `eclipse ship 9 has 100 point parity and deploys two-way containment bubble`() {
    val eclipse = RiftCodex.getShipById(9)
    assertEquals("Eclipse", eclipse.name)
    assertEquals("INTERDICTOR_DOME", eclipse.bombType)
    assertEquals(320f, eclipse.bombCost, 0.1f)
    assertEquals(8.5f, eclipse.bombCooldown, 0.1f)
    assertEquals(6.0f, RiftCalibrationState.interdictorBubbleDuration, 0.1f)
    assertEquals(275f, RiftCalibrationState.interdictorBubbleRadius, 0.1f)
    assertEquals(
      100,
      eclipse.speedPts +
        eclipse.thrustPts +
        eclipse.turnPts +
        eclipse.maxFluxPts +
        eclipse.rechargePts +
        eclipse.primaryPts +
        eclipse.secondaryPts,
    )
    assertEquals(100, eclipse.totalAllocatedPoints)

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

    // Deploy Eclipse Stasis Bubble at (2000, 2000)
    engine.fireSecondaryBomb(player, 0f)
    assertEquals(1, engine.interdictorBubbles.size)
    val bubble = engine.interdictorBubbles.first()
    assertEquals(275f, bubble.radius, 0.1f)
    assertEquals(6.0f, bubble.life, 0.1f)

    // 1. Inside ship attempting to cross outward beyond (radius - 36f = 239f) is clamped inside and reflected
    player.x = 2235f
    player.y = 2000f
    player.vx = 400f
    player.vy = 0f
    engine.stepSimulation(0.05f)
    val distInsideAfter = hypot(player.x - bubble.x, player.y - bubble.y)
    assertTrue("Ship inside dome must not escape perimeter", distInsideAfter <= bubble.radius - 35.5f)
    assertTrue("Ship velocity must reflect inward", player.vx < 0f)

    // 2. Outside ship attempting to enter inward within (radius + 36f = 311f) is clamped outside and reflected
    enemy.respawnTimer = 0f
    enemy.x = 2315f
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
  fun `stasis breach captures eliminated ships and prisoner core breakout releases all prisoners`() {
    engine.deployToRankedMode(com.example.engine.RankedGameMode.STASIS_BREACH, teamSizeBracket = 10)
    assertEquals(8000f, engine.westBrigGeneratorHp, 0.1f)
    assertEquals(8000f, engine.eastBrigGeneratorHp, 0.1f)

    val player = engine.localPlayer // freq = 0 (West Horizon)
    val eastVictim = engine.ships.first { it.freq == 1 }

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

    // Prisoner inside the West Brig destroys the 8,000f West Brig Reactor Core at 100% weapon damage
    engine.damageBrigReactorCore(targetBrigFreq = 0, attacker = eastVictim, rawDamage = 8000f)

    assertFalse("Destroyed Brig Reactor Core must release captured prisoner", eastVictim.isInStasisBrig)
    assertEquals("Brig Reactor Core HP must reset to 8000f after breach", 8000f, engine.westBrigGeneratorHp, 0.1f)
  }

  @Test
  fun `rift control disables safe zone completely and ship lock applies strictly to skirmish and syndicate wars`() {
    engine.deployToRankedMode(com.example.engine.RankedGameMode.RIFT_CONTROL, teamSizeBracket = 5)
    assertFalse("Rift Control must have no active Safe Zone", engine.hasActiveSafeZone())
    assertFalse("Center of arena must not count as Safe Zone in Rift Control", engine.isInsideSafeZone(4000f, 4000f))
    assertFalse("Rift Control must not lock ship selection", engine.doesModeLockShipSelection())

    assertTrue(engine.doesModeLockShipSelection(com.example.engine.RankedGameMode.RIFT_SKIRMISH))
    assertTrue(engine.doesModeLockShipSelection(com.example.engine.RankedGameMode.SYNDICATE_WARS))
    assertFalse(engine.doesModeLockShipSelection(com.example.engine.RankedGameMode.SECTOR_CONVERGENCE))
    assertFalse(engine.doesModeLockShipSelection(com.example.engine.RankedGameMode.FLUX_CORE))
    assertFalse(engine.doesModeLockShipSelection(com.example.engine.RankedGameMode.STASIS_BREACH))

    engine.pilotLevel = 20
    assertEquals("Pilot Level 20 must unlock full 100 customization points", 100, engine.getCustomizationPointsTotal())
  }

  @Test
  fun `canQueueSyndicateWars enforces non guest syndicate membership and same syndicate armada`() {
    engine.localPlayer.syndicateTag = "GUEST"
    engine.activeArmadaSlots.clear()
    assertFalse("GUEST pilot must not be eligible for Syndicate Wars", engine.canQueueSyndicateWars().first)

    engine.localPlayer.syndicateTag = "KRN"
    engine.activeArmadaSlots.clear()
    engine.activeArmadaSlots.add(
      com.example.data.ArmadaMemberSlot(
        uid = "pilot_1",
        callsign = "PILOT_1",
        syndicateTag = "KRN",
        shipId = 1,
        isLeader = true,
      )
    )
    engine.activeArmadaSlots.add(
      com.example.data.ArmadaMemberSlot(
        uid = "pilot_2",
        callsign = "WINGMAN_KRN",
        syndicateTag = "KRN",
        shipId = 3,
        isLeader = false,
      )
    )
    assertTrue("Same-Syndicate Armada must be eligible for Syndicate Wars", engine.canQueueSyndicateWars().first)

    // Add a mixed-Syndicate wingman -> must reject
    engine.activeArmadaSlots.add(
      com.example.data.ArmadaMemberSlot(
        uid = "pilot_3",
        callsign = "RIVAL_ZEN",
        syndicateTag = "ZEN",
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
    prefs.recordSyndicateWarOutcome(tag = "KRN", won = true, trophyDelta = 35)

    engine.allTimeKills = 42L
    engine.allTimeBountyPeak = 9500L
    engine.localPlayer.bounty = 1800
    engine.localPlayer.syndicateTag = "KRN"
    engine.activeArmadaName = "KRONOS STRIKE"

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

  @Test
  fun `stage5 circular wall-less safe zone and anti-afk apply strictly to sector convergence and flux core`() {
    // Active in SECTOR_CONVERGENCE & FLUX_CORE
    engine.deployToRankedMode(com.example.engine.RankedGameMode.SECTOR_CONVERGENCE)
    assertTrue(engine.hasActiveSafeZone())
    assertTrue(engine.isInsideSafeZone(engine.arenaCenterX, engine.arenaCenterY))
    assertTrue(
      "Safe Zone must be completely wall-less with zero PERMANENT_BARRIER obstacles",
      engine.walls.none { it.style == com.example.engine.SpaceObstacleStyle.PERMANENT_BARRIER },
    )

    engine.deployToRankedMode(com.example.engine.RankedGameMode.FLUX_CORE)
    assertTrue(engine.hasActiveSafeZone())
    assertTrue(engine.isInsideSafeZone(engine.arenaCenterX, engine.arenaCenterY))

    // Disabled in RIFT_CONTROL, STASIS_BREACH, RIFT_SKIRMISH, and SYNDICATE_WARS
    val noSafeModes =
      listOf(
        com.example.engine.RankedGameMode.RIFT_CONTROL,
        com.example.engine.RankedGameMode.STASIS_BREACH,
        com.example.engine.RankedGameMode.RIFT_SKIRMISH,
        com.example.engine.RankedGameMode.SYNDICATE_WARS,
      )
    for (mode in noSafeModes) {
      engine.deployToRankedMode(mode, teamSizeBracket = 5)
      assertFalse("Mode $mode must not have an active Safe Zone", engine.hasActiveSafeZone())
      assertFalse("Arena center must not be Safe Zone in $mode", engine.isInsideSafeZone(engine.arenaCenterX, engine.arenaCenterY))

      // Open-Space Anti-AFK must NEVER trigger outside SECTOR_CONVERGENCE / FLUX_CORE
      val p = engine.localPlayer
      p.respawnTimer = 0f
      p.x = 2200f
      p.y = 2200f
      p.vx = 0f
      p.vy = 0f
      p.playfieldIdleTimer = 25f
      engine.stepSimulation(0.10f)
      assertFalse("Anti-AFK drain must stay disabled in $mode", p.isIdleDraining)
      assertEquals(0f, p.playfieldIdleTimer, 0.01f)
    }
  }

  @Test
  fun `stage5 custom game standardizes 100 starting bounty and scales seeker and swarm drone speed by speedMult`() {
    engine.liveResonance = 1600 // Apex Archon (500 BTY in Ranked)
    engine.deployToRankedMode(com.example.engine.RankedGameMode.SECTOR_CONVERGENCE)
    assertEquals(500, engine.localPlayer.bounty)

    val fastCustom =
      com.example.engine.CustomGameConfig(
        instanceId = "cust_spd_test",
        title = "HYPER SEEKER LAB",
        hostCallsign = "TESTER",
        baseMode = com.example.engine.RankedGameMode.SECTOR_CONVERGENCE,
        botsEnabled = false,
        speedMult = 1.6f,
        damageMult = 1.5f,
      )
    engine.activeCustomGames.add(fastCustom)
    engine.joinCustomGame(fastCustom.instanceId)
    val p = engine.localPlayer
    assertEquals("Custom game must start local pilot at 100 bounty regardless of Resonance", 100, p.bounty)

    p.x = 2000f
    p.y = 2000f
    p.angle = 0f
    p.weaponAngle = 0f
    engine.walls.clear()
    engine.bombs.clear()
    p.specialWeaponType = "SEEKER_TORPEDO"
    p.specialWeaponCharges = 1
    engine.activateUtility("SPECIAL")
    assertEquals(1, engine.bombs.size)
    val seeker = engine.bombs.first()
    val seekerLaunchSpd = hypot(seeker.vx, seeker.vy)
    assertTrue(
      "Seeker launch speed ($seekerLaunchSpd) must scale by custom speedMult 1.6x",
      seekerLaunchSpd >= RiftCalibrationState.seekerSpeed * 1.5f,
    )

    // Verify SWARM_DRONES (buffType == 11) spawns 5 drones scaled by speedMult & damageMult
    engine.swarmDrones.clear()
    p.specialWeaponType = "SWARM_DRONES"
    p.specialWeaponCharges = 1
    engine.activateUtility("SPECIAL")
    assertEquals(5, engine.swarmDrones.size)
    val drone = engine.swarmDrones.first()
    assertTrue("Swarm drone speed must scale with custom speedMult", hypot(drone.vx, drone.vy) > 560f * 1.35f)
    assertEquals(210f * 1.5f, drone.damage, 1.0f)

    // Verify 12-buff crate pool drop weights sum to 100%
    assertEquals(12, RiftCodex.POWERUP_CODEX.size)
    assertEquals(100, RiftCodex.POWERUP_CODEX.sumOf { it.dropRatePercent })
  }

  @Test
  fun `stage5 overlapping rifts merge into 10s unstable rift and detonate for 75 percent max flux plus shatter asteroids`() {
    engine.deployToRankedMode(com.example.engine.RankedGameMode.SECTOR_CONVERGENCE)
    val player = engine.localPlayer
    val victim = engine.ships.first { !it.isLocalPlayer }
    engine.ships.retainAll { it === player || it === victim }
    player.x = 900f
    player.y = 900f

    engine.walls.clear()
    engine.pendingAsteroidRespawns.clear()
    engine.walls.add(
      WallRect(
        x = 2480f,
        y = 2480f,
        w = 164f,
        h = 164f,
        style = com.example.engine.SpaceObstacleStyle.CRYSTAL_ASTEROID,
        hp = 2400f,
        maxHp = 2400f,
      )
    )

    engine.anomalies.clear()
    engine.anomalies.add(
      SpatialAnomalyEntity(
        id = "rift_merge_1",
        type = AnomalyType.PLASMA_RIFT,
        x = 2500f,
        y = 2500f,
        physicalRadius = 210f,
        life = 40f,
        maxLife = 60f,
      )
    )
    engine.anomalies.add(
      SpatialAnomalyEntity(
        id = "rift_merge_2",
        type = AnomalyType.REPULSOR_RIFT,
        x = 2620f,
        y = 2500f,
        physicalRadius = 210f,
        life = 40f,
        maxLife = 60f,
      )
    )

    // Step once -> overlapping non-partnered Rifts fuse into a single 10.0s Unstable Rift
    engine.stepSimulation(0.05f)
    val merged = engine.anomalies.firstOrNull { it.isUnstableMergedRift }
    assertTrue("Overlapping Rifts must merge into an Unstable Rift", merged != null)
    val unstable = merged!!
    assertEquals(10.0f, unstable.maxLife, 0.1f)
    assertTrue(unstable.life in 9.5f..10.0f)

    // Place victim inside blast radius at full Flux and trigger 0.0s Doomsday detonation
    victim.respawnTimer = 0f
    victim.phaseShieldTimer = 0f
    victim.x = unstable.x + 120f
    victim.y = unstable.y
    victim.flux = victim.maxFlux
    val fullFlux = victim.maxFlux

    unstable.life = 0.02f
    engine.stepSimulation(0.05f)

    assertEquals("Doomsday blast must deal 75% of ship Max Flux", fullFlux * 0.25f, victim.flux, fullFlux * 0.04f)
    assertTrue(
      "Doomsday blast must shatter nearby Crystal Asteroids and queue 20s respawn",
      engine.walls.none { it.style == com.example.engine.SpaceObstacleStyle.CRYSTAL_ASTEROID } &&
        engine.pendingAsteroidRespawns.isNotEmpty(),
    )
  }

  @Test
  fun `stage5 max gravity rift pulls obstacles and repulsor inner core triggers supersonic slingshot`() {
    engine.deployToRankedMode(com.example.engine.RankedGameMode.SECTOR_CONVERGENCE)
    val p = engine.localPlayer
    engine.ships.retainAll { it === p }
    engine.walls.clear()
    engine.anomalies.clear()

    val rock =
      WallRect(
        x = 2650f,
        y = 2118f,
        w = 164f,
        h = 164f,
        style = com.example.engine.SpaceObstacleStyle.CRYSTAL_ASTEROID,
      )
    engine.walls.add(rock)
    val startRockCenterX = rock.x + rock.w * 0.5f

    engine.anomalies.add(
      SpatialAnomalyEntity(
        id = "max_grav",
        type = AnomalyType.GRAVITY_RIFT,
        x = 2200f,
        y = 2200f,
        physicalRadius = 420f,
        life = 30f,
        maxLife = 60f,
      )
    )
    p.x = 6200f
    p.y = 6200f
    repeat(10) { engine.stepSimulation(0.05f) }
    val pulledRockCenterX = rock.x + rock.w * 0.5f
    assertTrue("Max-size 420px Gravity Rift must pull nearby obstacle inward", pulledRockCenterX < startRockCenterX - 3f)

    // Test Repulsor inner-core supersonic slingshot (> afterburnerSpeed)
    engine.anomalies.clear()
    engine.anomalies.add(
      SpatialAnomalyEntity(
        id = "rep_sling",
        type = AnomalyType.REPULSOR_RIFT,
        x = 6200f,
        y = 6200f,
        physicalRadius = 220f,
        life = 30f,
        maxLife = 60f,
      )
    )
    p.x = 6230f
    p.y = 6200f
    p.vx = 40f
    p.vy = 0f
    engine.stepSimulation(0.05f)
    val slingSpeed = hypot(p.vx, p.vy)
    assertTrue(
      "Repulsor inner core must launch ship faster than afterburnerSpeed (${p.spec.afterburnerSpeed}), got $slingSpeed",
      slingSpeed > p.spec.afterburnerSpeed * 1.15f,
    )
  }

  @Test
  fun `stage5 destructible crystal asteroids drop crate 10-hit station fires 16-bolt nova and stasis barriers stack`() {
    engine.deployToRankedMode(com.example.engine.RankedGameMode.RIFT_SKIRMISH, teamSizeBracket = 2)
    val p = engine.localPlayer
    engine.ships.retainAll { it === p }
    engine.walls.clear()
    engine.greens.clear()
    engine.bullets.clear()
    engine.brickWalls.clear()
    engine.pendingAsteroidRespawns.clear()

    // 1. Stack two Stasis Barriers at the same coordinates -> single BrickWallEntity with stackCount == 2
    p.x = 1500f
    p.y = 1500f
    p.angle = 0f
    p.specialWeaponType = "STASIS_BARRIER"
    p.specialWeaponCharges = 1
    engine.activateUtility("SPECIAL")
    p.specialWeaponType = "STASIS_BARRIER"
    p.specialWeaponCharges = 1
    engine.activateUtility("SPECIAL")
    assertEquals(1, engine.brickWalls.size)
    assertEquals(2, engine.brickWalls.first().stackCount)

    // 2. Destructible 2,400 HP Crystal Asteroid shatters, drops guaranteed GreenCrate, and queues 20.0s respawn
    engine.walls.add(
      WallRect(
        x = 2200f,
        y = 2200f,
        w = 164f,
        h = 164f,
        style = com.example.engine.SpaceObstacleStyle.CRYSTAL_ASTEROID,
        hp = 200f,
        maxHp = 2400f,
      )
    )
    engine.bullets.add(
      com.example.engine.BulletEntity(
        ownerId = p.id,
        ownerCallsign = p.callsign,
        ownerSyndicateTag = p.syndicateTag,
        ownerArmadaName = "",
        ownerIsHuman = true,
        freq = p.freq,
        x = 2180f,
        y = 2282f,
        vx = 1200f,
        vy = 0f,
        damage = 260f,
        bouncesLeft = 0,
        life = 1.0f,
        color = 0xFF00F5D4.toInt(),
      )
    )
    engine.stepSimulation(0.05f)
    assertTrue("Crystal Asteroid reduced to <= 0 HP must shatter", engine.walls.isEmpty())
    assertTrue(
      "Shattered Crystal Asteroid must drop a guaranteed Supply Crate at its center",
      engine.greens.any { hypot(it.x - 2282f, it.y - 2282f) < 12f },
    )
    assertEquals("Shattered Crystal Asteroid must queue a 20.0s respawn", 1, engine.pendingAsteroidRespawns.size)
    assertTrue(engine.pendingAsteroidRespawns.first().timerSec >= 19.8f)

    // 3. Orbital Station fires a 16-bolt 360-degree Station Defense Nova on the 10th impact
    val station =
      WallRect(
        x = 3200f,
        y = 3200f,
        w = 168f,
        h = 168f,
        style = com.example.engine.SpaceObstacleStyle.ORBITAL_STATION,
      )
    engine.walls.add(station)
    engine.bullets.clear()
    repeat(10) {
      engine.bullets.add(
        com.example.engine.BulletEntity(
          ownerId = p.id,
          ownerCallsign = p.callsign,
          ownerSyndicateTag = p.syndicateTag,
          ownerArmadaName = "",
          ownerIsHuman = true,
          freq = p.freq,
          x = 3180f,
          y = 3284f,
          vx = 1400f,
          vy = 0f,
          damage = 120f,
          bouncesLeft = 0,
          life = 0.5f,
          color = 0xFF00F5D4.toInt(),
        )
      )
      engine.stepSimulation(0.03f)
    }
    val novaBolts = engine.bullets.filter { it.ownerId == "station_defense" }
    assertEquals("10th impact on Orbital Station must unleash 16 radial Defense Nova bolts", 16, novaBolts.size)
    assertEquals(-999, novaBolts.first().freq)
  }
}


