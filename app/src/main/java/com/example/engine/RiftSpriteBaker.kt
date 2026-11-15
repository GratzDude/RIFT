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
  val fluxIconBitmap: Bitmap = bake3dFluxReactorIcon(64)
  val riftShardIconBitmap: Bitmap = bake3dRiftShardIcon(64)
  var activeHullTrimStyle: Int = 0 // 0=Standard, 1=Obsidian Ice-Cyan, 2=Sovereign Gold, 3=Rift Violet
  private val trimmedShipSpriteCache = mutableMapOf<Int, Bitmap>()
  private val spaceObstacleBitmapCache = mutableMapOf<Int, Bitmap>()

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
    buffs[-13] = bakeBuffIconBitmap(-13, 68) // Turn Radius Overcharge
    buffs[-14] = bakeBuffIconBitmap(-14, 68) // Max Flux Overcharge
    buffIconBitmaps = buffs

    riftAnomalyIconBitmaps =
      mapOf(
        "plasma_rift" to bakeRiftAnomalyIconBitmap("plasma_rift", 96),
        "repulsor_rift" to bakeRiftAnomalyIconBitmap("repulsor_rift", 96),
        "gravity_rift" to bakeRiftAnomalyIconBitmap("gravity_rift", 96),
        "phase_rift" to bakeRiftAnomalyIconBitmap("phase_rift", 96),
        "phase_rift_pair" to bakeRiftAnomalyIconBitmap("phase_rift", 96),
      )
  }

  fun getRiftAnomalyIconBitmap(riftId: String): Bitmap {
    return riftAnomalyIconBitmaps[riftId] ?: riftAnomalyIconBitmaps["plasma_rift"]!!
  }

  fun bakeFluxCrystalIconBitmap(sizePx: Int = 64): Bitmap = bake3dFluxReactorIcon(sizePx)

  /**
   * Draws the canonical crisp 6-facet beveled diamond-hex Flux Prism icon (`#E0F2FE`, `#38BDF8`,
   * `#00F5D4`, `#0284C7`, `#0F172A`) — zero lightning bolts, zero gold rings.
   */
  fun drawFluxPrismIcon(
    c: Canvas,
    cx: Float,
    cy: Float,
    r: Float,
    alpha: Int = 255,
  ) {
    val aFrac = (alpha.coerceIn(0, 255)) / 255f
    fun argbHex(baseAlpha: Int, red: Int, green: Int, blue: Int): Int =
      Color.argb((baseAlpha * aFrac).toInt().coerceIn(0, 255), red, green, blue)

    val topY = cy - r
    val upperY = cy - r * 0.34f
    val lowerY = cy + r * 0.34f
    val botY = cy + r
    val halfW = r * 0.68f
    val innerW = r * 0.28f

    // Subtle dark obsidian silhouette backing for crisp contrast on any background
    val shadowPath =
      Path().apply {
        moveTo(cx, topY - r * 0.06f)
        lineTo(cx + halfW + r * 0.05f, upperY)
        lineTo(cx + halfW + r * 0.05f, lowerY)
        lineTo(cx, botY + r * 0.06f)
        lineTo(cx - halfW - r * 0.05f, lowerY)
        lineTo(cx - halfW - r * 0.05f, upperY)
        close()
      }
    val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    fillPaint.color = argbHex(220, 15, 23, 42) // #0F172A
    c.drawPath(shadowPath, fillPaint)

    // Facet 1: Upper-left lit crown (#E0F2FE)
    val f1 =
      Path().apply {
        moveTo(cx, topY)
        lineTo(cx, cy)
        lineTo(cx - halfW, upperY)
        close()
      }
    fillPaint.color = argbHex(255, 224, 242, 254)
    c.drawPath(f1, fillPaint)

    // Facet 2: Upper-right cyan crown (#00F5D4)
    val f2 =
      Path().apply {
        moveTo(cx, topY)
        lineTo(cx + halfW, upperY)
        lineTo(cx, cy)
        close()
      }
    fillPaint.color = argbHex(255, 0, 245, 212)
    c.drawPath(f2, fillPaint)

    // Facet 3: Mid-left sky-blue bevel (#38BDF8)
    val f3 =
      Path().apply {
        moveTo(cx - halfW, upperY)
        lineTo(cx, cy)
        lineTo(cx - halfW, lowerY)
        close()
      }
    fillPaint.color = argbHex(255, 56, 189, 248)
    c.drawPath(f3, fillPaint)

    // Facet 4: Mid-right deep azure bevel (#0284C7)
    val f4 =
      Path().apply {
        moveTo(cx + halfW, upperY)
        lineTo(cx + halfW, lowerY)
        lineTo(cx, cy)
        close()
      }
    fillPaint.color = argbHex(255, 2, 132, 199)
    c.drawPath(f4, fillPaint)

    // Facet 5: Lower-left teal facet (#00F5D4)
    val f5 =
      Path().apply {
        moveTo(cx - halfW, lowerY)
        lineTo(cx, cy)
        lineTo(cx, botY)
        close()
      }
    fillPaint.color = argbHex(245, 0, 245, 212)
    c.drawPath(f5, fillPaint)

    // Facet 6: Lower-right shaded sapphire facet (#0284C7 -> #0F172A)
    val f6 =
      Path().apply {
        moveTo(cx, cy)
        lineTo(cx + halfW, lowerY)
        lineTo(cx, botY)
        close()
      }
    fillPaint.color = argbHex(255, 3, 105, 161)
    c.drawPath(f6, fillPaint)

    // Central luminous inner diamond core (#E0F2FE)
    val coreDiamond =
      Path().apply {
        moveTo(cx, cy - r * 0.44f)
        lineTo(cx + innerW, cy)
        lineTo(cx, cy + r * 0.44f)
        lineTo(cx - innerW, cy)
        close()
      }
    fillPaint.color = argbHex(245, 224, 242, 254)
    c.drawPath(coreDiamond, fillPaint)

    // Crisp outer rim stroke (#E0F2FE)
    val outerHex =
      Path().apply {
        moveTo(cx, topY)
        lineTo(cx + halfW, upperY)
        lineTo(cx + halfW, lowerY)
        lineTo(cx, botY)
        lineTo(cx - halfW, lowerY)
        lineTo(cx - halfW, upperY)
        close()
      }
    val rimPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = (r * 0.11f).coerceAtLeast(1.1f)
        strokeJoin = Paint.Join.ROUND
        color = argbHex(235, 224, 242, 254)
      }
    c.drawPath(outerHex, rimPaint)
  }

  /**
   * Bakes the 6-facet beveled Flux Prism Icon (`fluxIconBitmap`) with an ambient cyan halo.
   */
  fun bake3dFluxReactorIcon(sizePx: Int = 64): Bitmap {
    val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val c = Canvas(bmp)
    val cx = sizePx * 0.5f
    val cy = sizePx * 0.5f
    val r = sizePx * 0.40f
    val haloPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        shader =
          RadialGradient(
            cx,
            cy,
            r * 1.22f,
            intArrayOf(
              Color.argb(165, 56, 189, 248),
              Color.argb(72, 0, 245, 212),
              Color.argb(0, 2, 132, 199),
            ),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP,
          )
      }
    c.drawCircle(cx, cy, r * 1.22f, haloPaint)
    drawFluxPrismIcon(c, cx, cy, r, alpha = 255)
    return bmp
  }

  /**
   * Bakes a 3D Faceted Amethyst Crystal Spire Icon (`riftShardIconBitmap`) emerging from a glowing
   * 3D violet-cyan Rift vortex ring at the base, flanked by 2 smaller angled side shards.
   */
  fun bake3dRiftShardIcon(sizePx: Int = 64): Bitmap {
    val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val c = Canvas(bmp)
    val cx = sizePx * 0.5f
    val cy = sizePx * 0.5f
    val r = sizePx * 0.44f

    // 1. Ambient Amethyst-Violet Halo
    val haloPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        shader =
          RadialGradient(
            cx,
            cy,
            r * 1.12f,
            intArrayOf(
              Color.argb(190, 217, 70, 239),
              Color.argb(88, 124, 58, 237),
              Color.argb(0, 76, 29, 149),
            ),
            floatArrayOf(0f, 0.56f, 1f),
            Shader.TileMode.CLAMP,
          )
      }
    c.drawCircle(cx, cy, r * 1.12f, haloPaint)

    // 2. Back-half of 3D Violet-Cyan Rift Vortex Base Ring
    val vortexY = cy + r * 0.56f
    val vortexRect = RectF(cx - r * 0.80f, vortexY - r * 0.24f, cx + r * 0.80f, vortexY + r * 0.24f)
    val vortexBackPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = (r * 0.11f).coerceAtLeast(1.8f)
        color = Color.parseColor("#7C3AED")
      }
    val vortexWellFill =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.argb(210, 15, 7, 36)
      }
    c.drawOval(vortexRect, vortexWellFill)
    c.drawArc(vortexRect, 180f, 180f, false, vortexBackPaint)

    fun drawFacetedShardCluster(
      ox: Float,
      oy: Float,
      tiltDeg: Float,
      shardW: Float,
      shardH: Float,
    ) {
      c.save()
      c.translate(ox, oy)
      c.rotate(tiltDeg)

      val topY = -shardH
      val shoulderY = -shardH * 0.62f
      val hipY = -shardH * 0.14f
      val botY = shardH * 0.12f

      val shadowOutline =
        Path().apply {
          moveTo(0f, topY)
          lineTo(shardW, shoulderY)
          lineTo(shardW * 0.78f, hipY)
          lineTo(0f, botY)
          lineTo(-shardW * 0.78f, hipY)
          lineTo(-shardW, shoulderY)
          close()
        }
      val darkBasePaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
          style = Paint.Style.FILL
          color = Color.parseColor("#3B0764")
        }
      c.drawPath(shadowOutline, darkBasePaint)

      // Left bright sunlit facet (#F5D0FE -> #E879F9)
      val leftFacet =
        Path().apply {
          moveTo(0f, topY)
          lineTo(-shardW * 0.12f, hipY * 0.85f)
          lineTo(0f, botY)
          lineTo(-shardW * 0.78f, hipY)
          lineTo(-shardW, shoulderY)
          close()
        }
      val leftPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
          style = Paint.Style.FILL
          shader =
            LinearGradient(
              -shardW,
              topY,
              0f,
              botY,
              intArrayOf(
                Color.parseColor("#F5D0FE"),
                Color.parseColor("#E879F9"),
                Color.parseColor("#A855F7"),
              ),
              floatArrayOf(0f, 0.52f, 1f),
              Shader.TileMode.CLAMP,
            )
        }
      c.drawPath(leftFacet, leftPaint)

      // Right deep amethyst facet (#C026D3 -> #581C87)
      val rightFacet =
        Path().apply {
          moveTo(0f, topY)
          lineTo(shardW, shoulderY)
          lineTo(shardW * 0.78f, hipY)
          lineTo(0f, botY)
          lineTo(-shardW * 0.12f, hipY * 0.85f)
          close()
        }
      val rightPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
          style = Paint.Style.FILL
          shader =
            LinearGradient(
              0f,
              topY,
              shardW,
              botY,
              intArrayOf(
                Color.parseColor("#D946EF"),
                Color.parseColor("#9333EA"),
                Color.parseColor("#4C1D95"),
              ),
              floatArrayOf(0f, 0.56f, 1f),
              Shader.TileMode.CLAMP,
            )
        }
      c.drawPath(rightFacet, rightPaint)

      // Center crown facet & crisp white ridge specular highlight
      val crownFacet =
        Path().apply {
          moveTo(0f, topY)
          lineTo(shardW * 0.38f, shoulderY * 0.92f)
          lineTo(-shardW * 0.10f, hipY * 0.82f)
          lineTo(-shardW * 0.44f, shoulderY * 0.94f)
          close()
        }
      val crownPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
          style = Paint.Style.FILL
          color = Color.argb(175, 250, 232, 255)
        }
      c.drawPath(crownFacet, crownPaint)

      val ridgePaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
          style = Paint.Style.STROKE
          strokeWidth = (r * 0.065f).coerceAtLeast(1.1f)
          strokeCap = Paint.Cap.ROUND
          strokeJoin = Paint.Join.ROUND
          color = Color.WHITE
        }
      c.drawLine(0f, topY, -shardW * 0.10f, hipY * 0.82f, ridgePaint)
      c.drawLine(0f, topY, -shardW, shoulderY, ridgePaint)

      val rimStroke =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
          style = Paint.Style.STROKE
          strokeWidth = (r * 0.055f).coerceAtLeast(1.0f)
          strokeJoin = Paint.Join.ROUND
          color = Color.parseColor("#F0ABFC")
        }
      c.drawPath(shadowOutline, rimStroke)
      c.restore()
    }

    // 3. Two angled side crystal shards + tall central sovereign crystal spire
    drawFacetedShardCluster(cx - r * 0.36f, vortexY - r * 0.04f, -24f, r * 0.25f, r * 0.78f)
    drawFacetedShardCluster(cx + r * 0.36f, vortexY - r * 0.04f, 23f, r * 0.24f, r * 0.74f)
    drawFacetedShardCluster(cx, vortexY + r * 0.02f, 0f, r * 0.38f, r * 1.38f)

    // 4. Front-half of 3D Rift Vortex Base Ring overlapping the crystal base (#00F5D4 + #E879F9)
    val vortexFrontGlow =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = (r * 0.17f).coerceAtLeast(2.4f)
        strokeCap = Paint.Cap.ROUND
        color = Color.argb(135, 0, 245, 212)
      }
    val vortexFrontCore =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = (r * 0.095f).coerceAtLeast(1.5f)
        strokeCap = Paint.Cap.ROUND
        color = Color.parseColor("#00F5D4")
      }
    c.drawArc(vortexRect, 0f, 180f, false, vortexFrontGlow)
    c.drawArc(vortexRect, 0f, 180f, false, vortexFrontCore)
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

  private fun bakeSpaceObstacleBitmap(type: SpaceObstacleType, variant: Int, sizePx: Int = 160): Bitmap {
    val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val c = Canvas(bmp)
    val cx = sizePx * 0.5f
    val cy = sizePx * 0.5f
    val r = sizePx * 0.44f

    val obsidianBase =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#040914")
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
            intArrayOf(
              Color.parseColor("#0E1E38"),
              Color.parseColor("#081224"),
              Color.parseColor("#040914"),
            ),
            floatArrayOf(0f, 0.45f, 1f),
            Shader.TileMode.CLAMP,
          )
      }
    val bevelRim =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2.2f
        color = Color.parseColor("#1E3A5F")
      }
    val conduitGlow =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4.2f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        color = Color.argb(95, 0, 245, 212)
      }
    val conduitCore =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.7f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        color = Color.parseColor("#00F5D4")
      }

    when (type) {
      SpaceObstacleType.CRYSTAL_ASTEROID -> {
        // Sleek 8-sided 45°-chamfered Pitch-Black Obsidian Monolith (#040914) with #081224 3D bevels & #00F5D4 conduits
        val oct = Path()
        for (i in 0 until 8) {
          val a = (i * Math.PI * 0.25 + Math.PI * 0.125).toFloat()
          val px = cx + kotlin.math.cos(a) * r * 0.94f
          val py = cy + kotlin.math.sin(a) * r * 0.94f
          if (i == 0) oct.moveTo(px, py) else oct.lineTo(px, py)
        }
        oct.close()
        c.drawPath(oct, obsidianBase)
        c.drawPath(oct, bevelRim)

        val innerOct = Path()
        for (i in 0 until 8) {
          val a = (i * Math.PI * 0.25 + Math.PI * 0.125).toFloat()
          val px = cx + kotlin.math.cos(a) * r * 0.72f
          val py = cy + kotlin.math.sin(a) * r * 0.72f
          if (i == 0) innerOct.moveTo(px, py) else innerOct.lineTo(px, py)
        }
        innerOct.close()
        c.drawPath(innerOct, bevelFill)
        c.drawPath(innerOct, Paint(bevelRim).apply { strokeWidth = 1.4f })

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
        // Pitch-Black #040914 Ringed Orbital Station with #081224 torus bevel, 4 structural spokes & cyan reactor hub
        val wingAngle = if (variant == 0) -18f else 24f
        c.save()
        c.translate(cx, cy)
        c.rotate(wingAngle)

        val spokePaint =
          Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 9f
            color = Color.parseColor("#040914")
          }
        val spokeRim =
          Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 3.2f
            color = Color.parseColor("#0E1E38")
          }
        for (s in 0 until 4) {
          val sa = s * (Math.PI.toFloat() * 0.5f) + 0.785f
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
            color = Color.parseColor("#040914")
          },
        )
        c.drawCircle(
          0f,
          0f,
          torusMidR,
          Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = r * 0.14f
            color = Color.parseColor("#081224")
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
                  Color.parseColor("#00F5D4"),
                  Color.parseColor("#0284C7"),
                  Color.parseColor("#040914"),
                ),
                floatArrayOf(0f, 0.34f, 0.72f, 1f),
                Shader.TileMode.CLAMP,
              )
          }
        c.drawCircle(0f, 0f, hubR * 0.88f, hubGlow)
        c.restore()
      }

      SpaceObstacleType.ASTEROID_STATION -> {
        // Pitch-Black #040914 Chamfered Reactor Outpost Housing + Recessed #38BDF8 Docking Bay + Dish Node
        val hullRect = RectF(cx - r * 0.88f, cy - r * 0.78f, cx + r * 0.88f, cy + r * 0.78f)
        val ch = r * 0.26f
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
          Paint(conduitCore).apply { color = Color.parseColor("#38BDF8") },
        )

        val nodeX = if (variant == 0) cx - r * 0.46f else cx + r * 0.46f
        c.drawCircle(nodeX, cy, r * 0.22f, obsidianBase)
        c.drawCircle(nodeX, cy, r * 0.22f, bevelRim)
        c.drawCircle(nodeX, cy, r * 0.09f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#00F5D4") })
      }

      SpaceObstacleType.FLOATING_DEBRIS -> {
        // Pitch-Black #040914 Angled Reactor Pylon Segment with #081224 armor ribs & #38BDF8 energy conduits
        c.save()
        c.translate(cx, cy)
        c.rotate(if (variant == 0) -22f else 28f)
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
        // Obsidian #040914 Forcefield Emitter Pylon with #00F5D4 core lens
        c.drawCircle(cx, cy, r * 0.76f, obsidianBase)
        c.drawCircle(cx, cy, r * 0.76f, bevelRim)
        c.drawCircle(cx, cy, r * 0.34f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#00F5D4") })
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
    if (buffType in 0..10 || buffType in -14..-10) {
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
        // FULL FLUX CHARGE: 6-facet beveled Flux Prism
        drawFluxPrismIcon(c, cx, cy, r * 0.64f, alpha = 255)
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
        // REACTOR OVERCHARGE (+8% x3): Capacitor diamond ring + central Flux Prism
        val diamond =
          Path().apply {
            moveTo(cx, cy - r * 0.56f)
            lineTo(cx + r * 0.52f, cy)
            lineTo(cx, cy + r * 0.56f)
            lineTo(cx - r * 0.52f, cy)
            close()
          }
        c.drawPath(diamond, glyphStrokePaint)
        drawFluxPrismIcon(c, cx, cy, r * 0.42f, alpha = 255)
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
        // MAX FLUX OVERCHARGE (+10% x3): Glowing #00F5D4 cell with Flux Prism
        val cellRect = RectF(cx - r * 0.50f, cy - r * 0.50f, cx + r * 0.50f, cy + r * 0.50f)
        c.drawRoundRect(cellRect, 6f, 6f, glyphStrokePaint)
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
  ) {
    when (type) {
      AnomalyType.PLASMA_RIFT -> {
        draw3dPlayfieldRift(
          canvas = canvas,
          cx = cx,
          cy = cy,
          r = radius,
          spinAngle = spinAngle,
          fadeAlpha = fadeAlpha,
          outerColor = Color.rgb(239, 68, 68),
          midColor = Color.rgb(217, 70, 239),
          coreColor = Color.rgb(255, 228, 230),
          darkVoidCenter = false,
          inwardSpiral = false,
          pulsePhaseOverride = pulsePhaseOverride,
        )
      }
      AnomalyType.REPULSOR_RIFT -> {
        draw3dPlayfieldRift(
          canvas = canvas,
          cx = cx,
          cy = cy,
          r = radius,
          spinAngle = -spinAngle * 1.15f,
          fadeAlpha = fadeAlpha,
          outerColor = Color.rgb(56, 189, 248),
          midColor = Color.rgb(186, 230, 253),
          coreColor = Color.rgb(255, 255, 255),
          darkVoidCenter = false,
          inwardSpiral = false,
          pulsePhaseOverride = pulsePhaseOverride,
        )
      }
      AnomalyType.GRAVITY_RIFT -> {
        val growthScale = 1f + (absorbedCount.coerceIn(0, 12) * 0.015f)
        draw3dPlayfieldRift(
          canvas = canvas,
          cx = cx,
          cy = cy,
          r = radius * growthScale,
          spinAngle = spinAngle * 1.35f,
          fadeAlpha = fadeAlpha,
          outerColor = Color.rgb(168, 85, 247),
          midColor = Color.rgb(99, 102, 241),
          coreColor = Color.rgb(233, 213, 255),
          darkVoidCenter = true,
          inwardSpiral = true,
          pulsePhaseOverride = pulsePhaseOverride,
        )
      }
      AnomalyType.PHASE_RIFT_A -> {
        draw3dPlayfieldRift(
          canvas = canvas,
          cx = cx,
          cy = cy,
          r = radius,
          spinAngle = spinAngle,
          fadeAlpha = fadeAlpha,
          outerColor = Color.rgb(45, 212, 191),
          midColor = Color.rgb(56, 189, 248),
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
          r = radius,
          spinAngle = -spinAngle,
          fadeAlpha = fadeAlpha,
          outerColor = Color.rgb(56, 189, 248),
          midColor = Color.rgb(168, 85, 247),
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
      else -> {
        val c1x = sizePx * 0.32f
        val c1y = sizePx * 0.36f
        val c2x = sizePx * 0.68f
        val c2y = sizePx * 0.64f
        val portalR = sizePx * 0.21f
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
        draw3dPlayfieldRift(c, AnomalyType.PHASE_RIFT_A, c1x, c1y, portalR, spinAngle = 0.45f, pulsePhaseOverride = 0.38f)
        draw3dPlayfieldRift(c, AnomalyType.PHASE_RIFT_B, c2x, c2y, portalR, spinAngle = -0.45f, pulsePhaseOverride = 0.62f)
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
}
