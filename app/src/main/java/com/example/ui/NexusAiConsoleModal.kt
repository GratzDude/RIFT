package com.example.ui

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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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
import com.example.data.SyndicateChatMessage
import com.example.data.SyndicateWarRecord
import com.example.engine.RankedGameMode
import com.example.engine.RiftCalibrationState
import com.example.engine.RiftCodex
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val NEXUS_STARTER_CHIPS =
  listOf(
    "How many pixels are Suppressor's lasers?",
    "Who holds the highest bounty ever?",
    "Theorycraft a 100-pt Rift Frame sniper build",
    "Analyze my Peak Bounty Run & Executioner",
    "Compare Interdictor Stasis Bubble vs Disruptor EMP",
    "Explain Flux Core & Stasis Breach strategies",
  )

private enum class NexusHubLayoutMode {
  SPLIT_COMMAND,
  CHANNELS_ONLY,
  DECK_ONLY,
}

private data class LocalChannelPost(
  val id: String,
  val senderCallsign: String,
  val senderTag: String,
  val timestamp: String,
  val text: String,
  val accentColor: Color = Color(0xFF38BDF8),
)

/**
 * Procedurally animated swirling Rift vortex glyph (cyan/violet accretion arcs + dark singularity core).
 */
@Composable
fun SwirlingRiftVortexGlyph(
  modifier: Modifier = Modifier,
  isStreaming: Boolean = false,
) {
  val transition = rememberInfiniteTransition(label = "nexus_rift_vortex")
  val outerDeg by
    transition.animateFloat(
      initialValue = 0f,
      targetValue = 360f,
      animationSpec =
        infiniteRepeatable(
          animation = tween(durationMillis = if (isStreaming) 1350 else 3600, easing = LinearEasing),
          repeatMode = RepeatMode.Restart,
        ),
      label = "vortex_outer_deg",
    )
  val corePulse by
    transition.animateFloat(
      initialValue = 0.72f,
      targetValue = 1.0f,
      animationSpec =
        infiniteRepeatable(
          animation = tween(durationMillis = if (isStreaming) 480 else 1100, easing = LinearEasing),
          repeatMode = RepeatMode.Reverse,
        ),
      label = "vortex_core_pulse",
    )

  Canvas(modifier = modifier) {
    val w = size.width
    val h = size.height
    val r = minOf(w, h) * 0.5f
    val c = Offset(w * 0.5f, h * 0.5f)
    if (r <= 1f) return@Canvas

    // 1. Ambient singularity radial aura
    drawCircle(
      brush =
        Brush.radialGradient(
          colors =
            listOf(
              Color(0xAA00F5D4),
              Color(0x777C3AED),
              Color.Transparent,
            ),
          center = c,
          radius = r * 1.02f,
        ),
      radius = r,
      center = c,
    )

    // 2. Outer rotating accretion arcs (Electric Cyan & Event-Horizon Violet)
    val outerInset = r * 0.14f
    val outerSize = Size((r - outerInset) * 2f, (r - outerInset) * 2f)
    val outerTopLeft = Offset(c.x - (r - outerInset), c.y - (r - outerInset))
    for (arm in 0 until 3) {
      val startA = outerDeg + arm * 120f
      drawArc(
        color = if (arm % 2 == 0) Color(0xFF00F5D4) else Color(0xFFB388FF),
        startAngle = startA,
        sweepAngle = 74f,
        useCenter = false,
        topLeft = outerTopLeft,
        size = outerSize,
        style = Stroke(width = (r * 0.13f).coerceAtLeast(1.8f), cap = StrokeCap.Round),
      )
    }

    // 3. Counter-rotating inner plasma ring
    val innerInset = r * 0.34f
    val innerSize = Size((r - innerInset) * 2f, (r - innerInset) * 2f)
    val innerTopLeft = Offset(c.x - (r - innerInset), c.y - (r - innerInset))
    for (arm in 0 until 2) {
      val startA = -outerDeg * 1.45f + arm * 180f
      drawArc(
        color = if (arm == 0) Color(0xFF38BDF8) else Color(0xFFE879F9),
        startAngle = startA,
        sweepAngle = 105f,
        useCenter = false,
        topLeft = innerTopLeft,
        size = innerSize,
        style = Stroke(width = (r * 0.11f).coerceAtLeast(1.5f), cap = StrokeCap.Round),
      )
    }

    // 4. Swirling quantum filaments spiraling into core
    val radBase = outerDeg * (PI.toFloat() / 180f)
    for (i in 0 until 4) {
      val a1 = radBase + i * (PI.toFloat() * 0.5f)
      val a2 = a1 + 0.72f
      val p1 = Offset(c.x + cos(a1) * (r * 0.78f), c.y + sin(a1) * (r * 0.78f))
      val p2 = Offset(c.x + cos(a2) * (r * 0.30f), c.y + sin(a2) * (r * 0.30f))
      drawLine(
        color = Color.White.copy(alpha = 0.72f * corePulse),
        start = p1,
        end = p2,
        strokeWidth = (r * 0.07f).coerceAtLeast(1.1f),
        cap = StrokeCap.Round,
      )
    }

    // 5. Pitch-black event horizon singularity + glowing photon ring
    drawCircle(
      color = Color(0xFF030712),
      radius = r * 0.31f,
      center = c,
    )
    drawCircle(
      color = Color(0xFF00F5D4).copy(alpha = corePulse),
      radius = r * 0.32f,
      center = c,
      style = Stroke(width = (r * 0.09f).coerceAtLeast(1.4f)),
    )
    drawCircle(
      color = Color.White.copy(alpha = corePulse),
      radius = r * 0.11f * corePulse,
      center = c,
    )
  }
}

/**
 * Pure, frameless, textless swirling Rift vortex button anchored on the right side inside the SPEED / SECTOR console pill.
 */
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
    SwirlingRiftVortexGlyph(
      modifier = Modifier.size(34.dp),
      isStreaming = isStreaming,
    )
  }
}

/**
 * Compact swirling Rift header button for Hangar and Codex top bars.
 * Label updated to "NEXUS" to open the unified NEXUS Command Hub.
 */
@Composable
fun SwirlingRiftHeaderPill(
  onClick: () -> Unit,
  tag: String,
  isStreaming: Boolean = false,
) {
  val desc = stringResource(R.string.nexus_orb_content_desc)
  Row(
    modifier =
      Modifier.minimumInteractiveComponentSize()
        .height(34.dp)
        .clip(RoundedCornerShape(8.dp))
        .background(
          Brush.horizontalGradient(
            listOf(Color(0xFF09192F), Color(0xFF1E1038))
          )
        )
        .border(
          width = 1.dp,
          brush = Brush.horizontalGradient(listOf(Color(0xFF00F5D4), Color(0xFFB388FF))),
          shape = RoundedCornerShape(8.dp),
        )
        .semantics { contentDescription = desc }
        .clickable { onClick() }
        .padding(horizontal = 9.dp)
        .testTag(tag),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(6.dp),
  ) {
    SwirlingRiftVortexGlyph(
      modifier = Modifier.size(18.dp),
      isStreaming = isStreaming,
    )
    Text(
      text = "NEXUS",
      color = Color(0xFF00F5D4),
      fontSize = 9.5.sp,
      fontWeight = FontWeight.Black,
      fontFamily = FontFamily.Monospace,
      letterSpacing = 0.5.sp,
    )
  }
}

/**
 * Unified Battle.net × Discord × Brawl Stars NEXUS Command Hub & Matchmaking Console.
 * Keeps the 120Hz space arena visible behind translucent obsidian glass while hosting:
 * - Left Sidebar: Discord-style Channel Directory (NEXUS Core, 10 Ship Hull Channels, Private [TAG] Syndicate Server, & Armada Party Comms)
 * - Center Window: Active Channel Feed / Live Streaming #nexus-ai Intelligence / Ship Telemetry / Changelog / Rules / Bug Report
 * - Right Deck: Brawl Stars Graphical "Best Ship" Armada Staging Deck + 5 Always-Open Ranked Game Mode Cards
 */
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
  bootToNexusHub: Boolean = true,
  onToggleBootToNexusHub: (Boolean) -> Unit = {},
  totalOnlinePilots: Int = 1,
  syndicateOnlineCount: Int = 1,
  presetArmadasReadyCount: Int = 0,
  activeArmadaName: String = "",
  armadaSlots: List<ArmadaMemberSlot> = emptyList(),
  onResolvePilotBestShipId: (uid: String, fallbackShipId: Int) -> Int = { _, fb -> fb },
  activeRankedMode: RankedGameMode = RankedGameMode.SECTOR_CONVERGENCE,
  activeTeamSizeBracket: Int = 3,
  humanPilotCountByMode: (RankedGameMode) -> Int = { 1 },
  canQueueSyndicateWars: Pair<Boolean, String> = Pair(false, "Requires enlisted Syndicate"),
  syndicateWarRecord: SyndicateWarRecord = SyndicateWarRecord(tag = "GUEST"),
  onDeployToRankedMode: (RankedGameMode, Int) -> Unit = { _, _ -> },
  onOpenArmadaModal: () -> Unit = {},
  onQuickFormSyndicateArmada: () -> Unit = {},
  onOpenSyndicatesModal: () -> Unit = {},
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
) {
  var selectedChannelKey by remember { mutableStateOf("#nexus-ai") }
  var layoutMode by remember { mutableStateOf(NexusHubLayoutMode.SPLIT_COMMAND) }
  var inputQuery by remember { mutableStateOf("") }
  var channelChatInput by remember { mutableStateOf("") }
  var skirmishBracket by remember { mutableIntStateOf(activeTeamSizeBracket.coerceIn(2, 5)) }
  var syndicateWarBracket by remember {
    mutableIntStateOf(if (activeTeamSizeBracket >= 10) 10 else if (activeTeamSizeBracket >= 5) 5 else 3)
  }

  // Syndicate channel manager & guest invite sub-dialogs
  var showChannelManager by remember { mutableStateOf(false) }
  var newChannelDraft by remember { mutableStateOf("") }
  var showGuestManager by remember { mutableStateOf(false) }
  var guestCallsignDraft by remember { mutableStateOf("") }

  // Bug report state
  var bugCategory by remember { mutableStateOf("PHYSICS / COLLISION") }
  var bugDescription by remember { mutableStateOf("") }
  var bugStatusBanner by remember { mutableStateOf<String?>(null) }

  // Per-channel local discussion posts (for Ship Channels, Syndicate custom channels, Armada comms, Bug Reports)
  val localChannelPosts = remember {
    mutableStateMapOf<String, MutableList<LocalChannelPost>>()
  }

  val keyboardController = LocalSoftwareKeyboardController.current
  val listState = rememberLazyListState()
  val isStreaming =
    streamState is NexusStreamState.Loading || streamState is NexusStreamState.Streaming

  // Auto-scroll to newest token chunk as NEXUS AI streams
  val lastMsgTextLen = messages.lastOrNull()?.text?.length ?: 0
  LaunchedEffect(messages.size, lastMsgTextLen, streamState, selectedChannelKey) {
    if (selectedChannelKey == "#nexus-ai") {
      val totalItems = listState.layoutInfo.totalItemsCount
      if (totalItems > 0) {
        listState.animateScrollToItem(totalItems - 1)
      }
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

  fun postToChannel(channelKey: String, rawText: String, accent: Color = Color(0xFF38BDF8)) {
    val clean = rawText.trim()
    if (clean.isEmpty()) return
    val tag = syndicateTag.ifBlank { "GUEST" }
    val list = localChannelPosts.getOrPut(channelKey) { mutableStateListOf() }
    list.add(
      LocalChannelPost(
        id = "post_${System.currentTimeMillis()}_${list.size}",
        senderCallsign = callsign,
        senderTag = tag,
        timestamp = "LIVE",
        text = clean,
        accentColor = accent,
      )
    )
    onSendChannelMessage(channelKey, clean)
  }

  val shipChannels = remember {
    listOf(
      1 to "#viper",
      2 to "#ricochet",
      3 to "#suppressor",
      4 to "#goliath",
      5 to "#twinfang",
      6 to "#disruptor",
      7 to "#valkyrie",
      8 to "#manta",
      9 to "#interdictor",
      10 to "#rift-frame",
    )
  }

  val cleanSyndicateTag =
    if (syndicateTag.isBlank() || syndicateTag in setOf("FREE", "SOLO", "UNAFFILIATED")) "GUEST"
    else syndicateTag

  Box(
    modifier =
      Modifier.fillMaxSize()
        .background(Color(0x78020611))
        .windowInsetsPadding(WindowInsets.safeDrawing)
        .imePadding()
        .padding(horizontal = 10.dp, vertical = 6.dp)
        .testTag("nexus_ai_console_modal"),
    contentAlignment = Alignment.Center,
  ) {
    Surface(
      modifier =
        Modifier.fillMaxWidth(0.98f)
          .fillMaxHeight(0.95f)
          .border(
            width = 1.4.dp,
            brush =
              Brush.linearGradient(
                colors =
                  listOf(
                    Color(0xDD00F5D4),
                    Color(0xCCB388FF),
                    Color(0xAA38BDF8),
                  )
              ),
            shape = RoundedCornerShape(16.dp),
          ),
      color = Color(0xD9050B16),
      shape = RoundedCornerShape(16.dp),
    ) {
      Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        // =========================================================================
        // 1. TOP TELEMETRY, HUMAN-ONLY COUNTERS & STARTUP BOOT PREFERENCE BAR
        // =========================================================================
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f),
          ) {
            SwirlingRiftVortexGlyph(
              modifier = Modifier.size(32.dp),
              isStreaming = isStreaming,
            )
            Column {
              Text(
                text = "NEXUS // SECTOR COMMAND HUB & MATCHMAKING",
                color = Color(0xFF00F5D4),
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 0.6.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.horizontalScroll(rememberScrollState()),
              ) {
                Text(
                  text = "🟢 $totalOnlinePilots PILOTS ONLINE",
                  color = Color(0xFF34D399),
                  fontSize = 9.sp,
                  fontWeight = FontWeight.ExtraBold,
                  fontFamily = FontFamily.Monospace,
                )
                Text(
                  text = "🏛️ $syndicateOnlineCount [$cleanSyndicateTag] MEMBERS ONLINE",
                  color = Color(0xFFFBBF24),
                  fontSize = 9.sp,
                  fontWeight = FontWeight.ExtraBold,
                  fontFamily = FontFamily.Monospace,
                )
                Text(
                  text = "🛡️ $presetArmadasReadyCount PRESET ARMADAS READY",
                  color = Color(0xFF60A5FA),
                  fontSize = 9.sp,
                  fontWeight = FontWeight.ExtraBold,
                  fontFamily = FontFamily.Monospace,
                )
                Text(
                  text = "• LVL $pilotLevel ($pointBudget PTS) • TOP: $highestBountySummary",
                  color = Color(0xFF94A3B8),
                  fontSize = 8.8.sp,
                  fontWeight = FontWeight.SemiBold,
                  fontFamily = FontFamily.Monospace,
                )
              }
            }
          }

          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            // Startup Boot Mode Preference Toggle Chip
            Box(
              modifier =
                Modifier.clip(RoundedCornerShape(8.dp))
                  .background(if (bootToNexusHub) Color(0xFF092238) else Color(0xFF231808))
                  .border(
                    1.dp,
                    if (bootToNexusHub) Color(0xFF00F5D4) else Color(0xFFF59E0B),
                    RoundedCornerShape(8.dp),
                  )
                  .clickable { onToggleBootToNexusHub(!bootToNexusHub) }
                  .padding(horizontal = 8.dp, vertical = 6.dp)
                  .testTag("nexus_boot_mode_toggle"),
            ) {
              Text(
                text =
                  if (bootToNexusHub) "⚙️ STARTUP: NEXUS HUB"
                  else "⚡ STARTUP: CHANGELOG + QUICK-JOIN",
                color = if (bootToNexusHub) Color(0xFF00F5D4) else Color(0xFFFACC15),
                fontSize = 8.8.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
              )
            }

            if (messages.isNotEmpty() && selectedChannelKey == "#nexus-ai") {
              Button(
                onClick = onClearHistory,
                colors =
                  ButtonDefaults.buttonColors(
                    containerColor = Color(0xAA0F172A),
                    contentColor = Color(0xFF94A3B8),
                  ),
                border = BorderStroke(1.dp, Color(0x66475569)),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                modifier =
                  Modifier.minimumInteractiveComponentSize()
                    .height(32.dp)
                    .testTag("nexus_clear_button"),
              ) {
                Text(
                  text = "↺ CLEAR",
                  fontSize = 9.sp,
                  fontWeight = FontWeight.Bold,
                  fontFamily = FontFamily.Monospace,
                )
              }
            }

            Button(
              onClick = onCloseModal,
              colors =
                ButtonDefaults.buttonColors(
                  containerColor = Color(0xCC28111B),
                  contentColor = Color(0xFFFF3B4E),
                ),
              border = BorderStroke(1.dp, Color(0xFFFF3B4E)),
              shape = RoundedCornerShape(8.dp),
              contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
              modifier =
                Modifier.minimumInteractiveComponentSize()
                  .height(32.dp)
                  .testTag("nexus_close_button"),
            ) {
              Text(
                text = "✕ RETURN TO ARENA",
                fontSize = 9.5.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
              )
            }
          }
        }

        // Sub-view mode bar (Split Command View vs Full Channels vs Full Armada & Ranked Queues)
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            NexusViewModeTab(
              label = "⚡ SPLIT COMMAND VIEW",
              selected = layoutMode == NexusHubLayoutMode.SPLIT_COMMAND,
              onClick = { layoutMode = NexusHubLayoutMode.SPLIT_COMMAND },
              tag = "nexus_tab_split_view",
            )
            NexusViewModeTab(
              label = "💬 CHANNELS & NEXUS AI",
              selected = layoutMode == NexusHubLayoutMode.CHANNELS_ONLY,
              onClick = { layoutMode = NexusHubLayoutMode.CHANNELS_ONLY },
              tag = "nexus_tab_channels_view",
            )
            NexusViewModeTab(
              label = "🎮 ARMADA DECK & RANKED QUEUES",
              selected = layoutMode == NexusHubLayoutMode.DECK_ONLY,
              onClick = { layoutMode = NexusHubLayoutMode.DECK_ONLY },
              tag = "nexus_tab_deck_view",
            )
          }

          Text(
            text = "ACTIVE MODE: ${activeRankedMode.title.uppercase()} • HULL: $activeShipSummary",
            color = Color(0xFF38BDF8),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
          )
        }

        // =========================================================================
        // 2. MAIN 3-PANE COMMAND HUB BODY
        // =========================================================================
        Row(
          modifier = Modifier.weight(1f).fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          // -----------------------------------------------------------------------
          // PANE A: LEFT SIDEBAR — DISCORD-STYLE CHANNEL DIRECTORY
          // -----------------------------------------------------------------------
          if (layoutMode != NexusHubLayoutMode.DECK_ONLY) {
            Column(
              modifier =
                Modifier.width(196.dp)
                  .fillMaxHeight()
                  .clip(RoundedCornerShape(12.dp))
                  .background(Color(0x99040914))
                  .border(1.dp, Color(0x4438BDF8), RoundedCornerShape(12.dp))
                  .padding(8.dp)
                  .verticalScroll(rememberScrollState())
                  .testTag("nexus_channel_directory"),
              verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              // SECTION 1: // NEXUS CORE
              ChannelCategoryHeader("// NEXUS CORE")
              ChannelNavItem(
                label = "🧠 #nexus-ai",
                badge = "AI LIVE",
                selected = selectedChannelKey == "#nexus-ai",
                accent = Color(0xFF00F5D4),
                onClick = { selectedChannelKey = "#nexus-ai" },
                tag = "nexus_ch_nexus_ai",
              )
              ChannelNavItem(
                label = "🌐 #main-lobby",
                badge = "${totalOnlinePilots}",
                selected = selectedChannelKey == "#main-lobby",
                accent = Color(0xFF38BDF8),
                onClick = { selectedChannelKey = "#main-lobby" },
                tag = "nexus_ch_main_lobby",
              )
              ChannelNavItem(
                label = "⚡ #change-log",
                badge = "v39.0",
                selected = selectedChannelKey == "#change-log",
                accent = Color(0xFFFACC15),
                onClick = { selectedChannelKey = "#change-log" },
                tag = "nexus_ch_change_log",
              )
              ChannelNavItem(
                label = "📜 #rules",
                badge = "FAIR",
                selected = selectedChannelKey == "#rules",
                accent = Color(0xFFA78BFA),
                onClick = { selectedChannelKey = "#rules" },
                tag = "nexus_ch_rules",
              )
              ChannelNavItem(
                label = "🛠️ #bug-report",
                badge = "FORM",
                selected = selectedChannelKey == "#bug-report",
                accent = Color(0xFFFB7185),
                onClick = { selectedChannelKey = "#bug-report" },
                tag = "nexus_ch_bug_report",
              )

              Spacer(modifier = Modifier.height(4.dp))

              // SECTION 2: // [TAG] SYNDICATE NAME (Private Syndicate Server)
              ChannelCategoryHeader("// [$cleanSyndicateTag] ${syndicateName.take(14)}")
              if (canAccessSyndicateServer) {
                syndicateCustomChannels.forEachIndexed { idx, chName ->
                  val key = "syndicate:$chName"
                  ChannelNavItem(
                    label = "🏛️ $chName",
                    badge = if (idx == 0) "CORE" else "CLAN",
                    selected = selectedChannelKey == key,
                    accent = Color(0xFFF59E0B),
                    onClick = { selectedChannelKey = key },
                    tag = "nexus_ch_syndicate_$idx",
                  )
                }
                Row(
                  modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                  horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                  Box(
                    modifier =
                      Modifier.weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF1E293B))
                        .border(1.dp, Color(0x66F59E0B), RoundedCornerShape(6.dp))
                        .clickable { showChannelManager = !showChannelManager }
                        .padding(vertical = 4.dp)
                        .testTag("nexus_btn_manage_syndicate_channels"),
                    contentAlignment = Alignment.Center,
                  ) {
                    Text(
                      text = "+ CH (${syndicateCustomChannels.size}/5)",
                      color = Color(0xFFFBBF24),
                      fontSize = 8.sp,
                      fontWeight = FontWeight.ExtraBold,
                      fontFamily = FontFamily.Monospace,
                    )
                  }
                  Box(
                    modifier =
                      Modifier.weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF1E293B))
                        .border(1.dp, Color(0x6638BDF8), RoundedCornerShape(6.dp))
                        .clickable { showGuestManager = !showGuestManager }
                        .padding(vertical = 4.dp)
                        .testTag("nexus_btn_syndicate_guest_pass"),
                    contentAlignment = Alignment.Center,
                  ) {
                    Text(
                      text = "🎟️ GUESTS (${syndicateInvitedGuests.size})",
                      color = Color(0xFF38BDF8),
                      fontSize = 8.sp,
                      fontWeight = FontWeight.ExtraBold,
                      fontFamily = FontFamily.Monospace,
                    )
                  }
                }
              } else {
                ChannelNavItem(
                  label = "🔒 Syndicate Locked",
                  badge = "JOIN",
                  selected = selectedChannelKey == "syndicate:locked",
                  accent = Color(0xFF64748B),
                  onClick = { selectedChannelKey = "syndicate:locked" },
                  tag = "nexus_ch_syndicate_locked",
                )
              }

              // SECTION 3: // ARMADA PARTY COMMS (When in a 2-5 Pilot Armada)
              if (armadaSlots.size >= 2 || activeArmadaName.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                ChannelCategoryHeader("// ARMADA PARTY COMMS")
                val armLabel = activeArmadaName.ifEmpty { "SQUAD" }
                ChannelNavItem(
                  label = "🛡️ #armada-chat ($armLabel)",
                  badge = "${armadaSlots.size.coerceAtLeast(1)}/5",
                  selected = selectedChannelKey == "#armada-chat",
                  accent = Color(0xFF60A5FA),
                  onClick = { selectedChannelKey = "#armada-chat" },
                  tag = "nexus_ch_armada_chat",
                )
              }

              Spacer(modifier = Modifier.height(4.dp))

              // SECTION 4: // SHIP CHANNELS (1–10)
              ChannelCategoryHeader("// SHIP CHANNELS (1–10)")
              shipChannels.forEach { (shipId, chHandle) ->
                ChannelNavItem(
                  label = "🚀 $chHandle",
                  badge = "#$shipId",
                  selected = selectedChannelKey == chHandle,
                  accent = if (shipId == 10) Color(0xFFD8B4FE) else Color(0xFF38BDF8),
                  onClick = { selectedChannelKey = chHandle },
                  tag = "nexus_ch_ship_$shipId",
                )
              }
            }
          }

          // -----------------------------------------------------------------------
          // PANE B: CENTER WINDOW — ACTIVE CHANNEL / NEXUS AI / SHIP / CHANGELOG
          // -----------------------------------------------------------------------
          if (layoutMode != NexusHubLayoutMode.DECK_ONLY) {
            Column(
              modifier =
                Modifier.weight(if (layoutMode == NexusHubLayoutMode.SPLIT_COMMAND) 1.05f else 1f)
                  .fillMaxHeight()
                  .clip(RoundedCornerShape(12.dp))
                  .background(Color(0x88040914))
                  .border(1.dp, Color(0x4438BDF8), RoundedCornerShape(12.dp))
                  .padding(10.dp),
              verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              // Optional Syndicate Channel / Guest Pass Manager Drawer
              if (showChannelManager && canAccessSyndicateServer) {
                SyndicateChannelManagerCard(
                  syndicateTag = cleanSyndicateTag,
                  channels = syndicateCustomChannels,
                  newChannelDraft = newChannelDraft,
                  onDraftChange = { newChannelDraft = it },
                  onAddChannel = {
                    if (newChannelDraft.isNotBlank()) {
                      onAddSyndicateChannel(newChannelDraft)
                      newChannelDraft = ""
                    }
                  },
                  onDeleteChannel = onDeleteSyndicateChannel,
                  onClose = { showChannelManager = false },
                )
              }
              if (showGuestManager && canAccessSyndicateServer) {
                SyndicateGuestPassManagerCard(
                  syndicateTag = cleanSyndicateTag,
                  invitedGuests = syndicateInvitedGuests,
                  guestDraft = guestCallsignDraft,
                  onDraftChange = { guestCallsignDraft = it },
                  onInvite = {
                    if (guestCallsignDraft.isNotBlank()) {
                      onInviteSyndicateGuest(guestCallsignDraft)
                      guestCallsignDraft = ""
                    }
                  },
                  onRevoke = onRevokeSyndicateGuest,
                  onClose = { showGuestManager = false },
                )
              }

              when {
                // 1) #nexus-ai (Live Streaming Gemini / NEXUS Core Channel)
                selectedChannelKey == "#nexus-ai" -> {
                  Row(
                    modifier =
                      Modifier.fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    NEXUS_STARTER_CHIPS.forEachIndexed { idx, chipPrompt ->
                      Box(
                        modifier =
                          Modifier.clip(RoundedCornerShape(14.dp))
                            .background(Color(0x990D1B2E))
                            .border(1.dp, Color(0x6638BDF8), RoundedCornerShape(14.dp))
                            .clickable(enabled = !isStreaming) { onSendQuestion(chipPrompt) }
                            .padding(horizontal = 9.dp, vertical = 5.dp)
                            .testTag("nexus_quick_chip_$idx"),
                      ) {
                        Text(
                          text = "⚡ $chipPrompt",
                          color = if (isStreaming) Color(0xFF64748B) else Color(0xFFE0F2FE),
                          fontSize = 9.sp,
                          fontWeight = FontWeight.SemiBold,
                          fontFamily = FontFamily.SansSerif,
                          maxLines = 1,
                        )
                      }
                    }
                  }

                  LazyColumn(
                    state = listState,
                    modifier =
                      Modifier.weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0x59030712))
                        .border(1.dp, Color(0x3338BDF8), RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                        .testTag("nexus_message_list"),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                  ) {
                    if (messages.isEmpty()) {
                      item(key = "nexus_zero_state") {
                        NexusWelcomeTelemetryCard()
                      }
                    }

                    items(items = messages, key = { it.id }) { msg ->
                      NexusMessageBubble(message = msg)
                    }

                    if (streamState is NexusStreamState.Error) {
                      item(key = "nexus_error_card") {
                        NexusErrorBanner(
                          errorMessage = streamState.message,
                          canRetry = !lastFailedQuestion.isNullOrBlank(),
                          onRetry = onRetryLastQuestion,
                        )
                      }
                    }
                  }

                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                  ) {
                    Box(
                      modifier =
                        Modifier.weight(1f)
                          .height(40.dp)
                          .clip(RoundedCornerShape(10.dp))
                          .background(Color(0xCC091324))
                          .border(
                            width = 1.dp,
                            color = if (isStreaming) Color(0x55475569) else Color(0x9900F5D4),
                            shape = RoundedCornerShape(10.dp),
                          )
                          .padding(horizontal = 10.dp)
                          .testTag("nexus_query_input_box"),
                      contentAlignment = Alignment.CenterStart,
                    ) {
                      if (inputQuery.isEmpty()) {
                        Text(
                          text = stringResource(R.string.nexus_input_placeholder),
                          color = Color(0xFF64748B),
                          fontSize = 10.5.sp,
                          fontFamily = FontFamily.SansSerif,
                          maxLines = 1,
                          overflow = TextOverflow.Ellipsis,
                        )
                      }
                      BasicTextField(
                        value = inputQuery,
                        onValueChange = { inputQuery = it },
                        enabled = !isStreaming,
                        singleLine = true,
                        cursorBrush = SolidColor(Color(0xFF00F5D4)),
                        textStyle =
                          TextStyle(
                            color = Color.White,
                            fontSize = 11.5.sp,
                            fontFamily = FontFamily.SansSerif,
                          ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = { submitNexusAiQuery() }),
                        modifier = Modifier.fillMaxWidth().testTag("nexus_query_input"),
                      )
                    }

                    Button(
                      onClick = { submitNexusAiQuery() },
                      enabled = inputQuery.isNotBlank() && !isStreaming,
                      colors =
                        ButtonDefaults.buttonColors(
                          containerColor = Color(0xFF0E7490),
                          contentColor = Color.White,
                          disabledContainerColor = Color(0x661E293B),
                          disabledContentColor = Color(0xFF64748B),
                        ),
                      border =
                        BorderStroke(
                          1.dp,
                          if (inputQuery.isNotBlank() && !isStreaming) Color(0xFF00F5D4) else Color(0x44475569),
                        ),
                      shape = RoundedCornerShape(10.dp),
                      contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                      modifier =
                        Modifier.minimumInteractiveComponentSize()
                          .height(40.dp)
                          .testTag("nexus_send_button"),
                    ) {
                      Text(
                        text = if (isStreaming) "SYNCING…" else "${stringResource(R.string.nexus_transmit)} ➢",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                      )
                    }
                  }
                }

                // 2) #change-log Channel
                selectedChannelKey == "#change-log" -> {
                  NexusChangelogChannelView()
                }

                // 3) #rules Channel
                selectedChannelKey == "#rules" -> {
                  NexusRulesChannelView()
                }

                // 4) #bug-report Channel
                selectedChannelKey == "#bug-report" -> {
                  NexusBugReportChannelView(
                    selectedCategory = bugCategory,
                    onSelectCategory = { bugCategory = it },
                    description = bugDescription,
                    onDescriptionChange = { bugDescription = it },
                    statusBanner = bugStatusBanner,
                    submittedPosts = localChannelPosts["#bug-report"] ?: emptyList(),
                    onSubmit = {
                      val clean = bugDescription.trim()
                      if (clean.isNotEmpty()) {
                        postToChannel(
                          "#bug-report",
                          "[$bugCategory] $clean",
                          Color(0xFFFB7185),
                        )
                        onSubmitBugReport(bugCategory, clean)
                        bugDescription = ""
                        bugStatusBanner = "✓ ANOMALY LOGGED TO NEXUS CORE TELEMETRY"
                        keyboardController?.hide()
                      }
                    },
                  )
                }

                // 5) Locked Syndicate Server Gate
                selectedChannelKey == "syndicate:locked" -> {
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
                    Text(
                      text = "🔒 PRIVATE SYNDICATE SERVER",
                      color = Color(0xFFFACC15),
                      fontSize = 14.sp,
                      fontWeight = FontWeight.Black,
                      fontFamily = FontFamily.Monospace,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                      text =
                        "ENLIST IN A SYNDICATE OR RECEIVE A GUEST INVITE TO ACCESS PRIVATE [TAG] SYNDICATE CHANNELS & SYNDICATE WARS.",
                      color = Color(0xFFCBD5E1),
                      fontSize = 11.sp,
                      textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                      onClick = onOpenSyndicatesModal,
                      colors =
                        ButtonDefaults.buttonColors(
                          containerColor = Color(0xFF78350F),
                          contentColor = Color(0xFFFEF08A),
                        ),
                      border = BorderStroke(1.dp, Color(0xFFF59E0B)),
                      shape = RoundedCornerShape(8.dp),
                    ) {
                      Text(
                        text = "🏛️ OPEN SYNDICATE DIRECTORY",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace,
                      )
                    }
                  }
                }

                // 6) Ship Hull Channels (#viper .. #rift-frame)
                shipChannels.any { it.second == selectedChannelKey } -> {
                  val shipId = shipChannels.first { it.second == selectedChannelKey }.first
                  val spec = RiftCodex.getShipById(shipId)
                  val shipPosts = localChannelPosts[selectedChannelKey] ?: emptyList()

                  NexusShipChannelView(
                    shipId = shipId,
                    channelHandle = selectedChannelKey,
                    spec = spec,
                    posts = shipPosts,
                    chatInput = channelChatInput,
                    onChatInputChange = { channelChatInput = it },
                    onSendPost = {
                      postToChannel(selectedChannelKey, channelChatInput, Color(0xFF38BDF8))
                      channelChatInput = ""
                      keyboardController?.hide()
                    },
                    onEquipInHangar = { onOpenHangarForShip(shipId) },
                    onAskNexusAiAboutHull = {
                      selectedChannelKey = "#nexus-ai"
                      onSendQuestion(
                        "Give me a complete combat breakdown, 100-pt stat ratios, and tactical tips for ${spec.name} (Hull #$shipId)."
                      )
                    },
                  )
                }

                // 7) #main-lobby, #armada-chat, or Private Syndicate Channels
                else -> {
                  val isSyndicateCh = selectedChannelKey.startsWith("syndicate:")
                  val displayTitle =
                    when {
                      isSyndicateCh ->
                        "🏛️ [$cleanSyndicateTag] ${selectedChannelKey.removePrefix("syndicate:")}"
                      selectedChannelKey == "#armada-chat" ->
                        "🛡️ #armada-chat [${activeArmadaName.ifEmpty { "SQUAD" }}]"
                      else -> "🌐 #main-lobby // GLOBAL SECTOR COMMS"
                    }
                  val channelPosts = localChannelPosts[selectedChannelKey] ?: emptyList()

                  NexusTextChannelFeedView(
                    channelTitle = displayTitle,
                    channelSubtitle =
                      when {
                        isSyndicateCh ->
                          "Private [$cleanSyndicateTag] $syndicateName Server • Members & Invited Guests (${syndicateInvitedGuests.size} Guests)"
                        selectedChannelKey == "#armada-chat" ->
                          "Encrypted Armada Wingman Comms • Friendly Fire Disabled Across Squad"
                        else ->
                          "Live Cross-Sector Pilot Feed • Zero Bot Clutter ($totalOnlinePilots Humans Online)"
                      },
                    globalComms = if (selectedChannelKey == "#main-lobby") lobbyMessages else emptyList(),
                    localPosts = channelPosts,
                    chatInput = channelChatInput,
                    onChatInputChange = { channelChatInput = it },
                    onSend = {
                      postToChannel(
                        selectedChannelKey,
                        channelChatInput,
                        if (isSyndicateCh) Color(0xFFF59E0B) else Color(0xFF38BDF8),
                      )
                      channelChatInput = ""
                      keyboardController?.hide()
                    },
                  )
                }
              }
            }
          }

          // -----------------------------------------------------------------------
          // PANE C: RIGHT STAGE — BRAWL STARS GRAPHICAL "BEST SHIP" ARMADA DECK
          //         + 5 ALWAYS-OPEN RANKED GAME MODE DEPLOYMENT CARDS
          // -----------------------------------------------------------------------
          if (layoutMode != NexusHubLayoutMode.CHANNELS_ONLY) {
            Column(
              modifier =
                Modifier.weight(1.1f)
                  .fillMaxHeight()
                  .clip(RoundedCornerShape(12.dp))
                  .background(Color(0x99040914))
                  .border(1.dp, Color(0x5500F5D4), RoundedCornerShape(12.dp))
                  .padding(10.dp)
                  .verticalScroll(rememberScrollState())
                  .testTag("nexus_armada_and_ranked_deck"),
              verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              // 1. Brawl Stars Graphical "Best Ship" Armada Staging Bay
              BrawlStarsArmadaStagingDeck(
                callsign = callsign,
                syndicateTag = cleanSyndicateTag,
                bestShipId = bestShipId,
                activeArmadaName = activeArmadaName,
                armadaSlots = armadaSlots,
                onResolvePilotBestShipId = onResolvePilotBestShipId,
                onOpenArmadaModal = onOpenArmadaModal,
                onQuickFormSyndicateArmada = onQuickFormSyndicateArmada,
                onOpenArmadaChat = {
                  selectedChannelKey = "#armada-chat"
                  if (layoutMode == NexusHubLayoutMode.DECK_ONLY) {
                    layoutMode = NexusHubLayoutMode.SPLIT_COMMAND
                  }
                },
              )

              // 2. 5 Always-Open Ranked Matchmaking Mode Cards
              Text(
                text = "🎯 5 ALWAYS-OPEN RANKED ARENA QUEUES (HUMAN PILOTS ONLY — ZERO BOT COUNTS)",
                color = Color(0xFF00F5D4),
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
              )

              RankedGameMode.entries.forEach { mode ->
                val humanCount = humanPilotCountByMode(mode).coerceAtLeast(1)
                val isCurrentMode = activeRankedMode == mode
                val bracketForMode =
                  when (mode) {
                    RankedGameMode.RIFT_SKIRMISH -> skirmishBracket
                    RankedGameMode.SYNDICATE_WARS -> syndicateWarBracket
                    RankedGameMode.STASIS_BREACH -> 10
                    else -> 20
                  }

                RankedModeDeploymentCard(
                  mode = mode,
                  isCurrentActive = isCurrentMode,
                  humanPilotsActive = humanCount,
                  armadaSize = armadaSlots.size.coerceAtLeast(1),
                  syndicateTag = cleanSyndicateTag,
                  selectedBracket = bracketForMode,
                  onSelectBracket = { newBracket ->
                    if (mode == RankedGameMode.RIFT_SKIRMISH) {
                      skirmishBracket = newBracket.coerceIn(2, 5)
                    } else if (mode == RankedGameMode.SYNDICATE_WARS) {
                      syndicateWarBracket = newBracket
                    }
                  },
                  canQueueSyndicateWars = canQueueSyndicateWars,
                  syndicateWarRecord = syndicateWarRecord,
                  onDeploy = {
                    onDeployToRankedMode(mode, bracketForMode)
                  },
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
private fun NexusViewModeTab(
  label: String,
  selected: Boolean,
  onClick: () -> Unit,
  tag: String,
) {
  Box(
    modifier =
      Modifier.clip(RoundedCornerShape(8.dp))
        .background(if (selected) Color(0xFF0E3A5A) else Color(0x880B1324))
        .border(
          1.dp,
          if (selected) Color(0xFF00F5D4) else Color(0x44475569),
          RoundedCornerShape(8.dp),
        )
        .clickable { onClick() }
        .padding(horizontal = 10.dp, vertical = 5.dp)
        .testTag(tag),
  ) {
    Text(
      text = label,
      color = if (selected) Color(0xFF00F5D4) else Color(0xFF94A3B8),
      fontSize = 9.sp,
      fontWeight = FontWeight.ExtraBold,
      fontFamily = FontFamily.Monospace,
    )
  }
}

@Composable
private fun ChannelCategoryHeader(title: String) {
  Text(
    text = title,
    color = Color(0xFF64748B),
    fontSize = 8.2.sp,
    fontWeight = FontWeight.Black,
    fontFamily = FontFamily.Monospace,
    modifier = Modifier.padding(top = 4.dp, bottom = 2.dp),
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
        .clip(RoundedCornerShape(7.dp))
        .background(if (selected) accent.copy(alpha = 0.18f) else Color.Transparent)
        .border(
          width = 1.dp,
          color = if (selected) accent else Color.Transparent,
          shape = RoundedCornerShape(7.dp),
        )
        .clickable { onClick() }
        .padding(horizontal = 7.dp, vertical = 5.dp)
        .testTag(tag),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      text = label,
      color = if (selected) Color.White else Color(0xFFCBD5E1),
      fontSize = 9.5.sp,
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
          .padding(horizontal = 4.dp, vertical = 1.dp)
    ) {
      Text(
        text = badge,
        color = accent,
        fontSize = 7.5.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace,
      )
    }
  }
}

@Composable
private fun BrawlStarsArmadaStagingDeck(
  callsign: String,
  syndicateTag: String,
  bestShipId: Int,
  activeArmadaName: String,
  armadaSlots: List<ArmadaMemberSlot>,
  onResolvePilotBestShipId: (uid: String, fallbackShipId: Int) -> Int,
  onOpenArmadaModal: () -> Unit,
  onQuickFormSyndicateArmada: () -> Unit,
  onOpenArmadaChat: () -> Unit,
) {
  val effectiveSlots =
    remember(armadaSlots, callsign, syndicateTag, bestShipId) {
      if (armadaSlots.isNotEmpty()) {
        armadaSlots
      } else {
        listOf(
          ArmadaMemberSlot(
            uid = "local_player",
            callsign = callsign,
            syndicateTag = syndicateTag,
            shipId = bestShipId,
            isLeader = true,
          )
        )
      }
    }

  Column(
    modifier =
      Modifier.fillMaxWidth()
        .clip(RoundedCornerShape(12.dp))
        .background(
          Brush.verticalGradient(
            listOf(Color(0xFF0A192F), Color(0xFF060E1E))
          )
        )
        .border(1.dp, Color(0x8838BDF8), RoundedCornerShape(12.dp))
        .padding(10.dp)
        .testTag("nexus_brawl_stars_armada_deck"),
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column {
        Text(
          text =
            if (activeArmadaName.isNotEmpty())
              "🛡️ ARMADA STAGING BAY // [$activeArmadaName] (${effectiveSlots.size}/5)"
            else "🛡️ SOLO / ARMADA STAGING BAY // BEST-HULL AVATARS",
          color = Color(0xFF38BDF8),
          fontSize = 10.sp,
          fontWeight = FontWeight.Black,
          fontFamily = FontFamily.Monospace,
        )
        Text(
          text = "Displaying each pilot's #1 Peak-Bounty Hull Avatar • Armada deploys together into any Ranked Queue",
          color = Color(0xFF94A3B8),
          fontSize = 8.2.sp,
          fontFamily = FontFamily.SansSerif,
        )
      }

      Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Box(
          modifier =
            Modifier.clip(RoundedCornerShape(6.dp))
              .background(Color(0xFF0E3A5A))
              .border(1.dp, Color(0xFF38BDF8), RoundedCornerShape(6.dp))
              .clickable { onOpenArmadaModal() }
              .padding(horizontal = 8.dp, vertical = 5.dp)
              .testTag("nexus_btn_manage_armada"),
        ) {
          Text(
            text = "+ FORM / MANAGE ARMADA",
            color = Color(0xFFE0F2FE),
            fontSize = 8.2.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = FontFamily.Monospace,
          )
        }
        Box(
          modifier =
            Modifier.clip(RoundedCornerShape(6.dp))
              .background(Color(0xFF1E1B4B))
              .border(1.dp, Color(0xFF818CF8), RoundedCornerShape(6.dp))
              .clickable { onOpenArmadaChat() }
              .padding(horizontal = 8.dp, vertical = 5.dp)
              .testTag("nexus_btn_open_armada_chat"),
        ) {
          Text(
            text = "💬 #ARMADA-CHAT",
            color = Color(0xFFC7D2FE),
            fontSize = 8.2.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = FontFamily.Monospace,
          )
        }
      }
    }

    // 5-Pedestal Brawl Stars Holographic Row
    Row(
      modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(6.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      for (slotIdx in 0 until 5) {
        val member = effectiveSlots.getOrNull(slotIdx)
        if (member != null) {
          val resolvedBestHullId =
            if (member.isLeader) bestShipId.coerceIn(1, 10)
            else onResolvePilotBestShipId(member.uid, member.shipId).coerceIn(1, 10)
          val bestHullSpec = RiftCodex.getShipById(resolvedBestHullId)
          val tagStr = member.syndicateTag.ifBlank { "GUEST" }

          Column(
            modifier =
              Modifier.width(104.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xCC071122))
                .border(
                  1.dp,
                  if (member.isLeader) Color(0xFF00F5D4) else Color(0xFF38BDF8),
                  RoundedCornerShape(10.dp),
                )
                .padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp),
          ) {
            Text(
              text = if (member.isLeader) "★ LEADER • READY" else "● WINGMAN • READY",
              color = if (member.isLeader) Color(0xFF00F5D4) else Color(0xFF34D399),
              fontSize = 7.2.sp,
              fontWeight = FontWeight.Black,
              fontFamily = FontFamily.Monospace,
            )
            Box(
              modifier = Modifier.size(48.dp),
              contentAlignment = Alignment.Center,
            ) {
              Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                  color = Color(0x3300F5D4),
                  radius = size.minDimension * 0.46f,
                )
                drawCircle(
                  color = Color(0xFF00F5D4),
                  radius = size.minDimension * 0.46f,
                  style = Stroke(width = 1.4.dp.toPx()),
                )
              }
              ShipSpritePreview(shipId = resolvedBestHullId, sizeDp = 38)
            }
            Text(
              text = "[$tagStr] ${member.callsign}",
              color = Color.White,
              fontSize = 8.2.sp,
              fontWeight = FontWeight.ExtraBold,
              fontFamily = FontFamily.Monospace,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Text(
              text = "BEST HULL: ${bestHullSpec.name.uppercase()}",
              color = Color(0xFFFACC15),
              fontSize = 7.2.sp,
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.Monospace,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        } else {
          // Empty holographic invite pedestal
          Column(
            modifier =
              Modifier.width(96.dp)
                .height(106.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0x55071122))
                .border(1.dp, Color(0x44475569), RoundedCornerShape(10.dp))
                .clickable { onQuickFormSyndicateArmada() }
                .padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
          ) {
            Text(
              text = "＋",
              color = Color(0xFF38BDF8),
              fontSize = 16.sp,
              fontWeight = FontWeight.Black,
            )
            Text(
              text = "ADD WINGMAN",
              color = Color(0xFF94A3B8),
              fontSize = 7.5.sp,
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.Monospace,
            )
            Text(
              text = "SLOT ${slotIdx + 1}/5",
              color = Color(0xFF64748B),
              fontSize = 7.sp,
              fontFamily = FontFamily.Monospace,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun RankedModeDeploymentCard(
  mode: RankedGameMode,
  isCurrentActive: Boolean,
  humanPilotsActive: Int,
  armadaSize: Int,
  syndicateTag: String,
  selectedBracket: Int,
  onSelectBracket: (Int) -> Unit,
  canQueueSyndicateWars: Pair<Boolean, String>,
  syndicateWarRecord: SyndicateWarRecord,
  onDeploy: () -> Unit,
) {
  val modeAccent =
    when (mode) {
      RankedGameMode.SECTOR_CONVERGENCE -> Color(0xFF00F5D4)
      RankedGameMode.RIFT_SKIRMISH -> Color(0xFF38BDF8)
      RankedGameMode.FLUX_CORE -> Color(0xFFFACC15)
      RankedGameMode.STASIS_BREACH -> Color(0xFFFB7185)
      RankedGameMode.SYNDICATE_WARS -> Color(0xFFF59E0B)
    }

  val modeIcon =
    when (mode) {
      RankedGameMode.SECTOR_CONVERGENCE -> "🌀"
      RankedGameMode.RIFT_SKIRMISH -> "⚔️"
      RankedGameMode.FLUX_CORE -> "⚡"
      RankedGameMode.STASIS_BREACH -> "🔒"
      RankedGameMode.SYNDICATE_WARS -> "🏴‍☠️"
    }

  val modeDescription =
    when (mode) {
      RankedGameMode.SECTOR_CONVERGENCE ->
        "20-Pilot FFA / 1–5 Armada Bounty • Ongoing 60m cycle • +100❖ Rift Shards for #1 Bounty with >50% presence."
      RankedGameMode.RIFT_SKIRMISH ->
        "Best-of-3 Team Deathmatch (15 Kills/Round) • Opposite West/East Warp Gates • +75❖ Rift Shards on match win."
      RankedGameMode.FLUX_CORE ->
        "20-Pilot Singularity Carrier FFA • +1 Overcharge Tier / 5s (up to +50% Dmg/Recharge & +40% Speed) + 15 Bounty/s • 30s Supersonic Death/Safe-Zone Eject."
      RankedGameMode.STASIS_BREACH ->
        "10v10 Divided Sector Jailbreak • Kills send victims to enemy Stasis Brig • Destroy 8,000⚡ Brig Reactor Core (100% outside / 25% prisoner dmg) for mass breakout!"
      RankedGameMode.SYNDICATE_WARS ->
        "Direct [$syndicateTag] Clan-vs-Clan Ranked Ladder • H2H Record: 🏆 ${syndicateWarRecord.warTrophies} WAR TROPHIES (${syndicateWarRecord.warWins}W - ${syndicateWarRecord.warLosses}L)."
    }

  val isSyndicateWarsLocked = mode == RankedGameMode.SYNDICATE_WARS && !canQueueSyndicateWars.first

  Column(
    modifier =
      Modifier.fillMaxWidth()
        .clip(RoundedCornerShape(10.dp))
        .background(if (isCurrentActive) Color(0xCC0B1D33) else Color(0x9907101F))
        .border(
          width = if (isCurrentActive) 1.5.dp else 1.dp,
          color = if (isCurrentActive) modeAccent else modeAccent.copy(alpha = 0.45f),
          shape = RoundedCornerShape(10.dp),
        )
        .padding(9.dp)
        .testTag("nexus_mode_card_${mode.id}"),
    verticalArrangement = Arrangement.spacedBy(5.dp),
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
          text = "$modeIcon ${mode.title.uppercase()}",
          color = modeAccent,
          fontSize = 10.5.sp,
          fontWeight = FontWeight.Black,
          fontFamily = FontFamily.Monospace,
        )
        Box(
          modifier =
            Modifier.clip(RoundedCornerShape(4.dp))
              .background(modeAccent.copy(alpha = 0.18f))
              .padding(horizontal = 5.dp, vertical = 2.dp)
        ) {
          Text(
            text = mode.teamFormatBadge,
            color = modeAccent,
            fontSize = 7.5.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
          )
        }
      }

      // Strict Human-Only Pilot Counter Badge
      Box(
        modifier =
          Modifier.clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF06281E))
            .border(1.dp, Color(0xFF10B981), RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
      ) {
        Text(
          text = "🔥 $humanPilotsActive PILOTS ACTIVE",
          color = Color(0xFF34D399),
          fontSize = 8.sp,
          fontWeight = FontWeight.ExtraBold,
          fontFamily = FontFamily.Monospace,
        )
      }
    }

    Text(
      text = modeDescription,
      color = Color(0xFFCBD5E1),
      fontSize = 9.sp,
      lineHeight = 12.5.sp,
      fontFamily = FontFamily.SansSerif,
    )

    // Bracket Selector Chips for RIFT_SKIRMISH (2v2..5v5) and SYNDICATE_WARS (3v3, 5v5, 10v10 Breach)
    if (mode == RankedGameMode.RIFT_SKIRMISH) {
      Row(
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "BRACKET:",
          color = Color(0xFF94A3B8),
          fontSize = 8.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.Monospace,
        )
        listOf(2, 3, 4, 5).forEach { sz ->
          val sel = selectedBracket == sz
          Box(
            modifier =
              Modifier.clip(RoundedCornerShape(5.dp))
                .background(if (sel) modeAccent.copy(alpha = 0.25f) else Color(0xFF0F172A))
                .border(1.dp, if (sel) modeAccent else Color(0xFF334155), RoundedCornerShape(5.dp))
                .clickable { onSelectBracket(sz) }
                .padding(horizontal = 7.dp, vertical = 3.dp)
          ) {
            Text(
              text = "${sz}v${sz}",
              color = if (sel) Color.White else Color(0xFF94A3B8),
              fontSize = 8.sp,
              fontWeight = FontWeight.ExtraBold,
              fontFamily = FontFamily.Monospace,
            )
          }
        }
      }
    } else if (mode == RankedGameMode.SYNDICATE_WARS) {
      Row(
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "WAR FORMAT:",
          color = Color(0xFF94A3B8),
          fontSize = 8.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.Monospace,
        )
        listOf(3 to "3v3 STRIKE", 5 to "5v5 STRIKE", 10 to "10v10 STASIS BREACH").forEach { (sz, lbl) ->
          val sel = selectedBracket == sz
          Box(
            modifier =
              Modifier.clip(RoundedCornerShape(5.dp))
                .background(if (sel) modeAccent.copy(alpha = 0.25f) else Color(0xFF0F172A))
                .border(1.dp, if (sel) modeAccent else Color(0xFF334155), RoundedCornerShape(5.dp))
                .clickable { onSelectBracket(sz) }
                .padding(horizontal = 7.dp, vertical = 3.dp)
          ) {
            Text(
              text = lbl,
              color = if (sel) Color.White else Color(0xFF94A3B8),
              fontSize = 7.8.sp,
              fontWeight = FontWeight.ExtraBold,
              fontFamily = FontFamily.Monospace,
            )
          }
        }
      }
    }

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      if (isSyndicateWarsLocked) {
        Text(
          text = "🔒 REQUIRES ENLISTED SYNDICATE & 100% [$syndicateTag] ARMADA",
          color = Color(0xFFF87171),
          fontSize = 8.sp,
          fontWeight = FontWeight.ExtraBold,
          fontFamily = FontFamily.Monospace,
          modifier = Modifier.weight(1f),
        )
      } else {
        Text(
          text =
            if (isCurrentActive) "● CURRENTLY DEPLOYED IN THIS SECTOR"
            else "✓ ALWAYS OPEN • DROP-IN INSTANT MATCHMAKING",
          color = if (isCurrentActive) modeAccent else Color(0xFF64748B),
          fontSize = 8.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.Monospace,
          modifier = Modifier.weight(1f),
        )
      }

      Button(
        onClick = onDeploy,
        enabled = !isSyndicateWarsLocked,
        colors =
          ButtonDefaults.buttonColors(
            containerColor = modeAccent.copy(alpha = 0.22f),
            contentColor = Color.White,
            disabledContainerColor = Color(0x551E293B),
            disabledContentColor = Color(0xFF64748B),
          ),
        border = BorderStroke(1.dp, if (isSyndicateWarsLocked) Color(0xFF475569) else modeAccent),
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
        modifier =
          Modifier.height(30.dp)
            .testTag("nexus_deploy_${mode.id}"),
      ) {
        Text(
          text =
            if (armadaSize >= 2) "🚀 DEPLOY ARMADA ($armadaSize)"
            else "🚀 DEPLOY",
          fontSize = 8.8.sp,
          fontWeight = FontWeight.Black,
          fontFamily = FontFamily.Monospace,
        )
      }
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
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = "🏛️ [$syndicateTag] CUSTOM CHANNELS (${channels.size}/5 MAX)",
        color = Color(0xFFFACC15),
        fontSize = 9.sp,
        fontWeight = FontWeight.Black,
        fontFamily = FontFamily.Monospace,
      )
      Text(
        text = "✕",
        color = Color(0xFF94A3B8),
        fontSize = 10.sp,
        modifier = Modifier.clickable { onClose() }.padding(horizontal = 4.dp),
      )
    }
    Row(
      modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
      channels.forEachIndexed { idx, ch ->
        Row(
          modifier =
            Modifier.clip(RoundedCornerShape(6.dp))
              .background(Color(0xFF1E293B))
              .padding(horizontal = 6.dp, vertical = 3.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          Text(
            text = ch,
            color = Color.White,
            fontSize = 8.2.sp,
            fontFamily = FontFamily.Monospace,
          )
          if (idx > 0) {
            Text(
              text = "✕",
              color = Color(0xFFEF4444),
              fontSize = 8.5.sp,
              fontWeight = FontWeight.Black,
              modifier = Modifier.clickable { onDeleteChannel(ch) },
            )
          }
        }
      }
    }
    if (channels.size < 5) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Box(
          modifier =
            Modifier.weight(1f)
              .height(30.dp)
              .clip(RoundedCornerShape(6.dp))
              .background(Color(0xFF060D1A))
              .border(1.dp, Color(0x66F59E0B), RoundedCornerShape(6.dp))
              .padding(horizontal = 8.dp),
          contentAlignment = Alignment.CenterStart,
        ) {
          if (newChannelDraft.isEmpty()) {
            Text(
              text = "New channel (e.g. #war-room)",
              color = Color(0xFF64748B),
              fontSize = 9.sp,
            )
          }
          BasicTextField(
            value = newChannelDraft,
            onValueChange = onDraftChange,
            singleLine = true,
            cursorBrush = SolidColor(Color(0xFFFACC15)),
            textStyle = TextStyle(color = Color.White, fontSize = 9.5.sp, fontFamily = FontFamily.Monospace),
            modifier = Modifier.fillMaxWidth(),
          )
        }
        Button(
          onClick = onAddChannel,
          contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
          modifier = Modifier.height(30.dp),
        ) {
          Text("ADD", fontSize = 8.5.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
        }
      }
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
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = "🎟️ [$syndicateTag] INVITED SYNDICATE GUEST PASSES (${invitedGuests.size})",
        color = Color(0xFF38BDF8),
        fontSize = 9.sp,
        fontWeight = FontWeight.Black,
        fontFamily = FontFamily.Monospace,
      )
      Text(
        text = "✕",
        color = Color(0xFF94A3B8),
        fontSize = 10.sp,
        modifier = Modifier.clickable { onClose() }.padding(horizontal = 4.dp),
      )
    }
    if (invitedGuests.isNotEmpty()) {
      Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
      ) {
        invitedGuests.forEach { guest ->
          Row(
            modifier =
              Modifier.clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF1E293B))
                .padding(horizontal = 6.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            Text(text = guest, color = Color.White, fontSize = 8.2.sp, fontFamily = FontFamily.Monospace)
            Text(
              text = "✕",
              color = Color(0xFFEF4444),
              fontSize = 8.5.sp,
              fontWeight = FontWeight.Black,
              modifier = Modifier.clickable { onRevoke(guest) },
            )
          }
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
            .height(30.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF060D1A))
            .border(1.dp, Color(0x6638BDF8), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.CenterStart,
      ) {
        if (guestDraft.isEmpty()) {
          Text(
            text = "Invite pilot callsign to [$syndicateTag] channels…",
            color = Color(0xFF64748B),
            fontSize = 9.sp,
          )
        }
        BasicTextField(
          value = guestDraft,
          onValueChange = onDraftChange,
          singleLine = true,
          cursorBrush = SolidColor(Color(0xFF38BDF8)),
          textStyle = TextStyle(color = Color.White, fontSize = 9.5.sp, fontFamily = FontFamily.Monospace),
          modifier = Modifier.fillMaxWidth(),
        )
      }
      Button(
        onClick = onInvite,
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
        modifier = Modifier.height(30.dp),
      ) {
        Text("INVITE", fontSize = 8.5.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
      }
    }
  }
}

@Composable
private fun NexusShipChannelView(
  shipId: Int,
  channelHandle: String,
  spec: com.example.engine.ShipSpec,
  posts: List<LocalChannelPost>,
  chatInput: String,
  onChatInputChange: (String) -> Unit,
  onSendPost: () -> Unit,
  onEquipInHangar: () -> Unit,
  onAskNexusAiAboutHull: () -> Unit,
) {
  Column(
    modifier = Modifier.fillMaxSize(),
    verticalArrangement = Arrangement.spacedBy(6.dp),
  ) {
    // Pinned Hull Telemetry & Best-Pilot Header Card
    Row(
      modifier =
        Modifier.fillMaxWidth()
          .clip(RoundedCornerShape(10.dp))
          .background(Color(0xCC0A1628))
          .border(1.dp, Color(0x8838BDF8), RoundedCornerShape(10.dp))
          .padding(8.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      ShipSpritePreview(shipId = shipId, sizeDp = 42)
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = "$channelHandle // HULL #$shipId: ${spec.name.uppercase()} (${spec.role.uppercase()})",
          color = Color(0xFF00F5D4),
          fontSize = 10.sp,
          fontWeight = FontWeight.Black,
          fontFamily = FontFamily.Monospace,
        )
        Text(
          text =
            "MAX FLUX: ${spec.maxFlux.toInt()}⚡ • RECHARGE: ${spec.rechargeRate.toInt()}⚡/s • SPEED: ${spec.topSpeed.toInt()} px/s • THRUST: ${spec.thrust.toInt()}",
          color = Color(0xFFFACC15),
          fontSize = 8.2.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.Monospace,
        )
        Text(
          text =
            "PRIMARY: ${spec.primaryTitle} (${spec.primaryCost.toInt()}⚡) • SECONDARY: ${spec.secondaryTitle} (${spec.bombCost.toInt()}⚡)",
          color = Color(0xFFCBD5E1),
          fontSize = 8.sp,
          fontFamily = FontFamily.Monospace,
        )
      }
      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(
          modifier =
            Modifier.clip(RoundedCornerShape(6.dp))
              .background(Color(0xFF0E7490))
              .clickable { onEquipInHangar() }
              .padding(horizontal = 7.dp, vertical = 4.dp)
        ) {
          Text(
            text = "🚀 EQUIP HULL",
            color = Color.White,
            fontSize = 7.8.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
          )
        }
        Box(
          modifier =
            Modifier.clip(RoundedCornerShape(6.dp))
              .background(Color(0xFF311042))
              .border(1.dp, Color(0xFFB388FF), RoundedCornerShape(6.dp))
              .clickable { onAskNexusAiAboutHull() }
              .padding(horizontal = 7.dp, vertical = 4.dp)
        ) {
          Text(
            text = "🤖 ASK #NEXUS-AI",
            color = Color(0xFFE9D5FF),
            fontSize = 7.8.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
          )
        }
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
      item {
        Text(
          text = "📌 PINNED // ${spec.name} Tactical Notes: ${spec.primaryTitle} + ${spec.secondaryTitle}. Share loadouts, bank shots, and matchup tips below.",
          color = Color(0xFF94A3B8),
          fontSize = 9.sp,
          fontFamily = FontFamily.SansSerif,
        )
      }
      items(posts, key = { it.id }) { post ->
        ChannelPostRow(post)
      }
    }

    ChannelInputComposer(
      placeholder = "Post to $channelHandle pilot channel…",
      value = chatInput,
      onValueChange = onChatInputChange,
      onSend = onSendPost,
    )
  }
}

@Composable
private fun NexusTextChannelFeedView(
  channelTitle: String,
  channelSubtitle: String,
  globalComms: List<SyndicateChatMessage>,
  localPosts: List<LocalChannelPost>,
  chatInput: String,
  onChatInputChange: (String) -> Unit,
  onSend: () -> Unit,
) {
  Column(
    modifier = Modifier.fillMaxSize(),
    verticalArrangement = Arrangement.spacedBy(6.dp),
  ) {
    Column(
      modifier =
        Modifier.fillMaxWidth()
          .clip(RoundedCornerShape(8.dp))
          .background(Color(0xAA091426))
          .border(1.dp, Color(0x4438BDF8), RoundedCornerShape(8.dp))
          .padding(8.dp)
    ) {
      Text(
        text = channelTitle,
        color = Color(0xFF00F5D4),
        fontSize = 10.5.sp,
        fontWeight = FontWeight.Black,
        fontFamily = FontFamily.Monospace,
      )
      Text(
        text = channelSubtitle,
        color = Color(0xFF94A3B8),
        fontSize = 8.5.sp,
        fontFamily = FontFamily.SansSerif,
      )
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
      if (globalComms.isEmpty() && localPosts.isEmpty()) {
        item {
          Text(
            text = "Channel frequency online. Transmit a message to all connected pilots in this channel.",
            color = Color(0xFF64748B),
            fontSize = 9.5.sp,
            fontFamily = FontFamily.Monospace,
          )
        }
      }
      items(globalComms, key = { "g_${it.id}" }) { msg ->
        ChannelPostRow(
          LocalChannelPost(
            id = msg.id,
            senderCallsign = msg.senderCallsign,
            senderTag = msg.senderSyndicateTag.ifBlank { "GUEST" },
            timestamp = msg.timestampLabel,
            text =
              if (msg.isArmadaRecruitBanner)
                "🛡️ ARMADA RECRUIT [${msg.armadaName}] (${msg.armadaCurrentSize}/${msg.armadaMaxSize}) — ${msg.armadaObjective}"
              else msg.messageText,
            accentColor = Color(0xFF38BDF8),
          )
        )
      }
      items(localPosts, key = { it.id }) { post ->
        ChannelPostRow(post)
      }
    }

    ChannelInputComposer(
      placeholder = "Transmit message to $channelTitle…",
      value = chatInput,
      onValueChange = onChatInputChange,
      onSend = onSend,
    )
  }
}

@Composable
private fun ChannelPostRow(post: LocalChannelPost) {
  Row(
    modifier =
      Modifier.fillMaxWidth()
        .clip(RoundedCornerShape(6.dp))
        .background(Color(0x880B1528))
        .padding(horizontal = 8.dp, vertical = 5.dp),
    horizontalArrangement = Arrangement.spacedBy(6.dp),
    verticalAlignment = Alignment.Top,
  ) {
    Text(
      text = "[${post.senderTag}] ${post.senderCallsign}:",
      color = post.accentColor,
      fontSize = 9.sp,
      fontWeight = FontWeight.ExtraBold,
      fontFamily = FontFamily.Monospace,
    )
    Text(
      text = post.text,
      color = Color(0xFFF1F5F9),
      fontSize = 9.5.sp,
      fontFamily = FontFamily.SansSerif,
      modifier = Modifier.weight(1f),
    )
  }
}

@Composable
private fun ChannelInputComposer(
  placeholder: String,
  value: String,
  onValueChange: (String) -> Unit,
  onSend: () -> Unit,
) {
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
          .background(Color(0xCC091324))
          .border(1.dp, Color(0x6638BDF8), RoundedCornerShape(8.dp))
          .padding(horizontal = 10.dp),
      contentAlignment = Alignment.CenterStart,
    ) {
      if (value.isEmpty()) {
        Text(
          text = placeholder,
          color = Color(0xFF64748B),
          fontSize = 9.5.sp,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
      BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        cursorBrush = SolidColor(Color(0xFF00F5D4)),
        textStyle = TextStyle(color = Color.White, fontSize = 10.5.sp, fontFamily = FontFamily.SansSerif),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
        keyboardActions = KeyboardActions(onSend = { onSend() }),
        modifier = Modifier.fillMaxWidth(),
      )
    }
    Button(
      onClick = onSend,
      enabled = value.isNotBlank(),
      contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
      modifier = Modifier.height(36.dp),
    ) {
      Text("SEND ➢", fontSize = 9.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
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
      text = "⚡ #change-log // RIFT v39.0 SECTOR COMMAND HUB & 5-MODE EXPANSION",
      color = Color(0xFFFACC15),
      fontSize = 11.sp,
      fontWeight = FontWeight.Black,
      fontFamily = FontFamily.Monospace,
    )
    val notes =
      listOf(
        "🌐 NEXUS COMMAND HUB: Unified Battle.net × Discord × Brawl Stars hub with #nexus-ai streaming intelligence, 10 Ship Channels, Private [TAG] Syndicate Servers (up to 5 custom channels + Guest Passes), and Startup Boot Toggle.",
        "🛡️ BRAWL STARS ARMADA STAGING BAY: Displays your 1–5 Pilot Armada with each pilot's #1 Peak-Bounty Hull Avatar.",
        "⚔️ MODE 2 — RIFT SKIRMISH (2v2–5v5 Bo3 TDM): West vs East Warp Gates, 15 kills/round, +75❖ Rift Shards on match victory.",
        "⚡ MODE 3 — VOLATILE FLUX CORE (Powerball FFA): Hold the center Singularity Orb for +15 Bounty/s and +1 Overcharge Tier every 5s (up to +50% Dmg/Recharge & +40% Speed). Safe Zone entry or death ejects the Core at 1,650 px/s!",
        "🔒 MODE 4 — STASIS BREACH (10v10 Territory Jailbreak): Divided West/East playfield. Eliminations imprison victims in the enemy Stasis Brig. Destroy the 8,000⚡ Brig Reactor Core (100% outside / 25% prisoner damage) to trigger a mass jailbreak!",
        "🏴‍☠️ MODE 5 — SYNDICATE WARS: Ranked Clan-vs-Clan matchmaking with persistent War Trophies and Win/Loss records.",
        "🚀 10-SHIP ROSTER: Standard Hull #9 Interdictor (600⚡ • 18s CD • 15.0s Two-Way Stasis Bubble) + Modular Ship #10 Rift Frame.",
      )
    notes.forEach { line ->
      Text(
        text = "• $line",
        color = Color(0xFFE2E8F0),
        fontSize = 9.5.sp,
        lineHeight = 14.sp,
        fontFamily = FontFamily.SansSerif,
      )
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
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    Text(
      text = "📜 #rules // OFFICIAL RIFT SECTOR DIRECTIVES & FAIR-PLAY CODE",
      color = Color(0xFFA78BFA),
      fontSize = 11.sp,
      fontWeight = FontWeight.Black,
      fontFamily = FontFamily.Monospace,
    )
    val rules =
      listOf(
        "1. HUMAN-ONLY TELEMETRY: Online pilot counters in NEXUS and Ranked Mode cards strictly count real human pilots — AI bots are never counted as human players.",
        "2. SEAMLESS BOT REPLACEMENT: Every 20-ship arena sector backfills empty slots with AI bots and immediately replaces 1 AI bot per joining human pilot.",
        "3. SYNDICATE WARS ELIGIBILITY: Queuing for Ranked Syndicate Wars requires an enlisted [TAG] Syndicate and 100% same-Syndicate Armada wingmen.",
        "4. SAFE ZONE & OPEN-SPACE ANTI-AFK: Center Safe Zone disarms weapons and warps out idle ships after 15.0s. Open-space inactivity triggers -5% Max Flux/s drain.",
        "5. 100-POINT UNIVERSAL PARITY: All 9 standard hulls and the #10 Rift Frame adhere strictly to the 100-point budget over the Universal Baseline Hull Floor.",
      )
    rules.forEach { rule ->
      Text(
        text = rule,
        color = Color(0xFFE2E8F0),
        fontSize = 9.5.sp,
        lineHeight = 14.sp,
        fontFamily = FontFamily.SansSerif,
      )
    }
  }
}

@Composable
private fun NexusBugReportChannelView(
  selectedCategory: String,
  onSelectCategory: (String) -> Unit,
  description: String,
  onDescriptionChange: (String) -> Unit,
  statusBanner: String?,
  submittedPosts: List<LocalChannelPost>,
  onSubmit: () -> Unit,
) {
  val categories = listOf("PHYSICS / COLLISION", "UI / HUD", "WEAPONS / BALANCE", "MULTIPLAYER / SYNC")
  Column(
    modifier = Modifier.fillMaxSize(),
    verticalArrangement = Arrangement.spacedBy(6.dp),
  ) {
    Text(
      text = "🛠️ #bug-report // SUBMIT SECTOR ANOMALY OR BUG TELEMETRY",
      color = Color(0xFFFB7185),
      fontSize = 10.5.sp,
      fontWeight = FontWeight.Black,
      fontFamily = FontFamily.Monospace,
    )
    Row(
      modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      categories.forEach { cat ->
        val sel = selectedCategory == cat
        Box(
          modifier =
            Modifier.clip(RoundedCornerShape(6.dp))
              .background(if (sel) Color(0xFF4C0519) else Color(0xFF0F172A))
              .border(1.dp, if (sel) Color(0xFFFB7185) else Color(0xFF334155), RoundedCornerShape(6.dp))
              .clickable { onSelectCategory(cat) }
              .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
          Text(
            text = cat,
            color = if (sel) Color.White else Color(0xFF94A3B8),
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
          )
        }
      }
    }
    if (statusBanner != null) {
      Text(
        text = statusBanner,
        color = Color(0xFF34D399),
        fontSize = 9.sp,
        fontWeight = FontWeight.ExtraBold,
        fontFamily = FontFamily.Monospace,
      )
    }
    LazyColumn(
      modifier =
        Modifier.weight(1f)
          .fillMaxWidth()
          .clip(RoundedCornerShape(8.dp))
          .background(Color(0x55030712))
          .padding(8.dp),
      verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
      if (submittedPosts.isEmpty()) {
        item {
          Text(
            text = "Describe any physics, controller, or HUD issue below to log it directly to the channel.",
            color = Color(0xFF64748B),
            fontSize = 9.sp,
          )
        }
      }
      items(submittedPosts, key = { it.id }) { post ->
        ChannelPostRow(post)
      }
    }
    ChannelInputComposer(
      placeholder = "Describe bug or anomaly ($selectedCategory)…",
      value = description,
      onValueChange = onDescriptionChange,
      onSend = onSubmit,
    )
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
        .padding(12.dp),
    verticalArrangement = Arrangement.spacedBy(5.dp),
  ) {
    Text(
      text = "⚡ NEXUS CORE TELEMETRY LINK ESTABLISHED",
      color = Color(0xFF00F5D4),
      fontSize = 11.sp,
      fontWeight = FontWeight.ExtraBold,
      fontFamily = FontFamily.Monospace,
    )
    Text(
      text =
        "I am NEXUS, the omniscient Rift Core tactical intelligence inside your Sector Command Hub. Ask me anything about RIFT — exact laser pixel dimensions, Newtonian drift & wall-bounce equations, 100-point Interdictor & Rift Frame theorycrafting, Flux Core & Stasis Breach tactics, or global highest bounty records.",
      color = Color(0xFFE2E8F0),
      fontSize = 11.sp,
      lineHeight = 15.5.sp,
      fontFamily = FontFamily.SansSerif,
    )
    Text(
      text = "Tap a starter query above, switch channels on the left, or deploy your Armada on the right.",
      color = Color(0xFFC4B5FD),
      fontSize = 9.5.sp,
      fontWeight = FontWeight.Medium,
      fontFamily = FontFamily.Monospace,
    )
  }
}

@Composable
private fun NexusMessageBubble(message: NexusChatMessage) {
  val isUser = message.role == NexusRole.USER
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
  ) {
    Column(
      modifier =
        Modifier.widthIn(max = 590.dp)
          .clip(
            RoundedCornerShape(
              topStart = 12.dp,
              topEnd = 12.dp,
              bottomStart = if (isUser) 12.dp else 3.dp,
              bottomEnd = if (isUser) 3.dp else 12.dp,
            )
          )
          .background(
            if (isUser) {
              Color(0xB80E2A47)
            } else {
              Color(0xBF091324)
            }
          )
          .border(
            width = 1.dp,
            color = if (isUser) Color(0x8838BDF8) else Color(0x8800F5D4),
            shape =
              RoundedCornerShape(
                topStart = 12.dp,
                topEnd = 12.dp,
                bottomStart = if (isUser) 12.dp else 3.dp,
                bottomEnd = if (isUser) 3.dp else 12.dp,
              ),
          )
          .padding(horizontal = 12.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Text(
          text = if (isUser) "PILOT QUERY" else "NEXUS // RIFT CORE",
          color = if (isUser) Color(0xFF38BDF8) else Color(0xFF00F5D4),
          fontSize = 9.sp,
          fontWeight = FontWeight.ExtraBold,
          fontFamily = FontFamily.Monospace,
        )
        if (message.isStreaming) {
          Text(
            text = "● STREAMING TELEMETRY",
            color = Color(0xFFD8B4FE),
            fontSize = 8.5.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
          )
        }
      }

      if (message.text.isEmpty() && message.isStreaming) {
        Text(
          text = "Scanning live Rift Core engine matrices & leaderboard records…",
          color = Color(0xFF94A3B8),
          fontSize = 11.sp,
          fontFamily = FontFamily.Monospace,
        )
      } else {
        val formatted = remember(message.text) { formatNexusMarkdown(message.text) }
        Text(
          text = formatted,
          color = Color(0xFFF1F5F9),
          fontSize = 11.sp,
          lineHeight = 16.sp,
          fontFamily = FontFamily.SansSerif,
        )
      }
    }
  }
}

@Composable
private fun NexusErrorBanner(
  errorMessage: String,
  canRetry: Boolean,
  onRetry: () -> Unit,
) {
  Row(
    modifier =
      Modifier.fillMaxWidth()
        .clip(RoundedCornerShape(10.dp))
        .background(Color(0xCC2A0A12))
        .border(1.dp, Color(0xFFFF3B4E), RoundedCornerShape(10.dp))
        .padding(horizontal = 12.dp, vertical = 8.dp)
        .testTag("nexus_error_banner"),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = "⚠ RIFT CORE LINK INTERRUPTED",
        color = Color(0xFFFF3B4E),
        fontSize = 9.5.sp,
        fontWeight = FontWeight.ExtraBold,
        fontFamily = FontFamily.Monospace,
      )
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = errorMessage,
        color = Color(0xFFFECDD3),
        fontSize = 10.5.sp,
        fontFamily = FontFamily.SansSerif,
      )
    }
    if (canRetry) {
      Spacer(modifier = Modifier.width(8.dp))
      Button(
        onClick = onRetry,
        colors =
          ButtonDefaults.buttonColors(
            containerColor = Color(0xFF4C0519),
            contentColor = Color(0xFFFECDD3),
          ),
        border = BorderStroke(1.dp, Color(0xFFFF3B4E)),
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
        modifier =
          Modifier.minimumInteractiveComponentSize()
            .height(36.dp)
            .testTag("nexus_retry_button"),
      ) {
        Text(
          text = "↻ ${stringResource(R.string.nexus_retry)}",
          fontSize = 9.5.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.Monospace,
        )
      }
    }
  }
}

/**
 * Lightweight inline formatter converting `**bold**` and `` `code` `` spans into crisp telemetry highlights.
 */
private fun formatNexusMarkdown(raw: String): AnnotatedString = buildAnnotatedString {
  var i = 0
  val n = raw.length
  while (i < n) {
    if (i + 1 < n && raw[i] == '*' && raw[i + 1] == '*') {
      val close = raw.indexOf("**", i + 2)
      if (close != -1) {
        withStyle(
          SpanStyle(
            color = Color(0xFFFFE800),
            fontWeight = FontWeight.Bold,
          )
        ) {
          append(raw.substring(i + 2, close))
        }
        i = close + 2
        continue
      }
    }
    if (raw[i] == '`') {
      val close = raw.indexOf('`', i + 1)
      if (close != -1 && close > i + 1) {
        withStyle(
          SpanStyle(
            color = Color(0xFF00F5D4),
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold,
          )
        ) {
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
