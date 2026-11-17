package com.example.ui

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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.pointer.pointerInput
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.R
import com.example.data.RiftBillingManager
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

/**
 * Signature Merged Tri-Concept `NEXUS` Brand Symbol (Threshold Dynamics Studio, LLC):
 * 1. Glowing multicolored swirling Singularity core at center (`#00F5D4`, `#A855F7`, `#FACC15`).
 * 2. Interlocking 3D-tilted Ring-Space gyro arcs (`+32°` cyan & `-32°` solar-gold/violet) looping
 *    behind and in front of the monogram for true 3D orbital depth.
 * 3. Two sleek swept starfighter wing-blades (`#F8FAFC` -> `#38BDF8` -> `#0284C7`) forming a
 *    geometric **`N`** monogram with a blazing cyan-to-gold diagonal energy slash.
 */
@Composable
fun NexusMergedBrandEmblem(modifier: Modifier = Modifier, isStreaming: Boolean = false) {
  val headless = remember { isHeadlessGlEnvironment() }
  val phaseDeg =
    if (headless) {
      28f
    } else {
      val transition = rememberInfiniteTransition(label = "nexus_merged_emblem")
      val animDeg by
        transition.animateFloat(
          initialValue = 0f,
          targetValue = 360f,
          animationSpec =
            infiniteRepeatable(
              tween(if (isStreaming) 1800 else 6400, easing = LinearEasing),
              RepeatMode.Restart,
            ),
          label = "emblem_spin_deg",
        )
      animDeg
    }

  Canvas(modifier = modifier) {
    val w = size.width
    val h = size.height
    if (w <= 1f || h <= 1f) return@Canvas
    val cx = w * 0.5f
    val cy = h * 0.5f
    val r = min(w, h) * 0.46f
    val center = Offset(cx, cy)

    // 1. Ambient Swirling Singularity Corona + Deep Singularity Heart
    drawCircle(
      brush =
        Brush.radialGradient(
          colors =
            listOf(
              Color(0x6600F5D4),
              Color(0x44A855F7),
              Color(0x00020611),
            ),
          center = center,
          radius = r * 1.06f,
        ),
      radius = r * 1.06f,
      center = center,
    )
    drawCircle(color = Color(0xFF030814), radius = r * 0.54f, center = center)

    val swirlColors = listOf(Color(0xFF00F5D4), Color(0xFF38BDF8), Color(0xFFA855F7), Color(0xFFFACC15))
    val swirlStroke = (r * 0.11f).coerceAtLeast(1.1f)
    for (arm in 0 until 4) {
      drawArc(
        color = swirlColors[arm],
        startAngle = phaseDeg + arm * 90f,
        sweepAngle = 58f,
        useCenter = false,
        topLeft = Offset(cx - r * 0.44f, cy - r * 0.44f),
        size = Size(r * 0.88f, r * 0.88f),
        style = Stroke(width = swirlStroke, cap = StrokeCap.Round),
      )
    }

    // 2. Back-Halves of Interlocking 3D-Tilted Ring-Space Gyro Ellipses (behind the Winged-N)
    val gyroRx = r * 0.96f
    val gyroRy = r * 0.36f
    val gyroTopLeft = Offset(cx - gyroRx, cy - gyroRy)
    val gyroSize = Size(gyroRx * 2f, gyroRy * 2f)
    val gyroStroke = (r * 0.105f).coerceAtLeast(1.15f)

    rotate(degrees = -32f, pivot = center) {
      drawArc(
        color = Color(0xAA00F5D4),
        startAngle = 180f,
        sweepAngle = 180f,
        useCenter = false,
        topLeft = gyroTopLeft,
        size = gyroSize,
        style = Stroke(width = gyroStroke, cap = StrokeCap.Round),
      )
    }
    rotate(degrees = 32f, pivot = center) {
      drawArc(
        color = Color(0xAAFACC15),
        startAngle = 180f,
        sweepAngle = 180f,
        useCenter = false,
        topLeft = gyroTopLeft,
        size = gyroSize,
        style = Stroke(width = gyroStroke * 0.92f, cap = StrokeCap.Round),
      )
    }

    // 3. Swept Starfighter Wing-Blades Forming Stylized Geometric 'N' Monogram
    val wingBrushLeft =
      Brush.linearGradient(
        colors = listOf(Color(0xFFF8FAFC), Color(0xFF38BDF8), Color(0xFF0284C7)),
        start = Offset(cx - r * 0.56f, cy - r * 0.78f),
        end = Offset(cx - r * 0.24f, cy + r * 0.78f),
      )
    val wingBrushRight =
      Brush.linearGradient(
        colors = listOf(Color(0xFF0284C7), Color(0xFF38BDF8), Color(0xFFF8FAFC)),
        start = Offset(cx + r * 0.24f, cy - r * 0.78f),
        end = Offset(cx + r * 0.56f, cy + r * 0.78f),
      )

    // Left swept starfighter wing-blade (vertical-swept left leg of 'N')
    val leftWing =
      Path().apply {
        moveTo(cx - r * 0.28f, cy - r * 0.76f)
        lineTo(cx - r * 0.58f, cy - r * 0.34f)
        lineTo(cx - r * 0.46f, cy + r * 0.76f)
        lineTo(cx - r * 0.24f, cy + r * 0.44f)
        close()
      }
    // Right swept starfighter wing-blade (vertical-swept right leg of 'N')
    val rightWing =
      Path().apply {
        moveTo(cx + r * 0.46f, cy - r * 0.76f)
        lineTo(cx + r * 0.24f, cy - r * 0.44f)
        lineTo(cx + r * 0.28f, cy + r * 0.76f)
        lineTo(cx + r * 0.58f, cy + r * 0.34f)
        close()
      }
    // Diagonal energy bridge connecting top-left wing apex across the Singularity to bottom-right wing apex
    val diagSlash =
      Path().apply {
        moveTo(cx - r * 0.32f, cy - r * 0.68f)
        lineTo(cx - r * 0.14f, cy - r * 0.68f)
        lineTo(cx + r * 0.32f, cy + r * 0.68f)
        lineTo(cx + r * 0.14f, cy + r * 0.68f)
        close()
      }

    drawPath(leftWing, brush = wingBrushLeft)
    drawPath(rightWing, brush = wingBrushRight)
    drawPath(
      diagSlash,
      brush =
        Brush.linearGradient(
          colors = listOf(Color(0xFF00F5D4), Color.White, Color(0xFFFACC15)),
          start = Offset(cx - r * 0.30f, cy - r * 0.68f),
          end = Offset(cx + r * 0.30f, cy + r * 0.68f),
        ),
    )

    val bevelStroke = (r * 0.055f).coerceAtLeast(0.85f)
    drawPath(leftWing, color = Color.White.copy(alpha = 0.82f), style = Stroke(width = bevelStroke, join = StrokeJoin.Round))
    drawPath(rightWing, color = Color(0xFF00F5D4).copy(alpha = 0.88f), style = Stroke(width = bevelStroke, join = StrokeJoin.Round))

    // Glowing Singularity Pinpoint at the center of the N slash
    drawCircle(color = Color(0xFF00F5D4).copy(alpha = 0.55f), radius = r * 0.20f, center = center)
    drawCircle(color = Color.White, radius = (r * 0.10f).coerceAtLeast(1.1f), center = center)

    // 4. Front-Halves of Interlocking 3D-Tilted Ring-Space Gyro Ellipses (crossing in front of Winged-N)
    rotate(degrees = -32f, pivot = center) {
      drawArc(
        brush = Brush.horizontalGradient(listOf(Color(0xFF00F5D4), Color.White, Color(0xFF38BDF8))),
        startAngle = 10f,
        sweepAngle = 160f,
        useCenter = false,
        topLeft = gyroTopLeft,
        size = gyroSize,
        style = Stroke(width = gyroStroke * 1.08f, cap = StrokeCap.Round),
      )
    }
    rotate(degrees = 32f, pivot = center) {
      drawArc(
        brush = Brush.horizontalGradient(listOf(Color(0xFFA855F7), Color(0xFFFACC15), Color.White)),
        startAngle = 12f,
        sweepAngle = 156f,
        useCenter = false,
        topLeft = gyroTopLeft,
        size = gyroSize,
        style = Stroke(width = gyroStroke * 0.96f, cap = StrokeCap.Round),
      )
    }
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
  isGoogleAuthenticated: Boolean = true,
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
  LaunchedEffect(activeNode, focusedOrbitalIndex, glViewRef, liveTrafficShipIds) {
    val view = glViewRef ?: return@LaunchedEffect
    view.updateOnlinePilotShips(liveTrafficShipIds)
    if (activeNode == NexusCoreNode.ORBITAL_HUB) {
      view.dimFactor = 1.0f
      view.focusedTearIndex = focusedOrbitalIndex
      view.resetCameraDive()
    } else {
      view.dimFactor = 0.44f
      view.focusedTearIndex = -1
    }
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

  // Full-bleed #02050B container BEFORE safeDrawing so zero Arena entities bleed around screen edges
  Box(
    modifier =
      Modifier.fillMaxSize()
        .background(if (isCalibrationPeekActive) Color(0x1802050B) else Color(0xFF02050B))
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
                glViewRef?.triggerDiveIntoTear(chosenIdx)
                onSelectNode(NEXUS_3D_RIFT_TEARS[chosenIdx].node)
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
    // Native OpenGL ES 3.0 3D NEXUS Station + Live Real-Ship Traffic + 7 Outward-Bleeding 3D Rifts
    if (!isCalibrationPeekActive && !isFullScreenForgeStage && !isHeadlessGlEnvironment()) {
      AndroidView(
        factory = { ctx ->
          NexusSpaceGlView(ctx).apply {
            isSplashMode = false
            dimFactor = if (activeNode == NexusCoreNode.ORBITAL_HUB) 1.0f else 0.44f
            focusedTearIndex = if (activeNode == NexusCoreNode.ORBITAL_HUB) focusedOrbitalIndex else -1
            updateOnlinePilotShips(liveTrafficShipIds)
            glViewRef = this
          }
        },
        update = { view ->
          glViewRef = view
          view.isSplashMode = false
          view.dimFactor = if (activeNode == NexusCoreNode.ORBITAL_HUB) 1.0f else 0.44f
          view.updateOnlinePilotShips(liveTrafficShipIds)
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
            totalOnlinePilots = totalOnlinePilots,
            unreadCommsCount = unreadCommsCount,
            bootToNexusHub = bootToNexusHub,
            isInMatch = engine?.hasJoinedMatch == true,
            isStreaming = isStreaming,
            focusedOrbitalIndex = focusedOrbitalIndex,
            glViewRef = glViewRef,
            onToggleBootToNexusHub = onToggleBootToNexusHub,
            onLeaveMatch = onLeaveMatch,
            onCloseModal = onCloseModal,
            onSelectNode = { tearIdx, node ->
              glViewRef?.triggerDiveIntoTear(tearIdx)
              onSelectNode(node)
            },
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
private fun OrbitalHubSpatialStage(
  callsign: String,
  syndicateTag: String,
  liveResonance: Int,
  riftShards: Long,
  totalOnlinePilots: Int,
  unreadCommsCount: Int,
  @Suppress("UNUSED_PARAMETER") bootToNexusHub: Boolean,
  isInMatch: Boolean,
  isStreaming: Boolean,
  focusedOrbitalIndex: Int,
  glViewRef: NexusSpaceGlView?,
  @Suppress("UNUSED_PARAMETER") onToggleBootToNexusHub: (Boolean) -> Unit,
  onLeaveMatch: () -> Unit,
  onCloseModal: () -> Unit,
  onSelectNode: (Int, NexusCoreNode) -> Unit,
) {
  val density = LocalDensity.current
  var stageOriginInRoot by remember { mutableStateOf(Offset.Zero) }
  var stageSizePx by remember { mutableStateOf(IntSize.Zero) }
  var projectedStates by remember { mutableStateOf<List<ProjectedRiftTearScreenState>>(emptyList()) }

  val visualRev = RiftVisualForgeState.visualRevision
  val resonanceBmp = remember(visualRev) { RiftSpriteBaker.resonanceIconBitmap.asImageBitmap() }
  val shardBmp = remember(visualRev) { RiftSpriteBaker.riftShardIconBitmap.asImageBitmap() }
  val shardText =
    if (RiftBillingManager.TESTER_INFINITE_SHARDS_ACTIVE) "999,999" else riftShards.toString()
  val pilotIdentityLabel =
    if (syndicateTag.isNotBlank()) "• [$syndicateTag] $callsign" else "• $callsign"

  LaunchedEffect(glViewRef, stageSizePx) {
    if (isHeadlessGlEnvironment() || glViewRef == null) {
      projectedStates =
        if (stageSizePx.width > 0 && stageSizePx.height > 0) {
          computeStaticProjectedTearStates(stageSizePx.width.toFloat(), stageSizePx.height.toFloat())
        } else {
          computeStaticProjectedTearStates(1280f, 720f)
        }
      return@LaunchedEffect
    }
    while (true) {
      withFrameNanos {
        val glW = glViewRef.width
        val glH = glViewRef.height
        projectedStates =
          if (glW > 0 && glH > 0) {
            glViewRef.computeProjectedTearStates(glW.toFloat(), glH.toFloat())
          } else if (stageSizePx.width > 0 && stageSizePx.height > 0) {
            computeStaticProjectedTearStates(stageSizePx.width.toFloat(), stageSizePx.height.toFloat())
          } else {
            emptyList()
          }
      }
    }
  }

  Column(modifier = Modifier.fillMaxSize()) {
    // Sleek frameless translucent horizon telemetry strip (no STARTUP toggle; custom Resonance & Shard icons)
    Box(
      modifier =
        Modifier.fillMaxWidth()
          .background(
            Brush.verticalGradient(
              colors = listOf(Color(0xDC010308), Color(0x88010308), Color.Transparent),
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
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          modifier = Modifier.weight(1f),
        ) {
          SwirlingRiftVortexGlyph(modifier = Modifier.size(24.dp), isStreaming = isStreaming)
          Text(
            text = "NEXUS",
            color = Color(0xFF00F5D4),
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.2.sp,
          )
          Text(
            text = "• $totalOnlinePilots PILOTS ONLINE",
            color = Color(0xFF34D399),
            fontSize = 8.8.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = FontFamily.Monospace,
          )
          Text(
            text = pilotIdentityLabel,
            color = Color(0xFFE2E8F0),
            fontSize = 8.8.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Text(
            text = "•",
            color = Color(0xFF64748B),
            fontSize = 8.8.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
          )
          // Custom Solar-Gold & Cyan Harmonic Winged Tuning Crest (replaces plain "RES " text)
          Canvas(modifier = Modifier.size(14.dp)) {
            drawImage(
              resonanceBmp,
              dstSize = IntSize(size.width.roundToInt().coerceAtLeast(1), size.height.roundToInt().coerceAtLeast(1)),
            )
          }
          Text(
            text = "$liveResonance",
            color = Color(0xFFFACC15),
            fontSize = 8.8.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = FontFamily.Monospace,
          )
          Text(
            text = "•",
            color = Color(0xFF64748B),
            fontSize = 8.8.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
          )
          // Single Jewel-Grade 3D Faceted Prismatic Shard Gem (zero duplicate ❖ glyph)
          Canvas(modifier = Modifier.size(14.dp)) {
            drawImage(
              shardBmp,
              dstSize = IntSize(size.width.roundToInt().coerceAtLeast(1), size.height.roundToInt().coerceAtLeast(1)),
            )
          }
          Text(
            text = shardText,
            color = Color(0xFFF0ABFC),
            fontSize = 8.8.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
          )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
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

    // 3D NEXUS Stage: Central Rotating 3D Space Station (idx 0) + 7 Round Outward-Bleeding 3D Rifts (idx 1..7)
    Box(
      modifier =
        Modifier.weight(1f)
          .fillMaxWidth()
          .onGloballyPositioned { coords ->
            stageOriginInRoot = coords.positionInRoot()
            stageSizePx = coords.size
          }
          .pointerInput(glViewRef) {
            detectDragGestures(
              onDragEnd = { glViewRef?.releaseCameraLookSpring() },
              onDragCancel = { glViewRef?.releaseCameraLookSpring() },
            ) { change, dragAmount ->
              change.consume()
              glViewRef?.addCameraLookDelta(dragAmount.x, dragAmount.y)
            }
          }
    ) {
      val activeProjections =
        if (projectedStates.size == NEXUS_3D_RIFT_TEARS.size) {
          projectedStates
        } else if (stageSizePx.width > 0 && stageSizePx.height > 0) {
          computeStaticProjectedTearStates(stageSizePx.width.toFloat(), stageSizePx.height.toFloat())
        } else {
          computeStaticProjectedTearStates(1280f, 720f)
        }

      val usingGlRootCoords = glViewRef != null && glViewRef.width > 0 && glViewRef.height > 0

      NEXUS_3D_RIFT_TEARS.forEachIndexed { idx, spec ->
        val proj = activeProjections.getOrNull(idx) ?: return@forEachIndexed
        if (!proj.visible && stageSizePx.width > 0) return@forEachIndexed

        val localCenterX =
          if (usingGlRootCoords) proj.screenCenterX - stageOriginInRoot.x else proj.screenCenterX
        val localCenterY =
          if (usingGlRootCoords) proj.screenCenterY - stageOriginInRoot.y else proj.screenCenterY

        val minHitPx = with(density) { 48.dp.toPx() }
        val hitW = proj.projectedWidthPx.coerceAtLeast(minHitPx * 2.1f)
        val hitH = proj.projectedHeightPx.coerceAtLeast(minHitPx * 1.15f)
        val hitWdp = with(density) { hitW.toDp() }
        val hitHdp = with(density) { hitH.toDp() }

        val stageW = stageSizePx.width.toFloat().coerceAtLeast(hitW)
        val stageH = stageSizePx.height.toFloat().coerceAtLeast(hitH)
        val offsetX = (localCenterX - hitW * 0.5f).coerceIn(0f, (stageW - hitW).coerceAtLeast(0f))
        val offsetY = (localCenterY - hitH * 0.5f).coerceIn(0f, (stageH - hitH).coerceAtLeast(0f))

        val isPrimaryNexus = idx == 0
        val isFocused = focusedOrbitalIndex == idx
        val primaryGlow =
          remember(spec.accentColor, isFocused, isPrimaryNexus) {
            Shadow(
              color = if (isPrimaryNexus) Color(0xEE010308) else if (isFocused) Color.White else spec.accentColor.copy(alpha = 0.85f),
              offset = Offset.Zero,
              blurRadius = if (isPrimaryNexus) 12f else 7f,
            )
          }

        // Frameless diegetic hit target over the Central 3D Station (idx 0) or inside the 7 #000000 Rift cores (idx 1..7)
        Box(
          modifier =
            Modifier.minimumInteractiveComponentSize()
              .width(hitWdp)
              .height(hitHdp)
              .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
              .clickable { onSelectNode(idx, spec.node) }
              .testTag(spec.testTag),
          contentAlignment = Alignment.Center,
        ) {
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier =
              if (isPrimaryNexus) {
                // Subtle frameless radial shadow veil over central reactor core so station text pops crisply
                Modifier.background(
                  Brush.radialGradient(
                    colors = listOf(Color(0xB8010308), Color(0x66010308), Color.Transparent),
                  ),
                  CircleShape,
                ).padding(horizontal = 12.dp, vertical = 6.dp)
              } else {
                Modifier.padding(horizontal = 6.dp)
              },
          ) {
            if (isPrimaryNexus) {
              val primaryLabel =
                if (unreadCommsCount > 0) "NEXUS • $unreadCommsCount" else "NEXUS"
              Text(
                text = primaryLabel,
                color = if (unreadCommsCount > 0) Color(0xFFFACC15) else Color.White,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.6.sp,
                maxLines = 1,
                textAlign = TextAlign.Center,
                style = TextStyle(shadow = primaryGlow),
              )
              Text(
                text = "COMMS & AI • $totalOnlinePilots ONLINE",
                color = if (isFocused) Color.White else Color(0xFF00F5D4),
                fontSize = 7.4.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 0.4.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                style = TextStyle(shadow = Shadow(color = Color.Black, offset = Offset.Zero, blurRadius = 8f)),
              )
            } else {
              Text(
                text = spec.title,
                color = Color.White,
                fontSize = 8.8.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 0.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                style = TextStyle(shadow = primaryGlow),
              )
              Text(
                text = spec.subtitle,
                color = if (isFocused) Color.White else spec.accentColor,
                fontSize = 6.8.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
              )
            }
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
                    Text("ARCHITECT // BUILD #$CURRENT_RIFT_BUILD_VERSION NEXUS DISPATCH", color = Color(0xFFFACC15), fontSize = 9.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                  }
                  Text(
                    "Welcome to NEXUS (Threshold Dynamics Studio, LLC). Drag across 3D volumetric space to look around and dive into the central NEXUS Tear or any of the 7 crackling Spacetime Rift Tears. All 10 Hulls operate at 100-Point Parity from 0 Resonance — share Rift Frame builds in #shipwright-meta, form Strike Wings in #strike-lfg, and log live telemetry in #bug-reports.",
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
