package com.example.ui

import android.graphics.Color as AndroidColor
import android.graphics.Paint as NativePaint
import android.graphics.RuntimeShader
import android.os.Build
import android.view.KeyEvent
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.ArmadaMemberSlot
import com.example.data.LivePilotState
import com.example.data.RiftCommunityRoles
import com.example.data.SyndicateChatMessage
import com.example.data.SyndicateWarRecord
import com.example.engine.CustomGameConfig
import com.example.engine.RankedGameMode
import com.example.engine.RiftCalibrationState
import com.example.engine.RiftGameEngine
import com.example.engine.RiftSpriteBaker
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

private const val RING_SPACE_SHADER_SRC =
  """
uniform float2 uResolution;
uniform float uTime;
uniform float uHubFactor;

float hash21(float2 p) {
  p = fract(p * float2(127.1, 311.7));
  p += dot(p, p + 34.23);
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

float fbm5(float2 p) {
  float v = 0.0;
  float amp = 0.5;
  float2 shift = float2(23.1, 41.7);
  for (int i = 0; i < 5; i++) {
    v += amp * noise2(p);
    p = float2(p.x * 1.64 - p.y * 1.16, p.x * 1.16 + p.y * 1.64) + shift;
    amp *= 0.5;
  }
  return v;
}

half4 main(float2 fragCoord) {
  float2 res = max(uResolution, float2(1.0, 1.0));
  float2 centerPx = float2(res.x * 0.50, res.y * 0.51);
  float minDim = min(res.x, res.y);
  float2 uv = (fragCoord - centerPx) / minDim;
  float dist = length(uv);
  float r2 = dot(uv, uv);

  // Curved 3D interior projection of the Ring-Space containment sphere
  float sphereZ = sqrt(max(0.05, 1.0 - r2 * 0.72));
  float2 sphereUv = uv / (0.58 + 0.42 * sphereZ);

  float t = uTime * 0.20;
  float flowA = fbm5(sphereUv * 2.35 + float2(t * 0.52, -t * 0.34));
  float flowB = fbm5(sphereUv * 3.90 + float2(flowA * 1.45 - t * 0.38, flowA * 1.20 + t * 0.29));
  float ridge = pow(max(0.0, 1.0 - abs(flowB * 2.0 - 1.0)), 3.6);

  // Curved Ring-Space boundary membrane & concentric gravity-wave ripples from Nexus Hub
  float containmentShell = exp(-16.0 * pow(dist - 0.58, 2.0)) + 0.45 * exp(-26.0 * pow(dist - 0.78, 2.0));
  float gravWave = 0.5 + 0.5 * sin(dist * 26.0 - uTime * 1.65 + flowA * 3.2);
  float gravEnv = exp(-3.4 * dist) * gravWave * 0.14;

  // Interfering Ring-Space caustics & volumetric shafts
  float caustic = 0.5 + 0.5 * sin(sphereUv.x * 10.5 + flowA * 5.2 + uTime * 0.85) * cos(sphereUv.y * 8.4 - uTime * 0.65);
  float shaftA = exp(-7.5 * pow(uv.x * 0.78 + uv.y * 0.34 - 0.10 * sin(uTime * 0.35), 2.0));
  float shaftB = exp(-8.5 * pow(uv.x * 0.72 - uv.y * 0.40 + 0.12 * cos(uTime * 0.28), 2.0));

  float3 voidCol = float3(0.008, 0.020, 0.043);
  float3 cyanCol = float3(0.00, 0.96, 0.83);
  float3 cobaltCol = float3(0.22, 0.74, 0.97);
  float3 indigoCol = float3(0.31, 0.27, 0.90);
  float3 amethystCol = float3(0.66, 0.33, 0.97);

  float intensity = mix(0.54, 1.0, clamp(uHubFactor, 0.0, 1.0));
  float3 col = voidCol;
  col += indigoCol * (flowB * 0.30 * intensity);
  col += cobaltCol * ((flowA * 0.20 + shaftA * 0.13 + shaftB * 0.10) * intensity);
  col += cyanCol * ((ridge * 0.34 + containmentShell * 0.26 + gravEnv + caustic * 0.05) * intensity);
  col += amethystCol * ((pow(flowB, 2.3) * 0.28 + containmentShell * 0.16) * intensity);

  // Central Nexus Hub singularity warm-cyan core glow
  float hubGlow = exp(-12.0 * r2) * 0.22 * intensity;
  col += mix(cyanCol, amethystCol, 0.42) * hubGlow;

  float edgeDepth = clamp(1.10 - r2 * 0.56, 0.16, 1.0);
  col *= edgeDepth;
  return half4(clamp(col, 0.0, 1.0), 1.0);
}
"""

private const val RIFT_LENS_AURA_SHADER_SRC =
  """
uniform float2 uResolution;
uniform float uTime;
uniform float3 uAccent;

half4 main(float2 fragCoord) {
  float2 res = max(uResolution, float2(1.0, 1.0));
  float2 p = (fragCoord - 0.5 * res) / (0.5 * min(res.x, res.y));
  float r = length(p);
  if (r >= 1.0) {
    return half4(0.0, 0.0, 0.0, 0.0);
  }
  float ang = atan(p.y, p.x);
  float swirl = 0.5 + 0.5 * sin(ang * 4.0 - r * 9.5 + uTime * 2.8);
  float counter = 0.5 + 0.5 * cos(ang * 3.0 + r * 7.0 - uTime * 2.1);
  float lensRim = exp(-18.0 * pow(r - 0.58, 2.0));
  float falloff = pow(max(0.0, 1.0 - r), 1.85);
  float alpha = clamp((falloff * (0.28 + 0.34 * swirl * counter) + lensRim * 0.36), 0.0, 0.82);
  float3 col = mix(uAccent, float3(1.0, 1.0, 1.0), lensRim * 0.42 + swirl * 0.16);
  return half4(col * alpha, alpha);
}
"""

private val NEXUS_STARTER_CHIPS =
  listOf(
    "How many pixels are Suppressor's lasers?",
    "Who holds the highest bounty ever?",
    "Theorycraft a 100-pt Rift Frame sniper build",
    "Analyze my Peak Bounty Run & Executioner",
    "Compare Eclipse Stasis vs Disruptor EMP",
    "Explain Flux Core, Stasis Breach & Rift Control",
  )

enum class NexusHubLayoutMode {
  SPLIT_COMMAND,
  CHANNELS_ONLY,
  DECK_ONLY,
}

enum class NexusCoreNode {
  ORBITAL_HUB,
  SUBSPACE_COMMS,
  CONVERGENCE_GATE,
  HANGAR_DECK,
  RIFT_CONDUIT,
  SYNDICATE_CITADEL,
  ARCHIVE_CODEX,
  HELM_SYSTEMS,
  STAR_FORGE,
}

enum class SyndicateCitadelSubDeck {
  SYNDICATES_WARS,
  STRIKE_WING,
}

enum class ArchiveCodexSubDeck {
  PILOT_DOSSIER,
  TACTICAL_CODEX,
}

private enum class ConvergenceGateTab {
  OFFICIAL_RANKED,
  CUSTOM_RIFT_GATE,
}

private data class SpatialRiftSpec(
  val node: NexusCoreNode,
  val title: String,
  val subtitle: String,
  val xFrac: Float,
  val yFrac: Float,
  val accent: Color,
  val outerColorInt: Int,
  val midColorInt: Int,
  val coreColorInt: Int,
  val phaseOffset: Float,
  val testTag: String,
)

private val SPATIAL_RIFT_SPECS =
  listOf(
    SpatialRiftSpec(
      node = NexusCoreNode.CONVERGENCE_GATE,
      title = "CONVERGENCE GATE",
      subtitle = "6 Ranked + Custom Rifts",
      xFrac = 0.50f,
      yFrac = 0.19f,
      accent = Color(0xFF00F5D4),
      outerColorInt = AndroidColor.rgb(0, 245, 212),
      midColorInt = AndroidColor.rgb(14, 165, 233),
      coreColorInt = AndroidColor.rgb(224, 255, 250),
      phaseOffset = 0.0f,
      testTag = "nexus_node_convergence_gate",
    ),
    SpatialRiftSpec(
      node = NexusCoreNode.HANGAR_DECK,
      title = "HANGAR DECK",
      subtitle = "10 Hulls • Shipwright",
      xFrac = 0.17f,
      yFrac = 0.32f,
      accent = Color(0xFF38BDF8),
      outerColorInt = AndroidColor.rgb(56, 189, 248),
      midColorInt = AndroidColor.rgb(59, 130, 246),
      coreColorInt = AndroidColor.rgb(224, 242, 254),
      phaseOffset = 0.9f,
      testTag = "nexus_node_hangar_deck",
    ),
    SpatialRiftSpec(
      node = NexusCoreNode.STAR_FORGE,
      title = "STAR FORGE",
      subtitle = "1:1 Playfield Studio",
      xFrac = 0.15f,
      yFrac = 0.66f,
      accent = Color(0xFFC084FC),
      outerColorInt = AndroidColor.rgb(192, 132, 252),
      midColorInt = AndroidColor.rgb(124, 58, 237),
      coreColorInt = AndroidColor.rgb(245, 208, 254),
      phaseOffset = 1.8f,
      testTag = "nexus_node_star_forge",
    ),
    SpatialRiftSpec(
      node = NexusCoreNode.RIFT_CONDUIT,
      title = "RIFT CONDUIT",
      subtitle = "Resonance & Directives",
      xFrac = 0.33f,
      yFrac = 0.83f,
      accent = Color(0xFFE879F9),
      outerColorInt = AndroidColor.rgb(232, 121, 249),
      midColorInt = AndroidColor.rgb(168, 85, 247),
      coreColorInt = AndroidColor.rgb(253, 244, 255),
      phaseOffset = 2.7f,
      testTag = "nexus_node_rift_conduit",
    ),
    SpatialRiftSpec(
      node = NexusCoreNode.SYNDICATE_CITADEL,
      title = "SYNDICATE CITADEL",
      subtitle = "Clans & Strike Wings",
      xFrac = 0.83f,
      yFrac = 0.32f,
      accent = Color(0xFFFACC15),
      outerColorInt = AndroidColor.rgb(250, 204, 21),
      midColorInt = AndroidColor.rgb(245, 158, 11),
      coreColorInt = AndroidColor.rgb(254, 249, 195),
      phaseOffset = 3.6f,
      testTag = "nexus_node_syndicate_citadel",
    ),
    SpatialRiftSpec(
      node = NexusCoreNode.ARCHIVE_CODEX,
      title = "ARCHIVE & CODEX",
      subtitle = "Dossier & Field Manual",
      xFrac = 0.85f,
      yFrac = 0.66f,
      accent = Color(0xFF34D399),
      outerColorInt = AndroidColor.rgb(52, 211, 153),
      midColorInt = AndroidColor.rgb(16, 185, 129),
      coreColorInt = AndroidColor.rgb(209, 250, 229),
      phaseOffset = 4.5f,
      testTag = "nexus_node_archive_codex",
    ),
    SpatialRiftSpec(
      node = NexusCoreNode.HELM_SYSTEMS,
      title = "HELM SYSTEMS",
      subtitle = "Audio, FOV & Controls",
      xFrac = 0.67f,
      yFrac = 0.83f,
      accent = Color(0xFF94A3B8),
      outerColorInt = AndroidColor.rgb(148, 163, 184),
      midColorInt = AndroidColor.rgb(56, 189, 248),
      coreColorInt = AndroidColor.rgb(241, 245, 249),
      phaseOffset = 5.4f,
      testTag = "nexus_node_helm_systems",
    ),
  )

internal fun generateSixCharRoomCode(seedSalt: Long = System.currentTimeMillis()): String {
  val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
  var v = abs(seedSalt xor 0x5DEECE66DL)
  val sb = StringBuilder(6)
  repeat(6) { idx ->
    val charIdx = ((v + idx * 17L) % alphabet.length).toInt()
    sb.append(alphabet[charIdx])
    v = (v * 1103515245L + 12345L) and 0x7FFFFFFFL
  }
  return sb.toString()
}

internal fun extractCustomRoomCode(instanceId: String): String {
  val parts = instanceId.split("_")
  if ((parts.firstOrNull() == "priv" || parts.firstOrNull() == "pub") && parts.size >= 2) {
    val candidate = parts[1].uppercase(Locale.US).replace(Regex("[^A-Z0-9]"), "").take(6)
    if (candidate.length in 4..6) return candidate
  }
  return generateSixCharRoomCode(instanceId.hashCode().toLong())
}

internal fun isPrivateCustomGame(config: CustomGameConfig): Boolean =
  config.instanceId.startsWith("priv_", ignoreCase = true)

@Composable
fun SwirlingRiftVortexGlyph(modifier: Modifier = Modifier, isStreaming: Boolean = false) {
  val transition = rememberInfiniteTransition(label = "nexus_rift_vortex")
  val outerDeg by
    transition.animateFloat(
      initialValue = 0f,
      targetValue = 360f,
      animationSpec =
        infiniteRepeatable(
          tween(if (isStreaming) 1250 else 3200, easing = LinearEasing),
          RepeatMode.Restart,
        ),
      label = "vortex_outer_deg",
    )
  val corePulse by
    transition.animateFloat(
      initialValue = 0.76f,
      targetValue = 1.0f,
      animationSpec =
        infiniteRepeatable(
          tween(if (isStreaming) 420 else 980, easing = LinearEasing),
          RepeatMode.Reverse,
        ),
      label = "vortex_core_pulse",
    )

  Canvas(modifier = modifier) {
    val r = minOf(size.width, size.height) * 0.5f
    val c = Offset(size.width * 0.5f, size.height * 0.5f)
    if (r <= 1f) return@Canvas
    val phaseRad = outerDeg * (PI.toFloat() / 180f)

    drawCircle(
      brush =
        Brush.radialGradient(
          colors =
            listOf(
              Color(0xFF02040A),
              Color(0xFF140B2E),
              Color(0xAA7C3AED),
              Color(0x6600F5D4),
              Color.Transparent,
            ),
          center = c,
          radius = r * 1.05f,
        ),
      radius = r,
      center = c,
    )

    for (t in 0 until 24) {
      val tickRad = (t * 15f - outerDeg * 0.45f) * (PI.toFloat() / 180f)
      val rIn = r * (if (t % 3 == 0) 0.80f else 0.87f)
      val rOut = r * 0.96f
      drawLine(
        color = if (t % 6 == 0) Color(0xFFFACC15) else Color(0x9938BDF8),
        start = Offset(c.x + cos(tickRad) * rIn, c.y + sin(tickRad) * rIn),
        end = Offset(c.x + cos(tickRad) * rOut, c.y + sin(tickRad) * rOut),
        strokeWidth = (r * 0.055f).coerceAtLeast(1.0f),
      )
    }

    val outerInset = r * 0.14f
    val outerSize = Size((r - outerInset) * 2f, (r - outerInset) * 2f)
    val outerTopLeft = Offset(c.x - (r - outerInset), c.y - (r - outerInset))
    for (arm in 0 until 3) {
      drawArc(
        color = if (arm % 2 == 0) Color(0xFF00F5D4) else Color(0xFFB388FF),
        startAngle = outerDeg + arm * 120f,
        sweepAngle = 68f,
        useCenter = false,
        topLeft = outerTopLeft,
        size = outerSize,
        style = Stroke(width = (r * 0.11f).coerceAtLeast(1.6f), cap = StrokeCap.Round),
      )
    }

    for (arm in 0 until 4) {
      val armBase = phaseRad * 1.35f + arm * (PI.toFloat() * 0.5f)
      val path = Path()
      for (s in 0..14) {
        val frac = s / 14f
        val sr = r * (0.16f + 0.68f * frac)
        val sa = armBase + frac * 2.4f
        val px = c.x + cos(sa) * sr
        val py = c.y + sin(sa) * sr
        if (s == 0) path.moveTo(px, py) else path.lineTo(px, py)
      }
      val armCol =
        when (arm % 3) {
          0 -> Color(0xFF00F5D4)
          1 -> Color(0xFFA855F7)
          else -> Color(0xFF38BDF8)
        }
      drawPath(
        path,
        armCol.copy(alpha = 0.88f * corePulse),
        style = Stroke(width = (r * 0.085f).coerceAtLeast(1.3f), cap = StrokeCap.Round),
      )
    }

    val ovalW = r * 1.56f
    val ovalH = r * 0.56f
    drawArc(
      color = Color(0xCC00F5D4),
      startAngle = -outerDeg * 1.5f,
      sweepAngle = 250f,
      useCenter = false,
      topLeft = Offset(c.x - ovalW * 0.5f, c.y - ovalH * 0.5f),
      size = Size(ovalW, ovalH),
      style = Stroke(width = (r * 0.075f).coerceAtLeast(1.2f), cap = StrokeCap.Round),
    )
    drawCircle(Color(0xFF030712), radius = r * 0.28f, center = c)
    drawCircle(
      Color(0xFF00F5D4).copy(alpha = corePulse),
      radius = r * 0.29f,
      center = c,
      style = Stroke(width = (r * 0.09f).coerceAtLeast(1.4f)),
    )
    drawCircle(Color.White.copy(alpha = corePulse), radius = r * 0.11f * corePulse, center = c)
  }
}

@Composable
fun SwirlingRiftHudOrb(
  modifier: Modifier = Modifier,
  isStreaming: Boolean = false,
  onClick: () -> Unit,
) {
  val desc = stringResource(R.string.nexus_orb_content_desc)
  val unlocked = RiftCalibrationState.isUiUnlockedForDragging
  Box(
    modifier =
      modifier
        .size(38.dp)
        .clip(CircleShape)
        .semantics { contentDescription = desc }
        .clickable(enabled = !unlocked) { onClick() }
        .testTag("hud_btn_nexus_orb"),
    contentAlignment = Alignment.Center,
  ) {
    SwirlingRiftVortexGlyph(modifier = Modifier.size(34.dp), isStreaming = isStreaming)
  }
}

@Composable
fun SwirlingRiftHeaderPill(onClick: () -> Unit, tag: String, isStreaming: Boolean = false) {
  val desc = stringResource(R.string.nexus_orb_content_desc)
  Row(
    modifier =
      Modifier.minimumInteractiveComponentSize()
        .height(34.dp)
        .clip(RoundedCornerShape(8.dp))
        .background(Brush.horizontalGradient(listOf(Color(0xFF09192F), Color(0xFF1E1038))))
        .border(
          1.dp,
          Brush.horizontalGradient(listOf(Color(0xFF00F5D4), Color(0xFFB388FF))),
          RoundedCornerShape(8.dp),
        )
        .semantics { contentDescription = desc }
        .clickable { onClick() }
        .padding(horizontal = 9.dp)
        .testTag(tag),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(6.dp),
  ) {
    SwirlingRiftVortexGlyph(modifier = Modifier.size(18.dp), isStreaming = isStreaming)
    Text(
      "NEXUS HUB",
      color = Color(0xFF00F5D4),
      fontSize = 9.5.sp,
      fontWeight = FontWeight.Black,
      fontFamily = FontFamily.Monospace,
      letterSpacing = 0.5.sp,
    )
  }
}

@Composable
fun NexusMasterModuleNavigationRibbon(
  activeModuleKey: String,
  onSelectNexusLobby: () -> Unit,
  onOpenHangar: () -> Unit,
  onOpenForge: () -> Unit = {},
  onOpenProgression: () -> Unit = {},
  onOpenArmadaModal: () -> Unit,
  onOpenSyndicateDirectory: () -> Unit,
  onOpenDossier: () -> Unit,
  onOpenCodex: () -> Unit,
  onOpenSettings: () -> Unit,
  onOpenCalibration: () -> Unit = onOpenForge,
  showReturnToArenaButton: Boolean = false,
  onReturnToArena: () -> Unit = {},
) {
  Row(
    modifier =
      Modifier.fillMaxWidth()
        .clip(RoundedCornerShape(10.dp))
        .background(Color(0xF2060D1A))
        .border(1.2.dp, Color(0x9900F5D4), RoundedCornerShape(10.dp))
        .padding(horizontal = 8.dp, vertical = 4.dp)
        .horizontalScroll(rememberScrollState())
        .testTag("nexus_master_module_bar"),
    horizontalArrangement = Arrangement.spacedBy(6.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    NexusRibbonPill("NEXUS HUB", activeModuleKey == "NEXUS", Color(0xFF00F5D4), "nexus_module_pill_lobby", onSelectNexusLobby)
    NexusRibbonPill("HANGAR DECK", activeModuleKey == "HANGAR", Color(0xFF38BDF8), "nexus_module_pill_hangar", onOpenHangar)
    NexusRibbonPill("STAR FORGE", activeModuleKey == "FORGE", Color(0xFFC084FC), "nexus_module_pill_forge", onOpenForge)
    NexusRibbonPill("RIFT CONDUIT", activeModuleKey == "PROGRESSION", Color(0xFFE879F9), "nexus_module_pill_progression", onOpenProgression)
    NexusRibbonPill("STRIKE WING", activeModuleKey == "ARMADA", Color(0xFF60A5FA), "nexus_module_pill_armada", onOpenArmadaModal)
    NexusRibbonPill("SYNDICATES", activeModuleKey == "SYNDICATES", Color(0xFFF59E0B), "nexus_module_pill_syndicates", onOpenSyndicateDirectory)
    NexusRibbonPill("PILOT DOSSIER", activeModuleKey == "DOSSIER", Color(0xFFA78BFA), "nexus_module_pill_dossier", onOpenDossier)
    NexusRibbonPill("TACTICAL CODEX", activeModuleKey == "CODEX", Color(0xFF34D399), "nexus_module_pill_codex", onOpenCodex)
    NexusRibbonPill("HELM SYSTEMS", activeModuleKey == "SETTINGS", Color(0xFF94A3B8), "nexus_module_pill_settings", onOpenSettings)
    if (showReturnToArenaButton) {
      NexusRibbonPill("ENTER RIFT", false, Color(0xFF00F5D4), "nexus_module_pill_return", onReturnToArena)
    }
  }
}

@Composable
private fun NexusRibbonPill(
  label: String,
  selected: Boolean,
  accentColor: Color,
  testTag: String,
  onClick: () -> Unit,
) {
  Box(
    modifier =
      Modifier.clip(RoundedCornerShape(8.dp))
        .background(if (selected) accentColor.copy(alpha = 0.24f) else Color(0xCC0B1528))
        .border(
          if (selected) 1.3.dp else 1.dp,
          if (selected) accentColor else accentColor.copy(alpha = 0.5f),
          RoundedCornerShape(8.dp),
        )
        .clickable { onClick() }
        .padding(horizontal = 9.dp, vertical = 4.dp)
        .testTag(testTag),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      label,
      color = if (selected) Color.White else accentColor,
      fontSize = 8.8.sp,
      fontWeight = FontWeight.Black,
      fontFamily = FontFamily.Monospace,
      maxLines = 1,
    )
  }
}

@Composable
fun NexusAiConsoleModal(
  messages: List<NexusChatMessage>,
  streamState: NexusStreamState,
  lastFailedQuestion: String?,
  activeShipSummary: String,
  pilotLevel: Int,
  pointBudget: Int,
  highestBountySummary: String,
  onSendQuestion: (String) -> Unit,
  onRetryLastQuestion: () -> Unit,
  onClearHistory: () -> Unit,
  onCloseModal: () -> Unit,
  callsign: String = "COMMANDER",
  syndicateTag: String = "GUEST",
  syndicateName: String = "UNAFFILIATED",
  activeShipId: Int = 1,
  bestShipId: Int = 1,
  riftShards: Long = 0L,
  unreadCommsCount: Int = 0,
  currentUserId: String = "",
  bootToNexusHub: Boolean = true,
  onToggleBootToNexusHub: (Boolean) -> Unit = {},
  totalOnlinePilots: Int = 0,
  syndicateOnlineCount: Int = 0,
  presetArmadasReadyCount: Int = 0,
  activeArmadaName: String = "",
  armadaSlots: List<ArmadaMemberSlot> = emptyList(),
  onlineHumanPilots: List<LivePilotState> = emptyList(),
  onResolvePilotBestShipId: (uid: String, fallbackShipId: Int) -> Int = { _, fb -> fb },
  activeRankedMode: RankedGameMode = RankedGameMode.SECTOR_CONVERGENCE,
  activeTeamSizeBracket: Int = 3,
  humanPilotCountByMode: (RankedGameMode) -> Int = { 0 },
  canQueueSyndicateWars: Pair<Boolean, String> = Pair(false, "Requires enlisted Syndicate"),
  syndicateWarRecord: SyndicateWarRecord = SyndicateWarRecord(tag = "GUEST"),
  onDeployToRankedMode: (RankedGameMode, Int) -> Unit = { _, _ -> },
  onLeaveMatch: () -> Unit = {},
  onOpenArmadaModal: () -> Unit = {},
  onQuickFormSyndicateArmada: () -> Unit = {},
  onOpenSyndicatesModal: () -> Unit = {},
  onOpenSyndicateDirectory: () -> Unit = onOpenSyndicatesModal,
  onOpenHangar: () -> Unit = {},
  onOpenDossier: () -> Unit = {},
  onOpenCodex: () -> Unit = {},
  onOpenSettings: () -> Unit = {},
  onOpenCalibration: () -> Unit = {},
  onOpenForge: () -> Unit = onOpenCalibration,
  onOpenHangarForShip: (Int) -> Unit = {},
  canAccessSyndicateServer: Boolean = false,
  syndicateCustomChannels: List<String> = listOf("#general"),
  syndicateInvitedGuests: Set<String> = emptySet(),
  onAddSyndicateChannel: (String) -> Unit = {},
  onDeleteSyndicateChannel: (String) -> Unit = {},
  onInviteSyndicateGuest: (String) -> Unit = {},
  onRevokeSyndicateGuest: (String) -> Unit = {},
  lobbyMessages: List<SyndicateChatMessage> = emptyList(),
  onSendChannelMessage: (channelKey: String, messageText: String) -> Unit = { _, _ -> },
  onSubmitBugReport: (category: String, description: String) -> Unit = { _, _ -> },
  onShareActiveRiftFrameBuild: () -> Unit = {},
  onLoadSharedRiftFrameBuild: (String) -> Unit = {},
  onPostStrikeWingInvite: () -> Unit = {},
  onPostPrivateRoomInvite: (roomCode: String, modeTitle: String) -> Unit = { _, _ -> },
  onJoinSharedStrikeWing: (SyndicateChatMessage) -> Unit = {},
  onAddOnlinePilotFriend: (String) -> Unit = {},
  onInviteOnlinePilotToWing: (LivePilotState) -> Unit = {},
  engine: RiftGameEngine? = null,
  initialLayoutMode: NexusHubLayoutMode = NexusHubLayoutMode.DECK_ONLY,
  activeModuleKey: String = "NEXUS",
  activeNode: NexusCoreNode = NexusCoreNode.ORBITAL_HUB,
  onSelectNode: (NexusCoreNode) -> Unit = {},
  syndicateSubDeck: SyndicateCitadelSubDeck = SyndicateCitadelSubDeck.SYNDICATES_WARS,
  onSelectSyndicateSubDeck: (SyndicateCitadelSubDeck) -> Unit = {},
  archiveSubDeck: ArchiveCodexSubDeck = ArchiveCodexSubDeck.PILOT_DOSSIER,
  onSelectArchiveSubDeck: (ArchiveCodexSubDeck) -> Unit = {},
  onSelectNexusLobby: () -> Unit = { onSelectNode(NexusCoreNode.ORBITAL_HUB) },
  onOpenProgression: () -> Unit = {},
  isCalibrationLivePeeking: Boolean = false,
  hideTopHeaderForFullScreenStage: Boolean = false,
  liveResonance: Int = -1,
  onStageRankedModeViaHangar: ((RankedGameMode, Int) -> Unit)? = null,
  onStageCustomGameViaHangar: ((CustomGameConfig) -> Unit)? = null,
  onStageExistingCustomInstanceViaHangar: ((String) -> Unit)? = null,
  embeddedModuleContent: (@Composable () -> Unit)? = null,
) {
  val context = LocalContext.current
  val keyboardController = LocalSoftwareKeyboardController.current
  val listState = rememberLazyListState()
  val isStreaming = streamState is NexusStreamState.Loading || streamState is NexusStreamState.Streaming
  val cleanSyndicateTag =
    if (syndicateTag.isBlank() || syndicateTag in setOf("FREE", "SOLO", "UNAFFILIATED")) "GUEST"
    else syndicateTag.uppercase(Locale.US)
  val isLocalArchitect = remember(currentUserId, callsign) {
    RiftCommunityRoles.isDeveloperArchitect(currentUserId, callsign)
  }
  val resolvedLiveResonance =
    when {
      liveResonance >= 0 -> liveResonance
      engine != null -> engine.liveResonance
      else -> ((pilotLevel - 1).coerceAtLeast(0) * 18)
    }

  var selectedChannelKey by remember { mutableStateOf("#nexus-ai") }
  var showOnlineRosterPane by remember { mutableStateOf(true) }
  var inputQuery by remember { mutableStateOf("") }
  var channelChatInput by remember { mutableStateOf("") }

  var gateTab by remember { mutableStateOf(ConvergenceGateTab.OFFICIAL_RANKED) }
  var skirmishBracket by remember { mutableIntStateOf(activeTeamSizeBracket.coerceIn(2, 5)) }
  var riftControlBracket by remember {
    mutableIntStateOf(if (activeTeamSizeBracket >= 10) 10 else if (activeTeamSizeBracket >= 5) 5 else 3)
  }
  var syndicateWarBracket by remember {
    mutableIntStateOf(if (activeTeamSizeBracket >= 10) 10 else if (activeTeamSizeBracket >= 5) 5 else 3)
  }

  var customIsPrivateRoom by remember { mutableStateOf(false) }
  var generatedRoomCode by remember { mutableStateOf(generateSixCharRoomCode()) }
  var joinRoomCodeDraft by remember { mutableStateOf("") }
  var roomCodeBanner by remember { mutableStateOf<String?>(null) }
  val unlockedPrivateCodes = remember { mutableStateListOf<String>() }

  var customBaseMode by remember { mutableStateOf(RankedGameMode.SECTOR_CONVERGENCE) }
  var customResetMinutes by remember { mutableIntStateOf(10) }
  var customTeamBracket by remember { mutableIntStateOf(5) }
  var customAllowArmadasInFfa by remember { mutableStateOf(true) }
  var customBotsEnabled by remember { mutableStateOf(true) }
  var customBulletSizeMult by remember { mutableFloatStateOf(1.0f) }
  var customSpeedMult by remember { mutableFloatStateOf(1.0f) }
  var customRechargeMult by remember { mutableFloatStateOf(1.0f) }
  var customDamageMult by remember { mutableFloatStateOf(1.0f) }
  var customCrateSpawnMult by remember { mutableFloatStateOf(1.0f) }
  var customGamesRefreshTick by remember { mutableIntStateOf(0) }

  var showChannelManager by remember { mutableStateOf(false) }
  var newChannelDraft by remember { mutableStateOf("") }
  var showGuestManager by remember { mutableStateOf(false) }
  var guestCallsignDraft by remember { mutableStateOf("") }

  var bugCategory by remember { mutableStateOf("PHYSICS / COLLISION") }
  var bugDescription by remember { mutableStateOf("") }
  var bugStatusBanner by remember { mutableStateOf<String?>(null) }

  var focusedOrbitalIndex by remember { mutableIntStateOf(0) }
  val localEchoMessages = remember { mutableStateMapOf<String, MutableList<SyndicateChatMessage>>() }

  val lastMsgTextLen = messages.lastOrNull()?.text?.length ?: 0
  LaunchedEffect(messages.size, lastMsgTextLen, streamState, selectedChannelKey) {
    if (selectedChannelKey == "#nexus-ai") {
      val totalItems = listState.layoutInfo.totalItemsCount
      if (totalItems > 0) listState.animateScrollToItem(totalItems - 1)
    }
  }

  fun submitNexusAiQuery() {
    val trimmed = inputQuery.trim()
    if (trimmed.isNotEmpty() && !isStreaming) {
      inputQuery = ""
      keyboardController?.hide()
      onSendQuestion(trimmed)
    }
  }

  fun postToCloudChannel(channelKey: String, rawText: String) {
    val clean = rawText.trim()
    if (clean.isEmpty()) return
    val normalizedKey = channelKey.trim().lowercase(Locale.US)
    val echo =
      SyndicateChatMessage(
        id = "local_${System.currentTimeMillis()}",
        senderUid = currentUserId,
        senderCallsign = callsign,
        senderSyndicateTag = cleanSyndicateTag,
        timestampLabel = channelKey.uppercase(Locale.US).take(24),
        messageText = clean,
        createdAtMs = System.currentTimeMillis(),
      )
    localEchoMessages.getOrPut(normalizedKey) { mutableStateListOf() }.add(echo)
    onSendChannelMessage(channelKey, clean)
  }

  fun stageRankedSelectionIntoHangar(mode: RankedGameMode, bracket: Int) {
    if (onStageRankedModeViaHangar != null) {
      onStageRankedModeViaHangar(mode, bracket)
      return
    }
    engine?.let { eng ->
      eng.currentCustomGame = null
      eng.activeRankedMode = mode
      eng.mode = mode.id
      eng.activeTeamSizeBracket = bracket
      eng.isShipLockedForMatch = false
      eng.hasJoinedMatch = false
      eng.isInLobbyStaging = true
    }
    onSelectNode(NexusCoreNode.HANGAR_DECK)
  }

  fun stageExistingCustomIntoHangar(instanceId: String) {
    if (onStageExistingCustomInstanceViaHangar != null) {
      onStageExistingCustomInstanceViaHangar(instanceId)
      return
    }
    engine?.let { eng ->
      eng.isShipLockedForMatch = false
      eng.joinCustomGame(instanceId)
      eng.isShipLockedForMatch = false
      RiftCalibrationState.applyCalibrationChangesLive(eng, context)
    }
    customGamesRefreshTick++
    onSelectNode(NexusCoreNode.HANGAR_DECK)
  }

  fun stageNewCustomConfigIntoHangar(cfg: CustomGameConfig) {
    if (onStageCustomGameViaHangar != null) {
      onStageCustomGameViaHangar(cfg)
      return
    }
    engine?.let { eng ->
      eng.isShipLockedForMatch = false
      eng.createAndJoinCustomGame(cfg)
      eng.isShipLockedForMatch = false
      RiftCalibrationState.botsEnabled = cfg.botsEnabled
      RiftCalibrationState.applyCalibrationChangesLive(eng, context)
    }
    customGamesRefreshTick++
    onSelectNode(NexusCoreNode.HANGAR_DECK)
  }

  fun joinCustomByRoomCode(rawCode: String) {
    val cleanCode = rawCode.trim().uppercase(Locale.US).replace(Regex("[^A-Z0-9]"), "").take(6)
    if (cleanCode.length < 4) {
      roomCodeBanner = "ENTER A VALID 6-CHARACTER RIFT ROOM CODE."
      return
    }
    if (cleanCode !in unlockedPrivateCodes) unlockedPrivateCodes.add(cleanCode)
    val matching =
      engine?.activeCustomGames?.firstOrNull {
        extractCustomRoomCode(it.instanceId).equals(cleanCode, ignoreCase = true)
      }
    if (matching != null) {
      roomCodeBanner = "STAGING ROOM [$cleanCode] // ${matching.title} -> CHOOSE HULL"
      stageExistingCustomIntoHangar(matching.instanceId)
    } else {
      val created =
        CustomGameConfig(
          instanceId = "priv_${cleanCode}_${System.currentTimeMillis()}",
          title = "PRIVATE RIFT [$cleanCode]",
          hostCallsign = callsign,
          baseMode = customBaseMode,
          teamSizeBracket = customTeamBracket,
          matchDurationMinutes = customResetMinutes,
          allowArmadasInFfa = customAllowArmadasInFfa,
          botsEnabled = customBotsEnabled,
          bulletSizeMult = customBulletSizeMult,
          damageMult = customDamageMult,
          speedMult = customSpeedMult,
          healthMult = if (customRechargeMult > 1.0f) 1.25f else 1.0f,
          rechargeMult = customRechargeMult,
          crateSpawnMult = customCrateSpawnMult,
        )
      roomCodeBanner = "STAGING PRIVATE RIFT [$cleanCode] -> CHOOSE HULL"
      stageNewCustomConfigIntoHangar(created)
    }
  }

  val isCalibrationPeekActive =
    (activeNode == NexusCoreNode.STAR_FORGE || activeModuleKey == "FORGE" || activeModuleKey == "CALIBRATE") &&
      isCalibrationLivePeeking
  val isFullScreenForgeStage =
    hideTopHeaderForFullScreenStage &&
      (activeNode == NexusCoreNode.STAR_FORGE || activeModuleKey == "FORGE")

  // Full-bleed #02050B container BEFORE safeDrawing so zero Arena entities bleed around screen edges
  Box(
    modifier =
      Modifier.fillMaxSize()
        .background(if (isCalibrationPeekActive) Color(0x1802050B) else Color(0xFF02050B))
        .onPreviewKeyEvent { keyEvent ->
          if (activeNode != NexusCoreNode.ORBITAL_HUB) return@onPreviewKeyEvent false
          val native = keyEvent.nativeKeyEvent
          if (native.action == KeyEvent.ACTION_DOWN && native.repeatCount == 0) {
            val totalTargets = SPATIAL_RIFT_SPECS.size + 1 // index 0 = Central Nexus Hub Station, 1..7 = 3D Rifts
            when (native.keyCode) {
              KeyEvent.KEYCODE_DPAD_RIGHT,
              KeyEvent.KEYCODE_DPAD_DOWN -> {
                focusedOrbitalIndex = (focusedOrbitalIndex + 1) % totalTargets
                true
              }
              KeyEvent.KEYCODE_DPAD_LEFT,
              KeyEvent.KEYCODE_DPAD_UP -> {
                focusedOrbitalIndex =
                  if (focusedOrbitalIndex <= 0) totalTargets - 1 else focusedOrbitalIndex - 1
                true
              }
              KeyEvent.KEYCODE_BUTTON_A,
              KeyEvent.KEYCODE_DPAD_CENTER,
              KeyEvent.KEYCODE_ENTER -> {
                val targetNode =
                  if (focusedOrbitalIndex == 0) NexusCoreNode.SUBSPACE_COMMS
                  else SPATIAL_RIFT_SPECS[(focusedOrbitalIndex - 1).coerceIn(0, SPATIAL_RIFT_SPECS.lastIndex)].node
                onSelectNode(targetNode)
                true
              }
              else -> false
            }
          } else {
            false
          }
        }
        .testTag("nexus_ai_console_modal"),
    contentAlignment = Alignment.Center,
  ) {
    // Full-screen GPU AGSL Ring-Space Containment Sphere rendered edge-to-edge before safeDrawing insets
    if (!isCalibrationPeekActive && !isFullScreenForgeStage) {
      RingSpaceNexusCanvas(
        modifier = Modifier.fillMaxSize(),
        isHubView = activeNode == NexusCoreNode.ORBITAL_HUB,
      )
    }

    Box(
      modifier =
        Modifier.fillMaxSize()
          .windowInsetsPadding(WindowInsets.safeDrawing)
          .imePadding(),
      contentAlignment = Alignment.Center,
    ) {
      when {
        isFullScreenForgeStage && embeddedModuleContent != null -> {
          Box(modifier = Modifier.fillMaxSize().testTag("nexus_embedded_module_container")) {
            embeddedModuleContent()
          }
        }
        activeNode == NexusCoreNode.ORBITAL_HUB -> {
          OrbitalHubSpatialStage(
            callsign = callsign,
            syndicateTag = cleanSyndicateTag,
            liveResonance = resolvedLiveResonance,
            riftShards = riftShards,
            totalOnlinePilots = totalOnlinePilots,
            unreadCommsCount = unreadCommsCount,
            bootToNexusHub = bootToNexusHub,
            isInMatch = engine?.hasJoinedMatch == true,
            isStreaming = isStreaming,
            focusedOrbitalIndex = focusedOrbitalIndex,
            onToggleBootToNexusHub = onToggleBootToNexusHub,
            onLeaveMatch = onLeaveMatch,
            onCloseModal = onCloseModal,
            onSelectNode = onSelectNode,
          )
        }
        else -> {
          Surface(
            modifier =
              Modifier.fillMaxWidth(0.96f)
                .fillMaxHeight(0.95f)
                .border(
                  1.3.dp,
                  Brush.linearGradient(listOf(Color(0xFF00F5D4), Color(0xFFA855F7), Color(0xFF38BDF8))),
                  RoundedCornerShape(14.dp),
                ),
            color = if (isCalibrationPeekActive) Color(0x40060D1A) else Color(0xF2060D1A),
            shape = RoundedCornerShape(14.dp),
          ) {
            Column(
              modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp),
              verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              GlassTerminalTopHeader(
                activeNode = activeNode,
                syndicateSubDeck = syndicateSubDeck,
                onSelectSyndicateSubDeck = onSelectSyndicateSubDeck,
                archiveSubDeck = archiveSubDeck,
                onSelectArchiveSubDeck = onSelectArchiveSubDeck,
                armadaCount = armadaSlots.size.coerceAtLeast(1),
                showClearAi = activeNode == NexusCoreNode.SUBSPACE_COMMS && selectedChannelKey == "#nexus-ai" && messages.isNotEmpty(),
                isInMatch = engine?.hasJoinedMatch == true,
                onBackToHub = { onSelectNode(NexusCoreNode.ORBITAL_HUB) },
                onClearAi = onClearHistory,
                onLeaveMatch = onLeaveMatch,
                onCloseToArena = onCloseModal,
              )

              Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when {
                  activeNode == NexusCoreNode.CONVERGENCE_GATE -> {
                    ConvergenceGateZeroScrollDeck(
                      gateTab = gateTab,
                      onSelectGateTab = { gateTab = it },
                      activeArmadaName = activeArmadaName,
                      armadaSize = armadaSlots.size.coerceAtLeast(1),
                      syndicateTag = cleanSyndicateTag,
                      onOpenStrikeWing = {
                        onSelectSyndicateSubDeck(SyndicateCitadelSubDeck.STRIKE_WING)
                        onSelectNode(NexusCoreNode.SYNDICATE_CITADEL)
                      },
                      activeRankedMode = activeRankedMode,
                      skirmishBracket = skirmishBracket,
                      onSelectSkirmishBracket = { skirmishBracket = it.coerceIn(2, 5) },
                      riftControlBracket = riftControlBracket,
                      onSelectRiftControlBracket = { riftControlBracket = it },
                      syndicateWarBracket = syndicateWarBracket,
                      onSelectSyndicateWarBracket = { syndicateWarBracket = it },
                      humanPilotCountByMode = humanPilotCountByMode,
                      canQueueSyndicateWars = canQueueSyndicateWars,
                      syndicateWarRecord = syndicateWarRecord,
                      onStageRanked = { mode, bracket -> stageRankedSelectionIntoHangar(mode, bracket) },
                      engine = engine,
                      customIsPrivateRoom = customIsPrivateRoom,
                      onTogglePrivateRoom = {
                        customIsPrivateRoom = it
                        generatedRoomCode = generateSixCharRoomCode()
                      },
                      generatedRoomCode = generatedRoomCode,
                      joinRoomCodeDraft = joinRoomCodeDraft,
                      onJoinRoomCodeDraftChange = { raw ->
                        joinRoomCodeDraft = raw.uppercase(Locale.US).replace(Regex("[^A-Z0-9]"), "").take(6)
                      },
                      roomCodeBanner = roomCodeBanner,
                      unlockedPrivateCodes = unlockedPrivateCodes,
                      onJoinByRoomCode = { joinCustomByRoomCode(joinRoomCodeDraft) },
                      onShareRoomToLfg = {
                        onPostPrivateRoomInvite(generatedRoomCode, customBaseMode.title)
                        roomCodeBanner = "SHARED ROOM CODE [$generatedRoomCode] TO #STRIKE-LFG"
                      },
                      customRefreshTick = customGamesRefreshTick,
                      customBaseMode = customBaseMode,
                      onSelectCustomBaseMode = { customBaseMode = it },
                      customResetMinutes = customResetMinutes,
                      onSelectCustomResetMinutes = { customResetMinutes = it },
                      customTeamBracket = customTeamBracket,
                      onSelectCustomTeamBracket = { customTeamBracket = it },
                      customAllowArmadasInFfa = customAllowArmadasInFfa,
                      onToggleAllowArmadasInFfa = { customAllowArmadasInFfa = it },
                      customBotsEnabled = customBotsEnabled,
                      onToggleBotsEnabled = { customBotsEnabled = it },
                      customBulletSizeMult = customBulletSizeMult,
                      onSelectBulletSizeMult = { customBulletSizeMult = it },
                      customSpeedMult = customSpeedMult,
                      onSelectSpeedMult = { customSpeedMult = it },
                      customRechargeMult = customRechargeMult,
                      onSelectRechargeMult = { customRechargeMult = it },
                      customDamageMult = customDamageMult,
                      onSelectDamageMult = { customDamageMult = it },
                      customCrateSpawnMult = customCrateSpawnMult,
                      onSelectCrateSpawnMult = { customCrateSpawnMult = it },
                      onReturnToRankedFfa = {
                        stageRankedSelectionIntoHangar(RankedGameMode.SECTOR_CONVERGENCE, 20)
                        customGamesRefreshTick++
                      },
                      onJoinCustomInstance = { instanceId -> stageExistingCustomIntoHangar(instanceId) },
                      onLaunchCustomGame = {
                        val prefix = if (customIsPrivateRoom) "priv" else "pub"
                        val code = generatedRoomCode
                        if (customIsPrivateRoom && code !in unlockedPrivateCodes) {
                          unlockedPrivateCodes.add(code)
                        }
                        val modeShort = customBaseMode.title.uppercase(Locale.US)
                        val cfg =
                          CustomGameConfig(
                            instanceId = "${prefix}_${code}_${System.currentTimeMillis()}",
                            title = "${callsign.uppercase(Locale.US)} $modeShort [$code]",
                            hostCallsign = callsign,
                            baseMode = customBaseMode,
                            teamSizeBracket = customTeamBracket,
                            matchDurationMinutes = customResetMinutes,
                            allowArmadasInFfa = customAllowArmadasInFfa,
                            botsEnabled = customBotsEnabled,
                            bulletSizeMult = customBulletSizeMult,
                            damageMult = customDamageMult,
                            speedMult = customSpeedMult,
                            healthMult = if (customRechargeMult > 1.0f) 1.25f else 1.0f,
                            rechargeMult = customRechargeMult,
                            crateSpawnMult = customCrateSpawnMult,
                          )
                        stageNewCustomConfigIntoHangar(cfg)
                      },
                    )
                  }
                  activeNode == NexusCoreNode.SUBSPACE_COMMS -> {
                    SubspaceCommsDiscordDeck(
                      selectedChannelKey = selectedChannelKey,
                      onSelectChannel = { selectedChannelKey = it },
                      messages = messages,
                      streamState = streamState,
                      isStreaming = isStreaming,
                      lastFailedQuestion = lastFailedQuestion,
                      listState = listState,
                      inputQuery = inputQuery,
                      onInputQueryChange = { inputQuery = it },
                      onSubmitAiQuery = { submitNexusAiQuery() },
                      onSendStarterQuestion = onSendQuestion,
                      onRetryLastQuestion = onRetryLastQuestion,
                      cleanSyndicateTag = cleanSyndicateTag,
                      syndicateName = syndicateName,
                      isLocalArchitect = isLocalArchitect,
                      activeArmadaName = activeArmadaName,
                      armadaSlots = armadaSlots,
                      canAccessSyndicateServer = canAccessSyndicateServer,
                      syndicateCustomChannels = syndicateCustomChannels,
                      syndicateInvitedGuests = syndicateInvitedGuests,
                      showChannelManager = showChannelManager,
                      onToggleChannelManager = { showChannelManager = !showChannelManager },
                      newChannelDraft = newChannelDraft,
                      onNewChannelDraftChange = { newChannelDraft = it },
                      onAddSyndicateChannel = {
                        if (newChannelDraft.isNotBlank()) {
                          onAddSyndicateChannel(newChannelDraft)
                          newChannelDraft = ""
                        }
                      },
                      onDeleteSyndicateChannel = onDeleteSyndicateChannel,
                      showGuestManager = showGuestManager,
                      onToggleGuestManager = { showGuestManager = !showGuestManager },
                      guestCallsignDraft = guestCallsignDraft,
                      onGuestDraftChange = { guestCallsignDraft = it },
                      onInviteGuest = {
                        if (guestCallsignDraft.isNotBlank()) {
                          onInviteSyndicateGuest(guestCallsignDraft)
                          guestCallsignDraft = ""
                        }
                      },
                      onRevokeGuest = onRevokeSyndicateGuest,
                      onOpenSyndicateCitadel = {
                        onSelectSyndicateSubDeck(SyndicateCitadelSubDeck.SYNDICATES_WARS)
                        onSelectNode(NexusCoreNode.SYNDICATE_CITADEL)
                      },
                      lobbyMessages = lobbyMessages,
                      localEchoMessages = localEchoMessages,
                      channelChatInput = channelChatInput,
                      onChannelChatInputChange = { channelChatInput = it },
                      onSendToChannel = { key, txt ->
                        postToCloudChannel(key, txt)
                        channelChatInput = ""
                        keyboardController?.hide()
                      },
                      bugCategory = bugCategory,
                      onSelectBugCategory = { bugCategory = it },
                      bugDescription = bugDescription,
                      onBugDescriptionChange = { bugDescription = it },
                      bugStatusBanner = bugStatusBanner,
                      onSubmitBug = {
                        val clean = bugDescription.trim()
                        if (clean.isNotEmpty()) {
                          onSubmitBugReport(bugCategory, clean)
                          bugDescription = ""
                          bugStatusBanner = "ANOMALY LOGGED TO CLOUD SECTOR TELEMETRY [$bugCategory]"
                          keyboardController?.hide()
                        }
                      },
                      activeCustomRoomCode = engine?.currentCustomGame?.let { extractCustomRoomCode(it.instanceId) },
                      activeCustomModeTitle = engine?.currentCustomGame?.baseMode?.title,
                      onShareRiftFrameBuild = onShareActiveRiftFrameBuild,
                      onLoadSharedBuild = onLoadSharedRiftFrameBuild,
                      onAskAiAboutBuild = { prompt ->
                        selectedChannelKey = "#nexus-ai"
                        onSendQuestion(prompt)
                      },
                      onPostStrikeWingInvite = onPostStrikeWingInvite,
                      onPostPrivateRoomInvite = onPostPrivateRoomInvite,
                      onJoinSharedStrikeWing = onJoinSharedStrikeWing,
                      onJoinCustomRoomCode = { code -> joinCustomByRoomCode(code) },
                      onlineHumanPilots = onlineHumanPilots,
                      totalOnlinePilots = totalOnlinePilots,
                      showOnlineRosterPane = showOnlineRosterPane,
                      onToggleOnlineRosterPane = { showOnlineRosterPane = !showOnlineRosterPane },
                      onAddOnlinePilotFriend = onAddOnlinePilotFriend,
                      onInviteOnlinePilotToWing = onInviteOnlinePilotToWing,
                    )
                  }
                  embeddedModuleContent != null -> {
                    Box(modifier = Modifier.fillMaxSize().testTag("nexus_embedded_module_container")) {
                      embeddedModuleContent()
                    }
                  }
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
private fun RingSpaceNexusCanvas(
  modifier: Modifier,
  isHubView: Boolean,
) {
  val transition = rememberInfiniteTransition(label = "ring_space_nexus_sphere")
  val shaderClockSec by
    transition.animateFloat(
      initialValue = 0f,
      targetValue = 180f,
      animationSpec = infiniteRepeatable(tween(180000, easing = LinearEasing), RepeatMode.Restart),
      label = "ring_space_clock",
    )
  val outerPhase by
    transition.animateFloat(
      initialValue = 0f,
      targetValue = (2.0 * PI).toFloat(),
      animationSpec = infiniteRepeatable(tween(16000, easing = LinearEasing), RepeatMode.Restart),
      label = "ring_space_outer_phase",
    )

  val agslRingShader = remember {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      runCatching { RuntimeShader(RING_SPACE_SHADER_SRC) to NativePaint(NativePaint.ANTI_ALIAS_FLAG) }.getOrNull()
    } else {
      null
    }
  }

  Canvas(modifier = modifier) {
    val w = size.width
    val h = size.height
    if (w <= 1f || h <= 1f) return@Canvas
    val center = Offset(w * 0.50f, h * 0.51f)

    var drewShader = false
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && agslRingShader != null) {
      val (shader, paint) = agslRingShader
      drewShader =
        runCatching {
            shader.setFloatUniform("uResolution", w, h)
            shader.setFloatUniform("uTime", shaderClockSec)
            shader.setFloatUniform("uHubFactor", if (isHubView) 1.0f else 0.52f)
            paint.shader = shader
            drawContext.canvas.nativeCanvas.drawRect(0f, 0f, w, h, paint)
            true
          }
          .getOrDefault(false)
    }

    if (!drewShader) {
      // Multi-layered procedural Ring-Space containment sphere fallback (API 24-32 & JVM tests)
      drawRect(
        brush =
          Brush.radialGradient(
            colors =
              if (isHubView) {
                listOf(Color(0xFF0E2140), Color(0xFF071226), Color(0xFF02050B))
              } else {
                listOf(Color(0xFF081428), Color(0xFF040914), Color(0xFF02050B))
              },
            center = center,
            radius = maxOf(w, h) * 0.78f,
          )
      )

      val sphereRadius = minOf(w, h) * 0.58f
      drawCircle(
        brush =
          Brush.radialGradient(
            colors =
              listOf(
                Color.Transparent,
                Color(0x1600F5D4),
                Color(0x22A855F7),
                Color.Transparent,
              ),
            center = center,
            radius = sphereRadius * 1.24f,
          ),
        radius = sphereRadius * 1.24f,
        center = center,
      )

      val beams =
        listOf(
          Triple(0.16f, 0.48f, Color(0x1400F5D4)),
          Triple(0.50f, 0.72f, Color(0x1238BDF8)),
          Triple(0.84f, 0.42f, Color(0x146366F1)),
        )
      beams.forEachIndexed { idx, (topFrac, botFrac, col) ->
        val drift = sin(outerPhase + idx * 1.9f) * w * 0.022f
        val topX = w * topFrac + drift
        val botX = w * botFrac - drift * 0.5f
        val p =
          Path().apply {
            moveTo(topX - w * 0.05f, 0f)
            lineTo(topX + w * 0.05f, 0f)
            lineTo(botX + w * 0.13f, h)
            lineTo(botX - w * 0.13f, h)
            close()
          }
        drawPath(
          p,
          Brush.verticalGradient(listOf(col, col.copy(alpha = 0.02f), Color.Transparent), 0f, h),
        )
      }

      for (band in 0 until 2) {
        val path = Path()
        val steps = 40
        for (s in 0..steps) {
          val t = s / steps.toFloat()
          val px = t * w
          val py =
            h * (0.24f + band * 0.50f) +
              sin(t * 5.6f + outerPhase + band * 1.5f) * (h * 0.038f) +
              cos(t * 9.4f - outerPhase * 1.25f) * (h * 0.015f)
          if (s == 0) path.moveTo(px, py) else path.lineTo(px, py)
        }
        drawPath(
          path,
          color = if (band == 0) Color(0x1C00F5D4) else Color(0x1AA855F7),
          style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round),
        )
      }
    }

    // 56 Parallax bioluminescent Ring-Space motes drifting inside the containment sphere (zero tether lines)
    for (i in 0 until 56) {
      val sx = (((i * 67 + 13) % 101) / 101f) * w
      val sy = (((i * 43 + 29) % 97) / 97f) * h
      val dx = sin(outerPhase + i * 0.55f) * 16.dp.toPx()
      val dy = cos(outerPhase * 0.8f + i * 0.75f) * 12.dp.toPx()
      val alpha = (0.18f + 0.32f * (0.5f + 0.5f * sin(outerPhase * 2f + i))) * (if (isHubView) 1f else 0.42f)
      val col =
        when (i % 4) {
          0 -> Color(0xFF00F5D4).copy(alpha = alpha)
          1 -> Color(0xFF38BDF8).copy(alpha = alpha)
          2 -> Color(0xFFA855F7).copy(alpha = alpha)
          else -> Color(0xFFFACC15).copy(alpha = alpha * 0.8f)
        }
      drawCircle(
        col,
        radius = (1.1f + (i % 3) * 0.8f).dp.toPx(),
        center = Offset((sx + dx).coerceIn(0f, w), (sy + dy).coerceIn(0f, h)),
      )
    }
  }
}

@Composable
private fun OrbitalHubSpatialStage(
  callsign: String,
  syndicateTag: String,
  liveResonance: Int,
  riftShards: Long,
  totalOnlinePilots: Int,
  unreadCommsCount: Int,
  bootToNexusHub: Boolean,
  isInMatch: Boolean,
  isStreaming: Boolean,
  focusedOrbitalIndex: Int,
  onToggleBootToNexusHub: (Boolean) -> Unit,
  onLeaveMatch: () -> Unit,
  onCloseModal: () -> Unit,
  onSelectNode: (NexusCoreNode) -> Unit,
) {
  val hubTransition = rememberInfiniteTransition(label = "nexus_ring_space_stage")
  val bobPhase by
    hubTransition.animateFloat(
      initialValue = 0f,
      targetValue = (2.0 * PI).toFloat(),
      animationSpec = infiniteRepeatable(tween(3600, easing = LinearEasing), RepeatMode.Restart),
      label = "rift_bob_phase",
    )
  val riftSpinRad by
    hubTransition.animateFloat(
      initialValue = 0f,
      targetValue = (2.0 * PI).toFloat(),
      animationSpec = infiniteRepeatable(tween(6800, easing = LinearEasing), RepeatMode.Restart),
      label = "rift_spin_rad",
    )
  val stationDeg by
    hubTransition.animateFloat(
      initialValue = 0f,
      targetValue = 360f,
      animationSpec = infiniteRepeatable(tween(15000, easing = LinearEasing), RepeatMode.Restart),
      label = "nexus_station_deg",
    )
  val lensTimeSec by
    hubTransition.animateFloat(
      initialValue = 0f,
      targetValue = 60f,
      animationSpec = infiniteRepeatable(tween(60000, easing = LinearEasing), RepeatMode.Restart),
      label = "rift_lens_clock",
    )

  val riftLensShaderHolder = remember {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      runCatching { RuntimeShader(RIFT_LENS_AURA_SHADER_SRC) to NativePaint(NativePaint.ANTI_ALIAS_FLAG) }.getOrNull()
    } else {
      null
    }
  }

  Column(modifier = Modifier.fillMaxSize()) {
    // Sleek frameless translucent horizon telemetry bar
    Box(
      modifier =
        Modifier.fillMaxWidth()
          .background(
            Brush.verticalGradient(
              colors = listOf(Color(0xD8030814), Color(0x88030814), Color.Transparent),
            )
          )
          .padding(horizontal = 16.dp, vertical = 6.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth().height(34.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.weight(1f),
        ) {
          SwirlingRiftVortexGlyph(modifier = Modifier.size(22.dp), isStreaming = isStreaming)
          Text(
            text = "NEXUS // RING SPACE",
            color = Color(0xFF00F5D4),
            fontSize = 12.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 0.8.sp,
          )
          Text(
            text = "• $totalOnlinePilots PILOTS ONLINE",
            color = Color(0xFF34D399),
            fontSize = 8.8.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = FontFamily.Monospace,
          )
          Text(
            text = "• [$syndicateTag] $callsign • RES $liveResonance • ❖ $riftShards",
            color = Color(0xFFE2E8F0),
            fontSize = 8.8.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = if (bootToNexusHub) "STARTUP: NEXUS" else "STARTUP: ORBIT",
            color = if (bootToNexusHub) Color(0xFF00F5D4) else Color(0xFFFACC15),
            fontSize = 8.4.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = FontFamily.Monospace,
            modifier =
              Modifier.clip(CircleShape)
                .clickable { onToggleBootToNexusHub(!bootToNexusHub) }
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .testTag("nexus_boot_mode_toggle"),
          )
          if (isInMatch) {
            Text(
              text = "LEAVE MATCH",
              color = Color(0xFFFB7185),
              fontSize = 8.4.sp,
              fontWeight = FontWeight.Black,
              fontFamily = FontFamily.Monospace,
              modifier =
                Modifier.clip(CircleShape)
                  .clickable { onLeaveMatch() }
                  .padding(horizontal = 8.dp, vertical = 4.dp)
                  .testTag("nexus_leave_match_button"),
            )
          }
          Text(
            text = if (isInMatch) "RETURN TO RIFT >" else "VIEW ORBIT >",
            color = Color(0xFF00F5D4),
            fontSize = 8.8.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
            modifier =
              Modifier.clip(CircleShape)
                .clickable { onCloseModal() }
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .testTag("nexus_close_button"),
          )
        }
      }
    }

    // Spatial 3D Stage: Central 3D NEXUS HUB Orbital Station + 7 Frameless Swirling 3D Rifts (Zero Boxes / Zero Wires)
    BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth()) {
      val stageW = maxWidth
      val stageH = maxHeight

      // Central 3D NEXUS HUB Orbital Station -> opens SUBSPACE COMMS & NEXUS AI
      val hubNodeW = 208.dp
      val hubNodeH = 152.dp
      val hubX = ((stageW * 0.50f) - (hubNodeW * 0.5f)).coerceIn(0.dp, (stageW - hubNodeW).coerceAtLeast(0.dp))
      val hubY = ((stageH * 0.50f) - (hubNodeH * 0.5f)).coerceIn(0.dp, (stageH - hubNodeH).coerceAtLeast(0.dp))
      NexusHubCentralStationNode(
        stationDeg = stationDeg,
        riftSpinRad = riftSpinRad,
        bobPhase = bobPhase,
        unreadCommsCount = unreadCommsCount,
        totalOnlinePilots = totalOnlinePilots,
        isFocused = focusedOrbitalIndex == 0,
        onClick = { onSelectNode(NexusCoreNode.SUBSPACE_COMMS) },
        modifier =
          Modifier.width(hubNodeW)
            .height(hubNodeH)
            .offset(x = hubX, y = hubY)
            .testTag("nexus_node_subspace_comms"),
      )

      // 7 Surrounding Living 3D Swirling Rifts
      val riftNodeW = 152.dp
      val riftNodeH = 132.dp
      SPATIAL_RIFT_SPECS.forEachIndexed { idx, spec ->
        val bobDp = (sin(bobPhase + spec.phaseOffset) * 4.5f).dp
        val rawX = (stageW * spec.xFrac) - (riftNodeW * 0.5f)
        val rawY = (stageH * spec.yFrac) - (riftNodeH * 0.5f) + bobDp
        val clampedX = rawX.coerceIn(2.dp, (stageW - riftNodeW - 2.dp).coerceAtLeast(2.dp))
        val clampedY = rawY.coerceIn(0.dp, (stageH - riftNodeH).coerceAtLeast(0.dp))

        SpatialRiftNode(
          spec = spec,
          riftSpinRad = riftSpinRad + spec.phaseOffset,
          pulsePhase = bobPhase + spec.phaseOffset,
          lensTimeSec = lensTimeSec + idx * 3.7f,
          riftLensShaderHolder = riftLensShaderHolder,
          isFocused = focusedOrbitalIndex == idx + 1,
          onClick = { onSelectNode(spec.node) },
          modifier =
            Modifier.width(riftNodeW)
              .height(riftNodeH)
              .offset(x = clampedX, y = clampedY)
              .testTag(spec.testTag),
        )
      }
    }
  }
}

@Composable
private fun NexusHubCentralStationNode(
  stationDeg: Float,
  riftSpinRad: Float,
  bobPhase: Float,
  unreadCommsCount: Int,
  totalOnlinePilots: Int,
  isFocused: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val calloutShadow = remember {
    Shadow(color = Color(0xEE02050B), offset = Offset(0f, 2f), blurRadius = 8f)
  }
  Column(
    modifier =
      modifier
        .clip(CircleShape)
        .clickable(onClick = onClick),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
  ) {
    Canvas(modifier = Modifier.width(184.dp).height(112.dp)) {
      val cx = size.width * 0.5f
      val cy = size.height * 0.52f
      val baseR = minOf(size.width, size.height) * 0.42f
      val pulse = 0.86f + 0.14f * sin(bobPhase * 2f)

      // 1. Volumetric multicolored NEXUS Singularity aura
      drawCircle(
        brush =
          Brush.radialGradient(
            colors =
              listOf(
                Color(0x6600F5D4),
                Color(0x44A855F7),
                Color(0x1E38BDF8),
                Color.Transparent,
              ),
            center = Offset(cx, cy),
            radius = baseR * 1.95f,
          ),
        radius = baseR * 1.95f,
        center = Offset(cx, cy),
      )

      // 2. Living 3D Playfield Rift vortex behind the orbital station
      RiftSpriteBaker.draw3dPlayfieldRift(
        canvas = drawContext.canvas.nativeCanvas,
        cx = cx,
        cy = cy,
        r = baseR * 0.86f,
        spinAngle = riftSpinRad * 1.25f,
        outerColor = AndroidColor.rgb(0, 245, 212),
        midColor = AndroidColor.rgb(168, 85, 247),
        coreColor = AndroidColor.rgb(254, 249, 195),
        darkVoidCenter = true,
        inwardSpiral = true,
        fadeAlpha = 0.96f,
        pulsePhaseOverride = bobPhase,
      )

      // 3. Back arcs of the two counter-rotating 3D-tilted habitat rings (foreshortened 0.54f)
      val outerRx = baseR * 1.48f
      val outerRy = outerRx * 0.54f
      val innerRx = baseR * 1.06f
      val innerRy = innerRx * 0.54f

      rotate(degrees = -14f, pivot = Offset(cx, cy)) {
        drawArc(
          color = Color(0xAA1E3A5F),
          startAngle = 180f,
          sweepAngle = 180f,
          useCenter = false,
          topLeft = Offset(cx - outerRx, cy - outerRy),
          size = Size(outerRx * 2f, outerRy * 2f),
          style = Stroke(width = 5.2.dp.toPx(), cap = StrokeCap.Round),
        )
        drawArc(
          color = Color(0xBB38BDF8),
          startAngle = 186f,
          sweepAngle = 168f,
          useCenter = false,
          topLeft = Offset(cx - outerRx, cy - outerRy),
          size = Size(outerRx * 2f, outerRy * 2f),
          style = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round),
        )
      }

      rotate(degrees = 16f, pivot = Offset(cx, cy)) {
        drawArc(
          color = Color(0xAA312E81),
          startAngle = 180f,
          sweepAngle = 180f,
          useCenter = false,
          topLeft = Offset(cx - innerRx, cy - innerRy),
          size = Size(innerRx * 2f, innerRy * 2f),
          style = Stroke(width = 4.0.dp.toPx(), cap = StrokeCap.Round),
        )
      }

      // 4. 6 Radial docking spires & central vertical command spire
      for (s in 0 until 6) {
        val spireRad = (stationDeg + s * 60f) * (PI.toFloat() / 180f)
        val sx0 = cx + cos(spireRad) * (baseR * 0.32f)
        val sy0 = cy + sin(spireRad) * (baseR * 0.32f) * 0.54f
        val sx1 = cx + cos(spireRad) * outerRx
        val sy1 = cy + sin(spireRad) * outerRy
        drawLine(
          color = Color(0xCC64748B),
          start = Offset(sx0, sy0),
          end = Offset(sx1, sy1),
          strokeWidth = 2.0.dp.toPx(),
        )
        drawCircle(
          color = if (s % 2 == 0) Color(0xFF00F5D4) else Color(0xFFFACC15),
          radius = 2.1.dp.toPx(),
          center = Offset(sx1, sy1),
        )
      }

      // Vertical command spire & central singularity core
      drawLine(
        brush = Brush.verticalGradient(listOf(Color(0xFF00F5D4), Color.White, Color(0xFFA855F7))),
        start = Offset(cx, cy - baseR * 0.88f),
        end = Offset(cx, cy + baseR * 0.78f),
        strokeWidth = 2.6.dp.toPx(),
        cap = StrokeCap.Round,
      )
      drawCircle(Color(0xFF030712), radius = baseR * 0.28f, center = Offset(cx, cy))
      drawCircle(
        color = Color(0xFF00F5D4),
        radius = baseR * 0.30f,
        center = Offset(cx, cy),
        style = Stroke(width = 2.0.dp.toPx()),
      )
      drawCircle(Color.White.copy(alpha = pulse), radius = baseR * 0.14f * pulse, center = Offset(cx, cy))

      // 5. Front arcs of the counter-rotating 3D-tilted habitat rings + illuminated docking windows
      rotate(degrees = 16f, pivot = Offset(cx, cy)) {
        drawArc(
          color = Color(0xEE4C1D95),
          startAngle = 8f,
          sweepAngle = 164f,
          useCenter = false,
          topLeft = Offset(cx - innerRx, cy - innerRy),
          size = Size(innerRx * 2f, innerRy * 2f),
          style = Stroke(width = 4.4.dp.toPx(), cap = StrokeCap.Round),
        )
        drawArc(
          color = Color(0xFFE879F9),
          startAngle = 14f,
          sweepAngle = 152f,
          useCenter = false,
          topLeft = Offset(cx - innerRx, cy - innerRy),
          size = Size(innerRx * 2f, innerRy * 2f),
          style = Stroke(width = 1.4.dp.toPx(), cap = StrokeCap.Round),
        )
      }

      rotate(degrees = -14f, pivot = Offset(cx, cy)) {
        drawArc(
          color = Color(0xF00F2942),
          startAngle = 6f,
          sweepAngle = 168f,
          useCenter = false,
          topLeft = Offset(cx - outerRx, cy - outerRy),
          size = Size(outerRx * 2f, outerRy * 2f),
          style = Stroke(width = 5.8.dp.toPx(), cap = StrokeCap.Round),
        )
        drawArc(
          color = Color(0xFF00F5D4),
          startAngle = 10f,
          sweepAngle = 160f,
          useCenter = false,
          topLeft = Offset(cx - outerRx, cy - outerRy),
          size = Size(outerRx * 2f, outerRy * 2f),
          style = Stroke(width = 1.9.dp.toPx(), cap = StrokeCap.Round),
        )
        // Illuminated habitat windows rotating along front beveled ring
        for (wIdx in 0 until 9) {
          val deg = ((wIdx * 20f + stationDeg * 1.4f) % 160f) + 10f
          val rad = deg * (PI.toFloat() / 180f)
          val wx = cx + cos(rad) * outerRx
          val wy = cy + sin(rad) * outerRy
          drawCircle(
            color = if (wIdx % 3 == 0) Color(0xFFFACC15) else Color(0xFF7DD3FC),
            radius = 1.5.dp.toPx(),
            center = Offset(wx, wy),
          )
        }
      }

      if (unreadCommsCount > 0 || isFocused) {
        drawCircle(
          color = if (unreadCommsCount > 0) Color(0xCCFACC15) else Color.White.copy(alpha = 0.85f),
          radius = baseR * 1.56f,
          center = Offset(cx, cy),
          style = Stroke(width = 1.5.dp.toPx()),
        )
      }
    }

    // Frameless diegetic holographic station projection (zero rectangular box)
    Text(
      text = if (unreadCommsCount > 0) "NEXUS HUB • $unreadCommsCount NEW" else "NEXUS HUB",
      color = if (unreadCommsCount > 0) Color(0xFFFACC15) else Color(0xFF00F5D4),
      fontSize = 11.sp,
      fontWeight = FontWeight.Black,
      fontFamily = FontFamily.Monospace,
      letterSpacing = 1.0.sp,
      maxLines = 1,
      style = TextStyle(shadow = calloutShadow),
    )
    Text(
      text = "SUBSPACE COMMS • AI • $totalOnlinePilots ONLINE",
      color = Color(0xFFE2E8F0),
      fontSize = 8.sp,
      fontWeight = FontWeight.Bold,
      fontFamily = FontFamily.Monospace,
      maxLines = 1,
      style = TextStyle(shadow = calloutShadow),
    )
  }
}

@Composable
private fun SpatialRiftNode(
  spec: SpatialRiftSpec,
  riftSpinRad: Float,
  pulsePhase: Float,
  lensTimeSec: Float,
  riftLensShaderHolder: Pair<RuntimeShader, NativePaint>?,
  isFocused: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val labelShadow = remember {
    Shadow(color = Color(0xF002050B), offset = Offset(0f, 2f), blurRadius = 7f)
  }
  Column(
    modifier =
      modifier
        .clip(CircleShape)
        .clickable(onClick = onClick),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
  ) {
    Canvas(modifier = Modifier.size(94.dp)) {
      val w = size.width
      val h = size.height
      val cx = w * 0.5f
      val cy = h * 0.5f
      val riftR = minOf(w, h) * 0.36f

      // 1. Native AGSL lensing plasma aura on API 33+ (or smooth radial halo fallback on API 24-32)
      var drewLensShader = false
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && riftLensShaderHolder != null) {
        val (shader, paint) = riftLensShaderHolder
        drewLensShader =
          runCatching {
              shader.setFloatUniform("uResolution", w, h)
              shader.setFloatUniform("uTime", lensTimeSec)
              shader.setFloatUniform("uAccent", spec.accent.red, spec.accent.green, spec.accent.blue)
              paint.shader = shader
              drawContext.canvas.nativeCanvas.drawRect(0f, 0f, w, h, paint)
              true
            }
            .getOrDefault(false)
      }
      if (!drewLensShader) {
        drawCircle(
          brush =
            Brush.radialGradient(
              colors =
                listOf(
                  spec.accent.copy(alpha = 0.42f),
                  spec.accent.copy(alpha = 0.14f),
                  Color.Transparent,
                ),
              center = Offset(cx, cy),
              radius = minOf(w, h) * 0.48f,
            ),
          radius = minOf(w, h) * 0.48f,
          center = Offset(cx, cy),
        )
      }

      // 2. Multi-layered 3D Playfield Rift vortex
      RiftSpriteBaker.draw3dPlayfieldRift(
        canvas = drawContext.canvas.nativeCanvas,
        cx = cx,
        cy = cy,
        r = riftR,
        spinAngle = riftSpinRad,
        outerColor = spec.outerColorInt,
        midColor = spec.midColorInt,
        coreColor = spec.coreColorInt,
        darkVoidCenter = true,
        inwardSpiral = true,
        fadeAlpha = 1.0f,
        pulsePhaseOverride = pulsePhase,
      )

      // 3. Glowing holographic silhouette suspended inside the dark Rift eye
      drawRiftEyeHologram(
        node = spec.node,
        accent = spec.accent,
        center = Offset(cx, cy),
        radius = riftR * 0.38f,
      )

      if (isFocused) {
        drawCircle(
          color = Color.White,
          radius = riftR * 1.18f,
          center = Offset(cx, cy),
          style = Stroke(width = 1.8.dp.toPx()),
        )
      }
    }

    // Frameless floating diegetic holographic callout (zero rectangular box)
    Text(
      text = spec.title,
      color = if (isFocused) Color.White else spec.accent,
      fontSize = 9.4.sp,
      fontWeight = FontWeight.Black,
      fontFamily = FontFamily.Monospace,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      textAlign = TextAlign.Center,
      style = TextStyle(shadow = labelShadow),
    )
    Text(
      text = spec.subtitle,
      color = Color(0xFFD6E4F0),
      fontSize = 7.6.sp,
      fontWeight = FontWeight.SemiBold,
      fontFamily = FontFamily.SansSerif,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      textAlign = TextAlign.Center,
      style = TextStyle(shadow = labelShadow),
    )
  }
}

private fun DrawScope.drawRiftEyeHologram(
  node: NexusCoreNode,
  accent: Color,
  center: Offset,
  radius: Float,
) {
  val strokeW = (radius * 0.16f).coerceAtLeast(1.5f)
  when (node) {
    NexusCoreNode.CONVERGENCE_GATE -> {
      drawCircle(Color.White, radius = radius * 0.92f, center = center, style = Stroke(strokeW))
      drawArc(
        color = accent,
        startAngle = -35f,
        sweepAngle = 250f,
        useCenter = false,
        topLeft = Offset(center.x - radius * 1.18f, center.y - radius * 0.52f),
        size = Size(radius * 2.36f, radius * 1.04f),
        style = Stroke(strokeW, cap = StrokeCap.Round),
      )
      drawCircle(Color.White, radius = radius * 0.24f, center = center)
    }
    NexusCoreNode.HANGAR_DECK -> {
      val hull =
        Path().apply {
          moveTo(center.x, center.y - radius)
          lineTo(center.x + radius * 0.88f, center.y + radius * 0.82f)
          lineTo(center.x, center.y + radius * 0.36f)
          lineTo(center.x - radius * 0.88f, center.y + radius * 0.82f)
          close()
        }
      drawPath(hull, accent.copy(alpha = 0.40f))
      drawPath(hull, Color.White, style = Stroke(strokeW))
    }
    NexusCoreNode.STAR_FORGE -> {
      val star =
        Path().apply {
          moveTo(center.x, center.y - radius)
          lineTo(center.x + radius * 0.34f, center.y - radius * 0.34f)
          lineTo(center.x + radius, center.y)
          lineTo(center.x + radius * 0.34f, center.y + radius * 0.34f)
          lineTo(center.x, center.y + radius)
          lineTo(center.x - radius * 0.34f, center.y + radius * 0.34f)
          lineTo(center.x - radius, center.y)
          lineTo(center.x - radius * 0.34f, center.y - radius * 0.34f)
          close()
        }
      drawPath(star, accent.copy(alpha = 0.35f))
      drawPath(star, Color.White, style = Stroke(strokeW))
    }
    NexusCoreNode.RIFT_CONDUIT -> {
      for (i in 0 until 3) {
        drawArc(
          color = if (i == 0) Color.White else accent,
          startAngle = i * 120f + 15f,
          sweepAngle = 82f,
          useCenter = false,
          topLeft = Offset(center.x - radius * 0.92f, center.y - radius * 0.92f),
          size = Size(radius * 1.84f, radius * 1.84f),
          style = Stroke(strokeW, cap = StrokeCap.Round),
        )
      }
      drawCircle(Color.White, radius = radius * 0.28f, center = center)
    }
    NexusCoreNode.SYNDICATE_CITADEL -> {
      val crest =
        Path().apply {
          moveTo(center.x, center.y - radius)
          lineTo(center.x + radius * 0.86f, center.y - radius * 0.36f)
          lineTo(center.x + radius * 0.62f, center.y + radius * 0.56f)
          lineTo(center.x, center.y + radius)
          lineTo(center.x - radius * 0.62f, center.y + radius * 0.56f)
          lineTo(center.x - radius * 0.86f, center.y - radius * 0.36f)
          close()
        }
      drawPath(crest, accent.copy(alpha = 0.36f))
      drawPath(crest, Color.White, style = Stroke(strokeW))
    }
    NexusCoreNode.ARCHIVE_CODEX -> {
      drawCircle(Color.White, radius = radius * 0.88f, center = center, style = Stroke(strokeW))
      drawLine(
        Color.White,
        Offset(center.x - radius * 0.46f, center.y - radius * 0.22f),
        Offset(center.x + radius * 0.46f, center.y - radius * 0.22f),
        strokeW,
      )
      drawLine(
        accent,
        Offset(center.x - radius * 0.46f, center.y + radius * 0.22f),
        Offset(center.x + radius * 0.46f, center.y + radius * 0.22f),
        strokeW,
      )
    }
    NexusCoreNode.HELM_SYSTEMS -> {
      drawCircle(Color.White, radius = radius * 0.64f, center = center, style = Stroke(strokeW))
      for (k in 0 until 6) {
        val a = (k * 60f) * (PI.toFloat() / 180f)
        drawLine(
          Color.White,
          Offset(center.x + cos(a) * radius * 0.64f, center.y + sin(a) * radius * 0.64f),
          Offset(center.x + cos(a) * radius, center.y + sin(a) * radius),
          strokeW,
        )
      }
    }
    else -> {
      drawCircle(Color.White, radius = radius * 0.78f, center = center, style = Stroke(strokeW))
    }
  }
}

@Composable
private fun GlassTerminalHeaderGlyph(node: NexusCoreNode, accent: Color, sizeDp: Int = 22) {
  Canvas(modifier = Modifier.size(sizeDp.dp)) {
    val c = Offset(size.width * 0.5f, size.height * 0.5f)
    val r = size.minDimension * 0.44f
    drawCircle(accent.copy(alpha = 0.22f), radius = r, center = c)
    drawRiftEyeHologram(node = node, accent = accent, center = c, radius = r * 0.78f)
  }
}

@Composable
private fun GlassTerminalTopHeader(
  activeNode: NexusCoreNode,
  syndicateSubDeck: SyndicateCitadelSubDeck,
  onSelectSyndicateSubDeck: (SyndicateCitadelSubDeck) -> Unit,
  archiveSubDeck: ArchiveCodexSubDeck,
  onSelectArchiveSubDeck: (ArchiveCodexSubDeck) -> Unit,
  armadaCount: Int,
  showClearAi: Boolean,
  isInMatch: Boolean,
  onBackToHub: () -> Unit,
  onClearAi: () -> Unit,
  onLeaveMatch: () -> Unit,
  onCloseToArena: () -> Unit,
) {
  val (title, accent) =
    when (activeNode) {
      NexusCoreNode.SUBSPACE_COMMS -> "SUBSPACE COMMS & NEXUS AI" to Color(0xFF00F5D4)
      NexusCoreNode.CONVERGENCE_GATE -> "CONVERGENCE GATE" to Color(0xFF00F5D4)
      NexusCoreNode.HANGAR_DECK -> "HANGAR DECK" to Color(0xFF38BDF8)
      NexusCoreNode.RIFT_CONDUIT -> "RIFT CONDUIT" to Color(0xFFE879F9)
      NexusCoreNode.SYNDICATE_CITADEL -> "SYNDICATE CITADEL" to Color(0xFFFACC15)
      NexusCoreNode.ARCHIVE_CODEX -> "ARCHIVE & CODEX" to Color(0xFF34D399)
      NexusCoreNode.HELM_SYSTEMS -> "HELM SYSTEMS" to Color(0xFF94A3B8)
      NexusCoreNode.STAR_FORGE -> "STAR FORGE" to Color(0xFFC084FC)
      NexusCoreNode.ORBITAL_HUB -> "NEXUS HUB" to Color(0xFF00F5D4)
    }

  Row(
    modifier =
      Modifier.fillMaxWidth()
        .height(36.dp)
        .clip(RoundedCornerShape(9.dp))
        .background(Color(0xFF071122))
        .border(1.dp, accent.copy(alpha = 0.55f), RoundedCornerShape(9.dp))
        .padding(horizontal = 8.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      Box(
        modifier =
          Modifier.clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF0D2238))
            .border(1.dp, Color(0xFF00F5D4), RoundedCornerShape(6.dp))
            .clickable { onBackToHub() }
            .padding(horizontal = 9.dp, vertical = 4.dp)
            .testTag("nexus_btn_back_to_hub"),
      ) {
        Text(
          text = "< NEXUS HUB",
          color = Color(0xFF00F5D4),
          fontSize = 8.8.sp,
          fontWeight = FontWeight.Black,
          fontFamily = FontFamily.Monospace,
        )
      }
      GlassTerminalHeaderGlyph(node = activeNode, accent = accent, sizeDp = 22)
      Text(
        text = title,
        color = accent,
        fontSize = 11.sp,
        fontWeight = FontWeight.Black,
        fontFamily = FontFamily.Monospace,
        maxLines = 1,
      )

      if (activeNode == NexusCoreNode.SYNDICATE_CITADEL) {
        HeaderSubDeckPill(
          label = "SYNDICATES & WARS",
          selected = syndicateSubDeck == SyndicateCitadelSubDeck.SYNDICATES_WARS,
          accent = Color(0xFFFACC15),
          tag = "citadel_sub_syndicates",
          onClick = { onSelectSyndicateSubDeck(SyndicateCitadelSubDeck.SYNDICATES_WARS) },
        )
        HeaderSubDeckPill(
          label = "STRIKE WING ($armadaCount/5)",
          selected = syndicateSubDeck == SyndicateCitadelSubDeck.STRIKE_WING,
          accent = Color(0xFF38BDF8),
          tag = "citadel_sub_strike_wing",
          onClick = { onSelectSyndicateSubDeck(SyndicateCitadelSubDeck.STRIKE_WING) },
        )
      } else if (activeNode == NexusCoreNode.ARCHIVE_CODEX) {
        HeaderSubDeckPill(
          label = "PILOT DOSSIER",
          selected = archiveSubDeck == ArchiveCodexSubDeck.PILOT_DOSSIER,
          accent = Color(0xFFA78BFA),
          tag = "archive_sub_dossier",
          onClick = { onSelectArchiveSubDeck(ArchiveCodexSubDeck.PILOT_DOSSIER) },
        )
        HeaderSubDeckPill(
          label = "TACTICAL CODEX",
          selected = archiveSubDeck == ArchiveCodexSubDeck.TACTICAL_CODEX,
          accent = Color(0xFF34D399),
          tag = "archive_sub_codex",
          onClick = { onSelectArchiveSubDeck(ArchiveCodexSubDeck.TACTICAL_CODEX) },
        )
      }
    }

    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
      if (showClearAi) {
        Box(
          modifier =
            Modifier.clip(RoundedCornerShape(6.dp))
              .background(Color(0xFF131F33))
              .border(1.dp, Color(0xFF475569), RoundedCornerShape(6.dp))
              .clickable { onClearAi() }
              .padding(horizontal = 8.dp, vertical = 4.dp)
              .testTag("nexus_clear_button"),
        ) {
          Text("CLEAR AI", color = Color(0xFF94A3B8), fontSize = 8.2.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        }
      }
      if (isInMatch) {
        Box(
          modifier =
            Modifier.clip(RoundedCornerShape(6.dp))
              .background(Color(0xCC2A151B))
              .border(1.dp, Color(0xFFFB7185), RoundedCornerShape(6.dp))
              .clickable { onLeaveMatch() }
              .padding(horizontal = 8.dp, vertical = 4.dp)
              .testTag("nexus_leave_match_button"),
        ) {
          Text("LEAVE MATCH", color = Color(0xFFFB7185), fontSize = 8.2.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
        }
      }
      Box(
        modifier =
          Modifier.clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF0A283E))
            .border(1.dp, Color(0xFF00F5D4), RoundedCornerShape(6.dp))
            .clickable { onCloseToArena() }
            .padding(horizontal = 10.dp, vertical = 4.dp)
            .testTag("nexus_close_button"),
      ) {
        Text(
          text = if (isInMatch) "RETURN TO RIFT >" else "VIEW ORBIT >",
          color = Color(0xFF00F5D4),
          fontSize = 8.5.sp,
          fontWeight = FontWeight.Black,
          fontFamily = FontFamily.Monospace,
        )
      }
    }
  }
}

@Composable
private fun HeaderSubDeckPill(
  label: String,
  selected: Boolean,
  accent: Color,
  tag: String,
  onClick: () -> Unit,
) {
  Box(
    modifier =
      Modifier.clip(RoundedCornerShape(6.dp))
        .background(if (selected) accent.copy(alpha = 0.24f) else Color(0xFF0B1629))
        .border(1.dp, if (selected) accent else Color(0xFF334155), RoundedCornerShape(6.dp))
        .clickable { onClick() }
        .padding(horizontal = 8.dp, vertical = 3.dp)
        .testTag(tag),
  ) {
    Text(
      text = label,
      color = if (selected) Color.White else Color(0xFF94A3B8),
      fontSize = 8.2.sp,
      fontWeight = FontWeight.ExtraBold,
      fontFamily = FontFamily.Monospace,
    )
  }
}

@Composable
private fun ConvergenceGateZeroScrollDeck(
  gateTab: ConvergenceGateTab,
  onSelectGateTab: (ConvergenceGateTab) -> Unit,
  activeArmadaName: String,
  armadaSize: Int,
  syndicateTag: String,
  onOpenStrikeWing: () -> Unit,
  activeRankedMode: RankedGameMode,
  skirmishBracket: Int,
  onSelectSkirmishBracket: (Int) -> Unit,
  riftControlBracket: Int,
  onSelectRiftControlBracket: (Int) -> Unit,
  syndicateWarBracket: Int,
  onSelectSyndicateWarBracket: (Int) -> Unit,
  humanPilotCountByMode: (RankedGameMode) -> Int,
  canQueueSyndicateWars: Pair<Boolean, String>,
  syndicateWarRecord: SyndicateWarRecord,
  onStageRanked: (RankedGameMode, Int) -> Unit,
  engine: RiftGameEngine?,
  customIsPrivateRoom: Boolean,
  onTogglePrivateRoom: (Boolean) -> Unit,
  generatedRoomCode: String,
  joinRoomCodeDraft: String,
  onJoinRoomCodeDraftChange: (String) -> Unit,
  roomCodeBanner: String?,
  unlockedPrivateCodes: List<String>,
  onJoinByRoomCode: () -> Unit,
  onShareRoomToLfg: () -> Unit,
  customRefreshTick: Int,
  customBaseMode: RankedGameMode,
  onSelectCustomBaseMode: (RankedGameMode) -> Unit,
  customResetMinutes: Int,
  onSelectCustomResetMinutes: (Int) -> Unit,
  customTeamBracket: Int,
  onSelectCustomTeamBracket: (Int) -> Unit,
  customAllowArmadasInFfa: Boolean,
  onToggleAllowArmadasInFfa: (Boolean) -> Unit,
  customBotsEnabled: Boolean,
  onToggleBotsEnabled: (Boolean) -> Unit,
  customBulletSizeMult: Float,
  onSelectBulletSizeMult: (Float) -> Unit,
  customSpeedMult: Float,
  onSelectSpeedMult: (Float) -> Unit,
  customRechargeMult: Float,
  onSelectRechargeMult: (Float) -> Unit,
  customDamageMult: Float,
  onSelectDamageMult: (Float) -> Unit,
  customCrateSpawnMult: Float,
  onSelectCrateSpawnMult: (Float) -> Unit,
  onReturnToRankedFfa: () -> Unit,
  onJoinCustomInstance: (String) -> Unit,
  onLaunchCustomGame: () -> Unit,
) {
  Column(
    modifier = Modifier.fillMaxSize().testTag("nexus_armada_and_ranked_deck"),
    verticalArrangement = Arrangement.spacedBy(6.dp),
  ) {
    // Slim Sub-Bar: Official Ranked vs Custom Rift Gate + Strike Wing Chip + Unified Hangar Staging Cue
    Row(
      modifier =
        Modifier.fillMaxWidth()
          .height(34.dp)
          .clip(RoundedCornerShape(8.dp))
          .background(Color(0xFF081426))
          .border(1.dp, Color(0x4438BDF8), RoundedCornerShape(8.dp))
          .padding(horizontal = 8.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        HeaderSubDeckPill(
          label = "OFFICIAL RANKED (6)",
          selected = gateTab == ConvergenceGateTab.OFFICIAL_RANKED,
          accent = Color(0xFF00F5D4),
          tag = "gate_tab_official_ranked",
          onClick = { onSelectGateTab(ConvergenceGateTab.OFFICIAL_RANKED) },
        )
        HeaderSubDeckPill(
          label = "CUSTOM RIFT GATE",
          selected = gateTab == ConvergenceGateTab.CUSTOM_RIFT_GATE,
          accent = Color(0xFFC084FC),
          tag = "gate_tab_custom_rift",
          onClick = { onSelectGateTab(ConvergenceGateTab.CUSTOM_RIFT_GATE) },
        )
        Box(
          modifier =
            Modifier.clip(RoundedCornerShape(6.dp))
              .background(Color(0xFF0D2036))
              .border(1.dp, Color(0xFF38BDF8), RoundedCornerShape(6.dp))
              .clickable { onOpenStrikeWing() }
              .padding(horizontal = 7.dp, vertical = 3.dp)
              .testTag("nexus_btn_manage_armada"),
        ) {
          val wingText =
            if (activeArmadaName.isNotBlank()) "STRIKE WING: [$activeArmadaName] ($armadaSize/5)"
            else "STRIKE WING: SOLO (1/5)"
          Text(wingText, color = Color(0xFF38BDF8), fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        }
      }

      Text(
        text = "1. SELECT RIFT MODE  ->  2. STAGE HULL IN HANGAR DECK",
        color = Color(0xFF94A3B8),
        fontSize = 7.8.sp,
        fontWeight = FontWeight.ExtraBold,
        fontFamily = FontFamily.Monospace,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }

    if (gateTab == ConvergenceGateTab.OFFICIAL_RANKED) {
      // Fixed 2x3 Non-Scrolling Grid across all 6 Ranked Modes
      val rows = RankedGameMode.entries.chunked(2)
      Column(
        modifier = Modifier.weight(1f).fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        rows.forEach { rowModes ->
          Row(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            rowModes.forEach { mode ->
              val bracket =
                when (mode) {
                  RankedGameMode.RIFT_SKIRMISH -> skirmishBracket
                  RankedGameMode.RIFT_CONTROL -> riftControlBracket
                  RankedGameMode.SYNDICATE_WARS -> syndicateWarBracket
                  RankedGameMode.STASIS_BREACH -> 10
                  else -> 20
                }
              val isCurrent =
                engine?.hasJoinedMatch == true && activeRankedMode == mode && engine.currentCustomGame == null
              Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                ZeroScrollRankedModeCard(
                  mode = mode,
                  isCurrentActive = isCurrent,
                  humanCount = humanPilotCountByMode(mode).coerceAtLeast(0),
                  armadaSize = armadaSize,
                  syndicateTag = syndicateTag,
                  selectedBracket = bracket,
                  onSelectBracket = { nb ->
                    when (mode) {
                      RankedGameMode.RIFT_SKIRMISH -> onSelectSkirmishBracket(nb)
                      RankedGameMode.RIFT_CONTROL -> onSelectRiftControlBracket(nb)
                      RankedGameMode.SYNDICATE_WARS -> onSelectSyndicateWarBracket(nb)
                      else -> {}
                    }
                  },
                  canQueueSyndicateWars = canQueueSyndicateWars,
                  syndicateWarRecord = syndicateWarRecord,
                  onSelectRiftMode = { onStageRanked(mode, bracket) },
                )
              }
            }
          }
        }
      }
    } else {
      // Fixed 2-Column Custom Rift Gate: Left = 6-Char Room Code Join + Public Browser, Right = 10-Rule Sandbox Cloner
      val activeCustom = remember(engine?.currentCustomGame, customRefreshTick) { engine?.currentCustomGame }
      val visibleCustomGames =
        remember(engine?.activeCustomGames?.size, customRefreshTick, unlockedPrivateCodes.size) {
          (engine?.activeCustomGames?.toList() ?: emptyList()).filter { g ->
            val code = extractCustomRoomCode(g.instanceId)
            !isPrivateCustomGame(g) || activeCustom?.instanceId == g.instanceId || code in unlockedPrivateCodes
          }
        }

      Row(
        modifier = Modifier.weight(1f).fillMaxWidth().testTag("nexus_custom_games_section"),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        // Left Pane: 6-Char Code Joiner + Public Custom Rift Browser
        Column(
          modifier =
            Modifier.weight(0.45f)
              .fillMaxHeight()
              .clip(RoundedCornerShape(10.dp))
              .background(Color(0xCC07101F))
              .border(1.dp, Color(0x6600F5D4), RoundedCornerShape(10.dp))
              .padding(8.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Text(
            text = "PRIVATE RIFT ROOM CODE (6-CHAR)",
            color = Color(0xFF00F5D4),
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
          )
          Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier =
                Modifier.weight(1f)
                  .height(32.dp)
                  .clip(RoundedCornerShape(7.dp))
                  .background(Color(0xFF040914))
                  .border(1.dp, Color(0xFF38BDF8), RoundedCornerShape(7.dp))
                  .padding(horizontal = 8.dp),
              contentAlignment = Alignment.CenterStart,
            ) {
              if (joinRoomCodeDraft.isEmpty()) {
                Text("ENTER 6-CHAR CODE…", color = Color(0xFF64748B), fontSize = 8.5.sp, fontFamily = FontFamily.Monospace)
              }
              BasicTextField(
                value = joinRoomCodeDraft,
                onValueChange = onJoinRoomCodeDraftChange,
                singleLine = true,
                cursorBrush = SolidColor(Color(0xFF00F5D4)),
                textStyle = TextStyle(color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace),
                modifier = Modifier.fillMaxWidth().testTag("nexus_custom_room_code_input"),
              )
            }
            Button(
              onClick = onJoinByRoomCode,
              colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0E7490), contentColor = Color.White),
              border = BorderStroke(1.dp, Color(0xFF00F5D4)),
              shape = RoundedCornerShape(7.dp),
              contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
              modifier = Modifier.height(32.dp).testTag("nexus_btn_join_room_code"),
            ) {
              Text("JOIN CODE", fontSize = 8.4.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
            }
          }
          if (roomCodeBanner != null) {
            Text(roomCodeBanner, color = Color(0xFF34D399), fontSize = 7.8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
          }

          if (activeCustom != null && engine?.hasJoinedMatch == true) {
            Row(
              modifier =
                Modifier.fillMaxWidth()
                  .clip(RoundedCornerShape(7.dp))
                  .background(Color(0xFF06281E))
                  .border(1.dp, Color(0xFF10B981), RoundedCornerShape(7.dp))
                  .padding(6.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = "ACTIVE: ${activeCustom.title}",
                color = Color(0xFF34D399),
                fontSize = 8.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                modifier = Modifier.weight(1f),
              )
              Box(
                modifier =
                  Modifier.clip(RoundedCornerShape(5.dp))
                    .background(Color(0xFF0F172A))
                    .border(1.dp, Color(0xFF34D399), RoundedCornerShape(5.dp))
                    .clickable { onReturnToRankedFfa() }
                    .padding(horizontal = 6.dp, vertical = 3.dp)
                    .testTag("nexus_btn_return_official_ranked"),
              ) {
                Text("RANKED FFA", color = Color(0xFF34D399), fontSize = 7.5.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
              }
            }
          }

          Text(
            text = "PUBLIC CUSTOM RIFT DIRECTORY (${visibleCustomGames.size})",
            color = Color(0xFF94A3B8),
            fontSize = 8.2.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
          )
          LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(5.dp),
          ) {
            if (visibleCustomGames.isEmpty()) {
              item {
                Text(
                  "0 public custom Rifts active. Configure rules on the right to stage a Public or Private Rift.",
                  color = Color(0xFF64748B),
                  fontSize = 8.4.sp,
                  fontFamily = FontFamily.Monospace,
                )
              }
            }
            items(visibleCustomGames, key = { it.instanceId }) { game ->
              val isJoined = engine?.hasJoinedMatch == true && activeCustom?.instanceId == game.instanceId
              val count = engine?.getHumanPilotCountForCustomGame(game.instanceId) ?: 0
              val code = extractCustomRoomCode(game.instanceId)
              Row(
                modifier =
                  Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(7.dp))
                    .background(if (isJoined) Color(0xCC0D2638) else Color(0x99060E1C))
                    .border(1.dp, if (isJoined) Color(0xFF00F5D4) else Color(0xFF334155), RoundedCornerShape(7.dp))
                    .padding(6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = "${game.title} • $count PILOTS",
                    color = if (isJoined) Color(0xFF00F5D4) else Color.White,
                    fontSize = 8.2.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                  )
                  Text(
                    text = "${game.baseMode.title} • CODE: $code • Bots: ${if (game.botsEnabled) "ON" else "OFF"}",
                    color = Color(0xFF94A3B8),
                    fontSize = 7.4.sp,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                  )
                }
                Box(
                  modifier =
                    Modifier.clip(RoundedCornerShape(6.dp))
                      .background(if (isJoined) Color(0xFF065F46) else Color(0xFF1E293B))
                      .border(1.dp, Color(0xFF00F5D4), RoundedCornerShape(6.dp))
                      .clickable { onJoinCustomInstance(game.instanceId) }
                      .padding(horizontal = 8.dp, vertical = 4.dp)
                      .testTag("nexus_join_custom_${game.instanceId}"),
                ) {
                  Text(
                    text = if (isJoined) "CHANGE HULL" else "STAGE RIFT >",
                    color = Color.White,
                    fontSize = 7.8.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                  )
                }
              }
            }
          }
        }

        // Right Pane: 10-Rule Custom Rift Sandbox Cloner (Public or Private 6-Char Room Code)
        Column(
          modifier =
            Modifier.weight(0.55f)
              .fillMaxHeight()
              .clip(RoundedCornerShape(10.dp))
              .background(Color(0xCC07101F))
              .border(1.dp, Color(0x66C084FC), RoundedCornerShape(10.dp))
              .padding(8.dp),
          verticalArrangement = Arrangement.SpaceBetween,
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
              HeaderSubDeckPill("PUBLIC ROOM", !customIsPrivateRoom, Color(0xFF00F5D4), "custom_vis_public") {
                onTogglePrivateRoom(false)
              }
              HeaderSubDeckPill("PRIVATE [$generatedRoomCode]", customIsPrivateRoom, Color(0xFFFACC15), "custom_vis_private") {
                onTogglePrivateRoom(true)
              }
            }
            Box(
              modifier =
                Modifier.clip(RoundedCornerShape(6.dp))
                  .background(Color(0xFF13263A))
                  .border(1.dp, Color(0xFF34D399), RoundedCornerShape(6.dp))
                  .clickable { onShareRoomToLfg() }
                  .padding(horizontal = 7.dp, vertical = 3.dp)
                  .testTag("nexus_btn_share_room_lfg"),
            ) {
              Text("SHARE TO #STRIKE-LFG", color = Color(0xFF34D399), fontSize = 7.6.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
            }
          }

          Column(
            modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            CustomRuleChipRow(
              "BASE MODE:",
              listOf(
                RankedGameMode.SECTOR_CONVERGENCE to "Convergence",
                RankedGameMode.RIFT_SKIRMISH to "Skirmish",
                RankedGameMode.FLUX_CORE to "Flux Core",
                RankedGameMode.STASIS_BREACH to "Stasis Breach",
                RankedGameMode.RIFT_CONTROL to "Rift Control",
              ),
              customBaseMode,
              onSelectCustomBaseMode,
            )
            CustomRuleChipRow(
              "TIMER & SIZE:",
              listOf(5 to "5m", 10 to "10m", 15 to "15m", 30 to "30m", 0 to "INF"),
              customResetMinutes,
              onSelectCustomResetMinutes,
            )
            CustomRuleChipRow(
              "TEAM BRACKET:",
              listOf(2 to "2v2", 3 to "3v3", 5 to "5v5", 10 to "10v10"),
              customTeamBracket,
              onSelectCustomTeamBracket,
            )
            CustomRuleChipRow(
              "FFA WINGS:",
              listOf(true to "Strike Wings ON", false to "Solo Only"),
              customAllowArmadasInFfa,
              onToggleAllowArmadasInFfa,
            )
            CustomRuleChipRow(
              "AI BOTS:",
              listOf(true to "Backfill [BOT]", false to "Humans Only"),
              customBotsEnabled,
              onToggleBotsEnabled,
            )
            CustomRuleChipRow(
              "BULLET SIZE:",
              listOf(1.0f to "1.0x Standard", 1.5f to "1.5x Heavy", 2.2f to "2.2x MegaFat Bullets"),
              customBulletSizeMult,
              onSelectBulletSizeMult,
            )
            CustomRuleChipRow(
              "SHIP SPEED:",
              listOf(0.8f to "0.8x Tactical", 1.0f to "1.0x Standard", 1.4f to "1.4x Overdrive"),
              customSpeedMult,
              onSelectSpeedMult,
            )
            CustomRuleChipRow(
              "FLUX RECHARGE:",
              listOf(1.0f to "1.0x Standard", 1.5f to "1.5x Surge", 2.5f to "2.5x Hyper"),
              customRechargeMult,
              onSelectRechargeMult,
            )
            CustomRuleChipRow(
              "WEAPON DMG:",
              listOf(0.75f to "0.75x Armored", 1.0f to "1.0x Standard", 1.5f to "1.5x Lethal"),
              customDamageMult,
              onSelectDamageMult,
            )
            CustomRuleChipRow(
              "RIFTS/CRATES:",
              listOf(0.5f to "0.5x Sparse", 1.0f to "1.0x Standard", 2.0f to "2.0x Dense"),
              customCrateSpawnMult,
              onSelectCrateSpawnMult,
            )
          }

          Button(
            onClick = onLaunchCustomGame,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0E7490), contentColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFF00F5D4)),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth().height(32.dp).testTag("nexus_btn_launch_custom_game"),
          ) {
            Text(
              text =
                if (customIsPrivateRoom) "STAGE PRIVATE RIFT [$generatedRoomCode] -> CHOOSE HULL"
                else "STAGE PUBLIC CUSTOM RIFT -> CHOOSE HULL",
              fontSize = 8.6.sp,
              fontWeight = FontWeight.Black,
              fontFamily = FontFamily.Monospace,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun ZeroScrollRankedModeCard(
  mode: RankedGameMode,
  isCurrentActive: Boolean,
  humanCount: Int,
  armadaSize: Int,
  syndicateTag: String,
  selectedBracket: Int,
  onSelectBracket: (Int) -> Unit,
  canQueueSyndicateWars: Pair<Boolean, String>,
  syndicateWarRecord: SyndicateWarRecord,
  onSelectRiftMode: () -> Unit,
) {
  val accent =
    when (mode) {
      RankedGameMode.SECTOR_CONVERGENCE,
      RankedGameMode.RIFT_CONTROL -> Color(0xFF00F5D4)
      RankedGameMode.RIFT_SKIRMISH,
      RankedGameMode.FLUX_CORE -> Color(0xFFF59E0B)
      RankedGameMode.STASIS_BREACH,
      RankedGameMode.SYNDICATE_WARS -> Color(0xFFA855F7)
    }
  val title = if (mode == RankedGameMode.RIFT_CONTROL) "RIFT CONTROL" else mode.title.uppercase(Locale.US)
  val desc =
    when (mode) {
      RankedGameMode.SECTOR_CONVERGENCE -> "20-Pilot FFA / 1-5 Strike Wing • Circular Rift Safe Zone • +100❖ #1 Bounty."
      RankedGameMode.RIFT_SKIRMISH -> "Best-of-3 Team Rounds • Opposite Warp Rifts • Locks Hull on Hangar Entry."
      RankedGameMode.FLUX_CORE -> "20-Pilot Carrier FFA • +1 Overcharge Tier / 5s • 30s Safe-Zone Eject."
      RankedGameMode.STASIS_BREACH -> "10v10 Stasis Jailbreak • No Safe Zone • Destroy 8,000 Flux Jail Core to free allies."
      RankedGameMode.RIFT_CONTROL -> "Dual-Rift Tug-of-War • No Safe Zone • Hold uncontested 30s to lock Rifts."
      RankedGameMode.SYNDICATE_WARS -> "[$syndicateTag] Clan Ladder • ${syndicateWarRecord.warTrophies} War Trophies (${syndicateWarRecord.warWins}W-${syndicateWarRecord.warLosses}L)."
    }
  val isLocked = mode == RankedGameMode.SYNDICATE_WARS && !canQueueSyndicateWars.first

  Column(
    modifier =
      Modifier.fillMaxSize()
        .clip(RoundedCornerShape(10.dp))
        .background(if (isCurrentActive) Color(0xE60B1D33) else Color(0xD907101F))
        .border(
          if (isCurrentActive) 1.5.dp else 1.dp,
          if (isCurrentActive) accent else accent.copy(alpha = 0.50f),
          RoundedCornerShape(10.dp),
        )
        .padding(horizontal = 9.dp, vertical = 6.dp)
        .testTag("nexus_mode_card_${mode.id}"),
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
        modifier = Modifier.weight(1f),
      ) {
        Text(
          text = title,
          color = accent,
          fontSize = 9.8.sp,
          fontWeight = FontWeight.Black,
          fontFamily = FontFamily.Monospace,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Box(
          modifier =
            Modifier.clip(RoundedCornerShape(4.dp))
              .background(accent.copy(alpha = 0.18f))
              .padding(horizontal = 5.dp, vertical = 1.dp),
        ) {
          Text(mode.teamFormatBadge, color = accent, fontSize = 7.2.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        }
      }
      Text(
        text = "$humanCount PILOTS",
        color = if (humanCount > 0) Color(0xFF34D399) else Color(0xFF64748B),
        fontSize = 7.8.sp,
        fontWeight = FontWeight.ExtraBold,
        fontFamily = FontFamily.Monospace,
      )
    }

    Text(
      text = desc,
      color = Color(0xFFCBD5E1),
      fontSize = 8.2.sp,
      lineHeight = 11.sp,
      maxLines = 2,
      overflow = TextOverflow.Ellipsis,
    )

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        when (mode) {
          RankedGameMode.RIFT_SKIRMISH ->
            listOf(2, 3, 4, 5).forEach { sz ->
              ModeBracketChip("${sz}v${sz}", selectedBracket == sz, accent) { onSelectBracket(sz) }
            }
          RankedGameMode.RIFT_CONTROL,
          RankedGameMode.SYNDICATE_WARS ->
            listOf(3, 5, 10).forEach { sz ->
              ModeBracketChip("${sz}v${sz}", selectedBracket == sz, accent) { onSelectBracket(sz) }
            }
          else -> {
            Text(
              text = if (isCurrentActive) "ACTIVE RIFT" else "STAGE VIA HANGAR",
              color = if (isCurrentActive) accent else Color(0xFF64748B),
              fontSize = 7.5.sp,
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.Monospace,
            )
          }
        }
      }

      Button(
        onClick = onSelectRiftMode,
        enabled = !isLocked,
        colors =
          ButtonDefaults.buttonColors(
            containerColor = accent.copy(alpha = 0.24f),
            contentColor = Color.White,
            disabledContainerColor = Color(0x551E293B),
            disabledContentColor = Color(0xFF64748B),
          ),
        border = BorderStroke(1.dp, if (isLocked) Color(0xFF475569) else accent),
        shape = RoundedCornerShape(7.dp),
        contentPadding = PaddingValues(horizontal = 9.dp, vertical = 2.dp),
        modifier = Modifier.height(26.dp).testTag("nexus_deploy_${mode.id}"),
      ) {
        Text(
          text =
            when {
              isLocked -> "SYNDICATE REQ"
              armadaSize >= 2 -> "SELECT RIFT ($armadaSize) >"
              else -> "SELECT RIFT >"
            },
          fontSize = 7.8.sp,
          fontWeight = FontWeight.Black,
          fontFamily = FontFamily.Monospace,
        )
      }
    }
  }
}

@Composable
private fun ModeBracketChip(label: String, selected: Boolean, accent: Color, onClick: () -> Unit) {
  Box(
    modifier =
      Modifier.clip(RoundedCornerShape(5.dp))
        .background(if (selected) accent.copy(alpha = 0.25f) else Color(0xFF0F172A))
        .border(1.dp, if (selected) accent else Color(0xFF334155), RoundedCornerShape(5.dp))
        .clickable { onClick() }
        .padding(horizontal = 5.dp, vertical = 2.dp),
  ) {
    Text(label, color = if (selected) Color.White else Color(0xFF94A3B8), fontSize = 7.4.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace)
  }
}

@Composable
private fun <T> CustomRuleChipRow(
  label: String,
  options: List<Pair<T, String>>,
  selected: T,
  onSelect: (T) -> Unit,
) {
  Row(
    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
    horizontalArrangement = Arrangement.spacedBy(4.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      label,
      color = Color(0xFF94A3B8),
      fontSize = 7.5.sp,
      fontWeight = FontWeight.Bold,
      fontFamily = FontFamily.Monospace,
      modifier = Modifier.width(88.dp),
    )
    options.forEach { (value, text) ->
      val isSel = selected == value
      Box(
        modifier =
          Modifier.clip(RoundedCornerShape(5.dp))
            .background(if (isSel) Color(0x3300F5D4) else Color(0xFF0F172A))
            .border(1.dp, if (isSel) Color(0xFF00F5D4) else Color(0xFF334155), RoundedCornerShape(5.dp))
            .clickable { onSelect(value) }
            .padding(horizontal = 6.dp, vertical = 2.dp),
      ) {
        Text(
          text,
          color = if (isSel) Color.White else Color(0xFF94A3B8),
          fontSize = 7.4.sp,
          fontWeight = FontWeight.ExtraBold,
          fontFamily = FontFamily.Monospace,
        )
      }
    }
  }
}

@Composable
private fun SubspaceCommsDiscordDeck(
  selectedChannelKey: String,
  onSelectChannel: (String) -> Unit,
  messages: List<NexusChatMessage>,
  streamState: NexusStreamState,
  isStreaming: Boolean,
  lastFailedQuestion: String?,
  listState: androidx.compose.foundation.lazy.LazyListState,
  inputQuery: String,
  onInputQueryChange: (String) -> Unit,
  onSubmitAiQuery: () -> Unit,
  onSendStarterQuestion: (String) -> Unit,
  onRetryLastQuestion: () -> Unit,
  cleanSyndicateTag: String,
  syndicateName: String,
  isLocalArchitect: Boolean,
  activeArmadaName: String,
  armadaSlots: List<ArmadaMemberSlot>,
  canAccessSyndicateServer: Boolean,
  syndicateCustomChannels: List<String>,
  syndicateInvitedGuests: Set<String>,
  showChannelManager: Boolean,
  onToggleChannelManager: () -> Unit,
  newChannelDraft: String,
  onNewChannelDraftChange: (String) -> Unit,
  onAddSyndicateChannel: () -> Unit,
  onDeleteSyndicateChannel: (String) -> Unit,
  showGuestManager: Boolean,
  onToggleGuestManager: () -> Unit,
  guestCallsignDraft: String,
  onGuestDraftChange: (String) -> Unit,
  onInviteGuest: () -> Unit,
  onRevokeGuest: (String) -> Unit,
  onOpenSyndicateCitadel: () -> Unit,
  lobbyMessages: List<SyndicateChatMessage>,
  localEchoMessages: Map<String, List<SyndicateChatMessage>>,
  channelChatInput: String,
  onChannelChatInputChange: (String) -> Unit,
  onSendToChannel: (String, String) -> Unit,
  bugCategory: String,
  onSelectBugCategory: (String) -> Unit,
  bugDescription: String,
  onBugDescriptionChange: (String) -> Unit,
  bugStatusBanner: String?,
  onSubmitBug: () -> Unit,
  activeCustomRoomCode: String?,
  activeCustomModeTitle: String?,
  onShareRiftFrameBuild: () -> Unit,
  onLoadSharedBuild: (String) -> Unit,
  onAskAiAboutBuild: (String) -> Unit,
  onPostStrikeWingInvite: () -> Unit,
  onPostPrivateRoomInvite: (String, String) -> Unit,
  onJoinSharedStrikeWing: (SyndicateChatMessage) -> Unit,
  onJoinCustomRoomCode: (String) -> Unit,
  onlineHumanPilots: List<LivePilotState>,
  totalOnlinePilots: Int,
  showOnlineRosterPane: Boolean,
  onToggleOnlineRosterPane: () -> Unit,
  onAddOnlinePilotFriend: (String) -> Unit,
  onInviteOnlinePilotToWing: (LivePilotState) -> Unit,
) {
  val strikeWingKey = "strike:${activeArmadaName.ifBlank { "SQUAD" }.uppercase(Locale.US)}"

  Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    // 1. Left Channel Directory
    Column(
      modifier =
        Modifier.width(188.dp)
          .fillMaxHeight()
          .clip(RoundedCornerShape(10.dp))
          .background(Color(0xB8040914))
          .border(1.dp, Color(0x4438BDF8), RoundedCornerShape(10.dp))
          .padding(7.dp)
          .verticalScroll(rememberScrollState())
          .testTag("nexus_channel_directory"),
      verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
      ChannelCategoryHeader("// NEXUS INTEL")
      ChannelNavItem("#nexus-ai", "AI", selectedChannelKey == "#nexus-ai", Color(0xFF00F5D4), { onSelectChannel("#nexus-ai") }, "nexus_ch_nexus_ai")
      ChannelNavItem("#announcements", "DEV", selectedChannelKey == "#announcements", Color(0xFFFACC15), { onSelectChannel("#announcements") }, "nexus_ch_announcements")
      ChannelNavItem("#patch-log", "v1.0.$CURRENT_RIFT_BUILD_VERSION", selectedChannelKey == "#patch-log", Color(0xFF38BDF8), { onSelectChannel("#patch-log") }, "nexus_ch_change_log")
      ChannelNavItem("#bug-reports", "CLOUD", selectedChannelKey == "#bug-reports", Color(0xFFFB7185), { onSelectChannel("#bug-reports") }, "nexus_ch_bug_report")
      ChannelNavItem("#rules", "FAIR", selectedChannelKey == "#rules", Color(0xFFA78BFA), { onSelectChannel("#rules") }, "nexus_ch_rules")

      Spacer(modifier = Modifier.height(3.dp))
      ChannelCategoryHeader("// COMMUNITY")
      ChannelNavItem("#sector-lounge", "$totalOnlinePilots", selectedChannelKey == "#sector-lounge", Color(0xFF38BDF8), { onSelectChannel("#sector-lounge") }, "nexus_ch_main_lobby")
      ChannelNavItem("#shipwright-meta", "100PT", selectedChannelKey == "#shipwright-meta", Color(0xFFC084FC), { onSelectChannel("#shipwright-meta") }, "nexus_ch_shipwright_meta")
      ChannelNavItem("#strike-lfg", "LFG", selectedChannelKey == "#strike-lfg", Color(0xFF34D399), { onSelectChannel("#strike-lfg") }, "nexus_ch_strike_lfg")

      Spacer(modifier = Modifier.height(3.dp))
      ChannelCategoryHeader("// SYNDICATE SERVER")
      if (canAccessSyndicateServer) {
        syndicateCustomChannels.forEachIndexed { idx, chName ->
          val cleanCh = if (chName.startsWith("#")) chName else "#$chName"
          val key = "syn:$cleanSyndicateTag:$cleanCh".lowercase(Locale.US)
          ChannelNavItem(
            label = "[$cleanSyndicateTag] $cleanCh",
            badge = if (idx == 0) "CORE" else "CLAN",
            selected = selectedChannelKey.equals(key, ignoreCase = true),
            accent = Color(0xFFF59E0B),
            onClick = { onSelectChannel(key) },
            tag = "nexus_ch_syndicate_$idx",
          )
        }
        Row(modifier = Modifier.fillMaxWidth().padding(top = 2.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
          Box(
            modifier =
              Modifier.weight(1f)
                .clip(RoundedCornerShape(5.dp))
                .background(Color(0xFF1E293B))
                .border(1.dp, Color(0x66F59E0B), RoundedCornerShape(5.dp))
                .clickable { onToggleChannelManager() }
                .padding(vertical = 3.dp)
                .testTag("nexus_btn_manage_syndicate_channels"),
            contentAlignment = Alignment.Center,
          ) {
            Text("+ CH (${syndicateCustomChannels.size}/5)", color = Color(0xFFFBBF24), fontSize = 7.6.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
          }
          Box(
            modifier =
              Modifier.weight(1f)
                .clip(RoundedCornerShape(5.dp))
                .background(Color(0xFF1E293B))
                .border(1.dp, Color(0x6638BDF8), RoundedCornerShape(5.dp))
                .clickable { onToggleGuestManager() }
                .padding(vertical = 3.dp)
                .testTag("nexus_btn_syndicate_guest_pass"),
            contentAlignment = Alignment.Center,
          ) {
            Text("GUESTS (${syndicateInvitedGuests.size})", color = Color(0xFF38BDF8), fontSize = 7.6.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
          }
        }
      } else {
        ChannelNavItem("Syndicate Locked", "ENLIST", selectedChannelKey == "syndicate:locked", Color(0xFF64748B), { onSelectChannel("syndicate:locked") }, "nexus_ch_syndicate_locked")
      }

      Spacer(modifier = Modifier.height(3.dp))
      ChannelCategoryHeader("// STRIKE WING")
      ChannelNavItem(
        label = "#strike-wing",
        badge = "${armadaSlots.size.coerceAtLeast(1)}/5",
        selected = selectedChannelKey.equals(strikeWingKey, ignoreCase = true),
        accent = Color(0xFF60A5FA),
        onClick = { onSelectChannel(strikeWingKey) },
        tag = "nexus_ch_armada_chat",
      )
    }

    // 2. Center Live Channel Feed & Composer
    Column(
      modifier =
        Modifier.weight(1f)
          .fillMaxHeight()
          .clip(RoundedCornerShape(10.dp))
          .background(Color(0x99040914))
          .border(1.dp, Color(0x4438BDF8), RoundedCornerShape(10.dp))
          .padding(8.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      if (showChannelManager && canAccessSyndicateServer) {
        SyndicateChannelManagerCard(
          syndicateTag = cleanSyndicateTag,
          channels = syndicateCustomChannels,
          newChannelDraft = newChannelDraft,
          onDraftChange = onNewChannelDraftChange,
          onAddChannel = onAddSyndicateChannel,
          onDeleteChannel = onDeleteSyndicateChannel,
          onClose = onToggleChannelManager,
        )
      }
      if (showGuestManager && canAccessSyndicateServer) {
        SyndicateGuestPassManagerCard(
          syndicateTag = cleanSyndicateTag,
          invitedGuests = syndicateInvitedGuests,
          guestDraft = guestCallsignDraft,
          onDraftChange = onGuestDraftChange,
          onInvite = onInviteGuest,
          onRevoke = onRevokeGuest,
          onClose = onToggleGuestManager,
        )
      }

      val normSelected = selectedChannelKey.trim().lowercase(Locale.US)
      val mergedChannelMessages =
        remember(lobbyMessages.size, normSelected, localEchoMessages[normSelected]?.size) {
          val fromCloud = lobbyMessages.filter { it.resolvedChannelKey() == normSelected }
          val cloudTexts = fromCloud.map { "${it.senderCallsign}|${it.messageText}" }.toSet()
          val echoes =
            (localEchoMessages[normSelected] ?: emptyList()).filterNot {
              "${it.senderCallsign}|${it.messageText}" in cloudTexts
            }
          (fromCloud + echoes).sortedBy { it.createdAtMs }
        }

      when {
        normSelected == "#nexus-ai" -> {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Row(
              modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState()),
              horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              NEXUS_STARTER_CHIPS.forEachIndexed { idx, chipPrompt ->
                Box(
                  modifier =
                    Modifier.clip(RoundedCornerShape(12.dp))
                      .background(Color(0x990D1B2E))
                      .border(1.dp, Color(0x6638BDF8), RoundedCornerShape(12.dp))
                      .clickable(enabled = !isStreaming) { onSendStarterQuestion(chipPrompt) }
                      .padding(horizontal = 8.dp, vertical = 4.dp)
                      .testTag("nexus_quick_chip_$idx"),
                ) {
                  Text(chipPrompt, color = if (isStreaming) Color(0xFF64748B) else Color(0xFFE0F2FE), fontSize = 8.5.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                }
              }
            }
            Spacer(modifier = Modifier.width(6.dp))
            OnlineRosterToggleButton(onlineHumanPilots.size, showOnlineRosterPane, onToggleOnlineRosterPane)
          }

          LazyColumn(
            state = listState,
            modifier =
              Modifier.weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0x59030712))
                .border(1.dp, Color(0x3338BDF8), RoundedCornerShape(8.dp))
                .padding(8.dp)
                .testTag("nexus_message_list"),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            if (messages.isEmpty()) item(key = "nexus_zero_state") { NexusWelcomeTelemetryCard() }
            items(items = messages, key = { it.id }) { msg -> NexusMessageBubble(message = msg) }
            if (streamState is NexusStreamState.Error) {
              item(key = "nexus_error_card") {
                NexusErrorBanner(streamState.message, !lastFailedQuestion.isNullOrBlank(), onRetryLastQuestion)
              }
            }
          }

          Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(
              modifier =
                Modifier.weight(1f)
                  .height(34.dp)
                  .clip(RoundedCornerShape(8.dp))
                  .background(Color(0xFF050B14))
                  .border(1.dp, if (isStreaming) Color(0x55475569) else Color(0xFF00F5D4), RoundedCornerShape(8.dp))
                  .padding(horizontal = 10.dp)
                  .testTag("nexus_query_input_box"),
              contentAlignment = Alignment.CenterStart,
            ) {
              if (inputQuery.isEmpty()) {
                Text(stringResource(R.string.nexus_input_placeholder), color = Color(0xFF64748B), fontSize = 9.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
              }
              BasicTextField(
                value = inputQuery,
                onValueChange = onInputQueryChange,
                enabled = !isStreaming,
                singleLine = true,
                cursorBrush = SolidColor(Color(0xFF00F5D4)),
                textStyle = TextStyle(color = Color.White, fontSize = 10.5.sp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onSubmitAiQuery() }),
                modifier = Modifier.fillMaxWidth().testTag("nexus_query_input"),
              )
            }
            Button(
              onClick = onSubmitAiQuery,
              enabled = inputQuery.isNotBlank() && !isStreaming,
              colors =
                ButtonDefaults.buttonColors(
                  containerColor = Color(0xFF0E7490),
                  contentColor = Color.White,
                  disabledContainerColor = Color(0x661E293B),
                  disabledContentColor = Color(0xFF64748B),
                ),
              border = BorderStroke(1.dp, if (inputQuery.isNotBlank() && !isStreaming) Color(0xFF00F5D4) else Color(0x44475569)),
              shape = RoundedCornerShape(8.dp),
              contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
              modifier = Modifier.height(34.dp).testTag("nexus_send_button"),
            ) {
              Text(if (isStreaming) "SYNCING…" else stringResource(R.string.nexus_transmit), fontSize = 9.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
            }
          }
        }
        normSelected == "#patch-log" -> NexusChangelogChannelView()
        normSelected == "#rules" -> NexusRulesChannelView()
        normSelected == "#bug-reports" -> {
          val bugFeed =
            remember(lobbyMessages.size, localEchoMessages["#bug-reports"]?.size) {
              lobbyMessages.filter { it.isBugReportCard() }
            }
          NexusBugReportChannelView(
            selectedCategory = bugCategory,
            onSelectCategory = onSelectBugCategory,
            description = bugDescription,
            onDescriptionChange = onBugDescriptionChange,
            statusBanner = bugStatusBanner,
            cloudBugCards = bugFeed,
            onSubmit = onSubmitBug,
          )
        }
        normSelected == "syndicate:locked" -> {
          Column(
            modifier =
              Modifier.fillMaxSize()
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0x66091324))
                .border(1.dp, Color(0x66F59E0B), RoundedCornerShape(10.dp))
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
          ) {
            Text("SYNDICATE LOCKED // ENLIST IN SYNDICATE CITADEL", color = Color(0xFFFACC15), fontSize = 12.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              "Enlist in or charter a real-player Syndicate to unlock private [TAG] channels, Syndicate Crests, and Ranked Syndicate Wars.",
              color = Color(0xFFCBD5E1),
              fontSize = 10.sp,
              textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(10.dp))
            Button(
              onClick = onOpenSyndicateCitadel,
              colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF78350F), contentColor = Color(0xFFFEF08A)),
              border = BorderStroke(1.dp, Color(0xFFF59E0B)),
              shape = RoundedCornerShape(8.dp),
            ) {
              Text("OPEN SYNDICATE CITADEL", fontSize = 9.5.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
            }
          }
        }
        else -> {
          val isAnnouncements = normSelected == "#announcements"
          val isShipwright = normSelected == "#shipwright-meta"
          val isLfg = normSelected == "#strike-lfg"
          val isSyndicateCh = normSelected.startsWith("syn:")
          val isStrikeWingCh = normSelected.startsWith("strike:")

          val headerTitle =
            when {
              isAnnouncements -> "#announcements // ARCHITECT & RING SPACE DISPATCH"
              isShipwright -> "#shipwright-meta // 100-PT RIFT FRAME LOADOUT EXCHANGE"
              isLfg -> "#strike-lfg // STRIKE WING & PRIVATE RIFT RECRUITMENT"
              isSyndicateCh -> "[$cleanSyndicateTag] ${selectedChannelKey.substringAfterLast(':')} // $syndicateName"
              isStrikeWingCh -> "#strike-wing // [${activeArmadaName.ifBlank { "SQUAD" }}] ENCRYPTED COMMS"
              else -> "#sector-lounge // LIVE CROSS-SECTOR PILOT FEED"
            }

          Row(
            modifier =
              Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xAA091426))
                .border(1.dp, Color(0x4438BDF8), RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = headerTitle,
              color = Color(0xFF00F5D4),
              fontSize = 9.5.sp,
              fontWeight = FontWeight.Black,
              fontFamily = FontFamily.Monospace,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.weight(1f),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
              if (isShipwright) {
                ActionHeaderChip("SHARE RIFT FRAME BUILD", Color(0xFFC084FC), "nexus_btn_share_build", onShareRiftFrameBuild)
              }
              if (isLfg) {
                ActionHeaderChip("POST STRIKE WING INVITE", Color(0xFF34D399), "nexus_btn_post_wing_lfg", onPostStrikeWingInvite)
                if (!activeCustomRoomCode.isNullOrBlank()) {
                  ActionHeaderChip(
                    "POST ROOM [$activeCustomRoomCode]",
                    Color(0xFFFACC15),
                    "nexus_btn_post_room_lfg",
                  ) {
                    onPostPrivateRoomInvite(activeCustomRoomCode, activeCustomModeTitle ?: "Custom Rift")
                  }
                }
              }
              OnlineRosterToggleButton(onlineHumanPilots.size, showOnlineRosterPane, onToggleOnlineRosterPane)
            }
          }

          LazyColumn(
            modifier =
              Modifier.weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0x55030712))
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            if (isAnnouncements) {
              item(key = "pinned_architect_dispatch") {
                Column(
                  modifier =
                    Modifier.fillMaxWidth()
                      .clip(RoundedCornerShape(8.dp))
                      .background(Color(0xCC1B1608))
                      .border(1.dp, Color(0xFFFACC15), RoundedCornerShape(8.dp))
                      .padding(8.dp),
                  verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                  Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    DevArchitectBadge()
                    Text("ARCHITECT // BUILD #$CURRENT_RIFT_BUILD_VERSION RING SPACE DISPATCH", color = Color(0xFFFACC15), fontSize = 9.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                  }
                  Text(
                    "Welcome to NEXUS (Threshold Dynamics Studio, LLC). Select any of the 7 living 3D Rifts around the central NEXUS HUB Station. All 10 Hulls operate at 100-Point Parity from 0 Resonance — share Rift Frame builds in #shipwright-meta, form Strike Wings in #strike-lfg, and log live telemetry in #bug-reports.",
                    color = Color(0xFFF1F5F9),
                    fontSize = 9.2.sp,
                    lineHeight = 13.sp,
                  )
                }
              }
            }
            if (mergedChannelMessages.isEmpty() && !isAnnouncements) {
              item {
                Text(
                  "Frequency online. Transmit a message or action card to connected Sworn Pilots.",
                  color = Color(0xFF64748B),
                  fontSize = 9.sp,
                  fontFamily = FontFamily.Monospace,
                )
              }
            }
            items(mergedChannelMessages, key = { it.id }) { msg ->
              CloudChannelMessageCard(
                msg = msg,
                onLoadSharedBuild = onLoadSharedBuild,
                onAskAiAboutBuild = onAskAiAboutBuild,
                onJoinSharedStrikeWing = onJoinSharedStrikeWing,
                onJoinCustomRoomCode = onJoinCustomRoomCode,
              )
            }
          }

          if (isStrikeWingCh || isSyndicateCh) {
            Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              TacticalQuickPingChip("ATTACK HVT", Color(0xFFFB7185), "nexus_quick_ping_attack_hvt") {
                onSendToChannel(selectedChannelKey, "[TACTICAL PING] CONVERGE ON SECTOR HVT NOW!")
              }
              TacticalQuickPingChip("FORM ON ME", Color(0xFF38BDF8), "nexus_quick_ping_form_on_me") {
                onSendToChannel(selectedChannelKey, "[TACTICAL PING] FORM UP ON MY STRIKE WING!")
              }
              TacticalQuickPingChip("LOW FLUX", Color(0xFFFACC15), "nexus_quick_ping_low_flux") {
                onSendToChannel(selectedChannelKey, "[TACTICAL PING] LOW FLUX — NEED STASIS / BARRIER COVER!")
              }
            }
          }

          if (isAnnouncements && !isLocalArchitect) {
            Text(
              "READ-ONLY ARCHITECT DISPATCH // POST FEEDBACK IN #SECTOR-LOUNGE OR #BUG-REPORTS",
              color = Color(0xFF94A3B8),
              fontSize = 8.2.sp,
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.Monospace,
            )
          } else {
            ChannelInputComposer(
              placeholder = "Transmit to $selectedChannelKey…",
              value = channelChatInput,
              onValueChange = onChannelChatInputChange,
              onSend = { onSendToChannel(selectedChannelKey, channelChatInput) },
            )
          }
        }
      }
    }

    // 3. Right Collapsible Live Online Pilots Roster
    if (showOnlineRosterPane) {
      Column(
        modifier =
          Modifier.width(184.dp)
            .fillMaxHeight()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xB8040914))
            .border(1.dp, Color(0x4410B981), RoundedCornerShape(10.dp))
            .padding(7.dp)
            .testTag("nexus_online_pilots_drawer"),
        verticalArrangement = Arrangement.spacedBy(5.dp),
      ) {
        Text(
          text = "ONLINE PILOTS (${onlineHumanPilots.size})",
          color = Color(0xFF34D399),
          fontSize = 8.8.sp,
          fontWeight = FontWeight.Black,
          fontFamily = FontFamily.Monospace,
        )
        LazyColumn(
          modifier = Modifier.weight(1f).fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
          if (onlineHumanPilots.isEmpty()) {
            item {
              Text(
                "No other human pilots broadcasting in active sectors right now.",
                color = Color(0xFF64748B),
                fontSize = 8.sp,
                fontFamily = FontFamily.Monospace,
              )
            }
          }
          items(onlineHumanPilots, key = { it.userId.ifBlank { it.id } }) { pilot ->
            val pTag = pilot.syndicateTag.trim().ifBlank { "SOLO" }
            Column(
              modifier =
                Modifier.fillMaxWidth()
                  .clip(RoundedCornerShape(7.dp))
                  .background(Color(0xCC091426))
                  .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(7.dp))
                  .padding(6.dp),
              verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
              Text(
                text = "[$pTag] ${pilot.callsign}",
                color = Color.White,
                fontSize = 8.2.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              Text(
                text = "${pilot.sectorId} • BTY ${pilot.bounty}",
                color = Color(0xFF94A3B8),
                fontSize = 7.4.sp,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
              )
              Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(
                  modifier =
                    Modifier.weight(1f)
                      .clip(RoundedCornerShape(4.dp))
                      .background(Color(0xFF0E3A5A))
                      .clickable { onAddOnlinePilotFriend(pilot.callsign) }
                      .padding(vertical = 2.dp),
                  contentAlignment = Alignment.Center,
                ) {
                  Text("+ FRIEND", color = Color(0xFF38BDF8), fontSize = 7.2.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                }
                Box(
                  modifier =
                    Modifier.weight(1f)
                      .clip(RoundedCornerShape(4.dp))
                      .background(Color(0xFF064E3B))
                      .clickable { onInviteOnlinePilotToWing(pilot) }
                      .padding(vertical = 2.dp),
                  contentAlignment = Alignment.Center,
                ) {
                  Text("+ WING", color = Color(0xFF34D399), fontSize = 7.2.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
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
private fun OnlineRosterToggleButton(count: Int, expanded: Boolean, onToggle: () -> Unit) {
  Box(
    modifier =
      Modifier.clip(RoundedCornerShape(6.dp))
        .background(if (expanded) Color(0xFF06281E) else Color(0xFF0F172A))
        .border(1.dp, Color(0xFF10B981), RoundedCornerShape(6.dp))
        .clickable { onToggle() }
        .padding(horizontal = 7.dp, vertical = 3.dp)
        .testTag("nexus_btn_toggle_online_roster"),
  ) {
    Text(
      text = "ONLINE ($count)",
      color = Color(0xFF34D399),
      fontSize = 7.8.sp,
      fontWeight = FontWeight.Black,
      fontFamily = FontFamily.Monospace,
    )
  }
}

@Composable
private fun ActionHeaderChip(label: String, accent: Color, tag: String, onClick: () -> Unit) {
  Box(
    modifier =
      Modifier.clip(RoundedCornerShape(6.dp))
        .background(accent.copy(alpha = 0.18f))
        .border(1.dp, accent, RoundedCornerShape(6.dp))
        .clickable { onClick() }
        .padding(horizontal = 7.dp, vertical = 3.dp)
        .testTag(tag),
  ) {
    Text(label, color = Color.White, fontSize = 7.6.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
  }
}

@Composable
private fun DevArchitectBadge() {
  Box(
    modifier =
      Modifier.clip(RoundedCornerShape(4.dp))
        .background(Color(0xFFFACC15))
        .padding(horizontal = 5.dp, vertical = 1.dp),
  ) {
    Text(
      text = "DEV // ARCHITECT",
      color = Color(0xFF02050B),
      fontSize = 7.2.sp,
      fontWeight = FontWeight.Black,
      fontFamily = FontFamily.Monospace,
    )
  }
}

@Composable
private fun CloudChannelMessageCard(
  msg: SyndicateChatMessage,
  onLoadSharedBuild: (String) -> Unit,
  onAskAiAboutBuild: (String) -> Unit,
  onJoinSharedStrikeWing: (SyndicateChatMessage) -> Unit,
  onJoinCustomRoomCode: (String) -> Unit,
) {
  val isDev = RiftCommunityRoles.isDeveloperArchitect(msg.senderUid, msg.senderCallsign)
  val tag = msg.senderSyndicateTag.ifBlank { "SOLO" }
  val buildPayload = msg.parsedBuildShareRecipeJson()
  val roomCode = msg.parsedRoomCode()

  Column(
    modifier =
      Modifier.fillMaxWidth()
        .clip(RoundedCornerShape(7.dp))
        .background(if (isDev) Color(0xCC191508) else Color(0x990B1528))
        .border(
          1.dp,
          when {
            isDev -> Color(0xFFFACC15)
            buildPayload != null -> Color(0xFFC084FC)
            msg.isArmadaRecruitBanner || roomCode != null -> Color(0xFF34D399)
            else -> Color(0xFF1E293B)
          },
          RoundedCornerShape(7.dp),
        )
        .padding(horizontal = 8.dp, vertical = 6.dp),
    verticalArrangement = Arrangement.spacedBy(4.dp),
  ) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
      if (isDev) DevArchitectBadge()
      Text(
        text = "[$tag] ${msg.senderCallsign}",
        color = if (isDev) Color(0xFFFACC15) else Color(0xFF38BDF8),
        fontSize = 8.8.sp,
        fontWeight = FontWeight.ExtraBold,
        fontFamily = FontFamily.Monospace,
      )
    }
    Text(text = msg.messageText, color = Color(0xFFF1F5F9), fontSize = 9.5.sp, lineHeight = 13.sp)

    if (buildPayload != null) {
      Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        ActionHeaderChip("LOAD INTO BAY", Color(0xFFC084FC), "btn_load_build_${msg.id}") {
          onLoadSharedBuild(buildPayload)
        }
        ActionHeaderChip("ANALYZE IN #NEXUS-AI", Color(0xFF00F5D4), "btn_ai_build_${msg.id}") {
          onAskAiAboutBuild("Evaluate this shared 100-Pt Rift Frame loadout and suggest tactical matchups: ${msg.messageText}")
        }
      }
    } else if (roomCode != null) {
      ActionHeaderChip("STAGE PRIVATE RIFT [$roomCode]", Color(0xFFFACC15), "btn_join_room_${msg.id}") {
        onJoinCustomRoomCode(roomCode)
      }
    } else if (msg.isArmadaRecruitBanner && msg.armadaName.isNotBlank()) {
      ActionHeaderChip(
        "JOIN STRIKE WING [${msg.armadaName}] (${msg.armadaCurrentSize}/${msg.armadaMaxSize})",
        Color(0xFF34D399),
        "btn_join_wing_${msg.id}",
      ) {
        onJoinSharedStrikeWing(msg)
      }
    }
  }
}

@Composable
private fun ChannelCategoryHeader(title: String) {
  Text(
    title,
    color = Color(0xFF64748B),
    fontSize = 7.8.sp,
    fontWeight = FontWeight.Black,
    fontFamily = FontFamily.Monospace,
    modifier = Modifier.padding(top = 3.dp, bottom = 1.dp),
  )
}

@Composable
private fun ChannelNavItem(
  label: String,
  badge: String,
  selected: Boolean,
  accent: Color,
  onClick: () -> Unit,
  tag: String,
) {
  Row(
    modifier =
      Modifier.fillMaxWidth()
        .clip(RoundedCornerShape(6.dp))
        .background(if (selected) accent.copy(alpha = 0.20f) else Color.Transparent)
        .border(1.dp, if (selected) accent else Color.Transparent, RoundedCornerShape(6.dp))
        .clickable { onClick() }
        .padding(horizontal = 6.dp, vertical = 4.dp)
        .testTag(tag),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      label,
      color = if (selected) Color.White else Color(0xFFCBD5E1),
      fontSize = 8.8.sp,
      fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
      fontFamily = FontFamily.Monospace,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      modifier = Modifier.weight(1f),
    )
    Box(
      modifier =
        Modifier.clip(RoundedCornerShape(4.dp))
          .background(accent.copy(alpha = 0.22f))
          .padding(horizontal = 4.dp, vertical = 1.dp),
    ) {
      Text(badge, color = accent, fontSize = 7.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
    }
  }
}

@Composable
private fun SyndicateChannelManagerCard(
  syndicateTag: String,
  channels: List<String>,
  newChannelDraft: String,
  onDraftChange: (String) -> Unit,
  onAddChannel: () -> Unit,
  onDeleteChannel: (String) -> Unit,
  onClose: () -> Unit,
) {
  Column(
    modifier =
      Modifier.fillMaxWidth()
        .clip(RoundedCornerShape(8.dp))
        .background(Color(0xEE0B1528))
        .border(1.dp, Color(0xFFF59E0B), RoundedCornerShape(8.dp))
        .padding(8.dp),
    verticalArrangement = Arrangement.spacedBy(5.dp),
  ) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
      Text("[$syndicateTag] CLOUD SYNDICATE CHANNELS (${channels.size}/5)", color = Color(0xFFFACC15), fontSize = 8.8.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
      Text("CLOSE", color = Color(0xFF94A3B8), fontSize = 8.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.clickable { onClose() })
    }
    Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
      channels.forEachIndexed { idx, ch ->
        Row(
          modifier = Modifier.clip(RoundedCornerShape(5.dp)).background(Color(0xFF1E293B)).padding(horizontal = 6.dp, vertical = 3.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          Text(ch, color = Color.White, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
          if (idx > 0) {
            Text("X", color = Color(0xFFEF4444), fontSize = 8.sp, fontWeight = FontWeight.Black, modifier = Modifier.clickable { onDeleteChannel(ch) })
          }
        }
      }
    }
    if (channels.size < 5) {
      ChannelInputComposer("New channel (e.g. #war-room)…", newChannelDraft, onDraftChange, onAddChannel)
    }
  }
}

@Composable
private fun SyndicateGuestPassManagerCard(
  syndicateTag: String,
  invitedGuests: Set<String>,
  guestDraft: String,
  onDraftChange: (String) -> Unit,
  onInvite: () -> Unit,
  onRevoke: (String) -> Unit,
  onClose: () -> Unit,
) {
  Column(
    modifier =
      Modifier.fillMaxWidth()
        .clip(RoundedCornerShape(8.dp))
        .background(Color(0xEE0B1528))
        .border(1.dp, Color(0xFF38BDF8), RoundedCornerShape(8.dp))
        .padding(8.dp),
    verticalArrangement = Arrangement.spacedBy(5.dp),
  ) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
      Text("[$syndicateTag] SYNDICATE GUEST PASSES (${invitedGuests.size})", color = Color(0xFF38BDF8), fontSize = 8.8.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
      Text("CLOSE", color = Color(0xFF94A3B8), fontSize = 8.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.clickable { onClose() })
    }
    if (invitedGuests.isNotEmpty()) {
      Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        invitedGuests.forEach { guest ->
          Row(
            modifier = Modifier.clip(RoundedCornerShape(5.dp)).background(Color(0xFF1E293B)).padding(horizontal = 6.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            Text(guest, color = Color.White, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
            Text("X", color = Color(0xFFEF4444), fontSize = 8.sp, fontWeight = FontWeight.Black, modifier = Modifier.clickable { onRevoke(guest) })
          }
        }
      }
    }
    ChannelInputComposer("Grant [$syndicateTag] guest pass to callsign…", guestDraft, onDraftChange, onInvite)
  }
}

@Composable
private fun TacticalQuickPingChip(label: String, accent: Color, tag: String, onClick: () -> Unit) {
  Box(
    modifier =
      Modifier.clip(RoundedCornerShape(6.dp))
        .background(accent.copy(alpha = 0.16f))
        .border(1.dp, accent.copy(alpha = 0.7f), RoundedCornerShape(6.dp))
        .clickable { onClick() }
        .padding(horizontal = 8.dp, vertical = 3.dp)
        .testTag(tag),
  ) {
    Text(label, color = accent, fontSize = 8.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
  }
}

@Composable
private fun ChannelInputComposer(
  placeholder: String,
  value: String,
  onValueChange: (String) -> Unit,
  onSend: () -> Unit,
) {
  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
    Box(
      modifier =
        Modifier.weight(1f)
          .height(34.dp)
          .clip(RoundedCornerShape(8.dp))
          .background(Color(0xFF050B14))
          .border(1.dp, Color(0xFF38BDF8), RoundedCornerShape(8.dp))
          .padding(horizontal = 10.dp),
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
        textStyle = TextStyle(color = Color.White, fontSize = 10.sp),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
        keyboardActions = KeyboardActions(onSend = { onSend() }),
        modifier = Modifier.fillMaxWidth(),
      )
    }
    Button(
      onClick = onSend,
      enabled = value.isNotBlank(),
      border = BorderStroke(1.dp, Color(0xFF00F5D4)),
      shape = RoundedCornerShape(8.dp),
      contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
      modifier = Modifier.height(34.dp),
    ) {
      Text("SEND >", fontSize = 8.8.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
    }
  }
}

@Composable
private fun NexusChangelogChannelView() {
  Column(
    modifier =
      Modifier.fillMaxSize()
        .clip(RoundedCornerShape(8.dp))
        .background(Color(0x66030712))
        .padding(10.dp)
        .verticalScroll(rememberScrollState()),
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    Text(
      "#patch-log // NEXUS v1.0.$CURRENT_RIFT_BUILD_VERSION (BUILD #$CURRENT_RIFT_BUILD_VERSION)",
      color = Color(0xFFFACC15),
      fontSize = 10.5.sp,
      fontWeight = FontWeight.Black,
      fontFamily = FontFamily.Monospace,
    )
    RIFT_FULL_CHANGELOG.first().highlights.forEach { line ->
      Text("> $line", color = Color(0xFFE2E8F0), fontSize = 9.2.sp, lineHeight = 13.5.sp)
    }
  }
}

@Composable
private fun NexusRulesChannelView() {
  Column(
    modifier =
      Modifier.fillMaxSize()
        .clip(RoundedCornerShape(8.dp))
        .background(Color(0x66030712))
        .padding(10.dp)
        .verticalScroll(rememberScrollState()),
    verticalArrangement = Arrangement.spacedBy(7.dp),
  ) {
    Text(
      "#rules // OFFICIAL NEXUS RING SPACE DIRECTIVES & FAIR-PLAY CODE",
      color = Color(0xFFA78BFA),
      fontSize = 10.5.sp,
      fontWeight = FontWeight.Black,
      fontFamily = FontFamily.Monospace,
    )
    listOf(
      "1. HUMAN-ONLY TELEMETRY: Online pilot counters strictly count real human pilots — AI bots carry [BOT] and never inflate counts or leaderboards.",
      "2. 100% NON-PAY-TO-WIN PARITY: All 10 Hulls (including Rift Frame at full 100-Point Budget) are unlocked from 0 Resonance.",
      "3. SYNDICATE WARS ELIGIBILITY: Queuing for Ranked Syndicate Wars requires an enlisted real-player Syndicate and 100% same-Syndicate Strike Wing.",
      "4. CIRCULAR RIFT SAFE ZONE & ANTI-AFK: Active strictly in Sector Convergence and Flux Core (disabled in Rift Control and Stasis Breach).",
    ).forEach { rule -> Text(rule, color = Color(0xFFE2E8F0), fontSize = 9.2.sp, lineHeight = 13.5.sp) }
  }
}

@Composable
private fun NexusBugReportChannelView(
  selectedCategory: String,
  onSelectCategory: (String) -> Unit,
  description: String,
  onDescriptionChange: (String) -> Unit,
  statusBanner: String?,
  cloudBugCards: List<SyndicateChatMessage>,
  onSubmit: () -> Unit,
) {
  val categories =
    listOf("PHYSICS / COLLISION", "UI / HUD", "WEAPONS / BALANCE", "MULTIPLAYER / SYNC", "CONTROLS / AUDIO")
  Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
    Text(
      "#bug-reports // CLOUD SECTOR TELEMETRY & LIVE ISSUE TRACKER",
      color = Color(0xFFFB7185),
      fontSize = 10.sp,
      fontWeight = FontWeight.Black,
      fontFamily = FontFamily.Monospace,
    )
    Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
      categories.forEach { cat ->
        val sel = selectedCategory == cat
        Box(
          modifier =
            Modifier.clip(RoundedCornerShape(6.dp))
              .background(if (sel) Color(0xFF4C0519) else Color(0xFF0F172A))
              .border(1.dp, if (sel) Color(0xFFFB7185) else Color(0xFF334155), RoundedCornerShape(6.dp))
              .clickable { onSelectCategory(cat) }
              .padding(horizontal = 7.dp, vertical = 3.dp),
        ) {
          Text(cat, color = if (sel) Color.White else Color(0xFF94A3B8), fontSize = 7.6.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        }
      }
    }
    if (statusBanner != null) {
      Text(statusBanner, color = Color(0xFF34D399), fontSize = 8.5.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace)
    }
    LazyColumn(
      modifier = Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color(0x55030712)).padding(8.dp),
      verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
      if (cloudBugCards.isEmpty()) {
        item {
          Text(
            "Submit any physics, HUD, weapon, or sync anomaly below. Reports write structured telemetry to Firestore sector_bug_reports.",
            color = Color(0xFF64748B),
            fontSize = 8.8.sp,
          )
        }
      }
      items(cloudBugCards, key = { it.id }) { card ->
        val meta = card.armadaObjective.removePrefix("BUG|").replace("|", " • ")
        Column(
          modifier =
            Modifier.fillMaxWidth()
              .clip(RoundedCornerShape(6.dp))
              .background(Color(0x99190B14))
              .border(1.dp, Color(0x66FB7185), RoundedCornerShape(6.dp))
              .padding(7.dp),
          verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
          Text(
            text = "[${card.senderSyndicateTag.ifBlank { "SOLO" }}] ${card.senderCallsign} • $meta",
            color = Color(0xFFFB7185),
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
          )
          Text(text = card.messageText, color = Color(0xFFF1F5F9), fontSize = 9.sp)
        }
      }
    }
    ChannelInputComposer("Describe bug or telemetry anomaly ($selectedCategory)…", description, onDescriptionChange, onSubmit)
  }
}

@Composable
private fun NexusWelcomeTelemetryCard() {
  Column(
    modifier =
      Modifier.fillMaxWidth()
        .clip(RoundedCornerShape(10.dp))
        .background(Color(0x8C0B172A))
        .border(1.dp, Color(0x55B388FF), RoundedCornerShape(10.dp))
        .padding(10.dp),
    verticalArrangement = Arrangement.spacedBy(4.dp),
  ) {
    Text("NEXUS HUB TELEMETRY LINK ESTABLISHED", color = Color(0xFF00F5D4), fontSize = 10.5.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace)
    Text(
      "Ask NEXUS AI anything — exact weapon dimensions, Newtonian drift & bounce raycasts, 100-point Rift Frame theorycrafting, Eclipse Stasis vs Disruptor EMP, Paired Phase Rifts, or Peak Resonance records.",
      color = Color(0xFFE2E8F0),
      fontSize = 10.sp,
      lineHeight = 14.5.sp,
    )
  }
}

@Composable
private fun NexusMessageBubble(message: NexusChatMessage) {
  val isUser = message.role == NexusRole.USER
  val shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomStart = if (isUser) 12.dp else 3.dp, bottomEnd = if (isUser) 3.dp else 12.dp)
  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start) {
    Column(
      modifier =
        Modifier.widthIn(max = 560.dp)
          .clip(shape)
          .background(if (isUser) Color(0xB80E2A47) else Color(0xBF091324))
          .border(1.dp, if (isUser) Color(0x8838BDF8) else Color(0x8800F5D4), shape)
          .padding(horizontal = 10.dp, vertical = 7.dp),
      verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
          if (isUser) "PILOT QUERY" else "NEXUS // RING SPACE AI",
          color = if (isUser) Color(0xFF38BDF8) else Color(0xFF00F5D4),
          fontSize = 8.5.sp,
          fontWeight = FontWeight.ExtraBold,
          fontFamily = FontFamily.Monospace,
        )
        if (message.isStreaming) {
          Text("STREAMING TELEMETRY", color = Color(0xFFD8B4FE), fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        }
      }
      if (message.text.isEmpty() && message.isStreaming) {
        Text("Scanning Ring Space physics & sector records…", color = Color(0xFF94A3B8), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
      } else {
        val formatted = remember(message.text) { formatNexusMarkdown(message.text) }
        Text(formatted, color = Color(0xFFF1F5F9), fontSize = 10.5.sp, lineHeight = 15.sp)
      }
    }
  }
}

@Composable
private fun NexusErrorBanner(errorMessage: String, canRetry: Boolean, onRetry: () -> Unit) {
  Row(
    modifier =
      Modifier.fillMaxWidth()
        .clip(RoundedCornerShape(10.dp))
        .background(Color(0xCC2A0A12))
        .border(1.dp, Color(0xFFFF3B4E), RoundedCornerShape(10.dp))
        .padding(horizontal = 10.dp, vertical = 7.dp)
        .testTag("nexus_error_banner"),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Column(modifier = Modifier.weight(1f)) {
      Text("NEXUS LINK INTERRUPTED", color = Color(0xFFFF3B4E), fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace)
      Text(errorMessage, color = Color(0xFFFECDD3), fontSize = 9.5.sp)
    }
    if (canRetry) {
      Button(
        onClick = onRetry,
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4C0519), contentColor = Color(0xFFFECDD3)),
        border = BorderStroke(1.dp, Color(0xFFFF3B4E)),
        shape = RoundedCornerShape(7.dp),
        contentPadding = PaddingValues(horizontal = 9.dp, vertical = 3.dp),
        modifier = Modifier.height(30.dp).testTag("nexus_retry_button"),
      ) {
        Text(stringResource(R.string.nexus_retry), fontSize = 8.8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
      }
    }
  }
}

private fun formatNexusMarkdown(raw: String): AnnotatedString = buildAnnotatedString {
  var i = 0
  val n = raw.length
  while (i < n) {
    if (i + 1 < n && raw[i] == '*' && raw[i + 1] == '*') {
      val close = raw.indexOf("**", i + 2)
      if (close != -1) {
        withStyle(SpanStyle(color = Color(0xFFFFE800), fontWeight = FontWeight.Bold)) {
          append(raw.substring(i + 2, close))
        }
        i = close + 2
        continue
      }
    }
    if (raw[i] == '`') {
      val close = raw.indexOf('`', i + 1)
      if (close != -1 && close > i + 1) {
        withStyle(SpanStyle(color = Color(0xFF00F5D4), fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold)) {
          append(raw.substring(i + 1, close))
        }
        i = close + 1
        continue
      }
    }
    append(raw[i])
    i++
  }
}
