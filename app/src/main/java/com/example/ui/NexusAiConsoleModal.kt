package com.example.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
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
import com.example.data.SyndicateChatMessage
import com.example.data.SyndicateWarRecord
import com.example.engine.*
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val NEXUS_STARTER_CHIPS = listOf(
 "How many pixels are Suppressor's lasers?",
 "Who holds the highest bounty ever?",
 "Theorycraft a 100-pt Rift Frame sniper build",
 "Analyze my Peak Bounty Run & Executioner",
 "Compare Eclipse Stasis Bubble vs Disruptor EMP",
 "Explain Flux Core, Stasis Breach & Rift Control",
)

enum class NexusHubLayoutMode { SPLIT_COMMAND, CHANNELS_ONLY, DECK_ONLY }

private data class LocalChannelPost(
 val id: String,
 val senderCallsign: String,
 val senderTag: String,
 val timestamp: String,
 val text: String,
 val accentColor: Color = Color(0xFF38BDF8),
)

private fun doesModeLockShipSelection(mode: RankedGameMode): Boolean =
 mode == RankedGameMode.RIFT_SKIRMISH || mode == RankedGameMode.SYNDICATE_WARS

@Composable
fun SwirlingRiftVortexGlyph(modifier: Modifier = Modifier, isStreaming: Boolean = false) {
 val transition = rememberInfiniteTransition(label = "nexus_rift_vortex")
 val outerDeg by transition.animateFloat(
  initialValue = 0f, targetValue = 360f,
  animationSpec = infiniteRepeatable(tween(if (isStreaming) 1250 else 3200, easing = LinearEasing), RepeatMode.Restart),
  label = "vortex_outer_deg",
 )
 val corePulse by transition.animateFloat(
  initialValue = 0.76f, targetValue = 1.0f,
  animationSpec = infiniteRepeatable(tween(if (isStreaming) 420 else 980, easing = LinearEasing), RepeatMode.Reverse),
  label = "vortex_core_pulse",
 )

 Canvas(modifier = modifier) {
  val r = minOf(size.width, size.height) * 0.5f
  val c = Offset(size.width * 0.5f, size.height * 0.5f)
  if (r <= 1f) return@Canvas
  val phaseRad = outerDeg * (PI.toFloat() / 180f)

  drawCircle(
   brush = Brush.radialGradient(
    colors = listOf(Color(0xFF02040A), Color(0xFF140B2E), Color(0xAA7C3AED), Color(0x6600F5D4), Color.Transparent),
    center = c, radius = r * 1.05f,
   ),
   radius = r, center = c,
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
    startAngle = outerDeg + arm * 120f, sweepAngle = 68f, useCenter = false,
    topLeft = outerTopLeft, size = outerSize,
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
   val armCol = when (arm % 3) {
    0 -> Color(0xFF00F5D4)
    1 -> Color(0xFFA855F7)
    else -> Color(0xFF38BDF8)
   }
   drawPath(path, armCol.copy(alpha = 0.88f * corePulse), style = Stroke(width = (r * 0.085f).coerceAtLeast(1.3f), cap = StrokeCap.Round))
  }

  val ovalW = r * 1.56f
  val ovalH = r * 0.56f
  drawArc(
   color = Color(0xCC00F5D4), startAngle = -outerDeg * 1.5f, sweepAngle = 250f, useCenter = false,
   topLeft = Offset(c.x - ovalW * 0.5f, c.y - ovalH * 0.5f), size = Size(ovalW, ovalH),
   style = Stroke(width = (r * 0.075f).coerceAtLeast(1.2f), cap = StrokeCap.Round),
  )
  drawCircle(Color(0xFF030712), radius = r * 0.28f, center = c)
  drawCircle(Color(0xFF00F5D4).copy(alpha = corePulse), radius = r * 0.29f, center = c, style = Stroke(width = (r * 0.09f).coerceAtLeast(1.4f)))
  drawCircle(Color.White.copy(alpha = corePulse), radius = r * 0.11f * corePulse, center = c)
 }
}

@Composable
fun SwirlingRiftHudOrb(modifier: Modifier = Modifier, isStreaming: Boolean = false, onClick: () -> Unit) {
 val desc = stringResource(R.string.nexus_orb_content_desc)
 val unlocked = RiftCalibrationState.isUiUnlockedForDragging
 Box(
  modifier = modifier.size(38.dp).clip(CircleShape).semantics { contentDescription = desc }
   .clickable(enabled = !unlocked) { onClick() }.testTag("hud_btn_nexus_orb"),
  contentAlignment = Alignment.Center,
 ) { SwirlingRiftVortexGlyph(modifier = Modifier.size(34.dp), isStreaming = isStreaming) }
}

@Composable
fun SwirlingRiftHeaderPill(onClick: () -> Unit, tag: String, isStreaming: Boolean = false) {
 val desc = stringResource(R.string.nexus_orb_content_desc)
 Row(
  modifier = Modifier.minimumInteractiveComponentSize().height(34.dp).clip(RoundedCornerShape(8.dp))
   .background(Brush.horizontalGradient(listOf(Color(0xFF09192F), Color(0xFF1E1038))))
   .border(1.dp, Brush.horizontalGradient(listOf(Color(0xFF00F5D4), Color(0xFFB388FF))), RoundedCornerShape(8.dp))
   .semantics { contentDescription = desc }.clickable { onClick() }.padding(horizontal = 9.dp).testTag(tag),
  verticalAlignment = Alignment.CenterVertically,
  horizontalArrangement = Arrangement.spacedBy(6.dp),
 ) {
  SwirlingRiftVortexGlyph(modifier = Modifier.size(18.dp), isStreaming = isStreaming)
  Text("NEXUS", color = Color(0xFF00F5D4), fontSize = 9.5.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, letterSpacing = 0.5.sp)
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
 engine: RiftGameEngine? = null,
 initialLayoutMode: NexusHubLayoutMode = NexusHubLayoutMode.DECK_ONLY,
 activeModuleKey: String = "NEXUS",
 onSelectNexusLobby: () -> Unit = {},
 onOpenProgression: () -> Unit = {},
 isCalibrationLivePeeking: Boolean = false,
 hideTopHeaderForFullScreenStage: Boolean = false,
 embeddedModuleContent: (@Composable () -> Unit)? = null,
) {
 val context = LocalContext.current
 var selectedChannelKey by remember { mutableStateOf("#nexus-ai") }
 var layoutMode by remember(initialLayoutMode) { mutableStateOf(initialLayoutMode) }
 var inputQuery by remember { mutableStateOf("") }
 var channelChatInput by remember { mutableStateOf("") }
 var skirmishBracket by remember { mutableIntStateOf(activeTeamSizeBracket.coerceIn(2, 5)) }
 var riftControlBracket by remember { mutableIntStateOf(if (activeTeamSizeBracket >= 10) 10 else if (activeTeamSizeBracket >= 5) 5 else 3) }
 var syndicateWarBracket by remember { mutableIntStateOf(if (activeTeamSizeBracket >= 10) 10 else if (activeTeamSizeBracket >= 5) 5 else 3) }
 var lockedTeamShipId by remember(activeShipId, engine?.localPlayer?.spec?.id) {
  mutableIntStateOf((engine?.localPlayer?.spec?.id ?: activeShipId).coerceIn(1, 10))
 }
 var pendingTeamDeployMode by remember { mutableStateOf<Pair<RankedGameMode, Int>?>(null) }

 var showCustomGameBuilder by remember { mutableStateOf(false) }
 var customBaseMode by remember { mutableStateOf(RankedGameMode.SECTOR_CONVERGENCE) }
 var customResetMinutes by remember { mutableIntStateOf(10) }
 var customTeamBracket by remember { mutableIntStateOf(5) }
 var customAllowArmadasInFfa by remember { mutableStateOf(true) }
 var customBotsEnabled by remember { mutableStateOf(true) }
 var customBulletSizeMult by remember { mutableStateOf(1.0f) }
 var customSpeedMult by remember { mutableStateOf(1.0f) }
 var customRechargeMult by remember { mutableStateOf(1.0f) }
 var customDamageMult by remember { mutableStateOf(1.0f) }
 var customCrateSpawnMult by remember { mutableStateOf(1.0f) }
 var customGamesRefreshTick by remember { mutableIntStateOf(0) }

 var showChannelManager by remember { mutableStateOf(false) }
 var newChannelDraft by remember { mutableStateOf("") }
 var showGuestManager by remember { mutableStateOf(false) }
 var guestCallsignDraft by remember { mutableStateOf("") }

 var bugCategory by remember { mutableStateOf("PHYSICS / COLLISION") }
 var bugDescription by remember { mutableStateOf("") }
 var bugStatusBanner by remember { mutableStateOf<String?>(null) }

 val localChannelPosts = remember { mutableStateMapOf<String, MutableList<LocalChannelPost>>() }
 val keyboardController = LocalSoftwareKeyboardController.current
 val listState = rememberLazyListState()
 val isStreaming = streamState is NexusStreamState.Loading || streamState is NexusStreamState.Streaming

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

 fun postToChannel(channelKey: String, rawText: String, accent: Color = Color(0xFF38BDF8)) {
  val clean = rawText.trim()
  if (clean.isEmpty()) return
  val tag = syndicateTag.ifBlank { "GUEST" }
  val list = localChannelPosts.getOrPut(channelKey) { mutableStateListOf() }
  list.add(LocalChannelPost("post_${System.currentTimeMillis()}_${list.size}", callsign, tag, "LIVE", clean, accent))
  onSendChannelMessage(channelKey, clean)
 }

 val shipChannels = remember {
  listOf(1 to "#viper", 2 to "#ricochet", 3 to "#suppressor", 4 to "#goliath", 5 to "#twinfang", 6 to "#disruptor", 7 to "#valkyrie", 8 to "#manta", 9 to "#eclipse", 10 to "#rift-frame")
 }

 val isCompactPhone = LocalConfiguration.current.screenHeightDp < 480
 val cleanSyndicateTag = if (syndicateTag.isBlank() || syndicateTag in setOf("FREE", "SOLO", "UNAFFILIATED")) "GUEST" else syndicateTag
 val isCalibrationPeekActive = activeModuleKey == "CALIBRATE" && isCalibrationLivePeeking

 Box(
  modifier = Modifier.fillMaxSize()
   .background(if (isCalibrationPeekActive) Color(0x14020611) else Color(0x78020611))
   .windowInsetsPadding(WindowInsets.safeDrawing).imePadding()
   .padding(if (isCompactPhone) PaddingValues(horizontal = 8.dp, vertical = 4.dp) else PaddingValues(horizontal = 10.dp, vertical = 6.dp))
   .testTag("nexus_ai_console_modal"),
  contentAlignment = Alignment.Center,
 ) {
  Surface(
   modifier = Modifier.fillMaxSize().border(
    1.2.dp,
    Brush.linearGradient(listOf(Color(0xFF00F5D4), Color(0xCCB388FF), Color(0xFF38BDF8))),
    RoundedCornerShape(12.dp),
   ),
   color = if (isCalibrationPeekActive) Color(0x40060D1A) else Color(0xF2060D1A),
   shape = RoundedCornerShape(12.dp),
  ) {
   Column(
    modifier = Modifier.fillMaxSize().padding(if (isCompactPhone) 8.dp else 14.dp),
    verticalArrangement = Arrangement.spacedBy(if (isCompactPhone) 4.dp else 6.dp),
   ) {
    val isFullScreenForgeStage = hideTopHeaderForFullScreenStage && activeModuleKey == "FORGE"
    if (!isFullScreenForgeStage) {
     Row(
      modifier = Modifier.fillMaxWidth().then(if (isCompactPhone) Modifier.height(30.dp) else Modifier),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
     ) {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
       SwirlingRiftVortexGlyph(modifier = Modifier.size(if (isCompactPhone) 24.dp else 32.dp), isStreaming = isStreaming)
       if (isCompactPhone) {
        Text("NEXUS // COMMAND HUB", color = Color(0xFF00F5D4), fontSize = 11.5.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Box(
         modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(Color(0xFF081A2C))
          .border(1.dp, Color(0xFF38BDF8), RoundedCornerShape(6.dp)).padding(horizontal = 6.dp, vertical = 2.dp),
        ) {
         Text("🟢 $totalOnlinePilots ONLINE • [$cleanSyndicateTag] • LVL $pilotLevel", color = Color(0xFF34D399), fontSize = 8.2.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace, maxLines = 1)
        }
       } else {
        Column {
         Text("NEXUS // SECTOR COMMAND HUB & MATCHMAKING", color = Color(0xFF00F5D4), fontSize = 13.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, letterSpacing = 0.6.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
         Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.horizontalScroll(rememberScrollState())) {
          Text("🟢 $totalOnlinePilots PILOTS ONLINE", color = Color(0xFF34D399), fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace)
          Text("🏛️ $syndicateOnlineCount [$cleanSyndicateTag] MEMBERS ONLINE", color = Color(0xFFFBBF24), fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace)
          Text("🛡️ $presetArmadasReadyCount PRESET ARMADAS READY", color = Color(0xFF60A5FA), fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace)
          Text("• LVL $pilotLevel ($pointBudget PTS) • TOP: $highestBountySummary", color = Color(0xFF94A3B8), fontSize = 8.8.sp, fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.Monospace)
         }
        }
       }
      }

      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
       Box(
        modifier = Modifier.shadow(2.dp, RoundedCornerShape(8.dp)).clip(RoundedCornerShape(8.dp))
         .background(if (bootToNexusHub) Color(0xFF092238) else Color(0xFF231808))
         .border(1.2.dp, if (bootToNexusHub) Color(0xFF00F5D4) else Color(0xFFF59E0B), RoundedCornerShape(8.dp))
         .clickable { onToggleBootToNexusHub(!bootToNexusHub) }
         .padding(horizontal = 8.dp, vertical = if (isCompactPhone) 4.dp else 6.dp)
         .testTag("nexus_boot_mode_toggle"),
       ) {
        Text(
         if (bootToNexusHub) "⚙️ STARTUP: NEXUS HUB" else "🚀 STARTUP: QUICK-JOIN",
         color = if (bootToNexusHub) Color(0xFF00F5D4) else Color(0xFFFACC15),
         fontSize = 8.5.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace, maxLines = 1,
        )
       }

       if (activeModuleKey == "NEXUS" && messages.isNotEmpty() && selectedChannelKey == "#nexus-ai") {
        Button(
         onClick = onClearHistory,
         colors = ButtonDefaults.buttonColors(containerColor = Color(0xAA0F172A), contentColor = Color(0xFF94A3B8)),
         border = BorderStroke(1.2.dp, Color(0x66475569)), shape = RoundedCornerShape(8.dp),
         contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
         modifier = Modifier.height(if (isCompactPhone) 28.dp else 32.dp).shadow(2.dp, RoundedCornerShape(8.dp)).testTag("nexus_clear_button"),
        ) { Text("↺ CLEAR", fontSize = 8.8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace) }
       }

       val isInMatch = engine?.hasJoinedMatch == true
       if (isInMatch) {
        Button(
         onClick = onLeaveMatch,
         colors = ButtonDefaults.buttonColors(containerColor = Color(0xCC2A151B), contentColor = Color(0xFFFB7185)),
         border = BorderStroke(1.2.dp, Color(0xFFFB7185)), shape = RoundedCornerShape(8.dp),
         contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
         modifier = Modifier.height(if (isCompactPhone) 28.dp else 32.dp).shadow(2.dp, RoundedCornerShape(8.dp)).testTag("nexus_leave_match_button"),
        ) { Text(if (isCompactPhone) "⏹ LEAVE" else "⏹ LEAVE MATCH", fontSize = 8.8.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace, maxLines = 1) }
       }

       Button(
        onClick = onCloseModal,
        colors = ButtonDefaults.buttonColors(
         containerColor = if (isInMatch) Color(0xCC0D2538) else Color(0xCC0B1C2C),
         contentColor = if (isInMatch) Color(0xFF00F5D4) else Color(0xFF38BDF8),
        ),
        border = BorderStroke(1.2.dp, if (isInMatch) Color(0xFF00F5D4) else Color(0xFF38BDF8)),
        shape = RoundedCornerShape(8.dp), contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
        modifier = Modifier.height(if (isCompactPhone) 28.dp else 32.dp).shadow(2.dp, RoundedCornerShape(8.dp)).testTag("nexus_close_button"),
       ) {
        Text(
         when {
          !isInMatch && isCompactPhone -> "👁️ ORBIT"
          !isInMatch -> "👁️ VIEW ORBIT"
          isCompactPhone -> "✕ ARENA"
          else -> "✕ RETURN TO ARENA"
         },
         fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace, maxLines = 1,
        )
       }
      }
     }

     NexusMasterModuleNavigationRibbon(
      activeModuleKey = activeModuleKey,
      onSelectNexusLobby = onSelectNexusLobby,
      onOpenHangar = onOpenHangar,
      onOpenForge = onOpenForge,
      onOpenProgression = onOpenProgression,
      onOpenArmadaModal = onOpenArmadaModal,
      onOpenSyndicateDirectory = onOpenSyndicateDirectory,
      onOpenDossier = onOpenDossier,
      onOpenCodex = onOpenCodex,
      onOpenSettings = onOpenSettings,
      onOpenCalibration = onOpenCalibration,
      showReturnToArenaButton = false,
      onReturnToArena = onCloseModal,
     )
    }

    if (activeModuleKey != "NEXUS" && embeddedModuleContent != null) {
     Box(modifier = Modifier.weight(1f).fillMaxWidth().testTag("nexus_embedded_module_container")) {
      embeddedModuleContent()
     }
    } else {
     Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
      Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
       NexusViewModeTab("🎯 MODE SELECTION & MATCHMAKING (FULL VIEW)", layoutMode == NexusHubLayoutMode.DECK_ONLY, { layoutMode = NexusHubLayoutMode.DECK_ONLY }, "nexus_tab_deck_view")
       NexusViewModeTab("💬 COMMS CHANNELS & NEXUS AI", layoutMode == NexusHubLayoutMode.CHANNELS_ONLY, { layoutMode = NexusHubLayoutMode.CHANNELS_ONLY }, "nexus_tab_channels_view")
       NexusViewModeTab("⊞ SPLIT COMMAND VIEW", layoutMode == NexusHubLayoutMode.SPLIT_COMMAND, { layoutMode = NexusHubLayoutMode.SPLIT_COMMAND }, "nexus_tab_split_view")
      }
      val activeCustomTitle = engine?.currentCustomGame?.title
      Text(
       when {
        engine?.hasJoinedMatch != true -> "🛰️ ORBITING NEXUS — NOT IN A MATCH • HULL: $activeShipSummary"
        activeCustomTitle != null -> "ACTIVE CUSTOM: ${activeCustomTitle.uppercase()} • HULL: $activeShipSummary"
        else -> "ACTIVE MATCH: ${activeRankedMode.title.uppercase()} • HULL: $activeShipSummary"
       },
       color = if (engine?.hasJoinedMatch == true) Color(0xFF00F5D4) else Color(0xFF94A3B8),
       fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, maxLines = 1,
      )
     }

     Row(modifier = Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      if (layoutMode != NexusHubLayoutMode.DECK_ONLY) {
       Column(
        modifier = Modifier.width(196.dp).fillMaxHeight().clip(RoundedCornerShape(12.dp))
         .background(Color(0x99040914)).border(1.dp, Color(0x4438BDF8), RoundedCornerShape(12.dp))
         .padding(8.dp).verticalScroll(rememberScrollState()).testTag("nexus_channel_directory"),
        verticalArrangement = Arrangement.spacedBy(4.dp),
       ) {
        ChannelCategoryHeader("// NEXUS CORE")
        ChannelNavItem("🧠 #nexus-ai", "AI LIVE", selectedChannelKey == "#nexus-ai", Color(0xFF00F5D4), { selectedChannelKey = "#nexus-ai" }, "nexus_ch_nexus_ai")
        ChannelNavItem("🌐 #main-lobby", "$totalOnlinePilots", selectedChannelKey == "#main-lobby", Color(0xFF38BDF8), { selectedChannelKey = "#main-lobby" }, "nexus_ch_main_lobby")
        ChannelNavItem("📋 #change-log", "v54.0", selectedChannelKey == "#change-log", Color(0xFFFACC15), { selectedChannelKey = "#change-log" }, "nexus_ch_change_log")
        ChannelNavItem("📜 #rules", "FAIR", selectedChannelKey == "#rules", Color(0xFFA78BFA), { selectedChannelKey = "#rules" }, "nexus_ch_rules")
        ChannelNavItem("🛠️ #bug-report", "FORM", selectedChannelKey == "#bug-report", Color(0xFFFB7185), { selectedChannelKey = "#bug-report" }, "nexus_ch_bug_report")

        Spacer(modifier = Modifier.height(4.dp))
        ChannelCategoryHeader("// [$cleanSyndicateTag] ${syndicateName.take(14)}")
        if (canAccessSyndicateServer) {
         syndicateCustomChannels.forEachIndexed { idx, chName ->
          val key = "syndicate:$chName"
          ChannelNavItem("🏛️ $chName", if (idx == 0) "CORE" else "CLAN", selectedChannelKey == key, Color(0xFFF59E0B), { selectedChannelKey = key }, "nexus_ch_syndicate_$idx")
         }
         Row(modifier = Modifier.fillMaxWidth().padding(top = 2.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
          Box(
           modifier = Modifier.weight(1f).clip(RoundedCornerShape(6.dp)).background(Color(0xFF1E293B))
            .border(1.dp, Color(0x66F59E0B), RoundedCornerShape(6.dp))
            .clickable { showChannelManager = !showChannelManager }.padding(vertical = 4.dp)
            .testTag("nexus_btn_manage_syndicate_channels"),
           contentAlignment = Alignment.Center,
          ) { Text("+ CH (${syndicateCustomChannels.size}/5)", color = Color(0xFFFBBF24), fontSize = 8.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace) }
          Box(
           modifier = Modifier.weight(1f).clip(RoundedCornerShape(6.dp)).background(Color(0xFF1E293B))
            .border(1.dp, Color(0x6638BDF8), RoundedCornerShape(6.dp))
            .clickable { showGuestManager = !showGuestManager }.padding(vertical = 4.dp)
            .testTag("nexus_btn_syndicate_guest_pass"),
           contentAlignment = Alignment.Center,
          ) { Text("🎟️ GUESTS (${syndicateInvitedGuests.size})", color = Color(0xFF38BDF8), fontSize = 8.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace) }
         }
        } else {
         ChannelNavItem("🔒 Syndicate Locked", "JOIN", selectedChannelKey == "syndicate:locked", Color(0xFF64748B), { selectedChannelKey = "syndicate:locked" }, "nexus_ch_syndicate_locked")
        }

        if (armadaSlots.size >= 2 || activeArmadaName.isNotEmpty()) {
         Spacer(modifier = Modifier.height(4.dp))
         ChannelCategoryHeader("// ARMADA PARTY COMMS")
         val armLabel = activeArmadaName.ifEmpty { "SQUAD" }
         ChannelNavItem("🛡️ #armada-chat ($armLabel)", "${armadaSlots.size.coerceAtLeast(1)}/5", selectedChannelKey == "#armada-chat", Color(0xFF60A5FA), { selectedChannelKey = "#armada-chat" }, "nexus_ch_armada_chat")
        }

        Spacer(modifier = Modifier.height(4.dp))
        ChannelCategoryHeader("// SHIP CHANNELS (1–10)")
        shipChannels.forEach { (shipId, chHandle) ->
         ChannelNavItem("🚀 $chHandle", "#$shipId", selectedChannelKey == chHandle, if (shipId == 10) Color(0xFFD8B4FE) else Color(0xFF38BDF8), { selectedChannelKey = chHandle }, "nexus_ch_ship_$shipId")
        }
       }

       Column(
        modifier = Modifier.weight(if (layoutMode == NexusHubLayoutMode.SPLIT_COMMAND) 1.05f else 1f)
         .fillMaxHeight().clip(RoundedCornerShape(12.dp)).background(Color(0x88040914))
         .border(1.dp, Color(0x4438BDF8), RoundedCornerShape(12.dp)).padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
       ) {
        if (showChannelManager && canAccessSyndicateServer) {
         SyndicateChannelManagerCard(
          syndicateTag = cleanSyndicateTag, channels = syndicateCustomChannels, newChannelDraft = newChannelDraft,
          onDraftChange = { newChannelDraft = it },
          onAddChannel = { if (newChannelDraft.isNotBlank()) { onAddSyndicateChannel(newChannelDraft); newChannelDraft = "" } },
          onDeleteChannel = onDeleteSyndicateChannel, onClose = { showChannelManager = false },
         )
        }
        if (showGuestManager && canAccessSyndicateServer) {
         SyndicateGuestPassManagerCard(
          syndicateTag = cleanSyndicateTag, invitedGuests = syndicateInvitedGuests, guestDraft = guestCallsignDraft,
          onDraftChange = { guestCallsignDraft = it },
          onInvite = { if (guestCallsignDraft.isNotBlank()) { onInviteSyndicateGuest(guestCallsignDraft); guestCallsignDraft = "" } },
          onRevoke = onRevokeSyndicateGuest, onClose = { showGuestManager = false },
         )
        }

        when {
         selectedChannelKey == "#nexus-ai" -> {
          Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
           NEXUS_STARTER_CHIPS.forEachIndexed { idx, chipPrompt ->
            Box(
             modifier = Modifier.clip(RoundedCornerShape(14.dp)).background(Color(0x990D1B2E))
              .border(1.dp, Color(0x6638BDF8), RoundedCornerShape(14.dp))
              .clickable(enabled = !isStreaming) { onSendQuestion(chipPrompt) }
              .padding(horizontal = 9.dp, vertical = 5.dp).testTag("nexus_quick_chip_$idx"),
            ) {
             Text("✦ $chipPrompt", color = if (isStreaming) Color(0xFF64748B) else Color(0xFFE0F2FE), fontSize = 9.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            }
           }
          }

          LazyColumn(
           state = listState,
           modifier = Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(10.dp))
            .background(Color(0x59030712)).border(1.dp, Color(0x3338BDF8), RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp).testTag("nexus_message_list"),
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
            modifier = Modifier.weight(1f).height(if (isCompactPhone) 34.dp else 38.dp).clip(RoundedCornerShape(8.dp))
             .background(Color(0xFF050B14))
             .border(1.dp, if (isStreaming) Color(0x55475569) else Color(0xFF00F5D4), RoundedCornerShape(8.dp))
             .padding(horizontal = 10.dp).testTag("nexus_query_input_box"),
            contentAlignment = Alignment.CenterStart,
           ) {
            if (inputQuery.isEmpty()) {
             Text(stringResource(R.string.nexus_input_placeholder), color = Color(0xFF64748B), fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            BasicTextField(
             value = inputQuery, onValueChange = { inputQuery = it }, enabled = !isStreaming, singleLine = true,
             cursorBrush = SolidColor(Color(0xFF00F5D4)),
             textStyle = TextStyle(color = Color.White, fontSize = 11.sp),
             keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
             keyboardActions = KeyboardActions(onSend = { submitNexusAiQuery() }),
             modifier = Modifier.fillMaxWidth().testTag("nexus_query_input"),
            )
           }

           Button(
            onClick = { submitNexusAiQuery() }, enabled = inputQuery.isNotBlank() && !isStreaming,
            colors = ButtonDefaults.buttonColors(
             containerColor = Color(0xFF0E7490), contentColor = Color.White,
             disabledContainerColor = Color(0x661E293B), disabledContentColor = Color(0xFF64748B),
            ),
            border = BorderStroke(1.2.dp, if (inputQuery.isNotBlank() && !isStreaming) Color(0xFF00F5D4) else Color(0x44475569)),
            shape = RoundedCornerShape(8.dp), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            modifier = Modifier.height(if (isCompactPhone) 34.dp else 38.dp).shadow(2.dp, RoundedCornerShape(8.dp)).testTag("nexus_send_button"),
           ) {
            Text(if (isStreaming) "SYNCING…" else "${stringResource(R.string.nexus_transmit)} ➢", fontSize = 9.5.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
           }
          }
         }
         selectedChannelKey == "#change-log" -> NexusChangelogChannelView()
         selectedChannelKey == "#rules" -> NexusRulesChannelView()
         selectedChannelKey == "#bug-report" -> NexusBugReportChannelView(
          selectedCategory = bugCategory, onSelectCategory = { bugCategory = it },
          description = bugDescription, onDescriptionChange = { bugDescription = it },
          statusBanner = bugStatusBanner,
          submittedPosts = localChannelPosts["#bug-report"] ?: emptyList(),
          onSubmit = {
           val clean = bugDescription.trim()
           if (clean.isNotEmpty()) {
            postToChannel("#bug-report", "[$bugCategory] $clean", Color(0xFFFB7185))
            onSubmitBugReport(bugCategory, clean)
            bugDescription = ""
            bugStatusBanner = "✓ ANOMALY LOGGED TO NEXUS CORE TELEMETRY"
            keyboardController?.hide()
           }
          },
         )
         selectedChannelKey == "syndicate:locked" -> {
          Column(
           modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(10.dp)).background(Color(0x66091324))
            .border(1.dp, Color(0x66F59E0B), RoundedCornerShape(10.dp)).padding(18.dp),
           horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
          ) {
           Text("🔒 PRIVATE SYNDICATE SERVER", color = Color(0xFFFACC15), fontSize = 14.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
           Spacer(modifier = Modifier.height(6.dp))
           Text(
            "ENLIST IN A SYNDICATE OR RECEIVE A GUEST INVITE TO ACCESS PRIVATE [TAG] SYNDICATE CHANNELS & SYNDICATE WARS.",
            color = Color(0xFFCBD5E1), fontSize = 11.sp, textAlign = TextAlign.Center,
           )
           Spacer(modifier = Modifier.height(12.dp))
           Button(
            onClick = onOpenSyndicateDirectory,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF78350F), contentColor = Color(0xFFFEF08A)),
            border = BorderStroke(1.dp, Color(0xFFF59E0B)), shape = RoundedCornerShape(8.dp),
           ) { Text("🏛️ OPEN SYNDICATE DIRECTORY", fontSize = 10.5.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace) }
          }
         }
         shipChannels.any { it.second == selectedChannelKey } -> {
          val shipId = shipChannels.first { it.second == selectedChannelKey }.first
          val spec = RiftCodex.getShipById(shipId)
          NexusShipChannelView(
           shipId = shipId, channelHandle = selectedChannelKey, spec = spec,
           posts = localChannelPosts[selectedChannelKey] ?: emptyList(),
           chatInput = channelChatInput, onChatInputChange = { channelChatInput = it },
           onSendPost = {
            postToChannel(selectedChannelKey, channelChatInput, Color(0xFF38BDF8))
            channelChatInput = ""
            keyboardController?.hide()
           },
           onEquipInHangar = { onOpenHangarForShip(shipId) },
           onAskNexusAiAboutHull = {
            selectedChannelKey = "#nexus-ai"
            onSendQuestion("Give me a complete combat breakdown, 100-pt stat ratios, and tactical tips for ${spec.name}.")
           },
          )
         }
         else -> {
          val isSyndicateCh = selectedChannelKey.startsWith("syndicate:")
          val displayTitle = when {
           isSyndicateCh -> "🏛️ [$cleanSyndicateTag] ${selectedChannelKey.removePrefix("syndicate:")}"
           selectedChannelKey == "#armada-chat" -> "🛡️ #armada-chat [${activeArmadaName.ifEmpty { "SQUAD" }}]"
           else -> "🌐 #main-lobby // GLOBAL SECTOR COMMS"
          }
          val accent = if (isSyndicateCh) Color(0xFFF59E0B) else Color(0xFF38BDF8)
          NexusTextChannelFeedView(
           channelTitle = displayTitle,
           channelSubtitle = when {
            isSyndicateCh -> "Private [$cleanSyndicateTag] $syndicateName Server • Members & Invited Guests (${syndicateInvitedGuests.size} Guests)"
            selectedChannelKey == "#armada-chat" -> "Encrypted Armada Wingman Comms • Friendly Fire Disabled Across Squad"
            else -> "Live Cross-Sector Pilot Feed • Zero Bot Clutter ($totalOnlinePilots Humans Online)"
           },
           globalComms = if (selectedChannelKey == "#main-lobby") lobbyMessages else emptyList(),
           localPosts = localChannelPosts[selectedChannelKey] ?: emptyList(),
           chatInput = channelChatInput, onChatInputChange = { channelChatInput = it },
           onSend = { postToChannel(selectedChannelKey, channelChatInput, accent); channelChatInput = ""; keyboardController?.hide() },
           onQuickPing = { pingText -> postToChannel(selectedChannelKey, pingText, accent) },
          )
         }
        }
       }
      }

      val renderArmadaDeckShared: @Composable (Boolean) -> Unit = { switchToSplitOnChat ->
       HolographicArmadaStagingDeck(
        callsign = callsign, syndicateTag = cleanSyndicateTag, bestShipId = bestShipId,
        activeArmadaName = activeArmadaName, armadaSlots = armadaSlots,
        onResolvePilotBestShipId = onResolvePilotBestShipId, onOpenArmadaModal = onOpenArmadaModal,
        onQuickFormSyndicateArmada = onQuickFormSyndicateArmada,
        onOpenArmadaChat = { selectedChannelKey = "#armada-chat"; if (switchToSplitOnChat) layoutMode = NexusHubLayoutMode.SPLIT_COMMAND },
       )
      }
      val renderShipLockShared: @Composable () -> Unit = {
       PreMatchTeamShipLockCard(
        selectedShipId = lockedTeamShipId, isCurrentlyLockedInMatch = engine?.isShipLockedForMatch == true,
        pendingTeamMode = pendingTeamDeployMode,
        onSelectShip = { chosenId ->
         lockedTeamShipId = chosenId.coerceIn(1, 10)
         if (engine?.isShipLockedForMatch != true) engine?.switchLocalPlayerShip(lockedTeamShipId, forceFromHangar = true)
        },
        onConfirmLockAndDeploy = { mode, bracket ->
         engine?.isShipLockedForMatch = false
         engine?.switchLocalPlayerShip(lockedTeamShipId, forceFromHangar = true)
         pendingTeamDeployMode = null
         onDeployToRankedMode(mode, bracket)
         onCloseModal()
        },
        onCancelPending = { pendingTeamDeployMode = null },
       )
      }
      val renderRankedModeCardShared: @Composable (RankedGameMode) -> Unit = { mode ->
       val humanCount = humanPilotCountByMode(mode).coerceAtLeast(0)
       val isCurrentMode = engine?.hasJoinedMatch == true && activeRankedMode == mode && engine.currentCustomGame == null
       val bracketForMode = when (mode) {
        RankedGameMode.RIFT_SKIRMISH -> skirmishBracket
        RankedGameMode.RIFT_CONTROL -> riftControlBracket
        RankedGameMode.SYNDICATE_WARS -> syndicateWarBracket
        RankedGameMode.STASIS_BREACH -> 10
        else -> 20
       }
       RankedModeDeploymentCard(
        mode = mode, isCurrentActive = isCurrentMode, humanPilotsActive = humanCount,
        armadaSize = armadaSlots.size.coerceAtLeast(1), syndicateTag = cleanSyndicateTag,
        selectedBracket = bracketForMode,
        onSelectBracket = { newBracket ->
         when (mode) {
          RankedGameMode.RIFT_SKIRMISH -> skirmishBracket = newBracket.coerceIn(2, 5)
          RankedGameMode.RIFT_CONTROL -> riftControlBracket = newBracket
          RankedGameMode.SYNDICATE_WARS -> syndicateWarBracket = newBracket
          else -> {}
         }
        },
        canQueueSyndicateWars = canQueueSyndicateWars, syndicateWarRecord = syndicateWarRecord,
        onDeploy = {
         if (doesModeLockShipSelection(mode)) {
          engine?.isShipLockedForMatch = false
          engine?.switchLocalPlayerShip(lockedTeamShipId, forceFromHangar = true)
         }
         pendingTeamDeployMode = null
         onDeployToRankedMode(mode, bracketForMode)
         onCloseModal()
        },
       )
      }
      val renderCustomSandboxShared: @Composable () -> Unit = {
       CustomGamesSectionCard(
        engine = engine, callsign = callsign, lockedTeamShipId = lockedTeamShipId,
        refreshTick = customGamesRefreshTick, showBuilder = showCustomGameBuilder,
        onToggleBuilder = { showCustomGameBuilder = !showCustomGameBuilder },
        baseMode = customBaseMode, onSelectBaseMode = { customBaseMode = it },
        resetMinutes = customResetMinutes, onSelectResetMinutes = { customResetMinutes = it },
        teamBracket = customTeamBracket, onSelectTeamBracket = { customTeamBracket = it },
        allowArmadasInFfa = customAllowArmadasInFfa, onToggleAllowArmadasInFfa = { customAllowArmadasInFfa = it },
        botsEnabled = customBotsEnabled, onToggleBotsEnabled = { customBotsEnabled = it },
        bulletSizeMult = customBulletSizeMult, onSelectBulletSizeMult = { customBulletSizeMult = it },
        speedMult = customSpeedMult, onSelectSpeedMult = { customSpeedMult = it },
        rechargeMult = customRechargeMult, onSelectRechargeMult = { customRechargeMult = it },
        damageMult = customDamageMult, onSelectDamageMult = { customDamageMult = it },
        crateSpawnMult = customCrateSpawnMult, onSelectCrateSpawnMult = { customCrateSpawnMult = it },
        onReturnToOfficialRankedFfa = { onDeployToRankedMode(RankedGameMode.SECTOR_CONVERGENCE, 20); customGamesRefreshTick++ },
        onJoinCustomGame = { instanceId ->
         val target = engine?.activeCustomGames?.firstOrNull { it.instanceId == instanceId }
         if (target != null && doesModeLockShipSelection(target.baseMode)) {
          engine?.isShipLockedForMatch = false
          engine?.switchLocalPlayerShip(lockedTeamShipId, forceFromHangar = true)
         }
         engine?.joinCustomGame(instanceId)
         RiftCalibrationState.applyCalibrationChangesLive(engine, context)
         customGamesRefreshTick++
         onCloseModal()
        },
        onLaunchCustomGame = {
         if (doesModeLockShipSelection(customBaseMode)) {
          engine?.isShipLockedForMatch = false
          engine?.switchLocalPlayerShip(lockedTeamShipId, forceFromHangar = true)
         }
         val modeShort = when (customBaseMode) {
          RankedGameMode.SECTOR_CONVERGENCE -> "FFA"
          RankedGameMode.RIFT_SKIRMISH -> "Skirmish"
          RankedGameMode.FLUX_CORE -> "Flux Core"
          RankedGameMode.STASIS_BREACH -> "Stasis Breach"
          RankedGameMode.RIFT_CONTROL -> "Rift Control"
          RankedGameMode.SYNDICATE_WARS -> "Syndicate Wars"
         }
         val newConfig = CustomGameConfig(
          instanceId = "custom_${System.currentTimeMillis()}",
          title = "${callsign.uppercase()}'S $modeShort SANDBOX",
          hostCallsign = callsign, baseMode = customBaseMode, teamSizeBracket = customTeamBracket,
          matchDurationMinutes = customResetMinutes, allowArmadasInFfa = customAllowArmadasInFfa,
          botsEnabled = customBotsEnabled, bulletSizeMult = customBulletSizeMult,
          damageMult = customDamageMult, speedMult = customSpeedMult,
          healthMult = if (customRechargeMult > 1.0f) 1.25f else 1.0f,
          rechargeMult = customRechargeMult, crateSpawnMult = customCrateSpawnMult,
         )
         engine?.createAndJoinCustomGame(newConfig)
         RiftCalibrationState.botsEnabled = customBotsEnabled
         RiftCalibrationState.applyCalibrationChangesLive(engine, context)
         customGamesRefreshTick++
         showCustomGameBuilder = false
         onCloseModal()
        },
       )
      }

      if (layoutMode == NexusHubLayoutMode.DECK_ONLY) {
       Row(modifier = Modifier.weight(1f).fillMaxHeight().testTag("nexus_armada_and_ranked_deck"), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Column(
         modifier = Modifier.weight(0.43f).fillMaxHeight().clip(RoundedCornerShape(12.dp))
          .background(Color(0x99040914)).border(1.dp, Color(0x5538BDF8), RoundedCornerShape(12.dp))
          .padding(10.dp).verticalScroll(rememberScrollState()),
         verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
         renderArmadaDeckShared(true)
         renderShipLockShared()
         renderCustomSandboxShared()
        }

        Column(
         modifier = Modifier.weight(0.57f).fillMaxHeight().clip(RoundedCornerShape(12.dp))
          .background(Color(0x99040914)).border(1.dp, Color(0x6600F5D4), RoundedCornerShape(12.dp))
          .padding(10.dp).verticalScroll(rememberScrollState()),
         verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
         Row(
          modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color(0xCC08182C))
           .border(1.dp, Color(0x8800F5D4), RoundedCornerShape(8.dp)).padding(horizontal = 10.dp, vertical = 6.dp),
          horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically,
         ) {
          Column {
           Text("🎯 SELECT GAME MODE TO DEPLOY // 6 OFFICIAL RANKED QUEUES", color = Color(0xFF00F5D4), fontSize = 10.5.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
           Text("Tap any mode below to launch directly into the sector with your selected hull & Armada.", color = Color(0xFFCBD5E1), fontSize = 8.4.sp)
          }
          Box(
           modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(Color(0xFF142842))
            .border(1.dp, Color(0xFF38BDF8), RoundedCornerShape(6.dp))
            .clickable { layoutMode = NexusHubLayoutMode.SPLIT_COMMAND }.padding(horizontal = 8.dp, vertical = 4.dp),
          ) { Text("💬 SHOW COMMS", color = Color(0xFFE0F2FE), fontSize = 8.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace) }
         }

         val modeChunks = if (isCompactPhone) RankedGameMode.entries.map { listOf(it) } else RankedGameMode.entries.chunked(2)
         modeChunks.forEach { rowModes ->
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
           rowModes.forEach { mode -> Box(modifier = Modifier.weight(1f)) { renderRankedModeCardShared(mode) } }
          }
         }
        }
       }
      } else if (layoutMode != NexusHubLayoutMode.CHANNELS_ONLY) {
       Column(
        modifier = Modifier.weight(1.1f).fillMaxHeight().clip(RoundedCornerShape(12.dp))
         .background(Color(0x99040914)).border(1.dp, Color(0x5500F5D4), RoundedCornerShape(12.dp))
         .padding(10.dp).verticalScroll(rememberScrollState()).testTag("nexus_armada_and_ranked_deck"),
        verticalArrangement = Arrangement.spacedBy(8.dp),
       ) {
        renderArmadaDeckShared(false)
        renderShipLockShared()
        Text("🎯 6 OFFICIAL RANKED ARENA QUEUES (100% FACTORY DEFAULTS)", color = Color(0xFF00F5D4), fontSize = 9.5.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
        RankedGameMode.entries.forEach { mode -> renderRankedModeCardShared(mode) }
        renderCustomSandboxShared()
       }
      }
     }
    }
   }
  }
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
 val isCompactPhone = LocalConfiguration.current.screenHeightDp < 480
 Row(
  modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color(0xF2060D1A))
   .border(1.2.dp, Color(0x9900F5D4), RoundedCornerShape(10.dp))
   .padding(horizontal = 8.dp, vertical = if (isCompactPhone) 3.dp else 5.dp)
   .horizontalScroll(rememberScrollState()).testTag("nexus_master_module_bar"),
  horizontalArrangement = Arrangement.spacedBy(6.dp),
  verticalAlignment = Alignment.CenterVertically,
 ) {
  NexusMasterModulePill("🌐 COMMAND & LOBBY", activeModuleKey == "NEXUS", Color(0xFF00F5D4), "nexus_module_pill_lobby", onSelectNexusLobby)
  NexusMasterModulePill("🚀 HANGAR", activeModuleKey == "HANGAR", Color(0xFF38BDF8), "nexus_module_pill_hangar", onOpenHangar)
  NexusMasterModulePill("🛠️ FORGE", activeModuleKey == "FORGE", Color(0xFFC084FC), "nexus_module_pill_forge", onOpenForge)
  NexusMasterModulePill("❖ PROGRESSION", activeModuleKey == "PROGRESSION", Color(0xFFE879F9), "nexus_module_pill_progression", onOpenProgression)
  NexusMasterModulePill("🛡️ ARMADA FLEET", activeModuleKey == "ARMADA", Color(0xFF60A5FA), "nexus_module_pill_armada", onOpenArmadaModal)
  NexusMasterModulePill("🏛️ SYNDICATES", activeModuleKey == "SYNDICATES", Color(0xFFF59E0B), "nexus_module_pill_syndicates", onOpenSyndicateDirectory)
  NexusMasterModulePill("📊 DOSSIER", activeModuleKey == "DOSSIER", Color(0xFFA78BFA), "nexus_module_pill_dossier", onOpenDossier)
  NexusMasterModulePill("📖 CODEX", activeModuleKey == "CODEX", Color(0xFF34D399), "nexus_module_pill_codex", onOpenCodex)
  NexusMasterModulePill("⚙️ SETTINGS", activeModuleKey == "SETTINGS", Color(0xFF94A3B8), "nexus_module_pill_settings", onOpenSettings)
 }
}

@Composable
private fun NexusMasterModulePill(label: String, selected: Boolean, accentColor: Color, testTag: String, onClick: () -> Unit) {
 val isCompactPhone = LocalConfiguration.current.screenHeightDp < 480
 Box(
  modifier = Modifier.shadow(2.dp, RoundedCornerShape(8.dp)).clip(RoundedCornerShape(8.dp))
   .background(if (selected) accentColor.copy(alpha = 0.24f) else Color(0xCC0B1528))
   .border(if (selected) 1.3.dp else 1.dp, if (selected) accentColor else accentColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
   .clickable { onClick() }
   .padding(horizontal = if (isCompactPhone) 8.dp else 10.dp, vertical = if (isCompactPhone) 3.dp else 5.dp)
   .testTag(testTag),
  contentAlignment = Alignment.Center,
 ) {
  Text(label, color = if (selected) Color.White else accentColor, fontSize = if (isCompactPhone) 8.5.sp else 9.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, maxLines = 1)
 }
}

@Composable
private fun NexusViewModeTab(label: String, selected: Boolean, onClick: () -> Unit, tag: String) {
 Box(
  modifier = Modifier.clip(RoundedCornerShape(8.dp))
   .background(if (selected) Color(0xFF0E3A5A) else Color(0x880B1324))
   .border(1.dp, if (selected) Color(0xFF00F5D4) else Color(0x44475569), RoundedCornerShape(8.dp))
   .clickable { onClick() }.padding(horizontal = 10.dp, vertical = 5.dp).testTag(tag),
 ) { Text(label, color = if (selected) Color(0xFF00F5D4) else Color(0xFF94A3B8), fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace) }
}

@Composable
private fun ChannelCategoryHeader(title: String) {
 Text(title, color = Color(0xFF64748B), fontSize = 8.2.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, modifier = Modifier.padding(top = 4.dp, bottom = 2.dp))
}

@Composable
private fun ChannelNavItem(label: String, badge: String, selected: Boolean, accent: Color, onClick: () -> Unit, tag: String) {
 Row(
  modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(7.dp))
   .background(if (selected) accent.copy(alpha = 0.18f) else Color.Transparent)
   .border(1.dp, if (selected) accent else Color.Transparent, RoundedCornerShape(7.dp))
   .clickable { onClick() }.padding(horizontal = 7.dp, vertical = 5.dp).testTag(tag),
  horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically,
 ) {
  Text(label, color = if (selected) Color.White else Color(0xFFCBD5E1), fontSize = 9.5.sp, fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold, fontFamily = FontFamily.Monospace, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
  Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(accent.copy(alpha = 0.22f)).padding(horizontal = 4.dp, vertical = 1.dp)) {
   Text(badge, color = accent, fontSize = 7.5.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
  }
 }
}

@Composable
private fun HolographicArmadaStagingDeck(
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
 val effectiveSlots = remember(armadaSlots, callsign, syndicateTag, bestShipId) {
  val humanOnly = armadaSlots.filter { !it.callsign.contains("[BOT]", ignoreCase = true) && !it.uid.startsWith("bot_", ignoreCase = true) }
  humanOnly.ifEmpty { listOf(ArmadaMemberSlot("local_player", callsign, syndicateTag, bestShipId, isLeader = true)) }
 }

 Column(
  modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
   .background(Brush.verticalGradient(listOf(Color(0xFF0A192F), Color(0xFF060E1E))))
   .border(1.dp, Color(0x8838BDF8), RoundedCornerShape(12.dp)).padding(9.dp)
   .testTag("nexus_holographic_armada_deck"),
  verticalArrangement = Arrangement.spacedBy(7.dp),
 ) {
  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
   Column(modifier = Modifier.weight(1f).padding(end = 6.dp)) {
    Text(
     if (activeArmadaName.isNotEmpty()) "🛡️ ARMADA STAGING // [$activeArmadaName] (${effectiveSlots.size}/5)" else "🛡️ ARMADA STAGING BAY (${effectiveSlots.size}/5)",
     color = Color(0xFF38BDF8), fontSize = 9.5.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, maxLines = 1, overflow = TextOverflow.Ellipsis,
    )
    Text("Real-pilot wingmen deploy together into any Ranked Queue", color = Color(0xFF94A3B8), fontSize = 8.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
   }
   Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
    Box(
     modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(Color(0xFF0E3A5A)).border(1.dp, Color(0xFF38BDF8), RoundedCornerShape(6.dp))
      .clickable { onOpenArmadaModal() }.padding(horizontal = 7.dp, vertical = 5.dp).testTag("nexus_btn_manage_armada"),
    ) { Text("+ ARMADA", color = Color(0xFFE0F2FE), fontSize = 8.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace, maxLines = 1) }
    Box(
     modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(Color(0xFF1E1B4B)).border(1.dp, Color(0xFF818CF8), RoundedCornerShape(6.dp))
      .clickable { onOpenArmadaChat() }.padding(horizontal = 7.dp, vertical = 5.dp).testTag("nexus_btn_open_armada_chat"),
    ) { Text("💬 CHAT", color = Color(0xFFC7D2FE), fontSize = 8.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace, maxLines = 1) }
   }
  }

  Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
   for (slotIdx in 0 until 5) {
    val member = effectiveSlots.getOrNull(slotIdx)
    if (member != null) {
     val resolvedBestHullId = if (member.isLeader) bestShipId.coerceIn(1, 10) else onResolvePilotBestShipId(member.uid, member.shipId).coerceIn(1, 10)
     val bestHullSpec = RiftCodex.getShipById(resolvedBestHullId)
     val tagStr = member.syndicateTag.ifBlank { "RFT" }
     val holoRingColor = when {
      member.isLeader -> Color(0xFFF59E0B)
      resolvedBestHullId == 10 || resolvedBestHullId == 9 -> Color(0xFFA855F7)
      else -> Color(0xFF00F5D4)
     }

     Column(
      modifier = Modifier.width(108.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xD9060E1C))
       .border(1.dp, holoRingColor.copy(alpha = 0.72f), RoundedCornerShape(10.dp)).padding(6.dp),
      horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp),
     ) {
      Text(if (member.isLeader) "★ LEADER • READY" else "● WINGMAN • READY", color = holoRingColor, fontSize = 7.2.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
      Box(modifier = Modifier.size(width = 58.dp, height = 52.dp), contentAlignment = Alignment.Center) {
       Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width; val h = size.height; val baseCx = w * 0.5f; val baseCy = h * 0.80f
        val conePath = Path().apply {
         moveTo(baseCx - w * 0.36f, baseCy); lineTo(baseCx - w * 0.44f, h * 0.16f)
         lineTo(baseCx + w * 0.44f, h * 0.16f); lineTo(baseCx + w * 0.36f, baseCy); close()
        }
        drawPath(conePath, Brush.verticalGradient(listOf(holoRingColor.copy(alpha = 0.03f), holoRingColor.copy(alpha = 0.25f)), startY = h * 0.16f, endY = baseCy))
        drawOval(holoRingColor.copy(alpha = 0.28f), Offset(baseCx - w * 0.42f, baseCy - h * 0.12f), Size(w * 0.84f, h * 0.24f))
        drawOval(holoRingColor, Offset(baseCx - w * 0.42f, baseCy - h * 0.12f), Size(w * 0.84f, h * 0.24f), style = Stroke(width = 1.5.dp.toPx()))
        drawOval(Color.White.copy(alpha = 0.78f), Offset(baseCx - w * 0.24f, baseCy - h * 0.07f), Size(w * 0.48f, h * 0.14f), style = Stroke(width = 1.dp.toPx()))
       }
       Box(modifier = Modifier.offset(y = (-4).dp)) { ShipSpritePreview(shipId = resolvedBestHullId, sizeDp = 38) }
      }
      Column(
       modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(5.dp)).background(Color(0xFF040914))
        .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(5.dp)).padding(horizontal = 4.dp, vertical = 2.dp),
       horizontalAlignment = Alignment.CenterHorizontally,
      ) {
       Text("[$tagStr] ${member.callsign}", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace, maxLines = 1, overflow = TextOverflow.Ellipsis)
       Text("HULL: ${bestHullSpec.name.uppercase()}", color = holoRingColor, fontSize = 7.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, maxLines = 1, overflow = TextOverflow.Ellipsis)
      }
     }
    } else {
     Column(
      modifier = Modifier.width(96.dp).height(110.dp).clip(RoundedCornerShape(10.dp)).background(Color(0x55071122))
       .border(1.dp, Color(0x44475569), RoundedCornerShape(10.dp)).clickable { onQuickFormSyndicateArmada() }.padding(6.dp),
      horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
     ) {
      Text("＋", color = Color(0xFF00F5D4), fontSize = 16.sp, fontWeight = FontWeight.Black)
      Text("ADD WINGMAN", color = Color(0xFF94A3B8), fontSize = 7.5.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
      Text("SLOT ${slotIdx + 1}/5", color = Color(0xFF64748B), fontSize = 7.sp, fontFamily = FontFamily.Monospace)
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
 val modeAccent = when (mode) {
  RankedGameMode.SECTOR_CONVERGENCE, RankedGameMode.RIFT_CONTROL -> Color(0xFF00F5D4)
  RankedGameMode.RIFT_SKIRMISH, RankedGameMode.FLUX_CORE -> Color(0xFFF59E0B)
  RankedGameMode.STASIS_BREACH, RankedGameMode.SYNDICATE_WARS -> Color(0xFFA855F7)
 }
 val displayTitle = if (mode == RankedGameMode.RIFT_CONTROL) "RIFT CONTROL (TUG-OF-WAR)" else mode.title.uppercase()
 val modeDescription = when (mode) {
  RankedGameMode.SECTOR_CONVERGENCE -> "20-Pilot FFA / 1–5 Armada Bounty • Ongoing 60m cycle • +100❖ Rift Shards for #1 Bounty with >50% presence."
  RankedGameMode.RIFT_SKIRMISH -> "Best-of-3 Team Deathmatch (3m Rounds • No Respawn) • Opposite West/East Warp Gates • +75❖ Rift Shards on match win."
  RankedGameMode.FLUX_CORE -> "20-Pilot Singularity Carrier FFA • +1 Overcharge Tier / 5s (up to +50% Dmg/Recharge & +40% Speed) + 15 Bounty/s • 30s Safe-Zone Eject."
  RankedGameMode.STASIS_BREACH -> "10v10 Circular Stasis Jailbreak • Kills send victims to enemy Stasis Jail • Prisoners destroy 8,000 Flux Center Core to break out!"
  RankedGameMode.RIFT_CONTROL -> "No Safe Zone • Hold a Rift uncontested for 30s to lock it • 3m match timer or instant win when both Rifts lock."
  RankedGameMode.SYNDICATE_WARS -> "Ranked [$syndicateTag] Clan-vs-Clan Ladder • Record: 🏆 ${syndicateWarRecord.warTrophies} WAR TROPHIES (${syndicateWarRecord.warWins}W - ${syndicateWarRecord.warLosses}L)."
 }

 val isSyndicateWarsLocked = mode == RankedGameMode.SYNDICATE_WARS && !canQueueSyndicateWars.first
 val locksShipSelection = doesModeLockShipSelection(mode)

 Box(
  modifier = Modifier.fillMaxWidth().height(152.dp).clip(RoundedCornerShape(10.dp))
   .background(if (isCurrentActive) Color(0xE60B1D33) else Color(0xD907101F))
   .border(if (isCurrentActive) 1.6.dp else 1.dp, if (isCurrentActive) modeAccent else modeAccent.copy(alpha = 0.52f), RoundedCornerShape(10.dp))
   .testTag("nexus_mode_card_${mode.id}"),
 ) {
  Canvas(modifier = Modifier.matchParentSize()) {
   val bLen = 10.dp.toPx(); val bStroke = 2.dp.toPx()
   drawLine(modeAccent, Offset.Zero, Offset(bLen, 0f), bStroke)
   drawLine(modeAccent, Offset.Zero, Offset(0f, bLen), bStroke)
   drawLine(modeAccent, Offset(size.width - bLen, 0f), Offset(size.width, 0f), bStroke)
   drawLine(modeAccent, Offset(size.width, 0f), Offset(size.width, bLen), bStroke)
   drawLine(modeAccent, Offset(0f, size.height), Offset(bLen, size.height), bStroke)
   drawLine(modeAccent, Offset(0f, size.height - bLen), Offset(0f, size.height), bStroke)
   drawLine(modeAccent, Offset(size.width - bLen, size.height), Offset(size.width, size.height), bStroke)
   drawLine(modeAccent, Offset(size.width, size.height - bLen), Offset(size.width, size.height), bStroke)
  }

  Column(modifier = Modifier.fillMaxSize().padding(10.dp), verticalArrangement = Arrangement.SpaceBetween) {
   Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.weight(1f)) {
     Box(
      modifier = Modifier.size(24.dp).clip(RoundedCornerShape(6.dp)).background(Color(0xFF040914)).border(1.dp, modeAccent, RoundedCornerShape(6.dp)),
      contentAlignment = Alignment.Center,
     ) {
      Canvas(modifier = Modifier.size(16.dp)) {
       val r = size.minDimension * 0.42f; val c = Offset(size.width * 0.5f, size.height * 0.5f)
       when (mode) {
        RankedGameMode.SECTOR_CONVERGENCE -> { drawCircle(modeAccent, r, c, style = Stroke(1.6f)); drawCircle(modeAccent, r * 0.38f, c) }
        RankedGameMode.RIFT_SKIRMISH -> { drawLine(modeAccent, Offset(c.x - r, c.y - r), Offset(c.x + r, c.y + r), 2f); drawLine(modeAccent, Offset(c.x + r, c.y - r), Offset(c.x - r, c.y + r), 2f) }
        RankedGameMode.FLUX_CORE -> { drawCircle(modeAccent, r * 0.85f, c, style = Stroke(1.8f)); drawCircle(Color.White, r * 0.40f, c) }
        RankedGameMode.STASIS_BREACH -> {
         drawCircle(modeAccent, r, c, style = Stroke(1.8f))
         drawLine(modeAccent, Offset(c.x - r * 0.5f, c.y), Offset(c.x + r * 0.5f, c.y), 1.6f)
         drawLine(modeAccent, Offset(c.x, c.y - r * 0.5f), Offset(c.x, c.y + r * 0.5f), 1.6f)
        }
        RankedGameMode.RIFT_CONTROL -> {
         drawCircle(modeAccent, r * 0.52f, Offset(c.x - r * 0.42f, c.y), style = Stroke(1.6f))
         drawCircle(modeAccent, r * 0.52f, Offset(c.x + r * 0.42f, c.y), style = Stroke(1.6f))
        }
        RankedGameMode.SYNDICATE_WARS -> {
         val shield = Path().apply {
          moveTo(c.x, c.y - r); lineTo(c.x + r, c.y - r * 0.35f); lineTo(c.x + r * 0.65f, c.y + r * 0.55f)
          lineTo(c.x, c.y + r); lineTo(c.x - r * 0.65f, c.y + r * 0.55f); lineTo(c.x - r, c.y - r * 0.35f); close()
         }
         drawPath(shield, modeAccent, style = Stroke(1.8f))
        }
       }
      }
     }
     Text(displayTitle, color = modeAccent, fontSize = 10.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, maxLines = 1, overflow = TextOverflow.Ellipsis)
     Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(modeAccent.copy(alpha = 0.18f)).padding(horizontal = 5.dp, vertical = 2.dp)) {
      Text(mode.teamFormatBadge, color = modeAccent, fontSize = 7.5.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, maxLines = 1)
     }
    }

    val hasActiveHumans = humanPilotsActive > 0
    Box(
     modifier = Modifier.clip(RoundedCornerShape(6.dp))
      .background(if (hasActiveHumans) Color(0xFF06281E) else Color(0xFF0F172A))
      .border(1.dp, if (hasActiveHumans) Color(0xFF10B981) else Color(0xFF334155), RoundedCornerShape(6.dp))
      .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
     Text(
      if (hasActiveHumans) "🔥 $humanPilotsActive PILOTS IN MATCH" else "0 PILOTS IN MATCH",
      color = if (hasActiveHumans) Color(0xFF34D399) else Color(0xFF94A3B8),
      fontSize = 7.8.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace, maxLines = 1,
     )
    }
   }

   Text(modeDescription, color = Color(0xFFCBD5E1), fontSize = 8.8.sp, lineHeight = 12.sp, minLines = 2, maxLines = 2, overflow = TextOverflow.Ellipsis)

   Row(modifier = Modifier.fillMaxWidth().height(24.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
    when (mode) {
     RankedGameMode.RIFT_SKIRMISH -> {
      Text("BRACKET:", color = Color(0xFF94A3B8), fontSize = 7.8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
      listOf(2, 3, 4, 5).forEach { sz -> ModeBracketChip("${sz}v${sz}", selectedBracket == sz, modeAccent) { onSelectBracket(sz) } }
     }
     RankedGameMode.RIFT_CONTROL -> {
      Text("BRACKET:", color = Color(0xFF94A3B8), fontSize = 7.8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
      listOf(3 to "3v3", 5 to "5v5", 10 to "10v10").forEach { (sz, lbl) -> ModeBracketChip(lbl, selectedBracket == sz, modeAccent) { onSelectBracket(sz) } }
     }
     RankedGameMode.SYNDICATE_WARS -> {
      Text("WAR FORMAT:", color = Color(0xFF94A3B8), fontSize = 7.8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
      listOf(3 to "3v3 STRIKE", 5 to "5v5 STRIKE", 10 to "10v10 BREACH").forEach { (sz, lbl) -> ModeBracketChip(lbl, selectedBracket == sz, modeAccent) { onSelectBracket(sz) } }
     }
     else -> {
      val formatPills = when (mode) {
       RankedGameMode.SECTOR_CONVERGENCE -> listOf("20-PILOT FFA", "1–5 ARMADA", "FREE HULL SWAP")
       RankedGameMode.FLUX_CORE -> listOf("20-PILOT CARRIER", "OVERCHARGE TIERS", "FREE HULL SWAP")
       else -> listOf("10v10 JAILBREAK", "8,000 FLUX CORE", "FREE HULL SWAP")
      }
      Text("FORMAT:", color = Color(0xFF94A3B8), fontSize = 7.8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
      formatPills.forEach { pillText ->
       Box(modifier = Modifier.clip(RoundedCornerShape(5.dp)).background(Color(0xFF0F172A)).border(1.dp, modeAccent.copy(alpha = 0.45f), RoundedCornerShape(5.dp)).padding(horizontal = 6.dp, vertical = 2.5.dp)) {
        Text(pillText, color = Color(0xFFE2E8F0), fontSize = 7.5.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
       }
      }
     }
    }
   }

   Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
    if (isSyndicateWarsLocked) {
     Text(
      "🔒 REQUIRES ENLISTED SYNDICATE & 100% [$syndicateTag] ARMADA",
      color = Color(0xFFF87171), fontSize = 7.8.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace,
      maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f).padding(end = 6.dp),
     )
    } else {
     Text(
      when {
       isCurrentActive -> "● CURRENTLY DEPLOYED IN THIS SECTOR"
       locksShipSelection -> "🔒 LOCKS SELECTED HULL FOR MATCH"
       else -> "✓ FREE HULL SWAP • DROP-IN QUEUE"
      },
      color = if (isCurrentActive) modeAccent else Color(0xFF64748B),
      fontSize = 7.8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace,
      maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f).padding(end = 6.dp),
     )
    }

    Button(
     onClick = onDeploy, enabled = !isSyndicateWarsLocked,
     colors = ButtonDefaults.buttonColors(
      containerColor = modeAccent.copy(alpha = 0.24f), contentColor = Color.White,
      disabledContainerColor = Color(0x551E293B), disabledContentColor = Color(0xFF64748B),
     ),
     border = BorderStroke(1.2.dp, if (isSyndicateWarsLocked) Color(0xFF475569) else modeAccent),
     shape = RoundedCornerShape(8.dp), contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
     modifier = Modifier.height(30.dp).shadow(2.dp, RoundedCornerShape(8.dp)).testTag("nexus_deploy_${mode.id}"),
    ) {
     Text(
      when {
       locksShipSelection -> "✅ LOCK SHIP & DEPLOY"
       armadaSize >= 2 -> "🚀 DEPLOY ARMADA ($armadaSize)"
       else -> "🚀 DEPLOY"
      },
      fontSize = 8.4.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, maxLines = 1,
     )
    }
   }
  }
 }
}

@Composable
private fun ModeBracketChip(label: String, selected: Boolean, accent: Color, onClick: () -> Unit) {
 Box(
  modifier = Modifier.clip(RoundedCornerShape(5.dp))
   .background(if (selected) accent.copy(alpha = 0.25f) else Color(0xFF0F172A))
   .border(1.dp, if (selected) accent else Color(0xFF334155), RoundedCornerShape(5.dp))
   .clickable { onClick() }.padding(horizontal = 7.dp, vertical = 2.5.dp),
 ) { Text(label, color = if (selected) Color.White else Color(0xFF94A3B8), fontSize = 7.8.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace) }
}

@Composable
private fun PreMatchTeamShipLockCard(
 selectedShipId: Int,
 isCurrentlyLockedInMatch: Boolean,
 pendingTeamMode: Pair<RankedGameMode, Int>?,
 onSelectShip: (Int) -> Unit,
 onConfirmLockAndDeploy: (RankedGameMode, Int) -> Unit,
 onCancelPending: () -> Unit,
) {
 val selectedSpec = RiftCodex.getShipById(selectedShipId.coerceIn(1, 10))
 Column(
  modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color(0xCC091526))
   .border(1.dp, Color(0xFFA855F7), RoundedCornerShape(10.dp)).padding(9.dp)
   .testTag("nexus_pre_match_ship_lock_picker"),
  verticalArrangement = Arrangement.spacedBy(6.dp),
 ) {
  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
   Text("🔒 SELECT HULL FOR SKIRMISH & SYNDICATE WARS", color = Color(0xFFE9D5FF), fontSize = 9.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
   Text(
    if (isCurrentlyLockedInMatch) "LOCKED: ${selectedSpec.name.uppercase()}" else "READY: ${selectedSpec.name.uppercase()}",
    color = Color(0xFF00F5D4), fontSize = 8.2.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace,
   )
  }

  Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
   for (shipId in 1..10) {
    val spec = RiftCodex.getShipById(shipId)
    val isSelected = selectedShipId == shipId
    Column(
     modifier = Modifier.width(68.dp).clip(RoundedCornerShape(8.dp))
      .background(if (isSelected) Color(0x44A855F7) else Color(0x9907101E))
      .border(if (isSelected) 1.5.dp else 1.dp, if (isSelected) Color(0xFF00F5D4) else Color(0xFF334155), RoundedCornerShape(8.dp))
      .clickable { onSelectShip(shipId) }.padding(vertical = 5.dp, horizontal = 4.dp)
      .testTag("nexus_team_lock_ship_$shipId"),
     horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
     ShipSpritePreview(shipId = shipId, sizeDp = 28)
     Text(spec.name, color = if (isSelected) Color.White else Color(0xFF94A3B8), fontSize = 7.5.sp, fontWeight = if (isSelected) FontWeight.Black else FontWeight.SemiBold, fontFamily = FontFamily.Monospace, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
   }
  }

  if (pendingTeamMode != null) {
   Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
    Text("Queue: ${pendingTeamMode.first.title} (${pendingTeamMode.second}v${pendingTeamMode.second})", color = Color(0xFFFACC15), fontSize = 8.2.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
     Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(Color(0xFF1E293B)).clickable { onCancelPending() }.padding(horizontal = 8.dp, vertical = 4.dp)) {
      Text("CANCEL", color = Color(0xFF94A3B8), fontSize = 8.sp, fontFamily = FontFamily.Monospace)
     }
     Box(
      modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(Color(0xFF0E7490))
       .border(1.dp, Color(0xFF00F5D4), RoundedCornerShape(6.dp))
       .clickable { onConfirmLockAndDeploy(pendingTeamMode.first, pendingTeamMode.second) }
       .padding(horizontal = 8.dp, vertical = 4.dp),
     ) { Text("✅ LOCK SHIP & DEPLOY", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace) }
    }
   }
  }
 }
}

@Composable
private fun CustomGamesSectionCard(
 engine: RiftGameEngine?,
 callsign: String,
 lockedTeamShipId: Int,
 refreshTick: Int,
 showBuilder: Boolean,
 onToggleBuilder: () -> Unit,
 baseMode: RankedGameMode,
 onSelectBaseMode: (RankedGameMode) -> Unit,
 resetMinutes: Int,
 onSelectResetMinutes: (Int) -> Unit,
 teamBracket: Int,
 onSelectTeamBracket: (Int) -> Unit,
 allowArmadasInFfa: Boolean,
 onToggleAllowArmadasInFfa: (Boolean) -> Unit,
 botsEnabled: Boolean,
 onToggleBotsEnabled: (Boolean) -> Unit,
 bulletSizeMult: Float,
 onSelectBulletSizeMult: (Float) -> Unit,
 speedMult: Float,
 onSelectSpeedMult: (Float) -> Unit,
 rechargeMult: Float,
 onSelectRechargeMult: (Float) -> Unit,
 damageMult: Float,
 onSelectDamageMult: (Float) -> Unit,
 crateSpawnMult: Float,
 onSelectCrateSpawnMult: (Float) -> Unit,
 onReturnToOfficialRankedFfa: () -> Unit,
 onJoinCustomGame: (String) -> Unit,
 onLaunchCustomGame: () -> Unit,
) {
 val activeCustomGame = remember(engine?.currentCustomGame, refreshTick) { engine?.currentCustomGame }
 val customGamesList = remember(engine?.activeCustomGames?.size, refreshTick) { engine?.activeCustomGames?.toList() ?: emptyList() }

 Column(
  modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0xCC071224))
   .border(1.dp, Color(0xFF00F5D4), RoundedCornerShape(12.dp)).padding(10.dp)
   .testTag("nexus_custom_games_section"),
  verticalArrangement = Arrangement.spacedBy(8.dp),
 ) {
  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
   Column(modifier = Modifier.weight(1f)) {
    Text("🧪 CUSTOM GAMES — LIVE CALIBRATION & [📡 SYNC TO ALL] ENABLED", color = Color(0xFF00F5D4), fontSize = 9.2.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
    Text("Clone any mode with custom rules • Live Physics Calibration & Multi-Tester Sync unlocked", color = Color(0xFF94A3B8), fontSize = 8.sp)
   }
   Box(
    modifier = Modifier.clip(RoundedCornerShape(7.dp))
     .background(if (showBuilder) Color(0xFF311042) else Color(0xFF0E7490))
     .border(1.dp, Color(0xFF00F5D4), RoundedCornerShape(7.dp))
     .clickable { onToggleBuilder() }.padding(horizontal = 8.dp, vertical = 5.dp)
     .testTag("nexus_btn_toggle_custom_game_builder"),
   ) {
    Text(if (showBuilder) "✕ CLOSE CLONER" else "+ CREATE / CLONE CUSTOM GAME", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
   }
  }

  if (activeCustomGame != null && engine?.hasJoinedMatch == true) {
   Row(
    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color(0xFF06281E))
     .border(1.dp, Color(0xFF10B981), RoundedCornerShape(8.dp)).padding(8.dp),
    horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically,
   ) {
    Column(modifier = Modifier.weight(1f)) {
     Text("🧪 PLAYING CUSTOM GAME: ${activeCustomGame.title}", color = Color(0xFF34D399), fontSize = 8.8.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
     Text("Base: ${activeCustomGame.baseMode.title} • Bots: ${if (activeCustomGame.botsEnabled) "ON" else "OFF"} • Live Calibration Active", color = Color(0xFFA7F3D0), fontSize = 7.8.sp, fontFamily = FontFamily.Monospace)
    }
    Box(
     modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(Color(0xFF0F172A)).border(1.dp, Color(0xFF34D399), RoundedCornerShape(6.dp))
      .clickable { onReturnToOfficialRankedFfa() }.padding(horizontal = 8.dp, vertical = 4.dp)
      .testTag("nexus_btn_return_official_ranked"),
    ) { Text("↩ RETURN TO OFFICIAL RANKED FFA", color = Color(0xFF34D399), fontSize = 7.8.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace) }
   }
  }

  if (showBuilder) {
   Column(
    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(9.dp)).background(Color(0xEE040B16))
     .border(1.dp, Color(0x8838BDF8), RoundedCornerShape(9.dp)).padding(9.dp),
    verticalArrangement = Arrangement.spacedBy(6.dp),
   ) {
    Text("⚙️ CONFIGURE 10 CUSTOM GAME RULES (HOST: ${callsign.uppercase()})", color = Color(0xFFFACC15), fontSize = 8.8.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
    CustomRuleChipRow("1. BASE MODE:", listOf(RankedGameMode.SECTOR_CONVERGENCE to "FFA", RankedGameMode.RIFT_SKIRMISH to "Skirmish", RankedGameMode.FLUX_CORE to "Flux Core", RankedGameMode.STASIS_BREACH to "Stasis Breach", RankedGameMode.RIFT_CONTROL to "Rift Control"), baseMode, onSelectBaseMode)
    CustomRuleChipRow("2. RESET TIMER:", listOf(5 to "5m", 10 to "10m", 15 to "15m", 30 to "30m", 60 to "60m", 0 to "∞"), resetMinutes, onSelectResetMinutes)
    CustomRuleChipRow("3. TEAM SIZE:", listOf(2 to "2v2", 3 to "3v3", 5 to "5v5", 10 to "10v10"), teamBracket, onSelectTeamBracket)
    CustomRuleChipRow("4. ARMADAS IN FFA:", listOf(true to "Allowed", false to "Solo Only"), allowArmadasInFfa, onToggleAllowArmadasInFfa)
    CustomRuleChipRow("5. AI BOTS:", listOf(true to "ON", false to "OFF"), botsEnabled, onToggleBotsEnabled)
    CustomRuleChipRow("6. BULLET SIZE:", listOf(1.0f to "1.0x Normal", 1.5f to "1.5x Fat", 2.2f to "2.2x Giant"), bulletSizeMult, onSelectBulletSizeMult)
    CustomRuleChipRow("7. SHIP SPEED:", listOf(0.8f to "0.8x Tactical", 1.0f to "1.0x Standard", 1.4f to "1.4x Turbo"), speedMult, onSelectSpeedMult)
    CustomRuleChipRow("8. FLUX RECHARGE:", listOf(1.0f to "1.0x Standard", 1.5f to "1.5x Overcharged", 2.5f to "2.5x Rapid"), rechargeMult, onSelectRechargeMult)
    CustomRuleChipRow("9. DAMAGE / BOUNCE:", listOf(0.75f to "0.75x Tanky", 1.0f to "1.0x Standard", 1.5f to "1.5x Lethal"), damageMult, onSelectDamageMult)
    CustomRuleChipRow("10. RIFTS & CRATES:", listOf(0.5f to "0.5x Sparse", 1.0f to "1.0x Standard", 2.0f to "2.0x Dense"), crateSpawnMult, onSelectCrateSpawnMult)

    Button(
     onClick = onLaunchCustomGame,
     colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0E7490), contentColor = Color.White),
     border = BorderStroke(1.dp, Color(0xFF00F5D4)), shape = RoundedCornerShape(8.dp),
     modifier = Modifier.fillMaxWidth().height(34.dp).testTag("nexus_btn_launch_custom_game"),
    ) {
     val readyHullName = RiftCodex.getShipById(lockedTeamShipId.coerceIn(1, 10)).name.uppercase()
     Text("🚀 LAUNCH CUSTOM GAME ($readyHullName READY)", fontSize = 9.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
    }
   }
  }

  customGamesList.forEach { game ->
   val isJoined = engine?.hasJoinedMatch == true && activeCustomGame?.instanceId == game.instanceId
   val customHumanCount = engine?.getHumanPilotCountForCustomGame(game.instanceId) ?: 0
   val timerLabel = if (game.matchDurationMinutes == 0) "∞" else "${game.matchDurationMinutes}m"
   Row(
    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
     .background(if (isJoined) Color(0xCC0D2638) else Color(0x99060E1C))
     .border(1.dp, if (isJoined) Color(0xFF00F5D4) else Color(0xFF334155), RoundedCornerShape(8.dp)).padding(8.dp),
    horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically,
   ) {
    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
     Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
      Text("🧪 ${game.title.uppercase()}", color = if (isJoined) Color(0xFF00F5D4) else Color.White, fontSize = 8.8.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
      Text("• $customHumanCount PILOTS IN MATCH", color = if (customHumanCount > 0) Color(0xFF34D399) else Color(0xFF94A3B8), fontSize = 7.8.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace)
     }
     Text("${game.baseMode.title} • Host: ${game.hostCallsign} • Timer: $timerLabel • Bots: ${if (game.botsEnabled) "ON" else "OFF"} • Bullets: ${game.bulletSizeMult}x • Speed: ${game.speedMult}x", color = Color(0xFF94A3B8), fontSize = 7.6.sp, fontFamily = FontFamily.Monospace)
    }
    Button(
     onClick = { onJoinCustomGame(game.instanceId) },
     colors = ButtonDefaults.buttonColors(containerColor = if (isJoined) Color(0xFF065F46) else Color(0xFF1E293B), contentColor = Color.White),
     border = BorderStroke(1.dp, Color(0xFF00F5D4)), shape = RoundedCornerShape(7.dp),
     contentPadding = PaddingValues(horizontal = 9.dp, vertical = 3.dp),
     modifier = Modifier.height(28.dp).testTag("nexus_join_custom_${game.instanceId}"),
    ) { Text(if (isJoined) "● ACTIVE" else "🚀 JOIN CUSTOM GAME", fontSize = 7.8.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace) }
   }
  }
 }
}

@Composable
private fun <T> CustomRuleChipRow(label: String, options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit) {
 Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
  Text(label, color = Color(0xFF94A3B8), fontSize = 7.8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, modifier = Modifier.width(105.dp))
  options.forEach { (value, text) ->
   val isSel = selected == value
   Box(
    modifier = Modifier.clip(RoundedCornerShape(5.dp))
     .background(if (isSel) Color(0x3300F5D4) else Color(0xFF0F172A))
     .border(1.dp, if (isSel) Color(0xFF00F5D4) else Color(0xFF334155), RoundedCornerShape(5.dp))
     .clickable { onSelect(value) }.padding(horizontal = 7.dp, vertical = 3.dp),
   ) { Text(text, color = if (isSel) Color.White else Color(0xFF94A3B8), fontSize = 7.6.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace) }
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
  modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color(0xEE0B1528))
   .border(1.dp, Color(0xFFF59E0B), RoundedCornerShape(8.dp)).padding(8.dp),
  verticalArrangement = Arrangement.spacedBy(5.dp),
 ) {
  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
   Text("🏛️ [$syndicateTag] CUSTOM CHANNELS (${channels.size}/5 MAX)", color = Color(0xFFFACC15), fontSize = 9.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
   Text("✕", color = Color(0xFF94A3B8), fontSize = 10.sp, modifier = Modifier.clickable { onClose() }.padding(horizontal = 4.dp))
  }
  Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
   channels.forEachIndexed { idx, ch ->
    Row(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(Color(0xFF1E293B)).padding(horizontal = 6.dp, vertical = 3.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
     Text(ch, color = Color.White, fontSize = 8.2.sp, fontFamily = FontFamily.Monospace)
     if (idx > 0) Text("✕", color = Color(0xFFEF4444), fontSize = 8.5.sp, fontWeight = FontWeight.Black, modifier = Modifier.clickable { onDeleteChannel(ch) })
    }
   }
  }
  if (channels.size < 5) {
   Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
    Box(
     modifier = Modifier.weight(1f).height(34.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFF050B14))
      .border(1.dp, Color(0x66F59E0B), RoundedCornerShape(8.dp)).padding(horizontal = 10.dp),
     contentAlignment = Alignment.CenterStart,
    ) {
     if (newChannelDraft.isEmpty()) Text("New channel (e.g. #war-room)", color = Color(0xFF64748B), fontSize = 9.sp)
     BasicTextField(
      value = newChannelDraft, onValueChange = onDraftChange, singleLine = true,
      cursorBrush = SolidColor(Color(0xFFFACC15)),
      textStyle = TextStyle(color = Color.White, fontSize = 9.5.sp, fontFamily = FontFamily.Monospace),
      modifier = Modifier.fillMaxWidth(),
     )
    }
    Button(
     onClick = onAddChannel, border = BorderStroke(1.2.dp, Color(0xFFF59E0B)), shape = RoundedCornerShape(8.dp),
     contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
     modifier = Modifier.height(34.dp).shadow(2.dp, RoundedCornerShape(8.dp)),
    ) { Text("ADD", fontSize = 8.5.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace) }
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
  modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color(0xEE0B1528))
   .border(1.dp, Color(0xFF38BDF8), RoundedCornerShape(8.dp)).padding(8.dp),
  verticalArrangement = Arrangement.spacedBy(5.dp),
 ) {
  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
   Text("🎟️ [$syndicateTag] INVITED SYNDICATE GUEST PASSES (${invitedGuests.size})", color = Color(0xFF38BDF8), fontSize = 9.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
   Text("✕", color = Color(0xFF94A3B8), fontSize = 10.sp, modifier = Modifier.clickable { onClose() }.padding(horizontal = 4.dp))
  }
  if (invitedGuests.isNotEmpty()) {
   Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
    invitedGuests.forEach { guest ->
     Row(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(Color(0xFF1E293B)).padding(horizontal = 6.dp, vertical = 3.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
      Text(guest, color = Color.White, fontSize = 8.2.sp, fontFamily = FontFamily.Monospace)
      Text("✕", color = Color(0xFFEF4444), fontSize = 8.5.sp, fontWeight = FontWeight.Black, modifier = Modifier.clickable { onRevoke(guest) })
     }
    }
   }
  }
  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
   Box(
    modifier = Modifier.weight(1f).height(34.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFF050B14))
     .border(1.dp, Color(0x6638BDF8), RoundedCornerShape(8.dp)).padding(horizontal = 10.dp),
    contentAlignment = Alignment.CenterStart,
   ) {
    if (guestDraft.isEmpty()) Text("Invite pilot callsign to [$syndicateTag] channels…", color = Color(0xFF64748B), fontSize = 9.sp)
    BasicTextField(
     value = guestDraft, onValueChange = onDraftChange, singleLine = true,
     cursorBrush = SolidColor(Color(0xFF38BDF8)),
     textStyle = TextStyle(color = Color.White, fontSize = 9.5.sp, fontFamily = FontFamily.Monospace),
     modifier = Modifier.fillMaxWidth(),
    )
   }
   Button(
    onClick = onInvite, border = BorderStroke(1.2.dp, Color(0xFF38BDF8)), shape = RoundedCornerShape(8.dp),
    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
    modifier = Modifier.height(34.dp).shadow(2.dp, RoundedCornerShape(8.dp)),
   ) { Text("INVITE", fontSize = 8.5.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace) }
  }
 }
}

@Composable
private fun NexusShipChannelView(
 shipId: Int,
 channelHandle: String,
 spec: ShipSpec,
 posts: List<LocalChannelPost>,
 chatInput: String,
 onChatInputChange: (String) -> Unit,
 onSendPost: () -> Unit,
 onEquipInHangar: () -> Unit,
 onAskNexusAiAboutHull: () -> Unit,
) {
 Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
  Row(
   modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color(0xCC0A1628))
    .border(1.dp, Color(0x8838BDF8), RoundedCornerShape(10.dp)).padding(8.dp),
   verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
  ) {
   ShipSpritePreview(shipId = shipId, sizeDp = 42)
   Column(modifier = Modifier.weight(1f)) {
    Text("$channelHandle // ${spec.name.uppercase()} (${spec.role.uppercase()})", color = Color(0xFF00F5D4), fontSize = 10.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
    Text("MAX FLUX: ${spec.maxFlux.toInt()} FLUX • RECHARGE: ${spec.rechargeRate.toInt()} FLUX/s • SPEED: ${spec.topSpeed.toInt()} px/s • THRUST: ${spec.thrust.toInt()}", color = Color(0xFFFACC15), fontSize = 8.2.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
    Text("PRIMARY: ${spec.primaryTitle} (${spec.primaryCost.toInt()} FLUX) • SECONDARY: ${spec.secondaryTitle} (${spec.bombCost.toInt()} FLUX)", color = Color(0xFFCBD5E1), fontSize = 8.sp, fontFamily = FontFamily.Monospace)
   }
   Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
    Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(Color(0xFF0E7490)).clickable { onEquipInHangar() }.padding(horizontal = 7.dp, vertical = 4.dp)) {
     Text("🚀 EQUIP HULL", color = Color.White, fontSize = 7.8.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
    }
    Box(
     modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(Color(0xFF311042))
      .border(1.dp, Color(0xFFB388FF), RoundedCornerShape(6.dp)).clickable { onAskNexusAiAboutHull() }.padding(horizontal = 7.dp, vertical = 4.dp),
    ) { Text("🤖 ASK #NEXUS-AI", color = Color(0xFFE9D5FF), fontSize = 7.8.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace) }
   }
  }

  LazyColumn(
   modifier = Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color(0x55030712)).padding(8.dp),
   verticalArrangement = Arrangement.spacedBy(6.dp),
  ) {
   item { Text("📌 PINNED // ${spec.name} Tactical Notes: ${spec.primaryTitle} + ${spec.secondaryTitle}. Share loadouts, bank shots, and matchup tips below.", color = Color(0xFF94A3B8), fontSize = 9.sp) }
   items(posts, key = { it.id }) { post -> ChannelPostRow(post) }
  }

  ChannelInputComposer("Post to $channelHandle pilot channel…", chatInput, onChatInputChange, onSendPost)
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
 onQuickPing: (String) -> Unit = {},
) {
 Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
  Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color(0xAA091426)).border(1.dp, Color(0x4438BDF8), RoundedCornerShape(8.dp)).padding(8.dp)) {
   Text(channelTitle, color = Color(0xFF00F5D4), fontSize = 10.5.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
   Text(channelSubtitle, color = Color(0xFF94A3B8), fontSize = 8.5.sp)
  }

  LazyColumn(
   modifier = Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color(0x55030712)).padding(8.dp),
   verticalArrangement = Arrangement.spacedBy(6.dp),
  ) {
   if (globalComms.isEmpty() && localPosts.isEmpty()) {
    item { Text("Channel frequency online. Transmit a message to all connected pilots in this channel.", color = Color(0xFF64748B), fontSize = 9.5.sp, fontFamily = FontFamily.Monospace) }
   }
   items(globalComms, key = { "g_${it.id}" }) { msg ->
    val text = if (msg.isArmadaRecruitBanner) "🛡️ ARMADA RECRUIT [${msg.armadaName}] (${msg.armadaCurrentSize}/${msg.armadaMaxSize}) — ${msg.armadaObjective}" else msg.messageText
    ChannelPostRow(LocalChannelPost(msg.id, msg.senderCallsign, msg.senderSyndicateTag.ifBlank { "GUEST" }, msg.timestampLabel, text, Color(0xFF38BDF8)))
   }
   items(localPosts, key = { it.id }) { post -> ChannelPostRow(post) }
  }

  Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
   TacticalQuickPingChip("🔴 ATTACK HVT", Color(0xFFFB7185), "nexus_quick_ping_attack_hvt") { onQuickPing("🔴 [TACTICAL PING] CONVERGE ON SECTOR HVT NOW!") }
   TacticalQuickPingChip("🛡️ FORM ON ME", Color(0xFF38BDF8), "nexus_quick_ping_form_on_me") { onQuickPing("🛡️ [TACTICAL PING] FORM UP ON MY WING!") }
   TacticalQuickPingChip("🔋 LOW FLUX", Color(0xFFFACC15), "nexus_quick_ping_low_flux") { onQuickPing("🔋 [TACTICAL PING] LOW FLUX — NEED COVER!") }
  }

  ChannelInputComposer("Transmit message to $channelTitle…", chatInput, onChatInputChange, onSend)
 }
}

@Composable
private fun TacticalQuickPingChip(label: String, accent: Color, tag: String, onClick: () -> Unit) {
 Box(
  modifier = Modifier.clip(RoundedCornerShape(7.dp)).background(accent.copy(alpha = 0.16f))
   .border(1.dp, accent.copy(alpha = 0.7f), RoundedCornerShape(7.dp)).clickable { onClick() }
   .padding(horizontal = 8.dp, vertical = 4.dp).testTag(tag),
 ) { Text(label, color = accent, fontSize = 8.5.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace) }
}

@Composable
private fun ChannelPostRow(post: LocalChannelPost) {
 Row(
  modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).background(Color(0x880B1528)).padding(horizontal = 8.dp, vertical = 5.dp),
  horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.Top,
 ) {
  Text("[${post.senderTag}] ${post.senderCallsign}:", color = post.accentColor, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace)
  Text(post.text, color = Color(0xFFF1F5F9), fontSize = 9.5.sp, modifier = Modifier.weight(1f))
 }
}

@Composable
private fun ChannelInputComposer(placeholder: String, value: String, onValueChange: (String) -> Unit, onSend: () -> Unit) {
 val isCompactPhone = LocalConfiguration.current.screenHeightDp < 480
 Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
  Box(
   modifier = Modifier.weight(1f).height(if (isCompactPhone) 34.dp else 38.dp).clip(RoundedCornerShape(8.dp))
    .background(Color(0xFF050B14)).border(1.dp, Color(0xFF38BDF8), RoundedCornerShape(8.dp)).padding(horizontal = 10.dp),
   contentAlignment = Alignment.CenterStart,
  ) {
   if (value.isEmpty()) Text(placeholder, color = Color(0xFF64748B), fontSize = 9.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
   BasicTextField(
    value = value, onValueChange = onValueChange, singleLine = true,
    cursorBrush = SolidColor(Color(0xFF00F5D4)),
    textStyle = TextStyle(color = Color.White, fontSize = 10.5.sp),
    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
    keyboardActions = KeyboardActions(onSend = { onSend() }),
    modifier = Modifier.fillMaxWidth(),
   )
  }
  Button(
   onClick = onSend, enabled = value.isNotBlank(), border = BorderStroke(1.2.dp, Color(0xFF00F5D4)),
   shape = RoundedCornerShape(8.dp), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
   modifier = Modifier.height(if (isCompactPhone) 34.dp else 38.dp).shadow(2.dp, RoundedCornerShape(8.dp)),
  ) { Text("SEND ➢", fontSize = 9.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace) }
 }
}

@Composable
private fun NexusChangelogChannelView() {
 Column(
  modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)).background(Color(0x66030712)).padding(10.dp).verticalScroll(rememberScrollState()),
  verticalArrangement = Arrangement.spacedBy(8.dp),
 ) {
  Text("📋 #change-log // RIFT BUILD #54 — NEXUS FORGE STUDIO, UNIFIED SHELL & 3D RIFTS", color = Color(0xFFFACC15), fontSize = 11.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
  listOf(
   "🛠️ NEXUS FORGE STUDIO: Ship & Cosmetic 360° Turntable, Track A–C Soundtrack & SFX Lab (60–180 BPM), Custom Buff Forge, Syndicate Crest Forge, and Interactive Playfield & HUD Studio.",
   "🌐 UNIFIED 9-TAB NEXUS MASTER SHELL: Command & Lobby, Hangar, Forge, Progression, Armada Fleet, Syndicates, Dossier, Codex, and Settings swap instantaneously inside one persistent glass window.",
   "❖ 6-PILLAR PROGRESSION & RIFT FRAME AT LV 20: Ship 10 (Rift Frame) unlocks its full 100-Point Parity Budget at Pilot Level 20, alongside Frame A–E bays and cosmetics.",
   "🌀 3D PLAYFIELD RIFTS & TUG-OF-WAR BAR: Codex Rifts render live 60fps Playfield Rifts, Rift Control uses open combat + Graphical Dual-Rift Tug-of-War HUD, and Stasis Breach Jail Cores default to 8,000 Flux.",
   "👁️ INTERACTIVE TEAMMATE SPECTATOR & REAL-PLAYER SYNDICATES: Cycle living teammates while awaiting respawn in team modes, and compete on 100% real-player-founded Syndicate War ladders.",
  ).forEach { line -> Text("• $line", color = Color(0xFFE2E8F0), fontSize = 9.5.sp, lineHeight = 14.sp) }
 }
}

@Composable
private fun NexusRulesChannelView() {
 Column(
  modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)).background(Color(0x66030712)).padding(10.dp).verticalScroll(rememberScrollState()),
  verticalArrangement = Arrangement.spacedBy(8.dp),
 ) {
  Text("📜 #rules // OFFICIAL RIFT SECTOR DIRECTIVES & FAIR-PLAY CODE", color = Color(0xFFA78BFA), fontSize = 11.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
  listOf(
   "1. HUMAN-ONLY TELEMETRY: Online pilot counters in NEXUS and Ranked Mode cards strictly count real human pilots — AI bots are never counted as human players.",
   "2. SEAMLESS BOT REPLACEMENT: Every 20-ship arena sector backfills empty slots with AI bots and immediately replaces 1 AI bot per joining human pilot.",
   "3. SYNDICATE WARS ELIGIBILITY: Queuing for Ranked Syndicate Wars requires an enlisted [TAG] Syndicate and 100% same-Syndicate Armada wingmen.",
   "4. SAFE ZONE & OPEN-SPACE ANTI-AFK: Center Safe Zone disarms weapons and warps out idle ships after 15.0s. Open-space inactivity triggers -5% Max Flux/s drain.",
   "5. 100-POINT UNIVERSAL PARITY: All 9 standard hulls and the #10 Rift Frame adhere strictly to the 100-point budget over the Universal Baseline Hull Floor.",
  ).forEach { rule -> Text(rule, color = Color(0xFFE2E8F0), fontSize = 9.5.sp, lineHeight = 14.sp) }
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
 Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
  Text("🛠️ #bug-report // SUBMIT SECTOR ANOMALY OR BUG TELEMETRY", color = Color(0xFFFB7185), fontSize = 10.5.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
  Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
   categories.forEach { cat ->
    val sel = selectedCategory == cat
    Box(
     modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(if (sel) Color(0xFF4C0519) else Color(0xFF0F172A))
      .border(1.dp, if (sel) Color(0xFFFB7185) else Color(0xFF334155), RoundedCornerShape(6.dp))
      .clickable { onSelectCategory(cat) }.padding(horizontal = 8.dp, vertical = 4.dp),
    ) { Text(cat, color = if (sel) Color.White else Color(0xFF94A3B8), fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace) }
   }
  }
  if (statusBanner != null) Text(statusBanner, color = Color(0xFF34D399), fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace)
  LazyColumn(
   modifier = Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color(0x55030712)).padding(8.dp),
   verticalArrangement = Arrangement.spacedBy(5.dp),
  ) {
   if (submittedPosts.isEmpty()) item { Text("Describe any physics, controller, or HUD issue below to log it directly to the channel.", color = Color(0xFF64748B), fontSize = 9.sp) }
   items(submittedPosts, key = { it.id }) { post -> ChannelPostRow(post) }
  }
  ChannelInputComposer("Describe bug or anomaly ($selectedCategory)…", description, onDescriptionChange, onSubmit)
 }
}

@Composable
private fun NexusWelcomeTelemetryCard() {
 Column(
  modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color(0x8C0B172A))
   .border(1.dp, Color(0x55B388FF), RoundedCornerShape(10.dp)).padding(12.dp),
  verticalArrangement = Arrangement.spacedBy(5.dp),
 ) {
  Text("✦ NEXUS CORE TELEMETRY LINK ESTABLISHED", color = Color(0xFF00F5D4), fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace)
  Text(
   "I am NEXUS, the omniscient Rift Core tactical intelligence inside your Sector Command Hub. Ask me anything about RIFT — exact laser pixel dimensions, Newtonian drift & wall-bounce equations, 100-point Interdictor & Rift Frame theorycrafting, Flux Core & Stasis Breach tactics, or global highest bounty records.",
   color = Color(0xFFE2E8F0), fontSize = 11.sp, lineHeight = 15.5.sp,
  )
  Text("Tap a starter query above, switch channels on the left, or deploy your Armada on the right.", color = Color(0xFFC4B5FD), fontSize = 9.5.sp, fontWeight = FontWeight.Medium, fontFamily = FontFamily.Monospace)
 }
}

@Composable
private fun NexusMessageBubble(message: NexusChatMessage) {
 val isUser = message.role == NexusRole.USER
 val shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomStart = if (isUser) 12.dp else 3.dp, bottomEnd = if (isUser) 3.dp else 12.dp)
 Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start) {
  Column(
   modifier = Modifier.widthIn(max = 590.dp).clip(shape)
    .background(if (isUser) Color(0xB80E2A47) else Color(0xBF091324))
    .border(1.dp, if (isUser) Color(0x8838BDF8) else Color(0x8800F5D4), shape)
    .padding(horizontal = 12.dp, vertical = 8.dp),
   verticalArrangement = Arrangement.spacedBy(4.dp),
  ) {
   Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
    Text(if (isUser) "PILOT QUERY" else "NEXUS // RIFT CORE", color = if (isUser) Color(0xFF38BDF8) else Color(0xFF00F5D4), fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace)
    if (message.isStreaming) Text("● STREAMING TELEMETRY", color = Color(0xFFD8B4FE), fontSize = 8.5.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
   }
   if (message.text.isEmpty() && message.isStreaming) {
    Text("Scanning live Rift Core engine matrices & leaderboard records…", color = Color(0xFF94A3B8), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
   } else {
    val formatted = remember(message.text) { formatNexusMarkdown(message.text) }
    Text(formatted, color = Color(0xFFF1F5F9), fontSize = 11.sp, lineHeight = 16.sp)
   }
  }
 }
}

@Composable
private fun NexusErrorBanner(errorMessage: String, canRetry: Boolean, onRetry: () -> Unit) {
 Row(
  modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color(0xCC2A0A12))
   .border(1.dp, Color(0xFFFF3B4E), RoundedCornerShape(10.dp)).padding(horizontal = 12.dp, vertical = 8.dp)
   .testTag("nexus_error_banner"),
  verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween,
 ) {
  Column(modifier = Modifier.weight(1f)) {
   Text("⚠ RIFT CORE LINK INTERRUPTED", color = Color(0xFFFF3B4E), fontSize = 9.5.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace)
   Spacer(modifier = Modifier.height(2.dp))
   Text(errorMessage, color = Color(0xFFFECDD3), fontSize = 10.5.sp)
  }
  if (canRetry) {
   Spacer(modifier = Modifier.width(8.dp))
   Button(
    onClick = onRetry,
    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4C0519), contentColor = Color(0xFFFECDD3)),
    border = BorderStroke(1.dp, Color(0xFFFF3B4E)), shape = RoundedCornerShape(8.dp),
    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
    modifier = Modifier.minimumInteractiveComponentSize().height(36.dp).testTag("nexus_retry_button"),
   ) { Text("↻ ${stringResource(R.string.nexus_retry)}", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace) }
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
    withStyle(SpanStyle(color = Color(0xFFFFE800), fontWeight = FontWeight.Bold)) { append(raw.substring(i + 2, close)) }
    i = close + 2
    continue
   }
  }
  if (raw[i] == '`') {
   val close = raw.indexOf('`', i + 1)
   if (close != -1 && close > i + 1) {
    withStyle(SpanStyle(color = Color(0xFF00F5D4), fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold)) { append(raw.substring(i + 1, close)) }
    i = close + 1
    continue
   }
  }
  append(raw[i])
  i++
 }
}
