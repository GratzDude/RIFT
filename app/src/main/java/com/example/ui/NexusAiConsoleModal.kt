package com.example.ui

import android.content.Context
import android.view.KeyEvent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.R
import com.example.data.ArmadaMemberSlot
import com.example.data.CloudSyncHealth
import com.example.data.LivePilotState
import com.example.data.LocalPilotPrefs
import com.example.data.RiftBillingManager
import com.example.data.RiftCommunityRoles
import com.example.data.SyndicateChatMessage
import com.example.data.SyndicateWarRecord
import com.example.engine.CustomGameConfig
import com.example.engine.RankedGameMode
import com.example.engine.RiftCalibrationState
import com.example.engine.RiftCodex
import com.example.engine.RiftGameEngine
import com.example.engine.RiftProgressionCatalog
import com.example.engine.RiftSoundSynthesizer
import com.example.engine.RiftSpriteBaker
import com.example.engine.RiftVisualForgeState
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin


private val NEXUS_STARTER_CHIPS =
  listOf(
    "How many pixels are Suppressor's lasers?",
    "Who holds the highest Peak Resonance record?",
    "Theorycraft a 100-pt Rift Frame sniper build",
    "Analyze my Peak Resonance Run & Executioner",
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

data class OverwatchRiftInstance(
  val key: String,
  val modeTitle: String,
  val riftLabel: String,
  val rankedMode: RankedGameMode,
  val customConfig: CustomGameConfig? = null,
  val pilotCount: Int,
  val combinedResonance: Int,
  val seedSalt: Int,
)

private fun formatOverwatchResonance(value: Int): String =
  String.format(Locale.US, "%,d", value.coerceAtLeast(0))


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
fun ResonanceCrestIcon(sizeDp: Dp = 14.dp, modifier: Modifier = Modifier) {
  val visualRev = RiftVisualForgeState.visualRevision
  val bmp = remember(visualRev) { RiftSpriteBaker.resonanceIconBitmap.asImageBitmap() }
  Canvas(modifier = modifier.size(sizeDp)) {
    drawImage(
      bmp,
      dstSize = IntSize(size.width.roundToInt().coerceAtLeast(1), size.height.roundToInt().coerceAtLeast(1)),
    )
  }
}

@Composable
fun RiftShardGemIcon(sizeDp: Dp = 14.dp, modifier: Modifier = Modifier) {
  val visualRev = RiftVisualForgeState.visualRevision
  val bmp = remember(visualRev) { RiftSpriteBaker.riftShardIconBitmap.asImageBitmap() }
  Canvas(modifier = modifier.size(sizeDp)) {
    drawImage(
      bmp,
      dstSize = IntSize(size.width.roundToInt().coerceAtLeast(1), size.height.roundToInt().coerceAtLeast(1)),
    )
  }
}

/**
 * Official 3-Tier Gunmetal & Solar-Gold Winged Vortex Crest (`NEXUS` — Threshold Dynamics Studio, LLC):
 * - Symmetrical 3-tier swept dark-titanium starfighter wings (`#1E293B` / `#334155` / `#475569`)
 *   with `#FACC15` solar-gold & `#38BDF8` sky-cyan bevel highlights, dorsal crown fin, and twin lower fangs.
 * - Circular `#091326` / `#00F5D4` containment collar with 4 solar-gold compass notches cradling
 *   live-spinning `#00F5D4` / `#38BDF8` / `#A855F7` / `#EC4899` Rift vortex ribbons around a `#02040A` throat.
 */
@Composable
fun NexusMergedBrandEmblem(modifier: Modifier = Modifier, isStreaming: Boolean = false) {
  val headless = remember { isHeadlessGlEnvironment() }
  val phaseDeg =
    if (headless) {
      24f
    } else {
      val transition = rememberInfiniteTransition(label = "nexus_winged_vortex_crest")
      val animDeg by
        transition.animateFloat(
          initialValue = 0f,
          targetValue = 360f,
          animationSpec =
            infiniteRepeatable(
              tween(if (isStreaming) 1800 else 6200, easing = LinearEasing),
              RepeatMode.Restart,
            ),
          label = "crest_vortex_spin_deg",
        )
      animDeg
    }

  Canvas(modifier = modifier) {
    val w = size.width
    val h = size.height
    if (w <= 1f || h <= 1f) return@Canvas
    val cx = w * 0.5f
    val cy = h * 0.5f
    val s = min(w, h) / 68f
    fun px(dx: Float) = cx + dx * s
    fun py(dy: Float) = cy + dy * s
    val center = Offset(cx, cy)

    // Ambient singularity corona behind the 3-tier wing frame
    drawCircle(
      brush =
        Brush.radialGradient(
          colors = listOf(Color(0x4400F5D4), Color(0x30A855F7), Color.Transparent),
          center = center,
          radius = 24f * s,
        ),
      radius = 24f * s,
      center = center,
    )

    val bevelStroke = (0.9f * s).coerceAtLeast(0.8f)

    // Helper for symmetrical left/right wing polygon
    fun drawSymWing(
      pts: FloatArray, // pairs of (dxPositive, dy)
      fill: Color,
      highlightFill: Color?,
      strokeCol: Color,
    ) {
      for (dir in intArrayOf(-1, 1)) {
        val path =
          Path().apply {
            moveTo(px(dir * pts[0]), py(pts[1]))
            var k = 2
            while (k < pts.size) {
              lineTo(px(dir * pts[k]), py(pts[k + 1]))
              k += 2
            }
            close()
          }
        drawPath(path, color = fill)
        if (highlightFill != null) {
          drawPath(path, color = highlightFill.copy(alpha = 0.25f))
        }
        drawPath(
          path,
          color = strokeCol,
          style = Stroke(width = bevelStroke, join = StrokeJoin.Round),
        )
      }
    }

    // TIER 1: Upper Swept Starfighter Wing Blades (Dark Gunmetal + Solar-Gold Bevels)
    drawSymWing(
      pts = floatArrayOf(12.2f, -8.2f, 31.2f, -18.2f, 25.4f, -8.8f, 14.0f, -4.6f),
      fill = Color(0xFF1E293B),
      highlightFill = Color(0xFF334155),
      strokeCol = Color(0xFFFACC15),
    )

    // TIER 2: Mid Primary Heavy Titanium Pinions (Gunmetal + Sky-Cyan Bevels)
    drawSymWing(
      pts = floatArrayOf(14.5f, -3.6f, 29.6f, -6.6f, 25.6f, 0.4f, 14.4f, 0.8f),
      fill = Color(0xFF334155),
      highlightFill = Color(0xFF475569),
      strokeCol = Color(0xFF38BDF8),
    )

    // TIER 3: Lower Swept Stabilizer Blades (Dark Titanium + Slate Trim)
    drawSymWing(
      pts = floatArrayOf(14.0f, 1.8f, 25.2f, 3.0f, 20.2f, 8.6f, 12.0f, 5.6f),
      fill = Color(0xFF1E293B),
      highlightFill = null,
      strokeCol = Color(0xFF94A3B8),
    )

    // Twin Lower Armored Fangs Cradling the Vortex Collar
    drawSymWing(
      pts = floatArrayOf(10.6f, 7.2f, 6.6f, 20.2f, 3.4f, 11.6f, 7.2f, 8.8f),
      fill = Color(0xFF334155),
      highlightFill = null,
      strokeCol = Color(0xFFFACC15),
    )

    // Crown Dorsal Apex Fin
    val crownFin =
      Path().apply {
        moveTo(px(0f), py(-20.8f))
        lineTo(px(3.5f), py(-14.0f))
        lineTo(px(0f), py(-15.4f))
        lineTo(px(-3.5f), py(-14.0f))
        close()
      }
    drawPath(crownFin, color = Color(0xFF1E293B))
    drawPath(crownFin, color = Color(0xFFFACC15), style = Stroke(width = bevelStroke, join = StrokeJoin.Round))

    // Outer Gunmetal & Cyan Containment Collar Ring
    val collarR = 14.4f * s
    drawCircle(color = Color(0xFF091326), radius = collarR, center = center)
    drawCircle(
      color = Color(0xFF475569),
      radius = collarR,
      center = center,
      style = Stroke(width = (1.8f * s).coerceAtLeast(1.0f)),
    )
    drawCircle(
      color = Color(0xFF00F5D4),
      radius = 13.0f * s,
      center = center,
      style = Stroke(width = (1.15f * s).coerceAtLeast(0.85f)),
    )

    // 4 Solar-Gold Compass Notches
    val notchStroke = (1.45f * s).coerceAtLeast(0.95f)
    drawLine(Color(0xFFFACC15), Offset(cx, py(-14.8f)), Offset(cx, py(-11.8f)), strokeWidth = notchStroke, cap = StrokeCap.Round)
    drawLine(Color(0xFFFACC15), Offset(cx, py(11.8f)), Offset(cx, py(14.8f)), strokeWidth = notchStroke, cap = StrokeCap.Round)
    drawLine(Color(0xFFFACC15), Offset(px(-14.8f), cy), Offset(px(-11.8f), cy), strokeWidth = notchStroke, cap = StrokeCap.Round)
    drawLine(Color(0xFFFACC15), Offset(px(11.8f), cy), Offset(px(14.8f), cy), strokeWidth = notchStroke, cap = StrokeCap.Round)

    // Live-Spinning Cyan, Sky, Violet & Magenta Rift Vortex Ribbons
    val ribbonColors =
      listOf(Color(0xFF00F5D4), Color(0xFF38BDF8), Color(0xFFA855F7), Color(0xFFEC4899))
    val outerRibbonR = 10.2f * s
    val innerRibbonR = 7.8f * s
    val ribbonStroke = (1.95f * s).coerceAtLeast(1.05f)
    for (arm in 0 until 4) {
      val col = ribbonColors[arm]
      drawArc(
        color = col,
        startAngle = phaseDeg + arm * 90f,
        sweepAngle = 64f,
        useCenter = false,
        topLeft = Offset(cx - outerRibbonR, cy - outerRibbonR),
        size = Size(outerRibbonR * 2f, outerRibbonR * 2f),
        style = Stroke(width = ribbonStroke, cap = StrokeCap.Round),
      )
      drawArc(
        color = col.copy(alpha = 0.85f),
        startAngle = phaseDeg * 1.35f + arm * 90f + 34f,
        sweepAngle = 52f,
        useCenter = false,
        topLeft = Offset(cx - innerRibbonR, cy - innerRibbonR),
        size = Size(innerRibbonR * 2f, innerRibbonR * 2f),
        style = Stroke(width = ribbonStroke * 0.78f, cap = StrokeCap.Round),
      )
    }

    // Deep #02040A Singularity Event-Horizon Throat & Specular Photon Arc
    val throatR = 5.8f * s
    drawCircle(color = Color(0xFF02040A), radius = throatR, center = center)
    drawCircle(
      color = Color(0xFF00F5D4),
      radius = throatR,
      center = center,
      style = Stroke(width = (0.9f * s).coerceAtLeast(0.75f)),
    )
    drawArc(
      color = Color.White,
      startAngle = -145f + phaseDeg * 0.35f,
      sweepAngle = 82f,
      useCenter = false,
      topLeft = Offset(cx - throatR * 0.72f, cy - throatR * 0.72f),
      size = Size(throatR * 1.44f, throatR * 1.44f),
      style = Stroke(width = (0.95f * s).coerceAtLeast(0.75f), cap = StrokeCap.Round),
    )
  }
}

@Composable
fun SwirlingRiftVortexGlyph(modifier: Modifier = Modifier, isStreaming: Boolean = false) {
  NexusMergedBrandEmblem(modifier = modifier, isStreaming = isStreaming)
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
  strikeWingOnlineCount: Int = 0,
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
  isInMatch: Boolean = engine?.hasJoinedMatch == true && engine?.isEphemeralPracticeRift != true,
  onEnterPracticeRift: () -> Unit = onCloseModal,
  onOpenHowToPlayTutorial: () -> Unit = onEnterPracticeRift,
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
  isGoogleAuthenticated: Boolean = true,
  overwatchInstances: List<OverwatchRiftInstance> = emptyList(),
  selectedOverwatchKey: String = "",
  favoriteOverwatchKey: String = "",
  onSelectOverwatchKey: (String) -> Unit = {},
  onToggleFavoriteOverwatchKey: (String) -> Unit = {},
  onQuickZoneIntoOverwatch: (OverwatchRiftInstance) -> Unit = {},
  onUpdateOverwatchViewport: (cx: Float, cy: Float, radiusPx: Float) -> Unit = { _, _, _ -> },
  overwatchZoom: Float = 1.0f,
  onOverwatchTransformGesture:
    (centroidRootX: Float, centroidRootY: Float, panDxPx: Float, panDyPx: Float, zoomChange: Float) -> Unit =
    { _, _, _, _, _ -> },
  onResetOverwatchCamera: () -> Unit = {},
  onStageRankedModeViaHangar: ((RankedGameMode, Int) -> Unit)? = null,
  onStageCustomGameViaHangar: ((CustomGameConfig) -> Unit)? = null,
  onStageExistingCustomInstanceViaHangar: ((String) -> Unit)? = null,
  isNetworkOnline: Boolean = true,
  cloudSyncHealth: CloudSyncHealth = CloudSyncHealth(),
  onSwitchAccount: () -> Unit = {},
  embeddedModuleContent: (@Composable () -> Unit)? = null,
) {
  val context = LocalContext.current
  val keyboardController = LocalSoftwareKeyboardController.current
  val listState = rememberLazyListState()
  val isStreaming = streamState is NexusStreamState.Loading || streamState is NexusStreamState.Streaming
  val cleanSyndicateTag =
    if (!isGoogleAuthenticated) {
      "RFT"
    } else {
      val upper = syndicateTag.trim().uppercase(Locale.US)
      if (upper.isBlank() || upper in setOf("RFT", "GUEST", "FREE", "SOLO", "UNAFFILIATED", "NONE", "BOT")) {
        ""
      } else {
        upper
      }
    }
  val liveTrafficShipIds =
    remember(activeShipId, onlineHumanPilots, currentUserId) {
      val ids = ArrayList<Int>(onlineHumanPilots.size + 1)
      ids.add(activeShipId.coerceIn(1, 10))
      for (p in onlineHumanPilots) {
        val pUid = p.userId.ifBlank { p.id }
        if (pUid != currentUserId) {
          ids.add(p.shipId.toInt().coerceIn(1, 10))
        }
      }
      ids.toIntArray()
    }
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
  var showWarpOffConfirmModal by remember { mutableStateOf(false) }
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

  var glViewRef by remember { mutableStateOf<NexusSpaceGlView?>(null) }
  var hubApertureRootBounds by remember { mutableStateOf(Triple(0f, 0f, 0f)) }

  val resolvedOverwatchInstances =
    remember(overwatchInstances) {
      overwatchInstances.ifEmpty {
        listOf(
          OverwatchRiftInstance(
            key = "SECTOR_CONVERGENCE",
            modeTitle = "SECTOR CONVERGENCE",
            riftLabel = "RIFT #01",
            rankedMode = RankedGameMode.SECTOR_CONVERGENCE,
            customConfig = null,
            pilotCount = 0,
            combinedResonance = 1040,
            seedSalt = 101,
          )
        )
      }
    }
  val activeOverwatchInstance =
    remember(resolvedOverwatchInstances, selectedOverwatchKey) {
      resolvedOverwatchInstances.firstOrNull { it.key == selectedOverwatchKey }
        ?: resolvedOverwatchInstances.first()
    }

  LaunchedEffect(activeNode, focusedOrbitalIndex, glViewRef, liveTrafficShipIds) {
    val view = glViewRef ?: return@LaunchedEffect
    view.updateOnlinePilotShips(liveTrafficShipIds)
    view.dimFactor = 0.44f
    view.focusedTearIndex = -1
  }

  val terminalScale by
    animateFloatAsState(
      targetValue = if (activeNode == NexusCoreNode.ORBITAL_HUB) 0.94f else 1.0f,
      animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
      label = "terminal_emerge_scale",
    )
  val terminalAlpha by
    animateFloatAsState(
      targetValue = if (activeNode == NexusCoreNode.ORBITAL_HUB) 0.0f else 1.0f,
      animationSpec = tween(durationMillis = 190, easing = FastOutSlowInEasing),
      label = "terminal_emerge_alpha",
    )

  // Full-bleed container BEFORE safeDrawing:
  // On ORBITAL_HUB, masks #060D1A sci-fi tablet glass everywhere outside the exact circular Overwatch aperture!
  Box(
    modifier =
      Modifier.fillMaxSize()
        .drawBehind {
          when {
            isCalibrationPeekActive -> drawRect(Color(0x1802050B))
            activeNode == NexusCoreNode.ORBITAL_HUB -> {
              val (acx, acy, ar) = hubApertureRootBounds
              if (ar > 8f) {
                val maskPath =
                  Path().apply {
                    fillType = PathFillType.EvenOdd
                    addRect(Rect(0f, 0f, size.width, size.height))
                    addOval(Rect(acx - ar, acy - ar, acx + ar, acy + ar))
                  }
                drawPath(maskPath, color = Color(0xFF060D1A))
              } else {
                drawRect(Color(0xFF060D1A))
              }
            }
            else -> drawRect(Color(0xFF02050B))
          }
        }
        .onPreviewKeyEvent { keyEvent ->
          if (activeNode != NexusCoreNode.ORBITAL_HUB) return@onPreviewKeyEvent false
          val native = keyEvent.nativeKeyEvent
          if (native.action == KeyEvent.ACTION_DOWN && native.repeatCount == 0) {
            val totalTargets = NEXUS_3D_RIFT_TEARS.size
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
                val chosenIdx = focusedOrbitalIndex.coerceIn(0, NEXUS_3D_RIFT_TEARS.lastIndex)
                if (chosenIdx == 0) {
                  onQuickZoneIntoOverwatch(activeOverwatchInstance)
                } else {
                  onSelectNode(NEXUS_3D_RIFT_TEARS[chosenIdx].node)
                }
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
    // Sub-modal 3D backdrop (NOT mounted on ORBITAL_HUB, which opens directly onto live RiftArenaView Overwatch!)
    if (
      activeNode != NexusCoreNode.ORBITAL_HUB &&
        !isCalibrationPeekActive &&
        !isFullScreenForgeStage &&
        !isHeadlessGlEnvironment()
    ) {
      AndroidView(
        factory = { ctx ->
          NexusSpaceGlView(ctx).apply {
            isSplashMode = false
            dimFactor = 0.44f
            focusedTearIndex = -1
            updateOnlinePilotShips(liveTrafficShipIds)
            updateStationHologramTelemetry(totalOnlinePilots, unreadCommsCount)
            glViewRef = this
          }
        },
        update = { view ->
          glViewRef = view
          view.isSplashMode = false
          view.dimFactor = 0.44f
          view.updateOnlinePilotShips(liveTrafficShipIds)
          view.updateStationHologramTelemetry(totalOnlinePilots, unreadCommsCount)
        },
        modifier = Modifier.fillMaxSize(),
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
            activeShipId = activeShipId,
            activeShipSummary = activeShipSummary,
            armadaCount = armadaSlots.size.coerceAtLeast(1),
            activeArmadaName = activeArmadaName,
            syndicateWarRecord = syndicateWarRecord,
            engine = engine,
            currentUserId = currentUserId,
            totalOnlinePilots = totalOnlinePilots,
            syndicateOnlineCount = syndicateOnlineCount,
            strikeWingOnlineCount = strikeWingOnlineCount,
            unreadCommsCount = unreadCommsCount,
            isNetworkOnline = isNetworkOnline,
            isGoogleAuthenticated = isGoogleAuthenticated,
            cloudSyncHealth = cloudSyncHealth,
            bootToNexusHub = bootToNexusHub,
            isInMatch = isInMatch,
            isStreaming = isStreaming,
            focusedOrbitalIndex = focusedOrbitalIndex,
            overwatchInstances = resolvedOverwatchInstances,
            activeOverwatchInstance = activeOverwatchInstance,
            favoriteOverwatchKey = favoriteOverwatchKey,
            onSelectOverwatchKey = onSelectOverwatchKey,
            onToggleFavoriteOverwatchKey = onToggleFavoriteOverwatchKey,
            onQuickZoneIntoOverwatch = onQuickZoneIntoOverwatch,
            overwatchZoom = overwatchZoom,
            onOverwatchTransformGesture = onOverwatchTransformGesture,
            onResetOverwatchCamera = onResetOverwatchCamera,
            onApertureRootBoundsChanged = { cx, cy, r ->
              if (
                abs(hubApertureRootBounds.first - cx) > 0.5f ||
                  abs(hubApertureRootBounds.second - cy) > 0.5f ||
                  abs(hubApertureRootBounds.third - r) > 0.5f
              ) {
                hubApertureRootBounds = Triple(cx, cy, r)
              }
              onUpdateOverwatchViewport(cx, cy, r)
            },
            onToggleBootToNexusHub = onToggleBootToNexusHub,
            onLeaveMatch = {
              if (isInMatch) {
                showWarpOffConfirmModal = true
              } else {
                onLeaveMatch()
              }
            },
            onEnterPracticeRift = onEnterPracticeRift,
            onOpenHowToPlayTutorial = onOpenHowToPlayTutorial,
            onCloseModal = onCloseModal,
            onClickCallsign = {
              focusedOrbitalIndex = 6
              onSelectArchiveSubDeck(ArchiveCodexSubDeck.PILOT_DOSSIER)
              onSelectNode(NexusCoreNode.ARCHIVE_CODEX)
            },
            onClickResonance = {
              focusedOrbitalIndex = 2
              onSelectNode(NexusCoreNode.RIFT_CONDUIT)
            },
            onClickShards = {
              focusedOrbitalIndex = 2
              onSelectNode(NexusCoreNode.RIFT_CONDUIT)
            },
            onClickSyn = {
              focusedOrbitalIndex = 5
              onSelectSyndicateSubDeck(SyndicateCitadelSubDeck.SYNDICATES_WARS)
              onSelectNode(NexusCoreNode.SYNDICATE_CITADEL)
            },
            onClickWing = {
              focusedOrbitalIndex = 5
              onSelectSyndicateSubDeck(SyndicateCitadelSubDeck.STRIKE_WING)
              onSelectNode(NexusCoreNode.SYNDICATE_CITADEL)
            },
            onClickOnline = {
              focusedOrbitalIndex = 4
              onSelectNode(NexusCoreNode.SUBSPACE_COMMS)
            },
            onOpenPilotDossier = {
              focusedOrbitalIndex = 6
              onSelectArchiveSubDeck(ArchiveCodexSubDeck.PILOT_DOSSIER)
              onSelectNode(NexusCoreNode.ARCHIVE_CODEX)
            },
            onSelectNode = { idx, node ->
              focusedOrbitalIndex = idx
              onSelectNode(node)
            },
            onSwitchAccount = onSwitchAccount,
          )
        }
        else -> {
          Surface(
            modifier =
              Modifier.fillMaxWidth(0.96f)
                .fillMaxHeight(0.95f)
                .graphicsLayer {
                  scaleX = terminalScale
                  scaleY = terminalScale
                  alpha = terminalAlpha
                }
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
                onOpenCodexInHelm = onOpenCodex,
                armadaCount = armadaSlots.size.coerceAtLeast(1),
                showClearAi = activeNode == NexusCoreNode.SUBSPACE_COMMS && selectedChannelKey == "#nexus-ai" && messages.isNotEmpty(),
                isInMatch = isInMatch,
                onBackToHub = { onSelectNode(NexusCoreNode.ORBITAL_HUB) },
                onClearAi = onClearHistory,
                onLeaveMatch = {
                  if (isInMatch) {
                    showWarpOffConfirmModal = true
                  } else {
                    onLeaveMatch()
                  }
                },
                onEnterPracticeRift = onEnterPracticeRift,
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

      if (showWarpOffConfirmModal) {
        NexusWarpOffConfirmationModal(
          onStayInRift = { showWarpOffConfirmModal = false },
          onConfirmWarpOff = {
            showWarpOffConfirmModal = false
            onLeaveMatch()
          },
        )
      }
    }
  }
}

@Composable
private fun OrbitalHubSpatialStage(
  callsign: String,
  syndicateTag: String,
  liveResonance: Int,
  riftShards: Long,
  activeShipId: Int,
  @Suppress("UNUSED_PARAMETER") activeShipSummary: String,
  armadaCount: Int,
  activeArmadaName: String,
  @Suppress("UNUSED_PARAMETER") syndicateWarRecord: SyndicateWarRecord,
  engine: RiftGameEngine?,
  currentUserId: String,
  totalOnlinePilots: Int,
  syndicateOnlineCount: Int,
  strikeWingOnlineCount: Int,
  unreadCommsCount: Int,
  isNetworkOnline: Boolean = true,
  @Suppress("UNUSED_PARAMETER") bootToNexusHub: Boolean,
  isInMatch: Boolean,
  isStreaming: Boolean,
  focusedOrbitalIndex: Int,
  overwatchInstances: List<OverwatchRiftInstance>,
  activeOverwatchInstance: OverwatchRiftInstance,
  favoriteOverwatchKey: String,
  onSelectOverwatchKey: (String) -> Unit,
  onToggleFavoriteOverwatchKey: (String) -> Unit,
  onQuickZoneIntoOverwatch: (OverwatchRiftInstance) -> Unit,
  overwatchZoom: Float,
  onOverwatchTransformGesture:
    (centroidRootX: Float, centroidRootY: Float, panDxPx: Float, panDyPx: Float, zoomChange: Float) -> Unit,
  onResetOverwatchCamera: () -> Unit,
  onApertureRootBoundsChanged: (cx: Float, cy: Float, radiusPx: Float) -> Unit,
  @Suppress("UNUSED_PARAMETER") onToggleBootToNexusHub: (Boolean) -> Unit,
  onLeaveMatch: () -> Unit,
  onEnterPracticeRift: () -> Unit,
  onOpenHowToPlayTutorial: () -> Unit,
  onCloseModal: () -> Unit,
  onClickCallsign: () -> Unit,
  onClickResonance: () -> Unit,
  onClickShards: () -> Unit,
  onClickSyn: () -> Unit,
  onClickWing: () -> Unit,
  onClickOnline: () -> Unit,
  onOpenPilotDossier: () -> Unit,
  onSelectNode: (Int, NexusCoreNode) -> Unit,
  isGoogleAuthenticated: Boolean = true,
  cloudSyncHealth: CloudSyncHealth = CloudSyncHealth(),
  onSwitchAccount: () -> Unit = {},
) {
  val context = LocalContext.current
  val headless = remember { isHeadlessGlEnvironment() }
  val localPrefs = remember(context) { LocalPilotPrefs(context) }

  val shardText =
    if (RiftBillingManager.TESTER_INFINITE_SHARDS_ACTIVE) "999,999" else String.format(Locale.US, "%,d", riftShards)
  val pilotIdentityLabel =
    if (syndicateTag.isNotBlank()) "[$syndicateTag] $callsign" else callsign

  // Live per-card pilot telemetry & cached 192x192 5-stop metallic NEXUS-station module emblems
  val activeCrestRecipe = RiftVisualForgeState.activeSyndicateCrestRecipe
  val spriteRev = RiftSpriteBaker.spriteVisualRevision
  val customShipRev = RiftSpriteBaker.customShip9Version
  val hullTrim = RiftSpriteBaker.activeHullTrimStyle
  val clampedShipId = activeShipId.coerceIn(1, 10)

  val equippedSpec = remember(clampedShipId, customShipRev) { RiftCodex.getShipById(clampedShipId) }
  val cleanRoleTitle =
    remember(equippedSpec.role) {
      equippedSpec.role.substringBefore("(").trim().uppercase(Locale.US)
    }
  val hangarPrimaryStat = "${equippedSpec.name.uppercase(Locale.US)} • $cleanRoleTitle"

  val conduitTier = remember(liveResonance) { RiftProgressionCatalog.getConduitTierForResonance(liveResonance) }
  val directives =
    remember(localPrefs, currentUserId, liveResonance) {
      localPrefs.getDailyDirectives(currentUserId.ifBlank { null })
    }
  val claimableDirectives = directives.count { it.isComplete && !it.claimed }
  val conduitBadge = if (claimableDirectives > 0) "$claimableDirectives CLAIM" else "$liveResonance RES"
  val conduitPrimaryStat = "${String.format(Locale.US, "%,d", liveResonance)} RES • ${conduitTier.title}"

  val activeTrackIdx = RiftSoundSynthesizer.activeTrackSlotIndex.coerceIn(0, 2)
  val activeMusicRecipe = RiftSoundSynthesizer.trackSlots.getOrNull(activeTrackIdx)
  val trackArchetypeTitle = (activeMusicRecipe?.archetype?.label ?: "Dark Synthwave").uppercase(Locale.US)
  val trackBpm = activeMusicRecipe?.bpm ?: 128
  val forgePrimaryStat = "$trackArchetypeTitle • $trackBpm BPM"

  val commsBadge =
    when {
      unreadCommsCount > 0 -> "$unreadCommsCount NEW"
      totalOnlinePilots > 0 -> "$totalOnlinePilots LIVE"
      else -> "AI READY"
    }
  val commsPrimaryStat =
    if (unreadCommsCount > 0) {
      "$unreadCommsCount UNREAD • $totalOnlinePilots ONLINE"
    } else {
      "$totalOnlinePilots PILOTS LIVE • SUBSPACE"
    }

  val syndicateCardLabel = if (syndicateTag.isNotBlank()) "[$syndicateTag] SYNDICATE" else "SOLO PILOT"
  val wingLabel =
    if (activeArmadaName.isNotBlank()) {
      "[${activeArmadaName.take(8)}] ${armadaCount.coerceIn(1, 5)}/5"
    } else {
      "WING ${armadaCount.coerceIn(1, 5)}/5"
    }
  val citadelBadge =
    when {
      strikeWingOnlineCount > 0 -> "WING $strikeWingOnlineCount"
      syndicateTag.isNotBlank() -> "[$syndicateTag]"
      else -> null
    }
  val citadelPrimaryStat = "$syndicateCardLabel • $wingLabel"

  val peakResonance = maxOf(liveResonance, localPrefs.getPeakResonance(currentUserId.ifBlank { null }))
  val totalWins = localPrefs.getConvergencesWon()
  val archiveBadge = if (totalWins > 0L) "${totalWins}W" else null
  val archivePrimaryStat = "PEAK ${String.format(Locale.US, "%,d", peakResonance)} RES • $totalWins WINS"

  val hangarEmblem =
    remember(clampedShipId, spriteRev, customShipRev, hullTrim) {
      RiftSpriteBaker.getNexusHubModuleEmblemBitmap(
        moduleKey = "HANGAR",
        activeShipId = clampedShipId,
        sizePx = 192,
      ).asImageBitmap()
    }
  val conduitEmblem =
    remember(spriteRev) {
      RiftSpriteBaker.getNexusHubModuleEmblemBitmap(
        moduleKey = "CONDUIT",
        sizePx = 192,
      ).asImageBitmap()
    }
  val forgeEmblem =
    remember(spriteRev) {
      RiftSpriteBaker.getNexusHubModuleEmblemBitmap(
        moduleKey = "FORGE",
        sizePx = 192,
      ).asImageBitmap()
    }
  val commsEmblem =
    remember(spriteRev) {
      RiftSpriteBaker.getNexusHubModuleEmblemBitmap(
        moduleKey = "COMMS",
        sizePx = 192,
      ).asImageBitmap()
    }
  val citadelEmblem =
    remember(spriteRev, activeCrestRecipe, syndicateTag) {
      RiftSpriteBaker.getNexusHubModuleEmblemBitmap(
        moduleKey = "CITADEL",
        crestRecipe = activeCrestRecipe,
        syndicateTag = syndicateTag,
        sizePx = 192,
      ).asImageBitmap()
    }
  val archiveEmblem =
    remember(spriteRev) {
      RiftSpriteBaker.getNexusHubModuleEmblemBitmap(
        moduleKey = "ARCHIVE",
        sizePx = 192,
      ).asImageBitmap()
    }

  Box(modifier = Modifier.fillMaxSize()) {
    // Unified #060D1A Sci-Fi Command Tablet Outer Bezel Chassis
    Box(
      modifier =
        Modifier.fillMaxSize()
          .padding(horizontal = 6.dp, vertical = 4.dp)
          .clip(RoundedCornerShape(14.dp))
          .border(
            width = 1.2.dp,
            brush =
              Brush.linearGradient(
                listOf(
                  Color(0x6600F5D4),
                  Color(0x3838BDF8),
                  Color(0x2E64748B),
                  Color(0x55FACC15),
                )
              ),
            shape = RoundedCornerShape(14.dp),
          ),
    ) {
      // Precision Sci-Fi Tablet Chassis Corner Registration Brackets
      Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        if (w <= 8f || h <= 8f) return@Canvas
        val tick = 14.dp.toPx().coerceAtMost(min(w, h) * 0.08f)
        val sw = 1.6.dp.toPx()
        val cyan = Color(0xFF00F5D4).copy(alpha = 0.65f)
        val gold = Color(0xFFFACC15).copy(alpha = 0.55f)
        val pad = 3.dp.toPx()

        // Top-Left & Bottom-Left registration corners
        drawLine(cyan, Offset(pad, pad + tick), Offset(pad, pad), strokeWidth = sw)
        drawLine(cyan, Offset(pad, pad), Offset(pad + tick, pad), strokeWidth = sw)
        drawLine(cyan, Offset(pad, h - pad - tick), Offset(pad, h - pad), strokeWidth = sw)
        drawLine(cyan, Offset(pad, h - pad), Offset(pad + tick, h - pad), strokeWidth = sw)

        // Top-Right & Bottom-Right registration corners
        drawLine(gold, Offset(w - pad - tick, pad), Offset(w - pad, pad), strokeWidth = sw)
        drawLine(gold, Offset(w - pad, pad), Offset(w - pad, pad + tick), strokeWidth = sw)
        drawLine(gold, Offset(w - pad - tick, h - pad), Offset(w - pad, h - pad), strokeWidth = sw)
        drawLine(gold, Offset(w - pad, h - pad - tick), Offset(w - pad, h - pad), strokeWidth = sw)
      }

      Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp, vertical = 5.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        // Enlarged Clickable Top Sci-Fi Tablet Header (#060D1A obsidian-glass bar)
        Row(
          modifier =
            Modifier.fillMaxWidth()
              .height(44.dp)
              .clip(RoundedCornerShape(9.dp))
              .background(Color(0xF0060D1A))
              .border(
                1.dp,
                Brush.horizontalGradient(listOf(Color(0x9900F5D4), Color(0x5538BDF8), Color(0x80FACC15))),
                RoundedCornerShape(9.dp),
              )
              .padding(horizontal = 9.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            modifier =
              Modifier.weight(1f)
                .padding(end = 6.dp)
                .horizontalScroll(rememberScrollState()),
          ) {
            SwirlingRiftVortexGlyph(modifier = Modifier.size(25.dp), isStreaming = isStreaming)
            Text(
              text = "RIFT HUB",
              color = Color(0xFF00F5D4),
              fontSize = 13.0.sp,
              fontWeight = FontWeight.Black,
              fontFamily = FontFamily.Monospace,
              letterSpacing = 1.0.sp,
            )

            // Clickable Callsign -> Pilot Dossier
            Row(
              modifier =
                Modifier.clip(RoundedCornerShape(6.dp))
                  .background(Color(0xFF0A1629))
                  .border(0.9.dp, Color(0xFF38BDF8).copy(alpha = 0.55f), RoundedCornerShape(6.dp))
                  .clickable { onClickCallsign() }
                  .padding(horizontal = 7.dp, vertical = 3.5.dp)
                  .testTag("nexus_top_callsign"),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = pilotIdentityLabel,
                color = Color(0xFFF8FAFC),
                fontSize = 11.8.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
              )
            }

            // Prominent 1-Tap Switch Account Button right beside Callsign
            Box(
              modifier =
                Modifier.clip(RoundedCornerShape(6.dp))
                  .background(Color(0xFF13263D))
                  .border(1.dp, Color(0xFFFACC15).copy(alpha = 0.88f), RoundedCornerShape(6.dp))
                  .clickable { onSwitchAccount() }
                  .padding(horizontal = 8.dp, vertical = 3.5.dp)
                  .testTag("nexus_top_switch_account"),
              contentAlignment = Alignment.Center,
            ) {
              Text(
                text = "SWITCH ACCOUNT",
                color = Color(0xFFFEF08A),
                fontSize = 10.6.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
              )
            }

            // Clickable Resonance -> Ranks & Quests (Rift Conduit)
            Row(
              modifier =
                Modifier.clip(RoundedCornerShape(6.dp))
                  .background(Color(0xFF0A1629))
                  .border(0.9.dp, Color(0xFFFACC15).copy(alpha = 0.65f), RoundedCornerShape(6.dp))
                  .clickable { onClickResonance() }
                  .padding(horizontal = 7.dp, vertical = 3.5.dp)
                  .testTag("nexus_top_resonance"),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              ResonanceCrestIcon(sizeDp = 17.dp)
              Text(
                text = "$liveResonance",
                color = Color(0xFFFACC15),
                fontSize = 11.8.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
              )
            }

            // Clickable Rift Shards -> Rift Conduit
            Row(
              modifier =
                Modifier.clip(RoundedCornerShape(6.dp))
                  .background(Color(0xFF0A1629))
                  .border(0.9.dp, Color(0xFFC084FC).copy(alpha = 0.65f), RoundedCornerShape(6.dp))
                  .clickable { onClickShards() }
                  .padding(horizontal = 7.dp, vertical = 3.5.dp)
                  .testTag("nexus_top_shards"),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              RiftShardGemIcon(sizeDp = 17.dp)
              Text(
                text = shardText,
                color = Color(0xFFF0ABFC),
                fontSize = 11.8.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
              )
            }

            // Clickable SYN -> Syndicate Citadel (Syndicates)
            Box(
              modifier =
                Modifier.clip(RoundedCornerShape(6.dp))
                  .background(Color(0xFF0A1629))
                  .border(0.9.dp, Color(0xFFFACC15).copy(alpha = 0.55f), RoundedCornerShape(6.dp))
                  .clickable { onClickSyn() }
                  .padding(horizontal = 7.dp, vertical = 3.5.dp)
                  .testTag("nexus_top_syn"),
              contentAlignment = Alignment.Center,
            ) {
              Text(
                text = "SYN $syndicateOnlineCount",
                color = Color(0xFFFEF08A),
                fontSize = 11.4.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
              )
            }

            // Clickable WING -> Syndicate Citadel (Strike Wing)
            Box(
              modifier =
                Modifier.clip(RoundedCornerShape(6.dp))
                  .background(Color(0xFF0A1629))
                  .border(0.9.dp, Color(0xFF38BDF8).copy(alpha = 0.60f), RoundedCornerShape(6.dp))
                  .clickable { onClickWing() }
                  .padding(horizontal = 7.dp, vertical = 3.5.dp)
                  .testTag("nexus_top_wing"),
              contentAlignment = Alignment.Center,
            ) {
              Text(
                text = "WING $strikeWingOnlineCount",
                color = Color(0xFF7DD3FC),
                fontSize = 11.4.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
              )
            }

            // Clickable ONLINE / OFFLINE -> Chat & NEXUS AI (Subspace Comms)
            val syncHealthy = isNetworkOnline && (!isGoogleAuthenticated || cloudSyncHealth.lastError.isNullOrBlank() || cloudSyncHealth.isConnected)
            val onlinePillLabel =
              when {
                !isNetworkOnline -> "OFFLINE • LOCAL"
                !isGoogleAuthenticated -> "DRIFTER • LOCAL"
                !syncHealthy -> "SYNC RETRY • $totalOnlinePilots"
                else -> "ONLINE • $totalOnlinePilots"
              }
            val onlineAccent = if (isNetworkOnline && isGoogleAuthenticated && syncHealthy) Color(0xFF34D399) else Color(0xFFFBBF24)
            Box(
              modifier =
                Modifier.clip(RoundedCornerShape(6.dp))
                  .background(if (isNetworkOnline && isGoogleAuthenticated && syncHealthy) Color(0xFF071E1B) else Color(0xFF281906))
                  .border(
                    0.9.dp,
                    onlineAccent.copy(alpha = 0.82f),
                    RoundedCornerShape(6.dp),
                  )
                  .clickable { onClickOnline() }
                  .padding(horizontal = 7.dp, vertical = 3.5.dp)
                  .testTag("nexus_top_online"),
              contentAlignment = Alignment.Center,
            ) {
              Text(
                text = onlinePillLabel,
                color = onlineAccent,
                fontSize = 11.4.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
              )
            }
          }

          Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            // [? HOW TO PLAY] -> Routes directly to In-Arena Practice Rift 8-Step Tutorial
            Box(
              modifier =
                Modifier.clip(RoundedCornerShape(6.dp))
                  .background(Color(0xFF0B192E))
                  .border(1.dp, Color(0xFFFACC15).copy(alpha = 0.88f), RoundedCornerShape(6.dp))
                  .clickable { onOpenHowToPlayTutorial() }
                  .padding(horizontal = 10.dp, vertical = 5.5.dp)
                  .testTag("nexus_btn_how_to_play"),
              contentAlignment = Alignment.Center,
            ) {
              Text(
                text = "[? HOW TO PLAY]",
                color = Color(0xFFFACC15),
                fontSize = 10.4.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
              )
            }

            // [HELM SYSTEMS] Top Header Button (Index 7)
            val helmFocused = focusedOrbitalIndex == 7
            Box(
              modifier =
                Modifier.clip(RoundedCornerShape(6.dp))
                  .background(if (helmFocused) Color(0x3338BDF8) else Color(0xFF0B192E))
                  .border(
                    1.dp,
                    if (helmFocused) Color.White else Color(0xFF94A3B8).copy(alpha = 0.80f),
                    RoundedCornerShape(6.dp),
                  )
                  .clickable { onSelectNode(7, NexusCoreNode.HELM_SYSTEMS) }
                  .padding(horizontal = 10.dp, vertical = 5.5.dp)
                  .testTag("nexus_node_helm_systems"),
              contentAlignment = Alignment.Center,
            ) {
              Text(
                text = "[HELM SYSTEMS]",
                color = if (helmFocused) Color.White else Color(0xFFCBD5E1),
                fontSize = 10.4.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
              )
            }

            if (isInMatch) {
              Box(
                modifier =
                  Modifier.clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF280D18))
                    .border(1.dp, Color(0xFFFB7185), RoundedCornerShape(6.dp))
                    .clickable { onLeaveMatch() }
                    .padding(horizontal = 10.dp, vertical = 5.5.dp)
                    .testTag("nexus_leave_match_button"),
                contentAlignment = Alignment.Center,
              ) {
                Text(
                  text = "LEAVE MATCH",
                  color = Color(0xFFFB7185),
                  fontSize = 10.4.sp,
                  fontWeight = FontWeight.Black,
                  fontFamily = FontFamily.Monospace,
                )
              }
            }

            // [PRACTICE RIFT >] / [RETURN TO RIFT >]
            Box(
              modifier =
                Modifier.clip(RoundedCornerShape(6.dp))
                  .background(Color(0xFF082233))
                  .border(1.dp, Color(0xFF00F5D4), RoundedCornerShape(6.dp))
                  .clickable {
                    if (isInMatch) {
                      onCloseModal()
                    } else {
                      onEnterPracticeRift()
                    }
                  }
                  .padding(horizontal = 10.dp, vertical = 5.5.dp)
                  .testTag("nexus_close_button"),
              contentAlignment = Alignment.Center,
            ) {
              Text(
                text = if (isInMatch) "RETURN TO RIFT >" else "PRACTICE RIFT >",
                color = Color(0xFF00F5D4),
                fontSize = 10.6.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
              )
            }
          }
        }

        // Main Zero-Scroll Sci-Fi Command Tablet Deck:
        // Left 25% Metallic Module Cards • Center 50% Round Overwatch Aperture • Right 25% Metallic Module Cards
        Row(
          modifier = Modifier.weight(1f).fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          // LEFT SCI-FI TABLET DOCK (3 Metallic Station-Styled Module Cards)
          Column(
            modifier =
              Modifier.weight(0.25f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xBF050B16))
                .border(1.dp, Color(0x2E38BDF8), RoundedCornerShape(10.dp))
                .padding(5.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            SciFiTabletModuleCard(
              modifier = Modifier.weight(1f).fillMaxWidth(),
              title = "SHIPS & BUILDS",
              primaryStat = hangarPrimaryStat,
              emblemBitmap = hangarEmblem,
              accent = Color(0xFF38BDF8),
              secondaryAccent = Color(0xFF00F5D4),
              badgeText = "SHIP #$clampedShipId",
              isFocused = focusedOrbitalIndex == 1,
              isRightColumn = false,
              testTag = "nexus_node_hangar_deck",
              onClick = { onSelectNode(1, NexusCoreNode.HANGAR_DECK) },
            )
            SciFiTabletModuleCard(
              modifier = Modifier.weight(1f).fillMaxWidth(),
              title = "RANKS & QUESTS",
              primaryStat = conduitPrimaryStat,
              emblemBitmap = conduitEmblem,
              accent = Color(0xFFFACC15),
              secondaryAccent = Color(0xFF00F5D4),
              badgeText = conduitBadge,
              isFocused = focusedOrbitalIndex == 2,
              isRightColumn = false,
              testTag = "nexus_node_rift_conduit",
              onClick = { onSelectNode(2, NexusCoreNode.RIFT_CONDUIT) },
            )
            SciFiTabletModuleCard(
              modifier = Modifier.weight(1f).fillMaxWidth(),
              title = "SKINS & AUDIO",
              primaryStat = forgePrimaryStat,
              emblemBitmap = forgeEmblem,
              accent = Color(0xFF00F5D4),
              secondaryAccent = Color(0xFFC084FC),
              badgeText = "1:1 LAB",
              isFocused = focusedOrbitalIndex == 3,
              isRightColumn = false,
              testTag = "nexus_node_star_forge",
              onClick = { onSelectNode(3, NexusCoreNode.STAR_FORGE) },
            )
          }

          // CENTER ROUND RIPPLING DIGITIZED APERTURE (~50% width):
          val centerFocused = focusedOrbitalIndex == 0
          RoundRipplingOverwatchAperture(
            modifier = Modifier.weight(0.50f).fillMaxHeight(),
            isFocused = centerFocused,
            drawHeadlessSafeZoneFallback = headless,
            overwatchInstances = overwatchInstances,
            activeInstance = activeOverwatchInstance,
            favoriteOverwatchKey = favoriteOverwatchKey,
            overwatchZoom = overwatchZoom,
            onSelectOverwatchKey = onSelectOverwatchKey,
            onToggleFavoriteOverwatchKey = onToggleFavoriteOverwatchKey,
            onQuickZoneIntoOverwatch = onQuickZoneIntoOverwatch,
            onOverwatchTransformGesture = onOverwatchTransformGesture,
            onResetOverwatchCamera = onResetOverwatchCamera,
            onOpenAllModesGate = { onSelectNode(0, NexusCoreNode.CONVERGENCE_GATE) },
            onApertureRootBoundsChanged = onApertureRootBoundsChanged,
          )

          // RIGHT SCI-FI TABLET DOCK (3 Metallic Station-Styled Module Cards)
          Column(
            modifier =
              Modifier.weight(0.25f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xBF050B16))
                .border(1.dp, Color(0x2E38BDF8), RoundedCornerShape(10.dp))
                .padding(5.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            SciFiTabletModuleCard(
              modifier = Modifier.weight(1f).fillMaxWidth(),
              title = "CHAT & NEXUS AI",
              primaryStat = commsPrimaryStat,
              emblemBitmap = commsEmblem,
              accent = Color(0xFF00F5D4),
              secondaryAccent = Color(0xFFFACC15),
              badgeText = commsBadge,
              isFocused = focusedOrbitalIndex == 4,
              isRightColumn = true,
              testTag = "nexus_node_subspace_comms",
              onClick = { onSelectNode(4, NexusCoreNode.SUBSPACE_COMMS) },
            )
            SciFiTabletModuleCard(
              modifier = Modifier.weight(1f).fillMaxWidth(),
              title = "SYNDICATE\nSTRIKE WING",
              primaryStat = citadelPrimaryStat,
              emblemBitmap = citadelEmblem,
              accent = Color(0xFFFACC15),
              secondaryAccent = Color(0xFF38BDF8),
              badgeText = citadelBadge,
              isFocused = focusedOrbitalIndex == 5,
              isRightColumn = true,
              testTag = "nexus_node_syndicate_citadel",
              onClick = { onSelectNode(5, NexusCoreNode.SYNDICATE_CITADEL) },
            )
            SciFiTabletModuleCard(
              modifier = Modifier.weight(1f).fillMaxWidth(),
              title = "PILOT DOSSIER",
              primaryStat = archivePrimaryStat,
              emblemBitmap = archiveEmblem,
              accent = Color(0xFF34D399),
              secondaryAccent = Color(0xFF38BDF8),
              badgeText = archiveBadge,
              isFocused = focusedOrbitalIndex == 6,
              isRightColumn = true,
              testTag = "nexus_node_archive_codex",
              onClick = { onOpenPilotDossier() },
            )
          }
        }
      }
    }
  }
}

@Composable
private fun SciFiTabletModuleCard(
  modifier: Modifier,
  title: String,
  primaryStat: String,
  emblemBitmap: androidx.compose.ui.graphics.ImageBitmap,
  accent: Color,
  secondaryAccent: Color,
  badgeText: String?,
  isFocused: Boolean,
  isRightColumn: Boolean,
  testTag: String,
  onClick: () -> Unit,
) {
  val titleGlow =
    remember(accent, isFocused) {
      Shadow(
        color = if (isFocused) Color.White.copy(alpha = 0.65f) else accent.copy(alpha = 0.45f),
        offset = Offset.Zero,
        blurRadius = 6f,
      )
    }
  val phaseSeedRad = remember(title) { ((abs(title.hashCode()) % 360) * (PI / 180.0)).toFloat() }
  val isTwoLineTitle = remember(title) { title.contains('\n') }

  Box(
    modifier =
      modifier
        .minimumInteractiveComponentSize()
        .clip(RoundedCornerShape(9.dp))
        .background(
          Brush.verticalGradient(
            colors =
              if (isFocused) {
                listOf(Color(0xF50B1C36), Color(0xF0060D1A))
              } else {
                listOf(Color(0xF2060D1A), Color(0xEA081428))
              }
          )
        )
        .border(
          width = if (isFocused) 1.6.dp else 1.1.dp,
          brush =
            Brush.horizontalGradient(
              if (isRightColumn) {
                listOf(secondaryAccent.copy(alpha = 0.68f), accent.copy(alpha = if (isFocused) 1f else 0.85f))
              } else {
                listOf(accent.copy(alpha = if (isFocused) 1f else 0.85f), secondaryAccent.copy(alpha = 0.68f))
              }
            ),
          shape = RoundedCornerShape(9.dp),
        )
        .clickable { onClick() }
        .testTag(testTag),
  ) {
    // Chamfered Tactical Corner Brackets + Inner Digitizing Aperture Coupling Bar + Instrument Divider
    Canvas(modifier = Modifier.fillMaxSize()) {
      val w = size.width
      val h = size.height
      if (w <= 4f || h <= 4f) return@Canvas
      val brk = 10.dp.toPx().coerceAtMost(min(w, h) * 0.24f)
      val sw = 1.6.dp.toPx()
      val bracketCol = if (isFocused) Color.White else accent
      val goldCorner = Color(0xFFFACC15).copy(alpha = 0.82f)

      // Top-left chamfer bracket
      drawLine(bracketCol, Offset(2f, 2f + brk), Offset(2f, 2f), strokeWidth = sw)
      drawLine(bracketCol, Offset(2f, 2f), Offset(2f + brk, 2f), strokeWidth = sw)
      // Bottom-right chamfer bracket
      drawLine(goldCorner, Offset(w - 2f, h - 2f - brk), Offset(w - 2f, h - 2f), strokeWidth = sw)
      drawLine(goldCorner, Offset(w - 2f - brk, h - 2f), Offset(w - 2f, h - 2f), strokeWidth = sw)

      // Subtle mid-deck instrument horizon line separating Top Graphic Bay from Bottom Title + Primary Stat
      val midY = h * 0.54f
      drawLine(
        brush =
          Brush.horizontalGradient(
            listOf(
              Color.Transparent,
              accent.copy(alpha = 0.28f),
              secondaryAccent.copy(alpha = 0.24f),
              Color.Transparent,
            )
          ),
        start = Offset(w * 0.08f, midY),
        end = Offset(w * 0.92f, midY),
        strokeWidth = 1.dp.toPx(),
      )

      // Inner Overwatch-facing digital telemetry rail
      val innerX = if (isRightColumn) 1.6f else (w - 1.6f)
      drawLine(
        brush = Brush.verticalGradient(listOf(Color.Transparent, accent, Color.Transparent)),
        start = Offset(innerX, h * 0.14f),
        end = Offset(innerX, h * 0.86f),
        strokeWidth = 2.4.dp.toPx(),
        cap = StrokeCap.Round,
      )
    }

    Column(
      modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp, vertical = 4.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.SpaceBetween,
    ) {
      // TOP SLOT (~54% card height): Slow-Moving Animated Instrument Readout framing the Metallic Emblem + Status Pill
      Box(
        modifier =
          Modifier.weight(0.54f)
            .fillMaxWidth()
            .heightIn(min = 38.dp, max = 66.dp),
        contentAlignment = Alignment.Center,
      ) {
        HubMetallicEmblemPedestal(
          emblemBitmap = emblemBitmap,
          contentDescription = title.replace('\n', ' '),
          accent = accent,
          secondaryAccent = secondaryAccent,
          phaseSeedRad = phaseSeedRad,
        )

        if (badgeText != null) {
          Box(
            modifier =
              Modifier.align(if (isRightColumn) Alignment.TopStart else Alignment.TopEnd)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xD9071326))
                .border(0.9.dp, secondaryAccent.copy(alpha = 0.82f), RoundedCornerShape(4.dp))
                .padding(horizontal = 4.5.dp, vertical = 1.dp),
          ) {
            Text(
              text = badgeText,
              color = secondaryAccent,
              fontSize = 8.2.sp,
              fontWeight = FontWeight.Black,
              fontFamily = FontFamily.Monospace,
              maxLines = 1,
              softWrap = false,
              style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
            )
          }
        }
      }

      // BOTTOM SLOT (~46% card height): Bold Module Title (1 or 2 Lines) + Live Primary Telemetry Only
      Column(
        modifier = Modifier.weight(0.46f).fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly,
      ) {
        Text(
          text = title,
          color = Color.White,
          fontSize = if (isTwoLineTitle) 11.2.sp else 12.6.sp,
          lineHeight = if (isTwoLineTitle) 12.6.sp else 14.0.sp,
          fontWeight = FontWeight.Black,
          fontFamily = FontFamily.Monospace,
          maxLines = if (isTwoLineTitle) 2 else 1,
          softWrap = isTwoLineTitle,
          overflow = TextOverflow.Ellipsis,
          textAlign = TextAlign.Center,
          style =
            TextStyle(
              shadow = titleGlow,
              platformStyle = PlatformTextStyle(includeFontPadding = false),
            ),
          modifier = Modifier.fillMaxWidth(),
        )

        Text(
          text = primaryStat,
          color = accent,
          fontSize = 10.2.sp,
          fontWeight = FontWeight.ExtraBold,
          fontFamily = FontFamily.Monospace,
          maxLines = 1,
          softWrap = false,
          overflow = TextOverflow.Ellipsis,
          textAlign = TextAlign.Center,
          style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
          modifier = Modifier.fillMaxWidth(),
        )
      }
    }
  }
}

@Composable
private fun HubMetallicEmblemPedestal(
  emblemBitmap: androidx.compose.ui.graphics.ImageBitmap,
  contentDescription: String,
  accent: Color,
  secondaryAccent: Color,
  phaseSeedRad: Float,
) {
  val headless = remember { isHeadlessGlEnvironment() }
  val instrumentPhaseRad =
    if (headless) {
      0.55f + phaseSeedRad
    } else {
      val transition = rememberInfiniteTransition(label = "hub_module_instrument")
      val animPhase by
        transition.animateFloat(
          initialValue = 0f,
          targetValue = (2.0 * PI).toFloat(),
          animationSpec =
            infiniteRepeatable(
              animation = tween(durationMillis = 16000, easing = LinearEasing),
              repeatMode = RepeatMode.Restart,
            ),
          label = "hub_instrument_sweep",
        )
      animPhase + phaseSeedRad
    }

  Box(
    modifier = Modifier.fillMaxHeight().aspectRatio(1f),
    contentAlignment = Alignment.Center,
  ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val minDim = size.minDimension
      if (minDim <= 6f) return@Canvas
      val cx = center.x
      val cy = center.y
      val outerR = minDim * 0.48f
      val pulse = 0.88f + 0.12f * sin(instrumentPhaseRad * 2f)

      // 1. Subtle radial instrument glow
      drawCircle(
        brush =
          Brush.radialGradient(
            colors =
              listOf(
                accent.copy(alpha = 0.24f * pulse),
                secondaryAccent.copy(alpha = 0.10f * pulse),
                Color.Transparent,
              ),
            center = center,
            radius = outerR * 1.05f,
          ),
        radius = outerR * 1.05f,
        center = center,
      )

      // 2. Calibrated 24-tick instrument dial ring
      val tickInnerMajor = outerR * 0.87f
      val tickInnerMinor = outerR * 0.92f
      for (idx in 0 until 24) {
        val ang = idx * ((2.0 * PI) / 24.0).toFloat() + instrumentPhaseRad * 0.25f
        val isMajor = idx % 6 == 0
        val r0 = if (isMajor) tickInnerMajor else tickInnerMinor
        drawLine(
          color =
            if (isMajor) accent.copy(alpha = 0.62f)
            else secondaryAccent.copy(alpha = 0.28f),
          start = Offset(cx + cos(ang) * r0, cy + sin(ang) * r0),
          end = Offset(cx + cos(ang) * outerR, cy + sin(ang) * outerR),
          strokeWidth = if (isMajor) 1.3.dp.toPx() else 0.8.dp.toPx(),
          cap = StrokeCap.Round,
        )
      }

      // 3. Slow-moving orbital sweep arc + counter-rotating secondary harmonic arc
      val sweepDeg = (instrumentPhaseRad * (180f / PI.toFloat())) % 360f
      val arcSize = Size(outerR * 2f, outerR * 2f)
      val arcTopLeft = Offset(cx - outerR, cy - outerR)
      drawArc(
        color = accent.copy(alpha = 0.78f),
        startAngle = sweepDeg,
        sweepAngle = 56f,
        useCenter = false,
        topLeft = arcTopLeft,
        size = arcSize,
        style = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round),
      )
      val innerReticleR = outerR * 0.81f
      drawArc(
        color = secondaryAccent.copy(alpha = 0.56f),
        startAngle = (-sweepDeg * 0.75f) + 140f,
        sweepAngle = 42f,
        useCenter = false,
        topLeft = Offset(cx - innerReticleR, cy - innerReticleR),
        size = Size(innerReticleR * 2f, innerReticleR * 2f),
        style = Stroke(width = 1.1.dp.toPx(), cap = StrokeCap.Round),
      )

      // Leading orbital telemetry pip
      val leadRad = (sweepDeg + 56f) * (PI.toFloat() / 180f)
      drawCircle(
        color = Color.White.copy(alpha = 0.90f),
        radius = 1.8.dp.toPx(),
        center = Offset(cx + cos(leadRad) * outerR, cy + sin(leadRad) * outerR),
      )
    }

    // Slightly inset metallic emblem so the animated instrument reticle & tick ring read cleanly around it
    Image(
      bitmap = emblemBitmap,
      contentDescription = contentDescription,
      modifier = Modifier.fillMaxSize(0.76f),
      contentScale = ContentScale.Fit,
    )
  }
}

/**
 * Round Rippling Dynamic-Lit Digitized Aperture (`ORBITAL_HUB` Center Column):
 * 1. Masks `#060D1A` sci-fi tablet glass outside an exact circular aperture so `RiftArenaView` renders a
 *    live top-down Full-Playfield Orbital Overwatch match (`100%` of `arenaRadius` visible inside the circle).
 * 2. Concentric polar voxel/matrix rings (`0.96r .. 1.22r`) ripple inward (`sin(band - wt)`) from solid
 *    `#060D1A` tablet glass into live space, lit by directional cyan/gold rim-lighting reacting to Combined Resonance.
 * 3. Top Compact Switcher Pill (`<` • `Game Type • RIFT #XX • Combined RES` • `[FAV]` • `>`) +
 *    Bottom Compact Action Row (`[ RIFT IN // RIFT #XX ]` + `[ ALL MODES ]`).
 */
@Composable
private fun RoundRipplingOverwatchAperture(
  modifier: Modifier,
  isFocused: Boolean,
  drawHeadlessSafeZoneFallback: Boolean,
  overwatchInstances: List<OverwatchRiftInstance>,
  activeInstance: OverwatchRiftInstance,
  favoriteOverwatchKey: String,
  overwatchZoom: Float,
  onSelectOverwatchKey: (String) -> Unit,
  onToggleFavoriteOverwatchKey: (String) -> Unit,
  onQuickZoneIntoOverwatch: (OverwatchRiftInstance) -> Unit,
  onOverwatchTransformGesture:
    (centroidRootX: Float, centroidRootY: Float, panDxPx: Float, panDyPx: Float, zoomChange: Float) -> Unit,
  onResetOverwatchCamera: () -> Unit,
  onOpenAllModesGate: () -> Unit,
  onApertureRootBoundsChanged: (cx: Float, cy: Float, radiusPx: Float) -> Unit,
) {
  val headless = remember { isHeadlessGlEnvironment() }
  val density = LocalDensity.current
  val ripplePhase =
    if (headless) {
      0.45f
    } else {
      val transition = rememberInfiniteTransition(label = "round_rippling_aperture")
      val p by
        transition.animateFloat(
          initialValue = 0f,
          targetValue = (2.0 * PI).toFloat(),
          animationSpec = infiniteRepeatable(tween(4600, easing = LinearEasing), RepeatMode.Restart),
          label = "round_ripple_rad",
        )
      p
    }

  val totalInstances = overwatchInstances.size.coerceAtLeast(1)
  val currentIdx =
    remember(overwatchInstances, activeInstance.key) {
      overwatchInstances.indexOfFirst { it.key == activeInstance.key }.let { if (it < 0) 0 else it }
    }
  val isFavorite = favoriteOverwatchKey.isNotBlank() && favoriteOverwatchKey == activeInstance.key
  val energyNorm = (activeInstance.combinedResonance / 1600f).coerceIn(0.22f, 1.0f)

  var apertureContainerSize by remember { mutableStateOf(IntSize.Zero) }
  var gestureSurfaceRootOrigin by remember { mutableStateOf(Offset.Zero) }
  val gestureCircleDiameterDp =
    with(density) {
      val minDim = min(apertureContainerSize.width, apertureContainerSize.height).toFloat()
      val rPx = if (minDim > 8f) (minDim * 0.41f).coerceAtLeast(48f) else 120.dp.toPx()
      (rPx * 2f).toDp()
    }

  Box(
    modifier =
      modifier.onGloballyPositioned { coords ->
        apertureContainerSize = coords.size
        val rootPos = coords.positionInRoot()
        val w = coords.size.width.toFloat()
        val h = coords.size.height.toFloat()
        if (w > 8f && h > 8f) {
          val cx = rootPos.x + w * 0.5f
          val cy = rootPos.y + h * 0.5f
          val radius = (min(w, h) * 0.41f).coerceAtLeast(48f)
          onApertureRootBoundsChanged(cx, cy, radius)
        }
      },
  ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val w = size.width
      val h = size.height
      if (w <= 8f || h <= 8f) return@Canvas
      val cx = w * 0.5f
      val cy = h * 0.5f
      val center = Offset(cx, cy)
      val apertureR = (min(w, h) * 0.41f).coerceAtLeast(48f)

      // 1. Mask #060D1A tablet glass everywhere outside the exact circular Overwatch aperture
      val outerMask =
        Path().apply {
          fillType = PathFillType.EvenOdd
          addRect(Rect(0f, 0f, w, h))
          addOval(Rect(cx - apertureR, cy - apertureR, cx + apertureR, cy + apertureR))
        }
      drawPath(outerMask, color = Color(0xFF060D1A))

      // Headless JVM/Robolectric Full-Playfield Orbital Overwatch Schematic Fallback
      if (drawHeadlessSafeZoneFallback) {
        drawCircle(color = Color(0xFF030917), radius = apertureR, center = center)
        val stormR = apertureR * 0.90f
        drawCircle(
          color = Color(0xFF00F5D4).copy(alpha = 0.52f),
          radius = stormR,
          center = center,
          style = Stroke(width = 2.dp.toPx()),
        )
        val safeR = stormR * 0.16f
        for (b in 0 until 8) {
          val ang = (b * PI * 0.25).toFloat()
          val innerPt = Offset(cx + cos(ang) * (safeR * 0.32f), cy + sin(ang) * (safeR * 0.32f))
          val outerPt = Offset(cx + cos(ang) * safeR, cy + sin(ang) * safeR)
          drawLine(
            brush = Brush.linearGradient(listOf(Color(0xFFFACC15), Color(0xFF00F5D4)), start = innerPt, end = outerPt),
            start = innerPt,
            end = outerPt,
            strokeWidth = 1.6.dp.toPx(),
          )
        }
        drawCircle(color = Color(0xFF334155), radius = safeR * 0.42f, center = center)
        drawCircle(
          color = Color(0xFF00F5D4),
          radius = safeR * 0.20f,
          center = center,
          style = Stroke(width = 1.3.dp.toPx()),
        )
      }

      // 2. Concentric Polar Voxel / Matrix Rings Rippling Inward (sin(band - wt)) + Dynamic Directional Lighting
      val numBands = 7
      val numSegs = 48
      val segSweepDeg = 360f / numSegs.toFloat()
      val segGapDeg = 1.35f
      val lightDirRad = ripplePhase * 0.75f
      val secondaryLightRad = -ripplePhase * 0.5f + 2.1f
      val borderSw = 0.9.dp.toPx()

      for (band in 0 until numBands) {
        // band 0 = outermost (#060D1A solid tablet rim) -> band 6 = innermost (dissolving into live Overwatch space)
        val outerFrac = 1.22f - band * 0.038f
        val innerFrac = outerFrac - 0.033f
        val rMid = apertureR * ((outerFrac + innerFrac) * 0.5f)
        val bandThickness = (apertureR * (outerFrac - innerFrac)).coerceAtLeast(1.6f)
        val arcRect = Rect(cx - rMid, cy - rMid, cx + rMid, cy + rMid)

        val keepThreshold =
          when (band) {
            0 -> 1.00f
            1 -> 0.92f
            2 -> 0.78f
            3 -> 0.60f
            4 -> 0.40f
            5 -> 0.22f
            else -> 0.10f
          }
        val baseTabletAlpha =
          when (band) {
            0 -> 0.95f
            1 -> 0.78f
            2 -> 0.56f
            3 -> 0.34f
            4 -> 0.18f
            5 -> 0.09f
            else -> 0.04f
          }

        for (seg in 0 until numSegs) {
          val hash = ((band * 73856093) xor (seg * 19349663)) and 0xFF
          val normHash = hash / 255f
          // Inward-propagating polar ripple: as ripplePhase advances, crest travels from band 0 -> band 6 (inward!)
          val segAngleRad = ((seg + 0.5f) / numSegs.toFloat()) * (2f * PI.toFloat())
          val inwardWave = 0.5f + 0.5f * sin(band * 0.95f - ripplePhase * 3.0f + sin(segAngleRad * 3f) * 0.55f)
          if (normHash > keepThreshold + inwardWave * 0.12f) continue

          val startDeg = seg * segSweepDeg + segGapDeg * 0.5f
          val sweepDeg = (segSweepDeg - segGapDeg).coerceAtLeast(1.2f)

          // Dissolving #060D1A polar matrix cell fill
          val fillAlpha = (baseTabletAlpha * (0.76f + 0.24f * inwardWave)).coerceIn(0f, 0.96f)
          if (fillAlpha > 0.02f) {
            drawArc(
              color = Color(0xFF060D1A).copy(alpha = fillAlpha),
              startAngle = startDeg,
              sweepAngle = sweepDeg,
              useCenter = false,
              topLeft = arcRect.topLeft,
              size = arcRect.size,
              style = Stroke(width = bandThickness, cap = StrokeCap.Butt),
            )
          }

          // Dynamic Directional Cyan / Solar-Gold Rim-Lighting on Polar Voxel Cells
          val primaryLit = (0.5f + 0.5f * cos(segAngleRad - lightDirRad)).pow(2.2f)
          val goldLit = (0.5f + 0.5f * cos(segAngleRad - secondaryLightRad)).pow(2.6f)
          val highlightStrength =
            ((0.18f + 0.56f * primaryLit + 0.38f * goldLit) * inwardWave * (0.55f + 0.45f * energyNorm))
              .coerceIn(0f, 0.88f)

          if (band >= 1 && ((hash and 3) == 0 || inwardWave > 0.72f)) {
            val litColor =
              when {
                goldLit > primaryLit * 1.05f -> Color(0xFFFACC15)
                (hash and 4) == 0 -> Color(0xFF00F5D4)
                else -> Color(0xFF38BDF8)
              }
            drawArc(
              color = litColor.copy(alpha = highlightStrength * (1f - band * 0.08f).coerceAtLeast(0.28f)),
              startAngle = startDeg,
              sweepAngle = sweepDeg,
              useCenter = false,
              topLeft = arcRect.topLeft,
              size = arcRect.size,
              style = Stroke(width = if (band >= 4) borderSw * 1.25f else borderSw, cap = StrokeCap.Butt),
            )
          }
        }
      }

      // 3. Dynamic-Lit Circular Aperture Bezel Rings + Azimuth Rangefinder Ticks
      rotate(degrees = (lightDirRad * 180f / PI.toFloat())) {
        drawCircle(
          brush =
            Brush.sweepGradient(
              0.00f to Color(0xFF00F5D4).copy(alpha = if (isFocused) 0.95f else 0.78f),
              0.22f to Color(0xFF38BDF8).copy(alpha = 0.42f),
              0.50f to Color(0xFFFACC15).copy(alpha = 0.82f),
              0.78f to Color(0xFF1E293B).copy(alpha = 0.45f),
              1.00f to Color(0xFF00F5D4).copy(alpha = if (isFocused) 0.95f else 0.78f),
              center = center,
            ),
          radius = apertureR,
          center = center,
          style = Stroke(width = if (isFocused) 2.2.dp.toPx() else 1.6.dp.toPx()),
        )
      }
      drawCircle(
        color = Color(0xFF38BDF8).copy(alpha = 0.28f + 0.16f * energyNorm),
        radius = apertureR * 1.055f,
        center = center,
        style = Stroke(width = 0.9.dp.toPx()),
      )

      // 12 Precision Radial Rangefinder Notches Around the Circular Overwatch Rim
      val tickStroke = 1.2.dp.toPx()
      for (t in 0 until 12) {
        val ang = (t * PI / 6.0).toFloat()
        val isCardinal = t % 3 == 0
        val r0 = apertureR * (if (isCardinal) 0.968f else 0.982f)
        val r1 = apertureR * (if (isCardinal) 1.032f else 1.016f)
        val col =
          if (isCardinal) Color(0xFFFACC15).copy(alpha = 0.78f)
          else Color(0xFF00F5D4).copy(alpha = 0.45f)
        drawLine(
          color = col,
          start = Offset(cx + cos(ang) * r0, cy + sin(ang) * r0),
          end = Offset(cx + cos(ang) * r1, cy + sin(ang) * r1),
          strokeWidth = tickStroke,
          cap = StrokeCap.Round,
        )
      }
    }

    // INTERACTIVE CIRCULAR OVERWATCH TOUCH SURFACE:
    // 2-Finger Pinch-to-Zoom (1.0x .. 6.0x anchored to centroid), 1-Finger Drag Pan, & Double-Tap Reset
    Box(
      modifier =
        Modifier.align(Alignment.Center)
          .size(gestureCircleDiameterDp)
          .clip(CircleShape)
          .onGloballyPositioned { coords ->
            gestureSurfaceRootOrigin = coords.positionInRoot()
          }
          .pointerInput(activeInstance.key) {
            detectTapGestures(
              onDoubleTap = { onResetOverwatchCamera() },
            )
          }
          .pointerInput(activeInstance.key) {
            detectTransformGestures { centroid, pan, zoom, _ ->
              onOverwatchTransformGesture(
                gestureSurfaceRootOrigin.x + centroid.x,
                gestureSurfaceRootOrigin.y + centroid.y,
                pan.x,
                pan.y,
                zoom,
              )
            }
          }
          .testTag("nexus_overwatch_gesture_surface"),
    )

    // TOP COMPACT OVERWATCH INSTANCE SWITCHER PILL + ZOOM RESET BADGE:
    // [ < ]  GAME TYPE • RIFT #XX • 1,420 RES  [ FAV ]  [ > ]
    Column(
      modifier =
        Modifier.align(Alignment.TopCenter)
          .padding(top = 4.dp, start = 8.dp, end = 8.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      Row(
        modifier =
          Modifier.clip(RoundedCornerShape(9.dp))
            .background(Color(0xEB060D1A))
            .border(
              1.dp,
              Brush.horizontalGradient(listOf(Color(0xAA00F5D4), Color(0x8838BDF8), Color(0xAAFACC15))),
              RoundedCornerShape(9.dp),
            )
            .padding(horizontal = 8.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        // Previous RIFT # Instance
        Box(
          modifier =
            Modifier.clip(RoundedCornerShape(5.dp))
              .background(Color(0xFF0B192E))
              .border(0.9.dp, Color(0xFF38BDF8).copy(alpha = 0.80f), RoundedCornerShape(5.dp))
              .clickable {
                val prevIdx = if (currentIdx <= 0) totalInstances - 1 else currentIdx - 1
                onSelectOverwatchKey(overwatchInstances[prevIdx].key)
              }
              .padding(horizontal = 8.dp, vertical = 3.5.dp)
              .testTag("nexus_overwatch_prev"),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = "<",
            color = Color(0xFF00F5D4),
            fontSize = 11.0.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
          )
        }

        // Active Match Mode + RIFT # + Combined Pilot Resonance
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
          Text(
            text = "${activeInstance.modeTitle} • ${activeInstance.riftLabel}",
            color = Color.White,
            fontSize = 9.8.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Text(
            text = "•",
            color = Color(0xFF64748B),
            fontSize = 9.6.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
          )
          ResonanceCrestIcon(sizeDp = 14.dp)
          Text(
            text = "${formatOverwatchResonance(activeInstance.combinedResonance)} RES",
            color = Color(0xFFFACC15),
            fontSize = 9.8.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
          )
          if (activeInstance.pilotCount > 0) {
            Text(
              text = "• ${activeInstance.pilotCount}P",
              color = Color(0xFF34D399),
              fontSize = 9.2.sp,
              fontWeight = FontWeight.Black,
              fontFamily = FontFamily.Monospace,
              maxLines = 1,
            )
          }
        }

        // [ FAV ] Pin Toggle
        Box(
          modifier =
            Modifier.clip(RoundedCornerShape(5.dp))
              .background(if (isFavorite) Color(0x33FACC15) else Color(0xFF0B192E))
              .border(
                0.9.dp,
                if (isFavorite) Color(0xFFFACC15) else Color(0xFF64748B).copy(alpha = 0.85f),
                RoundedCornerShape(5.dp),
              )
              .clickable { onToggleFavoriteOverwatchKey(activeInstance.key) }
              .padding(horizontal = 7.dp, vertical = 3.5.dp)
              .testTag("nexus_overwatch_fav_toggle"),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = if (isFavorite) "[FAV *]" else "[FAV]",
            color = if (isFavorite) Color(0xFFFACC15) else Color(0xFFCBD5E1),
            fontSize = 9.0.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
          )
        }

        // Next RIFT # Instance
        Box(
          modifier =
            Modifier.clip(RoundedCornerShape(5.dp))
              .background(Color(0xFF0B192E))
              .border(0.9.dp, Color(0xFF38BDF8).copy(alpha = 0.80f), RoundedCornerShape(5.dp))
              .clickable {
                val nextIdx = (currentIdx + 1) % totalInstances
                onSelectOverwatchKey(overwatchInstances[nextIdx].key)
              }
              .padding(horizontal = 8.dp, vertical = 3.5.dp)
              .testTag("nexus_overwatch_next"),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = ">",
            color = Color(0xFF00F5D4),
            fontSize = 11.0.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
          )
        }
      }

      if (overwatchZoom > 1.03f) {
        Box(
          modifier =
            Modifier.clip(RoundedCornerShape(6.dp))
              .background(Color(0xEB061428))
              .border(0.9.dp, Color(0xFF00F5D4), RoundedCornerShape(6.dp))
              .clickable { onResetOverwatchCamera() }
              .padding(horizontal = 9.dp, vertical = 3.5.dp)
              .testTag("nexus_overwatch_zoom_reset"),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = "${String.format(Locale.US, "%.1fx", overwatchZoom)} • [1.0x]",
            color = Color(0xFF00F5D4),
            fontSize = 9.0.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
          )
        }
      }
    }

    // BOTTOM COMPACT ACTION ROW:
    // 1-Click [ RIFT IN // RIFT #XX ] + [ ALL MODES ] (CONVERGENCE GATE)
    val quickZoneGlow =
      remember(isFocused) {
        Shadow(
          color = if (isFocused) Color.White else Color(0xFF00F5D4),
          offset = Offset.Zero,
          blurRadius = 10f,
        )
      }
    Row(
      modifier =
        Modifier.align(Alignment.BottomCenter)
          .padding(bottom = 4.dp, start = 8.dp, end = 8.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Box(
        modifier =
          Modifier.clip(RoundedCornerShape(9.dp))
            .background(
              Brush.horizontalGradient(
                listOf(Color(0xF0061428), Color(0xF50A223D), Color(0xF0061428))
              )
            )
            .border(
              width = if (isFocused) 1.5.dp else 1.1.dp,
              brush =
                Brush.horizontalGradient(
                  listOf(Color(0xEE00F5D4), Color(0xCCFACC15), Color(0xEE38BDF8))
                ),
              shape = RoundedCornerShape(9.dp),
            )
            .clickable { onQuickZoneIntoOverwatch(activeInstance) }
            .padding(horizontal = 15.dp, vertical = 7.dp)
            .testTag("nexus_btn_quick_zone_in"),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          text = "[ RIFT IN // ${activeInstance.riftLabel} ]",
          color = Color.White,
          fontSize = 11.6.sp,
          fontWeight = FontWeight.Black,
          fontFamily = FontFamily.Monospace,
          letterSpacing = 0.8.sp,
          maxLines = 1,
          style = TextStyle(shadow = quickZoneGlow),
        )
      }

      Box(
        modifier =
          Modifier.clip(RoundedCornerShape(9.dp))
            .background(Color(0xEB071224))
            .border(1.dp, Color(0xFF38BDF8).copy(alpha = 0.88f), RoundedCornerShape(9.dp))
            .clickable { onOpenAllModesGate() }
            .padding(horizontal = 12.dp, vertical = 7.dp)
            .testTag("nexus_node_convergence_gate"),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          text = "[ ALL MODES ]",
          color = Color(0xFF38BDF8),
          fontSize = 9.8.sp,
          fontWeight = FontWeight.Black,
          fontFamily = FontFamily.Monospace,
          letterSpacing = 0.6.sp,
          maxLines = 1,
        )
      }
    }
  }
}

/**
 * Explicit `#060D1A` Sci-Fi Tablet Warp-Off Confirmation Dialog triggered strictly when intentionally
 * tapping `LEAVE MATCH` from an active Rift instance (never shown when browsing Hub modules or `[? HOW TO PLAY]`).
 */
@Composable
private fun NexusWarpOffConfirmationModal(
  onStayInRift: () -> Unit,
  onConfirmWarpOff: () -> Unit,
) {
  Box(
    modifier =
      Modifier.fillMaxSize()
        .background(Color(0xCC02050B))
        .clickable(onClick = onStayInRift)
        .testTag("nexus_warp_off_confirm_modal"),
    contentAlignment = Alignment.Center,
  ) {
    Surface(
      modifier =
        Modifier.widthIn(min = 300.dp, max = 460.dp)
          .fillMaxWidth(0.76f)
          .clickable(enabled = false) {}
          .border(
            1.4.dp,
            Brush.linearGradient(listOf(Color(0xFFFB7185), Color(0xFFFACC15), Color(0xFF00F5D4))),
            RoundedCornerShape(14.dp),
          ),
      color = Color(0xF8060D1A),
      shape = RoundedCornerShape(14.dp),
    ) {
      Column(
        modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          SwirlingRiftVortexGlyph(modifier = Modifier.size(22.dp))
          Text(
            text = "CONFIRM WARP-OFF // LEAVE ACTIVE RIFT",
            color = Color(0xFFFB7185),
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 0.6.sp,
          )
        }

        Text(
          text = "Sure you want to warp off from your active Rift match and return to peaceful NEXUS orbit?",
          color = Color(0xFFE2E8F0),
          fontSize = 10.5.sp,
          lineHeight = 14.5.sp,
          fontWeight = FontWeight.SemiBold,
          fontFamily = FontFamily.SansSerif,
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Box(
            modifier =
              Modifier.minimumInteractiveComponentSize()
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF0A2438))
                .border(1.dp, Color(0xFF00F5D4), RoundedCornerShape(8.dp))
                .clickable { onStayInRift() }
                .padding(horizontal = 14.dp, vertical = 7.dp)
                .testTag("warp_off_stay_button"),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text = "[STAY IN RIFT]",
              color = Color(0xFF00F5D4),
              fontSize = 10.sp,
              fontWeight = FontWeight.Black,
              fontFamily = FontFamily.Monospace,
            )
          }

          Box(
            modifier =
              Modifier.minimumInteractiveComponentSize()
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF310E1B))
                .border(1.2.dp, Color(0xFFFB7185), RoundedCornerShape(8.dp))
                .clickable { onConfirmWarpOff() }
                .padding(horizontal = 14.dp, vertical = 7.dp)
                .testTag("warp_off_confirm_button"),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text = "[WARP OFF]",
              color = Color(0xFFFFB4C0),
              fontSize = 10.sp,
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
private fun GlassTerminalHeaderGlyph(node: NexusCoreNode, accent: Color, sizeDp: Int = 22) {
  if (node == NexusCoreNode.SUBSPACE_COMMS || node == NexusCoreNode.ORBITAL_HUB) {
    NexusMergedBrandEmblem(modifier = Modifier.size(sizeDp.dp))
    return
  }
  Canvas(modifier = Modifier.size(sizeDp.dp)) {
    val w = size.width
    val h = size.height
    if (w <= 1f || h <= 1f) return@Canvas
    val cx = w * 0.5f
    val cy = h * 0.5f
    val r = min(w, h) * 0.42f
    for (arm in 0 until 3) {
      drawArc(
        color = accent.copy(alpha = 0.88f),
        startAngle = arm * 120f + 16f,
        sweepAngle = 78f,
        useCenter = false,
        topLeft = Offset(cx - r, cy - r),
        size = Size(r * 2f, r * 2f),
        style = Stroke(width = (r * 0.24f).coerceAtLeast(1.4f), cap = StrokeCap.Round),
      )
    }
    drawCircle(Color.Black, radius = r * 0.62f, center = Offset(cx, cy))
    drawCircle(Color.White.copy(alpha = 0.85f), radius = r * 0.64f, center = Offset(cx, cy), style = Stroke(width = 1.0f))
  }
}

@Composable
private fun GlassTerminalTopHeader(
  activeNode: NexusCoreNode,
  syndicateSubDeck: SyndicateCitadelSubDeck,
  onSelectSyndicateSubDeck: (SyndicateCitadelSubDeck) -> Unit,
  @Suppress("UNUSED_PARAMETER") archiveSubDeck: ArchiveCodexSubDeck,
  @Suppress("UNUSED_PARAMETER") onSelectArchiveSubDeck: (ArchiveCodexSubDeck) -> Unit,
  onOpenCodexInHelm: () -> Unit = {},
  armadaCount: Int,
  showClearAi: Boolean,
  isInMatch: Boolean,
  onBackToHub: () -> Unit,
  onClearAi: () -> Unit,
  onLeaveMatch: () -> Unit,
  onEnterPracticeRift: () -> Unit,
  onCloseToArena: () -> Unit,
) {
  val (title, accent) =
    when (activeNode) {
      NexusCoreNode.SUBSPACE_COMMS -> "SUBSPACE COMMS & NEXUS AI" to Color(0xFF00F5D4)
      NexusCoreNode.CONVERGENCE_GATE -> "CONVERGENCE GATE" to Color(0xFF00F5D4)
      NexusCoreNode.HANGAR_DECK -> "HANGAR DECK" to Color(0xFF38BDF8)
      NexusCoreNode.RIFT_CONDUIT -> "RIFT CONDUIT" to Color(0xFFE879F9)
      NexusCoreNode.SYNDICATE_CITADEL -> "SYNDICATE CITADEL" to Color(0xFFFACC15)
      NexusCoreNode.ARCHIVE_CODEX -> "PILOT DOSSIER" to Color(0xFF34D399)
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
          label = "HELM CODEX >",
          selected = false,
          accent = Color(0xFF34D399),
          tag = "archive_sub_codex",
          onClick = onOpenCodexInHelm,
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
            .clickable {
              if (isInMatch) {
                onCloseToArena()
              } else {
                onEnterPracticeRift()
              }
            }
            .padding(horizontal = 10.dp, vertical = 4.dp)
            .testTag("nexus_close_button"),
      ) {
        Text(
          text = if (isInMatch) "RETURN TO RIFT >" else "PRACTICE RIFT >",
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
      RankedGameMode.SECTOR_CONVERGENCE -> "20-Pilot FFA / 1-5 Strike Wing • Nexus Station Safe Zone • +100 Shards #1 Resonance."
      RankedGameMode.RIFT_SKIRMISH -> "Best-of-3 Team Rounds • Opposite Warp Rifts • Locks Hull on Hangar Entry."
      RankedGameMode.FLUX_CORE -> "20-Pilot Carrier FFA • +1 Overcharge Tier / 5s • 30s Safe-Zone Eject."
      RankedGameMode.STASIS_BREACH -> "10v10 Stasis Jailbreak • No Safe Zone • Destroy 8,000 Flux Jail Core to free allies."
      RankedGameMode.RIFT_CONTROL -> "Dual-Rift Tug-of-War • No Safe Zone • Hold uncontested 30s to lock Rifts."
      RankedGameMode.SYNDICATE_WARS -> "[$syndicateTag] Syndicate Ladder • ${syndicateWarRecord.warTrophies} War Trophies (${syndicateWarRecord.warWins}W-${syndicateWarRecord.warLosses}L)."
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
  val strikeWingKey = "strike:${activeArmadaName.ifBlank { "WING" }.uppercase(Locale.US)}"

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
            badge = if (idx == 0) "CORE" else "SYN",
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
            items(items = messages, key = { it.id }) { msg -> NexusAiMessageCard(message = msg) }
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
              isAnnouncements -> "#announcements // ARCHITECT & NEXUS SECTOR DISPATCH"
              isShipwright -> "#shipwright-meta // 100-PT RIFT FRAME LOADOUT EXCHANGE"
              isLfg -> "#strike-lfg // STRIKE WING & PRIVATE RIFT RECRUITMENT"
              isSyndicateCh -> "[$cleanSyndicateTag] ${selectedChannelKey.substringAfterLast(':')} // $syndicateName"
              isStrikeWingCh -> "#strike-wing // [${activeArmadaName.ifBlank { "STRIKE WING" }}] ENCRYPTED COMMS"
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
                    Text("ARCHITECT // BUILD #$CURRENT_RIFT_BUILD_VERSION RIFT DISPATCH", color = Color(0xFFFACC15), fontSize = 9.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                  }
                  Text(
                    "Welcome to RIFT (Threshold Dynamics Studio, LLC). Use the Rift Hub Overwatch radar to inspect active matches and Rift In, or open any module deck around the aperture. All 10 ships operate at fair 100-Point Parity from 0 Resonance — share Rift Frame builds in #shipwright-meta, form Strike Wings in #strike-lfg, and post feedback in #bug-reports.",
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
              TacticalQuickPingChip("FOCUS RIFT LEADER", Color(0xFFFB7185), "nexus_quick_ping_attack_hvt") {
                onSendToChannel(selectedChannelKey, "[TACTICAL PING] CONVERGE ON HIGH-RESONANCE TARGET NOW!")
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
              val pilotRes = if (pilot.liveResonance > 0L) pilot.liveResonance else pilot.bounty
              Text(
                text = "${pilot.sectorId} • $pilotRes RES",
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
      "#rules // OFFICIAL NEXUS SECTOR DIRECTIVES & FAIR-PLAY CODE",
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
    Text("NEXUS TELEMETRY LINK ESTABLISHED", color = Color(0xFF00F5D4), fontSize = 10.5.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace)
    Text(
      "Ask NEXUS AI anything — exact weapon dimensions, Newtonian drift & bounce raycasts, 100-point Rift Frame theorycrafting, Eclipse Stasis vs Disruptor EMP, Paired Phase Rifts, or Peak Resonance records.",
      color = Color(0xFFE2E8F0),
      fontSize = 10.sp,
      lineHeight = 14.5.sp,
    )
  }
}

@Composable
private fun NexusAiMessageCard(message: NexusChatMessage) {
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
          if (isUser) "PILOT QUERY" else "NEXUS // TACTICAL AI",
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
        Text("Scanning NEXUS sector physics & records…", color = Color(0xFF94A3B8), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
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
