package com.example.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.PowerUpCodexEntry
import com.example.engine.RiftCodex
import com.example.engine.RiftSpriteBaker
import com.example.engine.RiftVisualForgeState
import com.example.engine.ShipSpec
import java.util.Locale
import kotlin.math.PI
import kotlin.math.sin

private val CodexMono = FontFamily.Monospace
private val CodexSans = FontFamily.SansSerif

private fun cleanCodexCopy(raw: String): String =
  raw
    .replace("\u26A1", "Flux")
    .replace(Regex("(?i)stasis\\s+field"), "Stasis")

@Composable
private fun VectorCodexEmblem(accent: Color, sizeDp: Dp = 16.dp) {
  val hexPath = remember { Path() }
  Canvas(modifier = Modifier.size(sizeDp)) {
    val cx = size.width * 0.5f
    val cy = size.height * 0.5f
    val r = size.minDimension * 0.44f
    hexPath.reset()
    hexPath.moveTo(cx, cy - r)
    hexPath.lineTo(cx + r * 0.86f, cy - r * 0.5f)
    hexPath.lineTo(cx + r * 0.86f, cy + r * 0.5f)
    hexPath.lineTo(cx, cy + r)
    hexPath.lineTo(cx - r * 0.86f, cy + r * 0.5f)
    hexPath.lineTo(cx - r * 0.86f, cy - r * 0.5f)
    hexPath.close()
    drawPath(hexPath, accent.copy(alpha = 0.18f))
    drawPath(hexPath, accent, style = Stroke(width = 1.3.dp.toPx()))
    drawLine(
      color = Color.White,
      start = Offset(cx - r * 0.42f, cy),
      end = Offset(cx + r * 0.42f, cy),
      strokeWidth = 1.2.dp.toPx(),
      cap = StrokeCap.Round,
    )
  }
}

@Composable
private fun VectorSwirlDeckGlyph(accent: Color, sizeDp: Dp = 11.dp) {
  Canvas(modifier = Modifier.size(sizeDp)) {
    val cx = size.width * 0.5f
    val cy = size.height * 0.5f
    val r = size.minDimension * 0.42f
    drawArc(
      color = accent,
      startAngle = 20f,
      sweepAngle = 140f,
      useCenter = false,
      topLeft = Offset(cx - r, cy - r),
      size = Size(r * 2f, r * 2f),
      style = Stroke(width = 1.4.dp.toPx(), cap = StrokeCap.Round),
    )
    drawArc(
      color = Color.White,
      startAngle = 200f,
      sweepAngle = 140f,
      useCenter = false,
      topLeft = Offset(cx - r * 0.72f, cy - r * 0.72f),
      size = Size(r * 1.44f, r * 1.44f),
      style = Stroke(width = 1.2.dp.toPx(), cap = StrokeCap.Round),
    )
  }
}

@Composable
private fun VectorHullChevronGlyph(accent: Color, sizeDp: Dp = 11.dp) {
  val hullPath = remember { Path() }
  Canvas(modifier = Modifier.size(sizeDp)) {
    val w = size.width
    val h = size.height
    hullPath.reset()
    hullPath.moveTo(w * 0.88f, h * 0.50f)
    hullPath.lineTo(w * 0.16f, h * 0.16f)
    hullPath.lineTo(w * 0.34f, h * 0.50f)
    hullPath.lineTo(w * 0.16f, h * 0.84f)
    hullPath.close()
    drawPath(hullPath, accent.copy(alpha = 0.28f))
    drawPath(hullPath, accent, style = Stroke(width = 1.2.dp.toPx()))
  }
}

@Composable
fun CodexModal(
  isNexusStreaming: Boolean = false,
  onOpenNexus: () -> Unit = {},
  onCloseModal: () -> Unit = {},
) {
  val isCompactPhone = LocalConfiguration.current.screenHeightDp < 480
  var selectedTab by remember { mutableIntStateOf(0) }
  val spriteVersion = RiftSpriteBaker.customShip9Version
  val visualRev = RiftVisualForgeState.visualRevision
  val allTenShips = remember(spriteVersion, visualRev) { RiftCodex.getAllTenShips() }
  val buffPairs = remember { RiftCodex.POWERUP_CODEX.chunked(2) }
  val shipPairs = remember(allTenShips) { allTenShips.chunked(2) }

  Column(
    modifier =
      Modifier.fillMaxSize()
        .padding(if (isCompactPhone) 6.dp else 8.dp)
        .testTag("codex_modal"),
    verticalArrangement = Arrangement.spacedBy(if (isCompactPhone) 5.dp else 7.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Row(
        modifier = Modifier.weight(1f, fill = false).padding(end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
      ) {
        VectorCodexEmblem(
          accent = Color(0xFF00F5D4),
          sizeDp = if (isCompactPhone) 16.dp else 19.dp,
        )
        Column {
          Text(
            text =
              if (isCompactPhone) "ARCHIVE & CODEX"
              else "ARCHIVE & CODEX // TACTICAL FIELD MANUAL",
            color = Color.White,
            fontSize = if (isCompactPhone) 12.5.sp else 15.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = CodexSans,
            letterSpacing = 0.5.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          if (!isCompactPhone) {
            Text(
              text = "SUPPLY BUFFS • PLAYFIELD RIFTS • 10-HULL 0-RESONANCE PARITY MATRIX",
              color = Color(0xFF00F5D4),
              fontSize = 9.sp,
              fontWeight = FontWeight.Bold,
              fontFamily = CodexMono,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
      }

      Row(
        modifier =
          Modifier.background(Color(0xFF050B14), RoundedCornerShape(8.dp))
            .border(1.dp, Color(0xFF1E3A5F), RoundedCornerShape(8.dp))
            .padding(2.dp)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        CodexTabPill(
          label = "[SUPPLY BUFFS]",
          glyphMode = 0,
          selected = selectedTab == 0,
          accentColor = Color(0xFF38BDF8),
          testTag = "codex_tab_buffs",
        ) {
          selectedTab = 0
        }
        CodexTabPill(
          label = "[PLAYFIELD RIFTS]",
          glyphMode = 1,
          selected = selectedTab == 1,
          accentColor = Color(0xFFA855F7),
          testTag = "codex_tab_rifts",
        ) {
          selectedTab = 1
        }
        CodexTabPill(
          label = "[10-HULL MATRIX]",
          glyphMode = 2,
          selected = selectedTab == 2,
          accentColor = Color(0xFF00F5D4),
          testTag = "codex_tab_ships",
        ) {
          selectedTab = 2
        }
      }
    }

    when (selectedTab) {
      0 -> {
        LazyColumn(
          modifier =
            Modifier.weight(1f)
              .fillMaxWidth()
              .background(Color(0xFF081426), RoundedCornerShape(10.dp))
              .border(1.dp, Color(0xFF1B3454), RoundedCornerShape(10.dp))
              .padding(if (isCompactPhone) 6.dp else 8.dp),
          verticalArrangement = Arrangement.spacedBy(if (isCompactPhone) 5.dp else 7.dp),
        ) {
          item(key = "supply_buffs_highlights") {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(if (isCompactPhone) 5.dp else 8.dp),
            ) {
              CodexHighlightCard(
                modifier = Modifier.weight(1f),
                title = "SPECIAL BUFF SLOT (0 FLUX)",
                body =
                  "Nova Ring, Shatter Cone, Seeker Torpedo, Holo Phantom, Stasis Barrier, Hyper Surge & 5-Drone Swarm equip to the 4th button at 0 Flux cost.",
                bgColor = Color(0xFF092318),
                borderColor = Color(0xFF10B981),
                bodyColor = Color(0xFFD1FAE5),
                isCompactPhone = isCompactPhone,
              )
              CodexHighlightCard(
                modifier = Modifier.weight(1f),
                title = "❖ HULL ENHANCERS",
                body =
                  "Ricochet Rounds, Speed / Thrust / Turn / Max Flux / Reactor Overcharge (+1..+3 stacks), Multi-Shot (+25), Repel (+1), & Flux Surge.",
                bgColor = Color(0xFF261908),
                borderColor = Color(0xFFF59E0B),
                bodyColor = Color(0xFFFEF3C7),
                isCompactPhone = isCompactPhone,
              )
              CodexHighlightCard(
                modifier = Modifier.weight(1f),
                title = "100-PT HULL PARITY",
                body =
                  "100-PT HULL PARITY: All 10 ships — including modular Rift Frame — are unlocked from 0 Resonance on an identical 100-Point competitive budget.",
                bgColor = Color(0xFF0D1E36),
                borderColor = Color(0xFF38BDF8),
                bodyColor = Color(0xFFE0F2FE),
                isCompactPhone = isCompactPhone,
              )
            }
          }

          items(buffPairs, key = { pair -> pair.first().id }) { pair ->
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(if (isCompactPhone) 6.dp else 8.dp),
            ) {
              pair.forEach { entry ->
                CodexSupplyBuffCard(
                  entry = entry,
                  isCompactPhone = isCompactPhone,
                  modifier = Modifier.weight(1f),
                )
              }
              if (pair.size == 1) {
                Spacer(modifier = Modifier.weight(1f))
              }
            }
          }
        }
      }

      1 -> {
        LazyColumn(
          modifier =
            Modifier.weight(1f)
              .fillMaxWidth()
              .background(Color(0xFF081426), RoundedCornerShape(10.dp))
              .border(1.dp, Color(0xFF1B3454), RoundedCornerShape(10.dp))
              .padding(if (isCompactPhone) 6.dp else 8.dp)
              .testTag("codex_playfield_rifts_list"),
          verticalArrangement = Arrangement.spacedBy(if (isCompactPhone) 6.dp else 8.dp),
        ) {
          item(key = "playfield_rifts_highlights") {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(if (isCompactPhone) 5.dp else 8.dp),
            ) {
              CodexHighlightCard(
                modifier = Modifier.weight(1f),
                title = "45S RIFT CYCLES",
                body =
                  "Playfield Rifts relocate every 45s outside the Safe Zone and never spawn inside Stasis Breach Jails or Rift Control rings.",
                bgColor = Color(0xFF1B102E),
                borderColor = Color(0xFFA855F7),
                bodyColor = Color(0xFFE9D5FF),
                isCompactPhone = isCompactPhone,
              )
              CodexHighlightCard(
                modifier = Modifier.weight(1f),
                title = "MAX GRAVITY & UNSTABLE RIFT",
                body =
                  "420px Gravity Rifts pull nearby obstacles & barriers. Touching non-paired Rifts fuse into a 10.0s Unstable Rift (75% Max Flux blast).",
                bgColor = Color(0xFF0A2229),
                borderColor = Color(0xFF2DD4BF),
                bodyColor = Color(0xFFCCFBF1),
                isCompactPhone = isCompactPhone,
              )
              CodexHighlightCard(
                modifier = Modifier.weight(1f),
                title = "PAIRED PHASE RIFTS",
                body =
                  "Radar colors: Crimson = Plasma Rift, White = Repulsor Rift, Violet = Gravity Rift, Teal/Cyan = Paired Phase Rifts (Phase Rift A & B).",
                bgColor = Color(0xFF241016),
                borderColor = Color(0xFFEF4444),
                bodyColor = Color(0xFFFECACA),
                isCompactPhone = isCompactPhone,
              )
            }
          }

          items(RiftCodex.RIFT_ANOMALIES_CODEX, key = { it.id }) { rift ->
            val accentColor =
              runCatching { Color(android.graphics.Color.parseColor(rift.accentHex)) }
                .getOrDefault(Color(0xFF38BDF8))
            Row(
              modifier =
                Modifier.fillMaxWidth()
                  .background(Color(0xFF0E1F38), RoundedCornerShape(8.dp))
                  .border(1.dp, accentColor.copy(alpha = 0.55f), RoundedCornerShape(8.dp))
                  .padding(if (isCompactPhone) 8.dp else 10.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(if (isCompactPhone) 10.dp else 12.dp),
            ) {
              RiftAnomalyIconPreview(
                riftId = rift.id,
                sizeDp = if (isCompactPhone) 54 else 64,
                accentColor = accentColor,
              )
              Column(modifier = Modifier.weight(1f)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                  ) {
                    Text(
                      text = cleanCodexCopy(rift.title),
                      color = accentColor,
                      fontSize = if (isCompactPhone) 12.sp else 13.5.sp,
                      fontWeight = FontWeight.Bold,
                      fontFamily = CodexSans,
                    )
                    Box(
                      modifier =
                        Modifier.background(Color(0x22FFFFFF), RoundedCornerShape(4.dp))
                          .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                      Text(
                        text = cleanCodexCopy(rift.classification),
                        color = Color(0xFFCBD5E1),
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = CodexMono,
                      )
                    }
                  }
                  Box(
                    modifier =
                      Modifier.background(accentColor.copy(alpha = 0.16f), RoundedCornerShape(6.dp))
                        .border(1.dp, accentColor, RoundedCornerShape(6.dp))
                        .padding(horizontal = 7.dp, vertical = 2.dp)
                  ) {
                    Text(
                      text = cleanCodexCopy(rift.specBadge),
                      color = accentColor,
                      fontSize = if (isCompactPhone) 9.sp else 10.sp,
                      fontWeight = FontWeight.Bold,
                      fontFamily = CodexMono,
                    )
                  }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                  text = cleanCodexCopy(rift.effectSummary),
                  color = Color.White,
                  fontSize = if (isCompactPhone) 10.sp else 11.sp,
                )
                Spacer(modifier = Modifier.height(1.dp))
                Text(
                  text = "TACTICAL: ${cleanCodexCopy(rift.tacticalNote)}",
                  color = Color(0xFF94A3B8),
                  fontSize = if (isCompactPhone) 9.2.sp else 10.sp,
                  lineHeight = if (isCompactPhone) 12.sp else 13.sp,
                )
              }
            }
          }
        }
      }

      else -> {
        LazyColumn(
          modifier =
            Modifier.weight(1f)
              .fillMaxWidth()
              .background(Color(0xFF081426), RoundedCornerShape(10.dp))
              .border(1.dp, Color(0xFF1B3454), RoundedCornerShape(10.dp))
              .padding(if (isCompactPhone) 6.dp else 8.dp)
              .testTag("codex_ships_matrix_list"),
          verticalArrangement = Arrangement.spacedBy(if (isCompactPhone) 6.dp else 8.dp),
        ) {
          item(key = "hull_matrix_header") {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(if (isCompactPhone) 5.dp else 8.dp),
            ) {
              CodexHighlightCard(
                modifier = Modifier.weight(1f),
                title = "ALL 10 HULLS OPEN AT 0 RESONANCE",
                body =
                  "Viper (#1) through Eclipse (#9) and modular Rift Frame (#10) are immediately playable from 0 Resonance.",
                bgColor = Color(0xFF082032),
                borderColor = Color(0xFF00F5D4),
                bodyColor = Color(0xFFE0F2FE),
                isCompactPhone = isCompactPhone,
              )
              CodexHighlightCard(
                modifier = Modifier.weight(1f),
                title = "STRICT 100-POINT COMPETITIVE PARITY",
                body =
                  "Every signature hull and Rift Frame bay (Frame A–E) distributes an identical 100-Point stat & ordnance budget.",
                bgColor = Color(0xFF1E1035),
                borderColor = Color(0xFFC084FC),
                bodyColor = Color(0xFFF3E8FF),
                isCompactPhone = isCompactPhone,
              )
              CodexHighlightCard(
                modifier = Modifier.weight(1f),
                title = "HELM TWIN-STICK REMAPPER",
                body =
                  "Customize hardware gamepad binds, camera FOV, and HUD layout anytime inside HELM SYSTEMS.",
                bgColor = Color(0xFF092318),
                borderColor = Color(0xFF34D399),
                bodyColor = Color(0xFFD1FAE5),
                isCompactPhone = isCompactPhone,
              )
            }
          }

          items(shipPairs, key = { pair -> pair.first().id }) { pair ->
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(if (isCompactPhone) 6.dp else 8.dp),
            ) {
              pair.forEach { ship ->
                CodexHullReferenceCard(
                  ship = ship,
                  spriteVersion = spriteVersion,
                  isCompactPhone = isCompactPhone,
                  modifier = Modifier.weight(1f),
                )
              }
              if (pair.size == 1) {
                Spacer(modifier = Modifier.weight(1f))
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun CodexSupplyBuffCard(
  entry: PowerUpCodexEntry,
  isCompactPhone: Boolean,
  modifier: Modifier = Modifier,
) {
  val buffType = entry.buffTypeIndex
  val accentColor =
    when (buffType) {
      0 -> Color(0xFFF59E0B)
      1 -> Color(0xFFFB7185)
      2 -> Color(0xFFEF4444)
      3 -> Color(0xFF38BDF8)
      4 -> Color(0xFF22D3EE)
      5 -> Color(0xFFA855F7)
      6 -> Color(0xFF10B981)
      7 -> Color(0xFF60A5FA)
      8 -> Color(0xFFFACC15)
      9 -> Color(0xFFC084FC)
      11 -> Color(0xFF00F5D4)
      -10 -> Color(0xFF38BDF8)
      -11 -> Color(0xFFF59E0B)
      -12 -> Color(0xFF10B981)
      else -> Color(0xFF38BDF8)
    }

  Row(
    modifier =
      modifier
        .background(Color(0xFF0E1F38), RoundedCornerShape(8.dp))
        .border(1.dp, accentColor.copy(alpha = 0.50f), RoundedCornerShape(8.dp))
        .padding(if (isCompactPhone) 7.dp else 9.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(if (isCompactPhone) 8.dp else 10.dp),
  ) {
    BuffIconPreview(
      buffType = buffType,
      sizeDp = if (isCompactPhone) 42 else 48,
      accentColor = accentColor,
    )
    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = cleanCodexCopy(entry.title),
          color = accentColor,
          fontSize = if (isCompactPhone) 10.2.sp else 11.5.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = CodexSans,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.weight(1f).padding(end = 4.dp),
        )
        Box(
          modifier =
            Modifier.background(Color(0x26FFD700), RoundedCornerShape(5.dp))
              .border(1.dp, Color(0xFFFFD700), RoundedCornerShape(5.dp))
              .padding(horizontal = 5.dp, vertical = 1.5.dp)
        ) {
          Text(
            text = "${entry.dropRatePercent}%",
            color = Color(0xFFFFD700),
            fontSize = 8.5.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = CodexMono,
          )
        }
      }
      Text(
        text = cleanCodexCopy(entry.effectSummary),
        color = Color.White,
        fontSize = if (isCompactPhone) 9.2.sp else 10.sp,
        lineHeight = if (isCompactPhone) 11.5.sp else 12.5.sp,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
      )
      Text(
        text = "TACTICAL: ${cleanCodexCopy(entry.tacticalNote)}",
        color = Color(0xFF94A3B8),
        fontSize = if (isCompactPhone) 8.5.sp else 9.2.sp,
        lineHeight = if (isCompactPhone) 10.8.sp else 12.sp,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
private fun CodexHullReferenceCard(
  ship: ShipSpec,
  spriteVersion: Int,
  isCompactPhone: Boolean,
  modifier: Modifier = Modifier,
) {
  val isModularFrame = ship.id == 10
  val accentColor = if (isModularFrame) Color(0xFFEC4899) else Color(ship.accentColorHex)
  val shipBmp =
    remember(ship.id, spriteVersion) {
      RiftSpriteBaker.getShipSprite(ship.id, isAllied = true, isHitFlashing = false).asImageBitmap()
    }
  val displayName =
    if (isModularFrame) "RIFT FRAME (MODULAR)"
    else ship.name.replace(Regex("^[0-9]+-"), "").uppercase(Locale.US)

  val secondaryDisplay =
    when {
      ship.id == 9 -> "Stasis (320 Flux • 6.0s • 275px • 8.5s CD)"
      ship.id == 6 -> "Cloak (36 Flux/s • 4.5s CD) + 0-Flux Decloak EMP"
      ship.id == 1 -> "Phase Dash (320 Flux • +35% Rail Overcharge)"
      ship.supportsNativeMultifire && !ship.hasBomb -> "Native Split Multi-Shot Toggle (0 Extra Flux)"
      else -> cleanCodexCopy(ship.secondaryTitle)
    }

  Column(
    modifier =
      modifier
        .background(Color(0xFF0E1F38), RoundedCornerShape(9.dp))
        .border(1.dp, accentColor.copy(alpha = 0.55f), RoundedCornerShape(9.dp))
        .padding(if (isCompactPhone) 7.dp else 9.dp)
        .testTag("codex_hull_card_${ship.id}"),
    verticalArrangement = Arrangement.spacedBy(5.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.weight(1f),
      ) {
        Box(
          modifier =
            Modifier.size(if (isCompactPhone) 40.dp else 46.dp)
              .background(Color(0xFF040914), RoundedCornerShape(8.dp))
              .border(1.dp, accentColor.copy(alpha = 0.65f), RoundedCornerShape(8.dp)),
          contentAlignment = Alignment.Center,
        ) {
          Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
              brush = Brush.radialGradient(listOf(accentColor.copy(alpha = 0.35f), Color.Transparent)),
              radius = size.minDimension * 0.48f,
            )
          }
          Image(
            bitmap = shipBmp,
            contentDescription = displayName,
            filterQuality = FilterQuality.High,
            modifier = Modifier.size(if (isCompactPhone) 34.dp else 38.dp),
          )
        }
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "#${ship.id} $displayName",
            color = Color.White,
            fontSize = if (isCompactPhone) 11.sp else 12.5.sp,
            fontWeight = FontWeight.Black,
            fontFamily = CodexSans,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Text(
            text = ship.role.uppercase(Locale.US),
            color = accentColor,
            fontSize = if (isCompactPhone) 9.sp else 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = CodexSans,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }
      Box(
        modifier =
          Modifier.background(Color(0xFF06281E), RoundedCornerShape(5.dp))
            .border(1.dp, Color(0xFF10B981), RoundedCornerShape(5.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
      ) {
        Text(
          text = "0 RES • 100 PTS",
          color = Color(0xFF34D399),
          fontSize = 8.sp,
          fontWeight = FontWeight.Black,
          fontFamily = CodexMono,
        )
      }
    }

    Row(
      modifier =
        Modifier.fillMaxWidth()
          .background(Color(0xFF060E1C), RoundedCornerShape(6.dp))
          .padding(horizontal = 6.dp, vertical = 4.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      HullStatChip("SPD", ship.speedPts, Color(0xFF38BDF8))
      HullStatChip("THR", ship.thrustPts, Color(0xFFF59E0B))
      HullStatChip("TRN", ship.turnPts, Color(0xFFEC4899))
      HullStatChip("FLX", ship.maxFluxPts, Color(0xFF00F5D4))
      HullStatChip("RCH", ship.rechargePts, Color(0xFF10B981))
    }

    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
      Text(
        text =
          "PRI (${ship.primaryPts}P): ${cleanCodexCopy(ship.primaryTitle)} • ${ship.primaryDamage.toInt()} DMG • ${ship.primaryCost.toInt()} Flux",
        color = Color(0xFFE2E8F0),
        fontSize = if (isCompactPhone) 8.8.sp else 9.5.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = CodexMono,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Text(
        text = "SEC (${ship.secondaryPts}P): $secondaryDisplay",
        color = Color(0xFFFBBF24),
        fontSize = if (isCompactPhone) 8.8.sp else 9.5.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = CodexMono,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
private fun HullStatChip(label: String, pts: Int, accent: Color) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(3.dp),
  ) {
    Text(
      text = label,
      color = Color(0xFF94A3B8),
      fontSize = 7.8.sp,
      fontWeight = FontWeight.Bold,
      fontFamily = CodexMono,
    )
    Box(
      modifier =
        Modifier.size(width = 18.dp, height = 4.dp)
          .clip(RoundedCornerShape(2.dp))
          .background(Color(0xFF1E293B))
    ) {
      Box(
        modifier =
          Modifier.fillMaxHeight()
            .fillMaxWidth((0.12f + (pts.coerceIn(0, 30) / 30f) * 0.88f).coerceIn(0f, 1f))
            .background(accent, RoundedCornerShape(2.dp))
      )
    }
    Text(
      text = "$pts",
      color = Color.White,
      fontSize = 8.sp,
      fontWeight = FontWeight.Black,
      fontFamily = CodexMono,
    )
  }
}

@Composable
private fun CodexTabPill(
  label: String,
  glyphMode: Int,
  selected: Boolean,
  accentColor: Color,
  testTag: String,
  onClick: () -> Unit,
) {
  Box(
    modifier =
      Modifier.height(28.dp)
        .clip(RoundedCornerShape(6.dp))
        .background(
          if (selected) {
            Brush.verticalGradient(
              listOf(accentColor.copy(alpha = 0.34f), accentColor.copy(alpha = 0.16f))
            )
          } else {
            Brush.verticalGradient(listOf(Color(0xFF0A1424), Color(0xFF060C16)))
          }
        )
        .border(1.dp, if (selected) accentColor else Color(0xFF1E293B), RoundedCornerShape(6.dp))
        .clickable(onClick = onClick)
        .padding(horizontal = 9.dp)
        .testTag(testTag),
    contentAlignment = Alignment.Center,
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      when (glyphMode) {
        0 -> FluxEmblem(sizeDp = 10.dp)
        1 -> VectorSwirlDeckGlyph(accent = accentColor, sizeDp = 10.dp)
        else -> VectorHullChevronGlyph(accent = accentColor, sizeDp = 10.dp)
      }
      Text(
        text = label,
        color = if (selected) Color.White else Color(0xFF94A3B8),
        fontSize = 9.5.sp,
        fontWeight = FontWeight.ExtraBold,
        fontFamily = CodexMono,
        maxLines = 1,
      )
    }
  }
}

@Composable
private fun CodexHighlightCard(
  modifier: Modifier = Modifier,
  title: String,
  body: String,
  bgColor: Color,
  borderColor: Color,
  bodyColor: Color,
  isCompactPhone: Boolean,
) {
  Box(
    modifier =
      modifier
        .background(bgColor, RoundedCornerShape(8.dp))
        .border(1.dp, borderColor, RoundedCornerShape(8.dp))
        .padding(
          horizontal = if (isCompactPhone) 6.dp else 10.dp,
          vertical = if (isCompactPhone) 5.dp else 7.dp,
        )
  ) {
    Column {
      Text(
        text = title,
        color = borderColor,
        fontSize = if (isCompactPhone) 9.sp else 10.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = CodexMono,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Text(
        text = body,
        color = bodyColor,
        fontSize = if (isCompactPhone) 8.4.sp else 9.4.sp,
        lineHeight = if (isCompactPhone) 11.sp else 12.5.sp,
        maxLines = if (isCompactPhone) 3 else 4,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
fun BuffIconPreview(buffType: Int, sizeDp: Int, accentColor: Color) {
  val visualRev = RiftVisualForgeState.visualRevision
  val bmp = remember(buffType, visualRev) { RiftSpriteBaker.getBuffIconBitmap(buffType).asImageBitmap() }
  Box(
    modifier =
      Modifier.size(sizeDp.dp)
        .clip(RoundedCornerShape(8.dp))
        .background(Color(0xFF030712))
        .border(1.5.dp, accentColor, RoundedCornerShape(8.dp)),
    contentAlignment = Alignment.Center,
  ) {
    Canvas(modifier = Modifier.size((sizeDp - 6).dp)) {
      drawImage(
        image = bmp,
        dstOffset = IntOffset.Zero,
        dstSize = IntSize(size.width.toInt(), size.height.toInt()),
      )
    }
  }
}

@Composable
fun RiftAnomalyIconPreview(riftId: String, sizeDp: Int, accentColor: Color) {
  val infiniteTransition = rememberInfiniteTransition(label = "codex_rift_preview_$riftId")
  val spinAngle by
    infiniteTransition.animateFloat(
      initialValue = 0f,
      targetValue = (2.0 * PI).toFloat(),
      animationSpec = infiniteRepeatable(tween(3600, easing = LinearEasing), RepeatMode.Restart),
      label = "rift_spin_angle",
    )
  val pulsePhase by
    infiniteTransition.animateFloat(
      initialValue = 0f,
      targetValue = 1f,
      animationSpec = infiniteRepeatable(tween(1600, easing = LinearEasing), RepeatMode.Restart),
      label = "rift_pulse_phase",
    )
  val linkPath = remember { Path() }

  Box(
    modifier =
      Modifier.size(sizeDp.dp)
        .clip(RoundedCornerShape(8.dp))
        .background(Color(0xFF030712))
        .border(1.5.dp, accentColor, RoundedCornerShape(8.dp)),
    contentAlignment = Alignment.Center,
  ) {
    Canvas(modifier = Modifier.fillMaxSize().padding(3.dp)) {
      val w = size.width
      val h = size.height
      val minDim = size.minDimension
      val cx = w * 0.5f
      val cy = h * 0.5f
      if (riftId == "phase_rift" || riftId == "phase_rift_pair") {
        val ax = w * 0.33f
        val ay = h * 0.35f
        val bx = w * 0.68f
        val by = h * 0.66f
        val midX = (ax + bx) * 0.5f + sin(spinAngle * 2f) * (minDim * 0.06f)
        val midY = (ay + by) * 0.5f - sin(spinAngle * 2f) * (minDim * 0.06f)
        linkPath.reset()
        linkPath.moveTo(ax, ay)
        linkPath.quadraticTo(midX, midY, bx, by)
        drawPath(
          linkPath,
          Brush.linearGradient(
            listOf(Color(0x662DD4BF), Color(0x6638BDF8)),
            Offset(ax, ay),
            Offset(bx, by),
          ),
          style = Stroke(width = 4.0.dp.toPx(), cap = StrokeCap.Round),
        )
        drawPath(
          linkPath,
          Brush.linearGradient(
            listOf(Color(0xFF2DD4BF), Color(0xFF38BDF8)),
            Offset(ax, ay),
            Offset(bx, by),
          ),
          style = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round),
        )
      }

      drawIntoCanvas { composeCanvas ->
        val nc = composeCanvas.nativeCanvas
        when (riftId) {
          "plasma_rift" ->
            RiftSpriteBaker.draw3dPlayfieldRift(
              nc,
              cx,
              cy,
              minDim * 0.38f,
              spinAngle,
              android.graphics.Color.rgb(239, 68, 68),
              android.graphics.Color.rgb(217, 70, 239),
              android.graphics.Color.rgb(255, 228, 230),
              darkVoidCenter = false,
              inwardSpiral = false,
              pulsePhaseOverride = pulsePhase,
            )
          "repulsor_rift" ->
            RiftSpriteBaker.draw3dPlayfieldRift(
              nc,
              cx,
              cy,
              minDim * 0.38f,
              -spinAngle * 1.15f,
              android.graphics.Color.rgb(56, 189, 248),
              android.graphics.Color.rgb(186, 230, 253),
              android.graphics.Color.rgb(255, 255, 255),
              darkVoidCenter = false,
              inwardSpiral = false,
              pulsePhaseOverride = pulsePhase,
            )
          "gravity_rift" ->
            RiftSpriteBaker.draw3dPlayfieldRift(
              nc,
              cx,
              cy,
              minDim * 0.38f,
              spinAngle * 1.35f,
              android.graphics.Color.rgb(168, 85, 247),
              android.graphics.Color.rgb(99, 102, 241),
              android.graphics.Color.rgb(233, 213, 255),
              darkVoidCenter = true,
              inwardSpiral = true,
              pulsePhaseOverride = pulsePhase,
            )
          "unstable_rift" ->
            RiftSpriteBaker.drawUnstableMergedRiftOnCanvas(
              nc,
              cx,
              cy,
              minDim * 0.39f,
              spinAngle * 2.1f,
              pulsePhase,
            )
          else -> {
            val nodeR = minDim * 0.22f
            RiftSpriteBaker.draw3dPlayfieldRift(
              nc,
              w * 0.33f,
              h * 0.35f,
              nodeR,
              spinAngle,
              android.graphics.Color.rgb(45, 212, 191),
              android.graphics.Color.rgb(56, 189, 248),
              android.graphics.Color.rgb(240, 253, 250),
              darkVoidCenter = true,
              inwardSpiral = false,
              pulsePhaseOverride = pulsePhase,
            )
            RiftSpriteBaker.draw3dPlayfieldRift(
              nc,
              w * 0.68f,
              h * 0.66f,
              nodeR,
              -spinAngle,
              android.graphics.Color.rgb(56, 189, 248),
              android.graphics.Color.rgb(168, 85, 247),
              android.graphics.Color.rgb(240, 253, 250),
              darkVoidCenter = true,
              inwardSpiral = false,
              pulsePhaseOverride = pulsePhase,
            )
          }
        }
      }
    }
  }
}
