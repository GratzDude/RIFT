package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
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
  var selectedTab by remember { mutableIntStateOf(0) } // 0 = Supply Buffs, 1 = Playfield Rifts

  Box(
    modifier =
      Modifier.fillMaxSize()
        .background(Color(0xB3000000))
        .padding(horizontal = 20.dp, vertical = 10.dp)
        .testTag("codex_modal"),
    contentAlignment = Alignment.Center,
  ) {
    Surface(
      modifier =
        Modifier.fillMaxSize()
          .border(1.dp, Color(0x6600F0FF), RoundedCornerShape(16.dp)),
      color = Color(0xE60A0F18),
      shape = RoundedCornerShape(16.dp),
    ) {
      Column(
        modifier =
          Modifier.fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp),
      ) {
        // Header + Tab Switcher + NEXUS & Close Actions
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column {
            Text(
              text = "📖 RIFT CODEX: TACTICAL FIELD MANUAL",
              color = Color.White,
              fontSize = 16.5.sp,
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.SansSerif,
              letterSpacing = 0.8.sp,
            )
            Text(
              text = "SUPPLY BUFF POOL • PLAYFIELD RIFT ANOMALIES • 100-PT SHIP PARITY",
              color = Color(0xFF00F0FF),
              fontSize = 10.sp,
              fontWeight = FontWeight.Medium,
              fontFamily = FontFamily.Monospace,
            )
          }

          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            // Segmented Tab Switcher: Supply Buffs | Playfield Rifts
            Row(
              modifier =
                Modifier.background(Color(0xFF060C16), RoundedCornerShape(9.dp))
                  .border(1.dp, Color(0xFF1E3A5F), RoundedCornerShape(9.dp))
                  .padding(3.dp),
              horizontalArrangement = Arrangement.spacedBy(4.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              CodexTabPill(
                label = "⚡ SUPPLY BUFFS",
                selected = selectedTab == 0,
                accentColor = Color(0xFF38BDF8),
                testTag = "codex_tab_supply_buffs",
                onClick = { selectedTab = 0 },
              )
              CodexTabPill(
                label = "🌀 PLAYFIELD RIFTS",
                selected = selectedTab == 1,
                accentColor = Color(0xFFA855F7),
                testTag = "codex_tab_playfield_rifts",
                onClick = { selectedTab = 1 },
              )
            }

            SwirlingRiftHeaderPill(
              onClick = onOpenNexus,
              tag = "btn_codex_nexus_ai",
              isStreaming = isNexusStreaming,
            )

            Button(
              onClick = onCloseModal,
              colors =
                ButtonDefaults.buttonColors(
                  containerColor = Color(0xFF28111B),
                  contentColor = Color(0xFFFF3B4E),
                ),
              border = BorderStroke(1.dp, Color(0xFFFF3B4E)),
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier.height(36.dp).testTag("codex_close_button"),
            ) {
              Text(
                text = "✕ CLOSE",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.SansSerif,
              )
            }
          }
        }

        if (selectedTab == 0) {
          // TAB 0: SUPPLY BUFFS
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            CodexHighlightCard(
              modifier = Modifier.weight(1f),
              title = "🎯 BUCKET A: SPECIAL BUFF SLOT (0⚡)",
              body = "Nova Ring, Shatter Cone, Seeker Torpedo, Holo Phantom, Stasis Barrier & Hyper Surge equip to the 4th button at 0⚡ Flux cost. Same-type stacks; new type overwrites.",
              bgColor = Color(0xFF092318),
              borderColor = Color(0xFF10B981),
              bodyColor = Color(0xFFD1FAE5),
            )
            CodexHighlightCard(
              modifier = Modifier.weight(1f),
              title = "⚡ BUCKET B: HULL UPGRADES & ENHANCERS",
              body = "Bouncing Bullets (+1 bounce until death), Speed / Thrust / Reactor Overcharge (+8% up to 3/3 until death), Multi-Shot (+25 shots), Repel (+1), & Full Flux.",
              bgColor = Color(0xFF261908),
              borderColor = Color(0xFFF59E0B),
              bodyColor = Color(0xFFFEF3C7),
            )
            CodexHighlightCard(
              modifier = Modifier.weight(1f),
              title = "⚖️ 100-POINT HULL PARITY",
              body = "Standard hulls share the Universal Baseline Floor + 100 Pts. Rift Frame grows from 20 Pts (Lv 20) to 100 Pts (Lv 100).",
              bgColor = Color(0xFF0D1E36),
              borderColor = Color(0xFF38BDF8),
              bodyColor = Color(0xFFE0F2FE),
            )
          }

          LazyColumn(
            modifier =
              Modifier.fillMaxSize()
                .background(Color(0xFF081426), RoundedCornerShape(10.dp))
                .border(1.dp, Color(0xFF1B3454), RoundedCornerShape(10.dp))
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
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
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
              ) {
                BuffIconPreview(buffType = buffType, sizeDp = 54, accentColor = accentColor)

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
                        fontSize = 13.5.sp,
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
                          fontSize = 9.sp,
                          fontWeight = FontWeight.Bold,
                          fontFamily = FontFamily.Monospace,
                        )
                      }
                    }

                    Box(
                      modifier =
                        Modifier.background(Color(0x26FFD700), RoundedCornerShape(8.dp))
                          .border(1.dp, Color(0xFFFFD700), RoundedCornerShape(8.dp))
                          .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                      Text(
                        text = "DROP ${entry.dropRatePercent}%",
                        color = Color(0xFFFFD700),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                      )
                    }
                  }

                  Spacer(modifier = Modifier.height(3.dp))

                  Text(
                    text = entry.effectSummary,
                    color = Color.White,
                    fontSize = 11.5.sp,
                    fontFamily = FontFamily.SansSerif,
                  )

                  Spacer(modifier = Modifier.height(2.dp))

                  Text(
                    text = "TACTICAL ADVICE: ${entry.tacticalNote}",
                    color = Color(0xFF94A3B8),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.SansSerif,
                    lineHeight = 13.sp,
                  )
                }
              }
            }
          }
        } else {
          // TAB 1: PLAYFIELD RIFTS (OBSTACLES & SPATIAL ANOMALIES)
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            CodexHighlightCard(
              modifier = Modifier.weight(1f),
              title = "⏱ 45-SECOND DYNAMIC RIFT CYCLES",
              body = "Playfield Rifts remain active for 45 seconds before collapsing in a particle burst and relocating to fresh open-space coordinates outside the Safe Zone.",
              bgColor = Color(0xFF1B102E),
              borderColor = Color(0xFFA855F7),
              bodyColor = Color(0xFFE9D5FF),
            )
            CodexHighlightCard(
              modifier = Modifier.weight(1f),
              title = "📐 5x HULL SCALE & ORDNANCE PHYSICS",
              body = "Each Rift spans a 180px physical core (5x ship scale) and directly interacts with both Newtonian ship trajectories and live bullets/warheads.",
              bgColor = Color(0xFF0A2229),
              borderColor = Color(0xFF2DD4BF),
              bodyColor = Color(0xFFCCFBF1),
            )
            CodexHighlightCard(
              modifier = Modifier.weight(1f),
              title = "📡 MINIMAP RADAR COLOR SIGNATURES",
              body = "Tracked live on your circular radar: Crimson = Plasma Rift, White = Repulsor Rift, Violet = Gravity Rift, Teal & Cyan = Paired Phase Rifts.",
              bgColor = Color(0xFF241016),
              borderColor = Color(0xFFEF4444),
              bodyColor = Color(0xFFFECACA),
            )
          }

          LazyColumn(
            modifier =
              Modifier.fillMaxSize()
                .background(Color(0xFF081426), RoundedCornerShape(10.dp))
                .border(1.dp, Color(0xFF1B3454), RoundedCornerShape(10.dp))
                .padding(8.dp)
                .testTag("codex_playfield_rifts_list"),
            verticalArrangement = Arrangement.spacedBy(8.dp),
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
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
              ) {
                RiftAnomalyIconPreview(riftId = rift.id, sizeDp = 56, accentColor = accentColor)

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
                        fontSize = 13.5.sp,
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
                          fontSize = 9.sp,
                          fontWeight = FontWeight.Bold,
                          fontFamily = FontFamily.Monospace,
                        )
                      }
                    }

                    Box(
                      modifier =
                        Modifier.background(accentColor.copy(alpha = 0.16f), RoundedCornerShape(8.dp))
                          .border(1.dp, accentColor, RoundedCornerShape(8.dp))
                          .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                      Text(
                        text = rift.specBadge,
                        color = accentColor,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                      )
                    }
                  }

                  Spacer(modifier = Modifier.height(3.dp))

                  Text(
                    text = rift.effectSummary,
                    color = Color.White,
                    fontSize = 11.5.sp,
                    fontFamily = FontFamily.SansSerif,
                  )

                  Spacer(modifier = Modifier.height(2.dp))

                  Text(
                    text = "TACTICAL ADVICE: ${rift.tacticalNote}",
                    color = Color(0xFF94A3B8),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.SansSerif,
                    lineHeight = 13.sp,
                  )
                }
              }
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
  testTag: String,
  onClick: () -> Unit,
) {
  Box(
    modifier =
      Modifier.clip(RoundedCornerShape(6.dp))
        .background(if (selected) accentColor.copy(alpha = 0.22f) else Color.Transparent)
        .border(
          width = if (selected) 1.dp else 0.dp,
          color = if (selected) accentColor else Color.Transparent,
          shape = RoundedCornerShape(6.dp),
        )
        .clickable(onClick = onClick)
        .padding(horizontal = 10.dp, vertical = 6.dp)
        .testTag(testTag),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      text = label,
      color = if (selected) Color.White else Color(0xFF94A3B8),
      fontSize = 10.5.sp,
      fontWeight = FontWeight.ExtraBold,
      fontFamily = FontFamily.Monospace,
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
) {
  Box(
    modifier =
      modifier
        .background(bgColor, RoundedCornerShape(8.dp))
        .border(1.dp, borderColor, RoundedCornerShape(8.dp))
        .padding(horizontal = 10.dp, vertical = 7.dp),
  ) {
    Column {
      Text(
        text = title,
        color = borderColor,
        fontSize = 10.5.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.SansSerif,
      )
      Text(
        text = body,
        color = bodyColor,
        fontSize = 9.5.sp,
        fontFamily = FontFamily.SansSerif,
        lineHeight = 12.5.sp,
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
