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
import androidx.compose.ui.platform.LocalConfiguration
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.FriendPilotEntry
import com.example.data.LivePilotState
import com.example.data.LocalPilotPrefs
import com.example.data.PilotAchievement
import com.example.data.RiftBillingManager
import com.example.data.RiftRepository
import com.example.data.SyndicateChatMessage
import com.example.data.UgcModeration
import com.example.engine.CustomGameConfig
import com.example.engine.CustomHullVisualRecipe
import com.example.engine.CustomShipLoadoutSlot
import com.example.engine.MusicSynthArchetype
import com.example.engine.RankedGameMode
import com.example.engine.RiftArenaView
import com.example.engine.RiftCalibrationState
import com.example.engine.RiftCodex
import com.example.engine.RiftGameEngine
import com.example.engine.RiftNexusKnowledge
import com.example.engine.RiftProgressionCatalog
import com.example.engine.RiftSoundSynthesizer
import com.example.engine.RiftSpriteBaker
import com.example.engine.RiftVisualForgeState
import com.example.engine.SyndicateCrestRecipe
import com.example.ui.ArchiveCodexSubDeck
import com.example.ui.ArmadaFleetModal
import com.example.ui.AuthScreen
import com.example.ui.CURRENT_RIFT_BUILD_VERSION
import com.example.ui.CodexModal
import com.example.ui.DossierModal
import com.example.ui.HangarModal
import com.example.ui.NexusAiConsoleModal
import com.example.ui.NexusAiViewModel
import com.example.ui.NexusCoreNode
import com.example.ui.NexusForgeModal
import com.example.ui.NexusForgeTab
import com.example.ui.NexusHubLayoutMode
import com.example.ui.NexusStreamState
import com.example.ui.OverwatchRiftInstance
import com.example.ui.RiftCalibrationDialog
import com.example.ui.RiftChangelogModal
import com.example.ui.RiftHudOverlay
import com.example.ui.RiftShardsStoreModal
import com.example.ui.RiftViewModel
import com.example.ui.SettingsModal
import com.example.ui.SyndicateCitadelSubDeck
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
import org.json.JSONObject

private sealed interface StagedRiftLaunch {
  data class Ranked(val mode: RankedGameMode, val teamSizeBracket: Int) : StagedRiftLaunch

  data class NewCustom(val config: CustomGameConfig) : StagedRiftLaunch

  data class ExistingCustom(val instanceId: String) : StagedRiftLaunch
}

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

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    configureAppCheck(this, intent)
    enableEdgeToEdge()
    hideSystemBars()
    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE

    setContent {
      MyApplicationTheme {
        AppNavigation()
      }
    }
  }

  override fun onResume() {
    super.onResume()
    RiftSoundSynthesizer.notifyAppForegroundState(true)
    hideSystemBars()
  }

  override fun onPause() {
    RiftSoundSynthesizer.notifyAppForegroundState(false)
    super.onPause()
  }

  override fun onStop() {
    RiftSoundSynthesizer.notifyAppForegroundState(false)
    super.onStop()
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
  var drifterSessionEpoch by remember { mutableIntStateOf(0) }
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
          drifterSessionEpoch++
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

      key("$uid-${user != null}-$drifterSessionEpoch") {
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
  val configuration = LocalConfiguration.current
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
  val allOnlineHumansState by viewModel.allOnlineHumansState.collectAsStateWithLifecycle()
  val cloudSyndicatesState by viewModel.cloudSyndicatesState.collectAsStateWithLifecycle()
  val sectorCommsState by viewModel.sectorCommsState.collectAsStateWithLifecycle()
  val latestCalibBroadcast by viewModel.latestCalibrationBroadcast.collectAsStateWithLifecycle()

  val nexusViewModel: NexusAiViewModel = viewModel(key = "nexus_$currentUserId")
  val nexusMessages by nexusViewModel.messages.collectAsStateWithLifecycle()
  val nexusStreamState by nexusViewModel.streamState.collectAsStateWithLifecycle()
  val isNexusStreaming =
    nexusStreamState is NexusStreamState.Loading || nexusStreamState is NexusStreamState.Streaming

  val lifecycleOwner = LocalLifecycleOwner.current
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

  val initialStartInPracticeRift = remember(currentUserId, isGoogleAuthenticated) {
    if (!isGoogleAuthenticated || localPrefs.isGuestBypassEnabled()) {
      true
    } else {
      val hasLocalCareerHistory =
        localPrefs.getAllTimeKills() > 0L ||
          localPrefs.getPvpKills() > 0L ||
          localPrefs.getConvergencesWon() > 0L ||
          localPrefs.getPeakResonance(currentUserId) > 0
      if (hasLocalCareerHistory) {
        localPrefs.setCompletedSwornPracticeTutorial(true, currentUserId)
        false
      } else if (!localPrefs.hasCompletedSwornPracticeTutorial(currentUserId)) {
        localPrefs.setCompletedSwornPracticeTutorial(true, currentUserId)
        true
      } else {
        false
      }
    }
  }
  var autoStartedSwornPracticePendingCloudCheck by remember(currentUserId, isGoogleAuthenticated) {
    mutableStateOf(
      isGoogleAuthenticated && !localPrefs.isGuestBypassEnabled() && initialStartInPracticeRift
    )
  }

  var currentFps by remember { mutableIntStateOf(60) }
  var frameTick by remember { mutableIntStateOf(0) }
  var toastMessage by remember {
    mutableStateOf<String?>(
      if (initialStartInPracticeRift) {
        "PRACTICE RIFT INITIALIZED // SOLO FLIGHT BRIEFING"
      } else {
        "NEXUS CORE ONLINE // SELECT RIFT TO DEPLOY"
      }
    )
  }
  var toastVersion by remember { mutableIntStateOf(0) }
  var unreadCommsCount by remember { mutableIntStateOf(0) }

  var bootToNexusHub by remember { mutableStateOf(true) }
  var isNexusShellOpen by remember { mutableStateOf(!initialStartInPracticeRift) }
  var activeNexusNode by remember { mutableStateOf(NexusCoreNode.ORBITAL_HUB) }
  var syndicateSubDeck by remember { mutableStateOf(SyndicateCitadelSubDeck.SYNDICATES_WARS) }
  var archiveSubDeck by remember { mutableStateOf(ArchiveCodexSubDeck.PILOT_DOSSIER) }
  var previousNodeBeforeConduit by remember { mutableStateOf(NexusCoreNode.ORBITAL_HUB) }
  var nexusInitialLayoutMode by remember { mutableStateOf(NexusHubLayoutMode.DECK_ONLY) }
  var forgeInitialTab by remember { mutableStateOf(NexusForgeTab.SHIP_COSMETICS) }
  var isForgeFullScreenStage by remember { mutableStateOf(false) }
  var showFullChangelogModal by remember { mutableStateOf(false) }
  var pendingStagedLaunch by remember { mutableStateOf<StagedRiftLaunch?>(null) }
  var favoriteOverwatchKey by remember {
    mutableStateOf(localPrefs.getFavoriteOverwatchRiftKey())
  }
  var userSelectedOverwatchKey by remember { mutableStateOf("") }

  var callsign by remember(currentUserId, isGoogleAuthenticated) {
    mutableStateOf(
      if (isGoogleAuthenticated) localPrefs.getCallsign(currentUserId) else localPrefs.getCallsign()
    )
  }
  var needsInitialSwornCallsign by remember(currentUserId, isGoogleAuthenticated) {
    mutableStateOf(false)
  }
  var initialSwornCallsignDraft by remember(callsign) {
    mutableStateOf(if (callsign.equals("COMMANDER", ignoreCase = true)) "" else callsign)
  }
  var syndicateTag by remember(currentUserId, isGoogleAuthenticated) {
    mutableStateOf(localPrefs.getSyndicateTag())
  }
  var syndicateName by remember(currentUserId, isGoogleAuthenticated) {
    mutableStateOf(localPrefs.getSyndicateName())
  }
  var selectedShipId by remember {
    mutableIntStateOf(localPrefs.getPreferredShipId().coerceIn(1, 10))
  }
  var selectedMode by remember {
    mutableStateOf(localPrefs.getPreferredMode().ifBlank { "SECTOR_CONVERGENCE" })
  }
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
  var directivesRefreshKey by remember { mutableIntStateOf(0) }
  var cosmeticsRefreshKey by remember { mutableIntStateOf(0) }
  var lastAppliedCloudCrestJson by remember { mutableStateOf("") }
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

  val chatMessages = remember { mutableStateListOf<SyndicateChatMessage>() }

  fun syncEquippedCosmeticsToEngine(targetEngine: RiftGameEngine) {
    val killFin = localPrefs.getEquippedCosmeticId("KILL_FINISHER", currentUserId)
    val hullTrim = localPrefs.getEquippedCosmeticId("HULL_TRIM", currentUserId).coerceIn(0, 3)
    val warpSig = localPrefs.getEquippedCosmeticId("WARP_SIGNATURE", currentUserId)
    val hvtHalo = localPrefs.getEquippedCosmeticId("HVT_HALO", currentUserId)
    targetEngine.localPlayer.equippedKillFinisher = killFin
    targetEngine.localPlayer.equippedWarpStyle = warpSig
    targetEngine.localPlayer.equippedHvtHaloStyle = hvtHalo
    if (RiftSpriteBaker.activeHullTrimStyle != hullTrim) {
      RiftSpriteBaker.activeHullTrimStyle = hullTrim
      RiftCodex.updateActiveCustomShipSlot(activeCustomSlot)
    }
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
      riftShards = RiftBillingManager.effectiveShardBalance(localPrefs.getRiftShards(currentUserId))
      localPrefs.setRiftShards(riftShards, currentUserId)
      liveResonance = localPrefs.getLiveResonance(currentUserId)
      peakResonance = localPrefs.getPeakResonance(currentUserId)
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
      syncEquippedCosmeticsToEngine(this)
      localPlayer.bounty = getLocalStartingBounty()
      if (initialStartInPracticeRift) {
        enterEphemeralPracticeRift(startTutorial = true)
      } else {
        leaveActiveMatchToNexusOrbit()
      }
    }
  }

  fun openNexusNode(node: NexusCoreNode) {
    if (engine.isEphemeralPracticeRift) {
      engine.closeEphemeralPracticeRift()
    }
    if (node == NexusCoreNode.SUBSPACE_COMMS) {
      unreadCommsCount = 0
    }
    if (node != NexusCoreNode.STAR_FORGE) {
      isForgeFullScreenStage = false
    }
    if (node == NexusCoreNode.CONVERGENCE_GATE) {
      pendingStagedLaunch = null
    }
    activeNexusNode = node
    isNexusShellOpen = true
  }

  DisposableEffect(engine, viewModel) {
    syncEquippedCosmeticsToEngine(engine)
    engine.onDailyDirectiveEvent = { metric, amount, isAbsolutePeak ->
      if (!engine.isEphemeralPracticeRift) {
        localPrefs.recordDailyDirectiveProgress(metric, amount, isAbsolutePeak, currentUserId)
        directivesRefreshKey++
      }
    }
    engine.onStatsUpdated = {
      if (engine.isEphemeralPracticeRift) {
        frameTick++
      } else {
      engine.riftShards = RiftBillingManager.effectiveShardBalance(engine.riftShards)
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
      localPrefs.setLiveResonance(engine.liveResonance, currentUserId)
      localPrefs.setPeakResonance(engine.peakResonance, currentUserId)
      engine.peakResonance = localPrefs.getPeakResonance(currentUserId)
      slot9BUnlocked = localPrefs.isCustomSlotUnlocked("Frame B", currentUserId)
      slot9CUnlocked = localPrefs.isCustomSlotUnlocked("Frame C", currentUserId)
      slot9DUnlocked = localPrefs.isCustomSlotUnlocked("Frame D", currentUserId)
      slot9EUnlocked = localPrefs.isCustomSlotUnlocked("Frame E", currentUserId)
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
    }
    engine.onLifeRunEnded = { finalizedSnapshot, hadPvp ->
      if (!engine.isEphemeralPracticeRift) {
        localPrefs.maybeRecordPeakBountySnapshot(
          snapshot = finalizedSnapshot,
          hadPvp = hadPvp,
          isFinalizedLifeEnd = true,
          uid = currentUserId,
        )
        allTimePeakSnapshot = localPrefs.getAllTimePeakBountySnapshot(currentUserId)
        pvpPeakSnapshot = localPrefs.getPvpPeakBountySnapshot(currentUserId)
      }
    }
    engine.onSyndicateContribution = { killsDelta, currentBounty, winsDelta ->
      if (!engine.isEphemeralPracticeRift) {
        localPrefs.recordSyndicateContribution(
          addKills = killsDelta,
          currentBounty = currentBounty,
          addPublicWins = winsDelta,
          uid = currentUserId,
        )
        syndicateHistory = localPrefs.getSyndicateServiceHistory(currentUserId)
      }
    }
    engine.onSyndicateWarFinished = { won, delta ->
      localPrefs.recordSyndicateWarOutcome(syndicateTag, won, delta)
      syndicateChannelsRefreshKey++
    }
    onDispose {
      sfx.release()
    }
  }

  LaunchedEffect(Unit) {
    RiftCalibrationState.loadPersistedLayout(context, engine)
    val savedLocalCrest = localPrefs.getSyndicateCrestJson(syndicateTag).orEmpty()
    if (savedLocalCrest.isNotBlank()) {
      runCatching {
        val parsed = SyndicateCrestRecipe.fromJson(JSONObject(savedLocalCrest))
        RiftVisualForgeState.updateSyndicateCrest(parsed, null)
        lastAppliedCloudCrestJson = savedLocalCrest
      }
    }
    musicBpm = sfx.musicBpm
    activeMusicArchetypeId = sfx.currentMusicRecipe.archetype.id
    activeMusicSummary = sfx.currentMusicRecipe.customPromptSummary
    RiftCalibrationState.applyCalibrationChangesLive(engine, null)
  }

  LaunchedEffect(isNexusShellOpen, activeNexusNode) {
    if (!isNexusShellOpen || activeNexusNode != NexusCoreNode.STAR_FORGE) {
      isForgeFullScreenStage = false
    }
  }

  LaunchedEffect(toastVersion) {
    if (toastMessage != null) {
      delay(1500L)
      toastMessage = null
    }
  }

  // Observe live Syndicate Crest, custom channels & guest passes from Firestore
  LaunchedEffect(syndicateTag, isGoogleAuthenticated) {
    if (!isGoogleAuthenticated) return@LaunchedEffect
    val cleanSyn = RiftGameEngine.cleanTag(syndicateTag)
    if (cleanSyn.isEmpty() || cleanSyn in setOf("GUEST", "FREE", "RFT", "BOT", "SOLO")) {
      return@LaunchedEffect
    }
    viewModel.observeSyndicateMeta(cleanSyn).collect { meta ->
      if (meta != null) {
        localPrefs.mergeCloudSyndicateMeta(meta)
        if (meta.crestJson.isNotBlank() && meta.crestJson != lastAppliedCloudCrestJson) {
          runCatching {
            val parsed = SyndicateCrestRecipe.fromJson(JSONObject(meta.crestJson))
            lastAppliedCloudCrestJson = meta.crestJson
            RiftVisualForgeState.updateSyndicateCrest(parsed, context)
          }
        }
        syndicateChannelsRefreshKey++
      }
    }
  }

  // Push local Syndicate Crest, channel list & guest passes to Firestore when updated
  val activeCrestRecipe = RiftVisualForgeState.activeSyndicateCrestRecipe
  LaunchedEffect(activeCrestRecipe, syndicateChannelsRefreshKey, syndicateTag, isGoogleAuthenticated) {
    val cleanSyn = RiftGameEngine.cleanTag(syndicateTag)
    if (cleanSyn.isEmpty() || cleanSyn in setOf("GUEST", "FREE", "RFT", "BOT", "SOLO")) {
      return@LaunchedEffect
    }
    val crestJson = activeCrestRecipe.toJson().toString()
    localPrefs.setSyndicateCrestJson(cleanSyn, crestJson)
    if (isGoogleAuthenticated) {
      viewModel.syncSyndicateMetaToCloud(
        tag = cleanSyn,
        crestJson = crestJson,
        channels = localPrefs.getSyndicateCustomChannels(cleanSyn),
        invitedGuests = localPrefs.getSyndicateInvitedGuests(cleanSyn),
      )
    }
  }

  // Monotonic max(local, cloud) merge for Sworn Pilot career stats, callsign & Syndicate tag across devices/updates
  LaunchedEffect(dossierState, currentUserId, isGoogleAuthenticated) {
    fun isValidSwornCallsign(raw: String): Boolean {
      val c = raw.trim().uppercase(Locale.US)
      return c.length >= 3 &&
        c != "COMMANDER" &&
        !c.startsWith("RFT-") &&
        !c.startsWith("#RFT") &&
        !c.contains("[BOT]")
    }

    if (isGoogleAuthenticated && !localPrefs.isGuestBypassEnabled()) {
      val scrubbedTag = localPrefs.getSyndicateTag()
      val scrubbedName = localPrefs.getSyndicateName()
      if (syndicateTag != scrubbedTag || engine.localPlayer.syndicateTag != scrubbedTag) {
        syndicateTag = scrubbedTag
        syndicateName = scrubbedName
        engine.localPlayer.syndicateTag = scrubbedTag
        engine.syncLocalArmadaSelfSlot()
      }
      if (isValidSwornCallsign(callsign)) {
        localPrefs.setHasChosenInitialCallsign(true, currentUserId)
        needsInitialSwornCallsign = false
      }
    } else {
      needsInitialSwornCallsign = false
    }

    if (dossierState is UiState.Loading) {
      return@LaunchedEffect
    }

    val cloudDossier = (dossierState as? UiState.Success)?.data
    if (cloudDossier != null) {
      val cloudHasPriorCareer =
        cloudDossier.allTimeKills > 0L ||
          cloudDossier.pvpKills > 0L ||
          cloudDossier.convergencesWon > 0L
      if (
        cloudHasPriorCareer &&
          isGoogleAuthenticated &&
          !localPrefs.isGuestBypassEnabled() &&
          currentUserId.isNotBlank()
      ) {
        localPrefs.setCompletedSwornPracticeTutorial(true, currentUserId)
        if (
          autoStartedSwornPracticePendingCloudCheck &&
            engine.isEphemeralPracticeRift &&
            engine.practiceTutorialStepIndex == 0
        ) {
          autoStartedSwornPracticePendingCloudCheck = false
          openNexusNode(NexusCoreNode.ORBITAL_HUB)
        }
      } else {
        autoStartedSwornPracticePendingCloudCheck = false
      }
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

      val cloudDerivedPeakResonance =
        ((cloudDossier.pvpBountyPeak - 100L).coerceAtLeast(0L) +
            cloudDossier.pvpKills * 12L +
            cloudDossier.convergencesWon * 35L)
          .coerceIn(0L, 50_000L)
          .toInt()
      if (cloudDerivedPeakResonance > engine.peakResonance) {
        engine.peakResonance = cloudDerivedPeakResonance
        localPrefs.setPeakResonance(cloudDerivedPeakResonance, currentUserId)
        slot9BUnlocked = localPrefs.isCustomSlotUnlocked("Frame B", currentUserId)
        slot9CUnlocked = localPrefs.isCustomSlotUnlocked("Frame C", currentUserId)
        slot9DUnlocked = localPrefs.isCustomSlotUnlocked("Frame D", currentUserId)
        slot9EUnlocked = localPrefs.isCustomSlotUnlocked("Frame E", currentUserId)
      }

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

      if (isGoogleAuthenticated && !localPrefs.isGuestBypassEnabled() && currentUserId.isNotBlank()) {
        val cloudSynTag = cloudDossier.syndicateTag.trim().uppercase(Locale.US)
        val nonPlayerTags =
          setOf("", "RFT", "GUEST", "FREE", "SOLO", "UNAFFILIATED", "NONE", "BOT") +
            RiftGameEngine.LEGACY_FOUNDRY_TAGS
        if (cloudSynTag.length in 1..4 && cloudSynTag !in nonPlayerTags && syndicateTag.isBlank()) {
          val resolvedSynName =
            cloudDossier.syndicateName
              .trim()
              .takeIf { it.isNotBlank() && !it.contains("Guest", ignoreCase = true) }
              ?: "$cloudSynTag Syndicate"
          localPrefs.joinOfficialSyndicate(
            tag = cloudSynTag,
            name = resolvedSynName,
            apply15DayLock = false,
          )
          syndicateTag = localPrefs.getSyndicateTag()
          syndicateName = localPrefs.getSyndicateName()
          engine.localPlayer.syndicateTag = syndicateTag
          engine.syncLocalArmadaSelfSlot()
          syndicateHistory = localPrefs.getSyndicateServiceHistory(currentUserId)
        }

        val cloudCallsign = cloudDossier.callsign.trim().uppercase(Locale.US)
        val cloudHasChosenName = isValidSwornCallsign(cloudCallsign)
        val localHasChosenName = isValidSwornCallsign(callsign)

        if (cloudHasChosenName) {
          localPrefs.setHasChosenInitialCallsign(true, currentUserId)
          needsInitialSwornCallsign = false
          if (!localHasChosenName) {
            val adopted = localPrefs.setCallsign(cloudCallsign, currentUserId)
            callsign = adopted
            initialSwornCallsignDraft = adopted
            engine.localPlayer.callsign = adopted
            engine.syncLocalArmadaSelfSlot()
          }
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

    if (isGoogleAuthenticated && !localPrefs.isGuestBypassEnabled() && currentUserId.isNotBlank()) {
      val hasValidNameNow =
        isValidSwornCallsign(callsign) || localPrefs.hasChosenInitialCallsign(currentUserId)
      needsInitialSwornCallsign = !hasValidNameNow
    } else {
      needsInitialSwornCallsign = false
    }
  }

  val mergedLivePilots =
    remember(sectorPilotsState, allOnlineHumansState, blockedPilotUids) {
      val byUid = linkedMapOf<String, LivePilotState>()
      val sectorList = (sectorPilotsState as? UiState.Success)?.data.orEmpty()
      for (pilot in sectorList + allOnlineHumansState) {
        val pUid = pilot.userId.ifEmpty { pilot.id }
        if (pUid.isBlank() || pUid in blockedPilotUids) continue
        val prev = byUid[pUid]
        val prevMs = prev?.updatedAt?.toDate()?.time ?: 0L
        val curMs = pilot.updatedAt?.toDate()?.time ?: 0L
        if (prev == null || curMs >= prevMs) {
          byUid[pUid] = pilot
        }
      }
      byUid.values.toList()
    }

  fun pushLivePilotHeartbeatNow() {
    if (!isGoogleAuthenticated) return
    val p = engine.localPlayer
    val safeCallsign =
      p.callsign.trim().ifBlank { callsign.trim() }.ifBlank { "PILOT" }.take(24).let {
        if (it.length < 2) "PILOT" else it
      }
    viewModel.syncNativeLiveState(
      sectorId = engine.activeMatchSectorTag(),
      callsign = safeCallsign,
      syndicateTag = p.syndicateTag.take(12),
      armadaName = p.armadaName.take(12),
      shipId = p.shipId.coerceIn(1, 10),
      freq = p.freq.coerceIn(0, 99),
      x = p.x.coerceIn(-99000f, 99000f),
      y = p.y.coerceIn(-99000f, 99000f),
      vx = p.vx.coerceIn(-4900f, 4900f),
      vy = p.vy.coerceIn(-4900f, 4900f),
      angle = p.angle.coerceIn(-99f, 99f),
      weaponAngle = p.weaponAngle.coerceIn(-99f, 99f),
      isFiringPrimary =
        engine.hasJoinedMatch && engine.fireStickActive && !engine.isCancelActive && p.canFire,
      isAfterburning = engine.hasJoinedMatch && p.afterburning,
      isCloaked = engine.hasJoinedMatch && p.isCloaked,
      flux = p.flux.coerceIn(0f, 10000f),
      maxFlux = p.maxFlux.coerceIn(100f, 10000f),
      bounty = p.bounty.coerceAtLeast(0),
      liveResonance = engine.liveResonance.coerceIn(0, 1000000),
    )
  }

  LaunchedEffect(mergedLivePilots) {
    engine.updateRemotePilots(mergedLivePilots, currentUserId)
  }

  LaunchedEffect(cloudSyndicatesState) {
    val cloudList = (cloudSyndicatesState as? UiState.Success)?.data ?: return@LaunchedEffect
    localPrefs.mergeCloudSyndicates(cloudList)
  }

  LaunchedEffect(sectorCommsState) {
    val remoteComms = (sectorCommsState as? UiState.Success)?.data ?: return@LaunchedEffect
    val existingIds = chatMessages.map { it.id }.toSet()
    var newIncomingCount = 0
    for (msg in remoteComms) {
      if (msg.id.isNotEmpty() && msg.id !in existingIds) {
        chatMessages.add(msg)
        val isSubspaceOpen = isNexusShellOpen && activeNexusNode == NexusCoreNode.SUBSPACE_COMMS
        if (msg.senderUid != currentUserId && !isSubspaceOpen) {
          newIncomingCount++
        }
      }
    }
    if (newIncomingCount > 0) {
      unreadCommsCount += newIncomingCount
    }
  }

  LaunchedEffect(currentUserId, isGoogleAuthenticated) {
    if (!isGoogleAuthenticated) return@LaunchedEffect
    pushLivePilotHeartbeatNow()
    while (true) {
      val intervalMs =
        if (engine.hasJoinedMatch && !engine.isEphemeralPracticeRift) 350L else 4000L
      delay(intervalMs)
      pushLivePilotHeartbeatNow()
    }
  }

  var arenaViewRef by remember { mutableStateOf<RiftArenaView?>(null) }
  val orderedNexusNodes = remember {
    listOf(
      NexusCoreNode.ORBITAL_HUB,
      NexusCoreNode.SUBSPACE_COMMS,
      NexusCoreNode.CONVERGENCE_GATE,
      NexusCoreNode.HANGAR_DECK,
      NexusCoreNode.RIFT_CONDUIT,
      NexusCoreNode.SYNDICATE_CITADEL,
      NexusCoreNode.ARCHIVE_CODEX,
      NexusCoreNode.HELM_SYSTEMS,
      NexusCoreNode.STAR_FORGE,
    )
  }

  LaunchedEffect(engine.currentCustomGame, engine.activeRankedMode) {
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
      toastMessage = "CALIBRATION SYNCED BY ${broadcast.authorCallsign.uppercase(Locale.US)}"
      toastVersion++
    }
  }

  LaunchedEffect(
    isNexusShellOpen,
    RiftCalibrationState.isCalibrationModalOpen,
    engine.hasJoinedMatch,
    frameTick,
  ) {
    val isInMenu =
      isNexusShellOpen ||
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
      forgeInitialTab = NexusForgeTab.DEV_STUDIO_STAGE
      openNexusNode(NexusCoreNode.STAR_FORGE)
    } else if (RiftCalibrationState.isCalibrationModalOpen) {
      RiftCalibrationState.isCalibrationModalOpen = false
    } else if (showFullChangelogModal) {
      showFullChangelogModal = false
    } else if (isNexusShellOpen && activeNexusNode == NexusCoreNode.STAR_FORGE && isForgeFullScreenStage) {
      isForgeFullScreenStage = false
    } else if (!isNexusShellOpen && engine.isEphemeralPracticeRift) {
      openNexusNode(NexusCoreNode.ORBITAL_HUB)
    } else if (isNexusShellOpen && activeNexusNode != NexusCoreNode.ORBITAL_HUB) {
      activeNexusNode = NexusCoreNode.ORBITAL_HUB
    } else if (isNexusShellOpen && activeNexusNode == NexusCoreNode.ORBITAL_HUB) {
      if (engine.hasJoinedMatch && !engine.isEphemeralPracticeRift) {
        isNexusShellOpen = false
      }
    } else {
      openNexusNode(NexusCoreNode.ORBITAL_HUB)
    }
  }

  val isLiveHudDragPillOpen =
    RiftCalibrationState.isUiUnlockedForDragging && RiftCalibrationState.isCalibrationModalOpen

  val isAnyModalCoveringArena =
    (isNexusShellOpen ||
      showFullChangelogModal ||
      needsInitialSwornCallsign) && !isLiveHudDragPillOpen

  val isHubOverwatchVisible =
    isNexusShellOpen &&
      activeNexusNode == NexusCoreNode.ORBITAL_HUB &&
      !showFullChangelogModal &&
      !needsInitialSwornCallsign &&
      !isLiveHudDragPillOpen

  val overwatchInstances =
    remember(mergedLivePilots, frameTick) {
      val cleanHumans = mergedLivePilots
      fun humanStatsForSector(sectorTag: String, fallbackSimResonance: Int): Pair<Int, Int> {
        val matching = cleanHumans.filter { it.sectorId.equals(sectorTag, ignoreCase = true) }
        if (matching.isEmpty()) return Pair(0, fallbackSimResonance)
        val combinedRes =
          matching.sumOf { p ->
            if (p.liveResonance > 0L) p.liveResonance.toInt()
            else (p.bounty.toInt() / 2).coerceAtLeast(180)
          }
        return Pair(matching.size, combinedRes)
      }

      val list = mutableListOf<OverwatchRiftInstance>()
      val rankedSeeds =
        listOf(
          Triple(RankedGameMode.SECTOR_CONVERGENCE, 101, 1420),
          Triple(RankedGameMode.FLUX_CORE, 202, 1080),
          Triple(RankedGameMode.RIFT_SKIRMISH, 303, 940),
          Triple(RankedGameMode.STASIS_BREACH, 404, 860),
          Triple(RankedGameMode.RIFT_CONTROL, 505, 1190),
          Triple(RankedGameMode.SYNDICATE_WARS, 606, 890),
        )
      rankedSeeds.forEachIndexed { idx, (mode, salt, simRes) ->
        val (pCount, cRes) = humanStatsForSector(mode.name, simRes)
        list.add(
          OverwatchRiftInstance(
            key = "${mode.name}_1",
            modeTitle = mode.title.uppercase(Locale.US),
            riftLabel = String.format(Locale.US, "RIFT #%02d", idx + 1),
            rankedMode = mode,
            customConfig = null,
            pilotCount = pCount,
            combinedResonance = cRes,
            seedSalt = salt,
          )
        )
      }
      // Secondary Sector Convergence instance (RIFT #07) so pilots can cycle multiple FFA Rifts
      list.add(
        OverwatchRiftInstance(
          key = "SECTOR_CONVERGENCE_2",
          modeTitle = RankedGameMode.SECTOR_CONVERGENCE.title.uppercase(Locale.US),
          riftLabel = "RIFT #07",
          rankedMode = RankedGameMode.SECTOR_CONVERGENCE,
          customConfig = null,
          pilotCount = 0,
          combinedResonance = 740,
          seedSalt = 707,
        )
      )
      // Public Custom Rifts (RIFT #08+)
      engine.activeCustomGames
        .filter { !it.instanceId.startsWith("priv_", ignoreCase = true) }
        .forEachIndexed { cIdx, cfg ->
          val sectorKey = "CUSTOM_${cfg.instanceId}"
          val simFallback = 620 + (kotlin.math.abs(cfg.instanceId.hashCode()) % 520)
          val (pCount, cRes) = humanStatsForSector(sectorKey, simFallback)
          list.add(
            OverwatchRiftInstance(
              key = sectorKey,
              modeTitle = "CUSTOM • ${cfg.baseMode.title.uppercase(Locale.US)}",
              riftLabel = String.format(Locale.US, "RIFT #%02d", 8 + cIdx),
              rankedMode = cfg.baseMode,
              customConfig = cfg,
              pilotCount = pCount,
              combinedResonance = cRes,
              seedSalt = cfg.instanceId.hashCode(),
            )
          )
        }
      list.toList()
    }

  val effectiveOverwatchInstance =
    remember(overwatchInstances, userSelectedOverwatchKey, favoriteOverwatchKey) {
      val manualMatch =
        if (userSelectedOverwatchKey.isNotBlank()) {
          overwatchInstances.firstOrNull { it.key == userSelectedOverwatchKey }
        } else {
          null
        }
      val favMatch =
        if (favoriteOverwatchKey.isNotBlank()) {
          overwatchInstances.firstOrNull { it.key == favoriteOverwatchKey }
        } else {
          null
        }
      when {
        manualMatch != null -> manualMatch
        favMatch != null -> favMatch
        else -> {
          val humanPopulated = overwatchInstances.filter { it.pilotCount > 0 }
          if (humanPopulated.isNotEmpty()) {
            humanPopulated.maxWithOrNull(
              compareBy<OverwatchRiftInstance> { it.combinedResonance }
                .thenBy { it.pilotCount }
            ) ?: overwatchInstances.first()
          } else {
            overwatchInstances.maxByOrNull { it.combinedResonance } ?: overwatchInstances.first()
          }
        }
      }
    }

  var overwatchZoomUi by remember { mutableFloatStateOf(1.0f) }

  DisposableEffect(lifecycleOwner, sfx) {
    val isCurrentlyResumed = lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
    RiftSoundSynthesizer.notifyAppForegroundState(isCurrentlyResumed)
    sfx.setAppForeground(isCurrentlyResumed)
    arenaViewRef?.onHostForegroundChanged(isCurrentlyResumed)

    val observer = LifecycleEventObserver { _, event ->
      when (event) {
        Lifecycle.Event.ON_RESUME -> {
          RiftSoundSynthesizer.notifyAppForegroundState(true)
          sfx.setAppForeground(true)
          arenaViewRef?.onHostForegroundChanged(true)
        }
        Lifecycle.Event.ON_PAUSE,
        Lifecycle.Event.ON_STOP,
        Lifecycle.Event.ON_DESTROY -> {
          RiftSoundSynthesizer.notifyAppForegroundState(false)
          sfx.setAppForeground(false)
          arenaViewRef?.onHostForegroundChanged(false)
        }
        else -> Unit
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose {
      lifecycleOwner.lifecycle.removeObserver(observer)
      sfx.setAppForeground(false)
    }
  }

  LaunchedEffect(effectiveOverwatchInstance.key) {
    overwatchZoomUi = 1.0f
    arenaViewRef?.resetOverwatchCamera()
  }

  LaunchedEffect(
    isHubOverwatchVisible,
    effectiveOverwatchInstance.key,
    engine.hasJoinedMatch,
  ) {
    arenaViewRef?.isHubOverwatchMode = isHubOverwatchVisible
    if (isHubOverwatchVisible && !engine.hasJoinedMatch) {
      engine.configureHubOverwatchInstance(
        instanceKey = effectiveOverwatchInstance.key,
        rankedMode = effectiveOverwatchInstance.rankedMode,
        customConfig = effectiveOverwatchInstance.customConfig,
        seedSalt = effectiveOverwatchInstance.seedSalt,
      )
      engine.updateRemotePilots(mergedLivePilots, currentUserId)
    } else if (!isHubOverwatchVisible) {
      engine.isHubOverwatchActive = false
      overwatchZoomUi = 1.0f
      arenaViewRef?.resetOverwatchCamera()
    }
  }

  Box(
    modifier =
      Modifier.fillMaxSize()
        .background(Color(0xFF02050B))
        .onPreviewKeyEvent { keyEvent ->
          val native = keyEvent.nativeKeyEvent
          if (native.action == KeyEvent.ACTION_DOWN && native.repeatCount == 0) {
            when (native.keyCode) {
              KeyEvent.KEYCODE_BUTTON_START -> {
                RiftCalibrationState.gamepadConnected = true
                if (isNexusShellOpen && activeNexusNode == NexusCoreNode.HANGAR_DECK) {
                  isNexusShellOpen = false
                } else {
                  openNexusNode(NexusCoreNode.HANGAR_DECK)
                }
                sfx.playMutedClick()
                true
              }

              KeyEvent.KEYCODE_BUTTON_SELECT -> {
                RiftCalibrationState.gamepadConnected = true
                if (RiftCalibrationState.isUiUnlockedForDragging) {
                  RiftCalibrationState.isUiUnlockedForDragging = false
                  RiftCalibrationState.isCalibrationModalOpen = false
                  forgeInitialTab = NexusForgeTab.DEV_STUDIO_STAGE
                  openNexusNode(NexusCoreNode.STAR_FORGE)
                } else {
                  RiftCalibrationState.isCalibrationModalOpen = false
                  if (isNexusShellOpen && activeNexusNode == NexusCoreNode.STAR_FORGE) {
                    isNexusShellOpen = false
                  } else {
                    openNexusNode(NexusCoreNode.STAR_FORGE)
                  }
                }
                sfx.playMutedClick()
                true
              }

              KeyEvent.KEYCODE_BUTTON_L1 -> {
                if (isNexusShellOpen) {
                  RiftCalibrationState.gamepadConnected = true
                  val curIdx = orderedNexusNodes.indexOf(activeNexusNode)
                  val nextIdx = if (curIdx <= 0) orderedNexusNodes.lastIndex else curIdx - 1
                  openNexusNode(orderedNexusNodes[nextIdx])
                  sfx.playMutedClick()
                  true
                } else {
                  false
                }
              }

              KeyEvent.KEYCODE_BUTTON_R1 -> {
                if (isNexusShellOpen) {
                  RiftCalibrationState.gamepadConnected = true
                  val curIdx = orderedNexusNodes.indexOf(activeNexusNode)
                  val nextIdx =
                    if (curIdx < 0 || curIdx >= orderedNexusNodes.lastIndex) 0 else curIdx + 1
                  openNexusNode(orderedNexusNodes[nextIdx])
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
            if (isNexusShellOpen && activeNexusNode == NexusCoreNode.HANGAR_DECK) {
              isNexusShellOpen = false
            } else {
              openNexusNode(NexusCoreNode.HANGAR_DECK)
            }
            sfx.playMutedClick()
          },
          onGamepadToggleCalibration = {
            if (RiftCalibrationState.isUiUnlockedForDragging) {
              RiftCalibrationState.isUiUnlockedForDragging = false
              RiftCalibrationState.isCalibrationModalOpen = false
              forgeInitialTab = NexusForgeTab.DEV_STUDIO_STAGE
              openNexusNode(NexusCoreNode.STAR_FORGE)
            } else {
              RiftCalibrationState.isCalibrationModalOpen = false
              if (isNexusShellOpen && activeNexusNode == NexusCoreNode.STAR_FORGE) {
                isNexusShellOpen = false
              } else {
                openNexusNode(NexusCoreNode.STAR_FORGE)
              }
            }
            sfx.playMutedClick()
          },
          onGamepadOpenNexus = {
            openNexusNode(NexusCoreNode.ORBITAL_HUB)
            sfx.playMutedClick()
          },
        ).apply {
          layoutParams =
            ViewGroup.LayoutParams(
              ViewGroup.LayoutParams.MATCH_PARENT,
              ViewGroup.LayoutParams.MATCH_PARENT,
            )
          isModalOverlayOpen = isAnyModalCoveringArena
          isHubOverwatchMode = isHubOverwatchVisible
          arenaViewRef = this
        }
      },
      update = { view ->
        view.isModalOverlayOpen = isAnyModalCoveringArena
        view.isHubOverwatchMode = isHubOverwatchVisible
      },
    )

    // 2. Native Fullscreen HUD Overlay (1:1 Device Touch Ergonomics; hidden cleanly behind open modals)
    RiftHudOverlay(
      engine = engine,
      fps = currentFps,
      sfxEnabled = sfxEnabled,
      unreadCommsCount = unreadCommsCount,
      toastMessage = toastMessage,
      frameTick = frameTick,
      cameraZoom = cameraZoom,
      isModalOpen = isAnyModalCoveringArena,
      onOpenHangar = { openNexusNode(NexusCoreNode.HANGAR_DECK) },
      onOpenCodex = {
        archiveSubDeck = ArchiveCodexSubDeck.TACTICAL_CODEX
        openNexusNode(NexusCoreNode.ARCHIVE_CODEX)
      },
      onOpenSyndicateCommand = {
        syndicateStatusBanner = null
        syndicateSubDeck = SyndicateCitadelSubDeck.SYNDICATES_WARS
        openNexusNode(NexusCoreNode.SYNDICATE_CITADEL)
      },
      onOpenArmadaFleet = {
        armadaStatusBanner = null
        syndicateSubDeck = SyndicateCitadelSubDeck.STRIKE_WING
        openNexusNode(NexusCoreNode.SYNDICATE_CITADEL)
      },
      onOpenComms = {
        nexusInitialLayoutMode = NexusHubLayoutMode.SPLIT_COMMAND
        openNexusNode(NexusCoreNode.SUBSPACE_COMMS)
      },
      onOpenDossier = {
        archiveSubDeck = ArchiveCodexSubDeck.PILOT_DOSSIER
        openNexusNode(NexusCoreNode.ARCHIVE_CODEX)
      },
      onOpenProgression = {
        previousNodeBeforeConduit = NexusCoreNode.ORBITAL_HUB
        openNexusNode(NexusCoreNode.RIFT_CONDUIT)
      },
      onOpenSettings = { openNexusNode(NexusCoreNode.HELM_SYSTEMS) },
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
        toastMessage = String.format(Locale.US, "CAMERA FOV SET TO %.2fx", nextZoom)
        toastVersion++
      },
      isNexusStreaming = isNexusStreaming,
      onOpenNexus = {
        nexusInitialLayoutMode = NexusHubLayoutMode.SPLIT_COMMAND
        openNexusNode(NexusCoreNode.ORBITAL_HUB)
      },
      onOpenModes = {
        nexusInitialLayoutMode = NexusHubLayoutMode.DECK_ONLY
        openNexusNode(NexusCoreNode.CONVERGENCE_GATE)
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

    // 3. Single Persistent NEXUS CORE Spatial Shell & Overlays Under Global DeviceAwareModalAutoScaler
    val hasAnyModalVisible =
      isNexusShellOpen ||
        showFullChangelogModal ||
        isLiveHudDragPillOpen ||
        needsInitialSwornCallsign

    if (hasAnyModalVisible) {
      DeviceAwareModalAutoScaler {
        Box(modifier = Modifier.fillMaxSize()) {
          if (isNexusShellOpen) {
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

            val onlineHumanPilotsList = mergedLivePilots

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
              remember(frameTick, sectorPilotsState, onlineHumanPilotsList) {
                maxOf(engine.getTotalHumanPilotsOnline(), onlineHumanPilotsList.size)
              }
            val syndicateOnlineCount =
              remember(syndicateTag, frameTick, friendsRefreshKey, sectorPilotsState, onlineHumanPilotsList) {
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
                  val remoteSynOnline =
                    onlineHumanPilotsList
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
                  1 + remoteSynOnline
                }
              }
            val strikeWingOnlineCount =
              remember(frameTick, friendsRefreshKey, sectorPilotsState, onlineHumanPilotsList) {
                val activeWing = engine.activeArmadaName.trim()
                if (activeWing.isBlank()) {
                  0
                } else {
                  val slotCallsigns =
                    engine.activeArmadaSlots
                      .filter { !it.isLeader && it.callsign.isNotBlank() }
                      .map { it.callsign.uppercase(Locale.US) }
                      .toSet()
                  val remoteWingUids =
                    onlineHumanPilotsList
                      .mapNotNull { st ->
                        val rUid = st.userId.ifEmpty { st.id }
                        if (
                          rUid.isNotBlank() &&
                            rUid != currentUserId &&
                            (st.armadaName.trim().equals(activeWing, ignoreCase = true) ||
                              st.callsign.trim().uppercase(Locale.US) in slotCallsigns)
                        ) {
                          rUid
                        } else {
                          null
                        }
                      }
                      .toMutableSet()
                  syndicateFriends.forEach { fr ->
                    if (
                      fr.isOnline &&
                        fr.uid.isNotBlank() &&
                        fr.uid != currentUserId &&
                        (fr.currentArmadaName.trim().equals(activeWing, ignoreCase = true) ||
                          fr.callsign.trim().uppercase(Locale.US) in slotCallsigns)
                    ) {
                      remoteWingUids.add(fr.uid)
                    }
                  }
                  (1 + remoteWingUids.size).coerceAtMost(5)
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
              when (activeNexusNode) {
                NexusCoreNode.ORBITAL_HUB,
                NexusCoreNode.SUBSPACE_COMMS,
                NexusCoreNode.CONVERGENCE_GATE -> "NEXUS"
                NexusCoreNode.HANGAR_DECK -> "HANGAR"
                NexusCoreNode.STAR_FORGE -> "FORGE"
                NexusCoreNode.RIFT_CONDUIT -> "PROGRESSION"
                NexusCoreNode.SYNDICATE_CITADEL ->
                  if (syndicateSubDeck == SyndicateCitadelSubDeck.STRIKE_WING) "ARMADA"
                  else "SYNDICATES"
                NexusCoreNode.ARCHIVE_CODEX ->
                  if (archiveSubDeck == ArchiveCodexSubDeck.TACTICAL_CODEX) "CODEX" else "DOSSIER"
                NexusCoreNode.HELM_SYSTEMS -> "SETTINGS"
              }

            val closeEmbeddedModal: () -> Unit = {
              activeNexusNode = NexusCoreNode.ORBITAL_HUB
            }
            val saveCustomSlotShared: (CustomShipLoadoutSlot) -> Unit = { updatedSlot ->
              activeCustomSlot = updatedSlot
              localPrefs.saveCustomShipSlot(updatedSlot, currentUserId)
              RiftCodex.updateActiveCustomShipSlot(updatedSlot)
              engine.spentCustomizationPoints = updatedSlot.totalAllocatedPoints()
              engine.syncLocalPlayerCustomShipSpec()
            }
            val switchCustomSlotShared: (String) -> Unit = { targetSlotId ->
              activeCustomSlotId = targetSlotId
              localPrefs.setActiveCustomSlotId(targetSlotId, currentUserId)
              val loaded = localPrefs.getCustomShipSlot(targetSlotId, currentUserId)
              activeCustomSlot = loaded
              RiftCodex.updateActiveCustomShipSlot(loaded)
              engine.spentCustomizationPoints = loaded.totalAllocatedPoints()
              engine.syncLocalPlayerCustomShipSpec()
              sfx.playPrize()
            }
            val unlockCustomSlotShared: (String) -> Boolean = { targetSlotId ->
              val gate = RiftProgressionCatalog.getFrameSlotGate(targetSlotId)
              val cost = gate.shardCost
              val freeByMilestone =
                engine.peakResonance >= gate.freeAtResonance || engine.pilotLevel >= gate.freeAtLevel
              if (freeByMilestone || RiftBillingManager.canAffordShardSpend(engine.riftShards, cost)) {
                if (!freeByMilestone && cost > 0L) {
                  engine.riftShards = RiftBillingManager.computeBalanceAfterSpend(engine.riftShards, cost)
                  localPrefs.setRiftShards(engine.riftShards, currentUserId)
                }
                engine.unlockedExtraSlots++
                localPrefs.setCustomSlotUnlocked(targetSlotId, true, currentUserId)
                slot9BUnlocked = localPrefs.isCustomSlotUnlocked("Frame B", currentUserId)
                slot9CUnlocked = localPrefs.isCustomSlotUnlocked("Frame C", currentUserId)
                slot9DUnlocked = localPrefs.isCustomSlotUnlocked("Frame D", currentUserId)
                slot9EUnlocked = localPrefs.isCustomSlotUnlocked("Frame E", currentUserId)
                switchCustomSlotShared(targetSlotId)
                engine.onStatsUpdated()
                true
              } else false
            }
            val forgeAiHullShared: (String, String) -> Pair<Boolean, String> = { promptText, hullTitle ->
              val isFirstFree = activeCustomSlot.aiGenerationsCount == 0
              val canAfford =
                isFirstFree ||
                  engine.aiShipDesignTokens > 0 ||
                  RiftBillingManager.canAffordShardSpend(engine.riftShards, 150L)
              if (!canAfford) {
                Pair(false, "NEED 150 ❖ RIFT SHARDS FOR HULL RE-DESIGN (OPEN RIFT CONDUIT).")
              } else {
                if (!isFirstFree) {
                  if (engine.aiShipDesignTokens > 0) {
                    engine.aiShipDesignTokens--
                  } else {
                    engine.riftShards = RiftBillingManager.computeBalanceAfterSpend(engine.riftShards, 150L)
                    localPrefs.setRiftShards(engine.riftShards, currentUserId)
                  }
                }
                val nextGenCount = activeCustomSlot.aiGenerationsCount + 1
                val baseRecipe = CustomHullVisualRecipe.synthesizeFromPrompt(promptText, nextGenCount)
                val finalTitle = hullTitle.trim().uppercase(Locale.US).take(18).ifEmpty { baseRecipe.hullName }
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
                val costNote =
                  when {
                    isFirstFree -> "FREE 1ST DESIGN"
                    RiftBillingManager.TESTER_INFINITE_SHARDS_ACTIVE -> "TESTER 0❖ DEPLETED"
                    else -> "-150 ❖"
                  }
                Pair(true, "HULL FORGED ($costNote): [$finalTitle] (${finalRecipe.silhouette})")
              }
            }
            val toggleSfxShared: () -> Unit = {
              sfxEnabled = !sfxEnabled; sfx.enabled = sfxEnabled; localPrefs.setSfxEnabled(sfxEnabled)
            }
            val toggleMusicShared: () -> Unit = {
              musicEnabled = !musicEnabled; localPrefs.setMusicEnabled(musicEnabled); sfx.setMusicEnabled(musicEnabled)
            }
            val changeMusicBpmShared: (Int) -> Unit = { newBpm ->
              val clamped = newBpm.coerceIn(60, 180)
              musicBpm = clamped; sfx.setMusicBpmLive(clamped)
              activeMusicSummary = sfx.currentMusicRecipe.customPromptSummary
              localPrefs.setMusicBpm(clamped)
            }
            val changeMusicVolShared: (Float) -> Unit = { newVol ->
              val clamped = newVol.coerceIn(0f, 1f)
              musicVolume = clamped; sfx.setMusicVolumeLive(clamped); localPrefs.setMusicVolume(clamped)
            }
            val changeSfxVolShared: (Float) -> Unit = { newVol ->
              val clamped = newVol.coerceIn(0f, 1f)
              sfxVolume = clamped; sfx.setSfxVolumeLive(clamped); localPrefs.setSfxVolume(clamped)
            }
            val selectMusicArchetypeShared: (MusicSynthArchetype) -> Unit = { archetype ->
              sfx.applyMusicArchetype(archetype)
              musicBpm = sfx.musicBpm
              activeMusicArchetypeId = archetype.id
              activeMusicSummary = sfx.currentMusicRecipe.customPromptSummary
              musicForgePrompt = ""
              localPrefs.setMusicArchetypeId(archetype.id)
              localPrefs.setMusicForgePrompt("")
              localPrefs.setMusicBpm(sfx.musicBpm)
              if (!musicEnabled) { musicEnabled = true; localPrefs.setMusicEnabled(true); sfx.setMusicEnabled(true) }
            }
            val forgeMusicPromptShared: (String, Boolean) -> Unit = { rawPrompt, showToast ->
              val forged = sfx.forgeMusicFromPrompt(rawPrompt)
              musicBpm = forged.bpm
              activeMusicArchetypeId = forged.archetype.id
              activeMusicSummary = forged.customPromptSummary
              musicForgePrompt = rawPrompt.trim()
              localPrefs.setMusicArchetypeId(forged.archetype.id)
              localPrefs.setMusicForgePrompt(musicForgePrompt)
              localPrefs.setMusicBpm(forged.bpm)
              if (!musicEnabled) { musicEnabled = true; localPrefs.setMusicEnabled(true); sfx.setMusicEnabled(true) }
              sfx.playPrize()
              if (showToast) {
                toastMessage = "FORGED TRACK: ${forged.customPromptSummary}"
                toastVersion++
              }
            }

            val stageRankedModeIntoHangar: (RankedGameMode, Int) -> Unit = { mode, bracket ->
              if (mode == RankedGameMode.SYNDICATE_WARS) {
                val (eligible, reason) = engine.canQueueSyndicateWars()
                if (!eligible) {
                  sfx.playMutedClick()
                  toastMessage = reason
                  toastVersion++
                } else {
                  pendingStagedLaunch = StagedRiftLaunch.Ranked(mode, bracket)
                  selectedMode = mode.id
                  sfx.playMutedClick()
                  activeNexusNode = NexusCoreNode.HANGAR_DECK
                  isNexusShellOpen = true
                  toastMessage = "STAGED ${mode.title.uppercase(Locale.US)} // SELECT HULL & ENTER RIFT"
                  toastVersion++
                }
              } else {
                pendingStagedLaunch = StagedRiftLaunch.Ranked(mode, bracket)
                selectedMode = mode.id
                sfx.playMutedClick()
                activeNexusNode = NexusCoreNode.HANGAR_DECK
                isNexusShellOpen = true
                toastMessage = "STAGED ${mode.title.uppercase(Locale.US)} // SELECT HULL & ENTER RIFT"
                toastVersion++
              }
            }

            val stagedTargetTitle: String? =
              remember(pendingStagedLaunch, frameTick) {
                when (val staged = pendingStagedLaunch) {
                  is StagedRiftLaunch.Ranked -> {
                    val bracketBadge =
                      when (staged.mode) {
                        RankedGameMode.RIFT_SKIRMISH,
                        RankedGameMode.RIFT_CONTROL,
                        RankedGameMode.SYNDICATE_WARS -> " (${staged.teamSizeBracket}V${staged.teamSizeBracket})"
                        else -> " (${staged.mode.teamFormatBadge})"
                      }
                    "${staged.mode.title}$bracketBadge"
                  }
                  is StagedRiftLaunch.NewCustom ->
                    "CUSTOM RIFT: ${staged.config.title} (${staged.config.baseMode.title})"
                  is StagedRiftLaunch.ExistingCustom -> {
                    val found = engine.activeCustomGames.firstOrNull { it.instanceId == staged.instanceId }
                    if (found != null) {
                      "CUSTOM RIFT: ${found.title} (${found.baseMode.title})"
                    } else {
                      "CUSTOM RIFT [${staged.instanceId}]"
                    }
                  }
                  null -> null
                }
              }

            val stagedHullLockNotice: Boolean =
              remember(pendingStagedLaunch, frameTick) {
                when (val staged = pendingStagedLaunch) {
                  is StagedRiftLaunch.Ranked -> engine.doesModeLockShipSelection(staged.mode)
                  is StagedRiftLaunch.NewCustom -> engine.doesModeLockShipSelection(staged.config.baseMode)
                  is StagedRiftLaunch.ExistingCustom -> {
                    val found = engine.activeCustomGames.firstOrNull { it.instanceId == staged.instanceId }
                    found != null && engine.doesModeLockShipSelection(found.baseMode)
                  }
                  null -> engine.hasJoinedMatch && engine.doesModeLockShipSelection()
                }
              }

            val embeddedModuleContent: (@Composable () -> Unit)? =
              when (activeNexusNode) {
                NexusCoreNode.ORBITAL_HUB,
                NexusCoreNode.SUBSPACE_COMMS,
                NexusCoreNode.CONVERGENCE_GATE -> null
                else -> {
                  {
                    when (activeNexusNode) {
                      NexusCoreNode.HANGAR_DECK -> {
                        val (_, _, hangarLvlFrac) =
                          remember(frameTick) { engine.getXpProgressInCurrentLevel() }
                        val currentSectorModeId =
                          when (val staged = pendingStagedLaunch) {
                            is StagedRiftLaunch.Ranked -> staged.mode.id
                            is StagedRiftLaunch.NewCustom -> staged.config.baseMode.id
                            is StagedRiftLaunch.ExistingCustom ->
                              engine.activeCustomGames
                                .firstOrNull { it.instanceId == staged.instanceId }
                                ?.baseMode
                                ?.id ?: engine.activeRankedMode.id
                            null ->
                              engine.currentCustomGame?.baseMode?.id ?: engine.activeRankedMode.id
                          }
                        HangarModal(
                          initialCallsign = callsign,
                          syndicateTag = syndicateTag,
                          syndicateName = syndicateName,
                          selectedShipId = selectedShipId,
                          bestShipId = bestShipId,
                          selectedMode = currentSectorModeId,
                          pilotLevel = engine.pilotLevel,
                          xpProgressFraction = hangarLvlFrac,
                          customizationPointsTotal = engine.getCustomizationPointsTotal(),
                          spentCustomizationPoints = activeCustomSlot.totalAllocatedPoints(),
                          riftShards = RiftBillingManager.effectiveShardBalance(engine.riftShards),
                          unlockFullNinthShipOverride = RiftCalibrationState.unlockFullNinthShipOverride,
                          activeCustomSlot = activeCustomSlot,
                          slot9BUnlocked = slot9BUnlocked,
                          slot9CUnlocked = slot9CUnlocked,
                          slot9DUnlocked = slot9DUnlocked,
                          slot9EUnlocked = slot9EUnlocked,
                          liveResonance = engine.liveResonance,
                          peakResonance = engine.peakResonance,
                          stagedTargetTitle = stagedTargetTitle,
                          stagedHullLockNotice = stagedHullLockNotice,
                          onCancelStagedModeToGate =
                            if (pendingStagedLaunch != null) {
                              {
                                pendingStagedLaunch = null
                                sfx.playMutedClick()
                                activeNexusNode = NexusCoreNode.CONVERGENCE_GATE
                                isNexusShellOpen = true
                              }
                            } else {
                              null
                            },
                          onOpenRiftShardsStore = {
                            previousNodeBeforeConduit = NexusCoreNode.HANGAR_DECK
                            openNexusNode(NexusCoreNode.RIFT_CONDUIT)
                          },
                          onOpenSyndicatesModal = {
                            syndicateStatusBanner = null
                            syndicateSubDeck = SyndicateCitadelSubDeck.SYNDICATES_WARS
                            openNexusNode(NexusCoreNode.SYNDICATE_CITADEL)
                          },
                          onSaveCustomSlot = saveCustomSlotShared,
                          onSwitchCustomSlot = switchCustomSlotShared,
                          onUnlockCustomSlot = unlockCustomSlotShared,
                          onRespecCustomSlot = {
                            if (
                              RiftBillingManager.canAffordShardSpend(engine.riftShards, 50L) &&
                                activeCustomSlot.totalAllocatedPoints() > 0
                            ) {
                              engine.riftShards =
                                RiftBillingManager.computeBalanceAfterSpend(engine.riftShards, 50L)
                              localPrefs.setRiftShards(engine.riftShards, currentUserId)
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
                              saveCustomSlotShared(zeroed)
                              engine.onStatsUpdated()
                              sfx.playPrize()
                              true
                            } else false
                          },
                          onForgeAiHullDesign = forgeAiHullShared,
                          isNexusStreaming = isNexusStreaming,
                          onOpenNexus = { openNexusNode(NexusCoreNode.ORBITAL_HUB) },
                          onOpenShipForge = {
                            forgeInitialTab = NexusForgeTab.SHIP_COSMETICS
                            openNexusNode(NexusCoreNode.STAR_FORGE)
                          },
                          onDeployPilot = { newCallsign, newShipId, _, asGuest ->
                            if (asGuest) localPrefs.setGuestBypassEnabled(true)
                            val cleanCallsign =
                              if (isGoogleAuthenticated) localPrefs.setCallsign(newCallsign, currentUserId)
                              else localPrefs.setCallsign(newCallsign)
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
                            callsign = cleanCallsign
                            selectedShipId = clampedShipId
                            syndicateTag = localPrefs.getSyndicateTag()
                            syndicateName = localPrefs.getSyndicateName()
                            localPrefs.setPreferredShipId(clampedShipId)
                            engine.localPlayer.callsign = cleanCallsign
                            engine.localPlayer.syndicateTag = syndicateTag
                            engine.syncLocalArmadaSelfSlot()

                            val staged = pendingStagedLaunch
                            if (staged != null || !engine.hasJoinedMatch) {
                              // Allow hull selection in Hangar staging before mode hull-lock engages
                              engine.isShipLockedForMatch = false
                            }
                            engine.switchLocalPlayerShip(clampedShipId, forceFromHangar = true)
                            syncEquippedCosmeticsToEngine(engine)

                            when (staged) {
                              is StagedRiftLaunch.Ranked -> {
                                selectedMode = staged.mode.id
                                localPrefs.setPreferredMode(staged.mode.id)
                                engine.deployToRankedMode(staged.mode, staged.teamSizeBracket)
                              }
                              is StagedRiftLaunch.NewCustom -> {
                                selectedMode = staged.config.baseMode.id
                                localPrefs.setPreferredMode(staged.config.baseMode.id)
                                engine.createAndJoinCustomGame(staged.config)
                              }
                              is StagedRiftLaunch.ExistingCustom -> {
                                engine.joinCustomGame(staged.instanceId)
                                val resolvedModeId =
                                  engine.currentCustomGame?.baseMode?.id ?: engine.activeRankedMode.id
                                selectedMode = resolvedModeId
                                localPrefs.setPreferredMode(resolvedModeId)
                              }
                              null -> {
                                val activeModeId =
                                  engine.currentCustomGame?.baseMode?.id ?: engine.activeRankedMode.id
                                selectedMode = activeModeId
                                localPrefs.setPreferredMode(activeModeId)
                                if (!engine.hasJoinedMatch) {
                                  engine.deployToRankedMode(
                                    engine.activeRankedMode,
                                    engine.activeTeamSizeBracket,
                                  )
                                }
                              }
                            }

                            syncEquippedCosmeticsToEngine(engine)
                            engine.onStatsUpdated()
                            if (engine.hasJoinedMatch) {
                              pushLivePilotHeartbeatNow()
                              pendingStagedLaunch = null
                              isNexusShellOpen = false
                            }
                          },
                          onCloseModal = closeEmbeddedModal,
                        )
                      }

                      NexusCoreNode.RIFT_CONDUIT -> {
                        val (curLvlXp, nextLvlSpan, lvlFrac) =
                          remember(frameTick) { engine.getXpProgressInCurrentLevel() }
                        val presencePct =
                          remember(frameTick) {
                            (engine.getMatchPresenceRatio() * 100f).toInt().coerceIn(0, 100)
                          }
                        val conduitDirectives =
                          remember(directivesRefreshKey, frameTick, currentUserId) {
                            localPrefs.getDailyDirectives(currentUserId)
                          }
                        val conduitCanReroll =
                          remember(directivesRefreshKey, currentUserId) {
                            localPrefs.canRerollDailyDirectiveToday(currentUserId)
                          }
                        val conduitSweepClaimed =
                          remember(directivesRefreshKey, currentUserId) {
                            localPrefs.isDailySweepBonusClaimed(currentUserId)
                          }

                        RiftShardsStoreModal(
                          pilotLevel = engine.pilotLevel,
                          currentLevelXp = curLvlXp,
                          nextLevelXpSpan = nextLvlSpan,
                          levelProgressFraction = lvlFrac,
                          totalCustomizationPoints = engine.getCustomizationPointsTotal(),
                          spentCustomizationPoints = activeCustomSlot.totalAllocatedPoints(),
                          riftShards = RiftBillingManager.effectiveShardBalance(engine.riftShards),
                          publicGameWins = engine.convergencesWon,
                          matchPresencePct = presencePct,
                          unlockedExtraSlots = engine.unlockedExtraSlots,
                          aiShipDesignTokens = engine.aiShipDesignTokens,
                          liveResonance = engine.liveResonance,
                          peakResonance = engine.peakResonance,
                          startingBounty = engine.getLocalStartingBounty(),
                          dailyDirectives = conduitDirectives,
                          canRerollDirectiveToday = conduitCanReroll,
                          dailySweepClaimed = conduitSweepClaimed,
                          activeShipId = selectedShipId,
                          activeCustomSlotId = activeCustomSlotId,
                          onTopUpRiftShards = { delta ->
                            engine.riftShards =
                              RiftBillingManager.effectiveShardBalance(engine.riftShards + delta)
                            localPrefs.setRiftShards(engine.riftShards, currentUserId)
                            engine.onStatsUpdated()
                          },
                          equippedCosmeticByCategory = { category ->
                            val _unused = cosmeticsRefreshKey
                            localPrefs.getEquippedCosmeticId(category, currentUserId)
                          },
                          isCosmeticUnlocked = { category, item ->
                            val _unused = cosmeticsRefreshKey
                            engine.peakResonance >= item.freeAtResonance ||
                              localPrefs.isCosmeticUnlocked(
                                category = category,
                                itemId = item.id,
                                pilotLevel = engine.pilotLevel,
                                uid = currentUserId,
                              )
                          },
                          isFrameSlotUnlocked = { slotId ->
                            when {
                              slotId.equals("Frame A", ignoreCase = true) -> true
                              slotId.equals("Frame B", ignoreCase = true) -> slot9BUnlocked
                              slotId.equals("Frame C", ignoreCase = true) -> slot9CUnlocked
                              slotId.equals("Frame D", ignoreCase = true) -> slot9DUnlocked
                              slotId.equals("Frame E", ignoreCase = true) -> slot9EUnlocked
                              else -> localPrefs.isCustomSlotUnlocked(slotId, currentUserId)
                            }
                          },
                          onClaimDailyDirective = { slotIdx ->
                            val claimed =
                              localPrefs.claimDailyDirectiveReward(slotIdx, currentUserId)
                            if (claimed != null) {
                              engine.riftShards =
                                RiftBillingManager.effectiveShardBalance(
                                  engine.riftShards + claimed.first
                                )
                              localPrefs.setRiftShards(engine.riftShards, currentUserId)
                              engine.liveResonance = localPrefs.getLiveResonance(currentUserId)
                              engine.peakResonance = localPrefs.getPeakResonance(currentUserId)
                              if (!engine.hasJoinedMatch) {
                                engine.localPlayer.bounty = engine.getLocalStartingBounty()
                              }
                              directivesRefreshKey++
                              sfx.playPrize()
                              engine.onStatsUpdated()
                              claimed.first
                            } else {
                              0L
                            }
                          },
                          onRerollDailyDirective = { slotIdx ->
                            val ok = localPrefs.rerollDailyDirective(slotIdx, currentUserId)
                            if (ok) {
                              directivesRefreshKey++
                              sfx.playMutedClick()
                            }
                            ok
                          },
                          onClaimDailySweep = {
                            val sweep = localPrefs.claimDailySweepBonus(currentUserId)
                            if (sweep != null) {
                              engine.riftShards =
                                RiftBillingManager.effectiveShardBalance(
                                  engine.riftShards + sweep.first
                                )
                              localPrefs.setRiftShards(engine.riftShards, currentUserId)
                              engine.liveResonance = localPrefs.getLiveResonance(currentUserId)
                              engine.peakResonance = localPrefs.getPeakResonance(currentUserId)
                              if (!engine.hasJoinedMatch) {
                                engine.localPlayer.bounty = engine.getLocalStartingBounty()
                              }
                              directivesRefreshKey++
                              sfx.playPrize()
                              engine.onStatsUpdated()
                              sweep.first
                            } else {
                              0L
                            }
                          },
                          onUnlockOrEquipCosmetic = { category, item ->
                            val alreadyUnlocked =
                              engine.peakResonance >= item.freeAtResonance ||
                                localPrefs.isCosmeticUnlocked(
                                  category = category,
                                  itemId = item.id,
                                  pilotLevel = engine.pilotLevel,
                                  uid = currentUserId,
                                )
                            if (alreadyUnlocked) {
                              localPrefs.setEquippedCosmeticId(category, item.id, currentUserId)
                              syncEquippedCosmeticsToEngine(engine)
                              cosmeticsRefreshKey++
                              sfx.playPrize()
                              engine.onStatsUpdated()
                              true to "EQUIPPED ${item.name.uppercase(Locale.US)}"
                            } else if (
                              RiftBillingManager.canAffordShardSpend(
                                engine.riftShards,
                                item.shardCost,
                              )
                            ) {
                              engine.riftShards =
                                RiftBillingManager.computeBalanceAfterSpend(
                                  engine.riftShards,
                                  item.shardCost,
                                )
                              localPrefs.setRiftShards(engine.riftShards, currentUserId)
                              localPrefs.setCosmeticUnlocked(category, item.id, true, currentUserId)
                              localPrefs.setEquippedCosmeticId(category, item.id, currentUserId)
                              syncEquippedCosmeticsToEngine(engine)
                              cosmeticsRefreshKey++
                              sfx.playPrize()
                              engine.onStatsUpdated()
                              val spendNote =
                                if (RiftBillingManager.TESTER_INFINITE_SHARDS_ACTIVE) {
                                  "TESTER 0❖ DEPLETED"
                                } else {
                                  "-${item.shardCost}❖"
                                }
                              true to
                                "UNLOCKED & EQUIPPED ${item.name.uppercase(Locale.US)} ($spendNote)"
                            } else {
                              false to
                                "REQUIRES ${item.freeAtResonance} PEAK RESONANCE (FREE) OR ${item.shardCost}❖ RIFT SHARDS"
                            }
                          },
                          onUnlockOrSelectFrameSlot = { slotId ->
                            val gate = RiftProgressionCatalog.getFrameSlotGate(slotId)
                            val unlockedNow =
                              slotId.equals("Frame A", ignoreCase = true) ||
                                localPrefs.isCustomSlotUnlocked(slotId, currentUserId)
                            if (unlockedNow) {
                              switchCustomSlotShared(slotId)
                              selectedShipId = 10
                              localPrefs.setPreferredShipId(10)
                              engine.switchLocalPlayerShip(10, forceFromHangar = true)
                              syncEquippedCosmeticsToEngine(engine)
                              engine.onStatsUpdated()
                              openNexusNode(NexusCoreNode.HANGAR_DECK)
                              true to "SELECTED ${slotId.uppercase(Locale.US)} // OPENING HANGAR DECK"
                            } else if (unlockCustomSlotShared(slotId)) {
                              selectedShipId = 10
                              localPrefs.setPreferredShipId(10)
                              engine.switchLocalPlayerShip(10, forceFromHangar = true)
                              syncEquippedCosmeticsToEngine(engine)
                              engine.onStatsUpdated()
                              val freeNote =
                                when {
                                  engine.peakResonance >= gate.freeAtResonance ->
                                    "PEAK RESONANCE MILESTONE"
                                  RiftBillingManager.TESTER_INFINITE_SHARDS_ACTIVE ->
                                    "TESTER 0❖ DEPLETED"
                                  else -> "-${gate.shardCost}❖ RIFT SHARDS"
                                }
                              true to "UNLOCKED ${slotId.uppercase(Locale.US)} ($freeNote)"
                            } else {
                              false to
                                "REQUIRES ${gate.freeAtResonance} PEAK RESONANCE (FREE) OR ${gate.shardCost}❖ RIFT SHARDS"
                            }
                          },
                          onSpendRiftShards = { cost ->
                            if (RiftBillingManager.canAffordShardSpend(engine.riftShards, cost)) {
                              engine.riftShards =
                                RiftBillingManager.computeBalanceAfterSpend(engine.riftShards, cost)
                              localPrefs.setRiftShards(engine.riftShards, currentUserId)
                              sfx.playPrize()
                              engine.onStatsUpdated()
                              true
                            } else {
                              false
                            }
                          },
                          onRespecCustomizationPoints = {
                            if (
                              RiftBillingManager.canAffordShardSpend(engine.riftShards, 50L) &&
                                activeCustomSlot.totalAllocatedPoints() > 0
                            ) {
                              engine.riftShards =
                                RiftBillingManager.computeBalanceAfterSpend(engine.riftShards, 50L)
                              localPrefs.setRiftShards(engine.riftShards, currentUserId)
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
                              saveCustomSlotShared(zeroed)
                              sfx.playPrize()
                              engine.onStatsUpdated()
                              true
                            } else {
                              false
                            }
                          },
                          onPurchaseAiDesignToken = {
                            if (RiftBillingManager.canAffordShardSpend(engine.riftShards, 150L)) {
                              engine.riftShards =
                                RiftBillingManager.computeBalanceAfterSpend(engine.riftShards, 150L)
                              localPrefs.setRiftShards(engine.riftShards, currentUserId)
                              engine.aiShipDesignTokens++
                              sfx.playPrize()
                              engine.onStatsUpdated()
                              true
                            } else {
                              false
                            }
                          },
                          onOpenHangarShip10 = {
                            selectedShipId = 10
                            localPrefs.setPreferredShipId(10)
                            engine.switchLocalPlayerShip(10, forceFromHangar = true)
                            syncEquippedCosmeticsToEngine(engine)
                            engine.onStatsUpdated()
                            openNexusNode(NexusCoreNode.HANGAR_DECK)
                          },
                          onCloseModal = { openNexusNode(previousNodeBeforeConduit) },
                        )
                      }

                      NexusCoreNode.SYNDICATE_CITADEL -> {
                        if (syndicateSubDeck == SyndicateCitadelSubDeck.STRIKE_WING) {
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
                              val trimmedWing =
                                newArmadaName
                                  .trim()
                                  .uppercase(Locale.US)
                                  .replace(Regex("[^A-Z0-9_-]"), "")
                                  .take(14)
                              if (trimmedWing.isEmpty() || UgcModeration.containsProfanity(trimmedWing)) {
                                armadaStatusBanner = "STRIKE WING TAG REJECTED BY SAFETY FILTER."
                              } else {
                                engine.formOrJoinArmada(
                                  trimmedWing,
                                  if (engine.activeArmadaSlots.size <= 1) friendsList.take(1)
                                  else emptyList(),
                                )
                                armadaStatusBanner =
                                  "STRIKE WING [${engine.activeArmadaName}] DEPLOYED (${engine.getCurrentArmadaSize()}/5 PILOTS)"
                                friendsRefreshKey++
                              }
                            },
                            onInviteFriendToArmada = { friend ->
                              val added = engine.addMemberToArmada(friend)
                              if (added) {
                                armadaStatusBanner =
                                  "${friend.callsign} JOINED STRIKE WING [${engine.activeArmadaName}] (${engine.getCurrentArmadaSize()}/5)"
                              }
                              friendsRefreshKey++
                            },
                            onRequestJoinFriendArmada = { friend ->
                              val targetArmada =
                                friend.currentArmadaName
                                  .ifEmpty { "${friend.syndicateTag}_WING" }
                                  .take(14)
                              engine.formOrJoinArmada(targetArmada, listOf(friend))
                              armadaStatusBanner =
                                "JOINED STRIKE WING [$targetArmada] WITH ${friend.callsign} (${engine.getCurrentArmadaSize()}/5)."
                              friendsRefreshKey++
                            },
                            onRemoveArmadaMember = { callsignToDrop ->
                              engine.removeMemberFromArmada(callsignToDrop)
                              armadaStatusBanner =
                                "ROSTER UPDATED -> NOW IN ${engine.getCurrentArmadaTierLabel()} TIER."
                              friendsRefreshKey++
                            },
                            onLeaveArmadaToSolo = {
                              engine.leaveArmadaToSolo()
                              armadaStatusBanner = "LEFT STRIKE WING -> OPERATING INDEPENDENTLY."
                              friendsRefreshKey++
                            },
                            onAddFriendByCallsign = { rawCallsign ->
                              viewModel.lookupAndAddRealFriendByCallsign(
                                rawCallsign = rawCallsign,
                                localPrefs = localPrefs,
                              ) { _, statusMsg ->
                                armadaStatusBanner = statusMsg
                                toastMessage = statusMsg
                                toastVersion++
                                friendsRefreshKey++
                              }
                            },
                            onBlockPilot = { pilotUid, pilotCallsign ->
                              localPrefs.blockPilot(pilotUid)
                              blockedPilotUids = localPrefs.getBlockedPilotUids()
                              armadaStatusBanner = "BLOCKED PILOT $pilotCallsign."
                            },
                            onReportPilot = { pilotUid, pilotCallsign ->
                              localPrefs.reportPilot(pilotUid, pilotCallsign, "Strike Wing Roster Report")
                              armadaStatusBanner = "REPORT SUBMITTED FOR $pilotCallsign."
                            },
                            onCloseModal = closeEmbeddedModal,
                          )
                        } else {
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
                                  "JOINED [$syndicateTag] $syndicateName! 15-DAY LOCK COOLDOWN (15d 00h) ACTIVE."
                                engine.onStatsUpdated()
                              } else {
                                syndicateStatusBanner =
                                  "CANNOT SWITCH SYNDICATES YET: ${localPrefs.getRemainingSyndicateLockFormatted()} REMAINING."
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
                                  "LEFT SYNDICATE -> UNAFFILIATED. 15-DAY LOCK COOLDOWN TRIGGERED."
                                engine.onStatsUpdated()
                              } else {
                                syndicateStatusBanner =
                                  "CANNOT LEAVE SYNDICATE YET: ${localPrefs.getRemainingSyndicateLockFormatted()} REMAINING."
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
                                  syndicateChannelsRefreshKey++
                                  syndicateStatusBanner =
                                    "CHARTERED [${created.tag}] ${created.name}! LOCKED FOR 15d 00h."
                                  engine.onStatsUpdated()
                                }
                                .onFailure { err ->
                                  syndicateStatusBanner = err.message ?: "INVALID SYNDICATE CHARTER."
                                }
                            },
                            onCloseModal = closeEmbeddedModal,
                          )
                        }
                      }

                      NexusCoreNode.ARCHIVE_CODEX -> {
                        if (archiveSubDeck == ArchiveCodexSubDeck.TACTICAL_CODEX) {
                          CodexModal(
                            isNexusStreaming = isNexusStreaming,
                            onOpenNexus = { openNexusNode(NexusCoreNode.ORBITAL_HUB) },
                            onCloseModal = closeEmbeddedModal,
                          )
                        } else {
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
                            spentCustomizationPoints = activeCustomSlot.totalAllocatedPoints(),
                            riftShards = RiftBillingManager.effectiveShardBalance(engine.riftShards),
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
                            liveResonance = engine.liveResonance,
                            peakResonance = engine.peakResonance,
                            startingBounty = engine.getLocalStartingBounty(),
                            onOpenRiftShardsStore = {
                              previousNodeBeforeConduit = NexusCoreNode.ARCHIVE_CODEX
                              openNexusNode(NexusCoreNode.RIFT_CONDUIT)
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
                              engine.riftShards = RiftBillingManager.effectiveShardBalance(0L)
                              localPrefs.setRiftShards(engine.riftShards, currentUserId)
                              engine.liveResonance = 0
                              engine.peakResonance = 0
                              engine.highestMilestoneLevelClaimed = 0
                              engine.spentCustomizationPoints = 0
                              engine.unlockedExtraSlots = 0
                              engine.aiShipDesignTokens = 0
                              engine.localPlayer.callsign = callsign
                              engine.localPlayer.syndicateTag = syndicateTag
                              engine.localPlayer.bounty = 100
                              engine.leaveArmadaToSolo()
                              engine.switchLocalPlayerShip(1, forceFromHangar = true)
                              syncEquippedCosmeticsToEngine(engine)
                              engine.resetCurrentLifeStats()
                              allTimePeakSnapshot = null
                              pvpPeakSnapshot = null
                              syndicateHistory = localPrefs.getSyndicateServiceHistory(currentUserId)
                              friendsRefreshKey++
                              directivesRefreshKey++
                              cosmeticsRefreshKey++
                              engine.onStatsUpdated()
                              toastMessage = "PILOT TELEMETRY RESET TO 0 RESONANCE (CALLSIGN PRESERVED)"
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
                              toastMessage = "SWORN PILOT CALLSIGN UPDATED TO $saved"
                              toastVersion++
                            },
                            onUpdateRiftShards = { remainingShards ->
                              val effective = RiftBillingManager.effectiveShardBalance(remainingShards)
                              engine.riftShards = effective
                              localPrefs.setRiftShards(effective, currentUserId)
                              engine.onStatsUpdated()
                            },
                            onCloseModal = closeEmbeddedModal,
                          )
                        }
                      }

                      NexusCoreNode.HELM_SYSTEMS -> {
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
                          onToggleSfx = toggleSfxShared,
                          onToggleMusic = toggleMusicShared,
                          onChangeMusicBpm = changeMusicBpmShared,
                          onChangeMusicVolume = changeMusicVolShared,
                          onChangeSfxVolume = changeSfxVolShared,
                          onSelectMusicArchetype = selectMusicArchetypeShared,
                          onForgeCustomMusicPrompt = { forgeMusicPromptShared(it, true) },
                          onChangeCameraZoom = { newZoom ->
                            val clamped = newZoom.coerceIn(0.5f, 1.5f)
                            cameraZoom = clamped; engine.cameraZoom = clamped; localPrefs.setCameraZoom(clamped)
                          },
                          onChangePlayfieldTextScale = { newScale ->
                            val clamped = newScale.coerceIn(0.75f, 1.75f)
                            playfieldTextScale = clamped; engine.playfieldTextScale = clamped
                            localPrefs.setPlayfieldTextScale(clamped); frameTick++
                          },
                          onRequestGoogleSignIn = {
                            onGoogleSignInClicked(
                              context = context,
                              credentialManager = credentialManager,
                              onAuthSuccess = { toastMessage = "GOOGLE CLOUD ACCOUNT LINKED"; toastVersion++ },
                              onAuthError = { msg -> toastMessage = "SIGN-IN: $msg"; toastVersion++ },
                              scope = scope,
                            )
                          },
                          onRequestSignOut = {
                            localPrefs.setGuestBypassEnabled(false)
                            signOut(credentialManager = credentialManager, scope = scope)
                          },
                          onCloseModal = closeEmbeddedModal,
                          onOpenChangelog = { showFullChangelogModal = true },
                          onOpenAudioForge = {
                            forgeInitialTab = NexusForgeTab.AUDIO_SFX
                            openNexusNode(NexusCoreNode.STAR_FORGE)
                          },
                        )
                      }

                      NexusCoreNode.STAR_FORGE -> {
                        NexusForgeModal(
                          engine = engine,
                          sfx = sfx,
                          localPrefs = localPrefs,
                          currentUserId = currentUserId,
                          callsign = callsign,
                          syndicateTag = syndicateTag,
                          pilotLevel = engine.pilotLevel,
                          riftShards = RiftBillingManager.effectiveShardBalance(engine.riftShards),
                          selectedShipId = selectedShipId,
                          activeCustomSlot = activeCustomSlot,
                          slot9BUnlocked = slot9BUnlocked,
                          slot9CUnlocked = slot9CUnlocked,
                          slot9DUnlocked = slot9DUnlocked,
                          slot9EUnlocked = slot9EUnlocked,
                          unlockFullNinthShipOverride = RiftCalibrationState.unlockFullNinthShipOverride,
                          sfxEnabled = sfxEnabled,
                          musicEnabled = musicEnabled,
                          musicBpm = musicBpm,
                          musicVolume = musicVolume,
                          sfxVolume = sfxVolume,
                          initialTab = forgeInitialTab,
                          isFullScreenStage = isForgeFullScreenStage,
                          onToggleFullScreenStage = { expanded -> isForgeFullScreenStage = expanded },
                          onSelectShipForDeploy = { targetShipId ->
                            val clamped = targetShipId.coerceIn(1, 10)
                            selectedShipId = clamped
                            localPrefs.setPreferredShipId(clamped)
                            engine.switchLocalPlayerShip(clamped, forceFromHangar = true)
                            engine.onStatsUpdated()
                            sfx.playPrize()
                          },
                          onSaveCustomSlot = saveCustomSlotShared,
                          onSwitchCustomSlot = switchCustomSlotShared,
                          onUnlockCustomSlot = unlockCustomSlotShared,
                          onForgeAiHullDesign = forgeAiHullShared,
                          onSpendRiftShards = { cost ->
                            if (RiftBillingManager.canAffordShardSpend(engine.riftShards, cost)) {
                              engine.riftShards =
                                RiftBillingManager.computeBalanceAfterSpend(engine.riftShards, cost)
                              localPrefs.setRiftShards(engine.riftShards, currentUserId)
                              sfx.playPrize()
                              engine.onStatsUpdated()
                              true
                            } else false
                          },
                          onToggleSfx = toggleSfxShared,
                          onToggleMusic = toggleMusicShared,
                          onChangeMusicBpm = changeMusicBpmShared,
                          onChangeMusicVolume = changeMusicVolShared,
                          onChangeSfxVolume = changeSfxVolShared,
                          onSelectMusicArchetype = selectMusicArchetypeShared,
                          onForgeCustomMusicPrompt = { forgeMusicPromptShared(it, false) },
                          onEnterHudDragArenaMode = {
                            RiftCalibrationState.isUiUnlockedForDragging = true
                            RiftCalibrationState.isCalibrationModalOpen = true
                            isNexusShellOpen = false
                          },
                          onSyncCalibrationToTesters = {
                            val customGameId = engine.currentCustomGame?.instanceId ?: "global_custom"
                            val json = RiftCalibrationState.exportGameplaySyncPayloadJson()
                            viewModel.broadcastCalibrationToCustomGame(customGameId, callsign, json)
                            toastMessage = "CUSTOM GAME CALIBRATION SYNCED TO ALL TESTERS"
                            toastVersion++
                          },
                          onShowToast = { msg -> toastMessage = msg; toastVersion++ },
                        )
                      }

                      else -> Unit
                    }
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
                isNexusShellOpen = false
              },
              callsign = callsign,
              syndicateTag = syndicateTag,
              syndicateName = syndicateName,
              isGoogleAuthenticated = isGoogleAuthenticated,
              activeShipId = selectedShipId,
              bestShipId = bestShipId,
              riftShards = RiftBillingManager.effectiveShardBalance(engine.riftShards),
              unreadCommsCount = unreadCommsCount,
              currentUserId = currentUserId,
              bootToNexusHub = bootToNexusHub,
              onToggleBootToNexusHub = { _ ->
                bootToNexusHub = true
                localPrefs.setBootToNexusHub(true)
              },
              totalOnlinePilots = humanPilotsOnline,
              syndicateOnlineCount = syndicateOnlineCount,
              strikeWingOnlineCount = strikeWingOnlineCount,
              isInMatch = engine.hasJoinedMatch && !engine.isEphemeralPracticeRift,
              onEnterPracticeRift = {
                pendingStagedLaunch = null
                engine.switchLocalPlayerShip(selectedShipId, forceFromHangar = true)
                syncEquippedCosmeticsToEngine(engine)
                engine.enterEphemeralPracticeRift(startTutorial = true)
                isNexusShellOpen = false
                toastMessage = "PRACTICE RIFT INITIALIZED // SOLO FLIGHT BRIEFING"
                toastVersion++
                frameTick++
              },
              presetArmadasReadyCount = presetArmadasReadyCount,
              activeArmadaName = engine.activeArmadaName,
              armadaSlots = engine.activeArmadaSlots.toList(),
              onlineHumanPilots = onlineHumanPilotsList,
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
                stageRankedModeIntoHangar(mode, bracket)
              },
              onLeaveMatch = {
                pendingStagedLaunch = null
                engine.leaveActiveMatchToNexusOrbit()
                pushLivePilotHeartbeatNow()
                toastMessage = "LEFT MATCH // ORBITING NEXUS SECTOR"
                toastVersion++
                frameTick++
              },
              onOpenArmadaModal = {
                armadaStatusBanner = null
                syndicateSubDeck = SyndicateCitadelSubDeck.STRIKE_WING
                openNexusNode(NexusCoreNode.SYNDICATE_CITADEL)
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
                val fleetTag = "${prefix}_WING".take(14)
                engine.formOrJoinArmada(
                  fleetTag,
                  if (synMates.isNotEmpty()) synMates else syndicateFriends.take(2),
                )
                friendsRefreshKey++
                toastMessage =
                  "STRIKE WING [$fleetTag] STAGED (${engine.getCurrentArmadaSize()}/5 PILOTS)"
                toastVersion++
              },
              onOpenSyndicatesModal = {
                syndicateStatusBanner = null
                syndicateSubDeck = SyndicateCitadelSubDeck.SYNDICATES_WARS
                openNexusNode(NexusCoreNode.SYNDICATE_CITADEL)
              },
              onOpenSyndicateDirectory = {
                syndicateStatusBanner = null
                syndicateSubDeck = SyndicateCitadelSubDeck.SYNDICATES_WARS
                openNexusNode(NexusCoreNode.SYNDICATE_CITADEL)
              },
              onOpenHangar = { openNexusNode(NexusCoreNode.HANGAR_DECK) },
              onOpenProgression = {
                previousNodeBeforeConduit = activeNexusNode
                openNexusNode(NexusCoreNode.RIFT_CONDUIT)
              },
              onOpenDossier = {
                archiveSubDeck = ArchiveCodexSubDeck.PILOT_DOSSIER
                openNexusNode(NexusCoreNode.ARCHIVE_CODEX)
              },
              onOpenCodex = {
                archiveSubDeck = ArchiveCodexSubDeck.TACTICAL_CODEX
                openNexusNode(NexusCoreNode.ARCHIVE_CODEX)
              },
              onOpenSettings = { openNexusNode(NexusCoreNode.HELM_SYSTEMS) },
              onOpenForge = {
                RiftCalibrationState.isUiUnlockedForDragging = false
                RiftCalibrationState.isCalibrationModalOpen = false
                openNexusNode(NexusCoreNode.STAR_FORGE)
              },
              onOpenCalibration = {
                RiftCalibrationState.isUiUnlockedForDragging = false
                RiftCalibrationState.isCalibrationModalOpen = false
                forgeInitialTab = NexusForgeTab.DEV_STUDIO_STAGE
                openNexusNode(NexusCoreNode.STAR_FORGE)
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
                toastMessage = "EQUIPPED $shipTitle FOR SECTOR DEPLOYMENT"
                toastVersion++
              },
              canAccessSyndicateServer = canAccessSyndicate,
              syndicateCustomChannels = customSyndicateChannels,
              syndicateInvitedGuests = invitedSyndicateGuests,
              onAddSyndicateChannel = { rawChannel ->
                val currentChannels = localPrefs.getSyndicateCustomChannels(syndicateTag)
                if (currentChannels.size >= 5) {
                  toastMessage = "MAXIMUM 5 CHANNELS REACHED FOR [$syndicateTag]"
                  toastVersion++
                } else {
                  localPrefs.setSyndicateCustomChannels(
                    syndicateTag,
                    currentChannels + rawChannel,
                  )
                  syndicateChannelsRefreshKey++
                  toastMessage = "CREATED SYNDICATE CHANNEL IN [$syndicateTag]"
                  toastVersion++
                }
              },
              onDeleteSyndicateChannel = { rawChannel ->
                val currentChannels = localPrefs.getSyndicateCustomChannels(syndicateTag)
                val filtered =
                  currentChannels.filterNot { it.equals(rawChannel, ignoreCase = true) }
                localPrefs.setSyndicateCustomChannels(syndicateTag, filtered)
                syndicateChannelsRefreshKey++
                toastMessage = "REMOVED CHANNEL $rawChannel"
                toastVersion++
              },
              onInviteSyndicateGuest = { guestCallsign ->
                localPrefs.inviteGuestToSyndicateChannels(syndicateTag, guestCallsign)
                syndicateChannelsRefreshKey++
                toastMessage = "GRANTED [$syndicateTag] GUEST PASS TO $guestCallsign"
                toastVersion++
              },
              onRevokeSyndicateGuest = { guestCallsign ->
                localPrefs.revokeGuestFromSyndicateChannels(syndicateTag, guestCallsign)
                syndicateChannelsRefreshKey++
                toastMessage = "REVOKED GUEST PASS FOR $guestCallsign"
                toastVersion++
              },
              lobbyMessages = chatMessages,
              onSendChannelMessage = { channelKey, messageText ->
                val safeText = UgcModeration.sanitizeUgcText(messageText)
                val localMsg =
                  SyndicateChatMessage(
                    id = "msg_${System.currentTimeMillis()}",
                    senderUid = currentUserId,
                    senderCallsign = callsign,
                    senderSyndicateTag = syndicateTag,
                    timestampLabel = channelKey.uppercase(Locale.US),
                    messageText = safeText,
                  )
                chatMessages.add(localMsg)
                viewModel.postStructuredChannelMessage(
                  senderCallsign = callsign,
                  senderSyndicateTag = syndicateTag,
                  channelKey = channelKey,
                  messageText = safeText,
                )
              },
              onSubmitBugReport = { category, description ->
                val safeDesc = UgcModeration.sanitizeUgcText(description)
                localPrefs.reportPilot(
                  uid = "BUG_${category.take(10)}",
                  callsign = category,
                  reason = safeDesc,
                )
                viewModel.submitCloudBugReport(
                  reporterCallsign = callsign,
                  syndicateTag = syndicateTag,
                  category = category,
                  description = safeDesc,
                  buildVersion = CURRENT_RIFT_BUILD_VERSION,
                  activeMode = engine.activeMatchSectorTag(),
                  activeShipId = selectedShipId,
                  screenDpSummary = "${configuration.screenWidthDp}x${configuration.screenHeightDp}dp",
                )
                val bugEcho =
                  SyndicateChatMessage(
                    id = "bug_${System.currentTimeMillis()}",
                    senderUid = currentUserId,
                    senderCallsign = callsign,
                    senderSyndicateTag = syndicateTag,
                    timestampLabel = "#BUG-REPORTS",
                    messageText = "[$category] $safeDesc",
                    armadaObjective = "BUG|$category|v1.0.$CURRENT_RIFT_BUILD_VERSION",
                  )
                chatMessages.add(bugEcho)
                toastMessage = "ANOMALY LOGGED TO CLOUD TELEMETRY [$category]"
                toastVersion++
              },
              onShareActiveRiftFrameBuild = {
                val frameTitle = activeCustomSlot.customName.take(16)
                val compactRecipe =
                  "BUILD|$frameTitle|" +
                    "${activeCustomSlot.speedPts},${activeCustomSlot.thrustPts},${activeCustomSlot.turnPts},${activeCustomSlot.maxFluxPts},${activeCustomSlot.rechargePts}|" +
                    "${activeCustomSlot.primaryArchetype}|${activeCustomSlot.secondaryType}"
                val summary =
                  "[$frameTitle] ${activeCustomSlot.totalAllocatedPoints()}-PT RIFT FRAME // " +
                    "${activeCustomSlot.primaryArchetype} + ${activeCustomSlot.secondaryType} " +
                    "(SPD ${activeCustomSlot.speedPts} / THR ${activeCustomSlot.thrustPts} / TRN ${activeCustomSlot.turnPts} / FLX ${activeCustomSlot.maxFluxPts} / RCH ${activeCustomSlot.rechargePts})"
                val buildCard =
                  SyndicateChatMessage(
                    id = "build_${System.currentTimeMillis()}",
                    senderUid = currentUserId,
                    senderCallsign = callsign,
                    senderSyndicateTag = syndicateTag,
                    timestampLabel = "#SHIPWRIGHT-META",
                    messageText = summary,
                    armadaName = frameTitle.take(12),
                    armadaObjective = compactRecipe.take(120),
                  )
                chatMessages.add(buildCard)
                viewModel.postStructuredChannelMessage(
                  senderCallsign = callsign,
                  senderSyndicateTag = syndicateTag,
                  channelKey = "#shipwright-meta",
                  messageText = summary,
                  armadaName = frameTitle.take(12),
                  armadaObjective = compactRecipe.take(120),
                )
                toastMessage = "SHARED RIFT FRAME [$frameTitle] TO #SHIPWRIGHT-META"
                toastVersion++
              },
              onLoadSharedRiftFrameBuild = { recipePayload ->
                val loadedSlot =
                  if (recipePayload.trim().startsWith("{")) {
                    CustomShipLoadoutSlot.fromJsonString(recipePayload, activeCustomSlotId)
                      .copy(slotId = activeCustomSlotId)
                  } else {
                    val stripped = recipePayload.removePrefix("BUILD|").trim()
                    val parts = stripped.split("|")
                    val namePart = parts.getOrNull(0)?.trim()?.take(18)?.ifEmpty { "Shared Frame" } ?: "Shared Frame"
                    val statNums =
                      parts.getOrNull(1)
                        ?.split(",")
                        ?.map { it.trim().toIntOrNull()?.coerceIn(0, 40) ?: 0 }
                        ?: emptyList()
                    val prim = parts.getOrNull(2)?.trim().orEmpty().ifEmpty { activeCustomSlot.primaryArchetype }
                    val sec = parts.getOrNull(3)?.trim().orEmpty().ifEmpty { activeCustomSlot.secondaryType }
                    val maxBudget = engine.getCustomizationPointsTotal()
                    val gearPts =
                      CustomShipLoadoutSlot.getPrimaryPointCost(prim) +
                        CustomShipLoadoutSlot.getSecondaryPointCost(sec)
                    var rem = (maxBudget - gearPts).coerceAtLeast(0)
                    fun takePt(v: Int): Int {
                      val c = v.coerceIn(0, rem)
                      rem -= c
                      return c
                    }
                    val sSpd = takePt(statNums.getOrElse(0) { 0 })
                    val sThr = takePt(statNums.getOrElse(1) { 0 })
                    val sTrn = takePt(statNums.getOrElse(2) { 0 })
                    val sFlx = takePt(statNums.getOrElse(3) { 0 })
                    val sRch = takePt(statNums.getOrElse(4) { 0 })
                    activeCustomSlot.copy(
                      slotId = activeCustomSlotId,
                      customName = namePart,
                      speedPts = sSpd,
                      thrustPts = sThr,
                      turnPts = sTrn,
                      maxFluxPts = sFlx,
                      rechargePts = sRch,
                      primaryArchetype = prim,
                      secondaryType = sec,
                    )
                  }
                saveCustomSlotShared(loadedSlot)
                selectedShipId = 10
                localPrefs.setPreferredShipId(10)
                engine.switchLocalPlayerShip(10, forceFromHangar = true)
                engine.onStatsUpdated()
                sfx.playPrize()
                openNexusNode(NexusCoreNode.HANGAR_DECK)
                toastMessage = "LOADED [${loadedSlot.customName}] INTO $activeCustomSlotId"
                toastVersion++
              },
              onPostStrikeWingInvite = {
                val wingTag =
                  engine.activeArmadaName
                    .ifBlank { "${callsign.take(8)}_WING" }
                    .uppercase(Locale.US)
                    .take(14)
                if (engine.activeArmadaName.isBlank()) {
                  engine.formOrJoinArmada(wingTag, emptyList())
                }
                val slotsNow = engine.getCurrentArmadaSize()
                val text =
                  "STRIKE WING [$wingTag] RECRUITING PILOTS // ${engine.activeRankedMode.title.uppercase(Locale.US)}"
                val inviteMsg =
                  SyndicateChatMessage(
                    id = "wing_${System.currentTimeMillis()}",
                    senderUid = currentUserId,
                    senderCallsign = callsign,
                    senderSyndicateTag = syndicateTag,
                    timestampLabel = "#STRIKE-LFG",
                    messageText = text,
                    isArmadaRecruitBanner = true,
                    armadaName = wingTag.take(12),
                    armadaCurrentSize = slotsNow.coerceIn(1, 5),
                    armadaMaxSize = 5,
                    armadaObjective = engine.activeRankedMode.title,
                  )
                chatMessages.add(inviteMsg)
                viewModel.postStructuredChannelMessage(
                  senderCallsign = callsign,
                  senderSyndicateTag = syndicateTag,
                  channelKey = "#strike-lfg",
                  messageText = text,
                  isArmadaRecruitBanner = true,
                  armadaName = wingTag.take(12),
                  armadaCurrentSize = slotsNow.coerceIn(1, 5),
                  armadaMaxSize = 5,
                  armadaObjective = engine.activeRankedMode.title,
                )
                toastMessage = "POSTED STRIKE WING [$wingTag] INVITE TO #STRIKE-LFG"
                toastVersion++
              },
              onPostPrivateRoomInvite = { roomCode, modeTitle ->
                val cleanCode = roomCode.trim().uppercase(Locale.US).take(6)
                val text = "PRIVATE RIFT [$cleanCode] OPEN // $modeTitle"
                val obj = "ROOM|$cleanCode|$modeTitle".take(120)
                val roomMsg =
                  SyndicateChatMessage(
                    id = "room_${System.currentTimeMillis()}",
                    senderUid = currentUserId,
                    senderCallsign = callsign,
                    senderSyndicateTag = syndicateTag,
                    timestampLabel = "#STRIKE-LFG",
                    messageText = text,
                    armadaName = cleanCode,
                    armadaObjective = obj,
                  )
                chatMessages.add(roomMsg)
                viewModel.postStructuredChannelMessage(
                  senderCallsign = callsign,
                  senderSyndicateTag = syndicateTag,
                  channelKey = "#strike-lfg",
                  messageText = text,
                  armadaName = cleanCode,
                  armadaObjective = obj,
                )
                toastMessage = "SHARED PRIVATE RIFT [$cleanCode] TO #STRIKE-LFG"
                toastVersion++
              },
              onJoinSharedStrikeWing = { msg ->
                val targetWing =
                  msg.armadaName
                    .ifBlank { "${msg.senderCallsign.take(8)}_WING" }
                    .uppercase(Locale.US)
                    .take(14)
                val leaderFriend =
                  if (msg.senderUid.isNotBlank() && msg.senderUid != currentUserId) {
                    FriendPilotEntry(
                      uid = msg.senderUid,
                      callsign = msg.senderCallsign,
                      syndicateTag = msg.senderSyndicateTag.ifBlank { "FREE" },
                      shipId = 1,
                      statusText = "ONLINE // STRIKE WING",
                      isOnline = true,
                      currentArmadaName = targetWing,
                      currentArmadaCount = msg.armadaCurrentSize.coerceIn(1, 5),
                      bounty = 100,
                    ).also { localPrefs.saveVerifiedFriendPilot(it) }
                  } else {
                    null
                  }
                engine.formOrJoinArmada(
                  targetWing,
                  if (leaderFriend != null) listOf(leaderFriend) else emptyList(),
                )
                friendsRefreshKey++
                toastMessage = "JOINED STRIKE WING [$targetWing]"
                toastVersion++
              },
              onAddOnlinePilotFriend = { targetCallsign ->
                viewModel.lookupAndAddRealFriendByCallsign(
                  rawCallsign = targetCallsign,
                  localPrefs = localPrefs,
                ) { _, statusMsg ->
                  armadaStatusBanner = statusMsg
                  toastMessage = statusMsg
                  toastVersion++
                  friendsRefreshKey++
                }
              },
              onInviteOnlinePilotToWing = { livePilot ->
                val pUid = livePilot.userId.ifEmpty { livePilot.id }
                if (pUid.isNotBlank() && pUid != currentUserId) {
                  val wingTag =
                    engine.activeArmadaName
                      .ifBlank { "${callsign.take(8)}_WING" }
                      .uppercase(Locale.US)
                      .take(14)
                  val verified =
                    FriendPilotEntry(
                      uid = pUid,
                      callsign = livePilot.callsign,
                      syndicateTag = livePilot.syndicateTag.ifBlank { "FREE" },
                      shipId = livePilot.shipId.toInt().coerceIn(1, 10),
                      statusText = "ONLINE // ${livePilot.sectorId.ifBlank { "SECTOR" }}",
                      isOnline = true,
                      currentArmadaName = wingTag,
                      currentArmadaCount = engine.getCurrentArmadaSize().coerceIn(1, 5),
                      bounty = livePilot.bounty.toInt().coerceAtLeast(100),
                    )
                  localPrefs.saveVerifiedFriendPilot(verified)
                  if (engine.activeArmadaName.isBlank()) {
                    engine.formOrJoinArmada(wingTag, listOf(verified))
                  } else {
                    engine.addMemberToArmada(verified)
                  }
                  friendsRefreshKey++
                  toastMessage = "ADDED ${livePilot.callsign} TO STRIKE WING [$wingTag]"
                  toastVersion++
                }
              },
              engine = engine,
              initialLayoutMode = nexusInitialLayoutMode,
              activeModuleKey = activeModuleKey,
              activeNode = activeNexusNode,
              onSelectNode = { nextNode -> openNexusNode(nextNode) },
              syndicateSubDeck = syndicateSubDeck,
              onSelectSyndicateSubDeck = { syndicateSubDeck = it },
              archiveSubDeck = archiveSubDeck,
              onSelectArchiveSubDeck = { archiveSubDeck = it },
              onSelectNexusLobby = { openNexusNode(NexusCoreNode.ORBITAL_HUB) },
              isCalibrationLivePeeking =
                activeNexusNode == NexusCoreNode.STAR_FORGE && RiftCalibrationState.isDraggingAnySlider,
              hideTopHeaderForFullScreenStage = isForgeFullScreenStage,
              liveResonance = engine.liveResonance,
              overwatchInstances = overwatchInstances,
              selectedOverwatchKey = effectiveOverwatchInstance.key,
              favoriteOverwatchKey = favoriteOverwatchKey,
              onSelectOverwatchKey = { nextKey ->
                if (engine.hasJoinedMatch && !engine.isEphemeralPracticeRift) {
                  sfx.playMutedClick()
                  toastMessage = "LEAVE ACTIVE MATCH FIRST TO SWITCH OVERWATCH RIFT"
                  toastVersion++
                } else {
                  userSelectedOverwatchKey = nextKey
                  overwatchZoomUi = 1.0f
                  arenaViewRef?.resetOverwatchCamera()
                  sfx.playMutedClick()
                  frameTick++
                }
              },
              onToggleFavoriteOverwatchKey = { targetKey ->
                val updatedFav = if (favoriteOverwatchKey == targetKey) "" else targetKey
                favoriteOverwatchKey = updatedFav
                localPrefs.setFavoriteOverwatchRiftKey(updatedFav)
                if (updatedFav.isNotBlank()) {
                  userSelectedOverwatchKey = updatedFav
                  toastMessage = "PINNED FAVORITE OVERWATCH // ${effectiveOverwatchInstance.riftLabel}"
                } else {
                  toastMessage = "CLEARED FAVORITE OVERWATCH PIN // AUTO HIGHEST RESONANCE"
                }
                sfx.playMutedClick()
                toastVersion++
              },
              onUpdateOverwatchViewport = { cx, cy, r ->
                arenaViewRef?.let { view ->
                  view.overwatchCenterX = cx
                  view.overwatchCenterY = cy
                  view.overwatchRadiusPx = r
                }
              },
              overwatchZoom = overwatchZoomUi,
              onOverwatchTransformGesture = { cx, cy, panDx, panDy, zoomChange ->
                val updatedZoom =
                  arenaViewRef?.applyOverwatchTransformGesture(cx, cy, panDx, panDy, zoomChange) ?: 1.0f
                overwatchZoomUi = updatedZoom
              },
              onResetOverwatchCamera = {
                arenaViewRef?.resetOverwatchCamera()
                overwatchZoomUi = 1.0f
                sfx.playMutedClick()
              },
              onQuickZoneIntoOverwatch = { inst ->
                val customCfg = inst.customConfig
                val targetMode = customCfg?.baseMode ?: inst.rankedMode
                val targetBracket =
                  customCfg?.teamSizeBracket
                    ?: when (inst.rankedMode) {
                      RankedGameMode.RIFT_SKIRMISH,
                      RankedGameMode.RIFT_CONTROL,
                      RankedGameMode.SYNDICATE_WARS -> 3
                      RankedGameMode.FLUX_CORE,
                      RankedGameMode.STASIS_BREACH -> 5
                      RankedGameMode.SECTOR_CONVERGENCE -> 20
                    }

                if (customCfg == null && inst.rankedMode == RankedGameMode.SYNDICATE_WARS) {
                  val (eligible, reason) = engine.canQueueSyndicateWars()
                  if (!eligible) {
                    sfx.playMutedClick()
                    toastMessage = reason
                    toastVersion++
                    syndicateSubDeck = SyndicateCitadelSubDeck.SYNDICATES_WARS
                    openNexusNode(NexusCoreNode.SYNDICATE_CITADEL)
                  } else if (engine.doesModeLockShipSelection(targetMode)) {
                    stageRankedModeIntoHangar(inst.rankedMode, targetBracket)
                  } else {
                    pendingStagedLaunch = null
                    engine.isShipLockedForMatch = false
                    engine.switchLocalPlayerShip(selectedShipId.coerceIn(1, 10), forceFromHangar = true)
                    syncEquippedCosmeticsToEngine(engine)
                    selectedMode = inst.rankedMode.id
                    localPrefs.setPreferredMode(inst.rankedMode.id)
                    engine.deployToRankedMode(inst.rankedMode, targetBracket)
                    syncEquippedCosmeticsToEngine(engine)
                    engine.onStatsUpdated()
                    pushLivePilotHeartbeatNow()
                    isNexusShellOpen = false
                    sfx.playPrize()
                    toastMessage = "ZONED INTO ${inst.modeTitle} // ${inst.riftLabel}"
                    toastVersion++
                    frameTick++
                  }
                } else if (engine.doesModeLockShipSelection(targetMode)) {
                  if (customCfg != null) {
                    pendingStagedLaunch =
                      if (engine.activeCustomGames.any { it.instanceId == customCfg.instanceId }) {
                        StagedRiftLaunch.ExistingCustom(customCfg.instanceId)
                      } else {
                        StagedRiftLaunch.NewCustom(customCfg)
                      }
                    selectedMode = customCfg.baseMode.id
                    sfx.playMutedClick()
                    activeNexusNode = NexusCoreNode.HANGAR_DECK
                    isNexusShellOpen = true
                    toastMessage = "STAGED ${inst.riftLabel} // LOCK HULL & ENTER RIFT"
                    toastVersion++
                  } else {
                    stageRankedModeIntoHangar(inst.rankedMode, targetBracket)
                  }
                } else {
                  pendingStagedLaunch = null
                  engine.isShipLockedForMatch = false
                  engine.switchLocalPlayerShip(selectedShipId.coerceIn(1, 10), forceFromHangar = true)
                  syncEquippedCosmeticsToEngine(engine)
                  if (customCfg != null) {
                    if (engine.activeCustomGames.any { it.instanceId == customCfg.instanceId }) {
                      engine.joinCustomGame(customCfg.instanceId)
                    } else {
                      engine.createAndJoinCustomGame(customCfg)
                    }
                    selectedMode = customCfg.baseMode.id
                    localPrefs.setPreferredMode(customCfg.baseMode.id)
                  } else {
                    selectedMode = inst.rankedMode.id
                    localPrefs.setPreferredMode(inst.rankedMode.id)
                    engine.deployToRankedMode(inst.rankedMode, targetBracket)
                  }
                  syncEquippedCosmeticsToEngine(engine)
                  engine.onStatsUpdated()
                  pushLivePilotHeartbeatNow()
                  isNexusShellOpen = false
                  sfx.playPrize()
                  toastMessage = "ZONED INTO ${inst.modeTitle} // ${inst.riftLabel}"
                  toastVersion++
                  frameTick++
                }
              },
              onStageRankedModeViaHangar = { mode, bracket ->
                stageRankedModeIntoHangar(mode, bracket)
              },
              onStageCustomGameViaHangar = { customCfg ->
                pendingStagedLaunch = StagedRiftLaunch.NewCustom(customCfg)
                selectedMode = customCfg.baseMode.id
                sfx.playMutedClick()
                activeNexusNode = NexusCoreNode.HANGAR_DECK
                isNexusShellOpen = true
                toastMessage =
                  "STAGED CUSTOM RIFT [${customCfg.title.uppercase(Locale.US)}] // SELECT HULL & ENTER RIFT"
                toastVersion++
              },
              onStageExistingCustomInstanceViaHangar = { instanceId ->
                pendingStagedLaunch = StagedRiftLaunch.ExistingCustom(instanceId)
                sfx.playMutedClick()
                activeNexusNode = NexusCoreNode.HANGAR_DECK
                isNexusShellOpen = true
                toastMessage = "STAGED CUSTOM RIFT // SELECT HULL & ENTER RIFT"
                toastVersion++
              },
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

          // Floating Live HUD Drag Pill rendered strictly once over the Arena
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
                  toastMessage = "CUSTOM GAME CALIBRATION SYNCED TO ALL TESTERS"
                  toastVersion++
                },
                onReturnToNexusCalibration = {
                  RiftCalibrationState.isUiUnlockedForDragging = false
                  RiftCalibrationState.isCalibrationModalOpen = false
                  forgeInitialTab = NexusForgeTab.DEV_STUDIO_STAGE
                  openNexusNode(NexusCoreNode.STAR_FORGE)
                },
              )
            }
          }

          if (needsInitialSwornCallsign) {
            Box(
              modifier =
                Modifier.fillMaxSize()
                  .zIndex(40f)
                  .background(Color(0xE602050B))
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
                color = Color(0xF2060D1A),
                shape = RoundedCornerShape(16.dp),
              ) {
                Column(
                  modifier = Modifier.padding(18.dp),
                  verticalArrangement = Arrangement.spacedBy(10.dp),
                  horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                  Text(
                    text = "CHOOSE YOUR SWORN PILOT CALLSIGN",
                    color = Color(0xFF00F5D4),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                  )
                  Text(
                    text =
                      "Welcome, Sworn Pilot (${localPrefs.getShortPilotTag(currentUserId)}). Choose your permanent arena callsign (3-14 chars). Your first callsign change in Pilot Dossier is also free.",
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
                            .uppercase(Locale.US)
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
                      toastMessage = "SWORN PILOT [$saved] REGISTERED & SYNCED"
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
                      text = "CONFIRM SWORN PILOT CALLSIGN",
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
