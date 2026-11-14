package com.example.ui

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.view.KeyEvent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.RiftCalibrationState
import java.util.Locale
import kotlin.math.roundToInt

private data class ControllerActionSlotInfo(
  val slotId: String,
  val title: String,
  val subtitle: String,
  val accentColor: Color,
)

private val REMAPPABLE_ACTION_SLOTS =
  listOf(
    ControllerActionSlotInfo(
      slotId = "PRIMARY_FIRE",
      title = "MAIN ATTACK",
      subtitle = "Hold to fire primary weapon (aims via R-Stick or L-Stick heading)",
      accentColor = Color(0xFFEF4444),
    ),
    ControllerActionSlotInfo(
      slotId = "SPECIAL_BUFF",
      title = "BUFF ATTACK (4TH SLOT)",
      subtitle = "Fires equipped 0⚡ Special Buff (Nova, Shatter, Seeker, Stasis, etc.)",
      accentColor = Color(0xFF34D399),
    ),
    ControllerActionSlotInfo(
      slotId = "RETRO_BRAKE",
      title = "MANUAL RETRO-BRAKE",
      subtitle = "Hold to damp Newtonian drift (releasing Left Stick coasts indefinitely)",
      accentColor = Color(0xFF38BDF8),
    ),
    ControllerActionSlotInfo(
      slotId = "SECONDARY_ABILITY",
      title = "SECONDARY ABILITY",
      subtitle = "Instant 1-press Phase Dash, Cloak/EMP, Stasis Bubble, or Warhead",
      accentColor = Color(0xFFF59E0B),
    ),
    ControllerActionSlotInfo(
      slotId = "REPEL SHOCKWAVE".let { "REPEL" },
      title = "REPEL SHOCKWAVE",
      subtitle = "Triggers 360° radial repulsion wave (320⚡ Flux)",
      accentColor = Color(0xFF60A5FA),
    ),
    ControllerActionSlotInfo(
      slotId = "MULTI_TOGGLE",
      title = "MULTI-SHOT TOGGLE",
      subtitle = "Toggles innate Split Multi-Shot on Suppressor, Twinfang & Rift Frame",
      accentColor = Color(0xFFFACC15),
    ),
  )

private val HARDWARE_BIND_CHIPS =
  listOf(
    RiftCalibrationState.BIND_RT to "RT / R2",
    KeyEvent.KEYCODE_BUTTON_R1 to "R1 / RB",
    RiftCalibrationState.BIND_LT to "LT / L2",
    KeyEvent.KEYCODE_BUTTON_L1 to "L1 / LB",
    KeyEvent.KEYCODE_BUTTON_X to "X BTN",
    KeyEvent.KEYCODE_BUTTON_A to "A BTN",
    KeyEvent.KEYCODE_BUTTON_B to "B BTN",
    KeyEvent.KEYCODE_BUTTON_Y to "Y BTN",
    KeyEvent.KEYCODE_BUTTON_THUMBL to "L3",
    KeyEvent.KEYCODE_BUTTON_THUMBR to "R3",
  )

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
  val context = LocalContext.current
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
              text = "AUDIO SFX • TWIN-STICK CONTROLLER REMAPPER • CAMERA FOV • PLAYFIELD TEXT SCALE • CLOUD IDENTITY",
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

        // Section 2: Interactive Twin-Stick Controller Schematic & WoW-Style Live Button Remapper
        ControllerDiagramAndMapperCard(context = context)

        // Section 3: Playfield Text Scale Slider (0.75x to 2.0x)
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
                text = "[GUEST] VIPER ACE • ★ 320",
                color = Color(0xFF38BDF8),
                fontSize = (13f * playfieldTextScale).sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.SansSerif,
              )
            }
          }
        }

        // Section 4: Camera Zoom / FOV Slider (0.5x to 1.5x)
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

@Composable
fun ControllerDiagramAndMapperCard(
  context: Context,
  modifier: Modifier = Modifier,
) {
  // Watch calibrationRevision so changes recompose immediately
  val rev = RiftCalibrationState.calibrationRevision
  var listeningSlot by remember { mutableStateOf<String?>(null) }
  var feedbackBanner by remember { mutableStateOf<String?>(null) }
  val mapperFocusRequester = remember { FocusRequester() }

  val pulseTransition = rememberInfiniteTransition(label = "controllerHighlightPulse")
  val pulseAlpha by
    pulseTransition.animateFloat(
      initialValue = 0.45f,
      targetValue = 1.0f,
      animationSpec =
        infiniteRepeatable(
          animation = tween(durationMillis = 520),
          repeatMode = RepeatMode.Reverse,
        ),
      label = "pulseAlpha",
    )

  LaunchedEffect(listeningSlot) {
    if (listeningSlot != null) {
      runCatching { mapperFocusRequester.requestFocus() }
    }
  }

  val activeListeningInfo =
    remember(listeningSlot) {
      REMAPPABLE_ACTION_SLOTS.find { it.slotId == listeningSlot }
    }
  val highlightedHardwareCode =
    remember(listeningSlot, rev) {
      listeningSlot?.let { RiftCalibrationState.getBindingForActionSlot(it) }
    }

  Column(
    modifier =
      modifier
        .fillMaxWidth()
        .background(Color(0xFF0B182C), RoundedCornerShape(12.dp))
        .border(1.dp, Color(0xFF1E3A5F), RoundedCornerShape(12.dp))
        .focusRequester(mapperFocusRequester)
        .focusable()
        .onPreviewKeyEvent { keyEvent ->
          val slot = listeningSlot ?: return@onPreviewKeyEvent false
          val native = keyEvent.nativeKeyEvent
          if (native.action == KeyEvent.ACTION_DOWN && native.repeatCount == 0) {
            val code =
              when (native.keyCode) {
                KeyEvent.KEYCODE_BUTTON_L2 -> RiftCalibrationState.BIND_LT
                KeyEvent.KEYCODE_BUTTON_R2 -> RiftCalibrationState.BIND_RT
                KeyEvent.KEYCODE_BUTTON_A,
                KeyEvent.KEYCODE_BUTTON_B,
                KeyEvent.KEYCODE_BUTTON_X,
                KeyEvent.KEYCODE_BUTTON_Y,
                KeyEvent.KEYCODE_BUTTON_L1,
                KeyEvent.KEYCODE_BUTTON_R1,
                KeyEvent.KEYCODE_BUTTON_THUMBL,
                KeyEvent.KEYCODE_BUTTON_THUMBR,
                KeyEvent.KEYCODE_DPAD_UP,
                KeyEvent.KEYCODE_DPAD_DOWN,
                KeyEvent.KEYCODE_DPAD_LEFT,
                KeyEvent.KEYCODE_DPAD_RIGHT -> native.keyCode
                else -> null
              }
            if (code != null) {
              RiftCalibrationState.gamepadConnected = true
              RiftCalibrationState.assignControllerBinding(slot, code, context)
              val slotTitle = REMAPPABLE_ACTION_SLOTS.find { it.slotId == slot }?.title ?: slot
              feedbackBanner =
                "✓ BOUND [$slotTitle] TO [${RiftCalibrationState.getBindingLabel(code)}]!"
              listeningSlot = null
              true
            } else {
              false
            }
          } else {
            false
          }
        }
        .padding(12.dp)
        .testTag("controller_diagram_and_mapper_card"),
    verticalArrangement = Arrangement.spacedBy(9.dp),
  ) {
    // Header Row + Reset Default Binds Button
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = "🎮 TWIN-STICK CONTROLLER SCHEMATIC & LIVE BUTTON REMAPPER",
          color = Color(0xFF00F0FF),
          fontSize = 13.5.sp,
          fontWeight = FontWeight.Black,
          fontFamily = FontFamily.SansSerif,
        )
        Text(
          text =
            "L-STICK: 360° Flight & Heading Aim (Zero-Friction Drift on Release) • R-STICK: 360° Independent Turret Aim (When Neutral, Weapons Fire Along L-Stick Heading!)",
          color = Color(0xFF94A3B8),
          fontSize = 10.sp,
          fontFamily = FontFamily.SansSerif,
        )
      }

      Button(
        onClick = {
          RiftCalibrationState.resetControllerBindingsToDefault(context)
          listeningSlot = null
          feedbackBanner =
            "↺ RESTORED DEFAULT CONTROLLER BINDINGS (RT=MAIN, R1=BUFF, LT=BRAKE, X=SECONDARY, A=REPEL, Y=MULTI)."
        },
        colors =
          ButtonDefaults.buttonColors(
            containerColor = Color(0xFF2B1810),
            contentColor = Color(0xFFFBBF24),
          ),
        border = BorderStroke(1.dp, Color(0xFFF59E0B)),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.height(32.dp).testTag("btn_reset_default_controller_binds"),
      ) {
        Text(
          text = "↺ RESET DEFAULT BINDS",
          fontSize = 9.5.sp,
          fontWeight = FontWeight.Black,
          fontFamily = FontFamily.Monospace,
        )
      }
    }

    // Listening or Feedback Banner
    if (activeListeningInfo != null) {
      Row(
        modifier =
          Modifier.fillMaxWidth()
            .background(Color(0xFF2B1D06), RoundedCornerShape(8.dp))
            .border(1.5.dp, Color(0xFFFACC15), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text =
            "🎯 PRESS ANY CONTROLLER BUTTON OR PULL LT/RT TO BIND [${activeListeningInfo.title}]... (OR TAP A HARDWARE CHIP BELOW)",
          color = Color(0xFFFEF08A),
          fontSize = 10.sp,
          fontWeight = FontWeight.Black,
          fontFamily = FontFamily.Monospace,
          modifier = Modifier.weight(1f),
        )
        Text(
          text = "✕ CANCEL",
          color = Color(0xFFF87171),
          fontSize = 9.5.sp,
          fontWeight = FontWeight.Black,
          fontFamily = FontFamily.Monospace,
          modifier =
            Modifier.padding(start = 8.dp)
              .clickable { listeningSlot = null },
        )
      }
    } else if (feedbackBanner != null) {
      Row(
        modifier =
          Modifier.fillMaxWidth()
            .background(Color(0xFF07261E), RoundedCornerShape(8.dp))
            .border(1.dp, Color(0xFF10B981), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = feedbackBanner ?: "",
          color = Color(0xFF6EE7B7),
          fontSize = 9.5.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.Monospace,
        )
        Text(
          text = "✕",
          color = Color(0xFF94A3B8),
          fontSize = 9.5.sp,
          modifier = Modifier.clickable { feedbackBanner = null },
        )
      }
    }

    // Main Content: Left = Procedural Vector Gamepad Schematic | Right = 6-Slot WoW-Style Click-to-Bind Grid
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(10.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      // 1. Procedural Vector Twin-Stick Controller Schematic Canvas
      TwinStickControllerSchematicCanvas(
        modifier =
          Modifier.weight(0.52f)
            .height(215.dp)
            .background(Color(0xFF050B14), RoundedCornerShape(10.dp))
            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(10.dp))
            .testTag("controller_schematic_canvas"),
        revision = rev,
        highlightedCode = highlightedHardwareCode,
        pulseAlpha = pulseAlpha,
      )

      // 2. WoW-Style 6-Slot Action Remapping Grid (2 Columns x 3 Rows)
      Column(
        modifier = Modifier.weight(0.48f),
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Text(
          text = "TAP ANY ACTION SLOT TO REBIND (AUTO-SWAPS DUPLICATES):",
          color = Color(0xFFCBD5E1),
          fontSize = 9.5.sp,
          fontWeight = FontWeight.Black,
          fontFamily = FontFamily.Monospace,
        )

        REMAPPABLE_ACTION_SLOTS.chunked(2).forEach { pair ->
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            pair.forEach { slotInfo ->
              val isListening = listeningSlot == slotInfo.slotId
              val boundCode =
                remember(rev, slotInfo.slotId) {
                  RiftCalibrationState.getBindingForActionSlot(slotInfo.slotId)
                }
              val boundLabel =
                remember(boundCode) {
                  RiftCalibrationState.getBindingLabel(boundCode)
                }

              Column(
                modifier =
                  Modifier.weight(1f)
                    .testTag("controller_bind_slot_${slotInfo.slotId}")
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                      if (isListening) Color(0xFF2D2106) else Color(0xFF081222)
                    )
                    .border(
                      width = if (isListening) 1.6.dp else 1.dp,
                      color =
                        if (isListening) Color(0xFFFACC15)
                        else slotInfo.accentColor.copy(alpha = 0.65f),
                      shape = RoundedCornerShape(8.dp),
                    )
                    .clickable {
                      listeningSlot =
                        if (listeningSlot == slotInfo.slotId) null else slotInfo.slotId
                    }
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Text(
                    text = slotInfo.title,
                    color = Color.White,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.SansSerif,
                    maxLines = 1,
                  )
                  Box(
                    modifier =
                      Modifier.background(
                          if (isListening) Color(0xFFFACC15)
                          else slotInfo.accentColor.copy(alpha = 0.22f),
                          RoundedCornerShape(4.dp),
                        )
                        .border(
                          1.dp,
                          if (isListening) Color(0xFFFACC15) else slotInfo.accentColor,
                          RoundedCornerShape(4.dp),
                        )
                        .padding(horizontal = 5.dp, vertical = 1.5.dp)
                  ) {
                    Text(
                      text = if (isListening) "PRESS KEY..." else boundLabel,
                      color = if (isListening) Color(0xFF030712) else Color.White,
                      fontSize = 8.5.sp,
                      fontWeight = FontWeight.Black,
                      fontFamily = FontFamily.Monospace,
                    )
                  }
                }
                Text(
                  text = slotInfo.subtitle,
                  color = Color(0xFF94A3B8),
                  fontSize = 7.8.sp,
                  lineHeight = 10.sp,
                  maxLines = 2,
                )
              }
            }
          }
        }
      }
    }

    // 1-Tap Hardware Binding Chips Row (Allows remapping by touch even when controller is powered off)
    Row(
      modifier =
        Modifier.fillMaxWidth()
          .background(Color(0xFF060D1A), RoundedCornerShape(8.dp))
          .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(8.dp))
          .padding(horizontal = 8.dp, vertical = 6.dp)
          .horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(6.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text =
          if (listeningSlot != null) "TAP CHIP TO BIND:"
          else "SELECT SLOT ABOVE THEN TAP CHIP:",
        color = if (listeningSlot != null) Color(0xFFFACC15) else Color(0xFF94A3B8),
        fontSize = 8.5.sp,
        fontWeight = FontWeight.Black,
        fontFamily = FontFamily.Monospace,
      )

      HARDWARE_BIND_CHIPS.forEach { (hwCode, hwLabel) ->
        val actionOnChip =
          remember(rev, hwCode) {
            RiftCalibrationState.getActionForBinding(hwCode)
          }
        val isHighlighted = highlightedHardwareCode == hwCode
        Box(
          modifier =
            Modifier.testTag("controller_hw_chip_$hwCode")
              .clip(RoundedCornerShape(6.dp))
              .background(
                when {
                  isHighlighted -> Color(0xFF3B2A07)
                  listeningSlot != null -> Color(0xFF132642)
                  else -> Color(0xFF0B1628)
                }
              )
              .border(
                1.dp,
                when {
                  isHighlighted -> Color(0xFFFACC15)
                  listeningSlot != null -> Color(0xFF38BDF8)
                  else -> Color(0xFF334155)
                },
                RoundedCornerShape(6.dp),
              )
              .clickable {
                val activeSlot = listeningSlot ?: "PRIMARY_FIRE"
                RiftCalibrationState.assignControllerBinding(activeSlot, hwCode, context)
                val slotTitle =
                  REMAPPABLE_ACTION_SLOTS.find { it.slotId == activeSlot }?.title ?: activeSlot
                feedbackBanner = "✓ BOUND [$slotTitle] TO [$hwLabel]!"
                listeningSlot = null
              }
              .padding(horizontal = 7.dp, vertical = 4.dp),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = if (actionOnChip != null) "$hwLabel ($actionOnChip)" else hwLabel,
            color = if (isHighlighted) Color(0xFFFEF08A) else Color.White,
            fontSize = 8.2.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
          )
        }
      }
    }
  }
}

@Composable
private fun TwinStickControllerSchematicCanvas(
  modifier: Modifier = Modifier,
  revision: Int,
  highlightedCode: Int?,
  pulseAlpha: Float,
) {
  val ltAction =
    remember(revision) {
      RiftCalibrationState.getActionForBinding(RiftCalibrationState.BIND_LT) ?: "UNBOUND"
    }
  val rtAction =
    remember(revision) {
      RiftCalibrationState.getActionForBinding(RiftCalibrationState.BIND_RT) ?: "UNBOUND"
    }
  val l1Action =
    remember(revision) {
      RiftCalibrationState.getActionForBinding(KeyEvent.KEYCODE_BUTTON_L1) ?: "MENU TAB ◀"
    }
  val r1Action =
    remember(revision) {
      RiftCalibrationState.getActionForBinding(KeyEvent.KEYCODE_BUTTON_R1) ?: "UNBOUND"
    }
  val xAction =
    remember(revision) {
      RiftCalibrationState.getActionForBinding(KeyEvent.KEYCODE_BUTTON_X) ?: "UNBOUND"
    }
  val yAction =
    remember(revision) {
      RiftCalibrationState.getActionForBinding(KeyEvent.KEYCODE_BUTTON_Y) ?: "UNBOUND"
    }
  val aAction =
    remember(revision) {
      RiftCalibrationState.getActionForBinding(KeyEvent.KEYCODE_BUTTON_A) ?: "UNBOUND"
    }
  val bAction =
    remember(revision) {
      RiftCalibrationState.getActionForBinding(KeyEvent.KEYCODE_BUTTON_B) ?: "BACK / CLOSE"
    }

  val labelPaint =
    remember {
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
      }
    }

  Canvas(modifier = modifier) {
    val w = size.width
    val h = size.height
    val cx = w * 0.5f
    val cy = h * 0.52f

    // 1. Controller Body Silhouette (Ergonomic Dual-Grip Gamepad)
    val bodyW = w * 0.44f
    val bodyH = h * 0.48f
    val bodyLeft = cx - bodyW * 0.5f
    val bodyTop = cy - bodyH * 0.46f
    val bodyRight = cx + bodyW * 0.5f
    val bodyBottom = cy + bodyH * 0.54f

    val gamepadPath =
      Path().apply {
        moveTo(bodyLeft + bodyW * 0.16f, bodyTop)
        lineTo(bodyRight - bodyW * 0.16f, bodyTop)
        quadraticTo(bodyRight + bodyW * 0.08f, bodyTop, bodyRight + bodyW * 0.12f, cy + bodyH * 0.18f)
        quadraticTo(
          bodyRight + bodyW * 0.15f,
          bodyBottom + bodyH * 0.34f,
          bodyRight - bodyW * 0.04f,
          bodyBottom + bodyH * 0.36f,
        )
        quadraticTo(
          bodyRight - bodyW * 0.18f,
          bodyBottom + bodyH * 0.34f,
          bodyRight - bodyW * 0.24f,
          bodyBottom - bodyH * 0.06f,
        )
        lineTo(bodyLeft + bodyW * 0.24f, bodyBottom - bodyH * 0.06f)
        quadraticTo(
          bodyLeft + bodyW * 0.18f,
          bodyBottom + bodyH * 0.34f,
          bodyLeft + bodyW * 0.04f,
          bodyBottom + bodyH * 0.36f,
        )
        quadraticTo(
          bodyLeft - bodyW * 0.15f,
          bodyBottom + bodyH * 0.34f,
          bodyLeft - bodyW * 0.12f,
          cy + bodyH * 0.18f,
        )
        quadraticTo(bodyLeft - bodyW * 0.08f, bodyTop, bodyLeft + bodyW * 0.16f, bodyTop)
        close()
      }

    drawPath(path = gamepadPath, color = Color(0xFF0E1B2E))
    drawPath(path = gamepadPath, color = Color(0xFF38BDF8), style = Stroke(width = 2.2f))

    // Helper for highlighting physical hardware controls
    fun strokeForCode(code: Int, defaultColor: Color): Color {
      return if (highlightedCode == code) {
        Color(0xFFFACC15).copy(alpha = pulseAlpha)
      } else {
        defaultColor
      }
    }

    // 2. Shoulder Triggers (LT/L2 & RT/R2) + Bumpers (L1/LB & R1/RB)
    val trigW = bodyW * 0.24f
    val trigH = h * 0.08f
    val ltCenter = Offset(bodyLeft + bodyW * 0.20f, bodyTop - trigH * 1.45f)
    val rtCenter = Offset(bodyRight - bodyW * 0.20f, bodyTop - trigH * 1.45f)
    val l1Center = Offset(bodyLeft + bodyW * 0.20f, bodyTop - trigH * 0.52f)
    val r1Center = Offset(bodyRight - bodyW * 0.20f, bodyTop - trigH * 0.52f)

    // LT & RT boxes
    drawRoundRect(
      color =
        if (highlightedCode == RiftCalibrationState.BIND_LT) Color(0xFF422806)
        else Color(0xFF132642),
      topLeft = Offset(ltCenter.x - trigW * 0.5f, ltCenter.y - trigH * 0.45f),
      size = Size(trigW, trigH * 0.9f),
      cornerRadius = CornerRadius(6f, 6f),
    )
    drawRoundRect(
      color = strokeForCode(RiftCalibrationState.BIND_LT, Color(0xFF38BDF8)),
      topLeft = Offset(ltCenter.x - trigW * 0.5f, ltCenter.y - trigH * 0.45f),
      size = Size(trigW, trigH * 0.9f),
      cornerRadius = CornerRadius(6f, 6f),
      style = Stroke(width = if (highlightedCode == RiftCalibrationState.BIND_LT) 3.2f else 1.8f),
    )

    drawRoundRect(
      color =
        if (highlightedCode == RiftCalibrationState.BIND_RT) Color(0xFF422806)
        else Color(0xFF2B1219),
      topLeft = Offset(rtCenter.x - trigW * 0.5f, rtCenter.y - trigH * 0.45f),
      size = Size(trigW, trigH * 0.9f),
      cornerRadius = CornerRadius(6f, 6f),
    )
    drawRoundRect(
      color = strokeForCode(RiftCalibrationState.BIND_RT, Color(0xFFEF4444)),
      topLeft = Offset(rtCenter.x - trigW * 0.5f, rtCenter.y - trigH * 0.45f),
      size = Size(trigW, trigH * 0.9f),
      cornerRadius = CornerRadius(6f, 6f),
      style = Stroke(width = if (highlightedCode == RiftCalibrationState.BIND_RT) 3.2f else 1.8f),
    )

    // L1 & R1 bumpers
    drawRoundRect(
      color =
        if (highlightedCode == KeyEvent.KEYCODE_BUTTON_L1) Color(0xFF422806)
        else Color(0xFF0F1D33),
      topLeft = Offset(l1Center.x - trigW * 0.5f, l1Center.y - trigH * 0.42f),
      size = Size(trigW, trigH * 0.84f),
      cornerRadius = CornerRadius(5f, 5f),
    )
    drawRoundRect(
      color = strokeForCode(KeyEvent.KEYCODE_BUTTON_L1, Color(0xFF94A3B8)),
      topLeft = Offset(l1Center.x - trigW * 0.5f, l1Center.y - trigH * 0.42f),
      size = Size(trigW, trigH * 0.84f),
      cornerRadius = CornerRadius(5f, 5f),
      style = Stroke(width = if (highlightedCode == KeyEvent.KEYCODE_BUTTON_L1) 3.2f else 1.6f),
    )

    drawRoundRect(
      color =
        if (highlightedCode == KeyEvent.KEYCODE_BUTTON_R1) Color(0xFF422806)
        else Color(0xFF0B291E),
      topLeft = Offset(r1Center.x - trigW * 0.5f, r1Center.y - trigH * 0.42f),
      size = Size(trigW, trigH * 0.84f),
      cornerRadius = CornerRadius(5f, 5f),
    )
    drawRoundRect(
      color = strokeForCode(KeyEvent.KEYCODE_BUTTON_R1, Color(0xFF34D399)),
      topLeft = Offset(r1Center.x - trigW * 0.5f, r1Center.y - trigH * 0.42f),
      size = Size(trigW, trigH * 0.84f),
      cornerRadius = CornerRadius(5f, 5f),
      style = Stroke(width = if (highlightedCode == KeyEvent.KEYCODE_BUTTON_R1) 3.2f else 1.6f),
    )

    // 3. Locked Left Stick (360° Flight & Heading Aim) & Right Stick (360° Turret Aim)
    val lStickCenter = Offset(cx - bodyW * 0.25f, cy - bodyH * 0.08f)
    val rStickCenter = Offset(cx + bodyW * 0.14f, cy + bodyH * 0.20f)
    val stickRadius = bodyH * 0.19f

    drawCircle(color = Color(0xFF07101E), radius = stickRadius, center = lStickCenter)
    drawCircle(
      color = strokeForCode(KeyEvent.KEYCODE_BUTTON_THUMBL, Color(0xFF00F0FF)),
      radius = stickRadius,
      center = lStickCenter,
      style = Stroke(width = 2.4f),
    )
    drawCircle(color = Color(0xFF00F0FF).copy(alpha = 0.35f), radius = stickRadius * 0.48f, center = lStickCenter)

    drawCircle(color = Color(0xFF07101E), radius = stickRadius, center = rStickCenter)
    drawCircle(
      color = strokeForCode(KeyEvent.KEYCODE_BUTTON_THUMBR, Color(0xFFC084FC)),
      radius = stickRadius,
      center = rStickCenter,
      style = Stroke(width = 2.4f),
    )
    drawCircle(color = Color(0xFFC084FC).copy(alpha = 0.35f), radius = stickRadius * 0.48f, center = rStickCenter)

    // 4. D-Pad Cross (Lower-Left)
    val dpadCenter = Offset(cx - bodyW * 0.14f, cy + bodyH * 0.20f)
    val dArm = stickRadius * 0.72f
    drawLine(
      color = Color(0xFF64748B),
      start = Offset(dpadCenter.x - dArm, dpadCenter.y),
      end = Offset(dpadCenter.x + dArm, dpadCenter.y),
      strokeWidth = 6f,
    )
    drawLine(
      color = Color(0xFF64748B),
      start = Offset(dpadCenter.x, dpadCenter.y - dArm),
      end = Offset(dpadCenter.x, dpadCenter.y + dArm),
      strokeWidth = 6f,
    )

    // 5. Face Buttons (Y = Top, X = Left, B = Right, A = Bottom)
    val faceClusterCenter = Offset(cx + bodyW * 0.26f, cy - bodyH * 0.08f)
    val faceOffset = stickRadius * 0.78f
    val btnR = stickRadius * 0.36f
    val yPos = Offset(faceClusterCenter.x, faceClusterCenter.y - faceOffset)
    val xPos = Offset(faceClusterCenter.x - faceOffset, faceClusterCenter.y)
    val bPos = Offset(faceClusterCenter.x + faceOffset, faceClusterCenter.y)
    val aPos = Offset(faceClusterCenter.x, faceClusterCenter.y + faceOffset)

    val faceButtons =
      listOf(
        Triple(KeyEvent.KEYCODE_BUTTON_Y, yPos, Color(0xFFFACC15) to "Y"),
        Triple(KeyEvent.KEYCODE_BUTTON_X, xPos, Color(0xFFF59E0B) to "X"),
        Triple(KeyEvent.KEYCODE_BUTTON_B, bPos, Color(0xFFEF4444) to "B"),
        Triple(KeyEvent.KEYCODE_BUTTON_A, aPos, Color(0xFF60A5FA) to "A"),
      )
    for ((code, pos, colAndLetter) in faceButtons) {
      val (col, letter) = colAndLetter
      val isHi = highlightedCode == code
      drawCircle(
        color = if (isHi) Color(0xFF422806) else Color(0xFF0A1424),
        radius = btnR,
        center = pos,
      )
      drawCircle(
        color = strokeForCode(code, col),
        radius = btnR,
        center = pos,
        style = Stroke(width = if (isHi) 3.0f else 1.8f),
      )
      drawContext.canvas.nativeCanvas.apply {
        labelPaint.color = android.graphics.Color.WHITE
        labelPaint.textSize = btnR * 1.15f
        drawText(letter, pos.x, pos.y + btnR * 0.40f, labelPaint)
      }
    }

    // 6. Leader Callouts & Live Bound Labels Around the Perimeter
    fun drawCalloutLabel(
      anchor: Offset,
      calloutPos: Offset,
      text: String,
      argbColor: Int,
      alignLeft: Boolean,
    ) {
      drawLine(
        color = Color(argbColor).copy(alpha = 0.65f),
        start = anchor,
        end = calloutPos,
        strokeWidth = 1.4f,
      )
      drawContext.canvas.nativeCanvas.apply {
        labelPaint.color = argbColor
        labelPaint.textSize = (h * 0.048f).coerceIn(14f, 22f)
        labelPaint.textAlign = if (alignLeft) Paint.Align.LEFT else Paint.Align.RIGHT
        drawText(
          text,
          if (alignLeft) calloutPos.x + 4f else calloutPos.x - 4f,
          calloutPos.y + 4f,
          labelPaint,
        )
      }
    }

    // Left Side Callouts
    drawCalloutLabel(
      anchor = ltCenter,
      calloutPos = Offset(w * 0.25f, h * 0.13f),
      text = "LT: $ltAction",
      argbColor = android.graphics.Color.parseColor("#38BDF8"),
      alignLeft = false,
    )
    drawCalloutLabel(
      anchor = l1Center,
      calloutPos = Offset(w * 0.25f, h * 0.25f),
      text = "L1: $l1Action",
      argbColor = android.graphics.Color.parseColor("#CBD5E1"),
      alignLeft = false,
    )
    drawCalloutLabel(
      anchor = lStickCenter,
      calloutPos = Offset(w * 0.24f, h * 0.48f),
      text = "L-STICK: 360° FLIGHT + AIM",
      argbColor = android.graphics.Color.parseColor("#00F0FF"),
      alignLeft = false,
    )
    drawCalloutLabel(
      anchor = dpadCenter,
      calloutPos = Offset(w * 0.24f, h * 0.78f),
      text = "D-PAD: ZOOM / HANGAR",
      argbColor = android.graphics.Color.parseColor("#94A3B8"),
      alignLeft = false,
    )

    // Right Side Callouts
    drawCalloutLabel(
      anchor = rtCenter,
      calloutPos = Offset(w * 0.75f, h * 0.13f),
      text = "RT: $rtAction",
      argbColor = android.graphics.Color.parseColor("#F87171"),
      alignLeft = true,
    )
    drawCalloutLabel(
      anchor = r1Center,
      calloutPos = Offset(w * 0.75f, h * 0.25f),
      text = "R1: $r1Action",
      argbColor = android.graphics.Color.parseColor("#34D399"),
      alignLeft = true,
    )
    drawCalloutLabel(
      anchor = yPos,
      calloutPos = Offset(w * 0.76f, h * 0.39f),
      text = "Y: $yAction",
      argbColor = android.graphics.Color.parseColor("#FACC15"),
      alignLeft = true,
    )
    drawCalloutLabel(
      anchor = xPos,
      calloutPos = Offset(w * 0.76f, h * 0.51f),
      text = "X: $xAction",
      argbColor = android.graphics.Color.parseColor("#F59E0B"),
      alignLeft = true,
    )
    drawCalloutLabel(
      anchor = aPos,
      calloutPos = Offset(w * 0.76f, h * 0.63f),
      text = "A: $aAction ($bAction = B)",
      argbColor = android.graphics.Color.parseColor("#60A5FA"),
      alignLeft = true,
    )
    drawCalloutLabel(
      anchor = rStickCenter,
      calloutPos = Offset(w * 0.75f, h * 0.80f),
      text = "R-STICK: 360° TURRET AIM",
      argbColor = android.graphics.Color.parseColor("#C084FC"),
      alignLeft = true,
    )
  }
}
