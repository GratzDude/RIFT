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

object RiftSpriteBaker {
  val alliedShipBitmaps: Map<Int, Bitmap>
  val hostileShipBitmaps: Map<Int, Bitmap>
  val flashShipBitmaps: Map<Int, Bitmap>
  val buffIconBitmaps: Map<Int, Bitmap>
  val riftAnomalyIconBitmaps: Map<String, Bitmap>

  val wallTileBitmap: Bitmap
  val hazardBorderBitmap: Bitmap
  val wallShader: BitmapShader
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
    for (buffType in 0..10) {
      buffs[buffType] = bakeBuffIconBitmap(buffType, 68)
    }
    buffs[-10] = bakeBuffIconBitmap(-10, 68) // Speed Overcharge
    buffs[-11] = bakeBuffIconBitmap(-11, 68) // Thrust Overcharge
    buffs[-12] = bakeBuffIconBitmap(-12, 68) // Reactor Overcharge
    buffIconBitmaps = buffs

    riftAnomalyIconBitmaps =
      mapOf(
        "plasma_rift" to bakeRiftAnomalyIconBitmap("plasma_rift", 68),
        "repulsor_rift" to bakeRiftAnomalyIconBitmap("repulsor_rift", 68),
        "gravity_rift" to bakeRiftAnomalyIconBitmap("gravity_rift", 68),
        "phase_rift_pair" to bakeRiftAnomalyIconBitmap("phase_rift_pair", 68),
      )
  }

  fun getRiftAnomalyIconBitmap(riftId: String): Bitmap {
    return riftAnomalyIconBitmaps[riftId] ?: riftAnomalyIconBitmaps["plasma_rift"]!!
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

  fun getShipSprite(shipId: Int, isAllied: Boolean = true, isHitFlashing: Boolean = false): Bitmap {
    if (shipId == 9) {
      if (isHitFlashing) return customShip9FlashBitmap
      return if (isAllied) customShip9AlliedBitmap else customShip9HostileBitmap
    }
    if (isHitFlashing) {
      return flashShipBitmaps[shipId] ?: flashShipBitmaps[1]!!
    }
    val map = if (isAllied) alliedShipBitmaps else hostileShipBitmaps
    return map[shipId] ?: map[1] ?: bakeShipBitmap(RiftCodex.getShipById(shipId), 128, isAllied)
  }

  private val customBuffIconCache = mutableMapOf<Int, Bitmap>()

  fun invalidateCustomBuffIcon(buffType: Int) {
    customBuffIconCache.remove(buffType)
  }

  fun getBuffIconBitmap(buffType: Int): Bitmap {
    if (buffType in 0..10 || buffType in -12..-10) {
      return buffIconBitmaps[buffType] ?: buffIconBitmaps[10]!!
    }
    customBuffIconCache[buffType]?.let { return it }
    val entry = RiftCodex.getBuffRegistryByIndex(buffType)
    if (entry != null) {
      val baked = bakeCustomBuffIconBitmap(entry, 68)
      customBuffIconCache[buffType] = baked
      return baked
    }
    return buffIconBitmaps[buffType.mod(11)] ?: buffIconBitmaps[0]!!
  }

  private fun bakeCustomBuffIconBitmap(entry: SpecialBuffRegistryEntry, sizePx: Int = 68): Bitmap {
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
   * 6 = Full Flux Charge (Emerald Lightning Bolt)
   * 7 = Repel Charge (Electric Cyan Concentric Shockwave Rings)
   * 8 = Shot-Limited Multi-Shot +25 Shots (Gold Triple-Spread Trident Beams)
   * 9 = Bouncing Bullets +30 Shots (Electric Violet Ricochet Bank-Shot)
   * 10 = Overcharge Surge +8% x3 (Solar Amber Triple Upward Overdrive Chevrons)
   */
  private fun bakeBuffIconBitmap(buffType: Int, sizePx: Int = 68): Bitmap {
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
        -10 -> Color.parseColor("#38BDF8") // Speed Overcharge (+8% x3)
        -11 -> Color.parseColor("#F59E0B") // Thrust Overcharge (+8% x3)
        -12 -> Color.parseColor("#10B981") // Reactor Overcharge (+8% x3)
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
        // FULL FLUX CHARGE: High-voltage lightning bolt
        val bolt =
          Path().apply {
            moveTo(cx + r * 0.12f, cy - r * 0.58f)
            lineTo(cx - r * 0.36f, cy + r * 0.05f)
            lineTo(cx - r * 0.02f, cy + r * 0.05f)
            lineTo(cx - r * 0.16f, cy + r * 0.58f)
            lineTo(cx + r * 0.38f, cy - r * 0.06f)
            lineTo(cx + r * 0.04f, cy - r * 0.06f)
            close()
          }
        c.drawPath(bolt, glyphStrokePaint)
        c.drawPath(bolt, glyphFillPaint)
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
        // REACTOR OVERCHARGE (+8% x3): Capacitor diamond ring + central lightning spark
        val diamond =
          Path().apply {
            moveTo(cx, cy - r * 0.50f)
            lineTo(cx + r * 0.46f, cy)
            lineTo(cx, cy + r * 0.50f)
            lineTo(cx - r * 0.46f, cy)
            close()
          }
        c.drawPath(diamond, glyphStrokePaint)
        c.drawCircle(cx, cy, r * 0.18f, glyphFillPaint)
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
   * Bakes a seamless 64x64 Dark Obsidian Bulkhead Tile with staggered armored hull plates,
   * mitered 45° corner chamfers, recessed dark titanium bolts, and subtle V-cyan conduit seams.
   */
  private fun bakeWallTile(): Bitmap {
    val size = 64
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val c = Canvas(bmp)

    // 1. Deep void-obsidian recessed channel base
    val abyssPaint =
      Paint().apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#04070D")
      }
    c.drawRect(0f, 0f, size.toFloat(), size.toFloat(), abyssPaint)

    // 2. Subtle glowing cyan energy conduit seams running along recessed channels
    val seamGlowPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3.2f
        color = Color.argb(46, 0, 240, 255)
      }
    val seamCorePaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.0f
        color = Color.argb(115, 56, 189, 248)
      }
    // Horizontal & staggered vertical conduit seams in the recessed grooves (seamless across 64x64 edges)
    c.drawLine(0f, 32f, 64f, 32f, seamGlowPaint)
    c.drawLine(0f, 0f, 64f, 0f, seamGlowPaint)
    c.drawLine(0f, 64f, 64f, 64f, seamGlowPaint)
    c.drawLine(32f, 0f, 32f, 32f, seamGlowPaint)
    c.drawLine(16f, 32f, 16f, 64f, seamGlowPaint)
    c.drawLine(48f, 32f, 48f, 64f, seamGlowPaint)

    c.drawLine(0f, 32f, 64f, 32f, seamCorePaint)
    c.drawLine(0f, 0.5f, 64f, 0.5f, seamCorePaint)
    c.drawLine(32f, 0f, 32f, 32f, seamCorePaint)
    c.drawLine(16f, 32f, 16f, 64f, seamCorePaint)
    c.drawLine(48f, 32f, 48f, 64f, seamCorePaint)

    val plateFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    val topBevelPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.15f
        color = Color.parseColor("#223452")
      }
    val bottomShadePaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.2f
        color = Color.parseColor("#020409")
      }
    val ribGroovePaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 0.9f
        color = Color.parseColor("#060B14")
      }
    val ribHighlightPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 0.8f
        color = Color.argb(95, 56, 189, 248)
      }
    val boltSocketPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#050912")
      }
    val boltPinPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#64748B")
      }
    val nodeSparkPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.argb(145, 0, 240, 255)
      }

    fun drawObsidianPlate(left: Float, top: Float, right: Float, bottom: Float, hasCenterNode: Boolean) {
      val insetL = left + 1.4f
      val insetT = top + 1.4f
      val insetR = right - 1.4f
      val insetB = bottom - 1.4f
      val ch = 3.0f

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
            Color.parseColor("#162338"),
            Color.parseColor("#0C1322"),
            Color.parseColor("#080D17"),
            Color.parseColor("#101A2C"),
          ),
          floatArrayOf(0f, 0.45f, 0.82f, 1f),
          Shader.TileMode.CLAMP,
        )
      c.drawPath(platePath, plateFillPaint)

      // Top/left specular armor bevel
      c.drawLine(insetL + ch, insetT, insetR - ch, insetT, topBevelPaint)
      c.drawLine(insetL, insetT + ch, insetL, insetB - ch, topBevelPaint)
      // Bottom/right recessed shadow bevel
      c.drawLine(insetL + ch, insetB, insetR - ch, insetB, bottomShadePaint)
      c.drawLine(insetR, insetT + ch, insetR, insetB - ch, bottomShadePaint)

      // Recessed internal structural rib + subtle cyan micro-trace
      val midY = (insetT + insetB) * 0.5f
      c.drawLine(insetL + 5f, midY, insetR - 5f, midY, ribGroovePaint)
      if (hasCenterNode) {
        c.drawLine(insetL + 7f, midY - 0.8f, insetR - 7f, midY - 0.8f, ribHighlightPaint)
        val midX = (insetL + insetR) * 0.5f
        c.drawCircle(midX, midY, 1.3f, nodeSparkPaint)
      }

      // Corner titanium micro-bolts
      val bOff = 4.2f
      val corners =
        arrayOf(
          insetL + bOff to insetT + bOff,
          insetR - bOff to insetT + bOff,
          insetL + bOff to insetB - bOff,
          insetR - bOff to insetB - bOff,
        )
      for ((bx, by) in corners) {
        c.drawCircle(bx, by, 1.5f, boltSocketPaint)
        c.drawCircle(bx - 0.25f, by - 0.25f, 0.9f, boltPinPaint)
      }
    }

    // Top row: two 32x32 obsidian bulkhead plates
    drawObsidianPlate(0f, 0f, 32f, 32f, hasCenterNode = true)
    drawObsidianPlate(32f, 0f, 64f, 32f, hasCenterNode = false)
    // Bottom row: staggered brick-bond obsidian bulkhead plates (wraps seamlessly at x=0/64)
    drawObsidianPlate(16f, 32f, 48f, 64f, hasCenterNode = true)
    drawObsidianPlate(-16f, 32f, 16f, 64f, hasCenterNode = false)
    drawObsidianPlate(48f, 32f, 80f, 64f, hasCenterNode = false)

    return bmp
  }

  private fun bakeHazardBorderTile(): Bitmap {
    val bmp = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888)
    val c = Canvas(bmp)
    val bgPaint =
      Paint().apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#070C16")
      }
    c.drawRect(0f, 0f, 32f, 32f, bgPaint)

    val stripePaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.argb(115, 14, 165, 233) // Subtle dark cyan-slate bulkhead chevron trim
      }

    val p1 =
      Path().apply {
        moveTo(0f, 0f)
        lineTo(10f, 0f)
        lineTo(0f, 10f)
        close()
      }
    val p2 =
      Path().apply {
        moveTo(16f, 0f)
        lineTo(26f, 0f)
        lineTo(0f, 26f)
        lineTo(0f, 16f)
        close()
      }
    val p3 =
      Path().apply {
        moveTo(32f, 16f)
        lineTo(32f, 26f)
        lineTo(16f, 32f)
        lineTo(6f, 32f)
        close()
      }
    c.drawPath(p1, stripePaint)
    c.drawPath(p2, stripePaint)
    c.drawPath(p3, stripePaint)
    return bmp
  }

  private fun bakeRiftAnomalyIconBitmap(riftId: String, sizePx: Int = 68): Bitmap {
    val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val c = Canvas(bmp)
    val cx = sizePx * 0.5f
    val cy = sizePx * 0.5f
    val r = sizePx * 0.36f

    when (riftId) {
      "plasma_rift" -> {
        val halo =
          Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader =
              RadialGradient(
                cx,
                cy,
                sizePx * 0.48f,
                intArrayOf(
                  Color.argb(235, 255, 240, 245),
                  Color.argb(210, 239, 68, 68),
                  Color.argb(135, 217, 70, 239),
                  Color.argb(0, 239, 68, 68),
                ),
                floatArrayOf(0f, 0.32f, 0.68f, 1f),
                Shader.TileMode.CLAMP,
              )
          }
        c.drawCircle(cx, cy, sizePx * 0.48f, halo)
        val arcPaint =
          Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2.6f
            strokeCap = Paint.Cap.ROUND
            color = Color.parseColor("#FCA5A5")
          }
        for (i in 0 until 6) {
          val a = (i * Math.PI / 3.0).toFloat()
          val p =
            Path().apply {
              moveTo(cx + kotlin.math.cos(a) * (r * 0.25f), cy + kotlin.math.sin(a) * (r * 0.25f))
              lineTo(cx + kotlin.math.cos(a + 0.28f) * (r * 0.68f), cy + kotlin.math.sin(a + 0.28f) * (r * 0.68f))
              lineTo(cx + kotlin.math.cos(a - 0.14f) * (r * 1.05f), cy + kotlin.math.sin(a - 0.14f) * (r * 1.05f))
            }
          c.drawPath(p, arcPaint)
        }
      }
      "repulsor_rift" -> {
        val halo =
          Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader =
              RadialGradient(
                cx,
                cy,
                sizePx * 0.48f,
                intArrayOf(
                  Color.WHITE,
                  Color.argb(190, 56, 189, 248),
                  Color.argb(0, 56, 189, 248),
                ),
                floatArrayOf(0f, 0.45f, 1f),
                Shader.TileMode.CLAMP,
              )
          }
        c.drawCircle(cx, cy, sizePx * 0.48f, halo)
        val ringPaint =
          Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2.2f
            color = Color.WHITE
          }
        c.drawCircle(cx, cy, r * 0.55f, ringPaint)
        ringPaint.color = Color.argb(180, 125, 211, 252)
        c.drawCircle(cx, cy, r * 0.92f, ringPaint)
        for (i in 0 until 4) {
          val a = (i * Math.PI / 2.0 + Math.PI / 4.0).toFloat()
          c.drawLine(
            cx + kotlin.math.cos(a) * (r * 0.58f),
            cy + kotlin.math.sin(a) * (r * 0.58f),
            cx + kotlin.math.cos(a) * (r * 1.08f),
            cy + kotlin.math.sin(a) * (r * 1.08f),
            ringPaint,
          )
        }
      }
      "gravity_rift" -> {
        val halo =
          Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader =
              RadialGradient(
                cx,
                cy,
                sizePx * 0.48f,
                intArrayOf(
                  Color.BLACK,
                  Color.argb(235, 168, 85, 247),
                  Color.argb(120, 99, 102, 241),
                  Color.argb(0, 99, 102, 241),
                ),
                floatArrayOf(0f, 0.34f, 0.72f, 1f),
                Shader.TileMode.CLAMP,
              )
          }
        c.drawCircle(cx, cy, sizePx * 0.48f, halo)
        val corePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#030008") }
        c.drawCircle(cx, cy, r * 0.42f, corePaint)
        val swirlPaint =
          Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2.2f
            strokeCap = Paint.Cap.ROUND
            color = Color.parseColor("#D8B4FE")
          }
        val rect = RectF(cx - r * 0.85f, cy - r * 0.85f, cx + r * 0.85f, cy + r * 0.85f)
        c.drawArc(rect, 15f, 120f, false, swirlPaint)
        c.drawArc(rect, 195f, 120f, false, swirlPaint)
      }
      else -> {
        // Paired Phase Rifts (Node A ⇄ Node B)
        val c1x = sizePx * 0.34f
        val c1y = sizePx * 0.36f
        val c2x = sizePx * 0.66f
        val c2y = sizePx * 0.64f
        val linkPaint =
          Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2.2f
            color = Color.argb(195, 45, 212, 191)
          }
        c.drawLine(c1x, c1y, c2x, c2y, linkPaint)
        for ((px, py, hex) in listOf(Triple(c1x, c1y, "#2DD4BF"), Triple(c2x, c2y, "#38BDF8"))) {
          val col = Color.parseColor(hex)
          val g =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
              shader =
                RadialGradient(
                  px,
                  py,
                  sizePx * 0.28f,
                  intArrayOf(Color.WHITE, col, Color.argb(0, Color.red(col), Color.green(col), Color.blue(col))),
                  floatArrayOf(0f, 0.52f, 1f),
                  Shader.TileMode.CLAMP,
                )
            }
          c.drawCircle(px, py, sizePx * 0.28f, g)
          val ring =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
              style = Paint.Style.STROKE
              strokeWidth = 1.8f
              color = Color.WHITE
            }
          c.drawCircle(px, py, sizePx * 0.14f, ring)
        }
      }
    }
    return bmp
  }

  fun bakeShipBitmap(spec: ShipSpec, sizePx: Int = 128, isAllied: Boolean = true): Bitmap {
    val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val c = Canvas(bmp)
    drawMetallicShipOnCanvas(c, sizePx / 2f, sizePx / 2f, sizePx * 0.39f, spec, isAllied)
    return bmp
  }

  /**
   * High-Definition Multi-Layered 3D Metallic Ship Renderer:
   * - Zero thruster flame plumes (strictly metallic exhaust housings)
   * - Hull-class specific 5-stop metallic armor shading + gold/bronze trim bevels
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
  ) {
    c.save()
    c.translate(cx, cy)

    // Hull-specific 5-stop 3D metallic gradient across [-r, r]
    val hullColors =
      when (spec.id) {
        3, 4 ->
          intArrayOf(
            Color.parseColor("#0D0B26"),
            Color.parseColor("#1E1B4B"),
            Color.parseColor("#6366F1"),
            Color.parseColor("#1E1B4B"),
            Color.parseColor("#0D0B26"),
          )
        5, 6 ->
          intArrayOf(
            Color.parseColor("#041924"),
            Color.parseColor("#0E3A4D"),
            Color.parseColor("#38BDF8"),
            Color.parseColor("#0C3142"),
            Color.parseColor("#041924"),
          )
        7, 8 ->
          intArrayOf(
            Color.parseColor("#14110F"),
            Color.parseColor("#292524"),
            Color.parseColor("#78716C"),
            Color.parseColor("#292524"),
            Color.parseColor("#14110F"),
          )
        else -> // Ships 1, 2 Obsidian Rail
          intArrayOf(
            Color.parseColor("#090D16"),
            Color.parseColor("#1E293B"),
            Color.parseColor("#64748B"),
            Color.parseColor("#1E293B"),
            Color.parseColor("#090D16"),
          )
      }

    val trimColors =
      when (spec.id) {
        3, 4 ->
          intArrayOf(
            Color.parseColor("#FEF08A"),
            Color.parseColor("#F59E0B"),
            Color.parseColor("#92400E"),
            Color.parseColor("#FACC15"),
          )
        7, 8 ->
          intArrayOf(
            Color.parseColor("#FDBA74"),
            Color.parseColor("#EA580C"),
            Color.parseColor("#7C2D12"),
            Color.parseColor("#FB923C"),
          )
        else ->
          intArrayOf(
            Color.parseColor("#FDE68A"),
            Color.parseColor("#D97706"),
            Color.parseColor("#78350F"),
            Color.parseColor("#FBBF24"),
          )
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
            floatArrayOf(0.0f, 0.30f, 0.50f, 0.70f, 1.0f),
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
            intArrayOf(
              hullColors[2],
              hullColors[1],
              hullColors[0],
            ),
            floatArrayOf(0f, 0.55f, 1f),
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
        strokeWidth = 1.6f
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
        strokeWidth = 2.6f
        strokeJoin = Paint.Join.ROUND
        color = if (isAllied) Color.parseColor("#38BDF8") else Color.parseColor("#EF4444")
      }

    val specularEdgePaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.5f
        strokeJoin = Paint.Join.ROUND
        color = Color.argb(200, 255, 255, 255)
      }

    val seamPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.5f
        color = Color.parseColor("#020617")
      }

    val canopyPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        shader =
          LinearGradient(
            -r * 0.2f,
            -r * 0.2f,
            r * 0.5f,
            r * 0.2f,
            intArrayOf(
              Color.parseColor("#E0F2FE"),
              Color.parseColor("#0284C7"),
              Color.parseColor("#082F49"),
            ),
            floatArrayOf(0f, 0.45f, 1f),
            Shader.TileMode.CLAMP,
          )
      }

    val goldWingPath = Path()
    val armorHullPath = Path()
    val upperDeckPath = Path()

    when (spec.id) {
      1 -> {
        // 1-Viper: "Forward-swept obsidian rail-prongs with bronze wing-tips and a narrow V-cyan reactor spine."
        // Protruding twin hyper-velocity railgun barrels inside forward prongs
        c.drawRect(r * 0.30f, -r * 0.22f, r * 1.18f, -r * 0.12f, gunBarrelPaint)
        c.drawRect(r * 0.30f, -r * 0.22f, r * 1.18f, -r * 0.12f, gunBarrelStroke)
        c.drawRect(r * 0.30f, r * 0.12f, r * 1.18f, r * 0.22f, gunBarrelPaint)
        c.drawRect(r * 0.30f, r * 0.12f, r * 1.18f, r * 0.22f, gunBarrelStroke)
        c.drawCircle(r * 1.16f, -r * 0.17f, r * 0.05f, muzzleGlowPaint)
        c.drawCircle(r * 1.16f, r * 0.17f, r * 0.05f, muzzleGlowPaint)

        // Outer bronze wing-tips & forward-swept rail prongs
        goldWingPath.apply {
          moveTo(r * 1.12f, -r * 0.26f)
          lineTo(r * 0.40f, -r * 0.32f)
          lineTo(r * 0.22f, -r * 0.98f) // Forward-swept wingtip
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
        // Obsidian main fuselage with forward prongs
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
        // Narrow elevated sniper spine
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
        // 2-Ricochet: "Twin-forked ballistic hull with gold-trimmed deflection vanes and a deep cyan V-core."
        // Central recessed bounce-torpedo launch tube + twin fork pulse emitters
        c.drawRect(r * 0.10f, -r * 0.11f, r * 0.78f, r * 0.11f, gunBarrelPaint)
        c.drawRect(r * 0.10f, -r * 0.11f, r * 0.78f, r * 0.11f, gunBarrelStroke)
        c.drawCircle(r * 0.74f, 0f, r * 0.08f, muzzleGlowPaint)

        goldWingPath.apply {
          moveTo(r * 1.10f, -r * 0.36f)
          lineTo(r * 0.32f, -r * 0.16f)
          lineTo(r * 0.32f, r * 0.16f)
          lineTo(r * 1.10f, r * 0.36f)
          lineTo(r * 0.56f, r * 0.68f)
          lineTo(-r * 0.18f, r * 1.02f) // Angled deflection vane
          lineTo(-r * 0.92f, r * 0.68f)
          lineTo(-r * 0.72f, 0f)
          lineTo(-r * 0.92f, -r * 0.68f)
          lineTo(-r * 0.18f, -r * 1.02f) // Angled deflection vane
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
        // 3-Suppressor: "Swept imperial crescent wings in dark indigo & gold with an overcharged V-cyan heart."
        // Quad-Stream Rapid Plasma Array barrels along crescent leading edge
        for (offsetY in floatArrayOf(-0.62f, -0.32f, 0.32f, 0.62f)) {
          val bx = if (kotlin.math.abs(offsetY) > 0.45f) r * 0.56f else r * 0.78f
          c.drawRect(bx - r * 0.35f, offsetY * r - 2.5f, bx + r * 0.22f, offsetY * r + 2.5f, gunBarrelPaint)
          c.drawRect(bx - r * 0.35f, offsetY * r - 2.5f, bx + r * 0.22f, offsetY * r + 2.5f, gunBarrelStroke)
          c.drawCircle(bx + r * 0.22f, offsetY * r, 3.2f, muzzleGlowPaint)
        }

        // Curved Imperial Crescent Gold Trim
        goldWingPath.apply {
          moveTo(r * 1.10f, 0f)
          cubicTo(r * 0.85f, -r * 0.55f, r * 0.30f, -r * 1.08f, -r * 0.72f, -r * 0.96f)
          cubicTo(-r * 0.32f, -r * 0.58f, -r * 0.30f, -r * 0.25f, -r * 0.82f, 0f)
          cubicTo(-r * 0.30f, r * 0.25f, -r * 0.32f, r * 0.58f, -r * 0.72f, r * 0.96f)
          cubicTo(r * 0.30f, r * 1.08f, r * 0.85f, r * 0.55f, r * 1.10f, 0f)
          close()
        }
        // Indigo Crescent Armor Hull
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
        // 4-Goliath: "Heavy armored dreadnought chassis with bronze bastion shoulders and a massive V-reactor."
        // Heavy Twin Autocannon + Central Siege Nova Mortar
        c.drawRect(r * 0.40f, -r * 0.46f, r * 1.14f, -r * 0.32f, gunBarrelPaint)
        c.drawRect(r * 0.40f, -r * 0.46f, r * 1.14f, -r * 0.32f, gunBarrelStroke)
        c.drawRect(r * 0.40f, r * 0.32f, r * 1.14f, r * 0.46f, gunBarrelPaint)
        c.drawRect(r * 0.40f, r * 0.32f, r * 1.14f, r * 0.46f, gunBarrelStroke)
        // Central Siege Mortar muzzle
        c.drawRect(r * 0.50f, -r * 0.16f, r * 1.08f, r * 0.16f, gunBarrelPaint)
        c.drawRect(r * 0.50f, -r * 0.16f, r * 1.08f, r * 0.16f, gunBarrelStroke)
        c.drawCircle(r * 1.04f, 0f, r * 0.10f, muzzleGlowPaint)

        // Cathedral Bronze Bastion Shoulders
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
        // Dreadnought Main Armor Block
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
        // Stepped Cathedral Citadel Superstructure
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
        // 5-Twinfang: "Predatory twin-boom manta frame with bronze fangs and a split electric-cyan core."
        // Tri-Spread Bio-Plasma Volley emitters in jaw
        c.drawCircle(r * 0.74f, 0f, r * 0.08f, muzzleGlowPaint)
        c.drawCircle(r * 1.06f, -r * 0.42f, r * 0.06f, muzzleGlowPaint)
        c.drawCircle(r * 1.06f, r * 0.42f, r * 0.06f, muzzleGlowPaint)

        // Predatory Twin-Boom Manta Frame & Bronze Fangs
        goldWingPath.apply {
          moveTo(r * 1.16f, -r * 0.42f) // Left bronze fang tip
          lineTo(r * 0.28f, -r * 0.18f)
          lineTo(r * 0.68f, 0f) // Center plasma needle
          lineTo(r * 0.28f, r * 0.18f)
          lineTo(r * 1.16f, r * 0.42f) // Right bronze fang tip
          lineTo(r * 0.45f, r * 0.74f)
          lineTo(-r * 0.28f, r * 1.06f) // Swept manta wingtip
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
        // 6-Disruptor: "Wide stingray hydrofoil with gold-plated arc emitters and a pulsing V-cyan capacitor."
        // Forward Gold-Plated Arc Emitter Prongs
        c.drawRect(r * 0.55f, -r * 0.28f, r * 1.16f, -r * 0.18f, goldPaint)
        c.drawRect(r * 0.55f, r * 0.18f, r * 1.16f, r * 0.28f, goldPaint)
        c.drawCircle(r * 1.14f, -r * 0.23f, r * 0.06f, muzzleGlowPaint)
        c.drawCircle(r * 1.14f, r * 0.23f, r * 0.06f, muzzleGlowPaint)

        // Wide Curved Stingray Hydrofoil
        goldWingPath.apply {
          moveTo(r * 1.06f, 0f)
          cubicTo(r * 0.72f, -r * 0.36f, r * 0.35f, -r * 1.12f, -r * 0.26f, -r * 1.06f)
          cubicTo(-r * 0.68f, -r * 0.92f, -r * 0.60f, -r * 0.36f, -r * 0.42f, -r * 0.18f)
          lineTo(-r * 1.12f, 0f) // Stingray tail spine
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
        // 7-Valkyrie: "Aggressive X-wing skeletal interceptor with bronze thruster pods and V-cyan reactor."
        // Twin Heavy Rotary Cannon Barrels
        c.drawRect(r * 0.45f, -r * 0.24f, r * 1.16f, -r * 0.12f, gunBarrelPaint)
        c.drawRect(r * 0.45f, -r * 0.24f, r * 1.16f, -r * 0.12f, gunBarrelStroke)
        c.drawRect(r * 0.45f, r * 0.12f, r * 1.16f, r * 0.24f, gunBarrelPaint)
        c.drawRect(r * 0.45f, r * 0.12f, r * 1.16f, r * 0.24f, gunBarrelStroke)

        // Outboard Bronze Thruster Pods on the rear X-frame
        c.drawRect(-r * 0.98f, -r * 0.64f, -r * 0.38f, -r * 0.44f, goldPaint)
        c.drawRect(-r * 0.98f, -r * 0.64f, -r * 0.38f, -r * 0.44f, gunBarrelStroke)
        c.drawRect(-r * 0.98f, r * 0.44f, -r * 0.38f, r * 0.64f, goldPaint)
        c.drawRect(-r * 0.98f, r * 0.44f, -r * 0.38f, r * 0.64f, gunBarrelStroke)

        // Skeletal X-Frame Wings
        goldWingPath.apply {
          moveTo(r * 1.06f, 0f)
          lineTo(r * 0.42f, -r * 0.22f)
          lineTo(r * 0.72f, -r * 1.02f) // Forward upper X-foil
          lineTo(r * 0.32f, -r * 1.04f)
          lineTo(-r * 0.08f, -r * 0.40f)
          lineTo(-r * 0.68f, -r * 1.02f) // Rear upper X-foil
          lineTo(-r * 0.98f, -r * 0.86f)
          lineTo(-r * 0.56f, -r * 0.22f)
          lineTo(-r * 0.90f, 0f)
          lineTo(-r * 0.56f, r * 0.22f)
          lineTo(-r * 0.98f, r * 0.86f)
          lineTo(-r * 0.68f, r * 1.02f) // Rear lower X-foil
          lineTo(-r * 0.08f, r * 0.40f)
          lineTo(r * 0.32f, r * 1.04f)
          lineTo(r * 0.72f, r * 1.02f) // Forward lower X-foil
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

      else -> {
        // 8-Manta: "Armored pincer hull with gold-beveled mandible plates and a twin-cell V-cyan core."
        // Dual Gauss Barrels inside pincer jaws + Rear Mine-Layer Bay
        c.drawRect(r * 0.32f, -r * 0.26f, r * 0.98f, -r * 0.14f, gunBarrelPaint)
        c.drawRect(r * 0.32f, -r * 0.26f, r * 0.98f, -r * 0.14f, gunBarrelStroke)
        c.drawRect(r * 0.32f, r * 0.14f, r * 0.98f, r * 0.26f, gunBarrelPaint)
        c.drawRect(r * 0.32f, r * 0.14f, r * 0.98f, r * 0.26f, gunBarrelStroke)
        // Rear magnetic mine chute
        c.drawCircle(-r * 0.72f, 0f, r * 0.18f, gunBarrelPaint)
        c.drawCircle(-r * 0.72f, 0f, r * 0.18f, gunBarrelStroke)

        // Gold-Beveled Mandible Pincer Plates
        goldWingPath.apply {
          moveTo(r * 1.12f, -r * 0.32f) // Inner pincer tip
          lineTo(r * 0.78f, -r * 0.68f) // Outer mandible bevel
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
    }

    // Layer 3: Outer Gold/Bronze Wing Trim + Brawl Stars Team Readability Outline
    c.drawPath(goldWingPath, goldPaint)
    c.drawPath(goldWingPath, readabilityOutlinePaint)

    // Layer 4: Primary 5-Stop 3D Metallic Armor Hull + Specular Leading Edge
    c.drawPath(armorHullPath, hullPaint)
    c.drawPath(armorHullPath, specularEdgePaint)

    // Layer 5: Elevated Superstructure Deck + Recessed Armor Panel Seams
    c.drawPath(upperDeckPath, upperPlatePaint)
    c.drawPath(upperDeckPath, seamPaint)
    c.drawLine(-r * 0.56f, -r * 0.38f, r * 0.38f, -r * 0.14f, seamPaint)
    c.drawLine(-r * 0.56f, r * 0.38f, r * 0.38f, r * 0.14f, seamPaint)

    // Ship-specific accent details (Disruptor EMP coils, Valkyrie X-ribs, Goliath bastion rivets)
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
    } else if (spec.id == 4) {
      // Goliath bastion shoulder armor bars
      c.drawLine(-r * 0.35f, -r * 0.72f, r * 0.25f, -r * 0.72f, specularEdgePaint)
      c.drawLine(-r * 0.35f, r * 0.72f, r * 0.25f, r * 0.72f, specularEdgePaint)
    }

    // Layer 6: Tinted 3D Cockpit Glass Canopy ahead of the reactor
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

    // Layer 7: Signature V-Shaped Electric-Cyan Reactor Core (with Twin-Cell variant for Twinfang & Manta)
    val cyanGlowPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#38BDF8")
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

    // Twin-cell aft reactor node for Twinfang (5), Goliath (4), and Manta (8)
    if (spec.id == 4 || spec.id == 5 || spec.id == 8) {
      c.drawCircle(-r * 0.42f, -r * 0.16f, r * 0.07f, cyanGlowPaint)
      c.drawCircle(-r * 0.42f, r * 0.16f, r * 0.07f, cyanGlowPaint)
      c.drawCircle(-r * 0.42f, -r * 0.16f, r * 0.035f, whiteCorePaint)
      c.drawCircle(-r * 0.42f, r * 0.16f, r * 0.035f, whiteCorePaint)
    }

    c.restore()
  }

  /**
   * Phase 4 AI Hull Forge Procedural 3D Metallic Renderer for the 9th Custom Modular Ship:
   * - Renders from the <2KB JSON CustomHullVisualRecipe
   * - Enforces minimum luminance contrast (L >= 0.32) against #000000 space
   * - Strictly fits within the 36x36px physical hitbox footprint with mandatory Allied/Hostile readability outline
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

    val hullColors =
      intArrayOf(
        scaleColor(baseHullInt, 0.45f),
        scaleColor(baseHullInt, 0.85f),
        scaleColor(baseHullInt, 1.45f),
        scaleColor(baseHullInt, 0.85f),
        scaleColor(baseHullInt, 0.45f),
      )

    val trimColors =
      intArrayOf(
        scaleColor(trimInt, 1.30f),
        trimInt,
        scaleColor(trimInt, 0.62f),
        scaleColor(trimInt, 1.15f),
      )

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
            floatArrayOf(0f, 0.30f, 0.50f, 0.70f, 1f),
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
            intArrayOf(hullColors[2], hullColors[1], hullColors[0]),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP,
          )
      }

    val readabilityOutlinePaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2.8f
        strokeJoin = Paint.Join.ROUND
        color = if (isAllied) Color.parseColor("#38BDF8") else Color.parseColor("#EF4444")
      }

    val specularEdgePaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.6f
        strokeJoin = Paint.Join.ROUND
        color = Color.argb(210, 255, 255, 255)
      }

    val seamPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.5f
        color = Color.parseColor("#020617")
      }

    val gunBarrelPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#1E293B")
      }
    val gunBarrelStroke =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.6f
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

    // Clamp wingSweep to ensure silhouette stays strictly inside the 36x36 hitbox footprint
    val sweep = recipe.wingSweep.coerceIn(0.68f, 1.08f)

    // 1. Engine Housing Pods based on recipe.engineStyle
    when (recipe.engineStyle) {
      "QUAD_NOVA" -> {
        for (ey in floatArrayOf(-0.62f, -0.34f, 0.34f, 0.62f)) {
          c.drawRect(-r * 0.96f, ey * r - r * 0.08f, -r * 0.45f, ey * r + r * 0.08f, trimPaint)
          c.drawRect(-r * 0.96f, ey * r - r * 0.08f, -r * 0.45f, ey * r + r * 0.08f, gunBarrelStroke)
        }
      }
      "HEAVY_THRUSTER" -> {
        c.drawRect(-r * 1.00f, -r * 0.56f, -r * 0.42f, -r * 0.24f, trimPaint)
        c.drawRect(-r * 1.00f, r * 0.24f, -r * 0.42f, r * 0.56f, trimPaint)
        c.drawRect(-r * 1.00f, -r * 0.56f, -r * 0.42f, -r * 0.24f, gunBarrelStroke)
        c.drawRect(-r * 1.00f, r * 0.24f, -r * 0.42f, r * 0.56f, gunBarrelStroke)
      }
      "BIO_ARC" -> {
        c.drawCircle(-r * 0.78f, -r * 0.36f, r * 0.14f, coreGlowPaint)
        c.drawCircle(-r * 0.78f, r * 0.36f, r * 0.14f, coreGlowPaint)
      }
      else -> {
        c.drawRect(-r * 0.94f, -r * 0.44f, -r * 0.46f, -r * 0.24f, gunBarrelPaint)
        c.drawRect(-r * 0.94f, r * 0.24f, -r * 0.46f, r * 0.44f, gunBarrelPaint)
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

    // Layer 1: Trim + Mandatory Team Readability Outline
    c.drawPath(outerTrimPath, trimPaint)
    c.drawPath(outerTrimPath, readabilityOutlinePaint)

    // Layer 2: Main 5-Stop Metallic Armor Hull + Specular Highlight
    c.drawPath(mainArmorPath, hullPaint)
    c.drawPath(mainArmorPath, specularEdgePaint)

    // Layer 3: Superstructure Deck & Chassis Plates (1..4)
    c.drawPath(upperDeckPath, upperPlatePaint)
    c.drawPath(upperDeckPath, seamPaint)
    for (pIdx in 1..recipe.chassisPlates.coerceIn(1, 4)) {
      val frac = pIdx * 0.15f
      c.drawLine(-r * 0.52f, -r * frac, r * 0.36f, -r * (frac * 0.5f), seamPaint)
      c.drawLine(-r * 0.52f, r * frac, r * 0.36f, r * (frac * 0.5f), seamPaint)
    }

    // Layer 4: glowing V-Core Reactor in custom coreGlowHex
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
}
