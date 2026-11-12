package com.example.engine

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import com.example.data.ArmadaMemberSlot
import com.example.data.FriendPilotEntry
import com.example.data.LivePilotState
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

data class WallRect(val x: Float, val y: Float, val w: Float, val h: Float)

data class GreenCrate(
  var x: Float,
  var y: Float,
  var spinPhase: Float,
  val buffType: Int = 0, // 0=Burst, 1=Shotgun, 2=Missile, 3=Decoy, 4=Brick, 5=Rocket, 6=Full Flux, 7=Repel, 8=Multi-Shot(+25), 9=Bouncing Bullets(+30), 10=Overcharge Surge(+8%)
  var vx: Float = 0f,
  var vy: Float = 0f,
)

data class DecoyEntity(
  val id: String,
  val ownerId: String,
  val callsign: String,
  val syndicateTag: String,
  val armadaName: String,
  val shipId: Int,
  val freq: Int,
  var x: Float,
  var y: Float,
  var vx: Float,
  var vy: Float,
  var angle: Float,
  val bounty: Int,
  var life: Float = 8.0f,
  val maxLife: Float = 8.0f,
)

data class BrickWallEntity(
  val id: String,
  val ownerId: String,
  val freq: Int,
  val x: Float,
  val y: Float,
  val w: Float,
  val h: Float,
  var life: Float = 10.0f,
  val maxLife: Float = 10.0f,
)

data class ShipEntity(
  val id: String,
  var callsign: String,
  var syndicateTag: String,
  var armadaName: String = "", // "" = [SOLO], non-empty = [ARMADA_NAME]
  var shipId: Int,
  var spec: ShipSpec,
  val freq: Int, // 0 = Allied/Friendly coloring, 1 = Rival coloring (dynamically evaluated via areAllied)
  val isHuman: Boolean,
  val isLocalPlayer: Boolean,
  var x: Float,
  var y: Float,
  var vx: Float = 0f,
  var vy: Float = 0f,
  var angle: Float = -PI.toFloat() * 0.5f,
  var weaponAngle: Float = angle,
  var independentAimActive: Boolean = false,
  val radius: Float = 36f,
  var flux: Float = spec.maxFlux,
  var maxFlux: Float = spec.maxFlux,
  var bounty: Int = 100,
  var kills: Int = 0,
  var deaths: Int = 0,
  var gunTimer: Float = 0f,
  var bombTimer: Float = 0f,
  var empTimer: Float = 0f,
  var respawnTimer: Float = 0f,
  var deathX: Float = x,
  var deathY: Float = y,
  var afterburning: Boolean = false,
  var multifireOn: Boolean = false,
  var repels: Int = spec.initialRepels,
  var specialWeaponType: String = "NONE", // "NONE", "BURST", "SHOTGUN", "MISSILE", "DECOY", "BRICK", "ROCKET"
  var specialWeaponCharges: Int = 0,
  var shotgunWavesRemaining: Int = 0,
  var shotgunWaveTimer: Float = 0f,
  var shotgunAimAngle: Float = 0f,
  var multiShotCharges: Int = 0, // Shot-limited Multi-Shot (+25 per pickup, stackable)
  var bouncingBulletsShots: Int = 0, // Shot-limited Bouncing Bullets (+30 per pickup, stackable)
  var overchargeSurgeStacks: Int = 0, // Stacks up to 3x (+8% Speed, Thrust, Recharge per stack until death)
  var sniperOverchargeReady: Boolean = false, // 1-Viper / Phase Dash overcharges next rail shot (+35% vel & dmg)
  var isCloaked: Boolean = false, // 6-Disruptor / Cloak+EMP stealth cloak (180❖/s drain, hides ship & minimap dot)
  var rocketBoostTimer: Float = 0f, // 3.0s Rocket hyper-thruster boost (1.65x max speed at 0❖)
  var wasInSafeZone: Boolean = false,
  var inSafeZone: Boolean = false,
  var weaponsLocked: Boolean = false,
  var phaseShieldTimer: Float = 0f,
  var canFire: Boolean = true,
  var weaponState: String = "ARMED",
  var hitFlashTimer: Float = 0f,
  var unstickTimer: Float = 0f,
  var unstickAngle: Float = 0f,
  var blackHoleImmunityTimer: Float = 0f,
  var wormholeCooldownTimer: Float = 0f,
  var plasmaRiftTickTimer: Float = 0f,
  var missileLockWarningTimer: Float = 0f,
  var missileBeepCooldown: Float = 0f,
  var safeZoneStayTimer: Float = 0f,
  var playfieldIdleTimer: Float = 0f,
  var isIdleDraining: Boolean = false,
  var matchPresenceTimeSec: Float = 0f,
  val aiSeed: Float = Random.nextFloat() * 100f,
)

data class BulletEntity(
  val ownerId: String,
  val ownerCallsign: String,
  val ownerSyndicateTag: String,
  val ownerArmadaName: String,
  val ownerIsHuman: Boolean,
  val freq: Int,
  var x: Float,
  var y: Float,
  var vx: Float,
  var vy: Float,
  val damage: Float,
  var bouncesLeft: Int,
  var life: Float,
  val color: Int,
  val isHeavyBurst: Boolean = false,
  var wormholeCooldownTimer: Float = 0f,
)

data class BombEntity(
  val ownerId: String,
  val ownerCallsign: String,
  val ownerSyndicateTag: String,
  val ownerArmadaName: String,
  val ownerIsHuman: Boolean,
  val freq: Int,
  val type: String, // "BOUNCE", "SIEGE", "EMP", "PROX", "MINE", "MISSILE"
  var x: Float,
  var y: Float,
  var vx: Float,
  var vy: Float,
  val damage: Float,
  var bouncesLeft: Int,
  val passWalls: Boolean,
  var life: Float,
  val radius: Float,
  var wormholeCooldownTimer: Float = 0f,
  var targetShipId: String = "",
)

enum class AnomalyType {
  PLASMA_RIFT,
  REPULSOR_RIFT,
  GRAVITY_RIFT,
  PHASE_RIFT_A,
  PHASE_RIFT_B,
}

data class SpatialAnomalyEntity(
  val id: String,
  val type: AnomalyType,
  var x: Float,
  var y: Float,
  val physicalRadius: Float = 180f, // Exactly 5x standard ship radius (36f * 5 = 180f)
  var life: Float = 45.0f,
  val maxLife: Float = 45.0f,
  var spinAngle: Float = 0f,
  var partnerId: String = "",
)

data class ParticleEntity(
  var x: Float,
  var y: Float,
  var vx: Float,
  var vy: Float,
  var life: Float,
  val maxLife: Float,
  val size: Float,
  val color: Int,
)

data class ShockwaveEntity(
  val x: Float,
  val y: Float,
  var r: Float,
  val maxR: Float,
  val color: Int,
)

data class ShieldRippleEntity(
  val shipId: String,
  val impactAngle: Float,
  var intensity: Float,
  var life: Float,
  val maxLife: Float = 0.28f,
)

data class FloatingPopup(
  var active: Boolean = true,
  var text: String = "",
  var x: Float = 0f,
  var y: Float = 0f,
  var vy: Float = -48f,
  var elapsed: Float = 0f,
  var maxLife: Float = 0.75f,
  var color: Int = Color.parseColor("#F1F5F9"),
  var scale: Float = 1.0f,
)

data class TrajectoryPoint(val x: Float, val y: Float, val isBounce: Boolean)

data class KillFeedEntry(
  val id: Long,
  val killerTag: String,
  val killerName: String,
  val killerIsLocal: Boolean,
  val killerIsAllied: Boolean,
  val victimTag: String,
  val victimName: String,
  val victimIsLocal: Boolean,
  val victimIsAllied: Boolean,
  val victimBounty: Int,
  val bountyAwarded: Int,
  val wasHvt: Boolean,
  var life: Float = 14.0f,
  val maxLife: Float = 14.0f,
)

private data class RaycastHit(
  val t: Float,
  val hitX: Float,
  val hitY: Float,
  val normalX: Float,
  val normalY: Float,
)

class RiftGameEngine(
  val sfx: RiftSoundSynthesizer,
  var onStatsUpdated: () -> Unit = {},
  var onPrizeToast: (String) -> Unit = {},
  var onArmadaTierKill: (tierSize: Int, bounty: Long) -> Unit = { _, _ -> },
) {
  companion object {
    const val WORLD_SIZE = 8000f
    const val CAMERA_SCALE = 0.65f
    const val BASE_CAMERA_SCALE = 0.65f
    const val WALL_ELASTICITY = 0.65f
    const val SAFE_ZONE_BOT_BAN_MARGIN = 400f
    const val HVT_BOUNTY_THRESHOLD = 250

    private val BOT_CALLSIGNS =
      listOf(
        "APEX ACE",
        "SOLARIS IX",
        "IRON WOLF",
        "NEXUS 7",
        "VIPER 09",
        "CHRONO X",
        "VALKYRIE Z",
        "MANTA RAY",
        "SOLARIS PRIME",
        "DARK STAR",
        "ZERO DRIFT",
        "HYPERION",
        "VOID REAPER",
        "PULSE 88",
        "TITAN 04",
        "ECHO V",
        "STAR VIPER",
        "OMEGA WING",
        "RAVEN SIX",
        "CRYO BLADE",
        "PHANTOM RAY",
        "STORM RIDER",
        "COBALT FANG",
        "SOLAR FLARE",
      )
    val LEGACY_FOUNDRY_TAGS = setOf("KSTL", "AURL", "VRTX", "RUST")
    private val RIVAL_ARMADAS = listOf("", "VOID 3", "CRIMSON 4", "", "IRON DUO", "")

    fun cleanTag(rawTag: String): String {
      val t = rawTag.trim().removeSurrounding("[", "]").trim()
      return when {
        t.isEmpty() || t in LEGACY_FOUNDRY_TAGS -> "GUEST"
        else -> t
      }
    }
  }

  val walls = mutableListOf<WallRect>()
  val brickWalls = mutableListOf<BrickWallEntity>()
  val decoys = mutableListOf<DecoyEntity>()
  private var nextDecoyBrickId: Long = 1L
  val safeZone = RectF(3600f, 3600f, 4400f, 4400f)
  val greens = mutableListOf<GreenCrate>()
  val recentBuffSpawnLocations = ArrayDeque<Pair<Float, Float>>()
  val pendingBuffSpawns = mutableListOf<Float>()
  var radarBgBitmap: Bitmap? = null

  fun getEffectiveBuffCap(): Int {
    return (RiftCalibrationState.buffTargetCountOverride ?: RiftCalibrationState.buffCapFfa)
      .coerceAtLeast(0)
  }

  var mode: String = "FFA"
  var teamSize: Int = 20
  var buffVersion: Int = 0

  // Fluid Up-to-5-Man Named Armada State
  var activeArmadaName: String = "" // "" = SOLO
  val activeArmadaSlots = mutableListOf<ArmadaMemberSlot>()

  val ships = mutableListOf<ShipEntity>()
  val bullets = mutableListOf<BulletEntity>()
  val bombs = mutableListOf<BombEntity>()
  val particles = mutableListOf<ParticleEntity>()
  val shockwaves = mutableListOf<ShockwaveEntity>()
  val shieldRipples = mutableListOf<ShieldRippleEntity>()
  val anomalies = mutableListOf<SpatialAnomalyEntity>()
  private var nextAnomalyId: Long = 1L

  // Phase 3.4 Object-Pooled Floating Combat Text (FCT)
  private val fctPool = Array(48) { FloatingPopup(active = false) }
  val floatingPopups = mutableListOf<FloatingPopup>()

  // Running Transparent Fade-Out Kill Feed (Top-Right HUD) & >1,000 Bounty Dual Alert Banner
  private var nextKillFeedId: Long = 1L
  val killFeed = mutableListOf<KillFeedEntry>()
  var highBountyShutdownBanner: String? = null
  var highBountyShutdownTimer: Float = 0f

  private val remotePilots = mutableMapOf<String, ShipEntity>()

  // Control states updated by touch subsystem
  var leftStickActive: Boolean = false
  var leftStickMag: Float = 0f
  var leftStickAngle: Float = 0f
  var autoBraking: Boolean = false

  // Right-Side 360-Degree Independent Weapon Aiming Joystick ([FIRE]) + Cancel Deadzone
  var fireStickActive: Boolean = false
  var fireStickHasAimVector: Boolean = false
  var fireStickAimAngle: Float = 0f
  var isCancelActive: Boolean = false

  var bombStickActive: Boolean = false
  var bombStickMag: Float = 0f
  var bombStickAngle: Float = 0f
  var isBombCancelActive: Boolean = false

  // Phase 3.4 Hit-Stop Freeze & Non-Linear Camera Trauma Shake + Requirement 5 Real-Time Camera Zoom (0.5x..1.5x)
  var hitStopTimer: Float = 0f
  var cameraTrauma: Float = 0f
  var traumaTimeSec: Float = 0f
  var cameraZoom: Float = 1.0f
  var playfieldTextScale: Float = 1.0f

  fun effectiveCameraScale(): Float =
    BASE_CAMERA_SCALE * (cameraZoom * RiftCalibrationState.cameraZoomScale).coerceIn(0.2f, 2.5f)

  // Persistent counters tracked by engine
  var pvpKills: Long = 0L
  var pvpDeaths: Long = 0L
  var pvpBountyPeak: Long = 100L
  var allTimeKills: Long = 0L
  var allTimeDeaths: Long = 0L
  var allTimeBountyPeak: Long = 100L
  var convergencesWon: Long = 0L
  var hvtEliminations: Long = 0L
  var greensCollected: Long = 0L
  private var safeZoneToastCooldown: Float = 0f

  // Phase 3: Pilot Level Progression (Levels 1..100), 5 XP Sources, Customization Points & Rift Shards Currency
  var xpFromBuffs: Float = 0f
  var xpFromDamage: Float = 0f
  var xpFromAccuracy: Float = 0f
  var xpFromBountyEarned: Float = 0f
  var xpFromBountyKilled: Float = 0f
  var pilotLevel: Int = 1
  var riftShards: Long = 150L
  var highestMilestoneLevelClaimed: Int = 0
  var spentCustomizationPoints: Int = 0
  var unlockedExtraSlots: Int = 0
  var aiShipDesignTokens: Int = 0

  // Phase 3: Public FFA Match Duration & >50% Presence Ratio Tracking (60-minute public cycle = 3600s)
  var publicMatchDurationSec: Float = 0f
  var publicMatchCycleLimitSec: Float = 3600f

  fun getTotalXp(): Long =
    xpFromBuffs.toLong() +
      xpFromDamage.toLong() +
      xpFromAccuracy.toLong() +
      xpFromBountyEarned.toLong() +
      xpFromBountyKilled.toLong()

  fun isNinthShipUnlocked(): Boolean =
    pilotLevel >= 20 || RiftCalibrationState.unlockFullNinthShipOverride

  fun getCustomizationPointsTotal(): Int =
    if (RiftCalibrationState.unlockFullNinthShipOverride) 100 else pilotLevel.coerceIn(1, 100)

  fun getCustomizationPointsAvailable(): Int =
    (getCustomizationPointsTotal() - spentCustomizationPoints).coerceAtLeast(0)

  fun getXpProgressInCurrentLevel(): Triple<Long, Long, Float> =
    com.example.data.LocalPilotPrefs.getXpProgressInLevel(getTotalXp())

  fun getMatchPresenceRatio(ship: ShipEntity = localPlayer): Float {
    if (publicMatchDurationSec <= 0.01f) return 1.0f
    return (ship.matchPresenceTimeSec / publicMatchDurationSec).coerceIn(0f, 1f)
  }

  fun awardPilotXp(
    buffXp: Float = 0f,
    damageXp: Float = 0f,
    accuracyXp: Float = 0f,
    bountyEarnedXp: Float = 0f,
    bountyKilledXp: Float = 0f,
    notifyChange: Boolean = true,
  ) {
    xpFromBuffs += buffXp.coerceAtLeast(0f)
    xpFromDamage += damageXp.coerceAtLeast(0f)
    xpFromAccuracy += accuracyXp.coerceAtLeast(0f)
    xpFromBountyEarned += bountyEarnedXp.coerceAtLeast(0f)
    xpFromBountyKilled += bountyKilledXp.coerceAtLeast(0f)

    val computedLvl = com.example.data.LocalPilotPrefs.computeLevelFromTotalXp(getTotalXp())
    if (computedLvl > pilotLevel) {
      pilotLevel = computedLvl
      var milestoneBonusShards = 0L
      while (highestMilestoneLevelClaimed + 5 <= pilotLevel && highestMilestoneLevelClaimed + 5 <= 100) {
        highestMilestoneLevelClaimed += 5
        milestoneBonusShards += 50L
      }
      if (milestoneBonusShards > 0L) {
        riftShards += milestoneBonusShards
        sfx.playPrize()
        spawnPooledFct(
          text = "★ LVL $pilotLevel! +${milestoneBonusShards}❖ SHARDS",
          x = localPlayer.x,
          y = localPlayer.y - 38f,
          color = Color.parseColor("#FFD700"),
          maxLife = 1.45f,
        )
        onPrizeToast("★ MILESTONE PILOT LVL $pilotLevel: +1 CUSTOM PT & +${milestoneBonusShards}❖ RIFT SHARDS!")
      } else {
        sfx.playPrize()
        spawnPooledFct(
          text = "★ PILOT LVL $pilotLevel (+1 PT)",
          x = localPlayer.x,
          y = localPlayer.y - 36f,
          color = Color.parseColor("#00F0FF"),
          maxLife = 1.25f,
        )
        onPrizeToast("★ PILOT LEVEL UP! LVL $pilotLevel (+1 CUSTOMIZATION POINT • ${pilotLevel}/100 PTS)")
      }
      if (notifyChange) {
        onStatsUpdated()
      }
    } else if (notifyChange) {
      onStatsUpdated()
    }
  }

  fun simulatePilotLevelUp() {
    if (pilotLevel >= 100) {
      onPrizeToast("★ MAX PILOT LEVEL 100 REACHED (100/100 CUSTOMIZATION POINTS)")
      return
    }
    val nextLvl = (pilotLevel + 1).coerceAtMost(100)
    val neededTotal = com.example.data.LocalPilotPrefs.xpForLevelStart(nextLvl)
    val deficit = (neededTotal - getTotalXp()).coerceAtLeast(50L).toFloat()
    val perSource = deficit / 5f
    awardPilotXp(
      buffXp = perSource,
      damageXp = perSource,
      accuracyXp = perSource,
      bountyEarnedXp = perSource,
      bountyKilledXp = perSource,
      notifyChange = true,
    )
  }

  /**
   * Evaluates public FFA match / Sector Convergence conclusion:
   * 1. Awards +100❖ Rift Shards & +1 Public Game Win if the #1 top-bounty player was present for >50% of total match duration.
   * 2. Procedurally resets the sector into a brand-new map (new seed, fresh walls, anomalies, Supply Crates, and radar map),
   *    clears projectiles, resets bounties to 100, restores full Flux, and starts a new 60-minute FFA match cycle.
   */
  fun concludePublicFfaMatchCycle(forceSimulateWinWithPresence: Boolean = false) {
    if (forceSimulateWinWithPresence) {
      publicMatchDurationSec = 3600f // 60-minute public FFA match
      localPlayer.matchPresenceTimeSec = 2280f // 38 minutes present (63.3% > 50% presence rule)
      val highestOtherBounty = ships.filter { !it.isLocalPlayer }.maxOfOrNull { it.bounty } ?: 250
      if (localPlayer.bounty <= highestOtherBounty) {
        localPlayer.bounty = highestOtherBounty + 85
      }
      allTimeBountyPeak = max(allTimeBountyPeak, localPlayer.bounty.toLong())
      pvpBountyPeak = max(pvpBountyPeak, localPlayer.bounty.toLong())
    }

    val effectiveDuration = publicMatchDurationSec.coerceAtLeast(1f)
    val presenceRatio = (localPlayer.matchPresenceTimeSec / effectiveDuration).coerceIn(0f, 1f)
    val presencePct = (presenceRatio * 100f).toInt()
    val topShip = ships.maxByOrNull { it.bounty }

    if (topShip != null && (topShip.isLocalPlayer || localPlayer.bounty >= topShip.bounty)) {
      if (presenceRatio > 0.50f) {
        convergencesWon++
        riftShards += 100L
        sfx.playPrize()
        val bannerMsg =
          "🏆 SECTOR CONVERGENCE VICTORY! #1 BOUNTY (${localPlayer.bounty} BTY • $presencePct% PRESENCE) — +100❖ RIFT SHARDS & NEW FFA MAP!"
        highBountyShutdownBanner = bannerMsg
        highBountyShutdownTimer = 5.5f
        onPrizeToast(bannerMsg)
      } else {
        val bannerMsg =
          "🌀 SECTOR CONVERGENCE: #1 BOUNTY ACHIEVED, BUT PRESENCE ($presencePct%) <= 50% — NEW FFA MAP GENERATED!"
        highBountyShutdownBanner = bannerMsg
        highBountyShutdownTimer = 4.5f
        onPrizeToast(bannerMsg)
      }
    } else {
      val winnerName = topShip?.callsign ?: "SECTOR ACE"
      val winnerBty = topShip?.bounty ?: 100
      val bannerMsg =
        "🌀 SECTOR CONVERGENCE: WINNER $winnerName ($winnerBty BTY) — PROCEDURAL NEW FFA MAP ONLINE!"
      highBountyShutdownBanner = bannerMsg
      highBountyShutdownTimer = 4.5f
      onPrizeToast(bannerMsg)
    }

    // Procedurally regenerate a brand-new arena map for the next 60-minute FFA match
    val newSeed = Random.nextInt(10_000, 999_999)
    generateSectorArena(seed = newSeed)
    bullets.clear()
    bombs.clear()
    decoys.clear()
    brickWalls.clear()
    shockwaves.clear()
    shieldRipples.clear()

    publicMatchDurationSec = 0f
    for (s in ships) {
      s.matchPresenceTimeSec = 0f
      s.bounty = 100
      s.flux = s.maxFlux
      s.respawnTimer = 0f
      s.vx = 0f
      s.vy = 0f
      s.isCloaked = false
      s.sniperOverchargeReady = false
      s.rocketBoostTimer = 0f
      s.overchargeSurgeStacks = 0
      s.multiShotCharges = 0
      s.bouncingBulletsShots = 0
      s.safeZoneStayTimer = 0f
      s.playfieldIdleTimer = 0f
      s.isIdleDraining = false
      if (s.isLocalPlayer) {
        val sp = getSafeZoneSpawnPosition()
        s.x = sp.first
        s.y = sp.second
        s.inSafeZone = true
        s.wasInSafeZone = true
        s.weaponsLocked = true
        s.canFire = false
        s.weaponState = "DISARMED"
        s.phaseShieldTimer = 3.0f
      } else {
        val sp = getOuterGateSpawnPosition(s.freq)
        s.x = sp.first
        s.y = sp.second
        s.inSafeZone = false
        s.wasInSafeZone = false
      }
    }
    sfx.playWarp()
    spawnPooledFct(
      text = "🌀 NEW SECTOR MAP CONVERGED",
      x = localPlayer.x,
      y = localPlayer.y - 38f,
      color = Color.parseColor("#00F0FF"),
      maxLife = 1.6f,
    )
    onStatsUpdated()
  }

  var localPlayer: ShipEntity

  init {
    generateSectorArena(seed = (System.currentTimeMillis() / 3600_000L).toInt())
    val initialSpec = RiftCodex.getShipById(1)
    val spawn = getSafeZoneSpawnPosition()
    localPlayer =
      ShipEntity(
        id = "local_player",
        callsign = "COMMANDER",
        syndicateTag = "GUEST",
        armadaName = "",
        shipId = initialSpec.id,
        spec = initialSpec,
        freq = 0,
        isHuman = true,
        isLocalPlayer = true,
        x = spawn.first,
        y = spawn.second,
        wasInSafeZone = true,
        inSafeZone = true,
        weaponsLocked = true,
        canFire = false,
        weaponState = "DISARMED",
        phaseShieldTimer = 3.0f,
      )
    syncLocalArmadaSelfSlot()
    rebuildRosterForMode("FFA")
  }

  /**
   * Phase 3.4: Hit-Stop Freeze — scales simulation dt to 0.05 for 32ms (~2 frames at 60Hz).
   */
  fun triggerHitStop(frames: Int = 2) {
    hitStopTimer = max(hitStopTimer, frames * 0.016f)
  }

  /**
   * Phase 3.4: Non-Linear Camera Trauma Shake (Offset = Trauma^2 * MaxOffset * Noise(t))
   */
  fun addTrauma(amount: Float) {
    cameraTrauma = (cameraTrauma + amount).coerceIn(0f, 1f)
  }

  /**
   * Phase 3.4: Spawn Object-Pooled Floating Combat Text (FCT) with elastic scale overshoot (1.0 -> 1.4 -> 1.0),
   * drifting upward at 48px/s over 750ms with quadratic fade.
   */
  fun spawnPooledFct(text: String, x: Float, y: Float, color: Int, maxLife: Float = 0.75f) {
    val slot =
      fctPool.firstOrNull { !it.active }
        ?: fctPool.minByOrNull { it.maxLife - it.elapsed }
        ?: FloatingPopup()
    slot.active = true
    slot.text = text
    slot.x = x + (Random.nextFloat() - 0.5f) * 14f
    slot.y = y - 10f
    slot.vy = -48f
    slot.elapsed = 0f
    slot.maxLife = maxLife
    slot.color = color
    slot.scale = 1.0f
    if (!floatingPopups.contains(slot)) {
      floatingPopups.add(slot)
    }
  }

  fun getCurrentArmadaSize(): Int {
    return if (activeArmadaName.isEmpty()) 1 else activeArmadaSlots.size.coerceIn(1, 5)
  }

  fun getCurrentArmadaTierLabel(): String {
    val sz = getCurrentArmadaSize()
    return when {
      activeArmadaName.isEmpty() || sz <= 1 -> "SOLO RIFT"
      sz == 2 -> "2-MAN ARMADA [$activeArmadaName]"
      sz == 3 -> "3-MAN ARMADA [$activeArmadaName]"
      sz == 4 -> "4-MAN ARMADA [$activeArmadaName]"
      else -> "5-MAN ARMADA [$activeArmadaName]"
    }
  }

  fun syncLocalArmadaSelfSlot() {
    if (activeArmadaName.isEmpty()) {
      activeArmadaSlots.clear()
      localPlayer.armadaName = ""
      return
    }
    localPlayer.armadaName = activeArmadaName
    val selfSlot =
      ArmadaMemberSlot(
        uid = localPlayer.id,
        callsign = localPlayer.callsign,
        syndicateTag = localPlayer.syndicateTag,
        shipId = localPlayer.shipId,
        isLeader = true,
      )
    val existingIdx = activeArmadaSlots.indexOfFirst { it.uid == localPlayer.id || it.isLeader }
    if (existingIdx >= 0) {
      activeArmadaSlots[existingIdx] = selfSlot
    } else {
      activeArmadaSlots.add(0, selfSlot)
    }
  }

  fun formOrJoinArmada(rawName: String, initialWingmen: List<FriendPilotEntry> = emptyList()) {
    val cleanName = rawName.trim().uppercase().replace(Regex("[^A-Z0-9_\\-]"), "").take(10)
    if (cleanName.isEmpty()) return
    activeArmadaName = cleanName
    activeArmadaSlots.clear()
    syncLocalArmadaSelfSlot()
    for (w in initialWingmen) {
      if (activeArmadaSlots.size < 5 && activeArmadaSlots.none { it.callsign == w.callsign }) {
        activeArmadaSlots.add(
          ArmadaMemberSlot(
            uid = w.uid,
            callsign = w.callsign,
            syndicateTag = w.syndicateTag,
            shipId = w.shipId,
            isLeader = false,
          )
        )
      }
    }
    applyArmadaRosterToSectorShips()
    onPrizeToast("🛡️ ARMADA [$activeArmadaName] ACTIVE (${getCurrentArmadaSize()}/5 PILOTS)")
    onStatsUpdated()
  }

  fun addMemberToArmada(friend: FriendPilotEntry): Boolean {
    if (activeArmadaName.isEmpty()) {
      activeArmadaName = "${localPlayer.syndicateTag}_WING"
      syncLocalArmadaSelfSlot()
    }
    if (activeArmadaSlots.size >= 5) {
      onPrizeToast("⚠️ ARMADA [$activeArmadaName] IS AT MAX 5-MAN CAPACITY")
      return false
    }
    if (activeArmadaSlots.any { it.callsign.equals(friend.callsign, ignoreCase = true) }) {
      onPrizeToast("${friend.callsign} IS ALREADY IN ARMADA [$activeArmadaName]")
      return false
    }
    val prevSize = getCurrentArmadaSize()
    activeArmadaSlots.add(
      ArmadaMemberSlot(
        uid = friend.uid,
        callsign = friend.callsign,
        syndicateTag = friend.syndicateTag,
        shipId = friend.shipId,
        isLeader = false,
      )
    )
    val newSize = getCurrentArmadaSize()
    applyArmadaRosterToSectorShips()
    onPrizeToast(
      "⚡ ARMADA ROSTER SHIFT: [$activeArmadaName] ${prevSize}-MAN ➔ ${newSize}-MAN LEADERBOARD TIER"
    )
    onStatsUpdated()
    return true
  }

  fun removeMemberFromArmada(uidOrCallsign: String) {
    if (activeArmadaName.isEmpty()) return
    val prevSize = getCurrentArmadaSize()
    activeArmadaSlots.removeAll {
      !it.isLeader && (it.uid == uidOrCallsign || it.callsign.equals(uidOrCallsign, ignoreCase = true))
    }
    if (activeArmadaSlots.size <= 1) {
      val oldName = activeArmadaName
      activeArmadaName = ""
      activeArmadaSlots.clear()
      localPlayer.armadaName = ""
      applyArmadaRosterToSectorShips()
      onPrizeToast("⚡ ARMADA [$oldName] DISBANDED ➔ SHIFTED TO [SOLO] LEADERBOARD TIER")
    } else {
      val newSize = getCurrentArmadaSize()
      applyArmadaRosterToSectorShips()
      onPrizeToast(
        "⚡ ARMADA ROSTER SHIFT: [$activeArmadaName] ${prevSize}-MAN ➔ ${newSize}-MAN LEADERBOARD TIER"
      )
    }
    onStatsUpdated()
  }

  fun simulateArmadaDropoutToDuo() {
    if (activeArmadaName.isEmpty() || activeArmadaSlots.size <= 2) {
      formOrJoinArmada(
        "APEX_4",
        listOf(
          FriendPilotEntry("f1", "VORTEX_ACE", localPlayer.syndicateTag, 1, "", true, "APEX_4", 4, 320),
          FriendPilotEntry("f2", "CHRONO_X", localPlayer.syndicateTag, 6, "", true, "APEX_4", 4, 210),
          FriendPilotEntry("f3", "VALKYRIE_Z", localPlayer.syndicateTag, 7, "", true, "APEX_4", 4, 180),
        ),
      )
    }
    val before = activeArmadaSlots.size
    while (activeArmadaSlots.size > 2) {
      activeArmadaSlots.removeAt(activeArmadaSlots.lastIndex)
    }
    applyArmadaRosterToSectorShips()
    onPrizeToast(
      "⚡ ROSTER SHIFT DETECTED: [$activeArmadaName] DROPPED ${before}-MAN ➔ 2-MAN DUO TIER!"
    )
    onStatsUpdated()
  }

  fun leaveArmadaToSolo() {
    val old = activeArmadaName
    activeArmadaName = ""
    activeArmadaSlots.clear()
    localPlayer.armadaName = ""
    applyArmadaRosterToSectorShips()
    if (old.isNotEmpty()) {
      onPrizeToast("⚡ LEFT ARMADA [$old] ➔ FLYING [SOLO] IN THE RIFT")
    }
    onStatsUpdated()
  }

  private fun applyArmadaRosterToSectorShips() {
    localPlayer.armadaName = activeArmadaName
    val wingCallsigns = activeArmadaSlots.filter { !it.isLeader }.map { it.callsign }.toSet()
    for (s in ships) {
      if (s.isLocalPlayer) continue
      if (s.callsign in wingCallsigns) {
        s.armadaName = activeArmadaName
      } else if (s.armadaName == activeArmadaName) {
        s.armadaName = ""
      }
    }
    for (slot in activeArmadaSlots) {
      if (slot.isLeader) continue
      if (ships.none { it.callsign == slot.callsign }) {
        val candidate = ships.firstOrNull { !it.isLocalPlayer && it.armadaName != activeArmadaName }
        if (candidate != null) {
          candidate.callsign = slot.callsign
          candidate.syndicateTag = slot.syndicateTag
          candidate.armadaName = activeArmadaName
          candidate.shipId = slot.shipId
          candidate.spec = RiftCodex.getShipById(slot.shipId)
        }
      }
    }
  }

  /**
   * Friendly Fire Rule:
   * Strictly disabled between active Armada mates AND between same-Syndicate mates!
   */
  fun areAllied(a: ShipEntity, b: ShipEntity): Boolean {
    if (a.id == b.id) return true
    if (a.armadaName.isNotEmpty() && a.armadaName == b.armadaName) return true
    if (
      a.syndicateTag.isNotEmpty() &&
        a.syndicateTag != "FREE" &&
        a.syndicateTag != "GUEST" &&
        a.syndicateTag == b.syndicateTag
    ) {
      return true
    }
    return false
  }

  fun isBulletAlliedWithShip(b: BulletEntity, s: ShipEntity): Boolean {
    if (b.ownerId == s.id) return true
    if (b.ownerArmadaName.isNotEmpty() && b.ownerArmadaName == s.armadaName) return true
    if (
      b.ownerSyndicateTag.isNotEmpty() &&
        b.ownerSyndicateTag != "FREE" &&
        b.ownerSyndicateTag != "GUEST" &&
        b.ownerSyndicateTag == s.syndicateTag
    ) {
      return true
    }
    return false
  }

  fun isBombAlliedWithShip(bm: BombEntity, s: ShipEntity): Boolean {
    if (bm.ownerId == s.id) return true
    if (bm.ownerArmadaName.isNotEmpty() && bm.ownerArmadaName == s.armadaName) return true
    if (
      bm.ownerSyndicateTag.isNotEmpty() &&
        bm.ownerSyndicateTag != "FREE" &&
        bm.ownerSyndicateTag != "GUEST" &&
        bm.ownerSyndicateTag == s.syndicateTag
    ) {
      return true
    }
    return false
  }

  fun generateSectorArena(seed: Int) {
    walls.clear()
    val b = 64f
    // Outer Arena Perimeter Walls
    walls.add(WallRect(0f, 0f, WORLD_SIZE, b))
    walls.add(WallRect(0f, WORLD_SIZE - b, WORLD_SIZE, b))
    walls.add(WallRect(0f, b, b, WORLD_SIZE - b * 2f))
    walls.add(WallRect(WORLD_SIZE - b, b, b, WORLD_SIZE - b * 2f))

    // Center Base Trench Pillars around Safe Zone
    val cx = WORLD_SIZE / 2f
    val cy = WORLD_SIZE / 2f
    walls.add(WallRect(cx - 440f, cy - 440f, 256f, 64f))
    walls.add(WallRect(cx + 184f, cy - 440f, 256f, 64f))
    walls.add(WallRect(cx - 440f, cy + 376f, 256f, 64f))
    walls.add(WallRect(cx + 184f, cy + 376f, 256f, 64f))

    walls.add(WallRect(cx - 440f, cy - 376f, 64f, 192f))
    walls.add(WallRect(cx - 440f, cy + 184f, 64f, 192f))
    walls.add(WallRect(cx + 376f, cy - 376f, 64f, 192f))
    walls.add(WallRect(cx + 376f, cy + 184f, 64f, 192f))

    var rng = seed
    fun nextRand(): Float {
      rng = rng * 1664525 + 1013904223
      return ((rng ushr 8) and 0xFFFFFF).toFloat() / 0x1000000.toFloat()
    }

    for (i in 0 until 18) {
      val rx = 512f + ((nextRand() * 2600f).toInt() / 32) * 32f
      val ry = 512f + ((nextRand() * 2600f).toInt() / 32) * 32f
      val horizontal = nextRand() > 0.5f
      val w = if (horizontal) 288f else 64f
      val h = if (horizontal) 64f else 288f

      walls.add(WallRect(rx, ry, w, h))
      walls.add(WallRect(WORLD_SIZE - rx - w, ry, w, h))
      walls.add(WallRect(rx, WORLD_SIZE - ry - h, w, h))
      walls.add(WallRect(WORLD_SIZE - rx - w, WORLD_SIZE - ry - h, w, h))
    }

    greens.clear()
    pendingBuffSpawns.clear()
    recentBuffSpawnLocations.clear()
    val initialCap = getEffectiveBuffCap()
    repeat(initialCap) { spawnGreenCrate() }
    spawnInitialSpatialAnomalies()
    bakeStaticRadarBitmap()
  }

  private fun bakeStaticRadarBitmap() {
    val size = 160
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val c = Canvas(bmp)
    val drawSize = size * 0.84f
    val ox = (size - drawSize) / 2f
    val oy = (size - drawSize) / 2f
    val scale = drawSize / WORLD_SIZE

    val borderPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.5f
        color = Color.argb(120, 56, 189, 248)
      }
    c.drawRect(ox, oy, ox + drawSize, oy + drawSize, borderPaint)

    val safePaint =
      Paint().apply {
        style = Paint.Style.FILL
        color = Color.argb(45, 16, 185, 129)
      }
    c.drawRect(
      ox + safeZone.left * scale,
      oy + safeZone.top * scale,
      ox + safeZone.right * scale,
      oy + safeZone.bottom * scale,
      safePaint,
    )

    val wallPaint =
      Paint().apply {
        style = Paint.Style.FILL
        color = Color.argb(135, 148, 163, 184)
      }
    for (w in walls) {
      c.drawRect(
        ox + w.x * scale,
        oy + w.y * scale,
        ox + (w.x + w.w) * scale,
        oy + (w.y + w.h) * scale,
        wallPaint,
      )
    }
    radarBgBitmap = bmp
  }

  fun isInsideSafeZone(x: Float, y: Float): Boolean {
    return x >= safeZone.left && x <= safeZone.right && y >= safeZone.top && y <= safeZone.bottom
  }

  fun isWithinBotBanPerimeter(x: Float, y: Float): Boolean {
    return x >= safeZone.left - SAFE_ZONE_BOT_BAN_MARGIN &&
      x <= safeZone.right + SAFE_ZONE_BOT_BAN_MARGIN &&
      y >= safeZone.top - SAFE_ZONE_BOT_BAN_MARGIN &&
      y <= safeZone.bottom + SAFE_ZONE_BOT_BAN_MARGIN
  }

  fun circleIntersectsWalls(cx: Float, cy: Float, r: Float): Boolean {
    for (w in walls) {
      val closestX = cx.coerceIn(w.x, w.x + w.w)
      val closestY = cy.coerceIn(w.y, w.y + w.h)
      val dx = cx - closestX
      val dy = cy - closestY
      if (dx * dx + dy * dy < r * r) return true
    }
    for (bw in brickWalls) {
      val closestX = cx.coerceIn(bw.x, bw.x + bw.w)
      val closestY = cy.coerceIn(bw.y, bw.y + bw.h)
      val dx = cx - closestX
      val dy = cy - closestY
      if (dx * dx + dy * dy < r * r) return true
    }
    return false
  }

  fun spawnGreenCrate(): Boolean {
    val cap = getEffectiveBuffCap()
    if (greens.size >= cap) return false

    var chosenX = 0f
    var chosenY = 0f
    var found = false

    // Attempt to pick a non-repeating random spot at least 250f away from previous spawn locations and active crates
    for (attempt in 0 until 60) {
      val minSeparation = if (attempt < 40) 250f else 150f
      val x: Float
      val y: Float
      if (Random.nextFloat() < 0.35f) {
        val angle = Random.nextFloat() * (PI.toFloat() * 2f)
        val dist = 600f + Random.nextFloat() * 1400f
        x = (WORLD_SIZE * 0.5f + cos(angle) * dist).coerceIn(180f, WORLD_SIZE - 180f)
        y = (WORLD_SIZE * 0.5f + sin(angle) * dist).coerceIn(180f, WORLD_SIZE - 180f)
      } else {
        x = 180f + Random.nextFloat() * (WORLD_SIZE - 360f)
        y = 180f + Random.nextFloat() * (WORLD_SIZE - 360f)
      }

      if (circleIntersectsWalls(x, y, 44f) || isInsideSafeZone(x, y)) {
        continue
      }

      // Check distance against currently active crates
      var tooClose = false
      for (g in greens) {
        if (hypot(g.x - x, g.y - y) < minSeparation) {
          tooClose = true
          break
        }
      }
      if (tooClose) continue

      // Check distance against previous spawn locations in match memory (never in the same spot)
      for (loc in recentBuffSpawnLocations) {
        if (hypot(loc.first - x, loc.second - y) < minSeparation) {
          tooClose = true
          break
        }
      }
      if (tooClose) continue

      chosenX = x
      chosenY = y
      found = true
      break
    }

    if (!found) {
      chosenX = 240f + Random.nextFloat() * (WORLD_SIZE - 480f)
      chosenY = 240f + Random.nextFloat() * (WORLD_SIZE - 480f)
      if (isInsideSafeZone(chosenX, chosenY)) {
        chosenX += 600f
      }
    }

    // Record in spatial history to guarantee uniqueness during the match
    if (recentBuffSpawnLocations.size >= 64) {
      recentBuffSpawnLocations.removeFirst()
    }
    recentBuffSpawnLocations.addLast(Pair(chosenX, chosenY))

    greens.add(
      GreenCrate(
        x = chosenX,
        y = chosenY,
        spinPhase = Random.nextFloat() * (PI.toFloat() * 2f),
        buffType = Random.nextInt(11),
      )
    )
    return true
  }

  fun findRandomAnomalyPosition(existingAnomalies: List<SpatialAnomalyEntity> = emptyList()): Pair<Float, Float> {
    for (attempt in 0 until 60) {
      val margin = 320f
      val x = margin + Random.nextFloat() * (WORLD_SIZE - margin * 2f)
      val y = margin + Random.nextFloat() * (WORLD_SIZE - margin * 2f)
      // Must be outside safe zone with clearance and outside walls
      val inSafePadded = x >= safeZone.left - 260f && x <= safeZone.right + 260f &&
        y >= safeZone.top - 260f && y <= safeZone.bottom + 260f
      if (inSafePadded) continue
      if (circleIntersectsWalls(x, y, 120f)) continue
      val tooCloseToOther = existingAnomalies.any { hypot(it.x - x, it.y - y) < 560f }
      if (tooCloseToOther) continue
      return x to y
    }
    // Fallback safe coordinate
    return (WORLD_SIZE * 0.25f) to (WORLD_SIZE * 0.25f)
  }

  fun spawnSingleAnomaly(type: AnomalyType, partnerId: String = ""): SpatialAnomalyEntity {
    val pos = findRandomAnomalyPosition(anomalies)
    val id = "anomaly_${nextAnomalyId++}"
    val anomaly = SpatialAnomalyEntity(
      id = id,
      type = type,
      x = pos.first,
      y = pos.second,
      physicalRadius = 180f,
      life = 45.0f,
      maxLife = 45.0f,
      partnerId = partnerId,
    )
    anomalies.add(anomaly)
    return anomaly
  }

  fun spawnPhaseRiftPair() {
    val idA = "anomaly_${nextAnomalyId++}"
    val idB = "anomaly_${nextAnomalyId++}"
    val posA = findRandomAnomalyPosition(anomalies)
    val a = SpatialAnomalyEntity(
      id = idA,
      type = AnomalyType.PHASE_RIFT_A,
      x = posA.first,
      y = posA.second,
      physicalRadius = 180f,
      life = 45.0f,
      maxLife = 45.0f,
      partnerId = idB,
    )
    anomalies.add(a)

    val posB = findRandomAnomalyPosition(anomalies)
    val b = SpatialAnomalyEntity(
      id = idB,
      type = AnomalyType.PHASE_RIFT_B,
      x = posB.first,
      y = posB.second,
      physicalRadius = 180f,
      life = 45.0f,
      maxLife = 45.0f,
      partnerId = idA,
    )
    anomalies.add(b)
  }

  private fun spawnInitialSpatialAnomalies() {
    anomalies.clear()
    spawnSingleAnomaly(AnomalyType.PLASMA_RIFT)
    spawnSingleAnomaly(AnomalyType.REPULSOR_RIFT)
    spawnSingleAnomaly(AnomalyType.GRAVITY_RIFT)
    spawnPhaseRiftPair()
  }

  private fun getSafeZoneSpawnPosition(): Pair<Float, Float> {
    val x = safeZone.left + 80f + Random.nextFloat() * (safeZone.width() - 160f)
    val y = safeZone.top + 80f + Random.nextFloat() * (safeZone.height() - 160f)
    return x to y
  }

  private fun getOuterGateSpawnPosition(freq: Int): Pair<Float, Float> {
    for (i in 0 until 30) {
      val baseX = if (freq == 0) 560f else WORLD_SIZE - 560f
      val x = baseX + (Random.nextFloat() - 0.5f) * 260f
      val y = (WORLD_SIZE / 2f + (Random.nextFloat() - 0.5f) * 1400f).coerceIn(180f, WORLD_SIZE - 180f)
      if (!circleIntersectsWalls(x, y, 36f) && !isWithinBotBanPerimeter(x, y)) {
        return x to y
      }
    }
    return (if (freq == 0) 560f else WORLD_SIZE - 560f) to (WORLD_SIZE / 2f)
  }

  fun rebuildRosterForMode(newMode: String = "FFA") {
    mode = "FFA"
    val isFfa = true
    teamSize = 20

    val updated = mutableListOf<ShipEntity>()
    updated.add(localPlayer)
    var friendlyCount = 1
    var rivalCount = 0

    for (rp in remotePilots.values) {
      if (rp.id != localPlayer.id) {
        updated.add(rp)
        if (areAllied(localPlayer, rp)) friendlyCount++ else rivalCount++
      }
    }

    var botIdx = 0
    val maxFriendlyBots = if (isFfa) 0 else min(4, teamSize)
    while (friendlyCount < maxFriendlyBots) {
      val shipId = ((botIdx * 3) % 8) + 1
      val spec = RiftCodex.getShipById(shipId)
      val pos = getOuterGateSpawnPosition(0)
      updated.add(
        ShipEntity(
          id = "bot_ally_$botIdx",
          callsign = BOT_CALLSIGNS[botIdx % BOT_CALLSIGNS.size],
          syndicateTag = if (activeArmadaName.isNotEmpty()) activeArmadaName else cleanTag(localPlayer.syndicateTag),
          armadaName = if (botIdx == 0 && activeArmadaName.isNotEmpty()) activeArmadaName else "",
          shipId = shipId,
          spec = spec,
          freq = 0,
          isHuman = false,
          isLocalPlayer = false,
          x = pos.first,
          y = pos.second,
          bounty = if (botIdx == 0) 280 else 100 + (botIdx * 25),
        )
      )
      friendlyCount++
      botIdx++
    }

    val maxTotalPlayers = if (isFfa) 20 else 24
    while (updated.size < maxTotalPlayers) {
      val shipId = ((botIdx * 5 + 1) % 8) + 1
      val spec = RiftCodex.getShipById(shipId)
      val pos = getOuterGateSpawnPosition(1)
      val rivalArmada = if (isFfa) "" else RIVAL_ARMADAS[botIdx % RIVAL_ARMADAS.size]
      val rivalTag = if (rivalArmada.isNotEmpty()) rivalArmada else "GUEST"
      updated.add(
        ShipEntity(
          id = "bot_rival_$botIdx",
          callsign = BOT_CALLSIGNS[(botIdx + 7) % BOT_CALLSIGNS.size],
          syndicateTag = rivalTag,
          armadaName = rivalArmada,
          shipId = shipId,
          spec = spec,
          freq = if (isFfa) (botIdx + 1) else 1,
          isHuman = false,
          isLocalPlayer = false,
          x = pos.first,
          y = pos.second,
          angle = PI.toFloat(),
          weaponAngle = PI.toFloat(),
          bounty = if (botIdx == 0) 1150 else if (botIdx % 3 == 0) 310 else 100 + (botIdx * 20),
        )
      )
      rivalCount++
      botIdx++
    }

    ships.clear()
    ships.addAll(updated)
    applyArmadaRosterToSectorShips()

    if (killFeed.isEmpty()) {
      pushKillFeedEntry(
        killerTag = if (localPlayer.armadaName.isNotEmpty()) localPlayer.armadaName else cleanTag(localPlayer.syndicateTag),
        killerName = "VORTEX ACE",
        killerIsLocal = false,
        killerIsAllied = true,
        victimTag = "GUEST",
        victimName = "ZERO DRIFT",
        victimIsLocal = false,
        victimIsAllied = false,
        victimBounty = 160,
        bountyAwarded = 72,
        wasHvt = false,
        triggerAnnouncement = false,
      )
      pushKillFeedEntry(
        killerTag = "VOID 3",
        killerName = "HYPERION",
        killerIsLocal = false,
        killerIsAllied = false,
        victimTag = "GUEST",
        victimName = "SOLARIS IX",
        victimIsLocal = false,
        victimIsAllied = true,
        victimBounty = 280,
        bountyAwarded = 126,
        wasHvt = true,
        triggerAnnouncement = false,
      )
      pushKillFeedEntry(
        killerTag = "CRIMSON 4",
        killerName = "TITAN 04",
        killerIsLocal = false,
        killerIsAllied = false,
        victimTag = "GUEST",
        victimName = "RAVEN SIX",
        victimIsLocal = false,
        victimIsAllied = false,
        victimBounty = 190,
        bountyAwarded = 85,
        wasHvt = false,
        triggerAnnouncement = false,
      )
      pushKillFeedEntry(
        killerTag = "IRON DUO",
        killerName = "IRON WOLF",
        killerIsLocal = false,
        killerIsAllied = false,
        victimTag = "GUEST",
        victimName = "NEXUS 7",
        victimIsLocal = false,
        victimIsAllied = false,
        victimBounty = 1280,
        bountyAwarded = 576,
        wasHvt = true,
        triggerAnnouncement = false,
      )
    }
  }

  fun pushKillFeedEntry(
    killerTag: String,
    killerName: String,
    killerIsLocal: Boolean,
    killerIsAllied: Boolean,
    victimTag: String,
    victimName: String,
    victimIsLocal: Boolean,
    victimIsAllied: Boolean,
    victimBounty: Int,
    bountyAwarded: Int,
    wasHvt: Boolean,
    initialLife: Float = 14.0f,
    triggerAnnouncement: Boolean = true,
  ) {
    val cleanKiller = cleanTag(killerTag)
    val cleanVictim = cleanTag(victimTag)
    killFeed.add(
      0,
      KillFeedEntry(
        id = nextKillFeedId++,
        killerTag = cleanKiller,
        killerName = killerName,
        killerIsLocal = killerIsLocal,
        killerIsAllied = killerIsAllied,
        victimTag = cleanVictim,
        victimName = victimName,
        victimIsLocal = victimIsLocal,
        victimIsAllied = victimIsAllied,
        victimBounty = victimBounty,
        bountyAwarded = bountyAwarded,
        wasHvt = wasHvt,
        life = initialLife,
        maxLife = 14.0f,
      ),
    )
    if (victimBounty > 1000 && triggerAnnouncement) {
      highBountyShutdownBanner =
        "★ HIGH BOUNTY SHUTDOWN: $killerName ELIMINATED $victimName ($victimBounty BOUNTY) ★"
      highBountyShutdownTimer = 4.2f
    }
    while (killFeed.size > 8) {
      killFeed.removeAt(killFeed.lastIndex)
    }
  }

  fun updateRemotePilots(states: List<LivePilotState>, currentUid: String) {
    for (st in states) {
      val uid = st.userId.ifEmpty { st.id }
      if (uid.isEmpty() || uid == currentUid) continue
      val existing = remotePilots[uid]
      val spec = RiftCodex.getShipById(st.shipId.toInt())
      if (existing == null) {
        val created =
          ShipEntity(
            id = uid,
            callsign = st.callsign,
            syndicateTag = "SYNC",
            armadaName = "",
            shipId = spec.id,
            spec = spec,
            freq = st.freq.toInt(),
            isHuman = true,
            isLocalPlayer = false,
            x = st.x.toFloat(),
            y = st.y.toFloat(),
            vx = st.vx.toFloat(),
            vy = st.vy.toFloat(),
            angle = st.angle.toFloat(),
            weaponAngle = st.angle.toFloat(),
            flux = st.energy.toFloat(),
            maxFlux = st.maxEnergy.toFloat(),
            bounty = st.bounty.toInt(),
          )
        remotePilots[uid] = created
        rebuildRosterForMode(mode)
      } else {
        existing.callsign = st.callsign
        if (existing.shipId != spec.id) {
          existing.shipId = spec.id
          existing.spec = spec
        }
        existing.x = st.x.toFloat()
        existing.y = st.y.toFloat()
        existing.vx = st.vx.toFloat()
        existing.vy = st.vy.toFloat()
        val remoteSp = hypot(existing.vx, existing.vy)
        existing.angle = if (remoteSp > 2f) atan2(existing.vy, existing.vx) else st.angle.toFloat()
        existing.weaponAngle = existing.angle
        existing.flux = st.energy.toFloat()
        existing.maxFlux = st.maxEnergy.toFloat()
        existing.bounty = st.bounty.toInt()
      }
    }
  }

  fun switchLocalPlayerShip(newShipId: Int, forceFromHangar: Boolean = false): Boolean {
    val atFullFlux = localPlayer.flux >= localPlayer.maxFlux * 0.98f
    val inSafe = isInsideSafeZone(localPlayer.x, localPlayer.y)
    if (!forceFromHangar && !atFullFlux && !inSafe) {
      onPrizeToast("MUST BE AT 100% FLUX OR IN SAFE ZONE TO SWAP HULL!")
      return false
    }
    val spec = RiftCodex.getShipById(newShipId)
    localPlayer.shipId = spec.id
    localPlayer.spec = spec
    localPlayer.maxFlux = spec.maxFlux
    localPlayer.flux = spec.maxFlux
    localPlayer.repels = spec.initialRepels
    localPlayer.shotgunWavesRemaining = 0
    localPlayer.shotgunWaveTimer = 0f
    localPlayer.isCloaked = false
    localPlayer.sniperOverchargeReady = false
    if (!spec.supportsNativeMultifire && localPlayer.multiShotCharges <= 0) {
      localPlayer.multifireOn = false
    }
    buffVersion++
    syncLocalArmadaSelfSlot()
    onPrizeToast("HULL EQUIPPED: ${spec.id}-${spec.name.uppercase()} (${spec.maxFlux.toInt()}❖)")
    return true
  }

  fun syncLocalPlayerCustomShipSpec() {
    if (localPlayer.shipId == 9) {
      val updatedSpec = RiftCodex.customShip9Spec
      val fluxRatio = (localPlayer.flux / localPlayer.maxFlux.coerceAtLeast(1f)).coerceIn(0.1f, 1f)
      localPlayer.spec = updatedSpec
      localPlayer.maxFlux = updatedSpec.maxFlux
      localPlayer.flux = (updatedSpec.maxFlux * fluxRatio).coerceAtLeast(200f)
      if (!updatedSpec.supportsNativeMultifire && localPlayer.multiShotCharges <= 0) {
        localPlayer.multifireOn = false
      }
      if (updatedSpec.bombType != "CLOAK_EMP") {
        localPlayer.isCloaked = false
      }
    }
    onStatsUpdated()
  }

  fun engageManualBrake() {
    if (localPlayer.respawnTimer > 0f) return
    val speed = hypot(localPlayer.vx, localPlayer.vy)
    if (speed < 4f) {
      localPlayer.vx = 0f
      localPlayer.vy = 0f
      autoBraking = false
      sfx.playMutedClick()
      return
    }
    autoBraking = !autoBraking
    sfx.playMutedClick()
    if (autoBraking) {
      onPrizeToast("🛑 RETRO-BRAKES ENGAGED")
    }
  }

  fun canShipUseMultiShot(ship: ShipEntity = localPlayer): Boolean =
    ship.spec.supportsNativeMultifire || ship.multiShotCharges > 0

  fun toggleMultifire(): Boolean {
    if (!canShipUseMultiShot(localPlayer)) {
      onPrizeToast("MULTI-SHOT REQUIRES NATIVE HULL PERK OR +25 SHOT CRATE")
      return false
    }
    localPlayer.multifireOn = !localPlayer.multifireOn
    buffVersion++
    val suffix =
      if (localPlayer.spec.supportsNativeMultifire) {
        if (localPlayer.multiShotCharges > 0) "5-STREAM OVERDRIVE (${localPlayer.multiShotCharges} SHOTS)" else "NATIVE SPLIT"
      } else {
        "${localPlayer.multiShotCharges} SHOTS REMAINING"
      }
    onPrizeToast("MULTI-SHOT: ${if (localPlayer.multifireOn) "ON ($suffix)" else "OFF (FOCUSED)"}")
    return localPlayer.multifireOn
  }

  private fun selfDrainFlux(ship: ShipEntity, cost: Float) {
    ship.flux = max(1f, ship.flux - cost)
  }

  fun triggerSafeZoneWeaponLockout(ship: ShipEntity) {
    ship.inSafeZone = true
    ship.weaponsLocked = true
    ship.canFire = false
    ship.weaponState = "DISARMED"
    ship.isCloaked = false
    if (ship.isLocalPlayer && safeZoneToastCooldown <= 0f) {
      safeZoneToastCooldown = 0.85f
      sfx.playMutedClick()
      onPrizeToast("SAFE ZONE RESTRICTED: WEAPONS DISARMED")
    }
  }

  /**
   * Phase 2.2: Quick-Tap Auto-Aim Helper — finds the closest visible hostile ship (or enemy Decoy hologram) outside the Safe Zone.
   */
  fun findQuickTapAutoAimAngle(shooter: ShipEntity, maxRange: Float = 680f): Float {
    var closestX = 0f
    var closestY = 0f
    var closestVx = 0f
    var closestVy = 0f
    var foundTarget = false
    var closestDist = maxRange

    // Enemy Decoy holograms actively draw auto-aim and AI targeting
    for (decoy in decoys) {
      if (decoy.ownerId != shooter.id && decoy.freq != shooter.freq && !isInsideSafeZone(decoy.x, decoy.y)) {
        val d = hypot(decoy.x - shooter.x, decoy.y - shooter.y)
        if (d < closestDist) {
          closestDist = d
          closestX = decoy.x
          closestY = decoy.y
          closestVx = decoy.vx
          closestVy = decoy.vy
          foundTarget = true
        }
      }
    }

    for (other in ships) {
      if (
        !areAllied(shooter, other) &&
          other.respawnTimer <= 0f &&
          !other.isCloaked &&
          !isInsideSafeZone(other.x, other.y)
      ) {
        val d = hypot(other.x - shooter.x, other.y - shooter.y)
        if (d < closestDist) {
          closestDist = d
          closestX = other.x
          closestY = other.y
          closestVx = other.vx
          closestVy = other.vy
          foundTarget = true
        }
      }
    }
    return if (foundTarget) {
      val leadT = min(0.45f, closestDist / shooter.spec.primarySpeed)
      val tx = closestX + closestVx * leadT
      val ty = closestY + closestVy * leadT
      atan2(ty - shooter.y, tx - shooter.x)
    } else {
      shooter.angle
    }
  }

  fun firePrimaryGun(ship: ShipEntity) {
    if (ship.respawnTimer > 0f) return
    if (ship.isLocalPlayer && isCancelActive) return
    if (isInsideSafeZone(ship.x, ship.y)) {
      triggerSafeZoneWeaponLockout(ship)
      return
    }
    ship.inSafeZone = false
    ship.weaponsLocked = false
    ship.canFire = true
    ship.weaponState = "ARMED"
    if (ship.gunTimer > 0f) return

    // Firing weapons outside Safe Zone immediately strips Exit Phase Shield and uncloaks
    if (ship.phaseShieldTimer > 0f) {
      ship.phaseShieldTimer = 0f
    }
    if (ship.isCloaked) {
      ship.isCloaked = false
    }

    // Validate Multi-Shot availability
    if (ship.multifireOn && !ship.spec.supportsNativeMultifire && ship.multiShotCharges <= 0) {
      ship.multifireOn = false
      if (ship.isLocalPlayer) buffVersion++
    }

    val spec = ship.spec
    val cost = spec.primaryCost
    if (ship.flux <= cost + 1f) return

    selfDrainFlux(ship, cost)
    ship.gunTimer = spec.primaryCooldown

    if (ship.isLocalPlayer) {
      sfx.playLaser()
    }

    val baseFireAngle =
      if (ship.independentAimActive) {
        ship.weaponAngle
      } else if (ship.isLocalPlayer) {
        findQuickTapAutoAimAngle(ship)
      } else {
        ship.angle
      }
    ship.weaponAngle = baseFireAngle

    // Check 1-Viper / Phase Dash Sniper Overcharge (+35% velocity and +35% damage on next rail shot)
    val overcharged = ship.sniperOverchargeReady
    if (overcharged) {
      ship.sniperOverchargeReady = false
      if (ship.isLocalPlayer) {
        buffVersion++
        spawnPooledFct("⚡ OVERCHARGE RAIL +35%", ship.x, ship.y - 26f, Color.parseColor("#00F5D4"))
      }
    }
    val overchargeMult = if (overcharged) 1.35f else 1.0f

    // Check Shot-Limited Bouncing Bullets (+30 shots per crate)
    var bonusBounce = 0
    if (ship.bouncingBulletsShots > 0) {
      bonusBounce = 1
      ship.bouncingBulletsShots--
      if (ship.isLocalPlayer) buffVersion++
    }
    val bounces = spec.primaryBounces + bonusBounce
    val bulletSpeed = spec.primarySpeed * overchargeMult
    val isFriendlyToLocal = areAllied(localPlayer, ship)
    val baseColor =
      when {
        overcharged -> Color.parseColor("#00F5D4")
        isFriendlyToLocal -> Color.parseColor("#38BDF8")
        else -> Color.parseColor("#EF4444")
      }

    // Determine streams, spread, and per-stream damage according to exact Class & Multi-Shot rules:
    var streams = spec.primaryStreams
    var spread = spec.primarySpreadRad
    var damagePerBolt = spec.primaryDamage * overchargeMult
    var useParallelTwin = spec.isParallelTwin

    if (ship.multifireOn) {
      if (ship.spec.supportsNativeMultifire) {
        if (ship.multiShotCharges > 0) {
          // Native Multi-Shot ship WITH Shot-Limited Multi-Shot crate -> +25% bonus damage / 5-stream overdrive!
          ship.multiShotCharges--
          if (ship.isLocalPlayer) buffVersion++
          val totalBaseDmg = spec.primaryDamage * spec.primaryStreams * 1.25f * overchargeMult
          streams = 5
          spread = 0.085f
          damagePerBolt = totalBaseDmg / 5f
          useParallelTwin = false
        } else if (spec.isParallelTwin) {
          // 5-Twinfang Native Multi-Shot ON -> 4-bolt fan with proportional damage split (50% per bolt = 100% total)
          streams = 4
          spread = 0.075f
          damagePerBolt = (spec.primaryDamage * 0.50f) * overchargeMult
          useParallelTwin = false
        } else {
          // 3-Suppressor (or 9-Rift Frame Native Split) -> 3 spread streams where each deals exact 1/3 (33.3%) damage
          streams = 3
          spread = 0.095f
          damagePerBolt = (spec.primaryDamage / 3.0f) * overchargeMult
          useParallelTwin = false
        }
      } else if (ship.multiShotCharges > 0) {
        // Non-native ship using Shot-Limited Multi-Shot (+25 shots crate)
        ship.multiShotCharges--
        if (ship.multiShotCharges <= 0) {
          ship.multifireOn = false
          if (ship.isLocalPlayer) {
            onPrizeToast("MULTI-SHOT CHARGES DEPLETED (0 SHOTS)")
          }
        }
        if (ship.isLocalPlayer) buffVersion++
        streams = spec.primaryStreams + 2
        spread = max(0.09f, spec.primarySpreadRad * 1.5f)
        damagePerBolt = spec.primaryDamage * 0.68f * overchargeMult
        useParallelTwin = false
      }
    }

    if (useParallelTwin && streams == 2) {
      // 5-Twinfang Parallel Twin-Stream Autocannon: fires 2 parallel high-velocity bolts offset laterally by ±12px
      val perpX = -sin(baseFireAngle) * 12f
      val perpY = cos(baseFireAngle) * 12f
      for (side in listOf(-1f, 1f)) {
        val bx = ship.x + cos(baseFireAngle) * 42f + perpX * side
        val by = ship.y + sin(baseFireAngle) * 42f + perpY * side
        if (isInsideSafeZone(bx, by)) continue
        val bvx = ship.vx * 0.35f + cos(baseFireAngle) * bulletSpeed
        val bvy = ship.vy * 0.35f + sin(baseFireAngle) * bulletSpeed
        bullets.add(
          BulletEntity(
            ownerId = ship.id,
            ownerCallsign = ship.callsign,
            ownerSyndicateTag = ship.syndicateTag,
            ownerArmadaName = ship.armadaName,
            ownerIsHuman = ship.isHuman,
            freq = ship.freq,
            x = bx,
            y = by,
            vx = bvx,
            vy = bvy,
            damage = damagePerBolt,
            bouncesLeft = bounces,
            life = 1.65f,
            color = baseColor,
            isHeavyBurst = bounces > 0 || overcharged,
          )
        )
      }
      return
    }

    for (i in 0 until streams) {
      val offsetIndex = i - (streams - 1) / 2f
      val a = baseFireAngle + offsetIndex * spread
      val bx = ship.x + cos(a) * 44f
      val by = ship.y + sin(a) * 44f
      if (isInsideSafeZone(bx, by)) continue
      val bvx = ship.vx * 0.35f + cos(a) * bulletSpeed
      val bvy = ship.vy * 0.35f + sin(a) * bulletSpeed

      bullets.add(
        BulletEntity(
          ownerId = ship.id,
          ownerCallsign = ship.callsign,
          ownerSyndicateTag = ship.syndicateTag,
          ownerArmadaName = ship.armadaName,
          ownerIsHuman = ship.isHuman,
          freq = ship.freq,
          x = bx,
          y = by,
          vx = bvx,
          vy = bvy,
          damage = damagePerBolt,
          bouncesLeft = bounces,
          life = 1.65f,
          color = baseColor,
          isHeavyBurst = bounces > 0 || overcharged,
        )
      )
    }
  }

  /**
   * Handles the Contextual Class / Secondary Ability Button for all ships (1–9):
   * - 1-Viper ("PHASE_DASH"): Instant directional Phase Dash dodge burst + Sniper Overcharge (+35% next rail shot), 300❖, 2.5s CD.
   * - 2-Ricochet ("BOUNCE"): 3-wall Bounce Bomb, 350❖ + Newtonian recoil.
   * - 4-Goliath ("SIEGE"): Massive Siege Bomb, 650❖ + heavy Newtonian recoil.
   * - 6-Disruptor ("CLOAK_EMP"): Tap when uncloaked -> Stealth Cloak (180❖/s drain); Fire while cloaked -> EMP Bomb (350❖, drains 35% Max Flux + 2.5s recharge freeze).
   * - 7-Valkyrie ("PROX"): Proximity Flak Bomb (450❖, 65px proximity fuse bursting into 8 shrapnel pellets + recoil).
   * - 8-Manta ("MINE"): Proximity Mine (340❖, max 6 active mines per Manta, 30s lifespan).
   * - 9-Rift Frame Native Special ("BURST", "SHOTGUN" at 500❖ / 4s CD, or "MISSILE" at 600❖ / 5s CD).
   */
  fun fireSecondaryBomb(ship: ShipEntity, aimAngle: Float? = null) {
    if (ship.respawnTimer > 0f) return
    if (ship.isLocalPlayer && isBombCancelActive) return
    if (isInsideSafeZone(ship.x, ship.y)) {
      triggerSafeZoneWeaponLockout(ship)
      return
    }
    ship.inSafeZone = false
    ship.weaponsLocked = false
    ship.canFire = true
    ship.weaponState = "ARMED"
    if (ship.bombTimer > 0f) return

    val spec = ship.spec
    if (!spec.hasBomb) return

    val fireAngle = aimAngle ?: if (ship.independentAimActive) ship.weaponAngle else ship.angle
    ship.weaponAngle = fireAngle

    // 1. 6-Disruptor Combined Tactical Cloak + EMP Bomb System
    if (spec.bombType == "CLOAK_EMP") {
      if (!ship.isCloaked) {
        if (ship.flux <= 200f) {
          if (ship.isLocalPlayer) onPrizeToast("INSUFFICIENT FLUX FOR TACTICAL CLOAK (MIN 200❖)")
          return
        }
        ship.isCloaked = true
        ship.bombTimer = 0.35f
        selfDrainFlux(ship, 60f)
        if (ship.isLocalPlayer) {
          buffVersion++
          sfx.playWarp()
          spawnPooledFct("👻 STEALTH CLOAK ENGAGED", ship.x, ship.y - 28f, Color.parseColor("#B388FF"))
          onPrizeToast("STEALTH CLOAK ACTIVE (180❖/s) — TAP OR AIM SECONDARY TO LAUNCH EMP BOMB!")
        }
        return
      } else {
        // Already cloaked: firing while cloaked breaks stealth and launches the 350❖ EMP Bomb!
        ship.isCloaked = false
        if (ship.isLocalPlayer) buffVersion++
        val empCost = spec.bombCost // 350❖
        if (ship.flux <= empCost + 1f) {
          if (ship.isLocalPlayer) onPrizeToast("STEALTH DISENGAGED (INSUFFICIENT FLUX FOR 350❖ EMP)")
          ship.bombTimer = 0.35f
          return
        }
        if (ship.phaseShieldTimer > 0f) ship.phaseShieldTimer = 0f
        selfDrainFlux(ship, empCost)
        ship.bombTimer = spec.bombCooldown
        if (ship.isLocalPlayer) {
          sfx.playBomb()
          spawnPooledFct("⚡ EMP TORPEDO LAUNCHED", ship.x, ship.y - 28f, Color.parseColor("#B388FF"))
        }
        val bx = ship.x + cos(fireAngle) * 52f
        val by = ship.y + sin(fireAngle) * 52f
        if (!isInsideSafeZone(bx, by)) {
          val speed = 640f
          bombs.add(
            BombEntity(
              ownerId = ship.id,
              ownerCallsign = ship.callsign,
              ownerSyndicateTag = ship.syndicateTag,
              ownerArmadaName = ship.armadaName,
              ownerIsHuman = ship.isHuman,
              freq = ship.freq,
              type = "EMP",
              x = bx,
              y = by,
              vx = ship.vx * 0.25f + cos(fireAngle) * speed,
              vy = ship.vy * 0.25f + sin(fireAngle) * speed,
              damage = spec.bombDamage,
              bouncesLeft = spec.bombBounces,
              passWalls = false,
              life = 30.0f,
              radius = 19f,
            )
          )
        }
        return
      }
    }

    if (ship.phaseShieldTimer > 0f) {
      ship.phaseShieldTimer = 0f
    }

    val cost = spec.bombCost
    if (ship.flux <= cost + 1f) {
      if (ship.isLocalPlayer) {
        onPrizeToast("INSUFFICIENT FLUX (${cost.toInt()}❖ REQUIRED)")
      }
      return
    }

    selfDrainFlux(ship, cost)
    ship.bombTimer = spec.bombCooldown

    // 2. 1-Viper Phase Dash + Sniper Overcharge (300❖, 2.5s cooldown)
    if (spec.bombType == "PHASE_DASH") {
      val dashHeading =
        if (ship.isLocalPlayer && leftStickActive && leftStickMag > 0.1f) {
          leftStickAngle
        } else if (hypot(ship.vx, ship.vy) > 25f) {
          atan2(ship.vy, ship.vx)
        } else {
          fireAngle
        }
      val dashSpeed = 620f
      ship.vx = cos(dashHeading) * dashSpeed
      ship.vy = sin(dashHeading) * dashSpeed
      ship.angle = dashHeading
      ship.sniperOverchargeReady = true
      shockwaves.add(ShockwaveEntity(ship.x, ship.y, 18f, 160f, Color.parseColor("#00F5D4")))
      if (ship.isLocalPlayer) {
        buffVersion++
        sfx.playWarp()
        addTrauma(0.18f)
        spawnPooledFct("⚡ PHASE DASH + OVERCHARGE", ship.x, ship.y - 28f, Color.parseColor("#00F5D4"))
      }
      return
    }

    if (ship.isLocalPlayer) {
      sfx.playBomb()
    }

    // Apply backward Newtonian recoil impulse if applicable (2-Ricochet, 4-Goliath, 7-Valkyrie)
    if (spec.bombRecoil > 0f) {
      ship.vx -= cos(fireAngle) * spec.bombRecoil
      ship.vy -= sin(fireAngle) * spec.bombRecoil
    }

    // 3. 8-Manta Proximity Mines (340❖, max 6 active mines per ship, 30s lifespan)
    if (spec.bombType == "MINE") {
      val mx = ship.x - cos(ship.angle) * 44f
      val my = ship.y - sin(ship.angle) * 44f
      if (isInsideSafeZone(mx, my)) return
      val existingMines = bombs.filter { it.ownerId == ship.id && it.type == "MINE" }
      if (existingMines.size >= 6) {
        val oldest = existingMines.minByOrNull { it.life }
        if (oldest != null) {
          bombs.remove(oldest)
        }
      }
      bombs.add(
        BombEntity(
          ownerId = ship.id,
          ownerCallsign = ship.callsign,
          ownerSyndicateTag = ship.syndicateTag,
          ownerArmadaName = ship.armadaName,
          ownerIsHuman = ship.isHuman,
          freq = ship.freq,
          type = "MINE",
          x = mx,
          y = my,
          vx = 0f,
          vy = 0f,
          damage = spec.bombDamage,
          bouncesLeft = 0,
          passWalls = false,
          life = 30.0f,
          radius = 20f,
        )
      )
      return
    }

    // 4. 9-Rift Frame Built-in Secondary Burst (500❖, 4s CD)
    if (spec.bombType == "BURST") {
      fireRadialBurstProjectiles(ship)
      return
    }

    // 5. 9-Rift Frame Built-in Secondary Shotgun (500❖, 4s CD)
    if (spec.bombType == "SHOTGUN") {
      ship.shotgunAimAngle = fireAngle
      fireShotgunWave(ship, fireAngle)
      ship.shotgunWavesRemaining = 2
      ship.shotgunWaveTimer = 0.085f
      return
    }

    // 6. 9-Rift Frame Built-in Secondary Lock-On Bouncing Missile (600❖, 5s CD)
    if (spec.bombType == "MISSILE") {
      launchLockOnBouncingMissile(ship, fireAngle)
      return
    }

    // 7. Standard Warheads: "BOUNCE" (2-Ricochet), "SIEGE" (4-Goliath), "PROX" (7-Valkyrie)
    val bx = ship.x + cos(fireAngle) * 52f
    val by = ship.y + sin(fireAngle) * 52f
    if (isInsideSafeZone(bx, by)) return

    val speed = if (spec.bombType == "SIEGE") 540f else 640f
    val warheadRadius = if (spec.bombType == "SIEGE") 24f else 17f
    bombs.add(
      BombEntity(
        ownerId = ship.id,
        ownerCallsign = ship.callsign,
        ownerSyndicateTag = ship.syndicateTag,
        ownerArmadaName = ship.armadaName,
        ownerIsHuman = ship.isHuman,
        freq = ship.freq,
        type = spec.bombType,
        x = bx,
        y = by,
        vx = ship.vx * 0.25f + cos(fireAngle) * speed,
        vy = ship.vy * 0.25f + sin(fireAngle) * speed,
        damage = spec.bombDamage,
        bouncesLeft = spec.bombBounces,
        passWalls = false,
        life = 30.0f,
        radius = warheadRadius,
      )
    )
  }

  /**
   * Stacking & Overwrite Inventory Rules for Bucket A Special Buff Slot Pickups (Charge-Based, 0❖ Cost):
   * - Same-Type Stacking: Picking up a Special Buff of the same type currently held adds to the charge count.
   * - Different-Type Overwrite: Picking up a Special Buff of a different type immediately replaces the held type and charges.
   */
  fun grantSpecialBuffWeapon(ship: ShipEntity, newType: String, baseGrant: Int) {
    if (ship.specialWeaponType == newType && ship.specialWeaponCharges > 0) {
      ship.specialWeaponCharges = min(12, ship.specialWeaponCharges + baseGrant)
    } else {
      ship.specialWeaponType = newType
      ship.specialWeaponCharges = baseGrant
      ship.shotgunWavesRemaining = 0
      ship.shotgunWaveTimer = 0f
    }
  }

  private fun fireRadialBurstProjectiles(ship: ShipEntity) {
    if (ship.isLocalPlayer) {
      sfx.playLaser()
      addTrauma(0.25f)
    }
    val count = 24
    for (i in 0 until count) {
      val a = (PI.toFloat() * 2f * i) / count
      bullets.add(
        BulletEntity(
          ownerId = ship.id,
          ownerCallsign = ship.callsign,
          ownerSyndicateTag = ship.syndicateTag,
          ownerArmadaName = ship.armadaName,
          ownerIsHuman = ship.isHuman,
          freq = ship.freq,
          x = ship.x + cos(a) * 44f,
          y = ship.y + sin(a) * 44f,
          vx = cos(a) * 860f,
          vy = sin(a) * 860f,
          damage = 280f,
          bouncesLeft = 2,
          life = 1.9f,
          color = Color.parseColor("#F59E0B"),
          isHeavyBurst = true,
        )
      )
    }
  }

  /**
   * Fires a single 60-degree cone wave of bouncing short-range Shotgun pellets.
   */
  fun fireShotgunWave(ship: ShipEntity, baseAngle: Float) {
    if (ship.respawnTimer > 0f || isInsideSafeZone(ship.x, ship.y)) return
    if (ship.isLocalPlayer) {
      sfx.playShotgun()
    }
    val pelletCount = 9
    val coneRad = (PI.toFloat() / 3f) // 60-degree cone
    val isFriendlyToLocal = areAllied(localPlayer, ship)
    val pelletColor = if (isFriendlyToLocal) Color.parseColor("#FB7185") else Color.parseColor("#EF4444")
    for (i in 0 until pelletCount) {
      val frac = (i.toFloat() / (pelletCount - 1).toFloat()) - 0.5f
      val jitter = (Random.nextFloat() - 0.5f) * 0.035f
      val a = baseAngle + frac * coneRad + jitter
      val bx = ship.x + cos(a) * 40f
      val by = ship.y + sin(a) * 40f
      if (isInsideSafeZone(bx, by)) continue
      val speed = 880f + (Random.nextFloat() - 0.5f) * 60f
      bullets.add(
        BulletEntity(
          ownerId = ship.id,
          ownerCallsign = ship.callsign,
          ownerSyndicateTag = ship.syndicateTag,
          ownerArmadaName = ship.armadaName,
          ownerIsHuman = ship.isHuman,
          freq = ship.freq,
          x = bx,
          y = by,
          vx = ship.vx * 0.25f + cos(a) * speed,
          vy = ship.vy * 0.25f + sin(a) * speed,
          damage = 55f,
          bouncesLeft = 2,
          life = 0.36f,
          color = pelletColor,
          isHeavyBurst = false,
        )
      )
    }
  }

  private fun launchLockOnBouncingMissile(ship: ShipEntity, aimAngle: Float) {
    if (ship.isLocalPlayer) {
      sfx.playBomb()
      addTrauma(0.20f)
    }
    val target = findMissileLockTarget(ship, aimAngle)
    val launchAngle =
      if (target != null) {
        atan2(target.y - ship.y, target.x - ship.x)
      } else {
        aimAngle
      }
    val goliathTopSpeed = RiftCodex.getShipById(4).topSpeed // Exact 4-Goliath speed (380 PX/S)
    val mx = ship.x + cos(launchAngle) * 48f
    val my = ship.y + sin(launchAngle) * 48f
    if (!isInsideSafeZone(mx, my)) {
      bombs.add(
        BombEntity(
          ownerId = ship.id,
          ownerCallsign = ship.callsign,
          ownerSyndicateTag = ship.syndicateTag,
          ownerArmadaName = ship.armadaName,
          ownerIsHuman = ship.isHuman,
          freq = ship.freq,
          type = "MISSILE",
          x = mx,
          y = my,
          vx = cos(launchAngle) * goliathTopSpeed,
          vy = sin(launchAngle) * goliathTopSpeed,
          damage = 780f,
          bouncesLeft = 8,
          passWalls = false,
          life = 1.5f,
          radius = 18f,
          targetShipId = target?.id ?: "",
        )
      )
      if (target != null) {
        target.missileLockWarningTimer = 1.5f
        if (ship.isLocalPlayer) {
          sfx.playMissileLockBeep()
          spawnPooledFct(
            text = "🎯 MISSILE LOCK: ${target.callsign}",
            x = ship.x,
            y = ship.y - 28f,
            color = Color.parseColor("#EF4444"),
          )
        }
      }
    }
  }

  /**
   * Spawns a 7-Valkyrie Proximity Flak 8-pellet shrapnel burst at (bx, by).
   */
  private fun spawnProximityFlakShrapnel(bomb: BombEntity) {
    val shrapnelCount = 8
    for (i in 0 until shrapnelCount) {
      val a = (PI.toFloat() * 2f * i) / shrapnelCount
      bullets.add(
        BulletEntity(
          ownerId = bomb.ownerId,
          ownerCallsign = bomb.ownerCallsign,
          ownerSyndicateTag = bomb.ownerSyndicateTag,
          ownerArmadaName = bomb.ownerArmadaName,
          ownerIsHuman = bomb.ownerIsHuman,
          freq = bomb.freq,
          x = bomb.x + cos(a) * 18f,
          y = bomb.y + sin(a) * 18f,
          vx = cos(a) * 760f,
          vy = sin(a) * 760f,
          damage = 145f,
          bouncesLeft = 1,
          life = 0.42f,
          color = Color.parseColor("#FF7043"),
          isHeavyBurst = true,
        )
      )
    }
  }

  /**
   * Finds the best hostile ship outside the Safe Zone to lock onto for the Lock-On Bouncing Missile.
   * Cloaked ships cannot be locked onto!
   */
  fun findMissileLockTarget(shooter: ShipEntity, aimAngle: Float): ShipEntity? {
    var bestTarget: ShipEntity? = null
    var bestScore = Float.MAX_VALUE
    for (other in ships) {
      if (
        !areAllied(shooter, other) &&
          other.respawnTimer <= 0f &&
          !other.isCloaked &&
          !isInsideSafeZone(other.x, other.y)
      ) {
        val dx = other.x - shooter.x
        val dy = other.y - shooter.y
        val dist = hypot(dx, dy)
        if (dist > 2200f) continue
        val toAngle = atan2(dy, dx)
        val angleDiff = kotlin.math.abs(atan2(sin(toAngle - aimAngle), cos(toAngle - aimAngle)))
        val score = dist * (0.65f + angleDiff * 0.55f)
        if (score < bestScore) {
          bestScore = score
          bestTarget = other
        }
      }
    }
    return bestTarget
  }

  /**
   * Activates a Bucket A Special Buff Slot charge (0❖ Flux Cost) for any ship (Local Player or AI Bot):
   * - BURST, SHOTGUN, MISSILE, DECOY (8s hologram clone), BRICK (120px 10s barrier wall), ROCKET (3.0s 1.65x boost)
   */
  fun activateShipSpecialBuff(ship: ShipEntity) {
    if (ship.respawnTimer > 0f || ship.specialWeaponCharges <= 0 || ship.specialWeaponType == "NONE") return
    if (isInsideSafeZone(ship.x, ship.y)) {
      if (ship.isLocalPlayer) triggerSafeZoneWeaponLockout(ship)
      return
    }
    if (ship.phaseShieldTimer > 0f) {
      ship.phaseShieldTimer = 0f
    }

    val activeType = ship.specialWeaponType
    ship.specialWeaponCharges--
    if (ship.specialWeaponCharges <= 0) {
      ship.specialWeaponCharges = 0
      ship.specialWeaponType = "NONE"
    }
    if (ship.isLocalPlayer) {
      buffVersion++
    }

    val aimAngle =
      if (ship.independentAimActive) {
        ship.weaponAngle
      } else if (ship.isLocalPlayer) {
        findQuickTapAutoAimAngle(ship)
      } else {
        ship.angle
      }

    when (activeType) {
      "BURST" -> {
        fireRadialBurstProjectiles(ship)
      }
      "SHOTGUN" -> {
        ship.weaponAngle = aimAngle
        fireShotgunWave(ship, aimAngle)
        ship.shotgunWavesRemaining = 2
        ship.shotgunWaveTimer = 0.085f
        ship.shotgunAimAngle = aimAngle
      }
      "MISSILE" -> {
        launchLockOnBouncingMissile(ship, aimAngle)
      }
      "DECOY" -> {
        // Spawns a hologram clone of your exact ship (with callsign and bounty tag) along a mirrored/divergent heading for 8s
        val divSign = if (Random.nextBoolean()) 1f else -1f
        val decoyAngle = ship.angle + divSign * 0.48f
        val speed = max(ship.spec.topSpeed * 0.92f, hypot(ship.vx, ship.vy))
        val decoyId = "decoy_${nextDecoyBrickId++}"
        decoys.add(
          DecoyEntity(
            id = decoyId,
            ownerId = ship.id,
            callsign = ship.callsign,
            syndicateTag = ship.syndicateTag,
            armadaName = ship.armadaName,
            shipId = ship.shipId,
            freq = ship.freq,
            x = ship.x + cos(decoyAngle) * 42f,
            y = ship.y + sin(decoyAngle) * 42f,
            vx = cos(decoyAngle) * speed,
            vy = sin(decoyAngle) * speed,
            angle = decoyAngle,
            bounty = ship.bounty,
            life = 8.0f,
            maxLife = 8.0f,
          )
        )
        // Divert any active enemy lock-on missiles currently targeting this ship onto the decoy!
        for (bm in bombs) {
          if (bm.type == "MISSILE" && bm.targetShipId == ship.id) {
            bm.targetShipId = decoyId
          }
        }
        ship.missileLockWarningTimer = 0f
        if (ship.isLocalPlayer) {
          sfx.playWarp()
          spawnPooledFct("👥 DECOY HOLOGRAM (8s)", ship.x, ship.y - 28f, Color.parseColor("#2DD4BF"))
        }
      }
      "BRICK" -> {
        // Deploys a temporary solid energy barrier wall (120px wide, lasts 10 seconds) perpendicular to ship heading
        val deployDist = 86f
        val cx = (ship.x + cos(ship.angle) * deployDist).coerceIn(140f, WORLD_SIZE - 140f)
        val cy = (ship.y + sin(ship.angle) * deployDist).coerceIn(140f, WORLD_SIZE - 140f)
        if (!isInsideSafeZone(cx, cy)) {
          val horizPerp = kotlin.math.abs(sin(ship.angle)) >= kotlin.math.abs(cos(ship.angle))
          val w = if (horizPerp) 120f else 22f
          val h = if (horizPerp) 22f else 120f
          brickWalls.add(
            BrickWallEntity(
              id = "brick_${nextDecoyBrickId++}",
              ownerId = ship.id,
              freq = ship.freq,
              x = cx - w * 0.5f,
              y = cy - h * 0.5f,
              w = w,
              h = h,
              life = 10.0f,
              maxLife = 10.0f,
            )
          )
          shockwaves.add(ShockwaveEntity(cx, cy, 12f, 95f, Color.parseColor("#38BDF8")))
          if (ship.isLocalPlayer) {
            sfx.playBounce()
            spawnPooledFct("🧱 BRICK BARRIER (10s)", cx, cy - 24f, Color.parseColor("#38BDF8"))
          }
        }
      }
      "ROCKET" -> {
        // Ignites a massive 3.0-second hyper-thruster boost (1.65x max speed) costing 0❖ Flux
        ship.rocketBoostTimer = 3.0f
        val boostSpd = ship.spec.topSpeed * 1.65f
        ship.vx = cos(ship.angle) * boostSpd
        ship.vy = sin(ship.angle) * boostSpd
        shockwaves.add(ShockwaveEntity(ship.x, ship.y, 18f, 180f, Color.parseColor("#F43F5E")))
        if (ship.isLocalPlayer) {
          buffVersion++
          sfx.playWarp()
          addTrauma(0.28f)
          spawnPooledFct("🚀 ROCKET HYPER-BOOST (3.0s)", ship.x, ship.y - 28f, Color.parseColor("#F43F5E"))
        }
      }
    }
  }

  fun activateUtility(type: String) {
    if (localPlayer.respawnTimer > 0f) return
    if (isInsideSafeZone(localPlayer.x, localPlayer.y)) {
      triggerSafeZoneWeaponLockout(localPlayer)
      return
    }
    if (localPlayer.phaseShieldTimer > 0f) {
      localPlayer.phaseShieldTimer = 0f
    }
    if (type == "SPECIAL" || type in setOf("BURST", "SHOTGUN", "MISSILE", "DECOY", "BRICK", "ROCKET")) {
      // Bucket A Special Buff Slot Pickups are strictly Charge-Based (0❖ Flux Cost!)
      activateShipSpecialBuff(localPlayer)
      return
    }
    if (type == "REPEL") {
      if (localPlayer.repels <= 0) return
      if (localPlayer.flux <= 320f) {
        onPrizeToast("INSUFFICIENT FLUX FOR REPEL (320❖)")
        return
      }
      localPlayer.repels--
      buffVersion++
      selfDrainFlux(localPlayer, 320f)
      sfx.playRepel()
      addTrauma(0.26f)
      val repelRadius = 520f
      shockwaves.add(
        ShockwaveEntity(
          x = localPlayer.x,
          y = localPlayer.y,
          r = 32f,
          maxR = repelRadius,
          color = Color.parseColor("#38BDF8"),
        )
      )
      // 1. Repel ALL other ships (hostile and allied)
      for (s in ships) {
        if (s.id != localPlayer.id && s.respawnTimer <= 0f) {
          val dx = s.x - localPlayer.x
          val dy = s.y - localPlayer.y
          val dist = hypot(dx, dy)
          if (dist < repelRadius && dist > 1f) {
            val push = ((repelRadius - dist) / repelRadius) * 960f
            s.vx += (dx / dist) * push
            s.vy += (dy / dist) * push
            if (hypot(s.vx, s.vy) > 2f) {
              s.angle = atan2(s.vy, s.vx)
            }
          }
        }
      }
      // 2. Repel ALL bullets
      for (b in bullets) {
        val dx = b.x - localPlayer.x
        val dy = b.y - localPlayer.y
        val dist = hypot(dx, dy)
        if (dist < repelRadius && dist > 1f) {
          val sp = max(720f, hypot(b.vx, b.vy))
          b.vx = (dx / dist) * sp
          b.vy = (dy / dist) * sp
        }
      }
      // 3. Repel ALL bombs and mines
      for (bm in bombs) {
        val dx = bm.x - localPlayer.x
        val dy = bm.y - localPlayer.y
        val dist = hypot(dx, dy)
        if (dist < repelRadius && dist > 1f) {
          val sp = max(640f, hypot(bm.vx, bm.vy))
          bm.vx = (dx / dist) * sp
          bm.vy = (dy / dist) * sp
        }
      }
      // 4. Repel ALL Supply Crates so they blast outward and smoothly damp to a stop
      for (g in greens) {
        val dx = g.x - localPlayer.x
        val dy = g.y - localPlayer.y
        val dist = hypot(dx, dy)
        if (dist < repelRadius && dist > 1f) {
          val push = ((repelRadius - dist) / repelRadius) * 860f
          g.vx += (dx / dist) * push
          g.vy += (dy / dist) * push
        }
      }
    }
  }

  /**
   * Phase 2.2: Exact 2-Segment Line-Intersection Raycast Engine (raycastWalls / intersectRaySegment)
   * against arena wall segments and active Brick barriers for ricochet/bounce ordnance trajectory prediction.
   */
  private fun intersectRaySegment(
    ox: Float,
    oy: Float,
    dx: Float,
    dy: Float,
    x1: Float,
    y1: Float,
    x2: Float,
    y2: Float,
    nx: Float,
    ny: Float,
  ): RaycastHit? {
    val sx = x2 - x1
    val sy = y2 - y1
    val denom = dx * sy - dy * sx
    if (kotlin.math.abs(denom) < 0.0001f) return null
    val qx = x1 - ox
    val qy = y1 - oy
    val t = (qx * sy - qy * sx) / denom
    val u = (qx * dy - qy * dx) / denom
    if (t > 1.5f && u in 0f..1f) {
      return RaycastHit(
        t = t,
        hitX = ox + dx * t,
        hitY = oy + dy * t,
        normalX = nx,
        normalY = ny,
      )
    }
    return null
  }

  fun computeBounceTrajectory(
    ship: ShipEntity,
    angle: Float,
    maxBounces: Int = 2,
    segmentMaxLen: Float = 540f,
  ): List<TrajectoryPoint> {
    val pts = mutableListOf(TrajectoryPoint(ship.x, ship.y, false))
    var curX = ship.x + cos(angle) * 24f
    var curY = ship.y + sin(angle) * 24f
    var dirX = cos(angle)
    var dirY = sin(angle)
    val remainingBounces = max(1, maxBounces)

    for (seg in 0..remainingBounces) {
      var closestHit: RaycastHit? = null
      var bestT = segmentMaxLen

      fun checkRect(left: Float, top: Float, right: Float, bottom: Float) {
        intersectRaySegment(curX, curY, dirX, dirY, left, top, right, top, 0f, -1f)?.let {
          if (it.t < bestT) {
            bestT = it.t
            closestHit = it
          }
        }
        intersectRaySegment(curX, curY, dirX, dirY, left, bottom, right, bottom, 0f, 1f)?.let {
          if (it.t < bestT) {
            bestT = it.t
            closestHit = it
          }
        }
        intersectRaySegment(curX, curY, dirX, dirY, left, top, left, bottom, -1f, 0f)?.let {
          if (it.t < bestT) {
            bestT = it.t
            closestHit = it
          }
        }
        intersectRaySegment(curX, curY, dirX, dirY, right, top, right, bottom, 1f, 0f)?.let {
          if (it.t < bestT) {
            bestT = it.t
            closestHit = it
          }
        }
      }

      for (w in walls) {
        checkRect(w.x, w.y, w.x + w.w, w.y + w.h)
      }
      for (bw in brickWalls) {
        checkRect(bw.x, bw.y, bw.x + bw.w, bw.y + bw.h)
      }

      val hit = closestHit
      if (hit != null) {
        pts.add(TrajectoryPoint(hit.hitX, hit.hitY, true))
        val dot = dirX * hit.normalX + dirY * hit.normalY
        dirX -= 2f * dot * hit.normalX
        dirY -= 2f * dot * hit.normalY
        curX = hit.hitX + hit.normalX * 2f
        curY = hit.hitY + hit.normalY * 2f
      } else {
        pts.add(TrajectoryPoint(curX + dirX * segmentMaxLen, curY + dirY * segmentMaxLen, false))
        break
      }
    }
    return pts
  }

  private fun bounceCircleAgainstRect(
    ship: ShipEntity,
    rx: Float,
    ry: Float,
    rw: Float,
    rh: Float,
  ) {
    val closestX = ship.x.coerceIn(rx, rx + rw)
    val closestY = ship.y.coerceIn(ry, ry + rh)
    val dx = ship.x - closestX
    val dy = ship.y - closestY
    val distSq = dx * dx + dy * dy
    if (distSq < ship.radius * ship.radius) {
      val dist = sqrt(distSq).coerceAtLeast(0.001f)
      val nx = dx / dist
      val ny = dy / dist
      val pen = ship.radius - dist
      ship.x += nx * (pen + 0.5f)
      ship.y += ny * (pen + 0.5f)

      val dot = ship.vx * nx + ship.vy * ny
      if (dot < 0f) {
        ship.vx = (ship.vx - 2f * dot * nx) * WALL_ELASTICITY
        ship.vy = (ship.vy - 2f * dot * ny) * WALL_ELASTICITY
        if (ship.isLocalPlayer && hypot(ship.vx, ship.vy) > 45f) {
          sfx.playBounce()
        }
      }
    }
  }

  private fun resolveShipWallBounce(ship: ShipEntity) {
    for (w in walls) {
      bounceCircleAgainstRect(ship, w.x, w.y, w.w, w.h)
    }
    for (bw in brickWalls) {
      bounceCircleAgainstRect(ship, bw.x, bw.y, bw.w, bw.h)
    }
  }

  private fun spawnExplosion(x: Float, y: Float, color: Int) {
    shockwaves.add(ShockwaveEntity(x, y, 16f, 290f, color))
    if (particles.size > 80) {
      repeat(20) { if (particles.isNotEmpty()) particles.removeAt(0) }
    }
    for (i in 0 until 18) {
      val a = Random.nextFloat() * PI.toFloat() * 2f
      val sp = 60f + Random.nextFloat() * 310f
      particles.add(
        ParticleEntity(
          x = x,
          y = y,
          vx = cos(a) * sp,
          vy = sin(a) * sp,
          life = 0.35f + Random.nextFloat() * 0.45f,
          maxLife = 0.8f,
          size = 4.4f + Random.nextFloat() * 4.8f,
          color = if (i % 2 == 0) color else Color.parseColor("#F59E0B"),
        )
      )
    }
  }

  private fun applyProjectileDamage(
    target: ShipEntity,
    attackerId: String,
    attackerIsHuman: Boolean,
    rawDamage: Float,
    impactAngle: Float,
    isHeavyImpact: Boolean = false,
    isEmp: Boolean = false,
  ) {
    if (target.respawnTimer > 0f) return
    if (isInsideSafeZone(target.x, target.y)) return

    // Taking damage immediately uncloaks a cloaked ship!
    if (target.isCloaked) {
      target.isCloaked = false
      if (target.isLocalPlayer) {
        buffVersion++
        onPrizeToast("⚠ STEALTH CLOAK BROKEN BY INCOMING FIRE!")
      }
    }

    // Anti-Spawn-Camp 3-Second Exit Phase Shield reduces incoming damage by 75%
    val shieldAbsorbed = target.phaseShieldTimer > 0f
    // 6-Disruptor EMP Bomb drains 35% Maximum Flux and disables Flux recharge for 2.5 seconds!
    val empFluxDrain = if (isEmp) target.maxFlux * 0.35f else 0f
    val baseCombined = max(rawDamage, empFluxDrain)
    val effectiveDamage = if (shieldAbsorbed) baseCombined * 0.25f else baseCombined

    if (isEmp) {
      target.empTimer = 2.5f
      if (target.isLocalPlayer) {
        onPrizeToast("⚡ HIT BY EMP BOMB! -35% FLUX & RECHARGE DISABLED FOR 2.5s!")
      }
    }

    target.hitFlashTimer = 0f
    shieldRipples.add(
      ShieldRippleEntity(
        shipId = target.id,
        impactAngle = impactAngle,
        intensity = if (isHeavyImpact || isEmp) 1.0f else 0.78f,
        life = 0.28f,
      )
    )

    if (attackerId == localPlayer.id || target.isLocalPlayer) {
      if (isHeavyImpact || isEmp) {
        triggerHitStop(2)
        addTrauma(0.45f)
      } else {
        addTrauma(0.08f)
      }
      val fctColor =
        when {
          isEmp -> Color.parseColor("#B388FF")
          shieldAbsorbed -> Color.parseColor("#38BDF8")
          isHeavyImpact -> Color.parseColor("#F59E0B")
          else -> Color.parseColor("#F1F5F9")
        }
      spawnPooledFct(
        text = if (isEmp) "⚡ EMP -${effectiveDamage.toInt()}❖" else "-${effectiveDamage.toInt()}",
        x = target.x,
        y = target.y - 18f,
        color = fctColor,
      )
    }

    if (attackerId == localPlayer.id && !target.isLocalPlayer) {
      awardPilotXp(
        damageXp = effectiveDamage * 0.10f,
        accuracyXp = 8f,
        notifyChange = false,
      )
    }

    target.flux -= effectiveDamage
    if (target.flux <= 0f) {
      val wasHvt = target.bounty >= HVT_BOUNTY_THRESHOLD
      val bountyAward = max(50, (target.bounty * 0.45f).toInt())
      target.flux = 0f
      target.deathX = target.x
      target.deathY = target.y
      target.respawnTimer = 3.0f
      target.deaths++
      target.isCloaked = false
      target.sniperOverchargeReady = false
      target.rocketBoostTimer = 0f
      target.overchargeSurgeStacks = 0

      if (target.isLocalPlayer || hypot(target.x - localPlayer.x, target.y - localPlayer.y) < 1100f) {
        sfx.playExplosion()
      }
      if (attackerId == localPlayer.id || target.isLocalPlayer) {
        triggerHitStop(2)
        addTrauma(0.45f)
      }
      spawnExplosion(
        target.x,
        target.y,
        if (areAllied(localPlayer, target)) Color.parseColor("#38BDF8") else Color.parseColor("#EF4444"),
      )

      spawnPooledFct(
        text = if (wasHvt) "★ +$bountyAward HVT!" else "+$bountyAward BTY",
        x = target.x,
        y = target.y - 26f,
        color = if (wasHvt) Color.parseColor("#F59E0B") else Color.parseColor("#10B981"),
        maxLife = 1.15f,
      )

      val victimPreDeathBounty = target.bounty
      val attacker = ships.find { it.id == attackerId }
      if (attacker != null) {
        attacker.kills++
        attacker.bounty += bountyAward
      }

      pushKillFeedEntry(
        killerTag =
          when {
            attacker == null -> "SECTOR"
            attacker.armadaName.isNotEmpty() -> attacker.armadaName
            else -> attacker.syndicateTag
          },
        killerName = attacker?.callsign ?: "ORDNANCE",
        killerIsLocal = attacker?.isLocalPlayer == true,
        killerIsAllied = attacker != null && areAllied(localPlayer, attacker),
        victimTag =
          if (target.armadaName.isNotEmpty()) target.armadaName else target.syndicateTag,
        victimName = target.callsign,
        victimIsLocal = target.isLocalPlayer,
        victimIsAllied = areAllied(localPlayer, target),
        victimBounty = victimPreDeathBounty,
        bountyAwarded = bountyAward,
        wasHvt = wasHvt,
      )

      if (attackerId == localPlayer.id) {
        allTimeKills++
        if (wasHvt) hvtEliminations++
        allTimeBountyPeak = max(allTimeBountyPeak, localPlayer.bounty.toLong())
        if (target.isHuman) {
          pvpKills++
          pvpBountyPeak = max(pvpBountyPeak, localPlayer.bounty.toLong())
        }
        awardPilotXp(
          bountyEarnedXp = bountyAward * 2f,
          bountyKilledXp = victimPreDeathBounty * 0.15f,
          notifyChange = false,
        )
        onArmadaTierKill(getCurrentArmadaSize(), localPlayer.bounty.toLong())
        onStatsUpdated()
      } else if (target.isLocalPlayer) {
        allTimeDeaths++
        if (attackerIsHuman) pvpDeaths++
        onStatsUpdated()
      }
    }
  }

  fun findRandomOpenPositionOutsideSafeZone(): Pair<Float, Float> {
    for (attempt in 0 until 60) {
      val margin = 240f
      val x = margin + Random.nextFloat() * (WORLD_SIZE - margin * 2f)
      val y = margin + Random.nextFloat() * (WORLD_SIZE - margin * 2f)
      val inSafePadded = x >= safeZone.left - 180f && x <= safeZone.right + 180f &&
        y >= safeZone.top - 180f && y <= safeZone.bottom + 180f
      if (inSafePadded) continue
      if (circleIntersectsWalls(x, y, 56f)) continue
      return x to y
    }
    return (WORLD_SIZE * 0.22f) to (WORLD_SIZE * 0.22f)
  }

  private fun destroyShipFromIdleDrain(target: ShipEntity) {
    if (target.respawnTimer > 0f) return
    val victimPreDeathBounty = target.bounty
    target.flux = 0f
    target.deathX = target.x
    target.deathY = target.y
    target.respawnTimer = 3.0f
    target.deaths++
    target.playfieldIdleTimer = 0f
    target.safeZoneStayTimer = 0f
    target.isIdleDraining = false

    if (target.isLocalPlayer || hypot(target.x - localPlayer.x, target.y - localPlayer.y) < 1100f) {
      sfx.playExplosion()
    }
    if (target.isLocalPlayer) {
      triggerHitStop(2)
      addTrauma(0.45f)
      allTimeDeaths++
      onStatsUpdated()
      onPrizeToast("SHIP DESTROYED — IDLE FLUX DRAIN DEPLETED")
    }
    spawnExplosion(
      target.x,
      target.y,
      Color.parseColor("#EF4444"),
    )
    spawnPooledFct(
      text = "⚠ IDLE FLUX OVERLOAD",
      x = target.x,
      y = target.y - 26f,
      color = Color.parseColor("#EF4444"),
      maxLife = 1.25f,
    )
    pushKillFeedEntry(
      killerTag = "SECTOR",
      killerName = "IDLE DRAIN",
      killerIsLocal = false,
      killerIsAllied = false,
      victimTag = if (target.armadaName.isNotEmpty()) target.armadaName else target.syndicateTag,
      victimName = target.callsign,
      victimIsLocal = target.isLocalPlayer,
      victimIsAllied = areAllied(localPlayer, target),
      victimBounty = victimPreDeathBounty,
      bountyAwarded = 0,
      wasHvt = victimPreDeathBounty >= HVT_BOUNTY_THRESHOLD,
    )
  }

  private fun destroyShipFromPlasmaRift(target: ShipEntity) {
    if (target.respawnTimer > 0f) return
    val victimPreDeathBounty = target.bounty
    target.flux = 0f
    target.deathX = target.x
    target.deathY = target.y
    target.respawnTimer = 3.0f
    target.deaths++
    target.playfieldIdleTimer = 0f
    target.safeZoneStayTimer = 0f
    target.isIdleDraining = false
    target.shotgunWavesRemaining = 0

    if (target.isLocalPlayer || hypot(target.x - localPlayer.x, target.y - localPlayer.y) < 1100f) {
      sfx.playExplosion()
    }
    if (target.isLocalPlayer) {
      triggerHitStop(2)
      addTrauma(0.45f)
      allTimeDeaths++
      onStatsUpdated()
      onPrizeToast("SHIP DESTROYED — PLASMA RIFT SHEAR OVERLOAD")
    }
    spawnExplosion(
      target.x,
      target.y,
      Color.parseColor("#EF4444"),
    )
    spawnPooledFct(
      text = "☢ PLASMA RIFT OVERLOAD",
      x = target.x,
      y = target.y - 26f,
      color = Color.parseColor("#EF4444"),
      maxLife = 1.25f,
    )
    pushKillFeedEntry(
      killerTag = "SECTOR",
      killerName = "PLASMA RIFT",
      killerIsLocal = false,
      killerIsAllied = false,
      victimTag = if (target.armadaName.isNotEmpty()) target.armadaName else target.syndicateTag,
      victimName = target.callsign,
      victimIsLocal = target.isLocalPlayer,
      victimIsAllied = areAllied(localPlayer, target),
      victimBounty = victimPreDeathBounty,
      bountyAwarded = 0,
      wasHvt = victimPreDeathBounty >= HVT_BOUNTY_THRESHOLD,
    )
  }

  /**
   * Requirement 5: Anti-Corner Bot Pathfinding & Raycasting
   * Casts an anticipation ray (lookAheadDist = 140px) along the bot's movement/heading vector.
   * Returns the nearest wall hit if within lookAheadDist.
   */
  private fun raycastBotLookAhead(
    ox: Float,
    oy: Float,
    dirX: Float,
    dirY: Float,
    lookAheadDist: Float = 140f,
  ): RaycastHit? {
    var closestHit: RaycastHit? = null
    var bestT = lookAheadDist
    for (w in walls) {
      val pad = 22f
      val left = w.x - pad
      val right = w.x + w.w + pad
      val top = w.y - pad
      val bottom = w.y + w.h + pad

      intersectRaySegment(ox, oy, dirX, dirY, left, top, right, top, 0f, -1f)?.let {
        if (it.t < bestT) {
          bestT = it.t
          closestHit = it
        }
      }
      intersectRaySegment(ox, oy, dirX, dirY, left, bottom, right, bottom, 0f, 1f)?.let {
        if (it.t < bestT) {
          bestT = it.t
          closestHit = it
        }
      }
      intersectRaySegment(ox, oy, dirX, dirY, left, top, left, bottom, -1f, 0f)?.let {
        if (it.t < bestT) {
          bestT = it.t
          closestHit = it
        }
      }
      intersectRaySegment(ox, oy, dirX, dirY, right, top, right, bottom, 1f, 0f)?.let {
        if (it.t < bestT) {
          bestT = it.t
          closestHit = it
        }
      }
    }
    return closestHit
  }

  private fun updateAiBot(bot: ShipEntity, dt: Float) {
    if (bot.respawnTimer > 0f) return

    val fluxRatio = bot.flux / bot.maxFlux
    // Highly self-protective survival instincts:
    // Retreat toward safe haven when flux drops below 40% (critical below 25%)
    val lowFluxRetreat = fluxRatio < 0.40f
    val insideSafeBanZone = isWithinBotBanPerimeter(bot.x, bot.y)

    // Requirement 5: Anti-Corner Bot Pathfinding & 1.2s Strafe/Reverse Override
    val speed = hypot(bot.vx, bot.vy)
    val moveDirX = if (speed > 8f) bot.vx / speed else cos(bot.angle)
    val moveDirY = if (speed > 8f) bot.vy / speed else sin(bot.angle)
    val wallHit = raycastBotLookAhead(bot.x, bot.y, moveDirX, moveDirY, lookAheadDist = 180f)

    if (wallHit != null && wallHit.t < 110f) {
      val orthoSign = if ((bot.aiSeed.toInt() and 1) == 0) 1f else -1f
      val orthoX = -wallHit.normalY * orthoSign
      val orthoY = wallHit.normalX * orthoSign
      val escapeX = moveDirX + wallHit.normalX * 2.2f + orthoX * 1.5f
      val escapeY = moveDirY + wallHit.normalY * 2.2f + orthoY * 1.5f
      val escapeAngle = atan2(escapeY, escapeX)
      bot.unstickAngle = escapeAngle
      bot.unstickTimer = 1.2f
      bot.vx += (wallHit.normalX * 2.2f + orthoX) * bot.spec.thrust * 0.65f * dt
      bot.vy += (wallHit.normalY * 2.2f + orthoY) * bot.spec.thrust * 0.65f * dt
    }

    if (bot.unstickTimer > 0f) {
      bot.unstickTimer = max(0f, bot.unstickTimer - dt)
      val damp = 0.25.pow(dt.toDouble()).toFloat()
      bot.vx = bot.vx * damp + cos(bot.unstickAngle) * bot.spec.thrust * 0.95f * dt
      bot.vy = bot.vy * damp + sin(bot.unstickAngle) * bot.spec.thrust * 0.95f * dt
      if (hypot(bot.vx, bot.vy) > 2f) {
        bot.angle = atan2(bot.vy, bot.vx)
      }
      bot.weaponAngle = bot.angle
      return
    }

    // Bots are strictly banned from loitering within 400px of the Safe Zone perimeter
    // unless retreating in survival mode
    if (!lowFluxRetreat && insideSafeBanZone) {
      val cx = WORLD_SIZE / 2f
      val cy = WORLD_SIZE / 2f
      val awayAngle = atan2(bot.y - cy, bot.x - cx)
      val damp = 0.28.pow(dt.toDouble()).toFloat()
      bot.vx = bot.vx * damp + cos(awayAngle) * bot.spec.thrust * 0.92f * dt
      bot.vy = bot.vy * damp + sin(awayAngle) * bot.spec.thrust * 0.92f * dt
      if (hypot(bot.vx, bot.vy) > 2f) {
        bot.angle = atan2(bot.vy, bot.vx)
      }
      bot.weaponAngle = bot.angle
      return
    }

    // Protective Survival Retreat: Fly toward Center Safe Haven when under threat or low flux
    if (lowFluxRetreat && !isInsideSafeZone(bot.x, bot.y)) {
      val cx = WORLD_SIZE / 2f
      val cy = WORLD_SIZE / 2f
      val toSafeAngle = atan2(cy - bot.y, cx - bot.x)
      val urgency = if (fluxRatio < 0.22f) 1.25f else 1.0f
      val damp = 0.28.pow(dt.toDouble()).toFloat()
      bot.vx = bot.vx * damp + cos(toSafeAngle) * bot.spec.thrust * urgency * dt
      bot.vy = bot.vy * damp + sin(toSafeAngle) * bot.spec.thrust * urgency * dt
      if (hypot(bot.vx, bot.vy) > 2f) {
        bot.angle = atan2(bot.vy, bot.vx)
      }
      bot.weaponAngle = bot.angle

      // Deploy defensive Repel when available and taking damage / chased
      if (bot.repels > 0 && bot.flux > 350f) {
        val chasersNearby = ships.any { !areAllied(bot, it) && it.respawnTimer <= 0f && hypot(it.x - bot.x, it.y - bot.y) < 360f }
        if (chasersNearby) {
          bot.repels--
          selfDrainFlux(bot, 320f)
          sfx.playRepel()
          shockwaves.add(
            ShockwaveEntity(
              x = bot.x,
              y = bot.y,
              r = 32f,
              maxR = 520f,
              color = Color.parseColor("#38BDF8"),
            )
          )
        }
      }
      return
    }

    // Scan for incoming projectile threats to dodge proactively
    var incomingThreatAngle: Float? = null
    var threatMinDist = 380f
    for (b in bullets) {
      if (b.ownerId != bot.id && !isBulletAlliedWithShip(b, bot)) {
        val dist = hypot(b.x - bot.x, b.y - bot.y)
        if (dist < threatMinDist) {
          val toShipX = bot.x - b.x
          val toShipY = bot.y - b.y
          val bulletSpeed = hypot(b.vx, b.vy).coerceAtLeast(1f)
          val dot = (b.vx / bulletSpeed) * (toShipX / dist) + (b.vy / bulletSpeed) * (toShipY / dist)
          if (dot > 0.65f) {
            threatMinDist = dist
            // Orthogonal dodge vector
            val dodgeSign = if ((bot.aiSeed.toInt() and 1) == 0) 1f else -1f
            incomingThreatAngle = atan2(b.vy, b.vx) + (PI.toFloat() * 0.5f * dodgeSign)
          }
        }
      }
    }

    for (bm in bombs) {
      if (bm.ownerId != bot.id && !isBombAlliedWithShip(bm, bot)) {
        val dist = hypot(bm.x - bot.x, bm.y - bot.y)
        if (dist < 460f) {
          threatMinDist = dist
          val awayAngle = atan2(bot.y - bm.y, bot.x - bm.x)
          incomingThreatAngle = awayAngle
        }
      }
    }

    var nearestEnemy: ShipEntity? = null
    var nearestDist = Float.MAX_VALUE
    for (other in ships) {
      if (
        !areAllied(bot, other) &&
          other.respawnTimer <= 0f &&
          !other.isCloaked &&
          !isInsideSafeZone(other.x, other.y) &&
          !isWithinBotBanPerimeter(other.x, other.y)
      ) {
        val d = hypot(other.x - bot.x, other.y - bot.y)
        if (d < nearestDist) {
          nearestDist = d
          nearestEnemy = other
        }
      }
    }

    // AI Bot Special Buff Slot Deployment (Burst, Shotgun, Missile, Decoy, Brick, Rocket)
    if (bot.specialWeaponCharges > 0 && bot.specialWeaponType != "NONE" && !isInsideSafeZone(bot.x, bot.y)) {
      val shouldTriggerBuff =
        when (bot.specialWeaponType) {
          "DECOY", "BRICK" -> incomingThreatAngle != null || (nearestEnemy != null && nearestDist < 520f && fluxRatio < 0.75f)
          "ROCKET" -> lowFluxRetreat || (nearestEnemy != null && nearestDist in 650f..1400f)
          "SHOTGUN", "BURST" -> nearestEnemy != null && nearestDist < 480f
          "MISSILE" -> nearestEnemy != null && nearestDist < 1100f
          else -> false
        }
      if (shouldTriggerBuff && Random.nextFloat() < 0.08f) {
        activateShipSpecialBuff(bot)
      }
    }

    // AI Bot Native Split Multi-Shot Toggle (3-Suppressor & 5-Twinfang & Shot-Limited Multi-Shot)
    if (bot.spec.supportsNativeMultifire || bot.multiShotCharges > 0) {
      bot.multifireOn = (nearestEnemy != null && nearestDist in 260f..640f) || bot.multiShotCharges > 0
    } else {
      bot.multifireOn = false
    }

    var desiredAngle: Float
    if (incomingThreatAngle != null) {
      desiredAngle = incomingThreatAngle
      bot.vx += cos(incomingThreatAngle) * bot.spec.thrust * 1.1f * dt
      bot.vy += sin(incomingThreatAngle) * bot.spec.thrust * 1.1f * dt
      // 1-Viper bots use Phase Dash to evade incoming ordnance
      if (bot.spec.bombType == "PHASE_DASH" && bot.bombTimer <= 0f && bot.flux > 450f) {
        fireSecondaryBomb(bot, incomingThreatAngle)
      }
    } else if (nearestEnemy != null && nearestDist < 1200f) {
      val leadT = min(0.75f, nearestDist / bot.spec.primarySpeed)
      val tx = nearestEnemy.x + nearestEnemy.vx * leadT
      val ty = nearestEnemy.y + nearestEnemy.vy * leadT
      val aimAngle = atan2(ty - bot.y, tx - bot.x)
      bot.weaponAngle = aimAngle
      bot.independentAimActive = true

      if (nearestDist < 380f) {
        val backAngle = atan2(bot.y - nearestEnemy.y, bot.x - nearestEnemy.x)
        val strafeSign = if ((bot.aiSeed.toInt() and 1) == 0) 0.65f else -0.65f
        desiredAngle = backAngle + strafeSign
      } else if (nearestDist > 750f) {
        desiredAngle = aimAngle
      } else {
        val strafeAngle = aimAngle + (PI.toFloat() * 0.45f * if ((bot.aiSeed.toInt() and 1) == 0) 1f else -1f)
        desiredAngle = strafeAngle
      }

      // 6-Disruptor bots cloak when approaching and fire EMP Bomb when within 560px
      if (bot.spec.bombType == "CLOAK_EMP") {
        if (!bot.isCloaked && nearestDist in 520f..1050f && bot.flux > bot.maxFlux * 0.60f && bot.bombTimer <= 0f) {
          fireSecondaryBomb(bot, aimAngle) // Engages Stealth Cloak
        } else if (bot.isCloaked && nearestDist < 540f && bot.flux > 420f && bot.bombTimer <= 0f) {
          fireSecondaryBomb(bot, aimAngle) // Breaks stealth and launches EMP Bomb!
        }
      } else if (bot.spec.bombType == "PHASE_DASH") {
        if (nearestDist in 450f..900f && bot.flux > bot.maxFlux * 0.55f && bot.bombTimer <= 0f) {
          fireSecondaryBomb(bot, aimAngle)
        }
      } else if (bot.spec.hasBomb && nearestDist in 260f..850f && bot.flux > bot.maxFlux * 0.50f) {
        fireSecondaryBomb(bot, aimAngle)
      }

      if (!bot.isCloaked && nearestDist < 850f && bot.flux > bot.maxFlux * 0.28f) {
        firePrimaryGun(bot)
      }
    } else {
      bot.independentAimActive = false
      val t = (System.currentTimeMillis() % 600000L) * 0.00035f
      val orbitRadius = 2400f
      val gx = WORLD_SIZE / 2f + cos(t + bot.aiSeed) * orbitRadius
      val gy = WORLD_SIZE / 2f + sin(t * 1.15f + bot.aiSeed) * orbitRadius
      desiredAngle = atan2(gy - bot.y, gx - bot.x)
      bot.weaponAngle = bot.angle
    }

    if (wallHit != null) {
      val steerX = cos(desiredAngle) + wallHit.normalX * 1.8f
      val steerY = sin(desiredAngle) + wallHit.normalY * 1.8f
      desiredAngle = atan2(steerY, steerX)
    }

    val surgeMult = 1f + bot.overchargeSurgeStacks * 0.08f
    val sideDamp = 0.34.pow(dt.toDouble()).toFloat()
    bot.vx = bot.vx * sideDamp + cos(desiredAngle) * bot.spec.thrust * surgeMult * 0.88f * dt
    bot.vy = bot.vy * sideDamp + sin(desiredAngle) * bot.spec.thrust * surgeMult * 0.88f * dt
    if (hypot(bot.vx, bot.vy) > 2f) {
      bot.angle = atan2(bot.vy, bot.vx)
    }
  }

  fun stepSimulation(rawDt: Float) {
    if (safeZoneToastCooldown > 0f) {
      safeZoneToastCooldown = max(0f, safeZoneToastCooldown - rawDt)
    }

    traumaTimeSec += rawDt
    if (cameraTrauma > 0f) {
      cameraTrauma = max(0f, cameraTrauma - 2.4f * rawDt)
    }
    val dt =
      if (hitStopTimer > 0f) {
        hitStopTimer = max(0f, hitStopTimer - rawDt)
        rawDt * 0.05f
      } else {
        rawDt
      }

    // 1. Local Player Thrust, Afterburner, Rocket Boost, Overcharge Surge, Snap-Back Auto-Brake & Aiming
    if (localPlayer.respawnTimer <= 0f) {
      val localInSafe = isInsideSafeZone(localPlayer.x, localPlayer.y)
      localPlayer.inSafeZone = localInSafe
      localPlayer.weaponsLocked = localInSafe
      localPlayer.canFire = !localInSafe
      localPlayer.weaponState = if (localInSafe) "DISARMED" else "ARMED"
      localPlayer.afterburning = false
      val surgeMult = 1f + localPlayer.overchargeSurgeStacks * 0.08f
      val rocketMult = if (localPlayer.rocketBoostTimer > 0f) 1.65f else 1.0f

      if (leftStickActive && leftStickMag > 0.08f) {
        autoBraking = false
        val isAfterburner = leftStickMag > 0.80f && localPlayer.flux > 25f && localPlayer.rocketBoostTimer <= 0f
        val accel =
          localPlayer.spec.thrust *
            surgeMult *
            rocketMult *
            (if (isAfterburner) 1.45f else leftStickMag)
        localPlayer.vx += cos(leftStickAngle) * accel * dt
        localPlayer.vy += sin(leftStickAngle) * accel * dt

        if (isAfterburner) {
          localPlayer.afterburning = true
          selfDrainFlux(localPlayer, 210f * dt)
        }
      } else if (autoBraking) {
        val brakeDamping = 0.22.pow(dt.toDouble()).toFloat()
        localPlayer.vx *= brakeDamping
        localPlayer.vy *= brakeDamping
        if (hypot(localPlayer.vx, localPlayer.vy) < 4f) {
          localPlayer.vx = 0f
          localPlayer.vy = 0f
          autoBraking = false
        }
      }

      if (fireStickActive && !isCancelActive) {
        if (fireStickHasAimVector) {
          localPlayer.independentAimActive = true
          localPlayer.weaponAngle = fireStickAimAngle
        } else {
          localPlayer.independentAimActive = false
          localPlayer.weaponAngle = findQuickTapAutoAimAngle(localPlayer)
        }
        firePrimaryGun(localPlayer)
      } else {
        localPlayer.independentAimActive = false
        localPlayer.weaponAngle = localPlayer.angle
      }
    }

    // Step Repel-pushed Supply Crates with wall bounce and smooth damping to rest
    for (g in greens) {
      val gSpeed = hypot(g.vx, g.vy)
      if (gSpeed > 1f) {
        val nx = (g.x + g.vx * dt).coerceIn(140f, WORLD_SIZE - 140f)
        val ny = (g.y + g.vy * dt).coerceIn(140f, WORLD_SIZE - 140f)
        if (circleIntersectsWalls(nx, g.y, 38f)) {
          g.vx = -g.vx * 0.7f
        } else {
          g.x = nx
        }
        if (circleIntersectsWalls(g.x, ny, 38f)) {
          g.vy = -g.vy * 0.7f
        } else {
          g.y = ny
        }
        val damp = 0.18.pow(dt.toDouble()).toFloat()
        g.vx *= damp
        g.vy *= damp
        if (hypot(g.vx, g.vy) < 4f) {
          g.vx = 0f
          g.vy = 0f
        }
      }
    }

    // Step temporary Brick Energy Barriers (10s lifespan)
    for (bwIdx in brickWalls.indices.reversed()) {
      val bw = brickWalls[bwIdx]
      bw.life -= dt
      if (bw.life <= 0f) {
        brickWalls.removeAt(bwIdx)
      }
    }

    // Step Decoy Hologram Clones (8s lifespan, wall bounce)
    for (dIdx in decoys.indices.reversed()) {
      val dec = decoys[dIdx]
      dec.life -= dt
      if (dec.life <= 0f || isInsideSafeZone(dec.x, dec.y)) {
        decoys.removeAt(dIdx)
        continue
      }
      val nx = dec.x + dec.vx * dt
      val ny = dec.y + dec.vy * dt
      if (circleIntersectsWalls(nx, dec.y, 32f)) {
        dec.vx = -dec.vx
      } else {
        dec.x = nx
      }
      if (circleIntersectsWalls(dec.x, ny, 32f)) {
        dec.vy = -dec.vy
      } else {
        dec.y = ny
      }
      if (hypot(dec.vx, dec.vy) > 2f) {
        dec.angle = atan2(dec.vy, dec.vx)
      }
    }

    // Advance pending buff replacement spawns and maintain mode cap
    val targetBuffCap = getEffectiveBuffCap()
    if (pendingBuffSpawns.isNotEmpty()) {
      val spawnIter = pendingBuffSpawns.listIterator()
      while (spawnIter.hasNext()) {
        val remainingTime = spawnIter.next() - dt
        if (remainingTime <= 0f) {
          spawnIter.remove()
          if (greens.size < targetBuffCap) {
            spawnGreenCrate()
          }
        } else {
          spawnIter.set(remainingTime)
        }
      }
    }
    if (RiftCalibrationState.buffRespawnFrequency > 0.001f || greens.isEmpty()) {
      while (greens.size + pendingBuffSpawns.size < targetBuffCap) {
        if (!spawnGreenCrate()) break
      }
    }
    while (greens.size > targetBuffCap && greens.isNotEmpty()) {
      val removed = greens.removeAt(greens.size - 1)
      if (particles.size < 90) {
        for (i in 0 until 6) {
          val a = Random.nextFloat() * PI.toFloat() * 2f
          val sp = 40f + Random.nextFloat() * 120f
          particles.add(
            ParticleEntity(
              x = removed.x,
              y = removed.y,
              vx = cos(a) * sp,
              vy = sin(a) * sp,
              life = 0.4f,
              maxLife = 0.4f,
              size = 5f,
              color = Color.parseColor("#10B981"),
            )
          )
        }
      }
    }

    publicMatchDurationSec += rawDt
    for (s in ships) {
      s.matchPresenceTimeSec += rawDt
    }
    if (publicMatchDurationSec >= publicMatchCycleLimitSec) {
      concludePublicFfaMatchCycle(forceSimulateWinWithPresence = false)
    }

    // 2. Step all ships
    for (s in ships) {
      if (s.hitFlashTimer > 0f) {
        s.hitFlashTimer = max(0f, s.hitFlashTimer - rawDt)
      }
      if (s.plasmaRiftTickTimer > 0f) {
        s.plasmaRiftTickTimer = max(0f, s.plasmaRiftTickTimer - rawDt)
      }
      if (s.missileLockWarningTimer > 0f) {
        s.missileLockWarningTimer = max(0f, s.missileLockWarningTimer - rawDt)
      }
      if (s.missileBeepCooldown > 0f) {
        s.missileBeepCooldown = max(0f, s.missileBeepCooldown - rawDt)
      }
      if (s.respawnTimer > 0f) {
        s.respawnTimer -= rawDt
        if (s.respawnTimer <= 0f) {
          s.respawnTimer = 0f
          val pos = if (s.isLocalPlayer) getSafeZoneSpawnPosition() else getOuterGateSpawnPosition(s.freq)
          s.x = pos.first
          s.y = pos.second
          s.vx = 0f
          s.vy = 0f
          s.bounty = 100
          s.flux = s.maxFlux
          s.overchargeSurgeStacks = 0
          s.multiShotCharges = 0
          s.bouncingBulletsShots = 0
          s.sniperOverchargeReady = false
          s.isCloaked = false
          s.rocketBoostTimer = 0f
          s.multifireOn = false
          s.repels = s.spec.initialRepels
          s.specialWeaponType = "NONE"
          s.specialWeaponCharges = 0
          s.shotgunWavesRemaining = 0
          s.shotgunWaveTimer = 0f
          s.missileLockWarningTimer = 0f
          s.plasmaRiftTickTimer = 0f
          if (s.isLocalPlayer) {
            buffVersion++
          }
          val inSafe = isInsideSafeZone(s.x, s.y)
          s.wasInSafeZone = inSafe
          s.inSafeZone = inSafe
          s.weaponsLocked = inSafe
          s.canFire = !inSafe
          s.weaponState = if (inSafe) "DISARMED" else "ARMED"
          s.phaseShieldTimer = 3.0f
          s.safeZoneStayTimer = 0f
          s.playfieldIdleTimer = 0f
          s.isIdleDraining = false
        }
        continue
      }

      if (!s.isHuman) {
        updateAiBot(s, dt)
      }

      if (s.gunTimer > 0f) s.gunTimer = max(0f, s.gunTimer - dt)
      if (s.bombTimer > 0f) s.bombTimer = max(0f, s.bombTimer - dt)

      // Step 3.0-second Rocket Hyper-Thruster Boost (1.65x max speed, 0❖ Flux cost, blazing plasma exhaust trail)
      if (s.rocketBoostTimer > 0f) {
        s.rocketBoostTimer = max(0f, s.rocketBoostTimer - dt)
        if (particles.size < 95 && Random.nextFloat() < 0.75f) {
          val tailX = s.x - cos(s.angle) * 32f
          val tailY = s.y - sin(s.angle) * 32f
          particles.add(
            ParticleEntity(
              x = tailX + (Random.nextFloat() - 0.5f) * 12f,
              y = tailY + (Random.nextFloat() - 0.5f) * 12f,
              vx = -cos(s.angle) * 240f + (Random.nextFloat() - 0.5f) * 60f,
              vy = -sin(s.angle) * 240f + (Random.nextFloat() - 0.5f) * 60f,
              life = 0.28f,
              maxLife = 0.28f,
              size = 5.5f,
              color = if (Random.nextBoolean()) Color.parseColor("#F43F5E") else Color.parseColor("#FACC15"),
            )
          )
        }
      }

      // Step 6-Disruptor Stealth Cloak continuous drain (180❖/sec; auto-uncloaks below 150❖)
      if (s.isCloaked) {
        selfDrainFlux(s, 180f * dt)
        if (s.flux < 150f || isInsideSafeZone(s.x, s.y)) {
          s.isCloaked = false
          if (s.isLocalPlayer) {
            buffVersion++
            onPrizeToast("STEALTH CLOAK DEACTIVATED (FLUX BELOW 150❖ FLOOR)")
          }
        }
      }

      // Step 3-wave rapid consecutive Shotgun cone burst
      if (s.shotgunWavesRemaining > 0) {
        s.shotgunWaveTimer -= dt
        if (s.shotgunWaveTimer <= 0f) {
          s.shotgunWavesRemaining--
          s.shotgunWaveTimer = 0.085f
          val waveAim = if (s.isLocalPlayer && s.independentAimActive) s.weaponAngle else s.shotgunAimAngle
          fireShotgunWave(s, waveAim)
        }
      }

      var currentlyInSafe = isInsideSafeZone(s.x, s.y)
      if (currentlyInSafe) {
        s.playfieldIdleTimer = 0f
        s.isIdleDraining = false
        s.safeZoneStayTimer += rawDt
        if (s.safeZoneStayTimer >= RiftCalibrationState.safeZoneIdleLimitSec) {
          val dest = findRandomOpenPositionOutsideSafeZone()
          s.x = dest.first
          s.y = dest.second
          s.vx = 0f
          s.vy = 0f
          s.safeZoneStayTimer = 0f
          currentlyInSafe = false
          s.wasInSafeZone = false
          s.phaseShieldTimer = 3.0f
          shockwaves.add(ShockwaveEntity(s.x, s.y, 14f, 220f, Color.parseColor("#38BDF8")))
          spawnPooledFct(
            text = "⚡ SAFE ZONE EVICTION WARP",
            x = s.x,
            y = s.y - 26f,
            color = Color.parseColor("#38BDF8"),
            maxLife = 1.25f,
          )
          if (s.isLocalPlayer) {
            sfx.playPrize()
            onPrizeToast("SAFE ZONE IDLE LIMIT (>15s) — TELEPORTED TO OPEN SECTOR")
          }
        }
      } else {
        s.safeZoneStayTimer = 0f
        val curSpeed = hypot(s.vx, s.vy)
        val hasThrustInput = if (s.isLocalPlayer) (leftStickActive && leftStickMag > 0.08f) else (curSpeed > 12f)
        val isMoving = curSpeed > 12f
        if (hasThrustInput || isMoving) {
          s.playfieldIdleTimer = 0f
          s.isIdleDraining = false
        } else {
          s.playfieldIdleTimer += rawDt
          if (s.playfieldIdleTimer >= RiftCalibrationState.playfieldIdleLimitSec) {
            s.isIdleDraining = true
            s.flux -= s.maxFlux * RiftCalibrationState.playfieldIdleDrainPctPerSec * rawDt
            if (s.flux <= 0f) {
              destroyShipFromIdleDrain(s)
              continue
            }
          }
        }
      }

      s.inSafeZone = currentlyInSafe
      s.weaponsLocked = currentlyInSafe
      s.canFire = !currentlyInSafe
      s.weaponState = if (currentlyInSafe) "DISARMED" else "ARMED"
      if (currentlyInSafe) {
        s.wasInSafeZone = true
        s.phaseShieldTimer = 3.0f
      } else {
        if (s.wasInSafeZone) {
          s.wasInSafeZone = false
          s.phaseShieldTimer = 3.0f
          if (s.isLocalPlayer) {
            onPrizeToast("SAFE ZONE SHIELD ACTIVE (3.0s // 75% DMG REDUCTION)")
          }
        } else if (s.phaseShieldTimer > 0f) {
          s.phaseShieldTimer = max(0f, s.phaseShieldTimer - dt)
        }
      }

      val surgeMult = 1f + s.overchargeSurgeStacks * 0.08f
      if (s.empTimer > 0f) {
        s.empTimer = max(0f, s.empTimer - dt)
      } else if (!s.isIdleDraining && !s.isCloaked) {
        val safeBonus = if (currentlyInSafe) 2.5f else 1.0f
        s.flux = min(s.maxFlux, s.flux + s.spec.rechargeRate * surgeMult * safeBonus * dt)
      }

      val rocketSpeedMult = if (s.rocketBoostTimer > 0f) 1.65f else 1.0f
      val maxSp =
        (if (s.afterburning) s.spec.afterburnerSpeed else s.spec.topSpeed) * surgeMult * rocketSpeedMult
      val curSp = hypot(s.vx, s.vy)
      if (curSp > maxSp) {
        s.vx = (s.vx / curSp) * maxSp
        s.vy = (s.vy / curSp) * maxSp
      }

      s.x += s.vx * dt
      s.y += s.vy * dt
      resolveShipWallBounce(s)

      val postBounceSpeed = hypot(s.vx, s.vy)
      if (postBounceSpeed > 2f) {
        s.angle = atan2(s.vy, s.vx)
      } else if (s.isLocalPlayer && leftStickActive && leftStickMag > 0.08f) {
        s.angle = leftStickAngle
      }

      // Collect 89%-Ship-Size Glowing Buff Pickups (11-Buff Pool: 0..10)
      for (gIdx in greens.indices.reversed()) {
        val gr = greens[gIdx]
        val pickupDist = if (s.isLocalPlayer) s.radius + 60f else s.radius + 20f
        if (hypot(s.x - gr.x, s.y - gr.y) < pickupDist) {
          greens.removeAt(gIdx)
          if (RiftCalibrationState.buffRespawnFrequency > 0.001f) {
            val jitter = (Random.nextFloat() * 1.5f - 0.75f)
            val baseDelay = (RiftCalibrationState.buffBaseRespawnDelay + jitter).coerceAtLeast(0.5f)
            val effectiveDelay = baseDelay / RiftCalibrationState.buffRespawnFrequency
            pendingBuffSpawns.add(effectiveDelay)
          }
          s.bounty += 15
          when (gr.buffType) {
            0 -> grantSpecialBuffWeapon(s, "BURST", 1)
            1 -> grantSpecialBuffWeapon(s, "SHOTGUN", 2)
            2 -> grantSpecialBuffWeapon(s, "MISSILE", 1)
            3 -> grantSpecialBuffWeapon(s, "DECOY", 1)
            4 -> grantSpecialBuffWeapon(s, "BRICK", 1)
            5 -> grantSpecialBuffWeapon(s, "ROCKET", 1)
            6 -> s.flux = s.maxFlux
            7 -> s.repels = min(6, s.repels + 1)
            8 -> {
              s.multiShotCharges += 25
              s.multifireOn = true
            }
            9 -> {
              s.bouncingBulletsShots += 30
            }
            else -> {
              s.overchargeSurgeStacks = min(3, s.overchargeSurgeStacks + 1)
            }
          }
          if (s.isLocalPlayer) {
            buffVersion++
            sfx.playPrize()
            greensCollected++
            val msg =
              when (gr.buffType) {
                0 -> {
                  spawnPooledFct("💥 BURST x${s.specialWeaponCharges} (0❖)", s.x, s.y - 24f, Color.parseColor("#F97316"))
                  "SPECIAL BUFF: BURST (+1 CHARGE • ${s.specialWeaponCharges} TOTAL • 0❖)"
                }
                1 -> {
                  spawnPooledFct("🔥 SHOTGUN x${s.specialWeaponCharges} (0❖)", s.x, s.y - 24f, Color.parseColor("#FB7185"))
                  "SPECIAL BUFF: 3-WAVE SHOTGUN (+2 CHARGES • ${s.specialWeaponCharges} TOTAL • 0❖)"
                }
                2 -> {
                  spawnPooledFct("🎯 MISSILE x${s.specialWeaponCharges} (0❖)", s.x, s.y - 24f, Color.parseColor("#EF4444"))
                  "SPECIAL BUFF: LOCK-ON MISSILE (+1 CHARGE • ${s.specialWeaponCharges} TOTAL • 0❖)"
                }
                3 -> {
                  spawnPooledFct("👥 DECOY x${s.specialWeaponCharges} (0❖)", s.x, s.y - 24f, Color.parseColor("#2DD4BF"))
                  "SPECIAL BUFF: DECOY HOLOGRAM (+1 CHARGE • ${s.specialWeaponCharges} TOTAL • 0❖)"
                }
                4 -> {
                  spawnPooledFct("🧱 BRICK x${s.specialWeaponCharges} (0❖)", s.x, s.y - 24f, Color.parseColor("#38BDF8"))
                  "SPECIAL BUFF: BRICK ENERGY BARRIER (+1 CHARGE • ${s.specialWeaponCharges} TOTAL • 0❖)"
                }
                5 -> {
                  spawnPooledFct("🚀 ROCKET x${s.specialWeaponCharges} (0❖)", s.x, s.y - 24f, Color.parseColor("#F43F5E"))
                  "SPECIAL BUFF: ROCKET HYPER-BOOST (+1 CHARGE • ${s.specialWeaponCharges} TOTAL • 0❖)"
                }
                6 -> {
                  spawnPooledFct("❖ FULL FLUX +15 BTY", s.x, s.y - 24f, Color.parseColor("#10B981"))
                  "INSTANT BUFF: 100% FULL FLUX CHARGE +15 BOUNTY"
                }
                7 -> {
                  spawnPooledFct("🛡️ REPEL +1 (${s.repels})", s.x, s.y - 24f, Color.parseColor("#00F5D4"))
                  "INSTANT BUFF: +1 REPEL CHARGE (${s.repels} TOTAL)"
                }
                8 -> {
                  val label =
                    if (s.spec.supportsNativeMultifire) {
                      "🔱 5-STREAM OVERDRIVE (${s.multiShotCharges} SHOTS)"
                    } else {
                      "🔱 MULTI-SHOT +25 (${s.multiShotCharges} SHOTS)"
                    }
                  spawnPooledFct(label, s.x, s.y - 24f, Color.parseColor("#FACC15"))
                  if (s.spec.supportsNativeMultifire) {
                    "SHOT-LIMITED MULTI-SHOT: +25 SHOTS (+25% DMG / 5-STREAM OVERDRIVE!)"
                  } else {
                    "SHOT-LIMITED MULTI-SHOT: +25 SHOTS (${s.multiShotCharges} TOTAL)"
                  }
                }
                9 -> {
                  spawnPooledFct("⛛ BOUNCE BULLETS (${s.bouncingBulletsShots})", s.x, s.y - 24f, Color.parseColor("#A855F7"))
                  "SHOT-LIMITED AUGMENT: BOUNCING BULLETS +30 SHOTS (${s.bouncingBulletsShots} TOTAL)"
                }
                else -> {
                  val pct = s.overchargeSurgeStacks * 8
                  spawnPooledFct("⚡ OVERCHARGE SURGE x${s.overchargeSurgeStacks} (+$pct%)", s.x, s.y - 24f, Color.parseColor("#F59E0B"))
                  "LIFE HULL SURGE: OVERCHARGE SURGE x${s.overchargeSurgeStacks} (+$pct% SPEED, THRUST & RECHARGE)"
                }
              }
            onPrizeToast(msg)
            allTimeBountyPeak = max(allTimeBountyPeak, localPlayer.bounty.toLong())
            awardPilotXp(
              buffXp = 35f,
              bountyEarnedXp = 30f,
              notifyChange = true,
            )
          }
        }
      }
    }

    // 3. Step Bullets (Safe Zone Boundary Despawn + Brick Walls + Decoys + Friendly Fire disabled)
    for (i in bullets.indices.reversed()) {
      val b = bullets[i]
      b.life -= dt
      if (b.life <= 0f) {
        bullets.removeAt(i)
        continue
      }
      val nx = b.x + b.vx * dt
      val ny = b.y + b.vy * dt
      if (isInsideSafeZone(nx, ny) || isInsideSafeZone(b.x, b.y)) {
        bullets.removeAt(i)
        if (particles.size < 90) {
          particles.add(
            ParticleEntity(
              x = nx,
              y = ny,
              vx = -b.vx * 0.1f,
              vy = -b.vy * 0.1f,
              life = 0.15f,
              maxLife = 0.15f,
              size = 2.5f,
              color = Color.parseColor("#38BDF8"),
            )
          )
        }
        continue
      }
      var hitWall = false
      for (w in walls) {
        if (nx >= w.x && nx <= w.x + w.w && ny >= w.y && ny <= w.y + w.h) {
          hitWall = true
          if (b.bouncesLeft > 0) {
            b.bouncesLeft--
            val prevInX = b.x >= w.x && b.x <= w.x + w.w
            if (!prevInX) b.vx = -b.vx else b.vy = -b.vy
          } else {
            bullets.removeAt(i)
          }
          break
        }
      }
      if (!hitWall) {
        for (bw in brickWalls) {
          if (nx >= bw.x && nx <= bw.x + bw.w && ny >= bw.y && ny <= bw.y + bw.h) {
            hitWall = true
            if (b.bouncesLeft > 0) {
              b.bouncesLeft--
              val prevInX = b.x >= bw.x && b.x <= bw.x + bw.w
              if (!prevInX) b.vx = -b.vx else b.vy = -b.vy
            } else {
              bullets.removeAt(i)
            }
            break
          }
        }
      }
      if (hitWall) continue
      b.x = nx
      b.y = ny

      // Check collision with enemy Decoy holograms (absorbs fire!)
      var absorbedByDecoy = false
      for (dIdx in decoys.indices.reversed()) {
        val dec = decoys[dIdx]
        if (dec.ownerId != b.ownerId && dec.freq != b.freq && hypot(dec.x - b.x, dec.y - b.y) < 34f) {
          bullets.removeAt(i)
          absorbedByDecoy = true
          spawnExplosion(dec.x, dec.y, Color.parseColor("#2DD4BF"))
          decoys.removeAt(dIdx)
          break
        }
      }
      if (absorbedByDecoy) continue

      for (s in ships) {
        if (!isBulletAlliedWithShip(b, s) && s.respawnTimer <= 0f) {
          if (hypot(s.x - b.x, s.y - b.y) < s.radius + 8f) {
            val incAngle = atan2(b.y - s.y, b.x - s.x)
            applyProjectileDamage(
              target = s,
              attackerId = b.ownerId,
              attackerIsHuman = b.ownerIsHuman,
              rawDamage = b.damage,
              impactAngle = incAngle,
              isHeavyImpact = b.isHeavyBurst,
              isEmp = false,
            )
            bullets.removeAt(i)
            break
          }
        }
      }
    }

    // 4. Step Bombs, Proximity Flak, Mines & Lock-On Bouncing Missiles
    val goliathTopSpeed = RiftCodex.getShipById(4).topSpeed // 380 PX/S exact cap for Lock-On Bouncing Missile
    for (i in bombs.indices.reversed()) {
      val b = bombs[i]
      b.life -= dt
      if (b.life <= 0f) {
        val isMissile = b.type == "MISSILE"
        val expRadius =
          when (b.type) {
            "MISSILE" -> 155f
            "SIEGE" -> 220f
            "EMP" -> 240f
            "PROX", "MINE" -> (b.radius + 65f) * 1.35f
            else -> b.radius * 3.8f
          }
        val expDmg = if (isMissile) b.damage else b.damage * 0.5f
        if (b.type == "PROX") {
          spawnProximityFlakShrapnel(b)
        }
        for (s in ships) {
          if (!isBombAlliedWithShip(b, s) && s.respawnTimer <= 0f) {
            val dist = hypot(s.x - b.x, s.y - b.y)
            if (dist < s.radius + expRadius) {
              val falloff = if (isMissile) (1f - (dist / (s.radius + expRadius)) * 0.35f).coerceIn(0.55f, 1f) else 1f
              val incAngle = atan2(b.y - s.y, b.x - s.x)
              applyProjectileDamage(
                target = s,
                attackerId = b.ownerId,
                attackerIsHuman = b.ownerIsHuman,
                rawDamage = expDmg * falloff,
                impactAngle = incAngle,
                isHeavyImpact = true,
                isEmp = b.type == "EMP",
              )
            }
          }
        }
        if (isMissile && hypot(b.x - localPlayer.x, b.y - localPlayer.y) < 1100f) {
          sfx.playExplosion()
        }
        spawnExplosion(
          b.x,
          b.y,
          when (b.type) {
            "EMP" -> Color.parseColor("#B388FF")
            "MISSILE" -> Color.parseColor("#EF4444")
            else -> Color.parseColor("#F59E0B")
          },
        )
        bombs.removeAt(i)
        continue
      }

      // Lock-On Bouncing Missile homing (checks Decoy holograms first, then uncloaked ships!)
      if (b.type == "MISSILE") {
        val lockedDecoy = decoys.firstOrNull { it.id == b.targetShipId }
        if (lockedDecoy != null) {
          val desiredAngle = atan2(lockedDecoy.y - b.y, lockedDecoy.x - b.x)
          val curAngle = atan2(b.vy, b.vx)
          val diff = atan2(sin(desiredAngle - curAngle), cos(desiredAngle - curAngle))
          val maxTurn = 4.8f * dt
          val newAngle = curAngle + diff.coerceIn(-maxTurn, maxTurn)
          b.vx = cos(newAngle) * goliathTopSpeed
          b.vy = sin(newAngle) * goliathTopSpeed
        } else {
          var lockedTarget = ships.firstOrNull {
            it.id == b.targetShipId && it.respawnTimer <= 0f && !it.isCloaked && !isInsideSafeZone(it.x, it.y)
          }
          if (lockedTarget == null) {
            var nearestDist = 1800f
            for (cand in ships) {
              if (
                !isBombAlliedWithShip(b, cand) &&
                  cand.respawnTimer <= 0f &&
                  !cand.isCloaked &&
                  !isInsideSafeZone(cand.x, cand.y)
              ) {
                val d = hypot(cand.x - b.x, cand.y - b.y)
                if (d < nearestDist) {
                  nearestDist = d
                  lockedTarget = cand
                }
              }
            }
            b.targetShipId = lockedTarget?.id ?: ""
          }
          if (lockedTarget != null) {
            lockedTarget.missileLockWarningTimer = max(lockedTarget.missileLockWarningTimer, 0.22f)
            if (
              (lockedTarget.isLocalPlayer || b.ownerId == localPlayer.id) &&
                lockedTarget.missileBeepCooldown <= 0f
            ) {
              lockedTarget.missileBeepCooldown = 0.38f
              sfx.playMissileLockBeep()
            }
            val desiredAngle = atan2(lockedTarget.y - b.y, lockedTarget.x - b.x)
            val curAngle = atan2(b.vy, b.vx)
            val diff = atan2(sin(desiredAngle - curAngle), cos(desiredAngle - curAngle))
            val maxTurn = 4.8f * dt
            val newAngle = curAngle + diff.coerceIn(-maxTurn, maxTurn)
            b.vx = cos(newAngle) * goliathTopSpeed
            b.vy = sin(newAngle) * goliathTopSpeed
          } else {
            val curSp = hypot(b.vx, b.vy).coerceAtLeast(1f)
            b.vx = (b.vx / curSp) * goliathTopSpeed
            b.vy = (b.vy / curSp) * goliathTopSpeed
          }
        }
      }

      val nx = b.x + b.vx * dt
      val ny = b.y + b.vy * dt
      if (isInsideSafeZone(nx, ny) || isInsideSafeZone(b.x, b.y)) {
        bombs.removeAt(i)
        continue
      }
      var hitWall = false

      if (!b.passWalls && (b.vx != 0f || b.vy != 0f)) {
        for (w in walls) {
          if (nx >= w.x && nx <= w.x + w.w && ny >= w.y && ny <= w.y + w.h) {
            hitWall = true
            if (b.bouncesLeft > 0) {
              b.bouncesLeft--
              val prevInX = b.x >= w.x && b.x <= w.x + w.w
              val bounceRestitution = if (b.type == "MISSILE" || b.type == "BOUNCE") 1.0f else 0.85f
              if (!prevInX) b.vx = -b.vx * bounceRestitution else b.vy = -b.vy * bounceRestitution
            } else {
              if (b.type == "PROX") spawnProximityFlakShrapnel(b)
              bombs.removeAt(i)
              spawnExplosion(
                b.x,
                b.y,
                when (b.type) {
                  "EMP" -> Color.parseColor("#B388FF")
                  "MISSILE" -> Color.parseColor("#EF4444")
                  else -> Color.parseColor("#F59E0B")
                },
              )
            }
            break
          }
        }
        if (!hitWall) {
          for (bw in brickWalls) {
            if (nx >= bw.x && nx <= bw.x + bw.w && ny >= bw.y && ny <= bw.y + bw.h) {
              hitWall = true
              if (b.bouncesLeft > 0) {
                b.bouncesLeft--
                val prevInX = b.x >= bw.x && b.x <= bw.x + bw.w
                val bounceRestitution = if (b.type == "MISSILE" || b.type == "BOUNCE") 1.0f else 0.85f
                if (!prevInX) b.vx = -b.vx * bounceRestitution else b.vy = -b.vy * bounceRestitution
              } else {
                if (b.type == "PROX") spawnProximityFlakShrapnel(b)
                bombs.removeAt(i)
                spawnExplosion(b.x, b.y, Color.parseColor("#38BDF8"))
              }
              break
            }
          }
        }
      }
      if (hitWall) continue
      b.x = nx
      b.y = ny

      // Check Decoy hologram soaking bombs/missiles
      var soakedByDecoy = false
      for (dIdx in decoys.indices.reversed()) {
        val dec = decoys[dIdx]
        if (dec.ownerId != b.ownerId && dec.freq != b.freq && hypot(dec.x - b.x, dec.y - b.y) < 42f) {
          soakedByDecoy = true
          spawnExplosion(dec.x, dec.y, Color.parseColor("#2DD4BF"))
          decoys.removeAt(dIdx)
          bombs.removeAt(i)
          break
        }
      }
      if (soakedByDecoy) continue

      val triggerRadius =
        when (b.type) {
          "PROX" -> 65f // 7-Valkyrie 65px proximity fuse
          "MINE" -> b.radius + 28f
          "MISSILE" -> b.radius + 10f
          else -> b.radius
        }
      for (s in ships) {
        if (!isBombAlliedWithShip(b, s) && s.respawnTimer <= 0f) {
          if (hypot(s.x - b.x, s.y - b.y) < s.radius + triggerRadius) {
            if (b.type == "PROX") {
              spawnProximityFlakShrapnel(b)
            }
            val blastRadius =
              when (b.type) {
                "EMP" -> 240f
                "SIEGE" -> 210f
                else -> s.radius + triggerRadius + 24f
              }
            for (victim in ships) {
              if (!isBombAlliedWithShip(b, victim) && victim.respawnTimer <= 0f) {
                val d = hypot(victim.x - b.x, victim.y - b.y)
                if (d <= blastRadius) {
                  val incAngle = atan2(b.y - victim.y, b.x - victim.x)
                  val scale = if (victim.id == s.id || b.type == "EMP") 1.0f else 0.65f
                  applyProjectileDamage(
                    target = victim,
                    attackerId = b.ownerId,
                    attackerIsHuman = b.ownerIsHuman,
                    rawDamage = b.damage * scale,
                    impactAngle = incAngle,
                    isHeavyImpact = true,
                    isEmp = b.type == "EMP",
                  )
                }
              }
            }
            spawnExplosion(
              b.x,
              b.y,
              when (b.type) {
                "EMP" -> Color.parseColor("#B388FF")
                "MISSILE" -> Color.parseColor("#EF4444")
                else -> Color.parseColor("#F59E0B")
              },
            )
            bombs.removeAt(i)
            break
          }
        }
      }
    }

    // 5. Step Particles, Shockwaves, Shield Ripples & Object-Pooled FCT
    for (i in particles.indices.reversed()) {
      val p = particles[i]
      p.life -= rawDt
      if (p.life <= 0f) {
        particles.removeAt(i)
        continue
      }
      p.x += p.vx * dt
      p.y += p.vy * dt
    }

    for (i in shockwaves.indices.reversed()) {
      val sw = shockwaves[i]
      sw.r += 480f * dt
      if (sw.r >= sw.maxR) shockwaves.removeAt(i)
    }

    for (i in shieldRipples.indices.reversed()) {
      val sr = shieldRipples[i]
      sr.life -= rawDt
      if (sr.life <= 0f) shieldRipples.removeAt(i)
    }

    for (i in floatingPopups.indices.reversed()) {
      val fp = floatingPopups[i]
      fp.elapsed += rawDt
      fp.y += fp.vy * rawDt
      // Elastic scale overshoot (1.0 -> 1.4 -> 1.0) in first 180ms
      val norm = (fp.elapsed / fp.maxLife).coerceIn(0f, 1f)
      fp.scale =
        if (norm < 0.14f) {
          1.0f + (norm / 0.14f) * 0.4f
        } else if (norm < 0.28f) {
          1.4f - ((norm - 0.14f) / 0.14f) * 0.4f
        } else {
          1.0f
        }
      if (fp.elapsed >= fp.maxLife) {
        fp.active = false
        floatingPopups.removeAt(i)
      }
    }

    // 6. Step Running Transparent Kill Feed Timers & High-Bounty Shutdown Banner
    for (i in killFeed.indices.reversed()) {
      val entry = killFeed[i]
      entry.life -= rawDt
      if (entry.life <= 0f) {
        killFeed.removeAt(i)
      }
    }
    if (highBountyShutdownTimer > 0f) {
      highBountyShutdownTimer = max(0f, highBountyShutdownTimer - rawDt)
      if (highBountyShutdownTimer <= 0f) {
        highBountyShutdownBanner = null
      }
    }

    // 7. Step Spatial Anomalies (45s Lifespan Cycling, Physics Fields, Despawn Particles & Respawn)
    stepSpatialAnomalies(dt, rawDt)
  }

  private fun stepSpatialAnomalies(dt: Float, rawDt: Float) {
    // 1. Update cooldown timers on all ships
    for (s in ships) {
      if (s.blackHoleImmunityTimer > 0f) {
        s.blackHoleImmunityTimer = max(0f, s.blackHoleImmunityTimer - dt)
      }
      if (s.wormholeCooldownTimer > 0f) {
        s.wormholeCooldownTimer = max(0f, s.wormholeCooldownTimer - dt)
      }
    }
    for (b in bullets) {
      if (b.wormholeCooldownTimer > 0f) {
        b.wormholeCooldownTimer = max(0f, b.wormholeCooldownTimer - dt)
      }
    }
    for (bm in bombs) {
      if (bm.wormholeCooldownTimer > 0f) {
        bm.wormholeCooldownTimer = max(0f, bm.wormholeCooldownTimer - dt)
      }
    }

    // 2. Step anomaly life, rotation, physics fields
    val expiredPairsToRespawn = mutableListOf<AnomalyType>()
    val expiredSinglesToRespawn = mutableListOf<AnomalyType>()

    for (aIdx in anomalies.indices.reversed()) {
      val anom = anomalies[aIdx]
      anom.life -= rawDt
      val spinSpeed = when (anom.type) {
        AnomalyType.PLASMA_RIFT -> 2.2f
        AnomalyType.REPULSOR_RIFT -> 1.8f
        AnomalyType.GRAVITY_RIFT -> -1.4f
        AnomalyType.PHASE_RIFT_A -> 2.5f
        AnomalyType.PHASE_RIFT_B -> -2.5f
      }
      anom.spinAngle = (anom.spinAngle + spinSpeed * dt) % (PI.toFloat() * 2f)

      // Ambient accretion disk particles
      if (Random.nextFloat() < 0.35f && particles.size < 95) {
        val pAngle = Random.nextFloat() * (PI.toFloat() * 2f)
        val pDist = anom.physicalRadius * (0.35f + Random.nextFloat() * 0.75f)
        val pColor = when (anom.type) {
          AnomalyType.PLASMA_RIFT -> if (Random.nextBoolean()) Color.parseColor("#EF4444") else Color.parseColor("#D946EF")
          AnomalyType.REPULSOR_RIFT -> if (Random.nextBoolean()) Color.WHITE else Color.parseColor("#38BDF8")
          AnomalyType.GRAVITY_RIFT -> if (Random.nextBoolean()) Color.parseColor("#C084FC") else Color.parseColor("#6366F1")
          else -> if (Random.nextBoolean()) Color.parseColor("#2DD4BF") else Color.parseColor("#38BDF8")
        }
        val swirlVx = -sin(pAngle) * 55f * (if (anom.type == AnomalyType.GRAVITY_RIFT) -1f else 1f)
        val swirlVy = cos(pAngle) * 55f * (if (anom.type == AnomalyType.GRAVITY_RIFT) -1f else 1f)
        particles.add(
          ParticleEntity(
            x = anom.x + cos(pAngle) * pDist,
            y = anom.y + sin(pAngle) * pDist,
            vx = swirlVx,
            vy = swirlVy,
            life = 0.32f,
            maxLife = 0.32f,
            size = 3.2f,
            color = pColor,
          )
        )
      }

      // Check lifespan expiration (45s)
      if (anom.life <= 0f) {
        // Despawn particle burst
        spawnExplosion(anom.x, anom.y, when (anom.type) {
          AnomalyType.PLASMA_RIFT -> Color.parseColor("#EF4444")
          AnomalyType.REPULSOR_RIFT -> Color.WHITE
          AnomalyType.GRAVITY_RIFT -> Color.parseColor("#818CF8")
          else -> Color.parseColor("#2DD4BF")
        })
        anomalies.removeAt(aIdx)
        if (anom.type == AnomalyType.PHASE_RIFT_A || anom.type == AnomalyType.PHASE_RIFT_B) {
          if (!expiredPairsToRespawn.contains(AnomalyType.PHASE_RIFT_A)) {
            expiredPairsToRespawn.add(AnomalyType.PHASE_RIFT_A)
          }
        } else {
          expiredSinglesToRespawn.add(anom.type)
        }
        continue
      }

      // Physics fields per Rift type
      when (anom.type) {
        AnomalyType.PLASMA_RIFT -> {
          // Damaging Plasma Rift (5x ship size = 180f radius):
          // Ships can fly straight through without losing velocity, but each pass-through inflicts 10% Maximum Flux damage.
          val tearRadius = anom.physicalRadius
          for (s in ships) {
            if (s.respawnTimer > 0f || isInsideSafeZone(s.x, s.y)) continue
            val dist = hypot(s.x - anom.x, s.y - anom.y)
            if (dist < tearRadius + s.radius * 0.4f && s.plasmaRiftTickTimer <= 0f) {
              s.plasmaRiftTickTimer = 0.65f
              val dmg = s.maxFlux * 0.10f
              s.flux -= dmg
              shieldRipples.add(
                ShieldRippleEntity(
                  shipId = s.id,
                  impactAngle = atan2(anom.y - s.y, anom.x - s.x),
                  intensity = 1.0f,
                  life = 0.30f,
                )
              )
              if (s.isLocalPlayer) {
                sfx.playBounce()
                addTrauma(0.24f)
                spawnPooledFct(
                  text = "☢ PLASMA RIFT -${dmg.toInt()}⚡ (10%)",
                  x = s.x,
                  y = s.y - 28f,
                  color = Color.parseColor("#EF4444"),
                )
              }
              if (s.flux <= 0f) {
                destroyShipFromPlasmaRift(s)
              }
            }
          }
        }

        AnomalyType.REPULSOR_RIFT -> {
          // Repulsion field radius: 520f (physical core radius 180f)
          val fieldRadius = 520f
          for (s in ships) {
            if (s.respawnTimer > 0f) continue
            val dx = s.x - anom.x
            val dy = s.y - anom.y
            val dist = hypot(dx, dy).coerceAtLeast(0.01f)
            if (dist < fieldRadius) {
              val nx = dx / dist
              val ny = dy / dist
              // Incoming approach velocity projected against radial inward vector
              val approachSpeed = -(s.vx * nx + s.vy * ny)
              val baseRepulse = 320f
              val slingshotBonus = if (approachSpeed > 0f) approachSpeed * 2.8f else 0f
              val forceMag = (baseRepulse + slingshotBonus) * (1f - dist / fieldRadius).coerceIn(0.2f, 1f)
              s.vx += nx * forceMag * dt
              s.vy += ny * forceMag * dt

              // Cap ship velocity to allowable afterburner envelope
              val maxAllowed = s.spec.afterburnerSpeed * 1.35f
              val curSp = hypot(s.vx, s.vy)
              if (curSp > maxAllowed) {
                s.vx = (s.vx / curSp) * maxAllowed
                s.vy = (s.vy / curSp) * maxAllowed
              }
              if (s.isLocalPlayer && approachSpeed > 60f) {
                sfx.playBounce()
              }
            }
          }

          // Deflect incoming projectiles outward
          for (b in bullets) {
            val dx = b.x - anom.x
            val dy = b.y - anom.y
            val dist = hypot(dx, dy).coerceAtLeast(0.01f)
            if (dist < fieldRadius * 0.75f) {
              val nx = dx / dist
              val ny = dy / dist
              val sp = hypot(b.vx, b.vy).coerceAtLeast(300f)
              b.vx += nx * 1400f * dt
              b.vy += ny * 1400f * dt
              val newSp = hypot(b.vx, b.vy).coerceAtLeast(1f)
              b.vx = (b.vx / newSp) * sp
              b.vy = (b.vy / newSp) * sp
            }
          }
          for (bm in bombs) {
            val dx = bm.x - anom.x
            val dy = bm.y - anom.y
            val dist = hypot(dx, dy).coerceAtLeast(0.01f)
            if (dist < fieldRadius * 0.75f) {
              val nx = dx / dist
              val ny = dy / dist
              val sp = hypot(bm.vx, bm.vy).coerceAtLeast(180f)
              bm.vx += nx * 900f * dt
              bm.vy += ny * 900f * dt
              val newSp = hypot(bm.vx, bm.vy).coerceAtLeast(1f)
              bm.vx = (bm.vx / newSp) * sp
              bm.vy = (bm.vy / newSp) * sp
            }
          }
        }

        AnomalyType.GRAVITY_RIFT -> {
          val pullRadius = 600f
          val coreRadius = anom.physicalRadius * 0.42f // ~75f core

          for (s in ships) {
            if (s.respawnTimer > 0f) continue
            val dx = anom.x - s.x
            val dy = anom.y - s.y
            val dist = hypot(dx, dy).coerceAtLeast(0.01f)

            if (dist < coreRadius) {
              // Contact with center core: Instant full stop (vx = 0, vy = 0), 1.75s escape immunity window
              if (s.blackHoleImmunityTimer <= 0f) {
                s.vx = 0f
                s.vy = 0f
                s.blackHoleImmunityTimer = 1.75f
                spawnExplosion(s.x, s.y, Color.parseColor("#C084FC"))
                if (s.isLocalPlayer) {
                  sfx.playBounce()
                  spawnPooledFct("⚠️ GRAVITY RIFT: FULL STOP", s.x, s.y - 32f, Color.parseColor("#A855F7"))
                }
              }
            } else if (dist < pullRadius) {
              // Only pull if not immune
              if (s.blackHoleImmunityTimer <= 0f) {
                val nx = dx / dist
                val ny = dy / dist
                val pullMag = 420f * (1f - dist / pullRadius).coerceIn(0.15f, 1f)
                s.vx += nx * pullMag * dt
                s.vy += ny * pullMag * dt
              }
            }
          }

          // Absorbs live projectiles touching the core
          for (bIdx in bullets.indices.reversed()) {
            val b = bullets[bIdx]
            if (hypot(b.x - anom.x, b.y - anom.y) < coreRadius) {
              bullets.removeAt(bIdx)
              spawnExplosion(b.x, b.y, Color.parseColor("#A855F7"))
            }
          }
          for (bmIdx in bombs.indices.reversed()) {
            val bm = bombs[bmIdx]
            if (hypot(bm.x - anom.x, bm.y - anom.y) < coreRadius) {
              bombs.removeAt(bmIdx)
              spawnExplosion(bm.x, bm.y, Color.parseColor("#A855F7"))
            }
          }
        }

        AnomalyType.PHASE_RIFT_A, AnomalyType.PHASE_RIFT_B -> {
          val portalRadius = anom.physicalRadius * 0.52f // ~93f trigger radius
          val partner = anomalies.firstOrNull { it.id == anom.partnerId }
          if (partner != null) {
            // Check ships entering Phase Rift
            for (s in ships) {
              if (s.respawnTimer > 0f) continue
              if (s.wormholeCooldownTimer <= 0f && hypot(s.x - anom.x, s.y - anom.y) < portalRadius + s.radius * 0.5f) {
                // Teleport to partner coordinates, preserving velocity vector and heading
                s.x = partner.x
                s.y = partner.y
                s.wormholeCooldownTimer = 1.0f
                // Teleport FX at entry and exit
                spawnExplosion(anom.x, anom.y, Color.parseColor("#2DD4BF"))
                spawnExplosion(partner.x, partner.y, Color.parseColor("#2DD4BF"))
                if (s.isLocalPlayer) {
                  sfx.playWarp()
                  spawnPooledFct("🌌 PHASE RIFT TRANSIT", s.x, s.y - 36f, Color.parseColor("#2DD4BF"))
                }
              }
            }

            // Check bullets entering Phase Rift
            for (b in bullets) {
              if (b.wormholeCooldownTimer <= 0f && hypot(b.x - anom.x, b.y - anom.y) < portalRadius) {
                b.x = partner.x
                b.y = partner.y
                b.wormholeCooldownTimer = 1.0f
              }
            }

            // Check bombs entering Phase Rift
            for (bm in bombs) {
              if (bm.wormholeCooldownTimer <= 0f && hypot(bm.x - anom.x, bm.y - anom.y) < portalRadius) {
                bm.x = partner.x
                bm.y = partner.y
                bm.wormholeCooldownTimer = 1.0f
              }
            }
          }
        }
      }
    }

    // Cleanly respawn expired Rifts on 45s cycles
    for (singleType in expiredSinglesToRespawn) {
      spawnSingleAnomaly(singleType)
    }
    if (expiredPairsToRespawn.isNotEmpty()) {
      // Remove any lingering orphan partner
      anomalies.removeAll { it.type == AnomalyType.PHASE_RIFT_A || it.type == AnomalyType.PHASE_RIFT_B }
      spawnPhaseRiftPair()
    }
  }
}
