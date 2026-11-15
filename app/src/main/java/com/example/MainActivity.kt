package com.example

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.KeyEvent
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.LocalPilotPrefs
import com.example.data.PilotAchievement
import com.example.data.RiftRepository
import com.example.data.SyndicateChatMessage
import com.example.engine.MusicSynthArchetype
import com.example.engine.RiftArenaView
import com.example.engine.RiftCalibrationState
import com.example.engine.RiftCodex
import com.example.engine.RiftGameEngine
import com.example.engine.RiftNexusKnowledge
import com.example.engine.RiftProgressionCatalog
import com.example.engine.RiftSoundSynthesizer
import com.example.ui.ArmadaFleetModal
import com.example.ui.AuthScreen
import com.example.ui.CURRENT_RIFT_BUILD_VERSION
import com.example.ui.CodexModal
import com.example.ui.DossierModal
import com.example.ui.HangarModal
import com.example.ui.NexusAiConsoleModal
import com.example.ui.NexusAiViewModel
import com.example.ui.NexusHubLayoutMode
import com.example.ui.NexusStreamState
import com.example.ui.RiftCalibrationDialog
import com.example.ui.RiftChangelogModal
import com.example.ui.RiftHudOverlay
import com.example.ui.RiftShardsStoreModal
import com.example.ui.RiftViewModel
import com.example.ui.SettingsModal
import com.example.ui.SyndicatesModal
import com.example.ui.UiState
import com.example.ui.onGoogleSignInClicked
import com.example.ui.signOut
import com.example.ui.theme.MyApplicationTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
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

/**
 * Global Device-Aware Modal Auto-Scaler:
 * Automatically scales modal window density against a 560dp x 1060dp reference cockpit canvas
 * so compact landscape phones (~411dp height) and high-res tablets render every modal with zero
 * clipped text, zero crushed top pills, and clean multi-column ergonomics.
 */
@Composable
fun DeviceAwareModalAutoScaler(content: @Composable () -> Unit) {
  BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val baseDensity = LocalDensity.current
    val scaleH = (maxHeight.value / 560f).coerceIn(0.66f, 1.15f)
    val scaleW = (maxWidth.value / 1060f).coerceIn(0.70f, 1.15f)
    val scale = minOf(scaleH, scaleW)
    val scaledDensity =
      remember(baseDensity.density, baseDensity.fontScale, scale) {
        Density(
          density = baseDensity.density * scale,
          fontScale = baseDensity.fontScale.coerceAtMost(1.08f),
        )
      }
    CompositionLocalProvider(LocalDensity provides scaledDensity) {
      content()
    }
  }
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
  CALIBRATE,
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
  var showPreAuthChangelog by remember { mutableStateOf(false) }
  val user = currentUser

  Box(modifier = Modifier.fillMaxSize()) {
    if (user == null && !isGuestMode) {
      AuthScreen(
        onAuthSuccess = {
          localPrefs.setBootToNexusHub(true)
        },
        onPlayAsGuest = {
          localPrefs.startFreshDrifterSession()
          localPrefs.setBootToNexusHub(true)
          isGuestMode = true
        },
        onOpenChangelog = { showPreAuthChangelog = true },
      )
      if (showPreAuthChangelog) {
        DeviceAwareModalAutoScaler {
          RiftChangelogModal(
            lastSeenBuild = localPrefs.getLastSeenChangelogBuild(),
            onAcknowledgeAndContinue = {
              localPrefs.setLastSeenChangelogBuild(CURRENT_RIFT_BUILD_VERSION)
              showPreAuthChangelog = false
            },
          )
        }
      }
    } else {
      if (user != null) {
        localPrefs.setGuestBypassEnabled(false)
      }
      val uid = user?.uid ?: permanentDeviceUid
      val initialCallsign =
        if (user != null) localPrefs.getCallsign(user.uid) else localPrefs.getCallsign()

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
      key = "${currentUserId}_$isGoogleAuthenticated",
      factory =
        viewModelFactory {
          initializer {
            val app =
              checkNotNull(this[APPLICATION_KEY]) {
                "APPLICATION_KEY missing from CreationExtras"
              }
            RiftViewModel(
              repository = RiftRepository(app),
              currentUserId = currentUserId,
              initialCallsign = initialCallsign,
              isGoogleAuthenticated = isGoogleAuthenticated,
            )
          }
        },
    )

  val dossierState by viewModel.dossierState.collectAsStateWithLifecycle()
  val leaderboardState by viewModel.leaderboardState.collectAsStateWithLifecycle()
  val sectorPilotsState by viewModel.sectorPilotsState.collectAsStateWithLifecycle()
  val cloudSyndicatesState by viewModel.cloudSyndicatesState.collectAsStateWithLifecycle()
  val sectorCommsState by viewModel.sectorCommsState.collectAsStateWithLifecycle()
  val latestCalibBroadcast by viewModel.latestCalibrationBroadcast.collectAsStateWithLifecycle()

  val nexusViewModel: NexusAiViewModel = viewModel(key = "nexus_$currentUserId")
  val nexusMessages by nexusViewModel.messages.collectAsStateWithLifecycle()
  val nexusStreamState by nexusViewModel.streamState.collectAsStateWithLifecycle()
  val isNexusStreaming =
    nexusStreamState is NexusStreamState.Loading || nexusStreamState is NexusStreamState.Streaming

  val sfx = remember {
    RiftSoundSynthesizer(context.cacheDir).apply {
      enabled = localPrefs.getSfxEnabled()
      setSfxVolumeLive(localPrefs.getSfxVolume())
      setMusicVolumeLive(localPrefs.getMusicVolume())
      val savedBpm = localPrefs.getMusicBpm()
      val savedPrompt = localPrefs.getMusicForgePrompt()
      val savedArchetype = MusicSynthArchetype.fromId(localPrefs.getMusicArchetypeId())
      if (savedPrompt.isNotBlank()) {
        forgeMusicFromPrompt(savedPrompt, savedBpm)
      } else {
        applyMusicArchetype(savedArchetype, overrideBpm = savedBpm)
      }
      setMusicEnabled(localPrefs.getMusicEnabled())
    }
  }
  var sfxEnabled by remember { mutableStateOf(localPrefs.getSfxEnabled()) }
  var musicEnabled by remember { mutableStateOf(localPrefs.getMusicEnabled()) }
  var musicBpm by remember { mutableIntStateOf(sfx.musicBpm) }
  var musicVolume by remember { mutableFloatStateOf(sfx.musicVolume) }
  var sfxVolume by remember { mutableFloatStateOf(sfx.sfxVolume) }
  var activeMusicArchetypeId by remember { mutableStateOf(sfx.currentMusicRecipe.archetype.id) }
  var activeMusicSummary by remember { mutableStateOf(sfx.currentMusicRecipe.customPromptSummary) }
  var musicForgePrompt by remember { mutableStateOf(localPrefs.getMusicForgePrompt()) }

  var currentFps by remember { mutableIntStateOf(60) }
  var frameTick by remember { mutableIntStateOf(0) }
  var toastMessage by remember {
    mutableStateOf<String?>("🛰️ RIFT NEXUS COMMAND ONLINE // SELECT GAME MODE TO DEPLOY")
  }
  var toastVersion by remember { mutableIntStateOf(0) }
  var unreadCommsCount by remember { mutableIntStateOf(0) }

  var bootToNexusHub by remember { mutableStateOf(localPrefs.getBootToNexusHub()) }
  var activeModal by remember {
    mutableStateOf(if (bootToNexusHub) ActiveModal.NEXUS else ActiveModal.NONE)
  }
  var nexusInitialLayoutMode by remember { mutableStateOf(NexusHubLayoutMode.DECK_ONLY) }
  var showFullChangelogModal by remember { mutableStateOf(false) }
  var previousModalBeforeStore by remember { mutableStateOf(ActiveModal.DOSSIER) }
  var previousModalBeforeNexus by remember { mutableStateOf(ActiveModal.NONE) }
  var callsign by remember(currentUserId, isGoogleAuthenticated) {
    mutableStateOf(
      if (isGoogleAuthenticated) localPrefs.getCallsign(currentUserId) else localPrefs.getCallsign()
    )
  }
  var needsInitialSwornCallsign by remember(currentUserId, isGoogleAuthenticated) {
    mutableStateOf(isGoogleAuthenticated && !localPrefs.hasChosenInitialCallsign(currentUserId))
  }
  var initialSwornCallsignDraft by remember(callsign) { mutableStateOf(callsign) }
  var syndicateTag by remember { mutableStateOf(localPrefs.getSyndicateTag()) }
  var syndicateName by remember { mutableStateOf(localPrefs.getSyndicateName()) }
  var selectedShipId by remember {
    mutableIntStateOf(localPrefs.getPreferredShipId().coerceIn(1, 10))
  }
  var selectedMode by remember { mutableStateOf("FFA") }
  var activeCustomSlotId by remember(currentUserId) {
    mutableStateOf(localPrefs.getActiveCustomSlotId(currentUserId))
  }
  var activeCustomSlot by remember(currentUserId, activeCustomSlotId) {
    val loaded = localPrefs.getCustomShipSlot(activeCustomSlotId, currentUserId)
    RiftCodex.updateActiveCustomShipSlot(loaded)
    mutableStateOf(loaded)
  }
  var slot9BUnlocked by remember(currentUserId) {
    mutableStateOf(localPrefs.isCustomSlotUnlocked("Frame B", currentUserId))
  }
  var slot9CUnlocked by remember(currentUserId) {
    mutableStateOf(localPrefs.isCustomSlotUnlocked("Frame C", currentUserId))
  }
  var slot9DUnlocked by remember(currentUserId) {
    mutableStateOf(localPrefs.isCustomSlotUnlocked("Frame D", currentUserId))
  }
  var slot9EUnlocked by remember(currentUserId) {
    mutableStateOf(localPrefs.isCustomSlotUnlocked("Frame E", currentUserId))
  }
  var cameraZoom by remember { mutableFloatStateOf(localPrefs.getCameraZoom()) }
  var playfieldTextScale by remember {
    mutableFloatStateOf(localPrefs.getPlayfieldTextScale().coerceIn(0.75f, 1.75f))
  }
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
      this.playfieldTextScale = localPrefs.getPlayfieldTextScale().coerceIn(0.75f, 1.75f)

      localPlayer.callsign = callsign
      localPlayer.syndicateTag = syndicateTag
      val savedArmada = localPrefs.getActiveArmadaName()
      if (savedArmada.isNotEmpty()) {
        formOrJoinArmada(savedArmada, localPrefs.getFriendsList().take(2))
      }
      switchLocalPlayerShip(selectedShipId, forceFromHangar = true)
      // Never auto-join a match on app launch: remain in peaceful NEXUS orbit until explicit deploy
      leaveActiveMatchToNexusOrbit()
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
      if (isGoogleAuthenticated) {
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
      }
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

  // Load persisted HUD layout offsets, Calibration overrides, and Forged Buffs on startup
  LaunchedEffect(Unit) {
    RiftCalibrationState.loadPersistedLayout(context)
    RiftCalibrationState.applyCalibrationChangesLive(engine, null)
  }

  // P1: Non-Blocking Tactical Toasts auto-fade after 1.5s
  LaunchedEffect(toastVersion) {
    if (toastMessage != null) {
      delay(1500L)
      toastMessage = null
    }
  }

  // Monotonic max(local, cloud) merge for Sworn Pilot career stats & callsign across devices/updates
  LaunchedEffect(dossierState, currentUserId, isGoogleAuthenticated) {
    val cloudDossier = (dossierState as? UiState.Success)?.data ?: return@LaunchedEffect
    val mergedAllTimeKills = maxOf(engine.allTimeKills, cloudDossier.allTimeKills)
    val mergedAllTimeDeaths = maxOf(engine.allTimeDeaths, cloudDossier.allTimeDeaths)
    val mergedAllTimeBountyPeak = maxOf(engine.allTimeBountyPeak, cloudDossier.allTimeBountyPeak)
    val mergedPvpKills = maxOf(engine.pvpKills, cloudDossier.pvpKills)
    val mergedPvpDeaths = maxOf(engine.pvpDeaths, cloudDossier.pvpDeaths)
    val mergedPvpBountyPeak = maxOf(engine.pvpBountyPeak, cloudDossier.pvpBountyPeak)
    val mergedConvergencesWon = maxOf(engine.convergencesWon, cloudDossier.convergencesWon)

    val anyCareerRaised =
      mergedAllTimeKills > engine.allTimeKills ||
        mergedAllTimeDeaths > engine.allTimeDeaths ||
        mergedAllTimeBountyPeak > engine.allTimeBountyPeak ||
        mergedPvpKills > engine.pvpKills ||
        mergedPvpDeaths > engine.pvpDeaths ||
        mergedPvpBountyPeak > engine.pvpBountyPeak ||
        mergedConvergencesWon > engine.convergencesWon

    engine.allTimeKills = mergedAllTimeKills
    engine.allTimeDeaths = mergedAllTimeDeaths
    engine.allTimeBountyPeak = mergedAllTimeBountyPeak
    engine.pvpKills = mergedPvpKills
    engine.pvpDeaths = mergedPvpDeaths
    engine.pvpBountyPeak = mergedPvpBountyPeak
    engine.convergencesWon = mergedConvergencesWon

    if (anyCareerRaised) {
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
    }

    if (isGoogleAuthenticated && currentUserId.isNotBlank()) {
      val cloudCallsign = cloudDossier.callsign.trim().uppercase(Locale.US)
      val cloudHasChosenName =
        cloudCallsign.length >= 3 &&
          cloudCallsign != "COMMANDER" &&
          !cloudCallsign.startsWith("RFT-") &&
          !cloudCallsign.contains("[BOT]")
      val localHasChosenName =
        callsign.length >= 3 &&
          callsign != "COMMANDER" &&
          !callsign.startsWith("RFT-") &&
          !callsign.contains("[BOT]")

      if (cloudHasChosenName && !localHasChosenName) {
        val adopted = localPrefs.setCallsign(cloudCallsign, currentUserId)
        localPrefs.setHasChosenInitialCallsign(true, currentUserId)
        callsign = adopted
        initialSwornCallsignDraft = adopted
        needsInitialSwornCallsign = false
        engine.localPlayer.callsign = adopted
      } else if (localHasChosenName && cloudCallsign != callsign) {
        val saved = localPrefs.setCallsign(callsign, currentUserId)
        localPrefs.setHasChosenInitialCallsign(true, currentUserId)
        needsInitialSwornCallsign = false
        engine.localPlayer.callsign = saved
        viewModel.syncNativePilotStats(
          callsign = saved,
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
      }
    }
  }

  // Push remote sector pilots from Firestore into engine
  LaunchedEffect(sectorPilotsState) {
    val remoteList = (sectorPilotsState as? UiState.Success)?.data ?: return@LaunchedEffect
    engine.updateRemotePilots(remoteList, currentUserId)
  }

  // Merge cloud-registered real-player Syndicates across devices
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
        if (msg.senderUid != currentUserId && activeModal != ActiveModal.NEXUS) {
          newIncomingCount++
        }
      }
    }
    if (newIncomingCount > 0) {
      unreadCommsCount += newIncomingCount
    }
  }

  // Live telemetry heartbeat for Google Authenticated sessions (only broadcasts active match presence when joined)
  LaunchedEffect(currentUserId, isGoogleAuthenticated) {
    if (!isGoogleAuthenticated) return@LaunchedEffect
    while (true) {
      delay(350L)
      if (!engine.hasJoinedMatch) continue
      val p = engine.localPlayer
      viewModel.syncNativeLiveState(
        sectorId = engine.activeMatchSectorTag(),
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
  // All 9 Persistent NEXUS Master Window Tabs in exact header ribbon order
  val nexusHubModals = remember {
    listOf(
      ActiveModal.NEXUS,
      ActiveModal.HANGAR,
      ActiveModal.RIFT_SHARDS_STORE,
      ActiveModal.ARMADA,
      ActiveModal.SYNDICATES,
      ActiveModal.DOSSIER,
      ActiveModal.CODEX,
      ActiveModal.SETTINGS,
      ActiveModal.CALIBRATE,
    )
  }

  LaunchedEffect(engine.currentCustomGame, engine.activeRankedMode, frameTick) {
    if (engine.currentCustomGame == null) {
      RiftCalibrationState.enforceRankedFactoryDefaultsIfNeeded(engine)
    }
  }

  LaunchedEffect(latestCalibBroadcast?.updatedAtMs, engine.currentCustomGame) {
    val broadcast = latestCalibBroadcast
    val activeCustom = engine.currentCustomGame
    if (
      activeCustom != null &&
        broadcast != null &&
        (broadcast.customGameId == activeCustom.instanceId ||
          broadcast.customGameId == "global_custom" ||
          broadcast.customGameId == "custom")
    ) {
      RiftCalibrationState.applyRemoteCustomGameSyncPayload(
        broadcast.payloadJson,
        engine,
        context,
      )
      toastMessage = "📡 CALIBRATION SYNCED BY ${broadcast.authorCallsign.uppercase()}"
      toastVersion++
    }
  }

  LaunchedEffect(
    activeModal,
    RiftCalibrationState.isCalibrationModalOpen,
    engine.hasJoinedMatch,
    frameTick,
  ) {
    val isInMenu =
      activeModal != ActiveModal.NONE ||
        RiftCalibrationState.isCalibrationModalOpen ||
        !engine.hasJoinedMatch
    engine.isInLobbyStaging = isInMenu
    if (isInMenu) {
      engine.localPlayer.isIdleDraining = false
      engine.localPlayer.playfieldIdleTimer = 0f
      engine.localPlayer.safeZoneStayTimer = 0f
      engine.localPlayer.safeZoneLockoutTimer = 0f
    } else {
      arenaViewRef?.requestFocus()
    }
  }

  BackHandler {
    if (RiftCalibrationState.isUiUnlockedForDragging && RiftCalibrationState.isCalibrationModalOpen) {
      RiftCalibrationState.isUiUnlockedForDragging = false
      RiftCalibrationState.isCalibrationModalOpen = false
      activeModal = ActiveModal.CALIBRATE
    } else if (RiftCalibrationState.isCalibrationModalOpen) {
      RiftCalibrationState.isCalibrationModalOpen = false
    } else if (activeModal == ActiveModal.NEXUS) {
      activeModal = previousModalBeforeNexus
      previousModalBeforeNexus = ActiveModal.NONE
    } else if (activeModal != ActiveModal.NONE) {
      activeModal = if (!engine.hasJoinedMatch) ActiveModal.NEXUS else ActiveModal.NONE
    } else if (!engine.hasJoinedMatch) {
      activeModal = ActiveModal.NEXUS
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
                RiftCalibrationState.gamepadConnected = true
                activeModal =
                  if (activeModal == ActiveModal.HANGAR) ActiveModal.NONE else ActiveModal.HANGAR
                sfx.playMutedClick()
                true
              }

              KeyEvent.KEYCODE_BUTTON_SELECT -> {
                RiftCalibrationState.gamepadConnected = true
                if (RiftCalibrationState.isUiUnlockedForDragging) {
                  RiftCalibrationState.isUiUnlockedForDragging = false
                  RiftCalibrationState.isCalibrationModalOpen = false
                  activeModal = ActiveModal.CALIBRATE
                } else {
                  RiftCalibrationState.isCalibrationModalOpen = false
                  activeModal =
                    if (activeModal == ActiveModal.CALIBRATE) ActiveModal.NONE else ActiveModal.CALIBRATE
                }
                sfx.playMutedClick()
                true
              }

              KeyEvent.KEYCODE_BUTTON_L1 -> {
                if (activeModal != ActiveModal.NONE) {
                  RiftCalibrationState.gamepadConnected = true
                  val curIdx = nexusHubModals.indexOf(activeModal)
                  val nextIdx =
                    if (curIdx <= 0) nexusHubModals.lastIndex else curIdx - 1
                  val nextModal = nexusHubModals[nextIdx]
                  if (nextModal == ActiveModal.NEXUS) unreadCommsCount = 0
                  activeModal = nextModal
                  sfx.playMutedClick()
                  true
                } else {
                  false
                }
              }

              KeyEvent.KEYCODE_BUTTON_R1 -> {
                if (activeModal != ActiveModal.NONE) {
                  RiftCalibrationState.gamepadConnected = true
                  val curIdx = nexusHubModals.indexOf(activeModal)
                  val nextIdx =
                    if (curIdx < 0 || curIdx >= nexusHubModals.lastIndex) 0 else curIdx + 1
                  val nextModal = nexusHubModals[nextIdx]
                  if (nextModal == ActiveModal.NEXUS) unreadCommsCount = 0
                  activeModal = nextModal
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
        .testTag("rift_arena_native_container")
  ) {
    // 1. High-Performance 120Hz Native Canvas View (Unscaled 1:1 Device Pixels)
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
            if (RiftCalibrationState.isUiUnlockedForDragging) {
              RiftCalibrationState.isUiUnlockedForDragging = false
              RiftCalibrationState.isCalibrationModalOpen = false
              activeModal = ActiveModal.CALIBRATE
            } else {
              RiftCalibrationState.isCalibrationModalOpen = false
              activeModal =
                if (activeModal == ActiveModal.CALIBRATE) ActiveModal.NONE else ActiveModal.CALIBRATE
            }
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

    // 2. Native Fullscreen HUD Overlay (1:1 Device Touch Ergonomics)
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
        previousModalBeforeNexus = ActiveModal.NONE
        nexusInitialLayoutMode = NexusHubLayoutMode.SPLIT_COMMAND
        activeModal = ActiveModal.NEXUS
      },
      onOpenDossier = { activeModal = ActiveModal.DOSSIER },
      onOpenProgression = {
        previousModalBeforeStore = ActiveModal.NEXUS
        activeModal = ActiveModal.RIFT_SHARDS_STORE
      },
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
        nexusInitialLayoutMode = NexusHubLayoutMode.SPLIT_COMMAND
        activeModal = ActiveModal.NEXUS
      },
      onOpenModes = {
        previousModalBeforeNexus = ActiveModal.NONE
        nexusInitialLayoutMode = NexusHubLayoutMode.DECK_ONLY
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

    // 3. Single Persistent NEXUS Master Window Shell & Overlays Under Global DeviceAwareModalAutoScaler
    val isLiveHudDragPillOpen =
      RiftCalibrationState.isUiUnlockedForDragging && RiftCalibrationState.isCalibrationModalOpen

    val hasAnyModalVisible =
      activeModal != ActiveModal.NONE ||
        showFullChangelogModal ||
        isLiveHudDragPillOpen ||
        needsInitialSwornCallsign

    if (hasAnyModalVisible) {
      DeviceAwareModalAutoScaler {
        Box(modifier = Modifier.fillMaxSize()) {
          if (activeModal == ActiveModal.CHAT) {
            LaunchedEffect(Unit) {
              unreadCommsCount = 0
              activeModal = ActiveModal.NEXUS
            }
          }

          if (activeModal in nexusHubModals) {
            val rawCloudLeaderboard =
              (leaderboardState as? UiState.Success)?.data ?: emptyList()
            val realSwornCloudLeaderboard =
              remember(rawCloudLeaderboard) {
                rawCloudLeaderboard.filter { entry ->
                  val cleanSyn = RiftGameEngine.cleanTag(entry.syndicateTag)
                  val cleanName = entry.callsign.trim().uppercase(Locale.US)
                  cleanName.isNotBlank() &&
                    !cleanName.contains("[BOT]") &&
                    !cleanName.startsWith("RFT-") &&
                    cleanName != "COMMANDER" &&
                    cleanSyn != "BOT" &&
                    cleanSyn != "RFT" &&
                    cleanSyn != "GUEST"
                }
              }

            val highestBountySummary =
              remember(
                realSwornCloudLeaderboard,
                engine.allTimeBountyPeak,
                isGoogleAuthenticated,
                callsign,
                syndicateTag,
                frameTick,
              ) {
                val topCloudEntry = realSwornCloudLeaderboard.maxByOrNull { it.allTimeBounty }
                val topRemoteHumanShip =
                  engine.ships
                    .filter { s ->
                      s != engine.localPlayer &&
                        s.isHuman &&
                        !s.callsign.contains("[BOT]", ignoreCase = true) &&
                        !s.callsign.startsWith("RFT-", ignoreCase = true) &&
                        RiftGameEngine.cleanTag(s.syndicateTag) !in
                          setOf("BOT", "RFT", "GUEST")
                    }
                    .maxByOrNull { it.bounty }

                val cloudMax = topCloudEntry?.allTimeBounty ?: 0L
                val localIsSworn =
                  isGoogleAuthenticated &&
                    callsign.isNotBlank() &&
                    !callsign.startsWith("RFT-", ignoreCase = true) &&
                    !callsign.contains("[BOT]", ignoreCase = true) &&
                    callsign != "COMMANDER"
                val localMax =
                  if (localIsSworn) {
                    maxOf(
                      engine.allTimeBountyPeak,
                      allTimePeakSnapshot?.bountyReached?.toLong() ?: 0L,
                    )
                  } else {
                    0L
                  }
                val remoteLiveMax = (topRemoteHumanShip?.bounty ?: 0).toLong()

                when {
                  cloudMax >= localMax &&
                    cloudMax >= remoteLiveMax &&
                    cloudMax > 0L &&
                    topCloudEntry != null -> {
                    val tag = RiftGameEngine.cleanTag(topCloudEntry.syndicateTag)
                    val prefix = if (tag.isNotBlank() && tag != "FREE") "[$tag] " else ""
                    "$prefix${topCloudEntry.callsign} (${topCloudEntry.allTimeBounty})"
                  }
                  localIsSworn && localMax >= remoteLiveMax && localMax > 0L -> {
                    val cleanSyn = RiftGameEngine.cleanTag(syndicateTag)
                    val prefix =
                      if (
                        cleanSyn.isNotBlank() &&
                          cleanSyn !in setOf("GUEST", "FREE", "RFT", "BOT")
                      ) {
                        "[$cleanSyn] "
                      } else {
                        ""
                      }
                    "$prefix$callsign ($localMax)"
                  }
                  topRemoteHumanShip != null && remoteLiveMax > 0L -> {
                    val cleanSyn = RiftGameEngine.cleanTag(topRemoteHumanShip.syndicateTag)
                    val prefix =
                      if (
                        cleanSyn.isNotBlank() &&
                          cleanSyn !in setOf("GUEST", "FREE", "RFT", "BOT")
                      ) {
                        "[$cleanSyn] "
                      } else {
                        ""
                      }
                    "$prefix${topRemoteHumanShip.callsign} (${topRemoteHumanShip.bounty})"
                  }
                  else -> "NO SWORN RECORD"
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
                cloudLeaderboard = realSwornCloudLeaderboard,
                allTimePeakSnapshot =
                  localPrefs.getAllTimePeakBountySnapshot(currentUserId)
                    ?: allTimePeakSnapshot,
                pvpPeakSnapshot =
                  localPrefs.getPvpPeakBountySnapshot(currentUserId) ?: pvpPeakSnapshot,
                syndicateHistory =
                  localPrefs.getSyndicateServiceHistory(currentUserId)
                    .ifEmpty { syndicateHistory },
              )
            }

            val bestShipId =
              remember(
                currentUserId,
                selectedShipId,
                allTimePeakSnapshot,
                pvpPeakSnapshot,
                frameTick,
              ) {
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
              remember(frameTick, sectorPilotsState) { engine.getTotalHumanPilotsOnline() }
            val syndicateOnlineCount =
              remember(syndicateTag, frameTick, friendsRefreshKey, sectorPilotsState) {
                val cleanSyn = RiftGameEngine.cleanTag(syndicateTag)
                if (
                  cleanSyn == "GUEST" ||
                    cleanSyn == "FREE" ||
                    cleanSyn == "RFT" ||
                    cleanSyn == "BOT" ||
                    cleanSyn.isEmpty()
                ) {
                  0
                } else {
                  val remoteList =
                    (sectorPilotsState as? UiState.Success)?.data ?: emptyList()
                  val remoteSynOnline =
                    remoteList
                      .mapNotNull { st ->
                        val rUid = st.userId.ifEmpty { st.id }
                        if (
                          rUid.isNotBlank() &&
                            rUid != currentUserId &&
                            RiftGameEngine.cleanTag(st.syndicateTag) == cleanSyn
                        ) {
                          rUid
                        } else {
                          null
                        }
                      }
                      .distinct()
                      .size
                  (if (engine.hasJoinedMatch) 1 else 0) + remoteSynOnline
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

            val activeModuleKey =
              when (activeModal) {
                ActiveModal.NEXUS -> "NEXUS"
                ActiveModal.HANGAR -> "HANGAR"
                ActiveModal.RIFT_SHARDS_STORE -> "PROGRESSION"
                ActiveModal.ARMADA -> "ARMADA"
                ActiveModal.SYNDICATES -> "SYNDICATES"
                ActiveModal.DOSSIER -> "DOSSIER"
                ActiveModal.CODEX -> "CODEX"
                ActiveModal.SETTINGS -> "SETTINGS"
                ActiveModal.CALIBRATE -> "CALIBRATE"
                else -> "NEXUS"
              }

            val embeddedModuleContent: (@Composable () -> Unit)? =
              if (activeModal == ActiveModal.NEXUS) {
                null
              } else {
                {
                  when (activeModal) {
                    ActiveModal.HANGAR -> {
                      val (_, _, hangarLvlFrac) =
                        remember(frameTick) { engine.getXpProgressInCurrentLevel() }
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
                          RiftCalibrationState.unlockFullNinthShipOverride,
                        activeCustomSlot = activeCustomSlot,
                        slot9BUnlocked = slot9BUnlocked,
                        slot9CUnlocked = slot9CUnlocked,
                        slot9DUnlocked = slot9DUnlocked,
                        slot9EUnlocked = slot9EUnlocked,
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
                          RiftCodex.updateActiveCustomShipSlot(updatedSlot)
                          engine.spentCustomizationPoints = updatedSlot.totalAllocatedPoints()
                          engine.syncLocalPlayerCustomShipSpec()
                        },
                        onSwitchCustomSlot = { targetSlotId ->
                          activeCustomSlotId = targetSlotId
                          localPrefs.setActiveCustomSlotId(targetSlotId, currentUserId)
                          val loaded = localPrefs.getCustomShipSlot(targetSlotId, currentUserId)
                          activeCustomSlot = loaded
                          RiftCodex.updateActiveCustomShipSlot(loaded)
                          engine.spentCustomizationPoints = loaded.totalAllocatedPoints()
                          engine.syncLocalPlayerCustomShipSpec()
                          sfx.playPrize()
                        },
                        onUnlockCustomSlot = { targetSlotId ->
                          val gate = RiftProgressionCatalog.getFrameSlotGate(targetSlotId)
                          val cost = gate.shardCost
                          val freeByLevel = engine.pilotLevel >= gate.freeAtLevel
                          if (freeByLevel || engine.riftShards >= cost) {
                            if (!freeByLevel && cost > 0L) {
                              engine.riftShards -= cost
                            }
                            engine.unlockedExtraSlots++
                            localPrefs.setCustomSlotUnlocked(targetSlotId, true, currentUserId)
                            slot9BUnlocked = localPrefs.isCustomSlotUnlocked("Frame B", currentUserId)
                            slot9CUnlocked = localPrefs.isCustomSlotUnlocked("Frame C", currentUserId)
                            slot9DUnlocked = localPrefs.isCustomSlotUnlocked("Frame D", currentUserId)
                            slot9EUnlocked = localPrefs.isCustomSlotUnlocked("Frame E", currentUserId)
                            activeCustomSlotId = targetSlotId
                            localPrefs.setActiveCustomSlotId(targetSlotId, currentUserId)
                            val loaded = localPrefs.getCustomShipSlot(targetSlotId, currentUserId)
                            activeCustomSlot = loaded
                            RiftCodex.updateActiveCustomShipSlot(loaded)
                            engine.spentCustomizationPoints = loaded.totalAllocatedPoints()
                            engine.syncLocalPlayerCustomShipSpec()
                            engine.onStatsUpdated()
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
                            RiftCodex.updateActiveCustomShipSlot(zeroed)
                            engine.spentCustomizationPoints = 0
                            engine.syncLocalPlayerCustomShipSpec()
                            engine.onStatsUpdated()
                            sfx.playPrize()
                            true
                          } else {
                            false
                          }
                        },
                        onForgeAiHullDesign = { promptText, hullTitle ->
                          val isFirstFree = activeCustomSlot.aiGenerationsCount == 0
                          val canAfford =
                            isFirstFree || engine.aiShipDesignTokens > 0 || engine.riftShards >= 150L
                          if (!canAfford) {
                            Pair(
                              false,
                              "⚠️ NEED 150 RIFT SHARDS FOR HULL RE-DESIGN (OPEN PROGRESSION TAB).",
                            )
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
                              hullTitle
                                .trim()
                                .uppercase(Locale.US)
                                .take(18)
                                .ifEmpty { baseRecipe.hullName }
                            val finalRecipe = baseRecipe.copy(hullName = finalTitle)
                            val updatedSlot =
                              activeCustomSlot.copy(
                                customName = finalTitle,
                                visualRecipe = finalRecipe,
                                aiGenerationsCount = nextGenCount,
                              )
                            activeCustomSlot = updatedSlot
                            localPrefs.saveCustomShipSlot(updatedSlot, currentUserId)
                            RiftCodex.updateActiveCustomShipSlot(updatedSlot)
                            engine.syncLocalPlayerCustomShipSpec()
                            engine.onStatsUpdated()
                            sfx.playPrize()
                            val costNote = if (isFirstFree) "FREE 1ST DESIGN" else "-150 SHARDS"
                            Pair(
                              true,
                              "🎨 HULL FORGED ($costNote): [$finalTitle] (${finalRecipe.silhouette})",
                            )
                          }
                        },
                        onToggleDevLevel20Or100 = {
                          RiftCalibrationState.unlockFullNinthShipOverride =
                            !RiftCalibrationState.unlockFullNinthShipOverride
                          RiftCalibrationState.savePersistedLayout(context)
                          engine.syncLocalPlayerCustomShipSpec()
                          sfx.playPrize()
                        },
                        isNexusStreaming = isNexusStreaming,
                        onOpenNexus = {
                          activeModal = ActiveModal.NEXUS
                        },
                        onDeployPilot = { newCallsign, newShipId, _, asGuest ->
                          if (asGuest) {
                            localPrefs.setGuestBypassEnabled(true)
                          }
                          val cleanCallsign =
                            if (isGoogleAuthenticated) {
                              localPrefs.setCallsign(newCallsign, currentUserId)
                            } else {
                              localPrefs.setCallsign(newCallsign)
                            }
                          if (
                            isGoogleAuthenticated &&
                              cleanCallsign.length >= 3 &&
                              cleanCallsign != "COMMANDER" &&
                              !cleanCallsign.startsWith("RFT-")
                          ) {
                            localPrefs.setHasChosenInitialCallsign(true, currentUserId)
                            needsInitialSwornCallsign = false
                          }
                          val clampedShipId = newShipId.coerceIn(1, 10)
                          val activeModeId = engine.activeRankedMode.id
                          callsign = cleanCallsign
                          selectedShipId = clampedShipId
                          selectedMode = activeModeId
                          syndicateTag = localPrefs.getSyndicateTag()
                          syndicateName = localPrefs.getSyndicateName()

                          localPrefs.setPreferredShipId(clampedShipId)
                          localPrefs.setPreferredMode(activeModeId)

                          engine.localPlayer.callsign = cleanCallsign
                          engine.localPlayer.syndicateTag = syndicateTag
                          engine.syncLocalArmadaSelfSlot()
                          engine.switchLocalPlayerShip(clampedShipId, forceFromHangar = true)
                          if (!engine.hasJoinedMatch) {
                            engine.deployToRankedMode(
                              engine.activeRankedMode,
                              engine.activeTeamSizeBracket,
                            )
                          }
                          engine.onStatsUpdated()
                          activeModal = ActiveModal.NONE
                        },
                        onCloseModal = {
                          activeModal = if (!engine.hasJoinedMatch) ActiveModal.NEXUS else ActiveModal.NONE
                        },
                      )
                    }

                    ActiveModal.RIFT_SHARDS_STORE -> {
                      val (curLvlXp, nextLvlSpan, lvlFrac) =
                        remember(frameTick) { engine.getXpProgressInCurrentLevel() }
                      val presencePct =
                        remember(frameTick) {
                          (engine.getMatchPresenceRatio() * 100f).toInt().coerceIn(0, 100)
                        }

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
                        onSpendRiftShards = { cost ->
                          if (engine.riftShards >= cost) {
                            engine.riftShards -= cost
                            sfx.playPrize()
                            engine.onStatsUpdated()
                            true
                          } else {
                            false
                          }
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
                          val nextSlot =
                            when {
                              !slot9BUnlocked -> "Frame B"
                              !slot9CUnlocked -> "Frame C"
                              !slot9DUnlocked -> "Frame D"
                              !slot9EUnlocked -> "Frame E"
                              else -> null
                            }
                          if (nextSlot == null) {
                            false
                          } else {
                            val gate = RiftProgressionCatalog.getFrameSlotGate(nextSlot)
                            val freeByLevel = engine.pilotLevel >= gate.freeAtLevel
                            if (freeByLevel || engine.riftShards >= gate.shardCost) {
                              if (!freeByLevel && gate.shardCost > 0L) {
                                engine.riftShards -= gate.shardCost
                              }
                              engine.unlockedExtraSlots++
                              localPrefs.setCustomSlotUnlocked(nextSlot, true, currentUserId)
                              slot9BUnlocked = localPrefs.isCustomSlotUnlocked("Frame B", currentUserId)
                              slot9CUnlocked = localPrefs.isCustomSlotUnlocked("Frame C", currentUserId)
                              slot9DUnlocked = localPrefs.isCustomSlotUnlocked("Frame D", currentUserId)
                              slot9EUnlocked = localPrefs.isCustomSlotUnlocked("Frame E", currentUserId)
                              sfx.playPrize()
                              engine.onStatsUpdated()
                              true
                            } else {
                              false
                            }
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
                        onOpenHangarShip10 = {
                          selectedShipId = 10
                          localPrefs.setPreferredShipId(10)
                          engine.switchLocalPlayerShip(10, forceFromHangar = true)
                          engine.onStatsUpdated()
                          activeModal = ActiveModal.HANGAR
                        },
                        onCloseModal = { activeModal = previousModalBeforeStore },
                      )
                    }

                    ActiveModal.ARMADA -> {
                      val friendsList =
                        remember(syndicateTag, frameTick, friendsRefreshKey) {
                          localPrefs.getFriendsList()
                        }

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
                              if (engine.activeArmadaSlots.size <= 1) friendsList.take(1)
                              else emptyList(),
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
                          val targetArmada =
                            friend.currentArmadaName.ifEmpty { "${friend.syndicateTag}_FLEET" }
                          engine.formOrJoinArmada(targetArmada, listOf(friend))
                          armadaStatusBanner =
                            "✓ INVITE REQUEST ACCEPTED BY ${friend.callsign}! DEPLOYED IN ARMADA [$targetArmada] (${engine.getCurrentArmadaSize()}/5)."
                          friendsRefreshKey++
                        },
                        onRemoveArmadaMember = { callsignToDrop ->
                          engine.removeMemberFromArmada(callsignToDrop)
                          armadaStatusBanner =
                            "🛡️ ROSTER UPDATED -> NOW IN ${engine.getCurrentArmadaTierLabel()} TIER."
                          friendsRefreshKey++
                        },
                        onLeaveArmadaToSolo = {
                          engine.leaveArmadaToSolo()
                          armadaStatusBanner = "🛰️ LEFT ARMADA -> STATUS: OPERATING INDEPENDENTLY."
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
                        onCloseModal = {
                          activeModal = if (!engine.hasJoinedMatch) ActiveModal.NEXUS else ActiveModal.NONE
                        },
                      )
                    }

                    ActiveModal.SYNDICATES -> {
                      val directoryList =
                        remember(syndicateTag, frameTick, cloudSyndicatesState) {
                          val cloudList =
                            (cloudSyndicatesState as? UiState.Success)?.data ?: emptyList()
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
                              unlocked =
                                engine.hvtEliminations >= 1L || engine.allTimeBountyPeak >= 250L,
                              progressText = "${engine.hvtEliminations} HVT KILLS",
                            ),
                            PilotAchievement(
                              id = "trench_runner",
                              title = "TRENCH RUNNER",
                              description =
                                "Achieve 10+ total eliminations across sector skirmishes.",
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
                                  syndicateTag != "FREE" &&
                                  syndicateTag != "RFT",
                              progressText =
                                if (syndicateTag.isNotBlank()) "[$syndicateTag] ACTIVE"
                                else "UNAFFILIATED",
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
                        activeShipsInSector = engine.ships.filter { it.isHuman },
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
                              "🛰️ LEFT SYNDICATE -> STATUS: UNAFFILIATED. 15-DAY LOCK COOLDOWN TRIGGERED."
                            engine.onStatsUpdated()
                          } else {
                            syndicateStatusBanner =
                              "🔒 CANNOT LEAVE SYNDICATE YET: ${localPrefs.getRemainingSyndicateLockFormatted()} REMAINING."
                          }
                        },
                        onCreateCustomSyndicate = { rawTag, rawName, rawMotto ->
                          val res =
                            localPrefs.createCustomSyndicate(
                              tag = rawTag,
                              name = rawName,
                              motto = rawMotto,
                              ownerUid = if (isGoogleAuthenticated) currentUserId else "",
                            )
                          res
                            .onSuccess { created ->
                              syndicateTag = created.tag
                              syndicateName = created.name
                              syndicateHistory = localPrefs.getSyndicateServiceHistory(currentUserId)
                              engine.localPlayer.syndicateTag = syndicateTag
                              engine.syncLocalArmadaSelfSlot()
                              viewModel.createCloudSyndicate(created)
                              syndicateStatusBanner =
                                "🏛️ CHARTERED [${created.tag}] ${created.name}! LOCKED FOR 15d 00h."
                              engine.onStatsUpdated()
                            }
                            .onFailure { err ->
                              syndicateStatusBanner = "⚠️ ${err.message}"
                            }
                        },
                        onCloseModal = {
                          activeModal = if (!engine.hasJoinedMatch) ActiveModal.NEXUS else ActiveModal.NONE
                        },
                      )
                    }

                    ActiveModal.DOSSIER -> {
                      val (curLvlXp, nextLvlSpan, lvlFrac) =
                        remember(frameTick) { engine.getXpProgressInCurrentLevel() }
                      val presencePct =
                        remember(frameTick) {
                          (engine.getMatchPresenceRatio() * 100f).toInt().coerceIn(0, 100)
                        }

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
                        cloudLeaderboardEntries = realSwornCloudLeaderboard,
                        allTimePeakSnapshot = allTimePeakSnapshot,
                        pvpPeakSnapshot = pvpPeakSnapshot,
                        currentLifeSnapshot =
                          remember(frameTick) {
                            if (engine.localPlayer.respawnTimer <= 0f) {
                              engine.currentLifeStats.toSnapshot()
                            } else {
                              null
                            }
                          },
                        syndicateServiceHistory = syndicateHistory,
                        onOpenRiftShardsStore = {
                          previousModalBeforeStore = ActiveModal.DOSSIER
                          activeModal = ActiveModal.RIFT_SHARDS_STORE
                        },
                        onResetLocalPilotData = {
                          localPrefs.resetAllLocalPilotData(
                            if (isGoogleAuthenticated) currentUserId else null
                          )
                          callsign =
                            if (isGoogleAuthenticated) localPrefs.getCallsign(currentUserId)
                            else localPrefs.getCallsign()
                          syndicateTag = localPrefs.getSyndicateTag()
                          syndicateName = localPrefs.getSyndicateName()
                          selectedShipId = localPrefs.getPreferredShipId()
                          slot9BUnlocked = localPrefs.isCustomSlotUnlocked("Frame B", currentUserId)
                          slot9CUnlocked = localPrefs.isCustomSlotUnlocked("Frame C", currentUserId)
                          slot9DUnlocked = localPrefs.isCustomSlotUnlocked("Frame D", currentUserId)
                          slot9EUnlocked = localPrefs.isCustomSlotUnlocked("Frame E", currentUserId)
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
                          toastMessage = "↺ PILOT STATS RESET TO LEVEL 1 (CALLSIGN PRESERVED)"
                          toastVersion++
                        },
                        onCallsignChanged = { updatedCallsign ->
                          val saved =
                            if (isGoogleAuthenticated) {
                              localPrefs.setHasChosenInitialCallsign(true, currentUserId)
                              localPrefs.setCallsign(updatedCallsign, currentUserId)
                            } else {
                              localPrefs.setCallsign(updatedCallsign)
                            }
                          callsign = saved
                          engine.localPlayer.callsign = saved
                          engine.onStatsUpdated()
                          toastMessage = "✓ SWORN PILOT CALLSIGN UPDATED TO $saved"
                          toastVersion++
                        },
                        onUpdateRiftShards = { remainingShards ->
                          engine.riftShards = remainingShards
                          localPrefs.setRiftShards(remainingShards, currentUserId)
                          engine.onStatsUpdated()
                        },
                        onCloseModal = {
                          activeModal = if (!engine.hasJoinedMatch) ActiveModal.NEXUS else ActiveModal.NONE
                        },
                      )
                    }

                    ActiveModal.CODEX -> {
                      CodexModal(
                        isNexusStreaming = isNexusStreaming,
                        onOpenNexus = {
                          activeModal = ActiveModal.NEXUS
                        },
                        onCloseModal = {
                          activeModal = if (!engine.hasJoinedMatch) ActiveModal.NEXUS else ActiveModal.NONE
                        },
                      )
                    }

                    ActiveModal.SETTINGS -> {
                      SettingsModal(
                        fps = currentFps,
                        sfxEnabled = sfxEnabled,
                        musicEnabled = musicEnabled,
                        musicBpm = musicBpm,
                        musicVolume = musicVolume,
                        sfxVolume = sfxVolume,
                        activeMusicArchetypeId = activeMusicArchetypeId,
                        activeMusicSummary = activeMusicSummary,
                        initialMusicPrompt = musicForgePrompt,
                        cameraZoom = cameraZoom,
                        playfieldTextScale = playfieldTextScale,
                        isGoogleAuthenticated = isGoogleAuthenticated,
                        onToggleSfx = {
                          sfxEnabled = !sfxEnabled
                          sfx.enabled = sfxEnabled
                          localPrefs.setSfxEnabled(sfxEnabled)
                        },
                        onToggleMusic = {
                          musicEnabled = !musicEnabled
                          localPrefs.setMusicEnabled(musicEnabled)
                          sfx.setMusicEnabled(musicEnabled)
                        },
                        onChangeMusicBpm = { newBpm ->
                          val clamped = newBpm.coerceIn(60, 180)
                          musicBpm = clamped
                          sfx.setMusicBpmLive(clamped)
                          activeMusicSummary = sfx.currentMusicRecipe.customPromptSummary
                          localPrefs.setMusicBpm(clamped)
                        },
                        onChangeMusicVolume = { newVol ->
                          val clamped = newVol.coerceIn(0f, 1f)
                          musicVolume = clamped
                          sfx.setMusicVolumeLive(clamped)
                          localPrefs.setMusicVolume(clamped)
                        },
                        onChangeSfxVolume = { newVol ->
                          val clamped = newVol.coerceIn(0f, 1f)
                          sfxVolume = clamped
                          sfx.setSfxVolumeLive(clamped)
                          localPrefs.setSfxVolume(clamped)
                        },
                        onSelectMusicArchetype = { archetype ->
                          sfx.applyMusicArchetype(archetype)
                          musicBpm = sfx.musicBpm
                          activeMusicArchetypeId = archetype.id
                          activeMusicSummary = sfx.currentMusicRecipe.customPromptSummary
                          musicForgePrompt = ""
                          localPrefs.setMusicArchetypeId(archetype.id)
                          localPrefs.setMusicForgePrompt("")
                          localPrefs.setMusicBpm(sfx.musicBpm)
                          if (!musicEnabled) {
                            musicEnabled = true
                            localPrefs.setMusicEnabled(true)
                            sfx.setMusicEnabled(true)
                          }
                        },
                        onForgeCustomMusicPrompt = { rawPrompt ->
                          val forged = sfx.forgeMusicFromPrompt(rawPrompt)
                          musicBpm = forged.bpm
                          activeMusicArchetypeId = forged.archetype.id
                          activeMusicSummary = forged.customPromptSummary
                          musicForgePrompt = rawPrompt.trim()
                          localPrefs.setMusicArchetypeId(forged.archetype.id)
                          localPrefs.setMusicForgePrompt(musicForgePrompt)
                          localPrefs.setMusicBpm(forged.bpm)
                          if (!musicEnabled) {
                            musicEnabled = true
                            localPrefs.setMusicEnabled(true)
                            sfx.setMusicEnabled(true)
                          }
                          sfx.playPrize()
                          toastMessage = "🎶 FORGED TRACK: ${forged.customPromptSummary}"
                          toastVersion++
                        },
                        onChangeCameraZoom = { newZoom ->
                          val clamped = newZoom.coerceIn(0.5f, 1.5f)
                          cameraZoom = clamped
                          engine.cameraZoom = clamped
                          localPrefs.setCameraZoom(clamped)
                        },
                        onChangePlayfieldTextScale = { newScale ->
                          val clamped = newScale.coerceIn(0.75f, 1.75f)
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
                          localPrefs.setGuestBypassEnabled(false)
                          signOut(credentialManager = credentialManager, scope = scope)
                        },
                        onCloseModal = {
                          activeModal = if (!engine.hasJoinedMatch) ActiveModal.NEXUS else ActiveModal.NONE
                        },
                        onOpenChangelog = {
                          showFullChangelogModal = true
                        },
                      )
                    }

                    ActiveModal.CALIBRATE -> {
                      RiftCalibrationDialog(
                        engine = engine,
                        onDismiss = {
                          activeModal = if (!engine.hasJoinedMatch) ActiveModal.NEXUS else ActiveModal.NONE
                        },
                        onSyncToAllTesters = {
                          val customGameId = engine.currentCustomGame?.instanceId ?: "global_custom"
                          val json = RiftCalibrationState.exportGameplaySyncPayloadJson()
                          viewModel.broadcastCalibrationToCustomGame(customGameId, callsign, json)
                          toastMessage = "📡 CUSTOM GAME CALIBRATION SYNCED TO ALL TESTERS"
                          toastVersion++
                        },
                        onEnterHudDragArenaMode = {
                          RiftCalibrationState.isUiUnlockedForDragging = true
                          RiftCalibrationState.isCalibrationModalOpen = true
                          activeModal = ActiveModal.NONE
                        },
                        onReturnToNexusCalibration = {
                          RiftCalibrationState.isUiUnlockedForDragging = false
                          RiftCalibrationState.isCalibrationModalOpen = false
                          activeModal = ActiveModal.CALIBRATE
                        },
                      )
                    }

                    else -> Unit
                  }
                }
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
                engine.isInLobbyStaging = !engine.hasJoinedMatch
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
                toastMessage =
                  if (enabled) {
                    "⚙️ STARTUP PREFERENCE: OPEN NEXUS COMMAND HUB ON LOGIN"
                  } else {
                    "🛰️ STARTUP PREFERENCE: ORBIT SECTOR UNTIL DEPLOY"
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
                activeModal = ActiveModal.NONE
              },
              onLeaveMatch = {
                engine.leaveActiveMatchToNexusOrbit()
                toastMessage = "🛰️ LEFT MATCH // ORBITING SECTOR"
                toastVersion++
                frameTick++
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
                    if (it == "GUEST" || it == "RFT" || it.isEmpty()) callsign.take(6) else it
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
              onOpenSyndicateDirectory = {
                syndicateStatusBanner = null
                activeModal = ActiveModal.SYNDICATES
              },
              onOpenHangar = {
                activeModal = ActiveModal.HANGAR
              },
              onOpenProgression = {
                previousModalBeforeStore = activeModal
                activeModal = ActiveModal.RIFT_SHARDS_STORE
              },
              onOpenDossier = {
                activeModal = ActiveModal.DOSSIER
              },
              onOpenCodex = {
                activeModal = ActiveModal.CODEX
              },
              onOpenSettings = {
                activeModal = ActiveModal.SETTINGS
              },
              onOpenCalibration = {
                RiftCalibrationState.isUiUnlockedForDragging = false
                RiftCalibrationState.isCalibrationModalOpen = false
                activeModal = ActiveModal.CALIBRATE
              },
              onOpenHangarForShip = { targetShipId ->
                val clamped = targetShipId.coerceIn(1, 10)
                selectedShipId = clamped
                localPrefs.setPreferredShipId(clamped)
                engine.switchLocalPlayerShip(clamped, forceFromHangar = true)
                engine.onStatsUpdated()
                val shipTitle =
                  RiftCodex.getAllNineShips()
                    .find { it.id == clamped }
                    ?.name
                    ?.replace(Regex("^[0-9]+-"), "")
                    ?.uppercase(Locale.US) ?: "RIFT FRAME"
                toastMessage = "🚀 EQUIPPED $shipTitle FOR SECTOR DEPLOYMENT"
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
                  localPrefs.setSyndicateCustomChannels(
                    syndicateTag,
                    currentChannels + rawChannel,
                  )
                  syndicateChannelsRefreshKey++
                  toastMessage = "✓ CREATED SYNDICATE CHANNEL IN [$syndicateTag]"
                  toastVersion++
                }
              },
              onDeleteSyndicateChannel = { rawChannel ->
                val currentChannels = localPrefs.getSyndicateCustomChannels(syndicateTag)
                val filtered =
                  currentChannels.filterNot { it.equals(rawChannel, ignoreCase = true) }
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
              engine = engine,
              initialLayoutMode = nexusInitialLayoutMode,
              activeModuleKey = activeModuleKey,
              onSelectNexusLobby = {
                unreadCommsCount = 0
                activeModal = ActiveModal.NEXUS
              },
              isCalibrationLivePeeking =
                activeModal == ActiveModal.CALIBRATE && RiftCalibrationState.isDraggingAnySlider,
              embeddedModuleContent = embeddedModuleContent,
            )
          }

          if (showFullChangelogModal) {
            Box(modifier = Modifier.fillMaxSize().zIndex(30f)) {
              RiftChangelogModal(
                lastSeenBuild = localPrefs.getLastSeenChangelogBuild(),
                onAcknowledgeAndContinue = {
                  localPrefs.setLastSeenChangelogBuild(CURRENT_RIFT_BUILD_VERSION)
                  showFullChangelogModal = false
                },
              )
            }
          }

          // Mode A: Floating Live HUD Drag Pill when unlocked directly over the Arena
          if (isLiveHudDragPillOpen) {
            Box(modifier = Modifier.fillMaxSize().zIndex(35f)) {
              RiftCalibrationDialog(
                engine = engine,
                onDismiss = {
                  RiftCalibrationState.isUiUnlockedForDragging = false
                  RiftCalibrationState.isCalibrationModalOpen = false
                },
                onSyncToAllTesters = {
                  val customGameId = engine.currentCustomGame?.instanceId ?: "global_custom"
                  val json = RiftCalibrationState.exportGameplaySyncPayloadJson()
                  viewModel.broadcastCalibrationToCustomGame(customGameId, callsign, json)
                  toastMessage = "📡 CUSTOM GAME CALIBRATION SYNCED TO ALL TESTERS"
                  toastVersion++
                },
                onReturnToNexusCalibration = {
                  RiftCalibrationState.isUiUnlockedForDragging = false
                  RiftCalibrationState.isCalibrationModalOpen = false
                  activeModal = ActiveModal.CALIBRATE
                },
              )
            }
          }

          if (needsInitialSwornCallsign) {
            Box(
              modifier =
                Modifier.fillMaxSize()
                  .zIndex(40f)
                  .background(Color(0xCC020611))
                  .clickable(enabled = false) {}
                  .padding(24.dp)
                  .testTag("sworn_pilot_initial_callsign_modal"),
              contentAlignment = Alignment.Center,
            ) {
              Surface(
                modifier =
                  Modifier.widthIn(max = 460.dp)
                    .fillMaxWidth()
                    .border(1.5.dp, Color(0xFF00F5D4), RoundedCornerShape(16.dp)),
                color = Color(0xF2071120),
                shape = RoundedCornerShape(16.dp),
              ) {
                Column(
                  modifier = Modifier.padding(18.dp),
                  verticalArrangement = Arrangement.spacedBy(10.dp),
                  horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                  Text(
                    text = "★ CHOOSE YOUR SWORN PILOT CALLSIGN",
                    color = Color(0xFF00F5D4),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                  )
                  Text(
                    text =
                      "Welcome, Sworn Pilot (${localPrefs.getShortPilotTag(currentUserId)}). Choose your permanent arena callsign (3–14 chars). Your first callsign change in Dossier is also free.",
                    color = Color(0xFFCBD5E1),
                    fontSize = 11.sp,
                  )
                  Box(
                    modifier =
                      Modifier.fillMaxWidth()
                        .height(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF040A14))
                        .border(1.dp, Color(0xFF38BDF8), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.CenterStart,
                  ) {
                    BasicTextField(
                      value = initialSwornCallsignDraft,
                      onValueChange = { raw ->
                        initialSwornCallsignDraft =
                          raw
                            .uppercase()
                            .filter { it.isLetterOrDigit() || it == '_' || it == '-' }
                            .take(14)
                      },
                      singleLine = true,
                      cursorBrush = SolidColor(Color(0xFF00F5D4)),
                      textStyle =
                        TextStyle(
                          color = Color.White,
                          fontSize = 14.sp,
                          fontWeight = FontWeight.Bold,
                          fontFamily = FontFamily.Monospace,
                        ),
                      modifier =
                        Modifier.fillMaxWidth().testTag("sworn_pilot_initial_callsign_input"),
                    )
                  }
                  Button(
                    onClick = {
                      val saved = localPrefs.setCallsign(initialSwornCallsignDraft, currentUserId)
                      localPrefs.setHasChosenInitialCallsign(true, currentUserId)
                      callsign = saved
                      engine.localPlayer.callsign = saved
                      needsInitialSwornCallsign = false
                      engine.onStatsUpdated()
                      toastMessage = "★ SWORN PILOT [$saved] REGISTERED & SYNCED"
                      toastVersion++
                    },
                    enabled = initialSwornCallsignDraft.trim().length in 3..14,
                    colors =
                      ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0E7490),
                        contentColor = Color.White,
                      ),
                    border = BorderStroke(1.dp, Color(0xFF00F5D4)),
                    shape = RoundedCornerShape(10.dp),
                    modifier =
                      Modifier.fillMaxWidth()
                        .height(42.dp)
                        .testTag("sworn_pilot_initial_callsign_confirm"),
                  ) {
                    Text(
                      text = "✅ CONFIRM SWORN PILOT CALLSIGN",
                      fontSize = 11.5.sp,
                      fontWeight = FontWeight.Black,
                      fontFamily = FontFamily.Monospace,
                    )
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
