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
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.graphics.Matrix
import android.os.Build
import android.os.SystemClock
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

private const val VOLUMETRIC_RIFT_AGSL =
  """
  uniform float2 uCenter;
  uniform float uRadius;
  uniform float uTime;
  uniform float uSpin;
  uniform float uTurbulence;
  uniform float uGlow;
  uniform float uOpacity;
  uniform float uInwardSign;
  uniform float3 uDeepRGB;
  uniform float3 uMidRGB;
  uniform float3 uBrightRGB;
  uniform float3 uCoreRGB;

  float hash21r(float2 p) {
    p = fract(p * float2(127.1, 311.7));
    p += dot(p, p + 41.23);
    return fract(p.x * p.y);
  }

  float noise2dr(float2 p) {
    float2 i = floor(p);
    float2 f = fract(p);
    float2 u = f * f * f * (f * (f * 6.0 - 15.0) + 10.0);
    float a = hash21r(i);
    float b = hash21r(i + float2(1.0, 0.0));
    float c = hash21r(i + float2(0.0, 1.0));
    float d = hash21r(i + float2(1.0, 1.0));
    return mix(mix(a, b, u.x), mix(c, d, u.x), u.y);
  }

  float fbmr3(float2 p) {
    float v = 0.0;
    float amp = 0.54;
    for (int i = 0; i < 3; i++) {
      v += amp * noise2dr(p);
      p = float2(p.x * 1.62 - p.y * 1.21, p.x * 1.21 + p.y * 1.62) + float2(-1.6, 1.8);
      amp *= 0.5;
    }
    return v;
  }

  half4 main(float2 fragCoord) {
    float safeR = max(uRadius, 1.0);
    float2 rel = (fragCoord - uCenter) / safeR;
    float r = length(rel);
    if (r > 1.22) {
      return half4(0.0);
    }

    float turb = clamp(uTurbulence, 0.20, 2.20);
    float glow = clamp(uGlow, 0.25, 2.40);

    // Smooth vortex domain twist in 2D Cartesian Rift space (zero atan2 seam, true-centered at uCenter)
    float twist = (1.08 - clamp(r, 0.0, 1.15)) * 1.35 * uInwardSign * turb + uSpin * 0.36;
    float ct = cos(twist);
    float st = sin(twist);
    float2 rotRel = float2(rel.x * ct - rel.y * st, rel.x * st + rel.y * ct);

    float2 warpUv = rotRel * (2.35 + 0.65 * turb) + float2(uTime * 0.22, -uTime * 0.19);
    float warpA = fbmr3(warpUv);
    float warpB = fbmr3(rotRel * (3.65 + 0.85 * turb) + float2(-uTime * 0.27 + warpA * 1.35, uTime * 0.24 - warpA * 1.15));
    float cloudDensity = clamp(warpA * 0.56 + warpB * 0.58, 0.0, 1.0);

    // Centered #000000 -> #02040A abyssal void throat (0.0r .. 0.42r) feathered smoothly into billowing storm clouds (0.30r .. 1.08r)
    float perturbedInner = r + (warpB - 0.5) * 0.11;
    float perturbedOuter = r + (warpA - 0.5) * 0.12 + (warpB - 0.5) * 0.06;
    float innerCloudFeather = smoothstep(0.24, 0.46, perturbedInner);
    float outerCloudFeather = 1.0 - smoothstep(0.80, 1.12, perturbedOuter);
    float cloudBand = innerCloudFeather * outerCloudFeather;

    // Smooth time-shifting palette modulation strictly within this Rift's fixed 4-stop range
    float paletteShift = 0.5 + 0.5 * sin(uTime * 1.45 + warpA * 4.4 - r * 2.2);
    float ridge = clamp(1.0 - abs(warpA - warpB) * 2.7, 0.0, 1.0);
    float wispHighlight = pow(ridge, 2.4) * cloudBand;

    float3 abyssThroat = mix(float3(0.0, 0.0, 0.0), float3(0.008, 0.016, 0.039), smoothstep(0.0, 0.42, r));
    float3 lowCloud = mix(uDeepRGB, uMidRGB, clamp(cloudDensity * 0.85 + paletteShift * 0.30, 0.0, 1.0));
    float3 highCloud = mix(uMidRGB, uBrightRGB, clamp(paletteShift * 0.65 + cloudDensity * 0.45, 0.0, 1.0));
    float3 stormRGB = mix(lowCloud, highCloud, smoothstep(0.28, 0.82, cloudDensity));
    stormRGB = mix(stormRGB, uCoreRGB, clamp(wispHighlight * 0.52 * glow, 0.0, 0.82));

    float cloudBlend = clamp(cloudBand * (0.38 + 0.76 * cloudDensity) * (0.82 + 0.24 * glow), 0.0, 1.0);
    float3 finalRGB = mix(abyssThroat, stormRGB, cloudBlend);

    float throatAlpha = (1.0 - smoothstep(0.26, 0.56, perturbedInner)) * 0.96;
    float stormAlpha = cloudBand * (0.28 + 0.68 * cloudDensity + wispHighlight * 0.24);
    float alpha = clamp(max(throatAlpha, stormAlpha) * uOpacity, 0.0, 0.97);
    return half4(half3(finalRGB * alpha), half(alpha));
  }
  """

object RiftSpriteBaker {
  private val cacheLock = Any()
  private val bakerStartElapsedNanos: Long = SystemClock.elapsedRealtimeNanos()
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
  private val volumetricRiftShaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
  private val volumetricRiftShaderPool: Array<Any?> =
    if (Build.VERSION.SDK_INT >= 33) {
      Array(12) { runCatching { RuntimeShader(VOLUMETRIC_RIFT_AGSL) }.getOrNull() }
    } else {
      emptyArray()
    }
  private var volumetricRiftShaderCursor: Int = 0
  private val sharedCloudMatrix = Matrix()
  private val sharedRiftRibbonPath = Path()
  private val sharedRiftSpinePath = Path()
  private val sharedRiftArcRect = RectF()
  private val sharedPlumePath = Path()
  private val sharedCrystalPath = Path()
  private val sharedFilamentPath = Path()
  private val sharedBoltPath = Path()
  private val sharedPrimaryLightningPath = Path()
  private val sharedForkLightningPath = Path()
  private val sharedStormCloudPath = Path()
  private val sharedLightningPtsX = FloatArray(16)
  private val sharedLightningPtsY = FloatArray(16)
  private val sharedCollarStops = IntArray(5)
  private val sharedCollarPositions = floatArrayOf(0f, 0.28f, 0.48f, 0.74f, 1f)

  private val unitAbyssalVoidShader =
    RadialGradient(
      0f,
      0f,
      1f,
      intArrayOf(
        Color.argb(252, 0, 0, 0),
        Color.argb(242, 2, 4, 10),
        Color.argb(136, 2, 4, 10),
        Color.argb(0, 2, 4, 10),
      ),
      floatArrayOf(0f, 0.52f, 0.80f, 1f),
      Shader.TileMode.CLAMP,
    )
  private val unitCloudPuffShaderCache = HashMap<Int, RadialGradient>(32)

  private fun getUnitCloudPuffShader(rgbColor: Int): RadialGradient {
    val key = rgbColor and 0x00FFFFFF
    unitCloudPuffShaderCache[key]?.let { return it }
    val r = (key shr 16) and 0xFF
    val g = (key shr 8) and 0xFF
    val b = key and 0xFF
    val created =
      RadialGradient(
        0f,
        0f,
        1f,
        intArrayOf(
          Color.argb(224, r, g, b),
          Color.argb(138, r, g, b),
          Color.argb(42, r, g, b),
          Color.argb(0, r, g, b),
        ),
        floatArrayOf(0f, 0.38f, 0.72f, 1f),
        Shader.TileMode.CLAMP,
      )
    if (unitCloudPuffShaderCache.size < 64) {
      unitCloudPuffShaderCache[key] = created
    }
    return created
  }

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
  private val sharedCosmeticFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
  private val sharedCosmeticStrokePaint =
    Paint(Paint.ANTI_ALIAS_FLAG).apply {
      style = Paint.Style.STROKE
      strokeCap = Paint.Cap.ROUND
      strokeJoin = Paint.Join.ROUND
    }
  private val sharedCosmeticArcRect = RectF()
  private val sharedCosmeticPath = Path()

  var spriteVisualRevision: Int = 1
    private set
  var activeHullTrimStyle: Int = 0 // 0=Standard, 1=Obsidian Ice-Cyan, 2=Sovereign Solar Gold, 3=Rift Singularity Violet, 4=Crimson Forge, 5=Cryo-Titanium
    set(value) {
      val clamped = value.coerceIn(0, 5)
      if (field != clamped) {
        field = clamped
        customShip9Version++
        spriteVisualRevision++
      }
    }
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

  // Pre-rendered offscreen radial glow & feathered deep-space stamps (Zero shadowBlur in render loop)
  val cyanGlowStamp: Bitmap
  val amberGlowStamp: Bitmap
  val crimsonGlowStamp: Bitmap
  val emeraldGlowStamp: Bitmap
  val nebulaCyanStamp: Bitmap
  val nebulaIndigoStamp: Bitmap
  val nebulaVioletStamp: Bitmap
  val spiralGalaxyStamp: Bitmap
  val distantCelestialMoonStamp: Bitmap

  var customShip9AlliedBitmap: Bitmap
    private set
  var customShip9HostileBitmap: Bitmap
    private set
  private var lastCustomShip9Recipe: CustomHullVisualRecipe? = null

  init {
    // Pre-warm fixed Rift palette cloud-puff shaders before baking Rift icons
    intArrayOf(
      0x991B1B, 0xEF4444, 0xEC4899, 0xFFE4E6,
      0x0369A1, 0x0EA5E9, 0x7DD3FC, 0xF0F9FF,
      0x3730A3, 0x7C3AED, 0xA855F7, 0xF3E8FF,
      0x0F766E, 0x06B6D4, 0x00F5D4, 0xCCFBF1,
      0x38BDF8, 0xFACC15, 0xF43F5E,
    ).forEach { getUnitCloudPuffShader(it) }

    fluxIconBitmap = bake3dFluxReactorIcon(64)
    resonanceIconBitmap = bake3dResonanceCrestIcon(64)
    riftShardIconBitmap = bake3dRiftShardIcon(64)
    safeZoneStationBitmap = bakeSafeZoneNexusStationBitmap(384)
    supplyPodFrameBitmap = bakeSupplyPodFrameBitmap(160)

    wallTileBitmap = bakeWallTile()
    hazardBorderBitmap = bakeHazardBorderTile()
    wallShader = BitmapShader(wallTileBitmap, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
    hazardShader = BitmapShader(hazardBorderBitmap, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)

    cyanGlowStamp = createPreRenderedGlow(48, Color.parseColor("#38BDF8"))
    amberGlowStamp = createPreRenderedGlow(48, Color.parseColor("#F59E0B"))
    crimsonGlowStamp = createPreRenderedGlow(48, Color.parseColor("#EF4444"))
    emeraldGlowStamp = createPreRenderedGlow(48, Color.parseColor("#10B981"))
    nebulaCyanStamp = bakeFeatheredNebulaStamp(256, Color.rgb(8, 51, 68), Color.rgb(14, 116, 144), 52, seed = 1)
    nebulaIndigoStamp = bakeFeatheredNebulaStamp(256, Color.rgb(30, 27, 75), Color.rgb(49, 46, 129), 58, seed = 2)
    nebulaVioletStamp = bakeFeatheredNebulaStamp(256, Color.rgb(59, 7, 100), Color.rgb(107, 33, 168), 50, seed = 3)
    spiralGalaxyStamp = bakeDistantSpiralGalaxyStamp(256)
    distantCelestialMoonStamp = bakeDistantCelestialMoonStamp(192)

    val allied = mutableMapOf<Int, Bitmap>()
    val hostile = mutableMapOf<Int, Bitmap>()
    for (spec in RiftCodex.SHIPS) {
      allied[spec.id] = bakeShipBitmap(spec, 256, isAllied = true)
      hostile[spec.id] = bakeShipBitmap(spec, 256, isAllied = false)
    }
    alliedShipBitmaps = allied
    hostileShipBitmaps = hostile

    val initialRecipe = RiftCodex.activeCustomShipSlot.visualRecipe
    lastCustomShip9Recipe = initialRecipe
    customShip9AlliedBitmap = bakeCustomShip9Bitmap(initialRecipe, 256, isAllied = true, hullTrimStyle = 0)
    customShip9HostileBitmap = bakeCustomShip9Bitmap(initialRecipe, 256, isAllied = false, hullTrimStyle = 0)

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
          safeZoneStationBitmap = bakeSafeZoneNexusStationBitmap(384)
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
          supplyPodFrameBitmap = bakeSupplyPodFrameBitmap(160)
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
      safeZoneStationBitmap = bakeSafeZoneNexusStationBitmap(384)
      supplyPodFrameBitmap = bakeSupplyPodFrameBitmap(160)
      fluxIconBitmap = bake3dFluxReactorIcon(64)
      resonanceIconBitmap = bake3dResonanceCrestIcon(64)
      riftShardIconBitmap = bake3dRiftShardIcon(64)
      riftAnomalyIconBitmaps = buildRiftAnomalyIconMap()
      spriteVisualRevision++
    }
  }

  /**
   * Prewarms heavy procedural sprites (all 224x224 Space Obstacle variants, active hull trim sprites,
   * and all 6 192x192 Rift Hub metallic module emblems) on a background worker thread after Frame #1
   * commits, eliminating main-thread SurfaceSyncGroup stalls.
   */
  fun prewarmStartupAssets(
    activeShipId: Int = 1,
    trimStyle: Int = 0,
    syndicateTag: String = "",
  ) {
    val clampedShip = activeShipId.coerceIn(1, 10)
    val clampedTrim = trimStyle.coerceIn(0, 5)
    if (clampedTrim != 0 && clampedShip in 1..10) {
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
   * Bakes the 2.5D beveled hexagonal Gunmetal/Emerald Containment Supply Pod frame (`160x160`)
   * with 3D metallic corner clamps, recessed dark-glass core window, and specular rim catchlights,
   * driven live by `ForgeElementTarget.SUPPLY_CRATE` in `STAR FORGE`.
   */
  fun bakeSupplyPodFrameBitmap(sizePx: Int = 160): Bitmap {
    val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val c = Canvas(bmp)
    val cx = sizePx * 0.5f
    val cy = sizePx * 0.5f
    val u = sizePx / 128f
    val rec = runCatching { RiftVisualForgeState.getRecipe(ForgeElementTarget.SUPPLY_CRATE) }.getOrNull()
    val useCustom = rec != null && !rec.isFactoryDefault
    val primaryCol = if (useCustom) rec!!.primaryColorInt else Color.parseColor("#050C18")
    val trimCol = if (useCustom) rec!!.trimColorInt else Color.parseColor("#10B981")
    val glowCol = if (useCustom) rec!!.glowColorInt else Color.parseColor("#00F5D4")
    val borderMult = (rec?.borderWeight ?: 1f).coerceIn(0.55f, 2.2f)
    val opacityFrac = (rec?.surfaceOpacity ?: 0.94f).coerceIn(0.35f, 1.0f)
    val rOuter = sizePx * 0.44f
    val rMid = sizePx * 0.39f
    val rInner = sizePx * 0.33f
    val cornerEffectOuter = CornerPathEffect(sizePx * 0.105f)
    val cornerEffectInner = CornerPathEffect(sizePx * 0.082f)

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
    val midHex = hexPath(rMid)
    val innerHex = hexPath(rInner)

    // 1. Calm outer powerup aura + stepped under-pod ambient occlusion shadow
    c.drawCircle(
      cx,
      cy,
      sizePx * 0.485f,
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        shader =
          RadialGradient(
            cx,
            cy,
            sizePx * 0.485f,
            intArrayOf(
              Color.argb(64, Color.red(glowCol), Color.green(glowCol), Color.blue(glowCol)),
              Color.argb(24, Color.red(trimCol), Color.green(trimCol), Color.blue(trimCol)),
              Color.argb(0, Color.red(trimCol), Color.green(trimCol), Color.blue(trimCol)),
            ),
            floatArrayOf(0f, 0.66f, 1f),
            Shader.TileMode.CLAMP,
          )
      },
    )
    c.save()
    c.translate(2.2f * u, 2.6f * u)
    c.drawPath(
      outerHex,
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        pathEffect = cornerEffectOuter
        color = Color.argb(185, 2, 6, 16)
      },
    )
    c.restore()

    // 2. Key-lit top-left L=(-0.58,-0.62,0.53) 5-stop brushed gunmetal/titanium outer containment pod
    val armorPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        pathEffect = cornerEffectOuter
        shader =
          LinearGradient(
            cx - rOuter * 0.95f,
            cy - rOuter * 1.02f,
            cx + rOuter * 0.95f,
            cy + rOuter * 1.02f,
            intArrayOf(
              Color.parseColor("#CBD5E1"),
              Color.parseColor("#64748B"),
              Color.parseColor("#334155"),
              Color.parseColor("#0F172A"),
              Color.parseColor("#040812"),
            ),
            floatArrayOf(0f, 0.22f, 0.48f, 0.78f, 1f),
            Shader.TileMode.CLAMP,
          )
      }
    c.drawPath(outerHex, armorPaint)

    // Stepped recessed mid-bevel trench (AO contact groove)
    c.drawPath(
      midHex,
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        pathEffect = cornerEffectInner
        strokeWidth = 1.6f * u
        color = Color.argb(190, 3, 7, 18)
      },
    )

    // Crisp outer outline + directional top-left Blinn-Phong specular catchlight
    c.drawPath(
      outerHex,
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        pathEffect = cornerEffectOuter
        strokeWidth = 2.3f * u * borderMult
        color = Color.parseColor("#030712")
      },
    )
    c.drawPath(
      outerHex,
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        pathEffect = cornerEffectOuter
        strokeWidth = 1.25f * u * borderMult
        shader =
          LinearGradient(
            cx - rOuter,
            cy - rOuter,
            cx + rOuter * 0.65f,
            cy + rOuter * 0.65f,
            intArrayOf(
              Color.argb(225, 248, 250, 252),
              Color.argb(115, 186, 230, 253),
              Color.argb(0, 15, 23, 42),
            ),
            floatArrayOf(0f, 0.45f, 1f),
            Shader.TileMode.CLAMP,
          )
      },
    )

    // 3. Recessed polarized dark-glass containment core window + specular sky-glint reflection
    c.drawPath(
      innerHex,
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        pathEffect = cornerEffectInner
        shader =
          RadialGradient(
            cx - rInner * 0.24f,
            cy - rInner * 0.26f,
            rInner * 1.28f,
            intArrayOf(
              Color.argb((215 * opacityFrac).roundToInt().coerceIn(90, 245), 14, 34, 58),
              Color.argb(
                (232 * opacityFrac).roundToInt().coerceIn(95, 248),
                Color.red(primaryCol),
                Color.green(primaryCol),
                Color.blue(primaryCol),
              ),
              Color.argb(248, 2, 5, 12),
            ),
            floatArrayOf(0f, 0.56f, 1f),
            Shader.TileMode.CLAMP,
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
              Color.argb(88, Color.red(glowCol), Color.green(glowCol), Color.blue(glowCol)),
              Color.argb(36, Color.red(trimCol), Color.green(trimCol), Color.blue(trimCol)),
              Color.argb(0, Color.red(trimCol), Color.green(trimCol), Color.blue(trimCol)),
            ),
            floatArrayOf(0f, 0.64f, 1f),
            Shader.TileMode.CLAMP,
          )
      },
    )

    // Polarized curved glass reflection streak across upper-left window quadrant
    val glassGlintRect = RectF(cx - rInner * 0.76f, cy - rInner * 0.76f, cx + rInner * 0.76f, cy + rInner * 0.76f)
    c.drawArc(
      glassGlintRect,
      198f,
      54f,
      false,
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeWidth = 1.65f * u
        color = Color.argb(148, 248, 250, 252)
      },
    )

    // Glowing inner containment bezel
    c.drawPath(
      innerHex,
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        pathEffect = cornerEffectInner
        strokeWidth = 1.95f * u * borderMult
        color = glowCol
      },
    )

    // 4. Six 3D Metallic Corner Clamps + Edge Energy Conduits
    val clampFillPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        shader =
          LinearGradient(
            cx - rOuter,
            cy - rOuter,
            cx + rOuter,
            cy + rOuter,
            intArrayOf(Color.parseColor("#E2E8F0"), Color.parseColor("#475569"), Color.parseColor("#0F172A")),
            floatArrayOf(0f, 0.48f, 1f),
            Shader.TileMode.CLAMP,
          )
      }
    val clampStrokePaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.25f * u
        color = Color.parseColor("#030712")
      }
    val clampRect = RectF()
    for (i in 0 until 6) {
      val vertDeg = i * 60f - 30f
      c.save()
      c.translate(cx, cy)
      c.rotate(vertDeg)
      clampRect.set(rInner * 0.90f, -4.4f * u, rOuter * 0.97f, 4.4f * u)
      c.drawRoundRect(clampRect, 2.2f * u, 2.2f * u, clampFillPaint)
      c.drawRoundRect(clampRect, 2.2f * u, 2.2f * u, clampStrokePaint)
      c.restore()
    }

    val arcRect = RectF(cx - rOuter * 0.89f, cy - rOuter * 0.89f, cx + rOuter * 0.89f, cy + rOuter * 0.89f)
    val accentArcPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeWidth = 2.1f * u * borderMult
        color = Color.argb(205, Color.red(trimCol), Color.green(trimCol), Color.blue(trimCol))
      }
    val nodePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

    for (i in 0 until 6) {
      val midEdgeDeg = i * 60f - 8f
      c.drawArc(arcRect, midEdgeDeg, 16f, false, accentArcPaint)

      val midEdgeA = ((i * 60.0) * PI / 180.0).toFloat()
      val nx = cx + cos(midEdgeA) * (rOuter * 0.89f)
      val ny = cy + sin(midEdgeA) * (rOuter * 0.89f)
      nodePaint.color = glowCol
      c.drawCircle(nx, ny, 2.15f * u, nodePaint)
      nodePaint.color = Color.parseColor("#F8FAFC")
      c.drawCircle(nx, ny, 1.0f * u, nodePaint)
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

    // 5-Stop Brushed Titanium / Gunmetal Station Armor Palette (key-lit L=(-0.58,-0.62,0.53))
    val metalStop0 = if (useCustom) mixRgb(Color.parseColor("#E2E8F0"), trimCol, 0.34f) else Color.parseColor("#E2E8F0")
    val metalStop1 = if (useCustom) mixRgb(Color.parseColor("#64748B"), trimCol, 0.32f) else Color.parseColor("#64748B")
    val metalStop2 = if (useCustom) mixRgb(Color.parseColor("#334155"), primaryCol, 0.46f) else Color.parseColor("#334155")
    val metalStop3 = if (useCustom) mixRgb(Color.parseColor("#162032"), primaryCol, 0.62f) else Color.parseColor("#162032")
    val metalStop4 = if (useCustom) mixRgb(Color.parseColor("#050A14"), primaryCol, 0.74f) else Color.parseColor("#050A14")

    // 5-Stop Directional Normal-Lit Deep-Space Basalt Rock Palette
    val rockStop0 = if (useCustom) mixRgb(Color.parseColor("#788A9F"), trimCol, 0.30f) else Color.parseColor("#788A9F")
    val rockStop1 = if (useCustom) mixRgb(Color.parseColor("#475569"), trimCol, 0.24f) else Color.parseColor("#475569")
    val rockStop2 = if (useCustom) mixRgb(Color.parseColor("#243247"), primaryCol, 0.44f) else Color.parseColor("#243247")
    val rockStop3 = if (useCustom) mixRgb(Color.parseColor("#111B2E"), primaryCol, 0.60f) else Color.parseColor("#111B2E")
    val rockStop4 = if (useCustom) mixRgb(Color.parseColor("#040812"), primaryCol, 0.76f) else Color.parseColor("#040812")

    val aoShadowPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.argb(175, 2, 5, 14)
      }
    val metalArmorFill =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        shader =
          LinearGradient(
            cx - r * 0.90f,
            cy - r * 0.98f,
            cx + r * 0.90f,
            cy + r * 0.98f,
            intArrayOf(metalStop0, metalStop1, metalStop2, metalStop3, metalStop4),
            floatArrayOf(0f, 0.20f, 0.48f, 0.78f, 1f),
            Shader.TileMode.CLAMP,
          )
      }
    val basaltFill =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        shader =
          LinearGradient(
            cx - r * 0.92f,
            cy - r * 1.02f,
            cx + r * 0.92f,
            cy + r * 1.02f,
            intArrayOf(rockStop0, rockStop1, rockStop2, rockStop3, rockStop4),
            floatArrayOf(0f, 0.19f, 0.46f, 0.76f, 1f),
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
        strokeWidth = 1.55f * borderMult
        color = Color.argb(205, 6, 11, 22)
      }
    val specHighlightStroke =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
        strokeWidth = (1.85f * rimDepth).coerceIn(1.1f, 3.2f)
        color = Color.argb((220 * rimDepth.coerceIn(0.35f, 1.15f)).roundToInt().coerceIn(75, 248), 248, 250, 252)
      }
    val conduitGlow =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 5.6f * borderMult
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        color =
          Color.argb(
            (122 * glowMult).roundToInt().coerceIn(0, 245),
            Color.red(glowCol),
            Color.green(glowCol),
            Color.blue(glowCol),
          )
      }
    val conduitCore =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2.25f * borderMult
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        color = glowCol
      }
    val hotSpineStroke =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.2f * borderMult
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        color = Color.parseColor("#F8FAFC")
      }

    fun drawRecessedCrater(crX: Float, crY: Float, crR: Float) {
      val pitPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
          style = Paint.Style.FILL
          shader =
            RadialGradient(
              crX - crR * 0.28f,
              crY - crR * 0.30f,
              crR * 1.22f,
              intArrayOf(
                Color.parseColor("#02050D"),
                Color.parseColor("#060D1A"),
                rockStop4,
                rockStop2,
              ),
              floatArrayOf(0f, 0.38f, 0.72f, 1f),
              Shader.TileMode.CLAMP,
            )
        }
      c.drawCircle(crX, crY, crR, pitPaint)
      val craterRect = RectF(crX - crR, crY - crR, crX + crR, crY + crR)
      // Deep AO upper-left crater overhang shadow
      c.drawArc(
        craterRect,
        138f,
        170f,
        false,
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
          style = Paint.Style.STROKE
          strokeWidth = (crR * 0.22f).coerceAtLeast(1.4f)
          strokeCap = Paint.Cap.ROUND
          color = Color.argb(235, 2, 6, 16)
        },
      )
      // Crisp key-lit lower-right crater rim catchlight
      c.drawArc(
        craterRect,
        -38f,
        152f,
        false,
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
          style = Paint.Style.STROKE
          strokeWidth = (crR * 0.17f).coerceAtLeast(1.25f)
          strokeCap = Paint.Cap.ROUND
          color = mixRgb(rockStop0, Color.parseColor("#E2E8F0"), 0.42f)
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
                -r * 0.95f,
                r * 0.9f,
                r * 0.95f,
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
            color = mixRgb(metalStop3, trimCol, 0.32f)
          }
        val finGridStroke =
          Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1.25f
            color = Color.argb(165, Color.red(trimCol), Color.green(trimCol), Color.blue(trimCol))
          }
        for (f in 0 until 4) {
          val fa = f * (PI.toFloat() * 0.5f) + finOffsetRad
          c.save()
          c.rotate(fa * (180f / PI.toFloat()))
          val finRect = RectF(r * 0.79f, -r * 0.088f, r * 0.985f, r * 0.088f)
          c.drawRoundRect(finRect, 4f, 4f, finFill)
          c.drawRoundRect(finRect, 4f, 4f, darkOutline)
          c.drawLine(r * 0.82f, 0f, r * 0.955f, 0f, finGridStroke)
          c.drawLine(r * 0.885f, -r * 0.072f, r * 0.885f, r * 0.072f, finGridStroke)
          c.restore()
        }

        // 2. 4 or 6 Armored Radial Docking Spokes + Docking Clamps with AO shadow
        for (s in 0 until spokeCount) {
          val sa = s * (360f / spokeCount)
          c.save()
          c.rotate(sa)
          val spokeRect = RectF(r * 0.24f, -r * 0.078f, r * 0.86f, r * 0.078f)
          c.drawRoundRect(RectF(spokeRect.left + 2f, spokeRect.top + 2.5f, spokeRect.right + 2f, spokeRect.bottom + 2.5f), 4f, 4f, aoShadowPaint)
          c.drawRoundRect(spokeRect, 4f, 4f, localMetalFill)
          c.drawRoundRect(spokeRect, 4f, 4f, darkOutline)
          c.drawLine(r * 0.30f, -r * 0.048f, r * 0.80f, -r * 0.048f, specHighlightStroke)
          c.drawLine(r * 0.32f, 0f, r * 0.78f, 0f, conduitCore)
          // Outer docking collar pod
          val podRect = RectF(r * 0.83f, -r * 0.108f, r * 0.945f, r * 0.108f)
          c.drawRoundRect(podRect, 4f, 4f, localMetalFill)
          c.drawRoundRect(podRect, 4f, 4f, darkOutline)
          c.restore()
        }

        // 3. Outer Docking Torus (0.86r) with AO drop shadow + 5-stop Metallic Shading + Illuminated Window Arrays
        val outerRingPath =
          Path().apply {
            fillType = Path.FillType.EVEN_ODD
            addCircle(0f, 0f, r * 0.86f, Path.Direction.CW)
            addCircle(0f, 0f, r * 0.67f, Path.Direction.CW)
          }
        c.save()
        c.translate(2.4f, 2.8f)
        c.drawPath(outerRingPath, aoShadowPaint)
        c.restore()
        c.drawPath(outerRingPath, localMetalFill)
        c.drawCircle(0f, 0f, r * 0.86f, darkOutline)
        c.drawCircle(0f, 0f, r * 0.67f, darkOutline)
        c.drawCircle(0f, 0f, r * 0.765f, seamStroke)
        // Upper-left specular rim on outer torus
        val torusSpecRect = RectF(-r * 0.845f, -r * 0.845f, r * 0.845f, r * 0.845f)
        c.drawArc(torusSpecRect, 152f, 150f, false, specHighlightStroke)
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

        // 16 Lit Cyan / Sovereign-Gold Habitat Micro-Windows around the Promenade Deck
        val winPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        for (wIdx in 0 until 16) {
          val wa = (wIdx + 0.5f) * (PI.toFloat() / 8f)
          val wx = cos(wa) * (r * 0.765f)
          val wy = sin(wa) * (r * 0.765f)
          val winCol = if ((wIdx + v) % 3 == 0) Color.parseColor("#FACC15") else glowCol
          winPaint.color = Color.argb(95, Color.red(winCol), Color.green(winCol), Color.blue(winCol))
          c.drawCircle(wx, wy, r * 0.038f, winPaint)
          winPaint.color = winCol
          c.drawCircle(wx, wy, r * 0.023f, winPaint)
          winPaint.color = Color.WHITE
          c.drawCircle(wx, wy, r * 0.010f, winPaint)
        }

        // Inner Command Ring (0.51r)
        val innerRingPath =
          Path().apply {
            fillType = Path.FillType.EVEN_ODD
            addCircle(0f, 0f, r * 0.51f, Path.Direction.CW)
            addCircle(0f, 0f, r * 0.37f, Path.Direction.CW)
          }
        c.save()
        c.translate(1.8f, 2.2f)
        c.drawPath(innerRingPath, aoShadowPaint)
        c.restore()
        c.drawPath(innerRingPath, localMetalFill)
        c.drawCircle(0f, 0f, r * 0.51f, darkOutline)
        c.drawCircle(0f, 0f, r * 0.37f, darkOutline)
        c.drawArc(RectF(-r * 0.495f, -r * 0.495f, r * 0.495f, r * 0.495f), 155f, 140f, false, specHighlightStroke)
        c.drawCircle(
          0f,
          0f,
          r * 0.44f,
          Paint(conduitCore).apply { strokeWidth = 1.65f * borderMult },
        )

        // 4. Central 5-Stop Armored Command Dome + Glowing Reactor Core
        val domeR = r * 0.28f
        c.drawCircle(1.8f, 2.0f, domeR, aoShadowPaint)
        c.drawCircle(0f, 0f, domeR, localMetalFill)
        c.drawCircle(0f, 0f, domeR, darkOutline)
        val coreDomeGlow =
          Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            shader =
              RadialGradient(
                -domeR * 0.24f,
                -domeR * 0.24f,
                domeR * 0.96f,
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
        c.save()
        c.translate(2.5f, 3.0f)
        c.drawPath(rockPath, aoShadowPaint)
        c.restore()
        c.drawPath(rockPath, basaltFill)

        // Sculpted normal-lit basalt facet planes + upper-left sunlit ridge bevel
        val facetPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        val ridgePath = Path()
        for (i in 0 until vCount) {
          val px = rockPtsX[i]
          val py = rockPtsY[i]
          val nx = rockPtsX[(i + 1) % vCount]
          val ny = rockPtsY[(i + 1) % vCount]
          val midX = (px + nx) * 0.5f - cx
          val midY = (py + ny) * 0.5f - cy
          val len = kotlin.math.hypot(midX, midY).coerceAtLeast(1f)
          val ndotl = ((-midX / len) * 0.58f + (-midY / len) * 0.62f + 0.28f).coerceIn(-0.6f, 1f)
          val facetTri =
            Path().apply {
              moveTo(cx, cy)
              lineTo(px, py)
              lineTo(nx, ny)
              close()
            }
          facetPaint.color =
            if (ndotl >= 0.15f) {
              Color.argb((ndotl * 52f).roundToInt().coerceIn(8, 56), 226, 232, 240)
            } else {
              Color.argb(((-ndotl + 0.25f) * 68f).roundToInt().coerceIn(10, 74), 2, 6, 18)
            }
          c.drawPath(facetTri, facetPaint)

          if ((px + nx) * 0.5f <= cx + r * 0.18f && (py + ny) * 0.5f <= cy + r * 0.18f) {
            ridgePath.moveTo(px, py)
            ridgePath.lineTo(nx, ny)
          }
          if (i % 2 == 0) {
            c.drawLine(px, py, cx + (px - cx) * 0.42f, cy + (py - cy) * 0.42f, seamStroke)
          }
        }
        c.drawPath(ridgePath, specHighlightStroke)
        c.drawPath(rockPath, darkOutline)

        // 3 Shaded 3D Recessed Craters on the Basalt Shell
        drawRecessedCrater(cx - r * 0.46f, cy - r * 0.36f, r * 0.15f)
        drawRecessedCrater(cx - r * 0.38f, cy + r * 0.42f, r * 0.18f)
        drawRecessedCrater(cx + r * 0.34f, cy + r * 0.48f, r * 0.13f)

        // Recessed Brushed-Titanium Hangar Bay Trench + Lit Command Windows + Geodesic Sensor Dome
        c.save()
        c.translate(cx, cy)
        c.rotate(v * 18f - 12f)

        val deckCollarRect = RectF(-r * 0.16f, -r * 0.38f, r * 0.68f, r * 0.34f)
        c.drawRoundRect(RectF(deckCollarRect.left + 2.5f, deckCollarRect.top + 3f, deckCollarRect.right + 2.5f, deckCollarRect.bottom + 3f), 9f, 9f, aoShadowPaint)
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
            color = Color.parseColor("#030712")
          },
        )
        c.drawRoundRect(
          trenchRect,
          6f,
          6f,
          Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1.65f * borderMult
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
            color = Color.argb(52, Color.red(glowCol), Color.green(glowCol), Color.blue(glowCol))
          },
        )
        val beaconDot = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        for (b in 0 until 5) {
          val bx = trenchRect.left + 10f + b * ((trenchRect.width() - 20f) / 4f)
          beaconDot.color = glowCol
          c.drawCircle(bx, trenchRect.top + 6.5f, 2.4f, beaconDot)
          c.drawCircle(bx, trenchRect.bottom - 6.5f, 2.4f, beaconDot)
          beaconDot.color = Color.WHITE
          c.drawCircle(bx, (trenchRect.top + trenchRect.bottom) * 0.5f, 1.35f, beaconDot)
          // Lit deck observation windows above & below hangar collar
          beaconDot.color = if (b % 2 == 0) Color.parseColor("#FACC15") else glowCol
          c.drawRect(bx - 2.2f, deckCollarRect.top + 6.5f, bx + 2.2f, deckCollarRect.top + 9.2f, beaconDot)
          c.drawRect(bx - 2.2f, deckCollarRect.bottom - 9.2f, bx + 2.2f, deckCollarRect.bottom - 6.5f, beaconDot)
        }

        // Armored Geodesic Command Sensor Dome + Comm Mast on West Ridge
        val domeCx = -r * 0.36f
        val domeCy = -r * 0.04f
        val domeR = r * 0.24f
        c.drawLine(domeCx - domeR * 0.5f, domeCy - domeR * 0.7f, domeCx - domeR * 1.35f, domeCy - domeR * 1.45f, Paint(specHighlightStroke).apply { strokeWidth = 2.2f })
        beaconDot.color = Color.parseColor("#FACC15")
        c.drawCircle(domeCx - domeR * 1.35f, domeCy - domeR * 1.45f, 3.3f, beaconDot)

        c.drawCircle(domeCx + 2f, domeCy + 2.2f, domeR, aoShadowPaint)
        c.drawCircle(domeCx, domeCy, domeR, metalArmorFill)
        c.drawCircle(domeCx, domeCy, domeR, darkOutline)
        c.drawArc(RectF(domeCx - domeR * 0.9f, domeCy - domeR * 0.9f, domeCx + domeR * 0.9f, domeCy + domeR * 0.9f), 160f, 130f, false, specHighlightStroke)
        c.drawLine(domeCx - domeR * 0.78f, domeCy, domeCx + domeR * 0.78f, domeCy, seamStroke)
        c.drawLine(domeCx, domeCy - domeR * 0.78f, domeCx, domeCy + domeR * 0.78f, seamStroke)
        beaconDot.color = glowCol
        c.drawCircle(domeCx, domeCy, domeR * 0.36f, beaconDot)
        beaconDot.color = Color.parseColor("#F8FAFC")
        c.drawCircle(domeCx - domeR * 0.08f, domeCy - domeR * 0.08f, domeR * 0.16f, beaconDot)
        c.restore()
      }

      SpaceObstacleType.CRYSTAL_ASTEROID -> {
        // Organic Multi-Faceted Normal-Lit Basalt Boulder + Deep AO Craters + Internal-Glowing Crystalline Rift Veins
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

        c.save()
        c.translate(2.5f, 3.0f)
        c.drawPath(outerRock, aoShadowPaint)
        c.restore()
        c.drawPath(outerRock, basaltFill)

        // Per-facet directional key-light normal shading across basalt skirt quads
        val facetShadePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        for (i in 0 until n) {
          val j = (i + 1) % n
          val mx = (vx[i] + vx[j] + ix[i] + ix[j]) * 0.25f - cx
          val my = (vy[i] + vy[j] + iy[i] + iy[j]) * 0.25f - cy
          val mLen = kotlin.math.hypot(mx, my).coerceAtLeast(1f)
          val ndotl = ((-mx / mLen) * 0.58f + (-my / mLen) * 0.62f + 0.24f).coerceIn(-0.65f, 1f)
          val quad =
            Path().apply {
              moveTo(vx[i], vy[i])
              lineTo(vx[j], vy[j])
              lineTo(ix[j], iy[j])
              lineTo(ix[i], iy[i])
              close()
            }
          facetShadePaint.color =
            if (ndotl >= 0.12f) {
              Color.argb((ndotl * 62f).roundToInt().coerceIn(8, 66), 226, 232, 240)
            } else {
              Color.argb(((-ndotl + 0.22f) * 78f).roundToInt().coerceIn(12, 84), 2, 5, 15)
            }
          c.drawPath(quad, facetShadePaint)
        }

        // Elevated sunlit upper-left plateau + faceted ridge lines
        c.drawPath(
          innerPlateau,
          Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            shader =
              LinearGradient(
                cx - r * 0.65f,
                cy - r * 0.68f,
                cx + r * 0.65f,
                cy + r * 0.68f,
                intArrayOf(
                  mixRgb(rockStop0, Color.parseColor("#CBD5E1"), 0.36f),
                  rockStop1,
                  rockStop3,
                ),
                floatArrayOf(0f, 0.46f, 1f),
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

        // Subsurface Internal-Glowing Crystalline Rift Halo + Branching Fissure Veins
        c.drawCircle(
          cx,
          cy,
          r * 0.52f,
          Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            shader =
              RadialGradient(
                cx,
                cy,
                r * 0.52f,
                intArrayOf(
                  Color.argb((84 * glowMult).roundToInt().coerceIn(0, 185), Color.red(glowCol), Color.green(glowCol), Color.blue(glowCol)),
                  Color.argb((28 * glowMult).roundToInt().coerceIn(0, 95), 168, 85, 247),
                  Color.argb(0, 0, 0, 0),
                ),
                floatArrayOf(0f, 0.55f, 1f),
                Shader.TileMode.CLAMP,
              )
          },
        )
        val veinPath = Path()
        val branchCount = 3 + (v % 2)
        for (b in 0 until branchCount) {
          val ba = crOff + b * (PI.toFloat() * 2f / branchCount)
          val midR = r * 0.35f
          val tipR = r * 0.66f
          val mx = cx + cos(ba + 0.22f) * midR
          val my = cy + sin(ba + 0.22f) * midR
          val tx = cx + cos(ba - 0.14f) * tipR
          val ty = cy + sin(ba - 0.14f) * tipR
          veinPath.moveTo(cx, cy)
          veinPath.lineTo(mx, my)
          veinPath.lineTo(tx, ty)
          val fx = mx + cos(ba + 0.72f) * (r * 0.25f)
          val fy = my + sin(ba + 0.72f) * (r * 0.25f)
          veinPath.moveTo(mx, my)
          veinPath.lineTo(fx, fy)
        }
        c.drawPath(veinPath, conduitGlow)
        c.drawPath(veinPath, conduitCore)
        c.drawPath(veinPath, hotSpineStroke)

        // Protruding Faceted Rift Crystal Cluster at Core
        val shardFill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        for (s in 0 until 4) {
          val sa = crOff + s * 1.57f
          val tipLen = if (s % 2 == 0) r * 0.26f else r * 0.20f
          val tipX = cx + cos(sa) * tipLen
          val tipY = cy + sin(sa) * tipLen
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
          shardFill.color = if (s % 2 == 0) glowCol else mixRgb(glowCol, Color.parseColor("#A855F7"), 0.45f)
          c.drawPath(shard, shardFill)
          c.drawPath(shard, hotSpineStroke)
        }
      }

      SpaceObstacleType.FLOATING_DEBRIS -> {
        // 4 Distinct Sheared Starship Hull & Broken Reactor-Pylon Wreckage Silhouettes
        c.save()
        c.translate(cx, cy)
        c.rotate(v * 34f - 22f + styleIdx * 14f)

        val localMetalFill =
          Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            shader =
              LinearGradient(
                -r * 0.85f,
                -r * 0.78f,
                r * 0.85f,
                r * 0.78f,
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
            color = Color.argb(185, 6, 10, 20)
          }

        for (ribIdx in -2..2) {
          val ry = ribIdx * (r * 0.14f)
          c.drawLine(r * 0.28f, ry, r * 0.82f - kotlin.math.abs(ribIdx) * r * 0.08f, ry + ribIdx * 3f, ribPaint)
        }

        val hullWreck = Path()
        when (v) {
          0 -> {
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
          1 -> {
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
          2 -> {
            hullWreck.moveTo(-r * 0.88f, 0f)
            hullWreck.lineTo(-r * 0.18f, -r * 0.56f)
            hullWreck.lineTo(r * 0.52f, -r * 0.32f)
            hullWreck.lineTo(r * 0.34f, r * 0.02f)
            hullWreck.lineTo(r * 0.62f, r * 0.28f)
            hullWreck.lineTo(-r * 0.14f, r * 0.58f)
            hullWreck.close()
          }
          else -> {
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

        c.save()
        c.translate(2.2f, 2.6f)
        c.drawPath(hullWreck, aoShadowPaint)
        c.restore()
        c.drawPath(hullWreck, localMetalFill)
        c.drawCircle(r * 0.34f, r * 0.04f, r * 0.22f, scorchPaint)
        c.drawPath(hullWreck, darkOutline)
        c.drawLine(-r * 0.68f, -r * 0.16f, r * 0.24f, -r * 0.34f, specHighlightStroke)
        c.drawLine(-r * 0.52f, r * 0.18f, r * 0.20f, r * 0.18f, seamStroke)

        // Micro-rivets & surviving illuminated deck viewports along bulkhead
        val sparkDot = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        for (w in 0 until 3) {
          val wx = -r * 0.42f + w * r * 0.20f
          sparkDot.color = if (w == 1) Color.parseColor("#FACC15") else glowCol
          c.drawRect(wx - 2.6f, -r * 0.26f, wx + 2.6f, -r * 0.21f, sparkDot)
        }

        // Broken Glowing Cyan Conduit + Amber Spark Nodes at the Tear Edge
        c.drawLine(-r * 0.50f, -r * 0.04f, r * 0.28f, -r * 0.04f, conduitGlow)
        c.drawLine(-r * 0.50f, -r * 0.04f, r * 0.28f, -r * 0.04f, conduitCore)
        sparkDot.color = glowCol
        c.drawCircle(-r * 0.48f, -r * 0.04f, 3.5f, sparkDot)
        sparkDot.color = Color.parseColor("#F59E0B")
        c.drawCircle(r * 0.32f, -r * 0.04f, 4.1f, sparkDot)
        sparkDot.color = Color.WHITE
        c.drawCircle(r * 0.32f, -r * 0.04f, 2.0f, sparkDot)
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
    val alliedBmp = bakeCustomShip9Bitmap(recipe, 256, isAllied = true, hullTrimStyle = 0)
    val hostileBmp = bakeCustomShip9Bitmap(recipe, 256, isAllied = false, hullTrimStyle = 0)
    synchronized(cacheLock) {
      lastCustomShip9Recipe = recipe
      customShip9AlliedBitmap = alliedBmp
      customShip9HostileBitmap = hostileBmp
      val iter = trimmedShipSpriteCache.keys.iterator()
      while (iter.hasNext()) {
        val k = iter.next()
        if (((k ushr 8) and 0xFF) == 10) {
          iter.remove()
        }
      }
      customShip9Version++
      spriteVisualRevision++
    }
  }

  fun bakeCustomShip9Bitmap(
    recipe: CustomHullVisualRecipe,
    sizePx: Int = 256,
    isAllied: Boolean = true,
    hullTrimStyle: Int = 0,
  ): Bitmap {
    val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val c = Canvas(bmp)
    drawCustomModularShipOnCanvas(c, sizePx / 2f, sizePx / 2f, sizePx * 0.39f, recipe, isAllied, hullTrimStyle)
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
            r * 0.94f,
            intArrayOf(
              Color.argb(alpha, red, green, blue),
              Color.argb((alpha * 0.45f).toInt(), red, green, blue),
              Color.argb(0, red, green, blue),
            ),
            floatArrayOf(0f, 0.44f, 1f),
            Shader.TileMode.CLAMP,
          )
      }
    c.drawCircle(r, r, r * 0.94f, paint)
    return bmp
  }

  private fun bakeFeatheredNebulaStamp(
    size: Int,
    deepColor: Int,
    midColor: Int,
    peakAlpha: Int,
    seed: Int,
  ): Bitmap {
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val c = Canvas(bmp)
    val cx = size * 0.5f
    val cy = size * 0.5f
    val baseR = size * 0.45f
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    val dR = Color.red(deepColor)
    val dG = Color.green(deepColor)
    val dB = Color.blue(deepColor)
    val mR = Color.red(midColor)
    val mG = Color.green(midColor)
    val mB = Color.blue(midColor)

    for (lobe in 0 until 7) {
      val ang = seed * 1.37f + lobe * 0.92f
      val dist = if (lobe == 0) 0f else baseR * (0.16f + (lobe % 3) * 0.11f)
      val lx = cx + cos(ang) * dist
      val ly = cy + sin(ang) * (dist * 0.82f)
      val lr = baseR * (if (lobe == 0) 0.92f else 0.58f + (lobe % 2) * 0.14f)
      val useMid = lobe % 2 == 1
      val cR = if (useMid) mR else dR
      val cG = if (useMid) mG else dG
      val cB = if (useMid) mB else dB
      val lobeAlpha = if (lobe == 0) peakAlpha else (peakAlpha * 0.68f).roundToInt()
      paint.shader =
        RadialGradient(
          lx,
          ly,
          lr,
          intArrayOf(
            Color.argb(lobeAlpha, cR, cG, cB),
            Color.argb((lobeAlpha * 0.52f).roundToInt(), cR, cG, cB),
            Color.argb((lobeAlpha * 0.14f).roundToInt(), dR, dG, dB),
            Color.argb(0, dR, dG, dB),
          ),
          floatArrayOf(0f, 0.38f, 0.72f, 1f),
          Shader.TileMode.CLAMP,
        )
      c.save()
      c.translate(lx, ly)
      c.rotate((seed * 27f + lobe * 31f) % 360f)
      c.scale(1.0f, 0.72f + (lobe % 3) * 0.10f)
      c.translate(-lx, -ly)
      c.drawCircle(lx, ly, lr, paint)
      c.restore()
    }
    return bmp
  }

  private fun bakeDistantSpiralGalaxyStamp(size: Int): Bitmap {
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val c = Canvas(bmp)
    val cx = size * 0.5f
    val cy = size * 0.5f
    val maxR = size * 0.43f
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

    c.save()
    c.translate(cx, cy)
    c.rotate(-24f)
    c.scale(1.0f, 0.54f)

    // Soft outer indigo-violet galactic dust envelope feathered to zero before bitmap edge
    paint.shader =
      RadialGradient(
        0f,
        0f,
        maxR,
        intArrayOf(
          Color.argb(66, 224, 242, 254),
          Color.argb(46, 124, 58, 237),
          Color.argb(24, 30, 27, 75),
          Color.argb(0, 15, 23, 42),
        ),
        floatArrayOf(0f, 0.30f, 0.68f, 1f),
        Shader.TileMode.CLAMP,
      )
    c.drawCircle(0f, 0f, maxR, paint)

    // Two soft logarithmic stardust cloud arms composed of feathered radial puffs (zero lines)
    for (arm in 0..1) {
      val armOffset = arm * PI.toFloat()
      for (step in 1..14) {
        val frac = step / 15f
        val theta = armOffset + frac * 3.8f
        val rad = maxR * (0.10f + frac * 0.74f)
        val px = cos(theta) * rad
        val py = sin(theta) * rad
        val puffR = maxR * (0.20f - frac * 0.08f).coerceAtLeast(0.08f)
        val armAlpha = ((1f - frac) * 38f).roundToInt().coerceIn(6, 42)
        val useCyan = step % 2 == 0
        val rCol = if (useCyan) 56 else 168
        val gCol = if (useCyan) 189 else 85
        val bCol = if (useCyan) 248 else 247
        paint.shader =
          RadialGradient(
            px,
            py,
            puffR,
            intArrayOf(
              Color.argb(armAlpha, rCol, gCol, bCol),
              Color.argb((armAlpha * 0.40f).roundToInt(), rCol, gCol, bCol),
              Color.argb(0, rCol, gCol, bCol),
            ),
            floatArrayOf(0f, 0.48f, 1f),
            Shader.TileMode.CLAMP,
          )
        c.drawCircle(px, py, puffR, paint)
      }
    }

    // Warm-white softly-lit galactic core glow
    paint.shader =
      RadialGradient(
        0f,
        0f,
        maxR * 0.28f,
        intArrayOf(
          Color.argb(96, 254, 249, 195),
          Color.argb(44, 186, 230, 253),
          Color.argb(0, 56, 189, 248),
        ),
        floatArrayOf(0f, 0.45f, 1f),
        Shader.TileMode.CLAMP,
      )
    c.drawCircle(0f, 0f, maxR * 0.28f, paint)
    c.restore()
    return bmp
  }

  private fun bakeDistantCelestialMoonStamp(size: Int): Bitmap {
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val c = Canvas(bmp)
    val cx = size * 0.5f
    val cy = size * 0.5f
    val moonR = size * 0.26f
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

    // Feathered atmospheric scatter halo
    paint.shader =
      RadialGradient(
        cx - moonR * 0.22f,
        cy - moonR * 0.22f,
        moonR * 1.65f,
        intArrayOf(
          Color.argb(38, 56, 189, 248),
          Color.argb(16, 30, 58, 138),
          Color.argb(0, 2, 6, 23),
        ),
        floatArrayOf(0f, 0.55f, 1f),
        Shader.TileMode.CLAMP,
      )
    c.drawCircle(cx, cy, moonR * 1.55f, paint)

    // Softly-lit crescent sphere shaded into deep-space shadow
    paint.shader =
      RadialGradient(
        cx - moonR * 0.48f,
        cy - moonR * 0.44f,
        moonR * 1.52f,
        intArrayOf(
          Color.argb(92, 186, 230, 253),
          Color.argb(64, 51, 65, 85),
          Color.argb(46, 15, 23, 42),
          Color.argb(18, 2, 6, 23),
        ),
        floatArrayOf(0f, 0.34f, 0.68f, 1f),
        Shader.TileMode.CLAMP,
      )
    c.drawCircle(cx, cy, moonR, paint)
    return bmp
  }

  fun getShipSprite(
    shipId: Int,
    isAllied: Boolean = true,
    @Suppress("UNUSED_PARAMETER") isHitFlashing: Boolean = false,
    trimStyleOverride: Int = if (isAllied) activeHullTrimStyle else 0,
  ): Bitmap {
    val effectiveTrim = trimStyleOverride.coerceIn(0, 5)
    if (shipId == 10 || shipId > 9) {
      if (effectiveTrim == 0) {
        return if (isAllied) customShip9AlliedBitmap else customShip9HostileBitmap
      }
      val recipeSnapshot: CustomHullVisualRecipe
      val versionSalt: Int
      synchronized(cacheLock) {
        versionSalt = customShip9Version and 0xFFF
        val cacheKey = (versionSalt shl 16) or (10 shl 8) or (effectiveTrim shl 1) or (if (isAllied) 1 else 0)
        trimmedShipSpriteCache[cacheKey]?.let { return it }
        recipeSnapshot = lastCustomShip9Recipe ?: RiftCodex.activeCustomShipSlot.visualRecipe
      }
      val baked = bakeCustomShip9Bitmap(recipeSnapshot, 256, isAllied, effectiveTrim)
      val cacheKey = (versionSalt shl 16) or (10 shl 8) or (effectiveTrim shl 1) or (if (isAllied) 1 else 0)
      synchronized(cacheLock) {
        trimmedShipSpriteCache[cacheKey]?.let { return it }
        trimmedShipSpriteCache[cacheKey] = baked
        return baked
      }
    }
    val clampedShip = shipId.coerceIn(1, 9)
    if (effectiveTrim != 0) {
      val cacheKey = (clampedShip shl 8) or (effectiveTrim shl 1) or (if (isAllied) 1 else 0)
      synchronized(cacheLock) {
        trimmedShipSpriteCache[cacheKey]?.let { return it }
      }
      val baked = bakeShipBitmap(RiftCodex.getShipById(clampedShip), 256, isAllied, effectiveTrim)
      synchronized(cacheLock) {
        trimmedShipSpriteCache[cacheKey]?.let { return it }
        trimmedShipSpriteCache[cacheKey] = baked
        return baked
      }
    }
    val map = if (isAllied) alliedShipBitmaps else hostileShipBitmaps
    return map[clampedShip] ?: map[1] ?: bakeShipBitmap(RiftCodex.getShipById(clampedShip), 256, isAllied)
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
   * Bucket A (Special Buff Slot Pickups — 0 Flux Cost):
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

  private fun lerpColorRgb(c1: Int, c2: Int, tRaw: Float, alphaInt: Int = 255): Int {
    val t = tRaw.coerceIn(0f, 1f)
    val inv = 1f - t
    val r = (Color.red(c1) * inv + Color.red(c2) * t).roundToInt().coerceIn(0, 255)
    val g = (Color.green(c1) * inv + Color.green(c2) * t).roundToInt().coerceIn(0, 255)
    val b = (Color.blue(c1) * inv + Color.blue(c2) * t).roundToInt().coerceIn(0, 255)
    return Color.argb(alphaInt.coerceIn(0, 255), r, g, b)
  }

  private fun samplePalette4(
    deepCol: Int,
    midCol: Int,
    brightCol: Int,
    coreCol: Int,
    posRaw: Float,
    alphaInt: Int = 255,
  ): Int {
    val p = posRaw.coerceIn(0f, 1f)
    return when {
      p < 0.36f -> lerpColorRgb(deepCol, midCol, p / 0.36f, alphaInt)
      p < 0.74f -> lerpColorRgb(midCol, brightCol, (p - 0.36f) / 0.38f, alphaInt)
      else -> lerpColorRgb(brightCol, coreCol, (p - 0.74f) / 0.26f, alphaInt)
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
    @Suppress("UNUSED_PARAMETER") parallaxOffsetX: Float = 0f,
    @Suppress("UNUSED_PARAMETER") parallaxOffsetY: Float = 0f,
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
    val turb = (rec?.shaderTurbulence ?: 1f).coerceIn(0.15f, 2.2f)
    val glowMult = (rec?.glowIntensity ?: 1f).coerceIn(0.25f, 2.4f)
    val borderW = (rec?.borderWeight ?: 1f).coerceIn(0.45f, 2.2f)

    when (type) {
      AnomalyType.PLASMA_RIFT -> {
        // Fixed Palette: Deep Crimson #991B1B -> Hot Scarlet #EF4444 -> Electric Magenta #EC4899 -> Rose-White #FFE4E6
        val deepCol = if (useCustomColors) rec!!.primaryColorInt else Color.rgb(153, 27, 27)
        val midCol = if (useCustomColors) rec!!.trimColorInt else Color.rgb(239, 68, 68)
        val brightCol = if (useCustomColors) rec!!.glowColorInt else Color.rgb(236, 72, 153)
        val coreCol = Color.rgb(255, 228, 230)
        drawVolumetricPlayfieldRiftInternal(
          canvas = canvas,
          cx = cx,
          cy = cy,
          r = rScaled,
          spinAngle = spinAngle,
          fadeAlpha = effFade,
          deepColor = deepCol,
          midColor = midCol,
          brightColor = brightCol,
          coreColor = coreCol,
          inwardSpiral = false,
          pulsePhaseOverride = pulsePhaseOverride,
          turbulence = turb,
          glowIntensity = glowMult,
          borderWeight = borderW,
          boltCount = 6,
          seedOffset = 1.1f,
        )
      }
      AnomalyType.REPULSOR_RIFT -> {
        // Locked Pure Ice/Sky-Blue Palette (zero purple/violet tint): Deep Cobalt #0369A1 -> Electric Sky #0EA5E9 -> Ice Blue #7DD3FC -> White-Blue #F0F9FF
        val deepCol = Color.rgb(3, 105, 161)
        val midCol = Color.rgb(14, 165, 233)
        val brightCol = Color.rgb(125, 211, 252)
        val coreCol = Color.rgb(240, 249, 255)
        drawVolumetricPlayfieldRiftInternal(
          canvas = canvas,
          cx = cx,
          cy = cy,
          r = rScaled,
          spinAngle = -spinAngle * 1.12f,
          fadeAlpha = effFade,
          deepColor = deepCol,
          midColor = midCol,
          brightColor = brightCol,
          coreColor = coreCol,
          inwardSpiral = false,
          pulsePhaseOverride = pulsePhaseOverride,
          turbulence = turb,
          glowIntensity = glowMult,
          borderWeight = borderW,
          boltCount = 6,
          seedOffset = 2.7f,
        )
      }
      AnomalyType.GRAVITY_RIFT -> {
        // Fixed Palette: Deep Indigo #3730A3 -> Royal Violet #7C3AED -> Luminous Purple #A855F7 -> Lavender-White #F3E8FF
        val deepCol = if (useCustomColors) rec!!.primaryColorInt else Color.rgb(55, 48, 163)
        val midCol = if (useCustomColors) rec!!.trimColorInt else Color.rgb(124, 58, 237)
        val brightCol = if (useCustomColors) rec!!.glowColorInt else Color.rgb(168, 85, 247)
        val coreCol = Color.rgb(243, 232, 255)
        val extraBolts = min(2, absorbedCount.coerceAtLeast(0) / 2)
        val massGlowBoost = (1f + min(6, absorbedCount.coerceAtLeast(0)) * 0.06f).coerceAtMost(1.45f)
        drawVolumetricPlayfieldRiftInternal(
          canvas = canvas,
          cx = cx,
          cy = cy,
          r = rScaled,
          spinAngle = spinAngle * 1.25f,
          fadeAlpha = effFade,
          deepColor = deepCol,
          midColor = midCol,
          brightColor = brightCol,
          coreColor = coreCol,
          inwardSpiral = true,
          pulsePhaseOverride = pulsePhaseOverride,
          turbulence = (turb * (1f + extraBolts * 0.08f)).coerceAtMost(2.2f),
          glowIntensity = (glowMult * massGlowBoost).coerceAtMost(2.4f),
          borderWeight = borderW,
          boltCount = 6 + extraBolts,
          seedOffset = 4.3f,
        )
      }
      AnomalyType.PHASE_RIFT_A,
      AnomalyType.PHASE_RIFT_B -> {
        // Fixed Palette: Deep Teal #0F766E -> Turquoise #06B6D4 -> Electric Mint-Cyan #00F5D4 -> Pale Cyan-White #CCFBF1
        val isNodeB = type == AnomalyType.PHASE_RIFT_B
        val deepCol = if (useCustomColors) rec!!.primaryColorInt else Color.rgb(15, 118, 110)
        val midCol = if (useCustomColors) rec!!.trimColorInt else Color.rgb(6, 182, 212)
        val brightCol = if (useCustomColors) rec!!.glowColorInt else Color.rgb(0, 245, 212)
        val coreCol = Color.rgb(204, 251, 241)
        drawVolumetricPlayfieldRiftInternal(
          canvas = canvas,
          cx = cx,
          cy = cy,
          r = rScaled,
          spinAngle = if (isNodeB) -spinAngle else spinAngle,
          fadeAlpha = effFade,
          deepColor = deepCol,
          midColor = midCol,
          brightColor = brightCol,
          coreColor = coreCol,
          inwardSpiral = false,
          pulsePhaseOverride = pulsePhaseOverride,
          turbulence = turb,
          glowIntensity = glowMult,
          borderWeight = borderW,
          boltCount = 5,
          seedOffset = if (isNodeB) 6.9f else 5.6f,
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
    parallaxOffsetX: Float = 0f,
    parallaxOffsetY: Float = 0f,
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
      parallaxOffsetX = parallaxOffsetX,
      parallaxOffsetY = parallaxOffsetY,
    )
  }

  /**
   * Volumetric Storm-Cloud & Color-Shifting Fractal Branching Lightning Playfield Rift:
   * - True-centered at `(cx, cy)` with at most `<= 0.025r` zero-mean organic breathing drift.
   * - Centered `#000000` -> `#02040A` abyssal void throat (`0.0r .. 0.42r`) feathered into
   *   multi-layered billowing storm clouds (`0.30r .. 1.08r`).
   * - Hardware AGSL `VOLUMETRIC_RIFT_AGSL` (3-octave Quintic domain-warped FBM) on API 33+
   *   and zero-allocation feathered `RadialGradient` cloud-puff Canvas renderer on API 24-32.
   * - 5-8 midpoint-displaced fractal branching electric lightning arcs (`0.68r .. 1.06r`) with
   *   2-3 thinner forked sub-branches shifting smoothly within the Rift's fixed palette.
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
    @Suppress("UNUSED_PARAMETER") darkVoidCenter: Boolean,
    inwardSpiral: Boolean,
    pulsePhaseOverride: Float? = null,
    @Suppress("UNUSED_PARAMETER") parallaxOffsetX: Float = 0f,
    @Suppress("UNUSED_PARAMETER") parallaxOffsetY: Float = 0f,
    turbulence: Float = 1.0f,
    glowIntensity: Float = 1.0f,
    borderWeight: Float = 1.0f,
  ) {
    val deepColor =
      Color.rgb(
        (Color.red(outerColor) * 0.58f).roundToInt().coerceIn(0, 255),
        (Color.green(outerColor) * 0.58f).roundToInt().coerceIn(0, 255),
        (Color.blue(outerColor) * 0.58f).roundToInt().coerceIn(0, 255),
      )
    drawVolumetricPlayfieldRiftInternal(
      canvas = canvas,
      cx = cx,
      cy = cy,
      r = r,
      spinAngle = spinAngle,
      fadeAlpha = fadeAlpha,
      deepColor = deepColor,
      midColor = outerColor,
      brightColor = midColor,
      coreColor = coreColor,
      inwardSpiral = inwardSpiral,
      pulsePhaseOverride = pulsePhaseOverride,
      turbulence = turbulence,
      glowIntensity = glowIntensity,
      borderWeight = borderWeight,
      boltCount = 6,
      seedOffset = ((outerColor shr 8) and 0xFF) * 0.031f,
    )
  }

  private fun drawVolumetricPlayfieldRiftInternal(
    canvas: Canvas,
    cx: Float,
    cy: Float,
    r: Float,
    spinAngle: Float,
    fadeAlpha: Float,
    deepColor: Int,
    midColor: Int,
    brightColor: Int,
    coreColor: Int,
    inwardSpiral: Boolean,
    pulsePhaseOverride: Float?,
    turbulence: Float,
    glowIntensity: Float,
    borderWeight: Float,
    boltCount: Int,
    seedOffset: Float,
  ) {
    if (r <= 1.2f || fadeAlpha <= 0.01f) return
    val monoSec =
      if (pulsePhaseOverride != null) {
        pulsePhaseOverride * 3.2f
      } else {
        (SystemClock.elapsedRealtimeNanos() - bakerStartElapsedNanos) * 1e-9f
      }
    val turbClamped = turbulence.coerceIn(0.20f, 2.2f)
    val glowClamped = glowIntensity.coerceIn(0.25f, 2.4f)
    val borderClamped = borderWeight.coerceIn(0.45f, 2.2f)
    val inwardSign = if (inwardSpiral) -1f else 1f

    // True-centered Rift core with at most zero-mean organic breathing drift strictly <= 0.025f * r
    val spatialSeed = seedOffset + cx * 0.0029f + cy * 0.0023f
    val driftX = sin(monoSec * 1.1f + spatialSeed) * (0.017f * r)
    val driftY = cos(monoSec * 0.85f + spatialSeed) * (0.017f * r)
    val trueCx = cx + driftX
    val trueCy = cy + driftY

    var drewAgsl = false
    if (Build.VERSION.SDK_INT >= 33 && canvas.isHardwareAccelerated && volumetricRiftShaderPool.isNotEmpty()) {
      val slotIdx = (volumetricRiftShaderCursor++ and 0x7FFFFFFF) % volumetricRiftShaderPool.size
      val rt = volumetricRiftShaderPool[slotIdx] as? RuntimeShader
      if (rt != null) {
        rt.setFloatUniform("uCenter", trueCx, trueCy)
        rt.setFloatUniform("uRadius", r)
        rt.setFloatUniform("uTime", monoSec)
        rt.setFloatUniform("uSpin", spinAngle)
        rt.setFloatUniform("uTurbulence", turbClamped)
        rt.setFloatUniform("uGlow", glowClamped)
        rt.setFloatUniform("uOpacity", fadeAlpha.coerceIn(0.05f, 1.0f))
        rt.setFloatUniform("uInwardSign", inwardSign)
        rt.setFloatUniform("uDeepRGB", Color.red(deepColor) / 255f, Color.green(deepColor) / 255f, Color.blue(deepColor) / 255f)
        rt.setFloatUniform("uMidRGB", Color.red(midColor) / 255f, Color.green(midColor) / 255f, Color.blue(midColor) / 255f)
        rt.setFloatUniform("uBrightRGB", Color.red(brightColor) / 255f, Color.green(brightColor) / 255f, Color.blue(brightColor) / 255f)
        rt.setFloatUniform("uCoreRGB", Color.red(coreColor) / 255f, Color.green(coreColor) / 255f, Color.blue(coreColor) / 255f)
        volumetricRiftShaderPaint.shader = rt
        val boundR = r * 1.24f
        canvas.drawRect(trueCx - boundR, trueCy - boundR, trueCx + boundR, trueCy + boundR, volumetricRiftShaderPaint)
        volumetricRiftShaderPaint.shader = null
        drewAgsl = true
      }
    }

    if (!drewAgsl) {
      drawFeatheredVolumetricRiftCloudsFallback(
        canvas = canvas,
        cx = trueCx,
        cy = trueCy,
        r = r,
        timeSec = monoSec,
        spinAngle = spinAngle,
        fadeAlpha = fadeAlpha,
        deepColor = deepColor,
        midColor = midColor,
        brightColor = brightColor,
        coreColor = coreColor,
        inwardSign = inwardSign,
        glowClamped = glowClamped,
      )
    } else {
      // Subtle foreground volumetric cloud wisps reinforcing 3D depth over the hardware shader
      drawFeatheredStormRingClouds(
        canvas = canvas,
        cx = trueCx,
        cy = trueCy,
        ringRadius = r * 0.72f,
        puffBaseRadius = r * 0.34f,
        puffCount = 7,
        timeSec = monoSec,
        spinRad = spinAngle * 0.42f * inwardSign,
        colorA = midColor,
        colorB = brightColor,
        alphaScale = (0.28f * fadeAlpha * glowClamped.coerceAtMost(1.5f)).coerceIn(0.04f, 0.46f),
      )
    }

    // Organic midpoint-displaced fractal branching electric lightning arcs around 0.68r .. 1.06r
    drawFractalPerimeterLightningArcs(
      canvas = canvas,
      cx = trueCx,
      cy = trueCy,
      radius = r,
      timeSec = monoSec,
      spinRad = spinAngle * 0.35f,
      arcCount = boltCount.coerceIn(5, 8),
      deepColor = deepColor,
      midColor = midColor,
      brightColor = brightColor,
      coreColor = coreColor,
      fadeAlpha = fadeAlpha,
      strokeWeight = borderClamped,
      seedOffset = spatialSeed,
    )
  }

  /**
   * Zero-allocation multi-layer feathered `RadialGradient` storm-cloud renderer for API 24-32
   * and offscreen Bitmap icon baking (`0.0r .. 0.42r` centered `#000000` -> `#02040A` abyssal void
   * throat + `0.30r .. 1.08r` billowing anisotropic volumetric cloud puffs).
   */
  private fun drawFeatheredVolumetricRiftCloudsFallback(
    canvas: Canvas,
    cx: Float,
    cy: Float,
    r: Float,
    timeSec: Float,
    spinAngle: Float,
    fadeAlpha: Float,
    deepColor: Int,
    midColor: Int,
    brightColor: Int,
    coreColor: Int,
    inwardSign: Float,
    glowClamped: Float,
  ) {
    val fill = sharedRiftFillPaint
    val twoPi = PI.toFloat() * 2f

    // 1. Outer diffused nebula mist (0.52r .. 1.08r)
    drawFeatheredStormRingClouds(
      canvas = canvas,
      cx = cx,
      cy = cy,
      ringRadius = r * 0.76f,
      puffBaseRadius = r * 0.44f,
      puffCount = 10,
      timeSec = timeSec,
      spinRad = spinAngle * 0.28f * inwardSign,
      colorA = deepColor,
      colorB = midColor,
      alphaScale = (0.56f * fadeAlpha * glowClamped.coerceAtMost(1.5f)).coerceIn(0.08f, 0.86f),
    )

    // 2. Centered #000000 -> #02040A abyssal void throat backdrop (soft feathered gradient, NEVER a hard circle)
    val voidOuterR = (r * 0.64f).coerceAtLeast(2f)
    sharedCloudMatrix.reset()
    sharedCloudMatrix.setScale(voidOuterR, voidOuterR)
    sharedCloudMatrix.postTranslate(cx, cy)
    unitAbyssalVoidShader.setLocalMatrix(sharedCloudMatrix)
    fill.shader = unitAbyssalVoidShader
    fill.alpha = (252f * fadeAlpha).roundToInt().coerceIn(0, 255)
    canvas.drawRect(cx - voidOuterR, cy - voidOuterR, cx + voidOuterR, cy + voidOuterR, fill)

    // 3. Mid billowing storm-cloud bank (0.44r .. 0.98r) with time-shifting palette highlights
    drawFeatheredStormRingClouds(
      canvas = canvas,
      cx = cx,
      cy = cy,
      ringRadius = r * 0.66f,
      puffBaseRadius = r * 0.36f,
      puffCount = 11,
      timeSec = timeSec * 1.18f + 1.4f,
      spinRad = -spinAngle * 0.44f * inwardSign,
      colorA = midColor,
      colorB = brightColor,
      alphaScale = (0.68f * fadeAlpha).coerceIn(0.08f, 0.92f),
    )

    // 4. Inner luminous cloud wisps hugging the 0.30r .. 0.62r abyssal throat edge
    val innerCount = 8
    for (i in 0 until innerCount) {
      val colorWave = 0.5f + 0.5f * sin(timeSec * 1.65f + i * 1.37f)
      val chosenCol = if (colorWave > 0.62f) coreColor else brightColor
      val puffShader = getUnitCloudPuffShader(chosenCol)
      val ang = spinAngle * 0.62f * inwardSign + i * (twoPi / innerCount.toFloat()) + 0.14f * sin(timeSec * 1.2f + i * 2.1f)
      val dist = r * (0.46f + 0.08f * sin(timeSec * 1.5f + i * 1.9f))
      val px = cx + cos(ang) * dist
      val py = cy + sin(ang) * dist
      val rx = (r * (0.24f + 0.05f * colorWave)).coerceAtLeast(1.5f)
      val ry = (rx * 0.68f).coerceAtLeast(1.2f)
      sharedCloudMatrix.reset()
      sharedCloudMatrix.setScale(rx, ry)
      sharedCloudMatrix.postRotate((ang * 180f / PI.toFloat()) + 90f)
      sharedCloudMatrix.postTranslate(px, py)
      puffShader.setLocalMatrix(sharedCloudMatrix)
      fill.shader = puffShader
      fill.alpha = ((92f + 68f * colorWave) * fadeAlpha * glowClamped.coerceAtMost(1.4f)).roundToInt().coerceIn(0, 230)
      val bound = max(rx, ry) * 1.05f
      canvas.drawRect(px - bound, py - bound, px + bound, py + bound, fill)
    }

    // 5. Re-deepen true center abyssal void throat (0.0r .. 0.42r) so storm clouds frame a pitch-dark centered core
    val innerThroatR = (r * 0.42f).coerceAtLeast(1.5f)
    sharedCloudMatrix.reset()
    sharedCloudMatrix.setScale(innerThroatR, innerThroatR)
    sharedCloudMatrix.postTranslate(cx, cy)
    unitAbyssalVoidShader.setLocalMatrix(sharedCloudMatrix)
    fill.shader = unitAbyssalVoidShader
    fill.alpha = (248f * fadeAlpha).roundToInt().coerceIn(0, 255)
    canvas.drawRect(cx - innerThroatR, cy - innerThroatR, cx + innerThroatR, cy + innerThroatR, fill)
    fill.shader = null
    fill.alpha = 255
  }

  /**
   * Shared zero-allocation feathered `RadialGradient` storm-cloud ring renderer used by
   * Playfield Rifts, Unstable Rifts, Safe-Zone storm rims, Stasis domes, and Outer Storm Wall fallbacks.
   */
  fun drawFeatheredStormRingClouds(
    canvas: Canvas,
    cx: Float,
    cy: Float,
    ringRadius: Float,
    puffBaseRadius: Float,
    puffCount: Int,
    timeSec: Float,
    spinRad: Float,
    colorA: Int,
    colorB: Int,
    alphaScale: Float,
  ) {
    if (ringRadius <= 1.5f || puffBaseRadius <= 1.2f || alphaScale <= 0.01f || puffCount < 3) return
    val fill = sharedRiftFillPaint
    val twoPi = PI.toFloat() * 2f
    val step = twoPi / puffCount.toFloat()
    val shaderA = getUnitCloudPuffShader(colorA)
    val shaderB = getUnitCloudPuffShader(colorB)

    for (i in 0 until puffCount) {
      val wave = 0.5f + 0.5f * sin(timeSec * 1.35f + i * 1.73f)
      val ang = spinRad + i * step + 0.12f * sin(timeSec * 0.92f + i * 2.31f)
      val radJit = ringRadius * (0.94f + 0.10f * cos(timeSec * 1.14f - i * 1.57f))
      val px = cx + cos(ang) * radJit
      val py = cy + sin(ang) * radJit
      val rx = (puffBaseRadius * (0.92f + 0.28f * wave)).coerceAtLeast(1.5f)
      val ry = (puffBaseRadius * (0.66f + 0.20f * (1f - wave))).coerceAtLeast(1.2f)
      val activeShader = if ((i + (timeSec * 0.8f).toInt()) % 2 == 0) shaderA else shaderB

      sharedCloudMatrix.reset()
      sharedCloudMatrix.setScale(rx, ry)
      sharedCloudMatrix.postRotate((ang * 180f / PI.toFloat()) + 90f)
      sharedCloudMatrix.postTranslate(px, py)
      activeShader.setLocalMatrix(sharedCloudMatrix)
      fill.shader = activeShader
      fill.alpha = ((125f + 85f * wave) * alphaScale).roundToInt().coerceIn(0, 245)
      val bound = max(rx, ry) * 1.05f
      canvas.drawRect(px - bound, py - bound, px + bound, py + bound, fill)
    }
    fill.shader = null
    fill.alpha = 255
  }

  /**
   * Renders 5-8 organic, midpoint-displaced fractal branching electric lightning arcs crackling
   * around `0.68r .. 1.06r` (curving tangentially/diagonally across the volumetric storm boundary
   * with 2-3 thinner forked sub-branches, `Paint.Cap.ROUND` & `Paint.Join.ROUND`, wide soft-alpha
   * plasma glow `~0.055r` + crisp bright inner filament `~0.014r`) shifting smoothly within the
   * Rift's fixed color palette. Zero heap allocations per frame.
   */
  fun drawFractalPerimeterLightningArcs(
    canvas: Canvas,
    cx: Float,
    cy: Float,
    radius: Float,
    timeSec: Float,
    spinRad: Float,
    arcCount: Int,
    deepColor: Int,
    midColor: Int,
    brightColor: Int,
    coreColor: Int,
    fadeAlpha: Float,
    strokeWeight: Float = 1.0f,
    seedOffset: Float = 0f,
  ) {
    if (radius <= 3f || fadeAlpha <= 0.02f || arcCount <= 0) return
    val stroke = sharedRiftStrokePaint
    val trunkPath = sharedPrimaryLightningPath
    val forkPath = sharedForkLightningPath
    val ptsX = sharedLightningPtsX
    val ptsY = sharedLightningPtsY
    stroke.shader = null
    stroke.strokeCap = Paint.Cap.ROUND
    stroke.strokeJoin = Paint.Join.ROUND

    val twoPi = PI.toFloat() * 2f
    val count = arcCount.coerceIn(3, 10)
    val weight = strokeWeight.coerceIn(0.55f, 1.85f)
    // Quantize lightning geometry re-seed smoothly by overlapping flicker envelopes (zero snap!)
    val trunkGlowW = (radius * 0.055f * weight).coerceIn(2.0f, 18.0f)
    val trunkCoreW = (radius * 0.014f * weight).coerceIn(0.85f, 4.6f)
    val forkGlowW = (trunkGlowW * 0.62f).coerceAtLeast(1.35f)
    val forkCoreW = (trunkCoreW * 0.66f).coerceAtLeast(0.70f)

    for (bIdx in 0 until count) {
      val arcSeed = seedOffset + bIdx * 2.39996f
      val flickerA = 0.5f + 0.5f * sin(timeSec * 4.7f + bIdx * 1.93f + seedOffset)
      val flickerB = 0.5f + 0.5f * cos(timeSec * 7.3f - bIdx * 2.41f)
      val env = (0.32f + 0.68f * flickerA * (0.55f + 0.45f * flickerB)).coerceIn(0f, 1f)
      if (env < 0.16f) continue

      val palettePos = 0.5f + 0.5f * sin(timeSec * 1.55f + bIdx * 1.18f + seedOffset)
      val boltAlpha = (fadeAlpha * env).coerceIn(0f, 1f)
      val glowCol =
        samplePalette4(
          deepCol = deepColor,
          midCol = midColor,
          brightCol = brightColor,
          coreCol = coreColor,
          posRaw = (0.28f + 0.52f * palettePos).coerceIn(0.15f, 0.85f),
          alphaInt = (124f * boltAlpha).roundToInt().coerceIn(0, 215),
        )
      val innerCol =
        samplePalette4(
          deepCol = deepColor,
          midCol = midColor,
          brightCol = brightColor,
          coreCol = coreColor,
          posRaw = (0.72f + 0.28f * palettePos).coerceIn(0.65f, 1.0f),
          alphaInt = (246f * boltAlpha).roundToInt().coerceIn(0, 255),
        )

      val startAng =
        spinRad +
          bIdx * (twoPi / count.toFloat()) +
          0.22f * sin(timeSec * 0.85f + arcSeed)
      val arcSpan = (if (bIdx % 2 == 0) 1f else -1f) * (0.48f + 0.24f * sin(arcSeed * 1.7f + timeSec * 0.6f))
      val startR = radius * (0.94f + 0.10f * sin(timeSec * 1.9f + arcSeed))
      val endR = radius * (0.72f + 0.14f * cos(timeSec * 1.6f - arcSeed))

      // Build 9-vertex (8-segment) midpoint-displaced curved lightning trunk within 0.68r .. 1.06r
      ptsX[0] = cx + cos(startAng) * startR
      ptsY[0] = cy + sin(startAng) * startR
      ptsX[8] = cx + cos(startAng + arcSpan) * endR
      ptsY[8] = cy + sin(startAng + arcSpan) * endR

      val midAng = startAng + arcSpan * 0.5f
      val midR = (startR + endR) * 0.5f + radius * 0.065f * sin(timeSec * 5.4f + arcSeed * 2.1f)
      ptsX[4] = cx + cos(midAng) * midR.coerceIn(radius * 0.68f, radius * 1.06f)
      ptsY[4] = cy + sin(midAng) * midR.coerceIn(radius * 0.68f, radius * 1.06f)

      // Level-2 midpoints (indices 2 and 6)
      displaceMidpoint(ptsX, ptsY, 0, 4, 2, radius * 0.058f, timeSec * 8.2f + arcSeed * 3.1f)
      displaceMidpoint(ptsX, ptsY, 4, 8, 6, radius * 0.058f, timeSec * 8.9f - arcSeed * 2.7f)

      // Level-3 midpoints (indices 1, 3, 5, 7)
      displaceMidpoint(ptsX, ptsY, 0, 2, 1, radius * 0.030f, timeSec * 11.4f + arcSeed * 1.9f)
      displaceMidpoint(ptsX, ptsY, 2, 4, 3, radius * 0.030f, timeSec * 12.1f - arcSeed * 4.3f)
      displaceMidpoint(ptsX, ptsY, 4, 6, 5, radius * 0.030f, timeSec * 10.7f + arcSeed * 5.1f)
      displaceMidpoint(ptsX, ptsY, 6, 8, 7, radius * 0.030f, timeSec * 11.8f - arcSeed * 3.6f)

      trunkPath.reset()
      trunkPath.moveTo(ptsX[0], ptsY[0])
      for (k in 1..8) {
        trunkPath.lineTo(ptsX[k], ptsY[k])
      }

      // Build 2-3 thinner forked sub-branches splitting off displaced trunk vertices
      forkPath.reset()
      val forkCount = if (bIdx % 2 == 0) 3 else 2
      for (fIdx in 0 until forkCount) {
        val anchorIdx =
          when (fIdx) {
            0 -> 2
            1 -> 5
            else -> 6
          }
        val ax = ptsX[anchorIdx]
        val ay = ptsY[anchorIdx]
        val dirX = ptsX[(anchorIdx + 1).coerceAtMost(8)] - ptsX[(anchorIdx - 1).coerceAtLeast(0)]
        val dirY = ptsY[(anchorIdx + 1).coerceAtMost(8)] - ptsY[(anchorIdx - 1).coerceAtLeast(0)]
        val baseForkAng =
          atan2(dirY, dirX) +
            (if ((bIdx + fIdx) % 2 == 0) 1f else -1f) * (0.46f + 0.16f * sin(timeSec * 6.1f + fIdx * 2.3f + arcSeed))
        val forkLen = radius * (0.16f + 0.08f * ((fIdx + 1) % 2))
        val kx = ax + cos(baseForkAng + 0.18f * sin(timeSec * 9.5f + fIdx + arcSeed)) * (forkLen * 0.54f)
        val ky = ay + sin(baseForkAng + 0.18f * sin(timeSec * 9.5f + fIdx + arcSeed)) * (forkLen * 0.54f)
        var tx = ax + cos(baseForkAng - 0.12f * cos(timeSec * 8.3f - fIdx)) * forkLen
        var ty = ay + sin(baseForkAng - 0.12f * cos(timeSec * 8.3f - fIdx)) * forkLen
        // Clamp fork tip inside 0.64r .. 1.08r so forks never stab into a central core ball
        val tipDist = hypot(tx - cx, ty - cy).coerceAtLeast(0.001f)
        val clampedTipDist = tipDist.coerceIn(radius * 0.64f, radius * 1.08f)
        tx = cx + ((tx - cx) / tipDist) * clampedTipDist
        ty = cy + ((ty - cy) / tipDist) * clampedTipDist

        forkPath.moveTo(ax, ay)
        forkPath.lineTo(kx, ky)
        forkPath.lineTo(tx, ty)
      }

      // Pass 1: Wide soft-alpha colored plasma glow
      stroke.color = glowCol
      stroke.strokeWidth = trunkGlowW
      canvas.drawPath(trunkPath, stroke)
      stroke.strokeWidth = forkGlowW
      canvas.drawPath(forkPath, stroke)

      // Pass 2: Crisp bright inner filament
      stroke.color = innerCol
      stroke.strokeWidth = trunkCoreW
      canvas.drawPath(trunkPath, stroke)
      stroke.strokeWidth = forkCoreW
      canvas.drawPath(forkPath, stroke)
    }
  }

  private fun displaceMidpoint(
    ptsX: FloatArray,
    ptsY: FloatArray,
    i0: Int,
    i1: Int,
    iMid: Int,
    amp: Float,
    phase: Float,
  ) {
    val mx = (ptsX[i0] + ptsX[i1]) * 0.5f
    val my = (ptsY[i0] + ptsY[i1]) * 0.5f
    val dx = ptsX[i1] - ptsX[i0]
    val dy = ptsY[i1] - ptsY[i0]
    val len = hypot(dx, dy).coerceAtLeast(0.001f)
    val nx = -dy / len
    val ny = dx / len
    val disp = amp * sin(phase) * cos(phase * 0.63f + iMid)
    ptsX[iMid] = mx + nx * disp
    ptsY[iMid] = my + ny * disp
  }

  /**
   * Renders soft feathered volumetric plasma ingress wisps along `outerRimR` toward `innerReachR`
   * (zero straight lines, zero circular droplet bulbs, zero rim beads).
   */
  fun drawLocalizedIngressPlumes(
    canvas: Canvas,
    cx: Float,
    cy: Float,
    outerRimR: Float,
    innerReachR: Float,
    plumeCount: Int,
    phaseRad: Float,
    outerColor: Int,
    midColor: Int,
    @Suppress("UNUSED_PARAMETER") coreColor: Int,
    fadeAlpha: Float = 1.0f,
    swirlBias: Float = 0.22f,
  ) {
    if (outerRimR <= 6f || fadeAlpha <= 0.02f || plumeCount <= 0) return
    val fill = sharedRiftFillPaint
    val twoPi = PI.toFloat() * 2f
    val clampedInner = innerReachR.coerceIn(outerRimR * 0.34f, outerRimR * 0.90f)
    val span = (outerRimR - clampedInner).coerceAtLeast(3.5f)
    val shaderOuter = getUnitCloudPuffShader(outerColor)
    val shaderMid = getUnitCloudPuffShader(midColor)

    for (idx in 0 until plumeCount) {
      val bloom = 0.5f + 0.5f * sin(phaseRad * 1.9f + idx * 1.83f)
      val ang = phaseRad + idx * (twoPi / plumeCount.toFloat()) + swirlBias * bloom
      val puffCenterR = outerRimR - span * (0.32f + 0.34f * bloom)
      val px = cx + cos(ang) * puffCenterR
      val py = cy + sin(ang) * puffCenterR
      val rx = (span * (0.48f + 0.22f * bloom)).coerceAtLeast(2.5f)
      val ry = (outerRimR * 0.14f * (0.85f + 0.30f * bloom)).coerceAtLeast(2.0f)
      val activeShader = if (idx % 2 == 0) shaderOuter else shaderMid

      sharedCloudMatrix.reset()
      sharedCloudMatrix.setScale(rx, ry)
      sharedCloudMatrix.postRotate(ang * 180f / PI.toFloat())
      sharedCloudMatrix.postTranslate(px, py)
      activeShader.setLocalMatrix(sharedCloudMatrix)
      fill.shader = activeShader
      fill.alpha = ((58f + 54f * bloom) * fadeAlpha).roundToInt().coerceIn(0, 185)
      val bound = max(rx, ry) * 1.05f
      canvas.drawRect(px - bound, py - bound, px + bound, py + bound, fill)
    }
    fill.shader = null
    fill.alpha = 255
  }

  /**
   * Renders a soft feathered volumetric storm surge puff & organic midpoint-displaced forked arc
   * at `ingressAngleRad` when ordnance or a starfighter crosses a Rift or Stasis boundary
   * (zero straight radial spoke lines or circular bulbs).
   */
  fun drawContactIngressBurstPlume(
    canvas: Canvas,
    cx: Float,
    cy: Float,
    outerRimR: Float,
    ingressAngleRad: Float,
    intensity: Float,
    outerColor: Int,
    midColor: Int,
    coreColor: Int,
  ) {
    val norm = intensity.coerceIn(0f, 1f)
    if (outerRimR <= 6f || norm <= 0.02f) return
    val fill = sharedRiftFillPaint
    val stroke = sharedRiftStrokePaint
    val trunk = sharedPrimaryLightningPath
    stroke.shader = null
    stroke.strokeCap = Paint.Cap.ROUND
    stroke.strokeJoin = Paint.Join.ROUND

    // 1. Feathered volumetric cloud surge puff at the boundary ingress point
    val puffR = (outerRimR * (0.24f + 0.14f * norm)).coerceAtLeast(4f)
    val px = cx + cos(ingressAngleRad) * (outerRimR * 0.86f)
    val py = cy + sin(ingressAngleRad) * (outerRimR * 0.86f)
    val surgeShader = getUnitCloudPuffShader(midColor)
    sharedCloudMatrix.reset()
    sharedCloudMatrix.setScale(puffR, puffR * 0.78f)
    sharedCloudMatrix.postRotate(ingressAngleRad * 180f / PI.toFloat())
    sharedCloudMatrix.postTranslate(px, py)
    surgeShader.setLocalMatrix(sharedCloudMatrix)
    fill.shader = surgeShader
    fill.alpha = (165f * norm).roundToInt().coerceIn(0, 235)
    canvas.drawRect(px - puffR, py - puffR, px + puffR, py + puffR, fill)
    fill.shader = null
    fill.alpha = 255

    // 2. Curved midpoint-displaced branching lightning ripple hugging the outer storm cloud band
    val span = 0.34f + 0.18f * norm
    val a0 = ingressAngleRad - span * 0.5f
    val a1 = ingressAngleRad
    val a2 = ingressAngleRad + span * 0.5f
    val x0 = cx + cos(a0) * (outerRimR * 0.96f)
    val y0 = cy + sin(a0) * (outerRimR * 0.96f)
    val xm = cx + cos(a1) * (outerRimR * 0.78f)
    val ym = cy + sin(a1) * (outerRimR * 0.78f)
    val x1 = cx + cos(a2) * (outerRimR * 0.94f)
    val y1 = cy + sin(a2) * (outerRimR * 0.94f)
    trunk.reset()
    trunk.moveTo(x0, y0)
    trunk.quadTo(xm, ym, x1, y1)

    stroke.strokeWidth = (outerRimR * 0.048f).coerceAtLeast(1.8f)
    stroke.color =
      Color.argb(
        (135f * norm).roundToInt().coerceIn(0, 220),
        Color.red(outerColor),
        Color.green(outerColor),
        Color.blue(outerColor),
      )
    canvas.drawPath(trunk, stroke)

    stroke.strokeWidth = (outerRimR * 0.014f).coerceAtLeast(0.85f)
    stroke.color =
      Color.argb(
        (242f * norm).roundToInt().coerceIn(0, 255),
        Color.red(coreColor),
        Color.green(coreColor),
        Color.blue(coreColor),
      )
    canvas.drawPath(trunk, stroke)
  }

  /**
   * Renders subtle optical specular crescents on physical/crystalline objects (e.g. Flux Core Orb).
   * Never draws central throat circles or droplet beads.
   */
  fun drawRefractiveMeniscusRim(
    canvas: Canvas,
    cx: Float,
    cy: Float,
    rimRadius: Float,
    @Suppress("UNUSED_PARAMETER") throatRadius: Float = 0f,
    lightAngleRad: Float = -2.26f,
    rimTintColor: Int,
    secondaryTintColor: Int,
    fadeAlpha: Float = 1.0f,
    strokeScale: Float = 1.0f,
    @Suppress("UNUSED_PARAMETER") throatOffsetX: Float = 0f,
    @Suppress("UNUSED_PARAMETER") throatOffsetY: Float = 0f,
  ) {
    if (rimRadius <= 5f || fadeAlpha <= 0.02f) return
    val stroke = sharedRiftStrokePaint
    val arcRect = sharedRiftArcRect
    stroke.shader = null

    val tR = Color.red(rimTintColor)
    val tG = Color.green(rimTintColor)
    val tB = Color.blue(rimTintColor)
    val sR = Color.red(secondaryTintColor)
    val sG = Color.green(secondaryTintColor)
    val sB = Color.blue(secondaryTintColor)
    val lightDeg = (lightAngleRad * 180f / PI.toFloat())

    val dispersionR = rimRadius * 0.965f
    arcRect.set(cx - dispersionR, cy - dispersionR, cx + dispersionR, cy + dispersionR)
    stroke.strokeWidth = (rimRadius * 0.026f * strokeScale).coerceAtLeast(1.1f)
    stroke.color = Color.argb((96f * fadeAlpha).roundToInt().coerceIn(0, 255), tR, tG, tB)
    canvas.drawArc(arcRect, lightDeg - 44f, 88f, false, stroke)

    stroke.strokeWidth = (rimRadius * 0.014f * strokeScale).coerceAtLeast(0.85f)
    stroke.color = Color.argb((132f * fadeAlpha).roundToInt().coerceIn(0, 255), sR, sG, sB)
    canvas.drawArc(arcRect, lightDeg + 148f, 64f, false, stroke)
  }

  /**
   * Renders a faceted crystalline precipitate shard (zero circular halos, zero orbiting balls,
   * zero protruding radial spoke lines).
   */
  fun drawPrecipitateCrystalCluster(
    canvas: Canvas,
    cx: Float,
    cy: Float,
    radius: Float,
    angleRad: Float,
    primaryColor: Int,
    coreColor: Int,
    alphaFrac: Float,
    facetSeed: Int,
  ) {
    val normAlpha = alphaFrac.coerceIn(0f, 1f)
    if (radius <= 1.2f || normAlpha <= 0.02f) return
    val fill = sharedRiftFillPaint
    val stroke = sharedRiftStrokePaint
    val path = sharedCrystalPath
    fill.shader = null
    stroke.shader = null

    val pR = Color.red(primaryColor)
    val pG = Color.green(primaryColor)
    val pB = Color.blue(primaryColor)
    val cR = Color.red(coreColor)
    val cG = Color.green(coreColor)
    val cB = Color.blue(coreColor)

    val vertices = if (facetSeed % 2 == 0) 6 else 4
    val step = (PI.toFloat() * 2f) / vertices.toFloat()
    path.reset()
    for (v in 0 until vertices) {
      val a = angleRad + v * step
      val stretch = if (v % 2 == 0) 1.18f else 0.58f
      val vx = cx + cos(a) * (radius * stretch)
      val vy = cy + sin(a) * (radius * stretch)
      if (v == 0) path.moveTo(vx, vy) else path.lineTo(vx, vy)
    }
    path.close()

    fill.color = Color.argb((178f * normAlpha).roundToInt().coerceIn(0, 255), pR, pG, pB)
    canvas.drawPath(path, fill)

    stroke.strokeWidth = (radius * 0.14f).coerceAtLeast(0.95f)
    stroke.color = Color.argb((228f * normAlpha).roundToInt().coerceIn(0, 255), cR, cG, cB)
    canvas.drawPath(path, stroke)
  }

  /**
   * Renders a curved, multi-stop luminous plasma filament trail (outer sheath -> mid plasma -> razor core)
   * connecting a Rift / Stasis / Storm reaction point along its fluid wake (zero tip bead circles).
   */
  fun drawReactiveFilamentStrand(
    canvas: Canvas,
    x0: Float,
    y0: Float,
    ctrlX: Float,
    ctrlY: Float,
    x1: Float,
    y1: Float,
    thickness: Float,
    outerColor: Int,
    midColor: Int,
    coreColor: Int,
    alphaFrac: Float,
  ) {
    val normAlpha = alphaFrac.coerceIn(0f, 1f)
    if (normAlpha <= 0.02f || thickness <= 0.3f) return
    val stroke = sharedRiftStrokePaint
    val path = sharedFilamentPath
    stroke.shader = null
    stroke.strokeCap = Paint.Cap.ROUND
    stroke.strokeJoin = Paint.Join.ROUND

    path.reset()
    path.moveTo(x0, y0)
    path.quadTo(ctrlX, ctrlY, x1, y1)

    stroke.strokeWidth = (thickness * 2.10f).coerceAtLeast(2.0f)
    stroke.color =
      Color.argb(
        (88f * normAlpha).roundToInt().coerceIn(0, 255),
        Color.red(outerColor),
        Color.green(outerColor),
        Color.blue(outerColor),
      )
    canvas.drawPath(path, stroke)

    stroke.strokeWidth = thickness.coerceAtLeast(1.1f)
    stroke.color =
      Color.argb(
        (192f * normAlpha).roundToInt().coerceIn(0, 255),
        Color.red(midColor),
        Color.green(midColor),
        Color.blue(midColor),
      )
    canvas.drawPath(path, stroke)

    stroke.strokeWidth = (thickness * 0.40f).coerceAtLeast(0.80f)
    stroke.color =
      Color.argb(
        (242f * normAlpha).roundToInt().coerceIn(0, 255),
        Color.red(coreColor),
        Color.green(coreColor),
        Color.blue(coreColor),
      )
    canvas.drawPath(path, stroke)
  }

  /**
   * Renders the converged 10.0s multicolored `Unstable Rift`:
   * - Swirling multi-color volumetric storm body (`#00F5D4`, `#38BDF8`, `#A855F7`, `#EC4899`)
   * - Centered `#000000` -> `#02040A` abyssal void throat locked at `(cx, cy)` (`<= 0.025r` drift)
   * - Zero straight/rigid lines, zero spokes, zero central balls
   * - Organic midpoint-displaced fractal branching electric lightning arcs around `0.68r .. 1.06r`
   */
  fun drawUnstableMergedRiftOnCanvas(
    canvas: Canvas,
    cx: Float,
    cy: Float,
    r: Float,
    spinAngle: Float,
    pulsePhase: Float = 0.5f,
    fadeAlpha: Float = 1f,
    @Suppress("UNUSED_PARAMETER") parallaxOffsetX: Float = 0f,
    @Suppress("UNUSED_PARAMETER") parallaxOffsetY: Float = 0f,
  ) {
    if (r <= 1f || fadeAlpha <= 0.01f) return
    val monoSec =
      if (pulsePhase != 0.5f) {
        pulsePhase * 3.4f
      } else {
        (SystemClock.elapsedRealtimeNanos() - bakerStartElapsedNanos) * 1e-9f
      }
    val seed = 8.4f + cx * 0.0027f + cy * 0.0021f
    val trueCx = cx + sin(monoSec * 1.1f + seed) * (0.017f * r)
    val trueCy = cy + cos(monoSec * 0.85f + seed) * (0.017f * r)

    val mintCyan = Color.rgb(0, 245, 212)   // #00F5D4
    val skyBlue = Color.rgb(56, 189, 248)   // #38BDF8
    val violet = Color.rgb(168, 85, 247)    // #A855F7
    val magenta = Color.rgb(236, 72, 153)   // #EC4899
    val paleCore = Color.rgb(240, 253, 250)

    // Primary volumetric storm-cloud body (violet -> magenta -> cyan) centered at (trueCx, trueCy)
    drawVolumetricPlayfieldRiftInternal(
      canvas = canvas,
      cx = cx,
      cy = cy,
      r = r,
      spinAngle = spinAngle * 0.52f,
      fadeAlpha = fadeAlpha,
      deepColor = Color.rgb(59, 7, 100),
      midColor = violet,
      brightColor = magenta,
      coreColor = paleCore,
      inwardSpiral = true,
      pulsePhaseOverride = if (pulsePhase != 0.5f) pulsePhase else null,
      turbulence = 1.28f,
      glowIntensity = 1.24f,
      borderWeight = 1.12f,
      boltCount = 5,
      seedOffset = 8.4f,
    )

    // Counter-swirling #00F5D4 & #38BDF8 feathered volumetric storm-cloud shelves around the centered throat
    drawFeatheredStormRingClouds(
      canvas = canvas,
      cx = trueCx,
      cy = trueCy,
      ringRadius = r * 0.68f,
      puffBaseRadius = r * 0.36f,
      puffCount = 10,
      timeSec = monoSec * 1.15f,
      spinRad = -spinAngle * 0.46f,
      colorA = mintCyan,
      colorB = skyBlue,
      alphaScale = (0.58f * fadeAlpha).coerceIn(0.08f, 0.88f),
    )

    // Re-anchor deep centered #000000 -> #02040A abyssal throat so counter-clouds never wash out the core
    val innerVoidR = (r * 0.39f).coerceAtLeast(1.5f)
    val fill = sharedRiftFillPaint
    sharedCloudMatrix.reset()
    sharedCloudMatrix.setScale(innerVoidR, innerVoidR)
    sharedCloudMatrix.postTranslate(trueCx, trueCy)
    unitAbyssalVoidShader.setLocalMatrix(sharedCloudMatrix)
    fill.shader = unitAbyssalVoidShader
    fill.alpha = (236f * fadeAlpha).roundToInt().coerceIn(0, 255)
    canvas.drawRect(trueCx - innerVoidR, trueCy - innerVoidR, trueCx + innerVoidR, trueCy + innerVoidR, fill)
    fill.shader = null
    fill.alpha = 255

    // Secondary cyan -> sky -> lavender fractal branching lightning arcs interwoven around 0.68r .. 1.06r
    drawFractalPerimeterLightningArcs(
      canvas = canvas,
      cx = trueCx,
      cy = trueCy,
      radius = r * 1.01f,
      timeSec = monoSec * 1.08f + 1.7f,
      spinRad = -spinAngle * 0.42f,
      arcCount = 5,
      deepColor = Color.rgb(15, 118, 110),
      midColor = skyBlue,
      brightColor = mintCyan,
      coreColor = Color.rgb(240, 249, 255),
      fadeAlpha = fadeAlpha,
      strokeWeight = 1.08f,
      seedOffset = 13.7f,
    )
  }

  fun drawAnimatedCodexRiftPreview(
    canvas: Canvas,
    riftId: String,
    cx: Float,
    cy: Float,
    radiusPx: Float,
    pulsePhase: Float,
    spinAngle: Float,
  ) {
    when (riftId) {
      "plasma_rift" ->
        draw3dPlayfieldRift(canvas, AnomalyType.PLASMA_RIFT, cx, cy, radiusPx, spinAngle = spinAngle, pulsePhaseOverride = pulsePhase)
      "repulsor_rift" ->
        draw3dPlayfieldRift(canvas, AnomalyType.REPULSOR_RIFT, cx, cy, radiusPx, spinAngle = spinAngle, pulsePhaseOverride = pulsePhase)
      "gravity_rift" ->
        draw3dPlayfieldRift(canvas, AnomalyType.GRAVITY_RIFT, cx, cy, radiusPx, spinAngle = spinAngle, pulsePhaseOverride = pulsePhase)
      "unstable_rift" ->
        drawUnstableMergedRiftOnCanvas(canvas, cx, cy, radiusPx * 1.02f, spinAngle = spinAngle, pulsePhase = pulsePhase)
      else -> {
        val c1x = cx - radiusPx * 0.44f
        val c1y = cy - radiusPx * 0.34f
        val c2x = cx + radiusPx * 0.44f
        val c2y = cy + radiusPx * 0.34f
        val nodeR = radiusPx * 0.56f
        val sway = sin(pulsePhase * 6.283f) * radiusPx * 0.16f
        drawReactiveFilamentStrand(
          canvas = canvas,
          x0 = c1x,
          y0 = c1y,
          ctrlX = cx + sway,
          ctrlY = cy - sway,
          x1 = c2x,
          y1 = c2y,
          thickness = (radiusPx * 0.09f).coerceAtLeast(1.8f),
          outerColor = Color.rgb(15, 118, 110),
          midColor = Color.rgb(6, 182, 212),
          coreColor = Color.rgb(204, 251, 241),
          alphaFrac = 0.92f,
        )
        draw3dPlayfieldRift(canvas, AnomalyType.PHASE_RIFT_A, c1x, c1y, nodeR, spinAngle = spinAngle, pulsePhaseOverride = pulsePhase)
        draw3dPlayfieldRift(canvas, AnomalyType.PHASE_RIFT_B, c2x, c2y, nodeR, spinAngle = -spinAngle, pulsePhaseOverride = (pulsePhase + 0.35f) % 1f)
      }
    }
  }

  private fun bakeRiftAnomalyIconBitmap(riftId: String, sizePx: Int = 96): Bitmap {
    val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val c = Canvas(bmp)
    val cx = sizePx * 0.5f
    val cy = sizePx * 0.5f
    val r = sizePx * 0.37f
    drawAnimatedCodexRiftPreview(c, riftId, cx, cy, r, pulsePhase = 0.42f, spinAngle = 0.55f)
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
    hullTrimStyle: Int = activeHullTrimStyle,
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
      when (hullTrimStyle) {
        1 ->
          intArrayOf(
            Color.parseColor("#A5F3FC"),
            Color.parseColor("#00F5D4"),
            Color.parseColor("#0E7490"),
            Color.parseColor("#38BDF8"),
          )
        2 ->
          intArrayOf(
            Color.parseColor("#FEF08A"),
            Color.parseColor("#FACC15"),
            Color.parseColor("#92400E"),
            Color.parseColor("#F59E0B"),
          )
        3 ->
          intArrayOf(
            Color.parseColor("#F5D0FE"),
            Color.parseColor("#D946EF"),
            Color.parseColor("#581C87"),
            Color.parseColor("#A855F7"),
          )
        4 ->
          intArrayOf(
            Color.parseColor("#FECACA"),
            Color.parseColor("#EF4444"),
            Color.parseColor("#7F1D1D"),
            Color.parseColor("#F97316"),
          )
        5 ->
          intArrayOf(
            Color.parseColor("#E0F2FE"),
            Color.parseColor("#94A3B8"),
            Color.parseColor("#334155"),
            Color.parseColor("#38BDF8"),
          )
        else ->
          intArrayOf(
            scaleColor(trimInt, 1.30f),
            trimInt,
            scaleColor(trimInt, 0.62f),
            scaleColor(trimInt, 1.15f),
          )
      }

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
   * Bakes the 5x Ship-Size (`180px` world radius / `360px` diameter) `Rift Station`
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

    // 4. 6 Swept Rift Station Curved Docking Pylons (3 Upper-Swept + 3 Lower-Swept Symmetric Pylons)
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
   * `stationRadius * 0.34f` around the central Ops spire of the `5x` `Rift Station`
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
   * Draws the 1:1 `STAR FORGE` Central Multicolored Shifting **Star Forge Core** (`stationRadius` = `180px`
   * matching the `Rift Station` footprint), encircled by rotating 5-stop metallic harvester
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

    // 2. Convecting Multicolored Star Forge Sphere
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

    // 3. Inner Tilted 5-Stop Metallic Gimbaled Harvester Ring
    val innerForgeRingR = stationRadius * 0.78f
    c.save()
    c.translate(cx, cy)
    c.rotate((timeSec * 22f) % 360f)
    c.scale(1.0f, 0.64f)
    stroke.strokeWidth = (stationRadius * 0.068f).coerceAtLeast(3.2f)
    stroke.shader =
      LinearGradient(
        -innerForgeRingR,
        -innerForgeRingR,
        innerForgeRingR,
        innerForgeRingR,
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
    c.drawCircle(0f, 0f, innerForgeRingR, stroke)
    stroke.shader = null
    stroke.strokeWidth = (stationRadius * 0.016f).coerceAtLeast(1.0f)
    stroke.color = Color.argb(230, 2, 6, 23)
    c.drawCircle(0f, 0f, innerForgeRingR + stationRadius * 0.034f, stroke)
    c.drawCircle(0f, 0f, innerForgeRingR - stationRadius * 0.034f, stroke)
    stroke.color = sunPrimary
    for (v in 0 until 8) {
      val va = v * (twoPi / 8f)
      c.drawLine(
        cos(va) * (innerForgeRingR - stationRadius * 0.03f),
        sin(va) * (innerForgeRingR - stationRadius * 0.03f),
        cos(va) * (innerForgeRingR + stationRadius * 0.03f),
        sin(va) * (innerForgeRingR + stationRadius * 0.03f),
        stroke,
      )
    }
    c.restore()

    // 4. Outer Primary 5-Stop Metallic Containment Ring (`0.94f * stationRadius`)
    val outerForgeRingR = stationRadius * 0.94f
    val bandW = (stationRadius * 0.088f).coerceAtLeast(4.0f)
    stroke.strokeWidth = bandW
    stroke.shader =
      LinearGradient(
        cx - outerForgeRingR,
        cy - outerForgeRingR,
        cx + outerForgeRingR,
        cy + outerForgeRingR,
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
    c.drawCircle(cx, cy, outerForgeRingR, stroke)
    stroke.shader = null

    stroke.strokeWidth = (stationRadius * 0.018f).coerceAtLeast(1.1f)
    stroke.color = Color.argb(235, 2, 6, 23)
    c.drawCircle(cx, cy, outerForgeRingR + bandW * 0.5f, stroke)
    c.drawCircle(cx, cy, outerForgeRingR - bandW * 0.5f, stroke)

    // 12 Collector Pylons & Glowing Energy Conduits around the Containment Ring
    for (node in 0 until 12) {
      val na = -timeSec * 0.28f + node * (twoPi / 12f)
      val ca = cos(na)
      val sa = sin(na)
      val rIn = outerForgeRingR - bandW * 0.62f
      val rOut = outerForgeRingR + bandW * 0.46f
      stroke.strokeWidth = (stationRadius * 0.024f).coerceAtLeast(1.3f)
      stroke.color = Color.rgb(30, 41, 59)
      c.drawLine(cx + ca * rIn, cy + sa * rIn, cx + ca * rOut, cy + sa * rOut, stroke)
      fill.color = if (node % 2 == 0) sunPrimary else sunSecondary
      c.drawCircle(cx + ca * outerForgeRingR, cy + sa * outerForgeRingR, (bandW * 0.22f).coerceAtLeast(1.5f), fill)
    }

    // 4b. Inward-Blooming Stellar Ingress Plumes & Refractive Meniscus Lens Highlights on the Star Forge Core
    drawLocalizedIngressPlumes(
      canvas = c,
      cx = cx,
      cy = cy,
      outerRimR = outerForgeRingR - bandW * 0.52f,
      innerReachR = coreR * 0.92f,
      plumeCount = 6,
      phaseRad = -timeSec * 0.56f,
      outerColor = sunSecondary,
      midColor = sunPrimary,
      coreColor = sunCore,
      fadeAlpha = 0.82f,
      swirlBias = 0.24f,
    )
    drawRefractiveMeniscusRim(
      canvas = c,
      cx = cx,
      cy = cy,
      rimRadius = outerForgeRingR - bandW * 0.48f,
      throatRadius = coreR * 0.82f,
      lightAngleRad = -2.28f + 0.08f * sin(timeSec * 0.7f),
      rimTintColor = sunPrimary,
      secondaryTintColor = sunSecondary,
      fadeAlpha = 0.90f,
      strokeScale = 0.95f,
    )

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
   * - `"FORGE"` (`SKINS & AUDIO`): Gunmetal & violet Orbital Foundry Crucible + 4 curved Rift Station manipulator claws + 3D blueprint ring + equalizer light bars.
   * - `"COMMS"` (`SUBSPACE COMMS`): Armored Subspace Transponder Dish & Comms Relay Core + twin sensor spires + 3D holographic comm-wave arcs.
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

        // 4 Curved Rift Station Manipulator Claws Arcing Inward (0°, 90°, 180°, 270°)
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

        // Twin Vertical Armored Sensor Spires Flanking Comms Relay Core
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

        // Central Subspace Comms Citadel Core Sphere
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
