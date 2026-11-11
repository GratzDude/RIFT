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
  val buffType: Int = 0, // 0=Full Charge, 1=Recharge, 2=Top Speed, 3=Multifire, 4=Bouncing Rails, 5=Burst, 6=Shotgun, 7=Lock-On Missile
  var vx: Float = 0f,
  var vy: Float = 0f,
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
  var specialWeaponType: String = if (spec.initialBursts > 0) "BURST" else "NONE", // "NONE", "BURST", "SHOTGUN", "MISSILE"
  var specialWeaponCharges: Int = spec.initialBursts,
  var shotgunWavesRemaining: Int = 0,
  var shotgunWaveTimer: Float = 0f,
  var shotgunAimAngle: Float = 0f,
  var rechargeStacks: Int = 0,
  var speedStacks: Int = 0,
  var bounceBulletsUpgrade: Int = 0,
  var bombTierUpgrade: Int = 0,
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
        "VORTEX ACE",
        "SOLARIS IX",
        "IRON WOLF",
        "NEXUS 7",
        "KESTREL 09",
        "CHRONO X",
        "VALKYRIE Z",
        "MANTA RAY",
        "AURELIA PRIME",
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
        buffType = Random.nextInt(8),
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
    localPlayer.specialWeaponType = if (spec.initialBursts > 0) "BURST" else "NONE"
    localPlayer.specialWeaponCharges = spec.initialBursts
    localPlayer.shotgunWavesRemaining = 0
    localPlayer.shotgunWaveTimer = 0f
    localPlayer.multifireOn = false
    buffVersion++
    syncLocalArmadaSelfSlot()
    onPrizeToast("HULL EQUIPPED: ${spec.id}-${spec.name.uppercase()} (${spec.maxFlux.toInt()}⚡)")
    return true
  }

  fun syncLocalPlayerCustomShipSpec() {
    if (localPlayer.shipId == 9) {
      val updatedSpec = RiftCodex.customShip9Spec
      val fluxRatio = (localPlayer.flux / localPlayer.maxFlux.coerceAtLeast(1f)).coerceIn(0.1f, 1f)
      localPlayer.spec = updatedSpec
      localPlayer.maxFlux = updatedSpec.maxFlux
      localPlayer.flux = (updatedSpec.maxFlux * fluxRatio).coerceAtLeast(200f)
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

  fun toggleMultifire(): Boolean {
    if (!localPlayer.spec.supportsMultifire) {
      onPrizeToast("MULTIFIRE NOT SUPPORTED ON THIS HULL")
      return false
    }
    localPlayer.multifireOn = !localPlayer.multifireOn
    buffVersion++
    onPrizeToast("MULTIFIRE: ${if (localPlayer.multifireOn) "ENABLED" else "DISABLED"}")
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
    if (ship.isLocalPlayer && safeZoneToastCooldown <= 0f) {
      safeZoneToastCooldown = 0.85f
      sfx.playMutedClick()
      onPrizeToast("SAFE ZONE RESTRICTED: WEAPONS DISARMED")
    }
  }

  /**
   * Phase 2.2: Quick-Tap Auto-Aim Helper — finds the closest hostile ship outside the Safe Zone within priority range.
   */
  fun findQuickTapAutoAimAngle(shooter: ShipEntity, maxRange: Float = 680f): Float {
    var closestHostile: ShipEntity? = null
    var closestDist = maxRange
    for (other in ships) {
      if (!areAllied(shooter, other) && other.respawnTimer <= 0f && !isInsideSafeZone(other.x, other.y)) {
        val d = hypot(other.x - shooter.x, other.y - shooter.y)
        if (d < closestDist) {
          closestDist = d
          closestHostile = other
        }
      }
    }
    return if (closestHostile != null) {
      val leadT = min(0.45f, closestDist / shooter.spec.primarySpeed)
      val tx = closestHostile.x + closestHostile.vx * leadT
      val ty = closestHostile.y + closestHostile.vy * leadT
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

    // Firing weapons outside Safe Zone immediately strips Exit Phase Shield
    if (ship.phaseShieldTimer > 0f) {
      ship.phaseShieldTimer = 0f
    }

    val spec = ship.spec
    val multiFactor = if (ship.multifireOn) 1.55f else 1.0f
    val cost = spec.primaryCost * multiFactor
    if (ship.flux <= cost + 1f) return

    selfDrainFlux(ship, cost)
    ship.gunTimer = spec.primaryCooldown

    if (ship.isLocalPlayer) {
      sfx.playLaser()
    }

    // Phase 2.2: Drag-Aiming along theta_2 vs. Quick-Tap Auto-Aim
    val baseFireAngle =
      if (ship.independentAimActive) {
        ship.weaponAngle
      } else if (ship.isLocalPlayer) {
        findQuickTapAutoAimAngle(ship)
      } else {
        ship.angle
      }
    ship.weaponAngle = baseFireAngle

    val streams = if (ship.multifireOn) spec.primaryStreams + 2 else spec.primaryStreams
    val spread = if (ship.multifireOn) max(0.12f, spec.primarySpreadRad * 1.7f) else spec.primarySpreadRad
    val bounces = spec.primaryBounces + ship.bounceBulletsUpgrade
    val isFriendlyToLocal = areAllied(localPlayer, ship)

    for (i in 0 until streams) {
      val offsetIndex = i - (streams - 1) / 2f
      val a = baseFireAngle + offsetIndex * spread
      val bx = ship.x + cos(a) * 44f
      val by = ship.y + sin(a) * 44f
      if (isInsideSafeZone(bx, by)) continue
      val bvx = ship.vx * 0.35f + cos(a) * spec.primarySpeed
      val bvy = ship.vy * 0.35f + sin(a) * spec.primarySpeed

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
          damage = spec.primaryDamage,
          bouncesLeft = bounces,
          life = 1.65f,
          color = if (isFriendlyToLocal) Color.parseColor("#38BDF8") else Color.parseColor("#EF4444"),
          isHeavyBurst = bounces > 0,
        )
      )
    }
  }

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

    if (ship.phaseShieldTimer > 0f) {
      ship.phaseShieldTimer = 0f
    }

    val spec = ship.spec
    if (!spec.hasBomb) return

    val cost = spec.bombCost
    if (ship.flux <= cost + 1f) return

    val fireAngle = aimAngle ?: if (ship.independentAimActive) ship.weaponAngle else ship.angle
    ship.weaponAngle = fireAngle

    selfDrainFlux(ship, cost)
    ship.bombTimer = spec.bombCooldown

    if (ship.isLocalPlayer) {
      sfx.playBomb()
    }

    if (spec.bombType == "MINE") {
      val mx = ship.x - cos(ship.angle) * 44f
      val my = ship.y - sin(ship.angle) * 44f
      if (isInsideSafeZone(mx, my)) return
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
          damage = spec.bombDamage * (1f + ship.bombTierUpgrade * 0.15f),
          bouncesLeft = 0,
          passWalls = false,
          life = 30.0f,
          radius = 20f,
        )
      )
      return
    }

    if (spec.bombType == "BURST") {
      // Built-in Secondary Burst (9th Custom Ship): 24-pellet radial bouncing shrapnel ring powered by Flux
      val count = 24
      val pelletDmg = (spec.bombDamage / 3.2f) * (1f + ship.bombTierUpgrade * 0.12f)
      for (i in 0 until count) {
        val a = (i.toFloat() / count) * (Math.PI.toFloat() * 2f)
        val spd = 880f
        bullets.add(
          BulletEntity(
            ownerId = ship.id,
            ownerCallsign = ship.callsign,
            ownerSyndicateTag = ship.syndicateTag,
            ownerArmadaName = ship.armadaName,
            ownerIsHuman = ship.isHuman,
            freq = ship.freq,
            x = ship.x + cos(a) * 24f,
            y = ship.y + sin(a) * 24f,
            vx = cos(a) * spd,
            vy = sin(a) * spd,
            damage = pelletDmg,
            bouncesLeft = 4,
            color = Color.parseColor("#FF7A18"),
            life = 2.6f,
            isHeavyBurst = true,
          )
        )
      }
      return
    }

    if (spec.bombType == "SHOTGUN") {
      // Built-in Secondary Shotgun (9th Custom Ship): 3-wave 60-degree cone blast powered by Flux
      ship.shotgunAimAngle = fireAngle
      fireShotgunWave(ship, fireAngle)
      ship.shotgunWavesRemaining = 2
      ship.shotgunWaveTimer = 0.085f
      return
    }

    if (spec.bombType == "MISSILE") {
      // Built-in Secondary Lock-On Bouncing Missile (9th Custom Ship): 1.5s homing capped to 4-Goliath top speed (420f)
      val mx = ship.x + cos(fireAngle) * 48f
      val my = ship.y + sin(fireAngle) * 48f
      if (isInsideSafeZone(mx, my)) return
      val missileMaxSpd = 420f
      val lockTarget = findMissileLockTarget(ship, fireAngle)
      val launchAngle =
        if (lockTarget != null) {
          atan2(lockTarget.y - ship.y, lockTarget.x - ship.x)
        } else {
          fireAngle
        }
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
          vx = cos(launchAngle) * missileMaxSpd,
          vy = sin(launchAngle) * missileMaxSpd,
          damage = spec.bombDamage * (1f + ship.bombTierUpgrade * 0.15f),
          bouncesLeft = spec.bombBounces.coerceAtLeast(3),
          passWalls = false,
          life = 1.5f,
          radius = 18f,
          targetShipId = lockTarget?.id ?: "",
        )
      )
      if (lockTarget != null) {
        lockTarget.missileLockWarningTimer = 1.5f
        if (ship.isLocalPlayer) {
          sfx.playMissileLockBeep()
          spawnPooledFct(
            text = "🎯 MISSILE LOCK: ${lockTarget.callsign}",
            x = ship.x,
            y = ship.y - 28f,
            color = Color.parseColor("#EF4444"),
          )
        }
      }
      return
    }

    val bx = ship.x + cos(fireAngle) * 52f
    val by = ship.y + sin(fireAngle) * 52f
    if (isInsideSafeZone(bx, by)) return

    val speed = 640f
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
        damage = spec.bombDamage * (1f + ship.bombTierUpgrade * 0.15f),
        bouncesLeft = spec.bombBounces,
        passWalls = false,
        life = 30.0f,
        radius = 17f,
      )
    )
  }

  /**
   * Stacking & Overwrite Inventory Rules for Special Buff Weapons:
   * - Same-Type Stacking: Picking up a Special Buff Weapon of the same type currently held adds to the charge count.
   * - Different-Type Overwrite: Picking up a Special Buff Weapon of a different type immediately replaces the held type and charges.
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

  /**
   * Finds the best hostile ship outside the Safe Zone to lock onto for the Lock-On Bouncing Missile.
   */
  fun findMissileLockTarget(shooter: ShipEntity, aimAngle: Float): ShipEntity? {
    var bestTarget: ShipEntity? = null
    var bestScore = Float.MAX_VALUE
    for (other in ships) {
      if (!areAllied(shooter, other) && other.respawnTimer <= 0f && !isInsideSafeZone(other.x, other.y)) {
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

  fun activateUtility(type: String) {
    if (localPlayer.respawnTimer > 0f) return
    if (isInsideSafeZone(localPlayer.x, localPlayer.y)) {
      triggerSafeZoneWeaponLockout(localPlayer)
      return
    }
    if (localPlayer.phaseShieldTimer > 0f) {
      localPlayer.phaseShieldTimer = 0f
    }
    val resolvedType = if (type == "SPECIAL") localPlayer.specialWeaponType else type
    when (resolvedType) {
      "BURST", "SHOTGUN", "MISSILE" -> {
        if (localPlayer.specialWeaponCharges <= 0 || localPlayer.specialWeaponType == "NONE") return
        val fluxCost = 260f
        if (localPlayer.flux <= fluxCost) {
          onPrizeToast("INSUFFICIENT FLUX FOR $resolvedType (260⚡)")
          return
        }
        localPlayer.specialWeaponCharges--
        val depleted = localPlayer.specialWeaponCharges <= 0
        if (depleted) {
          localPlayer.specialWeaponCharges = 0
          localPlayer.specialWeaponType = "NONE"
        }
        buffVersion++
        selfDrainFlux(localPlayer, fluxCost)

        when (resolvedType) {
          "BURST" -> {
            sfx.playLaser()
            addTrauma(0.25f)
            val count = 24
            for (i in 0 until count) {
              val a = (PI.toFloat() * 2f * i) / count
              bullets.add(
                BulletEntity(
                  ownerId = localPlayer.id,
                  ownerCallsign = localPlayer.callsign,
                  ownerSyndicateTag = localPlayer.syndicateTag,
                  ownerArmadaName = localPlayer.armadaName,
                  ownerIsHuman = true,
                  freq = localPlayer.freq,
                  x = localPlayer.x + cos(a) * 44f,
                  y = localPlayer.y + sin(a) * 44f,
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
          "SHOTGUN" -> {
            addTrauma(0.22f)
            val aimAngle =
              if (localPlayer.independentAimActive) {
                localPlayer.weaponAngle
              } else {
                findQuickTapAutoAimAngle(localPlayer)
              }
            localPlayer.weaponAngle = aimAngle
            fireShotgunWave(localPlayer, aimAngle)
            localPlayer.shotgunWavesRemaining = 2
            localPlayer.shotgunWaveTimer = 0.085f
            localPlayer.shotgunAimAngle = aimAngle
          }
          "MISSILE" -> {
            sfx.playBomb()
            addTrauma(0.20f)
            val aimAngle =
              if (localPlayer.independentAimActive) {
                localPlayer.weaponAngle
              } else {
                findQuickTapAutoAimAngle(localPlayer, maxRange = 1600f)
              }
            val target = findMissileLockTarget(localPlayer, aimAngle)
            val launchAngle =
              if (target != null) {
                atan2(target.y - localPlayer.y, target.x - localPlayer.x)
              } else {
                aimAngle
              }
            val goliathTopSpeed = RiftCodex.getShipById(4).topSpeed // Exact 4-Goliath top speed (420f)
            val mx = localPlayer.x + cos(launchAngle) * 48f
            val my = localPlayer.y + sin(launchAngle) * 48f
            if (!isInsideSafeZone(mx, my)) {
              bombs.add(
                BombEntity(
                  ownerId = localPlayer.id,
                  ownerCallsign = localPlayer.callsign,
                  ownerSyndicateTag = localPlayer.syndicateTag,
                  ownerArmadaName = localPlayer.armadaName,
                  ownerIsHuman = true,
                  freq = localPlayer.freq,
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
                sfx.playMissileLockBeep()
                spawnPooledFct(
                  text = "🎯 MISSILE LOCK: ${target.callsign}",
                  x = localPlayer.x,
                  y = localPlayer.y - 28f,
                  color = Color.parseColor("#EF4444"),
                )
              }
            }
          }
        }
      }
      "REPEL" -> {
        if (localPlayer.repels <= 0) return
        if (localPlayer.flux <= 320f) {
          onPrizeToast("INSUFFICIENT FLUX FOR REPEL (320⚡)")
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
        // 4. Repel ALL buffs (GreenCrates) so they blast outward and smoothly damp to a stop
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
  }

  /**
   * Phase 2.2: Exact 2-Segment Line-Intersection Raycast Engine (raycastWalls / intersectRaySegment)
   * against arena wall segments for ricochet/bounce ordnance trajectory prediction.
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
    var remainingBounces = max(1, maxBounces)

    for (seg in 0..remainingBounces) {
      var closestHit: RaycastHit? = null
      var bestT = segmentMaxLen

      for (w in walls) {
        val left = w.x
        val right = w.x + w.w
        val top = w.y
        val bottom = w.y + w.h

        // Top edge (normal 0, -1)
        intersectRaySegment(curX, curY, dirX, dirY, left, top, right, top, 0f, -1f)?.let {
          if (it.t < bestT) {
            bestT = it.t
            closestHit = it
          }
        }
        // Bottom edge (normal 0, 1)
        intersectRaySegment(curX, curY, dirX, dirY, left, bottom, right, bottom, 0f, 1f)?.let {
          if (it.t < bestT) {
            bestT = it.t
            closestHit = it
          }
        }
        // Left edge (normal -1, 0)
        intersectRaySegment(curX, curY, dirX, dirY, left, top, left, bottom, -1f, 0f)?.let {
          if (it.t < bestT) {
            bestT = it.t
            closestHit = it
          }
        }
        // Right edge (normal 1, 0)
        intersectRaySegment(curX, curY, dirX, dirY, right, top, right, bottom, 1f, 0f)?.let {
          if (it.t < bestT) {
            bestT = it.t
            closestHit = it
          }
        }
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

  private fun resolveShipWallBounce(ship: ShipEntity) {
    for (w in walls) {
      val closestX = ship.x.coerceIn(w.x, w.x + w.w)
      val closestY = ship.y.coerceIn(w.y, w.y + w.h)
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

    // Anti-Spawn-Camp 3-Second Exit Phase Shield reduces incoming damage by 75%
    val shieldAbsorbed = target.phaseShieldTimer > 0f
    val effectiveDamage = if (shieldAbsorbed) rawDamage * 0.25f else rawDamage

    if (isEmp) {
      target.empTimer = 2.8f
    }

    // Removed ship silhouette white flashing when hit; trigger directional shield impact ripple
    target.hitFlashTimer = 0f
    shieldRipples.add(
      ShieldRippleEntity(
        shipId = target.id,
        impactAngle = impactAngle,
        intensity = if (isHeavyImpact) 1.0f else 0.78f,
        life = 0.28f,
      )
    )

    // Phase 3.4: Hit-Stop Freeze & Non-Linear Camera Trauma Shake when localPlayer is attacker or target
    if (attackerId == localPlayer.id || target.isLocalPlayer) {
      if (isHeavyImpact) {
        triggerHitStop(2)
        addTrauma(0.45f)
      } else {
        addTrauma(0.08f)
      }
      // Spawn Object-Pooled FCT with elastic scale overshoot
      val fctColor =
        when {
          shieldAbsorbed -> Color.parseColor("#38BDF8") // #38bdf8 shield absorb
          isHeavyImpact -> Color.parseColor("#F59E0B") // #f59e0b crit/heavy ordnance
          else -> Color.parseColor("#F1F5F9") // #f1f5f9 kinetic
        }
      spawnPooledFct(
        text = "-${effectiveDamage.toInt()}",
        x = target.x,
        y = target.y - 18f,
        color = fctColor,
      )
    }

    // Phase 3: Track Damage Done XP (+0.10 XP per Flux damage) & Accuracy Achieved XP (+8 XP per hit landed, no shot minimums)
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

      // Phase 3.4: #10b981 emerald bounty kill / #f59e0b HVT kill FCT popup
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
        // Phase 3: Track Bounty Earned XP (+2 XP per bounty point) & Bounty Killed XP (+15% of victim's pre-death bounty)
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

    var desiredAngle: Float
    if (incomingThreatAngle != null) {
      // Immediate protective evasive burn
      desiredAngle = incomingThreatAngle
      bot.vx += cos(incomingThreatAngle) * bot.spec.thrust * 1.1f * dt
      bot.vy += sin(incomingThreatAngle) * bot.spec.thrust * 1.1f * dt
    } else if (nearestEnemy != null && nearestDist < 1200f) {
      val leadT = min(0.75f, nearestDist / bot.spec.primarySpeed)
      val tx = nearestEnemy.x + nearestEnemy.vx * leadT
      val ty = nearestEnemy.y + nearestEnemy.vy * leadT
      val aimAngle = atan2(ty - bot.y, tx - bot.x)
      bot.weaponAngle = aimAngle
      bot.independentAimActive = true

      // Maintain safe tactical standoff distance (380px..650px); retreat if enemy gets too close!
      if (nearestDist < 380f) {
        val backAngle = atan2(bot.y - nearestEnemy.y, bot.x - nearestEnemy.x)
        val strafeSign = if ((bot.aiSeed.toInt() and 1) == 0) 0.65f else -0.65f
        desiredAngle = backAngle + strafeSign
      } else if (nearestDist > 750f) {
        desiredAngle = aimAngle
      } else {
        // Orbit / strafe tangentially to avoid incoming fire
        val strafeAngle = aimAngle + (PI.toFloat() * 0.45f * if ((bot.aiSeed.toInt() and 1) == 0) 1f else -1f)
        desiredAngle = strafeAngle
      }

      if (nearestDist < 850f && bot.flux > bot.maxFlux * 0.28f) {
        firePrimaryGun(bot)
      }
      if (bot.spec.hasBomb && nearestDist in 260f..850f && bot.flux > bot.maxFlux * 0.50f) {
        fireSecondaryBomb(bot, aimAngle)
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

    // Smoothly align bot velocity with desired travel heading
    val sideDamp = 0.34.pow(dt.toDouble()).toFloat()
    bot.vx = bot.vx * sideDamp + cos(desiredAngle) * bot.spec.thrust * 0.88f * dt
    bot.vy = bot.vy * sideDamp + sin(desiredAngle) * bot.spec.thrust * 0.88f * dt
    if (hypot(bot.vx, bot.vy) > 2f) {
      bot.angle = atan2(bot.vy, bot.vx)
    }
  }

  fun stepSimulation(rawDt: Float) {
    if (safeZoneToastCooldown > 0f) {
      safeZoneToastCooldown = max(0f, safeZoneToastCooldown - rawDt)
    }

    // Phase 3.4: Camera Trauma Decay (lambda = 2.4 / s) & Hit-Stop Freeze (scales dt to 0.05 for 32ms)
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

    // 1. Local Player Thrust, Afterburner, Snap-Back Auto-Brake & Independent 360-Degree Weapon Aiming
    if (localPlayer.respawnTimer <= 0f) {
      val localInSafe = isInsideSafeZone(localPlayer.x, localPlayer.y)
      localPlayer.inSafeZone = localInSafe
      localPlayer.weaponsLocked = localInSafe
      localPlayer.canFire = !localInSafe
      localPlayer.weaponState = if (localInSafe) "DISARMED" else "ARMED"
      localPlayer.afterburning = false
      if (leftStickActive && leftStickMag > 0.08f) {
        autoBraking = false
        val isAfterburner = leftStickMag > 0.80f && localPlayer.flux > 25f
        val accel =
          localPlayer.spec.thrust *
            (1f + localPlayer.speedStacks * 0.06f) *
            (if (isAfterburner) 1.45f else leftStickMag)
        localPlayer.vx += cos(leftStickAngle) * accel * dt
        localPlayer.vy += sin(leftStickAngle) * accel * dt

        if (isAfterburner) {
          localPlayer.afterburning = true
          selfDrainFlux(localPlayer, 210f * dt)
        }
      } else if (autoBraking) {
        // Explicit [BRAKE] button retro-braking until stopped
        val brakeDamping = 0.22.pow(dt.toDouble()).toFloat()
        localPlayer.vx *= brakeDamping
        localPlayer.vy *= brakeDamping
        if (hypot(localPlayer.vx, localPlayer.vy) < 4f) {
          localPlayer.vx = 0f
          localPlayer.vy = 0f
          autoBraking = false
        }
      }

      // Independent 360-degree weapon aiming vector from right-side [FIRE] thumb stick
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

    // Step Repel-pushed Buff Icons (GreenCrates) with wall bounce and smooth damping to rest
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
    // If fewer crates than cap and not in zero-respawn with depleted crates, spawn up to cap
    if (RiftCalibrationState.buffRespawnFrequency > 0.001f || greens.isEmpty()) {
      while (greens.size + pendingBuffSpawns.size < targetBuffCap) {
        if (!spawnGreenCrate()) break
      }
    }
    // If more crates than cap, gracefully prune excess crates with particle puff
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

    // Phase 3: Track Public FFA Match Duration & Active Pilot Match Presence Time
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
          s.rechargeStacks = 0
          s.speedStacks = 0
          s.multifireOn = false
          s.bounceBulletsUpgrade = 0
          s.bombTierUpgrade = 0
          s.repels = s.spec.initialRepels
          s.specialWeaponType = if (s.spec.initialBursts > 0) "BURST" else "NONE"
          s.specialWeaponCharges = s.spec.initialBursts
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
          // Just exited Safe Zone -> grant 3.0-second exit phase shield!
          s.wasInSafeZone = false
          s.phaseShieldTimer = 3.0f
          if (s.isLocalPlayer) {
            onPrizeToast("SAFE ZONE SHIELD ACTIVE (3.0s // 75% DMG REDUCTION)")
          }
        } else if (s.phaseShieldTimer > 0f) {
          s.phaseShieldTimer = max(0f, s.phaseShieldTimer - dt)
        }
      }

      if (s.empTimer > 0f) {
        s.empTimer = max(0f, s.empTimer - dt)
      } else if (!s.isIdleDraining) {
        val safeBonus = if (currentlyInSafe) 2.5f else 1.0f
        val rechargeMult = 1f + s.rechargeStacks * 0.15f
        s.flux = min(s.maxFlux, s.flux + s.spec.rechargeRate * rechargeMult * safeBonus * dt)
      }

      val speedMult = 1f + s.speedStacks * 0.08f
      val maxSp = (if (s.afterburning) s.spec.afterburnerSpeed else s.spec.topSpeed) * speedMult
      val curSp = hypot(s.vx, s.vy)
      if (curSp > maxSp) {
        s.vx = (s.vx / curSp) * maxSp
        s.vy = (s.vy / curSp) * maxSp
      }

      s.x += s.vx * dt
      s.y += s.vy * dt
      resolveShipWallBounce(s)

      // Front of the ship always faces its movement direction
      val postBounceSpeed = hypot(s.vx, s.vy)
      if (postBounceSpeed > 2f) {
        s.angle = atan2(s.vy, s.vx)
      } else if (s.isLocalPlayer && leftStickActive && leftStickMag > 0.08f) {
        s.angle = leftStickAngle
      }

      // Collect 89%-Ship-Size Glowing Buff Pickups (0..7)
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
          s.flux = s.maxFlux
          when (gr.buffType) {
            0 -> {}
            1 -> s.rechargeStacks = min(3, s.rechargeStacks + 1)
            2 -> s.speedStacks = min(3, s.speedStacks + 1)
            3 -> s.multifireOn = true
            4 -> {
              s.bounceBulletsUpgrade = min(2, s.bounceBulletsUpgrade + 1)
              s.repels = min(5, s.repels + 1)
            }
            5 -> {
              s.bombTierUpgrade = min(3, s.bombTierUpgrade + 1)
              s.repels = min(5, s.repels + 1)
              grantSpecialBuffWeapon(s, "BURST", 1)
            }
            6 -> {
              grantSpecialBuffWeapon(s, "SHOTGUN", 2)
            }
            else -> {
              grantSpecialBuffWeapon(s, "MISSILE", 1)
            }
          }
          if (s.isLocalPlayer) {
            buffVersion++
            sfx.playPrize()
            greensCollected++
            val msg =
              when (gr.buffType) {
                0 -> {
                  spawnPooledFct("⚡ FULL FLUX +15 BTY", s.x, s.y - 24f, Color.parseColor("#10B981"))
                  "BUFF PICKUP: FULL FLUX CHARGE +15 BOUNTY"
                }
                1 -> {
                  spawnPooledFct("🔋 RECHARGE LVL ${s.rechargeStacks}", s.x, s.y - 24f, Color.parseColor("#38BDF8"))
                  "BUFF PICKUP: RECHARGE MATRIX UPGRADE (LVL ${s.rechargeStacks})"
                }
                2 -> {
                  spawnPooledFct("⏩ TOP SPEED LVL ${s.speedStacks}", s.x, s.y - 24f, Color.parseColor("#F59E0B"))
                  "BUFF PICKUP: TOP SPEED OVERDRIVE (LVL ${s.speedStacks})"
                }
                3 -> {
                  spawnPooledFct("🔱 MULTIFIRE ACTIVE", s.x, s.y - 24f, Color.parseColor("#FACC15"))
                  "BUFF PICKUP: MULTIFIRE SPREAD CALIBRATOR UNLOCKED"
                }
                4 -> {
                  spawnPooledFct("⛛ BOUNCE RAILS +${s.bounceBulletsUpgrade}", s.x, s.y - 24f, Color.parseColor("#A855F7"))
                  "BUFF PICKUP: BOUNCING RAILS (+${s.bounceBulletsUpgrade}) & +1 REPEL"
                }
                5 -> {
                  spawnPooledFct("💥 BURST x${s.specialWeaponCharges}", s.x, s.y - 24f, Color.parseColor("#F97316"))
                  "SPECIAL BUFF WEAPON: SHRAPNEL BURST (${s.specialWeaponCharges} CHARGES)"
                }
                6 -> {
                  spawnPooledFct("🔥 SHOTGUN x${s.specialWeaponCharges}", s.x, s.y - 24f, Color.parseColor("#FB7185"))
                  "SPECIAL BUFF WEAPON: 3-WAVE CONE SHOTGUN (${s.specialWeaponCharges} CHARGES)"
                }
                else -> {
                  spawnPooledFct("🎯 MISSILE x${s.specialWeaponCharges}", s.x, s.y - 24f, Color.parseColor("#EF4444"))
                  "SPECIAL BUFF WEAPON: LOCK-ON BOUNCING MISSILE (${s.specialWeaponCharges} CHARGES)"
                }
              }
            onPrizeToast(msg)
            allTimeBountyPeak = max(allTimeBountyPeak, localPlayer.bounty.toLong())
            // Phase 3: Track Buffs Collected XP (+35 XP) & Bounty Earned XP (+30 XP for +15 BTY)
            awardPilotXp(
              buffXp = 35f,
              bountyEarnedXp = 30f,
              notifyChange = true,
            )
          }
        }
      }
    }

    // 3. Step Bullets (Safe Zone Boundary Despawn + Friendly Fire strictly disabled!)
    for (i in bullets.indices.reversed()) {
      val b = bullets[i]
      b.life -= dt
      if (b.life <= 0f) {
        bullets.removeAt(i)
        continue
      }
      val nx = b.x + b.vx * dt
      val ny = b.y + b.vy * dt
      // Phase 1.1 Safe Zone Boundary Despawn: any projectile crossing the Safe Zone perimeter is immediately despawned
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
      if (hitWall) continue
      b.x = nx
      b.y = ny

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

    // 4. Step Bombs, Mines & Lock-On Bouncing Missiles (Safe Zone Boundary Despawn + Friendly Fire strictly disabled!)
    val goliathTopSpeed = RiftCodex.getShipById(4).topSpeed // 420f exact cap for Lock-On Bouncing Missile
    for (i in bombs.indices.reversed()) {
      val b = bombs[i]
      b.life -= dt
      if (b.life <= 0f) {
        // Active timed detonation! For MISSILE (1.5s timer), detonates in an explosive blast at full damage;
        // for 30s bombs/mines, detonates with 1.5x blast radius and 50% damage.
        val isMissile = b.type == "MISSILE"
        val expRadius =
          if (isMissile) {
            155f
          } else {
            (if (b.type == "PROX" || b.type == "MINE") b.radius + 28f else b.radius * 2.8f) * 1.5f
          }
        val expDmg = if (isMissile) b.damage else b.damage * 0.5f
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

      // Lock-On Bouncing Missile homing & lock-on warning indicator logic
      if (b.type == "MISSILE") {
        var lockedTarget = ships.firstOrNull {
          it.id == b.targetShipId && it.respawnTimer <= 0f && !isInsideSafeZone(it.x, it.y)
        }
        if (lockedTarget == null) {
          var nearestDist = 1800f
          for (cand in ships) {
            if (!isBombAlliedWithShip(b, cand) && cand.respawnTimer <= 0f && !isInsideSafeZone(cand.x, cand.y)) {
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

      val nx = b.x + b.vx * dt
      val ny = b.y + b.vy * dt
      // Phase 1.1 Safe Zone Boundary Despawn: any bomb/mine/missile crossing the Safe Zone perimeter is immediately despawned
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
              val bounceRestitution = if (b.type == "MISSILE") 1.0f else 0.85f
              if (!prevInX) b.vx = -b.vx * bounceRestitution else b.vy = -b.vy * bounceRestitution
            } else {
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
      }
      if (hitWall) continue
      b.x = nx
      b.y = ny

      val triggerRadius =
        when (b.type) {
          "PROX", "MINE" -> b.radius + 28f
          "MISSILE" -> b.radius + 10f
          else -> b.radius
        }
      for (s in ships) {
        if (!isBombAlliedWithShip(b, s) && s.respawnTimer <= 0f) {
          if (hypot(s.x - b.x, s.y - b.y) < s.radius + triggerRadius) {
            val incAngle = atan2(b.y - s.y, b.x - s.x)
            applyProjectileDamage(
              target = s,
              attackerId = b.ownerId,
              attackerIsHuman = b.ownerIsHuman,
              rawDamage = b.damage,
              impactAngle = incAngle,
              isHeavyImpact = true,
              isEmp = b.type == "EMP",
            )
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
