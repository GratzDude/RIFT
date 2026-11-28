package com.example.ui

import android.app.Activity
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
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
import com.example.engine.RiftVisualForgeState
import com.example.engine.SecondaryAbilityOption
import com.example.engine.ShipSpec
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlinx.coroutines.launch

internal fun isFrameBPurchasedInPrefs(localPrefs: LocalPilotPrefs, uidOrNull: String?): Boolean =
  localPrefs.isCosmeticUnlocked("FRAME_SLOT_PURCHASE", 2, 1, uidOrNull)

internal fun isPaidHullSkinOwnedInPrefs(
  itemId: Int,
  localPrefs: LocalPilotPrefs,
  uidOrNull: String?,
): Boolean = itemId <= 0 || localPrefs.isCosmeticUnlocked("SHIP_SKIN_PURCHASE", itemId, 1, uidOrNull)

/**
 * 3D Cockpit Pure-Light Holographic Projection used across Rift Hub, Hangar, Rift Frame,
 * Conduit Rank Track, Ranked Modes, and Shard Store:
 * - Zero dark rectangular slabs, zero vertical equator walls, zero closed borders, zero boxed corner badges
 * - Edge-free elliptical radial contrast feather behind floating art and typography
 * - Open 4-corner targeting reticle ticks + tapered center-outward laser underline when highlighted
 * - Dynamic spotlight scaling (hero Hub enlarged 66..74dp; spotlight 62dp; compact peer 38dp row)
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
  showBadgeResonanceIcon: Boolean = false,
  shardCost: Long? = null,
  statusLabel: String? = null,
  statusColor: Color = accent,
  @Suppress("UNUSED_PARAMETER") isTabletDeck: Boolean = false,
  minHeightDp: Dp = 96.dp,
  titleMaxLines: Int = 1,
  isCompactPeer: Boolean = false,
  heroHubMode: Boolean = false,
  testTag: String = "",
  visualContent: @Composable (Dp) -> Unit,
  onClick: () -> Unit,
) {
  val titleGlow =
    remember(accent, isHighlighted, heroHubMode) {
      Shadow(
        color = if (isHighlighted || heroHubMode) accent.copy(alpha = 0.65f) else Color(0xE6020611),
        offset = Offset(0f, 1f),
        blurRadius = if (isHighlighted || heroHubMode) 8f else 5f,
      )
    }
  val bodyShadow =
    remember {
      Shadow(color = Color(0xE6020611), offset = Offset(0f, 1f), blurRadius = 5f)
    }
  val targetScale =
    when {
      heroHubMode && isHighlighted -> 1.08f
      heroHubMode -> 1.0f
      isHighlighted && !isCompactPeer -> 1.08f
      isCompactPeer -> 0.92f
      else -> 1.0f
    }
  val spotlightScale by
    animateFloatAsState(
      targetValue = targetScale,
      animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
      label = "spotlight_card_scale",
    )

  BoxWithConstraints(
    modifier =
      modifier
        .minimumInteractiveComponentSize()
        .heightIn(min = minHeightDp.coerceAtLeast(48.dp))
        .graphicsLayer {
          scaleX = spotlightScale
          scaleY = spotlightScale
          clip = false
        }
        .drawBehind {
          val w = size.width
          val h = size.height
          if (w <= 8f || h <= 8f) return@drawBehind

          val cx = w * 0.5f
          val cy = h * 0.5f
          val featherR = maxOf(w * 0.60f, h * 0.74f)

          // 1. Edge-free radial contrast feather (zero dark rectangular slab or vertical rail)
          drawCircle(
            brush =
              Brush.radialGradient(
                colorStops =
                  arrayOf(
                    0.00f to Color(if (isHighlighted || heroHubMode) 0x94020611 else 0x6E020611),
                    0.56f to Color(if (isHighlighted || heroHubMode) 0x3C040C1D else 0x26040C1D),
                    1.00f to Color.Transparent,
                  ),
                center = Offset(cx, cy),
                radius = featherR,
              ),
            radius = featherR,
            center = Offset(cx, cy),
          )

          // 2. Luminous radial aura behind hero / spotlight visuals
          if (isHighlighted || heroHubMode) {
            val auraCenter = if (isCompactPeer) Offset(w * 0.22f, cy) else Offset(cx, h * 0.36f)
            val auraRadius = (min(w, h) * if (heroHubMode) 0.62f else 0.54f).coerceAtLeast(22f)
            drawCircle(
              brush =
                Brush.radialGradient(
                  colors =
                    listOf(
                      accent.copy(alpha = if (isHighlighted) 0.28f else 0.14f),
                      secondaryAccent.copy(alpha = if (isHighlighted) 0.11f else 0.05f),
                      Color.Transparent,
                    ),
                  center = auraCenter,
                  radius = auraRadius,
                ),
              radius = auraRadius,
              center = auraCenter,
            )
          }

          // 3. Open 4-corner targeting reticle ticks + tapered laser underline when highlighted
          if (isHighlighted) {
            val tick = minOf(6.dp.toPx(), w * 0.12f, h * 0.24f)
            val sw = 1.2.dp.toPx()
            val c = accent.copy(alpha = 0.92f)
            drawLine(c, Offset(2f, tick), Offset(2f, 2f), sw, StrokeCap.Round)
            drawLine(c, Offset(2f, 2f), Offset(tick, 2f), sw, StrokeCap.Round)
            drawLine(c, Offset(w - tick, 2f), Offset(w - 2f, 2f), sw, StrokeCap.Round)
            drawLine(c, Offset(w - 2f, 2f), Offset(w - 2f, tick), sw, StrokeCap.Round)
            drawLine(c, Offset(2f, h - tick), Offset(2f, h - 2f), sw, StrokeCap.Round)
            drawLine(c, Offset(2f, h - 2f), Offset(tick, h - 2f), sw, StrokeCap.Round)
            drawLine(c, Offset(w - tick, h - 2f), Offset(w - 2f, h - 2f), sw, StrokeCap.Round)
            drawLine(c, Offset(w - 2f, h - tick), Offset(w - 2f, h - 2f), sw, StrokeCap.Round)

            val yLine = (h - 1.2.dp.toPx()).coerceAtLeast(1f)
            drawLine(
              brush =
                Brush.horizontalGradient(
                  colorStops =
                    arrayOf(
                      0.00f to Color.Transparent,
                      0.18f to accent.copy(alpha = 0.85f),
                      0.50f to Color.White.copy(alpha = 0.90f),
                      0.82f to secondaryAccent.copy(alpha = 0.80f),
                      1.00f to Color.Transparent,
                    ),
                ),
              start = Offset(w * 0.06f, yLine),
              end = Offset(w * 0.94f, yLine),
              strokeWidth = 1.5.dp.toPx(),
              cap = StrokeCap.Round,
            )
          } else {
            val yLine = (h - 0.8.dp.toPx()).coerceAtLeast(1f)
            drawLine(
              brush =
                Brush.horizontalGradient(
                  colorStops =
                    if (isRightColumn) {
                      arrayOf(
                        0.00f to Color.Transparent,
                        0.30f to accent.copy(alpha = if (isCompactPeer) 0.22f else 0.34f),
                        0.82f to secondaryAccent.copy(alpha = if (isCompactPeer) 0.14f else 0.24f),
                        1.00f to Color.Transparent,
                      )
                    } else {
                      arrayOf(
                        0.00f to Color.Transparent,
                        0.18f to secondaryAccent.copy(alpha = if (isCompactPeer) 0.14f else 0.24f),
                        0.70f to accent.copy(alpha = if (isCompactPeer) 0.22f else 0.34f),
                        1.00f to Color.Transparent,
                      )
                    },
                ),
              start = Offset(w * 0.08f, yLine),
              end = Offset(w * 0.92f, yLine),
              strokeWidth = 0.85.dp.toPx(),
              cap = StrokeCap.Round,
            )
          }
        }
        .clickable { onClick() }
        .padding(
          horizontal = if (isCompactPeer) 6.dp else 8.dp,
          vertical = if (isCompactPeer) 2.dp else 5.dp,
        )
        .let { base -> if (testTag.isNotBlank()) base.testTag(testTag) else base },
  ) {
    if (isCompactPeer) {
      val compactVisualDp = 38.dp
      Row(
        modifier = Modifier.fillMaxWidth().align(Alignment.Center),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        if (!isRightColumn) {
          visualContent(compactVisualDp)
        }
        Column(
          modifier = Modifier.weight(1f),
          horizontalAlignment = if (isRightColumn) Alignment.End else Alignment.Start,
          verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
          Text(
            text = title,
            color = Color(0xFFE2E8F0),
            fontSize = 10.4.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
            textAlign = if (isRightColumn) TextAlign.End else TextAlign.Start,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = TextStyle(shadow = bodyShadow, platformStyle = PlatformTextStyle(includeFontPadding = false)),
          )
          if (!badgeText.isNullOrBlank()) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
              if (showBadgeResonanceIcon) {
                ResonanceCrestIcon(sizeDp = 9.5.dp)
              }
              Text(
                text = badgeText,
                color = badgeColor,
                fontSize = 8.6.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(shadow = bodyShadow, platformStyle = PlatformTextStyle(includeFontPadding = false)),
              )
            }
          } else if (shardCost != null && shardCost > 0L) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
              RiftShardGemIcon(sizeDp = 10.dp)
              Text(
                text = formatOverwatchResonance(shardCost.toInt()),
                color = Color(0xFF38BDF8),
                fontSize = 8.8.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                style = TextStyle(shadow = bodyShadow, platformStyle = PlatformTextStyle(includeFontPadding = false)),
              )
            }
          } else if (!statusLabel.isNullOrBlank()) {
            Text(
              text = statusLabel,
              color = statusColor,
              fontSize = 8.4.sp,
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.Monospace,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              style = TextStyle(shadow = bodyShadow, platformStyle = PlatformTextStyle(includeFontPadding = false)),
            )
          } else {
            Text(
              text = subtitle,
              color = Color(0xFF94A3B8),
              fontSize = 8.4.sp,
              fontWeight = FontWeight.Medium,
              fontFamily = FontFamily.SansSerif,
              textAlign = if (isRightColumn) TextAlign.End else TextAlign.Start,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              style = TextStyle(shadow = bodyShadow, platformStyle = PlatformTextStyle(includeFontPadding = false)),
            )
          }
        }
        if (isRightColumn) {
          visualContent(compactVisualDp)
        }
      }
    } else {
      val visualDp =
        when {
          heroHubMode && maxHeight >= 156.dp -> 84.dp
          heroHubMode && maxHeight >= 126.dp -> 78.dp
          heroHubMode -> 74.dp
          isHighlighted -> 76.dp
          maxHeight >= 120.dp -> 54.dp
          else -> 46.dp
        }
      val titleFontSp =
        when {
          heroHubMode -> 14.6.sp
          isHighlighted -> 13.4.sp
          titleMaxLines > 1 && title.length > 14 -> 10.8.sp
          else -> 12.0.sp
        }
      val subtitleFontSp = if (heroHubMode) 10.4.sp else 9.6.sp

      Column(
        modifier = Modifier.fillMaxWidth().align(Alignment.Center),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(if (heroHubMode) 3.dp else 2.5.dp),
      ) {
        visualContent(visualDp)

        Text(
          text = title,
          color = Color(0xFFF8FAFC),
          fontSize = titleFontSp,
          fontWeight = FontWeight.Black,
          fontFamily = FontFamily.Monospace,
          textAlign = TextAlign.Center,
          lineHeight = if (titleMaxLines > 1) 12.2.sp else (titleFontSp.value + 1.5f).sp,
          maxLines = titleMaxLines,
          softWrap = titleMaxLines > 1,
          overflow = TextOverflow.Ellipsis,
          style =
            TextStyle(
              shadow = titleGlow,
              platformStyle = PlatformTextStyle(includeFontPadding = false),
            ),
        )

        Text(
          text = subtitle,
          color = Color(0xFFCBD5E1),
          fontSize = subtitleFontSp,
          fontWeight = FontWeight.SemiBold,
          fontFamily = FontFamily.SansSerif,
          textAlign = TextAlign.Center,
          lineHeight = 11.6.sp,
          maxLines = if (heroHubMode) 1 else 2,
          overflow = TextOverflow.Ellipsis,
          style = TextStyle(shadow = bodyShadow, platformStyle = PlatformTextStyle(includeFontPadding = false)),
        )

        // Borderless inline HUD telemetry (badge / shard cost / status — zero boxed corner tags)
        if (!badgeText.isNullOrBlank()) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            if (showBadgeResonanceIcon) {
              ResonanceCrestIcon(sizeDp = 10.5.dp)
            }
            Text(
              text = badgeText,
              color = badgeColor,
              fontSize = 9.4.sp,
              fontWeight = FontWeight.Black,
              fontFamily = FontFamily.Monospace,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              style = TextStyle(shadow = bodyShadow, platformStyle = PlatformTextStyle(includeFontPadding = false)),
            )
          }
        }

        if (shardCost != null && shardCost > 0L) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            RiftShardGemIcon(sizeDp = 12.dp)
            Text(
              text = formatOverwatchResonance(shardCost.toInt()),
              color = Color(0xFF38BDF8),
              fontSize = 10.0.sp,
              fontWeight = FontWeight.Black,
              fontFamily = FontFamily.Monospace,
              maxLines = 1,
              style = TextStyle(shadow = bodyShadow, platformStyle = PlatformTextStyle(includeFontPadding = false)),
            )
          }
        } else if (!statusLabel.isNullOrBlank()) {
          Text(
            text = statusLabel,
            color = statusColor,
            fontSize = 9.2.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = TextStyle(shadow = bodyShadow, platformStyle = PlatformTextStyle(includeFontPadding = false)),
          )
        }
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

internal fun cleanShipRoleSummary(shipId: Int): String =
  when (shipId) {
    1 -> "Fast rail phase fighter"
    2 -> "Multi-bounce bank specialist"
    3 -> "Rapid twin-stream skirmisher"
    4 -> "Heavy siege juggernaut"
    5 -> "Agile dual-fang dogfighter"
    6 -> "Stealth cloak EMP ambusher"
    7 -> "Proximity flak interceptor"
    8 -> "Stealth minefield controller"
    9 -> "Two-way stasis controller"
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

    // Dynamic Spotlight Enlargement: spotlight ship steps forward at 120dp (76dp spinning 2.5D hull)
    // while the 4 peer ships stay compact (50dp min height, 4dp gap) so all 5 ships per wing fit 100% on screen.
    val spotlightShipId =
      when {
        ships.any { it.id == inspectedShipId } -> inspectedShipId
        ships.any { it.id == chargedShipId } -> chargedShipId
        ships.any { it.id == bestShipId } -> bestShipId
        else -> ships.firstOrNull()?.id ?: 1
      }

    Column(
      modifier =
        Modifier.weight(1f)
          .fillMaxWidth()
          .padding(
            start = if (isRightSide) 18.dp else 0.dp,
            end = if (isRightSide) 0.dp else 18.dp,
          )
          .verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      ships.forEach { ship ->
        val isCharged = chargedShipId == ship.id
        val isBest = bestShipId in 1..10 && bestShipId == ship.id
        val isInspected = inspectedShipId == ship.id
        val isSpotlight = ship.id == spotlightShipId
        val cleanName =
          remember(ship.id, ship.name) {
            if (ship.id == 10) "RIFT FRAME"
            else ship.name.replace(Regex("^[0-9]+-"), "").trim().uppercase(Locale.US)
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
            else -> Color(0xFF00F5D4)
          }
        val statusSuffix =
          when {
            isCharged -> "CHARGED"
            isBest -> "BEST"
            else -> null
          }
        val badgeText =
          if (!isSpotlight) {
            if (statusSuffix != null) "${formatOverwatchResonance(shipRes)} • $statusSuffix"
            else formatOverwatchResonance(shipRes)
          } else {
            statusSuffix
          }

        RiftVerticalCard(
          modifier = Modifier.fillMaxWidth(),
          title = cleanName,
          subtitle = cleanShipRoleSummary(ship.id),
          accent = primaryBorder,
          secondaryAccent = secondaryBorder,
          isRightColumn = isRightSide,
          isHighlighted = isSpotlight,
          isCompactPeer = !isSpotlight,
          badgeText = badgeText,
          badgeColor = if (isCharged) Color(0xFF00F5D4) else Color(0xFFFACC15),
          showBadgeResonanceIcon = !isSpotlight,
          minHeightDp = if (isSpotlight) 120.dp else 50.dp,
          testTag = "hangar_bay_ship_${ship.id}",
          visualContent = { visualDp ->
            if (isSpotlight) {
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
                  isSelected = isInspected || isSpotlight,
                  sizeDp = visualDp,
                )
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                  ResonanceCrestIcon(sizeDp = 10.5.dp)
                  Text(
                    text = formatOverwatchResonance(shipRes),
                    color = Color(0xFFFACC15),
                    fontSize = 9.6.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
                  )
                }
              }
            } else {
              Spinning25dShipWithOrbitingFlux(
                shipId = ship.id,
                shipSpinDeg = shipSpinDeg,
                fluxOrbitRad = fluxOrbitRad,
                isCharged = isCharged,
                isBestShip = isBest,
                isSelected = isInspected,
                sizeDp = visualDp,
              )
            }
          },
          onClick = { onTapShip(ship.id) },
        )
      }
    }
  }
}

internal data class DraggedRiftFrameItem(
  val isPrimary: Boolean,
  val itemId: String,
  val label: String,
  val pointCost: Int,
  val accentColor: Color,
)

private fun concisePrimaryWeaponSubtitle(pri: PrimaryWeaponOption): String =
  when (pri.id.uppercase(Locale.US)) {
    "LIGHT_PULSE" -> "Fast balanced pulse repeater"
    "ION_NEEDLER", "NEEDLER" -> "Rapid twin-stream ion bolts"
    "TWIN_SCATTER" -> "Wide close-range twin spread"
    "ANGLED_PULSE" -> "One-bounce bank-shot twin pulse"
    "QUAD_PULSE" -> "Four-bolt converging flak volley"
    "HEAVY_PLASMA" -> "High-caliber twin siege bolts"
    "TRI_PRISM" -> "Three-stream prism splitter volley"
    "SNIPER_RAIL" -> "One-bounce sniper rail beam"
    "PARALLEL_AUTOCANNON" -> "Parallel dogfighter twin stream"
    else ->
      pri.summary
        .replace(Regex("\\([^)]*\\)?"), "")
        .trim()
        .split(Regex("\\s+"))
        .take(4)
        .joinToString(" ")
  }

private fun conciseSecondaryAbilitySubtitle(sec: SecondaryAbilityOption): String =
  when (sec.id.uppercase(Locale.US)) {
    "NONE" -> "Save points for hull stats"
    "NATIVE_MULTI" -> "Permanent four-bolt multi-toggle"
    "PHASE_DASH" -> "Forward warp burst and shield"
    "MINE" -> "Magnetic proximity mines plus Repel"
    "BOUNCE" -> "Three-bounce bank plasma torpedo"
    "PROX" -> "Eight-bolt proximity flak warhead"
    "CLUSTER" -> "Eight-bolt cluster shrapnel bomb"
    "NOVA_RING", "BURST" -> "Sixteen-bolt radial plasma ring"
    "SHATTER_CONE" -> "Seven-pellet kinetic blast cone"
    "SEEKER_TORPEDO" -> "Predictive homing heavy warhead"
    "CLOAK_EMP" -> "Stealth cloak with decloak EMP"
    "SIEGE" -> "Mid-air detonate heavy bomb"
    "INTERDICTOR_DOME" -> "Two-way six-second Stasis field"
    "CHAKRAM", "DISC" -> "Four-bounce ricochet saw-disc"
    "HOLO_PHANTOM" -> "Mirrored clone and brief cloak"
    "STASIS_BARRIER" -> "Deployable reflective shield wall"
    "HYPER_SURGE" -> "Supersonic thruster speed burst"
    "SWARM_DRONES" -> "Five hunter micro-drones"
    "TRACTOR_BEAM" -> "Toggleable gravimetric snare beam"
    "PROX_SEEKER" -> "Infinite-bounce patrol lock missile"
    "LANCE_BEAM" -> "Toggleable thermal cutting beam"
    "NATIVE_RICOCHET" -> "Adds one primary wall bounce"
    "REPEL_PULSE" -> "Flux-powered radial Repel pulse"
    else ->
      sec.summary
        .replace(Regex("\\([^)]*\\)?"), "")
        .replace("(", "")
        .replace(")", "")
        .trim()
        .split(Regex("\\s+"))
        .take(4)
        .joinToString(" ")
  }

@Composable
internal fun RiftFrameLoadoutSideBay(
  isTabletDeck: Boolean,
  headerHeight: Dp,
  wingStackGap: Dp,
  activeCustomSlot: CustomShipLoadoutSlot,
  activeCustomSlotId: String,
  liveResonance: Int,
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
  onStartDragItem: (DraggedRiftFrameItem, Offset) -> Unit = { _, _ -> },
  onUpdateDragRootOffset: (Offset) -> Unit = {},
  onEndDragItem: () -> Unit = {},
  onOpenFrameRenamePrompt: (() -> Unit)? = null,
) {
  val density = LocalDensity.current
  val keyboardController = LocalSoftwareKeyboardController.current
  val uidOrNull = currentUserId.ifBlank { null }
  val effectiveBudget = pointBudget.coerceAtLeast(100)
  val normalizedActiveId = localPrefs.normalizeCustomSlotId(activeCustomSlotId)
  val isFrameBPurchased = isFrameBPurchasedInPrefs(localPrefs, uidOrNull)

  LaunchedEffect(normalizedActiveId, liveResonance, isFrameBPurchased) {
    if (!RiftProgressionCatalog.isFrameSlotActive(normalizedActiveId, liveResonance, isFrameBPurchased)) {
      onSelectFrameSlot("Frame A")
    }
  }

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
      trailingLabel = "DRYDOCK",
      trailingTag = "rift_frame_preview_overwatch",
      onTrailingClick = onPreviewInOverwatch,
      onBackToHub = onBackToHub,
    )

    // Borderless Cockpit Reticle Frame A..E Selector (Frame B via Shard Store; Frame C..E strictly via Live Resonance)
    Row(
      modifier =
        Modifier.fillMaxWidth()
          .padding(end = 18.dp)
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
        val isActiveSlot =
          RiftProgressionCatalog.isFrameSlotActive(fullSlotId, liveResonance, isFrameBPurchased)
        val gate = RiftProgressionCatalog.getFrameSlotGate(fullSlotId)
        val slotAccent =
          when {
            isSelected -> Color(0xFF00F5D4)
            isActiveSlot -> Color(0xFF38BDF8)
            else -> Color(0xFFFACC15)
          }
        Box(
          modifier =
            Modifier.weight(1f)
              .fillMaxHeight()
              .minimumInteractiveComponentSize()
              .cockpitReticleSurface(
                accent = slotAccent,
                isSelected = isSelected,
                drawCornerTicks = isSelected,
                featherAlpha = if (isSelected) 0.48f else 0.16f,
              )
              .clickable {
                if (isActiveSlot || fullSlotId == "Frame B") {
                  onSelectFrameSlot(fullSlotId)
                } else {
                  onPreviewWeaponStage(
                    OverwatchCircularPrompt.InfoStageInspect(
                      kicker = "RANKED FRAME SLOT",
                      title = "${gate.slotId.uppercase(Locale.US)} • ${gate.archetypeTitle.uppercase(Locale.US)}",
                      badge = "${formatOverwatchResonance(gate.freeAtResonance)} LIVE RESONANCE",
                      summary = gate.summary,
                      tacticalNote =
                        "Maintained solely through active Live Resonance (${formatOverwatchResonance(liveResonance)} / ${formatOverwatchResonance(gate.freeAtResonance)}). Saved builds remain preserved whenever your rank fluctuates.",
                      accentHex = 0xFFC084FCL,
                      shipId = 10,
                      showResonanceCrest = true,
                    )
                  )
                }
              }
              .testTag("rift_frame_slot_$shortKey"),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = if (isActiveSlot) shortKey else "${shortKey}*",
            color =
              when {
                isSelected -> Color.White
                isActiveSlot -> Color(0xFFCBD5E1)
                else -> Color(0xFFFACC15)
              },
            fontSize = 10.2.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
          )
        }
      }
    }

    Column(
      modifier = Modifier.weight(1f).fillMaxWidth().padding(end = 18.dp).verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(wingStackGap),
    ) {
      // Exact 100-Point Budget Readout + Borderless Frame Rename & Reset Points Controls
      Column(
        modifier =
          Modifier.fillMaxWidth()
            .chamferedSciFiPodSurface(
              isRightSide = false,
              primaryBorder = if (remainingPoints == 0) Color(0xFF00F5D4) else Color(0xFFFFD700),
              secondaryBorder = Color(0xFF38BDF8),
            )
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        Text(
          text = "USED: $usedPoints / $effectiveBudget PTS • FREE: $remainingPoints PTS",
          color = if (remainingPoints == 0) Color(0xFF00F5D4) else Color(0xFFFACC15),
          fontSize = 9.0.sp,
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
                  fontSize = 10.2.sp,
                  fontWeight = FontWeight.Bold,
                  fontFamily = FontFamily.Monospace,
                ),
              cursorBrush = SolidColor(Color(0xFF00F5D4)),
              keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
              keyboardActions = KeyboardActions(onDone = { commitRename() }),
              modifier =
                Modifier.weight(1f)
                  .heightIn(min = 38.dp)
                  .cockpitLaserInputSurface(Color(0xFF00F5D4))
                  .padding(horizontal = 8.dp, vertical = 8.dp)
                  .testTag("rift_frame_rename_input"),
            )
            Box(
              modifier =
                Modifier.minimumInteractiveComponentSize()
                  .height(48.dp)
                  .cockpitReticleSurface(
                    accent = Color(0xFF00F5D4),
                    isSelected = true,
                    drawCornerTicks = false,
                    featherAlpha = 0.42f,
                  )
                  .clickable { commitRename() }
                  .padding(horizontal = 10.dp)
                  .testTag("rift_frame_rename_save"),
              contentAlignment = Alignment.Center,
            ) {
              Text(
                text = "SAVE",
                color = Color.White,
                fontSize = 9.2.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
              )
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
              fontSize = 10.8.sp,
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
                  .cockpitReticleSurface(
                    accent = Color(0xFF38BDF8),
                    isSelected = true,
                    drawCornerTicks = false,
                    featherAlpha = 0.30f,
                  )
                  .clickable {
                    if (onOpenFrameRenamePrompt != null) {
                      onOpenFrameRenamePrompt()
                    } else {
                      renameDraft = draftSlot.customName.ifBlank { normalizedActiveId }
                      isEditingName = true
                    }
                  }
                  .padding(horizontal = 8.dp)
                  .testTag("rift_frame_rename_btn"),
              contentAlignment = Alignment.Center,
            ) {
              Text(
                text = "RENAME",
                color = Color(0xFF38BDF8),
                fontSize = 8.8.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
              )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Box(
              modifier =
                Modifier.minimumInteractiveComponentSize()
                  .height(48.dp)
                  .cockpitReticleSurface(
                    accent = Color(0xFFFB7185),
                    isSelected = true,
                    drawCornerTicks = false,
                    featherAlpha = 0.30f,
                  )
                  .clickable {
                    commitDraftClamped(draftSlot.resetToZeroedFrame(resetVisuals = false))
                  }
                  .padding(horizontal = 8.dp)
                  .testTag("rift_frame_reset_points_btn"),
              contentAlignment = Alignment.Center,
            ) {
              Text(
                text = "RESET",
                color = Color(0xFFFB7185),
                fontSize = 8.8.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
              )
            }
          }
        }

        Text(
          text = "HOLD & DRAG CARDS TO OVERWATCH SOCKETS OR TAP TO EQUIP",
          color = Color(0xFF94A3B8),
          fontSize = 7.4.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.Monospace,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }

      // Borderless Primary vs Secondary / Buff Reticle Tabs
      Row(
        modifier = Modifier.fillMaxWidth().height(48.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        listOf(
          "PRI" to "PRIMARY (${draftSlot.primaryPointCost()}P)",
          "SEC" to "BUFF / SEC (${draftSlot.secondaryPointCost()}P)",
        ).forEach { (key, label) ->
          val active = weaponTab == key
          Box(
            modifier =
              Modifier.weight(1f)
                .fillMaxHeight()
                .minimumInteractiveComponentSize()
                .cockpitReticleSurface(
                  accent = if (key == "PRI") Color(0xFF00F5D4) else Color(0xFFC084FC),
                  isSelected = active,
                  drawCornerTicks = false,
                  featherAlpha = if (active) 0.44f else 0.14f,
                )
                .clickable { weaponTab = key }
                .padding(horizontal = 4.dp)
                .testTag("rift_frame_weapon_tab_${key.lowercase(Locale.US)}"),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text = label,
              color = if (active) Color.White else Color(0xFF7C93B0),
              fontSize = 8.8.sp,
              fontWeight = FontWeight.Black,
              fontFamily = FontFamily.Monospace,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
      }

      if (weaponTab == "PRI") {
        primaryOptions.forEach { pri: PrimaryWeaponOption ->
          val selected = draftSlot.primaryArchetype == pri.id
          var cardRootOrigin by remember { mutableStateOf(Offset.Zero) }
          var dragPointerRoot by remember { mutableStateOf(Offset.Zero) }
          val iconBmp =
            remember(pri.id, RiftSpriteBaker.spriteVisualRevision) {
              RiftSpriteBaker.getPrimaryWeaponIconBitmap(pri.id).asImageBitmap()
            }

          Box(
            modifier =
              Modifier.fillMaxWidth()
                .onGloballyPositioned { coords -> cardRootOrigin = coords.positionInRoot() }
                .pointerInput(pri.id) {
                  detectDragGesturesAfterLongPress(
                    onDragStart = { localOffset ->
                      val rootStart = cardRootOrigin + localOffset
                      dragPointerRoot = rootStart
                      onStartDragItem(
                        DraggedRiftFrameItem(
                          isPrimary = true,
                          itemId = pri.id,
                          label = pri.title.uppercase(Locale.US),
                          pointCost = pri.pointCost,
                          accentColor = Color(0xFF00F5D4),
                        ),
                        rootStart,
                      )
                    },
                    onDrag = { change, dragAmount ->
                      change.consume()
                      dragPointerRoot += dragAmount
                      onUpdateDragRootOffset(dragPointerRoot)
                    },
                    onDragEnd = { onEndDragItem() },
                    onDragCancel = { onEndDragItem() },
                  )
                }
          ) {
            RiftVerticalCard(
              modifier = Modifier.fillMaxWidth(),
              title = pri.title.uppercase(Locale.US),
              subtitle = concisePrimaryWeaponSubtitle(pri),
              accent = if (selected) Color(0xFF00F5D4) else Color(0xFF38BDF8),
              secondaryAccent = if (selected) Color(0xFFFFD700) else Color(0xFF00F5D4),
              isRightColumn = false,
              isHighlighted = selected,
              badgeText = if (selected) "${pri.pointCost} PTS ON" else "${pri.pointCost} PTS",
              badgeColor = if (selected) Color(0xFF00F5D4) else Color(0xFFFACC15),
              statusLabel = if (selected) "MOUNTED IN PRIMARY" else "DRAG OR TAP TO MOUNT",
              statusColor = if (selected) Color(0xFF00F5D4) else Color(0xFF94A3B8),
              isTabletDeck = isTabletDeck,
              minHeightDp = if (isTabletDeck) 104.dp else 94.dp,
              titleMaxLines = 2,
              testTag = "rift_frame_pri_${pri.id.lowercase(Locale.US)}",
              visualContent = { visualDp ->
                Image(
                  bitmap = iconBmp,
                  contentDescription = pri.title,
                  contentScale = ContentScale.Fit,
                  modifier = Modifier.size((visualDp * 0.94f).coerceAtLeast(32.dp)),
                )
              },
              onClick = {
                commitDraftClamped(draftSlot.copy(primaryArchetype = pri.id))
                onPreviewInOverwatch()
              },
            )
          }
        }
      } else {
        secondaryOptions.forEach { sec: SecondaryAbilityOption ->
          val selected = draftSlot.secondaryType == sec.id
          var cardRootOrigin by remember { mutableStateOf(Offset.Zero) }
          var dragPointerRoot by remember { mutableStateOf(Offset.Zero) }
          val iconBmp =
            remember(sec.id, RiftSpriteBaker.spriteVisualRevision, RiftSpriteBaker.customShip9Version) {
              RiftSpriteBaker.getSecondaryAbilityIconBitmap(sec.id).asImageBitmap()
            }

          Box(
            modifier =
              Modifier.fillMaxWidth()
                .onGloballyPositioned { coords -> cardRootOrigin = coords.positionInRoot() }
                .pointerInput(sec.id) {
                  detectDragGesturesAfterLongPress(
                    onDragStart = { localOffset ->
                      val rootStart = cardRootOrigin + localOffset
                      dragPointerRoot = rootStart
                      onStartDragItem(
                        DraggedRiftFrameItem(
                          isPrimary = false,
                          itemId = sec.id,
                          label = sec.title.uppercase(Locale.US),
                          pointCost = sec.pointCost,
                          accentColor = Color(0xFFC084FC),
                        ),
                        rootStart,
                      )
                    },
                    onDrag = { change, dragAmount ->
                      change.consume()
                      dragPointerRoot += dragAmount
                      onUpdateDragRootOffset(dragPointerRoot)
                    },
                    onDragEnd = { onEndDragItem() },
                    onDragCancel = { onEndDragItem() },
                  )
                }
          ) {
            RiftVerticalCard(
              modifier = Modifier.fillMaxWidth(),
              title = sec.title.uppercase(Locale.US),
              subtitle = conciseSecondaryAbilitySubtitle(sec),
              accent = if (selected) Color(0xFFC084FC) else Color(0xFF38BDF8),
              secondaryAccent = if (selected) Color(0xFF00F5D4) else Color(0xFFC084FC),
              isRightColumn = false,
              isHighlighted = selected,
              badgeText = if (selected) "${sec.pointCost} PTS ON" else "${sec.pointCost} PTS",
              badgeColor = if (selected) Color(0xFFC084FC) else Color(0xFFFACC15),
              statusLabel = if (selected) "MOUNTED IN SECONDARY" else "DRAG OR TAP TO MOUNT",
              statusColor = if (selected) Color(0xFFC084FC) else Color(0xFF94A3B8),
              isTabletDeck = isTabletDeck,
              minHeightDp = if (isTabletDeck) 104.dp else 94.dp,
              titleMaxLines = 2,
              testTag = "rift_frame_sec_${sec.id.lowercase(Locale.US)}",
              visualContent = { visualDp ->
                Image(
                  bitmap = iconBmp,
                  contentDescription = sec.title,
                  contentScale = ContentScale.Fit,
                  modifier = Modifier.size((visualDp * 0.94f).coerceAtLeast(32.dp)),
                )
              },
              onClick = {
                commitDraftClamped(draftSlot.copy(secondaryType = sec.id))
                onPreviewInOverwatch()
              },
            )
          }
        }
      }

      // Borderless Reticle Charge Rift Frame Button
      val chargeFontSp = 10.4.sp
      val fluxDp = with(density) { chargeFontSp.toDp() }
      Row(
        modifier =
          Modifier.fillMaxWidth()
            .minimumInteractiveComponentSize()
            .height(48.dp)
            .cockpitReticleSurface(
              accent = Color(0xFF00F5D4),
              isSelected = true,
              drawCornerTicks = true,
              featherAlpha = 0.54f,
            )
            .clickable {
              commitDraftClamped(draftSlot)
              onChargeRiftFrame()
            }
            .padding(horizontal = 10.dp)
            .testTag("rift_frame_charge_btn"),
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

@Composable
internal fun RiftFrameCosmeticsSideBay(
  isTabletDeck: Boolean,
  headerHeight: Dp,
  wingStackGap: Dp,
  shardText: String,
  liveResonance: Int,
  shipSpinDeg: Float,
  fluxOrbitRad: Float,
  isCharged: Boolean,
  localPrefs: LocalPilotPrefs,
  currentUserId: String,
  indicatedShipId: Int = 10,
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
        "SKINS" -> RiftProgressionCatalog.hullTrims
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

    // Live 2.5D Rift Frame Preview + Live Resonance & Shards Readout
    Row(
      modifier =
        Modifier.fillMaxWidth()
          .padding(start = 18.dp)
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
        sizeDp = 44.dp,
      )
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
          text = "SKINS & RANKED EFFECTS",
          color = Color.White,
          fontSize = 10.5.sp,
          fontWeight = FontWeight.Black,
          fontFamily = FontFamily.Monospace,
          maxLines = 1,
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
          ResonanceCrestIcon(sizeDp = 11.dp)
          Text(
            text = "LIVE ${formatOverwatchResonance(liveResonance)}",
            color = Color(0xFFFFD700),
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
          )
          RiftShardGemIcon(sizeDp = 11.dp)
          Text(
            text = shardText,
            color = Color(0xFF00F5D4),
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
          )
        }
      }
    }

    // Borderless Laser-Underlined Category Reticle Tabs
    Row(
      modifier =
        Modifier.fillMaxWidth()
          .padding(start = 18.dp)
          .height(48.dp)
          .chamferedSciFiPodSurface(
            isRightSide = true,
            primaryBorder = Color(0xFF38BDF8),
            secondaryBorder = Color(0xFFC084FC),
          )
          .horizontalScroll(rememberScrollState())
          .padding(horizontal = 4.dp),
      horizontalArrangement = Arrangement.spacedBy(3.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      listOf("ALL", "SKINS", "FINISH", "WARP", "HALOS").forEach { tab ->
        val sel = categoryFilter == tab
        Box(
          modifier =
            Modifier.minimumInteractiveComponentSize()
              .fillMaxHeight()
              .cockpitReticleSurface(
                accent = Color(0xFFC084FC),
                isSelected = sel,
                drawCornerTicks = false,
                featherAlpha = if (sel) 0.42f else 0.14f,
              )
              .clickable { categoryFilter = tab }
              .padding(horizontal = 8.dp)
              .testTag("rift_frame_cosmetic_tab_${tab.lowercase(Locale.US)}"),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = tab,
            color = if (sel) Color.White else Color(0xFF7C93B0),
            fontSize = 9.6.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
            softWrap = false,
          )
        }
      }
    }

    Column(
      modifier = Modifier.weight(1f).fillMaxWidth().padding(start = 18.dp).verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      filteredCosmetics.forEach { item ->
        val isRanked = RiftProgressionCatalog.isRankedCosmeticCategory(item.category)
        val unlocked =
          if (isRanked) {
            RiftProgressionCatalog.isRankedCosmeticActive(item, liveResonance)
          } else {
            isPaidHullSkinOwnedInPrefs(item.id, localPrefs, uidOrNull)
          }
        val rawEquippedId = localPrefs.getEquippedCosmeticId(item.category, uidOrNull)
        val effectiveEquippedId =
          if (isRanked) {
            val savedItem = RiftProgressionCatalog.getCosmeticItem(item.category, rawEquippedId)
            if (savedItem != null && RiftProgressionCatalog.isRankedCosmeticActive(savedItem, liveResonance)) {
              rawEquippedId
            } else {
              0
            }
          } else {
            if (isPaidHullSkinOwnedInPrefs(rawEquippedId, localPrefs, uidOrNull)) rawEquippedId else 0
          }
        val equipped = unlocked && effectiveEquippedId == item.id
        val itemAccent = Color(item.accentColorHex)

        RiftVerticalCard(
          modifier = Modifier.fillMaxWidth(),
          title = item.name.uppercase(Locale.US),
          subtitle = item.summary,
          accent = if (equipped) Color(0xFF00F5D4) else itemAccent,
          secondaryAccent = if (unlocked) Color(0xFFFACC15) else Color(0xFF38BDF8),
          isRightColumn = true,
          isHighlighted = equipped,
          isCompactPeer = !equipped,
          badgeText =
            when {
              equipped -> "ACTIVE"
              unlocked -> if (isRanked) "ACTIVE TIER" else "OWNED"
              isRanked -> "${item.freeAtResonance}+"
              else -> "SKIN"
            },
          badgeColor = if (equipped) Color(0xFF00F5D4) else Color(0xFFFACC15),
          showBadgeResonanceIcon = !unlocked && isRanked,
          shardCost = if (!unlocked && !isRanked && item.shardCost > 0L) item.shardCost else null,
          statusLabel =
            when {
              equipped -> "EQUIPPED"
              unlocked -> "TAP TO EQUIP"
              isRanked -> "ACTIVE AT ${formatOverwatchResonance(item.freeAtResonance)} LIVE RESONANCE"
              else -> null
            },
          statusColor = if (equipped) Color(0xFF00F5D4) else Color(0xFFFACC15),
          minHeightDp = if (equipped) 112.dp else 52.dp,
          testTag = "rift_frame_cosmetic_${item.category.lowercase(Locale.US)}_${item.id}",
          visualContent = { visualDp ->
            if (item.category == "HULL_TRIM") {
              Spinning25dShipWithOrbitingFlux(
                shipId = indicatedShipId,
                shipSpinDeg = shipSpinDeg,
                fluxOrbitRad = fluxOrbitRad,
                isCharged = false,
                isBestShip = false,
                isSelected = equipped,
                sizeDp = visualDp,
                trimStyleOverride = item.id.coerceIn(0, 5),
              )
            } else {
              CosmeticSwatchEmblem(item = item, sizeDp = visualDp)
            }
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
      modifier = Modifier.weight(1f).fillMaxWidth().padding(start = 18.dp).verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      RiftProgressionCatalog.CONDUIT_RANK_TIERS.forEach { tier ->
        val isCurrent = currentTier.title == tier.title
        val isReached = liveResonance >= tier.minResonance
        val tierAccent = Color(tier.accentColorHex)

        RiftVerticalCard(
          modifier = Modifier.fillMaxWidth(),
          title = tier.title.uppercase(Locale.US),
          subtitle = "Start match with ${tier.startingBounty} Resonance",
          accent = if (isCurrent) Color(0xFF00F5D4) else tierAccent,
          secondaryAccent = if (isReached) Color(0xFFFACC15) else Color(0xFF38BDF8),
          isRightColumn = true,
          isHighlighted = isCurrent,
          isCompactPeer = !isCurrent,
          badgeText =
            when {
              isCurrent -> "ACTIVE"
              isReached -> "EARNED"
              else -> "${tier.minResonance}+"
            },
          badgeColor = if (isCurrent) Color(0xFF00F5D4) else tierAccent,
          showBadgeResonanceIcon = !isCurrent && !isReached,
          statusLabel = "MIN ${formatOverwatchResonance(tier.minResonance)} LIVE RESONANCE",
          statusColor = if (isReached) Color(0xFFFACC15) else Color(0xFF94A3B8),
          minHeightDp = if (isCurrent) 112.dp else 52.dp,
          testTag = "conduit_tier_${tier.title.lowercase(Locale.US).replace(" ", "_")}",
          visualContent = { visualDp ->
            Box(
              modifier =
                Modifier.size(visualDp)
                  .drawBehind {
                    val r = size.minDimension * 0.46f
                    drawCircle(
                      brush = Brush.radialGradient(listOf(tierAccent.copy(alpha = 0.24f), Color.Transparent)),
                      radius = r * 1.1f,
                      center = center,
                    )
                    drawCircle(
                      color = tierAccent.copy(alpha = 0.85f),
                      radius = r,
                      center = center,
                      style = Stroke(width = 1.25.dp.toPx()),
                    )
                  },
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
                badge =
                  if (isCurrent) "ACTIVE RANK"
                  else if (isReached) "ACTIVE TIER"
                  else "${formatOverwatchResonance(tier.minResonance)} LIVE RESONANCE",
                summary =
                  "Maintaining ${formatOverwatchResonance(tier.minResonance)} Live Resonance sets your starting match Resonance baseline to ${tier.startingBounty}.",
                tacticalNote = "Pure competitive tier progression — zero paid combat advantages.",
                accentHex = tier.accentColorHex,
                showResonanceCrest = true,
              )
            )
          },
        )
      }

      RiftProgressionCatalog.frameSlotGates
        .filter { it.freeAtResonance > 0 }
        .forEach { gate ->
          val active = liveResonance >= gate.freeAtResonance
          RiftVerticalCard(
            modifier = Modifier.fillMaxWidth(),
            title = gate.slotId.uppercase(Locale.US),
            subtitle = "Active at ${formatOverwatchResonance(gate.freeAtResonance)} Live Resonance",
            accent = if (active) Color(0xFF00F5D4) else Color(0xFFC084FC),
            isRightColumn = true,
            isHighlighted = active,
            isCompactPeer = !active,
            badgeText = if (active) "ACTIVE" else "${gate.freeAtResonance}+",
            badgeColor = if (active) Color(0xFF00F5D4) else Color(0xFFC084FC),
            showBadgeResonanceIcon = !active,
            statusLabel = gate.archetypeTitle.uppercase(Locale.US),
            statusColor = Color(0xFFCBD5E1),
            minHeightDp = if (active) 108.dp else 52.dp,
            testTag = "conduit_milestone_${gate.slotId.lowercase(Locale.US).replace(" ", "_")}",
            visualContent = { visualDp ->
              ResonanceCrestIcon(sizeDp = visualDp * 0.72f)
            },
            onClick = {
              onInspectTierStage(
                OverwatchCircularPrompt.InfoStageInspect(
                  kicker = "RANKED FRAME MILESTONE",
                  title = "${gate.slotId.uppercase(Locale.US)} • ${gate.archetypeTitle.uppercase(Locale.US)}",
                  badge =
                    if (active) "ACTIVE BY LIVE RESONANCE"
                    else "${formatOverwatchResonance(gate.freeAtResonance)} LIVE RESONANCE",
                  summary = "Active at ${formatOverwatchResonance(gate.freeAtResonance)} Live Resonance.",
                  tacticalNote =
                    "Ranked loadout bay maintained strictly through Live Resonance. Dropping below ${formatOverwatchResonance(gate.freeAtResonance)} preserves your saved build and switches to Frame A.",
                  accentHex = 0xFFC084FCL,
                  shipId = 10,
                  showResonanceCrest = true,
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
  activeCustomSlotId: String,
  purchaseStatusMessage: String?,
  localPrefs: LocalPilotPrefs,
  currentUserId: String,
  indicatedShipId: Int = 1,
  shipSpinDeg: Float = 0f,
  fluxOrbitRad: Float = 0f,
  onBackToHub: () -> Unit,
  onInspectFrameB: (FrameSlotGateInfo, Boolean, Boolean) -> Unit,
  onInspectPaidSkin: (CosmeticCatalogItem, Boolean, Boolean) -> Unit,
  onOpenRiftFrameBay: () -> Unit,
  onOpenConduitQuests: () -> Unit,
) {
  val uidOrNull = currentUserId.ifBlank { null }
  val normalizedActiveId = localPrefs.normalizeCustomSlotId(activeCustomSlotId)
  val isTesterMode = RiftBillingManager.TESTER_INFINITE_SHARDS_ACTIVE
  val frameBGate = remember { RiftProgressionCatalog.getFrameSlotGate("Frame B") }
  val isFrameBOwned = isFrameBPurchasedInPrefs(localPrefs, uidOrNull)
  val isFrameBSelected = isFrameBOwned && normalizedActiveId == "Frame B"
  val paidSkins = remember { RiftProgressionCatalog.hullTrims.filter { it.id >= 1 } }
  val equippedSkinId = localPrefs.getEquippedCosmeticId("HULL_TRIM", uidOrNull)
  val spotlightSkinId =
    paidSkins.firstOrNull { it.id == equippedSkinId }?.id ?: paidSkins.firstOrNull()?.id ?: 1

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
      modifier = Modifier.weight(1f).fillMaxWidth().padding(end = 18.dp).verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      // Live Treasury & Billing Status Shelf
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
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
          )
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
          ResonanceCrestIcon(sizeDp = 12.dp)
          Text(
            text = "LIVE ${formatOverwatchResonance(liveResonance)} RESONANCE",
            color = Color(0xFFFFD700),
            fontSize = 9.5.sp,
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
                "Google Play Billing active (${RiftBillingManager.STUDIO_LEGAL_NAME}). Unlock Frame B or permanent Ship Skins below."
              },
          color = if (purchaseStatusMessage != null) Color(0xFF00F5D4) else Color(0xFFCBD5E1),
          fontSize = 9.5.sp,
          fontFamily = FontFamily.SansSerif,
        )
      }

      // Single Purchasable Frame B Bay Card (250 Shards, permanent once bought)
      RiftVerticalCard(
        modifier = Modifier.fillMaxWidth(),
        title = "FRAME B BAY",
        subtitle = "Permanent custom 100-point bay",
        accent = if (isFrameBSelected) Color(0xFF00F5D4) else Color(0xFFC084FC),
        secondaryAccent = if (isFrameBOwned) Color(0xFF38BDF8) else Color(0xFFFACC15),
        isRightColumn = false,
        isHighlighted = isFrameBSelected || isFrameBOwned,
        badgeText =
          when {
            isFrameBSelected -> "ACTIVE"
            isFrameBOwned -> "OWNED"
            else -> "PERMANENT"
          },
        badgeColor = if (isFrameBSelected) Color(0xFF00F5D4) else Color(0xFFFACC15),
        shardCost = if (!isFrameBOwned) frameBGate.shardCost else null,
        statusLabel =
          when {
            isFrameBSelected -> "ACTIVE BAY"
            isFrameBOwned -> "TAP TO SELECT"
            else -> null
          },
        statusColor = if (isFrameBSelected) Color(0xFF00F5D4) else Color(0xFFC084FC),
        minHeightDp = 96.dp,
        testTag = "shard_vault_slot_B",
        visualContent = { visualDp ->
          PrismaticShardClusterEmblem(
            sizeDp = visualDp,
            accentColor = if (isFrameBSelected) Color(0xFF00F5D4) else Color(0xFFC084FC),
            crystalCount = if (isFrameBOwned) 3 else 2,
          )
        },
        onClick = { onInspectFrameB(frameBGate, isFrameBOwned, isFrameBSelected) },
      )

      // Permanent Purchasable Ship Skins (HULL_TRIM 1..5 rendered as live spinning 2.5D skinned hulls)
      paidSkins.forEach { skin ->
        val owned = isPaidHullSkinOwnedInPrefs(skin.id, localPrefs, uidOrNull)
        val equipped = owned && equippedSkinId == skin.id
        val isSpotlightSkin = skin.id == spotlightSkinId
        val skinAccent = Color(skin.accentColorHex)

        RiftVerticalCard(
          modifier = Modifier.fillMaxWidth(),
          title = skin.name.uppercase(Locale.US),
          subtitle = skin.summary,
          accent = if (equipped) Color(0xFF00F5D4) else skinAccent,
          secondaryAccent = if (owned) Color(0xFFFACC15) else Color(0xFF38BDF8),
          isRightColumn = false,
          isHighlighted = isSpotlightSkin,
          isCompactPeer = !isSpotlightSkin,
          badgeText =
            when {
              equipped -> "EQUIPPED"
              owned -> "OWNED"
              else -> "SHIP SKIN"
            },
          badgeColor = if (equipped) Color(0xFF00F5D4) else skinAccent,
          shardCost = if (!owned) skin.shardCost else null,
          statusLabel =
            when {
              equipped -> "ACTIVE LIVERY"
              owned -> "TAP TO EQUIP"
              else -> null
            },
          statusColor = if (equipped) Color(0xFF00F5D4) else Color(0xFFFACC15),
          minHeightDp = if (isSpotlightSkin) 114.dp else 54.dp,
          testTag = "shard_store_skin_${skin.id}",
          visualContent = { visualDp ->
            Spinning25dShipWithOrbitingFlux(
              shipId = indicatedShipId,
              shipSpinDeg = shipSpinDeg,
              fluxOrbitRad = fluxOrbitRad,
              isCharged = false,
              isBestShip = false,
              isSelected = isSpotlightSkin || equipped,
              sizeDp = visualDp,
              trimStyleOverride = skin.id.coerceIn(0, 5),
            )
          },
          onClick = { onInspectPaidSkin(skin, owned, equipped) },
        )
      }

      Box(
        modifier =
          Modifier.fillMaxWidth()
            .minimumInteractiveComponentSize()
            .height(48.dp)
            .cockpitReticleSurface(
              accent = Color(0xFFC084FC),
              isSelected = true,
              drawCornerTicks = true,
              featherAlpha = 0.48f,
            )
            .clickable { onOpenRiftFrameBay() }
            .testTag("shard_vault_open_rift_frame"),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          text = "CONFIGURE RIFT FRAME >>",
          color = Color(0xFFE9D5FF),
          fontSize = 9.8.sp,
          fontWeight = FontWeight.Black,
          fontFamily = FontFamily.Monospace,
        )
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
  var selectedPackIndex by remember { mutableIntStateOf(2) }

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
      modifier = Modifier.weight(1f).fillMaxWidth().padding(start = 18.dp).verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(4.dp),
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
        val isFeaturedPack = idx == selectedPackIndex

        RiftVerticalCard(
          modifier = Modifier.fillMaxWidth(),
          title = sku.title.uppercase(Locale.US),
          subtitle = subtitle,
          accent = packAccent,
          secondaryAccent = Color(0xFF00F5D4),
          isRightColumn = true,
          isHighlighted = isFeaturedPack,
          isCompactPeer = !isFeaturedPack,
          badgeText = sku.badgeLabel,
          badgeColor = packAccent,
          statusLabel = if (isBusy) "PROCESSING..." else "${sku.displayPriceUsd} • BUY PACK",
          statusColor = if (isBusy) Color(0xFF00F5D4) else Color.White,
          minHeightDp = if (isFeaturedPack) 112.dp else 52.dp,
          testTag = "shard_store_pack_${sku.productId}",
          visualContent = { visualDp ->
            PrismaticShardClusterEmblem(
              sizeDp = visualDp,
              accentColor = packAccent,
              crystalCount = (idx + 1).coerceAtMost(3),
            )
          },
          onClick = {
            selectedPackIndex = idx
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

@Composable
fun BuffIconPreview(buffType: Int, sizeDp: Int, accentColor: Color) {
  val visualRev = RiftVisualForgeState.visualRevision
  val bmp = remember(buffType, visualRev) { RiftSpriteBaker.getBuffIconBitmap(buffType).asImageBitmap() }
  Box(
    modifier =
      Modifier.size(sizeDp.dp).drawBehind {
        val radius = size.minDimension * 0.56f
        drawCircle(
          brush =
            Brush.radialGradient(
              colors = listOf(accentColor.copy(alpha = 0.28f), Color.Transparent),
              center = center,
              radius = radius.coerceAtLeast(1f),
            ),
          radius = radius,
          center = center,
        )
      },
    contentAlignment = Alignment.Center,
  ) {
    Canvas(modifier = Modifier.size((sizeDp - 6).coerceAtLeast(16).dp)) {
      drawImage(
        image = bmp,
        dstOffset = IntOffset.Zero,
        dstSize = IntSize(size.width.toInt().coerceAtLeast(1), size.height.toInt().coerceAtLeast(1)),
      )
    }
  }
}

@Composable
fun ShipSpritePreview(shipId: Int, sizeDp: Int = 32, isAllied: Boolean = true) {
  val customVer = RiftSpriteBaker.customShip9Version
  val trimVer = RiftSpriteBaker.activeHullTrimStyle
  val shipName = remember(shipId) { RiftCodex.getShipById(shipId.coerceIn(1, 10)).name }
  val bmp =
    remember(shipId, isAllied, customVer, trimVer) {
      RiftSpriteBaker.getShipSprite(shipId.coerceIn(1, 10), isAllied = isAllied, isHitFlashing = false).asImageBitmap()
    }
  Image(
    bitmap = bmp,
    contentDescription = shipName,
    filterQuality = FilterQuality.High,
    modifier = Modifier.size(sizeDp.dp),
  )
}

