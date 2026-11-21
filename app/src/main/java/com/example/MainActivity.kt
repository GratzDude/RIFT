package com.example

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.KeyEvent
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
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
import com.example.data.CloudCustomGameRoom
import com.example.data.CloudSyncHealth
import com.example.data.FriendPilotEntry
import com.example.data.LivePilotState
import com.example.data.LocalPilotPrefs
import com.example.data.PilotAchievement
import com.example.data.RiftAvailableUpdate
import com.example.data.RiftBillingManager
import com.example.data.RiftRepository
import com.example.data.RiftUpdateManager
import com.example.data.SyndicateChatMessage
import com.example.data.UgcModeration
import com.example.engine.BrickWallEntity
import com.example.engine.CustomGameConfig
import com.example.engine.CustomHullVisualRecipe
import com.example.engine.CustomShipLoadoutSlot
import com.example.engine.FluxCoreOrbState
import com.example.engine.GreenCrate
import com.example.engine.InterdictorBubbleEntity
import com.example.engine.KillFeedEntry
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
import com.example.engine.ShipEntity
import com.example.engine.SpatialAnomalyEntity
import com.example.engine.SyndicateCrestRecipe
import com.example.engine.WallRect
import com.example.ui.ArchiveCodexSubDeck
import com.example.ui.ArmadaFleetModal
import com.example.ui.AuthScreen
import com.example.ui.CURRENT_RIFT_BUILD_VERSION
import com.example.ui.CodexModal
import com.example.ui.DossierModal
import com.example.ui.HangarModal
import com.example.ui.MatchmakingQueueStatus
import com.example.ui.MatchmakingQueueViewModel
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
import com.example.ui.extractCustomRoomCode
import com.example.ui.isPrivateCustomGame
import com.example.ui.onGoogleSignInClicked
import com.example.ui.signOut
import com.example.ui.theme.MyApplicationTheme
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.InstallStatus
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import java.util.Locale
import kotlin.math.abs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

private sealed interface StagedRiftLaunch {
  data class Ranked(
    val mode: RankedGameMode,
    val teamSizeBracket: Int,
    val sectorInstanceOverride: String? = null,
  ) : StagedRiftLaunch

  data class NewCustom(val config: CustomGameConfig) : StagedRiftLaunch

  data class ExistingCustom(val instanceId: String) : StagedRiftLaunch
}

private data class SuspendedActiveMatchSnapshot(
  val sectorTag: String,
  val activeSectorInstanceOverride: String?,
  val selectedShipId: Int,
  val selectedModeId: String,
  val mode: String,
  val activeRankedMode: RankedGameMode,
  val activeTeamSizeBracket: Int,
  val teamSize: Int,
  val isShipLockedForMatch: Boolean,
  val currentCustomGame: CustomGameConfig?,
  val arenaRadius: Float,
  val westBrigReactorX: Float,
  val eastBrigReactorX: Float,
  val westBrigGeneratorHp: Float,
  val eastBrigGeneratorHp: Float,
  val stasisBreachMatchTimerSec: Float,
  val stasisWestCaptures: Int,
  val stasisEastCaptures: Int,
  val localStasisCaptures: Int,
  val riftAlphaX: Float,
  val riftOmegaX: Float,
  val riftAlphaControlSec: Float,
  val riftOmegaControlSec: Float,
  val riftAlphaLockedBy: Int,
  val riftOmegaLockedBy: Int,
  val riftControlWestScore: Float,
  val riftControlEastScore: Float,
  val riftControlMatchTimerSec: Float,
  val skirmishRound: Int,
  val skirmishWestRoundWins: Int,
  val skirmishEastRoundWins: Int,
  val skirmishWestKills: Int,
  val skirmishEastKills: Int,
  val skirmishRoundTimerSec: Float,
  val skirmishIs1v1Escalated: Boolean,
  val skirmishAbyssalBlend: Float,
  val skirmish1v1AlertBanner: String,
  val skirmish1v1AlertTimer: Float,
  val skirmishReadyCountdownSec: Float,
  val spectatingTargetShipId: String,
  val rivalSyndicateWarTag: String,
  val fluxCoreOrbSnapshot: FluxCoreOrbState,
  val fluxCoreCarrierEntityId: String,
  val localPlayerSnapshot: ShipEntity,
  val nonRemoteShipsSnapshot: List<ShipEntity>,
  val wallsSnapshot: List<WallRect>,
  val greensSnapshot: List<GreenCrate>,
  val anomaliesSnapshot: List<SpatialAnomalyEntity>,
  val brickWallsSnapshot: List<BrickWallEntity>,
  val interdictorBubblesSnapshot: List<InterdictorBubbleEntity>,
  val killFeedSnapshot: List<KillFeedEntry>,
)

private fun captureActiveMatchSnapshot(
  engine: RiftGameEngine,
  selectedShipId: Int,
  selectedModeId: String,
): SuspendedActiveMatchSnapshot {
  return SuspendedActiveMatchSnapshot(
    sectorTag = engine.activeMatchSectorTag(),
    activeSectorInstanceOverride = engine.activeSectorInstanceOverride,
    selectedShipId = selectedShipId,
    selectedModeId = selectedModeId,
    mode = engine.mode,
    activeRankedMode = engine.activeRankedMode,
    activeTeamSizeBracket = engine.activeTeamSizeBracket,
    teamSize = engine.teamSize,
    isShipLockedForMatch = engine.isShipLockedForMatch,
    currentCustomGame = engine.currentCustomGame?.copy(),
    arenaRadius = engine.arenaRadius,
    westBrigReactorX = engine.westBrigReactorX,
    eastBrigReactorX = engine.eastBrigReactorX,
    westBrigGeneratorHp = engine.westBrigGeneratorHp,
    eastBrigGeneratorHp = engine.eastBrigGeneratorHp,
    stasisBreachMatchTimerSec = engine.stasisBreachMatchTimerSec,
    stasisWestCaptures = engine.stasisWestCaptures,
    stasisEastCaptures = engine.stasisEastCaptures,
    localStasisCaptures = engine.localStasisCaptures,
    riftAlphaX = engine.riftAlphaX,
    riftOmegaX = engine.riftOmegaX,
    riftAlphaControlSec = engine.riftAlphaControlSec,
    riftOmegaControlSec = engine.riftOmegaControlSec,
    riftAlphaLockedBy = engine.riftAlphaLockedBy,
    riftOmegaLockedBy = engine.riftOmegaLockedBy,
    riftControlWestScore = engine.riftControlWestScore,
    riftControlEastScore = engine.riftControlEastScore,
    riftControlMatchTimerSec = engine.riftControlMatchTimerSec,
    skirmishRound = engine.skirmishRound,
    skirmishWestRoundWins = engine.skirmishWestRoundWins,
    skirmishEastRoundWins = engine.skirmishEastRoundWins,
    skirmishWestKills = engine.skirmishWestKills,
    skirmishEastKills = engine.skirmishEastKills,
    skirmishRoundTimerSec = engine.skirmishRoundTimerSec,
    skirmishIs1v1Escalated = engine.skirmishIs1v1Escalated,
    skirmishAbyssalBlend = engine.skirmishAbyssalBlend,
    skirmish1v1AlertBanner = engine.skirmish1v1AlertBanner,
    skirmish1v1AlertTimer = engine.skirmish1v1AlertTimer,
    skirmishReadyCountdownSec = engine.skirmishReadyCountdownSec,
    spectatingTargetShipId = engine.spectatingTargetShipId,
    rivalSyndicateWarTag = engine.rivalSyndicateWarTag,
    fluxCoreOrbSnapshot = engine.fluxCoreOrb.copy(),
    fluxCoreCarrierEntityId = engine.fluxCoreCarrierEntityId,
    localPlayerSnapshot = engine.localPlayer.copy(),
    nonRemoteShipsSnapshot = engine.ships.filter { !it.isLocalPlayer && !it.isHuman }.map { it.copy() },
    wallsSnapshot = engine.walls.map { it.copy() },
    greensSnapshot = engine.greens.map { it.copy() },
    anomaliesSnapshot = engine.anomalies.map { it.copy() },
    brickWallsSnapshot = engine.brickWalls.map { it.copy() },
    interdictorBubblesSnapshot = engine.interdictorBubbles.map { it.copy() },
    killFeedSnapshot = engine.killFeed.map { it.copy() },
  )
}

private fun restoreActiveMatchSnapshot(
  engine: RiftGameEngine,
  snap: SuspendedActiveMatchSnapshot,
) {
  engine.isHubOverwatchActive = false
  engine.isEphemeralPracticeRift = false
  engine.practiceTutorialActive = false
  engine.practiceTutorialStepIndex = 0
  engine.practiceTutorialStepJumpFlashSec = 0f
  engine.hasJoinedMatch = true
  engine.isInLobbyStaging = true
  engine.activeSectorInstanceOverride = snap.activeSectorInstanceOverride
  engine.mode = snap.mode
  engine.activeRankedMode = snap.activeRankedMode
  engine.activeTeamSizeBracket = snap.activeTeamSizeBracket
  engine.teamSize = snap.teamSize
  engine.currentCustomGame = snap.currentCustomGame?.copy()
  engine.arenaRadius = snap.arenaRadius
  engine.westBrigReactorX = snap.westBrigReactorX
  engine.eastBrigReactorX = snap.eastBrigReactorX
  engine.westBrigRect.set(
    snap.westBrigReactorX - engine.stasisJailRadius,
    engine.westBrigReactorY - engine.stasisJailRadius,
    snap.westBrigReactorX + engine.stasisJailRadius,
    engine.westBrigReactorY + engine.stasisJailRadius,
  )
  engine.eastBrigRect.set(
    snap.eastBrigReactorX - engine.stasisJailRadius,
    engine.eastBrigReactorY - engine.stasisJailRadius,
    snap.eastBrigReactorX + engine.stasisJailRadius,
    engine.eastBrigReactorY + engine.stasisJailRadius,
  )
  engine.westBrigGeneratorHp = snap.westBrigGeneratorHp
  engine.eastBrigGeneratorHp = snap.eastBrigGeneratorHp
  engine.stasisBreachMatchTimerSec = snap.stasisBreachMatchTimerSec
  engine.stasisWestCaptures = snap.stasisWestCaptures
  engine.stasisEastCaptures = snap.stasisEastCaptures
  engine.localStasisCaptures = snap.localStasisCaptures
  engine.riftAlphaX = snap.riftAlphaX
  engine.riftOmegaX = snap.riftOmegaX
  engine.riftAlphaControlSec = snap.riftAlphaControlSec
  engine.riftOmegaControlSec = snap.riftOmegaControlSec
  engine.riftAlphaLockedBy = snap.riftAlphaLockedBy
  engine.riftOmegaLockedBy = snap.riftOmegaLockedBy
  engine.riftControlWestScore = snap.riftControlWestScore
  engine.riftControlEastScore = snap.riftControlEastScore
  engine.riftControlMatchTimerSec = snap.riftControlMatchTimerSec
  engine.skirmishRound = snap.skirmishRound
  engine.skirmishWestRoundWins = snap.skirmishWestRoundWins
  engine.skirmishEastRoundWins = snap.skirmishEastRoundWins
  engine.skirmishWestKills = snap.skirmishWestKills
  engine.skirmishEastKills = snap.skirmishEastKills
  engine.skirmishRoundTimerSec = snap.skirmishRoundTimerSec
  engine.skirmishIs1v1Escalated = snap.skirmishIs1v1Escalated
  engine.skirmishAbyssalBlend = snap.skirmishAbyssalBlend
  engine.skirmish1v1AlertBanner = snap.skirmish1v1AlertBanner
  engine.skirmish1v1AlertTimer = snap.skirmish1v1AlertTimer
  engine.skirmishReadyCountdownSec = snap.skirmishReadyCountdownSec
  engine.spectatingTargetShipId = snap.spectatingTargetShipId
  engine.rivalSyndicateWarTag = snap.rivalSyndicateWarTag
  engine.fluxCoreOrb.x = snap.fluxCoreOrbSnapshot.x
  engine.fluxCoreOrb.y = snap.fluxCoreOrbSnapshot.y
  engine.fluxCoreOrb.vx = snap.fluxCoreOrbSnapshot.vx
  engine.fluxCoreOrb.vy = snap.fluxCoreOrbSnapshot.vy
  engine.fluxCoreOrb.carrierShipId = snap.fluxCoreOrbSnapshot.carrierShipId
  engine.fluxCoreOrb.holdDurationSec = snap.fluxCoreOrbSnapshot.holdDurationSec
  engine.fluxCoreOrb.overchargeTier = snap.fluxCoreOrbSnapshot.overchargeTier
  engine.fluxCoreOrb.ejectDecelTimerSec = snap.fluxCoreOrbSnapshot.ejectDecelTimerSec
  engine.fluxCoreOrb.pickupImmunitySec = snap.fluxCoreOrbSnapshot.pickupImmunitySec
  engine.fluxCoreOrb.pulsePhase = snap.fluxCoreOrbSnapshot.pulsePhase
  engine.fluxCoreCarrierEntityId = snap.fluxCoreCarrierEntityId

  val lp = engine.localPlayer
  val sp = snap.localPlayerSnapshot
  engine.isShipLockedForMatch = false
  engine.switchLocalPlayerShip(sp.shipId, forceFromHangar = true)
  engine.isShipLockedForMatch = snap.isShipLockedForMatch
  lp.callsign = sp.callsign
  lp.syndicateTag = sp.syndicateTag
  lp.armadaName = sp.armadaName
  lp.freq = sp.freq
  lp.x = sp.x
  lp.y = sp.y
  lp.vx = sp.vx
  lp.vy = sp.vy
  lp.angle = sp.angle
  lp.weaponAngle = sp.weaponAngle
  lp.independentAimActive = sp.independentAimActive
  lp.flux = sp.flux
  lp.maxFlux = sp.maxFlux
  lp.bounty = sp.bounty
  lp.kills = sp.kills
  lp.deaths = sp.deaths
  lp.gunTimer = sp.gunTimer
  lp.bombTimer = sp.bombTimer
  lp.empTimer = sp.empTimer
  lp.respawnTimer = sp.respawnTimer
  lp.deathX = sp.deathX
  lp.deathY = sp.deathY
  lp.afterburning = false
  lp.multifireOn = sp.multifireOn
  lp.repels = sp.repels
  lp.specialWeaponType = sp.specialWeaponType
  lp.specialWeaponCharges = sp.specialWeaponCharges
  lp.specialWeaponStacks = sp.specialWeaponStacks
  lp.multiShotCharges = sp.multiShotCharges
  lp.hasBouncingBulletsUpgrade = sp.hasBouncingBulletsUpgrade
  lp.speedOverchargeStacks = sp.speedOverchargeStacks
  lp.thrustOverchargeStacks = sp.thrustOverchargeStacks
  lp.rechargeOverchargeStacks = sp.rechargeOverchargeStacks
  lp.turnOverchargeStacks = sp.turnOverchargeStacks
  lp.maxFluxOverchargeStacks = sp.maxFluxOverchargeStacks
  lp.sniperOverchargeReady = sp.sniperOverchargeReady
  lp.isCloaked = sp.isCloaked
  lp.rocketBoostTimer = sp.rocketBoostTimer
  lp.phaseDashTimer = sp.phaseDashTimer
  lp.wasInSafeZone = sp.wasInSafeZone
  lp.inSafeZone = sp.inSafeZone
  lp.safeZoneLockoutTimer = sp.safeZoneLockoutTimer
  lp.weaponsLocked = sp.weaponsLocked
  lp.phaseShieldTimer = sp.phaseShieldTimer
  lp.canFire = sp.canFire
  lp.weaponState = sp.weaponState
  lp.blackHoleImmunityTimer = sp.blackHoleImmunityTimer
  lp.phaseRiftCooldownTimer = sp.phaseRiftCooldownTimer
  lp.safeZoneStayTimer = 0f
  lp.playfieldIdleTimer = 0f
  lp.isIdleDraining = false
  lp.matchPresenceTimeSec = sp.matchPresenceTimeSec
  lp.isInStasisBrig = sp.isInStasisBrig
  lp.deathLingerTimer = sp.deathLingerTimer
  lp.pendingRespawnBountyPenalty = sp.pendingRespawnBountyPenalty

  engine.ships.clear()
  engine.ships.add(lp)
  engine.ships.addAll(snap.nonRemoteShipsSnapshot.map { it.copy() })
  engine.walls.clear()
  engine.walls.addAll(snap.wallsSnapshot.map { it.copy() })
  engine.greens.clear()
  engine.greens.addAll(snap.greensSnapshot.map { it.copy() })
  engine.anomalies.clear()
  engine.anomalies.addAll(snap.anomaliesSnapshot.map { it.copy() })
  engine.brickWalls.clear()
  engine.brickWalls.addAll(snap.brickWallsSnapshot.map { it.copy() })
  engine.interdictorBubbles.clear()
  engine.interdictorBubbles.addAll(snap.interdictorBubblesSnapshot.map { it.copy() })
  engine.killFeed.clear()
  engine.killFeed.addAll(snap.killFeedSnapshot.map { it.copy() })
  engine.reattachRemotePilotsToRoster()
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
fun DeviceAwareModalAutoScaler(
  bypassScaling: Boolean = false,
  content: @Composable () -> Unit,
) {
  BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val baseDensity = LocalDensity.current
    val scaledDensity =
      if (bypassScaling) {
        remember(baseDensity.density, baseDensity.fontScale) {
          Density(
            density = baseDensity.density,
            fontScale = baseDensity.fontScale.coerceAtMost(1.08f),
          )
        }
      } else {
        val safeH = maxHeight.value.coerceAtLeast(1f)
        val safeW = maxWidth.value.coerceAtLeast(1f)
        val aspect = safeW / safeH
        val refW =
          when {
            aspect < 1.45f -> 900f
            aspect < 1.70f -> 980f
            else -> 1060f
          }
        val scaleH = (safeH / 560f).coerceIn(0.62f, 1.25f)
        val scaleW = (safeW / refW).coerceIn(0.62f, 1.25f)
        val scale = minOf(scaleH, scaleW)
        remember(baseDensity.density, baseDensity.fontScale, scale) {
          Density(
            density = baseDensity.density * scale,
            fontScale = baseDensity.fontScale.coerceAtMost(1.08f),
          )
        }
      }
    CompositionLocalProvider(LocalDensity provides scaledDensity) {
      content()
    }
  }
}

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    enableEdgeToEdge()
    super.onCreate(savedInstanceState)
    configureAppCheck(this, intent)
    if (requestedOrientation != ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE) {
      requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
    }
    window.decorView.post { hideSystemBars() }

    setContent {
      MyApplicationTheme {
        AppNavigation()
      }
    }
  }

  override fun onResume() {
    super.onResume()
    RiftSoundSynthesizer.notifyAppForegroundState(true)
    window.decorView.post { hideSystemBars() }
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
      window.decorView.post { hideSystemBars() }
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
fun RiftTitleLoadingScreen(
  progress: Float,
  statusLabel: String,
) {
  val clampedProgress = progress.coerceIn(0.05f, 1.0f)
  Box(
    modifier =
      Modifier.fillMaxSize()
        .background(Color(0xFF060D1A))
        .testTag("rift_startup_loading_screen"),
    contentAlignment = Alignment.Center,
  ) {
    // Full-bleed atmospheric backdrop fill so ultrawide phones & 4:3 tablets never show harsh letterbox bars
    Image(
      painter = painterResource(id = R.drawable.rift_title_loading),
      contentDescription = null,
      contentScale = ContentScale.Crop,
      alpha = 0.36f,
      modifier = Modifier.fillMaxSize(),
    )
    Box(
      modifier =
        Modifier.fillMaxSize()
          .background(
            Brush.radialGradient(
              colors = listOf(Color(0x22060D1A), Color(0x99040914), Color(0xE602050B)),
            )
          )
    )
    // Full uncropped RIFT key art centered cleanly across any aspect ratio
    Image(
      painter = painterResource(id = R.drawable.rift_title_loading),
      contentDescription = "RIFT Loading Artwork",
      contentScale = ContentScale.Fit,
      modifier = Modifier.fillMaxSize(),
    )

    // Clean plain-English bottom loading status & progress bar
    Column(
      modifier =
        Modifier.align(Alignment.BottomCenter)
          .windowInsetsPadding(WindowInsets.safeDrawing)
          .padding(horizontal = 20.dp, vertical = 14.dp)
          .widthIn(max = 460.dp)
          .fillMaxWidth()
          .clip(RoundedCornerShape(10.dp))
          .background(Color(0xDB060D1A))
          .border(
            1.dp,
            Brush.horizontalGradient(listOf(Color(0xFF00F5D4), Color(0xFFD946EF), Color(0xFF38BDF8))),
            RoundedCornerShape(10.dp),
          )
          .padding(horizontal = 14.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = statusLabel,
          color = Color(0xFFF8FAFC),
          fontSize = 10.5.sp,
          fontWeight = FontWeight.ExtraBold,
          fontFamily = FontFamily.Monospace,
          maxLines = 1,
        )
        Text(
          text = "v${BuildConfig.VERSION_NAME}",
          color = Color(0xFF00F5D4),
          fontSize = 10.sp,
          fontWeight = FontWeight.Black,
          fontFamily = FontFamily.Monospace,
          maxLines = 1,
        )
      }
      Box(
        modifier =
          Modifier.fillMaxWidth()
            .height(5.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(Color(0xFF0F1E36))
      ) {
        Box(
          modifier =
            Modifier.fillMaxHeight()
              .fillMaxWidth(clampedProgress)
              .clip(RoundedCornerShape(3.dp))
              .background(
                Brush.horizontalGradient(
                  listOf(Color(0xFF00F5D4), Color(0xFF38BDF8), Color(0xFFE879F9))
                )
              )
        )
      }
    }
  }
}

@Composable
private fun RiftUpdateAvailableBanner(
  update: RiftAvailableUpdate,
  onApplyOrDownload: () -> Unit,
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier =
      modifier
        .windowInsetsPadding(WindowInsets.safeDrawing)
        .padding(horizontal = 12.dp, vertical = 6.dp)
        .widthIn(max = 640.dp)
        .fillMaxWidth()
        .border(
          1.2.dp,
          Brush.horizontalGradient(listOf(Color(0xFF00F5D4), Color(0xFFFACC15))),
          RoundedCornerShape(10.dp),
        )
        .testTag("rift_update_available_banner"),
    color = Color(0xF2060D1A),
    shape = RoundedCornerShape(10.dp),
    tonalElevation = 8.dp,
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 7.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(
        modifier = Modifier.weight(1f).padding(end = 10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
      ) {
        Text(
          text = "NEW RIFT UPDATE AVAILABLE // ${update.versionLabel.uppercase(Locale.US)}",
          color = Color(0xFF00F5D4),
          fontSize = 10.5.sp,
          fontWeight = FontWeight.Black,
          fontFamily = FontFamily.Monospace,
          maxLines = 1,
        )
        Text(
          text = update.headline,
          color = Color(0xFFE2E8F0),
          fontSize = 9.5.sp,
          fontWeight = FontWeight.SemiBold,
          fontFamily = FontFamily.Monospace,
          maxLines = 1,
        )
      }
      Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Button(
          onClick = onApplyOrDownload,
          colors =
            ButtonDefaults.buttonColors(
              containerColor = Color(0xFF00F5D4),
              contentColor = Color(0xFF030A14),
            ),
          shape = RoundedCornerShape(7.dp),
          contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
          modifier = Modifier.height(34.dp).testTag("rift_update_banner_action_btn"),
        ) {
          Text(
            text =
              when {
                update.isPlayUpdateDownloaded -> "RESTART & UPDATE"
                update.isPlayStoreInstall -> "UPDATE ON PLAY"
                else -> "DOWNLOAD UPDATE"
              },
            fontSize = 9.6.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
          )
        }
        Button(
          onClick = onDismiss,
          colors =
            ButtonDefaults.buttonColors(
              containerColor = Color(0xFF0E1D33),
              contentColor = Color(0xFFCBD5E1),
            ),
          border = BorderStroke(1.dp, Color(0xFF475569)),
          shape = RoundedCornerShape(7.dp),
          contentPadding = PaddingValues(horizontal = 9.dp, vertical = 4.dp),
          modifier = Modifier.height(34.dp).testTag("rift_update_banner_dismiss_btn"),
        ) {
          Text(
            text = "LATER",
            fontSize = 9.2.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
          )
        }
      }
    }
  }
}

@Composable
fun AppNavigation(
  auth: FirebaseAuth = Firebase.auth,
) {
  val context = LocalContext.current
  val navScope = rememberCoroutineScope()
  val navCredentialManager = remember { CredentialManager.create(context) }
  val localPrefs = remember { LocalPilotPrefs(context) }
  val permanentDeviceUid = remember { localPrefs.getOrCreatePermanentDeviceUid() }

  val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
  var isGuestMode by remember { mutableStateOf(localPrefs.isGuestBypassEnabled()) }
  var drifterSessionEpoch by remember { mutableIntStateOf(0) }
  var showPreAuthChangelog by remember { mutableStateOf(false) }
  var isStartupAssetsReady by remember { mutableStateOf(false) }
  var startupProgress by remember { mutableFloatStateOf(0.16f) }
  var startupStatusText by remember { mutableStateOf("Loading RIFT Pilot Profile...") }

  val isPlayStoreInstall = remember(context) { RiftUpdateManager.isInstalledFromGooglePlay(context) }
  val appUpdateManager =
    remember(context, isPlayStoreInstall) {
      if (isPlayStoreInstall && !RiftUpdateManager.isHeadlessOrTestRuntime()) {
        runCatching { AppUpdateManagerFactory.create(context) }.getOrNull()
      } else {
        null
      }
    }
  var availableUpdate by remember { mutableStateOf<RiftAvailableUpdate?>(null) }
  var updateStatusLabel by remember { mutableStateOf("v${BuildConfig.VERSION_NAME} UP TO DATE") }
  var updateBannerDismissed by remember { mutableStateOf(false) }

  val playUpdateLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { _ -> }

  DisposableEffect(appUpdateManager) {
    if (appUpdateManager == null) {
      onDispose { }
    } else {
      val listener = InstallStateUpdatedListener { state ->
        if (state.installStatus() == InstallStatus.DOWNLOADED) {
          availableUpdate =
            RiftAvailableUpdate(
              versionLabel = "Google Play Update",
              headline = "Update downloaded. Tap Restart & Update to finish installing.",
              downloadUrl = "",
              isPlayStoreInstall = true,
              isPlayUpdateDownloaded = true,
            )
          updateStatusLabel = "PLAY UPDATE READY TO INSTALL"
          updateBannerDismissed = false
        }
      }
      runCatching { appUpdateManager.registerListener(listener) }
      onDispose { runCatching { appUpdateManager.unregisterListener(listener) } }
    }
  }

  val executeUpdateCheck: (Boolean, Boolean) -> Unit = { autoPromptPlay, resetDismiss ->
    navScope.launch {
      updateStatusLabel = "CHECKING FOR UPDATES..."
      val (foundUpdate, statusMsg) =
        if (isPlayStoreInstall && appUpdateManager != null) {
          RiftUpdateManager.checkAndPromptPlayStoreUpdate(
            appUpdateManager = appUpdateManager,
            launcher = playUpdateLauncher,
            autoLaunchPrompt = autoPromptPlay,
          )
        } else if (!isPlayStoreInstall) {
          RiftUpdateManager.checkDirectReleaseUpdate()
        } else {
          null to "v${BuildConfig.VERSION_NAME} UP TO DATE"
        }
      availableUpdate = foundUpdate
      updateStatusLabel = statusMsg
      if (foundUpdate != null && resetDismiss) {
        updateBannerDismissed = false
      }
    }
  }

  val applyOrDownloadAvailableUpdate: () -> Unit = {
    val upd = availableUpdate
    when {
      upd == null -> executeUpdateCheck(true, true)
      upd.isPlayUpdateDownloaded && appUpdateManager != null -> {
        runCatching { appUpdateManager.completeUpdate() }
      }
      upd.isPlayStoreInstall && appUpdateManager != null -> {
        executeUpdateCheck(true, true)
      }
      upd.downloadUrl.isNotBlank() -> {
        RiftUpdateManager.openExternalUpdateUrl(context, upd.downloadUrl)
      }
    }
  }

  val handleSwitchAccount: () -> Unit = {
    localPrefs.clearUnscopedCallsignOnAccountSwitch()
    localPrefs.setGuestBypassEnabled(false)
    isGuestMode = false
    signOut(
      credentialManager = navCredentialManager,
      scope = navScope,
      onSignOutComplete = {
        localPrefs.clearUnscopedCallsignOnAccountSwitch()
        localPrefs.setGuestBypassEnabled(false)
        isGuestMode = false
      },
    )
  }

  val user = currentUser

  LaunchedEffect(Unit) {
    // Allow ViewRootImpl to immediately commit initial frames to satisfy WMS SurfaceSyncGroup (<30ms)
    withFrameNanos { }
    withFrameNanos { }
    // Fire non-blocking automatic on-open update check in parallel
    executeUpdateCheck(true, false)

    val isHeadless = RiftUpdateManager.isHeadlessOrTestRuntime()
    startupProgress = 0.34f
    startupStatusText = "Preparing 2.5D Starfighters & Rift Station..."
    val prewarmUid = auth.currentUser?.uid ?: permanentDeviceUid
    withContext(Dispatchers.Default) {
      val preferredShip = localPrefs.getPreferredShipId().coerceIn(1, 10)
      val equippedTrim = localPrefs.getEquippedCosmeticId("HULL_TRIM", prewarmUid).coerceIn(0, 3)
      val activeSlotId = localPrefs.getActiveCustomSlotId(prewarmUid)
      val loadedSlot = localPrefs.getCustomShipSlot(activeSlotId, prewarmUid)
      val savedTag = localPrefs.getSyndicateTag(prewarmUid)
      RiftSpriteBaker.activeHullTrimStyle = equippedTrim
      RiftCodex.updateActiveCustomShipSlot(loadedSlot)
      RiftSpriteBaker.prewarmStartupAssets(
        activeShipId = preferredShip,
        trimStyle = equippedTrim,
        syndicateTag = savedTag,
      )
    }
    startupProgress = 0.88f
    startupStatusText = "Calibrating Sector Shaders & Audio..."
    if (!isHeadless) {
      delay(420L)
    }
    startupProgress = 1.0f
    startupStatusText = "Ready to Rift In"
    if (!isHeadless) {
      delay(180L)
    }
    isStartupAssetsReady = true
  }

  Box(modifier = Modifier.fillMaxSize().background(Color(0xFF060D1A))) {
    if (!isStartupAssetsReady) {
      RiftTitleLoadingScreen(
        progress = startupProgress,
        statusLabel = startupStatusText,
      )
      return@Box
    }
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
          onSwitchAccount = handleSwitchAccount,
          updateStatusLabel = updateStatusLabel,
          hasUpdateAvailable = availableUpdate != null,
          onCheckOrApplyUpdate = applyOrDownloadAvailableUpdate,
        )
      }
    }

    val bannerUpdate = availableUpdate
    if (bannerUpdate != null && !updateBannerDismissed) {
      RiftUpdateAvailableBanner(
        update = bannerUpdate,
        onApplyOrDownload = applyOrDownloadAvailableUpdate,
        onDismiss = { updateBannerDismissed = true },
        modifier = Modifier.align(Alignment.TopCenter).zIndex(45f),
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
  onOpenChangelog: () -> Unit = {},
  onSwitchAccount: () -> Unit = {},
  updateStatusLabel: String = "v${BuildConfig.VERSION_NAME} UP TO DATE",
  hasUpdateAvailable: Boolean = false,
  onCheckOrApplyUpdate: () -> Unit = {},
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
  val sectorCombatEvents by viewModel.sectorCombatEventsState.collectAsStateWithLifecycle()
  val sectorInstanceState by viewModel.sectorInstanceState.collectAsStateWithLifecycle()
  val cloudSyncHealth by viewModel.cloudSyncHealth.collectAsStateWithLifecycle()
  val cloudSyndicatesState by viewModel.cloudSyndicatesState.collectAsStateWithLifecycle()
  val sectorCommsState by viewModel.sectorCommsState.collectAsStateWithLifecycle()
  val latestCalibBroadcast by viewModel.latestCalibrationBroadcast.collectAsStateWithLifecycle()
  val publicCustomRoomsState by viewModel.publicCustomGamesState.collectAsStateWithLifecycle()
  val cloudPublicCustomConfigs =
    remember(publicCustomRoomsState) {
      publicCustomRoomsState.map { it.toCustomGameConfig() }
    }

  val nexusViewModel: NexusAiViewModel = viewModel(key = "nexus_$currentUserId")
  val nexusMessages by nexusViewModel.messages.collectAsStateWithLifecycle()
  val nexusStreamState by nexusViewModel.streamState.collectAsStateWithLifecycle()
  val isNexusStreaming =
    nexusStreamState is NexusStreamState.Loading || nexusStreamState is NexusStreamState.Streaming
  val matchmakingQueueViewModel: MatchmakingQueueViewModel =
    viewModel(key = "matchmaking_${currentUserId}_$isGoogleAuthenticated")
  val matchmakingQueueState by matchmakingQueueViewModel.uiState.collectAsStateWithLifecycle()
  val isDeviceOnline by
    remember(context) { com.example.data.NetworkConnectivityMonitor.observeOnlineState(context) }
      .collectAsStateWithLifecycle(
        initialValue = com.example.data.NetworkConnectivityMonitor.isCurrentlyOnline(context)
      )

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
        "RIFT HUB ONLINE // SELECT RIFT TO DEPLOY"
      }
    )
  }
  var toastVersion by remember { mutableIntStateOf(0) }
  var unreadCommsCount by remember { mutableIntStateOf(0) }

  var bootToNexusHub by remember { mutableStateOf(true) }
  var isNexusShellOpen by remember { mutableStateOf(!initialStartInPracticeRift) }
  var activeNexusNode by remember { mutableStateOf(NexusCoreNode.ORBITAL_HUB) }
  var hubRequestedSideBay by remember { mutableStateOf("HUB_MAIN") }
  var hubSideBayRequestNonce by remember { mutableIntStateOf(0) }
  var syndicateSubDeck by remember { mutableStateOf(SyndicateCitadelSubDeck.SYNDICATES_WARS) }
  var archiveSubDeck by remember { mutableStateOf(ArchiveCodexSubDeck.PILOT_DOSSIER) }
  var helmInitialSubDeck by remember { mutableIntStateOf(0) }
  var previousNodeBeforeConduit by remember { mutableStateOf(NexusCoreNode.ORBITAL_HUB) }
  var nexusInitialLayoutMode by remember { mutableStateOf(NexusHubLayoutMode.DECK_ONLY) }
  var forgeInitialTab by remember { mutableStateOf(NexusForgeTab.DEV_STUDIO_STAGE) }
  var isForgeFullScreenStage by remember { mutableStateOf(false) }
  var showFullChangelogModal by remember { mutableStateOf(false) }
  var pendingStagedLaunch by remember { mutableStateOf<StagedRiftLaunch?>(null) }
  var suspendedActiveMatch by remember { mutableStateOf<SuspendedActiveMatchSnapshot?>(null) }
  var favoriteOverwatchKey by remember {
    mutableStateOf(localPrefs.getFavoriteOverwatchRiftKey())
  }
  var userSelectedOverwatchKey by remember { mutableStateOf("") }

  var callsign by remember(currentUserId, isGoogleAuthenticated) {
    mutableStateOf(
      if (isGoogleAuthenticated) localPrefs.getCallsign(currentUserId) else localPrefs.getCallsign()
    )
  }
  var renamedCallsignInCurrentSession by remember(currentUserId, isGoogleAuthenticated) {
    mutableStateOf(false)
  }
  var needsInitialSwornCallsign by remember(currentUserId, isGoogleAuthenticated) {
    mutableStateOf(false)
  }
  var initialSwornCallsignDraft by remember(callsign) {
    mutableStateOf(if (callsign.equals("COMMANDER", ignoreCase = true)) "" else callsign)
  }
  var syndicateTag by remember(currentUserId, isGoogleAuthenticated) {
    mutableStateOf(
      if (isGoogleAuthenticated) localPrefs.getSyndicateTag(currentUserId)
      else localPrefs.getSyndicateTag()
    )
  }
  var syndicateName by remember(currentUserId, isGoogleAuthenticated) {
    mutableStateOf(
      if (isGoogleAuthenticated) localPrefs.getSyndicateName(currentUserId)
      else localPrefs.getSyndicateName()
    )
  }
  var selectedShipId by remember(currentUserId) {
    mutableIntStateOf(localPrefs.getBestShipId(currentUserId, 1).coerceIn(1, 10))
  }
  var hasExplicitHangarLock by remember(currentUserId, isGoogleAuthenticated) {
    mutableStateOf(false)
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
  var shipResonancesState by remember(currentUserId) {
    mutableStateOf(localPrefs.getAllShipResonances(currentUserId))
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
      loadShipResonances(localPrefs.getAllShipResonances(currentUserId), liveResonance)
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

  fun openNexusNode(node: NexusCoreNode, helmSubDeckOverride: Int? = null) {
    if (engine.isEphemeralPracticeRift) {
      engine.closeEphemeralPracticeRift()
      val snap = suspendedActiveMatch
      if (snap != null) {
        restoreActiveMatchSnapshot(engine, snap)
        selectedShipId = snap.selectedShipId
        selectedMode = snap.selectedModeId
        syncEquippedCosmeticsToEngine(engine)
        suspendedActiveMatch = null
        viewModel.setActiveSector(engine.activeMatchSectorTag())
        frameTick++
      }
    }
    val resolvedNode =
      if (
        node == NexusCoreNode.ARCHIVE_CODEX &&
          archiveSubDeck == ArchiveCodexSubDeck.TACTICAL_CODEX
      ) {
        archiveSubDeck = ArchiveCodexSubDeck.PILOT_DOSSIER
        NexusCoreNode.HELM_SYSTEMS
      } else {
        node
      }
    if (resolvedNode == NexusCoreNode.HELM_SYSTEMS) {
      helmInitialSubDeck =
        helmSubDeckOverride
          ?: if (node == NexusCoreNode.ARCHIVE_CODEX) 3 else helmInitialSubDeck.takeIf { it in 0..3 } ?: 0
    }
    if (resolvedNode == NexusCoreNode.ARCHIVE_CODEX) {
      archiveSubDeck = ArchiveCodexSubDeck.PILOT_DOSSIER
    }
    if (resolvedNode == NexusCoreNode.CONVERGENCE_GATE) {
      pendingStagedLaunch = null
    }
    when (resolvedNode) {
      NexusCoreNode.SUBSPACE_COMMS -> {
        unreadCommsCount = 0
        isForgeFullScreenStage = false
        viewModel.setActiveSector(engine.activeMatchSectorTag())
        activeNexusNode = NexusCoreNode.SUBSPACE_COMMS
      }
      NexusCoreNode.STAR_FORGE -> {
        isForgeFullScreenStage = (forgeInitialTab == NexusForgeTab.DEV_STUDIO_STAGE)
        activeNexusNode = NexusCoreNode.STAR_FORGE
      }
      NexusCoreNode.ORBITAL_HUB -> {
        isForgeFullScreenStage = false
        hubRequestedSideBay = "HUB_MAIN"
        hubSideBayRequestNonce++
        activeNexusNode = NexusCoreNode.ORBITAL_HUB
      }
      NexusCoreNode.HANGAR_DECK -> {
        isForgeFullScreenStage = false
        hubRequestedSideBay = "HANGAR_BAY"
        hubSideBayRequestNonce++
        activeNexusNode = NexusCoreNode.ORBITAL_HUB
      }
      NexusCoreNode.CONVERGENCE_GATE -> {
        isForgeFullScreenStage = false
        hubRequestedSideBay = "GAME_MODES_BAY"
        hubSideBayRequestNonce++
        activeNexusNode = NexusCoreNode.ORBITAL_HUB
      }
      NexusCoreNode.RIFT_CONDUIT -> {
        isForgeFullScreenStage = false
        hubRequestedSideBay = "CONDUIT_BAY"
        hubSideBayRequestNonce++
        activeNexusNode = NexusCoreNode.ORBITAL_HUB
      }
      NexusCoreNode.SYNDICATE_CITADEL -> {
        isForgeFullScreenStage = false
        hubRequestedSideBay =
          if (syndicateSubDeck == SyndicateCitadelSubDeck.STRIKE_WING) "SQUADRON_BAY"
          else "SYNDICATE_BAY"
        hubSideBayRequestNonce++
        activeNexusNode = NexusCoreNode.ORBITAL_HUB
      }
      NexusCoreNode.ARCHIVE_CODEX -> {
        isForgeFullScreenStage = false
        hubRequestedSideBay = "DOSSIER_BAY"
        hubSideBayRequestNonce++
        activeNexusNode = NexusCoreNode.ORBITAL_HUB
      }
      NexusCoreNode.HELM_SYSTEMS -> {
        isForgeFullScreenStage = false
        hubRequestedSideBay = "HELM_CODEX_BAY"
        hubSideBayRequestNonce++
        activeNexusNode = NexusCoreNode.ORBITAL_HUB
      }
    }
    isNexusShellOpen = true
  }

  DisposableEffect(engine, viewModel) {
    syncEquippedCosmeticsToEngine(engine)
    engine.onShipResonanceChanged = { shipId, shipRes, overallRes ->
      if (!engine.isEphemeralPracticeRift) {
        localPrefs.setShipResonance(shipId, shipRes, currentUserId)
        localPrefs.setLiveResonance(overallRes, currentUserId)
        localPrefs.setPeakResonance(engine.peakResonance, currentUserId)
        shipResonancesState = localPrefs.getAllShipResonances(currentUserId)
      }
    }
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
      if (isGoogleAuthenticated && isDeviceOnline) {
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
    engine.onOutboundCombatEvent = { evt ->
      if (isGoogleAuthenticated && isDeviceOnline && !engine.isEphemeralPracticeRift) {
        viewModel.emitCombatEvent(evt)
      }
    }
    onDispose {
      engine.onOutboundCombatEvent = {}
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
  LaunchedEffect(syndicateTag, isGoogleAuthenticated, isDeviceOnline) {
    if (!isGoogleAuthenticated || !isDeviceOnline) return@LaunchedEffect
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
  LaunchedEffect(
    activeCrestRecipe,
    syndicateChannelsRefreshKey,
    syndicateTag,
    isGoogleAuthenticated,
    isDeviceOnline,
  ) {
    val cleanSyn = RiftGameEngine.cleanTag(syndicateTag)
    if (cleanSyn.isEmpty() || cleanSyn in setOf("GUEST", "FREE", "RFT", "BOT", "SOLO")) {
      return@LaunchedEffect
    }
    val crestJson = activeCrestRecipe.toJson().toString()
    localPrefs.setSyndicateCrestJson(cleanSyn, crestJson)
    if (isGoogleAuthenticated && isDeviceOnline) {
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
      val scrubbedTag = localPrefs.getSyndicateTag(currentUserId)
      val scrubbedName = localPrefs.getSyndicateName(currentUserId)
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
    val dossierBelongsToCurrentPilot =
      cloudDossier != null &&
        (cloudDossier.userId.isBlank() || cloudDossier.userId == currentUserId)
    if (cloudDossier != null && dossierBelongsToCurrentPilot) {
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
        val cleanedLocalSyn = localPrefs.getSyndicateTag(currentUserId)
        if (syndicateTag != cleanedLocalSyn) {
          syndicateTag = cleanedLocalSyn
          syndicateName = localPrefs.getSyndicateName(currentUserId)
          engine.localPlayer.syndicateTag = syndicateTag
          engine.syncLocalArmadaSelfSlot()
        }
        val cloudSynTag = localPrefs.sanitizeSyndicateTag(cloudDossier.syndicateTag)
        val nonPlayerTags =
          LocalPilotPrefs.NON_PLAYER_SYNDICATE_TAGS + RiftGameEngine.LEGACY_FOUNDRY_TAGS
        if (
          cloudSynTag.length in 1..3 &&
            cloudSynTag !in nonPlayerTags &&
            !localPrefs.isGhostSyndicateName(cloudDossier.syndicateName) &&
            syndicateTag.isBlank()
        ) {
          val resolvedSynName =
            cloudDossier.syndicateName
              .trim()
              .takeIf { it.isNotBlank() && !localPrefs.isGhostSyndicateName(it) }
              ?: "$cloudSynTag Syndicate"
          localPrefs.joinOfficialSyndicate(
            tag = cloudSynTag,
            name = resolvedSynName,
            apply15DayLock = false,
            uid = currentUserId,
          )
          syndicateTag = localPrefs.getSyndicateTag(currentUserId)
          syndicateName = localPrefs.getSyndicateName(currentUserId)
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
          if (!renamedCallsignInCurrentSession) {
            val adopted = localPrefs.setCallsign(cloudCallsign, currentUserId)
            if (callsign != adopted || engine.localPlayer.callsign != adopted) {
              callsign = adopted
              initialSwornCallsignDraft = adopted
              engine.localPlayer.callsign = adopted
              engine.syncLocalArmadaSelfSlot()
              if (isDeviceOnline) {
                viewModel.syncNativePilotStats(
                  callsign = adopted,
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
        } else if (localHasChosenName && cloudCallsign != callsign) {
          val saved = localPrefs.setCallsign(callsign, currentUserId)
          localPrefs.setHasChosenInitialCallsign(true, currentUserId)
          needsInitialSwornCallsign = false
          engine.localPlayer.callsign = saved
          if (isDeviceOnline) {
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
      for (pilot in allOnlineHumansState + sectorList) {
        val pUid = pilot.userId.ifEmpty { pilot.id }
        if (pUid.isBlank() || pUid in blockedPilotUids) continue
        val prev = byUid[pUid]
        if (prev == null) {
          byUid[pUid] = pilot
        } else {
          val prevMs = prev.updatedAt?.toDate()?.time ?: 0L
          val curMs = pilot.updatedAt?.toDate()?.time ?: 0L
          if (curMs > prevMs || (curMs == prevMs && pilot.localSyncSeq >= prev.localSyncSeq)) {
            byUid[pUid] = pilot
          }
        }
      }
      byUid.values.toList()
    }

  fun pushLivePilotHeartbeatNow() {
    if (!isGoogleAuthenticated || !isDeviceOnline) return
    val suspended = suspendedActiveMatch
    val queueTargetSector =
      matchmakingQueueState.targetSectorKey.takeIf { matchmakingQueueState.isQueueActive }
    val p = suspended?.localPlayerSnapshot ?: engine.localPlayer
    val activeLiveMatch =
      suspended != null || (engine.hasJoinedMatch && !engine.isEphemeralPracticeRift)
    val safeCallsign =
      p.callsign.trim().ifBlank { callsign.trim() }.ifBlank { "PILOT" }.take(24).let {
        if (it.length < 2) "PILOT" else it
      }
    val broadcastSector =
      when {
        !queueTargetSector.isNullOrBlank() -> queueTargetSector
        suspended != null -> suspended.sectorTag
        else -> engine.activeMatchSectorTag()
      }
    viewModel.setActiveSector(broadcastSector)
    val cleanLiveSynTag = localPrefs.sanitizeSyndicateTag(p.syndicateTag)
    viewModel.syncNativeLiveState(
      sectorId = broadcastSector,
      callsign = safeCallsign,
      syndicateTag = cleanLiveSynTag,
      armadaName = p.armadaName.take(12),
      shipId = p.shipId.coerceIn(1, 10),
      freq = p.freq.coerceIn(0, 99),
      x = p.x.coerceIn(-99000f, 99000f),
      y = p.y.coerceIn(-99000f, 99000f),
      vx = if (suspended != null) 0f else p.vx.coerceIn(-4900f, 4900f),
      vy = if (suspended != null) 0f else p.vy.coerceIn(-4900f, 4900f),
      angle = p.angle.coerceIn(-99f, 99f),
      weaponAngle = p.weaponAngle.coerceIn(-99f, 99f),
      isFiringPrimary =
        suspended == null &&
          activeLiveMatch &&
          engine.fireStickActive &&
          !engine.isCancelActive &&
          p.canFire &&
          p.respawnTimer <= 0f,
      isAfterburning = suspended == null && activeLiveMatch && p.afterburning && p.respawnTimer <= 0f,
      isCloaked = activeLiveMatch && p.isCloaked,
      isRespawning = activeLiveMatch && p.respawnTimer > 0f,
      isInStasisBrig = activeLiveMatch && p.isInStasisBrig,
      secondarySeq = engine.localSecondaryFireSeq,
      secondaryType = p.spec.bombType,
      secondaryAim = p.weaponAngle.coerceIn(-99f, 99f),
      flux = p.flux.coerceIn(0f, 10000f),
      maxFlux = p.maxFlux.coerceIn(100f, 10000f),
      bounty = p.bounty.coerceAtLeast(0),
      liveResonance = engine.liveResonance.coerceIn(0, 1000000),
    )
  }

  DisposableEffect(engine, currentUserId, isGoogleAuthenticated, isDeviceOnline) {
    var lastLifecyclePushElapsedMs = 0L
    engine.onLocalLifecycleSyncNeeded = {
      val nowElapsed = android.os.SystemClock.elapsedRealtime()
      if (nowElapsed - lastLifecyclePushElapsedMs >= 30L) {
        lastLifecyclePushElapsedMs = nowElapsed
        pushLivePilotHeartbeatNow()
      }
    }
    onDispose {
      engine.onLocalLifecycleSyncNeeded = {}
    }
  }

  LaunchedEffect(
    currentUserId,
    callsign,
    syndicateTag,
    engine.activeArmadaName,
    selectedShipId,
    engine.liveResonance,
    isDeviceOnline,
    mergedLivePilots,
    matchmakingQueueState.isQueueActive,
  ) {
    matchmakingQueueViewModel.updateLobbySnapshot(
      currentUserId = currentUserId,
      localCallsign = callsign,
      localSyndicateTag = syndicateTag,
      localArmadaName = engine.activeArmadaName,
      localShipId = selectedShipId,
      localLiveResonance = engine.liveResonance,
      isDeviceOnline = isDeviceOnline,
      mergedLivePilots = mergedLivePilots,
    )
  }

  fun enqueueOrExecuteResonanceMatchmaking(
    modeTitle: String,
    riftLabel: String,
    targetSectorKey: String,
    rankedMode: RankedGameMode,
    isCustomInstance: Boolean,
    onExecuteLaunch: () -> Unit,
  ) {
    matchmakingQueueViewModel.updateLobbySnapshot(
      currentUserId = currentUserId,
      localCallsign = callsign,
      localSyndicateTag = syndicateTag,
      localArmadaName = engine.activeArmadaName,
      localShipId = selectedShipId,
      localLiveResonance = engine.liveResonance,
      isDeviceOnline = isDeviceOnline,
      mergedLivePilots = mergedLivePilots,
    )
    matchmakingQueueViewModel.enqueueOrExecute(
      modeTitle = modeTitle,
      riftLabel = riftLabel,
      targetSectorKey = targetSectorKey,
      rankedMode = rankedMode,
      isCustomInstance = isCustomInstance,
      onNoTeamsNotice = { msg ->
        toastMessage = msg
        toastVersion++
      },
      onExecuteLaunch = onExecuteLaunch,
    )
  }

  LaunchedEffect(
    matchmakingQueueState.isQueueActive,
    matchmakingQueueState.targetSectorKey,
    engine.hasJoinedMatch,
    engine.activeMatchSectorTag(),
  ) {
    if (isGoogleAuthenticated && isDeviceOnline) {
      pushLivePilotHeartbeatNow()
    }
  }

  LaunchedEffect(
    mergedLivePilots,
    suspendedActiveMatch == null,
    engine.isEphemeralPracticeRift,
    engine.hasJoinedMatch,
    engine.activeMatchSectorTag(),
  ) {
    engine.updateRemotePilots(mergedLivePilots, currentUserId)
  }

  LaunchedEffect(sectorCombatEvents, currentUserId) {
    if (sectorCombatEvents.isNotEmpty() && !engine.isEphemeralPracticeRift) {
      engine.applyIncomingCombatEvents(sectorCombatEvents, currentUserId)
    }
  }

  LaunchedEffect(sectorInstanceState, currentUserId) {
    if (!engine.isEphemeralPracticeRift) {
      engine.applyRemoteSectorInstanceState(sectorInstanceState, currentUserId)
    }
  }

  LaunchedEffect(cloudPublicCustomConfigs) {
    engine.syncCloudPublicCustomGames(cloudPublicCustomConfigs)
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

  LaunchedEffect(currentUserId, isGoogleAuthenticated, isDeviceOnline) {
    if (!isGoogleAuthenticated || !isDeviceOnline) return@LaunchedEffect
    pushLivePilotHeartbeatNow()
    while (true) {
      val inActiveCombatOrQueue =
        matchmakingQueueState.isQueueActive ||
          suspendedActiveMatch != null ||
          (engine.hasJoinedMatch && !engine.isEphemeralPracticeRift)
      val intervalMs = if (inActiveCombatOrQueue) 95L else 1200L
      delay(intervalMs)
      pushLivePilotHeartbeatNow()
    }
  }

  LaunchedEffect(currentUserId, isGoogleAuthenticated, isDeviceOnline) {
    if (!isGoogleAuthenticated || !isDeviceOnline) return@LaunchedEffect
    while (true) {
      delay(350L)
      if (
        suspendedActiveMatch == null &&
          engine.hasJoinedMatch &&
          !engine.isEphemeralPracticeRift &&
          engine.effectiveModeRules() != RankedGameMode.SECTOR_CONVERGENCE &&
          engine.isLocalObjectiveHost(currentUserId)
      ) {
        viewModel.publishSectorInstanceState(engine.exportSectorInstanceState(currentUserId))
      }
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
    } else if (isNexusShellOpen && activeNexusNode == NexusCoreNode.STAR_FORGE) {
      RiftCalibrationState.isDraggingAnySlider = false
      isForgeFullScreenStage = false
      forgeInitialTab = NexusForgeTab.DEV_STUDIO_STAGE
      activeNexusNode = NexusCoreNode.ORBITAL_HUB
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
    remember(mergedLivePilots, cloudPublicCustomConfigs, callsign, frameTick) {
      val cleanHumans = mergedLivePilots
      val publicHourResetSec =
        (3600 - ((System.currentTimeMillis() / 1000L) % 3600L).toInt()).coerceIn(1, 3600)

      fun humanStatsForSector(sectorTag: String, fallbackSimResonance: Int): Triple<Int, Int, Int> {
        val matching =
          cleanHumans.filter { p ->
            val pSec = p.sectorId.trim()
            if (sectorTag.equals("SECTOR_CONVERGENCE", ignoreCase = true)) {
              pSec.equals("SECTOR_CONVERGENCE", ignoreCase = true) ||
                pSec.equals("FFA", ignoreCase = true)
            } else {
              pSec.equals(sectorTag, ignoreCase = true)
            }
          }
        if (matching.isEmpty()) {
          val simAvg = (fallbackSimResonance / 6).coerceAtLeast(180)
          return Triple(0, fallbackSimResonance, simAvg)
        }
        val combinedRes =
          matching.sumOf { p ->
            if (p.liveResonance > 0L) p.liveResonance.toInt()
            else (p.bounty.toInt() / 2).coerceAtLeast(180)
          }
        val avgRes = (combinedRes / matching.size).coerceAtLeast(0)
        return Triple(matching.size, combinedRes, avgRes)
      }

      val list = mutableListOf<OverwatchRiftInstance>()

      // 1. Always-running public Sector Convergence #01 (1-hour reset)
      val (pCount1Raw, cRes1, avgRes1) = humanStatsForSector("SECTOR_CONVERGENCE", 1420)
      val pCount1 =
        maxOf(
          pCount1Raw,
          engine.getHumanPilotCountForSectorKey("SECTOR_CONVERGENCE", RankedGameMode.SECTOR_CONVERGENCE),
        )
      list.add(
        OverwatchRiftInstance(
          key = "SECTOR_CONVERGENCE",
          modeTitle = RankedGameMode.SECTOR_CONVERGENCE.title.uppercase(Locale.US),
          riftLabel = "RIFT #01",
          rankedMode = RankedGameMode.SECTOR_CONVERGENCE,
          customConfig = null,
          pilotCount = pCount1,
          combinedResonance = cRes1,
          seedSalt = 101,
          avgResonance = avgRes1,
          remainingSeconds = publicHourResetSec,
        )
      )

      // 2. Always-running public Sector Convergence #02 (1-hour reset)
      val (pCount2Raw, cRes2, avgRes2) = humanStatsForSector("SECTOR_CONVERGENCE_2", 980)
      val pCount2 =
        maxOf(
          pCount2Raw,
          engine.getHumanPilotCountForSectorKey("SECTOR_CONVERGENCE_2", RankedGameMode.SECTOR_CONVERGENCE),
        )
      list.add(
        OverwatchRiftInstance(
          key = "SECTOR_CONVERGENCE_2",
          modeTitle = RankedGameMode.SECTOR_CONVERGENCE.title.uppercase(Locale.US),
          riftLabel = "RIFT #02",
          rankedMode = RankedGameMode.SECTOR_CONVERGENCE,
          customConfig = null,
          pilotCount = pCount2,
          combinedResonance = cRes2,
          seedSalt = 707,
          avgResonance = avgRes2,
          remainingSeconds = publicHourResetSec,
        )
      )

      // 3. Always-running public Flux Core #03 (1-hour reset)
      val (pCount3Raw, cRes3, avgRes3) = humanStatsForSector("FLUX_CORE", 1080)
      val pCount3 =
        maxOf(
          pCount3Raw,
          engine.getHumanPilotCountForSectorKey("FLUX_CORE", RankedGameMode.FLUX_CORE),
        )
      list.add(
        OverwatchRiftInstance(
          key = "FLUX_CORE",
          modeTitle = RankedGameMode.FLUX_CORE.title.uppercase(Locale.US),
          riftLabel = "RIFT #03",
          rankedMode = RankedGameMode.FLUX_CORE,
          customConfig = null,
          pilotCount = pCount3,
          combinedResonance = cRes3,
          seedSalt = 202,
          avgResonance = avgRes3,
          remainingSeconds = publicHourResetSec,
        )
      )

      // 4. Strictly your own active Custom Game ONLY if baseMode is SECTOR_CONVERGENCE (FFA) or FLUX_CORE
      val ownEligibleCustoms =
        buildList {
          val seen = hashSetOf<String>()
          val activeJoinedInstanceId =
            if (engine.hasJoinedMatch && !engine.isHubOverwatchActive) {
              engine.currentCustomGame?.instanceId.orEmpty()
            } else {
              ""
            }
          for (c in cloudPublicCustomConfigs + engine.activeCustomGames) {
            val isAllowedBase =
              c.baseMode == RankedGameMode.SECTOR_CONVERGENCE ||
                c.baseMode == RankedGameMode.FLUX_CORE
            val isOwnCustom =
              (callsign.isNotBlank() && c.hostCallsign.equals(callsign, ignoreCase = true)) ||
                (activeJoinedInstanceId.isNotBlank() &&
                  c.instanceId.equals(activeJoinedInstanceId, ignoreCase = true))
            if (isAllowedBase && isOwnCustom && seen.add(c.instanceId.uppercase(Locale.US))) {
              add(c)
            }
          }
        }
      ownEligibleCustoms.forEach { cfg ->
        val sectorKey = "CUSTOM_${cfg.instanceId}"
        val simFallback = 720 + (kotlin.math.abs(cfg.instanceId.hashCode()) % 420)
        val (pCountRaw, cRes, avgRes) = humanStatsForSector(sectorKey, simFallback)
        val pCount = maxOf(pCountRaw, engine.getHumanPilotCountForSectorKey(sectorKey, cfg.baseMode))
        val customRemSec =
          if (cfg.matchDurationMinutes <= 0) publicHourResetSec
          else (cfg.matchDurationMinutes.coerceIn(1, 120) * 60)
        list.add(
          OverwatchRiftInstance(
            key = sectorKey,
            modeTitle = "CUSTOM • ${cfg.baseMode.title.uppercase(Locale.US)}",
            riftLabel = "CUSTOM",
            rankedMode = cfg.baseMode,
            customConfig = cfg,
            pilotCount = pCount,
            combinedResonance = cRes,
            seedSalt = cfg.instanceId.hashCode(),
            avgResonance = avgRes,
            remainingSeconds = customRemSec,
          )
        )
      }
      list.toList()
    }

  val effectiveOverwatchInstance =
    remember(overwatchInstances, userSelectedOverwatchKey, favoriteOverwatchKey) {
      val normUserKey = userSelectedOverwatchKey.removeSuffix("_1")
      val normFavKey = favoriteOverwatchKey.removeSuffix("_1")
      val manualMatch =
        if (userSelectedOverwatchKey.isNotBlank()) {
          overwatchInstances.firstOrNull { it.key == userSelectedOverwatchKey || it.key == normUserKey }
        } else {
          null
        }
      val favMatch =
        if (favoriteOverwatchKey.isNotBlank()) {
          overwatchInstances.firstOrNull { it.key == favoriteOverwatchKey || it.key == normFavKey }
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
        openNexusNode(NexusCoreNode.HELM_SYSTEMS, helmSubDeckOverride = 3)
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
      onOpenSettings = { openNexusNode(NexusCoreNode.HELM_SYSTEMS, helmSubDeckOverride = 0) },
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
        needsInitialSwornCallsign ||
        matchmakingQueueState.isQueueActive ||
        matchmakingQueueState.noticeMessage != null

    if (hasAnyModalVisible) {
      DeviceAwareModalAutoScaler(
        bypassScaling =
          isNexusShellOpen &&
            activeNexusNode == NexusCoreNode.STAR_FORGE &&
            isForgeFullScreenStage
      ) {
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
                shipResonancesState,
                frameTick,
              ) {
                localPrefs.getBestShipId(currentUserId, 1).coerceIn(1, 10)
              }
            LaunchedEffect(
              bestShipId,
              hasExplicitHangarLock,
              currentUserId,
              engine.isShipLockedForMatch,
              engine.hasJoinedMatch,
            ) {
              if (!hasExplicitHangarLock && !engine.isShipLockedForMatch && !engine.hasJoinedMatch) {
                val targetBest = bestShipId.coerceIn(1, 10)
                if (selectedShipId != targetBest) {
                  selectedShipId = targetBest
                }
                if (engine.localPlayer.shipId != targetBest) {
                  engine.switchLocalPlayerShip(targetBest, forceFromHangar = true)
                  syncEquippedCosmeticsToEngine(engine)
                }
              }
            }
            val hangarCardShipId =
              if (hasExplicitHangarLock || engine.isShipLockedForMatch) {
                selectedShipId.coerceIn(1, 10)
              } else {
                bestShipId.coerceIn(1, 10)
              }
            val hangarCardShipSummary =
              remember(
                hangarCardShipId,
                hasExplicitHangarLock,
                engine.isShipLockedForMatch,
                activeCustomSlot,
                activeShipTitle,
                frameTick,
              ) {
                val baseTitle =
                  if (hangarCardShipId == engine.localPlayer.shipId) {
                    activeShipTitle
                  } else if (hangarCardShipId == 10) {
                    activeCustomSlot.customName.ifBlank { "RIFT FRAME" }.uppercase(Locale.US)
                  } else {
                    RiftCodex.getAllNineShips()
                      .find { it.id == hangarCardShipId }
                      ?.name
                      ?.replace(Regex("^[0-9]+-"), "")
                      ?.uppercase(Locale.US) ?: activeShipTitle
                  }
                val prefix =
                  if (hasExplicitHangarLock || engine.isShipLockedForMatch) "LOCKED" else "BEST"
                "$prefix|$baseTitle"
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
              remember(frameTick, sectorPilotsState, onlineHumanPilotsList, isDeviceOnline, isGoogleAuthenticated) {
                if (!isDeviceOnline) {
                  0
                } else {
                  val remoteDistinct =
                    onlineHumanPilotsList
                      .map { it.userId.ifBlank { it.id } }
                      .filter { it.isNotBlank() && it != currentUserId }
                      .distinct()
                      .size
                  val combinedOnline = if (isGoogleAuthenticated) 1 + remoteDistinct else remoteDistinct.coerceAtLeast(1)
                  maxOf(engine.getTotalHumanPilotsOnline(), combinedOnline)
                }
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
                NexusCoreNode.ARCHIVE_CODEX -> "DOSSIER"
                NexusCoreNode.HELM_SYSTEMS ->
                  if (helmInitialSubDeck == 3) "CODEX" else "SETTINGS"
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
                Pair(false, "NEED 150 RIFT SHARDS FOR HULL RE-DESIGN (OPEN RIFT CONDUIT).")
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
                    RiftBillingManager.TESTER_INFINITE_SHARDS_ACTIVE -> "TESTER 0 SHARDS DEPLETED"
                    else -> "-150 SHARDS"
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

            val deployDirectlyWithHangarShip: (RankedGameMode, Int, String?) -> Unit =
              { mode, bracket, sectorOverride ->
                if (mode == RankedGameMode.SYNDICATE_WARS) {
                  val (eligible, reason) = engine.canQueueSyndicateWars()
                  if (!eligible) {
                    sfx.playMutedClick()
                    toastMessage = reason
                    toastVersion++
                    syndicateSubDeck = SyndicateCitadelSubDeck.SYNDICATES_WARS
                    openNexusNode(NexusCoreNode.SYNDICATE_CITADEL)
                  } else {
                    val deployShipId =
                      if (hasExplicitHangarLock || engine.isShipLockedForMatch) {
                        selectedShipId.coerceIn(1, 10)
                      } else {
                        bestShipId.coerceIn(1, 10)
                      }
                    selectedShipId = deployShipId
                    localPrefs.setPreferredShipId(deployShipId)
                    enqueueOrExecuteResonanceMatchmaking(
                      modeTitle = mode.title.uppercase(Locale.US),
                      riftLabel = "${bracket}V${bracket}",
                      targetSectorKey = sectorOverride ?: mode.id,
                      rankedMode = mode,
                      isCustomInstance = false,
                      onExecuteLaunch = {
                        pendingStagedLaunch = null
                        suspendedActiveMatch = null
                        engine.isShipLockedForMatch = false
                        engine.switchLocalPlayerShip(deployShipId, forceFromHangar = true)
                        syncEquippedCosmeticsToEngine(engine)
                        selectedMode = mode.id
                        localPrefs.setPreferredMode(mode.id)
                        engine.deployToRankedMode(
                          mode,
                          bracket,
                          sectorInstanceOverride = sectorOverride,
                        )
                        syncEquippedCosmeticsToEngine(engine)
                        engine.onStatsUpdated()
                        viewModel.setActiveSector(engine.activeMatchSectorTag())
                        engine.updateRemotePilots(mergedLivePilots, currentUserId)
                        pushLivePilotHeartbeatNow()
                        isNexusShellOpen = false
                        sfx.playPrize()
                        toastMessage = "RIFTED INTO ${mode.title.uppercase(Locale.US)}"
                        toastVersion++
                        frameTick++
                      },
                    )
                  }
                } else {
                  val deployShipId =
                    if (hasExplicitHangarLock || engine.isShipLockedForMatch) {
                      selectedShipId.coerceIn(1, 10)
                    } else {
                      bestShipId.coerceIn(1, 10)
                    }
                  selectedShipId = deployShipId
                  localPrefs.setPreferredShipId(deployShipId)
                  val formatLabel =
                    when (mode) {
                      RankedGameMode.RIFT_SKIRMISH,
                      RankedGameMode.RIFT_CONTROL,
                      RankedGameMode.SYNDICATE_WARS -> "${bracket}V${bracket}"
                      else -> mode.teamFormatBadge
                    }
                  enqueueOrExecuteResonanceMatchmaking(
                    modeTitle = mode.title.uppercase(Locale.US),
                    riftLabel = formatLabel,
                    targetSectorKey = sectorOverride ?: mode.id,
                    rankedMode = mode,
                    isCustomInstance = false,
                    onExecuteLaunch = {
                      pendingStagedLaunch = null
                      suspendedActiveMatch = null
                      engine.isShipLockedForMatch = false
                      engine.switchLocalPlayerShip(deployShipId, forceFromHangar = true)
                      syncEquippedCosmeticsToEngine(engine)
                      selectedMode = mode.id
                      localPrefs.setPreferredMode(mode.id)
                      engine.deployToRankedMode(
                        mode,
                        bracket,
                        sectorInstanceOverride = sectorOverride,
                      )
                      syncEquippedCosmeticsToEngine(engine)
                      engine.onStatsUpdated()
                      viewModel.setActiveSector(engine.activeMatchSectorTag())
                      engine.updateRemotePilots(mergedLivePilots, currentUserId)
                      pushLivePilotHeartbeatNow()
                      isNexusShellOpen = false
                      sfx.playPrize()
                      toastMessage = "RIFTED INTO ${mode.title.uppercase(Locale.US)}"
                      toastVersion++
                      frameTick++
                    },
                  )
                }
              }

            val stageRankedModeIntoHangar: (RankedGameMode, Int) -> Unit = { mode, bracket ->
              deployDirectlyWithHangarShip(mode, bracket, null)
            }

            var callsignRenameRefreshTick by remember { mutableIntStateOf(0) }
            val currentCallsignChangeCount =
              remember(currentUserId, isGoogleAuthenticated, callsignRenameRefreshTick) {
                if (isGoogleAuthenticated) localPrefs.getCallsignChangeCount(currentUserId) else 0
              }
            val applyUnifiedCallsignRename: (String) -> Pair<Boolean, String> = { rawRequested ->
              val trimmedReq = rawRequested.trim()
              if (trimmedReq.equals(callsign, ignoreCase = true) && callsign.isNotBlank()) {
                Pair(true, "Callsign already active: $callsign")
              } else if (isGoogleAuthenticated && !localPrefs.isGuestBypassEnabled()) {
                val effectiveBalance = RiftBillingManager.effectiveShardBalance(engine.riftShards)
                val (success, savedOrError, remainingShards) =
                  localPrefs.changeSwornPilotCallsign(trimmedReq, effectiveBalance, currentUserId)
                if (success) {
                  val finalShards = RiftBillingManager.effectiveShardBalance(remainingShards)
                  engine.riftShards = finalShards
                  localPrefs.setRiftShards(finalShards, currentUserId)
                  callsign = savedOrError
                  engine.localPlayer.callsign = savedOrError
                  engine.syncLocalArmadaSelfSlot()
                  renamedCallsignInCurrentSession = true
                  needsInitialSwornCallsign = false
                  callsignRenameRefreshTick++
                  engine.onStatsUpdated()
                  pushLivePilotHeartbeatNow()
                  toastMessage = "SWORN PILOT CALLSIGN UPDATED TO $savedOrError"
                  toastVersion++
                  Pair(true, "Callsign updated to $savedOrError")
                } else {
                  Pair(false, savedOrError)
                }
              } else {
                val cleanGuest = localPrefs.setCallsign(trimmedReq)
                callsign = cleanGuest
                engine.localPlayer.callsign = cleanGuest
                engine.syncLocalArmadaSelfSlot()
                callsignRenameRefreshTick++
                engine.onStatsUpdated()
                Pair(true, "Drifter callsign updated to $cleanGuest")
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

            val renderModuleForNode: @Composable (NexusCoreNode) -> Unit = { targetNode ->
              when (targetNode) {
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
                          shipResonances = shipResonancesState,
                          callsignChangeCount = currentCallsignChangeCount,
                          isGuestDrifter = !isGoogleAuthenticated || localPrefs.isGuestBypassEnabled(),
                          onRenameCallsign = applyUnifiedCallsignRename,
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
                            isForgeFullScreenStage = false
                            openNexusNode(NexusCoreNode.STAR_FORGE)
                          },
                          onDeployPilot = { newCallsign, newShipId, _, asGuest ->
                            if (asGuest) localPrefs.setGuestBypassEnabled(true)
                            val cleanCallsign =
                              if (!newCallsign.trim().equals(callsign, ignoreCase = true)) {
                                val (renamedOk, renameMsg) = applyUnifiedCallsignRename(newCallsign)
                                if (!renamedOk) {
                                  toastMessage = renameMsg
                                  toastVersion++
                                }
                                callsign
                              } else {
                                callsign
                              }
                            val clampedShipId = newShipId.coerceIn(1, 10)
                            callsign = cleanCallsign
                            selectedShipId = clampedShipId
                            hasExplicitHangarLock = true
                            syndicateTag =
                              if (isGoogleAuthenticated) localPrefs.getSyndicateTag(currentUserId)
                              else localPrefs.getSyndicateTag()
                            syndicateName =
                              if (isGoogleAuthenticated) localPrefs.getSyndicateName(currentUserId)
                              else localPrefs.getSyndicateName()
                            localPrefs.setPreferredShipId(clampedShipId)
                            engine.localPlayer.callsign = cleanCallsign
                            engine.localPlayer.syndicateTag = syndicateTag
                            engine.syncLocalArmadaSelfSlot()

                            val staged = pendingStagedLaunch
                            val finalizeHangarLaunch: () -> Unit = {
                              if (staged != null) {
                                suspendedActiveMatch = null
                              }
                              if (staged != null || !engine.hasJoinedMatch) {
                                engine.isShipLockedForMatch = false
                              }
                              engine.switchLocalPlayerShip(clampedShipId, forceFromHangar = true)
                              syncEquippedCosmeticsToEngine(engine)

                              when (staged) {
                                is StagedRiftLaunch.Ranked -> {
                                  selectedMode = staged.mode.id
                                  localPrefs.setPreferredMode(staged.mode.id)
                                  engine.deployToRankedMode(
                                    staged.mode,
                                    staged.teamSizeBracket,
                                    sectorInstanceOverride = staged.sectorInstanceOverride,
                                  )
                                }
                                is StagedRiftLaunch.NewCustom -> {
                                  selectedMode = staged.config.baseMode.id
                                  localPrefs.setPreferredMode(staged.config.baseMode.id)
                                  engine.createAndJoinCustomGame(staged.config)
                                  RiftCalibrationState.botsEnabled = staged.config.botsEnabled
                                  RiftCalibrationState.applyCalibrationChangesLive(engine, context)
                                  if (isGoogleAuthenticated && isDeviceOnline && currentUserId.isNotBlank()) {
                                    viewModel.publishCustomGameRoom(
                                      staged.config.toCloudCustomGameRoom(currentUserId)
                                    )
                                  }
                                }
                                is StagedRiftLaunch.ExistingCustom -> {
                                  engine.joinCustomGame(staged.instanceId)
                                  engine.currentCustomGame?.let { joinedCfg ->
                                    RiftCalibrationState.botsEnabled = joinedCfg.botsEnabled
                                    RiftCalibrationState.applyCalibrationChangesLive(engine, context)
                                  }
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
                                        sectorInstanceOverride = engine.activeSectorInstanceOverride,
                                      )
                                    }
                                  }
                                }

                                syncEquippedCosmeticsToEngine(engine)
                                engine.onStatsUpdated()
                                if (engine.hasJoinedMatch) {
                                  viewModel.setActiveSector(engine.activeMatchSectorTag())
                                  engine.updateRemotePilots(mergedLivePilots, currentUserId)
                                  pushLivePilotHeartbeatNow()
                                  pendingStagedLaunch = null
                                  isNexusShellOpen = false
                                }
                              }

                              when (staged) {
                                is StagedRiftLaunch.Ranked -> {
                                  enqueueOrExecuteResonanceMatchmaking(
                                    modeTitle = staged.mode.title.uppercase(Locale.US),
                                    riftLabel = "${staged.teamSizeBracket}V${staged.teamSizeBracket}",
                                    targetSectorKey = staged.sectorInstanceOverride ?: staged.mode.id,
                                    rankedMode = staged.mode,
                                    isCustomInstance = false,
                                    onExecuteLaunch = finalizeHangarLaunch,
                                  )
                                }
                                is StagedRiftLaunch.NewCustom,
                                is StagedRiftLaunch.ExistingCustom -> {
                                  finalizeHangarLaunch()
                                }
                                null -> {
                                  if (!engine.hasJoinedMatch && engine.currentCustomGame == null) {
                                    enqueueOrExecuteResonanceMatchmaking(
                                      modeTitle = engine.activeRankedMode.title.uppercase(Locale.US),
                                      riftLabel = engine.activeRankedMode.teamFormatBadge,
                                      targetSectorKey =
                                        engine.activeSectorInstanceOverride ?: engine.activeRankedMode.id,
                                      rankedMode = engine.activeRankedMode,
                                      isCustomInstance = false,
                                      onExecuteLaunch = finalizeHangarLaunch,
                                    )
                                  } else {
                                    finalizeHangarLaunch()
                                  }
                                }
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
                                  "TESTER 0 SHARDS DEPLETED"
                                } else {
                                  "-${item.shardCost} SHARDS"
                                }
                              true to
                                "UNLOCKED & EQUIPPED ${item.name.uppercase(Locale.US)} ($spendNote)"
                            } else {
                              false to
                                "REQUIRES ${item.freeAtResonance} PEAK RESONANCE (FREE) OR ${item.shardCost} RIFT SHARDS"
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
                                    "TESTER 0 SHARDS DEPLETED"
                                  else -> "-${gate.shardCost} RIFT SHARDS"
                                }
                              true to "UNLOCKED ${slotId.uppercase(Locale.US)} ($freeNote)"
                            } else {
                              false to
                                "REQUIRES ${gate.freeAtResonance} PEAK RESONANCE (FREE) OR ${gate.shardCost} RIFT SHARDS"
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
                          val rawFriendsList =
                            remember(syndicateTag, frameTick, friendsRefreshKey) {
                              localPrefs.getFriendsList()
                            }
                          val enrichedFriendsList =
                            remember(
                              rawFriendsList,
                              mergedLivePilots,
                              realSwornCloudLeaderboard,
                              frameTick,
                            ) {
                              rawFriendsList.map { friend ->
                                val liveMatch =
                                  mergedLivePilots.firstOrNull { p ->
                                    val pUid = p.userId.ifBlank { p.id }
                                    (friend.uid.isNotBlank() && pUid == friend.uid) ||
                                      p.callsign.equals(friend.callsign, ignoreCase = true)
                                  }
                                val cloudEntry =
                                  realSwornCloudLeaderboard.firstOrNull { e ->
                                    (friend.uid.isNotBlank() && e.userId == friend.uid) ||
                                      e.callsign.equals(friend.callsign, ignoreCase = true)
                                  }
                                val derivedCloudRes =
                                  cloudEntry?.let {
                                    ((it.pvpBounty - 100L).coerceAtLeast(0L) +
                                        it.pvpKills * 12L +
                                        it.convergencesWon * 35L)
                                      .coerceIn(0L, 50_000L)
                                      .toInt()
                                  } ?: 0
                                val resolvedRes =
                                  when {
                                    liveMatch != null && liveMatch.liveResonance > 0L ->
                                      liveMatch.liveResonance.toInt()
                                    derivedCloudRes > 0 -> derivedCloudRes
                                    else -> friend.bounty.coerceAtLeast(0)
                                  }
                                val isPilotOnline = liveMatch != null
                                val resolvedActivity =
                                  when {
                                    liveMatch == null -> "OFFLINE"
                                    liveMatch.sectorId.equals("NEXUS_LOBBY", ignoreCase = true) ||
                                      liveMatch.sectorId.equals("RIFT_LOBBY", ignoreCase = true) ->
                                      "ONLINE • RIFT HUB"
                                    else -> "IN COMBAT • ${liveMatch.sectorId}"
                                  }
                                friend.copy(
                                  shipId = liveMatch?.shipId?.toInt()?.coerceIn(1, 10) ?: friend.shipId,
                                  syndicateTag =
                                    liveMatch?.syndicateTag?.takeIf { it.isNotBlank() }
                                      ?: friend.syndicateTag,
                                  isOnline = isPilotOnline,
                                  statusText = resolvedActivity,
                                  currentArmadaName =
                                    liveMatch?.armadaName?.takeIf { !it.equals("SOLO", ignoreCase = true) }
                                      ?: friend.currentArmadaName,
                                  bounty = resolvedRes,
                                )
                              }
                            }
                          val enrichedArmadaSlots =
                            remember(
                              engine.activeArmadaSlots.size,
                              mergedLivePilots,
                              realSwornCloudLeaderboard,
                              engine.liveResonance,
                              isDeviceOnline,
                              frameTick,
                            ) {
                              engine.activeArmadaSlots.toList().map { slot ->
                                val isLocalSelf =
                                  (currentUserId.isNotBlank() && slot.uid == currentUserId) ||
                                    (slot.isLeader && slot.callsign.equals(callsign, ignoreCase = true))
                                if (isLocalSelf) {
                                  val selfActivity =
                                    when {
                                      !isDeviceOnline -> "OFFLINE"
                                      engine.hasJoinedMatch && !engine.isEphemeralPracticeRift ->
                                        "IN COMBAT • ${engine.activeMatchSectorTag()}"
                                      else -> "ONLINE • RIFT HUB"
                                    }
                                  slot.copy(
                                    resonance = engine.liveResonance.coerceAtLeast(0),
                                    isOnline = isDeviceOnline,
                                    activityLabel = selfActivity,
                                  )
                                } else {
                                  val liveMatch =
                                    mergedLivePilots.firstOrNull { p ->
                                      val pUid = p.userId.ifBlank { p.id }
                                      (slot.uid.isNotBlank() && pUid == slot.uid) ||
                                        p.callsign.equals(slot.callsign, ignoreCase = true)
                                    }
                                  val cloudEntry =
                                    realSwornCloudLeaderboard.firstOrNull { e ->
                                      (slot.uid.isNotBlank() && e.userId == slot.uid) ||
                                        e.callsign.equals(slot.callsign, ignoreCase = true)
                                    }
                                  val derivedCloudRes =
                                    cloudEntry?.let {
                                      ((it.pvpBounty - 100L).coerceAtLeast(0L) +
                                          it.pvpKills * 12L +
                                          it.convergencesWon * 35L)
                                        .coerceIn(0L, 50_000L)
                                        .toInt()
                                    } ?: 0
                                  val resolvedRes =
                                    when {
                                      liveMatch != null && liveMatch.liveResonance > 0L ->
                                        liveMatch.liveResonance.toInt()
                                      derivedCloudRes > 0 -> derivedCloudRes
                                      else -> slot.resonance.coerceAtLeast(0)
                                    }
                                  val resolvedActivity =
                                    when {
                                      liveMatch == null -> "OFFLINE"
                                      liveMatch.sectorId.equals("NEXUS_LOBBY", ignoreCase = true) ||
                                        liveMatch.sectorId.equals("RIFT_LOBBY", ignoreCase = true) ->
                                        "ONLINE • RIFT HUB"
                                      else -> "IN COMBAT • ${liveMatch.sectorId}"
                                    }
                                  slot.copy(
                                    shipId = liveMatch?.shipId?.toInt()?.coerceIn(1, 10) ?: slot.shipId,
                                    resonance = resolvedRes,
                                    isOnline = liveMatch != null,
                                    activityLabel = resolvedActivity,
                                  )
                                }
                              }
                            }

                          ArmadaFleetModal(
                            currentSyndicateTag = syndicateTag,
                            activeArmadaName = engine.activeArmadaName,
                            armadaTierLabel = engine.getCurrentArmadaTierLabel(),
                            armadaSlots = enrichedArmadaSlots,
                            friendsList = enrichedFriendsList,
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
                                armadaStatusBanner = "SQUADRON TAG REJECTED BY SAFETY FILTER."
                              } else {
                                engine.formOrJoinArmada(
                                  trimmedWing,
                                  if (engine.activeArmadaSlots.size <= 1) enrichedFriendsList.take(1)
                                  else emptyList(),
                                )
                                armadaStatusBanner =
                                  "SQUADRON [${engine.activeArmadaName}] DEPLOYED (${engine.getCurrentArmadaSize()}/5 PILOTS)"
                                friendsRefreshKey++
                              }
                            },
                            onInviteFriendToArmada = { friend ->
                              val added = engine.addMemberToArmada(friend)
                              if (added) {
                                armadaStatusBanner =
                                  "${friend.callsign} JOINED SQUADRON [${engine.activeArmadaName}] (${engine.getCurrentArmadaSize()}/5)"
                              }
                              friendsRefreshKey++
                            },
                            onRequestJoinFriendArmada = { friend ->
                              val targetArmada =
                                friend.currentArmadaName
                                  .ifEmpty { "${friend.syndicateTag}_SQD" }
                                  .take(14)
                              engine.formOrJoinArmada(targetArmada, listOf(friend))
                              armadaStatusBanner =
                                "JOINED SQUADRON [$targetArmada] WITH ${friend.callsign} (${engine.getCurrentArmadaSize()}/5)."
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
                              armadaStatusBanner = "LEFT SQUADRON -> OPERATING INDEPENDENTLY."
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
                              localPrefs.reportPilot(pilotUid, pilotCallsign, "Squadron Roster Report")
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
                                  description = "Eliminate an above-average Resonance rival (Resonance >= 250).",
                                  unlocked =
                                    engine.hvtEliminations >= 1L || engine.allTimeBountyPeak >= 250L,
                                  progressText = "${engine.hvtEliminations} TOP-RES KILLS",
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
                                  description = "Reach 250+ Resonance in open Rift combat.",
                                  unlocked = engine.allTimeBountyPeak >= 250L,
                                  progressText = "${engine.allTimeBountyPeak} / 250 RES",
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
                                      syndicateTag !in LocalPilotPrefs.NON_PLAYER_SYNDICATE_TAGS,
                                  progressText =
                                    if (
                                      syndicateTag.isNotBlank() &&
                                        syndicateTag !in LocalPilotPrefs.NON_PLAYER_SYNDICATE_TAGS
                                    ) {
                                      "[$syndicateTag] ACTIVE"
                                    } else {
                                      "UNAFFILIATED"
                                    },
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
                                  ownerUid = currentUserId.ifBlank { "local_founder" },
                                )
                              res
                                .onSuccess { created ->
                                  syndicateTag = created.tag
                                  syndicateName = created.name
                                  syndicateHistory = localPrefs.getSyndicateServiceHistory(currentUserId)
                                  engine.localPlayer.syndicateTag = syndicateTag
                                  engine.syncLocalArmadaSelfSlot()
                                  if (isGoogleAuthenticated) {
                                    viewModel.createCloudSyndicate(created)
                                  }
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
                          LaunchedEffect(Unit) {
                            archiveSubDeck = ArchiveCodexSubDeck.PILOT_DOSSIER
                            openNexusNode(NexusCoreNode.HELM_SYSTEMS, helmSubDeckOverride = 3)
                          }
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
                              hasExplicitHangarLock = false
                              selectedShipId = 1
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
                              shipResonancesState = localPrefs.getAllShipResonances(currentUserId)
                              engine.loadShipResonances(shipResonancesState, 0)
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
                                  renamedCallsignInCurrentSession = true
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
                            onRenameCallsign = applyUnifiedCallsignRename,
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
                          initialSubDeck = helmInitialSubDeck,
                          onSubDeckChanged = { helmInitialSubDeck = it },
                          isNexusStreaming = isNexusStreaming,
                          onOpenNexusFromCodex = { openNexusNode(NexusCoreNode.ORBITAL_HUB) },
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
                            pendingStagedLaunch = null
                            suspendedActiveMatch = null
                            matchmakingQueueViewModel.resetForAccountSwitch()
                            engine.leaveActiveMatchToNexusOrbit()
                            isNexusShellOpen = false
                            localPrefs.clearUnscopedCallsignOnAccountSwitch()
                            localPrefs.setGuestBypassEnabled(false)
                            onSwitchAccount()
                            signOut(credentialManager = credentialManager, scope = scope)
                          },
                          onSwitchAccount = {
                            pendingStagedLaunch = null
                            suspendedActiveMatch = null
                            matchmakingQueueViewModel.resetForAccountSwitch()
                            engine.leaveActiveMatchToNexusOrbit()
                            isNexusShellOpen = false
                            localPrefs.clearUnscopedCallsignOnAccountSwitch()
                            localPrefs.setGuestBypassEnabled(false)
                            onSwitchAccount()
                            if (isGoogleAuthenticated) {
                              signOut(credentialManager = credentialManager, scope = scope)
                            }
                          },
                          updateStatusLabel = updateStatusLabel,
                          hasUpdateAvailable = hasUpdateAvailable,
                          onCheckOrApplyUpdate = onCheckOrApplyUpdate,
                          onCloseModal = closeEmbeddedModal,
                          onOpenChangelog = { showFullChangelogModal = true },
                          onOpenAudioForge = {
                            forgeInitialTab = NexusForgeTab.AUDIO_SFX
                            isForgeFullScreenStage = false
                            openNexusNode(NexusCoreNode.STAR_FORGE)
                          },
                          currentUserId = currentUserId,
                          activeSectorTag = engine.activeMatchSectorTag(),
                          cloudSyncHealth = cloudSyncHealth,
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
                          onExitToNexusHub = {
                            RiftCalibrationState.isDraggingAnySlider = false
                            isForgeFullScreenStage = false
                            forgeInitialTab = NexusForgeTab.DEV_STUDIO_STAGE
                            openNexusNode(NexusCoreNode.ORBITAL_HUB)
                          },
                          onSelectShipForDeploy = { targetShipId ->
                            val clamped = targetShipId.coerceIn(1, 10)
                            selectedShipId = clamped
                            hasExplicitHangarLock = true
                            localPrefs.setPreferredShipId(clamped)
                            if (engine.hasJoinedMatch && !engine.isHubOverwatchActive) {
                              engine.switchLocalPlayerShip(clamped, forceFromHangar = true)
                            } else {
                              val chosenSpec = RiftCodex.getShipById(clamped)
                              engine.localPlayer.shipId = clamped
                              engine.localPlayer.spec = chosenSpec
                              engine.localPlayer.liveResonanceRating = engine.getShipResonance(clamped)
                              engine.syncLocalArmadaSelfSlot()
                            }
                            syncEquippedCosmeticsToEngine(engine)
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
            val embeddedModuleContent: (@Composable () -> Unit)? =
              when (activeNexusNode) {
                NexusCoreNode.ORBITAL_HUB,
                NexusCoreNode.SUBSPACE_COMMS,
                NexusCoreNode.CONVERGENCE_GATE -> null
                else -> {
                  { renderModuleForNode(activeNexusNode) }
                }
              }

            NexusAiConsoleModal(
              messages = nexusMessages,
              streamState = nexusStreamState,
              lastFailedQuestion = nexusViewModel.lastFailedQuestion,
              activeShipSummary = hangarCardShipSummary,
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
              activeShipId = hangarCardShipId,
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
              isNetworkOnline = isDeviceOnline,
              cloudSyncHealth = cloudSyncHealth,
              syndicateOnlineCount = syndicateOnlineCount,
              strikeWingOnlineCount = strikeWingOnlineCount,
              isInMatch =
                suspendedActiveMatch != null ||
                  (engine.hasJoinedMatch && !engine.isEphemeralPracticeRift),
              onEnterPracticeRift = {
                if (
                  engine.hasJoinedMatch &&
                    !engine.isEphemeralPracticeRift &&
                    suspendedActiveMatch == null
                ) {
                  suspendedActiveMatch =
                    captureActiveMatchSnapshot(engine, hangarCardShipId, selectedMode)
                }
                pendingStagedLaunch = null
                engine.isShipLockedForMatch = false
                engine.switchLocalPlayerShip(hangarCardShipId, forceFromHangar = true)
                syncEquippedCosmeticsToEngine(engine)
                engine.enterEphemeralPracticeRift(startTutorial = true)
                isNexusShellOpen = false
                toastMessage = "PRACTICE RIFT INITIALIZED // SOLO FLIGHT BRIEFING"
                toastVersion++
                frameTick++
              },
              onOpenHowToPlayTutorial = {
                if (
                  engine.hasJoinedMatch &&
                    !engine.isEphemeralPracticeRift &&
                    suspendedActiveMatch == null
                ) {
                  suspendedActiveMatch =
                    captureActiveMatchSnapshot(engine, hangarCardShipId, selectedMode)
                }
                pendingStagedLaunch = null
                engine.isShipLockedForMatch = false
                engine.switchLocalPlayerShip(hangarCardShipId, forceFromHangar = true)
                syncEquippedCosmeticsToEngine(engine)
                engine.enterEphemeralPracticeRift(startTutorial = true)
                isNexusShellOpen = false
                toastMessage = "HOW TO PLAY // INTERACTIVE PRACTICE RIFT TUTORIAL"
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
                deployDirectlyWithHangarShip(mode, bracket, null)
              },
              onLeaveMatch = {
                pendingStagedLaunch = null
                suspendedActiveMatch = null
                engine.leaveActiveMatchToNexusOrbit()
                viewModel.setActiveSector(engine.activeMatchSectorTag())
                pushLivePilotHeartbeatNow()
                toastMessage = "RIFT OUT // ORBITING RIFT STATION"
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
                val fleetTag = "${prefix}_SQD".take(14)
                engine.formOrJoinArmada(
                  fleetTag,
                  if (synMates.isNotEmpty()) synMates else syndicateFriends.take(2),
                )
                friendsRefreshKey++
                toastMessage =
                  "SQUADRON [$fleetTag] STAGED (${engine.getCurrentArmadaSize()}/5 PILOTS)"
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
                openNexusNode(NexusCoreNode.HELM_SYSTEMS, helmSubDeckOverride = 3)
              },
              onOpenSettings = { openNexusNode(NexusCoreNode.HELM_SYSTEMS, helmSubDeckOverride = 0) },
              onOpenForge = {
                RiftCalibrationState.isUiUnlockedForDragging = false
                RiftCalibrationState.isCalibrationModalOpen = false
                forgeInitialTab = NexusForgeTab.DEV_STUDIO_STAGE
                isForgeFullScreenStage = true
                openNexusNode(NexusCoreNode.STAR_FORGE)
              },
              onOpenCalibration = {
                RiftCalibrationState.isUiUnlockedForDragging = false
                RiftCalibrationState.isCalibrationModalOpen = false
                forgeInitialTab = NexusForgeTab.DEV_STUDIO_STAGE
                isForgeFullScreenStage = true
                openNexusNode(NexusCoreNode.STAR_FORGE)
              },
              onOpenHangarForShip = { targetShipId ->
                val clamped = targetShipId.coerceIn(1, 10)
                val changedSelection = selectedShipId != clamped || !hasExplicitHangarLock
                selectedShipId = clamped
                hasExplicitHangarLock = true
                localPrefs.setPreferredShipId(clamped)
                if (engine.hasJoinedMatch && !engine.isHubOverwatchActive) {
                  engine.switchLocalPlayerShip(clamped, forceFromHangar = true)
                } else {
                  val chosenSpec = RiftCodex.getShipById(clamped)
                  engine.localPlayer.shipId = clamped
                  engine.localPlayer.spec = chosenSpec
                  engine.localPlayer.liveResonanceRating = engine.getShipResonance(clamped)
                  engine.syncLocalArmadaSelfSlot()
                }
                syncEquippedCosmeticsToEngine(engine)
                engine.onStatsUpdated()
                if (changedSelection) {
                  val shipTitle =
                    RiftCodex.getAllNineShips()
                      .find { it.id == clamped }
                      ?.name
                      ?.replace(Regex("^[0-9]+-"), "")
                      ?.uppercase(Locale.US) ?: "RIFT FRAME"
                  toastMessage = "CHARGED $shipTitle ACROSS ALL MODES"
                  toastVersion++
                }
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
                val cleanSyn = localPrefs.sanitizeSyndicateTag(syndicateTag)
                val posted =
                  viewModel.postStructuredChannelMessage(
                    senderCallsign = callsign,
                    senderSyndicateTag = cleanSyn,
                    channelKey = channelKey,
                    messageText = safeText,
                  )
                if (chatMessages.none { it.id == posted.id }) {
                  chatMessages.add(posted)
                }
              },
              onSubmitBugReport = { category, description ->
                val safeDesc = UgcModeration.sanitizeUgcText(description)
                val cleanSyn = localPrefs.sanitizeSyndicateTag(syndicateTag)
                localPrefs.reportPilot(
                  uid = "BUG_${category.take(10)}",
                  callsign = category,
                  reason = safeDesc,
                )
                viewModel.submitCloudBugReport(
                  reporterCallsign = callsign,
                  syndicateTag = cleanSyn,
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
                    senderSyndicateTag = cleanSyn,
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
                val cleanSyn = localPrefs.sanitizeSyndicateTag(syndicateTag)
                val buildCard =
                  viewModel.postStructuredChannelMessage(
                    senderCallsign = callsign,
                    senderSyndicateTag = cleanSyn,
                    channelKey = "#shipwright-meta",
                    messageText = summary,
                    armadaName = frameTitle.take(12),
                    armadaObjective = compactRecipe.take(120),
                  )
                if (chatMessages.none { it.id == buildCard.id }) {
                  chatMessages.add(buildCard)
                }
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
                hasExplicitHangarLock = true
                localPrefs.setPreferredShipId(10)
                if (engine.hasJoinedMatch && !engine.isHubOverwatchActive) {
                  engine.switchLocalPlayerShip(10, forceFromHangar = true)
                } else {
                  val chosenSpec = RiftCodex.getShipById(10)
                  engine.localPlayer.shipId = 10
                  engine.localPlayer.spec = chosenSpec
                  engine.localPlayer.liveResonanceRating = engine.getShipResonance(10)
                  engine.syncLocalArmadaSelfSlot()
                }
                syncEquippedCosmeticsToEngine(engine)
                engine.onStatsUpdated()
                sfx.playPrize()
                openNexusNode(NexusCoreNode.HANGAR_DECK)
                toastMessage = "LOADED [${loadedSlot.customName}] INTO $activeCustomSlotId"
                toastVersion++
              },
              onPostStrikeWingInvite = {
                val wingTag =
                  engine.activeArmadaName
                    .ifBlank { "${callsign.take(8)}_SQD" }
                    .uppercase(Locale.US)
                    .take(14)
                if (engine.activeArmadaName.isBlank()) {
                  engine.formOrJoinArmada(wingTag, emptyList())
                }
                val slotsNow = engine.getCurrentArmadaSize()
                val text =
                  "SQUADRON [$wingTag] RECRUITING PILOTS // ${engine.activeRankedMode.title.uppercase(Locale.US)}"
                val cleanSyn = localPrefs.sanitizeSyndicateTag(syndicateTag)
                val inviteMsg =
                  viewModel.postStructuredChannelMessage(
                    senderCallsign = callsign,
                    senderSyndicateTag = cleanSyn,
                    channelKey = "#strike-lfg",
                    messageText = text,
                    isArmadaRecruitBanner = true,
                    armadaName = wingTag.take(12),
                    armadaCurrentSize = slotsNow.coerceIn(1, 5),
                    armadaMaxSize = 5,
                    armadaObjective = engine.activeRankedMode.title,
                  )
                if (chatMessages.none { it.id == inviteMsg.id }) {
                  chatMessages.add(inviteMsg)
                }
                toastMessage = "POSTED SQUADRON [$wingTag] INVITE TO #SQUADRON-LFG"
                toastVersion++
              },
              onPostPrivateRoomInvite = { roomCode, modeTitle ->
                val cleanCode = roomCode.trim().uppercase(Locale.US).take(6)
                val text = "PRIVATE RIFT [$cleanCode] OPEN // $modeTitle"
                val obj = "ROOM|$cleanCode|$modeTitle".take(120)
                val cleanSyn = localPrefs.sanitizeSyndicateTag(syndicateTag)
                val roomMsg =
                  viewModel.postStructuredChannelMessage(
                    senderCallsign = callsign,
                    senderSyndicateTag = cleanSyn,
                    channelKey = "#strike-lfg",
                    messageText = text,
                    armadaName = cleanCode,
                    armadaObjective = obj,
                  )
                if (chatMessages.none { it.id == roomMsg.id }) {
                  chatMessages.add(roomMsg)
                }
                toastMessage = "SHARED PRIVATE RIFT [$cleanCode] TO #SQUADRON-LFG"
                toastVersion++
              },
              onJoinSharedStrikeWing = { msg ->
                val targetWing =
                  msg.armadaName
                    .ifBlank { "${msg.senderCallsign.take(8)}_SQD" }
                    .uppercase(Locale.US)
                    .take(14)
                val leaderFriend =
                  if (msg.senderUid.isNotBlank() && msg.senderUid != currentUserId) {
                    FriendPilotEntry(
                      uid = msg.senderUid,
                      callsign = msg.senderCallsign,
                      syndicateTag = msg.senderSyndicateTag.ifBlank { "FREE" },
                      shipId = 1,
                      statusText = "ONLINE // SQUADRON",
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
                toastMessage = "JOINED SQUADRON [$targetWing]"
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
                      .ifBlank { "${callsign.take(8)}_SQD" }
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
                  toastMessage = "ADDED ${livePilot.callsign} TO SQUADRON [$wingTag]"
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
              onSelectArchiveSubDeck = { sub ->
                if (sub == ArchiveCodexSubDeck.TACTICAL_CODEX) {
                  archiveSubDeck = ArchiveCodexSubDeck.PILOT_DOSSIER
                  openNexusNode(NexusCoreNode.HELM_SYSTEMS, helmSubDeckOverride = 3)
                } else {
                  archiveSubDeck = ArchiveCodexSubDeck.PILOT_DOSSIER
                }
              },
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
                if (
                  engine.hasJoinedMatch &&
                    !engine.isEphemeralPracticeRift &&
                    inst.key.equals(engine.activeMatchSectorTag(), ignoreCase = true)
                ) {
                  isNexusShellOpen = false
                  sfx.playPrize()
                  toastMessage = "RETURNED TO ACTIVE MATCH // ${inst.riftLabel}"
                  toastVersion++
                  frameTick++
                  return@NexusAiConsoleModal
                }
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
                  } else {
                    deployDirectlyWithHangarShip(inst.rankedMode, targetBracket, null)
                  }
                } else {
                  val deployShipId = hangarCardShipId
                  selectedShipId = deployShipId
                  localPrefs.setPreferredShipId(deployShipId)
                  val executeDirectRiftIn: () -> Unit = {
                    pendingStagedLaunch = null
                    suspendedActiveMatch = null
                    engine.isShipLockedForMatch = false
                    engine.switchLocalPlayerShip(
                      deployShipId,
                      forceFromHangar = true,
                    )
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
                      val sectorOverride =
                        if (inst.key.equals("SECTOR_CONVERGENCE_2", ignoreCase = true)) {
                          "SECTOR_CONVERGENCE_2"
                        } else {
                          null
                        }
                      selectedMode = inst.rankedMode.id
                      localPrefs.setPreferredMode(inst.rankedMode.id)
                      engine.deployToRankedMode(
                        inst.rankedMode,
                        targetBracket,
                        sectorInstanceOverride = sectorOverride,
                      )
                    }
                    syncEquippedCosmeticsToEngine(engine)
                    engine.onStatsUpdated()
                    viewModel.setActiveSector(engine.activeMatchSectorTag())
                    engine.updateRemotePilots(mergedLivePilots, currentUserId)
                    pushLivePilotHeartbeatNow()
                    isNexusShellOpen = false
                    sfx.playPrize()
                    toastMessage = "RIFTED INTO ${inst.modeTitle} // ${inst.riftLabel}"
                    toastVersion++
                    frameTick++
                  }
                  enqueueOrExecuteResonanceMatchmaking(
                    modeTitle = inst.modeTitle,
                    riftLabel = inst.riftLabel,
                    targetSectorKey = inst.key,
                    rankedMode = targetMode,
                    isCustomInstance = customCfg != null,
                    onExecuteLaunch = executeDirectRiftIn,
                  )
                }
              },
              onStageRankedModeViaHangar = { mode, bracket ->
                deployDirectlyWithHangarShip(mode, bracket, null)
              },
              onStageCustomGameViaHangar = { customCfg ->
                val deployShipId = hangarCardShipId
                selectedShipId = deployShipId
                localPrefs.setPreferredShipId(deployShipId)
                pendingStagedLaunch = null
                suspendedActiveMatch = null
                engine.isShipLockedForMatch = false
                engine.switchLocalPlayerShip(deployShipId, forceFromHangar = true)
                syncEquippedCosmeticsToEngine(engine)
                selectedMode = customCfg.baseMode.id
                localPrefs.setPreferredMode(customCfg.baseMode.id)
                engine.createAndJoinCustomGame(customCfg)
                RiftCalibrationState.botsEnabled = customCfg.botsEnabled
                RiftCalibrationState.applyCalibrationChangesLive(engine, context)
                if (isGoogleAuthenticated && isDeviceOnline && currentUserId.isNotBlank()) {
                  viewModel.publishCustomGameRoom(
                    customCfg.toCloudCustomGameRoom(currentUserId)
                  )
                }
                syncEquippedCosmeticsToEngine(engine)
                engine.onStatsUpdated()
                viewModel.setActiveSector(engine.activeMatchSectorTag())
                engine.updateRemotePilots(mergedLivePilots, currentUserId)
                pushLivePilotHeartbeatNow()
                isNexusShellOpen = false
                sfx.playPrize()
                toastMessage = "RIFTED INTO CUSTOM RIFT [${customCfg.title.uppercase(Locale.US)}]"
                toastVersion++
                frameTick++
              },
              onStageExistingCustomInstanceViaHangar = { instanceId ->
                val deployShipId = hangarCardShipId
                selectedShipId = deployShipId
                localPrefs.setPreferredShipId(deployShipId)
                pendingStagedLaunch = null
                suspendedActiveMatch = null
                engine.isShipLockedForMatch = false
                engine.switchLocalPlayerShip(deployShipId, forceFromHangar = true)
                syncEquippedCosmeticsToEngine(engine)
                engine.joinCustomGame(instanceId)
                engine.currentCustomGame?.let { joinedCfg ->
                  RiftCalibrationState.botsEnabled = joinedCfg.botsEnabled
                  RiftCalibrationState.applyCalibrationChangesLive(engine, context)
                }
                val resolvedModeId =
                  engine.currentCustomGame?.baseMode?.id ?: engine.activeRankedMode.id
                selectedMode = resolvedModeId
                localPrefs.setPreferredMode(resolvedModeId)
                syncEquippedCosmeticsToEngine(engine)
                engine.onStatsUpdated()
                viewModel.setActiveSector(engine.activeMatchSectorTag())
                engine.updateRemotePilots(mergedLivePilots, currentUserId)
                pushLivePilotHeartbeatNow()
                isNexusShellOpen = false
                sfx.playPrize()
                val joinedTitle = engine.currentCustomGame?.title?.uppercase(Locale.US) ?: instanceId
                toastMessage = "RIFTED INTO CUSTOM RIFT [$joinedTitle]"
                toastVersion++
                frameTick++
              },
              publicCustomGames = cloudPublicCustomConfigs,
              onLookupCustomRoomByCode = { rawCode, onResult ->
                viewModel.lookupCustomGameByCode(rawCode) { cloudRoom, statusMsg ->
                  onResult(cloudRoom?.toCustomGameConfig(), statusMsg)
                }
              },
              onSwitchAccount = {
                pendingStagedLaunch = null
                suspendedActiveMatch = null
                matchmakingQueueViewModel.resetForAccountSwitch()
                engine.leaveActiveMatchToNexusOrbit()
                isNexusShellOpen = false
                localPrefs.clearUnscopedCallsignOnAccountSwitch()
                localPrefs.setGuestBypassEnabled(false)
                onSwitchAccount()
                if (isGoogleAuthenticated) {
                  signOut(credentialManager = credentialManager, scope = scope)
                }
              },
              requestedSideBayKey = hubRequestedSideBay,
              requestedSideBayNonce = hubSideBayRequestNonce,
              syndicateDirectory =
                remember(syndicateTag, frameTick, cloudSyndicatesState, syndicateChannelsRefreshKey) {
                  val cloudList = (cloudSyndicatesState as? UiState.Success)?.data ?: emptyList()
                  localPrefs.mergeCloudSyndicates(cloudList)
                  localPrefs.getAllSyndicates()
                },
              syndicateStatusMessage = syndicateStatusBanner,
              onJoinOfficialSyndicate = { info ->
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
              onLeaveCurrentSyndicate = {
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
              onCharterCustomSyndicate = { rawTag, rawName, rawMotto ->
                val res =
                  localPrefs.createCustomSyndicate(
                    tag = rawTag,
                    name = rawName,
                    motto = rawMotto,
                    ownerUid = currentUserId.ifBlank { "local_founder" },
                  )
                res
                  .onSuccess { created ->
                    syndicateTag = created.tag
                    syndicateName = created.name
                    syndicateHistory = localPrefs.getSyndicateServiceHistory(currentUserId)
                    engine.localPlayer.syndicateTag = syndicateTag
                    engine.syncLocalArmadaSelfSlot()
                    if (isGoogleAuthenticated) {
                      viewModel.createCloudSyndicate(created)
                    }
                    syndicateChannelsRefreshKey++
                    syndicateStatusBanner =
                      "CHARTERED [${created.tag}] ${created.name}! LOCKED FOR 15d 00h."
                    engine.onStatsUpdated()
                  }
                  .onFailure { err ->
                    syndicateStatusBanner = err.message ?: "INVALID SYNDICATE CHARTER."
                  }
              },
              onClaimDailyDirectiveSlot = { slotIdx ->
                val claimed = localPrefs.claimDailyDirectiveReward(slotIdx, currentUserId)
                if (claimed != null) {
                  engine.riftShards =
                    RiftBillingManager.effectiveShardBalance(engine.riftShards + claimed.first)
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
              onRerollDailyDirectiveSlot = { slotIdx ->
                val ok = localPrefs.rerollDailyDirective(slotIdx, currentUserId)
                if (ok) {
                  directivesRefreshKey++
                  sfx.playMutedClick()
                }
                ok
              },
              onClaimDailySweepBonus = {
                val sweep = localPrefs.claimDailySweepBonus(currentUserId)
                if (sweep != null) {
                  engine.riftShards =
                    RiftBillingManager.effectiveShardBalance(engine.riftShards + sweep.first)
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
              onUnlockOrEquipCosmeticItem = { category, item ->
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
                } else if (RiftBillingManager.canAffordShardSpend(engine.riftShards, item.shardCost)) {
                  engine.riftShards =
                    RiftBillingManager.computeBalanceAfterSpend(engine.riftShards, item.shardCost)
                  localPrefs.setRiftShards(engine.riftShards, currentUserId)
                  localPrefs.setCosmeticUnlocked(category, item.id, true, currentUserId)
                  localPrefs.setEquippedCosmeticId(category, item.id, currentUserId)
                  syncEquippedCosmeticsToEngine(engine)
                  cosmeticsRefreshKey++
                  sfx.playPrize()
                  engine.onStatsUpdated()
                  val spendNote =
                    if (RiftBillingManager.TESTER_INFINITE_SHARDS_ACTIVE) {
                      "TESTER 0 SHARDS DEPLETED"
                    } else {
                      "-${item.shardCost} SHARDS"
                    }
                  true to "UNLOCKED & EQUIPPED ${item.name.uppercase(Locale.US)} ($spendNote)"
                } else {
                  false to
                    "REQUIRES ${item.freeAtResonance} PEAK RESONANCE (FREE) OR ${item.shardCost} RIFT SHARDS"
                }
              },
              onUnlockOrSelectFrameSlotId = { slotId ->
                val gate = RiftProgressionCatalog.getFrameSlotGate(slotId)
                val unlockedNow =
                  slotId.equals("Frame A", ignoreCase = true) ||
                    localPrefs.isCustomSlotUnlocked(slotId, currentUserId)
                if (unlockedNow) {
                  switchCustomSlotShared(slotId)
                  selectedShipId = 10
                  hasExplicitHangarLock = true
                  localPrefs.setPreferredShipId(10)
                  if (engine.hasJoinedMatch && !engine.isHubOverwatchActive) {
                    engine.switchLocalPlayerShip(10, forceFromHangar = true)
                  } else {
                    val chosenSpec = RiftCodex.getShipById(10)
                    engine.localPlayer.shipId = 10
                    engine.localPlayer.spec = chosenSpec
                    engine.localPlayer.liveResonanceRating = engine.getShipResonance(10)
                    engine.syncLocalArmadaSelfSlot()
                  }
                  syncEquippedCosmeticsToEngine(engine)
                  engine.onStatsUpdated()
                  openNexusNode(NexusCoreNode.HANGAR_DECK)
                  true to "SELECTED ${slotId.uppercase(Locale.US)} // OPENING HANGAR BAY"
                } else if (unlockCustomSlotShared(slotId)) {
                  selectedShipId = 10
                  hasExplicitHangarLock = true
                  localPrefs.setPreferredShipId(10)
                  if (engine.hasJoinedMatch && !engine.isHubOverwatchActive) {
                    engine.switchLocalPlayerShip(10, forceFromHangar = true)
                  } else {
                    val chosenSpec = RiftCodex.getShipById(10)
                    engine.localPlayer.shipId = 10
                    engine.localPlayer.spec = chosenSpec
                    engine.localPlayer.liveResonanceRating = engine.getShipResonance(10)
                    engine.syncLocalArmadaSelfSlot()
                  }
                  syncEquippedCosmeticsToEngine(engine)
                  engine.onStatsUpdated()
                  val freeNote =
                    when {
                      engine.peakResonance >= gate.freeAtResonance -> "PEAK RESONANCE MILESTONE"
                      RiftBillingManager.TESTER_INFINITE_SHARDS_ACTIVE -> "TESTER 0 SHARDS DEPLETED"
                      else -> "-${gate.shardCost} RIFT SHARDS"
                    }
                  true to "UNLOCKED ${slotId.uppercase(Locale.US)} ($freeNote)"
                } else {
                  false to
                    "REQUIRES ${gate.freeAtResonance} PEAK RESONANCE (FREE) OR ${gate.shardCost} RIFT SHARDS"
                }
              },
              activeCustomSlotId = activeCustomSlotId,
              cloudLeaderboardEntries = realSwornCloudLeaderboard,
              onRenamePilotCallsign = applyUnifiedCallsignRename,
              onResetLocalPilotTelemetry = {
                localPrefs.resetAllLocalPilotData(
                  if (isGoogleAuthenticated) currentUserId else null
                )
                callsign =
                  if (isGoogleAuthenticated) localPrefs.getCallsign(currentUserId)
                  else localPrefs.getCallsign()
                syndicateTag = localPrefs.getSyndicateTag()
                syndicateName = localPrefs.getSyndicateName()
                hasExplicitHangarLock = false
                selectedShipId = 1
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
                shipResonancesState = localPrefs.getAllShipResonances(currentUserId)
                engine.loadShipResonances(shipResonancesState, 0)
                engine.highestMilestoneLevelClaimed = 0
                engine.spentCustomizationPoints = 0
                engine.unlockedExtraSlots = 0
                engine.aiShipDesignTokens = 0
                engine.localPlayer.callsign = callsign
                engine.localPlayer.syndicateTag = syndicateTag
                engine.localPlayer.bounty = 100
                engine.leaveArmadaToSolo()
                if (engine.hasJoinedMatch && !engine.isHubOverwatchActive) {
                  engine.switchLocalPlayerShip(1, forceFromHangar = true)
                } else {
                  val resetSpec = RiftCodex.getShipById(1)
                  engine.localPlayer.shipId = 1
                  engine.localPlayer.spec = resetSpec
                  engine.localPlayer.liveResonanceRating = 0
                  engine.syncLocalArmadaSelfSlot()
                }
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
              helmInitialSubDeck = helmInitialSubDeck,
              sfxEnabled = sfxEnabled,
              musicEnabled = musicEnabled,
              musicBpm = musicBpm,
              musicVolume = musicVolume,
              sfxVolume = sfxVolume,
              cameraZoom = cameraZoom,
              playfieldTextScale = playfieldTextScale,
              onToggleSfx = toggleSfxShared,
              onToggleMusic = toggleMusicShared,
              onChangeMusicBpm = changeMusicBpmShared,
              onChangeMusicVolume = changeMusicVolShared,
              onChangeSfxVolume = changeSfxVolShared,
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
              onOpenChangelogModal = { showFullChangelogModal = true },
              onOpenAudioSynthForge = {
                forgeInitialTab = NexusForgeTab.AUDIO_SFX
                isForgeFullScreenStage = false
                openNexusNode(NexusCoreNode.STAR_FORGE)
              },
              renderModuleForNode = renderModuleForNode,
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
                      renamedCallsignInCurrentSession = true
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

          if (matchmakingQueueState.isQueueActive) {
            Box(
              modifier =
                Modifier.fillMaxSize()
                  .zIndex(45f)
                  .background(Color(0xD9030712))
                  .clickable(enabled = false) {}
                  .padding(20.dp)
                  .testTag("resonance_matchmaking_modal"),
              contentAlignment = Alignment.Center,
            ) {
              val isExpanding =
                matchmakingQueueState.status == MatchmakingQueueStatus.EXPANDING_BRACKET
              val statusAccent = if (isExpanding) Color(0xFF38BDF8) else Color(0xFFFFD166)
              Surface(
                modifier =
                  Modifier.widthIn(max = 500.dp)
                    .fillMaxWidth()
                    .border(1.5.dp, statusAccent, RoundedCornerShape(16.dp)),
                color = Color(0xF5060D1A),
                shape = RoundedCornerShape(16.dp),
              ) {
                Column(
                  modifier = Modifier.padding(18.dp),
                  verticalArrangement = Arrangement.spacedBy(9.dp),
                  horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                  Text(
                    text = "RESONANCE MATCHMAKING // ${matchmakingQueueState.modeTitle}",
                    color = Color(0xFFFFD166),
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                  )
                  Box(
                    modifier =
                      Modifier.clip(RoundedCornerShape(8.dp))
                        .background(statusAccent.copy(alpha = 0.14f))
                        .border(1.dp, statusAccent.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                        .testTag("matchmaking_status_badge"),
                  ) {
                    Text(
                      text = matchmakingQueueState.statusTransitionLabel,
                      color = statusAccent,
                      fontSize = 10.sp,
                      fontWeight = FontWeight.Black,
                      fontFamily = FontFamily.Monospace,
                    )
                  }
                  Text(
                    text =
                      "${matchmakingQueueState.riftLabel} • TEAM AVG ${matchmakingQueueState.teamAvgResonance} RES",
                    color = Color(0xFFE2E8F0),
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                  )
                  Column(
                    modifier =
                      Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF040A14))
                        .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(10.dp))
                        .padding(10.dp)
                        .testTag("matchmaking_lobby_roster"),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                  ) {
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.SpaceBetween,
                      verticalAlignment = Alignment.CenterVertically,
                    ) {
                      Text(
                        text = "PILOTS IN LOBBY: ${matchmakingQueueState.lobbyPilots.size}",
                        color = Color(0xFF34D399),
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                      )
                      Text(
                        text = "OPPONENTS READY: ${matchmakingQueueState.eligibleOpponentCount}",
                        color = Color(0xFF38BDF8),
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                      )
                    }
                    Row(
                      modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                      horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                      matchmakingQueueState.lobbyPilots.forEach { pilot ->
                        val chipBorder =
                          when {
                            pilot.isLocalPilot -> Color(0xFF00F5D4)
                            pilot.isWingmate -> Color(0xFF34D399)
                            else -> Color(0xFFFFD166)
                          }
                        val synPrefix =
                          if (pilot.syndicateTag.isNotBlank()) "[${pilot.syndicateTag}] " else ""
                        Box(
                          modifier =
                            Modifier.clip(RoundedCornerShape(6.dp))
                              .background(Color(0xFF091426))
                              .border(1.dp, chipBorder.copy(alpha = 0.75f), RoundedCornerShape(6.dp))
                              .padding(horizontal = 8.dp, vertical = 4.dp),
                        ) {
                          Text(
                            text = "$synPrefix${pilot.callsign} • ${pilot.resonance} RES",
                            color = Color.White,
                            fontSize = 8.8.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                          )
                        }
                      }
                    }
                  }
                  Box(
                    modifier =
                      Modifier.fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color(0xFF1E293B)),
                  ) {
                    Box(
                      modifier =
                        Modifier.fillMaxHeight()
                          .fillMaxWidth(matchmakingQueueState.progressFraction)
                          .background(
                            Brush.horizontalGradient(
                              listOf(Color(0xFF00F5D4), statusAccent)
                            )
                          ),
                    )
                  }
                  Text(
                    text =
                      String.format(
                        Locale.US,
                        "QUEUE TIME: %.1fs / %.1fs MAX (%.1fs REMAINING)",
                        matchmakingQueueState.elapsedSec,
                        matchmakingQueueState.maxDurationSec,
                        matchmakingQueueState.remainingSec,
                      ),
                    color = Color(0xFF00F5D4),
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                  )
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                  ) {
                    if (!matchmakingQueueState.requiresOpponentTeam) {
                      Button(
                        onClick = { matchmakingQueueViewModel.deployNow() },
                        colors =
                          ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF0E7490),
                            contentColor = Color.White,
                          ),
                        border = BorderStroke(1.dp, Color(0xFF00F5D4)),
                        shape = RoundedCornerShape(10.dp),
                        modifier =
                          Modifier.weight(1f)
                            .height(40.dp)
                            .testTag("matchmaking_deploy_now_btn"),
                      ) {
                        Text(
                          text = "RIFT IN NOW",
                          fontSize = 11.sp,
                          fontWeight = FontWeight.Black,
                          fontFamily = FontFamily.Monospace,
                        )
                      }
                    }
                    Button(
                      onClick = {
                        matchmakingQueueViewModel.cancelQueue()
                        sfx.playMutedClick()
                      },
                      colors =
                        ButtonDefaults.buttonColors(
                          containerColor = Color(0xFF1E293B),
                          contentColor = Color(0xFFFCA5A5),
                        ),
                      border = BorderStroke(1.dp, Color(0xFFF87171)),
                      shape = RoundedCornerShape(10.dp),
                      modifier =
                        Modifier.weight(1f)
                          .height(40.dp)
                          .testTag("matchmaking_cancel_btn"),
                    ) {
                      Text(
                        text = "CANCEL QUEUE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                      )
                    }
                  }
                }
              }
            }
          }

          val noTeamsMsg = matchmakingQueueState.noticeMessage
          if (noTeamsMsg != null && !matchmakingQueueState.isQueueActive) {
            Box(
              modifier =
                Modifier.fillMaxSize()
                  .zIndex(46f)
                  .background(Color(0xB802050B))
                  .clickable { matchmakingQueueViewModel.dismissNotice() }
                  .padding(20.dp)
                  .testTag("matchmaking_no_teams_banner"),
              contentAlignment = Alignment.Center,
            ) {
              Surface(
                modifier =
                  Modifier.widthIn(max = 400.dp)
                    .fillMaxWidth()
                    .border(1.5.dp, Color(0xFFF87171), RoundedCornerShape(14.dp)),
                color = Color(0xF5060D1A),
                shape = RoundedCornerShape(14.dp),
              ) {
                Column(
                  modifier = Modifier.padding(18.dp),
                  verticalArrangement = Arrangement.spacedBy(10.dp),
                  horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                  Text(
                    text = noTeamsMsg,
                    color = Color(0xFFFCA5A5),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                  )
                  Button(
                    onClick = { matchmakingQueueViewModel.dismissNotice() },
                    colors =
                      ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1E293B),
                        contentColor = Color.White,
                      ),
                    border = BorderStroke(1.dp, Color(0xFF38BDF8)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().height(40.dp),
                  ) {
                    Text(
                      text = "ACKNOWLEDGE",
                      fontSize = 11.sp,
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

private fun CloudCustomGameRoom.toCustomGameConfig(): CustomGameConfig {
  val cleanCode =
    roomCode
      .ifBlank { id }
      .trim()
      .uppercase(java.util.Locale.US)
      .replace(Regex("[^A-Z0-9]"), "")
      .take(8)
  val isPriv = privateRoom || visibility.equals("private", ignoreCase = true)
  val resolvedInstanceId =
    instanceId
      .trim()
      .replace(Regex("[^A-Za-z0-9_\\-]"), "")
      .take(23)
      .ifBlank { "${if (isPriv) "priv" else "pub"}_$cleanCode" }
  val resolvedMode =
    RankedGameMode.entries.firstOrNull {
      it.id.equals(baseMode, ignoreCase = true) || it.name.equals(baseMode, ignoreCase = true)
    } ?: RankedGameMode.SECTOR_CONVERGENCE
  return CustomGameConfig(
    instanceId = resolvedInstanceId,
    roomCode = cleanCode,
    privateRoom = isPriv,
    title =
      title.ifBlank {
        "${hostCallsign.uppercase(java.util.Locale.US)} ${resolvedMode.title.uppercase(java.util.Locale.US)} [$cleanCode]"
      },
    hostCallsign = hostCallsign.ifBlank { "PILOT" },
    baseMode = resolvedMode,
    teamSizeBracket = teamSizeBracket.coerceIn(2, 20),
    matchDurationMinutes = matchDurationMinutes.coerceIn(0, 60),
    allowArmadasInFfa = allowArmadasInFfa,
    botsEnabled = botsEnabled,
    bulletSizeMult = bulletSizeMult.toFloat().coerceIn(0.4f, 4.0f),
    damageMult = damageMult.toFloat().coerceIn(0.25f, 4.0f),
    speedMult = speedMult.toFloat().coerceIn(0.4f, 3.0f),
    healthMult = healthMult.toFloat().coerceIn(0.5f, 4.0f),
    rechargeMult = rechargeMult.toFloat().coerceIn(0.4f, 4.0f),
    crateSpawnMult = crateSpawnMult.toFloat().coerceIn(0.2f, 4.0f),
  )
}

private fun CustomGameConfig.toCloudCustomGameRoom(hostUid: String): CloudCustomGameRoom {
  val code = extractCustomRoomCode(this)
  val isPriv = isPrivateCustomGame(this)
  val cleanInstanceId =
    instanceId
      .trim()
      .replace(Regex("[^A-Za-z0-9_\\-]"), "")
      .take(23)
      .ifBlank { "${if (isPriv) "priv" else "pub"}_$code" }
  return CloudCustomGameRoom(
    id = code,
    roomCode = code,
    instanceId = cleanInstanceId,
    hostUid = hostUid,
    hostCallsign = hostCallsign.ifBlank { "PILOT" }.take(24),
    title = title.ifBlank { "CUSTOM RIFT [$code]" }.take(48),
    visibility = if (isPriv) "private" else "public",
    privateRoom = isPriv,
    baseMode = baseMode.id,
    teamSizeBracket = teamSizeBracket.coerceIn(2, 20),
    matchDurationMinutes = matchDurationMinutes.coerceIn(0, 60),
    allowArmadasInFfa = allowArmadasInFfa,
    botsEnabled = botsEnabled,
    bulletSizeMult = bulletSizeMult.toDouble(),
    damageMult = damageMult.toDouble(),
    speedMult = speedMult.toDouble(),
    healthMult = healthMult.toDouble(),
    rechargeMult = rechargeMult.toDouble(),
    crateSpawnMult = crateSpawnMult.toDouble(),
    createdAtMs = System.currentTimeMillis(),
  )
}

