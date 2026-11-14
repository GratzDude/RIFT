package com.example

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.KeyEvent
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.FriendPilotEntry
import com.example.data.LocalPilotPrefs
import com.example.data.PilotAchievement
import com.example.data.RiftRepository
import com.example.data.SyndicateChatMessage
import com.example.engine.RankedGameMode
import com.example.engine.RiftArenaView
import com.example.engine.RiftGameEngine
import com.example.engine.RiftNexusKnowledge
import com.example.engine.RiftSoundSynthesizer
import com.example.ui.ArmadaFleetModal
import com.example.ui.AuthScreen
import com.example.ui.CURRENT_RIFT_BUILD_VERSION
import com.example.ui.CodexModal
import com.example.ui.DossierModal
import com.example.ui.HangarModal
import com.example.ui.NexusAiConsoleModal
import com.example.ui.NexusAiViewModel
import com.example.ui.NexusStreamState
import com.example.ui.RiftChangelogModal
import com.example.ui.RiftHudOverlay
import com.example.ui.RiftShardsStoreModal
import com.example.ui.RiftViewModel
import com.example.ui.SettingsModal
import com.example.ui.SyndicateChatModal
import com.example.ui.SyndicatesModal
import com.example.ui.UiState
import com.example.ui.onGoogleSignInClicked
import com.example.ui.signOut
import com.example.ui.theme.MyApplicationTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
  val listener = FirebaseAuth.AuthStateListener { auth -> trySend(auth.currentUser) }
  addAuthStateListener(listener)
  awaitClose { removeAuthStateListener(listener) }
}

private enum class ActiveModal {
  NONE,
  HANGAR,
  CODEX,
  SYNDICATES,
  ARMADA,
  CHAT,
  DOSSIER,
  SETTINGS,
  RIFT_SHARDS_STORE,
  NEXUS,
}

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    configureAppCheck(this, intent)
    enableEdgeToEdge()
    hideSystemBars()
    // P0: Native Landscape Enforcement (SCREEN_ORIENTATION_SENSOR_LANDSCAPE)
    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE

    setContent {
      MyApplicationTheme {
        AppNavigation()
      }
    }
  }

  override fun onWindowFocusChanged(hasFocus: Boolean) {
    super.onWindowFocusChanged(hasFocus)
    if (hasFocus) {
      hideSystemBars()
    }
  }

  private fun hideSystemBars() {
    val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
    windowInsetsController.systemBarsBehavior =
      WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
  }
}

@Composable
fun AppNavigation(
  auth: FirebaseAuth = Firebase.auth,
) {
  val context = LocalContext.current
  val localPrefs = remember { LocalPilotPrefs(context) }
  val permanentDeviceUid = remember { localPrefs.getOrCreatePermanentDeviceUid() }

  val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
  var isGuestMode by remember { mutableStateOf(localPrefs.isGuestBypassEnabled()) }
  var lastSeenBuild by remember { mutableIntStateOf(localPrefs.getLastSeenChangelogBuild()) }
  var showChangelogBeforeAuth by remember {
    mutableStateOf(lastSeenBuild < CURRENT_RIFT_BUILD_VERSION)
  }
  var manualChangelogOpen by remember { mutableStateOf(false) }
  val user = currentUser

  if (showChangelogBeforeAuth) {
    RiftChangelogModal(
      lastSeenBuild = lastSeenBuild,
      onAcknowledgeAndContinue = {
        localPrefs.setLastSeenChangelogBuild(CURRENT_RIFT_BUILD_VERSION)
        lastSeenBuild = CURRENT_RIFT_BUILD_VERSION
        showChangelogBeforeAuth = false
        manualChangelogOpen = false
      },
    )
  } else {
    Box(modifier = Modifier.fillMaxSize()) {
      if (user == null && !isGuestMode) {
        AuthScreen(
          onAuthSuccess = {},
          onPlayAsGuest = {
            localPrefs.setGuestBypassEnabled(true)
            if (localPrefs.getCallsign() == "COMMANDER") {
              localPrefs.setCallsign("GUEST_${(1000..9999).random()}")
            }
            isGuestMode = true
          },
          onOpenChangelog = { manualChangelogOpen = true },
        )
      } else {
        val uid = user?.uid ?: permanentDeviceUid
        val initialCallsign = localPrefs.getCallsign()

        key(uid) {
          RiftArenaScreen(
            currentUserId = uid,
            initialCallsign = initialCallsign,
            isGoogleAuthenticated = user != null,
            localPrefs = localPrefs,
            onOpenChangelog = { manualChangelogOpen = true },
          )
        }
      }

      if (manualChangelogOpen) {
        RiftChangelogModal(
          lastSeenBuild = lastSeenBuild,
          onAcknowledgeAndContinue = {
            localPrefs.setLastSeenChangelogBuild(CURRENT_RIFT_BUILD_VERSION)
            lastSeenBuild = CURRENT_RIFT_BUILD_VERSION
            showChangelogBeforeAuth = false
            manualChangelogOpen = false
          },
        )
      }
    }
  }
}

@Composable
fun RiftArenaScreen(
  currentUserId: String,
  initialCallsign: String,
  isGoogleAuthenticated: Boolean,
  localPrefs: LocalPilotPrefs = LocalPilotPrefs(LocalContext.current),
  onOpenChangelog: () -> Unit = {},
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember { CredentialManager.create(context) }

  val viewModel: RiftViewModel =
    viewModel(
      key = currentUserId,
      factory =
        viewModelFactory {
          initializer {
            val app =
              checkNotNull(this[APPLICATION_KEY]) {
                "APPLICATION_KEY missing from CreationExtras"
              }
            val databaseId = app.getString(R.string.firestore_database_id)
            val db = FirebaseFirestore.getInstance(databaseId)
            RiftViewModel(RiftRepository(db), currentUserId, initialCallsign)
          }
        },
    )

  val dossierState by viewModel.dossierState.collectAsStateWithLifecycle()
  val leaderboardState by viewModel.leaderboardState.collectAsStateWithLifecycle()
  val sectorPilotsState by viewModel.sectorPilotsState.collectAsStateWithLifecycle()
  val cloudSyndicatesState by viewModel.cloudSyndicatesState.collectAsStateWithLifecycle()
  val sectorCommsState by viewModel.sectorCommsState.collectAsStateWithLifecycle()

  val nexusViewModel: NexusAiViewModel = viewModel(key = "nexus_$currentUserId")
  val nexusMessages by nexusViewModel.messages.collectAsStateWithLifecycle()
  val nexusStreamState by nexusViewModel.streamState.collectAsStateWithLifecycle()
  val isNexusStreaming =
    nexusStreamState is NexusStreamState.Loading || nexusStreamState is NexusStreamState.Streaming

  val sfx = remember { RiftSoundSynthesizer() }
  var sfxEnabled by remember { mutableStateOf(true) }
  var currentFps by remember { mutableIntStateOf(60) }
  var frameTick by remember { mutableIntStateOf(0) }
  var toastMessage by remember {
    mutableStateOf<String?>("⚡ RIFT FFA ARENA ONLINE // 360° AIM & NAMED ARMADAS ACTIVE")
  }
  var toastVersion by remember { mutableIntStateOf(0) }
  var unreadCommsCount by remember { mutableIntStateOf(0) }

  var bootToNexusHub by remember { mutableStateOf(localPrefs.getBootToNexusHub()) }
  var showStartupChangelogQuickJoin by remember {
    mutableStateOf(!localPrefs.getBootToNexusHub())
  }
  var activeModal by remember {
    mutableStateOf(if (localPrefs.getBootToNexusHub()) ActiveModal.NEXUS else ActiveModal.NONE)
  }
  var previousModalBeforeStore by remember { mutableStateOf(ActiveModal.DOSSIER) }
  var previousModalBeforeNexus by remember { mutableStateOf(ActiveModal.NONE) }
  var callsign by remember { mutableStateOf(localPrefs.getCallsign()) }
  var syndicateTag by remember { mutableStateOf(localPrefs.getSyndicateTag()) }
  var syndicateName by remember { mutableStateOf(localPrefs.getSyndicateName()) }
  var selectedShipId by remember { mutableIntStateOf(localPrefs.getPreferredShipId().coerceIn(1, 10)) }
  var selectedMode by remember { mutableStateOf("FFA") }
  var activeCustomSlotId by remember(currentUserId) {
    mutableStateOf(localPrefs.getActiveCustomSlotId(currentUserId))
  }
  var activeCustomSlot by remember(currentUserId, activeCustomSlotId) {
    val loaded = localPrefs.getCustomShipSlot(activeCustomSlotId, currentUserId)
    com.example.engine.RiftCodex.updateActiveCustomShipSlot(loaded)
    mutableStateOf(loaded)
  }
  var slot9BUnlocked by remember(currentUserId) {
    mutableStateOf(localPrefs.isCustomSlotUnlocked("Frame B", currentUserId))
  }
  var slot9CUnlocked by remember(currentUserId) {
    mutableStateOf(localPrefs.isCustomSlotUnlocked("Frame C", currentUserId))
  }
  var cameraZoom by remember { mutableFloatStateOf(localPrefs.getCameraZoom()) }
  var playfieldTextScale by remember { mutableFloatStateOf(localPrefs.getPlayfieldTextScale()) }
  var syndicateStatusBanner by remember { mutableStateOf<String?>(null) }
  var armadaStatusBanner by remember { mutableStateOf<String?>(null) }
  var friendsRefreshKey by remember { mutableIntStateOf(0) }
  var syndicateChannelsRefreshKey by remember { mutableIntStateOf(0) }
  var blockedPilotUids by remember(currentUserId) {
    mutableStateOf(localPrefs.getBlockedPilotUids())
  }
  var allTimePeakSnapshot by remember(currentUserId) {
    mutableStateOf(localPrefs.getAllTimePeakBountySnapshot(currentUserId))
  }
  var pvpPeakSnapshot by remember(currentUserId) {
    mutableStateOf(localPrefs.getPvpPeakBountySnapshot(currentUserId))
  }
  var syndicateHistory by remember(currentUserId) {
    mutableStateOf(localPrefs.getSyndicateServiceHistory(currentUserId))
  }

  // Syndicate Chat Messages State (starts clean; populated by real player Comms & Armada Recruit banners)
  val chatMessages = remember { mutableStateListOf<SyndicateChatMessage>() }

  val engine = remember {
    RiftGameEngine(
      sfx = sfx,
      onStatsUpdated = {},
      onPrizeToast = { msg ->
        toastMessage = msg
        toastVersion++
      },
      onArmadaTierKill = { tierSize, bounty ->
        localPrefs.recordArmadaTierStat(tierSize, 1L, bounty)
      },
    ).apply {
      pvpKills = localPrefs.getPvpKills()
      pvpDeaths = localPrefs.getPvpDeaths()
      pvpBountyPeak = localPrefs.getPvpBountyPeak()
      allTimeKills = localPrefs.getAllTimeKills()
      allTimeDeaths = localPrefs.getAllTimeDeaths()
      allTimeBountyPeak = localPrefs.getAllTimeBountyPeak()
      convergencesWon = localPrefs.getConvergencesWon()
      hvtEliminations = localPrefs.getHvtEliminations()
      greensCollected = localPrefs.getGreensCollected()
      xpFromBuffs = localPrefs.getXpFromBuffs(currentUserId).toFloat()
      xpFromDamage = localPrefs.getXpFromDamage(currentUserId).toFloat()
      xpFromAccuracy = localPrefs.getXpFromAccuracy(currentUserId).toFloat()
      xpFromBountyEarned = localPrefs.getXpFromBountyEarned(currentUserId).toFloat()
      xpFromBountyKilled = localPrefs.getXpFromBountyKilled(currentUserId).toFloat()
      pilotLevel = localPrefs.getPilotLevel(currentUserId)
      riftShards = localPrefs.getRiftShards(currentUserId)
      highestMilestoneLevelClaimed = localPrefs.getHighestMilestoneLevelClaimed(currentUserId)
      spentCustomizationPoints = localPrefs.getSpentCustomizationPoints(currentUserId)
      unlockedExtraSlots = localPrefs.getUnlockedExtraSlots(currentUserId)
      aiShipDesignTokens = localPrefs.getAiShipDesignTokens(currentUserId)
      this.cameraZoom = localPrefs.getCameraZoom()
      this.playfieldTextScale = localPrefs.getPlayfieldTextScale()

      localPlayer.callsign = callsign
      localPlayer.syndicateTag = syndicateTag
      val savedArmada = localPrefs.getActiveArmadaName()
      if (savedArmada.isNotEmpty()) {
        formOrJoinArmada(savedArmada, localPrefs.getFriendsList().take(2))
      }
      switchLocalPlayerShip(selectedShipId, forceFromHangar = true)
      rebuildRosterForMode(selectedMode)
    }
  }

  // Bind engine stats callback to persist locally and sync to cloud
  DisposableEffect(engine, viewModel) {
    engine.onStatsUpdated = {
      localPrefs.setActiveArmadaName(engine.activeArmadaName)
      localPrefs.saveCombatStats(
        pvpKills = engine.pvpKills,
        pvpDeaths = engine.pvpDeaths,
        pvpBountyPeak = engine.pvpBountyPeak,
        allTimeKills = engine.allTimeKills,
        allTimeDeaths = engine.allTimeDeaths,
        allTimeBountyPeak = engine.allTimeBountyPeak,
        convergencesWon = engine.convergencesWon,
        hvtEliminations = engine.hvtEliminations,
        greensCollected = engine.greensCollected,
      )
      localPrefs.savePilotProgression(
        uid = currentUserId,
        xpFromBuffs = engine.xpFromBuffs.toLong(),
        xpFromDamage = engine.xpFromDamage.toLong(),
        xpFromAccuracy = engine.xpFromAccuracy.toLong(),
        xpFromBountyEarned = engine.xpFromBountyEarned.toLong(),
        xpFromBountyKilled = engine.xpFromBountyKilled.toLong(),
        riftShards = engine.riftShards,
        highestMilestoneLevelClaimed = engine.highestMilestoneLevelClaimed,
      )
      localPrefs.setSpentCustomizationPoints(engine.spentCustomizationPoints, currentUserId)
      localPrefs.setUnlockedExtraSlots(engine.unlockedExtraSlots, currentUserId)
      localPrefs.setAiShipDesignTokens(engine.aiShipDesignTokens, currentUserId)
      if (engine.localPlayer.respawnTimer <= 0f) {
        localPrefs.maybeRecordPeakBountySnapshot(
          snapshot = engine.currentLifeStats.toSnapshot(),
          hadPvp = engine.currentLifeHadPvp,
          isFinalizedLifeEnd = false,
          uid = currentUserId,
        )
        allTimePeakSnapshot = localPrefs.getAllTimePeakBountySnapshot(currentUserId)
        pvpPeakSnapshot = localPrefs.getPvpPeakBountySnapshot(currentUserId)
      }
      viewModel.syncNativePilotStats(
        callsign = engine.localPlayer.callsign,
        shipId = engine.localPlayer.shipId,
        foundry = engine.localPlayer.spec.role,
        mode = engine.mode,
        pvpKills = engine.pvpKills,
        pvpDeaths = engine.pvpDeaths,
        pvpBountyPeak = engine.pvpBountyPeak,
        allTimeKills = engine.allTimeKills,
        allTimeDeaths = engine.allTimeDeaths,
        allTimeBountyPeak = engine.allTimeBountyPeak,
        convergencesWon = engine.convergencesWon,
        syndicateTag = engine.localPlayer.syndicateTag,
        armadaTag = engine.activeArmadaName.ifBlank { "SOLO" },
        armadaTierSize = engine.getCurrentArmadaSize(),
      )
      frameTick++
    }
    engine.onLifeRunEnded = { finalizedSnapshot, hadPvp ->
      localPrefs.maybeRecordPeakBountySnapshot(
        snapshot = finalizedSnapshot,
        hadPvp = hadPvp,
        isFinalizedLifeEnd = true,
        uid = currentUserId,
      )
      allTimePeakSnapshot = localPrefs.getAllTimePeakBountySnapshot(currentUserId)
      pvpPeakSnapshot = localPrefs.getPvpPeakBountySnapshot(currentUserId)
    }
    engine.onSyndicateContribution = { killsDelta, currentBounty, winsDelta ->
      localPrefs.recordSyndicateContribution(
        addKills = killsDelta,
        currentBounty = currentBounty,
        addPublicWins = winsDelta,
        uid = currentUserId,
      )
      syndicateHistory = localPrefs.getSyndicateServiceHistory(currentUserId)
    }
    engine.onSyndicateWarFinished = { won, delta ->
      localPrefs.recordSyndicateWarOutcome(syndicateTag, won, delta)
      syndicateChannelsRefreshKey++
    }
    onDispose {
      sfx.release()
    }
  }

  // Load persisted HUD layout offsets, Calibration overrides, and AI-Forged Buffs on startup
  LaunchedEffect(Unit) {
    com.example.engine.RiftCalibrationState.loadPersistedLayout(context)
    com.example.engine.RiftCalibrationState.applyCalibrationChangesLive(engine, null)
  }

  // P1: Non-Blocking Tactical Toasts auto-fade after 1.5s
  LaunchedEffect(toastVersion) {
    if (toastMessage != null) {
      delay(1500L)
      toastMessage = null
    }
  }

  // Sync cloud dossier if higher than local stats
  LaunchedEffect(dossierState) {
    val cloudDossier = (dossierState as? UiState.Success)?.data ?: return@LaunchedEffect
    if (cloudDossier.allTimeKills > engine.allTimeKills) {
      engine.allTimeKills = cloudDossier.allTimeKills
      engine.allTimeDeaths = cloudDossier.allTimeDeaths
      engine.allTimeBountyPeak = cloudDossier.allTimeBountyPeak
      engine.pvpKills = cloudDossier.pvpKills
      engine.pvpDeaths = cloudDossier.pvpDeaths
      engine.pvpBountyPeak = cloudDossier.pvpBountyPeak
      engine.convergencesWon = cloudDossier.convergencesWon
    }
  }

  // Push remote sector pilots from Firestore into engine
  LaunchedEffect(sectorPilotsState) {
    val remoteList = (sectorPilotsState as? UiState.Success)?.data ?: return@LaunchedEffect
    engine.updateRemotePilots(remoteList, currentUserId)
  }

  // Merge cloud-registered Syndicates across devices
  LaunchedEffect(cloudSyndicatesState) {
    val cloudList = (cloudSyndicatesState as? UiState.Success)?.data ?: return@LaunchedEffect
    localPrefs.mergeCloudSyndicates(cloudList)
  }

  // Merge live Sector Comms & Armada Recruit Banners across devices
  LaunchedEffect(sectorCommsState) {
    val remoteComms = (sectorCommsState as? UiState.Success)?.data ?: return@LaunchedEffect
    val existingIds = chatMessages.map { it.id }.toSet()
    var newIncomingCount = 0
    for (msg in remoteComms) {
      if (msg.id.isNotEmpty() && msg.id !in existingIds) {
        chatMessages.add(msg)
        if (msg.senderUid != currentUserId && activeModal != ActiveModal.CHAT) {
          newIncomingCount++
        }
      }
    }
    if (newIncomingCount > 0) {
      unreadCommsCount += newIncomingCount
    }
  }

  // High-frequency live telemetry heartbeat for both Guest and Google Authenticated devices
  LaunchedEffect(currentUserId) {
    while (true) {
      delay(350L)
      val p = engine.localPlayer
      viewModel.syncNativeLiveState(
        sectorId = engine.mode,
        callsign = p.callsign,
        syndicateTag = p.syndicateTag,
        armadaName = p.armadaName,
        shipId = p.shipId,
        freq = p.freq,
        x = p.x,
        y = p.y,
        vx = p.vx,
        vy = p.vy,
        angle = p.angle,
        weaponAngle = p.weaponAngle,
        isFiringPrimary = engine.fireStickActive && !engine.isCancelActive && p.canFire,
        isAfterburning = p.afterburning,
        isCloaked = p.isCloaked,
        flux = p.flux,
        maxFlux = p.maxFlux,
        bounty = p.bounty,
      )
    }
  }

  var arenaViewRef by remember { mutableStateOf<RiftArenaView?>(null) }
  val topNavModals = remember {
    listOf(
      ActiveModal.HANGAR,
      ActiveModal.CODEX,
      ActiveModal.SYNDICATES,
      ActiveModal.ARMADA,
      ActiveModal.CHAT,
      ActiveModal.DOSSIER,
      ActiveModal.SETTINGS,
    )
  }

  LaunchedEffect(activeModal, com.example.engine.RiftCalibrationState.isCalibrationModalOpen) {
    if (
      activeModal == ActiveModal.NONE &&
        !com.example.engine.RiftCalibrationState.isCalibrationModalOpen
    ) {
      arenaViewRef?.requestFocus()
    }
  }

  BackHandler {
    if (com.example.engine.RiftCalibrationState.isCalibrationModalOpen) {
      com.example.engine.RiftCalibrationState.isCalibrationModalOpen = false
    } else if (activeModal == ActiveModal.NEXUS) {
      activeModal = previousModalBeforeNexus
      previousModalBeforeNexus = ActiveModal.NONE
    } else if (activeModal != ActiveModal.NONE) {
      activeModal = ActiveModal.NONE
    } else {
      activeModal = ActiveModal.HANGAR
    }
  }

  Box(
    modifier =
      Modifier.fillMaxSize()
        .background(Color.Black)
        .onPreviewKeyEvent { keyEvent ->
          val native = keyEvent.nativeKeyEvent
          if (native.action == KeyEvent.ACTION_DOWN && native.repeatCount == 0) {
            when (native.keyCode) {
              KeyEvent.KEYCODE_BUTTON_START -> {
                com.example.engine.RiftCalibrationState.gamepadConnected = true
                activeModal =
                  if (activeModal == ActiveModal.HANGAR) ActiveModal.NONE else ActiveModal.HANGAR
                sfx.playMutedClick()
                true
              }

              KeyEvent.KEYCODE_BUTTON_SELECT -> {
                com.example.engine.RiftCalibrationState.gamepadConnected = true
                com.example.engine.RiftCalibrationState.isCalibrationModalOpen =
                  !com.example.engine.RiftCalibrationState.isCalibrationModalOpen
                sfx.playMutedClick()
                true
              }

              KeyEvent.KEYCODE_BUTTON_L1 -> {
                if (activeModal != ActiveModal.NONE) {
                  com.example.engine.RiftCalibrationState.gamepadConnected = true
                  val curIdx = topNavModals.indexOf(activeModal)
                  val nextIdx =
                    if (curIdx <= 0) topNavModals.lastIndex else curIdx - 1
                  val nextModal = topNavModals[nextIdx]
                  if (nextModal == ActiveModal.CHAT) unreadCommsCount = 0
                  activeModal = nextModal
                  sfx.playMutedClick()
                  true
                } else {
                  false
                }
              }

              KeyEvent.KEYCODE_BUTTON_R1 -> {
                if (activeModal != ActiveModal.NONE) {
                  com.example.engine.RiftCalibrationState.gamepadConnected = true
                  val curIdx = topNavModals.indexOf(activeModal)
                  val nextIdx =
                    if (curIdx < 0 || curIdx >= topNavModals.lastIndex) 0 else curIdx + 1
                  val nextModal = topNavModals[nextIdx]
                  if (nextModal == ActiveModal.CHAT) unreadCommsCount = 0
                  activeModal = nextModal
                  sfx.playMutedClick()
                  true
                } else {
                  false
                }
              }

              KeyEvent.KEYCODE_BUTTON_B -> {
                if (com.example.engine.RiftCalibrationState.isCalibrationModalOpen) {
                  com.example.engine.RiftCalibrationState.gamepadConnected = true
                  com.example.engine.RiftCalibrationState.isCalibrationModalOpen = false
                  sfx.playMutedClick()
                  true
                } else if (
                  activeModal != ActiveModal.NONE && activeModal != ActiveModal.HANGAR
                ) {
                  com.example.engine.RiftCalibrationState.gamepadConnected = true
                  if (activeModal == ActiveModal.NEXUS) {
                    activeModal = previousModalBeforeNexus
                    previousModalBeforeNexus = ActiveModal.NONE
                  } else if (activeModal == ActiveModal.RIFT_SHARDS_STORE) {
                    activeModal = previousModalBeforeStore
                  } else {
                    activeModal = ActiveModal.NONE
                  }
                  sfx.playMutedClick()
                  true
                } else {
                  false
                }
              }

              else -> false
            }
          } else {
            false
          }
        }
        .onKeyEvent { keyEvent ->
          val native = keyEvent.nativeKeyEvent
          if (
            native.action == KeyEvent.ACTION_DOWN &&
              native.repeatCount == 0 &&
              native.keyCode == KeyEvent.KEYCODE_BUTTON_B &&
              activeModal != ActiveModal.NONE
          ) {
            com.example.engine.RiftCalibrationState.gamepadConnected = true
            activeModal = ActiveModal.NONE
            sfx.playMutedClick()
            true
          } else {
            false
          }
        }
        .testTag("rift_arena_native_container")
  ) {
    // 1. High-Performance 60+ FPS Native Canvas View
    AndroidView(
      modifier = Modifier.fillMaxSize().testTag("rift_arena_canvas_view"),
      factory = { ctx ->
        RiftArenaView(
          context = ctx,
          engine = engine,
          onHudTelemetryTick = { fps ->
            currentFps = fps
            frameTick++
          },
          onGamepadCycleCameraZoom = { deltaSteps ->
            val nextZoom = (cameraZoom + deltaSteps * 0.15f).coerceIn(0.50f, 1.50f)
            cameraZoom = nextZoom
            engine.cameraZoom = nextZoom
            localPrefs.setCameraZoom(nextZoom)
          },
          onGamepadToggleHangar = {
            activeModal =
              if (activeModal == ActiveModal.HANGAR) ActiveModal.NONE else ActiveModal.HANGAR
            sfx.playMutedClick()
          },
          onGamepadToggleCalibration = {
            com.example.engine.RiftCalibrationState.isCalibrationModalOpen =
              !com.example.engine.RiftCalibrationState.isCalibrationModalOpen
            sfx.playMutedClick()
          },
          onGamepadOpenNexus = {
            previousModalBeforeNexus = ActiveModal.NONE
            activeModal = ActiveModal.NEXUS
            sfx.playMutedClick()
          },
        ).apply {
          layoutParams =
            ViewGroup.LayoutParams(
              ViewGroup.LayoutParams.MATCH_PARENT,
              ViewGroup.LayoutParams.MATCH_PARENT,
            )
          arenaViewRef = this
        }
      },
    )

    // 2. Native Fullscreen HUD Overlay (7 Decoupled Top Navigation Tabs + Centered Active Powerups Dock)
    RiftHudOverlay(
      engine = engine,
      fps = currentFps,
      sfxEnabled = sfxEnabled,
      unreadCommsCount = unreadCommsCount,
      toastMessage = toastMessage,
      frameTick = frameTick,
      cameraZoom = cameraZoom,
      onOpenHangar = { activeModal = ActiveModal.HANGAR },
      onOpenCodex = { activeModal = ActiveModal.CODEX },
      onOpenSyndicateCommand = {
        syndicateStatusBanner = null
        activeModal = ActiveModal.SYNDICATES
      },
      onOpenArmadaFleet = {
        armadaStatusBanner = null
        activeModal = ActiveModal.ARMADA
      },
      onOpenComms = {
        unreadCommsCount = 0
        activeModal = ActiveModal.CHAT
      },
      onOpenDossier = { activeModal = ActiveModal.DOSSIER },
      onOpenSettings = { activeModal = ActiveModal.SETTINGS },
      onCycleCameraZoom = {
        val nextZoom =
          when {
            cameraZoom < 0.70f -> 0.75f
            cameraZoom < 0.95f -> 1.0f
            cameraZoom < 1.20f -> 1.25f
            cameraZoom < 1.45f -> 1.5f
            else -> 0.5f
          }
        cameraZoom = nextZoom
        engine.cameraZoom = nextZoom
        localPrefs.setCameraZoom(nextZoom)
        toastMessage = String.format(Locale.US, "🔍 CAMERA ZOOM SET TO %.2fx", nextZoom)
        toastVersion++
      },
      isNexusStreaming = isNexusStreaming,
      onOpenNexus = {
        previousModalBeforeNexus = ActiveModal.NONE
        activeModal = ActiveModal.NEXUS
      },
      onEngageBrake = {
        engine.engageManualBrake()
        frameTick++
      },
      onToggleMultifire = {
        engine.toggleMultifire()
        frameTick++
      },
    )

    // 3. Dedicated Windows (Hangar, Codex, Syndicate Command, Armada Fleet Deck, Comms Drawer, Dossier, Settings)
    when (activeModal) {
      ActiveModal.HANGAR -> {
        val (_, _, hangarLvlFrac) = remember(frameTick) { engine.getXpProgressInCurrentLevel() }
        HangarModal(
          initialCallsign = callsign,
          syndicateTag = syndicateTag,
          syndicateName = syndicateName,
          selectedShipId = selectedShipId,
          selectedMode = selectedMode,
          pilotLevel = engine.pilotLevel,
          xpProgressFraction = hangarLvlFrac,
          customizationPointsTotal = engine.getCustomizationPointsTotal(),
          spentCustomizationPoints = activeCustomSlot.totalAllocatedPoints(),
          riftShards = engine.riftShards,
          unlockFullNinthShipOverride =
            com.example.engine.RiftCalibrationState.unlockFullNinthShipOverride,
          activeCustomSlot = activeCustomSlot,
          slot9BUnlocked = slot9BUnlocked,
          slot9CUnlocked = slot9CUnlocked,
          onOpenRiftShardsStore = {
            previousModalBeforeStore = ActiveModal.HANGAR
            activeModal = ActiveModal.RIFT_SHARDS_STORE
          },
          onOpenSyndicatesModal = {
            syndicateStatusBanner = null
            activeModal = ActiveModal.SYNDICATES
          },
          onSaveCustomSlot = { updatedSlot ->
            activeCustomSlot = updatedSlot
            localPrefs.saveCustomShipSlot(updatedSlot, currentUserId)
            com.example.engine.RiftCodex.updateActiveCustomShipSlot(updatedSlot)
            engine.spentCustomizationPoints = updatedSlot.totalAllocatedPoints()
            engine.syncLocalPlayerCustomShipSpec()
          },
          onSwitchCustomSlot = { targetSlotId ->
            activeCustomSlotId = targetSlotId
            localPrefs.setActiveCustomSlotId(targetSlotId, currentUserId)
            val loaded = localPrefs.getCustomShipSlot(targetSlotId, currentUserId)
            activeCustomSlot = loaded
            com.example.engine.RiftCodex.updateActiveCustomShipSlot(loaded)
            engine.spentCustomizationPoints = loaded.totalAllocatedPoints()
            engine.syncLocalPlayerCustomShipSpec()
            sfx.playPrize()
          },
          onUnlockCustomSlot = { targetSlotId ->
            if (engine.riftShards >= 300L) {
              engine.riftShards -= 300L
              engine.unlockedExtraSlots++
              localPrefs.setCustomSlotUnlocked(targetSlotId, true, currentUserId)
              slot9BUnlocked = localPrefs.isCustomSlotUnlocked("Frame B", currentUserId)
              slot9CUnlocked = localPrefs.isCustomSlotUnlocked("Frame C", currentUserId)
              activeCustomSlotId = targetSlotId
              localPrefs.setActiveCustomSlotId(targetSlotId, currentUserId)
              val loaded = localPrefs.getCustomShipSlot(targetSlotId, currentUserId)
              activeCustomSlot = loaded
              com.example.engine.RiftCodex.updateActiveCustomShipSlot(loaded)
              engine.spentCustomizationPoints = loaded.totalAllocatedPoints()
              engine.syncLocalPlayerCustomShipSpec()
              sfx.playPrize()
              true
            } else {
              false
            }
          },
          onRespecCustomSlot = {
            if (engine.riftShards >= 50L && activeCustomSlot.totalAllocatedPoints() > 0) {
              engine.riftShards -= 50L
              val zeroed =
                activeCustomSlot.copy(
                  speedPts = 0,
                  thrustPts = 0,
                  turnPts = 0,
                  maxFluxPts = 0,
                  rechargePts = 0,
                  primaryArchetype = "LIGHT_PULSE",
                  secondaryType = "NONE",
                )
              activeCustomSlot = zeroed
              localPrefs.saveCustomShipSlot(zeroed, currentUserId)
              com.example.engine.RiftCodex.updateActiveCustomShipSlot(zeroed)
              engine.spentCustomizationPoints = 0
              engine.syncLocalPlayerCustomShipSpec()
              sfx.playPrize()
              true
            } else {
              false
            }
          },
          onForgeAiHullDesign = { promptText, hullTitle ->
            val isFirstFree = activeCustomSlot.aiGenerationsCount == 0
            val canAfford = isFirstFree || engine.aiShipDesignTokens > 0 || engine.riftShards >= 150L
            if (!canAfford) {
              Pair(false, "⚠️ NEED 150❖ RIFT SHARDS FOR AI HULL RE-DESIGN (TAP [+ GET RIFT SHARDS]).")
            } else {
              if (!isFirstFree) {
                if (engine.aiShipDesignTokens > 0) {
                  engine.aiShipDesignTokens--
                } else {
                  engine.riftShards -= 150L
                }
              }
              val nextGenCount = activeCustomSlot.aiGenerationsCount + 1
              val baseRecipe =
                com.example.engine.CustomHullVisualRecipe.synthesizeFromPrompt(
                  rawPrompt = promptText,
                  variationSalt = nextGenCount,
                )
              val finalTitle =
                hullTitle.trim().uppercase(Locale.US).take(18).ifEmpty { baseRecipe.hullName }
              val finalRecipe = baseRecipe.copy(hullName = finalTitle)
              val updatedSlot =
                activeCustomSlot.copy(
                  customName = finalTitle,
                  visualRecipe = finalRecipe,
                  aiGenerationsCount = nextGenCount,
                )
              activeCustomSlot = updatedSlot
              localPrefs.saveCustomShipSlot(updatedSlot, currentUserId)
              com.example.engine.RiftCodex.updateActiveCustomShipSlot(updatedSlot)
              engine.syncLocalPlayerCustomShipSpec()
              sfx.playPrize()
              val costNote = if (isFirstFree) "FREE 1ST DESIGN" else "-150❖ SHARDS"
              Pair(
                true,
                "🎨 AI HULL FORGED ($costNote): [$finalTitle] (${finalRecipe.silhouette} • ${finalRecipe.toJsonString().length}B JSON)",
              )
            }
          },
          onToggleDevLevel20Or100 = {
            com.example.engine.RiftCalibrationState.unlockFullNinthShipOverride =
              !com.example.engine.RiftCalibrationState.unlockFullNinthShipOverride
            com.example.engine.RiftCalibrationState.savePersistedLayout(context)
            engine.syncLocalPlayerCustomShipSpec()
            sfx.playPrize()
          },
          isNexusStreaming = isNexusStreaming,
          onOpenNexus = {
            previousModalBeforeNexus = ActiveModal.HANGAR
            activeModal = ActiveModal.NEXUS
          },
          onDeployPilot = { newCallsign, newShipId, _, asGuest ->
            if (asGuest) {
              localPrefs.setGuestBypassEnabled(true)
            }
            val cleanCallsign = localPrefs.setCallsign(newCallsign)
            val clampedShipId = newShipId.coerceIn(1, 10)
            callsign = cleanCallsign
            selectedShipId = clampedShipId
            selectedMode = "FFA"
            syndicateTag = localPrefs.getSyndicateTag()
            syndicateName = localPrefs.getSyndicateName()

            localPrefs.setPreferredShipId(clampedShipId)
            localPrefs.setPreferredMode("FFA")

            engine.localPlayer.callsign = cleanCallsign
            engine.localPlayer.syndicateTag = syndicateTag
            engine.syncLocalArmadaSelfSlot()
            engine.switchLocalPlayerShip(clampedShipId, forceFromHangar = true)
            engine.onStatsUpdated()
            activeModal = ActiveModal.NONE
          },
          onCloseModal = { activeModal = ActiveModal.NONE },
        )
      }

      ActiveModal.CODEX -> {
        CodexModal(
          isNexusStreaming = isNexusStreaming,
          onOpenNexus = {
            previousModalBeforeNexus = ActiveModal.CODEX
            activeModal = ActiveModal.NEXUS
          },
          onCloseModal = { activeModal = ActiveModal.NONE },
        )
      }

      ActiveModal.SYNDICATES -> {
        val directoryList =
          remember(syndicateTag, frameTick, cloudSyndicatesState) {
            val cloudList = (cloudSyndicatesState as? UiState.Success)?.data ?: emptyList()
            localPrefs.mergeCloudSyndicates(cloudList)
            localPrefs.getAllSyndicates()
          }
        val rosterMembers =
          remember(syndicateTag, callsign, selectedShipId, frameTick) {
            localPrefs.getMySyndicateRoster(
              currentUid = currentUserId,
              callsign = callsign,
              shipId = selectedShipId,
              bounty = engine.localPlayer.bounty.toLong(),
              kills = engine.allTimeKills,
            )
          }
        val achievements =
          remember(frameTick, syndicateTag) {
            listOf(
              PilotAchievement(
                id = "apex_hunter",
                title = "APEX HUNTER",
                description = "Eliminate a Sector High-Value Target (Bounty >= 250).",
                unlocked = engine.hvtEliminations >= 1L || engine.allTimeBountyPeak >= 250L,
                progressText = "${engine.hvtEliminations} HVT KILLS",
              ),
              PilotAchievement(
                id = "trench_runner",
                title = "TRENCH RUNNER",
                description = "Achieve 10+ total eliminations across sector skirmishes.",
                unlocked = engine.allTimeKills >= 10L,
                progressText = "${engine.allTimeKills} / 10 KILLS",
              ),
              PilotAchievement(
                id = "first_convergence",
                title = "FIRST CONVERGENCE",
                description = "Reach 250+ Bounty and become a High-Value Target.",
                unlocked = engine.allTimeBountyPeak >= 250L,
                progressText = "${engine.allTimeBountyPeak} / 250 BTY",
              ),
              PilotAchievement(
                id = "flux_overlord",
                title = "FLUX OVERLORD",
                description = "Collect 5+ Supply Crates in open space.",
                unlocked = engine.greensCollected >= 5L,
                progressText = "${engine.greensCollected} / 5 CRATES",
              ),
              PilotAchievement(
                id = "phase_striker",
                title = "SYNDICATE VANGUARD",
                description = "Charter or pledge allegiance to an active Syndicate.",
                unlocked =
                  syndicateTag.isNotEmpty() &&
                    syndicateTag != "GUEST" &&
                    syndicateTag != "FREE",
                progressText = if (syndicateTag.isNotBlank()) "[$syndicateTag] ACTIVE" else "UNAFFILIATED",
              ),
            )
          }

        SyndicatesModal(
          currentSyndicateTag = syndicateTag,
          currentSyndicateName = syndicateName,
          remainingLockDays = localPrefs.getRemainingSyndicateLockDays(),
          remainingLockFormatted = localPrefs.getRemainingSyndicateLockFormatted(),
          directoryList = directoryList,
          rosterMembers = rosterMembers,
          activeShipsInSector = engine.ships.toList(),
          achievements = achievements,
          statusBannerMessage = syndicateStatusBanner,
          onJoinSyndicate = { info ->
            val joined = localPrefs.joinOfficialSyndicate(info.tag, info.name)
            if (joined) {
              syndicateTag = localPrefs.getSyndicateTag()
              syndicateName = localPrefs.getSyndicateName()
              syndicateHistory = localPrefs.getSyndicateServiceHistory(currentUserId)
              engine.localPlayer.syndicateTag = syndicateTag
              engine.syncLocalArmadaSelfSlot()
              syndicateStatusBanner =
                "✓ JOINED [$syndicateTag] $syndicateName! 15-DAY LOCK COOLDOWN (15d 00h) ACTIVE."
              engine.onStatsUpdated()
            } else {
              syndicateStatusBanner =
                "🔒 CANNOT SWITCH SYNDICATES YET: ${localPrefs.getRemainingSyndicateLockFormatted()} REMAINING."
            }
          },
          onLeaveSyndicate = {
            val left = localPrefs.leaveSyndicate()
            if (left) {
              syndicateTag = localPrefs.getSyndicateTag()
              syndicateName = localPrefs.getSyndicateName()
              syndicateHistory = localPrefs.getSyndicateServiceHistory(currentUserId)
              engine.localPlayer.syndicateTag = syndicateTag
              engine.syncLocalArmadaSelfSlot()
              syndicateStatusBanner =
                "⚡ LEFT SYNDICATE -> STATUS: UNAFFILIATED. 15-DAY LOCK COOLDOWN TRIGGERED."
              engine.onStatsUpdated()
            } else {
              syndicateStatusBanner =
                "🔒 CANNOT LEAVE SYNDICATE YET: ${localPrefs.getRemainingSyndicateLockFormatted()} REMAINING."
            }
          },
          onCreateCustomSyndicate = { rawTag, rawName, rawMotto ->
            val res = localPrefs.createCustomSyndicate(rawTag, rawName, rawMotto)
            res.onSuccess { created ->
              syndicateTag = created.tag
              syndicateName = created.name
              syndicateHistory = localPrefs.getSyndicateServiceHistory(currentUserId)
              engine.localPlayer.syndicateTag = syndicateTag
              engine.syncLocalArmadaSelfSlot()
              viewModel.createCloudSyndicate(created)
              syndicateStatusBanner =
                "🏛️ CHARTERED [${created.tag}] ${created.name}! LOCKED FOR 15d 00h."
              engine.onStatsUpdated()
            }.onFailure { err ->
              syndicateStatusBanner = "⚠️ ${err.message}"
            }
          },
          onCloseModal = { activeModal = ActiveModal.NONE },
        )
      }

      ActiveModal.ARMADA -> {
        val friendsList =
          remember(syndicateTag, frameTick, friendsRefreshKey) { localPrefs.getFriendsList() }

        ArmadaFleetModal(
          currentSyndicateTag = syndicateTag,
          activeArmadaName = engine.activeArmadaName,
          armadaTierLabel = engine.getCurrentArmadaTierLabel(),
          armadaSlots = engine.activeArmadaSlots.toList(),
          friendsList = friendsList,
          blockedPilotUids = blockedPilotUids,
          statusBannerMessage = armadaStatusBanner,
          onCreateOrRenameArmada = { newArmadaName ->
            if (com.example.data.UgcModeration.containsProfanity(newArmadaName)) {
              armadaStatusBanner = "⚠️ ARMADA TAG REJECTED BY SAFETY FILTER."
            } else {
              engine.formOrJoinArmada(
                newArmadaName,
                if (engine.activeArmadaSlots.size <= 1) friendsList.take(1) else emptyList(),
              )
              armadaStatusBanner =
                "🛡️ ARMADA [${engine.activeArmadaName}] DEPLOYED (${engine.getCurrentArmadaSize()}/5 PILOTS)"
              friendsRefreshKey++
            }
          },
          onInviteFriendToArmada = { friend ->
            val added = engine.addMemberToArmada(friend)
            if (added) {
              armadaStatusBanner =
                "✓ ${friend.callsign} JOINED ARMADA [${engine.activeArmadaName}] (${engine.getCurrentArmadaSize()}/5)!"
            }
            friendsRefreshKey++
          },
          onRequestJoinFriendArmada = { friend ->
            val targetArmada = friend.currentArmadaName.ifEmpty { "${friend.syndicateTag}_FLEET" }
            engine.formOrJoinArmada(targetArmada, listOf(friend))
            armadaStatusBanner =
              "✓ INVITE REQUEST ACCEPTED BY ${friend.callsign}! DEPLOYED IN ARMADA [$targetArmada] (${engine.getCurrentArmadaSize()}/5)."
            friendsRefreshKey++
          },
          onRemoveArmadaMember = { callsignToDrop ->
            engine.removeMemberFromArmada(callsignToDrop)
            armadaStatusBanner =
              "⚡ ROSTER SHIFTED -> NOW IN ${engine.getCurrentArmadaTierLabel()} TIER."
            friendsRefreshKey++
          },
          onSimulateRosterShift4To2 = {
            engine.simulateArmadaDropoutToDuo()
            armadaStatusBanner =
              "⚡ ROSTER SHIFT DETECTED: 4-MAN ARMADA REDUCED TO 2-MAN DUO TIER!"
            friendsRefreshKey++
          },
          onLeaveArmadaToSolo = {
            engine.leaveArmadaToSolo()
            armadaStatusBanner = "⚡ LEFT ARMADA -> STATUS: OPERATING INDEPENDENTLY."
            friendsRefreshKey++
          },
          onAddFriendByCallsign = { rawCallsign ->
            val added = localPrefs.addFriendByCallsign(rawCallsign)
            if (added != null) {
              armadaStatusBanner =
                "✓ ADDED [${added.syndicateTag}] ${added.callsign} TO FRIENDS LIST."
              friendsRefreshKey++
            } else {
              armadaStatusBanner = "⚠️ INVALID OR RESTRICTED CALLSIGN."
            }
          },
          onBlockPilot = { pilotUid, pilotCallsign ->
            localPrefs.blockPilot(pilotUid)
            blockedPilotUids = localPrefs.getBlockedPilotUids()
            armadaStatusBanner = "🚫 BLOCKED PILOT $pilotCallsign."
          },
          onReportPilot = { pilotUid, pilotCallsign ->
            localPrefs.reportPilot(pilotUid, pilotCallsign, "Armada Roster Report")
            armadaStatusBanner = "⚑ REPORT SUBMITTED FOR $pilotCallsign."
          },
          onCloseModal = { activeModal = ActiveModal.NONE },
        )
      }

      ActiveModal.CHAT -> {
        val drawerFriendsList =
          remember(syndicateTag, frameTick, friendsRefreshKey) { localPrefs.getFriendsList() }

        SyndicateChatModal(
          syndicateTag = syndicateTag,
          syndicateName = syndicateName,
          activeArmadaName = engine.activeArmadaName,
          activeArmadaCount = engine.getCurrentArmadaSize(),
          armadaTierLabel = engine.getCurrentArmadaTierLabel(),
          armadaSlots = engine.activeArmadaSlots.toList(),
          friendsList = drawerFriendsList,
          chatMessages = chatMessages,
          activeShipsInSector = engine.ships.toList(),
          localPilotUid = currentUserId,
          blockedPilotUids = blockedPilotUids,
          onBlockPilot = { pilotUid, pilotCallsign ->
            localPrefs.blockPilot(pilotUid)
            blockedPilotUids = localPrefs.getBlockedPilotUids()
            toastMessage = "🚫 BLOCKED $pilotCallsign"
            toastVersion++
          },
          onReportPilot = { pilotUid, pilotCallsign ->
            localPrefs.reportPilot(pilotUid, pilotCallsign, "Syndicate Comms Report")
            toastMessage = "⚑ REPORT SUBMITTED FOR $pilotCallsign"
            toastVersion++
          },
          onSendChatMessage = { text ->
            val safeText = com.example.data.UgcModeration.sanitizeUgcText(text)
            val newMsg =
              SyndicateChatMessage(
                id = "msg_${System.currentTimeMillis()}",
                senderUid = currentUserId,
                senderCallsign = callsign,
                senderSyndicateTag = syndicateTag,
                timestampLabel = "JUST NOW",
                messageText = safeText,
              )
            chatMessages.add(newMsg)
            viewModel.sendSectorComm(newMsg)
          },
          onPostArmadaRecruitBanner = {
            if (engine.activeArmadaName.isEmpty()) {
              val prefix = syndicateTag.ifBlank { callsign.take(6) }
              engine.formOrJoinArmada("${prefix}_5", emptyList())
            }
            val armName = engine.activeArmadaName
            val curSize = engine.getCurrentArmadaSize()
            val bannerMsg =
              SyndicateChatMessage(
                id = "banner_${System.currentTimeMillis()}",
                senderUid = currentUserId,
                senderCallsign = callsign,
                senderSyndicateTag = syndicateTag,
                timestampLabel = "LIVE RECRUIT",
                messageText = "",
                isArmadaRecruitBanner = true,
                armadaName = armName,
                armadaCurrentSize = curSize,
                armadaMaxSize = 5,
                armadaObjective = "Forming up to 5-Man Armada in Sector — Tap [⇥ REQUEST INVITE]!",
              )
            chatMessages.add(bannerMsg)
            viewModel.sendSectorComm(bannerMsg)
            toastMessage = "📢 POSTED [ARMADA RECRUIT: UP TO 5-MAN] FOR [$armName]"
            toastVersion++
          },
          onRequestInviteToArmada = { targetArmadaName, leaderCallsign ->
            val wingman =
              FriendPilotEntry(
                uid = "leader_$leaderCallsign",
                callsign = leaderCallsign,
                syndicateTag = syndicateTag,
                shipId = 1,
                statusText = "Armada Commander",
                isOnline = true,
                currentArmadaName = targetArmadaName,
                currentArmadaCount = 3,
                bounty = 280,
              )
            engine.formOrJoinArmada(targetArmadaName, listOf(wingman))
            for (idx in chatMessages.indices) {
              val item = chatMessages[idx]
              if (item.isArmadaRecruitBanner && item.armadaName == targetArmadaName) {
                chatMessages[idx] =
                  item.copy(
                    armadaCurrentSize = engine.getCurrentArmadaSize(),
                    requestSent = true,
                  )
              }
            }
            toastMessage =
              "✓ REQUEST INVITE ACCEPTED BY $leaderCallsign! JOINED ARMADA [$targetArmadaName]"
            toastVersion++
          },
          onInviteFriendToArmada = { friend ->
            val added = engine.addMemberToArmada(friend)
            if (added) {
              toastMessage =
                "✓ ${friend.callsign} JOINED ARMADA [${engine.activeArmadaName}] (${engine.getCurrentArmadaSize()}/5)!"
              toastVersion++
            }
            friendsRefreshKey++
          },
          onRequestJoinFriendArmada = { friend ->
            val targetArmada = friend.currentArmadaName.ifEmpty { "${friend.syndicateTag}_FLEET" }
            engine.formOrJoinArmada(targetArmada, listOf(friend))
            toastMessage =
              "✓ JOINED [$targetArmada] (${engine.getCurrentArmadaSize()}/5)!"
            toastVersion++
            friendsRefreshKey++
          },
          onSimulateRosterShift4To2 = {
            engine.simulateArmadaDropoutToDuo()
            toastMessage = "⚡ ROSTER SHIFT: 4-MAN ARMADA REDUCED TO 2-MAN DUO TIER!"
            toastVersion++
            friendsRefreshKey++
          },
          onLeaveArmadaToSolo = {
            engine.leaveArmadaToSolo()
            toastMessage = "⚡ LEFT ARMADA -> FLYING [SOLO] IN THE FFA RIFT."
            toastVersion++
            friendsRefreshKey++
          },
          onCloseModal = { activeModal = ActiveModal.NONE },
        )
      }

      ActiveModal.DOSSIER -> {
        val cloudLeaderboard =
          (leaderboardState as? UiState.Success)?.data ?: emptyList()
        val (curLvlXp, nextLvlSpan, lvlFrac) =
          remember(frameTick) { engine.getXpProgressInCurrentLevel() }
        val presencePct =
          remember(frameTick) { (engine.getMatchPresenceRatio() * 100f).toInt().coerceIn(0, 100) }

        DossierModal(
          permanentUid = currentUserId,
          callsign = callsign,
          syndicateTag = syndicateTag,
          activeArmadaName = engine.activeArmadaName,
          activeArmadaSize = engine.getCurrentArmadaSize(),
          shipId = selectedShipId,
          pvpKills = engine.pvpKills,
          pvpDeaths = engine.pvpDeaths,
          pvpBountyPeak = engine.pvpBountyPeak,
          allTimeKills = engine.allTimeKills,
          allTimeDeaths = engine.allTimeDeaths,
          allTimeBountyPeak = engine.allTimeBountyPeak,
          convergencesWon = engine.convergencesWon,
          pilotLevel = engine.pilotLevel,
          currentLevelXp = curLvlXp,
          nextLevelXpSpan = nextLvlSpan,
          levelProgressFraction = lvlFrac,
          customizationPointsTotal = engine.getCustomizationPointsTotal(),
          spentCustomizationPoints = engine.spentCustomizationPoints,
          riftShards = engine.riftShards,
          xpFromBuffs = engine.xpFromBuffs.toLong(),
          xpFromDamage = engine.xpFromDamage.toLong(),
          xpFromAccuracy = engine.xpFromAccuracy.toLong(),
          xpFromBountyEarned = engine.xpFromBountyEarned.toLong(),
          xpFromBountyKilled = engine.xpFromBountyKilled.toLong(),
          matchPresencePct = presencePct,
          fps = currentFps,
          cloudLeaderboardEntries = cloudLeaderboard,
          allTimePeakSnapshot = allTimePeakSnapshot,
          pvpPeakSnapshot = pvpPeakSnapshot,
          currentLifeSnapshot =
            remember(frameTick) {
              if (engine.localPlayer.respawnTimer <= 0f) engine.currentLifeStats.toSnapshot() else null
            },
          syndicateServiceHistory = syndicateHistory,
          onOpenRiftShardsStore = {
            previousModalBeforeStore = ActiveModal.DOSSIER
            activeModal = ActiveModal.RIFT_SHARDS_STORE
          },
          onResetLocalPilotData = {
            localPrefs.resetAllLocalPilotData()
            callsign = localPrefs.getCallsign()
            syndicateTag = localPrefs.getSyndicateTag()
            syndicateName = localPrefs.getSyndicateName()
            selectedShipId = localPrefs.getPreferredShipId()
            engine.pvpKills = 0L
            engine.pvpDeaths = 0L
            engine.pvpBountyPeak = 100L
            engine.allTimeKills = 0L
            engine.allTimeDeaths = 0L
            engine.allTimeBountyPeak = 100L
            engine.convergencesWon = 0L
            engine.hvtEliminations = 0L
            engine.greensCollected = 0L
            engine.xpFromBuffs = 0f
            engine.xpFromDamage = 0f
            engine.xpFromAccuracy = 0f
            engine.xpFromBountyEarned = 0f
            engine.xpFromBountyKilled = 0f
            engine.pilotLevel = 1
            engine.riftShards = 0L
            engine.highestMilestoneLevelClaimed = 0
            engine.spentCustomizationPoints = 0
            engine.unlockedExtraSlots = 0
            engine.aiShipDesignTokens = 0
            engine.localPlayer.callsign = callsign
            engine.localPlayer.syndicateTag = syndicateTag
            engine.localPlayer.bounty = 100
            engine.leaveArmadaToSolo()
            engine.switchLocalPlayerShip(1, forceFromHangar = true)
            engine.resetCurrentLifeStats()
            allTimePeakSnapshot = null
            pvpPeakSnapshot = null
            syndicateHistory = localPrefs.getSyndicateServiceHistory(currentUserId)
            friendsRefreshKey++
            engine.onStatsUpdated()
            toastMessage = "↺ PILOT DATA RESET TO LEVEL 1 CLEAN SLATE"
            toastVersion++
          },
          onCloseModal = { activeModal = ActiveModal.NONE },
        )
      }

      ActiveModal.RIFT_SHARDS_STORE -> {
        val (curLvlXp, nextLvlSpan, lvlFrac) =
          remember(frameTick) { engine.getXpProgressInCurrentLevel() }
        val presencePct =
          remember(frameTick) { (engine.getMatchPresenceRatio() * 100f).toInt().coerceIn(0, 100) }

        RiftShardsStoreModal(
          pilotLevel = engine.pilotLevel,
          currentLevelXp = curLvlXp,
          nextLevelXpSpan = nextLvlSpan,
          levelProgressFraction = lvlFrac,
          totalCustomizationPoints = engine.getCustomizationPointsTotal(),
          spentCustomizationPoints = engine.spentCustomizationPoints,
          riftShards = engine.riftShards,
          publicGameWins = engine.convergencesWon,
          matchPresencePct = presencePct,
          unlockedExtraSlots = engine.unlockedExtraSlots,
          aiShipDesignTokens = engine.aiShipDesignTokens,
          onTopUpRiftShards = { amount ->
            engine.riftShards += amount
            sfx.playPrize()
            engine.onStatsUpdated()
          },
          onRespecCustomizationPoints = {
            if (engine.riftShards >= 50L && engine.spentCustomizationPoints > 0) {
              engine.riftShards -= 50L
              engine.spentCustomizationPoints = 0
              sfx.playPrize()
              engine.onStatsUpdated()
              true
            } else {
              false
            }
          },
          onAllocateCustomizationPoint = {
            if (engine.getCustomizationPointsAvailable() > 0) {
              engine.spentCustomizationPoints++
              sfx.playPrize()
              engine.onStatsUpdated()
              true
            } else {
              false
            }
          },
          onUnlockExtraSlot = {
            if (engine.riftShards >= 300L) {
              engine.riftShards -= 300L
              engine.unlockedExtraSlots++
              if (!slot9BUnlocked) {
                localPrefs.setCustomSlotUnlocked("Frame B", true, currentUserId)
                slot9BUnlocked = true
              } else if (!slot9CUnlocked) {
                localPrefs.setCustomSlotUnlocked("Frame C", true, currentUserId)
                slot9CUnlocked = true
              }
              sfx.playPrize()
              engine.onStatsUpdated()
              true
            } else {
              false
            }
          },
          onPurchaseAiDesignToken = {
            if (engine.riftShards >= 150L) {
              engine.riftShards -= 150L
              engine.aiShipDesignTokens++
              sfx.playPrize()
              engine.onStatsUpdated()
              true
            } else {
              false
            }
          },
          onSimulateFfaWinPayout = {
            engine.concludePublicFfaMatchCycle(forceSimulateWinWithPresence = true)
          },
          onSimulatePilotLevelUp = {
            engine.simulatePilotLevelUp()
          },
          onCloseModal = { activeModal = previousModalBeforeStore },
        )
      }

      ActiveModal.SETTINGS -> {
        SettingsModal(
          fps = currentFps,
          sfxEnabled = sfxEnabled,
          cameraZoom = cameraZoom,
          playfieldTextScale = playfieldTextScale,
          isGoogleAuthenticated = isGoogleAuthenticated,
          onToggleSfx = {
            sfxEnabled = !sfxEnabled
            sfx.enabled = sfxEnabled
          },
          onChangeCameraZoom = { newZoom ->
            val clamped = newZoom.coerceIn(0.5f, 1.5f)
            cameraZoom = clamped
            engine.cameraZoom = clamped
            localPrefs.setCameraZoom(clamped)
          },
          onChangePlayfieldTextScale = { newScale ->
            val clamped = newScale.coerceIn(0.75f, 2.0f)
            playfieldTextScale = clamped
            engine.playfieldTextScale = clamped
            localPrefs.setPlayfieldTextScale(clamped)
            frameTick++
          },
          onRequestGoogleSignIn = {
            onGoogleSignInClicked(
              context = context,
              credentialManager = credentialManager,
              onAuthSuccess = {
                toastMessage = "☁️ GOOGLE CLOUD ACCOUNT LINKED!"
                toastVersion++
              },
              onAuthError = { msg ->
                toastMessage = "⚠️ SIGN-IN: $msg"
                toastVersion++
              },
              scope = scope,
            )
          },
          onRequestSignOut = {
            localPrefs.setGuestBypassEnabled(true)
            signOut(credentialManager = credentialManager, scope = scope)
          },
          onCloseModal = { activeModal = ActiveModal.NONE },
          onOpenChangelog = onOpenChangelog,
        )
      }

      ActiveModal.NEXUS -> {
        val cloudLeaderboard =
          (leaderboardState as? UiState.Success)?.data ?: emptyList()
        val topCloudEntry = cloudLeaderboard.maxByOrNull { it.allTimeBounty }
        val topLiveShip = engine.ships.maxByOrNull { it.bounty }
        val highestBountySummary =
          remember(cloudLeaderboard, engine.allTimeBountyPeak, frameTick) {
            val cloudMax = topCloudEntry?.allTimeBounty ?: 0L
            val localMax =
              maxOf(engine.allTimeBountyPeak, allTimePeakSnapshot?.bountyReached?.toLong() ?: 0L)
            val liveMax = (topLiveShip?.bounty ?: 0).toLong()
            when {
              cloudMax >= localMax && cloudMax >= liveMax && topCloudEntry != null ->
                "[${RiftGameEngine.cleanTag(topCloudEntry.syndicateTag)}] ${topCloudEntry.callsign} (${topCloudEntry.allTimeBounty})"
              localMax >= liveMax && localMax > 0L ->
                "[$syndicateTag] $callsign ($localMax)"
              topLiveShip != null ->
                "[${topLiveShip.syndicateTag}] ${topLiveShip.callsign} (${topLiveShip.bounty})"
              else -> "[$syndicateTag] $callsign (${engine.localPlayer.bounty})"
            }
          }

        val activeShipTitle =
          remember(engine.localPlayer.shipId, activeCustomSlot, frameTick) {
            engine.formatShipLabel(engine.localPlayer)
          }

        val buildLiveSnapshot: () -> String = {
          RiftNexusKnowledge.compileLiveSnapshot(
            engine = engine,
            localPrefs = localPrefs,
            currentUserId = currentUserId,
            callsign = callsign,
            syndicateTag = syndicateTag,
            syndicateName = syndicateName,
            cloudLeaderboard = cloudLeaderboard,
            allTimePeakSnapshot = localPrefs.getAllTimePeakBountySnapshot(currentUserId) ?: allTimePeakSnapshot,
            pvpPeakSnapshot = localPrefs.getPvpPeakBountySnapshot(currentUserId) ?: pvpPeakSnapshot,
            syndicateHistory = localPrefs.getSyndicateServiceHistory(currentUserId).ifEmpty { syndicateHistory },
          )
        }

        val bestShipId =
          remember(currentUserId, selectedShipId, allTimePeakSnapshot, pvpPeakSnapshot, frameTick) {
            localPrefs.getBestShipId(currentUserId, selectedShipId)
          }
        val canAccessSyndicate =
          remember(syndicateTag, callsign, syndicateChannelsRefreshKey) {
            localPrefs.canAccessSyndicateChannels(syndicateTag, callsign, syndicateTag)
          }
        val customSyndicateChannels =
          remember(syndicateTag, syndicateChannelsRefreshKey) {
            localPrefs.getSyndicateCustomChannels(syndicateTag)
          }
        val invitedSyndicateGuests =
          remember(syndicateTag, syndicateChannelsRefreshKey) {
            localPrefs.getSyndicateInvitedGuests(syndicateTag)
          }
        val syndicateWarRecord =
          remember(syndicateTag, syndicateChannelsRefreshKey, frameTick) {
            localPrefs.getSyndicateWarRecord(syndicateTag)
          }
        val syndicateFriends =
          remember(syndicateTag, frameTick, friendsRefreshKey) {
            localPrefs.getFriendsList()
          }
        val humanPilotsOnline =
          remember(frameTick) { engine.getHumanPilotCountForMode(engine.activeRankedMode) }
        val syndicateOnlineCount =
          remember(syndicateTag, frameTick, friendsRefreshKey) {
            val cleanSyn = RiftGameEngine.cleanTag(syndicateTag)
            if (cleanSyn == "GUEST" || cleanSyn == "FREE" || cleanSyn.isEmpty()) {
              1
            } else {
              val inArena =
                engine.ships.count {
                  it.isHuman && RiftGameEngine.cleanTag(it.syndicateTag) == cleanSyn
                }
              val onlineFriends =
                syndicateFriends.count {
                  it.isOnline && RiftGameEngine.cleanTag(it.syndicateTag) == cleanSyn
                }
              maxOf(1, inArena + onlineFriends)
            }
          }
        val presetArmadasReadyCount =
          remember(frameTick, friendsRefreshKey) {
            val activeArmadaCount = if (engine.activeArmadaName.isNotBlank()) 1 else 0
            val friendArmadas =
              syndicateFriends
                .filter { it.isOnline && it.currentArmadaName.isNotBlank() }
                .map { it.currentArmadaName }
                .distinct()
                .size
            activeArmadaCount + friendArmadas
          }

        NexusAiConsoleModal(
          messages = nexusMessages,
          streamState = nexusStreamState,
          lastFailedQuestion = nexusViewModel.lastFailedQuestion,
          activeShipSummary = activeShipTitle,
          pilotLevel = engine.pilotLevel,
          pointBudget = engine.getCustomizationPointsTotal(),
          highestBountySummary = highestBountySummary,
          onSendQuestion = { query ->
            nexusViewModel.askNexus(
              rawQuestion = query,
              liveTelemetrySnapshot = buildLiveSnapshot(),
            )
          },
          onRetryLastQuestion = {
            nexusViewModel.retryLastQuestion(
              liveTelemetrySnapshot = buildLiveSnapshot(),
            )
          },
          onClearHistory = {
            nexusViewModel.clearConversation()
          },
          onCloseModal = {
            val target = previousModalBeforeNexus
            previousModalBeforeNexus = ActiveModal.NONE
            activeModal = target
          },
          callsign = callsign,
          syndicateTag = syndicateTag,
          syndicateName = syndicateName,
          activeShipId = selectedShipId,
          bestShipId = bestShipId,
          bootToNexusHub = bootToNexusHub,
          onToggleBootToNexusHub = { enabled ->
            bootToNexusHub = enabled
            localPrefs.setBootToNexusHub(enabled)
            showStartupChangelogQuickJoin = !enabled
            toastMessage =
              if (enabled) {
                "⚙️ STARTUP PREFERENCE: OPEN NEXUS COMMAND HUB ON LOGIN"
              } else {
                "⚡ STARTUP PREFERENCE: FAST-BOOT TO ARENA + QUICK-JOIN BAR"
              }
            toastVersion++
          },
          totalOnlinePilots = humanPilotsOnline,
          syndicateOnlineCount = syndicateOnlineCount,
          presetArmadasReadyCount = presetArmadasReadyCount,
          activeArmadaName = engine.activeArmadaName,
          armadaSlots = engine.activeArmadaSlots.toList(),
          onResolvePilotBestShipId = { uid, fallbackShip ->
            if (uid == currentUserId || uid == "local_self") {
              localPrefs.getBestShipId(currentUserId, fallbackShip)
            } else {
              localPrefs.getBestShipId(uid, fallbackShip)
            }
          },
          activeRankedMode = engine.activeRankedMode,
          activeTeamSizeBracket = engine.activeTeamSizeBracket,
          humanPilotCountByMode = { mode -> engine.getHumanPilotCountForMode(mode) },
          canQueueSyndicateWars = engine.canQueueSyndicateWars(),
          syndicateWarRecord = syndicateWarRecord,
          onDeployToRankedMode = { mode, bracket ->
            engine.deployToRankedMode(mode, bracket)
            selectedMode = mode.id
            showStartupChangelogQuickJoin = false
            activeModal = ActiveModal.NONE
          },
          onOpenArmadaModal = {
            armadaStatusBanner = null
            activeModal = ActiveModal.ARMADA
          },
          onQuickFormSyndicateArmada = {
            val synMates =
              syndicateFriends
                .filter {
                  it.isOnline &&
                    RiftGameEngine.cleanTag(it.syndicateTag) ==
                      RiftGameEngine.cleanTag(syndicateTag)
                }
                .take(4)
            val prefix =
              RiftGameEngine.cleanTag(syndicateTag).let {
                if (it == "GUEST" || it.isEmpty()) callsign.take(6) else it
              }
            val fleetTag = "${prefix}_FLEET"
            engine.formOrJoinArmada(
              fleetTag,
              if (synMates.isNotEmpty()) synMates else syndicateFriends.take(2),
            )
            friendsRefreshKey++
            toastMessage =
              "🛡️ SYNDICATE ARMADA [$fleetTag] STAGED (${engine.getCurrentArmadaSize()}/5 PILOTS)"
            toastVersion++
          },
          onOpenSyndicatesModal = {
            syndicateStatusBanner = null
            activeModal = ActiveModal.SYNDICATES
          },
          onOpenHangarForShip = { targetShipId ->
            val clamped = targetShipId.coerceIn(1, 10)
            selectedShipId = clamped
            localPrefs.setPreferredShipId(clamped)
            engine.switchLocalPlayerShip(clamped, forceFromHangar = true)
            engine.onStatsUpdated()
            toastMessage = "🚀 EQUIPPED HULL #$clamped FOR SECTOR DEPLOYMENT"
            toastVersion++
          },
          canAccessSyndicateServer = canAccessSyndicate,
          syndicateCustomChannels = customSyndicateChannels,
          syndicateInvitedGuests = invitedSyndicateGuests,
          onAddSyndicateChannel = { rawChannel ->
            val currentChannels = localPrefs.getSyndicateCustomChannels(syndicateTag)
            if (currentChannels.size >= 5) {
              toastMessage = "⚠️ MAXIMUM 5 CHANNELS REACHED FOR [$syndicateTag]"
              toastVersion++
            } else {
              localPrefs.setSyndicateCustomChannels(syndicateTag, currentChannels + rawChannel)
              syndicateChannelsRefreshKey++
              toastMessage = "✓ CREATED SYNDICATE CHANNEL IN [$syndicateTag]"
              toastVersion++
            }
          },
          onDeleteSyndicateChannel = { rawChannel ->
            val currentChannels = localPrefs.getSyndicateCustomChannels(syndicateTag)
            val filtered = currentChannels.filterNot { it.equals(rawChannel, ignoreCase = true) }
            localPrefs.setSyndicateCustomChannels(syndicateTag, filtered)
            syndicateChannelsRefreshKey++
            toastMessage = "✕ REMOVED CHANNEL $rawChannel"
            toastVersion++
          },
          onInviteSyndicateGuest = { guestCallsign ->
            localPrefs.inviteGuestToSyndicateChannels(syndicateTag, guestCallsign)
            syndicateChannelsRefreshKey++
            toastMessage = "🎟️ GRANTED [$syndicateTag] SERVER GUEST PASS TO $guestCallsign"
            toastVersion++
          },
          onRevokeSyndicateGuest = { guestCallsign ->
            localPrefs.revokeGuestFromSyndicateChannels(syndicateTag, guestCallsign)
            syndicateChannelsRefreshKey++
            toastMessage = "✕ REVOKED GUEST PASS FOR $guestCallsign"
            toastVersion++
          },
          lobbyMessages = chatMessages,
          onSendChannelMessage = { channelKey, messageText ->
            val safeText = com.example.data.UgcModeration.sanitizeUgcText(messageText)
            val newMsg =
              SyndicateChatMessage(
                id = "msg_${System.currentTimeMillis()}",
                senderUid = currentUserId,
                senderCallsign = callsign,
                senderSyndicateTag = syndicateTag,
                timestampLabel = channelKey.uppercase(Locale.US),
                messageText = safeText,
              )
            chatMessages.add(newMsg)
            viewModel.sendSectorComm(newMsg)
          },
          onSubmitBugReport = { category, description ->
            val safeDesc = com.example.data.UgcModeration.sanitizeUgcText(description)
            localPrefs.reportPilot(
              uid = "BUG_${category.take(10)}",
              callsign = category,
              reason = safeDesc,
            )
            toastMessage = "🛠️ BUG REPORT LOGGED TO SECTOR TELEMETRY [$category]"
            toastVersion++
          },
        )
      }

      ActiveModal.NONE -> {
        if (showStartupChangelogQuickJoin) {
          StartupChangelogQuickJoinBar(
            activeMode = engine.activeRankedMode,
            onQuickJoinMode = { mode ->
              val defaultBracket =
                when (mode) {
                  RankedGameMode.RIFT_SKIRMISH -> maxOf(2, engine.getCurrentArmadaSize())
                  RankedGameMode.SYNDICATE_WARS -> 3
                  else -> engine.getCurrentArmadaSize()
                }
              engine.deployToRankedMode(mode, defaultBracket)
              selectedMode = mode.id
              showStartupChangelogQuickJoin = false
            },
            onOpenNexusHub = {
              showStartupChangelogQuickJoin = false
              previousModalBeforeNexus = ActiveModal.NONE
              activeModal = ActiveModal.NEXUS
            },
            onOpenFullChangelog = onOpenChangelog,
            onDismiss = { showStartupChangelogQuickJoin = false },
          )
        }
      }
    }
  }
}

@Composable
private fun StartupChangelogQuickJoinBar(
  activeMode: RankedGameMode,
  onQuickJoinMode: (RankedGameMode) -> Unit,
  onOpenNexusHub: () -> Unit,
  onOpenFullChangelog: () -> Unit,
  onDismiss: () -> Unit,
) {
  Box(
    modifier =
      Modifier.fillMaxSize()
        .padding(top = 82.dp, start = 16.dp, end = 16.dp),
    contentAlignment = Alignment.TopCenter,
  ) {
    Column(
      modifier =
        Modifier.widthIn(max = 820.dp)
          .fillMaxWidth()
          .clip(RoundedCornerShape(14.dp))
          .background(Color(0xEB050B16))
          .border(1.2.dp, Color(0xFF00F5D4), RoundedCornerShape(14.dp))
          .padding(horizontal = 14.dp, vertical = 10.dp)
          .testTag("startup_changelog_quick_join_bar"),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = "⚡ FAST-BOOT ARENA // BUILD v39.0 NEXUS COMMAND HUB & 5 RANKED MODES",
            color = Color(0xFF00F5D4),
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
          )
          Box(
            modifier =
              Modifier.clip(RoundedCornerShape(6.dp))
                .background(Color(0x2238BDF8))
                .border(0.8.dp, Color(0x8838BDF8), RoundedCornerShape(6.dp))
                .clickable { onOpenFullChangelog() }
                .padding(horizontal = 7.dp, vertical = 3.dp)
                .testTag("startup_quick_join_changelog_btn"),
          ) {
            Text(
              text = "📜 PATCH NOTES",
              color = Color(0xFF38BDF8),
              fontSize = 8.5.sp,
              fontWeight = FontWeight.ExtraBold,
              fontFamily = FontFamily.Monospace,
            )
          }
        }
        Row(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Box(
            modifier =
              Modifier.clip(RoundedCornerShape(7.dp))
                .background(Color(0x2800F5D4))
                .border(1.dp, Color(0xFF00F5D4), RoundedCornerShape(7.dp))
                .clickable { onOpenNexusHub() }
                .padding(horizontal = 9.dp, vertical = 4.dp)
                .testTag("startup_quick_join_open_nexus"),
          ) {
            Text(
              text = "🌐 OPEN NEXUS HUB",
              color = Color(0xFF00F5D4),
              fontSize = 9.sp,
              fontWeight = FontWeight.Black,
              fontFamily = FontFamily.Monospace,
            )
          }
          Box(
            modifier =
              Modifier.clip(RoundedCornerShape(7.dp))
                .background(Color(0x33EF4444))
                .border(0.9.dp, Color(0xAAEF4444), RoundedCornerShape(7.dp))
                .clickable { onDismiss() }
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .testTag("startup_quick_join_dismiss"),
          ) {
            Text(
              text = "✕ DISMISS",
              color = Color(0xFFFCA5A5),
              fontSize = 9.sp,
              fontWeight = FontWeight.Black,
              fontFamily = FontFamily.Monospace,
            )
          }
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        QuickJoinModeChip(
          label = "⚡ QUICK-JOIN SECTOR CONVERGENCE",
          selected = activeMode == RankedGameMode.SECTOR_CONVERGENCE,
          accent = Color(0xFF38BDF8),
          tag = "quick_join_sector_convergence",
          onClick = { onQuickJoinMode(RankedGameMode.SECTOR_CONVERGENCE) },
        )
        QuickJoinModeChip(
          label = "⚔️ QUICK-JOIN RIFT SKIRMISH",
          selected = activeMode == RankedGameMode.RIFT_SKIRMISH,
          accent = Color(0xFFF59E0B),
          tag = "quick_join_rift_skirmish",
          onClick = { onQuickJoinMode(RankedGameMode.RIFT_SKIRMISH) },
        )
        QuickJoinModeChip(
          label = "⚡ QUICK-JOIN FLUX CORE",
          selected = activeMode == RankedGameMode.FLUX_CORE,
          accent = Color(0xFF00F5D4),
          tag = "quick_join_flux_core",
          onClick = { onQuickJoinMode(RankedGameMode.FLUX_CORE) },
        )
        QuickJoinModeChip(
          label = "🔒 QUICK-JOIN STASIS BREACH",
          selected = activeMode == RankedGameMode.STASIS_BREACH,
          accent = Color(0xFFA855F7),
          tag = "quick_join_stasis_breach",
          onClick = { onQuickJoinMode(RankedGameMode.STASIS_BREACH) },
        )
      }
    }
  }
}

@Composable
private fun QuickJoinModeChip(
  label: String,
  selected: Boolean,
  accent: Color,
  tag: String,
  onClick: () -> Unit,
) {
  Box(
    modifier =
      Modifier.clip(RoundedCornerShape(8.dp))
        .background(if (selected) accent.copy(alpha = 0.24f) else Color(0xCC0D1829))
        .border(if (selected) 1.2.dp else 0.9.dp, accent, RoundedCornerShape(8.dp))
        .clickable { onClick() }
        .padding(horizontal = 10.dp, vertical = 6.dp)
        .testTag(tag),
  ) {
    Text(
      text = label,
      color = if (selected) Color.White else accent,
      fontSize = 9.sp,
      fontWeight = FontWeight.Black,
      fontFamily = FontFamily.Monospace,
    )
  }
}
