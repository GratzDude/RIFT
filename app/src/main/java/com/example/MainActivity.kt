package com.example

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
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
import com.example.engine.RiftArenaView
import com.example.engine.RiftGameEngine
import com.example.engine.RiftSoundSynthesizer
import com.example.ui.ArmadaFleetModal
import com.example.ui.AuthScreen
import com.example.ui.DossierModal
import com.example.ui.CodexModal
import com.example.ui.HangarModal
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
}

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
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
  val user = currentUser

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
      )
    }
  }
}

@Composable
fun RiftArenaScreen(
  currentUserId: String,
  initialCallsign: String,
  isGoogleAuthenticated: Boolean,
  localPrefs: LocalPilotPrefs = LocalPilotPrefs(LocalContext.current),
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

  val sfx = remember { RiftSoundSynthesizer() }
  var sfxEnabled by remember { mutableStateOf(true) }
  var currentFps by remember { mutableIntStateOf(60) }
  var frameTick by remember { mutableIntStateOf(0) }
  var toastMessage by remember {
    mutableStateOf<String?>("⚡ RIFT FFA ARENA ONLINE // 360° AIM & NAMED ARMADAS ACTIVE")
  }
  var toastVersion by remember { mutableIntStateOf(0) }
  var unreadCommsCount by remember { mutableIntStateOf(2) }

  var activeModal by remember { mutableStateOf(ActiveModal.NONE) }
  var previousModalBeforeStore by remember { mutableStateOf(ActiveModal.DOSSIER) }
  var callsign by remember { mutableStateOf(localPrefs.getCallsign()) }
  var syndicateTag by remember { mutableStateOf(localPrefs.getSyndicateTag()) }
  var syndicateName by remember { mutableStateOf(localPrefs.getSyndicateName()) }
  var selectedShipId by remember { mutableIntStateOf(localPrefs.getPreferredShipId()) }
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
    mutableStateOf(localPrefs.isCustomSlotUnlocked("9B", currentUserId))
  }
  var slot9CUnlocked by remember(currentUserId) {
    mutableStateOf(localPrefs.isCustomSlotUnlocked("9C", currentUserId))
  }
  var cameraZoom by remember { mutableFloatStateOf(localPrefs.getCameraZoom()) }
  var playfieldTextScale by remember { mutableFloatStateOf(localPrefs.getPlayfieldTextScale()) }
  var syndicateStatusBanner by remember { mutableStateOf<String?>(null) }
  var armadaStatusBanner by remember { mutableStateOf<String?>(null) }
  var friendsRefreshKey by remember { mutableIntStateOf(0) }

  // Syndicate Chat Messages State (including interactive [ARMADA RECRUIT: UP TO 5-MAN] banners)
  val chatMessages = remember {
    mutableStateListOf(
      SyndicateChatMessage(
        id = "msg_1",
        senderUid = "friend_vortex_ace",
        senderCallsign = "VORTEX_ACE",
        senderSyndicateTag = syndicateTag,
        timestampLabel = "NOW",
        messageText = "Center trench is hot! Forming up a 5-man strike Armada for HVT hunting.",
      ),
      SyndicateChatMessage(
        id = "msg_banner_apex",
        senderUid = "friend_viper_ace",
        senderCallsign = "VIPER_ACE",
        senderSyndicateTag = syndicateTag,
        timestampLabel = "LIVE RECRUIT",
        messageText = "",
        isArmadaRecruitBanner = true,
        armadaName = "APEX_4",
        armadaCurrentSize = 3,
        armadaMaxSize = 5,
        armadaObjective = "Center Trench Control & HVT Bounty Sweep (Friendly Fire OFF)",
      ),
      SyndicateChatMessage(
        id = "msg_banner_gold",
        senderUid = "friend_solaris_prime",
        senderCallsign = "SOLARIS_PRIME",
        senderSyndicateTag = "SOLR",
        timestampLabel = "LIVE RECRUIT",
        messageText = "",
        isArmadaRecruitBanner = true,
        armadaName = "GOLD_WING",
        armadaCurrentSize = 2,
        armadaMaxSize = 5,
        armadaObjective = "Heavy Goliath & Ricochet Siege Wing — Open Slots!",
      ),
    )
  }

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
      )
      frameTick++
    }
    onDispose {
      sfx.release()
    }
  }

  // Load persisted HUD layout offsets on startup
  LaunchedEffect(Unit) {
    com.example.engine.RiftCalibrationState.loadPersistedLayout(context)
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

  // Periodic live telemetry heartbeat when authenticated
  LaunchedEffect(isGoogleAuthenticated, currentUserId) {
    if (!isGoogleAuthenticated) return@LaunchedEffect
    while (true) {
      delay(2000L)
      val p = engine.localPlayer
      viewModel.syncNativeLiveState(
        sectorId = engine.mode,
        callsign = p.callsign,
        shipId = p.shipId,
        freq = p.freq,
        x = p.x,
        y = p.y,
        vx = p.vx,
        vy = p.vy,
        angle = p.angle,
        flux = p.flux,
        maxFlux = p.maxFlux,
        bounty = p.bounty,
      )
    }
  }

  BackHandler {
    if (activeModal != ActiveModal.NONE) {
      activeModal = ActiveModal.NONE
    } else {
      activeModal = ActiveModal.HANGAR
    }
  }

  Box(
    modifier =
      Modifier.fillMaxSize()
        .background(Color.Black)
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
        ).apply {
          layoutParams =
            ViewGroup.LayoutParams(
              ViewGroup.LayoutParams.MATCH_PARENT,
              ViewGroup.LayoutParams.MATCH_PARENT,
            )
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
              slot9BUnlocked = localPrefs.isCustomSlotUnlocked("9B", currentUserId)
              slot9CUnlocked = localPrefs.isCustomSlotUnlocked("9C", currentUserId)
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
          onDeployPilot = { newCallsign, newShipId, _, asGuest ->
            if (asGuest) {
              localPrefs.setGuestBypassEnabled(true)
            }
            callsign = newCallsign
            selectedShipId = newShipId
            selectedMode = "FFA"
            syndicateTag = localPrefs.getSyndicateTag()
            syndicateName = localPrefs.getSyndicateName()

            localPrefs.setCallsign(newCallsign)
            localPrefs.setPreferredShipId(newShipId)
            localPrefs.setPreferredMode("FFA")

            engine.localPlayer.callsign = newCallsign
            engine.localPlayer.syndicateTag = syndicateTag
            engine.syncLocalArmadaSelfSlot()
            engine.switchLocalPlayerShip(newShipId, forceFromHangar = true)
            engine.onStatsUpdated()
            activeModal = ActiveModal.NONE
          },
          onCloseModal = { activeModal = ActiveModal.NONE },
        )
      }

      ActiveModal.CODEX -> {
        CodexModal(
          onCloseModal = { activeModal = ActiveModal.NONE },
        )
      }

      ActiveModal.SYNDICATES -> {
        val directoryList = remember(syndicateTag, frameTick) { localPrefs.getAllSyndicates() }
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
          remember(frameTick) {
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
                description = "Break free from [GUEST] and pledge allegiance to an active Syndicate.",
                unlocked =
                  syndicateTag.isNotEmpty() &&
                    syndicateTag != "GUEST" &&
                    syndicateTag != "FREE",
                progressText = "[$syndicateTag] ACTIVE",
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
              engine.localPlayer.syndicateTag = syndicateTag
              engine.syncLocalArmadaSelfSlot()
              syndicateStatusBanner =
                "⚡ LEFT SYNDICATE -> ASSIGNED TO [GUEST] PLACEHOLDER. 15-DAY LOCK COOLDOWN TRIGGERED."
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
              engine.localPlayer.syndicateTag = syndicateTag
              engine.syncLocalArmadaSelfSlot()
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
          statusBannerMessage = armadaStatusBanner,
          onCreateOrRenameArmada = { newArmadaName ->
            engine.formOrJoinArmada(
              newArmadaName,
              if (engine.activeArmadaSlots.size <= 1) friendsList.take(1) else emptyList(),
            )
            armadaStatusBanner =
              "🛡️ ARMADA [${engine.activeArmadaName}] DEPLOYED (${engine.getCurrentArmadaSize()}/5 PILOTS)"
            friendsRefreshKey++
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
            }
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
          onSendChatMessage = { text ->
            chatMessages.add(
              SyndicateChatMessage(
                id = "msg_${System.currentTimeMillis()}",
                senderUid = currentUserId,
                senderCallsign = callsign,
                senderSyndicateTag = syndicateTag,
                timestampLabel = "JUST NOW",
                messageText = text,
              )
            )
          },
          onPostArmadaRecruitBanner = {
            if (engine.activeArmadaName.isEmpty()) {
              engine.formOrJoinArmada("${syndicateTag}_5", emptyList())
            }
            val armName = engine.activeArmadaName
            val curSize = engine.getCurrentArmadaSize()
            chatMessages.add(
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
            )
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
          onOpenRiftShardsStore = {
            previousModalBeforeStore = ActiveModal.DOSSIER
            activeModal = ActiveModal.RIFT_SHARDS_STORE
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
                localPrefs.setCustomSlotUnlocked("9B", true, currentUserId)
                slot9BUnlocked = true
              } else if (!slot9CUnlocked) {
                localPrefs.setCustomSlotUnlocked("9C", true, currentUserId)
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
        )
      }

      ActiveModal.NONE -> Unit
    }
  }
}
