package com.example.engine

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.CornerPathEffect
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
  private val cacheLock = Any()
  private val trimmedShipSpriteCache = mutableMapOf<Int, Bitmap>()
  private val spaceObstacleBitmapCache = mutableMapOf<Int, Bitmap>()
  private val nexusHubModuleEmblemCache = mutableMapOf<String, Bitmap>()
  private val customBuffIconCache = mutableMapOf<Int, Bitmap>()

  private val sharedRiftFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
  private val sharedRiftStrokePaint =
    Paint(Paint.ANTI_ALIAS_FLAG).apply {
      style = Paint.Style.STROKE
      strokeCap = Paint.Cap.ROUND
      strokeJoin = Paint.Join.ROUND
    }
  private val sharedRiftRibbonPath = Path()
  private val sharedRiftSpinePath = Path()
  private val sharedRiftArcRect = RectF()

  private val stationHoloArcRect = RectF()
  private val stationHoloBandPaint =
    Paint(Paint.ANTI_ALIAS_FLAG).apply {
      style = Paint.Style.STROKE
      strokeCap = Paint.Cap.ROUND
    }
  private val dysonLetterPaint =
    Paint(Paint.ANTI_ALIAS_FLAG).apply {
      textAlign = Paint.Align.CENTER
      typeface = android.graphics.Typeface.create(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.BOLD)
    }

  var spriteVisualRevision: Int = 1
    private set
  var activeHullTrimStyle: Int = 0 // 0=Standard, 1=Obsidian Ice-Cyan, 2=Sovereign Gold, 3=Rift Violet
  var customShip9Version: Int = 1
    private set

  val alliedShipBitmaps: Map<Int, Bitmap>
  val hostileShipBitmaps: Map<Int, Bitmap>
  val buffIconBitmaps: Map<Int, Bitmap>
  var riftAnomalyIconBitmaps: Map<String, Bitmap>
    private set
  var fluxIconBitmap: Bitmap
    private set
  var resonanceIconBitmap: Bitmap
    private set
  var riftShardIconBitmap: Bitmap
    private set
  var safeZoneStationBitmap: Bitmap
  var supplyPodFrameBitmap: Bitmap
    private set

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
  private var lastCustomShip9Recipe: CustomHullVisualRecipe? = null

  init {
    fluxIconBitmap = bake3dFluxReactorIcon(64)
    resonanceIconBitmap = bake3dResonanceCrestIcon(64)
    riftShardIconBitmap = bake3dRiftShardIcon(64)
    safeZoneStationBitmap = bakeSafeZoneNexusStationBitmap(256)
    supplyPodFrameBitmap = bakeSupplyPodFrameBitmap(128)

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
    for (spec in RiftCodex.SHIPS) {
      allied[spec.id] = bakeShipBitmap(spec, 128, isAllied = true)
      hostile[spec.id] = bakeShipBitmap(spec, 128, isAllied = false)
    }
    alliedShipBitmaps = allied
    hostileShipBitmaps = hostile

    val initialRecipe = RiftCodex.activeCustomShipSlot.visualRecipe
    lastCustomShip9Recipe = initialRecipe
    customShip9AlliedBitmap = bakeCustomShip9Bitmap(initialRecipe, 128, isAllied = true)
    customShip9HostileBitmap = bakeCustomShip9Bitmap(initialRecipe, 128, isAllied = false)

    val buffs = mutableMapOf<Int, Bitmap>()
    for (buffType in 0..15) {
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
    synchronized(cacheLock) {
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
          safeZoneStationBitmap = bakeSafeZoneNexusStationBitmap(256)
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
        ForgeElementTarget.SUPPLY_CRATE -> {
          supplyPodFrameBitmap = bakeSupplyPodFrameBitmap(128)
        }
        else -> Unit
      }
      nexusHubModuleEmblemCache.clear()
      spriteVisualRevision++
    }
  }

  fun invalidateAllVisualTargets() {
    synchronized(cacheLock) {
      spaceObstacleBitmapCache.clear()
      nexusHubModuleEmblemCache.clear()
      wallTileBitmap = bakeWallTile()
      wallShader = BitmapShader(wallTileBitmap, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
      safeZoneStationBitmap = bakeSafeZoneNexusStationBitmap(256)
      supplyPodFrameBitmap = bakeSupplyPodFrameBitmap(128)
      fluxIconBitmap = bake3dFluxReactorIcon(64)
      resonanceIconBitmap = bake3dResonanceCrestIcon(64)
      riftShardIconBitmap = bake3dRiftShardIcon(64)
      riftAnomalyIconBitmaps = buildRiftAnomalyIconMap()
      spriteVisualRevision++
    }
  }

  /**
   * Prewarms heavy procedural sprites (all 224x224 Space Obstacle variants, active hull trim sprites,
   * and all 6 192x192 NEXUS Hub metallic module emblems) on a background worker thread after Frame #1
   * commits, eliminating main-thread SurfaceSyncGroup stalls.
   */
  fun prewarmStartupAssets(
    activeShipId: Int = 1,
    trimStyle: Int = 0,
    syndicateTag: String = "",
  ) {
    val clampedShip = activeShipId.coerceIn(1, 10)
    val clampedTrim = trimStyle.coerceIn(0, 3)
    if (clampedTrim != 0 && clampedShip in 1..9) {
      getShipSprite(clampedShip, isAllied = true, trimStyleOverride = clampedTrim)
    }
    val obstacleTypes =
      arrayOf(
        SpaceObstacleType.CRYSTAL_ASTEROID,
        SpaceObstacleType.ORBITAL_STATION,
        SpaceObstacleType.ASTEROID_STATION,
        SpaceObstacleType.FLOATING_DEBRIS,
      )
    for (type in obstacleTypes) {
      for (variant in 0..3) {
        getSpaceObstacleBitmap(type, variant)
      }
    }
    val hubModules = arrayOf("HANGAR", "CONDUIT", "FORGE", "COMMS", "CITADEL", "ARCHIVE")
    for (modKey in hubModules) {
      getNexusHubModuleEmblemBitmap(
        moduleKey = modKey,
        activeShipId = clampedShip,
        syndicateTag = syndicateTag,
        sizePx = 192,
      )
    }
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
   * Bakes the 3D hexagonal Gunmetal/Emerald Containment Supply Pod frame (`128x128`) driven live
   * by `ForgeElementTarget.SUPPLY_CRATE` in `STAR FORGE`.
   */
  fun bakeSupplyPodFrameBitmap(sizePx: Int = 128): Bitmap {
    val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val c = Canvas(bmp)
    val cx = sizePx * 0.5f
    val cy = sizePx * 0.5f
    val rec = runCatching { RiftVisualForgeState.getRecipe(ForgeElementTarget.SUPPLY_CRATE) }.getOrNull()
    val useCustom = rec != null && !rec.isFactoryDefault
    val primaryCol = if (useCustom) rec!!.primaryColorInt else Color.parseColor("#07101E")
    val trimCol = if (useCustom) rec!!.trimColorInt else Color.parseColor("#10B981")
    val glowCol = if (useCustom) rec!!.glowColorInt else Color.parseColor("#00F5D4")
    val borderMult = (rec?.borderWeight ?: 1f).coerceIn(0.55f, 2.2f)
    val opacityFrac = (rec?.surfaceOpacity ?: 0.94f).coerceIn(0.35f, 1.0f)
    val rOuter = sizePx * 0.44f
    val rInner = sizePx * 0.34f
    val cornerEffectOuter = CornerPathEffect(sizePx * 0.11f)
    val cornerEffectInner = CornerPathEffect(sizePx * 0.085f)

    fun hexPath(rad: Float): Path {
      val p = Path()
      for (i in 0 until 6) {
        val a = (i * PI / 3.0 - PI / 6.0).toFloat()
        val px = cx + cos(a) * rad
        val py = cy + sin(a) * rad
        if (i == 0) p.moveTo(px, py) else p.lineTo(px, py)
      }
      p.close()
      return p
    }

    val outerHex = hexPath(rOuter)
    val innerHex = hexPath(rInner)

    // Calm outer powerup aura
    c.drawCircle(
      cx,
      cy,
      sizePx * 0.48f,
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        shader =
          RadialGradient(
            cx,
            cy,
            sizePx * 0.48f,
            intArrayOf(
              Color.argb(58, Color.red(glowCol), Color.green(glowCol), Color.blue(glowCol)),
              Color.argb(22, Color.red(trimCol), Color.green(trimCol), Color.blue(trimCol)),
              Color.argb(0, Color.red(trimCol), Color.green(trimCol), Color.blue(trimCol)),
            ),
            floatArrayOf(0f, 0.65f, 1f),
            Shader.TileMode.CLAMP,
          )
      },
    )

    // Smooth rounded-bevel 5-stop brushed gunmetal/titanium outer containment pod
    val armorPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        pathEffect = cornerEffectOuter
        shader =
          LinearGradient(
            cx - rOuter,
            cy - rOuter,
            cx + rOuter,
            cy + rOuter,
            intArrayOf(
              Color.parseColor("#94A3B8"),
              Color.parseColor("#475569"),
              Color.parseColor("#1E293B"),
              Color.parseColor("#0F172A"),
              Color.parseColor("#040812"),
            ),
            floatArrayOf(0f, 0.24f, 0.52f, 0.80f, 1f),
            Shader.TileMode.CLAMP,
          )
      }
    c.drawPath(outerHex, armorPaint)

    // Smooth beveled outer outline + specular top-left arc
    c.drawPath(
      outerHex,
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        pathEffect = cornerEffectOuter
        strokeWidth = 2.2f * borderMult
        color = Color.parseColor("#040B16")
      },
    )
    c.drawPath(
      outerHex,
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        pathEffect = cornerEffectOuter
        strokeWidth = 1.15f * borderMult
        color = Color.argb(115, 224, 242, 254)
      },
    )

    // Translucent dark rounded containment chamber + calm radial powerup field
    c.drawPath(
      innerHex,
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        pathEffect = cornerEffectInner
        color =
          Color.argb(
            (220 * opacityFrac).roundToInt().coerceIn(85, 245),
            Color.red(primaryCol),
            Color.green(primaryCol),
            Color.blue(primaryCol),
          )
      },
    )
    c.drawCircle(
      cx,
      cy,
      rInner * 0.94f,
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        shader =
          RadialGradient(
            cx,
            cy,
            rInner * 0.96f,
            intArrayOf(
              Color.argb(82, Color.red(glowCol), Color.green(glowCol), Color.blue(glowCol)),
              Color.argb(34, Color.red(trimCol), Color.green(trimCol), Color.blue(trimCol)),
              Color.argb(0, Color.red(trimCol), Color.green(trimCol), Color.blue(trimCol)),
            ),
            floatArrayOf(0f, 0.64f, 1f),
            Shader.TileMode.CLAMP,
          )
      },
    )

    // Smooth rounded inner containment bezel + flush indicator capsules (zero protruding spikes)
    c.drawPath(
      innerHex,
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        pathEffect = cornerEffectInner
        strokeWidth = 1.85f * borderMult
        color = glowCol
      },
    )

    val arcRect = RectF(cx - rOuter * 0.89f, cy - rOuter * 0.89f, cx + rOuter * 0.89f, cy + rOuter * 0.89f)
    val accentArcPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeWidth = 2.0f * borderMult
        color = Color.argb(190, Color.red(trimCol), Color.green(trimCol), Color.blue(trimCol))
      }
    val nodePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

    for (i in 0 until 6) {
      val midEdgeDeg = i * 60f - 8f
      c.drawArc(arcRect, midEdgeDeg, 16f, false, accentArcPaint)

      val midEdgeA = ((i * 60.0) * PI / 180.0).toFloat()
      val nx = cx + cos(midEdgeA) * (rOuter * 0.89f)
      val ny = cy + sin(midEdgeA) * (rOuter * 0.89f)
      nodePaint.color = glowCol
      c.drawCircle(nx, ny, 2.0f, nodePaint)
      nodePaint.color = Color.parseColor("#E0F2FE")
      c.drawCircle(nx, ny, 0.95f, nodePaint)
    }

    return bmp
  }

  /**
   * Returns a cached 224x224 5-stop metallic/basalt 3D Space Obstacle sprite across 4 distinct seed variants.
   */
  fun getSpaceObstacleBitmap(type: SpaceObstacleType, variant: Int): Bitmap {
    val v = variant and 3
    val key = (type.ordinal shl 4) or v
    synchronized(cacheLock) {
      spaceObstacleBitmapCache[key]?.let { return it }
    }
    val baked = bakeSpaceObstacleBitmap(type, v, 224)
    synchronized(cacheLock) {
      spaceObstacleBitmapCache[key]?.let { return it }
      spaceObstacleBitmapCache[key] = baked
      return baked
    }
  }

  fun bakeSpaceObstacleBitmap(type: SpaceObstacleType, variant: Int, sizePx: Int = 224): Bitmap {
    val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val c = Canvas(bmp)
    val cx = sizePx * 0.5f
    val cy = sizePx * 0.5f
    val v = variant and 3
    val target =
      when (type) {
        SpaceObstacleType.CRYSTAL_ASTEROID -> ForgeElementTarget.OBSTACLE_SPACE_ASTEROID
        SpaceObstacleType.ORBITAL_STATION -> ForgeElementTarget.OBSTACLE_ORBITAL_STATION
        SpaceObstacleType.ASTEROID_STATION -> ForgeElementTarget.OBSTACLE_ASTEROID_STATION
        SpaceObstacleType.FLOATING_DEBRIS -> ForgeElementTarget.OBSTACLE_FLOATING_DEBRIS
        SpaceObstacleType.PERMANENT_BARRIER -> ForgeElementTarget.PLACED_STASIS_BARRIER
      }
    val rec = runCatching { RiftVisualForgeState.getRecipe(target) }.getOrNull()
    val useCustom = rec != null && !rec.isFactoryDefault
    val r = sizePx * 0.43f * (rec?.pixelScale ?: 1f).coerceIn(0.68f, 1.08f)
    val primaryCol = rec?.primaryColorInt ?: Color.parseColor("#0B1324")
    val trimCol = rec?.trimColorInt ?: Color.parseColor("#38BDF8")
    val glowCol = rec?.glowColorInt ?: Color.parseColor("#00F5D4")
    val borderMult = (rec?.borderWeight ?: 1f).coerceIn(0.45f, 2.3f)
    val glowMult = (rec?.glowIntensity ?: 1f).coerceIn(0f, 2.4f)
    val rimDepth = (rec?.rimLightDepth ?: 0.80f).coerceIn(0.15f, 1.5f)
    val styleIdx = (rec?.geometryStyle ?: 0).coerceIn(0, 3)

    fun mixRgb(cA: Int, cB: Int, t: Float): Int {
      val f = t.coerceIn(0f, 1f)
      return Color.rgb(
        (Color.red(cA) + (Color.red(cB) - Color.red(cA)) * f).roundToInt().coerceIn(0, 255),
        (Color.green(cA) + (Color.green(cB) - Color.green(cA)) * f).roundToInt().coerceIn(0, 255),
        (Color.blue(cA) + (Color.blue(cB) - Color.blue(cA)) * f).roundToInt().coerceIn(0, 255),
      )
    }

    // 5-Stop Brushed Titanium / Gunmetal Station Armor Palette (tinted live if customized in STAR FORGE)
    val metalStop0 = if (useCustom) mixRgb(Color.parseColor("#CBD5E1"), trimCol, 0.38f) else Color.parseColor("#CBD5E1")
    val metalStop1 = if (useCustom) mixRgb(Color.parseColor("#64748B"), trimCol, 0.34f) else Color.parseColor("#64748B")
    val metalStop2 = if (useCustom) mixRgb(Color.parseColor("#334155"), primaryCol, 0.48f) else Color.parseColor("#334155")
    val metalStop3 = if (useCustom) mixRgb(Color.parseColor("#1E293B"), primaryCol, 0.62f) else Color.parseColor("#1E293B")
    val metalStop4 = if (useCustom) mixRgb(Color.parseColor("#09101D"), primaryCol, 0.72f) else Color.parseColor("#09101D")

    // 5-Stop Sunlit Deep-Space Basalt Rock Palette
    val rockStop0 = if (useCustom) mixRgb(Color.parseColor("#64748B"), trimCol, 0.32f) else Color.parseColor("#64748B")
    val rockStop1 = if (useCustom) mixRgb(Color.parseColor("#475569"), trimCol, 0.25f) else Color.parseColor("#475569")
    val rockStop2 = if (useCustom) mixRgb(Color.parseColor("#273549"), primaryCol, 0.45f) else Color.parseColor("#273549")
    val rockStop3 = if (useCustom) mixRgb(Color.parseColor("#152033"), primaryCol, 0.60f) else Color.parseColor("#152033")
    val rockStop4 = if (useCustom) mixRgb(Color.parseColor("#080E1A"), primaryCol, 0.75f) else Color.parseColor("#080E1A")

    val metalArmorFill =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        shader =
          LinearGradient(
            cx - r * 0.92f,
            cy - r * 0.92f,
            cx + r * 0.92f,
            cy + r * 0.92f,
            intArrayOf(metalStop0, metalStop1, metalStop2, metalStop3, metalStop4),
            floatArrayOf(0f, 0.22f, 0.50f, 0.78f, 1f),
            Shader.TileMode.CLAMP,
          )
      }
    val basaltFill =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        shader =
          LinearGradient(
            cx - r * 0.95f,
            cy - r * 0.95f,
            cx + r * 0.95f,
            cy + r * 0.95f,
            intArrayOf(rockStop0, rockStop1, rockStop2, rockStop3, rockStop4),
            floatArrayOf(0f, 0.20f, 0.48f, 0.76f, 1f),
            Shader.TileMode.CLAMP,
          )
      }
    val darkOutline =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
        strokeWidth = 2.6f * borderMult
        color = Color.parseColor("#030712")
      }
    val seamStroke =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
        strokeWidth = 1.6f * borderMult
        color = Color.argb(190, 9, 16, 29)
      }
    val specHighlightStroke =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
        strokeWidth = (1.7f * rimDepth).coerceIn(1.0f, 3.0f)
        color = Color.argb((195 * rimDepth.coerceIn(0.3f, 1.15f)).roundToInt().coerceIn(60, 235), 226, 232, 240)
      }
    val conduitGlow =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 5.0f * borderMult
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        color =
          Color.argb(
            (110 * glowMult).roundToInt().coerceIn(0, 240),
            Color.red(glowCol),
            Color.green(glowCol),
            Color.blue(glowCol),
          )
      }
    val conduitCore =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2.1f * borderMult
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        color = glowCol
      }
    val hotSpineStroke =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.1f * borderMult
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        color = Color.parseColor("#F0FDFA")
      }

    fun drawRecessedCrater(crX: Float, crY: Float, crR: Float) {
      val pitPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
          style = Paint.Style.FILL
          shader =
            RadialGradient(
              crX - crR * 0.24f,
              crY - crR * 0.24f,
              crR * 1.15f,
              intArrayOf(
                Color.parseColor("#040812"),
                rockStop4,
                rockStop2,
              ),
              floatArrayOf(0f, 0.65f, 1f),
              Shader.TileMode.CLAMP,
            )
        }
      c.drawCircle(crX, crY, crR, pitPaint)
      val craterRect = RectF(crX - crR, crY - crR, crX + crR, crY + crR)
      // Shadowed upper-left crater lip
      c.drawArc(
        craterRect,
        140f,
        165f,
        false,
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
          style = Paint.Style.STROKE
          strokeWidth = (crR * 0.19f).coerceAtLeast(1.3f)
          strokeCap = Paint.Cap.ROUND
          color = Color.argb(215, 3, 7, 18)
        },
      )
      // Sunlit lower-right crater rim
      c.drawArc(
        craterRect,
        -40f,
        155f,
        false,
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
          style = Paint.Style.STROKE
          strokeWidth = (crR * 0.16f).coerceAtLeast(1.2f)
          strokeCap = Paint.Cap.ROUND
          color = mixRgb(rockStop0, Color.parseColor("#CBD5E1"), 0.35f)
        },
      )
    }

    when (type) {
      SpaceObstacleType.ORBITAL_STATION -> {
        val spokeCount = if (v % 2 == 1 || styleIdx == 2) 6 else 4
        val baseRotDeg = v * 22.5f + styleIdx * 12f
        c.save()
        c.translate(cx, cy)
        c.rotate(baseRotDeg)

        val localMetalFill =
          Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            shader =
              LinearGradient(
                -r * 0.9f,
                -r * 0.9f,
                r * 0.9f,
                r * 0.9f,
                intArrayOf(metalStop0, metalStop1, metalStop2, metalStop3, metalStop4),
                floatArrayOf(0f, 0.22f, 0.50f, 0.78f, 1f),
                Shader.TileMode.CLAMP,
              )
          }

        // 1. 4 Protruding Solar / Radiator Vane Fins beyond the outer ring
        val finOffsetRad = (PI / spokeCount).toFloat()
        val finFill =
          Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = mixRgb(metalStop3, trimCol, 0.30f)
          }
        val finGridStroke =
          Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1.2f
            color = Color.argb(155, Color.red(trimCol), Color.green(trimCol), Color.blue(trimCol))
          }
        for (f in 0 until 4) {
          val fa = f * (PI.toFloat() * 0.5f) + finOffsetRad
          c.save()
          c.rotate(fa * (180f / PI.toFloat()))
          val finRect = RectF(r * 0.79f, -r * 0.085f, r * 0.98f, r * 0.085f)
          c.drawRoundRect(finRect, 4f, 4f, finFill)
          c.drawRoundRect(finRect, 4f, 4f, darkOutline)
          c.drawLine(r * 0.82f, 0f, r * 0.95f, 0f, finGridStroke)
          c.drawLine(r * 0.885f, -r * 0.07f, r * 0.885f, r * 0.07f, finGridStroke)
          c.restore()
        }

        // 2. 4 or 6 Armored Radial Docking Spokes + Docking Clamps
        for (s in 0 until spokeCount) {
          val sa = s * (360f / spokeCount)
          c.save()
          c.rotate(sa)
          val spokeRect = RectF(r * 0.24f, -r * 0.075f, r * 0.86f, r * 0.075f)
          c.drawRoundRect(spokeRect, 4f, 4f, localMetalFill)
          c.drawRoundRect(spokeRect, 4f, 4f, darkOutline)
          c.drawLine(r * 0.30f, -r * 0.045f, r * 0.80f, -r * 0.045f, specHighlightStroke)
          c.drawLine(r * 0.32f, 0f, r * 0.78f, 0f, conduitCore)
          // Outer docking collar pod
          val podRect = RectF(r * 0.83f, -r * 0.105f, r * 0.94f, r * 0.105f)
          c.drawRoundRect(podRect, 4f, 4f, localMetalFill)
          c.drawRoundRect(podRect, 4f, 4f, darkOutline)
          c.restore()
        }

        // 3. Outer Docking Torus (0.84r) + Inner Command Ring (0.48r) with 5-stop Metallic Shading
        val outerRingPath =
          Path().apply {
            fillType = Path.FillType.EVEN_ODD
            addCircle(0f, 0f, r * 0.86f, Path.Direction.CW)
            addCircle(0f, 0f, r * 0.67f, Path.Direction.CW)
          }
        c.drawPath(outerRingPath, localMetalFill)
        c.drawCircle(0f, 0f, r * 0.86f, darkOutline)
        c.drawCircle(0f, 0f, r * 0.67f, darkOutline)
        // Upper-left specular rim on outer torus
        val torusSpecRect = RectF(-r * 0.84f, -r * 0.84f, r * 0.84f, r * 0.84f)
        c.drawArc(torusSpecRect, 155f, 145f, false, specHighlightStroke)
        // Recessed mechanical panel seams around outer torus
        for (seg in 0 until 12) {
          val a = seg * (PI.toFloat() / 6f)
          c.drawLine(
            cos(a) * r * 0.68f,
            sin(a) * r * 0.68f,
            cos(a) * r * 0.85f,
            sin(a) * r * 0.85f,
            seamStroke,
          )
        }

        // 12 Lit Cyan / Sovereign-Gold Habitat Micro-Windows around the 0.765r Promenade Deck
        val winPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        for (wIdx in 0 until 12) {
          val wa = (wIdx + 0.5f) * (PI.toFloat() / 6f)
          val wx = cos(wa) * (r * 0.765f)
          val wy = sin(wa) * (r * 0.765f)
          winPaint.color = if ((wIdx + v) % 3 == 0) Color.parseColor("#FACC15") else glowCol
          c.drawCircle(wx, wy, r * 0.026f, winPaint)
          winPaint.color = Color.WHITE
          c.drawCircle(wx, wy, r * 0.011f, winPaint)
        }

        // Inner Command Ring (0.48r)
        val innerRingPath =
          Path().apply {
            fillType = Path.FillType.EVEN_ODD
            addCircle(0f, 0f, r * 0.51f, Path.Direction.CW)
            addCircle(0f, 0f, r * 0.37f, Path.Direction.CW)
          }
        c.drawPath(innerRingPath, localMetalFill)
        c.drawCircle(0f, 0f, r * 0.51f, darkOutline)
        c.drawCircle(0f, 0f, r * 0.37f, darkOutline)
        c.drawCircle(
          0f,
          0f,
          r * 0.44f,
          Paint(conduitCore).apply { strokeWidth = 1.6f * borderMult },
        )

        // 4. Central 5-Stop Armored Command Dome + Glowing Reactor Core
        val domeR = r * 0.28f
        c.drawCircle(0f, 0f, domeR, localMetalFill)
        c.drawCircle(0f, 0f, domeR, darkOutline)
        val coreDomeGlow =
          Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            shader =
              RadialGradient(
                -domeR * 0.22f,
                -domeR * 0.22f,
                domeR * 0.95f,
                intArrayOf(
                  Color.WHITE,
                  Color.parseColor("#E0F2FE"),
                  glowCol,
                  trimCol,
                  metalStop4,
                ),
                floatArrayOf(0f, 0.22f, 0.52f, 0.80f, 1f),
                Shader.TileMode.CLAMP,
              )
          }
        c.drawCircle(0f, 0f, domeR * 0.68f, coreDomeGlow)
        c.restore()
      }

      SpaceObstacleType.ASTEROID_STATION -> {
        // Asymmetric 12..14-vertex Craggy Dark-Basalt Asteroid + Recessed Brushed-Titanium Hangar Outpost
        val rockProfiles =
          arrayOf(
            floatArrayOf(0.96f, 0.86f, 0.94f, 0.79f, 0.91f, 0.84f, 0.97f, 0.81f, 0.90f, 0.77f, 0.93f, 0.85f),
            floatArrayOf(0.89f, 0.97f, 0.82f, 0.93f, 0.78f, 0.95f, 0.85f, 0.91f, 0.79f, 0.96f, 0.84f, 0.88f, 0.92f),
            floatArrayOf(0.94f, 0.80f, 0.90f, 0.97f, 0.83f, 0.88f, 0.95f, 0.77f, 0.92f, 0.86f, 0.96f, 0.82f),
            floatArrayOf(0.86f, 0.94f, 0.88f, 0.78f, 0.96f, 0.83f, 0.91f, 0.95f, 0.80f, 0.89f, 0.97f, 0.84f, 0.90f, 0.81f),
          )
        val radii = rockProfiles[v]
        val vCount = radii.size
        val rockPath = Path()
        val rockPtsX = FloatArray(vCount)
        val rockPtsY = FloatArray(vCount)
        for (i in 0 until vCount) {
          val a = (i * (PI * 2.0 / vCount) + v * 0.35 + styleIdx * 0.22).toFloat()
          rockPtsX[i] = cx + cos(a) * r * radii[i]
          rockPtsY[i] = cy + sin(a) * r * radii[i]
          if (i == 0) rockPath.moveTo(rockPtsX[i], rockPtsY[i]) else rockPath.lineTo(rockPtsX[i], rockPtsY[i])
        }
        rockPath.close()
        c.drawPath(rockPath, basaltFill)

        // Internal basalt facet ridge lines + upper-left sunlit ridge bevel
        val ridgePath = Path()
        for (i in 0 until vCount) {
          val px = rockPtsX[i]
          val py = rockPtsY[i]
          val nx = rockPtsX[(i + 1) % vCount]
          val ny = rockPtsY[(i + 1) % vCount]
          if ((px + nx) * 0.5f <= cx + r * 0.18f && (py + ny) * 0.5f <= cy + r * 0.18f) {
            ridgePath.moveTo(px, py)
            ridgePath.lineTo(nx, ny)
          }
          if (i % 3 == 0) {
            c.drawLine(px, py, cx + (px - cx) * 0.42f, cy + (py - cy) * 0.42f, seamStroke)
          }
        }
        c.drawPath(ridgePath, specHighlightStroke)
        c.drawPath(rockPath, darkOutline)

        // 3 Shaded 3D Recessed Craters on the Basalt Shell
        drawRecessedCrater(cx - r * 0.46f, cy - r * 0.36f, r * 0.15f)
        drawRecessedCrater(cx - r * 0.38f, cy + r * 0.42f, r * 0.18f)
        drawRecessedCrater(cx + r * 0.34f, cy + r * 0.48f, r * 0.13f)

        // Recessed Brushed-Titanium Hangar Bay Trench + Landing Beacons + Geodesic Sensor Dome
        c.save()
        c.translate(cx, cy)
        c.rotate(v * 18f - 12f)

        val deckCollarRect = RectF(-r * 0.16f, -r * 0.38f, r * 0.68f, r * 0.34f)
        c.drawRoundRect(deckCollarRect, 9f, 9f, metalArmorFill)
        c.drawRoundRect(deckCollarRect, 9f, 9f, darkOutline)
        c.drawLine(deckCollarRect.left + 6f, deckCollarRect.top + 3f, deckCollarRect.right - 6f, deckCollarRect.top + 3f, specHighlightStroke)

        val trenchRect = RectF(-r * 0.06f, -r * 0.25f, r * 0.60f, r * 0.21f)
        c.drawRoundRect(
          trenchRect,
          6f,
          6f,
          Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.parseColor("#040914")
          },
        )
        c.drawRoundRect(
          trenchRect,
          6f,
          6f,
          Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1.6f * borderMult
            color = trimCol
          },
        )
        // Hangar forcefield shimmer + illuminated runway landing-strip guide beacons
        c.drawRoundRect(
          RectF(trenchRect.left + 4f, trenchRect.top + 4f, trenchRect.right - 4f, trenchRect.bottom - 4f),
          4f,
          4f,
          Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.argb(46, Color.red(glowCol), Color.green(glowCol), Color.blue(glowCol))
          },
        )
        val beaconDot = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        for (b in 0 until 5) {
          val bx = trenchRect.left + 10f + b * ((trenchRect.width() - 20f) / 4f)
          beaconDot.color = glowCol
          c.drawCircle(bx, trenchRect.top + 6.5f, 2.3f, beaconDot)
          c.drawCircle(bx, trenchRect.bottom - 6.5f, 2.3f, beaconDot)
          beaconDot.color = Color.WHITE
          c.drawCircle(bx, (trenchRect.top + trenchRect.bottom) * 0.5f, 1.3f, beaconDot)
        }

        // Armored Geodesic Command Sensor Dome + Comm Mast on West Ridge
        val domeCx = -r * 0.36f
        val domeCy = -r * 0.04f
        val domeR = r * 0.24f
        // Comm antenna mast
        c.drawLine(domeCx - domeR * 0.5f, domeCy - domeR * 0.7f, domeCx - domeR * 1.35f, domeCy - domeR * 1.45f, Paint(specHighlightStroke).apply { strokeWidth = 2.2f })
        beaconDot.color = Color.parseColor("#FACC15")
        c.drawCircle(domeCx - domeR * 1.35f, domeCy - domeR * 1.45f, 3.2f, beaconDot)

        c.drawCircle(domeCx, domeCy, domeR, metalArmorFill)
        c.drawCircle(domeCx, domeCy, domeR, darkOutline)
        c.drawLine(domeCx - domeR * 0.78f, domeCy, domeCx + domeR * 0.78f, domeCy, seamStroke)
        c.drawLine(domeCx, domeCy - domeR * 0.78f, domeCx, domeCy + domeR * 0.78f, seamStroke)
        beaconDot.color = glowCol
        c.drawCircle(domeCx, domeCy, domeR * 0.36f, beaconDot)
        beaconDot.color = Color.parseColor("#E0F2FE")
        c.drawCircle(domeCx - domeR * 0.08f, domeCy - domeR * 0.08f, domeR * 0.16f, beaconDot)
        c.restore()
      }

      SpaceObstacleType.CRYSTAL_ASTEROID -> {
        // Organic Multi-Faceted Craggy Basalt Boulder (4 distinct silhouettes) + Recessed 3D Craters + Glowing Rift Veins
        val profiles =
          arrayOf(
            floatArrayOf(0.97f, 0.84f, 0.91f, 0.74f, 0.95f, 0.82f, 0.89f, 0.76f, 0.96f, 0.81f, 0.90f),
            floatArrayOf(0.85f, 0.96f, 0.79f, 0.92f, 0.86f, 0.73f, 0.95f, 0.84f, 0.93f, 0.77f, 0.91f, 0.82f),
            floatArrayOf(0.93f, 0.76f, 0.89f, 0.98f, 0.81f, 0.87f, 0.74f, 0.94f, 0.83f, 0.92f),
            floatArrayOf(0.80f, 0.95f, 0.87f, 0.75f, 0.94f, 0.89f, 0.78f, 0.97f, 0.83f, 0.88f, 0.76f, 0.93f, 0.85f),
          )
        val radProfile = profiles[v]
        val n = radProfile.size
        val outerRock = Path()
        val innerPlateau = Path()
        val vx = FloatArray(n)
        val vy = FloatArray(n)
        val ix = FloatArray(n)
        val iy = FloatArray(n)
        for (i in 0 until n) {
          val a = (i * (PI * 2.0 / n) + v * 0.48 + styleIdx * 0.26).toFloat()
          val radOuter = r * radProfile[i]
          val radInner = radOuter * (if (i % 2 == 0) 0.62f else 0.54f)
          vx[i] = cx + cos(a) * radOuter
          vy[i] = cy + sin(a) * radOuter
          ix[i] = cx + cos(a + 0.08f) * radInner
          iy[i] = cy + sin(a + 0.08f) * radInner
          if (i == 0) {
            outerRock.moveTo(vx[i], vy[i])
            innerPlateau.moveTo(ix[i], iy[i])
          } else {
            outerRock.lineTo(vx[i], vy[i])
            innerPlateau.lineTo(ix[i], iy[i])
          }
        }
        outerRock.close()
        innerPlateau.close()

        c.drawPath(outerRock, basaltFill)

        // Elevated sunlit upper-left plateau + faceted ridge lines
        c.drawPath(
          innerPlateau,
          Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            shader =
              LinearGradient(
                cx - r * 0.65f,
                cy - r * 0.65f,
                cx + r * 0.65f,
                cy + r * 0.65f,
                intArrayOf(
                  mixRgb(rockStop0, Color.parseColor("#94A3B8"), 0.32f),
                  rockStop1,
                  rockStop3,
                ),
                floatArrayOf(0f, 0.48f, 1f),
                Shader.TileMode.CLAMP,
              )
          },
        )
        val sunlitBevel = Path()
        for (i in 0 until n) {
          c.drawLine(vx[i], vy[i], ix[i], iy[i], seamStroke)
          val nx = vx[(i + 1) % n]
          val ny = vy[(i + 1) % n]
          if ((vx[i] + nx) * 0.5f <= cx + r * 0.16f && (vy[i] + ny) * 0.5f <= cy + r * 0.16f) {
            sunlitBevel.moveTo(vx[i], vy[i])
            sunlitBevel.lineTo(nx, ny)
          }
        }
        c.drawPath(innerPlateau, seamStroke)
        c.drawPath(sunlitBevel, specHighlightStroke)
        c.drawPath(outerRock, darkOutline)

        // 4 Recessed 3D Impact Craters varied by seed variant
        val crOff = v * 0.65f
        drawRecessedCrater(cx + cos(crOff + 2.2f) * r * 0.46f, cy + sin(crOff + 2.2f) * r * 0.46f, r * 0.15f)
        drawRecessedCrater(cx + cos(crOff + 4.1f) * r * 0.50f, cy + sin(crOff + 4.1f) * r * 0.50f, r * 0.12f)
        drawRecessedCrater(cx + cos(crOff + 0.6f) * r * 0.48f, cy + sin(crOff + 0.6f) * r * 0.48f, r * 0.14f)
        if (v % 2 == 0) {
          drawRecessedCrater(cx + cos(crOff + 5.3f) * r * 0.36f, cy + sin(crOff + 5.3f) * r * 0.36f, r * 0.10f)
        }

        // Branching Internal Glowing Cyan / Violet Rift Crystal Fissure Veins + Embedded Crystal Cluster
        val veinPath = Path()
        val branchCount = 3 + (v % 2)
        for (b in 0 until branchCount) {
          val ba = crOff + b * (PI.toFloat() * 2f / branchCount)
          val midR = r * 0.34f
          val tipR = r * 0.64f
          val mx = cx + cos(ba + 0.22f) * midR
          val my = cy + sin(ba + 0.22f) * midR
          val tx = cx + cos(ba - 0.14f) * tipR
          val ty = cy + sin(ba - 0.14f) * tipR
          veinPath.moveTo(cx, cy)
          veinPath.lineTo(mx, my)
          veinPath.lineTo(tx, ty)
          // Sub-branch fork
          val fx = mx + cos(ba + 0.72f) * (r * 0.24f)
          val fy = my + sin(ba + 0.72f) * (r * 0.24f)
          veinPath.moveTo(mx, my)
          veinPath.lineTo(fx, fy)
        }
        c.drawPath(veinPath, conduitGlow)
        c.drawPath(veinPath, conduitCore)
        c.drawPath(veinPath, hotSpineStroke)

        // Protruding Faceted Rift Crystal Cluster at Core
        val shardFill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        for (s in 0 until 3) {
          val sa = crOff + s * 2.09f
          val tipX = cx + cos(sa) * (r * 0.24f)
          val tipY = cy + sin(sa) * (r * 0.24f)
          val pA = sa + 1.45f
          val pB = sa - 1.45f
          val shard =
            Path().apply {
              moveTo(tipX, tipY)
              lineTo(cx + cos(pA) * (r * 0.09f), cy + sin(pA) * (r * 0.09f))
              lineTo(cx, cy)
              lineTo(cx + cos(pB) * (r * 0.09f), cy + sin(pB) * (r * 0.09f))
              close()
            }
          shardFill.color = if (s == 0) glowCol else mixRgb(glowCol, Color.parseColor("#A855F7"), 0.42f)
          c.drawPath(shard, shardFill)
          c.drawPath(shard, hotSpineStroke)
        }
      }

      SpaceObstacleType.FLOATING_DEBRIS -> {
        // 4 Distinct Sheared Starship Hull & Broken Reactor-Pylon Wreckage Silhouettes (Zero rounded pills!)
        c.save()
        c.translate(cx, cy)
        c.rotate(v * 34f - 22f + styleIdx * 14f)

        val localMetalFill =
          Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            shader =
              LinearGradient(
                -r * 0.85f,
                -r * 0.75f,
                r * 0.85f,
                r * 0.75f,
                intArrayOf(metalStop0, metalStop1, metalStop2, metalStop3, metalStop4),
                floatArrayOf(0f, 0.22f, 0.50f, 0.78f, 1f),
                Shader.TileMode.CLAMP,
              )
          }
        val ribPaint =
          Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.SQUARE
            strokeWidth = 3.2f * borderMult
            color = Color.parseColor("#475569")
          }
        val scorchPaint =
          Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.argb(175, 8, 12, 22)
          }

        // Exposed structural titanium ribs protruding from sheared tear edge
        for (ribIdx in -2..2) {
          val ry = ribIdx * (r * 0.14f)
          c.drawLine(r * 0.28f, ry, r * 0.82f - kotlin.math.abs(ribIdx) * r * 0.08f, ry + ribIdx * 3f, ribPaint)
        }

        val hullWreck = Path()
        when (v) {
          0 -> { // Sheared Starfighter Wing Spar & Torn Nacelle
            hullWreck.moveTo(-r * 0.84f, -r * 0.14f)
            hullWreck.lineTo(-r * 0.28f, -r * 0.58f)
            hullWreck.lineTo(r * 0.46f, -r * 0.42f)
            hullWreck.lineTo(r * 0.30f, -r * 0.12f)
            hullWreck.lineTo(r * 0.58f, r * 0.08f)
            hullWreck.lineTo(r * 0.22f, r * 0.32f)
            hullWreck.lineTo(r * 0.44f, r * 0.54f)
            hullWreck.lineTo(-r * 0.42f, r * 0.48f)
            hullWreck.close()
          }
          1 -> { // Fractured Orbital Ring Arc Segment
            hullWreck.moveTo(-r * 0.78f, -r * 0.44f)
            hullWreck.lineTo(-r * 0.12f, -r * 0.68f)
            hullWreck.lineTo(r * 0.66f, -r * 0.36f)
            hullWreck.lineTo(r * 0.44f, r * 0.08f)
            hullWreck.lineTo(r * 0.14f, -r * 0.16f)
            hullWreck.lineTo(-r * 0.28f, -r * 0.04f)
            hullWreck.lineTo(-r * 0.42f, r * 0.52f)
            hullWreck.lineTo(-r * 0.84f, r * 0.22f)
            hullWreck.close()
          }
          2 -> { // Armored Cruiser Bulkhead Wedge with Severed Pylon
            hullWreck.moveTo(-r * 0.88f, 0f)
            hullWreck.lineTo(-r * 0.18f, -r * 0.56f)
            hullWreck.lineTo(r * 0.52f, -r * 0.32f)
            hullWreck.lineTo(r * 0.34f, r * 0.02f)
            hullWreck.lineTo(r * 0.62f, r * 0.28f)
            hullWreck.lineTo(-r * 0.14f, r * 0.58f)
            hullWreck.close()
          }
          else -> { // Split Twin-Conduit Reactor Housing
            hullWreck.moveTo(-r * 0.76f, -r * 0.48f)
            hullWreck.lineTo(r * 0.42f, -r * 0.52f)
            hullWreck.lineTo(r * 0.56f, -r * 0.18f)
            hullWreck.lineTo(r * 0.12f, -r * 0.08f)
            hullWreck.lineTo(r * 0.12f, r * 0.10f)
            hullWreck.lineTo(r * 0.58f, r * 0.22f)
            hullWreck.lineTo(r * 0.38f, r * 0.54f)
            hullWreck.lineTo(-r * 0.72f, r * 0.44f)
            hullWreck.close()
          }
        }

        c.drawPath(hullWreck, localMetalFill)
        c.drawCircle(r * 0.34f, r * 0.04f, r * 0.22f, scorchPaint)
        c.drawPath(hullWreck, darkOutline)
        c.drawLine(-r * 0.68f, -r * 0.16f, r * 0.24f, -r * 0.34f, specHighlightStroke)
        c.drawLine(-r * 0.52f, r * 0.18f, r * 0.20f, r * 0.18f, seamStroke)

        // Broken Glowing Cyan Conduit + Amber Spark Nodes at the Tear Edge
        c.drawLine(-r * 0.50f, -r * 0.04f, r * 0.28f, -r * 0.04f, conduitGlow)
        c.drawLine(-r * 0.50f, -r * 0.04f, r * 0.28f, -r * 0.04f, conduitCore)
        val sparkDot = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        sparkDot.color = glowCol
        c.drawCircle(-r * 0.48f, -r * 0.04f, 3.4f, sparkDot)
        sparkDot.color = Color.parseColor("#F59E0B")
        c.drawCircle(r * 0.32f, -r * 0.04f, 4.0f, sparkDot)
        sparkDot.color = Color.WHITE
        c.drawCircle(r * 0.32f, -r * 0.04f, 1.9f, sparkDot)
        c.restore()
      }

      SpaceObstacleType.PERMANENT_BARRIER -> {
        val hex = Path()
        for (i in 0 until 6) {
          val a = (i * PI / 3.0 - PI / 6.0).toFloat()
          val px = cx + cos(a) * r * 0.78f
          val py = cy + sin(a) * r * 0.78f
          if (i == 0) hex.moveTo(px, py) else hex.lineTo(px, py)
        }
        hex.close()
        c.drawPath(hex, metalArmorFill)
        c.drawPath(hex, darkOutline)
        c.drawCircle(cx, cy, r * 0.36f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = glowCol })
        c.drawCircle(cx, cy, r * 0.18f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#E0F2FE") })
      }
    }
    return bmp
  }

  fun updateCustomShip9Sprites(recipe: CustomHullVisualRecipe) {
    synchronized(cacheLock) {
      if (recipe == lastCustomShip9Recipe) return
    }
    val alliedBmp = bakeCustomShip9Bitmap(recipe, 128, isAllied = true)
    val hostileBmp = bakeCustomShip9Bitmap(recipe, 128, isAllied = false)
    synchronized(cacheLock) {
      lastCustomShip9Recipe = recipe
      customShip9AlliedBitmap = alliedBmp
      customShip9HostileBitmap = hostileBmp
      customShip9Version++
    }
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
    @Suppress("UNUSED_PARAMETER") isHitFlashing: Boolean = false,
    trimStyleOverride: Int = if (isAllied) activeHullTrimStyle else 0,
  ): Bitmap {
    if (shipId == 10 || shipId > 9) {
      return if (isAllied) customShip9AlliedBitmap else customShip9HostileBitmap
    }
    val effectiveTrim = trimStyleOverride.coerceIn(0, 3)
    if (effectiveTrim != 0) {
      val cacheKey = (shipId shl 4) or (effectiveTrim shl 1) or (if (isAllied) 1 else 0)
      synchronized(cacheLock) {
        trimmedShipSpriteCache[cacheKey]?.let { return it }
      }
      val baked = bakeShipBitmap(RiftCodex.getShipById(shipId), 128, isAllied, effectiveTrim)
      synchronized(cacheLock) {
        trimmedShipSpriteCache[cacheKey]?.let { return it }
        trimmedShipSpriteCache[cacheKey] = baked
        return baked
      }
    }
    val map = if (isAllied) alliedShipBitmaps else hostileShipBitmaps
    return map[shipId] ?: map[1] ?: bakeShipBitmap(RiftCodex.getShipById(shipId), 128, isAllied)
  }

  fun invalidateCustomBuffIcon(buffType: Int) {
    synchronized(cacheLock) {
      customBuffIconCache.remove(buffType)
    }
  }

  fun getBuffIconBitmap(buffType: Int): Bitmap {
    if (buffType in 0..15 || buffType in -14..-10) {
      return buffIconBitmaps[buffType] ?: buffIconBitmaps[10]!!
    }
    synchronized(cacheLock) {
      customBuffIconCache[buffType]?.let { return it }
    }
    val entry = RiftCodex.getBuffRegistryByIndex(buffType)
    if (entry != null) {
      val baked = bakeCustomBuffIconBitmap(entry, 68)
      synchronized(cacheLock) {
        customBuffIconCache[buffType]?.let { return it }
        customBuffIconCache[buffType] = baked
        return baked
      }
    }
    return buffIconBitmaps[buffType.mod(16)] ?: buffIconBitmaps[0]!!
  }

  /**
   * Renders the 8-slice pizza-colored spiked Chakram disc onto any Android Canvas (used for both
   * Supply Crate / HUD icon baking and live 60fps spinning projectile rendering in RiftArenaView).
   */
  fun drawChakramDiscOnCanvas(
    c: Canvas,
    cx: Float,
    cy: Float,
    radius: Float,
    spinAngleRad: Float = 0f,
  ) {
    val discR = radius * 0.80f
    val crustInnerR = discR * 0.80f
    val spikeOuterR = radius * 1.12f

    // 1. 8 metallic saw-tooth spikes around the spinning rim
    val spikeFill =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#E2E8F0")
      }
    val spikeEdge =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = (radius * 0.06f).coerceAtLeast(1.1f)
        strokeJoin = Paint.Join.ROUND
        color = Color.parseColor("#64748B")
      }
    val spikePath = Path()
    for (i in 0 until 8) {
      val baseAng = spinAngleRad + i * (PI.toFloat() * 0.25f) + (PI.toFloat() / 8f)
      val leadAng = baseAng + 0.16f
      val trailAng = baseAng - 0.18f
      val tipAng = baseAng + 0.08f
      spikePath.reset()
      spikePath.moveTo(cx + cos(trailAng) * (discR * 0.90f), cy + sin(trailAng) * (discR * 0.90f))
      spikePath.lineTo(cx + cos(tipAng) * spikeOuterR, cy + sin(tipAng) * spikeOuterR)
      spikePath.lineTo(cx + cos(leadAng) * (discR * 0.90f), cy + sin(leadAng) * (discR * 0.90f))
      spikePath.close()
      c.drawPath(spikePath, spikeFill)
      c.drawPath(spikePath, spikeEdge)
    }

    // 2. Toasted golden-crust outer ring
    val crustPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#D97706")
      }
    val crustRimStroke =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = (radius * 0.08f).coerceAtLeast(1.3f)
        color = Color.parseColor("#F59E0B")
      }
    c.drawCircle(cx, cy, discR, crustPaint)
    c.drawCircle(cx, cy, discR, crustRimStroke)

    // 3. 8 pizza-cut wedge slices (alternating melted solar-cheese & tomato-crimson)
    val wedgeRect = RectF(cx - crustInnerR, cy - crustInnerR, cx + crustInnerR, cy + crustInnerR)
    val wedgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    val pepPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#B91C1C")
      }
    val cheeseDotPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#FEF08A")
      }
    val baseDeg = (spinAngleRad * 180f / PI.toFloat())
    for (slice in 0 until 8) {
      val startDeg = baseDeg + slice * 45f
      val isCheeseSlice = (slice % 2 == 0)
      wedgePaint.color =
        if (isCheeseSlice) Color.parseColor("#FACC15") else Color.parseColor("#EF4444")
      c.drawArc(wedgeRect, startDeg, 45f, true, wedgePaint)

      val midRad = ((startDeg + 22.5f) * PI / 180.0).toFloat()
      val dotR = crustInnerR * 0.56f
      val dx = cx + cos(midRad) * dotR
      val dy = cy + sin(midRad) * dotR
      c.drawCircle(
        dx,
        dy,
        (radius * 0.095f).coerceAtLeast(1.3f),
        if (isCheeseSlice) pepPaint else cheeseDotPaint,
      )
    }

    // 4. Crisp 8-piece radial slice cut lines + glowing plasma hub
    val cutPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = (radius * 0.065f).coerceAtLeast(1.15f)
        color = Color.parseColor("#0F172A")
      }
    for (slice in 0 until 8) {
      val a = spinAngleRad + slice * (PI.toFloat() * 0.25f)
      c.drawLine(cx, cy, cx + cos(a) * discR, cy + sin(a) * discR, cutPaint)
    }

    val hubGlow =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#FEF08A")
      }
    c.drawCircle(cx, cy, radius * 0.20f, cutPaint)
    c.drawCircle(cx, cy, radius * 0.17f, hubGlow)
  }

  fun bakeCustomBuffIconBitmap(entry: SpecialBuffRegistryEntry, sizePx: Int = 68): Bitmap {
    val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val c = Canvas(bmp)
    val cx = sizePx * 0.5f
    val cy = sizePx * 0.5f
    val r = sizePx * 0.36f
    val accent = entry.strokeColorInt
    val softCorner = CornerPathEffect(sizePx * 0.11f)

    val glowPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        shader =
          RadialGradient(
            cx,
            cy,
            sizePx * 0.46f,
            intArrayOf(
              Color.argb(145, Color.red(accent), Color.green(accent), Color.blue(accent)),
              Color.argb(54, Color.red(accent), Color.green(accent), Color.blue(accent)),
              Color.argb(0, Color.red(accent), Color.green(accent), Color.blue(accent)),
            ),
            floatArrayOf(0f, 0.60f, 1f),
            Shader.TileMode.CLAMP,
          )
      }
    c.drawCircle(cx, cy, sizePx * 0.46f, glowPaint)

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
        pathEffect = softCorner
        color = Color.argb(235, 10, 18, 32)
      }
    c.drawPath(hexPath, platePaint)

    val borderPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        pathEffect = softCorner
        strokeWidth = sizePx * 0.040f
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
        12 -> Color.parseColor("#34D399") // Toggleable Tractor Beam
        13 -> Color.parseColor("#FACC15") // Proximity-Lock Infinite Missile
        14 -> Color.parseColor("#A855F7") // Continuous Lance Beam
        15 -> Color.parseColor("#F59E0B") // Chakram (8-Slice Pizza-Colored Spiked Saw-Disc)
        -10 -> Color.parseColor("#38BDF8") // Speed Overcharge (+8% x3)
        -11 -> Color.parseColor("#F59E0B") // Thrust Overcharge (+8% x3)
        -12 -> Color.parseColor("#10B981") // Reactor Overcharge (+8% x3)
        -13 -> Color.parseColor("#EC4899") // Turn Radius Overcharge (+12% x3)
        -14 -> Color.parseColor("#00F5D4") // Max Flux Overcharge (+10% x3)
        else -> Color.parseColor("#38BDF8") // Overcharge Surge (+8% x3)
      }

    // 1. Calm radial powerup halo glow
    val glowPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        shader =
          RadialGradient(
            cx,
            cy,
            sizePx * 0.46f,
            intArrayOf(
              Color.argb(148, Color.red(accentColor), Color.green(accentColor), Color.blue(accentColor)),
              Color.argb(56, Color.red(accentColor), Color.green(accentColor), Color.blue(accentColor)),
              Color.argb(0, Color.red(accentColor), Color.green(accentColor), Color.blue(accentColor)),
            ),
            floatArrayOf(0f, 0.60f, 1f),
            Shader.TileMode.CLAMP,
          )
      }
    c.drawCircle(cx, cy, sizePx * 0.46f, glowPaint)

    // 2. Smooth rounded-bevel metallic capsule plate (zero sharp hazard corners)
    val badgeCornerEffect = CornerPathEffect(sizePx * 0.115f)
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
        pathEffect = badgeCornerEffect
        color = Color.argb(232, 8, 15, 28)
      }
    val rimPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        pathEffect = badgeCornerEffect
        strokeWidth = 2.4f
        color = accentColor
      }
    val innerRimPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.15f
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
        strokeWidth = 2.8f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        color = accentColor
      }
    val glyphCoreStroke =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.55f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        color = Color.WHITE
      }

    // 3. Item-specific high-contrast vector icon inside smooth beveled housing
    when (buffType) {
      0 -> {
        // NOVA RING: Smooth concentric 360-degree expanding Nova wave rings + 6 orbital plasma pearls (zero spiky hazard rays)
        c.drawCircle(cx, cy, r * 0.56f, glyphStrokePaint)
        c.drawCircle(cx, cy, r * 0.36f, glyphCoreStroke)
        c.drawCircle(cx, cy, r * 0.17f, glyphFillPaint)
        for (i in 0 until 6) {
          val a = (Math.PI / 3.0 * i).toFloat()
          val px = cx + kotlin.math.cos(a) * r * 0.56f
          val py = cy + kotlin.math.sin(a) * r * 0.56f
          c.drawCircle(px, py, 2.0f, glyphFillPaint)
        }
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
      12 -> {
        // TRACTOR BEAM: Gravimetric laser-web funnel + inward pull chevrons & captured orb
        c.drawLine(cx, cy + r * 0.50f, cx - r * 0.44f, cy - r * 0.34f, glyphStrokePaint)
        c.drawLine(cx, cy + r * 0.50f, cx + r * 0.44f, cy - r * 0.34f, glyphStrokePaint)
        val webRect1 = RectF(cx - r * 0.42f, cy - r * 0.44f, cx + r * 0.42f, cy + r * 0.14f)
        val webRect2 = RectF(cx - r * 0.28f, cy - r * 0.18f, cx + r * 0.28f, cy + r * 0.32f)
        c.drawArc(webRect1, 205f, 130f, false, glyphCoreStroke)
        c.drawArc(webRect2, 205f, 130f, false, glyphCoreStroke)
        c.drawCircle(cx, cy - r * 0.32f, r * 0.16f, glyphFillPaint)
        val pullChev =
          Path().apply {
            moveTo(cx - r * 0.16f, cy + r * 0.08f)
            lineTo(cx, cy + r * 0.28f)
            lineTo(cx + r * 0.16f, cy + r * 0.08f)
          }
        c.drawPath(pullChev, glyphCoreStroke)
      }
      13 -> {
        // PROXIMITY-LOCK INFINITE MISSILE: Segmented proximity influence ring + sleek warhead
        val proxRect = RectF(cx - r * 0.56f, cy - r * 0.56f, cx + r * 0.56f, cy + r * 0.56f)
        for (seg in 0 until 4) {
          c.drawArc(proxRect, seg * 90f + 14f, 62f, false, glyphStrokePaint)
        }
        c.drawCircle(cx, cy, r * 0.36f, innerRimPaint)
        val warhead =
          Path().apply {
            moveTo(cx, cy - r * 0.32f)
            lineTo(cx + r * 0.19f, cy + r * 0.24f)
            lineTo(cx, cy + r * 0.12f)
            lineTo(cx - r * 0.19f, cy + r * 0.24f)
            close()
          }
        c.drawPath(warhead, glyphFillPaint)
      }
      14 -> {
        // CONTINUOUS LANCE BEAM: Focused non-penetrating plasma column + emitter & first-hit impact arc
        val emitterY = cy + r * 0.46f
        val hitY = cy - r * 0.38f
        val wideBeamPaint =
          Paint(glyphStrokePaint).apply {
            strokeWidth = 5.4f
            color = Color.argb(165, Color.red(accentColor), Color.green(accentColor), Color.blue(accentColor))
          }
        c.drawLine(cx, emitterY, cx, hitY, wideBeamPaint)
        c.drawLine(cx, emitterY, cx, hitY, glyphStrokePaint)
        c.drawLine(cx, emitterY, cx, hitY, glyphCoreStroke)
        val stopBarRect = RectF(cx - r * 0.42f, hitY - r * 0.16f, cx + r * 0.42f, hitY + r * 0.26f)
        c.drawArc(stopBarRect, 200f, 140f, false, glyphCoreStroke)
        c.drawCircle(cx, hitY, r * 0.15f, glyphFillPaint)
        c.drawCircle(cx, emitterY, r * 0.13f, glyphFillPaint)
      }
      15 -> {
        // CHAKRAM: 8-slice pizza-colored spiked saw-disc
        drawChakramDiscOnCanvas(c, cx, cy, r * 0.62f, spinAngleRad = 0.22f)
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

  /**
   * High-Definition Circular 2.5D Metallic-Collared Playfield Rift Aesthetic:
   * - Layer 1: Smooth circular plasma corona + deep `#02050D` / `#050C1E` circular event-horizon well.
   * - Layer 2: 6 smooth cubic-Bezier logarithmic plasma ribbons spiraling cleanly inside the circular
   *   well (`outerColor` -> `midColor` -> luminous `coreColor`) + concentric harmonic pulse wave.
   * - Layer 3: Precision circular 5-stop brushed gunmetal/titanium containment collar (`0.88r..0.99r`)
   *   with `#020617` silhouette outlines, sunlit top-left specular bevels, 24 calibrated degree ticks,
   *   and 6 illuminated emitter nodes matching ships and `Nexus Station`.
   * - Layer 4: Pitch-black circular singularity throat (`darkVoidCenter`) or radiant photon core.
   */
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
    if (r <= 1.2f || fadeAlpha <= 0.01f) return
    val oR = Color.red(outerColor)
    val oG = Color.green(outerColor)
    val oB = Color.blue(outerColor)
    val mR = Color.red(midColor)
    val mG = Color.green(midColor)
    val mB = Color.blue(midColor)
    val cR = Color.red(coreColor)
    val cG = Color.green(coreColor)
    val cB = Color.blue(coreColor)

    val fillPaint = sharedRiftFillPaint
    val ringPaint = sharedRiftStrokePaint
    val ribbonPath = sharedRiftRibbonPath
    val spinePath = sharedRiftSpinePath
    val arcRect = sharedRiftArcRect
    fillPaint.shader = null
    ringPaint.shader = null

    val pulsePhase = pulsePhaseOverride ?: ((System.currentTimeMillis() % 1800L) / 1800f)
    val twoPi = PI.toFloat() * 2f
    val breath = sin(pulsePhase * twoPi)

    // Layer 1: Smooth Circular Plasma Aura & Deep Event-Horizon Well
    fillPaint.color = Color.argb((30 * fadeAlpha).roundToInt().coerceIn(0, 255), oR, oG, oB)
    canvas.drawCircle(cx, cy, r * (1.20f + 0.025f * breath), fillPaint)
    fillPaint.color = Color.argb((62 * fadeAlpha).roundToInt().coerceIn(0, 255), mR, mG, mB)
    canvas.drawCircle(cx, cy, r * (1.05f + 0.015f * breath), fillPaint)

    val wellR = r * 0.89f
    fillPaint.color = Color.argb((248 * fadeAlpha).roundToInt().coerceIn(0, 255), 2, 5, 13)
    canvas.drawCircle(cx, cy, wellR, fillPaint)
    fillPaint.color = Color.argb((192 * fadeAlpha).roundToInt().coerceIn(0, 255), 5, 12, 30)
    canvas.drawCircle(cx, cy, r * 0.74f, fillPaint)

    // Subtle concentric depth rings inside the circular funnel
    ringPaint.strokeWidth = (r * 0.016f).coerceAtLeast(1.0f)
    ringPaint.color = Color.argb((58 * fadeAlpha).roundToInt().coerceIn(0, 255), oR, oG, oB)
    canvas.drawCircle(cx, cy, r * 0.68f, ringPaint)
    canvas.drawCircle(cx, cy, r * 0.46f, ringPaint)

    // Layer 2: 6 Smooth Logarithmic Plasma Vortex Ribbons
    val twistSign = if (inwardSpiral) -1f else 1f
    val rOuterBlade = r * 0.86f
    val rMidBladeA = r * 0.63f
    val rMidBladeB = r * 0.40f
    val rInnerThroat = r * 0.20f
    val stepRad = twoPi / 6f

    for (arm in 0 until 6) {
      val baseA = spinAngle + arm * stepRad
      val aOutLead = baseA
      val aOutTrail = aOutLead - twistSign * 0.50f
      val aMidALead = baseA + twistSign * 0.48f
      val aMidATrail = aMidALead - twistSign * 0.38f
      val aMidBLead = baseA + twistSign * 0.98f
      val aMidBTrail = aMidBLead - twistSign * 0.26f
      val aInLead = baseA + twistSign * 1.46f
      val aInTrail = aInLead - twistSign * 0.14f

      val oxL = cx + cos(aOutLead) * rOuterBlade
      val oyL = cy + sin(aOutLead) * rOuterBlade
      val mAxL = cx + cos(aMidALead) * rMidBladeA
      val mAyL = cy + sin(aMidALead) * rMidBladeA
      val mBxL = cx + cos(aMidBLead) * rMidBladeB
      val mByL = cy + sin(aMidBLead) * rMidBladeB
      val ixL = cx + cos(aInLead) * rInnerThroat
      val iyL = cy + sin(aInLead) * rInnerThroat

      val ixT = cx + cos(aInTrail) * rInnerThroat
      val iyT = cy + sin(aInTrail) * rInnerThroat
      val mBxT = cx + cos(aMidBTrail) * rMidBladeB
      val mByT = cy + sin(aMidBTrail) * rMidBladeB
      val mAxT = cx + cos(aMidATrail) * rMidBladeA
      val mAyT = cy + sin(aMidATrail) * rMidBladeA
      val oxT = cx + cos(aOutTrail) * rOuterBlade
      val oyT = cy + sin(aOutTrail) * rOuterBlade

      ribbonPath.reset()
      ribbonPath.moveTo(oxL, oyL)
      ribbonPath.cubicTo(mAxL, mAyL, mBxL, mByL, ixL, iyL)
      ribbonPath.lineTo(ixT, iyT)
      ribbonPath.cubicTo(mBxT, mByT, mAxT, mAyT, oxT, oyT)
      ribbonPath.close()

      fillPaint.color = Color.argb((148 * fadeAlpha).roundToInt().coerceIn(0, 255), oR, oG, oB)
      canvas.drawPath(ribbonPath, fillPaint)

      val aOutMid = aOutLead - twistSign * 0.26f
      val aMidAMid = aMidALead - twistSign * 0.20f
      val aMidBMid = aMidBLead - twistSign * 0.14f
      val aInMid = aInLead - twistSign * 0.08f
      ribbonPath.reset()
      ribbonPath.moveTo(oxL, oyL)
      ribbonPath.cubicTo(mAxL, mAyL, mBxL, mByL, ixL, iyL)
      ribbonPath.lineTo(cx + cos(aInMid) * rInnerThroat, cy + sin(aInMid) * rInnerThroat)
      ribbonPath.cubicTo(
        cx + cos(aMidBMid) * rMidBladeB,
        cy + sin(aMidBMid) * rMidBladeB,
        cx + cos(aMidAMid) * rMidBladeA,
        cy + sin(aMidAMid) * rMidBladeA,
        cx + cos(aOutMid) * rOuterBlade,
        cy + sin(aOutMid) * rOuterBlade,
      )
      ribbonPath.close()
      fillPaint.color = Color.argb((212 * fadeAlpha).roundToInt().coerceIn(0, 255), mR, mG, mB)
      canvas.drawPath(ribbonPath, fillPaint)

      spinePath.reset()
      spinePath.moveTo(oxL, oyL)
      spinePath.cubicTo(mAxL, mAyL, mBxL, mByL, ixL, iyL)
      ringPaint.strokeWidth = (r * 0.025f).coerceAtLeast(1.1f)
      ringPaint.color = Color.argb((238 * fadeAlpha).roundToInt().coerceIn(0, 255), cR, cG, cB)
      canvas.drawPath(spinePath, ringPaint)
    }

    // Concentric radial harmonic pulse ring
    val waveFrac = if (inwardSpiral) (1f - pulsePhase) else pulsePhase
    val waveR = r * (0.24f + waveFrac * 0.58f)
    ringPaint.strokeWidth = (r * 0.022f).coerceAtLeast(1.0f)
    ringPaint.color =
      Color.argb(
        ((1f - kotlin.math.abs(waveFrac - 0.5f) * 1.65f).coerceIn(0.10f, 1f) * 128f * fadeAlpha)
          .roundToInt()
          .coerceIn(0, 255),
        cR,
        cG,
        cB,
      )
    canvas.drawCircle(cx, cy, waveR, ringPaint)

    // Layer 3: Precision Circular 5-Stop Metallic Containment Collar (0.88r..0.99r)
    val collarInnerR = r * 0.88f
    val collarOuterR = r * 0.99f
    val collarMidR = (collarInnerR + collarOuterR) * 0.5f
    val collarThickness = (collarOuterR - collarInnerR).coerceAtLeast(2.2f)

    // Subtle inner plasma rim glow beneath the metallic collar lip
    ringPaint.strokeWidth = (r * 0.036f).coerceAtLeast(1.4f)
    ringPaint.color = Color.argb((185 * fadeAlpha).roundToInt().coerceIn(0, 255), mR, mG, mB)
    canvas.drawCircle(cx, cy, collarInnerR, ringPaint)

    val a255 = (255 * fadeAlpha).roundToInt().coerceIn(0, 255)
    val stop0 = blendStationRgb(Color.rgb(15, 23, 42), outerColor, 0.24f, alpha = a255)
    val stop1 = blendStationRgb(Color.rgb(51, 65, 85), midColor, 0.18f, alpha = a255)
    val stop2 = blendStationRgb(Color.rgb(203, 213, 225), coreColor, 0.22f, alpha = a255)
    val stop3 = blendStationRgb(Color.rgb(71, 85, 105), outerColor, 0.20f, alpha = a255)
    val stop4 = blendStationRgb(Color.rgb(11, 18, 32), outerColor, 0.28f, alpha = a255)

    ringPaint.strokeWidth = collarThickness
    ringPaint.shader =
      LinearGradient(
        cx - collarOuterR,
        cy - collarOuterR,
        cx + collarOuterR,
        cy + collarOuterR,
        intArrayOf(stop0, stop1, stop2, stop3, stop4),
        floatArrayOf(0f, 0.28f, 0.48f, 0.74f, 1f),
        Shader.TileMode.CLAMP,
      )
    canvas.drawCircle(cx, cy, collarMidR, ringPaint)
    ringPaint.shader = null

    // Crisp dark readability silhouette outlines on inner and outer collar edges
    ringPaint.strokeWidth = (r * 0.018f).coerceAtLeast(1.0f)
    ringPaint.color = Color.argb((235 * fadeAlpha).roundToInt().coerceIn(0, 255), 2, 6, 23)
    canvas.drawCircle(cx, cy, collarOuterR, ringPaint)
    canvas.drawCircle(cx, cy, collarInnerR, ringPaint)

    // Sunlit top-left specular bevel arc on the metallic collar
    arcRect.set(cx - collarOuterR, cy - collarOuterR, cx + collarOuterR, cy + collarOuterR)
    ringPaint.strokeWidth = (r * 0.016f).coerceAtLeast(0.95f)
    ringPaint.color = Color.argb((205 * fadeAlpha).roundToInt().coerceIn(0, 255), 248, 250, 252)
    canvas.drawArc(arcRect, 196f, 98f, false, ringPaint)

    // 24 Calibrated Telemetry Degree Ticks + 6 Illuminated Emitter Nodes Around the Circular Collar
    val tickCount = 24
    ringPaint.strokeWidth = (r * 0.015f).coerceAtLeast(0.9f)
    for (t in 0 until tickCount) {
      val ta = t * (twoPi / tickCount.toFloat()) + spinAngle * 0.18f
      val ca = cos(ta)
      val sa = sin(ta)
      val major = (t % 4 == 0)
      val tIn = if (major) collarInnerR + collarThickness * 0.14f else collarInnerR + collarThickness * 0.28f
      val tOut = collarOuterR - collarThickness * 0.14f
      ringPaint.color =
        if (major) {
          Color.argb((235 * fadeAlpha).roundToInt().coerceIn(0, 255), cR, cG, cB)
        } else {
          Color.argb((145 * fadeAlpha).roundToInt().coerceIn(0, 255), mR, mG, mB)
        }
      canvas.drawLine(cx + ca * tIn, cy + sa * tIn, cx + ca * tOut, cy + sa * tOut, ringPaint)
    }

    for (nIdx in 0 until 6) {
      val na = nIdx * (twoPi / 6f) - spinAngle * 0.25f
      val nx = cx + cos(na) * collarMidR
      val ny = cy + sin(na) * collarMidR
      fillPaint.color = Color.argb((215 * fadeAlpha).roundToInt().coerceIn(0, 255), 9, 14, 28)
      canvas.drawCircle(nx, ny, (collarThickness * 0.36f).coerceAtLeast(1.4f), fillPaint)
      fillPaint.color = Color.argb((245 * fadeAlpha).roundToInt().coerceIn(0, 255), cR, cG, cB)
      canvas.drawCircle(nx, ny, (collarThickness * 0.20f).coerceAtLeast(0.9f), fillPaint)
    }

    // Layer 4: Central Circular Singularity Throat (r * 0.22f) or Photon Rift Core
    val throatR = r * 0.22f
    if (darkVoidCenter) {
      ringPaint.strokeWidth = (r * 0.044f).coerceAtLeast(1.6f)
      ringPaint.color = Color.argb((220 * fadeAlpha).roundToInt().coerceIn(0, 255), oR, oG, oB)
      canvas.drawCircle(cx, cy, throatR * 1.14f, ringPaint)

      fillPaint.color = Color.argb((254 * fadeAlpha).roundToInt().coerceIn(0, 255), 0, 0, 0)
      canvas.drawCircle(cx, cy, throatR, fillPaint)

      ringPaint.strokeWidth = (r * 0.022f).coerceAtLeast(1.05f)
      ringPaint.color = Color.argb((242 * fadeAlpha).roundToInt().coerceIn(0, 255), cR, cG, cB)
      arcRect.set(cx - throatR, cy - throatR, cx + throatR, cy + throatR)
      val degBase = (spinAngle * 180f / PI.toFloat())
      canvas.drawArc(arcRect, degBase, 132f, false, ringPaint)
      canvas.drawArc(arcRect, degBase + 180f, 118f, false, ringPaint)
    } else {
      fillPaint.color = Color.argb((205 * fadeAlpha).roundToInt().coerceIn(0, 255), oR, oG, oB)
      canvas.drawCircle(cx, cy, throatR * 1.26f, fillPaint)
      fillPaint.color = Color.argb((238 * fadeAlpha).roundToInt().coerceIn(0, 255), mR, mG, mB)
      canvas.drawCircle(cx, cy, throatR * 0.94f, fillPaint)
      fillPaint.color = Color.argb((252 * fadeAlpha).roundToInt().coerceIn(0, 255), cR, cG, cB)
      canvas.drawCircle(cx, cy, throatR * 0.56f, fillPaint)
      fillPaint.color = Color.argb((255 * fadeAlpha).roundToInt().coerceIn(0, 255), 255, 255, 255)
      canvas.drawCircle(cx - throatR * 0.12f, cy - throatR * 0.12f, throatR * 0.25f, fillPaint)
    }
  }

  /**
   * Renders the converged 10.0s multicolored `Unstable Rift` as a smooth circular 2.5D
   * dual-collared singularity vortex with counter-revolving harmonic plasma rings.
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
    val calmPrimarySpin = spinAngle * 0.42f
    val calmCounterSpin = -spinAngle * 0.30f

    draw3dPlayfieldRiftColors(
      canvas = canvas,
      cx = cx,
      cy = cy,
      r = r,
      spinAngle = calmPrimarySpin,
      fadeAlpha = fadeAlpha,
      outerColor = Color.rgb(250, 204, 21),
      midColor = Color.rgb(239, 68, 68),
      coreColor = Color.rgb(254, 249, 195),
      darkVoidCenter = true,
      inwardSpiral = true,
      pulsePhaseOverride = pulsePhase,
    )
    draw3dPlayfieldRiftColors(
      canvas = canvas,
      cx = cx,
      cy = cy,
      r = r * 0.74f,
      spinAngle = calmCounterSpin,
      fadeAlpha = fadeAlpha * 0.82f,
      outerColor = Color.rgb(0, 245, 212),
      midColor = Color.rgb(168, 85, 247),
      coreColor = Color.rgb(224, 242, 254),
      darkVoidCenter = true,
      inwardSpiral = true,
      pulsePhaseOverride = (1f - pulsePhase).coerceIn(0f, 1f),
    )

    val arcPaint = sharedRiftStrokePaint
    val arcRect = sharedRiftArcRect
    arcPaint.shader = null
    val outerOrbitR = r * 1.05f
    arcRect.set(cx - outerOrbitR, cy - outerOrbitR, cx + outerOrbitR, cy + outerOrbitR)
    val baseDeg = (calmPrimarySpin * 180f / PI.toFloat())
    val palette =
      intArrayOf(
        Color.argb((195 * fadeAlpha).roundToInt().coerceIn(0, 255), 250, 204, 21),
        Color.argb((195 * fadeAlpha).roundToInt().coerceIn(0, 255), 0, 245, 212),
        Color.argb((195 * fadeAlpha).roundToInt().coerceIn(0, 255), 244, 63, 94),
        Color.argb((195 * fadeAlpha).roundToInt().coerceIn(0, 255), 192, 132, 252),
      )
    arcPaint.strokeWidth = (r * 0.026f).coerceAtLeast(1.3f)
    for (i in 0 until 8) {
      arcPaint.color = palette[i % palette.size]
      canvas.drawArc(arcRect, baseDeg + i * 45f, 24f, false, arcPaint)
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
        drawUnstableMergedRiftOnCanvas(c, cx, cy, r * 1.05f, spinAngle = 0.38f, pulsePhase = 0.48f)
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

  private fun blendStationRgb(c0: Int, c1: Int, t: Float, alpha: Int = 255): Int {
    val f = t.coerceIn(0f, 1f)
    val r = (Color.red(c0) + (Color.red(c1) - Color.red(c0)) * f).roundToInt().coerceIn(0, 255)
    val g = (Color.green(c0) + (Color.green(c1) - Color.green(c0)) * f).roundToInt().coerceIn(0, 255)
    val b = (Color.blue(c0) + (Color.blue(c1) - Color.blue(c0)) * f).roundToInt().coerceIn(0, 255)
    return Color.argb(alpha.coerceIn(0, 255), r, g, b)
  }

  /**
   * Bakes the 5x Ship-Size (`180px` world radius / `360px` diameter) `Nexus Station`
   * Metallic Safe-Zone Station Sprite using the exact 5-stop steel/titanium armor shading,
   * dark readability outlines, sunlit specular bevels, and lit viewport craft of `drawMetallicShipOnCanvas`,
   * dynamically driven by `ForgeElementTarget.SAFE_ZONE_PAD` in `STAR FORGE`.
   */
  fun bakeSafeZoneNexusStationBitmap(sizePx: Int = 256): Bitmap {
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
    val stopDark0 = blendStationRgb(Color.rgb(15, 23, 42), prim, 0.36f)
    val stopMid1 = blendStationRgb(Color.rgb(51, 65, 85), prim, 0.24f)
    val stopSpec2 =
      blendStationRgb(
        Color.rgb(203, 213, 225),
        blendStationRgb(Color.WHITE, trim, 0.22f),
        (0.32f * rimDepth).coerceIn(0.10f, 0.55f),
      )
    val stopSlope3 = blendStationRgb(Color.rgb(71, 85, 105), prim, 0.26f)
    val stopShadow4 = blendStationRgb(Color.rgb(12, 19, 34), prim, 0.42f)

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
              blendStationRgb(Color.rgb(30, 41, 59), trim, 0.16f),
              blendStationRgb(Color.rgb(226, 232, 240), Color.WHITE, 0.35f),
              blendStationRgb(Color.rgb(51, 65, 85), prim, 0.28f),
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
              blendStationRgb(Color.rgb(148, 163, 184), trim, 0.35f, alpha = 125),
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
        color = blendStationRgb(trim, Color.WHITE, 0.14f, alpha = 225)
      }

    val conduitPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = (sz / 128f) * 1.15f
        strokeCap = Paint.Cap.ROUND
        color = blendStationRgb(glow, Color.WHITE, 0.20f, alpha = (185 * glowInt).roundToInt().coerceIn(70, 245))
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
      beaconPaint.color = blendStationRgb(portCol, Color.WHITE, 0.25f, alpha = 235)
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
          wIdx % 2 == 0 -> blendStationRgb(glow, Color.WHITE, 0.35f)
          else -> blendStationRgb(trim, Color.WHITE, 0.25f)
        }
      c.drawCircle(wx, wy, r * 0.012f, beaconPaint)
    }

    // 4. 6 Swept Nexus Station Curved Docking Pylons (3 Upper-Swept + 3 Lower-Swept Symmetric Pylons)
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
      beaconPaint.color = blendStationRgb(clampColor, Color.WHITE, 0.15f, alpha = 95)
      c.drawCircle(clampX, clampY, r * 0.044f, beaconPaint)
      beaconPaint.color = blendStationRgb(clampColor, Color.WHITE, 0.30f, alpha = 240)
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
              blendStationRgb(glow, Color.WHITE, 0.35f),
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

  /**
   * Draws the crisp 3D-tilted revolving cyan/gold holographic ring orbiting at
   * `stationRadius * 0.34f` around the central Ops spire of the `5x` `Nexus Station`
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

    stationHoloArcRect.set(-holoR, -holoR, holoR, holoR)

    // 1. Soft translucent holographic carrier band
    stationHoloBandPaint.strokeWidth = 6.2f * unitStroke
    stationHoloBandPaint.color =
      Color.argb(
        (58 * op).roundToInt().coerceIn(0, 255),
        Color.red(primaryHolo),
        Color.green(primaryHolo),
        Color.blue(primaryHolo),
      )
    c.drawOval(stationHoloArcRect, stationHoloBandPaint)

    // 2. Revolving primary cyan/crimson segmented holographic arcs
    stationHoloBandPaint.strokeWidth = 2.4f * unitStroke
    stationHoloBandPaint.color =
      Color.argb(
        (225 * op).roundToInt().coerceIn(0, 255),
        Color.red(primaryHolo),
        Color.green(primaryHolo),
        Color.blue(primaryHolo),
      )
    for (seg in 0 until 4) {
      c.drawArc(stationHoloArcRect, spinDeg + seg * 90f, 54f, false, stationHoloBandPaint)
    }

    // 3. Counter-revolving inner gold/amber harmonic ring
    val innerR = holoR * 0.86f
    stationHoloArcRect.set(-innerR, -innerR, innerR, innerR)
    stationHoloBandPaint.strokeWidth = 1.6f * unitStroke
    stationHoloBandPaint.color =
      Color.argb(
        (205 * op).roundToInt().coerceIn(0, 255),
        Color.red(secondaryHolo),
        Color.green(secondaryHolo),
        Color.blue(secondaryHolo),
      )
    for (seg in 0 until 6) {
      c.drawArc(stationHoloArcRect, counterDeg + seg * 60f, 28f, false, stationHoloBandPaint)
    }

    // 4. 18 Precision Holographic Data Telemetry Ticks
    stationHoloBandPaint.strokeWidth = 1.35f * unitStroke
    val tickBaseRad = (spinDeg * PI.toFloat()) / 180f
    for (t in 0 until 18) {
      val ta = tickBaseRad + t * (2f * PI.toFloat() / 18f)
      val ca = cos(ta)
      val sa = sin(ta)
      val r0 = holoR * 0.93f
      val r1 = holoR * (if (t % 3 == 0) 1.10f else 1.04f)
      val col = if (t % 3 == 0) secondaryHolo else trimHolo
      stationHoloBandPaint.color =
        Color.argb(
          (215 * op).roundToInt().coerceIn(0, 255),
          Color.red(col),
          Color.green(col),
          Color.blue(col),
        )
      c.drawLine(ca * r0, sa * r0, ca * r1, sa * r1, stationHoloBandPaint)
    }

    // 5. Traveling White-Hot Scanline Sheen Crest
    stationHoloArcRect.set(-holoR, -holoR, holoR, holoR)
    stationHoloBandPaint.strokeWidth = 2.8f * unitStroke
    stationHoloBandPaint.color = Color.argb((235 * op).roundToInt().coerceIn(0, 255), 255, 255, 255)
    c.drawArc(stationHoloArcRect, (spinDeg * 1.6f) % 360f, 22f, false, stationHoloBandPaint)

    c.restore()
  }

  /**
   * Draws the 1:1 `STAR FORGE` Central Multicolored Shifting **Dyson Sun** (`stationRadius` = `180px`
   * matching the `Nexus Station` footprint), encircled by rotating 5-stop metallic Dyson harvester
   * rings and orbiting holographic letters reading `"STAR FORGE"`.
   */
  fun drawDysonSunStarForgeCenterpiece(
    c: Canvas,
    cx: Float,
    cy: Float,
    stationRadius: Float,
    timeSec: Float,
    isSelected: Boolean = false,
  ) {
    if (stationRadius <= 6f) return
    val fill = sharedRiftFillPaint
    val stroke = sharedRiftStrokePaint
    val arcRect = stationHoloArcRect
    fill.shader = null
    stroke.shader = null

    val palette =
      intArrayOf(
        Color.rgb(0, 245, 212),
        Color.rgb(250, 204, 21),
        Color.rgb(192, 132, 252),
        Color.rgb(244, 63, 94),
        Color.rgb(56, 189, 248),
      )
    val cycle = (timeSec * 0.42f) % palette.size.toFloat()
    val idx0 = cycle.toInt().coerceIn(0, palette.size - 1)
    val idx1 = (idx0 + 1) % palette.size
    val idx2 = (idx0 + 2) % palette.size
    val frac = cycle - idx0.toFloat()
    val sunPrimary = blendStationRgb(palette[idx0], palette[idx1], frac)
    val sunSecondary = blendStationRgb(palette[idx1], palette[idx2], frac)
    val sunCore = blendStationRgb(sunPrimary, Color.WHITE, 0.72f)

    val twoPi = PI.toFloat() * 2f
    val pulse = 0.5f + 0.5f * sin(timeSec * 2.4f)
    val coreR = stationRadius * 0.56f

    // 1. Outer Multicolored Stellar Corona & Prominence Flares
    fill.shader =
      RadialGradient(
        cx,
        cy,
        stationRadius * (1.26f + 0.05f * pulse),
        intArrayOf(
          blendStationRgb(sunCore, Color.WHITE, 0.45f, alpha = 220),
          blendStationRgb(sunPrimary, sunSecondary, 0.35f, alpha = 155),
          blendStationRgb(sunSecondary, Color.rgb(15, 23, 42), 0.25f, alpha = 64),
          Color.argb(0, 2, 6, 23),
        ),
        floatArrayOf(0f, 0.42f, 0.78f, 1f),
        Shader.TileMode.CLAMP,
      )
    c.drawCircle(cx, cy, stationRadius * (1.26f + 0.05f * pulse), fill)
    fill.shader = null

    // 8 Solar Prominence Loops erupting around the stellar limb
    stroke.strokeWidth = (stationRadius * 0.026f).coerceAtLeast(1.6f)
    for (p in 0 until 8) {
      val pa = timeSec * 0.38f + p * (twoPi / 8f)
      val flareR = coreR * (1.10f + 0.10f * sin(timeSec * 2.1f + p * 1.4f))
      arcRect.set(cx - flareR, cy - flareR, cx + flareR, cy + flareR)
      val flareCol = if (p % 2 == 0) sunPrimary else sunSecondary
      stroke.color = Color.argb(185, Color.red(flareCol), Color.green(flareCol), Color.blue(flareCol))
      c.drawArc(arcRect, (pa * 180f / PI.toFloat()), 26f, false, stroke)
    }

    // 2. Convecting Multicolored Dyson Star Sphere
    fill.shader =
      RadialGradient(
        cx - coreR * 0.20f,
        cy - coreR * 0.20f,
        coreR * 1.12f,
        intArrayOf(
          Color.WHITE,
          sunCore,
          sunPrimary,
          sunSecondary,
          blendStationRgb(sunSecondary, Color.rgb(15, 23, 42), 0.55f),
        ),
        floatArrayOf(0f, 0.24f, 0.58f, 0.85f, 1f),
        Shader.TileMode.CLAMP,
      )
    c.drawCircle(cx, cy, coreR, fill)
    fill.shader = null

    // Swirling solar convection filaments across the stellar photosphere
    stroke.strokeWidth = (stationRadius * 0.018f).coerceAtLeast(1.2f)
    arcRect.set(cx - coreR * 0.76f, cy - coreR * 0.76f, cx + coreR * 0.76f, cy + coreR * 0.76f)
    stroke.color = Color.argb(165, 255, 255, 255)
    for (s in 0 until 5) {
      c.drawArc(arcRect, (timeSec * 36f + s * 72f) % 360f, 38f, false, stroke)
    }

    // 3. Inner Tilted 5-Stop Metallic Gimbaled Dyson Harvester Ring
    val innerDysonR = stationRadius * 0.78f
    c.save()
    c.translate(cx, cy)
    c.rotate((timeSec * 22f) % 360f)
    c.scale(1.0f, 0.64f)
    stroke.strokeWidth = (stationRadius * 0.068f).coerceAtLeast(3.2f)
    stroke.shader =
      LinearGradient(
        -innerDysonR,
        -innerDysonR,
        innerDysonR,
        innerDysonR,
        intArrayOf(
          Color.rgb(15, 23, 42),
          Color.rgb(71, 85, 105),
          Color.rgb(226, 232, 240),
          Color.rgb(51, 65, 85),
          Color.rgb(11, 18, 32),
        ),
        floatArrayOf(0f, 0.28f, 0.50f, 0.74f, 1f),
        Shader.TileMode.CLAMP,
      )
    c.drawCircle(0f, 0f, innerDysonR, stroke)
    stroke.shader = null
    stroke.strokeWidth = (stationRadius * 0.016f).coerceAtLeast(1.0f)
    stroke.color = Color.argb(230, 2, 6, 23)
    c.drawCircle(0f, 0f, innerDysonR + stationRadius * 0.034f, stroke)
    c.drawCircle(0f, 0f, innerDysonR - stationRadius * 0.034f, stroke)
    stroke.color = sunPrimary
    for (v in 0 until 8) {
      val va = v * (twoPi / 8f)
      c.drawLine(
        cos(va) * (innerDysonR - stationRadius * 0.03f),
        sin(va) * (innerDysonR - stationRadius * 0.03f),
        cos(va) * (innerDysonR + stationRadius * 0.03f),
        sin(va) * (innerDysonR + stationRadius * 0.03f),
        stroke,
      )
    }
    c.restore()

    // 4. Outer Primary 5-Stop Metallic Dyson Megastructure Ring (`0.94f * stationRadius`)
    val outerDysonR = stationRadius * 0.94f
    val bandW = (stationRadius * 0.088f).coerceAtLeast(4.0f)
    stroke.strokeWidth = bandW
    stroke.shader =
      LinearGradient(
        cx - outerDysonR,
        cy - outerDysonR,
        cx + outerDysonR,
        cy + outerDysonR,
        intArrayOf(
          blendStationRgb(Color.rgb(15, 23, 42), sunPrimary, 0.20f),
          Color.rgb(51, 65, 85),
          Color.rgb(226, 232, 240),
          blendStationRgb(Color.rgb(71, 85, 105), sunSecondary, 0.20f),
          Color.rgb(11, 18, 32),
        ),
        floatArrayOf(0f, 0.28f, 0.48f, 0.74f, 1f),
        Shader.TileMode.CLAMP,
      )
    c.drawCircle(cx, cy, outerDysonR, stroke)
    stroke.shader = null

    stroke.strokeWidth = (stationRadius * 0.018f).coerceAtLeast(1.1f)
    stroke.color = Color.argb(235, 2, 6, 23)
    c.drawCircle(cx, cy, outerDysonR + bandW * 0.5f, stroke)
    c.drawCircle(cx, cy, outerDysonR - bandW * 0.5f, stroke)

    // 12 Dyson Collector Pylons & Glowing Energy Conduits around the Megastructure Ring
    for (node in 0 until 12) {
      val na = -timeSec * 0.28f + node * (twoPi / 12f)
      val ca = cos(na)
      val sa = sin(na)
      val rIn = outerDysonR - bandW * 0.62f
      val rOut = outerDysonR + bandW * 0.46f
      stroke.strokeWidth = (stationRadius * 0.024f).coerceAtLeast(1.3f)
      stroke.color = Color.rgb(30, 41, 59)
      c.drawLine(cx + ca * rIn, cy + sa * rIn, cx + ca * rOut, cy + sa * rOut, stroke)
      fill.color = if (node % 2 == 0) sunPrimary else sunSecondary
      c.drawCircle(cx + ca * outerDysonR, cy + sa * outerDysonR, (bandW * 0.22f).coerceAtLeast(1.5f), fill)
    }

    // 5. Orbiting Holographic Ring Emitting "STAR FORGE" Letters
    val holoOrbitR = stationRadius * 1.13f
    arcRect.set(cx - holoOrbitR, cy - holoOrbitR, cx + holoOrbitR, cy + holoOrbitR)
    stroke.strokeWidth = (stationRadius * 0.020f).coerceAtLeast(1.2f)
    stroke.color = Color.argb(135, Color.red(sunPrimary), Color.green(sunPrimary), Color.blue(sunPrimary))
    c.drawCircle(cx, cy, holoOrbitR, stroke)

    if (isSelected) {
      stroke.strokeWidth = (stationRadius * 0.028f).coerceAtLeast(1.8f)
      stroke.color = Color.rgb(250, 204, 21)
      c.drawCircle(cx, cy, stationRadius * 1.24f, stroke)
    }

    val title = "STAR FORGE"
    val letterSize = (stationRadius * 0.135f).coerceIn(9f, 32f)
    dysonLetterPaint.textSize = letterSize
    val baseArcDeg = -90f + sin(timeSec * 0.85f) * 8f
    val spanDeg = 108f
    val startDeg = baseArcDeg - spanDeg * 0.5f
    val stepDeg = spanDeg / (title.length - 1).coerceAtLeast(1).toFloat()

    for (i in title.indices) {
      val ch = title[i]
      if (ch == ' ') continue
      val deg = startDeg + i * stepDeg
      val rad = (deg * PI.toFloat()) / 180f
      val lx = cx + cos(rad) * holoOrbitR
      val ly = cy + sin(rad) * holoOrbitR
      c.save()
      c.translate(lx, ly)
      c.rotate(deg + 90f)
      dysonLetterPaint.color = Color.argb(195, 2, 6, 23)
      c.drawText(ch.toString(), 1.2f, 1.2f, dysonLetterPaint)
      dysonLetterPaint.color = if (i < 4) Color.rgb(0, 245, 212) else Color.rgb(250, 204, 21)
      c.drawText(ch.toString(), 0f, 0f, dysonLetterPaint)
      c.restore()
    }
  }

  /**
   * Returns a cached `192x192` 5-stop metallic `NEXUS`-station-styled Hub module emblem bitmap
   * keyed by module, equipped hull, trim style, visual forge revision, and Syndicate crest recipe.
   */
  fun getNexusHubModuleEmblemBitmap(
    moduleKey: String,
    activeShipId: Int = 1,
    crestRecipe: SyndicateCrestRecipe = RiftVisualForgeState.activeSyndicateCrestRecipe,
    syndicateTag: String = "",
    sizePx: Int = 192,
  ): Bitmap {
    val cleanKey = moduleKey.trim().uppercase()
    val cleanShip = activeShipId.coerceIn(1, 10)
    val cleanSize = sizePx.coerceIn(96, 384)
    val cleanTag = syndicateTag.trim().take(4)
    val cacheKey =
      "$cleanKey:$cleanShip:$spriteVisualRevision:$customShip9Version:$activeHullTrimStyle:${crestRecipe.hashCode()}:$cleanTag:$cleanSize"
    synchronized(cacheLock) {
      nexusHubModuleEmblemCache[cacheKey]?.let { cached ->
        if (!cached.isRecycled) return cached
      }
    }
    val baked =
      bakeNexusHubModuleEmblem(
        moduleKey = cleanKey,
        activeShipId = cleanShip,
        crestRecipe = crestRecipe,
        syndicateTag = cleanTag,
        sizePx = cleanSize,
      )
    synchronized(cacheLock) {
      nexusHubModuleEmblemCache[cacheKey]?.let { cached ->
        if (!cached.isRecycled) return cached
      }
      if (nexusHubModuleEmblemCache.size > 36) {
        nexusHubModuleEmblemCache.clear()
      }
      nexusHubModuleEmblemCache[cacheKey] = baked
      return baked
    }
  }

  /**
   * Bakes one of the 6 `192x192` `NEXUS`-Station-Styled Metallic Hub Module Emblems (~88% bounds fill)
   * using the 5-stop brushed gunmetal/titanium armor shading (`#0B1220 -> #1E293B -> #94A3B8 -> #334155 -> #0F172A`),
   * `#020617` silhouette outlines, sunlit `#F8FAFC` specular bevels, recessed dark vents, lit micro-windows,
   * and 3D-tilted glowing holographic rings:
   * - `"HANGAR"` (`SHIPS & BUILDS`): Armored octagonal orbital docking gantry + 4 clamp pylons + 3D cyan scanner arc cradling live equipped ship sprite.
   * - `"CONDUIT"` (`RANKS & QUESTS`): Brushed titanium & solar-gold Harmonic Resonance Citadel Spire + 3-tier wings + 3D holo-ring + glowing `RiftShardGemIcon` heart.
   * - `"FORGE"` (`SKINS & AUDIO`): Gunmetal & violet Orbital Foundry Crucible + 4 curved Nexus Station manipulator claws + 3D blueprint ring + equalizer light bars.
   * - `"COMMS"` (`CHAT & NEXUS AI`): Armored Subspace Transponder Dish & `NEXUS` AI Citadel Core + twin sensor spires + 3D holographic comm-wave arcs.
   * - `"CITADEL"` (`SYNDICATES & SQUADS`): Heavy titanium & solar-gold Syndicate Bastion Ring + 4 armored war pylons framing `bakeSyndicateCrestBitmap`.
   * - `"ARCHIVE"` (`STATS & CODEX`): Gunmetal & emerald Black-Box Holocron Vault + beveled octagonal armor plates + dual counter-rotating 3D astrolabe holo-rings.
   */
  fun bakeNexusHubModuleEmblem(
    moduleKey: String,
    activeShipId: Int = 1,
    crestRecipe: SyndicateCrestRecipe = RiftVisualForgeState.activeSyndicateCrestRecipe,
    syndicateTag: String = "",
    sizePx: Int = 192,
  ): Bitmap {
    val sz = sizePx.coerceIn(96, 384)
    val bmp = Bitmap.createBitmap(sz, sz, Bitmap.Config.ARGB_8888)
    val c = Canvas(bmp)
    val cx = sz * 0.5f
    val cy = sz * 0.5f
    val r = sz * 0.44f
    val u = sz / 192f

    val (accentMain, accentSecondary) =
      when (moduleKey.uppercase()) {
        "HANGAR" -> Color.rgb(56, 189, 248) to Color.rgb(0, 245, 212)
        "CONDUIT" -> Color.rgb(250, 204, 21) to Color.rgb(0, 245, 212)
        "FORGE" -> Color.rgb(0, 245, 212) to Color.rgb(192, 132, 252)
        "COMMS" -> Color.rgb(0, 245, 212) to Color.rgb(250, 204, 21)
        "CITADEL" -> Color.rgb(250, 204, 21) to Color.rgb(56, 189, 248)
        else -> Color.rgb(52, 211, 153) to Color.rgb(56, 189, 248)
      }

    // 5-stop brushed gunmetal/titanium armor gradient (#0B1220 -> #1E293B -> #94A3B8 -> #334155 -> #0F172A)
    val stop0 = blendStationRgb(Color.rgb(11, 18, 32), accentMain, 0.14f)
    val stop1 = blendStationRgb(Color.rgb(30, 41, 59), accentMain, 0.12f)
    val stop2 = blendStationRgb(Color.rgb(148, 163, 184), Color.rgb(226, 232, 240), 0.42f)
    val stop3 = blendStationRgb(Color.rgb(51, 65, 85), accentSecondary, 0.12f)
    val stop4 = Color.rgb(15, 23, 42)

    val armorPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        shader =
          LinearGradient(
            cx - r * 0.92f,
            cy - r * 0.92f,
            cx + r * 0.92f,
            cy + r * 0.92f,
            intArrayOf(stop0, stop1, stop2, stop3, stop4),
            floatArrayOf(0f, 0.28f, 0.48f, 0.72f, 1f),
            Shader.TileMode.CLAMP,
          )
      }

    val upperPlatePaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        shader =
          LinearGradient(
            cx - r * 0.65f,
            cy - r * 0.65f,
            cx + r * 0.65f,
            cy + r * 0.65f,
            intArrayOf(
              blendStationRgb(Color.rgb(30, 41, 59), accentMain, 0.22f),
              Color.rgb(226, 232, 240),
              blendStationRgb(Color.rgb(51, 65, 85), accentSecondary, 0.20f),
              Color.rgb(11, 18, 32),
            ),
            floatArrayOf(0f, 0.36f, 0.72f, 1f),
            Shader.TileMode.CLAMP,
          )
      }

    val goldTrimPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        shader =
          LinearGradient(
            cx - r * 0.55f,
            cy - r * 0.55f,
            cx + r * 0.55f,
            cy + r * 0.55f,
            intArrayOf(
              Color.rgb(120, 53, 15),
              Color.rgb(245, 158, 11),
              Color.rgb(254, 240, 138),
              Color.rgb(180, 83, 9),
            ),
            floatArrayOf(0f, 0.35f, 0.56f, 1f),
            Shader.TileMode.CLAMP,
          )
      }

    val darkOutlinePaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2.2f * u
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
        color = Color.argb(240, 2, 6, 23)
      }

    val seamPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.25f * u
        strokeCap = Paint.Cap.ROUND
        color = Color.argb(195, 9, 14, 28)
      }

    val bevelPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.45f * u
        strokeCap = Paint.Cap.ROUND
        shader =
          LinearGradient(
            cx - r,
            cy - r,
            cx + r * 0.55f,
            cy + r * 0.55f,
            intArrayOf(
              Color.argb(235, 248, 250, 252),
              Color.argb(145, Color.red(accentMain), Color.green(accentMain), Color.blue(accentMain)),
              Color.argb(24, 15, 23, 42),
            ),
            floatArrayOf(0f, 0.46f, 1f),
            Shader.TileMode.CLAMP,
          )
      }

    val recessPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.argb(230, 5, 10, 22)
      }

    val dropShadowPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.argb(155, 2, 6, 18)
      }

    val holoStrokePaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
      }

    val glowFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    val windowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    val arcRect = RectF()

    // Soft radial ambient volumetric halo behind the station module
    glowFillPaint.shader =
      RadialGradient(
        cx,
        cy,
        r * 1.06f,
        intArrayOf(
          Color.argb(86, Color.red(accentMain), Color.green(accentMain), Color.blue(accentMain)),
          Color.argb(38, Color.red(accentSecondary), Color.green(accentSecondary), Color.blue(accentSecondary)),
          Color.argb(0, 2, 6, 23),
        ),
        floatArrayOf(0f, 0.62f, 1f),
        Shader.TileMode.CLAMP,
      )
    c.drawCircle(cx, cy, r * 1.06f, glowFillPaint)
    glowFillPaint.shader = null

    fun drawPolygonRing(sides: Int, outerR: Float, innerR: Float, rotOffsetRad: Float = 0f, fill: Paint = armorPaint) {
      val ringPath = Path().apply { fillType = Path.FillType.EVEN_ODD }
      for (i in 0 until sides) {
        val a = rotOffsetRad + i * (2f * PI.toFloat() / sides)
        val px = cx + cos(a) * outerR
        val py = cy + sin(a) * outerR
        if (i == 0) ringPath.moveTo(px, py) else ringPath.lineTo(px, py)
      }
      ringPath.close()
      if (innerR > 1f) {
        for (i in 0 until sides) {
          val a = rotOffsetRad + i * (2f * PI.toFloat() / sides)
          val px = cx + cos(a) * innerR
          val py = cy + sin(a) * innerR
          if (i == 0) ringPath.moveTo(px, py) else ringPath.lineTo(px, py)
        }
        ringPath.close()
      }
      c.save()
      c.translate(2.5f * u, 3.5f * u)
      c.drawPath(ringPath, dropShadowPaint)
      c.restore()
      c.drawPath(ringPath, fill)
      c.drawPath(ringPath, darkOutlinePaint)
      c.drawPath(ringPath, bevelPaint)
    }

    fun drawTiltedHoloRing(
      tiltDeg: Float,
      squashY: Float,
      ringR: Float,
      colA: Int,
      colB: Int,
      segments: Int = 4,
      sweepDeg: Float = 56f,
    ) {
      c.save()
      c.translate(cx, cy)
      c.rotate(tiltDeg)
      c.scale(1f, squashY)
      arcRect.set(-ringR, -ringR, ringR, ringR)
      holoStrokePaint.strokeWidth = 5.4f * u
      holoStrokePaint.color = Color.argb(66, Color.red(colA), Color.green(colA), Color.blue(colA))
      c.drawOval(arcRect, holoStrokePaint)

      holoStrokePaint.strokeWidth = 2.3f * u
      holoStrokePaint.color = Color.argb(230, Color.red(colA), Color.green(colA), Color.blue(colA))
      val step = 360f / segments.coerceAtLeast(1)
      for (s in 0 until segments) {
        c.drawArc(arcRect, s * step + 12f, sweepDeg, false, holoStrokePaint)
      }

      val innerR = ringR * 0.86f
      arcRect.set(-innerR, -innerR, innerR, innerR)
      holoStrokePaint.strokeWidth = 1.55f * u
      holoStrokePaint.color = Color.argb(205, Color.red(colB), Color.green(colB), Color.blue(colB))
      for (s in 0 until (segments + 2)) {
        c.drawArc(arcRect, s * (360f / (segments + 2)) - 18f, 28f, false, holoStrokePaint)
      }

      arcRect.set(-ringR, -ringR, ringR, ringR)
      holoStrokePaint.strokeWidth = 2.6f * u
      holoStrokePaint.color = Color.argb(235, 255, 255, 255)
      c.drawArc(arcRect, -38f, 20f, false, holoStrokePaint)
      c.restore()
    }

    when (moduleKey.uppercase()) {
      "HANGAR" -> {
        // 1. Armored Octagonal Orbital Docking Gantry Ring + Recessed Launch Bay
        c.drawCircle(cx, cy, r * 0.72f, recessPaint)
        holoStrokePaint.strokeWidth = 1.2f * u
        holoStrokePaint.color = Color.argb(85, 56, 189, 248)
        c.drawCircle(cx, cy, r * 0.56f, holoStrokePaint)
        c.drawLine(cx - r * 0.62f, cy, cx + r * 0.62f, cy, holoStrokePaint)
        c.drawLine(cx, cy - r * 0.62f, cx, cy + r * 0.62f, holoStrokePaint)

        drawPolygonRing(
          sides = 8,
          outerR = r * 0.96f,
          innerR = r * 0.68f,
          rotOffsetRad = (PI / 8.0).toFloat(),
          fill = armorPaint,
        )

        // Radial armor panel seams & lit deck micro-windows around gantry
        for (i in 0 until 8) {
          val a = (PI / 8.0).toFloat() + i * (PI.toFloat() * 0.25f)
          val ca = cos(a)
          val sa = sin(a)
          c.drawLine(cx + ca * r * 0.69f, cy + sa * r * 0.69f, cx + ca * r * 0.95f, cy + sa * r * 0.95f, seamPaint)
          val midA = a + (PI.toFloat() * 0.125f)
          val wx = cx + cos(midA) * (r * 0.82f)
          val wy = cy + sin(midA) * (r * 0.82f)
          windowPaint.color = if (i % 2 == 0) Color.rgb(0, 245, 212) else Color.rgb(250, 204, 21)
          c.drawCircle(wx, wy, 2.0f * u, windowPaint)
        }

        // 4 Heavy Hydraulic Clamp Pylons (45°, 135°, 225°, 315°)
        for (k in 0 until 4) {
          val deg = 45f + k * 90f
          c.save()
          c.translate(cx, cy)
          c.rotate(deg)
          val clampPath =
            Path().apply {
              moveTo(r * 0.54f, -7.5f * u)
              lineTo(r * 1.00f, -11.5f * u)
              lineTo(r * 1.00f, 11.5f * u)
              lineTo(r * 0.54f, 7.5f * u)
              close()
            }
          c.drawPath(clampPath, upperPlatePaint)
          c.drawPath(clampPath, darkOutlinePaint)
          c.drawPath(clampPath, bevelPaint)
          windowPaint.color = Color.rgb(0, 245, 212)
          c.drawCircle(r * 0.60f, 0f, 2.4f * u, windowPaint)
          c.restore()
        }

        // 2. Live Equipped Ship Sprite angled at -35° with glowing twin ion thrusters
        val shipBmp = getShipSprite(shipId = activeShipId.coerceIn(1, 10), isAllied = true, isHitFlashing = false)
        c.save()
        c.translate(cx, cy)
        c.rotate(-35f)
        // Twin ion exhaust plumes trailing aft (-X in ship sprite space)
        for (vyOff in floatArrayOf(-8.5f * u, 8.5f * u)) {
          glowFillPaint.shader =
            LinearGradient(
              -r * 0.18f,
              vyOff,
              -r * 0.72f,
              vyOff,
              intArrayOf(
                Color.argb(245, 255, 255, 255),
                Color.argb(215, 0, 245, 212),
                Color.argb(0, 56, 189, 248),
              ),
              floatArrayOf(0f, 0.42f, 1f),
              Shader.TileMode.CLAMP,
            )
          val plumeRect = RectF(-r * 0.70f, vyOff - 4.2f * u, -r * 0.14f, vyOff + 4.2f * u)
          c.drawRoundRect(plumeRect, 4f * u, 4f * u, glowFillPaint)
        }
        glowFillPaint.shader = null

        val shipDrawDiameter = sz * 0.68f
        val shipHalf = shipDrawDiameter * 0.5f
        val dstShip = RectF(-shipHalf + 3f * u, -shipHalf, shipHalf + 3f * u, shipHalf)
        val shadowRect = RectF(dstShip.left + 3f * u, dstShip.top + 4f * u, dstShip.right + 3f * u, dstShip.bottom + 4f * u)
        val shadowTint =
          Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            colorFilter = PorterDuffColorFilter(Color.argb(165, 2, 6, 18), PorterDuff.Mode.SRC_IN)
          }
        c.drawBitmap(shipBmp, null, shadowRect, shadowTint)
        c.drawBitmap(shipBmp, null, dstShip, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
        c.restore()

        // 3. Forefront 3D-Tilted Cyan Holographic Scanner Arc
        drawTiltedHoloRing(
          tiltDeg = -22f,
          squashY = 0.46f,
          ringR = r * 0.88f,
          colA = Color.rgb(0, 245, 212),
          colB = Color.rgb(56, 189, 248),
          segments = 4,
          sweepDeg = 52f,
        )
      }

      "CONDUIT" -> {
        // 1. Brushed Titanium & Solar-Gold 3-Tier Harmonic Winged Citadel Spire
        for (tier in 0..2) {
          val spanX = r * (0.96f - tier * 0.18f)
          val topY = cy - r * (0.46f - tier * 0.16f)
          val botY = cy + r * (0.28f + tier * 0.12f)
          val wingPath =
            Path().apply {
              moveTo(cx, topY + 14f * u)
              lineTo(cx - spanX, topY)
              lineTo(cx - spanX * 0.78f, botY * 0.5f + topY * 0.5f + 16f * u)
              lineTo(cx, botY)
              lineTo(cx + spanX * 0.78f, botY * 0.5f + topY * 0.5f + 16f * u)
              lineTo(cx + spanX, topY)
              close()
            }
          c.save()
          c.translate(2f * u, 3f * u)
          c.drawPath(wingPath, dropShadowPaint)
          c.restore()
          c.drawPath(wingPath, if (tier == 1) goldTrimPaint else armorPaint)
          c.drawPath(wingPath, darkOutlinePaint)
          c.drawPath(wingPath, bevelPaint)
        }

        // Central Vertical Armored Harmonic Obelisk Spine
        val spirePath =
          Path().apply {
            moveTo(cx, cy - r * 0.98f)
            lineTo(cx + r * 0.24f, cy - r * 0.34f)
            lineTo(cx + r * 0.19f, cy + r * 0.76f)
            lineTo(cx, cy + r * 0.98f)
            lineTo(cx - r * 0.19f, cy + r * 0.76f)
            lineTo(cx - r * 0.24f, cy - r * 0.34f)
            close()
          }
        c.drawPath(spirePath, upperPlatePaint)
        c.drawPath(spirePath, darkOutlinePaint)
        c.drawPath(spirePath, bevelPaint)

        // Recessed Dark Vents & Lit Solar-Gold / Cyan Spire Deck Windows
        c.drawRoundRect(RectF(cx - 5f * u, cy - r * 0.74f, cx + 5f * u, cy - r * 0.44f), 2.5f * u, 2.5f * u, recessPaint)
        for (wIdx in -2..2) {
          if (wIdx == 0) continue
          windowPaint.color = if (wIdx % 2 == 0) Color.rgb(250, 204, 21) else Color.rgb(0, 245, 212)
          c.drawCircle(cx + wIdx * 6.5f * u, cy + r * 0.54f, 2.0f * u, windowPaint)
        }

        // 3D-Tilted Gold/Cyan Harmonic Holo-Ring
        drawTiltedHoloRing(
          tiltDeg = -18f,
          squashY = 0.42f,
          ringR = r * 0.90f,
          colA = Color.rgb(250, 204, 21),
          colB = Color.rgb(0, 245, 212),
          segments = 6,
          sweepDeg = 38f,
        )

        // Faceted Prismatic Rift Shard Gem Heart at Spire Core
        c.drawCircle(cx, cy - 2f * u, r * 0.31f, recessPaint)
        drawRiftShardGemIcon(c, cx, cy - 2f * u, r * 0.29f, alpha = 255)
      }

      "FORGE" -> {
        // 1. Gunmetal & Violet Orbital Foundry Crucible Collar
        c.drawCircle(cx, cy, r * 0.74f, recessPaint)
        drawPolygonRing(
          sides = 12,
          outerR = r * 0.94f,
          innerR = r * 0.70f,
          rotOffsetRad = (PI / 12.0).toFloat(),
          fill = armorPaint,
        )

        // 4 Curved Nexus Station Manipulator Claws Arcing Inward (0°, 90°, 180°, 270°)
        for (i in 0 until 4) {
          c.save()
          c.translate(cx, cy)
          c.rotate(i * 90f + 45f)
          val clawPath =
            Path().apply {
              moveTo(r * 0.92f, -12f * u)
              quadTo(r * 0.64f, -20f * u, r * 0.36f, -6.5f * u)
              lineTo(r * 0.36f, 6.5f * u)
              quadTo(r * 0.64f, 20f * u, r * 0.92f, 12f * u)
              close()
            }
          c.drawPath(clawPath, upperPlatePaint)
          c.drawPath(clawPath, darkOutlinePaint)
          c.drawPath(clawPath, bevelPaint)
          windowPaint.color = Color.rgb(0, 245, 212)
          c.drawCircle(r * 0.40f, 0f, 3.0f * u, windowPaint)
          windowPaint.color = Color.WHITE
          c.drawCircle(r * 0.40f, 0f, 1.4f * u, windowPaint)
          c.restore()
        }

        // 2. Central Audio-Spectrum Equalizer Light Bars + Crucible Energy Core
        val barHeights = floatArrayOf(0.22f, 0.38f, 0.52f, 0.64f, 0.48f, 0.34f, 0.20f)
        val barSpacing = 7.4f * u
        val barW = 4.4f * u
        for (bIdx in barHeights.indices) {
          val bx = cx + (bIdx - 3) * barSpacing
          val bh = r * barHeights[bIdx]
          windowPaint.color =
            when {
              bIdx == 3 -> Color.rgb(250, 204, 21)
              bIdx % 2 == 0 -> Color.rgb(0, 245, 212)
              else -> Color.rgb(192, 132, 252)
            }
          c.drawRoundRect(RectF(bx - barW * 0.5f, cy - bh * 0.5f, bx + barW * 0.5f, cy + bh * 0.5f), 2f * u, 2f * u, windowPaint)
        }

        // 3. 3D-Tilted Cyan/Violet Holographic Blueprint Ring
        drawTiltedHoloRing(
          tiltDeg = 20f,
          squashY = 0.45f,
          ringR = r * 0.86f,
          colA = Color.rgb(0, 245, 212),
          colB = Color.rgb(192, 132, 252),
          segments = 4,
          sweepDeg = 58f,
        )
      }

      "COMMS" -> {
        // 1. Armored Parabolic Subspace Transponder Dish Base (Lower Cradle)
        val dishPath =
          Path().apply {
            moveTo(cx - r * 0.90f, cy + r * 0.08f)
            quadTo(cx, cy + r * 0.92f, cx + r * 0.90f, cy + r * 0.08f)
            lineTo(cx + r * 0.72f, cy - r * 0.06f)
            quadTo(cx, cy + r * 0.54f, cx - r * 0.72f, cy - r * 0.06f)
            close()
          }
        c.save()
        c.translate(2.5f * u, 3.5f * u)
        c.drawPath(dishPath, dropShadowPaint)
        c.restore()
        c.drawPath(dishPath, armorPaint)
        c.drawPath(dishPath, darkOutlinePaint)
        c.drawPath(dishPath, bevelPaint)

        // Armored Subspace Pedestal Keel
        val keelPath =
          Path().apply {
            moveTo(cx - r * 0.34f, cy + r * 0.54f)
            lineTo(cx + r * 0.34f, cy + r * 0.54f)
            lineTo(cx + r * 0.48f, cy + r * 0.92f)
            lineTo(cx - r * 0.48f, cy + r * 0.92f)
            close()
          }
        c.drawPath(keelPath, upperPlatePaint)
        c.drawPath(keelPath, darkOutlinePaint)
        c.drawPath(keelPath, bevelPaint)

        // Twin Vertical Armored Sensor Spires Flanking NEXUS AI Core
        for (dir in intArrayOf(-1, 1)) {
          val sx = cx + dir * r * 0.50f
          val towerPath =
            Path().apply {
              moveTo(sx - dir * 4f * u, cy - r * 0.72f)
              lineTo(sx + dir * 11f * u, cy - r * 0.46f)
              lineTo(sx + dir * 13f * u, cy + r * 0.28f)
              lineTo(sx - dir * 5f * u, cy + r * 0.22f)
              close()
            }
          c.drawPath(towerPath, upperPlatePaint)
          c.drawPath(towerPath, darkOutlinePaint)
          c.drawPath(towerPath, bevelPaint)
          windowPaint.color = Color.rgb(0, 245, 212)
          c.drawCircle(sx + dir * 3f * u, cy - r * 0.28f, 2.1f * u, windowPaint)
          windowPaint.color = Color.rgb(250, 204, 21)
          c.drawCircle(sx + dir * 4f * u, cy - r * 0.04f, 2.1f * u, windowPaint)
        }

        // Lit Promenade Micro-Windows Along Dish Rim
        for (w in -3..3) {
          val wx = cx + w * (r * 0.20f)
          val wy = cy + r * 0.36f - (w * w) * 1.9f * u
          windowPaint.color = if (w % 2 == 0) Color.rgb(0, 245, 212) else Color.rgb(250, 204, 21)
          c.drawCircle(wx, wy, 2.1f * u, windowPaint)
        }

        // Central NEXUS AI Citadel Neural Core Sphere
        c.drawCircle(cx, cy - r * 0.08f, r * 0.26f, recessPaint)
        glowFillPaint.shader =
          RadialGradient(
            cx - 4f * u,
            cy - r * 0.08f - 4f * u,
            r * 0.24f,
            intArrayOf(
              Color.WHITE,
              Color.rgb(0, 245, 212),
              Color.rgb(2, 132, 199),
            ),
            floatArrayOf(0f, 0.48f, 1f),
            Shader.TileMode.CLAMP,
          )
        c.drawCircle(cx, cy - r * 0.08f, r * 0.21f, glowFillPaint)
        glowFillPaint.shader = null
        c.drawCircle(cx, cy - r * 0.08f, r * 0.24f, darkOutlinePaint)

        // 3 Concentric Holographic Subspace Comm-Wave Arcs Radiating Upward
        for (wave in 1..3) {
          val wr = r * (0.34f + wave * 0.19f)
          arcRect.set(cx - wr, cy - r * 0.08f - wr, cx + wr, cy - r * 0.08f + wr)
          holoStrokePaint.strokeWidth = (3.0f - wave * 0.55f) * u
          holoStrokePaint.color =
            if (wave == 2) Color.argb(225, 250, 204, 21) else Color.argb(235 - wave * 35, 0, 245, 212)
          c.drawArc(arcRect, -138f, 96f, false, holoStrokePaint)
        }
      }

      "CITADEL" -> {
        // 1. Heavy Titanium & Solar-Gold Syndicate Bastion Ring
        c.drawCircle(cx, cy, r * 0.72f, recessPaint)
        drawPolygonRing(
          sides = 8,
          outerR = r * 0.96f,
          innerR = r * 0.68f,
          rotOffsetRad = 0f,
          fill = armorPaint,
        )

        // 4 Armored Cardinal War Pylons (0°, 90°, 180°, 270°) with Solar-Gold Chevrons
        for (pIdx in 0 until 4) {
          c.save()
          c.translate(cx, cy)
          c.rotate(pIdx * 90f)
          val bastionPylon =
            Path().apply {
              moveTo(r * 0.62f, -12f * u)
              lineTo(r * 1.02f, -8f * u)
              lineTo(r * 1.05f, 0f)
              lineTo(r * 1.02f, 8f * u)
              lineTo(r * 0.62f, 12f * u)
              close()
            }
          c.drawPath(bastionPylon, goldTrimPaint)
          c.drawPath(bastionPylon, darkOutlinePaint)
          c.drawPath(bastionPylon, bevelPaint)
          windowPaint.color = Color.rgb(56, 189, 248)
          c.drawCircle(r * 0.80f, 0f, 2.5f * u, windowPaint)
          c.restore()
        }

        // 3D-Tilted Gold/Cyan Tactical Ring Behind Crest
        drawTiltedHoloRing(
          tiltDeg = -16f,
          squashY = 0.44f,
          ringR = r * 0.88f,
          colA = Color.rgb(250, 204, 21),
          colB = Color.rgb(56, 189, 248),
          segments = 4,
          sweepDeg = 48f,
        )

        // 2. Pilot's Forged SyndicateCrestBitmap Framed at Dead Center
        val crestPx = (sz * 0.58f).roundToInt().coerceAtLeast(64)
        val crestBmp = bakeSyndicateCrestBitmap(recipe = crestRecipe, tagText = syndicateTag, sizePx = crestPx)
        val halfCrest = crestPx * 0.5f
        val dstCrest = RectF(cx - halfCrest, cy - halfCrest, cx + halfCrest, cy + halfCrest)
        c.drawBitmap(crestBmp, null, dstCrest, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
      }

      else -> {
        // "ARCHIVE" (STATS & CODEX): Gunmetal & Emerald Black-Box Holocron Vault + Dual Astrolabe Holo-Rings
        c.drawCircle(cx, cy, r * 0.72f, recessPaint)
        drawPolygonRing(
          sides = 8,
          outerR = r * 0.94f,
          innerR = r * 0.66f,
          rotOffsetRad = (PI / 8.0).toFloat(),
          fill = armorPaint,
        )

        // Inner Beveled Octagonal Black-Box Data Vault Plates
        val vaultR = r * 0.50f
        val vaultPath = Path()
        for (i in 0 until 8) {
          val a = i * (PI.toFloat() * 0.25f)
          val px = cx + cos(a) * vaultR
          val py = cy + sin(a) * vaultR
          if (i == 0) vaultPath.moveTo(px, py) else vaultPath.lineTo(px, py)
        }
        vaultPath.close()
        c.drawPath(vaultPath, upperPlatePaint)
        c.drawPath(vaultPath, darkOutlinePaint)
        c.drawPath(vaultPath, bevelPaint)

        // Vault Seam Crossbars & Emerald/Cyan Status Micro-Windows
        for (i in 0 until 4) {
          val a = i * (PI.toFloat() * 0.5f) + (PI.toFloat() * 0.25f)
          c.drawLine(
            cx + cos(a) * (vaultR * 0.42f),
            cy + sin(a) * (vaultR * 0.42f),
            cx + cos(a) * (r * 0.92f),
            cy + sin(a) * (r * 0.92f),
            seamPaint,
          )
          windowPaint.color = if (i % 2 == 0) Color.rgb(52, 211, 153) else Color.rgb(56, 189, 248)
          c.drawCircle(cx + cos(a) * (r * 0.79f), cy + sin(a) * (r * 0.79f), 2.4f * u, windowPaint)
        }

        // Dual Counter-Rotating 3D-Tilted Emerald/Cyan Astrolabe Holo-Rings
        drawTiltedHoloRing(
          tiltDeg = 26f,
          squashY = 0.44f,
          ringR = r * 0.88f,
          colA = Color.rgb(52, 211, 153),
          colB = Color.rgb(56, 189, 248),
          segments = 4,
          sweepDeg = 54f,
        )
        drawTiltedHoloRing(
          tiltDeg = -26f,
          squashY = 0.44f,
          ringR = r * 0.78f,
          colA = Color.rgb(56, 189, 248),
          colB = Color.rgb(250, 204, 21),
          segments = 3,
          sweepDeg = 62f,
        )

        // Glowing Crystalline Holocron Data Core
        val coreR = r * 0.24f
        val diamondCore =
          Path().apply {
            moveTo(cx, cy - coreR * 1.18f)
            lineTo(cx + coreR, cy)
            lineTo(cx, cy + coreR * 1.18f)
            lineTo(cx - coreR, cy)
            close()
          }
        glowFillPaint.shader =
          RadialGradient(
            cx - 3f * u,
            cy - 4f * u,
            coreR * 1.2f,
            intArrayOf(
              Color.WHITE,
              Color.rgb(52, 211, 153),
              Color.rgb(6, 95, 70),
            ),
            floatArrayOf(0f, 0.52f, 1f),
            Shader.TileMode.CLAMP,
          )
        c.drawPath(diamondCore, glowFillPaint)
        glowFillPaint.shader = null
        c.drawPath(diamondCore, darkOutlinePaint)
      }
    }

    return bmp
  }
}
