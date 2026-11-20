package com.example.engine

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt
import org.json.JSONArray
import org.json.JSONObject

enum class LabelPlacement(val label: String) {
  TOP("TOP"),
  BOTTOM("BOTTOM"),
  LEFT("LEFT"),
  RIGHT("RIGHT"),
  CONSOLIDATED_TOP("CONSOLIDATED TOP"),
  CONSOLIDATED_BOTTOM("CONSOLIDATED BOTTOM"),
}

data class CalibrationParamDescriptor(
  val key: String,
  val tabIndex: Int, // 0..7 (Tab 8 = AI Buff & Mechanic Forge)
  val shipIdFilter: Int = 0, // 0 = global, 1..9 = per-ship
  val title: String,
  val unit: String,
  val description: String,
  val min: Float,
  val max: Float,
  val step: Float,
  val decimals: Int = 2,
  val getter: () -> Float,
  val setter: (Float) -> Unit,
) {
  fun formatValue(v: Float = getter()): String {
    return when {
      unit == "%" && max <= 2.5f -> String.format(Locale.US, "%d%%", (v * 100f).roundToInt())
      decimals <= 0 -> String.format(Locale.US, "%d%s", v.roundToInt(), if (unit.isNotEmpty()) " $unit" else "")
      decimals == 1 -> String.format(Locale.US, "%.1f%s", v, if (unit.isNotEmpty()) " $unit" else "")
      else -> String.format(Locale.US, "%.2f%s", v, if (unit.isNotEmpty()) " $unit" else "")
    }
  }
}

/**
 * Centralized 100% live-wired Calibration & Game Balance Configuration for RIFT.
 * Every physics loop, weapon trigger, Special Buff, Playfield Rift, AI bot controller,
 * and Canvas/HUD renderer reads directly from this singleton in real time.
 */
object RiftCalibrationState {
  private const val PREFS_NAME = "rift_ui_layout_prefs"
  private const val KEY_HIDDEN_SET = "hidden_calibration_keys_csv"
  private const val KEY_FORGED_BUFFS_JSON = "custom_forged_buffs_json"
  private const val KEY_SAVED_CONFIG_JSON = "saved_calibration_values_json"
  private const val KEY_FORGED_VISUAL_RECIPES_JSON = "forged_visual_recipes_json"
  private const val KEY_FORGED_AUDIO_JSON = "forged_audio_recipes_json"
  private const val KEY_FORGED_SYNDICATE_CREST_JSON = "forged_syndicate_crest_json"

  // UI Modal & Live Peek States
  var isCalibrationModalOpen by mutableStateOf(false)
  var isDraggingAnySlider by mutableStateOf(false)
  var activeDraggingParamKey by mutableStateOf<String?>(null)
  var showHiddenParameters by mutableStateOf(false)
  var hiddenCalibrationKeys by mutableStateOf<Set<String>>(emptySet())
  var calibrationRevision by mutableIntStateOf(0)

  // 1. Camera, HUD & 1:1 Physical Screen-Pixel Text Sizes (Tab 0)
  var cameraZoomScale by mutableFloatStateOf(1.0f)     // Multiplier on base 0.65f camera zoom (0.35x .. 2.2x)
  var nametagFontSizePx by mutableFloatStateOf(15.0f)  // True 1:1 physical screen pixels for ship [TAG] Callsign
  var bountyFontSizePx by mutableFloatStateOf(16.0f)   // True 1:1 physical screen pixels for ship Bounty readout
  var fctFontSizePx by mutableFloatStateOf(14.0f)      // True 1:1 physical screen pixels for Floating Combat Text
  var shipScale by mutableFloatStateOf(1.0f)           // In-world ship sprite scale (0.5x .. 2.0x)
  var anomalyScale by mutableFloatStateOf(1.0f)        // Spatial anomaly visual scale (0.3x .. 2.0x)
  var crateScale by mutableFloatStateOf(1.0f)          // Supply Crate visual scale (0.5x .. 2.0x)
  var safeZoneScale by mutableFloatStateOf(1.0f)       // Safe Zone pad & boundary bracket scale (0.5x .. 2.0x)
  var aimTelegraphScale by mutableFloatStateOf(1.0f)   // Turret reticle & aim telegraph cone length (0.5x .. 2.0x)
  var projectileScale by mutableFloatStateOf(1.0f)     // Bullet beam width & bomb/mine radius (0.5x .. 2.0x)
  var fctScale by mutableFloatStateOf(1.0f)            // Floating combat text rise & scale multiplier (0.5x .. 2.0x)

  // Ship Label Clock Positioning
  var bountyPlacement by mutableStateOf(LabelPlacement.TOP)
  var tagPlacement by mutableStateOf(LabelPlacement.BOTTOM)
  var namePlacement by mutableStateOf(LabelPlacement.BOTTOM)
  var labelTextScale by mutableFloatStateOf(1.0f)
  var labelDistanceOffset by mutableFloatStateOf(1.0f)

  // HUD Element Scales
  var topBarScale by mutableFloatStateOf(1.0f)
  var buffBarScale by mutableFloatStateOf(1.0f)
  var killLogScale by mutableFloatStateOf(1.0f)
  var bottomConsoleScale by mutableFloatStateOf(1.0f)
  var combatArcScale by mutableFloatStateOf(1.0f)
  var leftStickScale by mutableFloatStateOf(1.0f)
  var idleWarningBannerScale by mutableFloatStateOf(1.0f)

  // Testing Feature Flags & Hardware Gamepad State
  var forceAllButtonsVisible by mutableStateOf(false)
  var isUiUnlockedForDragging by mutableStateOf(false)
  var unlockFullNinthShipOverride by mutableStateOf(false)
  var gamepadConnected by mutableStateOf(false)
  var gamepadDeadzoneLeft by mutableFloatStateOf(0.12f)
  var gamepadDeadzoneRight by mutableFloatStateOf(0.18f)
  var gamepadIdleTouchOpacity by mutableFloatStateOf(0.28f)

  // Virtual Keycodes & Ergonomic Default Controller Bindings (Live Hardware Remapper)
  const val BIND_LT = 1001 // Left Trigger (AXIS_LTRIGGER / AXIS_BRAKE / KEYCODE_BUTTON_L2)
  const val BIND_RT = 1002 // Right Trigger (AXIS_RTRIGGER / AXIS_GAS / AXIS_THROTTLE / KEYCODE_BUTTON_R2)

  var bindPrimaryFire by mutableIntStateOf(BIND_RT)
  var bindSpecialBuff by mutableIntStateOf(android.view.KeyEvent.KEYCODE_BUTTON_R1)
  var bindRetroBrake by mutableIntStateOf(BIND_LT)
  var bindSecondaryAbility by mutableIntStateOf(android.view.KeyEvent.KEYCODE_BUTTON_X)
  var bindRepel by mutableIntStateOf(android.view.KeyEvent.KEYCODE_BUTTON_A)
  var bindMultiToggle by mutableIntStateOf(android.view.KeyEvent.KEYCODE_BUTTON_Y)

  fun getBindingLabel(code: Int): String {
    return when (code) {
      BIND_LT, android.view.KeyEvent.KEYCODE_BUTTON_L2 -> "LT / L2"
      BIND_RT, android.view.KeyEvent.KEYCODE_BUTTON_R2 -> "RT / R2"
      android.view.KeyEvent.KEYCODE_BUTTON_R1 -> "R1 / RB"
      android.view.KeyEvent.KEYCODE_BUTTON_L1 -> "L1 / LB"
      android.view.KeyEvent.KEYCODE_BUTTON_A -> "A BTN"
      android.view.KeyEvent.KEYCODE_BUTTON_B -> "B BTN"
      android.view.KeyEvent.KEYCODE_BUTTON_X -> "X BTN"
      android.view.KeyEvent.KEYCODE_BUTTON_Y -> "Y BTN"
      android.view.KeyEvent.KEYCODE_BUTTON_THUMBL -> "L3"
      android.view.KeyEvent.KEYCODE_BUTTON_THUMBR -> "R3"
      android.view.KeyEvent.KEYCODE_DPAD_UP -> "D-UP"
      android.view.KeyEvent.KEYCODE_DPAD_DOWN -> "D-DOWN"
      android.view.KeyEvent.KEYCODE_DPAD_LEFT -> "D-LEFT"
      android.view.KeyEvent.KEYCODE_DPAD_RIGHT -> "D-RIGHT"
      else -> "KEY $code"
    }
  }

  fun getActionForBinding(code: Int): String? {
    val norm =
      when (code) {
        android.view.KeyEvent.KEYCODE_BUTTON_L2 -> BIND_LT
        android.view.KeyEvent.KEYCODE_BUTTON_R2 -> BIND_RT
        else -> code
      }
    return when (norm) {
      bindPrimaryFire -> "MAIN ATK"
      bindSpecialBuff -> "BUFF ATK"
      bindRetroBrake -> "BRAKE"
      bindSecondaryAbility -> "SECONDARY"
      bindRepel -> "REPEL"
      bindMultiToggle -> "MULTI"
      else -> null
    }
  }

  fun getBindingForActionSlot(actionSlot: String): Int {
    return when (actionSlot) {
      "PRIMARY_FIRE" -> bindPrimaryFire
      "SPECIAL_BUFF" -> bindSpecialBuff
      "RETRO_BRAKE" -> bindRetroBrake
      "SECONDARY_ABILITY" -> bindSecondaryAbility
      "REPEL" -> bindRepel
      "MULTI_TOGGLE" -> bindMultiToggle
      else -> BIND_RT
    }
  }

  fun assignControllerBinding(actionSlot: String, newCode: Int, context: Context? = null) {
    val normalizedNew =
      when (newCode) {
        android.view.KeyEvent.KEYCODE_BUTTON_L2 -> BIND_LT
        android.view.KeyEvent.KEYCODE_BUTTON_R2 -> BIND_RT
        else -> newCode
      }
    val oldCode = getBindingForActionSlot(actionSlot)
    if (oldCode == normalizedNew) return

    // Swap if another slot already uses normalizedNew so bindings never duplicate
    when {
      actionSlot != "PRIMARY_FIRE" && bindPrimaryFire == normalizedNew -> bindPrimaryFire = oldCode
      actionSlot != "SPECIAL_BUFF" && bindSpecialBuff == normalizedNew -> bindSpecialBuff = oldCode
      actionSlot != "RETRO_BRAKE" && bindRetroBrake == normalizedNew -> bindRetroBrake = oldCode
      actionSlot != "SECONDARY_ABILITY" && bindSecondaryAbility == normalizedNew -> bindSecondaryAbility = oldCode
      actionSlot != "REPEL" && bindRepel == normalizedNew -> bindRepel = oldCode
      actionSlot != "MULTI_TOGGLE" && bindMultiToggle == normalizedNew -> bindMultiToggle = oldCode
    }

    when (actionSlot) {
      "PRIMARY_FIRE" -> bindPrimaryFire = normalizedNew
      "SPECIAL_BUFF" -> bindSpecialBuff = normalizedNew
      "RETRO_BRAKE" -> bindRetroBrake = normalizedNew
      "SECONDARY_ABILITY" -> bindSecondaryAbility = normalizedNew
      "REPEL" -> bindRepel = normalizedNew
      "MULTI_TOGGLE" -> bindMultiToggle = normalizedNew
    }

    calibrationRevision++
    if (context != null) {
      savePersistedLayout(context)
    }
  }

  fun resetControllerBindingsToDefault(context: Context? = null) {
    bindPrimaryFire = BIND_RT
    bindSpecialBuff = android.view.KeyEvent.KEYCODE_BUTTON_R1
    bindRetroBrake = BIND_LT
    bindSecondaryAbility = android.view.KeyEvent.KEYCODE_BUTTON_X
    bindRepel = android.view.KeyEvent.KEYCODE_BUTTON_A
    bindMultiToggle = android.view.KeyEvent.KEYCODE_BUTTON_Y
    calibrationRevision++
    if (context != null) {
      savePersistedLayout(context)
    }
  }

  // Draggable Offsets (in pixels) for every independent on-screen HUD element
  var topBarOffset by mutableStateOf(Offset.Zero)
  var buffBarOffset by mutableStateOf(Offset.Zero)
  var killLogOffset by mutableStateOf(Offset.Zero)
  var bottomConsoleOffset by mutableStateOf(Offset.Zero)
  var combatArcOffset by mutableStateOf(Offset.Zero)
  var leftStickOffset by mutableStateOf(Offset.Zero)
  var fireStickOffset by mutableStateOf(Offset.Zero)
  var bombBtnOffset by mutableStateOf(Offset.Zero)
  var repelBtnOffset by mutableStateOf(Offset.Zero)
  var burstBtnOffset by mutableStateOf(Offset.Zero)
  var brakeBtnOffset by mutableStateOf(Offset.Zero)
  var multiBtnOffset by mutableStateOf(Offset.Zero)
  var calibrationBtnOffset by mutableStateOf(Offset.Zero)
  var nexusBtnOffset by mutableStateOf(Offset.Zero)
  var toastContainerOffset by mutableStateOf(Offset.Zero)
  var spectatorBoxOffset by mutableStateOf(Offset.Zero)
  var idleWarningBannerOffset by mutableStateOf(Offset.Zero)

  // 2. Flight Physics & Safe Zone Rules (Tab 1)
  var wallElasticity by mutableFloatStateOf(0.65f)
  var afterburnerThrustMult by mutableFloatStateOf(1.45f)
  var afterburnerDrainPerSec by mutableFloatStateOf(210f)
  var retroBrakeDamping by mutableFloatStateOf(0.22f)
  var exitPhaseShieldSec by mutableFloatStateOf(3.0f)
  var respawnDelaySec by mutableFloatStateOf(3.0f)
  var safeZoneRechargeMult by mutableFloatStateOf(2.5f)
  var safeZoneIdleLimitSec by mutableFloatStateOf(15.0f)
  var safeZoneReentryLockoutTime by mutableFloatStateOf(15.0f)
  var safeZoneRepelForce by mutableFloatStateOf(720f)
  var playfieldIdleLimitSec by mutableFloatStateOf(15.0f)
  var playfieldIdleDrainPctPerSec by mutableFloatStateOf(0.10f)

  // 4. Shared Class & Secondary Weapon Mechanics (Tab 3)
  var cloakDrainPerSec by mutableFloatStateOf(36f)
  var empPulseRadius by mutableFloatStateOf(220f)
  var empFluxDrainPct by mutableFloatStateOf(0.25f)
  var empFreezeSec by mutableFloatStateOf(2.0f)
  var phaseDashImpulse by mutableFloatStateOf(1120f)
  var phaseOverchargeBonusPct by mutableFloatStateOf(0.35f)
  var proxFuseRadius by mutableFloatStateOf(65f)
  var maxMinesPerShip by mutableIntStateOf(6)
  var interdictorBubbleRadius by mutableFloatStateOf(275f)
  var interdictorBubbleDuration by mutableFloatStateOf(6.0f)
  var interdictorBubbleCooldown by mutableFloatStateOf(8.5f)

  // 5. Official RIFT Special Buffs & Bucket B Enhancers (Tab 4)
  var seekerFlightTime by mutableFloatStateOf(2.5f)      // Official 2.5s Seeker Torpedo flight time
  var seekerSpeed by mutableFloatStateOf(380f)           // 380 px/s homing velocity
  var seekerTurnRate by mutableFloatStateOf(4.8f)        // 4.8 rad/s homing turn rate
  var seekerDamage by mutableFloatStateOf(780f)          // 780 Flux warhead damage
  var shatterWaves by mutableIntStateOf(3)               // 3 rapid waves
  var shatterPelletsPerWave by mutableIntStateOf(9)      // 9 bouncing shards per wave
  var shatterPelletDamage by mutableFloatStateOf(55f)    // 55 Flux per shard
  var novaBoltCount by mutableIntStateOf(24)             // 24 radial ricochet bolts
  var novaBoltDamage by mutableFloatStateOf(280f)        // 280 Flux per bolt
  var novaBoltSpeed by mutableFloatStateOf(860f)         // 860 px/s bolt velocity
  var stasisBarrierDuration by mutableFloatStateOf(45.0f)// 45.0s tethered hard-light forcefield lifespan
  var stasisBarrierLength by mutableFloatStateOf(120f)   // 120px barrier span
  var hyperSurgeDuration by mutableFloatStateOf(3.0f)    // 3.0s thruster burn
  var hyperSurgeSpeedMult by mutableFloatStateOf(1.65f)  // 1.65x speed multiplier
  var holoPhantomDuration by mutableFloatStateOf(8.0f)   // 8.0s decoy clone lifespan
  var repelCost by mutableFloatStateOf(320f)             // 320 Flux Repel cost
  var repelRadius by mutableFloatStateOf(520f)           // 520px shockwave radius
  var repelPushForce by mutableFloatStateOf(960f)        // 960 px/s outward impulse
  var crateMultiShotAmmo by mutableIntStateOf(25)        // +25 shots per Multi-Shot crate
  var crateBounceBulletAmmo by mutableIntStateOf(30)     // +30 shots per Bouncing Bullets crate
  var overchargeSurgePctPerStack by mutableFloatStateOf(0.08f) // +8% speed/thrust/recharge per stack

  // 6. Playfield Rifts & Supply Crates (Tab 5)
  var riftLifespanSec by mutableFloatStateOf(45.0f)
  var riftPhysicalRadius by mutableFloatStateOf(180f)
  var plasmaRiftDamagePct by mutableFloatStateOf(0.10f)
  var repulsorFieldRadius by mutableFloatStateOf(520f)
  var repulsorBaseForce by mutableFloatStateOf(320f)
  var gravityPullRadius by mutableFloatStateOf(600f)
  var gravityPullForce by mutableFloatStateOf(420f)
  var gravityStopImmunitySec by mutableFloatStateOf(1.75f)
  var phaseRiftCooldownSec by mutableFloatStateOf(1.0f)
  var buffCapFfa by mutableStateOf(40)
  var buffModeOverride by mutableStateOf("FFA")
  var buffTargetCountOverride by mutableStateOf<Int?>(null)
  var buffRespawnFrequency by mutableFloatStateOf(1.0f)
  var buffBaseRespawnDelay by mutableFloatStateOf(2.0f)

  // 7. AI Bot Behavior (Tab 6)
  var botsEnabled by mutableStateOf(true)
  var botRetreatFluxRatio by mutableFloatStateOf(0.40f)
  var botSafeZoneBanMargin by mutableFloatStateOf(400f)
  var botEngageRange by mutableFloatStateOf(1200f)
  var botGunFireRange by mutableFloatStateOf(850f)
  var botSpecialBuffChance by mutableFloatStateOf(0.08f)
  var botLookAheadDist by mutableFloatStateOf(180f)

  // 8. Auto-Discovered Engine Variables (Tab 7)
  var publicMatchCycleLimitSec by mutableFloatStateOf(3600f)
  var hvtBountyThreshold by mutableIntStateOf(250)
  var bountyKillBonusPct by mutableFloatStateOf(0.45f)
  var cratePickupBounty by mutableIntStateOf(15)
  var fullChargeCrateBounty by mutableIntStateOf(15)
  var hitStopDurationSec by mutableFloatStateOf(0.032f)
  var skirmishKillTargetPerRound by mutableIntStateOf(15)
  var fluxCoreBountyPerSec by mutableIntStateOf(15)
  var fluxCoreEjectSpeed by mutableFloatStateOf(1650f)
  var stasisBrigReactorMaxHp by mutableFloatStateOf(8000f)

  /**
   * Converts any camelCase or snake_case variable name into a clean Plain-English Title.
   */
  fun camelCaseToPlainEnglish(rawKey: String): String {
    return rawKey
      .replace("_", " ")
      .replace(Regex("([a-z])([A-Z0-9])"), "$1 $2")
      .split(" ")
      .filter { it.isNotBlank() }
      .joinToString(" ") { word ->
        when (word.uppercase(Locale.US)) {
          "FFA" -> "FFA"
          "HVT" -> "High Resonance"
          "BOUNTY" -> "Resonance"
          "FCT" -> "Floating Text"
          "EMP" -> "EMP"
          "HUD" -> "HUD"
          "AI" -> "AI"
          "PCT" -> "Percent"
          "SEC" -> "Duration"
          "PX" -> "Size"
          "MULT" -> "Multiplier"
          else -> word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString() }
        }
      }
  }

  /**
   * Builds the complete live descriptor catalog across Tabs 0..7 (including Ships 1..9 and Custom Forged Buffs).
   */
  fun buildAllParameterDescriptors(): List<CalibrationParamDescriptor> {
    val list = mutableListOf<CalibrationParamDescriptor>()

    // --- TAB 0: HUD & ARENA VISUAL SCALES ---
    list += CalibrationParamDescriptor(
      key = "labelDistanceOffset",
      tabIndex = 0,
      title = "Ship Label Spacing",
      unit = "x",
      description = "Gap between a ship and its floating name and Resonance.",
      min = 0.60f, max = 2.20f, step = 0.05f, decimals = 2,
      getter = { labelDistanceOffset }, setter = { labelDistanceOffset = it.coerceIn(0.60f, 2.20f) },
    )
    list += CalibrationParamDescriptor(
      key = "shipScale",
      tabIndex = 0,
      title = "Ship Size",
      unit = "x",
      description = "Scales all ships and their Flux rings.",
      min = 0.50f, max = 2.00f, step = 0.05f, decimals = 2,
      getter = { shipScale }, setter = { shipScale = it.coerceIn(0.50f, 2.00f) },
    )
    list += CalibrationParamDescriptor(
      key = "projectileScale",
      tabIndex = 0,
      title = "Bullet & Bomb Size",
      unit = "x",
      description = "Scales bullets, bombs, mines, and torpedoes.",
      min = 0.50f, max = 2.00f, step = 0.05f, decimals = 2,
      getter = { projectileScale }, setter = { projectileScale = it.coerceIn(0.50f, 2.00f) },
    )
    list += CalibrationParamDescriptor(
      key = "crateScale",
      tabIndex = 0,
      title = "Crate Size",
      unit = "x",
      description = "Scales glowing supply crates in space.",
      min = 0.50f, max = 2.00f, step = 0.05f, decimals = 2,
      getter = { crateScale }, setter = { crateScale = it.coerceIn(0.50f, 2.00f) },
    )
    list += CalibrationParamDescriptor(
      key = "anomalyScale",
      tabIndex = 0,
      title = "Rift Vortex Size",
      unit = "x",
      description = "Scales the visual size of Playfield Rifts.",
      min = 0.40f, max = 2.00f, step = 0.05f, decimals = 2,
      getter = { anomalyScale }, setter = { anomalyScale = it.coerceIn(0.40f, 2.00f) },
    )
    list += CalibrationParamDescriptor(
      key = "aimTelegraphScale",
      tabIndex = 0,
      title = "Aim Line Size",
      unit = "x",
      description = "Scales aim lines and bounce previews.",
      min = 0.50f, max = 2.00f, step = 0.05f, decimals = 2,
      getter = { aimTelegraphScale }, setter = { aimTelegraphScale = it.coerceIn(0.50f, 2.00f) },
    )
    list += CalibrationParamDescriptor(
      key = "topBarScale",
      tabIndex = 0,
      title = "Top Pill Bar Size",
      unit = "x",
      description = "Resizes the top status pill.",
      min = 0.60f, max = 1.60f, step = 0.05f, decimals = 2,
      getter = { topBarScale }, setter = { topBarScale = it.coerceIn(0.60f, 1.60f) },
    )
    list += CalibrationParamDescriptor(
      key = "buffBarScale",
      tabIndex = 0,
      title = "Buff Bar Size",
      unit = "x",
      description = "Resizes the ship upgrade buff strip.",
      min = 0.60f, max = 1.80f, step = 0.05f, decimals = 2,
      getter = { buffBarScale }, setter = { buffBarScale = it.coerceIn(0.60f, 1.80f) },
    )
    list += CalibrationParamDescriptor(
      key = "killLogScale",
      tabIndex = 0,
      title = "Kill Feed Size",
      unit = "x",
      description = "Resizes the top-right kill feed.",
      min = 0.60f, max = 1.80f, step = 0.05f, decimals = 2,
      getter = { killLogScale }, setter = { killLogScale = it.coerceIn(0.60f, 1.80f) },
    )
    list += CalibrationParamDescriptor(
      key = "bottomConsoleScale",
      tabIndex = 0,
      title = "Bottom Radar Size",
      unit = "x",
      description = "Resizes the bottom radar and stat wings.",
      min = 0.60f, max = 1.60f, step = 0.05f, decimals = 2,
      getter = { bottomConsoleScale }, setter = { bottomConsoleScale = it.coerceIn(0.60f, 1.60f) },
    )
    list += CalibrationParamDescriptor(
      key = "combatArcScale",
      tabIndex = 0,
      title = "Right Buttons Size",
      unit = "x",
      description = "Resizes the right-hand fire stick and ability buttons.",
      min = 0.60f, max = 1.60f, step = 0.05f, decimals = 2,
      getter = { combatArcScale }, setter = { combatArcScale = it.coerceIn(0.60f, 1.60f) },
    )
    list += CalibrationParamDescriptor(
      key = "leftStickScale",
      tabIndex = 0,
      title = "Left Stick Size",
      unit = "x",
      description = "Resizes the left movement stick.",
      min = 0.60f, max = 1.60f, step = 0.05f, decimals = 2,
      getter = { leftStickScale }, setter = { leftStickScale = it.coerceIn(0.60f, 1.60f) },
    )
    list += CalibrationParamDescriptor(
      key = "safeZoneScale",
      tabIndex = 0,
      title = "Safe Zone Pad & Wall Scale",
      unit = "x",
      description = "Scales the center Safe Zone pad and boundary forcefield brackets.",
      min = 0.50f, max = 2.00f, step = 0.05f, decimals = 2,
      getter = { safeZoneScale }, setter = { safeZoneScale = it.coerceIn(0.50f, 2.00f) },
    )
    list += CalibrationParamDescriptor(
      key = "idleWarningBannerScale",
      tabIndex = 0,
      title = "Tactical Alert Banner Size",
      unit = "x",
      description = "Resizes the Safe Zone lockout and AFK Flux drain warning pill.",
      min = 0.60f, max = 1.60f, step = 0.05f, decimals = 2,
      getter = { idleWarningBannerScale }, setter = { idleWarningBannerScale = it.coerceIn(0.60f, 1.60f) },
    )
    list += CalibrationParamDescriptor(
      key = "gamepadIdleTouchOpacity",
      tabIndex = 0,
      title = "Gamepad Touch Ring Opacity",
      unit = "%",
      description = "Opacity of touch rings while using a controller.",
      min = 0.10f, max = 0.85f, step = 0.05f, decimals = 2,
      getter = { gamepadIdleTouchOpacity }, setter = { gamepadIdleTouchOpacity = it.coerceIn(0.10f, 0.85f) },
    )

    // --- TAB 1: FLIGHT PHYSICS & SAFE ZONE RULES ---
    list += CalibrationParamDescriptor(
      key = "gamepadDeadzoneLeft",
      tabIndex = 1,
      title = "Left Stick Deadzone",
      unit = "%",
      description = "Controller left stick deadzone.",
      min = 0.05f, max = 0.35f, step = 0.01f, decimals = 2,
      getter = { gamepadDeadzoneLeft }, setter = { gamepadDeadzoneLeft = it.coerceIn(0.05f, 0.35f) },
    )
    list += CalibrationParamDescriptor(
      key = "gamepadDeadzoneRight",
      tabIndex = 1,
      title = "Right Stick Deadzone",
      unit = "%",
      description = "Controller right stick deadzone.",
      min = 0.05f, max = 0.35f, step = 0.01f, decimals = 2,
      getter = { gamepadDeadzoneRight }, setter = { gamepadDeadzoneRight = it.coerceIn(0.05f, 0.35f) },
    )
    list += CalibrationParamDescriptor(
      key = "wallElasticity",
      tabIndex = 1,
      title = "Wall Bounce",
      unit = "x",
      description = "Speed kept when bouncing off walls (0 Flux damage).",
      min = 0.20f, max = 1.00f, step = 0.05f, decimals = 2,
      getter = { wallElasticity }, setter = { wallElasticity = it.coerceIn(0.20f, 1.00f) },
    )
    list += CalibrationParamDescriptor(
      key = "afterburnerThrustMult",
      tabIndex = 1,
      title = "Afterburner Thrust",
      unit = "x",
      description = "Extra thrust when pushing the left stick past 80%.",
      min = 1.05f, max = 2.50f, step = 0.05f, decimals = 2,
      getter = { afterburnerThrustMult }, setter = { afterburnerThrustMult = it.coerceIn(1.05f, 2.50f) },
    )
    list += CalibrationParamDescriptor(
      key = "afterburnerDrainPerSec",
      tabIndex = 1,
      title = "Afterburner Drain",
      unit = "Flux/s",
      description = "Flux used per second while burning afterburners (stops at 1 Flux).",
      min = 0f, max = 600f, step = 15f, decimals = 0,
      getter = { afterburnerDrainPerSec }, setter = { afterburnerDrainPerSec = it.coerceIn(0f, 600f) },
    )
    list += CalibrationParamDescriptor(
      key = "retroBrakeDamping",
      tabIndex = 1,
      title = "Brake Strength",
      unit = "x",
      description = "Stopping power when holding [BRAKE].",
      min = 0.05f, max = 0.85f, step = 0.02f, decimals = 2,
      getter = { retroBrakeDamping }, setter = { retroBrakeDamping = it.coerceIn(0.05f, 0.85f) },
    )
    list += CalibrationParamDescriptor(
      key = "safeZoneReentryLockoutTime",
      tabIndex = 1,
      title = "Safe Zone Lockout",
      unit = "s",
      description = "Seconds you are blocked from re-entering the Safe Zone after leaving.",
      min = 0f, max = 45f, step = 1f, decimals = 1,
      getter = { safeZoneReentryLockoutTime }, setter = { safeZoneReentryLockoutTime = it.coerceIn(0f, 45f) },
    )
    list += CalibrationParamDescriptor(
      key = "safeZoneRepelForce",
      tabIndex = 1,
      title = "Safe Zone Pushback",
      unit = "px/s",
      description = "Knockback speed when hitting a locked Safe Zone wall.",
      min = 200f, max = 1600f, step = 40f, decimals = 0,
      getter = { safeZoneRepelForce }, setter = { safeZoneRepelForce = it.coerceIn(200f, 1600f) },
    )
    list += CalibrationParamDescriptor(
      key = "safeZoneIdleLimitSec",
      tabIndex = 1,
      title = "Safe Zone Time Limit",
      unit = "s",
      description = "Max seconds in the Safe Zone before auto-warp into space.",
      min = 5f, max = 60f, step = 1f, decimals = 0,
      getter = { safeZoneIdleLimitSec }, setter = { safeZoneIdleLimitSec = it.coerceIn(5f, 60f) },
    )
    list += CalibrationParamDescriptor(
      key = "safeZoneRechargeMult",
      tabIndex = 1,
      title = "Safe Zone Recharge Boost",
      unit = "x",
      description = "Flux recharge multiplier inside the Safe Zone.",
      min = 1.0f, max = 6.0f, step = 0.25f, decimals = 2,
      getter = { safeZoneRechargeMult }, setter = { safeZoneRechargeMult = it.coerceIn(1.0f, 6.0f) },
    )
    list += CalibrationParamDescriptor(
      key = "exitPhaseShieldSec",
      tabIndex = 1,
      title = "Spawn Shield Time",
      unit = "s",
      description = "Brief shield after spawning or leaving the Safe Zone.",
      min = 0.5f, max = 8.0f, step = 0.5f, decimals = 1,
      getter = { exitPhaseShieldSec }, setter = { exitPhaseShieldSec = it.coerceIn(0.5f, 8.0f) },
    )
    list += CalibrationParamDescriptor(
      key = "respawnDelaySec",
      tabIndex = 1,
      title = "Respawn Delay",
      unit = "s",
      description = "Seconds on the death screen before respawning.",
      min = 1.0f, max = 8.0f, step = 0.5f, decimals = 1,
      getter = { respawnDelaySec }, setter = { respawnDelaySec = it.coerceIn(1.0f, 8.0f) },
    )
    list += CalibrationParamDescriptor(
      key = "playfieldIdleLimitSec",
      tabIndex = 1,
      title = "AFK Grace Time",
      unit = "s",
      description = "Idle seconds in space before AFK Flux drain starts.",
      min = 5f, max = 60f, step = 1f, decimals = 0,
      getter = { playfieldIdleLimitSec }, setter = { playfieldIdleLimitSec = it.coerceIn(5f, 60f) },
    )
    list += CalibrationParamDescriptor(
      key = "playfieldIdleDrainPctPerSec",
      tabIndex = 1,
      title = "AFK Drain Rate",
      unit = "%",
      description = "Max Flux drained per second while AFK in space.",
      min = 0.02f, max = 0.50f, step = 0.02f, decimals = 2,
      getter = { playfieldIdleDrainPctPerSec }, setter = { playfieldIdleDrainPctPerSec = it.coerceIn(0.02f, 0.50f) },
    )

    // --- TAB 2: PER-SHIP HULL STATS ---
    for (shipId in 1..10) {
      val spec = RiftCodex.getShipById(shipId)
      val prefix = if (shipId == 10) "Rift Frame" else spec.name
      list += CalibrationParamDescriptor(
        key = "ship_${shipId}_maxFlux",
        tabIndex = 2,
        shipIdFilter = shipId,
        title = "$prefix Maximum Flux",
        unit = "Flux",
        description = "Unified Flux capacitor pool (HP + weapon ammo) for $prefix.",
        min = 1200f, max = 7500f, step = 100f, decimals = 0,
        getter = { RiftCodex.getShipById(shipId).maxFlux },
        setter = { RiftCodex.getShipById(shipId).maxFlux = it.coerceIn(1200f, 7500f) },
      )
      list += CalibrationParamDescriptor(
        key = "ship_${shipId}_rechargeRate",
        tabIndex = 2,
        shipIdFilter = shipId,
        title = "$prefix Flux Recharge Rate",
        unit = "Flux/s",
        description = "Passive Flux restored per second in open space for $prefix.",
        min = 100f, max = 900f, step = 10f, decimals = 0,
        getter = { RiftCodex.getShipById(shipId).rechargeRate },
        setter = { RiftCodex.getShipById(shipId).rechargeRate = it.coerceIn(100f, 900f) },
      )
      list += CalibrationParamDescriptor(
        key = "ship_${shipId}_topSpeed",
        tabIndex = 2,
        shipIdFilter = shipId,
        title = "$prefix Top Cruise Speed",
        unit = "px/s",
        description = "Maximum standard Newtonian flight velocity for $prefix (also scales afterburner cap).",
        min = 240f, max = 950f, step = 10f, decimals = 0,
        getter = { RiftCodex.getShipById(shipId).topSpeed },
        setter = {
          val s = RiftCodex.getShipById(shipId)
          val clamped = it.coerceIn(240f, 950f)
          s.topSpeed = clamped
          s.afterburnerSpeed = clamped * 1.32f
        },
      )
      list += CalibrationParamDescriptor(
        key = "ship_${shipId}_thrust",
        tabIndex = 2,
        shipIdFilter = shipId,
        title = "$prefix Engine Thrust",
        unit = "px/s²",
        description = "Main thruster acceleration responsiveness when changing drift vectors in $prefix.",
        min = 350f, max = 1600f, step = 25f, decimals = 0,
        getter = { RiftCodex.getShipById(shipId).thrust },
        setter = { RiftCodex.getShipById(shipId).thrust = it.coerceIn(350f, 1600f) },
      )
      list += CalibrationParamDescriptor(
        key = "ship_${shipId}_turnRate",
        tabIndex = 2,
        shipIdFilter = shipId,
        title = "$prefix Hull Turn Rate",
        unit = "rad/s",
        description = "Angular rotational speed of $prefix when re-orienting nose heading.",
        min = 2.0f, max = 9.0f, step = 0.1f, decimals = 1,
        getter = { RiftCodex.getShipById(shipId).turnRate },
        setter = { RiftCodex.getShipById(shipId).turnRate = it.coerceIn(2.0f, 9.0f) },
      )
    }

    // --- TAB 3: PRIMARY & SECONDARY WEAPONS ---
    list += CalibrationParamDescriptor(
      key = "cloakDrainPerSec",
      tabIndex = 3,
      shipIdFilter = 0,
      title = "Stealth Cloak Flux Drain",
      unit = "Flux/s",
      description = "Continuous Flux drain while Tactical Cloak is active on Disruptor or Rift Frame (default 36 Flux/s).",
      min = 10f, max = 250f, step = 4f, decimals = 0,
      getter = { cloakDrainPerSec }, setter = { cloakDrainPerSec = it.coerceIn(10f, 250f) },
    )
    list += CalibrationParamDescriptor(
      key = "empPulseRadius",
      tabIndex = 3,
      shipIdFilter = 0,
      title = "Disruptor 360° AoE EMP Radius",
      unit = "px",
      description = "Radial shockwave radius centered on Disruptor's hull when breaking cloak with EMP Pulse.",
      min = 100f, max = 420f, step = 10f, decimals = 0,
      getter = { empPulseRadius }, setter = { empPulseRadius = it.coerceIn(100f, 420f) },
    )
    list += CalibrationParamDescriptor(
      key = "empFluxDrainPct",
      tabIndex = 3,
      shipIdFilter = 0,
      title = "AoE EMP Max Flux Drain",
      unit = "%",
      description = "Percentage of enemy Maximum Flux drained by Disruptor's 360° AoE EMP pulse.",
      min = 0.10f, max = 0.80f, step = 0.05f, decimals = 2,
      getter = { empFluxDrainPct }, setter = { empFluxDrainPct = it.coerceIn(0.10f, 0.80f) },
    )
    list += CalibrationParamDescriptor(
      key = "empFreezeSec",
      tabIndex = 3,
      shipIdFilter = 0,
      title = "EMP Recharge Lockout Duration",
      unit = "s",
      description = "Seconds an EMP-struck target's passive Flux regeneration is frozen.",
      min = 0.5f, max = 6.0f, step = 0.25f, decimals = 2,
      getter = { empFreezeSec }, setter = { empFreezeSec = it.coerceIn(0.5f, 6.0f) },
    )
    list += CalibrationParamDescriptor(
      key = "phaseDashImpulse",
      tabIndex = 3,
      shipIdFilter = 0,
      title = "Phase Dash Burst Velocity",
      unit = "px/s",
      description = "Supersonic forward phase-shift burst velocity granted to Viper and Phase Dash hulls.",
      min = 700f, max = 1650f, step = 20f, decimals = 0,
      getter = { phaseDashImpulse }, setter = { phaseDashImpulse = it.coerceIn(700f, 1650f) },
    )
    list += CalibrationParamDescriptor(
      key = "phaseOverchargeBonusPct",
      tabIndex = 3,
      shipIdFilter = 0,
      title = "Phase Rail Overcharge Bonus",
      unit = "%",
      description = "Bonus damage added to your next Primary shot immediately after Phase Dashing.",
      min = 0.10f, max = 1.00f, step = 0.05f, decimals = 2,
      getter = { phaseOverchargeBonusPct }, setter = { phaseOverchargeBonusPct = it.coerceIn(0.10f, 1.00f) },
    )
    list += CalibrationParamDescriptor(
      key = "proxFuseRadius",
      tabIndex = 3,
      shipIdFilter = 0,
      title = "Proximity Flak Trigger Radius",
      unit = "px",
      description = "Detection radius at which Valkyrie Proximity Flak warheads detonate into shrapnel.",
      min = 30f, max = 160f, step = 5f, decimals = 0,
      getter = { proxFuseRadius }, setter = { proxFuseRadius = it.coerceIn(30f, 160f) },
    )
    list += CalibrationParamDescriptor(
      key = "maxMinesPerShip",
      tabIndex = 3,
      shipIdFilter = 0,
      title = "Max Active Proximity Mines",
      unit = "mines",
      description = "Maximum simultaneous armed mines a single ship can maintain before oldest despawns.",
      min = 2f, max = 16f, step = 1f, decimals = 0,
      getter = { maxMinesPerShip.toFloat() }, setter = { maxMinesPerShip = it.roundToInt().coerceIn(2, 16) },
    )
    list += CalibrationParamDescriptor(
      key = "interdictorBubbleRadius",
      tabIndex = 3,
      shipIdFilter = 0,
      title = "Eclipse Stasis: Radius",
      unit = "px",
      description = "Radius of Eclipse's two-way Stasis containment field (default 275px).",
      min = 180f, max = 480f, step = 5f, decimals = 0,
      getter = { interdictorBubbleRadius }, setter = { interdictorBubbleRadius = it.coerceIn(180f, 480f) },
    )
    list += CalibrationParamDescriptor(
      key = "interdictorBubbleDuration",
      tabIndex = 3,
      shipIdFilter = 0,
      title = "Eclipse Stasis: Duration",
      unit = "s",
      description = "Seconds an Eclipse Stasis containment field stays active (default 6.0s).",
      min = 2.0f, max = 15.0f, step = 0.5f, decimals = 1,
      getter = { interdictorBubbleDuration }, setter = { interdictorBubbleDuration = it.coerceIn(2.0f, 15.0f) },
    )
    list += CalibrationParamDescriptor(
      key = "interdictorBubbleCooldown",
      tabIndex = 3,
      shipIdFilter = 0,
      title = "Eclipse Stasis: Cooldown",
      unit = "s",
      description = "Cooldown between Eclipse Stasis deployments (default 8.5s).",
      min = 4.0f, max = 25.0f, step = 0.5f, decimals = 1,
      getter = { interdictorBubbleCooldown },
      setter = {
        val v = it.coerceIn(4.0f, 25.0f)
        interdictorBubbleCooldown = v
        RiftCodex.getShipById(9).bombCooldown = v
      },
    )

    for (shipId in 1..10) {
      val spec = RiftCodex.getShipById(shipId)
      val prefix = if (shipId == 10) "Rift Frame" else spec.name
      list += CalibrationParamDescriptor(
        key = "ship_${shipId}_primaryDamage",
        tabIndex = 3,
        shipIdFilter = shipId,
        title = "$prefix Primary Bolt Damage",
        unit = "Flux",
        description = "Base Flux damage per primary projectile stream for $prefix.",
        min = 30f, max = 800f, step = 5f, decimals = 0,
        getter = { RiftCodex.getShipById(shipId).primaryDamage },
        setter = { RiftCodex.getShipById(shipId).primaryDamage = it.coerceIn(30f, 800f) },
      )
      list += CalibrationParamDescriptor(
        key = "ship_${shipId}_primaryCost",
        tabIndex = 3,
        shipIdFilter = shipId,
        title = "$prefix Primary Flux Cost",
        unit = "Flux",
        description = "Flux drained per primary trigger pull on $prefix.",
        min = 0f, max = 120f, step = 2f, decimals = 0,
        getter = { RiftCodex.getShipById(shipId).primaryCost },
        setter = { RiftCodex.getShipById(shipId).primaryCost = it.coerceIn(0f, 120f) },
      )
      list += CalibrationParamDescriptor(
        key = "ship_${shipId}_primaryCooldown",
        tabIndex = 3,
        shipIdFilter = shipId,
        title = "$prefix Primary Fire Interval",
        unit = "s",
        description = "Seconds between primary shots on $prefix (lower = faster RPM).",
        min = 0.05f, max = 0.60f, step = 0.01f, decimals = 2,
        getter = { RiftCodex.getShipById(shipId).primaryCooldown },
        setter = { RiftCodex.getShipById(shipId).primaryCooldown = it.coerceIn(0.05f, 0.60f) },
      )
      list += CalibrationParamDescriptor(
        key = "ship_${shipId}_primarySpeed",
        tabIndex = 3,
        shipIdFilter = shipId,
        title = "$prefix Primary Muzzle Velocity",
        unit = "px/s",
        description = "Projectile flight velocity for $prefix primary guns.",
        min = 450f, max = 1800f, step = 20f, decimals = 0,
        getter = { RiftCodex.getShipById(shipId).primarySpeed },
        setter = { RiftCodex.getShipById(shipId).primarySpeed = it.coerceIn(450f, 1800f) },
      )
      if (spec.hasBomb) {
        if (spec.bombType != "CLOAK_EMP") {
          list += CalibrationParamDescriptor(
            key = "ship_${shipId}_bombCost",
            tabIndex = 3,
            shipIdFilter = shipId,
            title = "$prefix Secondary Flux Cost",
            unit = "Flux",
            description = "Flux drained when activating $prefix's secondary (${spec.secondaryShortLabel}).",
            min = 0f, max = 1200f, step = 25f, decimals = 0,
            getter = { RiftCodex.getShipById(shipId).bombCost },
            setter = { RiftCodex.getShipById(shipId).bombCost = it.coerceIn(0f, 1200f) },
          )
        }
        list += CalibrationParamDescriptor(
          key = "ship_${shipId}_bombCooldown",
          tabIndex = 3,
          shipIdFilter = shipId,
          title = "$prefix Secondary Cooldown",
          unit = "s",
          description = "Reload cooldown in seconds for $prefix's secondary (${spec.secondaryShortLabel}).",
          min = 0.20f, max = 35.0f, step = 0.10f, decimals = 2,
          getter = { RiftCodex.getShipById(shipId).bombCooldown },
          setter = { RiftCodex.getShipById(shipId).bombCooldown = it.coerceIn(0.20f, 35.0f) },
        )
        if (spec.bombDamage > 0f) {
          list += CalibrationParamDescriptor(
            key = "ship_${shipId}_bombDamage",
            tabIndex = 3,
            shipIdFilter = shipId,
            title = "$prefix Secondary Warhead Damage",
            unit = "Flux",
            description = "Direct Flux damage dealt by $prefix's secondary warhead.",
            min = 50f, max = 2500f, step = 25f, decimals = 0,
            getter = { RiftCodex.getShipById(shipId).bombDamage },
            setter = { RiftCodex.getShipById(shipId).bombDamage = it.coerceIn(50f, 2500f) },
          )
        }
        if (spec.bombRecoil > 0f || spec.bombType in setOf("BOUNCE", "SIEGE", "PROX")) {
          list += CalibrationParamDescriptor(
            key = "ship_${shipId}_bombRecoil",
            tabIndex = 3,
            shipIdFilter = shipId,
            title = "$prefix Secondary Firing Recoil",
            unit = "px/s",
            description = "Backward Newtonian recoil impulse applied to $prefix when launching warheads.",
            min = 0f, max = 650f, step = 15f, decimals = 0,
            getter = { RiftCodex.getShipById(shipId).bombRecoil },
            setter = { RiftCodex.getShipById(shipId).bombRecoil = it.coerceIn(0f, 650f) },
          )
        }
      }
    }

    // --- TAB 4: SPECIAL BUFFS & CUSTOM FORGED BUFFS ---
    list += CalibrationParamDescriptor(
      key = "seekerFlightTime",
      tabIndex = 4,
      title = "Seeker Torpedo Flight Time",
      unit = "s",
      description = "Active homing flight lifespan of Seeker Torpedoes before self-detonation (default 2.5s).",
      min = 0.8f, max = 8.0f, step = 0.1f, decimals = 1,
      getter = { seekerFlightTime },
      setter = {
        val v = it.coerceIn(0.8f, 8.0f)
        seekerFlightTime = v
        RiftCodex.BUFF_REGISTRY["SEEKER_TORPEDO"]?.flightDuration = v
      },
    )
    list += CalibrationParamDescriptor(
      key = "seekerSpeed",
      tabIndex = 4,
      title = "Seeker Torpedo Flight Speed",
      unit = "px/s",
      description = "Cruising velocity of homing Seeker Torpedoes across space.",
      min = 180f, max = 950f, step = 20f, decimals = 0,
      getter = { seekerSpeed },
      setter = {
        val v = it.coerceIn(180f, 950f)
        seekerSpeed = v
        RiftCodex.BUFF_REGISTRY["SEEKER_TORPEDO"]?.projectileSpeed = v
      },
    )
    list += CalibrationParamDescriptor(
      key = "seekerTurnRate",
      tabIndex = 4,
      title = "Seeker Torpedo Homing Turn Rate",
      unit = "rad/s",
      description = "Maximum steering agility of Seeker Torpedoes while tracking hostile targets.",
      min = 1.0f, max = 12.0f, step = 0.2f, decimals = 1,
      getter = { seekerTurnRate }, setter = { seekerTurnRate = it.coerceIn(1.0f, 12.0f) },
    )
    list += CalibrationParamDescriptor(
      key = "seekerDamage",
      tabIndex = 4,
      title = "Seeker Torpedo Impact Damage",
      unit = "Flux",
      description = "Flux damage inflicted when a Seeker Torpedo strikes a target.",
      min = 200f, max = 2000f, step = 25f, decimals = 0,
      getter = { seekerDamage },
      setter = {
        val v = it.coerceIn(200f, 2000f)
        seekerDamage = v
        RiftCodex.BUFF_REGISTRY["SEEKER_TORPEDO"]?.damage = v
      },
    )
    list += CalibrationParamDescriptor(
      key = "shatterWaves",
      tabIndex = 4,
      title = "Shatter Cone Wave Count",
      unit = "waves",
      description = "Number of rapid sequential plasma shard waves fired per Shatter Cone trigger.",
      min = 1f, max = 6f, step = 1f, decimals = 0,
      getter = { shatterWaves.toFloat() }, setter = { shatterWaves = it.roundToInt().coerceIn(1, 6) },
    )
    list += CalibrationParamDescriptor(
      key = "shatterPelletsPerWave",
      tabIndex = 4,
      title = "Shatter Cone Shards Per Wave",
      unit = "shards",
      description = "Number of bouncing plasma shards spread across the 60° cone per wave.",
      min = 3f, max = 18f, step = 1f, decimals = 0,
      getter = { shatterPelletsPerWave.toFloat() }, setter = { shatterPelletsPerWave = it.roundToInt().coerceIn(3, 18) },
    )
    list += CalibrationParamDescriptor(
      key = "shatterPelletDamage",
      tabIndex = 4,
      title = "Shatter Cone Shard Damage",
      unit = "Flux",
      description = "Flux damage dealt by each individual Shatter Cone plasma shard.",
      min = 15f, max = 220f, step = 5f, decimals = 0,
      getter = { shatterPelletDamage }, setter = { shatterPelletDamage = it.coerceIn(15f, 220f) },
    )
    list += CalibrationParamDescriptor(
      key = "novaBoltCount",
      tabIndex = 4,
      title = "Nova Ring Radial Bolt Count",
      unit = "bolts",
      description = "Total ricochet plasma bolts fired in a 360° circle by Nova Ring.",
      min = 8f, max = 48f, step = 2f, decimals = 0,
      getter = { novaBoltCount.toFloat() }, setter = { novaBoltCount = it.roundToInt().coerceIn(8, 48) },
    )
    list += CalibrationParamDescriptor(
      key = "novaBoltDamage",
      tabIndex = 4,
      title = "Nova Ring Bolt Damage",
      unit = "Flux",
      description = "Flux damage dealt per individual Nova Ring ricochet bolt.",
      min = 60f, max = 700f, step = 20f, decimals = 0,
      getter = { novaBoltDamage }, setter = { novaBoltDamage = it.coerceIn(60f, 700f) },
    )
    list += CalibrationParamDescriptor(
      key = "novaBoltSpeed",
      tabIndex = 4,
      title = "Nova Ring Bolt Velocity",
      unit = "px/s",
      description = "Outward speed of Nova Ring plasma bolts.",
      min = 400f, max = 1500f, step = 25f, decimals = 0,
      getter = { novaBoltSpeed }, setter = { novaBoltSpeed = it.coerceIn(400f, 1500f) },
    )
    list += CalibrationParamDescriptor(
      key = "stasisBarrierDuration",
      tabIndex = 4,
      title = "Stasis Barrier Duration",
      unit = "s",
      description = "Seconds a deployed Stasis Barrier hard-light forcefield persists in space (default 45.0s).",
      min = 5.0f, max = 120.0f, step = 1.0f, decimals = 1,
      getter = { stasisBarrierDuration },
      setter = {
        val v = it.coerceIn(5.0f, 120.0f)
        stasisBarrierDuration = v
        RiftCodex.BUFF_REGISTRY["STASIS_BARRIER"]?.flightDuration = v
      },
    )
    list += CalibrationParamDescriptor(
      key = "stasisBarrierLength",
      tabIndex = 4,
      title = "Stasis Barrier Wall Span",
      unit = "px",
      description = "Total physical width of the perpendicular Stasis Barrier wall.",
      min = 60f, max = 280f, step = 10f, decimals = 0,
      getter = { stasisBarrierLength }, setter = { stasisBarrierLength = it.coerceIn(60f, 280f) },
    )
    list += CalibrationParamDescriptor(
      key = "hyperSurgeDuration",
      tabIndex = 4,
      title = "Hyper Surge Boost Duration",
      unit = "s",
      description = "Duration of the 0-Flux Hyper Surge thruster speed burn.",
      min = 1.0f, max = 8.0f, step = 0.25f, decimals = 2,
      getter = { hyperSurgeDuration }, setter = { hyperSurgeDuration = it.coerceIn(1.0f, 8.0f) },
    )
    list += CalibrationParamDescriptor(
      key = "hyperSurgeSpeedMult",
      tabIndex = 4,
      title = "Hyper Surge Speed Multiplier",
      unit = "x",
      description = "Top speed and thrust multiplier granted while Hyper Surge is burning.",
      min = 1.15f, max = 2.50f, step = 0.05f, decimals = 2,
      getter = { hyperSurgeSpeedMult }, setter = { hyperSurgeSpeedMult = it.coerceIn(1.15f, 2.50f) },
    )
    list += CalibrationParamDescriptor(
      key = "holoPhantomDuration",
      tabIndex = 4,
      title = "Holo Phantom Clone Lifespan",
      unit = "s",
      description = "Seconds your holographic phantom clone mirrors flight and breaks Seeker locks.",
      min = 2.0f, max = 20.0f, step = 0.5f, decimals = 1,
      getter = { holoPhantomDuration }, setter = { holoPhantomDuration = it.coerceIn(2.0f, 20.0f) },
    )
    list += CalibrationParamDescriptor(
      key = "repelCost",
      tabIndex = 4,
      title = "Repel Shockwave Flux Cost",
      unit = "Flux",
      description = "Flux drained when triggering a 360° Repel defensive shockwave.",
      min = 0f, max = 800f, step = 20f, decimals = 0,
      getter = { repelCost }, setter = { repelCost = it.coerceIn(0f, 800f) },
    )
    list += CalibrationParamDescriptor(
      key = "repelRadius",
      tabIndex = 4,
      title = "Repel Shockwave Radius",
      unit = "px",
      description = "Effective blast radius of Repel pushing enemy ships, bombs, mines, and crates.",
      min = 200f, max = 900f, step = 20f, decimals = 0,
      getter = { repelRadius }, setter = { repelRadius = it.coerceIn(200f, 900f) },
    )
    list += CalibrationParamDescriptor(
      key = "repelPushForce",
      tabIndex = 4,
      title = "Repel Knockback Velocity",
      unit = "px/s",
      description = "Outward velocity impulse imparted by Repel to nearby ships and projectiles.",
      min = 300f, max = 1800f, step = 40f, decimals = 0,
      getter = { repelPushForce }, setter = { repelPushForce = it.coerceIn(300f, 1800f) },
    )
    list += CalibrationParamDescriptor(
      key = "crateMultiShotAmmo",
      tabIndex = 4,
      title = "Multi-Shot Crate Ammo Grant",
      unit = "shots",
      description = "Trigger pulls of Multi-Shot granted per Multi-Shot Supply Crate.",
      min = 5f, max = 80f, step = 5f, decimals = 0,
      getter = { crateMultiShotAmmo.toFloat() }, setter = { crateMultiShotAmmo = it.roundToInt().coerceIn(5, 80) },
    )
    list += CalibrationParamDescriptor(
      key = "overchargeSurgePctPerStack",
      tabIndex = 4,
      title = "Permanent Overcharge Bonus Per Stack",
      unit = "%",
      description = "Stat bonus per stack for Speed, Thrust, or Reactor Overcharge upgrades (max 3 stacks each).",
      min = 0.02f, max = 0.25f, step = 0.01f, decimals = 2,
      getter = { overchargeSurgePctPerStack }, setter = { overchargeSurgePctPerStack = it.coerceIn(0.02f, 0.25f) },
    )

    // Dynamically append sliders for all Custom AI-Forged Buffs in BUFF_REGISTRY!
    for (customBuff in RiftCodex.getCustomForgedBuffs()) {
      val bId = customBuff.id
      val bName = customBuff.officialName
      list += CalibrationParamDescriptor(
        key = "forged_${bId}_charges",
        tabIndex = 4,
        title = "[FORGED] $bName Charges",
        unit = "charges",
        description = "Charges granted per Supply Crate pickup for AI-forged buff $bName.",
        min = 1f, max = 8f, step = 1f, decimals = 0,
        getter = { (RiftCodex.BUFF_REGISTRY[bId]?.chargesPerPickup ?: 1).toFloat() },
        setter = { RiftCodex.BUFF_REGISTRY[bId]?.chargesPerPickup = it.roundToInt().coerceIn(1, 8) },
      )
      list += CalibrationParamDescriptor(
        key = "forged_${bId}_duration",
        tabIndex = 4,
        title = "[FORGED] $bName Flight / Effect Time",
        unit = "s",
        description = "Active flight or effect lifespan in seconds for $bName.",
        min = 0.5f, max = 12.0f, step = 0.25f, decimals = 2,
        getter = { RiftCodex.BUFF_REGISTRY[bId]?.flightDuration ?: 2.5f },
        setter = { RiftCodex.BUFF_REGISTRY[bId]?.flightDuration = it.coerceIn(0.5f, 12.0f) },
      )
      list += CalibrationParamDescriptor(
        key = "forged_${bId}_speed",
        tabIndex = 4,
        title = "[FORGED] $bName Projectile Speed",
        unit = "px/s",
        description = "Launch velocity in pixels/sec for $bName.",
        min = 120f, max = 1600f, step = 20f, decimals = 0,
        getter = { RiftCodex.BUFF_REGISTRY[bId]?.projectileSpeed ?: 640f },
        setter = { RiftCodex.BUFF_REGISTRY[bId]?.projectileSpeed = it.coerceIn(120f, 1600f) },
      )
      list += CalibrationParamDescriptor(
        key = "forged_${bId}_damage",
        tabIndex = 4,
        title = "[FORGED] $bName Flux Damage",
        unit = "Flux",
        description = "Direct/AoE Flux damage inflicted by $bName.",
        min = 0f, max = 2500f, step = 25f, decimals = 0,
        getter = { RiftCodex.BUFF_REGISTRY[bId]?.damage ?: 480f },
        setter = { RiftCodex.BUFF_REGISTRY[bId]?.damage = it.coerceIn(0f, 2500f) },
      )
      list += CalibrationParamDescriptor(
        key = "forged_${bId}_radius",
        tabIndex = 4,
        title = "[FORGED] $bName Effect Radius",
        unit = "px",
        description = "Blast, vortex pull, EMP, or barrier span radius for $bName.",
        min = 40f, max = 650f, step = 15f, decimals = 0,
        getter = { RiftCodex.BUFF_REGISTRY[bId]?.blastRadius ?: 200f },
        setter = { RiftCodex.BUFF_REGISTRY[bId]?.blastRadius = it.coerceIn(40f, 650f) },
      )
    }

    // --- TAB 5: PLAYFIELD RIFTS & SUPPLY CRATES ---
    list += CalibrationParamDescriptor(
      key = "riftLifespanSec",
      tabIndex = 5,
      title = "Playfield Rift Lifespan",
      unit = "s",
      description = "Active lifetime of each Playfield Rift before relocating to a new sector.",
      min = 15f, max = 120f, step = 5f, decimals = 0,
      getter = { riftLifespanSec }, setter = { riftLifespanSec = it.coerceIn(15f, 120f) },
    )
    list += CalibrationParamDescriptor(
      key = "riftPhysicalRadius",
      tabIndex = 5,
      title = "Playfield Rift Core Radius",
      unit = "px",
      description = "Base physical core radius for Phase, Plasma, Repulsor, and Gravity Rifts.",
      min = 80f, max = 340f, step = 10f, decimals = 0,
      getter = { riftPhysicalRadius }, setter = { riftPhysicalRadius = it.coerceIn(80f, 340f) },
    )
    list += CalibrationParamDescriptor(
      key = "plasmaRiftDamagePct",
      tabIndex = 5,
      title = "Plasma Rift Burn Rate",
      unit = "%",
      description = "Fraction of Max Flux burned per 0.6s tick while inside a crimson Plasma Rift.",
      min = 0.02f, max = 0.40f, step = 0.02f, decimals = 2,
      getter = { plasmaRiftDamagePct }, setter = { plasmaRiftDamagePct = it.coerceIn(0.02f, 0.40f) },
    )
    list += CalibrationParamDescriptor(
      key = "repulsorFieldRadius",
      tabIndex = 5,
      title = "Repulsor Rift Push Radius",
      unit = "px",
      description = "Outer radial field distance at which emerald Repulsor Rifts push ships outward.",
      min = 240f, max = 900f, step = 20f, decimals = 0,
      getter = { repulsorFieldRadius }, setter = { repulsorFieldRadius = it.coerceIn(240f, 900f) },
    )
    list += CalibrationParamDescriptor(
      key = "repulsorBaseForce",
      tabIndex = 5,
      title = "Repulsor Rift Push Force",
      unit = "px/s²",
      description = "Outward acceleration strength exerted by Repulsor Rifts.",
      min = 80f, max = 900f, step = 20f, decimals = 0,
      getter = { repulsorBaseForce }, setter = { repulsorBaseForce = it.coerceIn(80f, 900f) },
    )
    list += CalibrationParamDescriptor(
      key = "gravityPullRadius",
      tabIndex = 5,
      title = "Gravity Rift Suction Radius",
      unit = "px",
      description = "Outer event-horizon pull radius of amber Gravity Rifts.",
      min = 240f, max = 1000f, step = 20f, decimals = 0,
      getter = { gravityPullRadius }, setter = { gravityPullRadius = it.coerceIn(240f, 1000f) },
    )
    list += CalibrationParamDescriptor(
      key = "gravityPullForce",
      tabIndex = 5,
      title = "Gravity Rift Pull Strength",
      unit = "px/s²",
      description = "Inward gravitational acceleration pulling ships and projectiles toward the singularity.",
      min = 80f, max = 1000f, step = 20f, decimals = 0,
      getter = { gravityPullForce }, setter = { gravityPullForce = it.coerceIn(80f, 1000f) },
    )
    list += CalibrationParamDescriptor(
      key = "gravityStopImmunitySec",
      tabIndex = 5,
      title = "Gravity Core Escape Immunity",
      unit = "s",
      description = "Pull-immunity window granted after hitting dead-stop at a Gravity Rift center.",
      min = 0.5f, max = 5.0f, step = 0.25f, decimals = 2,
      getter = { gravityStopImmunitySec }, setter = { gravityStopImmunitySec = it.coerceIn(0.5f, 5.0f) },
    )
    list += CalibrationParamDescriptor(
      key = "phaseRiftCooldownSec",
      tabIndex = 5,
      title = "Phase Rift Warp Re-Entry Cooldown",
      unit = "s",
      description = "Per-ship teleportation cooldown after warping through a violet Phase Rift.",
      min = 0.25f, max = 5.0f, step = 0.25f, decimals = 2,
      getter = { phaseRiftCooldownSec }, setter = { phaseRiftCooldownSec = it.coerceIn(0.25f, 5.0f) },
    )
    list += CalibrationParamDescriptor(
      key = "buffCapFfa",
      tabIndex = 5,
      title = "Arena Supply Crate Cap",
      unit = "crates",
      description = "Maximum simultaneous Supply Crates maintained across the Public FFA sector.",
      min = 0f, max = 80f, step = 2f, decimals = 0,
      getter = { (buffTargetCountOverride ?: buffCapFfa).toFloat() },
      setter = {
        val c = it.roundToInt().coerceIn(0, 80)
        buffCapFfa = c
        if (buffTargetCountOverride != null) buffTargetCountOverride = c
      },
    )
    list += CalibrationParamDescriptor(
      key = "buffRespawnFrequency",
      tabIndex = 5,
      title = "Supply Crate Respawn Rate",
      unit = "x",
      description = "Multiplier on how rapidly picked-up Supply Crates replenish (0x disables respawns).",
      min = 0.0f, max = 3.0f, step = 0.1f, decimals = 1,
      getter = { buffRespawnFrequency }, setter = { buffRespawnFrequency = it.coerceIn(0.0f, 3.0f) },
    )
    list += CalibrationParamDescriptor(
      key = "buffBaseRespawnDelay",
      tabIndex = 5,
      title = "Supply Crate Base Spawn Delay",
      unit = "s",
      description = "Baseline delay in seconds between individual Supply Crate spawns.",
      min = 0.5f, max = 10.0f, step = 0.5f, decimals = 1,
      getter = { buffBaseRespawnDelay }, setter = { buffBaseRespawnDelay = it.coerceIn(0.5f, 10.0f) },
    )

    // --- TAB 6: AI BOT BEHAVIOR ---
    list += CalibrationParamDescriptor(
      key = "botRetreatFluxRatio",
      tabIndex = 6,
      title = "AI Low-Flux Evasion Threshold",
      unit = "%",
      description = "Flux capacitor percentage below which AI bots switch to defensive kiting and crate hunting.",
      min = 0.10f, max = 0.75f, step = 0.05f, decimals = 2,
      getter = { botRetreatFluxRatio }, setter = { botRetreatFluxRatio = it.coerceIn(0.10f, 0.75f) },
    )
    list += CalibrationParamDescriptor(
      key = "botSafeZoneBanMargin",
      tabIndex = 6,
      title = "AI Safe Zone Exclusion Buffer",
      unit = "px",
      description = "Outer exclusion margin around the Safe Zone that AI bots actively steer away from.",
      min = 100f, max = 800f, step = 25f, decimals = 0,
      getter = { botSafeZoneBanMargin }, setter = { botSafeZoneBanMargin = it.coerceIn(100f, 800f) },
    )
    list += CalibrationParamDescriptor(
      key = "botEngageRange",
      tabIndex = 6,
      title = "AI Target Acquisition Range",
      unit = "px",
      description = "Maximum radar distance within which AI bots lock onto and pursue hostile ships.",
      min = 500f, max = 2400f, step = 50f, decimals = 0,
      getter = { botEngageRange }, setter = { botEngageRange = it.coerceIn(500f, 2400f) },
    )
    list += CalibrationParamDescriptor(
      key = "botGunFireRange",
      tabIndex = 6,
      title = "AI Primary Weapon Fire Range",
      unit = "px",
      description = "Distance within which AI bots open fire with primary guns when aligned on target.",
      min = 350f, max = 1600f, step = 50f, decimals = 0,
      getter = { botGunFireRange }, setter = { botGunFireRange = it.coerceIn(350f, 1600f) },
    )
    list += CalibrationParamDescriptor(
      key = "botSpecialBuffChance",
      tabIndex = 6,
      title = "AI Special Buff Aggression",
      unit = "%",
      description = "Probability per tactical decision tick that an AI bot fires its held Special Buff.",
      min = 0.00f, max = 0.50f, step = 0.02f, decimals = 2,
      getter = { botSpecialBuffChance }, setter = { botSpecialBuffChance = it.coerceIn(0.00f, 0.50f) },
    )
    list += CalibrationParamDescriptor(
      key = "botLookAheadDist",
      tabIndex = 6,
      title = "AI Obstacle Look-Ahead Sensor",
      unit = "px",
      description = "Forward raycast whisker distance AI bots use to avoid arena walls and hazards.",
      min = 80f, max = 400f, step = 20f, decimals = 0,
      getter = { botLookAheadDist }, setter = { botLookAheadDist = it.coerceIn(80f, 400f) },
    )

    // --- TAB 7: AUTO-DISCOVERED ENGINE VARIABLES ---
    list += CalibrationParamDescriptor(
      key = "publicMatchCycleLimitSec",
      tabIndex = 7,
      title = camelCaseToPlainEnglish("publicMatchCycleLimitSec"),
      unit = "s",
      description = "Total duration of a Public FFA Sector Convergence match cycle before procedural map reset.",
      min = 300f, max = 7200f, step = 300f, decimals = 0,
      getter = { publicMatchCycleLimitSec }, setter = { publicMatchCycleLimitSec = it.coerceIn(300f, 7200f) },
    )
    list += CalibrationParamDescriptor(
      key = "hvtBountyThreshold",
      tabIndex = 7,
      title = camelCaseToPlainEnglish("hvtBountyThreshold"),
      unit = "RES",
      description = "Resonance score at which any ship earns a gold Rift Leader radar ping.",
      min = 150f, max = 1000f, step = 25f, decimals = 0,
      getter = { hvtBountyThreshold.toFloat() }, setter = { hvtBountyThreshold = it.roundToInt().coerceIn(150, 1000) },
    )
    list += CalibrationParamDescriptor(
      key = "bountyKillBonusPct",
      tabIndex = 7,
      title = camelCaseToPlainEnglish("bountyKillBonusPct"),
      unit = "%",
      description = "Fraction of a victim's accumulated Resonance awarded to their executioner upon elimination.",
      min = 0.10f, max = 1.00f, step = 0.05f, decimals = 2,
      getter = { bountyKillBonusPct }, setter = { bountyKillBonusPct = it.coerceIn(0.10f, 1.00f) },
    )
    list += CalibrationParamDescriptor(
      key = "cratePickupBounty",
      tabIndex = 7,
      title = camelCaseToPlainEnglish("cratePickupBounty"),
      unit = "RES",
      description = "Bonus Resonance awarded whenever a ship collects a standard Supply Crate.",
      min = 0f, max = 50f, step = 5f, decimals = 0,
      getter = { cratePickupBounty.toFloat() }, setter = { cratePickupBounty = it.roundToInt().coerceIn(0, 50) },
    )
    list += CalibrationParamDescriptor(
      key = "fullChargeCrateBounty",
      tabIndex = 7,
      title = camelCaseToPlainEnglish("fullChargeCrateBounty"),
      unit = "RES",
      description = "Bonus Resonance awarded when collecting a Full Flux Charge Supply Crate.",
      min = 0f, max = 60f, step = 5f, decimals = 0,
      getter = { fullChargeCrateBounty.toFloat() }, setter = { fullChargeCrateBounty = it.roundToInt().coerceIn(0, 60) },
    )
    list += CalibrationParamDescriptor(
      key = "hitStopDurationSec",
      tabIndex = 7,
      title = camelCaseToPlainEnglish("hitStopDurationSec"),
      unit = "s",
      description = "Brief micro-freeze impact frame duration triggered on heavy lethal ship explosions.",
      min = 0.00f, max = 0.12f, step = 0.005f, decimals = 2,
      getter = { hitStopDurationSec }, setter = { hitStopDurationSec = it.coerceIn(0.00f, 0.12f) },
    )
    list += CalibrationParamDescriptor(
      key = "skirmishKillTargetPerRound",
      tabIndex = 7,
      title = "Rift Skirmish Kills Per Round",
      unit = "kills",
      description = "Team eliminations required to win a single round in Best-of-3 Rift Skirmish and Syndicate Wars TDM.",
      min = 5f, max = 40f, step = 1f, decimals = 0,
      getter = { skirmishKillTargetPerRound.toFloat() },
      setter = { skirmishKillTargetPerRound = it.roundToInt().coerceIn(5, 40) },
    )
    list += CalibrationParamDescriptor(
      key = "fluxCoreBountyPerSec",
      tabIndex = 7,
      title = "Flux Core Carrier Resonance Tick",
      unit = "RES/s",
      description = "Resonance awarded every second to the pilot carrying the Volatile Flux Core singularity.",
      min = 5f, max = 50f, step = 5f, decimals = 0,
      getter = { fluxCoreBountyPerSec.toFloat() },
      setter = { fluxCoreBountyPerSec = it.roundToInt().coerceIn(5, 50) },
    )
    list += CalibrationParamDescriptor(
      key = "fluxCoreEjectSpeed",
      tabIndex = 7,
      title = "Flux Core Death Ejection Velocity",
      unit = "px/s",
      description = "Initial supersonic velocity when the Flux Core shoots off upon carrier elimination or Safe Zone entry.",
      min = 800f, max = 2600f, step = 50f, decimals = 0,
      getter = { fluxCoreEjectSpeed },
      setter = { fluxCoreEjectSpeed = it.coerceIn(800f, 2600f) },
    )
    list += CalibrationParamDescriptor(
      key = "stasisBrigReactorMaxHp",
      tabIndex = 7,
      title = "Brig Core HP",
      unit = "Flux",
      description = "Max HP of the West and East Stasis Cores in Stasis Breach (8,000 Flux default; prisoners deal 100% damage).",
      min = 2000f, max = 100000f, step = 1000f, decimals = 0,
      getter = { stasisBrigReactorMaxHp },
      setter = { stasisBrigReactorMaxHp = it.coerceIn(2000f, 100000f) },
    )

    return list
  }

  val descriptors: List<CalibrationParamDescriptor>
    get() = buildAllParameterDescriptors()

  /**
   * Deep-frozen baseline snapshot of all factory default numeric calibration parameters.
   */
  val DEFAULT_GAME_CONFIG: Map<String, Float> by lazy {
    val snapshot = LinkedHashMap<String, Float>()
    buildAllParameterDescriptors()
      .filter { !it.key.startsWith("forged_") }
      .forEach { snapshot[it.key] = it.getter() }
    snapshot
  }

  fun ensureBaselineCaptured() {
    DEFAULT_GAME_CONFIG.size
  }

  fun isCustomGameSandboxActive(engine: RiftGameEngine?): Boolean =
    engine?.currentCustomGame != null

  /**
   * Guarantees that all 6 Main Ranked Modes ALWAYS run 100% factory default rules,
   * without overwriting the user's saved custom slider preset in SharedPreferences or HUD layout offsets.
   */
  fun enforceRankedFactoryDefaultsIfNeeded(engine: RiftGameEngine?) {
    if (engine != null && engine.currentCustomGame == null) {
      ensureBaselineCaptured()
      RiftCodex.resetShipsToDefaults()
      RiftCodex.resetBuffRegistryToDefaults(preserveCustomForged = true)

      for (desc in buildAllParameterDescriptors()) {
        if (desc.tabIndex in 1..7 && !desc.key.startsWith("forged_")) {
          DEFAULT_GAME_CONFIG[desc.key]?.let { desc.setter(it) }
        }
      }
      shipScale = 1.0f
      projectileScale = 1.0f
      anomalyScale = 1.0f
      botsEnabled = true

      cloakDrainPerSec = 36f
      empFluxDrainPct = 0.25f
      empFreezeSec = 2.0f
      interdictorBubbleRadius = 275f
      interdictorBubbleDuration = 6.0f
      interdictorBubbleCooldown = 8.5f
      stasisBarrierDuration = 45.0f
      stasisBrigReactorMaxHp = 8000f
      RiftCodex.getShipById(6).bombCost = 0f
      RiftCodex.getShipById(6).bombCooldown = 4.5f
      RiftCodex.getShipById(9).bombCost = 320f
      RiftCodex.getShipById(9).bombCooldown = 8.5f

      engine.publicMatchCycleLimitSec = publicMatchCycleLimitSec
      for (ship in engine.ships) {
        val updatedSpec = RiftCodex.getShipById(ship.spec.id)
        ship.spec = updatedSpec
        val targetMax = updatedSpec.maxFlux
        if (abs(ship.maxFlux - targetMax) > 0.5f) {
          val ratio = if (ship.maxFlux > 1f) (ship.flux / ship.maxFlux).coerceIn(0.05f, 1f) else 1f
          ship.maxFlux = targetMax
          ship.flux = (targetMax * ratio).coerceIn(1f, targetMax)
        }
      }
      for (anom in engine.anomalies) {
        if (!anom.isUnstableMergedRift &&
          !(anom.type == AnomalyType.GRAVITY_RIFT && anom.absorbedWeaponCount > 0) &&
          anom.absorbedWeaponCount == 0
        ) {
          anom.physicalRadius = riftPhysicalRadius
        }
      }
      for (b in engine.interdictorBubbles) {
        b.radius = interdictorBubbleRadius
      }
      engine.enforceBotsEnabledState()
      calibrationRevision++
    }
  }

  /**
   * Immediately propagates any modified Calibration value into active live ships, match timers,
   * and registry entries when in a Custom Game, or enforces factory defaults in a Main Ranked Game.
   */
  fun applyCalibrationChangesLive(engine: RiftGameEngine?, context: Context? = null) {
    ensureBaselineCaptured()
    if (context != null) {
      savePersistedLayout(context)
    }

    if (engine != null && engine.currentCustomGame == null) {
      enforceRankedFactoryDefaultsIfNeeded(engine)
      return
    }

    RiftCodex.BUFF_REGISTRY["SEEKER_TORPEDO"]?.let {
      it.flightDuration = seekerFlightTime
      it.projectileSpeed = seekerSpeed
      it.damage = seekerDamage
    }
    RiftCodex.BUFF_REGISTRY["NOVA_RING"]?.let {
      it.projectileSpeed = novaBoltSpeed
      it.damage = novaBoltDamage
    }
    RiftCodex.BUFF_REGISTRY["SHATTER_CONE"]?.let {
      it.damage = shatterPelletDamage
    }
    RiftCodex.BUFF_REGISTRY["STASIS_BARRIER"]?.let {
      it.flightDuration = stasisBarrierDuration
      it.blastRadius = stasisBarrierLength
    }
    RiftCodex.BUFF_REGISTRY["HYPER_SURGE"]?.let {
      it.flightDuration = hyperSurgeDuration
    }
    RiftCodex.BUFF_REGISTRY["HOLO_PHANTOM"]?.let {
      it.flightDuration = holoPhantomDuration
    }
    RiftCodex.getShipById(9).bombCooldown = interdictorBubbleCooldown

    if (engine != null && engine.currentCustomGame != null) {
      engine.publicMatchCycleLimitSec = publicMatchCycleLimitSec
      for (ship in engine.ships) {
        val updatedSpec = RiftCodex.getShipById(ship.spec.id)
        ship.spec = updatedSpec
        val targetMax = updatedSpec.maxFlux
        if (abs(ship.maxFlux - targetMax) > 0.5f) {
          val ratio = if (ship.maxFlux > 1f) (ship.flux / ship.maxFlux).coerceIn(0.05f, 1f) else 1f
          ship.maxFlux = targetMax
          ship.flux = (targetMax * ratio).coerceIn(1f, targetMax)
        }
      }
      for (anom in engine.anomalies) {
        if (!anom.isUnstableMergedRift &&
          !(anom.type == AnomalyType.GRAVITY_RIFT && anom.absorbedWeaponCount > 0) &&
          anom.absorbedWeaponCount == 0
        ) {
          anom.physicalRadius = riftPhysicalRadius
        }
      }
      for (b in engine.interdictorBubbles) {
        b.radius = interdictorBubbleRadius
      }
      engine.enforceBotsEnabledState()
    }
    calibrationRevision++
  }

  fun exportForgedBuffsJson(): String {
    val forgedArr = JSONArray()
    RiftCodex.getCustomForgedBuffs().forEach { forgedArr.put(it.toJsonObject()) }
    return forgedArr.toString()
  }

  fun exportGameplaySyncPayloadJson(): String {
    ensureBaselineCaptured()
    val slidersObj = JSONObject()
    val allowedTab0Keys = setOf("shipScale", "projectileScale", "anomalyScale")
    for (desc in buildAllParameterDescriptors()) {
      if (desc.tabIndex in 1..7 || desc.key in allowedTab0Keys) {
        slidersObj.put(desc.key, desc.getter().toDouble())
      }
    }
    return JSONObject().apply {
      put("botsEnabled", botsEnabled)
      put("forgedBuffsJson", exportForgedBuffsJson())
      put("sliders", slidersObj)
    }.toString()
  }

  fun applyRemoteCustomGameSyncPayload(
    payloadJson: String,
    engine: RiftGameEngine?,
    context: Context? = null,
  ): Boolean {
    if (engine?.currentCustomGame == null) return false
    ensureBaselineCaptured()
    return try {
      val root = JSONObject(payloadJson)
      if (root.has("botsEnabled")) {
        botsEnabled = root.optBoolean("botsEnabled", true)
      }
      val forgedRaw = root.optString("forgedBuffsJson", "")
      if (forgedRaw.isNotBlank()) {
        try {
          val arr = JSONArray(forgedRaw)
          for (i in 0 until arr.length()) {
            val entry = SpecialBuffRegistryEntry.fromJsonObject(arr.getJSONObject(i))
            RiftCodex.registerCustomBuff(entry)
          }
        } catch (_: Exception) {}
      }
      val slidersObj = root.optJSONObject("sliders")
      if (slidersObj != null) {
        val descriptorsByKey = buildAllParameterDescriptors().associateBy { it.key }
        val keys = slidersObj.keys()
        while (keys.hasNext()) {
          val k = keys.next()
          val desc = descriptorsByKey[k]
          if (desc != null) {
            val v = slidersObj.optDouble(k, desc.getter().toDouble()).toFloat()
            desc.setter(v)
          }
        }
      }
      calibrationRevision++
      applyCalibrationChangesLive(engine, context)
      true
    } catch (_: Exception) {
      false
    }
  }

  fun generateLocalHullRecipe(prompt: String, variationSalt: Int = 0): CustomHullVisualRecipe =
    CustomHullVisualRecipe.synthesizeFromPrompt(prompt, variationSalt)

  fun forgeBuffFromPrompt(prompt: String): SpecialBuffRegistryEntry =
    SpecialBuffRegistryEntry.synthesizeFromPrompt(prompt, RiftCodex.nextCustomBuffTypeIndex())

  fun toggleParameterHidden(key: String, hidden: Boolean, context: Context? = null) {
    hiddenCalibrationKeys =
      if (hidden) {
        hiddenCalibrationKeys + key
      } else {
        hiddenCalibrationKeys - key
      }
    calibrationRevision++
    if (context != null) {
      savePersistedLayout(context)
    }
  }

  fun unhideAllParameters(context: Context? = null) {
    hiddenCalibrationKeys = emptySet()
    calibrationRevision++
    if (context != null) {
      savePersistedLayout(context)
    }
  }

  @JvmOverloads
  fun loadPersistedLayout(context: Context, engine: RiftGameEngine? = null) {
    ensureBaselineCaptured()
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    fun readOffset(key: String): Offset {
      val x = prefs.getFloat("${key}_x", 0f)
      val y = prefs.getFloat("${key}_y", 0f)
      return Offset(x, y)
    }
    topBarOffset = readOffset("topBar")
    buffBarOffset = readOffset("buffBar")
    killLogOffset = readOffset("killLog")
    bottomConsoleOffset = readOffset("bottomConsole")
    combatArcOffset = readOffset("combatArc")
    leftStickOffset = readOffset("leftStick")
    fireStickOffset = readOffset("fireStick")
    bombBtnOffset = readOffset("bombBtn")
    repelBtnOffset = readOffset("repelBtn")
    burstBtnOffset = readOffset("burstBtn")
    brakeBtnOffset = readOffset("brakeBtn")
    multiBtnOffset = readOffset("multiBtn")
    calibrationBtnOffset = readOffset("calibrationBtn")
    nexusBtnOffset = readOffset("nexusBtn")
    toastContainerOffset = readOffset("toastContainer")
    spectatorBoxOffset = readOffset("spectatorBox")
    idleWarningBannerOffset = readOffset("idleWarningBanner")
    unlockFullNinthShipOverride = prefs.getBoolean("unlockFullNinthShipOverride", false)
    botsEnabled = prefs.getBoolean("bots_enabled", true)

    bindPrimaryFire = prefs.getInt("bindPrimaryFire", BIND_RT)
    bindSpecialBuff = prefs.getInt("bindSpecialBuff", android.view.KeyEvent.KEYCODE_BUTTON_R1)
    bindRetroBrake = prefs.getInt("bindRetroBrake", BIND_LT)
    bindSecondaryAbility = prefs.getInt("bindSecondaryAbility", android.view.KeyEvent.KEYCODE_BUTTON_X)
    bindRepel = prefs.getInt("bindRepel", android.view.KeyEvent.KEYCODE_BUTTON_A)
    bindMultiToggle = prefs.getInt("bindMultiToggle", android.view.KeyEvent.KEYCODE_BUTTON_Y)

    val hiddenCsv = prefs.getString(KEY_HIDDEN_SET, "") ?: ""
    hiddenCalibrationKeys =
      hiddenCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()

    // Restore persisted Forged Custom Buffs into BUFF_REGISTRY
    val forgedRaw = prefs.getString(KEY_FORGED_BUFFS_JSON, "[]") ?: "[]"
    try {
      val arr = JSONArray(forgedRaw)
      for (i in 0 until arr.length()) {
        val entry = SpecialBuffRegistryEntry.fromJsonObject(arr.getJSONObject(i))
        RiftCodex.registerCustomBuff(entry)
      }
    } catch (_: Exception) {}

    // Restore persisted NEXUS FORGE Visual Recipes & Dynamic Lighting
    val visualForgeRaw = prefs.getString(KEY_FORGED_VISUAL_RECIPES_JSON, null)
    if (!visualForgeRaw.isNullOrBlank()) {
      try {
        RiftVisualForgeState.loadFromJsonObject(JSONObject(visualForgeRaw))
      } catch (_: Exception) {}
    }

    // Restore persisted NEXUS FORGE Soundtrack Track A-C & SFX Recipes
    val audioForgeRaw = prefs.getString(KEY_FORGED_AUDIO_JSON, null)
    if (!audioForgeRaw.isNullOrBlank()) {
      try {
        RiftSoundSynthesizer.importAudioForgeJson(JSONObject(audioForgeRaw))
      } catch (_: Exception) {}
    }

    // Restore persisted Syndicate Crest Recipe
    val crestRaw = prefs.getString(KEY_FORGED_SYNDICATE_CREST_JSON, null)
    if (!crestRaw.isNullOrBlank()) {
      try {
        RiftVisualForgeState.updateSyndicateCrest(SyndicateCrestRecipe.fromJson(JSONObject(crestRaw)))
      } catch (_: Exception) {}
    }

    // Restore persisted slider overrides
    val savedConfigRaw = prefs.getString(KEY_SAVED_CONFIG_JSON, null)
    if (!savedConfigRaw.isNullOrBlank()) {
      try {
        val obj = JSONObject(savedConfigRaw)
        val excludedUserSettingKeys =
          setOf(
            "cameraZoomScale",
            "nametagFontSizePx",
            "bountyFontSizePx",
            "fctFontSizePx",
            "labelTextScale",
            "fctScale",
            "ship_6_bombCost",
          )
        val descriptorsByKey = buildAllParameterDescriptors().associateBy { it.key }
        val keys = obj.keys()
        while (keys.hasNext()) {
          val k = keys.next()
          if (k in excludedUserSettingKeys) continue
          val desc = descriptorsByKey[k]
          if (desc != null) {
            val loadedVal = obj.optDouble(k, desc.getter().toDouble()).toFloat()
            when {
              k == "phaseDashImpulse" && loadedVal <= 650f -> desc.setter(1120f)
              k == "stasisBrigReactorMaxHp" && abs(loadedVal - 40000f) < 50f -> desc.setter(8000f)
              k == "cloakDrainPerSec" && loadedVal >= 150f -> desc.setter(36f)
              k == "empFluxDrainPct" && loadedVal >= 0.34f -> desc.setter(0.25f)
              k == "empFreezeSec" && loadedVal >= 2.4f -> desc.setter(2.0f)
              k == "interdictorBubbleDuration" && (loadedVal >= 7.5f || abs(loadedVal - 5.0f) < 0.05f) -> desc.setter(6.0f)
              k == "interdictorBubbleCooldown" && loadedVal >= 9.0f -> desc.setter(8.5f)
              k == "stasisBarrierDuration" && loadedVal <= 15.0f -> desc.setter(45.0f)
              k == "ship_9_bombCost" && loadedVal >= 350f -> desc.setter(320f)
              k == "ship_9_bombCooldown" && loadedVal >= 9.0f -> desc.setter(8.5f)
              k == "ship_6_bombCooldown" && loadedVal <= 1.2f -> desc.setter(4.5f)
              else -> desc.setter(loadedVal)
            }
          }
        }
      } catch (_: Exception) {}
    }
    RiftCodex.getShipById(6).bombCost = 0f
    // Keep base constants neutral so user Settings (rift_user_settings) exclusively controls Zoom & Text Size
    cameraZoomScale = 1.0f
    nametagFontSizePx = 15.0f
    bountyFontSizePx = 16.0f
    fctFontSizePx = 14.0f
    labelTextScale = 1.0f
    fctScale = 1.0f
    applyCalibrationChangesLive(engine, null)
  }

  fun loadFromPrefs(context: Context) = loadPersistedLayout(context)

  fun savePersistedLayout(context: Context) {
    ensureBaselineCaptured()
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val editor = prefs.edit()
    fun writeOffset(key: String, offset: Offset) {
      editor.putFloat("${key}_x", offset.x)
      editor.putFloat("${key}_y", offset.y)
    }
    writeOffset("topBar", topBarOffset)
    writeOffset("buffBar", buffBarOffset)
    writeOffset("killLog", killLogOffset)
    writeOffset("bottomConsole", bottomConsoleOffset)
    writeOffset("combatArc", combatArcOffset)
    writeOffset("leftStick", leftStickOffset)
    writeOffset("fireStick", fireStickOffset)
    writeOffset("bombBtn", bombBtnOffset)
    writeOffset("repelBtn", repelBtnOffset)
    writeOffset("burstBtn", burstBtnOffset)
    writeOffset("brakeBtn", brakeBtnOffset)
    writeOffset("multiBtn", multiBtnOffset)
    writeOffset("calibrationBtn", calibrationBtnOffset)
    writeOffset("nexusBtn", nexusBtnOffset)
    writeOffset("toastContainer", toastContainerOffset)
    writeOffset("spectatorBox", spectatorBoxOffset)
    writeOffset("idleWarningBanner", idleWarningBannerOffset)
    editor.putBoolean("unlockFullNinthShipOverride", unlockFullNinthShipOverride)
    editor.putBoolean("bots_enabled", botsEnabled)
    editor.putInt("bindPrimaryFire", bindPrimaryFire)
    editor.putInt("bindSpecialBuff", bindSpecialBuff)
    editor.putInt("bindRetroBrake", bindRetroBrake)
    editor.putInt("bindSecondaryAbility", bindSecondaryAbility)
    editor.putInt("bindRepel", bindRepel)
    editor.putInt("bindMultiToggle", bindMultiToggle)
    editor.putString(KEY_HIDDEN_SET, hiddenCalibrationKeys.joinToString(","))

    val forgedArr = JSONArray()
    RiftCodex.getCustomForgedBuffs().forEach { forgedArr.put(it.toJsonObject()) }
    editor.putString(KEY_FORGED_BUFFS_JSON, forgedArr.toString())

    editor.putString(KEY_FORGED_VISUAL_RECIPES_JSON, RiftVisualForgeState.toJsonObject().toString())
    editor.putString(KEY_FORGED_AUDIO_JSON, RiftSoundSynthesizer.exportAudioForgeJson().toString())
    editor.putString(
      KEY_FORGED_SYNDICATE_CREST_JSON,
      RiftVisualForgeState.activeSyndicateCrestRecipe.toJson().toString(),
    )

    val configObj = JSONObject()
    buildAllParameterDescriptors().forEach { desc ->
      val currentVal = desc.getter()
      val defaultVal = DEFAULT_GAME_CONFIG[desc.key]
      if (defaultVal == null || abs(currentVal - defaultVal) > 0.0005f) {
        configObj.put(desc.key, currentVal.toDouble())
      }
    }
    editor.putString(KEY_SAVED_CONFIG_JSON, configObj.toString())
    editor.apply()
  }

  fun resetTab(tabIndex: Int, context: Context? = null, engine: RiftGameEngine? = null) {
    ensureBaselineCaptured()
    if (tabIndex == 2 || tabIndex == 3) {
      if (tabIndex == 2) {
        buildAllParameterDescriptors()
          .filter { it.tabIndex == 2 }
          .forEach { desc -> DEFAULT_GAME_CONFIG[desc.key]?.let { desc.setter(it) } }
      } else {
        buildAllParameterDescriptors()
          .filter { it.tabIndex == 3 }
          .forEach { desc -> DEFAULT_GAME_CONFIG[desc.key]?.let { desc.setter(it) } }
      }
    } else {
      buildAllParameterDescriptors()
        .filter { it.tabIndex == tabIndex && !it.key.startsWith("forged_") }
        .forEach { desc -> DEFAULT_GAME_CONFIG[desc.key]?.let { desc.setter(it) } }
      if (tabIndex == 0) {
        bountyPlacement = LabelPlacement.TOP
        tagPlacement = LabelPlacement.BOTTOM
        namePlacement = LabelPlacement.BOTTOM
      }
    }
    applyCalibrationChangesLive(engine, context)
  }

  fun resetAll(context: Context? = null, engine: RiftGameEngine? = null) {
    ensureBaselineCaptured()
    RiftCodex.resetShipsToDefaults()
    RiftCodex.resetBuffRegistryToDefaults(preserveCustomForged = false)

    for (desc in buildAllParameterDescriptors()) {
      DEFAULT_GAME_CONFIG[desc.key]?.let { desc.setter(it) }
    }

    cloakDrainPerSec = 36f
    empFluxDrainPct = 0.25f
    empFreezeSec = 2.0f
    interdictorBubbleRadius = 275f
    interdictorBubbleDuration = 6.0f
    interdictorBubbleCooldown = 8.5f
    stasisBarrierDuration = 45.0f
    stasisBrigReactorMaxHp = 8000f
    RiftCodex.getShipById(6).bombCost = 0f
    RiftCodex.getShipById(6).bombCooldown = 4.5f
    RiftCodex.getShipById(9).bombCost = 320f
    RiftCodex.getShipById(9).bombCooldown = 8.5f

    bountyPlacement = LabelPlacement.TOP
    tagPlacement = LabelPlacement.BOTTOM
    namePlacement = LabelPlacement.BOTTOM

    forceAllButtonsVisible = false
    isUiUnlockedForDragging = false
    unlockFullNinthShipOverride = false
    botsEnabled = true

    topBarOffset = Offset.Zero
    buffBarOffset = Offset.Zero
    killLogOffset = Offset.Zero
    bottomConsoleOffset = Offset.Zero
    combatArcOffset = Offset.Zero
    leftStickOffset = Offset.Zero
    fireStickOffset = Offset.Zero
    bombBtnOffset = Offset.Zero
    repelBtnOffset = Offset.Zero
    burstBtnOffset = Offset.Zero
    brakeBtnOffset = Offset.Zero
    multiBtnOffset = Offset.Zero
    calibrationBtnOffset = Offset.Zero
    nexusBtnOffset = Offset.Zero
    toastContainerOffset = Offset.Zero
    spectatorBoxOffset = Offset.Zero
    idleWarningBannerOffset = Offset.Zero

    buffModeOverride = "FFA"
    buffTargetCountOverride = null
    resetControllerBindingsToDefault(null)
    RiftVisualForgeState.resetAllVisualTargetsToDefault()
    RiftSoundSynthesizer.resetAudioForgeToDefaults()

    applyCalibrationChangesLive(engine, context)
  }

  /**
   * Exports a complete JSON bundle of all NEXUS FORGE visual recipes, dynamic lighting,
   * Syndicate crest, Track A-C + SFX audio recipes, HUD offsets, forged buffs, and calibration sliders.
   */
  fun exportDevForgeBundleJson(): String {
    ensureBaselineCaptured()
    val calibrationObj = JSONObject()
    for (desc in buildAllParameterDescriptors()) {
      val cur = desc.getter()
      calibrationObj.put(desc.key, ((cur * 1000f).roundToInt() / 1000.0))
    }

    fun offsetJson(o: Offset): JSONObject =
      JSONObject().apply {
        put("x", ((o.x * 10f).roundToInt() / 10.0))
        put("y", ((o.y * 10f).roundToInt() / 10.0))
      }

    val hudOffsetsObj =
      JSONObject().apply {
        put("topBar", offsetJson(topBarOffset))
        put("buffBar", offsetJson(buffBarOffset))
        put("killLog", offsetJson(killLogOffset))
        put("bottomConsole", offsetJson(bottomConsoleOffset))
        put("combatArc", offsetJson(combatArcOffset))
        put("leftStick", offsetJson(leftStickOffset))
        put("fireStick", offsetJson(fireStickOffset))
        put("bombBtn", offsetJson(bombBtnOffset))
        put("repelBtn", offsetJson(repelBtnOffset))
        put("burstBtn", offsetJson(burstBtnOffset))
        put("brakeBtn", offsetJson(brakeBtnOffset))
        put("multiBtn", offsetJson(multiBtnOffset))
        put("calibrationBtn", offsetJson(calibrationBtnOffset))
        put("nexusBtn", offsetJson(nexusBtnOffset))
        put("toastContainer", offsetJson(toastContainerOffset))
        put("spectatorBox", offsetJson(spectatorBoxOffset))
        put("idleWarningBanner", offsetJson(idleWarningBannerOffset))
      }

    val forgedBuffsArr = JSONArray()
    RiftCodex.getCustomForgedBuffs().forEach { forgedBuffsArr.put(it.toJsonObject()) }

    return JSONObject().apply {
      put("bundleVersion", 1)
      put("visualForge", RiftVisualForgeState.toJsonObject())
      put("syndicateCrest", RiftVisualForgeState.activeSyndicateCrestRecipe.toJson())
      put("audioForge", RiftSoundSynthesizer.exportAudioForgeJson())
      put("hudOffsets", hudOffsetsObj)
      put(
        "labelPlacements",
        JSONObject().apply {
          put("bountyPlacement", bountyPlacement.name)
          put("tagPlacement", tagPlacement.name)
          put("namePlacement", namePlacement.name)
        },
      )
      put("forgedBuffs", forgedBuffsArr)
      put("calibrationValues", calibrationObj)
    }.toString(2)
  }

  /**
   * Imports a Developer Forge JSON bundle (raw JSON or markdown code block) and applies all
   * visual recipes, audio tracks, HUD offsets, forged buffs, and calibration sliders live.
   */
  fun importDevForgeBundleJson(
    rawJson: String,
    engine: RiftGameEngine? = null,
    context: Context? = null,
  ): Boolean {
    ensureBaselineCaptured()
    val trimmed = rawJson.trim()
    if (trimmed.isEmpty()) return false
    val extracted =
      if (trimmed.contains("```")) {
        val afterOpen = trimmed.substringAfter("```").let {
          if (it.startsWith("json", ignoreCase = true)) it.substring(4) else it
        }
        afterOpen.substringBefore("```").trim()
      } else {
        trimmed
      }
    return try {
      val root = JSONObject(extracted)

      root.optJSONObject("visualForge")?.let { RiftVisualForgeState.loadFromJsonObject(it) }
      root.optJSONObject("syndicateCrest")?.let {
        RiftVisualForgeState.updateSyndicateCrest(SyndicateCrestRecipe.fromJson(it))
      }
      root.optJSONObject("audioForge")?.let { RiftSoundSynthesizer.importAudioForgeJson(it) }

      root.optJSONObject("hudOffsets")?.let { hObj ->
        fun parseOff(key: String, fallback: Offset): Offset {
          val o = hObj.optJSONObject(key) ?: return fallback
          return Offset(
            o.optDouble("x", fallback.x.toDouble()).toFloat(),
            o.optDouble("y", fallback.y.toDouble()).toFloat(),
          )
        }
        topBarOffset = parseOff("topBar", topBarOffset)
        buffBarOffset = parseOff("buffBar", buffBarOffset)
        killLogOffset = parseOff("killLog", killLogOffset)
        bottomConsoleOffset = parseOff("bottomConsole", bottomConsoleOffset)
        combatArcOffset = parseOff("combatArc", combatArcOffset)
        leftStickOffset = parseOff("leftStick", leftStickOffset)
        fireStickOffset = parseOff("fireStick", fireStickOffset)
        bombBtnOffset = parseOff("bombBtn", bombBtnOffset)
        repelBtnOffset = parseOff("repelBtn", repelBtnOffset)
        burstBtnOffset = parseOff("burstBtn", burstBtnOffset)
        brakeBtnOffset = parseOff("brakeBtn", brakeBtnOffset)
        multiBtnOffset = parseOff("multiBtn", multiBtnOffset)
        calibrationBtnOffset = parseOff("calibrationBtn", calibrationBtnOffset)
        nexusBtnOffset = parseOff("nexusBtn", nexusBtnOffset)
        toastContainerOffset = parseOff("toastContainer", toastContainerOffset)
        spectatorBoxOffset = parseOff("spectatorBox", spectatorBoxOffset)
        idleWarningBannerOffset = parseOff("idleWarningBanner", idleWarningBannerOffset)
      }

      root.optJSONObject("labelPlacements")?.let { lp ->
        runCatching { LabelPlacement.valueOf(lp.optString("bountyPlacement", "TOP")) }
          .getOrNull()?.let { bountyPlacement = it }
        runCatching { LabelPlacement.valueOf(lp.optString("tagPlacement", "BOTTOM")) }
          .getOrNull()?.let { tagPlacement = it }
        runCatching { LabelPlacement.valueOf(lp.optString("namePlacement", "BOTTOM")) }
          .getOrNull()?.let { namePlacement = it }
      }

      root.optJSONArray("forgedBuffs")?.let { arr ->
        for (i in 0 until arr.length()) {
          val bObj = arr.optJSONObject(i) ?: continue
          RiftCodex.registerCustomBuff(SpecialBuffRegistryEntry.fromJsonObject(bObj))
        }
      }

      val calObj = root.optJSONObject("calibrationValues") ?: root.optJSONObject("DEFAULT_GAME_CONFIG")
      if (calObj != null) {
        val byKey = buildAllParameterDescriptors().associateBy { it.key }
        val keys = calObj.keys()
        while (keys.hasNext()) {
          val k = keys.next()
          val desc = byKey[k] ?: continue
          val v = calObj.optDouble(k, desc.getter().toDouble()).toFloat()
          desc.setter(v)
        }
      }

      RiftSpriteBaker.invalidateAllVisualTargets()
      calibrationRevision++
      applyCalibrationChangesLive(engine, context)
      true
    } catch (_: Exception) {
      false
    }
  }

  /**
   * Generates a complete 1-click AI Studio prompt containing:
   * - Section A: Modified Settings & Visual Forge Diff (`key: oldDefault -> newValue`)
   * - Section B: Complete Replacement Default Objects (`DEFAULT_GAME_CONFIG`, `visualForge`, `audioForge`, `BUFF_REGISTRY`, `hiddenCalibrationKeys`)
   * Returns (promptString, modifiedCount).
   */
  fun generateAiStudioDefaultsPrompt(): Pair<String, Int> {
    ensureBaselineCaptured()
    val descriptors = buildAllParameterDescriptors()
    val diffs = mutableListOf<String>()
    val fullConfigJson = JSONObject()

    for (desc in descriptors) {
      val cur = desc.getter()
      fullConfigJson.put(desc.key, ((cur * 1000f).roundToInt() / 1000.0))
      val def = DEFAULT_GAME_CONFIG[desc.key]
      if (def != null && abs(cur - def) > 0.0005f) {
        diffs += "  - ${desc.key} (${desc.title}): ${desc.formatValue(def)} -> ${desc.formatValue(cur)}"
      }
    }

    for (target in ForgeElementTarget.entries) {
      val rec = RiftVisualForgeState.getRecipe(target)
      if (!rec.isFactoryDefault) {
        diffs +=
          "  - [VISUAL FORGE] ${target.key} (${target.title}): primary=${rec.primaryHex}, trim=${rec.trimHex}, glow=${rec.glowHex}, style=${rec.geometryStyle}, scale=${rec.pixelScale}x, glowInt=${rec.glowIntensity}x, turb=${rec.shaderTurbulence}x (\"${rec.promptUsed}\")"
      }
    }

    if (bountyPlacement != LabelPlacement.TOP) {
      diffs += "  - bountyPlacement: TOP -> ${bountyPlacement.name}"
    }
    if (tagPlacement != LabelPlacement.BOTTOM) {
      diffs += "  - tagPlacement: BOTTOM -> ${tagPlacement.name}"
    }
    if (namePlacement != LabelPlacement.BOTTOM) {
      diffs += "  - namePlacement: BOTTOM -> ${namePlacement.name}"
    }

    val customBuffs = RiftCodex.getCustomForgedBuffs()
    for (cb in customBuffs) {
      diffs += "  - [NEW FORGED BUFF] ${cb.id} (${cb.officialName} / ${cb.shortLabel}): ${cb.summary}"
    }

    val registryJson = JSONObject()
    for ((k, v) in RiftCodex.BUFF_REGISTRY) {
      registryJson.put(k, v.toJsonObject())
    }

    val hiddenArr = JSONArray()
    hiddenCalibrationKeys.sorted().forEach { hiddenArr.put(it) }

    val totalModified = diffs.size + (if (hiddenCalibrationKeys.isNotEmpty()) 1 else 0)

    val sb = StringBuilder()
    sb.appendLine("Update RIFT's permanent factory defaults in RiftCalibrationState.kt, RiftVisualForgeState.kt, and ShipSpecs.kt to match my forged configuration below:")
    sb.appendLine()
    sb.appendLine("### SECTION A: MODIFIED SETTINGS & VISUAL FORGE DIFF (${diffs.size} Changes)")
    if (diffs.isEmpty()) {
      sb.appendLine("  - (All numeric sliders and visual elements currently match factory defaults)")
    } else {
      diffs.forEach { sb.appendLine(it) }
    }
    if (hiddenCalibrationKeys.isNotEmpty()) {
      sb.appendLine("  - hiddenCalibrationKeys (${hiddenCalibrationKeys.size} hidden): ${hiddenCalibrationKeys.sorted().joinToString(", ")}")
    }
    sb.appendLine()
    sb.appendLine("### SECTION B: COMPLETE REPLACEMENT DEFAULT OBJECTS")
    sb.appendLine("```json")
    val bundle = JSONObject().apply {
      put("DEFAULT_GAME_CONFIG", fullConfigJson)
      put("visualForge", RiftVisualForgeState.toJsonObject())
      put("syndicateCrest", RiftVisualForgeState.activeSyndicateCrestRecipe.toJson())
      put("audioForge", RiftSoundSynthesizer.exportAudioForgeJson())
      put("labelPlacements", JSONObject().apply {
        put("bountyPlacement", bountyPlacement.name)
        put("tagPlacement", tagPlacement.name)
        put("namePlacement", namePlacement.name)
      })
      put("BUFF_REGISTRY", registryJson)
      put("hiddenCalibrationKeys", hiddenArr)
    }
    sb.appendLine(bundle.toString(2))
    sb.appendLine("```")

    return Pair(sb.toString(), totalModified)
  }
}
