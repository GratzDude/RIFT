package com.example.ui

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun SettingsModal(
  fps: Int,
  sfxEnabled: Boolean,
  cameraZoom: Float,
  playfieldTextScale: Float,
  isGoogleAuthenticated: Boolean,
  onToggleSfx: () -> Unit,
  onChangeCameraZoom: (Float) -> Unit,
  onChangePlayfieldTextScale: (Float) -> Unit,
  onRequestGoogleSignIn: () -> Unit,
  onRequestSignOut: () -> Unit,
  onCloseModal: () -> Unit,
  onOpenChangelog: () -> Unit = {},
) {
  val scrollState = rememberScrollState()

  Box(
    modifier =
      Modifier.fillMaxSize()
        .background(Color(0xB3000000))
        .padding(horizontal = 24.dp, vertical = 12.dp)
        .testTag("settings_modal"),
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
            .padding(16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        // Header Row
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
              text = "⚙️ ARENA COCKPIT SETTINGS",
              color = Color.White,
              fontSize = 18.sp,
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.SansSerif,
              letterSpacing = 1.2.sp,
            )
            Text(
              text = "AUDIO SFX • CAMERA FOV • PLAYFIELD TEXT SCALE • CLOUD IDENTITY",
              color = Color(0xFF00F0FF),
              fontSize = 11.sp,
              fontWeight = FontWeight.Medium,
              fontFamily = FontFamily.Monospace,
            )
          }

          Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Button(
              onClick = onOpenChangelog,
              colors =
                ButtonDefaults.buttonColors(
                  containerColor = Color(0xFF0D2036),
                  contentColor = Color(0xFF00F5D4),
                ),
              border = BorderStroke(1.dp, Color(0xFF00F5D4)),
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier.height(36.dp).testTag("settings_btn_open_changelog"),
            ) {
              Text(
                text = "📜 FULL VERSION CHANGELOG",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.SansSerif,
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
              modifier = Modifier.height(36.dp).testTag("settings_close_button"),
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

        // Section 1: Audio & Cloud Identity
        Row(
          modifier =
            Modifier.fillMaxWidth()
              .background(Color(0xFF0D1E36), RoundedCornerShape(10.dp))
              .border(1.dp, Color(0xFF1B3454), RoundedCornerShape(10.dp))
              .padding(12.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "SYNTHESIZED AUDIO & ENGINE",
              color = Color(0xFF00F0FF),
              fontSize = 13.sp,
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.SansSerif,
            )
            Text(
              text = "Procedural audio synthesizer ($fps FPS dynamic simulation)",
              color = Color(0xFF8A99AD),
              fontSize = 11.sp,
              fontFamily = FontFamily.SansSerif,
            )
          }

          Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
              onClick = onToggleSfx,
              colors =
                ButtonDefaults.buttonColors(
                  containerColor = if (sfxEnabled) Color(0xFF0E2B22) else Color(0xFF2B141C),
                  contentColor = if (sfxEnabled) Color(0xFF00E676) else Color(0xFFFF3B4E),
                ),
              border =
                BorderStroke(
                  1.dp,
                  if (sfxEnabled) Color(0xFF00E676) else Color(0xFFFF3B4E),
                ),
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier.height(36.dp).testTag("settings_sfx_toggle_button"),
            ) {
              Text(
                text = if (sfxEnabled) "🔊 SFX: ON" else "🔇 SFX: MUTE",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.SansSerif,
              )
            }

            if (isGoogleAuthenticated) {
              Button(
                onClick = onRequestSignOut,
                colors =
                  ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1A283D),
                    contentColor = Color(0xFF8A99AD),
                  ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.height(36.dp),
              ) {
                Text(
                  text = "SIGN OUT CLOUD",
                  fontSize = 10.5.sp,
                  fontWeight = FontWeight.Bold,
                  fontFamily = FontFamily.SansSerif,
                )
              }
            } else {
              Button(
                onClick = onRequestGoogleSignIn,
                colors =
                  ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF12332B),
                    contentColor = Color(0xFF00F0FF),
                  ),
                border = BorderStroke(1.dp, Color(0xFF00F0FF)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.height(36.dp),
              ) {
                Text(
                  text = "☁️ LINK GOOGLE CLOUD",
                  fontSize = 10.5.sp,
                  fontWeight = FontWeight.Bold,
                  fontFamily = FontFamily.SansSerif,
                )
              }
            }
          }
        }

        // Section 2: Playfield Text Scale Slider (0.75x to 2.0x)
        Column(
          modifier =
            Modifier.fillMaxWidth()
              .background(Color(0xFF0D1E36), RoundedCornerShape(10.dp))
              .border(1.dp, Color(0xFF1B3454), RoundedCornerShape(10.dp))
              .padding(12.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column {
              val formattedScale = String.format(Locale.US, "%.2fx", playfieldTextScale)
              Text(
                text = "🔤 PLAYFIELD TEXT SIZE: $formattedScale",
                color = Color(0xFF00F0FF),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.SansSerif,
              )
              Text(
                text = "Scales bounty and callsign labels above and below ships on the space playfield",
                color = Color(0xFF8A99AD),
                fontSize = 11.sp,
                fontFamily = FontFamily.SansSerif,
              )
            }

            // Quick Preset Chips
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { preset ->
                val isSelected = (playfieldTextScale - preset).let { it >= -0.05f && it <= 0.05f }
                Box(
                  modifier =
                    Modifier.clip(RoundedCornerShape(6.dp))
                      .background(if (isSelected) Color(0xFF00F0FF) else Color(0xFF132642))
                      .border(
                        1.dp,
                        if (isSelected) Color(0xFF00F0FF) else Color(0x3300F0FF),
                        RoundedCornerShape(6.dp),
                      )
                      .clickable { onChangePlayfieldTextScale(preset) }
                      .padding(horizontal = 7.dp, vertical = 4.dp),
                ) {
                  Text(
                    text = "${preset}x",
                    color = if (isSelected) Color(0xFF030712) else Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                  )
                }
              }
            }
          }

          Slider(
            value = playfieldTextScale,
            onValueChange = { onChangePlayfieldTextScale(((it * 20f).roundToInt() / 20f).coerceIn(0.75f, 2.0f)) },
            valueRange = 0.75f..2.0f,
            colors =
              SliderDefaults.colors(
                thumbColor = Color(0xFF00F0FF),
                activeTrackColor = Color(0xFF00F0FF),
                inactiveTrackColor = Color(0xFF132642),
              ),
            modifier = Modifier.fillMaxWidth().testTag("settings_text_scale_slider"),
          )

          // Live Text Preview Box
          Box(
            modifier =
              Modifier.fillMaxWidth()
                .background(Color(0xFF060B14), RoundedCornerShape(6.dp))
                .border(1.dp, Color(0x2200F0FF), RoundedCornerShape(6.dp))
                .padding(horizontal = 14.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center,
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Text(
                text = "[GUEST] VORTEX ACE • ★ 320",
                color = Color(0xFF38BDF8),
                fontSize = (13f * playfieldTextScale).sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.SansSerif,
              )
            }
          }
        }

        // Section 3: Camera Zoom / FOV Slider (0.5x to 1.5x)
        Column(
          modifier =
            Modifier.fillMaxWidth()
              .background(Color(0xFF0D1E36), RoundedCornerShape(10.dp))
              .border(1.dp, Color(0xFF1B3454), RoundedCornerShape(10.dp))
              .padding(12.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column {
              val formattedZoom = String.format(Locale.US, "%.2fx", cameraZoom)
              val zoomMode =
                when {
                  cameraZoom <= 0.65f -> "TACTICAL OVERVIEW"
                  cameraZoom >= 1.25f -> "CINEMATIC CLOSE-UP"
                  else -> "STANDARD ARENA VIEW"
                }
              Text(
                text = "🔍 CAMERA ZOOM / FOV: $formattedZoom ($zoomMode)",
                color = Color(0xFF00F0FF),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.SansSerif,
              )
              Text(
                text = "0.5x Wide tactical field to 1.5x tight dogfighting close-up",
                color = Color(0xFF8A99AD),
                fontSize = 11.sp,
                fontFamily = FontFamily.SansSerif,
              )
            }

            // Quick Preset Chips
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f).forEach { preset ->
                val isSelected = (cameraZoom - preset).let { it >= -0.05f && it <= 0.05f }
                Box(
                  modifier =
                    Modifier.clip(RoundedCornerShape(6.dp))
                      .background(if (isSelected) Color(0xFF00F0FF) else Color(0xFF132642))
                      .border(
                        1.dp,
                        if (isSelected) Color(0xFF00F0FF) else Color(0x3300F0FF),
                        RoundedCornerShape(6.dp),
                      )
                      .clickable { onChangeCameraZoom(preset) }
                      .padding(horizontal = 7.dp, vertical = 4.dp),
                ) {
                  Text(
                    text = "${preset}x",
                    color = if (isSelected) Color(0xFF030712) else Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                  )
                }
              }
            }
          }

          Slider(
            value = cameraZoom,
            onValueChange = { onChangeCameraZoom(((it * 20f).roundToInt() / 20f).coerceIn(0.5f, 1.5f)) },
            valueRange = 0.5f..1.5f,
            colors =
              SliderDefaults.colors(
                thumbColor = Color(0xFF00F0FF),
                activeTrackColor = Color(0xFF00F0FF),
                inactiveTrackColor = Color(0xFF132642),
              ),
            modifier = Modifier.fillMaxWidth().testTag("settings_camera_zoom_slider"),
          )
        }
      }
    }
  }
}
