package com.example.ui

import android.app.Activity
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.LocalPilotPrefs
import com.example.data.RiftBillingManager
import com.example.data.ShardPurchaseResult
import com.example.data.ShardVaultSkuPack
import com.example.engine.CosmeticCatalogItem
import com.example.engine.CustomShipLoadoutSlot
import com.example.engine.FrameSlotGateInfo
import com.example.engine.PrimaryWeaponOption
import com.example.engine.RiftCodex
import com.example.engine.RiftProgressionCatalog
import com.example.engine.RiftSpriteBaker
import com.example.engine.SecondaryAbilityOption
import com.example.engine.ShipSpec
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.launch

/**
 * Reusable Vertical Card Standard used across Rift Hub, Hangar, Rift Frame Cosmetics,
 * Conduit Rank Track, and Shard Store packs:
 * - Sculpted dark gunmetal/abyssal slate chassis with top-left catchlight
 * - Optional top-right status/unread badge
 * - Centered visual/icon on top
 * - Full untruncated uppercase Title below
 * - 2-5 word plain-English description underneath
 * - Optional bottom cost / status pill (using RiftShardGemIcon rather than 'S' text)
 */
@Composable
internal fun RiftVerticalCard(
  modifier: Modifier = Modifier,
  title: String,
  subtitle: String,
  accent: Color,
  secondaryAccent: Color = Color(0xFF38BDF8),
  isRightColumn: Boolean = false,
  isHighlighted: Boolean = false,
  badgeText: String? = null,
  badgeColor: Color = secondaryAccent,
  shardCost: Long? = null,
  statusLabel: String? = null,
  statusColor: Color = accent,
  isTabletDeck: Boolean = false,
  minHeightDp: Dp = 96.dp,
  testTag: String = "",
  visualContent: @Composable (Dp) -> Unit,
  onClick: () -> Unit,
) {
  val titleGlow =
    remember(accent, isHighlighted) {
      Shadow(
        color = if (isHighlighted) Color.White.copy(alpha = 0.50f) else accent.copy(alpha = 0.30f),
        offset = Offset.Zero,
        blurRadius = 4f,
      )
    }

  BoxWithConstraints(
    modifier =
      modifier
        .minimumInteractiveComponentSize()
        .heightIn(min = minHeightDp.coerceAtLeast(48.dp))
        .drawBehind {
          val w = size.width
          val h = size.height
          if (w <= 8f || h <= 8f) return@drawBehind
          val drop = 2.2.dp.toPx()
          val outerCut = 11.dp.toPx().coerceAtMost(h * 0.20f)
          val innerCut = 5.dp.toPx()
          val bodyH = (h - drop).coerceAtLeast(8f)

          fun cardPath(yOff: Float): Path =
            Path().apply {
              if (!isRightColumn) {
                moveTo(outerCut, yOff)
                lineTo(w - innerCut, yOff)
                lineTo(w, yOff + innerCut)
                lineTo(w, yOff + bodyH - innerCut)
                lineTo(w - innerCut, yOff + bodyH)
                lineTo(outerCut, yOff + bodyH)
                lineTo(0f, yOff + bodyH - outerCut)
                lineTo(0f, yOff + outerCut)
                close()
              } else {
                moveTo(innerCut, yOff)
                lineTo(w - outerCut, yOff)
                lineTo(w, yOff + outerCut)
                lineTo(w, yOff + bodyH - outerCut)
                lineTo(w - outerCut, yOff + bodyH)
                lineTo(innerCut, yOff + bodyH)
                lineTo(0f, yOff + bodyH - innerCut)
                lineTo(0f, yOff + innerCut)
                close()
              }
            }

          drawPath(cardPath(drop), color = Color(0xE0010308))
          drawPath(
            path = cardPath(0f),
            brush =
              Brush.verticalGradient(
                if (isHighlighted) {
                  listOf(Color(0xFA102442), Color(0xF8091528), Color(0xFC030711))
                } else {
                  listOf(Color(0xFA0B192E), Color(0xF8060D1A), Color(0xFC030711))
                }
              ),
          )
          drawPath(
            path = cardPath(0f),
            brush =
              Brush.horizontalGradient(
                if (isRightColumn) {
                  listOf(
                    accent.copy(alpha = if (isHighlighted) 0.98f else 0.85f),
                    Color(0x551E293B),
                    secondaryAccent.copy(alpha = 0.72f),
                  )
                } else {
                  listOf(
                    secondaryAccent.copy(alpha = 0.72f),
                    Color(0x551E293B),
                    accent.copy(alpha = if (isHighlighted) 0.98f else 0.85f),
                  )
                }
              ),
            style = Stroke(width = if (isHighlighted) 1.6.dp.toPx() else 1.2.dp.toPx()),
          )
          drawLine(
            brush =
              Brush.horizontalGradient(
                listOf(
                  Color.White.copy(alpha = if (isHighlighted) 0.56f else 0.42f),
                  accent.copy(alpha = 0.40f),
                  Color.Transparent,
                )
              ),
            start = Offset((if (isRightColumn) innerCut else outerCut) + 4.dp.toPx(), 1.3.dp.toPx()),
            end = Offset(w * 0.72f, 1.3.dp.toPx()),
            strokeWidth = 1.1.dp.toPx(),
          )
        }
        .clickable { onClick() }
        .padding(horizontal = if (isTabletDeck) 9.dp else 7.dp, vertical = if (isTabletDeck) 7.dp else 5.dp)
        .let { base -> if (testTag.isNotBlank()) base.testTag(testTag) else base },
  ) {
    val compactHeight = maxHeight < 108.dp
    val visualDp =
      when {
        maxHeight >= 156.dp -> if (isTabletDeck) 56.dp else 46.dp
        maxHeight >= 116.dp -> if (isTabletDeck) 46.dp else 38.dp
        else -> if (isTabletDeck) 38.dp else 32.dp
      }

    if (!badgeText.isNullOrBlank()) {
      Box(
        modifier =
          Modifier.align(Alignment.TopEnd)
            .clip(RoundedCornerShape(4.dp))
            .background(badgeColor.copy(alpha = 0.16f))
            .border(0.8.dp, badgeColor.copy(alpha = 0.85f), RoundedCornerShape(4.dp))
            .padding(horizontal = 5.dp, vertical = 1.5.dp),
      ) {
        Text(
          text = badgeText,
          color = badgeColor,
          fontSize = if (isTabletDeck) 8.4.sp else 7.4.sp,
          fontWeight = FontWeight.Black,
          fontFamily = FontFamily.Monospace,
          maxLines = 1,
          softWrap = false,
          style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
        )
      }
    }

    Column(
      modifier = Modifier.fillMaxWidth().align(Alignment.Center),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(if (compactHeight) 2.5.dp else 4.dp),
    ) {
      visualContent(visualDp)

      Text(
        text = title,
        color = Color(0xFFF8FAFC),
        fontSize = if (isTabletDeck) 12.0.sp else 10.6.sp,
        fontWeight = FontWeight.Black,
        fontFamily = FontFamily.Monospace,
        textAlign = TextAlign.Center,
        lineHeight = if (isTabletDeck) 14.sp else 12.5.sp,
        maxLines = 2,
        softWrap = true,
        style =
          TextStyle(
            shadow = titleGlow,
            platformStyle = PlatformTextStyle(includeFontPadding = false),
          ),
      )

      Text(
        text = subtitle,
        color = Color(0xFF94A3B8),
        fontSize = if (isTabletDeck) 9.4.sp else 8.4.sp,
        fontWeight = FontWeight.SemiBold,
        fontFamily = FontFamily.SansSerif,
        textAlign = TextAlign.Center,
        lineHeight = if (isTabletDeck) 12.sp else 10.5.sp,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
      )

      if (shardCost != null && shardCost > 0L) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          RiftShardGemIcon(sizeDp = if (isTabletDeck) 13.dp else 11.dp)
          Text(
            text = formatOverwatchResonance(shardCost.toInt()),
            color = Color(0xFF38BDF8),
            fontSize = if (isTabletDeck) 9.4.sp else 8.4.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
            style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
          )
        }
      } else if (!statusLabel.isNullOrBlank()) {
        Text(
          text = statusLabel,
          color = statusColor,
          fontSize = if (isTabletDeck) 9.2.sp else 8.2.sp,
          fontWeight = FontWeight.Black,
          fontFamily = FontFamily.Monospace,
          textAlign = TextAlign.Center,
          maxLines = 1,
          style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
        )
      }
    }
  }
}

@Composable
internal fun SubspaceWaveformEmblem(
  sizeDp: Dp,
  hasUnread: Boolean,
) {
  Box(modifier = Modifier.size(sizeDp), contentAlignment = Alignment.Center) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val cx = center.x
      val cy = center.y
      val r = size.minDimension * 0.45f
      drawCircle(
        brush = Brush.radialGradient(listOf(Color(0xFF0E2C47), Color(0xFF060D1A), Color(0xFF02050B))),
        radius = r,
        center = center,
      )
      drawCircle(
        color = if (hasUnread) Color(0xFFFB7185) else Color(0xFF00F5D4),
        radius = r,
        center = center,
        style = Stroke(width = 1.4.dp.toPx()),
      )
      val barCount = 7
      val barSpan = r * 1.18f
      val startX = cx - barSpan * 0.5f
      val step = barSpan / (barCount - 1).coerceAtLeast(1)
      val heights = floatArrayOf(0.32f, 0.58f, 0.88f, 1.0f, 0.82f, 0.50f, 0.30f)
      for (i in 0 until barCount) {
        val x = startX + i * step
        val halfH = r * 0.46f * heights[i]
        drawLine(
          brush = Brush.verticalGradient(listOf(Color(0xFF38BDF8), Color(0xFF00F5D4))),
          start = Offset(x, cy - halfH),
          end = Offset(x, cy + halfH),
          strokeWidth = 2.1.dp.toPx(),
          cap = StrokeCap.Round,
        )
      }
    }
  }
}

@Composable
internal fun ActiveRiftsEmblem(
  sizeDp: Dp,
) {
  Box(modifier = Modifier.size(sizeDp), contentAlignment = Alignment.Center) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val cx = center.x
      val cy = center.y
      val r = size.minDimension * 0.45f
      drawCircle(
        brush = Brush.radialGradient(listOf(Color(0xFF281A06), Color(0xFF091528), Color(0xFF02050B))),
        radius = r,
        center = center,
      )
      drawCircle(
        brush = Brush.sweepGradient(listOf(Color(0xFFFFD700), Color(0xFF00F5D4), Color(0xFFA855F7), Color(0xFFFFD700))),
        radius = r,
        center = center,
        style = Stroke(width = 1.6.dp.toPx()),
      )
      drawArc(
        color = Color(0xFF00F5D4),
        startAngle = -35f,
        sweepAngle = 115f,
        useCenter = false,
        topLeft = Offset(cx - r * 0.68f, cy - r * 0.68f),
        size = Size(r * 1.36f, r * 1.36f),
        style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round),
      )
      drawArc(
        color = Color(0xFFFFD700),
        startAngle = 145f,
        sweepAngle = 115f,
        useCenter = false,
        topLeft = Offset(cx - r * 0.68f, cy - r * 0.68f),
        size = Size(r * 1.36f, r * 1.36f),
        style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round),
      )
      drawCircle(
        brush = Brush.radialGradient(listOf(Color(0xFF00F5D4).copy(alpha = 0.55f), Color.Transparent)),
        radius = r * 0.42f,
        center = center,
      )
    }
  }
}

@Composable
internal fun PrismaticShardClusterEmblem(
  sizeDp: Dp,
  accentColor: Color,
  crystalCount: Int = 3,
) {
  Box(modifier = Modifier.size(sizeDp), contentAlignment = Alignment.Center) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val cx = center.x
      val cy = center.y
      val r = size.minDimension * 0.45f
      drawCircle(
        brush = Brush.radialGradient(listOf(accentColor.copy(alpha = 0.25f), Color(0xFF060D1A), Color(0xFF02050B))),
        radius = r,
        center = center,
      )
      drawCircle(
        color = accentColor.copy(alpha = 0.78f),
        radius = r,
        center = center,
        style = Stroke(width = 1.25.dp.toPx()),
      )

      fun drawFacetedGem(gx: Float, gy: Float, gw: Float, gh: Float, fillA: Color, fillB: Color) {
        val p =
          Path().apply {
            moveTo(gx, gy - gh * 0.5f)
            lineTo(gx + gw * 0.5f, gy - gh * 0.08f)
            lineTo(gx + gw * 0.28f, gy + gh * 0.5f)
            lineTo(gx - gw * 0.28f, gy + gh * 0.5f)
            lineTo(gx - gw * 0.5f, gy - gh * 0.08f)
            close()
          }
        drawPath(p, brush = Brush.verticalGradient(listOf(fillA, fillB)))
        drawPath(p, color = Color.White.copy(alpha = 0.65f), style = Stroke(width = 0.9.dp.toPx()))
        drawLine(
          color = Color.White.copy(alpha = 0.55f),
          start = Offset(gx, gy - gh * 0.48f),
          end = Offset(gx, gy + gh * 0.44f),
          strokeWidth = 0.8.dp.toPx(),
        )
      }

      if (crystalCount >= 3) {
        drawFacetedGem(cx - r * 0.34f, cy + r * 0.08f, r * 0.44f, r * 0.68f, Color(0xFF38BDF8), accentColor)
        drawFacetedGem(cx + r * 0.34f, cy + r * 0.08f, r * 0.44f, r * 0.68f, Color(0xFF00F5D4), accentColor)
      } else if (crystalCount == 2) {
        drawFacetedGem(cx - r * 0.28f, cy + r * 0.06f, r * 0.48f, r * 0.72f, Color(0xFF38BDF8), accentColor)
      }
      drawFacetedGem(cx, cy - r * 0.02f, r * 0.60f, r * 0.96f, Color(0xFFE0F2FE), accentColor)
    }
  }
}

@Composable
internal fun CosmeticSwatchEmblem(
  item: CosmeticCatalogItem,
  sizeDp: Dp,
) {
  val accent = Color(item.accentColorHex)
  Box(modifier = Modifier.size(sizeDp), contentAlignment = Alignment.Center) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val cx = center.x
      val cy = center.y
      val r = size.minDimension * 0.44f
      drawCircle(
        brush = Brush.radialGradient(listOf(accent.copy(alpha = 0.28f), Color(0xFF060D1A), Color(0xFF02050B))),
        radius = r,
        center = center,
      )
      drawCircle(
        color = accent,
        radius = r,
        center = center,
        style = Stroke(width = 1.3.dp.toPx()),
      )
      when (item.category) {
        "HULL_TRIM" -> {
          val hull =
            Path().apply {
              moveTo(cx, cy - r * 0.58f)
              lineTo(cx + r * 0.48f, cy + r * 0.42f)
              lineTo(cx, cy + r * 0.18f)
              lineTo(cx - r * 0.48f, cy + r * 0.42f)
              close()
            }
          drawPath(hull, brush = Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.85f), accent)))
          drawPath(hull, color = Color.White, style = Stroke(width = 1.0.dp.toPx()))
        }
        "KILL_FINISHER" -> {
          for (i in 0 until 8) {
            val a = (i * 45.0 * PI / 180.0).toFloat()
            val inner = r * 0.18f
            val outer = if (i % 2 == 0) r * 0.66f else r * 0.44f
            drawLine(
              color = if (i % 2 == 0) Color.White else accent,
              start = Offset(cx + cos(a) * inner, cy + sin(a) * inner),
              end = Offset(cx + cos(a) * outer, cy + sin(a) * outer),
              strokeWidth = 1.6.dp.toPx(),
              cap = StrokeCap.Round,
            )
          }
        }
        "WARP_SIGNATURE" -> {
          drawCircle(color = accent, radius = r * 0.56f, style = Stroke(width = 1.6.dp.toPx()))
          drawArc(
            color = Color.White,
            startAngle = -140f,
            sweepAngle = 110f,
            useCenter = false,
            topLeft = Offset(cx - r * 0.36f, cy - r * 0.36f),
            size = Size(r * 0.72f, r * 0.72f),
            style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round),
          )
        }
        else -> {
          // HVT_HALO (Rift Leader Crown Halo)
          drawCircle(color = accent, radius = r * 0.58f, style = Stroke(width = 1.8.dp.toPx()))
          val crown =
            Path().apply {
              moveTo(cx - r * 0.36f, cy + r * 0.16f)
              lineTo(cx - r * 0.30f, cy - r * 0.24f)
              lineTo(cx - r * 0.10f, cy - r * 0.02f)
              lineTo(cx, cy - r * 0.34f)
              lineTo(cx + r * 0.10f, cy - r * 0.02f)
              lineTo(cx + r * 0.30f, cy - r * 0.24f)
              lineTo(cx + r * 0.36f, cy + r * 0.16f)
              close()
            }
          drawPath(crown, color = accent.copy(alpha = 0.85f))
          drawPath(crown, color = Color.White, style = Stroke(width = 0.9.dp.toPx()))
        }
      }
    }
  }
}

internal fun cleanShipRoleSummary(role: String): String =
  when {
    role.contains("Interceptor", ignoreCase = true) -> "Fast precision striker"
    role.contains("Twin", ignoreCase = true) -> "Dual laser skirmisher"
    role.contains("EMP", ignoreCase = true) || role.contains("Disrupt", ignoreCase = true) -> "Cloak and EMP ambush"
    role.contains("Scatter", ignoreCase = true) || role.contains("Brawl", ignoreCase = true) -> "Close-range scatter brawler"
    role.contains("Aura", ignoreCase = true) || role.contains("Wave", ignoreCase = true) -> "360-degree pulse defender"
    role.contains("Ricochet", ignoreCase = true) || role.contains("Bank", ignoreCase = true) -> "Multi-bounce bank shooter"
    role.contains("Heavy", ignoreCase = true) || role.contains("Siege", ignoreCase = true) -> "Heavy sniper siege hull"
    role.contains("Stasis", ignoreCase = true) || role.contains("Dome", ignoreCase = true) -> "Tactical stasis zone control"
    role.contains("Laser", ignoreCase = true) || role.contains("Suppress", ignoreCase = true) -> "Twin-beam suppression cruiser"
    else -> "Custom 100-point modular hull"
  }

@Composable
internal fun HangarVerticalWingBay(
  ships: List<ShipSpec>,
  isRightSide: Boolean,
  isTabletDeck: Boolean,
  headerHeight: Dp,
  wingStackGap: Dp,
  chargedShipId: Int,
  bestShipId: Int,
  inspectedShipId: Int,
  shipSpinDeg: Float,
  fluxOrbitRad: Float,
  shipResonanceFor: (Int) -> Int,
  onBackToHub: () -> Unit,
  onOpenStudio: () -> Unit,
  onTapShip: (Int) -> Unit,
) {
  Column(
    modifier = Modifier.fillMaxSize(),
    verticalArrangement = Arrangement.spacedBy(wingStackGap),
  ) {
    if (!isRightSide) {
      HubDigitizedReturnBar(
        heightDp = headerHeight,
        isRightSide = false,
        isTabletDeck = isTabletDeck,
        sectionTitle = "HANGAR 1-5",
        accent = Color(0xFF00F5D4),
        onBackToHub = onBackToHub,
      )
    } else {
      HubDigitizedReturnBar(
        heightDp = headerHeight,
        isRightSide = true,
        isTabletDeck = isTabletDeck,
        sectionTitle = "HANGAR 6-10",
        accent = Color(0xFF38BDF8),
        trailingLabel = "RIFT FRAME >",
        trailingTag = "hangar_bay_open_studio",
        onTrailingClick = onOpenStudio,
      )
    }

    Column(
      modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(wingStackGap),
    ) {
      ships.forEach { ship ->
        val isCharged = chargedShipId == ship.id
        val isBest = bestShipId == ship.id
        val isInspected = inspectedShipId == ship.id
        val cleanName = remember(ship.name) {
          ship.name.replace(Regex("^[0-9]+-"), "").trim().uppercase(Locale.US)
        }
        val shipRes = shipResonanceFor(ship.id)
        val primaryBorder =
          when {
            isCharged -> Color(0xFF00F5D4)
            isBest -> Color(0xFFFACC15)
            isInspected -> Color.White
            else -> Color(0xFF38BDF8)
          }
        val secondaryBorder =
          when {
            isCharged -> Color(0xFF38BDF8)
            isBest -> Color(0xFFFFD700)
            else -> Color(0xFF091528)
          }
        val badgeText =
          when {
            isCharged -> "CHARGED"
            isBest -> "BEST"
            else -> null
          }

        RiftVerticalCard(
          modifier = Modifier.fillMaxWidth(),
          title = cleanName,
          subtitle = cleanShipRoleSummary(ship.role),
          accent = primaryBorder,
          secondaryAccent = secondaryBorder,
          isRightColumn = isRightSide,
          isHighlighted = isCharged || isBest || isInspected,
          badgeText = badgeText,
          badgeColor = if (isCharged) Color(0xFF00F5D4) else Color(0xFFFACC15),
          isTabletDeck = isTabletDeck,
          minHeightDp = if (isTabletDeck) 120.dp else 104.dp,
          testTag = "hangar_bay_ship_${ship.id}",
          visualContent = { visualDp ->
            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
              Spinning25dShipWithOrbitingFlux(
                shipId = ship.id,
                shipSpinDeg = shipSpinDeg,
                fluxOrbitRad = fluxOrbitRad,
                isCharged = isCharged,
                isBestShip = isBest,
                isSelected = isInspected,
                sizeDp = visualDp,
              )
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp),
              ) {
                ResonanceCrestIcon(sizeDp = 10.dp)
                Text(
                  text = formatOverwatchResonance(shipRes),
                  color = Color(0xFFFACC15),
                  fontSize = if (isTabletDeck) 8.6.sp else 7.8.sp,
                  fontWeight = FontWeight.Black,
                  fontFamily = FontFamily.Monospace,
                  maxLines = 1,
                  style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
                )
              }
            }
          },
          onClick = { onTapShip(ship.id) },
        )
      }
    }
  }
}

@Composable
internal fun RiftFrameLoadoutSideBay(
  isTabletDeck: Boolean,
  headerHeight: Dp,
  wingStackGap: Dp,
  activeCustomSlot: CustomShipLoadoutSlot,
  activeCustomSlotId: String,
  pointBudget: Int,
  isCharged: Boolean,
  localPrefs: LocalPilotPrefs,
  currentUserId: String,
  onBackToHub: () -> Unit,
  onSelectFrameSlot: (String) -> Unit,
  onSaveCustomSlot: (CustomShipLoadoutSlot) -> Unit,
  onPreviewWeaponStage: (OverwatchCircularPrompt.InfoStageInspect) -> Unit,
  onPreviewInOverwatch: () -> Unit,
  onChargeRiftFrame: () -> Unit,
) {
  val density = LocalDensity.current
  val keyboardController = LocalSoftwareKeyboardController.current
  val uidOrNull = currentUserId.ifBlank { null }
  val effectiveBudget = pointBudget.coerceAtLeast(100)
  val normalizedActiveId = localPrefs.normalizeCustomSlotId(activeCustomSlotId)
  var draftSlot by remember(activeCustomSlot, normalizedActiveId, effectiveBudget) {
    mutableStateOf(activeCustomSlot.copy(slotId = normalizedActiveId).clampToPointBudget(effectiveBudget))
  }
  var isEditingName by remember(normalizedActiveId) { mutableStateOf(false) }
  var renameDraft by remember(activeCustomSlot.customName, normalizedActiveId) {
    mutableStateOf(activeCustomSlot.customName.ifBlank { normalizedActiveId })
  }
  var weaponTab by remember { mutableStateOf("PRI") }

  val primaryOptions = remember { CustomShipLoadoutSlot.PRIMARY_OPTIONS }
  val secondaryOptions = remember(RiftSpriteBaker.customShip9Version) {
    CustomShipLoadoutSlot.getAllAvailableSecondaryOptions()
  }

  val usedPoints = draftSlot.totalAllocatedPoints()
  val remainingPoints = (effectiveBudget - usedPoints).coerceAtLeast(0)

  fun commitDraftClamped(updated: CustomShipLoadoutSlot) {
    val clamped = updated.clampToPointBudget(effectiveBudget)
    draftSlot = clamped
    onSaveCustomSlot(clamped)
  }

  fun commitRename() {
    val cleanName =
      renameDraft
        .trim()
        .replace(Regex("[^a-zA-Z0-9 _\\-]"), "")
        .take(16)
        .ifBlank { normalizedActiveId }
    renameDraft = cleanName
    isEditingName = false
    keyboardController?.hide()
    commitDraftClamped(draftSlot.copy(customName = cleanName))
  }

  Column(
    modifier = Modifier.fillMaxSize().testTag("rift_frame_left_loadout_bay"),
    verticalArrangement = Arrangement.spacedBy(wingStackGap),
  ) {
    HubDigitizedReturnBar(
      heightDp = headerHeight,
      isRightSide = false,
      isTabletDeck = isTabletDeck,
      sectionTitle = "RIFT FRAME • $usedPoints/$effectiveBudget PTS",
      accent = Color(0xFFC084FC),
      trailingLabel = "INSPECT >",
      trailingTag = "rift_frame_preview_overwatch",
      onTrailingClick = onPreviewInOverwatch,
      onBackToHub = onBackToHub,
    )

    // Frame A..E Loadout Selector Pills
    Row(
      modifier =
        Modifier.fillMaxWidth()
          .height(48.dp)
          .chamferedSciFiPodSurface(
            isRightSide = false,
            primaryBorder = Color(0xFFC084FC),
            secondaryBorder = Color(0xFF00F5D4),
          )
          .padding(horizontal = 4.dp),
      horizontalArrangement = Arrangement.spacedBy(3.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      listOf("A", "B", "C", "D", "E").forEach { shortKey ->
        val fullSlotId = localPrefs.normalizeCustomSlotId(shortKey)
        val isSelected = normalizedActiveId == fullSlotId
        val isUnlocked = localPrefs.isCustomSlotUnlocked(fullSlotId, uidOrNull)
        Box(
          modifier =
            Modifier.weight(1f)
              .fillMaxHeight()
              .minimumInteractiveComponentSize()
              .clickable { onSelectFrameSlot(fullSlotId) }
              .padding(vertical = 5.dp)
              .testTag("rift_frame_slot_$shortKey"),
          contentAlignment = Alignment.Center,
        ) {
          Box(
            modifier =
              Modifier.fillMaxSize()
                .clip(RoundedCornerShape(6.dp))
                .background(
                  when {
                    isSelected -> Color(0x44C084FC)
                    isUnlocked -> Color(0x66060D1A)
                    else -> Color(0x6602050B)
                  }
                )
                .border(
                  1.dp,
                  when {
                    isSelected -> Color(0xFF00F5D4)
                    isUnlocked -> Color(0xFF38BDF8)
                    else -> Color(0x66FACC15)
                  },
                  RoundedCornerShape(6.dp),
                ),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text = if (isUnlocked) shortKey else "${shortKey}*",
              color =
                when {
                  isSelected -> Color.White
                  isUnlocked -> Color(0xFFE2E8F0)
                  else -> Color(0xFFFACC15)
                },
              fontSize = 8.8.sp,
              fontWeight = FontWeight.Black,
              fontFamily = FontFamily.Monospace,
            )
          }
        }
      }
    }

    Column(
      modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(wingStackGap),
    ) {
      // Exact 100-Point Budget Readout + Frame Rename & Reset Points Controls
      Column(
        modifier =
          Modifier.fillMaxWidth()
            .chamferedSciFiPodSurface(
              isRightSide = false,
              primaryBorder = if (remainingPoints == 0) Color(0xFF00F5D4) else Color(0xFFFFD700),
              secondaryBorder = Color(0xFF091528),
            )
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
      ) {
        Text(
          text = "USED: $usedPoints / $effectiveBudget PTS • REMAINING: $remainingPoints PTS",
          color = if (remainingPoints == 0) Color(0xFF00F5D4) else Color(0xFFFACC15),
          fontSize = if (isTabletDeck) 9.0.sp else 8.0.sp,
          fontWeight = FontWeight.Black,
          fontFamily = FontFamily.Monospace,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )

        if (isEditingName) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
          ) {
            BasicTextField(
              value = renameDraft,
              onValueChange = { renameDraft = it.take(16) },
              singleLine = true,
              textStyle =
                TextStyle(
                  color = Color.White,
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold,
                  fontFamily = FontFamily.Monospace,
                ),
              cursorBrush = SolidColor(Color(0xFF00F5D4)),
              keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
              keyboardActions = KeyboardActions(onDone = { commitRename() }),
              modifier =
                Modifier.weight(1f)
                  .heightIn(min = 38.dp)
                  .clip(RoundedCornerShape(6.dp))
                  .background(Color(0xFF02050B))
                  .border(1.dp, Color(0xFF00F5D4), RoundedCornerShape(6.dp))
                  .padding(horizontal = 8.dp, vertical = 8.dp)
                  .testTag("rift_frame_rename_input"),
            )
            Box(
              modifier =
                Modifier.minimumInteractiveComponentSize()
                  .height(48.dp)
                  .clickable { commitRename() }
                  .testTag("rift_frame_rename_save"),
              contentAlignment = Alignment.Center,
            ) {
              Box(
                modifier =
                  Modifier.clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF0D3554))
                    .border(1.dp, Color(0xFF00F5D4), RoundedCornerShape(6.dp))
                    .padding(horizontal = 9.dp, vertical = 6.dp),
              ) {
                Text(
                  text = "SAVE",
                  color = Color.White,
                  fontSize = 8.4.sp,
                  fontWeight = FontWeight.Black,
                  fontFamily = FontFamily.Monospace,
                )
              }
            }
          }
        } else {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = draftSlot.customName.ifBlank { normalizedActiveId }.uppercase(Locale.US),
              color = Color.White,
              fontSize = if (isTabletDeck) 9.8.sp else 8.8.sp,
              fontWeight = FontWeight.Black,
              fontFamily = FontFamily.Monospace,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.weight(1f).padding(end = 4.dp),
            )
            Box(
              modifier =
                Modifier.minimumInteractiveComponentSize()
                  .height(48.dp)
                  .clickable {
                    renameDraft = draftSlot.customName.ifBlank { normalizedActiveId }
                    isEditingName = true
                  }
                  .testTag("rift_frame_rename_btn"),
              contentAlignment = Alignment.Center,
            ) {
              Box(
                modifier =
                  Modifier.clip(RoundedCornerShape(5.dp))
                    .background(Color(0xFF091528))
                    .border(1.dp, Color(0xFF38BDF8), RoundedCornerShape(5.dp))
                    .padding(horizontal = 7.dp, vertical = 5.dp),
              ) {
                Text(
                  text = "RENAME",
                  color = Color(0xFF38BDF8),
                  fontSize = 7.8.sp,
                  fontWeight = FontWeight.Black,
                  fontFamily = FontFamily.Monospace,
                )
              }
            }
            Spacer(modifier = Modifier.width(4.dp))
            Box(
              modifier =
                Modifier.minimumInteractiveComponentSize()
                  .height(48.dp)
                  .clickable {
                    commitDraftClamped(draftSlot.resetToZeroedFrame(resetVisuals = false))
                  }
                  .testTag("rift_frame_reset_points_btn"),
              contentAlignment = Alignment.Center,
            ) {
              Box(
                modifier =
                  Modifier.clip(RoundedCornerShape(5.dp))
                    .background(Color(0xFF1F0C18))
                    .border(1.dp, Color(0xFFFB7185), RoundedCornerShape(5.dp))
                    .padding(horizontal = 7.dp, vertical = 5.dp),
              ) {
                Text(
                  text = "RESET",
                  color = Color(0xFFFB7185),
                  fontSize = 7.8.sp,
                  fontWeight = FontWeight.Black,
                  fontFamily = FontFamily.Monospace,
                )
              }
            }
          }
        }
      }

      // 5 Hull Stat Point Steppers (strictly <= 100 pts)
      HelmStepperControlRow(
        label = "TOP SPEED",
        valueText = "${draftSlot.speedPts} PT",
        onMinus = { commitDraftClamped(draftSlot.copy(speedPts = (draftSlot.speedPts - 1).coerceAtLeast(0))) },
        onPlus = {
          if (remainingPoints > 0 && draftSlot.speedPts < 30) {
            val step = minOf(2, remainingPoints, 30 - draftSlot.speedPts)
            commitDraftClamped(draftSlot.copy(speedPts = draftSlot.speedPts + step))
          }
        },
      )
      HelmStepperControlRow(
        label = "THRUST",
        valueText = "${draftSlot.thrustPts} PT",
        onMinus = { commitDraftClamped(draftSlot.copy(thrustPts = (draftSlot.thrustPts - 1).coerceAtLeast(0))) },
        onPlus = {
          if (remainingPoints > 0 && draftSlot.thrustPts < 30) {
            val step = minOf(2, remainingPoints, 30 - draftSlot.thrustPts)
            commitDraftClamped(draftSlot.copy(thrustPts = draftSlot.thrustPts + step))
          }
        },
      )
      HelmStepperControlRow(
        label = "TURN RATE",
        valueText = "${draftSlot.turnPts} PT",
        onMinus = { commitDraftClamped(draftSlot.copy(turnPts = (draftSlot.turnPts - 1).coerceAtLeast(0))) },
        onPlus = {
          if (remainingPoints > 0 && draftSlot.turnPts < 30) {
            val step = minOf(2, remainingPoints, 30 - draftSlot.turnPts)
            commitDraftClamped(draftSlot.copy(turnPts = draftSlot.turnPts + step))
          }
        },
      )
      HelmStepperControlRow(
        label = "MAX FLUX",
        valueText = "${draftSlot.maxFluxPts} PT",
        onMinus = { commitDraftClamped(draftSlot.copy(maxFluxPts = (draftSlot.maxFluxPts - 1).coerceAtLeast(0))) },
        onPlus = {
          if (remainingPoints > 0 && draftSlot.maxFluxPts < 30) {
            val step = minOf(2, remainingPoints, 30 - draftSlot.maxFluxPts)
            commitDraftClamped(draftSlot.copy(maxFluxPts = draftSlot.maxFluxPts + step))
          }
        },
      )
      HelmStepperControlRow(
        label = "FLUX REGEN",
        valueText = "${draftSlot.rechargePts} PT",
        onMinus = { commitDraftClamped(draftSlot.copy(rechargePts = (draftSlot.rechargePts - 1).coerceAtLeast(0))) },
        onPlus = {
          if (remainingPoints > 0 && draftSlot.rechargePts < 30) {
            val step = minOf(2, remainingPoints, 30 - draftSlot.rechargePts)
            commitDraftClamped(draftSlot.copy(rechargePts = draftSlot.rechargePts + step))
          }
        },
      )

      // Primary vs Secondary Weapon Selector Deck (auto-clamps hull stats when switching to costlier weapons)
      Row(
        modifier = Modifier.fillMaxWidth().height(48.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        listOf("PRI" to "PRIMARY (${draftSlot.primaryPointCost()}P)", "SEC" to "SECONDARY (${draftSlot.secondaryPointCost()}P)").forEach { (key, label) ->
          val active = weaponTab == key
          Box(
            modifier =
              Modifier.weight(1f)
                .fillMaxHeight()
                .minimumInteractiveComponentSize()
                .clickable { weaponTab = key }
                .padding(vertical = 4.dp)
                .testTag("rift_frame_weapon_tab_${key.lowercase(Locale.US)}"),
            contentAlignment = Alignment.Center,
          ) {
            Box(
              modifier =
                Modifier.fillMaxSize()
                  .clip(RoundedCornerShape(6.dp))
                  .background(if (active) Color(0x3800F5D4) else Color(0xFF060D1A))
                  .border(1.dp, if (active) Color(0xFF00F5D4) else Color(0x4438BDF8), RoundedCornerShape(6.dp)),
              contentAlignment = Alignment.Center,
            ) {
              Text(
                text = label,
                color = if (active) Color.White else Color(0xFF94A3B8),
                fontSize = 8.0.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }
        }
      }

      if (weaponTab == "PRI") {
        primaryOptions.forEach { pri: PrimaryWeaponOption ->
          val selected = draftSlot.primaryArchetype == pri.id
          Row(
            modifier =
              Modifier.fillMaxWidth()
                .minimumInteractiveComponentSize()
                .chamferedSciFiPodSurface(
                  isRightSide = false,
                  primaryBorder = if (selected) Color(0xFF00F5D4) else Color(0xFF38BDF8),
                  secondaryBorder = if (selected) Color(0xFFFFD700) else Color(0xFF091528),
                )
                .clickable {
                  commitDraftClamped(draftSlot.copy(primaryArchetype = pri.id))
                  onPreviewWeaponStage(
                    OverwatchCircularPrompt.InfoStageInspect(
                      kicker = "PRIMARY WEAPON EQUIPPED",
                      title = pri.title.uppercase(Locale.US),
                      badge = "${pri.pointCost} POINTS",
                      summary = pri.summary,
                      tacticalNote = "Automatically balanced within your 100-Point Rift Frame parity budget.",
                      accentHex = 0xFF00F5D4L,
                      shipId = 10,
                    )
                  )
                }
                .padding(horizontal = 8.dp, vertical = 6.dp)
                .testTag("rift_frame_pri_${pri.id.lowercase(Locale.US)}"),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.weight(1f).padding(end = 6.dp)) {
              Text(
                text = pri.title.uppercase(Locale.US),
                color = if (selected) Color(0xFF00F5D4) else Color.White,
                fontSize = if (isTabletDeck) 8.8.sp else 8.0.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              Text(
                text = pri.summary,
                color = Color(0xFF94A3B8),
                fontSize = 7.4.sp,
                fontFamily = FontFamily.SansSerif,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
            Text(
              text = if (selected) "${pri.pointCost}P ON" else "${pri.pointCost}P",
              color = if (selected) Color(0xFF00F5D4) else Color(0xFFFACC15),
              fontSize = 8.2.sp,
              fontWeight = FontWeight.Black,
              fontFamily = FontFamily.Monospace,
            )
          }
        }
      } else {
        secondaryOptions.forEach { sec: SecondaryAbilityOption ->
          val selected = draftSlot.secondaryType == sec.id
          Row(
            modifier =
              Modifier.fillMaxWidth()
                .minimumInteractiveComponentSize()
                .chamferedSciFiPodSurface(
                  isRightSide = false,
                  primaryBorder = if (selected) Color(0xFFC084FC) else Color(0xFF38BDF8),
                  secondaryBorder = if (selected) Color(0xFF00F5D4) else Color(0xFF091528),
                )
                .clickable {
                  commitDraftClamped(draftSlot.copy(secondaryType = sec.id))
                  onPreviewWeaponStage(
                    OverwatchCircularPrompt.InfoStageInspect(
                      kicker = "SECONDARY ABILITY EQUIPPED",
                      title = sec.title.uppercase(Locale.US),
                      badge = "${sec.pointCost} POINTS",
                      summary = sec.summary,
                      tacticalNote = "${sec.fluxCostLabel} • Auto-balanced to 100-Point budget.",
                      accentHex = 0xFFC084FCL,
                      shipId = 10,
                    )
                  )
                }
                .padding(horizontal = 8.dp, vertical = 6.dp)
                .testTag("rift_frame_sec_${sec.id.lowercase(Locale.US)}"),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.weight(1f).padding(end = 6.dp)) {
              Text(
                text = sec.title.uppercase(Locale.US),
                color = if (selected) Color(0xFFC084FC) else Color.White,
                fontSize = if (isTabletDeck) 8.8.sp else 8.0.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              Text(
                text = "${sec.fluxCostLabel} • ${sec.summary}",
                color = Color(0xFF94A3B8),
                fontSize = 7.4.sp,
                fontFamily = FontFamily.SansSerif,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
            Text(
              text = if (selected) "${sec.pointCost}P ON" else "${sec.pointCost}P",
              color = if (selected) Color(0xFFC084FC) else Color(0xFFFACC15),
              fontSize = 8.2.sp,
              fontWeight = FontWeight.Black,
              fontFamily = FontFamily.Monospace,
            )
          }
        }
      }

      // Charge Rift Frame Button
      val chargeFontSp = if (isTabletDeck) 9.4.sp else 8.4.sp
      val fluxDp = with(density) { chargeFontSp.toDp() }
      Box(
        modifier =
          Modifier.fillMaxWidth()
            .minimumInteractiveComponentSize()
            .height(48.dp)
            .clickable {
              commitDraftClamped(draftSlot)
              onChargeRiftFrame()
            }
            .padding(vertical = 3.dp)
            .testTag("rift_frame_charge_btn"),
        contentAlignment = Alignment.Center,
      ) {
        Row(
          modifier =
            Modifier.fillMaxWidth()
              .clip(RoundedCornerShape(8.dp))
              .background(Brush.horizontalGradient(listOf(Color(0xFF1A103D), Color(0xFF0D3349))))
              .border(
                1.2.dp,
                Brush.horizontalGradient(listOf(Color(0xFFC084FC), Color(0xFF00F5D4), Color(0xFFFFD700))),
                RoundedCornerShape(8.dp),
              )
              .padding(horizontal = 10.dp, vertical = 7.dp),
          horizontalArrangement = Arrangement.Center,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = if (isCharged) "RIFT FRAME CHARGED " else "CHARGE RIFT FRAME ",
            color = Color.White,
            fontSize = chargeFontSp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
          )
          FluxToroidIcon(sizeDp = fluxDp)
        }
      }
    }
  }
}

@Composable
internal fun RiftFrameCosmeticsSideBay(
  isTabletDeck: Boolean,
  headerHeight: Dp,
  wingStackGap: Dp,
  shardText: String,
  peakResonance: Int,
  shipSpinDeg: Float,
  fluxOrbitRad: Float,
  isCharged: Boolean,
  localPrefs: LocalPilotPrefs,
  currentUserId: String,
  onShareBuild: () -> Unit,
  onInspectCosmetic: (CosmeticCatalogItem, Boolean, Boolean) -> Unit,
) {
  val uidOrNull = currentUserId.ifBlank { null }
  var categoryFilter by remember { mutableStateOf("ALL") }
  val allCosmetics = remember {
    RiftProgressionCatalog.hullTrims +
      RiftProgressionCatalog.killFinishers +
      RiftProgressionCatalog.warpSignatures +
      RiftProgressionCatalog.hvtHalos
  }
  val filteredCosmetics =
    remember(categoryFilter, allCosmetics) {
      when (categoryFilter) {
        "TRIM" -> RiftProgressionCatalog.hullTrims
        "FINISH" -> RiftProgressionCatalog.killFinishers
        "WARP" -> RiftProgressionCatalog.warpSignatures
        "HALOS" -> RiftProgressionCatalog.hvtHalos
        else -> allCosmetics
      }
    }

  Column(
    modifier = Modifier.fillMaxSize().testTag("rift_frame_right_cosmetics_bay"),
    verticalArrangement = Arrangement.spacedBy(wingStackGap),
  ) {
    HubDigitizedReturnBar(
      heightDp = headerHeight,
      isRightSide = true,
      isTabletDeck = isTabletDeck,
      sectionTitle = "COSMETICS",
      accent = Color(0xFFFFD700),
      trailingLabel = "SHARE >",
      trailingTag = "rift_frame_share_build",
      onTrailingClick = onShareBuild,
    )

    // Live 2.5D Rift Frame Preview + Peak Resonance & Shards Readout
    Row(
      modifier =
        Modifier.fillMaxWidth()
          .chamferedSciFiPodSurface(
            isRightSide = true,
            primaryBorder = Color(0xFFC084FC),
            secondaryBorder = Color(0xFF00F5D4),
          )
          .padding(horizontal = 8.dp, vertical = 5.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Spinning25dShipWithOrbitingFlux(
        shipId = 10,
        shipSpinDeg = shipSpinDeg,
        fluxOrbitRad = fluxOrbitRad,
        isCharged = isCharged,
        isBestShip = false,
        isSelected = true,
        sizeDp = if (isTabletDeck) 50.dp else 40.dp,
      )
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
          text = "RESONANCE COSMETICS",
          color = Color.White,
          fontSize = if (isTabletDeck) 8.8.sp else 7.8.sp,
          fontWeight = FontWeight.Black,
          fontFamily = FontFamily.Monospace,
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
          ResonanceCrestIcon(sizeDp = 10.dp)
          Text(
            text = "PEAK ${formatOverwatchResonance(peakResonance)}",
            color = Color(0xFFFFD700),
            fontSize = 7.6.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
          )
          RiftShardGemIcon(sizeDp = 10.dp)
          Text(
            text = shardText,
            color = Color(0xFF00F5D4),
            fontSize = 7.6.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
          )
        }
      }
    }

    // All 4 Untruncated Category Tabs (+ ALL)
    Row(
      modifier =
        Modifier.fillMaxWidth()
          .height(48.dp)
          .chamferedSciFiPodSurface(
            isRightSide = true,
            primaryBorder = Color(0xFF38BDF8),
            secondaryBorder = Color(0xFF091528),
          )
          .horizontalScroll(rememberScrollState())
          .padding(horizontal = 4.dp),
      horizontalArrangement = Arrangement.spacedBy(3.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      listOf("ALL", "TRIM", "FINISH", "WARP", "HALOS").forEach { tab ->
        val sel = categoryFilter == tab
        Box(
          modifier =
            Modifier.minimumInteractiveComponentSize()
              .fillMaxHeight()
              .clickable { categoryFilter = tab }
              .padding(vertical = 6.dp)
              .testTag("rift_frame_cosmetic_tab_${tab.lowercase(Locale.US)}"),
          contentAlignment = Alignment.Center,
        ) {
          Box(
            modifier =
              Modifier.fillMaxHeight()
                .clip(RoundedCornerShape(5.dp))
                .background(if (sel) Color(0x38C084FC) else Color(0x6602050B))
                .border(1.dp, if (sel) Color(0xFFC084FC) else Color(0x4438BDF8), RoundedCornerShape(5.dp))
                .padding(horizontal = 7.dp),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text = tab,
              color = if (sel) Color.White else Color(0xFF94A3B8),
              fontSize = 7.6.sp,
              fontWeight = FontWeight.Black,
              fontFamily = FontFamily.Monospace,
              maxLines = 1,
              softWrap = false,
            )
          }
        }
      }
    }

    Column(
      modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(wingStackGap),
    ) {
      filteredCosmetics.forEach { item ->
        val unlockedByResonance = item.freeAtResonance > 0 && peakResonance >= item.freeAtResonance
        val unlocked = unlockedByResonance || localPrefs.isCosmeticUnlocked(item.category, item.id, uid = uidOrNull)
        val equipped = localPrefs.getEquippedCosmeticId(item.category, uidOrNull) == item.id
        val itemAccent = Color(item.accentColorHex)

        RiftVerticalCard(
          modifier = Modifier.fillMaxWidth(),
          title = item.name.uppercase(Locale.US),
          subtitle = item.summary,
          accent = if (equipped) Color(0xFF00F5D4) else itemAccent,
          secondaryAccent = if (unlocked) Color(0xFFFACC15) else Color(0xFF38BDF8),
          isRightColumn = true,
          isHighlighted = equipped,
          badgeText =
            when {
              equipped -> "ACTIVE"
              unlocked -> "OWNED"
              item.freeAtResonance > 0 -> "PEAK ${item.freeAtResonance}"
              else -> null
            },
          badgeColor = if (equipped) Color(0xFF00F5D4) else Color(0xFFFACC15),
          shardCost = if (!unlocked && item.shardCost > 0L) item.shardCost else null,
          statusLabel =
            when {
              equipped -> "EQUIPPED"
              unlocked -> "TAP TO EQUIP"
              else -> null
            },
          statusColor = if (equipped) Color(0xFF00F5D4) else Color(0xFFFACC15),
          isTabletDeck = isTabletDeck,
          minHeightDp = if (isTabletDeck) 114.dp else 98.dp,
          testTag = "rift_frame_cosmetic_${item.category.lowercase(Locale.US)}_${item.id}",
          visualContent = { visualDp ->
            CosmeticSwatchEmblem(item = item, sizeDp = visualDp)
          },
          onClick = { onInspectCosmetic(item, unlocked, equipped) },
        )
      }
    }
  }
}

@Composable
internal fun ConduitRankTrackSideBay(
  isTabletDeck: Boolean,
  headerHeight: Dp,
  wingStackGap: Dp,
  liveResonance: Int,
  peakResonance: Int,
  onInspectTierStage: (OverwatchCircularPrompt.InfoStageInspect) -> Unit,
) {
  val currentTier = remember(liveResonance) { RiftProgressionCatalog.getConduitTierForResonance(liveResonance) }

  Column(
    modifier = Modifier.fillMaxSize().testTag("conduit_rank_track_bay"),
    verticalArrangement = Arrangement.spacedBy(wingStackGap),
  ) {
    HubDigitizedReturnBar(
      heightDp = headerHeight,
      isRightSide = true,
      isTabletDeck = isTabletDeck,
      sectionTitle = "CONDUIT TRACK",
      accent = Color(0xFFFFD700),
    )

    Column(
      modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(wingStackGap),
    ) {
      RiftProgressionCatalog.CONDUIT_RANK_TIERS.forEach { tier ->
        val isCurrent = currentTier.title == tier.title
        val isReached = peakResonance >= tier.minResonance || liveResonance >= tier.minResonance
        val tierAccent = Color(tier.accentColorHex)

        RiftVerticalCard(
          modifier = Modifier.fillMaxWidth(),
          title = tier.title.uppercase(Locale.US),
          subtitle = "Start match with ${tier.startingBounty} Resonance",
          accent = if (isCurrent) Color(0xFF00F5D4) else tierAccent,
          secondaryAccent = if (isReached) Color(0xFFFACC15) else Color(0xFF38BDF8),
          isRightColumn = true,
          isHighlighted = isCurrent,
          badgeText =
            when {
              isCurrent -> "ACTIVE"
              isReached -> "UNLOCKED"
              else -> "${tier.minResonance}+ RES"
            },
          badgeColor = if (isCurrent) Color(0xFF00F5D4) else tierAccent,
          statusLabel = "MIN ${formatOverwatchResonance(tier.minResonance)} RESONANCE",
          statusColor = if (isReached) Color(0xFFFACC15) else Color(0xFF94A3B8),
          isTabletDeck = isTabletDeck,
          minHeightDp = if (isTabletDeck) 108.dp else 94.dp,
          testTag = "conduit_tier_${tier.title.lowercase(Locale.US).replace(" ", "_")}",
          visualContent = { visualDp ->
            Box(
              modifier =
                Modifier.size(visualDp)
                  .clip(CircleShape)
                  .background(tierAccent.copy(alpha = 0.14f))
                  .border(1.2.dp, tierAccent, CircleShape),
              contentAlignment = Alignment.Center,
            ) {
              ResonanceCrestIcon(sizeDp = visualDp * 0.65f)
            }
          },
          onClick = {
            onInspectTierStage(
              OverwatchCircularPrompt.InfoStageInspect(
                kicker = "CONDUIT RANK TIER",
                title = tier.title.uppercase(Locale.US),
                badge = if (isCurrent) "ACTIVE RANK" else if (isReached) "UNLOCKED" else "${tier.minResonance} RESONANCE",
                summary =
                  "Reaching ${formatOverwatchResonance(tier.minResonance)} Resonance upgrades your starting match Resonance baseline to ${tier.startingBounty}.",
                tacticalNote = "Pure competitive tier progression — zero paid combat advantages.",
                accentHex = tier.accentColorHex,
              )
            )
          },
        )
      }

      RiftProgressionCatalog.frameSlotGates.forEach { gate ->
        val unlocked = peakResonance >= gate.freeAtResonance
        RiftVerticalCard(
          modifier = Modifier.fillMaxWidth(),
          title = gate.slotId.uppercase(Locale.US),
          subtitle = "Free at ${formatOverwatchResonance(gate.freeAtResonance)} Peak Resonance",
          accent = if (unlocked) Color(0xFF00F5D4) else Color(0xFFC084FC),
          isRightColumn = true,
          isHighlighted = unlocked,
          badgeText = if (unlocked) "EARNED" else "MILESTONE",
          badgeColor = if (unlocked) Color(0xFF00F5D4) else Color(0xFFC084FC),
          statusLabel = gate.archetypeTitle.uppercase(Locale.US),
          statusColor = Color(0xFFCBD5E1),
          isTabletDeck = isTabletDeck,
          minHeightDp = if (isTabletDeck) 104.dp else 92.dp,
          testTag = "conduit_milestone_${gate.slotId.lowercase(Locale.US).replace(" ", "_")}",
          visualContent = { visualDp ->
            ResonanceCrestIcon(sizeDp = visualDp * 0.72f)
          },
          onClick = {
            onInspectTierStage(
              OverwatchCircularPrompt.InfoStageInspect(
                kicker = "CONDUIT FRAME MILESTONE",
                title = "${gate.slotId.uppercase(Locale.US)} • ${gate.archetypeTitle.uppercase(Locale.US)}",
                badge = if (unlocked) "UNLOCKED BY RESONANCE" else "${gate.freeAtResonance} PEAK RESONANCE",
                summary = gate.summary,
                tacticalNote = "Earn Peak Resonance in Sector Convergence or unlock instantly in the Shard Store.",
                accentHex = 0xFFC084FCL,
                shipId = 10,
              )
            )
          },
        )
      }
    }
  }
}

@Composable
internal fun ShardStoreLeftTreasuryBay(
  isTabletDeck: Boolean,
  headerHeight: Dp,
  wingStackGap: Dp,
  shardText: String,
  liveResonance: Int,
  peakResonance: Int,
  activeCustomSlotId: String,
  purchaseStatusMessage: String?,
  localPrefs: LocalPilotPrefs,
  currentUserId: String,
  onBackToHub: () -> Unit,
  onSelectFrameSlot: (String) -> Unit,
  onInspectFrameSlot: (FrameSlotGateInfo, String, Boolean, Boolean) -> Unit,
  onOpenRiftFrameBay: () -> Unit,
  onOpenConduitQuests: () -> Unit,
) {
  val uidOrNull = currentUserId.ifBlank { null }
  val normalizedActiveId = localPrefs.normalizeCustomSlotId(activeCustomSlotId)
  val isTesterMode = RiftBillingManager.TESTER_INFINITE_SHARDS_ACTIVE

  Column(
    modifier = Modifier.fillMaxSize().testTag("shard_store_left_summary_bay"),
    verticalArrangement = Arrangement.spacedBy(wingStackGap),
  ) {
    HubDigitizedReturnBar(
      heightDp = headerHeight,
      isRightSide = false,
      isTabletDeck = isTabletDeck,
      sectionTitle = "SHARD STORE",
      accent = Color(0xFF00F5D4),
      trailingLabel = "CONDUIT >",
      trailingTag = "shard_vault_open_quests",
      onTrailingClick = onOpenConduitQuests,
      onBackToHub = onBackToHub,
    )

    Column(
      modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(wingStackGap),
    ) {
      // Live Treasury & Billing Status Card
      Column(
        modifier =
          Modifier.fillMaxWidth()
            .chamferedSciFiPodSurface(
              isRightSide = false,
              primaryBorder = Color(0xFF00F5D4),
              secondaryBorder = Color(0xFFFFD700),
            )
            .padding(horizontal = 9.dp, vertical = 7.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          RiftShardGemIcon(sizeDp = 16.dp)
          Text(
            text = "$shardText RIFT SHARDS",
            color = Color(0xFF00F5D4),
            fontSize = if (isTabletDeck) 11.2.sp else 10.0.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
          )
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
          ResonanceCrestIcon(sizeDp = 12.dp)
          Text(
            text = "LIVE ${formatOverwatchResonance(liveResonance)} • PEAK ${formatOverwatchResonance(peakResonance)}",
            color = Color(0xFFFFD700),
            fontSize = 8.4.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
          )
        }
        Text(
          text =
            purchaseStatusMessage
              ?: if (isTesterMode) {
                "Tester Treasury active (999,999 non-depleting Shards). Google Play Billing by ${RiftBillingManager.STUDIO_LEGAL_NAME}."
              } else {
                "Google Play Billing active (${RiftBillingManager.STUDIO_LEGAL_NAME}). Tap any Shard Pack on the right."
              },
          color = if (purchaseStatusMessage != null) Color(0xFF00F5D4) else Color(0xFFCBD5E1),
          fontSize = 8.0.sp,
          fontFamily = FontFamily.SansSerif,
        )
      }

      // Frame B..E Unlock Cards
      RiftProgressionCatalog.frameSlotGates.forEach { gate ->
        val slotId = localPrefs.normalizeCustomSlotId(gate.slotId)
        val unlocked = localPrefs.isCustomSlotUnlocked(slotId, uidOrNull) || peakResonance >= gate.freeAtResonance
        val selected = normalizedActiveId == slotId

        RiftVerticalCard(
          modifier = Modifier.fillMaxWidth(),
          title = "${gate.slotId.uppercase(Locale.US)} • ${gate.archetypeTitle.uppercase(Locale.US)}",
          subtitle =
            if (gate.freeAtResonance <= 0) "Open immediately at 0 Resonance"
            else "Free at ${formatOverwatchResonance(gate.freeAtResonance)} Peak Resonance",
          accent = if (selected) Color(0xFF00F5D4) else Color(0xFFC084FC),
          secondaryAccent = if (unlocked) Color(0xFF38BDF8) else Color(0xFFFACC15),
          isRightColumn = false,
          isHighlighted = selected,
          badgeText =
            when {
              selected -> "ACTIVE"
              unlocked -> "OWNED"
              else -> "UNLOCK"
            },
          badgeColor = if (selected) Color(0xFF00F5D4) else Color(0xFFFACC15),
          shardCost = if (!unlocked && gate.shardCost > 0L) gate.shardCost else null,
          statusLabel =
            when {
              selected -> "ACTIVE BAY"
              unlocked -> "TAP TO SELECT"
              else -> null
            },
          statusColor = if (selected) Color(0xFF00F5D4) else Color(0xFFC084FC),
          isTabletDeck = isTabletDeck,
          minHeightDp = if (isTabletDeck) 98.dp else 88.dp,
          testTag = "shard_vault_slot_${slotId.takeLast(1)}",
          visualContent = { visualDp ->
            PrismaticShardClusterEmblem(
              sizeDp = visualDp,
              accentColor = if (selected) Color(0xFF00F5D4) else Color(0xFFC084FC),
              crystalCount = if (unlocked) 3 else 2,
            )
          },
          onClick = { onInspectFrameSlot(gate, slotId, unlocked, selected) },
        )
      }

      Box(
        modifier =
          Modifier.fillMaxWidth()
            .minimumInteractiveComponentSize()
            .height(48.dp)
            .clickable { onOpenRiftFrameBay() }
            .padding(vertical = 3.dp)
            .testTag("shard_vault_open_rift_frame"),
        contentAlignment = Alignment.Center,
      ) {
        Box(
          modifier =
            Modifier.fillMaxWidth()
              .clip(RoundedCornerShape(7.dp))
              .background(Color(0xFF1A1038))
              .border(1.dp, Color(0xFFC084FC), RoundedCornerShape(7.dp))
              .padding(vertical = 7.dp),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = "CONFIGURE RIFT FRAME >",
            color = Color(0xFFE9D5FF),
            fontSize = 8.2.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
          )
        }
      }
    }
  }
}

@Composable
internal fun ShardStoreRightPacksBay(
  isTabletDeck: Boolean,
  headerHeight: Dp,
  wingStackGap: Dp,
  currentUserId: String,
  callsign: String,
  onInspectSkuPack: (ShardVaultSkuPack, () -> Unit) -> Unit,
  onPurchaseResultBanner: (String, Long) -> Unit,
) {
  val context = LocalContext.current
  val activity = context as? Activity
  val coroutineScope = rememberCoroutineScope()
  var purchaseBusyProductId by remember { mutableStateOf<String?>(null) }

  Column(
    modifier = Modifier.fillMaxSize().testTag("shard_store_right_packs_bay"),
    verticalArrangement = Arrangement.spacedBy(wingStackGap),
  ) {
    HubDigitizedReturnBar(
      heightDp = headerHeight,
      isRightSide = true,
      isTabletDeck = isTabletDeck,
      sectionTitle = "SHARD PACKS",
      accent = Color(0xFFFACC15),
    )

    Column(
      modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(wingStackGap),
    ) {
      RiftBillingManager.OFFICIAL_SKU_CATALOG.forEachIndexed { idx, sku ->
        val packAccent =
          when (sku.productId) {
            "shards_250" -> Color(0xFF38BDF8)
            "shards_700" -> Color(0xFF00F5D4)
            "shards_2000" -> Color(0xFFA855F7)
            else -> Color(0xFFFACC15)
          }
        val subtitle =
          if (sku.bonusShards > 0L) {
            "${formatOverwatchResonance(sku.shardAmount.toInt())} + ${sku.bonusShards} bonus shards"
          } else {
            "${formatOverwatchResonance(sku.shardAmount.toInt())} Rift Shards pack"
          }
        val isBusy = purchaseBusyProductId == sku.productId

        RiftVerticalCard(
          modifier = Modifier.fillMaxWidth(),
          title = sku.title.uppercase(Locale.US),
          subtitle = subtitle,
          accent = packAccent,
          secondaryAccent = Color(0xFF00F5D4),
          isRightColumn = true,
          isHighlighted = idx >= 2,
          badgeText = sku.badgeLabel,
          badgeColor = packAccent,
          statusLabel = if (isBusy) "PROCESSING..." else "${sku.displayPriceUsd} • BUY PACK",
          statusColor = if (isBusy) Color(0xFF00F5D4) else Color.White,
          isTabletDeck = isTabletDeck,
          minHeightDp = if (isTabletDeck) 118.dp else 104.dp,
          testTag = "shard_store_pack_${sku.productId}",
          visualContent = { visualDp ->
            PrismaticShardClusterEmblem(
              sizeDp = visualDp,
              accentColor = packAccent,
              crystalCount = (idx + 1).coerceAtMost(3),
            )
          },
          onClick = {
            if (purchaseBusyProductId == null) {
              onInspectSkuPack(sku) {
                purchaseBusyProductId = sku.productId
                coroutineScope.launch {
                  try {
                    when (
                      val res =
                        RiftBillingManager.launchShardPackPurchaseFlow(
                          activity = activity,
                          sku = sku,
                          currentUid = currentUserId,
                          callsign = callsign,
                        )
                    ) {
                      is ShardPurchaseResult.VerifiedAndCredited -> {
                        onPurchaseResultBanner(
                          "CREDITED +${formatOverwatchResonance(res.shardsCredited.toInt())} RIFT SHARDS (${sku.title})",
                          res.shardsCredited,
                        )
                      }
                      is ShardPurchaseResult.TesterModeDormantNotice -> {
                        onPurchaseResultBanner(res.message, 0L)
                      }
                      is ShardPurchaseResult.BillingUnavailable -> {
                        onPurchaseResultBanner(res.reason, 0L)
                      }
                    }
                  } finally {
                    purchaseBusyProductId = null
                  }
                }
              }
            }
          },
        )
      }
    }
  }
}
