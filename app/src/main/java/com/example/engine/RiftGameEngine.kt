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
  var specialWeaponType: String = "NONE", // "NONE", "NOVA_RING", "SHATTER_CONE", "SEEKER_TORPEDO", "HOLO_PHANTOM", "STASIS_BARRIER", "HYPER_SURGE"
  var specialWeaponCharges: Int = 0,
  var shotgunWavesRemaining: Int = 0,
  var shotgunWaveTimer: Float = 0f,
  var shotgunAimAngle: Float = 0f,
  var multiShotCharges: Int = 0, // Shot-limited Multi-Shot (+25 per pickup, stackable)
  var hasBouncingBulletsUpgrade: Boolean = false, // Permanent-until-death +1 primary wall bounce upgrade
  var speedOverchargeStacks: Int = 0, // Stacks up to 3x (+8% Top Speed per stack until death)
  var thrustOverchargeStacks: Int = 0, // Stacks up to 3x (+8% Thrust per stack until death)
  var rechargeOverchargeStacks: Int = 0, // Stacks up to 3x (+8% Flux Recharge per stack until death)
  var sniperOverchargeReady: Boolean = false, // Viper / Phase Dash overcharges next rail shot (+35% vel & dmg)
  var isCloaked: Boolean = false, // Disruptor / Cloak+EMP stealth cloak (180⚡/s drain, hides ship & minimap dot)
  var rocketBoostTimer: Float = 0f, // 3.0s Hyper Surge boost (1.65x max speed at 0⚡)
  var phaseDashTimer: Float = 0f, // Active Phase Dash supersonic burst + combat dodge i-frame window
  var wasInSafeZone: Boolean = false,
  var inSafeZone: Boolean = false,
  var safeZoneLockoutTimer: Float = 0f, // 15.0s Safe Zone re-entry repulsion lockout timer
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
  var remoteFiringPrimary: Boolean = false,
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
  val ownerShipLabel: String = "Viper",
  val weaponLabel: String = "Primary Blaster",
)

data class BombEntity(
  val ownerId: String,
  val ownerCallsign: String,
  val ownerSyndicateTag: String,
  val ownerArmadaName: String,
  val ownerIsHuman: Boolean,
  val freq: Int,
  val type: String, // "BOUNCE", "SIEGE", "EMP", "PROX", "MINE", "MISSILE", "CUSTOM_ORB"
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
  val ownerShipLabel: String = "Ricochet",
  val weaponLabel: String = "Warhead",
  val customColor: Int = 0,
  val customEffect: CustomBuffEffect = CustomBuffEffect.NONE,
  val customBlastRadius: Float = 0f,
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
  var physicalRadius: Float = 180f, // Exactly 5x standard ship radius (36f * 5 = 180f)
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

data class PhaseAfterimageEntity(
  val shipId: Int,
  val x: Float,
  val y: Float,
  val angleRad: Float,
  val isAllied: Boolean,
  var life: Float,
  val maxLife: Float = 0.38f,
)

data class InterdictorBubbleEntity(
  val id: String,
  val ownerId: String,
  val ownerFreq: Int,
  val ownerShipLabel: String,
  val x: Float,
  val y: Float,
  var radius: Float,
  var life: Float,
  val maxLife: Float,
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

data class TrajectoryPoint(
  val x: Float,
  val y: Float,
  val isBounce: Boolean,
  val isWarp: Boolean = false,
)

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
  var onLifeRunEnded: (snapshot: com.example.data.PeakBountyRunSnapshot, hadPvp: Boolean) -> Unit = { _, _ -> },
  var onSyndicateContribution: (killsDelta: Long, bounty: Long, winsDelta: Long) -> Unit = { _, _, _ -> },
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
    RiftCalibrationState.buffTargetCountOverride?.let { return it.coerceAtLeast(0) }
    val modeCap =
      when (RiftCalibrationState.buffModeOverride.uppercase()) {
        "3V3" -> 12
        "5V5" -> 10
        "10V10" -> 20
        else -> RiftCalibrationState.buffCapFfa
      }
    return modeCap.coerceAtLeast(0)
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
  val phaseAfterimages = mutableListOf<PhaseAfterimageEntity>()
  val interdictorBubbles = mutableListOf<InterdictorBubbleEntity>()
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
  var riftShards: Long = 0L
  var highestMilestoneLevelClaimed: Int = 0
  var spentCustomizationPoints: Int = 0
  var unlockedExtraSlots: Int = 0
  var aiShipDesignTokens: Int = 0

  // Phase 3: Public FFA Match Duration & >50% Presence Ratio Tracking (60-minute public cycle = 3600s)
  var publicMatchDurationSec: Float = 0f
  var publicMatchCycleLimitSec: Float = RiftCalibrationState.publicMatchCycleLimitSec

  // Dossier Upgrade 1: Live Current-Life Telemetry & Peak Bounty Run Snapshot Tracker
  var currentLifeStats: com.example.data.CurrentLifeStats =
    com.example.data.CurrentLifeStats(shipId = 1, shipFlown = "Viper")
  var currentLifeHadPvp: Boolean = false

  fun formatShipLabel(ship: ShipEntity): String =
    if (ship.spec.id == 10) "Rift Frame" else ship.spec.name

  fun resetCurrentLifeStats() {
    currentLifeStats =
      com.example.data.CurrentLifeStats(
        bountyReached = max(100, localPlayer.bounty),
        shipId = localPlayer.spec.id,
        shipFlown = formatShipLabel(localPlayer),
      )
    currentLifeHadPvp = remotePilots.isNotEmpty()
  }

  /**
   * Immediately equips a newly synthesized AI-Forged Custom Buff into the local player's
   * 4th Right-Hand Special Buff Slot for instant live arena playtesting.
   */
  fun equipAndTestCustomForgedBuff(entry: SpecialBuffRegistryEntry) {
    RiftCodex.registerCustomBuff(entry)
    localPlayer.specialWeaponType = entry.id.uppercase(java.util.Locale.US)
    localPlayer.specialWeaponCharges = max(2, entry.chargesPerPickup)
    buffVersion++
    sfx.playPrize()
    spawnPooledFct(
      text = "🧪 FORGED: ${entry.shortLabel} [${localPlayer.specialWeaponCharges}]",
      x = localPlayer.x,
      y = localPlayer.y - 32f,
      color = entry.strokeColorInt,
      maxLife = 1.35f,
    )
    onPrizeToast("🧪 AI BUFF FORGED & EQUIPPED: ${entry.officialName} (${localPlayer.specialWeaponCharges} CHARGES)")
  }

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

    // Finalize current life telemetry as SURVIVED TO SECTOR CONVERGENCE if player is alive at cycle end
    if (localPlayer.respawnTimer <= 0f) {
      currentLifeStats.bountyReached = max(currentLifeStats.bountyReached, localPlayer.bounty)
      currentLifeStats.shipId = localPlayer.spec.id
      currentLifeStats.shipFlown = formatShipLabel(localPlayer)
      val convergenceSnapshot =
        currentLifeStats.toSnapshot(
          executioner =
            com.example.data.ExecutionerTelemetry(
              survivedToConvergence = true,
              killerCallsign = "SURVIVED TO SECTOR CONVERGENCE",
              killerSyndicateTag = cleanTag(localPlayer.syndicateTag),
              killerShip = formatShipLabel(localPlayer),
              killingWeapon = "Sector Convergence Cycle Complete",
              killerBountyAtKill = localPlayer.bounty,
              bountyEarnedFromYou = 0,
            )
        )
      onLifeRunEnded(convergenceSnapshot, currentLifeHadPvp || remotePilots.isNotEmpty() || forceSimulateWinWithPresence)
    }

    val effectiveDuration = publicMatchDurationSec.coerceAtLeast(1f)
    val presenceRatio = (localPlayer.matchPresenceTimeSec / effectiveDuration).coerceIn(0f, 1f)
    val presencePct = (presenceRatio * 100f).toInt()
    val topShip = ships.maxByOrNull { it.bounty }

    if (topShip != null && (topShip.isLocalPlayer || localPlayer.bounty >= topShip.bounty)) {
      if (presenceRatio > 0.50f) {
        convergencesWon++
        riftShards += 100L
        onSyndicateContribution(0L, localPlayer.bounty.toLong(), 1L)
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
    phaseAfterimages.clear()
    interdictorBubbles.clear()

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
      s.phaseDashTimer = 0f
      s.speedOverchargeStacks = 0
      s.thrustOverchargeStacks = 0
      s.rechargeOverchargeStacks = 0
      s.multiShotCharges = 0
      s.hasBouncingBulletsUpgrade = false
      s.safeZoneStayTimer = 0f
      s.safeZoneLockoutTimer = 0f
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
        s.phaseShieldTimer = RiftCalibrationState.exitPhaseShieldSec
      } else {
        val sp = getOuterGateSpawnPosition(s.freq)
        s.x = sp.first
        s.y = sp.second
        s.inSafeZone = false
        s.wasInSafeZone = false
      }
    }
    resetCurrentLifeStats()
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
        syndicateTag = "",
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
    hitStopTimer = max(hitStopTimer, (frames * 0.5f) * RiftCalibrationState.hitStopDurationSec)
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
        a.syndicateTag != "BOT" &&
        a.syndicateTag != "UNAFFILIATED" &&
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
        b.ownerSyndicateTag != "BOT" &&
        b.ownerSyndicateTag != "UNAFFILIATED" &&
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
        bm.ownerSyndicateTag != "BOT" &&
        bm.ownerSyndicateTag != "UNAFFILIATED" &&
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
    interdictorBubbles.clear()
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
    val margin = RiftCalibrationState.botSafeZoneBanMargin
    return x >= safeZone.left - margin &&
      x <= safeZone.right + margin &&
      y >= safeZone.top - margin &&
      y <= safeZone.bottom + margin
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

    val customIndices = RiftCodex.getCustomForgedBuffs().map { it.buffTypeIndex }
    val chosenBuffType =
      if (customIndices.isNotEmpty() && Random.nextFloat() < 0.16f) {
        customIndices[Random.nextInt(customIndices.size)]
      } else {
        when (val baseRoll = Random.nextInt(11)) {
          10 ->
            when (Random.nextInt(3)) {
              0 -> -10 // Speed Overcharge (+8% Top Speed)
              1 -> -11 // Thrust Overcharge (+8% Thrust)
              else -> -12 // Reactor Overcharge (+8% Flux Recharge)
            }
          else -> baseRoll
        }
      }

    greens.add(
      GreenCrate(
        x = chosenX,
        y = chosenY,
        spinPhase = Random.nextFloat() * (PI.toFloat() * 2f),
        buffType = chosenBuffType,
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
    val lifespan = RiftCalibrationState.riftLifespanSec
    val rad = RiftCalibrationState.riftPhysicalRadius
    val anomaly = SpatialAnomalyEntity(
      id = id,
      type = type,
      x = pos.first,
      y = pos.second,
      physicalRadius = rad,
      life = lifespan,
      maxLife = lifespan,
      partnerId = partnerId,
    )
    anomalies.add(anomaly)
    return anomaly
  }

  fun spawnPhaseRiftPair() {
    val idA = "anomaly_${nextAnomalyId++}"
    val idB = "anomaly_${nextAnomalyId++}"
    val lifespan = RiftCalibrationState.riftLifespanSec
    val rad = RiftCalibrationState.riftPhysicalRadius
    val posA = findRandomAnomalyPosition(anomalies)
    val a = SpatialAnomalyEntity(
      id = idA,
      type = AnomalyType.PHASE_RIFT_A,
      x = posA.first,
      y = posA.second,
      physicalRadius = rad,
      life = lifespan,
      maxLife = lifespan,
      partnerId = idB,
    )
    anomalies.add(a)

    val posB = findRandomAnomalyPosition(anomalies)
    val b = SpatialAnomalyEntity(
      id = idB,
      type = AnomalyType.PHASE_RIFT_B,
      x = posB.first,
      y = posB.second,
      physicalRadius = rad,
      life = lifespan,
      maxLife = lifespan,
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
      val shipId = ((botIdx * 3) % 9) + 1
      val spec = RiftCodex.getShipById(shipId)
      val pos = getOuterGateSpawnPosition(0)
      updated.add(
        ShipEntity(
          id = "bot_ally_$botIdx",
          callsign = BOT_CALLSIGNS[botIdx % BOT_CALLSIGNS.size],
          syndicateTag = "BOT",
          armadaName = "",
          shipId = shipId,
          spec = spec,
          freq = 0,
          isHuman = false,
          isLocalPlayer = false,
          x = pos.first,
          y = pos.second,
          bounty = 100,
        )
      )
      friendlyCount++
      botIdx++
    }

    val maxTotalPlayers = if (isFfa) 20 else 24
    while (updated.size < maxTotalPlayers) {
      val shipId = ((botIdx * 5 + 1) % 9) + 1
      val spec = RiftCodex.getShipById(shipId)
      val pos = getOuterGateSpawnPosition(1)
      updated.add(
        ShipEntity(
          id = "bot_rival_$botIdx",
          callsign = BOT_CALLSIGNS[(botIdx + 7) % BOT_CALLSIGNS.size],
          syndicateTag = "BOT",
          armadaName = "",
          shipId = shipId,
          spec = spec,
          freq = if (isFfa) (botIdx + 1) else 1,
          isHuman = false,
          isLocalPlayer = false,
          x = pos.first,
          y = pos.second,
          angle = PI.toFloat(),
          weaponAngle = PI.toFloat(),
          bounty = 100,
        )
      )
      rivalCount++
      botIdx++
    }

    ships.clear()
    ships.addAll(updated)
    applyArmadaRosterToSectorShips()
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
    val activeRemoteUids = mutableSetOf<String>()
    var rosterChanged = false

    for (st in states) {
      val uid = st.userId.ifEmpty { st.id }
      if (uid.isEmpty() || uid == currentUid) continue
      activeRemoteUids.add(uid)

      val existing = remotePilots[uid]
      val spec = RiftCodex.getShipById(st.shipId.toInt())
      val targetX = st.x.toFloat()
      val targetY = st.y.toFloat()
      val targetVx = st.vx.toFloat()
      val targetVy = st.vy.toFloat()
      val targetAngle = st.angle.toFloat()
      val targetWeaponAngle = st.weaponAngle.toFloat()

      if (existing == null) {
        val created =
          ShipEntity(
            id = uid,
            callsign = st.callsign,
            syndicateTag = st.syndicateTag,
            armadaName = st.armadaName,
            shipId = spec.id,
            spec = spec,
            freq = st.freq.toInt(),
            isHuman = true,
            isLocalPlayer = false,
            x = targetX,
            y = targetY,
            vx = targetVx,
            vy = targetVy,
            angle = targetAngle,
            weaponAngle = targetWeaponAngle,
            independentAimActive = true,
            flux = st.energy.toFloat(),
            maxFlux = st.maxEnergy.toFloat(),
            bounty = st.bounty.toInt(),
            afterburning = st.isAfterburning,
            isCloaked = st.isCloaked,
            remoteFiringPrimary = st.isFiringPrimary,
          )
        remotePilots[uid] = created
        currentLifeHadPvp = true
        rosterChanged = true
        if (st.isFiringPrimary && created.gunTimer <= 0f) {
          firePrimaryWeapon(created)
        }
      } else {
        currentLifeHadPvp = true
        existing.callsign = st.callsign
        existing.syndicateTag = st.syndicateTag
        existing.armadaName = st.armadaName
        if (existing.shipId != spec.id) {
          existing.shipId = spec.id
          existing.spec = spec
        }
        val posDelta = hypot(targetX - existing.x, targetY - existing.y)
        if (posDelta < 220f) {
          existing.x += (targetX - existing.x) * 0.45f
          existing.y += (targetY - existing.y) * 0.45f
        } else {
          existing.x = targetX
          existing.y = targetY
        }
        existing.vx = targetVx
        existing.vy = targetVy
        val remoteSp = hypot(existing.vx, existing.vy)
        existing.angle = if (remoteSp > 2f) atan2(existing.vy, existing.vx) else targetAngle
        existing.weaponAngle = targetWeaponAngle
        existing.independentAimActive = true
        existing.flux = st.energy.toFloat()
        existing.maxFlux = st.maxEnergy.toFloat()
        existing.bounty = st.bounty.toInt()
        existing.afterburning = st.isAfterburning
        existing.isCloaked = st.isCloaked
        existing.remoteFiringPrimary = st.isFiringPrimary

        if (st.isFiringPrimary && existing.gunTimer <= 0f) {
          firePrimaryWeapon(existing)
        }
      }
    }

    val staleUids = remotePilots.keys.filter { it !in activeRemoteUids }
    if (staleUids.isNotEmpty()) {
      for (stale in staleUids) {
        remotePilots.remove(stale)
      }
      rosterChanged = true
    }

    if (rosterChanged) {
      rebuildRosterForMode(mode)
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
    currentLifeStats.shipId = spec.id
    currentLifeStats.shipFlown = formatShipLabel(localPlayer)
    buffVersion++
    syncLocalArmadaSelfSlot()
    onPrizeToast("HULL EQUIPPED: ${formatShipLabel(localPlayer).uppercase(java.util.Locale.US)} (${spec.maxFlux.toInt()}⚡)")
    return true
  }

  fun syncLocalPlayerCustomShipSpec() {
    if (localPlayer.shipId == 10) {
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
    ship.spec.supportsNativeMultifire

  fun toggleMultifire(): Boolean {
    if (!localPlayer.spec.supportsNativeMultifire) {
      onPrizeToast("MULTI TOGGLE IS EXCLUSIVE TO NATIVE MULTI-SHOT HULLS")
      return false
    }
    localPlayer.multifireOn = !localPlayer.multifireOn
    buffVersion++
    val modeName = if (localPlayer.spec.isParallelTwin) "TWIN MULTI" else "SPLIT MULTI"
    val suffix =
      if (localPlayer.multiShotCharges > 0) {
        "5-STREAM OVERDRIVE (${localPlayer.multiShotCharges} SHOTS)"
      } else {
        "0⚡ EXTRA COST"
      }
    onPrizeToast("$modeName: ${if (localPlayer.multifireOn) "ON ($suffix)" else "OFF (FOCUSED)"}")
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

  fun firePrimaryWeapon(ship: ShipEntity) = firePrimaryGun(ship)

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
      if (ship.isLocalPlayer) buffVersion++
      if (ship.spec.bombType == "CLOAK_EMP") {
        ship.bombTimer = ship.spec.bombCooldown
        detonateHullEmpPulse(ship)
      }
    }

    // Validate Multi-Shot toggle state on non-native hulls
    if (ship.multifireOn && !ship.spec.supportsNativeMultifire) {
      ship.multifireOn = false
      if (ship.isLocalPlayer) buffVersion++
    }

    val spec = ship.spec
    val cost = spec.primaryCost
    if (ship.flux <= cost + 1f) return

    selfDrainFlux(ship, cost)
    ship.gunTimer = spec.primaryCooldown

    if (ship.isLocalPlayer) {
      currentLifeStats.shotsFired++
      sfx.playLaser()
    }

    val baseFireAngle =
      if (ship.independentAimActive) {
        ship.weaponAngle
      } else if (ship.isLocalPlayer && RiftCalibrationState.gamepadConnected) {
        ship.angle
      } else if (ship.isLocalPlayer) {
        findQuickTapAutoAimAngle(ship)
      } else {
        ship.angle
      }
    ship.weaponAngle = baseFireAngle

    // Check Viper / Phase Dash Sniper Overcharge (+bonus% velocity and damage on next rail shot)
    val overcharged = ship.sniperOverchargeReady
    val bonusPct = RiftCalibrationState.phaseOverchargeBonusPct
    if (overcharged) {
      ship.sniperOverchargeReady = false
      if (ship.isLocalPlayer) {
        buffVersion++
        spawnPooledFct("⚡ OVERCHARGE RAIL +${(bonusPct * 100f).toInt()}%", ship.x, ship.y - 26f, Color.parseColor("#00F5D4"))
      }
    }
    val overchargeMult = if (overcharged) (1.0f + bonusPct) else 1.0f

    // Permanent-Until-Death Bouncing Bullets Hull Upgrade (+1 wall bounce)
    val bonusBounce = if (ship.hasBouncingBulletsUpgrade) 1 else 0
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
      } else if (ship.multifireOn) {
        if (spec.isParallelTwin) {
          // 5-Twinfang Native Multi-Shot ON -> 4-bolt fan with proportional damage split (50% per bolt = 100% total)
          streams = 4
          spread = 0.075f
          damagePerBolt = (spec.primaryDamage * 0.50f) * overchargeMult
          useParallelTwin = false
        } else {
          // 3-Suppressor (or 9-Custom Ship Native Split) -> 3 spread streams where each deals exact 1/3 (33.3%) damage at 0⚡ extra cost
          streams = 3
          spread = 0.095f
          damagePerBolt = (spec.primaryDamage / 3.0f) * overchargeMult
          useParallelTwin = false
        }
      }
    } else if (ship.multiShotCharges > 0) {
      // Non-native ship with Shot-Limited Multi-Shot Crate Pickup: automatically modifies the Primary Fire Joystick Trigger!
      ship.multiShotCharges--
      if (ship.multiShotCharges <= 0 && ship.isLocalPlayer) {
        onPrizeToast("MULTI-SHOT CRATE AMMO DEPLETED (0 SHOTS)")
      }
      if (ship.isLocalPlayer) buffVersion++
      streams = spec.primaryStreams + 2
      spread = max(0.09f, spec.primarySpreadRad * 1.5f)
      damagePerBolt = spec.primaryDamage * 0.68f * overchargeMult
      useParallelTwin = false
    }

    val shipLbl = formatShipLabel(ship)
    val wepLbl = if (overcharged) "${spec.primaryTitle} (Overcharged)" else spec.primaryTitle

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
            ownerShipLabel = shipLbl,
            weaponLabel = wepLbl,
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
          ownerShipLabel = shipLbl,
          weaponLabel = wepLbl,
        )
      )
    }
  }

  /**
   * Unleashes a 360-degree radial AoE EMP shockwave centered directly on Disruptor's hull,
   * draining 35% Maximum Flux from all hostile ships inside empPulseRadius and disabling
   * their Flux recharge for 2.5 seconds.
   */
  private fun detonateHullEmpPulse(ship: ShipEntity) {
    val pulseRadius = RiftCalibrationState.empPulseRadius
    shockwaves.add(ShockwaveEntity(ship.x, ship.y, 18f, pulseRadius, Color.parseColor("#00F5D4")))
    shockwaves.add(ShockwaveEntity(ship.x, ship.y, 10f, pulseRadius * 0.86f, Color.parseColor("#B388FF")))

    if (particles.size < 92) {
      for (i in 0 until 14) {
        val a = (i / 14f) * (PI.toFloat() * 2f)
        val sp = 180f + Random.nextFloat() * 160f
        particles.add(
          ParticleEntity(
            x = ship.x + cos(a) * 18f,
            y = ship.y + sin(a) * 18f,
            vx = cos(a) * sp,
            vy = sin(a) * sp,
            life = 0.36f,
            maxLife = 0.36f,
            size = 5.5f,
            color = if (i % 2 == 0) Color.parseColor("#00F5D4") else Color.parseColor("#B388FF"),
          )
        )
      }
    }

    if (ship.isLocalPlayer || hypot(ship.x - localPlayer.x, ship.y - localPlayer.y) < 1100f) {
      sfx.playBomb()
    }
    if (ship.isLocalPlayer) {
      currentLifeStats.shotsFired++
      addTrauma(0.32f)
      spawnPooledFct(
        text = "⚡ 360° EMP PULSE",
        x = ship.x,
        y = ship.y - 30f,
        color = Color.parseColor("#00F5D4"),
        maxLife = 1.15f,
      )
    }

    val attackerLbl = formatShipLabel(ship)
    for (other in ships) {
      if (other.id == ship.id) continue
      if (other.respawnTimer > 0f) continue
      if (areAllied(ship, other)) continue
      if (isInsideSafeZone(other.x, other.y)) continue
      val dist = hypot(other.x - ship.x, other.y - ship.y)
      if (dist <= pulseRadius + other.radius * 0.5f) {
        val impactAngle = atan2(other.y - ship.y, other.x - ship.x)
        applyProjectileDamage(
          target = other,
          attackerId = ship.id,
          attackerIsHuman = ship.isHuman,
          rawDamage = 0f,
          impactAngle = impactAngle,
          isHeavyImpact = true,
          isEmp = true,
          attackerShipLabel = attackerLbl,
          weaponLabel = "360° AoE EMP Pulse",
        )
      }
    }
  }

  /**
   * Handles the Contextual Class / Secondary Ability Button for all ships.
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
    val shipLbl = formatShipLabel(ship)

    // 1. Disruptor Combined Tactical Cloak + 360° AoE EMP Pulse System (No Bomb Projectile; 0⚡ Decloak EMP)
    if (spec.bombType == "CLOAK_EMP") {
      if (!ship.isCloaked) {
        if (ship.flux <= 200f) {
          if (ship.isLocalPlayer) onPrizeToast("INSUFFICIENT FLUX FOR TACTICAL CLOAK (MIN 200⚡)")
          return
        }
        ship.isCloaked = true
        ship.bombTimer = 0.35f
        selfDrainFlux(ship, 60f)
        if (ship.isLocalPlayer) {
          buffVersion++
          sfx.playWarp()
          spawnPooledFct("👻 STEALTH CLOAK ENGAGED", ship.x, ship.y - 28f, Color.parseColor("#B388FF"))
          onPrizeToast("STEALTH CLOAK ACTIVE (${RiftCalibrationState.cloakDrainPerSec.toInt()}⚡/s) — TAP EMP PULSE (0⚡) OR FIRE TO UNLEASH 360° EMP!")
        }
        return
      } else {
        // Already cloaked: tapping EMP PULSE breaks stealth and unleashes a 360° radial AoE EMP centered on Disruptor's hull at 0⚡ Flux!
        ship.isCloaked = false
        if (ship.isLocalPlayer) buffVersion++
        if (ship.phaseShieldTimer > 0f) ship.phaseShieldTimer = 0f
        ship.bombTimer = spec.bombCooldown
        detonateHullEmpPulse(ship)
        return
      }
    }

    if (ship.phaseShieldTimer > 0f) {
      ship.phaseShieldTimer = 0f
    }

    val cost = spec.bombCost
    if (ship.flux <= cost + 1f) {
      if (ship.isLocalPlayer) {
        onPrizeToast("INSUFFICIENT FLUX (${cost.toInt()}⚡ REQUIRED)")
      }
      return
    }

    // 1b. Interdictor Stasis Bubble (Two-Way Containment Dome: 290px radius, 15s duration, 600⚡ cost)
    if (spec.bombType == "INTERDICTOR_DOME") {
      val bubbleR = RiftCalibrationState.interdictorBubbleRadius
      val safeDist = hypot(ship.x - safeZone.centerX(), ship.y - safeZone.centerY())
      if (safeDist < (safeZone.width() * 0.5f + bubbleR + 30f)) {
        if (ship.isLocalPlayer) {
          sfx.playMutedClick()
          onPrizeToast("CANNOT ANCHOR STASIS BUBBLE OVER SAFE ZONE PERIMETER")
        }
        return
      }
      selfDrainFlux(ship, cost)
      ship.bombTimer = spec.bombCooldown
      interdictorBubbles.removeAll { it.ownerId == ship.id }
      val dur = RiftCalibrationState.interdictorBubbleDuration
      interdictorBubbles.add(
        InterdictorBubbleEntity(
          id = "bubble_${ship.id}_${System.currentTimeMillis()}",
          ownerId = ship.id,
          ownerFreq = ship.freq,
          ownerShipLabel = shipLbl,
          x = ship.x,
          y = ship.y,
          radius = bubbleR,
          life = dur,
          maxLife = dur,
        )
      )
      shockwaves.add(ShockwaveEntity(ship.x, ship.y, 24f, bubbleR, Color.parseColor("#EC4899")))
      shockwaves.add(ShockwaveEntity(ship.x, ship.y, 16f, bubbleR * 0.85f, Color.parseColor("#00F5D4")))
      if (ship.isLocalPlayer || hypot(ship.x - localPlayer.x, ship.y - localPlayer.y) < 1200f) {
        sfx.playWarp()
      }
      if (ship.isLocalPlayer) {
        buffVersion++
        currentLifeStats.shotsFired++
        addTrauma(0.28f)
        spawnPooledFct(
          text = "🌐 STASIS BUBBLE (${dur.toInt()}s)",
          x = ship.x,
          y = ship.y - 30f,
          color = Color.parseColor("#EC4899"),
          maxLife = 1.25f,
        )
      }
      return
    }

    selfDrainFlux(ship, cost)
    ship.bombTimer = spec.bombCooldown

    // 2. Viper / Rift Frame Phase Dash: 135px Spatial Blink + Supersonic Dodge Burst + 0.65s I-Frames + Rail Overcharge
    if (spec.bombType == "PHASE_DASH") {
      if (ship.isLocalPlayer) {
        autoBraking = false
      }
      val dashHeading =
        if (ship.isLocalPlayer && leftStickActive && leftStickMag > 0.1f) {
          leftStickAngle
        } else if (ship.isLocalPlayer && fireStickActive && fireStickHasAimVector) {
          fireStickAimAngle
        } else if (hypot(ship.vx, ship.vy) > 25f) {
          atan2(ship.vy, ship.vx)
        } else {
          fireAngle
        }

      val startX = ship.x
      val startY = ship.y
      var bestX = startX
      var bestY = startY
      val cosH = cos(dashHeading)
      val sinH = sin(dashHeading)
      var stepDist = 15f
      while (stepDist <= 135f) {
        val candX = startX + cosH * stepDist
        val candY = startY + sinH * stepDist
        val inWorld = candX in 110f..(WORLD_SIZE - 110f) && candY in 110f..(WORLD_SIZE - 110f)
        val blockedBySafeLockout =
          ship.safeZoneLockoutTimer > 0f &&
            candX >= safeZone.left - ship.radius - 6f &&
            candX <= safeZone.right + ship.radius + 6f &&
            candY >= safeZone.top - ship.radius - 6f &&
            candY <= safeZone.bottom + ship.radius + 6f
        var crossesBubbleWall = false
        for (ib in interdictorBubbles) {
          val startDist = hypot(startX - ib.x, startY - ib.y)
          val candDist = hypot(candX - ib.x, candY - ib.y)
          if (startDist <= ib.radius && candDist > (ib.radius - ship.radius - 2f).coerceAtLeast(36f)) {
            crossesBubbleWall = true
            break
          }
          if (startDist > ib.radius && candDist < ib.radius + ship.radius + 2f) {
            crossesBubbleWall = true
            break
          }
        }
        if (
          !inWorld ||
            isInsideSafeZone(candX, candY) ||
            blockedBySafeLockout ||
            crossesBubbleWall ||
            circleIntersectsWalls(candX, candY, ship.radius + 4f)
        ) {
          break
        }
        bestX = candX
        bestY = candY
        stepDist += 15f
      }

      val isAlliedToLocal = areAllied(localPlayer, ship)
      if (phaseAfterimages.size < 36) {
        val echoFractions = floatArrayOf(0f, 0.42f, 0.78f)
        val echoLives = floatArrayOf(0.22f, 0.30f, 0.38f)
        for (idx in echoFractions.indices) {
          val frac = echoFractions[idx]
          phaseAfterimages.add(
            PhaseAfterimageEntity(
              shipId = ship.shipId,
              x = startX + (bestX - startX) * frac,
              y = startY + (bestY - startY) * frac,
              angleRad = dashHeading,
              isAllied = isAlliedToLocal,
              life = echoLives[idx],
              maxLife = 0.38f,
            )
          )
        }
      }
      if (particles.size < 92) {
        for (i in 0 until 10) {
          val t = i / 9f
          val px = startX + (bestX - startX) * t + (Random.nextFloat() - 0.5f) * 14f
          val py = startY + (bestY - startY) * t + (Random.nextFloat() - 0.5f) * 14f
          particles.add(
            ParticleEntity(
              x = px,
              y = py,
              vx = -cosH * (90f + Random.nextFloat() * 130f) + (Random.nextFloat() - 0.5f) * 45f,
              vy = -sinH * (90f + Random.nextFloat() * 130f) + (Random.nextFloat() - 0.5f) * 45f,
              life = 0.32f,
              maxLife = 0.32f,
              size = 5.2f,
              color = if (i % 2 == 0) Color.parseColor("#00F5D4") else Color.parseColor("#38BDF8"),
            )
          )
        }
      }

      val dashSpeed = max(ship.spec.afterburnerSpeed * 1.42f, RiftCalibrationState.phaseDashImpulse)
      ship.x = bestX
      ship.y = bestY
      ship.vx = cosH * dashSpeed
      ship.vy = sinH * dashSpeed
      ship.angle = dashHeading
      ship.phaseDashTimer = 0.42f
      ship.sniperOverchargeReady = true
      ship.phaseShieldTimer = max(ship.phaseShieldTimer, 0.65f)
      shockwaves.add(ShockwaveEntity(startX, startY, 14f, 125f, Color.parseColor("#38BDF8")))
      shockwaves.add(ShockwaveEntity(bestX, bestY, 18f, 170f, Color.parseColor("#00F5D4")))
      if (ship.isLocalPlayer) {
        buffVersion++
        sfx.playWarp()
        addTrauma(0.22f)
        spawnPooledFct("⚡ PHASE DASH + RAIL OVERCHARGE", ship.x, ship.y - 28f, Color.parseColor("#00F5D4"))
      }
      return
    }

    if (ship.isLocalPlayer) {
      currentLifeStats.shotsFired++
      sfx.playBomb()
    }

    // Apply backward Newtonian recoil impulse if applicable (2-Ricochet, 4-Goliath, 7-Valkyrie)
    if (spec.bombRecoil > 0f) {
      ship.vx -= cos(fireAngle) * spec.bombRecoil
      ship.vy -= sin(fireAngle) * spec.bombRecoil
    }

    // 3. 8-Manta Proximity Mines
    if (spec.bombType == "MINE") {
      val mx = ship.x - cos(ship.angle) * 44f
      val my = ship.y - sin(ship.angle) * 44f
      if (isInsideSafeZone(mx, my)) return
      val existingMines = bombs.filter { it.ownerId == ship.id && it.type == "MINE" }
      val maxMines = RiftCalibrationState.maxMinesPerShip.coerceAtLeast(1)
      if (existingMines.size >= maxMines) {
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
          ownerShipLabel = shipLbl,
          weaponLabel = "Proximity Mine",
        )
      )
      return
    }

    // 4. 9-Custom Ship Built-in Secondary Nova Ring
    if (spec.bombType == "BURST" || spec.bombType == "NOVA_RING") {
      fireRadialBurstProjectiles(ship)
      return
    }

    // 5. 9-Custom Ship Built-in Secondary Shatter Cone
    if (spec.bombType == "SHOTGUN" || spec.bombType == "SHATTER_CONE") {
      ship.shotgunAimAngle = fireAngle
      fireShotgunWave(ship, fireAngle)
      ship.shotgunWavesRemaining = (RiftCalibrationState.shatterWaves - 1).coerceAtLeast(0)
      ship.shotgunWaveTimer = 0.085f
      return
    }

    // 6. 9-Custom Ship Built-in Secondary Seeker Torpedo
    if (spec.bombType == "MISSILE" || spec.bombType == "SEEKER_TORPEDO") {
      launchLockOnBouncingMissile(ship, fireAngle)
      return
    }

    // 7. Standard Warheads: "BOUNCE" (2-Ricochet), "SIEGE" (4-Goliath), "PROX" (7-Valkyrie)
    val bx = ship.x + cos(fireAngle) * 52f
    val by = ship.y + sin(fireAngle) * 52f
    if (isInsideSafeZone(bx, by)) return

    val speed = if (spec.bombType == "SIEGE") 540f else 640f
    val warheadRadius = if (spec.bombType == "SIEGE") 24f else 17f
    val warheadTitle =
      when (spec.bombType) {
        "SIEGE" -> "Siege Bomb"
        "BOUNCE" -> "Bounce Bomb"
        "PROX" -> "Proximity Flak Bomb"
        else -> spec.secondaryShortLabel
      }
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
        ownerShipLabel = shipLbl,
        weaponLabel = warheadTitle,
      )
    )
  }

  /**
   * Stacking & Overwrite Inventory Rules for Bucket A Special Buff Slot Pickups (Charge-Based, 0⚡ Cost).
   */
  fun grantSpecialBuffWeapon(ship: ShipEntity, newType: String, baseGrant: Int) {
    val canonicalType = RiftCodex.normalizeSpecialBuffId(newType)
    val currentCanonical = RiftCodex.normalizeSpecialBuffId(ship.specialWeaponType)
    if (currentCanonical == canonicalType && ship.specialWeaponCharges > 0) {
      ship.specialWeaponCharges = min(12, ship.specialWeaponCharges + baseGrant)
      ship.specialWeaponType = canonicalType
    } else {
      ship.specialWeaponType = canonicalType
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
    val count = RiftCalibrationState.novaBoltCount.coerceIn(6, 64)
    val boltSpeed = RiftCalibrationState.novaBoltSpeed
    val boltDmg = RiftCalibrationState.novaBoltDamage
    val shipLbl = formatShipLabel(ship)
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
          vx = cos(a) * boltSpeed,
          vy = sin(a) * boltSpeed,
          damage = boltDmg,
          bouncesLeft = 2,
          life = 1.9f,
          color = Color.parseColor("#F59E0B"),
          isHeavyBurst = true,
          ownerShipLabel = shipLbl,
          weaponLabel = "Nova Ring",
        )
      )
    }
  }

  /**
   * Fires a single 60-degree cone wave of bouncing short-range Shatter Cone plasma shards.
   */
  fun fireShotgunWave(ship: ShipEntity, baseAngle: Float) {
    if (ship.respawnTimer > 0f || isInsideSafeZone(ship.x, ship.y)) return
    if (ship.isLocalPlayer) {
      sfx.playShotgun()
    }
    val pelletCount = RiftCalibrationState.shatterPelletsPerWave.coerceIn(3, 24)
    val pelletDmg = RiftCalibrationState.shatterPelletDamage
    val coneRad = (PI.toFloat() / 3f) // 60-degree cone
    val isFriendlyToLocal = areAllied(localPlayer, ship)
    val pelletColor = if (isFriendlyToLocal) Color.parseColor("#FB7185") else Color.parseColor("#EF4444")
    val shipLbl = formatShipLabel(ship)
    for (i in 0 until pelletCount) {
      val frac = (i.toFloat() / max(1, pelletCount - 1).toFloat()) - 0.5f
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
          damage = pelletDmg,
          bouncesLeft = 2,
          life = 0.36f,
          color = pelletColor,
          isHeavyBurst = false,
          ownerShipLabel = shipLbl,
          weaponLabel = "Shatter Cone",
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
    val seekerSpd = RiftCalibrationState.seekerSpeed
    val seekerLife = RiftCalibrationState.seekerFlightTime
    val seekerDmg = RiftCalibrationState.seekerDamage
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
          vx = cos(launchAngle) * seekerSpd,
          vy = sin(launchAngle) * seekerSpd,
          damage = seekerDmg,
          bouncesLeft = 8,
          passWalls = false,
          life = seekerLife,
          radius = 18f,
          targetShipId = target?.id ?: "",
          ownerShipLabel = formatShipLabel(ship),
          weaponLabel = "Seeker Torpedo",
        )
      )
      if (target != null) {
        target.missileLockWarningTimer = seekerLife
        if (ship.isLocalPlayer) {
          sfx.playMissileLockBeep()
          spawnPooledFct(
            text = "🎯 SEEKER LOCK: ${target.callsign}",
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
          ownerShipLabel = bomb.ownerShipLabel,
          weaponLabel = "Proximity Flak Shrapnel",
        )
      )
    }
  }

  /**
   * Finds the best hostile ship outside the Safe Zone to lock onto for the Seeker Torpedo.
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
   * Activates a Bucket A Special Buff Slot charge (0⚡ Flux Cost) for any ship (Local Player or AI Bot),
   * including both the 6 core BUFF_REGISTRY items and live AI-Forged Custom Buffs!
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

    val activeType = RiftCodex.normalizeSpecialBuffId(ship.specialWeaponType)
    ship.specialWeaponCharges--
    if (ship.specialWeaponCharges <= 0) {
      ship.specialWeaponCharges = 0
      ship.specialWeaponType = "NONE"
    }
    if (ship.isLocalPlayer) {
      currentLifeStats.shotsFired++
      buffVersion++
    }

    val aimAngle =
      if (ship.independentAimActive) {
        ship.weaponAngle
      } else if (ship.isLocalPlayer && RiftCalibrationState.gamepadConnected) {
        ship.angle
      } else if (ship.isLocalPlayer) {
        findQuickTapAutoAimAngle(ship)
      } else {
        ship.angle
      }

    when (activeType) {
      "NOVA_RING" -> {
        fireRadialBurstProjectiles(ship)
      }
      "SHATTER_CONE" -> {
        ship.weaponAngle = aimAngle
        fireShotgunWave(ship, aimAngle)
        ship.shotgunWavesRemaining = (RiftCalibrationState.shatterWaves - 1).coerceAtLeast(0)
        ship.shotgunWaveTimer = 0.085f
        ship.shotgunAimAngle = aimAngle
      }
      "SEEKER_TORPEDO" -> {
        launchLockOnBouncingMissile(ship, aimAngle)
      }
      "HOLO_PHANTOM" -> {
        val dur = RiftCalibrationState.holoPhantomDuration
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
            life = dur,
            maxLife = dur,
          )
        )
        for (bm in bombs) {
          if (bm.type == "MISSILE" && bm.targetShipId == ship.id) {
            bm.targetShipId = decoyId
          }
        }
        ship.missileLockWarningTimer = 0f
        if (ship.isLocalPlayer) {
          sfx.playWarp()
          spawnPooledFct("👥 HOLO PHANTOM (${dur.toInt()}s)", ship.x, ship.y - 28f, Color.parseColor("#2DD4BF"))
        }
      }
      "STASIS_BARRIER" -> {
        val dur = RiftCalibrationState.stasisBarrierDuration
        val span = RiftCalibrationState.stasisBarrierLength
        val deployDist = 86f
        val cx = (ship.x + cos(ship.angle) * deployDist).coerceIn(140f, WORLD_SIZE - 140f)
        val cy = (ship.y + sin(ship.angle) * deployDist).coerceIn(140f, WORLD_SIZE - 140f)
        if (!isInsideSafeZone(cx, cy)) {
          val horizPerp = kotlin.math.abs(sin(ship.angle)) >= kotlin.math.abs(cos(ship.angle))
          val w = if (horizPerp) span else 22f
          val h = if (horizPerp) 22f else span
          brickWalls.add(
            BrickWallEntity(
              id = "brick_${nextDecoyBrickId++}",
              ownerId = ship.id,
              freq = ship.freq,
              x = cx - w * 0.5f,
              y = cy - h * 0.5f,
              w = w,
              h = h,
              life = dur,
              maxLife = dur,
            )
          )
          shockwaves.add(ShockwaveEntity(cx, cy, 12f, 95f, Color.parseColor("#38BDF8")))
          if (ship.isLocalPlayer) {
            sfx.playBounce()
            spawnPooledFct("🧱 STASIS BARRIER (${dur.toInt()}s)", cx, cy - 24f, Color.parseColor("#38BDF8"))
          }
        }
      }
      "HYPER_SURGE" -> {
        val dur = RiftCalibrationState.hyperSurgeDuration
        val mult = RiftCalibrationState.hyperSurgeSpeedMult
        ship.rocketBoostTimer = dur
        val boostSpd = ship.spec.topSpeed * mult
        ship.vx = cos(ship.angle) * boostSpd
        ship.vy = sin(ship.angle) * boostSpd
        shockwaves.add(ShockwaveEntity(ship.x, ship.y, 18f, 180f, Color.parseColor("#F43F5E")))
        if (ship.isLocalPlayer) {
          buffVersion++
          sfx.playWarp()
          addTrauma(0.28f)
          spawnPooledFct("🚀 HYPER SURGE (${String.format(java.util.Locale.US, "%.1fs", dur)})", ship.x, ship.y - 28f, Color.parseColor("#F43F5E"))
        }
      }
      else -> {
        // Custom AI-Forged Buff execution from BUFF_REGISTRY!
        val forged = RiftCodex.getBuffRegistryEntry(activeType) ?: return
        val accent = forged.strokeColorInt
        when (forged.specialEffect) {
          CustomBuffEffect.BARRIER -> {
            val deployDist = 88f
            val cx = (ship.x + cos(aimAngle) * deployDist).coerceIn(140f, WORLD_SIZE - 140f)
            val cy = (ship.y + sin(aimAngle) * deployDist).coerceIn(140f, WORLD_SIZE - 140f)
            if (!isInsideSafeZone(cx, cy)) {
              val horizPerp = kotlin.math.abs(sin(aimAngle)) >= kotlin.math.abs(cos(aimAngle))
              val span = forged.blastRadius.coerceIn(70f, 320f)
              val w = if (horizPerp) span else 24f
              val h = if (horizPerp) 24f else span
              brickWalls.add(
                BrickWallEntity(
                  id = "brick_${nextDecoyBrickId++}",
                  ownerId = ship.id,
                  freq = ship.freq,
                  x = cx - w * 0.5f,
                  y = cy - h * 0.5f,
                  w = w,
                  h = h,
                  life = forged.flightDuration,
                  maxLife = forged.flightDuration,
                )
              )
              shockwaves.add(ShockwaveEntity(cx, cy, 14f, span * 0.8f, accent))
              if (ship.isLocalPlayer) {
                sfx.playBounce()
                spawnPooledFct("🧪 ${forged.officialName}", cx, cy - 26f, accent)
              }
            }
          }
          CustomBuffEffect.SPEED_BOOST -> {
            ship.rocketBoostTimer = forged.flightDuration
            val boostSpd = max(ship.spec.topSpeed * 1.65f, forged.projectileSpeed)
            ship.vx = cos(aimAngle) * boostSpd
            ship.vy = sin(aimAngle) * boostSpd
            ship.angle = aimAngle
            shockwaves.add(ShockwaveEntity(ship.x, ship.y, 18f, 190f, accent))
            if (ship.isLocalPlayer) {
              buffVersion++
              sfx.playWarp()
              addTrauma(0.24f)
              spawnPooledFct("🧪 ${forged.officialName}", ship.x, ship.y - 28f, accent)
            }
          }
          CustomBuffEffect.CLONE -> {
            val decoyAngle = aimAngle + 0.38f
            val speed = max(ship.spec.topSpeed * 0.95f, forged.projectileSpeed * 0.7f)
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
                life = forged.flightDuration,
                maxLife = forged.flightDuration,
              )
            )
            if (ship.isLocalPlayer) {
              sfx.playWarp()
              spawnPooledFct("🧪 ${forged.officialName}", ship.x, ship.y - 28f, accent)
            }
          }
          else -> {
            val ox = ship.x + cos(aimAngle) * 50f
            val oy = ship.y + sin(aimAngle) * 50f
            if (!isInsideSafeZone(ox, oy)) {
              bombs.add(
                BombEntity(
                  ownerId = ship.id,
                  ownerCallsign = ship.callsign,
                  ownerSyndicateTag = ship.syndicateTag,
                  ownerArmadaName = ship.armadaName,
                  ownerIsHuman = ship.isHuman,
                  freq = ship.freq,
                  type = "CUSTOM_ORB",
                  x = ox,
                  y = oy,
                  vx = ship.vx * 0.2f + cos(aimAngle) * forged.projectileSpeed,
                  vy = ship.vy * 0.2f + sin(aimAngle) * forged.projectileSpeed,
                  damage = forged.damage,
                  bouncesLeft = forged.wallBounces,
                  passWalls = false,
                  life = forged.flightDuration,
                  radius = 19f,
                  ownerShipLabel = formatShipLabel(ship),
                  weaponLabel = forged.officialName,
                  customColor = accent,
                  customEffect = forged.specialEffect,
                  customBlastRadius = forged.blastRadius,
                )
              )
              if (ship.isLocalPlayer) {
                sfx.playBomb()
                addTrauma(0.20f)
                spawnPooledFct("🧪 ${forged.officialName}", ship.x, ship.y - 28f, accent)
              }
            }
          }
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
    if (
      type == "SPECIAL" ||
        RiftCodex.getBuffRegistryEntry(type) != null
    ) {
      // Bucket A Special Buff Slot Pickups are strictly Charge-Based (0⚡ Flux Cost!)
      activateShipSpecialBuff(localPlayer)
      return
    }
    if (type == "REPEL") {
      if (localPlayer.repels <= 0) return
      val rCost = RiftCalibrationState.repelCost
      if (localPlayer.flux <= rCost) {
        onPrizeToast("INSUFFICIENT FLUX FOR REPEL (${rCost.toInt()}⚡)")
        return
      }
      localPlayer.repels--
      buffVersion++
      selfDrainFlux(localPlayer, rCost)
      sfx.playRepel()
      addTrauma(0.26f)
      val repelRadius = RiftCalibrationState.repelRadius
      val pushForce = RiftCalibrationState.repelPushForce
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
            val push = ((repelRadius - dist) / repelRadius) * pushForce
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
    val pts = mutableListOf(TrajectoryPoint(ship.x, ship.y, isBounce = false, isWarp = false))
    var curX = ship.x + cos(angle) * 24f
    var curY = ship.y + sin(angle) * 24f
    var dirX = cos(angle)
    var dirY = sin(angle)
    val allowedBounces = max(1, maxBounces)
    var wallBouncesUsed = 0
    var warpsUsed = 0
    var lastExitedPartnerId = ""

    while (wallBouncesUsed <= allowedBounces && warpsUsed <= 2) {
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

      // Check linked Phase Rift A ⇄ B portal warps closer than the closest wall hit
      var closestPortalEntry: Pair<Float, SpatialAnomalyEntity>? = null
      for (anom in anomalies) {
        if (anom.type != AnomalyType.PHASE_RIFT_A && anom.type != AnomalyType.PHASE_RIFT_B) continue
        if (anom.id == lastExitedPartnerId || anom.partnerId.isEmpty()) continue
        val portalR = anom.physicalRadius * 0.52f
        val fx = curX - anom.x
        val fy = curY - anom.y
        val b = fx * dirX + fy * dirY
        val c = fx * fx + fy * fy - portalR * portalR
        val disc = b * b - c
        if (disc >= 0f) {
          val tHit = -b - sqrt(disc)
          if (tHit > 1.5f && tHit < bestT) {
            if (closestPortalEntry == null || tHit < closestPortalEntry.first) {
              closestPortalEntry = Pair(tHit, anom)
            }
          }
        }
      }

      val portalEntry = closestPortalEntry
      if (portalEntry != null) {
        val partner = anomalies.firstOrNull { it.id == portalEntry.second.partnerId }
        if (partner != null) {
          val entryX = curX + dirX * portalEntry.first
          val entryY = curY + dirY * portalEntry.first
          pts.add(TrajectoryPoint(entryX, entryY, isBounce = false, isWarp = true))
          pts.add(TrajectoryPoint(partner.x, partner.y, isBounce = true, isWarp = true))
          curX = partner.x + dirX * 2f
          curY = partner.y + dirY * 2f
          lastExitedPartnerId = partner.id
          warpsUsed++
          continue
        }
      }

      val hit = closestHit
      if (hit != null) {
        pts.add(TrajectoryPoint(hit.hitX, hit.hitY, isBounce = true, isWarp = false))
        val dot = dirX * hit.normalX + dirY * hit.normalY
        dirX -= 2f * dot * hit.normalX
        dirY -= 2f * dot * hit.normalY
        curX = hit.hitX + hit.normalX * 2f
        curY = hit.hitY + hit.normalY * 2f
        lastExitedPartnerId = ""
        wallBouncesUsed++
      } else {
        pts.add(
          TrajectoryPoint(
            curX + dirX * segmentMaxLen,
            curY + dirY * segmentMaxLen,
            isBounce = false,
            isWarp = false,
          )
        )
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
        val elast = RiftCalibrationState.wallElasticity
        ship.vx = (ship.vx - 2f * dot * nx) * elast
        ship.vy = (ship.vy - 2f * dot * ny) * elast
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
    attackerShipLabel: String = "",
    weaponLabel: String = "Primary Blaster",
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

    val phaseDodged = target.phaseDashTimer > 0f && !isEmp
    val shieldAbsorbed = target.phaseShieldTimer > 0f
    val empFluxDrain = if (isEmp) target.maxFlux * RiftCalibrationState.empFluxDrainPct else 0f
    val baseCombined = max(rawDamage, empFluxDrain)
    val effectiveDamage =
      when {
        phaseDodged -> 0f
        shieldAbsorbed -> baseCombined * 0.25f
        else -> baseCombined
      }

    if (phaseDodged) {
      shieldRipples.add(
        ShieldRippleEntity(
          shipId = target.id,
          impactAngle = impactAngle,
          intensity = 1.0f,
          life = 0.28f,
        )
      )
      if (attackerId == localPlayer.id || target.isLocalPlayer) {
        spawnPooledFct(
          text = "⚡ PHASE EVADED",
          x = target.x,
          y = target.y - 20f,
          color = Color.parseColor("#00F5D4"),
        )
      }
      return
    }

    if (isEmp) {
      target.empTimer = RiftCalibrationState.empFreezeSec
      if (target.isLocalPlayer) {
        onPrizeToast("⚡ HIT BY 360° EMP PULSE! -${(RiftCalibrationState.empFluxDrainPct * 100f).toInt()}% FLUX & RECHARGE DISABLED FOR ${String.format(java.util.Locale.US, "%.1fs", RiftCalibrationState.empFreezeSec)}!")
      }
    }

    // Track live CurrentLifeStats damage & accuracy telemetry
    if (attackerId == localPlayer.id && !target.isLocalPlayer) {
      currentLifeStats.shotsHit++
      currentLifeStats.damageDealt += effectiveDamage
      if (target.isHuman) currentLifeHadPvp = true
    } else if (target.isLocalPlayer) {
      currentLifeStats.damageTaken += effectiveDamage
      if (attackerIsHuman) currentLifeHadPvp = true
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
        text = if (isEmp) "⚡ EMP -${effectiveDamage.toInt()}⚡" else "-${effectiveDamage.toInt()}",
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
      val wasHvt = target.bounty >= RiftCalibrationState.hvtBountyThreshold
      val bountyAward = max(50, (target.bounty * RiftCalibrationState.bountyKillBonusPct).toInt())
      target.flux = 0f
      target.deathX = target.x
      target.deathY = target.y
      target.respawnTimer = RiftCalibrationState.respawnDelaySec
      target.deaths++
      target.isCloaked = false
      target.sniperOverchargeReady = false
      target.rocketBoostTimer = 0f
      target.phaseDashTimer = 0f
      target.speedOverchargeStacks = 0
      target.thrustOverchargeStacks = 0
      target.rechargeOverchargeStacks = 0
      target.hasBouncingBulletsUpgrade = false

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
      val attackerPreKillBounty = attacker?.bounty ?: 100
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
        if (wasHvt) {
          hvtEliminations++
          currentLifeStats.hvtShutdowns++
        }
        allTimeBountyPeak = max(allTimeBountyPeak, localPlayer.bounty.toLong())
        currentLifeStats.bountyReached = max(currentLifeStats.bountyReached, localPlayer.bounty)
        if (target.isHuman) {
          pvpKills++
          currentLifeStats.pvpKills++
          currentLifeHadPvp = true
          pvpBountyPeak = max(pvpBountyPeak, localPlayer.bounty.toLong())
        } else {
          currentLifeStats.botKills++
        }
        awardPilotXp(
          bountyEarnedXp = bountyAward * 2f,
          bountyKilledXp = victimPreDeathBounty * 0.15f,
          notifyChange = false,
        )
        onArmadaTierKill(getCurrentArmadaSize(), localPlayer.bounty.toLong())
        onSyndicateContribution(1L, localPlayer.bounty.toLong(), 0L)
        onStatsUpdated()
      } else if (target.isLocalPlayer) {
        allTimeDeaths++
        if (attackerIsHuman) pvpDeaths++
        currentLifeStats.bountyReached = max(currentLifeStats.bountyReached, victimPreDeathBounty)
        currentLifeStats.shipId = localPlayer.spec.id
        currentLifeStats.shipFlown = formatShipLabel(localPlayer)
        val execTag =
          when {
            attacker == null -> "GUEST"
            attacker.armadaName.isNotEmpty() -> cleanTag(attacker.armadaName)
            else -> cleanTag(attacker.syndicateTag)
          }
        val execShip =
          when {
            attackerShipLabel.isNotBlank() -> attackerShipLabel
            attacker != null -> formatShipLabel(attacker)
            else -> "Sector Ordnance"
          }
        val finalizedSnapshot =
          currentLifeStats.toSnapshot(
            executioner =
              com.example.data.ExecutionerTelemetry(
                survivedToConvergence = false,
                killerCallsign = attacker?.callsign ?: "ORDNANCE",
                killerSyndicateTag = execTag,
                killerShip = execShip,
                killingWeapon = weaponLabel,
                killerBountyAtKill = attackerPreKillBounty,
                bountyEarnedFromYou = bountyAward,
              )
          )
        onLifeRunEnded(finalizedSnapshot, currentLifeHadPvp || attackerIsHuman)
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
    target.respawnTimer = RiftCalibrationState.respawnDelaySec
    target.deaths++
    target.playfieldIdleTimer = 0f
    target.safeZoneStayTimer = 0f
    target.isIdleDraining = false
    target.isCloaked = false
    target.sniperOverchargeReady = false
    target.rocketBoostTimer = 0f
    target.phaseDashTimer = 0f
    target.speedOverchargeStacks = 0
    target.thrustOverchargeStacks = 0
    target.rechargeOverchargeStacks = 0
    target.hasBouncingBulletsUpgrade = false

    if (target.isLocalPlayer || hypot(target.x - localPlayer.x, target.y - localPlayer.y) < 1100f) {
      sfx.playExplosion()
    }
    if (target.isLocalPlayer) {
      triggerHitStop(2)
      addTrauma(0.45f)
      allTimeDeaths++
      currentLifeStats.bountyReached = max(currentLifeStats.bountyReached, victimPreDeathBounty)
      val finalized =
        currentLifeStats.toSnapshot(
          executioner =
            com.example.data.ExecutionerTelemetry(
              survivedToConvergence = false,
              killerCallsign = "AFK FLUX OVERLOAD",
              killerSyndicateTag = "SECTOR",
              killerShip = "Arena Hazard",
              killingWeapon = "Open-Space Idle Drain",
              killerBountyAtKill = 0,
              bountyEarnedFromYou = 0,
            )
        )
      onLifeRunEnded(finalized, currentLifeHadPvp)
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
      wasHvt = victimPreDeathBounty >= RiftCalibrationState.hvtBountyThreshold,
    )
  }

  private fun destroyShipFromPlasmaRift(target: ShipEntity) {
    if (target.respawnTimer > 0f) return
    val victimPreDeathBounty = target.bounty
    target.flux = 0f
    target.deathX = target.x
    target.deathY = target.y
    target.respawnTimer = RiftCalibrationState.respawnDelaySec
    target.deaths++
    target.playfieldIdleTimer = 0f
    target.safeZoneStayTimer = 0f
    target.isIdleDraining = false
    target.shotgunWavesRemaining = 0
    target.isCloaked = false
    target.sniperOverchargeReady = false
    target.rocketBoostTimer = 0f
    target.phaseDashTimer = 0f
    target.speedOverchargeStacks = 0
    target.thrustOverchargeStacks = 0
    target.rechargeOverchargeStacks = 0
    target.hasBouncingBulletsUpgrade = false

    if (target.isLocalPlayer || hypot(target.x - localPlayer.x, target.y - localPlayer.y) < 1100f) {
      sfx.playExplosion()
    }
    if (target.isLocalPlayer) {
      triggerHitStop(2)
      addTrauma(0.45f)
      allTimeDeaths++
      currentLifeStats.bountyReached = max(currentLifeStats.bountyReached, victimPreDeathBounty)
      val finalized =
        currentLifeStats.toSnapshot(
          executioner =
            com.example.data.ExecutionerTelemetry(
              survivedToConvergence = false,
              killerCallsign = "PLASMA RIFT",
              killerSyndicateTag = "SECTOR",
              killerShip = "Playfield Rift",
              killingWeapon = "Plasma Shear Overload",
              killerBountyAtKill = 0,
              bountyEarnedFromYou = 0,
            )
        )
      onLifeRunEnded(finalized, currentLifeHadPvp)
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
      wasHvt = victimPreDeathBounty >= RiftCalibrationState.hvtBountyThreshold,
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
    // Self-protective survival instincts driven by live Calibration slider:
    val lowFluxRetreat = fluxRatio < RiftCalibrationState.botRetreatFluxRatio
    val insideSafeBanZone = isWithinBotBanPerimeter(bot.x, bot.y)

    // Requirement 5: Anti-Corner Bot Pathfinding & 1.2s Strafe/Reverse Override
    val speed = hypot(bot.vx, bot.vy)
    val moveDirX = if (speed > 8f) bot.vx / speed else cos(bot.angle)
    val moveDirY = if (speed > 8f) bot.vy / speed else sin(bot.angle)
    val wallHit = raycastBotLookAhead(bot.x, bot.y, moveDirX, moveDirY, lookAheadDist = RiftCalibrationState.botLookAheadDist)

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

    // Bots are strictly banned from loitering within botSafeZoneBanMargin of the Safe Zone perimeter
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

    // Protective Survival Retreat: Fly toward Center Safe Haven when under threat or low flux (only if Safe Zone is not locked out!)
    if (lowFluxRetreat && bot.safeZoneLockoutTimer <= 0f && !isInsideSafeZone(bot.x, bot.y)) {
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
      val rCost = RiftCalibrationState.repelCost
      if (bot.repels > 0 && bot.flux > rCost + 30f) {
        val chasersNearby = ships.any { !areAllied(bot, it) && it.respawnTimer <= 0f && hypot(it.x - bot.x, it.y - bot.y) < 360f }
        if (chasersNearby) {
          bot.repels--
          selfDrainFlux(bot, rCost)
          sfx.playRepel()
          shockwaves.add(
            ShockwaveEntity(
              x = bot.x,
              y = bot.y,
              r = 32f,
              maxR = RiftCalibrationState.repelRadius,
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

    // AI Bot Special Buff Slot Deployment (Core 6 + Custom AI-Forged Buffs)
    if (bot.specialWeaponCharges > 0 && bot.specialWeaponType != "NONE" && !isInsideSafeZone(bot.x, bot.y)) {
      val shouldTriggerBuff =
        when (RiftCodex.normalizeSpecialBuffId(bot.specialWeaponType)) {
          "HOLO_PHANTOM", "STASIS_BARRIER" -> incomingThreatAngle != null || (nearestEnemy != null && nearestDist < 520f && fluxRatio < 0.75f)
          "HYPER_SURGE" -> lowFluxRetreat || (nearestEnemy != null && nearestDist in 650f..1400f)
          "SHATTER_CONE", "NOVA_RING" -> nearestEnemy != null && nearestDist < 480f
          "SEEKER_TORPEDO" -> nearestEnemy != null && nearestDist < 1100f
          else -> RiftCodex.getBuffRegistryEntry(bot.specialWeaponType)?.isCustomForged == true && nearestEnemy != null && nearestDist < 780f
        }
      if (shouldTriggerBuff && Random.nextFloat() < RiftCalibrationState.botSpecialBuffChance) {
        if (nearestEnemy != null) {
          bot.weaponAngle = atan2(nearestEnemy.y - bot.y, nearestEnemy.x - bot.x)
          bot.independentAimActive = true
        }
        activateShipSpecialBuff(bot)
      }
    }

    // AI Bot Native Split Multi-Shot Toggle (3-Suppressor & 5-Twinfang)
    if (bot.spec.supportsNativeMultifire) {
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
    } else if (nearestEnemy != null && nearestDist < RiftCalibrationState.botEngageRange) {
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

      // Disruptor bots cloak when approaching and unleash 360° AoE EMP Pulse inside empPulseRadius
      if (bot.spec.bombType == "CLOAK_EMP") {
        val pulseR = RiftCalibrationState.empPulseRadius
        if (!bot.isCloaked && nearestDist in (pulseR * 1.15f)..980f && bot.flux > bot.maxFlux * 0.55f && bot.bombTimer <= 0f) {
          fireSecondaryBomb(bot, aimAngle) // Engages Stealth Cloak
        } else if (bot.isCloaked && nearestDist <= pulseR * 0.92f && bot.bombTimer <= 0f) {
          fireSecondaryBomb(bot, aimAngle) // Breaks stealth and unleashes 360° AoE EMP Pulse at 0⚡!
        }
      } else if (bot.spec.bombType == "INTERDICTOR_DOME") {
        if (nearestDist <= 240f && bot.flux > 1100f && bot.bombTimer <= 0f) {
          fireSecondaryBomb(bot, aimAngle)
        }
      } else if (bot.spec.bombType == "PHASE_DASH") {
        if (nearestDist in 450f..900f && bot.flux > bot.maxFlux * 0.55f && bot.bombTimer <= 0f) {
          fireSecondaryBomb(bot, aimAngle)
        }
      } else if (bot.spec.hasBomb && nearestDist in 260f..850f && bot.flux > bot.maxFlux * 0.50f) {
        fireSecondaryBomb(bot, aimAngle)
      }

      if (!bot.isCloaked && nearestDist < RiftCalibrationState.botGunFireRange && bot.flux > bot.maxFlux * 0.28f) {
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

    val thrustSurgeMult = 1f + bot.thrustOverchargeStacks * RiftCalibrationState.overchargeSurgePctPerStack
    val sideDamp = 0.34.pow(dt.toDouble()).toFloat()
    bot.vx = bot.vx * sideDamp + cos(desiredAngle) * bot.spec.thrust * thrustSurgeMult * 0.88f * dt
    bot.vy = bot.vy * sideDamp + sin(desiredAngle) * bot.spec.thrust * thrustSurgeMult * 0.88f * dt
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

    // 1. Local Player Thrust, Afterburner, Hyper Surge, Overcharge Surge, Manual HUD Brake & Live Life Stats
    if (localPlayer.respawnTimer <= 0f) {
      currentLifeStats.timeSurvivedSec += rawDt
      currentLifeStats.bountyReached = max(currentLifeStats.bountyReached, localPlayer.bounty)
      currentLifeStats.shipId = localPlayer.spec.id
      currentLifeStats.shipFlown = formatShipLabel(localPlayer)

      val localInSafe = isInsideSafeZone(localPlayer.x, localPlayer.y)
      localPlayer.inSafeZone = localInSafe
      localPlayer.weaponsLocked = localInSafe
      localPlayer.canFire = !localInSafe
      localPlayer.weaponState = if (localInSafe) "DISARMED" else "ARMED"
      localPlayer.afterburning = false
      val thrustSurgeMult = 1f + localPlayer.thrustOverchargeStacks * RiftCalibrationState.overchargeSurgePctPerStack
      val rocketMult = if (localPlayer.rocketBoostTimer > 0f) RiftCalibrationState.hyperSurgeSpeedMult else 1.0f

      if (leftStickActive && leftStickMag > 0.08f) {
        autoBraking = false
        val isAfterburner = leftStickMag > 0.80f && localPlayer.flux > 25f && localPlayer.rocketBoostTimer <= 0f
        val accel =
          localPlayer.spec.thrust *
            thrustSurgeMult *
            rocketMult *
            (if (isAfterburner) RiftCalibrationState.afterburnerThrustMult else leftStickMag)
        localPlayer.vx += cos(leftStickAngle) * accel * dt
        localPlayer.vy += sin(leftStickAngle) * accel * dt

        if (isAfterburner) {
          localPlayer.afterburning = true
          selfDrainFlux(localPlayer, RiftCalibrationState.afterburnerDrainPerSec * dt)
        }
      } else if (autoBraking) {
        val brakeDamping = RiftCalibrationState.retroBrakeDamping.toDouble().pow(dt.toDouble()).toFloat()
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
          localPlayer.weaponAngle =
            if (RiftCalibrationState.gamepadConnected) localPlayer.angle
            else findQuickTapAutoAimAngle(localPlayer)
        }
        firePrimaryGun(localPlayer)
      } else if (fireStickHasAimVector) {
        localPlayer.independentAimActive = true
        localPlayer.weaponAngle = fireStickAimAngle
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

    // Step Interdictor Stasis Bubbles (15s two-way containment domes)
    for (ibIdx in interdictorBubbles.indices.reversed()) {
      val ib = interdictorBubbles[ibIdx]
      ib.life -= rawDt
      if (ib.life <= 0f) {
        shockwaves.add(ShockwaveEntity(ib.x, ib.y, ib.radius * 0.25f, ib.radius, Color.parseColor("#EC4899")))
        interdictorBubbles.removeAt(ibIdx)
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
          s.speedOverchargeStacks = 0
          s.thrustOverchargeStacks = 0
          s.rechargeOverchargeStacks = 0
          s.multiShotCharges = 0
          s.hasBouncingBulletsUpgrade = false
          s.sniperOverchargeReady = false
          s.isCloaked = false
          s.rocketBoostTimer = 0f
          s.phaseDashTimer = 0f
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
            resetCurrentLifeStats()
          }
          val inSafe = isInsideSafeZone(s.x, s.y)
          s.wasInSafeZone = inSafe
          s.inSafeZone = inSafe
          s.weaponsLocked = inSafe
          s.canFire = !inSafe
          s.weaponState = if (inSafe) "DISARMED" else "ARMED"
          s.phaseShieldTimer = RiftCalibrationState.exitPhaseShieldSec
          s.safeZoneStayTimer = 0f
          s.safeZoneLockoutTimer = 0f
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

      if (s.isHuman && !s.isLocalPlayer && s.remoteFiringPrimary && s.gunTimer <= 0f) {
        s.independentAimActive = true
        firePrimaryGun(s)
      }

      // Step 3.0-second Rocket Hyper-Thruster Boost (1.65x max speed, 0⚡ Flux cost, blazing plasma exhaust trail)
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

      // Step Disruptor Stealth Cloak continuous drain (cloakDrainPerSec; forced uncloak at 150⚡ floor without firing EMP)
      if (s.isCloaked) {
        selfDrainFlux(s, RiftCalibrationState.cloakDrainPerSec * dt)
        if (s.flux <= 150f || isInsideSafeZone(s.x, s.y)) {
          s.isCloaked = false
          if (s.isLocalPlayer) {
            buffVersion++
            onPrizeToast("STEALTH CLOAK DEACTIVATED (FLUX AT 150⚡ SAFETY FLOOR)")
          }
        }
      }

      // Step rapid consecutive Shatter Cone burst
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
          s.safeZoneLockoutTimer = RiftCalibrationState.safeZoneReentryLockoutTime
          s.phaseShieldTimer = RiftCalibrationState.exitPhaseShieldSec
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
            onPrizeToast("SAFE ZONE IDLE LIMIT EXCEEDED — TELEPORTED TO OPEN SECTOR")
          }
        }
      } else {
        s.safeZoneStayTimer = 0f
        if (s.safeZoneLockoutTimer > 0f) {
          s.safeZoneLockoutTimer = max(0f, s.safeZoneLockoutTimer - rawDt)
        }
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
        s.phaseShieldTimer = RiftCalibrationState.exitPhaseShieldSec
      } else {
        if (s.wasInSafeZone) {
          s.wasInSafeZone = false
          s.phaseShieldTimer = RiftCalibrationState.exitPhaseShieldSec
          s.safeZoneLockoutTimer = RiftCalibrationState.safeZoneReentryLockoutTime
          if (s.isLocalPlayer && s.safeZoneLockoutTimer > 0f) {
            onPrizeToast(
              String.format(
                java.util.Locale.US,
                "SAFE ZONE EXITED — %.1fs RE-ENTRY REPULSION LOCKOUT ACTIVE",
                s.safeZoneLockoutTimer,
              )
            )
          }
        } else if (s.phaseShieldTimer > 0f) {
          s.phaseShieldTimer = max(0f, s.phaseShieldTimer - dt)
        }
      }

      val rechargeSurgeMult = 1f + s.rechargeOverchargeStacks * RiftCalibrationState.overchargeSurgePctPerStack
      if (s.empTimer > 0f) {
        s.empTimer = max(0f, s.empTimer - dt)
      } else if (!s.isIdleDraining && !s.isCloaked) {
        val safeBonus = if (currentlyInSafe) RiftCalibrationState.safeZoneRechargeMult else 1.0f
        s.flux = min(s.maxFlux, s.flux + s.spec.rechargeRate * rechargeSurgeMult * safeBonus * dt)
      }

      if (s.phaseDashTimer > 0f) {
        s.phaseDashTimer = max(0f, s.phaseDashTimer - dt)
      }

      val speedSurgeMult = 1f + s.speedOverchargeStacks * RiftCalibrationState.overchargeSurgePctPerStack
      val rocketSpeedMult = if (s.rocketBoostTimer > 0f) RiftCalibrationState.hyperSurgeSpeedMult else 1.0f
      val baseMaxSp =
        (if (s.afterburning) s.spec.afterburnerSpeed else s.spec.topSpeed) * speedSurgeMult * rocketSpeedMult
      val maxSp =
        if (s.phaseDashTimer > 0f) {
          max(baseMaxSp, max(s.spec.afterburnerSpeed * 1.42f, RiftCalibrationState.phaseDashImpulse))
        } else {
          baseMaxSp
        }
      val curSp = hypot(s.vx, s.vy)
      if (curSp > maxSp) {
        s.vx = (s.vx / curSp) * maxSp
        s.vy = (s.vy / curSp) * maxSp
      }

      val prevShipX = s.x
      val prevShipY = s.y
      s.x += s.vx * dt
      s.y += s.vy * dt

      // Two-Way Interdictor Stasis Bubble Containment (0⚡ collision damage, elastic reflection)
      for (ib in interdictorBubbles) {
        val prevDist = hypot(prevShipX - ib.x, prevShipY - ib.y)
        val dx = s.x - ib.x
        val dy = s.y - ib.y
        val curDist = hypot(dx, dy)
        if (prevDist <= ib.radius) {
          val innerLimit = (ib.radius - s.radius).coerceAtLeast(36f)
          if (curDist > innerLimit) {
            val dSafe = curDist.coerceAtLeast(0.001f)
            val nx = dx / dSafe
            val ny = dy / dSafe
            s.x = ib.x + nx * innerLimit
            s.y = ib.y + ny * innerLimit
            val dot = s.vx * nx + s.vy * ny
            if (dot > 0f) {
              val elast = RiftCalibrationState.wallElasticity
              s.vx = (s.vx - 2f * dot * nx) * elast
              s.vy = (s.vy - 2f * dot * ny) * elast
              if (s.isLocalPlayer) sfx.playBounce()
            }
          }
        } else {
          val outerLimit = ib.radius + s.radius
          if (curDist < outerLimit) {
            val dSafe = curDist.coerceAtLeast(0.001f)
            val nx = if (curDist > 0.001f) dx / dSafe else 1f
            val ny = if (curDist > 0.001f) dy / dSafe else 0f
            s.x = ib.x + nx * outerLimit
            s.y = ib.y + ny * outerLimit
            val dot = s.vx * nx + s.vy * ny
            if (dot < 0f) {
              val elast = RiftCalibrationState.wallElasticity
              s.vx = (s.vx - 2f * dot * nx) * elast
              s.vy = (s.vy - 2f * dot * ny) * elast
              if (s.isLocalPlayer) sfx.playBounce()
            }
          }
        }
      }

      // Safe Zone 15-Second Re-Entry Lockout & Strong Outward Repulsion Barrier:
      // If any ship with safeZoneLockoutTimer > 0 touches or attempts to cross back into the Safe Zone boundary,
      // compute the outward unit normal vector (nx, ny) from Safe Zone center to the ship, clamp outside the perimeter,
      // and apply a strong outward physical repulsion impulse (safeZoneRepelForce = 720 PX/S) with crimson shockwave & trauma!
      if (
        s.safeZoneLockoutTimer > 0f &&
          s.x >= safeZone.left - s.radius &&
          s.x <= safeZone.right + s.radius &&
          s.y >= safeZone.top - s.radius &&
          s.y <= safeZone.bottom + s.radius
      ) {
        val szCx = safeZone.centerX()
        val szCy = safeZone.centerY()
        val dx = s.x - szCx
        val dy = s.y - szCy
        val dist = hypot(dx, dy).coerceAtLeast(0.001f)
        val nx = dx / dist
        val ny = dy / dist

        // Clamp ship cleanly outside the closest Safe Zone perimeter axis
        val halfW = safeZone.width() * 0.5f + s.radius + 4f
        val halfH = safeZone.height() * 0.5f + s.radius + 4f
        if (kotlin.math.abs(dx) >= kotlin.math.abs(dy)) {
          s.x = szCx + (if (dx >= 0f) halfW else -halfW)
        } else {
          s.y = szCy + (if (dy >= 0f) halfH else -halfH)
        }

        val repelForce = RiftCalibrationState.safeZoneRepelForce
        s.vx = nx * repelForce
        s.vy = ny * repelForce
        s.inSafeZone = false
        s.wasInSafeZone = false

        shockwaves.add(ShockwaveEntity(s.x, s.y, 16f, 165f, Color.parseColor("#EF4444")))
        if (s.isLocalPlayer) {
          sfx.playBounce()
          addTrauma(0.25f)
          spawnPooledFct(
            text = "⛔ SAFE ZONE LOCKED (${String.format(java.util.Locale.US, "%.1fs", s.safeZoneLockoutTimer)})",
            x = s.x,
            y = s.y - 28f,
            color = Color.parseColor("#EF4444"),
          )
        }
      }

      resolveShipWallBounce(s)

      val postBounceSpeed = hypot(s.vx, s.vy)
      if (postBounceSpeed > 2f) {
        s.angle = atan2(s.vy, s.vx)
      } else if (s.isLocalPlayer && leftStickActive && leftStickMag > 0.08f) {
        s.angle = leftStickAngle
      }

      // Collect 89%-Ship-Size Glowing Buff Pickups (Core 11-Buff Pool 0..10 + Custom AI-Forged Buffs 11+)
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
          val customForgedEntry = if (gr.buffType >= 11) RiftCodex.getBuffRegistryByIndex(gr.buffType) else null
          val pickupBty =
            if (gr.buffType == 6) RiftCalibrationState.fullChargeCrateBounty else RiftCalibrationState.cratePickupBounty
          s.bounty += pickupBty
          when {
            customForgedEntry != null ->
              grantSpecialBuffWeapon(s, customForgedEntry.id, customForgedEntry.chargesPerPickup)
            gr.buffType == 0 -> grantSpecialBuffWeapon(s, "NOVA_RING", RiftCodex.BUFF_REGISTRY["NOVA_RING"]?.chargesPerPickup ?: 1)
            gr.buffType == 1 -> grantSpecialBuffWeapon(s, "SHATTER_CONE", RiftCodex.BUFF_REGISTRY["SHATTER_CONE"]?.chargesPerPickup ?: 2)
            gr.buffType == 2 -> grantSpecialBuffWeapon(s, "SEEKER_TORPEDO", RiftCodex.BUFF_REGISTRY["SEEKER_TORPEDO"]?.chargesPerPickup ?: 1)
            gr.buffType == 3 -> grantSpecialBuffWeapon(s, "HOLO_PHANTOM", RiftCodex.BUFF_REGISTRY["HOLO_PHANTOM"]?.chargesPerPickup ?: 1)
            gr.buffType == 4 -> grantSpecialBuffWeapon(s, "STASIS_BARRIER", RiftCodex.BUFF_REGISTRY["STASIS_BARRIER"]?.chargesPerPickup ?: 1)
            gr.buffType == 5 -> grantSpecialBuffWeapon(s, "HYPER_SURGE", RiftCodex.BUFF_REGISTRY["HYPER_SURGE"]?.chargesPerPickup ?: 1)
            gr.buffType == 6 -> s.flux = s.maxFlux
            gr.buffType == 7 -> s.repels = min(6, s.repels + 1)
            gr.buffType == 8 -> {
              s.multiShotCharges += RiftCalibrationState.crateMultiShotAmmo
              if (s.spec.supportsNativeMultifire) {
                s.multifireOn = true
              }
            }
            gr.buffType == 9 -> {
              s.hasBouncingBulletsUpgrade = true
            }
            gr.buffType == -11 -> {
              s.thrustOverchargeStacks = min(3, s.thrustOverchargeStacks + 1)
            }
            gr.buffType == -12 -> {
              s.rechargeOverchargeStacks = min(3, s.rechargeOverchargeStacks + 1)
            }
            else -> {
              s.speedOverchargeStacks = min(3, s.speedOverchargeStacks + 1)
            }
          }
          if (s.isLocalPlayer) {
            buffVersion++
            sfx.playPrize()
            greensCollected++
            currentLifeStats.buffsCollected++
            currentLifeStats.bountyReached = max(currentLifeStats.bountyReached, localPlayer.bounty)
            onSyndicateContribution(0L, localPlayer.bounty.toLong(), 0L)
            val msg =
              when {
                customForgedEntry != null -> {
                  spawnPooledFct(
                    "🧪 ${customForgedEntry.shortLabel} [${s.specialWeaponCharges}] (0⚡)",
                    s.x,
                    s.y - 24f,
                    customForgedEntry.strokeColorInt,
                  )
                  "FORGED BUFF: ${customForgedEntry.officialName} (+${customForgedEntry.chargesPerPickup} CHARGES • ${s.specialWeaponCharges} TOTAL)"
                }
                gr.buffType == 0 -> {
                  spawnPooledFct("💥 NOVA [${s.specialWeaponCharges}] (0⚡)", s.x, s.y - 24f, Color.parseColor("#F97316"))
                  "SPECIAL BUFF: NOVA RING (+1 CHARGE • ${s.specialWeaponCharges} TOTAL • 0⚡)"
                }
                gr.buffType == 1 -> {
                  spawnPooledFct("🔥 SHATTER [${s.specialWeaponCharges}] (0⚡)", s.x, s.y - 24f, Color.parseColor("#FB7185"))
                  "SPECIAL BUFF: SHATTER CONE (+2 CHARGES • ${s.specialWeaponCharges} TOTAL • 0⚡)"
                }
                gr.buffType == 2 -> {
                  spawnPooledFct("🎯 SEEKER [${s.specialWeaponCharges}] (0⚡)", s.x, s.y - 24f, Color.parseColor("#EF4444"))
                  "SPECIAL BUFF: SEEKER TORPEDO (+1 CHARGE • ${s.specialWeaponCharges} TOTAL • 0⚡)"
                }
                gr.buffType == 3 -> {
                  spawnPooledFct("👥 PHANTOM [${s.specialWeaponCharges}] (0⚡)", s.x, s.y - 24f, Color.parseColor("#2DD4BF"))
                  "SPECIAL BUFF: HOLO PHANTOM (+1 CHARGE • ${s.specialWeaponCharges} TOTAL • 0⚡)"
                }
                gr.buffType == 4 -> {
                  spawnPooledFct("🧱 BARRIER [${s.specialWeaponCharges}] (0⚡)", s.x, s.y - 24f, Color.parseColor("#38BDF8"))
                  "SPECIAL BUFF: STASIS BARRIER (+1 CHARGE • ${s.specialWeaponCharges} TOTAL • 0⚡)"
                }
                gr.buffType == 5 -> {
                  spawnPooledFct("🚀 SURGE [${s.specialWeaponCharges}] (0⚡)", s.x, s.y - 24f, Color.parseColor("#F43F5E"))
                  "SPECIAL BUFF: HYPER SURGE (+1 CHARGE • ${s.specialWeaponCharges} TOTAL • 0⚡)"
                }
                gr.buffType == 6 -> {
                  spawnPooledFct("⚡ FULL FLUX +$pickupBty BTY", s.x, s.y - 24f, Color.parseColor("#10B981"))
                  "INSTANT BUFF: 100% FULL FLUX CHARGE +$pickupBty BOUNTY"
                }
                gr.buffType == 7 -> {
                  spawnPooledFct("🛡️ REPEL +1 (${s.repels})", s.x, s.y - 24f, Color.parseColor("#00F5D4"))
                  "INSTANT BUFF: +1 REPEL CHARGE (${s.repels} TOTAL)"
                }
                gr.buffType == 8 -> {
                  val label =
                    if (s.spec.supportsNativeMultifire) {
                      "🔱 5-STREAM OVERDRIVE [${s.multiShotCharges}]"
                    } else {
                      "🔱 MULTI [${s.multiShotCharges}]"
                    }
                  spawnPooledFct(label, s.x, s.y - 24f, Color.parseColor("#FACC15"))
                  if (s.spec.supportsNativeMultifire) {
                    "SHOT-LIMITED MULTI-SHOT: +${RiftCalibrationState.crateMultiShotAmmo} SHOTS (+25% DMG / 5-STREAM OVERDRIVE!)"
                  } else {
                    "SHOT-LIMITED MULTI-SHOT: +${RiftCalibrationState.crateMultiShotAmmo} SHOTS ON PRIMARY TRIGGER (MULTI [${s.multiShotCharges}])"
                  }
                }
                gr.buffType == 9 -> {
                  spawnPooledFct("⛛ BOUNCE UPGRADE (+1)", s.x, s.y - 24f, Color.parseColor("#A855F7"))
                  "HULL UPGRADE: BOUNCING BULLETS ACTIVE UNTIL DEATH (+1 WALL BOUNCE)"
                }
                gr.buffType == -11 -> {
                  val pct = (s.thrustOverchargeStacks * RiftCalibrationState.overchargeSurgePctPerStack * 100f).toInt()
                  spawnPooledFct("🔥 THR +$pct% (${s.thrustOverchargeStacks}/3)", s.x, s.y - 24f, Color.parseColor("#F59E0B"))
                  "HULL UPGRADE: THRUST OVERCHARGE ${s.thrustOverchargeStacks}/3 (+$pct% THRUST UNTIL DEATH)"
                }
                gr.buffType == -12 -> {
                  val pct = (s.rechargeOverchargeStacks * RiftCalibrationState.overchargeSurgePctPerStack * 100f).toInt()
                  spawnPooledFct("⚡ RCH +$pct% (${s.rechargeOverchargeStacks}/3)", s.x, s.y - 24f, Color.parseColor("#10B981"))
                  "HULL UPGRADE: REACTOR OVERCHARGE ${s.rechargeOverchargeStacks}/3 (+$pct% FLUX RECHARGE UNTIL DEATH)"
                }
                else -> {
                  val pct = (s.speedOverchargeStacks * RiftCalibrationState.overchargeSurgePctPerStack * 100f).toInt()
                  spawnPooledFct("⚡ SPD +$pct% (${s.speedOverchargeStacks}/3)", s.x, s.y - 24f, Color.parseColor("#38BDF8"))
                  "HULL UPGRADE: SPEED OVERCHARGE ${s.speedOverchargeStacks}/3 (+$pct% TOP SPEED UNTIL DEATH)"
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
      var reflectedByBubble = false
      for (ib in interdictorBubbles) {
        val prevDist = hypot(b.x - ib.x, b.y - ib.y)
        val nextDist = hypot(nx - ib.x, ny - ib.y)
        if (prevDist <= ib.radius && nextDist >= ib.radius) {
          val dSafe = nextDist.coerceAtLeast(0.001f)
          val rnx = (nx - ib.x) / dSafe
          val rny = (ny - ib.y) / dSafe
          b.x = ib.x + rnx * (ib.radius - 2f)
          b.y = ib.y + rny * (ib.radius - 2f)
          val dot = b.vx * rnx + b.vy * rny
          if (dot > 0f) {
            b.vx -= 2f * dot * rnx
            b.vy -= 2f * dot * rny
          }
          reflectedByBubble = true
          break
        } else if (prevDist > ib.radius && nextDist <= ib.radius) {
          val dSafe = nextDist.coerceAtLeast(0.001f)
          val rnx = if (nextDist > 0.001f) (nx - ib.x) / dSafe else 1f
          val rny = if (nextDist > 0.001f) (ny - ib.y) / dSafe else 0f
          b.x = ib.x + rnx * (ib.radius + 2f)
          b.y = ib.y + rny * (ib.radius + 2f)
          val dot = b.vx * rnx + b.vy * rny
          if (dot < 0f) {
            b.vx -= 2f * dot * rnx
            b.vy -= 2f * dot * rny
          }
          reflectedByBubble = true
          break
        }
      }
      if (!reflectedByBubble) {
        b.x = nx
        b.y = ny
      }

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
              attackerShipLabel = b.ownerShipLabel,
              weaponLabel = b.weaponLabel,
            )
            bullets.removeAt(i)
            break
          }
        }
      }
    }

    // 4. Step Bombs, Proximity Flak, Mines, Seeker Torpedoes & Custom AI-Forged Orbs
    val seekerCruiseSpeed = RiftCalibrationState.seekerSpeed
    val seekerMaxTurnRad = RiftCalibrationState.seekerTurnRate
    for (i in bombs.indices.reversed()) {
      val b = bombs[i]
      b.life -= dt
      if (b.life <= 0f) {
        val isMissile = b.type == "MISSILE"
        val isCustomOrb = b.type == "CUSTOM_ORB"
        val expRadius =
          when (b.type) {
            "CUSTOM_ORB" -> if (b.customBlastRadius > 0f) b.customBlastRadius else 190f
            "MISSILE" -> 155f
            "SIEGE" -> 220f
            "EMP" -> 240f
            "PROX", "MINE" -> (b.radius + RiftCalibrationState.proxFuseRadius) * 1.35f
            else -> b.radius * 3.8f
          }
        val expDmg = if (isMissile || isCustomOrb) b.damage else b.damage * 0.5f
        if (b.type == "PROX") {
          spawnProximityFlakShrapnel(b)
        }
        for (s in ships) {
          if (!isBombAlliedWithShip(b, s) && s.respawnTimer <= 0f && !isInsideSafeZone(s.x, s.y)) {
            val dist = hypot(s.x - b.x, s.y - b.y)
            if (dist < s.radius + expRadius) {
              val falloff = if (isMissile) (1f - (dist / (s.radius + expRadius)) * 0.35f).coerceIn(0.55f, 1f) else 1f
              val incAngle = atan2(b.y - s.y, b.x - s.x)
              if (isCustomOrb) {
                when (b.customEffect) {
                  CustomBuffEffect.PUSH -> {
                    val pushAng = atan2(s.y - b.y, s.x - b.x)
                    s.vx += cos(pushAng) * 620f
                    s.vy += sin(pushAng) * 620f
                  }
                  CustomBuffEffect.PULL -> {
                    val pullAng = atan2(b.y - s.y, b.x - s.x)
                    s.vx += cos(pullAng) * 420f
                    s.vy += sin(pullAng) * 420f
                  }
                  CustomBuffEffect.SLOW -> {
                    s.vx *= 0.25f
                    s.vy *= 0.25f
                  }
                  else -> {}
                }
              }
              applyProjectileDamage(
                target = s,
                attackerId = b.ownerId,
                attackerIsHuman = b.ownerIsHuman,
                rawDamage = expDmg * falloff,
                impactAngle = incAngle,
                isHeavyImpact = true,
                isEmp = b.type == "EMP" || (isCustomOrb && b.customEffect == CustomBuffEffect.EMP),
                attackerShipLabel = b.ownerShipLabel,
                weaponLabel = b.weaponLabel,
              )
            }
          }
        }
        if ((isMissile || isCustomOrb) && hypot(b.x - localPlayer.x, b.y - localPlayer.y) < 1100f) {
          sfx.playExplosion()
        }
        if (isCustomOrb) {
          shockwaves.add(ShockwaveEntity(b.x, b.y, 16f, expRadius, if (b.customColor != 0) b.customColor else Color.parseColor("#22D3EE")))
        }
        spawnExplosion(
          b.x,
          b.y,
          when (b.type) {
            "CUSTOM_ORB" -> if (b.customColor != 0) b.customColor else Color.parseColor("#22D3EE")
            "EMP" -> Color.parseColor("#B388FF")
            "MISSILE" -> Color.parseColor("#EF4444")
            else -> Color.parseColor("#F59E0B")
          },
        )
        bombs.removeAt(i)
        continue
      }

      // Continuous field physics for Custom AI-Forged Orbs (Gravity Pull, Repulse Push, Cryo Slow)
      if (b.type == "CUSTOM_ORB" && b.customEffect in setOf(CustomBuffEffect.PULL, CustomBuffEffect.PUSH, CustomBuffEffect.SLOW)) {
        val fieldR = b.customBlastRadius.coerceAtLeast(120f)
        for (s in ships) {
          if (!isBombAlliedWithShip(b, s) && s.respawnTimer <= 0f && !isInsideSafeZone(s.x, s.y)) {
            val dx = b.x - s.x
            val dy = b.y - s.y
            val d = hypot(dx, dy).coerceAtLeast(1f)
            if (d < fieldR) {
              val nx = dx / d
              val ny = dy / d
              val strength = (1f - d / fieldR).coerceIn(0.25f, 1f)
              when (b.customEffect) {
                CustomBuffEffect.PULL -> {
                  s.vx += nx * 560f * strength * dt
                  s.vy += ny * 560f * strength * dt
                }
                CustomBuffEffect.PUSH -> {
                  s.vx -= nx * 640f * strength * dt
                  s.vy -= ny * 640f * strength * dt
                }
                CustomBuffEffect.SLOW -> {
                  val damp = 0.16.pow(dt.toDouble()).toFloat()
                  s.vx *= damp
                  s.vy *= damp
                }
                else -> {}
              }
            }
          }
        }
      }

      // Seeker Torpedo homing (checks Holo Phantom decoys first, then uncloaked ships!)
      if (b.type == "MISSILE") {
        val lockedDecoy = decoys.firstOrNull { it.id == b.targetShipId }
        if (lockedDecoy != null) {
          val desiredAngle = atan2(lockedDecoy.y - b.y, lockedDecoy.x - b.x)
          val curAngle = atan2(b.vy, b.vx)
          val diff = atan2(sin(desiredAngle - curAngle), cos(desiredAngle - curAngle))
          val maxTurn = seekerMaxTurnRad * dt
          val newAngle = curAngle + diff.coerceIn(-maxTurn, maxTurn)
          b.vx = cos(newAngle) * seekerCruiseSpeed
          b.vy = sin(newAngle) * seekerCruiseSpeed
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
            val maxTurn = seekerMaxTurnRad * dt
            val newAngle = curAngle + diff.coerceIn(-maxTurn, maxTurn)
            b.vx = cos(newAngle) * seekerCruiseSpeed
            b.vy = sin(newAngle) * seekerCruiseSpeed
          } else {
            val curSp = hypot(b.vx, b.vy).coerceAtLeast(1f)
            b.vx = (b.vx / curSp) * seekerCruiseSpeed
            b.vy = (b.vy / curSp) * seekerCruiseSpeed
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
              val bounceRestitution = if (b.type == "MISSILE" || b.type == "BOUNCE" || b.type == "CUSTOM_ORB") 1.0f else 0.85f
              if (!prevInX) b.vx = -b.vx * bounceRestitution else b.vy = -b.vy * bounceRestitution
            } else {
              if (b.type == "PROX") spawnProximityFlakShrapnel(b)
              bombs.removeAt(i)
              spawnExplosion(
                b.x,
                b.y,
                when (b.type) {
                  "CUSTOM_ORB" -> if (b.customColor != 0) b.customColor else Color.parseColor("#22D3EE")
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
                val bounceRestitution = if (b.type == "MISSILE" || b.type == "BOUNCE" || b.type == "CUSTOM_ORB") 1.0f else 0.85f
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
      var bombReflectedByBubble = false
      for (ib in interdictorBubbles) {
        val prevDist = hypot(b.x - ib.x, b.y - ib.y)
        val nextDist = hypot(nx - ib.x, ny - ib.y)
        if (prevDist <= ib.radius && nextDist >= ib.radius) {
          val dSafe = nextDist.coerceAtLeast(0.001f)
          val rnx = (nx - ib.x) / dSafe
          val rny = (ny - ib.y) / dSafe
          b.x = ib.x + rnx * (ib.radius - 2f)
          b.y = ib.y + rny * (ib.radius - 2f)
          val dot = b.vx * rnx + b.vy * rny
          if (dot > 0f) {
            b.vx -= 2f * dot * rnx
            b.vy -= 2f * dot * rny
          }
          bombReflectedByBubble = true
          break
        } else if (prevDist > ib.radius && nextDist <= ib.radius) {
          val dSafe = nextDist.coerceAtLeast(0.001f)
          val rnx = if (nextDist > 0.001f) (nx - ib.x) / dSafe else 1f
          val rny = if (nextDist > 0.001f) (ny - ib.y) / dSafe else 0f
          b.x = ib.x + rnx * (ib.radius + 2f)
          b.y = ib.y + rny * (ib.radius + 2f)
          val dot = b.vx * rnx + b.vy * rny
          if (dot < 0f) {
            b.vx -= 2f * dot * rnx
            b.vy -= 2f * dot * rny
          }
          bombReflectedByBubble = true
          break
        }
      }
      if (!bombReflectedByBubble) {
        b.x = nx
        b.y = ny
      }

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
          "PROX" -> RiftCalibrationState.proxFuseRadius
          "MINE" -> b.radius + 28f
          "MISSILE" -> b.radius + 10f
          "CUSTOM_ORB" -> if (b.customEffect == CustomBuffEffect.PULL) b.radius + 6f else b.radius + 14f
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
                "CUSTOM_ORB" -> if (b.customBlastRadius > 0f) b.customBlastRadius else 190f
                "EMP" -> 240f
                "SIEGE" -> 210f
                else -> s.radius + triggerRadius + 24f
              }
            for (victim in ships) {
              if (!isBombAlliedWithShip(b, victim) && victim.respawnTimer <= 0f && !isInsideSafeZone(victim.x, victim.y)) {
                val d = hypot(victim.x - b.x, victim.y - b.y)
                if (d <= blastRadius) {
                  val incAngle = atan2(b.y - victim.y, b.x - victim.x)
                  val scale = if (victim.id == s.id || b.type == "EMP" || b.type == "CUSTOM_ORB") 1.0f else 0.65f
                  if (b.type == "CUSTOM_ORB") {
                    when (b.customEffect) {
                      CustomBuffEffect.PUSH -> {
                        val pushAng = atan2(victim.y - b.y, victim.x - b.x)
                        victim.vx += cos(pushAng) * 620f
                        victim.vy += sin(pushAng) * 620f
                      }
                      CustomBuffEffect.PULL -> {
                        val pullAng = atan2(b.y - victim.y, b.x - victim.x)
                        victim.vx += cos(pullAng) * 420f
                        victim.vy += sin(pullAng) * 420f
                      }
                      CustomBuffEffect.SLOW -> {
                        victim.vx *= 0.25f
                        victim.vy *= 0.25f
                      }
                      else -> {}
                    }
                  }
                  applyProjectileDamage(
                    target = victim,
                    attackerId = b.ownerId,
                    attackerIsHuman = b.ownerIsHuman,
                    rawDamage = b.damage * scale,
                    impactAngle = incAngle,
                    isHeavyImpact = true,
                    isEmp = b.type == "EMP" || (b.type == "CUSTOM_ORB" && b.customEffect == CustomBuffEffect.EMP),
                    attackerShipLabel = b.ownerShipLabel,
                    weaponLabel = b.weaponLabel,
                  )
                }
              }
            }
            if (b.type == "CUSTOM_ORB") {
              shockwaves.add(ShockwaveEntity(b.x, b.y, 16f, blastRadius, if (b.customColor != 0) b.customColor else Color.parseColor("#22D3EE")))
            }
            spawnExplosion(
              b.x,
              b.y,
              when (b.type) {
                "CUSTOM_ORB" -> if (b.customColor != 0) b.customColor else Color.parseColor("#22D3EE")
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

    for (i in phaseAfterimages.indices.reversed()) {
      val pa = phaseAfterimages[i]
      pa.life -= rawDt
      if (pa.life <= 0f) phaseAfterimages.removeAt(i)
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

      // Physics fields per Rift type (100% live-wired to RiftCalibrationState)
      when (anom.type) {
        AnomalyType.PLASMA_RIFT -> {
          val tearRadius = anom.physicalRadius
          val dmgPct = RiftCalibrationState.plasmaRiftDamagePct
          for (s in ships) {
            if (s.respawnTimer > 0f || isInsideSafeZone(s.x, s.y)) continue
            val dist = hypot(s.x - anom.x, s.y - anom.y)
            if (dist < tearRadius + s.radius * 0.4f && s.plasmaRiftTickTimer <= 0f) {
              s.plasmaRiftTickTimer = 0.60f
              val dmg = s.maxFlux * dmgPct
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
                currentLifeStats.damageTaken += dmg
                sfx.playBounce()
                addTrauma(0.24f)
                spawnPooledFct(
                  text = "☢ PLASMA RIFT -${dmg.toInt()}⚡ (${(dmgPct * 100f).toInt()}%)",
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
          val fieldRadius = RiftCalibrationState.repulsorFieldRadius
          val baseRepulse = RiftCalibrationState.repulsorBaseForce
          for (s in ships) {
            if (s.respawnTimer > 0f) continue
            val dx = s.x - anom.x
            val dy = s.y - anom.y
            val dist = hypot(dx, dy).coerceAtLeast(0.01f)
            if (dist < fieldRadius) {
              val nx = dx / dist
              val ny = dy / dist
              val approachSpeed = -(s.vx * nx + s.vy * ny)
              val slingshotBonus = if (approachSpeed > 0f) approachSpeed * 2.8f else 0f
              val forceMag = (baseRepulse + slingshotBonus) * (1f - dist / fieldRadius).coerceIn(0.2f, 1f)
              s.vx += nx * forceMag * dt
              s.vy += ny * forceMag * dt

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
          val pullRadius = RiftCalibrationState.gravityPullRadius
          val pullStrength = RiftCalibrationState.gravityPullForce
          val coreRadius = anom.physicalRadius * 0.42f

          for (s in ships) {
            if (s.respawnTimer > 0f) continue
            val dx = anom.x - s.x
            val dy = anom.y - s.y
            val dist = hypot(dx, dy).coerceAtLeast(0.01f)

            if (dist < coreRadius) {
              if (s.blackHoleImmunityTimer <= 0f) {
                s.vx = 0f
                s.vy = 0f
                s.blackHoleImmunityTimer = RiftCalibrationState.gravityStopImmunitySec
                spawnExplosion(s.x, s.y, Color.parseColor("#C084FC"))
                if (s.isLocalPlayer) {
                  sfx.playBounce()
                  spawnPooledFct("⚠️ GRAVITY RIFT: FULL STOP", s.x, s.y - 32f, Color.parseColor("#A855F7"))
                }
              }
            } else if (dist < pullRadius) {
              if (s.blackHoleImmunityTimer <= 0f) {
                val nx = dx / dist
                val ny = dy / dist
                val pullMag = pullStrength * (1f - dist / pullRadius).coerceIn(0.15f, 1f)
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
          val portalRadius = anom.physicalRadius * 0.52f
          val phaseCd = RiftCalibrationState.phaseRiftCooldownSec
          val partner = anomalies.firstOrNull { it.id == anom.partnerId }
          if (partner != null) {
            for (s in ships) {
              if (s.respawnTimer > 0f) continue
              if (s.wormholeCooldownTimer <= 0f && hypot(s.x - anom.x, s.y - anom.y) < portalRadius + s.radius * 0.5f) {
                s.x = partner.x
                s.y = partner.y
                s.wormholeCooldownTimer = phaseCd
                spawnExplosion(anom.x, anom.y, Color.parseColor("#2DD4BF"))
                spawnExplosion(partner.x, partner.y, Color.parseColor("#2DD4BF"))
                if (s.isLocalPlayer) {
                  sfx.playWarp()
                  spawnPooledFct("🌌 PHASE RIFT TRANSIT", s.x, s.y - 36f, Color.parseColor("#2DD4BF"))
                }
              }
            }

            for (b in bullets) {
              if (b.wormholeCooldownTimer <= 0f && hypot(b.x - anom.x, b.y - anom.y) < portalRadius) {
                b.x = partner.x
                b.y = partner.y
                b.wormholeCooldownTimer = phaseCd
              }
            }

            for (bm in bombs) {
              if (bm.wormholeCooldownTimer <= 0f && hypot(bm.x - anom.x, bm.y - anom.y) < portalRadius) {
                bm.x = partner.x
                bm.y = partner.y
                bm.wormholeCooldownTimer = phaseCd
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
