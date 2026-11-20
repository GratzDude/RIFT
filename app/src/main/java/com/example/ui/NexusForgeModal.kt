package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.RuntimeShader
import android.os.Build
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import com.example.data.LocalPilotPrefs
import com.example.data.RiftBillingManager
import com.example.engine.AnomalyType
import com.example.engine.BrickWallEntity
import com.example.engine.CalibrationParamDescriptor
import com.example.engine.CosmeticCatalogItem
import com.example.engine.CustomShipLoadoutSlot
import com.example.engine.ForgeElementLayer
import com.example.engine.ForgeElementTarget
import com.example.engine.GreenCrate
import com.example.engine.InterdictorBubbleEntity
import com.example.engine.MusicSynthArchetype
import com.example.engine.PowerUpCodexEntry
import com.example.engine.RankedGameMode
import com.example.engine.RiftArenaView
import com.example.engine.RiftCalibrationState
import com.example.engine.RiftCodex
import com.example.engine.RiftGameEngine
import com.example.engine.RiftProgressionCatalog
import com.example.engine.RiftSoundSynthesizer
import com.example.engine.RiftSpriteBaker
import com.example.engine.RiftVisualForgeState
import com.example.engine.SfxForgeRecipe
import com.example.engine.SpaceObstacleType
import com.example.engine.SpatialAnomalyEntity
import com.example.engine.SpecialBuffRegistryEntry
import com.example.engine.WallRect
import com.example.engine.VisualElementRecipe
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

enum class NexusForgeTab(val label: String, val accent: Color) {
  SHIP_COSMETICS("SHIP & COSMETICS", Color(0xFF38BDF8)),
  AUDIO_SFX("AUDIO & SFX LAB", Color(0xFF00F5D4)),
  BUFF_LAB("BUFF FORGE", Color(0xFF34D399)),
  SYNDICATE_CREST("SYNDICATE CREST", Color(0xFFFACC15)),
  DEV_STUDIO_STAGE("1:1 PLAYFIELD STUDIO", Color(0xFFC084FC)),
}

private enum class DysonCreationTab(val label: String, val accent: Color) {
  SHIPS_RIFT_FRAME("SHIPS & RIFT FRAME", Color(0xFF38BDF8)),
  CUSTOM_BUFF_LAB("CUSTOM BUFF LAB", Color(0xFF34D399)),
  AUDIO_SFX_LAB("TRACK A-C AUDIO & SFX", Color(0xFF00F5D4)),
  SYNDICATE_CREST("SYNDICATE CREST", Color(0xFFFACC15)),
}

private enum class InspectorSubTab(val label: String) {
  SHADER_PALETTE("PROCEDURAL SHADER & PALETTE"),
  PHYSICS_HUD("PHYSICS & HUD ATTRIBUTES"),
}

private data class StudioBookmark(
  val shortLabel: String,
  val worldX: Float,
  val worldY: Float,
  val zoom: Float,
)

private data class StageHitEntry(
  val target: ForgeElementTarget,
  val screenX: Float,
  val screenY: Float,
  val hitRadiusPx: Float,
)

private data class StageCrateBuffHitEntry(
  val codexEntry: PowerUpCodexEntry?,
  val customBuff: SpecialBuffRegistryEntry?,
  val screenX: Float,
  val screenY: Float,
  val hitRadiusPx: Float,
)

private val STUDIO_BOOKMARKS =
  listOf(
    StudioBookmark("1: NEXUS RIFT", 4000f, 4000f, 1.00f),
    StudioBookmark("2: OBSTACLES", 3160f, 3520f, 1.00f),
    StudioBookmark("3: RIFTS", 4840f, 3520f, 1.00f),
    StudioBookmark("4: ORDNANCE & STASIS", 4000f, 4720f, 1.00f),
    StudioBookmark("5: STORM WALL", 4000f, 920f, 0.85f),
    StudioBookmark("6: HUD OVERLAY", 4000f, 4000f, 1.00f),
  )

private val FORGE_COLOR_SWATCHES =
  listOf(
    "#040914",
    "#0B1528",
    "#1E293B",
    "#00F5D4",
    "#38BDF8",
    "#A855F7",
    "#EC4899",
    "#FACC15",
    "#F97316",
    "#EF4444",
    "#10B981",
    "#E0F2FE",
  )

private const val FORGE_VORTEX_AGSL =
  """
  uniform float2 uRes;
  uniform float2 uCenter;
  uniform float uRadius;
  uniform float uTime;
  uniform float uTurbulence;
  uniform float uGlow;
  uniform float uOpacity;
  uniform float uMode;
  uniform float3 uPrimary;
  uniform float3 uTrim;
  uniform float3 uAura;

  float hash21(float2 p) {
    p = fract(p * float2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
  }

  float noise2(float2 p) {
    float2 i = floor(p);
    float2 f = fract(p);
    float2 u = f * f * (3.0 - 2.0 * f);
    float a = hash21(i);
    float b = hash21(i + float2(1.0, 0.0));
    float c = hash21(i + float2(0.0, 1.0));
    float d = hash21(i + float2(1.0, 1.0));
    return mix(mix(a, b, u.x), mix(c, d, u.x), u.y);
  }

  float fbm(float2 p) {
    float v = 0.0;
    float amp = 0.5;
    for (int i = 0; i < 4; i++) {
      v += amp * noise2(p);
      p = p * 2.03 + float2(17.1, 31.7);
      amp *= 0.5;
    }
    return v;
  }

  half4 main(float2 fragCoord) {
    float2 d = fragCoord - uCenter;
    float dist = length(d);
    float normR = dist / max(uRadius, 1.0);
    if (uMode < 1.5 && normR > 1.24) return half4(0.0);

    float ang = atan(d.y, d.x);
    float turb = clamp(uTurbulence, 0.05, 2.2);

    if (uMode < 0.5) {
      // Safe Zone Nexus Aperture looking down into Ring-Space & Nexus Hub
      float swirl = ang + (1.25 - normR) * 2.8 * turb - uTime * 0.85;
      float2 q = float2(cos(swirl), sin(swirl)) * normR * (2.6 + turb * 1.4);
      float n = fbm(q + float2(uTime * 0.24, -uTime * 0.19));
      float caustics = 0.5 + 0.5 * sin(normR * 18.0 - uTime * 3.2 + n * 5.0 * turb);
      float rim = exp(-26.0 * abs(normR - 0.96));
      float hubCore = exp(-9.5 * normR * normR);
      float hubRing = exp(-48.0 * abs(normR - 0.26));
      half3 col = mix(half3(uPrimary), half3(uTrim), half(clamp(n * 0.85 + caustics * 0.35, 0.0, 1.0)));
      col += half3(uAura) * half((rim * 1.15 + hubRing * 0.85 + caustics * 0.22) * uGlow);
      col += half3(0.95, 0.98, 1.0) * half(hubCore * 0.9);
      float edgeMask = 1.0 - smoothstep(0.96, 1.20, normR);
      half alpha = half(clamp(edgeMask * (uOpacity * 0.82 + rim * 0.55), 0.0, 1.0));
      return half4(col * alpha, alpha);
    } else if (uMode < 1.5) {
      // Eclipse Stasis 2-Way Gravimetric Containment Field
      float hexWave = 0.5 + 0.5 * sin(ang * 12.0 + uTime * 2.4) * cos(normR * 16.0 - uTime * 1.8);
      float n = fbm(d / max(uRadius * 0.32, 1.0) + float2(uTime * 0.35 * turb, -uTime * 0.28 * turb));
      float shell = smoothstep(0.52, 0.98, normR) * (1.0 - smoothstep(0.98, 1.14, normR));
      float rim = exp(-34.0 * abs(normR - 0.96));
      float innerRim = exp(-30.0 * abs(normR - 0.84));
      half3 col = mix(half3(uPrimary), half3(uTrim), half(clamp(n * turb * 0.7 + hexWave * 0.35, 0.0, 1.0)));
      col += half3(uAura) * half((rim * 1.25 + innerRim * 0.75) * uGlow);
      float fillAlpha = (1.0 - smoothstep(0.98, 1.14, normR)) * (0.26 + 0.32 * shell + 0.14 * hexWave) * uOpacity;
      half alpha = half(clamp(fillAlpha + rim * 0.85, 0.0, 1.0));
      return half4(col * alpha, alpha);
    } else {
      // Outer Playfield Rift Storm Cloud Wall
      float band = (normR - 1.0) * uRadius;
      if (band < -180.0 || band > 190.0) return half4(0.0);
      float2 stormUv = float2(ang * 9.0, band * 0.022) + float2(uTime * 0.32 * turb, -uTime * 0.18);
      float cloud = fbm(stormUv * (1.0 + 0.45 * turb));
      float arc = pow(clamp(1.0 - abs(sin(ang * 22.0 + cloud * 6.2 - uTime * 5.4)), 0.0, 1.0), 8.0);
      float wallFalloff = exp(-abs(band) / 72.0);
      half3 col = mix(half3(uPrimary), half3(uTrim), half(clamp(cloud * 1.15, 0.0, 1.0)));
      col += half3(uAura) * half((arc * 1.35 + exp(-abs(band) / 18.0) * 0.65) * uGlow);
      half alpha = half(clamp(wallFalloff * (0.35 + 0.65 * cloud) * uOpacity + arc * wallFalloff * 0.65, 0.0, 1.0));
      return half4(col * alpha, alpha);
    }
  }
  """

@Composable
private fun ForgeChip(
  text: String,
  bg: Color,
  border: Color,
  textColor: Color = Color.White,
  fontSize: TextUnit = 8.2.sp,
  radius: Dp = 6.dp,
  px: Dp = 8.dp,
  py: Dp = 4.dp,
  borderWidth: Dp = 1.dp,
  enabled: Boolean = true,
  mono: Boolean = true,
  bold: Boolean = true,
  modifier: Modifier = Modifier,
  tag: String? = null,
  onClick: () -> Unit,
) {
  val m =
    modifier
      .clip(RoundedCornerShape(radius))
      .background(bg)
      .border(borderWidth, border, RoundedCornerShape(radius))
      .clickable(enabled = enabled, onClick = onClick)
      .padding(horizontal = px, vertical = py)
      .let { if (tag != null) it.testTag(tag) else it }
  Box(modifier = m, contentAlignment = Alignment.Center) {
    Text(
      text = text,
      color = textColor,
      fontSize = fontSize,
      fontWeight = if (bold) FontWeight.Black else FontWeight.SemiBold,
      fontFamily = if (mono) FontFamily.Monospace else FontFamily.SansSerif,
      maxLines = 1,
    )
  }
}

@Composable
fun NexusForgeModal(
  engine: RiftGameEngine,
  sfx: RiftSoundSynthesizer,
  localPrefs: LocalPilotPrefs,
  currentUserId: String,
  callsign: String,
  syndicateTag: String,
  pilotLevel: Int,
  riftShards: Long,
  selectedShipId: Int,
  activeCustomSlot: CustomShipLoadoutSlot,
  slot9BUnlocked: Boolean,
  slot9CUnlocked: Boolean,
  slot9DUnlocked: Boolean,
  slot9EUnlocked: Boolean,
  unlockFullNinthShipOverride: Boolean,
  sfxEnabled: Boolean,
  musicEnabled: Boolean,
  musicBpm: Int,
  musicVolume: Float,
  sfxVolume: Float,
  initialTab: NexusForgeTab = NexusForgeTab.SHIP_COSMETICS,
  isFullScreenStage: Boolean = false,
  onToggleFullScreenStage: (Boolean) -> Unit = {},
  onExitToNexusHub: () -> Unit = {},
  onSelectShipForDeploy: (Int) -> Unit = {},
  onSaveCustomSlot: (CustomShipLoadoutSlot) -> Unit = {},
  onSwitchCustomSlot: (String) -> Unit = {},
  onUnlockCustomSlot: (String) -> Boolean = { false },
  onForgeAiHullDesign: (String, String) -> Pair<Boolean, String> = { _, _ -> Pair(false, "") },
  onSpendRiftShards: (Long) -> Boolean = { false },
  onToggleSfx: () -> Unit = {},
  onToggleMusic: () -> Unit = {},
  onChangeMusicBpm: (Int) -> Unit = {},
  onChangeMusicVolume: (Float) -> Unit = {},
  onChangeSfxVolume: (Float) -> Unit = {},
  onSelectMusicArchetype: (MusicSynthArchetype) -> Unit = {},
  onForgeCustomMusicPrompt: (String) -> Unit = {},
  onEnterHudDragArenaMode: () -> Unit = {},
  onSyncCalibrationToTesters: () -> Unit = {},
  onShowToast: (String) -> Unit = {},
) {
  val context = LocalContext.current
  val isCompactPhone = LocalConfiguration.current.screenHeightDp < 480
  val availableTabs =
    remember {
      if (RiftVisualForgeState.SHOW_DEV_FORGE) NexusForgeTab.entries
      else NexusForgeTab.entries.filter { it != NexusForgeTab.DEV_STUDIO_STAGE }
    }
  var activeTab by remember(initialTab) {
    mutableStateOf(if (initialTab in availableTabs) initialTab else NexusForgeTab.SHIP_COSMETICS)
  }
  val visualRev = RiftVisualForgeState.visualRevision
  val calibRev = RiftCalibrationState.calibrationRevision
  val shardBitmap =
    remember(visualRev, RiftSpriteBaker.spriteVisualRevision) {
      RiftSpriteBaker.riftShardIconBitmap.asImageBitmap()
    }

  LaunchedEffect(activeTab) {
    if (activeTab == NexusForgeTab.DEV_STUDIO_STAGE && !isFullScreenStage) {
      onToggleFullScreenStage(true)
    }
  }
  DisposableEffect(Unit) {
    onDispose {
      RiftCalibrationState.isDraggingAnySlider = false
      onToggleFullScreenStage(false)
    }
  }

  val shardReadoutText =
    if (RiftBillingManager.TESTER_INFINITE_SHARDS_ACTIVE) {
      "INF • RES ${engine.liveResonance}"
    } else {
      "$riftShards • RES ${engine.liveResonance}"
    }

  if (activeTab == NexusForgeTab.DEV_STUDIO_STAGE && isFullScreenStage) {
    Box(
      modifier =
        Modifier.fillMaxSize()
          .background(Color(0xFF02050B))
          .testTag("nexus_forge_modal"),
    ) {
      DevPlayfieldAndHudStudioBay(
        context = context,
        engine = engine,
        sfx = sfx,
        localPrefs = localPrefs,
        currentUserId = currentUserId,
        callsign = callsign,
        syndicateTag = syndicateTag,
        pilotLevel = pilotLevel,
        riftShards = riftShards,
        selectedShipId = selectedShipId,
        activeCustomSlot = activeCustomSlot,
        slot9BUnlocked = slot9BUnlocked,
        slot9CUnlocked = slot9CUnlocked,
        slot9DUnlocked = slot9DUnlocked,
        slot9EUnlocked = slot9EUnlocked,
        unlockFullNinthShipOverride = unlockFullNinthShipOverride,
        sfxEnabled = sfxEnabled,
        musicEnabled = musicEnabled,
        musicBpm = musicBpm,
        musicVolume = musicVolume,
        sfxVolume = sfxVolume,
        isCompactPhone = isCompactPhone,
        visualRev = visualRev,
        calibRev = calibRev,
        isFullScreenStage = true,
        shardBitmap = shardBitmap,
        shardReadoutText = shardReadoutText,
        onSelectShipForDeploy = onSelectShipForDeploy,
        onSaveCustomSlot = onSaveCustomSlot,
        onSwitchCustomSlot = onSwitchCustomSlot,
        onUnlockCustomSlot = onUnlockCustomSlot,
        onForgeAiHullDesign = onForgeAiHullDesign,
        onSpendRiftShards = onSpendRiftShards,
        onToggleSfx = onToggleSfx,
        onToggleMusic = onToggleMusic,
        onChangeMusicBpm = onChangeMusicBpm,
        onChangeMusicVolume = onChangeMusicVolume,
        onChangeSfxVolume = onChangeSfxVolume,
        onSelectMusicArchetype = onSelectMusicArchetype,
        onForgeCustomMusicPrompt = onForgeCustomMusicPrompt,
        onExitToForgeTabs = {
          RiftCalibrationState.isDraggingAnySlider = false
          onToggleFullScreenStage(false)
          onExitToNexusHub()
        },
        onExitToNexusHub = onExitToNexusHub,
        onToggleFullScreenStage = onToggleFullScreenStage,
        onEnterHudDragArenaMode = onEnterHudDragArenaMode,
        onSyncCalibrationToTesters = onSyncCalibrationToTesters,
        onShowToast = onShowToast,
      )
    }
    return
  }

  Column(
    modifier =
      Modifier.fillMaxSize()
        .padding(if (isCompactPhone) 4.dp else 8.dp)
        .testTag("nexus_forge_modal"),
    verticalArrangement = Arrangement.spacedBy(if (isCompactPhone) 5.dp else 7.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Row(
        modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        availableTabs.forEach { tab ->
          val isSel = activeTab == tab
          ForgeChip(
            text = tab.label,
            bg = if (isSel) tab.accent.copy(alpha = 0.24f) else Color(0xCC091324),
            border = if (isSel) tab.accent else tab.accent.copy(alpha = 0.45f),
            textColor = if (isSel) Color.White else tab.accent,
            fontSize = if (isCompactPhone) 8.8.sp else 9.6.sp,
            radius = 8.dp,
            px = if (isCompactPhone) 8.dp else 11.dp,
            py = if (isCompactPhone) 4.dp else 6.dp,
            borderWidth = if (isSel) 1.3.dp else 1.dp,
            tag = "forge_subtab_${tab.name.lowercase(Locale.US)}",
          ) {
            activeTab = tab
            if (tab == NexusForgeTab.DEV_STUDIO_STAGE) {
              onToggleFullScreenStage(true)
            } else if (isFullScreenStage) {
              onToggleFullScreenStage(false)
            }
          }
        }
      }
      Spacer(modifier = Modifier.width(8.dp))
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        if (activeTab == NexusForgeTab.DEV_STUDIO_STAGE) {
          ForgeChip(
            text = if (isFullScreenStage) "[< NEXUS HUB]" else "[FULL 1:1 STAGE]",
            bg = if (isFullScreenStage) Color(0xFF0B253A) else Color(0xFF0E1D34),
            border = if (isFullScreenStage) Color(0xFF00F5D4) else Color(0xFFC084FC),
            textColor = if (isFullScreenStage) Color(0xFFE0F2FE) else Color(0xFFE9D5FF),
            fontSize = 8.5.sp,
            radius = 7.dp,
            tag = "forge_btn_toggle_fullscreen_stage",
          ) {
            if (isFullScreenStage) {
              RiftCalibrationState.isDraggingAnySlider = false
              onToggleFullScreenStage(false)
              onExitToNexusHub()
            } else {
              onToggleFullScreenStage(true)
            }
          }
        }
        Row(
          modifier =
            Modifier.clip(RoundedCornerShape(7.dp))
              .background(Color(0xFF160E2C))
              .border(1.dp, Color(0xFFA855F7), RoundedCornerShape(7.dp))
              .padding(horizontal = 8.dp, vertical = 4.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
          Image(bitmap = shardBitmap, contentDescription = "Rift Shards", modifier = Modifier.size(13.dp))
          Text(
            text = shardReadoutText,
            color = Color(0xFFE9D5FF),
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
          )
        }
      }
    }

    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
      when (activeTab) {
        NexusForgeTab.SHIP_COSMETICS ->
          ShipAndCosmeticTurntableBay(
            localPrefs = localPrefs,
            currentUserId = currentUserId,
            pilotLevel = pilotLevel,
            riftShards = riftShards,
            selectedShipId = selectedShipId,
            activeCustomSlot = activeCustomSlot,
            slot9BUnlocked = slot9BUnlocked,
            slot9CUnlocked = slot9CUnlocked,
            slot9DUnlocked = slot9DUnlocked,
            slot9EUnlocked = slot9EUnlocked,
            unlockFullNinthShipOverride = unlockFullNinthShipOverride,
            isCompactPhone = isCompactPhone,
            onSelectShipForDeploy = onSelectShipForDeploy,
            onSaveCustomSlot = onSaveCustomSlot,
            onSwitchCustomSlot = onSwitchCustomSlot,
            onUnlockCustomSlot = onUnlockCustomSlot,
            onForgeAiHullDesign = onForgeAiHullDesign,
            onSpendRiftShards = onSpendRiftShards,
            onShowToast = onShowToast,
          )
        NexusForgeTab.AUDIO_SFX ->
          SoundtrackAndSfxForgeBay(
            context = context,
            sfx = sfx,
            sfxEnabled = sfxEnabled,
            musicEnabled = musicEnabled,
            musicBpm = musicBpm,
            musicVolume = musicVolume,
            sfxVolume = sfxVolume,
            isCompactPhone = isCompactPhone,
            onToggleSfx = onToggleSfx,
            onToggleMusic = onToggleMusic,
            onChangeMusicBpm = onChangeMusicBpm,
            onChangeMusicVolume = onChangeMusicVolume,
            onChangeSfxVolume = onChangeSfxVolume,
            onSelectMusicArchetype = onSelectMusicArchetype,
            onForgeCustomMusicPrompt = onForgeCustomMusicPrompt,
            onShowToast = onShowToast,
          )
        NexusForgeTab.BUFF_LAB -> CustomBuffForgeBay(context, engine, sfx, calibRev, onShowToast)
        NexusForgeTab.SYNDICATE_CREST ->
          SyndicateCrestForgeBay(context, sfx, syndicateTag, callsign, visualRev, onShowToast)
        NexusForgeTab.DEV_STUDIO_STAGE ->
          DevPlayfieldAndHudStudioBay(
            context = context,
            engine = engine,
            sfx = sfx,
            localPrefs = localPrefs,
            currentUserId = currentUserId,
            callsign = callsign,
            syndicateTag = syndicateTag,
            pilotLevel = pilotLevel,
            riftShards = riftShards,
            selectedShipId = selectedShipId,
            activeCustomSlot = activeCustomSlot,
            slot9BUnlocked = slot9BUnlocked,
            slot9CUnlocked = slot9CUnlocked,
            slot9DUnlocked = slot9DUnlocked,
            slot9EUnlocked = slot9EUnlocked,
            unlockFullNinthShipOverride = unlockFullNinthShipOverride,
            sfxEnabled = sfxEnabled,
            musicEnabled = musicEnabled,
            musicBpm = musicBpm,
            musicVolume = musicVolume,
            sfxVolume = sfxVolume,
            isCompactPhone = isCompactPhone,
            visualRev = visualRev,
            calibRev = calibRev,
            isFullScreenStage = false,
            shardBitmap = shardBitmap,
            shardReadoutText = shardReadoutText,
            onSelectShipForDeploy = onSelectShipForDeploy,
            onSaveCustomSlot = onSaveCustomSlot,
            onSwitchCustomSlot = onSwitchCustomSlot,
            onUnlockCustomSlot = onUnlockCustomSlot,
            onForgeAiHullDesign = onForgeAiHullDesign,
            onSpendRiftShards = onSpendRiftShards,
            onToggleSfx = onToggleSfx,
            onToggleMusic = onToggleMusic,
            onChangeMusicBpm = onChangeMusicBpm,
            onChangeMusicVolume = onChangeMusicVolume,
            onChangeSfxVolume = onChangeSfxVolume,
            onSelectMusicArchetype = onSelectMusicArchetype,
            onForgeCustomMusicPrompt = onForgeCustomMusicPrompt,
            onExitToForgeTabs = {
              RiftCalibrationState.isDraggingAnySlider = false
              onToggleFullScreenStage(false)
              onExitToNexusHub()
            },
            onExitToNexusHub = onExitToNexusHub,
            onToggleFullScreenStage = onToggleFullScreenStage,
            onEnterHudDragArenaMode = onEnterHudDragArenaMode,
            onSyncCalibrationToTesters = onSyncCalibrationToTesters,
            onShowToast = onShowToast,
          )
      }
    }
  }
}

@Composable
private fun ShipAndCosmeticTurntableBay(
  localPrefs: LocalPilotPrefs,
  currentUserId: String,
  pilotLevel: Int,
  riftShards: Long,
  selectedShipId: Int,
  activeCustomSlot: CustomShipLoadoutSlot,
  slot9BUnlocked: Boolean,
  slot9CUnlocked: Boolean,
  slot9DUnlocked: Boolean,
  slot9EUnlocked: Boolean,
  unlockFullNinthShipOverride: Boolean,
  isCompactPhone: Boolean,
  onSelectShipForDeploy: (Int) -> Unit,
  onSaveCustomSlot: (CustomShipLoadoutSlot) -> Unit,
  onSwitchCustomSlot: (String) -> Unit,
  onUnlockCustomSlot: (String) -> Boolean,
  onForgeAiHullDesign: (String, String) -> Pair<Boolean, String>,
  onSpendRiftShards: (Long) -> Boolean,
  onShowToast: (String) -> Unit,
) {
  var inspectedShipId by remember(selectedShipId) { mutableIntStateOf(selectedShipId.coerceIn(1, 10)) }
  var turntableAngleDeg by remember { mutableFloatStateOf(-90f) }
  var autoSpin by remember { mutableStateOf(true) }
  var cosmeticCategory by remember { mutableStateOf("HULL_TRIM") }
  var cosmeticRefreshTick by remember { mutableIntStateOf(0) }
  var hullPromptDraft by remember(activeCustomSlot.slotId) { mutableStateOf("") }
  var hullTitleDraft by remember(activeCustomSlot.customName) { mutableStateOf(activeCustomSlot.customName) }
  var hullStatusBanner by remember { mutableStateOf<String?>(null) }

  val infiniteTransition = rememberInfiniteTransition(label = "ship_turntable_spin")
  val spinPhase by
    infiniteTransition.animateFloat(
      initialValue = 0f,
      targetValue = 360f,
      animationSpec = infiniteRepeatable(tween(11000, easing = LinearEasing), RepeatMode.Restart),
      label = "turntable_deg",
    )

  val spriteVer = RiftSpriteBaker.customShip9Version
  val allShips = remember(activeCustomSlot, spriteVer) { RiftCodex.getAllNineShips() }
  val currentSpec = allShips.firstOrNull { it.id == inspectedShipId } ?: allShips.first()
  val cleanShipName =
    if (currentSpec.id == 10) "RIFT FRAME"
    else currentSpec.name.replace(Regex("^[0-9]+-"), "").uppercase(Locale.US)
  val equippedTrimId =
    remember(cosmeticRefreshTick, currentUserId) {
      localPrefs.getEquippedCosmeticId("HULL_TRIM", currentUserId)
    }
  val activeTrimStyleIndex = equippedTrimId.coerceIn(0, 3)
  RiftSpriteBaker.activeHullTrimStyle = activeTrimStyleIndex

  Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
    Column(
      modifier =
        Modifier.weight(0.46f)
          .fillMaxHeight()
          .background(Color(0xFF071120), RoundedCornerShape(10.dp))
          .border(1.dp, Color(0xFF1E3A5F), RoundedCornerShape(10.dp))
          .padding(10.dp),
      verticalArrangement = Arrangement.SpaceBetween,
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            "360° HOLOGRAPHIC TURNTABLE",
            color = Color(0xFF00F5D4),
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
          )
          Text(
            "$cleanShipName • ${currentSpec.role.uppercase(Locale.US)}",
            color = Color.White,
            fontSize = if (isCompactPhone) 12.sp else 14.sp,
            fontWeight = FontWeight.Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
        ForgeChip(
          text = if (autoSpin) "[AUTO-SPIN]" else "[DRAG SPIN]",
          bg = if (autoSpin) Color(0xFF0E4F5C) else Color(0xFF132238),
          border = Color(0xFF00F5D4),
          textColor = Color(0xFF00F5D4),
          px = 7.dp,
        ) { autoSpin = !autoSpin }
      }

      val effectiveHeadingDeg = if (autoSpin) spinPhase else turntableAngleDeg
      Box(
        modifier =
          Modifier.fillMaxWidth()
            .weight(1f)
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF040914))
            .border(1.dp, Color(0x5538BDF8), RoundedCornerShape(10.dp))
            .pointerInput(Unit) {
              detectDragGestures { change, dragAmount ->
                change.consume()
                autoSpin = false
                turntableAngleDeg = (turntableAngleDeg + dragAmount.x * 0.85f) % 360f
              }
            }
            .testTag("forge_ship_turntable_canvas"),
        contentAlignment = Alignment.Center,
      ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
          val cx = size.width * 0.5f
          val cy = size.height * 0.52f
          val ringR = size.minDimension * 0.38f
          drawCircle(
            brush =
              Brush.radialGradient(
                listOf(Color(0x3800F5D4), Color(0x1438BDF8), Color.Transparent),
                center = Offset(cx, cy),
                radius = ringR * 1.35f,
              ),
            radius = ringR * 1.35f,
            center = Offset(cx, cy),
          )
          drawCircle(Color(0x4438BDF8), ringR, Offset(cx, cy), style = Stroke(1.5.dp.toPx()))
          drawCircle(Color(0x8800F5D4), ringR * 0.72f, Offset(cx, cy), style = Stroke(1.dp.toPx()))
          for (tick in 0 until 24) {
            val a = (tick * 15f + effectiveHeadingDeg * 0.25f) * (PI.toFloat() / 180f)
            val r0 = if (tick % 3 == 0) ringR * 0.88f else ringR * 0.94f
            drawLine(
              Color(0x667DD3FC),
              Offset(cx + cos(a) * r0, cy + sin(a) * r0),
              Offset(cx + cos(a) * ringR, cy + sin(a) * ringR),
              1.2f,
            )
          }
          drawIntoCanvas { canvas ->
            val nc = canvas.nativeCanvas
            nc.save()
            nc.translate(cx, cy)
            nc.rotate(effectiveHeadingDeg)
            val shipRadius = (size.minDimension * 0.27f).coerceIn(28f, 78f)
            if (currentSpec.id == 10) {
              val bmp = RiftSpriteBaker.customShip9AlliedBitmap
              nc.drawBitmap(bmp, null, android.graphics.RectF(-shipRadius, -shipRadius, shipRadius, shipRadius), null)
            } else {
              RiftSpriteBaker.drawMetallicShipOnCanvas(
                nc,
                0f,
                0f,
                shipRadius,
                currentSpec,
                isAllied = true,
                hullTrimStyle = activeTrimStyleIndex,
              )
            }
            nc.restore()
          }
        }
        Text(
          "DRAG HORIZONTALLY TO ROTATE 360°",
          color = Color(0xFF64748B),
          fontSize = 8.sp,
          fontFamily = FontFamily.Monospace,
          modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 5.dp),
        )
      }

      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
          allShips.forEach { ship ->
            val shortLbl =
              if (ship.id == 10) "10 FRAME"
              else "${ship.id} ${ship.name.replace(Regex("^[0-9]+-"), "").take(6).uppercase(Locale.US)}"
            val sel = ship.id == inspectedShipId
            ForgeChip(
              text = shortLbl,
              bg = if (sel) Color(0xFF0E3A5D) else Color(0xFF0A1426),
              border = if (sel) Color(0xFF00F5D4) else Color(0xFF334155),
              textColor = if (sel) Color.White else Color(0xFF94A3B8),
              px = 7.dp,
            ) { inspectedShipId = ship.id }
          }
        }
        Button(
          onClick = { onSelectShipForDeploy(inspectedShipId) },
          colors =
            ButtonDefaults.buttonColors(
              containerColor = if (inspectedShipId == selectedShipId) Color(0xFF15803D) else Color(0xFF0284C7)
            ),
          border = BorderStroke(1.dp, Color(0xFF00F5D4)),
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier.fillMaxWidth().height(34.dp).testTag("forge_btn_equip_ship"),
        ) {
          Text(
            text =
              if (inspectedShipId == selectedShipId) "[$cleanShipName STAGED FOR DEPLOY]"
              else "[STAGE $cleanShipName FOR DEPLOY]",
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
          )
        }
      }
    }

    Column(
      modifier =
        Modifier.weight(0.54f)
          .fillMaxHeight()
          .background(Color(0xFF071120), RoundedCornerShape(10.dp))
          .border(1.dp, Color(0xFF1E3A5F), RoundedCornerShape(10.dp))
          .padding(10.dp)
          .verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
      Column(
        modifier =
          Modifier.fillMaxWidth()
            .background(Color(0xFF120B24), RoundedCornerShape(8.dp))
            .border(1.dp, Color(0xFFA855F7), RoundedCornerShape(8.dp))
            .padding(9.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        val normSlot = CustomShipLoadoutSlot.normalizeSlotId(activeCustomSlot.slotId)
        val budget = RiftProgressionCatalog.getRiftFramePointBudget(pilotLevel.coerceAtLeast(20), unlockFullNinthShipOverride || true)
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            "RIFT FRAME MODULAR HULL FORGE ($normSlot)",
            color = Color(0xFFE9D5FF),
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
          )
          Text(
            "${activeCustomSlot.totalAllocatedPoints()}/$budget PTS",
            color = Color(0xFF00F5D4),
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
          )
        }
        Row(
          modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
          val frames =
            listOf(
              "Frame A" to true,
              "Frame B" to slot9BUnlocked,
              "Frame C" to slot9CUnlocked,
              "Frame D" to slot9DUnlocked,
              "Frame E" to slot9EUnlocked,
            )
          frames.forEach { (slotName, unlocked) ->
            val isAct = normSlot.equals(slotName, ignoreCase = true)
            val gate = RiftProgressionCatalog.getFrameSlotGate(slotName)
            val lockedLabel =
              if (RiftBillingManager.TESTER_INFINITE_SHARDS_ACTIVE) {
                "[UNLOCK ${slotName.uppercase(Locale.US)} • 0]"
              } else {
                "[UNLOCK ${slotName.uppercase(Locale.US)} • ${gate.shardCost} SHARDS]"
              }
            ForgeChip(
              text = if (unlocked) slotName.uppercase(Locale.US) else lockedLabel,
              bg = if (isAct) Color(0xFF4C1D95) else Color(0xFF0B1324),
              border = if (isAct) Color(0xFFEC4899) else Color(0xFF475569),
              textColor = if (isAct) Color.White else Color(0xFFCBD5E1),
            ) {
              inspectedShipId = 10
              if (unlocked) onSwitchCustomSlot(slotName)
              else if (!onUnlockCustomSlot(slotName)) {
                onShowToast("Requires ${gate.shardCost} Rift Shards")
              }
            }
          }
        }
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          ForgeTextInputBox(hullTitleDraft, { hullTitleDraft = it.take(18) }, "Hull Name...", Modifier.weight(0.38f))
          ForgeTextInputBox(
            hullPromptDraft,
            { hullPromptDraft = it.take(120) },
            "Describe hull (e.g. obsidian stealth delta wing with gold trim)...",
            Modifier.weight(0.62f),
          )
        }
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(
            modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            listOf(
              "Obsidian Stealth Wing",
              "Sovereign Gold Dreadnought",
              "Bio-Cyan Twin Manta",
              "Amethyst Rift Interceptor",
            ).forEach { preset ->
              ForgeChip(
                preset,
                Color(0xFF1E1436),
                Color(0x66C084FC),
                Color(0xFFD8B4FE),
                fontSize = 7.8.sp,
                radius = 5.dp,
                px = 6.dp,
                py = 3.dp,
                mono = false,
                bold = false,
              ) { hullPromptDraft = preset }
            }
          }
          Spacer(modifier = Modifier.width(6.dp))
          val isFirstFree = activeCustomSlot.aiGenerationsCount == 0 || RiftBillingManager.TESTER_INFINITE_SHARDS_ACTIVE
          Box(
            modifier =
              Modifier.clip(RoundedCornerShape(7.dp))
                .background(Brush.horizontalGradient(listOf(Color(0xFF6D28D9), Color(0xFFBE185D))))
                .border(1.dp, Color(0xFFEC4899), RoundedCornerShape(7.dp))
                .clickable {
                  inspectedShipId = 10
                  val (ok, msg) =
                    onForgeAiHullDesign(
                      hullPromptDraft.ifBlank { "Sleek obsidian cyan interceptor" },
                      hullTitleDraft,
                    )
                  hullStatusBanner = msg
                  if (ok) onSaveCustomSlot(RiftCodex.activeCustomShipSlot)
                }
                .padding(horizontal = 10.dp, vertical = 6.dp)
                .testTag("forge_btn_synthesize_hull"),
          ) {
            Text(
              if (isFirstFree) "[FORGE HULL • FREE]" else "[FORGE • 150 SHARDS]",
              color = Color.White,
              fontSize = 8.5.sp,
              fontWeight = FontWeight.Black,
              fontFamily = FontFamily.Monospace,
            )
          }
        }
        if (!hullStatusBanner.isNullOrBlank()) {
          Text(hullStatusBanner!!, color = Color(0xFF67E8F9), fontSize = 8.5.sp, fontFamily = FontFamily.Monospace)
        }
      }

      Column(
        modifier =
          Modifier.fillMaxWidth()
            .background(Color(0xFF0A1628), RoundedCornerShape(8.dp))
            .border(1.dp, Color(0xFF1E3A5F), RoundedCornerShape(8.dp))
            .padding(9.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
      ) {
        Text(
          "ZERO-ADVANTAGE COSMETIC VAULT",
          color = Color(0xFF00F5D4),
          fontSize = 10.sp,
          fontWeight = FontWeight.Black,
          fontFamily = FontFamily.Monospace,
        )
        Row(
          modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
          listOf(
            "HULL_TRIM" to "HULL TRIMS",
            "KILL_FINISHER" to "KILL FINISHERS",
            "WARP_SIGNATURE" to "WARP TRAILS",
            "HVT_HALO" to "LEADER HALOS",
          ).forEach { (cat, title) ->
            val sel = cosmeticCategory == cat
            ForgeChip(
              title,
              if (sel) Color(0xFF0E4F5C) else Color(0xFF07101E),
              if (sel) Color(0xFF00F5D4) else Color(0xFF334155),
              if (sel) Color.White else Color(0xFF94A3B8),
            ) { cosmeticCategory = cat }
          }
        }
        val items: List<CosmeticCatalogItem> =
          remember(cosmeticCategory) {
            when (cosmeticCategory) {
              "HULL_TRIM" -> RiftProgressionCatalog.hullTrims
              "KILL_FINISHER" -> RiftProgressionCatalog.killFinishers
              "WARP_SIGNATURE" -> RiftProgressionCatalog.warpSignatures
              "HVT_HALO" -> RiftProgressionCatalog.hvtHalos
              else -> RiftProgressionCatalog.hullTrims
            }
          }
        val equippedForCat =
          remember(cosmeticCategory, cosmeticRefreshTick, currentUserId) {
            localPrefs.getEquippedCosmeticId(cosmeticCategory, currentUserId)
          }
        for (item in items) {
          val unlockedByLvl = pilotLevel >= item.freeAtLevel || item.shardCost <= 0L
          val unlockedSaved =
            remember(cosmeticCategory, item.id, cosmeticRefreshTick, pilotLevel, currentUserId) {
              localPrefs.isCosmeticUnlocked(cosmeticCategory, item.id, pilotLevel, currentUserId)
            }
          val isUnlocked = unlockedByLvl || unlockedSaved
          val isEquipped = equippedForCat == item.id
          Row(
            modifier =
              Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(7.dp))
                .background(if (isEquipped) Color(0xFF0D283A) else Color(0xFF060E1B))
                .border(1.dp, if (isEquipped) Color(0xFF00F5D4) else Color(0xFF1E293B), RoundedCornerShape(7.dp))
                .padding(horizontal = 9.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Row(
              modifier = Modifier.weight(1f).padding(end = 8.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Box(
                modifier =
                  Modifier.size(16.dp)
                    .clip(CircleShape)
                    .background(Color(item.accentColorHex))
                    .border(1.dp, Color.White.copy(alpha = 0.7f), CircleShape)
              )
              Column {
                Text(item.name, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text(
                  item.summary,
                  color = Color(0xFF94A3B8),
                  fontSize = 8.2.sp,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
            }
            val unlockBtnLabel =
              when {
                isEquipped -> "[EQUIPPED]"
                isUnlocked -> "[EQUIP]"
                RiftBillingManager.TESTER_INFINITE_SHARDS_ACTIVE -> "[UNLOCK • 0]"
                else -> "[UNLOCK ${item.shardCost} SHARDS]"
              }
            ForgeChip(
              text = unlockBtnLabel,
              bg = when {
                isEquipped -> Color(0xFF065F46)
                isUnlocked -> Color(0xFF0E7490)
                else -> Color(0xFF3B1366)
              },
              border = when {
                isEquipped -> Color(0xFF34D399)
                isUnlocked -> Color(0xFF00F5D4)
                else -> Color(0xFFA855F7)
              },
              fontSize = 8.sp,
            ) {
              if (isEquipped) {
                if (item.id != 0) {
                  localPrefs.setEquippedCosmeticId(cosmeticCategory, 0, currentUserId)
                  cosmeticRefreshTick++
                }
              } else if (isUnlocked) {
                localPrefs.setCosmeticUnlocked(cosmeticCategory, item.id, true, currentUserId)
                localPrefs.setEquippedCosmeticId(cosmeticCategory, item.id, currentUserId)
                cosmeticRefreshTick++
                onShowToast("Equipped ${item.name}")
              } else if (
                RiftBillingManager.canAffordShardSpend(riftShards, item.shardCost) &&
                  onSpendRiftShards(item.shardCost)
              ) {
                localPrefs.setCosmeticUnlocked(cosmeticCategory, item.id, true, currentUserId)
                localPrefs.setEquippedCosmeticId(cosmeticCategory, item.id, currentUserId)
                cosmeticRefreshTick++
                onShowToast("Unlocked & Equipped ${item.name}")
              } else {
                onShowToast("Requires ${item.shardCost} Rift Shards")
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun SoundtrackAndSfxForgeBay(
  context: Context,
  sfx: RiftSoundSynthesizer,
  sfxEnabled: Boolean,
  musicEnabled: Boolean,
  musicBpm: Int,
  musicVolume: Float,
  sfxVolume: Float,
  isCompactPhone: Boolean,
  onToggleSfx: () -> Unit,
  onToggleMusic: () -> Unit,
  onChangeMusicBpm: (Int) -> Unit,
  onChangeMusicVolume: (Float) -> Unit,
  onChangeSfxVolume: (Float) -> Unit,
  onSelectMusicArchetype: (MusicSynthArchetype) -> Unit,
  onForgeCustomMusicPrompt: (String) -> Unit,
  onShowToast: (String) -> Unit,
) {
  var activeSlotIdx by remember { mutableIntStateOf(sfx.currentTrackSlot) }
  var musicPromptDraft by remember { mutableStateOf("") }
  var sfxPromptDraft by remember { mutableStateOf("") }
  var sfxRecipeState by remember { mutableStateOf(sfx.currentSfxRecipe) }
  var drumsOn by remember { mutableStateOf(sfx.currentMusicRecipe.drumsEnabled) }

  Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
    Column(
      modifier =
        Modifier.weight(0.50f)
          .fillMaxHeight()
          .background(Color(0xFF071120), RoundedCornerShape(10.dp))
          .border(1.dp, Color(0xFF1E3A5F), RoundedCornerShape(10.dp))
          .padding(10.dp)
          .verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            "PROCEDURAL SOUNDTRACK LAB (60-180 BPM)",
            color = Color(0xFF00F5D4),
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
          )
          Text(
            sfx.currentMusicRecipe.customPromptSummary.replace("\u26A1", "Flux"),
            color = Color(0xFF7DD3FC),
            fontSize = 8.8.sp,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
        ForgeChip(
          text = if (musicEnabled) "[MUSIC: ON]" else "[MUSIC: OFF]",
          bg = if (musicEnabled) Color(0xFF0E4F5C) else Color(0xFF361620),
          border = if (musicEnabled) Color(0xFF00F5D4) else Color(0xFFEF4444),
          fontSize = 8.5.sp,
          radius = 7.dp,
        ) { onToggleMusic() }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        listOf(0 to "TRACK A", 1 to "TRACK B", 2 to "TRACK C").forEach { (idx, title) ->
          val sel = activeSlotIdx == idx
          val slotRec = sfx.musicTrackSlots.getOrNull(idx) ?: sfx.currentMusicRecipe
          Box(
            modifier =
              Modifier.weight(1f)
                .clip(RoundedCornerShape(7.dp))
                .background(if (sel) Color(0xFF0E3A5D) else Color(0xFF0A1424))
                .border(1.dp, if (sel) Color(0xFF00F5D4) else Color(0xFF334155), RoundedCornerShape(7.dp))
                .clickable {
                  sfx.selectTrackSlot(idx)
                  activeSlotIdx = idx
                  drumsOn = sfx.currentMusicRecipe.drumsEnabled
                  onChangeMusicBpm(sfx.musicBpm)
                  RiftCalibrationState.savePersistedLayout(context)
                }
                .padding(horizontal = 6.dp, vertical = 5.dp)
                .testTag("forge_music_slot_$idx"),
          ) {
            Column {
              Text(
                title,
                color = if (sel) Color(0xFF00F5D4) else Color.White,
                fontSize = 8.8.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
              )
              Text("${slotRec.bpm} BPM", color = Color(0xFF94A3B8), fontSize = 7.8.sp, fontFamily = FontFamily.Monospace)
            }
          }
        }
        ForgeChip("[SAVE]", Color(0xFF143D34), Color(0xFF34D399), Color(0xFF6EE7B7), fontSize = 8.5.sp, radius = 7.dp, py = 8.dp) {
          sfx.saveCurrentMusicToSlot(activeSlotIdx)
          RiftCalibrationState.savePersistedLayout(context)
          onShowToast("Saved to Track ${('A' + activeSlotIdx)}")
        }
      }

      Column(
        modifier =
          Modifier.fillMaxWidth()
            .background(Color(0xFF050C18), RoundedCornerShape(8.dp))
            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(8.dp))
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            "TEMPO: $musicBpm BPM",
            color = Color(0xFFFBBF24),
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
          )
          ForgeChip(
            text = if (drumsOn) "[DRUMS: ON]" else "[DRUMS: AMBIENT]",
            bg = if (drumsOn) Color(0xFF1E3A5F) else Color(0xFF1F172A),
            border = Color(0xFF38BDF8),
            fontSize = 8.sp,
            radius = 5.dp,
            px = 7.dp,
            py = 2.5.dp,
          ) {
            drumsOn = !drumsOn
            sfx.currentMusicRecipe = sfx.currentMusicRecipe.copy(drumsEnabled = drumsOn)
            sfx.saveCurrentMusicToSlot(activeSlotIdx)
            RiftCalibrationState.savePersistedLayout(context)
          }
        }
        Slider(
          value = musicBpm.coerceIn(60, 180).toFloat(),
          onValueChange = { onChangeMusicBpm(it.roundToInt().coerceIn(60, 180)) },
          onValueChangeFinished = { RiftCalibrationState.savePersistedLayout(context) },
          valueRange = 60f..180f,
          colors =
            SliderDefaults.colors(
              thumbColor = Color(0xFFF59E0B),
              activeTrackColor = Color(0xFFF59E0B),
              inactiveTrackColor = Color(0xFF132642),
            ),
          modifier = Modifier.fillMaxWidth().height(24.dp),
        )
        Text(
          "MUSIC VOLUME: ${(musicVolume * 100f).roundToInt()}%",
          color = Color(0xFF00F5D4),
          fontSize = 8.8.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.Monospace,
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
          modifier = Modifier.fillMaxWidth().height(22.dp),
        )
      }

      Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
      ) {
        MusicSynthArchetype.entries.forEach { arch ->
          val sel = sfx.currentMusicRecipe.archetype == arch
          ForgeChip(
            text = "${arch.label.replace("\u26A1", "")} (${arch.defaultBpm})",
            bg = if (sel) Color(0xFF0E4F5C) else Color(0xFF0A1426),
            border = if (sel) Color(0xFF00F5D4) else Color(0xFF334155),
            textColor = if (sel) Color(0xFF00F5D4) else Color(0xFFE2E8F0),
            mono = false,
          ) {
            onSelectMusicArchetype(arch)
            sfx.saveCurrentMusicToSlot(activeSlotIdx)
            drumsOn = sfx.currentMusicRecipe.drumsEnabled
            RiftCalibrationState.savePersistedLayout(context)
          }
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        ForgeTextInputBox(
          musicPromptDraft,
          { musicPromptDraft = it.take(120) },
          "Prompt track (e.g. dark cyber bassline 142 bpm, calm cosmic pad)...",
          Modifier.weight(1f),
        )
        ForgeChip(
          text = "[FORGE]",
          bg = Color(0xFF4C1D95),
          border = Color(0xFFC084FC),
          fontSize = 8.8.sp,
          radius = 7.dp,
          px = 10.dp,
          py = 8.dp,
          tag = "forge_btn_synthesize_music",
        ) {
          onForgeCustomMusicPrompt(musicPromptDraft.ifBlank { "Driving dark synthwave pulse 128 bpm" })
          sfx.saveCurrentMusicToSlot(activeSlotIdx)
          drumsOn = sfx.currentMusicRecipe.drumsEnabled
          RiftCalibrationState.savePersistedLayout(context)
        }
      }
    }

    Column(
      modifier =
        Modifier.weight(0.50f)
          .fillMaxHeight()
          .background(Color(0xFF071120), RoundedCornerShape(10.dp))
          .border(1.dp, Color(0xFF1E3A5F), RoundedCornerShape(10.dp))
          .padding(10.dp)
          .verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            "WEAPON & COMBAT SFX SYNTHESIZER",
            color = Color(0xFF38BDF8),
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
          )
          Text(
            sfxRecipeState.promptUsed.replace("\u26A1", "Flux"),
            color = Color(0xFF94A3B8),
            fontSize = 8.5.sp,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
          ForgeChip(
            text = if (sfxEnabled) "[SFX: ON]" else "[MUTED]",
            bg = if (sfxEnabled) Color(0xFF103328) else Color(0xFF361620),
            border = if (sfxEnabled) Color(0xFF34D399) else Color(0xFFEF4444),
            fontSize = 8.sp,
            px = 7.dp,
          ) { onToggleSfx() }
          ForgeChip("[RESET]", Color(0xFF1E293B), Color(0xFF64748B), Color(0xFFE2E8F0), fontSize = 8.sp, px = 7.dp) {
            sfx.applySfxRecipe(SfxForgeRecipe())
            sfxRecipeState = sfx.currentSfxRecipe
            RiftCalibrationState.savePersistedLayout(context)
            sfx.previewForgedPrizeChime()
          }
        }
      }

      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf(
          "[LASER]" to { sfx.previewForgedLaser() },
          "[BLAST]" to { sfx.previewForgedExplosion() },
          "[SHIELD]" to { sfx.previewForgedShieldHit() },
          "[CHIME]" to { sfx.previewForgedPrizeChime() },
        ).forEach { (lbl, trigger) ->
          ForgeChip(
            lbl,
            Color(0xFF0C1E36),
            Color(0xFF38BDF8),
            Color(0xFFE0F2FE),
            fontSize = if (isCompactPhone) 7.8.sp else 8.4.sp,
            radius = 7.dp,
            py = 6.dp,
            modifier = Modifier.weight(1f),
          ) { trigger() }
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        ForgeTextInputBox(
          sfxPromptDraft,
          { sfxPromptDraft = it.take(110) },
          "Prompt SFX (e.g. punchy overdrive laser & deep sub-bass boom)...",
          Modifier.weight(1f),
        )
        ForgeChip("[FORGE SFX]", Color(0xFF0284C7), Color(0xFF00F5D4), fontSize = 8.4.sp, radius = 7.dp, px = 10.dp, py = 8.dp) {
          sfxRecipeState = sfx.synthesizeSfxFromPrompt(sfxPromptDraft.ifBlank { "Punchy overdrive plasma & deep sub boom" })
          RiftCalibrationState.savePersistedLayout(context)
          sfx.previewForgedLaser()
        }
      }

      CompactForgeSliderRow(
        "SFX Master Volume",
        sfxVolume,
        0f..1f,
        "${(sfxVolume * 100f).roundToInt()}%",
        Color(0xFF38BDF8),
        onChange = onChangeSfxVolume,
      )

      fun updateSfx(mutator: (SfxForgeRecipe) -> SfxForgeRecipe) {
        val next = mutator(sfxRecipeState)
        sfxRecipeState = next
        sfx.applySfxRecipe(next)
        RiftCalibrationState.savePersistedLayout(context)
      }
      CompactForgeSliderRow(
        "Laser Pitch Multiplier",
        sfxRecipeState.laserPitchMultiplier,
        0.60f..1.60f,
        String.format(Locale.US, "%.2fx", sfxRecipeState.laserPitchMultiplier),
        Color(0xFF00F5D4),
        onChange = { v -> updateSfx { it.copy(laserPitchMultiplier = v) } },
      )
      CompactForgeSliderRow(
        "Laser Harmonic Drive",
        sfxRecipeState.laserHarmonicDrive,
        0.50f..2.00f,
        String.format(Locale.US, "%.2fx", sfxRecipeState.laserHarmonicDrive),
        Color(0xFF00F5D4),
        onChange = { v -> updateSfx { it.copy(laserHarmonicDrive = v) } },
      )
      CompactForgeSliderRow(
        "Explosion Sub-Bass",
        sfxRecipeState.explosionSubBass,
        0.50f..2.00f,
        String.format(Locale.US, "%.2fx", sfxRecipeState.explosionSubBass),
        Color(0xFFF97316),
        onChange = { v -> updateSfx { it.copy(explosionSubBass = v) } },
      )
      CompactForgeSliderRow(
        "Shield Ripple Resonance",
        sfxRecipeState.shieldRippleResonance,
        0.50f..2.00f,
        String.format(Locale.US, "%.2fx", sfxRecipeState.shieldRippleResonance),
        Color(0xFFA855F7),
        onChange = { v -> updateSfx { it.copy(shieldRippleResonance = v) } },
      )
    }
  }
}

@Composable
private fun CustomBuffForgeBay(
  context: Context,
  engine: RiftGameEngine,
  sfx: RiftSoundSynthesizer,
  calibRev: Int,
  onShowToast: (String) -> Unit,
) {
  var buffPromptDraft by remember { mutableStateOf("Singularity gravity pulse pulling hostile hulls inward") }
  var buffNameDraft by remember { mutableStateOf("VOID LANCE") }
  var previewSalt by remember { mutableIntStateOf(1) }
  var registryTick by remember { mutableIntStateOf(0) }
  var draftEntry by remember {
    mutableStateOf(
      SpecialBuffRegistryEntry.synthesizeFromPrompt(
        "Singularity gravity pulse pulling hostile hulls inward",
        RiftCodex.nextCustomBuffTypeIndex(),
      )
    )
  }
  val previewBitmap = remember(draftEntry) { RiftSpriteBaker.bakeCustomBuffIconBitmap(draftEntry, 96).asImageBitmap() }
  val customBuffList = remember(calibRev, registryTick) { RiftCodex.getCustomForgedBuffs() }

  Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
    Column(
      modifier =
        Modifier.weight(0.54f)
          .fillMaxHeight()
          .background(Color(0xFF071120), RoundedCornerShape(10.dp))
          .border(1.dp, Color(0xFF1E3A5F), RoundedCornerShape(10.dp))
          .padding(10.dp)
          .verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
      Text(
        "ON-DEVICE SPECIAL BUFF SYNTHESIZER",
        color = Color(0xFF34D399),
        fontSize = 10.sp,
        fontWeight = FontWeight.Black,
        fontFamily = FontFamily.Monospace,
      )
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Box(
          modifier =
            Modifier.size(64.dp)
              .clip(RoundedCornerShape(10.dp))
              .background(Color(0xFF040914))
              .border(1.dp, Color(0xFF34D399), RoundedCornerShape(10.dp)),
          contentAlignment = Alignment.Center,
        ) {
          Image(
            bitmap = previewBitmap,
            contentDescription = draftEntry.officialName,
            filterQuality = FilterQuality.High,
            modifier = Modifier.size(54.dp),
          )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
          ForgeTextInputBox(
            buffNameDraft,
            {
              buffNameDraft = it.take(18).uppercase(Locale.US)
              val clean = buffNameDraft.ifBlank { "CUSTOM BUFF" }
              draftEntry = draftEntry.copy(officialName = clean, shortLabel = clean.take(8))
            },
            "Buff Title...",
          )
          Text(
            "Effect: ${draftEntry.specialEffect.name} • ${draftEntry.damage.roundToInt()} DMG • 0 Flux • +${draftEntry.chargesPerPickup} Charges",
            color = Color(0xFF94A3B8),
            fontSize = 8.4.sp,
            fontFamily = FontFamily.Monospace,
          )
        }
      }
      ForgeTextInputBox(
        buffPromptDraft,
        { buffPromptDraft = it.take(140) },
        "Describe tactical crate buff (e.g. crimson EMP shockwave, cyan stasis wall)...",
        Modifier.fillMaxWidth(),
      )
      Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
      ) {
        listOf(
          "Gravity Pull Vortex",
          "360° Repulsor Wave",
          "Arcing EMP Lance",
          "Hard-Light Barrier",
          "Ricochet Scatter Volley",
        ).forEach { preset ->
          ForgeChip(
            preset,
            Color(0xFF0C2233),
            Color(0x6634D399),
            Color(0xFFA7F3D0),
            fontSize = 8.sp,
            radius = 5.dp,
            px = 7.dp,
            py = 3.5.dp,
            mono = false,
            bold = false,
          ) {
            buffPromptDraft = preset
            previewSalt++
            val synth = SpecialBuffRegistryEntry.synthesizeFromPrompt(preset, RiftCodex.nextCustomBuffTypeIndex())
            buffNameDraft = synth.officialName
            draftEntry = synth
          }
        }
      }
      CompactForgeSliderRow(
        "Projectile / Pulse Damage",
        draftEntry.damage,
        40f..1400f,
        "${draftEntry.damage.roundToInt()} DMG",
        Color(0xFF34D399),
        onChange = { draftEntry = draftEntry.copy(damage = it) },
      )
      CompactForgeSliderRow(
        "Blast / Field Radius",
        draftEntry.blastRadius,
        60f..520f,
        "${draftEntry.blastRadius.roundToInt()} px",
        Color(0xFF38BDF8),
        onChange = { draftEntry = draftEntry.copy(blastRadius = it) },
      )
      CompactForgeSliderRow(
        "Projectile Velocity",
        draftEntry.projectileSpeed,
        160f..1300f,
        "${draftEntry.projectileSpeed.roundToInt()} px/s",
        Color(0xFFFACC15),
        onChange = { draftEntry = draftEntry.copy(projectileSpeed = it) },
      )

      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(
          onClick = {
            previewSalt++
            val synth =
              SpecialBuffRegistryEntry.synthesizeFromPrompt(
                buffPromptDraft.ifBlank { "Plasma burst nova" },
                RiftCodex.nextCustomBuffTypeIndex(),
              )
            buffNameDraft = synth.officialName
            draftEntry = synth
          },
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
          border = BorderStroke(1.dp, Color(0xFF38BDF8)),
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier.weight(0.42f).height(34.dp),
        ) {
          Text("[SYNTHESIZE]", color = Color.White, fontSize = 8.8.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
        }
        Button(
          onClick = {
            val nextIdx = RiftCodex.nextCustomBuffTypeIndex()
            val cleanTitle = buffNameDraft.ifBlank { draftEntry.officialName }.take(18).uppercase(Locale.US)
            val committed =
              draftEntry.copy(
                id = "FORGED_BUFF_${System.currentTimeMillis()}",
                buffTypeIndex = nextIdx,
                officialName = cleanTitle,
                shortLabel = cleanTitle.take(8),
                isCustomForged = true,
              )
            RiftCodex.registerCustomBuff(committed)
            RiftCalibrationState.savePersistedLayout(context)
            engine.grantSpecialBuffWeapon(engine.localPlayer, committed.id, committed.chargesPerPickup)
            sfx.playPrize()
            registryTick++
            onShowToast("Forged & Equipped [${committed.officialName}] in Special Buff Slot")
          },
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
          border = BorderStroke(1.dp, Color(0xFF6EE7B7)),
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier.weight(0.58f).height(34.dp).testTag("forge_btn_commit_custom_buff"),
        ) {
          Text("[ADD TO CRATES & EQUIP]", color = Color.White, fontSize = 8.8.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
        }
      }
    }

    Column(
      modifier =
        Modifier.weight(0.46f)
          .fillMaxHeight()
          .background(Color(0xFF071120), RoundedCornerShape(10.dp))
          .border(1.dp, Color(0xFF1E3A5F), RoundedCornerShape(10.dp))
          .padding(10.dp)
          .verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
      Text(
        "FORGED CRATE BUFF ROSTER (${customBuffList.size})",
        color = Color(0xFF00F5D4),
        fontSize = 10.sp,
        fontWeight = FontWeight.Black,
        fontFamily = FontFamily.Monospace,
      )
      if (customBuffList.isEmpty()) {
        Box(
          modifier =
            Modifier.fillMaxWidth()
              .background(Color(0xFF040914), RoundedCornerShape(8.dp))
              .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(8.dp))
              .padding(14.dp),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            "No custom buffs forged yet. Synthesize a tactical buff on the left to inject it into Supply Crates and equip it immediately.",
            color = Color(0xFF94A3B8),
            fontSize = 9.5.sp,
            lineHeight = 13.sp,
          )
        }
      } else {
        for (entry in customBuffList) {
          val iconBmp = remember(entry) { RiftSpriteBaker.bakeCustomBuffIconBitmap(entry, 64).asImageBitmap() }
          Row(
            modifier =
              Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF050C18))
                .border(1.dp, Color(entry.strokeColorInt), RoundedCornerShape(8.dp))
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Row(
              modifier = Modifier.weight(1f),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Image(bitmap = iconBmp, contentDescription = entry.officialName, modifier = Modifier.size(34.dp))
              Column {
                Text(entry.officialName, color = Color.White, fontSize = 9.8.sp, fontWeight = FontWeight.Bold)
                Text(
                  "${entry.specialEffect.name} • ${entry.damage.roundToInt()} DMG • 0 Flux",
                  color = Color(0xFF94A3B8),
                  fontSize = 8.sp,
                  fontFamily = FontFamily.Monospace,
                )
              }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
              ForgeChip("[EQUIP]", Color(0xFF0E4F5C), Color(0xFF00F5D4), fontSize = 7.8.sp, radius = 5.dp, px = 7.dp) {
                engine.grantSpecialBuffWeapon(engine.localPlayer, entry.id, entry.chargesPerPickup)
                sfx.playPrize()
                onShowToast("Equipped [${entry.officialName}] for Test Flight")
              }
              ForgeChip("[X]", Color(0xFF361620), Color(0xFFEF4444), Color(0xFFFCA5A5), fontSize = 8.sp, radius = 5.dp, px = 6.dp) {
                RiftCodex.removeCustomBuff(entry.id)
                RiftCalibrationState.savePersistedLayout(context)
                registryTick++
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun SyndicateCrestForgeBay(
  context: Context,
  sfx: RiftSoundSynthesizer,
  syndicateTag: String,
  callsign: String,
  visualRev: Int,
  onShowToast: (String) -> Unit,
) {
  val crest = RiftVisualForgeState.activeSyndicateCrestRecipe
  var promptDraft by remember(visualRev) { mutableStateOf(crest.promptUsed) }
  var variationSalt by remember { mutableIntStateOf(0) }
  val cleanBadgeTag =
    remember(syndicateTag, callsign) {
      val c = RiftGameEngine.cleanTag(syndicateTag)
      if (c.isBlank() || c in setOf("GUEST", "FREE", "RFT", "BOT")) callsign.take(4).uppercase(Locale.US) else c
    }

  Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
    Column(
      modifier =
        Modifier.weight(0.42f)
          .fillMaxHeight()
          .background(Color(0xFF071120), RoundedCornerShape(10.dp))
          .border(1.dp, Color(0xFF1E3A5F), RoundedCornerShape(10.dp))
          .padding(10.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.SpaceBetween,
    ) {
      Text(
        "SYNDICATE HERALDRY PREVIEW",
        color = Color(0xFFFACC15),
        fontSize = 10.sp,
        fontWeight = FontWeight.Black,
        fontFamily = FontFamily.Monospace,
      )
      Box(
        modifier =
          Modifier.fillMaxWidth()
            .weight(1f)
            .padding(vertical = 8.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF040914))
            .border(1.dp, Color(0x55FACC15), RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center,
      ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
          val cx = size.width * 0.5f
          val cy = size.height * 0.5f
          val r = (size.minDimension * 0.36f).coerceIn(36f, 110f)
          drawIntoCanvas { canvas ->
            RiftSpriteBaker.drawSyndicateCrestOnCanvas(canvas.nativeCanvas, cx, cy, r, crest, cleanBadgeTag)
          }
        }
      }
      Text(
        "${crest.shieldShapeLabel.uppercase(Locale.US)} • ${crest.emblemGlyphLabel.uppercase(Locale.US)}",
        color = Color(0xFFE2E8F0),
        fontSize = 9.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace,
      )
    }

    Column(
      modifier =
        Modifier.weight(0.58f)
          .fillMaxHeight()
          .background(Color(0xFF071120), RoundedCornerShape(10.dp))
          .border(1.dp, Color(0xFF1E3A5F), RoundedCornerShape(10.dp))
          .padding(10.dp)
          .verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Text(
        "ON-DEVICE CREST SYNTHESIZER",
        color = Color(0xFF00F5D4),
        fontSize = 10.sp,
        fontWeight = FontWeight.Black,
        fontFamily = FontFamily.Monospace,
      )
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        ForgeTextInputBox(
          promptDraft,
          { promptDraft = it.take(110) },
          "Describe crest (e.g. sovereign gold falcon shield, crimson twin blades)...",
          Modifier.weight(1f),
        )
        ForgeChip(
          "[FORGE CREST]",
          Color(0xFFB45309),
          Color(0xFFFACC15),
          fontSize = 8.6.sp,
          radius = 7.dp,
          px = 10.dp,
          py = 8.dp,
          tag = "forge_btn_synthesize_crest",
        ) {
          variationSalt++
          RiftVisualForgeState.reforgeSyndicateCrestFromPrompt(
            promptDraft.ifBlank { "Sovereign Gold & Cyan Aegis" },
            variationSalt,
            context,
          )
          sfx.playPrize()
          onShowToast("Syndicate Crest Forged & Saved")
        }
      }
      Text(
        "SHIELD CHASSIS:",
        color = Color(0xFF94A3B8),
        fontSize = 8.5.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace,
      )
      Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        listOf(
          0 to "Hexagon Aegis",
          1 to "Chevron Kite",
          2 to "Octagon Bastion",
          3 to "Diamond Sovereign",
        ).forEach { (idx, name) ->
          val sel = crest.shieldShape == idx
          ForgeChip(
            name,
            if (sel) Color(0xFF3B2A0A) else Color(0xFF0A1426),
            if (sel) Color(0xFFFACC15) else Color(0xFF334155),
            if (sel) Color(0xFFFEF08A) else Color(0xFFCBD5E1),
            fontSize = 8.4.sp,
            mono = false,
          ) { RiftVisualForgeState.updateSyndicateCrest(crest.copy(shieldShape = idx), context) }
        }
      }
      Text(
        "CENTRAL EMBLEM:",
        color = Color(0xFF94A3B8),
        fontSize = 8.5.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace,
      )
      Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        listOf(
          0 to "Rift Singularity",
          1 to "Twin Blades",
          2 to "Crown Spire",
          3 to "Vortex Eye",
          4 to "Winged Falcon",
          5 to "Tactician Visor",
        ).forEach { (idx, name) ->
          val sel = crest.emblemGlyph == idx
          ForgeChip(
            name,
            if (sel) Color(0xFF0E4F5C) else Color(0xFF0A1426),
            if (sel) Color(0xFF00F5D4) else Color(0xFF334155),
            if (sel) Color.White else Color(0xFFCBD5E1),
            fontSize = 8.4.sp,
            mono = false,
          ) { RiftVisualForgeState.updateSyndicateCrest(crest.copy(emblemGlyph = idx), context) }
        }
      }
      ForgeSwatchPickerRow("TRIM RIM", crest.trimHex) { hex ->
        RiftVisualForgeState.updateSyndicateCrest(crest.copy(trimHex = hex), context)
      }
      ForgeSwatchPickerRow("AURA GLOW", crest.glowHex) { hex ->
        RiftVisualForgeState.updateSyndicateCrest(crest.copy(glowHex = hex), context)
      }
    }
  }
}

private fun populateStudioShowcaseSector(engine: RiftGameEngine) {
  if (!engine.hasJoinedMatch) {
    engine.deployToRankedMode(RankedGameMode.SECTOR_CONVERGENCE, engine.activeTeamSizeBracket)
  }
  val cx = engine.arenaCenterX
  val cy = engine.arenaCenterY
  engine.localPlayer.x = cx + 520f
  engine.localPlayer.y = cy
  engine.localPlayer.vx = 0f
  engine.localPlayer.vy = 0f
  engine.localPlayer.flux = engine.localPlayer.maxFlux

  if (engine.anomalies.none { it.id.startsWith("STUDIO_") }) {
    val physR = RiftCalibrationState.riftPhysicalRadius * RiftCalibrationState.anomalyScale
    engine.anomalies.add(SpatialAnomalyEntity("STUDIO_PLASMA", AnomalyType.PLASMA_RIFT, cx + 860f, cy - 420f, physR, 120f, 120f))
    engine.anomalies.add(SpatialAnomalyEntity("STUDIO_REPULSOR", AnomalyType.REPULSOR_RIFT, cx + 1240f, cy - 420f, physR, 120f, 120f))
    engine.anomalies.add(SpatialAnomalyEntity("STUDIO_GRAVITY", AnomalyType.GRAVITY_RIFT, cx + 860f, cy + 420f, physR, 120f, 120f))
    engine.anomalies.add(
      SpatialAnomalyEntity("STUDIO_PHASE_A", AnomalyType.PHASE_RIFT_A, cx + 1240f, cy + 320f, physR, 120f, 120f, partnerId = "STUDIO_PHASE_B")
    )
    engine.anomalies.add(
      SpatialAnomalyEntity("STUDIO_PHASE_B", AnomalyType.PHASE_RIFT_B, cx + 1580f, cy + 520f, physR, 120f, 120f, partnerId = "STUDIO_PHASE_A")
    )
  }
  if (engine.walls.none { it.variantSeed == 777 }) {
    engine.walls.add(WallRect(cx - 980f, cy - 520f, 190f, 190f, SpaceObstacleType.ORBITAL_STATION, 777))
    engine.walls.add(WallRect(cx - 720f, cy - 520f, 175f, 175f, SpaceObstacleType.ASTEROID_STATION, 777))
    engine.walls.add(WallRect(cx - 960f, cy - 260f, 145f, 145f, SpaceObstacleType.CRYSTAL_ASTEROID, 777))
    engine.walls.add(WallRect(cx - 720f, cy - 260f, 130f, 130f, SpaceObstacleType.FLOATING_DEBRIS, 777))
  }
  if (engine.brickWalls.none { it.id == "STUDIO_BARRIER" }) {
    val bLen = RiftCalibrationState.stasisBarrierLength.coerceIn(120f, 520f)
    engine.brickWalls.add(BrickWallEntity("STUDIO_BARRIER", engine.localPlayer.id, engine.localPlayer.freq, cx + 660f, cy - bLen * 0.5f, 22f, bLen, 45f, 45f))
  }
  if (engine.interdictorBubbles.none { it.id == "STUDIO_STASIS" }) {
    val r = RiftCalibrationState.interdictorBubbleRadius
    val dur = RiftCalibrationState.interdictorBubbleDuration
    engine.interdictorBubbles.add(InterdictorBubbleEntity("STUDIO_STASIS", engine.localPlayer.id, engine.localPlayer.freq, "Eclipse", cx + 520f, cy + 440f, r, dur, dur))
  }
  if (engine.greens.size < 6) {
    for (i in 0..5) {
      engine.greens.add(GreenCrate(cx + 380f + i * 95f, cy - 260f, i * 0.6f, i % 11))
    }
  }
}

@Composable
private fun DevPlayfieldAndHudStudioBay(
  context: Context,
  engine: RiftGameEngine,
  sfx: RiftSoundSynthesizer,
  localPrefs: LocalPilotPrefs,
  currentUserId: String,
  callsign: String,
  syndicateTag: String,
  pilotLevel: Int,
  riftShards: Long,
  selectedShipId: Int,
  activeCustomSlot: CustomShipLoadoutSlot,
  slot9BUnlocked: Boolean,
  slot9CUnlocked: Boolean,
  slot9DUnlocked: Boolean,
  slot9EUnlocked: Boolean,
  unlockFullNinthShipOverride: Boolean,
  sfxEnabled: Boolean,
  musicEnabled: Boolean,
  musicBpm: Int,
  musicVolume: Float,
  sfxVolume: Float,
  isCompactPhone: Boolean,
  visualRev: Int,
  calibRev: Int,
  isFullScreenStage: Boolean,
  shardBitmap: androidx.compose.ui.graphics.ImageBitmap,
  shardReadoutText: String,
  onSelectShipForDeploy: (Int) -> Unit,
  onSaveCustomSlot: (CustomShipLoadoutSlot) -> Unit,
  onSwitchCustomSlot: (String) -> Unit,
  onUnlockCustomSlot: (String) -> Boolean,
  onForgeAiHullDesign: (String, String) -> Pair<Boolean, String>,
  onSpendRiftShards: (Long) -> Boolean,
  onToggleSfx: () -> Unit,
  onToggleMusic: () -> Unit,
  onChangeMusicBpm: (Int) -> Unit,
  onChangeMusicVolume: (Float) -> Unit,
  onChangeSfxVolume: (Float) -> Unit,
  onSelectMusicArchetype: (MusicSynthArchetype) -> Unit,
  onForgeCustomMusicPrompt: (String) -> Unit,
  onExitToForgeTabs: () -> Unit,
  onExitToNexusHub: () -> Unit = {},
  onToggleFullScreenStage: (Boolean) -> Unit,
  onEnterHudDragArenaMode: () -> Unit,
  onSyncCalibrationToTesters: () -> Unit,
  onShowToast: (String) -> Unit,
) {
  var isTestFlightMode by remember { mutableStateOf(false) }
  var layerFilter by remember { mutableStateOf<ForgeElementLayer?>(null) }
  var selectedTarget by remember { mutableStateOf(ForgeElementTarget.SAFE_ZONE_PAD) }
  var editingModalTarget by remember { mutableStateOf<ForgeElementTarget?>(null) }
  var showDysonCreationOverlay by remember { mutableStateOf(false) }
  var activeDysonTab by remember { mutableStateOf(DysonCreationTab.SHIPS_RIFT_FRAME) }
  var editingCrateBuffSelection by remember { mutableStateOf<Pair<PowerUpCodexEntry?, SpecialBuffRegistryEntry?>?>(null) }
  var customBuffRosterTick by remember { mutableIntStateOf(0) }
  var showCombatSandboxModal by remember { mutableStateOf(false) }
  var showImportJsonDialog by remember { mutableStateOf(false) }
  var importJsonDraft by remember { mutableStateOf("") }
  var sandboxShipFilter by remember { mutableIntStateOf(0) }

  // Isolated Sandbox RiftGameEngine for [120HZ TEST FLIGHT] — NEVER mutates the live ranked `engine`!
  val sandboxFlightEngine = remember {
    RiftGameEngine(
      sfx = sfx,
      onStatsUpdated = {},
      onPrizeToast = onShowToast,
    ).apply {
      localPlayer.callsign = callsign
      localPlayer.syndicateTag = syndicateTag
      cameraZoom = engine.cameraZoom
      playfieldTextScale = engine.playfieldTextScale
      switchLocalPlayerShip(selectedShipId.coerceIn(1, 10), forceFromHangar = true)
    }
  }

  var camWorldX by remember { mutableFloatStateOf(4000f) }
  var camWorldY by remember { mutableFloatStateOf(4000f) }
  var studioZoom by remember { mutableFloatStateOf(1.00f) }
  var activeBookmarkIdx by remember { mutableIntStateOf(0) }
  var testFlightFrameTick by remember { mutableIntStateOf(0) }

  var continuousNanos by remember { mutableLongStateOf(0L) }
  LaunchedEffect(Unit) {
    while (true) {
      withFrameNanos {
        continuousNanos = it
        if (isTestFlightMode) testFlightFrameTick++
      }
    }
  }

  val vortexAgslShader =
    remember {
      if (Build.VERSION.SDK_INT >= 33) {
        runCatching { RuntimeShader(FORGE_VORTEX_AGSL) }.getOrNull()
      } else null
    }

  val filteredTargets =
    remember(layerFilter) {
      ForgeElementTarget.entries.filter { layerFilter == null || it.layer == layerFilter }
    }

  val customForgedBuffsOnStage =
    remember(calibRev, visualRev, customBuffRosterTick) {
      RiftCodex.getCustomForgedBuffs()
    }

  fun closeAllForgeGlassOverlays() {
    showDysonCreationOverlay = false
    editingCrateBuffSelection = null
    editingModalTarget = null
    showCombatSandboxModal = false
    showImportJsonDialog = false
    RiftCalibrationState.isDraggingAnySlider = false
  }

  fun exitTestFlightCleanly() {
    isTestFlightMode = false
    sandboxFlightEngine.leaveActiveMatchToNexusOrbit()
    sandboxFlightEngine.isInLobbyStaging = true
  }

  fun exitStarForgeToHubSingleAction() {
    closeAllForgeGlassOverlays()
    if (isTestFlightMode) {
      exitTestFlightCleanly()
    }
    onToggleFullScreenStage(false)
    onExitToNexusHub()
  }

  BackHandler {
    when {
      editingCrateBuffSelection != null -> editingCrateBuffSelection = null
      editingModalTarget != null -> editingModalTarget = null
      showDysonCreationOverlay -> showDysonCreationOverlay = false
      showCombatSandboxModal -> showCombatSandboxModal = false
      showImportJsonDialog -> showImportJsonDialog = false
      isTestFlightMode -> exitTestFlightCleanly()
      else -> exitStarForgeToHubSingleAction()
    }
  }

  DisposableEffect(Unit) {
    onDispose {
      closeAllForgeGlassOverlays()
      if (isTestFlightMode) {
        sandboxFlightEngine.leaveActiveMatchToNexusOrbit()
      }
    }
  }

  Box(modifier = Modifier.fillMaxSize().background(Color(0xFF02050B))) {
    if (isTestFlightMode) {
      AndroidView(
        modifier = Modifier.fillMaxSize().testTag("dev_forge_test_flight_arena"),
        factory = { ctx ->
          RiftArenaView(context = ctx, engine = sandboxFlightEngine).apply {
            isModalOverlayOpen = false
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            requestFocus()
          }
        },
        update = { view -> view.isModalOverlayOpen = false },
      )
      RiftHudOverlay(
        engine = sandboxFlightEngine,
        fps = 120,
        sfxEnabled = true,
        unreadCommsCount = 0,
        toastMessage = null,
        frameTick = testFlightFrameTick,
        cameraZoom = sandboxFlightEngine.cameraZoom,
        onOpenHangar = { exitTestFlightCleanly() },
        onOpenCodex = { exitTestFlightCleanly() },
        onOpenSyndicateCommand = { exitTestFlightCleanly() },
        onOpenArmadaFleet = { exitTestFlightCleanly() },
        onOpenComms = { exitTestFlightCleanly() },
        onOpenDossier = { exitTestFlightCleanly() },
        onOpenSettings = { exitTestFlightCleanly() },
        isModalOpen = false,
        onOpenNexus = { exitStarForgeToHubSingleAction() },
        onEngageBrake = { sandboxFlightEngine.autoBraking = !sandboxFlightEngine.autoBraking },
        onToggleMultifire = { sandboxFlightEngine.localPlayer.multifireOn = !sandboxFlightEngine.localPlayer.multifireOn },
      )
      Row(
        modifier =
          Modifier.align(Alignment.TopStart)
            .padding(top = 48.dp, start = 10.dp)
            .zIndex(50f)
            .clip(RoundedCornerShape(9.dp))
            .background(Color(0xEB040A16))
            .border(1.2.dp, Color(0xFF00F5D4), RoundedCornerShape(9.dp))
            .padding(horizontal = 8.dp, vertical = 5.dp)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        ForgeChip(
          text = "[< NEXUS HUB]",
          bg = Color(0xFF0B253A),
          border = Color(0xFF00F5D4),
          textColor = Color(0xFFE0F2FE),
          fontSize = 8.2.sp,
          px = 8.dp,
          py = 4.dp,
        ) {
          sfx.playMutedClick()
          exitStarForgeToHubSingleAction()
        }
        ForgeChip(
          text = "[FREEZE & INSPECT 1:1]",
          bg = Color(0xFF4C1D95),
          border = Color(0xFFC084FC),
          textColor = Color.White,
          fontSize = 8.2.sp,
          px = 8.dp,
          py = 4.dp,
          tag = "dev_forge_btn_exit_test_flight",
        ) { exitTestFlightCleanly() }
        Text("HULL:", color = Color(0xFF00F5D4), fontSize = 8.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
        for (shipId in 1..10) {
          val active = sandboxFlightEngine.localPlayer.shipId == shipId
          ForgeChip(
            text = "$shipId",
            bg = if (active) Color(0xFF0284C7) else Color(0xFF0F172A),
            border = if (active) Color(0xFF00F5D4) else Color(0xFF334155),
            fontSize = 7.8.sp,
            radius = 5.dp,
            px = 6.dp,
            py = 2.5.dp,
          ) { sandboxFlightEngine.switchLocalPlayerShip(shipId, forceFromHangar = true) }
        }
      }
    } else {
      val hitEntriesState = remember { mutableListOf<StageHitEntry>() }
      val crateBuffHitsState = remember { mutableListOf<StageCrateBuffHitEntry>() }
      var dysonSunScreenCenter by remember { mutableStateOf(Offset.Zero) }
      var dysonSunHitRadiusPx by remember { mutableFloatStateOf(0f) }
      var dysonHoloBannerRect by remember { mutableStateOf(Rect.Zero) }

      Canvas(
        modifier =
          Modifier.fillMaxSize()
            .pointerInput(layerFilter) {
              detectTransformGestures { _, pan, zoom, _ ->
                val orb = RiftVisualForgeState.pointLightTestOrbWorldPos
                val baseScale = engine.effectiveCameraScale() * studioZoom
                if (orb != null && hypot(pan.x, pan.y) > 0.5f && zoom == 1f) {
                  val w = size.width.toFloat()
                  val h = size.height.toFloat()
                  val orbSx = w * 0.5f + (orb.x - camWorldX) * baseScale
                  val orbSy = h * 0.5f + (orb.y - camWorldY) * baseScale
                  if (abs(orbSx - w * 0.5f) < w * 0.48f && abs(orbSy - h * 0.5f) < h * 0.48f) {
                    camWorldX = (camWorldX - pan.x / baseScale.coerceAtLeast(0.15f)).coerceIn(200f, 7800f)
                    camWorldY = (camWorldY - pan.y / baseScale.coerceAtLeast(0.15f)).coerceIn(200f, 7800f)
                  }
                } else {
                  studioZoom = (studioZoom * zoom).coerceIn(0.45f, 2.50f)
                  val effScale = (engine.effectiveCameraScale() * studioZoom).coerceAtLeast(0.15f)
                  camWorldX = (camWorldX - pan.x / effScale).coerceIn(200f, 7800f)
                  camWorldY = (camWorldY - pan.y / effScale).coerceIn(200f, 7800f)
                }
              }
            }
            .pointerInput(layerFilter, visualRev, calibRev, camWorldX, camWorldY, studioZoom, customBuffRosterTick) {
              detectTapGestures { tapOffset ->
                // 1. Check 180px Shifting Multicolored Dyson Sun & Holographic "STAR FORGE" Letters tap first
                val hitDysonCore =
                  dysonSunHitRadiusPx > 4f &&
                    hypot(tapOffset.x - dysonSunScreenCenter.x, tapOffset.y - dysonSunScreenCenter.y) <= dysonSunHitRadiusPx
                val hitDysonHoloLetters =
                  !dysonHoloBannerRect.isEmpty && dysonHoloBannerRect.contains(tapOffset)
                if (layerFilter != ForgeElementLayer.HUD && (hitDysonCore || hitDysonHoloLetters)) {
                  editingModalTarget = null
                  editingCrateBuffSelection = null
                  showDysonCreationOverlay = true
                  sfx.playPrize()
                  return@detectTapGestures
                }

                // 2. Check individual Supply Crate Buff specimens on the 1:1 playfield
                if (layerFilter != ForgeElementLayer.HUD) {
                  val crateHit =
                    crateBuffHitsState
                      .filter { hypot(tapOffset.x - it.screenX, tapOffset.y - it.screenY) <= it.hitRadiusPx }
                      .minByOrNull { hypot(tapOffset.x - it.screenX, tapOffset.y - it.screenY) }
                  if (crateHit != null) {
                    editingModalTarget = null
                    showDysonCreationOverlay = false
                    editingCrateBuffSelection = Pair(crateHit.codexEntry, crateHit.customBuff)
                    sfx.playMutedClick()
                    return@detectTapGestures
                  }
                }

                // 3. Check 36 World & HUD ForgeElementTarget specimens
                val hit =
                  hitEntriesState
                    .filter { layerFilter == null || it.target.layer == layerFilter }
                    .filter { hypot(tapOffset.x - it.screenX, tapOffset.y - it.screenY) <= it.hitRadiusPx }
                    .minByOrNull { hypot(tapOffset.x - it.screenX, tapOffset.y - it.screenY) / it.hitRadiusPx.coerceAtLeast(16f) }
                if (hit != null) {
                  selectedTarget = hit.target
                  editingCrateBuffSelection = null
                  showDysonCreationOverlay = false
                  editingModalTarget = hit.target
                  sfx.playMutedClick()
                }
              }
            }
            .testTag("dev_forge_interactive_stage_canvas"),
      ) {
        val keepAlive = continuousNanos + visualRev + calibRev
        if (keepAlive == Long.MIN_VALUE) return@Canvas
        val timeSec = ((System.currentTimeMillis() % 120000L) / 1000f)
        val spinAngle = timeSec * 1.35f
        val w = size.width
        val h = size.height
        val worldScale = (engine.effectiveCameraScale() * studioZoom).coerceIn(0.22f, 2.50f)
        fun w2sX(wx: Float): Float = w * 0.5f + (wx - camWorldX) * worldScale
        fun w2sY(wy: Float): Float = h * 0.5f + (wy - camWorldY) * worldScale

        hitEntriesState.clear()
        crateBuffHitsState.clear()
        dysonSunHitRadiusPx = 0f
        dysonHoloBannerRect = Rect.Zero

        // 1. Deep-Space Nebula & Stars (1:1 backdrop)
        val bgRec = RiftVisualForgeState.getRecipe(ForgeElementTarget.BACKGROUND_NEBULA)
        val bgBase = Color(bgRec.primaryColorInt)
        val bgTrim = Color(bgRec.trimColorInt)
        val bgGlow = Color(bgRec.glowColorInt)
        drawRect(color = bgBase)
        drawCircle(
          brush =
            Brush.radialGradient(
              colors =
                listOf(
                  bgTrim.copy(alpha = (0.16f * bgRec.glowIntensity * (1f - RiftVisualForgeState.ambientSpaceDarkness)).coerceIn(0f, 0.55f)),
                  bgGlow.copy(alpha = (0.10f * bgRec.glowIntensity).coerceIn(0f, 0.40f)),
                  Color.Transparent,
                ),
              center = Offset(w * 0.35f, h * 0.38f),
              radius = max(w, h) * 0.62f,
            ),
          radius = max(w, h) * 0.62f,
          center = Offset(w * 0.35f, h * 0.38f),
        )
        val gridWorldStep = 250f * worldScale
        if (gridWorldStep >= 18f) {
          val startX = ((w * 0.5f - camWorldX * worldScale) % gridWorldStep + gridWorldStep) % gridWorldStep
          var gx = startX
          while (gx < w) {
            drawLine(Color(0x1638BDF8), Offset(gx, 0f), Offset(gx, h), 1f)
            gx += gridWorldStep
          }
          val startY = ((h * 0.5f - camWorldY * worldScale) % gridWorldStep + gridWorldStep) % gridWorldStep
          var gy = startY
          while (gy < h) {
            drawLine(Color(0x1638BDF8), Offset(0f, gy), Offset(w, gy), 1f)
            gy += gridWorldStep
          }
        }
        hitEntriesState += StageHitEntry(ForgeElementTarget.BACKGROUND_NEBULA, w * 0.08f, h * 0.20f, 44.dp.toPx())

        if (layerFilter != ForgeElementLayer.HUD) {
          // 2. Outer Playfield Rift Storm Cloud Wall (1:1 at arenaRadius = 3200f)
          val perimRec = RiftVisualForgeState.getRecipe(ForgeElementTarget.ARENA_PERIMETER_RING)
          val stormWorldR = 3200f
          val stormScreenCx = w2sX(4000f)
          val stormScreenCy = w2sY(4000f)
          val stormScreenR = stormWorldR * worldScale
          drawShaderOrFallbackVortex(
            shader = vortexAgslShader,
            mode = 2f,
            center = Offset(stormScreenCx, stormScreenCy),
            radiusPx = stormScreenR,
            timeSec = timeSec,
            recipe = perimRec,
          )
          val stormTopY = w2sY(4000f - stormWorldR)
          val scoutX = w2sX(4000f)
          val scoutY = w2sY(4000f - stormWorldR + 175f)
          if (stormTopY in -240f..(h + 240f)) {
            val boltPath = Path().apply {
              moveTo(scoutX - 28f * worldScale, stormTopY + 8f * worldScale)
              lineTo(scoutX - 10f * worldScale + sin(timeSec * 19f) * 9f, (stormTopY + scoutY) * 0.5f)
              lineTo(scoutX, scoutY)
            }
            drawPath(boltPath, Color(perimRec.glowColorInt).copy(alpha = 0.88f), style = Stroke(width = 2.4.dp.toPx() * perimRec.borderWeight, cap = StrokeCap.Round))
            drawPath(boltPath, Color.White, style = Stroke(width = 1.0.dp.toPx(), cap = StrokeCap.Round))
            hitEntriesState += StageHitEntry(ForgeElementTarget.ARENA_PERIMETER_RING, scoutX, stormTopY + 18f, 64.dp.toPx())
          }

          // 3. Center (4000, 4000): 180px Dyson Sun "STAR FORGE" Centerpiece + Safe-Zone Rift Rim + Clickable Nexus Station Specimen
          val padRec = RiftVisualForgeState.getRecipe(ForgeElementTarget.SAFE_ZONE_PAD)
          val rimRec = RiftVisualForgeState.getRecipe(ForgeElementTarget.SAFE_ZONE_WALL)
          val szWorldR = 400f * RiftCalibrationState.safeZoneScale * padRec.pixelScale.coerceIn(0.65f, 1.35f)
          val szCx = w2sX(4000f)
          val szCy = w2sY(4000f)
          val szScreenR = (szWorldR * worldScale).coerceAtLeast(18f)
          drawShaderOrFallbackVortex(
            shader = vortexAgslShader,
            mode = 0f,
            center = Offset(szCx, szCy),
            radiusPx = szScreenR,
            timeSec = timeSec,
            recipe = padRec,
          )
          // 180px Shifting Multicolored Dyson Sun with rotating 5-stop metallic Dyson rings emitting upward holographic "STAR FORGE" letters
          val dysonScreenR =
            (180f * RiftCalibrationState.safeZoneScale * padRec.pixelScale.coerceIn(0.65f, 1.45f) * worldScale)
              .coerceAtLeast(18f)
          val dysonLiveLeadCol = sampleDysonStellarSpectrum(timeSec * 0.16f)
          for (bIdx in 0 until 8) {
            val ba = -timeSec * 0.14f + bIdx * (PI.toFloat() * 0.25f)
            val beamCol = sampleDysonStellarSpectrum(timeSec * 0.16f + bIdx * 0.125f)
            val bStart = Offset(szCx + cos(ba) * (dysonScreenR * 0.88f), szCy + sin(ba) * (dysonScreenR * 0.88f))
            val bEnd = Offset(szCx + cos(ba) * (szScreenR * 0.985f), szCy + sin(ba) * (szScreenR * 0.985f))
            drawLine(
              brush =
                Brush.linearGradient(
                  listOf(beamCol.copy(alpha = 0.84f), dysonLiveLeadCol.copy(alpha = 0.55f), Color(rimRec.glowColorInt).copy(alpha = 0.72f)),
                  start = bStart,
                  end = bEnd,
                ),
              start = bStart,
              end = bEnd,
              strokeWidth = 2.1.dp.toPx(),
            )
          }
          dysonHoloBannerRect =
            drawShiftingMulticoloredDysonSunWithHoloLetters(
              center = Offset(szCx, szCy),
              dysonRadiusPx = dysonScreenR,
              timeSec = timeSec,
              isSelected = showDysonCreationOverlay,
            )
          drawIntoCanvas { canvas ->
            val nc = canvas.nativeCanvas
            // Clickable Nexus Station Safe-Zone Specimen (north of Dyson Sun at 4000, 3420)
            val nexusStationCx = w2sX(4000f)
            val nexusStationCy = w2sY(3420f)
            val nexusStationR = (118f * padRec.pixelScale.coerceIn(0.65f, 1.45f) * worldScale).coerceAtLeast(14f)
            val bmp = RiftSpriteBaker.safeZoneStationBitmap
            nc.save()
            nc.translate(nexusStationCx, nexusStationCy)
            nc.rotate((timeSec * 8f) % 360f)
            nc.drawBitmap(
              bmp,
              null,
              android.graphics.RectF(-nexusStationR, -nexusStationR, nexusStationR, nexusStationR),
              null,
            )
            nc.restore()
            RiftSpriteBaker.drawSafeZoneStationHoloRing(
              c = nc,
              cx = nexusStationCx,
              cy = nexusStationCy,
              stationRadius = nexusStationR,
              timeSec = timeSec,
              isLockedOut = false,
              alphaScale = padRec.surfaceOpacity,
            )
            hitEntriesState += StageHitEntry(ForgeElementTarget.SAFE_ZONE_PAD, nexusStationCx, nexusStationCy, max(nexusStationR, 30.dp.toPx()))
          }
          dysonSunScreenCenter = Offset(szCx, szCy)
          dysonSunHitRadiusPx = max(dysonScreenR * 1.08f, 40.dp.toPx())

          drawCircle(
            color = Color(rimRec.glowColorInt).copy(alpha = (0.32f * rimRec.glowIntensity).coerceIn(0.08f, 0.85f)),
            radius = szScreenR,
            center = Offset(szCx, szCy),
            style = Stroke(width = 7.dp.toPx() * rimRec.borderWeight),
          )
          drawCircle(
            color = Color(rimRec.trimColorInt).copy(alpha = rimRec.surfaceOpacity),
            radius = szScreenR,
            center = Offset(szCx, szCy),
            style = Stroke(width = 2.2.dp.toPx() * rimRec.borderWeight),
          )
          for (tick in 0 until 24) {
            val a = tick * (PI.toFloat() / 12f) + timeSec * 0.25f
            val r0 = szScreenR * 0.95f
            val r1 = szScreenR * 1.02f
            drawLine(
              Color(rimRec.glowColorInt),
              Offset(szCx + cos(a) * r0, szCy + sin(a) * r0),
              Offset(szCx + cos(a) * r1, szCy + sin(a) * r1),
              1.4.dp.toPx(),
            )
          }
          hitEntriesState += StageHitEntry(ForgeElementTarget.SAFE_ZONE_WALL, szCx + szScreenR, szCy, 44.dp.toPx())

          // 4. True 1:1 Obstacles & Round Metallic-Collared 2.5D Playfield Rifts
          drawIntoCanvas { canvas ->
            val nc = canvas.nativeCanvas
            fun drawObstacle1To1(target: ForgeElementTarget, type: SpaceObstacleType, wx: Float, wy: Float, worldRadius: Float) {
              val rec = RiftVisualForgeState.getRecipe(target)
              val sx = w2sX(wx)
              val sy = w2sY(wy)
              val sr = worldRadius * rec.pixelScale * worldScale
              val bmp = RiftSpriteBaker.getSpaceObstacleBitmap(type, 0)
              nc.drawBitmap(bmp, null, android.graphics.RectF(sx - sr, sy - sr, sx + sr, sy + sr), null)
              hitEntriesState += StageHitEntry(target, sx, sy, max(sr, 28.dp.toPx()))
            }
            drawObstacle1To1(ForgeElementTarget.OBSTACLE_ORBITAL_STATION, SpaceObstacleType.ORBITAL_STATION, 2920f, 3360f, 115f)
            drawObstacle1To1(ForgeElementTarget.OBSTACLE_ASTEROID_STATION, SpaceObstacleType.ASTEROID_STATION, 3360f, 3360f, 105f)
            drawObstacle1To1(ForgeElementTarget.OBSTACLE_SPACE_ASTEROID, SpaceObstacleType.CRYSTAL_ASTEROID, 2940f, 3740f, 84f)
            drawObstacle1To1(ForgeElementTarget.OBSTACLE_FLOATING_DEBRIS, SpaceObstacleType.FLOATING_DEBRIS, 3340f, 3740f, 70f)

            val baseRiftR = RiftCalibrationState.riftPhysicalRadius * RiftCalibrationState.anomalyScale
            fun drawRift1To1(target: ForgeElementTarget, anomType: AnomalyType, wx: Float, wy: Float) {
              val rec = RiftVisualForgeState.getRecipe(target)
              val sx = w2sX(wx)
              val sy = w2sY(wy)
              val sr = (baseRiftR * rec.pixelScale * worldScale).coerceAtLeast(16f)
              RiftSpriteBaker.draw3dPlayfieldRift(nc, anomType, sx, sy, sr, spinAngle * (0.7f + 0.5f * rec.shaderTurbulence))
              hitEntriesState += StageHitEntry(target, sx, sy, max(sr * 0.85f, 30.dp.toPx()))
            }
            drawRift1To1(ForgeElementTarget.RIFT_PLASMA, AnomalyType.PLASMA_RIFT, 4640f, 3340f)
            drawRift1To1(ForgeElementTarget.RIFT_REPULSOR, AnomalyType.REPULSOR_RIFT, 5060f, 3340f)
            drawRift1To1(ForgeElementTarget.RIFT_GRAVITY, AnomalyType.GRAVITY_RIFT, 4640f, 3760f)
            val phaseRec = RiftVisualForgeState.getRecipe(ForgeElementTarget.RIFT_PHASE_PAIR)
            val pR = (baseRiftR * 0.82f * phaseRec.pixelScale * worldScale).coerceAtLeast(14f)
            val pAx = w2sX(5000f)
            val pAy = w2sY(3720f)
            val pBx = w2sX(5260f)
            val pBy = w2sY(3820f)
            RiftSpriteBaker.draw3dPlayfieldRift(nc, AnomalyType.PHASE_RIFT_A, pAx, pAy, pR, spinAngle)
            RiftSpriteBaker.draw3dPlayfieldRift(nc, AnomalyType.PHASE_RIFT_B, pBx, pBy, pR, -spinAngle)
            hitEntriesState += StageHitEntry(ForgeElementTarget.RIFT_PHASE_PAIR, (pAx + pBx) * 0.5f, (pAy + pBy) * 0.5f, max(pR * 1.5f, 36.dp.toPx()))

            val shipAuraRec = RiftVisualForgeState.getRecipe(ForgeElementTarget.SHIP_AURA_LABELS)
            val shipSx = w2sX(4320f)
            val shipSy = w2sY(4080f)
            val shipR = 36f * RiftCalibrationState.shipScale * shipAuraRec.pixelScale * worldScale
            RiftSpriteBaker.drawMetallicShipOnCanvas(nc, shipSx, shipSy, shipR.coerceAtLeast(14f), engine.localPlayer.spec, isAllied = true)
            hitEntriesState += StageHitEntry(ForgeElementTarget.SHIP_AURA_LABELS, shipSx, shipSy, max(shipR * 1.5f, 30.dp.toPx()))
          }

          val shipAuraRec = RiftVisualForgeState.getRecipe(ForgeElementTarget.SHIP_AURA_LABELS)
          val shipSx = w2sX(4320f)
          val shipSy = w2sY(4080f)
          val shipAuraR = 46f * RiftCalibrationState.shipScale * shipAuraRec.pixelScale * worldScale
          drawArc(
            color = Color(shipAuraRec.glowColorInt),
            startAngle = -90f,
            sweepAngle = 290f,
            useCenter = false,
            topLeft = Offset(shipSx - shipAuraR, shipSy - shipAuraR),
            size = Size(shipAuraR * 2f, shipAuraR * 2f),
            style = Stroke(width = 2.4.dp.toPx() * shipAuraRec.borderWeight, cap = StrokeCap.Round),
          )

          // Placed Stasis Barrier (1:1 length)
          val barRec = RiftVisualForgeState.getRecipe(ForgeElementTarget.PLACED_STASIS_BARRIER)
          val barHalf = (RiftCalibrationState.stasisBarrierLength * 0.5f * barRec.pixelScale) * worldScale
          val barX = w2sX(3480f)
          val barY = w2sY(3980f)
          drawLine(
            Color(barRec.glowColorInt).copy(alpha = 0.36f * barRec.glowIntensity.coerceIn(0.2f, 2f)),
            Offset(barX, barY - barHalf),
            Offset(barX, barY + barHalf),
            12.dp.toPx() * barRec.borderWeight,
            StrokeCap.Round,
          )
          drawLine(
            Color(barRec.trimColorInt),
            Offset(barX, barY - barHalf),
            Offset(barX, barY + barHalf),
            3.6.dp.toPx() * barRec.borderWeight,
            StrokeCap.Round,
          )
          drawCircle(Color.White, 4.5.dp.toPx(), Offset(barX, barY - barHalf))
          drawCircle(Color.White, 4.5.dp.toPx(), Offset(barX, barY + barHalf))
          hitEntriesState += StageHitEntry(ForgeElementTarget.PLACED_STASIS_BARRIER, barX, barY, max(barHalf, 32.dp.toPx()))

          // 6. Sector 4 (South): Eclipse Stasis, Flux Core Orb, Stasis Brig Jail, Master Supply Crate Shader, Bolts, Proximity Mine & ALL Individual Supply Crate Buffs
          val stasisRec = RiftVisualForgeState.getRecipe(ForgeElementTarget.ECLIPSE_STASIS_BUBBLE)
          val stasisCx = w2sX(3540f)
          val stasisCy = w2sY(4660f)
          val stasisR = (RiftCalibrationState.interdictorBubbleRadius * stasisRec.pixelScale * worldScale).coerceAtLeast(20f)
          drawShaderOrFallbackVortex(
            shader = vortexAgslShader,
            mode = 1f,
            center = Offset(stasisCx, stasisCy),
            radiusPx = stasisR,
            timeSec = timeSec,
            recipe = stasisRec,
          )
          hitEntriesState += StageHitEntry(ForgeElementTarget.ECLIPSE_STASIS_BUBBLE, stasisCx, stasisCy, max(stasisR * 0.85f, 34.dp.toPx()))

          val coreRec = RiftVisualForgeState.getRecipe(ForgeElementTarget.FLUX_CORE_ORB)
          val coreCx = w2sX(4000f)
          val coreCy = w2sY(4560f)
          val corePulse = 0.5f + 0.5f * sin(timeSec * 3.0f)
          val coreR = ((38f + 6f * corePulse) * coreRec.pixelScale.coerceIn(0.65f, 1.45f) * worldScale).coerceAtLeast(12f)
          drawFluxCoreOrbSpecimen(
            center = Offset(coreCx, coreCy),
            radiusPx = coreR,
            timeSec = timeSec,
            recipe = coreRec,
          )
          hitEntriesState += StageHitEntry(ForgeElementTarget.FLUX_CORE_ORB, coreCx, coreCy, max(coreR * 1.6f, 28.dp.toPx()))

          val brigRec = RiftVisualForgeState.getRecipe(ForgeElementTarget.STASIS_BRIG_JAIL)
          val brigCx = w2sX(4480f)
          val brigCy = w2sY(4680f)
          val brigR = (360f * brigRec.pixelScale * worldScale).coerceAtLeast(26f)
          val brigCoreR = (54f * brigRec.pixelScale * worldScale).coerceAtLeast(10f)
          drawCircle(Color(brigRec.primaryColorInt).copy(alpha = 0.45f * brigRec.surfaceOpacity), brigR, Offset(brigCx, brigCy))
          drawCircle(Color(brigRec.trimColorInt), brigR, Offset(brigCx, brigCy), style = Stroke(2.6.dp.toPx() * brigRec.borderWeight))
          drawCircle(Color(brigRec.glowColorInt), brigCoreR, Offset(brigCx, brigCy))
          drawCircle(Color.White, brigCoreR * 0.42f, Offset(brigCx, brigCy))
          hitEntriesState += StageHitEntry(ForgeElementTarget.STASIS_BRIG_JAIL, brigCx, brigCy, max(brigCoreR * 2.2f, 36.dp.toPx()))

          val crateRec = RiftVisualForgeState.getRecipe(ForgeElementTarget.SUPPLY_CRATE)
          val crateCx = w2sX(3860f)
          val crateCy = w2sY(4780f)
          val crateR = (39f * RiftCalibrationState.crateScale * crateRec.pixelScale * worldScale).coerceAtLeast(13f)
          drawCircle(
            Color(crateRec.glowColorInt).copy(alpha = (0.24f * crateRec.glowIntensity).coerceIn(0.06f, 0.55f)),
            crateR * 1.18f,
            Offset(crateCx, crateCy),
          )
          drawIntoCanvas { c ->
            val nc = c.nativeCanvas
            val podBmp = RiftSpriteBaker.supplyPodFrameBitmap
            val podR = crateR * 1.12f
            val iconR = crateR * 0.78f
            nc.save()
            nc.translate(crateCx, crateCy)
            nc.rotate((timeSec * 36f) % 360f)
            nc.drawBitmap(podBmp, null, android.graphics.RectF(-podR, -podR, podR, podR), null)
            nc.restore()
            val buffBmp = RiftSpriteBaker.getBuffIconBitmap(0)
            nc.drawBitmap(buffBmp, null, android.graphics.RectF(crateCx - iconR, crateCy - iconR, crateCx + iconR, crateCy + iconR), null)
          }
          hitEntriesState += StageHitEntry(ForgeElementTarget.SUPPLY_CRATE, crateCx, crateCy, max(crateR * 1.55f, 26.dp.toPx()))

          // ALL 15 Individual Supply Crate Buffs (RiftCodex.POWERUP_CODEX) + Custom Forged Buffs on 1:1 Playfield!
          val officialBuffs = RiftCodex.POWERUP_CODEX
          val totalBuffCount = officialBuffs.size + customForgedBuffsOnStage.size
          val colsPerRow = 8
          val labelPaint =
            android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
              color = android.graphics.Color.rgb(226, 232, 240)
              textAlign = android.graphics.Paint.Align.CENTER
              typeface = android.graphics.Typeface.create(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.BOLD)
              textSize = (11f * worldScale).coerceIn(8.5f, 13.5f)
            }
          drawIntoCanvas { c ->
            val nc = c.nativeCanvas
            val podBmp = RiftSpriteBaker.supplyPodFrameBitmap
            for (idx in 0 until totalBuffCount) {
              val row = idx / colsPerRow
              val col = idx % colsPerRow
              val wx = 3300f + col * 200f
              val wy = 5080f + row * 195f
              val bx = w2sX(wx)
              val by = w2sY(wy)
              val bCrateR = (36f * RiftCalibrationState.crateScale * crateRec.pixelScale * worldScale).coerceAtLeast(12f)
              val podR = bCrateR * 1.12f
              val iconR = bCrateR * 0.78f

              val codexItem = officialBuffs.getOrNull(idx)
              val customItem = if (codexItem == null) customForgedBuffsOnStage.getOrNull(idx - officialBuffs.size) else null
              val buffBmp =
                when {
                  codexItem != null -> RiftSpriteBaker.getBuffIconBitmap(codexItem.buffTypeIndex)
                  customItem != null -> RiftSpriteBaker.bakeCustomBuffIconBitmap(customItem, 64)
                  else -> RiftSpriteBaker.getBuffIconBitmap(0)
                }

              nc.save()
              nc.translate(bx, by)
              nc.rotate(((timeSec * 32f) + idx * 24f) % 360f)
              nc.drawBitmap(podBmp, null, android.graphics.RectF(-podR, -podR, podR, podR), null)
              nc.restore()
              nc.drawBitmap(buffBmp, null, android.graphics.RectF(bx - iconR, by - iconR, bx + iconR, by + iconR), null)

              if (worldScale >= 0.44f) {
                val shortTag =
                  codexItem?.title?.substringBefore("(")?.trim()?.take(11)
                    ?: customItem?.shortLabel?.take(11)
                    ?: "BUFF"
                nc.drawText(shortTag, bx, by + podR + (12f * worldScale).coerceAtLeast(10f), labelPaint)
              }

              crateBuffHitsState +=
                StageCrateBuffHitEntry(
                  codexEntry = codexItem,
                  customBuff = customItem,
                  screenX = bx,
                  screenY = by,
                  hitRadiusPx = max(podR * 1.25f, 24.dp.toPx()),
                )
            }
          }

          val boltRec = RiftVisualForgeState.getRecipe(ForgeElementTarget.PROJECTILE_PRIMARY_BOLT)
          val boltCx = w2sX(4120f)
          val boltCy = w2sY(4760f)
          val boltLen = (34f * RiftCalibrationState.projectileScale * boltRec.pixelScale * worldScale).coerceAtLeast(14f)
          drawLine(
            Color(boltRec.glowColorInt).copy(alpha = 0.45f),
            Offset(boltCx - boltLen, boltCy),
            Offset(boltCx + boltLen, boltCy),
            8.dp.toPx() * boltRec.borderWeight,
            StrokeCap.Round,
          )
          drawLine(
            Color(boltRec.trimColorInt),
            Offset(boltCx - boltLen, boltCy),
            Offset(boltCx + boltLen, boltCy),
            3.dp.toPx() * boltRec.borderWeight,
            StrokeCap.Round,
          )
          hitEntriesState += StageHitEntry(ForgeElementTarget.PROJECTILE_PRIMARY_BOLT, boltCx, boltCy, max(boltLen, 28.dp.toPx()))

          val mineRec = RiftVisualForgeState.getRecipe(ForgeElementTarget.PROJECTILE_PROXIMITY_MINE)
          val mineCx = w2sX(4120f)
          val mineCy = w2sY(4890f)
          val mineR = (22f * RiftCalibrationState.projectileScale * mineRec.pixelScale * worldScale).coerceAtLeast(10f)
          val fuseR = (RiftCalibrationState.proxFuseRadius * 0.35f * worldScale).coerceAtLeast(mineR * 1.6f)
          drawCircle(Color(mineRec.glowColorInt).copy(alpha = 0.25f), fuseR, Offset(mineCx, mineCy), style = Stroke(1.2.dp.toPx()))
          drawCircle(Color(mineRec.primaryColorInt), mineR, Offset(mineCx, mineCy))
          drawCircle(Color(mineRec.trimColorInt), mineR, Offset(mineCx, mineCy), style = Stroke(2.dp.toPx() * mineRec.borderWeight))
          drawCircle(Color(mineRec.glowColorInt), mineR * 0.36f, Offset(mineCx, mineCy))
          hitEntriesState += StageHitEntry(ForgeElementTarget.PROJECTILE_PROXIMITY_MINE, mineCx, mineCy, max(fuseR, 28.dp.toPx()))

          // Point-Light Test Orb if active
          RiftVisualForgeState.pointLightTestOrbWorldPos?.let { orbW ->
            val ox = w2sX(orbW.x)
            val oy = w2sY(orbW.y)
            val lr = 240f * RiftVisualForgeState.projectileLightRadius * worldScale
            drawCircle(
              brush = Brush.radialGradient(listOf(Color(0x66F5D0FE), Color(0x2200F5D4), Color.Transparent), Offset(ox, oy), lr.coerceAtLeast(24f)),
              radius = lr.coerceAtLeast(24f),
              center = Offset(ox, oy),
            )
            drawCircle(Color(0xFFE879F9), 8.dp.toPx(), Offset(ox, oy))
            drawCircle(Color.White, 3.5.dp.toPx(), Offset(ox, oy))
          }
        }

        // 7. 1:1 HUD Layer Specimens (anchored at true screen coordinates + live HUD offsets)
        if (layerFilter != ForgeElementLayer.WORLD) {
          fun drawHudPillSpecimen(
            target: ForgeElementTarget,
            cx: Float,
            cy: Float,
            pillW: Float,
            pillH: Float,
            isCircle: Boolean = false,
          ) {
            val rec = RiftVisualForgeState.getRecipe(target)
            val off = RiftVisualForgeState.getHudOffsetForTarget(target) ?: Offset.Zero
            val fx = cx + off.x
            val fy = cy + off.y
            val sw = pillW * rec.pixelScale
            val sh = pillH * rec.pixelScale
            val fill = Color(rec.primaryColorInt).copy(alpha = rec.surfaceOpacity.coerceIn(0.25f, 0.96f))
            val rim = Color(rec.trimColorInt)
            if (isCircle) {
              val r = min(sw, sh) * 0.5f
              drawCircle(Color(rec.glowColorInt).copy(alpha = 0.20f * rec.glowIntensity), r * 1.18f, Offset(fx, fy))
              drawCircle(fill, r, Offset(fx, fy))
              drawCircle(rim, r, Offset(fx, fy), style = Stroke(1.8.dp.toPx() * rec.borderWeight))
              hitEntriesState += StageHitEntry(target, fx, fy, max(r * 1.15f, 26.dp.toPx()))
            } else {
              drawRoundRect(fill, Offset(fx - sw * 0.5f, fy - sh * 0.5f), Size(sw, sh), CornerRadius(10.dp.toPx(), 10.dp.toPx()))
              drawRoundRect(rim, Offset(fx - sw * 0.5f, fy - sh * 0.5f), Size(sw, sh), CornerRadius(10.dp.toPx(), 10.dp.toPx()), style = Stroke(1.6.dp.toPx() * rec.borderWeight))
              hitEntriesState += StageHitEntry(target, fx, fy, max(max(sw, sh) * 0.52f, 28.dp.toPx()))
            }
          }

          drawHudPillSpecimen(ForgeElementTarget.HUD_TOP_BAR, w * 0.50f, 54.dp.toPx(), 230.dp.toPx(), 26.dp.toPx())
          drawHudPillSpecimen(ForgeElementTarget.HUD_KILL_FEED, w - 126.dp.toPx(), 88.dp.toPx(), 190.dp.toPx(), 48.dp.toPx())
          drawHudPillSpecimen(ForgeElementTarget.HUD_WARNING_BANNER, w * 0.50f, 92.dp.toPx(), 220.dp.toPx(), 28.dp.toPx())
          drawHudPillSpecimen(ForgeElementTarget.HUD_SPECTATOR_BOX, w - 138.dp.toPx(), h - 156.dp.toPx(), 210.dp.toPx(), 54.dp.toPx())
          drawHudPillSpecimen(ForgeElementTarget.HUD_BUFF_STRIP, w * 0.50f, h - 152.dp.toPx(), 236.dp.toPx(), 24.dp.toPx())
          drawHudPillSpecimen(ForgeElementTarget.HUD_MINIMAP_CONSOLE, w * 0.50f, h - 84.dp.toPx(), 116.dp.toPx(), 116.dp.toPx(), isCircle = true)
          drawHudPillSpecimen(ForgeElementTarget.HUD_NEXUS_BUTTON, w * 0.50f + 168.dp.toPx(), h - 76.dp.toPx(), 42.dp.toPx(), 42.dp.toPx(), isCircle = true)
          drawHudPillSpecimen(ForgeElementTarget.HUD_BRAKE_BTN, w * 0.50f - 230.dp.toPx(), h - 68.dp.toPx(), 48.dp.toPx(), 48.dp.toPx(), isCircle = true)
          drawHudPillSpecimen(ForgeElementTarget.HUD_MULTI_BTN, w * 0.50f + 230.dp.toPx(), h - 68.dp.toPx(), 48.dp.toPx(), 48.dp.toPx(), isCircle = true)
          drawHudPillSpecimen(ForgeElementTarget.HUD_LEFT_STICK, 108.dp.toPx(), h - 108.dp.toPx(), 108.dp.toPx(), 108.dp.toPx(), isCircle = true)
          drawHudPillSpecimen(ForgeElementTarget.HUD_FIRE_STICK, w - 108.dp.toPx(), h - 108.dp.toPx(), 108.dp.toPx(), 108.dp.toPx(), isCircle = true)
          drawHudPillSpecimen(ForgeElementTarget.HUD_SECONDARY_BTN, w - 208.dp.toPx(), h - 92.dp.toPx(), 52.dp.toPx(), 52.dp.toPx(), isCircle = true)
          drawHudPillSpecimen(ForgeElementTarget.HUD_SPECIAL_BUFF_BTN, w - 186.dp.toPx(), h - 166.dp.toPx(), 52.dp.toPx(), 52.dp.toPx(), isCircle = true)
          drawHudPillSpecimen(ForgeElementTarget.HUD_REPEL_BTN, w - 116.dp.toPx(), h - 206.dp.toPx(), 52.dp.toPx(), 52.dp.toPx(), isCircle = true)

          val fluxX = w * 0.50f - 104.dp.toPx()
          val fluxY = h - 76.dp.toPx()
          val shardX = w * 0.50f + 104.dp.toPx()
          val shardY = h - 76.dp.toPx()
          drawIntoCanvas { canvas ->
            val nc = canvas.nativeCanvas
            RiftSpriteBaker.drawFluxPrismIcon(nc, fluxX, fluxY, 14.dp.toPx(), alpha = 255)
            val sBmp = RiftSpriteBaker.riftShardIconBitmap
            val sr = 14.dp.toPx()
            nc.drawBitmap(sBmp, null, android.graphics.RectF(shardX - sr, shardY - sr, shardX + sr, shardY + sr), null)
          }
          hitEntriesState += StageHitEntry(ForgeElementTarget.CURRENCY_FLUX_ICON, fluxX, fluxY, 26.dp.toPx())
          hitEntriesState += StageHitEntry(ForgeElementTarget.CURRENCY_SHARD_ICON, shardX, shardY, 26.dp.toPx())
        }

        // Subtle selection reticle around every staged specimen
        hitEntriesState.forEach { entry ->
          val isSel = entry.target == selectedTarget
          val rec = RiftVisualForgeState.getRecipe(entry.target)
          val ringCol = if (isSel) Color(0xFF00F5D4) else Color(rec.glowColorInt).copy(alpha = 0.55f)
          val markerR = if (isSel) 15.dp.toPx() else 8.dp.toPx()
          drawCircle(ringCol.copy(alpha = if (isSel) 0.24f else 0.12f), markerR, Offset(entry.screenX, entry.screenY))
          drawCircle(ringCol, markerR, Offset(entry.screenX, entry.screenY), style = Stroke(if (isSel) 2.dp.toPx() else 1.dp.toPx()))
        }
      }

      // Bottom compact target strip (tap any chip to jump & open its 95% Obsidian-Glass Inspector)
      Row(
        modifier =
          Modifier.align(Alignment.BottomCenter)
            .fillMaxWidth()
            .background(Color(0xDD040914))
            .border(0.8.dp, Color(0x3338BDF8))
            .padding(horizontal = 8.dp, vertical = 5.dp)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          "TAP DYSON SUN, ANY CRATE BUFF, OR 1:1 ASSET:",
          color = Color(0xFFFACC15),
          fontSize = 7.8.sp,
          fontWeight = FontWeight.Black,
          fontFamily = FontFamily.Monospace,
        )
        filteredTargets.forEach { target ->
          val sel = target == selectedTarget
          ForgeChip(
            text = target.title,
            bg = if (sel) Color(0xFF4C1D95) else Color(0xFF0B1528),
            border = if (sel) Color(0xFF00F5D4) else Color(0xFF334155),
            textColor = if (sel) Color.White else Color(0xFFCBD5E1),
            fontSize = 7.6.sp,
            radius = 5.dp,
            px = 7.dp,
            py = 3.dp,
            tag = "forge_target_chip_${target.key}",
          ) {
            selectedTarget = target
            editingModalTarget = target
            sfx.playMutedClick()
          }
        }
      }
    }

    // Sleek Floating Top Glass Studio Toolbar
    Column(
      modifier =
        Modifier.align(Alignment.TopCenter)
          .fillMaxWidth()
          .background(Color(0xE6040A16))
          .border(0.8.dp, Color(0x44C084FC))
          .padding(horizontal = 8.dp, vertical = 5.dp)
          .zIndex(45f),
      verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        ForgeChip(
          text = "[< NEXUS HUB]",
          bg = Color(0xFF0B253A),
          border = Color(0xFF00F5D4),
          textColor = Color(0xFFE0F2FE),
          fontSize = 8.2.sp,
          px = 9.dp,
          tag = "forge_btn_back_to_tabs",
        ) {
          sfx.playMutedClick()
          exitStarForgeToHubSingleAction()
        }
        ForgeChip(
          text = "[DYSON FORGE CORE]",
          bg = if (showDysonCreationOverlay) Color(0xFF523504) else Color(0xFF281906),
          border = Color(0xFFFFD700),
          textColor = Color(0xFFFEF08A),
          fontSize = 8.sp,
          px = 8.dp,
          tag = "forge_btn_open_dyson_overlay",
        ) {
          editingModalTarget = null
          editingCrateBuffSelection = null
          showDysonCreationOverlay = !showDysonCreationOverlay
          sfx.playPrize()
        }
        ForgeChip(
          text = "[1:1 FROZEN STAGE]",
          bg = if (!isTestFlightMode) Color(0xFF4C1D95) else Color(0xFF0E1B2E),
          border = Color(0xFFC084FC),
          fontSize = 8.sp,
          px = 8.dp,
          tag = "dev_forge_mode_inspect",
        ) {
          if (isTestFlightMode) exitTestFlightCleanly()
        }
        ForgeChip(
          text = "[120HZ TEST FLIGHT]",
          bg = if (isTestFlightMode) Color(0xFF065F46) else Color(0xFF0E1B2E),
          border = Color(0xFF34D399),
          textColor = Color(0xFFA7F3D0),
          fontSize = 8.sp,
          px = 8.dp,
          tag = "dev_forge_mode_test_flight",
        ) {
          if (!isTestFlightMode) {
            sandboxFlightEngine.localPlayer.callsign = callsign
            sandboxFlightEngine.localPlayer.syndicateTag = syndicateTag
            sandboxFlightEngine.switchLocalPlayerShip(selectedShipId.coerceIn(1, 10), forceFromHangar = true)
            populateStudioShowcaseSector(sandboxFlightEngine)
            sandboxFlightEngine.isInLobbyStaging = false
            editingModalTarget = null
            editingCrateBuffSelection = null
            showDysonCreationOverlay = false
            isTestFlightMode = true
          }
        }
        ForgeChip(
          text = "[CRATE BUFFS (${RiftCodex.POWERUP_CODEX.size + customForgedBuffsOnStage.size})]",
          bg = Color(0xFF0A241E),
          border = Color(0xFF34D399),
          textColor = Color(0xFFA7F3D0),
          fontSize = 7.8.sp,
          px = 7.dp,
          tag = "forge_btn_jump_crate_buffs",
        ) {
          layerFilter = ForgeElementLayer.WORLD
          camWorldX = 4000f
          camWorldY = 5140f
          studioZoom = 1.05f
          sfx.playMutedClick()
        }
        listOf(null to "ALL (36)", ForgeElementLayer.WORLD to "WORLD (20)", ForgeElementLayer.HUD to "HUD (16)").forEach { (flt, lbl) ->
          val sel = layerFilter == flt
          ForgeChip(
            lbl,
            if (sel) Color(0xFF0E4F5C) else Color(0xFF091324),
            if (sel) Color(0xFF00F5D4) else Color(0xFF334155),
            if (sel) Color.White else Color(0xFF94A3B8),
            fontSize = 7.8.sp,
            px = 7.dp,
          ) { layerFilter = flt }
        }
        STUDIO_BOOKMARKS.forEachIndexed { idx, bm ->
          val sel = !isTestFlightMode && activeBookmarkIdx == idx
          ForgeChip(
            text = "[${bm.shortLabel}]",
            bg = if (sel) Color(0xFF1E3A5F) else Color(0xFF091324),
            border = if (sel) Color(0xFF38BDF8) else Color(0xFF284166),
            textColor = if (sel) Color.White else Color(0xFFBAE6FD),
            fontSize = 7.6.sp,
            px = 6.dp,
          ) {
            activeBookmarkIdx = idx
            camWorldX = bm.worldX
            camWorldY = bm.worldY
            studioZoom = bm.zoom
            if (idx == 5) layerFilter = null
          }
        }
        ForgeChip(
          text = "[SCALE ${String.format(Locale.US, "%.2fx", studioZoom)}]",
          bg = Color(0xFF0A1628),
          border = Color(0xFF38BDF8),
          textColor = Color(0xFF00F5D4),
          fontSize = 7.6.sp,
          px = 6.dp,
        ) {
          camWorldX = 4000f
          camWorldY = 4000f
          studioZoom = 1.00f
        }
        ForgeChip(
          text = if (RiftVisualForgeState.pointLightEnabled) "[RIM LIGHT: ON]" else "[RIM LIGHT: OFF]",
          bg = if (RiftVisualForgeState.pointLightEnabled) Color(0xFF3B2A0A) else Color(0xFF091324),
          border = Color(0xFFFACC15),
          textColor = Color(0xFFFDE68A),
          fontSize = 7.8.sp,
          px = 7.dp,
        ) {
          RiftVisualForgeState.pointLightEnabled = !RiftVisualForgeState.pointLightEnabled
          RiftVisualForgeState.visualRevision++
          RiftCalibrationState.savePersistedLayout(context)
        }
        ForgeChip(
          text = if (RiftVisualForgeState.pointLightTestOrbWorldPos != null) "[ORB: ON]" else "[TEST ORB]",
          bg = if (RiftVisualForgeState.pointLightTestOrbWorldPos != null) Color(0xFF3B1366) else Color(0xFF091324),
          border = Color(0xFFE879F9),
          textColor = Color(0xFFF5D0FE),
          fontSize = 7.8.sp,
          px = 7.dp,
        ) {
          RiftVisualForgeState.pointLightTestOrbWorldPos =
            if (RiftVisualForgeState.pointLightTestOrbWorldPos == null) Offset(camWorldX + 180f, camWorldY - 120f) else null
          RiftVisualForgeState.visualRevision++
        }
        ForgeChip(
          "[COMBAT STATS]",
          if (showCombatSandboxModal) Color(0xFF0E4F5C) else Color(0xFF091324),
          Color(0xFF38BDF8),
          Color(0xFFBAE6FD),
          fontSize = 7.8.sp,
          px = 7.dp,
        ) { showCombatSandboxModal = !showCombatSandboxModal }
        ForgeChip(
          "[DRAG HUD]",
          Color(0xFF143D34),
          Color(0xFF00F5D4),
          Color(0xFF6EE7B7),
          fontSize = 7.8.sp,
          px = 7.dp,
          tag = "dev_forge_btn_live_hud_drag",
        ) { onEnterHudDragArenaMode() }
        ForgeChip(
          "[SAVE DEFAULTS]",
          Color(0xFF065F46),
          Color(0xFF34D399),
          fontSize = 7.8.sp,
          px = 7.dp,
          tag = "dev_forge_btn_save_defaults",
        ) {
          RiftCalibrationState.savePersistedLayout(context)
          sfx.playPrize()
          onShowToast("Saved All Visual, Shader, Audio, Crest & Physics Defaults")
        }
        ForgeChip("[COPY BUNDLE]", Color(0xFF1E293B), Color(0xFF94A3B8), Color(0xFFE2E8F0), fontSize = 7.8.sp, px = 7.dp) {
          val (promptOut, modCount) = RiftCalibrationState.generateAiStudioDefaultsPrompt()
          val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
          cm?.setPrimaryClip(ClipData.newPlainText("RIFT_FORGE_BUNDLE", promptOut))
          onShowToast("Copied STAR FORGE Bundle ($modCount Modified Settings) to Clipboard")
        }
        ForgeChip("[IMPORT]", Color(0xFF1E293B), Color(0xFF60A5FA), Color(0xFFBFDBFE), fontSize = 7.8.sp, px = 7.dp) {
          showImportJsonDialog = !showImportJsonDialog
        }
        ForgeChip("[SYNC TESTERS]", Color(0xFF172554), Color(0xFF38BDF8), Color(0xFF7DD3FC), fontSize = 7.8.sp, px = 7.dp) {
          onSyncCalibrationToTesters()
        }
        if (!isFullScreenStage) {
          ForgeChip("[EXPAND 1:1]", Color(0xFF3B1366), Color(0xFFC084FC), Color(0xFFE9D5FF), fontSize = 7.8.sp, px = 7.dp) {
            onToggleFullScreenStage(true)
          }
        }
        Row(
          modifier =
            Modifier.clip(RoundedCornerShape(6.dp))
              .background(Color(0xFF160E2C))
              .border(1.dp, Color(0xFFA855F7), RoundedCornerShape(6.dp))
              .padding(horizontal = 7.dp, vertical = 3.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          Image(bitmap = shardBitmap, contentDescription = "Rift Shards", modifier = Modifier.size(12.dp))
          Text(shardReadoutText, color = Color(0xFFE9D5FF), fontSize = 8.2.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
        }
      }

      if (showImportJsonDialog) {
        Row(
          modifier =
            Modifier.fillMaxWidth()
              .background(Color(0xFF0A1426), RoundedCornerShape(8.dp))
              .border(1.dp, Color(0xFF60A5FA), RoundedCornerShape(8.dp))
              .padding(6.dp),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          ForgeTextInputBox(importJsonDraft, { importJsonDraft = it }, "Paste exported STAR FORGE JSON bundle...", Modifier.weight(1f))
          ForgeChip("[APPLY JSON]", Color(0xFF0284C7), Color(0xFF38BDF8), px = 10.dp, py = 7.dp) {
            if (RiftCalibrationState.importDevForgeBundleJson(importJsonDraft, engine, context)) {
              showImportJsonDialog = false
              importJsonDraft = ""
              onShowToast("Imported STAR FORGE Bundle Live")
            } else onShowToast("Invalid JSON Bundle")
          }
        }
      }
    }

    // 95% Obsidian-Glass Dyson Creation Overlay (opened by clicking the 180px Dyson Sun "STAR FORGE" centerpiece)
    if (showDysonCreationOverlay) {
      BackHandler { showDysonCreationOverlay = false }
      Box(
        modifier =
          Modifier.fillMaxSize()
            .background(Color(0xD902050B))
            .clickable { showDysonCreationOverlay = false }
            .zIndex(78f)
            .testTag("forge_dyson_creation_overlay"),
        contentAlignment = Alignment.Center,
      ) {
        Column(
          modifier =
            Modifier.fillMaxWidth(0.95f)
              .fillMaxHeight(0.94f)
              .clip(RoundedCornerShape(16.dp))
              .background(Color(0xF6060D1A))
              .border(
                1.5.dp,
                Brush.linearGradient(listOf(Color(0xFFFFD700), Color(0xFF00F5D4), Color(0xFFC084FC))),
                RoundedCornerShape(16.dp),
              )
              .clickable(enabled = false) {}
              .padding(12.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Row(
              modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState()),
              horizontalArrangement = Arrangement.spacedBy(6.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                "STAR FORGE // DYSON CREATION CORE",
                color = Color(0xFFFFD700),
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
              )
              DysonCreationTab.entries.forEach { dTab ->
                val isSel = activeDysonTab == dTab
                ForgeChip(
                  text = dTab.label,
                  bg = if (isSel) dTab.accent.copy(alpha = 0.25f) else Color(0xFF0B1528),
                  border = if (isSel) dTab.accent else dTab.accent.copy(alpha = 0.45f),
                  textColor = if (isSel) Color.White else dTab.accent,
                  fontSize = 8.4.sp,
                  px = 9.dp,
                  py = 5.dp,
                  tag = "dyson_tab_${dTab.name.lowercase(Locale.US)}",
                ) { activeDysonTab = dTab }
              }
              ForgeChip(
                text = "[NEXUS STATION SHADER]",
                bg = Color(0xFF0E293D),
                border = Color(0xFF38BDF8),
                textColor = Color(0xFFBAE6FD),
                fontSize = 8.2.sp,
                px = 8.dp,
                py = 5.dp,
              ) {
                showDysonCreationOverlay = false
                selectedTarget = ForgeElementTarget.SAFE_ZONE_PAD
                editingModalTarget = ForgeElementTarget.SAFE_ZONE_PAD
              }
            }
            Spacer(modifier = Modifier.width(8.dp))
            ForgeChip(
              text = "[CLOSE TO 1:1 STAGE]",
              bg = Color(0xFF4C1D95),
              border = Color(0xFF00F5D4),
              textColor = Color.White,
              fontSize = 8.4.sp,
              px = 10.dp,
              py = 5.5.dp,
              tag = "dyson_overlay_close_btn",
            ) { showDysonCreationOverlay = false }
          }

          Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (activeDysonTab) {
              DysonCreationTab.SHIPS_RIFT_FRAME ->
                ShipAndCosmeticTurntableBay(
                  localPrefs = localPrefs,
                  currentUserId = currentUserId,
                  pilotLevel = pilotLevel,
                  riftShards = riftShards,
                  selectedShipId = selectedShipId,
                  activeCustomSlot = activeCustomSlot,
                  slot9BUnlocked = slot9BUnlocked,
                  slot9CUnlocked = slot9CUnlocked,
                  slot9DUnlocked = slot9DUnlocked,
                  slot9EUnlocked = slot9EUnlocked,
                  unlockFullNinthShipOverride = unlockFullNinthShipOverride,
                  isCompactPhone = isCompactPhone,
                  onSelectShipForDeploy = onSelectShipForDeploy,
                  onSaveCustomSlot = onSaveCustomSlot,
                  onSwitchCustomSlot = onSwitchCustomSlot,
                  onUnlockCustomSlot = onUnlockCustomSlot,
                  onForgeAiHullDesign = onForgeAiHullDesign,
                  onSpendRiftShards = onSpendRiftShards,
                  onShowToast = onShowToast,
                )
              DysonCreationTab.CUSTOM_BUFF_LAB ->
                CustomBuffForgeBay(context, engine, sfx, calibRev, onShowToast)
              DysonCreationTab.AUDIO_SFX_LAB ->
                SoundtrackAndSfxForgeBay(
                  context = context,
                  sfx = sfx,
                  sfxEnabled = sfxEnabled,
                  musicEnabled = musicEnabled,
                  musicBpm = musicBpm,
                  musicVolume = musicVolume,
                  sfxVolume = sfxVolume,
                  isCompactPhone = isCompactPhone,
                  onToggleSfx = onToggleSfx,
                  onToggleMusic = onToggleMusic,
                  onChangeMusicBpm = onChangeMusicBpm,
                  onChangeMusicVolume = onChangeMusicVolume,
                  onChangeSfxVolume = onChangeSfxVolume,
                  onSelectMusicArchetype = onSelectMusicArchetype,
                  onForgeCustomMusicPrompt = onForgeCustomMusicPrompt,
                  onShowToast = onShowToast,
                )
              DysonCreationTab.SYNDICATE_CREST ->
                SyndicateCrestForgeBay(context, sfx, syndicateTag, callsign, visualRev, onShowToast)
            }
          }
        }
      }
    }

    // 95% Obsidian-Glass Individual Supply Crate Buff Inspector Modal (opened by clicking any Crate Buff on the 1:1 playfield)
    val activeCrateBuffPair = editingCrateBuffSelection
    if (activeCrateBuffPair != null) {
      CrateBuffGlassInspectorModal(
        codexEntry = activeCrateBuffPair.first,
        initialCustomBuff = activeCrateBuffPair.second,
        context = context,
        engine = engine,
        sfx = sfx,
        continuousNanos = continuousNanos,
        onOpenDysonCustomBuffLab = {
          editingCrateBuffSelection = null
          activeDysonTab = DysonCreationTab.CUSTOM_BUFF_LAB
          showDysonCreationOverlay = true
        },
        onOpenPodShaderModal = {
          editingCrateBuffSelection = null
          selectedTarget = ForgeElementTarget.SUPPLY_CRATE
          editingModalTarget = ForgeElementTarget.SUPPLY_CRATE
        },
        onBuffRosterChanged = { customBuffRosterTick++ },
        onDismiss = { editingCrateBuffSelection = null },
        onShowToast = onShowToast,
      )
    }

    // 95% Obsidian-Glass Combat Physics Sandbox Modal (when toggled)
    if (showCombatSandboxModal) {
      BackHandler { showCombatSandboxModal = false }
      Box(
        modifier =
          Modifier.fillMaxSize()
            .background(Color(0xD902050B))
            .clickable { showCombatSandboxModal = false }
            .zIndex(75f),
        contentAlignment = Alignment.Center,
      ) {
        Column(
          modifier =
            Modifier.fillMaxWidth(0.95f)
              .fillMaxHeight(0.95f)
              .clip(RoundedCornerShape(16.dp))
              .background(Color(0xF5060D1A))
              .border(1.5.dp, Brush.linearGradient(listOf(Color(0xFF38BDF8), Color(0xFFC084FC))), RoundedCornerShape(16.dp))
              .clickable(enabled = false) {}
              .padding(14.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              "LIVE COMBAT & PHYSICS SANDBOX",
              color = Color(0xFF00F5D4),
              fontSize = 12.sp,
              fontWeight = FontWeight.Black,
              fontFamily = FontFamily.Monospace,
            )
            ForgeChip("[DONE — BACK TO 1:1 STAGE]", Color(0xFF0E4F5C), Color(0xFF00F5D4), fontSize = 8.6.sp, px = 10.dp, py = 6.dp) {
              showCombatSandboxModal = false
            }
          }
          Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
          ) {
            (listOf(0 to "GLOBAL") + (1..10).map { it to "SHIP $it" }).forEach { (id, lbl) ->
              val sel = sandboxShipFilter == id
              ForgeChip(
                lbl,
                if (sel) Color(0xFF0E4F5C) else Color(0xFF0A1426),
                Color(0xFF38BDF8),
                fontSize = 8.sp,
                radius = 6.dp,
                px = 8.dp,
                py = 4.dp,
              ) { sandboxShipFilter = id }
            }
          }
          val combatDescs =
            remember(sandboxShipFilter, calibRev, visualRev) {
              RiftVisualForgeState.getCombatSandboxDescriptors(sandboxShipFilter)
            }
          Column(
            modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(7.dp),
          ) {
            combatDescs.forEach { desc -> BoundCalibrationDescriptorSlider(desc, engine, context) }
          }
        }
      }
    }

    // 95% Obsidian-Glass Tap-to-Edit Element Reforge Inspector Modal
    val activeModalTarget = editingModalTarget
    if (activeModalTarget != null) {
      ElementReforgeGlassModal(
        target = activeModalTarget,
        engine = engine,
        sfx = sfx,
        context = context,
        visualRev = visualRev,
        calibRev = calibRev,
        continuousNanos = continuousNanos,
        vortexAgslShader = vortexAgslShader,
        onSelectAdjacentTarget = { nextTarget ->
          selectedTarget = nextTarget
          editingModalTarget = nextTarget
        },
        onDismiss = { editingModalTarget = null },
        onShowToast = onShowToast,
      )
    }
  }
}

@Composable
private fun CrateBuffGlassInspectorModal(
  codexEntry: PowerUpCodexEntry?,
  initialCustomBuff: SpecialBuffRegistryEntry?,
  context: Context,
  engine: RiftGameEngine,
  sfx: RiftSoundSynthesizer,
  continuousNanos: Long,
  onOpenDysonCustomBuffLab: () -> Unit,
  onOpenPodShaderModal: () -> Unit,
  onBuffRosterChanged: () -> Unit,
  onDismiss: () -> Unit,
  onShowToast: (String) -> Unit,
) {
  BackHandler { onDismiss() }
  var editableCustom by remember(initialCustomBuff) { mutableStateOf(initialCustomBuff) }
  val title = codexEntry?.title ?: editableCustom?.officialName ?: "SUPPLY CRATE BUFF"
  val category = codexEntry?.category ?: "CUSTOM FORGED SPECIAL BUFF (0 FLUX)"
  val dropLabel =
    if (codexEntry != null) "${codexEntry.dropRatePercent}% CRATE DROP WEIGHT"
    else "CUSTOM FORGED CRATE POOL"
  val summary =
    codexEntry?.effectSummary
      ?: editableCustom?.let {
        "Custom ${it.specialEffect.name} tactical payload dealing ${it.damage.roundToInt()} DMG across ${it.blastRadius.roundToInt()}px radius at ${it.projectileSpeed.roundToInt()} px/s."
      }
      ?: ""
  val tacticalNote =
    codexEntry?.tacticalNote
      ?: "Synthesized in the Star Forge Custom Buff Lab and injected directly into Supply Crates."

  Box(
    modifier =
      Modifier.fillMaxSize()
        .background(Color(0xD902050B))
        .clickable { onDismiss() }
        .zIndex(79f)
        .testTag("forge_crate_buff_inspector_modal"),
    contentAlignment = Alignment.Center,
  ) {
    Column(
      modifier =
        Modifier.fillMaxWidth(0.88f)
          .fillMaxHeight(0.86f)
          .clip(RoundedCornerShape(16.dp))
          .background(Color(0xF6060D1A))
          .border(
            1.5.dp,
            Brush.linearGradient(listOf(Color(0xFF34D399), Color(0xFF00F5D4), Color(0xFFFACC15))),
            RoundedCornerShape(16.dp),
          )
          .clickable(enabled = false) {}
          .padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "1:1 SUPPLY CRATE BUFF INSPECTOR // $category",
            color = Color(0xFF34D399),
            fontSize = 9.2.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
          )
          Text(
            text = title,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
          )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          ForgeChip(
            text = "[POD SHADER]",
            bg = Color(0xFF0E293D),
            border = Color(0xFF38BDF8),
            textColor = Color(0xFFBAE6FD),
            fontSize = 8.2.sp,
            px = 8.dp,
            py = 5.dp,
          ) { onOpenPodShaderModal() }
          ForgeChip(
            text = "[DONE]",
            bg = Color(0xFF065F46),
            border = Color(0xFF00F5D4),
            textColor = Color.White,
            fontSize = 8.4.sp,
            px = 10.dp,
            py = 5.dp,
          ) { onDismiss() }
        }
      }

      Row(
        modifier = Modifier.weight(1f).fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Box(
          modifier =
            Modifier.weight(0.38f)
              .fillMaxHeight()
              .clip(RoundedCornerShape(12.dp))
              .background(Color(0xFF040914))
              .border(1.dp, Color(0x5534D399), RoundedCornerShape(12.dp)),
          contentAlignment = Alignment.Center,
        ) {
          Canvas(modifier = Modifier.fillMaxSize()) {
            if (continuousNanos == Long.MIN_VALUE) return@Canvas
            val t = (System.currentTimeMillis() % 60000L) / 1000f
            val cx = size.width * 0.5f
            val cy = size.height * 0.5f
            val podR = (size.minDimension * 0.30f).coerceIn(28f, 84f)
            val iconR = podR * 0.72f
            drawCircle(Color(0x2834D399), podR * 1.32f, Offset(cx, cy))
            drawIntoCanvas { c ->
              val nc = c.nativeCanvas
              val podBmp = RiftSpriteBaker.supplyPodFrameBitmap
              val buffBmp =
                when {
                  codexEntry != null -> RiftSpriteBaker.getBuffIconBitmap(codexEntry.buffTypeIndex)
                  editableCustom != null -> RiftSpriteBaker.bakeCustomBuffIconBitmap(editableCustom!!, 96)
                  else -> RiftSpriteBaker.getBuffIconBitmap(0)
                }
              nc.save()
              nc.translate(cx, cy)
              nc.rotate((t * 34f) % 360f)
              nc.drawBitmap(podBmp, null, android.graphics.RectF(-podR, -podR, podR, podR), null)
              nc.restore()
              nc.drawBitmap(buffBmp, null, android.graphics.RectF(cx - iconR, cy - iconR, cx + iconR, cy + iconR), null)
            }
          }
          Text(
            text = dropLabel,
            color = Color(0xFFFACC15),
            fontSize = 8.6.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp),
          )
        }

        Column(
          modifier =
            Modifier.weight(0.62f)
              .fillMaxHeight()
              .background(Color(0xFF071120), RoundedCornerShape(12.dp))
              .border(1.dp, Color(0xFF1E3A5F), RoundedCornerShape(12.dp))
              .padding(12.dp)
              .verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
          Text(
            text = summary,
            color = Color(0xFFE2E8F0),
            fontSize = 10.5.sp,
            lineHeight = 15.sp,
          )
          Text(
            text = "TACTICAL NOTE: $tacticalNote",
            color = Color(0xFF67E8F9),
            fontSize = 9.5.sp,
            lineHeight = 13.5.sp,
            fontFamily = FontFamily.Monospace,
          )

          val customSpec = editableCustom
          if (customSpec != null) {
            CompactForgeSliderRow(
              "Payload Damage",
              customSpec.damage,
              40f..1400f,
              "${customSpec.damage.roundToInt()} DMG",
              Color(0xFF34D399),
              onChange = {
                val updated = customSpec.copy(damage = it)
                editableCustom = updated
                RiftCodex.registerCustomBuff(updated)
                RiftCalibrationState.savePersistedLayout(context)
                onBuffRosterChanged()
              },
            )
            CompactForgeSliderRow(
              "Blast / Field Radius",
              customSpec.blastRadius,
              60f..520f,
              "${customSpec.blastRadius.roundToInt()} px",
              Color(0xFF38BDF8),
              onChange = {
                val updated = customSpec.copy(blastRadius = it)
                editableCustom = updated
                RiftCodex.registerCustomBuff(updated)
                RiftCalibrationState.savePersistedLayout(context)
                onBuffRosterChanged()
              },
            )
            CompactForgeSliderRow(
              "Projectile Velocity",
              customSpec.projectileSpeed,
              160f..1300f,
              "${customSpec.projectileSpeed.roundToInt()} px/s",
              Color(0xFFFACC15),
              onChange = {
                val updated = customSpec.copy(projectileSpeed = it)
                editableCustom = updated
                RiftCodex.registerCustomBuff(updated)
                RiftCalibrationState.savePersistedLayout(context)
                onBuffRosterChanged()
              },
            )
          }

          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ForgeChip(
              text = "[CLONE / FORGE VARIANT IN DYSON BUFF LAB]",
              bg = Color(0xFF065F46),
              border = Color(0xFF34D399),
              textColor = Color.White,
              fontSize = 8.5.sp,
              px = 10.dp,
              py = 6.dp,
            ) {
              if (codexEntry != null) {
                val synth =
                  SpecialBuffRegistryEntry.synthesizeFromPrompt(
                    codexEntry.title + " " + codexEntry.effectSummary,
                    RiftCodex.nextCustomBuffTypeIndex(),
                  )
                RiftCodex.registerCustomBuff(synth)
                RiftCalibrationState.savePersistedLayout(context)
                onBuffRosterChanged()
                sfx.playPrize()
                onShowToast("Cloned [${synth.officialName}] into Custom Crate Buff Roster")
              }
              onOpenDysonCustomBuffLab()
            }
          }
        }
      }
    }
  }
}

@Composable
private fun ElementReforgeGlassModal(
  target: ForgeElementTarget,
  engine: RiftGameEngine,
  sfx: RiftSoundSynthesizer,
  context: Context,
  visualRev: Int,
  calibRev: Int,
  continuousNanos: Long,
  vortexAgslShader: RuntimeShader?,
  onSelectAdjacentTarget: (ForgeElementTarget) -> Unit,
  onDismiss: () -> Unit,
  onShowToast: (String) -> Unit,
) {
  BackHandler { onDismiss() }
  var inspectorTab by remember { mutableStateOf(InspectorSubTab.SHADER_PALETTE) }
  var customReforgePrompt by remember(target) { mutableStateOf("") }
  var variationSalt by remember { mutableIntStateOf(0) }
  var previewZoom by remember(target) { mutableFloatStateOf(1.15f) }
  var showNebulaGridBackdrop by remember { mutableStateOf(true) }

  val recipe = remember(target, visualRev) { RiftVisualForgeState.getRecipe(target) }
  val boundDescriptors = remember(target, calibRev, visualRev) { RiftVisualForgeState.getCalibrationDescriptorsForTarget(target) }
  val canUndo = remember(target, visualRev) { RiftVisualForgeState.canUndoTarget(target) }
  val allTargets = ForgeElementTarget.entries
  val targetIdx = allTargets.indexOf(target).coerceAtLeast(0)

  Box(
    modifier =
      Modifier.fillMaxSize()
        .background(Color(0xDC02050B))
        .clickable { onDismiss() }
        .zIndex(80f)
        .testTag("forge_element_inspector_modal"),
    contentAlignment = Alignment.Center,
  ) {
    Column(
      modifier =
        Modifier.fillMaxWidth(0.95f)
          .fillMaxHeight(0.95f)
          .clip(RoundedCornerShape(16.dp))
          .background(Color(0xF6060D1C))
          .border(
            1.5.dp,
            Brush.linearGradient(listOf(Color(0xFFC084FC), Color(0xFF00F5D4), Color(0xFF38BDF8))),
            RoundedCornerShape(16.dp),
          )
          .clickable(enabled = false) {}
          .padding(12.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      // Modal Header
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          modifier = Modifier.weight(1f),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          ForgeChip(
            "<",
            Color(0xFF102038),
            Color(0xFF38BDF8),
            px = 8.dp,
            py = 5.dp,
          ) {
            val prev = allTargets[(targetIdx - 1 + allTargets.size) % allTargets.size]
            onSelectAdjacentTarget(prev)
          }
          ForgeChip(
            ">",
            Color(0xFF102038),
            Color(0xFF38BDF8),
            px = 8.dp,
            py = 5.dp,
          ) {
            val next = allTargets[(targetIdx + 1) % allTargets.size]
            onSelectAdjacentTarget(next)
          }
          Column(modifier = Modifier.weight(1f)) {
            Text(
              "${target.layer.name} ASSET [${targetIdx + 1}/${allTargets.size}] // ${target.title.uppercase(Locale.US)}",
              color = Color(0xFF00F5D4),
              fontSize = 11.5.sp,
              fontWeight = FontWeight.ExtraBold,
              fontFamily = FontFamily.Monospace,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Text(
              "${target.subtitle} • Active Style: ${recipe.promptUsed}",
              color = Color(0xFF94A3B8),
              fontSize = 8.8.sp,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
          ForgeChip(
            text = "[UNDO]",
            bg = if (canUndo) Color(0xFF1E3A5F) else Color(0xFF0F172A),
            border = if (canUndo) Color(0xFF38BDF8) else Color(0xFF334155),
            textColor = if (canUndo) Color.White else Color(0xFF64748B),
            fontSize = 8.2.sp,
            px = 9.dp,
            py = 6.dp,
            enabled = canUndo,
            tag = "dev_forge_btn_undo_target",
          ) { RiftVisualForgeState.undoTarget(target, engine, context) }
          ForgeChip(
            text = "[RESET ELEMENT]",
            bg = Color(0xFF361620),
            border = Color(0xFFEF4444),
            textColor = Color(0xFFFCA5A5),
            fontSize = 8.2.sp,
            px = 9.dp,
            py = 6.dp,
            tag = "dev_forge_btn_reset_target",
          ) {
            RiftVisualForgeState.resetTargetToDefault(target, engine, context)
            onShowToast("Reset ${target.title} to Factory Default")
          }
          ForgeChip(
            text = "[DONE — BACK TO 1:1 STAGE]",
            bg = Color(0xFF0E4F5C),
            border = Color(0xFF00F5D4),
            textColor = Color.White,
            fontSize = 8.5.sp,
            px = 11.dp,
            py = 6.dp,
            tag = "forge_inspector_btn_done",
          ) { onDismiss() }
        }
      }

      // Split Left 60fps Specimen Stage (0.42f) & Right Controls Deck (0.58f)
      Row(modifier = Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(
          modifier =
            Modifier.weight(0.42f)
              .fillMaxHeight()
              .background(Color(0xFF040914), RoundedCornerShape(12.dp))
              .border(1.dp, Color(0x4438BDF8), RoundedCornerShape(12.dp))
              .padding(8.dp),
          verticalArrangement = Arrangement.SpaceBetween,
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              "LIVE 60FPS AGSL / CANVAS SPECIMEN",
              color = Color(0xFFC084FC),
              fontSize = 8.4.sp,
              fontWeight = FontWeight.Black,
              fontFamily = FontFamily.Monospace,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
              ForgeChip(
                text = if (showNebulaGridBackdrop) "[GRID]" else "[VOID]",
                bg = Color(0xFF0D1B30),
                border = Color(0xFF38BDF8),
                fontSize = 7.4.sp,
                px = 6.dp,
                py = 2.5.dp,
              ) { showNebulaGridBackdrop = !showNebulaGridBackdrop }
              ForgeChip(
                text = "${String.format(Locale.US, "%.2fx", previewZoom)}",
                bg = Color(0xFF0D1B30),
                border = Color(0xFF00F5D4),
                fontSize = 7.4.sp,
                px = 6.dp,
                py = 2.5.dp,
              ) { previewZoom = if (previewZoom >= 1.75f) 0.85f else previewZoom + 0.30f }
            }
          }

          Box(
            modifier =
              Modifier.fillMaxWidth()
                .weight(1f)
                .padding(vertical = 6.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF02050B))
                .border(1.dp, Color(0x3300F5D4), RoundedCornerShape(10.dp))
                .pointerInput(target) {
                  detectTransformGestures { _, _, zoom, _ ->
                    previewZoom = (previewZoom * zoom).coerceIn(0.65f, 2.40f)
                  }
                },
            contentAlignment = Alignment.Center,
          ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
              val tick = continuousNanos + visualRev
              if (tick == Long.MIN_VALUE) return@Canvas
              val timeSec = ((System.currentTimeMillis() % 120000L) / 1000f)
              val cx = size.width * 0.5f
              val cy = size.height * 0.5f
              val minDim = size.minDimension * previewZoom
              if (showNebulaGridBackdrop) {
                val step = 28.dp.toPx()
                var gx = step
                while (gx < size.width) {
                  drawLine(Color(0x1638BDF8), Offset(gx, 0f), Offset(gx, size.height), 1f)
                  gx += step
                }
                var gy = step
                while (gy < size.height) {
                  drawLine(Color(0x1638BDF8), Offset(0f, gy), Offset(size.width, gy), 1f)
                  gy += step
                }
              }
              drawIsolatedTargetSpecimen(
                target = target,
                recipe = recipe,
                engine = engine,
                cx = cx,
                cy = cy,
                baseSpan = minDim * 0.34f,
                timeSec = timeSec,
                vortexAgslShader = vortexAgslShader,
              )
            }
          }

          Text(
            "HEX: ${recipe.primaryHex} / ${recipe.trimHex} / ${recipe.glowHex} • TURB ${String.format(Locale.US, "%.2fx", recipe.shaderTurbulence)}",
            color = Color(0xFF7DD3FC),
            fontSize = 7.8.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }

        Column(
          modifier =
            Modifier.weight(0.58f)
              .fillMaxHeight()
              .background(Color(0xFF071120), RoundedCornerShape(12.dp))
              .border(1.dp, Color(0xFF1E3A5F), RoundedCornerShape(12.dp))
              .padding(10.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            InspectorSubTab.entries.forEach { tab ->
              val sel = inspectorTab == tab
              ForgeChip(
                text = tab.label,
                bg = if (sel) Color(0xFF4C1D95) else Color(0xFF0B1528),
                border = if (sel) Color(0xFFC084FC) else Color(0xFF334155),
                textColor = if (sel) Color.White else Color(0xFF94A3B8),
                fontSize = 8.2.sp,
                radius = 7.dp,
                py = 5.dp,
                modifier = Modifier.weight(1f),
              ) { inspectorTab = tab }
            }
          }

          Column(
            modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(7.dp),
          ) {
            if (inspectorTab == InspectorSubTab.SHADER_PALETTE) {
              Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
              ) {
                target.presetPrompts.forEach { (chipTitle, chipPrompt) ->
                  ForgeChip(
                    text = chipTitle,
                    bg = Color(0xFF171033),
                    border = Color(0x99C084FC),
                    textColor = Color(0xFFE9D5FF),
                    fontSize = 8.sp,
                    px = 8.dp,
                    mono = false,
                  ) {
                    variationSalt++
                    RiftVisualForgeState.reforgeTargetFromPrompt(target, chipPrompt, variationSalt, recordUndo = true, context = context)
                    sfx.playMutedClick()
                  }
                }
              }

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                ForgeTextInputBox(
                  customReforgePrompt,
                  { customReforgePrompt = it.take(120) },
                  "Describe shader/palette for ${target.title}...",
                  Modifier.weight(1f),
                )
                ForgeChip(
                  text = "[REFORGE]",
                  bg = Color(0xFF5B21B6),
                  border = Color(0xFF00F5D4),
                  fontSize = 8.4.sp,
                  radius = 7.dp,
                  px = 10.dp,
                  py = 8.dp,
                  tag = "dev_forge_btn_reforge_prompt",
                ) {
                  variationSalt++
                  val prompt = customReforgePrompt.ifBlank { target.presetPrompts.firstOrNull()?.second ?: "Obsidian & Cyan" }
                  RiftVisualForgeState.reforgeTargetFromPrompt(target, prompt, variationSalt, recordUndo = true, context = context)
                  sfx.playPrize()
                }
                ForgeChip(
                  text = "[VARIATION]",
                  bg = Color(0xFF0E3A5D),
                  border = Color(0xFF38BDF8),
                  fontSize = 8.2.sp,
                  radius = 7.dp,
                  px = 9.dp,
                  py = 8.dp,
                ) {
                  variationSalt++
                  val prompt = customReforgePrompt.ifBlank { recipe.promptUsed }
                  RiftVisualForgeState.reforgeTargetFromPrompt(target, prompt, variationSalt, recordUndo = true, context = context)
                  sfx.playMutedClick()
                }
              }

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text(
                  "GEOMETRY:",
                  color = Color(0xFF94A3B8),
                  fontSize = 8.sp,
                  fontWeight = FontWeight.Bold,
                  fontFamily = FontFamily.Monospace,
                )
                for (st in 0..3) {
                  val sel = recipe.geometryStyle == st
                  ForgeChip(
                    "STYLE ${st + 1}",
                    if (sel) Color(0xFF0E4F5C) else Color(0xFF0B1426),
                    if (sel) Color(0xFF00F5D4) else Color(0xFF334155),
                    fontSize = 7.8.sp,
                    radius = 5.dp,
                    py = 3.dp,
                  ) {
                    RiftVisualForgeState.updateRecipe(target, recipe.copy(geometryStyle = st), recordUndo = true, context = context)
                  }
                }
              }

              ForgeSwatchPickerRow("BASE", recipe.primaryHex) { hex ->
                RiftVisualForgeState.updateRecipe(target, recipe.copy(primaryHex = hex), recordUndo = true, context = context)
              }
              ForgeSwatchPickerRow("TRIM", recipe.trimHex) { hex ->
                RiftVisualForgeState.updateRecipe(target, recipe.copy(trimHex = hex), recordUndo = true, context = context)
              }
              ForgeSwatchPickerRow("GLOW", recipe.glowHex) { hex ->
                RiftVisualForgeState.updateRecipe(target, recipe.copy(glowHex = hex), recordUndo = true, context = context)
              }

              val saveLayout = { RiftCalibrationState.savePersistedLayout(context) }
              CompactForgeSliderRow(
                "Element Scale",
                recipe.pixelScale,
                0.50f..2.00f,
                String.format(Locale.US, "%.2fx", recipe.pixelScale),
                Color(0xFF00F5D4),
                { v -> RiftVisualForgeState.updateRecipe(target, recipe.copy(pixelScale = v), recordUndo = false) },
                saveLayout,
              )
              CompactForgeSliderRow(
                "Border Weight",
                recipe.borderWeight,
                0.40f..2.50f,
                String.format(Locale.US, "%.2fx", recipe.borderWeight),
                Color(0xFF38BDF8),
                { v -> RiftVisualForgeState.updateRecipe(target, recipe.copy(borderWeight = v), recordUndo = false) },
                saveLayout,
              )
              CompactForgeSliderRow(
                "Glow Intensity",
                recipe.glowIntensity,
                0.00f..2.50f,
                String.format(Locale.US, "%.2fx", recipe.glowIntensity),
                Color(0xFFC084FC),
                { v -> RiftVisualForgeState.updateRecipe(target, recipe.copy(glowIntensity = v), recordUndo = false) },
                saveLayout,
              )
              CompactForgeSliderRow(
                "Surface Opacity",
                recipe.surfaceOpacity,
                0.15f..1.00f,
                "${(recipe.surfaceOpacity * 100f).roundToInt()}%",
                Color(0xFF34D399),
                { v -> RiftVisualForgeState.updateRecipe(target, recipe.copy(surfaceOpacity = v), recordUndo = false) },
                saveLayout,
              )
              CompactForgeSliderRow(
                "3D Rim-Light Depth",
                recipe.rimLightDepth,
                0.00f..1.50f,
                String.format(Locale.US, "%.2fx", recipe.rimLightDepth),
                Color(0xFFFACC15),
                { v -> RiftVisualForgeState.updateRecipe(target, recipe.copy(rimLightDepth = v), recordUndo = false) },
                saveLayout,
              )
              CompactForgeSliderRow(
                "Surface Detail Density",
                recipe.detailDensity,
                0.00f..1.50f,
                String.format(Locale.US, "%.2fx", recipe.detailDensity),
                Color(0xFF38BDF8),
                { v -> RiftVisualForgeState.updateRecipe(target, recipe.copy(detailDensity = v), recordUndo = false) },
                saveLayout,
              )
              CompactForgeSliderRow(
                "AGSL / FBM Shader Turbulence",
                recipe.shaderTurbulence,
                0.00f..2.00f,
                String.format(Locale.US, "%.2fx", recipe.shaderTurbulence),
                Color(0xFFEC4899),
                { v -> RiftVisualForgeState.updateRecipe(target, recipe.copy(shaderTurbulence = v), recordUndo = false) },
                saveLayout,
              )
            } else {
              val hudOff = RiftVisualForgeState.getHudOffsetForTarget(target)
              if (hudOff != null) {
                Column(
                  modifier =
                    Modifier.fillMaxWidth()
                      .background(Color(0xFF050C18), RoundedCornerShape(8.dp))
                      .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(8.dp))
                      .padding(8.dp),
                  verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    val channelLabel =
                      if (target == ForgeElementTarget.HUD_SPECTATOR_BOX) {
                        "SPECTATOR OFFSET (${hudOff.x.roundToInt()}, ${hudOff.y.roundToInt()}) px"
                      } else {
                        "HUD OFFSET (${hudOff.x.roundToInt()}, ${hudOff.y.roundToInt()}) px"
                      }
                    Text(
                      channelLabel,
                      color = Color(0xFF00F5D4),
                      fontSize = 8.6.sp,
                      fontWeight = FontWeight.Black,
                      fontFamily = FontFamily.Monospace,
                    )
                    ForgeChip("[ZERO OFFSET]", Color(0xFF1E293B), Color(0xFF38BDF8), fontSize = 7.6.sp, px = 7.dp, py = 3.dp) {
                      RiftVisualForgeState.setHudOffsetForTarget(target, Offset.Zero, recordUndo = true)
                      RiftCalibrationState.savePersistedLayout(context)
                    }
                  }
                  Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                    listOf(
                      "X -10" to Offset(-10f, 0f),
                      "X -1" to Offset(-1f, 0f),
                      "X +1" to Offset(1f, 0f),
                      "X +10" to Offset(10f, 0f),
                      "Y -10" to Offset(0f, -10f),
                      "Y +10" to Offset(0f, 10f),
                    ).forEach { (lbl, delta) ->
                      ForgeChip(lbl, Color(0xFF132642), Color(0xFF284B73), fontSize = 7.6.sp, radius = 5.dp, px = 6.dp, py = 3.dp) {
                        RiftVisualForgeState.setHudOffsetForTarget(
                          target,
                          Offset(hudOff.x + delta.x, hudOff.y + delta.y),
                          recordUndo = true,
                        )
                        RiftCalibrationState.savePersistedLayout(context)
                      }
                    }
                  }
                }
              }

              if (boundDescriptors.isEmpty()) {
                Box(
                  modifier =
                    Modifier.fillMaxWidth()
                      .background(Color(0xFF050C18), RoundedCornerShape(8.dp))
                      .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(8.dp))
                      .padding(12.dp),
                ) {
                  Text(
                    "Pure cosmetic icon target — zero gameplay hitbox impact. Customize its 3D toroid/crystal shader & palette in the left tab.",
                    color = Color(0xFF94A3B8),
                    fontSize = 9.sp,
                  )
                }
              } else {
                Text(
                  "BOUND ENGINE & HUD ATTRIBUTES (${boundDescriptors.size})",
                  color = Color(0xFFFACC15),
                  fontSize = 9.sp,
                  fontWeight = FontWeight.Black,
                  fontFamily = FontFamily.Monospace,
                )
                boundDescriptors.forEach { desc -> BoundCalibrationDescriptorSlider(desc, engine, context) }
              }
            }
          }
        }
      }
    }
  }
}

private fun DrawScope.drawShaderOrFallbackVortex(
  shader: RuntimeShader?,
  mode: Float,
  center: Offset,
  radiusPx: Float,
  timeSec: Float,
  recipe: VisualElementRecipe,
) {
  val pCol = Color(recipe.primaryColorInt)
  val tCol = Color(recipe.trimColorInt)
  val gCol = Color(recipe.glowColorInt)
  var drewAgsl = false
  if (Build.VERSION.SDK_INT >= 33 && shader != null) {
    drewAgsl =
      runCatching {
        shader.setFloatUniform("uRes", size.width, size.height)
        shader.setFloatUniform("uCenter", center.x, center.y)
        shader.setFloatUniform("uRadius", radiusPx.coerceAtLeast(8f))
        shader.setFloatUniform("uTime", timeSec)
        shader.setFloatUniform("uTurbulence", recipe.shaderTurbulence.coerceIn(0.05f, 2.0f))
        shader.setFloatUniform("uGlow", recipe.glowIntensity.coerceIn(0.0f, 2.5f))
        shader.setFloatUniform("uOpacity", recipe.surfaceOpacity.coerceIn(0.15f, 1.0f))
        shader.setFloatUniform("uMode", mode)
        shader.setFloatUniform("uPrimary", pCol.red, pCol.green, pCol.blue)
        shader.setFloatUniform("uTrim", tCol.red, tCol.green, tCol.blue)
        shader.setFloatUniform("uAura", gCol.red, gCol.green, gCol.blue)
        val boundR = if (mode > 1.5f) radiusPx + 190f else radiusPx * 1.24f
        val left = (center.x - boundR).coerceAtLeast(0f)
        val top = (center.y - boundR).coerceAtLeast(0f)
        val right = (center.x + boundR).coerceAtMost(size.width)
        val bottom = (center.y + boundR).coerceAtMost(size.height)
        if (right > left && bottom > top) {
          drawRect(brush = ShaderBrush(shader), topLeft = Offset(left, top), size = Size(right - left, bottom - top))
        }
        true
      }.getOrDefault(false)
  }
  if (!drewAgsl) {
    if (mode > 1.5f) {
      // Storm cloud fallback
      for (i in 0..3) {
        val rOff = radiusPx + (i - 1.5f) * 22f * recipe.shaderTurbulence.coerceAtLeast(0.4f)
        drawCircle(
          color = if (i % 2 == 0) tCol.copy(alpha = 0.22f * recipe.surfaceOpacity) else gCol.copy(alpha = 0.28f),
          radius = rOff.coerceAtLeast(8f),
          center = center,
          style = Stroke(width = (18f - i * 3f).dp.toPx() * recipe.borderWeight),
        )
      }
    } else {
      drawCircle(
        brush =
          Brush.radialGradient(
            listOf(
              Color.White.copy(alpha = if (mode < 0.5f) 0.65f else 0.18f),
              tCol.copy(alpha = 0.52f * recipe.surfaceOpacity),
              pCol.copy(alpha = 0.72f * recipe.surfaceOpacity),
              Color.Transparent,
            ),
            center = center,
            radius = (radiusPx * 1.12f).coerceAtLeast(10f),
          ),
        radius = (radiusPx * 1.12f).coerceAtLeast(10f),
        center = center,
      )
      for (arm in 0 until 4) {
        drawArc(
          color = gCol.copy(alpha = 0.68f),
          startAngle = arm * 90f + timeSec * 42f * recipe.shaderTurbulence.coerceAtLeast(0.25f),
          sweepAngle = 58f,
          useCenter = false,
          topLeft = Offset(center.x - radiusPx * 0.88f, center.y - radiusPx * 0.88f),
          size = Size(radiusPx * 1.76f, radiusPx * 1.76f),
          style = Stroke(width = 2.2.dp.toPx() * recipe.borderWeight, cap = StrokeCap.Round),
        )
      }
    }
  }
}

private fun DrawScope.drawIsolatedTargetSpecimen(
  target: ForgeElementTarget,
  recipe: VisualElementRecipe,
  engine: RiftGameEngine,
  cx: Float,
  cy: Float,
  baseSpan: Float,
  timeSec: Float,
  vortexAgslShader: RuntimeShader?,
) {
  val span = (baseSpan * recipe.pixelScale).coerceAtLeast(18f)
  val spin = timeSec * (0.9f + 0.8f * recipe.shaderTurbulence)
  when (target) {
    ForgeElementTarget.SAFE_ZONE_PAD -> {
      drawCircle(Color(0xFF030917), span * 1.04f, Offset(cx, cy))
      drawCircle(
        Color(recipe.glowColorInt).copy(alpha = 0.28f),
        span * 1.04f,
        Offset(cx, cy),
        style = Stroke(2.0.dp.toPx() * recipe.borderWeight),
      )
      val stationR = span * 0.88f
      drawIntoCanvas { c ->
        val nc = c.nativeCanvas
        val bmp = RiftSpriteBaker.safeZoneStationBitmap
        nc.save()
        nc.translate(cx, cy)
        nc.rotate((timeSec * 12f) % 360f)
        nc.drawBitmap(
          bmp,
          null,
          android.graphics.RectF(-stationR, -stationR, stationR, stationR),
          null,
        )
        nc.restore()
        RiftSpriteBaker.drawSafeZoneStationHoloRing(
          c = nc,
          cx = cx,
          cy = cy,
          stationRadius = stationR,
          timeSec = timeSec,
          isLockedOut = false,
          alphaScale = recipe.surfaceOpacity,
        )
      }
    }
    ForgeElementTarget.SAFE_ZONE_WALL -> {
      drawShaderOrFallbackVortex(vortexAgslShader, 0f, Offset(cx, cy), span, timeSec, recipe)
      drawCircle(Color(recipe.trimColorInt), span, Offset(cx, cy), style = Stroke(2.6.dp.toPx() * recipe.borderWeight))
    }
    ForgeElementTarget.ARENA_PERIMETER_RING -> {
      drawShaderOrFallbackVortex(vortexAgslShader, 2f, Offset(cx, cy + span * 1.45f), span * 1.65f, timeSec, recipe)
    }
    ForgeElementTarget.ECLIPSE_STASIS_BUBBLE -> {
      drawShaderOrFallbackVortex(vortexAgslShader, 1f, Offset(cx, cy), span, timeSec, recipe)
    }
    ForgeElementTarget.OBSTACLE_ORBITAL_STATION,
    ForgeElementTarget.OBSTACLE_ASTEROID_STATION,
    ForgeElementTarget.OBSTACLE_SPACE_ASTEROID,
    ForgeElementTarget.OBSTACLE_FLOATING_DEBRIS -> {
      val type =
        when (target) {
          ForgeElementTarget.OBSTACLE_ORBITAL_STATION -> SpaceObstacleType.ORBITAL_STATION
          ForgeElementTarget.OBSTACLE_ASTEROID_STATION -> SpaceObstacleType.ASTEROID_STATION
          ForgeElementTarget.OBSTACLE_SPACE_ASTEROID -> SpaceObstacleType.CRYSTAL_ASTEROID
          else -> SpaceObstacleType.FLOATING_DEBRIS
        }
      drawIntoCanvas { c ->
        val bmp = RiftSpriteBaker.getSpaceObstacleBitmap(type, 0)
        c.nativeCanvas.drawBitmap(bmp, null, android.graphics.RectF(cx - span, cy - span, cx + span, cy + span), null)
      }
    }
    ForgeElementTarget.RIFT_PLASMA,
    ForgeElementTarget.RIFT_REPULSOR,
    ForgeElementTarget.RIFT_GRAVITY,
    ForgeElementTarget.RIFT_PHASE_PAIR -> {
      drawIntoCanvas { c ->
        val nc = c.nativeCanvas
        when (target) {
          ForgeElementTarget.RIFT_PLASMA -> RiftSpriteBaker.draw3dPlayfieldRift(nc, AnomalyType.PLASMA_RIFT, cx, cy, span, spin)
          ForgeElementTarget.RIFT_REPULSOR -> RiftSpriteBaker.draw3dPlayfieldRift(nc, AnomalyType.REPULSOR_RIFT, cx, cy, span, spin)
          ForgeElementTarget.RIFT_GRAVITY -> RiftSpriteBaker.draw3dPlayfieldRift(nc, AnomalyType.GRAVITY_RIFT, cx, cy, span, spin)
          else -> {
            RiftSpriteBaker.draw3dPlayfieldRift(nc, AnomalyType.PHASE_RIFT_A, cx - span * 0.55f, cy, span * 0.62f, spin)
            RiftSpriteBaker.draw3dPlayfieldRift(nc, AnomalyType.PHASE_RIFT_B, cx + span * 0.55f, cy, span * 0.62f, -spin)
          }
        }
      }
    }
    ForgeElementTarget.CURRENCY_FLUX_ICON -> {
      drawIntoCanvas { c -> RiftSpriteBaker.drawFluxPrismIcon(c.nativeCanvas, cx, cy, span * 0.72f, alpha = 255) }
    }
    ForgeElementTarget.CURRENCY_SHARD_ICON -> {
      drawIntoCanvas { c ->
        val bmp = RiftSpriteBaker.riftShardIconBitmap
        val r = span * 0.78f
        c.nativeCanvas.drawBitmap(bmp, null, android.graphics.RectF(cx - r, cy - r, cx + r, cy + r), null)
      }
    }
    ForgeElementTarget.PLACED_STASIS_BARRIER -> {
      drawLine(Color(recipe.glowColorInt).copy(alpha = 0.38f), Offset(cx - span, cy), Offset(cx + span, cy), 14.dp.toPx() * recipe.borderWeight, StrokeCap.Round)
      drawLine(Color(recipe.trimColorInt), Offset(cx - span, cy), Offset(cx + span, cy), 4.dp.toPx() * recipe.borderWeight, StrokeCap.Round)
      drawCircle(Color.White, 5.dp.toPx(), Offset(cx - span, cy))
      drawCircle(Color.White, 5.dp.toPx(), Offset(cx + span, cy))
    }
    ForgeElementTarget.SHIP_AURA_LABELS -> {
      drawIntoCanvas { c ->
        RiftSpriteBaker.drawMetallicShipOnCanvas(c.nativeCanvas, cx, cy, span * 0.58f, engine.localPlayer.spec, isAllied = true)
      }
      drawArc(
        Color(recipe.glowColorInt),
        -90f,
        285f,
        false,
        Offset(cx - span * 0.85f, cy - span * 0.85f),
        Size(span * 1.70f, span * 1.70f),
        style = Stroke(width = 3.dp.toPx() * recipe.borderWeight, cap = StrokeCap.Round),
      )
    }
    ForgeElementTarget.SUPPLY_CRATE -> {
      drawCircle(
        Color(recipe.glowColorInt).copy(alpha = (0.24f * recipe.glowIntensity).coerceIn(0.06f, 0.55f)),
        span * 1.02f,
        Offset(cx, cy),
      )
      drawIntoCanvas { c ->
        val nc = c.nativeCanvas
        val podBmp = RiftSpriteBaker.supplyPodFrameBitmap
        val podR = span * 0.88f
        val iconR = span * 0.60f
        nc.save()
        nc.translate(cx, cy)
        nc.rotate((timeSec * 36f) % 360f)
        nc.drawBitmap(podBmp, null, android.graphics.RectF(-podR, -podR, podR, podR), null)
        nc.restore()
        val buffBmp = RiftSpriteBaker.getBuffIconBitmap(0)
        nc.drawBitmap(buffBmp, null, android.graphics.RectF(cx - iconR, cy - iconR, cx + iconR, cy + iconR), null)
      }
    }
    ForgeElementTarget.FLUX_CORE_ORB -> {
      val corePulse = 0.5f + 0.5f * sin(timeSec * 3.0f)
      drawFluxCoreOrbSpecimen(
        center = Offset(cx, cy),
        radiusPx = (span * (0.74f + 0.06f * corePulse)).coerceAtLeast(16f),
        timeSec = timeSec,
        recipe = recipe,
      )
    }
    else -> {
      val fill = Color(recipe.primaryColorInt).copy(alpha = recipe.surfaceOpacity.coerceIn(0.2f, 0.98f))
      val trim = Color(recipe.trimColorInt)
      val glow = Color(recipe.glowColorInt)
      val isRound =
        target in
          setOf(
            ForgeElementTarget.STASIS_BRIG_JAIL,
            ForgeElementTarget.PROJECTILE_PROXIMITY_MINE,
            ForgeElementTarget.HUD_NEXUS_BUTTON,
            ForgeElementTarget.HUD_MINIMAP_CONSOLE,
            ForgeElementTarget.HUD_LEFT_STICK,
            ForgeElementTarget.HUD_FIRE_STICK,
            ForgeElementTarget.HUD_SECONDARY_BTN,
            ForgeElementTarget.HUD_SPECIAL_BUFF_BTN,
            ForgeElementTarget.HUD_REPEL_BTN,
            ForgeElementTarget.HUD_BRAKE_BTN,
            ForgeElementTarget.HUD_MULTI_BTN,
          )
      if (isRound) {
        drawCircle(glow.copy(alpha = (0.28f * recipe.glowIntensity).coerceIn(0f, 0.85f)), span * 1.18f, Offset(cx, cy))
        drawCircle(fill, span * 0.86f, Offset(cx, cy))
        drawCircle(trim, span * 0.86f, Offset(cx, cy), style = Stroke(2.4.dp.toPx() * recipe.borderWeight))
        drawCircle(glow, span * 0.24f, Offset(cx, cy))
      } else {
        val wRect = span * 1.75f
        val hRect = span * 0.68f
        drawRoundRect(glow.copy(alpha = (0.24f * recipe.glowIntensity).coerceIn(0f, 0.8f)), Offset(cx - wRect * 0.54f, cy - hRect * 0.62f), Size(wRect * 1.08f, hRect * 1.24f), CornerRadius(14.dp.toPx(), 14.dp.toPx()))
        drawRoundRect(fill, Offset(cx - wRect * 0.5f, cy - hRect * 0.5f), Size(wRect, hRect), CornerRadius(12.dp.toPx(), 12.dp.toPx()))
        drawRoundRect(trim, Offset(cx - wRect * 0.5f, cy - hRect * 0.5f), Size(wRect, hRect), CornerRadius(12.dp.toPx(), 12.dp.toPx()), style = Stroke(2.2.dp.toPx() * recipe.borderWeight))
      }
    }
  }
}

@Composable
private fun BoundCalibrationDescriptorSlider(
  desc: CalibrationParamDescriptor,
  engine: RiftGameEngine,
  context: Context,
) {
  var localVal by remember(desc.key, RiftCalibrationState.calibrationRevision) { mutableFloatStateOf(desc.getter()) }
  val fmtVal =
    if (desc.decimals <= 0) "${localVal.roundToInt()} ${desc.unit}".trim()
    else String.format(Locale.US, "%.${desc.decimals}f %s", localVal, desc.unit).trim()
  CompactForgeSliderRow(
    title = desc.title,
    value = localVal.coerceIn(desc.min, desc.max),
    range = desc.min..desc.max,
    valueText = fmtVal,
    accent = Color(0xFFFACC15),
    onChange = { raw ->
      localVal = raw
      desc.setter(raw)
      RiftCalibrationState.applyCalibrationChangesLive(engine, null)
    },
    onFinished = { RiftCalibrationState.applyCalibrationChangesLive(engine, context) },
  )
}

@Composable
private fun ForgeTextInputBox(
  value: String,
  onValueChange: (String) -> Unit,
  placeholder: String,
  modifier: Modifier = Modifier,
) {
  Box(
    modifier =
      modifier
        .height(34.dp)
        .clip(RoundedCornerShape(7.dp))
        .background(Color(0xFF040914))
        .border(1.dp, Color(0xFF284B73), RoundedCornerShape(7.dp))
        .padding(horizontal = 9.dp),
    contentAlignment = Alignment.CenterStart,
  ) {
    if (value.isEmpty()) {
      Text(placeholder, color = Color(0xFF64748B), fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
    BasicTextField(
      value = value,
      onValueChange = onValueChange,
      singleLine = true,
      cursorBrush = SolidColor(Color(0xFF00F5D4)),
      textStyle = TextStyle(color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Medium, fontFamily = FontFamily.SansSerif),
      modifier = Modifier.fillMaxWidth(),
    )
  }
}

@Composable
private fun ForgeSwatchPickerRow(
  label: String,
  selectedHex: String,
  onSelectHex: (String) -> Unit,
) {
  Row(
    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
    horizontalArrangement = Arrangement.spacedBy(5.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      "$label:",
      color = Color(0xFF94A3B8),
      fontSize = 7.8.sp,
      fontWeight = FontWeight.Bold,
      fontFamily = FontFamily.Monospace,
      modifier = Modifier.width(42.dp),
    )
    FORGE_COLOR_SWATCHES.forEach { hex ->
      val isSel = selectedHex.equals(hex, ignoreCase = true)
      val colInt = VisualElementRecipe.parseHexColorSafe(hex, 0xFF38BDF8.toInt())
      Box(
        modifier =
          Modifier.size(18.dp)
            .clip(CircleShape)
            .background(Color(colInt))
            .border(if (isSel) 2.dp else 1.dp, if (isSel) Color.White else Color(0x88475569), CircleShape)
            .clickable { onSelectHex(hex) },
      )
    }
  }
}

@Composable
private fun CompactForgeSliderRow(
  title: String,
  value: Float,
  range: ClosedFloatingPointRange<Float>,
  valueText: String,
  accent: Color,
  onChange: (Float) -> Unit,
  onFinished: () -> Unit = {},
) {
  Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(1.dp)) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(title, color = Color(0xFFCBD5E1), fontSize = 8.4.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
      Text(valueText, color = accent, fontSize = 8.4.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
    }
    Slider(
      value = value.coerceIn(range.start, range.endInclusive),
      onValueChange = onChange,
      onValueChangeFinished = onFinished,
      valueRange = range,
      colors = SliderDefaults.colors(thumbColor = accent, activeTrackColor = accent, inactiveTrackColor = Color(0xFF132642)),
      modifier = Modifier.fillMaxWidth().height(20.dp),
    )
  }
}

private fun DrawScope.drawFluxCoreOrbSpecimen(
  center: Offset,
  radiusPx: Float,
  timeSec: Float,
  recipe: VisualElementRecipe,
) {
  val r = radiusPx.coerceAtLeast(12f)
  val pulsePhase = timeSec * 3.0f
  val pulse = 0.5f + 0.5f * sin(pulsePhase)
  val glowMult = recipe.glowIntensity.coerceIn(0.2f, 1.8f)
  val borderMult = recipe.borderWeight.coerceIn(0.5f, 2.2f)
  val auraCol = if (recipe.isFactoryDefault) Color(0xFFFACC15) else Color(recipe.glowColorInt)
  val trimCol = if (recipe.isFactoryDefault) Color(0xFFFACC15) else Color(recipe.trimColorInt)
  val haloAlpha = (((75f + 50f * pulse) * glowMult) / 255f).coerceIn(0f, 0.92f)

  // 1. Pulsing outer containment aura / halo
  drawCircle(
    color = auraCol.copy(alpha = haloAlpha),
    radius = r * 1.56f,
    center = center,
  )

  // 2. Outer cyan capture ring & inner golden/trim containment collar
  drawCircle(
    color = Color(0xFF22D3EE),
    radius = r * 1.04f,
    center = center,
    style = Stroke(width = (r * 0.105f * borderMult).coerceAtLeast(1.6f)),
  )
  drawCircle(
    color = trimCol,
    radius = r * 0.91f,
    center = center,
    style = Stroke(width = (r * 0.078f * borderMult).coerceAtLeast(1.3f)),
  )

  // 3. Canonical 3D Swirling Plasma Toroid / Tri-Arc Vortex Coil Flux icon centered inside
  drawIntoCanvas { c ->
    val nc = c.nativeCanvas
    nc.save()
    nc.translate(center.x, center.y)
    nc.rotate((pulsePhase * 22f) % 360f)
    RiftSpriteBaker.drawFluxPrismIcon(
      c = nc,
      cx = 0f,
      cy = 0f,
      r = r * 0.80f,
      alpha = 255,
    )
    nc.restore()
  }
}

private val DYSON_SPECTRUM_STOPS =
  listOf(
    Color(0xFFFFD700), // Solar Gold
    Color(0xFF00F5D4), // Electric Cyan
    Color(0xFFEC4899), // Hyper Magenta
    Color(0xFFA855F7), // Astral Violet
    Color(0xFF38BDF8), // Ion Sapphire
    Color(0xFFFB923C), // Stellar Amber
  )

private fun sampleDysonStellarSpectrum(phase: Float): Color {
  val wrapped = ((phase % 1f) + 1f) % 1f
  val scaled = wrapped * DYSON_SPECTRUM_STOPS.size
  val idx0 = scaled.toInt() % DYSON_SPECTRUM_STOPS.size
  val idx1 = (idx0 + 1) % DYSON_SPECTRUM_STOPS.size
  val frac = (scaled - scaled.toInt()).coerceIn(0f, 1f)
  return lerp(DYSON_SPECTRUM_STOPS[idx0], DYSON_SPECTRUM_STOPS[idx1], frac)
}

private fun DrawScope.drawTiltedMetallicDysonRingHemisphere(
  center: Offset,
  rx: Float,
  ry: Float,
  tiltRad: Float,
  spinPhaseRad: Float,
  thicknessPx: Float,
  frontHemisphere: Boolean,
  energyHue: Color,
) {
  val cosTilt = cos(tiltRad)
  val sinTilt = sin(tiltRad)
  val startA = if (frontHemisphere) 0f else PI.toFloat()
  val endA = if (frontHemisphere) PI.toFloat() else (PI.toFloat() * 2f)
  val steps = 36
  val path = Path()
  for (i in 0..steps) {
    val t = startA + (endA - startA) * (i.toFloat() / steps.toFloat())
    val lx = cos(t) * rx
    val ly = sin(t) * ry
    val sx = center.x + lx * cosTilt - ly * sinTilt
    val sy = center.y + lx * sinTilt + ly * cosTilt
    if (i == 0) path.moveTo(sx, sy) else path.lineTo(sx, sy)
  }

  val gradSpanX = cos(tiltRad + spinPhaseRad * 0.35f) * rx
  val gradSpanY = sin(tiltRad + spinPhaseRad * 0.35f) * rx
  val metallic5StopBrush =
    Brush.linearGradient(
      colorStops =
        arrayOf(
          0.00f to Color(0xFF0B1324),
          0.24f to Color(0xFF475569),
          0.50f to Color(0xFFF8FAFC),
          0.76f to Color(0xFF94A3B8),
          1.00f to Color(0xFF1E293B),
        ),
      start = Offset(center.x - gradSpanX, center.y - gradSpanY),
      end = Offset(center.x + gradSpanX, center.y + gradSpanY),
    )
  val depthAlpha = if (frontHemisphere) 0.96f else 0.64f

  // Outer energy conduit aura along the Dyson ring
  drawPath(
    path = path,
    color = energyHue.copy(alpha = if (frontHemisphere) 0.30f else 0.16f),
    style = Stroke(width = thicknessPx * 1.95f, cap = StrokeCap.Round, join = StrokeJoin.Round),
  )
  // Dark gunmetal chassis shadow
  drawPath(
    path = path,
    color = Color(0xFF050A14).copy(alpha = depthAlpha),
    style = Stroke(width = thicknessPx * 1.24f, cap = StrokeCap.Butt, join = StrokeJoin.Round),
  )
  // 5-stop metallic Dyson megastructure band
  drawPath(
    path = path,
    brush = metallic5StopBrush,
    alpha = depthAlpha,
    style = Stroke(width = thicknessPx, cap = StrokeCap.Butt, join = StrokeJoin.Round),
  )
  // Specular highlight filament down the ring spine
  drawPath(
    path = path,
    color = lerp(Color.White, energyHue, 0.35f).copy(alpha = if (frontHemisphere) 0.82f else 0.38f),
    style = Stroke(width = (thicknessPx * 0.22f).coerceAtLeast(1.1f), cap = StrokeCap.Round, join = StrokeJoin.Round),
  )

  // Collector nodes orbiting along the tilted Dyson ring
  val nodeCount = 10
  for (n in 0 until nodeCount) {
    val orbAngle = ((spinPhaseRad + n * (PI.toFloat() * 2f / nodeCount)) % (PI.toFloat() * 2f) + PI.toFloat() * 2f) % (PI.toFloat() * 2f)
    val inHemisphere = if (frontHemisphere) (orbAngle <= PI.toFloat()) else (orbAngle > PI.toFloat())
    if (!inHemisphere) continue
    val lx = cos(orbAngle) * rx
    val ly = sin(orbAngle) * ry
    val nx = center.x + lx * cosTilt - ly * sinTilt
    val ny = center.y + lx * sinTilt + ly * cosTilt
    val nodeR = (thicknessPx * 0.56f).coerceAtLeast(2.2f)
    val nodeHue = sampleDysonStellarSpectrum((orbAngle / (PI.toFloat() * 2f)) + spinPhaseRad * 0.08f)
    drawCircle(
      color = nodeHue.copy(alpha = if (frontHemisphere) 0.45f else 0.22f),
      radius = nodeR * 2.0f,
      center = Offset(nx, ny),
    )
    drawCircle(
      color = Color(0xFFE2E8F0).copy(alpha = depthAlpha),
      radius = nodeR,
      center = Offset(nx, ny),
    )
    drawCircle(
      color = nodeHue.copy(alpha = depthAlpha),
      radius = nodeR * 0.58f,
      center = Offset(nx, ny),
    )
  }
}

private fun buildStarForgeHoloGlyphPath(
  ch: Char,
  left: Float,
  top: Float,
  w: Float,
  h: Float,
): Path {
  val r = left + w
  val b = top + h
  val mx = left + w * 0.5f
  val my = top + h * 0.5f
  val bev = w * 0.20f
  return Path().apply {
    when (ch) {
      'S' -> {
        moveTo(r, top + h * 0.18f)
        lineTo(r - bev, top)
        lineTo(left + bev, top)
        lineTo(left, top + h * 0.18f)
        lineTo(left, my - h * 0.06f)
        lineTo(left + bev, my)
        lineTo(r - bev, my)
        lineTo(r, my + h * 0.08f)
        lineTo(r, b - h * 0.18f)
        lineTo(r - bev, b)
        lineTo(left + bev, b)
        lineTo(left, b - h * 0.18f)
      }
      'T' -> {
        moveTo(left, top)
        lineTo(r, top)
        moveTo(mx, top)
        lineTo(mx, b)
      }
      'A' -> {
        moveTo(left, b)
        lineTo(left + w * 0.16f, top + h * 0.22f)
        lineTo(mx, top)
        lineTo(r - w * 0.16f, top + h * 0.22f)
        lineTo(r, b)
        moveTo(left + w * 0.14f, my + h * 0.10f)
        lineTo(r - w * 0.14f, my + h * 0.10f)
      }
      'R' -> {
        moveTo(left, b)
        lineTo(left, top)
        lineTo(r - bev, top)
        lineTo(r, top + h * 0.16f)
        lineTo(r, my - h * 0.06f)
        lineTo(r - bev, my)
        lineTo(left, my)
        moveTo(mx + w * 0.04f, my)
        lineTo(r, b)
      }
      'F' -> {
        moveTo(left, b)
        lineTo(left, top)
        lineTo(r, top)
        moveTo(left, my)
        lineTo(r - w * 0.18f, my)
      }
      'O' -> {
        moveTo(left + bev, top)
        lineTo(r - bev, top)
        lineTo(r, top + h * 0.20f)
        lineTo(r, b - h * 0.20f)
        lineTo(r - bev, b)
        lineTo(left + bev, b)
        lineTo(left, b - h * 0.20f)
        lineTo(left, top + h * 0.20f)
        close()
      }
      'G' -> {
        moveTo(r, top + h * 0.20f)
        lineTo(r - bev, top)
        lineTo(left + bev, top)
        lineTo(left, top + h * 0.20f)
        lineTo(left, b - h * 0.20f)
        lineTo(left + bev, b)
        lineTo(r - bev, b)
        lineTo(r, b - h * 0.18f)
        lineTo(r, my + h * 0.04f)
        lineTo(mx + w * 0.02f, my + h * 0.04f)
      }
      'E' -> {
        moveTo(r, top)
        lineTo(left, top)
        lineTo(left, b)
        lineTo(r, b)
        moveTo(left, my)
        lineTo(r - w * 0.18f, my)
      }
    }
  }
}

private fun DrawScope.drawShiftingMulticoloredDysonSunWithHoloLetters(
  center: Offset,
  dysonRadiusPx: Float,
  timeSec: Float,
  isSelected: Boolean,
): Rect {
  val r = dysonRadiusPx.coerceAtLeast(18f)
  val basePhase = timeSec * 0.15f
  val hueCore = sampleDysonStellarSpectrum(basePhase)
  val hueMid = sampleDysonStellarSpectrum(basePhase + 0.31f)
  val hueOuter = sampleDysonStellarSpectrum(basePhase + 0.63f)

  // 1. Deep outer shifting multicolored stellar corona halo
  val coronaR = r * 1.56f
  drawCircle(
    brush =
      Brush.radialGradient(
        colorStops =
          arrayOf(
            0.00f to hueCore.copy(alpha = 0.44f),
            0.38f to hueMid.copy(alpha = 0.28f),
            0.72f to hueOuter.copy(alpha = 0.14f),
            1.00f to Color.Transparent,
          ),
        center = center,
        radius = coronaR,
      ),
    radius = coronaR,
    center = center,
  )

  // 2. Back-hemisphere of 3 counter-rotating 5-stop metallic Dyson rings (3D occluded behind sun)
  val ringThickness = (r * 0.082f).coerceIn(2.6f, 16f)
  val tiltA = (-24f + sin(timeSec * 0.42f) * 6f) * (PI.toFloat() / 180f)
  val tiltB = (31f + cos(timeSec * 0.36f) * 7f) * (PI.toFloat() / 180f)
  val tiltC = (-63f + sin(timeSec * 0.29f + 1.2f) * 5f) * (PI.toFloat() / 180f)
  drawTiltedMetallicDysonRingHemisphere(
    center = center,
    rx = r * 1.04f,
    ry = r * 0.40f,
    tiltRad = tiltC,
    spinPhaseRad = -timeSec * 0.85f,
    thicknessPx = ringThickness * 0.84f,
    frontHemisphere = false,
    energyHue = hueOuter,
  )
  drawTiltedMetallicDysonRingHemisphere(
    center = center,
    rx = r * 0.99f,
    ry = r * 0.46f,
    tiltRad = tiltB,
    spinPhaseRad = timeSec * 1.12f,
    thicknessPx = ringThickness * 0.92f,
    frontHemisphere = false,
    energyHue = hueMid,
  )
  drawTiltedMetallicDysonRingHemisphere(
    center = center,
    rx = r * 0.94f,
    ry = r * 0.36f,
    tiltRad = tiltA,
    spinPhaseRad = -timeSec * 1.38f,
    thicknessPx = ringThickness,
    frontHemisphere = false,
    energyHue = hueCore,
  )

  // 3. Animated solar prominences / magnetic plasma loops around the Dyson Sun sphere
  val sunCoreR = r * 0.72f
  val flareCount = 12
  for (fIdx in 0 until flareCount) {
    val baseAng = fIdx * (PI.toFloat() * 2f / flareCount) + timeSec * 0.34f
    val pulse = 0.5f + 0.5f * sin(timeSec * 2.8f + fIdx * 1.37f)
    val flarePeakR = sunCoreR * (1.07f + 0.16f * pulse)
    val span = 0.16f + 0.05f * pulse
    val p0 = Offset(center.x + cos(baseAng - span) * (sunCoreR * 0.94f), center.y + sin(baseAng - span) * (sunCoreR * 0.94f))
    val ctrl = Offset(center.x + cos(baseAng) * flarePeakR, center.y + sin(baseAng) * flarePeakR)
    val p1 = Offset(center.x + cos(baseAng + span) * (sunCoreR * 0.94f), center.y + sin(baseAng + span) * (sunCoreR * 0.94f))
    val flarePath = Path().apply {
      moveTo(p0.x, p0.y)
      quadraticTo(ctrl.x, ctrl.y, p1.x, p1.y)
    }
    val flareHue = sampleDysonStellarSpectrum(basePhase + fIdx * (1f / flareCount))
    drawPath(
      path = flarePath,
      color = flareHue.copy(alpha = 0.56f),
      style = Stroke(width = (r * 0.032f).coerceAtLeast(1.4f), cap = StrokeCap.Round),
    )
  }

  // 4. Shifting Multicolored Dyson Sun Photosphere & Convective Plasma Core
  drawCircle(
    brush =
      Brush.radialGradient(
        colorStops =
          arrayOf(
            0.00f to Color.White,
            0.22f to lerp(Color.White, hueCore, 0.55f),
            0.52f to hueCore,
            0.78f to hueMid,
            0.94f to hueOuter,
            1.00f to hueOuter.copy(alpha = 0.15f),
          ),
        center = Offset(center.x - sunCoreR * 0.14f, center.y - sunCoreR * 0.14f),
        radius = sunCoreR * 1.06f,
      ),
    radius = sunCoreR,
    center = center,
  )

  // Swirling multicolored convective plasma bands across the solar surface
  for (band in 0 until 8) {
    val startDeg = (timeSec * (44f + band * 5f) + band * 45f) % 360f
    val bandR = sunCoreR * (0.34f + (band % 4) * 0.15f)
    val bandHue = sampleDysonStellarSpectrum(basePhase + 0.18f + band * 0.11f)
    drawArc(
      color = lerp(Color.White, bandHue, 0.62f).copy(alpha = 0.44f),
      startAngle = startDeg,
      sweepAngle = 68f + 22f * sin(timeSec * 1.9f + band),
      useCenter = false,
      topLeft = Offset(center.x - bandR, center.y - bandR),
      size = Size(bandR * 2f, bandR * 2f),
      style = Stroke(width = (r * 0.028f).coerceAtLeast(1.2f), cap = StrokeCap.Round),
    )
  }

  // Brilliant solar limb rim
  drawCircle(
    color = Color.White.copy(alpha = 0.72f),
    radius = sunCoreR * 0.985f,
    center = center,
    style = Stroke(width = (r * 0.018f).coerceAtLeast(1.1f)),
  )

  // 5. Front-hemisphere of 3 counter-rotating 5-stop metallic Dyson rings (3D passing in front of sun)
  drawTiltedMetallicDysonRingHemisphere(
    center = center,
    rx = r * 1.04f,
    ry = r * 0.40f,
    tiltRad = tiltC,
    spinPhaseRad = -timeSec * 0.85f,
    thicknessPx = ringThickness * 0.84f,
    frontHemisphere = true,
    energyHue = hueOuter,
  )
  drawTiltedMetallicDysonRingHemisphere(
    center = center,
    rx = r * 0.99f,
    ry = r * 0.46f,
    tiltRad = tiltB,
    spinPhaseRad = timeSec * 1.12f,
    thicknessPx = ringThickness * 0.92f,
    frontHemisphere = true,
    energyHue = hueMid,
  )
  drawTiltedMetallicDysonRingHemisphere(
    center = center,
    rx = r * 0.94f,
    ry = r * 0.36f,
    tiltRad = tiltA,
    spinPhaseRad = -timeSec * 1.38f,
    thicknessPx = ringThickness,
    frontHemisphere = true,
    energyHue = hueCore,
  )

  if (isSelected) {
    val selR = r * 1.12f
    drawCircle(
      color = Color(0xFF00F5D4).copy(alpha = 0.78f),
      radius = selR,
      center = center,
      style = Stroke(width = (r * 0.024f).coerceAtLeast(1.8f)),
    )
  }

  // 6. Upward-Emitted Volumetric Holographic Projection Rays & Graphic Letters Spelling "STAR FORGE"
  val titleChars = "STAR FORGE"
  val glyphH = (r * 0.255f).coerceIn(11f, 52f)
  val glyphW = glyphH * 0.64f
  val glyphGap = glyphW * 0.30f
  val spaceW = glyphW * 0.62f
  var totalBannerW = 0f
  titleChars.forEachIndexed { idx, c ->
    totalBannerW += if (c == ' ') spaceW else glyphW
    if (idx < titleChars.lastIndex) totalBannerW += glyphGap
  }

  val hoverLift = sin(timeSec * 2.1f) * (r * 0.026f)
  val bannerCy = center.y - r * 0.94f + hoverLift
  val bannerTop = bannerCy - glyphH * 0.5f
  val bannerLeft = center.x - totalBannerW * 0.5f

  // Upward emitter cone rays fanning from the Dyson Sun's northern crown up into "STAR FORGE"
  val rayCount = 7
  for (rayIdx in 0 until rayCount) {
    val frac = rayIdx.toFloat() / (rayCount - 1).toFloat()
    val emitterX = center.x + (frac - 0.5f) * (sunCoreR * 0.86f)
    val emitterY = center.y - sunCoreR * 0.46f
    val targetX = bannerLeft + frac * totalBannerW
    val targetY = bannerTop + glyphH * 1.05f
    val rayHue = sampleDysonStellarSpectrum(basePhase + frac * 0.45f)
    drawLine(
      brush =
        Brush.linearGradient(
          listOf(
            rayHue.copy(alpha = 0.42f),
            Color(0xFF00F5D4).copy(alpha = 0.24f),
            Color.Transparent,
          ),
          start = Offset(emitterX, emitterY),
          end = Offset(targetX, bannerTop - glyphH * 0.25f),
        ),
      start = Offset(emitterX, emitterY),
      end = Offset(targetX, targetY),
      strokeWidth = (r * 0.036f).coerceAtLeast(1.6f),
      cap = StrokeCap.Round,
    )
  }

  // Upward-rising holographic plasma motes streaming from the Dyson Sun through the letters
  for (mIdx in 0 until 10) {
    val prog = ((timeSec * 0.58f + mIdx * 0.103f) % 1f)
    val mx = bannerLeft + ((mIdx * 0.108f + sin(timeSec * 1.3f + mIdx) * 0.04f).coerceIn(0.03f, 0.97f)) * totalBannerW
    val my = (center.y - sunCoreR * 0.40f) - prog * (r * 0.78f)
    val moteAlpha = sin(prog * PI.toFloat()).coerceIn(0f, 1f) * 0.78f
    val moteCol = sampleDysonStellarSpectrum(basePhase + mIdx * 0.09f)
    drawCircle(
      color = moteCol.copy(alpha = moteAlpha),
      radius = (r * 0.019f).coerceAtLeast(1.3f),
      center = Offset(mx, my),
    )
  }

  // Translucent holographic projection pedestal plate behind "STAR FORGE"
  val padX = glyphW * 0.48f
  val padY = glyphH * 0.34f
  val bannerRect =
    Rect(
      left = bannerLeft - padX,
      top = bannerTop - padY,
      right = bannerLeft + totalBannerW + padX,
      bottom = bannerTop + glyphH + padY,
    )
  drawRoundRect(
    color = Color(0x99040B18),
    topLeft = Offset(bannerRect.left, bannerRect.top),
    size = Size(bannerRect.width, bannerRect.height),
    cornerRadius = CornerRadius(glyphH * 0.28f, glyphH * 0.28f),
  )
  drawRoundRect(
    brush =
      Brush.linearGradient(
        listOf(
          hueCore.copy(alpha = 0.85f),
          Color(0xFF00F5D4).copy(alpha = 0.88f),
          hueMid.copy(alpha = 0.85f),
        ),
        start = Offset(bannerRect.left, bannerRect.top),
        end = Offset(bannerRect.right, bannerRect.bottom),
      ),
    topLeft = Offset(bannerRect.left, bannerRect.top),
    size = Size(bannerRect.width, bannerRect.height),
    cornerRadius = CornerRadius(glyphH * 0.28f, glyphH * 0.28f),
    style = Stroke(width = (glyphH * 0.065f).coerceAtLeast(1.1f)),
  )

  // Custom 3D holographic vector graphic letters "STAR FORGE"
  var cursorX = bannerLeft
  val coreStroke = (glyphH * 0.135f).coerceIn(1.4f, 6.2f)
  val scanSweepY = bannerTop + ((1f - ((timeSec * 0.85f) % 1f)) * glyphH)
  titleChars.forEachIndexed { idx, ch ->
    if (ch == ' ') {
      cursorX += spaceW + glyphGap
    } else {
      val letterWaveY = sin(timeSec * 3.4f - idx * 0.48f) * (glyphH * 0.065f)
      val letterTop = bannerTop + letterWaveY
      val letterHue = sampleDysonStellarSpectrum(basePhase + idx * 0.075f)
      val shadowPath =
        buildStarForgeHoloGlyphPath(
          ch = ch,
          left = cursorX + coreStroke * 0.55f,
          top = letterTop + coreStroke * 0.62f,
          w = glyphW,
          h = glyphH,
        )
      val mainPath =
        buildStarForgeHoloGlyphPath(
          ch = ch,
          left = cursorX,
          top = letterTop,
          w = glyphW,
          h = glyphH,
        )

      // Pass 1: 3D holographic extrusion shadow
      drawPath(
        path = shadowPath,
        color = Color(0xCC09152B),
        style = Stroke(width = coreStroke * 1.55f, cap = StrokeCap.Square, join = StrokeJoin.Bevel),
      )
      // Pass 2: Shifting multicolored holographic outer bloom
      drawPath(
        path = mainPath,
        color = letterHue.copy(alpha = 0.52f),
        style = Stroke(width = coreStroke * 2.15f, cap = StrokeCap.Round, join = StrokeJoin.Round),
      )
      // Pass 3: Vibrant cyan/stellar holographic body stroke
      drawPath(
        path = mainPath,
        color = lerp(letterHue, Color(0xFF00F5D4), 0.42f),
        style = Stroke(width = coreStroke, cap = StrokeCap.Square, join = StrokeJoin.Bevel),
      )
      // Pass 4: Crisp white-hot inner hologram filament
      drawPath(
        path = mainPath,
        color = Color.White.copy(alpha = 0.92f),
        style = Stroke(width = (coreStroke * 0.38f).coerceAtLeast(0.9f), cap = StrokeCap.Round, join = StrokeJoin.Round),
      )

      cursorX += glyphW + glyphGap
    }
  }

  // Upward-sweeping holographic scanline across the "STAR FORGE" letters
  drawLine(
    brush =
      Brush.horizontalGradient(
        listOf(
          Color.Transparent,
          Color(0x9900F5D4),
          Color(0xCCFFFFFF),
          hueCore.copy(alpha = 0.72f),
          Color.Transparent,
        ),
        startX = bannerRect.left,
        endX = bannerRect.right,
      ),
    start = Offset(bannerRect.left + padX * 0.35f, scanSweepY),
    end = Offset(bannerRect.right - padX * 0.35f, scanSweepY),
    strokeWidth = (glyphH * 0.055f).coerceAtLeast(1.0f),
    cap = StrokeCap.Round,
  )

  return bannerRect
}

