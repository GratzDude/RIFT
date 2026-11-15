package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.RiftCodex
import com.example.engine.RiftSpriteBaker

@Composable
fun CodexModal(
  isNexusStreaming: Boolean = false,
  onOpenNexus: () -> Unit = {},
  onCloseModal: () -> Unit,
) {
  val context = LocalContext.current
  val isCompactPhone = LocalConfiguration.current.screenHeightDp < 480
  var selectedTab by remember { mutableIntStateOf(0) } // 0 = Supply Buffs, 1 = Playfield Rifts, 2 = Controller

  val outerPadding =
    if (isCompactPhone) {
      PaddingValues(start = 8.dp, end = 8.dp, top = 40.dp, bottom = 4.dp)
    } else {
      PaddingValues(horizontal = 20.dp, vertical = 10.dp)
    }

  Box(
    modifier =
      Modifier.fillMaxSize()
        .background(Color(0xB3000000))
        .padding(outerPadding)
        .testTag("codex_modal"),
    contentAlignment = Alignment.Center,
  ) {
    Surface(
      modifier =
        Modifier.fillMaxSize()
          .border(1.2.dp, Color(0x9900F5D4), RoundedCornerShape(12.dp)),
      color = Color(0xF2060D1A),
      shape = RoundedCornerShape(12.dp),
    ) {
      Column(
        modifier =
          Modifier.fillMaxSize()
            .padding(if (isCompactPhone) 8.dp else 14.dp),
        verticalArrangement = Arrangement.spacedBy(if (isCompactPhone) 5.dp else 9.dp),
      ) {
        // Unified Single-Row Header (32.dp compact phone height)
        Row(
          modifier =
            Modifier.fillMaxWidth()
              .then(if (isCompactPhone) Modifier.height(32.dp) else Modifier),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f, fill = false).padding(end = 6.dp)) {
            Text(
              text = if (isCompactPhone) "📖 RIFT CODEX" else "📖 RIFT CODEX: TACTICAL FIELD MANUAL",
              color = Color.White,
              fontSize = if (isCompactPhone) 13.5.sp else 16.5.sp,
              fontWeight = FontWeight.ExtraBold,
              fontFamily = FontFamily.SansSerif,
              letterSpacing = 0.6.sp,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            if (!isCompactPhone) {
              Text(
                text = "SUPPLY BUFF POOL • PLAYFIELD RIFT ANOMALIES • TWIN-STICK CONTROLLER & 10-SHIP PARITY",
                color = Color(0xFF00F5D4),
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }

          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            // Segmented Tab Switcher: Supply Buffs | Playfield Rifts | Controller
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
                label = "⚡ SUPPLY BUFFS",
                selected = selectedTab == 0,
                accentColor = Color(0xFF38BDF8),
                isCompactPhone = isCompactPhone,
                testTag = "codex_tab_supply_buffs",
                onClick = { selectedTab = 0 },
              )
              CodexTabPill(
                label = "🌀 PLAYFIELD RIFTS",
                selected = selectedTab == 1,
                accentColor = Color(0xFFA855F7),
                isCompactPhone = isCompactPhone,
                testTag = "codex_tab_playfield_rifts",
                onClick = { selectedTab = 1 },
              )
              CodexTabPill(
                label = "🎮 CONTROLLER",
                selected = selectedTab == 2,
                accentColor = Color(0xFF34D399),
                isCompactPhone = isCompactPhone,
                testTag = "codex_tab_controller",
                onClick = { selectedTab = 2 },
              )
            }

            SwirlingRiftHeaderPill(
              onClick = onOpenNexus,
              tag = "btn_codex_nexus_ai",
              isStreaming = isNexusStreaming,
            )

            Box(
              modifier =
                Modifier.defaultMinSize(minWidth = if (isCompactPhone) 68.dp else 80.dp)
                  .height(if (isCompactPhone) 28.dp else 36.dp)
                  .clip(RoundedCornerShape(8.dp))
                  .background(
                    Brush.verticalGradient(
                      listOf(Color(0xFF3D1422), Color(0xFF220A12))
                    )
                  )
                  .border(1.dp, Color(0xFFFF3B4E), RoundedCornerShape(8.dp))
                  .clickable(onClick = onCloseModal)
                  .padding(horizontal = if (isCompactPhone) 8.dp else 12.dp)
                  .testTag("codex_close_button"),
              contentAlignment = Alignment.Center,
            ) {
              Text(
                text = "✕ CLOSE",
                color = Color(0xFFFF6B7A),
                fontSize = if (isCompactPhone) 10.sp else 11.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.SansSerif,
              )
            }
          }
        }

        when (selectedTab) {
          0 -> {
            // TAB 0: SUPPLY BUFFS (75%+ vertical screen height for LazyColumn on phone landscape)
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(if (isCompactPhone) 5.dp else 8.dp),
            ) {
              CodexHighlightCard(
                modifier = Modifier.weight(1f),
                title = "🎯 SPECIAL BUFF SLOT (0⚡)",
                body = "Nova Ring, Shatter Cone, Seeker Torpedo, Holo Phantom, Stasis Barrier & Hyper Surge equip to the 4th button at 0⚡.",
                bgColor = Color(0xFF092318),
                borderColor = Color(0xFF10B981),
                bodyColor = Color(0xFFD1FAE5),
                isCompactPhone = isCompactPhone,
              )
              CodexHighlightCard(
                modifier = Modifier.weight(1f),
                title = "⚡ HULL ENHANCERS",
                body = "Bouncing Bullets (+1 bounce), Speed / Thrust / Reactor Overcharge (+8% up to 3/3), Multi-Shot (+25), Repel (+1), & Full ⚡.",
                bgColor = Color(0xFF261908),
                borderColor = Color(0xFFF59E0B),
                bodyColor = Color(0xFFFEF3C7),
                isCompactPhone = isCompactPhone,
              )
              CodexHighlightCard(
                modifier = Modifier.weight(1f),
                title = "⚖️ 100-PT HULL PARITY",
                body = "All 9 standard hulls (Viper..Eclipse) share the Universal Floor + 100 Pts. Rift Frame scales from 20 Pts to 100 Pts.",
                bgColor = Color(0xFF0D1E36),
                borderColor = Color(0xFF38BDF8),
                bodyColor = Color(0xFFE0F2FE),
                isCompactPhone = isCompactPhone,
              )
            }

            LazyColumn(
              modifier =
                Modifier.weight(1f)
                  .fillMaxWidth()
                  .background(Color(0xFF081426), RoundedCornerShape(10.dp))
                  .border(1.dp, Color(0xFF1B3454), RoundedCornerShape(10.dp))
                  .padding(if (isCompactPhone) 6.dp else 8.dp),
              verticalArrangement = Arrangement.spacedBy(if (isCompactPhone) 6.dp else 8.dp),
            ) {
              itemsIndexed(RiftCodex.POWERUP_CODEX) { _, entry ->
                val buffType = entry.buffTypeIndex
                val accentColor =
                  when (buffType) {
                    0 -> Color(0xFFF59E0B) // Nova Ring
                    1 -> Color(0xFFFB7185) // Shatter Cone
                    2 -> Color(0xFFEF4444) // Seeker Torpedo
                    3 -> Color(0xFF38BDF8) // Holo Phantom
                    4 -> Color(0xFF22D3EE) // Stasis Barrier
                    5 -> Color(0xFFA855F7) // Hyper Surge
                    6 -> Color(0xFF10B981) // Full Flux Charge
                    7 -> Color(0xFF60A5FA) // Repel Charge
                    8 -> Color(0xFFFACC15) // Shot-Limited Multi-Shot
                    9 -> Color(0xFFC084FC) // Permanent Bouncing Bullets
                    -10 -> Color(0xFF38BDF8) // Speed Overcharge
                    -11 -> Color(0xFFF59E0B) // Thrust Overcharge
                    -12 -> Color(0xFF10B981) // Reactor Overcharge
                    else -> Color(0xFF38BDF8)
                  }

                Row(
                  modifier =
                    Modifier.fillMaxWidth()
                      .background(Color(0xFF0E1F38), RoundedCornerShape(8.dp))
                      .border(1.dp, accentColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                      .padding(if (isCompactPhone) 8.dp else 10.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(if (isCompactPhone) 10.dp else 12.dp),
                ) {
                  BuffIconPreview(
                    buffType = buffType,
                    sizeDp = if (isCompactPhone) 46 else 54,
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
                          text = entry.title,
                          color = accentColor,
                          fontSize = if (isCompactPhone) 12.sp else 13.5.sp,
                          fontWeight = FontWeight.Bold,
                          fontFamily = FontFamily.SansSerif,
                        )
                        Box(
                          modifier =
                            Modifier.background(Color(0x22FFFFFF), RoundedCornerShape(4.dp))
                              .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                          Text(
                            text = entry.category,
                            color = Color(0xFFCBD5E1),
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                          )
                        }
                      }

                      Box(
                        modifier =
                          Modifier.background(Color(0x26FFD700), RoundedCornerShape(6.dp))
                            .border(1.dp, Color(0xFFFFD700), RoundedCornerShape(6.dp))
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                      ) {
                        Text(
                          text = "DROP ${entry.dropRatePercent}%",
                          color = Color(0xFFFFD700),
                          fontSize = if (isCompactPhone) 9.5.sp else 11.sp,
                          fontWeight = FontWeight.Bold,
                          fontFamily = FontFamily.Monospace,
                        )
                      }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                      text = entry.effectSummary,
                      color = Color.White,
                      fontSize = if (isCompactPhone) 10.5.sp else 11.5.sp,
                      fontFamily = FontFamily.SansSerif,
                    )

                    Spacer(modifier = Modifier.height(1.dp))

                    Text(
                      text = "TACTICAL: ${entry.tacticalNote}",
                      color = Color(0xFF94A3B8),
                      fontSize = if (isCompactPhone) 9.5.sp else 10.sp,
                      fontFamily = FontFamily.SansSerif,
                      lineHeight = if (isCompactPhone) 12.sp else 13.sp,
                    )
                  }
                }
              }
            }
          }

          1 -> {
            // TAB 1: PLAYFIELD RIFTS (OBSTACLES & SPATIAL ANOMALIES)
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(if (isCompactPhone) 5.dp else 8.dp),
            ) {
              CodexHighlightCard(
                modifier = Modifier.weight(1f),
                title = "⏱ 45S RIFT CYCLES",
                body = "Playfield Rifts relocate every 45s outside the Safe Zone and never spawn inside Stasis Breach Jails or Rift Control rings.",
                bgColor = Color(0xFF1B102E),
                borderColor = Color(0xFFA855F7),
                bodyColor = Color(0xFFE9D5FF),
                isCompactPhone = isCompactPhone,
              )
              CodexHighlightCard(
                modifier = Modifier.weight(1f),
                title = "⚫ GRAVITY & PHANTOMS",
                body = "Gravity Rifts absorb bullets (+6px), bombs (+18px), and Holo Phantoms (+14px) up to 420px. Phantoms react to all 4 Rifts.",
                bgColor = Color(0xFF0A2229),
                borderColor = Color(0xFF2DD4BF),
                bodyColor = Color(0xFFCCFBF1),
                isCompactPhone = isCompactPhone,
              )
              CodexHighlightCard(
                modifier = Modifier.weight(1f),
                title = "📡 RADAR SIGNATURES",
                body = "Live circular radar colors: Crimson = Plasma Rift, White = Repulsor Rift, Violet = Gravity Rift, Teal/Cyan = Phase Rifts.",
                bgColor = Color(0xFF241016),
                borderColor = Color(0xFFEF4444),
                bodyColor = Color(0xFFFECACA),
                isCompactPhone = isCompactPhone,
              )
            }

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
                          text = rift.title,
                          color = accentColor,
                          fontSize = if (isCompactPhone) 12.sp else 13.5.sp,
                          fontWeight = FontWeight.Bold,
                          fontFamily = FontFamily.SansSerif,
                        )
                        Box(
                          modifier =
                            Modifier.background(Color(0x22FFFFFF), RoundedCornerShape(4.dp))
                              .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                          Text(
                            text = rift.classification,
                            color = Color(0xFFCBD5E1),
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
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
                          text = rift.specBadge,
                          color = accentColor,
                          fontSize = if (isCompactPhone) 9.5.sp else 10.5.sp,
                          fontWeight = FontWeight.Bold,
                          fontFamily = FontFamily.Monospace,
                        )
                      }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                      text = rift.effectSummary,
                      color = Color.White,
                      fontSize = if (isCompactPhone) 10.5.sp else 11.5.sp,
                      fontFamily = FontFamily.SansSerif,
                    )

                    Spacer(modifier = Modifier.height(1.dp))

                    Text(
                      text = "TACTICAL: ${rift.tacticalNote}",
                      color = Color(0xFF94A3B8),
                      fontSize = if (isCompactPhone) 9.5.sp else 10.sp,
                      fontFamily = FontFamily.SansSerif,
                      lineHeight = if (isCompactPhone) 12.sp else 13.sp,
                    )
                  }
                }
              }
            }
          }

          else -> {
            // TAB 2: TWIN-STICK CONTROLLER SCHEMATIC & LIVE BUTTON REMAPPER
            Column(
              modifier =
                Modifier.weight(1f)
                  .fillMaxWidth()
                  .verticalScroll(rememberScrollState()),
              verticalArrangement = Arrangement.spacedBy(if (isCompactPhone) 6.dp else 8.dp),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(if (isCompactPhone) 5.dp else 8.dp),
              ) {
                CodexHighlightCard(
                  modifier = Modifier.weight(1f),
                  title = "🕹️ L-STICK: FLIGHT & DRIFT",
                  body = "360° Newtonian thrust (>80% ignites Afterburners; release coasts indefinitely). Fires along heading when R-Stick is neutral.",
                  bgColor = Color(0xFF082032),
                  borderColor = Color(0xFF00F5D4),
                  bodyColor = Color(0xFFE0F2FE),
                  isCompactPhone = isCompactPhone,
                )
                CodexHighlightCard(
                  modifier = Modifier.weight(1f),
                  title = "🎯 R-STICK: 360° TURRET",
                  body = "Deflect Right Stick at any time to aim 360° independently of your flight vector. Release to return aim to ship heading.",
                  bgColor = Color(0xFF1E1035),
                  borderColor = Color(0xFFC084FC),
                  bodyColor = Color(0xFFF3E8FF),
                  isCompactPhone = isCompactPhone,
                )
                CodexHighlightCard(
                  modifier = Modifier.weight(1f),
                  title = "⚡ LIVE BUTTON REMAPPER",
                  body = "Default: RT=Main, R1=Buff, LT=Brake, X=Secondary, A=Repel, Y=Multi-Shot. Tap any slot below to rebind live!",
                  bgColor = Color(0xFF092318),
                  borderColor = Color(0xFF34D399),
                  bodyColor = Color(0xFFD1FAE5),
                  isCompactPhone = isCompactPhone,
                )
              }

              ControllerDiagramAndMapperCard(context = context)
            }
          }
        }
      }
    }
  }
}

@Composable
private fun CodexTabPill(
  label: String,
  selected: Boolean,
  accentColor: Color,
  isCompactPhone: Boolean,
  testTag: String,
  onClick: () -> Unit,
) {
  Box(
    modifier =
      Modifier.height(if (isCompactPhone) 24.dp else 30.dp)
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
        .border(
          width = 1.dp,
          color = if (selected) accentColor else Color(0xFF1E293B),
          shape = RoundedCornerShape(6.dp),
        )
        .clickable(onClick = onClick)
        .padding(horizontal = if (isCompactPhone) 8.dp else 10.dp)
        .testTag(testTag),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      text = label,
      color = if (selected) Color.White else Color(0xFF94A3B8),
      fontSize = if (isCompactPhone) 9.5.sp else 10.5.sp,
      fontWeight = FontWeight.ExtraBold,
      fontFamily = FontFamily.Monospace,
      maxLines = 1,
    )
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
        ),
  ) {
    Column {
      Text(
        text = title,
        color = borderColor,
        fontSize = if (isCompactPhone) 9.5.sp else 10.5.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.SansSerif,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Text(
        text = body,
        color = bodyColor,
        fontSize = if (isCompactPhone) 8.5.sp else 9.5.sp,
        fontFamily = FontFamily.SansSerif,
        lineHeight = if (isCompactPhone) 11.sp else 12.5.sp,
        maxLines = if (isCompactPhone) 2 else 4,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
fun BuffIconPreview(buffType: Int, sizeDp: Int, accentColor: Color) {
  val bmp = remember(buffType) { RiftSpriteBaker.getBuffIconBitmap(buffType).asImageBitmap() }
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
  val bmp = remember(riftId) { RiftSpriteBaker.getRiftAnomalyIconBitmap(riftId).asImageBitmap() }
  Box(
    modifier =
      Modifier.size(sizeDp.dp)
        .clip(RoundedCornerShape(8.dp))
        .background(Color(0xFF030712))
        .border(1.5.dp, accentColor, RoundedCornerShape(8.dp)),
    contentAlignment = Alignment.Center,
  ) {
    Canvas(modifier = Modifier.size((sizeDp - 4).dp)) {
      drawImage(
        image = bmp,
        dstOffset = IntOffset.Zero,
        dstSize = IntSize(size.width.toInt(), size.height.toInt()),
      )
    }
  }
}
