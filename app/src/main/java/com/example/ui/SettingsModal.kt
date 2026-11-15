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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.LocalPilotPrefs
import com.example.engine.MusicSynthArchetype
import com.example.engine.RiftCalibrationState
import com.example.engine.RiftSpriteBaker
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
      slotId = "REPEL",
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
private fun SettingsBeveledButton(
  label: String,
  topColor: Color,
  bottomColor: Color,
  borderColor: Color,
  textColor: Color,
  isCompactPhone: Boolean,
  modifier: Modifier = Modifier,
  testTag: String? = null,
  onClick: () -> Unit,
) {
  Box(
    modifier =
      modifier
        .height(if (isCompactPhone) 28.dp else 34.dp)
        .clip(RoundedCornerShape(8.dp))
        .background(Brush.verticalGradient(listOf(topColor, bottomColor)))
        .border(1.dp, borderColor, RoundedCornerShape(8.dp))
        .clickable(onClick = onClick)
        .padding(horizontal = if (isCompactPhone) 9.dp else 12.dp)
        .then(if (testTag != null) Modifier.testTag(testTag) else Modifier),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      text = label,
      color = textColor,
      fontSize = if (isCompactPhone) 9.5.sp else 10.5.sp,
      fontWeight = FontWeight.ExtraBold,
      fontFamily = FontFamily.SansSerif,
      maxLines = 1,
    )
  }
}

@Composable
fun SettingsModal(
  fps: Int,
  sfxEnabled: Boolean,
  musicEnabled: Boolean = true,
  musicBpm: Int = 118,
  musicVolume: Float = 0.70f,
  sfxVolume: Float = 0.85f,
  activeMusicArchetypeId: String = "DARK_SYNTHWAVE",
  activeMusicSummary: String = "Dark Synthwave • 118 BPM",
  initialMusicPrompt: String = "",
  cameraZoom: Float,
  playfieldTextScale: Float,
  isGoogleAuthenticated: Boolean,
  onToggleSfx: () -> Unit,
  onToggleMusic: () -> Unit = {},
  onChangeMusicBpm: (Int) -> Unit = {},
  onChangeMusicVolume: (Float) -> Unit = {},
  onChangeSfxVolume: (Float) -> Unit = {},
  onSelectMusicArchetype: (MusicSynthArchetype) -> Unit = {},
  onForgeCustomMusicPrompt: (String) -> Unit = {},
  onChangeCameraZoom: (Float) -> Unit,
  onChangePlayfieldTextScale: (Float) -> Unit,
  onRequestGoogleSignIn: () -> Unit,
  onRequestSignOut: () -> Unit,
  onCloseModal: () -> Unit,
  onOpenChangelog: () -> Unit = {},
) {
  val context = LocalContext.current
  val isCompactPhone = LocalConfiguration.current.screenHeightDp < 480
  val scrollState = rememberScrollState()
  val pilotPrefs = remember(context) { LocalPilotPrefs(context) }
  val riftShardsBalance = remember(isGoogleAuthenticated) { pilotPrefs.getRiftShards() }
  val activeCallsign = remember(isGoogleAuthenticated) { pilotPrefs.getCallsign() }
  val riftShardImage = remember { RiftSpriteBaker.riftShardIconBitmap.asImageBitmap() }
  var forgePromptDraft by remember(initialMusicPrompt) { mutableStateOf(initialMusicPrompt) }

  // Clean Top-Bar Clearance across phones & tablets (leaves exact space for top module switcher bar)
  val outerPadding = PaddingValues(start = 10.dp, end = 10.dp, top = 42.dp, bottom = 6.dp)

  Box(
    modifier =
      Modifier.fillMaxSize()
        .background(Color(0xB3000000))
        .padding(outerPadding)
        .testTag("settings_modal"),
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
        verticalArrangement = Arrangement.spacedBy(if (isCompactPhone) 6.dp else 10.dp),
      ) {
        // Pinned Single-Row Header (Close & Changelog always reachable)
        Row(
          modifier =
            Modifier.fillMaxWidth()
              .then(if (isCompactPhone) Modifier.height(30.dp) else Modifier),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text(
              text =
                if (isCompactPhone) "⚙️ COCKPIT SETTINGS ($fps FPS)"
                else "⚙️ ARENA COCKPIT SETTINGS ($fps FPS)",
              color = Color.White,
              fontSize = if (isCompactPhone) 13.5.sp else 17.sp,
              fontWeight = FontWeight.ExtraBold,
              fontFamily = FontFamily.SansSerif,
              letterSpacing = 0.8.sp,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            if (!isCompactPhone) {
              Text(
                text =
                  "60–180 BPM SYNTH & MUSIC FORGE • TWIN-STICK CONTROLLER REMAPPER • PLAYFIELD TEXT • CAMERA FOV",
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
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            SettingsBeveledButton(
              label = if (isCompactPhone) "📜 CHANGELOG" else "📜 VERSION CHANGELOG",
              topColor = Color(0xFF102A45),
              bottomColor = Color(0xFF081728),
              borderColor = Color(0xFF00F5D4),
              textColor = Color(0xFF00F5D4),
              isCompactPhone = isCompactPhone,
              testTag = "settings_btn_open_changelog",
              onClick = onOpenChangelog,
            )

            SettingsBeveledButton(
              label = "✕ CLOSE",
              topColor = Color(0xFF3D1422),
              bottomColor = Color(0xFF220A12),
              borderColor = Color(0xFFFF3B4E),
              textColor = Color(0xFFFF6B7A),
              isCompactPhone = isCompactPhone,
              testTag = "settings_close_button",
              onClick = onCloseModal,
            )
          }
        }

        // Scrollable Body: Synth Audio & NEXUS Music Forge + Controller Remapper + Sliders
        Column(
          modifier =
            Modifier.weight(1f)
              .fillMaxWidth()
              .verticalScroll(scrollState),
          verticalArrangement = Arrangement.spacedBy(if (isCompactPhone) 8.dp else 11.dp),
        ) {
          // Section 1: SYNTH AUDIO, 60-180 BPM SLIDER, VOLUME SLIDERS & NEXUS MUSIC FORGE
          Column(
            modifier =
              Modifier.fillMaxWidth()
                .background(Color(0xFF0B182B), RoundedCornerShape(10.dp))
                .border(1.dp, Color(0xFF1B3454), RoundedCornerShape(10.dp))
                .padding(if (isCompactPhone) 8.dp else 12.dp)
                .testTag("settings_synth_music_forge_card"),
            verticalArrangement = Arrangement.spacedBy(if (isCompactPhone) 7.dp else 10.dp),
          ) {
            // 1A. Top Row: Title + Active Soundtrack Badge + Sworn Pilot / Single 3D Shard Icon + Toggles + Cloud Auth
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column(
                modifier = Modifier.weight(1f).padding(end = 8.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp),
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                  Text(
                    text = "🎵 SYNTH AUDIO & NEXUS MUSIC FORGE",
                    color = Color(0xFF00F5D4),
                    fontSize = if (isCompactPhone) 11.sp else 12.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.SansSerif,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                  Box(
                    modifier =
                      Modifier.background(Color(0xFF071324), RoundedCornerShape(5.dp))
                        .border(1.dp, Color(0x6600F5D4), RoundedCornerShape(5.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                  ) {
                    Text(
                      text = activeMusicSummary,
                      color = Color(0xFF7DD3FC),
                      fontSize = if (isCompactPhone) 8.5.sp else 9.5.sp,
                      fontWeight = FontWeight.Bold,
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
                  Text(
                    text =
                      if (isGoogleAuthenticated) "SWORN PILOT: $activeCallsign"
                      else "PILOT: $activeCallsign (UNLINKED)",
                    color = if (isGoogleAuthenticated) Color(0xFF34D399) else Color(0xFF94A3B8),
                    fontSize = if (isCompactPhone) 9.5.sp else 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )

                  // Single 3D Faceted Amethyst Crystal Icon + Balance (Zero duplicate text glyph)
                  Row(
                    modifier =
                      Modifier.background(Color(0xFF1A1030), RoundedCornerShape(5.dp))
                        .border(1.dp, Color(0xFFA855F7), RoundedCornerShape(5.dp))
                        .padding(horizontal = 6.dp, vertical = 1.5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                  ) {
                    Image(
                      bitmap = riftShardImage,
                      contentDescription = "Rift Shards",
                      modifier = Modifier.size(if (isCompactPhone) 11.dp else 13.dp),
                    )
                    Text(
                      text = "$riftShardsBalance",
                      color = Color(0xFFE9D5FF),
                      fontSize = if (isCompactPhone) 9.sp else 10.sp,
                      fontWeight = FontWeight.Black,
                      fontFamily = FontFamily.Monospace,
                    )
                  }
                }
              }

              Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                SettingsBeveledButton(
                  label = if (musicEnabled) "🎵 MUSIC: ON" else "🔇 MUSIC: OFF",
                  topColor = if (musicEnabled) Color(0xFF0F2D46) else Color(0xFF361620),
                  bottomColor = if (musicEnabled) Color(0xFF081A2A) else Color(0xFF1F0B11),
                  borderColor = if (musicEnabled) Color(0xFF00F5D4) else Color(0xFFFF3B4E),
                  textColor = if (musicEnabled) Color(0xFF00F5D4) else Color(0xFFFF6B7A),
                  isCompactPhone = isCompactPhone,
                  testTag = "settings_music_toggle_button",
                  onClick = onToggleMusic,
                )

                SettingsBeveledButton(
                  label = if (sfxEnabled) "🔊 SFX: ON" else "🔇 SFX: MUTE",
                  topColor = if (sfxEnabled) Color(0xFF103328) else Color(0xFF361620),
                  bottomColor = if (sfxEnabled) Color(0xFF081C16) else Color(0xFF1F0B11),
                  borderColor = if (sfxEnabled) Color(0xFF00E676) else Color(0xFFFF3B4E),
                  textColor = if (sfxEnabled) Color(0xFF00E676) else Color(0xFFFF6B7A),
                  isCompactPhone = isCompactPhone,
                  testTag = "settings_sfx_toggle_button",
                  onClick = onToggleSfx,
                )

                if (isGoogleAuthenticated) {
                  SettingsBeveledButton(
                    label = if (isCompactPhone) "SIGN OUT" else "SIGN OUT CLOUD",
                    topColor = Color(0xFF1E2D45),
                    bottomColor = Color(0xFF111B2B),
                    borderColor = Color(0xFF475569),
                    textColor = Color(0xFFCBD5E1),
                    isCompactPhone = isCompactPhone,
                    onClick = onRequestSignOut,
                  )
                } else {
                  SettingsBeveledButton(
                    label = if (isCompactPhone) "☁️ SIGN IN" else "☁️ LINK GOOGLE",
                    topColor = Color(0xFF143D34),
                    bottomColor = Color(0xFF0A211C),
                    borderColor = Color(0xFF00F5D4),
                    textColor = Color(0xFF00F5D4),
                    isCompactPhone = isCompactPhone,
                    onClick = onRequestGoogleSignIn,
                  )
                }
              }
            }

            // 1B. Sliders Row (2-Column Split on Landscape): Left = Real-Time 60-180 BPM | Right = Music & SFX Volumes
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(if (isCompactPhone) 8.dp else 12.dp),
              verticalAlignment = Alignment.Top,
            ) {
              // Left Column: Real-Time Tempo (60 - 180 BPM)
              Column(
                modifier =
                  Modifier.weight(1f)
                    .background(Color(0xFF071120), RoundedCornerShape(8.dp))
                    .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = if (isCompactPhone) 6.dp else 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Text(
                    text = "TEMPO: $musicBpm BPM",
                    color = Color(0xFFFBBF24),
                    fontSize = if (isCompactPhone) 10.sp else 11.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                  )

                  Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(90, 118, 135, 155).forEach { bpmPreset ->
                      val isPresetActive = kotlin.math.abs(musicBpm - bpmPreset) <= 1
                      Box(
                        modifier =
                          Modifier.clip(RoundedCornerShape(5.dp))
                            .background(
                              if (isPresetActive) Color(0xFFF59E0B) else Color(0xFF132642)
                            )
                            .border(
                              1.dp,
                              if (isPresetActive) Color(0xFFFBBF24) else Color(0x44F59E0B),
                              RoundedCornerShape(5.dp),
                            )
                            .clickable { onChangeMusicBpm(bpmPreset) }
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                      ) {
                        Text(
                          text = "$bpmPreset",
                          color = if (isPresetActive) Color(0xFF030712) else Color.White,
                          fontSize = 8.5.sp,
                          fontWeight = FontWeight.Bold,
                          fontFamily = FontFamily.Monospace,
                        )
                      }
                    }
                  }
                }

                Slider(
                  value = musicBpm.coerceIn(60, 180).toFloat(),
                  onValueChange = { raw ->
                    onChangeMusicBpm(raw.roundToInt().coerceIn(60, 180))
                  },
                  valueRange = 60f..180f,
                  colors =
                    SliderDefaults.colors(
                      thumbColor = Color(0xFFF59E0B),
                      activeTrackColor = Color(0xFFF59E0B),
                      inactiveTrackColor = Color(0xFF132642),
                    ),
                  modifier =
                    Modifier.fillMaxWidth()
                      .height(if (isCompactPhone) 24.dp else 30.dp)
                      .testTag("settings_music_bpm_slider"),
                )

                Text(
                  text = "60 BPM Ambient Drift  ◀──▶  180 BPM Overdrive (Phase-Continuous)",
                  color = Color(0xFF8A99AD),
                  fontSize = 8.5.sp,
                  fontFamily = FontFamily.SansSerif,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }

              // Right Column: Music Volume & SFX Volume Sliders
              Column(
                modifier =
                  Modifier.weight(1f)
                    .background(Color(0xFF071120), RoundedCornerShape(8.dp))
                    .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = if (isCompactPhone) 6.dp else 8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Text(
                    text = "MUSIC VOL: ${(musicVolume * 100f).roundToInt()}%",
                    color = Color(0xFF00F5D4),
                    fontSize = if (isCompactPhone) 9.5.sp else 10.5.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                  )
                  Text(
                    text = if (musicEnabled) "LIVE STREAM" else "MUTED",
                    color = if (musicEnabled) Color(0xFF34D399) else Color(0xFFF87171),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                  )
                }

                Slider(
                  value = musicVolume.coerceIn(0f, 1f),
                  onValueChange = { onChangeMusicVolume(it.coerceIn(0f, 1f)) },
                  valueRange = 0f..1f,
                  colors =
                    SliderDefaults.colors(
                      thumbColor = Color(0xFF00F5D4),
                      activeTrackColor = Color(0xFF00F5D4),
                      inactiveTrackColor = Color(0xFF132642),
                    ),
                  modifier =
                    Modifier.fillMaxWidth()
                      .height(if (isCompactPhone) 22.dp else 26.dp)
                      .testTag("settings_music_volume_slider"),
                )

                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Text(
                    text = "SFX VOL: ${(sfxVolume * 100f).roundToInt()}%",
                    color = Color(0xFF38BDF8),
                    fontSize = if (isCompactPhone) 9.5.sp else 10.5.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                  )
                  Text(
                    text = if (sfxEnabled) "WEAPONS & THRUSTERS" else "MUTED",
                    color = if (sfxEnabled) Color(0xFF94A3B8) else Color(0xFFF87171),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                  )
                }

                Slider(
                  value = sfxVolume.coerceIn(0f, 1f),
                  onValueChange = { onChangeSfxVolume(it.coerceIn(0f, 1f)) },
                  valueRange = 0f..1f,
                  colors =
                    SliderDefaults.colors(
                      thumbColor = Color(0xFF38BDF8),
                      activeTrackColor = Color(0xFF38BDF8),
                      inactiveTrackColor = Color(0xFF132642),
                    ),
                  modifier =
                    Modifier.fillMaxWidth()
                      .height(if (isCompactPhone) 22.dp else 26.dp)
                      .testTag("settings_sfx_volume_slider"),
                )
              }
            }

            // 1C. NEXUS Music Forge Workbench (100% Local On-Device Procedural Synthesizer)
            Column(
              modifier =
                Modifier.fillMaxWidth()
                  .background(Color(0xFF060E1B), RoundedCornerShape(8.dp))
                  .border(1.dp, Color(0xFF1E3A5F), RoundedCornerShape(8.dp))
                  .padding(horizontal = 10.dp, vertical = if (isCompactPhone) 6.dp else 8.dp),
              verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              // 5 Beveled Genre Archetype Chips
              Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text(
                  text = "SYNTH PRESETS:",
                  color = Color(0xFF94A3B8),
                  fontSize = 8.5.sp,
                  fontWeight = FontWeight.Black,
                  fontFamily = FontFamily.Monospace,
                )

                MusicSynthArchetype.entries.forEach { archetype ->
                  val isSelected =
                    activeMusicArchetypeId.equals(archetype.id, ignoreCase = true) &&
                      forgePromptDraft.isBlank()
                  Box(
                    modifier =
                      Modifier.clip(RoundedCornerShape(6.dp))
                        .background(
                          if (isSelected) {
                            Brush.verticalGradient(listOf(Color(0xFF0E4F5C), Color(0xFF072B36)))
                          } else {
                            Brush.verticalGradient(listOf(Color(0xFF102038), Color(0xFF091324)))
                          }
                        )
                        .border(
                          1.dp,
                          if (isSelected) Color(0xFF00F5D4) else Color(0xFF334155),
                          RoundedCornerShape(6.dp),
                        )
                        .clickable {
                          forgePromptDraft = ""
                          onSelectMusicArchetype(archetype)
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .testTag("settings_music_archetype_${archetype.id}"),
                    contentAlignment = Alignment.Center,
                  ) {
                    Text(
                      text = "${archetype.label} (${archetype.defaultBpm})",
                      color = if (isSelected) Color(0xFF00F5D4) else Color(0xFFE2E8F0),
                      fontSize = 9.sp,
                      fontWeight = FontWeight.Bold,
                      fontFamily = FontFamily.SansSerif,
                      maxLines = 1,
                    )
                  }
                }
              }

              // Unclipped 36.dp Custom Prompt Input + [🎶 FORGE TRACK] Button
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Box(
                  modifier =
                    Modifier.weight(1f)
                      .height(36.dp)
                      .clip(RoundedCornerShape(8.dp))
                      .background(Color(0xFF040914))
                      .border(1.dp, Color(0xFF284B73), RoundedCornerShape(8.dp))
                      .padding(horizontal = 10.dp),
                  contentAlignment = Alignment.CenterStart,
                ) {
                  if (forgePromptDraft.isEmpty()) {
                    Text(
                      text =
                        "Describe custom battle music (e.g. fast dark acid bassline, calm cosmic pad, retro 8-bit boss)...",
                      color = Color(0xFF64748B),
                      fontSize = if (isCompactPhone) 9.5.sp else 10.5.sp,
                      fontFamily = FontFamily.SansSerif,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                    )
                  }
                  BasicTextField(
                    value = forgePromptDraft,
                    onValueChange = { forgePromptDraft = it.take(120) },
                    singleLine = true,
                    cursorBrush = SolidColor(Color(0xFF00F5D4)),
                    textStyle =
                      TextStyle(
                        color = Color.White,
                        fontSize = if (isCompactPhone) 10.5.sp else 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.SansSerif,
                      ),
                    modifier =
                      Modifier.fillMaxWidth()
                        .testTag("settings_music_forge_input"),
                  )
                }

                Box(
                  modifier =
                    Modifier.height(36.dp)
                      .clip(RoundedCornerShape(8.dp))
                      .background(
                        Brush.verticalGradient(listOf(Color(0xFF4C1D95), Color(0xFF2E1065)))
                      )
                      .border(1.dp, Color(0xFFC084FC), RoundedCornerShape(8.dp))
                      .clickable {
                        onForgeCustomMusicPrompt(forgePromptDraft)
                      }
                      .padding(horizontal = 12.dp)
                      .testTag("settings_music_forge_btn"),
                  contentAlignment = Alignment.Center,
                ) {
                  Text(
                    text = "🎶 FORGE TRACK",
                    color = Color(0xFFF3E8FF),
                    fontSize = if (isCompactPhone) 9.5.sp else 10.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.SansSerif,
                    maxLines = 1,
                  )
                }
              }
            }
          }

          // Section 2: Interactive Twin-Stick Controller Schematic & Live Button Remapper
          ControllerDiagramAndMapperCard(context = context)

          // Section 3: Playfield Text Scale Slider (0.75x to 1.75x — 75% to 175%)
          Column(
            modifier =
              Modifier.fillMaxWidth()
                .background(Color(0xFF0B182B), RoundedCornerShape(10.dp))
                .border(1.dp, Color(0xFF1B3454), RoundedCornerShape(10.dp))
                .padding(if (isCompactPhone) 8.dp else 12.dp),
            verticalArrangement = Arrangement.spacedBy(if (isCompactPhone) 4.dp else 8.dp),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                val pct = (playfieldTextScale * 100f).roundToInt()
                Text(
                  text = "🔤 PLAYFIELD TEXT SIZE: $pct%",
                  color = Color(0xFF00F5D4),
                  fontSize = if (isCompactPhone) 11.5.sp else 13.sp,
                  fontWeight = FontWeight.Bold,
                  fontFamily = FontFamily.SansSerif,
                )
                if (!isCompactPhone) {
                  Text(
                    text =
                      "Scales ship callsigns, bounties, rift anomaly labels, and stasis timers on the space playfield",
                    color = Color(0xFF8A99AD),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.SansSerif,
                  )
                }
              }

              // Quick Preset Chips (75% .. 175%)
              Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                listOf(
                    0.75f to "75%",
                    1.0f to "100%",
                    1.25f to "125%",
                    1.5f to "150%",
                    1.75f to "175%",
                  )
                  .forEach { (preset, label) ->
                    val isSelected = kotlin.math.abs(playfieldTextScale - preset) <= 0.04f
                    Box(
                      modifier =
                        Modifier.clip(RoundedCornerShape(6.dp))
                          .background(if (isSelected) Color(0xFF00F5D4) else Color(0xFF132642))
                          .border(
                            1.dp,
                            if (isSelected) Color(0xFF00F5D4) else Color(0x4400F5D4),
                            RoundedCornerShape(6.dp),
                          )
                          .clickable { onChangePlayfieldTextScale(preset) }
                          .padding(horizontal = 7.dp, vertical = 3.5.dp),
                    ) {
                      Text(
                        text = label,
                        color = if (isSelected) Color(0xFF030712) else Color.White,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                      )
                    }
                  }
              }
            }

            Slider(
              value = playfieldTextScale.coerceIn(0.75f, 1.75f),
              onValueChange = {
                onChangePlayfieldTextScale(((it * 20f).roundToInt() / 20f).coerceIn(0.75f, 1.75f))
              },
              valueRange = 0.75f..1.75f,
              colors =
                SliderDefaults.colors(
                  thumbColor = Color(0xFF00F5D4),
                  activeTrackColor = Color(0xFF00F5D4),
                  inactiveTrackColor = Color(0xFF132642),
                ),
              modifier =
                Modifier.fillMaxWidth()
                  .height(if (isCompactPhone) 24.dp else 32.dp)
                  .testTag("settings_text_scale_slider"),
            )

            // Live Text Preview Box
            Box(
              modifier =
                Modifier.fillMaxWidth()
                  .background(Color(0xFF050B14), RoundedCornerShape(6.dp))
                  .border(1.dp, Color(0x2800F5D4), RoundedCornerShape(6.dp))
                  .padding(horizontal = 12.dp, vertical = if (isCompactPhone) 5.dp else 8.dp),
              contentAlignment = Alignment.Center,
            ) {
              Text(
                text = "[AEX] VIPER ACE • ★ 320",
                color = Color(0xFF38BDF8),
                fontSize = (13f * playfieldTextScale.coerceIn(0.75f, 1.75f)).sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.SansSerif,
              )
            }
          }

          // Section 4: Camera Zoom / FOV Slider (0.5x to 1.5x)
          Column(
            modifier =
              Modifier.fillMaxWidth()
                .background(Color(0xFF0B182B), RoundedCornerShape(10.dp))
                .border(1.dp, Color(0xFF1B3454), RoundedCornerShape(10.dp))
                .padding(if (isCompactPhone) 8.dp else 12.dp),
            verticalArrangement = Arrangement.spacedBy(if (isCompactPhone) 4.dp else 8.dp),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                val formattedZoom = String.format(Locale.US, "%.2fx", cameraZoom)
                val zoomMode =
                  when {
                    cameraZoom <= 0.65f -> "TACTICAL OVERVIEW"
                    cameraZoom >= 1.25f -> "CINEMATIC CLOSE-UP"
                    else -> "STANDARD ARENA VIEW"
                  }
                Text(
                  text = "🔍 CAMERA ZOOM / FOV: $formattedZoom ($zoomMode)",
                  color = Color(0xFF00F5D4),
                  fontSize = if (isCompactPhone) 11.5.sp else 13.sp,
                  fontWeight = FontWeight.Bold,
                  fontFamily = FontFamily.SansSerif,
                )
                if (!isCompactPhone) {
                  Text(
                    text = "0.5x Wide tactical field to 1.5x tight dogfighting close-up",
                    color = Color(0xFF8A99AD),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.SansSerif,
                  )
                }
              }

              // Quick Preset Chips
              Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f).forEach { preset ->
                  val isSelected = (cameraZoom - preset).let { it >= -0.05f && it <= 0.05f }
                  Box(
                    modifier =
                      Modifier.clip(RoundedCornerShape(6.dp))
                        .background(if (isSelected) Color(0xFF00F5D4) else Color(0xFF132642))
                        .border(
                          1.dp,
                          if (isSelected) Color(0xFF00F5D4) else Color(0x4400F5D4),
                          RoundedCornerShape(6.dp),
                        )
                        .clickable { onChangeCameraZoom(preset) }
                        .padding(horizontal = 7.dp, vertical = 3.5.dp),
                  ) {
                    Text(
                      text = "${preset}x",
                      color = if (isSelected) Color(0xFF030712) else Color.White,
                      fontSize = 9.5.sp,
                      fontWeight = FontWeight.Bold,
                      fontFamily = FontFamily.Monospace,
                    )
                  }
                }
              }
            }

            Slider(
              value = cameraZoom,
              onValueChange = {
                onChangeCameraZoom(((it * 20f).roundToInt() / 20f).coerceIn(0.5f, 1.5f))
              },
              valueRange = 0.5f..1.5f,
              colors =
                SliderDefaults.colors(
                  thumbColor = Color(0xFF00F5D4),
                  activeTrackColor = Color(0xFF00F5D4),
                  inactiveTrackColor = Color(0xFF132642),
                ),
              modifier =
                Modifier.fillMaxWidth()
                  .height(if (isCompactPhone) 24.dp else 32.dp)
                  .testTag("settings_camera_zoom_slider"),
            )
          }
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
  val isCompactPhone = LocalConfiguration.current.screenHeightDp < 480
  // Watch calibrationRevision so changes recompose immediately
  val rev = RiftCalibrationState.calibrationRevision
  val isGamepadConnected = RiftCalibrationState.gamepadConnected
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
        .background(Color(0xFF071120), RoundedCornerShape(12.dp))
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
        .padding(if (isCompactPhone) 8.dp else 12.dp)
        .testTag("controller_diagram_and_mapper_card"),
    verticalArrangement = Arrangement.spacedBy(if (isCompactPhone) 6.dp else 9.dp),
  ) {
    // 1. Top Status Header: Dark-glass header bar with #00F5D4 Cyan / #F59E0B Gold corner ticks
    Box(
      modifier =
        Modifier.fillMaxWidth()
          .background(Color(0xFF050B14), RoundedCornerShape(8.dp))
          .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(8.dp))
    ) {
      Canvas(modifier = Modifier.matchParentSize()) {
        val tick = 9.dp.toPx()
        val strokeW = 2.dp.toPx()
        drawLine(Color(0xFF00F5D4), Offset(0f, 0f), Offset(tick, 0f), strokeW)
        drawLine(Color(0xFF00F5D4), Offset(0f, 0f), Offset(0f, tick), strokeW)
        drawLine(Color(0xFF00F5D4), Offset(0f, size.height), Offset(tick, size.height), strokeW)
        drawLine(Color(0xFF00F5D4), Offset(0f, size.height), Offset(0f, size.height - tick), strokeW)
        drawLine(Color(0xFFF59E0B), Offset(size.width - tick, 0f), Offset(size.width, 0f), strokeW)
        drawLine(Color(0xFFF59E0B), Offset(size.width, 0f), Offset(size.width, tick), strokeW)
        drawLine(
          Color(0xFFF59E0B),
          Offset(size.width - tick, size.height),
          Offset(size.width, size.height),
          strokeW,
        )
        drawLine(
          Color(0xFFF59E0B),
          Offset(size.width, size.height - tick),
          Offset(size.width, size.height),
          strokeW,
        )
      }

      Row(
        modifier =
          Modifier.fillMaxWidth()
            .padding(
              horizontal = if (isCompactPhone) 8.dp else 10.dp,
              vertical = if (isCompactPhone) 5.dp else 8.dp,
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Text(
              text = "🎮 TWIN-STICK GAMEPAD & TOUCH HUD",
              color = Color(0xFF00F5D4),
              fontSize = if (isCompactPhone) 11.sp else 13.sp,
              fontWeight = FontWeight.Black,
              fontFamily = FontFamily.Monospace,
            )
            Box(
              modifier =
                Modifier.background(
                    if (isGamepadConnected) Color(0xFF052E16) else Color(0xFF0F172A),
                    RoundedCornerShape(5.dp),
                  )
                  .border(
                    1.dp,
                    if (isGamepadConnected) Color(0xFF22C55E) else Color(0xFF475569),
                    RoundedCornerShape(5.dp),
                  )
                  .padding(horizontal = 6.dp, vertical = 1.5.dp)
            ) {
              Text(
                text =
                  if (isGamepadConnected) "🟢 GAMEPAD CONNECTED" else "⚪ TOUCH HUD ENABLED",
                color = if (isGamepadConnected) Color(0xFF4ADE80) else Color(0xFFCBD5E1),
                fontSize = 8.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
              )
            }
          }
          if (!isCompactPhone) {
            Text(
              text =
                "L-STICK: Flight & Heading (Zero-Friction Drift) • R-STICK: 360° Independent Aim (Fires Along Heading When Neutral)",
              color = Color(0xFF94A3B8),
              fontSize = 9.5.sp,
              fontFamily = FontFamily.SansSerif,
            )
          }
        }

        SettingsBeveledButton(
          label = "↺ RESET BINDS",
          topColor = Color(0xFF382012),
          bottomColor = Color(0xFF1F1008),
          borderColor = Color(0xFFF59E0B),
          textColor = Color(0xFFFBBF24),
          isCompactPhone = isCompactPhone,
          testTag = "btn_reset_default_controller_binds",
          onClick = {
            RiftCalibrationState.resetControllerBindingsToDefault(context)
            listeningSlot = null
            feedbackBanner =
              "↺ RESTORED DEFAULT CONTROLLER BINDINGS (RT=MAIN, R1=BUFF, LT=BRAKE, X=SECONDARY, A=REPEL, Y=MULTI)."
          },
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
            .padding(horizontal = 10.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text =
            "🎯 PRESS ANY CONTROLLER BUTTON OR PULL LT/RT TO BIND [${activeListeningInfo.title}]... (OR TAP A HARDWARE CHIP BELOW)",
          color = Color(0xFFFEF08A),
          fontSize = 9.5.sp,
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

    // Main Content: Left (0.52f) = Sculpted Vector Controller Schematic | Right (0.48f) = 2x3 Beveled Action Cards
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(if (isCompactPhone) 8.dp else 10.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      // 2. Left Panel (0.52f width): Sculpted Vector Twin-Stick Controller Schematic
      TwinStickControllerSchematicCanvas(
        modifier =
          Modifier.weight(0.52f)
            .height(if (isCompactPhone) 186.dp else 225.dp)
            .background(Color(0xFF050B14), RoundedCornerShape(10.dp))
            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(10.dp))
            .testTag("controller_schematic_canvas"),
        revision = rev,
        highlightedCode = highlightedHardwareCode,
        pulseAlpha = pulseAlpha,
      )

      // 3. Right Panel (0.48f width): 2x3 Beveled Sci-Fi Action Binding Cards
      Column(
        modifier = Modifier.weight(0.48f),
        verticalArrangement = Arrangement.spacedBy(if (isCompactPhone) 5.dp else 6.dp),
      ) {
        REMAPPABLE_ACTION_SLOTS.chunked(2).forEach { pair ->
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(if (isCompactPhone) 5.dp else 6.dp),
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
              val bracketColor =
                if (slotInfo.slotId == "PRIMARY_FIRE" || slotInfo.slotId == "MULTI_TOGGLE") {
                  Color(0xFFF59E0B)
                } else {
                  Color(0xFF00F5D4)
                }

              Box(
                modifier =
                  Modifier.weight(1f)
                    .height(if (isCompactPhone) 58.dp else 68.dp)
                    .testTag("controller_bind_slot_${slotInfo.slotId}")
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                      if (isListening) {
                        Brush.verticalGradient(listOf(Color(0xFF3B2806), Color(0xFF1E1403)))
                      } else {
                        Brush.verticalGradient(listOf(Color(0xFF0C1A33), Color(0xFF081224)))
                      }
                    )
                    .border(
                      width = if (isListening) 1.6.dp else 1.dp,
                      color =
                        if (isListening) Color(0xFFFACC15)
                        else bracketColor.copy(alpha = 0.45f),
                      shape = RoundedCornerShape(8.dp),
                    )
                    .clickable {
                      listeningSlot =
                        if (listeningSlot == slotInfo.slotId) null else slotInfo.slotId
                    }
              ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                  val bLen = 8.dp.toPx()
                  val bStroke = 1.8.dp.toPx()
                  val cCol = if (isListening) Color(0xFFFACC15) else bracketColor

                  drawLine(cCol, Offset(0f, 0f), Offset(bLen, 0f), bStroke)
                  drawLine(cCol, Offset(0f, 0f), Offset(0f, bLen), bStroke)
                  drawLine(cCol, Offset(size.width - bLen, 0f), Offset(size.width, 0f), bStroke)
                  drawLine(cCol, Offset(size.width, 0f), Offset(size.width, bLen), bStroke)
                  drawLine(cCol, Offset(0f, size.height), Offset(bLen, size.height), bStroke)
                  drawLine(cCol, Offset(0f, size.height - bLen), Offset(0f, size.height), bStroke)
                  drawLine(
                    cCol,
                    Offset(size.width - bLen, size.height),
                    Offset(size.width, size.height),
                    bStroke,
                  )
                  drawLine(
                    cCol,
                    Offset(size.width, size.height - bLen),
                    Offset(size.width, size.height),
                    bStroke,
                  )

                  val gx = size.width * 0.22f
                  val gy = size.height * 0.30f
                  val gw = 24.dp.toPx()
                  val gh = 13.dp.toPx()
                  drawRoundRect(
                    color = Color(0xFF050B14),
                    topLeft = Offset(gx - gw * 0.5f, gy - gh * 0.5f),
                    size = Size(gw, gh),
                    cornerRadius = CornerRadius(4f, 4f),
                  )
                  drawRoundRect(
                    color = Color(0xFF475569),
                    topLeft = Offset(gx - gw * 0.5f, gy - gh * 0.5f),
                    size = Size(gw, gh),
                    cornerRadius = CornerRadius(4f, 4f),
                    style = Stroke(width = 1.2f),
                  )
                  val dotOffset =
                    when (boundCode) {
                      RiftCalibrationState.BIND_LT,
                      KeyEvent.KEYCODE_BUTTON_L1 -> Offset(gx - gw * 0.34f, gy - gh * 0.48f)
                      RiftCalibrationState.BIND_RT,
                      KeyEvent.KEYCODE_BUTTON_R1 -> Offset(gx + gw * 0.34f, gy - gh * 0.48f)
                      KeyEvent.KEYCODE_BUTTON_Y -> Offset(gx + gw * 0.26f, gy - gh * 0.22f)
                      KeyEvent.KEYCODE_BUTTON_X -> Offset(gx + gw * 0.14f, gy)
                      KeyEvent.KEYCODE_BUTTON_B -> Offset(gx + gw * 0.38f, gy)
                      else -> Offset(gx + gw * 0.26f, gy + gh * 0.22f)
                    }
                  drawCircle(
                    color = cCol,
                    radius = 3.0.dp.toPx(),
                    center = dotOffset,
                  )
                }

                Column(
                  modifier = Modifier.fillMaxSize().padding(horizontal = 7.dp, vertical = 4.dp),
                  verticalArrangement = Arrangement.SpaceBetween,
                ) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    Box(
                      modifier =
                        Modifier.background(
                            if (isListening) Color(0xFFFACC15) else Color(0xFF050B14),
                            RoundedCornerShape(4.dp),
                          )
                          .border(
                            1.dp,
                            if (isListening) Color(0xFFFACC15) else bracketColor,
                            RoundedCornerShape(4.dp),
                          )
                          .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                      Text(
                        text = if (isListening) "PRESS KEY..." else "[$boundLabel]",
                        color = if (isListening) Color(0xFF030712) else bracketColor,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                      )
                    }
                  }

                  Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(
                      text = slotInfo.title,
                      color = Color.White,
                      fontSize = if (isCompactPhone) 8.8.sp else 9.5.sp,
                      fontWeight = FontWeight.Black,
                      fontFamily = FontFamily.Monospace,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                      text = slotInfo.subtitle,
                      color = Color(0xFF94A3B8),
                      fontSize = 7.2.sp,
                      lineHeight = 8.5.sp,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                    )
                  }
                }
              }
            }
          }
        }
      }
    }

    // 1-Tap Hardware Binding Chips Row
    Row(
      modifier =
        Modifier.fillMaxWidth()
          .background(Color(0xFF060D1A), RoundedCornerShape(8.dp))
          .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(8.dp))
          .padding(horizontal = 8.dp, vertical = 5.dp)
          .horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(6.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text =
          if (listeningSlot != null) "TAP CHIP TO BIND:"
          else "SELECT SLOT THEN TAP CHIP:",
        color = if (listeningSlot != null) Color(0xFFFACC15) else Color(0xFF94A3B8),
        fontSize = 8.2.sp,
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
              .padding(horizontal = 7.dp, vertical = 3.5.dp),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = if (actionOnChip != null) "$hwLabel ($actionOnChip)" else hwLabel,
            color = if (isHighlighted) Color(0xFFFEF08A) else Color.White,
            fontSize = 8.sp,
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
      RiftCalibrationState.getActionForBinding(KeyEvent.KEYCODE_BUTTON_B) ?: "UNBOUND"
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
    val cy = h * 0.54f

    // Blueprint corner reticle ticks inside #050B14 canvas
    val tick = 10.dp.toPx()
    val pad = 5.dp.toPx()
    val reticleCol = Color(0xFF00F5D4).copy(alpha = 0.55f)
    drawLine(reticleCol, Offset(pad, pad), Offset(pad + tick, pad), 1.5f)
    drawLine(reticleCol, Offset(pad, pad), Offset(pad, pad + tick), 1.5f)
    drawLine(reticleCol, Offset(w - pad - tick, pad), Offset(w - pad, pad), 1.5f)
    drawLine(reticleCol, Offset(w - pad, pad), Offset(w - pad, pad + tick), 1.5f)
    drawLine(reticleCol, Offset(pad, h - pad), Offset(pad + tick, h - pad), 1.5f)
    drawLine(reticleCol, Offset(pad, h - pad - tick), Offset(pad, h - pad), 1.5f)
    drawLine(reticleCol, Offset(w - pad - tick, h - pad), Offset(w - pad, h - pad), 1.5f)
    drawLine(reticleCol, Offset(w - pad, h - pad - tick), Offset(w - pad, h - pad), 1.5f)

    // Helper for highlighting physical hardware controls
    fun strokeForCode(code: Int, defaultColor: Color): Color {
      return if (highlightedCode == code) {
        Color(0xFFFACC15).copy(alpha = pulseAlpha)
      } else {
        defaultColor
      }
    }

    val bodyW = w * 0.44f
    val bodyH = h * 0.48f
    val bodyLeft = cx - bodyW * 0.5f
    val bodyTop = cy - bodyH * 0.46f
    val bodyRight = cx + bodyW * 0.5f
    val bodyBottom = cy + bodyH * 0.54f

    // 1. Sculpted Shoulder Triggers (LT/L2 & RT/R2) & Curved Bumpers (L1/LB & R1/RB)
    val trigW = bodyW * 0.24f
    val trigH = h * 0.075f
    val ltCenter = Offset(bodyLeft + bodyW * 0.20f, bodyTop - trigH * 1.48f)
    val rtCenter = Offset(bodyRight - bodyW * 0.20f, bodyTop - trigH * 1.48f)
    val l1Center = Offset(bodyLeft + bodyW * 0.20f, bodyTop - trigH * 0.54f)
    val r1Center = Offset(bodyRight - bodyW * 0.20f, bodyTop - trigH * 0.54f)

    // Stepped rear trigger housings (LT/L2 & RT/R2)
    drawRoundRect(
      color =
        if (highlightedCode == RiftCalibrationState.BIND_LT) Color(0xFF422806)
        else Color(0xFF0F223D),
      topLeft = Offset(ltCenter.x - trigW * 0.48f, ltCenter.y - trigH * 0.45f),
      size = Size(trigW * 0.96f, trigH * 0.90f),
      cornerRadius = CornerRadius(7f, 7f),
    )
    drawRoundRect(
      color = strokeForCode(RiftCalibrationState.BIND_LT, Color(0xFF00F5D4)),
      topLeft = Offset(ltCenter.x - trigW * 0.48f, ltCenter.y - trigH * 0.45f),
      size = Size(trigW * 0.96f, trigH * 0.90f),
      cornerRadius = CornerRadius(7f, 7f),
      style = Stroke(width = if (highlightedCode == RiftCalibrationState.BIND_LT) 3.2f else 1.8f),
    )

    drawRoundRect(
      color =
        if (highlightedCode == RiftCalibrationState.BIND_RT) Color(0xFF422806)
        else Color(0xFF281509),
      topLeft = Offset(rtCenter.x - trigW * 0.48f, rtCenter.y - trigH * 0.45f),
      size = Size(trigW * 0.96f, trigH * 0.90f),
      cornerRadius = CornerRadius(7f, 7f),
    )
    drawRoundRect(
      color = strokeForCode(RiftCalibrationState.BIND_RT, Color(0xFFF59E0B)),
      topLeft = Offset(rtCenter.x - trigW * 0.48f, rtCenter.y - trigH * 0.45f),
      size = Size(trigW * 0.96f, trigH * 0.90f),
      cornerRadius = CornerRadius(7f, 7f),
      style = Stroke(width = if (highlightedCode == RiftCalibrationState.BIND_RT) 3.2f else 1.8f),
    )

    // Curved top-edge bumper plates (L1/LB & R1/RB)
    drawRoundRect(
      color =
        if (highlightedCode == KeyEvent.KEYCODE_BUTTON_L1) Color(0xFF422806)
        else Color(0xFF111E34),
      topLeft = Offset(l1Center.x - trigW * 0.52f, l1Center.y - trigH * 0.40f),
      size = Size(trigW * 1.04f, trigH * 0.80f),
      cornerRadius = CornerRadius(6f, 6f),
    )
    drawRoundRect(
      color = strokeForCode(KeyEvent.KEYCODE_BUTTON_L1, Color(0xFF94A3B8)),
      topLeft = Offset(l1Center.x - trigW * 0.52f, l1Center.y - trigH * 0.40f),
      size = Size(trigW * 1.04f, trigH * 0.80f),
      cornerRadius = CornerRadius(6f, 6f),
      style = Stroke(width = if (highlightedCode == KeyEvent.KEYCODE_BUTTON_L1) 3.2f else 1.6f),
    )

    drawRoundRect(
      color =
        if (highlightedCode == KeyEvent.KEYCODE_BUTTON_R1) Color(0xFF422806)
        else Color(0xFF0A2823),
      topLeft = Offset(r1Center.x - trigW * 0.52f, r1Center.y - trigH * 0.40f),
      size = Size(trigW * 1.04f, trigH * 0.80f),
      cornerRadius = CornerRadius(6f, 6f),
    )
    drawRoundRect(
      color = strokeForCode(KeyEvent.KEYCODE_BUTTON_R1, Color(0xFF00F5D4)),
      topLeft = Offset(r1Center.x - trigW * 0.52f, r1Center.y - trigH * 0.40f),
      size = Size(trigW * 1.04f, trigH * 0.80f),
      cornerRadius = CornerRadius(6f, 6f),
      style = Stroke(width = if (highlightedCode == KeyEvent.KEYCODE_BUTTON_R1) 3.2f else 1.6f),
    )

    // 2. Sculpted Dual-Grip Shell: Smooth cubic Bezier silhouette with #0F172A->#1E293B gradient,
    // outer #38BDF8 Cyan contour (2.2.dp), and inner #F59E0B Gold ergonomic grip seam contours
    val gamepadPath =
      Path().apply {
        moveTo(bodyLeft + bodyW * 0.16f, bodyTop)
        lineTo(bodyRight - bodyW * 0.16f, bodyTop)
        cubicTo(
          bodyRight + bodyW * 0.08f,
          bodyTop,
          bodyRight + bodyW * 0.15f,
          cy - bodyH * 0.05f,
          bodyRight + bodyW * 0.13f,
          cy + bodyH * 0.22f,
        )
        cubicTo(
          bodyRight + bodyW * 0.15f,
          bodyBottom + bodyH * 0.36f,
          bodyRight - bodyW * 0.02f,
          bodyBottom + bodyH * 0.40f,
          bodyRight - bodyW * 0.10f,
          bodyBottom + bodyH * 0.34f,
        )
        cubicTo(
          bodyRight - bodyW * 0.19f,
          bodyBottom + bodyH * 0.26f,
          bodyRight - bodyW * 0.22f,
          bodyBottom + bodyH * 0.02f,
          bodyRight - bodyW * 0.26f,
          bodyBottom - bodyH * 0.06f,
        )
        lineTo(bodyLeft + bodyW * 0.26f, bodyBottom - bodyH * 0.06f)
        cubicTo(
          bodyLeft + bodyW * 0.22f,
          bodyBottom + bodyH * 0.02f,
          bodyLeft + bodyW * 0.19f,
          bodyBottom + bodyH * 0.26f,
          bodyLeft + bodyW * 0.10f,
          bodyBottom + bodyH * 0.34f,
        )
        cubicTo(
          bodyLeft + bodyW * 0.02f,
          bodyBottom + bodyH * 0.40f,
          bodyLeft - bodyW * 0.15f,
          bodyBottom + bodyH * 0.36f,
          bodyLeft - bodyW * 0.13f,
          cy + bodyH * 0.22f,
        )
        cubicTo(
          bodyLeft - bodyW * 0.15f,
          cy - bodyH * 0.05f,
          bodyLeft - bodyW * 0.08f,
          bodyTop,
          bodyLeft + bodyW * 0.16f,
          bodyTop,
        )
        close()
      }

    drawPath(
      path = gamepadPath,
      brush =
        Brush.verticalGradient(
          colors = listOf(Color(0xFF1E293B), Color(0xFF0F172A)),
          startY = bodyTop,
          endY = bodyBottom + bodyH * 0.38f,
        ),
    )
    drawPath(
      path = gamepadPath,
      color = Color(0xFF38BDF8),
      style = Stroke(width = 2.2.dp.toPx()),
    )

    // Inner #F59E0B Gold ergonomic grip seam contours along left and right handles
    val leftGripSeam =
      Path().apply {
        moveTo(bodyLeft + bodyW * 0.05f, bodyTop + bodyH * 0.16f)
        quadraticTo(
          bodyLeft - bodyW * 0.05f,
          cy + bodyH * 0.16f,
          bodyLeft + bodyW * 0.06f,
          bodyBottom + bodyH * 0.24f,
        )
      }
    val rightGripSeam =
      Path().apply {
        moveTo(bodyRight - bodyW * 0.05f, bodyTop + bodyH * 0.16f)
        quadraticTo(
          bodyRight + bodyW * 0.05f,
          cy + bodyH * 0.16f,
          bodyRight - bodyW * 0.06f,
          bodyBottom + bodyH * 0.24f,
        )
      }
    drawPath(
      path = leftGripSeam,
      color = Color(0xFFF59E0B).copy(alpha = 0.72f),
      style = Stroke(width = 1.4.dp.toPx()),
    )
    drawPath(
      path = rightGripSeam,
      color = Color(0xFFF59E0B).copy(alpha = 0.72f),
      style = Stroke(width = 1.4.dp.toPx()),
    )

    // 3. Concentric 3-Ring Analog Thumbsticks
    val lStickCenter = Offset(cx - bodyW * 0.25f, cy - bodyH * 0.08f)
    val rStickCenter = Offset(cx + bodyW * 0.14f, cy + bodyH * 0.20f)
    val stickRadius = bodyH * 0.19f

    // Left Stick: outer knurled ring + recessed dark well + glowing #00F5D4 Cyan center cap
    drawCircle(color = Color(0xFF050B14), radius = stickRadius, center = lStickCenter)
    drawCircle(
      color = strokeForCode(KeyEvent.KEYCODE_BUTTON_THUMBL, Color(0xFF00F5D4)),
      radius = stickRadius,
      center = lStickCenter,
      style = Stroke(width = 2.2f),
    )
    drawCircle(
      color = Color(0xFF1E293B),
      radius = stickRadius * 0.72f,
      center = lStickCenter,
      style = Stroke(width = 1.4f),
    )
    drawCircle(
      color = Color(0xFF00F5D4).copy(alpha = 0.42f),
      radius = stickRadius * 0.42f,
      center = lStickCenter,
    )
    drawCircle(
      color = Color(0xFF00F5D4),
      radius = stickRadius * 0.42f,
      center = lStickCenter,
      style = Stroke(width = 1.4f),
    )

    // Right Stick: outer knurled ring + recessed dark well + glowing #C084FC Purple / #F59E0B Gold center cap
    drawCircle(color = Color(0xFF050B14), radius = stickRadius, center = rStickCenter)
    drawCircle(
      color = strokeForCode(KeyEvent.KEYCODE_BUTTON_THUMBR, Color(0xFFC084FC)),
      radius = stickRadius,
      center = rStickCenter,
      style = Stroke(width = 2.2f),
    )
    drawCircle(
      color = Color(0xFFF59E0B).copy(alpha = 0.65f),
      radius = stickRadius * 0.72f,
      center = rStickCenter,
      style = Stroke(width = 1.4f),
    )
    drawCircle(
      color = Color(0xFFC084FC).copy(alpha = 0.42f),
      radius = stickRadius * 0.42f,
      center = rStickCenter,
    )
    drawCircle(
      color = Color(0xFFC084FC),
      radius = stickRadius * 0.42f,
      center = rStickCenter,
      style = Stroke(width = 1.4f),
    )

    // 4. Beveled 4-Way D-Pad with Directional Chevrons (Lower-Left)
    val dpadCenter = Offset(cx - bodyW * 0.14f, cy + bodyH * 0.20f)
    val dArm = stickRadius * 0.76f
    val dThick = stickRadius * 0.46f
    drawRoundRect(
      color = Color(0xFF0B1528),
      topLeft = Offset(dpadCenter.x - dArm, dpadCenter.y - dThick * 0.5f),
      size = Size(dArm * 2f, dThick),
      cornerRadius = CornerRadius(3f, 3f),
    )
    drawRoundRect(
      color = Color(0xFF0B1528),
      topLeft = Offset(dpadCenter.x - dThick * 0.5f, dpadCenter.y - dArm),
      size = Size(dThick, dArm * 2f),
      cornerRadius = CornerRadius(3f, 3f),
    )
    drawRoundRect(
      color = Color(0xFF64748B),
      topLeft = Offset(dpadCenter.x - dArm, dpadCenter.y - dThick * 0.5f),
      size = Size(dArm * 2f, dThick),
      cornerRadius = CornerRadius(3f, 3f),
      style = Stroke(width = 1.3f),
    )
    drawRoundRect(
      color = Color(0xFF64748B),
      topLeft = Offset(dpadCenter.x - dThick * 0.5f, dpadCenter.y - dArm),
      size = Size(dThick, dArm * 2f),
      cornerRadius = CornerRadius(3f, 3f),
      style = Stroke(width = 1.3f),
    )
    // Directional chevrons on D-Pad arms
    val chevCol = Color(0xFF38BDF8)
    val chevOff = dArm * 0.62f
    drawCircle(color = chevCol, radius = 2.0f, center = Offset(dpadCenter.x, dpadCenter.y - chevOff))
    drawCircle(color = chevCol, radius = 2.0f, center = Offset(dpadCenter.x, dpadCenter.y + chevOff))
    drawCircle(color = chevCol, radius = 2.0f, center = Offset(dpadCenter.x - chevOff, dpadCenter.y))
    drawCircle(color = chevCol, radius = 2.0f, center = Offset(dpadCenter.x + chevOff, dpadCenter.y))

    // 5. 4 Color-Coded Face Buttons (Y = Top, X = Left, B = Right, A = Bottom)
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
        Triple(KeyEvent.KEYCODE_BUTTON_X, xPos, Color(0xFF00F5D4) to "X"),
        Triple(KeyEvent.KEYCODE_BUTTON_B, bPos, Color(0xFFEF4444) to "B"),
        Triple(KeyEvent.KEYCODE_BUTTON_A, aPos, Color(0xFF38BDF8) to "A"),
      )
    for ((code, pos, colAndLetter) in faceButtons) {
      val (col, letter) = colAndLetter
      val isHi = highlightedCode == code
      val isBound = RiftCalibrationState.getActionForBinding(code) != null
      drawCircle(
        color = if (isHi) Color(0xFF422806) else Color(0xFF0A1424),
        radius = btnR,
        center = pos,
      )
      drawCircle(
        color = strokeForCode(code, if (isBound) col else col.copy(alpha = 0.65f)),
        radius = btnR,
        center = pos,
        style = Stroke(width = if (isHi) 3.2f else if (isBound) 2.2f else 1.6f),
      )
      drawContext.canvas.nativeCanvas.apply {
        labelPaint.color = android.graphics.Color.WHITE
        labelPaint.textSize = btnR * 1.15f
        labelPaint.textAlign = Paint.Align.CENTER
        drawText(letter, pos.x, pos.y + btnR * 0.40f, labelPaint)
      }
    }

    // 6. Clean 2-Segment Angled Leader Callout Lines with Glowing Origin Dots
    fun drawCalloutLabel(
      anchor: Offset,
      elbowPos: Offset,
      calloutPos: Offset,
      text: String,
      argbColor: Int,
      alignLeft: Boolean,
    ) {
      val cCol = Color(argbColor)
      drawCircle(color = cCol, radius = 2.6f, center = anchor)
      drawLine(
        color = cCol.copy(alpha = 0.78f),
        start = anchor,
        end = elbowPos,
        strokeWidth = 1.4f,
      )
      drawLine(
        color = cCol.copy(alpha = 0.78f),
        start = elbowPos,
        end = calloutPos,
        strokeWidth = 1.4f,
      )
      drawContext.canvas.nativeCanvas.apply {
        labelPaint.color = argbColor
        labelPaint.textSize = (h * 0.046f).coerceIn(13f, 20f)
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
      elbowPos = Offset(w * 0.30f, h * 0.13f),
      calloutPos = Offset(w * 0.25f, h * 0.13f),
      text = "LT/L2: $ltAction",
      argbColor = android.graphics.Color.parseColor("#00F5D4"),
      alignLeft = false,
    )
    drawCalloutLabel(
      anchor = l1Center,
      elbowPos = Offset(w * 0.30f, h * 0.25f),
      calloutPos = Offset(w * 0.25f, h * 0.25f),
      text = "L1/LB: $l1Action",
      argbColor = android.graphics.Color.parseColor("#CBD5E1"),
      alignLeft = false,
    )
    drawCalloutLabel(
      anchor = lStickCenter,
      elbowPos = Offset(w * 0.30f, h * 0.50f),
      calloutPos = Offset(w * 0.25f, h * 0.50f),
      text = "L-STICK: FLIGHT & HEADING",
      argbColor = android.graphics.Color.parseColor("#00F5D4"),
      alignLeft = false,
    )
    drawCalloutLabel(
      anchor = dpadCenter,
      elbowPos = Offset(w * 0.30f, h * 0.80f),
      calloutPos = Offset(w * 0.25f, h * 0.80f),
      text = "D-PAD: ZOOM / HANGAR",
      argbColor = android.graphics.Color.parseColor("#94A3B8"),
      alignLeft = false,
    )

    // Right Side Callouts
    drawCalloutLabel(
      anchor = rtCenter,
      elbowPos = Offset(w * 0.70f, h * 0.12f),
      calloutPos = Offset(w * 0.75f, h * 0.12f),
      text = "RT/R2: $rtAction",
      argbColor = android.graphics.Color.parseColor("#F59E0B"),
      alignLeft = true,
    )
    drawCalloutLabel(
      anchor = r1Center,
      elbowPos = Offset(w * 0.70f, h * 0.23f),
      calloutPos = Offset(w * 0.75f, h * 0.23f),
      text = "R1/RB: $r1Action",
      argbColor = android.graphics.Color.parseColor("#00F5D4"),
      alignLeft = true,
    )
    drawCalloutLabel(
      anchor = yPos,
      elbowPos = Offset(w * 0.71f, h * 0.35f),
      calloutPos = Offset(w * 0.75f, h * 0.35f),
      text = "Y: $yAction",
      argbColor = android.graphics.Color.parseColor("#FACC15"),
      alignLeft = true,
    )
    drawCalloutLabel(
      anchor = xPos,
      elbowPos = Offset(w * 0.71f, h * 0.46f),
      calloutPos = Offset(w * 0.75f, h * 0.46f),
      text = "X: $xAction",
      argbColor = android.graphics.Color.parseColor("#00F5D4"),
      alignLeft = true,
    )
    drawCalloutLabel(
      anchor = bPos,
      elbowPos = Offset(w * 0.71f, h * 0.57f),
      calloutPos = Offset(w * 0.75f, h * 0.57f),
      text = "B: $bAction",
      argbColor = android.graphics.Color.parseColor("#EF4444"),
      alignLeft = true,
    )
    drawCalloutLabel(
      anchor = aPos,
      elbowPos = Offset(w * 0.71f, h * 0.68f),
      calloutPos = Offset(w * 0.75f, h * 0.68f),
      text = "A: $aAction",
      argbColor = android.graphics.Color.parseColor("#38BDF8"),
      alignLeft = true,
    )
    drawCalloutLabel(
      anchor = rStickCenter,
      elbowPos = Offset(w * 0.70f, h * 0.82f),
      calloutPos = Offset(w * 0.75f, h * 0.82f),
      text = "R-STICK: 360° AIM",
      argbColor = android.graphics.Color.parseColor("#C084FC"),
      alignLeft = true,
    )
  }
}
