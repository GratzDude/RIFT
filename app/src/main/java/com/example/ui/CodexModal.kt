package com.example.ui

import android.graphics.RectF
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
import com.example.engine.AnomalyType
import com.example.engine.PowerUpCodexEntry
import com.example.engine.RiftCodex
import com.example.engine.RiftSpriteBaker
import com.example.engine.RiftVisualForgeState
import com.example.engine.ShipSpec
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val CodexMono = FontFamily.Monospace
private val CodexSans = FontFamily.SansSerif

private fun cleanCodexCopy(raw: String): String =
  raw
    .replace("\u26A1", "Flux")
    .replace("\u2696\uFE0F", "")
    .replace("\u2696", "")
    .replace("\uFE0F", "")
    .replace(Regex("(?i)stasis\\s+field"), "Stasis")
    .trim()

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
              if (isCompactPhone) "STATS & CODEX"
              else "STATS & CODEX // TACTICAL FIELD MANUAL",
            color = Color.White,
            fontSize = if (isCompactPhone) 13.sp else 15.5.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = CodexSans,
            letterSpacing = 0.5.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          if (!isCompactPhone) {
            Text(
              text = "FLIGHT MANUAL • 6 COMPETITIVE MODES • SUPPLY BUFFS • SPACETIME RIFTS & RIFT STATION • 10 HULLS",
              color = Color(0xFF00F5D4),
              fontSize = 10.4.sp,
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
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        CodexTabPill(
          label = "[HOW TO PLAY]",
          glyphMode = 0,
          selected = selectedTab == 0,
          accentColor = Color(0xFF34D399),
          testTag = "codex_tab_howto",
        ) {
          selectedTab = 0
        }
        CodexTabPill(
          label = "[GAME MODES]",
          glyphMode = 1,
          selected = selectedTab == 1,
          accentColor = Color(0xFFFACC15),
          testTag = "codex_tab_modes",
        ) {
          selectedTab = 1
        }
        CodexTabPill(
          label = "[SUPPLY BUFFS]",
          glyphMode = 0,
          selected = selectedTab == 2,
          accentColor = Color(0xFF38BDF8),
          testTag = "codex_tab_buffs",
        ) {
          selectedTab = 2
        }
        CodexTabPill(
          label = "[RIFTS & OBSTACLES]",
          glyphMode = 1,
          selected = selectedTab == 3,
          accentColor = Color(0xFFA855F7),
          testTag = "codex_tab_rifts",
        ) {
          selectedTab = 3
        }
        CodexTabPill(
          label = "[SHIPS & MODULES]",
          glyphMode = 2,
          selected = selectedTab == 4,
          accentColor = Color(0xFF00F5D4),
          testTag = "codex_tab_ships",
        ) {
          selectedTab = 4
        }
      }
    }

    when (selectedTab) {
      0 -> {
        CodexHowToPlaySection(
          isCompactPhone = isCompactPhone,
          modifier = Modifier.weight(1f).fillMaxWidth(),
        )
      }

      1 -> {
        CodexGameModesSection(
          isCompactPhone = isCompactPhone,
          modifier = Modifier.weight(1f).fillMaxWidth(),
        )
      }

      2 -> {
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
                title = "SPECIAL BUFF SLOT & RIFT FRAME",
                body =
                  "Chakram (4-bounce saw-disc), Nova Ring, Shatter Cone, Seeker, Phantom, Barrier, Surge, Swarm, Tractor, Prox Lock & Lance equip at 0 Flux from Crates—or as Rift Frame Secondaries!",
                bgColor = Color(0xFF092318),
                borderColor = Color(0xFF10B981),
                bodyColor = Color(0xFFD1FAE5),
                isCompactPhone = isCompactPhone,
              )
              CodexHighlightCard(
                modifier = Modifier.weight(1f),
                title = "HULL ENHANCERS",
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
                  "All 10 hulls — including modular Rift Frame — are unlocked from 0 Resonance on an identical 100-Point competitive budget.",
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

      3 -> {
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
                title = "45S VOLUMETRIC RIFT CYCLES",
                body =
                  "Volumetric storm-cloud Playfield Rifts with centered abyssal throats and color-shifting branching lightning relocate every 45s outside the Safe Zone and never spawn inside Stasis Breach Jails or Rift Control rings.",
                bgColor = Color(0xFF1B102E),
                borderColor = Color(0xFFA855F7),
                bodyColor = Color(0xFFE9D5FF),
                isCompactPhone = isCompactPhone,
              )
              CodexHighlightCard(
                modifier = Modifier.weight(1f),
                title = "MAX GRAVITY & UNSTABLE RIFT",
                body =
                  "0.92x swept weapon absorption swells Gravity Rifts up to 440px; at >= 260px they drag & implode Asteroids/Stations (950/s crush + 360° Nova). Any 2 overlapping Rifts fuse into a 10.0s Unstable Rift (-25 Resonance outside Safe Zone).",
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

          item(key = "safe_zone_ds9_and_sector_obstacles") {
            SafeZoneAndSectorObstaclesCard(
              isCompactPhone = isCompactPhone,
              visualRev = visualRev,
            )
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
                sizeDp = if (isCompactPhone) 56 else 66,
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
                      fontSize = if (isCompactPhone) 12.2.sp else 13.8.sp,
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
                        fontSize = 9.0.sp,
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
                      fontSize = if (isCompactPhone) 9.5.sp else 10.2.sp,
                      fontWeight = FontWeight.Bold,
                      fontFamily = CodexMono,
                    )
                  }
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                  text = cleanCodexCopy(rift.effectSummary),
                  color = Color.White,
                  fontSize = if (isCompactPhone) 10.2.sp else 11.4.sp,
                  lineHeight = if (isCompactPhone) 13.5.sp else 15.sp,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                  text = "TACTICAL: ${cleanCodexCopy(rift.tacticalNote)}",
                  color = Color(0xFFCBD5E1),
                  fontSize = if (isCompactPhone) 9.8.sp else 10.5.sp,
                  lineHeight = if (isCompactPhone) 12.8.sp else 14.sp,
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

private data class CodexGuideTopic(
  val badge: String,
  val title: String,
  val summary: String,
  val detail: String,
  val accent: Color,
)

@Composable
private fun CodexHowToPlaySection(
  isCompactPhone: Boolean,
  modifier: Modifier = Modifier,
) {
  val topics =
    remember {
      listOf(
        CodexGuideTopic(
          badge = "FLIGHT & SHARED FLUX",
          title = "Inertial Twin-Stick Flight & Shared Flux Reactor",
          summary =
            "Left stick controls thrust and heading; right stick or touch controls primary fire, secondary ordnance, brake, and Special Crate buffs. Your ship's Flux ring around the circular radar powers BOTH your shields and your weapons.",
          detail =
            "Firing non-stop drains your Flux shield pool and leaves your hull exposed. Feather your shots, collect emerald ambient energy motes, or disengage briefly so your reactor recharges before re-entering a dogfight.",
          accent = Color(0xFF00F5D4),
        ),
        CodexGuideTopic(
          badge = "RESONANCE & HULLS",
          title = "Per-Ship Resonance, Best Ship & 100-Point Hull Parity",
          summary =
            "Every hull (#1 Viper through #10 modular Rift Frame) is unlocked immediately at 0 Resonance on an identical 100-Point stat and weapon budget. Each hull tracks its own Resonance, feeding into your overall Pilot Resonance.",
          detail =
            "Your highest-Resonance hull is highlighted in gold as your Best Ship in the Hangar, while your actively charged hull uses a cyan lock border. Eliminate rivals, survive skirmishes, and complete Daily Directives in the Rift Conduit to climb tiers.",
          accent = Color(0xFFFACC15),
        ),
        CodexGuideTopic(
          badge = "SAFE HARBOR & CRATES",
          title = "5x Rift Station Safe Harbor & Supply Crate Tech",
          summary =
            "Sector Convergence and Flux Core feature a central 5x ship-scale Rift Station Safe Zone. Weapons lock inside, and staying beyond 15 seconds (or firing just before entry) triggers a 15-second Safe-Zone lockout.",
          detail =
            "Shatter 2,400-Flux Crystal Asteroids or collapse swollen Gravity Rifts (>= 260px) to spawn metallic Supply Crates containing 0-Flux Special weapons (Chakram, Nova Ring, Swarm, Barrier, Lance) and permanent Hull Overcharge stacks.",
          accent = Color(0xFF38BDF8),
        ),
        CodexGuideTopic(
          badge = "SYNDICATES & SHARDS",
          title = "Syndicates, Squadrons, Matchmaking & Shard Store",
          summary =
            "Found or join player-chartered Syndicates, form up to 5-pilot Squadrons, and queue into Resonance-matched battles that launch within 15 seconds.",
          detail =
            "Open the Shard Store from the top Rift Shards readout (999,999 tester Shards active) to unlock hull trim styles, engine trails, warp effects, and modular Rift Frame slots A through E.",
          accent = Color(0xFFC084FC),
        ),
      )
    }

  LazyColumn(
    modifier =
      modifier
        .background(Color(0xFF081426), RoundedCornerShape(10.dp))
        .border(1.dp, Color(0xFF1B3454), RoundedCornerShape(10.dp))
        .padding(if (isCompactPhone) 7.dp else 10.dp)
        .testTag("codex_howto_list"),
    verticalArrangement = Arrangement.spacedBy(if (isCompactPhone) 7.dp else 9.dp),
  ) {
    items(topics, key = { it.title }) { topic ->
      Column(
        modifier =
          Modifier.fillMaxWidth()
            .background(Color(0xFF0E1F38), RoundedCornerShape(9.dp))
            .border(1.dp, topic.accent.copy(alpha = 0.62f), RoundedCornerShape(9.dp))
            .padding(if (isCompactPhone) 9.dp else 11.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = topic.title,
            color = Color.White,
            fontSize = if (isCompactPhone) 12.2.sp else 13.8.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = CodexSans,
            modifier = Modifier.weight(1f).padding(end = 8.dp),
          )
          Box(
            modifier =
              Modifier.background(topic.accent.copy(alpha = 0.18f), RoundedCornerShape(5.dp))
                .border(1.dp, topic.accent, RoundedCornerShape(5.dp))
                .padding(horizontal = 7.dp, vertical = 2.dp),
          ) {
            Text(
              text = topic.badge,
              color = topic.accent,
              fontSize = 9.6.sp,
              fontWeight = FontWeight.Black,
              fontFamily = CodexMono,
            )
          }
        }
        Text(
          text = topic.summary,
          color = Color(0xFFE2E8F0),
          fontSize = if (isCompactPhone) 10.6.sp else 11.6.sp,
          lineHeight = if (isCompactPhone) 14.0.sp else 15.4.sp,
        )
        Text(
          text = "TIP: ${topic.detail}",
          color = topic.accent.copy(alpha = 0.92f),
          fontSize = if (isCompactPhone) 10.0.sp else 10.8.sp,
          lineHeight = if (isCompactPhone) 13.2.sp else 14.4.sp,
        )
      }
    }
  }
}

@Composable
private fun CodexGameModesSection(
  isCompactPhone: Boolean,
  modifier: Modifier = Modifier,
) {
  val modes =
    remember {
      listOf(
        CodexGuideTopic(
          badge = "OPEN SECTOR • 60M CYCLE",
          title = "1. Sector Convergence",
          summary =
            "Always-live open sector combat anchored by the central Rift Station Safe Harbor. Drop in solo or with your Squadron, harvest Supply Crates, outmaneuver Playfield Rifts, and build per-ship and overall Resonance.",
          detail =
            "Anti-AFK Flux bleed applies outside combat if idle; dock inside Rift Station for up to 15 seconds to regroup safely.",
          accent = Color(0xFF00F5D4),
        ),
        CodexGuideTopic(
          badge = "OBJECTIVE • ORB CONTROL",
          title = "2. Flux Core",
          summary =
            "Capture and hold the golden Flux Core Singularity Orb at the center of the sector to generate continuous Resonance while broadcasting a high-visibility radar beacon to every rival pilot.",
          detail =
            "Entering the Rift Station Safe Zone or activating Cloak immediately ejects the Flux Core at 1,650 px/s—rely on escort teammates, Anti-Bleed Barriers, and Repulsor Rifts to survive.",
          accent = Color(0xFFFACC15),
        ),
        CodexGuideTopic(
          badge = "TEAM PVP • <= 15S QUEUE",
          title = "3. Rift Skirmish",
          summary =
            "Coordinated 5v5 Squadron dogfights matched against the closest-Resonance opponent squad within a strict 15-second matchmaking window.",
          detail =
            "Combine frontline brawlers (Goliath, Manta), flankers (Viper, Disruptor), and Eclipse Stasis bubbles to control Supply Crate spawns and win on team score.",
          accent = Color(0xFF38BDF8),
        ),
        CodexGuideTopic(
          badge = "TERRITORY • DUAL RINGS",
          title = "4. Rift Control",
          summary =
            "Contest and lock down the Alpha and Omega Rift Control rings to accumulate team dominance points. No central Safe Zone is active—commitment and rotation timing decide victory.",
          detail =
            "Playfield Rifts never spawn inside Alpha or Omega rings; use Proximity Locks, Stacked Barriers, and Shatter Cones to hold contested perimeter edges.",
          accent = Color(0xFFA855F7),
        ),
        CodexGuideTopic(
          badge = "RESCUE & LOCKDOWN",
          title = "5. Stasis Breach",
          summary =
            "Eliminated pilots are detained inside the rival team's West or East Brig Reactor. Crack the enemy Brig shield to trigger a prison break for all captured allies, or capture the entire opposing wing for an instant sweep.",
          detail =
            "Assign a fast striker to harass the enemy Brig Reactor whenever two or more wingmates are detained.",
          accent = Color(0xFFF43F5E),
        ),
        CodexGuideTopic(
          badge = "SURVIVAL & CUSTOM LAB",
          title = "6. Arena Elimination, Custom Games & Solo Tutorial",
          summary =
            "High-intensity round-based elimination inside the Storm Wall, plus full Custom Games (custom physics speed multipliers, crate rates, bot counts, and Rift toggles) and the interactive Solo Tutorial.",
          detail =
            "Launch Tutorial anytime from the Rift Hub or Helm Systems to test all 10 hulls at max Flux and practice combat maneuvers with +ADD BOT.",
          accent = Color(0xFF34D399),
        ),
      )
    }

  LazyColumn(
    modifier =
      modifier
        .background(Color(0xFF081426), RoundedCornerShape(10.dp))
        .border(1.dp, Color(0xFF1B3454), RoundedCornerShape(10.dp))
        .padding(if (isCompactPhone) 7.dp else 10.dp)
        .testTag("codex_modes_list"),
    verticalArrangement = Arrangement.spacedBy(if (isCompactPhone) 7.dp else 9.dp),
  ) {
    items(modes, key = { it.title }) { mode ->
      Column(
        modifier =
          Modifier.fillMaxWidth()
            .background(Color(0xFF0E1F38), RoundedCornerShape(9.dp))
            .border(1.dp, mode.accent.copy(alpha = 0.62f), RoundedCornerShape(9.dp))
            .padding(if (isCompactPhone) 9.dp else 11.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = mode.title,
            color = Color.White,
            fontSize = if (isCompactPhone) 12.2.sp else 13.8.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = CodexSans,
            modifier = Modifier.weight(1f).padding(end = 8.dp),
          )
          Box(
            modifier =
              Modifier.background(mode.accent.copy(alpha = 0.18f), RoundedCornerShape(5.dp))
                .border(1.dp, mode.accent, RoundedCornerShape(5.dp))
                .padding(horizontal = 7.dp, vertical = 2.dp),
          ) {
            Text(
              text = mode.badge,
              color = mode.accent,
              fontSize = 9.6.sp,
              fontWeight = FontWeight.Black,
              fontFamily = CodexMono,
            )
          }
        }
        Text(
          text = mode.summary,
          color = Color(0xFFE2E8F0),
          fontSize = if (isCompactPhone) 10.6.sp else 11.6.sp,
          lineHeight = if (isCompactPhone) 14.0.sp else 15.4.sp,
        )
        Text(
          text = "TACTICAL: ${mode.detail}",
          color = mode.accent.copy(alpha = 0.92f),
          fontSize = if (isCompactPhone) 10.0.sp else 10.8.sp,
          lineHeight = if (isCompactPhone) 13.2.sp else 14.4.sp,
        )
      }
    }
  }
}

/**
 * Dedicated Safe-Zone `Rift Station` & Sector Obstacles reference card inside the Playfield Rifts tab.
 * Renders the live `RiftSpriteBaker.safeZoneStationBitmap` (`5x` ship-scale `180px` Rift Station)
 * + revolving Ops Holo-Ring alongside exact Safe Harbor, `2,400` Flux Crystal Asteroid, and `10`-Hit Station Nova rules.
 */
@Composable
private fun SafeZoneAndSectorObstaclesCard(
  isCompactPhone: Boolean,
  visualRev: Int,
) {
  val headless = remember { isHeadlessGlEnvironment() }
  var continuousNanos by remember { androidx.compose.runtime.mutableLongStateOf(0L) }
  val startEpochNs = remember { System.nanoTime() }
  if (!headless) {
    androidx.compose.runtime.LaunchedEffect(Unit) {
      while (true) {
        androidx.compose.runtime.withFrameNanos { frameNs ->
          continuousNanos = frameNs
        }
      }
    }
  }
  val continuousSec =
    if (headless || continuousNanos == 0L) 5.4f else ((continuousNanos - startEpochNs) * 1e-9f)
  val stationBmp = remember(visualRev) { RiftSpriteBaker.safeZoneStationBitmap }
  val stationRect = remember { RectF() }
  val previewSizeDp = if (isCompactPhone) 62 else 74

  Row(
    modifier =
      Modifier.fillMaxWidth()
        .background(Color(0xFF0B1C34), RoundedCornerShape(9.dp))
        .border(
          1.2.dp,
          Brush.horizontalGradient(listOf(Color(0xFF00F5D4), Color(0xFFFACC15), Color(0xFF38BDF8))),
          RoundedCornerShape(9.dp),
        )
        .padding(if (isCompactPhone) 8.dp else 10.dp)
        .testTag("codex_safe_zone_ds9_card"),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(if (isCompactPhone) 10.dp else 12.dp),
  ) {
    Box(
      modifier =
        Modifier.size(previewSizeDp.dp)
          .clip(RoundedCornerShape(9.dp))
          .background(Color(0xFF030814))
          .border(1.4.dp, Color(0xFF00F5D4), RoundedCornerShape(9.dp)),
      contentAlignment = Alignment.Center,
    ) {
      Canvas(modifier = Modifier.fillMaxSize().padding(3.dp)) {
        val cx = size.width * 0.5f
        val cy = size.height * 0.5f
        val shieldR = size.minDimension * 0.47f
        val stationR = size.minDimension * 0.38f
        val timeSec = continuousSec

        // Feathered sanctuary aura + volumetric storm-cloud & branching-lightning perimeter (zero radial spoke lines)
        drawCircle(
          brush =
            Brush.radialGradient(
              colorStops =
                arrayOf(
                  0.00f to Color(0xFF030B1B).copy(alpha = 0.72f),
                  0.68f to Color(0xFF0F766E).copy(alpha = 0.28f),
                  0.92f to Color(0xFF00F5D4).copy(alpha = 0.38f),
                  1.00f to Color.Transparent,
                ),
              center = Offset(cx, cy),
              radius = shieldR * 1.06f,
            ),
          radius = shieldR * 1.06f,
          center = Offset(cx, cy),
        )
        drawIntoCanvas { composeCanvas ->
          val nc = composeCanvas.nativeCanvas
          RiftSpriteBaker.drawFeatheredStormRingClouds(
            canvas = nc,
            cx = cx,
            cy = cy,
            ringRadius = shieldR * 0.94f,
            puffBaseRadius = shieldR * 0.26f,
            puffCount = 10,
            timeSec = timeSec,
            spinRad = -timeSec * 0.35f,
            colorA = android.graphics.Color.rgb(15, 118, 110),
            colorB = android.graphics.Color.rgb(6, 182, 212),
            alphaScale = 0.72f,
          )
          nc.save()
          nc.translate(cx, cy)
          nc.rotate((timeSec * 40f) % 360f)
          stationRect.set(-stationR, -stationR, stationR, stationR)
          nc.drawBitmap(stationBmp, null, stationRect, null)
          nc.restore()
          RiftSpriteBaker.drawSafeZoneStationHoloRing(
            c = nc,
            cx = cx,
            cy = cy,
            stationRadius = stationR,
            timeSec = timeSec,
            isLockedOut = false,
            alphaScale = 1f,
          )
          RiftSpriteBaker.drawFractalPerimeterLightningArcs(
            canvas = nc,
            cx = cx,
            cy = cy,
            radius = shieldR,
            timeSec = timeSec,
            spinRad = -timeSec * 0.32f,
            arcCount = 5,
            deepColor = android.graphics.Color.rgb(15, 118, 110),
            midColor = android.graphics.Color.rgb(6, 182, 212),
            brightColor = android.graphics.Color.rgb(0, 245, 212),
            coreColor = android.graphics.Color.rgb(204, 251, 241),
            fadeAlpha = 0.90f,
            strokeWeight = 0.9f,
            seedOffset = 2.1f,
          )
        }
      }
    }

    Column(
      modifier = Modifier.weight(1f),
      verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          modifier = Modifier.weight(1f, fill = false),
        ) {
          Text(
            text = "RIFT STATION SAFE HARBOR & SECTOR OBSTACLES",
            color = Color(0xFF00F5D4),
            fontSize = if (isCompactPhone) 11.8.sp else 13.5.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = CodexSans,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Box(
            modifier =
              Modifier.background(Color(0x26FACC15), RoundedCornerShape(4.dp))
                .padding(horizontal = 5.dp, vertical = 2.dp)
          ) {
            Text(
              text = "STAR FORGE LIVE",
              color = Color(0xFFFACC15),
              fontSize = 8.8.sp,
              fontWeight = FontWeight.Bold,
              fontFamily = CodexMono,
            )
          }
        }
        Box(
          modifier =
            Modifier.background(Color(0x2200F5D4), RoundedCornerShape(6.dp))
              .border(1.dp, Color(0xFF00F5D4), RoundedCornerShape(6.dp))
              .padding(horizontal = 7.dp, vertical = 2.dp)
        ) {
          Text(
            text = "5X SHIP (180px) • 15S STAY",
            color = Color(0xFF00F5D4),
            fontSize = if (isCompactPhone) 9.2.sp else 10.0.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = CodexMono,
          )
        }
      }

      Text(
        text =
          "SAFE HARBOR (SECTOR CONVERGENCE & FLUX CORE ONLY): Anchored by a 5x ship-scale (180px radius) Rift Station with a revolving cyan/gold Ops Holo-Ring & radial stabilizer beams (editable in STAR FORGE). Weapons are locked inside; exceeding the 15s max stay or firing in combat triggers a 15s Safe-Zone lockout (ejects Flux Core at 1,650 px/s).",
        color = Color.White,
        fontSize = if (isCompactPhone) 10.0.sp else 11.2.sp,
        lineHeight = if (isCompactPhone) 13.2.sp else 14.8.sp,
      )
      Text(
        text =
          "OBSTACLES & STATION NOVAS: Crystal Asteroids have 2,400 Flux integrity, drop a guaranteed Supply Crate on shatter, and respawn in 20.0s. Orbital & Asteroid Stations charge a 10-segment ring and unleash a 16-bolt 360° Defense Nova every 10 hits (or when imploded by a >= 260px Gravity Rift).",
        color = Color(0xFFCBD5E1),
        fontSize = if (isCompactPhone) 9.8.sp else 10.5.sp,
        lineHeight = if (isCompactPhone) 12.8.sp else 14.0.sp,
      )
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
      12 -> Color(0xFF34D399)
      13 -> Color(0xFFFACC15)
      14 -> Color(0xFFA855F7)
      15 -> Color(0xFFF59E0B)
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
          fontSize = if (isCompactPhone) 10.8.sp else 11.8.sp,
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
            fontSize = 9.0.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = CodexMono,
          )
        }
      }
      Text(
        text = cleanCodexCopy(entry.effectSummary),
        color = Color.White,
        fontSize = if (isCompactPhone) 9.8.sp else 10.6.sp,
        lineHeight = if (isCompactPhone) 12.4.sp else 13.6.sp,
        maxLines = 3,
        overflow = TextOverflow.Ellipsis,
      )
      Text(
        text = "TACTICAL: ${cleanCodexCopy(entry.tacticalNote)}",
        color = Color(0xFFCBD5E1),
        fontSize = if (isCompactPhone) 9.4.sp else 10.0.sp,
        lineHeight = if (isCompactPhone) 11.8.sp else 13.0.sp,
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
            fontSize = if (isCompactPhone) 11.4.sp else 12.8.sp,
            fontWeight = FontWeight.Black,
            fontFamily = CodexSans,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Text(
            text = ship.role.uppercase(Locale.US),
            color = accentColor,
            fontSize = if (isCompactPhone) 9.6.sp else 10.4.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = CodexSans,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }
      Row(
        modifier =
          Modifier.background(Color(0xFF06281E), RoundedCornerShape(5.dp))
            .border(1.dp, Color(0xFF10B981), RoundedCornerShape(5.dp))
            .padding(horizontal = 6.dp, vertical = 2.5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        ResonanceCrestIcon(sizeDp = 11.dp)
        Text(
          text = "0 • 100 PTS",
          color = Color(0xFF34D399),
          fontSize = 9.4.sp,
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
        fontSize = if (isCompactPhone) 9.6.sp else 10.2.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = CodexMono,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Text(
        text = "SEC (${ship.secondaryPts}P): $secondaryDisplay",
        color = Color(0xFFFBBF24),
        fontSize = if (isCompactPhone) 9.6.sp else 10.2.sp,
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
      color = Color(0xFFCBD5E1),
      fontSize = 8.4.sp,
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
      fontSize = 8.8.sp,
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
      Modifier.height(29.dp)
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
        fontSize = 10.4.sp,
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
          horizontal = if (isCompactPhone) 7.dp else 10.dp,
          vertical = if (isCompactPhone) 5.dp else 7.dp,
        )
  ) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
      Text(
        text = title,
        color = borderColor,
        fontSize = if (isCompactPhone) 9.8.sp else 10.8.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = CodexMono,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Text(
        text = body,
        color = bodyColor,
        fontSize = if (isCompactPhone) 9.5.sp else 10.2.sp,
        lineHeight = if (isCompactPhone) 12.2.sp else 13.6.sp,
        maxLines = if (isCompactPhone) 4 else 5,
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
  val headless = remember { isHeadlessGlEnvironment() }
  var continuousNanos by remember { androidx.compose.runtime.mutableLongStateOf(0L) }
  val startEpochNs = remember { System.nanoTime() }
  if (!headless) {
    androidx.compose.runtime.LaunchedEffect(riftId) {
      while (true) {
        androidx.compose.runtime.withFrameNanos { frameNs ->
          continuousNanos = frameNs
        }
      }
    }
  }
  val timeSec =
    if (headless || continuousNanos == 0L) 1.45f else ((continuousNanos - startEpochNs) * 1e-9f)
  val spinAngle = timeSec * 1.65f
  val pulsePhase = timeSec * 0.55f

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
        // Soft feathered quantum mist corridor between paired Phase Rift nodes (zero solid wire lines)
        val mistPulse = 0.5f + 0.5f * sin(timeSec * 2.2f)
        drawCircle(
          brush =
            Brush.radialGradient(
              colors =
                listOf(
                  Color(0xFF00F5D4).copy(alpha = 0.22f + 0.12f * mistPulse),
                  Color(0xFF06B6D4).copy(alpha = 0.12f),
                  Color.Transparent,
                ),
              center = Offset(cx, cy),
              radius = minDim * 0.36f,
            ),
          radius = minDim * 0.36f,
          center = Offset(cx, cy),
        )
      }

      val baseRiftR = minDim * 0.38f

      drawIntoCanvas { composeCanvas ->
        val nc = composeCanvas.nativeCanvas
        when (riftId) {
          "plasma_rift" ->
            RiftSpriteBaker.draw3dPlayfieldRift(
              canvas = nc,
              type = AnomalyType.PLASMA_RIFT,
              cx = cx,
              cy = cy,
              radius = baseRiftR,
              spinAngle = spinAngle,
              pulsePhaseOverride = pulsePhase,
              parallaxOffsetX = 0f,
              parallaxOffsetY = 0f,
            )
          "repulsor_rift" ->
            RiftSpriteBaker.draw3dPlayfieldRift(
              canvas = nc,
              type = AnomalyType.REPULSOR_RIFT,
              cx = cx,
              cy = cy,
              radius = baseRiftR,
              spinAngle = spinAngle,
              pulsePhaseOverride = pulsePhase,
              parallaxOffsetX = 0f,
              parallaxOffsetY = 0f,
            )
          "gravity_rift" ->
            RiftSpriteBaker.draw3dPlayfieldRift(
              canvas = nc,
              type = AnomalyType.GRAVITY_RIFT,
              cx = cx,
              cy = cy,
              radius = baseRiftR,
              spinAngle = spinAngle,
              absorbedCount = 3,
              pulsePhaseOverride = pulsePhase,
              parallaxOffsetX = 0f,
              parallaxOffsetY = 0f,
            )
          "unstable_rift" ->
            RiftSpriteBaker.drawUnstableMergedRiftOnCanvas(
              canvas = nc,
              cx = cx,
              cy = cy,
              r = minDim * 0.39f,
              spinAngle = spinAngle * 0.45f,
              pulsePhase = pulsePhase,
              parallaxOffsetX = 0f,
              parallaxOffsetY = 0f,
            )
          else -> {
            val nodeR = minDim * 0.24f
            RiftSpriteBaker.draw3dPlayfieldRift(
              canvas = nc,
              type = AnomalyType.PHASE_RIFT_A,
              cx = w * 0.34f,
              cy = h * 0.36f,
              radius = nodeR,
              spinAngle = spinAngle,
              pulsePhaseOverride = pulsePhase,
              parallaxOffsetX = 0f,
              parallaxOffsetY = 0f,
            )
            RiftSpriteBaker.draw3dPlayfieldRift(
              canvas = nc,
              type = AnomalyType.PHASE_RIFT_B,
              cx = w * 0.67f,
              cy = h * 0.65f,
              radius = nodeR,
              spinAngle = spinAngle,
              pulsePhaseOverride = pulsePhase,
              parallaxOffsetX = 0f,
              parallaxOffsetY = 0f,
            )
          }
        }
      }
    }
  }
}
