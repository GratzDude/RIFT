package com.example.engine

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

object RiftSpriteBaker {
  val alliedShipBitmaps: Map<Int, Bitmap>
  val hostileShipBitmaps: Map<Int, Bitmap>
  val flashShipBitmaps: Map<Int, Bitmap>
  val buffIconBitmaps: Map<Int, Bitmap>
  var riftAnomalyIconBitmaps: Map<String, Bitmap>
    private set
  var fluxIconBitmap: Bitmap = bake3dFluxReactorIcon(64)
    private set
  var resonanceIconBitmap: Bitmap = bake3dResonanceCrestIcon(64)
    private set
  var riftShardIconBitmap: Bitmap = bake3dRiftShardIcon(64)
    private set
  var safeZoneStationBitmap: Bitmap = bakeSafeZoneDs9StationBitmap(256)
  var spriteVisualRevision: Int = 1
    private set
  var activeHullTrimStyle: Int = 0 // 0=Standard, 1=Obsidian Ice-Cyan, 2=Sovereign Gold, 3=Rift Violet
  private val trimmedShipSpriteCache = mutableMapOf<Int, Bitmap>()
  private val spaceObstacleBitmapCache = mutableMapOf<Int, Bitmap>()

  var wallTileBitmap: Bitmap
    private set
  val hazardBorderBitmap: Bitmap
  var wallShader: BitmapShader
    private set
  val hazardShader: BitmapShader

  // Pre-rendered offscreen radial glow & volumetric nebula stamps (Zero shadowBlur in render loop)
  val cyanGlowStamp: Bitmap
  val amberGlowStamp: Bitmap
  val crimsonGlowStamp: Bitmap
  val emeraldGlowStamp: Bitmap
  val nebulaCyanStamp: Bitmap
  val nebulaIndigoStamp: Bitmap

  var customShip9AlliedBitmap: Bitmap
    private set
  var customShip9HostileBitmap: Bitmap
    private set
  var customShip9FlashBitmap: Bitmap
    private set
  var customShip9Version: Int = 1
    private set

  init {
    wallTileBitmap = bakeWallTile()
    hazardBorderBitmap = bakeHazardBorderTile()
    wallShader = BitmapShader(wallTileBitmap, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
    hazardShader = BitmapShader(hazardBorderBitmap, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)

    cyanGlowStamp = createPreRenderedGlow(48, Color.parseColor("#38BDF8"))
    amberGlowStamp = createPreRenderedGlow(48, Color.parseColor("#F59E0B"))
    crimsonGlowStamp = createPreRenderedGlow(48, Color.parseColor("#EF4444"))
    emeraldGlowStamp = createPreRenderedGlow(48, Color.parseColor("#10B981"))
    nebulaCyanStamp = createPreRenderedGlow(256, Color.argb(38, 14, 116, 144))
    nebulaIndigoStamp = createPreRenderedGlow(256, Color.argb(42, 49, 46, 129))

    val allied = mutableMapOf<Int, Bitmap>()
    val hostile = mutableMapOf<Int, Bitmap>()
    val flash = mutableMapOf<Int, Bitmap>()
    for (spec in RiftCodex.SHIPS) {
      allied[spec.id] = bakeShipBitmap(spec, 128, isAllied = true)
      hostile[spec.id] = bakeShipBitmap(spec, 128, isAllied = false)
      flash[spec.id] = bakeSolidWhiteFlashBitmap(allied[spec.id]!!)
    }
    alliedShipBitmaps = allied
    hostileShipBitmaps = hostile
    flashShipBitmaps = flash

    val initialRecipe = RiftCodex.activeCustomShipSlot.visualRecipe
    customShip9AlliedBitmap = bakeCustomShip9Bitmap(initialRecipe, 128, isAllied = true)
    customShip9HostileBitmap = bakeCustomShip9Bitmap(initialRecipe, 128, isAllied = false)
    customShip9FlashBitmap = bakeSolidWhiteFlashBitmap(customShip9AlliedBitmap)

    val buffs = mutableMapOf<Int, Bitmap>()
    for (buffType in 0..11) {
      buffs[buffType] = bakeBuffIconBitmap(buffType, 68)
    }
    buffs[-10] = bakeBuffIconBitmap(-10, 68) // Speed Overcharge
    buffs[-11] = bakeBuffIconBitmap(-11, 68) // Thrust Overcharge
    buffs[-12] = bakeBuffIconBitmap(-12, 68) // Reactor Overcharge
    buffs[-13] = bakeBuffIconBitmap(-13, 68) // Turn Radius Overcharge
    buffs[-14] = bakeBuffIconBitmap(-14, 68) // Max Flux Overcharge
    buffIconBitmaps = buffs

    riftAnomalyIconBitmaps = buildRiftAnomalyIconMap()
  }

  private fun buildRiftAnomalyIconMap(): Map<String, Bitmap> =
    mapOf(
      "plasma_rift" to bakeRiftAnomalyIconBitmap("plasma_rift", 96),
      "repulsor_rift" to bakeRiftAnomalyIconBitmap("repulsor_rift", 96),
      "gravity_rift" to bakeRiftAnomalyIconBitmap("gravity_rift", 96),
      "phase_rift" to bakeRiftAnomalyIconBitmap("phase_rift", 96),
      "phase_rift_pair" to bakeRiftAnomalyIconBitmap("phase_rift", 96),
      "unstable_rift" to bakeRiftAnomalyIconBitmap("unstable_rift", 96),
    )

  /**
   * Re-bakes or invalidates cached sprites immediately when any `ForgeElementTarget` recipe is
   * edited in `NEXUS FORGE`.
   */
  fun invalidateVisualTarget(target: ForgeElementTarget) {
    when (target) {
      ForgeElementTarget.OBSTACLE_ORBITAL_STATION,
      ForgeElementTarget.OBSTACLE_ASTEROID_STATION,
      ForgeElementTarget.OBSTACLE_SPACE_ASTEROID,
      ForgeElementTarget.OBSTACLE_FLOATING_DEBRIS,
      ForgeElementTarget.PLACED_STASIS_BARRIER -> {
        spaceObstacleBitmapCache.clear()
      }
      ForgeElementTarget.SAFE_ZONE_WALL,
      ForgeElementTarget.SAFE_ZONE_PAD -> {
        wallTileBitmap = bakeWallTile()
        wallShader = BitmapShader(wallTileBitmap, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
        safeZoneStationBitmap = bakeSafeZoneDs9StationBitmap(256)
      }
      ForgeElementTarget.CURRENCY_FLUX_ICON -> {
        fluxIconBitmap = bake3dFluxReactorIcon(64)
      }
      ForgeElementTarget.CURRENCY_SHARD_ICON -> {
        riftShardIconBitmap = bake3dRiftShardIcon(64)
      }
      ForgeElementTarget.RIFT_PLASMA,
      ForgeElementTarget.RIFT_REPULSOR,
      ForgeElementTarget.RIFT_GRAVITY,
      ForgeElementTarget.RIFT_PHASE_PAIR -> {
        riftAnomalyIconBitmaps = buildRiftAnomalyIconMap()
      }
      else -> Unit
    }
    spriteVisualRevision++
  }

  fun invalidateAllVisualTargets() {
    spaceObstacleBitmapCache.clear()
    wallTileBitmap = bakeWallTile()
    wallShader = BitmapShader(wallTileBitmap, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
    safeZoneStationBitmap = bakeSafeZoneDs9StationBitmap(256)
    fluxIconBitmap = bake3dFluxReactorIcon(64)
    resonanceIconBitmap = bake3dResonanceCrestIcon(64)
    riftShardIconBitmap = bake3dRiftShardIcon(64)
    riftAnomalyIconBitmaps = buildRiftAnomalyIconMap()
    spriteVisualRevision++
  }

  fun getRiftAnomalyIconBitmap(riftId: String): Bitmap {
    return riftAnomalyIconBitmaps[riftId] ?: riftAnomalyIconBitmaps["plasma_rift"]!!
  }

  fun bakeFluxCrystalIconBitmap(sizePx: Int = 64): Bitmap = bake3dFluxReactorIcon(sizePx)

  /**
   * Draws the canonical 3D Swirling Plasma Toroid / Tri-Arc Vortex Coil Flux icon:
   * 1. Deep dark magnetic containment shadow disc (`#040D1A`) for crisp contrast on any background.
   * 2. Three 120°-spaced swirling helical plasma ribbons (`#0284C7` -> `#38BDF8` -> `#00F5D4`)
   *    spiraling inward like a miniature magnetic fusion tokamak, plus segmented containment arcs.
   * 3. Volumetric 3D spherical plasma fusion core (`#E0F2FE` / `#FFFFFF` top-left specular highlight)
   *    and 3 tiny orbital plasma spark nodes — 100% circular/helical energy, never a diamond.
   */
  fun drawFluxPrismIcon(
    c: Canvas,
    cx: Float,
    cy: Float,
    r: Float,
    alpha: Int = 255,
    recipeOverride: VisualElementRecipe? = null,
  ) {
    val rec =
      recipeOverride
        ?: runCatching { RiftVisualForgeState.getRecipe(ForgeElementTarget.CURRENCY_FLUX_ICON) }
          .getOrNull()
    val aFrac = (alpha.coerceIn(0, 255)) / 255f
    if (aFrac <= 0.005f || r <= 0.5f) return

    fun argbHex(baseAlpha: Int, red: Int, green: Int, blue: Int): Int =
      Color.argb((baseAlpha * aFrac).toInt().coerceIn(0, 255), red, green, blue)
    fun argbColor(baseAlpha: Int, colorInt: Int): Int =
      argbHex(baseAlpha, Color.red(colorInt), Color.green(colorInt), Color.blue(colorInt))

    val isDefaultRecipe = rec == null || rec.isFactoryDefault
    val scaledR = r * (rec?.pixelScale ?: 1f).coerceIn(0.65f, 1.50f)
    val glowMult = (rec?.glowIntensity ?: 1f).coerceIn(0f, 2f)
    val borderMult = (rec?.borderWeight ?: 1f).coerceIn(0.5f, 2.2f)

    val glowCol = if (isDefaultRecipe) Color.rgb(0, 245, 212) else rec!!.glowColorInt
    val skyCol = if (isDefaultRecipe) Color.rgb(56, 189, 248) else rec!!.trimColorInt
    val deepAzureCol =
      if (isDefaultRecipe) {
        Color.rgb(2, 132, 199)
      } else {
        Color.rgb(
          (Color.red(skyCol) * 0.45f + Color.red(rec!!.primaryColorInt) * 0.35f).toInt().coerceIn(0, 255),
          (Color.green(skyCol) * 0.58f + Color.green(rec.primaryColorInt) * 0.25f).toInt().coerceIn(0, 255),
          (Color.blue(skyCol) * 0.82f + Color.blue(rec.primaryColorInt) * 0.18f).toInt().coerceIn(0, 255),
        )
      }
    val containmentDiscCol =
      if (isDefaultRecipe) Color.rgb(4, 13, 26) else rec!!.primaryColorInt

    val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    val strokePaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
      }

    // 1. Soft outer radial plasma bloom
    if (glowMult > 0.05f) {
      val bloomR = scaledR * 1.16f
      fillPaint.shader =
        RadialGradient(
          cx,
          cy,
          bloomR.coerceAtLeast(1f),
          intArrayOf(
            argbColor((92 * glowMult).toInt().coerceIn(0, 220), glowCol),
            argbColor((42 * glowMult).toInt().coerceIn(0, 160), skyCol),
            argbColor(0, deepAzureCol),
          ),
          floatArrayOf(0f, 0.62f, 1f),
          Shader.TileMode.CLAMP,
        )
      c.drawCircle(cx, cy, bloomR, fillPaint)
      fillPaint.shader = null
    }

    // 2. Deep dark magnetic containment shadow disc (#040D1A)
    val discR = scaledR * 0.90f
    fillPaint.color = argbColor(242, containmentDiscCol)
    c.drawCircle(cx, cy, discR, fillPaint)

    // Subtle recessed tokamak magnetic track & segmented outer containment arcs
    strokePaint.strokeWidth = (scaledR * 0.08f * borderMult).coerceAtLeast(0.8f)
    strokePaint.color = argbColor(85, deepAzureCol)
    c.drawCircle(cx, cy, scaledR * 0.72f, strokePaint)

    val ringR = scaledR * 0.88f
    val ringRect = RectF(cx - ringR, cy - ringR, cx + ringR, cy + ringR)
    strokePaint.strokeWidth = (scaledR * 0.095f * borderMult).coerceAtLeast(0.9f)
    strokePaint.color = argbColor(175, skyCol)
    for (seg in 0 until 3) {
      c.drawArc(ringRect, -78f + seg * 120f, 64f, false, strokePaint)
    }

    // 3. Three 120°-spaced swirling helical plasma ribbons (#0284C7 -> #38BDF8 -> #00F5D4)
    val armPath = Path()
    val filamentPath = Path()
    for (arm in 0 until 3) {
      val baseRad = (-Math.PI * 0.5 + arm * (Math.PI * 2.0 / 3.0)).toFloat()
      val a0 = baseRad
      val aMidOut = baseRad + 0.92f
      val aMidIn = baseRad + 1.06f
      val aTip = baseRad + 2.05f
      val aTailIn = baseRad + 0.16f

      val rOuter = scaledR * 0.85f
      val rMidOut = scaledR * 0.69f
      val rMidIn = scaledR * 0.45f
      val rTip = scaledR * 0.30f
      val rTailIn = scaledR * 0.74f

      val x0 = cx + kotlin.math.cos(a0) * rOuter
      val y0 = cy + kotlin.math.sin(a0) * rOuter
      val xCtrlOut = cx + kotlin.math.cos(aMidOut) * rMidOut
      val yCtrlOut = cy + kotlin.math.sin(aMidOut) * rMidOut
      val xTip = cx + kotlin.math.cos(aTip) * rTip
      val yTip = cy + kotlin.math.sin(aTip) * rTip
      val xCtrlIn = cx + kotlin.math.cos(aMidIn) * rMidIn
      val yCtrlIn = cy + kotlin.math.sin(aMidIn) * rMidIn
      val xTailIn = cx + kotlin.math.cos(aTailIn) * rTailIn
      val yTailIn = cy + kotlin.math.sin(aTailIn) * rTailIn

      armPath.reset()
      armPath.moveTo(x0, y0)
      armPath.quadTo(xCtrlOut, yCtrlOut, xTip, yTip)
      armPath.quadTo(xCtrlIn, yCtrlIn, xTailIn, yTailIn)
      armPath.close()

      fillPaint.shader =
        LinearGradient(
          x0,
          y0,
          xTip,
          yTip,
          intArrayOf(
            argbColor(235, deepAzureCol),
            argbColor(250, skyCol),
            argbColor(255, glowCol),
          ),
          floatArrayOf(0f, 0.52f, 1f),
          Shader.TileMode.CLAMP,
        )
      c.drawPath(armPath, fillPaint)
      fillPaint.shader = null

      // Luminous #E0F2FE inner helical plasma filament along each ribbon spine
      val xSpineCtrl = cx + kotlin.math.cos(baseRad + 0.98f) * (scaledR * 0.58f)
      val ySpineCtrl = cy + kotlin.math.sin(baseRad + 0.98f) * (scaledR * 0.58f)
      val xSpineStart = cx + kotlin.math.cos(baseRad + 0.08f) * (scaledR * 0.80f)
      val ySpineStart = cy + kotlin.math.sin(baseRad + 0.08f) * (scaledR * 0.80f)
      filamentPath.reset()
      filamentPath.moveTo(xSpineStart, ySpineStart)
      filamentPath.quadTo(xSpineCtrl, ySpineCtrl, xTip, yTip)
      strokePaint.strokeWidth = (scaledR * 0.085f * borderMult).coerceAtLeast(0.8f)
      strokePaint.color = argbHex(225, 224, 242, 254)
      c.drawPath(filamentPath, strokePaint)

      // Tiny orbital plasma spark node per vortex arm
      val sparkAngle = baseRad + 0.36f
      val sparkR = scaledR * 0.77f
      val sx = cx + kotlin.math.cos(sparkAngle) * sparkR
      val sy = cy + kotlin.math.sin(sparkAngle) * sparkR
      fillPaint.color = argbColor(190, glowCol)
      c.drawCircle(sx, sy, (scaledR * 0.12f).coerceAtLeast(0.9f), fillPaint)
      fillPaint.color = argbHex(255, 255, 255, 255)
      c.drawCircle(sx, sy, (scaledR * 0.06f).coerceAtLeast(0.55f), fillPaint)
    }

    // 4. Volumetric 3D spherical plasma fusion core + offset specular highlight
    val coreR = (scaledR * 0.36f).coerceAtLeast(1.2f)
    fillPaint.color = argbColor(150, glowCol)
    c.drawCircle(cx, cy, coreR * 1.24f, fillPaint)

    val specOffX = cx - coreR * 0.28f
    val specOffY = cy - coreR * 0.28f
    fillPaint.shader =
      RadialGradient(
        specOffX,
        specOffY,
        (coreR * 1.32f).coerceAtLeast(1.2f),
        intArrayOf(
          argbHex(255, 255, 255, 255),
          argbHex(252, 224, 242, 254),
          argbColor(248, glowCol),
          argbColor(242, deepAzureCol),
        ),
        floatArrayOf(0f, 0.30f, 0.70f, 1f),
        Shader.TileMode.CLAMP,
      )
    c.drawCircle(cx, cy, coreR, fillPaint)
    fillPaint.shader = null

    strokePaint.strokeWidth = (scaledR * 0.065f * borderMult).coerceAtLeast(0.75f)
    strokePaint.color = argbHex(210, 224, 242, 254)
    c.drawCircle(cx, cy, coreR, strokePaint)

    // Crisp top-left #FFFFFF specular droplet highlight
    fillPaint.color = argbHex(255, 255, 255, 255)
    c.drawCircle(
      cx - coreR * 0.32f,
      cy - coreR * 0.32f,
      (coreR * 0.27f).coerceAtLeast(0.65f),
      fillPaint,
    )
  }

  /**
   * Bakes the 3D Swirling Plasma Toroid / Tri-Arc Vortex Coil (`fluxIconBitmap`) with an ambient halo.
   */
  fun bake3dFluxReactorIcon(sizePx: Int = 64): Bitmap {
    val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val c = Canvas(bmp)
    val cx = sizePx * 0.5f
    val cy = sizePx * 0.5f
    val r = sizePx * 0.41f
    val rec =
      runCatching { RiftVisualForgeState.getRecipe(ForgeElementTarget.CURRENCY_FLUX_ICON) }
        .getOrNull()
    val isDefault = rec == null || rec.isFactoryDefault
    val trim = if (isDefault) Color.rgb(56, 189, 248) else rec!!.trimColorInt
    val glow = if (isDefault) Color.rgb(0, 245, 212) else rec!!.glowColorInt
    val glowMult = (rec?.glowIntensity ?: 1f).coerceIn(0f, 2f)
    val haloPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        shader =
          RadialGradient(
            cx,
            cy,
            (sizePx * 0.49f).coerceAtLeast(4f),
            intArrayOf(
              Color.argb(
                (170 * glowMult).toInt().coerceIn(0, 255),
                Color.red(glow),
                Color.green(glow),
                Color.blue(glow),
              ),
              Color.argb(
                (78 * glowMult).toInt().coerceIn(0, 255),
                Color.red(trim),
                Color.green(trim),
                Color.blue(trim),
              ),
              Color.argb(0, Color.red(trim), Color.green(trim), Color.blue(trim)),
            ),
            floatArrayOf(0f, 0.56f, 1f),
            Shader.TileMode.CLAMP,
          )
      }
    c.drawCircle(cx, cy, sizePx * 0.49f, haloPaint)
    drawFluxPrismIcon(c, cx, cy, r, alpha = 255, recipeOverride = rec)
    return bmp
  }

  /**
   * Draws the custom Solar-Gold & Cyan Harmonic Winged Tuning Crest (`resonanceIconBitmap`):
   * - Deep midnight containment backing disc (`#050E1D`) with subtle gold/cyan corona.
   * - Twin outward-radiating curved acoustic resonance wave arcs (`#00F5D4` outer + `#FACC15` inner).
   * - Sleek upward-swept winged solar-gold chevron (`#F59E0B` -> `#FACC15` -> `#FEF08A`) cradling
   *   a bright white-gold harmonic tuning sphere (`#FFFFFF` -> `#FACC15` -> `#00F5D4`).
   */
  fun drawResonanceCrestIcon(
    c: Canvas,
    cx: Float,
    cy: Float,
    r: Float,
    alpha: Int = 255,
  ) {
    val aFrac = (alpha.coerceIn(0, 255)) / 255f
    if (aFrac <= 0.005f || r <= 0.5f) return
    fun aCol(a: Int, red: Int, green: Int, blue: Int): Int =
      Color.argb((a * aFrac).toInt().coerceIn(0, 255), red, green, blue)

    val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    val strokePaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
      }

    // 1. Ambient solar-cyan corona + dark midnight contrast disc
    fillPaint.shader =
      RadialGradient(
        cx,
        cy,
        (r * 1.05f).coerceAtLeast(1f),
        intArrayOf(aCol(110, 250, 204, 21), aCol(62, 0, 245, 212), aCol(0, 5, 14, 29)),
        floatArrayOf(0f, 0.62f, 1f),
        Shader.TileMode.CLAMP,
      )
    c.drawCircle(cx, cy, r * 1.05f, fillPaint)
    fillPaint.shader = null

    fillPaint.color = aCol(235, 5, 14, 29)
    c.drawCircle(cx, cy, r * 0.88f, fillPaint)

    // 2. Twin flanking curved harmonic resonance wave arcs (left & right)
    val outerWaveRect = RectF(cx - r * 0.94f, cy - r * 0.86f, cx + r * 0.94f, cy + r * 0.86f)
    strokePaint.strokeWidth = (r * 0.105f).coerceAtLeast(1.2f)
    strokePaint.color = aCol(235, 0, 245, 212)
    c.drawArc(outerWaveRect, 144f, 72f, false, strokePaint)
    c.drawArc(outerWaveRect, -36f, 72f, false, strokePaint)

    val innerWaveRect = RectF(cx - r * 0.72f, cy - r * 0.66f, cx + r * 0.72f, cy + r * 0.66f)
    strokePaint.strokeWidth = (r * 0.09f).coerceAtLeast(1.0f)
    strokePaint.color = aCol(215, 250, 204, 21)
    c.drawArc(innerWaveRect, 150f, 60f, false, strokePaint)
    c.drawArc(innerWaveRect, -30f, 60f, false, strokePaint)

    // 3. Upward-swept solar-gold winged tuning chevron
    val leftWing =
      Path().apply {
        moveTo(cx, cy + r * 0.72f)
        lineTo(cx - r * 0.72f, cy - r * 0.44f)
        lineTo(cx - r * 0.44f, cy - r * 0.62f)
        lineTo(cx, cy + r * 0.22f)
        close()
      }
    val rightWing =
      Path().apply {
        moveTo(cx, cy + r * 0.72f)
        lineTo(cx + r * 0.72f, cy - r * 0.44f)
        lineTo(cx + r * 0.44f, cy - r * 0.62f)
        lineTo(cx, cy + r * 0.22f)
        close()
      }
    fillPaint.shader =
      LinearGradient(
        cx - r * 0.72f,
        cy - r * 0.62f,
        cx,
        cy + r * 0.72f,
        intArrayOf(aCol(255, 254, 240, 138), aCol(255, 250, 204, 21), aCol(255, 245, 158, 11)),
        floatArrayOf(0f, 0.55f, 1f),
        Shader.TileMode.CLAMP,
      )
    c.drawPath(leftWing, fillPaint)

    fillPaint.shader =
      LinearGradient(
        cx + r * 0.72f,
        cy - r * 0.62f,
        cx,
        cy + r * 0.72f,
        intArrayOf(aCol(255, 250, 204, 21), aCol(255, 245, 158, 11), aCol(255, 180, 83, 9)),
        floatArrayOf(0f, 0.55f, 1f),
        Shader.TileMode.CLAMP,
      )
    c.drawPath(rightWing, fillPaint)
    fillPaint.shader = null

    // Crisp specular edge highlight along upper wing bevels
    strokePaint.strokeWidth = (r * 0.065f).coerceAtLeast(0.95f)
    strokePaint.color = aCol(240, 255, 255, 255)
    c.drawLine(cx - r * 0.70f, cy - r * 0.43f, cx - r * 0.44f, cy - r * 0.60f, strokePaint)
    c.drawLine(cx - r * 0.44f, cy - r * 0.60f, cx, cy + r * 0.20f, strokePaint)
    strokePaint.color = aCol(220, 0, 245, 212)
    c.drawLine(cx + r * 0.44f, cy - r * 0.60f, cx + r * 0.70f, cy - r * 0.43f, strokePaint)

    // 4. Central bright harmonic tuning sphere cradled between the wings
    val orbY = cy - r * 0.22f
    val orbR = r * 0.28f
    fillPaint.shader =
      RadialGradient(
        cx - orbR * 0.25f,
        orbY - orbR * 0.25f,
        (orbR * 1.45f).coerceAtLeast(1f),
        intArrayOf(
          aCol(255, 255, 255, 255),
          aCol(254, 254, 240, 138),
          aCol(245, 0, 245, 212),
          aCol(0, 0, 245, 212),
        ),
        floatArrayOf(0f, 0.42f, 0.78f, 1f),
        Shader.TileMode.CLAMP,
      )
    c.drawCircle(cx, orbY, orbR * 1.38f, fillPaint)
    fillPaint.shader = null
    fillPaint.color = aCol(255, 255, 255, 255)
    c.drawCircle(cx, orbY, orbR * 0.56f, fillPaint)
  }

  fun bake3dResonanceCrestIcon(sizePx: Int = 64): Bitmap {
    val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val c = Canvas(bmp)
    val cx = sizePx * 0.5f
    val cy = sizePx * 0.5f
    drawResonanceCrestIcon(c, cx, cy, sizePx * 0.43f, alpha = 255)
    return bmp
  }

  /**
   * Draws ONE single, large, jewel-grade 3D Faceted Prismatic Shard Gem filling ~86% of icon bounds
   * so it remains razor-sharp from 13dp HUD readouts up to 64px store headers:
   * - Deep `#3B0764` shadow facet, `#9333EA` mid amethyst facet, `#E879F9` lit facet,
   *   `#FAE8FF` central crown table, crisp `#00F5D4` cyan prismatic rim highlight, and `#FFFFFF` gleam.
   */
  fun drawRiftShardGemIcon(
    c: Canvas,
    cx: Float,
    cy: Float,
    r: Float,
    alpha: Int = 255,
    recipeOverride: VisualElementRecipe? = null,
  ) {
    val rec =
      recipeOverride
        ?: runCatching { RiftVisualForgeState.getRecipe(ForgeElementTarget.CURRENCY_SHARD_ICON) }
          .getOrNull()
    val aFrac = (alpha.coerceIn(0, 255)) / 255f
    if (aFrac <= 0.005f || r <= 0.5f) return

    fun argbColor(baseAlpha: Int, col: Int): Int =
      Color.argb(
        (baseAlpha * aFrac).toInt().coerceIn(0, 255),
        Color.red(col),
        Color.green(col),
        Color.blue(col),
      )

    val isDefault = rec == null || rec.isFactoryDefault
    val gemR = r * (rec?.pixelScale ?: 1f).coerceIn(0.75f, 1.35f)
    val glowMult = (rec?.glowIntensity ?: 1f).coerceIn(0f, 2f)
    val borderMult = (rec?.borderWeight ?: 1f).coerceIn(0.6f, 2.0f)

    val deepShadowCol = if (isDefault) Color.parseColor("#3B0764") else rec!!.primaryColorInt
    val midVioletCol = if (isDefault) Color.parseColor("#9333EA") else rec!!.trimColorInt
    val litMagentaCol = if (isDefault) Color.parseColor("#E879F9") else rec!!.glowColorInt
    val crownTableCol = Color.parseColor("#FAE8FF")
    val cyanRimCol = Color.parseColor("#00F5D4")

    val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    val strokePaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
      }

    // 1. Subtle prismatic refraction halo behind the single gem
    if (glowMult > 0.05f) {
      fillPaint.shader =
        RadialGradient(
          cx,
          cy,
          (gemR * 1.12f).coerceAtLeast(1f),
          intArrayOf(
            argbColor((108 * glowMult).toInt().coerceIn(0, 230), litMagentaCol),
            argbColor((52 * glowMult).toInt().coerceIn(0, 175), cyanRimCol),
            argbColor(0, deepShadowCol),
          ),
          floatArrayOf(0f, 0.64f, 1f),
          Shader.TileMode.CLAMP,
        )
      c.drawCircle(cx, cy, gemR * 1.12f, fillPaint)
      fillPaint.shader = null
    }

    // Single bold 6-vertex cleaved prismatic crystal geometry (~86% of icon bounds)
    val topX = cx
    val topY = cy - gemR * 0.98f
    val botX = cx
    val botY = cy + gemR * 0.98f
    val ulX = cx - gemR * 0.68f
    val ulY = cy - gemR * 0.28f
    val urX = cx + gemR * 0.68f
    val urY = cy - gemR * 0.28f
    val llX = cx - gemR * 0.50f
    val llY = cy + gemR * 0.42f
    val lrX = cx + gemR * 0.50f
    val lrY = cy + gemR * 0.42f

    // Inner crown table vertices
    val tTopX = cx - gemR * 0.04f
    val tTopY = cy - gemR * 0.52f
    val tLeftX = cx - gemR * 0.28f
    val tLeftY = cy - gemR * 0.14f
    val tRightX = cx + gemR * 0.24f
    val tRightY = cy - gemR * 0.12f
    val tBotX = cx + gemR * 0.02f
    val tBotY = cy + gemR * 0.36f

    val outerHull =
      Path().apply {
        moveTo(topX, topY)
        lineTo(urX, urY)
        lineTo(lrX, lrY)
        lineTo(botX, botY)
        lineTo(llX, llY)
        lineTo(ulX, ulY)
        close()
      }

    // Base shadow fill (#3B0764)
    fillPaint.color = argbColor(255, deepShadowCol)
    c.drawPath(outerHull, fillPaint)

    // Upper-left bright lit facet (#E879F9)
    val upperLeftFacet =
      Path().apply {
        moveTo(topX, topY)
        lineTo(tTopX, tTopY)
        lineTo(tLeftX, tLeftY)
        lineTo(ulX, ulY)
        close()
      }
    fillPaint.color = argbColor(255, litMagentaCol)
    c.drawPath(upperLeftFacet, fillPaint)

    // Mid-left amethyst facet (#9333EA -> #E879F9)
    val midLeftFacet =
      Path().apply {
        moveTo(ulX, ulY)
        lineTo(tLeftX, tLeftY)
        lineTo(tBotX, tBotY)
        lineTo(llX, llY)
        close()
      }
    fillPaint.shader =
      LinearGradient(
        ulX,
        ulY,
        llX,
        llY,
        intArrayOf(argbColor(255, litMagentaCol), argbColor(255, midVioletCol)),
        null,
        Shader.TileMode.CLAMP,
      )
    c.drawPath(midLeftFacet, fillPaint)
    fillPaint.shader = null

    // Lower-left keel facet (#9333EA)
    val lowerLeftFacet =
      Path().apply {
        moveTo(llX, llY)
        lineTo(tBotX, tBotY)
        lineTo(botX, botY)
        close()
      }
    fillPaint.color = argbColor(255, midVioletCol)
    c.drawPath(lowerLeftFacet, fillPaint)

    // Upper-right cyan-refracted facet (#9333EA -> #00F5D4)
    val upperRightFacet =
      Path().apply {
        moveTo(topX, topY)
        lineTo(urX, urY)
        lineTo(tRightX, tRightY)
        lineTo(tTopX, tTopY)
        close()
      }
    fillPaint.shader =
      LinearGradient(
        topX,
        topY,
        urX,
        urY,
        intArrayOf(argbColor(255, midVioletCol), argbColor(235, cyanRimCol)),
        null,
        Shader.TileMode.CLAMP,
      )
    c.drawPath(upperRightFacet, fillPaint)
    fillPaint.shader = null

    // Mid-right deep refraction facet (#9333EA -> #3B0764)
    val midRightFacet =
      Path().apply {
        moveTo(urX, urY)
        lineTo(lrX, lrY)
        lineTo(tBotX, tBotY)
        lineTo(tRightX, tRightY)
        close()
      }
    fillPaint.shader =
      LinearGradient(
        urX,
        urY,
        lrX,
        botY,
        intArrayOf(argbColor(255, midVioletCol), argbColor(255, deepShadowCol)),
        null,
        Shader.TileMode.CLAMP,
      )
    c.drawPath(midRightFacet, fillPaint)
    fillPaint.shader = null

    // Central high-luminance prismatic crown table (#FAE8FF -> #E879F9)
    val crownTable =
      Path().apply {
        moveTo(tTopX, tTopY)
        lineTo(tRightX, tRightY)
        lineTo(tBotX, tBotY)
        lineTo(tLeftX, tLeftY)
        close()
      }
    fillPaint.shader =
      LinearGradient(
        tLeftX,
        tTopY,
        tRightX,
        tBotY,
        intArrayOf(argbColor(255, Color.WHITE), argbColor(245, crownTableCol), argbColor(235, litMagentaCol)),
        floatArrayOf(0f, 0.55f, 1f),
        Shader.TileMode.CLAMP,
      )
    c.drawPath(crownTable, fillPaint)
    fillPaint.shader = null

    // Crisp internal facet ridges (#FFFFFF)
    strokePaint.strokeWidth = (gemR * 0.068f * borderMult).coerceAtLeast(1.0f)
    strokePaint.color = argbColor(235, Color.WHITE)
    c.drawPath(crownTable, strokePaint)
    c.drawLine(topX, topY, tTopX, tTopY, strokePaint)
    c.drawLine(ulX, ulY, tLeftX, tLeftY, strokePaint)
    strokePaint.color = argbColor(175, crownTableCol)
    c.drawLine(tBotX, tBotY, botX, botY, strokePaint)
    c.drawLine(urX, urY, tRightX, tRightY, strokePaint)

    // Outer crystal silhouette: #F0ABFC left bevel + #00F5D4 cyan specular right rim
    strokePaint.strokeWidth = (gemR * 0.088f * borderMult).coerceAtLeast(1.25f)
    val leftBevel =
      Path().apply {
        moveTo(botX, botY)
        lineTo(llX, llY)
        lineTo(ulX, ulY)
        lineTo(topX, topY)
      }
    strokePaint.color = argbColor(250, Color.parseColor("#F0ABFC"))
    c.drawPath(leftBevel, strokePaint)

    val rightCyanRim =
      Path().apply {
        moveTo(topX, topY)
        lineTo(urX, urY)
        lineTo(lrX, lrY)
        lineTo(botX, botY)
      }
    strokePaint.color = argbColor(255, cyanRimCol)
    c.drawPath(rightCyanRim, strokePaint)

    // Crisp white specular star-gleam on upper-left crown shoulder
    val gleamX = cx - gemR * 0.24f
    val gleamY = cy - gemR * 0.42f
    fillPaint.color = argbColor(255, Color.WHITE)
    c.drawCircle(gleamX, gleamY, (gemR * 0.095f).coerceAtLeast(1.1f), fillPaint)
  }

  /**
   * Bakes ONE single jewel-grade 3D Faceted Prismatic Shard Gem (`riftShardIconBitmap`) filling
   * ~86% of the 64x64 frame so it remains crystal-clear down to 13dp–16dp HUD & header badges.
   */
  fun bake3dRiftShardIcon(sizePx: Int = 64): Bitmap {
    val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val c = Canvas(bmp)
    val cx = sizePx * 0.5f
    val cy = sizePx * 0.5f
    drawRiftShardGemIcon(c, cx, cy, sizePx * 0.43f, alpha = 255)
    return bmp
  }

  /**
   * Returns a cached 160x160 3D Space Obstacle sprite (2 distinct variants per type) with zero flat rectangles.
   */
  fun getSpaceObstacleBitmap(type: SpaceObstacleType, variant: Int): Bitmap {
    val v = variant and 1
    val key = (type.ordinal shl 4) or v
    spaceObstacleBitmapCache[key]?.let { return it }
    val baked = bakeSpaceObstacleBitmap(type, v, 160)
    spaceObstacleBitmapCache[key] = baked
    return baked
  }

  fun bakeSpaceObstacleBitmap(type: SpaceObstacleType, variant: Int, sizePx: Int = 160): Bitmap {
    val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val c = Canvas(bmp)
    val cx = sizePx * 0.5f
    val cy = sizePx * 0.5f
    val target =
      when (type) {
        SpaceObstacleType.CRYSTAL_ASTEROID -> ForgeElementTarget.OBSTACLE_SPACE_ASTEROID
        SpaceObstacleType.ORBITAL_STATION -> ForgeElementTarget.OBSTACLE_ORBITAL_STATION
        SpaceObstacleType.ASTEROID_STATION -> ForgeElementTarget.OBSTACLE_ASTEROID_STATION
        SpaceObstacleType.FLOATING_DEBRIS -> ForgeElementTarget.OBSTACLE_FLOATING_DEBRIS
        SpaceObstacleType.PERMANENT_BARRIER -> ForgeElementTarget.PLACED_STASIS_BARRIER
      }
    val rec = runCatching { RiftVisualForgeState.getRecipe(target) }.getOrNull()
    val r = sizePx * 0.44f * (rec?.pixelScale ?: 1f).coerceIn(0.65f, 1.12f)
    val primaryCol = rec?.primaryColorInt ?: Color.parseColor("#040914")
    val trimCol = rec?.trimColorInt ?: Color.parseColor("#1E3A5F")
    val glowCol = rec?.glowColorInt ?: Color.parseColor("#00F5D4")
    val borderMult = (rec?.borderWeight ?: 1f).coerceIn(0.4f, 2.4f)
    val glowMult = (rec?.glowIntensity ?: 1f).coerceIn(0f, 2.4f)
    val rimDepth = (rec?.rimLightDepth ?: 0.75f).coerceIn(0f, 1.5f)
    val styleIdx = (rec?.geometryStyle ?: 0).coerceIn(0, 3)

    val litDeckCol =
      Color.rgb(
        (Color.red(primaryCol) + 18 * rimDepth).toInt().coerceIn(0, 255),
        (Color.green(primaryCol) + 28 * rimDepth).toInt().coerceIn(0, 255),
        (Color.blue(primaryCol) + 46 * rimDepth).toInt().coerceIn(0, 255),
      )
    val midDeckCol =
      Color.rgb(
        (Color.red(primaryCol) + 8 * rimDepth).toInt().coerceIn(0, 255),
        (Color.green(primaryCol) + 14 * rimDepth).toInt().coerceIn(0, 255),
        (Color.blue(primaryCol) + 24 * rimDepth).toInt().coerceIn(0, 255),
      )

    val obsidianBase =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = primaryCol
      }
    val bevelFill =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        shader =
          LinearGradient(
            cx - r,
            cy - r,
            cx + r,
            cy + r,
            intArrayOf(litDeckCol, midDeckCol, primaryCol),
            floatArrayOf(0f, 0.45f, 1f),
            Shader.TileMode.CLAMP,
          )
      }
    val bevelRim =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2.2f * borderMult
        color = trimCol
      }
    val conduitGlow =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4.2f * borderMult
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        color =
          Color.argb(
            (95 * glowMult).toInt().coerceIn(0, 240),
            Color.red(glowCol),
            Color.green(glowCol),
            Color.blue(glowCol),
          )
      }
    val conduitCore =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.7f * borderMult
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        color = glowCol
      }

    when (type) {
      SpaceObstacleType.CRYSTAL_ASTEROID -> {
        // Style 0=Octagon Monolith, 1=Craggy Meteorite Facets, 2=Hex Reactor Spire, 3=Diamond Bastion
        val sides =
          when (styleIdx) {
            1 -> 11
            2 -> 6
            3 -> 4
            else -> 8
          }
        val oct = Path()
        for (i in 0 until sides) {
          val a = (i * (Math.PI * 2.0 / sides) + Math.PI * 0.125).toFloat()
          val crag =
            if (styleIdx == 1) {
              if (i % 2 == 0) 0.96f else 0.78f
            } else {
              0.94f
            }
          val px = cx + kotlin.math.cos(a) * r * crag
          val py = cy + kotlin.math.sin(a) * r * crag
          if (i == 0) oct.moveTo(px, py) else oct.lineTo(px, py)
        }
        oct.close()
        c.drawPath(oct, obsidianBase)
        c.drawPath(oct, bevelRim)

        val innerOct = Path()
        for (i in 0 until sides) {
          val a = (i * (Math.PI * 2.0 / sides) + Math.PI * 0.125).toFloat()
          val crag =
            if (styleIdx == 1) {
              if (i % 2 == 0) 0.72f else 0.56f
            } else {
              0.72f
            }
          val px = cx + kotlin.math.cos(a) * r * crag
          val py = cy + kotlin.math.sin(a) * r * crag
          if (i == 0) innerOct.moveTo(px, py) else innerOct.lineTo(px, py)
        }
        innerOct.close()
        c.drawPath(innerOct, bevelFill)
        c.drawPath(innerOct, Paint(bevelRim).apply { strokeWidth = 1.4f * borderMult })

        val seamPath =
          Path().apply {
            if (variant == 0) {
              moveTo(cx - r * 0.56f, cy)
              lineTo(cx + r * 0.56f, cy)
              moveTo(cx, cy - r * 0.56f)
              lineTo(cx, cy + r * 0.56f)
            } else {
              moveTo(cx - r * 0.44f, cy - r * 0.44f)
              lineTo(cx + r * 0.44f, cy + r * 0.44f)
              moveTo(cx + r * 0.44f, cy - r * 0.44f)
              lineTo(cx - r * 0.44f, cy + r * 0.44f)
            }
          }
        c.drawPath(seamPath, conduitGlow)
        c.drawPath(seamPath, conduitCore)
        drawFluxPrismIcon(c, cx, cy, r * 0.22f, alpha = 235)
      }

      SpaceObstacleType.ORBITAL_STATION -> {
        val wingAngle = if (variant == 0) -18f else 24f
        c.save()
        c.translate(cx, cy)
        c.rotate(wingAngle + styleIdx * 15f)

        val spokeCount = if (styleIdx == 2) 6 else 4
        val spokePaint =
          Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 9f
            color = primaryCol
          }
        val spokeRim =
          Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 3.2f * borderMult
            color = litDeckCol
          }
        for (s in 0 until spokeCount) {
          val sa = s * (Math.PI.toFloat() * 2f / spokeCount) + 0.785f
          val sx = kotlin.math.cos(sa) * (r * 0.80f)
          val sy = kotlin.math.sin(sa) * (r * 0.80f)
          c.drawLine(0f, 0f, sx, sy, spokePaint)
          c.drawLine(0f, 0f, sx, sy, spokeRim)
        }

        val torusOuterR = r * 0.86f
        val torusMidR = r * 0.73f
        c.drawCircle(
          0f,
          0f,
          torusMidR,
          Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = r * 0.26f
            color = primaryCol
          },
        )
        c.drawCircle(
          0f,
          0f,
          torusMidR,
          Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = r * 0.14f
            color = midDeckCol
          },
        )
        c.drawCircle(0f, 0f, torusOuterR, bevelRim)
        c.drawCircle(0f, 0f, r * 0.60f, bevelRim)
        c.drawCircle(0f, 0f, torusMidR, conduitCore)

        val hubR = r * 0.34f
        c.drawCircle(0f, 0f, hubR * 1.18f, obsidianBase)
        c.drawCircle(0f, 0f, hubR * 1.18f, bevelRim)
        val hubGlow =
          Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            shader =
              RadialGradient(
                -hubR * 0.25f,
                -hubR * 0.25f,
                hubR * 1.2f,
                intArrayOf(
                  Color.parseColor("#E0F2FE"),
                  glowCol,
                  trimCol,
                  primaryCol,
                ),
                floatArrayOf(0f, 0.34f, 0.72f, 1f),
                Shader.TileMode.CLAMP,
              )
          }
        c.drawCircle(0f, 0f, hubR * 0.88f, hubGlow)
        c.restore()
      }

      SpaceObstacleType.ASTEROID_STATION -> {
        val hullRect = RectF(cx - r * 0.88f, cy - r * 0.78f, cx + r * 0.88f, cy + r * 0.78f)
        val ch = r * (0.18f + styleIdx * 0.05f)
        val hullPath =
          Path().apply {
            moveTo(hullRect.left + ch, hullRect.top)
            lineTo(hullRect.right - ch, hullRect.top)
            lineTo(hullRect.right, hullRect.top + ch)
            lineTo(hullRect.right, hullRect.bottom - ch)
            lineTo(hullRect.right - ch, hullRect.bottom)
            lineTo(hullRect.left + ch, hullRect.bottom)
            lineTo(hullRect.left, hullRect.bottom - ch)
            lineTo(hullRect.left, hullRect.top + ch)
            close()
          }
        c.drawPath(hullPath, obsidianBase)
        c.drawPath(hullPath, bevelFill)
        c.drawPath(hullPath, bevelRim)

        val bayRect =
          if (variant == 0) {
            RectF(cx - r * 0.18f, cy - r * 0.36f, cx + r * 0.60f, cy + r * 0.36f)
          } else {
            RectF(cx - r * 0.60f, cy - r * 0.36f, cx + r * 0.18f, cy + r * 0.36f)
          }
        c.drawRoundRect(bayRect, 6f, 6f, obsidianBase)
        c.drawRoundRect(bayRect, 6f, 6f, bevelRim)
        c.drawLine(bayRect.left + 8f, bayRect.top + 8f, bayRect.right - 8f, bayRect.top + 8f, conduitCore)
        c.drawLine(bayRect.left + 8f, bayRect.bottom - 8f, bayRect.right - 8f, bayRect.bottom - 8f, conduitCore)
        c.drawLine(
          bayRect.left + 12f,
          bayRect.centerY(),
          bayRect.right - 12f,
          bayRect.centerY(),
          Paint(conduitCore).apply { color = trimCol },
        )

        val nodeX = if (variant == 0) cx - r * 0.46f else cx + r * 0.46f
        c.drawCircle(nodeX, cy, r * 0.22f, obsidianBase)
        c.drawCircle(nodeX, cy, r * 0.22f, bevelRim)
        c.drawCircle(nodeX, cy, r * 0.09f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = glowCol })
      }

      SpaceObstacleType.FLOATING_DEBRIS -> {
        c.save()
        c.translate(cx, cy)
        c.rotate((if (variant == 0) -22f else 28f) + styleIdx * 12f)
        val segRect = RectF(-r * 0.86f, -r * 0.48f, r * 0.86f, r * 0.48f)
        c.drawRoundRect(segRect, 10f, 10f, obsidianBase)
        c.drawRoundRect(
          RectF(-r * 0.74f, -r * 0.36f, r * 0.74f, r * 0.36f),
          7f,
          7f,
          bevelFill,
        )
        c.drawRoundRect(segRect, 10f, 10f, bevelRim)
        c.drawLine(-r * 0.62f, 0f, r * 0.62f, 0f, conduitGlow)
        c.drawLine(-r * 0.62f, 0f, r * 0.62f, 0f, conduitCore)
        for (rxOff in floatArrayOf(-r * 0.44f, 0f, r * 0.44f)) {
          c.drawCircle(rxOff, 0f, 3.4f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#E0F2FE") })
        }
        c.restore()
      }

      SpaceObstacleType.PERMANENT_BARRIER -> {
        c.drawCircle(cx, cy, r * 0.76f, obsidianBase)
        c.drawCircle(cx, cy, r * 0.76f, bevelRim)
        c.drawCircle(cx, cy, r * 0.34f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = glowCol })
        c.drawCircle(cx, cy, r * 0.16f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#E0F2FE") })
      }
    }
    return bmp
  }

  fun updateCustomShip9Sprites(recipe: CustomHullVisualRecipe) {
    customShip9AlliedBitmap = bakeCustomShip9Bitmap(recipe, 128, isAllied = true)
    customShip9HostileBitmap = bakeCustomShip9Bitmap(recipe, 128, isAllied = false)
    customShip9FlashBitmap = bakeSolidWhiteFlashBitmap(customShip9AlliedBitmap)
    customShip9Version++
  }

  fun bakeCustomShip9Bitmap(
    recipe: CustomHullVisualRecipe,
    sizePx: Int = 128,
    isAllied: Boolean = true,
  ): Bitmap {
    val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val c = Canvas(bmp)
    drawCustomModularShipOnCanvas(c, sizePx / 2f, sizePx / 2f, sizePx * 0.39f, recipe, isAllied)
    return bmp
  }

  /**
   * Phase 3.1: Pre-render ambient radial glow onto an offscreen Bitmap once at startup.
   */
  fun createPreRenderedGlow(size: Int, color: Int): Bitmap {
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val c = Canvas(bmp)
    val r = size * 0.5f
    val alpha = Color.alpha(color)
    val red = Color.red(color)
    val green = Color.green(color)
    val blue = Color.blue(color)
    val paint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        shader =
          RadialGradient(
            r,
            r,
            r,
            intArrayOf(
              Color.argb(alpha, red, green, blue),
              Color.argb((alpha * 0.45f).toInt(), red, green, blue),
              Color.argb(0, red, green, blue),
            ),
            floatArrayOf(0f, 0.45f, 1f),
            Shader.TileMode.CLAMP,
          )
      }
    c.drawCircle(r, r, r, paint)
    return bmp
  }

  fun getShipSprite(
    shipId: Int,
    isAllied: Boolean = true,
    isHitFlashing: Boolean = false,
    trimStyleOverride: Int = if (isAllied) activeHullTrimStyle else 0,
  ): Bitmap {
    if (shipId == 10 || shipId > 9) {
      if (isHitFlashing) return customShip9FlashBitmap
      return if (isAllied) customShip9AlliedBitmap else customShip9HostileBitmap
    }
    if (isHitFlashing) {
      return flashShipBitmaps[shipId] ?: flashShipBitmaps[1]!!
    }
    val effectiveTrim = trimStyleOverride.coerceIn(0, 3)
    if (effectiveTrim != 0) {
      val cacheKey = (shipId shl 4) or (effectiveTrim shl 1) or (if (isAllied) 1 else 0)
      trimmedShipSpriteCache[cacheKey]?.let { return it }
      val baked = bakeShipBitmap(RiftCodex.getShipById(shipId), 128, isAllied, effectiveTrim)
      trimmedShipSpriteCache[cacheKey] = baked
      return baked
    }
    val map = if (isAllied) alliedShipBitmaps else hostileShipBitmaps
    return map[shipId] ?: map[1] ?: bakeShipBitmap(RiftCodex.getShipById(shipId), 128, isAllied)
  }

  private val customBuffIconCache = mutableMapOf<Int, Bitmap>()

  fun invalidateCustomBuffIcon(buffType: Int) {
    customBuffIconCache.remove(buffType)
  }

  fun getBuffIconBitmap(buffType: Int): Bitmap {
    if (buffType in 0..11 || buffType in -14..-10) {
      return buffIconBitmaps[buffType] ?: buffIconBitmaps[10]!!
    }
    customBuffIconCache[buffType]?.let { return it }
    val entry = RiftCodex.getBuffRegistryByIndex(buffType)
    if (entry != null) {
      val baked = bakeCustomBuffIconBitmap(entry, 68)
      customBuffIconCache[buffType] = baked
      return baked
    }
    return buffIconBitmaps[buffType.mod(12)] ?: buffIconBitmaps[0]!!
  }

  fun bakeCustomBuffIconBitmap(entry: SpecialBuffRegistryEntry, sizePx: Int = 68): Bitmap {
    val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val c = Canvas(bmp)
    val cx = sizePx * 0.5f
    val cy = sizePx * 0.5f
    val r = sizePx * 0.36f
    val accent = entry.strokeColorInt

    val glowPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        shader =
          RadialGradient(
            cx,
            cy,
            sizePx * 0.48f,
            intArrayOf(
              Color.argb(200, Color.red(accent), Color.green(accent), Color.blue(accent)),
              Color.argb(80, Color.red(accent), Color.green(accent), Color.blue(accent)),
              Color.argb(0, Color.red(accent), Color.green(accent), Color.blue(accent)),
            ),
            floatArrayOf(0f, 0.58f, 1f),
            Shader.TileMode.CLAMP,
          )
      }
    c.drawCircle(cx, cy, sizePx * 0.48f, glowPaint)

    val hexPath = Path()
    for (i in 0 until 6) {
      val a = (Math.PI / 3.0 * i - Math.PI / 6.0).toFloat()
      val hx = cx + kotlin.math.cos(a) * r
      val hy = cy + kotlin.math.sin(a) * r
      if (i == 0) hexPath.moveTo(hx, hy) else hexPath.lineTo(hx, hy)
    }
    hexPath.close()

    val platePaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.argb(235, 10, 18, 32)
      }
    c.drawPath(hexPath, platePaint)

    val borderPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = sizePx * 0.042f
        color = accent
      }
    c.drawPath(hexPath, borderPaint)

    val glyphPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = sizePx * 0.048f
        strokeCap = Paint.Cap.ROUND
        color = Color.WHITE
      }
    val corePaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = accent
      }

    when (entry.specialEffect) {
      CustomBuffEffect.PULL -> {
        c.drawCircle(cx, cy, r * 0.24f, corePaint)
        for (i in 0 until 4) {
          val a = (Math.PI / 2.0 * i + Math.PI / 4.0).toFloat()
          c.drawLine(
            cx + kotlin.math.cos(a) * r * 0.62f,
            cy + kotlin.math.sin(a) * r * 0.62f,
            cx + kotlin.math.cos(a + 0.45f) * r * 0.26f,
            cy + kotlin.math.sin(a + 0.45f) * r * 0.26f,
            glyphPaint,
          )
        }
      }
      CustomBuffEffect.PUSH -> {
        c.drawCircle(cx, cy, r * 0.52f, borderPaint)
        c.drawCircle(cx, cy, r * 0.26f, glyphPaint)
      }
      CustomBuffEffect.EMP -> {
        val bolt = Path().apply {
          moveTo(cx + r * 0.12f, cy - r * 0.55f)
          lineTo(cx - r * 0.22f, cy - r * 0.04f)
          lineTo(cx + r * 0.08f, cy + r * 0.02f)
          lineTo(cx - r * 0.14f, cy + r * 0.55f)
        }
        c.drawPath(bolt, glyphPaint)
      }
      CustomBuffEffect.BARRIER -> {
        c.drawLine(cx - r * 0.52f, cy, cx + r * 0.52f, cy, glyphPaint)
        c.drawLine(cx - r * 0.42f, cy - r * 0.24f, cx + r * 0.42f, cy - r * 0.24f, borderPaint)
        c.drawLine(cx - r * 0.42f, cy + r * 0.24f, cx + r * 0.42f, cy + r * 0.24f, borderPaint)
      }
      else -> {
        c.drawCircle(cx, cy, r * 0.34f, corePaint)
        c.drawCircle(cx, cy, r * 0.48f, glyphPaint)
      }
    }
    return bmp
  }

  /**
   * Pre-bakes high-definition item-specific graphical buff icons with glowing halos (0..10):
   * Bucket A (Special Buff Slot Pickups — 0❖ Cost):
   * 0 = Burst (Orange 8-Point Shrapnel Starburst)
   * 1 = Shotgun (Coral-Rose 60° Cone 3-Wave Pellet Blast)
   * 2 = Lock-On Bouncing Missile (Crimson Homing Missile & Target Reticle)
   * 3 = Decoy (Teal Twin Hologram Clone Ships)
   * 4 = Brick (Cyan Solid Energy Barrier Wall)
   * 5 = Rocket (Magenta-Red Hyper-Thruster Boost Plume)
   * Bucket B (Instant Ship & Weapon Enhancers):
   * 6 = Full Flux Charge (3D Swirling Plasma Toroid)
   * 7 = Repel Charge (Electric Cyan Concentric Shockwave Rings)
   * 8 = Shot-Limited Multi-Shot +25 Shots (Gold Triple-Spread Trident Beams)
   * 9 = Bouncing Bullets +30 Shots (Electric Violet Ricochet Bank-Shot)
   * 10 = Overcharge Surge +8% x3 (Solar Amber Triple Upward Overdrive Chevrons)
   */
  fun bakeBuffIconBitmap(buffType: Int, sizePx: Int = 68): Bitmap {
    val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val c = Canvas(bmp)
    val cx = sizePx * 0.5f
    val cy = sizePx * 0.5f
    val r = sizePx * 0.36f

    val accentColor =
      when (buffType) {
        0 -> Color.parseColor("#F97316") // Burst
        1 -> Color.parseColor("#FB7185") // Shotgun
        2 -> Color.parseColor("#EF4444") // Missile
        3 -> Color.parseColor("#2DD4BF") // Decoy
        4 -> Color.parseColor("#38BDF8") // Brick
        5 -> Color.parseColor("#F43F5E") // Rocket
        6 -> Color.parseColor("#10B981") // Full Flux Charge
        7 -> Color.parseColor("#00F5D4") // Repel Charge
        8 -> Color.parseColor("#FACC15") // Shot-Limited Multi-Shot (+25)
        9 -> Color.parseColor("#A855F7") // Bouncing Bullets (Permanent Life Upgrade)
        11 -> Color.parseColor("#00F5D4") // 5-Drone SWARM
        -10 -> Color.parseColor("#38BDF8") // Speed Overcharge (+8% x3)
        -11 -> Color.parseColor("#F59E0B") // Thrust Overcharge (+8% x3)
        -12 -> Color.parseColor("#10B981") // Reactor Overcharge (+8% x3)
        -13 -> Color.parseColor("#EC4899") // Turn Radius Overcharge (+12% x3)
        -14 -> Color.parseColor("#00F5D4") // Max Flux Overcharge (+10% x3)
        else -> Color.parseColor("#38BDF8") // Overcharge Surge (+8% x3)
      }

    // 1. Pre-rendered outer radial halo glow
    val glowPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        shader =
          RadialGradient(
            cx,
            cy,
            sizePx * 0.48f,
            intArrayOf(
              Color.argb(190, Color.red(accentColor), Color.green(accentColor), Color.blue(accentColor)),
              Color.argb(75, Color.red(accentColor), Color.green(accentColor), Color.blue(accentColor)),
              Color.argb(0, Color.red(accentColor), Color.green(accentColor), Color.blue(accentColor)),
            ),
            floatArrayOf(0f, 0.58f, 1f),
            Shader.TileMode.CLAMP,
          )
      }
    c.drawCircle(cx, cy, sizePx * 0.48f, glowPaint)

    // 2. Dark octagonal/hexagonal metallic housing plate with glowing bevel
    val hexPath = Path()
    for (i in 0 until 6) {
      val a = (Math.PI / 3.0 * i - Math.PI / 6.0).toFloat()
      val hx = cx + kotlin.math.cos(a) * r
      val hy = cy + kotlin.math.sin(a) * r
      if (i == 0) hexPath.moveTo(hx, hy) else hexPath.lineTo(hx, hy)
    }
    hexPath.close()

    val platePaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.argb(230, 8, 15, 28)
      }
    val rimPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2.6f
        color = accentColor
      }
    val innerRimPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.1f
        color = Color.argb(175, 255, 255, 255)
      }
    c.drawPath(hexPath, platePaint)
    c.drawPath(hexPath, rimPaint)

    val glyphFillPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.WHITE
      }
    val glyphStrokePaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3.0f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        color = accentColor
      }
    val glyphCoreStroke =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.6f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        color = Color.WHITE
      }

    // 3. Item-specific high-contrast vector icon inside housing
    when (buffType) {
      0 -> {
        // BURST: 8-point radial shrapnel starburst
        for (i in 0 until 4) {
          val a = (Math.PI / 4.0 * i).toFloat()
          val dx = kotlin.math.cos(a) * r * 0.52f
          val dy = kotlin.math.sin(a) * r * 0.52f
          c.drawLine(cx - dx, cy - dy, cx + dx, cy + dy, glyphStrokePaint)
          c.drawLine(cx - dx * 0.7f, cy - dy * 0.7f, cx + dx * 0.7f, cy + dy * 0.7f, glyphCoreStroke)
        }
        c.drawCircle(cx, cy, r * 0.22f, glyphFillPaint)
      }
      1 -> {
        // SHOTGUN: 60-degree cone blast with 3 concentric pellet wave arcs
        c.drawLine(cx, cy + r * 0.45f, cx - r * 0.46f, cy - r * 0.38f, glyphStrokePaint)
        c.drawLine(cx, cy + r * 0.45f, cx + r * 0.46f, cy - r * 0.38f, glyphStrokePaint)
        for (wave in 1..3) {
          val waveY = cy + r * 0.32f - wave * (r * 0.22f)
          val spreadX = wave * (r * 0.13f)
          c.drawCircle(cx - spreadX, waveY, 2.2f, glyphFillPaint)
          c.drawCircle(cx, waveY - 1.5f, 2.2f, glyphFillPaint)
          c.drawCircle(cx + spreadX, waveY, 2.2f, glyphFillPaint)
        }
      }
      2 -> {
        // LOCK-ON BOUNCING MISSILE: Homing warhead inside target lock reticle
        c.drawCircle(cx, cy, r * 0.48f, innerRimPaint)
        c.drawLine(cx - r * 0.58f, cy, cx - r * 0.32f, cy, glyphStrokePaint)
        c.drawLine(cx + r * 0.32f, cy, cx + r * 0.58f, cy, glyphStrokePaint)
        c.drawLine(cx, cy - r * 0.58f, cx, cy - r * 0.32f, glyphStrokePaint)
        c.drawLine(cx, cy + r * 0.32f, cx, cy + r * 0.58f, glyphStrokePaint)
        val missilePath =
          Path().apply {
            moveTo(cx, cy - r * 0.34f)
            lineTo(cx + r * 0.20f, cy + r * 0.26f)
            lineTo(cx, cy + r * 0.14f)
            lineTo(cx - r * 0.20f, cy + r * 0.26f)
            close()
          }
        c.drawPath(missilePath, glyphFillPaint)
      }
      3 -> {
        // DECOY: Twin mirrored hologram ship chevrons
        for (offsetX in listOf(-r * 0.24f, r * 0.24f)) {
          val holo =
            Path().apply {
              moveTo(cx + offsetX, cy - r * 0.42f)
              lineTo(cx + offsetX + r * 0.20f, cy + r * 0.34f)
              lineTo(cx + offsetX, cy + r * 0.18f)
              lineTo(cx + offsetX - r * 0.20f, cy + r * 0.34f)
              close()
            }
          c.drawPath(holo, glyphStrokePaint)
          c.drawPath(holo, glyphCoreStroke)
        }
      }
      4 -> {
        // BRICK: Solid interlocked energy barrier wall
        c.drawRect(cx - r * 0.52f, cy - r * 0.22f, cx + r * 0.52f, cy + r * 0.22f, glyphStrokePaint)
        c.drawRect(cx - r * 0.46f, cy - r * 0.16f, cx + r * 0.46f, cy + r * 0.16f, glyphFillPaint)
        c.drawLine(cx, cy - r * 0.22f, cx, cy + r * 0.22f, rimPaint)
      }
      5 -> {
        // ROCKET: Hyper-thruster boost fuselage with twin exhaust flames
        val rocket =
          Path().apply {
            moveTo(cx, cy - r * 0.52f)
            lineTo(cx + r * 0.26f, cy + r * 0.12f)
            lineTo(cx - r * 0.26f, cy + r * 0.12f)
            close()
          }
        c.drawPath(rocket, glyphStrokePaint)
        c.drawPath(rocket, glyphFillPaint)
        c.drawLine(cx - r * 0.14f, cy + r * 0.18f, cx - r * 0.22f, cy + r * 0.52f, glyphStrokePaint)
        c.drawLine(cx, cy + r * 0.18f, cx, cy + r * 0.56f, glyphCoreStroke)
        c.drawLine(cx + r * 0.14f, cy + r * 0.18f, cx + r * 0.22f, cy + r * 0.52f, glyphStrokePaint)
      }
      6 -> {
        // FULL FLUX CHARGE: 3D Swirling Plasma Toroid
        c.drawCircle(cx, cy, r * 0.68f, innerRimPaint)
        drawFluxPrismIcon(c, cx, cy, r * 0.62f, alpha = 255)
      }
      7 -> {
        // REPEL CHARGE: Dual concentric repulsion wave rings + outward arrows
        c.drawCircle(cx, cy, r * 0.50f, glyphStrokePaint)
        c.drawCircle(cx, cy, r * 0.26f, glyphCoreStroke)
        c.drawLine(cx - r * 0.56f, cy, cx + r * 0.56f, cy, innerRimPaint)
        c.drawLine(cx, cy - r * 0.56f, cx, cy + r * 0.56f, innerRimPaint)
      }
      8 -> {
        // SHOT-LIMITED MULTI-SHOT (+25 SHOTS): Triple-spread diverging laser trident
        c.drawLine(cx, cy + r * 0.46f, cx, cy - r * 0.50f, glyphStrokePaint)
        c.drawLine(cx, cy + r * 0.46f, cx - r * 0.46f, cy - r * 0.36f, glyphStrokePaint)
        c.drawLine(cx, cy + r * 0.46f, cx + r * 0.46f, cy - r * 0.36f, glyphStrokePaint)
        c.drawLine(cx, cy + r * 0.46f, cx, cy - r * 0.50f, glyphCoreStroke)
        c.drawLine(cx, cy + r * 0.46f, cx - r * 0.46f, cy - r * 0.36f, glyphCoreStroke)
        c.drawLine(cx, cy + r * 0.46f, cx + r * 0.46f, cy - r * 0.36f, glyphCoreStroke)
      }
      9 -> {
        // BOUNCING BULLETS (+30 SHOTS): Ricochet V-bank trajectory with wall node
        val ricochet =
          Path().apply {
            moveTo(cx - r * 0.46f, cy + r * 0.36f)
            lineTo(cx - r * 0.05f, cy - r * 0.44f)
            lineTo(cx + r * 0.46f, cy + r * 0.24f)
          }
        c.drawPath(ricochet, glyphStrokePaint)
        c.drawPath(ricochet, glyphCoreStroke)
        c.drawCircle(cx - r * 0.05f, cy - r * 0.44f, 3.5f, glyphFillPaint)
      }
      11 -> {
        // SWARM (5-DRONE HUNTER FORMATION): 5 crisp V-wing micro-drones + weaving exhaust trails
        val droneOffsets =
          listOf(
            0f to -r * 0.44f,
            -r * 0.34f to -r * 0.14f,
            r * 0.34f to -r * 0.14f,
            -r * 0.22f to r * 0.28f,
            r * 0.22f to r * 0.28f,
          )
        val trailPaint =
          Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
            strokeCap = Paint.Cap.ROUND
            color = Color.argb(170, 56, 189, 248)
          }
        for ((dx, dy) in droneOffsets) {
          val ux = cx + dx
          val uy = cy + dy
          c.drawLine(ux, uy + r * 0.08f, ux, uy + r * 0.24f, trailPaint)
          val dronePath =
            Path().apply {
              moveTo(ux, uy - r * 0.15f)
              lineTo(ux + r * 0.13f, uy + r * 0.11f)
              lineTo(ux, uy + r * 0.04f)
              lineTo(ux - r * 0.13f, uy + r * 0.11f)
              close()
            }
          c.drawPath(dronePath, glyphStrokePaint)
          c.drawPath(dronePath, glyphFillPaint)
        }
      }
      -11 -> {
        // THRUST OVERCHARGE (+8% x3): Twin vector thruster nozzles with high-impulse flare
        val nozzle =
          Path().apply {
            moveTo(cx, cy - r * 0.48f)
            lineTo(cx + r * 0.34f, cy + r * 0.14f)
            lineTo(cx + r * 0.12f, cy + r * 0.14f)
            lineTo(cx + r * 0.18f, cy + r * 0.48f)
            lineTo(cx, cy + r * 0.28f)
            lineTo(cx - r * 0.18f, cy + r * 0.48f)
            lineTo(cx - r * 0.12f, cy + r * 0.14f)
            lineTo(cx - r * 0.34f, cy + r * 0.14f)
            close()
          }
        c.drawPath(nozzle, glyphStrokePaint)
        c.drawPath(nozzle, glyphCoreStroke)
      }
      -12 -> {
        // REACTOR OVERCHARGE (+8% x3): Sleek circular dual-arc turbine ring + central 3D Plasma Toroid
        val turbineR = r * 0.58f
        val turbineRect = RectF(cx - turbineR, cy - turbineR, cx + turbineR, cy + turbineR)
        c.drawArc(turbineRect, -155f, 130f, false, glyphStrokePaint)
        c.drawArc(turbineRect, 25f, 130f, false, glyphStrokePaint)
        c.drawArc(turbineRect, -145f, 110f, false, glyphCoreStroke)
        c.drawArc(turbineRect, 35f, 110f, false, glyphCoreStroke)
        drawFluxPrismIcon(c, cx, cy, r * 0.44f, alpha = 255)
      }
      -13 -> {
        // TURN RADIUS OVERCHARGE (+12% x3): Twin rotational thruster arrows (#EC4899 / #38BDF8) around a ship chevron
        val arcRect = RectF(cx - r * 0.56f, cy - r * 0.56f, cx + r * 0.56f, cy + r * 0.56f)
        val pinkArcPaint =
          Paint(glyphStrokePaint).apply {
            color = Color.parseColor("#EC4899")
          }
        val cyanArcPaint =
          Paint(glyphStrokePaint).apply {
            color = Color.parseColor("#38BDF8")
          }
        c.drawArc(arcRect, -150f, 120f, false, pinkArcPaint)
        c.drawArc(arcRect, 30f, 120f, false, cyanArcPaint)

        val chev =
          Path().apply {
            moveTo(cx, cy - r * 0.34f)
            lineTo(cx + r * 0.26f, cy + r * 0.28f)
            lineTo(cx, cy + r * 0.12f)
            lineTo(cx - r * 0.26f, cy + r * 0.28f)
            close()
          }
        c.drawPath(chev, glyphFillPaint)
      }
      -14 -> {
        // MAX FLUX OVERCHARGE (+10% x3): Circular tokamak containment ring + central 3D Plasma Toroid
        c.drawCircle(cx, cy, r * 0.56f, glyphStrokePaint)
        c.drawCircle(cx, cy, r * 0.56f, innerRimPaint)
        drawFluxPrismIcon(c, cx, cy, r * 0.44f, alpha = 255)
      }
      else -> {
        // SPEED OVERCHARGE (-10 / 10): Double forward supersonic overdrive chevrons
        for (offset in listOf(-r * 0.22f, r * 0.14f)) {
          val chev =
            Path().apply {
              moveTo(cx + offset - r * 0.22f, cy - r * 0.42f)
              lineTo(cx + offset + r * 0.24f, cy)
              lineTo(cx + offset - r * 0.22f, cy + r * 0.42f)
            }
          c.drawPath(chev, glyphStrokePaint)
          c.drawPath(chev, glyphCoreStroke)
        }
      }
    }

    return bmp
  }

  private fun bakeSolidWhiteFlashBitmap(src: Bitmap): Bitmap {
    val bmp = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
    val c = Canvas(bmp)
    val whitePaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        colorFilter = PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN)
      }
    c.drawBitmap(src, 0f, 0f, whitePaint)
    return bmp
  }

  /**
   * Bakes a seamless 64x64 Pitch-Black Obsidian Bulkhead Tile (`#040914`) with `#081224` 3D bevels
   * and `#00F5D4` / `#38BDF8` energy conduit seams (zero yellow hazard stripes).
   */
  private fun bakeWallTile(): Bitmap {
    val size = 64
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val c = Canvas(bmp)

    c.drawRect(
      0f,
      0f,
      size.toFloat(),
      size.toFloat(),
      Paint().apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#040914")
      },
    )

    val seamGlowPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2.8f
        color = Color.argb(95, 0, 245, 212)
      }
    val seamCorePaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.2f
        color = Color.parseColor("#00F5D4")
      }
    c.drawLine(0f, 32f, 64f, 32f, seamGlowPaint)
    c.drawLine(0f, 0f, 64f, 0f, seamGlowPaint)
    c.drawLine(32f, 0f, 32f, 32f, seamGlowPaint)
    c.drawLine(16f, 32f, 16f, 64f, seamGlowPaint)
    c.drawLine(48f, 32f, 48f, 64f, seamGlowPaint)

    c.drawLine(0f, 32f, 64f, 32f, seamCorePaint)
    c.drawLine(0f, 0.6f, 64f, 0.6f, seamCorePaint)
    c.drawLine(32f, 0f, 32f, 32f, seamCorePaint)
    c.drawLine(16f, 32f, 16f, 64f, seamCorePaint)
    c.drawLine(48f, 32f, 48f, 64f, seamCorePaint)

    val plateFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    val topBevelPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.3f
        color = Color.parseColor("#1E3A5F")
      }
    val bottomShadePaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.3f
        color = Color.parseColor("#02050C")
      }
    val cyanNodePaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#38BDF8")
      }

    fun drawObsidianPlate(left: Float, top: Float, right: Float, bottom: Float, hasCenterNode: Boolean) {
      val insetL = left + 1.8f
      val insetT = top + 1.8f
      val insetR = right - 1.8f
      val insetB = bottom - 1.8f
      val ch = 3.2f
      val platePath =
        Path().apply {
          moveTo(insetL + ch, insetT)
          lineTo(insetR - ch, insetT)
          lineTo(insetR, insetT + ch)
          lineTo(insetR, insetB - ch)
          lineTo(insetR - ch, insetB)
          lineTo(insetL + ch, insetB)
          lineTo(insetL, insetB - ch)
          lineTo(insetL, insetT + ch)
          close()
        }
      plateFillPaint.shader =
        LinearGradient(
          insetL,
          insetT,
          insetR,
          insetB,
          intArrayOf(
            Color.parseColor("#0C182D"),
            Color.parseColor("#081224"),
            Color.parseColor("#040914"),
          ),
          floatArrayOf(0f, 0.5f, 1f),
          Shader.TileMode.CLAMP,
        )
      c.drawPath(platePath, plateFillPaint)
      c.drawLine(insetL + ch, insetT, insetR - ch, insetT, topBevelPaint)
      c.drawLine(insetL, insetT + ch, insetL, insetB - ch, topBevelPaint)
      c.drawLine(insetL + ch, insetB, insetR - ch, insetB, bottomShadePaint)
      c.drawLine(insetR, insetT + ch, insetR, insetB - ch, bottomShadePaint)

      if (hasCenterNode) {
        val midX = (insetL + insetR) * 0.5f
        val midY = (insetT + insetB) * 0.5f
        c.drawCircle(midX, midY, 1.8f, cyanNodePaint)
      }
    }

    drawObsidianPlate(0f, 0f, 32f, 32f, hasCenterNode = true)
    drawObsidianPlate(32f, 0f, 64f, 32f, hasCenterNode = false)
    drawObsidianPlate(16f, 32f, 48f, 64f, hasCenterNode = true)
    drawObsidianPlate(-16f, 32f, 16f, 64f, hasCenterNode = false)
    drawObsidianPlate(48f, 32f, 80f, 64f, hasCenterNode = false)

    return bmp
  }

  private fun bakeHazardBorderTile(): Bitmap {
    val bmp = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888)
    val c = Canvas(bmp)
    c.drawRect(
      0f,
      0f,
      32f,
      32f,
      Paint().apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#040914")
      },
    )
    val conduitTrim =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.6f
        color = Color.argb(170, 0, 245, 212)
      }
    c.drawLine(0f, 8f, 32f, 8f, conduitTrim)
    c.drawLine(0f, 24f, 32f, 24f, conduitTrim)
    return bmp
  }

  /**
   * Shared 4-layer 3D swirling Playfield Rift renderer used by BOTH the live arena (`RiftArenaView`)
   * and Codex / icon bakers (`bakeRiftAnomalyIconBitmap`) for 1:1 visual parity.
   */
  fun bakeSyndicateCrestBitmap(
    recipe: SyndicateCrestRecipe,
    tagText: String = "",
    sizePx: Int = 160,
  ): Bitmap {
    val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val c = Canvas(bmp)
    drawSyndicateCrestOnCanvas(c, sizePx * 0.5f, sizePx * 0.5f, sizePx * 0.42f, recipe, tagText)
    return bmp
  }

  fun drawSyndicateCrestOnCanvas(
    c: Canvas,
    cx: Float,
    cy: Float,
    r: Float,
    recipe: SyndicateCrestRecipe,
    tagText: String = "",
  ) {
    val primaryCol = recipe.primaryColorInt
    val trimCol = recipe.trimColorInt
    val glowCol = recipe.glowColorInt

    // Outer volumetric radial aura
    val haloPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        shader =
          RadialGradient(
            cx,
            cy,
            r * 1.18f,
            intArrayOf(
              Color.argb(145, Color.red(glowCol), Color.green(glowCol), Color.blue(glowCol)),
              Color.argb(55, Color.red(trimCol), Color.green(trimCol), Color.blue(trimCol)),
              Color.argb(0, Color.red(glowCol), Color.green(glowCol), Color.blue(glowCol)),
            ),
            floatArrayOf(0f, 0.58f, 1f),
            Shader.TileMode.CLAMP,
          )
      }
    c.drawCircle(cx, cy, r * 1.18f, haloPaint)

    fun buildShieldPath(rad: Float): Path {
      val p = Path()
      when (recipe.shieldShape.coerceIn(0, 3)) {
        1 -> { // Chevron Kite Shield
          p.moveTo(cx, cy - rad)
          p.lineTo(cx + rad * 0.86f, cy - rad * 0.46f)
          p.lineTo(cx + rad * 0.72f, cy + rad * 0.36f)
          p.lineTo(cx, cy + rad)
          p.lineTo(cx - rad * 0.72f, cy + rad * 0.36f)
          p.lineTo(cx - rad * 0.86f, cy - rad * 0.46f)
          p.close()
        }
        2 -> { // Octagon Bastion
          for (i in 0 until 8) {
            val a = (i * Math.PI * 0.25 + Math.PI * 0.125).toFloat()
            val px = cx + kotlin.math.cos(a) * rad
            val py = cy + kotlin.math.sin(a) * rad
            if (i == 0) p.moveTo(px, py) else p.lineTo(px, py)
          }
          p.close()
        }
        3 -> { // Diamond Sovereign
          p.moveTo(cx, cy - rad * 1.04f)
          p.lineTo(cx + rad * 0.84f, cy)
          p.lineTo(cx, cy + rad * 1.04f)
          p.lineTo(cx - rad * 0.84f, cy)
          p.close()
        }
        else -> { // Hexagon Aegis
          for (i in 0 until 6) {
            val a = (i * Math.PI / 3.0 - Math.PI * 0.5).toFloat()
            val px = cx + kotlin.math.cos(a) * rad
            val py = cy + kotlin.math.sin(a) * rad
            if (i == 0) p.moveTo(px, py) else p.lineTo(px, py)
          }
          p.close()
        }
      }
      return p
    }

    val outerShield = buildShieldPath(r)
    val innerShield = buildShieldPath(r * 0.82f)

    val platePaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        shader =
          LinearGradient(
            cx - r,
            cy - r,
            cx + r,
            cy + r,
            intArrayOf(
              Color.rgb(
                (Color.red(primaryCol) + 28).coerceAtMost(255),
                (Color.green(primaryCol) + 38).coerceAtMost(255),
                (Color.blue(primaryCol) + 56).coerceAtMost(255),
              ),
              primaryCol,
              Color.parseColor("#030712"),
            ),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP,
          )
      }
    c.drawPath(outerShield, platePaint)

    val rimPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = (r * 0.085f).coerceAtLeast(2.2f)
        strokeJoin = Paint.Join.ROUND
        color = trimCol
      }
    c.drawPath(outerShield, rimPaint)

    val innerRimPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = (r * 0.038f).coerceAtLeast(1.2f)
        strokeJoin = Paint.Join.ROUND
        color = glowCol
      }
    c.drawPath(innerShield, innerRimPaint)

    // Central Emblem Glyph (0..5)
    val glyphStroke =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = (r * 0.075f).coerceAtLeast(2.0f)
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        color = Color.WHITE
      }
    val glyphFill =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = trimCol
      }
    val glyphGlowFill =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = glowCol
      }
    val gy = if (tagText.isNotBlank()) cy - r * 0.10f else cy
    val gr = r * 0.44f

    when (recipe.emblemGlyph.coerceIn(0, 5)) {
      1 -> { // Twin Blades
        c.drawLine(cx - gr * 0.78f, gy + gr * 0.72f, cx + gr * 0.78f, gy - gr * 0.72f, glyphStroke)
        c.drawLine(cx + gr * 0.78f, gy + gr * 0.72f, cx - gr * 0.78f, gy - gr * 0.72f, glyphStroke)
        c.drawCircle(cx, gy, gr * 0.22f, glyphGlowFill)
      }
      2 -> { // Crown Spire
        val crown =
          Path().apply {
            moveTo(cx - gr * 0.80f, gy + gr * 0.46f)
            lineTo(cx - gr * 0.65f, gy - gr * 0.35f)
            lineTo(cx - gr * 0.25f, gy + gr * 0.06f)
            lineTo(cx, gy - gr * 0.72f)
            lineTo(cx + gr * 0.25f, gy + gr * 0.06f)
            lineTo(cx + gr * 0.65f, gy - gr * 0.35f)
            lineTo(cx + gr * 0.80f, gy + gr * 0.46f)
            close()
          }
        c.drawPath(crown, glyphFill)
        c.drawPath(crown, glyphStroke)
      }
      3 -> { // Vortex Eye
        c.drawCircle(cx, gy, gr * 0.74f, Paint(glyphStroke).apply { color = glowCol })
        c.drawCircle(cx, gy, gr * 0.42f, glyphStroke)
        c.drawCircle(cx, gy, gr * 0.20f, glyphFill)
      }
      4 -> { // Winged Falcon
        val wing =
          Path().apply {
            moveTo(cx, gy + gr * 0.56f)
            lineTo(cx - gr * 0.92f, gy - gr * 0.42f)
            lineTo(cx - gr * 0.30f, gy - gr * 0.12f)
            lineTo(cx, gy - gr * 0.68f)
            lineTo(cx + gr * 0.30f, gy - gr * 0.12f)
            lineTo(cx + gr * 0.92f, gy - gr * 0.42f)
            close()
          }
        c.drawPath(wing, glyphFill)
        c.drawPath(wing, glyphStroke)
      }
      5 -> { // Tactician Visor
        val visor =
          Path().apply {
            moveTo(cx - gr * 0.78f, gy - gr * 0.16f)
            lineTo(cx, gy - gr * 0.48f)
            lineTo(cx + gr * 0.78f, gy - gr * 0.16f)
            lineTo(cx + gr * 0.52f, gy + gr * 0.48f)
            lineTo(cx, gy + gr * 0.24f)
            lineTo(cx - gr * 0.52f, gy + gr * 0.48f)
            close()
          }
        c.drawPath(visor, glyphGlowFill)
        c.drawPath(visor, glyphStroke)
      }
      else -> { // Rift Star
        val star = Path()
        for (i in 0 until 8) {
          val a = (i * Math.PI * 0.25 - Math.PI * 0.5).toFloat()
          val sr = if (i % 2 == 0) gr * 0.88f else gr * 0.34f
          val sx = cx + kotlin.math.cos(a) * sr
          val sy = gy + kotlin.math.sin(a) * sr
          if (i == 0) star.moveTo(sx, sy) else star.lineTo(sx, sy)
        }
        star.close()
        c.drawPath(star, glyphFill)
        c.drawPath(star, glyphStroke)
      }
    }

    if (tagText.isNotBlank()) {
      val clean = tagText.trim().take(5)
      val textPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
          textAlign = Paint.Align.CENTER
          isFakeBoldText = true
          textSize = (r * 0.28f).coerceAtLeast(10f)
          color = Color.WHITE
        }
      c.drawText(clean, cx, cy + r * 0.58f, textPaint)
    }
  }

  fun draw3dPlayfieldRift(
    canvas: Canvas,
    type: AnomalyType,
    cx: Float,
    cy: Float,
    radius: Float,
    spinAngle: Float,
    absorbedCount: Int = 0,
    pulsePhaseOverride: Float? = null,
    fadeAlpha: Float = 1.0f,
    recipeOverride: VisualElementRecipe? = null,
  ) {
    val target =
      when (type) {
        AnomalyType.PLASMA_RIFT -> ForgeElementTarget.RIFT_PLASMA
        AnomalyType.REPULSOR_RIFT -> ForgeElementTarget.RIFT_REPULSOR
        AnomalyType.GRAVITY_RIFT -> ForgeElementTarget.RIFT_GRAVITY
        AnomalyType.PHASE_RIFT_A,
        AnomalyType.PHASE_RIFT_B -> ForgeElementTarget.RIFT_PHASE_PAIR
      }
    val rec = recipeOverride ?: runCatching { RiftVisualForgeState.getRecipe(target) }.getOrNull()
    val useCustomColors = rec != null && !rec.isFactoryDefault
    val rScaled = radius * (rec?.pixelScale ?: 1f).coerceIn(0.65f, 1.55f)
    val effFade = (fadeAlpha * (rec?.surfaceOpacity ?: 1f).coerceIn(0.25f, 1f)).coerceIn(0.1f, 1f)

    when (type) {
      AnomalyType.PLASMA_RIFT -> {
        draw3dPlayfieldRift(
          canvas = canvas,
          cx = cx,
          cy = cy,
          r = rScaled,
          spinAngle = spinAngle,
          fadeAlpha = effFade,
          outerColor = if (useCustomColors) rec!!.trimColorInt else Color.rgb(239, 68, 68),
          midColor = if (useCustomColors) rec!!.glowColorInt else Color.rgb(217, 70, 239),
          coreColor = Color.rgb(255, 228, 230),
          darkVoidCenter = (rec?.geometryStyle == 3),
          inwardSpiral = false,
          pulsePhaseOverride = pulsePhaseOverride,
        )
      }
      AnomalyType.REPULSOR_RIFT -> {
        draw3dPlayfieldRift(
          canvas = canvas,
          cx = cx,
          cy = cy,
          r = rScaled,
          spinAngle = -spinAngle * 1.15f,
          fadeAlpha = effFade,
          outerColor = if (useCustomColors) rec!!.trimColorInt else Color.rgb(56, 189, 248),
          midColor = if (useCustomColors) rec!!.glowColorInt else Color.rgb(186, 230, 253),
          coreColor = Color.rgb(255, 255, 255),
          darkVoidCenter = (rec?.geometryStyle == 3),
          inwardSpiral = false,
          pulsePhaseOverride = pulsePhaseOverride,
        )
      }
      AnomalyType.GRAVITY_RIFT -> {
        draw3dPlayfieldRift(
          canvas = canvas,
          cx = cx,
          cy = cy,
          r = rScaled,
          spinAngle = spinAngle * 1.35f,
          fadeAlpha = effFade,
          outerColor = if (useCustomColors) rec!!.trimColorInt else Color.rgb(168, 85, 247),
          midColor = if (useCustomColors) rec!!.glowColorInt else Color.rgb(99, 102, 241),
          coreColor = Color.rgb(233, 213, 255),
          darkVoidCenter = true,
          inwardSpiral = true,
          pulsePhaseOverride = pulsePhaseOverride,
        )
        val arcCount = kotlin.math.min(8, absorbedCount.coerceAtLeast(0))
        if (arcCount > 0 && rScaled > 8f) {
          val arcPaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
              style = Paint.Style.STROKE
              strokeCap = Paint.Cap.ROUND
            }
          val outerArcCol = if (useCustomColors) rec!!.trimColorInt else Color.rgb(192, 132, 252)
          for (idx in 0 until arcCount) {
            val baseAng =
              -spinAngle * 1.85f + idx * (Math.PI.toFloat() * 2f / arcCount.toFloat())
            val rOuterRim = rScaled * 1.04f
            val rMidA = rScaled * 0.80f
            val rMidB = rScaled * 0.58f
            val rInnerThroat = rScaled * 0.38f
            val jagA = 0.14f * kotlin.math.sin(spinAngle * 6.2f + idx * 2.1f)
            val jagB = -0.16f * kotlin.math.cos(spinAngle * 5.4f - idx * 1.7f)
            val x0 = cx + kotlin.math.cos(baseAng) * rOuterRim
            val y0 = cy + kotlin.math.sin(baseAng) * rOuterRim
            val x1 = cx + kotlin.math.cos(baseAng + jagA) * rMidA
            val y1 = cy + kotlin.math.sin(baseAng + jagA) * rMidA
            val x2 = cx + kotlin.math.cos(baseAng + jagB) * rMidB
            val y2 = cy + kotlin.math.sin(baseAng + jagB) * rMidB
            val x3 = cx + kotlin.math.cos(baseAng + jagA * 0.5f) * rInnerThroat
            val y3 = cy + kotlin.math.sin(baseAng + jagA * 0.5f) * rInnerThroat

            arcPaint.strokeWidth = (rScaled * 0.042f).coerceAtLeast(2.4f)
            arcPaint.color =
              Color.argb(
                (175 * effFade).toInt().coerceIn(0, 255),
                Color.red(outerArcCol),
                Color.green(outerArcCol),
                Color.blue(outerArcCol),
              )
            canvas.drawLine(x0, y0, x1, y1, arcPaint)
            canvas.drawLine(x1, y1, x2, y2, arcPaint)
            canvas.drawLine(x2, y2, x3, y3, arcPaint)

            arcPaint.strokeWidth = (rScaled * 0.018f).coerceAtLeast(1.2f)
            arcPaint.color = Color.argb((245 * effFade).toInt().coerceIn(0, 255), 250, 245, 255)
            canvas.drawLine(x0, y0, x1, y1, arcPaint)
            canvas.drawLine(x1, y1, x2, y2, arcPaint)
            canvas.drawLine(x2, y2, x3, y3, arcPaint)
          }
        }
      }
      AnomalyType.PHASE_RIFT_A -> {
        draw3dPlayfieldRift(
          canvas = canvas,
          cx = cx,
          cy = cy,
          r = rScaled,
          spinAngle = spinAngle,
          fadeAlpha = effFade,
          outerColor = if (useCustomColors) rec!!.trimColorInt else Color.rgb(45, 212, 191),
          midColor = if (useCustomColors) rec!!.glowColorInt else Color.rgb(56, 189, 248),
          coreColor = Color.rgb(240, 253, 250),
          darkVoidCenter = true,
          inwardSpiral = false,
          pulsePhaseOverride = pulsePhaseOverride,
        )
      }
      AnomalyType.PHASE_RIFT_B -> {
        draw3dPlayfieldRift(
          canvas = canvas,
          cx = cx,
          cy = cy,
          r = rScaled,
          spinAngle = -spinAngle,
          fadeAlpha = effFade,
          outerColor = if (useCustomColors) rec!!.glowColorInt else Color.rgb(56, 189, 248),
          midColor = if (useCustomColors) rec!!.trimColorInt else Color.rgb(168, 85, 247),
          coreColor = Color.rgb(240, 253, 250),
          darkVoidCenter = true,
          inwardSpiral = false,
          pulsePhaseOverride = pulsePhaseOverride,
        )
      }
    }
  }

  fun draw3dPlayfieldRift(
    canvas: Canvas,
    cx: Float,
    cy: Float,
    r: Float,
    spinAngle: Float,
    outerColor: Int,
    midColor: Int,
    coreColor: Int,
    darkVoidCenter: Boolean,
    inwardSpiral: Boolean,
    fadeAlpha: Float = 1.0f,
    pulsePhaseOverride: Float? = null,
  ) {
    draw3dPlayfieldRiftColors(
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
      pulsePhaseOverride = pulsePhaseOverride,
    )
  }

  fun draw3dPlayfieldRiftColors(
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
    pulsePhaseOverride: Float? = null,
  ) {
    val oR = Color.red(outerColor)
    val oG = Color.green(outerColor)
    val oB = Color.blue(outerColor)
    val mR = Color.red(midColor)
    val mG = Color.green(midColor)
    val mB = Color.blue(midColor)
    val cR = Color.red(coreColor)
    val cG = Color.green(coreColor)
    val cB = Color.blue(coreColor)

    val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    val ringPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
      }
    val arcRect = RectF()

    // Layer 1: Volumetric Radial Accretion Halo
    fillPaint.color = Color.argb((34 * fadeAlpha).toInt().coerceIn(0, 255), oR, oG, oB)
    canvas.drawCircle(cx, cy, r * 1.34f, fillPaint)
    fillPaint.color = Color.argb((64 * fadeAlpha).toInt().coerceIn(0, 255), mR, mG, mB)
    canvas.drawCircle(cx, cy, r * 1.05f, fillPaint)
    fillPaint.color = Color.argb((185 * fadeAlpha).toInt().coerceIn(0, 255), 3, 8, 20)
    canvas.drawCircle(cx, cy, r * 0.88f, fillPaint)

    // Layer 2: Pulsing Accretion Shockwave Ring + Beveled Outer Containment Ring with 16 Degree Ticks
    val pulsePhase = pulsePhaseOverride ?: ((System.currentTimeMillis() % 1400L) / 1400f)
    val waveFrac = if (inwardSpiral) (1f - pulsePhase) else pulsePhase
    val waveR = r * (0.34f + waveFrac * 0.64f)
    ringPaint.strokeWidth = (r * 0.045f).coerceAtLeast(1.8f)
    ringPaint.color =
      Color.argb(
        ((1f - kotlin.math.abs(waveFrac - 0.5f) * 1.6f).coerceIn(0.15f, 1f) * 165f * fadeAlpha).toInt().coerceIn(0, 255),
        mR,
        mG,
        mB,
      )
    canvas.drawCircle(cx, cy, waveR, ringPaint)

    ringPaint.strokeWidth = (r * 0.055f).coerceAtLeast(2.2f)
    ringPaint.color = Color.argb((220 * fadeAlpha).toInt().coerceIn(0, 255), oR, oG, oB)
    canvas.drawCircle(cx, cy, r, ringPaint)

    ringPaint.strokeWidth = (r * 0.028f).coerceAtLeast(1.2f)
    ringPaint.color = Color.argb((175 * fadeAlpha).toInt().coerceIn(0, 255), cR, cG, cB)
    for (t in 0 until 16) {
      val ta = -spinAngle * 0.35f + t * (Math.PI.toFloat() / 8f)
      val r0 = if (t % 2 == 0) r * 0.90f else r * 0.94f
      canvas.drawLine(
        cx + kotlin.math.cos(ta) * r0,
        cy + kotlin.math.sin(ta) * r0,
        cx + kotlin.math.cos(ta) * r,
        cy + kotlin.math.sin(ta) * r,
        ringPaint,
      )
    }

    val innerArcR = r * 0.76f
    arcRect.set(cx - innerArcR, cy - innerArcR, cx + innerArcR, cy + innerArcR)
    ringPaint.strokeWidth = (r * 0.038f).coerceAtLeast(1.5f)
    ringPaint.color = Color.argb((170 * fadeAlpha).toInt().coerceIn(0, 255), mR, mG, mB)
    val degBase = (-spinAngle * 1.3f) * (180f / Math.PI.toFloat())
    for (seg in 0 until 4) {
      canvas.drawArc(arcRect, degBase + seg * 90f, 52f, false, ringPaint)
    }

    // Layer 3: 6 Multi-Layered 3D Logarithmic Spiral Plasma Arms
    val twistSign = if (inwardSpiral) -1f else 1f
    for (arm in 0 until 6) {
      val baseA = spinAngle + arm * (Math.PI.toFloat() / 3f)
      val r0 = r * 0.24f
      val r1 = r * 0.50f
      val r2 = r * 0.74f
      val r3 = r * 0.94f
      val a0 = baseA
      val a1 = baseA + twistSign * 0.42f
      val a2 = baseA + twistSign * 0.86f
      val a3 = baseA + twistSign * 1.28f

      val x0 = cx + kotlin.math.cos(a0) * r0
      val y0 = cy + kotlin.math.sin(a0) * r0
      val x1 = cx + kotlin.math.cos(a1) * r1
      val y1 = cy + kotlin.math.sin(a1) * r1
      val x2 = cx + kotlin.math.cos(a2) * r2
      val y2 = cy + kotlin.math.sin(a2) * r2
      val x3 = cx + kotlin.math.cos(a3) * r3
      val y3 = cy + kotlin.math.sin(a3) * r3

      ringPaint.strokeWidth = (r * 0.075f).coerceAtLeast(2.8f)
      ringPaint.color = Color.argb((95 * fadeAlpha).toInt().coerceIn(0, 255), oR, oG, oB)
      canvas.drawLine(x0, y0, x1, y1, ringPaint)
      canvas.drawLine(x1, y1, x2, y2, ringPaint)
      canvas.drawLine(x2, y2, x3, y3, ringPaint)

      ringPaint.strokeWidth = (r * 0.032f).coerceAtLeast(1.3f)
      ringPaint.color =
        if (arm % 2 == 0) Color.argb((225 * fadeAlpha).toInt().coerceIn(0, 255), cR, cG, cB)
        else Color.argb((210 * fadeAlpha).toInt().coerceIn(0, 255), mR, mG, mB)
      canvas.drawLine(x0, y0, x1, y1, ringPaint)
      canvas.drawLine(x1, y1, x2, y2, ringPaint)
      canvas.drawLine(x2, y2, x3, y3, ringPaint)
    }

    // Layer 4: 3D Event-Horizon Singularity Core & Specular Photon Ring
    val coreR = r * 0.36f
    if (darkVoidCenter) {
      ringPaint.strokeWidth = (r * 0.065f).coerceAtLeast(2.2f)
      ringPaint.color = Color.argb((245 * fadeAlpha).toInt().coerceIn(0, 255), oR, oG, oB)
      canvas.drawCircle(cx, cy, coreR * 1.06f, ringPaint)

      ringPaint.strokeWidth = (r * 0.032f).coerceAtLeast(1.3f)
      ringPaint.color = Color.argb((240 * fadeAlpha).toInt().coerceIn(0, 255), cR, cG, cB)
      canvas.drawCircle(cx, cy, coreR * 0.94f, ringPaint)

      fillPaint.color = Color.argb((248 * fadeAlpha).toInt().coerceIn(0, 255), 2, 6, 16)
      canvas.drawCircle(cx, cy, coreR * 0.86f, fillPaint)

      val lensR = coreR * 0.62f
      arcRect.set(cx - lensR, cy - lensR, cx + lensR, cy + lensR)
      ringPaint.strokeWidth = (r * 0.028f).coerceAtLeast(1.1f)
      ringPaint.color = Color.argb((195 * fadeAlpha).toInt().coerceIn(0, 255), cR, cG, cB)
      canvas.drawArc(arcRect, (spinAngle * 180f / Math.PI.toFloat()), 135f, false, ringPaint)
    } else {
      fillPaint.color = Color.argb((195 * fadeAlpha).toInt().coerceIn(0, 255), oR, oG, oB)
      canvas.drawCircle(cx, cy, coreR * 1.05f, fillPaint)
      fillPaint.color = Color.argb((235 * fadeAlpha).toInt().coerceIn(0, 255), mR, mG, mB)
      canvas.drawCircle(cx, cy, coreR * 0.72f, fillPaint)
      fillPaint.color = Color.argb((255 * fadeAlpha).toInt().coerceIn(0, 255), cR, cG, cB)
      canvas.drawCircle(cx, cy, coreR * 0.38f, fillPaint)
      ringPaint.strokeWidth = (r * 0.042f).coerceAtLeast(1.6f)
      ringPaint.color = Color.argb((235 * fadeAlpha).toInt().coerceIn(0, 255), cR, cG, cB)
      canvas.drawCircle(cx, cy, coreR, ringPaint)
    }
  }

  /**
   * Renders the converged 10.0s multicolored `Unstable Rift` singularity:
   * Counter-rotating crimson/gold/cyan/violet plasma ribbons + crackling fracture arcs + blazing white-gold core.
   */
  fun drawUnstableMergedRiftOnCanvas(
    canvas: Canvas,
    cx: Float,
    cy: Float,
    r: Float,
    spinAngle: Float,
    pulsePhase: Float = 0.5f,
    fadeAlpha: Float = 1f,
  ) {
    if (r <= 1f || fadeAlpha <= 0.01f) return
    // Base inward violet-gold vortex
    draw3dPlayfieldRiftColors(
      canvas = canvas,
      cx = cx,
      cy = cy,
      r = r,
      spinAngle = spinAngle,
      fadeAlpha = fadeAlpha,
      outerColor = Color.rgb(250, 204, 21),
      midColor = Color.rgb(239, 68, 68),
      coreColor = Color.rgb(255, 255, 255),
      darkVoidCenter = true,
      inwardSpiral = true,
      pulsePhaseOverride = pulsePhase,
    )
    // Counter-rotating cyan/amethyst shear vortex overlay
    draw3dPlayfieldRiftColors(
      canvas = canvas,
      cx = cx,
      cy = cy,
      r = r * 0.84f,
      spinAngle = -spinAngle * 1.35f,
      fadeAlpha = fadeAlpha * 0.82f,
      outerColor = Color.rgb(0, 245, 212),
      midColor = Color.rgb(168, 85, 247),
      coreColor = Color.rgb(254, 240, 138),
      darkVoidCenter = false,
      inwardSpiral = true,
      pulsePhaseOverride = (1f - pulsePhase).coerceIn(0f, 1f),
    )
    // Radial instability lightning filaments
    val arcPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeWidth = (r * 0.045f).coerceAtLeast(1.4f)
      }
    val arcColors =
      intArrayOf(
        Color.argb((230 * fadeAlpha).toInt().coerceIn(0, 255), 250, 204, 21),
        Color.argb((230 * fadeAlpha).toInt().coerceIn(0, 255), 0, 245, 212),
        Color.argb((230 * fadeAlpha).toInt().coerceIn(0, 255), 244, 63, 94),
        Color.argb((230 * fadeAlpha).toInt().coerceIn(0, 255), 192, 132, 252),
      )
    val bolt = Path()
    for (i in 0 until 6) {
      val a0 = spinAngle * 1.4f + i * (Math.PI.toFloat() / 3f)
      val r0 = r * 0.28f
      val rMid = r * 0.66f
      val r1 = r * 1.04f
      val x0 = cx + kotlin.math.cos(a0) * r0
      val y0 = cy + kotlin.math.sin(a0) * r0
      val xm = cx + kotlin.math.cos(a0 + 0.24f) * rMid
      val ym = cy + kotlin.math.sin(a0 + 0.24f) * rMid
      val x1 = cx + kotlin.math.cos(a0 - 0.14f) * r1
      val y1 = cy + kotlin.math.sin(a0 - 0.14f) * r1
      bolt.reset()
      bolt.moveTo(x0, y0)
      bolt.lineTo(xm, ym)
      bolt.lineTo(x1, y1)
      arcPaint.color = arcColors[i % arcColors.size]
      canvas.drawPath(bolt, arcPaint)
    }
  }

  private fun bakeRiftAnomalyIconBitmap(riftId: String, sizePx: Int = 96): Bitmap {
    val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val c = Canvas(bmp)
    val cx = sizePx * 0.5f
    val cy = sizePx * 0.5f
    val r = sizePx * 0.35f

    when (riftId) {
      "plasma_rift" ->
        draw3dPlayfieldRift(c, AnomalyType.PLASMA_RIFT, cx, cy, r, spinAngle = 0.55f, pulsePhaseOverride = 0.42f)
      "repulsor_rift" ->
        draw3dPlayfieldRift(c, AnomalyType.REPULSOR_RIFT, cx, cy, r, spinAngle = 0.55f, pulsePhaseOverride = 0.42f)
      "gravity_rift" ->
        draw3dPlayfieldRift(c, AnomalyType.GRAVITY_RIFT, cx, cy, r, spinAngle = 0.55f, pulsePhaseOverride = 0.42f)
      "unstable_rift" ->
        drawUnstableMergedRiftOnCanvas(c, cx, cy, r * 1.05f, spinAngle = 0.65f, pulsePhase = 0.48f)
      else -> {
        val c1x = sizePx * 0.32f
        val c1y = sizePx * 0.36f
        val c2x = sizePx * 0.68f
        val c2y = sizePx * 0.64f
        val nodeR = sizePx * 0.21f
        val bridgeGlow =
          Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 4.0f
            strokeCap = Paint.Cap.ROUND
            color = Color.argb(130, 45, 212, 191)
          }
        val bridgeCore =
          Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1.8f
            strokeCap = Paint.Cap.ROUND
            color = Color.parseColor("#E0F2FE")
          }
        val bridgePath =
          Path().apply {
            moveTo(c1x, c1y)
            lineTo((c1x + c2x) * 0.5f - 4f, (c1y + c2y) * 0.5f + 5f)
            lineTo((c1x + c2x) * 0.5f + 4f, (c1y + c2y) * 0.5f - 5f)
            lineTo(c2x, c2y)
          }
        c.drawPath(bridgePath, bridgeGlow)
        c.drawPath(bridgePath, bridgeCore)
        draw3dPlayfieldRift(c, AnomalyType.PHASE_RIFT_A, c1x, c1y, nodeR, spinAngle = 0.45f, pulsePhaseOverride = 0.38f)
        draw3dPlayfieldRift(c, AnomalyType.PHASE_RIFT_B, c2x, c2y, nodeR, spinAngle = -0.45f, pulsePhaseOverride = 0.62f)
      }
    }
    return bmp
  }

  fun bakeShipBitmap(
    spec: ShipSpec,
    sizePx: Int = 128,
    isAllied: Boolean = true,
    hullTrimStyle: Int = 0,
  ): Bitmap {
    val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val c = Canvas(bmp)
    drawMetallicShipOnCanvas(c, sizePx / 2f, sizePx / 2f, sizePx * 0.39f, spec, isAllied, hullTrimStyle)
    return bmp
  }

  /**
   * High-Definition Multi-Layered 3D Metallic Ship Renderer:
   * - Zero thruster flame plumes (strictly metallic exhaust housings)
   * - Hull-class specific 5-stop metallic armor shading + cosmetic trim bevels
   * - Protruding weapon barrels, rail prongs, torpedo/mine bays, cockpit glass & V-cyan reactor cores
   * - Crisp team readability outline (#38BDF8 allied / #EF4444 hostile)
   */
  fun drawMetallicShipOnCanvas(
    c: Canvas,
    cx: Float,
    cy: Float,
    r: Float,
    spec: ShipSpec,
    isAllied: Boolean = true,
    hullTrimStyle: Int = activeHullTrimStyle,
  ) {
    c.save()
    c.translate(cx, cy)

    // Layer 2 Base 5-stop Metallic Steel/Titanium Shader (#0F172A -> #334155 -> #CBD5E1 -> #475569 -> #0F172A)
    // blended with each ship's distinct faction hull tint
    val hullColors =
      when (spec.id) {
        9 -> // 9-Eclipse: Amethyst/Magenta twin-ring gravimetric interdictor cruiser
          intArrayOf(
            Color.parseColor("#0F172A"),
            Color.parseColor("#3B1D5E"),
            Color.parseColor("#CBD5E1"),
            Color.parseColor("#6D28D9"),
            Color.parseColor("#0F172A"),
          )
        3 -> // 3-Suppressor: Imperial Indigo/Gold swept crescent wings
          intArrayOf(
            Color.parseColor("#0F172A"),
            Color.parseColor("#2A2F6D"),
            Color.parseColor("#CBD5E1"),
            Color.parseColor("#3730A3"),
            Color.parseColor("#0F172A"),
          )
        4 -> // 4-Goliath: Heavy Dreadnought bastion armor with stepped shoulder plates
          intArrayOf(
            Color.parseColor("#0F172A"),
            Color.parseColor("#334155"),
            Color.parseColor("#CBD5E1"),
            Color.parseColor("#475569"),
            Color.parseColor("#0F172A"),
          )
        5 -> // 5-Twinfang: Predatory bio-cyan twin-boom manta frame
          intArrayOf(
            Color.parseColor("#0F172A"),
            Color.parseColor("#155E75"),
            Color.parseColor("#CBD5E1"),
            Color.parseColor("#0E7490"),
            Color.parseColor("#0F172A"),
          )
        6 -> // 6-Disruptor: Wide stingray hydrofoil with gold EMP arc emitters
          intArrayOf(
            Color.parseColor("#0F172A"),
            Color.parseColor("#1E3A5F"),
            Color.parseColor("#CBD5E1"),
            Color.parseColor("#334155"),
            Color.parseColor("#0F172A"),
          )
        7, 8 -> // 7-Valkyrie & 8-Manta: Gunmetal titanium interceptor & armored mandible pincer hull
          intArrayOf(
            Color.parseColor("#0F172A"),
            Color.parseColor("#334155"),
            Color.parseColor("#CBD5E1"),
            Color.parseColor("#475569"),
            Color.parseColor("#0F172A"),
          )
        else -> // 1-Viper & 2-Ricochet: Obsidian/Titanium-Steel precision hulls
          intArrayOf(
            Color.parseColor("#0F172A"),
            Color.parseColor("#334155"),
            Color.parseColor("#CBD5E1"),
            Color.parseColor("#475569"),
            Color.parseColor("#0F172A"),
          )
      }

    val trimColors =
      when (hullTrimStyle) {
        1 -> // Obsidian Ice-Cyan
          intArrayOf(
            Color.parseColor("#A5F3FC"),
            Color.parseColor("#00F5D4"),
            Color.parseColor("#0E7490"),
            Color.parseColor("#38BDF8"),
          )
        2 -> // Sovereign Gold
          intArrayOf(
            Color.parseColor("#FEF08A"),
            Color.parseColor("#FACC15"),
            Color.parseColor("#92400E"),
            Color.parseColor("#F59E0B"),
          )
        3 -> // Rift Violet
          intArrayOf(
            Color.parseColor("#F5D0FE"),
            Color.parseColor("#D946EF"),
            Color.parseColor("#581C87"),
            Color.parseColor("#A855F7"),
          )
        else ->
          when (spec.id) {
            9 -> // Eclipse Amethyst-Magenta & Gold trim
              intArrayOf(
                Color.parseColor("#F472B6"),
                Color.parseColor("#EC4899"),
                Color.parseColor("#831843"),
                Color.parseColor("#A855F7"),
              )
            5 -> // Twinfang Bio-Cyan & Bronze-Gold trim
              intArrayOf(
                Color.parseColor("#67E8F9"),
                Color.parseColor("#00F5D4"),
                Color.parseColor("#0E7490"),
                Color.parseColor("#F59E0B"),
              )
            3, 4, 6 -> // Imperial Gold & Amber trim
              intArrayOf(
                Color.parseColor("#FEF08A"),
                Color.parseColor("#F59E0B"),
                Color.parseColor("#92400E"),
                Color.parseColor("#FACC15"),
              )
            7, 8 -> // Valkyrie / Manta Burnished Bronze-Gold trim
              intArrayOf(
                Color.parseColor("#FDBA74"),
                Color.parseColor("#EA580C"),
                Color.parseColor("#7C2D12"),
                Color.parseColor("#F59E0B"),
              )
            else -> // Viper / Ricochet Amber-Gold & Titanium-Gold trim
              intArrayOf(
                Color.parseColor("#FDE68A"),
                Color.parseColor("#D97706"),
                Color.parseColor("#78350F"),
                Color.parseColor("#FBBF24"),
              )
          }
      }

    val underChassisShadowPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#030712")
      }

    val goldPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        shader =
          LinearGradient(
            -r,
            -r,
            r,
            r,
            trimColors,
            floatArrayOf(0f, 0.38f, 0.72f, 1f),
            Shader.TileMode.CLAMP,
          )
      }

    val hullPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        shader =
          LinearGradient(
            0f,
            -r,
            0f,
            r,
            hullColors,
            floatArrayOf(0.0f, 0.28f, 0.50f, 0.72f, 1.0f),
            Shader.TileMode.CLAMP,
          )
      }

    val upperPlatePaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        shader =
          LinearGradient(
            -r * 0.55f,
            -r * 0.55f,
            r * 0.65f,
            r * 0.55f,
            intArrayOf(
              Color.parseColor("#E2E8F0"),
              hullColors[1],
              Color.parseColor("#0F172A"),
            ),
            floatArrayOf(0f, 0.52f, 1f),
            Shader.TileMode.CLAMP,
          )
      }

    val gunBarrelPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#1E293B")
      }
    val gunBarrelStroke =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.4f
        color = Color.parseColor("#94A3B8")
      }
    val muzzleGlowPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = spec.accentColorHex
      }

    val readabilityOutlinePaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2.4f
        strokeJoin = Paint.Join.ROUND
        color = if (isAllied) Color.parseColor("#38BDF8") else Color.parseColor("#EF4444")
      }

    // Layer 3: 1.0f px bright specular edge highlight (#E2E8F0 at 60% alpha) on upper-left facing bevels
    val specularEdgePaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.0f
        strokeJoin = Paint.Join.ROUND
        color = Color.argb(153, 226, 232, 240)
      }

    // Layer 3: Crisp 1.2f px dark mechanical panel seams (#050811)
    val seamPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.2f
        color = Color.parseColor("#050811")
      }

    val intakeVentPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#050811")
      }

    // Recessed stern metallic exhaust housings + focused inner plasma core (#00F5D4 Allied / #FF5500 Hostile)
    val nozzleHousingPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#0B1120")
      }
    val nozzleRimPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.3f
        color = Color.parseColor("#64748B")
      }
    val plasmaExhaustCoreColor =
      if (isAllied) Color.parseColor("#00F5D4") else Color.parseColor("#FF5500")
    val plasmaExhaustOuterPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = plasmaExhaustCoreColor
      }
    val plasmaExhaustInnerPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.WHITE
      }

    // Layer 4: Faceted 3D Glass Cockpit Canopy with deep glowing interior
    val canopyPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        shader =
          RadialGradient(
            r * 0.22f,
            -r * 0.04f,
            r * 0.38f,
            intArrayOf(
              Color.parseColor("#38BDF8"),
              Color.parseColor("#0369A1"),
              Color.parseColor("#082F49"),
            ),
            floatArrayOf(0f, 0.58f, 1f),
            Shader.TileMode.CLAMP,
          )
      }
    val canopyReflectionPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.3f
        strokeCap = Paint.Cap.ROUND
        color = Color.argb(235, 255, 255, 255)
      }

    fun drawRecessedExhaustNozzle(nx: Float, ny: Float, halfHeight: Float) {
      c.drawRoundRect(
        nx - r * 0.14f,
        ny - halfHeight,
        nx + r * 0.10f,
        ny + halfHeight,
        3f,
        3f,
        nozzleHousingPaint,
      )
      c.drawRoundRect(
        nx - r * 0.14f,
        ny - halfHeight,
        nx + r * 0.10f,
        ny + halfHeight,
        3f,
        3f,
        nozzleRimPaint,
      )
      c.drawCircle(nx - r * 0.08f, ny, halfHeight * 0.72f, plasmaExhaustOuterPaint)
      c.drawCircle(nx - r * 0.06f, ny, halfHeight * 0.36f, plasmaExhaustInnerPaint)
    }

    val goldWingPath = Path()
    val armorHullPath = Path()
    val upperDeckPath = Path()

    when (spec.id) {
      1 -> {
        // 1-Viper: Forward-swept obsidian/amber-gold twin railgun prongs & narrow V-cyan reactor spine
        drawRecessedExhaustNozzle(-r * 0.76f, -r * 0.20f, r * 0.11f)
        drawRecessedExhaustNozzle(-r * 0.76f, r * 0.20f, r * 0.11f)

        // Protruding twin hyper-velocity railgun barrels inside forward prongs
        c.drawRect(r * 0.30f, -r * 0.22f, r * 1.18f, -r * 0.12f, gunBarrelPaint)
        c.drawRect(r * 0.30f, -r * 0.22f, r * 1.18f, -r * 0.12f, gunBarrelStroke)
        c.drawRect(r * 0.30f, r * 0.12f, r * 1.18f, r * 0.22f, gunBarrelPaint)
        c.drawRect(r * 0.30f, r * 0.12f, r * 1.18f, r * 0.22f, gunBarrelStroke)
        c.drawCircle(r * 1.16f, -r * 0.17f, r * 0.05f, muzzleGlowPaint)
        c.drawCircle(r * 1.16f, r * 0.17f, r * 0.05f, muzzleGlowPaint)

        goldWingPath.apply {
          moveTo(r * 1.12f, -r * 0.26f)
          lineTo(r * 0.40f, -r * 0.32f)
          lineTo(r * 0.22f, -r * 0.98f)
          lineTo(-r * 0.38f, -r * 0.92f)
          lineTo(-r * 0.92f, -r * 0.56f)
          lineTo(-r * 0.72f, 0f)
          lineTo(-r * 0.92f, r * 0.56f)
          lineTo(-r * 0.38f, r * 0.92f)
          lineTo(r * 0.22f, r * 0.98f)
          lineTo(r * 0.40f, r * 0.32f)
          lineTo(r * 1.12f, r * 0.26f)
          lineTo(r * 0.35f, r * 0.08f)
          lineTo(r * 0.58f, 0f)
          lineTo(r * 0.35f, -r * 0.08f)
          close()
        }
        armorHullPath.apply {
          moveTo(r * 1.02f, -r * 0.22f)
          lineTo(r * 0.28f, -r * 0.28f)
          lineTo(r * 0.10f, -r * 0.82f)
          lineTo(-r * 0.34f, -r * 0.78f)
          lineTo(-r * 0.80f, -r * 0.44f)
          lineTo(-r * 0.60f, 0f)
          lineTo(-r * 0.80f, r * 0.44f)
          lineTo(-r * 0.34f, r * 0.78f)
          lineTo(r * 0.10f, r * 0.82f)
          lineTo(r * 0.28f, r * 0.28f)
          lineTo(r * 1.02f, r * 0.22f)
          lineTo(r * 0.24f, r * 0.08f)
          lineTo(r * 0.48f, 0f)
          lineTo(r * 0.24f, -r * 0.08f)
          close()
        }
        upperDeckPath.apply {
          moveTo(r * 0.46f, 0f)
          lineTo(-r * 0.15f, -r * 0.32f)
          lineTo(-r * 0.62f, -r * 0.20f)
          lineTo(-r * 0.48f, 0f)
          lineTo(-r * 0.62f, r * 0.20f)
          lineTo(-r * 0.15f, r * 0.32f)
          close()
        }
      }

      2 -> {
        // 2-Ricochet: Titanium-Gold twin-forked deflection hull
        drawRecessedExhaustNozzle(-r * 0.76f, -r * 0.24f, r * 0.12f)
        drawRecessedExhaustNozzle(-r * 0.76f, r * 0.24f, r * 0.12f)

        c.drawRect(r * 0.10f, -r * 0.11f, r * 0.78f, r * 0.11f, gunBarrelPaint)
        c.drawRect(r * 0.10f, -r * 0.11f, r * 0.78f, r * 0.11f, gunBarrelStroke)
        c.drawCircle(r * 0.74f, 0f, r * 0.08f, muzzleGlowPaint)

        goldWingPath.apply {
          moveTo(r * 1.10f, -r * 0.36f)
          lineTo(r * 0.32f, -r * 0.16f)
          lineTo(r * 0.32f, r * 0.16f)
          lineTo(r * 1.10f, r * 0.36f)
          lineTo(r * 0.56f, r * 0.68f)
          lineTo(-r * 0.18f, r * 1.02f)
          lineTo(-r * 0.92f, r * 0.68f)
          lineTo(-r * 0.72f, 0f)
          lineTo(-r * 0.92f, -r * 0.68f)
          lineTo(-r * 0.18f, -r * 1.02f)
          lineTo(r * 0.56f, -r * 0.68f)
          close()
        }
        armorHullPath.apply {
          moveTo(r * 0.98f, -r * 0.30f)
          lineTo(r * 0.22f, -r * 0.14f)
          lineTo(r * 0.22f, r * 0.14f)
          lineTo(r * 0.98f, r * 0.30f)
          lineTo(r * 0.44f, r * 0.56f)
          lineTo(-r * 0.16f, r * 0.86f)
          lineTo(-r * 0.80f, r * 0.54f)
          lineTo(-r * 0.60f, 0f)
          lineTo(-r * 0.80f, -r * 0.54f)
          lineTo(-r * 0.16f, -r * 0.86f)
          lineTo(r * 0.44f, -r * 0.56f)
          close()
        }
        upperDeckPath.apply {
          moveTo(r * 0.68f, -r * 0.24f)
          lineTo(r * 0.10f, -r * 0.48f)
          lineTo(-r * 0.58f, -r * 0.34f)
          lineTo(-r * 0.44f, 0f)
          lineTo(-r * 0.58f, r * 0.34f)
          lineTo(r * 0.10f, r * 0.48f)
          lineTo(r * 0.68f, r * 0.24f)
          lineTo(r * 0.12f, 0f)
          close()
        }
      }

      3 -> {
        // 3-Suppressor: Imperial Indigo/Gold swept crescent wings
        drawRecessedExhaustNozzle(-r * 0.68f, -r * 0.20f, r * 0.10f)
        drawRecessedExhaustNozzle(-r * 0.68f, r * 0.20f, r * 0.10f)

        for (offsetY in floatArrayOf(-0.62f, -0.32f, 0.32f, 0.62f)) {
          val bx = if (kotlin.math.abs(offsetY) > 0.45f) r * 0.56f else r * 0.78f
          c.drawRect(bx - r * 0.35f, offsetY * r - 2.5f, bx + r * 0.22f, offsetY * r + 2.5f, gunBarrelPaint)
          c.drawRect(bx - r * 0.35f, offsetY * r - 2.5f, bx + r * 0.22f, offsetY * r + 2.5f, gunBarrelStroke)
          c.drawCircle(bx + r * 0.22f, offsetY * r, 3.2f, muzzleGlowPaint)
        }

        goldWingPath.apply {
          moveTo(r * 1.10f, 0f)
          cubicTo(r * 0.85f, -r * 0.55f, r * 0.30f, -r * 1.08f, -r * 0.72f, -r * 0.96f)
          cubicTo(-r * 0.32f, -r * 0.58f, -r * 0.30f, -r * 0.25f, -r * 0.82f, 0f)
          cubicTo(-r * 0.30f, r * 0.25f, -r * 0.32f, r * 0.58f, -r * 0.72f, r * 0.96f)
          cubicTo(r * 0.30f, r * 1.08f, r * 0.85f, r * 0.55f, r * 1.10f, 0f)
          close()
        }
        armorHullPath.apply {
          moveTo(r * 0.95f, 0f)
          cubicTo(r * 0.72f, -r * 0.46f, r * 0.22f, -r * 0.92f, -r * 0.58f, -r * 0.82f)
          cubicTo(-r * 0.22f, -r * 0.48f, -r * 0.22f, -r * 0.20f, -r * 0.68f, 0f)
          cubicTo(-r * 0.22f, r * 0.20f, -r * 0.22f, r * 0.48f, -r * 0.58f, r * 0.82f)
          cubicTo(r * 0.22f, r * 0.92f, r * 0.72f, r * 0.46f, r * 0.95f, 0f)
          close()
        }
        upperDeckPath.apply {
          moveTo(r * 0.66f, 0f)
          lineTo(r * 0.08f, -r * 0.52f)
          lineTo(-r * 0.42f, -r * 0.32f)
          lineTo(-r * 0.28f, 0f)
          lineTo(-r * 0.42f, r * 0.32f)
          lineTo(r * 0.08f, r * 0.52f)
          close()
        }
      }

      4 -> {
        // 4-Goliath: Heavy Dreadnought bastion armor with stepped shoulder plates
        drawRecessedExhaustNozzle(-r * 0.88f, -r * 0.44f, r * 0.13f)
        drawRecessedExhaustNozzle(-r * 0.84f, 0f, r * 0.15f)
        drawRecessedExhaustNozzle(-r * 0.88f, r * 0.44f, r * 0.13f)

        c.drawRect(r * 0.40f, -r * 0.46f, r * 1.14f, -r * 0.32f, gunBarrelPaint)
        c.drawRect(r * 0.40f, -r * 0.46f, r * 1.14f, -r * 0.32f, gunBarrelStroke)
        c.drawRect(r * 0.40f, r * 0.32f, r * 1.14f, r * 0.46f, gunBarrelPaint)
        c.drawRect(r * 0.40f, r * 0.32f, r * 1.14f, r * 0.46f, gunBarrelStroke)
        c.drawRect(r * 0.50f, -r * 0.16f, r * 1.08f, r * 0.16f, gunBarrelPaint)
        c.drawRect(r * 0.50f, -r * 0.16f, r * 1.08f, r * 0.16f, gunBarrelStroke)
        c.drawCircle(r * 1.04f, 0f, r * 0.10f, muzzleGlowPaint)

        goldWingPath.apply {
          moveTo(r * 0.98f, -r * 0.26f)
          lineTo(r * 0.68f, -r * 0.68f)
          lineTo(r * 0.42f, -r * 1.04f)
          lineTo(-r * 0.56f, -r * 1.04f)
          lineTo(-r * 0.98f, -r * 0.76f)
          lineTo(-r * 0.98f, -r * 0.28f)
          lineTo(-r * 0.82f, 0f)
          lineTo(-r * 0.98f, r * 0.28f)
          lineTo(-r * 0.98f, r * 0.76f)
          lineTo(-r * 0.56f, r * 1.04f)
          lineTo(r * 0.42f, r * 1.04f)
          lineTo(r * 0.68f, r * 0.68f)
          lineTo(r * 0.98f, r * 0.26f)
          close()
        }
        armorHullPath.apply {
          moveTo(r * 0.86f, -r * 0.22f)
          lineTo(r * 0.56f, -r * 0.56f)
          lineTo(r * 0.32f, -r * 0.88f)
          lineTo(-r * 0.48f, -r * 0.88f)
          lineTo(-r * 0.84f, -r * 0.64f)
          lineTo(-r * 0.72f, 0f)
          lineTo(-r * 0.84f, r * 0.64f)
          lineTo(-r * 0.48f, r * 0.88f)
          lineTo(r * 0.32f, r * 0.88f)
          lineTo(r * 0.56f, r * 0.56f)
          lineTo(r * 0.86f, r * 0.22f)
          close()
        }
        upperDeckPath.apply {
          moveTo(r * 0.62f, -r * 0.20f)
          lineTo(r * 0.20f, -r * 0.62f)
          lineTo(-r * 0.58f, -r * 0.58f)
          lineTo(-r * 0.58f, r * 0.58f)
          lineTo(r * 0.20f, r * 0.62f)
          lineTo(r * 0.62f, r * 0.20f)
          close()
        }
      }

      5 -> {
        // 5-Twinfang: Predatory bio-cyan twin-boom manta frame
        drawRecessedExhaustNozzle(-r * 0.78f, -r * 0.26f, r * 0.11f)
        drawRecessedExhaustNozzle(-r * 0.78f, r * 0.26f, r * 0.11f)

        c.drawCircle(r * 0.74f, 0f, r * 0.08f, muzzleGlowPaint)
        c.drawCircle(r * 1.06f, -r * 0.42f, r * 0.06f, muzzleGlowPaint)
        c.drawCircle(r * 1.06f, r * 0.42f, r * 0.06f, muzzleGlowPaint)

        goldWingPath.apply {
          moveTo(r * 1.16f, -r * 0.42f)
          lineTo(r * 0.28f, -r * 0.18f)
          lineTo(r * 0.68f, 0f)
          lineTo(r * 0.28f, r * 0.18f)
          lineTo(r * 1.16f, r * 0.42f)
          lineTo(r * 0.45f, r * 0.74f)
          lineTo(-r * 0.28f, r * 1.06f)
          lineTo(-r * 0.94f, r * 0.54f)
          lineTo(-r * 0.62f, 0f)
          lineTo(-r * 0.94f, -r * 0.54f)
          lineTo(-r * 0.28f, -r * 1.06f)
          lineTo(r * 0.45f, -r * 0.74f)
          close()
        }
        armorHullPath.apply {
          moveTo(r * 0.96f, -r * 0.36f)
          lineTo(r * 0.16f, -r * 0.14f)
          lineTo(r * 0.52f, 0f)
          lineTo(r * 0.16f, r * 0.14f)
          lineTo(r * 0.96f, r * 0.36f)
          lineTo(r * 0.36f, r * 0.62f)
          lineTo(-r * 0.24f, r * 0.88f)
          lineTo(-r * 0.80f, r * 0.42f)
          lineTo(-r * 0.50f, 0f)
          lineTo(-r * 0.80f, -r * 0.42f)
          lineTo(-r * 0.24f, -r * 0.88f)
          lineTo(r * 0.36f, -r * 0.62f)
          close()
        }
        upperDeckPath.apply {
          moveTo(r * 0.40f, 0f)
          lineTo(-r * 0.12f, -r * 0.54f)
          lineTo(-r * 0.56f, -r * 0.28f)
          lineTo(-r * 0.38f, 0f)
          lineTo(-r * 0.56f, r * 0.28f)
          lineTo(-r * 0.12f, r * 0.54f)
          close()
        }
      }

      6 -> {
        // 6-Disruptor: Wide stingray hydrofoil with gold EMP arc emitters
        drawRecessedExhaustNozzle(-r * 0.60f, -r * 0.28f, r * 0.10f)
        drawRecessedExhaustNozzle(-r * 0.60f, r * 0.28f, r * 0.10f)

        c.drawRect(r * 0.55f, -r * 0.28f, r * 1.16f, -r * 0.18f, goldPaint)
        c.drawRect(r * 0.55f, r * 0.18f, r * 1.16f, r * 0.28f, goldPaint)
        c.drawCircle(r * 1.14f, -r * 0.23f, r * 0.06f, muzzleGlowPaint)
        c.drawCircle(r * 1.14f, r * 0.23f, r * 0.06f, muzzleGlowPaint)

        goldWingPath.apply {
          moveTo(r * 1.06f, 0f)
          cubicTo(r * 0.72f, -r * 0.36f, r * 0.35f, -r * 1.12f, -r * 0.26f, -r * 1.06f)
          cubicTo(-r * 0.68f, -r * 0.92f, -r * 0.60f, -r * 0.36f, -r * 0.42f, -r * 0.18f)
          lineTo(-r * 1.12f, 0f)
          lineTo(-r * 0.42f, r * 0.18f)
          cubicTo(-r * 0.60f, r * 0.36f, -r * 0.68f, r * 0.92f, -r * 0.26f, r * 1.06f)
          cubicTo(r * 0.35f, r * 1.12f, r * 0.72f, r * 0.36f, r * 1.06f, 0f)
          close()
        }
        armorHullPath.apply {
          moveTo(r * 0.92f, 0f)
          cubicTo(r * 0.62f, -r * 0.30f, r * 0.28f, -r * 0.96f, -r * 0.22f, -r * 0.90f)
          cubicTo(-r * 0.56f, -r * 0.76f, -r * 0.48f, -r * 0.30f, -r * 0.34f, -r * 0.14f)
          lineTo(-r * 0.94f, 0f)
          lineTo(-r * 0.34f, r * 0.14f)
          cubicTo(-r * 0.48f, r * 0.30f, -r * 0.56f, r * 0.76f, -r * 0.22f, r * 0.90f)
          cubicTo(r * 0.28f, r * 0.96f, r * 0.62f, r * 0.30f, r * 0.92f, 0f)
          close()
        }
        upperDeckPath.apply {
          moveTo(r * 0.68f, 0f)
          lineTo(r * 0.06f, -r * 0.56f)
          lineTo(-r * 0.38f, -r * 0.38f)
          lineTo(-r * 0.56f, 0f)
          lineTo(-r * 0.38f, r * 0.38f)
          lineTo(r * 0.06f, r * 0.56f)
          close()
        }
      }

      7 -> {
        // 7-Valkyrie: Aggressive X-wing interceptor with outboard thruster pods
        drawRecessedExhaustNozzle(-r * 0.94f, -r * 0.54f, r * 0.12f)
        drawRecessedExhaustNozzle(-r * 0.94f, r * 0.54f, r * 0.12f)
        drawRecessedExhaustNozzle(-r * 0.80f, 0f, r * 0.10f)

        c.drawRect(r * 0.45f, -r * 0.24f, r * 1.16f, -r * 0.12f, gunBarrelPaint)
        c.drawRect(r * 0.45f, -r * 0.24f, r * 1.16f, -r * 0.12f, gunBarrelStroke)
        c.drawRect(r * 0.45f, r * 0.12f, r * 1.16f, r * 0.24f, gunBarrelPaint)
        c.drawRect(r * 0.45f, r * 0.12f, r * 1.16f, r * 0.24f, gunBarrelStroke)

        c.drawRect(-r * 0.98f, -r * 0.64f, -r * 0.38f, -r * 0.44f, goldPaint)
        c.drawRect(-r * 0.98f, -r * 0.64f, -r * 0.38f, -r * 0.44f, gunBarrelStroke)
        c.drawRect(-r * 0.98f, r * 0.44f, -r * 0.38f, r * 0.64f, goldPaint)
        c.drawRect(-r * 0.98f, r * 0.44f, -r * 0.38f, r * 0.64f, gunBarrelStroke)

        goldWingPath.apply {
          moveTo(r * 1.06f, 0f)
          lineTo(r * 0.42f, -r * 0.22f)
          lineTo(r * 0.72f, -r * 1.02f)
          lineTo(r * 0.32f, -r * 1.04f)
          lineTo(-r * 0.08f, -r * 0.40f)
          lineTo(-r * 0.68f, -r * 1.02f)
          lineTo(-r * 0.98f, -r * 0.86f)
          lineTo(-r * 0.56f, -r * 0.22f)
          lineTo(-r * 0.90f, 0f)
          lineTo(-r * 0.56f, r * 0.22f)
          lineTo(-r * 0.98f, r * 0.86f)
          lineTo(-r * 0.68f, r * 1.02f)
          lineTo(-r * 0.08f, r * 0.40f)
          lineTo(r * 0.32f, r * 1.04f)
          lineTo(r * 0.72f, r * 1.02f)
          lineTo(r * 0.42f, r * 0.22f)
          close()
        }
        armorHullPath.apply {
          moveTo(r * 0.92f, 0f)
          lineTo(r * 0.34f, -r * 0.18f)
          lineTo(r * 0.58f, -r * 0.88f)
          lineTo(r * 0.28f, -r * 0.90f)
          lineTo(-r * 0.08f, -r * 0.32f)
          lineTo(-r * 0.60f, -r * 0.88f)
          lineTo(-r * 0.84f, -r * 0.74f)
          lineTo(-r * 0.46f, -r * 0.18f)
          lineTo(-r * 0.76f, 0f)
          lineTo(-r * 0.46f, r * 0.18f)
          lineTo(-r * 0.84f, r * 0.74f)
          lineTo(-r * 0.60f, r * 0.88f)
          lineTo(-r * 0.08f, r * 0.32f)
          lineTo(r * 0.28f, r * 0.90f)
          lineTo(r * 0.58f, r * 0.88f)
          lineTo(r * 0.34f, r * 0.18f)
          close()
        }
        upperDeckPath.apply {
          moveTo(r * 0.64f, 0f)
          lineTo(r * 0.04f, -r * 0.28f)
          lineTo(-r * 0.56f, -r * 0.22f)
          lineTo(-r * 0.42f, 0f)
          lineTo(-r * 0.56f, r * 0.22f)
          lineTo(r * 0.04f, r * 0.28f)
          close()
        }
      }

      8 -> {
        // 8-Manta: Armored mandible pincer hull
        drawRecessedExhaustNozzle(-r * 0.82f, -r * 0.42f, r * 0.12f)
        drawRecessedExhaustNozzle(-r * 0.82f, r * 0.42f, r * 0.12f)

        c.drawRect(r * 0.32f, -r * 0.26f, r * 0.98f, -r * 0.14f, gunBarrelPaint)
        c.drawRect(r * 0.32f, -r * 0.26f, r * 0.98f, -r * 0.14f, gunBarrelStroke)
        c.drawRect(r * 0.32f, r * 0.14f, r * 0.98f, r * 0.26f, gunBarrelPaint)
        c.drawRect(r * 0.32f, r * 0.14f, r * 0.98f, r * 0.26f, gunBarrelStroke)
        c.drawCircle(-r * 0.72f, 0f, r * 0.18f, gunBarrelPaint)
        c.drawCircle(-r * 0.72f, 0f, r * 0.18f, gunBarrelStroke)

        goldWingPath.apply {
          moveTo(r * 1.12f, -r * 0.32f)
          lineTo(r * 0.78f, -r * 0.68f)
          lineTo(r * 0.28f, -r * 0.36f)
          lineTo(r * 0.28f, r * 0.36f)
          lineTo(r * 0.78f, r * 0.68f)
          lineTo(r * 1.12f, r * 0.32f)
          lineTo(r * 0.62f, r * 0.92f)
          lineTo(-r * 0.12f, r * 1.06f)
          lineTo(-r * 0.94f, r * 0.74f)
          lineTo(-r * 0.74f, r * 0.24f)
          lineTo(-r * 0.74f, -r * 0.24f)
          lineTo(-r * 0.94f, -r * 0.74f)
          lineTo(-r * 0.12f, -r * 1.06f)
          lineTo(r * 0.62f, -r * 0.92f)
          close()
        }
        armorHullPath.apply {
          moveTo(r * 0.94f, -r * 0.32f)
          lineTo(r * 0.64f, -r * 0.56f)
          lineTo(r * 0.18f, -r * 0.28f)
          lineTo(r * 0.18f, r * 0.28f)
          lineTo(r * 0.64f, r * 0.56f)
          lineTo(r * 0.94f, r * 0.32f)
          lineTo(r * 0.48f, r * 0.78f)
          lineTo(-r * 0.10f, r * 0.90f)
          lineTo(-r * 0.80f, r * 0.60f)
          lineTo(-r * 0.62f, 0f)
          lineTo(-r * 0.80f, -r * 0.60f)
          lineTo(-r * 0.10f, -r * 0.90f)
          lineTo(r * 0.48f, -r * 0.78f)
          close()
        }
        upperDeckPath.apply {
          moveTo(r * 0.44f, -r * 0.42f)
          lineTo(-r * 0.08f, -r * 0.64f)
          lineTo(-r * 0.58f, -r * 0.38f)
          lineTo(-r * 0.48f, r * 0.38f)
          lineTo(-r * 0.08f, r * 0.64f)
          lineTo(r * 0.44f, r * 0.42f)
          lineTo(r * 0.12f, 0f)
          close()
        }
      }

      else -> {
        // 9-Eclipse: Amethyst/Magenta twin-ring gravimetric interdictor cruiser
        drawRecessedExhaustNozzle(-r * 0.82f, -r * 0.28f, r * 0.12f)
        drawRecessedExhaustNozzle(-r * 0.82f, r * 0.28f, r * 0.12f)

        c.drawRect(r * 0.36f, -r * 0.30f, r * 1.12f, -r * 0.18f, gunBarrelPaint)
        c.drawRect(r * 0.36f, -r * 0.30f, r * 1.12f, -r * 0.18f, gunBarrelStroke)
        c.drawRect(r * 0.36f, r * 0.18f, r * 1.12f, r * 0.30f, gunBarrelPaint)
        c.drawRect(r * 0.36f, r * 0.18f, r * 1.12f, r * 0.30f, gunBarrelStroke)
        c.drawCircle(r * 1.08f, -r * 0.24f, r * 0.06f, muzzleGlowPaint)
        c.drawCircle(r * 1.08f, r * 0.24f, r * 0.06f, muzzleGlowPaint)

        goldWingPath.apply {
          moveTo(r * 1.10f, -r * 0.34f)
          lineTo(r * 0.64f, -r * 0.78f)
          lineTo(r * 0.08f, -r * 1.04f)
          lineTo(-r * 0.56f, -r * 0.96f)
          lineTo(-r * 0.96f, -r * 0.56f)
          lineTo(-r * 0.78f, 0f)
          lineTo(-r * 0.96f, r * 0.56f)
          lineTo(-r * 0.56f, r * 0.96f)
          lineTo(r * 0.08f, r * 1.04f)
          lineTo(r * 0.64f, r * 0.78f)
          lineTo(r * 1.10f, r * 0.34f)
          lineTo(r * 0.42f, r * 0.18f)
          lineTo(r * 0.42f, -r * 0.18f)
          close()
        }
        armorHullPath.apply {
          moveTo(r * 0.96f, -r * 0.28f)
          lineTo(r * 0.52f, -r * 0.66f)
          lineTo(r * 0.04f, -r * 0.90f)
          lineTo(-r * 0.48f, -r * 0.82f)
          lineTo(-r * 0.82f, -r * 0.46f)
          lineTo(-r * 0.64f, 0f)
          lineTo(-r * 0.82f, r * 0.46f)
          lineTo(-r * 0.48f, r * 0.82f)
          lineTo(r * 0.04f, r * 0.90f)
          lineTo(r * 0.52f, r * 0.66f)
          lineTo(r * 0.96f, r * 0.28f)
          lineTo(r * 0.30f, r * 0.14f)
          lineTo(r * 0.30f, -r * 0.14f)
          close()
        }
        upperDeckPath.apply {
          moveTo(r * 0.58f, -r * 0.22f)
          lineTo(r * 0.06f, -r * 0.58f)
          lineTo(-r * 0.52f, -r * 0.42f)
          lineTo(-r * 0.44f, 0f)
          lineTo(-r * 0.52f, r * 0.42f)
          lineTo(r * 0.06f, r * 0.58f)
          lineTo(r * 0.58f, r * 0.22f)
          lineTo(r * 0.18f, 0f)
          close()
        }
      }
    }

    // Layer 1: Under-Chassis Depth Silhouette (#030712 drop-shadow hull layer offset by +1.8f px)
    c.save()
    c.translate(1.8f, 1.8f)
    c.drawPath(goldWingPath, underChassisShadowPaint)
    c.drawPath(armorHullPath, underChassisShadowPaint)
    c.restore()

    // Layer 2A: Outer Faction Trim + Team Readability Outline
    c.drawPath(goldWingPath, goldPaint)
    c.drawPath(goldWingPath, readabilityOutlinePaint)

    // Layer 2B: Primary 5-Stop 3D Metallic Armor Hull + 1.0f px Specular Leading Edge
    c.drawPath(armorHullPath, hullPaint)
    c.drawPath(armorHullPath, specularEdgePaint)

    // Layer 3: Elevated Superstructure Deck + Recessed 1.2f px Mechanical Panel Seams & Intake Vents
    c.drawPath(upperDeckPath, upperPlatePaint)
    c.drawPath(upperDeckPath, seamPaint)
    c.drawPath(upperDeckPath, specularEdgePaint)
    c.drawLine(-r * 0.58f, -r * 0.40f, r * 0.42f, -r * 0.15f, seamPaint)
    c.drawLine(-r * 0.58f, r * 0.40f, r * 0.42f, r * 0.15f, seamPaint)
    c.drawLine(-r * 0.24f, -r * 0.64f, r * 0.14f, -r * 0.30f, seamPaint)
    c.drawLine(-r * 0.24f, r * 0.64f, r * 0.14f, r * 0.30f, seamPaint)

    // Recessed forward intake vents on port and starboard shoulders
    c.drawRect(-r * 0.18f, -r * 0.46f, r * 0.06f, -r * 0.36f, intakeVentPaint)
    c.drawRect(-r * 0.18f, r * 0.36f, r * 0.06f, r * 0.46f, intakeVentPaint)

    // Ship-specific accent details (Disruptor EMP coils, Eclipse twin singularity rings, Goliath bastion rivets)
    if (spec.id == 6) {
      val empCoilPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
          style = Paint.Style.FILL
          color = Color.parseColor("#B388FF")
        }
      c.drawCircle(-r * 0.06f, -r * 0.56f, r * 0.11f, empCoilPaint)
      c.drawCircle(-r * 0.06f, r * 0.56f, r * 0.11f, empCoilPaint)
      c.drawCircle(-r * 0.06f, -r * 0.56f, r * 0.11f, specularEdgePaint)
      c.drawCircle(-r * 0.06f, r * 0.56f, r * 0.11f, specularEdgePaint)
    } else if (spec.id == 9) {
      val ringMagentaPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
          style = Paint.Style.STROKE
          strokeWidth = 2.4f
          color = Color.parseColor("#EC4899")
        }
      val ringCyanPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
          style = Paint.Style.STROKE
          strokeWidth = 1.8f
          color = Color.parseColor("#00F5D4")
        }
      c.drawCircle(-r * 0.06f, 0f, r * 0.46f, ringMagentaPaint)
      c.drawCircle(-r * 0.06f, 0f, r * 0.30f, ringCyanPaint)
    } else if (spec.id == 4) {
      // Goliath stepped shoulder plates & armor bevel bars
      c.drawLine(-r * 0.38f, -r * 0.72f, r * 0.28f, -r * 0.72f, specularEdgePaint)
      c.drawLine(-r * 0.38f, r * 0.72f, r * 0.28f, r * 0.72f, specularEdgePaint)
      c.drawLine(-r * 0.46f, -r * 0.52f, r * 0.18f, -r * 0.52f, seamPaint)
      c.drawLine(-r * 0.46f, r * 0.52f, r * 0.18f, r * 0.52f, seamPaint)
    }

    // Layer 4A: Faceted 3D Glass Cockpit Canopy + Diagonal White Reflection Streak
    val canopyPath =
      Path().apply {
        moveTo(r * 0.58f, 0f)
        lineTo(r * 0.18f, -r * 0.18f)
        lineTo(-r * 0.08f, 0f)
        lineTo(r * 0.18f, r * 0.18f)
        close()
      }
    c.drawPath(canopyPath, canopyPaint)
    c.drawPath(canopyPath, seamPaint)
    // Facet frame lines + sharp white diagonal reflection streak
    c.drawLine(r * 0.18f, -r * 0.18f, r * 0.18f, r * 0.18f, seamPaint)
    c.drawLine(r * 0.42f, -r * 0.05f, r * 0.14f, -r * 0.12f, canopyReflectionPaint)

    // Layer 4B: Signature V-Shaped Electric-Cyan Reactor Core (with Twin-Cell variant for Goliath, Twinfang, Manta, Eclipse)
    val cyanGlowPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = if (spec.id == 9) Color.parseColor("#00F5D4") else Color.parseColor("#38BDF8")
      }
    val whiteCorePaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.WHITE
      }

    val vCoreOuter =
      Path().apply {
        moveTo(r * 0.36f, 0f)
        lineTo(-r * 0.32f, -r * 0.36f)
        lineTo(-r * 0.12f, 0f)
        lineTo(-r * 0.32f, r * 0.36f)
        close()
      }
    c.drawPath(vCoreOuter, cyanGlowPaint)

    val vCoreInner =
      Path().apply {
        moveTo(r * 0.22f, 0f)
        lineTo(-r * 0.22f, -r * 0.20f)
        lineTo(-r * 0.06f, 0f)
        lineTo(-r * 0.22f, r * 0.20f)
        close()
      }
    c.drawPath(vCoreInner, whiteCorePaint)

    if (spec.id == 4 || spec.id == 5 || spec.id == 8 || spec.id == 9) {
      c.drawCircle(-r * 0.42f, -r * 0.16f, r * 0.07f, cyanGlowPaint)
      c.drawCircle(-r * 0.42f, r * 0.16f, r * 0.07f, cyanGlowPaint)
      c.drawCircle(-r * 0.42f, -r * 0.16f, r * 0.035f, whiteCorePaint)
      c.drawCircle(-r * 0.42f, r * 0.16f, r * 0.035f, whiteCorePaint)
    }

    c.restore()
  }

  /**
   * AAA 128x128 3D Multi-Layered Modular Ship Renderer for Ship #10 (Rift Frame):
   * - Renders from the CustomHullVisualRecipe with 4-layer depth shadow, recessed plasma nozzles,
   *   5-stop metallic armor shader, recessed mechanical seams, and faceted 3D glass canopy.
   */
  fun drawCustomModularShipOnCanvas(
    c: Canvas,
    cx: Float,
    cy: Float,
    r: Float,
    recipe: CustomHullVisualRecipe,
    isAllied: Boolean = true,
  ) {
    c.save()
    c.translate(cx, cy)

    val safePrimaryHex = CustomHullVisualRecipe.ensureMinLuminanceHex(recipe.primaryHex, 0.32f)
    val safeTrimHex = CustomHullVisualRecipe.ensureMinLuminanceHex(recipe.trimHex, 0.45f)
    val safeCoreHex = CustomHullVisualRecipe.ensureMinLuminanceHex(recipe.coreGlowHex, 0.55f)

    val baseHullInt = Color.parseColor(safePrimaryHex)
    val trimInt = Color.parseColor(safeTrimHex)
    val coreInt = Color.parseColor(safeCoreHex)

    fun scaleColor(color: Int, factor: Float): Int {
      val red = (Color.red(color) * factor).toInt().coerceIn(14, 255)
      val green = (Color.green(color) * factor).toInt().coerceIn(16, 255)
      val blue = (Color.blue(color) * factor).toInt().coerceIn(22, 255)
      return Color.rgb(red, green, blue)
    }

    // 5-Stop Metallic Shader anchored with #0F172A deep edge and #CBD5E1 specular spine
    val hullColors =
      intArrayOf(
        Color.parseColor("#0F172A"),
        scaleColor(baseHullInt, 0.90f),
        Color.parseColor("#CBD5E1"),
        scaleColor(baseHullInt, 0.95f),
        Color.parseColor("#0F172A"),
      )

    val trimColors =
      intArrayOf(
        scaleColor(trimInt, 1.30f),
        trimInt,
        scaleColor(trimInt, 0.62f),
        scaleColor(trimInt, 1.15f),
      )

    val underChassisShadowPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#030712")
      }

    val trimPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        shader =
          LinearGradient(
            -r,
            -r,
            r,
            r,
            trimColors,
            floatArrayOf(0f, 0.38f, 0.72f, 1f),
            Shader.TileMode.CLAMP,
          )
      }

    val hullPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        shader =
          LinearGradient(
            0f,
            -r,
            0f,
            r,
            hullColors,
            floatArrayOf(0f, 0.28f, 0.50f, 0.72f, 1f),
            Shader.TileMode.CLAMP,
          )
      }

    val upperPlatePaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        shader =
          LinearGradient(
            -r * 0.5f,
            -r * 0.5f,
            r * 0.6f,
            r * 0.5f,
            intArrayOf(Color.parseColor("#E2E8F0"), hullColors[1], hullColors[0]),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP,
          )
      }

    val readabilityOutlinePaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2.6f
        strokeJoin = Paint.Join.ROUND
        color = if (isAllied) Color.parseColor("#38BDF8") else Color.parseColor("#EF4444")
      }

    val specularEdgePaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.0f
        strokeJoin = Paint.Join.ROUND
        color = Color.argb(153, 226, 232, 240)
      }

    val seamPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.2f
        color = Color.parseColor("#050811")
      }

    val gunBarrelPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#1E293B")
      }
    val gunBarrelStroke =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.5f
        color = Color.parseColor("#94A3B8")
      }
    val coreGlowPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = coreInt
      }
    val whiteCorePaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.WHITE
      }

    val plasmaExhaustColor =
      if (isAllied) Color.parseColor("#00F5D4") else Color.parseColor("#FF5500")
    val plasmaExhaustPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = plasmaExhaustColor
      }

    val canopyPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        shader =
          RadialGradient(
            r * 0.22f,
            -r * 0.04f,
            r * 0.36f,
            intArrayOf(
              Color.parseColor("#38BDF8"),
              Color.parseColor("#0369A1"),
              Color.parseColor("#082F49"),
            ),
            floatArrayOf(0f, 0.58f, 1f),
            Shader.TileMode.CLAMP,
          )
      }
    val canopyReflectionPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.3f
        strokeCap = Paint.Cap.ROUND
        color = Color.argb(235, 255, 255, 255)
      }

    val sweep = recipe.wingSweep.coerceIn(0.68f, 1.08f)

    // 1. Recessed Engine Housing Pods + Focused Inner Plasma Cores
    when (recipe.engineStyle) {
      "QUAD_NOVA" -> {
        for (ey in floatArrayOf(-0.62f, -0.34f, 0.34f, 0.62f)) {
          c.drawRect(-r * 0.96f, ey * r - r * 0.08f, -r * 0.45f, ey * r + r * 0.08f, trimPaint)
          c.drawRect(-r * 0.96f, ey * r - r * 0.08f, -r * 0.45f, ey * r + r * 0.08f, gunBarrelStroke)
          c.drawCircle(-r * 0.92f, ey * r, r * 0.06f, plasmaExhaustPaint)
          c.drawCircle(-r * 0.90f, ey * r, r * 0.03f, whiteCorePaint)
        }
      }
      "HEAVY_THRUSTER" -> {
        c.drawRect(-r * 1.00f, -r * 0.56f, -r * 0.42f, -r * 0.24f, trimPaint)
        c.drawRect(-r * 1.00f, r * 0.24f, -r * 0.42f, r * 0.56f, trimPaint)
        c.drawRect(-r * 1.00f, -r * 0.56f, -r * 0.42f, -r * 0.24f, gunBarrelStroke)
        c.drawRect(-r * 1.00f, r * 0.24f, -r * 0.42f, r * 0.56f, gunBarrelStroke)
        c.drawCircle(-r * 0.94f, -r * 0.40f, r * 0.10f, plasmaExhaustPaint)
        c.drawCircle(-r * 0.94f, r * 0.40f, r * 0.10f, plasmaExhaustPaint)
        c.drawCircle(-r * 0.92f, -r * 0.40f, r * 0.05f, whiteCorePaint)
        c.drawCircle(-r * 0.92f, r * 0.40f, r * 0.05f, whiteCorePaint)
      }
      "BIO_ARC" -> {
        c.drawCircle(-r * 0.78f, -r * 0.36f, r * 0.14f, plasmaExhaustPaint)
        c.drawCircle(-r * 0.78f, r * 0.36f, r * 0.14f, plasmaExhaustPaint)
        c.drawCircle(-r * 0.78f, -r * 0.36f, r * 0.06f, whiteCorePaint)
        c.drawCircle(-r * 0.78f, r * 0.36f, r * 0.06f, whiteCorePaint)
      }
      else -> {
        c.drawRect(-r * 0.94f, -r * 0.44f, -r * 0.46f, -r * 0.24f, gunBarrelPaint)
        c.drawRect(-r * 0.94f, r * 0.24f, -r * 0.46f, r * 0.44f, gunBarrelPaint)
        c.drawCircle(-r * 0.90f, -r * 0.34f, r * 0.08f, plasmaExhaustPaint)
        c.drawCircle(-r * 0.90f, r * 0.34f, r * 0.08f, plasmaExhaustPaint)
        c.drawCircle(-r * 0.88f, -r * 0.34f, r * 0.04f, whiteCorePaint)
        c.drawCircle(-r * 0.88f, r * 0.34f, r * 0.04f, whiteCorePaint)
      }
    }

    // Forward modular weapon barrels
    c.drawRect(r * 0.36f, -r * 0.24f, r * 1.12f, -r * 0.12f, gunBarrelPaint)
    c.drawRect(r * 0.36f, -r * 0.24f, r * 1.12f, -r * 0.12f, gunBarrelStroke)
    c.drawRect(r * 0.36f, r * 0.12f, r * 1.12f, r * 0.24f, gunBarrelPaint)
    c.drawRect(r * 0.36f, r * 0.12f, r * 1.12f, r * 0.24f, gunBarrelStroke)
    c.drawCircle(r * 1.10f, -r * 0.18f, r * 0.055f, coreGlowPaint)
    c.drawCircle(r * 1.10f, r * 0.18f, r * 0.055f, coreGlowPaint)

    val outerTrimPath = Path()
    val mainArmorPath = Path()
    val upperDeckPath = Path()

    when (recipe.silhouette) {
      "CRESCENT_SCYTHE" -> {
        outerTrimPath.apply {
          moveTo(r * 1.08f, 0f)
          cubicTo(r * 0.82f, -r * 0.55f * sweep, r * 0.28f, -r * 1.04f * sweep, -r * 0.72f, -r * 0.94f * sweep)
          cubicTo(-r * 0.30f, -r * 0.54f * sweep, -r * 0.28f, -r * 0.22f, -r * 0.80f, 0f)
          cubicTo(-r * 0.28f, r * 0.22f, -r * 0.30f, r * 0.54f * sweep, -r * 0.72f, r * 0.94f * sweep)
          cubicTo(r * 0.28f, r * 1.04f * sweep, r * 0.82f, r * 0.55f * sweep, r * 1.08f, 0f)
          close()
        }
        mainArmorPath.apply {
          moveTo(r * 0.94f, 0f)
          cubicTo(r * 0.70f, -r * 0.44f * sweep, r * 0.20f, -r * 0.88f * sweep, -r * 0.56f, -r * 0.80f * sweep)
          cubicTo(-r * 0.20f, -r * 0.44f * sweep, -r * 0.20f, -r * 0.18f, -r * 0.66f, 0f)
          cubicTo(-r * 0.20f, r * 0.18f, -r * 0.20f, r * 0.44f * sweep, -r * 0.56f, r * 0.80f * sweep)
          cubicTo(r * 0.20f, r * 0.88f * sweep, r * 0.70f, r * 0.44f * sweep, r * 0.94f, 0f)
          close()
        }
      }
      "BASTION_DREADNOUGHT" -> {
        outerTrimPath.apply {
          moveTo(r * 0.98f, -r * 0.28f)
          lineTo(r * 0.66f, -r * 0.68f * sweep)
          lineTo(r * 0.38f, -r * 0.98f * sweep)
          lineTo(-r * 0.56f, -r * 0.98f * sweep)
          lineTo(-r * 0.96f, -r * 0.72f * sweep)
          lineTo(-r * 0.82f, 0f)
          lineTo(-r * 0.96f, r * 0.72f * sweep)
          lineTo(-r * 0.56f, r * 0.98f * sweep)
          lineTo(r * 0.38f, r * 0.98f * sweep)
          lineTo(r * 0.66f, r * 0.68f * sweep)
          lineTo(r * 0.98f, r * 0.28f)
          close()
        }
        mainArmorPath.apply {
          moveTo(r * 0.84f, -r * 0.22f)
          lineTo(r * 0.54f, -r * 0.56f * sweep)
          lineTo(r * 0.28f, -r * 0.84f * sweep)
          lineTo(-r * 0.48f, -r * 0.84f * sweep)
          lineTo(-r * 0.82f, -r * 0.60f * sweep)
          lineTo(-r * 0.68f, 0f)
          lineTo(-r * 0.82f, r * 0.60f * sweep)
          lineTo(-r * 0.48f, r * 0.84f * sweep)
          lineTo(r * 0.28f, r * 0.84f * sweep)
          lineTo(r * 0.54f, r * 0.56f * sweep)
          lineTo(r * 0.84f, r * 0.22f)
          close()
        }
      }
      "MANTA_STINGRAY" -> {
        outerTrimPath.apply {
          moveTo(r * 1.10f, -r * 0.38f * sweep)
          lineTo(r * 0.28f, -r * 0.16f)
          lineTo(r * 0.72f, 0f)
          lineTo(r * 0.28f, r * 0.16f)
          lineTo(r * 1.10f, r * 0.38f * sweep)
          lineTo(r * 0.44f, r * 0.74f * sweep)
          lineTo(-r * 0.26f, r * 1.00f * sweep)
          lineTo(-r * 0.92f, r * 0.52f * sweep)
          lineTo(-r * 0.62f, 0f)
          lineTo(-r * 0.92f, -r * 0.52f * sweep)
          lineTo(-r * 0.26f, -r * 1.00f * sweep)
          lineTo(r * 0.44f, -r * 0.74f * sweep)
          close()
        }
        mainArmorPath.apply {
          moveTo(r * 0.94f, -r * 0.32f * sweep)
          lineTo(r * 0.18f, -r * 0.12f)
          lineTo(r * 0.54f, 0f)
          lineTo(r * 0.18f, r * 0.12f)
          lineTo(r * 0.94f, r * 0.32f * sweep)
          lineTo(r * 0.36f, r * 0.60f * sweep)
          lineTo(-r * 0.22f, r * 0.86f * sweep)
          lineTo(-r * 0.78f, r * 0.42f * sweep)
          lineTo(-r * 0.50f, 0f)
          lineTo(-r * 0.78f, -r * 0.42f * sweep)
          lineTo(-r * 0.22f, -r * 0.86f * sweep)
          lineTo(r * 0.36f, -r * 0.60f * sweep)
          close()
        }
      }
      "X_VALKYRIE" -> {
        outerTrimPath.apply {
          moveTo(r * 1.06f, 0f)
          lineTo(r * 0.42f, -r * 0.22f)
          lineTo(r * 0.72f, -r * 0.98f * sweep)
          lineTo(r * 0.32f, -r * 1.00f * sweep)
          lineTo(-r * 0.08f, -r * 0.38f)
          lineTo(-r * 0.68f, -r * 0.98f * sweep)
          lineTo(-r * 0.96f, -r * 0.82f * sweep)
          lineTo(-r * 0.54f, -r * 0.20f)
          lineTo(-r * 0.88f, 0f)
          lineTo(-r * 0.54f, r * 0.20f)
          lineTo(-r * 0.96f, r * 0.82f * sweep)
          lineTo(-r * 0.68f, r * 0.98f * sweep)
          lineTo(-r * 0.08f, r * 0.38f)
          lineTo(r * 0.32f, r * 1.00f * sweep)
          lineTo(r * 0.72f, r * 0.98f * sweep)
          lineTo(r * 0.42f, r * 0.22f)
          close()
        }
        mainArmorPath.apply {
          moveTo(r * 0.92f, 0f)
          lineTo(r * 0.34f, -r * 0.18f)
          lineTo(r * 0.58f, -r * 0.84f * sweep)
          lineTo(r * 0.28f, -r * 0.86f * sweep)
          lineTo(-r * 0.08f, -r * 0.30f)
          lineTo(-r * 0.58f, -r * 0.84f * sweep)
          lineTo(-r * 0.82f, -r * 0.70f * sweep)
          lineTo(-r * 0.44f, -r * 0.16f)
          lineTo(-r * 0.74f, 0f)
          lineTo(-r * 0.44f, r * 0.16f)
          lineTo(-r * 0.82f, r * 0.70f * sweep)
          lineTo(-r * 0.58f, r * 0.84f * sweep)
          lineTo(-r * 0.08f, r * 0.30f)
          lineTo(r * 0.28f, r * 0.86f * sweep)
          lineTo(r * 0.58f, r * 0.84f * sweep)
          lineTo(r * 0.34f, r * 0.18f)
          close()
        }
      }
      "PINCER_MANDIBLE" -> {
        outerTrimPath.apply {
          moveTo(r * 1.08f, -r * 0.30f)
          lineTo(r * 0.76f, -r * 0.66f * sweep)
          lineTo(r * 0.26f, -r * 0.34f)
          lineTo(r * 0.26f, r * 0.34f)
          lineTo(r * 0.76f, r * 0.66f * sweep)
          lineTo(r * 1.08f, r * 0.30f)
          lineTo(r * 0.58f, r * 0.90f * sweep)
          lineTo(-r * 0.12f, r * 1.00f * sweep)
          lineTo(-r * 0.92f, r * 0.70f * sweep)
          lineTo(-r * 0.72f, 0f)
          lineTo(-r * 0.92f, -r * 0.70f * sweep)
          lineTo(-r * 0.12f, -r * 1.00f * sweep)
          lineTo(r * 0.58f, -r * 0.90f * sweep)
          close()
        }
        mainArmorPath.apply {
          moveTo(r * 0.92f, -r * 0.28f)
          lineTo(r * 0.62f, -r * 0.54f * sweep)
          lineTo(r * 0.18f, -r * 0.26f)
          lineTo(r * 0.18f, r * 0.26f)
          lineTo(r * 0.62f, r * 0.54f * sweep)
          lineTo(r * 0.92f, r * 0.28f)
          lineTo(r * 0.46f, r * 0.76f * sweep)
          lineTo(-r * 0.10f, r * 0.86f * sweep)
          lineTo(-r * 0.78f, r * 0.56f * sweep)
          lineTo(-r * 0.60f, 0f)
          lineTo(-r * 0.78f, -r * 0.56f * sweep)
          lineTo(-r * 0.10f, -r * 0.86f * sweep)
          lineTo(r * 0.46f, -r * 0.76f * sweep)
          close()
        }
      }
      else -> {
        // RAZOR_INTERCEPTOR
        outerTrimPath.apply {
          moveTo(r * 1.08f, -r * 0.24f)
          lineTo(r * 0.38f, -r * 0.30f)
          lineTo(r * 0.20f, -r * 0.96f * sweep)
          lineTo(-r * 0.36f, -r * 0.90f * sweep)
          lineTo(-r * 0.90f, -r * 0.54f * sweep)
          lineTo(-r * 0.70f, 0f)
          lineTo(-r * 0.90f, r * 0.54f * sweep)
          lineTo(-r * 0.36f, r * 0.90f * sweep)
          lineTo(r * 0.20f, r * 0.96f * sweep)
          lineTo(r * 0.38f, r * 0.30f)
          lineTo(r * 1.08f, r * 0.24f)
          lineTo(r * 0.34f, r * 0.08f)
          lineTo(r * 0.60f, 0f)
          lineTo(r * 0.34f, -r * 0.08f)
          close()
        }
        mainArmorPath.apply {
          moveTo(r * 0.98f, -r * 0.20f)
          lineTo(r * 0.26f, -r * 0.26f)
          lineTo(r * 0.10f, -r * 0.80f * sweep)
          lineTo(-r * 0.32f, -r * 0.76f * sweep)
          lineTo(-r * 0.78f, -r * 0.42f * sweep)
          lineTo(-r * 0.58f, 0f)
          lineTo(-r * 0.78f, r * 0.42f * sweep)
          lineTo(-r * 0.32f, r * 0.76f * sweep)
          lineTo(r * 0.10f, r * 0.80f * sweep)
          lineTo(r * 0.26f, r * 0.26f)
          lineTo(r * 0.98f, r * 0.20f)
          lineTo(r * 0.22f, r * 0.08f)
          lineTo(r * 0.48f, 0f)
          lineTo(r * 0.22f, -r * 0.08f)
          close()
        }
      }
    }

    upperDeckPath.apply {
      moveTo(r * 0.58f, 0f)
      lineTo(r * 0.04f, -r * 0.44f)
      lineTo(-r * 0.54f, -r * 0.28f)
      lineTo(-r * 0.40f, 0f)
      lineTo(-r * 0.54f, r * 0.28f)
      lineTo(r * 0.04f, r * 0.44f)
      close()
    }

    // Layer 1: Under-Chassis Depth Silhouette (#030712 drop-shadow hull layer offset by +1.8f px)
    c.save()
    c.translate(1.8f, 1.8f)
    c.drawPath(outerTrimPath, underChassisShadowPaint)
    c.drawPath(mainArmorPath, underChassisShadowPaint)
    c.restore()

    // Layer 2: Trim + Mandatory Team Readability Outline + 5-Stop Metallic Armor Hull + Specular Highlight
    c.drawPath(outerTrimPath, trimPaint)
    c.drawPath(outerTrimPath, readabilityOutlinePaint)
    c.drawPath(mainArmorPath, hullPaint)
    c.drawPath(mainArmorPath, specularEdgePaint)

    // Layer 3: Superstructure Deck & Recessed Mechanical Seams (1..4)
    c.drawPath(upperDeckPath, upperPlatePaint)
    c.drawPath(upperDeckPath, seamPaint)
    c.drawPath(upperDeckPath, specularEdgePaint)
    for (pIdx in 1..recipe.chassisPlates.coerceIn(1, 4)) {
      val frac = pIdx * 0.15f
      c.drawLine(-r * 0.52f, -r * frac, r * 0.36f, -r * (frac * 0.5f), seamPaint)
      c.drawLine(-r * 0.52f, r * frac, r * 0.36f, r * (frac * 0.5f), seamPaint)
    }

    // Layer 4: Faceted 3D Glass Cockpit Canopy + Diagonal White Reflection Streak + V-Core Reactor
    val canopyPath =
      Path().apply {
        moveTo(r * 0.56f, 0f)
        lineTo(r * 0.16f, -r * 0.16f)
        lineTo(-r * 0.06f, 0f)
        lineTo(r * 0.16f, r * 0.16f)
        close()
      }
    c.drawPath(canopyPath, canopyPaint)
    c.drawPath(canopyPath, seamPaint)
    c.drawLine(r * 0.40f, -r * 0.04f, r * 0.14f, -r * 0.11f, canopyReflectionPaint)

    val vCoreOuter =
      Path().apply {
        moveTo(r * 0.40f, 0f)
        lineTo(-r * 0.32f, -r * 0.38f)
        lineTo(-r * 0.10f, 0f)
        lineTo(-r * 0.32f, r * 0.38f)
        close()
      }
    c.drawPath(vCoreOuter, coreGlowPaint)

    val vCoreInner =
      Path().apply {
        moveTo(r * 0.24f, 0f)
        lineTo(-r * 0.20f, -r * 0.20f)
        lineTo(-r * 0.05f, 0f)
        lineTo(-r * 0.20f, r * 0.20f)
        close()
      }
    c.drawPath(vCoreInner, whiteCorePaint)

    c.restore()
  }

  private fun blendDs9Rgb(c0: Int, c1: Int, t: Float, alpha: Int = 255): Int {
    val f = t.coerceIn(0f, 1f)
    val r = (Color.red(c0) + (Color.red(c1) - Color.red(c0)) * f).roundToInt().coerceIn(0, 255)
    val g = (Color.green(c0) + (Color.green(c1) - Color.green(c0)) * f).roundToInt().coerceIn(0, 255)
    val b = (Color.blue(c0) + (Color.blue(c1) - Color.blue(c0)) * f).roundToInt().coerceIn(0, 255)
    return Color.argb(alpha.coerceIn(0, 255), r, g, b)
  }

  /**
   * Bakes the 5x Ship-Size (`180px` world radius / `360px` diameter) Deep Space Nine (`DS9`)
   * Metallic Safe-Zone Station Sprite using the exact 5-stop steel/titanium armor shading,
   * dark readability outlines, sunlit specular bevels, and lit viewport craft of `drawMetallicShipOnCanvas`,
   * dynamically driven by `ForgeElementTarget.SAFE_ZONE_PAD` in `STAR FORGE`.
   */
  fun bakeSafeZoneDs9StationBitmap(sizePx: Int = 256): Bitmap {
    val sz = sizePx.coerceIn(96, 512)
    val bmp = Bitmap.createBitmap(sz, sz, Bitmap.Config.ARGB_8888)
    val c = Canvas(bmp)
    val cx = sz * 0.5f
    val cy = sz * 0.5f
    val r = sz * 0.48f

    val rec = RiftVisualForgeState.getRecipe(ForgeElementTarget.SAFE_ZONE_PAD)
    val prim = rec.primaryColorInt
    val trim = rec.trimColorInt
    val glow = rec.glowColorInt
    val rimDepth = rec.rimLightDepth.coerceIn(0.15f, 1.50f)
    val density = rec.detailDensity.coerceIn(0.20f, 1.50f)
    val borderWt = rec.borderWeight.coerceIn(0.45f, 2.20f)
    val glowInt = rec.glowIntensity.coerceIn(0.25f, 2.20f)

    // 5-stop metallic steel/titanium armor palette tinted by SAFE_ZONE_PAD recipe
    val stopDark0 = blendDs9Rgb(Color.rgb(15, 23, 42), prim, 0.36f)
    val stopMid1 = blendDs9Rgb(Color.rgb(51, 65, 85), prim, 0.24f)
    val stopSpec2 =
      blendDs9Rgb(
        Color.rgb(203, 213, 225),
        blendDs9Rgb(Color.WHITE, trim, 0.22f),
        (0.32f * rimDepth).coerceIn(0.10f, 0.55f),
      )
    val stopSlope3 = blendDs9Rgb(Color.rgb(71, 85, 105), prim, 0.26f)
    val stopShadow4 = blendDs9Rgb(Color.rgb(12, 19, 34), prim, 0.42f)

    val armorPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        shader =
          LinearGradient(
            cx - r * 0.92f,
            cy - r * 0.92f,
            cx + r * 0.92f,
            cy + r * 0.92f,
            intArrayOf(stopDark0, stopMid1, stopSpec2, stopSlope3, stopShadow4),
            floatArrayOf(0f, 0.28f, 0.48f, 0.72f, 1f),
            Shader.TileMode.CLAMP,
          )
      }

    val upperDeckPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        shader =
          LinearGradient(
            cx - r * 0.65f,
            cy - r * 0.65f,
            cx + r * 0.65f,
            cy + r * 0.65f,
            intArrayOf(
              blendDs9Rgb(Color.rgb(30, 41, 59), trim, 0.16f),
              blendDs9Rgb(Color.rgb(226, 232, 240), Color.WHITE, 0.35f),
              blendDs9Rgb(Color.rgb(51, 65, 85), prim, 0.28f),
              Color.rgb(15, 23, 42),
            ),
            floatArrayOf(0f, 0.36f, 0.72f, 1f),
            Shader.TileMode.CLAMP,
          )
      }

    val shadowPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.argb(175, 3, 7, 18)
      }

    val darkOutlinePaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = (sz / 128f) * 1.45f * borderWt
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
        color = Color.argb(235, 2, 6, 23)
      }

    val seamPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = (sz / 128f) * 0.90f
        strokeCap = Paint.Cap.ROUND
        color = Color.argb(190, 9, 14, 28)
      }

    val specularBevelPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = (sz / 128f) * 1.05f
        strokeCap = Paint.Cap.ROUND
        shader =
          LinearGradient(
            cx - r,
            cy - r,
            cx + r * 0.55f,
            cy + r * 0.55f,
            intArrayOf(
              Color.argb((225 * rimDepth).roundToInt().coerceIn(80, 255), 248, 250, 252),
              blendDs9Rgb(Color.rgb(148, 163, 184), trim, 0.35f, alpha = 125),
              Color.argb(0, 15, 23, 42),
            ),
            floatArrayOf(0f, 0.46f, 1f),
            Shader.TileMode.CLAMP,
          )
      }

    val trimStrokePaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = (sz / 128f) * 1.30f * borderWt
        color = blendDs9Rgb(trim, Color.WHITE, 0.14f, alpha = 225)
      }

    val conduitPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = (sz / 128f) * 1.15f
        strokeCap = Paint.Cap.ROUND
        color = blendDs9Rgb(glow, Color.WHITE, 0.20f, alpha = (185 * glowInt).roundToInt().coerceIn(70, 245))
      }

    val beaconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

    fun buildAnnulusPath(rOuter: Float, rInner: Float): Path =
      Path().apply {
        fillType = Path.FillType.EVEN_ODD
        addCircle(cx, cy, rOuter, Path.Direction.CW)
        addCircle(cx, cy, rInner, Path.Direction.CW)
      }

    val outerRingOut = r * 0.94f
    val outerRingIn = r * 0.78f
    val promRingOut = r * 0.55f
    val promRingIn = r * 0.42f
    val opsBaseR = r * 0.22f
    val opsDeckR = r * 0.145f

    val outerRingPath = buildAnnulusPath(outerRingOut, outerRingIn)
    val promRingPath = buildAnnulusPath(promRingOut, promRingIn)

    // 1. 3 Heavy Crossover Structural Bridges (6 Radial Spokes at 0°, 60°, 120°)
    val bridgesPath = Path()
    val bridgeHalfW = r * 0.056f
    for (bIdx in 0 until 3) {
      val ang = bIdx * (PI.toFloat() / 3f)
      val ca = cos(ang)
      val sa = sin(ang)
      val px = -sa * bridgeHalfW
      val py = ca * bridgeHalfW
      val spanR = outerRingIn + r * 0.02f
      val p =
        Path().apply {
          moveTo(cx - ca * spanR + px, cy - sa * spanR + py)
          lineTo(cx + ca * spanR + px, cy + sa * spanR + py)
          lineTo(cx + ca * spanR - px, cy + sa * spanR - py)
          lineTo(cx - ca * spanR - px, cy - sa * spanR - py)
          close()
        }
      bridgesPath.addPath(p)
    }

    // Under-chassis drop shadows for 3D deck separation
    val shOff = (sz / 256f) * 3.2f
    c.save()
    c.translate(shOff, shOff * 1.15f)
    c.drawPath(bridgesPath, shadowPaint)
    c.drawPath(outerRingPath, shadowPaint)
    c.drawPath(promRingPath, shadowPaint)
    c.restore()

    // Draw Crossover Bridges + Glowing Power Conduits
    c.drawPath(bridgesPath, armorPaint)
    c.drawPath(bridgesPath, darkOutlinePaint)
    c.drawPath(bridgesPath, specularBevelPaint)
    for (sIdx in 0 until 6) {
      val ang = sIdx * (PI.toFloat() / 3f)
      val ca = cos(ang)
      val sa = sin(ang)
      c.drawLine(
        cx + ca * (opsBaseR * 0.92f),
        cy + sa * (opsBaseR * 0.92f),
        cx + ca * (outerRingIn * 0.99f),
        cy + sa * (outerRingIn * 0.99f),
        conduitPaint,
      )
    }

    // 2. Outer Sovereign Docking Ring (0.78r..0.94r) with Segmented Seams & 12 Lit Docking Airlocks
    c.drawPath(outerRingPath, armorPaint)
    c.drawCircle(cx, cy, outerRingOut, trimStrokePaint)
    c.drawCircle(cx, cy, outerRingOut, darkOutlinePaint)
    c.drawCircle(cx, cy, outerRingIn, darkOutlinePaint)
    c.drawCircle(cx, cy, (outerRingOut + outerRingIn) * 0.5f, seamPaint)
    c.drawCircle(cx, cy, outerRingOut - (sz / 256f) * 1.6f, specularBevelPaint)

    val outerSeams = (18 + (10 * density).roundToInt()).coerceIn(16, 36)
    for (i in 0 until outerSeams) {
      val a = i * (2f * PI.toFloat() / outerSeams.toFloat())
      val ca = cos(a)
      val sa = sin(a)
      c.drawLine(
        cx + ca * outerRingIn,
        cy + sa * outerRingIn,
        cx + ca * outerRingOut,
        cy + sa * outerRingOut,
        seamPaint,
      )
    }

    // 12 Recessed Docking Airlocks & Lit Approach Beacons around 0.86r
    val airlockMidR = (outerRingOut + outerRingIn) * 0.5f
    for (dIdx in 0 until 12) {
      val a = dIdx * (PI.toFloat() / 6f) + (PI.toFloat() / 12f)
      val ax = cx + cos(a) * airlockMidR
      val ay = cy + sin(a) * airlockMidR
      beaconPaint.color = Color.rgb(10, 16, 30)
      c.drawCircle(ax, ay, r * 0.030f, beaconPaint)
      val portCol = if (dIdx % 3 == 0) Color.rgb(250, 204, 21) else glow
      beaconPaint.color = blendDs9Rgb(portCol, Color.WHITE, 0.25f, alpha = 235)
      c.drawCircle(ax, ay, r * 0.017f, beaconPaint)
      beaconPaint.color = Color.WHITE
      c.drawCircle(ax, ay, r * 0.008f, beaconPaint)
    }

    // 3. Inner Habitat & Promenade Ring (0.42r..0.55r) with Lit Micro-Windows
    c.drawPath(promRingPath, upperDeckPaint)
    c.drawCircle(cx, cy, promRingOut, trimStrokePaint)
    c.drawCircle(cx, cy, promRingOut, darkOutlinePaint)
    c.drawCircle(cx, cy, promRingIn, darkOutlinePaint)
    c.drawCircle(cx, cy, promRingOut - (sz / 256f) * 1.4f, specularBevelPaint)

    val promWinCount = (18 + (8 * density).roundToInt()).coerceIn(16, 32)
    val promMidR = (promRingOut + promRingIn) * 0.5f
    for (wIdx in 0 until promWinCount) {
      val wa = wIdx * (2f * PI.toFloat() / promWinCount.toFloat())
      val wx = cx + cos(wa) * promMidR
      val wy = cy + sin(wa) * promMidR
      beaconPaint.color =
        when {
          wIdx % 4 == 0 -> Color.rgb(250, 204, 21)
          wIdx % 2 == 0 -> blendDs9Rgb(glow, Color.WHITE, 0.35f)
          else -> blendDs9Rgb(trim, Color.WHITE, 0.25f)
        }
      c.drawCircle(wx, wy, r * 0.012f, beaconPaint)
    }

    // 4. 6 Swept DS9-Style Curved Docking Pylons (3 Upper-Swept + 3 Lower-Swept Symmetric Pylons)
    // Arcing inward from the Outer Docking Ring (0.93r) over the Promenade gap with glowing clamps!
    for (pIdx in 0 until 6) {
      val baseAng = pIdx * (PI.toFloat() / 3f) + (PI.toFloat() / 6f)
      val curveDir = if (pIdx % 2 == 0) 1f else -1f // Alternating upper/lower canted 3D sweep
      val baseSpread = 0.135f
      val tipAngleOffset = curveDir * 0.20f

      val aOuterL = baseAng - baseSpread
      val aOuterR = baseAng + baseSpread
      val rBase = r * 0.93f
      val rCtrl = r * 0.76f
      val rTip = r * 0.56f
      val aTip = baseAng + tipAngleOffset
      val tipSpread = 0.034f

      val bxL = cx + cos(aOuterL) * rBase
      val byL = cy + sin(aOuterL) * rBase
      val bxR = cx + cos(aOuterR) * rBase
      val byR = cy + sin(aOuterR) * rBase

      val ctrlL = baseAng - baseSpread * 0.42f + tipAngleOffset * 0.58f
      val ctrlR = baseAng + baseSpread * 0.42f + tipAngleOffset * 0.58f
      val cxL = cx + cos(ctrlL) * rCtrl
      val cyL = cy + sin(ctrlL) * rCtrl
      val cxR = cx + cos(ctrlR) * rCtrl
      val cyR = cy + sin(ctrlR) * rCtrl

      val txL = cx + cos(aTip - tipSpread) * rTip
      val tyL = cy + sin(aTip - tipSpread) * rTip
      val txR = cx + cos(aTip + tipSpread) * rTip
      val tyR = cy + sin(aTip + tipSpread) * rTip

      val pylonPath =
        Path().apply {
          moveTo(bxL, byL)
          quadTo(cxL, cyL, txL, tyL)
          lineTo(txR, tyR)
          quadTo(cxR, cyR, bxR, byR)
          close()
        }

      c.save()
      c.translate(shOff * 0.85f, shOff)
      c.drawPath(pylonPath, shadowPaint)
      c.restore()

      c.drawPath(pylonPath, upperDeckPaint)
      c.drawPath(pylonPath, darkOutlinePaint)
      c.drawPath(pylonPath, specularBevelPaint)

      // Center curved spine rib along pylon
      val spinePath =
        Path().apply {
          moveTo(cx + cos(baseAng) * (rBase * 0.98f), cy + sin(baseAng) * (rBase * 0.98f))
          quadTo(
            cx + cos(baseAng + tipAngleOffset * 0.55f) * rCtrl,
            cy + sin(baseAng + tipAngleOffset * 0.55f) * rCtrl,
            cx + cos(aTip) * rTip,
            cy + sin(aTip) * rTip,
          )
        }
      c.drawPath(spinePath, trimStrokePaint)

      // Glowing Cyan/Gold Docking Clamp at Pylon Apex
      val clampX = (txL + txR) * 0.5f
      val clampY = (tyL + tyR) * 0.5f
      val clampColor = if (pIdx % 2 == 0) glow else Color.rgb(250, 204, 21)
      beaconPaint.color = blendDs9Rgb(clampColor, Color.WHITE, 0.15f, alpha = 95)
      c.drawCircle(clampX, clampY, r * 0.044f, beaconPaint)
      beaconPaint.color = blendDs9Rgb(clampColor, Color.WHITE, 0.30f, alpha = 240)
      c.drawCircle(clampX, clampY, r * 0.022f, beaconPaint)
      beaconPaint.color = Color.WHITE
      c.drawCircle(clampX, clampY, r * 0.010f, beaconPaint)
    }

    // 5. Central Armored Ops Spire & Singularity Reactor Core (0.22r)
    val opsOctagon =
      Path().apply {
        for (v in 0 until 8) {
          val va = v * (PI.toFloat() * 0.25f) + (PI.toFloat() / 8f)
          val vx = cx + cos(va) * opsBaseR
          val vy = cy + sin(va) * opsBaseR
          if (v == 0) moveTo(vx, vy) else lineTo(vx, vy)
        }
        close()
      }
    c.save()
    c.translate(shOff * 0.7f, shOff * 0.85f)
    c.drawPath(opsOctagon, shadowPaint)
    c.restore()

    c.drawPath(opsOctagon, armorPaint)
    c.drawPath(opsOctagon, trimStrokePaint)
    c.drawPath(opsOctagon, darkOutlinePaint)
    c.drawPath(opsOctagon, specularBevelPaint)

    c.drawCircle(cx, cy, opsDeckR, upperDeckPaint)
    c.drawCircle(cx, cy, opsDeckR, darkOutlinePaint)

    for (wIdx in 0 until 8) {
      val wa = wIdx * (PI.toFloat() * 0.25f)
      val wx = cx + cos(wa) * (opsBaseR * 0.74f)
      val wy = cy + sin(wa) * (opsBaseR * 0.74f)
      beaconPaint.color = if (wIdx % 2 == 0) Color.rgb(250, 204, 21) else glow
      c.drawCircle(wx, wy, r * 0.013f, beaconPaint)
    }

    val coreR = r * 0.088f
    val reactorCorePaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        shader =
          RadialGradient(
            cx - coreR * 0.22f,
            cy - coreR * 0.22f,
            coreR * 1.45f,
            intArrayOf(
              Color.WHITE,
              blendDs9Rgb(glow, Color.WHITE, 0.35f),
              trim,
              Color.argb(0, Color.red(trim), Color.green(trim), Color.blue(trim)),
            ),
            floatArrayOf(0f, 0.38f, 0.74f, 1f),
            Shader.TileMode.CLAMP,
          )
      }
    c.drawCircle(cx, cy, coreR * 1.42f, reactorCorePaint)
    beaconPaint.color = Color.WHITE
    c.drawCircle(cx, cy, coreR * 0.42f, beaconPaint)

    return bmp
  }

  private val ds9HoloArcRect = RectF()
  private val ds9HoloBandPaint =
    Paint(Paint.ANTI_ALIAS_FLAG).apply {
      style = Paint.Style.STROKE
      strokeCap = Paint.Cap.ROUND
    }

  /**
   * Draws the shrunk, crisp 3D-tilted revolving cyan/gold holographic ring orbiting at
   * `stationRadius * 0.34f` around the central Ops spire of the `5x` DS9 Safe-Zone Station
   * (turning high-alert crimson when `isLockedOut` is true).
   */
  fun drawSafeZoneStationHoloRing(
    c: Canvas,
    cx: Float,
    cy: Float,
    stationRadius: Float,
    timeSec: Float,
    isLockedOut: Boolean = false,
    alphaScale: Float = 1f,
  ) {
    if (stationRadius <= 4f || alphaScale <= 0.02f) return
    val rec = RiftVisualForgeState.getRecipe(ForgeElementTarget.SAFE_ZONE_PAD)
    val op = (alphaScale * rec.surfaceOpacity.coerceIn(0.35f, 1f)).coerceIn(0.05f, 1f)
    val holoR = stationRadius * 0.34f

    val primaryHolo =
      if (isLockedOut) Color.rgb(244, 63, 94) else rec.glowColorInt
    val secondaryHolo =
      if (isLockedOut) Color.rgb(251, 146, 60) else Color.rgb(250, 204, 21)
    val trimHolo =
      if (isLockedOut) Color.rgb(251, 113, 133) else rec.trimColorInt

    val spinDeg = (timeSec * 46f) % 360f
    val counterDeg = (-timeSec * 32f) % 360f
    val unitStroke = (stationRadius / 180f).coerceIn(0.45f, 2.4f)

    c.save()
    c.translate(cx, cy)
    // Subtle 3D orbital tilt around the central Ops tower
    c.rotate(-16f)
    c.scale(1.0f, 0.72f)

    ds9HoloArcRect.set(-holoR, -holoR, holoR, holoR)

    // 1. Soft translucent holographic carrier band
    ds9HoloBandPaint.strokeWidth = 6.2f * unitStroke
    ds9HoloBandPaint.color =
      Color.argb(
        (58 * op).roundToInt().coerceIn(0, 255),
        Color.red(primaryHolo),
        Color.green(primaryHolo),
        Color.blue(primaryHolo),
      )
    c.drawOval(ds9HoloArcRect, ds9HoloBandPaint)

    // 2. Revolving primary cyan/crimson segmented holographic arcs
    ds9HoloBandPaint.strokeWidth = 2.4f * unitStroke
    ds9HoloBandPaint.color =
      Color.argb(
        (225 * op).roundToInt().coerceIn(0, 255),
        Color.red(primaryHolo),
        Color.green(primaryHolo),
        Color.blue(primaryHolo),
      )
    for (seg in 0 until 4) {
      c.drawArc(ds9HoloArcRect, spinDeg + seg * 90f, 54f, false, ds9HoloBandPaint)
    }

    // 3. Counter-revolving inner gold/amber harmonic ring
    val innerR = holoR * 0.86f
    ds9HoloArcRect.set(-innerR, -innerR, innerR, innerR)
    ds9HoloBandPaint.strokeWidth = 1.6f * unitStroke
    ds9HoloBandPaint.color =
      Color.argb(
        (205 * op).roundToInt().coerceIn(0, 255),
        Color.red(secondaryHolo),
        Color.green(secondaryHolo),
        Color.blue(secondaryHolo),
      )
    for (seg in 0 until 6) {
      c.drawArc(ds9HoloArcRect, counterDeg + seg * 60f, 28f, false, ds9HoloBandPaint)
    }

    // 4. 18 Precision Holographic Data Telemetry Ticks
    ds9HoloBandPaint.strokeWidth = 1.35f * unitStroke
    val tickBaseRad = (spinDeg * PI.toFloat()) / 180f
    for (t in 0 until 18) {
      val ta = tickBaseRad + t * (2f * PI.toFloat() / 18f)
      val ca = cos(ta)
      val sa = sin(ta)
      val r0 = holoR * 0.93f
      val r1 = holoR * (if (t % 3 == 0) 1.10f else 1.04f)
      val col = if (t % 3 == 0) secondaryHolo else trimHolo
      ds9HoloBandPaint.color =
        Color.argb(
          (215 * op).roundToInt().coerceIn(0, 255),
          Color.red(col),
          Color.green(col),
          Color.blue(col),
        )
      c.drawLine(ca * r0, sa * r0, ca * r1, sa * r1, ds9HoloBandPaint)
    }

    // 5. Traveling White-Hot Scanline Sheen Crest
    ds9HoloArcRect.set(-holoR, -holoR, holoR, holoR)
    ds9HoloBandPaint.strokeWidth = 2.8f * unitStroke
    ds9HoloBandPaint.color = Color.argb((235 * op).roundToInt().coerceIn(0, 255), 255, 255, 255)
    c.drawArc(ds9HoloArcRect, (spinDeg * 1.6f) % 360f, 22f, false, ds9HoloBandPaint)

    c.restore()
  }
}
