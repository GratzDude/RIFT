package com.example.engine

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset

enum class LabelPlacement(val label: String) {
  TOP("TOP"),
  BOTTOM("BOTTOM"),
  LEFT("LEFT"),
  RIGHT("RIGHT"),
  CONSOLIDATED_TOP("CONSOLIDATED TOP"),
  CONSOLIDATED_BOTTOM("CONSOLIDATED BOTTOM"),
}

/**
 * Shared live calibration state for interactive real-time tuning of playfield zoom,
 * entity sizes, label clock placement, and on-screen UI element scales & positions.
 */
object RiftCalibrationState {
  private const val PREFS_NAME = "rift_ui_layout_prefs"

  // 1. Playfield & Entity Sizing Scales
  var cameraZoomScale by mutableFloatStateOf(1.0f)     // Multiplier on base 0.65f camera zoom (0.35x .. 2.2x)
  var shipScale by mutableFloatStateOf(1.0f)           // In-world ship sprite scale (0.5x .. 2.0x)
  var anomalyScale by mutableFloatStateOf(1.0f)        // Spatial anomaly scale (0.3x .. 2.0x)
  var crateScale by mutableFloatStateOf(1.0f)          // Prize crate scale (0.5x .. 2.0x)
  var safeZoneScale by mutableFloatStateOf(1.0f)       // Safe Zone pad & boundary bracket scale (0.5x .. 2.0x)
  var aimTelegraphScale by mutableFloatStateOf(1.0f)   // Turret reticle & aim telegraph cone length (0.5x .. 2.0x)
  var projectileScale by mutableFloatStateOf(1.0f)     // Bullet beam width & bomb/mine radius (0.5x .. 2.0x)
  var fctScale by mutableFloatStateOf(1.0f)            // Floating combat text font size & rise (0.5x .. 2.0x)

  // 2. Ship Label Preset Positioning (Top, Bottom, Left, Right, Consolidated)
  var bountyPlacement by mutableStateOf(LabelPlacement.TOP)
  var tagPlacement by mutableStateOf(LabelPlacement.BOTTOM)
  var namePlacement by mutableStateOf(LabelPlacement.BOTTOM)
  var labelTextScale by mutableFloatStateOf(1.0f)      // Scale multiplier for ship callsign & bounty text (0.6x .. 1.8x)
  var labelDistanceOffset by mutableFloatStateOf(1.0f) // Spacing distance from hull perimeter (0.6x .. 2.2x)

  // 3. HUD Element Sizing Scales
  var topBarScale by mutableFloatStateOf(1.0f)         // Top pill bar scale (0.6x .. 1.8x)
  var buffBarScale by mutableFloatStateOf(1.0f)        // Buff dock scale (0.6x .. 2.0x)
  var killLogScale by mutableFloatStateOf(1.0f)        // Kill log window scale (0.6x .. 1.8x)
  var bottomConsoleScale by mutableFloatStateOf(1.0f)  // Bottom console wings & minimap scale (0.6x .. 1.8x)
  var combatArcScale by mutableFloatStateOf(1.0f)      // Right combat stick/buttons scale (0.6x .. 1.8x)
  var leftStickScale by mutableFloatStateOf(1.0f)      // Left movement stick scale (0.6x .. 1.8x)

  // 4. Testing Feature Flags
  var forceAllButtonsVisible by mutableStateOf(true)   // Display all combat buttons simultaneously for layout testing
  var isUiUnlockedForDragging by mutableStateOf(false) // Toggle allowing direct touch dragging of UI clusters
  var isCalibrationModalOpen by mutableStateOf(false)  // Toggle opening the full-screen calibration lab

  // 5. Draggable Offsets (in pixels) for every independent on-screen HUD element
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
  var toastContainerOffset by mutableStateOf(Offset.Zero)

  // 6. Playfield Buff Spawner & Mode Cap Settings
  var buffCap3v3 by mutableStateOf(12)
  var buffCap5v5 by mutableStateOf(10)
  var buffCap10v10 by mutableStateOf(20)
  var buffCapFfa by mutableStateOf(40)
  var buffModeOverride by mutableStateOf("AUTO") // "AUTO", "3v3", "5v5", "10v10", "FFA", "CUSTOM"
  var buffTargetCountOverride by mutableStateOf<Int?>(null) // null = follow mode cap
  var buffRespawnFrequency by mutableFloatStateOf(1.0f) // 0.0f = Zero Respawn / Disabled, 1.0f = standard, up to 3.0f
  var buffBaseRespawnDelay by mutableFloatStateOf(2.0f) // 0.5s .. 10.0s

  // 7. Anti-Camping Safe Zone Teleport & Playfield AFK Idle Flux Drain Settings
  var safeZoneIdleLimitSec by mutableFloatStateOf(15.0f)        // 3.0s .. 60.0s (default 15.0s)
  var playfieldIdleLimitSec by mutableFloatStateOf(15.0f)       // 3.0s .. 60.0s (default 15.0s)
  var playfieldIdleDrainPctPerSec by mutableFloatStateOf(0.10f) // 0.01f .. 0.50f (default 10%/sec)
  var idleWarningBannerScale by mutableFloatStateOf(1.0f)       // 0.50x .. 1.80x
  var idleWarningBannerOffset by mutableStateOf(Offset.Zero)    // Draggable offset for warning banner

  fun loadPersistedLayout(context: Context) {
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
    toastContainerOffset = readOffset("toastContainer")
    idleWarningBannerOffset = readOffset("idleWarningBanner")
  }

  fun savePersistedLayout(context: Context) {
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
    writeOffset("toastContainer", toastContainerOffset)
    writeOffset("idleWarningBanner", idleWarningBannerOffset)
    editor.apply()
  }

  fun resetAll(context: Context? = null) {
    cameraZoomScale = 1.0f
    shipScale = 1.0f
    anomalyScale = 1.0f
    crateScale = 1.0f
    safeZoneScale = 1.0f
    aimTelegraphScale = 1.0f
    projectileScale = 1.0f
    fctScale = 1.0f

    bountyPlacement = LabelPlacement.TOP
    tagPlacement = LabelPlacement.BOTTOM
    namePlacement = LabelPlacement.BOTTOM
    labelTextScale = 1.0f
    labelDistanceOffset = 1.0f

    topBarScale = 1.0f
    buffBarScale = 1.0f
    killLogScale = 1.0f
    bottomConsoleScale = 1.0f
    combatArcScale = 1.0f
    leftStickScale = 1.0f

    forceAllButtonsVisible = true
    isUiUnlockedForDragging = false

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
    toastContainerOffset = Offset.Zero

    buffCap3v3 = 12
    buffCap5v5 = 10
    buffCap10v10 = 20
    buffCapFfa = 40
    buffModeOverride = "AUTO"
    buffTargetCountOverride = null
    buffRespawnFrequency = 1.0f
    buffBaseRespawnDelay = 2.0f

    safeZoneIdleLimitSec = 15.0f
    playfieldIdleLimitSec = 15.0f
    playfieldIdleDrainPctPerSec = 0.10f
    idleWarningBannerScale = 1.0f
    idleWarningBannerOffset = Offset.Zero

    if (context != null) {
      savePersistedLayout(context)
    }
  }
}

