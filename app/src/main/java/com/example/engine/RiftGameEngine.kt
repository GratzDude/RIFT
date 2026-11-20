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
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

enum class SpaceObstacleStyle {
  ORBITAL_STATION,
  ASTEROID_STATION,
  CRYSTAL_ASTEROID,
  FLOATING_DEBRIS,
  PERMANENT_BARRIER,
}

typealias SpaceObstacleType = SpaceObstacleStyle

data class WallRect(
  var x: Float,
  var y: Float,
  val w: Float,
  val h: Float,
  val style: SpaceObstacleStyle = SpaceObstacleStyle.ORBITAL_STATION,
  val variantSeed: Int = 0,
  var hp: Float = 2400f,
  val maxHp: Float = 2400f,
  var stationHitCounter: Int = 0,
  var hitFlashTimer: Float = 0f,
  var lastHitAngle: Float = 0f,
  var lastAttackerShipId: String = "",
) {
  val type: SpaceObstacleType
    get() = style
  val seed: Int
    get() = variantSeed
}

data class GreenCrate(
  var x: Float,
  var y: Float,
  var spinPhase: Float,
  val buffType: Int = 0, // 0=Burst, 1=Shotgun, 2=Missile, 3=Decoy, 4=Brick, 5=Rocket, 6=Full Flux, 7=Repel, 8=Multi-Shot(+25), 9=Bouncing Bullets(+30), 10=Overcharge Surge(+8%), 11=Swarm(5 Drones)
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
  var phaseRiftCooldownTimer: Float = 0f,
)

data class BrickWallEntity(
  val id: String,
  val ownerId: String,
  val freq: Int,
  var x: Float,
  var y: Float,
  val w: Float,
  val h: Float,
  var life: Float = 45.0f,
  val maxLife: Float = 45.0f,
  var stackCount: Int = 1,
)

enum class RankedGameMode(
  val id: String,
  val title: String,
  val subtitle: String,
  val teamFormatBadge: String,
) {
  SECTOR_CONVERGENCE(
    "SECTOR_CONVERGENCE",
    "Sector Convergence",
    "Free-For-All • 1-5 Strike Wing Resonance",
    "20 PILOTS • 60M",
  ),
  RIFT_SKIRMISH(
    "RIFT_SKIRMISH",
    "Rift Skirmish",
    "Team Deathmatch • 3m Rounds • No Respawn",
    "2v2 - 5v5 • BO3",
  ),
  FLUX_CORE(
    "FLUX_CORE",
    "Flux Core",
    "Hold the Core for +15 RES/s & Overcharge",
    "20 PILOTS • BALL",
  ),
  STASIS_BREACH(
    "STASIS_BREACH",
    "Stasis Breach",
    "10v10 • 3m Stasis Captures or Lockdown",
    "10v10 • 3M",
  ),
  RIFT_CONTROL(
    "RIFT_CONTROL",
    "Rift Control",
    "2-Rift Tug-of-War • Hold 15s to Lock",
    "3v3 - 10v10 • 3M",
  ),
  SYNDICATE_WARS(
    "SYNDICATE_WARS",
    "Syndicate Wars",
    "Ranked Syndicate-vs-Syndicate Battles",
    "3v3 / 5v5 / 10v10",
  );

  companion object {
    fun fromId(raw: String): RankedGameMode {
      val clean = raw.trim().uppercase()
      return entries.firstOrNull { it.id == clean }
        ?: when (clean) {
          "FFA", "CONVERGENCE" -> SECTOR_CONVERGENCE
          "SKIRMISH", "TDM", "3V3", "5V5" -> RIFT_SKIRMISH
          "CORE", "JUGGERNAUT" -> FLUX_CORE
          "BREACH", "JAILBREAK", "10V10" -> STASIS_BREACH
          "CONTROL", "RIFT_CONTROL", "TUG", "KOTH" -> RIFT_CONTROL
          "WARS", "CLAN_WARS" -> SYNDICATE_WARS
          else -> SECTOR_CONVERGENCE
        }
    }
  }
}

data class CustomGameConfig(
  val instanceId: String,
  val title: String,
  val hostCallsign: String,
  val baseMode: RankedGameMode,
  val teamSizeBracket: Int = 5,
  val matchDurationMinutes: Int = 10, // 3..60m
  val allowArmadasInFfa: Boolean = true,
  val botsEnabled: Boolean = true,
  val bulletSizeMult: Float = 1.0f, // 1.0f = Standard, 2.2f = MegaFat Bullets
  val damageMult: Float = 1.0f, // 0.5f..2.5f
  val speedMult: Float = 1.0f, // 0.7f..1.8f
  val healthMult: Float = 1.0f, // 0.5f..2.5f
  val rechargeMult: Float = 1.0f, // 0.5f..2.5f
  val crateSpawnMult: Float = 1.0f, // 0.0f..2.5f
) {
  val projectileSizeMult: Float
    get() = bulletSizeMult
}

data class FluxCoreOrbState(
  var x: Float = 4000f,
  var y: Float = 4000f,
  var vx: Float = 0f,
  var vy: Float = 0f,
  var carrierShipId: Int = -1,
  var holdDurationSec: Float = 0f,
  var overchargeTier: Int = 0, // 0..10 (+5% weapon damage, +5% Flux recharge, +4% top speed per tier)
  var ejectDecelTimerSec: Float = 0f, // 30.0s supersonic elastic wall-bounce deceleration
  var pickupImmunitySec: Float = 0f,
  var pulsePhase: Float = 0f,
)

data class ShipEntity(
  val id: String,
  var callsign: String,
  var syndicateTag: String,
  var armadaName: String = "", // "" = [SOLO], non-empty = [ARMADA_NAME]
  var shipId: Int,
  var spec: ShipSpec,
  var freq: Int, // 0 = Allied/West Horizon coloring, 1 = Rival/East Horizon coloring
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
  var liveResonanceRating: Int = 0,
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
  var specialWeaponType: String = "NONE",
  var specialWeaponCharges: Int = 0,
  var specialWeaponStacks: Int = 0,
  var shotgunWavesRemaining: Int = 0,
  var shotgunWaveTimer: Float = 0f,
  var shotgunAimAngle: Float = 0f,
  var novaWavesRemaining: Int = 0,
  var novaWaveTimer: Float = 0f,
  var tractorBeamActive: Boolean = false,
  var tractorFromSecondary: Boolean = false,
  var tractorTargetShipId: String = "",
  var lanceBeamActive: Boolean = false,
  var lanceFromSecondary: Boolean = false,
  var lanceActiveStacks: Int = 1,
  var lanceTickTimer: Float = 0f,
  var lanceRenderEndX: Float = 0f,
  var lanceRenderEndY: Float = 0f,
  var lanceRenderWidthPx: Float = 14f,
  var lanceHitSparkActive: Boolean = false,
  var multiShotCharges: Int = 0, // Shot-limited Multi-Shot (+25 per pickup, stackable)
  var hasBouncingBulletsUpgrade: Boolean = false, // Permanent-until-death +1 primary wall bounce upgrade
  var speedOverchargeStacks: Int = 0, // Stacks up to 3x (+8% Top Speed per stack until death)
  var thrustOverchargeStacks: Int = 0, // Stacks up to 3x (+8% Thrust per stack until death)
  var rechargeOverchargeStacks: Int = 0, // Stacks up to 3x (+8% Flux Recharge per stack until death)
  var turnOverchargeStacks: Int = 0, // Stacks up to 3x (+12% Turn Rate per stack until death)
  var maxFluxOverchargeStacks: Int = 0, // Stacks up to 3x (+10% Max Flux per stack until death)
  var sniperOverchargeReady: Boolean = false, // Viper / Phase Dash overcharges next rail shot (+35% vel & dmg)
  var isCloaked: Boolean = false, // Disruptor / Cloak+EMP stealth cloak (36 Flux/s drain, hides ship & minimap dot)
  var rocketBoostTimer: Float = 0f, // 3.0s Hyper Surge boost (1.65x max speed at 0 Flux)
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
  var phaseRiftCooldownTimer: Float = 0f,
  var plasmaRiftTickTimer: Float = 0f,
  var missileLockWarningTimer: Float = 0f,
  var missileBeepCooldown: Float = 0f,
  var safeZoneStayTimer: Float = 0f,
  var playfieldIdleTimer: Float = 0f,
  var isIdleDraining: Boolean = false,
  var matchPresenceTimeSec: Float = 0f,
  var remoteFiringPrimary: Boolean = false,
  var lastRemoteSecondarySeq: Long = 0L,
  var isInStasisBrig: Boolean = false,
  val aiSeed: Float = Random.nextFloat() * 100f,
  var visualBankRoll: Float = 0f,
  var barrelRollTimer: Float = 0f,
  var barrelRollDuration: Float = 0.48f,
  var barrelRollDir: Float = 1f,
  var lastBarrelRollCooldown: Float = 0f,
  var hullImpactJoltTimer: Float = 0f,
  var hullImpactNormalX: Float = 0f,
  var hullImpactNormalY: Float = 0f,
  var hullImpactType: Int = 0,
  var equippedKillFinisher: Int = 0,
  var equippedWarpStyle: Int = 0,
  var equippedHvtHaloStyle: Int = 0,
  var warpInAnimTimer: Float = 0.65f,
  var deathLingerTimer: Float = 0f,
  var prevHeadingForBank: Float = angle,
  var pendingRespawnBountyPenalty: Int = 0,
  var netTargetX: Float = x,
  var netTargetY: Float = y,
  var netTargetVx: Float = vx,
  var netTargetVy: Float = vy,
  var netTargetAngle: Float = angle,
  var netTargetWeaponAngle: Float = weaponAngle,
  var netPacketAgeSec: Float = 0f,
  var netSmoothOffsetX: Float = 0f,
  var netSmoothOffsetY: Float = 0f,
  var netSmoothVelX: Float = 0f,
  var netSmoothVelY: Float = 0f,
  var netWasRespawning: Boolean = false,
  var netLocalKillGraceSec: Float = 0f,
  var netRespawnHoldSec: Float = 0f,
  var lastNetUpdateElapsedMs: Long = 0L,
  var hasNetTarget: Boolean = false,
) {
  var empDisableTimer: Float
    get() = empTimer
    set(value) {
      empTimer = value
    }

  var spawnShieldTimer: Float
    get() = phaseShieldTimer
    set(value) {
      phaseShieldTimer = value
    }

  var safeZoneTimer: Float
    get() = safeZoneStayTimer
    set(value) {
      safeZoneStayTimer = value
    }

  var safeZoneLockedTimer: Float
    get() = safeZoneLockoutTimer
    set(value) {
      safeZoneLockoutTimer = value
    }
}

data class BulletEntity(
  val ownerId: String,
  val ownerShipId: Int = 1,
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
  var phaseRiftCooldownTimer: Float = 0f,
  val ownerShipLabel: String = "Viper",
  val weaponLabel: String = "Primary Blaster",
  val triggeredByShipId: String = "",
)

data class BombEntity(
  val ownerId: String,
  val ownerCallsign: String,
  val ownerSyndicateTag: String,
  val ownerArmadaName: String,
  val ownerIsHuman: Boolean,
  val freq: Int,
  val type: String, // "BOUNCE", "SIEGE", "EMP", "PROX", "MINE", "MISSILE", "PROX_MISSILE", "CUSTOM_ORB"
  var x: Float,
  var y: Float,
  var vx: Float,
  var vy: Float,
  val damage: Float,
  var bouncesLeft: Int,
  val passWalls: Boolean,
  var life: Float,
  val radius: Float,
  var phaseRiftCooldownTimer: Float = 0f,
  var targetShipId: String = "",
  val ownerShipLabel: String = "Ricochet",
  val weaponLabel: String = "Warhead",
  val customColor: Int = 0,
  val customEffect: CustomBuffEffect = CustomBuffEffect.NONE,
  val customBlastRadius: Float = 0f,
  var armTimer: Float = 0.35f,
  val isInfiniteProxMissile: Boolean = false,
  val proxLockRadius: Float = 0f,
  val shipsInsideRadiusPrevStep: MutableSet<String> = mutableSetOf(),
  val recentHitShipTimers: MutableMap<String, Float> = mutableMapOf(),
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
  var maxLife: Float = 45.0f,
  var spinAngle: Float = 0f,
  var partnerId: String = "",
  var absorbedWeaponCount: Int = 0,
  var isUnstableMergedRift: Boolean = false,
  var lastIngressAngle: Float = 0f,
  var lastIngressTimer: Float = 0f,
  var secondaryIngressAngle: Float = 0f,
  var secondaryIngressTimer: Float = 0f,
) {
  fun recordBoundaryIngress(angleRad: Float, durationSec: Float = 0.56f) {
    if (lastIngressTimer > 0.08f && kotlin.math.abs(atan2(sin(angleRad - lastIngressAngle), cos(angleRad - lastIngressAngle))) > 0.28f) {
      secondaryIngressAngle = lastIngressAngle
      secondaryIngressTimer = lastIngressTimer
    }
    lastIngressAngle = angleRad
    lastIngressTimer = max(lastIngressTimer, durationSec)
  }
}

enum class PracticeTutorialStep(
  val stepNumber: Int,
  val title: String,
  val body: String,
) {
  MOVEMENT_AND_BOOST(
    stepNumber = 1,
    title = "LEFT STICK: DRIFT & BOOST",
    body =
      "Drag the Left Stick to thrust with zero-G Newtonian drift. Push past the 80% amber ring to ignite Afterburner boost.",
  ),
  FIRING_AND_SHARED_FLUX(
    stepNumber = 2,
    title = "RIGHT STICK & SHARED FLUX",
    body =
      "Aim and fire with the Right Stick & Secondary controls. Weapons and hull shields share the center Flux Ring—over-firing drains your shields.",
  ),
  BUFF_CRATE(
    stepNumber = 3,
    title = "SUPPLY POD BUFF CRATES",
    body =
      "Fly over glowing 3D Supply Pods to equip tactical ordnance, Swarm Drones, Tractor/Lance beams, or stacked ammo upgrades.",
  ),
  OBSTACLE(
    stepNumber = 4,
    title = "OBSTACLES & ARMED STATIONS",
    body =
      "Crystal Asteroids shatter at 2,400 Flux damage to drop Supply Pods. Armed Stations return fire and unleash a 360-degree Nova every 10 hits.",
  ),
  PLAYFIELD_RIFT(
    stepNumber = 5,
    title = "ACTIVE PLAYFIELD RIFTS",
    body =
      "Spacetime Rifts bend combat: Phase Rifts teleport ships and fire, Repulsors slingshot hulls, Plasma Rifts sear shields, and Gravity Rifts swallow fire up to 440px.",
  ),
  SAFE_ZONE(
    stepNumber = 6,
    title = "CENTER SAFE-ZONE HARBOR",
    body =
      "The central 5x Orbital Station Safe Harbor disarms weapons and accelerates Flux recharge. Exiting grants a brief Phase Shield.",
  ),
  FREE_FLIGHT(
    stepNumber = 7,
    title = "FREE FLIGHT PRACTICE",
    body =
      "Fly around freely to get the feel of drift turns, bounce barrel-rolls, 1-10 hull swapping at top, and Shared Flux pacing.",
  ),
  ADD_BOT_AND_NEXUS(
    stepNumber = 8,
    title = "ADD SPARRING BOTS OR ENTER RIFT HUB",
    body =
      "Tap the stationary [+ ADD BOT] button in the top-left anytime to spawn AI sparring rivals, or tap [EXIT TO RIFT HUB] when ready for live Rifts.",
  ),
}

data class PracticeHighlightWorldTarget(
  val x: Float,
  val y: Float,
  val radius: Float,
  val tagLabel: String,
)

data class PendingAsteroidRespawn(
  val x: Float,
  val y: Float,
  val w: Float,
  val h: Float,
  val variantSeed: Int,
  var timerSec: Float = 20.0f,
  val style: SpaceObstacleStyle = SpaceObstacleStyle.CRYSTAL_ASTEROID,
)

data class SwarmDroneEntity(
  val id: String,
  val ownerId: String,
  val ownerCallsign: String,
  val ownerSyndicateTag: String,
  val ownerArmadaName: String,
  val ownerIsHuman: Boolean,
  val ownerShipLabel: String,
  val freq: Int,
  var x: Float,
  var y: Float,
  var vx: Float,
  var vy: Float,
  var angle: Float,
  var life: Float = 6.5f,
  val maxLife: Float = 6.5f,
  val damage: Float = 210f,
  var zigPhase: Float = 0f,
  var targetShipId: String = "",
  var phaseRiftCooldownTimer: Float = 0f,
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

data class ShipDebrisChunk(
  var x: Float,
  var y: Float,
  var vx: Float,
  var vy: Float,
  var angle: Float,
  var angVel: Float,
  val size: Float,
  var life: Float,
  val maxLife: Float,
  val trimColor: Int,
  var emberTimer: Float = 0f,
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
  var lastIngressAngle: Float = 0f,
  var lastIngressTimer: Float = 0f,
  var secondaryIngressAngle: Float = 0f,
  var secondaryIngressTimer: Float = 0f,
) {
  fun recordBoundaryIngress(angleRad: Float, durationSec: Float = 0.54f) {
    if (lastIngressTimer > 0.08f && kotlin.math.abs(atan2(sin(angleRad - lastIngressAngle), cos(angleRad - lastIngressAngle))) > 0.28f) {
      secondaryIngressAngle = lastIngressAngle
      secondaryIngressTimer = lastIngressTimer
    }
    lastIngressAngle = angleRad
    lastIngressTimer = max(lastIngressTimer, durationSec)
  }
}

data class ReactivePrecipitateCluster(
  var x: Float,
  var y: Float,
  var vx: Float,
  var vy: Float,
  val anchorX: Float,
  val anchorY: Float,
  val pullToAnchor: Boolean,
  var angle: Float,
  val angVel: Float,
  val radius: Float,
  var life: Float,
  val maxLife: Float,
  val primaryColor: Int,
  val coreColor: Int,
  val facetSeed: Int,
)

data class ReactiveFilamentTrail(
  var x0: Float,
  var y0: Float,
  var ctrlX: Float,
  var ctrlY: Float,
  var x1: Float,
  var y1: Float,
  var vx1: Float,
  var vy1: Float,
  var life: Float,
  val maxLife: Float,
  val outerColor: Int,
  val midColor: Int,
  val coreColor: Int,
  val thickness: Float,
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

private data class SweptWallImpact(
  val t: Float,
  val hitX: Float,
  val hitY: Float,
  val normalX: Float,
  val normalY: Float,
  val wallIndex: Int,
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
    val LEGACY_FOUNDRY_TAGS =
      setOf("KSTL", "AURL", "VRTX", "RUST", "GUE", "GUES", "NOV", "NOVA")

    fun cleanTag(rawTag: String): String {
      val t = rawTag.trim().removeSurrounding("[", "]").trim().uppercase()
      return when {
        t.isEmpty() ||
          t in LEGACY_FOUNDRY_TAGS ||
          t in com.example.data.LocalPilotPrefs.NON_PLAYER_SYNDICATE_TAGS -> "GUEST"
        else -> t.take(3)
      }
    }
  }

  val walls = mutableListOf<WallRect>()
  val brickWalls = mutableListOf<BrickWallEntity>()
  val decoys = mutableListOf<DecoyEntity>()
  val swarmDrones = mutableListOf<SwarmDroneEntity>()
  val pendingAsteroidRespawns = mutableListOf<PendingAsteroidRespawn>()
  private var nextDecoyBrickId: Long = 1L
  private var nextSwarmDroneId: Long = 1L
  private var gravityRadarBakeTimer: Float = 0f
  val safeZone = RectF(3620f, 3620f, 4380f, 4380f)
  val greens = mutableListOf<GreenCrate>()
  val recentBuffSpawnLocations = ArrayDeque<Pair<Float, Float>>()
  val pendingBuffSpawns = mutableListOf<Float>()
  var radarBgBitmap: Bitmap? = null

  fun getSafeZoneRadius(): Float {
    val r = 380f * RiftCalibrationState.safeZoneScale.coerceIn(0.5f, 2.0f)
    safeZone.set(arenaCenterX - r, arenaCenterY - r, arenaCenterX + r, arenaCenterY + r)
    return r
  }

  fun getEffectiveBuffCap(): Int {
    val mult = (currentCustomGame?.crateSpawnMult ?: 1.0f).coerceIn(0f, 3f)
    RiftCalibrationState.buffTargetCountOverride?.let { return (it * mult).toInt().coerceAtLeast(0) }
    val modeCap =
      when (RiftCalibrationState.buffModeOverride.uppercase()) {
        "3V3" -> 12
        "5V5" -> 10
        "10V10" -> 20
        else -> RiftCalibrationState.buffCapFfa
      }
    return (modeCap * mult).toInt().coerceAtLeast(0)
  }

  var mode: String = "SECTOR_CONVERGENCE"
  var activeRankedMode: RankedGameMode = RankedGameMode.SECTOR_CONVERGENCE
  var activeTeamSizeBracket: Int = 3 // 2..5 for RIFT_SKIRMISH, 3/5/10 for RIFT_CONTROL & SYNDICATE_WARS, 10 for STASIS_BREACH
  var teamSize: Int = 20
  var buffVersion: Int = 0
  var isShipLockedForMatch: Boolean = false

  // Cloneable Non-Ranked Custom Game Instances
  val activeCustomGames = mutableListOf<CustomGameConfig>()
  var currentCustomGame: CustomGameConfig? = null

  // Mode 2 (RIFT_SKIRMISH) & Syndicate Wars TDM Best-of-3 State (3m rounds, no respawn until round end, ally spectate, 1v1 escalation & 10s ready-up)
  var skirmishRound: Int = 1
  val skirmishCurrentRound: Int
    get() = skirmishRound
  var skirmishWestRoundWins: Int = 0
  var skirmishEastRoundWins: Int = 0
  var skirmishWestKills: Int = 0
  var skirmishEastKills: Int = 0
  var skirmishRoundTimerSec: Float = 180f
  var skirmishIs1v1Escalated: Boolean = false
  var skirmishAbyssalBlend: Float = 0f
  var skirmish1v1AlertBanner: String = ""
  var skirmish1v1AlertTimer: Float = 0f
  var skirmishReadyCountdownSec: Float = 0f
  val skirmishReadyDurationSec: Float = 10.0f
  var spectatingTargetShipId: String = ""
  var rivalSyndicateWarTag: String = "BOT"
  var onSyndicateWarFinished: (won: Boolean, trophyDelta: Int) -> Unit = { _, _ -> }

  private fun clearShipToggleBuffs(ship: ShipEntity, clearSlotIfActive: Boolean = false) {
    val hadCrateToggleActive =
      (ship.tractorBeamActive && !ship.tractorFromSecondary) ||
        (ship.lanceBeamActive && !ship.lanceFromSecondary)
    ship.tractorBeamActive = false
    ship.tractorFromSecondary = false
    ship.tractorTargetShipId = ""
    ship.lanceBeamActive = false
    ship.lanceFromSecondary = false
    ship.lanceHitSparkActive = false
    ship.lanceTickTimer = 0f
    ship.novaWavesRemaining = 0
    ship.novaWaveTimer = 0f
    if (clearSlotIfActive && hadCrateToggleActive) {
      ship.specialWeaponType = "NONE"
      ship.specialWeaponCharges = 0
      ship.specialWeaponStacks = 0
      if (ship.isLocalPlayer) buffVersion++
    }
  }

  private fun resetDeathmatchEscalationAndReadyState() {
    skirmishIs1v1Escalated = false
    skirmishAbyssalBlend = 0f
    skirmish1v1AlertBanner = ""
    skirmish1v1AlertTimer = 0f
    skirmishReadyCountdownSec = 0f
    sfx.setDynamicEscalationTargetBpm(null)
  }

  // Mode 3 (FLUX_CORE) Volatile Singularity State
  val fluxCoreOrb = FluxCoreOrbState()
  var fluxCoreCarrierEntityId: String = ""
  private var fluxCoreBountyTickAccum: Float = 0f

  // Circular Dynamic-Radius Playfield (centered at 4000, 4000; scales 2100f..3800f with ship count)
  val arenaCenterX: Float = 4000f
  val arenaCenterY: Float = 4000f
  var arenaRadius: Float = 3800f
  private var currentArenaSeed: Int = (System.currentTimeMillis() / 3600_000L).toInt()

  // Mode 4 (STASIS_BREACH) 10v10 Circular Stasis Bubble Jail, 3:00 Timer & Capture Counters
  val stasisJailRadius: Float = 360f
  var westBrigReactorX: Float = 1400f
  val westBrigReactorY: Float = 4000f
  var eastBrigReactorX: Float = 6600f
  val eastBrigReactorY: Float = 4000f
  val westBrigRect = RectF(westBrigReactorX - stasisJailRadius, westBrigReactorY - stasisJailRadius, westBrigReactorX + stasisJailRadius, westBrigReactorY + stasisJailRadius)
  val eastBrigRect = RectF(eastBrigReactorX - stasisJailRadius, eastBrigReactorY - stasisJailRadius, eastBrigReactorX + stasisJailRadius, eastBrigReactorY + stasisJailRadius)
  val brigReactorRadius: Float = 54f
  var westBrigGeneratorHp: Float = 40000f
  var eastBrigGeneratorHp: Float = 40000f
  var stasisBreachMatchTimerSec: Float = 180f
  var stasisWestCaptures: Int = 0
  var stasisEastCaptures: Int = 0
  var localStasisCaptures: Int = 0

  fun getEffectiveBrigReactorMaxHp(): Float =
    RiftCalibrationState.stasisBrigReactorMaxHp.coerceAtLeast(1000f)

  // Mode 5 (RIFT_CONTROL) 2-Rift Tug-of-War (15s Capture Lock) & 1,000-Pt Score Race State
  var riftAlphaX: Float = 2600f
  val riftAlphaY: Float = 4000f
  var riftOmegaX: Float = 5400f
  val riftOmegaY: Float = 4000f
  val riftCaptureRadius: Float = 340f
  var riftAlphaControlSec: Float = 0f // -15f (West Lock) .. +15f (East Lock)
  var riftOmegaControlSec: Float = 0f // -15f (West Lock) .. +15f (East Lock)
  var riftAlphaLockedBy: Int = -1 // -1 = unlocked, 0 = West, 1 = East
  var riftOmegaLockedBy: Int = -1 // -1 = unlocked, 0 = West, 1 = East
  var riftAlphaWestCount: Int = 0
  var riftAlphaEastCount: Int = 0
  var riftOmegaWestCount: Int = 0
  var riftOmegaEastCount: Int = 0
  var riftControlWestScore: Float = 0f
  var riftControlEastScore: Float = 0f
  var riftControlMatchTimerSec: Float = 180f

  // Fluid Up-to-5-Man Named Armada State (Strictly Real Human Players Only)
  var activeArmadaName: String = "" // "" = SOLO
  val activeArmadaSlots = mutableListOf<ArmadaMemberSlot>()
  val armadaMemberSlots: MutableList<ArmadaMemberSlot>
    get() = activeArmadaSlots

  val ships = mutableListOf<ShipEntity>()
  val bullets = mutableListOf<BulletEntity>()
  val bombs = mutableListOf<BombEntity>()
  val particles = mutableListOf<ParticleEntity>()
  val shockwaves = mutableListOf<ShockwaveEntity>()
  val shipDebrisChunks = mutableListOf<ShipDebrisChunk>()
  val shieldRipples = mutableListOf<ShieldRippleEntity>()
  val phaseAfterimages = mutableListOf<PhaseAfterimageEntity>()
  val interdictorBubbles = mutableListOf<InterdictorBubbleEntity>()
  val anomalies = mutableListOf<SpatialAnomalyEntity>()
  val reactivePrecipitates = mutableListOf<ReactivePrecipitateCluster>()
  val reactiveFilaments = mutableListOf<ReactiveFilamentTrail>()
  private var nextAnomalyId: Long = 1L
  private var nextPrecipitateSeed: Int = 1

  private fun spawnDropletReactionBurst(
    originX: Float,
    originY: Float,
    targetOrWakeX: Float,
    targetOrWakeY: Float,
    pullInward: Boolean,
    crystalCount: Int,
    filamentCount: Int,
    outerColor: Int,
    midColor: Int,
    coreColor: Int,
    baseCrystalRadius: Float = 5.6f,
    durationSec: Float = 0.56f,
  ) {
    val dx = targetOrWakeX - originX
    val dy = targetOrWakeY - originY
    val span = hypot(dx, dy).coerceAtLeast(1f)
    val ux = dx / span
    val uy = dy / span
    val px = -uy
    val py = ux

    val maxPrecipitates = 96
    if (reactivePrecipitates.size + crystalCount > maxPrecipitates) {
      val drop = (reactivePrecipitates.size + crystalCount - maxPrecipitates).coerceAtLeast(0)
      repeat(min(drop, reactivePrecipitates.size)) { reactivePrecipitates.removeAt(0) }
    }
    for (c in 0 until crystalCount) {
      val spread = (Random.nextFloat() - 0.5f) * 1.15f
      val baseSpeed = if (pullInward) (115f + Random.nextFloat() * 145f) else (78f + Random.nextFloat() * 135f)
      val dirAng = atan2(uy, ux) + spread * 0.55f
      val seed = nextPrecipitateSeed++
      val life = (durationSec * (0.78f + Random.nextFloat() * 0.36f)).coerceIn(0.24f, 1.15f)
      reactivePrecipitates.add(
        ReactivePrecipitateCluster(
          x = originX + px * spread * 14f,
          y = originY + py * spread * 14f,
          vx = cos(dirAng) * baseSpeed + px * spread * 48f,
          vy = sin(dirAng) * baseSpeed + py * spread * 48f,
          anchorX = targetOrWakeX,
          anchorY = targetOrWakeY,
          pullToAnchor = pullInward,
          angle = Random.nextFloat() * (PI.toFloat() * 2f),
          angVel = (if (seed % 2 == 0) 1f else -1f) * (3.2f + Random.nextFloat() * 4.2f),
          radius = (baseCrystalRadius * (0.76f + Random.nextFloat() * 0.52f)).coerceIn(2.6f, 12.5f),
          life = life,
          maxLife = life,
          primaryColor = if (c % 2 == 0) midColor else outerColor,
          coreColor = coreColor,
          facetSeed = seed,
        )
      )
    }

    val maxFilaments = 84
    if (reactiveFilaments.size + filamentCount > maxFilaments) {
      val drop = (reactiveFilaments.size + filamentCount - maxFilaments).coerceAtLeast(0)
      repeat(min(drop, reactiveFilaments.size)) { reactiveFilaments.removeAt(0) }
    }
    for (f in 0 until filamentCount) {
      val archSign = if (f % 2 == 0) 1f else -1f
      val archOffset = (16f + Random.nextFloat() * 26f) * archSign
      val reach = if (pullInward) min(span * 0.88f, 138f) else (38f + Random.nextFloat() * 58f)
      val endX = originX + ux * reach + px * (archSign * 10f)
      val endY = originY + uy * reach + py * (archSign * 10f)
      val ctrlX = originX + ux * (reach * 0.52f) + px * archOffset
      val ctrlY = originY + uy * (reach * 0.52f) + py * archOffset
      val fLife = (durationSec * (0.72f + Random.nextFloat() * 0.30f)).coerceIn(0.22f, 0.95f)
      reactiveFilaments.add(
        ReactiveFilamentTrail(
          x0 = originX,
          y0 = originY,
          ctrlX = ctrlX,
          ctrlY = ctrlY,
          x1 = endX,
          y1 = endY,
          vx1 = ux * (if (pullInward) 85f else 64f) + px * archSign * 34f,
          vy1 = uy * (if (pullInward) 85f else 64f) + py * archSign * 34f,
          life = fLife,
          maxLife = fLife,
          outerColor = outerColor,
          midColor = midColor,
          coreColor = coreColor,
          thickness = (2.8f + Random.nextFloat() * 1.8f),
        )
      )
    }
  }

  // Executioner Callcard Telemetry (populated on local pilot death)
  var lastKillerCallsign: String = ""
  var lastKillerShipId: Int = 1
  var lastKillerRankTitle: String = "CADET"
  var lastKillerWeaponLabel: String = ""
  var lastKillerFinisherStyle: Int = 0

  // Phase 3.4 Object-Pooled Floating Combat Text (FCT)
  private val fctPool = Array(48) { FloatingPopup(active = false) }
  val floatingPopups = mutableListOf<FloatingPopup>()

  // Running Transparent Fade-Out Kill Feed (Top-Right HUD) & >1,000 Bounty Dual Alert Banner
  private var nextKillFeedId: Long = 1L
  val killFeed = mutableListOf<KillFeedEntry>()
  var highBountyShutdownBanner: String? = null
  var highBountyShutdownTimer: Float = 0f

  private val remotePilots = mutableMapOf<String, ShipEntity>()
  private val onlineRemoteSectorByUid = mutableMapOf<String, String>()
  var currentAuthenticatedUid: String = ""
  var activeSectorInstanceOverride: String? = null
  var localSecondaryFireSeq: Long = 0L
  var localLifecycleSyncSeq: Long = 0L
  var onLocalLifecycleSyncNeeded: () -> Unit = {}
  var onOutboundCombatEvent: (com.example.data.SectorCombatEvent) -> Unit = {}
  private val processedCombatEventIds = LinkedHashSet<String>()

  private fun notifyLocalLifecycleSync() {
    localLifecycleSyncSeq++
    if (!isEphemeralPracticeRift) {
      onLocalLifecycleSyncNeeded()
    }
  }

  fun reattachRemotePilotsToRoster() {
    if ((!hasJoinedMatch && !isHubOverwatchActive) || isEphemeralPracticeRift) return
    for (rp in remotePilots.values) {
      if (rp.id != localPlayer.id && ships.none { it.id == rp.id }) {
        replaceBotWithHuman(rp)
      }
    }
  }

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

  fun isCustomGameActive(): Boolean = currentCustomGame != null

  // False on cold boot so the pilot orbits peacefully in NEXUS without inflating any match count or spawning combat bots
  var hasJoinedMatch: Boolean = false
  var lobbyOrbitAngle: Float = 0f

  // True while the player is browsing the NEXUS Command Hub / Modal windows before deploying into combat
  var isInLobbyStaging: Boolean = true

  // True when flying in the on-demand ephemeral solo PRACTICE RIFT flight sandbox (0 initial bots, 0 remotes, 0 stat mutations)
  var isEphemeralPracticeRift: Boolean = false
  var practiceTutorialActive: Boolean = false
  var practiceTutorialStepIndex: Int = 0
  var practiceTutorialStepJumpFlashSec: Float = 0f
  var practiceSpawnedBotCounter: Int = 0

  fun currentPracticeTutorialStep(): PracticeTutorialStep? =
    if (isEphemeralPracticeRift && practiceTutorialActive) {
      PracticeTutorialStep.entries.getOrNull(practiceTutorialStepIndex)
    } else {
      null
    }

  /**
   * Advances the Practice Rift tutorial strictly on manual [NEXT] button tap (never auto-advanced).
   */
  fun advancePracticeTutorialStep() {
    if (!isEphemeralPracticeRift || !practiceTutorialActive) return
    if (practiceTutorialStepIndex < PracticeTutorialStep.entries.lastIndex) {
      practiceTutorialStepIndex++
      practiceTutorialStepJumpFlashSec = 0.42f
      ensurePracticeTutorialTargetsPresent()
      sfx.playMutedClick()
    } else {
      practiceTutorialActive = false
      practiceTutorialStepJumpFlashSec = 0f
      sfx.playPrize()
    }
    onStatsUpdated()
  }

  /**
   * Dismisses the Practice Rift tutorial strictly on manual [SKIP] button tap while keeping the pilot in Practice Rift.
   */
  fun skipPracticeTutorial() {
    if (!practiceTutorialActive) return
    practiceTutorialActive = false
    practiceTutorialStepJumpFlashSec = 0f
    sfx.playMutedClick()
    onStatsUpdated()
  }

  fun activePracticeBotCount(): Int =
    if (isEphemeralPracticeRift) ships.count { !it.isLocalPlayer && !it.isHuman } else 0

  fun ensurePracticeTutorialTargetsPresent() {
    if (!isEphemeralPracticeRift) return
    if (greens.isEmpty()) {
      val baseAng = localPlayer.angle
      var placed = false
      for (attempt in 0 until 20) {
        val ang = baseAng + (attempt * 0.42f)
        val dist = 340f + (attempt % 4) * 55f
        val gx = (localPlayer.x + cos(ang) * dist).coerceIn(arenaCenterX - arenaRadius + 220f, arenaCenterX + arenaRadius - 220f)
        val gy = (localPlayer.y + sin(ang) * dist).coerceIn(arenaCenterY - arenaRadius + 220f, arenaCenterY + arenaRadius - 220f)
        if (isClearCrateSpawnPosition(gx, gy)) {
          greens.add(
            GreenCrate(
              x = gx,
              y = gy,
              spinPhase = 0f,
              buffType = attempt % 15,
            )
          )
          placed = true
          break
        }
      }
      if (!placed) {
        val sp = getPracticeRiftOpenSpawnPosition()
        val clearSp = resolveClearCrateDropPosition(sp.first, sp.second)
        greens.add(GreenCrate(x = clearSp.first, y = clearSp.second, spinPhase = 0f, buffType = 8))
      }
    }
    if (walls.isEmpty()) {
      val sp = getPracticeRiftOpenSpawnPosition()
      walls.add(
        WallRect(
          x = sp.first - 96f,
          y = sp.second - 96f,
          w = 192f,
          h = 192f,
          style = SpaceObstacleStyle.CRYSTAL_ASTEROID,
          variantSeed = 79,
          hp = 2400f,
          maxHp = 2400f,
        )
      )
    }
    if (anomalies.isEmpty()) {
      spawnSingleAnomaly(AnomalyType.GRAVITY_RIFT)
    }
  }

  fun resolvePracticeTutorialWorldTarget(): PracticeHighlightWorldTarget? {
    val step = currentPracticeTutorialStep() ?: return null
    val px = localPlayer.x
    val py = localPlayer.y
    return when (step) {
      PracticeTutorialStep.BUFF_CRATE -> {
        val crate = greens.minByOrNull { hypot(it.x - px, it.y - py) } ?: return null
        PracticeHighlightWorldTarget(
          x = crate.x,
          y = crate.y,
          radius = 66f,
          tagLabel = "SUPPLY POD CRATE",
        )
      }
      PracticeTutorialStep.OBSTACLE -> {
        val wall =
          walls.minByOrNull { hypot((it.x + it.w * 0.5f) - px, (it.y + it.h * 0.5f) - py) }
            ?: return null
        val wx = wall.x + wall.w * 0.5f
        val wy = wall.y + wall.h * 0.5f
        val rad = (hypot(wall.w, wall.h) * 0.52f + 22f).coerceAtLeast(86f)
        val label =
          when (wall.style) {
            SpaceObstacleStyle.CRYSTAL_ASTEROID -> "CRYSTAL ASTEROID"
            SpaceObstacleStyle.ORBITAL_STATION,
            SpaceObstacleStyle.ASTEROID_STATION -> "ARMED STATION"
            else -> "SECTOR OBSTACLE"
          }
        PracticeHighlightWorldTarget(x = wx, y = wy, radius = rad, tagLabel = label)
      }
      PracticeTutorialStep.PLAYFIELD_RIFT -> {
        val anom = anomalies.minByOrNull { hypot(it.x - px, it.y - py) } ?: return null
        val riftTitle =
          when (anom.type) {
            AnomalyType.GRAVITY_RIFT -> "GRAVITY RIFT"
            AnomalyType.PLASMA_RIFT -> "PLASMA RIFT"
            AnomalyType.REPULSOR_RIFT -> "REPULSOR RIFT"
            AnomalyType.PHASE_RIFT_A,
            AnomalyType.PHASE_RIFT_B -> "PHASE RIFT"
          }
        PracticeHighlightWorldTarget(
          x = anom.x,
          y = anom.y,
          radius = anom.physicalRadius + 28f,
          tagLabel = riftTitle,
        )
      }
      PracticeTutorialStep.SAFE_ZONE -> {
        PracticeHighlightWorldTarget(
          x = arenaCenterX,
          y = arenaCenterY,
          radius = getSafeZoneRadius() + 20f,
          tagLabel = "SAFE-ZONE HARBOR",
        )
      }
      else -> null
    }
  }

  fun spawnPracticeBot(): ShipEntity? {
    if (!isEphemeralPracticeRift) return null
    val idx = practiceSpawnedBotCounter++
    val shipId = (idx % 9) + 1
    val spec = RiftCodex.getShipById(shipId)
    var spawnX = arenaCenterX + 760f
    var spawnY = arenaCenterY
    for (attempt in 0 until 36) {
      val ang = localPlayer.angle + ((attempt * 0.55f) - 1.1f)
      val dist = 620f + (attempt % 5) * 85f
      val candX =
        (localPlayer.x + cos(ang) * dist).coerceIn(
          arenaCenterX - arenaRadius + 200f,
          arenaCenterX + arenaRadius - 200f,
        )
      val candY =
        (localPlayer.y + sin(ang) * dist).coerceIn(
          arenaCenterY - arenaRadius + 200f,
          arenaCenterY + arenaRadius - 200f,
        )
      if (!circleIntersectsWalls(candX, candY, 48f) && !isInsideSafeZone(candX, candY)) {
        spawnX = candX
        spawnY = candY
        break
      }
    }
    val headingToPlayer = atan2(localPlayer.y - spawnY, localPlayer.x - spawnX)
    val bot =
      ShipEntity(
        id = "practice_bot_${idx + 1}",
        callsign = BOT_CALLSIGNS[idx % BOT_CALLSIGNS.size],
        syndicateTag = "BOT",
        armadaName = "",
        shipId = shipId,
        spec = spec,
        freq = 1,
        isHuman = false,
        isLocalPlayer = false,
        x = spawnX,
        y = spawnY,
        angle = headingToPlayer,
        weaponAngle = headingToPlayer,
        bounty = 100,
        phaseShieldTimer = 1.0f,
        warpInAnimTimer = 0.65f,
      )
    ships.add(bot)
    shockwaves.add(ShockwaveEntity(spawnX, spawnY, 16f, 165f, Color.parseColor("#00F5D4")))
    sfx.playWarp()
    onPrizeToast("PRACTICE BOT DEPLOYED // ${activePracticeBotCount()} IN SECTOR")
    onStatsUpdated()
    return bot
  }

  // True when the NEXUS Hub circular Overwatch aperture is observing an active match without the local pilot joined
  var isHubOverwatchActive: Boolean = false
  private var currentOverwatchKey: String = ""

  fun configureHubOverwatchInstance(
    instanceKey: String,
    rankedMode: RankedGameMode,
    customConfig: CustomGameConfig?,
    seedSalt: Int,
  ) {
    if (hasJoinedMatch) return
    isHubOverwatchActive = true
    if (currentOverwatchKey != instanceKey || ships.size <= 1) {
      currentOverwatchKey = instanceKey
      hasJoinedMatch = false
      isInLobbyStaging = true
      isEphemeralPracticeRift = false
      practiceTutorialActive = false
      isShipLockedForMatch = false
      spectatingTargetShipId = ""
      currentCustomGame = customConfig
      activeSectorInstanceOverride = if (instanceKey.equals("SECTOR_CONVERGENCE_2", ignoreCase = true)) "SECTOR_CONVERGENCE_2" else null
      activeRankedMode = rankedMode
      mode = rankedMode.id
      activeTeamSizeBracket =
        customConfig?.teamSizeBracket
          ?: when (rankedMode) {
            RankedGameMode.RIFT_SKIRMISH -> 5
            RankedGameMode.STASIS_BREACH -> 10
            RankedGameMode.RIFT_CONTROL, RankedGameMode.SYNDICATE_WARS -> 5
            else -> 20
          }
      publicMatchDurationSec = 0f
      publicMatchCycleLimitSec =
        if (customConfig != null) {
          (customConfig.matchDurationMinutes.coerceIn(1, 120) * 60f)
        } else {
          RiftCalibrationState.publicMatchCycleLimitSec
        }
      skirmishRound = 1
      skirmishWestRoundWins = 0
      skirmishEastRoundWins = 0
      skirmishWestKills = 0
      skirmishEastKills = 0
      skirmishRoundTimerSec = 180f
      stasisBreachMatchTimerSec = 180f
      stasisWestCaptures = 0
      stasisEastCaptures = 0
      localStasisCaptures = 0

      bullets.clear()
      bombs.clear()
      decoys.clear()
      swarmDrones.clear()
      brickWalls.clear()
      shockwaves.clear()
      shipDebrisChunks.clear()
      interdictorBubbles.clear()
      killFeed.clear()

      currentArenaSeed = seedSalt
      generateSectorArena(seedSalt)
      rebuildRosterForMode(rankedMode.id)
      resetFluxCoreToCenter()
      westBrigGeneratorHp = getEffectiveBrigReactorMaxHp()
      eastBrigGeneratorHp = getEffectiveBrigReactorMaxHp()
      resetRiftControlZones()

      if (customConfig != null) {
        for (s in ships) {
          s.maxFlux = s.spec.maxFlux * customConfig.healthMult
          s.flux = s.maxFlux
        }
      }
    }
    localPlayer.respawnTimer = 999f
    localPlayer.deathLingerTimer = 999f
    localPlayer.isInStasisBrig = false
    localPlayer.x = arenaCenterX
    localPlayer.y = arenaCenterY
    localPlayer.vx = 0f
    localPlayer.vy = 0f
  }

  private fun getPracticeRiftOpenSpawnPosition(): Pair<Float, Float> {
    val minR = (getSafeZoneRadius() + 185f).coerceAtLeast(480f)
    val maxR = (arenaRadius * 0.56f).coerceAtLeast(minR + 240f)
    for (attempt in 0 until 42) {
      val ang = Random.nextFloat() * PI.toFloat() * 2f
      val dist = minR + Random.nextFloat() * (maxR - minR)
      val x = arenaCenterX + cos(ang) * dist
      val y = arenaCenterY + sin(ang) * dist
      if (!circleIntersectsWalls(x, y, 48f) && !isInsideSafeZone(x, y)) {
        return x to y
      }
    }
    return (arenaCenterX + minR) to arenaCenterY
  }

  fun enterEphemeralPracticeRift(startTutorial: Boolean = true) {
    isHubOverwatchActive = false
    currentOverwatchKey = ""
    isEphemeralPracticeRift = true
    practiceTutorialActive = startTutorial
    practiceTutorialStepIndex = 0
    practiceTutorialStepJumpFlashSec = if (startTutorial) 0.42f else 0f
    practiceSpawnedBotCounter = 0
    hasJoinedMatch = true
    isInLobbyStaging = false
    currentCustomGame = null
    activeSectorInstanceOverride = null
    activeRankedMode = RankedGameMode.SECTOR_CONVERGENCE
    mode = "SECTOR_CONVERGENCE"
    isShipLockedForMatch = false
    spectatingTargetShipId = ""

    bullets.clear()
    bombs.clear()
    decoys.clear()
    swarmDrones.clear()
    brickWalls.clear()
    shockwaves.clear()
    shipDebrisChunks.clear()
    interdictorBubbles.clear()
    killFeed.clear()
    remotePilots.clear()

    arenaRadius = computeTargetArenaRadius(6)
    val brigOffset = (arenaRadius - 560f).coerceIn(1350f, 2600f)
    westBrigReactorX = arenaCenterX - brigOffset
    eastBrigReactorX = arenaCenterX + brigOffset
    westBrigRect.set(
      westBrigReactorX - stasisJailRadius,
      westBrigReactorY - stasisJailRadius,
      westBrigReactorX + stasisJailRadius,
      westBrigReactorY + stasisJailRadius,
    )
    eastBrigRect.set(
      eastBrigReactorX - stasisJailRadius,
      eastBrigReactorY - stasisJailRadius,
      eastBrigReactorX + stasisJailRadius,
      eastBrigReactorY + stasisJailRadius,
    )
    val riftOffset = (arenaRadius * 0.40f).coerceIn(820f, 1450f)
    riftAlphaX = arenaCenterX - riftOffset
    riftOmegaX = arenaCenterX + riftOffset

    generateSectorArena(Random.nextInt(10_000, 999_999))

    // Strict 0-bot, 0-remote initial solo Practice Rift roster
    ships.clear()
    ships.add(localPlayer)

    val sp = getPracticeRiftOpenSpawnPosition()
    localPlayer.respawnTimer = 0f
    localPlayer.deathLingerTimer = 0f
    localPlayer.hullImpactJoltTimer = 0f
    localPlayer.warpInAnimTimer = 0.65f
    localPlayer.x = sp.first
    localPlayer.y = sp.second
    localPlayer.vx = 0f
    localPlayer.vy = 0f
    localPlayer.kills = 0
    localPlayer.deaths = 0
    localPlayer.bounty = getLocalStartingBounty()
    localPlayer.visualBankRoll = 0f
    localPlayer.barrelRollTimer = 0f
    localPlayer.afterburning = false
    localPlayer.isCloaked = false
    localPlayer.isInStasisBrig = false
    localPlayer.maxFlux = localPlayer.spec.maxFlux * (1f + localPlayer.maxFluxOverchargeStacks * 0.10f)
    localPlayer.flux = localPlayer.maxFlux
    localPlayer.isIdleDraining = false
    localPlayer.playfieldIdleTimer = 0f
    localPlayer.safeZoneStayTimer = 0f
    localPlayer.safeZoneLockoutTimer = 0f
    localPlayer.inSafeZone = false
    localPlayer.wasInSafeZone = false
    localPlayer.weaponsLocked = false
    localPlayer.canFire = true
    localPlayer.weaponState = "ARMED"
    localPlayer.phaseShieldTimer = 1.25f

    // Place a guaranteed visible Supply Pod near the pilot's practice spawn so Step 3 always has a close target
    ensurePracticeTutorialTargetsPresent()
    for (attempt in 0 until 20) {
      val ang = localPlayer.angle + attempt * 0.45f
      val dist = 300f + (attempt % 3) * 60f
      val gx = localPlayer.x + cos(ang) * dist
      val gy = localPlayer.y + sin(ang) * dist
      if (isClearCrateSpawnPosition(gx, gy)) {
        greens.removeAll { hypot(it.x - gx, it.y - gy) < 120f }
        greens.add(0, GreenCrate(x = gx, y = gy, spinPhase = 0f, buffType = 8))
        break
      }
    }
    sfx.playWarp()
  }

  fun closeEphemeralPracticeRift() {
    isHubOverwatchActive = false
    currentOverwatchKey = ""
    isEphemeralPracticeRift = false
    practiceTutorialActive = false
    practiceTutorialStepIndex = 0
    practiceTutorialStepJumpFlashSec = 0f
    practiceSpawnedBotCounter = 0
    hasJoinedMatch = false
    isInLobbyStaging = true
    activeSectorInstanceOverride = null
    isShipLockedForMatch = false
    spectatingTargetShipId = ""
    clearShipToggleBuffs(localPlayer, clearSlotIfActive = true)
    bullets.clear()
    bombs.clear()
    decoys.clear()
    swarmDrones.clear()
    brickWalls.clear()
    shockwaves.clear()
    shipDebrisChunks.clear()
    particles.clear()
    walls.clear()
    greens.clear()
    anomalies.clear()
    interdictorBubbles.clear()
    remotePilots.clear()
    ships.removeAll { !it.isLocalPlayer }
    localPlayer.vx = 0f
    localPlayer.vy = 0f
    localPlayer.kills = 0
    localPlayer.deaths = 0
    localPlayer.bounty = getLocalStartingBounty()
    localPlayer.respawnTimer = 0f
    localPlayer.flux = localPlayer.maxFlux
    localPlayer.afterburning = false
    localPlayer.isIdleDraining = false
    localPlayer.playfieldIdleTimer = 0f
    localPlayer.safeZoneStayTimer = 0f
  }

  fun computeTargetArenaRadius(totalShips: Int): Float =
    (1700f + totalShips.coerceIn(2, 24) * 105f).coerceIn(2100f, 3800f)

  fun updateDynamicArenaGeometryForShipCount(totalShips: Int, regenerateObstacles: Boolean = false) {
    val targetR = computeTargetArenaRadius(totalShips)
    val radiusChanged = kotlin.math.abs(targetR - arenaRadius) > 35f
    arenaRadius = targetR

    val brigOffset = (arenaRadius - 560f).coerceIn(1350f, 2600f)
    westBrigReactorX = arenaCenterX - brigOffset
    eastBrigReactorX = arenaCenterX + brigOffset
    westBrigRect.set(
      westBrigReactorX - stasisJailRadius,
      westBrigReactorY - stasisJailRadius,
      westBrigReactorX + stasisJailRadius,
      westBrigReactorY + stasisJailRadius,
    )
    eastBrigRect.set(
      eastBrigReactorX - stasisJailRadius,
      eastBrigReactorY - stasisJailRadius,
      eastBrigReactorX + stasisJailRadius,
      eastBrigReactorY + stasisJailRadius,
    )

    val riftOffset = (arenaRadius * 0.40f).coerceIn(820f, 1450f)
    riftAlphaX = arenaCenterX - riftOffset
    riftOmegaX = arenaCenterX + riftOffset

    if (regenerateObstacles || radiusChanged || walls.isEmpty()) {
      generateSectorArena(currentArenaSeed)
    }
  }

  fun spawnLocalPlayerCleanly() {
    hasJoinedMatch = true
    isInLobbyStaging = false
    val customHealthMult = currentCustomGame?.healthMult ?: 1.0f
    localPlayer.respawnTimer = 0f
    localPlayer.maxFlux =
      localPlayer.spec.maxFlux * (1f + localPlayer.maxFluxOverchargeStacks * 0.10f) * customHealthMult
    localPlayer.flux = localPlayer.maxFlux
    localPlayer.isIdleDraining = false
    localPlayer.playfieldIdleTimer = 0f
    localPlayer.safeZoneStayTimer = 0f
    localPlayer.safeZoneLockoutTimer = 0f
    localPlayer.vx = 0f
    localPlayer.vy = 0f
    localPlayer.visualBankRoll = 0f
    localPlayer.barrelRollTimer = 0f
    localPlayer.hullImpactJoltTimer = 0f
    localPlayer.deathLingerTimer = 0f
    localPlayer.warpInAnimTimer = 0.65f
    localPlayer.afterburning = false
    clearShipToggleBuffs(localPlayer, clearSlotIfActive = true)
    spectatingTargetShipId = ""
    if (isTeamBasedRankedMode() || !hasActiveSafeZone()) {
      val sp = getSkirmishGateSpawnPosition(localPlayer.freq)
      localPlayer.x = sp.first
      localPlayer.y = sp.second
      localPlayer.angle = if (localPlayer.freq == 1) PI.toFloat() else 0f
      localPlayer.weaponAngle = localPlayer.angle
      localPlayer.inSafeZone = false
      localPlayer.wasInSafeZone = false
      localPlayer.weaponsLocked = false
      localPlayer.canFire = true
      localPlayer.weaponState = "ARMED"
      localPlayer.phaseShieldTimer = RiftCalibrationState.exitPhaseShieldSec
    } else {
      val sp = getSafeZoneSpawnPosition()
      localPlayer.x = sp.first
      localPlayer.y = sp.second
      localPlayer.inSafeZone = true
      localPlayer.wasInSafeZone = true
      localPlayer.weaponsLocked = true
      localPlayer.canFire = false
      localPlayer.weaponState = "DISARMED"
      localPlayer.phaseShieldTimer = RiftCalibrationState.exitPhaseShieldSec
    }
    localPlayer.bounty = getLocalStartingBounty()
    resetCurrentLifeStats()
    if (!isEphemeralPracticeRift) {
      onDailyDirectiveEvent(DailyDirectiveMetric.BOUNTY_PEAK_IN_LIFE, localPlayer.bounty, true)
      notifyLocalLifecycleSync()
    }
  }

  fun getEffectiveProjectileScale(): Float =
    if (currentCustomGame != null) {
      RiftCalibrationState.projectileScale * currentCustomGame!!.projectileSizeMult
    } else {
      1.0f
    }

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

  // Rift Conduit Continuous Per-Ship, Live & Peak Resonance + Daily Directive Hooks
  val shipResonanceById: IntArray = IntArray(11)
  private var combatDamageResonanceCarry: Float = 0f
  var liveResonance: Int = 0
  var peakResonance: Int = 0
  var onDailyDirectiveEvent: (DailyDirectiveMetric, Int, Boolean) -> Unit = { _, _, _ -> }
  var onShipResonanceChanged: (Int, Int, Int) -> Unit = { _, _, _ -> }

  fun getShipResonance(shipId: Int): Int =
    shipResonanceById[shipId.coerceIn(1, 10)].coerceAtLeast(0)

  fun loadShipResonances(resonances: Map<Int, Int>, fallbackLiveResonance: Int = liveResonance) {
    var sum = 0
    for (id in 1..10) {
      val v = (resonances[id] ?: 0).coerceAtLeast(0)
      shipResonanceById[id] = v
      sum += v
    }
    val activeId = localPlayer.shipId.coerceIn(1, 10)
    if (sum == 0 && fallbackLiveResonance > 0) {
      shipResonanceById[activeId] = fallbackLiveResonance
      sum = fallbackLiveResonance
    }
    liveResonance = max(sum, fallbackLiveResonance.coerceAtLeast(0))
    peakResonance = max(peakResonance, liveResonance)
    localPlayer.liveResonanceRating = shipResonanceById[activeId]
  }

  fun getLocalStartingBounty(): Int =
    if (currentCustomGame != null) {
      100
    } else {
      RiftProgressionCatalog.computeStartingBountyForResonance(liveResonance)
    }

  fun adjustLocalResonance(
    delta: Int,
    reasonLabel: String = "",
    showFctEvenIfRespawning: Boolean = false,
  ) {
    if (currentCustomGame != null || isEphemeralPracticeRift || delta == 0) return
    val activeShipId = localPlayer.shipId.coerceIn(1, 10)
    val prevShipRes = shipResonanceById[activeShipId].coerceAtLeast(0)
    val nextShipRes = (prevShipRes + delta).coerceAtLeast(0)
    val shipApplied = nextShipRes - prevShipRes
    shipResonanceById[activeShipId] = nextShipRes
    localPlayer.liveResonanceRating = nextShipRes

    var sumAllShips = 0
    for (id in 1..10) {
      sumAllShips += shipResonanceById[id].coerceAtLeast(0)
    }
    val prevTotal = liveResonance
    liveResonance = max(sumAllShips, (prevTotal + shipApplied).coerceAtLeast(0))
    if (liveResonance > peakResonance) {
      peakResonance = liveResonance
    }
    onShipResonanceChanged(activeShipId, nextShipRes, liveResonance)

    if (shipApplied != 0 && (localPlayer.respawnTimer <= 0f || showFctEvenIfRespawning)) {
      val sign = if (shipApplied > 0) "+" else ""
      val suffix = if (reasonLabel.isNotBlank()) " ($reasonLabel)" else ""
      spawnPooledFct(
        text = "${sign}${shipApplied} RES$suffix",
        x = localPlayer.x,
        y = localPlayer.y - 42f,
        color = if (shipApplied > 0) Color.parseColor("#FFD700") else Color.parseColor("#F59E0B"),
        maxLife = 1.25f,
      )
    }
  }

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
   * Master Fresh-Start Wipe:
   * Resets all combat stats, 5 XP sources, Rift Shards (to 150), Armadas, Syndicates,
   * and live sector state back to a clean Level 1 / GUEST / SOLO state.
   */
  fun performMasterFreshStartWipe(
    localPrefs: com.example.data.LocalPilotPrefs? = null,
    uid: String? = null,
  ) {
    isEphemeralPracticeRift = false
    localPrefs?.wipeAllStatsArmadasAndSyndicates(uid)

    pvpKills = 0L
    pvpDeaths = 0L
    pvpBountyPeak = 100L
    allTimeKills = 0L
    allTimeDeaths = 0L
    allTimeBountyPeak = 100L
    convergencesWon = 0L
    hvtEliminations = 0L
    greensCollected = 0L

    xpFromBuffs = 0f
    xpFromDamage = 0f
    xpFromAccuracy = 0f
    xpFromBountyEarned = 0f
    xpFromBountyKilled = 0f
    pilotLevel = 1
    riftShards = 150L
    highestMilestoneLevelClaimed = 1
    spentCustomizationPoints = 0
    unlockedExtraSlots = 0
    aiShipDesignTokens = 0
    for (id in 1..10) {
      shipResonanceById[id] = 0
    }
    combatDamageResonanceCarry = 0f
    liveResonance = 0
    peakResonance = 0

    activeArmadaName = ""
    activeArmadaSlots.clear()
    localPlayer.armadaName = ""
    localPlayer.syndicateTag = "GUEST"
    localPlayer.bounty = getLocalStartingBounty()
    localPlayer.liveResonanceRating = 0
    localPlayer.kills = 0
    localPlayer.deaths = 0
    localPlayer.flux = localPlayer.maxFlux
    localPlayer.isInStasisBrig = false

    for (s in ships) {
      if (!s.isLocalPlayer && !s.isHuman) {
        s.armadaName = ""
        s.syndicateTag = "BOT"
        s.bounty = 100
        s.isInStasisBrig = false
      }
    }

    RiftCodex.updateActiveCustomShipSlot(com.example.engine.CustomShipLoadoutSlot(slotId = "Frame A"))
    syncLocalPlayerCustomShipSpec()
    resetCurrentLifeStats()
    sfx.playPrize()
    onStatsUpdated()
    onPrizeToast("MASTER WIPE COMPLETE: ALL STATS, ARMADAS & SYNDICATES RESET!")
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
      text = "FORGED: ${entry.shortLabel} [${localPlayer.specialWeaponCharges}]",
      x = localPlayer.x,
      y = localPlayer.y - 32f,
      color = entry.strokeColorInt,
      maxLife = 1.35f,
    )
    onPrizeToast("BUFF FORGED & EQUIPPED: ${entry.officialName} (${localPlayer.specialWeaponCharges} CHARGES)")
  }

  fun getTotalXp(): Long =
    xpFromBuffs.toLong() +
      xpFromDamage.toLong() +
      xpFromAccuracy.toLong() +
      xpFromBountyEarned.toLong() +
      xpFromBountyKilled.toLong()

  fun isNinthShipUnlocked(): Boolean = true

  fun getCustomizationPointsTotal(): Int = 100

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
    if (isEphemeralPracticeRift) return
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
          text = "LVL $pilotLevel! +$milestoneBonusShards SHARDS",
          x = localPlayer.x,
          y = localPlayer.y - 38f,
          color = Color.parseColor("#FFD700"),
          maxLife = 1.45f,
        )
        onPrizeToast("MILESTONE PILOT LVL $pilotLevel: +1 CUSTOM PT & +$milestoneBonusShards RIFT SHARDS!")
      } else {
        sfx.playPrize()
        spawnPooledFct(
          text = "PILOT LVL $pilotLevel (+1 PT)",
          x = localPlayer.x,
          y = localPlayer.y - 36f,
          color = Color.parseColor("#00F0FF"),
          maxLife = 1.25f,
        )
        onPrizeToast("PILOT LEVEL UP! LVL $pilotLevel (+1 CUSTOMIZATION POINT • ${pilotLevel}/100 PTS)")
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
      onPrizeToast("MAX PILOT LEVEL 100 REACHED (100/100 CUSTOMIZATION POINTS)")
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
    if (!hasJoinedMatch) {
      val newSeed = Random.nextInt(10_000, 999_999)
      bullets.clear()
      bombs.clear()
      decoys.clear()
      swarmDrones.clear()
      brickWalls.clear()
      shockwaves.clear()
      shipDebrisChunks.clear()
      shieldRipples.clear()
      phaseAfterimages.clear()
      interdictorBubbles.clear()
      generateSectorArena(seed = newSeed)
      publicMatchDurationSec = 0f
      for (s in ships) {
        s.matchPresenceTimeSec = 0f
        s.bounty = 100
        s.maxFlux = s.spec.maxFlux * (currentCustomGame?.healthMult ?: 1.0f)
        s.flux = s.maxFlux
        if (s.isLocalPlayer) {
          s.respawnTimer = 999f
          s.x = arenaCenterX
          s.y = arenaCenterY
          s.vx = 0f
          s.vy = 0f
        } else {
          s.respawnTimer = 0f
          val sp = findRandomOpenPositionOutsideSafeZone()
          s.x = sp.first
          s.y = sp.second
          s.vx = 0f
          s.vy = 0f
        }
      }
      return
    }
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

    onDailyDirectiveEvent(DailyDirectiveMetric.MATCHES_PLAYED, 1, false)

    if (topShip != null && (topShip.isLocalPlayer || localPlayer.bounty >= topShip.bounty)) {
      if (presenceRatio > 0.50f) {
        convergencesWon++
        riftShards += 100L
        adjustLocalResonance(30, "CONVERGENCE WIN")
        onSyndicateContribution(0L, localPlayer.bounty.toLong(), 1L)
        sfx.playPrize()
        val bannerMsg =
          "SECTOR CONVERGENCE VICTORY! #1 RESONANCE (${localPlayer.liveResonanceRating} RES • $presencePct% PRESENCE) — +100 RIFT SHARDS & NEW FFA MAP!"
        highBountyShutdownBanner = bannerMsg
        highBountyShutdownTimer = 5.5f
        onPrizeToast(bannerMsg)
      } else {
        val bannerMsg =
          "SECTOR CONVERGENCE: #1 RESONANCE ACHIEVED, BUT PRESENCE ($presencePct%) <= 50% — NEW FFA MAP GENERATED!"
        highBountyShutdownBanner = bannerMsg
        highBountyShutdownTimer = 4.5f
        onPrizeToast(bannerMsg)
      }
    } else {
      val winnerName = topShip?.callsign ?: "SECTOR ACE"
      val winnerRes = topShip?.liveResonanceRating?.takeIf { it > 0 } ?: (topShip?.bounty ?: 100)
      val bannerMsg =
        "SECTOR CONVERGENCE: WINNER $winnerName ($winnerRes RES) — PROCEDURAL NEW FFA MAP ONLINE!"
      highBountyShutdownBanner = bannerMsg
      highBountyShutdownTimer = 4.5f
      onPrizeToast(bannerMsg)
    }

    // Procedurally regenerate a brand-new arena map for the next 60-minute FFA match
    val newSeed = Random.nextInt(10_000, 999_999)
    bullets.clear()
    bombs.clear()
    decoys.clear()
    swarmDrones.clear()
    brickWalls.clear()
    shockwaves.clear()
    shipDebrisChunks.clear()
    shieldRipples.clear()
    phaseAfterimages.clear()
    interdictorBubbles.clear()
    generateSectorArena(seed = newSeed)

    publicMatchDurationSec = 0f
    for (s in ships) {
      s.matchPresenceTimeSec = 0f
      s.bounty = if (s.isLocalPlayer) getLocalStartingBounty() else 100
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
      s.turnOverchargeStacks = 0
      s.maxFluxOverchargeStacks = 0
      s.maxFlux = s.spec.maxFlux * (currentCustomGame?.healthMult ?: 1.0f)
      s.flux = s.maxFlux
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
      } else if (!s.isHuman) {
        val sp = getOuterGateSpawnPosition(s.freq)
        s.x = sp.first
        s.y = sp.second
        s.inSafeZone = false
        s.wasInSafeZone = false
      }
    }
    reattachRemotePilotsToRoster()
    resetCurrentLifeStats()
    onDailyDirectiveEvent(DailyDirectiveMetric.BOUNTY_PEAK_IN_LIFE, localPlayer.bounty, true)
    sfx.playWarp()
    spawnPooledFct(
      text = "NEW SECTOR MAP CONVERGED",
      x = localPlayer.x,
      y = localPlayer.y - 38f,
      color = Color.parseColor("#00F0FF"),
      maxLife = 1.6f,
    )
    notifyLocalLifecycleSync()
    onStatsUpdated()
  }

  var localPlayer: ShipEntity

  init {
    currentArenaSeed = (System.currentTimeMillis() / 3600_000L).toInt()
    updateDynamicArenaGeometryForShipCount(20, regenerateObstacles = true)
    val initialSpec = RiftCodex.getShipById(1)
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
        x = arenaCenterX + 760f,
        y = arenaCenterY,
        angle = PI.toFloat() * 0.5f,
        wasInSafeZone = false,
        inSafeZone = false,
        weaponsLocked = true,
        canFire = false,
        weaponState = "DISARMED",
        phaseShieldTimer = 3.0f,
      )
    syncLocalArmadaSelfSlot()
    activeCustomGames.add(
      CustomGameConfig(
        instanceId = "custom_fat_bullets_ffa",
        title = "MegaFat Bullets 5m FFA",
        hostCallsign = "RIFT HOST",
        baseMode = RankedGameMode.SECTOR_CONVERGENCE,
        teamSizeBracket = 20,
        matchDurationMinutes = 5,
        allowArmadasInFfa = false,
        botsEnabled = true,
        bulletSizeMult = 2.2f,
        damageMult = 1.15f,
        speedMult = 1.0f,
        healthMult = 1.0f,
        rechargeMult = 1.15f,
        crateSpawnMult = 1.25f,
      )
    )
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
    val cleanName = rawName.trim().uppercase().replace(Regex("[^A-Z0-9_\\-]"), "").take(14)
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
    onPrizeToast("STRIKE WING [$activeArmadaName] ACTIVE (${getCurrentArmadaSize()}/5 PILOTS)")
    onStatsUpdated()
  }

  fun addMemberToArmada(friend: FriendPilotEntry): Boolean {
    if (activeArmadaName.isEmpty()) {
      activeArmadaName = "${localPlayer.syndicateTag}_WING".take(14)
      syncLocalArmadaSelfSlot()
    }
    if (activeArmadaSlots.size >= 5) {
      onPrizeToast("STRIKE WING [$activeArmadaName] IS AT MAX 5-PILOT CAPACITY")
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
      "ARMADA ROSTER SHIFT: [$activeArmadaName] ${prevSize}-MAN -> ${newSize}-MAN LEADERBOARD TIER"
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
      onPrizeToast("ARMADA [$oldName] DISBANDED -> SHIFTED TO [SOLO] LEADERBOARD TIER")
    } else {
      val newSize = getCurrentArmadaSize()
      applyArmadaRosterToSectorShips()
      onPrizeToast(
        "ARMADA ROSTER SHIFT: [$activeArmadaName] ${prevSize}-MAN -> ${newSize}-MAN LEADERBOARD TIER"
      )
    }
    onStatsUpdated()
  }

  fun simulateArmadaDropoutToDuo() {
    if (activeArmadaName.isEmpty() || activeArmadaSlots.size <= 2) {
      onPrizeToast("Invite 2+ real pilots to your Armada first to test squad size reduction.")
      return
    }
    val before = activeArmadaSlots.size
    while (activeArmadaSlots.size > 2) {
      activeArmadaSlots.removeAt(activeArmadaSlots.lastIndex)
    }
    applyArmadaRosterToSectorShips()
    onPrizeToast(
      "ARMADA [$activeArmadaName] TRIMMED ${before}-MAN -> 2-MAN DUO TIER"
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
      onPrizeToast("LEFT ARMADA [$old] -> FLYING [SOLO]")
    }
    onStatsUpdated()
  }

  private fun applyArmadaRosterToSectorShips() {
    localPlayer.armadaName = activeArmadaName
    val wingCallsigns = activeArmadaSlots.filter { !it.isLeader }.map { it.callsign }.toSet()
    for (s in ships) {
      if (s.isLocalPlayer) continue
      if (!s.isHuman) {
        s.syndicateTag = "BOT"
        s.armadaName = ""
        continue
      }
      if (s.callsign in wingCallsigns) {
        s.armadaName = activeArmadaName
      } else if (s.armadaName == activeArmadaName) {
        s.armadaName = ""
      }
    }
  }

  fun effectiveModeRules(): RankedGameMode = currentCustomGame?.baseMode ?: activeRankedMode

  fun isTeamBasedRankedMode(): Boolean =
    effectiveModeRules() in
      setOf(
        RankedGameMode.RIFT_SKIRMISH,
        RankedGameMode.STASIS_BREACH,
        RankedGameMode.RIFT_CONTROL,
        RankedGameMode.SYNDICATE_WARS,
      )

  fun isStasisBreachRulesActive(): Boolean =
    effectiveModeRules() == RankedGameMode.STASIS_BREACH ||
      (effectiveModeRules() == RankedGameMode.SYNDICATE_WARS && activeTeamSizeBracket >= 10)

  fun isSkirmishRulesActive(): Boolean =
    effectiveModeRules() == RankedGameMode.RIFT_SKIRMISH ||
      (effectiveModeRules() == RankedGameMode.SYNDICATE_WARS && activeTeamSizeBracket < 10)

  fun isFluxCoreRulesActive(): Boolean =
    effectiveModeRules() == RankedGameMode.FLUX_CORE

  fun isRiftControlRulesActive(): Boolean =
    effectiveModeRules() == RankedGameMode.RIFT_CONTROL

  fun hasActiveSafeZone(): Boolean {
    val m = effectiveModeRules()
    return m == RankedGameMode.SECTOR_CONVERGENCE || m == RankedGameMode.FLUX_CORE
  }

  fun doesModeLockShipSelection(targetMode: RankedGameMode = effectiveModeRules()): Boolean =
    targetMode == RankedGameMode.RIFT_SKIRMISH || targetMode == RankedGameMode.SYNDICATE_WARS

  fun getLivingSpectatableTeammates(): List<ShipEntity> =
    if (!isSkirmishRulesActive() || skirmishReadyCountdownSec > 0f) {
      emptyList()
    } else {
      ships.filter {
        !it.isLocalPlayer &&
          it.respawnTimer <= 0f &&
          !it.isInStasisBrig &&
          areAllied(localPlayer, it)
      }
    }

  fun isActivelySpectatingTeammates(): Boolean =
    hasJoinedMatch &&
      !isInLobbyStaging &&
      isSkirmishRulesActive() &&
      skirmishReadyCountdownSec <= 0f &&
      localPlayer.respawnTimer > 0f &&
      localPlayer.deathLingerTimer <= 0f &&
      getLivingSpectatableTeammates().isNotEmpty()

  fun selectSpectatorTarget(shipId: String): Boolean {
    val living = getLivingSpectatableTeammates()
    val match = living.firstOrNull { it.id == shipId } ?: return false
    spectatingTargetShipId = match.id
    sfx.playMutedClick()
    return true
  }

  fun cycleSpectatorTarget(delta: Int): ShipEntity? {
    val living = getLivingSpectatableTeammates()
    if (living.isEmpty()) {
      spectatingTargetShipId = ""
      return null
    }
    val curIdx = living.indexOfFirst { it.id == spectatingTargetShipId }
    val nextIdx =
      if (curIdx < 0) {
        0
      } else {
        ((curIdx + delta) % living.size + living.size) % living.size
      }
    val chosen = living[nextIdx]
    spectatingTargetShipId = chosen.id
    sfx.playMutedClick()
    return chosen
  }

  fun enforceBotsEnabledState() {
    if (!hasJoinedMatch && !isHubOverwatchActive) return
    if (isEphemeralPracticeRift) {
      ships.removeAll { !it.isLocalPlayer }
      return
    }
    val botsAllowed = currentCustomGame?.botsEnabled ?: RiftCalibrationState.botsEnabled
    if (!botsAllowed) {
      ships.removeAll { !it.isHuman && !it.isLocalPlayer }
      updateDynamicArenaGeometryForShipCount(ships.size.coerceAtLeast(2), regenerateObstacles = false)
    } else if (ships.none { !it.isHuman && !it.isLocalPlayer }) {
      rebuildRosterForMode(mode)
    }
  }

  fun createAndJoinCustomGame(config: CustomGameConfig) {
    isHubOverwatchActive = false
    currentOverwatchKey = ""
    isEphemeralPracticeRift = false
    hasJoinedMatch = true
    isInLobbyStaging = false
    activeCustomGames.removeAll { it.instanceId == config.instanceId }
    activeCustomGames.add(0, config)
    joinCustomGame(config.instanceId)
  }

  fun joinCustomGame(instanceId: String) {
    val cfg = activeCustomGames.firstOrNull { it.instanceId == instanceId } ?: return
    isHubOverwatchActive = false
    currentOverwatchKey = ""
    isEphemeralPracticeRift = false
    hasJoinedMatch = true
    isInLobbyStaging = false
    currentCustomGame = cfg
    activeSectorInstanceOverride = null
    activeRankedMode = cfg.baseMode
    mode = cfg.baseMode.id
    activeTeamSizeBracket = cfg.teamSizeBracket
    publicMatchDurationSec = 0f
    publicMatchCycleLimitSec = (cfg.matchDurationMinutes.coerceIn(1, 120) * 60f)
    skirmishRound = 1
    skirmishWestRoundWins = 0
    skirmishEastRoundWins = 0
    skirmishWestKills = 0
    skirmishEastKills = 0
    skirmishRoundTimerSec = 180f
    stasisBreachMatchTimerSec = 180f
    stasisWestCaptures = 0
    stasisEastCaptures = 0
    localStasisCaptures = 0
    resetDeathmatchEscalationAndReadyState()
    spectatingTargetShipId = ""
    resetFluxCoreToCenter()
    westBrigGeneratorHp = getEffectiveBrigReactorMaxHp()
    eastBrigGeneratorHp = getEffectiveBrigReactorMaxHp()
    resetRiftControlZones()
    bullets.clear()
    bombs.clear()
    decoys.clear()
    swarmDrones.clear()
    brickWalls.clear()
    shipDebrisChunks.clear()
    interdictorBubbles.clear()
    rebuildRosterForMode(cfg.baseMode.id)
    for (s in ships) {
      s.maxFlux = s.spec.maxFlux * cfg.healthMult
      s.flux = s.maxFlux
    }
    spawnLocalPlayerCleanly()
    isShipLockedForMatch = doesModeLockShipSelection()
    sfx.playWarp()
    onPrizeToast("JOINED CUSTOM GAME: ${cfg.title.uppercase()} (${cfg.baseMode.title})")
    onStatsUpdated()
  }

  private fun resetRiftControlZones() {
    riftAlphaControlSec = 0f
    riftOmegaControlSec = 0f
    riftAlphaLockedBy = -1
    riftOmegaLockedBy = -1
    riftAlphaWestCount = 0
    riftAlphaEastCount = 0
    riftOmegaWestCount = 0
    riftOmegaEastCount = 0
    riftControlWestScore = 0f
    riftControlEastScore = 0f
    riftControlMatchTimerSec = 180f
  }

  fun activeMatchSectorTag(): String =
    if (!hasJoinedMatch || isEphemeralPracticeRift) {
      "NEXUS_LOBBY"
    } else {
      currentCustomGame?.let { "CUSTOM_${it.instanceId}" }
        ?: activeSectorInstanceOverride?.takeIf { it.isNotBlank() }
        ?: activeRankedMode.id
    }

  private fun matchesRankedModeSector(rawSectorId: String, targetMode: RankedGameMode): Boolean {
    val clean = rawSectorId.trim().uppercase()
    if (clean.isEmpty() || clean == "NEXUS_LOBBY" || clean.startsWith("CUSTOM_")) return false
    val mapped =
      when (clean) {
        "SECTOR_CONVERGENCE", "SECTOR_CONVERGENCE_2", "FFA", "CONVERGENCE" -> RankedGameMode.SECTOR_CONVERGENCE
        "RIFT_SKIRMISH", "SKIRMISH", "TDM", "3V3", "5V5" -> RankedGameMode.RIFT_SKIRMISH
        "FLUX_CORE", "CORE", "JUGGERNAUT" -> RankedGameMode.FLUX_CORE
        "STASIS_BREACH", "BREACH", "JAILBREAK", "10V10" -> RankedGameMode.STASIS_BREACH
        "RIFT_CONTROL", "CONTROL", "TUG", "KOTH" -> RankedGameMode.RIFT_CONTROL
        "SYNDICATE_WARS", "WARS", "CLAN_WARS" -> RankedGameMode.SYNDICATE_WARS
        else -> null
      }
    return mapped == targetMode
  }

  fun matchesCurrentInstanceSector(rawSectorId: String): Boolean {
    val clean = rawSectorId.trim().uppercase()
    if (clean.isEmpty() || clean == "NEXUS_LOBBY") return false
    val myCustom = currentCustomGame
    if (myCustom != null) {
      return clean.equals("CUSTOM_${myCustom.instanceId}", ignoreCase = true)
    }
    val overrideKey =
      when {
        isHubOverwatchActive && !hasJoinedMatch && currentOverwatchKey.equals("SECTOR_CONVERGENCE_2", ignoreCase = true) ->
          "SECTOR_CONVERGENCE_2"
        activeSectorInstanceOverride.equals("SECTOR_CONVERGENCE_2", ignoreCase = true) ->
          "SECTOR_CONVERGENCE_2"
        else -> null
      }
    if (overrideKey != null) {
      return clean == "SECTOR_CONVERGENCE_2"
    }
    if (activeRankedMode == RankedGameMode.SECTOR_CONVERGENCE && clean == "SECTOR_CONVERGENCE_2") {
      return false
    }
    return matchesRankedModeSector(clean, activeRankedMode)
  }

  /**
   * Returns the exact count of real HUMAN pilots currently active in the specified Ranked Mode (0 when empty; NEVER counts AI bots).
   */
  fun getHumanPilotCountForMode(mode: RankedGameMode = activeRankedMode): Int {
    val localInMode =
      if (hasJoinedMatch && !isEphemeralPracticeRift && currentCustomGame == null && activeRankedMode == mode) 1
      else 0
    val remoteInMode = onlineRemoteSectorByUid.values.count { matchesRankedModeSector(it, mode) }
    return localInMode + remoteInMode
  }

  /**
   * Returns the exact count of real HUMAN pilots in a specific canonical Overwatch / Rift instance key.
   */
  fun getHumanPilotCountForSectorKey(sectorKey: String, fallbackMode: RankedGameMode): Int {
    val keyUpper = sectorKey.trim().uppercase()
    if (keyUpper.startsWith("CUSTOM_")) {
      return getHumanPilotCountForCustomGame(keyUpper.removePrefix("CUSTOM_"))
    }
    if (keyUpper == "SECTOR_CONVERGENCE_2") {
      val localHere =
        if (hasJoinedMatch && !isEphemeralPracticeRift && activeMatchSectorTag().equals("SECTOR_CONVERGENCE_2", ignoreCase = true)) 1
        else 0
      val remoteHere = onlineRemoteSectorByUid.values.count { it.equals("SECTOR_CONVERGENCE_2", ignoreCase = true) }
      return localHere + remoteHere
    }
    if (fallbackMode == RankedGameMode.SECTOR_CONVERGENCE) {
      val localHere =
        if (
          hasJoinedMatch &&
            !isEphemeralPracticeRift &&
            currentCustomGame == null &&
            activeRankedMode == RankedGameMode.SECTOR_CONVERGENCE &&
            !activeMatchSectorTag().equals("SECTOR_CONVERGENCE_2", ignoreCase = true)
        ) {
          1
        } else {
          0
        }
      val remoteHere =
        onlineRemoteSectorByUid.values.count {
          !it.equals("SECTOR_CONVERGENCE_2", ignoreCase = true) &&
            matchesRankedModeSector(it, RankedGameMode.SECTOR_CONVERGENCE)
        }
      return localHere + remoteHere
    }
    return getHumanPilotCountForMode(fallbackMode)
  }

  /**
   * Returns the exact count of real HUMAN pilots currently active in a specific Custom Game instance (0 when empty).
   */
  fun getHumanPilotCountForCustomGame(instanceId: String): Int {
    val targetSector = "CUSTOM_${instanceId.trim()}"
    val localInCustom =
      if (hasJoinedMatch && !isEphemeralPracticeRift && currentCustomGame?.instanceId == instanceId) 1 else 0
    val remoteInCustom = onlineRemoteSectorByUid.values.count { it.equals(targetSector, ignoreCase = true) }
    return localInCustom + remoteInCustom
  }

  /**
   * Returns the total number of real HUMAN pilots online across the app (local pilot + live remote heartbeats).
   */
  fun getTotalHumanPilotsOnline(): Int = 1 + onlineRemoteSectorByUid.size

  /**
   * Cleanly leaves the active combat instance and returns the pilot to peaceful unjoined NEXUS orbit.
   */
  fun leaveActiveMatchToNexusOrbit() {
    isHubOverwatchActive = false
    currentOverwatchKey = ""
    isEphemeralPracticeRift = false
    hasJoinedMatch = false
    isInLobbyStaging = true
    isShipLockedForMatch = false
    currentCustomGame = null
    activeSectorInstanceOverride = null
    resetDeathmatchEscalationAndReadyState()
    spectatingTargetShipId = ""
    ships.clear()
    remotePilots.clear()
    bullets.clear()
    bombs.clear()
    decoys.clear()
    swarmDrones.clear()
    brickWalls.clear()
    interdictorBubbles.clear()
    killFeed.clear()
    localPlayer.vx = 0f
    localPlayer.vy = 0f
    localPlayer.respawnTimer = 0f
    localPlayer.flux = localPlayer.maxFlux
    localPlayer.isCloaked = false
    localPlayer.afterburning = false
    clearShipToggleBuffs(localPlayer, clearSlotIfActive = true)
    localPlayer.visualBankRoll = 0f
    localPlayer.barrelRollTimer = 0f
    updateDynamicArenaGeometryForShipCount(20, regenerateObstacles = true)
    sfx.playMutedClick()
    onPrizeToast("RETURNED TO RIFT STATION ORBIT")
    localLifecycleSyncSeq++
    onLocalLifecycleSyncNeeded()
    onStatsUpdated()
  }

  /**
   * Seamless Bot-Replacement Scaling: when a human pilot joins a 20-ship instance,
   * replaces 1 AI bot (!it.isHuman) so total instance ships never exceed capacity.
   */
  fun replaceBotWithHuman(pilotShip: ShipEntity) {
    val existingIdx = ships.indexOfFirst { it.id == pilotShip.id }
    if (existingIdx >= 0) {
      ships[existingIdx] = pilotShip
      return
    }
    val cap =
      when (activeRankedMode) {
        RankedGameMode.RIFT_SKIRMISH -> (activeTeamSizeBracket.coerceIn(2, 5) * 2)
        RankedGameMode.RIFT_CONTROL, RankedGameMode.SYNDICATE_WARS -> (activeTeamSizeBracket.coerceIn(3, 10) * 2)
        else -> 20
      }
    if (ships.size >= cap) {
      val botToRemove =
        ships.firstOrNull { !it.isHuman && !it.isLocalPlayer && it.freq == pilotShip.freq }
          ?: ships.firstOrNull { !it.isHuman && !it.isLocalPlayer }
      if (botToRemove != null) {
        ships.remove(botToRemove)
      }
    }
    ships.add(pilotShip)
  }

  /**
   * Validates whether the local pilot and their active Armada wingmen are eligible for Ranked Syndicate Wars.
   */
  fun canQueueSyndicateWars(): Pair<Boolean, String> {
    val myTag = localPlayer.syndicateTag.trim().uppercase()
    if (
      myTag.isEmpty() ||
        myTag in com.example.data.LocalPilotPrefs.NON_PLAYER_SYNDICATE_TAGS ||
        myTag in LEGACY_FOUNDRY_TAGS
    ) {
      return false to "Join or found a Syndicate first to queue for Ranked Syndicate Wars."
    }
    val mismatched =
      activeArmadaSlots.firstOrNull {
        !it.isLeader && !it.syndicateTag.trim().equals(myTag, ignoreCase = true)
      }
    if (mismatched != null) {
      return false to "All Armada wingmen must belong to Syndicate [$myTag] (${mismatched.callsign} is [${mismatched.syndicateTag}])."
    }
    return true to "ELIGIBLE FOR [$myTag] SYNDICATE WARS"
  }

  /**
   * Deploys the local pilot (and their active Armada squad on Team 0) into any of the 6 always-open Ranked Modes.
   */
  fun deployToRankedMode(
    mode: RankedGameMode,
    teamSizeBracket: Int = 3,
    sectorInstanceOverride: String? = null,
  ) {
    if (mode == RankedGameMode.SYNDICATE_WARS) {
      val (eligible, reason) = canQueueSyndicateWars()
      if (!eligible) {
        sfx.playMutedClick()
        onPrizeToast(reason)
        return
      }
    }
    isHubOverwatchActive = false
    currentOverwatchKey = ""
    isEphemeralPracticeRift = false
    hasJoinedMatch = true
    isInLobbyStaging = false
    currentCustomGame = null
    activeSectorInstanceOverride = sectorInstanceOverride?.takeIf { it.isNotBlank() }
    publicMatchCycleLimitSec = RiftCalibrationState.publicMatchCycleLimitSec
    activeRankedMode = mode
    this.mode = mode.id
    activeTeamSizeBracket =
      when (mode) {
        RankedGameMode.RIFT_SKIRMISH -> teamSizeBracket.coerceIn(2, 5)
        RankedGameMode.STASIS_BREACH -> 10
        RankedGameMode.RIFT_CONTROL, RankedGameMode.SYNDICATE_WARS ->
          when {
            teamSizeBracket >= 10 -> 10
            teamSizeBracket >= 5 -> 5
            else -> 3
          }
        else -> 20
      }

    // Reset mode-specific state
    skirmishRound = 1
    skirmishWestRoundWins = 0
    skirmishEastRoundWins = 0
    skirmishWestKills = 0
    skirmishEastKills = 0
    skirmishRoundTimerSec = 180f
    stasisBreachMatchTimerSec = 180f
    stasisWestCaptures = 0
    stasisEastCaptures = 0
    localStasisCaptures = 0
    resetDeathmatchEscalationAndReadyState()
    spectatingTargetShipId = ""
    rivalSyndicateWarTag = "BOT"

    resetFluxCoreToCenter()
    westBrigGeneratorHp = getEffectiveBrigReactorMaxHp()
    eastBrigGeneratorHp = getEffectiveBrigReactorMaxHp()
    resetRiftControlZones()

    bullets.clear()
    bombs.clear()
    decoys.clear()
    swarmDrones.clear()
    brickWalls.clear()
    shipDebrisChunks.clear()
    interdictorBubbles.clear()

    rebuildRosterForMode(mode.id)
    spawnLocalPlayerCleanly()
    isShipLockedForMatch = doesModeLockShipSelection()
    sfx.playWarp()
    val humanCount = getHumanPilotCountForMode(mode)
    val botCount = (ships.size - humanCount).coerceAtLeast(0)
    onPrizeToast(
      "DEPLOYED TO ${mode.title.uppercase()} (${mode.teamFormatBadge}) • $humanCount HUMAN / $botCount AI BOTS"
    )
    onStatsUpdated()
  }

  private fun resetFluxCoreToCenter() {
    fluxCoreCarrierEntityId = ""
    fluxCoreBountyTickAccum = 0f
    fluxCoreOrb.carrierShipId = -1
    fluxCoreOrb.holdDurationSec = 0f
    fluxCoreOrb.overchargeTier = 0
    fluxCoreOrb.ejectDecelTimerSec = 0f
    fluxCoreOrb.pickupImmunitySec = 0.5f
    // Place just outside the North Safe Zone rim so pilots can immediately contest it in open space
    val safeR = getSafeZoneRadius()
    fluxCoreOrb.x = arenaCenterX
    fluxCoreOrb.y = arenaCenterY - safeR - 320f
    fluxCoreOrb.vx = 0f
    fluxCoreOrb.vy = 0f
  }

  private fun ejectFluxCoreFromShip(ship: ShipEntity, reason: String) {
    if (activeRankedMode != RankedGameMode.FLUX_CORE) return
    val isCarrier =
      fluxCoreCarrierEntityId == ship.id ||
        (fluxCoreOrb.carrierShipId != -1 && fluxCoreOrb.carrierShipId == ship.shipId && hypot(ship.x - fluxCoreOrb.x, ship.y - fluxCoreOrb.y) < 180f)
    if (!isCarrier) return

    fluxCoreCarrierEntityId = ""
    fluxCoreOrb.carrierShipId = -1
    fluxCoreOrb.holdDurationSec = 0f
    fluxCoreOrb.overchargeTier = 0
    fluxCoreOrb.ejectDecelTimerSec = 30.0f
    fluxCoreOrb.pickupImmunitySec = 0.75f
    fluxCoreBountyTickAccum = 0f

    // Launch outward at 1,650 px/s away from Safe Zone center
    val cx = arenaCenterX
    val cy = arenaCenterY
    val baseAng =
      if (hypot(ship.x - cx, ship.y - cy) > 40f) {
        atan2(ship.y - cy, ship.x - cx) + (Random.nextFloat() - 0.5f) * 0.65f
      } else {
        Random.nextFloat() * PI.toFloat() * 2f
      }
    val ejectSp = RiftCalibrationState.fluxCoreEjectSpeed
    fluxCoreOrb.x = ship.x
    fluxCoreOrb.y = ship.y
    val distFromCenter = hypot(fluxCoreOrb.x - cx, fluxCoreOrb.y - cy)
    val maxOrbDist = (arenaRadius - 150f).coerceAtLeast(600f)
    if (distFromCenter > maxOrbDist) {
      fluxCoreOrb.x = cx + ((fluxCoreOrb.x - cx) / distFromCenter) * maxOrbDist
      fluxCoreOrb.y = cy + ((fluxCoreOrb.y - cy) / distFromCenter) * maxOrbDist
    }
    if (isInsideSafeZone(fluxCoreOrb.x, fluxCoreOrb.y)) {
      val pushR = getSafeZoneRadius() + 90f
      fluxCoreOrb.x = cx + cos(baseAng) * pushR
      fluxCoreOrb.y = cy + sin(baseAng) * pushR
    }
    fluxCoreOrb.vx = cos(baseAng) * ejectSp
    fluxCoreOrb.vy = sin(baseAng) * ejectSp

    shockwaves.add(ShockwaveEntity(fluxCoreOrb.x, fluxCoreOrb.y, 20f, 320f, Color.parseColor("#FACC15")))
    shockwaves.add(ShockwaveEntity(fluxCoreOrb.x, fluxCoreOrb.y, 12f, 240f, Color.parseColor("#00F5D4")))
    spawnPooledFct(
      text = "FLUX CORE EJECTED ($reason)",
      x = fluxCoreOrb.x,
      y = fluxCoreOrb.y - 32f,
      color = Color.parseColor("#FACC15"),
      maxLife = 1.4f,
    )
    if (ship.isLocalPlayer) {
      sfx.playWarp()
      onPrizeToast("VOLATILE FLUX CORE EJECTED ($reason) — RICOCHETING AT ${ejectSp.toInt()} PX/S!")
    }
  }

  /**
   * Friendly Fire Rule:
   * Strictly disabled between active Armada mates, same-Syndicate mates, and same-Team mates in Team Modes!
   */
  private fun isRealPlayerSyndicateAllianceTag(rawTag: String): Boolean {
    val t = rawTag.trim().uppercase()
    return t.length in 1..3 &&
      t !in com.example.data.LocalPilotPrefs.NON_PLAYER_SYNDICATE_TAGS &&
      t !in LEGACY_FOUNDRY_TAGS
  }

  fun areAllied(a: ShipEntity, b: ShipEntity): Boolean {
    if (a.id == b.id) return true
    if (isTeamBasedRankedMode()) return a.freq == b.freq
    val allowFfaArmadas = currentCustomGame?.allowArmadasInFfa ?: true
    if (!allowFfaArmadas) return false
    if (a.armadaName.isNotEmpty() && a.armadaName == b.armadaName) return true
    if (
      isRealPlayerSyndicateAllianceTag(a.syndicateTag) &&
        a.syndicateTag.trim().equals(b.syndicateTag.trim(), ignoreCase = true)
    ) {
      return true
    }
    return false
  }

  private fun isStationDefenseOwner(ownerId: String): Boolean =
    ownerId == "station_defense" || ownerId == "station_retaliation" || ownerId == "station_cascade"

  fun isBulletAlliedWithShip(b: BulletEntity, s: ShipEntity): Boolean {
    if (isStationDefenseOwner(b.ownerId)) return false
    if (b.ownerId == s.id) return true
    if (isTeamBasedRankedMode()) return b.freq == s.freq
    val allowFfaArmadas = currentCustomGame?.allowArmadasInFfa ?: true
    if (!allowFfaArmadas) return false
    if (b.ownerArmadaName.isNotEmpty() && b.ownerArmadaName == s.armadaName) return true
    if (
      isRealPlayerSyndicateAllianceTag(b.ownerSyndicateTag) &&
        b.ownerSyndicateTag.trim().equals(s.syndicateTag.trim(), ignoreCase = true)
    ) {
      return true
    }
    return false
  }

  fun isBombAlliedWithShip(bm: BombEntity, s: ShipEntity): Boolean {
    if (bm.ownerId == s.id) return true
    if (isTeamBasedRankedMode()) return bm.freq == s.freq
    val allowFfaArmadas = currentCustomGame?.allowArmadasInFfa ?: true
    if (!allowFfaArmadas) return false
    if (bm.ownerArmadaName.isNotEmpty() && bm.ownerArmadaName == s.armadaName) return true
    if (
      isRealPlayerSyndicateAllianceTag(bm.ownerSyndicateTag) &&
        bm.ownerSyndicateTag.trim().equals(s.syndicateTag.trim(), ignoreCase = true)
    ) {
      return true
    }
    return false
  }

  fun isDroneAlliedWithShip(dr: SwarmDroneEntity, s: ShipEntity): Boolean {
    if (dr.ownerId == s.id) return true
    if (isTeamBasedRankedMode()) return dr.freq == s.freq
    val allowFfaArmadas = currentCustomGame?.allowArmadasInFfa ?: true
    if (!allowFfaArmadas) return false
    if (dr.ownerArmadaName.isNotEmpty() && dr.ownerArmadaName == s.armadaName) return true
    if (
      isRealPlayerSyndicateAllianceTag(dr.ownerSyndicateTag) &&
        dr.ownerSyndicateTag.trim().equals(s.syndicateTag.trim(), ignoreCase = true)
    ) {
      return true
    }
    return false
  }

  private fun rectOverlapsProtectedZone(rx: Float, ry: Float, rw: Float, rh: Float): Boolean {
    val rRight = rx + rw
    val rBottom = ry + rh
    val rCx = rx + rw * 0.5f
    val rCy = ry + rh * 0.5f
    val halfDiag = hypot(rw * 0.5f, rh * 0.5f)
    // Must fit comfortably inside circular sector containment ring
    if (hypot(rCx - arenaCenterX, rCy - arenaCenterY) + halfDiag > arenaRadius - 190f) return true
    // Protect circular Safe Zone + docking clearance when active
    if (
      hasActiveSafeZone() &&
        hypot(rCx - arenaCenterX, rCy - arenaCenterY) < getSafeZoneRadius() + halfDiag + 180f
    ) {
      return true
    }
    // Protect West/East Warp Gates & Circular Stasis Jails
    val westGateX = arenaCenterX - arenaRadius * 0.76f
    val eastGateX = arenaCenterX + arenaRadius * 0.76f
    if (hypot(rCx - westGateX, rCy - arenaCenterY) < halfDiag + 380f) return true
    if (hypot(rCx - eastGateX, rCy - arenaCenterY) < halfDiag + 380f) return true
    if (hypot(rCx - westBrigReactorX, rCy - westBrigReactorY) < halfDiag + stasisJailRadius + 160f) return true
    if (hypot(rCx - eastBrigReactorX, rCy - eastBrigReactorY) < halfDiag + stasisJailRadius + 160f) return true
    // Protect Rift Control Alpha & Omega Zones
    if (hypot(rCx - riftAlphaX, rCy - riftAlphaY) < halfDiag + riftCaptureRadius + 140f) return true
    if (hypot(rCx - riftOmegaX, rCy - riftOmegaY) < halfDiag + riftCaptureRadius + 140f) return true
    // Protect Flux Core initial spawn north of Safe Zone
    val overlapsFluxSpawn = rRight >= 3780f && rx <= 4220f && rBottom >= 3140f && ry <= 3600f
    return overlapsFluxSpawn
  }

  fun generateSectorArena(seed: Int) {
    currentArenaSeed = seed
    walls.clear()
    pendingAsteroidRespawns.clear()
    swarmDrones.clear()
    getSafeZoneRadius()

    val cx = arenaCenterX
    val cy = arenaCenterY

    var rng = seed
    fun nextRand(): Float {
      rng = rng * 1664525 + 1013904223
      return ((rng ushr 8) and 0xFFFFFF).toFloat() / 0x1000000.toFloat()
    }

    val openStyles =
      listOf(
        SpaceObstacleStyle.ORBITAL_STATION,
        SpaceObstacleStyle.ASTEROID_STATION,
        SpaceObstacleStyle.CRYSTAL_ASTEROID,
        SpaceObstacleStyle.FLOATING_DEBRIS,
      )
    val clusterCount = ((arenaRadius / 3800f) * 18f).toInt().coerceIn(9, 18)
    for (i in 0 until clusterCount) {
      val radialMin = 680f
      val radialMax = (arenaRadius - 320f).coerceAtLeast(radialMin + 220f)
      val dist = radialMin + nextRand() * (radialMax - radialMin)
      val theta = nextRand() * (PI.toFloat() * 0.46f) + 0.04f
      val stylePick = openStyles[(i + (nextRand() * 4f).toInt()) % openStyles.size]
      val (w, h) =
        when (stylePick) {
          SpaceObstacleStyle.ORBITAL_STATION -> 168f to 168f
          SpaceObstacleStyle.ASTEROID_STATION -> 168f to 168f
          SpaceObstacleStyle.CRYSTAL_ASTEROID -> 164f to 164f
          else -> 160f to 160f
        }

      val offX = ((cos(theta) * dist).toInt() / 32) * 32f
      val offY = ((sin(theta) * dist).toInt() / 32) * 32f
      val candidates =
        listOf(
          WallRect(cx - offX - w * 0.5f, cy - offY - h * 0.5f, w, h, stylePick, seed + i * 31 + 1),
          WallRect(cx + offX - w * 0.5f, cy - offY - h * 0.5f, w, h, stylePick, seed + i * 31 + 2),
          WallRect(cx - offX - w * 0.5f, cy + offY - h * 0.5f, w, h, stylePick, seed + i * 31 + 3),
          WallRect(cx + offX - w * 0.5f, cy + offY - h * 0.5f, w, h, stylePick, seed + i * 31 + 4),
        )
      for (cand in candidates) {
        val overlapsExisting =
          walls.any { ex ->
            cand.x < ex.x + ex.w + 96f &&
              cand.x + cand.w + 96f > ex.x &&
              cand.y < ex.y + ex.h + 96f &&
              cand.y + cand.h + 96f > ex.y
          }
        if (!overlapsExisting && !rectOverlapsProtectedZone(cand.x, cand.y, cand.w, cand.h)) {
          walls.add(cand)
        }
      }
    }

    greens.clear()
    pendingBuffSpawns.clear()
    recentBuffSpawnLocations.clear()
    interdictorBubbles.clear()
    reactivePrecipitates.clear()
    reactiveFilaments.clear()
    spawnInitialSpatialAnomalies()
    val initialCap = getEffectiveBuffCap()
    repeat(initialCap) { spawnGreenCrate() }
    bakeStaticRadarBitmap()
  }

  fun bakeStaticRadarBitmap() {
    val size = 192
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val c = Canvas(bmp)
    val half = size * 0.5f
    val rSafe = arenaRadius.coerceAtLeast(1000f)
    val scale = half / rSafe

    val borderPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.8f
        color = Color.argb(145, 56, 189, 248)
      }
    c.drawCircle(half, half, half - 1.5f, borderPaint)

    if (hasActiveSafeZone()) {
      val safeRadPx = getSafeZoneRadius() * scale
      val safePaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
          style = Paint.Style.FILL
          color = Color.argb(55, 0, 245, 212)
        }
      c.drawCircle(half, half, safeRadPx, safePaint)
      val safeRingPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
          style = Paint.Style.STROKE
          strokeWidth = 1.4f
          color = Color.argb(140, 0, 245, 212)
        }
      c.drawCircle(half, half, safeRadPx, safeRingPaint)
    }

    val stationPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.argb(175, 148, 163, 184)
      }
    val asteroidPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.argb(175, 167, 139, 250)
      }
    for (w in walls) {
      val left = half + (w.x - arenaCenterX) * scale
      val top = half + (w.y - arenaCenterY) * scale
      val right = half + (w.x + w.w - arenaCenterX) * scale
      val bottom = half + (w.y + w.h - arenaCenterY) * scale
      if (w.style == SpaceObstacleStyle.CRYSTAL_ASTEROID || w.style == SpaceObstacleStyle.ASTEROID_STATION) {
        c.drawRoundRect(left, top, right, bottom, 3f, 3f, asteroidPaint)
      } else {
        c.drawRect(left, top, right, bottom, stationPaint)
      }
    }

    if (brickWalls.isNotEmpty()) {
      val barrierPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
          style = Paint.Style.FILL
          color = Color.argb(235, 0, 245, 212)
        }
      for (bw in brickWalls) {
        val left = half + (bw.x - arenaCenterX) * scale
        val top = half + (bw.y - arenaCenterY) * scale
        val right = half + (bw.x + bw.w - arenaCenterX) * scale
        val bottom = half + (bw.y + bw.h - arenaCenterY) * scale
        c.drawRect(left, top, max(left + 1.8f, right), max(top + 1.8f, bottom), barrierPaint)
      }
    }

    radarBgBitmap = bmp
  }

  fun isInsideSafeZone(x: Float, y: Float): Boolean {
    if (!hasActiveSafeZone()) return false
    val r = getSafeZoneRadius()
    return hypot(x - arenaCenterX, y - arenaCenterY) <= r
  }

  fun isWithinBotBanPerimeter(x: Float, y: Float): Boolean {
    if (!hasActiveSafeZone()) return false
    val margin = RiftCalibrationState.botSafeZoneBanMargin
    return hypot(x - arenaCenterX, y - arenaCenterY) <= getSafeZoneRadius() + margin
  }

  fun circleIntersectsWalls(cx: Float, cy: Float, r: Float): Boolean {
    if (hypot(cx - arenaCenterX, cy - arenaCenterY) + r > arenaRadius) return true
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

  fun circleIntersectsActiveAnomalies(cx: Float, cy: Float, padRadius: Float = 58f): Boolean {
    for (a in anomalies) {
      if (hypot(cx - a.x, cy - a.y) < a.physicalRadius + padRadius) return true
    }
    return false
  }

  fun isClearCrateSpawnPosition(
    cx: Float,
    cy: Float,
    wallMargin: Float = 52f,
    riftMargin: Float = 58f,
  ): Boolean {
    if (hypot(cx - arenaCenterX, cy - arenaCenterY) + wallMargin > arenaRadius) return false
    if (isInsideSafeZone(cx, cy)) return false
    if (circleIntersectsWalls(cx, cy, wallMargin)) return false
    if (circleIntersectsActiveAnomalies(cx, cy, riftMargin)) return false
    return true
  }

  fun resolveClearCrateDropPosition(prefX: Float, prefY: Float): Pair<Float, Float> {
    if (isClearCrateSpawnPosition(prefX, prefY)) return prefX to prefY
    for (ring in 1..14) {
      val searchR = ring * 68f
      for (step in 0 until 16) {
        val ang = (step / 16f) * (PI.toFloat() * 2f) + ring * 0.27f
        val candX = prefX + cos(ang) * searchR
        val candY = prefY + sin(ang) * searchR
        if (isClearCrateSpawnPosition(candX, candY)) {
          return candX to candY
        }
      }
    }
    val maxSpawnR = (arenaRadius - 180f).coerceAtLeast(680f)
    for (attempt in 0 until 48) {
      val ang = (attempt / 48f) * (PI.toFloat() * 2f)
      val dist = 620f + (attempt % 6) * ((maxSpawnR - 620f) / 6f)
      val candX = arenaCenterX + cos(ang) * dist
      val candY = arenaCenterY + sin(ang) * dist
      if (isClearCrateSpawnPosition(candX, candY)) {
        return candX to candY
      }
    }
    return prefX to prefY
  }

  private fun relocateOverlappingCrates() {
    for (g in greens) {
      if (!isClearCrateSpawnPosition(g.x, g.y)) {
        val clearPos = resolveClearCrateDropPosition(g.x, g.y)
        g.x = clearPos.first
        g.y = clearPos.second
      }
    }
  }

  fun spawnGreenCrate(): Boolean {
    val cap = getEffectiveBuffCap()
    if (greens.size >= cap) return false

    var chosenX = 0f
    var chosenY = 0f
    var found = false
    val maxSpawnR = (arenaRadius - 160f).coerceAtLeast(650f)

    // Attempt to pick a non-repeating random spot clear of obstacles, placed barriers, and active Playfield Rifts
    for (attempt in 0 until 80) {
      val minSeparation = if (attempt < 45) 250f else 140f
      val angle = Random.nextFloat() * (PI.toFloat() * 2f)
      val dist =
        if (Random.nextFloat() < 0.35f) {
          (580f + Random.nextFloat() * (maxSpawnR - 580f) * 0.55f).coerceAtMost(maxSpawnR)
        } else {
          560f + sqrt(Random.nextFloat()) * (maxSpawnR - 560f)
        }
      val x = arenaCenterX + cos(angle) * dist
      val y = arenaCenterY + sin(angle) * dist

      if (!isClearCrateSpawnPosition(x, y)) {
        continue
      }

      var tooClose = false
      for (g in greens) {
        if (hypot(g.x - x, g.y - y) < minSeparation) {
          tooClose = true
          break
        }
      }
      if (tooClose) continue

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
      val fallbackAng = Random.nextFloat() * PI.toFloat() * 2f
      val rawX = arenaCenterX + cos(fallbackAng) * 720f
      val rawY = arenaCenterY + sin(fallbackAng) * 720f
      val clearFallback = resolveClearCrateDropPosition(rawX, rawY)
      if (!isClearCrateSpawnPosition(clearFallback.first, clearFallback.second)) return false
      chosenX = clearFallback.first
      chosenY = clearFallback.second
    }

    if (recentBuffSpawnLocations.size >= 64) {
      recentBuffSpawnLocations.removeFirst()
    }
    recentBuffSpawnLocations.addLast(Pair(chosenX, chosenY))

    val chosenBuffType = rollRandomCrateBuffType()

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

  private fun rollRandomCrateBuffType(): Int {
    val customIndices = RiftCodex.getCustomForgedBuffs().map { it.buffTypeIndex }
    return if (customIndices.isNotEmpty() && Random.nextFloat() < 0.16f) {
      customIndices[Random.nextInt(customIndices.size)]
    } else {
      when (val baseRoll = Random.nextInt(16)) {
        10 ->
          when (Random.nextInt(5)) {
            0 -> -10 // Speed Overcharge (+8% Top Speed)
            1 -> -11 // Thrust Overcharge (+8% Thrust)
            2 -> -12 // Reactor Overcharge (+8% Flux Recharge)
            3 -> -13 // Turn Overcharge (+12% Turn Rate)
            else -> -14 // Capacitor Overcharge (+10% Max Flux)
          }
        else -> baseRoll
      }
    }
  }

  fun findRandomAnomalyPosition(existingAnomalies: List<SpatialAnomalyEntity> = emptyList()): Pair<Float, Float> {
    val maxAnomR = (arenaRadius - 320f).coerceAtLeast(820f)
    val westGateX = arenaCenterX - arenaRadius * 0.76f
    val eastGateX = arenaCenterX + arenaRadius * 0.76f
    for (attempt in 0 until 85) {
      val ang = Random.nextFloat() * PI.toFloat() * 2f
      val dist = 740f + sqrt(Random.nextFloat()) * (maxAnomR - 740f)
      val x = arenaCenterX + cos(ang) * dist
      val y = arenaCenterY + sin(ang) * dist
      val inSafePadded =
        hasActiveSafeZone() &&
          hypot(x - arenaCenterX, y - arenaCenterY) < getSafeZoneRadius() + 280f
      if (inSafePadded) continue
      if (circleIntersectsWalls(x, y, 140f)) continue

      if (hypot(x - westBrigReactorX, y - westBrigReactorY) < 760f) continue
      if (hypot(x - eastBrigReactorX, y - eastBrigReactorY) < 760f) continue
      if (hypot(x - riftAlphaX, y - riftAlphaY) < 720f) continue
      if (hypot(x - riftOmegaX, y - riftOmegaY) < 720f) continue

      if (hypot(x - westGateX, y - arenaCenterY) < 480f) continue
      if (hypot(x - eastGateX, y - arenaCenterY) < 480f) continue

      val tooCloseToOther = existingAnomalies.any { hypot(it.x - x, it.y - y) < 520f }
      if (tooCloseToOther) continue
      return x to y
    }
    return (arenaCenterX - maxAnomR * 0.55f) to (arenaCenterY - maxAnomR * 0.55f)
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
    relocateOverlappingCrates()
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
    relocateOverlappingCrates()
  }

  private fun spawnInitialSpatialAnomalies() {
    anomalies.clear()
    spawnSingleAnomaly(AnomalyType.PLASMA_RIFT)
    spawnSingleAnomaly(AnomalyType.REPULSOR_RIFT)
    spawnSingleAnomaly(AnomalyType.GRAVITY_RIFT)
    spawnPhaseRiftPair()
  }

  private fun getSafeZoneSpawnPosition(): Pair<Float, Float> {
    val rMax = (getSafeZoneRadius() - 72f).coerceAtLeast(60f)
    val ang = Random.nextFloat() * PI.toFloat() * 2f
    val dist = sqrt(Random.nextFloat()) * rMax
    return (arenaCenterX + cos(ang) * dist) to (arenaCenterY + sin(ang) * dist)
  }

  private fun getSkirmishGateSpawnPosition(freq: Int): Pair<Float, Float> {
    if (isStasisBreachRulesActive()) {
      val forwardBaseX =
        if (freq == 0) {
          westBrigReactorX + stasisJailRadius + 210f
        } else {
          eastBrigReactorX - stasisJailRadius - 210f
        }
      val baseY = arenaCenterY
      for (attempt in 0 until 32) {
        val x = forwardBaseX + (if (freq == 0) 1f else -1f) * Random.nextFloat() * 190f
        val y = baseY + (Random.nextFloat() - 0.5f) * 860f
        val clearOfWestJail = hypot(x - westBrigReactorX, y - westBrigReactorY) > stasisJailRadius + 64f
        val clearOfEastJail = hypot(x - eastBrigReactorX, y - eastBrigReactorY) > stasisJailRadius + 64f
        val inWorld = hypot(x - arenaCenterX, y - arenaCenterY) < arenaRadius - 96f
        if (inWorld && clearOfWestJail && clearOfEastJail && !circleIntersectsWalls(x, y, 42f)) {
          return x to y
        }
      }
      return forwardBaseX to (baseY + (if (freq == 0) -140f else 140f))
    }
    val gateOffset = (arenaRadius * 0.76f).coerceAtLeast(1050f)
    val baseX = if (freq == 0) arenaCenterX - gateOffset else arenaCenterX + gateOffset
    val baseY = arenaCenterY
    for (attempt in 0 until 25) {
      val x = baseX + (Random.nextFloat() - 0.5f) * 200f
      val y = baseY + (Random.nextFloat() - 0.5f) * 460f
      if (!circleIntersectsWalls(x, y, 42f)) return x to y
    }
    return baseX to baseY
  }

  private fun getStasisBrigPrisonerSpawnPosition(prisonerFreq: Int): Pair<Float, Float> {
    val cx = if (prisonerFreq == 0) eastBrigReactorX else westBrigReactorX
    val cy = if (prisonerFreq == 0) eastBrigReactorY else westBrigReactorY
    val ang = Random.nextFloat() * PI.toFloat() * 2f
    val dist = 120f + Random.nextFloat() * (stasisJailRadius - 180f)
    return (cx + cos(ang) * dist) to (cy + sin(ang) * dist)
  }

  private fun getOuterGateSpawnPosition(freq: Int): Pair<Float, Float> {
    if (isSkirmishRulesActive() || isStasisBreachRulesActive() || isRiftControlRulesActive()) {
      return getSkirmishGateSpawnPosition(freq)
    }
    val outerBandMin = (arenaRadius * 0.55f).coerceAtLeast(900f)
    val outerBandMax = (arenaRadius - 180f).coerceAtLeast(outerBandMin + 200f)
    for (i in 0 until 35) {
      val baseAng = if (freq % 2 == 0) PI.toFloat() else 0f
      val ang = baseAng + (Random.nextFloat() - 0.5f) * (PI.toFloat() * 0.85f)
      val dist = outerBandMin + Random.nextFloat() * (outerBandMax - outerBandMin)
      val x = arenaCenterX + cos(ang) * dist
      val y = arenaCenterY + sin(ang) * dist
      if (!circleIntersectsWalls(x, y, 38f) && !isWithinBotBanPerimeter(x, y)) {
        return x to y
      }
    }
    val fallbackX = if (freq % 2 == 0) arenaCenterX - outerBandMin else arenaCenterX + outerBandMin
    return fallbackX to arenaCenterY
  }

  fun rebuildRosterForMode(newMode: String = activeRankedMode.id) {
    if (isEphemeralPracticeRift) {
      remotePilots.clear()
      ships.removeAll { !it.isLocalPlayer && it.isHuman }
      if (ships.none { it.isLocalPlayer }) {
        ships.add(localPlayer)
      }
      isShipLockedForMatch = false
      return
    }
    val resolvedMode = RankedGameMode.fromId(newMode)
    activeRankedMode = resolvedMode
    mode = resolvedMode.id

    val isTeamMode = isTeamBasedRankedMode()
    isShipLockedForMatch = doesModeLockShipSelection(resolvedMode)
    val perTeamTarget =
      when (resolvedMode) {
        RankedGameMode.RIFT_SKIRMISH -> activeTeamSizeBracket.coerceIn(2, 5)
        RankedGameMode.STASIS_BREACH -> 10
        RankedGameMode.RIFT_CONTROL, RankedGameMode.SYNDICATE_WARS -> activeTeamSizeBracket.coerceIn(3, 10)
        else -> 20
      }
    teamSize = if (isTeamMode) perTeamTarget else 20
    val botsAllowedForSizing = currentCustomGame?.botsEnabled ?: RiftCalibrationState.botsEnabled
    val expectedTotalShips =
      if (botsAllowedForSizing) {
        if (isTeamMode) perTeamTarget * 2 else 20
      } else {
        (1 + remotePilots.size).coerceAtLeast(2)
      }
    updateDynamicArenaGeometryForShipCount(expectedTotalShips, regenerateObstacles = true)

    val overwatchUnjoined = isHubOverwatchActive && !hasJoinedMatch
    localPlayer.isInStasisBrig = false
    spectatingTargetShipId = ""

    // Deterministic cross-device West (0) / East (1) team assignment so all devices agree on opposing sides!
    val assignedTeamByUid = mutableMapOf<String, Int>()
    if (isTeamMode) {
      data class HumanSlot(
        val uid: String,
        val groupKey: String,
      )
      val humanSlots = mutableListOf<HumanSlot>()
      val localKey = currentAuthenticatedUid.ifBlank { localPlayer.id }
      if (!overwatchUnjoined) {
        val myCleanSyn =
          localPlayer.syndicateTag.trim().uppercase().takeIf {
            it.isNotEmpty() && it !in setOf("GUEST", "SOLO", "FREE", "BOT", "UNAFFILIATED", "RFT")
          } ?: ""
        val myGrp =
          when {
            localPlayer.armadaName.isNotBlank() -> "ARM_${localPlayer.armadaName.trim().uppercase()}"
            resolvedMode == RankedGameMode.SYNDICATE_WARS && myCleanSyn.isNotEmpty() -> "SYN_$myCleanSyn"
            else -> "SOLO_$localKey"
          }
        humanSlots.add(HumanSlot(localKey, myGrp))
      }
      for ((rUid, rp) in remotePilots) {
        if (rp.id == localPlayer.id || rUid == localKey) continue
        val rCleanSyn =
          rp.syndicateTag.trim().uppercase().takeIf { isRealPlayerSyndicateAllianceTag(it) } ?: ""
        val rGrp =
          when {
            rp.armadaName.isNotBlank() -> "ARM_${rp.armadaName.trim().uppercase()}"
            resolvedMode == RankedGameMode.SYNDICATE_WARS && rCleanSyn.isNotEmpty() -> "SYN_$rCleanSyn"
            else -> "SOLO_$rUid"
          }
        humanSlots.add(HumanSlot(rUid, rGrp))
      }

      val sortedGroups =
        humanSlots
          .groupBy { it.groupKey }
          .entries
          .sortedBy { entry -> entry.value.minOf { it.uid } }
      var westAssigned = 0
      var eastAssigned = 0
      for ((_, members) in sortedGroups) {
        val targetFreq =
          when {
            westAssigned + members.size <= perTeamTarget && westAssigned <= eastAssigned -> 0
            eastAssigned + members.size <= perTeamTarget -> 1
            westAssigned <= eastAssigned -> 0
            else -> 1
          }
        for (m in members.sortedBy { it.uid }) {
          val slotFreq =
            if (targetFreq == 0 && westAssigned < perTeamTarget) {
              westAssigned++
              0
            } else if (eastAssigned < perTeamTarget) {
              eastAssigned++
              1
            } else {
              westAssigned++
              0
            }
          assignedTeamByUid[m.uid] = slotFreq
        }
      }
      localPlayer.freq = if (overwatchUnjoined) 0 else (assignedTeamByUid[localKey] ?: 0)
      val sp = getSkirmishGateSpawnPosition(localPlayer.freq)
      localPlayer.x = sp.first
      localPlayer.y = sp.second
      localPlayer.vx = 0f
      localPlayer.vy = 0f
      localPlayer.angle = if (localPlayer.freq == 1) PI.toFloat() else 0f
      localPlayer.weaponAngle = localPlayer.angle
      localPlayer.inSafeZone = false
      localPlayer.wasInSafeZone = false
      localPlayer.weaponsLocked = false
      localPlayer.canFire = true
      localPlayer.weaponState = "ARMED"
      localPlayer.phaseShieldTimer = RiftCalibrationState.exitPhaseShieldSec
    } else {
      localPlayer.freq = 0
    }

    val updated = mutableListOf<ShipEntity>()
    updated.add(localPlayer)
    var friendlyCount = if (!overwatchUnjoined && localPlayer.freq == 0) 1 else 0
    var rivalCount = if (!overwatchUnjoined && localPlayer.freq == 1) 1 else 0

    var freeForAllFreqCounter = 1
    for ((rUid, rp) in remotePilots.entries.sortedBy { it.key }) {
      if (rp.id != localPlayer.id) {
        if (isTeamMode) {
          rp.freq = assignedTeamByUid[rUid] ?: if (friendlyCount <= rivalCount) 0 else 1
          if (rp.freq == 0) friendlyCount++ else rivalCount++
        } else {
          rp.freq = freeForAllFreqCounter++
          rivalCount++
        }
        updated.add(rp)
      }
    }
    if (resolvedMode == RankedGameMode.SYNDICATE_WARS) {
      val opposingHumanTag =
        remotePilots.values
          .firstOrNull {
            it.freq != localPlayer.freq && isRealPlayerSyndicateAllianceTag(it.syndicateTag)
          }
          ?.syndicateTag
      if (!opposingHumanTag.isNullOrBlank()) {
        rivalSyndicateWarTag = opposingHumanTag
      }
    }

    val botsAllowed = currentCustomGame?.botsEnabled ?: RiftCalibrationState.botsEnabled
    var botIdx = 0
    if (botsAllowed) {
      if (isTeamMode) {
        while (friendlyCount < perTeamTarget) {
          val shipId = ((botIdx * 3) % 9) + 1
          val spec = RiftCodex.getShipById(shipId)
          val pos = getSkirmishGateSpawnPosition(0)
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
              angle = 0f,
              weaponAngle = 0f,
              bounty = 100,
              phaseShieldTimer = 3.0f,
              warpInAnimTimer = 0.65f,
            )
          )
          friendlyCount++
          botIdx++
        }

        while (rivalCount < perTeamTarget) {
          val shipId = ((botIdx * 5 + 1) % 9) + 1
          val spec = RiftCodex.getShipById(shipId)
          val pos = getSkirmishGateSpawnPosition(1)
          updated.add(
            ShipEntity(
              id = "bot_rival_$botIdx",
              callsign = BOT_CALLSIGNS[(botIdx + 7) % BOT_CALLSIGNS.size],
              syndicateTag = "BOT",
              armadaName = "",
              shipId = shipId,
              spec = spec,
              freq = 1,
              isHuman = false,
              isLocalPlayer = false,
              x = pos.first,
              y = pos.second,
              angle = PI.toFloat(),
              weaponAngle = PI.toFloat(),
              bounty = 100,
              phaseShieldTimer = 3.0f,
              warpInAnimTimer = 0.65f,
            )
          )
          rivalCount++
          botIdx++
        }
      } else {
        val maxTotalPlayers = 20
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
              freq = botIdx + 1,
              isHuman = false,
              isLocalPlayer = false,
              x = pos.first,
              y = pos.second,
              angle = PI.toFloat(),
              weaponAngle = PI.toFloat(),
              bounty = 100,
              warpInAnimTimer = 0.65f,
            )
          )
          rivalCount++
          botIdx++
        }
      }
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
        "HIGH RESONANCE SHUTDOWN: $killerName ELIMINATED $victimName ($victimBounty RES)"
      highBountyShutdownTimer = 4.2f
    }
    while (killFeed.size > 8) {
      killFeed.removeAt(killFeed.lastIndex)
    }
  }

  fun updateRemotePilots(states: List<LivePilotState>, currentUid: String) {
    val liveOnlineUids = mutableSetOf<String>()
    for (st in states) {
      val uid = st.userId.ifEmpty { st.id }
      if (uid.isEmpty() || uid == currentUid) continue
      liveOnlineUids.add(uid)
      onlineRemoteSectorByUid[uid] = st.sectorId.ifBlank { "NEXUS_LOBBY" }
    }
    val offlineUids = onlineRemoteSectorByUid.keys.filter { it !in liveOnlineUids }
    for (off in offlineUids) {
      onlineRemoteSectorByUid.remove(off)
    }

    if ((!hasJoinedMatch && !isHubOverwatchActive) || isEphemeralPracticeRift) {
      if (remotePilots.isNotEmpty()) {
        remotePilots.clear()
      }
      if (isEphemeralPracticeRift) {
        ships.removeAll { !it.isLocalPlayer && it.isHuman }
      }
      return
    }

    currentAuthenticatedUid = currentUid
    val activeMatchRemoteUids = mutableSetOf<String>()
    var needsInitialRosterBuild = ships.isEmpty()

    for (st in states) {
      val uid = st.userId.ifEmpty { st.id }
      if (uid.isEmpty() || uid == currentUid) continue
      if (!matchesCurrentInstanceSector(st.sectorId)) continue

      activeMatchRemoteUids.add(uid)
      val remoteEntityId = "remote_$uid"
      val existing = remotePilots[uid]
      val spec = RiftCodex.getShipById(st.shipId.toInt())
      val cleanRemoteSyn =
        st.syndicateTag.trim().uppercase().let { raw ->
          if (
            raw in com.example.data.LocalPilotPrefs.NON_PLAYER_SYNDICATE_TAGS ||
              raw in LEGACY_FOUNDRY_TAGS
          ) {
            ""
          } else {
            raw.replace(Regex("[^A-Z0-9]"), "").take(3).let { t ->
              if (
                t in com.example.data.LocalPilotPrefs.NON_PLAYER_SYNDICATE_TAGS ||
                  t in LEGACY_FOUNDRY_TAGS
              ) {
                ""
              } else {
                t
              }
            }
          }
        }
      val nowElapsedMs = android.os.SystemClock.elapsedRealtime()
      val rawTargetX = st.x.toFloat()
      val rawTargetY = st.y.toFloat()
      val maxAllowedR = (arenaRadius - 36f).coerceAtLeast(120f)
      val rawDx = rawTargetX - arenaCenterX
      val rawDy = rawTargetY - arenaCenterY
      val rawDistFromCenter = hypot(rawDx, rawDy)
      val targetX =
        if (rawDistFromCenter > maxAllowedR && rawDistFromCenter > 0.001f) {
          arenaCenterX + (rawDx / rawDistFromCenter) * maxAllowedR
        } else {
          rawTargetX
        }
      val targetY =
        if (rawDistFromCenter > maxAllowedR && rawDistFromCenter > 0.001f) {
          arenaCenterY + (rawDy / rawDistFromCenter) * maxAllowedR
        } else {
          rawTargetY
        }
      val targetVx = st.vx.toFloat()
      val targetVy = st.vy.toFloat()
      val targetAngle = st.angle.toFloat()
      val targetWeaponAngle = st.weaponAngle.toFloat()

      if (existing == null) {
        val assignedFreq =
          if (isTeamBasedRankedMode()) {
            st.freq.toInt().coerceIn(0, 1)
          } else {
            (1..99).firstOrNull { f -> ships.none { it.freq == f } }
              ?: (((uid.hashCode() and 0x7FFFFFFF) % 80) + 11)
          }
        val created =
          ShipEntity(
            id = remoteEntityId,
            callsign = st.callsign,
            syndicateTag = cleanRemoteSyn,
            armadaName = st.armadaName,
            shipId = spec.id,
            spec = spec,
            freq = assignedFreq,
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
            maxFlux = st.maxEnergy.toFloat().coerceAtLeast(100f),
            bounty = st.bounty.toInt(),
            liveResonanceRating = st.liveResonance.toInt().coerceAtLeast(0),
            afterburning = st.isAfterburning,
            isCloaked = st.isCloaked,
            remoteFiringPrimary = st.isFiringPrimary,
            respawnTimer = if (st.isRespawning) 2.5f else 0f,
            isInStasisBrig = st.isInStasisBrig,
            lastRemoteSecondarySeq = st.secondarySeq,
            netTargetX = targetX,
            netTargetY = targetY,
            netTargetVx = targetVx,
            netTargetVy = targetVy,
            netTargetAngle = targetAngle,
            netTargetWeaponAngle = targetWeaponAngle,
            netPacketAgeSec = 0f,
            netWasRespawning = st.isRespawning,
            netLocalKillGraceSec = 0f,
            netRespawnHoldSec = 0f,
            lastNetUpdateElapsedMs = nowElapsedMs,
            hasNetTarget = true,
          )
        remotePilots[uid] = created
        currentLifeHadPvp = true
        if (!needsInitialRosterBuild) {
          replaceBotWithHuman(created)
        }
        if (
          activeRankedMode == RankedGameMode.SYNDICATE_WARS &&
            created.freq != localPlayer.freq &&
            isRealPlayerSyndicateAllianceTag(cleanRemoteSyn)
        ) {
          rivalSyndicateWarTag = cleanRemoteSyn
        }
        if (!st.isRespawning && st.isFiringPrimary && created.gunTimer <= 0f) {
          firePrimaryWeapon(created)
        }
      } else {
        currentLifeHadPvp = true
        existing.lastNetUpdateElapsedMs = nowElapsedMs
        existing.callsign = st.callsign
        existing.syndicateTag = cleanRemoteSyn
        existing.armadaName = st.armadaName
        if (isTeamBasedRankedMode()) {
          existing.freq = st.freq.toInt().coerceIn(0, 1)
        }
        if (existing.shipId != spec.id) {
          existing.shipId = spec.id
          existing.spec = spec
        }
        existing.independentAimActive = true
        existing.maxFlux = st.maxEnergy.toFloat().coerceAtLeast(100f)
        existing.bounty = st.bounty.toInt()
        existing.liveResonanceRating = st.liveResonance.toInt().coerceAtLeast(0)
        existing.afterburning = st.isAfterburning
        existing.isCloaked = st.isCloaked
        existing.remoteFiringPrimary = st.isFiringPrimary
        existing.isInStasisBrig = st.isInStasisBrig

        val staleWhileLocallyKilled =
          !st.isRespawning && existing.netLocalKillGraceSec > 0f && !existing.netWasRespawning
        if (!staleWhileLocallyKilled) {
          existing.flux = st.energy.toFloat()
        }
        val justEnteredRespawn =
          st.isRespawning && !existing.netWasRespawning && existing.respawnTimer <= 0f
        val justFinishedRespawn =
          !st.isRespawning && existing.netWasRespawning && !staleWhileLocallyKilled

        if (st.isRespawning) {
          existing.netWasRespawning = true
          existing.netLocalKillGraceSec = 0f
          if (justEnteredRespawn) {
            existing.deathX = existing.x
            existing.deathY = existing.y
            existing.respawnTimer = max(1.35f, RiftCalibrationState.respawnDelaySec)
            existing.netRespawnHoldSec = 0f
            shockwaves.add(
              ShockwaveEntity(existing.x, existing.y, 18f, 185f, Color.parseColor("#F43F5E"))
            )
            if (hypot(existing.x - localPlayer.x, existing.y - localPlayer.y) < 1100f) {
              sfx.playExplosion()
            }
          } else {
            existing.respawnTimer = max(existing.respawnTimer, 0.35f)
          }
        } else if (!staleWhileLocallyKilled) {
          existing.netWasRespawning = false
          existing.netRespawnHoldSec = 0f
          if (
            existing.respawnTimer > 0f &&
              (!isSkirmishRulesActive() || skirmishReadyCountdownSec > 0f || justFinishedRespawn)
          ) {
            existing.respawnTimer = 0f
            existing.deathLingerTimer = 0f
            existing.warpInAnimTimer = 0.65f
          }
        }

        val jumpDist = hypot(targetX - existing.x, targetY - existing.y)
        val shouldSnapImmediately =
          !existing.hasNetTarget ||
            justFinishedRespawn ||
            st.isRespawning ||
            jumpDist > 650f
        if (!staleWhileLocallyKilled) {
          if (shouldSnapImmediately) {
            existing.x = targetX
            existing.y = targetY
            existing.vx = targetVx
            existing.vy = targetVy
            existing.angle = targetAngle
            existing.weaponAngle = targetWeaponAngle
            existing.prevHeadingForBank = targetAngle
            existing.netSmoothOffsetX = 0f
            existing.netSmoothOffsetY = 0f
            existing.netSmoothVelX = 0f
            existing.netSmoothVelY = 0f
          } else {
            existing.netSmoothOffsetX = existing.x - targetX
            existing.netSmoothOffsetY = existing.y - targetY
            existing.netSmoothVelX = existing.vx - targetVx
            existing.netSmoothVelY = existing.vy - targetVy
          }
          existing.netTargetX = targetX
          existing.netTargetY = targetY
          existing.netTargetVx = targetVx
          existing.netTargetVy = targetVy
          existing.netTargetAngle = targetAngle
          existing.netTargetWeaponAngle = targetWeaponAngle
          existing.netPacketAgeSec = 0f
          existing.hasNetTarget = true
        }

        if (!needsInitialRosterBuild && ships.none { it.id == existing.id }) {
          replaceBotWithHuman(existing)
        }

        if (
          !st.isRespawning &&
            !staleWhileLocallyKilled &&
            st.isFiringPrimary &&
            existing.gunTimer <= 0f
        ) {
          firePrimaryWeapon(existing)
        }
        if (
          !st.isRespawning &&
            !staleWhileLocallyKilled &&
            st.secondarySeq > existing.lastRemoteSecondarySeq
        ) {
          existing.lastRemoteSecondarySeq = st.secondarySeq
          existing.flux = max(existing.flux, existing.spec.bombCost + 25f)
          existing.bombTimer = 0f
          fireSecondaryBomb(existing, st.secondaryAim.toFloat())
        }
      }
    }

    val nowCleanupMs = android.os.SystemClock.elapsedRealtime()
    val staleUids =
      remotePilots.entries
        .filter { (uid, ship) ->
          if (uid in activeMatchRemoteUids) {
            false
          } else {
            val reportedSector = onlineRemoteSectorByUid[uid]
            if (reportedSector != null && !matchesCurrentInstanceSector(reportedSector)) {
              true
            } else {
              nowCleanupMs - ship.lastNetUpdateElapsedMs > 4_500L
            }
          }
        }
        .map { it.key }
    if (staleUids.isNotEmpty()) {
      val botsAllowed = currentCustomGame?.botsEnabled ?: RiftCalibrationState.botsEnabled
      val cap =
        when (activeRankedMode) {
          RankedGameMode.RIFT_SKIRMISH -> (activeTeamSizeBracket.coerceIn(2, 5) * 2)
          RankedGameMode.RIFT_CONTROL, RankedGameMode.SYNDICATE_WARS ->
            (activeTeamSizeBracket.coerceIn(3, 10) * 2)
          else -> 20
        }
      for (stale in staleUids) {
        val removedRemote = remotePilots.remove(stale)
        ships.removeAll { it.id == "remote_$stale" }
        if (!needsInitialRosterBuild && botsAllowed && ships.size < cap) {
          val backfillIdx = ships.size + (stale.hashCode() and 0x7FFF)
          val botShipId = ((backfillIdx * 5 + 1) % 9) + 1
          val botSpec = RiftCodex.getShipById(botShipId)
          val botFreq =
            if (isTeamBasedRankedMode()) {
              removedRemote?.freq?.coerceIn(0, 1) ?: 1
            } else {
              (1..99).firstOrNull { f -> ships.none { it.freq == f } } ?: (ships.size + 1)
            }
          val spawnPos =
            if (isTeamBasedRankedMode()) getSkirmishGateSpawnPosition(botFreq)
            else getOuterGateSpawnPosition(1)
          ships.add(
            ShipEntity(
              id = "bot_backfill_${System.nanoTime()}",
              callsign = BOT_CALLSIGNS[backfillIdx % BOT_CALLSIGNS.size],
              syndicateTag = "BOT",
              armadaName = "",
              shipId = botShipId,
              spec = botSpec,
              freq = botFreq,
              isHuman = false,
              isLocalPlayer = false,
              x = spawnPos.first,
              y = spawnPos.second,
              angle = if (botFreq == 1) PI.toFloat() else 0f,
              weaponAngle = if (botFreq == 1) PI.toFloat() else 0f,
              bounty = 100,
              warpInAnimTimer = 0.65f,
            )
          )
        }
      }
    }

    if (needsInitialRosterBuild && (hasJoinedMatch || isHubOverwatchActive)) {
      rebuildRosterForMode(mode)
    }
  }

  fun isLocalObjectiveHost(currentUid: String): Boolean {
    if (!hasJoinedMatch || isEphemeralPracticeRift || currentUid.isBlank()) return false
    val lowestRemoteUid = remotePilots.keys.minOrNull()
    return lowestRemoteUid == null || currentUid <= lowestRemoteUid
  }

  fun exportSectorInstanceState(currentUid: String): com.example.data.SectorInstanceState {
    val jailedCsv =
      if (isStasisBreachRulesActive()) {
        val ids = mutableListOf<String>()
        if (localPlayer.isInStasisBrig && currentUid.isNotBlank()) ids.add(currentUid)
        for ((rUid, rp) in remotePilots) {
          if (rp.isInStasisBrig) ids.add(rUid)
        }
        ids.sorted().joinToString(",")
      } else {
        ""
      }
    val resolvedCarrierUid =
      when {
        fluxCoreCarrierEntityId == localPlayer.id -> currentUid
        fluxCoreCarrierEntityId.startsWith("remote_") -> fluxCoreCarrierEntityId.removePrefix("remote_")
        else -> fluxCoreCarrierEntityId
      }
    return com.example.data.SectorInstanceState(
      sectorId = activeMatchSectorTag(),
      hostUid = currentUid,
      fluxCarrierUid = resolvedCarrierUid,
      fluxOrbX = fluxCoreOrb.x.toDouble(),
      fluxOrbY = fluxCoreOrb.y.toDouble(),
      fluxOrbVx = fluxCoreOrb.vx.toDouble(),
      fluxOrbVy = fluxCoreOrb.vy.toDouble(),
      fluxTier = fluxCoreOrb.overchargeTier.toLong(),
      fluxHoldSec = fluxCoreOrb.holdDurationSec.toDouble(),
      rcWestScore = riftControlWestScore.toDouble(),
      rcEastScore = riftControlEastScore.toDouble(),
      rcTimerSec = riftControlMatchTimerSec.toDouble(),
      rcAlphaCtrl = riftAlphaControlSec.toDouble(),
      rcAlphaLock = riftAlphaLockedBy.toLong(),
      rcOmegaCtrl = riftOmegaControlSec.toDouble(),
      rcOmegaLock = riftOmegaLockedBy.toLong(),
      sbTimerSec = stasisBreachMatchTimerSec.toDouble(),
      sbWestCaps = stasisWestCaptures.toLong(),
      sbEastCaps = stasisEastCaptures.toLong(),
      sbWestCoreHp = westBrigGeneratorHp.toDouble(),
      sbEastCoreHp = eastBrigGeneratorHp.toDouble(),
      jailedUidsCsv = jailedCsv,
      updatedAtMs = System.currentTimeMillis(),
    )
  }

  fun applyRemoteSectorInstanceState(state: com.example.data.SectorInstanceState?, currentUid: String) {
    if (state == null || !hasJoinedMatch || isEphemeralPracticeRift) return
    if (state.hostUid.isBlank() || state.hostUid == currentUid) return
    if (isLocalObjectiveHost(currentUid)) return
    if (
      !state.sectorId.equals(activeMatchSectorTag(), ignoreCase = true) &&
        !matchesCurrentInstanceSector(state.sectorId)
    ) {
      return
    }

    if (isRiftControlRulesActive()) {
      riftControlWestScore = state.rcWestScore.toFloat().coerceIn(0f, 1000f)
      riftControlEastScore = state.rcEastScore.toFloat().coerceIn(0f, 1000f)
      riftAlphaControlSec = state.rcAlphaCtrl.toFloat().coerceIn(-15f, 15f)
      riftOmegaControlSec = state.rcOmegaCtrl.toFloat().coerceIn(-15f, 15f)
      riftAlphaLockedBy = state.rcAlphaLock.toInt().coerceIn(-1, 1)
      riftOmegaLockedBy = state.rcOmegaLock.toInt().coerceIn(-1, 1)
      riftControlMatchTimerSec = state.rcTimerSec.toFloat().coerceIn(0f, 180f)
    } else if (isStasisBreachRulesActive()) {
      stasisBreachMatchTimerSec = state.sbTimerSec.toFloat().coerceIn(0f, 180f)
      stasisWestCaptures = max(stasisWestCaptures, state.sbWestCaps.toInt().coerceAtLeast(0))
      stasisEastCaptures = max(stasisEastCaptures, state.sbEastCaps.toInt().coerceAtLeast(0))
      westBrigGeneratorHp = state.sbWestCoreHp.toFloat().coerceIn(0f, getEffectiveBrigReactorMaxHp())
      eastBrigGeneratorHp = state.sbEastCoreHp.toFloat().coerceIn(0f, getEffectiveBrigReactorMaxHp())
      val jailedSet =
        state.jailedUidsCsv
          .split(",")
          .map { it.trim() }
          .filter { it.isNotEmpty() }
          .toSet()
      if (currentUid in jailedSet && !localPlayer.isInStasisBrig) {
        localPlayer.isInStasisBrig = true
      } else if (currentUid !in jailedSet && localPlayer.isInStasisBrig && state.jailedUidsCsv.isEmpty()) {
        localPlayer.isInStasisBrig = false
      }
    } else if (isFluxCoreRulesActive()) {
      val mappedCarrier =
        when {
          state.fluxCarrierUid.isBlank() -> ""
          state.fluxCarrierUid == currentUid -> localPlayer.id
          remotePilots.containsKey(state.fluxCarrierUid) -> "remote_${state.fluxCarrierUid}"
          else -> ""
        }
      fluxCoreCarrierEntityId = mappedCarrier
      if (mappedCarrier.isEmpty()) {
        fluxCoreOrb.x = state.fluxOrbX.toFloat()
        fluxCoreOrb.y = state.fluxOrbY.toFloat()
        fluxCoreOrb.vx = state.fluxOrbVx.toFloat()
        fluxCoreOrb.vy = state.fluxOrbVy.toFloat()
      }
      fluxCoreOrb.overchargeTier = state.fluxTier.toInt().coerceIn(0, 10)
      fluxCoreOrb.holdDurationSec = state.fluxHoldSec.toFloat().coerceAtLeast(0f)
    }
  }

  fun applyIncomingCombatEvents(events: List<com.example.data.SectorCombatEvent>, currentUid: String) {
    if (!hasJoinedMatch || isEphemeralPracticeRift || currentUid.isBlank()) return
    for (ev in events) {
      if (ev.id.isBlank() || !processedCombatEventIds.add(ev.id)) continue
      if (processedCombatEventIds.size > 240) {
        val first = processedCombatEventIds.firstOrNull()
        if (first != null) processedCombatEventIds.remove(first)
      }
      if (
        !ev.sectorId.equals(activeMatchSectorTag(), ignoreCase = true) &&
          !matchesCurrentInstanceSector(ev.sectorId)
      ) {
        continue
      }
      if (ev.senderUid == currentUid) continue
      if (ev.eventType == "ELIMINATED" && ev.targetUid == currentUid && localPlayer.respawnTimer <= 0f) {
        val remoteAttacker = remotePilots[ev.senderUid]
        applyProjectileDamage(
          target = localPlayer,
          attackerId = remoteAttacker?.id ?: "remote_${ev.senderUid}",
          attackerIsHuman = true,
          rawDamage = (localPlayer.flux + 500f).coerceAtLeast(1500f),
          impactAngle = localPlayer.angle + PI.toFloat(),
          isHeavyImpact = true,
          isEmp = false,
          attackerShipLabel = remoteAttacker?.let { formatShipLabel(it) } ?: "Rival Pilot",
          weaponLabel = ev.weaponLabel.ifBlank { "Plasma Cannon" },
        )
      } else if (ev.eventType == "ELIMINATED" && ev.targetUid != currentUid) {
        val victimShip = remotePilots[ev.targetUid]
        val award = ev.bountyAward.toInt().coerceAtLeast(25)
        pushKillFeedEntry(
          killerTag = ev.senderTag,
          killerName = ev.senderCallsign,
          killerIsLocal = false,
          killerIsAllied = false,
          victimTag = victimShip?.syndicateTag ?: "",
          victimName = victimShip?.callsign ?: "PILOT",
          victimIsLocal = false,
          victimIsAllied = false,
          victimBounty = award,
          bountyAwarded = award,
          wasHvt = award >= RiftCalibrationState.hvtBountyThreshold / 2,
        )
      }
    }
  }

  fun switchLocalPlayerShip(newShipId: Int, forceFromHangar: Boolean = false): Boolean {
    if (isShipLockedForMatch && doesModeLockShipSelection()) {
      sfx.playMutedClick()
      onPrizeToast("Ship is locked during Rift Skirmish & Syndicate Wars. Choose your hull before deploying.")
      return false
    }
    val atFullFlux = localPlayer.flux >= localPlayer.maxFlux * 0.98f
    val inSafe = isInsideSafeZone(localPlayer.x, localPlayer.y)
    if (!forceFromHangar && !atFullFlux && !inSafe) {
      onPrizeToast("MUST BE AT 100% FLUX OR IN SAFE ZONE TO SWAP HULL!")
      return false
    }
    val spec = RiftCodex.getShipById(newShipId)
    val customHealthMult = currentCustomGame?.healthMult ?: 1.0f
    localPlayer.shipId = spec.id
    localPlayer.spec = spec
    localPlayer.liveResonanceRating = getShipResonance(spec.id)
    localPlayer.maxFlux = spec.maxFlux * (1f + localPlayer.maxFluxOverchargeStacks * 0.10f) * customHealthMult
    localPlayer.flux = localPlayer.maxFlux
    localPlayer.repels = spec.initialRepels
    localPlayer.shotgunWavesRemaining = 0
    localPlayer.shotgunWaveTimer = 0f
    localPlayer.isCloaked = false
    localPlayer.sniperOverchargeReady = false
    if (!spec.supportsNativeMultifire && localPlayer.multiShotCharges <= 0) {
      localPlayer.multifireOn = false
    }
    if (forceFromHangar) {
      spawnLocalPlayerCleanly()
    } else {
      localPlayer.isIdleDraining = false
      localPlayer.playfieldIdleTimer = 0f
      localPlayer.safeZoneStayTimer = 0f
      localPlayer.safeZoneLockoutTimer = 0f
    }
    currentLifeStats.shipId = spec.id
    currentLifeStats.shipFlown = formatShipLabel(localPlayer)
    buffVersion++
    syncLocalArmadaSelfSlot()
    onPrizeToast("HULL EQUIPPED: ${formatShipLabel(localPlayer).uppercase(java.util.Locale.US)} (${localPlayer.maxFlux.toInt()} FLUX)")
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
      onPrizeToast("RETRO-BRAKES ENGAGED")
      notifyLocalLifecycleSync()
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
        "0 EXTRA FLUX"
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
        spawnPooledFct("OVERCHARGE RAIL +${(bonusPct * 100f).toInt()}%", ship.x, ship.y - 26f, Color.parseColor("#00F5D4"))
      }
    }
    val fluxCoreTierBonus =
      if (
        activeRankedMode == RankedGameMode.FLUX_CORE &&
          (fluxCoreCarrierEntityId == ship.id || (fluxCoreOrb.carrierShipId == ship.shipId && hypot(ship.x - fluxCoreOrb.x, ship.y - fluxCoreOrb.y) < 120f))
      ) {
        fluxCoreOrb.overchargeTier.coerceIn(0, 10) * 0.05f
      } else {
        0f
      }
    val customDmgMult = currentCustomGame?.damageMult ?: 1.0f
    val customSpdMult = currentCustomGame?.speedMult ?: 1.0f
    val overchargeMult = (if (overcharged) (1.0f + bonusPct) else 1.0f) * (1.0f + fluxCoreTierBonus) * customDmgMult

    // Permanent-Until-Death Bouncing Bullets Hull Upgrade (+1 wall bounce)
    val bonusBounce = if (ship.hasBouncingBulletsUpgrade) 1 else 0
    val bounces = spec.primaryBounces + bonusBounce
    val bulletSpeed = spec.primarySpeed * (if (overcharged) (1.0f + bonusPct) else 1.0f) * customSpdMult
    val isFriendlyToLocal = areAllied(localPlayer, ship)
    val baseColor =
      when {
        overcharged || fluxCoreTierBonus > 0f -> Color.parseColor("#00F5D4")
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
          // 3-Suppressor (or Rift Frame Native Split) -> 3 spread streams where each deals exact 1/3 (33.3%) damage at 0 extra Flux cost
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
        spawnBulletWithMuzzleCheck(
          ship.x,
          ship.y,
          BulletEntity(
            ownerId = ship.id,
            ownerShipId = ship.spec.id,
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
          ),
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

      spawnBulletWithMuzzleCheck(
        ship.x,
        ship.y,
        BulletEntity(
          ownerId = ship.id,
          ownerShipId = ship.spec.id,
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
        ),
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
        text = "360° EMP PULSE",
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

  fun fireSecondaryAbility(ship: ShipEntity, aimAngle: Float? = null) {
    fireSecondaryBomb(ship, aimAngle)
  }

  private fun detonateActiveBombAtIndex(index: Int) {
    if (index !in bombs.indices) return
    val b = bombs[index]
    val isMissile = b.type == "MISSILE"
    val isCustomOrb = b.type == "CUSTOM_ORB"
    val expRadius =
      when (b.type) {
        "CUSTOM_ORB" -> if (b.customBlastRadius > 0f) b.customBlastRadius else 190f
        "MISSILE" -> 155f
        "SIEGE" -> 235f
        "EMP" -> 240f
        "PROX", "MINE" -> (b.radius + RiftCalibrationState.proxFuseRadius) * 1.35f
        else -> b.radius * 3.8f
      }
    val expDmg = if (b.type == "SIEGE" || isMissile || isCustomOrb) b.damage else b.damage * 0.65f
    if (b.type == "PROX") {
      spawnProximityFlakShrapnel(b)
    }
    for (s in ships) {
      if (!(isInLobbyStaging && s.isLocalPlayer) && !isBombAlliedWithShip(b, s) && s.respawnTimer <= 0f && !isInsideSafeZone(s.x, s.y)) {
        val dist = hypot(s.x - b.x, s.y - b.y)
        if (dist < s.radius + expRadius) {
          val falloff = (1f - (dist / (s.radius + expRadius)) * 0.35f).coerceIn(0.55f, 1f)
          val incAngle = atan2(b.y - s.y, b.x - s.x)
          applyProjectileDamage(
            target = s,
            attackerId = b.ownerId,
            attackerIsHuman = b.ownerIsHuman,
            rawDamage = expDmg * falloff,
            impactAngle = incAngle,
            isHeavyImpact = true,
            isEmp = b.type == "EMP" || (isCustomOrb && b.customEffect == CustomBuffEffect.EMP),
            attackerShipLabel = b.ownerShipLabel,
            weaponLabel = if (b.type == "SIEGE") "Mid-Air Siege Detonation" else b.weaponLabel,
          )
        }
      }
    }
    shockwaves.add(ShockwaveEntity(b.x, b.y, 18f, expRadius, Color.parseColor("#F97316")))
    shockwaves.add(ShockwaveEntity(b.x, b.y, 10f, expRadius * 0.72f, Color.parseColor("#FACC15")))
    if (hypot(b.x - localPlayer.x, b.y - localPlayer.y) < 1200f) {
      sfx.playExplosion()
    }
    if (b.ownerId == localPlayer.id) {
      addTrauma(0.32f)
      spawnPooledFct("SIEGE DETONATED", b.x, b.y - 26f, Color.parseColor("#F97316"), maxLife = 1.0f)
    }
    spawnExplosion(b.x, b.y, Color.parseColor("#F97316"))
    bombs.removeAt(index)
  }

  /**
   * Handles the Contextual Class / Secondary Ability Button for all ships.
   */
  fun fireSecondaryBomb(ship: ShipEntity, aimAngle: Float? = null) {
    if (ship.respawnTimer > 0f) return
    if (ship.isLocalPlayer && isBombCancelActive) return

    // Goliath (#4) Manual Mid-Air Siege Bomb Detonation at 0 extra Flux
    if (ship.spec.id == 4 || ship.spec.bombType == "SIEGE") {
      val activeSiegeIdx = bombs.indexOfFirst { it.ownerId == ship.id && it.type == "SIEGE" }
      if (activeSiegeIdx >= 0) {
        detonateActiveBombAtIndex(activeSiegeIdx)
        if (ship.isLocalPlayer) {
          buffVersion++
        }
        return
      }
    }

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

    // Toggle OFF continuous Secondary Tractor Beam / Lance Beam at 0 extra Flux
    if (spec.bombType == "TRACTOR_BEAM" && ship.tractorBeamActive) {
      val wasSecondary = ship.tractorFromSecondary
      clearShipToggleBuffs(ship, clearSlotIfActive = !wasSecondary)
      ship.bombTimer = spec.bombCooldown.coerceAtLeast(0.45f)
      if (ship.isLocalPlayer) {
        localSecondaryFireSeq++
        sfx.playMutedClick()
        spawnPooledFct("TRACTOR BEAM OFF", ship.x, ship.y - 26f, Color.parseColor("#34D399"))
      }
      return
    }
    if (spec.bombType == "LANCE_BEAM" && ship.lanceBeamActive) {
      val wasSecondary = ship.lanceFromSecondary
      clearShipToggleBuffs(ship, clearSlotIfActive = !wasSecondary)
      ship.bombTimer = spec.bombCooldown.coerceAtLeast(0.55f)
      if (ship.isLocalPlayer) {
        localSecondaryFireSeq++
        sfx.playMutedClick()
        spawnPooledFct("LANCE BEAM OFF", ship.x, ship.y - 26f, Color.parseColor("#A855F7"))
      }
      return
    }

    // 1. Disruptor Combined Tactical Cloak + 360° AoE EMP Pulse System (No Bomb Projectile; 0-Flux Decloak EMP)
    if (spec.bombType == "CLOAK_EMP") {
      if (!ship.isCloaked) {
        if (ship.flux <= 180f) {
          if (ship.isLocalPlayer) onPrizeToast("INSUFFICIENT FLUX FOR TACTICAL CLOAK (MIN 180 FLUX)")
          return
        }
        ship.isCloaked = true
        ship.bombTimer = 0.35f
        selfDrainFlux(ship, 20f)
        if (ship.isLocalPlayer) {
          localSecondaryFireSeq++
          buffVersion++
          sfx.playWarp()
          spawnPooledFct("STEALTH CLOAK ENGAGED", ship.x, ship.y - 28f, Color.parseColor("#B388FF"))
          onPrizeToast("STEALTH CLOAK ACTIVE (${RiftCalibrationState.cloakDrainPerSec.toInt()} FLUX/S) — TAP EMP PULSE OR FIRE TO UNLEASH 360° EMP!")
        }
        return
      } else {
        // Already cloaked: tapping EMP PULSE breaks stealth and unleashes a 360° radial AoE EMP centered on Disruptor's hull at 0 Flux!
        ship.isCloaked = false
        if (ship.isLocalPlayer) {
          localSecondaryFireSeq++
          buffVersion++
        }
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
        onPrizeToast("INSUFFICIENT FLUX (${cost.toInt()} FLUX REQUIRED)")
      }
      return
    }

    // 1b. Eclipse Stasis (Two-Way Containment Field: 275px radius, 6.0s duration, 320 Flux cost, 8.5s CD)
    if (spec.bombType == "INTERDICTOR_DOME") {
      val bubbleR = RiftCalibrationState.interdictorBubbleRadius
      val safeDist = hypot(ship.x - arenaCenterX, ship.y - arenaCenterY)
      if (hasActiveSafeZone() && safeDist < (getSafeZoneRadius() + bubbleR + 30f)) {
        if (ship.isLocalPlayer) {
          sfx.playMutedClick()
          onPrizeToast("CANNOT ANCHOR STASIS OVER SAFE ZONE PERIMETER")
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
        localSecondaryFireSeq++
        buffVersion++
        currentLifeStats.shotsFired++
        addTrauma(0.28f)
        spawnPooledFct(
          text = "STASIS (${dur.toInt()}s)",
          x = ship.x,
          y = ship.y - 30f,
          color = Color.parseColor("#EC4899"),
          maxLife = 1.25f,
        )
      }
      return
    }

    if (spec.bombType == "TRACTOR_BEAM") {
      if (ship.flux <= 90f) {
        if (ship.isLocalPlayer) onPrizeToast("INSUFFICIENT FLUX FOR TRACTOR BEAM")
        return
      }
      clearShipToggleBuffs(ship, clearSlotIfActive = false)
      selfDrainFlux(ship, cost)
      ship.bombTimer = 0.25f
      ship.tractorBeamActive = true
      ship.tractorFromSecondary = true
      ship.tractorTargetShipId = ""
      if (ship.isLocalPlayer) {
        localSecondaryFireSeq++
        currentLifeStats.shotsFired++
        buffVersion++
        sfx.playWarp()
        spawnPooledFct("TRACTOR WEB ENGAGED", ship.x, ship.y - 28f, Color.parseColor("#34D399"))
      }
      return
    }

    if (spec.bombType == "LANCE_BEAM") {
      if (ship.flux <= 90f) {
        if (ship.isLocalPlayer) onPrizeToast("INSUFFICIENT FLUX FOR LANCE BEAM")
        return
      }
      clearShipToggleBuffs(ship, clearSlotIfActive = false)
      selfDrainFlux(ship, cost)
      ship.bombTimer = 0.25f
      ship.lanceBeamActive = true
      ship.lanceFromSecondary = true
      ship.lanceActiveStacks = 1
      ship.lanceTickTimer = 0.05f
      ship.lanceHitSparkActive = false
      if (ship.isLocalPlayer) {
        localSecondaryFireSeq++
        currentLifeStats.shotsFired++
        buffVersion++
        sfx.playLaser()
        spawnPooledFct("LANCE BEAM ENGAGED", ship.x, ship.y - 28f, Color.parseColor("#A855F7"))
      }
      return
    }

    selfDrainFlux(ship, cost)
    ship.bombTimer = spec.bombCooldown
    if (ship.isLocalPlayer) {
      localSecondaryFireSeq++
    }

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
        val inWorld = hypot(candX - arenaCenterX, candY - arenaCenterY) <= arenaRadius - ship.radius - 8f
        val blockedBySafeLockout =
          hasActiveSafeZone() &&
            ship.safeZoneLockoutTimer > 0f &&
            hypot(candX - arenaCenterX, candY - arenaCenterY) <= getSafeZoneRadius() + ship.radius + 6f
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
        val crossesBrigWall =
          if (isStasisBreachRulesActive()) {
            val wDist0 = hypot(startX - westBrigReactorX, startY - westBrigReactorY)
            val wDist1 = hypot(candX - westBrigReactorX, candY - westBrigReactorY)
            val eDist0 = hypot(startX - eastBrigReactorX, startY - eastBrigReactorY)
            val eDist1 = hypot(candX - eastBrigReactorX, candY - eastBrigReactorY)
            val crossesWest =
              (wDist0 <= stasisJailRadius && wDist1 > stasisJailRadius - ship.radius - 2f) ||
                (wDist0 > stasisJailRadius && wDist1 < stasisJailRadius + ship.radius + 2f)
            val crossesEast =
              (eDist0 <= stasisJailRadius && eDist1 > stasisJailRadius - ship.radius - 2f) ||
                (eDist0 > stasisJailRadius && eDist1 < stasisJailRadius + ship.radius + 2f)
            crossesWest || crossesEast
          } else {
            false
          }
        if (
          !inWorld ||
            isInsideSafeZone(candX, candY) ||
            blockedBySafeLockout ||
            crossesBubbleWall ||
            crossesBrigWall ||
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
        spawnPooledFct("PHASE DASH + RAIL OVERCHARGE", ship.x, ship.y - 28f, Color.parseColor("#00F5D4"))
        notifyLocalLifecycleSync()
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
    if (ship.isLocalPlayer) {
      notifyLocalLifecycleSync()
    }

    val customDmgMult = currentCustomGame?.damageMult ?: 1.0f
    val customSpdMult = currentCustomGame?.speedMult ?: 1.0f

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
          damage = spec.bombDamage * customDmgMult,
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

    // 6b. All Supply Crate Buffs Unlocked as Rift Frame Secondaries
    if (spec.bombType == "CHAKRAM") {
      launchChakramDisc(ship, fireAngle)
      return
    }
    if (spec.bombType == "PROX_SEEKER") {
      val existingProx = bombs.filter { it.ownerId == ship.id && it.isInfiniteProxMissile }
      if (existingProx.size >= 2) {
        val oldest = existingProx.firstOrNull()
        if (oldest != null) bombs.remove(oldest)
      }
      launchInfiniteProximityMissile(ship, fireAngle)
      return
    }
    if (spec.bombType == "SWARM_DRONES") {
      val droneSpd = 560f * customSpdMult
      val droneDmg = 210f * customDmgMult
      val lockTgt = findMissileLockTarget(ship, fireAngle)
      for (idx in 0 until 5) {
        val launchAng = fireAngle + (idx - 2) * 0.26f
        val sx = ship.x + cos(launchAng) * 42f
        val sy = ship.y + sin(launchAng) * 42f
        if (!isInsideSafeZone(sx, sy)) {
          swarmDrones.add(
            SwarmDroneEntity(
              id = "swarm_${nextDecoyBrickId++}",
              ownerId = ship.id,
              ownerCallsign = ship.callsign,
              ownerSyndicateTag = ship.syndicateTag,
              ownerArmadaName = ship.armadaName,
              ownerIsHuman = ship.isHuman,
              freq = ship.freq,
              ownerShipLabel = shipLbl,
              x = sx,
              y = sy,
              vx = cos(launchAng) * droneSpd,
              vy = sin(launchAng) * droneSpd,
              angle = launchAng,
              life = 6.5f,
              maxLife = 6.5f,
              zigPhase = idx * 1.25f,
              damage = droneDmg,
              targetShipId = lockTgt?.id ?: "",
            )
          )
        }
      }
      shockwaves.add(ShockwaveEntity(ship.x, ship.y, 16f, 145f, Color.parseColor("#00F5D4")))
      if (ship.isLocalPlayer) {
        sfx.playWarp()
        spawnPooledFct("5x SWARM DRONES LAUNCHED", ship.x, ship.y - 28f, Color.parseColor("#00F5D4"))
      }
      return
    }
    if (spec.bombType == "HOLO_PHANTOM") {
      val decoyDur = RiftCalibrationState.holoPhantomDuration.coerceAtLeast(4.0f)
      val decoySpd = ship.spec.topSpeed * 0.92f * customSpdMult
      decoys.removeAll { it.ownerId == ship.id }
      decoys.add(
        DecoyEntity(
          id = "decoy_${nextDecoyBrickId++}",
          ownerId = ship.id,
          callsign = ship.callsign,
          syndicateTag = ship.syndicateTag,
          armadaName = ship.armadaName,
          shipId = ship.shipId,
          freq = ship.freq,
          x = ship.x + cos(fireAngle) * 34f,
          y = ship.y + sin(fireAngle) * 34f,
          vx = cos(fireAngle) * decoySpd,
          vy = sin(fireAngle) * decoySpd,
          angle = fireAngle,
          bounty = ship.bounty,
          life = decoyDur,
          maxLife = decoyDur,
        )
      )
      if (ship.isLocalPlayer) {
        sfx.playWarp()
        spawnPooledFct("HOLO PHANTOM DEPLOYED", ship.x, ship.y - 28f, Color.parseColor("#2DD4BF"))
      }
      return
    }
    if (spec.bombType == "STASIS_BARRIER") {
      deployOrStackStasisBarrier(
        ship = ship,
        deployAngle = ship.angle,
        span = RiftCalibrationState.stasisBarrierLength,
        barDur = RiftCalibrationState.stasisBarrierDuration.coerceAtLeast(5.0f),
        accent = Color.parseColor("#00F5D4"),
        labelTitle = "STASIS BARRIER",
      )
      return
    }
    if (spec.bombType == "HYPER_SURGE") {
      val surgeDur = RiftCalibrationState.hyperSurgeDuration.coerceAtLeast(1.5f)
      ship.rocketBoostTimer = surgeDur
      val boostSpd = ship.spec.afterburnerSpeed * RiftCalibrationState.hyperSurgeSpeedMult * customSpdMult
      ship.vx = cos(ship.angle) * boostSpd
      ship.vy = sin(ship.angle) * boostSpd
      shockwaves.add(ShockwaveEntity(ship.x, ship.y, 18f, 160f, Color.parseColor("#F43F5E")))
      if (ship.isLocalPlayer) {
        sfx.playWarp()
        addTrauma(0.22f)
        spawnPooledFct("HYPER SURGE (${surgeDur.toInt()}s)", ship.x, ship.y - 28f, Color.parseColor("#F43F5E"))
        notifyLocalLifecycleSync()
      }
      return
    }
    if (spec.bombType == "REPEL_PULSE") {
      val repelRadius = RiftCalibrationState.repelRadius
      val pushForce = RiftCalibrationState.repelPushForce
      shockwaves.add(ShockwaveEntity(ship.x, ship.y, 32f, repelRadius, Color.parseColor("#38BDF8")))
      for (s in ships) {
        if (s.id != ship.id && s.respawnTimer <= 0f) {
          val dx = s.x - ship.x
          val dy = s.y - ship.y
          val dist = hypot(dx, dy)
          if (dist < repelRadius && dist > 1f) {
            val push = ((repelRadius - dist) / repelRadius) * pushForce
            s.vx += (dx / dist) * push
            s.vy += (dy / dist) * push
          }
        }
      }
      for (b in bullets) {
        val dx = b.x - ship.x
        val dy = b.y - ship.y
        val dist = hypot(dx, dy)
        if (dist < repelRadius && dist > 1f) {
          val sp = max(720f, hypot(b.vx, b.vy))
          b.vx = (dx / dist) * sp
          b.vy = (dy / dist) * sp
        }
      }
      for (bm in bombs) {
        val dx = bm.x - ship.x
        val dy = bm.y - ship.y
        val dist = hypot(dx, dy)
        if (dist < repelRadius && dist > 1f) {
          val sp = max(640f, hypot(bm.vx, bm.vy))
          bm.vx = (dx / dist) * sp
          bm.vy = (dy / dist) * sp
        }
      }
      if (ship.isLocalPlayer) {
        sfx.playRepel()
        addTrauma(0.24f)
        spawnPooledFct("REPEL PULSE", ship.x, ship.y - 28f, Color.parseColor("#38BDF8"))
      }
      return
    }
    if (spec.bombType == "NATIVE_MULTI") {
      ship.multiShotCharges = min(60, ship.multiShotCharges + 18)
      ship.multifireOn = true
      if (ship.isLocalPlayer) {
        buffVersion++
        sfx.playPrize()
        spawnPooledFct("MULTI OVERDRIVE [${ship.multiShotCharges}]", ship.x, ship.y - 28f, Color.parseColor("#F59E0B"))
      }
      return
    }
    if (spec.bombType == "NATIVE_RICOCHET") {
      val fanAngles = floatArrayOf(-0.18f, -0.09f, 0f, 0.09f, 0.18f)
      val bSpd = 880f * customSpdMult
      val bDmg = 145f * customDmgMult
      for (off in fanAngles) {
        val a = fireAngle + off
        val bx = ship.x + cos(a) * 42f
        val by = ship.y + sin(a) * 42f
        if (!isInsideSafeZone(bx, by)) {
          spawnBulletWithMuzzleCheck(
            ship.x,
            ship.y,
            BulletEntity(
              ownerId = ship.id,
              ownerShipId = ship.spec.id,
              ownerCallsign = ship.callsign,
              ownerSyndicateTag = ship.syndicateTag,
              ownerArmadaName = ship.armadaName,
              ownerIsHuman = ship.isHuman,
              freq = ship.freq,
              x = bx,
              y = by,
              vx = ship.vx * 0.2f + cos(a) * bSpd,
              vy = ship.vy * 0.2f + sin(a) * bSpd,
              damage = bDmg,
              bouncesLeft = 4,
              life = 2.2f,
              color = Color.parseColor("#00F5D4"),
              isHeavyBurst = true,
              ownerShipLabel = shipLbl,
              weaponLabel = "Ricochet Volley",
            ),
          )
        }
      }
      return
    }
    val customForgedSec = RiftCodex.getCustomForgedBuffs().firstOrNull { it.id == spec.bombType }
    if (customForgedSec != null) {
      val ox = ship.x + cos(fireAngle) * 50f
      val oy = ship.y + sin(fireAngle) * 50f
      if (!isInsideSafeZone(ox, oy)) {
        val accent = runCatching { Color.parseColor(customForgedSec.strokeColorHex) }.getOrDefault(Color.parseColor("#22D3EE"))
        spawnBombWithMuzzleCheck(
          ship.x,
          ship.y,
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
            vx = ship.vx * 0.2f + cos(fireAngle) * (customForgedSec.projectileSpeed * customSpdMult),
            vy = ship.vy * 0.2f + sin(fireAngle) * (customForgedSec.projectileSpeed * customSpdMult),
            damage = customForgedSec.damage * customDmgMult,
            bouncesLeft = customForgedSec.wallBounces,
            passWalls = false,
            life = customForgedSec.flightDuration,
            radius = 19f,
            ownerShipLabel = shipLbl,
            weaponLabel = customForgedSec.officialName,
            customColor = accent,
            customEffect = customForgedSec.specialEffect,
            customBlastRadius = customForgedSec.blastRadius,
          ),
        )
      }
      return
    }

    // 7. Standard Warheads: "BOUNCE" (2-Ricochet), "SIEGE" (4-Goliath), "PROX" (7-Valkyrie)
    val bx = ship.x + cos(fireAngle) * 52f
    val by = ship.y + sin(fireAngle) * 52f
    if (isInsideSafeZone(bx, by)) return

    val speed = (if (spec.bombType == "SIEGE") 540f else 640f) * customSpdMult
    val warheadRadius = if (spec.bombType == "SIEGE") 24f else 17f
    val warheadTitle =
      when (spec.bombType) {
        "SIEGE" -> "Siege Bomb"
        "BOUNCE" -> "Bounce Bomb"
        "PROX" -> "Proximity Flak Bomb"
        else -> spec.secondaryShortLabel
      }
    spawnBombWithMuzzleCheck(
      ship.x,
      ship.y,
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
        damage = spec.bombDamage * customDmgMult,
        bouncesLeft = spec.bombBounces,
        passWalls = false,
        life = 30.0f,
        radius = warheadRadius,
        ownerShipLabel = shipLbl,
        weaponLabel = warheadTitle,
      ),
    )
  }

  /**
   * Stacking & Overwrite Inventory Rules for Bucket A Special Buff Slot Pickups (Charge-Based, 0 Flux Cost).
   */
  fun grantSpecialBuffWeapon(ship: ShipEntity, newType: String, baseGrant: Int) {
    val canonicalType = RiftCodex.normalizeSpecialBuffId(newType)
    val currentCanonical = RiftCodex.normalizeSpecialBuffId(ship.specialWeaponType)
    if (
      currentCanonical == canonicalType &&
        (ship.specialWeaponCharges > 0 || ship.tractorBeamActive || ship.lanceBeamActive)
    ) {
      ship.specialWeaponCharges = min(12, ship.specialWeaponCharges + baseGrant)
      ship.specialWeaponStacks = min(6, max(1, ship.specialWeaponStacks) + 1)
      ship.specialWeaponType = canonicalType
      if (ship.lanceBeamActive && canonicalType == "LANCE_BEAM") {
        ship.lanceActiveStacks = ship.specialWeaponStacks
      }
    } else {
      clearShipToggleBuffs(ship, clearSlotIfActive = false)
      ship.specialWeaponType = canonicalType
      ship.specialWeaponCharges = baseGrant
      ship.specialWeaponStacks = 1
      ship.shotgunWavesRemaining = 0
      ship.shotgunWaveTimer = 0f
      ship.novaWavesRemaining = 0
      ship.novaWaveTimer = 0f
    }
  }

  private fun fireRadialBurstProjectiles(ship: ShipEntity) {
    if (ship.isLocalPlayer) {
      sfx.playLaser()
      addTrauma(0.25f)
    }
    val customSpdMult = currentCustomGame?.speedMult ?: 1.0f
    val customDmgMult = currentCustomGame?.damageMult ?: 1.0f
    val count = RiftCalibrationState.novaBoltCount.coerceIn(6, 64)
    val boltSpeed = RiftCalibrationState.novaBoltSpeed * customSpdMult
    val boltDmg = RiftCalibrationState.novaBoltDamage * customDmgMult
    val shipLbl = formatShipLabel(ship)
    for (i in 0 until count) {
      val a = (PI.toFloat() * 2f * i) / count
      val bx = ship.x + cos(a) * 44f
      val by = ship.y + sin(a) * 44f
      if (isInsideSafeZone(bx, by)) continue
      spawnBulletWithMuzzleCheck(
        ship.x,
        ship.y,
        BulletEntity(
          ownerId = ship.id,
          ownerShipId = ship.spec.id,
          ownerCallsign = ship.callsign,
          ownerSyndicateTag = ship.syndicateTag,
          ownerArmadaName = ship.armadaName,
          ownerIsHuman = ship.isHuman,
          freq = ship.freq,
          x = bx,
          y = by,
          vx = cos(a) * boltSpeed,
          vy = sin(a) * boltSpeed,
          damage = boltDmg,
          bouncesLeft = 2,
          life = 1.9f,
          color = Color.parseColor("#F59E0B"),
          isHeavyBurst = true,
          ownerShipLabel = shipLbl,
          weaponLabel = "Nova Ring",
        ),
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
    val customSpdMult = currentCustomGame?.speedMult ?: 1.0f
    val customDmgMult = currentCustomGame?.damageMult ?: 1.0f
    val pelletCount = RiftCalibrationState.shatterPelletsPerWave.coerceIn(3, 24)
    val pelletDmg = RiftCalibrationState.shatterPelletDamage * customDmgMult
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
      val speed = (880f + (Random.nextFloat() - 0.5f) * 60f) * customSpdMult
      spawnBulletWithMuzzleCheck(
        ship.x,
        ship.y,
        BulletEntity(
          ownerId = ship.id,
          ownerShipId = ship.spec.id,
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
        ),
      )
    }
  }

  private fun launchLockOnBouncingMissile(ship: ShipEntity, aimAngle: Float) {
    if (ship.isLocalPlayer) {
      sfx.playBomb()
      addTrauma(0.20f)
    }
    val customSpdMult = currentCustomGame?.speedMult ?: 1.0f
    val customDmgMult = currentCustomGame?.damageMult ?: 1.0f
    val seekerSpd = max(RiftCalibrationState.seekerSpeed, 560f) * customSpdMult
    val seekerLife = RiftCalibrationState.seekerFlightTime
    val seekerDmg = RiftCalibrationState.seekerDamage * customDmgMult
    val target = findMissileLockTarget(ship, aimAngle)
    val launchAngle =
      if (target != null) {
        val dist = hypot(target.x - ship.x, target.y - ship.y)
        val leadT = (dist / seekerSpd.coerceAtLeast(1f)).coerceIn(0f, 0.55f)
        atan2((target.y + target.vy * leadT) - ship.y, (target.x + target.vx * leadT) - ship.x)
      } else {
        aimAngle
      }
    val mx = ship.x + cos(launchAngle) * 48f
    val my = ship.y + sin(launchAngle) * 48f
    if (!isInsideSafeZone(mx, my)) {
      spawnBombWithMuzzleCheck(
        ship.x,
        ship.y,
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
        ),
      )
      if (target != null) {
        target.missileLockWarningTimer = seekerLife
        if (ship.isLocalPlayer) {
          sfx.playMissileLockBeep()
          spawnPooledFct(
            text = "SEEKER LOCK: ${target.callsign}",
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
    val customSpdMult = currentCustomGame?.speedMult ?: 1.0f
    val customDmgMult = currentCustomGame?.damageMult ?: 1.0f
    val shrapSpd = 760f * customSpdMult
    val shrapDmg = 145f * customDmgMult
    val shrapnelCount = 8
    for (i in 0 until shrapnelCount) {
      val a = (PI.toFloat() * 2f * i) / shrapnelCount
      spawnBulletWithMuzzleCheck(
        bomb.x,
        bomb.y,
        BulletEntity(
          ownerId = bomb.ownerId,
          ownerShipId = 7,
          ownerCallsign = bomb.ownerCallsign,
          ownerSyndicateTag = bomb.ownerSyndicateTag,
          ownerArmadaName = bomb.ownerArmadaName,
          ownerIsHuman = bomb.ownerIsHuman,
          freq = bomb.freq,
          x = bomb.x + cos(a) * 18f,
          y = bomb.y + sin(a) * 18f,
          vx = cos(a) * shrapSpd,
          vy = sin(a) * shrapSpd,
          damage = shrapDmg,
          bouncesLeft = 1,
          life = 0.42f,
          color = Color.parseColor("#FF7043"),
          isHeavyBurst = true,
          ownerShipLabel = bomb.ownerShipLabel,
          weaponLabel = "Proximity Flak Shrapnel",
        ),
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

  private fun deployOrStackStasisBarrier(
    ship: ShipEntity,
    deployAngle: Float,
    span: Float,
    barDur: Float,
    accent: Int,
    labelTitle: String,
    perpOffsetPx: Float = 0f,
    allowSameSpotStack: Boolean = true,
    emitFeedback: Boolean = true,
  ) {
    val thickness = 48f
    val deployDist = 98f
    val perpX = -sin(deployAngle)
    val perpY = cos(deployAngle)
    var cx = ship.x + cos(deployAngle) * deployDist + perpX * perpOffsetPx
    var cy = ship.y + sin(deployAngle) * deployDist + perpY * perpOffsetPx
    val dCenter = hypot(cx - arenaCenterX, cy - arenaCenterY)
    val maxBarrierDist = (arenaRadius - span * 0.5f - 48f).coerceAtLeast(620f)
    if (dCenter > maxBarrierDist) {
      cx = arenaCenterX + ((cx - arenaCenterX) / dCenter) * maxBarrierDist
      cy = arenaCenterY + ((cy - arenaCenterY) / dCenter) * maxBarrierDist
    }
    if (isInsideSafeZone(cx, cy)) return

    val stackMate =
      if (allowSameSpotStack) {
        brickWalls.firstOrNull { bw ->
          (bw.ownerId == ship.id || (isTeamBasedRankedMode() && bw.freq == ship.freq)) &&
            hypot((bw.x + bw.w * 0.5f) - cx, (bw.y + bw.h * 0.5f) - cy) <= 78f
        }
      } else {
        null
      }
    if (stackMate != null) {
      stackMate.stackCount = (stackMate.stackCount + 1).coerceAtMost(9)
      stackMate.life = max(stackMate.life, barDur * (1f + 0.25f * (stackMate.stackCount - 1)))
      val mCx = stackMate.x + stackMate.w * 0.5f
      val mCy = stackMate.y + stackMate.h * 0.5f
      shockwaves.add(ShockwaveEntity(mCx, mCy, 14f, 130f, accent))
      bakeStaticRadarBitmap()
      if (emitFeedback && ship.isLocalPlayer) {
        sfx.playBounce()
        spawnPooledFct("BARRIER STACK x${stackMate.stackCount}", mCx, mCy - 26f, accent)
      }
      return
    }

    val horizPerp = kotlin.math.abs(sin(deployAngle)) >= kotlin.math.abs(cos(deployAngle))
    val w = if (horizPerp) span else thickness
    val h = if (horizPerp) thickness else span
    val bx = cx - w * 0.5f
    val by = cy - h * 0.5f
    brickWalls.add(
      BrickWallEntity(
        id = "brick_${nextDecoyBrickId++}",
        ownerId = ship.id,
        freq = ship.freq,
        x = bx,
        y = by,
        w = w,
        h = h,
        life = barDur,
        maxLife = barDur,
        stackCount = 1,
      )
    )
    relocateOverlappingCrates()
    val closestX = ship.x.coerceIn(bx, bx + w)
    val closestY = ship.y.coerceIn(by, by + h)
    val dx = ship.x - closestX
    val dy = ship.y - closestY
    val dist = hypot(dx, dy)
    if (dist < ship.radius + 4f) {
      val pushAng = if (dist > 0.5f) atan2(dy, dx) else (deployAngle + PI.toFloat())
      ship.x = closestX + cos(pushAng) * (ship.radius + 6f)
      ship.y = closestY + sin(pushAng) * (ship.radius + 6f)
    }
    bakeStaticRadarBitmap()
    shockwaves.add(ShockwaveEntity(cx, cy, 12f, 110f, accent))
    if (emitFeedback && ship.isLocalPlayer) {
      sfx.playBounce()
      spawnPooledFct("$labelTitle (${barDur.toInt()}s)", cx, cy - 24f, accent)
    }
  }

  private fun launchInfiniteProximityMissile(ship: ShipEntity, aimAngle: Float) {
    if (ship.isLocalPlayer) {
      sfx.playBomb()
      addTrauma(0.20f)
    }
    val customSpdMult = currentCustomGame?.speedMult ?: 1.0f
    val customDmgMult = currentCustomGame?.damageMult ?: 1.0f
    val cruiseSpd = 540f * customSpdMult
    val lockR = 340f
    val mx = ship.x + cos(aimAngle) * 48f
    val my = ship.y + sin(aimAngle) * 48f
    if (isInsideSafeZone(mx, my)) return

    val initialInside = mutableSetOf<String>()
    var initialLock: ShipEntity? = null
    var initialBestDist = lockR
    for (other in ships) {
      if (
        !(isInLobbyStaging && other.isLocalPlayer) &&
          !areAllied(ship, other) &&
          other.respawnTimer <= 0f &&
          !other.isCloaked &&
          !isInsideSafeZone(other.x, other.y)
      ) {
        val d = hypot(other.x - mx, other.y - my)
        if (d <= lockR) {
          initialInside.add(other.id)
          if (d < initialBestDist) {
            initialBestDist = d
            initialLock = other
          }
        }
      }
    }

    spawnBombWithMuzzleCheck(
      ship.x,
      ship.y,
      BombEntity(
        ownerId = ship.id,
        ownerCallsign = ship.callsign,
        ownerSyndicateTag = ship.syndicateTag,
        ownerArmadaName = ship.armadaName,
        ownerIsHuman = ship.isHuman,
        freq = ship.freq,
        type = "PROX_MISSILE",
        x = mx,
        y = my,
        vx = cos(aimAngle) * cruiseSpd,
        vy = sin(aimAngle) * cruiseSpd,
        damage = 840f * customDmgMult,
        bouncesLeft = 999_999,
        passWalls = false,
        life = 999_999f,
        radius = 18f,
        targetShipId = initialLock?.id ?: "",
        ownerShipLabel = formatShipLabel(ship),
        weaponLabel = "Proximity Missile",
        armTimer = 0.20f,
        isInfiniteProxMissile = true,
        proxLockRadius = lockR,
        shipsInsideRadiusPrevStep = initialInside,
      ),
    )
    if (ship.isLocalPlayer) {
      spawnPooledFct(
        text = if (initialLock != null) "PROX LOCK: ${initialLock.callsign}" else "PROX MISSILE ACTIVE",
        x = ship.x,
        y = ship.y - 28f,
        color = Color.parseColor("#FACC15"),
      )
    }
  }

  private fun launchChakramDisc(ship: ShipEntity, aimAngle: Float) {
    val customSpdMult = currentCustomGame?.speedMult ?: 1.0f
    val customDmgMult = currentCustomGame?.damageMult ?: 1.0f
    val discSpeed = 760f * customSpdMult
    val discDmg = 560f * customDmgMult
    val cx = ship.x + cos(aimAngle) * 48f
    val cy = ship.y + sin(aimAngle) * 48f
    if (isInsideSafeZone(cx, cy)) return

    spawnBombWithMuzzleCheck(
      ship.x,
      ship.y,
      BombEntity(
        ownerId = ship.id,
        ownerCallsign = ship.callsign,
        ownerSyndicateTag = ship.syndicateTag,
        ownerArmadaName = ship.armadaName,
        ownerIsHuman = ship.isHuman,
        freq = ship.freq,
        type = "CHAKRAM",
        x = cx,
        y = cy,
        vx = ship.vx * 0.22f + cos(aimAngle) * discSpeed,
        vy = ship.vy * 0.22f + sin(aimAngle) * discSpeed,
        damage = discDmg,
        bouncesLeft = 4,
        passWalls = false,
        life = 3.4f,
        radius = 24f,
        ownerShipLabel = formatShipLabel(ship),
        weaponLabel = "Chakram Saw Disc",
        customColor = Color.parseColor("#F59E0B"),
      ),
    )
    if (ship.isLocalPlayer) {
      sfx.playShotgun()
      addTrauma(0.18f)
      spawnPooledFct(
        text = "CHAKRAM LAUNCHED",
        x = ship.x,
        y = ship.y - 28f,
        color = Color.parseColor("#F59E0B"),
      )
    }
  }

  /**
   * Activates or toggles a Bucket A Special Buff Slot item for any ship (Local Player or AI Bot),
   * including N-stacked Nova/Barrier/Lance scaling and toggleable Tractor & Lance beams.
   */
  fun activateShipSpecialBuff(ship: ShipEntity) {
    if (
      ship.respawnTimer > 0f ||
        (isInLobbyStaging && ship.isLocalPlayer) ||
        (isSkirmishRulesActive() && skirmishReadyCountdownSec > 0f)
    ) {
      return
    }

    // Toggle OFF if Tractor Beam or Lance Beam is already active (clears the buff immediately)
    if (ship.tractorBeamActive) {
      clearShipToggleBuffs(ship, clearSlotIfActive = true)
      if (ship.isLocalPlayer) {
        sfx.playMutedClick()
        spawnPooledFct("TRACTOR BEAM OFF", ship.x, ship.y - 26f, Color.parseColor("#34D399"))
      }
      return
    }
    if (ship.lanceBeamActive) {
      clearShipToggleBuffs(ship, clearSlotIfActive = true)
      if (ship.isLocalPlayer) {
        sfx.playMutedClick()
        spawnPooledFct("LANCE BEAM OFF", ship.x, ship.y - 26f, Color.parseColor("#A855F7"))
      }
      return
    }

    if (ship.specialWeaponCharges <= 0 || ship.specialWeaponType == "NONE") return
    if (isInsideSafeZone(ship.x, ship.y)) {
      if (ship.isLocalPlayer) triggerSafeZoneWeaponLockout(ship)
      return
    }
    if (ship.phaseShieldTimer > 0f) {
      ship.phaseShieldTimer = 0f
    }

    val activeType = RiftCodex.normalizeSpecialBuffId(ship.specialWeaponType)
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

    if (activeType == "TRACTOR_BEAM") {
      if (ship.flux <= 60f) {
        if (ship.isLocalPlayer) {
          onPrizeToast("INSUFFICIENT FLUX FOR TRACTOR BEAM")
        }
        return
      }
      ship.tractorBeamActive = true
      ship.tractorFromSecondary = false
      ship.tractorTargetShipId = ""
      ship.specialWeaponType = "TRACTOR_BEAM"
      ship.specialWeaponCharges = 1
      if (ship.isLocalPlayer) {
        currentLifeStats.shotsFired++
        buffVersion++
        sfx.playWarp()
        spawnPooledFct("TRACTOR WEB ENGAGED", ship.x, ship.y - 28f, Color.parseColor("#34D399"))
      }
      return
    }

    if (activeType == "LANCE_BEAM") {
      if (ship.flux <= 60f) {
        if (ship.isLocalPlayer) {
          onPrizeToast("INSUFFICIENT FLUX FOR LANCE BEAM")
        }
        return
      }
      val stacks = max(1, max(ship.specialWeaponStacks, ship.specialWeaponCharges)).coerceIn(1, 6)
      ship.lanceBeamActive = true
      ship.lanceFromSecondary = false
      ship.lanceActiveStacks = stacks
      ship.lanceTickTimer = 0.05f
      ship.lanceHitSparkActive = false
      ship.specialWeaponType = "LANCE_BEAM"
      ship.specialWeaponCharges = 1
      ship.specialWeaponStacks = stacks
      if (ship.isLocalPlayer) {
        currentLifeStats.shotsFired++
        buffVersion++
        sfx.playLaser()
        spawnPooledFct(
          if (stacks > 1) "LANCE BEAM x$stacks ENGAGED" else "LANCE BEAM ENGAGED",
          ship.x,
          ship.y - 28f,
          Color.parseColor("#A855F7"),
        )
      }
      return
    }

    if (activeType == "NOVA_RING") {
      val novaWaves = max(1, max(ship.specialWeaponStacks, ship.specialWeaponCharges)).coerceIn(1, 6)
      ship.specialWeaponCharges = 0
      ship.specialWeaponStacks = 0
      ship.specialWeaponType = "NONE"
      if (ship.isLocalPlayer) {
        currentLifeStats.shotsFired++
        buffVersion++
        if (novaWaves > 1) {
          spawnPooledFct("NOVA RING x$novaWaves WAVES", ship.x, ship.y - 28f, Color.parseColor("#F97316"))
        }
      }
      fireRadialBurstProjectiles(ship)
      ship.novaWavesRemaining = (novaWaves - 1).coerceAtLeast(0)
      ship.novaWaveTimer = 0.22f
      return
    }

    if (activeType == "STASIS_BARRIER") {
      val segCount = max(1, max(ship.specialWeaponStacks, ship.specialWeaponCharges)).coerceIn(1, 6)
      ship.specialWeaponCharges = 0
      ship.specialWeaponStacks = 0
      ship.specialWeaponType = "NONE"
      if (ship.isLocalPlayer) {
        currentLifeStats.shotsFired++
        buffVersion++
      }
      val span = RiftCalibrationState.stasisBarrierLength
      val barDur = RiftCalibrationState.stasisBarrierDuration.coerceAtLeast(5.0f)
      val accent = Color.parseColor("#00F5D4")
      if (segCount <= 1) {
        deployOrStackStasisBarrier(
          ship = ship,
          deployAngle = ship.angle,
          span = span,
          barDur = barDur,
          accent = accent,
          labelTitle = "STASIS BARRIER",
          perpOffsetPx = 0f,
          allowSameSpotStack = true,
          emitFeedback = true,
        )
      } else {
        val segStride = span + 16f
        for (idx in 0 until segCount) {
          val offset = (idx - (segCount - 1) * 0.5f) * segStride
          deployOrStackStasisBarrier(
            ship = ship,
            deployAngle = ship.angle,
            span = span,
            barDur = barDur,
            accent = accent,
            labelTitle = "STASIS BARRIER",
            perpOffsetPx = offset,
            allowSameSpotStack = false,
            emitFeedback = false,
          )
        }
        if (ship.isLocalPlayer) {
          sfx.playBounce()
          spawnPooledFct(
            "BARRIER WALL ($segCount SEGMENTS)",
            ship.x + cos(ship.angle) * 98f,
            ship.y + sin(ship.angle) * 98f - 26f,
            accent,
          )
        }
      }
      return
    }

    ship.specialWeaponCharges--
    if (ship.specialWeaponCharges <= 0) {
      ship.specialWeaponCharges = 0
      ship.specialWeaponStacks = 0
      ship.specialWeaponType = "NONE"
    } else {
      ship.specialWeaponStacks = min(ship.specialWeaponStacks, ship.specialWeaponCharges).coerceAtLeast(1)
    }
    if (ship.isLocalPlayer) {
      currentLifeStats.shotsFired++
      buffVersion++
    }

    when (activeType) {
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
      "PROX_SEEKER" -> {
        launchInfiniteProximityMissile(ship, aimAngle)
      }
      "CHAKRAM" -> {
        ship.weaponAngle = aimAngle
        launchChakramDisc(ship, aimAngle)
      }
      "SWARM_DRONES" -> {
        val customSpdMult = currentCustomGame?.speedMult ?: 1.0f
        val customDmgMult = currentCustomGame?.damageMult ?: 1.0f
        val droneSpd = 560f * customSpdMult
        val droneDmg = 210f * customDmgMult
        val droneLife = 6.5f
        val shipLbl = formatShipLabel(ship)
        val lockTgt = findMissileLockTarget(ship, aimAngle)
        for (idx in 0 until 5) {
          val fanOff = (idx - 2) * 0.26f
          val launchAng = aimAngle + fanOff
          val sx = ship.x + cos(launchAng) * 42f
          val sy = ship.y + sin(launchAng) * 42f
          if (!isInsideSafeZone(sx, sy)) {
            swarmDrones.add(
              SwarmDroneEntity(
                id = "swarm_${nextSwarmDroneId++}",
                ownerId = ship.id,
                ownerCallsign = ship.callsign,
                ownerSyndicateTag = ship.syndicateTag,
                ownerArmadaName = ship.armadaName,
                ownerIsHuman = ship.isHuman,
                ownerShipLabel = shipLbl,
                freq = ship.freq,
                x = sx,
                y = sy,
                vx = cos(launchAng) * droneSpd,
                vy = sin(launchAng) * droneSpd,
                angle = launchAng,
                life = droneLife,
                maxLife = droneLife,
                damage = droneDmg,
                zigPhase = idx * 1.25f,
                targetShipId = lockTgt?.id ?: "",
              )
            )
          }
        }
        shockwaves.add(ShockwaveEntity(ship.x, ship.y, 16f, 165f, Color.parseColor("#00F5D4")))
        if (ship.isLocalPlayer) {
          sfx.playWarp()
          addTrauma(0.22f)
          spawnPooledFct("SWARM DEPLOYED (5 DRONES)", ship.x, ship.y - 28f, Color.parseColor("#00F5D4"))
        }
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
          if ((bm.type == "MISSILE" || bm.type == "PROX_MISSILE") && bm.targetShipId == ship.id) {
            bm.targetShipId = decoyId
          }
        }
        for (dr in swarmDrones) {
          if (dr.targetShipId == ship.id) {
            dr.targetShipId = decoyId
          }
        }
        ship.missileLockWarningTimer = 0f
        if (ship.isLocalPlayer) {
          sfx.playWarp()
          spawnPooledFct("HOLO PHANTOM (${dur.toInt()}s)", ship.x, ship.y - 28f, Color.parseColor("#2DD4BF"))
        }
      }
      "HYPER_SURGE" -> {
        val dur = RiftCalibrationState.hyperSurgeDuration
        val mult = RiftCalibrationState.hyperSurgeSpeedMult
        ship.rocketBoostTimer = dur
        val boostSpd = ship.spec.topSpeed * mult * (currentCustomGame?.speedMult ?: 1.0f)
        ship.vx = cos(ship.angle) * boostSpd
        ship.vy = sin(ship.angle) * boostSpd
        shockwaves.add(ShockwaveEntity(ship.x, ship.y, 18f, 180f, Color.parseColor("#F43F5E")))
        if (ship.isLocalPlayer) {
          buffVersion++
          sfx.playWarp()
          addTrauma(0.28f)
          spawnPooledFct("HYPER SURGE (${String.format(java.util.Locale.US, "%.1fs", dur)})", ship.x, ship.y - 28f, Color.parseColor("#F43F5E"))
          notifyLocalLifecycleSync()
        }
      }
      else -> {
        val forged = RiftCodex.getBuffRegistryEntry(activeType) ?: return
        val accent = forged.strokeColorInt
        val customSpdMult = currentCustomGame?.speedMult ?: 1.0f
        val customDmgMult = currentCustomGame?.damageMult ?: 1.0f
        when (forged.specialEffect) {
          CustomBuffEffect.BARRIER -> {
            deployOrStackStasisBarrier(
              ship = ship,
              deployAngle = aimAngle,
              span = forged.blastRadius.coerceIn(90f, 320f),
              barDur = RiftCalibrationState.stasisBarrierDuration.coerceAtLeast(5.0f),
              accent = accent,
              labelTitle = forged.officialName,
            )
          }
          CustomBuffEffect.SPEED_BOOST -> {
            ship.rocketBoostTimer = forged.flightDuration
            val boostSpd = max(ship.spec.topSpeed * 1.65f, forged.projectileSpeed) * customSpdMult
            ship.vx = cos(aimAngle) * boostSpd
            ship.vy = sin(aimAngle) * boostSpd
            ship.angle = aimAngle
            shockwaves.add(ShockwaveEntity(ship.x, ship.y, 18f, 190f, accent))
            if (ship.isLocalPlayer) {
              buffVersion++
              sfx.playWarp()
              addTrauma(0.24f)
              spawnPooledFct(forged.officialName, ship.x, ship.y - 28f, accent)
              notifyLocalLifecycleSync()
            }
          }
          CustomBuffEffect.CLONE -> {
            val decoyAngle = aimAngle + 0.38f
            val speed = max(ship.spec.topSpeed * 0.95f, forged.projectileSpeed * 0.7f) * customSpdMult
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
              spawnPooledFct(forged.officialName, ship.x, ship.y - 28f, accent)
            }
          }
          else -> {
            val ox = ship.x + cos(aimAngle) * 50f
            val oy = ship.y + sin(aimAngle) * 50f
            if (!isInsideSafeZone(ox, oy)) {
              spawnBombWithMuzzleCheck(
                ship.x,
                ship.y,
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
                  vx = ship.vx * 0.2f + cos(aimAngle) * (forged.projectileSpeed * customSpdMult),
                  vy = ship.vy * 0.2f + sin(aimAngle) * (forged.projectileSpeed * customSpdMult),
                  damage = forged.damage * customDmgMult,
                  bouncesLeft = forged.wallBounces,
                  passWalls = false,
                  life = forged.flightDuration,
                  radius = 19f,
                  ownerShipLabel = formatShipLabel(ship),
                  weaponLabel = forged.officialName,
                  customColor = accent,
                  customEffect = forged.specialEffect,
                  customBlastRadius = forged.blastRadius,
                ),
              )
              if (ship.isLocalPlayer) {
                sfx.playBomb()
                addTrauma(0.20f)
                spawnPooledFct(forged.officialName, ship.x, ship.y - 28f, accent)
              }
            }
          }
        }
      }
    }
  }

  fun activateUtility(type: String) {
    if (
      localPlayer.respawnTimer > 0f ||
        isInLobbyStaging ||
        (isSkirmishRulesActive() && skirmishReadyCountdownSec > 0f)
    ) {
      return
    }
    if (
      (type == "SPECIAL" || RiftCodex.getBuffRegistryEntry(type) != null) &&
        (localPlayer.tractorBeamActive || localPlayer.lanceBeamActive)
    ) {
      activateShipSpecialBuff(localPlayer)
      return
    }
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
      activateShipSpecialBuff(localPlayer)
      return
    }
    if (type == "REPEL") {
      if (localPlayer.repels <= 0) return
      val rCost = RiftCalibrationState.repelCost
      if (localPlayer.flux <= rCost) {
        onPrizeToast("INSUFFICIENT FLUX FOR REPEL (${rCost.toInt()} FLUX)")
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
      // 3b. Repel ALL Swarm Drones
      for (dr in swarmDrones) {
        val dx = dr.x - localPlayer.x
        val dy = dr.y - localPlayer.y
        val dist = hypot(dx, dy)
        if (dist < repelRadius && dist > 1f) {
          val sp = max(620f, hypot(dr.vx, dr.vy))
          dr.vx = (dx / dist) * sp
          dr.vy = (dy / dist) * sp
          dr.angle = atan2(dr.vy, dr.vx)
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
   * Swept Segment-vs-AABB Anti-Bleed Raycast across all arena obstacles (`walls`) and deployed barriers (`brickWalls`).
   * Eliminates high-speed / point-blank projectile tunneling through stations, asteroids, and stacked barriers.
   */
  private fun segmentFirstWallHit(
    x0: Float,
    y0: Float,
    x1: Float,
    y1: Float,
    pad: Float = 0f,
  ): SweptWallImpact? {
    val dx = x1 - x0
    val dy = y1 - y0
    var bestHit: SweptWallImpact? = null
    var bestT = 1.0001f

    fun testBox(left: Float, top: Float, right: Float, bottom: Float, wallIdx: Int) {
      // Handle start-point already inside expanded box (point-blank muzzle against wall face)
      if (x0 >= left && x0 <= right && y0 >= top && y0 <= bottom) {
        val dLeft = x0 - left
        val dRight = right - x0
        val dTop = y0 - top
        val dBottom = bottom - y0
        val minEdge = min(min(dLeft, dRight), min(dTop, dBottom))
        val (hx, hy, nx, ny) =
          when (minEdge) {
            dLeft -> arrayOf(left, y0, -1f, 0f)
            dRight -> arrayOf(right, y0, 1f, 0f)
            dTop -> arrayOf(x0, top, 0f, -1f)
            else -> arrayOf(x0, bottom, 0f, 1f)
          }
        if (0f < bestT) {
          bestT = 0f
          bestHit = SweptWallImpact(0f, hx, hy, nx, ny, wallIdx)
        }
        return
      }

      var tMin = 0f
      var tMax = 1f
      var hitNx = 0f
      var hitNy = 0f

      if (kotlin.math.abs(dx) < 1e-6f) {
        if (x0 < left || x0 > right) return
      } else {
        val invDx = 1f / dx
        var t1 = (left - x0) * invDx
        var t2 = (right - x0) * invDx
        var n1x = -1f
        if (t1 > t2) {
          val tmp = t1
          t1 = t2
          t2 = tmp
          n1x = 1f
        }
        if (t1 > tMin) {
          tMin = t1
          hitNx = n1x
          hitNy = 0f
        }
        if (t2 < tMax) tMax = t2
        if (tMin > tMax) return
      }

      if (kotlin.math.abs(dy) < 1e-6f) {
        if (y0 < top || y0 > bottom) return
      } else {
        val invDy = 1f / dy
        var t1 = (top - y0) * invDy
        var t2 = (bottom - y0) * invDy
        var n1y = -1f
        if (t1 > t2) {
          val tmp = t1
          t1 = t2
          t2 = tmp
          n1y = 1f
        }
        if (t1 > tMin) {
          tMin = t1
          hitNx = 0f
          hitNy = n1y
        }
        if (t2 < tMax) tMax = t2
        if (tMin > tMax) return
      }

      if (tMin in 0f..1f && tMin < bestT) {
        if (hitNx == 0f && hitNy == 0f) {
          hitNx = if (dx >= 0f) -1f else 1f
        }
        bestT = tMin
        bestHit =
          SweptWallImpact(
            t = tMin,
            hitX = x0 + dx * tMin,
            hitY = y0 + dy * tMin,
            normalX = hitNx,
            normalY = hitNy,
            wallIndex = wallIdx,
          )
      }
    }

    for (idx in walls.indices) {
      val w = walls[idx]
      testBox(w.x - pad, w.y - pad, w.x + w.w + pad, w.y + w.h + pad, idx)
    }
    for (bw in brickWalls) {
      testBox(bw.x - pad, bw.y - pad, bw.x + bw.w + pad, bw.y + bw.h + pad, -1)
    }
    return bestHit
  }

  /**
   * Increments `station.stationHitCounter` ONLY on the struck station (zero immediate per-shot
   * retaliation and zero neighbor-station cascade fire), unleashing its `16`-bolt `360°`
   * Overload Defense Nova strictly on every `10` hits against that station!
   */
  fun triggerStationAttackResponse(station: WallRect, attacker: ShipEntity?, impactAngle: Float) {
    station.lastHitAngle = impactAngle
    station.hitFlashTimer = 0.22f
    val triggerShipId = attacker?.id?.takeIf { it.isNotEmpty() } ?: station.lastAttackerShipId
    if (triggerShipId.isNotEmpty()) {
      station.lastAttackerShipId = triggerShipId
    }
    station.stationHitCounter++

    val stCx = station.x + station.w * 0.5f
    val stCy = station.y + station.h * 0.5f
    val customSpdMult = currentCustomGame?.speedMult ?: 1.0f
    val customDmgMult = currentCustomGame?.damageMult ?: 1.0f

    if (station.stationHitCounter >= 10) {
      station.stationHitCounter = 0
      val novaSpeed = 820f * customSpdMult
      val novaDamage = 165f * customDmgMult
      val exitR = hypot(station.w * 0.5f, station.h * 0.5f) + 10f
      for (k in 0 until 16) {
        val a = (PI.toFloat() * 2f * k) / 16f
        val sx = stCx + cos(a) * exitR
        val sy = stCy + sin(a) * exitR
        if (isInsideSafeZone(sx, sy)) continue
        bullets.add(
          BulletEntity(
            ownerId = "station_defense",
            ownerShipId = 0,
            ownerCallsign = "STATION DEFENSE",
            ownerSyndicateTag = "SECTOR",
            ownerArmadaName = "",
            ownerIsHuman = false,
            freq = -999,
            x = sx,
            y = sy,
            vx = cos(a) * novaSpeed,
            vy = sin(a) * novaSpeed,
            damage = novaDamage,
            bouncesLeft = 1,
            life = 1.85f,
            color = Color.parseColor("#FACC15"),
            isHeavyBurst = true,
            ownerShipLabel = "Orbital Station",
            weaponLabel = "Station Defense Nova",
            triggeredByShipId = triggerShipId,
          )
        )
      }
      shockwaves.add(ShockwaveEntity(stCx, stCy, 24f, 260f, Color.parseColor("#FACC15")))
      shockwaves.add(ShockwaveEntity(stCx, stCy, 14f, 180f, Color.parseColor("#EF4444")))
      if (hypot(stCx - localPlayer.x, stCy - localPlayer.y) < 1400f) {
        sfx.playLaser()
        addTrauma(0.24f)
      }
      spawnPooledFct(
        text = "STATION DEFENSE NOVA!",
        x = stCx,
        y = stCy - 34f,
        color = Color.parseColor("#FACC15"),
        maxLife = 1.2f,
      )
    }
  }

  /**
   * Applies projectile impact telemetry to arena obstacles:
   * - `CRYSTAL_ASTEROID` (`2,400 HP`): shatters at `0 HP`, drops a guaranteed `GreenCrate`, and respawns after `20.0s`.
   * - `ORBITAL_STATION` / `ASTEROID_STATION`: triggers directional hit ripple, retaliatory 5-bolt salvo, neighbor cascade, and 10-hit `360°` Overload Defense Nova!
   */
  private fun onObstacleProjectileHit(
    wallIndex: Int,
    damage: Float,
    hitX: Float,
    hitY: Float,
    attackerId: String = "",
    isStationProjectile: Boolean = false,
  ) {
    if (wallIndex !in walls.indices) return
    val w = walls[wallIndex]
    val cx = w.x + w.w * 0.5f
    val cy = w.y + w.h * 0.5f
    val impactAngle = atan2(hitY - cy, hitX - cx)
    w.lastHitAngle = impactAngle

    if (w.style == SpaceObstacleStyle.CRYSTAL_ASTEROID) {
      w.hitFlashTimer = 0.22f
      w.hp -= damage.coerceAtLeast(1f)
      if (w.hp <= 0f) {
        pendingAsteroidRespawns.add(
          PendingAsteroidRespawn(
            x = w.x,
            y = w.y,
            w = w.w,
            h = w.h,
            variantSeed = w.variantSeed,
            timerSec = 20.0f,
          )
        )
        walls.removeAt(wallIndex)
        val clearDrop = resolveClearCrateDropPosition(cx, cy)
        greens.add(
          GreenCrate(
            x = clearDrop.first,
            y = clearDrop.second,
            spinPhase = Random.nextFloat() * (PI.toFloat() * 2f),
            buffType = rollRandomCrateBuffType(),
          )
        )
        shockwaves.add(ShockwaveEntity(cx, cy, 18f, 220f, Color.parseColor("#A855F7")))
        shockwaves.add(ShockwaveEntity(cx, cy, 12f, 150f, Color.parseColor("#00F5D4")))
        spawnExplosion(cx, cy, Color.parseColor("#C084FC"))
        if (hypot(cx - localPlayer.x, cy - localPlayer.y) < 1300f) {
          sfx.playExplosion()
          addTrauma(0.26f)
        }
        spawnPooledFct(
          text = "ASTEROID SHATTERED • SUPPLY DROP",
          x = cx,
          y = cy - 28f,
          color = Color.parseColor("#C084FC"),
          maxLife = 1.25f,
        )
        bakeStaticRadarBitmap()
      }
    } else if (
      w.style == SpaceObstacleStyle.ORBITAL_STATION ||
        w.style == SpaceObstacleStyle.ASTEROID_STATION
    ) {
      if (isStationProjectile) {
        w.hitFlashTimer = max(w.hitFlashTimer, 0.14f)
      } else {
        val attacker = if (attackerId.isNotEmpty()) ships.firstOrNull { it.id == attackerId } else null
        triggerStationAttackResponse(w, attacker, impactAngle)
      }
    } else {
      w.hitFlashTimer = 0.22f
    }
  }

  private fun applyExplosionToObstacles(
    blastX: Float,
    blastY: Float,
    blastRadius: Float,
    baseDamage: Float,
    attackerId: String,
  ) {
    if (blastRadius <= 0f || walls.isEmpty()) return
    for (wIdx in walls.indices.reversed()) {
      val w = walls[wIdx]
      if (w.style == SpaceObstacleStyle.PERMANENT_BARRIER) continue
      val nearX = blastX.coerceIn(w.x, w.x + w.w)
      val nearY = blastY.coerceIn(w.y, w.y + w.h)
      val dist = hypot(blastX - nearX, blastY - nearY)
      if (dist <= blastRadius) {
        val falloff = (1f - (dist / blastRadius.coerceAtLeast(1f)) * 0.45f).coerceIn(0.45f, 1f)
        onObstacleProjectileHit(
          wallIndex = wIdx,
          damage = baseDamage * falloff,
          hitX = nearX,
          hitY = nearY,
          attackerId = attackerId,
          isStationProjectile = false,
        )
      }
    }
  }

  private fun spawnBulletWithMuzzleCheck(originX: Float, originY: Float, bullet: BulletEntity) {
    val hit = segmentFirstWallHit(originX, originY, bullet.x, bullet.y, pad = 1.5f)
    if (hit == null) {
      bullets.add(bullet)
      return
    }
    if (hit.wallIndex >= 0) {
      onObstacleProjectileHit(
        wallIndex = hit.wallIndex,
        damage = bullet.damage,
        hitX = hit.hitX,
        hitY = hit.hitY,
        attackerId = bullet.ownerId,
        isStationProjectile = isStationDefenseOwner(bullet.ownerId),
      )
    }
    if (bullet.bouncesLeft > 0) {
      bullet.bouncesLeft--
      bullet.x = hit.hitX + hit.normalX * 3f
      bullet.y = hit.hitY + hit.normalY * 3f
      val dot = bullet.vx * hit.normalX + bullet.vy * hit.normalY
      if (dot < 0f) {
        bullet.vx -= 2f * dot * hit.normalX
        bullet.vy -= 2f * dot * hit.normalY
      }
      bullets.add(bullet)
    } else if (particles.size < 130) {
      particles.add(
        ParticleEntity(
          x = hit.hitX,
          y = hit.hitY,
          vx = hit.normalX * 80f,
          vy = hit.normalY * 80f,
          life = 0.18f,
          maxLife = 0.18f,
          size = 3.2f,
          color = bullet.color,
        )
      )
    }
  }

  private fun spawnBombWithMuzzleCheck(originX: Float, originY: Float, bomb: BombEntity) {
    if (bomb.passWalls) {
      bombs.add(bomb)
      return
    }
    val hit = segmentFirstWallHit(originX, originY, bomb.x, bomb.y, pad = 3.5f)
    if (hit == null) {
      bombs.add(bomb)
      return
    }
    if (hit.wallIndex >= 0) {
      onObstacleProjectileHit(
        wallIndex = hit.wallIndex,
        damage = bomb.damage,
        hitX = hit.hitX,
        hitY = hit.hitY,
        attackerId = bomb.ownerId,
      )
    }
    if (bomb.bouncesLeft > 0) {
      bomb.bouncesLeft--
      val rest = if (bomb.type == "MISSILE" || bomb.type == "BOUNCE" || bomb.type == "CUSTOM_ORB") 1.0f else 0.85f
      bomb.x = hit.hitX + hit.normalX * 4f
      bomb.y = hit.hitY + hit.normalY * 4f
      val dot = bomb.vx * hit.normalX + bomb.vy * hit.normalY
      if (dot < 0f) {
        bomb.vx = (bomb.vx - 2f * dot * hit.normalX) * rest
        bomb.vy = (bomb.vy - 2f * dot * hit.normalY) * rest
      }
      bombs.add(bomb)
    } else {
      bomb.x = hit.hitX + hit.normalX * 2f
      bomb.y = hit.hitY + hit.normalY * 2f
      if (bomb.type == "PROX") spawnProximityFlakShrapnel(bomb)
      spawnExplosion(
        bomb.x,
        bomb.y,
        when (bomb.type) {
          "CUSTOM_ORB" -> if (bomb.customColor != 0) bomb.customColor else Color.parseColor("#22D3EE")
          "EMP" -> Color.parseColor("#B388FF")
          "MISSILE" -> Color.parseColor("#EF4444")
          else -> Color.parseColor("#F59E0B")
        },
      )
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

      // Check circular outer sector containment perimeter ray intersection
      val fxPerim = curX - arenaCenterX
      val fyPerim = curY - arenaCenterY
      val bPerim = fxPerim * dirX + fyPerim * dirY
      val cPerim = fxPerim * fxPerim + fyPerim * fyPerim - arenaRadius * arenaRadius
      val discPerim = bPerim * bPerim - cPerim
      if (discPerim >= 0f) {
        val tExit = -bPerim + sqrt(discPerim)
        if (tExit > 1.5f && tExit < bestT) {
          val hx = curX + dirX * tExit
          val hy = curY + dirY * tExit
          val invR = 1f / arenaRadius.coerceAtLeast(1f)
          bestT = tExit
          closestHit =
            RaycastHit(
              t = tExit,
              hitX = hx,
              hitY = hy,
              normalX = (arenaCenterX - hx) * invR,
              normalY = (arenaCenterY - hy) * invR,
            )
        }
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

  private fun computePilotRankTitleForBounty(bounty: Int, isHuman: Boolean): String {
    if (!isHuman) return "AI BOT"
    return when {
      bounty >= 1000 -> "SOVEREIGN"
      bounty >= 600 -> "APEX WARLORD"
      bounty >= 350 -> "ACE COMMANDER"
      bounty >= 200 -> "VANGUARD"
      else -> "PILOT"
    }
  }

  private fun triggerWallBounceReaction(
    ship: ShipEntity,
    nx: Float,
    ny: Float,
    preVx: Float,
    preVy: Float,
  ) {
    val totalSpeed = hypot(preVx, preVy)
    if (totalSpeed < 22f) return
    val impactSpeed = kotlin.math.abs(preVx * nx + preVy * ny)
    val tangentDot = preVx * (-ny) + preVy * nx
    val glancingCos = kotlin.math.abs(tangentDot) / totalSpeed.coerceAtLeast(1f)
    val cross = cos(ship.angle) * ny - sin(ship.angle) * nx

    // Reaction 1: Rare High-Speed Glancing Stunt Barrel Roll (35% roll chance, 3.5s cooldown)
    if (
      totalSpeed > 360f &&
        glancingCos > 0.58f &&
        ship.lastBarrelRollCooldown <= 0f &&
        ship.barrelRollTimer <= 0f &&
        Random.nextFloat() < 0.35f
    ) {
      ship.barrelRollDuration = 0.48f
      ship.barrelRollTimer = 0.48f
      ship.barrelRollDir = if (cross >= 0f) 1f else -1f
      ship.lastBarrelRollCooldown = 3.5f
      ship.hullImpactType = 1
      return
    }

    // Reaction 2: Hard Direct Impact (> 200 px/s normal component)
    if (impactSpeed > 200f) {
      ship.hullImpactType = 2
      ship.hullImpactJoltTimer = 0.22f
      ship.hullImpactNormalX = nx
      ship.hullImpactNormalY = ny
      shieldRipples.add(
        ShieldRippleEntity(
          shipId = ship.id,
          impactAngle = atan2(-ny, -nx),
          intensity = 1.0f,
          life = 0.30f,
        )
      )
      if (particles.size < 160) {
        val baseAng = atan2(ny, nx)
        for (i in 0 until 10) {
          val sp = 95f + Random.nextFloat() * 190f
          val a = baseAng + (Random.nextFloat() - 0.5f) * 1.45f
          particles.add(
            ParticleEntity(
              x = ship.x - nx * (ship.radius * 0.85f),
              y = ship.y - ny * (ship.radius * 0.85f),
              vx = cos(a) * sp,
              vy = sin(a) * sp,
              life = 0.28f + Random.nextFloat() * 0.16f,
              maxLife = 0.44f,
              size = 3.8f + Random.nextFloat() * 2.4f,
              color = if (i % 2 == 0) Color.parseColor("#00F5D4") else Color.parseColor("#FACC15"),
            )
          )
        }
      }
      if (ship.isLocalPlayer) {
        addTrauma(0.18f)
      }
      return
    }

    // Reaction 3: Medium Scrape (105..200 px/s)
    if (impactSpeed >= 105f) {
      ship.hullImpactType = 3
      ship.hullImpactJoltTimer = 0.14f
      ship.hullImpactNormalX = nx
      ship.hullImpactNormalY = ny
      val rollSnap = if (cross >= 0f) 0.55f else -0.55f
      ship.visualBankRoll = (ship.visualBankRoll + rollSnap).coerceIn(-1f, 1f)
      if (particles.size < 160) {
        val baseAng = atan2(ny, nx)
        for (i in 0 until 5) {
          val sp = 65f + Random.nextFloat() * 130f
          val a = baseAng + (Random.nextFloat() - 0.5f) * 1.6f
          particles.add(
            ParticleEntity(
              x = ship.x - nx * (ship.radius * 0.85f),
              y = ship.y - ny * (ship.radius * 0.85f),
              vx = cos(a) * sp,
              vy = sin(a) * sp,
              life = 0.22f,
              maxLife = 0.22f,
              size = 3.2f,
              color = Color.parseColor("#38BDF8"),
            )
          )
        }
      }
      return
    }

    // Reaction 4: Light Graze (< 105 px/s)
    ship.hullImpactType = 4
    shieldRipples.add(
      ShieldRippleEntity(
        shipId = ship.id,
        impactAngle = atan2(-ny, -nx),
        intensity = 0.45f,
        life = 0.18f,
      )
    )
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

      val preVx = ship.vx
      val preVy = ship.vy
      val dot = preVx * nx + preVy * ny
      if (dot < 0f) {
        val elast = RiftCalibrationState.wallElasticity
        ship.vx = (preVx - 2f * dot * nx) * elast
        ship.vy = (preVy - 2f * dot * ny) * elast
        triggerWallBounceReaction(ship, nx, ny, preVx, preVy)
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
    // Circular Sector Containment Ring Elastic Bounce
    val dxC = ship.x - arenaCenterX
    val dyC = ship.y - arenaCenterY
    val distC = hypot(dxC, dyC)
    val maxShipR = (arenaRadius - ship.radius).coerceAtLeast(400f)
    if (distC > maxShipR) {
      val outNx = dxC / distC.coerceAtLeast(0.001f)
      val outNy = dyC / distC.coerceAtLeast(0.001f)
      ship.x = arenaCenterX + outNx * maxShipR
      ship.y = arenaCenterY + outNy * maxShipR
      val preVx = ship.vx
      val preVy = ship.vy
      val dotOut = preVx * outNx + preVy * outNy
      if (dotOut > 0f) {
        val elast = RiftCalibrationState.wallElasticity
        ship.vx = (preVx - 2f * dotOut * outNx) * elast
        ship.vy = (preVy - 2f * dotOut * outNy) * elast
        triggerWallBounceReaction(ship, -outNx, -outNy, preVx, preVy)
        spawnDropletReactionBurst(
          originX = arenaCenterX + outNx * arenaRadius,
          originY = arenaCenterY + outNy * arenaRadius,
          targetOrWakeX = ship.x + ship.vx * 0.16f,
          targetOrWakeY = ship.y + ship.vy * 0.16f,
          pullInward = false,
          crystalCount = 4,
          filamentCount = 3,
          outerColor = Color.rgb(168, 85, 247),
          midColor = Color.rgb(0, 245, 212),
          coreColor = Color.rgb(250, 204, 21),
          baseCrystalRadius = 6.2f,
          durationSec = 0.58f,
        )
        if (ship.isLocalPlayer && hypot(ship.vx, ship.vy) > 45f) {
          sfx.playBounce()
        }
      }
    }
  }

  private fun spawnExplosion(x: Float, y: Float, color: Int) {
    shockwaves.add(ShockwaveEntity(x, y, 16f, 290f, color))
    if (particles.size > 150) {
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

  private fun triggerMultiStageShipExplosion(
    target: ShipEntity,
    attacker: ShipEntity?,
    weaponLabel: String,
    fallbackKillerName: String = "ORDNANCE",
  ) {
    target.deathLingerTimer = 1.0f
    target.respawnTimer = max(target.respawnTimer, 1.35f)
    val finisher = attacker?.equippedKillFinisher ?: 0
    val (primaryCol, secondaryCol, trimCol) =
      when (finisher) {
        1 -> Triple(Color.parseColor("#A855F7"), Color.parseColor("#00F5D4"), Color.parseColor("#C084FC"))
        2 -> Triple(Color.parseColor("#FACC15"), Color.parseColor("#FEF08A"), Color.parseColor("#F59E0B"))
        3 -> Triple(Color.parseColor("#38BDF8"), Color.parseColor("#00F5D4"), Color.parseColor("#7DD3FC"))
        else -> Triple(Color.parseColor("#F97316"), Color.parseColor("#FACC15"), Color.parseColor("#FB923C"))
      }

    // 3 staggered concentric shockwave rings
    shockwaves.add(ShockwaveEntity(target.x, target.y, 12f, 155f, Color.WHITE))
    shockwaves.add(ShockwaveEntity(target.x, target.y, 24f, 360f, primaryCol))
    shockwaves.add(ShockwaveEntity(target.x, target.y, 6f, 480f, secondaryCol))

    // 7 spinning metallic hull debris chunks with glowing ember trails
    while (shipDebrisChunks.size > 35) {
      shipDebrisChunks.removeAt(0)
    }
    for (i in 0 until 7) {
      val ang = (i / 7f) * (PI.toFloat() * 2f) + (Random.nextFloat() - 0.5f) * 0.55f
      val sp = 95f + Random.nextFloat() * 195f
      val spin = (if (i % 2 == 0) 1f else -1f) * (4.2f + Random.nextFloat() * 6.8f)
      val life = 1.05f + Random.nextFloat() * 0.25f
      shipDebrisChunks.add(
        ShipDebrisChunk(
          x = target.x + cos(ang) * 10f,
          y = target.y + sin(ang) * 10f,
          vx = cos(ang) * sp,
          vy = sin(ang) * sp,
          angle = Random.nextFloat() * PI.toFloat() * 2f,
          angVel = spin,
          size = 11f + Random.nextFloat() * 8f,
          life = life,
          maxLife = life,
          trimColor = trimCol,
        )
      )
    }

    // 48 high-velocity plasma & reactor fire particles
    while (particles.size > 140) {
      particles.removeAt(0)
    }
    for (i in 0 until 48) {
      val a = Random.nextFloat() * PI.toFloat() * 2f
      val sp = 75f + Random.nextFloat() * 420f
      val pLife = 0.42f + Random.nextFloat() * 0.62f
      val pCol =
        when (i % 4) {
          0 -> Color.WHITE
          1 -> primaryCol
          2 -> secondaryCol
          else -> Color.parseColor("#EF4444")
        }
      particles.add(
        ParticleEntity(
          x = target.x,
          y = target.y,
          vx = cos(a) * sp,
          vy = sin(a) * sp,
          life = pLife,
          maxLife = pLife,
          size = 4.2f + Random.nextFloat() * 5.6f,
          color = pCol,
        )
      )
    }

    if (target.isLocalPlayer) {
      lastKillerCallsign = attacker?.callsign ?: fallbackKillerName
      lastKillerShipId = attacker?.shipId ?: target.shipId
      lastKillerRankTitle =
        if (attacker == null) "SECTOR HAZARD"
        else computePilotRankTitleForBounty(attacker.bounty, attacker.isHuman)
      lastKillerWeaponLabel = weaponLabel.ifBlank { "Primary Blaster" }
      lastKillerFinisherStyle = finisher
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
    stationTriggeredByShipId: String = "",
    forceUnstableRiftInstaKill: Boolean = false,
  ) {
    if (target.respawnTimer > 0f) return
    if (isInLobbyStaging && target.isLocalPlayer) return
    if (isInsideSafeZone(target.x, target.y)) return

    // Taking damage immediately uncloaks a cloaked ship without firing EMP (2.0s short lockout)
    if (target.isCloaked) {
      target.isCloaked = false
      if (target.spec.bombType == "CLOAK_EMP") {
        target.bombTimer = max(target.bombTimer, 2.0f)
      }
      if (target.isLocalPlayer) {
        buffVersion++
        onPrizeToast("STEALTH CLOAK BROKEN BY INCOMING FIRE")
      }
    }

    val phaseDodged = !forceUnstableRiftInstaKill && target.phaseDashTimer > 0f && !isEmp
    val shieldAbsorbed = !forceUnstableRiftInstaKill && target.phaseShieldTimer > 0f
    // Disruptor (#6) EMP Cap: calibrated % of target maxFlux (default 25%), capped at 850 Flux, calibrated recharge lock (default 2.0s)
    val empFluxDrain = if (isEmp) min(850f, target.maxFlux * RiftCalibrationState.empFluxDrainPct) else 0f
    val baseCombined = max(rawDamage, empFluxDrain)
    val effectiveDamage =
      when {
        forceUnstableRiftInstaKill -> max(rawDamage, target.flux + target.maxFlux + 9999f)
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
          text = "PHASE EVADED",
          x = target.x,
          y = target.y - 20f,
          color = Color.parseColor("#00F5D4"),
        )
      }
      return
    }

    if (isEmp) {
      val freezeDur = RiftCalibrationState.empFreezeSec
      target.empTimer = max(target.empTimer, freezeDur)
      if (target.isLocalPlayer) {
        onPrizeToast("HIT BY EMP PULSE! -${effectiveDamage.toInt()} FLUX & ${String.format(java.util.Locale.US, "%.1fs", freezeDur)} RECHARGE LOCK")
      }
    }

    // Track live CurrentLifeStats damage, accuracy & per-ship Resonance combat milestones
    if (attackerId == localPlayer.id && !target.isLocalPlayer) {
      currentLifeStats.shotsHit++
      currentLifeStats.damageDealt += effectiveDamage
      if (effectiveDamage >= 1f) {
        onDailyDirectiveEvent(DailyDirectiveMetric.DAMAGE_DEALT, effectiveDamage.roundToInt().coerceAtLeast(1), false)
        if (!isEphemeralPracticeRift && currentCustomGame == null) {
          combatDamageResonanceCarry += effectiveDamage
          while (combatDamageResonanceCarry >= 1000f) {
            combatDamageResonanceCarry -= 1000f
            adjustLocalResonance(2, "COMBAT DMG")
          }
        }
      }
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
        text = if (isEmp) "EMP -${effectiveDamage.toInt()}" else "-${effectiveDamage.toInt()}",
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
      target.respawnTimer = max(1.35f, RiftCalibrationState.respawnDelaySec)
      target.deaths++
      if (target.isHuman && !target.isLocalPlayer) {
        target.netLocalKillGraceSec = 0.85f
        target.netRespawnHoldSec = 0f
      }
      target.isCloaked = false
      target.sniperOverchargeReady = false
      target.rocketBoostTimer = 0f
      target.phaseDashTimer = 0f
      target.speedOverchargeStacks = 0
      target.thrustOverchargeStacks = 0
      target.rechargeOverchargeStacks = 0
      target.turnOverchargeStacks = 0
      target.maxFluxOverchargeStacks = 0
      target.maxFlux = target.spec.maxFlux * (currentCustomGame?.healthMult ?: 1.0f)
      target.hasBouncingBulletsUpgrade = false

      if (target.isLocalPlayer || hypot(target.x - localPlayer.x, target.y - localPlayer.y) < 1100f) {
        sfx.playExplosion()
      }
      if (attackerId == localPlayer.id || target.isLocalPlayer) {
        triggerHitStop(2)
        addTrauma(0.55f)
      }

      val victimPreDeathBounty = target.bounty
      val attacker = ships.find { it.id == attackerId }
      val attackerPreKillBounty = attacker?.bounty ?: 100
      val isUnstableRiftKill = forceUnstableRiftInstaKill || attackerId == "unstable_rift"
      val isStationKill = isStationDefenseOwner(attackerId)
      val isEnemyStationTriggerKill =
        isStationKill && stationTriggeredByShipId.isNotEmpty() && stationTriggeredByShipId != target.id
      val isSelfStationTriggerDeath =
        isStationKill && stationTriggeredByShipId.isNotEmpty() && stationTriggeredByShipId == target.id

      if (attacker != null) {
        attacker.kills++
        attacker.bounty += bountyAward
      }

      // Station Counter-Attack Trigger Credit: 10% of normal kill Bounty & Live Resonance
      val triggerShip =
        if (isEnemyStationTriggerKill) ships.find { it.id == stationTriggeredByShipId } else null
      val stationAssistBounty =
        if (isEnemyStationTriggerKill) max(1, (bountyAward * 0.10f).roundToInt()) else 0
      if (triggerShip != null && stationAssistBounty > 0) {
        triggerShip.bounty += stationAssistBounty
      }

      // Self-Triggered Station Counter-Attack Death Penalty: deduct Bounty now & on next respawn
      val selfStationBountyPenalty =
        if (isSelfStationTriggerDeath) max(25, (victimPreDeathBounty * 0.15f).roundToInt()) else 0
      if (isSelfStationTriggerDeath) {
        target.bounty = max(50, target.bounty - selfStationBountyPenalty)
        target.pendingRespawnBountyPenalty = min(50, selfStationBountyPenalty)
      }

      val explosionFallbackName =
        when {
          isUnstableRiftKill -> "UNSTABLE RIFT"
          isStationKill -> "STATION DEFENSE"
          else -> "ORDNANCE"
        }
      triggerMultiStageShipExplosion(
        target = target,
        attacker = attacker,
        weaponLabel = weaponLabel,
        fallbackKillerName = explosionFallbackName,
      )

      when {
        isUnstableRiftKill ->
          spawnPooledFct(
            text = "UNSTABLE RIFT // -25 RES",
            x = target.x,
            y = target.y - 26f,
            color = Color.parseColor("#FACC15"),
            maxLife = 1.35f,
          )
        isSelfStationTriggerDeath ->
          spawnPooledFct(
            text = "-${selfStationBountyPenalty} RES // STATION BACKFIRE",
            x = target.x,
            y = target.y - 26f,
            color = Color.parseColor("#EF4444"),
            maxLife = 1.30f,
          )
        isEnemyStationTriggerKill && stationAssistBounty > 0 ->
          spawnPooledFct(
            text = "+$stationAssistBounty RES (10% STATION)",
            x = target.x,
            y = target.y - 26f,
            color = Color.parseColor("#38BDF8"),
            maxLife = 1.25f,
          )
        else ->
          spawnPooledFct(
            text = if (wasHvt) "+$bountyAward TOP RES!" else "+$bountyAward RES",
            x = target.x,
            y = target.y - 26f,
            color = if (wasHvt) Color.parseColor("#F59E0B") else Color.parseColor("#10B981"),
            maxLife = 1.15f,
          )
      }

      pushKillFeedEntry(
        killerTag =
          when {
            attacker != null ->
              if (attacker.armadaName.isNotEmpty()) attacker.armadaName else attacker.syndicateTag
            triggerShip != null ->
              if (triggerShip.armadaName.isNotEmpty()) triggerShip.armadaName else triggerShip.syndicateTag
            else -> "SECTOR"
          },
        killerName =
          when {
            attacker != null -> attacker.callsign
            triggerShip != null -> "${triggerShip.callsign} [STN]"
            isUnstableRiftKill -> "UNSTABLE RIFT"
            isStationKill -> "STATION DEFENSE"
            else -> "ORDNANCE"
          },
        killerIsLocal = attacker?.isLocalPlayer == true || triggerShip?.isLocalPlayer == true,
        killerIsAllied =
          (attacker != null && areAllied(localPlayer, attacker)) ||
            (triggerShip != null && areAllied(localPlayer, triggerShip)),
        victimTag =
          if (target.armadaName.isNotEmpty()) target.armadaName else target.syndicateTag,
        victimName = target.callsign,
        victimIsLocal = target.isLocalPlayer,
        victimIsAllied = areAllied(localPlayer, target),
        victimBounty = victimPreDeathBounty,
        bountyAwarded = if (isEnemyStationTriggerKill) stationAssistBounty else bountyAward,
        wasHvt = wasHvt,
      )

      if (!isEphemeralPracticeRift && isEnemyStationTriggerKill && triggerShip?.isLocalPlayer == true) {
        allTimeBountyPeak = max(allTimeBountyPeak, localPlayer.bounty.toLong())
        currentLifeStats.bountyReached = max(currentLifeStats.bountyReached, localPlayer.bounty)
        onDailyDirectiveEvent(DailyDirectiveMetric.BOUNTY_PEAK_IN_LIFE, localPlayer.bounty, true)
        val normalKillRes =
          if (target.isHuman) {
            currentLifeHadPvp = true
            pvpBountyPeak = max(pvpBountyPeak, localPlayer.bounty.toLong())
            if (wasHvt) 28 else 18
          } else {
            when {
              liveResonance < 250 -> 6
              liveResonance < 500 -> 3
              else -> 10
            }
          }
        val stationResCredit = max(1, (normalKillRes * 0.10f).roundToInt())
        adjustLocalResonance(stationResCredit, "10% STATION TRIGGER", showFctEvenIfRespawning = true)
        awardPilotXp(
          bountyEarnedXp = stationAssistBounty * 2f,
          bountyKilledXp = victimPreDeathBounty * 0.05f,
          notifyChange = false,
        )
        onPrizeToast(
          "STATION TRIGGER KILL CREDIT (10%) // +$stationAssistBounty RES (+$stationResCredit LIVE RES)"
        )
        onStatsUpdated()
      }

      if (!isEphemeralPracticeRift && attackerId == localPlayer.id) {
        allTimeKills++
        onDailyDirectiveEvent(DailyDirectiveMetric.KILLS_EARNED, 1, false)
        if (wasHvt) {
          hvtEliminations++
          currentLifeStats.hvtShutdowns++
          onDailyDirectiveEvent(DailyDirectiveMetric.HVT_SHUTDOWNS, 1, false)
        }
        allTimeBountyPeak = max(allTimeBountyPeak, localPlayer.bounty.toLong())
        currentLifeStats.bountyReached = max(currentLifeStats.bountyReached, localPlayer.bounty)
        onDailyDirectiveEvent(DailyDirectiveMetric.BOUNTY_PEAK_IN_LIFE, localPlayer.bounty, true)
        if (target.isHuman) {
          pvpKills++
          currentLifeStats.pvpKills++
          currentLifeHadPvp = true
          pvpBountyPeak = max(pvpBountyPeak, localPlayer.bounty.toLong())
          adjustLocalResonance(if (wasHvt) 28 else 18, if (wasHvt) "HIGH RES SHUTDOWN" else "PVP KILL")
        } else {
          currentLifeStats.botKills++
          val botResGain =
            when {
              liveResonance < 250 -> 6
              liveResonance < 500 -> 4
              else -> 2
            }
          adjustLocalResonance(botResGain, "COMBAT KILL")
        }
        awardPilotXp(
          bountyEarnedXp = bountyAward * 2f,
          bountyKilledXp = victimPreDeathBounty * 0.15f,
          notifyChange = false,
        )
        onArmadaTierKill(getCurrentArmadaSize(), localPlayer.bounty.toLong())
        onSyndicateContribution(1L, localPlayer.bounty.toLong(), 0L)
        onStatsUpdated()
      } else if (!isEphemeralPracticeRift && target.isLocalPlayer) {
        allTimeDeaths++
        if (attackerIsHuman) pvpDeaths++
        when {
          isUnstableRiftKill -> {
            if (currentCustomGame == null) {
              adjustLocalResonance(-25, "UNSTABLE RIFT", showFctEvenIfRespawning = true)
            } else {
              liveResonance = (liveResonance - 25).coerceAtLeast(0)
            }
            onPrizeToast("KILLED OUTSIDE SAFE ZONE BY UNSTABLE RIFT // -25 LIVE RESONANCE")
          }
          isSelfStationTriggerDeath -> {
            if (currentCustomGame == null) {
              adjustLocalResonance(-15, "SELF-STATION PENALTY", showFctEvenIfRespawning = true)
            } else {
              liveResonance = (liveResonance - 15).coerceAtLeast(0)
            }
            onPrizeToast(
              "SELF-TRIGGERED STATION DEATH // -${selfStationBountyPenalty} RES & -15 LIVE RESONANCE"
            )
          }
          else -> {
            val shots = currentLifeStats.shotsFired
            val accMod =
              if (shots >= 8) {
                val accPct = (currentLifeStats.shotsHit * 100) / shots.coerceAtLeast(1)
                when {
                  accPct >= 45 -> 4
                  accPct < 15 -> -2
                  else -> 0
                }
              } else {
                0
              }
            val baseDeathPenalty = if (liveResonance >= 200) -10 else -5
            adjustLocalResonance(baseDeathPenalty + accMod, "HULL LOST")
          }
        }
        currentLifeStats.bountyReached = max(currentLifeStats.bountyReached, victimPreDeathBounty)
        currentLifeStats.shipId = localPlayer.spec.id
        currentLifeStats.shipFlown = formatShipLabel(localPlayer)
        val execTag =
          when {
            attacker != null ->
              if (attacker.armadaName.isNotEmpty()) cleanTag(attacker.armadaName) else cleanTag(attacker.syndicateTag)
            isUnstableRiftKill || isStationKill -> "SECTOR"
            else -> "GUEST"
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
                killerCallsign = attacker?.callsign ?: explosionFallbackName,
                killerSyndicateTag = execTag,
                killerShip = execShip,
                killingWeapon = weaponLabel,
                killerBountyAtKill = attackerPreKillBounty,
                bountyEarnedFromYou = bountyAward,
              )
          )
        onLifeRunEnded(finalizedSnapshot, currentLifeHadPvp || attackerIsHuman)
        notifyLocalLifecycleSync()
        onStatsUpdated()
      } else if (isEphemeralPracticeRift) {
        onStatsUpdated()
      }

      if (
        !isEphemeralPracticeRift &&
          hasJoinedMatch &&
          attacker != null &&
          attacker.isLocalPlayer &&
          target.isHuman &&
          !target.isLocalPlayer
      ) {
        val remoteVictimUid = target.id.removePrefix("remote_")
        onOutboundCombatEvent(
          com.example.data.SectorCombatEvent(
            targetUid = remoteVictimUid,
            sectorId = activeMatchSectorTag(),
            eventType = "ELIMINATED",
            damage = rawDamage.toLong().coerceAtLeast(0L),
            bountyAward = bountyAward.toLong().coerceAtLeast(0L),
            weaponLabel = weaponLabel,
            senderCallsign = attacker.callsign,
            senderTag = if (attacker.armadaName.isNotEmpty()) attacker.armadaName else attacker.syndicateTag,
            senderShipId = attacker.shipId.toLong(),
            createdAtMs = System.currentTimeMillis(),
          )
        )
      }

      handleRankedModeShipEliminated(target, attacker)
    }
  }

  private fun handleRankedModeShipEliminated(victim: ShipEntity, attacker: ShipEntity?) {
    clearShipToggleBuffs(victim, clearSlotIfActive = true)

    // 1. Mode 3: FLUX_CORE — Eject Flux Core at 1,650 px/s upon carrier elimination
    if (activeRankedMode == RankedGameMode.FLUX_CORE) {
      ejectFluxCoreFromShip(victim, reason = "CARRIER TERMINATED")
    }

    // 2. Mode 2: RIFT_SKIRMISH (or 3v3/5v5 SYNDICATE_WARS) — 3-Minute Rounds, No Respawn Until Round End, 1v1 Escalation
    if (isSkirmishRulesActive()) {
      victim.respawnTimer = 9999f
      if (victim.freq == 1) {
        skirmishWestKills++
      } else {
        skirmishEastKills++
      }
      val westAlive = ships.count { it.freq == 0 && it.respawnTimer <= 0f }
      val eastAlive = ships.count { it.freq == 1 && it.respawnTimer <= 0f }
      val killTarget = RiftCalibrationState.skirmishKillTargetPerRound.coerceAtLeast(1)
      if (eastAlive == 0 || skirmishWestKills >= killTarget) {
        resolveSkirmishRoundWon(winningFreq = 0)
      } else if (westAlive == 0 || skirmishEastKills >= killTarget) {
        resolveSkirmishRoundWon(winningFreq = 1)
      } else if (westAlive == 1 && eastAlive == 1 && !skirmishIs1v1Escalated) {
        skirmishIs1v1Escalated = true
        skirmish1v1AlertBanner = "1V1 CONVERGENCE // ABYSSAL DUEL • 180 BPM"
        skirmish1v1AlertTimer = 4.6f
        sfx.setDynamicEscalationTargetBpm(180f)
        if (hasJoinedMatch && !isInLobbyStaging) {
          sfx.playPortal()
        }
      }
    }

    // Auto-select top-bounty living teammate for interactive spectator camera strictly in round-based Deathmatch
    if (victim.isLocalPlayer && isSkirmishRulesActive() && skirmishReadyCountdownSec <= 0f) {
      val bestAlly = getLivingSpectatableTeammates().maxByOrNull { it.bounty }
      spectatingTargetShipId = bestAlly?.id ?: ""
    }

    // 3. Mode 4: STASIS_BREACH (or 10v10 SYNDICATE_WARS) — Send eliminated pilot into enemy Circular Stasis Jail & record team capture
    if (isStasisBreachRulesActive()) {
      val wasAlreadyJailed = victim.isInStasisBrig
      victim.isInStasisBrig = true
      if (!wasAlreadyJailed) {
        if (victim.freq == 1) {
          stasisWestCaptures++
        } else if (victim.freq == 0) {
          stasisEastCaptures++
        }
        if (attacker?.isLocalPlayer == true && victim.freq != localPlayer.freq) {
          localStasisCaptures++
          spawnPooledFct(
            text = "STASIS CAPTURE #${localStasisCaptures}!",
            x = victim.deathX,
            y = victim.deathY - 36f,
            color = Color.parseColor("#00F5D4"),
            maxLife = 1.25f,
          )
        }
      }
      if (victim.isLocalPlayer) {
        onPrizeToast("Captured in Enemy Stasis Jail! Shoot the center Core to break out!")
      }
      checkStasisLockdownVictory()
    }
  }

  private fun resolveSkirmishRoundWon(winningFreq: Int) {
    if (winningFreq == 0) {
      skirmishWestRoundWins++
    } else {
      skirmishEastRoundWins++
    }
    skirmishWestKills = 0
    skirmishEastKills = 0
    skirmishRoundTimerSec = 180f
    skirmishIs1v1Escalated = false
    skirmish1v1AlertTimer = 0f
    spectatingTargetShipId = ""

    reattachRemotePilotsToRoster()
    // Stage & heal all ships at their West/East Horizon Warp Gates for the 10-second Ready-Up countdown
    for (s in ships) {
      clearShipToggleBuffs(s, clearSlotIfActive = true)
      s.respawnTimer = 0f
      s.deathLingerTimer = 0f
      s.warpInAnimTimer = 0.65f
      s.maxFlux = s.spec.maxFlux * (currentCustomGame?.healthMult ?: 1.0f)
      s.flux = s.maxFlux
      s.vx = 0f
      s.vy = 0f
      val sp = getSkirmishGateSpawnPosition(s.freq)
      s.x = sp.first
      s.y = sp.second
      if (s.isHuman && !s.isLocalPlayer) {
        s.netTargetX = s.x
        s.netTargetY = s.y
        s.netTargetVx = 0f
        s.netTargetVy = 0f
        s.netPacketAgeSec = 0f
        s.netSmoothOffsetX = 0f
        s.netSmoothOffsetY = 0f
        s.netSmoothVelX = 0f
        s.netSmoothVelY = 0f
        s.netWasRespawning = false
        s.netLocalKillGraceSec = 0f
        s.netRespawnHoldSec = 0f
      }
      s.phaseShieldTimer = 3.0f
      s.isCloaked = false
    }
    notifyLocalLifecycleSync()
    bullets.clear()
    bombs.clear()
    swarmDrones.clear()
    brickWalls.clear()
    shipDebrisChunks.clear()
    interdictorBubbles.clear()
    reactivePrecipitates.clear()
    reactiveFilaments.clear()
    bakeStaticRadarBitmap()

    if (!hasJoinedMatch) {
      skirmishReadyCountdownSec = 0f
      skirmishAbyssalBlend = 0f
      sfx.setDynamicEscalationTargetBpm(null)
      localPlayer.respawnTimer = 999f
      localPlayer.x = arenaCenterX
      localPlayer.y = arenaCenterY
      localPlayer.vx = 0f
      localPlayer.vy = 0f
      if (skirmishWestRoundWins >= 2 || skirmishEastRoundWins >= 2) {
        skirmishRound = 1
        skirmishWestRoundWins = 0
        skirmishEastRoundWins = 0
      } else {
        skirmishRound = (skirmishWestRoundWins + skirmishEastRoundWins + 1).coerceIn(1, 3)
      }
      return
    }

    skirmishReadyCountdownSec = skirmishReadyDurationSec
    for (s in ships) {
      s.weaponsLocked = true
      s.canFire = false
      s.weaponState = "READY-UP"
    }

    if (skirmishWestRoundWins >= 2 || skirmishEastRoundWins >= 2) {
      val westWonMatch = skirmishWestRoundWins >= 2
      val localTeamWon = if (localPlayer.freq == 1) !westWonMatch else westWonMatch
      val isClanWar = activeRankedMode == RankedGameMode.SYNDICATE_WARS
      val winHorizon = if (westWonMatch) "WEST HORIZON" else "EAST HORIZON"
      val winScore = max(skirmishWestRoundWins, skirmishEastRoundWins)
      val loseScore = min(skirmishWestRoundWins, skirmishEastRoundWins)
      onDailyDirectiveEvent(DailyDirectiveMetric.MATCHES_PLAYED, 1, false)
      if (localTeamWon) {
        riftShards += 75L
        convergencesWon++
        adjustLocalResonance(30, if (isClanWar) "SYNDICATE WAR WIN" else "SKIRMISH WIN")
        onSyndicateContribution(0L, localPlayer.bounty.toLong(), 1L)
        if (isClanWar) {
          onSyndicateWarFinished(true, 25)
        }
        val matchTitle =
          if (isClanWar) {
            "SYNDICATE WAR VICTORY! [${localPlayer.syndicateTag.ifEmpty { "SOLO" }}] DEFEATED [$rivalSyndicateWarTag] ($winScore-$loseScore) • +75 SHARDS +25 WAR TROPHIES!"
          } else {
            "RIFT SKIRMISH VICTORY! $winHorizon WINS $winScore-$loseScore • +75 RIFT SHARDS!"
          }
        highBountyShutdownBanner = matchTitle
        highBountyShutdownTimer = 5.2f
        sfx.playPrize()
        onPrizeToast(matchTitle)
      } else {
        if (liveResonance >= 200) {
          adjustLocalResonance(-12, "MATCH LOSS")
        }
        if (isClanWar) {
          onSyndicateWarFinished(false, -15)
        }
        val defeatTitle =
          if (isClanWar) {
            "SYNDICATE WAR CONCLUDED — [$rivalSyndicateWarTag] WON $winScore-$loseScore"
          } else {
            "RIFT SKIRMISH CONCLUDED — $winHorizon WON $winScore-$loseScore"
          }
        highBountyShutdownBanner = defeatTitle
        highBountyShutdownTimer = 4.5f
        sfx.playWarp()
        onPrizeToast(defeatTitle)
      }
      skirmishRound = 1
      skirmishWestRoundWins = 0
      skirmishEastRoundWins = 0
      onStatsUpdated()
    } else {
      skirmishRound = (skirmishWestRoundWins + skirmishEastRoundWins + 1).coerceIn(1, 3)
      val roundWinnerName = if (winningFreq == 0) "WEST HORIZON" else "EAST HORIZON"
      val banner =
        "$roundWinnerName WINS ROUND ${skirmishRound - 1}! SERIES: $skirmishWestRoundWins - $skirmishEastRoundWins • 10S READY-UP"
      highBountyShutdownBanner = banner
      highBountyShutdownTimer = 4.0f
      sfx.playWarp()
      onPrizeToast(banner)
    }
  }

  private fun checkStasisLockdownVictory() {
    if (!isStasisBreachRulesActive()) return
    val westTeam = ships.filter { it.freq == 0 && (hasJoinedMatch || !it.isLocalPlayer) }
    val eastTeam = ships.filter { it.freq == 1 }
    val allWestJailed = westTeam.isNotEmpty() && westTeam.all { it.isInStasisBrig }
    val allEastJailed = eastTeam.isNotEmpty() && eastTeam.all { it.isInStasisBrig }
    if (!allWestJailed && !allEastJailed) return
    val winningFreq = if (allEastJailed) 0 else 1
    finishStasisBreachMatch(winningFreq = winningFreq, isTotalLockdown = true)
  }

  private fun finishStasisBreachMatch(winningFreq: Int, isTotalLockdown: Boolean) {
    val westScoreFinal = stasisWestCaptures
    val eastScoreFinal = stasisEastCaptures
    val personalCapsFinal = localStasisCaptures
    val localTeamWon = localPlayer.freq == winningFreq
    val isClanWar = activeRankedMode == RankedGameMode.SYNDICATE_WARS

    stasisBreachMatchTimerSec = 180f
    stasisWestCaptures = 0
    stasisEastCaptures = 0
    localStasisCaptures = 0
    reattachRemotePilotsToRoster()
    for (s in ships) {
      s.isInStasisBrig = false
      s.deathLingerTimer = 0f
      s.warpInAnimTimer = 0.65f
      s.maxFlux = s.spec.maxFlux * (currentCustomGame?.healthMult ?: 1.0f)
      s.flux = s.maxFlux
      s.vx = 0f
      s.vy = 0f
      if (s.isLocalPlayer && !hasJoinedMatch) {
        s.respawnTimer = 999f
        s.x = arenaCenterX
        s.y = arenaCenterY
      } else {
        s.respawnTimer = 0f
        val sp = getSkirmishGateSpawnPosition(s.freq)
        s.x = sp.first
        s.y = sp.second
        if (s.isHuman && !s.isLocalPlayer) {
          s.netTargetX = s.x
          s.netTargetY = s.y
          s.netTargetVx = 0f
          s.netTargetVy = 0f
          s.netPacketAgeSec = 0f
          s.netSmoothOffsetX = 0f
          s.netSmoothOffsetY = 0f
          s.netSmoothVelX = 0f
          s.netSmoothVelY = 0f
          s.netWasRespawning = false
          s.netLocalKillGraceSec = 0f
          s.netRespawnHoldSec = 0f
        }
        s.phaseShieldTimer = 3.0f
      }
    }
    notifyLocalLifecycleSync()
    swarmDrones.clear()
    brickWalls.clear()
    shipDebrisChunks.clear()
    bakeStaticRadarBitmap()
    westBrigGeneratorHp = getEffectiveBrigReactorMaxHp()
    eastBrigGeneratorHp = getEffectiveBrigReactorMaxHp()
    if (!hasJoinedMatch) return

    val winTeamLabel = if (winningFreq == 0) "WEST HORIZON" else "EAST HORIZON"
    onDailyDirectiveEvent(DailyDirectiveMetric.MATCHES_PLAYED, 1, false)
    if (localTeamWon) {
      val shardReward = if (isTotalLockdown) 100L else 85L
      riftShards += shardReward
      convergencesWon++
      adjustLocalResonance(30, "STASIS BREACH WIN")
      onSyndicateContribution(0L, localPlayer.bounty.toLong(), 1L)
      if (isClanWar) {
        onSyndicateWarFinished(true, 35)
      }
      val msg =
        if (isTotalLockdown) {
          "TOTAL STASIS LOCKDOWN VICTORY! $winTeamLabel $westScoreFinal-$eastScoreFinal CAPTURES (YOU $personalCapsFinal) • +$shardReward SHARDS"
        } else {
          "STASIS BREACH VICTORY! $winTeamLabel $westScoreFinal-$eastScoreFinal CAPTURES (YOU $personalCapsFinal) • +$shardReward SHARDS"
        }
      highBountyShutdownBanner = msg
      highBountyShutdownTimer = 5.5f
      sfx.playPrize()
      onPrizeToast(msg)
      onStatsUpdated()
    } else {
      if (liveResonance >= 200) {
        adjustLocalResonance(-12, "MATCH LOSS")
      }
      if (isClanWar) {
        onSyndicateWarFinished(false, -20)
      }
      val msg =
        if (isTotalLockdown) {
          "TOTAL STASIS LOCKDOWN — $winTeamLabel JAILED ALL PILOTS ($westScoreFinal-$eastScoreFinal CAPTURES)"
        } else {
          "STASIS BREACH CONCLUDED — $winTeamLabel WON $westScoreFinal-$eastScoreFinal CAPTURES (YOU $personalCapsFinal)"
        }
      highBountyShutdownBanner = msg
      highBountyShutdownTimer = 4.8f
      sfx.playWarp()
      onPrizeToast(msg)
      onStatsUpdated()
    }
  }

  fun damageBrigReactorCore(
    targetBrigFreq: Int, // 0 = West Brig Core (holds East freq=1 prisoners), 1 = East Brig Core (holds West freq=0 prisoners)
    attacker: ShipEntity?,
    rawDamage: Float,
  ) {
    if (!isStasisBreachRulesActive()) return
    // Prisoners inside that jail deal 100% weapon damage against the 40,000 HP center Core
    if (attacker == null || !attacker.isInStasisBrig || attacker.freq == targetBrigFreq) return
    val effectiveDmg = rawDamage
    if (effectiveDmg <= 0f) return

    val rx = if (targetBrigFreq == 0) westBrigReactorX else eastBrigReactorX
    val ry = if (targetBrigFreq == 0) westBrigReactorY else eastBrigReactorY

    if (targetBrigFreq == 0) {
      westBrigGeneratorHp = max(0f, westBrigGeneratorHp - effectiveDmg)
    } else {
      eastBrigGeneratorHp = max(0f, eastBrigGeneratorHp - effectiveDmg)
    }

    if (attacker.isLocalPlayer) {
      currentLifeStats.shotsHit++
      currentLifeStats.damageDealt += effectiveDmg
      spawnPooledFct(
        text = "JAIL CORE -${effectiveDmg.toInt()}",
        x = rx,
        y = ry - 36f,
        color = Color.parseColor("#FACC15"),
      )
    }

    if (targetBrigFreq == 0 && westBrigGeneratorHp <= 0f) {
      breachStasisBrig(brigTeamFreq = 0)
    } else if (targetBrigFreq == 1 && eastBrigGeneratorHp <= 0f) {
      breachStasisBrig(brigTeamFreq = 1)
    }
  }

  private fun breachStasisBrig(brigTeamFreq: Int) {
    val maxHp = getEffectiveBrigReactorMaxHp()
    val rx = if (brigTeamFreq == 0) westBrigReactorX else eastBrigReactorX
    val ry = if (brigTeamFreq == 0) westBrigReactorY else eastBrigReactorY
    val releasedPrisonerFreq = if (brigTeamFreq == 0) 1 else 0

    if (brigTeamFreq == 0) {
      westBrigGeneratorHp = maxHp
    } else {
      eastBrigGeneratorHp = maxHp
    }

    var releasedCount = 0
    for (s in ships) {
      if (s.freq == releasedPrisonerFreq && s.isInStasisBrig) {
        s.isInStasisBrig = false
        s.flux = s.maxFlux
        s.phaseShieldTimer = 3.0f
        releasedCount++
      }
    }

    // Outward Core Explosion: damages and blasts away nearby outside enemy ships (s.freq == brigTeamFreq)
    val blastRadius = 640f
    for (s in ships) {
      if (s.freq == brigTeamFreq && s.respawnTimer <= 0f && !isInsideSafeZone(s.x, s.y)) {
        val dx = s.x - rx
        val dy = s.y - ry
        val dist = hypot(dx, dy)
        if (dist <= blastRadius) {
          val dSafe = dist.coerceAtLeast(1f)
          val nx = dx / dSafe
          val ny = dy / dSafe
          s.vx += nx * 950f
          s.vy += ny * 950f
          val blastDmg = 1200f * (1f - (dist / blastRadius) * 0.45f).coerceIn(0.45f, 1f)
          applyProjectileDamage(
            target = s,
            attackerId = "",
            attackerIsHuman = false,
            rawDamage = blastDmg,
            impactAngle = atan2(ry - s.y, rx - s.x),
            isHeavyImpact = true,
            isEmp = false,
            attackerShipLabel = "Stasis Jail Core",
            weaponLabel = "Core Detonation",
          )
        }
      }
    }

    shockwaves.add(ShockwaveEntity(rx, ry, 28f, 640f, Color.parseColor("#00F5D4")))
    shockwaves.add(ShockwaveEntity(rx, ry, 20f, 480f, Color.parseColor("#FACC15")))
    shockwaves.add(ShockwaveEntity(rx, ry, 14f, 320f, Color.parseColor("#EC4899")))
    spawnExplosion(rx, ry, Color.parseColor("#00F5D4"))
    addTrauma(0.65f)
    sfx.playWarp()

    val msg = "STASIS CORE DETONATED! $releasedCount PRISONERS FREED!"
    highBountyShutdownBanner = msg
    highBountyShutdownTimer = 4.5f
    spawnPooledFct(
      text = "CORE DETONATED!",
      x = rx,
      y = ry - 48f,
      color = Color.parseColor("#00F5D4"),
      maxLife = 1.5f,
    )
    onPrizeToast(msg)
  }

  fun findRandomOpenPositionOutsideSafeZone(): Pair<Float, Float> {
    val maxR = (arenaRadius - 200f).coerceAtLeast(700f)
    for (attempt in 0 until 60) {
      val ang = Random.nextFloat() * PI.toFloat() * 2f
      val dist = 620f + sqrt(Random.nextFloat()) * (maxR - 620f)
      val x = arenaCenterX + cos(ang) * dist
      val y = arenaCenterY + sin(ang) * dist
      val inSafePadded =
        hasActiveSafeZone() &&
          hypot(x - arenaCenterX, y - arenaCenterY) < getSafeZoneRadius() + 180f
      if (inSafePadded) continue
      if (circleIntersectsWalls(x, y, 56f)) continue
      return x to y
    }
    return (arenaCenterX - maxR * 0.6f) to (arenaCenterY - maxR * 0.6f)
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
    target.turnOverchargeStacks = 0
    target.maxFluxOverchargeStacks = 0
    target.maxFlux = target.spec.maxFlux * (currentCustomGame?.healthMult ?: 1.0f)
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
      notifyLocalLifecycleSync()
      onStatsUpdated()
      onPrizeToast("SHIP DESTROYED — IDLE FLUX DRAIN DEPLETED")
    }
    triggerMultiStageShipExplosion(
      target = target,
      attacker = null,
      weaponLabel = "Open-Space Idle Drain",
      fallbackKillerName = "IDLE DRAIN",
    )
    spawnPooledFct(
      text = "IDLE FLUX OVERLOAD",
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
    handleRankedModeShipEliminated(target, null)
  }

  private fun destroyShipFromPlasmaRift(target: ShipEntity) {
    if (target.respawnTimer > 0f) return
    val victimPreDeathBounty = target.bounty
    target.flux = 0f
    target.deathX = target.x
    target.deathY = target.y
    target.respawnTimer = max(1.35f, RiftCalibrationState.respawnDelaySec)
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
    target.turnOverchargeStacks = 0
    target.maxFluxOverchargeStacks = 0
    target.maxFlux = target.spec.maxFlux * (currentCustomGame?.healthMult ?: 1.0f)
    target.hasBouncingBulletsUpgrade = false

    if (target.isLocalPlayer || hypot(target.x - localPlayer.x, target.y - localPlayer.y) < 1100f) {
      sfx.playExplosion()
    }
    if (target.isLocalPlayer) {
      triggerHitStop(2)
      addTrauma(0.55f)
      if (!isEphemeralPracticeRift) {
        allTimeDeaths++
      }
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
      notifyLocalLifecycleSync()
      onStatsUpdated()
      onPrizeToast("SHIP DESTROYED — PLASMA RIFT SHEAR OVERLOAD")
    }
    triggerMultiStageShipExplosion(
      target = target,
      attacker = null,
      weaponLabel = "Plasma Shear Overload",
      fallbackKillerName = "PLASMA RIFT",
    )
    spawnPooledFct(
      text = "PLASMA RIFT OVERLOAD",
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
    handleRankedModeShipEliminated(target, null)
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
    fun checkRect(left: Float, top: Float, right: Float, bottom: Float) {
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

    val pad = 22f
    for (w in walls) {
      checkRect(w.x - pad, w.y - pad, w.x + w.w + pad, w.y + w.h + pad)
    }
    for (bw in brickWalls) {
      checkRect(bw.x - pad, bw.y - pad, bw.x + bw.w + pad, bw.y + bw.h + pad)
    }

    // Avoid outside Stasis Jail containment bubbles so AI bots steer smoothly around the 720px-wide spheres
    if (isStasisBreachRulesActive()) {
      val avoidR = stasisJailRadius + 46f
      val jails = listOf(Pair(westBrigReactorX, westBrigReactorY), Pair(eastBrigReactorX, eastBrigReactorY))
      for ((jx, jy) in jails) {
        if (hypot(ox - jx, oy - jy) > stasisJailRadius + 10f) {
          val fxJ = ox - jx
          val fyJ = oy - jy
          val bJ = fxJ * dirX + fyJ * dirY
          val cJ = fxJ * fxJ + fyJ * fyJ - avoidR * avoidR
          val discJ = bJ * bJ - cJ
          if (discJ >= 0f) {
            val tEntry = -bJ - sqrt(discJ)
            if (tEntry > 1.0f && tEntry < bestT) {
              val hx = ox + dirX * tEntry
              val hy = oy + dirY * tEntry
              val invR = 1f / avoidR
              bestT = tEntry
              closestHit =
                RaycastHit(
                  t = tEntry,
                  hitX = hx,
                  hitY = hy,
                  normalX = (hx - jx) * invR,
                  normalY = (hy - jy) * invR,
                )
            }
          }
        }
      }
    }

    // Check circular outer sector boundary for AI bot look-ahead steering
    val safePerimR = (arenaRadius - 48f).coerceAtLeast(500f)
    val fxP = ox - arenaCenterX
    val fyP = oy - arenaCenterY
    val bP = fxP * dirX + fyP * dirY
    val cP = fxP * fxP + fyP * fyP - safePerimR * safePerimR
    val discP = bP * bP - cP
    if (discP >= 0f) {
      val tExit = -bP + sqrt(discP)
      if (tExit > 1.5f && tExit < bestT) {
        val hx = ox + dirX * tExit
        val hy = oy + dirY * tExit
        val invR = 1f / safePerimR
        bestT = tExit
        closestHit =
          RaycastHit(
            t = tExit,
            hitX = hx,
            hitY = hy,
            normalX = (arenaCenterX - hx) * invR,
            normalY = (arenaCenterY - hy) * invR,
          )
      }
    }
    return closestHit
  }

  private fun triggerBotDefensiveRepel(bot: ShipEntity, triggerRadius: Float): Boolean {
    val rCost = RiftCalibrationState.repelCost
    if (bot.repels <= 0 || bot.flux <= rCost + 30f) return false
    val chasersNearby =
      ships.any {
        !(isInLobbyStaging && it.isLocalPlayer) &&
          !areAllied(bot, it) &&
          it.respawnTimer <= 0f &&
          !it.isInStasisBrig &&
          hypot(it.x - bot.x, it.y - bot.y) < triggerRadius
      }
    if (!chasersNearby) return false
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
    return true
  }

  private fun findNearestSupplyCrateForBot(
    bot: ShipEntity,
    maxSearchDist: Float,
    extraSafeMargin: Float = 0f,
  ): GreenCrate? {
    var bestCrate: GreenCrate? = null
    var bestDist = maxSearchDist
    val safeR = getSafeZoneRadius()
    for (g in greens) {
      if (isInsideSafeZone(g.x, g.y) || isWithinBotBanPerimeter(g.x, g.y)) continue
      if (
        extraSafeMargin > 0f &&
          hasActiveSafeZone() &&
          hypot(g.x - arenaCenterX, g.y - arenaCenterY) <= safeR + extraSafeMargin
      ) {
        continue
      }
      val d = hypot(g.x - bot.x, g.y - bot.y)
      if (d < bestDist) {
        bestDist = d
        bestCrate = g
      }
    }
    return bestCrate
  }

  private fun fireBotCombatOrdnanceAt(
    bot: ShipEntity,
    target: ShipEntity,
    dist: Float,
    minFluxRatio: Float = 0.26f,
  ) {
    val leadT = min(0.75f, dist / bot.spec.primarySpeed.coerceAtLeast(120f))
    val tx = target.x + target.vx * leadT
    val ty = target.y + target.vy * leadT
    val aimAngle = atan2(ty - bot.y, tx - bot.x)
    bot.weaponAngle = aimAngle
    bot.independentAimActive = true

    if (bot.spec.bombType == "CLOAK_EMP") {
      val pulseR = RiftCalibrationState.empPulseRadius
      if (!bot.isCloaked && dist in (pulseR * 1.15f)..980f && bot.flux > bot.maxFlux * 0.55f && bot.bombTimer <= 0f) {
        fireSecondaryBomb(bot, aimAngle)
      } else if (bot.isCloaked && dist <= pulseR * 0.92f && bot.bombTimer <= 0f) {
        fireSecondaryBomb(bot, aimAngle)
      }
    } else if (bot.spec.bombType == "INTERDICTOR_DOME") {
      if (dist < 245f && bot.flux > 620f && bot.bombTimer <= 0f) {
        fireSecondaryBomb(bot, aimAngle)
      }
    } else if (bot.spec.bombType == "PHASE_DASH") {
      if (dist in 450f..900f && bot.flux > bot.maxFlux * 0.55f && bot.bombTimer <= 0f) {
        fireSecondaryBomb(bot, aimAngle)
      }
    } else if (bot.spec.hasBomb && dist in 240f..860f && bot.flux > bot.maxFlux * 0.48f && bot.bombTimer <= 0f) {
      fireSecondaryBomb(bot, aimAngle)
    }

    if (!bot.isCloaked && dist < RiftCalibrationState.botGunFireRange && bot.flux > bot.maxFlux * minFluxRatio) {
      firePrimaryGun(bot)
    }
  }

  private fun hasClearBotLineOfSight(x0: Float, y0: Float, x1: Float, y1: Float): Boolean {
    return segmentFirstWallHit(x0, y0, x1, y1, pad = 2f) == null
  }

  private fun steerBotAwayFromVolatileRifts(bot: ShipEntity, baseAngle: Float): Float {
    var outX = cos(baseAngle)
    var outY = sin(baseAngle)
    for (anom in anomalies) {
      val dangerR =
        when {
          anom.isUnstableMergedRift -> anom.physicalRadius * 2.25f
          anom.type == AnomalyType.GRAVITY_RIFT && anom.physicalRadius >= 240f -> anom.physicalRadius * 1.45f
          anom.type == AnomalyType.PLASMA_RIFT -> anom.physicalRadius * 1.25f
          else -> 0f
        }
      if (dangerR <= 0f) continue
      val dx = bot.x - anom.x
      val dy = bot.y - anom.y
      val d = hypot(dx, dy).coerceAtLeast(1f)
      if (d < dangerR) {
        val push = ((dangerR - d) / dangerR) * 2.1f
        outX += (dx / d) * push
        outY += (dy / d) * push
      }
    }
    return atan2(outY, outX)
  }

  private fun stepBotToggleableAndSpecialBuffs(
    bot: ShipEntity,
    fluxRatio: Float,
    primaryTarget: ShipEntity?,
    primaryTargetDist: Float,
    incomingThreatAngle: Float?,
    isFluxCoreMode: Boolean,
    isBotFluxCoreCarrier: Boolean,
    lowFluxRetreat: Boolean,
  ) {
    if (isInsideSafeZone(bot.x, bot.y)) return

    if (bot.tractorBeamActive) {
      if (fluxRatio < 0.30f || primaryTarget == null || primaryTargetDist > 610f) {
        activateShipSpecialBuff(bot)
      } else {
        bot.weaponAngle = atan2(primaryTarget.y - bot.y, primaryTarget.x - bot.x)
        bot.independentAimActive = true
      }
      return
    }

    if (bot.lanceBeamActive) {
      val clearLos =
        primaryTarget != null &&
          primaryTargetDist <= 620f + (bot.lanceActiveStacks - 1) * 150f &&
          hasClearBotLineOfSight(bot.x, bot.y, primaryTarget.x, primaryTarget.y)
      if (fluxRatio < 0.28f || !clearLos) {
        activateShipSpecialBuff(bot)
      } else {
        bot.weaponAngle = atan2(primaryTarget.y - bot.y, primaryTarget.x - bot.x)
        bot.independentAimActive = true
      }
      return
    }

    if (bot.specialWeaponCharges <= 0 || bot.specialWeaponType == "NONE") return
    val normType = RiftCodex.normalizeSpecialBuffId(bot.specialWeaponType)
    val shouldTrigger =
      when (normType) {
        "HOLO_PHANTOM", "STASIS_BARRIER" ->
          incomingThreatAngle != null ||
            (primaryTarget != null && primaryTargetDist < 540f && (fluxRatio < 0.75f || isBotFluxCoreCarrier))
        "HYPER_SURGE" ->
          lowFluxRetreat ||
            isBotFluxCoreCarrier ||
            (isFluxCoreMode && fluxCoreCarrierEntityId.isEmpty()) ||
            (primaryTarget != null && primaryTargetDist in 620f..1600f)
        "SHATTER_CONE", "NOVA_RING" -> primaryTarget != null && primaryTargetDist < 480f
        "SEEKER_TORPEDO", "SWARM_DRONES", "PROX_SEEKER" -> primaryTarget != null && primaryTargetDist < 1200f
        "TRACTOR_BEAM" ->
          fluxRatio > 0.55f &&
            primaryTarget != null &&
            primaryTargetDist in 160f..520f &&
            hasClearBotLineOfSight(bot.x, bot.y, primaryTarget.x, primaryTarget.y)
        "LANCE_BEAM" ->
          fluxRatio > 0.52f &&
            primaryTarget != null &&
            primaryTargetDist <= 580f &&
            hasClearBotLineOfSight(bot.x, bot.y, primaryTarget.x, primaryTarget.y)
        else ->
          RiftCodex.getBuffRegistryEntry(bot.specialWeaponType)?.isCustomForged == true &&
            primaryTarget != null &&
            primaryTargetDist < 780f
      }
    if (shouldTrigger && Random.nextFloat() < RiftCalibrationState.botSpecialBuffChance) {
      if (primaryTarget != null) {
        bot.weaponAngle = atan2(primaryTarget.y - bot.y, primaryTarget.x - bot.x)
        bot.independentAimActive = true
      }
      activateShipSpecialBuff(bot)
    }
  }

  private fun updateAiBot(bot: ShipEntity, dt: Float) {
    if (bot.respawnTimer > 0f || !bot.canFire) return
    bot.afterburning = false

    val isFluxCoreMode = isFluxCoreRulesActive()
    val isStasisBreachMode = isStasisBreachRulesActive()
    val isRiftControlMode = isRiftControlRulesActive()
    val isSkirmishMode = isSkirmishRulesActive()
    val isBotFluxCoreCarrier =
      isFluxCoreMode &&
        (bot.id == fluxCoreCarrierEntityId ||
          (fluxCoreOrb.carrierShipId == bot.shipId && fluxCoreCarrierEntityId.isNotEmpty()))

    // Mode 4A (STASIS_BREACH): Imprisoned bots orbit the Stasis Brig Core and focus all fire on breaking out!
    if (isStasisBreachMode && bot.isInStasisBrig) {
      val coreX = if (bot.freq == 0) eastBrigReactorX else westBrigReactorX
      val coreY = if (bot.freq == 0) eastBrigReactorY else westBrigReactorY
      val aimAng = atan2(coreY - bot.y, coreX - bot.x)
      bot.weaponAngle = aimAng
      bot.independentAimActive = true
      val orbitAng = (System.currentTimeMillis() % 10000L) * 0.0020f + bot.aiSeed
      val goalX = coreX + cos(orbitAng) * 160f
      val goalY = coreY + sin(orbitAng) * 160f
      val moveAng = atan2(goalY - bot.y, goalX - bot.x)
      bot.vx = bot.vx * 0.90f + cos(moveAng) * bot.spec.thrust * 0.62f * dt
      bot.vy = bot.vy * 0.90f + sin(moveAng) * bot.spec.thrust * 0.62f * dt
      bot.angle = aimAng
      if (bot.spec.supportsNativeMultifire) {
        bot.multifireOn = bot.flux > bot.maxFlux * 0.42f
      }
      if (bot.lanceBeamActive && bot.flux < bot.maxFlux * 0.25f) {
        activateShipSpecialBuff(bot)
      } else if (
        !bot.lanceBeamActive &&
          bot.specialWeaponCharges > 0 &&
          RiftCodex.normalizeSpecialBuffId(bot.specialWeaponType) == "LANCE_BEAM" &&
          bot.flux > bot.maxFlux * 0.50f
      ) {
        activateShipSpecialBuff(bot)
      }
      if (bot.flux > bot.maxFlux * 0.18f) {
        firePrimaryGun(bot)
      }
      if (
        bot.spec.hasBomb &&
          bot.spec.bombType !in setOf("PHASE_DASH", "CLOAK_EMP", "INTERDICTOR_DOME") &&
          bot.flux > bot.maxFlux * 0.55f &&
          bot.bombTimer <= 0f
      ) {
        fireSecondaryBomb(bot, aimAng)
      }
      return
    }

    val fluxRatio = bot.flux / bot.maxFlux
    val lowFluxRetreat = fluxRatio < RiftCalibrationState.botRetreatFluxRatio
    val insideSafeBanZone = isWithinBotBanPerimeter(bot.x, bot.y)

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

    // Mode 3 Carrier Hard Guardrail: NEVER approach Safe Zone while holding the Flux Core!
    val nearSafeForCarrier =
      isBotFluxCoreCarrier &&
        hasActiveSafeZone() &&
        hypot(bot.x - arenaCenterX, bot.y - arenaCenterY) <= getSafeZoneRadius() + 300f

    if (nearSafeForCarrier || (!lowFluxRetreat && insideSafeBanZone)) {
      val cx = WORLD_SIZE / 2f
      val cy = WORLD_SIZE / 2f
      val radialAway = atan2(bot.y - cy, bot.x - cx)
      val tangSign = if ((bot.aiSeed.toInt() and 1) == 0) 0.44f else -0.44f
      val awayAngle = if (nearSafeForCarrier) (radialAway + tangSign) else radialAway
      val damp = 0.28.pow(dt.toDouble()).toFloat()
      bot.vx = bot.vx * damp + cos(awayAngle) * bot.spec.thrust * 1.08f * dt
      bot.vy = bot.vy * damp + sin(awayAngle) * bot.spec.thrust * 1.08f * dt
      if (hypot(bot.vx, bot.vy) > 2f) {
        bot.angle = atan2(bot.vy, bot.vx)
      }
      if (isBotFluxCoreCarrier) {
        triggerBotDefensiveRepel(bot, triggerRadius = 210f)
      } else {
        bot.weaponAngle = bot.angle
      }
      return
    }

    // Scan valid enemy targets across the playfield
    var nearestEnemy: ShipEntity? = null
    var nearestDist = Float.MAX_VALUE
    var lowestFluxSkirmishEnemy: ShipEntity? = null
    var lowestFluxVal = Float.MAX_VALUE
    var lowestFluxDist = Float.MAX_VALUE
    var nearestHvtEnemy: ShipEntity? = null
    var nearestHvtDist = Float.MAX_VALUE
    var bestHvtBounty = 0
    var fluxCoreCarrierEnemy: ShipEntity? = null
    var fluxCoreCarrierDist = Float.MAX_VALUE
    var enemyAttackingOurBrig: ShipEntity? = null
    var enemyAttackingOurBrigDist = Float.MAX_VALUE

    val myTeamBrigX = if (bot.freq == 0) westBrigReactorX else eastBrigReactorX
    val myTeamBrigY = if (bot.freq == 0) westBrigReactorY else eastBrigReactorY

    for (other in ships) {
      if (
        !(isInLobbyStaging && other.isLocalPlayer) &&
          !areAllied(bot, other) &&
          other.respawnTimer <= 0f &&
          !other.isCloaked &&
          !other.isInStasisBrig &&
          !isInsideSafeZone(other.x, other.y) &&
          (isFluxCoreMode || !isWithinBotBanPerimeter(other.x, other.y))
      ) {
        val d = hypot(other.x - bot.x, other.y - bot.y)
        if (d < nearestDist) {
          nearestDist = d
          nearestEnemy = other
        }
        if (isFluxCoreMode && fluxCoreCarrierEntityId.isNotEmpty() && other.id == fluxCoreCarrierEntityId) {
          fluxCoreCarrierEnemy = other
          fluxCoreCarrierDist = d
        }
        if (isSkirmishMode && d < 980f && other.flux < lowestFluxVal) {
          lowestFluxVal = other.flux
          lowestFluxSkirmishEnemy = other
          lowestFluxDist = d
        }
        if (other.bounty >= RiftCalibrationState.hvtBountyThreshold && d < 1750f) {
          if (other.bounty > bestHvtBounty || (other.bounty == bestHvtBounty && d < nearestHvtDist)) {
            bestHvtBounty = other.bounty
            nearestHvtDist = d
            nearestHvtEnemy = other
          }
        }
        if (isStasisBreachMode && hypot(other.x - myTeamBrigX, other.y - myTeamBrigY) <= stasisJailRadius + 480f && d < enemyAttackingOurBrigDist) {
          enemyAttackingOurBrigDist = d
          enemyAttackingOurBrig = other
        }
      }
    }

    // Deathmatch Low-Flux Caution (Rift Skirmish / Syndicate Wars): preserve single round life!
    if (isSkirmishMode && fluxRatio < 0.38f) {
      if (bot.tractorBeamActive || bot.lanceBeamActive) {
        activateShipSpecialBuff(bot)
      }
      triggerBotDefensiveRepel(bot, triggerRadius = 380f)
      val defensiveCrate = findNearestSupplyCrateForBot(bot, maxSearchDist = 1750f)
      val healthiestAlly =
        ships
          .filter { it.id != bot.id && areAllied(bot, it) && it.respawnTimer <= 0f && !(isInLobbyStaging && it.isLocalPlayer) }
          .maxByOrNull { it.flux }
      val retreatAngle =
        steerBotAwayFromVolatileRifts(
          bot,
          when {
            defensiveCrate != null -> atan2(defensiveCrate.y - bot.y, defensiveCrate.x - bot.x)
            healthiestAlly != null && hypot(healthiestAlly.x - bot.x, healthiestAlly.y - bot.y) > 200f ->
              atan2(healthiestAlly.y - bot.y, healthiestAlly.x - bot.x)
            nearestEnemy != null -> atan2(bot.y - nearestEnemy.y, bot.x - nearestEnemy.x) + 0.35f
            else -> atan2(WORLD_SIZE / 2f - bot.y, WORLD_SIZE / 2f - bot.x)
          },
        )
      if (nearestEnemy != null && nearestDist < 580f && fluxRatio > 0.24f) {
        fireBotCombatOrdnanceAt(bot, nearestEnemy, nearestDist, minFluxRatio = 0.24f)
      } else {
        bot.independentAimActive = false
        bot.weaponAngle = retreatAngle
      }
      val damp = 0.28.pow(dt.toDouble()).toFloat()
      bot.vx = bot.vx * damp + cos(retreatAngle) * bot.spec.thrust * 1.10f * dt
      bot.vy = bot.vy * damp + sin(retreatAngle) * bot.spec.thrust * 1.10f * dt
      if (hypot(bot.vx, bot.vy) > 2f) {
        bot.angle = atan2(bot.vy, bot.vx)
      }
      return
    }

    // Safe-Zone Survival Retreat strictly in Sector Convergence (never in Flux Core or non-Safe-Zone modes)
    if (
      lowFluxRetreat &&
        hasActiveSafeZone() &&
        !isFluxCoreMode &&
        bot.safeZoneLockoutTimer <= 0f &&
        !isInsideSafeZone(bot.x, bot.y)
    ) {
      if (bot.tractorBeamActive || bot.lanceBeamActive) {
        activateShipSpecialBuff(bot)
      }
      val toSafeAngle = steerBotAwayFromVolatileRifts(bot, atan2(arenaCenterY - bot.y, arenaCenterX - bot.x))
      val urgency = if (fluxRatio < 0.22f) 1.25f else 1.0f
      val damp = 0.28.pow(dt.toDouble()).toFloat()
      bot.vx = bot.vx * damp + cos(toSafeAngle) * bot.spec.thrust * urgency * dt
      bot.vy = bot.vy * damp + sin(toSafeAngle) * bot.spec.thrust * urgency * dt
      if (hypot(bot.vx, bot.vy) > 2f) {
        bot.angle = atan2(bot.vy, bot.vx)
      }
      bot.weaponAngle = bot.angle
      triggerBotDefensiveRepel(bot, triggerRadius = 360f)
      return
    }

    // Incoming ordnance dodge scan
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
            val dodgeSign = if ((bot.aiSeed.toInt() and 1) == 0) 1f else -1f
            incomingThreatAngle = atan2(b.vy, b.vx) + (PI.toFloat() * 0.5f * dodgeSign)
          }
        }
      }
    }
    for (bm in bombs) {
      if (bm.ownerId != bot.id && !isBombAlliedWithShip(bm, bot)) {
        val dist = hypot(bm.x - bot.x, bm.y - bot.y)
        val alertR = if (bm.isInfiniteProxMissile) (bm.proxLockRadius + 80f) else 460f
        if (dist < alertR) {
          threatMinDist = dist
          incomingThreatAngle = atan2(bot.y - bm.y, bot.x - bm.x)
        }
      }
    }

    val primaryTarget: ShipEntity?
    val primaryTargetDist: Float
    when {
      isFluxCoreMode && !isBotFluxCoreCarrier && fluxCoreCarrierEnemy != null && nearestDist >= 220f -> {
        primaryTarget = fluxCoreCarrierEnemy
        primaryTargetDist = fluxCoreCarrierDist
      }
      isSkirmishMode && lowestFluxSkirmishEnemy != null -> {
        primaryTarget = lowestFluxSkirmishEnemy
        primaryTargetDist = lowestFluxDist
      }
      isStasisBreachMode && enemyAttackingOurBrig != null && nearestDist >= 280f -> {
        primaryTarget = enemyAttackingOurBrig
        primaryTargetDist = enemyAttackingOurBrigDist
      }
      !isTeamBasedRankedMode() && !isFluxCoreMode && nearestHvtEnemy != null && fluxRatio >= 0.48f && nearestDist >= 290f -> {
        primaryTarget = nearestHvtEnemy
        primaryTargetDist = nearestHvtDist
      }
      else -> {
        primaryTarget = nearestEnemy
        primaryTargetDist = nearestDist
      }
    }

    stepBotToggleableAndSpecialBuffs(
      bot = bot,
      fluxRatio = fluxRatio,
      primaryTarget = primaryTarget,
      primaryTargetDist = primaryTargetDist,
      incomingThreatAngle = incomingThreatAngle,
      isFluxCoreMode = isFluxCoreMode,
      isBotFluxCoreCarrier = isBotFluxCoreCarrier,
      lowFluxRetreat = lowFluxRetreat,
    )

    if (bot.spec.supportsNativeMultifire) {
      bot.multifireOn = (primaryTarget != null && primaryTargetDist in 240f..650f) || bot.multiShotCharges > 0
    } else {
      bot.multifireOn = false
    }

    var desiredAngle: Float
    var wantsAfterburner = false

    when {
      // BRAIN 1: FLUX CORE MODE (Sprint for loose Core, intercept carrier, or kite/grab crates/avoid Safe Zone as Carrier)
      isFluxCoreMode -> {
        if (isBotFluxCoreCarrier) {
          if (nearestEnemy != null && nearestDist < 205f) {
            triggerBotDefensiveRepel(bot, triggerRadius = 205f)
          }
          val carrierCrate =
            findNearestSupplyCrateForBot(
              bot,
              maxSearchDist = if (fluxRatio < 0.72f) 1950f else 950f,
              extraSafeMargin = 310f,
            )
          desiredAngle =
            when {
              incomingThreatAngle != null -> incomingThreatAngle
              nearestEnemy != null && nearestDist < 580f -> {
                val awayFromPursuer = atan2(bot.y - nearestEnemy.y, bot.x - nearestEnemy.x)
                val strafe = if ((bot.aiSeed.toInt() and 1) == 0) 0.44f else -0.44f
                wantsAfterburner = fluxRatio > 0.42f
                awayFromPursuer + strafe
              }
              carrierCrate != null -> {
                wantsAfterburner = fluxRatio > 0.50f
                atan2(carrierCrate.y - bot.y, carrierCrate.x - bot.x)
              }
              else -> {
                val t = (System.currentTimeMillis() % 600000L) * 0.00042f
                val orbitR = (arenaRadius * 0.66f).coerceIn(1250f, 2600f)
                val gx = arenaCenterX + cos(t + bot.aiSeed) * orbitR
                val gy = arenaCenterY + sin(t * 1.12f + bot.aiSeed) * orbitR
                atan2(gy - bot.y, gx - bot.x)
              }
            }
          val toCenterDist = hypot(arenaCenterX - bot.x, arenaCenterY - bot.y).coerceAtLeast(1f)
          val toCenterX = (arenaCenterX - bot.x) / toCenterDist
          val toCenterY = (arenaCenterY - bot.y) / toCenterDist
          val steerDotCenter = cos(desiredAngle) * toCenterX + sin(desiredAngle) * toCenterY
          if (toCenterDist < getSafeZoneRadius() + 920f && steerDotCenter > 0.10f) {
            val tangSign = if ((bot.aiSeed.toInt() and 1) == 0) 1f else -1f
            val tangX = -toCenterY * tangSign - toCenterX * 0.62f
            val tangY = toCenterX * tangSign - toCenterY * 0.62f
            desiredAngle = atan2(tangY, tangX)
          }
          if (nearestEnemy != null && nearestDist < RiftCalibrationState.botGunFireRange) {
            fireBotCombatOrdnanceAt(bot, nearestEnemy, nearestDist, minFluxRatio = 0.24f)
          } else {
            bot.independentAimActive = false
            bot.weaponAngle = desiredAngle
          }
        } else if (fluxCoreCarrierEntityId.isEmpty() && !isInsideSafeZone(fluxCoreOrb.x, fluxCoreOrb.y)) {
          val distToOrb = hypot(fluxCoreOrb.x - bot.x, fluxCoreOrb.y - bot.y)
          desiredAngle = incomingThreatAngle ?: atan2(fluxCoreOrb.y - bot.y, fluxCoreOrb.x - bot.x)
          wantsAfterburner = fluxRatio > 0.35f && distToOrb > 260f
          if (primaryTarget != null && primaryTargetDist < RiftCalibrationState.botGunFireRange) {
            fireBotCombatOrdnanceAt(bot, primaryTarget, primaryTargetDist)
          } else {
            bot.weaponAngle = desiredAngle
            bot.independentAimActive = false
          }
        } else if (fluxCoreCarrierEnemy != null) {
          val leadT = min(0.75f, fluxCoreCarrierDist / bot.spec.primarySpeed.coerceAtLeast(120f))
          val interceptAngle =
            atan2(
              (fluxCoreCarrierEnemy.y + fluxCoreCarrierEnemy.vy * leadT) - bot.y,
              (fluxCoreCarrierEnemy.x + fluxCoreCarrierEnemy.vx * leadT) - bot.x,
            )
          desiredAngle =
            incomingThreatAngle
              ?: if (fluxCoreCarrierDist < 280f) {
                val strafeSign = if ((bot.aiSeed.toInt() and 1) == 0) 0.55f else -0.55f
                interceptAngle + strafeSign
              } else {
                interceptAngle
              }
          wantsAfterburner = fluxRatio > 0.36f && fluxCoreCarrierDist > 360f
          val shootTarget = if (nearestEnemy != null && nearestDist < 240f) nearestEnemy else fluxCoreCarrierEnemy
          val shootDist = if (shootTarget === nearestEnemy) nearestDist else fluxCoreCarrierDist
          fireBotCombatOrdnanceAt(bot, shootTarget, shootDist, minFluxRatio = 0.20f)
        } else {
          val fallbackCrate = findNearestSupplyCrateForBot(bot, maxSearchDist = 1400f, extraSafeMargin = 220f)
          desiredAngle =
            if (fallbackCrate != null) atan2(fallbackCrate.y - bot.y, fallbackCrate.x - bot.x)
            else atan2(arenaCenterY - bot.y, arenaCenterX - bot.x) + 1.1f
          bot.weaponAngle = desiredAngle
          bot.independentAimActive = false
        }
      }

      // BRAIN 2: RIFT CONTROL MODE (Push Alpha/Omega capture rings & dogfight contesting enemies inside the ring)
      isRiftControlMode -> {
        val myTeam = bot.freq
        val alphaLockedMine = riftAlphaLockedBy == myTeam
        val omegaLockedMine = riftOmegaLockedBy == myTeam
        val alphaContested = (myTeam == 0 && riftAlphaEastCount > 0) || (myTeam == 1 && riftAlphaWestCount > 0)
        val omegaContested = (myTeam == 0 && riftOmegaEastCount > 0) || (myTeam == 1 && riftOmegaWestCount > 0)
        val distAlpha = hypot(bot.x - riftAlphaX, bot.y - riftAlphaY)
        val distOmega = hypot(bot.x - riftOmegaX, bot.y - riftOmegaY)
        val preferAlphaBySeed = (bot.aiSeed.toInt() and 1) == 0
        val targetAlpha =
          when {
            alphaContested && !omegaContested -> true
            omegaContested && !alphaContested -> false
            !alphaLockedMine && omegaLockedMine -> true
            alphaLockedMine && !omegaLockedMine -> false
            !alphaLockedMine && !omegaLockedMine -> if (kotlin.math.abs(distAlpha - distOmega) < 520f) preferAlphaBySeed else distAlpha <= distOmega
            else -> preferAlphaBySeed
          }
        val rx = if (targetAlpha) riftAlphaX else riftOmegaX
        val ry = if (targetAlpha) riftAlphaY else riftOmegaY
        val distToRift = hypot(rx - bot.x, ry - bot.y)
        val toRiftAng = atan2(ry - bot.y, rx - bot.x)

        if (primaryTarget != null && primaryTargetDist <= 740f) {
          fireBotCombatOrdnanceAt(bot, primaryTarget, primaryTargetDist, minFluxRatio = 0.20f)
          desiredAngle =
            if (distToRift > riftCaptureRadius - 42f) {
              toRiftAng
            } else if (incomingThreatAngle != null && distToRift < riftCaptureRadius - 95f) {
              incomingThreatAngle
            } else {
              val strafeDir = if ((bot.aiSeed.toInt() and 1) == 0) 1f else -1f
              val toEnemyAng = atan2(primaryTarget.y - bot.y, primaryTarget.x - bot.x)
              if (primaryTargetDist < 250f) {
                toRiftAng + (PI.toFloat() * 0.45f * strafeDir)
              } else {
                toEnemyAng + (PI.toFloat() * 0.36f * strafeDir)
              }
            }
        } else {
          wantsAfterburner = fluxRatio > 0.45f && distToRift > riftCaptureRadius + 210f
          val orbitDir = if ((bot.aiSeed.toInt() and 1) == 0) 1f else -1f
          desiredAngle =
            if (distToRift > 180f) toRiftAng else (toRiftAng + PI.toFloat() * 0.5f * orbitDir)
          if (primaryTarget != null && primaryTargetDist < RiftCalibrationState.botGunFireRange) {
            fireBotCombatOrdnanceAt(bot, primaryTarget, primaryTargetDist)
          } else {
            bot.weaponAngle = desiredAngle
            bot.independentAimActive = false
          }
        }
      }

      // BRAIN 3: STASIS BREACH FREE-ROAM (Hunt unjailed enemies, defend friendly Brig from raiders, relieve imprisoned allies)
      isStasisBreachMode -> {
        val imprisonedAllies = ships.count { it.freq == bot.freq && it.isInStasisBrig && it.respawnTimer <= 0f }
        val jailedEnemies = ships.count { it.freq != bot.freq && it.isInStasisBrig && it.respawnTimer <= 0f }
        val defenderRole = (bot.aiSeed.toInt() % 3 == 0) && jailedEnemies >= 2
        if (incomingThreatAngle != null) {
          desiredAngle = incomingThreatAngle
          if (primaryTarget != null && primaryTargetDist < RiftCalibrationState.botGunFireRange) {
            fireBotCombatOrdnanceAt(bot, primaryTarget, primaryTargetDist)
          }
        } else if (defenderRole && enemyAttackingOurBrig != null) {
          fireBotCombatOrdnanceAt(bot, enemyAttackingOurBrig, enemyAttackingOurBrigDist, minFluxRatio = 0.22f)
          wantsAfterburner = fluxRatio > 0.45f && enemyAttackingOurBrigDist > 480f
          desiredAngle = atan2(enemyAttackingOurBrig.y - bot.y, enemyAttackingOurBrig.x - bot.x)
        } else if (imprisonedAllies >= 3 && (primaryTarget == null || primaryTargetDist > 480f)) {
          val enemyBrigX = if (bot.freq == 0) eastBrigReactorX else westBrigReactorX
          val enemyBrigY = if (bot.freq == 0) eastBrigReactorY else westBrigReactorY
          val distToEnemyBrig = hypot(enemyBrigX - bot.x, enemyBrigY - bot.y)
          val toBrigAng = atan2(enemyBrigY - bot.y, enemyBrigX - bot.x)
          wantsAfterburner = fluxRatio > 0.48f && distToEnemyBrig > stasisJailRadius + 300f
          val orbitDir = if ((bot.aiSeed.toInt() and 1) == 0) 1f else -1f
          desiredAngle =
            if (distToEnemyBrig > stasisJailRadius + 95f) toBrigAng else (toBrigAng + PI.toFloat() * 0.5f * orbitDir)
          if (primaryTarget != null && primaryTargetDist < RiftCalibrationState.botGunFireRange) {
            fireBotCombatOrdnanceAt(bot, primaryTarget, primaryTargetDist)
          } else {
            bot.weaponAngle = desiredAngle
            bot.independentAimActive = false
          }
        } else if (primaryTarget != null) {
          fireBotCombatOrdnanceAt(bot, primaryTarget, primaryTargetDist)
          val aimAngle = bot.weaponAngle
          desiredAngle =
            when {
              primaryTargetDist < 340f ->
                atan2(bot.y - primaryTarget.y, bot.x - primaryTarget.x) +
                  (if ((bot.aiSeed.toInt() and 1) == 0) 0.62f else -0.62f)
              primaryTargetDist > 720f -> {
                wantsAfterburner = fluxRatio > 0.54f && primaryTargetDist > 1050f
                aimAngle
              }
              else -> aimAngle + (PI.toFloat() * 0.44f * if ((bot.aiSeed.toInt() and 1) == 0) 1f else -1f)
            }
        } else {
          val guardR = stasisJailRadius + 180f
          val orbitAng = (System.currentTimeMillis() % 600000L) * 0.0005f + bot.aiSeed
          val gx = myTeamBrigX + cos(orbitAng) * guardR
          val gy = myTeamBrigY + sin(orbitAng) * guardR
          desiredAngle = atan2(gy - bot.y, gx - bot.x)
          bot.weaponAngle = desiredAngle
          bot.independentAimActive = false
        }
      }

      // BRAIN 4: DEATHMATCH SQUAD COORDINATION (Rift Skirmish & Syndicate Wars — wing formation, crate control & focus-fire)
      isSkirmishMode -> {
        val wingAnchor =
          ships
            .filter { it.id != bot.id && areAllied(bot, it) && it.respawnTimer <= 0f && !(isInLobbyStaging && it.isLocalPlayer) }
            .minByOrNull { hypot(it.x - bot.x, it.y - bot.y) }
        val wingCrate = if (bot.specialWeaponCharges == 0 || fluxRatio < 0.78f) findNearestSupplyCrateForBot(bot, maxSearchDist = 720f) else null

        if (incomingThreatAngle != null) {
          desiredAngle = incomingThreatAngle
          if (bot.spec.bombType == "PHASE_DASH" && bot.bombTimer <= 0f && bot.flux > 450f) {
            fireSecondaryBomb(bot, incomingThreatAngle)
          }
          if (primaryTarget != null && primaryTargetDist < RiftCalibrationState.botGunFireRange) {
            fireBotCombatOrdnanceAt(bot, primaryTarget, primaryTargetDist)
          }
        } else if (wingCrate != null && (primaryTarget == null || primaryTargetDist > 520f)) {
          desiredAngle = atan2(wingCrate.y - bot.y, wingCrate.x - bot.x)
          if (primaryTarget != null && primaryTargetDist < RiftCalibrationState.botGunFireRange) {
            fireBotCombatOrdnanceAt(bot, primaryTarget, primaryTargetDist)
          } else {
            bot.weaponAngle = desiredAngle
            bot.independentAimActive = false
          }
        } else if (primaryTarget != null) {
          fireBotCombatOrdnanceAt(bot, primaryTarget, primaryTargetDist, minFluxRatio = 0.25f)
          val aimAngle = bot.weaponAngle
          val allyDist = if (wingAnchor != null) hypot(wingAnchor.x - bot.x, wingAnchor.y - bot.y) else 0f
          desiredAngle =
            if (allyDist > 560f && primaryTargetDist > 480f && wingAnchor != null) {
              atan2((primaryTarget.y + wingAnchor.y) * 0.5f - bot.y, (primaryTarget.x + wingAnchor.x) * 0.5f - bot.x)
            } else if (primaryTargetDist < 380f) {
              atan2(bot.y - primaryTarget.y, bot.x - primaryTarget.x) +
                (if ((bot.aiSeed.toInt() and 1) == 0) 0.68f else -0.68f)
            } else if (primaryTargetDist > 760f) {
              wantsAfterburner = fluxRatio > 0.62f && primaryTargetDist > 1150f
              aimAngle
            } else {
              aimAngle + (PI.toFloat() * 0.46f * if ((bot.aiSeed.toInt() and 1) == 0) 1f else -1f)
            }
        } else if (wingAnchor != null && hypot(wingAnchor.x - bot.x, wingAnchor.y - bot.y) > 340f) {
          desiredAngle = atan2(wingAnchor.y - bot.y, wingAnchor.x - bot.x)
          bot.weaponAngle = desiredAngle
          bot.independentAimActive = false
        } else {
          val t = (System.currentTimeMillis() % 600000L) * 0.00038f
          val orbitRadius = (arenaRadius * 0.56f).coerceIn(980f, 2200f)
          val gx = arenaCenterX + cos(t + bot.aiSeed) * orbitRadius
          val gy = arenaCenterY + sin(t * 1.15f + bot.aiSeed) * orbitRadius
          desiredAngle = atan2(gy - bot.y, gx - bot.x)
          bot.weaponAngle = desiredAngle
          bot.independentAimActive = false
        }
      }

      // BRAIN 5: SECTOR CONVERGENCE (HVT bounty hunting, aggressive crate stacking & safe standoff positioning)
      else -> {
        val ffaCrate =
          findNearestSupplyCrateForBot(
            bot,
            maxSearchDist = if (bot.specialWeaponCharges < 2 || fluxRatio < 0.76f) 980f else 480f,
          )
        val nearbyEnemyCount =
          ships.count {
            !(isInLobbyStaging && it.isLocalPlayer) &&
              !areAllied(bot, it) &&
              it.respawnTimer <= 0f &&
              !it.isCloaked &&
              hypot(it.x - bot.x, it.y - bot.y) < 580f
          }
        if (incomingThreatAngle != null) {
          desiredAngle = incomingThreatAngle
          if (bot.spec.bombType == "PHASE_DASH" && bot.bombTimer <= 0f && bot.flux > 450f) {
            fireSecondaryBomb(bot, incomingThreatAngle)
          }
          if (primaryTarget != null && primaryTargetDist < RiftCalibrationState.botGunFireRange) {
            fireBotCombatOrdnanceAt(bot, primaryTarget, primaryTargetDist)
          }
        } else if (nearbyEnemyCount >= 3 && fluxRatio < 0.65f && nearestEnemy != null) {
          // Disengage safely from crowded multi-ship crossfire
          val escapeAng = atan2(bot.y - nearestEnemy.y, bot.x - nearestEnemy.x) + 0.45f
          desiredAngle = if (ffaCrate != null) atan2(ffaCrate.y - bot.y, ffaCrate.x - bot.x) else escapeAng
          wantsAfterburner = fluxRatio > 0.44f
          if (primaryTarget != null && primaryTargetDist < RiftCalibrationState.botGunFireRange) {
            fireBotCombatOrdnanceAt(bot, primaryTarget, primaryTargetDist)
          }
        } else if (ffaCrate != null && (primaryTarget == null || (primaryTargetDist > 410f && nearestHvtEnemy == null))) {
          desiredAngle = atan2(ffaCrate.y - bot.y, ffaCrate.x - bot.x)
          wantsAfterburner = fluxRatio > 0.64f && hypot(ffaCrate.x - bot.x, ffaCrate.y - bot.y) > 480f
          if (primaryTarget != null && primaryTargetDist < RiftCalibrationState.botGunFireRange) {
            fireBotCombatOrdnanceAt(bot, primaryTarget, primaryTargetDist)
          } else {
            bot.weaponAngle = desiredAngle
            bot.independentAimActive = false
          }
        } else if (primaryTarget != null && primaryTargetDist < max(RiftCalibrationState.botEngageRange, 1850f)) {
          fireBotCombatOrdnanceAt(bot, primaryTarget, primaryTargetDist)
          val aimAngle = bot.weaponAngle
          val isHuntingHvt = primaryTarget === nearestHvtEnemy
          desiredAngle =
            when {
              primaryTargetDist < 380f ->
                atan2(bot.y - primaryTarget.y, bot.x - primaryTarget.x) +
                  (if ((bot.aiSeed.toInt() and 1) == 0) 0.66f else -0.66f)
              primaryTargetDist > 720f -> {
                wantsAfterburner = fluxRatio > 0.52f && (isHuntingHvt || primaryTargetDist > 1080f)
                aimAngle
              }
              else -> aimAngle + (PI.toFloat() * 0.45f * if ((bot.aiSeed.toInt() and 1) == 0) 1f else -1f)
            }
        } else {
          bot.independentAimActive = false
          val t = (System.currentTimeMillis() % 600000L) * 0.00035f
          val orbitRadius = (arenaRadius * 0.62f).coerceIn(1150f, 2400f)
          val gx = arenaCenterX + cos(t + bot.aiSeed) * orbitRadius
          val gy = arenaCenterY + sin(t * 1.15f + bot.aiSeed) * orbitRadius
          desiredAngle = atan2(gy - bot.y, gx - bot.x)
          bot.weaponAngle = bot.angle
        }
      }
    }

    desiredAngle = steerBotAwayFromVolatileRifts(bot, desiredAngle)
    if (wallHit != null) {
      val steerX = cos(desiredAngle) + wallHit.normalX * 1.8f
      val steerY = sin(desiredAngle) + wallHit.normalY * 1.8f
      desiredAngle = atan2(steerY, steerX)
    }

    val canAfterburn = wantsAfterburner && bot.flux > 90f && bot.rocketBoostTimer <= 0f
    val afterburnAccelMult = if (canAfterburn) RiftCalibrationState.afterburnerThrustMult else 0.88f
    if (canAfterburn) {
      bot.afterburning = true
      val cloakAbMult = if (bot.isCloaked) 0.5f else 1.0f
      selfDrainFlux(bot, RiftCalibrationState.afterburnerDrainPerSec * cloakAbMult * dt)
    }

    val turnMult = 1f + bot.turnOverchargeStacks * 0.12f
    val thrustSurgeMult = (1f + bot.thrustOverchargeStacks * RiftCalibrationState.overchargeSurgePctPerStack) * (currentCustomGame?.speedMult ?: 1.0f)
    val sideDamp = (0.34 / turnMult.coerceAtLeast(1f)).pow(dt.toDouble()).toFloat()
    bot.vx = bot.vx * sideDamp + cos(desiredAngle) * bot.spec.thrust * thrustSurgeMult * turnMult * afterburnAccelMult * dt
    bot.vy = bot.vy * sideDamp + sin(desiredAngle) * bot.spec.thrust * thrustSurgeMult * turnMult * afterburnAccelMult * dt
    if (hypot(bot.vx, bot.vy) > 2f) {
      bot.angle = atan2(bot.vy, bot.vx)
    }
  }

  private fun stepContinuousToggleBuffs(ship: ShipEntity, dt: Float) {
    if (ship.respawnTimer > 0f || !ship.canFire || isInsideSafeZone(ship.x, ship.y) || ship.empTimer > 0f) {
      if (ship.tractorBeamActive || ship.lanceBeamActive) {
        clearShipToggleBuffs(ship, clearSlotIfActive = true)
      }
      return
    }

    val aimAngle =
      if (ship.independentAimActive) {
        ship.weaponAngle
      } else {
        ship.angle
      }

    // 1. Toggleable Tractor Beam: laser web slows and pulls caught ship toward user while draining Flux
    if (ship.tractorBeamActive) {
      val drain = 112f * dt
      selfDrainFlux(ship, drain)
      if (ship.flux <= 45f) {
        clearShipToggleBuffs(ship, clearSlotIfActive = true)
        if (ship.isLocalPlayer) {
          sfx.playMutedClick()
          onPrizeToast("TRACTOR BEAM DEPLETED (FLUX FLOOR)")
        }
        return
      }
      val maxWebRange = 560f
      var caught =
        ships.firstOrNull {
          it.id == ship.tractorTargetShipId &&
            !(isInLobbyStaging && it.isLocalPlayer) &&
            !areAllied(ship, it) &&
            it.respawnTimer <= 0f &&
            !it.isCloaked &&
            !isInsideSafeZone(it.x, it.y) &&
            hypot(it.x - ship.x, it.y - ship.y) <= maxWebRange + 65f &&
            hasClearBotLineOfSight(ship.x, ship.y, it.x, it.y)
        }
      if (caught == null) {
        var bestScore = Float.MAX_VALUE
        for (cand in ships) {
          if (
            !(isInLobbyStaging && cand.isLocalPlayer) &&
              !areAllied(ship, cand) &&
              cand.respawnTimer <= 0f &&
              !cand.isCloaked &&
              !isInsideSafeZone(cand.x, cand.y)
          ) {
            val dx = cand.x - ship.x
            val dy = cand.y - ship.y
            val dist = hypot(dx, dy)
            if (dist > maxWebRange || dist < 1f) continue
            val angDiff = kotlin.math.abs(atan2(sin(atan2(dy, dx) - aimAngle), cos(atan2(dy, dx) - aimAngle)))
            if (angDiff <= 0.92f && hasClearBotLineOfSight(ship.x, ship.y, cand.x, cand.y)) {
              val score = dist * (0.55f + angDiff * 0.7f)
              if (score < bestScore) {
                bestScore = score
                caught = cand
              }
            }
          }
        }
      }
      ship.tractorTargetShipId = caught?.id ?: ""
      if (caught != null) {
        val slowDamp = 0.16.pow(dt.toDouble()).toFloat()
        caught.vx *= slowDamp
        caught.vy *= slowDamp
        val pullDx = ship.x - caught.x
        val pullDy = ship.y - caught.y
        val pullDist = hypot(pullDx, pullDy).coerceAtLeast(1f)
        if (pullDist > ship.radius + caught.radius + 14f) {
          val pullAccel = 690f * (currentCustomGame?.speedMult ?: 1.0f)
          caught.vx += (pullDx / pullDist) * pullAccel * dt
          caught.vy += (pullDy / pullDist) * pullAccel * dt
        }
      }
    }

    // 2. Toggleable Continuous Lance Beam: stops on FIRST ship/obstacle hit (no penetration), ticks damage every 0.5s, scales wider/longer with stacks
    if (ship.lanceBeamActive) {
      val stacks = ship.lanceActiveStacks.coerceIn(1, 6)
      val drain = (132f + (stacks - 1) * 22f) * dt
      selfDrainFlux(ship, drain)
      if (ship.flux <= 45f) {
        clearShipToggleBuffs(ship, clearSlotIfActive = true)
        if (ship.isLocalPlayer) {
          sfx.playMutedClick()
          onPrizeToast("LANCE BEAM DEPLETED (FLUX FLOOR)")
        }
        return
      }

      val beamLen = 620f + (stacks - 1) * 165f
      val beamWidth = 14f + (stacks - 1) * 6.5f
      ship.lanceRenderWidthPx = beamWidth

      val dirX = cos(aimAngle)
      val dirY = sin(aimAngle)
      val startX = ship.x + dirX * (ship.radius + 6f)
      val startY = ship.y + dirY * (ship.radius + 6f)
      val maxEndX = startX + dirX * beamLen
      val maxEndY = startY + dirY * beamLen

      var bestT = 1.0f
      var hitKind = 0 // 0 = none, 1 = wall/obstacle, 2 = ship, 3 = decoy, 4 = brigCore, 5 = boundary/safeZone
      var hitWallIdx = -1
      var hitShip: ShipEntity? = null
      var hitDecoyIdx = -1
      var hitBrigFreq = -1

      // A. Raycast walls & placed Stasis Barriers
      val wallImpact = segmentFirstWallHit(startX, startY, maxEndX, maxEndY, pad = (beamWidth * 0.28f).coerceAtMost(10f))
      if (wallImpact != null && wallImpact.t < bestT) {
        bestT = wallImpact.t.coerceIn(0f, 1f)
        hitKind = 1
        hitWallIdx = wallImpact.wallIndex
      }

      // Helper for ray-circle entry t along [0..1]
      fun rayCircleFirstT(cx: Float, cy: Float, rad: Float): Float? {
        val fx = startX - cx
        val fy = startY - cy
        val segDx = maxEndX - startX
        val segDy = maxEndY - startY
        val a = segDx * segDx + segDy * segDy
        if (a <= 0.0001f) return null
        val b = 2f * (fx * segDx + fy * segDy)
        val c = fx * fx + fy * fy - rad * rad
        if (c <= 0f) return 0f
        val disc = b * b - 4f * a * c
        if (disc < 0f) return null
        val t0 = (-b - sqrt(disc)) / (2f * a)
        return if (t0 in 0f..1f) t0 else null
      }

      // B. Safe Zone outer rim (if active)
      if (hasActiveSafeZone()) {
        val szT = rayCircleFirstT(arenaCenterX, arenaCenterY, getSafeZoneRadius() + 4f)
        if (szT != null && szT < bestT) {
          bestT = szT
          hitKind = 5
        }
      }

      // C. Outer Storm Wall boundary
      val segDx = maxEndX - startX
      val segDy = maxEndY - startY
      val endCenterDist = hypot(maxEndX - arenaCenterX, maxEndY - arenaCenterY)
      if (endCenterDist > arenaRadius) {
        val fx = startX - arenaCenterX
        val fy = startY - arenaCenterY
        val a = segDx * segDx + segDy * segDy
        val b = 2f * (fx * segDx + fy * segDy)
        val c = fx * fx + fy * fy - arenaRadius * arenaRadius
        val disc = b * b - 4f * a * c
        if (disc >= 0f && a > 0.0001f) {
          val tExit = (-b + sqrt(disc)) / (2f * a)
          if (tExit in 0f..bestT) {
            bestT = tExit
            hitKind = 5
          }
        }
      }

      // D. Stasis Brig Reactor Core (when imprisoned in Stasis Breach)
      if (isStasisBreachRulesActive() && ship.isInStasisBrig) {
        val targetBrigFreq = if (ship.freq == 0) 1 else 0
        val coreX = if (targetBrigFreq == 1) eastBrigReactorX else westBrigReactorX
        val coreY = if (targetBrigFreq == 1) eastBrigReactorY else westBrigReactorY
        val coreT = rayCircleFirstT(coreX, coreY, brigReactorRadius + beamWidth * 0.45f)
        if (coreT != null && coreT < bestT) {
          bestT = coreT
          hitKind = 4
          hitBrigFreq = targetBrigFreq
        }
      }

      // E. Hostile Holo Phantom Decoys
      for (dIdx in decoys.indices) {
        val dec = decoys[dIdx]
        if (dec.ownerId != ship.id && dec.freq != ship.freq && !isInsideSafeZone(dec.x, dec.y)) {
          val dT = rayCircleFirstT(dec.x, dec.y, 36f + beamWidth * 0.45f)
          if (dT != null && dT < bestT) {
            bestT = dT
            hitKind = 3
            hitDecoyIdx = dIdx
          }
        }
      }

      // F. Hostile Ships (strictly stops on the VERY FIRST ship hit — zero penetration!)
      for (cand in ships) {
        if (
          cand.id != ship.id &&
            !(isInLobbyStaging && cand.isLocalPlayer) &&
            !areAllied(ship, cand) &&
            cand.respawnTimer <= 0f &&
            !isInsideSafeZone(cand.x, cand.y)
        ) {
          val sT = rayCircleFirstT(cand.x, cand.y, cand.radius + beamWidth * 0.45f)
          if (sT != null && sT < bestT) {
            bestT = sT
            hitKind = 2
            hitShip = cand
          }
        }
      }

      val stopX = startX + segDx * bestT
      val stopY = startY + segDy * bestT
      ship.lanceRenderEndX = stopX
      ship.lanceRenderEndY = stopY
      ship.lanceHitSparkActive = hitKind != 0

      ship.lanceTickTimer -= dt
      if (ship.lanceTickTimer <= 0f) {
        ship.lanceTickTimer = 0.50f
        val customDmgMult = currentCustomGame?.damageMult ?: 1.0f
        val tickDmg = (265f + (stacks - 1) * 48f) * customDmgMult
        when (hitKind) {
          1 -> {
            if (hitWallIdx in walls.indices) {
              onObstacleProjectileHit(
                wallIndex = hitWallIdx,
                damage = tickDmg,
                hitX = stopX,
                hitY = stopY,
                attackerId = ship.id,
              )
            }
          }
          2 -> {
            val victim = hitShip
            if (victim != null) {
              applyProjectileDamage(
                target = victim,
                attackerId = ship.id,
                attackerIsHuman = ship.isHuman,
                rawDamage = tickDmg,
                impactAngle = aimAngle + PI.toFloat(),
                isHeavyImpact = stacks >= 2,
                isEmp = false,
                attackerShipLabel = formatShipLabel(ship),
                weaponLabel = if (stacks > 1) "Lance Beam x$stacks" else "Lance Beam",
              )
              if (ship.isLocalPlayer) {
                currentLifeStats.shotsHit++
                sfx.playLaser()
              }
            }
          }
          3 -> {
            if (hitDecoyIdx in decoys.indices) {
              val dec = decoys.removeAt(hitDecoyIdx)
              spawnExplosion(dec.x, dec.y, Color.parseColor("#2DD4BF"))
            }
          }
          4 -> {
            if (hitBrigFreq >= 0) {
              damageBrigReactorCore(
                targetBrigFreq = hitBrigFreq,
                attacker = ship,
                rawDamage = tickDmg,
              )
            }
          }
        }
      }
    }
  }

  fun stepSimulation(rawDt: Float) {
    val inHubOverwatch = isHubOverwatchActive && !hasJoinedMatch
    val prevSfxEnabled = sfx.enabled
    if (inHubOverwatch) {
      sfx.enabled = false
      isInLobbyStaging = true
      localPlayer.respawnTimer = 999f
      localPlayer.deathLingerTimer = 999f
      localPlayer.isInStasisBrig = false
      localPlayer.x = arenaCenterX
      localPlayer.y = arenaCenterY
      localPlayer.vx = 0f
      localPlayer.vy = 0f
    }
    try {
      stepSimulationInternal(rawDt)
    } finally {
      if (inHubOverwatch) {
        sfx.enabled = prevSfxEnabled
        localPlayer.respawnTimer = 999f
        localPlayer.deathLingerTimer = 999f
        localPlayer.isInStasisBrig = false
        localPlayer.x = arenaCenterX
        localPlayer.y = arenaCenterY
        localPlayer.vx = 0f
        localPlayer.vy = 0f
      }
    }
  }

  private fun stepSimulationInternal(rawDt: Float) {
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

    // Unjoined Startup / NEXUS Orbit Mode (when Overwatch is NOT active): peacefully orbit with 0 combat ships
    if (!hasJoinedMatch && !isHubOverwatchActive) {
      lobbyOrbitAngle = (lobbyOrbitAngle + rawDt * 0.16f) % (PI.toFloat() * 2f)
      localPlayer.x = arenaCenterX + cos(lobbyOrbitAngle) * 760f
      localPlayer.y = arenaCenterY + sin(lobbyOrbitAngle) * 760f
      localPlayer.angle = lobbyOrbitAngle + PI.toFloat() * 0.5f
      localPlayer.weaponAngle = localPlayer.angle
      localPlayer.vx = 0f
      localPlayer.vy = 0f
      localPlayer.flux = localPlayer.maxFlux
      localPlayer.visualBankRoll = 0.22f
      localPlayer.barrelRollTimer = 0f
      for (g in greens) {
        g.spinPhase = (g.spinPhase + rawDt * 1.8f) % (PI.toFloat() * 2f)
      }
      stepSpatialAnomalies(dt, rawDt)
      for (i in particles.indices.reversed()) {
        val p = particles[i]
        p.life -= rawDt
        if (p.life <= 0f) particles.removeAt(i) else {
          p.x += p.vx * dt
          p.y += p.vy * dt
        }
      }
      return
    }

    if (skirmish1v1AlertTimer > 0f) {
      skirmish1v1AlertTimer = max(0f, skirmish1v1AlertTimer - rawDt)
    }

    // Smooth abyssal-garden visual blend & 10-second between-round Deathmatch Ready-Up countdown
    if (isSkirmishRulesActive()) {
      if (skirmishReadyCountdownSec > 0f) {
        skirmishReadyCountdownSec = max(0f, skirmishReadyCountdownSec - rawDt)
        if (skirmishReadyCountdownSec <= 6.5f) {
          sfx.setDynamicEscalationTargetBpm(null)
          skirmishAbyssalBlend = max(0f, skirmishAbyssalBlend - rawDt * 0.28f)
        }
        if (skirmishReadyCountdownSec <= 0f) {
          skirmishReadyCountdownSec = 0f
          skirmishAbyssalBlend = 0f
          sfx.setDynamicEscalationTargetBpm(null)
          for (s in ships) {
            s.weaponsLocked = false
            s.canFire = true
            s.weaponState = "ARMED"
            s.warpInAnimTimer = 0.55f
            s.phaseShieldTimer = RiftCalibrationState.exitPhaseShieldSec
          }
          if (hasJoinedMatch && !isInLobbyStaging) {
            sfx.playWarp()
            onPrizeToast("ROUND $skirmishRound LIVE — WEAPONS HOT!")
          }
        }
      } else if (skirmishIs1v1Escalated) {
        skirmishAbyssalBlend = min(1f, skirmishAbyssalBlend + rawDt * 0.24f)
      } else {
        skirmishAbyssalBlend = max(0f, skirmishAbyssalBlend - rawDt * 0.35f)
      }
    } else {
      skirmishAbyssalBlend = max(0f, skirmishAbyssalBlend - rawDt * 0.45f)
    }

    val inDeathmatchReadyHold = isSkirmishRulesActive() && skirmishReadyCountdownSec > 0f

    // 1. Local Player Thrust, Afterburner, Hyper Surge, Overcharge Surge, Manual HUD Brake & Live Life Stats
    if (isInLobbyStaging) {
      localPlayer.isIdleDraining = false
      localPlayer.playfieldIdleTimer = 0f
      localPlayer.safeZoneStayTimer = 0f
      localPlayer.safeZoneLockoutTimer = 0f
      localPlayer.vx = 0f
      localPlayer.vy = 0f
      localPlayer.afterburning = false
      localPlayer.flux = localPlayer.maxFlux
    } else if (inDeathmatchReadyHold) {
      localPlayer.vx = 0f
      localPlayer.vy = 0f
      localPlayer.afterburning = false
      localPlayer.weaponsLocked = true
      localPlayer.canFire = false
      localPlayer.weaponState = "READY-UP"
      localPlayer.flux = localPlayer.maxFlux
    } else if (localPlayer.respawnTimer <= 0f) {
      currentLifeStats.timeSurvivedSec += rawDt
      currentLifeStats.bountyReached = max(currentLifeStats.bountyReached, localPlayer.bounty)
      currentLifeStats.shipId = localPlayer.spec.id
      currentLifeStats.shipFlown = formatShipLabel(localPlayer)

      val localInSafe = isInsideSafeZone(localPlayer.x, localPlayer.y)
      localPlayer.inSafeZone = localInSafe
      localPlayer.weaponsLocked = localInSafe
      localPlayer.canFire = !localInSafe
      localPlayer.weaponState = if (localInSafe) "DISARMED" else "ARMED"
      val wasAfterburning = localPlayer.afterburning
      localPlayer.afterburning = false
      val turnMult = 1f + localPlayer.turnOverchargeStacks * 0.12f
      val thrustSurgeMult = (1f + localPlayer.thrustOverchargeStacks * RiftCalibrationState.overchargeSurgePctPerStack) * (currentCustomGame?.speedMult ?: 1.0f)
      val rocketMult = if (localPlayer.rocketBoostTimer > 0f) RiftCalibrationState.hyperSurgeSpeedMult else 1.0f

      if (leftStickActive && leftStickMag > 0.08f) {
        autoBraking = false
        val isAfterburner = leftStickMag > 0.80f && localPlayer.flux > 25f && localPlayer.rocketBoostTimer <= 0f
        val accel =
          localPlayer.spec.thrust *
            thrustSurgeMult *
            turnMult *
            rocketMult *
            (if (isAfterburner) RiftCalibrationState.afterburnerThrustMult else leftStickMag)
        localPlayer.vx += cos(leftStickAngle) * accel * dt
        localPlayer.vy += sin(leftStickAngle) * accel * dt

        if (isAfterburner) {
          localPlayer.afterburning = true
          val cloakAbMult = if (localPlayer.isCloaked) 0.5f else 1.0f
          selfDrainFlux(localPlayer, RiftCalibrationState.afterburnerDrainPerSec * cloakAbMult * dt)
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
      if (localPlayer.afterburning != wasAfterburning && !isEphemeralPracticeRift) {
        notifyLocalLifecycleSync()
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

    // Step Repel-pushed Supply Crates with circular perimeter & wall bounce and smooth damping to rest
    for (g in greens) {
      val gSpeed = hypot(g.vx, g.vy)
      if (gSpeed > 1f) {
        val nx = g.x + g.vx * dt
        val ny = g.y + g.vy * dt
        if (circleIntersectsWalls(nx, g.y, 38f) || circleIntersectsActiveAnomalies(nx, g.y, 48f)) {
          g.vx = -g.vx * 0.7f
        } else {
          g.x = nx
        }
        if (circleIntersectsWalls(g.x, ny, 38f) || circleIntersectsActiveAnomalies(g.x, ny, 48f)) {
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

    // Step 45.0s Placed Tethered Hard-Light Stasis Barriers
    var removedBarrier = false
    for (bwIdx in brickWalls.indices.reversed()) {
      val bw = brickWalls[bwIdx]
      bw.life -= rawDt
      if (bw.life <= 0f) {
        shockwaves.add(
          ShockwaveEntity(
            bw.x + bw.w * 0.5f,
            bw.y + bw.h * 0.5f,
            12f,
            95f,
            Color.parseColor("#00F5D4"),
          )
        )
        brickWalls.removeAt(bwIdx)
        removedBarrier = true
      }
    }
    if (removedBarrier) {
      bakeStaticRadarBitmap()
    }

    // Step obstacle hit-flash timers & 20.0s shattered Crystal Asteroid respawns
    if (gravityRadarBakeTimer > 0f) {
      gravityRadarBakeTimer = max(0f, gravityRadarBakeTimer - rawDt)
    }
    for (w in walls) {
      if (w.hitFlashTimer > 0f) {
        w.hitFlashTimer = max(0f, w.hitFlashTimer - rawDt)
      }
    }
    if (pendingAsteroidRespawns.isNotEmpty()) {
      var respawnedAny = false
      for (paIdx in pendingAsteroidRespawns.indices.reversed()) {
        val pa = pendingAsteroidRespawns[paIdx]
        pa.timerSec -= rawDt
        if (pa.timerSec <= 0f) {
          val blockedByShip =
            ships.any { s ->
              s.respawnTimer <= 0f &&
                s.x >= pa.x - 40f &&
                s.x <= pa.x + pa.w + 40f &&
                s.y >= pa.y - 40f &&
                s.y <= pa.y + pa.h + 40f
            }
          if (blockedByShip) {
            pa.timerSec = 1.0f
          } else {
            walls.add(
              WallRect(
                x = pa.x,
                y = pa.y,
                w = pa.w,
                h = pa.h,
                style = pa.style,
                variantSeed = pa.variantSeed,
                hp = 2400f,
                maxHp = 2400f,
              )
            )
            shockwaves.add(
              ShockwaveEntity(
                pa.x + pa.w * 0.5f,
                pa.y + pa.h * 0.5f,
                16f,
                195f,
                Color.parseColor("#A855F7"),
              )
            )
            pendingAsteroidRespawns.removeAt(paIdx)
            respawnedAny = true
          }
        }
      }
      if (respawnedAny) {
        relocateOverlappingCrates()
        bakeStaticRadarBitmap()
      }
    }

    // Step Interdictor Stasis Bubbles (15s two-way containment domes)
    for (ibIdx in interdictorBubbles.indices.reversed()) {
      val ib = interdictorBubbles[ibIdx]
      ib.life -= rawDt
      if (ib.lastIngressTimer > 0f) ib.lastIngressTimer = max(0f, ib.lastIngressTimer - rawDt)
      if (ib.secondaryIngressTimer > 0f) ib.secondaryIngressTimer = max(0f, ib.secondaryIngressTimer - rawDt)
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
    if (practiceTutorialStepJumpFlashSec > 0f) {
      practiceTutorialStepJumpFlashSec = max(0f, practiceTutorialStepJumpFlashSec - rawDt)
    }
    if (isEphemeralPracticeRift && practiceTutorialActive) {
      ensurePracticeTutorialTargetsPresent()
    }
    for (s in ships) {
      s.matchPresenceTimeSec += rawDt
    }
    if (!isEphemeralPracticeRift && publicMatchDurationSec >= publicMatchCycleLimitSec) {
      concludePublicFfaMatchCycle(forceSimulateWinWithPresence = false)
    }

    // 2. Step all ships
    for (s in ships) {
      if (isInLobbyStaging && s.isLocalPlayer) {
        s.isIdleDraining = false
        s.playfieldIdleTimer = 0f
        s.safeZoneStayTimer = 0f
        s.safeZoneLockoutTimer = 0f
        s.vx = 0f
        s.vy = 0f
        s.afterburning = false
        s.flux = s.maxFlux
        continue
      }
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
      if (s.warpInAnimTimer > 0f) {
        s.warpInAnimTimer = max(0f, s.warpInAnimTimer - rawDt)
      }
      if (s.lastBarrelRollCooldown > 0f) {
        s.lastBarrelRollCooldown = max(0f, s.lastBarrelRollCooldown - rawDt)
      }
      if (s.hullImpactJoltTimer > 0f) {
        s.hullImpactJoltTimer = max(0f, s.hullImpactJoltTimer - rawDt)
      }
      if (s.netLocalKillGraceSec > 0f) {
        s.netLocalKillGraceSec = max(0f, s.netLocalKillGraceSec - rawDt)
      }
      if (s.respawnTimer > 0f) {
        if (s.deathLingerTimer > 0f) {
          s.deathLingerTimer = max(0f, s.deathLingerTimer - rawDt)
        }
        if (s.isLocalPlayer && isSkirmishRulesActive() && skirmishReadyCountdownSec <= 0f && s.deathLingerTimer <= 0f) {
          val livingAllies = getLivingSpectatableTeammates()
          val curTarget = livingAllies.firstOrNull { it.id == spectatingTargetShipId }
          if (curTarget == null) {
            spectatingTargetShipId = livingAllies.maxByOrNull { it.bounty }?.id ?: ""
          }
        }
        if (isSkirmishRulesActive()) {
          continue
        }
        if (s.isHuman && !s.isLocalPlayer && (s.netWasRespawning || s.netLocalKillGraceSec > 0f)) {
          s.netRespawnHoldSec += rawDt
          if (s.netRespawnHoldSec < 5.5f) {
            s.respawnTimer = max(0.20f, s.respawnTimer - rawDt)
            continue
          }
          s.netWasRespawning = false
          s.netLocalKillGraceSec = 0f
          s.netRespawnHoldSec = 0f
        }
        s.respawnTimer -= rawDt
        if (s.respawnTimer <= 0f) {
          s.respawnTimer = 0f
          s.deathLingerTimer = 0f
          s.warpInAnimTimer = 0.65f
          s.hullImpactJoltTimer = 0f
          s.lastBarrelRollCooldown = 0f
          val pos =
            when {
              isStasisBreachRulesActive() && s.isInStasisBrig -> getStasisBrigPrisonerSpawnPosition(s.freq)
              isTeamBasedRankedMode() || !hasActiveSafeZone() -> getSkirmishGateSpawnPosition(s.freq)
              s.isLocalPlayer && isEphemeralPracticeRift -> getPracticeRiftOpenSpawnPosition()
              s.isLocalPlayer -> getSafeZoneSpawnPosition()
              s.isHuman && s.hasNetTarget -> s.netTargetX to s.netTargetY
              else -> getOuterGateSpawnPosition(s.freq)
            }
          s.x = pos.first
          s.y = pos.second
          s.vx = 0f
          s.vy = 0f
          val baseRespawnBty = if (s.isLocalPlayer) getLocalStartingBounty() else 100
          s.bounty =
            if (s.pendingRespawnBountyPenalty > 0) {
              max(50, baseRespawnBty - s.pendingRespawnBountyPenalty)
            } else {
              baseRespawnBty
            }
          s.pendingRespawnBountyPenalty = 0
          s.speedOverchargeStacks = 0
          s.thrustOverchargeStacks = 0
          s.rechargeOverchargeStacks = 0
          s.turnOverchargeStacks = 0
          s.maxFluxOverchargeStacks = 0
          s.maxFlux = s.spec.maxFlux * (currentCustomGame?.healthMult ?: 1.0f)
          s.flux = s.maxFlux
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
          s.specialWeaponStacks = 0
          s.shotgunWavesRemaining = 0
          s.shotgunWaveTimer = 0f
          s.novaWavesRemaining = 0
          s.novaWaveTimer = 0f
          s.tractorBeamActive = false
          s.tractorFromSecondary = false
          s.tractorTargetShipId = ""
          s.lanceBeamActive = false
          s.lanceFromSecondary = false
          s.lanceHitSparkActive = false
          s.missileLockWarningTimer = 0f
          s.plasmaRiftTickTimer = 0f
          s.visualBankRoll = 0f
          s.barrelRollTimer = 0f
          s.prevHeadingForBank = s.angle
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
          if (s.isLocalPlayer) {
            spectatingTargetShipId = ""
            buffVersion++
            resetCurrentLifeStats()
            if (!isEphemeralPracticeRift) {
              onDailyDirectiveEvent(DailyDirectiveMetric.BOUNTY_PEAK_IN_LIFE, localPlayer.bounty, true)
              notifyLocalLifecycleSync()
            }
          }
        }
        continue
      }

      if (inDeathmatchReadyHold) {
        s.vx = 0f
        s.vy = 0f
        s.afterburning = false
        s.weaponsLocked = true
        s.canFire = false
        s.weaponState = "READY-UP"
        s.flux = s.maxFlux
        s.phaseShieldTimer = 3.0f
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

      // Step 3.0-second Rocket Hyper-Thruster Boost (1.65x max speed, 0 Flux cost, blazing plasma exhaust trail)
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

      // Step Disruptor Stealth Cloak continuous drain
      if (s.isCloaked) {
        selfDrainFlux(s, RiftCalibrationState.cloakDrainPerSec * dt)
        if (s.flux <= 150f || isInsideSafeZone(s.x, s.y)) {
          s.isCloaked = false
          if (s.spec.bombType == "CLOAK_EMP") {
            s.bombTimer = max(s.bombTimer, 2.0f)
          }
          if (s.isLocalPlayer) {
            buffVersion++
            onPrizeToast("STEALTH CLOAK DEACTIVATED (FLUX AT 150 SAFETY FLOOR)")
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

      // Step staggered N-stack 360-degree Nova waves
      if (s.novaWavesRemaining > 0) {
        s.novaWaveTimer -= dt
        if (s.novaWaveTimer <= 0f) {
          s.novaWavesRemaining--
          s.novaWaveTimer = 0.22f
          fireRadialBurstProjectiles(s)
        }
      }

      // Step active Toggleable Tractor Beam & Continuous Lance Beam
      if (s.tractorBeamActive || s.lanceBeamActive) {
        stepContinuousToggleBuffs(s, dt)
      }

      var currentlyInSafe = isInsideSafeZone(s.x, s.y)
      if (currentlyInSafe) {
        if (activeRankedMode == RankedGameMode.FLUX_CORE) {
          ejectFluxCoreFromShip(s, reason = "SAFE ZONE ENTRY")
        }
        s.playfieldIdleTimer = 0f
        s.isIdleDraining = false
        if (isEphemeralPracticeRift || (s.isHuman && !s.isLocalPlayer)) {
          s.safeZoneStayTimer = 0f
        } else {
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
              text = "SAFE ZONE EVICTION WARP",
              x = s.x,
              y = s.y - 26f,
              color = Color.parseColor("#38BDF8"),
              maxLife = 1.25f,
            )
            if (s.isLocalPlayer) {
              notifyLocalLifecycleSync()
              sfx.playPrize()
              onPrizeToast("SAFE ZONE IDLE LIMIT EXCEEDED — TELEPORTED TO OPEN SECTOR")
            }
          }
        }
      } else {
        s.safeZoneStayTimer = 0f
        if (s.safeZoneLockoutTimer > 0f) {
          s.safeZoneLockoutTimer = max(0f, s.safeZoneLockoutTimer - rawDt)
        }
        // Open-Space Anti-AFK Flux Drain runs ONLY in modes with an active Safe Zone (SECTOR_CONVERGENCE & FLUX_CORE), NEVER in Practice Rift, and authoritatively per-client for humans
        if (!hasActiveSafeZone() || isEphemeralPracticeRift || (s.isHuman && !s.isLocalPlayer)) {
          s.playfieldIdleTimer = 0f
          s.isIdleDraining = false
        } else {
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
        } else if (s.phaseShieldTimer > 0f) {
          s.phaseShieldTimer = max(0f, s.phaseShieldTimer - dt)
        }
      }

      val isCarrierOfFluxCore =
        activeRankedMode == RankedGameMode.FLUX_CORE &&
          (fluxCoreCarrierEntityId == s.id || (fluxCoreOrb.carrierShipId == s.shipId && hypot(s.x - fluxCoreOrb.x, s.y - fluxCoreOrb.y) < 120f))
      val fluxCoreRchMult =
        if (isCarrierOfFluxCore) (1f + fluxCoreOrb.overchargeTier.coerceIn(0, 10) * 0.05f) else 1f
      val rechargeSurgeMult =
        (1f + s.rechargeOverchargeStacks * RiftCalibrationState.overchargeSurgePctPerStack) *
          fluxCoreRchMult *
          (currentCustomGame?.rechargeMult ?: 1.0f)
      if (s.empTimer > 0f) {
        s.empTimer = max(0f, s.empTimer - dt)
      } else if (!s.isIdleDraining && !s.isCloaked && !s.tractorBeamActive && !s.lanceBeamActive) {
        val safeBonus = if (currentlyInSafe) RiftCalibrationState.safeZoneRechargeMult else 1.0f
        s.flux = min(s.maxFlux, s.flux + s.spec.rechargeRate * rechargeSurgeMult * safeBonus * dt)
      }

      if (s.phaseDashTimer > 0f) {
        s.phaseDashTimer = max(0f, s.phaseDashTimer - dt)
      }

      val fluxCoreSpdMult =
        if (isCarrierOfFluxCore) (1f + fluxCoreOrb.overchargeTier.coerceIn(0, 10) * 0.04f) else 1f
      val speedSurgeMult =
        (1f + s.speedOverchargeStacks * RiftCalibrationState.overchargeSurgePctPerStack) *
          fluxCoreSpdMult *
          (currentCustomGame?.speedMult ?: 1.0f)
      val rocketSpeedMult = if (s.rocketBoostTimer > 0f) RiftCalibrationState.hyperSurgeSpeedMult else 1.0f
      val baseMaxSp =
        (if (s.afterburning) s.spec.afterburnerSpeed else s.spec.topSpeed) * speedSurgeMult * rocketSpeedMult
      val maxSp =
        if (s.phaseDashTimer > 0f) {
          max(baseMaxSp, max(s.spec.afterburnerSpeed * 1.42f, RiftCalibrationState.phaseDashImpulse))
        } else {
          baseMaxSp
        }
      val isRemoteHumanShip = s.isHuman && !s.isLocalPlayer
      var remoteDeadReckonX = s.x
      var remoteDeadReckonY = s.y
      if (isRemoteHumanShip && s.hasNetTarget) {
        s.netPacketAgeSec = (s.netPacketAgeSec + dt).coerceAtMost(0.32f)
        val extrapSec =
          if (s.netPacketAgeSec <= 0.16f) {
            s.netPacketAgeSec
          } else {
            0.16f + (s.netPacketAgeSec - 0.16f) * 0.40f
          }
        val maxRemoteRadial = (arenaRadius - s.radius).coerceAtLeast(120f)
        remoteDeadReckonX = s.netTargetX + s.netTargetVx * extrapSec
        remoteDeadReckonY = s.netTargetY + s.netTargetVy * extrapSec
        val predDx = remoteDeadReckonX - arenaCenterX
        val predDy = remoteDeadReckonY - arenaCenterY
        val predRadial = hypot(predDx, predDy)
        if (predRadial > maxRemoteRadial && predRadial > 0.001f) {
          remoteDeadReckonX = arenaCenterX + (predDx / predRadial) * maxRemoteRadial
          remoteDeadReckonY = arenaCenterY + (predDy / predRadial) * maxRemoteRadial
        }

        // Closed-form critically damped spring-damper decay (zeta = 1.0, zero overshoot)
        val omega = 14.5f
        val expTerm = kotlin.math.exp(-omega * dt)
        val e0x = s.netSmoothOffsetX
        val v0x = s.netSmoothVelX
        val e0y = s.netSmoothOffsetY
        val v0y = s.netSmoothVelY
        s.netSmoothOffsetX = (e0x + (v0x + omega * e0x) * dt) * expTerm
        s.netSmoothVelX = (v0x - (v0x + omega * e0x) * omega * dt) * expTerm
        s.netSmoothOffsetY = (e0y + (v0y + omega * e0y) * dt) * expTerm
        s.netSmoothVelY = (v0y - (v0y + omega * e0y) * omega * dt) * expTerm

        s.vx = s.netTargetVx + s.netSmoothVelX
        s.vy = s.netTargetVy + s.netSmoothVelY
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

      if (isRemoteHumanShip && s.hasNetTarget) {
        val rawErrDist = hypot(remoteDeadReckonX - s.x, remoteDeadReckonY - s.y)
        if (rawErrDist > 650f) {
          s.x = remoteDeadReckonX
          s.y = remoteDeadReckonY
          s.vx = s.netTargetVx
          s.vy = s.netTargetVy
          s.netSmoothOffsetX = 0f
          s.netSmoothOffsetY = 0f
          s.netSmoothVelX = 0f
          s.netSmoothVelY = 0f
        } else {
          val smoothGoalX = remoteDeadReckonX + s.netSmoothOffsetX
          val smoothGoalY = remoteDeadReckonY + s.netSmoothOffsetY
          val anchorBlend = (1f - kotlin.math.exp(-18f * dt)).coerceIn(0f, 1f)
          s.x += (smoothGoalX - s.x) * anchorBlend
          s.y += (smoothGoalY - s.y) * anchorBlend
        }
      }
      val preCollisionRemoteX = s.x
      val preCollisionRemoteY = s.y

      // Mode 4 (STASIS_BREACH): Two-Way Circular Stasis Bubble Jails at West (1400, 4000) and East (6600, 4000)
      if (isStasisBreachRulesActive()) {
        val jails = listOf(Pair(westBrigReactorX, westBrigReactorY), Pair(eastBrigReactorX, eastBrigReactorY))
        for ((jx, jy) in jails) {
          val prevDist = hypot(prevShipX - jx, prevShipY - jy)
          val dx = s.x - jx
          val dy = s.y - jy
          val curDist = hypot(dx, dy)
          if (s.isInStasisBrig && prevDist <= stasisJailRadius + 24f) {
            val innerLimit = (stasisJailRadius - s.radius).coerceAtLeast(36f)
            if (curDist > innerLimit) {
              val dSafe = curDist.coerceAtLeast(0.001f)
              val nx = dx / dSafe
              val ny = dy / dSafe
              s.x = jx + nx * innerLimit
              s.y = jy + ny * innerLimit
              val preVx = s.vx
              val preVy = s.vy
              val dot = preVx * nx + preVy * ny
              if (dot > 0f) {
                val elast = RiftCalibrationState.wallElasticity
                s.vx = (preVx - 2f * dot * nx) * elast
                s.vy = (preVy - 2f * dot * ny) * elast
                triggerWallBounceReaction(s, -nx, -ny, preVx, preVy)
                if (s.isLocalPlayer) sfx.playBounce()
              }
            }
          } else if (!s.isInStasisBrig && prevDist > stasisJailRadius - 24f) {
            val outerLimit = stasisJailRadius + s.radius
            if (curDist < outerLimit) {
              val dSafe = curDist.coerceAtLeast(0.001f)
              val nx = if (curDist > 0.001f) dx / dSafe else 1f
              val ny = if (curDist > 0.001f) dy / dSafe else 0f
              s.x = jx + nx * outerLimit
              s.y = jy + ny * outerLimit
              val preVx = s.vx
              val preVy = s.vy
              val dot = preVx * nx + preVy * ny
              if (dot < 0f) {
                val elast = RiftCalibrationState.wallElasticity
                s.vx = (preVx - 2f * dot * nx) * elast
                s.vy = (preVy - 2f * dot * ny) * elast
                triggerWallBounceReaction(s, nx, ny, preVx, preVy)
                if (s.isLocalPlayer) sfx.playBounce()
              }
            }
          }
        }
      }

      // Two-Way Interdictor Stasis Bubble Containment (0 collision damage, elastic reflection)
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
            val preVx = s.vx
            val preVy = s.vy
            val dot = preVx * nx + preVy * ny
            if (dot > 0f) {
              val elast = RiftCalibrationState.wallElasticity
              s.vx = (preVx - 2f * dot * nx) * elast
              s.vy = (preVy - 2f * dot * ny) * elast
              triggerWallBounceReaction(s, -nx, -ny, preVx, preVy)
              val hitAng = atan2(ny, nx)
              ib.recordBoundaryIngress(hitAng, 0.58f)
              spawnDropletReactionBurst(
                originX = ib.x + nx * ib.radius,
                originY = ib.y + ny * ib.radius,
                targetOrWakeX = s.x + s.vx * 0.14f,
                targetOrWakeY = s.y + s.vy * 0.14f,
                pullInward = true,
                crystalCount = 4,
                filamentCount = 3,
                outerColor = Color.rgb(236, 72, 153),
                midColor = Color.rgb(0, 245, 212),
                coreColor = Color.rgb(254, 249, 195),
                baseCrystalRadius = 5.8f,
              )
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
            val preVx = s.vx
            val preVy = s.vy
            val dot = preVx * nx + preVy * ny
            if (dot < 0f) {
              val elast = RiftCalibrationState.wallElasticity
              s.vx = (preVx - 2f * dot * nx) * elast
              s.vy = (preVy - 2f * dot * ny) * elast
              triggerWallBounceReaction(s, nx, ny, preVx, preVy)
              val hitAng = atan2(ny, nx)
              ib.recordBoundaryIngress(hitAng, 0.58f)
              spawnDropletReactionBurst(
                originX = ib.x + nx * ib.radius,
                originY = ib.y + ny * ib.radius,
                targetOrWakeX = s.x + s.vx * 0.15f,
                targetOrWakeY = s.y + s.vy * 0.15f,
                pullInward = false,
                crystalCount = 4,
                filamentCount = 3,
                outerColor = Color.rgb(236, 72, 153),
                midColor = Color.rgb(0, 245, 212),
                coreColor = Color.rgb(254, 249, 195),
                baseCrystalRadius = 5.8f,
              )
              if (s.isLocalPlayer) sfx.playBounce()
            }
          }
        }
      }

      // Circular Wall-less Safe Zone 15-Second Re-Entry Lockout & Outward Radial Repulsion:
      val szRadius = getSafeZoneRadius()
      val szDx = s.x - arenaCenterX
      val szDy = s.y - arenaCenterY
      val szDist = hypot(szDx, szDy)
      if (
        hasActiveSafeZone() &&
          s.safeZoneLockoutTimer > 0f &&
          szDist <= szRadius + s.radius
      ) {
        val distSafe = szDist.coerceAtLeast(0.001f)
        val nx = if (szDist > 0.001f) szDx / distSafe else 0f
        val ny = if (szDist > 0.001f) szDy / distSafe else -1f
        val preVx = s.vx
        val preVy = s.vy

        val pushOutR = szRadius + s.radius + 6f
        s.x = arenaCenterX + nx * pushOutR
        s.y = arenaCenterY + ny * pushOutR

        val repelForce = RiftCalibrationState.safeZoneRepelForce
        s.vx = nx * repelForce
        s.vy = ny * repelForce
        triggerWallBounceReaction(s, nx, ny, preVx, preVy)
        s.inSafeZone = false
        s.wasInSafeZone = false

        shockwaves.add(ShockwaveEntity(s.x, s.y, 16f, 165f, Color.parseColor("#EF4444")))
        if (s.isLocalPlayer) {
          sfx.playBounce()
          addTrauma(0.22f)
          spawnPooledFct(
            text = "SAFE ZONE LOCKED (${String.format(java.util.Locale.US, "%.1fs", s.safeZoneLockoutTimer)})",
            x = s.x,
            y = s.y - 28f,
            color = Color.parseColor("#EF4444"),
          )
        }
      }

      resolveShipWallBounce(s)

      if (isRemoteHumanShip) {
        val clampR = (arenaRadius - s.radius).coerceAtLeast(120f)
        val cdx = s.x - arenaCenterX
        val cdy = s.y - arenaCenterY
        val cdist = hypot(cdx, cdy)
        if (cdist > clampR && cdist > 0.001f) {
          s.x = arenaCenterX + (cdx / cdist) * clampR
          s.y = arenaCenterY + (cdy / cdist) * clampR
        }
        if (s.hasNetTarget && hypot(s.x - preCollisionRemoteX, s.y - preCollisionRemoteY) > 0.5f) {
          s.netSmoothOffsetX = s.x - remoteDeadReckonX
          s.netSmoothOffsetY = s.y - remoteDeadReckonY
          s.netSmoothVelX = s.vx - s.netTargetVx
          s.netSmoothVelY = s.vy - s.netTargetVy
        }
      }

      val postBounceSpeed = hypot(s.vx, s.vy)
      if (isRemoteHumanShip && s.hasNetTarget) {
        val rotBlend = (1f - kotlin.math.exp(-18f * dt)).coerceIn(0f, 1f)
        val desiredHeading = if (postBounceSpeed > 24f) atan2(s.vy, s.vx) else s.netTargetAngle
        val headingDiff = atan2(sin(desiredHeading - s.angle), cos(desiredHeading - s.angle))
        s.angle += headingDiff * rotBlend
        val aimDiff =
          atan2(sin(s.netTargetWeaponAngle - s.weaponAngle), cos(s.netTargetWeaponAngle - s.weaponAngle))
        s.weaponAngle += aimDiff * rotBlend
      } else if (postBounceSpeed > 2f) {
        s.angle = atan2(s.vy, s.vx)
      } else if (s.isLocalPlayer && leftStickActive && leftStickMag > 0.08f) {
        s.angle = leftStickAngle
      }

      // Update purely visual 3D Turn Banking (-1f..+1f) & 0.48s 360° Barrel Roll timer (zero hitbox impact)
      if (s.barrelRollTimer > 0f) {
        s.barrelRollTimer = max(0f, s.barrelRollTimer - rawDt)
      }
      val headingDelta = atan2(sin(s.angle - s.prevHeadingForBank), cos(s.angle - s.prevHeadingForBank))
      s.prevHeadingForBank = s.angle
      val safeBankDt = rawDt.coerceIn(0.004f, 0.05f)
      val angVelBank =
        if (kotlin.math.abs(headingDelta) < 1.35f) {
          (headingDelta / safeBankDt) * 0.20f
        } else {
          0f
        }
      val stickCrossBank =
        if (s.isLocalPlayer && leftStickActive && leftStickMag > 0.08f) {
          val cross = cos(s.angle) * sin(leftStickAngle) - sin(s.angle) * cos(leftStickAngle)
          cross * leftStickMag * 0.85f
        } else {
          0f
        }
      val targetBank = (angVelBank + stickCrossBank).coerceIn(-1f, 1f)
      val bankLerp = (1f - 0.0008.pow(safeBankDt.toDouble()).toFloat()).coerceIn(0.08f, 0.45f)
      s.visualBankRoll += (targetBank - s.visualBankRoll) * bankLerp

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
          val customForgedEntry = if (gr.buffType >= 16) RiftCodex.getBuffRegistryByIndex(gr.buffType) else null
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
            gr.buffType == 11 -> grantSpecialBuffWeapon(s, "SWARM_DRONES", RiftCodex.BUFF_REGISTRY["SWARM_DRONES"]?.chargesPerPickup ?: 1)
            gr.buffType == 12 -> grantSpecialBuffWeapon(s, "TRACTOR_BEAM", RiftCodex.BUFF_REGISTRY["TRACTOR_BEAM"]?.chargesPerPickup ?: 1)
            gr.buffType == 13 -> grantSpecialBuffWeapon(s, "PROX_SEEKER", RiftCodex.BUFF_REGISTRY["PROX_SEEKER"]?.chargesPerPickup ?: 1)
            gr.buffType == 14 -> grantSpecialBuffWeapon(s, "LANCE_BEAM", RiftCodex.BUFF_REGISTRY["LANCE_BEAM"]?.chargesPerPickup ?: 1)
            gr.buffType == 15 -> grantSpecialBuffWeapon(s, "CHAKRAM", RiftCodex.BUFF_REGISTRY["CHAKRAM"]?.chargesPerPickup ?: 2)
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
            gr.buffType == -13 -> {
              s.turnOverchargeStacks = min(3, s.turnOverchargeStacks + 1)
            }
            gr.buffType == -14 -> {
              s.maxFluxOverchargeStacks = min(3, s.maxFluxOverchargeStacks + 1)
              val customHealthMult = currentCustomGame?.healthMult ?: 1.0f
              s.maxFlux = s.spec.maxFlux * (1f + s.maxFluxOverchargeStacks * 0.10f) * customHealthMult
              s.flux = min(s.maxFlux, s.flux + s.spec.maxFlux * 0.10f * customHealthMult)
            }
            else -> {
              s.speedOverchargeStacks = min(3, s.speedOverchargeStacks + 1)
            }
          }
          if (s.isLocalPlayer) {
            buffVersion++
            sfx.playPrize()
            if (!isEphemeralPracticeRift) {
              greensCollected++
              currentLifeStats.buffsCollected++
              currentLifeStats.bountyReached = max(currentLifeStats.bountyReached, localPlayer.bounty)
              onDailyDirectiveEvent(DailyDirectiveMetric.CRATES_COLLECTED, 1, false)
              onDailyDirectiveEvent(DailyDirectiveMetric.BOUNTY_PEAK_IN_LIFE, localPlayer.bounty, true)
              onSyndicateContribution(0L, localPlayer.bounty.toLong(), 0L)
            }
            val msg =
              when {
                customForgedEntry != null -> {
                  spawnPooledFct(
                    "${customForgedEntry.shortLabel} [${s.specialWeaponCharges}] (0 FLUX)",
                    s.x,
                    s.y - 24f,
                    customForgedEntry.strokeColorInt,
                  )
                  "FORGED BUFF: ${customForgedEntry.officialName} (+${customForgedEntry.chargesPerPickup} CHARGES • ${s.specialWeaponCharges} TOTAL)"
                }
                gr.buffType == 0 -> {
                  spawnPooledFct("NOVA x${s.specialWeaponStacks} WAVES", s.x, s.y - 24f, Color.parseColor("#F97316"))
                  "SPECIAL BUFF: NOVA RING (x${s.specialWeaponStacks} STAGGERED 360° WAVES • 0 FLUX)"
                }
                gr.buffType == 1 -> {
                  spawnPooledFct("SHATTER [${s.specialWeaponCharges}] (0 FLUX)", s.x, s.y - 24f, Color.parseColor("#FB7185"))
                  "SPECIAL BUFF: SHATTER CONE (+2 CHARGES • ${s.specialWeaponCharges} TOTAL • 0 FLUX)"
                }
                gr.buffType == 2 -> {
                  spawnPooledFct("SEEKER [${s.specialWeaponCharges}] (0 FLUX)", s.x, s.y - 24f, Color.parseColor("#EF4444"))
                  "SPECIAL BUFF: SEEKER TORPEDO (+1 CHARGE • ${s.specialWeaponCharges} TOTAL • 0 FLUX)"
                }
                gr.buffType == 3 -> {
                  spawnPooledFct("PHANTOM [${s.specialWeaponCharges}] (0 FLUX)", s.x, s.y - 24f, Color.parseColor("#2DD4BF"))
                  "SPECIAL BUFF: HOLO PHANTOM (+1 CHARGE • ${s.specialWeaponCharges} TOTAL • 0 FLUX)"
                }
                gr.buffType == 4 -> {
                  spawnPooledFct("BARRIER x${s.specialWeaponStacks} SEG", s.x, s.y - 24f, Color.parseColor("#38BDF8"))
                  "SPECIAL BUFF: STASIS BARRIER (${s.specialWeaponStacks} WALL SEGMENTS READY • 0 FLUX)"
                }
                gr.buffType == 5 -> {
                  spawnPooledFct("SURGE [${s.specialWeaponCharges}] (0 FLUX)", s.x, s.y - 24f, Color.parseColor("#F43F5E"))
                  "SPECIAL BUFF: HYPER SURGE (+1 CHARGE • ${s.specialWeaponCharges} TOTAL • 0 FLUX)"
                }
                gr.buffType == 11 -> {
                  spawnPooledFct("SWARM [${s.specialWeaponCharges}] (0 FLUX)", s.x, s.y - 24f, Color.parseColor("#00F5D4"))
                  "SPECIAL BUFF: SWARM (+1 CHARGE • ${s.specialWeaponCharges} TOTAL • 5 AUTONOMOUS DRONES)"
                }
                gr.buffType == 12 -> {
                  spawnPooledFct("TRACTOR BEAM READY", s.x, s.y - 24f, Color.parseColor("#34D399"))
                  "SPECIAL BUFF: TRACTOR BEAM (TOGGLE LASER WEB • DRAINS FLUX)"
                }
                gr.buffType == 13 -> {
                  spawnPooledFct("PROX MISSILE [${s.specialWeaponCharges}]", s.x, s.y - 24f, Color.parseColor("#FACC15"))
                  "SPECIAL BUFF: PROXIMITY MISSILE (INFINITE DURATION & BOUNCES • RE-LOCK RING)"
                }
                gr.buffType == 14 -> {
                  spawnPooledFct("LANCE BEAM x${s.specialWeaponStacks}", s.x, s.y - 24f, Color.parseColor("#A855F7"))
                  "SPECIAL BUFF: LANCE BEAM (x${s.specialWeaponStacks} WIDTH/RANGE • TOGGLE CONTINUOUS)"
                }
                gr.buffType == 15 -> {
                  spawnPooledFct("CHAKRAM [${s.specialWeaponCharges}] (0 FLUX)", s.x, s.y - 24f, Color.parseColor("#F59E0B"))
                  "SPECIAL BUFF: CHAKRAM (+2 CHARGES • ${s.specialWeaponCharges} TOTAL • 8-SLICE BOUNCING SAW DISC)"
                }
                gr.buffType == 6 -> {
                  spawnPooledFct("FULL FLUX +$pickupBty RES", s.x, s.y - 24f, Color.parseColor("#10B981"))
                  "INSTANT BUFF: 100% FULL FLUX CHARGE +$pickupBty RESONANCE"
                }
                gr.buffType == 7 -> {
                  spawnPooledFct("REPEL +1 (${s.repels})", s.x, s.y - 24f, Color.parseColor("#00F5D4"))
                  "INSTANT BUFF: +1 REPEL CHARGE (${s.repels} TOTAL)"
                }
                gr.buffType == 8 -> {
                  val label =
                    if (s.spec.supportsNativeMultifire) {
                      "5-STREAM OVERDRIVE [${s.multiShotCharges}]"
                    } else {
                      "MULTI [${s.multiShotCharges}]"
                    }
                  spawnPooledFct(label, s.x, s.y - 24f, Color.parseColor("#FACC15"))
                  if (s.spec.supportsNativeMultifire) {
                    "SHOT-LIMITED MULTI-SHOT: +${RiftCalibrationState.crateMultiShotAmmo} SHOTS (+25% DMG / 5-STREAM OVERDRIVE!)"
                  } else {
                    "SHOT-LIMITED MULTI-SHOT: +${RiftCalibrationState.crateMultiShotAmmo} SHOTS ON PRIMARY TRIGGER (MULTI [${s.multiShotCharges}])"
                  }
                }
                gr.buffType == 9 -> {
                  spawnPooledFct("BOUNCE UPGRADE (+1)", s.x, s.y - 24f, Color.parseColor("#A855F7"))
                  "HULL UPGRADE: BOUNCING BULLETS ACTIVE UNTIL DEATH (+1 WALL BOUNCE)"
                }
                gr.buffType == -11 -> {
                  val pct = (s.thrustOverchargeStacks * RiftCalibrationState.overchargeSurgePctPerStack * 100f).toInt()
                  spawnPooledFct("THR +$pct% (${s.thrustOverchargeStacks}/3)", s.x, s.y - 24f, Color.parseColor("#F59E0B"))
                  "HULL UPGRADE: THRUST +$pct% (${s.thrustOverchargeStacks}/3)"
                }
                gr.buffType == -12 -> {
                  val pct = (s.rechargeOverchargeStacks * RiftCalibrationState.overchargeSurgePctPerStack * 100f).toInt()
                  spawnPooledFct("RCH +$pct% (${s.rechargeOverchargeStacks}/3)", s.x, s.y - 24f, Color.parseColor("#10B981"))
                  "HULL UPGRADE: FLUX RECHARGE +$pct% (${s.rechargeOverchargeStacks}/3)"
                }
                gr.buffType == -13 -> {
                  val pct = s.turnOverchargeStacks * 12
                  spawnPooledFct("TURN +$pct% (${s.turnOverchargeStacks}/3)", s.x, s.y - 24f, Color.parseColor("#00F5D4"))
                  "HULL UPGRADE: TURN RATE +$pct% (${s.turnOverchargeStacks}/3)"
                }
                gr.buffType == -14 -> {
                  val pct = s.maxFluxOverchargeStacks * 10
                  spawnPooledFct("MAX FLUX +$pct% (${s.maxFluxOverchargeStacks}/3)", s.x, s.y - 24f, Color.parseColor("#B388FF"))
                  "HULL UPGRADE: MAX FLUX +$pct% (${s.maxFluxOverchargeStacks}/3)"
                }
                else -> {
                  val pct = (s.speedOverchargeStacks * RiftCalibrationState.overchargeSurgePctPerStack * 100f).toInt()
                  spawnPooledFct("SPD +$pct% (${s.speedOverchargeStacks}/3)", s.x, s.y - 24f, Color.parseColor("#38BDF8"))
                  "HULL UPGRADE: TOP SPEED +$pct% (${s.speedOverchargeStacks}/3)"
                }
              }
            onPrizeToast(msg)
            if (!isEphemeralPracticeRift) {
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
      val bulletWallHit = segmentFirstWallHit(b.x, b.y, nx, ny, pad = 1.0f)
      if (bulletWallHit != null) {
        hitWall = true
        if (bulletWallHit.wallIndex >= 0) {
          onObstacleProjectileHit(
            wallIndex = bulletWallHit.wallIndex,
            damage = b.damage,
            hitX = bulletWallHit.hitX,
            hitY = bulletWallHit.hitY,
            attackerId = b.ownerId,
            isStationProjectile = isStationDefenseOwner(b.ownerId),
          )
        }
        if (b.bouncesLeft > 0) {
          b.bouncesLeft--
          b.x = bulletWallHit.hitX + bulletWallHit.normalX * 2.5f
          b.y = bulletWallHit.hitY + bulletWallHit.normalY * 2.5f
          val dot = b.vx * bulletWallHit.normalX + b.vy * bulletWallHit.normalY
          if (dot < 0f) {
            b.vx -= 2f * dot * bulletWallHit.normalX
            b.vy -= 2f * dot * bulletWallHit.normalY
          }
        } else {
          bullets.removeAt(i)
        }
      }
      if (!hitWall) {
        val nextPerimDist = hypot(nx - arenaCenterX, ny - arenaCenterY)
        if (nextPerimDist >= arenaRadius) {
          hitWall = true
          if (b.bouncesLeft > 0) {
            b.bouncesLeft--
            val dSafe = nextPerimDist.coerceAtLeast(0.001f)
            val rnx = (nx - arenaCenterX) / dSafe
            val rny = (ny - arenaCenterY) / dSafe
            b.x = arenaCenterX + rnx * (arenaRadius - 3f)
            b.y = arenaCenterY + rny * (arenaRadius - 3f)
            val dot = b.vx * rnx + b.vy * rny
            if (dot > 0f) {
              b.vx -= 2f * dot * rnx
              b.vy -= 2f * dot * rny
            }
          } else {
            bullets.removeAt(i)
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
          ib.recordBoundaryIngress(atan2(rny, rnx), 0.44f)
          spawnDropletReactionBurst(
            originX = b.x,
            originY = b.y,
            targetOrWakeX = b.x + b.vx * 0.07f,
            targetOrWakeY = b.y + b.vy * 0.07f,
            pullInward = true,
            crystalCount = 2,
            filamentCount = 2,
            outerColor = Color.rgb(236, 72, 153),
            midColor = Color.rgb(0, 245, 212),
            coreColor = Color.WHITE,
            baseCrystalRadius = 4.2f,
            durationSec = 0.40f,
          )
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
          ib.recordBoundaryIngress(atan2(rny, rnx), 0.44f)
          spawnDropletReactionBurst(
            originX = b.x,
            originY = b.y,
            targetOrWakeX = b.x + b.vx * 0.07f,
            targetOrWakeY = b.y + b.vy * 0.07f,
            pullInward = false,
            crystalCount = 2,
            filamentCount = 2,
            outerColor = Color.rgb(236, 72, 153),
            midColor = Color.rgb(0, 245, 212),
            coreColor = Color.WHITE,
            baseCrystalRadius = 4.2f,
            durationSec = 0.40f,
          )
          reflectedByBubble = true
          break
        }
      }
      if (!reflectedByBubble) {
        b.x = nx
        b.y = ny
      }

      // STASIS_BREACH: Two-Way Circular Stasis Bubble Jail reflection & Center Core damage by prisoners only
      if (isStasisBreachRulesActive()) {
        val shooter = ships.firstOrNull { it.id == b.ownerId }
        val shooterInBrig = shooter?.isInStasisBrig == true
        val jails = listOf(Triple(0, westBrigReactorX, westBrigReactorY), Triple(1, eastBrigReactorX, eastBrigReactorY))
        var jailHandled = false
        for ((brigFreq, jx, jy) in jails) {
          val prevDist = hypot(b.x - b.vx * dt - jx, b.y - b.vy * dt - jy)
          val curDist = hypot(b.x - jx, b.y - jy)
          // Check center Core hit by prisoner inside this jail
          if (shooterInBrig && b.freq != brigFreq && curDist <= 54f) {
            damageBrigReactorCore(
              targetBrigFreq = brigFreq,
              attacker = shooter,
              rawDamage = b.damage,
            )
            bullets.removeAt(i)
            jailHandled = true
            break
          }
          // Two-way circular boundary reflection
          if (prevDist <= stasisJailRadius && curDist >= stasisJailRadius) {
            val dSafe = curDist.coerceAtLeast(0.001f)
            val rnx = (b.x - jx) / dSafe
            val rny = (b.y - jy) / dSafe
            b.x = jx + rnx * (stasisJailRadius - 2f)
            b.y = jy + rny * (stasisJailRadius - 2f)
            val dot = b.vx * rnx + b.vy * rny
            if (dot > 0f) {
              b.vx -= 2f * dot * rnx
              b.vy -= 2f * dot * rny
            }
          } else if (prevDist > stasisJailRadius && curDist <= stasisJailRadius) {
            val dSafe = curDist.coerceAtLeast(0.001f)
            val rnx = if (curDist > 0.001f) (b.x - jx) / dSafe else 1f
            val rny = if (curDist > 0.001f) (b.y - jy) / dSafe else 0f
            b.x = jx + rnx * (stasisJailRadius + 2f)
            b.y = jy + rny * (stasisJailRadius + 2f)
            val dot = b.vx * rnx + b.vy * rny
            if (dot < 0f) {
              b.vx -= 2f * dot * rnx
              b.vy -= 2f * dot * rny
            }
          }
        }
        if (jailHandled) continue
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

      val bulletHitPadding = 8f * (currentCustomGame?.bulletSizeMult ?: 1.0f)
      for (s in ships) {
        if (!(isInLobbyStaging && s.isLocalPlayer) && !isBulletAlliedWithShip(b, s) && s.respawnTimer <= 0f) {
          if (hypot(s.x - b.x, s.y - b.y) < s.radius + bulletHitPadding) {
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
              stationTriggeredByShipId = b.triggeredByShipId,
            )
            bullets.removeAt(i)
            break
          }
        }
      }
    }

    // 4. Step Bombs, Proximity Flak, Mines, Predictive Seeker Torpedoes & Custom AI-Forged Orbs
    val customSpdMult = currentCustomGame?.speedMult ?: 1.0f
    val seekerCruiseSpeed = max(RiftCalibrationState.seekerSpeed, 560f) * customSpdMult
    val seekerMaxTurnRad = max(RiftCalibrationState.seekerTurnRate, 6.2f) * customSpdMult
    for (i in bombs.indices.reversed()) {
      val b = bombs[i]
      if (b.recentHitShipTimers.isNotEmpty()) {
        val hitIt = b.recentHitShipTimers.entries.iterator()
        while (hitIt.hasNext()) {
          val entry = hitIt.next()
          val nextRem = entry.value - dt
          if (nextRem <= 0f) hitIt.remove() else entry.setValue(nextRem)
        }
      }
      if (!b.isInfiniteProxMissile) {
        b.life -= dt
      }
      if (!b.isInfiniteProxMissile && b.life <= 0f) {
        if (b.type == "CHAKRAM") {
          spawnExplosion(b.x, b.y, Color.parseColor("#F59E0B"))
          bombs.removeAt(i)
          continue
        }
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
        applyExplosionToObstacles(
          blastX = b.x,
          blastY = b.y,
          blastRadius = expRadius,
          baseDamage = expDmg,
          attackerId = b.ownerId,
        )
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

      if (b.armTimer > 0f) {
        b.armTimer = max(0f, b.armTimer - dt)
      } else if (b.type == "MINE") {
        // Manta (#8) Short-Range Magnetic Mine Lunge (165px detection, 420 px/s^2 pull)
        var nearestEnemy: ShipEntity? = null
        var nearestDist = 165f
        for (cand in ships) {
          if (
            !(isInLobbyStaging && cand.isLocalPlayer) &&
              !isBombAlliedWithShip(b, cand) &&
              cand.respawnTimer <= 0f &&
              !cand.isCloaked &&
              !isInsideSafeZone(cand.x, cand.y)
          ) {
            val d = hypot(cand.x - b.x, cand.y - b.y)
            if (d < nearestDist) {
              nearestDist = d
              nearestEnemy = cand
            }
          }
        }
        if (nearestEnemy != null) {
          val dx = nearestEnemy.x - b.x
          val dy = nearestEnemy.y - b.y
          val dSafe = hypot(dx, dy).coerceAtLeast(1f)
          val mineAccel = 420f * customSpdMult
          val mineMaxSpd = 340f * customSpdMult
          b.vx += (dx / dSafe) * mineAccel * dt
          b.vy += (dy / dSafe) * mineAccel * dt
          val sp = hypot(b.vx, b.vy)
          if (sp > mineMaxSpd) {
            b.vx = (b.vx / sp) * mineMaxSpd
            b.vy = (b.vy / sp) * mineMaxSpd
          }
        } else {
          val damp = 0.25.pow(dt.toDouble()).toFloat()
          b.vx *= damp
          b.vy *= damp
        }
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

      // Predictive Seeker Torpedo homing (checks Holo Phantom decoys first, then uncloaked ships with velocity lead!)
      if (b.type == "MISSILE") {
        val lockedDecoy = decoys.firstOrNull { it.id == b.targetShipId }
        if (lockedDecoy != null) {
          val dist = hypot(lockedDecoy.x - b.x, lockedDecoy.y - b.y)
          val leadT = (dist / seekerCruiseSpeed.coerceAtLeast(1f)).coerceIn(0f, 0.55f)
          val aimX = lockedDecoy.x + lockedDecoy.vx * leadT
          val aimY = lockedDecoy.y + lockedDecoy.vy * leadT
          val desiredAngle = atan2(aimY - b.y, aimX - b.x)
          val curAngle = atan2(b.vy, b.vx)
          val diff = atan2(sin(desiredAngle - curAngle), cos(desiredAngle - curAngle))
          val maxTurn = seekerMaxTurnRad * dt
          val newAngle = curAngle + diff.coerceIn(-maxTurn, maxTurn)
          b.vx = cos(newAngle) * seekerCruiseSpeed
          b.vy = sin(newAngle) * seekerCruiseSpeed
        } else {
          var lockedTarget = ships.firstOrNull {
            !(isInLobbyStaging && it.isLocalPlayer) &&
              it.id == b.targetShipId &&
              it.respawnTimer <= 0f &&
              !it.isCloaked &&
              !isInsideSafeZone(it.x, it.y)
          }
          if (lockedTarget == null) {
            var nearestDist = 1850f
            for (cand in ships) {
              if (
                !(isInLobbyStaging && cand.isLocalPlayer) &&
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
            val dist = hypot(lockedTarget.x - b.x, lockedTarget.y - b.y)
            val leadT = (dist / seekerCruiseSpeed.coerceAtLeast(1f)).coerceIn(0f, 0.55f)
            val aimX = lockedTarget.x + lockedTarget.vx * leadT
            val aimY = lockedTarget.y + lockedTarget.vy * leadT
            val desiredAngle = atan2(aimY - b.y, aimX - b.x)
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

      if (b.isInfiniteProxMissile) {
        val lockR = b.proxLockRadius.coerceAtLeast(220f)
        val insideShips =
          ships.filter { cand ->
            !(isInLobbyStaging && cand.isLocalPlayer) &&
              !isBombAlliedWithShip(b, cand) &&
              cand.respawnTimer <= 0f &&
              !cand.isCloaked &&
              !isInsideSafeZone(cand.x, cand.y) &&
              hypot(cand.x - b.x, cand.y - b.y) <= lockR
          }
        val newlyEntered =
          insideShips.filter { cand ->
            cand.id !in b.shipsInsideRadiusPrevStep && cand.id != b.targetShipId
          }
        b.shipsInsideRadiusPrevStep.clear()
        for (cand in insideShips) {
          b.shipsInsideRadiusPrevStep.add(cand.id)
        }

        var lockedTarget =
          ships.firstOrNull {
            !(isInLobbyStaging && it.isLocalPlayer) &&
              it.id == b.targetShipId &&
              it.respawnTimer <= 0f &&
              !it.isCloaked &&
              !isInsideSafeZone(it.x, it.y)
          }
        if (lockedTarget == null && b.targetShipId.isNotEmpty()) {
          b.targetShipId = ""
        }

        if (newlyEntered.isNotEmpty()) {
          val switchedTarget = newlyEntered.minByOrNull { hypot(it.x - b.x, it.y - b.y) }
          if (switchedTarget != null) {
            val wasPursuingAnother = lockedTarget != null && lockedTarget.id != switchedTarget.id
            lockedTarget = switchedTarget
            b.targetShipId = switchedTarget.id
            if (b.ownerId == localPlayer.id || switchedTarget.isLocalPlayer) {
              sfx.playMissileLockBeep()
              if (wasPursuingAnother && b.ownerId == localPlayer.id) {
                spawnPooledFct(
                  text = "LOCK SWITCH: ${switchedTarget.callsign}",
                  x = b.x,
                  y = b.y - 18f,
                  color = Color.parseColor("#FACC15"),
                )
              }
            }
          }
        } else if (lockedTarget == null && insideShips.isNotEmpty()) {
          val acquiredTarget = insideShips.minByOrNull { hypot(it.x - b.x, it.y - b.y) }
          if (acquiredTarget != null) {
            lockedTarget = acquiredTarget
            b.targetShipId = acquiredTarget.id
            if (b.ownerId == localPlayer.id || acquiredTarget.isLocalPlayer) {
              sfx.playMissileLockBeep()
            }
          }
        }

        val proxCruiseSpeed = 540f * customSpdMult
        val proxTurnRate = 6.8f * customSpdMult
        if (lockedTarget != null) {
          lockedTarget.missileLockWarningTimer = max(lockedTarget.missileLockWarningTimer, 0.22f)
          if (
            (lockedTarget.isLocalPlayer || b.ownerId == localPlayer.id) &&
              lockedTarget.missileBeepCooldown <= 0f
          ) {
            lockedTarget.missileBeepCooldown = 0.42f
            sfx.playMissileLockBeep()
          }
          val dist = hypot(lockedTarget.x - b.x, lockedTarget.y - b.y)
          val leadT = (dist / proxCruiseSpeed.coerceAtLeast(1f)).coerceIn(0f, 0.42f)
          val aimX = lockedTarget.x + lockedTarget.vx * leadT
          val aimY = lockedTarget.y + lockedTarget.vy * leadT
          val desiredAngle = atan2(aimY - b.y, aimX - b.x)
          val curAngle = atan2(b.vy, b.vx)
          val diff = atan2(sin(desiredAngle - curAngle), cos(desiredAngle - curAngle))
          val maxTurn = proxTurnRate * dt
          val newAngle = curAngle + diff.coerceIn(-maxTurn, maxTurn)
          b.vx = cos(newAngle) * proxCruiseSpeed
          b.vy = sin(newAngle) * proxCruiseSpeed
        } else {
          val curSp = hypot(b.vx, b.vy).coerceAtLeast(1f)
          b.vx = (b.vx / curSp) * proxCruiseSpeed
          b.vy = (b.vy / curSp) * proxCruiseSpeed
        }
      }

      val nx = b.x + b.vx * dt
      val ny = b.y + b.vy * dt
      if (isInsideSafeZone(nx, ny) || isInsideSafeZone(b.x, b.y)) {
        if (b.isInfiniteProxMissile || (b.type == "CHAKRAM" && b.bouncesLeft > 0)) {
          if (b.type == "CHAKRAM") b.bouncesLeft--
          val dx = nx - arenaCenterX
          val dy = ny - arenaCenterY
          val dSafe = hypot(dx, dy).coerceAtLeast(0.001f)
          val rnx = if (hypot(dx, dy) > 0.001f) dx / dSafe else 1f
          val rny = if (hypot(dx, dy) > 0.001f) dy / dSafe else 0f
          val pushR = getSafeZoneRadius() + b.radius + 6f
          b.x = arenaCenterX + rnx * pushR
          b.y = arenaCenterY + rny * pushR
          val dot = b.vx * rnx + b.vy * rny
          if (dot < 0f) {
            b.vx -= 2f * dot * rnx
            b.vy -= 2f * dot * rny
          }
          continue
        } else {
          bombs.removeAt(i)
          continue
        }
      }
      var hitWall = false

      if (!b.passWalls && (b.vx != 0f || b.vy != 0f)) {
        val bombWallHit = segmentFirstWallHit(b.x, b.y, nx, ny, pad = (b.radius * 0.35f).coerceAtMost(8f))
        if (bombWallHit != null) {
          hitWall = true
          if (bombWallHit.wallIndex >= 0) {
            onObstacleProjectileHit(
              wallIndex = bombWallHit.wallIndex,
              damage = b.damage,
              hitX = bombWallHit.hitX,
              hitY = bombWallHit.hitY,
              attackerId = b.ownerId,
            )
          }
          if (b.isInfiniteProxMissile || b.bouncesLeft > 0) {
            if (!b.isInfiniteProxMissile) {
              b.bouncesLeft--
            }
            val bounceRestitution =
              if (b.isInfiniteProxMissile || b.type == "CHAKRAM" || b.type == "MISSILE" || b.type == "BOUNCE" || b.type == "CUSTOM_ORB") {
                1.0f
              } else {
                0.85f
              }
            b.x = bombWallHit.hitX + bombWallHit.normalX * 3.5f
            b.y = bombWallHit.hitY + bombWallHit.normalY * 3.5f
            val dot = b.vx * bombWallHit.normalX + b.vy * bombWallHit.normalY
            if (dot < 0f) {
              b.vx = (b.vx - 2f * dot * bombWallHit.normalX) * bounceRestitution
              b.vy = (b.vy - 2f * dot * bombWallHit.normalY) * bounceRestitution
            } else {
              b.vx *= bounceRestitution
              b.vy *= bounceRestitution
            }
            if (b.type == "CHAKRAM" && hypot(b.x - localPlayer.x, b.y - localPlayer.y) < 950f) {
              sfx.playBounce()
            }
          } else {
            b.x = bombWallHit.hitX
            b.y = bombWallHit.hitY
            if (b.type == "PROX") spawnProximityFlakShrapnel(b)
            bombs.removeAt(i)
            spawnExplosion(
              bombWallHit.hitX,
              bombWallHit.hitY,
              when (b.type) {
                "CUSTOM_ORB" -> if (b.customColor != 0) b.customColor else Color.parseColor("#22D3EE")
                "EMP" -> Color.parseColor("#B388FF")
                "MISSILE" -> Color.parseColor("#EF4444")
                else -> Color.parseColor("#F59E0B")
              },
            )
          }
        }
        if (!hitWall) {
          val nextPerimDist = hypot(nx - arenaCenterX, ny - arenaCenterY)
          if (nextPerimDist + b.radius >= arenaRadius) {
            hitWall = true
            if (b.isInfiniteProxMissile || b.bouncesLeft > 0) {
              if (!b.isInfiniteProxMissile) {
                b.bouncesLeft--
              }
              val dSafe = nextPerimDist.coerceAtLeast(0.001f)
              val rnx = (nx - arenaCenterX) / dSafe
              val rny = (ny - arenaCenterY) / dSafe
              b.x = arenaCenterX + rnx * (arenaRadius - b.radius - 3f)
              b.y = arenaCenterY + rny * (arenaRadius - b.radius - 3f)
              val dot = b.vx * rnx + b.vy * rny
              val bounceRestitution =
                if (b.isInfiniteProxMissile || b.type == "CHAKRAM" || b.type == "MISSILE" || b.type == "BOUNCE" || b.type == "CUSTOM_ORB") {
                  1.0f
                } else {
                  0.85f
                }
              if (dot > 0f) {
                b.vx = (b.vx - 2f * dot * rnx) * bounceRestitution
                b.vy = (b.vy - 2f * dot * rny) * bounceRestitution
              }
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
          ib.recordBoundaryIngress(atan2(rny, rnx), 0.52f)
          spawnDropletReactionBurst(
            originX = b.x,
            originY = b.y,
            targetOrWakeX = b.x + b.vx * 0.09f,
            targetOrWakeY = b.y + b.vy * 0.09f,
            pullInward = true,
            crystalCount = 3,
            filamentCount = 3,
            outerColor = Color.rgb(236, 72, 153),
            midColor = Color.rgb(0, 245, 212),
            coreColor = Color.rgb(250, 204, 21),
            baseCrystalRadius = 5.4f,
            durationSec = 0.48f,
          )
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
          ib.recordBoundaryIngress(atan2(rny, rnx), 0.52f)
          spawnDropletReactionBurst(
            originX = b.x,
            originY = b.y,
            targetOrWakeX = b.x + b.vx * 0.09f,
            targetOrWakeY = b.y + b.vy * 0.09f,
            pullInward = false,
            crystalCount = 3,
            filamentCount = 3,
            outerColor = Color.rgb(236, 72, 153),
            midColor = Color.rgb(0, 245, 212),
            coreColor = Color.rgb(250, 204, 21),
            baseCrystalRadius = 5.4f,
            durationSec = 0.48f,
          )
          bombReflectedByBubble = true
          break
        }
      }
      if (!bombReflectedByBubble) {
        b.x = nx
        b.y = ny
      }

      // STASIS_BREACH: Two-Way Circular Stasis Bubble Jail reflection & Center Core bomb damage by prisoners only
      if (isStasisBreachRulesActive()) {
        val shooter = ships.firstOrNull { it.id == b.ownerId }
        val shooterInBrig = shooter?.isInStasisBrig == true
        val jails = listOf(Triple(0, westBrigReactorX, westBrigReactorY), Triple(1, eastBrigReactorX, eastBrigReactorY))
        var bombJailHandled = false
        for ((brigFreq, jx, jy) in jails) {
          val prevDist = hypot(b.x - b.vx * dt - jx, b.y - b.vy * dt - jy)
          val curDist = hypot(b.x - jx, b.y - jy)
          if (shooterInBrig && b.freq != brigFreq && curDist <= b.radius + 54f) {
            damageBrigReactorCore(
              targetBrigFreq = brigFreq,
              attacker = shooter,
              rawDamage = b.damage,
            )
            spawnExplosion(b.x, b.y, Color.parseColor("#F59E0B"))
            bombs.removeAt(i)
            bombJailHandled = true
            break
          }
          if (prevDist <= stasisJailRadius && curDist >= stasisJailRadius) {
            val dSafe = curDist.coerceAtLeast(0.001f)
            val rnx = (b.x - jx) / dSafe
            val rny = (b.y - jy) / dSafe
            b.x = jx + rnx * (stasisJailRadius - 2f)
            b.y = jy + rny * (stasisJailRadius - 2f)
            val dot = b.vx * rnx + b.vy * rny
            if (dot > 0f) {
              b.vx -= 2f * dot * rnx
              b.vy -= 2f * dot * rny
            }
          } else if (prevDist > stasisJailRadius && curDist <= stasisJailRadius) {
            val dSafe = curDist.coerceAtLeast(0.001f)
            val rnx = if (curDist > 0.001f) (b.x - jx) / dSafe else 1f
            val rny = if (curDist > 0.001f) (b.y - jy) / dSafe else 0f
            b.x = jx + rnx * (stasisJailRadius + 2f)
            b.y = jy + rny * (stasisJailRadius + 2f)
            val dot = b.vx * rnx + b.vy * rny
            if (dot < 0f) {
              b.vx -= 2f * dot * rnx
              b.vy -= 2f * dot * rny
            }
          }
        }
        if (bombJailHandled) continue
      }

      // Check Decoy hologram soaking bombs/missiles
      var soakedByDecoy = false
      for (dIdx in decoys.indices.reversed()) {
        val dec = decoys[dIdx]
        if (dec.ownerId != b.ownerId && dec.freq != b.freq && hypot(dec.x - b.x, dec.y - b.y) < 42f) {
          soakedByDecoy = true
          spawnExplosion(dec.x, dec.y, Color.parseColor("#2DD4BF"))
          decoys.removeAt(dIdx)
          if (b.type != "CHAKRAM") {
            bombs.removeAt(i)
          }
          break
        }
      }
      if (soakedByDecoy && b.type != "CHAKRAM") continue

      if (b.type == "CHAKRAM") {
        for (s in ships) {
          if (
            !(isInLobbyStaging && s.isLocalPlayer) &&
              !isBombAlliedWithShip(b, s) &&
              s.respawnTimer <= 0f &&
              !isInsideSafeZone(s.x, s.y) &&
              b.recentHitShipTimers[s.id] == null &&
              hypot(s.x - b.x, s.y - b.y) < s.radius + b.radius + 6f
          ) {
            b.recentHitShipTimers[s.id] = 0.32f
            val incAngle = atan2(b.y - s.y, b.x - s.x)
            applyProjectileDamage(
              target = s,
              attackerId = b.ownerId,
              attackerIsHuman = b.ownerIsHuman,
              rawDamage = b.damage,
              impactAngle = incAngle,
              isHeavyImpact = true,
              isEmp = false,
              attackerShipLabel = b.ownerShipLabel,
              weaponLabel = b.weaponLabel,
            )
            shockwaves.add(ShockwaveEntity(s.x, s.y, 12f, 95f, Color.parseColor("#F59E0B")))
          }
        }
        continue
      }

      val triggerRadius =
        when (b.type) {
          "PROX" -> RiftCalibrationState.proxFuseRadius
          "MINE" -> b.radius + 28f
          "MISSILE", "PROX_MISSILE" -> b.radius + 10f
          "CUSTOM_ORB" -> if (b.customEffect == CustomBuffEffect.PULL) b.radius + 6f else b.radius + 14f
          else -> b.radius
        }
      for (s in ships) {
        if (!(isInLobbyStaging && s.isLocalPlayer) && !isBombAlliedWithShip(b, s) && s.respawnTimer <= 0f) {
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
              if (!(isInLobbyStaging && victim.isLocalPlayer) && !isBombAlliedWithShip(b, victim) && victim.respawnTimer <= 0f && !isInsideSafeZone(victim.x, victim.y)) {
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
            applyExplosionToObstacles(
              blastX = b.x,
              blastY = b.y,
              blastRadius = blastRadius,
              baseDamage = b.damage * 0.75f,
              attackerId = b.ownerId,
            )
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
                "PROX_MISSILE" -> Color.parseColor("#FACC15")
                else -> Color.parseColor("#F59E0B")
              },
            )
            bombs.removeAt(i)
            break
          }
        }
      }
    }

    // 4b. Step Autonomous Zig-Zagging Swarm Drones (5-Drone SWARM Special Buff)
    val droneCruiseSpeed = 560f * customSpdMult
    val droneTurnRate = 8.4f * customSpdMult
    for (drIdx in swarmDrones.indices.reversed()) {
      val dr = swarmDrones[drIdx]
      dr.life -= dt
      dr.zigPhase += dt
      if (dr.life <= 0f || isInsideSafeZone(dr.x, dr.y)) {
        if (dr.life <= 0f) {
          spawnExplosion(dr.x, dr.y, Color.parseColor("#00F5D4"))
        }
        swarmDrones.removeAt(drIdx)
        continue
      }

      // Target nearest hostile decoy first, else nearest living uncloaked hostile ship within 1550px
      val decoyTarget =
        decoys
          .filter { it.ownerId != dr.ownerId && it.freq != dr.freq && !isInsideSafeZone(it.x, it.y) }
          .minByOrNull { hypot(it.x - dr.x, it.y - dr.y) }
      if (decoyTarget != null && hypot(decoyTarget.x - dr.x, decoyTarget.y - dr.y) <= 1550f) {
        val baseAng = atan2(decoyTarget.y - dr.y, decoyTarget.x - dr.x)
        val weaveAng = baseAng + sin(dr.zigPhase * 11.5f) * 0.62f
        val diff = atan2(sin(weaveAng - dr.angle), cos(weaveAng - dr.angle))
        val maxTurn = droneTurnRate * dt
        dr.angle += diff.coerceIn(-maxTurn, maxTurn)
      } else {
        var lockedShip =
          ships.firstOrNull {
            !(isInLobbyStaging && it.isLocalPlayer) &&
              it.id == dr.targetShipId &&
              !isDroneAlliedWithShip(dr, it) &&
              it.respawnTimer <= 0f &&
              !it.isCloaked &&
              !isInsideSafeZone(it.x, it.y)
          }
        if (lockedShip == null) {
          var bestDist = 1550f
          for (cand in ships) {
            if (
              !(isInLobbyStaging && cand.isLocalPlayer) &&
                !isDroneAlliedWithShip(dr, cand) &&
                cand.respawnTimer <= 0f &&
                !cand.isCloaked &&
                !isInsideSafeZone(cand.x, cand.y)
            ) {
              val d = hypot(cand.x - dr.x, cand.y - dr.y)
              if (d < bestDist) {
                bestDist = d
                lockedShip = cand
              }
            }
          }
          dr.targetShipId = lockedShip?.id ?: ""
        }
        if (lockedShip != null) {
          val d = hypot(lockedShip.x - dr.x, lockedShip.y - dr.y)
          val leadT = (d / droneCruiseSpeed.coerceAtLeast(1f)).coerceIn(0f, 0.42f)
          val aimX = lockedShip.x + lockedShip.vx * leadT
          val aimY = lockedShip.y + lockedShip.vy * leadT
          val baseAng = atan2(aimY - dr.y, aimX - dr.x)
          val weaveAmp = if (d < 220f) 0.24f else 0.62f
          val weaveAng = baseAng + sin(dr.zigPhase * 11.5f) * weaveAmp
          val diff = atan2(sin(weaveAng - dr.angle), cos(weaveAng - dr.angle))
          val maxTurn = droneTurnRate * dt
          dr.angle += diff.coerceIn(-maxTurn, maxTurn)
        } else {
          dr.angle += sin(dr.zigPhase * 8.5f) * 2.2f * dt
        }
      }

      dr.vx = cos(dr.angle) * droneCruiseSpeed
      dr.vy = sin(dr.angle) * droneCruiseSpeed
      val dnx = dr.x + dr.vx * dt
      val dny = dr.y + dr.vy * dt

      if (isInsideSafeZone(dnx, dny)) {
        swarmDrones.removeAt(drIdx)
        continue
      }

      val droneWallHit = segmentFirstWallHit(dr.x, dr.y, dnx, dny, pad = 6f)
      if (droneWallHit != null) {
        if (droneWallHit.wallIndex >= 0) {
          onObstacleProjectileHit(
            wallIndex = droneWallHit.wallIndex,
            damage = dr.damage * 0.35f,
            hitX = droneWallHit.hitX,
            hitY = droneWallHit.hitY,
            attackerId = dr.ownerId,
          )
        }
        dr.x = droneWallHit.hitX + droneWallHit.normalX * 4f
        dr.y = droneWallHit.hitY + droneWallHit.normalY * 4f
        val dot = dr.vx * droneWallHit.normalX + dr.vy * droneWallHit.normalY
        if (dot < 0f) {
          dr.vx -= 2f * dot * droneWallHit.normalX
          dr.vy -= 2f * dot * droneWallHit.normalY
        }
        dr.angle = atan2(dr.vy, dr.vx)
        continue
      }

      val nextPerimDist = hypot(dnx - arenaCenterX, dny - arenaCenterY)
      if (nextPerimDist + 12f >= arenaRadius) {
        val dSafe = nextPerimDist.coerceAtLeast(0.001f)
        val rnx = (dnx - arenaCenterX) / dSafe
        val rny = (dny - arenaCenterY) / dSafe
        dr.x = arenaCenterX + rnx * (arenaRadius - 15f)
        dr.y = arenaCenterY + rny * (arenaRadius - 15f)
        val dot = dr.vx * rnx + dr.vy * rny
        if (dot > 0f) {
          dr.vx -= 2f * dot * rnx
          dr.vy -= 2f * dot * rny
        }
        dr.angle = atan2(dr.vy, dr.vx)
        continue
      }

      var droneReflected = false
      for (ib in interdictorBubbles) {
        val prevDist = hypot(dr.x - ib.x, dr.y - ib.y)
        val curDist = hypot(dnx - ib.x, dny - ib.y)
        if ((prevDist <= ib.radius && curDist >= ib.radius) || (prevDist > ib.radius && curDist <= ib.radius)) {
          val dSafe = curDist.coerceAtLeast(0.001f)
          val rnx = (dnx - ib.x) / dSafe
          val rny = (dny - ib.y) / dSafe
          dr.x = ib.x + rnx * (if (prevDist <= ib.radius) ib.radius - 4f else ib.radius + 4f)
          dr.y = ib.y + rny * (if (prevDist <= ib.radius) ib.radius - 4f else ib.radius + 4f)
          val dot = dr.vx * rnx + dr.vy * rny
          dr.vx -= 2f * dot * rnx
          dr.vy -= 2f * dot * rny
          dr.angle = atan2(dr.vy, dr.vx)
          droneReflected = true
          break
        }
      }
      if (!droneReflected) {
        dr.x = dnx
        dr.y = dny
      }

      // Check Decoy soak
      var droneSoaked = false
      for (decIdx in decoys.indices.reversed()) {
        val dec = decoys[decIdx]
        if (dec.ownerId != dr.ownerId && dec.freq != dr.freq && hypot(dec.x - dr.x, dec.y - dr.y) < 38f) {
          spawnExplosion(dec.x, dec.y, Color.parseColor("#00F5D4"))
          decoys.removeAt(decIdx)
          swarmDrones.removeAt(drIdx)
          droneSoaked = true
          break
        }
      }
      if (droneSoaked) continue

      // Check hostile ship impact
      for (s in ships) {
        if (!(isInLobbyStaging && s.isLocalPlayer) && !isDroneAlliedWithShip(dr, s) && s.respawnTimer <= 0f && !isInsideSafeZone(s.x, s.y)) {
          if (hypot(s.x - dr.x, s.y - dr.y) < s.radius + 16f) {
            val incAngle = atan2(dr.y - s.y, dr.x - s.x)
            applyProjectileDamage(
              target = s,
              attackerId = dr.ownerId,
              attackerIsHuman = dr.ownerIsHuman,
              rawDamage = dr.damage,
              impactAngle = incAngle,
              isHeavyImpact = true,
              isEmp = false,
              attackerShipLabel = dr.ownerShipLabel,
              weaponLabel = "Swarm Drone",
            )
            shockwaves.add(ShockwaveEntity(dr.x, dr.y, 12f, 110f, Color.parseColor("#00F5D4")))
            spawnExplosion(dr.x, dr.y, Color.parseColor("#00F5D4"))
            swarmDrones.removeAt(drIdx)
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

    // Step spinning 3D metallic ship debris chunks & glowing ember trails
    for (i in shipDebrisChunks.indices.reversed()) {
      val dc = shipDebrisChunks[i]
      dc.life -= rawDt
      if (dc.life <= 0f) {
        shipDebrisChunks.removeAt(i)
        continue
      }
      dc.x += dc.vx * dt
      dc.y += dc.vy * dt
      val drag = 0.42.pow(dt.toDouble()).toFloat()
      dc.vx *= drag
      dc.vy *= drag
      dc.angle += dc.angVel * dt
      dc.emberTimer += rawDt
      if (dc.emberTimer >= 0.06f && particles.size < 165) {
        dc.emberTimer = 0f
        particles.add(
          ParticleEntity(
            x = dc.x + (Random.nextFloat() - 0.5f) * dc.size * 0.5f,
            y = dc.y + (Random.nextFloat() - 0.5f) * dc.size * 0.5f,
            vx = (Random.nextFloat() - 0.5f) * 35f,
            vy = (Random.nextFloat() - 0.5f) * 35f,
            life = 0.26f,
            maxLife = 0.26f,
            size = 3.2f,
            color = if (Random.nextBoolean()) Color.parseColor("#F97316") else dc.trimColor,
          )
        )
      }
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

    for (i in reactivePrecipitates.indices.reversed()) {
      val pc = reactivePrecipitates[i]
      pc.life -= rawDt
      if (pc.life <= 0f) {
        reactivePrecipitates.removeAt(i)
        continue
      }
      if (pc.pullToAnchor) {
        val ax = pc.anchorX - pc.x
        val ay = pc.anchorY - pc.y
        val ad = hypot(ax, ay).coerceAtLeast(1f)
        val nx = ax / ad
        val ny = ay / ad
        // Swirl + radial pull into Rift singularity throat
        pc.vx += (nx * 360f - ny * 185f) * dt
        pc.vy += (ny * 360f + nx * 185f) * dt
      } else {
        val drag = 0.24.pow(dt.toDouble()).toFloat()
        pc.vx *= drag
        pc.vy *= drag
      }
      pc.x += pc.vx * dt
      pc.y += pc.vy * dt
      pc.angle += pc.angVel * dt
    }

    for (i in reactiveFilaments.indices.reversed()) {
      val fl = reactiveFilaments[i]
      fl.life -= rawDt
      if (fl.life <= 0f) {
        reactiveFilaments.removeAt(i)
        continue
      }
      fl.x1 += fl.vx1 * dt
      fl.y1 += fl.vy1 * dt
      fl.ctrlX += fl.vx1 * 0.45f * dt
      fl.ctrlY += fl.vy1 * 0.45f * dt
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

    // 8. Step FLUX CORE Singularity Mode (Overcharge Tiers, Bounty Tick, Safe Zone Ejection & 30s Supersonic Deceleration)
    stepFluxCoreMode(dt, rawDt)

    // 9. Step RIFT SKIRMISH 3-Minute Round Timer & Tiebreaker (paused during 10s between-round Ready-Up)
    if (isSkirmishRulesActive() && skirmishReadyCountdownSec <= 0f) {
      skirmishRoundTimerSec = max(0f, skirmishRoundTimerSec - rawDt)
      if (skirmishRoundTimerSec <= 0f) {
        val westAlive = ships.count { it.freq == 0 && it.respawnTimer <= 0f }
        val eastAlive = ships.count { it.freq == 1 && it.respawnTimer <= 0f }
        val winner =
          when {
            westAlive > eastAlive -> 0
            eastAlive > westAlive -> 1
            skirmishWestKills >= skirmishEastKills -> 0
            else -> 1
          }
        resolveSkirmishRoundWon(winningFreq = winner)
      }
    }

    // 10. Step RIFT CONTROL 2-Rift Tug-of-War Mode
    stepRiftControlMode(dt, rawDt)

    // 11. Step STASIS BREACH 3-Minute Capture Counter & Match Timer
    stepStasisBreachMode(rawDt)
  }

  private fun stepStasisBreachMode(rawDt: Float) {
    if (!isStasisBreachRulesActive()) return
    stasisBreachMatchTimerSec = max(0f, stasisBreachMatchTimerSec - rawDt)
    if (stasisBreachMatchTimerSec <= 0f) {
      val westJailedCount = ships.count { it.freq == 0 && it.isInStasisBrig }
      val eastJailedCount = ships.count { it.freq == 1 && it.isInStasisBrig }
      val winnerFreq =
        when {
          stasisWestCaptures > stasisEastCaptures -> 0
          stasisEastCaptures > stasisWestCaptures -> 1
          eastJailedCount > westJailedCount -> 0
          westJailedCount > eastJailedCount -> 1
          eastBrigGeneratorHp < westBrigGeneratorHp -> 0
          else -> 0
        }
      finishStasisBreachMatch(winningFreq = winnerFreq, isTotalLockdown = false)
    }
  }

  private fun stepRiftControlMode(dt: Float, rawDt: Float) {
    if (!isRiftControlRulesActive()) return

    fun updateZone(
      zx: Float,
      zy: Float,
      curControl: Float,
      curLockedBy: Int,
      zoneLabel: String,
      isAlpha: Boolean,
    ): Pair<Float, Int> {
      var westInside = 0
      var eastInside = 0
      for (s in ships) {
        if (
          !(isInLobbyStaging && s.isLocalPlayer) &&
            s.respawnTimer <= 0f &&
            !s.isCloaked &&
            hypot(s.x - zx, s.y - zy) <= riftCaptureRadius
        ) {
          if (s.freq == 0) westInside++ else if (s.freq == 1) eastInside++
        }
      }
      if (isAlpha) {
        riftAlphaWestCount = westInside
        riftAlphaEastCount = eastInside
      } else {
        riftOmegaWestCount = westInside
        riftOmegaEastCount = eastInside
      }

      var nextControl = curControl
      var nextLocked = curLockedBy

      if (westInside > eastInside) {
        val adv = (westInside - eastInside).coerceIn(1, 4)
        val advMult = 1.0f + (adv - 1) * 0.25f
        val reversalMult = if (curControl > 0f) 1.5f else 1.0f
        nextControl = max(-15f, curControl - rawDt * advMult * reversalMult)
        if (curLockedBy == 1 && nextControl < 7.5f) {
          nextLocked = -1
          onPrizeToast("$zoneLabel UNLOCKED — WEST HORIZON CONTESTING!")
        }
        if (nextControl <= -15f && curLockedBy != 0) {
          nextLocked = 0
          shockwaves.add(ShockwaveEntity(zx, zy, 24f, riftCaptureRadius * 1.3f, Color.parseColor("#38BDF8")))
          sfx.playPrize()
          if (hasJoinedMatch && localPlayer.freq == 0) {
            adjustLocalResonance(6, "RIFT CAPTURE")
          }
          onPrizeToast("$zoneLabel CAPTURED BY WEST HORIZON!")
        }
      } else if (eastInside > westInside) {
        val adv = (eastInside - westInside).coerceIn(1, 4)
        val advMult = 1.0f + (adv - 1) * 0.25f
        val reversalMult = if (curControl < 0f) 1.5f else 1.0f
        nextControl = min(15f, curControl + rawDt * advMult * reversalMult)
        if (curLockedBy == 0 && nextControl > -7.5f) {
          nextLocked = -1
          onPrizeToast("$zoneLabel UNLOCKED — EAST HORIZON CONTESTING!")
        }
        if (nextControl >= 15f && curLockedBy != 1) {
          nextLocked = 1
          shockwaves.add(ShockwaveEntity(zx, zy, 24f, riftCaptureRadius * 1.3f, Color.parseColor("#EF4444")))
          sfx.playWarp()
          if (hasJoinedMatch && localPlayer.freq == 1) {
            adjustLocalResonance(6, "RIFT CAPTURE")
          }
          onPrizeToast("$zoneLabel CAPTURED BY EAST HORIZON!")
        }
      } else if (westInside == 0 && eastInside == 0 && curLockedBy == -1) {
        if (curControl < 0f) {
          nextControl = min(0f, curControl + 0.25f * rawDt)
        } else if (curControl > 0f) {
          nextControl = max(0f, curControl - 0.25f * rawDt)
        }
      }
      return nextControl to nextLocked
    }

    val (alphaCtrl, alphaLock) =
      updateZone(riftAlphaX, riftAlphaY, riftAlphaControlSec, riftAlphaLockedBy, "RIFT ALPHA", isAlpha = true)
    riftAlphaControlSec = alphaCtrl
    riftAlphaLockedBy = alphaLock

    val (omegaCtrl, omegaLock) =
      updateZone(riftOmegaX, riftOmegaY, riftOmegaControlSec, riftOmegaLockedBy, "RIFT OMEGA", isAlpha = false)
    riftOmegaControlSec = omegaCtrl
    riftOmegaLockedBy = omegaLock

    riftControlMatchTimerSec = max(0f, riftControlMatchTimerSec - rawDt)

    val westLocks = (if (riftAlphaLockedBy == 0) 1 else 0) + (if (riftOmegaLockedBy == 0) 1 else 0)
    val eastLocks = (if (riftAlphaLockedBy == 1) 1 else 0) + (if (riftOmegaLockedBy == 1) 1 else 0)
    val westRate =
      when (westLocks) {
        2 -> 10.0f
        1 -> 4.0f
        else -> 0f
      }
    val eastRate =
      when (eastLocks) {
        2 -> 10.0f
        1 -> 4.0f
        else -> 0f
      }
    riftControlWestScore = (riftControlWestScore + westRate * rawDt).coerceAtMost(1000f)
    riftControlEastScore = (riftControlEastScore + eastRate * rawDt).coerceAtMost(1000f)

    if (riftControlWestScore >= 1000f || riftControlEastScore >= 1000f || riftControlMatchTimerSec <= 0f) {
      if (hasJoinedMatch) {
        val westWon =
          when {
            riftControlWestScore >= 1000f && riftControlWestScore >= riftControlEastScore -> true
            riftControlEastScore >= 1000f -> false
            riftControlWestScore > riftControlEastScore + 0.5f -> true
            riftControlEastScore > riftControlWestScore + 0.5f -> false
            westLocks > eastLocks -> true
            eastLocks > westLocks -> false
            else -> (-riftAlphaControlSec - riftOmegaControlSec) >= 0f
          }
        val localTeamWon = if (localPlayer.freq == 1) !westWon else westWon
        val winTeamLabel = if (westWon) "WEST HORIZON" else "EAST HORIZON"
        onDailyDirectiveEvent(DailyDirectiveMetric.MATCHES_PLAYED, 1, false)
        if (localTeamWon) {
          riftShards += 85L
          convergencesWon++
          adjustLocalResonance(30, "RIFT CONTROL WIN")
          onSyndicateContribution(0L, localPlayer.bounty.toLong(), 1L)
          val msg =
            "RIFT CONTROL VICTORY! $winTeamLabel ${riftControlWestScore.toInt()}-${riftControlEastScore.toInt()} (+85 RIFT SHARDS)"
          highBountyShutdownBanner = msg
          highBountyShutdownTimer = 5.0f
          sfx.playPrize()
          onPrizeToast(msg)
          onStatsUpdated()
        } else {
          if (liveResonance >= 200) {
            adjustLocalResonance(-12, "MATCH LOSS")
          }
          val msg =
            "RIFT CONTROL CONCLUDED — $winTeamLabel WON ${riftControlWestScore.toInt()}-${riftControlEastScore.toInt()}"
          highBountyShutdownBanner = msg
          highBountyShutdownTimer = 4.5f
          sfx.playWarp()
          onPrizeToast(msg)
          onStatsUpdated()
        }
      }
      swarmDrones.clear()
      brickWalls.clear()
      shipDebrisChunks.clear()
      bakeStaticRadarBitmap()
      resetRiftControlZones()
      reattachRemotePilotsToRoster()
      for (s in ships) {
        s.deathLingerTimer = 0f
        s.warpInAnimTimer = 0.65f
        s.maxFlux = s.spec.maxFlux * (currentCustomGame?.healthMult ?: 1.0f)
        s.flux = s.maxFlux
        s.vx = 0f
        s.vy = 0f
        if (s.isLocalPlayer && !hasJoinedMatch) {
          s.respawnTimer = 999f
          s.x = arenaCenterX
          s.y = arenaCenterY
        } else {
          s.respawnTimer = 0f
          val sp = getSkirmishGateSpawnPosition(s.freq)
          s.x = sp.first
          s.y = sp.second
          if (s.isHuman && !s.isLocalPlayer) {
            s.netTargetX = s.x
            s.netTargetY = s.y
            s.netTargetVx = 0f
            s.netTargetVy = 0f
            s.netPacketAgeSec = 0f
            s.netSmoothOffsetX = 0f
            s.netSmoothOffsetY = 0f
            s.netSmoothVelX = 0f
            s.netSmoothVelY = 0f
            s.netWasRespawning = false
            s.netLocalKillGraceSec = 0f
            s.netRespawnHoldSec = 0f
          }
          s.phaseShieldTimer = 3.0f
        }
      }
      notifyLocalLifecycleSync()
    }
  }

  private fun stepFluxCoreMode(dt: Float, rawDt: Float) {
    if (!isFluxCoreRulesActive()) return
    fluxCoreOrb.pulsePhase += rawDt * 4.2f
    if (fluxCoreOrb.pickupImmunitySec > 0f) {
      fluxCoreOrb.pickupImmunitySec = max(0f, fluxCoreOrb.pickupImmunitySec - rawDt)
    }
    val cid = fluxCoreCarrierEntityId
    if (cid.isNotEmpty()) {
      val carrier = ships.firstOrNull { it.id == cid && it.respawnTimer <= 0f }
      if (carrier == null) {
        fluxCoreCarrierEntityId = ""
        fluxCoreOrb.carrierShipId = -1
        fluxCoreOrb.overchargeTier = 0
        fluxCoreOrb.holdDurationSec = 0f
        fluxCoreBountyTickAccum = 0f
        return
      }
      if (isInsideSafeZone(carrier.x, carrier.y)) {
        ejectFluxCoreFromShip(carrier, "SAFE ZONE ENTRY")
        return
      }
      fluxCoreOrb.x = carrier.x
      fluxCoreOrb.y = carrier.y
      fluxCoreOrb.vx = carrier.vx
      fluxCoreOrb.vy = carrier.vy
      fluxCoreOrb.holdDurationSec += rawDt
      fluxCoreBountyTickAccum += rawDt

      val targetTier = (fluxCoreOrb.holdDurationSec / 5.0f).toInt().coerceIn(0, 10)
      if (targetTier > fluxCoreOrb.overchargeTier) {
        fluxCoreOrb.overchargeTier = targetTier
        shockwaves.add(ShockwaveEntity(carrier.x, carrier.y, 18f, 220f, Color.parseColor("#FACC15")))
        if (carrier.isLocalPlayer) {
          sfx.playPrize()
          spawnPooledFct(
            text = "FLUX CORE TIER ${fluxCoreOrb.overchargeTier}/10!",
            x = carrier.x,
            y = carrier.y - 36f,
            color = Color.parseColor("#FACC15"),
            maxLife = 1.15f,
          )
        }
      }

      if (fluxCoreBountyTickAccum >= 1.0f) {
        fluxCoreBountyTickAccum -= 1.0f
        val bountyTick = RiftCalibrationState.fluxCoreBountyPerSec.coerceAtLeast(1)
        carrier.bounty += bountyTick
        if (carrier.isLocalPlayer) {
          currentLifeStats.bountyReached = max(currentLifeStats.bountyReached, carrier.bounty)
          allTimeBountyPeak = max(allTimeBountyPeak, carrier.bounty.toLong())
          pvpBountyPeak = max(pvpBountyPeak, carrier.bounty.toLong())
          onDailyDirectiveEvent(DailyDirectiveMetric.BOUNTY_PEAK_IN_LIFE, carrier.bounty, true)
          onStatsUpdated()
        }
      }
    } else {
      // Free-floating or supersonic-ejected Flux Core Orb
      if (fluxCoreOrb.ejectDecelTimerSec > 0f) {
        fluxCoreOrb.ejectDecelTimerSec = max(0f, fluxCoreOrb.ejectDecelTimerSec - rawDt)
        val decelRatio = (fluxCoreOrb.ejectDecelTimerSec / 30.0f).coerceIn(0f, 1f)
        val targetSpd = RiftCalibrationState.fluxCoreEjectSpeed * decelRatio
        val curSpd = hypot(fluxCoreOrb.vx, fluxCoreOrb.vy)
        if (curSpd > 0.01f) {
          fluxCoreOrb.vx = (fluxCoreOrb.vx / curSpd) * targetSpd
          fluxCoreOrb.vy = (fluxCoreOrb.vy / curSpd) * targetSpd
        }
        if (fluxCoreOrb.ejectDecelTimerSec <= 0f) {
          fluxCoreOrb.vx = 0f
          fluxCoreOrb.vy = 0f
        }
      }

      if (hypot(fluxCoreOrb.vx, fluxCoreOrb.vy) > 1f) {
        val nx = fluxCoreOrb.x + fluxCoreOrb.vx * dt
        val ny = fluxCoreOrb.y + fluxCoreOrb.vy * dt
        val orbR = 32f
        var bounced = false
        val maxOrbDist = (arenaRadius - orbR - 24f).coerceAtLeast(500f)
        val nextDist = hypot(nx - arenaCenterX, ny - arenaCenterY)
        if (nextDist > maxOrbDist) {
          val rnx = (nx - arenaCenterX) / nextDist.coerceAtLeast(0.001f)
          val rny = (ny - arenaCenterY) / nextDist.coerceAtLeast(0.001f)
          val dot = fluxCoreOrb.vx * rnx + fluxCoreOrb.vy * rny
          if (dot > 0f) {
            fluxCoreOrb.vx -= 2f * dot * rnx
            fluxCoreOrb.vy -= 2f * dot * rny
          }
          fluxCoreOrb.x = arenaCenterX + rnx * maxOrbDist
          fluxCoreOrb.y = arenaCenterY + rny * maxOrbDist
          bounced = true
        }
        if (!bounced && hasActiveSafeZone()) {
          val szLimit = getSafeZoneRadius() + orbR + 4f
          val szDx = nx - arenaCenterX
          val szDy = ny - arenaCenterY
          val szDist = hypot(szDx, szDy)
          if (szDist <= szLimit) {
            val dSafe = szDist.coerceAtLeast(0.001f)
            val rnx = if (szDist > 0.001f) szDx / dSafe else 0f
            val rny = if (szDist > 0.001f) szDy / dSafe else -1f
            val dot = fluxCoreOrb.vx * rnx + fluxCoreOrb.vy * rny
            if (dot < 0f) {
              fluxCoreOrb.vx -= 2f * dot * rnx
              fluxCoreOrb.vy -= 2f * dot * rny
            }
            fluxCoreOrb.x = arenaCenterX + rnx * (szLimit + 2f)
            fluxCoreOrb.y = arenaCenterY + rny * (szLimit + 2f)
            bounced = true
          }
        }
        if (!bounced) {
          for (w in walls) {
            if (nx + orbR >= w.x && nx - orbR <= w.x + w.w && ny + orbR >= w.y && ny - orbR <= w.y + w.h) {
              val prevInX = fluxCoreOrb.x + orbR >= w.x && fluxCoreOrb.x - orbR <= w.x + w.w
              if (!prevInX) fluxCoreOrb.vx = -fluxCoreOrb.vx else fluxCoreOrb.vy = -fluxCoreOrb.vy
              bounced = true
              break
            }
          }
        }
        if (!bounced) {
          for (bw in brickWalls) {
            if (nx + orbR >= bw.x && nx - orbR <= bw.x + bw.w && ny + orbR >= bw.y && ny - orbR <= bw.y + bw.h) {
              val prevInX = fluxCoreOrb.x + orbR >= bw.x && fluxCoreOrb.x - orbR <= bw.x + bw.w
              if (!prevInX) fluxCoreOrb.vx = -fluxCoreOrb.vx else fluxCoreOrb.vy = -fluxCoreOrb.vy
              bounced = true
              break
            }
          }
        }
        val movedX = fluxCoreOrb.x + fluxCoreOrb.vx * dt
        val movedY = fluxCoreOrb.y + fluxCoreOrb.vy * dt
        val movedDist = hypot(movedX - arenaCenterX, movedY - arenaCenterY)
        if (movedDist <= maxOrbDist) {
          fluxCoreOrb.x = movedX
          fluxCoreOrb.y = movedY
        } else {
          fluxCoreOrb.x = arenaCenterX + ((movedX - arenaCenterX) / movedDist) * maxOrbDist
          fluxCoreOrb.y = arenaCenterY + ((movedY - arenaCenterY) / movedDist) * maxOrbDist
        }
      }

      // Check pickup by any alive ship outside the Safe Zone (`hypot <= ship.radius + 46f`)
      if (fluxCoreOrb.pickupImmunitySec <= 0f) {
        for (s in ships) {
          if (s.respawnTimer <= 0f && !isInsideSafeZone(s.x, s.y)) {
            if (hypot(s.x - fluxCoreOrb.x, s.y - fluxCoreOrb.y) <= s.radius + 46f) {
              fluxCoreCarrierEntityId = s.id
              fluxCoreOrb.carrierShipId = s.shipId
              fluxCoreOrb.overchargeTier = 1
              fluxCoreOrb.holdDurationSec = 0f
              fluxCoreBountyTickAccum = 0f
              fluxCoreOrb.ejectDecelTimerSec = 0f
              fluxCoreOrb.vx = 0f
              fluxCoreOrb.vy = 0f
              shockwaves.add(ShockwaveEntity(s.x, s.y, 20f, 260f, Color.parseColor("#FACC15")))
              if (s.isLocalPlayer) {
                sfx.playPrize()
                onPrizeToast("FLUX CORE ACQUIRED! +15 RES/S & OVERCHARGE TIER SCALING ACTIVE!")
              } else if (!isHubOverwatchActive || hasJoinedMatch) {
                onPrizeToast("${s.callsign} CLAIMED THE FLUX CORE!")
              }
              break
            }
          }
        }
      }
    }
  }

  private fun stepSpatialAnomalies(dt: Float, rawDt: Float) {
    // 1. Update cooldown timers on all ships & entities
    for (s in ships) {
      if (s.blackHoleImmunityTimer > 0f) {
        s.blackHoleImmunityTimer = max(0f, s.blackHoleImmunityTimer - dt)
      }
      if (s.phaseRiftCooldownTimer > 0f) {
        s.phaseRiftCooldownTimer = max(0f, s.phaseRiftCooldownTimer - dt)
      }
    }
    for (b in bullets) {
      if (b.phaseRiftCooldownTimer > 0f) {
        b.phaseRiftCooldownTimer = max(0f, b.phaseRiftCooldownTimer - dt)
      }
    }
    for (bm in bombs) {
      if (bm.phaseRiftCooldownTimer > 0f) {
        bm.phaseRiftCooldownTimer = max(0f, bm.phaseRiftCooldownTimer - dt)
      }
    }
    for (d in decoys) {
      if (d.phaseRiftCooldownTimer > 0f) {
        d.phaseRiftCooldownTimer = max(0f, d.phaseRiftCooldownTimer - dt)
      }
    }
    for (dr in swarmDrones) {
      if (dr.phaseRiftCooldownTimer > 0f) {
        dr.phaseRiftCooldownTimer = max(0f, dr.phaseRiftCooldownTimer - dt)
      }
    }

    // 1b. Live Unstable Rift Convergence: when ANY two active Playfield Rifts (including Paired Phase Rifts) overlap, merge into a calm-spinning 10.0s Unstable Rift!
    var mergedPairFound = true
    while (mergedPairFound && anomalies.size >= 2) {
      mergedPairFound = false
      outer@ for (i in 0 until anomalies.size - 1) {
        val a = anomalies[i]
        for (j in i + 1 until anomalies.size) {
          val b = anomalies[j]
          if (a.isUnstableMergedRift && b.isUnstableMergedRift) continue
          val overlapThreshold = (a.physicalRadius + b.physicalRadius) * 0.88f
          if (hypot(a.x - b.x, a.y - b.y) <= overlapThreshold) {
            val mx = (a.x + b.x) * 0.5f
            val my = (a.y + b.y) * 0.5f
            val mergedRadius = (max(a.physicalRadius, b.physicalRadius) * 1.30f).coerceIn(235f, 380f)
            val removedIdA = a.id
            val removedIdB = b.id
            a.x = mx
            a.y = my
            a.isUnstableMergedRift = true
            a.life = 10.0f
            a.maxLife = 10.0f
            a.physicalRadius = mergedRadius
            a.partnerId = ""
            anomalies.removeAt(j)
            for (rem in anomalies) {
              if (rem.partnerId == removedIdA || rem.partnerId == removedIdB) {
                rem.partnerId = ""
              }
            }
            relocateOverlappingCrates()
            shockwaves.add(ShockwaveEntity(mx, my, 28f, mergedRadius * 1.85f, Color.parseColor("#FACC15")))
            shockwaves.add(ShockwaveEntity(mx, my, 18f, mergedRadius * 1.35f, Color.parseColor("#EC4899")))
            shockwaves.add(ShockwaveEntity(mx, my, 12f, mergedRadius * 0.95f, Color.parseColor("#00F5D4")))
            if (!isHubOverwatchActive || hasJoinedMatch) {
              val shelterHint = if (hasActiveSafeZone()) "RETURN TO SAFE ZONE" else "EVACUATE BLAST ZONE"
              highBountyShutdownBanner = "UNSTABLE RIFT CONVERGENCE // $shelterHint"
              highBountyShutdownTimer = 10.0f
              sfx.playWarp()
              addTrauma(0.35f)
              onPrizeToast("UNSTABLE RIFT CONVERGENCE // 10.0s DETONATION — $shelterHint")
            }
            mergedPairFound = true
            break@outer
          }
        }
      }
    }

    // 2. Step anomaly life, rotation, physics fields & Unstable Rift Convergence Detonation
    val expiredPairsToRespawn = mutableListOf<AnomalyType>()
    val expiredSinglesToRespawn = mutableListOf<AnomalyType>()
    var unstableDetonatedThisStep = false

    for (aIdx in anomalies.indices.reversed()) {
      val anom = anomalies[aIdx]
      anom.life -= rawDt
      if (anom.lastIngressTimer > 0f) anom.lastIngressTimer = max(0f, anom.lastIngressTimer - rawDt)
      if (anom.secondaryIngressTimer > 0f) anom.secondaryIngressTimer = max(0f, anom.secondaryIngressTimer - rawDt)

      if (anom.isUnstableMergedRift) {
        // Calm, ominous spacetime-tear rotation speed
        anom.spinAngle = (anom.spinAngle + 0.55f * dt) % (PI.toFloat() * 2f)
        anom.physicalRadius = (anom.physicalRadius + 6f * rawDt).coerceAtMost(420f)
        if (Random.nextFloat() < 0.55f && particles.size < 130) {
          val pAngle = Random.nextFloat() * (PI.toFloat() * 2f)
          val pDist = anom.physicalRadius * (0.25f + Random.nextFloat() * 0.92f)
          val pCol =
            when (Random.nextInt(4)) {
              0 -> Color.parseColor("#00F5D4")
              1 -> Color.parseColor("#EC4899")
              2 -> Color.parseColor("#FACC15")
              else -> Color.parseColor("#A855F7")
            }
          particles.add(
            ParticleEntity(
              x = anom.x + cos(pAngle) * pDist,
              y = anom.y + sin(pAngle) * pDist,
              vx = -sin(pAngle) * 62f,
              vy = cos(pAngle) * 62f,
              life = 0.42f,
              maxLife = 0.42f,
              size = 4.0f,
              color = pCol,
            )
          )
        }

        // 0.0s Convergence Detonation: insta-kill ships outside the Safe Zone (-25 Live Resonance, floored at 0) + shatter nearby Crystal Asteroids!
        if (anom.life <= 0f) {
          val blastR = anom.physicalRadius * 2.15f
          shockwaves.add(ShockwaveEntity(anom.x, anom.y, 42f, max(blastR, arenaRadius * 1.15f), Color.parseColor("#FACC15")))
          shockwaves.add(ShockwaveEntity(anom.x, anom.y, 26f, blastR, Color.parseColor("#EC4899")))
          shockwaves.add(ShockwaveEntity(anom.x, anom.y, 16f, blastR * 0.62f, Color.parseColor("#00F5D4")))
          spawnExplosion(anom.x, anom.y, Color.parseColor("#FACC15"))
          spawnExplosion(anom.x + 48f, anom.y - 36f, Color.parseColor("#EC4899"))
          spawnExplosion(anom.x - 48f, anom.y + 36f, Color.parseColor("#00F5D4"))

          val livingTargetsSnapshot = ships.toList()
          for (s in livingTargetsSnapshot) {
            if ((isInLobbyStaging && s.isLocalPlayer) || s.respawnTimer > 0f || isInsideSafeZone(s.x, s.y)) continue
            s.phaseDashTimer = 0f
            s.phaseShieldTimer = 0f
            val instaKillDamage = s.flux + s.maxFlux + 99999f
            applyProjectileDamage(
              target = s,
              attackerId = "unstable_rift",
              attackerIsHuman = false,
              rawDamage = instaKillDamage,
              impactAngle = atan2(anom.y - s.y, anom.x - s.x),
              isHeavyImpact = true,
              isEmp = false,
              attackerShipLabel = "Unstable Rift",
              weaponLabel = "Unstable Rift Convergence",
              forceUnstableRiftInstaKill = true,
            )
          }

          // Remove the detonated Unstable Rift first so dropped asteroid crates resolve clear positions cleanly
          val detX = anom.x
          val detY = anom.y
          anomalies.removeAt(aIdx)

          // Shatter all destructible Crystal Asteroids inside blast radius
          var shatteredAnyRock = false
          for (wIdx in walls.indices.reversed()) {
            val w = walls[wIdx]
            if (w.style != SpaceObstacleStyle.CRYSTAL_ASTEROID) continue
            val wCx = w.x + w.w * 0.5f
            val wCy = w.y + w.h * 0.5f
            if (hypot(wCx - detX, wCy - detY) <= blastR + 120f) {
              pendingAsteroidRespawns.add(
                PendingAsteroidRespawn(
                  x = w.x,
                  y = w.y,
                  w = w.w,
                  h = w.h,
                  variantSeed = w.variantSeed,
                  timerSec = 20.0f,
                )
              )
              walls.removeAt(wIdx)
              val clearDrop = resolveClearCrateDropPosition(wCx, wCy)
              greens.add(
                GreenCrate(
                  x = clearDrop.first,
                  y = clearDrop.second,
                  spinPhase = Random.nextFloat() * (PI.toFloat() * 2f),
                  buffType = rollRandomCrateBuffType(),
                )
              )
              spawnExplosion(wCx, wCy, Color.parseColor("#C084FC"))
              shatteredAnyRock = true
            }
          }
          if (shatteredAnyRock) {
            bakeStaticRadarBitmap()
          }
          if (hasJoinedMatch && !isInLobbyStaging) {
            sfx.playExplosion()
            addTrauma(0.65f)
            spawnPooledFct(
              text = "UNSTABLE RIFT CONVERGENCE // SAFE ZONE ONLY",
              x = detX,
              y = detY - 36f,
              color = Color.parseColor("#FACC15"),
              maxLife = 1.55f,
            )
          }
          unstableDetonatedThisStep = true
          continue
        }

        // Unstable Rift active 10.0s shear & pull field
        val pullR = anom.physicalRadius * 2.15f
        val shearR = anom.physicalRadius * 0.68f
        for (s in ships) {
          if (s.respawnTimer > 0f || isInsideSafeZone(s.x, s.y)) continue
          val dx = anom.x - s.x
          val dy = anom.y - s.y
          val dist = hypot(dx, dy).coerceAtLeast(0.01f)
          if (dist < pullR) {
            val nx = dx / dist
            val ny = dy / dist
            val pullMag = 840f * (1f - dist / pullR).coerceIn(0.22f, 1f)
            s.vx += nx * pullMag * dt
            s.vy += ny * pullMag * dt
          }
          if (dist < shearR + s.radius * 0.4f && s.plasmaRiftTickTimer <= 0f) {
            s.plasmaRiftTickTimer = 0.55f
            val dmg = s.maxFlux * 0.12f
            s.flux -= dmg
            anom.recordBoundaryIngress(atan2(s.y - anom.y, s.x - anom.x), 0.55f)
            spawnDropletReactionBurst(
              originX = s.x,
              originY = s.y,
              targetOrWakeX = anom.x,
              targetOrWakeY = anom.y,
              pullInward = true,
              crystalCount = 4,
              filamentCount = 3,
              outerColor = Color.rgb(236, 72, 153),
              midColor = Color.rgb(0, 245, 212),
              coreColor = Color.rgb(250, 204, 21),
              baseCrystalRadius = 6.0f,
            )
            if (s.isLocalPlayer) {
              currentLifeStats.damageTaken += dmg
              addTrauma(0.20f)
            }
            if (s.flux <= 0f) {
              destroyShipFromPlasmaRift(s)
            }
          }
        }
        for (bIdx in bullets.indices.reversed()) {
          val b = bullets[bIdx]
          val dx = anom.x - b.x
          val dy = anom.y - b.y
          val dist = hypot(dx, dy).coerceAtLeast(0.01f)
          if (dist < anom.physicalRadius * 0.42f) {
            anom.recordBoundaryIngress(atan2(b.y - anom.y, b.x - anom.x), 0.48f)
            spawnDropletReactionBurst(
              originX = b.x,
              originY = b.y,
              targetOrWakeX = anom.x,
              targetOrWakeY = anom.y,
              pullInward = true,
              crystalCount = 3,
              filamentCount = 2,
              outerColor = Color.rgb(250, 204, 21),
              midColor = Color.rgb(0, 245, 212),
              coreColor = Color.WHITE,
              baseCrystalRadius = 5.0f,
            )
            bullets.removeAt(bIdx)
            anom.physicalRadius = (anom.physicalRadius + 4f).coerceAtMost(420f)
          } else if (dist < pullR * 0.9f) {
            val nx = dx / dist
            val ny = dy / dist
            val sp = hypot(b.vx, b.vy).coerceAtLeast(320f)
            b.vx += nx * 1650f * dt
            b.vy += ny * 1650f * dt
            val newSp = hypot(b.vx, b.vy).coerceAtLeast(1f)
            b.vx = (b.vx / newSp) * sp
            b.vy = (b.vy / newSp) * sp
          }
        }
        for (bmIdx in bombs.indices.reversed()) {
          val bm = bombs[bmIdx]
          val dx = anom.x - bm.x
          val dy = anom.y - bm.y
          val dist = hypot(dx, dy).coerceAtLeast(0.01f)
          if (dist < anom.physicalRadius * 0.44f) {
            anom.recordBoundaryIngress(atan2(bm.y - anom.y, bm.x - anom.x), 0.58f)
            spawnDropletReactionBurst(
              originX = bm.x,
              originY = bm.y,
              targetOrWakeX = anom.x,
              targetOrWakeY = anom.y,
              pullInward = true,
              crystalCount = 5,
              filamentCount = 3,
              outerColor = Color.rgb(236, 72, 153),
              midColor = Color.rgb(250, 204, 21),
              coreColor = Color.rgb(0, 245, 212),
              baseCrystalRadius = 6.6f,
            )
            bombs.removeAt(bmIdx)
            anom.physicalRadius = (anom.physicalRadius + 12f).coerceAtMost(420f)
          } else if (dist < pullR * 0.9f) {
            val nx = dx / dist
            val ny = dy / dist
            val sp = hypot(bm.vx, bm.vy).coerceAtLeast(220f)
            bm.vx += nx * 1250f * dt
            bm.vy += ny * 1250f * dt
            val newSp = hypot(bm.vx, bm.vy).coerceAtLeast(1f)
            bm.vx = (bm.vx / newSp) * sp
            bm.vy = (bm.vy / newSp) * sp
          }
        }
        for (drIdx in swarmDrones.indices.reversed()) {
          val dr = swarmDrones[drIdx]
          val dx = anom.x - dr.x
          val dy = anom.y - dr.y
          val dist = hypot(dx, dy).coerceAtLeast(0.01f)
          if (dist < anom.physicalRadius * 0.44f) {
            anom.recordBoundaryIngress(atan2(dr.y - anom.y, dr.x - anom.x), 0.50f)
            spawnDropletReactionBurst(
              originX = dr.x,
              originY = dr.y,
              targetOrWakeX = anom.x,
              targetOrWakeY = anom.y,
              pullInward = true,
              crystalCount = 3,
              filamentCount = 2,
              outerColor = Color.rgb(0, 245, 212),
              midColor = Color.rgb(250, 204, 21),
              coreColor = Color.WHITE,
              baseCrystalRadius = 5.4f,
            )
            swarmDrones.removeAt(drIdx)
            anom.physicalRadius = (anom.physicalRadius + 6f).coerceAtMost(420f)
          } else if (dist < pullR * 0.9f) {
            val nx = dx / dist
            val ny = dy / dist
            dr.vx += nx * 1150f * dt
            dr.vy += ny * 1150f * dt
            dr.angle = atan2(dr.vy, dr.vx)
          }
        }
        continue
      }

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
              anom.recordBoundaryIngress(atan2(s.y - anom.y, s.x - anom.x), 0.56f)
              spawnDropletReactionBurst(
                originX = s.x,
                originY = s.y,
                targetOrWakeX = s.x + s.vx * 0.16f,
                targetOrWakeY = s.y + s.vy * 0.16f,
                pullInward = false,
                crystalCount = 4,
                filamentCount = 3,
                outerColor = Color.rgb(239, 68, 68),
                midColor = Color.rgb(217, 70, 239),
                coreColor = Color.rgb(250, 204, 21),
                baseCrystalRadius = 5.8f,
              )
              if (s.isLocalPlayer) {
                currentLifeStats.damageTaken += dmg
                sfx.playBounce()
                addTrauma(0.24f)
                spawnPooledFct(
                  text = "PLASMA RIFT -${dmg.toInt()} FLUX (${(dmgPct * 100f).toInt()}%)",
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
          // Incinerate Holo Phantoms & Swarm Drones entering the Plasma Rift core
          for (dIdx in decoys.indices.reversed()) {
            val d = decoys[dIdx]
            if (hypot(d.x - anom.x, d.y - anom.y) < tearRadius + 18f) {
              anom.recordBoundaryIngress(atan2(d.y - anom.y, d.x - anom.x), 0.50f)
              spawnDropletReactionBurst(
                originX = d.x,
                originY = d.y,
                targetOrWakeX = anom.x,
                targetOrWakeY = anom.y,
                pullInward = true,
                crystalCount = 3,
                filamentCount = 2,
                outerColor = Color.rgb(239, 68, 68),
                midColor = Color.rgb(217, 70, 239),
                coreColor = Color.WHITE,
              )
              decoys.removeAt(dIdx)
              spawnExplosion(d.x, d.y, Color.parseColor("#EF4444"))
            }
          }
          for (drIdx in swarmDrones.indices.reversed()) {
            val dr = swarmDrones[drIdx]
            if (hypot(dr.x - anom.x, dr.y - anom.y) < tearRadius + 14f) {
              anom.recordBoundaryIngress(atan2(dr.y - anom.y, dr.x - anom.x), 0.48f)
              spawnDropletReactionBurst(
                originX = dr.x,
                originY = dr.y,
                targetOrWakeX = anom.x,
                targetOrWakeY = anom.y,
                pullInward = true,
                crystalCount = 3,
                filamentCount = 2,
                outerColor = Color.rgb(239, 68, 68),
                midColor = Color.rgb(217, 70, 239),
                coreColor = Color.WHITE,
              )
              swarmDrones.removeAt(drIdx)
              spawnExplosion(dr.x, dr.y, Color.parseColor("#EF4444"))
            }
          }
        }

        AnomalyType.REPULSOR_RIFT -> {
          val fieldRadius = RiftCalibrationState.repulsorFieldRadius
          val baseRepulse = max(RiftCalibrationState.repulsorBaseForce, 1480f)
          val slingshotCoreRadius = anom.physicalRadius * 0.58f
          val customSpd = currentCustomGame?.speedMult ?: 1.0f
          for (s in ships) {
            if (s.respawnTimer > 0f) continue
            val dx = s.x - anom.x
            val dy = s.y - anom.y
            val dist = hypot(dx, dy).coerceAtLeast(0.01f)
            if (dist < fieldRadius) {
              val nx = dx / dist
              val ny = dy / dist
              val approachSpeed = -(s.vx * nx + s.vy * ny)
              if (dist < slingshotCoreRadius) {
                // Supersonic Inner-Core Slingshot: instant radial catapult exceeding max afterburner velocity!
                val catapultSpeed = s.spec.afterburnerSpeed * 1.45f * customSpd
                s.vx = nx * catapultSpeed
                s.vy = ny * catapultSpeed
                s.rocketBoostTimer = max(s.rocketBoostTimer, 0.45f)
                anom.recordBoundaryIngress(atan2(dy, dx), 0.60f)
                spawnDropletReactionBurst(
                  originX = anom.x + nx * (anom.physicalRadius * 0.72f),
                  originY = anom.y + ny * (anom.physicalRadius * 0.72f),
                  targetOrWakeX = s.x + s.vx * 0.14f,
                  targetOrWakeY = s.y + s.vy * 0.14f,
                  pullInward = false,
                  crystalCount = 5,
                  filamentCount = 4,
                  outerColor = Color.rgb(56, 189, 248),
                  midColor = Color.rgb(0, 245, 212),
                  coreColor = Color.rgb(250, 204, 21),
                  baseCrystalRadius = 6.2f,
                  durationSec = 0.62f,
                )
                shockwaves.add(ShockwaveEntity(anom.x, anom.y, 16f, anom.physicalRadius * 1.15f, Color.parseColor("#38BDF8")))
                if (s.isLocalPlayer) {
                  sfx.playBounce()
                  spawnPooledFct("REPULSOR SLINGSHOT!", s.x, s.y - 28f, Color.parseColor("#38BDF8"))
                }
              } else {
                val slingshotBonus = if (approachSpeed > 0f) approachSpeed * 3.1f else 0f
                val forceMag = (baseRepulse + slingshotBonus) * (1f - dist / fieldRadius).coerceIn(0.28f, 1f)
                s.vx += nx * forceMag * dt
                s.vy += ny * forceMag * dt
              }

              val maxAllowed = s.spec.afterburnerSpeed * 1.48f * customSpd
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

          // Slingshot Holo Phantoms & Swarm Drones outward
          for (d in decoys) {
            val dx = d.x - anom.x
            val dy = d.y - anom.y
            val dist = hypot(dx, dy).coerceAtLeast(0.01f)
            if (dist < fieldRadius) {
              val nx = dx / dist
              val ny = dy / dist
              val approachSpeed = -(d.vx * nx + d.vy * ny)
              val slingshotBonus = if (approachSpeed > 0f) approachSpeed * 2.8f else 0f
              val forceMag = (baseRepulse + slingshotBonus) * (1f - dist / fieldRadius).coerceIn(0.25f, 1f)
              d.vx += nx * forceMag * dt
              d.vy += ny * forceMag * dt
              val sp = hypot(d.vx, d.vy)
              if (sp > 960f) {
                d.vx = (d.vx / sp) * 960f
                d.vy = (d.vy / sp) * 960f
              }
              if (sp > 20f) {
                d.angle = atan2(d.vy, d.vx)
              }
            }
          }
          for (dr in swarmDrones) {
            val dx = dr.x - anom.x
            val dy = dr.y - anom.y
            val dist = hypot(dx, dy).coerceAtLeast(0.01f)
            if (dist < fieldRadius * 0.85f) {
              val nx = dx / dist
              val ny = dy / dist
              dr.vx += nx * 1350f * dt
              dr.vy += ny * 1350f * dt
              dr.angle = atan2(dr.vy, dr.vx)
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
          val baseRadius = RiftCalibrationState.riftPhysicalRadius.coerceAtLeast(120f)
          val growthRatio = (anom.physicalRadius / baseRadius).coerceIn(1f, 2.5f)
          val pullRadius = RiftCalibrationState.gravityPullRadius * growthRatio
          val pullStrength = RiftCalibrationState.gravityPullForce * (0.85f + 0.15f * growthRatio)
          val coreRadius = anom.physicalRadius * 0.44f

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
                anom.recordBoundaryIngress(atan2(s.y - anom.y, s.x - anom.x), 0.62f)
                spawnDropletReactionBurst(
                  originX = s.x,
                  originY = s.y,
                  targetOrWakeX = anom.x,
                  targetOrWakeY = anom.y,
                  pullInward = true,
                  crystalCount = 5,
                  filamentCount = 4,
                  outerColor = Color.rgb(168, 85, 247),
                  midColor = Color.rgb(0, 245, 212),
                  coreColor = Color.rgb(250, 204, 21),
                  baseCrystalRadius = 6.4f,
                )
                spawnExplosion(s.x, s.y, Color.parseColor("#C084FC"))
                if (s.isLocalPlayer) {
                  sfx.playBounce()
                  spawnPooledFct("GRAVITY RIFT: FULL STOP", s.x, s.y - 32f, Color.parseColor("#A855F7"))
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

          fun sweptDistToRift(x0: Float, y0: Float, x1: Float, y1: Float): Float {
            val sx = x1 - x0
            val sy = y1 - y0
            val segLenSq = sx * sx + sy * sy
            if (segLenSq <= 1e-4f) return hypot(anom.x - x1, anom.y - y1)
            val t = (((anom.x - x0) * sx + (anom.y - y0) * sy) / segLenSq).coerceIn(0f, 1f)
            val cx = x0 + sx * t
            val cy = y0 + sy * t
            return hypot(anom.x - cx, anom.y - cy)
          }

          // Pull and absorb Holo Phantoms & Swarm Drones across 0.92x visible Gravity Rift radius (+14px up to 440px)
          for (dIdx in decoys.indices.reversed()) {
            val d = decoys[dIdx]
            val prevX = d.x - d.vx * dt
            val prevY = d.y - d.vy * dt
            val sweptDist = sweptDistToRift(prevX, prevY, d.x, d.y)
            val dx = anom.x - d.x
            val dy = anom.y - d.y
            val dist = hypot(dx, dy).coerceAtLeast(0.01f)
            if (sweptDist <= anom.physicalRadius * 0.92f) {
              anom.recordBoundaryIngress(atan2(d.y - anom.y, d.x - anom.x), 0.56f)
              spawnDropletReactionBurst(
                originX = d.x,
                originY = d.y,
                targetOrWakeX = anom.x,
                targetOrWakeY = anom.y,
                pullInward = true,
                crystalCount = 4,
                filamentCount = 3,
                outerColor = Color.rgb(168, 85, 247),
                midColor = Color.rgb(0, 245, 212),
                coreColor = Color.rgb(250, 204, 21),
                baseCrystalRadius = 5.8f,
              )
              decoys.removeAt(dIdx)
              anom.absorbedWeaponCount++
              anom.physicalRadius = (anom.physicalRadius + 14f).coerceAtMost(440f)
              spawnExplosion(d.x, d.y, Color.parseColor("#C084FC"))
              shockwaves.add(
                ShockwaveEntity(
                  anom.x,
                  anom.y,
                  anom.physicalRadius * 0.36f,
                  anom.physicalRadius * 1.08f,
                  Color.parseColor("#A855F7"),
                )
              )
            } else if (dist < pullRadius) {
              val nx = dx / dist
              val ny = dy / dist
              val pullMag = pullStrength * 1.15f * (1f - dist / pullRadius).coerceIn(0.20f, 1f)
              d.vx += nx * pullMag * dt
              d.vy += ny * pullMag * dt
              if (hypot(d.vx, d.vy) > 20f) {
                d.angle = atan2(d.vy, d.vx)
              }
            }
          }

          for (drIdx in swarmDrones.indices.reversed()) {
            val dr = swarmDrones[drIdx]
            val prevX = dr.x - dr.vx * dt
            val prevY = dr.y - dr.vy * dt
            val sweptDist = sweptDistToRift(prevX, prevY, dr.x, dr.y)
            val dx = anom.x - dr.x
            val dy = anom.y - dr.y
            val dist = hypot(dx, dy).coerceAtLeast(0.01f)
            if (sweptDist <= anom.physicalRadius * 0.92f) {
              anom.recordBoundaryIngress(atan2(dr.y - anom.y, dr.x - anom.x), 0.56f)
              spawnDropletReactionBurst(
                originX = dr.x,
                originY = dr.y,
                targetOrWakeX = anom.x,
                targetOrWakeY = anom.y,
                pullInward = true,
                crystalCount = 4,
                filamentCount = 3,
                outerColor = Color.rgb(168, 85, 247),
                midColor = Color.rgb(0, 245, 212),
                coreColor = Color.rgb(250, 204, 21),
                baseCrystalRadius = 5.6f,
              )
              swarmDrones.removeAt(drIdx)
              anom.absorbedWeaponCount++
              anom.physicalRadius = (anom.physicalRadius + 14f).coerceAtMost(440f)
              spawnExplosion(dr.x, dr.y, Color.parseColor("#C084FC"))
              shockwaves.add(
                ShockwaveEntity(
                  anom.x,
                  anom.y,
                  anom.physicalRadius * 0.36f,
                  anom.physicalRadius * 1.08f,
                  Color.parseColor("#A855F7"),
                )
              )
            } else if (dist < pullRadius) {
              val nx = dx / dist
              val ny = dy / dist
              dr.vx += nx * pullStrength * dt
              dr.vy += ny * pullStrength * dt
              dr.angle = atan2(dr.vy, dr.vx)
            }
          }

          // Swept-segment bullet absorption across 0.92x visible physicalRadius (+18px per bullet up to 440px + violet accretion pulse)
          for (bIdx in bullets.indices.reversed()) {
            val b = bullets[bIdx]
            if (isStationDefenseOwner(b.ownerId)) continue
            val prevX = b.x - b.vx * dt
            val prevY = b.y - b.vy * dt
            val sweptDist = sweptDistToRift(prevX, prevY, b.x, b.y)
            val dx = anom.x - b.x
            val dy = anom.y - b.y
            val dist = hypot(dx, dy).coerceAtLeast(0.01f)
            if (sweptDist <= anom.physicalRadius * 0.92f) {
              anom.recordBoundaryIngress(atan2(b.y - anom.y, b.x - anom.x), 0.52f)
              spawnDropletReactionBurst(
                originX = b.x,
                originY = b.y,
                targetOrWakeX = anom.x,
                targetOrWakeY = anom.y,
                pullInward = true,
                crystalCount = 3,
                filamentCount = 2,
                outerColor = Color.rgb(168, 85, 247),
                midColor = Color.rgb(0, 245, 212),
                coreColor = Color.rgb(233, 213, 255),
                baseCrystalRadius = 5.2f,
              )
              bullets.removeAt(bIdx)
              anom.absorbedWeaponCount++
              anom.physicalRadius = (anom.physicalRadius + 18f).coerceAtMost(440f)
              spawnExplosion(b.x, b.y, Color.parseColor("#A855F7"))
              shockwaves.add(
                ShockwaveEntity(
                  anom.x,
                  anom.y,
                  anom.physicalRadius * 0.34f,
                  anom.physicalRadius * 1.06f,
                  Color.parseColor("#C084FC"),
                )
              )
            } else if (dist < pullRadius * 0.85f) {
              val nx = dx / dist
              val ny = dy / dist
              val sp = hypot(b.vx, b.vy).coerceAtLeast(320f)
              b.vx += nx * (pullStrength * 2.4f) * dt
              b.vy += ny * (pullStrength * 2.4f) * dt
              val newSp = hypot(b.vx, b.vy).coerceAtLeast(1f)
              b.vx = (b.vx / newSp) * sp
              b.vy = (b.vy / newSp) * sp
            }
          }

          // Swept-segment bomb/torpedo absorption across 0.95x visible physicalRadius (+40px per bomb up to 440px + heavy accretion shockwave)
          for (bmIdx in bombs.indices.reversed()) {
            val bm = bombs[bmIdx]
            val prevX = bm.x - bm.vx * dt
            val prevY = bm.y - bm.vy * dt
            val sweptDist = sweptDistToRift(prevX, prevY, bm.x, bm.y)
            val dx = anom.x - bm.x
            val dy = anom.y - bm.y
            val dist = hypot(dx, dy).coerceAtLeast(0.01f)
            if (sweptDist <= anom.physicalRadius * 0.95f) {
              anom.recordBoundaryIngress(atan2(bm.y - anom.y, bm.x - anom.x), 0.65f)
              spawnDropletReactionBurst(
                originX = bm.x,
                originY = bm.y,
                targetOrWakeX = anom.x,
                targetOrWakeY = anom.y,
                pullInward = true,
                crystalCount = 6,
                filamentCount = 4,
                outerColor = Color.rgb(168, 85, 247),
                midColor = Color.rgb(250, 204, 21),
                coreColor = Color.rgb(0, 245, 212),
                baseCrystalRadius = 7.0f,
                durationSec = 0.68f,
              )
              bombs.removeAt(bmIdx)
              anom.absorbedWeaponCount += 3
              anom.physicalRadius = (anom.physicalRadius + 40f).coerceAtMost(440f)
              spawnExplosion(bm.x, bm.y, Color.parseColor("#C084FC"))
              shockwaves.add(
                ShockwaveEntity(
                  anom.x,
                  anom.y,
                  anom.physicalRadius * 0.42f,
                  anom.physicalRadius * 1.18f,
                  Color.parseColor("#A855F7"),
                )
              )
            } else if (dist < pullRadius * 0.90f) {
              val nx = dx / dist
              val ny = dy / dist
              val sp = hypot(bm.vx, bm.vy).coerceAtLeast(200f)
              bm.vx += nx * (pullStrength * 1.9f) * dt
              bm.vy += ny * (pullStrength * 1.9f) * dt
              val newSp = hypot(bm.vx, bm.vy).coerceAtLeast(1f)
              bm.vx = (bm.vx / newSp) * sp
              bm.vy = (bm.vy / newSp) * sp
            }
          }

          // Swollen Gravity Rift (>= 260px): drags Asteroids, Orbital/Asteroid Stations, Debris & Stasis Barriers straight into the core (zero minStop!) with 950/s tidal crush implosion
          if (anom.physicalRadius >= 260f) {
            val obstaclePullRange = anom.physicalRadius * 2.35f
            var movedAnyObstacle = false
            var implodedAnyObstacle = false
            for (wIdx in walls.indices.reversed()) {
              val w = walls[wIdx]
              if (w.style == SpaceObstacleStyle.PERMANENT_BARRIER) continue
              var wCx = w.x + w.w * 0.5f
              var wCy = w.y + w.h * 0.5f
              val halfDiag = hypot(w.w * 0.5f, w.h * 0.5f)
              val dx = anom.x - wCx
              val dy = anom.y - wCy
              var dist = hypot(dx, dy)
              if (dist <= obstaclePullRange) {
                if (dist > 0.5f) {
                  val pullSpeed = 185f + 135f * (1f - (dist / obstaclePullRange).coerceIn(0f, 1f))
                  val step = min(dist, pullSpeed * dt)
                  val nextX = w.x + (dx / dist) * step
                  val nextY = w.y + (dy / dist) * step
                  if (!rectOverlapsProtectedZone(nextX, nextY, w.w, w.h)) {
                    w.x = nextX
                    w.y = nextY
                    wCx = w.x + w.w * 0.5f
                    wCy = w.y + w.h * 0.5f
                    dist = hypot(anom.x - wCx, anom.y - wCy)
                    movedAnyObstacle = true
                  }
                }

                // Tidal crush inside 0.95x physicalRadius (plus obstacle rim overlap)
                if (dist <= anom.physicalRadius * 0.95f + halfDiag * 0.35f) {
                  w.hp -= 950f * dt
                  w.lastHitAngle = atan2(anom.y - wCy, anom.x - wCx)
                  w.hitFlashTimer = max(w.hitFlashTimer, 0.12f)
                  anom.recordBoundaryIngress(atan2(wCy - anom.y, wCx - anom.x), 0.35f)
                  if (Random.nextFloat() < 0.35f) {
                    spawnDropletReactionBurst(
                      originX = wCx,
                      originY = wCy,
                      targetOrWakeX = anom.x,
                      targetOrWakeY = anom.y,
                      pullInward = true,
                      crystalCount = 2,
                      filamentCount = 2,
                      outerColor = Color.rgb(168, 85, 247),
                      midColor = Color.rgb(0, 245, 212),
                      coreColor = Color.rgb(250, 204, 21),
                      baseCrystalRadius = 5.4f,
                      durationSec = 0.42f,
                    )
                  }
                }

                if (w.hp <= 0f || dist <= anom.physicalRadius * 0.42f) {
                  anom.recordBoundaryIngress(atan2(wCy - anom.y, wCx - anom.x), 0.72f)
                  spawnDropletReactionBurst(
                    originX = wCx,
                    originY = wCy,
                    targetOrWakeX = anom.x,
                    targetOrWakeY = anom.y,
                    pullInward = true,
                    crystalCount = 8,
                    filamentCount = 5,
                    outerColor = Color.rgb(168, 85, 247),
                    midColor = Color.rgb(0, 245, 212),
                    coreColor = Color.rgb(250, 204, 21),
                    baseCrystalRadius = 8.2f,
                    durationSec = 0.78f,
                  )
                  val isStation =
                    w.style == SpaceObstacleStyle.ORBITAL_STATION ||
                      w.style == SpaceObstacleStyle.ASTEROID_STATION
                  pendingAsteroidRespawns.add(
                    PendingAsteroidRespawn(
                      x = w.x,
                      y = w.y,
                      w = w.w,
                      h = w.h,
                      variantSeed = w.variantSeed,
                      timerSec = 20.0f,
                      style = w.style,
                    )
                  )
                  walls.removeAt(wIdx)
                  val clearDrop = resolveClearCrateDropPosition(wCx, wCy)
                  greens.add(
                    GreenCrate(
                      x = clearDrop.first,
                      y = clearDrop.second,
                      spinPhase = Random.nextFloat() * (PI.toFloat() * 2f),
                      buffType = rollRandomCrateBuffType(),
                    )
                  )
                  if (isStation) {
                    val customSpdMult = currentCustomGame?.speedMult ?: 1.0f
                    val customDmgMult = currentCustomGame?.damageMult ?: 1.0f
                    val novaSpeed = 820f * customSpdMult
                    val novaDamage = 165f * customDmgMult
                    val exitR = halfDiag + 12f
                    val overloadTriggerId = w.lastAttackerShipId
                    for (k in 0 until 16) {
                      val a = (PI.toFloat() * 2f * k) / 16f
                      val sx = wCx + cos(a) * exitR
                      val sy = wCy + sin(a) * exitR
                      if (isInsideSafeZone(sx, sy)) continue
                      bullets.add(
                        BulletEntity(
                          ownerId = "station_defense",
                          ownerShipId = 0,
                          ownerCallsign = "STATION DEFENSE",
                          ownerSyndicateTag = "SECTOR",
                          ownerArmadaName = "",
                          ownerIsHuman = false,
                          freq = -999,
                          x = sx,
                          y = sy,
                          vx = cos(a) * novaSpeed,
                          vy = sin(a) * novaSpeed,
                          damage = novaDamage,
                          bouncesLeft = 1,
                          life = 1.85f,
                          color = Color.parseColor("#FACC15"),
                          isHeavyBurst = true,
                          ownerShipLabel = "Orbital Station",
                          weaponLabel = "Station Overload Nova",
                          triggeredByShipId = overloadTriggerId,
                        )
                      )
                    }
                    shockwaves.add(ShockwaveEntity(wCx, wCy, 28f, 290f, Color.parseColor("#FACC15")))
                  }
                  shockwaves.add(ShockwaveEntity(anom.x, anom.y, 22f, anom.physicalRadius * 1.22f, Color.parseColor("#A855F7")))
                  shockwaves.add(ShockwaveEntity(wCx, wCy, 14f, 185f, Color.parseColor("#00F5D4")))
                  spawnExplosion(wCx, wCy, Color.parseColor("#C084FC"))
                  if (hypot(wCx - localPlayer.x, wCy - localPlayer.y) < 1400f) {
                    sfx.playExplosion()
                    addTrauma(if (isStation) 0.36f else 0.28f)
                  }
                  spawnPooledFct(
                    text =
                      if (isStation) "STATION IMPLODED • OVERLOAD NOVA!"
                      else "RIFT IMPLOSION • SUPPLY DROP",
                    x = wCx,
                    y = wCy - 32f,
                    color = if (isStation) Color.parseColor("#FACC15") else Color.parseColor("#C084FC"),
                    maxLife = 1.35f,
                  )
                  implodedAnyObstacle = true
                }
              }
            }

            for (bwIdx in brickWalls.indices.reversed()) {
              val bw = brickWalls[bwIdx]
              var bCx = bw.x + bw.w * 0.5f
              var bCy = bw.y + bw.h * 0.5f
              val dx = anom.x - bCx
              val dy = anom.y - bCy
              var dist = hypot(dx, dy)
              if (dist <= obstaclePullRange) {
                if (dist > 0.5f) {
                  val step = min(dist, 215f * dt)
                  val nextX = bw.x + (dx / dist) * step
                  val nextY = bw.y + (dy / dist) * step
                  if (!rectOverlapsProtectedZone(nextX, nextY, bw.w, bw.h)) {
                    bw.x = nextX
                    bw.y = nextY
                    bCx = bw.x + bw.w * 0.5f
                    bCy = bw.y + bw.h * 0.5f
                    dist = hypot(anom.x - bCx, anom.y - bCy)
                    movedAnyObstacle = true
                  }
                }
                if (dist <= anom.physicalRadius * 0.95f) {
                  bw.life -= 8.5f * dt
                }
                if (bw.life <= 0f || dist <= anom.physicalRadius * 0.42f) {
                  brickWalls.removeAt(bwIdx)
                  shockwaves.add(ShockwaveEntity(bCx, bCy, 14f, 165f, Color.parseColor("#A855F7")))
                  spawnExplosion(bCx, bCy, Color.parseColor("#C084FC"))
                }
              }
            }

            if (implodedAnyObstacle || (movedAnyObstacle && gravityRadarBakeTimer <= 0f)) {
              gravityRadarBakeTimer = 0.45f
              bakeStaticRadarBitmap()
            }
          }

          // Supermassive Gravity Rift gravitational attraction toward nearby non-partnered Rifts (enables live convergence)
          if (anom.physicalRadius >= 360f) {
            val nearbyOther =
              anomalies.firstOrNull {
                it.id != anom.id &&
                  !it.isUnstableMergedRift &&
                  hypot(it.x - anom.x, it.y - anom.y) <= 980f
              }
            if (nearbyOther != null) {
              val dx = nearbyOther.x - anom.x
              val dy = nearbyOther.y - anom.y
              val d = hypot(dx, dy).coerceAtLeast(1f)
              anom.x += (dx / d) * 22f * dt
              anom.y += (dy / d) * 22f * dt
            }
          }
        }

        AnomalyType.PHASE_RIFT_A, AnomalyType.PHASE_RIFT_B -> {
          val nodeRadius = anom.physicalRadius * 0.52f
          val phaseCd = RiftCalibrationState.phaseRiftCooldownSec
          val partner = anomalies.firstOrNull { it.id == anom.partnerId }
          if (partner != null) {
            for (s in ships) {
              if (s.respawnTimer > 0f) continue
              if (s.phaseRiftCooldownTimer <= 0f && hypot(s.x - anom.x, s.y - anom.y) < nodeRadius + s.radius * 0.5f) {
                val entryAng = atan2(s.y - anom.y, s.x - anom.x)
                val exitAng = atan2(s.vy, s.vx)
                anom.recordBoundaryIngress(entryAng, 0.60f)
                partner.recordBoundaryIngress(exitAng, 0.60f)
                s.x = partner.x
                s.y = partner.y
                s.phaseRiftCooldownTimer = phaseCd
                spawnDropletReactionBurst(
                  originX = partner.x + cos(exitAng) * (partner.physicalRadius * 0.55f),
                  originY = partner.y + sin(exitAng) * (partner.physicalRadius * 0.55f),
                  targetOrWakeX = s.x + s.vx * 0.18f,
                  targetOrWakeY = s.y + s.vy * 0.18f,
                  pullInward = false,
                  crystalCount = 5,
                  filamentCount = 4,
                  outerColor = Color.rgb(45, 212, 191),
                  midColor = Color.rgb(56, 189, 248),
                  coreColor = Color.rgb(250, 204, 21),
                  baseCrystalRadius = 6.0f,
                )
                spawnExplosion(anom.x, anom.y, Color.parseColor("#2DD4BF"))
                spawnExplosion(partner.x, partner.y, Color.parseColor("#2DD4BF"))
                if (s.isLocalPlayer) {
                  sfx.playWarp()
                  spawnPooledFct("PHASE RIFT TRANSIT", s.x, s.y - 36f, Color.parseColor("#2DD4BF"))
                }
              }
            }

            for (d in decoys) {
              if (d.phaseRiftCooldownTimer <= 0f && hypot(d.x - anom.x, d.y - anom.y) < nodeRadius + 18f) {
                anom.recordBoundaryIngress(atan2(d.y - anom.y, d.x - anom.x), 0.50f)
                partner.recordBoundaryIngress(atan2(d.vy, d.vx), 0.50f)
                d.x = partner.x
                d.y = partner.y
                d.phaseRiftCooldownTimer = phaseCd
                spawnExplosion(anom.x, anom.y, Color.parseColor("#2DD4BF"))
                spawnExplosion(partner.x, partner.y, Color.parseColor("#2DD4BF"))
              }
            }

            for (dr in swarmDrones) {
              if (dr.phaseRiftCooldownTimer <= 0f && hypot(dr.x - anom.x, dr.y - anom.y) < nodeRadius + 14f) {
                anom.recordBoundaryIngress(atan2(dr.y - anom.y, dr.x - anom.x), 0.50f)
                partner.recordBoundaryIngress(atan2(dr.vy, dr.vx), 0.50f)
                dr.x = partner.x
                dr.y = partner.y
                dr.phaseRiftCooldownTimer = phaseCd
                spawnExplosion(anom.x, anom.y, Color.parseColor("#00F5D4"))
                spawnExplosion(partner.x, partner.y, Color.parseColor("#00F5D4"))
              }
            }

            for (b in bullets) {
              if (b.phaseRiftCooldownTimer <= 0f && hypot(b.x - anom.x, b.y - anom.y) < nodeRadius) {
                anom.recordBoundaryIngress(atan2(b.y - anom.y, b.x - anom.x), 0.42f)
                partner.recordBoundaryIngress(atan2(b.vy, b.vx), 0.42f)
                b.x = partner.x
                b.y = partner.y
                b.phaseRiftCooldownTimer = phaseCd
              }
            }

            for (bm in bombs) {
              if (bm.phaseRiftCooldownTimer <= 0f && hypot(bm.x - anom.x, bm.y - anom.y) < nodeRadius) {
                anom.recordBoundaryIngress(atan2(bm.y - anom.y, bm.x - anom.x), 0.52f)
                partner.recordBoundaryIngress(atan2(bm.vy, bm.vx), 0.52f)
                bm.x = partner.x
                bm.y = partner.y
                bm.phaseRiftCooldownTimer = phaseCd
              }
            }
          }
        }
      }
    }

    // Cleanly respawn expired Rifts on 45s cycles & replenish sector Rifts after Unstable Rift Collapse
    for (singleType in expiredSinglesToRespawn) {
      spawnSingleAnomaly(singleType)
    }
    if (expiredPairsToRespawn.isNotEmpty()) {
      anomalies.removeAll { !it.isUnstableMergedRift && (it.type == AnomalyType.PHASE_RIFT_A || it.type == AnomalyType.PHASE_RIFT_B) }
      spawnPhaseRiftPair()
    }
    if (unstableDetonatedThisStep) {
      for (reqType in listOf(AnomalyType.PLASMA_RIFT, AnomalyType.REPULSOR_RIFT, AnomalyType.GRAVITY_RIFT)) {
        if (anomalies.none { !it.isUnstableMergedRift && it.type == reqType }) {
          spawnSingleAnomaly(reqType)
        }
      }
      val activePhaseNodes = anomalies.count { !it.isUnstableMergedRift && (it.type == AnomalyType.PHASE_RIFT_A || it.type == AnomalyType.PHASE_RIFT_B) && it.partnerId.isNotEmpty() }
      if (activePhaseNodes < 2) {
        anomalies.removeAll { !it.isUnstableMergedRift && (it.type == AnomalyType.PHASE_RIFT_A || it.type == AnomalyType.PHASE_RIFT_B) }
        spawnPhaseRiftPair()
      }
    }
  }
}
