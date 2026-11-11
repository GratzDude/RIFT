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
import android.graphics.Typeface
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
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
)

class RiftArenaView(
  context: Context,
  val engine: RiftGameEngine,
  val onHudTelemetryTick: (fps: Int) -> Unit = {},
) : View(context) {

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

  private var lastFrameNs: Long = System.nanoTime()
  private var fpsFrameCount: Int = 0
  private var fpsAccumSec: Float = 0f
  var currentFps: Int = 60
    private set

  // Camera state (stationary on death)
  private var camX: Float = engine.localPlayer.x
  private var camY: Float = engine.localPlayer.y

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

  // Procedural Beveled Industrial Wall Paints
  private val wallBodyPaint =
    Paint().apply {
      style = Paint.Style.FILL
      color = Color.parseColor("#0B1320")
    }

  private val wallTopBevelPaint =
    Paint().apply {
      style = Paint.Style.FILL
      color = Color.parseColor("#1E293B")
    }

  private val wallBottomBevelPaint =
    Paint().apply {
      style = Paint.Style.FILL
      color = Color.parseColor("#060A12")
    }

  private val wallHazardPaint =
    Paint().apply {
      style = Paint.Style.STROKE
      strokeWidth = 7f
      shader = RiftSpriteBaker.hazardShader
    }

  private val wallRivetPaint =
    Paint(Paint.ANTI_ALIAS_FLAG).apply {
      style = Paint.Style.FILL
      color = Color.parseColor("#94A3B8")
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
  private val arcRect = RectF()
  private val trajPath = Path()
  private val plumePath = Path()
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
   * Computes the active non-zero utility button slots for the 4-Input Right-Hand Combat Cluster:
   * - Special Buff Weapon Button (Burst, Shotgun, or Lock-On Bouncing Missile)
   * - Repel Button (Defensive repulsion shockwave)
   */
  private fun rebuildActiveUtilitySlots(p: ShipEntity) {
    activeUtilitySlots.clear()
    val showAll = RiftCalibrationState.forceAllButtonsVisible || RiftCalibrationState.isUiUnlockedForDragging
    val hasBombEffective = p.spec.hasBomb || showAll
    val startDeg = if (hasBombEffective) 227.0 else 194.0
    val stepDeg = 38.0
    var slotIndex = 0
    val arcScale = RiftCalibrationState.combatArcScale
    val baseArcX = fireBtnX + RiftCalibrationState.combatArcOffset.x
    val baseArcY = fireBtnY + RiftCalibrationState.combatArcOffset.y
    val scaledDist = arcDistUtil * arcScale
    val scaledR = utilBtnRadius * arcScale

    if (p.specialWeaponCharges > 0 || showAll) {
      val rad = ((startDeg + slotIndex * stepDeg) * PI / 180.0).toFloat()
      val count = if (p.specialWeaponCharges > 0) p.specialWeaponCharges else 2
      val activeType = if (p.specialWeaponCharges > 0 && p.specialWeaponType != "NONE") p.specialWeaponType else "BURST"
      val titleStr =
        when (activeType) {
          "SHOTGUN" -> "SHOTGUN $count"
          "MISSILE" -> "MISSILE $count"
          else -> "BURST $count"
        }
      val strokeCol =
        when (activeType) {
          "SHOTGUN" -> Color.parseColor("#FB7185")
          "MISSILE" -> Color.parseColor("#EF4444")
          else -> Color.parseColor("#F59E0B")
        }
      val fillCol =
        when (activeType) {
          "SHOTGUN" -> Color.argb(155, 88, 18, 38)
          "MISSILE" -> Color.argb(155, 88, 14, 18)
          else -> Color.argb(155, 80, 40, 6)
        }
      activeUtilitySlots.add(
        UtilityButtonSlot(
          type = "SPECIAL",
          title = titleStr,
          sub = "260⚡",
          cx = baseArcX + cos(rad) * scaledDist + RiftCalibrationState.burstBtnOffset.x,
          cy = baseArcY + sin(rad) * scaledDist + RiftCalibrationState.burstBtnOffset.y,
          r = scaledR,
          strokeColor = strokeCol,
          fillColor = fillCol,
          enabled = p.canFire && p.specialWeaponCharges > 0 && (p.flux - 260f >= 1f),
        )
      )
      slotIndex++
    }

    if (p.repels > 0 || showAll) {
      val rad = ((startDeg + slotIndex * stepDeg) * PI / 180.0).toFloat()
      val count = if (p.repels > 0) p.repels else 4
      activeUtilitySlots.add(
        UtilityButtonSlot(
          type = "REPEL",
          title = "REPEL $count",
          sub = "320⚡",
          cx = baseArcX + cos(rad) * scaledDist + RiftCalibrationState.repelBtnOffset.x,
          cy = baseArcY + sin(rad) * scaledDist + RiftCalibrationState.repelBtnOffset.y,
          r = scaledR,
          strokeColor = Color.parseColor("#38BDF8"),
          fillColor = Color.argb(155, 10, 48, 82),
          enabled = p.canFire && p.repels > 0 && (p.flux - 320f >= 1f),
        )
      )
      slotIndex++
    }
  }

  override fun onDraw(canvas: Canvas) {
    super.onDraw(canvas)

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

    // Stationary Death Camera: anchor at deathX, deathY on death
    if (p.respawnTimer > 0f) {
      camX = p.deathX
      camY = p.deathY
    } else {
      camX = p.x
      camY = p.y
    }

    // Non-Linear Camera Trauma Shake (Offset = Trauma^2 * MaxOffset * Noise(t))
    val traumaSq = engine.cameraTrauma * engine.cameraTrauma
    val tSec = engine.traumaTimeSec
    val shakeOffsetX = if (traumaSq > 0.0001f) traumaSq * 14f * sin(tSec * 47.0f) else 0f
    val shakeOffsetY = if (traumaSq > 0.0001f) traumaSq * 14f * cos(tSec * 53.0f) else 0f
    val shakeAngleDeg =
      if (traumaSq > 0.0001f) (traumaSq * 0.035f * sin(tSec * 39.0f)) * (180f / PI.toFloat()) else 0f

    val effectiveCamX = camX + shakeOffsetX
    val effectiveCamY = camY + shakeOffsetY

    // [Z-0] Obsidian Void (#030712) + [Z-1] 3-Tier Parallax Background
    canvas.drawColor(Color.parseColor("#030712"))

    // Tier 3: Volumetric Nebula Radial Gradient Blobs (Pre-rendered offscreen Bitmaps)
    for (nb in nebulaBlobs) {
      val nx = ((nb.x - effectiveCamX * nb.parallax) % vw + vw) % vw
      val ny = ((nb.y - effectiveCamY * nb.parallax) % vh + vh) % vh
      nebulaDstRect.set(nx - nb.radius, ny - nb.radius, nx + nb.radius, ny + nb.radius)
      val stamp = if (nb.isCyan) RiftSpriteBaker.nebulaCyanStamp else RiftSpriteBaker.nebulaIndigoStamp
      canvas.drawBitmap(stamp, null, nebulaDstRect, null)
    }

    // Tier 1 & Tier 2: 200 Distant Stars (drift 0.05) + 30 Dust Motes (drift 0.15)
    for (st in stars) {
      val sx = ((st.x - effectiveCamX * st.parallax) % vw + vw) % vw
      val sy = ((st.y - effectiveCamY * st.parallax) % vh + vh) % vh
      starPaint.color = st.color
      canvas.drawRect(sx, sy, sx + st.size, sy + st.size, starPaint)
    }

    // 3. World-Space Camera Transform (Base 0.65x * Real-Time Camera Zoom Slider [0.5x..1.5x] + Non-Linear Trauma Shake)
    val worldScale = engine.effectiveCameraScale()
    canvas.save()
    canvas.translate(vw * 0.5f, vh * 0.5f)
    if (shakeAngleDeg != 0f) {
      canvas.rotate(shakeAngleDeg)
    }
    canvas.scale(worldScale, worldScale)
    canvas.translate(-effectiveCamX, -effectiveCamY)

    val halfWorldW = (vw / worldScale) * 0.56f
    val halfWorldH = (vh / worldScale) * 0.56f
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

    // 4. Center Safe Zone Pad (Subtle dotted pad + corner brackets) scaled by calibration
    val szBase = engine.safeZone
    val szScale = RiftCalibrationState.safeZoneScale
    val szW = (szBase.right - szBase.left) * szScale
    val szH = (szBase.bottom - szBase.top) * szScale
    val szCX = szBase.centerX()
    val szCY = szBase.centerY()
    val sz = RectF(szCX - szW * 0.5f, szCY - szH * 0.5f, szCX + szW * 0.5f, szCY + szH * 0.5f)
    if (sz.right >= viewLeft && sz.left <= viewRight && sz.bottom >= viewTop && sz.top <= viewBottom) {
      canvas.drawRect(sz, safeFillPaint)
      canvas.drawRect(sz, safeDashPaint)

      val bLen = 28f * szScale
      canvas.drawLine(sz.left, sz.top, sz.left + bLen, sz.top, safeBracketPaint)
      canvas.drawLine(sz.left, sz.top, sz.left, sz.top + bLen, safeBracketPaint)
      canvas.drawLine(sz.right - bLen, sz.top, sz.right, sz.top, safeBracketPaint)
      canvas.drawLine(sz.right, sz.top, sz.right, sz.top + bLen, safeBracketPaint)
      canvas.drawLine(sz.left, sz.bottom - bLen, sz.left, sz.bottom, safeBracketPaint)
      canvas.drawLine(sz.left, sz.bottom, sz.left + bLen, sz.bottom, safeBracketPaint)
      canvas.drawLine(sz.right - bLen, sz.bottom, sz.right, sz.bottom, safeBracketPaint)
      canvas.drawLine(sz.right, sz.bottom - bLen, sz.right, sz.bottom, safeBracketPaint)

      safeTextPaint.textSize = 14f * szScale
      val safeBannerText =
        if (p.inSafeZone && p.respawnTimer <= 0f) {
          val rem = max(0f, RiftCalibrationState.safeZoneIdleLimitSec - p.safeZoneStayTimer)
          String.format(java.util.Locale.US, "SAFE ZONE // WEAPONS DISARMED // EVICTION WARP IN %.1fs", rem)
        } else {
          String.format(java.util.Locale.US, "SAFE ZONE // WEAPONS DISARMED // %.0fs MAX STAY", RiftCalibrationState.safeZoneIdleLimitSec)
        }
      canvas.drawText(
        safeBannerText,
        sz.centerX(),
        sz.top + 26f * szScale,
        safeTextPaint,
      )
    }

    // 5. Procedural Beveled Industrial Walls
    for (w in engine.walls) {
      if (w.x + w.w < viewLeft || w.x > viewRight || w.y + w.h < viewTop || w.y > viewBottom) {
        continue
      }
      drawIndustrialWallSegment(canvas, w)
    }

    // 6. 2x Glowing Buff Icons (88f * 0.89f = 78.32f) with Pulsing Green Glow Aura
    val buffDrawSize = 88f * 0.89f * RiftCalibrationState.crateScale
    val buffHalf = buffDrawSize * 0.5f
    for (g in engine.greens) {
      g.spinPhase += dt * 2.8f
      if (g.x < viewLeft - 90f || g.x > viewRight + 90f || g.y < viewTop - 90f || g.y > viewBottom + 90f) {
        continue
      }
      val bobOffsetY = sin(g.spinPhase) * 3.5f
      val pulse = 0.5f + 0.5f * sin(g.spinPhase * 1.8f)
      val glowRadius = buffHalf * (1.28f + 0.38f * pulse)
      buffGlowPaint.alpha = (155 + (100 * pulse).toInt()).coerceIn(140, 255)
      nebulaDstRect.set(
        g.x - glowRadius,
        g.y + bobOffsetY - glowRadius,
        g.x + glowRadius,
        g.y + bobOffsetY + glowRadius,
      )
      canvas.drawBitmap(RiftSpriteBaker.emeraldGlowStamp, null, nebulaDstRect, buffGlowPaint)

      val buffBmp = RiftSpriteBaker.getBuffIconBitmap(g.buffType)
      nebulaDstRect.set(
        g.x - buffHalf,
        g.y + bobOffsetY - buffHalf,
        g.x + buffHalf,
        g.y + bobOffsetY + buffHalf,
      )
      canvas.drawBitmap(buffBmp, null, nebulaDstRect, null)
    }

    // 6b. Playfield Rift Obstacles (Plasma Rift, Repulsor Rift, Gravity Rift, Phase Rifts at 5x Ship Size)
    for (anom in engine.anomalies) {
      val r = anom.physicalRadius * RiftCalibrationState.anomalyScale // 180f = 5x 36f ship radius
      if (anom.x + r < viewLeft || anom.x - r > viewRight || anom.y + r < viewTop || anom.y - r > viewBottom) {
        continue
      }
      val fadeAlpha = (anom.life / 2.0f).coerceIn(0f, 1f)
      val baseAlphaInt = (255 * fadeAlpha).toInt().coerceIn(0, 255)

      when (anom.type) {
        AnomalyType.PLASMA_RIFT -> {
          // Swirling crimson/magenta damaging Plasma Rift tear (10% Max Flux damage per pass-through)
          anomalyFillPaint.color = Color.argb((58 * fadeAlpha).toInt(), 239, 68, 68)
          canvas.drawCircle(anom.x, anom.y, r * 1.15f, anomalyFillPaint)

          // Pulsing crimson hazard shear boundary ring
          val pulsePhase = (System.currentTimeMillis() % 1100L) / 1100f
          val waveR = r * (0.45f + pulsePhase * 0.55f)
          anomalyRingPaint.strokeWidth = 3.8f
          anomalyRingPaint.color = Color.argb((185 * (1f - pulsePhase) * fadeAlpha).toInt(), 251, 113, 133)
          canvas.drawCircle(anom.x, anom.y, waveR, anomalyRingPaint)

          // Outer 180f Plasma Tear perimeter ring
          anomalyRingPaint.strokeWidth = 3.2f
          anomalyRingPaint.color = Color.argb((210 * fadeAlpha).toInt(), 239, 68, 68)
          canvas.drawCircle(anom.x, anom.y, r, anomalyRingPaint)

          // Swirling jagged plasma filaments
          anomalyRingPaint.strokeWidth = 2.8f
          anomalyRingPaint.color = Color.argb((210 * fadeAlpha).toInt(), 217, 70, 239)
          for (i in 0 until 6) {
            val a = anom.spinAngle + i * (PI.toFloat() / 3f)
            val p1x = anom.x + cos(a) * (r * 0.25f)
            val p1y = anom.y + sin(a) * (r * 0.25f)
            val midX = anom.x + cos(a + 0.28f) * (r * 0.62f)
            val midY = anom.y + sin(a + 0.28f) * (r * 0.62f)
            val p2x = anom.x + cos(a - 0.18f) * (r * 0.96f)
            val p2y = anom.y + sin(a - 0.18f) * (r * 0.96f)
            canvas.drawLine(p1x, p1y, midX, midY, anomalyRingPaint)
            canvas.drawLine(midX, midY, p2x, p2y, anomalyRingPaint)
          }

          // Crimson/white volatile plasma core
          anomalyCorePaint.color = Color.argb(baseAlphaInt, 239, 68, 68)
          canvas.drawCircle(anom.x, anom.y, r * 0.30f, anomalyCorePaint)
          anomalyCorePaint.color = Color.argb(baseAlphaInt, 255, 228, 230)
          canvas.drawCircle(anom.x, anom.y, r * 0.14f, anomalyCorePaint)
        }

        AnomalyType.REPULSOR_RIFT -> {
          // Radiant white/cyan outer accretion disk
          anomalyFillPaint.color = Color.argb((45 * fadeAlpha).toInt(), 56, 189, 248)
          canvas.drawCircle(anom.x, anom.y, r * 1.55f, anomalyFillPaint)

          // Glowing repulsor waves
          val waveOffset = (System.currentTimeMillis() % 1500L) / 1500f
          val waveR = r * (0.4f + waveOffset * 0.95f)
          anomalyRingPaint.strokeWidth = 3.5f
          anomalyRingPaint.color = Color.argb((160 * (1f - waveOffset) * fadeAlpha).toInt(), 255, 255, 255)
          canvas.drawCircle(anom.x, anom.y, waveR, anomalyRingPaint)

          // Spiraling white ejecta prongs
          anomalyRingPaint.strokeWidth = 2.8f
          anomalyRingPaint.color = Color.argb((185 * fadeAlpha).toInt(), 186, 230, 253)
          for (i in 0 until 6) {
            val a = anom.spinAngle + i * (PI.toFloat() / 3f)
            val p1x = anom.x + cos(a) * (r * 0.35f)
            val p1y = anom.y + sin(a) * (r * 0.35f)
            val p2x = anom.x + cos(a + 0.45f) * r
            val p2y = anom.y + sin(a + 0.45f) * r
            canvas.drawLine(p1x, p1y, p2x, p2y, anomalyRingPaint)
          }

          // Blinding white-hot core
          anomalyCorePaint.color = Color.argb(baseAlphaInt, 255, 255, 255)
          canvas.drawCircle(anom.x, anom.y, r * 0.32f, anomalyCorePaint)
          anomalyRingPaint.strokeWidth = 3.2f
          anomalyRingPaint.color = Color.argb(baseAlphaInt, 56, 189, 248)
          canvas.drawCircle(anom.x, anom.y, r * 0.32f, anomalyRingPaint)
        }

        AnomalyType.GRAVITY_RIFT -> {
          // Deep violet gravitational vortex halo
          anomalyFillPaint.color = Color.argb((55 * fadeAlpha).toInt(), 99, 102, 241)
          canvas.drawCircle(anom.x, anom.y, r * 1.6f, anomalyFillPaint)

          // Inward accretion spiral rings
          val waveOffset = 1f - ((System.currentTimeMillis() % 1800L) / 1800f)
          val waveR = r * (0.35f + waveOffset * 0.95f)
          anomalyRingPaint.strokeWidth = 3.6f
          anomalyRingPaint.color = Color.argb((175 * waveOffset * fadeAlpha).toInt(), 192, 132, 252)
          canvas.drawCircle(anom.x, anom.y, waveR, anomalyRingPaint)

          // Inward spiral vortex strands
          anomalyRingPaint.strokeWidth = 2.4f
          anomalyRingPaint.color = Color.argb((150 * fadeAlpha).toInt(), 129, 140, 248)
          for (i in 0 until 5) {
            val a = anom.spinAngle + i * (PI.toFloat() * 2f / 5f)
            val p1x = anom.x + cos(a) * r
            val p1y = anom.y + sin(a) * r
            val p2x = anom.x + cos(a + 0.7f) * (r * 0.38f)
            val p2y = anom.y + sin(a + 0.7f) * (r * 0.38f)
            canvas.drawLine(p1x, p1y, p2x, p2y, anomalyRingPaint)
          }

          // Pitch-black event horizon core + glowing event horizon photon ring
          anomalyRingPaint.strokeWidth = 4.2f
          anomalyRingPaint.color = Color.argb(baseAlphaInt, 168, 85, 247)
          canvas.drawCircle(anom.x, anom.y, r * 0.42f, anomalyRingPaint)
          anomalyCorePaint.color = Color.argb(baseAlphaInt, 3, 7, 18)
          canvas.drawCircle(anom.x, anom.y, r * 0.40f, anomalyCorePaint)
        }

        AnomalyType.PHASE_RIFT_A, AnomalyType.PHASE_RIFT_B -> {
          val isNodeA = anom.type == AnomalyType.PHASE_RIFT_A
          val ringColor = if (isNodeA) Color.parseColor("#2DD4BF") else Color.parseColor("#38BDF8")

          // Luminous transit aura
          anomalyFillPaint.color = Color.argb((45 * fadeAlpha).toInt(), Color.red(ringColor), Color.green(ringColor), Color.blue(ringColor))
          canvas.drawCircle(anom.x, anom.y, r * 1.35f, anomalyFillPaint)

          // Inter-dimensional gateway rings
          val pulsePhase = (System.currentTimeMillis() % 1200L) / 1200f
          val waveR = r * (0.35f + pulsePhase * 0.45f)
          anomalyRingPaint.strokeWidth = 3.2f
          anomalyRingPaint.color = Color.argb((160 * (1f - pulsePhase) * fadeAlpha).toInt(), Color.red(ringColor), Color.green(ringColor), Color.blue(ringColor))
          canvas.drawCircle(anom.x, anom.y, waveR, anomalyRingPaint)

          // Rotating quantum torus gate
          anomalyRingPaint.strokeWidth = 4.0f
          anomalyRingPaint.color = Color.argb(baseAlphaInt, Color.red(ringColor), Color.green(ringColor), Color.blue(ringColor))
          canvas.drawCircle(anom.x, anom.y, r * 0.52f, anomalyRingPaint)

          // Swirling quantum filaments
          anomalyRingPaint.strokeWidth = 2.0f
          anomalyRingPaint.color = Color.argb((190 * fadeAlpha).toInt(), 255, 255, 255)
          for (i in 0 until 4) {
            val a = anom.spinAngle + i * (PI.toFloat() * 0.5f)
            val p1x = anom.x + cos(a) * (r * 0.50f)
            val p1y = anom.y + sin(a) * (r * 0.50f)
            val p2x = anom.x + cos(a + 0.6f) * (r * 0.18f)
            val p2y = anom.y + sin(a + 0.6f) * (r * 0.18f)
            canvas.drawLine(p1x, p1y, p2x, p2y, anomalyRingPaint)
          }

          // Gateway Singularity portal center
          anomalyCorePaint.color = Color.argb(baseAlphaInt, 5, 20, 28)
          canvas.drawCircle(anom.x, anom.y, r * 0.48f, anomalyCorePaint)
          anomalyCorePaint.color = Color.argb((200 * fadeAlpha).toInt(), Color.red(ringColor), Color.green(ringColor), Color.blue(ringColor))
          canvas.drawCircle(anom.x, anom.y, r * 0.18f, anomalyCorePaint)
        }
      }
    }

    // 7. AimingTelegraphSystem — Spread Cones & 2-Bounce Raycast Prediction
    val telegraphScale = RiftCalibrationState.aimTelegraphScale
    if (p.respawnTimer <= 0f && p.canFire) {
      if (engine.fireStickActive && !engine.isCancelActive) {
        val activeAimAngle =
          if (p.independentAimActive) p.weaponAngle else engine.findQuickTapAutoAimAngle(p)
        val streams = if (p.multifireOn) p.spec.primaryStreams + 2 else p.spec.primaryStreams
        val spreadRad =
          if (p.multifireOn) max(0.12f, p.spec.primarySpreadRad * 1.7f) else p.spec.primarySpreadRad

        if (streams > 1 && spreadRad > 0.03f) {
          val halfConeDeg = (spreadRad * (streams - 1) * 0.5f) * (180f / PI.toFloat())
          val centerDeg = activeAimAngle * (180f / PI.toFloat())
          val coneR = 440f * telegraphScale
          arcRect.set(p.x - coneR, p.y - coneR, p.x + coneR, p.y + coneR)
          canvas.drawArc(arcRect, centerDeg - halfConeDeg, halfConeDeg * 2f, true, spreadConePaint)
        }

        val gunBounces = p.spec.primaryBounces + p.bounceBulletsUpgrade
        if (gunBounces > 0) {
          val pts = engine.computeBounceTrajectory(p, activeAimAngle, maxBounces = 2, segmentMaxLen = 540f * telegraphScale)
          drawRaycastTrajectory(canvas, pts, Color.parseColor("#38BDF8"))
        } else {
          val ax = p.x + cos(activeAimAngle) * (500f * telegraphScale)
          val ay = p.y + sin(activeAimAngle) * (500f * telegraphScale)
          canvas.drawLine(p.x, p.y, ax, ay, aimGuidePaint)
        }
      }

      if (engine.bombStickActive && !engine.isBombCancelActive && engine.bombStickMag > 0.12f && p.spec.hasBomb) {
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

    // 8. 2x Bullets scaled by projectileScale
    val projScale = RiftCalibrationState.projectileScale
    for (b in engine.bullets) {
      if (b.x < viewLeft || b.x > viewRight || b.y < viewTop || b.y > viewBottom) continue
      val tailX = b.x - b.vx * 0.036f
      val tailY = b.y - b.vy * 0.036f
      bulletPaint.color = b.color
      bulletPaint.strokeWidth = 6.4f * projScale
      bulletCorePaint.strokeWidth = 2.8f * projScale
      canvas.drawLine(b.x, b.y, tailX, tailY, bulletPaint)
      canvas.drawLine(b.x, b.y, tailX, tailY, bulletCorePaint)
    }

    // 9. 2x Bombs, Mines & Lock-On Bouncing Missiles scaled by projectileScale
    for (bm in engine.bombs) {
      if (bm.x < viewLeft - 60f || bm.x > viewRight + 60f || bm.y < viewTop - 60f || bm.y > viewBottom + 60f) {
        continue
      }
      val c =
        when (bm.type) {
          "EMP" -> Color.parseColor("#B388FF")
          "SIEGE" -> Color.parseColor("#F59E0B")
          "MINE", "MISSILE" -> Color.parseColor("#EF4444")
          else -> Color.parseColor("#FBBF24")
        }
      if (bm.type == "MISSILE") {
        val tailX = bm.x - bm.vx * 0.055f
        val tailY = bm.y - bm.vy * 0.055f
        bulletPaint.color = Color.parseColor("#FB7185")
        bulletPaint.strokeWidth = 8.0f * projScale
        canvas.drawLine(bm.x, bm.y, tailX, tailY, bulletPaint)
        bombRingPaint.color = Color.argb(190, 239, 68, 68)
        canvas.drawCircle(bm.x, bm.y, (bm.radius + 10f) * projScale, bombRingPaint)
      }
      bombPaint.color = c
      canvas.drawCircle(bm.x, bm.y, bm.radius * projScale, bombPaint)
      bombPaint.color = Color.WHITE
      canvas.drawCircle(bm.x, bm.y, bm.radius * 0.44f * projScale, bombPaint)
      if (bm.type == "PROX" || bm.type == "MINE") {
        bombRingPaint.color = Color.argb(145, 239, 68, 68)
        canvas.drawCircle(bm.x, bm.y, (bm.radius + 28f) * projScale, bombRingPaint)
      }
    }

    // 10. 2x Playfield 3D Metallic Ship Sprites (88px) + Split World Labels:
    //     - Zero thruster plumes
    //     - Bounty centered ABOVE ship hull
    //     - [TAG] Callsign centered BELOW ship hull
    for (s in engine.ships) {
      if (s.respawnTimer > 0f) continue
      if (s.x < viewLeft - 110f || s.x > viewRight + 110f || s.y < viewTop - 110f || s.y > viewBottom + 110f) {
        continue
      }

      val isFriendly = engine.areAllied(p, s)

      // 2x 3D Metallic Ship Sprite (88f world units, up from 44f) scaled by calibration
      val bmp =
        RiftSpriteBaker.getShipSprite(
          shipId = s.shipId,
          isAllied = isFriendly,
          isHitFlashing = false,
        )
      val halfBmp = bmp.width * 0.5f
      val drawScale = (88f / bmp.width.toFloat()) * RiftCalibrationState.shipScale
      val renderHeadingRad = if (hypot(s.vx, s.vy) > 2f) atan2(s.vy, s.vx) else s.angle
      shipMatrix.reset()
      shipMatrix.postTranslate(-halfBmp, -halfBmp)
      shipMatrix.postScale(drawScale, drawScale)
      shipMatrix.postRotate(renderHeadingRad * 180f / PI.toFloat())
      shipMatrix.postTranslate(s.x, s.y)
      canvas.drawBitmap(bmp, shipMatrix, null)

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

        popupPaint.color = Color.parseColor("#EF4444")
        popupPaint.alpha = (195 + (60 * pulse).toInt()).coerceIn(0, 255)
        popupPaint.textSize = 14f * RiftCalibrationState.shipScale
        canvas.drawText("⚠ MISSILE LOCK", s.x, s.y - lockR - 8f, popupPaint)
        popupPaint.alpha = 255
      }

      // 1) Customizable Ship Labels (Bounty, Tag, Name) with clock positioning and consolidated mode
      // Omit bounty and name of the player's ship; keep them for all other ships on the playfield.
      if (s.isLocalPlayer) {
        continue
      }

      val inSafe = engine.isInsideSafeZone(s.x, s.y)
      val labelAlpha = if (inSafe) 130 else 255
      val isHvt = s.bounty >= RiftGameEngine.HVT_BOUNTY_THRESHOLD

      val baseTextScale = engine.playfieldTextScale * RiftCalibrationState.labelTextScale
      val bountyTextSize = (if (isHvt) 44.0f else 40.0f) * baseTextScale
      val callsignTextSize = 36.0f * baseTextScale
      val tagTextSize = 34.0f * baseTextScale

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

      if (inBottomConsole || inTopBar || inLeftStick || inRightControls) {
        continue // Skip drawing nametag when projected under HUD buttons so ship names never bleed over MULTI or BRAKE STOP
      }

      val badgePrefix =
        when {
          s.armadaName.isNotEmpty() -> "[${s.armadaName}]"
          s.syndicateTag.isNotEmpty() && s.syndicateTag !in RiftGameEngine.LEGACY_FOUNDRY_TAGS -> "[${s.syndicateTag}]"
          else -> "[GUEST]"
        }
      val bountyText = if (isHvt) "★ ${s.bounty}" else "${s.bounty}"
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

        shipLabelStrokePaint.strokeWidth = 5.2f * baseTextScale
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
          shipLabelStrokePaint.strokeWidth = 5.2f * baseTextScale
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

    for (fp in engine.floatingPopups) {
      if (!fp.active) continue
      val norm = (fp.elapsed / fp.maxLife).coerceIn(0f, 1f)
      val quadFade = (1f - norm * norm).coerceIn(0f, 1f)
      popupPaint.color = fp.color
      popupPaint.alpha = (quadFade * 255f).toInt()
      popupPaint.textSize = 15.5f * fp.scale * RiftCalibrationState.fctScale
      canvas.drawText(fp.text, fp.x, fp.y, popupPaint)
    }
    popupPaint.alpha = 255

    canvas.restore()

    // 12. Screen-Space Multi-Touch Controls (Always-Visible Left Stick + Strict Dynamic Right Combat Arc)
    drawTouchControlsOverlay(canvas, p)

    // 13. Stationary Death Camera 3-Second Respawn Countdown Overlay
    if (p.respawnTimer > 0f) {
      drawStationaryRespawnBanner(canvas, vw, vh, p.respawnTimer)
    }

    // Schedule next 60+ FPS frame
    postInvalidateOnAnimation()
  }

  private fun drawIndustrialWallSegment(canvas: Canvas, w: WallRect) {
    val left = w.x
    val top = w.y
    val right = w.x + w.w
    val bottom = w.y + w.h

    canvas.drawRect(left, top, right, bottom, wallBodyPaint)
    canvas.drawRect(left, top, right, top + 3.5f, wallTopBevelPaint)
    canvas.drawRect(left, top, left + 3.5f, bottom, wallTopBevelPaint)
    canvas.drawRect(left, bottom - 3.5f, right, bottom, wallBottomBevelPaint)
    canvas.drawRect(right - 3.5f, top, right, bottom, wallBottomBevelPaint)
    canvas.drawRect(left + 5f, top + 5f, right - 5f, bottom - 5f, wallHazardPaint)

    var rx = left + 16f
    while (rx <= right - 12f) {
      canvas.drawCircle(rx, top + 11f, 2.2f, wallRivetPaint)
      canvas.drawCircle(rx, bottom - 11f, 2.2f, wallRivetPaint)
      rx += 64f
    }
  }

  private fun drawRaycastTrajectory(canvas: Canvas, pts: List<TrajectoryPoint>, color: Int) {
    if (pts.size < 2) return
    trajectoryLinePaint.color = color
    trajPath.reset()
    trajPath.moveTo(pts[0].x, pts[0].y)
    for (i in 1 until pts.size) {
      trajPath.lineTo(pts[i].x, pts[i].y)
    }
    canvas.drawPath(trajPath, trajectoryLinePaint)
    bombRingPaint.color = color
    for (pt in pts) {
      if (pt.isBounce) {
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

    // 1. Always-Visible Left Dynamic Thumbstick (0.55 opacity when idle, 1.0 on active touch or unlocked)
    val leftActive = leftPointerId != MotionEvent.INVALID_POINTER_ID
    val stickAlphaScale = if (leftActive || unlocked) 1.0f else 0.55f
    val baseLeftX = effLeftBaseX()
    val baseLeftY = effLeftBaseY()
    val effLeftR = effLeftRadius()
    val lx = if (leftActive) leftOriginX else baseLeftX
    val ly = if (leftActive) leftOriginY else baseLeftY
    val kx = if (leftActive) leftCurX else baseLeftX
    val ky = if (leftActive) leftCurY else baseLeftY

    hudCircleFillPaint.color = Color.argb((110 * stickAlphaScale).toInt(), 8, 18, 34)
    canvas.drawCircle(lx, ly, effLeftR, hudCircleFillPaint)

    hudCircleStrokePaint.color =
      if (p.afterburning) {
        Color.argb((255 * stickAlphaScale).toInt(), 245, 158, 11)
      } else {
        Color.argb((220 * stickAlphaScale).toInt(), 56, 189, 248)
      }
    canvas.drawCircle(lx, ly, effLeftR, hudCircleStrokePaint)

    if (unlocked) {
      canvas.drawCircle(lx, ly, effLeftR + 4f * dpDensity, unlockDashPaint)
    }

    // Afterburner 80% threshold ring
    hudCircleStrokePaint.strokeWidth = 1.5f
    hudCircleStrokePaint.color = Color.argb((110 * stickAlphaScale).toInt(), 245, 158, 11)
    canvas.drawCircle(lx, ly, effLeftR * 0.80f, hudCircleStrokePaint)
    hudCircleStrokePaint.strokeWidth = 2.6f

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
      if (unlocked) "DRAG" else if (p.afterburning) "BURN" else "THRUST",
      kx,
      ky + 3.2f * dpDensity,
      hudLabelPaint,
    )

    val disarmedInSafeZone = !p.canFire && !unlocked
    val gunCost = (p.spec.primaryCost * (if (p.multifireOn) 1.55f else 1f)).toInt()
    val arcScale = RiftCalibrationState.combatArcScale

    // 2. Primary Right [FIRE] Stick with 32dp Cancel Deadzone Ring
    val fX = effFireBtnX()
    val fY = effFireBtnY()
    val fR = effFireRadius()
    val fireAlpha = if (disarmedInSafeZone) 110 else 255
    hudCircleFillPaint.color =
      when {
        disarmedInSafeZone -> Color.argb(85, 45, 20, 28)
        engine.isCancelActive -> Color.argb(190, 127, 29, 29)
        engine.fireStickActive -> Color.argb(175, 95, 16, 30)
        else -> Color.argb(145, 65, 12, 22)
      }
    canvas.drawCircle(fX, fY, fR, hudCircleFillPaint)
    hudCircleStrokePaint.color =
      if (disarmedInSafeZone) Color.argb(100, 239, 68, 68) else Color.parseColor("#EF4444")
    canvas.drawCircle(fX, fY, fR, hudCircleStrokePaint)

    if (unlocked) {
      canvas.drawCircle(fX, fY, fR + 4f * dpDensity, unlockDashPaint)
    }

    if (engine.fireStickActive && fireHasExitedDeadzone) {
      hudCircleStrokePaint.strokeWidth = 2.0f
      hudCircleStrokePaint.color =
        if (engine.isCancelActive) Color.parseColor("#EF4444") else Color.argb(110, 239, 68, 68)
      canvas.drawCircle(fX, fY, cancelDeadzonePx, hudCircleStrokePaint)
      hudCircleStrokePaint.strokeWidth = 2.6f
    }

    val fkX = if (firePointerId != MotionEvent.INVALID_POINTER_ID) fireCurX else fX
    val fkY = if (firePointerId != MotionEvent.INVALID_POINTER_ID) fireCurY else fY
    hudCircleFillPaint.color =
      when {
        disarmedInSafeZone -> Color.argb(95, 140, 40, 52)
        engine.isCancelActive -> Color.parseColor("#EF4444")
        engine.fireStickActive -> Color.argb(235, 239, 68, 68)
        else -> Color.argb(185, 210, 40, 60)
      }
    canvas.drawCircle(fkX, fkY, fR * 0.56f, hudCircleFillPaint)

    hudLabelPaint.textSize = 11.0f * dpDensity * arcScale
    hudLabelPaint.color = Color.argb(fireAlpha, 255, 255, 255)
    canvas.drawText(
      if (engine.isCancelActive) "✕" else "FIRE",
      fkX,
      fkY - 1.5f * dpDensity * arcScale,
      hudLabelPaint,
    )
    hudLabelPaint.textSize = 8.5f * dpDensity * arcScale
    hudLabelPaint.color = Color.argb(fireAlpha, 254, 202, 202)
    canvas.drawText(
      when {
        unlocked -> "DRAG"
        engine.isCancelActive -> "CANCEL"
        disarmedInSafeZone -> "SAFE"
        else -> "${gunCost}⚡"
      },
      fkX,
      fkY + 10.0f * dpDensity * arcScale,
      hudLabelPaint,
    )

    // 3. Secondary [BOMB / MINE] Circular Stick — ONLY rendered for bomb/mine ships (or when forceAllButtonsVisible/unlocked is on)
    if (p.spec.hasBomb || RiftCalibrationState.forceAllButtonsVisible || unlocked) {
      val bX = effBombBtnX()
      val bY = effBombBtnY()
      val bR = effBombRadius()
      val bombCost = if (p.spec.hasBomb) p.spec.bombCost else 180f
      val bombAvailable = (p.canFire && (p.flux - bombCost >= 1f)) || unlocked
      val bombAlpha = if (bombAvailable) 255 else 110
      hudCircleFillPaint.color =
        if (engine.isBombCancelActive) {
          Color.argb(195, 127, 29, 29)
        } else if (engine.bombStickActive && bombAvailable) {
          Color.argb(215, 245, 158, 11)
        } else {
          Color.argb(if (bombAvailable) 155 else 85, 95, 62, 8)
        }
      canvas.drawCircle(bX, bY, bR, hudCircleFillPaint)
      hudCircleStrokePaint.color =
        if (engine.isBombCancelActive) {
          Color.parseColor("#EF4444")
        } else {
          Color.argb(bombAlpha, 245, 158, 11)
        }
      canvas.drawCircle(bX, bY, bR, hudCircleStrokePaint)

      if (unlocked) {
        canvas.drawCircle(bX, bY, bR + 3.5f * dpDensity, unlockDashPaint)
      }

      val bLabel =
        when {
          engine.isBombCancelActive -> "✕"
          p.spec.bombType == "MINE" -> "MINE"
          p.spec.bombType == "EMP" -> "EMP"
          p.spec.bombType == "SIEGE" -> "SIEGE"
          p.spec.bombType == "PROX" -> "PROX"
          p.spec.bombType == "BOUNCE" -> "BOUNCE"
          p.spec.bombType == "BURST" -> "BURST"
          p.spec.bombType == "SHOTGUN" -> "SHOTGN"
          p.spec.bombType == "MISSILE" -> "MISSILE"
          else -> "BOMB"
        }
      val bSub =
        when {
          unlocked -> "DRAG"
          engine.isBombCancelActive -> "CANCEL"
          else -> "${bombCost.toInt()}⚡"
        }
      hudLabelPaint.textSize = 9.5f * dpDensity * arcScale
      hudLabelPaint.color = Color.argb(bombAlpha, 255, 255, 255)
      canvas.drawText(bLabel, bX, bY - 1.2f * dpDensity * arcScale, hudLabelPaint)
      hudLabelPaint.textSize = 8.0f * dpDensity * arcScale
      hudLabelPaint.color = Color.argb(bombAlpha, 253, 224, 71)
      canvas.drawText(bSub, bX, bY + 9.2f * dpDensity * arcScale, hudLabelPaint)
    }

    // 4. Dynamic Upper-Arc Satellite Utility Buttons (SPECIAL BUFF SLOT & REPEL — strictly hidden when count == 0)
    for (slot in activeUtilitySlots) {
      drawSatelliteButton(
        canvas = canvas,
        cx = slot.cx,
        cy = slot.cy,
        r = slot.r,
        title = slot.title,
        sub = if (unlocked) "DRAG" else slot.sub,
        strokeColor = slot.strokeColor,
        fillColor = slot.fillColor,
        enabled = slot.enabled || unlocked,
        unlocked = unlocked,
        arcScale = arcScale,
      )
    }
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
  ) {
    val alphaVal = if (enabled) 255 else 115
    hudCircleFillPaint.color =
      Color.argb(
        if (enabled) 155 else 75,
        Color.red(fillColor),
        Color.green(fillColor),
        Color.blue(fillColor),
      )
    canvas.drawCircle(cx, cy, r, hudCircleFillPaint)
    hudCircleStrokePaint.color =
      Color.argb(
        alphaVal,
        Color.red(strokeColor),
        Color.green(strokeColor),
        Color.blue(strokeColor),
      )
    canvas.drawCircle(cx, cy, r, hudCircleStrokePaint)

    if (unlocked) {
      canvas.drawCircle(cx, cy, r + 3.2f * dpDensity, unlockDashPaint)
    }

    hudLabelPaint.textSize = 8.6f * dpDensity * arcScale
    hudLabelPaint.color = Color.argb(alphaVal, 255, 255, 255)
    canvas.drawText(title, cx, cy - 1.0f * dpDensity * arcScale, hudLabelPaint)
    hudLabelPaint.textSize = 7.8f * dpDensity * arcScale
    hudLabelPaint.color =
      Color.argb(
        alphaVal,
        Color.red(strokeColor),
        Color.green(strokeColor),
        Color.blue(strokeColor),
      )
    canvas.drawText(sub, cx, cy + 8.6f * dpDensity * arcScale, hudLabelPaint)
  }

  private fun drawStationaryRespawnBanner(canvas: Canvas, vw: Float, vh: Float, timer: Float) {
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
    canvas.drawText(
      "RECONSTRUCTING AT SAFE ZONE IN ${secStr}s...",
      vw * 0.5f,
      top + 58f * dpDensity,
      hudLabelPaint,
    )
  }

  @SuppressLint("ClickableViewAccessibility")
  override fun onTouchEvent(event: MotionEvent): Boolean {
    if (height > width) return true
    val action = event.actionMasked
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

        // 2. Check Secondary [BOMB / MINE] Concentric Slot
        if ((p.spec.hasBomb || RiftCalibrationState.forceAllButtonsVisible) &&
          hypot(x - bX, y - bY) <= bR * 1.18f
        ) {
          if (!p.canFire) {
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
}
