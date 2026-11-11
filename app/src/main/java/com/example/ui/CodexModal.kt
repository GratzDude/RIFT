package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
  onCloseModal: () -> Unit,
) {
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
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        // Header
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column {
            Text(
              text = "📖 RIFT CODEX: POWER-UPS & ARENA DOCTRINE",
              color = Color.White,
              fontSize = 18.sp,
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.SansSerif,
              letterSpacing = 1.2.sp,
            )
            Text(
              text = "SPINNING EMERALD CRATES ('GREENS') • PERMANENT LIFE STACKS • ARENA RULES",
              color = Color(0xFF00F0FF),
              fontSize = 11.sp,
              fontWeight = FontWeight.Medium,
              fontFamily = FontFamily.Monospace,
            )
          }

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

        // Persistent Buffs & 30s Munitions Rule Highlights
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Box(
            modifier =
              Modifier.weight(1f)
                .background(Color(0xFF092318), RoundedCornerShape(8.dp))
                .border(1.dp, Color(0xFF10B981), RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 7.dp),
          ) {
            Column {
              Text(
                text = "🛡️ PERMANENT LIFE BUFFS",
                color = Color(0xFF10B981),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.SansSerif,
              )
              Text(
                text = "Collected powerups stay active until your ship is destroyed. Active buffs with stack counts appear under the top HUD bar.",
                color = Color(0xFFD1FAE5),
                fontSize = 10.sp,
                fontFamily = FontFamily.SansSerif,
                lineHeight = 13.sp,
              )
            }
          }

          Box(
            modifier =
              Modifier.weight(1f)
                .background(Color(0xFF261908), RoundedCornerShape(8.dp))
                .border(1.dp, Color(0xFFF59E0B), RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 7.dp),
          ) {
            Column {
              Text(
                text = "💣 30-SECOND MUNITION EXPIRATION",
                color = Color(0xFFF59E0B),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.SansSerif,
              )
              Text(
                text = "Mines & unexploded bombs linger for 30s. Upon timer expiration, they detonate with 1.5x blast radius at 50% damage.",
                color = Color(0xFFFEF3C7),
                fontSize = 10.sp,
                fontFamily = FontFamily.SansSerif,
                lineHeight = 13.sp,
              )
            }
          }

          Box(
            modifier =
              Modifier.weight(1f)
                .background(Color(0xFF0D1E36), RoundedCornerShape(8.dp))
                .border(1.dp, Color(0xFF38BDF8), RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 7.dp),
          ) {
            Column {
              Text(
                text = "🏰 SAFE HAVEN & PHASE SHIELD",
                color = Color(0xFF38BDF8),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.SansSerif,
              )
              Text(
                text = "Safe Haven strictly disarms weapons. Exiting grants 3.0s 75% defense Phase Shield (stripped upon firing).",
                color = Color(0xFFE0F2FE),
                fontSize = 10.sp,
                fontFamily = FontFamily.SansSerif,
                lineHeight = 13.sp,
              )
            }
          }
        }

        // Scrollable Power-Up Cards List
        LazyColumn(
          modifier =
            Modifier.fillMaxSize()
              .background(Color(0xFF081426), RoundedCornerShape(10.dp))
              .border(1.dp, Color(0xFF1B3454), RoundedCornerShape(10.dp))
              .padding(8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          itemsIndexed(RiftCodex.POWERUP_CODEX) { idx, entry ->
            val buffType = idx % 8
            val accentColor =
              when (buffType) {
                0 -> Color(0xFF10B981) // Full Charge
                1 -> Color(0xFF38BDF8) // Recharge
                2 -> Color(0xFFF59E0B) // Speed
                3 -> Color(0xFFFACC15) // Multifire
                4 -> Color(0xFFA855F7) // Ricochet
                5 -> Color(0xFFF97316) // Burst Special Weapon
                6 -> Color(0xFFFB7185) // Shotgun Special Weapon
                else -> Color(0xFFEF4444) // Lock-On Bouncing Missile
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
              // Playfield Icon Graphic Preview
              BuffIconPreview(buffType = buffType, sizeDp = 54, accentColor = accentColor)

              // Content Details
              Column(modifier = Modifier.weight(1f)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
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

                  // Drop Rate Badge
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
      }
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
