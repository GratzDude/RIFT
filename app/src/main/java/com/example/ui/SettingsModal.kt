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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.LocalPilotPrefs
import com.example.data.RiftBillingManager
import com.example.engine.MusicSynthArchetype
import com.example.engine.RiftCalibrationState
import com.example.engine.RiftSpriteBaker
import com.example.engine.RiftVisualForgeState
import java.util.Locale
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private val Mono = FontFamily.Monospace
private val Sans = FontFamily.SansSerif

private data class ControllerActionSlotInfo(
  val slotId: String,
  val title: String,
  val subtitle: String,
  val accentColor: Color,
)

private val REMAPPABLE_ACTION_SLOTS =
  listOf(
    ControllerActionSlotInfo(
      "PRIMARY_FIRE",
      "MAIN ATTACK",
      "Hold to fire primary weapon (aims via R-Stick or L-Stick heading)",
      Color(0xFFEF4444),
    ),
    ControllerActionSlotInfo(
      "SPECIAL_BUFF",
      "BUFF ATTACK (4TH SLOT)",
      "Fires equipped 0 Flux Special Buff (Nova, Shatter, Seeker, Stasis, etc.)",
      Color(0xFF34D399),
    ),
    ControllerActionSlotInfo(
      "RETRO_BRAKE",
      "MANUAL RETRO-BRAKE",
      "Hold to damp Newtonian drift (releasing Left Stick coasts indefinitely)",
      Color(0xFF38BDF8),
    ),
    ControllerActionSlotInfo(
      "SECONDARY_ABILITY",
      "SECONDARY ABILITY",
      "Instant 1-press Phase Dash, Cloak/EMP, Stasis, or Warhead",
      Color(0xFFF59E0B),
    ),
    ControllerActionSlotInfo(
      "REPEL",
      "REPEL SHOCKWAVE",
      "Triggers 360° radial repulsion wave (320 Flux)",
      Color(0xFF60A5FA),
    ),
    ControllerActionSlotInfo(
      "MULTI_TOGGLE",
      "MULTI-SHOT TOGGLE",
      "Toggles innate Split Multi-Shot on Suppressor, Twinfang & Rift Frame",
      Color(0xFFFACC15),
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

private fun DrawScope.drawSettingsBrackets(
  pad: Float,
  tick: Float,
  strokeW: Float,
  leftCol: Color,
  rightCol: Color = leftCol,
) {
  drawLine(leftCol, Offset(pad, pad), Offset(pad + tick, pad), strokeW)
  drawLine(leftCol, Offset(pad, pad), Offset(pad, pad + tick), strokeW)
  drawLine(leftCol, Offset(pad, size.height - pad), Offset(pad + tick, size.height - pad), strokeW)
  drawLine(leftCol, Offset(pad, size.height - pad - tick), Offset(pad, size.height - pad), strokeW)
  drawLine(rightCol, Offset(size.width - pad - tick, pad), Offset(size.width - pad, pad), strokeW)
  drawLine(rightCol, Offset(size.width - pad, pad), Offset(size.width - pad, pad + tick), strokeW)
  drawLine(
    rightCol,
    Offset(size.width - pad - tick, size.height - pad),
    Offset(size.width - pad, size.height - pad),
    strokeW,
  )
  drawLine(
    rightCol,
    Offset(size.width - pad, size.height - pad - tick),
    Offset(size.width - pad, size.height - pad),
    strokeW,
  )
}

@Composable
private fun VectorHelmWheelGlyph(accent: Color, sizeDp: Dp = 15.dp) {
  Canvas(modifier = Modifier.size(sizeDp)) {
    val cx = size.width * 0.5f
    val cy = size.height * 0.5f
    val r = size.minDimension * 0.42f
    drawCircle(accent, r, Offset(cx, cy), style = Stroke(width = 1.4.dp.toPx()))
    drawCircle(Color.White, r * 0.24f, Offset(cx, cy))
    for (i in 0 until 6) {
      val a = Math.toRadians(i * 60.0)
      drawLine(
        accent,
        Offset(cx + cos(a).toFloat() * (r * 0.28f), cy + sin(a).toFloat() * (r * 0.28f)),
        Offset(cx + cos(a).toFloat() * (r * 0.96f), cy + sin(a).toFloat() * (r * 0.96f)),
        strokeWidth = 1.1.dp.toPx(),
      )
    }
  }
}

@Composable
private fun SettingsBeveledButton(
  label: String,
  topColor: Color,
  bottomColor: Color,
  borderColor: Color,
  textColor: Color,
  isCompactPhone: Boolean,
  modifier: Modifier = Modifier,
  heightOverride: Dp? = null,
  testTag: String? = null,
  onClick: () -> Unit,
) {
  val h = heightOverride ?: if (isCompactPhone) 28.dp else 30.dp
  Box(
    modifier =
      modifier
        .height(h)
        .clip(RoundedCornerShape(8.dp))
        .background(Brush.verticalGradient(listOf(topColor, bottomColor)))
        .border(1.dp, borderColor, RoundedCornerShape(8.dp))
        .clickable(onClick = onClick)
        .padding(horizontal = if (isCompactPhone) 8.dp else 10.dp)
        .then(if (testTag != null) Modifier.testTag(testTag) else Modifier),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      text = label,
      color = textColor,
      fontSize = if (isCompactPhone) 9.sp else 9.8.sp,
      fontWeight = FontWeight.ExtraBold,
      fontFamily = Mono,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
  }
}

@Composable
private fun HelmSubDeckPill(
  label: String,
  selected: Boolean,
  accent: Color,
  testTag: String,
  onClick: () -> Unit,
) {
  Box(
    modifier =
      Modifier.height(26.dp)
        .clip(RoundedCornerShape(6.dp))
        .background(
          if (selected) {
            Brush.verticalGradient(listOf(accent.copy(alpha = 0.34f), accent.copy(alpha = 0.16f)))
          } else {
            Brush.verticalGradient(listOf(Color(0xFF0A1424), Color(0xFF060C16)))
          }
        )
        .border(1.dp, if (selected) accent else Color(0xFF1E293B), RoundedCornerShape(6.dp))
        .clickable(onClick = onClick)
        .padding(horizontal = 9.dp)
        .testTag(testTag),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      text = label,
      color = if (selected) Color.White else Color(0xFF94A3B8),
      fontSize = 9.2.sp,
      fontWeight = FontWeight.ExtraBold,
      fontFamily = Mono,
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
  onCloseModal: () -> Unit = {},
  onOpenChangelog: () -> Unit = {},
  onOpenAudioForge: () -> Unit = {},
) {
  val context = LocalContext.current
  val isCompactPhone = LocalConfiguration.current.screenHeightDp < 480
  val pilotPrefs = remember(context) { LocalPilotPrefs(context) }
  val isSwornPilot = isGoogleAuthenticated && !pilotPrefs.isGuestBypassEnabled()
  val riftShardsBalance = remember(isGoogleAuthenticated) { pilotPrefs.getRiftShards() }
  val liveResonanceBalance = remember(isGoogleAuthenticated) { pilotPrefs.getLiveResonance() }
  val activeCallsign = remember(isGoogleAuthenticated) { pilotPrefs.getCallsign() }
  val activeSynTag =
    remember(isGoogleAuthenticated) {
      if (!isSwornPilot) {
        "RFT"
      } else {
        pilotPrefs
          .getSyndicateTag()
          .trim()
          .uppercase(Locale.US)
          .takeIf {
            it.isNotEmpty() &&
              it !in setOf("RFT", "GUEST", "FREE", "SOLO", "UNAFFILIATED", "NONE")
          } ?: ""
      }
    }
  val visualRev = RiftVisualForgeState.visualRevision
  val resonanceImage =
    remember(visualRev) { RiftSpriteBaker.resonanceIconBitmap.asImageBitmap() }
  val riftShardImage =
    remember(visualRev) { RiftSpriteBaker.riftShardIconBitmap.asImageBitmap() }
  var forgePromptDraft by remember(initialMusicPrompt) { mutableStateOf(initialMusicPrompt) }
  var subDeck by remember { mutableIntStateOf(0) }

  val shardReadoutText =
    if (RiftBillingManager.TESTER_INFINITE_SHARDS_ACTIVE) {
      "INF (999,999)"
    } else {
      "$riftShardsBalance"
    }

  val activeGraphicsTier =
    remember(
      visualRev,
      RiftVisualForgeState.pointLightEnabled,
      RiftVisualForgeState.dynamicLightIntensity,
    ) {
      when {
        !RiftVisualForgeState.pointLightEnabled ||
          RiftVisualForgeState.dynamicLightIntensity <= 0.15f -> 0
        RiftVisualForgeState.dynamicLightIntensity >= 1.15f -> 2
        else -> 1
      }
    }

  Column(
    modifier =
      Modifier.fillMaxSize()
        .padding(if (isCompactPhone) 6.dp else 8.dp)
        .testTag("settings_modal"),
    verticalArrangement = Arrangement.spacedBy(if (isCompactPhone) 6.dp else 8.dp),
  ) {
    // Top Header Row (30.dp): HELM SYSTEMS // $fps FPS + 3 Sub-Deck Pills + [VERSION LOG]
    Row(
      modifier = Modifier.fillMaxWidth().height(30.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        VectorHelmWheelGlyph(accent = Color(0xFF00F5D4), sizeDp = 15.dp)
        Text(
          text = "HELM SYSTEMS // $fps FPS",
          color = Color.White,
          fontSize = if (isCompactPhone) 11.5.sp else 13.5.sp,
          fontWeight = FontWeight.Black,
          fontFamily = Mono,
          letterSpacing = 0.5.sp,
          maxLines = 1,
        )
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
        HelmSubDeckPill(
          label = "[AUDIO & ACCOUNT]",
          selected = subDeck == 0,
          accent = Color(0xFF00F5D4),
          testTag = "settings_subdeck_audio",
        ) {
          subDeck = 0
        }
        HelmSubDeckPill(
          label = "[DISPLAY & DETAIL]",
          selected = subDeck == 1,
          accent = Color(0xFF38BDF8),
          testTag = "settings_subdeck_display",
        ) {
          subDeck = 1
        }
        HelmSubDeckPill(
          label = "[TWIN-STICK & HUD]",
          selected = subDeck == 2,
          accent = Color(0xFFC084FC),
          testTag = "settings_subdeck_controller",
        ) {
          subDeck = 2
        }
      }

      SettingsBeveledButton(
        label = "[VERSION LOG]",
        topColor = Color(0xFF102A45),
        bottomColor = Color(0xFF081728),
        borderColor = Color(0xFF00F5D4),
        textColor = Color(0xFF00F5D4),
        isCompactPhone = isCompactPhone,
        heightOverride = 28.dp,
        testTag = "settings_btn_open_changelog",
        onClick = onOpenChangelog,
      )
    }

    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
      when (subDeck) {
        // SUB-DECK 0: AUDIO & ACCOUNT (2-Column Zero-Scroll Split)
        0 -> {
          Row(
            modifier = Modifier.fillMaxSize().testTag("settings_synth_music_forge_card"),
            horizontalArrangement = Arrangement.spacedBy(if (isCompactPhone) 8.dp else 12.dp),
          ) {
            // Left Column: Sworn Pilot / [RFT] Drifter Account Link + Single Shard Icon + SFX Lab Jump
            Column(
              modifier =
                Modifier.weight(0.36f)
                  .fillMaxHeight()
                  .background(Color(0xFF0B182B), RoundedCornerShape(10.dp))
                  .border(1.dp, Color(0xFF1B3454), RoundedCornerShape(10.dp))
                  .padding(if (isCompactPhone) 8.dp else 12.dp),
              verticalArrangement = Arrangement.SpaceBetween,
            ) {
              Column(verticalArrangement = Arrangement.spacedBy(if (isCompactPhone) 5.dp else 7.dp)) {
                Text(
                  text = "PILOT IDENTITY & CLOUD UPLINK",
                  color = Color(0xFF00F5D4),
                  fontSize = if (isCompactPhone) 9.8.sp else 11.sp,
                  fontWeight = FontWeight.Black,
                  fontFamily = Mono,
                )
                Box(
                  modifier =
                    Modifier.fillMaxWidth()
                      .background(Color(0xFF060E1B), RoundedCornerShape(8.dp))
                      .border(
                        1.dp,
                        if (isGoogleAuthenticated) Color(0xFF10B981) else Color(0xFF475569),
                        RoundedCornerShape(8.dp),
                      )
                      .padding(horizontal = 10.dp, vertical = if (isCompactPhone) 6.dp else 8.dp)
                ) {
                  Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                      Box(
                        modifier =
                          Modifier.size(7.dp)
                            .clip(CircleShape)
                            .background(
                              if (isGoogleAuthenticated) Color(0xFF10B981) else Color(0xFFF59E0B)
                            )
                      )
                      Text(
                        text =
                          if (isSwornPilot) "SWORN PILOT LINKED"
                          else "[RFT] DRIFTER SESSION",
                        color =
                          if (isSwornPilot) Color(0xFF34D399) else Color(0xFFFACC15),
                        fontSize = 8.8.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = Mono,
                      )
                    }
                    Text(
                      text =
                        if (activeSynTag.isNotEmpty()) "[$activeSynTag] $activeCallsign"
                        else activeCallsign,
                      color = Color.White,
                      fontSize = if (isCompactPhone) 12.sp else 14.sp,
                      fontWeight = FontWeight.Black,
                      fontFamily = Sans,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                    )
                  }
                }

                // Custom Resonance Crest + Single Prismatic Rift Shard Gem (ZERO plain RES / duplicate ❖!)
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(6.dp),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Row(
                    modifier =
                      Modifier.weight(0.85f)
                        .background(Color(0xFF171308), RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0xFFFACC15), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = if (isCompactPhone) 5.dp else 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                  ) {
                    Image(
                      bitmap = resonanceImage,
                      contentDescription = "Resonance",
                      filterQuality = FilterQuality.High,
                      modifier = Modifier.size(14.dp),
                    )
                    Text(
                      text = "$liveResonanceBalance",
                      color = Color(0xFFFEF08A),
                      fontSize = if (isCompactPhone) 9.8.sp else 10.8.sp,
                      fontWeight = FontWeight.Black,
                      fontFamily = Mono,
                      maxLines = 1,
                    )
                  }

                  Row(
                    modifier =
                      Modifier.weight(1.15f)
                        .background(Color(0xFF140B29), RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0xFFA855F7), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = if (isCompactPhone) 5.dp else 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                  ) {
                    Image(
                      bitmap = riftShardImage,
                      contentDescription = "Rift Shards",
                      filterQuality = FilterQuality.High,
                      modifier = Modifier.size(14.dp),
                    )
                    Text(
                      text = shardReadoutText,
                      color = Color(0xFFE9D5FF),
                      fontSize = if (isCompactPhone) 9.6.sp else 10.6.sp,
                      fontWeight = FontWeight.Black,
                      fontFamily = Mono,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                    )
                  }
                }
              }

              Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SettingsBeveledButton(
                  label = "[TRACK A–C & SFX LAB]",
                  topColor = Color(0xFF281242),
                  bottomColor = Color(0xFF150926),
                  borderColor = Color(0xFFC084FC),
                  textColor = Color(0xFFE9D5FF),
                  isCompactPhone = isCompactPhone,
                  modifier = Modifier.fillMaxWidth(),
                  heightOverride = if (isCompactPhone) 32.dp else 36.dp,
                  testTag = "settings_btn_open_audio_forge",
                  onClick = onOpenAudioForge,
                )
                if (isGoogleAuthenticated) {
                  SettingsBeveledButton(
                    label = "SIGN OUT",
                    topColor = Color(0xFF1E2D45),
                    bottomColor = Color(0xFF111B2B),
                    borderColor = Color(0xFF475569),
                    textColor = Color(0xFFCBD5E1),
                    isCompactPhone = isCompactPhone,
                    modifier = Modifier.fillMaxWidth(),
                    heightOverride = if (isCompactPhone) 32.dp else 36.dp,
                    onClick = onRequestSignOut,
                  )
                } else {
                  SettingsBeveledButton(
                    label = "SIGN IN WITH GOOGLE",
                    topColor = Color(0xFF143D34),
                    bottomColor = Color(0xFF0A211C),
                    borderColor = Color(0xFF00F5D4),
                    textColor = Color(0xFF00F5D4),
                    isCompactPhone = isCompactPhone,
                    modifier = Modifier.fillMaxWidth(),
                    heightOverride = if (isCompactPhone) 32.dp else 36.dp,
                    onClick = onRequestGoogleSignIn,
                  )
                }
              }
            }

            // Right Column: Music/SFX Toggles, BPM Slider, Volume Sliders, 5 Genre Chips, 36.dp Forge Bar
            Column(
              modifier =
                Modifier.weight(0.64f)
                  .fillMaxHeight()
                  .background(Color(0xFF0B182B), RoundedCornerShape(10.dp))
                  .border(1.dp, Color(0xFF1B3454), RoundedCornerShape(10.dp))
                  .padding(if (isCompactPhone) 8.dp else 12.dp),
              verticalArrangement = Arrangement.SpaceBetween,
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp),
                  modifier = Modifier.weight(1f).padding(end = 6.dp),
                ) {
                  Text(
                    text = "SYNTH ENGINE",
                    color = Color(0xFF00F5D4),
                    fontSize = if (isCompactPhone) 10.sp else 11.5.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = Mono,
                  )
                  Box(
                    modifier =
                      Modifier.background(Color(0xFF071324), RoundedCornerShape(5.dp))
                        .border(1.dp, Color(0x6600F5D4), RoundedCornerShape(5.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                  ) {
                    Text(
                      text = activeMusicSummary.replace("\u26A1", "Flux"),
                      color = Color(0xFF7DD3FC),
                      fontSize = 8.5.sp,
                      fontWeight = FontWeight.Bold,
                      fontFamily = Mono,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                    )
                  }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                  SettingsBeveledButton(
                    label = if (musicEnabled) "MUSIC: ON" else "MUSIC: OFF",
                    topColor = if (musicEnabled) Color(0xFF0F2D46) else Color(0xFF361620),
                    bottomColor = if (musicEnabled) Color(0xFF081A2A) else Color(0xFF1F0B11),
                    borderColor = if (musicEnabled) Color(0xFF00F5D4) else Color(0xFFFF3B4E),
                    textColor = if (musicEnabled) Color(0xFF00F5D4) else Color(0xFFFF6B7A),
                    isCompactPhone = isCompactPhone,
                    heightOverride = 26.dp,
                    testTag = "settings_music_toggle_button",
                    onClick = onToggleMusic,
                  )
                  SettingsBeveledButton(
                    label = if (sfxEnabled) "SFX: ON" else "SFX: OFF",
                    topColor = if (sfxEnabled) Color(0xFF103328) else Color(0xFF361620),
                    bottomColor = if (sfxEnabled) Color(0xFF081C16) else Color(0xFF1F0B11),
                    borderColor = if (sfxEnabled) Color(0xFF00E676) else Color(0xFFFF3B4E),
                    textColor = if (sfxEnabled) Color(0xFF00E676) else Color(0xFFFF6B7A),
                    isCompactPhone = isCompactPhone,
                    heightOverride = 26.dp,
                    testTag = "settings_sfx_toggle_button",
                    onClick = onToggleSfx,
                  )
                }
              }

              // Sliders Row: Tempo (Left) + Music/SFX Volume (Right)
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Column(
                  modifier =
                    Modifier.weight(1f)
                      .background(Color(0xFF071120), RoundedCornerShape(8.dp))
                      .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(8.dp))
                      .padding(horizontal = 8.dp, vertical = 5.dp),
                  verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    Text(
                      text = "TEMPO: $musicBpm BPM",
                      color = Color(0xFFFBBF24),
                      fontSize = 9.2.sp,
                      fontWeight = FontWeight.Black,
                      fontFamily = Mono,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                      listOf(90, 118, 135, 155).forEach { bpmPreset ->
                        val isPresetActive = abs(musicBpm - bpmPreset) <= 1
                        Box(
                          modifier =
                            Modifier.clip(RoundedCornerShape(4.dp))
                              .background(
                                if (isPresetActive) Color(0xFFF59E0B) else Color(0xFF132642)
                              )
                              .border(
                                1.dp,
                                if (isPresetActive) Color(0xFFFBBF24) else Color(0x44F59E0B),
                                RoundedCornerShape(4.dp),
                              )
                              .clickable { onChangeMusicBpm(bpmPreset) }
                              .padding(horizontal = 4.5.dp, vertical = 1.5.dp)
                        ) {
                          Text(
                            text = "$bpmPreset",
                            color = if (isPresetActive) Color(0xFF030712) else Color.White,
                            fontSize = 7.8.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = Mono,
                          )
                        }
                      }
                    }
                  }
                  Slider(
                    value = musicBpm.coerceIn(60, 180).toFloat(),
                    onValueChange = { onChangeMusicBpm(it.roundToInt().coerceIn(60, 180)) },
                    valueRange = 60f..180f,
                    colors =
                      SliderDefaults.colors(
                        thumbColor = Color(0xFFF59E0B),
                        activeTrackColor = Color(0xFFF59E0B),
                        inactiveTrackColor = Color(0xFF132642),
                      ),
                    modifier =
                      Modifier.fillMaxWidth().height(22.dp).testTag("settings_music_bpm_slider"),
                  )
                  Text(
                    text = "60 BPM Ambient <--> 180 BPM Overdrive",
                    color = Color(0xFF8A99AD),
                    fontSize = 8.sp,
                    fontFamily = Mono,
                    maxLines = 1,
                  )
                }

                Column(
                  modifier =
                    Modifier.weight(1f)
                      .background(Color(0xFF071120), RoundedCornerShape(8.dp))
                      .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(8.dp))
                      .padding(horizontal = 8.dp, vertical = 5.dp),
                  verticalArrangement = Arrangement.spacedBy(1.dp),
                ) {
                  Text(
                    text = "MUSIC VOL: ${(musicVolume * 100f).roundToInt()}%",
                    color = Color(0xFF00F5D4),
                    fontSize = 8.8.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = Mono,
                  )
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
                      Modifier.fillMaxWidth().height(20.dp).testTag("settings_music_volume_slider"),
                  )
                  Text(
                    text = "SFX VOL: ${(sfxVolume * 100f).roundToInt()}%",
                    color = Color(0xFF38BDF8),
                    fontSize = 8.8.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = Mono,
                  )
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
                      Modifier.fillMaxWidth().height(20.dp).testTag("settings_sfx_volume_slider"),
                  )
                }
              }

              // 5 Synth Genre Preset Chips + Unclipped 36.dp Prompt-to-Synth Track Forge Bar
              Column(
                modifier =
                  Modifier.fillMaxWidth()
                    .background(Color(0xFF060E1B), RoundedCornerShape(8.dp))
                    .border(1.dp, Color(0xFF1E3A5F), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp),
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                  horizontalArrangement = Arrangement.spacedBy(5.dp),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Text(
                    text = "GENRES:",
                    color = Color(0xFF94A3B8),
                    fontSize = 8.2.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = Mono,
                  )
                  MusicSynthArchetype.entries.forEach { archetype ->
                    val isSelected =
                      activeMusicArchetypeId.equals(archetype.id, ignoreCase = true) &&
                        forgePromptDraft.isBlank()
                    Box(
                      modifier =
                        Modifier.clip(RoundedCornerShape(6.dp))
                          .background(
                            Brush.verticalGradient(
                              if (isSelected) listOf(Color(0xFF0E4F5C), Color(0xFF072B36))
                              else listOf(Color(0xFF102038), Color(0xFF091324))
                            )
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
                          .padding(horizontal = 7.dp, vertical = 3.dp)
                          .testTag("settings_music_archetype_${archetype.id}"),
                      contentAlignment = Alignment.Center,
                    ) {
                      Text(
                        text = "${archetype.label.replace("\u26A1", "")} (${archetype.defaultBpm})",
                        color = if (isSelected) Color(0xFF00F5D4) else Color(0xFFE2E8F0),
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = Sans,
                        maxLines = 1,
                      )
                    }
                  }
                }

                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(6.dp),
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
                          "Prompt custom battle synth (e.g. dark acid bassline, cosmic pad, retro boss)...",
                        color = Color(0xFF64748B),
                        fontSize = 9.5.sp,
                        fontFamily = Sans,
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
                          fontSize = 10.5.sp,
                          fontWeight = FontWeight.Medium,
                          fontFamily = Sans,
                        ),
                      modifier = Modifier.fillMaxWidth().testTag("settings_music_forge_input"),
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
                        .clickable { onForgeCustomMusicPrompt(forgePromptDraft) }
                        .padding(horizontal = 12.dp)
                        .testTag("settings_music_forge_btn"),
                    contentAlignment = Alignment.Center,
                  ) {
                    Text(
                      text = "FORGE TRACK",
                      color = Color(0xFFF3E8FF),
                      fontSize = 9.5.sp,
                      fontWeight = FontWeight.ExtraBold,
                      fontFamily = Mono,
                      maxLines = 1,
                    )
                  }
                }
              }
            }
          }
        }

        // SUB-DECK 1: DISPLAY & DETAIL (2-Column Zero-Scroll Split)
        1 -> {
          Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(if (isCompactPhone) 8.dp else 12.dp),
          ) {
            // Left Column: Camera Zoom / FOV + GRAPHICS DETAIL: LOW | MEDIUM | HIGH
            Column(
              modifier =
                Modifier.weight(1f)
                  .fillMaxHeight()
                  .background(Color(0xFF0B182B), RoundedCornerShape(10.dp))
                  .border(1.dp, Color(0xFF1B3454), RoundedCornerShape(10.dp))
                  .padding(if (isCompactPhone) 8.dp else 12.dp),
              verticalArrangement = Arrangement.SpaceBetween,
            ) {
              Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                val zoomMode =
                  when {
                    cameraZoom <= 0.65f -> "TACTICAL OVERVIEW"
                    cameraZoom >= 1.25f -> "DOGFIGHT CLOSE-UP"
                    else -> "STANDARD ARENA"
                  }
                Text(
                  text =
                    "CAMERA ZOOM / ARENA FOV: ${String.format(Locale.US, "%.2fx", cameraZoom)} ($zoomMode)",
                  color = Color(0xFF00F5D4),
                  fontSize = if (isCompactPhone) 10.5.sp else 12.sp,
                  fontWeight = FontWeight.Black,
                  fontFamily = Mono,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                  listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f).forEach { preset ->
                    val isSelected = abs(cameraZoom - preset) <= 0.05f
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
                          .padding(horizontal = 8.dp, vertical = 3.5.dp)
                    ) {
                      Text(
                        text = "${preset}x",
                        color = if (isSelected) Color(0xFF030712) else Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = Mono,
                      )
                    }
                  }
                }
                Slider(
                  value = cameraZoom.coerceIn(0.5f, 1.5f),
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
                    Modifier.fillMaxWidth().height(26.dp).testTag("settings_camera_zoom_slider"),
                )
              }

              Column(
                modifier =
                  Modifier.fillMaxWidth()
                    .background(Color(0xFF060E1B), RoundedCornerShape(8.dp))
                    .border(1.dp, Color(0xFF1E3A5F), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                Text(
                  text = "GRAPHICS DETAIL // DYNAMIC POINT LIGHTING",
                  color = Color(0xFF38BDF8),
                  fontSize = if (isCompactPhone) 9.5.sp else 10.5.sp,
                  fontWeight = FontWeight.Black,
                  fontFamily = Mono,
                )
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                  val presets =
                    listOf(
                      Triple(0, "LOW", "120HZ MAX FPS"),
                      Triple(1, "MEDIUM", "BALANCED"),
                      Triple(2, "HIGH", "FULL VOLUMETRIC"),
                    )
                  presets.forEach { (tierIdx, tierLabel, subText) ->
                    val selected = activeGraphicsTier == tierIdx
                    val accent =
                      when (tierIdx) {
                        0 -> Color(0xFF38BDF8)
                        1 -> Color(0xFF00F5D4)
                        else -> Color(0xFFC084FC)
                      }
                    Column(
                      modifier =
                        Modifier.weight(1f)
                          .clip(RoundedCornerShape(7.dp))
                          .background(
                            if (selected) accent.copy(alpha = 0.22f) else Color(0xFF0B1628)
                          )
                          .border(
                            1.2.dp,
                            if (selected) accent else Color(0xFF334155),
                            RoundedCornerShape(7.dp),
                          )
                          .clickable {
                            when (tierIdx) {
                              0 -> {
                                RiftVisualForgeState.pointLightEnabled = false
                                RiftVisualForgeState.dynamicLightIntensity = 0.0f
                              }
                              1 -> {
                                RiftVisualForgeState.pointLightEnabled = true
                                RiftVisualForgeState.dynamicLightIntensity = 0.85f
                                RiftVisualForgeState.projectileLightRadius = 0.9f
                              }
                              else -> {
                                RiftVisualForgeState.pointLightEnabled = true
                                RiftVisualForgeState.dynamicLightIntensity = 1.35f
                                RiftVisualForgeState.projectileLightRadius = 1.25f
                              }
                            }
                            RiftVisualForgeState.visualRevision++
                            RiftCalibrationState.savePersistedLayout(context)
                          }
                          .padding(vertical = 6.dp, horizontal = 4.dp)
                          .testTag("settings_gfx_${tierLabel.lowercase(Locale.US)}"),
                      horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                      Text(
                        text = tierLabel,
                        color = if (selected) Color.White else Color(0xFFCBD5E1),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = Mono,
                      )
                      Text(
                        text = subText,
                        color = if (selected) accent else Color(0xFF64748B),
                        fontSize = 7.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = Mono,
                        maxLines = 1,
                      )
                    }
                  }
                }
              }
            }

            // Right Column: Playfield Text Scale (75% - 175%) + Presets + Live Dark-Space Preview
            Column(
              modifier =
                Modifier.weight(1f)
                  .fillMaxHeight()
                  .background(Color(0xFF0B182B), RoundedCornerShape(10.dp))
                  .border(1.dp, Color(0xFF1B3454), RoundedCornerShape(10.dp))
                  .padding(if (isCompactPhone) 8.dp else 12.dp),
              verticalArrangement = Arrangement.SpaceBetween,
            ) {
              Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                  text = "PLAYFIELD TEXT SCALE: ${(playfieldTextScale * 100f).roundToInt()}%",
                  color = Color(0xFF00F5D4),
                  fontSize = if (isCompactPhone) 10.5.sp else 12.sp,
                  fontWeight = FontWeight.Black,
                  fontFamily = Mono,
                )
                Text(
                  text = "Scales world callsigns, bounties, Rift callouts, and Stasis timers.",
                  color = Color(0xFF8A99AD),
                  fontSize = if (isCompactPhone) 9.sp else 10.sp,
                  fontFamily = Sans,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                  listOf(0.85f to "85%", 1.0f to "100%", 1.25f to "125%", 1.50f to "150%").forEach {
                    (preset, label) ->
                    val isSelected = abs(playfieldTextScale - preset) <= 0.04f
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
                          .padding(horizontal = 9.dp, vertical = 3.5.dp)
                    ) {
                      Text(
                        text = label,
                        color = if (isSelected) Color(0xFF030712) else Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = Mono,
                      )
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
                    Modifier.fillMaxWidth().height(26.dp).testTag("settings_text_scale_slider"),
                )
              }

              Box(
                modifier =
                  Modifier.fillMaxWidth()
                    .height(if (isCompactPhone) 68.dp else 88.dp)
                    .background(Color(0xFF030712), RoundedCornerShape(8.dp))
                    .border(1.dp, Color(0x4400F5D4), RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center,
              ) {
                Canvas(modifier = Modifier.matchParentSize()) {
                  drawSettingsBrackets(
                    4.dp.toPx(),
                    8.dp.toPx(),
                    1.4.dp.toPx(),
                    Color(0xFF00F5D4).copy(alpha = 0.55f),
                  )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                  Text(
                    text = "LIVE ARENA CALLSIGN PREVIEW",
                    color = Color(0xFF64748B),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = Mono,
                  )
                  Spacer(modifier = Modifier.height(3.dp))
                  Text(
                    text = "[SYN] VIPER_ACE • BTY 350",
                    color = Color(0xFF38BDF8),
                    fontSize = (12.5f * playfieldTextScale.coerceIn(0.75f, 1.75f)).sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = Mono,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                }
              }
            }
          }
        }

        // SUB-DECK 2: TWIN-STICK & HUD (Full-Width Side-by-Side Controller Schematic + 6 Action Binds)
        else -> {
          ControllerDiagramAndMapperCard(context = context, modifier = Modifier.fillMaxSize())
        }
      }
    }
  }
}

@Composable
fun ControllerDiagramAndMapperCard(context: Context, modifier: Modifier = Modifier) {
  val isCompactPhone = LocalConfiguration.current.screenHeightDp < 480
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
      animationSpec = infiniteRepeatable(tween(520), RepeatMode.Reverse),
      label = "pulseAlpha",
    )

  LaunchedEffect(listeningSlot) {
    if (listeningSlot != null) runCatching { mapperFocusRequester.requestFocus() }
  }

  val activeListeningInfo =
    remember(listeningSlot) { REMAPPABLE_ACTION_SLOTS.find { it.slotId == listeningSlot } }
  val highlightedHardwareCode =
    remember(listeningSlot, rev) {
      listeningSlot?.let { RiftCalibrationState.getBindingForActionSlot(it) }
    }

  Row(
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
              val slotTitle =
                REMAPPABLE_ACTION_SLOTS.find { it.slotId == slot }?.title ?: slot
              feedbackBanner =
                "BOUND [$slotTitle] TO [${RiftCalibrationState.getBindingLabel(code)}]"
              listeningSlot = null
              true
            } else false
          } else false
        }
        .padding(if (isCompactPhone) 8.dp else 10.dp)
        .testTag("controller_diagram_and_mapper_card"),
    horizontalArrangement = Arrangement.spacedBy(if (isCompactPhone) 8.dp else 10.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    // Left (0.50f): Contoured Vector Twin-Stick Gamepad Schematic
    TwinStickControllerSchematicCanvas(
      modifier =
        Modifier.weight(0.50f)
          .fillMaxHeight()
          .background(Color(0xFF050B14), RoundedCornerShape(10.dp))
          .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(10.dp))
          .testTag("controller_schematic_canvas"),
      revision = rev,
      highlightedCode = highlightedHardwareCode,
      pulseAlpha = pulseAlpha,
    )

    // Right (0.50f): Status Header + [RESET BINDS] + 2x3 Action Slot Grid + Hardware Bind Chips
    Column(
      modifier = Modifier.weight(0.50f).fillMaxHeight(),
      verticalArrangement = Arrangement.SpaceBetween,
    ) {
      Row(
        modifier =
          Modifier.fillMaxWidth()
            .background(Color(0xFF050B14), RoundedCornerShape(8.dp))
            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          modifier = Modifier.weight(1f),
        ) {
          Box(
            modifier =
              Modifier.size(7.dp)
                .clip(CircleShape)
                .background(if (isGamepadConnected) Color(0xFF22C55E) else Color(0xFF94A3B8))
          )
          Text(
            text =
              if (isGamepadConnected) "GAMEPAD LINKED" else "TOUCH HUD + GAMEPAD READY",
            color = if (isGamepadConnected) Color(0xFF4ADE80) else Color(0xFF00F5D4),
            fontSize = if (isCompactPhone) 8.8.sp else 9.8.sp,
            fontWeight = FontWeight.Black,
            fontFamily = Mono,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
        SettingsBeveledButton(
          label = "[RESET BINDS]",
          topColor = Color(0xFF382012),
          bottomColor = Color(0xFF1F1008),
          borderColor = Color(0xFFF59E0B),
          textColor = Color(0xFFFBBF24),
          isCompactPhone = isCompactPhone,
          heightOverride = 24.dp,
          testTag = "btn_reset_default_controller_binds",
          onClick = {
            RiftCalibrationState.resetControllerBindingsToDefault(context)
            listeningSlot = null
            feedbackBanner =
              "RESTORED DEFAULT BINDS (RT=MAIN, R1=BUFF, LT=BRAKE, X=SEC, A=REPEL, Y=MULTI)"
          },
        )
      }

      if (activeListeningInfo != null) {
        Row(
          modifier =
            Modifier.fillMaxWidth()
              .background(Color(0xFF2B1D06), RoundedCornerShape(7.dp))
              .border(1.2.dp, Color(0xFFFACC15), RoundedCornerShape(7.dp))
              .padding(horizontal = 8.dp, vertical = 4.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = "PRESS KEY OR TAP CHIP FOR [${activeListeningInfo.title}]...",
            color = Color(0xFFFEF08A),
            fontSize = 8.5.sp,
            fontWeight = FontWeight.Black,
            fontFamily = Mono,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
          )
          Text(
            text = "[CANCEL]",
            color = Color(0xFFF87171),
            fontSize = 8.5.sp,
            fontWeight = FontWeight.Black,
            fontFamily = Mono,
            modifier = Modifier.padding(start = 6.dp).clickable { listeningSlot = null },
          )
        }
      } else if (feedbackBanner != null) {
        Row(
          modifier =
            Modifier.fillMaxWidth()
              .background(Color(0xFF07261E), RoundedCornerShape(7.dp))
              .border(1.dp, Color(0xFF10B981), RoundedCornerShape(7.dp))
              .padding(horizontal = 8.dp, vertical = 4.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = feedbackBanner ?: "",
            color = Color(0xFF6EE7B7),
            fontSize = 8.5.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = Mono,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
          )
          Text(
            text = "[X]",
            color = Color(0xFF94A3B8),
            fontSize = 8.5.sp,
            fontFamily = Mono,
            modifier = Modifier.padding(start = 6.dp).clickable { feedbackBanner = null },
          )
        }
      }

      // 2x3 Remappable Action Slot Grid
      Column(
        modifier = Modifier.weight(1f).fillMaxWidth().padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
      ) {
        REMAPPABLE_ACTION_SLOTS.chunked(2).forEach { pair ->
          Row(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
          ) {
            pair.forEach { slotInfo ->
              val isListening = listeningSlot == slotInfo.slotId
              val boundCode =
                remember(rev, slotInfo.slotId) {
                  RiftCalibrationState.getBindingForActionSlot(slotInfo.slotId)
                }
              val boundLabel = remember(boundCode) { RiftCalibrationState.getBindingLabel(boundCode) }
              val bracketColor =
                if (slotInfo.slotId == "PRIMARY_FIRE" || slotInfo.slotId == "MULTI_TOGGLE") {
                  Color(0xFFF59E0B)
                } else {
                  Color(0xFF00F5D4)
                }

              Box(
                modifier =
                  Modifier.weight(1f)
                    .fillMaxHeight()
                    .testTag("controller_bind_slot_${slotInfo.slotId}")
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                      Brush.verticalGradient(
                        if (isListening) listOf(Color(0xFF3B2806), Color(0xFF1E1403))
                        else listOf(Color(0xFF0C1A33), Color(0xFF081224))
                      )
                    )
                    .border(
                      if (isListening) 1.5.dp else 1.dp,
                      if (isListening) Color(0xFFFACC15) else bracketColor.copy(alpha = 0.45f),
                      RoundedCornerShape(8.dp),
                    )
                    .clickable {
                      listeningSlot =
                        if (listeningSlot == slotInfo.slotId) null else slotInfo.slotId
                    }
              ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                  val cCol = if (isListening) Color(0xFFFACC15) else bracketColor
                  drawSettingsBrackets(0f, 7.dp.toPx(), 1.6.dp.toPx(), cCol)
                  val gx = size.width * 0.20f
                  val gy = size.height * 0.30f
                  val gw = 22.dp.toPx()
                  val gh = 12.dp.toPx()
                  drawRoundRect(
                    Color(0xFF050B14),
                    Offset(gx - gw * 0.5f, gy - gh * 0.5f),
                    Size(gw, gh),
                    CornerRadius(4f, 4f),
                  )
                  drawRoundRect(
                    Color(0xFF475569),
                    Offset(gx - gw * 0.5f, gy - gh * 0.5f),
                    Size(gw, gh),
                    CornerRadius(4f, 4f),
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
                  drawCircle(cCol, 2.8.dp.toPx(), dotOffset)
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
                        text = if (isListening) "PRESS..." else "[$boundLabel]",
                        color = if (isListening) Color(0xFF030712) else bracketColor,
                        fontSize = 7.8.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = Mono,
                      )
                    }
                  }
                  Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(
                      text = slotInfo.title,
                      color = Color.White,
                      fontSize = if (isCompactPhone) 8.4.sp else 9.2.sp,
                      fontWeight = FontWeight.Black,
                      fontFamily = Mono,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                      text = slotInfo.subtitle,
                      color = Color(0xFF94A3B8),
                      fontSize = 7.sp,
                      lineHeight = 8.2.sp,
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

      // Hardware Bind Chips Strip
      Row(
        modifier =
          Modifier.fillMaxWidth()
            .background(Color(0xFF060D1A), RoundedCornerShape(8.dp))
            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = if (listeningSlot != null) "BIND CHIP:" else "CHIPS:",
          color = if (listeningSlot != null) Color(0xFFFACC15) else Color(0xFF94A3B8),
          fontSize = 8.sp,
          fontWeight = FontWeight.Black,
          fontFamily = Mono,
        )
        HARDWARE_BIND_CHIPS.forEach { (hwCode, hwLabel) ->
          val actionOnChip =
            remember(rev, hwCode) { RiftCalibrationState.getActionForBinding(hwCode) }
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
                  feedbackBanner = "BOUND [$slotTitle] TO [$hwLabel]"
                  listeningSlot = null
                }
                .padding(horizontal = 6.dp, vertical = 3.dp),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text = if (actionOnChip != null) "$hwLabel ($actionOnChip)" else hwLabel,
              color = if (isHighlighted) Color(0xFFFEF08A) else Color.White,
              fontSize = 7.8.sp,
              fontWeight = FontWeight.Bold,
              fontFamily = Mono,
            )
          }
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
      RiftCalibrationState.getActionForBinding(KeyEvent.KEYCODE_BUTTON_L1) ?: "MENU TAB"
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

  val labelPaint = remember {
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
    drawSettingsBrackets(5.dp.toPx(), 10.dp.toPx(), 1.5f, Color(0xFF00F5D4).copy(alpha = 0.55f))

    fun strokeForCode(code: Int, defaultColor: Color): Color =
      if (highlightedCode == code) Color(0xFFFACC15).copy(alpha = pulseAlpha) else defaultColor

    val bodyW = w * 0.44f
    val bodyH = h * 0.48f
    val bodyLeft = cx - bodyW * 0.5f
    val bodyTop = cy - bodyH * 0.46f
    val bodyRight = cx + bodyW * 0.5f
    val bodyBottom = cy + bodyH * 0.54f
    val trigW = bodyW * 0.24f
    val trigH = h * 0.075f
    val ltCenter = Offset(bodyLeft + bodyW * 0.20f, bodyTop - trigH * 1.48f)
    val rtCenter = Offset(bodyRight - bodyW * 0.20f, bodyTop - trigH * 1.48f)
    val l1Center = Offset(bodyLeft + bodyW * 0.20f, bodyTop - trigH * 0.54f)
    val r1Center = Offset(bodyRight - bodyW * 0.20f, bodyTop - trigH * 0.54f)

    fun drawShoulder(
      code: Int,
      center: Offset,
      wScale: Float,
      hScale: Float,
      rad: Float,
      fillCol: Color,
      strokeCol: Color,
      baseStroke: Float,
    ) {
      val topLeft = Offset(center.x - trigW * wScale * 0.5f, center.y - trigH * hScale * 0.5f)
      val sz = Size(trigW * wScale, trigH * hScale)
      drawRoundRect(
        if (highlightedCode == code) Color(0xFF422806) else fillCol,
        topLeft,
        sz,
        CornerRadius(rad, rad),
      )
      drawRoundRect(
        strokeForCode(code, strokeCol),
        topLeft,
        sz,
        CornerRadius(rad, rad),
        style = Stroke(width = if (highlightedCode == code) 3.2f else baseStroke),
      )
    }
    drawShoulder(
      RiftCalibrationState.BIND_LT,
      ltCenter,
      0.96f,
      0.90f,
      7f,
      Color(0xFF0F223D),
      Color(0xFF00F5D4),
      1.8f,
    )
    drawShoulder(
      RiftCalibrationState.BIND_RT,
      rtCenter,
      0.96f,
      0.90f,
      7f,
      Color(0xFF281509),
      Color(0xFFF59E0B),
      1.8f,
    )
    drawShoulder(
      KeyEvent.KEYCODE_BUTTON_L1,
      l1Center,
      1.04f,
      0.80f,
      6f,
      Color(0xFF111E34),
      Color(0xFF94A3B8),
      1.6f,
    )
    drawShoulder(
      KeyEvent.KEYCODE_BUTTON_R1,
      r1Center,
      1.04f,
      0.80f,
      6f,
      Color(0xFF0A2823),
      Color(0xFF00F5D4),
      1.6f,
    )

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
      gamepadPath,
      Brush.verticalGradient(
        listOf(Color(0xFF1E293B), Color(0xFF0F172A)),
        startY = bodyTop,
        endY = bodyBottom + bodyH * 0.38f,
      ),
    )
    drawPath(gamepadPath, Color(0xFF38BDF8), style = Stroke(width = 2.2.dp.toPx()))

    val seamCol = Color(0xFFF59E0B).copy(alpha = 0.72f)
    drawPath(
      Path().apply {
        moveTo(bodyLeft + bodyW * 0.05f, bodyTop + bodyH * 0.16f)
        quadraticTo(
          bodyLeft - bodyW * 0.05f,
          cy + bodyH * 0.16f,
          bodyLeft + bodyW * 0.06f,
          bodyBottom + bodyH * 0.24f,
        )
      },
      seamCol,
      style = Stroke(width = 1.4.dp.toPx()),
    )
    drawPath(
      Path().apply {
        moveTo(bodyRight - bodyW * 0.05f, bodyTop + bodyH * 0.16f)
        quadraticTo(
          bodyRight + bodyW * 0.05f,
          cy + bodyH * 0.16f,
          bodyRight - bodyW * 0.06f,
          bodyBottom + bodyH * 0.24f,
        )
      },
      seamCol,
      style = Stroke(width = 1.4.dp.toPx()),
    )

    val lStickCenter = Offset(cx - bodyW * 0.25f, cy - bodyH * 0.08f)
    val rStickCenter = Offset(cx + bodyW * 0.14f, cy + bodyH * 0.20f)
    val stickRadius = bodyH * 0.19f
    fun drawThumbStick(center: Offset, code: Int, mainCol: Color, innerRingCol: Color) {
      drawCircle(Color(0xFF050B14), stickRadius, center)
      drawCircle(strokeForCode(code, mainCol), stickRadius, center, style = Stroke(width = 2.2f))
      drawCircle(innerRingCol, stickRadius * 0.72f, center, style = Stroke(width = 1.4f))
      drawCircle(mainCol.copy(alpha = 0.42f), stickRadius * 0.42f, center)
      drawCircle(mainCol, stickRadius * 0.42f, center, style = Stroke(width = 1.4f))
    }
    drawThumbStick(
      lStickCenter,
      KeyEvent.KEYCODE_BUTTON_THUMBL,
      Color(0xFF00F5D4),
      Color(0xFF1E293B),
    )
    drawThumbStick(
      rStickCenter,
      KeyEvent.KEYCODE_BUTTON_THUMBR,
      Color(0xFFC084FC),
      Color(0xFFF59E0B).copy(alpha = 0.65f),
    )

    val dpadCenter = Offset(cx - bodyW * 0.14f, cy + bodyH * 0.20f)
    val dArm = stickRadius * 0.76f
    val dThick = stickRadius * 0.46f
    drawRoundRect(
      Color(0xFF0B1528),
      Offset(dpadCenter.x - dArm, dpadCenter.y - dThick * 0.5f),
      Size(dArm * 2f, dThick),
      CornerRadius(3f, 3f),
    )
    drawRoundRect(
      Color(0xFF0B1528),
      Offset(dpadCenter.x - dThick * 0.5f, dpadCenter.y - dArm),
      Size(dThick, dArm * 2f),
      CornerRadius(3f, 3f),
    )
    drawRoundRect(
      Color(0xFF64748B),
      Offset(dpadCenter.x - dArm, dpadCenter.y - dThick * 0.5f),
      Size(dArm * 2f, dThick),
      CornerRadius(3f, 3f),
      style = Stroke(width = 1.3f),
    )
    drawRoundRect(
      Color(0xFF64748B),
      Offset(dpadCenter.x - dThick * 0.5f, dpadCenter.y - dArm),
      Size(dThick, dArm * 2f),
      CornerRadius(3f, 3f),
      style = Stroke(width = 1.3f),
    )
    val chevCol = Color(0xFF38BDF8)
    val chevOff = dArm * 0.62f
    listOf(Offset(0f, -chevOff), Offset(0f, chevOff), Offset(-chevOff, 0f), Offset(chevOff, 0f))
      .forEach { d -> drawCircle(chevCol, 2.0f, dpadCenter + d) }

    val faceClusterCenter = Offset(cx + bodyW * 0.26f, cy - bodyH * 0.08f)
    val faceOffset = stickRadius * 0.78f
    val btnR = stickRadius * 0.36f
    val yPos = Offset(faceClusterCenter.x, faceClusterCenter.y - faceOffset)
    val xPos = Offset(faceClusterCenter.x - faceOffset, faceClusterCenter.y)
    val bPos = Offset(faceClusterCenter.x + faceOffset, faceClusterCenter.y)
    val aPos = Offset(faceClusterCenter.x, faceClusterCenter.y + faceOffset)

    listOf(
        Triple(KeyEvent.KEYCODE_BUTTON_Y, yPos, Color(0xFFFACC15) to "Y"),
        Triple(KeyEvent.KEYCODE_BUTTON_X, xPos, Color(0xFF00F5D4) to "X"),
        Triple(KeyEvent.KEYCODE_BUTTON_B, bPos, Color(0xFFEF4444) to "B"),
        Triple(KeyEvent.KEYCODE_BUTTON_A, aPos, Color(0xFF38BDF8) to "A"),
      )
      .forEach { (code, pos, colAndLetter) ->
        val (col, letter) = colAndLetter
        val isHi = highlightedCode == code
        val isBound = RiftCalibrationState.getActionForBinding(code) != null
        drawCircle(if (isHi) Color(0xFF422806) else Color(0xFF0A1424), btnR, pos)
        drawCircle(
          strokeForCode(code, if (isBound) col else col.copy(alpha = 0.65f)),
          btnR,
          pos,
          style = Stroke(width = if (isHi) 3.2f else if (isBound) 2.2f else 1.6f),
        )
        drawContext.canvas.nativeCanvas.apply {
          labelPaint.color = android.graphics.Color.WHITE
          labelPaint.textSize = btnR * 1.15f
          labelPaint.textAlign = Paint.Align.CENTER
          drawText(letter, pos.x, pos.y + btnR * 0.40f, labelPaint)
        }
      }

    fun drawCalloutLabel(
      anchor: Offset,
      elbowX: Float,
      calloutX: Float,
      yFrac: Float,
      text: String,
      hex: String,
      alignLeft: Boolean,
    ) {
      val argb = android.graphics.Color.parseColor(hex)
      val cCol = Color(argb)
      val elbowPos = Offset(w * elbowX, h * yFrac)
      val calloutPos = Offset(w * calloutX, h * yFrac)
      drawCircle(cCol, 2.6f, anchor)
      drawLine(cCol.copy(alpha = 0.78f), anchor, elbowPos, 1.4f)
      drawLine(cCol.copy(alpha = 0.78f), elbowPos, calloutPos, 1.4f)
      drawContext.canvas.nativeCanvas.apply {
        labelPaint.color = argb
        labelPaint.textSize = (h * 0.044f).coerceIn(11f, 18f)
        labelPaint.textAlign = if (alignLeft) Paint.Align.LEFT else Paint.Align.RIGHT
        drawText(text, if (alignLeft) calloutPos.x + 4f else calloutPos.x - 4f, calloutPos.y + 4f, labelPaint)
      }
    }

    drawCalloutLabel(ltCenter, 0.30f, 0.25f, 0.13f, "LT/L2: $ltAction", "#00F5D4", false)
    drawCalloutLabel(l1Center, 0.30f, 0.25f, 0.25f, "L1/LB: $l1Action", "#CBD5E1", false)
    drawCalloutLabel(lStickCenter, 0.30f, 0.25f, 0.50f, "L-STICK: FLIGHT", "#00F5D4", false)
    drawCalloutLabel(dpadCenter, 0.30f, 0.25f, 0.80f, "D-PAD: ZOOM/HANGAR", "#94A3B8", false)
    drawCalloutLabel(rtCenter, 0.70f, 0.75f, 0.12f, "RT/R2: $rtAction", "#F59E0B", true)
    drawCalloutLabel(r1Center, 0.70f, 0.75f, 0.23f, "R1/RB: $r1Action", "#00F5D4", true)
    drawCalloutLabel(yPos, 0.71f, 0.75f, 0.35f, "Y: $yAction", "#FACC15", true)
    drawCalloutLabel(xPos, 0.71f, 0.75f, 0.46f, "X: $xAction", "#00F5D4", true)
    drawCalloutLabel(bPos, 0.71f, 0.75f, 0.57f, "B: $bAction", "#EF4444", true)
    drawCalloutLabel(aPos, 0.71f, 0.75f, 0.68f, "A: $aAction", "#38BDF8", true)
    drawCalloutLabel(rStickCenter, 0.70f, 0.75f, 0.82f, "R-STICK: 360° AIM", "#C084FC", true)
  }
}
