package com.example.engine

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.RuntimeShader
import android.graphics.Typeface
import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

private data class StarPoint(
  val x: Float,
  val y: Float,
  val size: Float,
  val parallax: Float,
  val color: Int,
)

private data class NebulaBlob(
  val x: Float,
  val y: Float,
  val radius: Float,
  val parallax: Float,
  val isCyan: Boolean,
)

private data class ParallaxMicroRock(
  val x: Float,
  val y: Float,
  val radius: Float,
  val parallax: Float,
  val fillAlpha: Int,
  val rimAlpha: Int,
  val vxOffsets: FloatArray,
  val vyOffsets: FloatArray,
)

private data class UtilityButtonSlot(
  val type: String,
  val title: String,
  val sub: String,
  val cx: Float,
  val cy: Float,
  val r: Float,
  val strokeColor: Int,
  val fillColor: Int,
  val enabled: Boolean,
  val iconBuffType: Int = -1,
)

class RiftArenaView(
  context: Context,
  val engine: RiftGameEngine,
  val onHudTelemetryTick: (fps: Int) -> Unit = {},
  val onGamepadCycleCameraZoom: (deltaSteps: Int) -> Unit = {},
  val onGamepadToggleHangar: () -> Unit = {},
  val onGamepadToggleCalibration: () -> Unit = {},
  val onGamepadOpenNexus: () -> Unit = {},
) : View(context) {

  init {
    isFocusable = true
    isFocusableInTouchMode = true
    requestFocus()
  }

  // [Z-0] & [Z-1] 3-Tier Parallax Background (Tier 1: 200 distant stars drift 0.05, Tier 2: 30 dust motes drift 0.15)
  private val stars: List<StarPoint> =
    List(230) { i ->
      val isDustMote = i >= 200
      StarPoint(
        x = Random.nextFloat() * 2600f,
        y = Random.nextFloat() * 2600f,
        size = if (isDustMote) 2.4f else if (i % 3 == 0) 1.8f else 1.2f,
        parallax = if (isDustMote) 0.15f else 0.05f,
        color =
          if (isDustMote) {
            Color.argb(190, 186, 230, 253)
          } else if (i % 2 == 0) {
            Color.argb(135, 148, 163, 184)
          } else {
            Color.argb(195, 241, 245, 249)
          },
      )
    }

  // Tier 3: Offscreen pre-rendered volumetric nebula radial gradient blobs
  private val nebulaBlobs: List<NebulaBlob> =
    List(8) { i ->
      NebulaBlob(
        x = 350f + (i * 480f) % 2400f,
        y = 420f + (i * 670f) % 2400f,
        radius = 220f + (i % 3) * 75f,
        parallax = 0.25f,
        isCyan = i % 2 == 0,
      )
    }

  // Tier 2b: 28 Deterministic Non-Collidable Deep-Parallax Micro-Asteroid Silhouettes (0.16x & 0.28x planes)
  private val parallaxMicroRocks: List<ParallaxMicroRock> =
    List(28) { idx ->
      val deepPlane = idx < 16
      val pFactor = if (deepPlane) 0.16f else 0.28f
      val baseR = if (deepPlane) 9.5f + (idx % 4) * 3.2f else 14.5f + (idx % 5) * 4.0f
      val vCount = 7 + (idx % 3)
      val vxArr = FloatArray(vCount)
      val vyArr = FloatArray(vCount)
      for (v in 0 until vCount) {
        val ang = (v * (PI * 2.0 / vCount) + idx * 0.43).toFloat()
        val crag = if ((v + idx) % 2 == 0) 0.96f else 0.72f + ((idx + v) % 3) * 0.06f
        vxArr[v] = cos(ang) * baseR * crag
        vyArr[v] = sin(ang) * baseR * crag
      }
      ParallaxMicroRock(
        x = ((idx * 397 + 180) % 2560).toFloat(),
        y = ((idx * 613 + 290) % 2560).toFloat(),
        radius = baseR,
        parallax = pFactor,
        fillAlpha = if (deepPlane) 66 else 84,
        rimAlpha = if (deepPlane) 56 else 82,
        vxOffsets = vxArr,
        vyOffsets = vyArr,
      )
    }

  private var lastFrameNs: Long = System.nanoTime()
  private var fpsFrameCount: Int = 0
  private var fpsAccumSec: Float = 0f
  var currentFps: Int = 60
    private set

  // Camera state (stationary on death)
  private var camX: Float = engine.localPlayer.x
  private var camY: Float = engine.localPlayer.y

  @Volatile private var isWindowVisibleForRender: Boolean = true

  var isModalOverlayOpen: Boolean = false
    set(value) {
      val wasOpen = field
      field = value
      if (value && !RiftCalibrationState.isUiUnlockedForDragging) {
        leftPointerId = MotionEvent.INVALID_POINTER_ID
        firePointerId = MotionEvent.INVALID_POINTER_ID
        bombPointerId = MotionEvent.INVALID_POINTER_ID
        engine.leftStickActive = false
        engine.leftStickMag = 0f
        engine.fireStickActive = false
        engine.bombStickActive = false
      }
      if (
        isWindowVisibleForRender &&
          RiftSoundSynthesizer.isAppCurrentlyForeground() &&
          ((wasOpen && !value) || !value || isHubOverwatchMode || RiftCalibrationState.isUiUnlockedForDragging)
      ) {
        lastFrameNs = System.nanoTime()
        postInvalidateOnAnimation()
      }
    }

  var isHubOverwatchMode: Boolean = false
    set(value) {
      val changed = field != value
      field = value
      if (!value && changed) {
        overwatchZoom = 1.0f
        overwatchPanWorldX = 0f
        overwatchPanWorldY = 0f
      }
      if (isWindowVisibleForRender && RiftSoundSynthesizer.isAppCurrentlyForeground() && (value || changed)) {
        lastFrameNs = System.nanoTime()
        postInvalidateOnAnimation()
      }
    }

  var overwatchCenterX: Float = 0f
  var overwatchCenterY: Float = 0f
  var overwatchRadiusPx: Float = 0f
  var overwatchZoom: Float = 1.0f
    private set
  var overwatchPanWorldX: Float = 0f
    private set
  var overwatchPanWorldY: Float = 0f
    private set
  private var currentRenderWorldScale: Float = 1.0f
  private var overwatchLabelAlphaFactor: Float = 1.0f
  private val overwatchClipPath = Path()

  override fun onWindowVisibilityChanged(visibility: Int) {
    super.onWindowVisibilityChanged(visibility)
    val nowVisible = visibility == View.VISIBLE
    isWindowVisibleForRender = nowVisible
    if (!nowVisible) {
      leftPointerId = MotionEvent.INVALID_POINTER_ID
      firePointerId = MotionEvent.INVALID_POINTER_ID
      bombPointerId = MotionEvent.INVALID_POINTER_ID
      engine.leftStickActive = false
      engine.leftStickMag = 0f
      engine.fireStickActive = false
      engine.bombStickActive = false
    } else if (
      RiftSoundSynthesizer.isAppCurrentlyForeground() &&
        (!isModalOverlayOpen || isHubOverwatchMode || RiftCalibrationState.isUiUnlockedForDragging)
    ) {
      lastFrameNs = System.nanoTime()
      postInvalidateOnAnimation()
    }
  }

  fun onHostForegroundChanged(foreground: Boolean) {
    if (!foreground) {
      leftPointerId = MotionEvent.INVALID_POINTER_ID
      firePointerId = MotionEvent.INVALID_POINTER_ID
      bombPointerId = MotionEvent.INVALID_POINTER_ID
      engine.leftStickActive = false
      engine.leftStickMag = 0f
      engine.fireStickActive = false
      engine.bombStickActive = false
    } else if (
      isWindowVisibleForRender &&
        (!isModalOverlayOpen || isHubOverwatchMode || RiftCalibrationState.isUiUnlockedForDragging)
    ) {
      lastFrameNs = System.nanoTime()
      postInvalidateOnAnimation()
    }
  }

  fun resetOverwatchCamera() {
    overwatchZoom = 1.0f
    overwatchPanWorldX = 0f
    overwatchPanWorldY = 0f
    if (isWindowVisibleForRender && RiftSoundSynthesizer.isAppCurrentlyForeground() && isHubOverwatchMode) {
      postInvalidateOnAnimation()
    }
  }

  fun applyOverwatchTransformGesture(
    centroidScreenX: Float,
    centroidScreenY: Float,
    panDxPx: Float,
    panDyPx: Float,
    zoomChange: Float,
  ): Float {
    val vw = width.toFloat().coerceAtLeast(1f)
    val vh = height.toFloat().coerceAtLeast(1f)
    val effectiveOverwatchR =
      if (overwatchRadiusPx > 24f) overwatchRadiusPx else (min(vw, vh) * 0.36f).coerceAtLeast(120f)
    val pivotX = if (overwatchCenterX > 1f) overwatchCenterX else vw * 0.5f
    val pivotY = if (overwatchCenterY > 1f) overwatchCenterY else vh * 0.5f
    val baseScale =
      ((effectiveOverwatchR * 0.93f) / (engine.arenaRadius + 190f).coerceAtLeast(1200f))
        .coerceAtLeast(0.0001f)

    val oldZoom = overwatchZoom.coerceIn(1.0f, 6.0f)
    val safeZoomFactor = if (zoomChange.isFinite() && zoomChange > 0.01f) zoomChange else 1.0f
    val rawNewZoom = (oldZoom * safeZoomFactor).coerceIn(1.0f, 6.0f)
    val newZoom = if (rawNewZoom < 1.01f && safeZoomFactor <= 1.0f) 1.0f else rawNewZoom

    val oldWorldScale = baseScale * oldZoom
    val newWorldScale = baseScale * newZoom

    val safeCentroidX = if (centroidScreenX.isFinite()) centroidScreenX else pivotX
    val safeCentroidY = if (centroidScreenY.isFinite()) centroidScreenY else pivotY
    val safePanDx = if (panDxPx.isFinite()) panDxPx else 0f
    val safePanDy = if (panDyPx.isFinite()) panDyPx else 0f

    val prevRelX = (safeCentroidX - safePanDx) - pivotX
    val prevRelY = (safeCentroidY - safePanDy) - pivotY
    val curRelX = safeCentroidX - pivotX
    val curRelY = safeCentroidY - pivotY

    val anchorWorldOffsetX = overwatchPanWorldX + (prevRelX / oldWorldScale)
    val anchorWorldOffsetY = overwatchPanWorldY + (prevRelY / oldWorldScale)

    var candidatePanX = anchorWorldOffsetX - (curRelX / newWorldScale)
    var candidatePanY = anchorWorldOffsetY - (curRelY / newWorldScale)

    val zoomProgress = ((newZoom - 1.0f) / 0.35f).coerceIn(0f, 1f)
    val maxPanWorldRadius =
      if (newZoom <= 1.005f) {
        0f
      } else {
        engine.arenaRadius * (1f - 1f / newZoom).coerceAtLeast(0f) + 180f * zoomProgress
      }
    val panDist = hypot(candidatePanX, candidatePanY)
    if (panDist > maxPanWorldRadius && panDist > 0.001f) {
      val clampScale = maxPanWorldRadius / panDist
      candidatePanX *= clampScale
      candidatePanY *= clampScale
    }

    overwatchZoom = newZoom
    overwatchPanWorldX = candidatePanX
    overwatchPanWorldY = candidatePanY

    if (isWindowVisibleForRender && RiftSoundSynthesizer.isAppCurrentlyForeground() && isHubOverwatchMode) {
      postInvalidateOnAnimation()
    }
    return overwatchZoom
  }

  private fun blendColorsInt(c1: Int, c2: Int, frac: Float): Int {
    val t = frac.coerceIn(0f, 1f)
    val inv = 1f - t
    val r = (Color.red(c1) * inv + Color.red(c2) * t).toInt().coerceIn(0, 255)
    val g = (Color.green(c1) * inv + Color.green(c2) * t).toInt().coerceIn(0, 255)
    val b = (Color.blue(c1) * inv + Color.blue(c2) * t).toInt().coerceIn(0, 255)
    return Color.rgb(r, g, b)
  }

  private val ambientDarknessPaint = Paint()
  private val dynamicRimPaint =
    Paint(Paint.ANTI_ALIAS_FLAG).apply {
      style = Paint.Style.STROKE
      strokeCap = Paint.Cap.ROUND
    }

  // Multi-touch pointer tracking
  private var leftPointerId: Int = MotionEvent.INVALID_POINTER_ID
  private var leftOriginX: Float = 0f
  private var leftOriginY: Float = 0f
  private var leftCurX: Float = 0f
  private var leftCurY: Float = 0f

  private var firePointerId: Int = MotionEvent.INVALID_POINTER_ID
  private var fireCurX: Float = 0f
  private var fireCurY: Float = 0f
  private var fireHasExitedDeadzone: Boolean = false

  private var bombPointerId: Int = MotionEvent.INVALID_POINTER_ID
  private var bombCurX: Float = 0f
  private var bombCurY: Float = 0f
  private var bombHasExitedDeadzone: Boolean = false

  // Hardware Gamepad / Controller tracking state
  private var gamepadLeftStickActive: Boolean = false
  private var gamepadRightStickActive: Boolean = false
  private var gamepadRightTriggerHeld: Boolean = false
  private var gamepadLeftTriggerHeld: Boolean = false
  private var lastDpadUpPressed: Boolean = false
  private var lastDpadDownPressed: Boolean = false

  // Pre-allocated Paints (Zero allocations or shadowBlur inside onDraw)
  private val starPaint = Paint().apply { style = Paint.Style.FILL }
  private val nebulaDstRect = RectF()

  private val sectorTickPaint =
    Paint(Paint.ANTI_ALIAS_FLAG).apply {
      style = Paint.Style.STROKE
      strokeWidth = 1.2f
      color = Color.argb(89, 30, 41, 59)
    }

  private val safeFillPaint =
    Paint().apply {
      style = Paint.Style.FILL
      color = Color.argb(18, 16, 185, 129)
    }

  private val safeDashPaint =
    Paint(Paint.ANTI_ALIAS_FLAG).apply {
      style = Paint.Style.STROKE
      strokeWidth = 2f
      color = Color.argb(150, 56, 189, 248)
      pathEffect = DashPathEffect(floatArrayOf(12f, 10f), 0f)
    }

  private val safeBracketPaint =
    Paint(Paint.ANTI_ALIAS_FLAG).apply {
      style = Paint.Style.STROKE
      strokeWidth = 3f
      color = Color.parseColor("#38BDF8")
    }

  private val safeTextPaint =
    Paint(Paint.ANTI_ALIAS_FLAG).apply {
      typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
      textSize = 14f
      textAlign = Paint.Align.CENTER
      color = Color.argb(185, 56, 189, 248)
    }

  // High-Contrast Brushed Gunmetal & Neon Bulkhead Wall Paints
  private val wallBodyPaint =
    Paint(Paint.FILTER_BITMAP_FLAG).apply {
      style = Paint.Style.FILL
      color = Color.parseColor("#1E293B")
      shader = RiftSpriteBaker.wallShader
    }

  private val wallTopBevelPaint =
    Paint(Paint.ANTI_ALIAS_FLAG).apply {
      style = Paint.Style.FILL
      color = Color.parseColor("#64748B")
    }

  private val wallBottomBevelPaint =
    Paint(Paint.ANTI_ALIAS_FLAG).apply {
      style = Paint.Style.FILL
      color = Color.parseColor("#0F172A")
    }

  private val wallHazardPaint =
    Paint(Paint.FILTER_BITMAP_FLAG).apply {
      style = Paint.Style.STROKE
      strokeWidth = 4.5f
      shader = RiftSpriteBaker.hazardShader
    }

  private val wallCyanSeamPaint =
    Paint(Paint.ANTI_ALIAS_FLAG).apply {
      style = Paint.Style.STROKE
      strokeWidth = 3.2f
      color = Color.parseColor("#00F5D4")
    }

  private val wallGoldBracketPaint =
    Paint(Paint.ANTI_ALIAS_FLAG).apply {
      style = Paint.Style.STROKE
      strokeWidth = 3.4f
      strokeCap = Paint.Cap.SQUARE
      color = Color.parseColor("#F59E0B")
    }

  private val wallRivetPaint =
    Paint(Paint.ANTI_ALIAS_FLAG).apply {
      style = Paint.Style.FILL
      color = Color.parseColor("#94A3B8")
    }

  private val phaseEchoPaint =
    Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
      colorFilter =
        android.graphics.PorterDuffColorFilter(
          Color.argb(185, 0, 245, 212),
          android.graphics.PorterDuff.Mode.SRC_ATOP,
        )
    }

  // Pre-allocated 2.5D Perspective-Extruded Space Obstacle & 3D Ship Roll Paints (Zero GC)
  private val obstacleSideDarkPaint =
    Paint(Paint.ANTI_ALIAS_FLAG).apply {
      style = Paint.Style.FILL
      color = Color.parseColor("#0B1324")
    }
  private val obstacleSideLitPaint =
    Paint(Paint.ANTI_ALIAS_FLAG).apply {
      style = Paint.Style.FILL
      color = Color.parseColor("#1E293B")
    }
  private val obstacleCapPaint =
    Paint(Paint.ANTI_ALIAS_FLAG).apply {
      style = Paint.Style.FILL
      color = Color.parseColor("#283548")
    }
  private val obstacleRockPaint =
    Paint(Paint.ANTI_ALIAS_FLAG).apply {
      style = Paint.Style.FILL
      color = Color.parseColor("#334155")
    }
  private val obstacleCrystalPaint =
    Paint(Paint.ANTI_ALIAS_FLAG).apply {
      style = Paint.Style.FILL
      color = Color.parseColor("#A855F7")
    }
  private val obstacleBeaconPaint =
    Paint(Paint.ANTI_ALIAS_FLAG).apply {
      style = Paint.Style.FILL
      color = Color.parseColor("#00F5D4")
    }
  private val perimeterVoidPaint =
    Paint(Paint.ANTI_ALIAS_FLAG).apply {
      style = Paint.Style.STROKE
      strokeWidth = 280f
      color = Color.argb(235, 2, 5, 11)
    }
  private val perimeterStormShaderPaint =
    Paint(Paint.ANTI_ALIAS_FLAG).apply {
      style = Paint.Style.STROKE
      strokeWidth = 460f
    }
  private val perimeterGlowPaint =
    Paint(Paint.ANTI_ALIAS_FLAG).apply {
      style = Paint.Style.STROKE
      strokeWidth = 22f
      color = Color.argb(90, 0, 245, 212)
    }
  private val perimeterRingPaint =
    Paint(Paint.ANTI_ALIAS_FLAG).apply {
      style = Paint.Style.STROKE
      strokeWidth = 4.2f
      color = Color.parseColor("#00F5D4")
    }
  private val safeNexusShaderPaint =
    Paint(Paint.ANTI_ALIAS_FLAG).apply {
      style = Paint.Style.FILL
    }
  private val lightningOuterPaint =
    Paint(Paint.ANTI_ALIAS_FLAG).apply {
      style = Paint.Style.STROKE
      strokeCap = Paint.Cap.ROUND
      strokeJoin = Paint.Join.ROUND
      strokeWidth = 10f
    }
  private val lightningMidPaint =
    Paint(Paint.ANTI_ALIAS_FLAG).apply {
      style = Paint.Style.STROKE
      strokeCap = Paint.Cap.ROUND
      strokeJoin = Paint.Join.ROUND
      strokeWidth = 4.4f
    }
  private val lightningCorePaint =
    Paint(Paint.ANTI_ALIAS_FLAG).apply {
      style = Paint.Style.STROKE
      strokeCap = Paint.Cap.ROUND
      strokeJoin = Paint.Join.ROUND
      strokeWidth = 2.0f
      color = Color.WHITE
    }
  private val lightningPath = Path()

  private val stormBoundaryRuntimeShader: Any? =
    if (Build.VERSION.SDK_INT >= 33) {
      runCatching { RuntimeShader(STORM_BOUNDARY_AGSL) }.getOrNull()
    } else {
      null
    }

  private val safeNexusRuntimeShader: Any? =
    if (Build.VERSION.SDK_INT >= 33) {
      runCatching { RuntimeShader(SAFE_NEXUS_RIFT_AGSL) }.getOrNull()
    } else {
      null
    }
  private val shipShadowPaint =
    Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
      colorFilter =
        android.graphics.PorterDuffColorFilter(
          Color.argb(135, 2, 6, 18),
          android.graphics.PorterDuff.Mode.SRC_IN,
        )
    }
  private val obstacleSpritePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
  private val nebulaPrimaryPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
  private val nebulaSecondaryPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
  private var cachedNebulaTintKey: Int = Int.MIN_VALUE

  private val parallaxMegaArcPaint =
    Paint(Paint.ANTI_ALIAS_FLAG).apply {
      style = Paint.Style.STROKE
      strokeCap = Paint.Cap.ROUND
    }
  private val parallaxMicroRockFillPaint =
    Paint(Paint.ANTI_ALIAS_FLAG).apply {
      style = Paint.Style.FILL
      color = Color.argb(78, 7, 14, 27)
    }
  private val parallaxMicroRockRimPaint =
    Paint(Paint.ANTI_ALIAS_FLAG).apply {
      style = Paint.Style.STROKE
      strokeCap = Paint.Cap.ROUND
      strokeJoin = Paint.Join.ROUND
      strokeWidth = 1.25f
    }

  private val shipBellyTintPaint =
    Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
      colorFilter =
        android.graphics.PorterDuffColorFilter(
          Color.argb(115, 56, 189, 248),
          android.graphics.PorterDuff.Mode.SRC_ATOP,
        )
    }
  private val shipGlintPaint =
    Paint(Paint.ANTI_ALIAS_FLAG).apply {
      style = Paint.Style.STROKE
      strokeCap = Paint.Cap.ROUND
      strokeWidth = 3.2f
      color = Color.argb(185, 224, 242, 254)
    }
  private val vortexRibbonPaint =
    Paint(Paint.ANTI_ALIAS_FLAG).apply {
      style = Paint.Style.STROKE
      strokeCap = Paint.Cap.ROUND
      strokeWidth = 3.4f
    }

  private val shieldRipplePaint =
    Paint(Paint.ANTI_ALIAS_FLAG).apply {
      style = Paint.Style.STROKE
      strokeWidth = 6.0f
      strokeCap = Paint.Cap.ROUND
    }

  private val buffGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG)

  private val turretReticlePaint =
    Paint(Paint.ANTI_ALIAS_FLAG).apply {
      style = Paint.Style.STROKE
      strokeWidth = 3.2f
      color = Color.parseColor("#EF4444")
    }

  private val spreadConePaint =
    Paint(Paint.ANTI_ALIAS_FLAG).apply {
      style = Paint.Style.FILL
      color = Color.argb(72, 56, 189, 248)
    }

  // Dashed Cyan/Amber Trajectory Line
  private val trajectoryLinePaint =
    Paint(Paint.ANTI_ALIAS_FLAG).apply {
      style = Paint.Style.STROKE
      strokeWidth = 3.2f
      color = Color.parseColor("#38BDF8")
      pathEffect = DashPathEffect(floatArrayOf(12f, 6f), 0f)
    }

  private val aimGuidePaint =
    Paint(Paint.ANTI_ALIAS_FLAG).apply {
      style = Paint.Style.STROKE
      strokeWidth = 2.8f
      color = Color.argb(180, 56, 189, 248)
      pathEffect = DashPathEffect(floatArrayOf(12f, 6f), 0f)
    }

  // Split World Ship Label Paints (Bounty above hull, Callsign below hull)
  private val shipLabelStrokePaint =
    Paint(Paint.ANTI_ALIAS_FLAG).apply {
      typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
      style = Paint.Style.STROKE
      strokeWidth = 4.2f
      strokeJoin = Paint.Join.ROUND
      textAlign = Paint.Align.CENTER
      color = Color.argb(235, 3, 7, 18)
    }

  private val shipLabelFillPaint =
    Paint(Paint.ANTI_ALIAS_FLAG).apply {
      typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
      style = Paint.Style.FILL
      textAlign = Paint.Align.CENTER
    }

  private val bulletPaint =
    Paint(Paint.ANTI_ALIAS_FLAG).apply {
      style = Paint.Style.STROKE
      strokeWidth = 6.4f
      strokeCap = Paint.Cap.ROUND
    }

  private val bulletCorePaint =
    Paint(Paint.ANTI_ALIAS_FLAG).apply {
      style = Paint.Style.STROKE
      strokeWidth = 2.8f
      color = Color.WHITE
    }

  private val bombPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
  private val bombRingPaint =
    Paint(Paint.ANTI_ALIAS_FLAG).apply {
      style = Paint.Style.STROKE
      strokeWidth = 3.6f
    }

  private val particlePaint = Paint().apply { style = Paint.Style.FILL }
  private val shockwavePaint =
    Paint(Paint.ANTI_ALIAS_FLAG).apply {
      style = Paint.Style.STROKE
      strokeWidth = 5.0f
    }

  // Spatial Anomaly Rendering Paints (Pre-allocated, Zero GC)
  private val anomalyFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
  private val anomalyRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
  private val anomalyCorePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

  private val popupPaint =
    Paint(Paint.ANTI_ALIAS_FLAG).apply {
      typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
      textSize = 16f
      textAlign = Paint.Align.CENTER
    }

  private val hudCircleFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
  private val hudCircleStrokePaint =
    Paint(Paint.ANTI_ALIAS_FLAG).apply {
      style = Paint.Style.STROKE
      strokeWidth = 2.6f
    }
  private val hudLabelPaint =
    Paint(Paint.ANTI_ALIAS_FLAG).apply {
      typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
      textAlign = Paint.Align.CENTER
    }

  private val respawnOverlayFill =
    Paint().apply {
      style = Paint.Style.FILL
      color = Color.argb(195, 3, 7, 18)
    }
  private val respawnOverlayStroke =
    Paint(Paint.ANTI_ALIAS_FLAG).apply {
      style = Paint.Style.STROKE
      strokeWidth = 3f
      color = Color.parseColor("#EF4444")
    }

  private val shipMatrix = Matrix()
  private val shadowMatrix = Matrix()
  private val arcRect = RectF()
  private val trajPath = Path()
  private val plumePath = Path()
  private val obstaclePath = Path()
  private val activeUtilitySlots = mutableListOf<UtilityButtonSlot>()

  private var dpDensity: Float = resources.displayMetrics.density
  private var cancelDeadzonePx: Float = 32f

  // Dynamic Circular Touch Button Coordinates (calculated per layout size)
  private var leftStickBaseX = 0f
  private var leftStickBaseY = 0f
  private var leftStickRadius = 0f

  private var fireBtnX = 0f
  private var fireBtnY = 0f
  private var fireBtnRadius = 0f

  private var bombBtnX = 0f
  private var bombBtnY = 0f
  private var bombBtnRadius = 0f

  private var utilBtnRadius = 0f
  private var arcDistBomb = 0f
  private var arcDistUtil = 0f

  // UI Unlock drag state for canvas controls
  private var uiDragPointerId: Int = MotionEvent.INVALID_POINTER_ID
  private var uiDraggedControl: String? = null
  private var uiDragLastX: Float = 0f
  private var uiDragLastY: Float = 0f

  private val unlockDashPaint =
    Paint(Paint.ANTI_ALIAS_FLAG).apply {
      style = Paint.Style.STROKE
      strokeWidth = 3.0f
      color = Color.parseColor("#FACC15")
      pathEffect = DashPathEffect(floatArrayOf(10f, 6f), 0f)
    }

  private fun effLeftBaseX(): Float = leftStickBaseX + RiftCalibrationState.leftStickOffset.x
  private fun effLeftBaseY(): Float = leftStickBaseY + RiftCalibrationState.leftStickOffset.y
  private fun effLeftRadius(): Float = leftStickRadius * RiftCalibrationState.leftStickScale

  private fun effFireBtnX(): Float =
    fireBtnX + RiftCalibrationState.combatArcOffset.x + RiftCalibrationState.fireStickOffset.x
  private fun effFireBtnY(): Float =
    fireBtnY + RiftCalibrationState.combatArcOffset.y + RiftCalibrationState.fireStickOffset.y
  private fun effFireRadius(): Float = fireBtnRadius * RiftCalibrationState.combatArcScale

  private fun effBombBtnX(): Float =
    bombBtnX + RiftCalibrationState.combatArcOffset.x + RiftCalibrationState.bombBtnOffset.x
  private fun effBombBtnY(): Float =
    bombBtnY + RiftCalibrationState.combatArcOffset.y + RiftCalibrationState.bombBtnOffset.y
  private fun effBombRadius(): Float = bombBtnRadius * RiftCalibrationState.combatArcScale

  override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
    super.onSizeChanged(w, h, oldw, oldh)
    dpDensity = resources.displayMetrics.density
    cancelDeadzonePx = 32f * dpDensity
    val wf = w.toFloat()
    val hf = h.toFloat()

    // Left Floating Virtual Joystick base anchor (permanently visible at 0.55 idle opacity)
    leftStickRadius = max(42f * dpDensity, min(wf, hf) * 0.115f)
    leftStickBaseX = max(78f * dpDensity, wf * 0.125f)
    leftStickBaseY = hf - max(94f * dpDensity, hf * 0.26f)
    if (leftPointerId == MotionEvent.INVALID_POINTER_ID) {
      leftOriginX = effLeftBaseX()
      leftOriginY = effLeftBaseY()
      leftCurX = effLeftBaseX()
      leftCurY = effLeftBaseY()
    }

    // Primary Right [FIRE] Joystick anchored cleanly in bottom-right corner
    fireBtnRadius = max(40f * dpDensity, min(wf, hf) * 0.098f)
    fireBtnX = wf - max(82f * dpDensity, wf * 0.11f)
    fireBtnY = hf - max(84f * dpDensity, hf * 0.24f)
    if (firePointerId == MotionEvent.INVALID_POINTER_ID) {
      fireCurX = effFireBtnX()
      fireCurY = effFireBtnY()
    }

    bombBtnRadius = max(25f * dpDensity, min(wf, hf) * 0.058f)
    utilBtnRadius = max(21.5f * dpDensity, min(wf, hf) * 0.049f)
    // Ergonomic thumb arc around the Fire stick with ~10dp clean breathing room between buttons
    arcDistBomb = fireBtnRadius + bombBtnRadius + 20f * dpDensity
    arcDistUtil = fireBtnRadius + utilBtnRadius + 24f * dpDensity

    // 188° (Left-Slightly-Up in Canvas coordinates) -> [BOMB / MINE]
    val angle188 = (188.0 * PI / 180.0).toFloat()
    bombBtnX = fireBtnX + cos(angle188) * arcDistBomb
    bombBtnY = fireBtnY + sin(angle188) * arcDistBomb
  }

  /**
   * Computes the fixed-angle muscle-memory utility button slots for the Right-Hand Combat Cluster:
   * - 188°: Secondary Ability / Bomb (handled by bombBtnX/Y)
   * - 227°: Special Crate Attack Button (permanently locked at 227°; never shifts when charges == 0)
   * - 265°: Repel Shockwave Button (permanently locked at 265°; never shifts down to 227°)
   */
  private fun rebuildActiveUtilitySlots(p: ShipEntity) {
    activeUtilitySlots.clear()
    val showAll = RiftCalibrationState.forceAllButtonsVisible || RiftCalibrationState.isUiUnlockedForDragging
    val arcScale = RiftCalibrationState.combatArcScale
    val baseArcX = fireBtnX + RiftCalibrationState.combatArcOffset.x
    val baseArcY = fireBtnY + RiftCalibrationState.combatArcOffset.y
    val scaledDist = arcDistUtil * arcScale
    val scaledR = utilBtnRadius * arcScale

    // Slot 1: Permanently locked at 227.0° -> SPECIAL CRATE ATTACK (or Active Toggle Off for Tractor / Lance)
    val radSpecial = (227.0 * PI / 180.0).toFloat()
    val isToggleActive = p.tractorBeamActive || p.lanceBeamActive
    val hasSpecialCharges = p.specialWeaponCharges > 0 || isToggleActive
    val specialCount = if (p.specialWeaponCharges > 0) p.specialWeaponCharges else if (showAll) 1 else 0
    val activeType =
      when {
        p.tractorBeamActive -> "TRACTOR_BEAM"
        p.lanceBeamActive -> "LANCE_BEAM"
        p.specialWeaponType != "NONE" -> p.specialWeaponType
        else -> "NOVA_RING"
      }
    val reg = RiftCodex.getBuffRegistryEntry(activeType) ?: RiftCodex.BUFF_REGISTRY.getValue("NOVA_RING")
    val stackBadge =
      if (p.specialWeaponStacks > 1 && activeType in setOf("STASIS_BARRIER", "NOVA_RING", "LANCE_BEAM")) {
        "x${p.specialWeaponStacks}"
      } else {
        "[$specialCount]"
      }
    val specialTitle =
      when {
        p.tractorBeamActive -> "TRACTOR ON"
        p.lanceBeamActive -> if (p.specialWeaponStacks > 1) "LANCE x${p.specialWeaponStacks}" else "LANCE ON"
        hasSpecialCharges || showAll -> "${reg.shortLabel} $stackBadge"
        else -> "CRATE [0]"
      }
    val specialSub =
      when {
        isToggleActive -> "TAP OFF"
        activeType in setOf("TRACTOR_BEAM", "LANCE_BEAM") && hasSpecialCharges -> "TOGGLE"
        hasSpecialCharges || showAll -> "0"
        else -> "EMPTY"
      }
    activeUtilitySlots.add(
      UtilityButtonSlot(
        type = "SPECIAL",
        title = specialTitle,
        sub = specialSub,
        cx = baseArcX + cos(radSpecial) * scaledDist + RiftCalibrationState.burstBtnOffset.x,
        cy = baseArcY + sin(radSpecial) * scaledDist + RiftCalibrationState.burstBtnOffset.y,
        r = scaledR,
        strokeColor =
          when {
            isToggleActive -> Color.parseColor("#00F5D4")
            hasSpecialCharges || showAll -> reg.strokeColorInt
            else -> Color.parseColor("#475569")
          },
        fillColor =
          when {
            isToggleActive -> Color.argb(195, 6, 78, 59)
            hasSpecialCharges || showAll -> reg.fillColorArgb
            else -> Color.argb(120, 15, 23, 42)
          },
        enabled = isToggleActive || (p.canFire && p.specialWeaponCharges > 0),
        iconBuffType = reg.buffTypeIndex.coerceAtLeast(0),
      )
    )

    // Slot 2: Permanently locked at 265.0° -> REPEL SHOCKWAVE
    val radRepel = (265.0 * PI / 180.0).toFloat()
    val repelCount = if (p.repels > 0) p.repels else if (showAll) 3 else 0
    val rCost = RiftCalibrationState.repelCost
    activeUtilitySlots.add(
      UtilityButtonSlot(
        type = "REPEL",
        title = "REPEL [$repelCount]",
        sub = "${rCost.toInt()}",
        cx = baseArcX + cos(radRepel) * scaledDist + RiftCalibrationState.repelBtnOffset.x,
        cy = baseArcY + sin(radRepel) * scaledDist + RiftCalibrationState.repelBtnOffset.y,
        r = scaledR,
        strokeColor = Color.parseColor("#38BDF8"),
        fillColor = Color.argb(165, 10, 48, 82),
        enabled = p.canFire && p.repels > 0 && (p.flux - rCost >= 1f),
        iconBuffType = 7, // Codex / Playfield Repel Charge icon
      )
    )
  }

  override fun onDraw(canvas: Canvas) {
    super.onDraw(canvas)

    if (!isWindowVisibleForRender || !RiftSoundSynthesizer.isAppCurrentlyForeground()) {
      return
    }

    val vw = width.toFloat()
    val vh = height.toFloat()

    // Native Landscape Enforcement Guard
    if (vh > vw) {
      canvas.drawColor(Color.parseColor("#030712"))
      hudLabelPaint.textSize = 18f * dpDensity
      hudLabelPaint.color = Color.parseColor("#38BDF8")
      canvas.drawText("PLEASE ROTATE DEVICE TO LANDSCAPE", vw * 0.5f, vh * 0.48f, hudLabelPaint)
      hudLabelPaint.textSize = 12f * dpDensity
      hudLabelPaint.color = Color.parseColor("#94A3B8")
      canvas.drawText("RIFT WIDESCREEN ARENA REQUIRES LANDSCAPE VIEWPORT", vw * 0.5f, vh * 0.54f, hudLabelPaint)
      postInvalidateOnAnimation()
      return
    }

    if (isModalOverlayOpen && !isHubOverwatchMode && !RiftCalibrationState.isUiUnlockedForDragging) {
      canvas.drawColor(Color.parseColor("#02050B"))
      return
    }

    // 1. Delta-time step
    val nowNs = System.nanoTime()
    val dt = ((nowNs - lastFrameNs) / 1_000_000_000f).coerceIn(0.001f, 0.033f)
    lastFrameNs = nowNs

    fpsFrameCount++
    fpsAccumSec += dt
    if (fpsAccumSec >= 0.35f) {
      currentFps = ((fpsFrameCount / fpsAccumSec).toInt()).coerceIn(30, 120)
      fpsFrameCount = 0
      fpsAccumSec = 0f
      onHudTelemetryTick(currentFps)
    }

    engine.stepSimulation(dt)

    val p = engine.localPlayer
    rebuildActiveUtilitySlots(p)

    // Stationary Death Camera or Interactive Living-Teammate Spectator Smooth Follow Camera
    if (p.respawnTimer > 0f) {
      val specTarget =
        if (engine.isActivelySpectatingTeammates()) {
          engine.getLivingSpectatableTeammates().firstOrNull { it.id == engine.spectatingTargetShipId }
            ?: engine.getLivingSpectatableTeammates().firstOrNull()
        } else {
          null
        }
      if (specTarget != null) {
        val followLerp = (dt * 12f).coerceIn(0.08f, 1f)
        if (hypot(specTarget.x - camX, specTarget.y - camY) > 1800f) {
          camX = specTarget.x
          camY = specTarget.y
        } else {
          camX += (specTarget.x - camX) * followLerp
          camY += (specTarget.y - camY) * followLerp
        }
      } else {
        camX = p.deathX
        camY = p.deathY
      }
    } else {
      camX = p.x
      camY = p.y
    }

    val tSec = engine.traumaTimeSec
    val traumaSq = if (isHubOverwatchMode) 0f else engine.cameraTrauma * engine.cameraTrauma
    val shakeOffsetX = if (traumaSq > 0.0001f) traumaSq * 14f * sin(tSec * 47.0f) else 0f
    val shakeOffsetY = if (traumaSq > 0.0001f) traumaSq * 14f * cos(tSec * 53.0f) else 0f
    val shakeAngleDeg =
      if (traumaSq > 0.0001f) (traumaSq * 0.035f * sin(tSec * 39.0f)) * (180f / PI.toFloat()) else 0f

    val effectiveOverwatchR =
      if (overwatchRadiusPx > 24f) overwatchRadiusPx else (min(vw, vh) * 0.36f).coerceAtLeast(120f)
    val pivotScreenX = if (isHubOverwatchMode && overwatchCenterX > 1f) overwatchCenterX else vw * 0.5f
    val pivotScreenY = if (isHubOverwatchMode && overwatchCenterY > 1f) overwatchCenterY else vh * 0.5f
    val activeOverwatchZoom = overwatchZoom.coerceIn(1.0f, 6.0f)
    val worldScale =
      if (isHubOverwatchMode) {
        val baseOverwatchScale =
          (effectiveOverwatchR * 0.93f) / (engine.arenaRadius + 190f).coerceAtLeast(1200f)
        baseOverwatchScale * activeOverwatchZoom
      } else {
        engine.effectiveCameraScale()
      }
    currentRenderWorldScale = worldScale
    overwatchLabelAlphaFactor =
      if (!isHubOverwatchMode) {
        1.0f
      } else {
        ((activeOverwatchZoom - 1.55f) / 0.45f).coerceIn(0f, 1f)
      }

    val effectiveCamX =
      if (isHubOverwatchMode) engine.arenaCenterX + overwatchPanWorldX else camX + shakeOffsetX
    val effectiveCamY =
      if (isHubOverwatchMode) engine.arenaCenterY + overwatchPanWorldY else camY + shakeOffsetY

    val clippedToOverwatchCircle = isHubOverwatchMode
    if (clippedToOverwatchCircle) {
      canvas.drawColor(Color.parseColor("#02050B"))
      canvas.save()
      overwatchClipPath.reset()
      overwatchClipPath.addCircle(pivotScreenX, pivotScreenY, effectiveOverwatchR, Path.Direction.CW)
      canvas.clipPath(overwatchClipPath)
    }

    // [Z-0] Obsidian Void (#030712) + [Z-1] Multi-Plane Mode-Tinted Deep-Space Parallax Backdrop (+ Smooth 1v1 Abyssal-Garden Blend)
    val abyssalBlend = if (isHubOverwatchMode) 0f else engine.skirmishAbyssalBlend.coerceIn(0f, 1f)
    val nebRec = RiftVisualForgeState.getRecipe(ForgeElementTarget.BACKGROUND_NEBULA)
    val rawVoidColor =
      if (!nebRec.isFactoryDefault) {
        nebRec.primaryColorInt
      } else {
        Color.parseColor("#030712")
      }
    val baseVoidColor =
      if (abyssalBlend > 0.005f) {
        Color.rgb(
          (Color.red(rawVoidColor) + (2 - Color.red(rawVoidColor)) * abyssalBlend).roundToInt().coerceIn(0, 255),
          (Color.green(rawVoidColor) + (18 - Color.green(rawVoidColor)) * abyssalBlend).roundToInt().coerceIn(0, 255),
          (Color.blue(rawVoidColor) + (29 - Color.blue(rawVoidColor)) * abyssalBlend).roundToInt().coerceIn(0, 255),
        )
      } else {
        rawVoidColor
      }
    canvas.drawColor(baseVoidColor)

    // Mode-tinted Volumetric Nebula Stamps (smoothly cross-blending into Bioluminescent Abyssal Garden during 1v1 Deathmatch)
    val abyssalQuant = (abyssalBlend * 40f).roundToInt()
    val nebTintKey =
      (engine.activeRankedMode.ordinal shl 20) xor
        (if (nebRec.isFactoryDefault) 0 else (nebRec.trimColorInt xor (nebRec.glowColorInt shl 3))) xor
        ((nebRec.surfaceOpacity * 100f).roundToInt() shl 6) xor
        abyssalQuant
    if (nebTintKey != cachedNebulaTintKey) {
      cachedNebulaTintKey = nebTintKey
      val (rawTintA, rawTintB) =
        if (!nebRec.isFactoryDefault) {
          nebRec.trimColorInt to nebRec.glowColorInt
        } else {
          when (engine.activeRankedMode) {
            RankedGameMode.SECTOR_CONVERGENCE -> Color.rgb(14, 116, 144) to Color.rgb(49, 46, 129)
            RankedGameMode.RIFT_SKIRMISH -> Color.rgb(56, 189, 248) to Color.rgb(30, 64, 175)
            RankedGameMode.FLUX_CORE -> Color.rgb(217, 119, 6) to Color.rgb(153, 27, 27)
            RankedGameMode.STASIS_BREACH,
            RankedGameMode.SYNDICATE_WARS -> Color.rgb(147, 51, 234) to Color.rgb(88, 28, 135)
            RankedGameMode.RIFT_CONTROL -> Color.rgb(13, 148, 136) to Color.rgb(180, 83, 9)
          }
        }
      val tintA =
        if (abyssalBlend > 0.01f) {
          Color.rgb(
            (Color.red(rawTintA) + (0 - Color.red(rawTintA)) * abyssalBlend).roundToInt().coerceIn(0, 255),
            (Color.green(rawTintA) + (245 - Color.green(rawTintA)) * abyssalBlend).roundToInt().coerceIn(0, 255),
            (Color.blue(rawTintA) + (196 - Color.blue(rawTintA)) * abyssalBlend).roundToInt().coerceIn(0, 255),
          )
        } else {
          rawTintA
        }
      val tintB =
        if (abyssalBlend > 0.01f) {
          Color.rgb(
            (Color.red(rawTintB) + (16 - Color.red(rawTintB)) * abyssalBlend).roundToInt().coerceIn(0, 255),
            (Color.green(rawTintB) + (142 - Color.green(rawTintB)) * abyssalBlend).roundToInt().coerceIn(0, 255),
            (Color.blue(rawTintB) + (178 - Color.blue(rawTintB)) * abyssalBlend).roundToInt().coerceIn(0, 255),
          )
        } else {
          rawTintB
        }
      val nebAlpha =
        ((235f + 20f * abyssalBlend) * nebRec.surfaceOpacity.coerceIn(0.25f, 1.0f)).roundToInt().coerceIn(60, 255)
      nebulaPrimaryPaint.alpha = nebAlpha
      nebulaSecondaryPaint.alpha = nebAlpha
      nebulaPrimaryPaint.colorFilter =
        if (abyssalBlend <= 0.01f && nebRec.isFactoryDefault && engine.activeRankedMode == RankedGameMode.SECTOR_CONVERGENCE) {
          null
        } else {
          android.graphics.PorterDuffColorFilter(
            Color.argb(180, Color.red(tintA), Color.green(tintA), Color.blue(tintA)),
            android.graphics.PorterDuff.Mode.SRC_ATOP,
          )
        }
      nebulaSecondaryPaint.colorFilter =
        if (abyssalBlend <= 0.01f && nebRec.isFactoryDefault && engine.activeRankedMode == RankedGameMode.SECTOR_CONVERGENCE) {
          null
        } else {
          android.graphics.PorterDuffColorFilter(
            Color.argb(180, Color.red(tintB), Color.green(tintB), Color.blue(tintB)),
            android.graphics.PorterDuff.Mode.SRC_ATOP,
          )
        }
    }

    for (nb in nebulaBlobs) {
      val nx = ((nb.x - effectiveCamX * nb.parallax) % vw + vw) % vw
      val ny = ((nb.y - effectiveCamY * nb.parallax) % vh + vh) % vh
      val scaledR = nb.radius * nebRec.pixelScale.coerceIn(0.65f, 1.45f) * (1f + 0.14f * abyssalBlend)
      nebulaDstRect.set(nx - scaledR, ny - scaledR, nx + scaledR, ny + scaledR)
      if (nb.isCyan) {
        canvas.drawBitmap(RiftSpriteBaker.nebulaCyanStamp, null, nebulaDstRect, nebulaPrimaryPaint)
      } else {
        canvas.drawBitmap(RiftSpriteBaker.nebulaIndigoStamp, null, nebulaDstRect, nebulaSecondaryPaint)
      }
    }

    // 1v1 Abyssal-Garden Bioluminescent Spore Motes & Harmonic Canopy Ripples (smoothly faded by abyssalBlend)
    if (abyssalBlend > 0.01f) {
      val gardenAlpha = (abyssalBlend * 110f).roundToInt().coerceIn(0, 130)
      parallaxMegaArcPaint.strokeWidth = 1.8f
      for (ringIdx in 0 until 4) {
        val gPhase = (tSec * 0.22f + ringIdx * 0.25f) % 1.0f
        val gRad = min(vw, vh) * (0.18f + gPhase * 0.62f)
        val ringFade = sin(gPhase * PI.toFloat()).coerceIn(0f, 1f)
        parallaxMegaArcPaint.color =
          if (ringIdx % 2 == 0) {
            Color.argb((gardenAlpha * ringFade * 0.65f).roundToInt().coerceIn(0, 95), 0, 245, 212)
          } else {
            Color.argb((gardenAlpha * ringFade * 0.55f).roundToInt().coerceIn(0, 85), 52, 211, 153)
          }
        canvas.drawCircle(vw * 0.5f, vh * 0.5f, gRad, parallaxMegaArcPaint)
      }
      for (spIdx in 0 until 22) {
        val baseX = ((spIdx * 317f + tSec * (12f + (spIdx % 5) * 3f) - effectiveCamX * 0.11f) % vw + vw) % vw
        val baseY =
          ((spIdx * 193f + sin(tSec * 0.9f + spIdx) * 26f - effectiveCamY * 0.11f) % vh + vh) % vh
        val sporePulse = 0.5f + 0.5f * sin(tSec * 2.4f + spIdx * 1.3f)
        val spAlpha = (abyssalBlend * (65f + 115f * sporePulse)).roundToInt().coerceIn(0, 200)
        starPaint.color =
          when (spIdx % 3) {
            0 -> Color.argb(spAlpha, 0, 245, 212)
            1 -> Color.argb(spAlpha, 52, 211, 153)
            else -> Color.argb(spAlpha, 167, 139, 250)
          }
        canvas.drawCircle(baseX, baseY, 2.2f + 1.8f * sporePulse, starPaint)
      }
    }

    // Tier 1 & Tier 2: 200 Distant Stars (drift 0.05) + 30 Dust Motes (drift 0.15)
    for (st in stars) {
      val sx = ((st.x - effectiveCamX * st.parallax) % vw + vw) % vw
      val sy = ((st.y - effectiveCamY * st.parallax) % vh + vh) % vh
      starPaint.color = st.color
      canvas.drawRect(sx, sy, sx + st.size, sy + st.size, starPaint)
    }

    // Tier 1b: Colossal Distant Curved Orbital Ring Megastructure Arc (0.08x parallax, faint deep-field silhouette)
    val megaCenterX = vw * 0.64f - ((effectiveCamX - engine.arenaCenterX) * 0.08f).coerceIn(-vw * 0.42f, vw * 0.42f)
    val megaCenterY = vh * 1.38f - ((effectiveCamY - engine.arenaCenterY) * 0.08f).coerceIn(-vh * 0.36f, vh * 0.36f)
    val megaR = max(vw, vh) * 1.18f
    arcRect.set(megaCenterX - megaR, megaCenterY - megaR, megaCenterX + megaR, megaCenterY + megaR)
    parallaxMegaArcPaint.strokeWidth = (max(vw, vh) * 0.022f).coerceIn(14f, 34f)
    parallaxMegaArcPaint.color = Color.argb(42, 15, 23, 42)
    canvas.drawArc(arcRect, 208f, 96f, false, parallaxMegaArcPaint)
    parallaxMegaArcPaint.strokeWidth = 2.0f
    parallaxMegaArcPaint.color = Color.argb(54, 56, 189, 248)
    canvas.drawArc(arcRect, 210f, 92f, false, parallaxMegaArcPaint)
    // Tiny lit distant superstructure rib ticks along the megastructure arc
    parallaxMegaArcPaint.strokeWidth = 1.4f
    for (tIdx in 0 until 11) {
      val deg = 214f + tIdx * 8.4f
      val rad = deg * (PI.toFloat() / 180f)
      val rIn = megaR - 8f
      val rOut = megaR + 8f
      parallaxMegaArcPaint.color = if (tIdx % 3 == 0) Color.argb(62, 0, 245, 212) else Color.argb(44, 148, 163, 184)
      canvas.drawLine(
        megaCenterX + cos(rad) * rIn,
        megaCenterY + sin(rad) * rIn,
        megaCenterX + cos(rad) * rOut,
        megaCenterY + sin(rad) * rOut,
        parallaxMegaArcPaint,
      )
    }

    // Tier 2b: 28 Non-Collidable Deep-Parallax Micro-Asteroid Silhouettes (0.16x & 0.28x planes, alpha <= 88)
    for (mr in parallaxMicroRocks) {
      val mx = ((mr.x - effectiveCamX * mr.parallax) % vw + vw) % vw
      val my = ((mr.y - effectiveCamY * mr.parallax) % vh + vh) % vh
      obstaclePath.reset()
      val nV = mr.vxOffsets.size
      for (vIdx in 0 until nV) {
        val px = mx + mr.vxOffsets[vIdx]
        val py = my + mr.vyOffsets[vIdx]
        if (vIdx == 0) obstaclePath.moveTo(px, py) else obstaclePath.lineTo(px, py)
      }
      obstaclePath.close()
      parallaxMicroRockFillPaint.color = Color.argb(mr.fillAlpha, 7, 14, 27)
      canvas.drawPath(obstaclePath, parallaxMicroRockFillPaint)

      // Subtle upper-left starlight rim highlight only (clearly non-collidable background depth)
      parallaxMicroRockRimPaint.color = Color.argb(mr.rimAlpha, 100, 116, 139)
      for (vIdx in 0 until nV) {
        val x0 = mr.vxOffsets[vIdx]
        val y0 = mr.vyOffsets[vIdx]
        val x1 = mr.vxOffsets[(vIdx + 1) % nV]
        val y1 = mr.vyOffsets[(vIdx + 1) % nV]
        if ((x0 + x1) <= 0f || (y0 + y1) <= 0f) {
          canvas.drawLine(mx + x0, my + y0, mx + x1, my + y1, parallaxMicroRockRimPaint)
        }
      }
    }

    // Optional ambient space darkness contrast veil from NEXUS FORGE lighting studio
    val ambientDark = RiftVisualForgeState.ambientSpaceDarkness.coerceIn(0f, 0.85f)
    if (ambientDark > 0.01f) {
      ambientDarknessPaint.color = Color.argb((ambientDark * 220f).toInt().coerceIn(0, 220), 2, 4, 10)
      canvas.drawRect(0f, 0f, vw, vh, ambientDarknessPaint)
    }

    // 3. World-Space Camera Transform (Zoomable/Pannable Orbital Overwatch when isHubOverwatchMode, else Pilot Camera)
    canvas.save()
    canvas.translate(pivotScreenX, pivotScreenY)
    if (shakeAngleDeg != 0f) {
      canvas.rotate(shakeAngleDeg)
    }
    canvas.scale(worldScale, worldScale)
    canvas.translate(-effectiveCamX, -effectiveCamY)

    val safeWorldScale = worldScale.coerceAtLeast(0.0001f)
    val halfWorldW =
      if (isHubOverwatchMode) (effectiveOverwatchR / safeWorldScale) + 380f else (vw / safeWorldScale) * 0.56f
    val halfWorldH =
      if (isHubOverwatchMode) (effectiveOverwatchR / safeWorldScale) + 380f else (vh / safeWorldScale) * 0.56f
    val viewLeft = effectiveCamX - halfWorldW
    val viewRight = effectiveCamX + halfWorldW
    val viewTop = effectiveCamY - halfWorldH
    val viewBottom = effectiveCamY + halfWorldH

    // Subtle Tactical Sector Coordinate Ticks (rgba(30, 41, 59, 0.35))
    val firstTickX = ((viewLeft / 500f).toInt()) * 500f
    val firstTickY = ((viewTop / 500f).toInt()) * 500f
    var tx = firstTickX
    while (tx <= viewRight) {
      var ty = firstTickY
      while (ty <= viewBottom) {
        canvas.drawLine(tx - 6f, ty, tx + 6f, ty, sectorTickPaint)
        canvas.drawLine(tx, ty - 6f, tx, ty + 6f, sectorTickPaint)
        ty += 500f
      }
      tx += 500f
    }

    // 4. Wall-less Circular 3D Safe-Zone Rift Aperture Looking Down into the Nexus Hub (active ONLY in SECTOR_CONVERGENCE & FLUX_CORE)
    if (engine.hasActiveSafeZone()) {
      drawCircularNexusSafeRift(
        canvas = canvas,
        p = p,
        worldScale = worldScale,
        tSec = tSec,
        viewLeft = viewLeft,
        viewRight = viewRight,
        viewTop = viewTop,
        viewBottom = viewBottom,
      )
    }

    // 4b. STASIS_BREACH (10v10 Jailbreak): Vertical Sector Divider (x = 4000), Circular Stasis Jails & 8,000 Flux Reactor Cores
    if (engine.isStasisBreachRulesActive()) {
      drawStasisBreachPlayfieldStructures(canvas, p, viewLeft, viewRight, viewTop, viewBottom)
    }

    // 4b-2. RIFT_CONTROL: 2-Rift Tug-of-War Capture Zones (Rift Alpha & Rift Omega)
    if (engine.isRiftControlRulesActive()) {
      drawRiftControlZones(canvas, p, viewLeft, viewRight, viewTop, viewBottom)
    }

    // 4c. FLUX_CORE: Free/Ejected Singularity Orb & Supersonic Velocity Trail
    if (engine.isFluxCoreRulesActive() && engine.fluxCoreCarrierEntityId.isEmpty()) {
      drawFreeFluxCoreOrb(canvas, viewLeft, viewRight, viewTop, viewBottom)
    }

    // 5. Circular Dynamic-Radius Containment Perimeter + 2.5D Perspective-Extruded Space Obstacles
    drawCircularContainmentPerimeter(canvas, effectiveCamX, effectiveCamY, viewLeft, viewRight, viewTop, viewBottom)
    for (w in engine.walls) {
      if (w.x + w.w < viewLeft - 48f || w.x > viewRight + 48f || w.y + w.h < viewTop - 48f || w.y > viewBottom + 48f) {
        continue
      }
      drawSpaceObstacle3D(canvas, w, effectiveCamX, effectiveCamY)
    }

    // 6. Contained Smooth-Beveled 3D Supply Pods + Calm Radial Powerup Aura + Crisp Non-Spiky Buff Icons
    val crateRec = RiftVisualForgeState.getRecipe(ForgeElementTarget.SUPPLY_CRATE)
    val buffDrawSize = 88f * 0.89f * RiftCalibrationState.crateScale * crateRec.pixelScale.coerceIn(0.70f, 1.35f)
    val buffHalf = buffDrawSize * 0.5f
    val podBmp = RiftSpriteBaker.supplyPodFrameBitmap
    val podHalf = buffHalf * 1.10f
    val iconHalf = buffHalf * 0.78f
    val podBmpHalfSrc = podBmp.width * 0.5f
    for (g in engine.greens) {
      g.spinPhase += dt * 1.85f
      if (g.x < viewLeft - 95f || g.x > viewRight + 95f || g.y < viewTop - 95f || g.y > viewBottom + 95f) {
        continue
      }
      val bobOffsetY = sin(g.spinPhase) * 2.4f
      val gy = g.y + bobOffsetY
      val pulse = 0.5f + 0.5f * sin(g.spinPhase * 1.25f)

      // Calm, smooth radial emerald-cyan powerup halo
      val glowRadius = podHalf * (1.02f + 0.08f * pulse)
      buffGlowPaint.alpha =
        ((56f + 26f * pulse) * crateRec.glowIntensity.coerceIn(0.2f, 1.8f)).roundToInt().coerceIn(24, 128)
      nebulaDstRect.set(
        g.x - glowRadius,
        gy - glowRadius,
        g.x + glowRadius,
        gy + glowRadius,
      )
      canvas.drawBitmap(RiftSpriteBaker.emeraldGlowStamp, null, nebulaDstRect, buffGlowPaint)

      // Smoothly Rotating Beveled Gunmetal/Emerald Containment Pod Frame
      val podScale = (podHalf / podBmpHalfSrc.coerceAtLeast(1f))
      shipMatrix.reset()
      shipMatrix.postTranslate(-podBmpHalfSrc, -podBmpHalfSrc)
      shipMatrix.postRotate((g.spinPhase * 22f) % 360f)
      shipMatrix.postScale(podScale, podScale)
      shipMatrix.postTranslate(g.x, gy)
      obstacleSpritePaint.alpha = (255f * crateRec.surfaceOpacity.coerceIn(0.35f, 1.0f)).roundToInt().coerceIn(90, 255)
      canvas.drawBitmap(podBmp, shipMatrix, obstacleSpritePaint)

      // Crisp Centered Rounded Buff Icon inside the Supply Pod
      val buffBmp = RiftSpriteBaker.getBuffIconBitmap(g.buffType)
      nebulaDstRect.set(
        g.x - iconHalf,
        gy - iconHalf,
        g.x + iconHalf,
        gy + iconHalf,
      )
      canvas.drawBitmap(buffBmp, null, nebulaDstRect, null)
    }
    obstacleSpritePaint.alpha = 255

    // 6b. 4-Layer 3D Playfield Rift Obstacles + Multicolored Unstable Merged Rift (10.0s Collapse)
    for (anom in engine.anomalies) {
      val r = anom.physicalRadius * RiftCalibrationState.anomalyScale
      val cullMargin = if (anom.isUnstableMergedRift) r * 2.35f else r * 1.65f
      if (anom.x + cullMargin < viewLeft || anom.x - cullMargin > viewRight || anom.y + cullMargin < viewTop || anom.y - cullMargin > viewBottom) {
        continue
      }
      val safeScale = worldScale.coerceAtLeast(0.15f)
      if (anom.isUnstableMergedRift) {
        val lifeFrac = (anom.life / anom.maxLife.coerceAtLeast(1f)).coerceIn(0f, 1f)
        if (anom.life <= 4.0f) {
          val warnPulse = 0.5f + 0.5f * sin(tSec * 14f)
          safeDashPaint.color = Color.argb((95 + (95 * warnPulse).toInt()).coerceIn(60, 210), 250, 204, 21)
          safeDashPaint.strokeWidth = 3.0f
          canvas.drawCircle(anom.x, anom.y, anom.physicalRadius * 2.15f, safeDashPaint)
          safeDashPaint.strokeWidth = 2f
        }
        RiftSpriteBaker.drawUnstableMergedRiftOnCanvas(
          canvas = canvas,
          cx = anom.x,
          cy = anom.y,
          r = r,
          spinAngle = anom.spinAngle,
          fadeAlpha = 1.0f,
        )
        val ringR = r * 1.08f
        arcRect.set(anom.x - ringR, anom.y - ringR, anom.x + ringR, anom.y + ringR)
        anomalyRingPaint.strokeWidth = 5.4f
        anomalyRingPaint.color = if (anom.life <= 3.0f) Color.parseColor("#EF4444") else Color.parseColor("#FACC15")
        canvas.drawArc(arcRect, -90f, lifeFrac * 360f, false, anomalyRingPaint)

        drawWorldOverheadBanner(
          canvas = canvas,
          text = String.format(java.util.Locale.US, "UNSTABLE RIFT • %.1fs // COLLAPSE 75%% FLUX", max(0f, anom.life)),
          worldX = anom.x,
          worldY = anom.y - ringR - (20f / safeScale),
          fillColor = if (anom.life <= 3.0f) Color.parseColor("#FB7185") else Color.parseColor("#FACC15"),
          worldScale = worldScale,
          sizeMultiplier = 1.24f,
          alpha = 255,
        )
        continue
      }

      val fadeAlpha = (anom.life / 2.0f).coerceIn(0f, 1f)
      val baseAlphaInt = (255 * fadeAlpha).toInt().coerceIn(0, 255)

      when (anom.type) {
        AnomalyType.PLASMA_RIFT -> {
          RiftSpriteBaker.draw3dPlayfieldRift(
            canvas = canvas,
            type = AnomalyType.PLASMA_RIFT,
            cx = anom.x,
            cy = anom.y,
            radius = r,
            spinAngle = anom.spinAngle,
            fadeAlpha = fadeAlpha,
          )
          drawWorldOverheadBanner(
            canvas = canvas,
            text = "PLASMA RIFT • ${anom.life.toInt()}s",
            worldX = anom.x,
            worldY = anom.y - r - 18f,
            fillColor = Color.parseColor("#FB7185"),
            worldScale = worldScale,
            sizeMultiplier = 1.15f,
            alpha = baseAlphaInt,
          )
        }

        AnomalyType.REPULSOR_RIFT -> {
          RiftSpriteBaker.draw3dPlayfieldRift(
            canvas = canvas,
            type = AnomalyType.REPULSOR_RIFT,
            cx = anom.x,
            cy = anom.y,
            radius = r,
            spinAngle = anom.spinAngle,
            fadeAlpha = fadeAlpha,
          )
          drawWorldOverheadBanner(
            canvas = canvas,
            text = "REPULSOR RIFT • ${anom.life.toInt()}s",
            worldX = anom.x,
            worldY = anom.y - r - 18f,
            fillColor = Color.parseColor("#38BDF8"),
            worldScale = worldScale,
            sizeMultiplier = 1.15f,
            alpha = baseAlphaInt,
          )
        }

        AnomalyType.GRAVITY_RIFT -> {
          RiftSpriteBaker.draw3dPlayfieldRift(
            canvas = canvas,
            type = AnomalyType.GRAVITY_RIFT,
            cx = anom.x,
            cy = anom.y,
            radius = r,
            spinAngle = anom.spinAngle,
            absorbedCount = anom.absorbedWeaponCount,
            fadeAlpha = fadeAlpha,
          )
          val baseOffset = 18f / safeScale
          drawWorldOverheadBanner(
            canvas = canvas,
            text = "GRAVITY RIFT • ${anom.life.toInt()}s",
            worldX = anom.x,
            worldY = anom.y - r - baseOffset,
            fillColor = Color.parseColor("#C084FC"),
            worldScale = worldScale,
            sizeMultiplier = 1.15f,
            alpha = baseAlphaInt,
          )
          if (anom.absorbedWeaponCount > 0) {
            val growthPct = ((anom.physicalRadius / RiftCalibrationState.riftPhysicalRadius.coerceAtLeast(120f)) * 100f).toInt()
            val maxTag = if (anom.physicalRadius >= 395f) " // MAX PULL" else ""
            drawWorldOverheadBanner(
              canvas = canvas,
              text = "MASS ${growthPct}% (+${anom.absorbedWeaponCount} ABSORBED)$maxTag",
              worldX = anom.x,
              worldY = anom.y - r - baseOffset - (22f / safeScale),
              fillColor = Color.parseColor("#E9D5FF"),
              worldScale = worldScale,
              sizeMultiplier = 1.05f,
              alpha = baseAlphaInt,
            )
          }
        }

        AnomalyType.PHASE_RIFT_A, AnomalyType.PHASE_RIFT_B -> {
          val isNodeA = anom.type == AnomalyType.PHASE_RIFT_A
          RiftSpriteBaker.draw3dPlayfieldRift(
            canvas = canvas,
            type = anom.type,
            cx = anom.x,
            cy = anom.y,
            radius = r * 0.85f,
            spinAngle = anom.spinAngle,
            fadeAlpha = fadeAlpha,
          )
          drawWorldOverheadBanner(
            canvas = canvas,
            text = "PHASE RIFT ${if (isNodeA) "A" else "B"} • ${anom.life.toInt()}s",
            worldX = anom.x,
            worldY = anom.y - r * 0.85f - 18f,
            fillColor = Color.parseColor("#2DD4BF"),
            worldScale = worldScale,
            sizeMultiplier = 1.15f,
            alpha = baseAlphaInt,
          )
        }
      }
    }

    // 7. AimingTelegraphSystem — Spread Cones & 2-Bounce Raycast Prediction
    val telegraphScale = RiftCalibrationState.aimTelegraphScale
    if (p.respawnTimer <= 0f && p.canFire) {
      if (engine.fireStickActive && !engine.isCancelActive) {
        val activeAimAngle =
          when {
            p.independentAimActive -> p.weaponAngle
            RiftCalibrationState.gamepadConnected -> p.angle
            else -> engine.findQuickTapAutoAimAngle(p)
          }
        val hasMultiShot = (p.spec.supportsNativeMultifire && p.multifireOn) || p.multiShotCharges > 0
        val streams =
          when {
            p.shipId == 3 && !hasMultiShot -> 1
            p.shipId == 3 && p.multiShotCharges > 0 -> 5
            p.shipId == 3 && hasMultiShot -> 3
            p.shipId == 5 && !hasMultiShot -> 2
            p.shipId == 5 && p.multiShotCharges > 0 -> 5
            p.shipId == 5 && hasMultiShot -> 4
            hasMultiShot -> p.spec.primaryStreams + 2
            else -> p.spec.primaryStreams
          }
        val spreadRad =
          if (hasMultiShot) max(0.11f, p.spec.primarySpreadRad * 1.4f) else p.spec.primarySpreadRad

        if (streams > 1 && spreadRad > 0.03f) {
          val halfConeDeg = (spreadRad * (streams - 1) * 0.5f) * (180f / PI.toFloat())
          val centerDeg = activeAimAngle * (180f / PI.toFloat())
          val coneR = 440f * telegraphScale
          arcRect.set(p.x - coneR, p.y - coneR, p.x + coneR, p.y + coneR)
          canvas.drawArc(arcRect, centerDeg - halfConeDeg, halfConeDeg * 2f, true, spreadConePaint)
        }

        val gunBounces = p.spec.primaryBounces + (if (p.hasBouncingBulletsUpgrade) 1 else 0)
        if (gunBounces > 0) {
          val pts = engine.computeBounceTrajectory(p, activeAimAngle, maxBounces = 2, segmentMaxLen = 540f * telegraphScale)
          drawRaycastTrajectory(canvas, pts, Color.parseColor("#38BDF8"))
        } else {
          val ax = p.x + cos(activeAimAngle) * (500f * telegraphScale)
          val ay = p.y + sin(activeAimAngle) * (500f * telegraphScale)
          canvas.drawLine(p.x, p.y, ax, ay, aimGuidePaint)
        }
      }

      val isProjectileBomb =
        p.spec.hasBomb &&
          p.spec.bombType != "PHASE_DASH" &&
          p.spec.bombType != "CLOAK_EMP" &&
          p.spec.bombType != "INTERDICTOR_DOME"
      if (engine.bombStickActive && !engine.isBombCancelActive && engine.bombStickMag > 0.12f && isProjectileBomb) {
        val pts =
          engine.computeBounceTrajectory(
            p,
            engine.bombStickAngle,
            maxBounces = max(1, p.spec.bombBounces),
            segmentMaxLen = 600f * telegraphScale,
          )
        drawRaycastTrajectory(canvas, pts, Color.parseColor("#F59E0B"))
      }
    }

    // 7b. Permanent Placed 2.5D Anchored Rift Bulkhead Barriers (stay until round/match reset)
    for (bw in engine.brickWalls) {
      if (bw.x + bw.w < viewLeft - 56f || bw.x > viewRight + 56f ||
        bw.y + bw.h < viewTop - 56f || bw.y > viewBottom + 56f
      ) {
        continue
      }
      val isFriendlyWall = bw.ownerId == p.id || bw.freq == p.freq
      drawPermanentBarrier3D(canvas, bw, isFriendlyWall, effectiveCamX, effectiveCamY)
    }

    // 7c. Eclipse Stasis Bubbles (Two-Way Containment Domes: 275px radius, 6.0s lifespan)
    val bubbleRec = RiftVisualForgeState.getRecipe(ForgeElementTarget.ECLIPSE_STASIS_BUBBLE)
    val bubblePrimary = VisualElementRecipe.parseHexColorSafe(bubbleRec.primaryHex, Color.rgb(236, 72, 153))
    val bubbleGlow = VisualElementRecipe.parseHexColorSafe(bubbleRec.glowHex, Color.rgb(0, 245, 212))
    val bubbleTrim = VisualElementRecipe.parseHexColorSafe(bubbleRec.trimHex, Color.rgb(244, 114, 182))
    for (ib in engine.interdictorBubbles) {
      val r = ib.radius * bubbleRec.pixelScale.coerceIn(0.6f, 1.6f)
      if (ib.x + r < viewLeft - 40f || ib.x - r > viewRight + 40f ||
        ib.y + r < viewTop - 40f || ib.y - r > viewBottom + 40f
      ) {
        continue
      }
      val fadeAlpha = ((ib.life / 1.5f).coerceIn(0.25f, 1f) * bubbleRec.surfaceOpacity.coerceIn(0.25f, 1f))
      anomalyFillPaint.color =
        Color.argb((24 * fadeAlpha).toInt().coerceIn(6, 90), Color.red(bubblePrimary), Color.green(bubblePrimary), Color.blue(bubblePrimary))
      canvas.drawCircle(ib.x, ib.y, r, anomalyFillPaint)

      val spinPhase = ((System.currentTimeMillis() % 4000L) / 4000f) * (PI.toFloat() * 2f) * bubbleRec.glowIntensity.coerceIn(0.2f, 2.5f)
      anomalyRingPaint.strokeWidth = 2.2f * bubbleRec.borderWeight.coerceIn(0.5f, 2.5f)
      anomalyRingPaint.color =
        Color.argb((150 * fadeAlpha).toInt().coerceIn(30, 255), Color.red(bubbleGlow), Color.green(bubbleGlow), Color.blue(bubbleGlow))
      val innerR = r * 0.92f
      arcRect.set(ib.x - innerR, ib.y - innerR, ib.x + innerR, ib.y + innerR)
      val degOffset = (spinPhase * 180f / PI.toFloat())
      for (seg in 0 until 8) {
        canvas.drawArc(arcRect, degOffset + seg * 45f, 24f, false, anomalyRingPaint)
      }

      anomalyRingPaint.strokeWidth = 3.6f
      anomalyRingPaint.color =
        Color.argb((225 * fadeAlpha).toInt().coerceIn(45, 255), Color.red(bubblePrimary), Color.green(bubblePrimary), Color.blue(bubblePrimary))
      canvas.drawCircle(ib.x, ib.y, r, anomalyRingPaint)

      for (nodeIdx in 0 until 8) {
        val na = spinPhase * 0.5f + nodeIdx * (PI.toFloat() * 0.25f)
        val nx = ib.x + cos(na) * r
        val ny = ib.y + sin(na) * r
        anomalyCorePaint.color =
          Color.argb((235 * fadeAlpha).toInt().coerceIn(50, 255), Color.red(bubbleGlow), Color.green(bubbleGlow), Color.blue(bubbleGlow))
        canvas.drawCircle(nx, ny, 5.2f, anomalyCorePaint)
      }

      val safeScale = worldScale.coerceAtLeast(0.15f)
      drawWorldOverheadBanner(
        canvas = canvas,
        text = "STASIS • ${String.format(java.util.Locale.US, "%.1fs", ib.life)}",
        worldX = ib.x,
        worldY = ib.y - r - (16f / safeScale),
        fillColor = bubbleTrim,
        worldScale = worldScale,
        sizeMultiplier = 1.20f,
        alpha = (240 * fadeAlpha).toInt().coerceIn(0, 255),
      )
    }

    // 8. Distinct Per-Ship Primary Projectiles + Station Defense Nova Bolts (scaled by engine.getEffectiveProjectileScale())
    val projScale = engine.getEffectiveProjectileScale()
    val nowSpinRad = ((System.currentTimeMillis() % 900L) / 900f) * (PI.toFloat() * 2f)
    for (b in engine.bullets) {
      if (b.x < viewLeft - 40f || b.x > viewRight + 40f || b.y < viewTop - 40f || b.y > viewBottom + 40f) continue
      val speed = hypot(b.vx, b.vy).coerceAtLeast(1f)
      val dirX = b.vx / speed
      val dirY = b.vy / speed
      val perpX = -dirY
      val perpY = dirX

      if (b.ownerId.startsWith("station_")) {
        val isCascade = b.ownerId == "station_cascade"
        val len = (if (isCascade) 27f else 31f) * projScale
        val tailX = b.x - dirX * len
        val tailY = b.y - dirY * len
        bulletPaint.color = if (isCascade) Color.parseColor("#38BDF8") else Color.parseColor("#FACC15")
        bulletPaint.strokeWidth = (if (isCascade) 5.6f else 6.4f) * projScale
        bulletCorePaint.strokeWidth = 2.6f * projScale
        canvas.drawLine(b.x, b.y, tailX, tailY, bulletPaint)
        canvas.drawLine(b.x, b.y, tailX, tailY, bulletCorePaint)
        bombPaint.color = if (isCascade) Color.parseColor("#FACC15") else Color.parseColor("#EF4444")
        canvas.drawCircle(b.x, b.y, 4.8f * projScale, bombPaint)
        continue
      }

      when (b.ownerShipId) {
        1 -> {
          // 1-Viper: Elongated razor-thin cyan/white high-velocity rail beam (34f) + bright white diamond tip
          val len = 34f * projScale
          val tailX = b.x - dirX * len
          val tailY = b.y - dirY * len
          bulletPaint.color = b.color
          bulletPaint.strokeWidth = 4.5f * projScale
          bulletCorePaint.strokeWidth = 2.2f * projScale
          canvas.drawLine(b.x, b.y, tailX, tailY, bulletPaint)
          canvas.drawLine(b.x, b.y, tailX, tailY, bulletCorePaint)
          val dSize = 4.8f * projScale
          plumePath.reset()
          plumePath.moveTo(b.x + dirX * dSize, b.y + dirY * dSize)
          plumePath.lineTo(b.x + perpX * (dSize * 0.6f), b.y + perpY * (dSize * 0.6f))
          plumePath.lineTo(b.x - dirX * dSize, b.y - dirY * dSize)
          plumePath.lineTo(b.x - perpX * (dSize * 0.6f), b.y - perpY * (dSize * 0.6f))
          plumePath.close()
          bombPaint.color = Color.WHITE
          canvas.drawPath(plumePath, bombPaint)
        }
        2 -> {
          // 2-Ricochet: Spinning emerald-cyan plasma disc (6.5f) with outer bounce-energy ring
          val r = 6.5f * projScale
          bombRingPaint.strokeWidth = 2.2f * projScale
          bombRingPaint.color = Color.parseColor("#10B981")
          canvas.drawCircle(b.x, b.y, r * 1.35f, bombRingPaint)
          bombPaint.color = b.color
          canvas.drawCircle(b.x, b.y, r, bombPaint)
          bombPaint.color = Color.WHITE
          canvas.drawCircle(b.x, b.y, r * 0.48f, bombPaint)
        }
        3 -> {
          // 3-Suppressor: Sharp golden-amber rapid-fire needle darts (22f)
          val len = 22f * projScale
          val tailX = b.x - dirX * len
          val tailY = b.y - dirY * len
          bulletPaint.color = Color.parseColor("#F59E0B")
          bulletPaint.strokeWidth = 4.2f * projScale
          bulletCorePaint.strokeWidth = 2.0f * projScale
          canvas.drawLine(b.x, b.y, tailX, tailY, bulletPaint)
          canvas.drawLine(b.x, b.y, tailX, tailY, bulletCorePaint)
        }
        4 -> {
          // 4-Goliath: Massive heavy armor-piercing cannon shell (8.5f glowing orange-red plasma slug + wake + white-hot nose)
          val r = 8.5f * projScale
          val wakeX = b.x - dirX * (28f * projScale)
          val wakeY = b.y - dirY * (28f * projScale)
          bulletPaint.color = Color.argb(185, 234, 88, 12)
          bulletPaint.strokeWidth = 11.5f * projScale
          canvas.drawLine(b.x, b.y, wakeX, wakeY, bulletPaint)
          bombPaint.color = Color.parseColor("#F97316")
          canvas.drawCircle(b.x, b.y, r, bombPaint)
          bombPaint.color = Color.parseColor("#FEF08A")
          canvas.drawCircle(b.x + dirX * (r * 0.28f), b.y + dirY * (r * 0.28f), r * 0.56f, bombPaint)
        }
        5 -> {
          // 5-Twinfang: High-speed dual-tone cyan/violet needle bolts (24f) with trailing spark
          val len = 24f * projScale
          val tailX = b.x - dirX * len
          val tailY = b.y - dirY * len
          bulletPaint.color = Color.parseColor("#38BDF8")
          bulletPaint.strokeWidth = 4.0f * projScale
          canvas.drawLine(b.x, b.y, tailX, tailY, bulletPaint)
          bulletCorePaint.strokeWidth = 1.9f * projScale
          canvas.drawLine(b.x, b.y, tailX, tailY, bulletCorePaint)
          bombPaint.color = Color.parseColor("#C084FC")
          canvas.drawCircle(tailX, tailY, 3.0f * projScale, bombPaint)
        }
        6 -> {
          // 6-Disruptor: Violet-magenta EMP crescent arc pulse (6.5f)
          val r = 6.5f * projScale
          val tailX = b.x - dirX * (16f * projScale)
          val tailY = b.y - dirY * (16f * projScale)
          bulletPaint.color = Color.parseColor("#C084FC")
          bulletPaint.strokeWidth = 5.0f * projScale
          canvas.drawLine(b.x, b.y, tailX, tailY, bulletPaint)
          bombRingPaint.strokeWidth = 2.8f * projScale
          bombRingPaint.color = Color.parseColor("#E879F9")
          canvas.drawCircle(b.x, b.y, r, bombRingPaint)
          bombPaint.color = Color.WHITE
          canvas.drawCircle(b.x, b.y, r * 0.45f, bombPaint)
        }
        7 -> {
          // 7-Valkyrie: Crimson-gold star-shaped flak bolts (6.0f with 4 crossed X-fins)
          val r = 6.0f * projScale
          bulletPaint.color = Color.parseColor("#FB7185")
          bulletPaint.strokeWidth = 3.2f * projScale
          val c1 = cos(nowSpinRad) * r * 1.35f
          val s1 = sin(nowSpinRad) * r * 1.35f
          canvas.drawLine(b.x - c1, b.y - s1, b.x + c1, b.y + s1, bulletPaint)
          canvas.drawLine(b.x + s1, b.y - c1, b.x - s1, b.y + c1, bulletPaint)
          bombPaint.color = Color.parseColor("#FACC15")
          canvas.drawCircle(b.x, b.y, r * 0.65f, bombPaint)
        }
        8 -> {
          // 8-Manta: Bio-luminescent green plasma spheres (6.8f) with soft outer halo
          val r = 6.8f * projScale
          bombPaint.color = Color.argb(95, 16, 185, 129)
          canvas.drawCircle(b.x, b.y, r * 1.55f, bombPaint)
          bombPaint.color = Color.parseColor("#34D399")
          canvas.drawCircle(b.x, b.y, r, bombPaint)
          bombPaint.color = Color.WHITE
          canvas.drawCircle(b.x, b.y, r * 0.45f, bombPaint)
        }
        9 -> {
          // 9-Eclipse: Heavy amethyst-cyan gravimetric lance bolts (28f, strokeWidth 5.8f)
          val len = 28f * projScale
          val tailX = b.x - dirX * len
          val tailY = b.y - dirY * len
          bulletPaint.color = Color.parseColor("#EC4899")
          bulletPaint.strokeWidth = 5.8f * projScale
          bulletCorePaint.strokeWidth = 2.6f * projScale
          canvas.drawLine(b.x, b.y, tailX, tailY, bulletPaint)
          canvas.drawLine(b.x, b.y, tailX, tailY, bulletCorePaint)
          bombPaint.color = Color.parseColor("#00F5D4")
          canvas.drawCircle(b.x, b.y, 4.2f * projScale, bombPaint)
        }
        else -> {
          // 10-Rift Frame: Prismatic cyan-gold modular bolt (26f, strokeWidth 5.2f)
          val len = 26f * projScale
          val tailX = b.x - dirX * len
          val tailY = b.y - dirY * len
          bulletPaint.color = b.color
          bulletPaint.strokeWidth = 5.2f * projScale
          bulletCorePaint.strokeWidth = 2.4f * projScale
          canvas.drawLine(b.x, b.y, tailX, tailY, bulletPaint)
          canvas.drawLine(b.x, b.y, tailX, tailY, bulletCorePaint)
          bombPaint.color = Color.parseColor("#FACC15")
          canvas.drawCircle(b.x, b.y, 3.6f * projScale, bombPaint)
        }
      }
    }

    // 9. Distinct Per-Ship Secondary Warheads, Mines, Seeker Torpedoes & Infinite Proximity-Lock Missiles (scaled by projScale)
    for (bm in engine.bombs) {
      val cullPad = if (bm.isInfiniteProxMissile) bm.proxLockRadius + 60f else 95f
      if (bm.x < viewLeft - cullPad || bm.x > viewRight + cullPad || bm.y < viewTop - cullPad || bm.y > viewBottom + cullPad) {
        continue
      }
      when (bm.type) {
        "SIEGE" -> {
          // Goliath Siege Bomb: 26px Spiked Nuclear Core with 6 rotating outer reactor spikes & double shock-ring
          val coreR = 24f * projScale
          val spikeOuterR = 32f * projScale
          bombRingPaint.strokeWidth = 3.0f * projScale
          bombRingPaint.color = Color.argb(195, 249, 115, 22)
          canvas.drawCircle(bm.x, bm.y, coreR * 1.25f, bombRingPaint)
          bombRingPaint.strokeWidth = 1.8f * projScale
          bombRingPaint.color = Color.argb(135, 254, 240, 138)
          canvas.drawCircle(bm.x, bm.y, coreR * 1.48f, bombRingPaint)

          // 6 rotating outer reactor spikes
          bulletPaint.color = Color.parseColor("#EA580C")
          bulletPaint.strokeWidth = 5.2f * projScale
          for (i in 0 until 6) {
            val a = nowSpinRad + i * (PI.toFloat() / 3f)
            val sx0 = bm.x + cos(a) * (coreR * 0.7f)
            val sy0 = bm.y + sin(a) * (coreR * 0.7f)
            val sx1 = bm.x + cos(a) * spikeOuterR
            val sy1 = bm.y + sin(a) * spikeOuterR
            canvas.drawLine(sx0, sy0, sx1, sy1, bulletPaint)
          }

          // Dark-iron outer shell + solar-orange / molten yellow core
          bombPaint.color = Color.parseColor("#431407")
          canvas.drawCircle(bm.x, bm.y, coreR, bombPaint)
          bombPaint.color = Color.parseColor("#F97316")
          canvas.drawCircle(bm.x, bm.y, coreR * 0.76f, bombPaint)
          bombPaint.color = Color.parseColor("#FEF08A")
          canvas.drawCircle(bm.x, bm.y, coreR * 0.42f, bombPaint)
        }

        "PROX" -> {
          // Valkyrie Proximity Flak Bomb: 18px Octagonal Shrapnel Warhead with 8 outward fins + pulsing dashed 95px trigger ring
          val r = 17f * projScale
          val proxRingR = 95f * projScale
          safeDashPaint.color = Color.argb(165, 251, 113, 133)
          safeDashPaint.strokeWidth = 2.2f
          canvas.drawCircle(bm.x, bm.y, proxRingR, safeDashPaint)

          // 8 outward shrapnel fins
          bulletPaint.color = Color.parseColor("#FB7185")
          bulletPaint.strokeWidth = 3.6f * projScale
          for (i in 0 until 8) {
            val a = -nowSpinRad * 0.8f + i * (PI.toFloat() * 0.25f)
            val fx = bm.x + cos(a) * (r * 1.38f)
            val fy = bm.y + sin(a) * (r * 1.38f)
            canvas.drawLine(bm.x, bm.y, fx, fy, bulletPaint)
          }

          // Octagonal crimson-coral body + white-gold core
          plumePath.reset()
          for (i in 0 until 8) {
            val a = i * (PI.toFloat() * 0.25f)
            val ox = bm.x + cos(a) * r
            val oy = bm.y + sin(a) * r
            if (i == 0) plumePath.moveTo(ox, oy) else plumePath.lineTo(ox, oy)
          }
          plumePath.close()
          bombPaint.color = Color.parseColor("#E11D48")
          canvas.drawPath(plumePath, bombPaint)
          bombPaint.color = Color.parseColor("#FDE047")
          canvas.drawCircle(bm.x, bm.y, r * 0.46f, bombPaint)
        }

        "BOUNCE" -> {
          // Ricochet Bounce Bomb: 17px Emerald-Gold Gyro Sphere with dual counter-rotating gyroscopic bounce rings
          val r = 17f * projScale
          bombRingPaint.strokeWidth = 2.8f * projScale
          bombRingPaint.color = Color.parseColor("#10B981")
          arcRect.set(bm.x - r * 1.32f, bm.y - r * 1.32f, bm.x + r * 1.32f, bm.y + r * 1.32f)
          val deg = (nowSpinRad * 180f / PI.toFloat())
          canvas.drawArc(arcRect, deg, 120f, false, bombRingPaint)
          canvas.drawArc(arcRect, deg + 180f, 120f, false, bombRingPaint)

          bombRingPaint.color = Color.parseColor("#FACC15")
          arcRect.set(bm.x - r * 1.05f, bm.y - r * 1.05f, bm.x + r * 1.05f, bm.y + r * 1.05f)
          canvas.drawArc(arcRect, -deg + 45f, 110f, false, bombRingPaint)
          canvas.drawArc(arcRect, -deg + 225f, 110f, false, bombRingPaint)

          bombPaint.color = Color.parseColor("#10B981")
          canvas.drawCircle(bm.x, bm.y, r * 0.72f, bombPaint)
          bombPaint.color = Color.parseColor("#FEF08A")
          canvas.drawCircle(bm.x, bm.y, r * 0.36f, bombPaint)
        }

        "MINE" -> {
          // Manta Proximity Mine: #050A14 3D radial-shaded armored sphere + 6 beveled metallic contact prongs + pulsing #EF4444/#F59E0B core LED
          val r = 20f * projScale
          safeDashPaint.color = Color.argb(120, 239, 68, 68)
          safeDashPaint.strokeWidth = 1.8f
          canvas.drawCircle(bm.x, bm.y, 85f * projScale, safeDashPaint)

          for (i in 0 until 6) {
            val a = i * (PI.toFloat() / 3f)
            val sx = bm.x + cos(a) * (r * 1.38f)
            val sy = bm.y + sin(a) * (r * 1.38f)
            bulletPaint.color = Color.parseColor("#0F172A")
            bulletPaint.strokeWidth = 5.2f * projScale
            canvas.drawLine(bm.x, bm.y, sx, sy, bulletPaint)
            bulletPaint.color = Color.parseColor("#94A3B8")
            bulletPaint.strokeWidth = 2.6f * projScale
            canvas.drawLine(bm.x, bm.y, sx, sy, bulletPaint)
            bombPaint.color = Color.parseColor("#F59E0B")
            canvas.drawCircle(sx, sy, 2.5f * projScale, bombPaint)
          }

          bombPaint.color = Color.parseColor("#050A14")
          canvas.drawCircle(bm.x, bm.y, r * 0.84f, bombPaint)
          bombRingPaint.strokeWidth = 1.8f * projScale
          bombRingPaint.color = Color.parseColor("#334155")
          canvas.drawCircle(bm.x, bm.y, r * 0.84f, bombRingPaint)
          bombPaint.color = Color.argb(95, 148, 163, 184)
          canvas.drawCircle(bm.x - r * 0.22f, bm.y - r * 0.22f, r * 0.28f, bombPaint)

          val pulseOn = ((System.currentTimeMillis() / 200L) % 2L) == 0L
          bombPaint.color = if (pulseOn) Color.parseColor("#EF4444") else Color.parseColor("#F59E0B")
          canvas.drawCircle(bm.x, bm.y, r * 0.36f, bombPaint)
          bombPaint.color = Color.WHITE
          canvas.drawCircle(bm.x - r * 0.08f, bm.y - r * 0.08f, r * 0.11f, bombPaint)
        }

        "MISSILE" -> {
          // Seeker Torpedo: Elongated finned homing torpedo fuselage aligned to (bm.vx, bm.vy) + twin exhaust plumes
          val spd = hypot(bm.vx, bm.vy).coerceAtLeast(1f)
          val dx = bm.vx / spd
          val dy = bm.vy / spd
          val px = -dy
          val py = dx
          val len = 24f * projScale
          val wid = 9f * projScale

          // Twin afterburner exhaust plumes
          for (side in listOf(-0.45f, 0.45f)) {
            val ex0 = bm.x - dx * (len * 0.55f) + px * (wid * side)
            val ey0 = bm.y - dy * (len * 0.55f) + py * (wid * side)
            val ex1 = ex0 - dx * (22f * projScale)
            val ey1 = ey0 - dy * (22f * projScale)
            bulletPaint.color = Color.parseColor("#FB923C")
            bulletPaint.strokeWidth = 4.2f * projScale
            canvas.drawLine(ex0, ey0, ex1, ey1, bulletPaint)
          }

          // Finned torpedo fuselage
          plumePath.reset()
          plumePath.moveTo(bm.x + dx * len, bm.y + dy * len)
          plumePath.lineTo(bm.x - dx * (len * 0.5f) + px * wid, bm.y - dy * (len * 0.5f) + py * wid)
          plumePath.lineTo(bm.x - dx * (len * 0.3f), bm.y - dy * (len * 0.3f))
          plumePath.lineTo(bm.x - dx * (len * 0.5f) - px * wid, bm.y - dy * (len * 0.5f) - py * wid)
          plumePath.close()
          bombPaint.color = Color.parseColor("#EF4444")
          canvas.drawPath(plumePath, bombPaint)
          bombPaint.color = Color.WHITE
          canvas.drawCircle(bm.x + dx * (len * 0.35f), bm.y + dy * (len * 0.35f), 3.8f * projScale, bombPaint)
        }

        "PROX_MISSILE" -> {
          // Infinite Proximity-Lock Missile: Visible 340px Influence Ring + Dynamic Re-Lock Tether + Solar-Gold/Cyan Warhead
          val lockR = bm.proxLockRadius.coerceAtLeast(220f)
          val hasLock = bm.targetShipId.isNotEmpty()
          val ringCol = if (hasLock) Color.parseColor("#FACC15") else Color.parseColor("#38BDF8")
          val ringPulse = 0.5f + 0.5f * sin(tSec * (if (hasLock) 8.5f else 3.2f))

          // Subtle translucent influence field fill + crisp segmented proximity ring
          anomalyFillPaint.color =
            Color.argb(
              if (hasLock) 18 else 11,
              Color.red(ringCol),
              Color.green(ringCol),
              Color.blue(ringCol),
            )
          canvas.drawCircle(bm.x, bm.y, lockR, anomalyFillPaint)

          safeDashPaint.color =
            Color.argb(
              (115f + 65f * ringPulse).roundToInt().coerceIn(85, 195),
              Color.red(ringCol),
              Color.green(ringCol),
              Color.blue(ringCol),
            )
          safeDashPaint.strokeWidth = if (hasLock) 2.6f else 2.0f
          canvas.drawCircle(bm.x, bm.y, lockR, safeDashPaint)

          arcRect.set(bm.x - lockR, bm.y - lockR, bm.x + lockR, bm.y + lockR)
          bombRingPaint.strokeWidth = 3.0f
          bombRingPaint.color =
            Color.argb(185, Color.red(ringCol), Color.green(ringCol), Color.blue(ringCol))
          val rotDeg = (tSec * (if (hasLock) 72f else 32f)) % 360f
          for (seg in 0 until 4) {
            canvas.drawArc(arcRect, rotDeg + seg * 90f, 22f, false, bombRingPaint)
          }

          if (hasLock) {
            val lockedShip = engine.ships.firstOrNull { it.id == bm.targetShipId && it.respawnTimer <= 0f }
            if (lockedShip != null && !lockedShip.isCloaked) {
              safeDashPaint.color = Color.argb(145, 250, 204, 21)
              safeDashPaint.strokeWidth = 1.8f
              canvas.drawLine(bm.x, bm.y, lockedShip.x, lockedShip.y, safeDashPaint)
            }
          }

          val spd = hypot(bm.vx, bm.vy).coerceAtLeast(1f)
          val dx = bm.vx / spd
          val dy = bm.vy / spd
          val px = -dy
          val py = dx
          val len = 25f * projScale
          val wid = 10f * projScale

          // Twin cyan-gold plasma exhaust trails
          for (side in listOf(-0.46f, 0.46f)) {
            val ex0 = bm.x - dx * (len * 0.52f) + px * (wid * side)
            val ey0 = bm.y - dy * (len * 0.52f) + py * (wid * side)
            bulletPaint.color = Color.parseColor("#00F5D4")
            bulletPaint.strokeWidth = 4.0f * projScale
            canvas.drawLine(ex0, ey0, ex0 - dx * (24f * projScale), ey0 - dy * (24f * projScale), bulletPaint)
          }

          plumePath.reset()
          plumePath.moveTo(bm.x + dx * len, bm.y + dy * len)
          plumePath.lineTo(bm.x - dx * (len * 0.50f) + px * wid, bm.y - dy * (len * 0.50f) + py * wid)
          plumePath.lineTo(bm.x - dx * (len * 0.26f), bm.y - dy * (len * 0.26f))
          plumePath.lineTo(bm.x - dx * (len * 0.50f) - px * wid, bm.y - dy * (len * 0.50f) - py * wid)
          plumePath.close()
          bombPaint.color = Color.parseColor("#FACC15")
          canvas.drawPath(plumePath, bombPaint)
          bombRingPaint.strokeWidth = 1.8f * projScale
          bombRingPaint.color = Color.parseColor("#00F5D4")
          canvas.drawPath(plumePath, bombRingPaint)
          bombPaint.color = Color.WHITE
          canvas.drawCircle(bm.x + dx * (len * 0.34f), bm.y + dy * (len * 0.34f), 4.0f * projScale, bombPaint)
        }

        else -> {
          val c =
            when (bm.type) {
              "EMP" -> Color.parseColor("#B388FF")
              "CUSTOM_ORB" -> if (bm.customColor != 0) bm.customColor else Color.parseColor("#22D3EE")
              else -> Color.parseColor("#FBBF24")
            }
          val tailX = bm.x - bm.vx * 0.05f
          val tailY = bm.y - bm.vy * 0.05f
          bulletPaint.color = c
          bulletPaint.strokeWidth = 7.5f * projScale
          canvas.drawLine(bm.x, bm.y, tailX, tailY, bulletPaint)
          bombRingPaint.color = Color.argb(150, Color.red(c), Color.green(c), Color.blue(c))
          val auraR =
            if (bm.customEffect != CustomBuffEffect.NONE && bm.customBlastRadius > 0f) {
              (bm.customBlastRadius * 0.35f).coerceIn(bm.radius + 12f, 110f)
            } else {
              bm.radius + 12f
            }
          canvas.drawCircle(bm.x, bm.y, auraR * projScale, bombRingPaint)
          bombPaint.color = c
          canvas.drawCircle(bm.x, bm.y, bm.radius * projScale, bombPaint)
          bombPaint.color = Color.WHITE
          canvas.drawCircle(bm.x, bm.y, bm.radius * 0.44f * projScale, bombPaint)
        }
      }
    }

    // 9a. 5-Drone Autonomous Zig-Zagging SWARM Drones (3D Delta Chassis + Twin Plasma Trails)
    for (dr in engine.swarmDrones) {
      if (dr.x < viewLeft - 70f || dr.x > viewRight + 70f || dr.y < viewTop - 70f || dr.y > viewBottom + 70f) {
        continue
      }
      val isFriendlyDrone = engine.isDroneAlliedWithShip(dr, p)
      val trimCol = if (isFriendlyDrone) Color.parseColor("#00F5D4") else Color.parseColor("#FB7185")
      val coreCol = if (isFriendlyDrone) Color.parseColor("#38BDF8") else Color.parseColor("#FACC15")
      val dx = cos(dr.angle)
      val dy = sin(dr.angle)
      val px = -dy
      val py = dx
      val dScale = projScale.coerceIn(0.75f, 1.5f)
      val noseLen = 17f * dScale
      val wingSpan = 13f * dScale

      // Twin plasma exhaust trails
      for (side in listOf(-0.52f, 0.52f)) {
        val ex0 = dr.x - dx * (noseLen * 0.45f) + px * (wingSpan * side)
        val ey0 = dr.y - dy * (noseLen * 0.45f) + py * (wingSpan * side)
        val ex1 = ex0 - dx * (22f * dScale)
        val ey1 = ey0 - dy * (noseLen * 0.45f + 22f * dScale - noseLen * 0.45f)
        bulletPaint.color = Color.argb(175, Color.red(trimCol), Color.green(trimCol), Color.blue(trimCol))
        bulletPaint.strokeWidth = 3.2f * dScale
        canvas.drawLine(ex0, ey0, ex0 - dx * (20f * dScale), ey0 - dy * (20f * dScale), bulletPaint)
      }

      // Outer energy halo
      bombRingPaint.strokeWidth = 1.8f * dScale
      bombRingPaint.color = Color.argb(115, Color.red(trimCol), Color.green(trimCol), Color.blue(trimCol))
      canvas.drawCircle(dr.x, dr.y, 16f * dScale, bombRingPaint)

      // 3D Delta Hunter Chassis
      plumePath.reset()
      plumePath.moveTo(dr.x + dx * noseLen, dr.y + dy * noseLen)
      plumePath.lineTo(dr.x - dx * (noseLen * 0.55f) + px * wingSpan, dr.y - dy * (noseLen * 0.55f) + py * wingSpan)
      plumePath.lineTo(dr.x - dx * (noseLen * 0.22f), dr.y - dy * (noseLen * 0.22f))
      plumePath.lineTo(dr.x - dx * (noseLen * 0.55f) - px * wingSpan, dr.y - dy * (noseLen * 0.55f) - py * wingSpan)
      plumePath.close()
      bombPaint.color = Color.parseColor("#061224")
      canvas.drawPath(plumePath, bombPaint)
      bombRingPaint.strokeWidth = 2.2f * dScale
      bombRingPaint.color = trimCol
      canvas.drawPath(plumePath, bombRingPaint)
      bombPaint.color = coreCol
      canvas.drawCircle(dr.x + dx * (noseLen * 0.18f), dr.y + dy * (noseLen * 0.18f), 3.6f * dScale, bombPaint)
      bombPaint.color = Color.WHITE
      canvas.drawCircle(dr.x + dx * (noseLen * 0.22f), dr.y + dy * (noseLen * 0.22f), 1.7f * dScale, bombPaint)
    }

    // 9b. Decoy Hologram Clones (Render identically to real ships for enemies; subtle cyan ring for owner/allies)
    for (dc in engine.decoys) {
      if (dc.x < viewLeft - 110f || dc.x > viewRight + 110f || dc.y < viewTop - 110f || dc.y > viewBottom + 110f) {
        continue
      }
      val isFriendlyDecoy =
        dc.ownerId == p.id ||
          (p.armadaName.isNotEmpty() && dc.armadaName == p.armadaName) ||
          (p.syndicateTag != "GUEST" && dc.syndicateTag == p.syndicateTag)
      val bmp =
        RiftSpriteBaker.getShipSprite(
          shipId = dc.shipId,
          isAllied = isFriendlyDecoy,
          isHitFlashing = false,
        )
      val halfBmp = bmp.width * 0.5f
      val drawScale = (88f / bmp.width.toFloat()) * RiftCalibrationState.shipScale
      val renderHeadingRad = if (hypot(dc.vx, dc.vy) > 2f) atan2(dc.vy, dc.vx) else dc.angle
      shipMatrix.reset()
      shipMatrix.postTranslate(-halfBmp, -halfBmp)
      shipMatrix.postScale(drawScale, drawScale)
      shipMatrix.postRotate(renderHeadingRad * 180f / PI.toFloat())
      shipMatrix.postTranslate(dc.x, dc.y)
      if (isFriendlyDecoy) {
        buffGlowPaint.alpha = 185
        canvas.drawBitmap(bmp, shipMatrix, buffGlowPaint)
        bombRingPaint.color = Color.argb(175, 56, 189, 248)
        canvas.drawCircle(dc.x, dc.y, 44f * RiftCalibrationState.shipScale, bombRingPaint)
      } else {
        canvas.drawBitmap(bmp, shipMatrix, null)
      }

      if (overwatchLabelAlphaFactor > 0.02f) {
        val safeWorldScale = worldScale.coerceAtLeast(0.15f)
        val baseTextScale = engine.playfieldTextScale * RiftCalibrationState.labelTextScale
        val callsignTextSize = (RiftCalibrationState.nametagFontSizePx * 0.5f * dpDensity * baseTextScale) / safeWorldScale
        val radialDist = (44f * RiftCalibrationState.shipScale + 10f) * RiftCalibrationState.labelDistanceOffset
        val tagStr = if (dc.armadaName.isNotEmpty()) "[${dc.armadaName}]" else "[${dc.syndicateTag.ifEmpty { "GUEST" }}]"
        val labelText = if (isFriendlyDecoy) "[PHANTOM] ${dc.callsign}" else "$tagStr ${dc.callsign} • ${dc.bounty}"
        val decoyAlpha = (240f * overwatchLabelAlphaFactor).roundToInt().coerceIn(0, 255)
        shipLabelStrokePaint.strokeWidth = (1.7f * dpDensity * baseTextScale) / safeWorldScale
        shipLabelStrokePaint.textSize = callsignTextSize
        shipLabelStrokePaint.alpha = decoyAlpha
        shipLabelStrokePaint.textAlign = Paint.Align.CENTER
        shipLabelFillPaint.textSize = callsignTextSize
        shipLabelFillPaint.alpha = decoyAlpha
        shipLabelFillPaint.textAlign = Paint.Align.CENTER
        shipLabelFillPaint.color = if (isFriendlyDecoy) Color.parseColor("#38BDF8") else Color.parseColor("#F87171")
        canvas.drawText(labelText, dc.x, dc.y + radialDist + callsignTextSize * 0.85f, shipLabelStrokePaint)
        canvas.drawText(labelText, dc.x, dc.y + radialDist + callsignTextSize * 0.85f, shipLabelFillPaint)
      }
    }

    // 9b. Phase Dash Holographic Cyan Afterimages (spatial blink echoes)
    for (pa in engine.phaseAfterimages) {
      if (pa.x < viewLeft - 110f || pa.x > viewRight + 110f || pa.y < viewTop - 110f || pa.y > viewBottom + 110f) {
        continue
      }
      val echoBmp =
        RiftSpriteBaker.getShipSprite(
          shipId = pa.shipId,
          isAllied = pa.isAllied,
          isHitFlashing = false,
        )
      val halfEcho = echoBmp.width * 0.5f
      val echoScale = (88f / echoBmp.width.toFloat()) * RiftCalibrationState.shipScale
      val normLife = (pa.life / pa.maxLife).coerceIn(0f, 1f)
      shipMatrix.reset()
      shipMatrix.postTranslate(-halfEcho, -halfEcho)
      shipMatrix.postScale(echoScale, echoScale)
      shipMatrix.postRotate(pa.angleRad * 180f / PI.toFloat())
      shipMatrix.postTranslate(pa.x, pa.y)
      phaseEchoPaint.alpha = (normLife * 195f).toInt().coerceIn(0, 255)
      canvas.drawBitmap(echoBmp, shipMatrix, phaseEchoPaint)
    }

    // 10. 2x Playfield 3D Metallic Ship Sprites (88px) + Active Tractor Webs & Continuous Lance Beams + 3D Turn Banking:
    val shipsToRender =
      if (engine.hasJoinedMatch || isHubOverwatchMode || engine.isHubOverwatchActive) engine.ships else listOf(p)
    for (beamOwner in shipsToRender) {
      if (beamOwner.respawnTimer > 0f) continue
      val aimRad = if (beamOwner.independentAimActive) beamOwner.weaponAngle else beamOwner.angle
      val muzzleX = beamOwner.x + cos(aimRad) * (beamOwner.radius * 0.85f)
      val muzzleY = beamOwner.y + sin(aimRad) * (beamOwner.radius * 0.85f)

      // 10-pre-A: Toggleable Tractor Beam Laser Web (scanning cone or braided snare web pulling caught ship)
      if (beamOwner.tractorBeamActive) {
        val caught =
          if (beamOwner.tractorTargetShipId.isNotEmpty()) {
            engine.ships.firstOrNull { it.id == beamOwner.tractorTargetShipId && it.respawnTimer <= 0f }
          } else {
            null
          }
        if (caught != null) {
          val tx = caught.x
          val ty = caught.y
          val dx = tx - muzzleX
          val dy = ty - muzzleY
          val dist = hypot(dx, dy).coerceAtLeast(1f)
          val ux = dx / dist
          val uy = dy / dist
          val px = -uy
          val py = ux

          // Soft gravimetric tether glow
          bulletPaint.color = Color.argb(86, 16, 185, 129)
          bulletPaint.strokeWidth = 18f
          canvas.drawLine(muzzleX, muzzleY, tx, ty, bulletPaint)

          // Dual braided laser-web strands + transverse harmonic web rungs
          val segs = 12
          var prevAx = muzzleX
          var prevAy = muzzleY
          var prevBx = muzzleX
          var prevBy = muzzleY
          for (step in 1..segs) {
            val frac = step / segs.toFloat()
            val env = sin(frac * PI.toFloat())
            val wave = sin(frac * 9.5f - tSec * 12f) * (16f * env + 4f)
            val cxPt = muzzleX + dx * frac
            val cyPt = muzzleY + dy * frac
            val ax = cxPt + px * wave
            val ay = cyPt + py * wave
            val bx = cxPt - px * wave
            val by = cyPt - py * wave
            anomalyRingPaint.strokeWidth = 2.6f
            anomalyRingPaint.color = Color.argb(225, 52, 211, 153)
            canvas.drawLine(prevAx, prevAy, ax, ay, anomalyRingPaint)
            anomalyRingPaint.color = Color.argb(215, 0, 245, 212)
            canvas.drawLine(prevBx, prevBy, bx, by, anomalyRingPaint)
            if (step < segs && step % 2 == 0) {
              anomalyRingPaint.strokeWidth = 1.6f
              anomalyRingPaint.color = Color.argb(150, 167, 243, 208)
              canvas.drawLine(ax, ay, bx, by, anomalyRingPaint)
            }
            prevAx = ax
            prevAy = ay
            prevBx = bx
            prevBy = by
          }
          // Snare containment rings around the caught ship
          val snareR = caught.radius * (1.16f + 0.06f * sin(tSec * 9f))
          anomalyRingPaint.strokeWidth = 3.0f
          anomalyRingPaint.color = Color.argb(225, 52, 211, 153)
          canvas.drawCircle(tx, ty, snareR, anomalyRingPaint)
          anomalyRingPaint.strokeWidth = 1.8f
          anomalyRingPaint.color = Color.argb(180, 0, 245, 212)
          canvas.drawCircle(tx, ty, snareR * 0.82f, anomalyRingPaint)
        } else {
          // Scanning laser-web emitter cone ahead of ship
          val scanRange = 540f
          val halfCone = 0.40f
          val leftX = muzzleX + cos(aimRad - halfCone) * scanRange
          val leftY = muzzleY + sin(aimRad - halfCone) * scanRange
          val rightX = muzzleX + cos(aimRad + halfCone) * scanRange
          val rightY = muzzleY + sin(aimRad + halfCone) * scanRange
          safeDashPaint.color = Color.argb(120, 52, 211, 153)
          safeDashPaint.strokeWidth = 1.8f
          canvas.drawLine(muzzleX, muzzleY, leftX, leftY, safeDashPaint)
          canvas.drawLine(muzzleX, muzzleY, rightX, rightY, safeDashPaint)
          arcRect.set(muzzleX - scanRange, muzzleY - scanRange, muzzleX + scanRange, muzzleY + scanRange)
          val startDeg = ((aimRad - halfCone) * 180f / PI.toFloat())
          val sweepDeg = (halfCone * 2f * 180f / PI.toFloat())
          anomalyRingPaint.strokeWidth = 2.2f
          anomalyRingPaint.color = Color.argb(135, 0, 245, 212)
          canvas.drawArc(arcRect, startDeg, sweepDeg, false, anomalyRingPaint)
          val innerArcR = scanRange * (0.35f + 0.55f * ((tSec * 1.35f) % 1.0f))
          arcRect.set(muzzleX - innerArcR, muzzleY - innerArcR, muzzleX + innerArcR, muzzleY + innerArcR)
          anomalyRingPaint.strokeWidth = 1.6f
          anomalyRingPaint.color = Color.argb(95, 52, 211, 153)
          canvas.drawArc(arcRect, startDeg, sweepDeg, false, anomalyRingPaint)
        }
      }

      // 10-pre-B: Toggleable Continuous Lance Beam (stops cleanly at first ship/obstacle impact point!)
      if (beamOwner.lanceBeamActive) {
        val endX =
          if (beamOwner.lanceRenderEndX != 0f || beamOwner.lanceRenderEndY != 0f) {
            beamOwner.lanceRenderEndX
          } else {
            muzzleX + cos(aimRad) * 760f
          }
        val endY =
          if (beamOwner.lanceRenderEndX != 0f || beamOwner.lanceRenderEndY != 0f) {
            beamOwner.lanceRenderEndY
          } else {
            muzzleY + sin(aimRad) * 760f
          }
        val wPx = beamOwner.lanceRenderWidthPx.coerceIn(12f, 54f)
        val pulseW = 1f + 0.09f * sin(tSec * 24f)

        // Outer violet-magenta plasma sheath
        bulletPaint.color = Color.argb(115, 168, 85, 247)
        bulletPaint.strokeWidth = wPx * 1.65f * pulseW
        canvas.drawLine(muzzleX, muzzleY, endX, endY, bulletPaint)

        // Mid focused amethyst-cyan lance body
        bulletPaint.color = Color.argb(215, 217, 70, 239)
        bulletPaint.strokeWidth = wPx * 0.92f * pulseW
        canvas.drawLine(muzzleX, muzzleY, endX, endY, bulletPaint)

        // White-cyan razor core
        bulletCorePaint.color = Color.WHITE
        bulletCorePaint.strokeWidth = (wPx * 0.36f).coerceAtLeast(3.4f)
        canvas.drawLine(muzzleX, muzzleY, endX, endY, bulletCorePaint)

        // Muzzle lens & non-penetrating impact flare
        bombPaint.color = Color.parseColor("#E879F9")
        canvas.drawCircle(muzzleX, muzzleY, wPx * 0.52f, bombPaint)
        bombPaint.color = Color.WHITE
        canvas.drawCircle(muzzleX, muzzleY, wPx * 0.24f, bombPaint)

        val flareR = wPx * (0.68f + 0.16f * sin(tSec * 28f))
        bombRingPaint.strokeWidth = 2.6f
        bombRingPaint.color = Color.parseColor("#00F5D4")
        canvas.drawCircle(endX, endY, flareR * 1.18f, bombRingPaint)
        bombPaint.color = Color.parseColor("#D946EF")
        canvas.drawCircle(endX, endY, flareR * 0.78f, bombPaint)
        bombPaint.color = Color.WHITE
        canvas.drawCircle(endX, endY, flareR * 0.38f, bombPaint)
      }
    }
    for (s in shipsToRender) {
      if (s.respawnTimer > 0f) continue
      if (s.x < viewLeft - 110f || s.x > viewRight + 110f || s.y < viewTop - 110f || s.y > viewBottom + 110f) {
        continue
      }

      val isFriendly = engine.areAllied(p, s)
      if (s.isCloaked && !isFriendly && !s.isLocalPlayer) {
        // Complete playfield + nametag invisibility against enemies while Stealth Cloak is active!
        continue
      }

      // 2x 3D Metallic Ship Sprite (88f world units) with Dynamic 3D Turn Banking & 0.48s 360° Bounce Barrel Roll
      val bmp =
        RiftSpriteBaker.getShipSprite(
          shipId = s.shipId,
          isAllied = isFriendly,
          isHitFlashing = false,
        )
      val halfBmp = bmp.width * 0.5f
      val drawScale = (88f / bmp.width.toFloat()) * RiftCalibrationState.shipScale
      val renderHeadingRad = if (hypot(s.vx, s.vy) > 2f) atan2(s.vy, s.vx) else s.angle
      val renderHeadingDeg = renderHeadingRad * 180f / PI.toFloat()

      val barrelProgress =
        if (s.barrelRollTimer > 0f) (1f - (s.barrelRollTimer / 0.48f)).coerceIn(0f, 1f) else 0f
      val easedBarrel = barrelProgress * barrelProgress * (3f - 2f * barrelProgress)
      val barrelRad =
        if (s.barrelRollTimer > 0f) easedBarrel * (PI.toFloat() * 2f) * s.barrelRollDir else 0f
      val totalRollRad = (s.visualBankRoll * 0.72f) + barrelRad
      val cosRoll = cos(totalRollRad)
      val sinRoll = sin(totalRollRad)
      val lateralScaleY = kotlin.math.abs(cosRoll).coerceAtLeast(0.28f) * drawScale
      val rollSkewY = sinRoll * 0.14f

      // 10a. Offset 3D Silhouette Drop-Shadow beneath the banking/rolling ship
      if (!s.isCloaked) {
        shadowMatrix.reset()
        shadowMatrix.postTranslate(-halfBmp, -halfBmp)
        shadowMatrix.postScale(drawScale * 0.96f, lateralScaleY * 0.94f)
        shadowMatrix.postSkew(0f, rollSkewY)
        shadowMatrix.postRotate(renderHeadingDeg)
        shadowMatrix.postTranslate(s.x + 8f + sinRoll * 6f, s.y + 12f)
        canvas.drawBitmap(bmp, shadowMatrix, shipShadowPaint)
      }

      // 10b. Foreshortened & Perspective-Skewed 3D Ship Hull (inverted cool-steel belly tint when cosRoll < 0f)
      shipMatrix.reset()
      shipMatrix.postTranslate(-halfBmp, -halfBmp)
      shipMatrix.postScale(drawScale, lateralScaleY)
      shipMatrix.postSkew(0f, rollSkewY)
      shipMatrix.postRotate(renderHeadingDeg)
      shipMatrix.postTranslate(s.x, s.y)
      if (s.isCloaked) {
        buffGlowPaint.alpha = 78
        canvas.drawBitmap(bmp, shipMatrix, buffGlowPaint)
        bombRingPaint.color = Color.argb(165, 179, 136, 255)
        canvas.drawCircle(s.x, s.y, 44f * RiftCalibrationState.shipScale, bombRingPaint)
      } else if (cosRoll < 0f) {
        canvas.drawBitmap(bmp, shipMatrix, shipBellyTintPaint)
      } else {
        canvas.drawBitmap(bmp, shipMatrix, null)
      }

      // 10c. Specular Wing-Ridge Glint on Steep Bank/Roll + Swirling 360° Wingtip Vortex Helices on Bounce Barrel Roll
      if (!s.isCloaked) {
        val bankIntensity = kotlin.math.abs(sinRoll)
        if (bankIntensity > 0.22f) {
          val perpAng = renderHeadingRad + (if (sinRoll >= 0f) -PI.toFloat() * 0.5f else PI.toFloat() * 0.5f)
          val ridgeDist = 16f * RiftCalibrationState.shipScale * kotlin.math.abs(cosRoll).coerceAtLeast(0.32f)
          val gx = s.x + cos(perpAng) * ridgeDist
          val gy = s.y + sin(perpAng) * ridgeDist
          val halfSpan = 20f * RiftCalibrationState.shipScale
          shipGlintPaint.alpha = ((bankIntensity - 0.20f) * 230f).toInt().coerceIn(30, 195)
          canvas.drawLine(
            gx - cos(renderHeadingRad) * halfSpan,
            gy - sin(renderHeadingRad) * halfSpan,
            gx + cos(renderHeadingRad) * halfSpan,
            gy + sin(renderHeadingRad) * halfSpan,
            shipGlintPaint,
          )
        }
        if (s.barrelRollTimer > 0f) {
          val fade = sin(barrelProgress * PI.toFloat()).coerceIn(0f, 1f)
          val vortexR = 38f * RiftCalibrationState.shipScale
          arcRect.set(s.x - vortexR, s.y - vortexR, s.x + vortexR, s.y + vortexR)
          val spinDeg = (barrelRad * 180f / PI.toFloat()) + renderHeadingDeg
          vortexRibbonPaint.strokeWidth = 3.2f
          vortexRibbonPaint.color = Color.argb((215 * fade).toInt(), 0, 245, 212)
          canvas.drawArc(arcRect, spinDeg, 105f, false, vortexRibbonPaint)
          vortexRibbonPaint.color = Color.argb((205 * fade).toInt(), 250, 204, 21)
          canvas.drawArc(arcRect, spinDeg + 180f, 105f, false, vortexRibbonPaint)
        }
      }

      if (s.phaseDashTimer > 0f) {
        bombRingPaint.strokeWidth = 3.2f
        bombRingPaint.color = Color.argb(235, 0, 245, 212)
        canvas.drawCircle(s.x, s.y, 50f * RiftCalibrationState.shipScale, bombRingPaint)
        bombRingPaint.strokeWidth = 2.2f
      }
      if (s.sniperOverchargeReady) {
        bombRingPaint.color = Color.argb(210, 56, 189, 248)
        canvas.drawCircle(s.x, s.y, 46f * RiftCalibrationState.shipScale, bombRingPaint)
      }
      if (s.rocketBoostTimer > 0f) {
        bombRingPaint.color = Color.argb(215, 249, 115, 22)
        canvas.drawCircle(s.x, s.y, 48f * RiftCalibrationState.shipScale, bombRingPaint)
      }
      if (engine.isFluxCoreRulesActive() && engine.fluxCoreCarrierEntityId == s.id) {
        val pulse = 0.5f + 0.5f * sin(engine.fluxCoreOrb.pulsePhase * 1.6f)
        val tier = engine.fluxCoreOrb.overchargeTier.coerceIn(1, 10)
        val ringR = (54f + 8f * pulse) * RiftCalibrationState.shipScale
        anomalyFillPaint.color = Color.argb((42 + (34 * pulse).toInt()).coerceIn(0, 140), 250, 204, 21)
        canvas.drawCircle(s.x, s.y, ringR + 12f, anomalyFillPaint)
        bombRingPaint.strokeWidth = 3.8f
        bombRingPaint.color = Color.argb(235, 250, 204, 21)
        canvas.drawCircle(s.x, s.y, ringR, bombRingPaint)
        bombRingPaint.strokeWidth = 2.2f
        bombRingPaint.color = Color.argb(200, 34, 211, 238)
        canvas.drawCircle(s.x, s.y, ringR + 7f, bombRingPaint)

        val orbitAngle = engine.fluxCoreOrb.pulsePhase * 0.92f - PI.toFloat() * 0.5f
        val carriedOrbX = s.x + cos(orbitAngle) * ringR
        val carriedOrbY = s.y + sin(orbitAngle) * ringR
        val carriedOrbR = (16f + 2.2f * pulse) * RiftCalibrationState.shipScale.coerceIn(0.75f, 1.35f)
        drawFluxCoreOrbCoreAndHalo(
          canvas = canvas,
          cx = carriedOrbX,
          cy = carriedOrbY,
          r = carriedOrbR,
          pulsePhase = engine.fluxCoreOrb.pulsePhase,
        )

        drawWorldOverheadBanner(
          canvas = canvas,
          text = "FLUX CORE TIER $tier/10",
          worldX = s.x,
          worldY = s.y - ringR - 22f,
          fillColor = Color.parseColor("#FACC15"),
          worldScale = worldScale,
          sizeMultiplier = 1.18f,
        )
      }
      if (engine.isStasisBreachRulesActive() && s.isInStasisBrig) {
        bombRingPaint.strokeWidth = 2.6f
        bombRingPaint.color = Color.argb(210, 244, 63, 94)
        canvas.drawCircle(s.x, s.y, 46f * RiftCalibrationState.shipScale, bombRingPaint)
      }

      // Independent 360-degree turret/rail directional indicator on 2x hull
      if (s.independentAimActive) {
        val reticleScale = RiftCalibrationState.shipScale
        val tx1 = s.x + cos(s.weaponAngle) * (34f * reticleScale)
        val ty1 = s.y + sin(s.weaponAngle) * (34f * reticleScale)
        val tx2 = s.x + cos(s.weaponAngle) * (52f * reticleScale)
        val ty2 = s.y + sin(s.weaponAngle) * (52f * reticleScale)
        canvas.drawLine(tx1, ty1, tx2, ty2, turretReticlePaint)
      }

      // Pulsing Red MISSILE LOCK Reticle & Warning Tag on targeted ship
      if (s.missileLockWarningTimer > 0f) {
        val pulse = 0.5f + 0.5f * sin((System.currentTimeMillis() % 320L) / 320f * (PI.toFloat() * 2f))
        val lockR = (54f + 8f * pulse) * RiftCalibrationState.shipScale
        bombRingPaint.strokeWidth = 3.2f
        bombRingPaint.color = Color.argb((180 + (75 * pulse).toInt()).coerceIn(0, 255), 239, 68, 68)
        canvas.drawCircle(s.x, s.y, lockR, bombRingPaint)
        val bLen = 14f * RiftCalibrationState.shipScale
        val bx0 = s.x - lockR
        val by0 = s.y - lockR
        val bx1 = s.x + lockR
        val by1 = s.y + lockR
        canvas.drawLine(bx0, by0, bx0 + bLen, by0, bombRingPaint)
        canvas.drawLine(bx0, by0, bx0, by0 + bLen, bombRingPaint)
        canvas.drawLine(bx1 - bLen, by0, bx1, by0, bombRingPaint)
        canvas.drawLine(bx1, by0, bx1, by0 + bLen, bombRingPaint)
        canvas.drawLine(bx0, by1 - bLen, bx0, by1, bombRingPaint)
        canvas.drawLine(bx0, by1, bx0 + bLen, by1, bombRingPaint)
        canvas.drawLine(bx1 - bLen, by1, bx1, by1, bombRingPaint)
        canvas.drawLine(bx1, by1 - bLen, bx1, by1, bombRingPaint)
        bombRingPaint.strokeWidth = 2.2f

        if (overwatchLabelAlphaFactor > 0.02f) {
          popupPaint.color = Color.parseColor("#EF4444")
          popupPaint.alpha = ((195 + (60 * pulse).toInt()) * overwatchLabelAlphaFactor).roundToInt().coerceIn(0, 255)
          popupPaint.textSize = 14f * RiftCalibrationState.shipScale
          canvas.drawText("SEEKER LOCK", s.x, s.y - lockR - 8f, popupPaint)
          popupPaint.alpha = 255
        }
      }

      // 1) Customizable Ship Labels (Bounty, Tag, Name) with clock positioning and consolidated mode
      // Omit bounty and name of the player's ship; keep them for all other ships on the playfield.
      if (s.isLocalPlayer || overwatchLabelAlphaFactor <= 0.02f) {
        continue
      }

      val inSafe = engine.isInsideSafeZone(s.x, s.y)
      val labelAlpha = ((if (inSafe) 130 else 255) * overwatchLabelAlphaFactor).roundToInt().coerceIn(0, 255)
      val isHvt = s.bounty >= RiftCalibrationState.hvtBountyThreshold

      val safeWorldScale = worldScale.coerceAtLeast(0.15f)
      val baseTextScale = engine.playfieldTextScale * RiftCalibrationState.labelTextScale
      val bountyScreenPx =
        if (isHvt) RiftCalibrationState.bountyFontSizePx * 1.12f else RiftCalibrationState.bountyFontSizePx
      val bountyTextSize = (bountyScreenPx * 0.5f * dpDensity * baseTextScale) / safeWorldScale
      val callsignTextSize = (RiftCalibrationState.nametagFontSizePx * 0.5f * dpDensity * baseTextScale) / safeWorldScale
      val tagTextSize = (RiftCalibrationState.nametagFontSizePx * 0.94f * 0.5f * dpDensity * baseTextScale) / safeWorldScale

      // Project ship position to screen coordinates to prevent HUD console and controls overlap
      val screenX = (s.x - effectiveCamX) * worldScale + vw * 0.5f
      val screenY = (s.y - effectiveCamY) * worldScale + vh * 0.5f

      // 1. Bottom console bounding box: [vw/2 - 270dp .. vw/2 + 270dp], y >= vh - 84dp
      val inBottomConsole = (screenX >= vw * 0.5f - 270f * dpDensity &&
        screenX <= vw * 0.5f + 270f * dpDensity &&
        screenY >= vh - 92f * dpDensity)

      // 2. Top navigation bar & powerups dock: y <= 48dp, or y <= 75dp near center
      val inTopBar = screenY <= 52f * dpDensity ||
        (screenY <= 80f * dpDensity && screenX >= vw * 0.5f - 150f * dpDensity && screenX <= vw * 0.5f + 150f * dpDensity)

      // 3. Left thumbstick: x <= 160dp && y >= vh - 170dp
      val inLeftStick = screenX <= 160f * dpDensity && screenY >= vh - 170f * dpDensity

      // 4. Right combat buttons: x >= vw - 180dp && y >= vh - 220dp
      val inRightControls = screenX >= vw - 180f * dpDensity && screenY >= vh - 220f * dpDensity

      if (!isHubOverwatchMode && (inBottomConsole || inTopBar || inLeftStick || inRightControls)) {
        continue // Skip drawing nametag when projected under HUD buttons so ship names never bleed over MULTI or BRAKE STOP
      }

      val badgePrefix =
        when {
          s.armadaName.isNotEmpty() -> "[${s.armadaName}]"
          s.syndicateTag.isNotEmpty() && s.syndicateTag !in RiftGameEngine.LEGACY_FOUNDRY_TAGS -> "[${s.syndicateTag}]"
          else -> "[GUEST]"
        }
      val displayedResonance =
        when {
          s.isLocalPlayer ->
            engine.getShipResonance(s.shipId).let { if (it > 0) it else engine.liveResonance }
          s.isHuman -> s.liveResonanceRating
          else ->
            kotlin.math.max(
              s.liveResonanceRating,
              (s.kills * 16 + kotlin.math.max(0, s.bounty - 100) / 4 - s.deaths * 4).coerceAtLeast(0),
            )
        }
      val bountyText = if (isHvt) "HVT $displayedResonance RES" else "$displayedResonance RES"
      val teamColor =
        when {
          isFriendly -> Color.parseColor("#60A5FA")
          else -> Color.parseColor("#F87171")
        }
      val bountyColor = if (isHvt) Color.parseColor("#FFD700") else Color.parseColor("#FACC15")

      val radialDist = (44f * RiftCalibrationState.shipScale + 12f) * RiftCalibrationState.labelDistanceOffset

      val isConsolidated = (RiftCalibrationState.bountyPlacement == LabelPlacement.CONSOLIDATED_TOP ||
        RiftCalibrationState.bountyPlacement == LabelPlacement.CONSOLIDATED_BOTTOM ||
        RiftCalibrationState.tagPlacement == LabelPlacement.CONSOLIDATED_TOP ||
        RiftCalibrationState.tagPlacement == LabelPlacement.CONSOLIDATED_BOTTOM ||
        RiftCalibrationState.namePlacement == LabelPlacement.CONSOLIDATED_TOP ||
        RiftCalibrationState.namePlacement == LabelPlacement.CONSOLIDATED_BOTTOM)

      if (isConsolidated) {
        val isTop = (RiftCalibrationState.bountyPlacement == LabelPlacement.CONSOLIDATED_TOP ||
          RiftCalibrationState.tagPlacement == LabelPlacement.CONSOLIDATED_TOP ||
          RiftCalibrationState.namePlacement == LabelPlacement.CONSOLIDATED_TOP)
        val textY = if (isTop) s.y - radialDist else s.y + radialDist + callsignTextSize * 0.85f
        val lineText = "$badgePrefix ${s.callsign} • $bountyText"

        shipLabelStrokePaint.strokeWidth = (1.7f * dpDensity * baseTextScale) / safeWorldScale
        shipLabelStrokePaint.textSize = callsignTextSize
        shipLabelStrokePaint.alpha = labelAlpha
        shipLabelStrokePaint.textAlign = Paint.Align.CENTER

        shipLabelFillPaint.textSize = callsignTextSize
        shipLabelFillPaint.alpha = labelAlpha
        shipLabelFillPaint.textAlign = Paint.Align.CENTER
        shipLabelFillPaint.color = teamColor

        canvas.drawText(lineText, s.x, textY, shipLabelStrokePaint)
        canvas.drawText(lineText, s.x, textY, shipLabelFillPaint)
      } else {
        // Render each preset clock position (TOP, BOTTOM, LEFT, RIGHT)
        val clockPositions = arrayOf(LabelPlacement.TOP, LabelPlacement.BOTTOM, LabelPlacement.LEFT, LabelPlacement.RIGHT)
        for (pos in clockPositions) {
          // Gather items at this clock position
          var hasBounty = (RiftCalibrationState.bountyPlacement == pos)
          var hasTag = (RiftCalibrationState.tagPlacement == pos)
          var hasName = (RiftCalibrationState.namePlacement == pos)

          if (!hasBounty && !hasTag && !hasName) continue

          val items = mutableListOf<Triple<String, Float, Int>>() // text, size, color
          if (hasBounty) {
            items.add(Triple(bountyText, bountyTextSize, bountyColor))
          }
          if (hasTag && hasName) {
            items.add(Triple("$badgePrefix ${s.callsign}", callsignTextSize, teamColor))
          } else {
            if (hasTag) items.add(Triple(badgePrefix, tagTextSize, teamColor))
            if (hasName) items.add(Triple(s.callsign, callsignTextSize, teamColor))
          }

          val align = when (pos) {
            LabelPlacement.TOP, LabelPlacement.BOTTOM -> Paint.Align.CENTER
            LabelPlacement.LEFT -> Paint.Align.RIGHT
            LabelPlacement.RIGHT -> Paint.Align.LEFT
            else -> Paint.Align.CENTER
          }

          shipLabelStrokePaint.textAlign = align
          shipLabelFillPaint.textAlign = align
          shipLabelStrokePaint.strokeWidth = (1.7f * dpDensity * baseTextScale) / safeWorldScale
          shipLabelStrokePaint.alpha = labelAlpha
          shipLabelFillPaint.alpha = labelAlpha

          for (i in items.indices) {
            val (text, size, color) = items[i]
            var itemX = s.x
            var itemY = s.y

            when (pos) {
              LabelPlacement.TOP -> {
                itemX = s.x
                itemY = if (items.size == 1) s.y - radialDist else s.y - radialDist - (items.size - 1 - i) * (size * 0.95f)
              }
              LabelPlacement.BOTTOM -> {
                itemX = s.x
                itemY = if (items.size == 1) s.y + radialDist + size * 0.85f else s.y + radialDist + (i + 1) * (size * 0.88f)
              }
              LabelPlacement.LEFT -> {
                itemX = s.x - radialDist
                itemY = if (items.size == 1) s.y + size * 0.35f else s.y + (i - 0.5f * (items.size - 1)) * (size * 1.05f) + size * 0.35f
              }
              LabelPlacement.RIGHT -> {
                itemX = s.x + radialDist
                itemY = if (items.size == 1) s.y + size * 0.35f else s.y + (i - 0.5f * (items.size - 1)) * (size * 1.05f) + size * 0.35f
              }
              else -> {}
            }

            // HVT glow behind bounty number
            if (hasBounty && items[i].first == bountyText && isHvt && !inSafe) {
              val glowHalfW = 68f * baseTextScale
              nebulaDstRect.set(itemX - glowHalfW, itemY - 22f * baseTextScale, itemX + glowHalfW, itemY + 12f * baseTextScale)
              canvas.drawBitmap(RiftSpriteBaker.amberGlowStamp, null, nebulaDstRect, null)
            }

            shipLabelStrokePaint.textSize = size
            shipLabelFillPaint.textSize = size
            shipLabelFillPaint.color = color

            canvas.drawText(text, itemX, itemY, shipLabelStrokePaint)
            canvas.drawText(text, itemX, itemY, shipLabelFillPaint)
          }
        }
      }
    }
    shipLabelStrokePaint.alpha = 255
    shipLabelStrokePaint.textAlign = Paint.Align.CENTER
    shipLabelFillPaint.alpha = 255
    shipLabelFillPaint.textAlign = Paint.Align.CENTER

    // Directional Shield Impact Ripples
    for (sr in engine.shieldRipples) {
      val targetShip = engine.ships.firstOrNull { it.id == sr.shipId && it.respawnTimer <= 0f } ?: continue
      drawShieldImpactRipple(
        canvas = canvas,
        ship = targetShip,
        impactAngle = sr.impactAngle,
        intensity = ((sr.life / sr.maxLife).coerceIn(0f, 1f) * sr.intensity),
      )
    }

    // 11. Particles, Shockwaves & Object-Pooled FCT with Elastic Scale Overshoot & Quadratic Fade
    for (pt in engine.particles) {
      if (pt.x < viewLeft || pt.x > viewRight || pt.y < viewTop || pt.y > viewBottom) continue
      particlePaint.color = pt.color
      particlePaint.alpha = ((pt.life / pt.maxLife).coerceIn(0f, 1f) * 255f).toInt()
      canvas.drawRect(pt.x - pt.size * 0.5f, pt.y - pt.size * 0.5f, pt.x + pt.size * 0.5f, pt.y + pt.size * 0.5f, particlePaint)
    }
    particlePaint.alpha = 255

    for (sw in engine.shockwaves) {
      shockwavePaint.color = sw.color
      shockwavePaint.alpha = ((1f - sw.r / sw.maxR).coerceIn(0f, 1f) * 230f).toInt()
      canvas.drawCircle(sw.x, sw.y, sw.r * RiftCalibrationState.projectileScale, shockwavePaint)
    }
    shockwavePaint.alpha = 255

    if (overwatchLabelAlphaFactor > 0.02f) {
      val safeFctWorldScale = worldScale.coerceAtLeast(0.15f)
      for (fp in engine.floatingPopups) {
        if (!fp.active) continue
        val norm = (fp.elapsed / fp.maxLife).coerceIn(0f, 1f)
        val quadFade = (1f - norm * norm).coerceIn(0f, 1f)
        val fctAlpha = (quadFade * 255f * overwatchLabelAlphaFactor).roundToInt().coerceIn(0, 255)
        if (fctAlpha < 6) continue
        popupPaint.color = fp.color
        popupPaint.alpha = fctAlpha
        popupPaint.textSize =
          (RiftCalibrationState.fctFontSizePx * 0.5f * dpDensity * fp.scale * engine.playfieldTextScale * RiftCalibrationState.fctScale) / safeFctWorldScale
        canvas.drawText(fp.text, fp.x, fp.y, popupPaint)
      }
      popupPaint.alpha = 255
    }

    // 11a. Interactive Point-Light Test Orb (visible when spawned from NEXUS FORGE Studio Stage)
    val testOrbPos = RiftVisualForgeState.pointLightTestOrbWorldPos
    if (testOrbPos != null) {
      val lightRadius = RiftVisualForgeState.projectileLightRadius.coerceIn(120f, 900f)
      dynamicRimPaint.strokeWidth = 2.0f
      dynamicRimPaint.color = Color.argb(95, 250, 204, 21)
      canvas.drawCircle(testOrbPos.x, testOrbPos.y, lightRadius, dynamicRimPaint)
      anomalyFillPaint.color = Color.argb(38, 250, 204, 21)
      canvas.drawCircle(testOrbPos.x, testOrbPos.y, 44f, anomalyFillPaint)
      obstacleBeaconPaint.color = Color.parseColor("#FACC15")
      canvas.drawCircle(testOrbPos.x, testOrbPos.y, 16f, obstacleBeaconPaint)
      obstacleBeaconPaint.color = Color.WHITE
      canvas.drawCircle(testOrbPos.x, testOrbPos.y, 7f, obstacleBeaconPaint)
      drawWorldOverheadBanner(
        canvas = canvas,
        text = "POINT-LIGHT TEST ORB",
        worldX = testOrbPos.x,
        worldY = testOrbPos.y - 28f,
        fillColor = Color.parseColor("#FACC15"),
        worldScale = worldScale,
        sizeMultiplier = 1.0f,
        alpha = 235,
      )
    }

    // 11a-2. Practice Rift World-Space Golden Rift Target Outline Highlighter
    if (engine.isEphemeralPracticeRift && engine.practiceTutorialActive && !isHubOverwatchMode) {
      drawPracticeTutorialWorldHighlight(canvas, worldScale, tSec)
    }

    canvas.restore()
    if (clippedToOverwatchCircle) {
      canvas.restore()
    }

    // Suppress screen-space Arena HUD overlays behind NEXUS Master Window, Overwatch & modals (Law #4)
    if ((!isModalOverlayOpen && !isHubOverwatchMode) || RiftCalibrationState.isUiUnlockedForDragging) {
      // 11b. Screen-Space Ranked Mode Banner & Off-Screen Objective Indicator (Flux Core / Stasis Breach / Skirmish)
      drawRankedModeScreenOverlay(canvas, vw, vh, p, effectiveCamX, effectiveCamY, worldScale)

      // 12. Screen-Space Multi-Touch Controls (Always-Visible Left Stick + Strict Dynamic Right Combat Arc)
      drawTouchControlsOverlay(canvas, p)

      // 12b. Practice Rift Screen-Space Golden Rift Control Outline & Offscreen Target Pointer
      if (engine.isEphemeralPracticeRift && engine.practiceTutorialActive && !RiftCalibrationState.isUiUnlockedForDragging) {
        drawPracticeTutorialScreenHighlight(canvas, vw, vh, p, effectiveCamX, effectiveCamY, worldScale, tSec)
      }

      // 13. Stationary Death Camera 3-Second Respawn Countdown Overlay
      if (p.respawnTimer > 0f) {
        drawStationaryRespawnBanner(canvas, vw, vh, p.respawnTimer)
      }
    }

    // Schedule next 60+ FPS frame only while window is visible and app is foregrounded
    if (isWindowVisibleForRender && RiftSoundSynthesizer.isAppCurrentlyForeground()) {
      postInvalidateOnAnimation()
    }
  }

  private fun drawStasisBreachPlayfieldStructures(
    canvas: Canvas,
    p: ShipEntity,
    viewLeft: Float,
    viewRight: Float,
    viewTop: Float,
    viewBottom: Float,
  ) {
    // 1. Vertical Sector Frontier Line at x = 4000f
    if (4000f in (viewLeft - 60f)..(viewRight + 60f)) {
      safeDashPaint.color = Color.argb(135, 244, 63, 94)
      safeDashPaint.strokeWidth = 3.2f
      canvas.drawLine(4000f, max(0f, viewTop), 4000f, min(RiftGameEngine.WORLD_SIZE, viewBottom), safeDashPaint)
      safeDashPaint.strokeWidth = 2f
    }

    val maxReactorHp = RiftCalibrationState.stasisBrigReactorMaxHp.coerceAtLeast(1000f)
    val eastPrisonersInWestBrig = engine.ships.count { it.freq == 1 && it.isInStasisBrig }
    val westPrisonersInEastBrig = engine.ships.count { it.freq == 0 && it.isInStasisBrig }
    val jailR = engine.stasisJailRadius
    val spinPhase = ((System.currentTimeMillis() % 4800L) / 4800f) * (PI.toFloat() * 2f)

    fun drawCircularStasisJailAndCore(
      reactorX: Float,
      reactorY: Float,
      reactorHp: Float,
      brigOwnerFreq: Int,
      capturedCount: Int,
      labelPrefix: String,
    ) {
      if (reactorX + jailR < viewLeft - 90f || reactorX - jailR > viewRight + 90f ||
        reactorY + jailR < viewTop - 90f || reactorY - jailR > viewBottom + 90f
      ) {
        return
      }
      val isFriendlyBrig = p.freq == brigOwnerFreq
      val borderCol = if (isFriendlyBrig) Color.parseColor("#38BDF8") else Color.parseColor("#F43F5E")
      val midCol = if (isFriendlyBrig) Color.parseColor("#00F5D4") else Color.parseColor("#FB7185")

      // 1. 3D Layered Stasis Containment Vortex Field
      anomalyFillPaint.color =
        if (brigOwnerFreq == 0) Color.argb(26, 56, 189, 248) else Color.argb(26, 244, 63, 94)
      canvas.drawCircle(reactorX, reactorY, jailR * 1.06f, anomalyFillPaint)
      anomalyFillPaint.color = Color.argb(145, 4, 10, 22)
      canvas.drawCircle(reactorX, reactorY, jailR, anomalyFillPaint)

      // 2. Counter-Rotating 3D Spiral Stasis Filaments across the Jail Dome
      anomalyRingPaint.strokeWidth = 2.2f
      anomalyRingPaint.color = Color.argb(75, Color.red(borderCol), Color.green(borderCol), Color.blue(borderCol))
      for (arm in 0 until 6) {
        val baseA = -spinPhase * 0.8f + arm * (PI.toFloat() / 3f)
        val x0 = reactorX + cos(baseA) * (jailR * 0.22f)
        val y0 = reactorY + sin(baseA) * (jailR * 0.22f)
        val xm = reactorX + cos(baseA + 0.42f) * (jailR * 0.62f)
        val ym = reactorY + sin(baseA + 0.42f) * (jailR * 0.62f)
        val x1 = reactorX + cos(baseA + 0.82f) * (jailR * 0.94f)
        val y1 = reactorY + sin(baseA + 0.82f) * (jailR * 0.94f)
        canvas.drawLine(x0, y0, xm, ym, anomalyRingPaint)
        canvas.drawLine(xm, ym, x1, y1, anomalyRingPaint)
      }

      // 3. Rotating Segmented Beveled Containment Arcs + 24 Degree Ticks
      val innerR = jailR * 0.94f
      arcRect.set(reactorX - innerR, reactorY - innerR, reactorX + innerR, reactorY + innerR)
      anomalyRingPaint.strokeWidth = 3.2f
      anomalyRingPaint.color = Color.argb(185, Color.red(midCol), Color.green(midCol), Color.blue(midCol))
      val degOffset = spinPhase * 180f / PI.toFloat()
      for (seg in 0 until 8) {
        canvas.drawArc(arcRect, degOffset + seg * 45f, 28f, false, anomalyRingPaint)
      }
      anomalyRingPaint.strokeWidth = 1.6f
      for (t in 0 until 24) {
        val ta = t * (PI.toFloat() / 12f)
        canvas.drawLine(
          reactorX + cos(ta) * (jailR * 0.96f),
          reactorY + sin(ta) * (jailR * 0.96f),
          reactorX + cos(ta) * jailR,
          reactorY + sin(ta) * jailR,
          anomalyRingPaint,
        )
      }

      // 4. Crisp Outer 3D Stasis Perimeter Ring + 12 Beveled Emitter Pylons
      anomalyRingPaint.strokeWidth = 5.0f
      anomalyRingPaint.color = borderCol
      canvas.drawCircle(reactorX, reactorY, jailR, anomalyRingPaint)
      for (i in 0 until 12) {
        val na = spinPhase * 0.5f + i * (PI.toFloat() * 2f / 12f)
        val nx = reactorX + cos(na) * jailR
        val ny = reactorY + sin(na) * jailR
        anomalyCorePaint.color = borderCol
        canvas.drawCircle(nx, ny, 7.2f, anomalyCorePaint)
        anomalyCorePaint.color = Color.WHITE
        canvas.drawCircle(nx, ny, 3.2f, anomalyCorePaint)
      }

      val curWorldScale = if (isHubOverwatchMode) currentRenderWorldScale else engine.effectiveCameraScale()
      drawWorldOverheadBanner(
        canvas = canvas,
        text = "$labelPrefix STASIS RIFT JAIL • $capturedCount/10 CAPTURED",
        worldX = reactorX,
        worldY = reactorY - jailR - 20f,
        fillColor = borderCol,
        worldScale = curWorldScale,
        sizeMultiplier = 1.20f,
      )

      // 5. Center 8,000 Flux 3D Stasis Singularity Core (coreR = 54f) with HP Bar & Breakout Instructions
      val coreR = 54f
      draw3dPlayfieldRift(
        canvas = canvas,
        cx = reactorX,
        cy = reactorY,
        r = coreR * 1.35f,
        spinAngle = spinPhase * 2.2f,
        fadeAlpha = 1.0f,
        outerColor = if (isFriendlyBrig) Color.rgb(56, 189, 248) else Color.rgb(245, 158, 11),
        midColor = Color.rgb(250, 204, 21),
        coreColor = Color.rgb(255, 255, 255),
        darkVoidCenter = true,
        inwardSpiral = true,
      )

      val hpFrac = (reactorHp / maxReactorHp).coerceIn(0f, 1f)
      val barW = 168f
      val barH = 12f
      val barLeft = reactorX - barW * 0.5f
      val barTop = reactorY - coreR * 1.35f - 28f
      arcRect.set(barLeft, barTop, barLeft + barW, barTop + barH)
      hudCircleFillPaint.color = Color.argb(225, 10, 18, 34)
      canvas.drawRoundRect(arcRect, 5f, 5f, hudCircleFillPaint)
      arcRect.set(barLeft, barTop, barLeft + barW * hpFrac, barTop + barH)
      hudCircleFillPaint.color =
        if (hpFrac > 0.5f) Color.parseColor("#22D3EE")
        else if (hpFrac > 0.25f) Color.parseColor("#F59E0B")
        else Color.parseColor("#EF4444")
      canvas.drawRoundRect(arcRect, 5f, 5f, hudCircleFillPaint)

      drawWorldOverheadBanner(
        canvas = canvas,
        text = "STASIS RIFT CORE: ${reactorHp.toInt()} / ${maxReactorHp.toInt()} FLUX (SHOOT TO BREAK OUT)",
        worldX = reactorX,
        worldY = barTop - 10f,
        fillColor = Color.parseColor("#FACC15"),
        worldScale = curWorldScale,
        sizeMultiplier = 1.08f,
      )
    }

    drawCircularStasisJailAndCore(
      reactorX = engine.westBrigReactorX,
      reactorY = engine.westBrigReactorY,
      reactorHp = engine.westBrigGeneratorHp,
      brigOwnerFreq = 0,
      capturedCount = eastPrisonersInWestBrig,
      labelPrefix = "WEST",
    )
    drawCircularStasisJailAndCore(
      reactorX = engine.eastBrigReactorX,
      reactorY = engine.eastBrigReactorY,
      reactorHp = engine.eastBrigGeneratorHp,
      brigOwnerFreq = 1,
      capturedCount = westPrisonersInEastBrig,
      labelPrefix = "EAST",
    )
  }

  private fun drawRiftControlZones(
    canvas: Canvas,
    p: ShipEntity,
    viewLeft: Float,
    viewRight: Float,
    viewTop: Float,
    viewBottom: Float,
  ) {
    val zoneR = engine.riftCaptureRadius
    val pulse = 0.5f + 0.5f * sin((System.currentTimeMillis() % 900L) / 900f * (PI.toFloat() * 2f))
    val spinPhase = ((System.currentTimeMillis() % 4200L) / 4200f) * (PI.toFloat() * 2f)

    fun drawSingleControlZone(
      cx: Float,
      cy: Float,
      controlSec: Float,
      lockedBy: Int,
      westCount: Int,
      eastCount: Int,
      zoneTitle: String,
    ) {
      if (cx + zoneR < viewLeft - 80f || cx - zoneR > viewRight + 80f ||
        cy + zoneR < viewTop - 80f || cy - zoneR > viewBottom + 80f
      ) {
        return
      }

      val isContested = westCount > 0 && eastCount > 0 && westCount == eastCount
      val isWestControl = lockedBy == 0 || (lockedBy == -1 && controlSec < -0.1f)
      val isEastControl = lockedBy == 1 || (lockedBy == -1 && controlSec > 0.1f)
      val primaryColor =
        when {
          isContested -> Color.parseColor("#F59E0B")
          lockedBy == 0 -> Color.parseColor("#38BDF8")
          lockedBy == 1 -> Color.parseColor("#F43F5E")
          isWestControl -> Color.parseColor("#38BDF8")
          isEastControl -> Color.parseColor("#F43F5E")
          else -> Color.parseColor("#00F5D4")
        }
      val secondaryColor =
        when {
          lockedBy != -1 -> Color.parseColor("#FACC15")
          isContested -> Color.parseColor("#FB7185")
          else -> Color.parseColor("#38BDF8")
        }

      // 1. 3D Volumetric Rift Capture Horizon Fill
      anomalyFillPaint.color =
        Color.argb(
          if (lockedBy != -1 || isContested) 46 else (26 + (14 * pulse).toInt()),
          Color.red(primaryColor),
          Color.green(primaryColor),
          Color.blue(primaryColor),
        )
      canvas.drawCircle(cx, cy, zoneR, anomalyFillPaint)

      // 2. Swirling 3D Accretion Arms inside the Capture Ring
      anomalyRingPaint.strokeWidth = 2.4f
      anomalyRingPaint.color = Color.argb(88, Color.red(primaryColor), Color.green(primaryColor), Color.blue(primaryColor))
      for (arm in 0 until 6) {
        val a = spinPhase + arm * (PI.toFloat() / 3f)
        val x0 = cx + cos(a) * (zoneR * 0.28f)
        val y0 = cy + sin(a) * (zoneR * 0.28f)
        val xm = cx + cos(a + 0.45f) * (zoneR * 0.64f)
        val ym = cy + sin(a + 0.45f) * (zoneR * 0.64f)
        val x1 = cx + cos(a + 0.85f) * (zoneR * 0.94f)
        val y1 = cy + sin(a + 0.85f) * (zoneR * 0.94f)
        canvas.drawLine(x0, y0, xm, ym, anomalyRingPaint)
        canvas.drawLine(xm, ym, x1, y1, anomalyRingPaint)
      }

      // 3. Beveled Outer Capture Boundary Ring + 24 Degree Ticks
      anomalyRingPaint.strokeWidth = if (isContested) 4.4f else 3.4f
      anomalyRingPaint.color = Color.argb(185, Color.red(primaryColor), Color.green(primaryColor), Color.blue(primaryColor))
      canvas.drawCircle(cx, cy, zoneR, anomalyRingPaint)
      anomalyRingPaint.strokeWidth = 1.8f
      for (t in 0 until 24) {
        val ta = t * (PI.toFloat() / 12f)
        canvas.drawLine(
          cx + cos(ta) * (zoneR * 0.95f),
          cy + sin(ta) * (zoneR * 0.95f),
          cx + cos(ta) * zoneR,
          cy + sin(ta) * zoneR,
          anomalyRingPaint,
        )
      }

      // 4. 360-Degree Segmented Tug-of-War Progress Arc (sweepAngle = (abs(controlSec) / 30f) * 360f)
      val progressFrac = if (lockedBy != -1) 1f else (abs(controlSec) / 30f).coerceIn(0f, 1f)
      if (progressFrac > 0.01f) {
        arcRect.set(cx - zoneR, cy - zoneR, cx + zoneR, cy + zoneR)
        anomalyRingPaint.strokeWidth = 7.5f
        anomalyRingPaint.color = if (lockedBy != -1) Color.parseColor("#FACC15") else primaryColor
        canvas.drawArc(arcRect, -90f, progressFrac * 360f, false, anomalyRingPaint)
      }

      // 5. Center 3D Rift Singularity Vortex Beacon
      val beaconR = 78f + 6f * pulse
      draw3dPlayfieldRift(
        canvas = canvas,
        cx = cx,
        cy = cy,
        r = beaconR,
        spinAngle = spinPhase * 1.8f,
        fadeAlpha = 1.0f,
        outerColor = primaryColor,
        midColor = secondaryColor,
        coreColor = if (lockedBy != -1) Color.rgb(250, 204, 21) else Color.rgb(255, 255, 255),
        darkVoidCenter = true,
        inwardSpiral = true,
      )

      // 6. Status Text above Rift Zone
      val pct = ((abs(controlSec) / 30f) * 100f).toInt().coerceIn(0, 100)
      val statusText =
        when {
          lockedBy == 0 -> "$zoneTitle • CAPTURED BY WEST ($pct%)"
          lockedBy == 1 -> "$zoneTitle • CAPTURED BY EAST ($pct%)"
          isContested -> "$zoneTitle • CONTESTED (WEST $westCount vs EAST $eastCount)"
          controlSec < -0.2f -> "$zoneTitle • WEST CAPTURING $pct% (${abs(controlSec).toInt()}s / 30s)"
          controlSec > 0.2f -> "$zoneTitle • EAST CAPTURING $pct% (${abs(controlSec).toInt()}s / 30s)"
          else -> "$zoneTitle • OPEN OBJECTIVE (FLY INSIDE RING TO CAPTURE)"
        }
      val statusCol =
        when {
          isContested -> Color.parseColor("#FACC15")
          lockedBy != -1 -> Color.parseColor("#FACC15")
          else -> primaryColor
        }
      val curWorldScale = if (isHubOverwatchMode) currentRenderWorldScale else engine.effectiveCameraScale()
      drawWorldOverheadBanner(
        canvas = canvas,
        text = statusText,
        worldX = cx,
        worldY = cy - zoneR - 20f,
        fillColor = statusCol,
        worldScale = curWorldScale,
        sizeMultiplier = 1.20f,
      )

      // 7. Live Ship Count Sub-Label below Center 3D Rift Beacon
      drawWorldOverheadBanner(
        canvas = canvas,
        text = "WEST: $westCount  |  EAST: $eastCount",
        worldX = cx,
        worldY = cy + beaconR + 28f,
        fillColor = Color.parseColor("#E2E8F0"),
        worldScale = curWorldScale,
        sizeMultiplier = 1.05f,
      )
    }

    drawSingleControlZone(
      cx = engine.riftAlphaX,
      cy = engine.riftAlphaY,
      controlSec = engine.riftAlphaControlSec,
      lockedBy = engine.riftAlphaLockedBy,
      westCount = engine.riftAlphaWestCount,
      eastCount = engine.riftAlphaEastCount,
      zoneTitle = "RIFT ALPHA",
    )
    drawSingleControlZone(
      cx = engine.riftOmegaX,
      cy = engine.riftOmegaY,
      controlSec = engine.riftOmegaControlSec,
      lockedBy = engine.riftOmegaLockedBy,
      westCount = engine.riftOmegaWestCount,
      eastCount = engine.riftOmegaEastCount,
      zoneTitle = "RIFT OMEGA",
    )
  }

  /**
   * Renders a 4-Layer 3D Swirling Rift Singularity on the playfield Canvas:
   * - Layer 1: Volumetric outer radial accretion aura + deep-space event horizon backdrop
   * - Layer 2: Beveled 3D outer containment torus ring with 16 degree ticks & counter-rotating inner arc
   * - Layer 3: Multi-layered 3D logarithmic spiral plasma arms (outer glow + bright inner core filaments)
   * - Layer 4: 3D Event-Horizon Singularity Core with specular photon ring
   */
  private fun draw3dPlayfieldRift(
    canvas: Canvas,
    cx: Float,
    cy: Float,
    r: Float,
    spinAngle: Float,
    fadeAlpha: Float,
    outerColor: Int,
    midColor: Int,
    coreColor: Int,
    darkVoidCenter: Boolean,
    inwardSpiral: Boolean,
  ) {
    RiftSpriteBaker.draw3dPlayfieldRiftColors(
      canvas = canvas,
      cx = cx,
      cy = cy,
      r = r,
      spinAngle = spinAngle,
      fadeAlpha = fadeAlpha,
      outerColor = outerColor,
      midColor = midColor,
      coreColor = coreColor,
      darkVoidCenter = darkVoidCenter,
      inwardSpiral = inwardSpiral,
    )
  }

  private fun drawFluxCoreOrbCoreAndHalo(
    canvas: Canvas,
    cx: Float,
    cy: Float,
    r: Float,
    pulsePhase: Float,
  ) {
    val coreRec = RiftVisualForgeState.getRecipe(ForgeElementTarget.FLUX_CORE_ORB)
    val pulse = 0.5f + 0.5f * sin(pulsePhase)
    val glowMult = coreRec.glowIntensity.coerceIn(0.2f, 1.8f)
    val borderMult = coreRec.borderWeight.coerceIn(0.5f, 2.2f)
    val auraBaseCol =
      if (coreRec.isFactoryDefault) Color.rgb(250, 204, 21) else coreRec.glowColorInt
    val trimCol =
      if (coreRec.isFactoryDefault) Color.parseColor("#FACC15") else coreRec.trimColorInt

    val haloAlpha = ((75f + 50f * pulse) * glowMult).roundToInt().coerceIn(0, 235)
    anomalyFillPaint.color =
      Color.argb(
        haloAlpha,
        Color.red(auraBaseCol),
        Color.green(auraBaseCol),
        Color.blue(auraBaseCol),
      )
    canvas.drawCircle(cx, cy, r * 1.56f, anomalyFillPaint)

    anomalyRingPaint.strokeWidth = (r * 0.105f * borderMult).coerceAtLeast(1.6f)
    anomalyRingPaint.color = Color.parseColor("#22D3EE")
    canvas.drawCircle(cx, cy, r * 1.04f, anomalyRingPaint)

    anomalyRingPaint.strokeWidth = (r * 0.078f * borderMult).coerceAtLeast(1.3f)
    anomalyRingPaint.color = trimCol
    canvas.drawCircle(cx, cy, r * 0.91f, anomalyRingPaint)

    canvas.save()
    canvas.translate(cx, cy)
    canvas.rotate((pulsePhase * 22f) % 360f)
    RiftSpriteBaker.drawFluxPrismIcon(
      c = canvas,
      cx = 0f,
      cy = 0f,
      r = r * 0.80f,
      alpha = 255,
    )
    canvas.restore()
  }

  private fun drawFreeFluxCoreOrb(
    canvas: Canvas,
    viewLeft: Float,
    viewRight: Float,
    viewTop: Float,
    viewBottom: Float,
  ) {
    val orb = engine.fluxCoreOrb
    if (orb.x < viewLeft - 140f || orb.x > viewRight + 140f ||
      orb.y < viewTop - 140f || orb.y > viewBottom + 140f
    ) {
      return
    }
    val coreRec = RiftVisualForgeState.getRecipe(ForgeElementTarget.FLUX_CORE_ORB)
    val pulse = 0.5f + 0.5f * sin(orb.pulsePhase)
    val r = (38f + 6f * pulse) * coreRec.pixelScale.coerceIn(0.65f, 1.45f)

    if (orb.ejectDecelTimerSec > 0f && hypot(orb.vx, orb.vy) > 25f) {
      val tailX = orb.x - orb.vx * 0.085f
      val tailY = orb.y - orb.vy * 0.085f
      bulletPaint.color = Color.argb(190, 250, 204, 21)
      bulletPaint.strokeWidth = 14f
      canvas.drawLine(orb.x, orb.y, tailX, tailY, bulletPaint)
      bulletCorePaint.strokeWidth = 6f
      canvas.drawLine(orb.x, orb.y, tailX, tailY, bulletCorePaint)
    }

    drawFluxCoreOrbCoreAndHalo(
      canvas = canvas,
      cx = orb.x,
      cy = orb.y,
      r = r,
      pulsePhase = orb.pulsePhase,
    )

    val statusLbl =
      if (orb.ejectDecelTimerSec > 0f) {
        "FLUX CORE EJECTED (${orb.ejectDecelTimerSec.toInt()}s)"
      } else {
        "FLUX CORE SINGULARITY (+15 BOUNTY/S)"
      }
    drawWorldOverheadBanner(
      canvas = canvas,
      text = statusLbl,
      worldX = orb.x,
      worldY = orb.y - r - 18f,
      fillColor = Color.parseColor("#FACC15"),
      worldScale = if (isHubOverwatchMode) currentRenderWorldScale else engine.effectiveCameraScale(),
      sizeMultiplier = 1.18f,
    )
  }

  /**
   * Renders enlarged, zoom-compensated world overhead text banners above Playfield Rifts,
   * Stasis Bubbles, Flux Core Carriers, Rift Control Zones, and Stasis Breach Jails.
   * Hidden cleanly below `1.55x` Hub Overwatch zoom (`overwatchLabelAlphaFactor`) and fades in as you zoom closer.
   */
  private fun drawWorldOverheadBanner(
    canvas: Canvas,
    text: String,
    worldX: Float,
    worldY: Float,
    fillColor: Int,
    worldScale: Float,
    sizeMultiplier: Float = 1.0f,
    alpha: Int = 255,
  ) {
    val clampedAlpha = (alpha.coerceIn(0, 255) * overwatchLabelAlphaFactor).roundToInt().coerceIn(0, 255)
    if (clampedAlpha < 6) return

    val safeScale = worldScale.coerceAtLeast(0.15f)
    val baseTextScale = engine.playfieldTextScale * RiftCalibrationState.labelTextScale
    val effectiveSizePx =
      (RiftCalibrationState.nametagFontSizePx * 0.68f * dpDensity * baseTextScale * sizeMultiplier) / safeScale

    shipLabelStrokePaint.strokeWidth = (2.4f * dpDensity * baseTextScale) / safeScale
    shipLabelStrokePaint.textSize = effectiveSizePx
    shipLabelStrokePaint.alpha = clampedAlpha
    shipLabelStrokePaint.textAlign = Paint.Align.CENTER

    shipLabelFillPaint.textSize = effectiveSizePx
    shipLabelFillPaint.color = fillColor
    shipLabelFillPaint.alpha = clampedAlpha
    shipLabelFillPaint.textAlign = Paint.Align.CENTER

    canvas.drawText(text, worldX, worldY, shipLabelStrokePaint)
    canvas.drawText(text, worldX, worldY, shipLabelFillPaint)
  }

  private fun drawRankedModeScreenOverlay(
    canvas: Canvas,
    vw: Float,
    vh: Float,
    p: ShipEntity,
    effectiveCamX: Float,
    effectiveCamY: Float,
    worldScale: Float,
  ) {
    // Off-Screen Directional Indicator for Free/Unclaimed Flux Core Orb ONLY (top banner is rendered cleanly by Compose RiftHudOverlay)
    if (engine.isFluxCoreRulesActive() && engine.fluxCoreCarrierEntityId.isEmpty()) {
      val tx = engine.fluxCoreOrb.x
      val ty = engine.fluxCoreOrb.y
      val sx = (tx - effectiveCamX) * worldScale + vw * 0.5f
      val sy = (ty - effectiveCamY) * worldScale + vh * 0.5f
      if (sx < 40f || sx > vw - 40f || sy < 40f || sy > vh - 40f) {
        val ang = atan2(ty - effectiveCamY, tx - effectiveCamX)
        val edgeR = min(vw, vh) * 0.38f
        val ix = vw * 0.5f + cos(ang) * edgeR
        val iy = vh * 0.5f + sin(ang) * edgeR
        hudCircleFillPaint.color = Color.argb(225, 10, 18, 34)
        canvas.drawCircle(ix, iy, 13f * dpDensity, hudCircleFillPaint)
        hudCircleStrokePaint.strokeWidth = 1.8f * dpDensity
        hudCircleStrokePaint.color = Color.parseColor("#FFD700")
        canvas.drawCircle(ix, iy, 13f * dpDensity, hudCircleStrokePaint)
        hudCircleStrokePaint.strokeWidth = 2.6f
        RiftSpriteBaker.drawFluxPrismIcon(canvas, ix, iy, 7.2f * dpDensity, alpha = 255)
      }
    }

    // Off-Screen Directional Pointers for RIFT ALPHA & RIFT OMEGA in RIFT_CONTROL
    if (engine.isRiftControlRulesActive()) {
      fun drawRiftPointer(
        tx: Float,
        ty: Float,
        controlSec: Float,
        lockedBy: Int,
        westCnt: Int,
        eastCnt: Int,
        shortName: String,
      ) {
        val sx = (tx - effectiveCamX) * worldScale + vw * 0.5f
        val sy = (ty - effectiveCamY) * worldScale + vh * 0.5f
        if (sx < 56f || sx > vw - 56f || sy < 56f || sy > vh - 56f) {
          val ang = atan2(ty - effectiveCamY, tx - effectiveCamX)
          val edgeRx = (vw * 0.5f - 68f * dpDensity).coerceAtLeast(80f)
          val edgeRy = (vh * 0.5f - 58f * dpDensity).coerceAtLeast(60f)
          val ix = (vw * 0.5f + cos(ang) * edgeRx).coerceIn(58f * dpDensity, vw - 58f * dpDensity)
          val iy = (vh * 0.5f + sin(ang) * edgeRy).coerceIn(48f * dpDensity, vh - 86f * dpDensity)
          val distM = (hypot(tx - p.x, ty - p.y) / 10f).toInt()
          val isContested = westCnt > 0 && eastCnt > 0 && westCnt == eastCnt
          val badgeColor =
            when {
              isContested -> Color.parseColor("#F59E0B")
              lockedBy == 0 || controlSec < -0.2f -> Color.parseColor("#38BDF8")
              lockedBy == 1 || controlSec > 0.2f -> Color.parseColor("#EF4444")
              else -> Color.parseColor("#A855F7")
            }
          val pillW = 48f * dpDensity
          val pillH = 13f * dpDensity
          arcRect.set(ix - pillW, iy - pillH, ix + pillW, iy + pillH)
          hudCircleFillPaint.color = Color.argb(215, 7, 14, 28)
          canvas.drawRoundRect(arcRect, 6f * dpDensity, 6f * dpDensity, hudCircleFillPaint)
          anomalyRingPaint.strokeWidth = 1.6f * dpDensity
          anomalyRingPaint.color = badgeColor
          canvas.drawRoundRect(arcRect, 6f * dpDensity, 6f * dpDensity, anomalyRingPaint)

          val arrowTipX = ix + cos(ang) * (pillW + 7f * dpDensity)
          val arrowTipY = iy + sin(ang) * (pillH + 7f * dpDensity)
          hudCircleFillPaint.color = badgeColor
          canvas.drawCircle(arrowTipX, arrowTipY, 3.6f * dpDensity, hudCircleFillPaint)

          hudLabelPaint.textSize = 8.2f * dpDensity
          hudLabelPaint.color = badgeColor
          canvas.drawText("$shortName • ${distM}m", ix, iy + 3f * dpDensity, hudLabelPaint)
        }
      }

      drawRiftPointer(
        tx = engine.riftAlphaX,
        ty = engine.riftAlphaY,
        controlSec = engine.riftAlphaControlSec,
        lockedBy = engine.riftAlphaLockedBy,
        westCnt = engine.riftAlphaWestCount,
        eastCnt = engine.riftAlphaEastCount,
        shortName = "ALPHA",
      )
      drawRiftPointer(
        tx = engine.riftOmegaX,
        ty = engine.riftOmegaY,
        controlSec = engine.riftOmegaControlSec,
        lockedBy = engine.riftOmegaLockedBy,
        westCnt = engine.riftOmegaWestCount,
        eastCnt = engine.riftOmegaEastCount,
        shortName = "OMEGA",
      )
    }
  }

  private fun drawExtrudedBox3D(
    canvas: Canvas,
    left: Float,
    top: Float,
    right: Float,
    bottom: Float,
    ex: Float,
    ey: Float,
    sideDarkColor: Int,
    sideLitColor: Int,
  ) {
    val tLeft = left + ex
    val tTop = top + ey
    val tRight = right + ex
    val tBottom = bottom + ey

    obstacleSideDarkPaint.color = sideDarkColor
    obstacleSideLitPaint.color = sideLitColor

    if (ey > 0.5f) {
      obstaclePath.reset()
      obstaclePath.moveTo(left, top)
      obstaclePath.lineTo(right, top)
      obstaclePath.lineTo(tRight, tTop)
      obstaclePath.lineTo(tLeft, tTop)
      obstaclePath.close()
      canvas.drawPath(obstaclePath, obstacleSideLitPaint)
    } else if (ey < -0.5f) {
      obstaclePath.reset()
      obstaclePath.moveTo(left, bottom)
      obstaclePath.lineTo(right, bottom)
      obstaclePath.lineTo(tRight, tBottom)
      obstaclePath.lineTo(tLeft, tBottom)
      obstaclePath.close()
      canvas.drawPath(obstaclePath, obstacleSideDarkPaint)
    }

    if (ex > 0.5f) {
      obstaclePath.reset()
      obstaclePath.moveTo(left, top)
      obstaclePath.lineTo(left, bottom)
      obstaclePath.lineTo(tLeft, tBottom)
      obstaclePath.lineTo(tLeft, tTop)
      obstaclePath.close()
      canvas.drawPath(obstaclePath, obstacleSideLitPaint)
    } else if (ex < -0.5f) {
      obstaclePath.reset()
      obstaclePath.moveTo(right, top)
      obstaclePath.lineTo(right, bottom)
      obstaclePath.lineTo(tRight, tBottom)
      obstaclePath.lineTo(tRight, tTop)
      obstaclePath.close()
      canvas.drawPath(obstaclePath, obstacleSideDarkPaint)
    }
  }

  private fun drawCircularNexusSafeRift(
    canvas: Canvas,
    p: ShipEntity,
    worldScale: Float,
    tSec: Float,
    viewLeft: Float,
    viewRight: Float,
    viewTop: Float,
    viewBottom: Float,
  ) {
    val szPadRec = RiftVisualForgeState.getRecipe(ForgeElementTarget.SAFE_ZONE_PAD)
    val szCX = engine.arenaCenterX
    val szCY = engine.arenaCenterY
    val szR = engine.getSafeZoneRadius() * szPadRec.pixelScale.coerceIn(0.6f, 1.6f)
    if (szCX + szR * 1.35f < viewLeft || szCX - szR * 1.35f > viewRight || szCY + szR * 1.35f < viewTop || szCY - szR * 1.35f > viewBottom) {
      return
    }

    val isLockedOut = p.safeZoneLockoutTimer > 0f && p.respawnTimer <= 0f
    val padGlow = VisualElementRecipe.parseHexColorSafe(szPadRec.glowHex, Color.rgb(0, 245, 212))
    val padTrim = VisualElementRecipe.parseHexColorSafe(szPadRec.trimHex, Color.rgb(56, 189, 248))
    val opScale = szPadRec.surfaceOpacity.coerceIn(0.30f, 1.0f)

    // Layer 1: Calm #030A16 Sanctuary Inside (r < szR) + Collapsing Outer Rift Storm Tendrils Pressing Inward Outside (r in szR..szR*1.30f)
    var drewAgsl = false
    if (Build.VERSION.SDK_INT >= 33) {
      val rt = safeNexusRuntimeShader as? RuntimeShader
      if (rt != null) {
        rt.setFloatUniform("uCenter", szCX, szCY)
        rt.setFloatUniform("uRadius", szR)
        rt.setFloatUniform("uTime", tSec)
        rt.setFloatUniform("uLocked", if (isLockedOut) 1.0f else 0.0f)
        rt.setFloatUniform("uOpacity", opScale)
        safeNexusShaderPaint.shader = rt
        canvas.drawCircle(szCX, szCY, szR * 1.32f, safeNexusShaderPaint)
        drewAgsl = true
      }
    }
    if (!drewAgsl) {
      // Fallback outer collapsing Rift collar pressing inward against szR
      anomalyRingPaint.strokeWidth = (szR * 0.22f).coerceAtLeast(18f)
      anomalyRingPaint.color =
        if (isLockedOut) Color.argb(66, 239, 68, 68) else Color.argb((62 * opScale).toInt().coerceIn(22, 110), 168, 85, 247)
      canvas.drawCircle(szCX, szCY, szR * 1.12f, anomalyRingPaint)
      // Calm clear #030A16 sanctuary interior
      anomalyFillPaint.color =
        if (isLockedOut) Color.argb(72, 69, 10, 10) else Color.argb((128 * opScale).toInt().coerceIn(55, 185), 3, 10, 22)
      canvas.drawCircle(szCX, szCY, szR, anomalyFillPaint)
    }

    val rimPulse = 0.5f + 0.5f * sin(tSec * (if (isLockedOut) 9.5f else 2.6f))
    val hubAccent =
      if (isLockedOut) {
        if (rimPulse > 0.5f) Color.parseColor("#EF4444") else Color.parseColor("#F59E0B")
      } else {
        padGlow
      }
    val hubSecondary = if (isLockedOut) Color.parseColor("#F59E0B") else padTrim
    val goldCol = if (isLockedOut) Color.parseColor("#EF4444") else Color.parseColor("#FACC15")

    // Subtle serene concentric harbor guide rings inside the calm sanctuary
    anomalyRingPaint.strokeWidth = 1.4f
    anomalyRingPaint.color = Color.argb((42 * opScale).toInt().coerceIn(18, 72), Color.red(hubSecondary), Color.green(hubSecondary), Color.blue(hubSecondary))
    canvas.drawCircle(szCX, szCY, szR * 0.72f, anomalyRingPaint)

    // Layer 2: 5x Ship-Size (180px base world radius) Nexus Station Metallic Safe-Zone Station Sprite
    // + Revolving Ops Holographic Ring + 8 Golden-Cyan Stabilizer Beams Bracing szR
    val stationR =
      (180f * RiftCalibrationState.safeZoneScale * szPadRec.pixelScale.coerceIn(0.65f, 1.45f))
        .coerceAtMost(szR * 0.86f)
        .coerceAtLeast(48f)

    // 8 Golden-Cyan Stabilizer Beams projecting from the Nexus Station Outer Docking Ring outward to brace the szR Containment Ring + 8 Outward Emitter Chevrons
    for (i in 0 until 8) {
      val ang = i * (PI.toFloat() * 0.25f) + sin(tSec * 0.4f) * 0.02f
      val ca = cos(ang)
      val sa = sin(ang)
      val bStartR = stationR * 0.86f
      val bEndR = szR - 3f
      val x0 = szCX + ca * bStartR
      val y0 = szCY + sa * bStartR
      val x1 = szCX + ca * bEndR
      val y1 = szCY + sa * bEndR

      // Outer beam halo
      anomalyRingPaint.strokeWidth = 5.4f
      anomalyRingPaint.color =
        Color.argb((58 * opScale).toInt().coerceIn(22, 95), Color.red(hubAccent), Color.green(hubAccent), Color.blue(hubAccent))
      canvas.drawLine(x0, y0, x1, y1, anomalyRingPaint)

      // Crisp golden-cyan stabilizer beam core
      anomalyRingPaint.strokeWidth = 2.1f
      val beamTint = if (i % 2 == 0) hubAccent else goldCol
      anomalyRingPaint.color =
        Color.argb((185 * opScale).toInt().coerceIn(70, 235), Color.red(beamTint), Color.green(beamTint), Color.blue(beamTint))
      canvas.drawLine(x0, y0, x1, y1, anomalyRingPaint)

      // Outward-facing bracing emitter chevron on szR physically holding back the collapsing Rift tendrils
      val perpX = -sa
      val perpY = ca
      val chevBaseX = szCX + ca * (szR - 4f)
      val chevBaseY = szCY + sa * (szR - 4f)
      val chevTipX = szCX + ca * (szR + 13f)
      val chevTipY = szCY + sa * (szR + 13f)
      val wingSpan = 10.5f
      anomalyRingPaint.strokeWidth = 2.8f
      anomalyRingPaint.color =
        Color.argb((230 * opScale).toInt().coerceIn(90, 255), Color.red(goldCol), Color.green(goldCol), Color.blue(goldCol))
      canvas.drawLine(chevBaseX + perpX * wingSpan, chevBaseY + perpY * wingSpan, chevTipX, chevTipY, anomalyRingPaint)
      canvas.drawLine(chevBaseX - perpX * wingSpan, chevBaseY - perpY * wingSpan, chevTipX, chevTipY, anomalyRingPaint)
      obstacleBeaconPaint.color = Color.WHITE
      canvas.drawCircle(chevTipX, chevTipY, 2.6f, obstacleBeaconPaint)
    }

    // Smoothly Rotating 5x Metallic Nexus Station Sprite (diameter = stationR * 2f)
    val stationBmp = RiftSpriteBaker.safeZoneStationBitmap
    val halfBmp = stationBmp.width * 0.5f
    val stationScale = (stationR * 2f) / stationBmp.width.toFloat().coerceAtLeast(1f)
    val stationSpinDeg = (tSec * 7.5f) % 360f

    // Subtle deep-space under-shadow beneath the 5x Nexus Station
    shadowMatrix.reset()
    shadowMatrix.postTranslate(-halfBmp, -halfBmp)
    shadowMatrix.postScale(stationScale * 0.98f, stationScale * 0.98f)
    shadowMatrix.postRotate(stationSpinDeg)
    shadowMatrix.postTranslate(szCX + 10f, szCY + 14f)
    canvas.drawBitmap(stationBmp, shadowMatrix, shipShadowPaint)

    shipMatrix.reset()
    shipMatrix.postTranslate(-halfBmp, -halfBmp)
    shipMatrix.postScale(stationScale, stationScale)
    shipMatrix.postRotate(stationSpinDeg)
    shipMatrix.postTranslate(szCX, szCY)
    if (opScale < 0.99f) {
      buffGlowPaint.alpha = (opScale * 255f).toInt().coerceIn(75, 255)
      canvas.drawBitmap(stationBmp, shipMatrix, buffGlowPaint)
      buffGlowPaint.alpha = 255
    } else {
      canvas.drawBitmap(stationBmp, shipMatrix, null)
    }

    // Shrunk Revolving 3D-Tilted Cyan/Gold Holographic Ring Orbiting Central Ops Spire (stationR * 0.34f)
    RiftSpriteBaker.drawSafeZoneStationHoloRing(
      c = canvas,
      cx = szCX,
      cy = szCY,
      stationRadius = stationR,
      timeSec = tSec,
      isLockedOut = isLockedOut,
      alphaScale = opScale,
    )

    // Layer 3: Crisp Braced Stabilizer Containment Shield Ring at exact physics radius szR
    val rimColor = hubAccent
    anomalyRingPaint.strokeWidth = 4.2f
    anomalyRingPaint.color =
      Color.argb((225 * opScale).toInt().coerceIn(90, 255), Color.red(rimColor), Color.green(rimColor), Color.blue(rimColor))
    canvas.drawCircle(szCX, szCY, szR, anomalyRingPaint)

    arcRect.set(szCX - szR * 0.97f, szCY - szR * 0.97f, szCX + szR * 0.97f, szCY + szR * 0.97f)
    anomalyRingPaint.strokeWidth = 2.0f
    anomalyRingPaint.color =
      Color.argb((155 * opScale).toInt().coerceIn(55, 215), Color.red(hubSecondary), Color.green(hubSecondary), Color.blue(hubSecondary))
    val rimSpinDeg = (tSec * 14f) % 360f
    for (seg in 0 until 16) {
      canvas.drawArc(arcRect, rimSpinDeg + seg * 22.5f, 12f, false, anomalyRingPaint)
    }

    // Layer 4: Readable Zoom-Compensated Safe-Harbor Banner
    val safeScale = worldScale.coerceAtLeast(0.15f)
    val safeBannerText =
      when {
        isLockedOut ->
          String.format(
            java.util.Locale.US,
            "SAFE HARBOR LOCKED • %.1fs // REPULSION SHIELD ACTIVE",
            p.safeZoneLockoutTimer,
          )
        p.inSafeZone && p.respawnTimer <= 0f -> {
          val rem = max(0f, RiftCalibrationState.safeZoneIdleLimitSec - p.safeZoneStayTimer)
          String.format(java.util.Locale.US, "NEXUS SAFE HARBOR // WEAPONS DISARMED • EVICTION WARP IN %.1fs", rem)
        }
        else ->
          String.format(java.util.Locale.US, "NEXUS SAFE HARBOR // WEAPONS DISARMED • %.0fs MAX STAY", RiftCalibrationState.safeZoneIdleLimitSec)
      }
    drawWorldOverheadBanner(
      canvas = canvas,
      text = safeBannerText,
      worldX = szCX,
      worldY = szCY - szR - (24f / safeScale),
      fillColor = if (isLockedOut) rimColor else Color.parseColor("#67E8F9"),
      worldScale = worldScale,
      sizeMultiplier = 1.22f,
      alpha = 245,
    )
  }

  private fun drawLightningBoltBranch(
    canvas: Canvas,
    x0: Float,
    y0: Float,
    x1: Float,
    y1: Float,
    jitterAmp: Float,
    seedPhase: Float,
    outerCol: Int,
    midCol: Int,
  ) {
    val dx = x1 - x0
    val dy = y1 - y0
    val len = hypot(dx, dy).coerceAtLeast(1f)
    val px = -dy / len
    val py = dx / len
    val steps = 6
    lightningPath.reset()
    lightningPath.moveTo(x0, y0)
    var midBranchX = x0 + dx * 0.5f
    var midBranchY = y0 + dy * 0.5f
    for (k in 1 until steps) {
      val frac = k.toFloat() / steps.toFloat()
      val env = sin(frac * PI.toFloat())
      val n = sin(seedPhase * 17.3f + k * 4.7f) * cos(seedPhase * 9.1f - k * 2.9f)
      val jx = x0 + dx * frac + px * jitterAmp * env * n
      val jy = y0 + dy * frac + py * jitterAmp * env * n
      if (k == 3) {
        midBranchX = jx
        midBranchY = jy
      }
      lightningPath.lineTo(jx, jy)
    }
    lightningPath.lineTo(x1, y1)

    lightningOuterPaint.color = outerCol
    canvas.drawPath(lightningPath, lightningOuterPaint)
    lightningMidPaint.color = midCol
    canvas.drawPath(lightningPath, lightningMidPaint)
    canvas.drawPath(lightningPath, lightningCorePaint)

    // Secondary forked plasma tendril from midpoint
    val forkAng = atan2(dy, dx) + (if (sin(seedPhase * 5.3f) >= 0f) 0.58f else -0.58f)
    val forkLen = len * 0.36f
    val fx = midBranchX + cos(forkAng) * forkLen
    val fy = midBranchY + sin(forkAng) * forkLen
    lightningMidPaint.strokeWidth = 2.6f
    canvas.drawLine(midBranchX, midBranchY, fx, fy, lightningMidPaint)
    lightningMidPaint.strokeWidth = 4.4f
  }

  private fun drawCircularContainmentPerimeter(
    canvas: Canvas,
    camX: Float,
    camY: Float,
    viewLeft: Float,
    viewRight: Float,
    viewTop: Float,
    viewBottom: Float,
  ) {
    val cx = engine.arenaCenterX
    val cy = engine.arenaCenterY
    val r = engine.arenaRadius
    val tSec = engine.traumaTimeSec
    val perimRec = RiftVisualForgeState.getRecipe(ForgeElementTarget.ARENA_PERIMETER_RING)
    val rawPerimTrim = VisualElementRecipe.parseHexColorSafe(perimRec.trimHex, Color.parseColor("#38BDF8"))
    val rawPerimGlow = VisualElementRecipe.parseHexColorSafe(perimRec.glowHex, Color.parseColor("#00F5D4"))
    val perimAbyssal = if (isHubOverwatchMode) 0f else engine.skirmishAbyssalBlend.coerceIn(0f, 1f)
    val perimTrim =
      if (perimAbyssal > 0.01f) {
        Color.rgb(
          (Color.red(rawPerimTrim) + (16 - Color.red(rawPerimTrim)) * perimAbyssal).roundToInt().coerceIn(0, 255),
          (Color.green(rawPerimTrim) + (185 - Color.green(rawPerimTrim)) * perimAbyssal).roundToInt().coerceIn(0, 255),
          (Color.blue(rawPerimTrim) + (129 - Color.blue(rawPerimTrim)) * perimAbyssal).roundToInt().coerceIn(0, 255),
        )
      } else {
        rawPerimTrim
      }
    val perimGlow =
      if (perimAbyssal > 0.01f) {
        Color.rgb(
          (Color.red(rawPerimGlow) + (52 - Color.red(rawPerimGlow)) * perimAbyssal).roundToInt().coerceIn(0, 255),
          (Color.green(rawPerimGlow) + (211 - Color.green(rawPerimGlow)) * perimAbyssal).roundToInt().coerceIn(0, 255),
          (Color.blue(rawPerimGlow) + (153 - Color.blue(rawPerimGlow)) * perimAbyssal).roundToInt().coerceIn(0, 255),
        )
      } else {
        rawPerimGlow
      }

    // Outer deep-void collar beyond the storm wall
    canvas.drawCircle(cx, cy, r + 245f, perimeterVoidPaint)

    // Layer 1: GPU AGSL Billowing 3D Rift-Cloud Storm Wall bleeding 260px inward (API 33+) + Procedural Cloud Fallback
    var drewStormShader = false
    if (Build.VERSION.SDK_INT >= 33) {
      val rt = stormBoundaryRuntimeShader as? RuntimeShader
      if (rt != null) {
        rt.setFloatUniform("uCenter", cx, cy)
        rt.setFloatUniform("uRadius", r)
        rt.setFloatUniform("uTime", tSec)
        rt.setFloatUniform("uGlowRGB", Color.red(perimGlow) / 255f, Color.green(perimGlow) / 255f, Color.blue(perimGlow) / 255f)
        rt.setFloatUniform("uTrimRGB", Color.red(perimTrim) / 255f, Color.green(perimTrim) / 255f, Color.blue(perimTrim) / 255f)
        perimeterStormShaderPaint.shader = rt
        perimeterStormShaderPaint.strokeWidth = 460f
        canvas.drawCircle(cx, cy, r - 35f, perimeterStormShaderPaint)
        drewStormShader = true
      }
    }

    if (!drewStormShader) {
      // Multi-layered inward plasma mist & billowing storm cloud bank fallback (API 24-32 / JVM tests)
      perimeterGlowPaint.strokeWidth = 220f
      perimeterGlowPaint.color = Color.argb(34, Color.red(perimGlow), Color.green(perimGlow), Color.blue(perimGlow))
      canvas.drawCircle(cx, cy, r - 110f, perimeterGlowPaint)
      perimeterGlowPaint.strokeWidth = 135f
      perimeterGlowPaint.color = Color.argb(72, 76, 29, 149)
      canvas.drawCircle(cx, cy, r - 20f, perimeterGlowPaint)
      perimeterGlowPaint.strokeWidth = 84f
      perimeterGlowPaint.color = Color.argb(115, Color.red(perimTrim), Color.green(perimTrim), Color.blue(perimTrim))
      canvas.drawCircle(cx, cy, r + 24f, perimeterGlowPaint)
      perimeterGlowPaint.strokeWidth = 22f
    }

    // Layer 2: Billowing Volumetric Storm Cloud Lobes & Internal Cloud-to-Cloud Sheet Lightning along visible arc
    val lobeCount = 72
    for (i in 0 until lobeCount) {
      val baseAng = (i.toFloat() / lobeCount.toFloat()) * (PI.toFloat() * 2f)
      val driftAng = baseAng + sin(tSec * 0.45f + i * 0.7f) * 0.018f
      val radialBillow = sin(tSec * 1.35f + i * 1.9f) * 28f
      val lx = cx + cos(driftAng) * (r + radialBillow)
      val ly = cy + sin(driftAng) * (r + radialBillow)
      if (lx < viewLeft - 240f || lx > viewRight + 240f || ly < viewTop - 240f || ly > viewBottom + 240f) {
        continue
      }
      val cloudR = (92f + 26f * cos(tSec * 0.9f + i * 2.3f)) * perimRec.pixelScale.coerceIn(0.7f, 1.4f)
      anomalyFillPaint.color =
        if (i % 2 == 0) Color.argb(36, 56, 189, 248) else Color.argb(42, 124, 58, 237)
      canvas.drawCircle(lx, ly, cloudR, anomalyFillPaint)

      // Periodic internal storm lightning veins jumping between adjacent cloud crests
      val flashWave = sin(tSec * 7.5f + i * 3.17f)
      if (flashWave > 0.92f) {
        val nextAng = driftAng + (PI.toFloat() * 2f / lobeCount.toFloat()) * 1.4f
        val nx = cx + cos(nextAng) * (r + radialBillow * 0.5f)
        val ny = cy + sin(nextAng) * (r + radialBillow * 0.5f)
        drawLightningBoltBranch(
          canvas = canvas,
          x0 = lx,
          y0 = ly,
          x1 = nx,
          y1 = ny,
          jitterAmp = 18f,
          seedPhase = tSec * 3.2f + i,
          outerCol = Color.argb(115, 168, 85, 247),
          midCol = Color.argb(210, 0, 245, 212),
        )
      }
    }

    // Subtle glowing plasma horizon seam along exact physics collision radius r
    perimeterRingPaint.color = Color.argb(155, Color.red(perimGlow), Color.green(perimGlow), Color.blue(perimGlow))
    perimeterRingPaint.strokeWidth = 3.2f
    canvas.drawCircle(cx, cy, r, perimeterRingPaint)

    // Layer 3: Live Proximity Arc Lightning striking outward toward ANY living ship within 220px of arenaRadius!
    val candidateShips = if (engine.hasJoinedMatch) engine.ships else listOf(engine.localPlayer)
    for (s in candidateShips) {
      if (s.respawnTimer > 0f) continue
      if (s.x < viewLeft - 260f || s.x > viewRight + 260f || s.y < viewTop - 260f || s.y > viewBottom + 260f) {
        continue
      }
      val dx = s.x - cx
      val dy = s.y - cy
      val distCenter = hypot(dx, dy)
      val distFromWall = r - distCenter
      if (distFromWall in -30f..220f && distCenter > 1f) {
        val proximityFrac = (1f - (distFromWall.coerceAtLeast(0f) / 220f)).coerceIn(0.18f, 1f)
        val rootAng = atan2(dy, dx)
        val rootX = cx + cos(rootAng) * (r + 12f)
        val rootY = cy + sin(rootAng) * (r + 12f)
        val outerAlpha = (165f * proximityFrac).toInt().coerceIn(45, 220)
        val midAlpha = (245f * proximityFrac).toInt().coerceIn(85, 255)
        drawLightningBoltBranch(
          canvas = canvas,
          x0 = rootX,
          y0 = rootY,
          x1 = s.x,
          y1 = s.y,
          jitterAmp = 24f * proximityFrac + 8f,
          seedPhase = tSec * 6.5f + s.shipId * 1.7f,
          outerCol = Color.argb(outerAlpha, 168, 85, 247),
          midCol = Color.argb(midAlpha, 0, 245, 212),
        )
        if (proximityFrac > 0.55f) {
          val sideAng = rootAng + 0.045f * sin(tSec * 9f + s.shipId)
          drawLightningBoltBranch(
            canvas = canvas,
            x0 = cx + cos(sideAng) * (r + 18f),
            y0 = cy + sin(sideAng) * (r + 18f),
            x1 = s.x,
            y1 = s.y,
            jitterAmp = 18f,
            seedPhase = tSec * 8.1f + s.shipId * 3.1f,
            outerCol = Color.argb((outerAlpha * 0.75f).toInt(), 56, 189, 248),
            midCol = Color.argb((midAlpha * 0.85f).toInt(), 250, 204, 21),
          )
        }
        anomalyCorePaint.color = Color.argb(midAlpha, 224, 242, 254)
        canvas.drawCircle(rootX, rootY, 7.5f * proximityFrac + 3f, anomalyCorePaint)
      }
    }
  }

  private fun drawTetheredHardLightForcefield(
    canvas: Canvas,
    left: Float,
    top: Float,
    right: Float,
    bottom: Float,
    camX: Float,
    camY: Float,
    railColor: Int,
    coreColor: Int,
    pulseAlphaFrac: Float = 1.0f,
  ) {
    val w = right - left
    val h = bottom - top
    val cx = left + w * 0.5f
    val cy = top + h * 0.5f
    val ex = ((cx - camX) * 0.036f).coerceIn(-18f, 18f)
    val ey = ((cy - camY) * 0.036f).coerceIn(-18f, 18f)
    val tLeft = left + ex
    val tTop = top + ey
    val tRight = right + ex
    val tBottom = bottom + ey

    // Translucent obsidian containment channel (#AA030814)
    obstacleCapPaint.color = Color.argb((170 * pulseAlphaFrac).toInt().coerceIn(45, 210), 3, 8, 20)
    arcRect.set(tLeft, tTop, tRight, tBottom)
    canvas.drawRoundRect(arcRect, 7f, 7f, obstacleCapPaint)

    val rRed = Color.red(railColor)
    val rGrn = Color.green(railColor)
    val rBlu = Color.blue(railColor)

    val isHorizontal = w >= h
    val ax = if (isHorizontal) tLeft + 10f else (tLeft + tRight) * 0.5f
    val ay = if (isHorizontal) (tTop + tBottom) * 0.5f else tTop + 10f
    val bx = if (isHorizontal) tRight - 10f else (tLeft + tRight) * 0.5f
    val by = if (isHorizontal) (tTop + tBottom) * 0.5f else tBottom - 10f

    // Outer plasma glow sheath
    bulletPaint.color = Color.argb((115 * pulseAlphaFrac).toInt().coerceIn(30, 180), rRed, rGrn, rBlu)
    bulletPaint.strokeWidth = min(w, h).coerceIn(10f, 22f)
    canvas.drawLine(ax, ay, bx, by, bulletPaint)

    // Dual parallel hard-light forcefield rails
    val railOffset = (min(w, h) * 0.24f).coerceIn(2.6f, 6.5f)
    anomalyRingPaint.strokeWidth = 2.4f
    anomalyRingPaint.color = Color.argb((235 * pulseAlphaFrac).toInt().coerceIn(70, 255), rRed, rGrn, rBlu)
    if (isHorizontal) {
      canvas.drawLine(ax, ay - railOffset, bx, by - railOffset, anomalyRingPaint)
      canvas.drawLine(ax, ay + railOffset, bx, by + railOffset, anomalyRingPaint)
    } else {
      canvas.drawLine(ax - railOffset, ay, bx - railOffset, by, anomalyRingPaint)
      canvas.drawLine(ax + railOffset, ay, bx + railOffset, by, anomalyRingPaint)
    }

    // White-cyan center plasma filament
    bulletCorePaint.color =
      Color.argb(
        (240 * pulseAlphaFrac).toInt().coerceIn(80, 255),
        Color.red(coreColor),
        Color.green(coreColor),
        Color.blue(coreColor),
      )
    bulletCorePaint.strokeWidth = 2.6f
    canvas.drawLine(ax, ay, bx, by, bulletCorePaint)
    bulletCorePaint.color = Color.WHITE

    // Twin 5-Stop Beveled Hexagonal Gunmetal Emitter Pylons anchored at both ends
    val pylonR = (min(w, h) * 0.58f).coerceIn(9.5f, 16.5f)
    for ((px, py) in arrayOf(ax to ay, bx to by)) {
      obstaclePath.reset()
      for (hIdx in 0 until 6) {
        val ha = (hIdx * PI / 3.0 - PI / 6.0).toFloat()
        val hx = px + cos(ha) * pylonR
        val hy = py + sin(ha) * pylonR
        if (hIdx == 0) obstaclePath.moveTo(hx, hy) else obstaclePath.lineTo(hx, hy)
      }
      obstaclePath.close()
      obstacleCapPaint.color = Color.parseColor("#1E293B")
      canvas.drawPath(obstaclePath, obstacleCapPaint)

      // Upper-left specular titanium bevel + dark armor outline
      anomalyRingPaint.strokeWidth = 2.0f
      anomalyRingPaint.color = Color.parseColor("#040914")
      canvas.drawPath(obstaclePath, anomalyRingPaint)
      anomalyRingPaint.strokeWidth = 1.5f
      anomalyRingPaint.color = Color.parseColor("#94A3B8")
      canvas.drawLine(
        px + cos((-5.0 * PI / 6.0).toFloat()) * (pylonR * 0.88f),
        py + sin((-5.0 * PI / 6.0).toFloat()) * (pylonR * 0.88f),
        px + cos((-PI / 6.0).toFloat()) * (pylonR * 0.88f),
        py + sin((-PI / 6.0).toFloat()) * (pylonR * 0.88f),
        anomalyRingPaint,
      )

      // Inner sovereign-gold collar + glowing cyan/core emitter lens
      anomalyRingPaint.strokeWidth = 1.4f
      anomalyRingPaint.color = Color.parseColor("#F59E0B")
      canvas.drawCircle(px, py, pylonR * 0.54f, anomalyRingPaint)
      obstacleBeaconPaint.color = railColor
      canvas.drawCircle(px, py, pylonR * 0.42f, obstacleBeaconPaint)
      obstacleBeaconPaint.color = Color.parseColor("#E0F2FE")
      canvas.drawCircle(px - pylonR * 0.08f, py - pylonR * 0.08f, pylonR * 0.19f, obstacleBeaconPaint)
    }
  }

  private fun findNearestPointLightEmitter(cx: Float, cy: Float): Triple<Float, Float, Int>? {
    val maxRange = RiftVisualForgeState.projectileLightRadius.coerceIn(120f, 950f)
    var bestDistSq = maxRange * maxRange
    var bestX = 0f
    var bestY = 0f
    var bestColor = 0
    var found = false

    val testOrb = RiftVisualForgeState.pointLightTestOrbWorldPos
    if (testOrb != null) {
      val dx = testOrb.x - cx
      val dy = testOrb.y - cy
      val dSq = dx * dx + dy * dy
      if (dSq < bestDistSq) {
        bestDistSq = dSq
        bestX = testOrb.x
        bestY = testOrb.y
        bestColor = Color.parseColor("#FACC15")
        found = true
      }
    }

    for (b in engine.bullets) {
      val dx = b.x - cx
      val dy = b.y - cy
      val dSq = dx * dx + dy * dy
      if (dSq < bestDistSq) {
        bestDistSq = dSq
        bestX = b.x
        bestY = b.y
        bestColor = b.color
        found = true
      }
    }

    for (anom in engine.anomalies) {
      val dx = anom.x - cx
      val dy = anom.y - cy
      val dSq = dx * dx + dy * dy
      if (dSq < bestDistSq) {
        bestDistSq = dSq
        bestX = anom.x
        bestY = anom.y
        bestColor =
          when (anom.type) {
            AnomalyType.PLASMA_RIFT -> Color.parseColor("#FB7185")
            AnomalyType.REPULSOR_RIFT -> Color.parseColor("#38BDF8")
            AnomalyType.GRAVITY_RIFT -> Color.parseColor("#C084FC")
            AnomalyType.PHASE_RIFT_A, AnomalyType.PHASE_RIFT_B -> Color.parseColor("#2DD4BF")
          }
        found = true
      }
    }

    return if (found) Triple(bestX, bestY, bestColor) else null
  }

  private fun drawSpaceObstacle3D(
    canvas: Canvas,
    w: WallRect,
    camX: Float,
    camY: Float,
  ) {
    val left = w.x
    val top = w.y
    val right = w.x + w.w
    val bottom = w.y + w.h

    if (w.style == SpaceObstacleStyle.PERMANENT_BARRIER) {
      val szWallRec = RiftVisualForgeState.getRecipe(ForgeElementTarget.SAFE_ZONE_WALL)
      drawTetheredHardLightForcefield(
        canvas = canvas,
        left = left,
        top = top,
        right = right,
        bottom = bottom,
        camX = camX,
        camY = camY,
        railColor = VisualElementRecipe.parseHexColorSafe(szWallRec.glowHex, Color.parseColor("#00F5D4")),
        coreColor = VisualElementRecipe.parseHexColorSafe(szWallRec.trimHex, Color.parseColor("#E0F2FE")),
        pulseAlphaFrac = szWallRec.surfaceOpacity.coerceIn(0.3f, 1.0f),
      )
      return
    }

    val forgeTarget =
      when (w.style) {
        SpaceObstacleStyle.ORBITAL_STATION -> ForgeElementTarget.OBSTACLE_ORBITAL_STATION
        SpaceObstacleStyle.ASTEROID_STATION -> ForgeElementTarget.OBSTACLE_ASTEROID_STATION
        SpaceObstacleStyle.CRYSTAL_ASTEROID -> ForgeElementTarget.OBSTACLE_SPACE_ASTEROID
        SpaceObstacleStyle.FLOATING_DEBRIS,
        SpaceObstacleStyle.PERMANENT_BARRIER -> ForgeElementTarget.OBSTACLE_FLOATING_DEBRIS
      }
    val obsRec = RiftVisualForgeState.getRecipe(forgeTarget)

    val wCx = left + w.w * 0.5f
    val wCy = top + w.h * 0.5f
    val visR = (max(w.w, w.h) * 0.56f * obsRec.pixelScale.coerceIn(0.70f, 1.35f)).coerceAtLeast(18f)
    val rotDeg =
      ((w.variantSeed * 47) and 359).toFloat() +
        if (w.style == SpaceObstacleStyle.ORBITAL_STATION) {
          (engine.traumaTimeSec * 4.5f) % 360f
        } else {
          0f
        }

    // 1. Soft 2.5D Camera-Offset Drop Shadow on the Sector Plane (away from camera)
    val ex = ((wCx - camX) * 0.045f).coerceIn(-22f, 22f)
    val ey = ((wCy - camY) * 0.045f).coerceIn(-22f, 22f)
    val bmp = RiftSpriteBaker.getSpaceObstacleBitmap(w.style, w.variantSeed)
    val bmpHalf = (bmp.width * 0.5f).coerceAtLeast(1f)
    val spriteScale = visR / bmpHalf

    shadowMatrix.reset()
    shadowMatrix.postTranslate(-bmpHalf, -bmpHalf)
    shadowMatrix.postRotate(rotDeg)
    shadowMatrix.postScale(spriteScale * 1.03f, spriteScale * 1.03f)
    shadowMatrix.postTranslate(wCx + ex, wCy + ey)
    canvas.drawBitmap(bmp, shadowMatrix, shipShadowPaint)

    // 2. Crisp 224x224 5-Stop Metallic / Sunlit-Basalt Obstacle Sprite centered at (wCx, wCy)
    shipMatrix.reset()
    shipMatrix.postTranslate(-bmpHalf, -bmpHalf)
    shipMatrix.postRotate(rotDeg)
    shipMatrix.postScale(spriteScale, spriteScale)
    shipMatrix.postTranslate(wCx, wCy)
    obstacleSpritePaint.alpha = (255f * obsRec.surfaceOpacity.coerceIn(0.35f, 1.0f)).roundToInt().coerceIn(90, 255)
    canvas.drawBitmap(bmp, shipMatrix, obstacleSpritePaint)
    obstacleSpritePaint.alpha = 255

    // 3. Ship-Style Expanding Directional Impact Ripple Arcs + Radial Armor Sparks at w.lastHitAngle (Zero White-Washing)
    if (w.hitFlashTimer > 0f) {
      val t = (w.hitFlashTimer / 0.22f).coerceIn(0f, 1f)
      val progress = 1f - t
      val degCenter = w.lastHitAngle * (180f / PI.toFloat())
      val isStationObs =
        w.style == SpaceObstacleStyle.ORBITAL_STATION || w.style == SpaceObstacleStyle.ASTEROID_STATION
      val outerRippleRgb =
        when {
          isStationObs && w.stationHitCounter >= 8 -> Color.rgb(239, 68, 68)
          isStationObs -> Color.rgb(56, 189, 248)
          w.style == SpaceObstacleStyle.CRYSTAL_ASTEROID -> Color.rgb(192, 132, 252)
          else -> Color.rgb(56, 189, 248)
        }
      val innerRippleRgb =
        when {
          isStationObs -> Color.rgb(250, 204, 21)
          w.style == SpaceObstacleStyle.CRYSTAL_ASTEROID -> Color.rgb(0, 245, 212)
          else -> Color.rgb(224, 242, 254)
        }

      // Outer expanding shield/hull deflection arc
      val outerR = visR * (0.94f + 0.18f * progress)
      val outerSweep = 56f + 28f * progress
      val outerAlpha = (t * 245f).roundToInt().coerceIn(0, 255)
      arcRect.set(wCx - outerR, wCy - outerR, wCx + outerR, wCy + outerR)
      shieldRipplePaint.strokeWidth = 3.6f * (0.55f + 0.45f * t)
      shieldRipplePaint.color =
        Color.argb(
          outerAlpha,
          Color.red(outerRippleRgb),
          Color.green(outerRippleRgb),
          Color.blue(outerRippleRgb),
        )
      canvas.drawArc(arcRect, degCenter - outerSweep * 0.5f, outerSweep, false, shieldRipplePaint)

      // Inner harmonic reaction arc hugging the impact rim
      val innerR = visR * (0.85f + 0.09f * progress)
      val innerSweep = 38f + 16f * progress
      val innerAlpha = (t * t * 225f).roundToInt().coerceIn(0, 255)
      arcRect.set(wCx - innerR, wCy - innerR, wCx + innerR, wCy + innerR)
      shieldRipplePaint.strokeWidth = 2.2f
      shieldRipplePaint.color =
        Color.argb(
          innerAlpha,
          Color.red(innerRippleRgb),
          Color.green(innerRippleRgb),
          Color.blue(innerRippleRgb),
        )
      canvas.drawArc(arcRect, degCenter - innerSweep * 0.5f, innerSweep, false, shieldRipplePaint)

      // Short radial armor sparks bursting outward along the impact normal
      val sparkAlpha = (t * 235f).roundToInt().coerceIn(0, 255)
      if (sparkAlpha > 16) {
        shieldRipplePaint.strokeWidth = 1.9f
        shieldRipplePaint.color =
          Color.argb(
            sparkAlpha,
            Color.red(innerRippleRgb),
            Color.green(innerRippleRgb),
            Color.blue(innerRippleRgb),
          )
        for (sIdx in -2..2) {
          val sAng = w.lastHitAngle + sIdx * 0.17f
          val startR = visR * (0.88f + 0.14f * progress)
          val endR = startR + visR * (0.14f * t + 0.06f)
          canvas.drawLine(
            wCx + cos(sAng) * startR,
            wCy + sin(sAng) * startR,
            wCx + cos(sAng) * endR,
            wCy + sin(sAng) * endR,
            shieldRipplePaint,
          )
        }
      }
      shieldRipplePaint.strokeWidth = 3.2f
    }

    // 4. 10-Segment Station Defense Nova Charge Ring (ORBITAL_STATION & ASTEROID_STATION)
    if (
      (w.style == SpaceObstacleStyle.ORBITAL_STATION || w.style == SpaceObstacleStyle.ASTEROID_STATION) &&
        w.stationHitCounter > 0
    ) {
      val chargeR = (visR * 0.56f).coerceAtLeast(18f)
      arcRect.set(wCx - chargeR, wCy - chargeR, wCx + chargeR, wCy + chargeR)
      anomalyRingPaint.strokeWidth = 3.4f
      anomalyRingPaint.color =
        if (w.stationHitCounter >= 8) Color.parseColor("#EF4444") else Color.parseColor("#FACC15")
      for (seg in 0 until w.stationHitCounter.coerceIn(0, 10)) {
        canvas.drawArc(arcRect, -90f + seg * 36f, 26f, false, anomalyRingPaint)
      }
    }

    // 5. Circular Flux Integrity Ring whenever Obstacle Takes Damage (w.hp < w.maxHp)
    if (w.hp < w.maxHp) {
      val hpFrac = (w.hp / w.maxHp.coerceAtLeast(1f)).coerceIn(0f, 1f)
      val integR = (visR * 0.84f).coerceAtLeast(20f)
      arcRect.set(wCx - integR, wCy - integR, wCx + integR, wCy + integR)
      anomalyRingPaint.strokeWidth = 3.8f
      anomalyRingPaint.color = Color.argb(145, 15, 23, 42)
      canvas.drawCircle(wCx, wCy, integR, anomalyRingPaint)
      anomalyRingPaint.color =
        when {
          hpFrac > 0.55f -> Color.parseColor("#00F5D4")
          hpFrac > 0.25f -> Color.parseColor("#FACC15")
          else -> Color.parseColor("#EF4444")
        }
      canvas.drawArc(arcRect, -90f, hpFrac * 360f, false, anomalyRingPaint)
    }

    // 6. Dynamic Point-Light Directional Rim-Lighting Arc facing nearby Projectiles, Rifts, or Test Orb
    if (RiftVisualForgeState.pointLightEnabled && RiftVisualForgeState.dynamicLightIntensity > 0.05f) {
      val emitter = findNearestPointLightEmitter(wCx, wCy)
      if (emitter != null) {
        val (lx, ly, lColor) = emitter
        val dx = lx - wCx
        val dy = ly - wCy
        val dist = hypot(dx, dy).coerceAtLeast(1f)
        val maxRange = RiftVisualForgeState.projectileLightRadius.coerceIn(120f, 950f)
        val falloff = (1f - dist / maxRange).coerceIn(0f, 1f)
        val rimAlpha =
          (falloff * falloff * 225f * RiftVisualForgeState.dynamicLightIntensity.coerceIn(0f, 2.5f))
            .toInt()
            .coerceIn(0, 245)
        if (rimAlpha >= 14) {
          val lightDeg = atan2(dy, dx) * (180f / PI.toFloat())
          val rimR = visR * 0.88f
          arcRect.set(wCx - rimR, wCy - rimR, wCx + rimR, wCy + rimR)
          dynamicRimPaint.strokeWidth = 3.2f
          dynamicRimPaint.color = Color.argb(rimAlpha, Color.red(lColor), Color.green(lColor), Color.blue(lColor))
          canvas.drawArc(arcRect, lightDeg - 46f, 92f, false, dynamicRimPaint)
        }
      }
    }
  }

  private fun drawPermanentBarrier3D(
    canvas: Canvas,
    bw: BrickWallEntity,
    isFriendlyWall: Boolean,
    camX: Float,
    camY: Float,
  ) {
    val barrierRec = RiftVisualForgeState.getRecipe(ForgeElementTarget.PLACED_STASIS_BARRIER)
    val pulseAlpha =
      if (bw.life <= 5.0f) {
        val warnWave = 0.55f + 0.45f * sin((System.currentTimeMillis() % 320L) / 320f * (PI.toFloat() * 2f))
        warnWave.coerceIn(0.38f, 1.0f)
      } else {
        barrierRec.surfaceOpacity.coerceIn(0.35f, 1.0f)
      }
    val railColor =
      if (!barrierRec.isFactoryDefault) {
        VisualElementRecipe.parseHexColorSafe(barrierRec.glowHex, Color.parseColor("#00F5D4"))
      } else if (isFriendlyWall) {
        Color.parseColor("#00F5D4")
      } else {
        Color.parseColor("#38BDF8")
      }
    val coreColor =
      if (!barrierRec.isFactoryDefault) {
        VisualElementRecipe.parseHexColorSafe(barrierRec.trimHex, Color.parseColor("#E0F2FE"))
      } else if (isFriendlyWall) {
        Color.parseColor("#E0F2FE")
      } else {
        Color.parseColor("#BAE6FD")
      }
    drawTetheredHardLightForcefield(
      canvas = canvas,
      left = bw.x,
      top = bw.y,
      right = bw.x + bw.w,
      bottom = bw.y + bw.h,
      camX = camX,
      camY = camY,
      railColor = railColor,
      coreColor = coreColor,
      pulseAlphaFrac = pulseAlpha,
    )
    if (bw.stackCount > 1) {
      val curScale = engine.effectiveCameraScale().coerceAtLeast(0.15f)
      drawWorldOverheadBanner(
        canvas = canvas,
        text = "BARRIER x${bw.stackCount}",
        worldX = bw.x + bw.w * 0.5f,
        worldY = bw.y - (12f / curScale),
        fillColor = Color.parseColor("#00F5D4"),
        worldScale = curScale,
        sizeMultiplier = 0.98f,
        alpha = (235 * pulseAlpha).toInt().coerceIn(90, 255),
      )
    }
  }

  private fun drawRaycastTrajectory(canvas: Canvas, pts: List<TrajectoryPoint>, color: Int) {
    if (pts.size < 2) return
    trajectoryLinePaint.color = color
    trajPath.reset()
    trajPath.moveTo(pts[0].x, pts[0].y)
    for (i in 1 until pts.size) {
      val pt = pts[i]
      if (pt.isWarp && pt.isBounce) {
        trajPath.moveTo(pt.x, pt.y)
      } else {
        trajPath.lineTo(pt.x, pt.y)
      }
    }
    canvas.drawPath(trajPath, trajectoryLinePaint)
    for (pt in pts) {
      if (pt.isWarp) {
        bombRingPaint.color = Color.parseColor("#2DD4BF")
        canvas.drawCircle(pt.x, pt.y, 9.0f, bombRingPaint)
      } else if (pt.isBounce) {
        bombRingPaint.color = color
        canvas.drawCircle(pt.x, pt.y, 6.5f, bombRingPaint)
      }
    }
  }

  private fun drawShieldImpactRipple(
    canvas: Canvas,
    ship: ShipEntity,
    impactAngle: Float,
    intensity: Float,
  ) {
    val r = ship.radius + 12f
    arcRect.set(ship.x - r, ship.y - r, ship.x + r, ship.y + r)
    val degCenter = impactAngle * (180f / PI.toFloat())
    val alpha = (intensity.coerceIn(0f, 1f) * 255f).toInt()
    shieldRipplePaint.color = Color.argb(alpha, 56, 189, 248)
    canvas.drawArc(arcRect, degCenter - 30f, 60f, false, shieldRipplePaint)
  }

  private fun drawTouchControlsOverlay(canvas: Canvas, p: ShipEntity) {
    val unlocked = RiftCalibrationState.isUiUnlockedForDragging
    if (!engine.hasJoinedMatch && !unlocked) return
    val gpConnected = RiftCalibrationState.gamepadConnected
    val gpIdleAlpha = (RiftCalibrationState.gamepadIdleTouchOpacity * 255f).toInt().coerceIn(20, 240)

    // 1. Always-Visible Left Dynamic Thumbstick (0.55 opacity when touch idle, gamepadIdleTouchOpacity when gamepad idle, 1.0 when active or unlocked)
    val leftActive = leftPointerId != MotionEvent.INVALID_POINTER_ID || gamepadLeftStickActive
    val stickAlphaScale =
      when {
        leftActive || unlocked -> 1.0f
        gpConnected -> RiftCalibrationState.gamepadIdleTouchOpacity
        else -> 0.55f
      }
    val baseLeftX = effLeftBaseX()
    val baseLeftY = effLeftBaseY()
    val effLeftR = effLeftRadius()
    // Lock all concentric rings and center puck to shared (baseLeftX, baseLeftY) center when UI Unlock is active
    val lx = if (leftPointerId != MotionEvent.INVALID_POINTER_ID && !unlocked) leftOriginX else baseLeftX
    val ly = if (leftPointerId != MotionEvent.INVALID_POINTER_ID && !unlocked) leftOriginY else baseLeftY
    val kx = if (leftActive && !unlocked) leftCurX else baseLeftX
    val ky = if (leftActive && !unlocked) leftCurY else baseLeftY

    hudCircleFillPaint.color = Color.argb((110 * stickAlphaScale).toInt(), 8, 18, 34)
    canvas.drawCircle(lx, ly, effLeftR, hudCircleFillPaint)

    hudCircleStrokePaint.color =
      if (p.afterburning) {
        Color.argb((255 * stickAlphaScale).toInt(), 245, 158, 11)
      } else {
        Color.argb((220 * stickAlphaScale).toInt(), 56, 189, 248)
      }
    canvas.drawCircle(lx, ly, effLeftR, hudCircleStrokePaint)

    // Afterburner 80% threshold ring (concentric with outer ring)
    hudCircleStrokePaint.strokeWidth = 1.5f
    hudCircleStrokePaint.color = Color.argb((110 * stickAlphaScale).toInt(), 245, 158, 11)
    canvas.drawCircle(lx, ly, effLeftR * 0.80f, hudCircleStrokePaint)
    hudCircleStrokePaint.strokeWidth = 2.6f

    // 70-Degree Directional Thrust Arc along the outer rim when Left Stick is deflected (leftStickMag > 0.08f)
    if (engine.leftStickMag > 0.08f && !unlocked) {
      arcRect.set(lx - effLeftR, ly - effLeftR, lx + effLeftR, ly + effLeftR)
      hudCircleStrokePaint.strokeWidth = 3.5f * dpDensity
      hudCircleStrokePaint.color =
        if (p.afterburning) {
          Color.parseColor("#F59E0B")
        } else {
          Color.parseColor("#00F5D4")
        }
      val centerDeg = (engine.leftStickAngle * 180f / PI.toFloat())
      canvas.drawArc(arcRect, centerDeg - 35f, 70f, false, hudCircleStrokePaint)
      hudCircleStrokePaint.strokeWidth = 2.6f
    }

    val knobR = effLeftR * 0.42f
    hudCircleFillPaint.color =
      if (p.afterburning) {
        Color.argb((240 * stickAlphaScale).toInt(), 245, 158, 11)
      } else {
        Color.argb((230 * stickAlphaScale).toInt(), 56, 189, 248)
      }
    canvas.drawCircle(kx, ky, knobR, hudCircleFillPaint)

    hudLabelPaint.textSize = 9.0f * dpDensity * RiftCalibrationState.leftStickScale
    hudLabelPaint.color = Color.argb((255 * stickAlphaScale).toInt(), 3, 7, 18)
    canvas.drawText(
      if (p.afterburning) "BURN" else "THRUST",
      kx,
      ky + 3.2f * dpDensity,
      hudLabelPaint,
    )

    if (unlocked) {
      canvas.drawCircle(lx, ly, effLeftR, unlockDashPaint)
      drawUnlockCornerBadge(canvas, lx + effLeftR * 0.707f, ly - effLeftR * 0.707f)
    }

    val disarmedInSafeZone = !p.canFire && !unlocked
    val gunCost = p.spec.primaryCost.toInt()
    val arcScale = RiftCalibrationState.combatArcScale

    // 2. Primary Right [FIRE] Stick with exact Per-Ship Primary Weapon Label & Flux Prism Cost + 32dp Cancel Deadzone Ring
    val fX = effFireBtnX()
    val fY = effFireBtnY()
    val fR = effFireRadius()
    val rightStickActive =
      firePointerId != MotionEvent.INVALID_POINTER_ID || gamepadRightStickActive || engine.fireStickActive
    val fireAlpha =
      when {
        disarmedInSafeZone -> 110
        gpConnected && !rightStickActive && !unlocked -> gpIdleAlpha
        else -> 255
      }
    hudCircleFillPaint.color =
      when {
        disarmedInSafeZone -> Color.argb(85, 45, 20, 28)
        engine.isCancelActive -> Color.argb(190, 127, 29, 29)
        engine.fireStickActive -> Color.argb(175, 95, 16, 30)
        gpConnected && !rightStickActive && !unlocked -> Color.argb((gpIdleAlpha * 0.6f).toInt(), 65, 12, 22)
        else -> Color.argb(145, 65, 12, 22)
      }
    canvas.drawCircle(fX, fY, fR, hudCircleFillPaint)
    hudCircleStrokePaint.color =
      when {
        disarmedInSafeZone -> Color.argb(100, 239, 68, 68)
        p.multiShotCharges > 0 -> Color.argb(fireAlpha, 250, 204, 21)
        else -> Color.argb(fireAlpha, 239, 68, 68)
      }
    canvas.drawCircle(fX, fY, fR, hudCircleStrokePaint)

    if (engine.fireStickActive && fireHasExitedDeadzone && !unlocked) {
      hudCircleStrokePaint.strokeWidth = 2.0f
      hudCircleStrokePaint.color =
        if (engine.isCancelActive) Color.parseColor("#EF4444") else Color.argb(110, 239, 68, 68)
      canvas.drawCircle(fX, fY, cancelDeadzonePx, hudCircleStrokePaint)
      hudCircleStrokePaint.strokeWidth = 2.6f
    }

    val fkX =
      if ((firePointerId != MotionEvent.INVALID_POINTER_ID || gamepadRightStickActive) && !unlocked) fireCurX
      else fX
    val fkY =
      if ((firePointerId != MotionEvent.INVALID_POINTER_ID || gamepadRightStickActive) && !unlocked) fireCurY
      else fY
    hudCircleFillPaint.color =
      when {
        disarmedInSafeZone -> Color.argb(95, 140, 40, 52)
        engine.isCancelActive -> Color.parseColor("#EF4444")
        engine.fireStickActive -> Color.argb(235, 239, 68, 68)
        gpConnected && !rightStickActive && !unlocked -> Color.argb((gpIdleAlpha * 0.85f).toInt(), 210, 40, 60)
        else -> Color.argb(185, 210, 40, 60)
      }
    canvas.drawCircle(fkX, fkY, fR * 0.56f, hudCircleFillPaint)

    val primaryShort = p.spec.primaryShortLabel.ifEmpty { "FIRE" }
    hudLabelPaint.textSize = (if (primaryShort.length >= 9) 8.2f else 9.8f) * dpDensity * arcScale
    hudLabelPaint.color = Color.argb(fireAlpha, 255, 255, 255)
    canvas.drawText(
      if (engine.isCancelActive) "X" else primaryShort,
      fkX,
      fkY - 1.5f * dpDensity * arcScale,
      hudLabelPaint,
    )
    hudLabelPaint.textSize = 8.4f * dpDensity * arcScale
    hudLabelPaint.color = Color.argb(fireAlpha, 254, 202, 202)
    if (engine.isCancelActive || !p.canFire) {
      canvas.drawText(
        if (engine.isCancelActive) "CANCEL" else "SAFE",
        fkX,
        fkY + 10.0f * dpDensity * arcScale,
        hudLabelPaint,
      )
    } else {
      val costText = gunCost.toString()
      val costW = hudLabelPaint.measureText(costText)
      val prismR = 4.2f * dpDensity * arcScale
      val gap = 2.5f * dpDensity * arcScale
      val totalW = prismR * 2f + gap + costW
      val startX = fkX - totalW * 0.5f
      val rowY = fkY + 7.2f * dpDensity * arcScale
      RiftSpriteBaker.drawFluxPrismIcon(canvas, startX + prismR, rowY, prismR, alpha = fireAlpha)
      canvas.drawText(
        costText,
        startX + prismR * 2f + gap + costW * 0.5f,
        fkY + 10.0f * dpDensity * arcScale,
        hudLabelPaint,
      )
    }

    if (unlocked) {
      canvas.drawCircle(fX, fY, fR, unlockDashPaint)
      drawUnlockCornerBadge(canvas, fX + fR * 0.707f, fY - fR * 0.707f)
    }

    // 25-Shot Multi-Shot Crate Counter Badge on Primary Fire Stick Ring
    if (p.multiShotCharges > 0) {
      val badgeW = 68f * dpDensity * arcScale
      val badgeH = 16f * dpDensity * arcScale
      val badgeTop = fY - fR - badgeH * 0.65f
      arcRect.set(fX - badgeW * 0.5f, badgeTop, fX + badgeW * 0.5f, badgeTop + badgeH)
      hudCircleFillPaint.color = Color.argb(230, 66, 32, 6)
      canvas.drawRoundRect(arcRect, 8f * dpDensity, 8f * dpDensity, hudCircleFillPaint)
      hudCircleStrokePaint.strokeWidth = 1.6f
      hudCircleStrokePaint.color = Color.parseColor("#FACC15")
      canvas.drawRoundRect(arcRect, 8f * dpDensity, 8f * dpDensity, hudCircleStrokePaint)
      hudCircleStrokePaint.strokeWidth = 2.6f
      hudLabelPaint.textSize = 8.2f * dpDensity * arcScale
      hudLabelPaint.color = Color.parseColor("#FEF08A")
      canvas.drawText(
        "MULTI [${p.multiShotCharges}]",
        fX,
        badgeTop + badgeH * 0.72f,
        hudLabelPaint,
      )
    }

    // 3. Contextual Secondary / Class Ability Circular Button — ONLY rendered for ships with a Secondary/Class Ability (HIDDEN on Suppressor & Twinfang)
    if (p.spec.hasBomb || RiftCalibrationState.forceAllButtonsVisible || unlocked) {
      val bX = effBombBtnX()
      val bY = effBombBtnY()
      val bR = effBombRadius()
      val isCloakToggleReady = (p.spec.bombType == "CLOAK_EMP" && !p.isCloaked)
      val isCloakEmpPulseReady = (p.spec.bombType == "CLOAK_EMP" && p.isCloaked)
      val bombCost =
        when {
          !p.spec.hasBomb -> 300f
          isCloakToggleReady -> RiftCalibrationState.cloakDrainPerSec
          isCloakEmpPulseReady -> 0f
          else -> p.spec.bombCost
        }
      val hasEnoughFluxForSecondary =
        when {
          isCloakEmpPulseReady -> true
          isCloakToggleReady -> p.flux > 180f
          else -> (p.flux - bombCost) >= 1f
        }
      val bombAvailable =
        (p.canFire && p.bombTimer <= 0f && hasEnoughFluxForSecondary) || unlocked
      val bombAlpha = if (bombAvailable) 255 else 115
      val strokeCol =
        when {
          engine.isBombCancelActive -> Color.parseColor("#EF4444")
          p.spec.bombType == "PHASE_DASH" -> Color.parseColor("#38BDF8")
          p.spec.bombType == "CLOAK_EMP" -> Color.parseColor("#B388FF")
          p.spec.bombType == "INTERDICTOR_DOME" -> Color.parseColor("#EC4899")
          else -> Color.parseColor("#F59E0B")
        }
      val fillCol =
        when {
          engine.isBombCancelActive -> Color.rgb(127, 29, 29)
          p.isCloaked && p.spec.bombType == "CLOAK_EMP" -> Color.rgb(109, 40, 217)
          p.spec.bombType == "PHASE_DASH" -> Color.rgb(12, 74, 110)
          p.spec.bombType == "CLOAK_EMP" -> Color.rgb(76, 29, 149)
          p.spec.bombType == "INTERDICTOR_DOME" -> Color.rgb(131, 24, 67)
          else -> Color.rgb(113, 63, 18)
        }
      val sR = Color.red(strokeCol)
      val sG = Color.green(strokeCol)
      val sB = Color.blue(strokeCol)

      // 1. Unified 3-Ring Tactile Cockpit Button Housing
      // Outer Ring 1: Dark-obsidian 3D bezel ring (bR * 1.06f) with 4 cardinal ticks
      hudCircleFillPaint.color = Color.argb(if (bombAvailable) 230 else 150, 7, 15, 30)
      canvas.drawCircle(bX, bY, bR * 1.06f, hudCircleFillPaint)
      hudCircleStrokePaint.strokeWidth = 2.4f * dpDensity * arcScale
      hudCircleStrokePaint.color = Color.argb(bombAlpha, sR, sG, sB)
      canvas.drawCircle(bX, bY, bR, hudCircleStrokePaint)
      hudCircleStrokePaint.strokeWidth = 1.4f * dpDensity * arcScale
      for (tick in 0 until 4) {
        val ta = tick * (PI.toFloat() * 0.5f)
        canvas.drawLine(
          bX + cos(ta) * (bR * 0.90f),
          bY + sin(ta) * (bR * 0.90f),
          bX + cos(ta) * (bR * 1.04f),
          bY + sin(ta) * (bR * 1.04f),
          hudCircleStrokePaint,
        )
      }

      // Inner Ring 2: Recessed tech ring (bR * 0.88f)
      hudCircleFillPaint.color =
        Color.argb(
          if (engine.bombStickActive && bombAvailable) 220 else if (bombAvailable) 175 else 75,
          Color.red(fillCol),
          Color.green(fillCol),
          Color.blue(fillCol),
        )
      canvas.drawCircle(bX, bY, bR * 0.88f, hudCircleFillPaint)
      hudCircleStrokePaint.strokeWidth = 1.1f * dpDensity * arcScale
      hudCircleStrokePaint.color = Color.argb((bombAlpha * 0.65f).toInt(), 255, 255, 255)
      canvas.drawCircle(bX, bY, bR * 0.84f, hudCircleStrokePaint)

      // Inner Ring 3: Top specular highlight arc
      arcRect.set(bX - bR * 0.78f, bY - bR * 0.78f, bX + bR * 0.78f, bY + bR * 0.78f)
      hudCircleStrokePaint.strokeWidth = 1.6f * dpDensity * arcScale
      hudCircleStrokePaint.color = Color.argb((bombAlpha * 0.50f).toInt(), 255, 255, 255)
      canvas.drawArc(arcRect, 205f, 130f, false, hudCircleStrokePaint)
      hudCircleStrokePaint.strokeWidth = 2.6f

      val activeMineCount =
        if (p.spec.bombType == "MINE") {
          engine.bombs.count { it.ownerId == p.id && it.type == "MINE" }
        } else {
          0
        }
      val bLabel =
        when {
          engine.isBombCancelActive -> "X"
          p.spec.bombType == "PHASE_DASH" -> "PHASE"
          p.spec.bombType == "CLOAK_EMP" -> if (p.isCloaked) "EMP PULSE" else "CLOAK"
          p.spec.bombType == "INTERDICTOR_DOME" -> "STASIS"
          p.spec.bombType == "MINE" -> "PROX MINE"
          p.spec.bombType == "EMP" -> "EMP BOMB"
          p.spec.bombType == "SIEGE" -> "SIEGE BOMB"
          p.spec.bombType == "PROX" -> "PROX FLAK"
          p.spec.bombType == "BOUNCE" -> "BOUNCE BOMB"
          p.spec.bombType == "BURST" || p.spec.bombType == "NOVA_RING" -> "NOVA RING"
          p.spec.bombType == "SHOTGUN" || p.spec.bombType == "SHATTER_CONE" -> "SHATTER"
          p.spec.bombType == "MISSILE" || p.spec.bombType == "SEEKER_TORPEDO" -> "SEEKER"
          else -> p.spec.secondaryShortLabel.ifEmpty { "BOMB" }
        }
      val bSub =
        when {
          engine.isBombCancelActive -> "CANCEL"
          p.bombTimer > 0.1f -> String.format(java.util.Locale.US, "%.1fs", p.bombTimer)
          isCloakToggleReady -> "${RiftCalibrationState.cloakDrainPerSec.toInt()}/S"
          isCloakEmpPulseReady -> "0 EMP"
          p.spec.bombType == "INTERDICTOR_DOME" -> "${bombCost.toInt()} • ${RiftCalibrationState.interdictorBubbleDuration.toInt()}s"
          p.spec.bombType == "MINE" -> "${bombCost.toInt()} • $activeMineCount/6"
          else -> "${bombCost.toInt()}"
        }

      // Upper Center Bold Title
      hudLabelPaint.textSize = (if (bLabel.length >= 9) 7.4f else 8.8f) * dpDensity * arcScale
      hudLabelPaint.color = Color.argb(bombAlpha, 255, 255, 255)
      canvas.drawText(bLabel, bX, bY - 1.8f * dpDensity * arcScale, hudLabelPaint)

      // Docked Bottom Pill Badge with Vector Flux Prism Icon inline next to bSub
      val pillW = bR * 1.86f
      val pillH = 14.5f * dpDensity * arcScale
      val pillTop = bY + bR * 0.34f
      arcRect.set(bX - pillW * 0.5f, pillTop, bX + pillW * 0.5f, pillTop + pillH)
      hudCircleFillPaint.color = Color.argb(if (bombAvailable) 235 else 175, 4, 10, 22)
      canvas.drawRoundRect(arcRect, 6f * dpDensity, 6f * dpDensity, hudCircleFillPaint)
      hudCircleStrokePaint.strokeWidth = 1.2f * dpDensity * arcScale
      hudCircleStrokePaint.color = Color.argb(bombAlpha, sR, sG, sB)
      canvas.drawRoundRect(arcRect, 6f * dpDensity, 6f * dpDensity, hudCircleStrokePaint)
      hudCircleStrokePaint.strokeWidth = 2.6f

      hudLabelPaint.textSize = 7.4f * dpDensity * arcScale
      hudLabelPaint.color = Color.argb(bombAlpha, 253, 224, 71)
      val showPrism = !engine.isBombCancelActive && p.bombTimer <= 0.1f
      val textW = hudLabelPaint.measureText(bSub)
      if (showPrism) {
        val prismR = 4.4f * dpDensity * arcScale
        val gap = 2.5f * dpDensity * arcScale
        val totalContentW = prismR * 2f + gap + textW
        val startX = bX - totalContentW * 0.5f
        RiftSpriteBaker.drawFluxPrismIcon(canvas, startX + prismR, pillTop + pillH * 0.5f, prismR, alpha = bombAlpha)
        canvas.drawText(
          bSub,
          startX + prismR * 2f + gap + textW * 0.5f,
          pillTop + pillH * 0.74f,
          hudLabelPaint,
        )
      } else {
        canvas.drawText(bSub, bX, pillTop + pillH * 0.74f, hudLabelPaint)
      }

      if (unlocked) {
        canvas.drawCircle(bX, bY, bR, unlockDashPaint)
        drawUnlockCornerBadge(canvas, bX + bR * 0.707f, bY - bR * 0.707f)
      }
    }

    // 4. Fixed-Position Muscle-Memory Upper-Arc Utility Buttons (227° SPECIAL CRATE & 265° REPEL)
    for (slot in activeUtilitySlots) {
      drawSatelliteButton(
        canvas = canvas,
        cx = slot.cx,
        cy = slot.cy,
        r = slot.r,
        title = slot.title,
        sub = slot.sub,
        strokeColor = slot.strokeColor,
        fillColor = slot.fillColor,
        enabled = slot.enabled || unlocked,
        unlocked = unlocked,
        arcScale = arcScale,
        iconBuffType = slot.iconBuffType,
      )
    }
  }

  private fun drawUnlockCornerBadge(canvas: Canvas, x: Float, y: Float) {
    val badgeR = 6.2f * dpDensity
    hudCircleFillPaint.color = Color.argb(235, 30, 27, 8)
    canvas.drawCircle(x, y, badgeR, hudCircleFillPaint)
    hudCircleStrokePaint.strokeWidth = 1.4f
    hudCircleStrokePaint.color = Color.parseColor("#FACC15")
    canvas.drawCircle(x, y, badgeR, hudCircleStrokePaint)
    hudCircleStrokePaint.strokeWidth = 2.6f
    hudLabelPaint.textSize = 7.2f * dpDensity
    hudLabelPaint.color = Color.parseColor("#FACC15")
    canvas.drawText("+", x, y + 2.5f * dpDensity, hudLabelPaint)
  }

  private fun drawSatelliteButton(
    canvas: Canvas,
    cx: Float,
    cy: Float,
    r: Float,
    title: String,
    sub: String,
    strokeColor: Int,
    fillColor: Int,
    enabled: Boolean,
    unlocked: Boolean = false,
    arcScale: Float = 1.0f,
    iconBuffType: Int = -1,
  ) {
    val alphaVal = if (enabled) 255 else 115
    val sR = Color.red(strokeColor)
    val sG = Color.green(strokeColor)
    val sB = Color.blue(strokeColor)

    // 1. Outer 3D Beveled Dark-Glass Housing + Metallic Rim
    hudCircleFillPaint.color = Color.argb(if (enabled) 225 else 145, 7, 15, 30)
    canvas.drawCircle(cx, cy, r * 1.06f, hudCircleFillPaint)
    hudCircleFillPaint.color =
      Color.argb(
        if (enabled) 165 else 70,
        Color.red(fillColor),
        Color.green(fillColor),
        Color.blue(fillColor),
      )
    canvas.drawCircle(cx, cy, r * 0.94f, hudCircleFillPaint)

    hudCircleStrokePaint.strokeWidth = 2.6f * dpDensity * arcScale
    hudCircleStrokePaint.color = Color.argb(alphaVal, sR, sG, sB)
    canvas.drawCircle(cx, cy, r, hudCircleStrokePaint)

    // Subtle inner beveled highlight ring
    hudCircleStrokePaint.strokeWidth = 1.1f * dpDensity * arcScale
    hudCircleStrokePaint.color = Color.argb((alphaVal * 0.55f).toInt(), 255, 255, 255)
    canvas.drawCircle(cx, cy, r * 0.86f, hudCircleStrokePaint)
    hudCircleStrokePaint.strokeWidth = 2.6f

    // 2. Enlarged Codex / Playfield Crate Icon centered inside the button
    if (iconBuffType >= 0) {
      val bmp = RiftSpriteBaker.getBuffIconBitmap(iconBuffType)
      val iconHalf = r * 0.72f
      nebulaDstRect.set(
        cx - iconHalf,
        cy - iconHalf - 2.5f * dpDensity * arcScale,
        cx + iconHalf,
        cy + iconHalf - 2.5f * dpDensity * arcScale,
      )
      buffGlowPaint.alpha = if (enabled) 250 else 95
      canvas.drawBitmap(bmp, null, nebulaDstRect, buffGlowPaint)
      buffGlowPaint.alpha = 255
    }

    // 3. Compact Beveled Bottom Readout Pill (Charge Count + Flux Cost)
    val pillW = r * 1.88f
    val pillH = 14.5f * dpDensity * arcScale
    val pillTop = cy + r * 0.36f
    arcRect.set(cx - pillW * 0.5f, pillTop, cx + pillW * 0.5f, pillTop + pillH)
    hudCircleFillPaint.color = Color.argb(if (enabled) 235 else 175, 4, 10, 22)
    canvas.drawRoundRect(arcRect, 6f * dpDensity, 6f * dpDensity, hudCircleFillPaint)
    hudCircleStrokePaint.strokeWidth = 1.2f * dpDensity * arcScale
    hudCircleStrokePaint.color = Color.argb(alphaVal, sR, sG, sB)
    canvas.drawRoundRect(arcRect, 6f * dpDensity, 6f * dpDensity, hudCircleStrokePaint)
    hudCircleStrokePaint.strokeWidth = 2.6f

    hudLabelPaint.textSize = 7.4f * dpDensity * arcScale
    hudLabelPaint.color = Color.argb(alphaVal, 255, 255, 255)
    val numericFluxCost = sub.all { it.isDigit() } && sub.isNotEmpty()
    if (numericFluxCost) {
      val leftStr = "$title • "
      val leftW = hudLabelPaint.measureText(leftStr)
      val numW = hudLabelPaint.measureText(sub)
      val prismR = 3.8f * dpDensity * arcScale
      val gap = 2.0f * dpDensity * arcScale
      val totalW = leftW + prismR * 2f + gap + numW
      val startX = cx - totalW * 0.5f
      canvas.drawText(leftStr, startX + leftW * 0.5f, pillTop + pillH * 0.73f, hudLabelPaint)
      RiftSpriteBaker.drawFluxPrismIcon(canvas, startX + leftW + prismR, pillTop + pillH * 0.50f, prismR, alpha = alphaVal)
      canvas.drawText(sub, startX + leftW + prismR * 2f + gap + numW * 0.5f, pillTop + pillH * 0.73f, hudLabelPaint)
    } else {
      canvas.drawText("$title • $sub", cx, pillTop + pillH * 0.73f, hudLabelPaint)
    }

    if (unlocked) {
      canvas.drawCircle(cx, cy, r, unlockDashPaint)
      drawUnlockCornerBadge(canvas, cx + r * 0.707f, cy - r * 0.707f)
    }
  }

  private fun drawStationaryRespawnBanner(canvas: Canvas, vw: Float, vh: Float, timer: Float) {
    // When spectating living teammates, keep the center viewport completely unobstructed for RiftHudOverlay's interactive spectator switcher!
    if (engine.isActivelySpectatingTeammates()) {
      return
    }

    val boxW = min(vw * 0.64f, 360f * dpDensity)
    val boxH = 82f * dpDensity
    val left = (vw - boxW) * 0.5f
    val top = (vh - boxH) * 0.38f
    arcRect.set(left, top, left + boxW, top + boxH)
    canvas.drawRoundRect(arcRect, 14f * dpDensity, 14f * dpDensity, respawnOverlayFill)
    canvas.drawRoundRect(arcRect, 14f * dpDensity, 14f * dpDensity, respawnOverlayStroke)

    hudLabelPaint.textSize = 15f * dpDensity
    hudLabelPaint.color = Color.parseColor("#EF4444")
    canvas.drawText("HULL BREACH // FLUX DEPLETED", vw * 0.5f, top + 30f * dpDensity, hudLabelPaint)

    hudLabelPaint.textSize = 13f * dpDensity
    hudLabelPaint.color = Color.parseColor("#F59E0B")
    val secStr = String.format(java.util.Locale.US, "%.1f", max(0f, timer))
    val reconCopy =
      if (engine.hasActiveSafeZone()) "RECONSTRUCTING AT SAFE ZONE IN ${secStr}s..."
      else "RECONSTRUCTING IN SECTOR IN ${secStr}s..."
    canvas.drawText(
      reconCopy,
      vw * 0.5f,
      top + 58f * dpDensity,
      hudLabelPaint,
    )
  }

  @SuppressLint("ClickableViewAccessibility")
  override fun onTouchEvent(event: MotionEvent): Boolean {
    if (isModalOverlayOpen && !RiftCalibrationState.isUiUnlockedForDragging) return false
    if (height > width) return true
    val action = event.actionMasked
    if (action == MotionEvent.ACTION_DOWN) {
      RiftCalibrationState.gamepadConnected = false
      requestFocus()
    }
    val actionIndex = event.actionIndex
    val pointerId = event.getPointerId(actionIndex)
    val x = event.getX(actionIndex)
    val y = event.getY(actionIndex)
    val p = engine.localPlayer
    rebuildActiveUtilitySlots(p)

    val fX = effFireBtnX()
    val fY = effFireBtnY()
    val fR = effFireRadius()
    val bX = effBombBtnX()
    val bY = effBombBtnY()
    val bR = effBombRadius()
    val lX = effLeftBaseX()
    val lY = effLeftBaseY()
    val lR = effLeftRadius()

    // In UI Unlock Mode: allow independent dragging of Left Stick, Right Fire Stick, Bomb, Repel, and Special Buff Slot
    if (RiftCalibrationState.isUiUnlockedForDragging) {
      when (action) {
        MotionEvent.ACTION_DOWN,
        MotionEvent.ACTION_POINTER_DOWN -> {
          if (uiDragPointerId != MotionEvent.INVALID_POINTER_ID) return true
          for (slot in activeUtilitySlots) {
            if (hypot(x - slot.cx, y - slot.cy) <= slot.r * 1.28f) {
              uiDragPointerId = pointerId
              uiDraggedControl = slot.type
              uiDragLastX = x
              uiDragLastY = y
              return true
            }
          }
          if (hypot(x - bX, y - bY) <= bR * 1.28f) {
            uiDragPointerId = pointerId
            uiDraggedControl = "BOMB"
            uiDragLastX = x
            uiDragLastY = y
            return true
          }
          if (hypot(x - fX, y - fY) <= fR * 1.28f) {
            uiDragPointerId = pointerId
            uiDraggedControl = "FIRE"
            uiDragLastX = x
            uiDragLastY = y
            return true
          }
          if (hypot(x - lX, y - lY) <= lR * 1.28f) {
            uiDragPointerId = pointerId
            uiDraggedControl = "LEFT_STICK"
            uiDragLastX = x
            uiDragLastY = y
            return true
          }
          return false
        }

        MotionEvent.ACTION_MOVE -> {
          if (uiDragPointerId == MotionEvent.INVALID_POINTER_ID) return false
          for (i in 0 until event.pointerCount) {
            if (event.getPointerId(i) == uiDragPointerId) {
              val px = event.getX(i)
              val py = event.getY(i)
              val dx = px - uiDragLastX
              val dy = py - uiDragLastY
              uiDragLastX = px
              uiDragLastY = py
              val delta = androidx.compose.ui.geometry.Offset(dx, dy)
              when (uiDraggedControl) {
                "SPECIAL", "BURST" -> RiftCalibrationState.burstBtnOffset += delta
                "REPEL" -> RiftCalibrationState.repelBtnOffset += delta
                "BOMB" -> RiftCalibrationState.bombBtnOffset += delta
                "FIRE" -> RiftCalibrationState.fireStickOffset += delta
                "LEFT_STICK" -> RiftCalibrationState.leftStickOffset += delta
              }
              return true
            }
          }
          return false
        }

        MotionEvent.ACTION_UP,
        MotionEvent.ACTION_POINTER_UP,
        MotionEvent.ACTION_CANCEL -> {
          if (pointerId == uiDragPointerId || action == MotionEvent.ACTION_CANCEL) {
            uiDragPointerId = MotionEvent.INVALID_POINTER_ID
            uiDraggedControl = null
            RiftCalibrationState.savePersistedLayout(context)
            return true
          }
          return false
        }
      }
      return false
    }

    when (action) {
      MotionEvent.ACTION_DOWN,
      MotionEvent.ACTION_POINTER_DOWN -> {
        // 1. Check Active Upper-Arc Utility Buttons (SPECIAL BUFF SLOT & REPEL — only present when count > 0 or showAll)
        for (slot in activeUtilitySlots) {
          if (hypot(x - slot.cx, y - slot.cy) <= slot.r * 1.16f) {
            engine.activateUtility(slot.type)
            return true
          }
        }

        // 2. Check Secondary / Class Ability Concentric Slot
        if ((p.spec.hasBomb || RiftCalibrationState.forceAllButtonsVisible) &&
          hypot(x - bX, y - bY) <= bR * 1.18f
        ) {
          val isInstantSelfAbility =
            !p.canFire ||
              p.spec.bombType == "PHASE_DASH" ||
              p.spec.bombType == "CLOAK_EMP" ||
              p.spec.bombType == "INTERDICTOR_DOME"
          if (isInstantSelfAbility) {
            engine.fireSecondaryBomb(p, p.angle)
            return true
          }
          bombPointerId = pointerId
          bombCurX = x
          bombCurY = y
          bombHasExitedDeadzone = false
          engine.isBombCancelActive = false
          engine.bombStickActive = true
          engine.bombStickAngle = p.angle
          engine.bombStickMag = 0.2f
          return true
        }

        // 3. Check Primary [FIRE] 360-Degree Aiming Stick
        if (
          hypot(x - fX, y - fY) <= fR * 1.32f ||
            (x > width * 0.72f && y > height * 0.56f && firePointerId == MotionEvent.INVALID_POINTER_ID)
        ) {
          firePointerId = pointerId
          fireHasExitedDeadzone = false
          engine.isCancelActive = false
          val dx = x - fX
          val dy = y - fY
          val dist = hypot(dx, dy)
          val clamped = min(dist, fR * 0.68f)
          if (dist >= cancelDeadzonePx) {
            fireHasExitedDeadzone = true
            val aimA = atan2(dy, dx)
            fireCurX = fX + cos(aimA) * clamped
            fireCurY = fY + sin(aimA) * clamped
            engine.fireStickHasAimVector = true
            engine.fireStickAimAngle = aimA
            p.independentAimActive = true
            p.weaponAngle = aimA
          } else {
            fireCurX = fX
            fireCurY = fY
            engine.fireStickHasAimVector = false
            p.independentAimActive = false
            p.weaponAngle = engine.findQuickTapAutoAimAngle(p)
          }
          engine.fireStickActive = true
          engine.firePrimaryGun(p)
          return true
        }

        // 4. Left-Screen Dynamic Floating Movement Stick (x in 0..width*0.45 or within effLeftRadius * 1.4)
        if ((x < width * 0.45f || hypot(x - lX, y - lY) <= lR * 1.4f) && leftPointerId == MotionEvent.INVALID_POINTER_ID) {
          leftPointerId = pointerId
          leftOriginX = x
          leftOriginY = y
          leftCurX = x
          leftCurY = y
          engine.leftStickActive = true
          engine.leftStickMag = 0f
          engine.autoBraking = false
          return true
        }
      }

      MotionEvent.ACTION_MOVE -> {
        for (i in 0 until event.pointerCount) {
          val pid = event.getPointerId(i)
          val px = event.getX(i)
          val py = event.getY(i)

          if (pid == leftPointerId) {
            val dx = px - leftOriginX
            val dy = py - leftOriginY
            val dist = hypot(dx, dy)
            val clampedDist = min(dist, lR)
            val angle = atan2(dy, dx)
            leftCurX = leftOriginX + cos(angle) * clampedDist
            leftCurY = leftOriginY + sin(angle) * clampedDist
            engine.leftStickActive = true
            engine.leftStickAngle = angle
            engine.leftStickMag = (dist / lR).coerceIn(0f, 1f)
          } else if (pid == firePointerId) {
            val dx = px - fX
            val dy = py - fY
            val dist = hypot(dx, dy)
            val clamped = min(dist, fR * 0.68f)
            if (dist >= cancelDeadzonePx) {
              fireHasExitedDeadzone = true
              engine.isCancelActive = false
              val aimA = atan2(dy, dx)
              fireCurX = fX + cos(aimA) * clamped
              fireCurY = fY + sin(aimA) * clamped
              engine.fireStickHasAimVector = true
              engine.fireStickAimAngle = aimA
              p.independentAimActive = true
              p.weaponAngle = aimA
            } else {
              fireCurX = fX
              fireCurY = fY
              if (fireHasExitedDeadzone) {
                if (!engine.isCancelActive) {
                  engine.isCancelActive = true
                  performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                }
              } else {
                engine.fireStickHasAimVector = false
                p.independentAimActive = false
                p.weaponAngle = engine.findQuickTapAutoAimAngle(p)
              }
            }
          } else if (pid == bombPointerId) {
            bombCurX = px
            bombCurY = py
            val dx = px - bX
            val dy = py - bY
            val dist = hypot(dx, dy)
            if (dist >= cancelDeadzonePx) {
              bombHasExitedDeadzone = true
              engine.isBombCancelActive = false
              engine.bombStickAngle = atan2(dy, dx)
              p.weaponAngle = engine.bombStickAngle
            } else if (bombHasExitedDeadzone) {
              if (!engine.isBombCancelActive) {
                engine.isBombCancelActive = true
                performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
              }
            }
            engine.bombStickMag = (dist / bR).coerceIn(0.15f, 1f)
          }
        }
      }

      MotionEvent.ACTION_UP,
      MotionEvent.ACTION_POINTER_UP,
      MotionEvent.ACTION_CANCEL -> {
        if (pointerId == leftPointerId || action == MotionEvent.ACTION_CANCEL) {
          leftPointerId = MotionEvent.INVALID_POINTER_ID
          leftCurX = lX
          leftCurY = lY
          engine.leftStickActive = false
          engine.leftStickMag = 0f
          // Pure SubSpace zero-friction glide: releasing the stick does NOT slow down the ship!
        }
        if (pointerId == firePointerId || action == MotionEvent.ACTION_CANCEL) {
          firePointerId = MotionEvent.INVALID_POINTER_ID
          fireCurX = fX
          fireCurY = fY
          fireHasExitedDeadzone = false
          engine.isCancelActive = false
          engine.fireStickActive = false
          engine.fireStickHasAimVector = false
          p.independentAimActive = false
        }
        if (pointerId == bombPointerId || action == MotionEvent.ACTION_CANCEL) {
          if (action != MotionEvent.ACTION_CANCEL && !engine.isBombCancelActive) {
            engine.fireSecondaryBomb(p, engine.bombStickAngle)
          }
          bombPointerId = MotionEvent.INVALID_POINTER_ID
          bombHasExitedDeadzone = false
          engine.isBombCancelActive = false
          engine.bombStickActive = false
          engine.bombStickMag = 0f
        }
      }
    }
    return true
  }

  private fun getCenteredAxis(event: MotionEvent, axis: Int): Float {
    val range = event.device?.getMotionRange(axis, event.source)
    val value = event.getAxisValue(axis)
    if (range != null) {
      val flat = range.flat
      if (abs(value) <= flat) return 0f
    }
    return value
  }

  private fun isGamepadSource(source: Int): Boolean {
    return (source and InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD ||
      (source and InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK ||
      (source and InputDevice.SOURCE_DPAD) == InputDevice.SOURCE_DPAD
  }

  private fun handleBoundControllerInput(bindCode: Int, isPressed: Boolean) {
    val p = engine.localPlayer
    val normCode =
      when (bindCode) {
        KeyEvent.KEYCODE_BUTTON_L2 -> RiftCalibrationState.BIND_LT
        KeyEvent.KEYCODE_BUTTON_R2 -> RiftCalibrationState.BIND_RT
        else -> bindCode
      }

    // Keep weaponAngle aligned to Right Stick (if active) or Left Stick heading (p.angle)
    val aimAngle = if (engine.fireStickHasAimVector) engine.fireStickAimAngle else p.angle

    if (normCode == RiftCalibrationState.bindPrimaryFire) {
      if (isPressed) {
        engine.fireStickActive = true
        engine.isCancelActive = false
        p.weaponAngle = aimAngle
        engine.firePrimaryGun(p)
      } else {
        if (firePointerId == MotionEvent.INVALID_POINTER_ID) {
          engine.fireStickActive = false
        }
      }
    }

    if (normCode == RiftCalibrationState.bindSecondaryAbility && isPressed) {
      p.weaponAngle = aimAngle
      engine.fireSecondaryBomb(p, aimAngle)
    }

    if (normCode == RiftCalibrationState.bindSpecialBuff && isPressed) {
      p.weaponAngle = aimAngle
      engine.activateUtility("SPECIAL")
    }

    if (normCode == RiftCalibrationState.bindRetroBrake) {
      if (isPressed) {
        engine.autoBraking = true
        engine.leftStickActive = false
        engine.leftStickMag = 0f
        engine.sfx.playMutedClick()
      } else {
        engine.autoBraking = false
      }
    }

    if (normCode == RiftCalibrationState.bindRepel && isPressed) {
      engine.activateUtility("REPEL")
    }

    if (normCode == RiftCalibrationState.bindMultiToggle && isPressed) {
      if (p.spec.supportsNativeMultifire) {
        p.multifireOn = !p.multifireOn
        engine.sfx.playMutedClick()
        engine.buffVersion++
      }
    }
  }

  override fun onGenericMotionEvent(event: MotionEvent): Boolean {
    if ((event.source and InputDevice.SOURCE_JOYSTICK) != InputDevice.SOURCE_JOYSTICK ||
      event.action != MotionEvent.ACTION_MOVE
    ) {
      return super.onGenericMotionEvent(event)
    }

    RiftCalibrationState.gamepadConnected = true
    val p = engine.localPlayer

    // 1. Left Stick (AXIS_X, AXIS_Y) -> Permanently locked to 360° Flight Thrust, >80% Afterburner & Zero-Friction Drift on Release
    val lx = getCenteredAxis(event, MotionEvent.AXIS_X)
    val ly = getCenteredAxis(event, MotionEvent.AXIS_Y)
    val lMagRaw = hypot(lx, ly)
    val dzL = RiftCalibrationState.gamepadDeadzoneLeft
    if (lMagRaw >= dzL) {
      gamepadLeftStickActive = true
      val normMag = ((lMagRaw - dzL) / (1f - dzL).coerceAtLeast(0.1f)).coerceIn(0f, 1f)
      val ang = atan2(ly, lx)
      engine.leftStickActive = true
      engine.leftStickAngle = ang
      engine.leftStickMag = normMag
      engine.autoBraking = false
      p.angle = ang
      leftOriginX = effLeftBaseX()
      leftOriginY = effLeftBaseY()
      leftCurX = effLeftBaseX() + cos(ang) * normMag * effLeftRadius()
      leftCurY = effLeftBaseY() + sin(ang) * normMag * effLeftRadius()
    } else if (gamepadLeftStickActive && leftPointerId == MotionEvent.INVALID_POINTER_ID) {
      gamepadLeftStickActive = false
      engine.leftStickActive = false
      engine.leftStickMag = 0f
      leftCurX = effLeftBaseX()
      leftCurY = effLeftBaseY()
      // Zero-friction Newtonian drift: NEVER set engine.autoBraking = true on Left Stick release!
    }

    // 2. Right Stick (AXIS_Z, AXIS_RZ) -> Permanently locked to 360° Independent Turret Aiming (or fallback to Left-Stick heading p.angle)
    val rx = getCenteredAxis(event, MotionEvent.AXIS_Z)
    val ry = getCenteredAxis(event, MotionEvent.AXIS_RZ)
    val rMagRaw = hypot(rx, ry)
    val dzR = RiftCalibrationState.gamepadDeadzoneRight
    if (rMagRaw >= dzR) {
      gamepadRightStickActive = true
      val normMag = ((rMagRaw - dzR) / (1f - dzR).coerceAtLeast(0.1f)).coerceIn(0f, 1f)
      val aimA = atan2(ry, rx)
      engine.fireStickHasAimVector = true
      engine.fireStickAimAngle = aimA
      p.independentAimActive = true
      p.weaponAngle = aimA
      fireCurX = effFireBtnX() + cos(aimA) * normMag * effFireRadius() * 0.68f
      fireCurY = effFireBtnY() + sin(aimA) * normMag * effFireRadius() * 0.68f
    } else if (firePointerId == MotionEvent.INVALID_POINTER_ID) {
      if (gamepadRightStickActive) {
        gamepadRightStickActive = false
        fireCurX = effFireBtnX()
        fireCurY = effFireBtnY()
      }
      engine.fireStickHasAimVector = false
      p.independentAimActive = false
      p.weaponAngle = p.angle
    }

    // 3. Right Trigger (AXIS_RTRIGGER / AXIS_GAS / AXIS_THROTTLE) -> BIND_RT
    val rt =
      max(
        event.getAxisValue(MotionEvent.AXIS_RTRIGGER),
        max(
          event.getAxisValue(MotionEvent.AXIS_GAS),
          event.getAxisValue(MotionEvent.AXIS_THROTTLE),
        ),
      )
    if (rt > 0.30f) {
      if (!gamepadRightTriggerHeld) {
        gamepadRightTriggerHeld = true
        handleBoundControllerInput(RiftCalibrationState.BIND_RT, isPressed = true)
      } else if (RiftCalibrationState.bindPrimaryFire == RiftCalibrationState.BIND_RT) {
        p.weaponAngle = if (engine.fireStickHasAimVector) engine.fireStickAimAngle else p.angle
        engine.firePrimaryGun(p)
      }
    } else if (gamepadRightTriggerHeld) {
      gamepadRightTriggerHeld = false
      handleBoundControllerInput(RiftCalibrationState.BIND_RT, isPressed = false)
    }

    // 4. Left Trigger (AXIS_LTRIGGER / AXIS_BRAKE) -> BIND_LT
    val lt = max(event.getAxisValue(MotionEvent.AXIS_LTRIGGER), event.getAxisValue(MotionEvent.AXIS_BRAKE))
    if (lt > 0.35f) {
      if (!gamepadLeftTriggerHeld) {
        gamepadLeftTriggerHeld = true
        handleBoundControllerInput(RiftCalibrationState.BIND_LT, isPressed = true)
      } else if (RiftCalibrationState.bindPrimaryFire == RiftCalibrationState.BIND_LT) {
        p.weaponAngle = if (engine.fireStickHasAimVector) engine.fireStickAimAngle else p.angle
        engine.firePrimaryGun(p)
      }
    } else if (gamepadLeftTriggerHeld) {
      gamepadLeftTriggerHeld = false
      handleBoundControllerInput(RiftCalibrationState.BIND_LT, isPressed = false)
    }

    // 5. Hat Switch D-Pad (AXIS_HAT_X, AXIS_HAT_Y) -> Camera Zoom Step (unless bound to a custom combat action)
    val hatY = event.getAxisValue(MotionEvent.AXIS_HAT_Y)
    if (hatY < -0.5f && !lastDpadUpPressed) {
      lastDpadUpPressed = true
      if (RiftCalibrationState.getActionForBinding(KeyEvent.KEYCODE_DPAD_UP) != null) {
        handleBoundControllerInput(KeyEvent.KEYCODE_DPAD_UP, isPressed = true)
      } else {
        onGamepadCycleCameraZoom(1)
      }
    } else if (hatY > 0.5f && !lastDpadDownPressed) {
      lastDpadDownPressed = true
      if (RiftCalibrationState.getActionForBinding(KeyEvent.KEYCODE_DPAD_DOWN) != null) {
        handleBoundControllerInput(KeyEvent.KEYCODE_DPAD_DOWN, isPressed = true)
      } else {
        onGamepadCycleCameraZoom(-1)
      }
    } else if (abs(hatY) <= 0.3f) {
      if (lastDpadUpPressed) {
        lastDpadUpPressed = false
        handleBoundControllerInput(KeyEvent.KEYCODE_DPAD_UP, isPressed = false)
      }
      if (lastDpadDownPressed) {
        lastDpadDownPressed = false
        handleBoundControllerInput(KeyEvent.KEYCODE_DPAD_DOWN, isPressed = false)
      }
    }

    return true
  }

  override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
    if (isGamepadSource(event.source)) {
      RiftCalibrationState.gamepadConnected = true
    }

    when (keyCode) {
      KeyEvent.KEYCODE_BUTTON_START -> {
        if (event.repeatCount == 0) {
          onGamepadToggleHangar()
        }
        return true
      }

      KeyEvent.KEYCODE_BUTTON_SELECT -> {
        if (event.repeatCount == 0) {
          onGamepadToggleCalibration()
        }
        return true
      }
    }

    // Interactive Teammate Spectator Cycling on Gamepad LB/RB or D-Pad Left/Right while spectating
    if (engine.isActivelySpectatingTeammates() && event.repeatCount == 0) {
      when (keyCode) {
        KeyEvent.KEYCODE_BUTTON_L1, KeyEvent.KEYCODE_DPAD_LEFT -> {
          engine.cycleSpectatorTarget(-1)
          return true
        }
        KeyEvent.KEYCODE_BUTTON_R1, KeyEvent.KEYCODE_DPAD_RIGHT -> {
          engine.cycleSpectatorTarget(1)
          return true
        }
      }
    }

    val normCode =
      when (keyCode) {
        KeyEvent.KEYCODE_BUTTON_L2 -> RiftCalibrationState.BIND_LT
        KeyEvent.KEYCODE_BUTTON_R2 -> RiftCalibrationState.BIND_RT
        else -> keyCode
      }

    // If this button/trigger is mapped to one of the 6 combat actions, execute via handleBoundControllerInput
    if (RiftCalibrationState.getActionForBinding(normCode) != null) {
      if (normCode == RiftCalibrationState.BIND_LT && gamepadLeftTriggerHeld) return true
      if (normCode == RiftCalibrationState.BIND_RT && gamepadRightTriggerHeld) return true
      if (event.repeatCount == 0 || normCode == RiftCalibrationState.bindPrimaryFire) {
        handleBoundControllerInput(normCode, isPressed = true)
      }
      return true
    }

    // Unbound utility / navigation keys
    when (keyCode) {
      KeyEvent.KEYCODE_DPAD_UP -> {
        if (event.repeatCount == 0) {
          onGamepadCycleCameraZoom(1)
        }
        return true
      }

      KeyEvent.KEYCODE_DPAD_DOWN -> {
        if (event.repeatCount == 0) {
          onGamepadCycleCameraZoom(-1)
        }
        return true
      }

      KeyEvent.KEYCODE_DPAD_RIGHT -> {
        if (event.repeatCount == 0) {
          onGamepadOpenNexus()
        }
        return true
      }
    }
    return super.onKeyDown(keyCode, event)
  }

  override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
    if (isGamepadSource(event.source)) {
      RiftCalibrationState.gamepadConnected = true
    }
    val normCode =
      when (keyCode) {
        KeyEvent.KEYCODE_BUTTON_L2 -> RiftCalibrationState.BIND_LT
        KeyEvent.KEYCODE_BUTTON_R2 -> RiftCalibrationState.BIND_RT
        else -> keyCode
      }
    if (RiftCalibrationState.getActionForBinding(normCode) != null) {
      if (normCode == RiftCalibrationState.BIND_LT && gamepadLeftTriggerHeld) return true
      if (normCode == RiftCalibrationState.BIND_RT && gamepadRightTriggerHeld) return true
      handleBoundControllerInput(normCode, isPressed = false)
      return true
    }
    return super.onKeyUp(keyCode, event)
  }

  private fun drawGoldenRiftOutlineCircle(
    canvas: Canvas,
    cx: Float,
    cy: Float,
    baseRadius: Float,
    strokeScale: Float,
    tSec: Float,
    jumpFrac: Float,
  ) {
    val pulse = 0.5f + 0.5f * sin(tSec * 5.2f)
    val r = baseRadius * (1.0f + 0.24f * jumpFrac + 0.035f * pulse)

    // Soft solar-gold outer Rift aura
    anomalyFillPaint.color = Color.argb((32 + (36 * jumpFrac).toInt()).coerceIn(16, 84), 250, 204, 21)
    canvas.drawCircle(cx, cy, r * 1.12f, anomalyFillPaint)

    // Outer rotating 6-segment Golden Rift vortex ribbon (#FFD700)
    val spinDeg = (tSec * 58f) % 360f
    anomalyRingPaint.strokeWidth = 3.4f * strokeScale
    anomalyRingPaint.color = Color.argb(242, 255, 215, 0)
    arcRect.set(cx - r, cy - r, cx + r, cy + r)
    for (seg in 0 until 6) {
      canvas.drawArc(arcRect, spinDeg + seg * 60f, 38f, false, anomalyRingPaint)
    }

    // Counter-rotating inner harmonic gold-cream ring (#FEF08A)
    val innerR = r * 0.90f
    val counterDeg = (-tSec * 42f) % 360f
    anomalyRingPaint.strokeWidth = 2.0f * strokeScale
    anomalyRingPaint.color = Color.argb(215, 254, 240, 138)
    arcRect.set(cx - innerR, cy - innerR, cx + innerR, cy + innerR)
    for (seg in 0 until 4) {
      canvas.drawArc(arcRect, counterDeg + seg * 90f, 52f, false, anomalyRingPaint)
    }

    // 4 Golden Rift harmonic diamond nodes along the outer ring
    val nodeSize = (4.8f * strokeScale).coerceAtLeast(2.4f)
    for (nIdx in 0 until 4) {
      val rad = (spinDeg + nIdx * 90f) * (PI.toFloat() / 180f)
      val nx = cx + cos(rad) * r
      val ny = cy + sin(rad) * r
      anomalyCorePaint.color = Color.parseColor("#FFD700")
      canvas.drawCircle(nx, ny, nodeSize, anomalyCorePaint)
      anomalyCorePaint.color = Color.WHITE
      canvas.drawCircle(nx, ny, nodeSize * 0.46f, anomalyCorePaint)
    }

    // 4 Tactical Golden Rift lock brackets framing the circular outline
    val bracketR = r * 1.07f
    val arm = (r * 0.22f).coerceIn(10f * strokeScale, 34f * strokeScale)
    anomalyRingPaint.strokeWidth = 2.6f * strokeScale
    anomalyRingPaint.color = Color.parseColor("#FACC15")
    val x0 = cx - bracketR
    val y0 = cy - bracketR
    val x1 = cx + bracketR
    val y1 = cy + bracketR
    canvas.drawLine(x0, y0, x0 + arm, y0, anomalyRingPaint)
    canvas.drawLine(x0, y0, x0, y0 + arm, anomalyRingPaint)
    canvas.drawLine(x1 - arm, y0, x1, y0, anomalyRingPaint)
    canvas.drawLine(x1, y0, x1, y0 + arm, anomalyRingPaint)
    canvas.drawLine(x0, y1 - arm, x0, y1, anomalyRingPaint)
    canvas.drawLine(x0, y1, x0 + arm, y1, anomalyRingPaint)
    canvas.drawLine(x1 - arm, y1, x1, y1, anomalyRingPaint)
    canvas.drawLine(x1, y1 - arm, x1, y1, anomalyRingPaint)
  }

  private fun drawPracticeTutorialWorldHighlight(
    canvas: Canvas,
    worldScale: Float,
    tSec: Float,
  ) {
    val target = engine.resolvePracticeTutorialWorldTarget() ?: return
    val safeScale = worldScale.coerceAtLeast(0.18f)
    val invScale = 1f / safeScale
    val jumpFrac = (engine.practiceTutorialStepJumpFlashSec / 0.42f).coerceIn(0f, 1f)

    drawGoldenRiftOutlineCircle(
      canvas = canvas,
      cx = target.x,
      cy = target.y,
      baseRadius = target.radius,
      strokeScale = invScale * dpDensity * 0.92f,
      tSec = tSec,
      jumpFrac = jumpFrac,
    )

    drawWorldOverheadBanner(
      canvas = canvas,
      text = "TARGET // ${target.tagLabel}",
      worldX = target.x,
      worldY = target.y - target.radius * 1.12f - (14f * invScale),
      fillColor = Color.parseColor("#FFD700"),
      worldScale = worldScale,
      sizeMultiplier = 1.14f,
      alpha = 245,
    )
  }

  private fun drawPracticeTutorialScreenHighlight(
    canvas: Canvas,
    vw: Float,
    vh: Float,
    p: ShipEntity,
    effectiveCamX: Float,
    effectiveCamY: Float,
    worldScale: Float,
    tSec: Float,
  ) {
    val step = engine.currentPracticeTutorialStep() ?: return
    val jumpFrac = (engine.practiceTutorialStepJumpFlashSec / 0.42f).coerceIn(0f, 1f)

    when (step) {
      PracticeTutorialStep.MOVEMENT_AND_BOOST -> {
        val lx = if (leftPointerId != MotionEvent.INVALID_POINTER_ID) leftOriginX else effLeftBaseX()
        val ly = if (leftPointerId != MotionEvent.INVALID_POINTER_ID) leftOriginY else effLeftBaseY()
        val lr = effLeftRadius() + 12f * dpDensity
        drawGoldenRiftOutlineCircle(
          canvas = canvas,
          cx = lx,
          cy = ly,
          baseRadius = lr,
          strokeScale = dpDensity * 0.90f,
          tSec = tSec,
          jumpFrac = jumpFrac,
        )
      }
      PracticeTutorialStep.FIRING_AND_SHARED_FLUX -> {
        val fx = effFireBtnX()
        val fy = effFireBtnY()
        val fr = effFireRadius() + 12f * dpDensity
        drawGoldenRiftOutlineCircle(
          canvas = canvas,
          cx = fx,
          cy = fy,
          baseRadius = fr,
          strokeScale = dpDensity * 0.90f,
          tSec = tSec,
          jumpFrac = jumpFrac,
        )
        if (p.spec.hasBomb) {
          drawGoldenRiftOutlineCircle(
            canvas = canvas,
            cx = effBombBtnX(),
            cy = effBombBtnY(),
            baseRadius = effBombRadius() + 8f * dpDensity,
            strokeScale = dpDensity * 0.72f,
            tSec = tSec,
            jumpFrac = jumpFrac,
          )
        }
      }
      PracticeTutorialStep.BUFF_CRATE,
      PracticeTutorialStep.OBSTACLE,
      PracticeTutorialStep.PLAYFIELD_RIFT,
      PracticeTutorialStep.SAFE_ZONE -> {
        val target = engine.resolvePracticeTutorialWorldTarget() ?: return
        val sx = (target.x - effectiveCamX) * worldScale + vw * 0.5f
        val sy = (target.y - effectiveCamY) * worldScale + vh * 0.5f
        val marginX = 96f * dpDensity
        val marginTop = 148f * dpDensity
        val marginBot = 112f * dpDensity
        val isOffscreen =
          sx < marginX || sx > vw - marginX || sy < marginTop || sy > vh - marginBot
        if (isOffscreen) {
          val cx = vw * 0.5f
          val cy = vh * 0.5f
          val ang = atan2(sy - cy, sx - cx)
          val rx = (vw * 0.5f - marginX).coerceAtLeast(100f)
          val ry = (vh * 0.5f - max(marginTop, marginBot)).coerceAtLeast(70f)
          val px = (cx + cos(ang) * rx).coerceIn(marginX, vw - marginX)
          val py = (cy + sin(ang) * ry).coerceIn(marginTop, vh - marginBot)
          val pulse = 0.5f + 0.5f * sin(tSec * 6.0f)

          hudCircleFillPaint.color = Color.argb(215, 6, 13, 26)
          canvas.drawCircle(px, py, 20f * dpDensity, hudCircleFillPaint)
          hudCircleStrokePaint.strokeWidth = 2.4f * dpDensity
          hudCircleStrokePaint.color =
            Color.argb((210 + (45 * pulse).toInt()).coerceIn(0, 255), 255, 215, 0)
          canvas.drawCircle(px, py, 20f * dpDensity, hudCircleStrokePaint)

          val tipDist = 29f * dpDensity
          val tipX = px + cos(ang) * tipDist
          val tipY = py + sin(ang) * tipDist
          val wingLeftX = px + cos(ang + 0.42f) * (19f * dpDensity)
          val wingLeftY = py + sin(ang + 0.42f) * (19f * dpDensity)
          val wingRightX = px + cos(ang - 0.42f) * (19f * dpDensity)
          val wingRightY = py + sin(ang - 0.42f) * (19f * dpDensity)
          plumePath.reset()
          plumePath.moveTo(tipX, tipY)
          plumePath.lineTo(wingLeftX, wingLeftY)
          plumePath.lineTo(wingRightX, wingRightY)
          plumePath.close()
          hudCircleFillPaint.color = Color.parseColor("#FFD700")
          canvas.drawPath(plumePath, hudCircleFillPaint)

          val distMeters = (hypot(target.x - p.x, target.y - p.y) / 10f).roundToInt()
          hudLabelPaint.textSize = 8.4f * dpDensity
          hudLabelPaint.color = Color.parseColor("#FEF08A")
          canvas.drawText("${distMeters}m", px, py + 3.0f * dpDensity, hudLabelPaint)
          hudLabelPaint.textSize = 7.8f * dpDensity
          hudLabelPaint.color = Color.parseColor("#FFD700")
          canvas.drawText(
            target.tagLabel,
            px,
            py + (if (sin(ang) > 0f) -26f else 32f) * dpDensity,
            hudLabelPaint,
          )
        }
      }
      else -> Unit
    }
  }
}

private const val SAFE_NEXUS_RIFT_AGSL =
  """
  uniform float2 uCenter;
  uniform float uRadius;
  uniform float uTime;
  uniform float uLocked;
  uniform float uOpacity;

  float hash21(float2 p) {
    p = fract(p * float2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
  }

  float noise2d(float2 p) {
    float2 i = floor(p);
    float2 f = fract(p);
    float2 u = f * f * (3.0 - 2.0 * f);
    float a = hash21(i);
    float b = hash21(i + float2(1.0, 0.0));
    float c = hash21(i + float2(0.0, 1.0));
    float d = hash21(i + float2(1.0, 1.0));
    return mix(mix(a, b, u.x), mix(c, d, u.x), u.y);
  }

  float fbm(float2 p) {
    float v = 0.0;
    float amp = 0.5;
    for (int i = 0; i < 4; i++) {
      v += amp * noise2d(p);
      p = p * 2.03 + float2(1.7, -1.3);
      amp *= 0.5;
    }
    return v;
  }

  half4 main(float2 fragCoord) {
    float safeR = max(uRadius, 1.0);
    float2 rel = (fragCoord - uCenter) / safeR;
    float r = length(rel);
    if (r > 1.30) {
      return half4(0.0);
    }

    float2 dir = rel / max(r, 1e-4);
    float locked = clamp(uLocked, 0.0, 1.0);

    if (r < 1.0) {
      // Calm, clear #030A16 Safe Harbor sanctuary inside szR (stabilized by the central 3D NEXUS Station!)
      float rimGlow = smoothstep(0.84, 1.0, r);
      float calmRipple = 0.5 + 0.5 * sin(r * 18.0 - uTime * 1.4);
      float3 calmBase = mix(float3(0.012, 0.039, 0.086), float3(0.11, 0.015, 0.025), locked);
      float3 shieldRim = mix(float3(0.00, 0.96, 0.83), float3(0.96, 0.24, 0.28), locked);
      float3 col = calmBase + shieldRim * (rimGlow * 0.42 + calmRipple * rimGlow * 0.14);
      float alpha = clamp((0.46 + 0.34 * rimGlow) * uOpacity, 0.0, 0.86);
      return half4(half3(col * alpha), half(alpha));
    }

    // Outside szR (r in 1.0..1.30): Volatile cyan/violet/magenta Rift storm tendrils pressing INWARD against the braced shield rim!
    float outFrac = (r - 1.0) / 0.30;
    float2 inwardUv = dir * 3.4 + float2(r * 5.2 + uTime * 0.95, -r * 4.4 - uTime * 0.78);
    float n1 = fbm(inwardUv);
    float n2 = fbm(inwardUv * 1.85 + float2(-uTime * 0.65, uTime * 0.55));
    float tendril = pow(clamp(1.0 - abs(n1 - n2) * 2.5, 0.0, 1.0), 1.85);
    float pressMask = (1.0 - smoothstep(0.0, 1.0, outFrac)) * (0.55 + 0.75 * n1);

    float3 riftCyan = float3(0.00, 0.96, 0.83);
    float3 riftViolet = float3(0.66, 0.33, 0.97);
    float3 riftMagenta = float3(0.93, 0.28, 0.60);
    float3 stormOpen = mix(riftViolet, riftMagenta, n2) * (0.65 + 0.55 * tendril) + riftCyan * (tendril * (1.0 - outFrac) * 0.85);
    float3 stormLocked = mix(float3(0.96, 0.24, 0.28), float3(0.98, 0.62, 0.20), tendril);
    float3 col = mix(stormOpen, stormLocked, locked);
    float alpha = clamp(pressMask * (0.52 + 0.48 * tendril) * uOpacity, 0.0, 0.92);
    return half4(half3(col * alpha), half(alpha));
  }
  """

private const val STORM_BOUNDARY_AGSL =
  """
  uniform float2 uCenter;
  uniform float uRadius;
  uniform float uTime;
  uniform float3 uGlowRGB;
  uniform float3 uTrimRGB;

  float hash21s(float2 p) {
    p = fract(p * float2(234.56, 567.89));
    p += dot(p, p + 34.56);
    return fract(p.x * p.y);
  }

  float noise2ds(float2 p) {
    float2 i = floor(p);
    float2 f = fract(p);
    float2 u = f * f * (3.0 - 2.0 * f);
    float a = hash21s(i);
    float b = hash21s(i + float2(1.0, 0.0));
    float c = hash21s(i + float2(0.0, 1.0));
    float d = hash21s(i + float2(1.0, 1.0));
    return mix(mix(a, b, u.x), mix(c, d, u.x), u.y);
  }

  float fbms(float2 p) {
    float v = 0.0;
    float amp = 0.5;
    for (int i = 0; i < 4; i++) {
      v += amp * noise2ds(p);
      p = p * 2.07 + float2(-1.4, 1.9);
      amp *= 0.5;
    }
    return v;
  }

  half4 main(float2 fragCoord) {
    float2 delta = fragCoord - uCenter;
    float dist = length(delta);
    float signedOff = dist - uRadius;
    if (signedOff < -265.0 || signedOff > 205.0) {
      return half4(0.0);
    }

    // Seamless 2D world-space direction vector (zero atan(y, x) ±PI discontinuity at West wall!)
    float2 dir = delta / max(dist, 1.0);
    float ringScale = max(uRadius, 1.0) * 0.0046;
    float radialCoord = signedOff * 0.014;
    float2 baseWorldUv = dir * ringScale;

    float cloudA = fbms(baseWorldUv * 1.45 + float2(uTime * 0.16, radialCoord - uTime * 0.24));
    float cloudB = fbms(baseWorldUv * 2.95 + float2(-uTime * 0.29, radialCoord * 1.8 + cloudA * 1.3));
    float billow = clamp(cloudA * 0.62 + cloudB * 0.55, 0.0, 1.0);

    float inwardMask = smoothstep(-260.0, -18.0 + (cloudA - 0.5) * 95.0, signedOff);
    float outwardMask = 1.0 - smoothstep(55.0, 200.0, signedOff);
    float stormEnvelope = inwardMask * outwardMask;

    float ridge = 1.0 - abs(cloudA - cloudB) * 3.4;
    float flash = 0.55 + 0.45 * sin(uTime * 7.5 + dot(dir, float2(7.3, -6.1)) + cloudB * 6.0);
    float lightningVein = pow(clamp(ridge, 0.0, 1.0), 4.0) * flash * smoothstep(-150.0, 25.0, signedOff);

    float3 deepStorm = mix(float3(0.03, 0.06, 0.16), uTrimRGB * 0.68, billow);
    float3 plasmaMist = mix(deepStorm, uGlowRGB, clamp(billow * 0.82 + smoothstep(-60.0, 20.0, signedOff) * 0.35, 0.0, 1.0));
    float3 col = plasmaMist + float3(0.88, 0.97, 1.0) * (lightningVein * 1.15);

    float alpha = clamp(stormEnvelope * (0.26 + 0.66 * billow) + lightningVein * 0.45, 0.0, 0.95);
    return half4(half3(col * alpha), half(alpha));
  }
  """

