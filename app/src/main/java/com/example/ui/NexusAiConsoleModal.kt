package com.example.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.view.KeyEvent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
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
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
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
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.R
import com.example.data.ArmadaMemberSlot
import com.example.data.CloudSyncHealth
import com.example.data.LivePilotState
import com.example.data.LocalPilotPrefs
import androidx.compose.runtime.produceState
import com.example.data.LeaderboardEntry
import com.example.data.RiftBillingManager
import com.example.data.RiftCommunityRoles
import com.example.data.SyndicateChatMessage
import com.example.data.SyndicateInfo
import com.example.data.SyndicateWarRecord
import com.example.data.formatSurvivalDurationSec
import com.example.data.snapCustomFivePercentScale
import com.example.engine.CosmeticCatalogItem
import com.example.engine.CustomGameConfig
import com.example.engine.CustomShipLoadoutSlot
import com.example.engine.DailyConduitDirective
import com.example.engine.RankedGameMode
import com.example.engine.RiftCalibrationState
import com.example.engine.RiftCodex
import com.example.engine.RiftGameEngine
import com.example.engine.RiftProgressionCatalog
import com.example.engine.RiftSoundSynthesizer
import com.example.engine.RiftSpriteBaker
import com.example.engine.RiftVisualForgeState
import com.example.engine.ShipSpec
import com.example.engine.SyndicateCrestRecipe
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

private val NEXUS_STARTER_CHIPS =
  listOf(
    "How many pixels are Suppressor's lasers?",
    "Who holds the highest Peak Resonance record?",
    "Theorycraft a 100-pt Rift Frame sniper build",
    "Analyze my Peak Resonance Run & Top-Resonance Takedowns",
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

internal enum class RiftHubSideBayMode {
  HUB_MAIN,
  HANGAR_BAY,
  RIFT_FRAME_BAY,
  SQUADRON_BAY,
  SYNDICATE_BAY,
  GAME_MODES_BAY,
  CONDUIT_BAY,
  SHARD_STORE_BAY,
  DOSSIER_BAY,
  HELM_CODEX_BAY,
  OVERWATCH_INSPECT_BAY,
}

private fun parseSideBayModeKey(rawKey: String): RiftHubSideBayMode =
  when (rawKey.trim().uppercase(Locale.US)) {
    "HANGAR_BAY" -> RiftHubSideBayMode.HANGAR_BAY
    "RIFT_FRAME_BAY" -> RiftHubSideBayMode.RIFT_FRAME_BAY
    "SQUADRON_BAY" -> RiftHubSideBayMode.SQUADRON_BAY
    "SYNDICATE_BAY" -> RiftHubSideBayMode.SYNDICATE_BAY
    "GAME_MODES_BAY", "ACTIVE_RIFTS_BAY" -> RiftHubSideBayMode.GAME_MODES_BAY
    "CONDUIT_BAY" -> RiftHubSideBayMode.CONDUIT_BAY
    "SHARD_STORE_BAY" -> RiftHubSideBayMode.SHARD_STORE_BAY
    "DOSSIER_BAY" -> RiftHubSideBayMode.DOSSIER_BAY
    "HELM_CODEX_BAY" -> RiftHubSideBayMode.HELM_CODEX_BAY
    "OVERWATCH_INSPECT_BAY" -> RiftHubSideBayMode.OVERWATCH_INSPECT_BAY
    else -> RiftHubSideBayMode.HUB_MAIN
  }

internal enum class SquadronPilotFilter {
  ALL,
  SYNDICATE,
  FRIENDS,
}

internal enum class SquadronPilotSort {
  RESONANCE,
  ACTIVITY,
}

internal data class SquadronOnlinePilotItem(
  val uid: String,
  val callsign: String,
  val syndicateTag: String,
  val bestShipId: Int,
  val lockedShipId: Int,
  val resonance: Int,
  val sectorLabel: String,
  val isFriend: Boolean,
  val isInSquadron: Boolean,
  val rawLivePilot: LivePilotState,
)

internal sealed interface OverwatchCircularPrompt {
  data object None : OverwatchCircularPrompt
  data object ResonanceLeaderboard : OverwatchCircularPrompt
  data object RiftFrameDrydock : OverwatchCircularPrompt
  data class FrameSlotRename(val slotId: String, val initialName: String) : OverwatchCircularPrompt
  data class SliderWorkbench(
    val key: String,
    val title: String,
    val subtitle: String,
    val currentValue: Float,
    val minValue: Float,
    val maxValue: Float,
    val stepSize: Float,
    val defaultValue: Float,
    val unitSuffix: String = "",
    val accentHex: Long = 0xFF00F5D4L,
    val presets: List<Pair<String, Float>> = emptyList(),
    val valueFormatter: ((Float) -> String)? = null,
    val onValueChange: (Float) -> Unit = {},
  ) : OverwatchCircularPrompt
  data class ShipInspect(val shipId: Int, val hideChargeButton: Boolean = false) : OverwatchCircularPrompt
  data class PilotInspect(val pilot: SquadronOnlinePilotItem) : OverwatchCircularPrompt
  data class ActionConfirm(
    val kicker: String,
    val title: String,
    val subtitle: String,
    val confirmLabel: String,
    val accentHex: Long,
    val onConfirm: () -> Unit = {},
  ) : OverwatchCircularPrompt
  data class CallsignRename(val initialCallsign: String) : OverwatchCircularPrompt
  data class SyndicateCrestBuilder(val syndicateTag: String) : OverwatchCircularPrompt
  data class InfoStageInspect(
    val kicker: String,
    val title: String,
    val badge: String,
    val summary: String,
    val tacticalNote: String = "",
    val accentHex: Long = 0xFF00F5D4L,
    val shipId: Int = 0,
    val buffTypeIndex: Int = Int.MIN_VALUE,
    val riftAnomalyId: String = "",
    val showStationPreview: Boolean = false,
    val showResonanceCrest: Boolean = false,
    val codexBasicIndex: Int = -1,
    val codexModeIndex: Int = -1,
    val primaryActionLabel: String = "",
    val onPrimaryAction: (() -> Unit)? = null,
    val previewHullSkinId: Int? = null,
    val previewFinisherId: Int? = null,
    val previewWarpStyleId: Int? = null,
    val previewHaloStyleId: Int? = null,
  ) : OverwatchCircularPrompt
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
  val avgResonance: Int = 0,
  val remainingSeconds: Int = 3600,
)

internal fun formatOverwatchResonance(value: Int): String =
  String.format(Locale.US, "%,d", value.coerceAtLeast(0))

internal fun formatOverwatchTimerMmSs(totalSeconds: Int): String {
  val clamped = totalSeconds.coerceAtLeast(0)
  val mins = clamped / 60
  val secs = clamped % 60
  return String.format(Locale.US, "%02d:%02d", mins, secs)
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
  val prefix = parts.firstOrNull()?.lowercase(Locale.US)
  if ((prefix == "priv" || prefix == "pub") && parts.size >= 2) {
    val candidate = parts[1].uppercase(Locale.US).replace(Regex("[^A-Z0-9]"), "").take(8)
    if (candidate.length in 4..8) return candidate
  }
  return generateSixCharRoomCode(instanceId.hashCode().toLong())
}

internal fun extractCustomRoomCode(config: CustomGameConfig): String =
  config.roomCode
    .trim()
    .uppercase(Locale.US)
    .replace(Regex("[^A-Z0-9]"), "")
    .take(8)
    .takeIf { it.length in 4..8 }
    ?: extractCustomRoomCode(config.instanceId)

internal fun isPrivateCustomGame(config: CustomGameConfig): Boolean =
  config.privateRoom || config.instanceId.startsWith("priv_", ignoreCase = true)

private fun riftHubCardTransitionOrder(node: NexusCoreNode): Int =
  when (node) {
    NexusCoreNode.ORBITAL_HUB -> -1
    NexusCoreNode.HANGAR_DECK -> 0
    NexusCoreNode.STAR_FORGE -> 1
    NexusCoreNode.SUBSPACE_COMMS -> 2
    NexusCoreNode.CONVERGENCE_GATE -> 3
    NexusCoreNode.RIFT_CONDUIT -> 4
    NexusCoreNode.SYNDICATE_CITADEL -> 5
    NexusCoreNode.ARCHIVE_CODEX -> 6
    NexusCoreNode.HELM_SYSTEMS -> 7
  }

private fun isLeftSideHubCard(node: NexusCoreNode): Boolean =
  node == NexusCoreNode.HANGAR_DECK ||
    node == NexusCoreNode.STAR_FORGE ||
    node == NexusCoreNode.SUBSPACE_COMMS

@Composable
fun FluxToroidIcon(sizeDp: Dp = 14.dp, modifier: Modifier = Modifier) {
  val visualRev = RiftVisualForgeState.visualRevision
  val spriteRev = RiftSpriteBaker.spriteVisualRevision
  val bmp = remember(visualRev, spriteRev) { RiftSpriteBaker.fluxIconBitmap.asImageBitmap() }
  Canvas(modifier = modifier.size(sizeDp)) {
    drawImage(
      bmp,
      dstSize = IntSize(size.width.roundToInt().coerceAtLeast(1), size.height.roundToInt().coerceAtLeast(1)),
    )
  }
}

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
 * Renders an authentic baked 2.5D ship sprite (`RiftSpriteBaker.getShipSprite`) slowly spinning inside a
 * sculpted gunmetal/cyan socket, and—when `isCharged` is true—rings the ship with rotating 3D Flux icons
 * (`fluxIconBitmap`) that carry from the Hangar bay directly into the Rift Hub view.
 */
@Composable
internal fun Spinning25dShipWithOrbitingFlux(
  shipId: Int,
  shipSpinDeg: Float,
  fluxOrbitRad: Float,
  isCharged: Boolean,
  isBestShip: Boolean = false,
  isSelected: Boolean = false,
  sizeDp: Dp = 54.dp,
  orbitingFluxCount: Int = 5,
  trimStyleOverride: Int? = null,
  previewFinisherId: Int? = null,
  previewWarpStyleId: Int? = null,
  previewHaloStyleId: Int? = null,
  modifier: Modifier = Modifier,
) {
  val clampedId = shipId.coerceIn(1, 10)
  val spriteRev = RiftSpriteBaker.spriteVisualRevision
  val customRev = RiftSpriteBaker.customShip9Version
  val effectiveTrim = (trimStyleOverride ?: RiftSpriteBaker.activeHullTrimStyle).coerceIn(0, 5)
  val visualRev = RiftVisualForgeState.visualRevision
  val shipBmp =
    remember(clampedId, spriteRev, customRev, effectiveTrim, visualRev) {
      RiftSpriteBaker.getShipSprite(
        shipId = clampedId,
        isAllied = true,
        isHitFlashing = false,
        trimStyleOverride = effectiveTrim,
      ).asImageBitmap()
    }
  val fluxBmp = remember(visualRev, spriteRev) { RiftSpriteBaker.fluxIconBitmap.asImageBitmap() }

  Box(
    modifier = modifier.size(sizeDp),
    contentAlignment = Alignment.Center,
  ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val minDim = size.minDimension
      if (minDim <= 6f) return@Canvas
      val cx = center.x
      val cy = center.y
      val socketR = minDim * 0.45f

      // 1. Sculpted 2.5D Gunmetal/Abyssal Turret Well (#02050B -> #091528 -> #0B192E)
      drawCircle(
        color = Color(0xD9010308),
        radius = socketR,
        center = Offset(cx, cy + 1.4.dp.toPx()),
      )
      drawCircle(
        brush =
          Brush.radialGradient(
            colors =
              when {
                isCharged -> listOf(Color(0xFF102E4A), Color(0xFF060D1A), Color(0xFF02050B))
                isBestShip -> listOf(Color(0xFF241E0A), Color(0xFF091528), Color(0xFF02050B))
                else -> listOf(Color(0xFF0B192E), Color(0xFF060D1A), Color(0xFF02050B))
              },
            center = center,
            radius = socketR,
          ),
        radius = socketR,
        center = center,
      )

      val rimColor =
        when {
          isCharged -> Color(0xFF00F5D4)
          isBestShip -> Color(0xFFFFD700)
          isSelected -> Color(0xFF38BDF8)
          else -> Color(0xFF38BDF8).copy(alpha = 0.52f)
        }
      drawCircle(
        brush =
          Brush.verticalGradient(
            listOf(Color.White.copy(alpha = 0.42f), rimColor, Color(0xFF02050B))
          ),
        radius = socketR,
        center = center,
        style = Stroke(width = if (isCharged || isBestShip || isSelected) 1.45.dp.toPx() else 1.0.dp.toPx()),
      )

      // Distinct Best-Ship Solar-Gold Crown Notches (differentiating Best Ship vs Charged/Selected Ship)
      if (isBestShip && !isCharged) {
        val crownR = socketR * 0.92f
        for (k in 0 until 4) {
          drawArc(
            color = Color(0xFFFFD700),
            startAngle = k * 90f - 14f,
            sweepAngle = 28f,
            useCenter = false,
            topLeft = Offset(cx - crownR, cy - crownR),
            size = Size(crownR * 2f, crownR * 2f),
            style = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round),
          )
        }
      }

      // Charged Harmonic Plasma Orbit Track
      if (isCharged) {
        val orbitR = minDim * 0.425f
        drawCircle(
          color = Color(0x4000F5D4),
          radius = orbitR,
          center = center,
          style = Stroke(width = 1.25.dp.toPx()),
        )
      }
    }

    // 2. Slowly Spinning 2.5D Ship Sprite (with subtle phase offset per ship hull)
    val hasCosmeticFx =
      previewHaloStyleId != null || previewWarpStyleId != null || previewFinisherId != null
    val hullFraction =
      when {
        hasCosmeticFx -> 0.58f
        isCharged -> 0.66f
        else -> 0.72f
      }
    Image(
      bitmap = shipBmp,
      contentDescription = null,
      contentScale = ContentScale.Fit,
      modifier =
        Modifier.fillMaxSize(hullFraction)
          .graphicsLayer {
            rotationZ = (shipSpinDeg + clampedId * 27f) % 360f
          },
    )

    // 3. Orbiting 3D Flux Icons Ringing the Charged Ship (suppressed during dedicated FX preview)
    if (isCharged && orbitingFluxCount > 0 && !hasCosmeticFx) {
      Canvas(modifier = Modifier.fillMaxSize()) {
        val minDim = size.minDimension
        if (minDim <= 8f) return@Canvas
        val cx = center.x
        val cy = center.y
        val orbitR = minDim * 0.425f
        val iconPx = (minDim * 0.22f).roundToInt().coerceAtLeast(8)
        val halfIcon = iconPx * 0.5f
        for (idx in 0 until orbitingFluxCount) {
          val ang = fluxOrbitRad + idx * ((2.0 * PI) / orbitingFluxCount.toDouble()).toFloat()
          val ix = (cx + cos(ang) * orbitR - halfIcon).roundToInt()
          val iy = (cy + sin(ang) * orbitR - halfIcon).roundToInt()
          drawImage(
            image = fluxBmp,
            dstOffset = IntOffset(ix, iy),
            dstSize = IntSize(iconPx, iconPx),
          )
        }
      }
    }

    // 4. Live Animated Overwatch Preview FX (Resonance Leader Halos 0..3, Warp Signatures 0..3, Kill Finishers 0..4)
    if (hasCosmeticFx) {
      Canvas(modifier = Modifier.fillMaxSize()) {
        val minDim = size.minDimension
        if (minDim <= 10f) return@Canvas
        val cx = center.x
        val cy = center.y
        val baseR = minDim * 0.36f
        val cycleT = ((fluxOrbitRad / (2.0 * PI)).toFloat() % 1f).let { if (it < 0f) it + 1f else it }
        val rotDeg = cycleT * 360f

        previewHaloStyleId?.let { rawStyle ->
          val style = rawStyle.coerceIn(0, 3)
          when (style) {
            0 -> {
              val ringR = baseR * 1.12f
              drawCircle(
                color = Color(0xFFF59E0B).copy(alpha = 0.72f),
                radius = ringR,
                center = center,
                style = Stroke(width = 1.4.dp.toPx()),
              )
              for (i in 0 until 4) {
                val a = ((rotDeg * 0.85f + i * 90f) * PI / 180.0).toFloat()
                drawLine(
                  color = Color(0xFFFDE047),
                  start = Offset(cx + cos(a) * (ringR - 3.dp.toPx()), cy + sin(a) * (ringR - 3.dp.toPx())),
                  end = Offset(cx + cos(a) * (ringR + 3.dp.toPx()), cy + sin(a) * (ringR + 3.dp.toPx())),
                  strokeWidth = 1.6.dp.toPx(),
                  cap = StrokeCap.Round,
                )
              }
            }
            1 -> {
              val innerR = baseR * 1.04f
              val outerR = baseR * 1.24f
              drawCircle(
                color = Color(0xFFFACC15).copy(alpha = 0.85f),
                radius = innerR,
                center = center,
                style = Stroke(width = 1.5.dp.toPx()),
              )
              drawCircle(
                color = Color(0xFFFEF08A).copy(alpha = 0.45f),
                radius = outerR,
                center = center,
                style = Stroke(width = 1.0.dp.toPx()),
              )
              val chevPath = Path()
              for (i in 0 until 6) {
                val a = ((rotDeg + i * 60f) * PI / 180.0).toFloat()
                val ax = cos(a)
                val ay = sin(a)
                val tx = -ay
                val ty = ax
                val tipX = cx + ax * (outerR + 3.2.dp.toPx())
                val tipY = cy + ay * (outerR + 3.2.dp.toPx())
                val b1x = cx + ax * (innerR + 0.8.dp.toPx()) - tx * 2.6.dp.toPx()
                val b1y = cy + ay * (innerR + 0.8.dp.toPx()) - ty * 2.6.dp.toPx()
                val b2x = cx + ax * (innerR + 0.8.dp.toPx()) + tx * 2.6.dp.toPx()
                val b2y = cy + ay * (innerR + 0.8.dp.toPx()) + ty * 2.6.dp.toPx()
                chevPath.reset()
                chevPath.moveTo(tipX, tipY)
                chevPath.lineTo(b1x, b1y)
                chevPath.lineTo(b2x, b2y)
                chevPath.close()
                drawPath(chevPath, color = Color(0xFFFFD700).copy(alpha = 0.90f))
              }
            }
            2 -> {
              val ringR = baseR * 1.15f
              drawCircle(
                color = Color(0xFFA855F7).copy(alpha = 0.55f),
                radius = ringR,
                center = center,
                style = Stroke(width = 1.3.dp.toPx()),
              )
              val dSize = 3.6.dp.toPx()
              val dPath = Path()
              for (i in 0 until 4) {
                val a = ((rotDeg * 1.25f + i * 90f) * PI / 180.0).toFloat()
                val px = cx + cos(a) * ringR
                val py = cy + sin(a) * ringR
                dPath.reset()
                dPath.moveTo(px, py - dSize)
                dPath.lineTo(px + dSize * 0.72f, py)
                dPath.lineTo(px, py + dSize)
                dPath.lineTo(px - dSize * 0.72f, py)
                dPath.close()
                drawPath(dPath, color = Color(0xFFE9D5FF))
                drawPath(dPath, color = Color(0xFFC084FC), style = Stroke(width = 1.0.dp.toPx()))
              }
            }
            else -> {
              val outerR = baseR * 1.20f
              val innerR = baseR * 1.02f
              for (i in 0 until 3) {
                drawArc(
                  color = Color(0xFF00F5D4).copy(alpha = 0.92f),
                  startAngle = rotDeg * 1.3f + i * 120f,
                  sweepAngle = 56f,
                  useCenter = false,
                  topLeft = Offset(cx - outerR, cy - outerR),
                  size = Size(outerR * 2f, outerR * 2f),
                  style = Stroke(width = 2.0.dp.toPx(), cap = StrokeCap.Round),
                )
                drawArc(
                  color = Color(0xFFFB7185).copy(alpha = 0.88f),
                  startAngle = -rotDeg * 1.55f + i * 120f + 40f,
                  sweepAngle = 48f,
                  useCenter = false,
                  topLeft = Offset(cx - innerR, cy - innerR),
                  size = Size(innerR * 2f, innerR * 2f),
                  style = Stroke(width = 1.7.dp.toPx(), cap = StrokeCap.Round),
                )
              }
            }
          }
        }

        previewWarpStyleId?.let { rawStyle ->
          val style = rawStyle.coerceIn(0, 3)
          val p = cycleT
          val fade = (1f - p).coerceIn(0f, 1f)
          when (style) {
            0 -> {
              val rOuter = baseR * (0.45f + p * 0.95f)
              val rInner = baseR * (0.28f + p * 0.62f)
              drawCircle(
                color = Color(0xFF00F5D4).copy(alpha = 0.86f * fade),
                radius = rOuter,
                center = center,
                style = Stroke(width = 2.1.dp.toPx()),
              )
              drawCircle(
                color = Color(0xFF38BDF8).copy(alpha = 0.56f * fade),
                radius = rInner,
                center = center,
                style = Stroke(width = 1.3.dp.toPx()),
              )
            }
            1 -> {
              val rFlare = baseR * (0.55f + p * 0.80f)
              for (k in 0 until 2) {
                drawArc(
                  color = Color(0xFFFACC15).copy(alpha = 0.90f * fade),
                  startAngle = rotDeg * 1.4f + k * 180f,
                  sweepAngle = 115f,
                  useCenter = false,
                  topLeft = Offset(cx - rFlare, cy - rFlare),
                  size = Size(rFlare * 2f, rFlare * 2f),
                  style = Stroke(width = 2.3.dp.toPx(), cap = StrokeCap.Round),
                )
              }
              drawCircle(
                color = Color(0xFFFEF08A).copy(alpha = 0.30f * fade),
                radius = rFlare * 0.72f,
                center = center,
              )
            }
            2 -> {
              val rRing = baseR * (0.50f + p * 0.84f)
              drawCircle(
                color = Color(0xFFA855F7).copy(alpha = 0.82f * fade),
                radius = rRing,
                center = center,
                style = Stroke(width = 1.9.dp.toPx()),
              )
              for (i in 0 until 8) {
                val a = ((i * 45f + rotDeg * 0.9f) * PI / 180.0).toFloat()
                val r1 = baseR * (0.34f + p * 0.48f)
                val r2 = r1 + baseR * 0.34f * fade
                drawLine(
                  color = Color(0xFFE9D5FF).copy(alpha = 0.90f * fade),
                  start = Offset(cx + cos(a) * r1, cy + sin(a) * r1),
                  end = Offset(cx + cos(a) * r2, cy + sin(a) * r2),
                  strokeWidth = 1.6.dp.toPx(),
                  cap = StrokeCap.Round,
                )
              }
            }
            else -> {
              val rShock = baseR * (0.42f + p * 0.98f)
              drawCircle(
                color = Color(0xFFF43F5E).copy(alpha = 0.92f * fade),
                radius = rShock,
                center = center,
                style = Stroke(width = 2.6.dp.toPx()),
              )
              drawCircle(
                color = Color(0xFFFDA4AF).copy(alpha = 0.65f * fade),
                radius = rShock * 0.68f,
                center = center,
                style = Stroke(width = 1.3.dp.toPx()),
              )
            }
          }
        }

        previewFinisherId?.let { rawStyle ->
          val style = rawStyle.coerceIn(0, 4)
          val p = cycleT
          val fade = (1f - p).coerceIn(0f, 1f)
          when (style) {
            0 -> {
              val rOuter = baseR * (0.36f + p * 1.05f)
              drawCircle(
                color = Color(0xFF00F5D4).copy(alpha = 0.90f * fade),
                radius = rOuter,
                center = center,
                style = Stroke(width = 2.4.dp.toPx()),
              )
              drawCircle(
                color = Color(0xFFF59E0B).copy(alpha = 0.42f * fade),
                radius = rOuter * 0.65f,
                center = center,
              )
            }
            1 -> {
              if (p < 0.50f) {
                val pull = 1f - (p / 0.50f)
                val rIn = baseR * (0.28f + pull * 1.02f)
                drawCircle(
                  color = Color(0xFF6366F1).copy(alpha = 0.86f),
                  radius = rIn,
                  center = center,
                  style = Stroke(width = 2.0.dp.toPx()),
                )
                for (i in 0 until 8) {
                  val a = ((i * 45f + rotDeg) * PI / 180.0).toFloat()
                  drawLine(
                    color = Color(0xFFA5B4FC).copy(alpha = 0.85f),
                    start = Offset(cx + cos(a) * rIn, cy + sin(a) * rIn),
                    end = Offset(cx + cos(a) * (rIn * 0.52f), cy + sin(a) * (rIn * 0.52f)),
                    strokeWidth = 1.4.dp.toPx(),
                    cap = StrokeCap.Round,
                  )
                }
              } else {
                val snapP = (p - 0.50f) / 0.50f
                val snapFade = (1f - snapP).coerceIn(0f, 1f)
                val rSnap = baseR * (0.32f + snapP * 1.08f)
                drawCircle(
                  color = Color(0xFFE0E7FF).copy(alpha = 0.94f * snapFade),
                  radius = rSnap,
                  center = center,
                  style = Stroke(width = 2.6.dp.toPx()),
                )
              }
            }
            2 -> {
              val rCrown = baseR * (0.42f + p * 0.95f)
              drawCircle(
                color = Color(0xFFFFD700).copy(alpha = 0.92f * fade),
                radius = rCrown,
                center = center,
                style = Stroke(width = 2.3.dp.toPx()),
              )
              for (i in 0 until 12) {
                val a = ((i * 30f + rotDeg * 0.5f) * PI / 180.0).toFloat()
                val r1 = rCrown * 0.52f
                val r2 = rCrown * 1.15f
                drawLine(
                  color = Color(0xFFFEF08A).copy(alpha = 0.88f * fade),
                  start = Offset(cx + cos(a) * r1, cy + sin(a) * r1),
                  end = Offset(cx + cos(a) * r2, cy + sin(a) * r2),
                  strokeWidth = 1.5.dp.toPx(),
                  cap = StrokeCap.Round,
                )
              }
            }
            3 -> {
              val rShatter = baseR * (0.38f + p * 0.96f)
              val shardPath = Path()
              for (i in 0 until 8) {
                val a = ((i * 45f + rotDeg * 0.65f) * PI / 180.0).toFloat()
                val ax = cos(a)
                val ay = sin(a)
                val tx = -ay
                val ty = ax
                val tipR = rShatter * 1.16f
                val baseShardR = rShatter * 0.68f
                shardPath.reset()
                shardPath.moveTo(cx + ax * tipR, cy + ay * tipR)
                shardPath.lineTo(cx + ax * baseShardR - tx * 2.8.dp.toPx(), cy + ay * baseShardR - ty * 2.8.dp.toPx())
                shardPath.lineTo(cx + ax * baseShardR + tx * 2.8.dp.toPx(), cy + ay * baseShardR + ty * 2.8.dp.toPx())
                shardPath.close()
                drawPath(shardPath, color = Color(0xFFC084FC).copy(alpha = 0.88f * fade))
              }
              drawCircle(
                color = Color(0xFFA855F7).copy(alpha = 0.72f * fade),
                radius = rShatter * 0.84f,
                center = center,
                style = Stroke(width = 1.6.dp.toPx()),
              )
            }
            else -> {
              val rApex = baseR * (0.36f + p * 1.04f)
              drawCircle(
                color = Color(0xFFFB7185).copy(alpha = 0.92f * fade),
                radius = rApex,
                center = center,
                style = Stroke(width = 2.4.dp.toPx()),
              )
              for (k in 0 until 3) {
                drawArc(
                  color = Color(0xFF00F5D4).copy(alpha = 0.90f * fade),
                  startAngle = rotDeg * 1.6f + k * 120f,
                  sweepAngle = 72f,
                  useCenter = false,
                  topLeft = Offset(cx - rApex * 0.78f, cy - rApex * 0.78f),
                  size = Size(rApex * 1.56f, rApex * 1.56f),
                  style = Stroke(width = 2.0.dp.toPx(), cap = StrokeCap.Round),
                )
              }
            }
          }
        }
      }
    }
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
        .minimumInteractiveComponentSize()
        .size(48.dp)
        .clip(CircleShape)
        .semantics { contentDescription = desc }
        .clickable(enabled = !unlocked) { onClick() }
        .testTag("hud_btn_nexus_orb"),
    contentAlignment = Alignment.Center,
  ) {
    SwirlingRiftVortexGlyph(modifier = Modifier.size(36.dp), isStreaming = isStreaming)
  }
}

@Composable
fun SwirlingRiftHeaderPill(onClick: () -> Unit, tag: String, isStreaming: Boolean = false) {
  val desc = stringResource(R.string.nexus_orb_content_desc)
  Box(
    modifier =
      Modifier.minimumInteractiveComponentSize()
        .height(48.dp)
        .semantics { contentDescription = desc }
        .clickable { onClick() }
        .padding(vertical = 6.dp)
        .testTag(tag),
    contentAlignment = Alignment.Center,
  ) {
    Row(
      modifier =
        Modifier.clip(RoundedCornerShape(8.dp))
          .background(Brush.verticalGradient(listOf(Color(0xFF102440), Color(0xFF071326))))
          .border(
            1.1.dp,
            Brush.horizontalGradient(listOf(Color(0xFF00F5D4), Color(0xFFB388FF))),
            RoundedCornerShape(8.dp),
          )
          .padding(horizontal = 10.dp, vertical = 5.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      SwirlingRiftVortexGlyph(modifier = Modifier.size(19.dp), isStreaming = isStreaming)
      Text(
        "RIFT HUB",
        color = Color(0xFF00F5D4),
        fontSize = 9.5.sp,
        fontWeight = FontWeight.Black,
        fontFamily = FontFamily.Monospace,
        letterSpacing = 0.5.sp,
      )
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
  Row(
    modifier =
      Modifier.fillMaxWidth()
        .height(48.dp)
        .clip(RoundedCornerShape(10.dp))
        .background(
          Brush.verticalGradient(
            listOf(Color(0xF810203A), Color(0xF5071122), Color(0xFA040A15))
          )
        )
        .border(
          1.2.dp,
          Brush.horizontalGradient(listOf(Color(0xAA00F5D4), Color(0x6638BDF8), Color(0x99FACC15))),
          RoundedCornerShape(10.dp),
        )
        .padding(horizontal = 8.dp)
        .horizontalScroll(rememberScrollState())
        .testTag("nexus_master_module_bar"),
    horizontalArrangement = Arrangement.spacedBy(6.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    NexusRibbonPill("RIFT HUB", activeModuleKey == "NEXUS", Color(0xFF00F5D4), "nexus_module_pill_lobby", onSelectNexusLobby)
    NexusRibbonPill("HANGAR DECK", activeModuleKey == "HANGAR", Color(0xFF38BDF8), "nexus_module_pill_hangar", onOpenHangar)
    NexusRibbonPill("STAR FORGE", activeModuleKey == "FORGE", Color(0xFFC084FC), "nexus_module_pill_forge", onOpenForge)
    NexusRibbonPill("RIFT CONDUIT", activeModuleKey == "PROGRESSION", Color(0xFFE879F9), "nexus_module_pill_progression", onOpenProgression)
    NexusRibbonPill("SQUADRON", activeModuleKey == "ARMADA", Color(0xFF60A5FA), "nexus_module_pill_armada", onOpenArmadaModal)
    NexusRibbonPill("SYNDICATES", activeModuleKey == "SYNDICATES", Color(0xFFF59E0B), "nexus_module_pill_syndicates", onOpenSyndicateDirectory)
    NexusRibbonPill("PILOT DOSSIER", activeModuleKey == "DOSSIER", Color(0xFFA78BFA), "nexus_module_pill_dossier", onOpenDossier)
    NexusRibbonPill("TACTICAL CODEX", activeModuleKey == "CODEX", Color(0xFF34D399), "nexus_module_pill_codex", onOpenCodex)
    NexusRibbonPill("HELM SYSTEMS", activeModuleKey == "SETTINGS", Color(0xFF94A3B8), "nexus_module_pill_settings", onOpenSettings)
    if (showReturnToArenaButton) {
      NexusRibbonPill("RIFT IN", false, Color(0xFF00F5D4), "nexus_module_pill_return", onReturnToArena)
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
      Modifier.minimumInteractiveComponentSize()
        .height(48.dp)
        .clickable { onClick() }
        .padding(vertical = 6.dp)
        .testTag(testTag),
    contentAlignment = Alignment.Center,
  ) {
    Box(
      modifier =
        Modifier.clip(RoundedCornerShape(8.dp))
          .background(
            if (selected) {
              Brush.verticalGradient(listOf(accentColor.copy(alpha = 0.30f), accentColor.copy(alpha = 0.14f)))
            } else {
              Brush.verticalGradient(listOf(Color(0xE6101E36), Color(0xE6071020)))
            }
          )
          .border(
            if (selected) 1.3.dp else 1.dp,
            if (selected) accentColor else accentColor.copy(alpha = 0.55f),
            RoundedCornerShape(8.dp),
          )
          .padding(horizontal = 10.dp, vertical = 5.dp),
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
  publicCustomGames: List<CustomGameConfig> = emptyList(),
  onLookupCustomRoomByCode: ((String, (CustomGameConfig?, String) -> Unit) -> Unit)? = null,
  isNetworkOnline: Boolean = true,
  cloudSyncHealth: CloudSyncHealth = CloudSyncHealth(),
  onSwitchAccount: () -> Unit = {},
  requestedSideBayKey: String = "HUB_MAIN",
  requestedSideBayNonce: Int = 0,
  syndicateDirectory: List<SyndicateInfo> = emptyList(),
  syndicateStatusMessage: String? = null,
  onJoinOfficialSyndicate: (SyndicateInfo) -> Unit = {},
  onLeaveCurrentSyndicate: () -> Unit = {},
  onCharterCustomSyndicate: (tag: String, name: String, motto: String) -> Unit = { _, _, _ -> },
  onClaimDailyDirectiveSlot: (Int) -> Long = { 0L },
  onRerollDailyDirectiveSlot: (Int) -> Boolean = { false },
  onClaimDailySweepBonus: () -> Long = { 0L },
  onUnlockOrEquipCosmeticItem: (String, CosmeticCatalogItem) -> Pair<Boolean, String> = { _, _ -> false to "" },
  onUnlockOrSelectFrameSlotId: (String) -> Pair<Boolean, String> = { false to "" },
  activeCustomSlotId: String = "Frame A",
  activeCustomSlot: CustomShipLoadoutSlot = CustomShipLoadoutSlot(),
  onSaveCustomSlot: (CustomShipLoadoutSlot) -> Unit = {},
  cloudLeaderboard: List<LeaderboardEntry> = emptyList(),
  cloudLeaderboardEntries: List<LeaderboardEntry> = cloudLeaderboard,
  onRenamePilotCallsign: (String) -> Pair<Boolean, String> = { false to "" },
  onResetLocalPilotTelemetry: () -> Unit = {},
  helmInitialSubDeck: Int = 0,
  sfxEnabled: Boolean = true,
  musicEnabled: Boolean = true,
  musicBpm: Int = 118,
  musicVolume: Float = 0.70f,
  sfxVolume: Float = 0.85f,
  cameraZoom: Float = 1.0f,
  playfieldTextScale: Float = 1.0f,
  onToggleSfx: () -> Unit = {},
  onToggleMusic: () -> Unit = {},
  onChangeMusicBpm: (Int) -> Unit = {},
  onChangeMusicVolume: (Float) -> Unit = {},
  onChangeSfxVolume: (Float) -> Unit = {},
  onChangeCameraZoom: (Float) -> Unit = {},
  onChangePlayfieldTextScale: (Float) -> Unit = {},
  onOpenChangelogModal: () -> Unit = {},
  onOpenAudioSynthForge: () -> Unit = {},
  updateStatusLabel: String = "v1.0.$CURRENT_RIFT_BUILD_VERSION UP TO DATE",
  hasUpdateAvailable: Boolean = false,
  onCheckOrApplyUpdate: () -> Unit = {},
  renderModuleForNode: (@Composable (NexusCoreNode) -> Unit)? = null,
  embeddedModuleContent: (@Composable () -> Unit)? = null,
) {
  val params =
    NexusAiConsoleParams(
      messages = messages,
      streamState = streamState,
      lastFailedQuestion = lastFailedQuestion,
      activeShipSummary = activeShipSummary,
      pilotLevel = pilotLevel,
      pointBudget = pointBudget,
      highestBountySummary = highestBountySummary,
      onSendQuestion = onSendQuestion,
      onRetryLastQuestion = onRetryLastQuestion,
      onClearHistory = onClearHistory,
      onCloseModal = onCloseModal,
      callsign = callsign,
      syndicateTag = syndicateTag,
      syndicateName = syndicateName,
      activeShipId = activeShipId,
      bestShipId = bestShipId,
      riftShards = riftShards,
      unreadCommsCount = unreadCommsCount,
      currentUserId = currentUserId,
      bootToNexusHub = bootToNexusHub,
      onToggleBootToNexusHub = onToggleBootToNexusHub,
      totalOnlinePilots = totalOnlinePilots,
      syndicateOnlineCount = syndicateOnlineCount,
      strikeWingOnlineCount = strikeWingOnlineCount,
      presetArmadasReadyCount = presetArmadasReadyCount,
      activeArmadaName = activeArmadaName,
      armadaSlots = armadaSlots,
      onlineHumanPilots = onlineHumanPilots,
      onResolvePilotBestShipId = onResolvePilotBestShipId,
      activeRankedMode = activeRankedMode,
      activeTeamSizeBracket = activeTeamSizeBracket,
      humanPilotCountByMode = humanPilotCountByMode,
      canQueueSyndicateWars = canQueueSyndicateWars,
      syndicateWarRecord = syndicateWarRecord,
      onDeployToRankedMode = onDeployToRankedMode,
      onLeaveMatch = onLeaveMatch,
      onOpenArmadaModal = onOpenArmadaModal,
      onQuickFormSyndicateArmada = onQuickFormSyndicateArmada,
      onOpenSyndicatesModal = onOpenSyndicatesModal,
      onOpenSyndicateDirectory = onOpenSyndicateDirectory,
      onOpenHangar = onOpenHangar,
      onOpenDossier = onOpenDossier,
      onOpenCodex = onOpenCodex,
      onOpenSettings = onOpenSettings,
      onOpenCalibration = onOpenCalibration,
      onOpenForge = onOpenForge,
      onOpenHangarForShip = onOpenHangarForShip,
      canAccessSyndicateServer = canAccessSyndicateServer,
      syndicateCustomChannels = syndicateCustomChannels,
      syndicateInvitedGuests = syndicateInvitedGuests,
      onAddSyndicateChannel = onAddSyndicateChannel,
      onDeleteSyndicateChannel = onDeleteSyndicateChannel,
      onInviteSyndicateGuest = onInviteSyndicateGuest,
      onRevokeSyndicateGuest = onRevokeSyndicateGuest,
      lobbyMessages = lobbyMessages,
      onSendChannelMessage = onSendChannelMessage,
      onSubmitBugReport = onSubmitBugReport,
      onShareActiveRiftFrameBuild = onShareActiveRiftFrameBuild,
      onLoadSharedRiftFrameBuild = onLoadSharedRiftFrameBuild,
      onPostStrikeWingInvite = onPostStrikeWingInvite,
      onPostPrivateRoomInvite = onPostPrivateRoomInvite,
      onJoinSharedStrikeWing = onJoinSharedStrikeWing,
      onAddOnlinePilotFriend = onAddOnlinePilotFriend,
      onInviteOnlinePilotToWing = onInviteOnlinePilotToWing,
      engine = engine,
      isInMatch = isInMatch,
      onEnterPracticeRift = onEnterPracticeRift,
      onOpenHowToPlayTutorial = onOpenHowToPlayTutorial,
      initialLayoutMode = initialLayoutMode,
      activeModuleKey = activeModuleKey,
      activeNode = activeNode,
      onSelectNode = onSelectNode,
      syndicateSubDeck = syndicateSubDeck,
      onSelectSyndicateSubDeck = onSelectSyndicateSubDeck,
      archiveSubDeck = archiveSubDeck,
      onSelectArchiveSubDeck = onSelectArchiveSubDeck,
      onSelectNexusLobby = onSelectNexusLobby,
      onOpenProgression = onOpenProgression,
      isCalibrationLivePeeking = isCalibrationLivePeeking,
      hideTopHeaderForFullScreenStage = hideTopHeaderForFullScreenStage,
      liveResonance = liveResonance,
      isGoogleAuthenticated = isGoogleAuthenticated,
      overwatchInstances = overwatchInstances,
      selectedOverwatchKey = selectedOverwatchKey,
      favoriteOverwatchKey = favoriteOverwatchKey,
      onSelectOverwatchKey = onSelectOverwatchKey,
      onToggleFavoriteOverwatchKey = onToggleFavoriteOverwatchKey,
      onQuickZoneIntoOverwatch = onQuickZoneIntoOverwatch,
      onUpdateOverwatchViewport = onUpdateOverwatchViewport,
      overwatchZoom = overwatchZoom,
      onOverwatchTransformGesture = onOverwatchTransformGesture,
      onResetOverwatchCamera = onResetOverwatchCamera,
      onStageRankedModeViaHangar = onStageRankedModeViaHangar,
      onStageCustomGameViaHangar = onStageCustomGameViaHangar,
      onStageExistingCustomInstanceViaHangar = onStageExistingCustomInstanceViaHangar,
      publicCustomGames = publicCustomGames,
      onLookupCustomRoomByCode = onLookupCustomRoomByCode,
      isNetworkOnline = isNetworkOnline,
      cloudSyncHealth = cloudSyncHealth,
      onSwitchAccount = onSwitchAccount,
      requestedSideBayKey = requestedSideBayKey,
      requestedSideBayNonce = requestedSideBayNonce,
      syndicateDirectory = syndicateDirectory,
      syndicateStatusMessage = syndicateStatusMessage,
      onJoinOfficialSyndicate = onJoinOfficialSyndicate,
      onLeaveCurrentSyndicate = onLeaveCurrentSyndicate,
      onCharterCustomSyndicate = onCharterCustomSyndicate,
      onClaimDailyDirectiveSlot = onClaimDailyDirectiveSlot,
      onRerollDailyDirectiveSlot = onRerollDailyDirectiveSlot,
      onClaimDailySweepBonus = onClaimDailySweepBonus,
      onUnlockOrEquipCosmeticItem = onUnlockOrEquipCosmeticItem,
      onUnlockOrSelectFrameSlotId = onUnlockOrSelectFrameSlotId,
      activeCustomSlotId = activeCustomSlotId,
      activeCustomSlot = activeCustomSlot,
      onSaveCustomSlot = onSaveCustomSlot,
      cloudLeaderboardEntries =
        if (cloudLeaderboard.isNotEmpty()) cloudLeaderboard else cloudLeaderboardEntries,
      onRenamePilotCallsign = onRenamePilotCallsign,
      onResetLocalPilotTelemetry = onResetLocalPilotTelemetry,
      helmInitialSubDeck = helmInitialSubDeck,
      sfxEnabled = sfxEnabled,
      musicEnabled = musicEnabled,
      musicBpm = musicBpm,
      musicVolume = musicVolume,
      sfxVolume = sfxVolume,
      cameraZoom = cameraZoom,
      playfieldTextScale = playfieldTextScale,
      onToggleSfx = onToggleSfx,
      onToggleMusic = onToggleMusic,
      onChangeMusicBpm = onChangeMusicBpm,
      onChangeMusicVolume = onChangeMusicVolume,
      onChangeSfxVolume = onChangeSfxVolume,
      onChangeCameraZoom = onChangeCameraZoom,
      onChangePlayfieldTextScale = onChangePlayfieldTextScale,
      onOpenChangelogModal = onOpenChangelogModal,
      onOpenAudioSynthForge = onOpenAudioSynthForge,
      updateStatusLabel = updateStatusLabel,
      hasUpdateAvailable = hasUpdateAvailable,
      onCheckOrApplyUpdate = onCheckOrApplyUpdate,
      renderModuleForNode = renderModuleForNode,
      embeddedModuleContent = embeddedModuleContent,
    )
  NexusAiConsoleRootContent(params = params)
}

private class NexusAiConsoleParams(
  val messages: List<NexusChatMessage>,
  val streamState: NexusStreamState,
  val lastFailedQuestion: String?,
  val activeShipSummary: String,
  val pilotLevel: Int,
  val pointBudget: Int,
  val highestBountySummary: String,
  val onSendQuestion: (String) -> Unit,
  val onRetryLastQuestion: () -> Unit,
  val onClearHistory: () -> Unit,
  val onCloseModal: () -> Unit,
  val callsign: String,
  val syndicateTag: String,
  val syndicateName: String,
  val activeShipId: Int,
  val bestShipId: Int,
  val riftShards: Long,
  val unreadCommsCount: Int,
  val currentUserId: String,
  val bootToNexusHub: Boolean,
  val onToggleBootToNexusHub: (Boolean) -> Unit,
  val totalOnlinePilots: Int,
  val syndicateOnlineCount: Int,
  val strikeWingOnlineCount: Int,
  val presetArmadasReadyCount: Int,
  val activeArmadaName: String,
  val armadaSlots: List<ArmadaMemberSlot>,
  val onlineHumanPilots: List<LivePilotState>,
  val onResolvePilotBestShipId: (uid: String, fallbackShipId: Int) -> Int,
  val activeRankedMode: RankedGameMode,
  val activeTeamSizeBracket: Int,
  val humanPilotCountByMode: (RankedGameMode) -> Int,
  val canQueueSyndicateWars: Pair<Boolean, String>,
  val syndicateWarRecord: SyndicateWarRecord,
  val onDeployToRankedMode: (RankedGameMode, Int) -> Unit,
  val onLeaveMatch: () -> Unit,
  val onOpenArmadaModal: () -> Unit,
  val onQuickFormSyndicateArmada: () -> Unit,
  val onOpenSyndicatesModal: () -> Unit,
  val onOpenSyndicateDirectory: () -> Unit,
  val onOpenHangar: () -> Unit,
  val onOpenDossier: () -> Unit,
  val onOpenCodex: () -> Unit,
  val onOpenSettings: () -> Unit,
  val onOpenCalibration: () -> Unit,
  val onOpenForge: () -> Unit,
  val onOpenHangarForShip: (Int) -> Unit,
  val canAccessSyndicateServer: Boolean,
  val syndicateCustomChannels: List<String>,
  val syndicateInvitedGuests: Set<String>,
  val onAddSyndicateChannel: (String) -> Unit,
  val onDeleteSyndicateChannel: (String) -> Unit,
  val onInviteSyndicateGuest: (String) -> Unit,
  val onRevokeSyndicateGuest: (String) -> Unit,
  val lobbyMessages: List<SyndicateChatMessage>,
  val onSendChannelMessage: (channelKey: String, messageText: String) -> Unit,
  val onSubmitBugReport: (category: String, description: String) -> Unit,
  val onShareActiveRiftFrameBuild: () -> Unit,
  val onLoadSharedRiftFrameBuild: (String) -> Unit,
  val onPostStrikeWingInvite: () -> Unit,
  val onPostPrivateRoomInvite: (roomCode: String, modeTitle: String) -> Unit,
  val onJoinSharedStrikeWing: (SyndicateChatMessage) -> Unit,
  val onAddOnlinePilotFriend: (String) -> Unit,
  val onInviteOnlinePilotToWing: (LivePilotState) -> Unit,
  val engine: RiftGameEngine?,
  val isInMatch: Boolean,
  val onEnterPracticeRift: () -> Unit,
  val onOpenHowToPlayTutorial: () -> Unit,
  val initialLayoutMode: NexusHubLayoutMode,
  val activeModuleKey: String,
  val activeNode: NexusCoreNode,
  val onSelectNode: (NexusCoreNode) -> Unit,
  val syndicateSubDeck: SyndicateCitadelSubDeck,
  val onSelectSyndicateSubDeck: (SyndicateCitadelSubDeck) -> Unit,
  val archiveSubDeck: ArchiveCodexSubDeck,
  val onSelectArchiveSubDeck: (ArchiveCodexSubDeck) -> Unit,
  val onSelectNexusLobby: () -> Unit,
  val onOpenProgression: () -> Unit,
  val isCalibrationLivePeeking: Boolean,
  val hideTopHeaderForFullScreenStage: Boolean,
  val liveResonance: Int,
  val isGoogleAuthenticated: Boolean,
  val overwatchInstances: List<OverwatchRiftInstance>,
  val selectedOverwatchKey: String,
  val favoriteOverwatchKey: String,
  val onSelectOverwatchKey: (String) -> Unit,
  val onToggleFavoriteOverwatchKey: (String) -> Unit,
  val onQuickZoneIntoOverwatch: (OverwatchRiftInstance) -> Unit,
  val onUpdateOverwatchViewport: (cx: Float, cy: Float, radiusPx: Float) -> Unit,
  val overwatchZoom: Float,
  val onOverwatchTransformGesture:
    (centroidRootX: Float, centroidRootY: Float, panDxPx: Float, panDyPx: Float, zoomChange: Float) -> Unit,
  val onResetOverwatchCamera: () -> Unit,
  val onStageRankedModeViaHangar: ((RankedGameMode, Int) -> Unit)?,
  val onStageCustomGameViaHangar: ((CustomGameConfig) -> Unit)?,
  val onStageExistingCustomInstanceViaHangar: ((String) -> Unit)?,
  val publicCustomGames: List<CustomGameConfig>,
  val onLookupCustomRoomByCode: ((String, (CustomGameConfig?, String) -> Unit) -> Unit)?,
  val isNetworkOnline: Boolean,
  val cloudSyncHealth: CloudSyncHealth,
  val onSwitchAccount: () -> Unit,
  val requestedSideBayKey: String,
  val requestedSideBayNonce: Int,
  val syndicateDirectory: List<SyndicateInfo>,
  val syndicateStatusMessage: String?,
  val onJoinOfficialSyndicate: (SyndicateInfo) -> Unit,
  val onLeaveCurrentSyndicate: () -> Unit,
  val onCharterCustomSyndicate: (tag: String, name: String, motto: String) -> Unit,
  val onClaimDailyDirectiveSlot: (Int) -> Long,
  val onRerollDailyDirectiveSlot: (Int) -> Boolean,
  val onClaimDailySweepBonus: () -> Long,
  val onUnlockOrEquipCosmeticItem: (String, CosmeticCatalogItem) -> Pair<Boolean, String>,
  val onUnlockOrSelectFrameSlotId: (String) -> Pair<Boolean, String>,
  val activeCustomSlotId: String,
  val activeCustomSlot: CustomShipLoadoutSlot,
  val onSaveCustomSlot: (CustomShipLoadoutSlot) -> Unit,
  val cloudLeaderboardEntries: List<LeaderboardEntry>,
  val onRenamePilotCallsign: (String) -> Pair<Boolean, String>,
  val onResetLocalPilotTelemetry: () -> Unit,
  val helmInitialSubDeck: Int,
  val sfxEnabled: Boolean,
  val musicEnabled: Boolean,
  val musicBpm: Int,
  val musicVolume: Float,
  val sfxVolume: Float,
  val cameraZoom: Float,
  val playfieldTextScale: Float,
  val onToggleSfx: () -> Unit,
  val onToggleMusic: () -> Unit,
  val onChangeMusicBpm: (Int) -> Unit,
  val onChangeMusicVolume: (Float) -> Unit,
  val onChangeSfxVolume: (Float) -> Unit,
  val onChangeCameraZoom: (Float) -> Unit,
  val onChangePlayfieldTextScale: (Float) -> Unit,
  val onOpenChangelogModal: () -> Unit,
  val onOpenAudioSynthForge: () -> Unit,
  val updateStatusLabel: String,
  val hasUpdateAvailable: Boolean,
  val onCheckOrApplyUpdate: () -> Unit,
  val renderModuleForNode: (@Composable (NexusCoreNode) -> Unit)?,
  val embeddedModuleContent: (@Composable () -> Unit)?,
) {
  val isStreaming: Boolean
    get() = streamState is NexusStreamState.Loading || streamState is NexusStreamState.Streaming

  val cleanSyndicateTag: String
    get() =
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

  val resolvedLiveResonance: Int
    get() =
      when {
        liveResonance >= 0 -> liveResonance
        engine != null -> engine.liveResonance
        else -> ((pilotLevel - 1).coerceAtLeast(0) * 18)
      }

  val isCalibrationPeekActive: Boolean
    get() =
      (activeNode == NexusCoreNode.STAR_FORGE || activeModuleKey == "FORGE" || activeModuleKey == "CALIBRATE") &&
        isCalibrationLivePeeking

  val isFullScreenForgeStage: Boolean
    get() =
      hideTopHeaderForFullScreenStage &&
        (activeNode == NexusCoreNode.STAR_FORGE || activeModuleKey == "FORGE")
}

private class NexusConsoleUiState(
  val context: Context,
  val localPrefs: LocalPilotPrefs,
  val chargedPrefs: android.content.SharedPreferences,
  initialChargedPrefsKey: String,
  initialTeamSizeBracket: Int,
  initialCustomGame: CustomGameConfig?,
  initialSideBayKey: String,
) {
  var selectedChannelKey by mutableStateOf("#nexus-ai")
  var showOnlineRosterPane by mutableStateOf(true)
  var inputQuery by mutableStateOf("")
  var channelChatInput by mutableStateOf("")

  var gateTab by mutableStateOf(ConvergenceGateTab.OFFICIAL_RANKED)
  var skirmishBracket by mutableIntStateOf(initialTeamSizeBracket.coerceIn(2, 5))
  var riftControlBracket by
    mutableIntStateOf(if (initialTeamSizeBracket >= 10) 10 else if (initialTeamSizeBracket >= 5) 5 else 3)
  var syndicateWarBracket by
    mutableIntStateOf(if (initialTeamSizeBracket >= 10) 10 else if (initialTeamSizeBracket >= 5) 5 else 3)

  var customIsPrivateRoom by mutableStateOf(false)
  var generatedRoomCode by mutableStateOf(generateSixCharRoomCode())
  var joinRoomCodeDraft by mutableStateOf("")
  var roomCodeBanner by mutableStateOf<String?>(null)
  val unlockedPrivateCodes = mutableStateListOf<String>()

  var customBaseMode by mutableStateOf(RankedGameMode.SECTOR_CONVERGENCE)
  var customResetMinutes by mutableIntStateOf(10)
  var customTeamBracket by mutableIntStateOf(5)
  var customAllowArmadasInFfa by mutableStateOf(true)
  var customBotsEnabled by mutableStateOf(true)
  var customBotCount by mutableIntStateOf(initialCustomGame?.botCount ?: 12)
  var customBotSkillLevel by mutableIntStateOf(initialCustomGame?.botSkillLevel ?: 1)
  var customScoreCap by mutableIntStateOf(initialCustomGame?.scoreCap ?: 50)
  var customBulletSizeMult by mutableFloatStateOf(1.0f)
  var customSpeedMult by mutableFloatStateOf(1.0f)
  var customRechargeMult by mutableFloatStateOf(1.0f)
  var customDamageMult by mutableFloatStateOf(1.0f)
  var customCrateSpawnMult by mutableFloatStateOf(1.0f)
  var customDamageTakenScalePct by
    mutableIntStateOf(snapCustomFivePercentScale(initialCustomGame?.damageTakenScalePct ?: 100))
  var customWeaponFluxCostScalePct by
    mutableIntStateOf(snapCustomFivePercentScale(initialCustomGame?.weaponFluxCostScalePct ?: 100))
  var customGamesRefreshTick by mutableIntStateOf(0)
  var showHubSurvivalSpreadsheetModal by mutableStateOf(false)

  var showChannelManager by mutableStateOf(false)
  var newChannelDraft by mutableStateOf("")
  var showGuestManager by mutableStateOf(false)
  var guestCallsignDraft by mutableStateOf("")

  var bugCategory by mutableStateOf("PHYSICS / COLLISION")
  var bugDescription by mutableStateOf("")
  var bugStatusBanner by mutableStateOf<String?>(null)

  var focusedOrbitalIndex by mutableIntStateOf(0)
  var sideBayMode by mutableStateOf(parseSideBayModeKey(initialSideBayKey))
  var overwatchPrompt by mutableStateOf<OverwatchCircularPrompt>(OverwatchCircularPrompt.None)
  var squadronFilter by mutableStateOf(SquadronPilotFilter.ALL)
  var squadronSort by mutableStateOf(SquadronPilotSort.RESONANCE)
  var selectedSquadronPilotUid by mutableStateOf("")
  var squadronStatusBanner by mutableStateOf<String?>(null)

  var chargedShipId by
    mutableIntStateOf(chargedPrefs.getInt(initialChargedPrefsKey, 0).let { if (it in 1..10) it else 0 })

  var showWarpOffConfirmModal by mutableStateOf(false)
  var showSwitchAccountConfirmModal by mutableStateOf(false)
  val localEchoMessages = mutableStateMapOf<String, MutableList<SyndicateChatMessage>>()

  var glViewRef by mutableStateOf<NexusSpaceGlView?>(null)
  var hubApertureRootBounds by mutableStateOf(Triple(0f, 0f, 0f))
  var modalRootOrigin by mutableStateOf(Offset.Zero)
  var lastCardNodeForSlide by mutableStateOf(NexusCoreNode.HANGAR_DECK)

  fun updateCustomDamageTakenScale(rawPct: Int, engine: RiftGameEngine?) {
    val snapped = snapCustomFivePercentScale(rawPct)
    customDamageTakenScalePct = snapped
    engine?.currentCustomGame?.let { activeCfg ->
      activeCfg.damageTakenScalePct = snapped
    }
  }

  fun updateCustomWeaponFluxCostScale(rawPct: Int, engine: RiftGameEngine?) {
    val snapped = snapCustomFivePercentScale(rawPct)
    customWeaponFluxCostScalePct = snapped
    engine?.currentCustomGame?.let { activeCfg ->
      activeCfg.weaponFluxCostScalePct = snapped
    }
  }

  fun chargeShipAcrossAllModes(
    targetShipId: Int,
    chargedPrefsKey: String,
    params: NexusAiConsoleParams,
  ) {
    val clamped = targetShipId.coerceIn(1, 10)
    chargedShipId = clamped
    chargedPrefs.edit().putInt(chargedPrefsKey, clamped).apply()
    localPrefs.setPreferredShipId(clamped)
    params.onOpenHangarForShip(clamped)
    params.engine?.let { eng ->
      if (eng.hasJoinedMatch && !eng.isHubOverwatchActive) {
        eng.isShipLockedForMatch = false
        eng.switchLocalPlayerShip(clamped, forceFromHangar = true)
      }
    }
  }

  fun submitNexusAiQuery(
    params: NexusAiConsoleParams,
    keyboardController: androidx.compose.ui.platform.SoftwareKeyboardController?,
  ) {
    val trimmed = inputQuery.trim()
    if (trimmed.isNotEmpty() && !params.isStreaming) {
      inputQuery = ""
      keyboardController?.hide()
      params.onSendQuestion(trimmed)
    }
  }

  fun postToCloudChannel(channelKey: String, rawText: String, params: NexusAiConsoleParams) {
    val clean = rawText.trim()
    if (clean.isEmpty()) return
    val normalizedKey = channelKey.trim().lowercase(Locale.US)
    val echo =
      SyndicateChatMessage(
        id = "local_${System.currentTimeMillis()}",
        senderUid = params.currentUserId,
        senderCallsign = params.callsign,
        senderSyndicateTag = params.cleanSyndicateTag,
        timestampLabel = channelKey.uppercase(Locale.US).take(24),
        messageText = clean,
        createdAtMs = System.currentTimeMillis(),
      )
    localEchoMessages.getOrPut(normalizedKey) { mutableStateListOf() }.add(echo)
    params.onSendChannelMessage(channelKey, clean)
  }

  fun resolveGateDeployShipId(params: NexusAiConsoleParams): Int {
    val uidOrNull = params.currentUserId.ifBlank { null }
    val hasAnyRes =
      params.resolvedLiveResonance > 0 ||
        localPrefs.getAllShipResonances(uidOrNull).values.any { it > 0 }
    val resolvedBestShipForGate = if (hasAnyRes && params.bestShipId in 1..10) params.bestShipId else 0
    val clampedActiveShipForGate = params.activeShipId.coerceIn(1, 10)
    val isGateShipSelected =
      chargedShipId in 1..10 ||
        params.activeShipSummary.startsWith("CHARGED|", ignoreCase = true) ||
        params.activeShipSummary.startsWith("LOCKED|", ignoreCase = true) ||
        params.engine?.isShipLockedForMatch == true ||
        resolvedBestShipForGate == 0 ||
        clampedActiveShipForGate != resolvedBestShipForGate
    return when {
      chargedShipId in 1..10 -> chargedShipId
      isGateShipSelected -> clampedActiveShipForGate
      else -> resolvedBestShipForGate.coerceIn(1, 10)
    }
  }

  fun resolveGateShipStatusSummary(params: NexusAiConsoleParams, gateDeployShipId: Int): String {
    val uidOrNull = params.currentUserId.ifBlank { null }
    val hasAnyRes =
      params.resolvedLiveResonance > 0 ||
        localPrefs.getAllShipResonances(uidOrNull).values.any { it > 0 }
    val resolvedBestShipForGate = if (hasAnyRes && params.bestShipId in 1..10) params.bestShipId else 0
    val clampedActiveShipForGate = params.activeShipId.coerceIn(1, 10)
    val isGateShipSelected =
      chargedShipId in 1..10 ||
        params.activeShipSummary.startsWith("CHARGED|", ignoreCase = true) ||
        params.activeShipSummary.startsWith("LOCKED|", ignoreCase = true) ||
        params.engine?.isShipLockedForMatch == true ||
        resolvedBestShipForGate == 0 ||
        clampedActiveShipForGate != resolvedBestShipForGate
    val gateDeployShipName =
      if (gateDeployShipId == 10) {
        "RIFT FRAME"
      } else {
        RiftCodex.getShipById(gateDeployShipId)
          .name
          .replace(Regex("^[0-9]+-"), "")
          .trim()
          .uppercase(Locale.US)
      }
    val tag =
      when {
        chargedShipId in 1..10 || isGateShipSelected || resolvedBestShipForGate == 0 -> "CHARGED"
        else -> "BEST"
      }
    return "$gateDeployShipName ($tag)"
  }

  fun deployOrStageRankedSelection(mode: RankedGameMode, bracket: Int, params: NexusAiConsoleParams) {
    if (chargedShipId in 1..10) {
      params.onOpenHangarForShip(chargedShipId)
    }
    if (params.onStageRankedModeViaHangar != null) {
      params.onStageRankedModeViaHangar.invoke(mode, bracket)
      return
    }
    params.onDeployToRankedMode(mode, bracket)
  }

  fun deployOrStageExistingCustom(
    instanceId: String,
    gateDeployShipId: Int,
    params: NexusAiConsoleParams,
  ) {
    if (chargedShipId in 1..10) {
      params.onOpenHangarForShip(chargedShipId)
    }
    if (params.onStageExistingCustomInstanceViaHangar != null) {
      params.onStageExistingCustomInstanceViaHangar.invoke(instanceId)
      return
    }
    params.engine?.let { eng ->
      eng.isShipLockedForMatch = false
      eng.switchLocalPlayerShip(gateDeployShipId, forceFromHangar = true)
      eng.joinCustomGame(instanceId)
      RiftCalibrationState.applyCalibrationChangesLive(eng, context)
    }
    customGamesRefreshTick++
    params.onCloseModal()
  }

  fun deployOrStageNewCustomConfig(
    cfg: CustomGameConfig,
    gateDeployShipId: Int,
    params: NexusAiConsoleParams,
  ) {
    if (chargedShipId in 1..10) {
      params.onOpenHangarForShip(chargedShipId)
    }
    if (params.onStageCustomGameViaHangar != null) {
      params.onStageCustomGameViaHangar.invoke(cfg)
      return
    }
    params.engine?.let { eng ->
      eng.isShipLockedForMatch = false
      eng.switchLocalPlayerShip(gateDeployShipId, forceFromHangar = true)
      eng.createAndJoinCustomGame(cfg)
      RiftCalibrationState.botsEnabled = cfg.botsEnabled
      RiftCalibrationState.applyCalibrationChangesLive(eng, context)
    }
    customGamesRefreshTick++
    params.onCloseModal()
  }

  fun launchCustomGameFromDraft(gateDeployShipId: Int, params: NexusAiConsoleParams) {
    val prefix = if (customIsPrivateRoom) "priv" else "pub"
    val code = generatedRoomCode
    if (code !in unlockedPrivateCodes) {
      unlockedPrivateCodes.add(code)
    }
    val modeShort = customBaseMode.title.uppercase(Locale.US)
    val cfg =
      CustomGameConfig(
        instanceId = "${prefix}_${code}",
        roomCode = code,
        privateRoom = customIsPrivateRoom,
        title = "${params.callsign.uppercase(Locale.US)} $modeShort [$code]",
        hostCallsign = params.callsign,
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
        damageTakenScalePct = customDamageTakenScalePct,
        weaponFluxCostScalePct = customWeaponFluxCostScalePct,
        botCount = if (customBotsEnabled) customBotCount else 0,
        botSkillLevel = customBotSkillLevel,
        scoreCap = customScoreCap,
      )
    generatedRoomCode = generateSixCharRoomCode(System.currentTimeMillis() + 7919L)
    deployOrStageNewCustomConfig(cfg, gateDeployShipId, params)
  }

  fun joinCustomByRoomCode(
    rawCode: String,
    gateDeployShipId: Int,
    params: NexusAiConsoleParams,
  ) {
    val cleanCode = rawCode.trim().uppercase(Locale.US).replace(Regex("[^A-Z0-9]"), "").take(8)
    if (cleanCode.length < 4) {
      roomCodeBanner = "ENTER A VALID 6-CHARACTER JOIN CODE."
      return
    }
    if (cleanCode !in unlockedPrivateCodes) unlockedPrivateCodes.add(cleanCode)
    val combinedKnown =
      buildList {
        addAll(params.publicCustomGames)
        addAll(params.engine?.activeCustomGames.orEmpty())
      }
    val matching =
      combinedKnown.firstOrNull { candidate ->
        extractCustomRoomCode(candidate).equals(cleanCode, ignoreCase = true) ||
          candidate.instanceId.equals("priv_$cleanCode", ignoreCase = true) ||
          candidate.instanceId.equals("pub_$cleanCode", ignoreCase = true)
      }
    if (matching != null) {
      params.engine?.let { eng ->
        if (eng.activeCustomGames.none { it.instanceId.equals(matching.instanceId, ignoreCase = true) }) {
          eng.activeCustomGames.add(0, matching)
        }
      }
      roomCodeBanner = "JOINING ROOM [$cleanCode] // ${matching.title}"
      customGamesRefreshTick++
      deployOrStageExistingCustom(matching.instanceId, gateDeployShipId, params)
      return
    }
    if (params.onLookupCustomRoomByCode != null) {
      roomCodeBanner = "SEARCHING SECTOR FOR JOIN CODE [$cleanCode]..."
      params.onLookupCustomRoomByCode.invoke(cleanCode) { cloudMatch, statusMsg ->
        if (cloudMatch != null) {
          val resolvedCode = extractCustomRoomCode(cloudMatch)
          if (resolvedCode !in unlockedPrivateCodes) unlockedPrivateCodes.add(resolvedCode)
          params.engine?.let { eng ->
            val existingIdx =
              eng.activeCustomGames.indexOfFirst {
                it.instanceId.equals(cloudMatch.instanceId, ignoreCase = true)
              }
            if (existingIdx >= 0) {
              eng.activeCustomGames[existingIdx] = cloudMatch
            } else {
              eng.activeCustomGames.add(0, cloudMatch)
            }
          }
          roomCodeBanner = "JOINING ROOM [$resolvedCode] // ${cloudMatch.title}"
          customGamesRefreshTick++
          deployOrStageExistingCustom(cloudMatch.instanceId, gateDeployShipId, params)
        } else {
          roomCodeBanner = statusMsg.ifBlank { "NO ACTIVE CUSTOM GAME FOUND FOR CODE [$cleanCode]." }
        }
      }
    } else {
      val created =
        CustomGameConfig(
          instanceId = "priv_$cleanCode",
          roomCode = cleanCode,
          privateRoom = true,
          title = "PRIVATE RIFT [$cleanCode]",
          hostCallsign = params.callsign,
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
          damageTakenScalePct = customDamageTakenScalePct,
          weaponFluxCostScalePct = customWeaponFluxCostScalePct,
          botCount = if (customBotsEnabled) customBotCount else 0,
          botSkillLevel = customBotSkillLevel,
          scoreCap = customScoreCap,
        )
      roomCodeBanner = "ENTERING PRIVATE RIFT [$cleanCode]"
      deployOrStageNewCustomConfig(created, gateDeployShipId, params)
    }
  }
}

@Composable
private fun NexusConsoleSideEffects(
  params: NexusAiConsoleParams,
  uiState: NexusConsoleUiState,
  listState: LazyListState,
  liveTrafficShipIds: IntArray,
) {
  LaunchedEffect(params.engine?.currentCustomGame?.instanceId) {
    params.engine?.currentCustomGame?.let { activeCfg ->
      uiState.customDamageTakenScalePct = snapCustomFivePercentScale(activeCfg.damageTakenScalePct)
      uiState.customWeaponFluxCostScalePct = snapCustomFivePercentScale(activeCfg.weaponFluxCostScalePct)
      uiState.customBotCount = activeCfg.botCount.coerceIn(0, 19)
      uiState.customBotSkillLevel = activeCfg.botSkillLevel.coerceIn(0, 3)
      uiState.customScoreCap = activeCfg.scoreCap.coerceIn(5, 500)
    }
  }

  LaunchedEffect(params.requestedSideBayNonce, params.requestedSideBayKey) {
    if (params.requestedSideBayNonce > 0) {
      val parsedMode = parseSideBayModeKey(params.requestedSideBayKey)
      uiState.sideBayMode = parsedMode
      uiState.overwatchPrompt =
        if (parsedMode == RiftHubSideBayMode.CONDUIT_BAY) {
          OverwatchCircularPrompt.ResonanceLeaderboard
        } else {
          OverwatchCircularPrompt.None
        }
    }
  }

  // Keep any persisted Charged ship locked across all game modes on mount / user switch (never auto-launching)
  LaunchedEffect(uiState.chargedShipId, params.currentUserId) {
    if (uiState.chargedShipId in 1..10 && params.activeShipId.coerceIn(1, 10) != uiState.chargedShipId) {
      uiState.localPrefs.setPreferredShipId(uiState.chargedShipId)
      params.onOpenHangarForShip(uiState.chargedShipId)
    }
  }

  // Reset Overwatch circular prompt when leaving ORBITAL_HUB
  LaunchedEffect(params.activeNode) {
    if (params.activeNode != NexusCoreNode.ORBITAL_HUB) {
      uiState.overwatchPrompt = OverwatchCircularPrompt.None
    }
  }

  // Hardware/System Back cleanly dismisses HTML spreadsheet first, circular Overwatch prompt second, then dissolves side-bay back to Rift Hub
  BackHandler(
    enabled =
      uiState.showHubSurvivalSpreadsheetModal ||
        (params.activeNode == NexusCoreNode.ORBITAL_HUB &&
          (uiState.overwatchPrompt != OverwatchCircularPrompt.None ||
            uiState.sideBayMode != RiftHubSideBayMode.HUB_MAIN))
  ) {
    when {
      uiState.showHubSurvivalSpreadsheetModal -> uiState.showHubSurvivalSpreadsheetModal = false
      uiState.overwatchPrompt != OverwatchCircularPrompt.None ->
        uiState.overwatchPrompt = OverwatchCircularPrompt.None
      else -> uiState.sideBayMode = RiftHubSideBayMode.HUB_MAIN
    }
  }

  val lastMsgTextLen = params.messages.lastOrNull()?.text?.length ?: 0
  LaunchedEffect(params.messages.size, lastMsgTextLen, params.streamState, uiState.selectedChannelKey) {
    if (uiState.selectedChannelKey == "#nexus-ai") {
      val totalItems = listState.layoutInfo.totalItemsCount
      if (totalItems > 0) listState.animateScrollToItem(totalItems - 1)
    }
  }

  LaunchedEffect(params.activeNode, uiState.focusedOrbitalIndex, uiState.glViewRef, liveTrafficShipIds) {
    val view = uiState.glViewRef ?: return@LaunchedEffect
    view.updateOnlinePilotShips(liveTrafficShipIds)
    view.dimFactor = 0.44f
    view.focusedTearIndex = -1
  }
}

private fun handleNexusOrbitalKeyEvent(
  native: KeyEvent,
  params: NexusAiConsoleParams,
  uiState: NexusConsoleUiState,
  activeOverwatchInstance: OverwatchRiftInstance,
): Boolean {
  if (params.activeNode != NexusCoreNode.ORBITAL_HUB) return false
  if (native.action != KeyEvent.ACTION_DOWN || native.repeatCount != 0) return false
  val totalTargets = 5
  return when (native.keyCode) {
    KeyEvent.KEYCODE_DPAD_RIGHT,
    KeyEvent.KEYCODE_DPAD_DOWN -> {
      uiState.focusedOrbitalIndex = (uiState.focusedOrbitalIndex + 1) % totalTargets
      true
    }
    KeyEvent.KEYCODE_DPAD_LEFT,
    KeyEvent.KEYCODE_DPAD_UP -> {
      uiState.focusedOrbitalIndex =
        if (uiState.focusedOrbitalIndex <= 0) totalTargets - 1 else uiState.focusedOrbitalIndex - 1
      true
    }
    KeyEvent.KEYCODE_BUTTON_A,
    KeyEvent.KEYCODE_DPAD_CENTER,
    KeyEvent.KEYCODE_ENTER -> {
      when (uiState.focusedOrbitalIndex.coerceIn(0, 4)) {
        0 -> params.onQuickZoneIntoOverwatch(activeOverwatchInstance)
        1 -> {
          uiState.overwatchPrompt = OverwatchCircularPrompt.None
          uiState.sideBayMode = RiftHubSideBayMode.HANGAR_BAY
        }
        2 -> {
          uiState.sideBayMode = RiftHubSideBayMode.CONDUIT_BAY
          uiState.overwatchPrompt = OverwatchCircularPrompt.ResonanceLeaderboard
        }
        3 -> params.onSelectNode(NexusCoreNode.SUBSPACE_COMMS)
        else -> {
          uiState.overwatchPrompt = OverwatchCircularPrompt.None
          uiState.sideBayMode = RiftHubSideBayMode.GAME_MODES_BAY
        }
      }
      true
    }
    else -> false
  }
}

@Composable
private fun NexusAiConsoleRootContent(params: NexusAiConsoleParams) {
  val context = LocalContext.current
  val keyboardController = LocalSoftwareKeyboardController.current
  val listState = rememberLazyListState()
  val localPrefs = remember(context) { LocalPilotPrefs(context) }
  val chargedPrefs =
    remember(context) {
      context.getSharedPreferences("rift_hub_charged_ship_prefs", Context.MODE_PRIVATE)
    }
  val chargedPrefsKey =
    remember(params.currentUserId) {
      "charged_ship_${params.currentUserId.trim().ifBlank { "local" }}"
    }
  val uiState =
    remember(chargedPrefsKey) {
      NexusConsoleUiState(
        context = context,
        localPrefs = localPrefs,
        chargedPrefs = chargedPrefs,
        initialChargedPrefsKey = chargedPrefsKey,
        initialTeamSizeBracket = params.activeTeamSizeBracket,
        initialCustomGame = params.engine?.currentCustomGame,
        initialSideBayKey = params.requestedSideBayKey,
      )
    }

  val liveTrafficShipIds =
    remember(params.activeShipId, params.onlineHumanPilots, params.currentUserId) {
      val ids = ArrayList<Int>(params.onlineHumanPilots.size + 1)
      ids.add(params.activeShipId.coerceIn(1, 10))
      for (p in params.onlineHumanPilots) {
        val pUid = p.userId.ifBlank { p.id }
        if (pUid != params.currentUserId) {
          ids.add(p.shipId.toInt().coerceIn(1, 10))
        }
      }
      ids.toIntArray()
    }
  val isLocalArchitect =
    remember(params.currentUserId, params.callsign) {
      RiftCommunityRoles.isDeveloperArchitect(params.currentUserId, params.callsign)
    }

  val resolvedOverwatchInstances =
    remember(
      params.overwatchInstances,
      params.callsign,
      params.engine?.currentCustomGame,
      params.engine?.hasJoinedMatch,
    ) {
      val allowedModes = setOf(RankedGameMode.SECTOR_CONVERGENCE, RankedGameMode.FLUX_CORE)
      val filtered =
        params.overwatchInstances.filter { inst ->
          val cfg = inst.customConfig
          if (cfg == null) {
            inst.rankedMode in allowedModes
          } else {
            val isOwnCustom =
              cfg.hostCallsign.equals(params.callsign, ignoreCase = true) ||
                (params.engine?.hasJoinedMatch == true &&
                  params.engine.currentCustomGame?.instanceId?.equals(cfg.instanceId, ignoreCase = true) == true)
            isOwnCustom && cfg.baseMode in allowedModes
          }
        }
      filtered.ifEmpty {
        listOf(
          OverwatchRiftInstance(
            key = "SECTOR_CONVERGENCE",
            modeTitle = "SECTOR CONVERGENCE",
            riftLabel = "",
            rankedMode = RankedGameMode.SECTOR_CONVERGENCE,
            customConfig = null,
            pilotCount = 0,
            combinedResonance = 1040,
            seedSalt = 101,
            avgResonance = 175,
            remainingSeconds = 3600,
          ),
          OverwatchRiftInstance(
            key = "FLUX_CORE",
            modeTitle = "FLUX CORE",
            riftLabel = "",
            rankedMode = RankedGameMode.FLUX_CORE,
            customConfig = null,
            pilotCount = 0,
            combinedResonance = 920,
            seedSalt = 202,
            avgResonance = 154,
            remainingSeconds = 3600,
          ),
        )
      }
    }
  val activeOverwatchInstance =
    remember(resolvedOverwatchInstances, params.selectedOverwatchKey, params.favoriteOverwatchKey) {
      resolvedOverwatchInstances.firstOrNull { it.key == params.selectedOverwatchKey }
        ?: params.favoriteOverwatchKey
          .takeIf { it.isNotBlank() }
          ?.let { fav -> resolvedOverwatchInstances.firstOrNull { it.key == fav } }
        ?: resolvedOverwatchInstances.maxByOrNull { it.combinedResonance }
        ?: resolvedOverwatchInstances.first()
    }

  NexusConsoleSideEffects(
    params = params,
    uiState = uiState,
    listState = listState,
    liveTrafficShipIds = liveTrafficShipIds,
  )

  if (params.activeNode != NexusCoreNode.ORBITAL_HUB) {
    uiState.lastCardNodeForSlide = params.activeNode
  }
  val enterDirectionSign = if (isLeftSideHubCard(uiState.lastCardNodeForSlide)) -1f else 1f
  val terminalScale by
    animateFloatAsState(
      targetValue = if (params.activeNode == NexusCoreNode.ORBITAL_HUB) 0.95f else 1.0f,
      animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
      label = "terminal_emerge_scale",
    )
  val terminalAlpha by
    animateFloatAsState(
      targetValue = if (params.activeNode == NexusCoreNode.ORBITAL_HUB) 0.0f else 1.0f,
      animationSpec = tween(durationMillis = 210, easing = FastOutSlowInEasing),
      label = "terminal_emerge_alpha",
    )
  val terminalSlideFraction by
    animateFloatAsState(
      targetValue = if (params.activeNode == NexusCoreNode.ORBITAL_HUB) 0.08f * enterDirectionSign else 0.0f,
      animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
      label = "terminal_slide_fraction",
    )
  val hubStageAlpha by
    animateFloatAsState(
      targetValue = if (params.activeNode == NexusCoreNode.ORBITAL_HUB) 1.0f else 0.0f,
      animationSpec = tween(durationMillis = 210, easing = FastOutSlowInEasing),
      label = "hub_stage_alpha",
    )

  val gateDeployShipId = uiState.resolveGateDeployShipId(params)
  val gateShipStatusSummary = uiState.resolveGateShipStatusSummary(params, gateDeployShipId)

  Box(
    modifier =
      Modifier.fillMaxSize()
        .onGloballyPositioned { coords ->
          uiState.modalRootOrigin = coords.positionInRoot()
        }
        .drawBehind {
          when {
            params.isCalibrationPeekActive -> drawRect(Color(0x1802050B))
            params.activeNode == NexusCoreNode.ORBITAL_HUB -> {
              drawNexusOrbitalChassisBackdrop(
                hubApertureRootBounds = uiState.hubApertureRootBounds,
                modalRootOrigin = uiState.modalRootOrigin,
              )
            }
            else -> drawRect(Color(0xFF02050B))
          }
        }
        .onPreviewKeyEvent { keyEvent ->
          handleNexusOrbitalKeyEvent(
            native = keyEvent.nativeKeyEvent,
            params = params,
            uiState = uiState,
            activeOverwatchInstance = activeOverwatchInstance,
          )
        }
        .testTag("nexus_ai_console_modal"),
    contentAlignment = Alignment.Center,
  ) {
    if (
      params.activeNode != NexusCoreNode.ORBITAL_HUB &&
        !params.isCalibrationPeekActive &&
        !params.isFullScreenForgeStage &&
        !isHeadlessGlEnvironment()
    ) {
      AndroidView(
        factory = { ctx ->
          NexusSpaceGlView(ctx).apply {
            isSplashMode = false
            dimFactor = 0.44f
            focusedTearIndex = -1
            updateOnlinePilotShips(liveTrafficShipIds)
            updateStationHologramTelemetry(params.totalOnlinePilots, params.unreadCommsCount)
            uiState.glViewRef = this
          }
        },
        update = { view ->
          uiState.glViewRef = view
          view.isSplashMode = false
          view.dimFactor = 0.44f
          view.updateOnlinePilotShips(liveTrafficShipIds)
          view.updateStationHologramTelemetry(params.totalOnlinePilots, params.unreadCommsCount)
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
        params.isFullScreenForgeStage &&
          (params.renderModuleForNode != null || params.embeddedModuleContent != null) -> {
          Box(modifier = Modifier.fillMaxSize().testTag("nexus_embedded_module_container")) {
            if (params.renderModuleForNode != null) {
              params.renderModuleForNode.invoke(params.activeNode)
            } else {
              params.embeddedModuleContent?.invoke()
            }
          }
        }
        params.activeNode == NexusCoreNode.ORBITAL_HUB -> {
          NexusOrbitalHubStageHost(
            params = params,
            uiState = uiState,
            chargedPrefsKey = chargedPrefsKey,
            gateDeployShipId = gateDeployShipId,
            resolvedOverwatchInstances = resolvedOverwatchInstances,
            activeOverwatchInstance = activeOverwatchInstance,
            hubStageAlpha = hubStageAlpha,
          )
        }
        else -> {
          NexusGlassTerminalDeckHost(
            params = params,
            uiState = uiState,
            chargedPrefsKey = chargedPrefsKey,
            gateDeployShipId = gateDeployShipId,
            gateShipStatusSummary = gateShipStatusSummary,
            isLocalArchitect = isLocalArchitect,
            listState = listState,
            keyboardController = keyboardController,
            terminalScale = terminalScale,
            terminalAlpha = terminalAlpha,
            terminalSlideFraction = terminalSlideFraction,
          )
        }
      }

      if (uiState.showWarpOffConfirmModal) {
        NexusWarpOffConfirmationModal(
          onStayInRift = { uiState.showWarpOffConfirmModal = false },
          onConfirmWarpOff = {
            uiState.showWarpOffConfirmModal = false
            params.onLeaveMatch()
          },
        )
      }
      if (uiState.showSwitchAccountConfirmModal) {
        NexusSwitchAccountConfirmationModal(
          onCancel = { uiState.showSwitchAccountConfirmModal = false },
          onConfirmSwitchAccount = {
            uiState.showSwitchAccountConfirmModal = false
            params.onSwitchAccount()
          },
        )
      }
      if (uiState.showHubSurvivalSpreadsheetModal) {
        RiftTelemetryWebSheetModal(
          callsign = params.callsign,
          liveResonance = params.resolvedLiveResonance,
          uidOrNull = params.currentUserId.ifBlank { null },
          localPrefs = localPrefs,
          onClose = { uiState.showHubSurvivalSpreadsheetModal = false },
        )
      }
    }
  }
}

private fun resolveHubTapControlTag(
  xNorm: Float,
  yNorm: Float,
  sideBayMode: RiftHubSideBayMode,
  overwatchPrompt: OverwatchCircularPrompt,
): String {
  return when {
    xNorm < 0.30f ->
      when {
        yNorm < 0.16f -> "TOP_LEFT_RETURN_OR_DOSSIER"
        yNorm > 0.84f -> "BOTTOM_LEFT_RESONANCE_SHARDS"
        yNorm < 0.50f -> "LEFT_SHELF_UPPER_${sideBayMode.name}"
        else -> "LEFT_SHELF_LOWER_${sideBayMode.name}"
      }
    xNorm > 0.70f ->
      when {
        yNorm < 0.16f -> "TOP_RIGHT_TUTORIAL_OR_HELM"
        yNorm > 0.84f -> "BOTTOM_RIGHT_SYNDICATE_SQUADRON"
        yNorm < 0.50f -> "RIGHT_SHELF_UPPER_${sideBayMode.name}"
        else -> "RIGHT_SHELF_LOWER_${sideBayMode.name}"
      }
    else ->
      when {
        yNorm < 0.18f && xNorm < 0.50f -> "OVERWATCH_SECTOR_SWITCH"
        yNorm < 0.18f && xNorm >= 0.50f -> "TOP_RIGHT_RIFT_IN_TRIGGER"
        yNorm > 0.82f -> "OVERWATCH_BOTTOM_ACTION_BAR"
        overwatchPrompt !is OverwatchCircularPrompt.None ->
          "OVERWATCH_PROMPT_${overwatchPrompt::class.simpleName ?: "ACTIVE"}"
        else -> "OVERWATCH_WORKBENCH_CENTER"
      }
  }
}

@Composable
private fun NexusOrbitalHubStageHost(
  params: NexusAiConsoleParams,
  uiState: NexusConsoleUiState,
  chargedPrefsKey: String,
  gateDeployShipId: Int,
  resolvedOverwatchInstances: List<OverwatchRiftInstance>,
  activeOverwatchInstance: OverwatchRiftInstance,
  hubStageAlpha: Float,
) {
  Box(
    modifier =
      Modifier.fillMaxSize()
        .pointerInput(uiState.sideBayMode, uiState.overwatchPrompt) {
          awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            val w = size.width.toFloat().coerceAtLeast(1f)
            val h = size.height.toFloat().coerceAtLeast(1f)
            val xNorm = (down.position.x / w).coerceIn(0f, 1f)
            val yNorm = (down.position.y / h).coerceIn(0f, 1f)
            uiState.localPrefs.recordHubTapTelemetry(
              tapXNorm = xNorm,
              tapYNorm = yNorm,
              activeHubView = uiState.sideBayMode.name,
              targetControlTag =
                resolveHubTapControlTag(xNorm, yNorm, uiState.sideBayMode, uiState.overwatchPrompt),
            )
          }
        }
        .graphicsLayer {
          alpha = hubStageAlpha
        }
  ) {
    OrbitalHubSpatialStage(
      callsign = params.callsign,
      syndicateTag = params.cleanSyndicateTag,
      syndicateName = params.syndicateName,
      liveResonance = params.resolvedLiveResonance,
      riftShards = params.riftShards,
      activeShipId = gateDeployShipId,
      bestShipId = params.bestShipId,
      chargedShipId = uiState.chargedShipId,
      onChargeShip = { shipToCharge ->
        uiState.chargeShipAcrossAllModes(shipToCharge, chargedPrefsKey, params)
      },
      activeShipSummary = params.activeShipSummary,
      armadaCount = params.armadaSlots.size.coerceAtLeast(1),
      activeArmadaName = params.activeArmadaName,
      armadaSlots = params.armadaSlots,
      onlineHumanPilots = params.onlineHumanPilots,
      onResolvePilotBestShipId = params.onResolvePilotBestShipId,
      onInviteOnlinePilotToWing = { targetPilot ->
        params.onInviteOnlinePilotToWing(targetPilot)
        uiState.squadronStatusBanner =
          "SQUADRON INVITE SENT TO ${targetPilot.callsign.uppercase(Locale.US)}"
      },
      onAddOnlinePilotFriend = { targetCallsign ->
        params.onAddOnlinePilotFriend(targetCallsign)
        uiState.squadronStatusBanner =
          "FRIEND REQUEST DISPATCHED TO ${targetCallsign.uppercase(Locale.US)}"
      },
      onPostStrikeWingInvite = {
        params.onPostStrikeWingInvite()
        uiState.squadronStatusBanner = "BROADCASTED SQUADRON INVITE TO #SQUADRON-LFG"
      },
      squadronStatusBanner = uiState.squadronStatusBanner,
      sideBayMode = uiState.sideBayMode,
      onChangeSideBayMode = { nextBay ->
        uiState.overwatchPrompt = OverwatchCircularPrompt.None
        uiState.sideBayMode = nextBay
      },
      overwatchPrompt = uiState.overwatchPrompt,
      onChangeOverwatchPrompt = { nextPrompt -> uiState.overwatchPrompt = nextPrompt },
      squadronFilter = uiState.squadronFilter,
      onSelectSquadronFilter = { uiState.squadronFilter = it },
      squadronSort = uiState.squadronSort,
      onToggleSquadronSort = {
        uiState.squadronSort =
          if (uiState.squadronSort == SquadronPilotSort.RESONANCE) SquadronPilotSort.ACTIVITY
          else SquadronPilotSort.RESONANCE
      },
      selectedSquadronPilotUid = uiState.selectedSquadronPilotUid,
      onSelectSquadronPilotUid = { uiState.selectedSquadronPilotUid = it },
      activeRankedMode = params.activeRankedMode,
      skirmishBracket = uiState.skirmishBracket,
      onSelectSkirmishBracket = { uiState.skirmishBracket = it.coerceIn(2, 5) },
      riftControlBracket = uiState.riftControlBracket,
      onSelectRiftControlBracket = { uiState.riftControlBracket = it },
      syndicateWarBracket = uiState.syndicateWarBracket,
      onSelectSyndicateWarBracket = { uiState.syndicateWarBracket = it },
      humanPilotCountByMode = params.humanPilotCountByMode,
      canQueueSyndicateWars = params.canQueueSyndicateWars,
      onStageRanked = { mode, bracket ->
        uiState.deployOrStageRankedSelection(mode, bracket, params)
      },
      publicCustomGames = params.publicCustomGames,
      customIsPrivateRoom = uiState.customIsPrivateRoom,
      onTogglePrivateRoom = {
        uiState.customIsPrivateRoom = it
        uiState.generatedRoomCode = generateSixCharRoomCode()
      },
      generatedRoomCode = uiState.generatedRoomCode,
      joinRoomCodeDraft = uiState.joinRoomCodeDraft,
      onJoinRoomCodeDraftChange = { raw ->
        uiState.joinRoomCodeDraft =
          raw.uppercase(Locale.US).replace(Regex("[^A-Z0-9]"), "").take(8)
      },
      roomCodeBanner = uiState.roomCodeBanner,
      unlockedPrivateCodes = uiState.unlockedPrivateCodes,
      onJoinByRoomCode = {
        uiState.joinCustomByRoomCode(uiState.joinRoomCodeDraft, gateDeployShipId, params)
      },
      customBaseMode = uiState.customBaseMode,
      onSelectCustomBaseMode = { uiState.customBaseMode = it },
      onJoinCustomInstance = { instanceId ->
        val known =
          params.publicCustomGames.firstOrNull {
            it.instanceId.equals(instanceId, ignoreCase = true)
          }
        if (known != null && params.engine != null) {
          if (params.engine.activeCustomGames.none {
              it.instanceId.equals(known.instanceId, ignoreCase = true)
            }
          ) {
            params.engine.activeCustomGames.add(0, known)
          }
        }
        uiState.deployOrStageExistingCustom(instanceId, gateDeployShipId, params)
      },
      onQuickCreateCustomGame = {
        uiState.launchCustomGameFromDraft(gateDeployShipId, params)
      },
      customDamageTakenScalePct = uiState.customDamageTakenScalePct,
      onSelectCustomDamageTakenScalePct = {
        uiState.updateCustomDamageTakenScale(it, params.engine)
      },
      customWeaponFluxCostScalePct = uiState.customWeaponFluxCostScalePct,
      onSelectCustomWeaponFluxCostScalePct = {
        uiState.updateCustomWeaponFluxCostScale(it, params.engine)
      },
      customBotCount = uiState.customBotCount,
      onSelectCustomBotCount = { uiState.customBotCount = it.coerceIn(0, 19) },
      customBotSkillLevel = uiState.customBotSkillLevel,
      onSelectCustomBotSkillLevel = { uiState.customBotSkillLevel = it.coerceIn(0, 3) },
      customResetMinutes = uiState.customResetMinutes,
      onSelectCustomResetMinutes = { uiState.customResetMinutes = it.coerceIn(5, 60) },
      customScoreCap = uiState.customScoreCap,
      onSelectCustomScoreCap = { uiState.customScoreCap = it.coerceIn(10, 200) },
      customBulletSizeMult = uiState.customBulletSizeMult,
      onSelectCustomBulletSizeMult = {
        uiState.customBulletSizeMult = it.coerceIn(0.70f, 1.75f)
      },
      customSpeedMult = uiState.customSpeedMult,
      onSelectCustomSpeedMult = { uiState.customSpeedMult = it },
      customDamageMult = uiState.customDamageMult,
      onSelectCustomDamageMult = { uiState.customDamageMult = it },
      customRechargeMult = uiState.customRechargeMult,
      onSelectCustomRechargeMult = { uiState.customRechargeMult = it },
      customCrateSpawnMult = uiState.customCrateSpawnMult,
      onSelectCustomCrateSpawnMult = { uiState.customCrateSpawnMult = it },
      customAllowArmadasInFfa = uiState.customAllowArmadasInFfa,
      onToggleAllowArmadasInFfa = { uiState.customAllowArmadasInFfa = it },
      onOpenSurvivalSpreadsheet = { uiState.showHubSurvivalSpreadsheetModal = true },
      onOpenShipwrightStudio = { params.onOpenForge() },
      onOpenFullSquadronModal = {
        uiState.overwatchPrompt = OverwatchCircularPrompt.None
        uiState.sideBayMode = RiftHubSideBayMode.SQUADRON_BAY
      },
      syndicateWarRecord = params.syndicateWarRecord,
      syndicateDirectory = params.syndicateDirectory,
      syndicateStatusMessage = params.syndicateStatusMessage,
      onJoinOfficialSyndicate = params.onJoinOfficialSyndicate,
      onLeaveCurrentSyndicate = params.onLeaveCurrentSyndicate,
      onCharterCustomSyndicate = params.onCharterCustomSyndicate,
      onClaimDailyDirectiveSlot = { slotIdx -> params.onClaimDailyDirectiveSlot(slotIdx) },
      onRerollDailyDirectiveSlot = { slotIdx -> params.onRerollDailyDirectiveSlot(slotIdx) },
      onClaimDailySweepBonus = { params.onClaimDailySweepBonus() },
      onUnlockOrEquipCosmeticItem = { item ->
        params.onUnlockOrEquipCosmeticItem(item.category, item)
      },
      onUnlockOrSelectFrameSlotId = { slotId -> params.onUnlockOrSelectFrameSlotId(slotId) },
      activeCustomSlotId = params.activeCustomSlotId,
      activeCustomSlot = params.activeCustomSlot,
      pointBudget = params.pointBudget,
      onSaveCustomSlot = params.onSaveCustomSlot,
      onShareActiveRiftFrameBuild = params.onShareActiveRiftFrameBuild,
      cloudLeaderboardEntries = params.cloudLeaderboardEntries,
      onRenamePilotCallsign = { updated -> params.onRenamePilotCallsign(updated) },
      onResetLocalPilotTelemetry = params.onResetLocalPilotTelemetry,
      helmInitialSubDeck = params.helmInitialSubDeck,
      sfxEnabled = params.sfxEnabled,
      musicEnabled = params.musicEnabled,
      musicBpm = params.musicBpm,
      musicVolume = params.musicVolume,
      sfxVolume = params.sfxVolume,
      cameraZoom = params.cameraZoom,
      playfieldTextScale = params.playfieldTextScale,
      onToggleSfx = { _ -> params.onToggleSfx() },
      onToggleMusic = { _ -> params.onToggleMusic() },
      onChangeMusicBpm = params.onChangeMusicBpm,
      onChangeMusicVolume = params.onChangeMusicVolume,
      onChangeSfxVolume = params.onChangeSfxVolume,
      onChangeCameraZoom = params.onChangeCameraZoom,
      onChangePlayfieldTextScale = params.onChangePlayfieldTextScale,
      onOpenChangelogModal = params.onOpenChangelogModal,
      onOpenAudioSynthForge = params.onOpenAudioSynthForge,
      updateStatusLabel = params.updateStatusLabel,
      hasUpdateAvailable = params.hasUpdateAvailable,
      onCheckOrApplyUpdate = params.onCheckOrApplyUpdate,
      engine = params.engine,
      currentUserId = params.currentUserId,
      totalOnlinePilots = params.totalOnlinePilots,
      syndicateOnlineCount = params.syndicateOnlineCount,
      strikeWingOnlineCount = params.strikeWingOnlineCount,
      unreadCommsCount = params.unreadCommsCount,
      isNetworkOnline = params.isNetworkOnline,
      isGoogleAuthenticated = params.isGoogleAuthenticated,
      cloudSyncHealth = params.cloudSyncHealth,
      bootToNexusHub = params.bootToNexusHub,
      isInMatch = params.isInMatch,
      isStreaming = params.isStreaming,
      focusedOrbitalIndex = uiState.focusedOrbitalIndex,
      overwatchInstances = resolvedOverwatchInstances,
      activeOverwatchInstance = activeOverwatchInstance,
      favoriteOverwatchKey = params.favoriteOverwatchKey,
      onSelectOverwatchKey = params.onSelectOverwatchKey,
      onToggleFavoriteOverwatchKey = params.onToggleFavoriteOverwatchKey,
      onQuickZoneIntoOverwatch = { inst ->
        if (uiState.chargedShipId in 1..10) {
          params.onOpenHangarForShip(uiState.chargedShipId)
        }
        params.onQuickZoneIntoOverwatch(inst)
      },
      overwatchZoom = params.overwatchZoom,
      onOverwatchTransformGesture = params.onOverwatchTransformGesture,
      onResetOverwatchCamera = params.onResetOverwatchCamera,
      onApertureRootBoundsChanged = { cx, cy, r ->
        if (
          abs(uiState.hubApertureRootBounds.first - cx) > 0.5f ||
            abs(uiState.hubApertureRootBounds.second - cy) > 0.5f ||
            abs(uiState.hubApertureRootBounds.third - r) > 0.5f
        ) {
          uiState.hubApertureRootBounds = Triple(cx, cy, r)
        }
        params.onUpdateOverwatchViewport(cx, cy, r)
      },
      onToggleBootToNexusHub = params.onToggleBootToNexusHub,
      onLeaveMatch = {
        if (params.isInMatch) {
          uiState.showWarpOffConfirmModal = true
        } else {
          params.onLeaveMatch()
        }
      },
      onEnterPracticeRift = {
        if (uiState.chargedShipId in 1..10) {
          params.onOpenHangarForShip(uiState.chargedShipId)
        }
        params.onEnterPracticeRift()
      },
      onOpenHowToPlayTutorial = {
        if (uiState.chargedShipId in 1..10) {
          params.onOpenHangarForShip(uiState.chargedShipId)
        }
        params.onOpenHowToPlayTutorial()
      },
      onCloseModal = params.onCloseModal,
      onClickCallsign = {
        uiState.overwatchPrompt = OverwatchCircularPrompt.None
        uiState.sideBayMode = RiftHubSideBayMode.DOSSIER_BAY
      },
      onClickResonance = {
        uiState.sideBayMode = RiftHubSideBayMode.CONDUIT_BAY
        uiState.overwatchPrompt = OverwatchCircularPrompt.ResonanceLeaderboard
      },
      onClickShards = {
        uiState.overwatchPrompt = OverwatchCircularPrompt.None
        uiState.sideBayMode = RiftHubSideBayMode.SHARD_STORE_BAY
      },
      onClickSyn = {
        uiState.overwatchPrompt = OverwatchCircularPrompt.None
        uiState.sideBayMode = RiftHubSideBayMode.SYNDICATE_BAY
      },
      onClickWing = {
        uiState.overwatchPrompt = OverwatchCircularPrompt.None
        uiState.sideBayMode = RiftHubSideBayMode.SQUADRON_BAY
      },
      onClickOnline = {
        uiState.focusedOrbitalIndex = 3
        params.onSelectNode(NexusCoreNode.SUBSPACE_COMMS)
      },
      onOpenPilotDossier = {
        uiState.focusedOrbitalIndex = 6
        uiState.overwatchPrompt = OverwatchCircularPrompt.None
        uiState.sideBayMode = RiftHubSideBayMode.DOSSIER_BAY
      },
      onSelectNode = { idx, node ->
        uiState.focusedOrbitalIndex = idx
        when (node) {
          NexusCoreNode.HANGAR_DECK -> {
            uiState.overwatchPrompt = OverwatchCircularPrompt.None
            uiState.sideBayMode = RiftHubSideBayMode.HANGAR_BAY
          }
          NexusCoreNode.CONVERGENCE_GATE -> {
            uiState.overwatchPrompt = OverwatchCircularPrompt.None
            uiState.sideBayMode = RiftHubSideBayMode.GAME_MODES_BAY
          }
          NexusCoreNode.RIFT_CONDUIT -> {
            uiState.sideBayMode = RiftHubSideBayMode.CONDUIT_BAY
            uiState.overwatchPrompt = OverwatchCircularPrompt.ResonanceLeaderboard
          }
          NexusCoreNode.SYNDICATE_CITADEL -> {
            uiState.overwatchPrompt = OverwatchCircularPrompt.None
            uiState.sideBayMode =
              if (params.syndicateSubDeck == SyndicateCitadelSubDeck.STRIKE_WING) {
                RiftHubSideBayMode.SQUADRON_BAY
              } else {
                RiftHubSideBayMode.SYNDICATE_BAY
              }
          }
          NexusCoreNode.ARCHIVE_CODEX -> {
            uiState.overwatchPrompt = OverwatchCircularPrompt.None
            uiState.sideBayMode =
              if (params.archiveSubDeck == ArchiveCodexSubDeck.TACTICAL_CODEX) {
                RiftHubSideBayMode.HELM_CODEX_BAY
              } else {
                RiftHubSideBayMode.DOSSIER_BAY
              }
          }
          NexusCoreNode.HELM_SYSTEMS -> {
            uiState.overwatchPrompt = OverwatchCircularPrompt.None
            uiState.sideBayMode = RiftHubSideBayMode.HELM_CODEX_BAY
          }
          else -> params.onSelectNode(node)
        }
      },
      onSwitchAccount = { uiState.showSwitchAccountConfirmModal = true },
    )
  }
}

@Composable
private fun NexusGlassTerminalDeckHost(
  params: NexusAiConsoleParams,
  uiState: NexusConsoleUiState,
  chargedPrefsKey: String,
  gateDeployShipId: Int,
  gateShipStatusSummary: String,
  isLocalArchitect: Boolean,
  listState: LazyListState,
  keyboardController: androidx.compose.ui.platform.SoftwareKeyboardController?,
  terminalScale: Float,
  terminalAlpha: Float,
  terminalSlideFraction: Float,
) {
  NexusGlassTerminalDeckPanel(
    terminalScale = terminalScale,
    terminalAlpha = terminalAlpha,
    terminalSlideFraction = terminalSlideFraction,
    isCalibrationPeekActive = params.isCalibrationPeekActive,
    activeNode = params.activeNode,
    syndicateSubDeck = params.syndicateSubDeck,
    onSelectSyndicateSubDeck = params.onSelectSyndicateSubDeck,
    archiveSubDeck = params.archiveSubDeck,
    onSelectArchiveSubDeck = params.onSelectArchiveSubDeck,
    onOpenCodex = params.onOpenCodex,
    armadaSlots = params.armadaSlots,
    selectedChannelKey = uiState.selectedChannelKey,
    onSelectChannel = { uiState.selectedChannelKey = it },
    messages = params.messages,
    isInMatch = params.isInMatch,
    onOpenHangarFromHeader = {
      uiState.overwatchPrompt = OverwatchCircularPrompt.None
      uiState.sideBayMode = RiftHubSideBayMode.HANGAR_BAY
      params.onSelectNode(NexusCoreNode.ORBITAL_HUB)
    },
    onSelectNode = params.onSelectNode,
    onBackToHub = {
      uiState.overwatchPrompt = OverwatchCircularPrompt.None
      uiState.sideBayMode = RiftHubSideBayMode.HUB_MAIN
      params.onSelectNode(NexusCoreNode.ORBITAL_HUB)
    },
    onClearHistory = params.onClearHistory,
    onRequestLeaveMatch = {
      if (params.isInMatch) uiState.showWarpOffConfirmModal = true else params.onLeaveMatch()
    },
    onEnterPracticeRift = params.onEnterPracticeRift,
    onCloseModal = params.onCloseModal,
    gateTab = uiState.gateTab,
    onSelectGateTab = { uiState.gateTab = it },
    activeArmadaName = params.activeArmadaName,
    cleanSyndicateTag = params.cleanSyndicateTag,
    gateShipStatusSummary = gateShipStatusSummary,
    gateDeployShipId = gateDeployShipId,
    resolvedBestShipForGate = params.bestShipId.coerceIn(1, 10),
    onChargeShip = { uiState.chargeShipAcrossAllModes(it, chargedPrefsKey, params) },
    onOpenSquadronBay = {
      uiState.overwatchPrompt = OverwatchCircularPrompt.None
      uiState.sideBayMode = RiftHubSideBayMode.SQUADRON_BAY
      params.onSelectNode(NexusCoreNode.ORBITAL_HUB)
    },
    activeRankedMode = params.activeRankedMode,
    skirmishBracket = uiState.skirmishBracket,
    onSelectSkirmishBracket = { uiState.skirmishBracket = it.coerceIn(2, 5) },
    riftControlBracket = uiState.riftControlBracket,
    onSelectRiftControlBracket = { uiState.riftControlBracket = it },
    syndicateWarBracket = uiState.syndicateWarBracket,
    onSelectSyndicateWarBracket = { uiState.syndicateWarBracket = it },
    humanPilotCountByMode = params.humanPilotCountByMode,
    canQueueSyndicateWars = params.canQueueSyndicateWars,
    syndicateWarRecord = params.syndicateWarRecord,
    onDeployOrStageRanked = { mode, bracket ->
      uiState.deployOrStageRankedSelection(mode, bracket, params)
    },
    engine = params.engine,
    publicCustomGames = params.publicCustomGames,
    onlineHumanPilots = params.onlineHumanPilots,
    customIsPrivateRoom = uiState.customIsPrivateRoom,
    onTogglePrivateRoom = {
      uiState.customIsPrivateRoom = it
      uiState.generatedRoomCode = generateSixCharRoomCode()
    },
    generatedRoomCode = uiState.generatedRoomCode,
    joinRoomCodeDraft = uiState.joinRoomCodeDraft,
    onJoinRoomCodeDraftChange = { raw ->
      uiState.joinRoomCodeDraft =
        raw.uppercase(Locale.US).replace(Regex("[^A-Z0-9]"), "").take(8)
    },
    roomCodeBanner = uiState.roomCodeBanner,
    onRoomCodeBannerChange = { uiState.roomCodeBanner = it },
    unlockedPrivateCodes = uiState.unlockedPrivateCodes,
    onJoinByRoomCode = {
      uiState.joinCustomByRoomCode(uiState.joinRoomCodeDraft, gateDeployShipId, params)
    },
    onPostPrivateRoomInvite = params.onPostPrivateRoomInvite,
    customGamesRefreshTick = uiState.customGamesRefreshTick,
    onIncrementCustomGamesRefreshTick = { uiState.customGamesRefreshTick++ },
    customBaseMode = uiState.customBaseMode,
    onSelectCustomBaseMode = { uiState.customBaseMode = it },
    customResetMinutes = uiState.customResetMinutes,
    onSelectCustomResetMinutes = { uiState.customResetMinutes = it },
    customTeamBracket = uiState.customTeamBracket,
    onSelectCustomTeamBracket = { uiState.customTeamBracket = it },
    customAllowArmadasInFfa = uiState.customAllowArmadasInFfa,
    onToggleAllowArmadasInFfa = { uiState.customAllowArmadasInFfa = it },
    customBotsEnabled = uiState.customBotsEnabled,
    onToggleBotsEnabled = {
      uiState.customBotsEnabled = it
      if (it && uiState.customBotCount <= 0) uiState.customBotCount = 12
    },
    customBotCount = uiState.customBotCount,
    onSelectCustomBotCount = { count ->
      uiState.customBotCount = count.coerceIn(0, 19)
      uiState.customBotsEnabled = count > 0
    },
    customBotSkillLevel = uiState.customBotSkillLevel,
    onSelectCustomBotSkillLevel = { uiState.customBotSkillLevel = it.coerceIn(0, 3) },
    customScoreCap = uiState.customScoreCap,
    onSelectCustomScoreCap = { uiState.customScoreCap = it.coerceIn(5, 500) },
    customBulletSizeMult = uiState.customBulletSizeMult,
    onSelectCustomBulletSizeMult = { uiState.customBulletSizeMult = it },
    customSpeedMult = uiState.customSpeedMult,
    onSelectCustomSpeedMult = { uiState.customSpeedMult = it },
    customRechargeMult = uiState.customRechargeMult,
    onSelectCustomRechargeMult = { uiState.customRechargeMult = it },
    customDamageMult = uiState.customDamageMult,
    onSelectCustomDamageMult = { uiState.customDamageMult = it },
    customCrateSpawnMult = uiState.customCrateSpawnMult,
    onSelectCustomCrateSpawnMult = { uiState.customCrateSpawnMult = it },
    customDamageTakenScalePct = uiState.customDamageTakenScalePct,
    onUpdateCustomDamageTakenScale = {
      uiState.updateCustomDamageTakenScale(it, params.engine)
    },
    customWeaponFluxCostScalePct = uiState.customWeaponFluxCostScalePct,
    onUpdateCustomWeaponFluxCostScale = {
      uiState.updateCustomWeaponFluxCostScale(it, params.engine)
    },
    onDeployOrStageExistingCustom = {
      uiState.deployOrStageExistingCustom(it, gateDeployShipId, params)
    },
    onLaunchCustomGame = { uiState.launchCustomGameFromDraft(gateDeployShipId, params) },
    streamState = params.streamState,
    isStreaming = params.isStreaming,
    lastFailedQuestion = params.lastFailedQuestion,
    listState = listState,
    inputQuery = uiState.inputQuery,
    onInputQueryChange = { uiState.inputQuery = it },
    onSubmitAiQuery = { uiState.submitNexusAiQuery(params, keyboardController) },
    onSendQuestion = params.onSendQuestion,
    onRetryLastQuestion = params.onRetryLastQuestion,
    callsign = params.callsign,
    syndicateName = params.syndicateName,
    isLocalArchitect = isLocalArchitect,
    canAccessSyndicateServer = params.canAccessSyndicateServer,
    syndicateCustomChannels = params.syndicateCustomChannels,
    syndicateInvitedGuests = params.syndicateInvitedGuests,
    showChannelManager = uiState.showChannelManager,
    onToggleChannelManager = { uiState.showChannelManager = !uiState.showChannelManager },
    newChannelDraft = uiState.newChannelDraft,
    onNewChannelDraftChange = { uiState.newChannelDraft = it },
    onAddSyndicateChannel = {
      if (uiState.newChannelDraft.isNotBlank()) {
        params.onAddSyndicateChannel(uiState.newChannelDraft)
        uiState.newChannelDraft = ""
      }
    },
    onDeleteSyndicateChannel = params.onDeleteSyndicateChannel,
    showGuestManager = uiState.showGuestManager,
    onToggleGuestManager = { uiState.showGuestManager = !uiState.showGuestManager },
    guestCallsignDraft = uiState.guestCallsignDraft,
    onGuestDraftChange = { uiState.guestCallsignDraft = it },
    onInviteGuest = {
      if (uiState.guestCallsignDraft.isNotBlank()) {
        params.onInviteSyndicateGuest(uiState.guestCallsignDraft)
        uiState.guestCallsignDraft = ""
      }
    },
    onRevokeGuest = params.onRevokeSyndicateGuest,
    onOpenSyndicateCitadel = {
      uiState.overwatchPrompt = OverwatchCircularPrompt.None
      uiState.sideBayMode = RiftHubSideBayMode.SYNDICATE_BAY
      params.onSelectNode(NexusCoreNode.ORBITAL_HUB)
    },
    lobbyMessages = params.lobbyMessages,
    localEchoMessages = uiState.localEchoMessages,
    channelChatInput = uiState.channelChatInput,
    onChannelChatInputChange = { uiState.channelChatInput = it },
    onSendToChannel = { key, txt ->
      uiState.postToCloudChannel(key, txt, params)
      uiState.channelChatInput = ""
      keyboardController?.hide()
    },
    onSendVoiceHeartbeat = { voiceKey, payload ->
      params.onSendChannelMessage(voiceKey, payload)
    },
    bugCategory = uiState.bugCategory,
    onSelectBugCategory = { uiState.bugCategory = it },
    bugDescription = uiState.bugDescription,
    onBugDescriptionChange = { uiState.bugDescription = it },
    bugStatusBanner = uiState.bugStatusBanner,
    onSubmitBug = {
      val clean = uiState.bugDescription.trim()
      if (clean.isNotEmpty()) {
        params.onSubmitBugReport(uiState.bugCategory, clean)
        uiState.bugDescription = ""
        uiState.bugStatusBanner =
          "ANOMALY LOGGED TO CLOUD SECTOR TELEMETRY [${uiState.bugCategory}]"
        keyboardController?.hide()
      }
    },
    onShareRiftFrameBuild = params.onShareActiveRiftFrameBuild,
    onLoadSharedBuild = params.onLoadSharedRiftFrameBuild,
    onPostStrikeWingInvite = params.onPostStrikeWingInvite,
    onJoinSharedStrikeWing = params.onJoinSharedStrikeWing,
    onJoinCustomRoomCode = { code ->
      uiState.joinCustomByRoomCode(code, gateDeployShipId, params)
    },
    totalOnlinePilots = params.totalOnlinePilots,
    showOnlineRosterPane = uiState.showOnlineRosterPane,
    onToggleOnlineRosterPane = { uiState.showOnlineRosterPane = !uiState.showOnlineRosterPane },
    onAddOnlinePilotFriend = params.onAddOnlinePilotFriend,
    onInviteOnlinePilotToWing = params.onInviteOnlinePilotToWing,
    renderModuleForNode = params.renderModuleForNode,
    embeddedModuleContent = params.embeddedModuleContent,
  )
}

private fun DrawScope.drawNexusOrbitalChassisBackdrop(
  hubApertureRootBounds: Triple<Float, Float, Float>,
  modalRootOrigin: Offset,
) {
  val w = size.width
  val h = size.height
  val (acx, acy, ar) = hubApertureRootBounds
  val localCx = if (ar > 8f) acx - modalRootOrigin.x else w * 0.5f
  val localCy = if (ar > 8f) acy - modalRootOrigin.y else h * 0.5f
  val cutR = if (ar > 8f) ar else min(w, h) * 0.34f

  drawContext.canvas.save()
  if (ar > 8f) {
    val maskPath =
      Path().apply {
        fillType = PathFillType.EvenOdd
        addRect(Rect(0f, 0f, w, h))
        addOval(Rect(localCx - cutR, localCy - cutR, localCx + cutR, localCy + cutR))
      }
    drawContext.canvas.clipPath(maskPath)
  }

  drawRect(
    brush = Brush.verticalGradient(colors = listOf(Color(0xFF060D1A), Color(0xFF030711), Color(0xFF02050B))),
    size = size,
  )
  drawRect(
    brush =
      Brush.radialGradient(
        colors = listOf(Color(0xFF0B192E), Color(0xFF091528), Color(0xFF060D1A), Color(0xFF02050B)),
        center = Offset(localCx, localCy),
        radius = maxOf(w, h) * 0.64f,
      ),
    size = size,
  )
  for (spoke in 0 until 12) {
    val rad = spoke * (PI.toFloat() / 6f)
    val r0 = cutR * 1.05f
    val r1 = maxOf(w, h) * 0.74f
    drawLine(
      color = if (spoke % 3 == 0) Color(0x1E00F5D4) else Color(0x1438BDF8),
      start = Offset(localCx + cos(rad) * r0, localCy + sin(rad) * r0),
      end = Offset(localCx + cos(rad) * r1, localCy + sin(rad) * r1),
      strokeWidth = 0.85.dp.toPx(),
    )
  }

  val ring1 = cutR * 1.10f
  val ring2 = cutR * 1.28f
  val ring3 = cutR * 1.52f
  drawCircle(
    brush = Brush.verticalGradient(listOf(Color(0x4500F5D4), Color(0x2438BDF8), Color(0x32FACC15)), startY = localCy - ring1, endY = localCy + ring1),
    radius = ring1,
    center = Offset(localCx, localCy),
    style = Stroke(width = 1.25.dp.toPx()),
  )
  drawCircle(
    brush = Brush.verticalGradient(listOf(Color(0x28FFD700), Color(0x1838BDF8), Color(0x2600F5D4)), startY = localCy - ring2, endY = localCy + ring2),
    radius = ring2,
    center = Offset(localCx, localCy),
    style = Stroke(width = 0.95.dp.toPx()),
  )
  drawCircle(
    color = Color(0x1438BDF8),
    radius = ring3,
    center = Offset(localCx, localCy),
    style = Stroke(width = 0.8.dp.toPx()),
  )

  val conduitAnglesDeg = floatArrayOf(-148f, 148f, -32f, 32f)
  for (i in conduitAnglesDeg.indices) {
    val rad = conduitAnglesDeg[i] * (PI.toFloat() / 180f)
    val p0 = Offset(localCx + cos(rad) * (cutR * 1.045f), localCy + sin(rad) * (cutR * 1.045f))
    val p1 = Offset(localCx + cos(rad) * (ring2 * 1.26f), localCy + sin(rad) * (ring2 * 1.26f))
    val coreCol =
      when (i) {
        0 -> Color(0x4838BDF8)
        1 -> Color(0x4800F5D4)
        2 -> Color(0x48FFD700)
        else -> Color(0x4600F5D4)
      }
    drawLine(color = Color(0xC002050B), start = p0, end = p1, strokeWidth = 4.0.dp.toPx(), cap = StrokeCap.Round)
    drawLine(
      brush = Brush.linearGradient(listOf(coreCol, Color.Transparent), start = p0, end = p1),
      start = p0,
      end = p1,
      strokeWidth = 1.65.dp.toPx(),
      cap = StrokeCap.Round,
    )
  }

  drawContext.canvas.restore()
}

@Composable
private fun NexusGlassTerminalDeckPanel(
  terminalScale: Float,
  terminalAlpha: Float,
  terminalSlideFraction: Float,
  isCalibrationPeekActive: Boolean,
  activeNode: NexusCoreNode,
  syndicateSubDeck: SyndicateCitadelSubDeck,
  onSelectSyndicateSubDeck: (SyndicateCitadelSubDeck) -> Unit,
  archiveSubDeck: ArchiveCodexSubDeck,
  onSelectArchiveSubDeck: (ArchiveCodexSubDeck) -> Unit,
  onOpenCodex: () -> Unit,
  armadaSlots: List<ArmadaMemberSlot>,
  selectedChannelKey: String,
  onSelectChannel: (String) -> Unit,
  messages: List<NexusChatMessage>,
  isInMatch: Boolean,
  onOpenHangarFromHeader: () -> Unit,
  onSelectNode: (NexusCoreNode) -> Unit,
  onBackToHub: () -> Unit,
  onClearHistory: () -> Unit,
  onRequestLeaveMatch: () -> Unit,
  onEnterPracticeRift: () -> Unit,
  onCloseModal: () -> Unit,
  gateTab: ConvergenceGateTab,
  onSelectGateTab: (ConvergenceGateTab) -> Unit,
  activeArmadaName: String,
  cleanSyndicateTag: String,
  gateShipStatusSummary: String,
  gateDeployShipId: Int,
  resolvedBestShipForGate: Int,
  onChargeShip: (Int) -> Unit,
  onOpenSquadronBay: () -> Unit,
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
  onDeployOrStageRanked: (RankedGameMode, Int) -> Unit,
  engine: RiftGameEngine?,
  publicCustomGames: List<CustomGameConfig>,
  onlineHumanPilots: List<LivePilotState>,
  customIsPrivateRoom: Boolean,
  onTogglePrivateRoom: (Boolean) -> Unit,
  generatedRoomCode: String,
  joinRoomCodeDraft: String,
  onJoinRoomCodeDraftChange: (String) -> Unit,
  roomCodeBanner: String?,
  onRoomCodeBannerChange: (String) -> Unit,
  unlockedPrivateCodes: MutableList<String>,
  onJoinByRoomCode: () -> Unit,
  onPostPrivateRoomInvite: (String, String) -> Unit,
  customGamesRefreshTick: Int,
  onIncrementCustomGamesRefreshTick: () -> Unit,
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
  customBotCount: Int,
  onSelectCustomBotCount: (Int) -> Unit,
  customBotSkillLevel: Int,
  onSelectCustomBotSkillLevel: (Int) -> Unit,
  customScoreCap: Int,
  onSelectCustomScoreCap: (Int) -> Unit,
  customBulletSizeMult: Float,
  onSelectCustomBulletSizeMult: (Float) -> Unit,
  customSpeedMult: Float,
  onSelectCustomSpeedMult: (Float) -> Unit,
  customRechargeMult: Float,
  onSelectCustomRechargeMult: (Float) -> Unit,
  customDamageMult: Float,
  onSelectCustomDamageMult: (Float) -> Unit,
  customCrateSpawnMult: Float,
  onSelectCustomCrateSpawnMult: (Float) -> Unit,
  customDamageTakenScalePct: Int,
  onUpdateCustomDamageTakenScale: (Int) -> Unit,
  customWeaponFluxCostScalePct: Int,
  onUpdateCustomWeaponFluxCostScale: (Int) -> Unit,
  onDeployOrStageExistingCustom: (String) -> Unit,
  onLaunchCustomGame: () -> Unit,
  streamState: NexusStreamState,
  isStreaming: Boolean,
  lastFailedQuestion: String?,
  listState: LazyListState,
  inputQuery: String,
  onInputQueryChange: (String) -> Unit,
  onSubmitAiQuery: () -> Unit,
  onSendQuestion: (String) -> Unit,
  onRetryLastQuestion: () -> Unit,
  callsign: String,
  syndicateName: String,
  isLocalArchitect: Boolean,
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
  onSendVoiceHeartbeat: (String, String) -> Unit,
  bugCategory: String,
  onSelectBugCategory: (String) -> Unit,
  bugDescription: String,
  onBugDescriptionChange: (String) -> Unit,
  bugStatusBanner: String?,
  onSubmitBug: () -> Unit,
  onShareRiftFrameBuild: () -> Unit,
  onLoadSharedBuild: (String) -> Unit,
  onPostStrikeWingInvite: () -> Unit,
  onJoinSharedStrikeWing: (SyndicateChatMessage) -> Unit,
  onJoinCustomRoomCode: (String) -> Unit,
  totalOnlinePilots: Int,
  showOnlineRosterPane: Boolean,
  onToggleOnlineRosterPane: () -> Unit,
  onAddOnlinePilotFriend: (String) -> Unit,
  onInviteOnlinePilotToWing: (LivePilotState) -> Unit,
  renderModuleForNode: (@Composable (NexusCoreNode) -> Unit)?,
  embeddedModuleContent: (@Composable () -> Unit)?,
) {
  val terminalChassisShape =
    CutCornerShape(topStart = 14.dp, topEnd = 8.dp, bottomEnd = 14.dp, bottomStart = 8.dp)
  Surface(
    modifier =
      Modifier.fillMaxSize()
        .padding(horizontal = 4.dp, vertical = 2.dp)
        .graphicsLayer {
          scaleX = terminalScale
          scaleY = terminalScale
          alpha = terminalAlpha
          translationX = size.width * terminalSlideFraction
        }
        .border(
          1.35.dp,
          Brush.linearGradient(
            listOf(Color(0xFF00F5D4), Color(0xFF38BDF8), Color(0xFF1E293B), Color(0xFFFACC15))
          ),
          terminalChassisShape,
        ),
    color = Color.Transparent,
    shape = terminalChassisShape,
    shadowElevation = 14.dp,
  ) {
    Column(
      modifier =
        Modifier.fillMaxSize()
          .background(
            if (isCalibrationPeekActive) {
              Brush.verticalGradient(listOf(Color(0x48091528), Color(0x3D030711)))
            } else {
              Brush.verticalGradient(
                listOf(Color(0xFA091528), Color(0xF8060D1A), Color(0xFC030711))
              )
            }
          )
          .drawBehind {
            drawLine(
              brush =
                Brush.horizontalGradient(
                  listOf(
                    Color.White.copy(alpha = 0.42f),
                    Color(0xFF00F5D4).copy(alpha = 0.34f),
                    Color.Transparent,
                  )
                ),
              start = Offset(16.dp.toPx(), 1.4.dp.toPx()),
              end = Offset(size.width * 0.62f, 1.4.dp.toPx()),
              strokeWidth = 1.2.dp.toPx(),
            )
          }
          .padding(horizontal = 10.dp, vertical = 6.dp),
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
        onSelectHubCard = { nextCardNode ->
          when (nextCardNode) {
            NexusCoreNode.HANGAR_DECK -> onOpenHangarFromHeader()
            else -> {
              if (nextCardNode != activeNode) {
                onSelectNode(nextCardNode)
              }
            }
          }
        },
        onBackToHub = onBackToHub,
        onClearAi = onClearHistory,
        onLeaveMatch = onRequestLeaveMatch,
        onEnterPracticeRift = onEnterPracticeRift,
        onCloseToArena = onCloseModal,
      )

      AnimatedContent(
        targetState = activeNode,
        modifier = Modifier.weight(1f).fillMaxWidth(),
        transitionSpec = {
          val fromOrder = riftHubCardTransitionOrder(initialState)
          val toOrder = riftHubCardTransitionOrder(targetState)
          val forward = toOrder >= fromOrder
          (slideInHorizontally(
            animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing)
          ) { fullWidth ->
            if (forward) (fullWidth / 5).coerceAtLeast(24) else -(fullWidth / 5).coerceAtLeast(24)
          } + fadeIn(animationSpec = tween(durationMillis = 210, easing = FastOutSlowInEasing)))
            .togetherWith(
              slideOutHorizontally(
                animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing)
              ) { fullWidth ->
                if (forward) -(fullWidth / 6).coerceAtLeast(20) else (fullWidth / 6).coerceAtLeast(20)
              } + fadeOut(animationSpec = tween(durationMillis = 160, easing = FastOutSlowInEasing))
            )
            .using(SizeTransform(clip = false))
        },
        label = "rift_hub_card_deck_transition",
      ) { targetCardNode ->
        Box(modifier = Modifier.fillMaxSize()) {
          when {
            targetCardNode == NexusCoreNode.CONVERGENCE_GATE -> {
              ConvergenceGateZeroScrollDeck(
                gateTab = gateTab,
                onSelectGateTab = onSelectGateTab,
                activeArmadaName = activeArmadaName,
                armadaSize = armadaSlots.size.coerceAtLeast(1),
                syndicateTag = cleanSyndicateTag,
                hangarShipSummaryLabel = gateShipStatusSummary,
                activeShipId = gateDeployShipId,
                bestShipId = resolvedBestShipForGate,
                onSelectShipInPlace = onChargeShip,
                onOpenStrikeWing = onOpenSquadronBay,
                activeRankedMode = activeRankedMode,
                skirmishBracket = skirmishBracket,
                onSelectSkirmishBracket = onSelectSkirmishBracket,
                riftControlBracket = riftControlBracket,
                onSelectRiftControlBracket = onSelectRiftControlBracket,
                syndicateWarBracket = syndicateWarBracket,
                onSelectSyndicateWarBracket = onSelectSyndicateWarBracket,
                humanPilotCountByMode = humanPilotCountByMode,
                canQueueSyndicateWars = canQueueSyndicateWars,
                syndicateWarRecord = syndicateWarRecord,
                onStageRanked = onDeployOrStageRanked,
                engine = engine,
                publicCustomGames = publicCustomGames,
                onlineHumanPilots = onlineHumanPilots,
                customIsPrivateRoom = customIsPrivateRoom,
                onTogglePrivateRoom = onTogglePrivateRoom,
                generatedRoomCode = generatedRoomCode,
                joinRoomCodeDraft = joinRoomCodeDraft,
                onJoinRoomCodeDraftChange = onJoinRoomCodeDraftChange,
                roomCodeBanner = roomCodeBanner,
                unlockedPrivateCodes = unlockedPrivateCodes,
                onJoinByRoomCode = onJoinByRoomCode,
                onShareRoomToLfg = {
                  val activeRoom = engine?.currentCustomGame
                  val shareCode =
                    if (activeRoom != null && engine.hasJoinedMatch) extractCustomRoomCode(activeRoom)
                    else generatedRoomCode
                  val shareMode =
                    if (activeRoom != null && engine.hasJoinedMatch) activeRoom.baseMode.title
                    else customBaseMode.title
                  onPostPrivateRoomInvite(shareCode, shareMode)
                  onRoomCodeBannerChange("SHARED JOIN CODE [$shareCode] TO #SQUADRON-LFG")
                },
                customRefreshTick = customGamesRefreshTick,
                customBaseMode = customBaseMode,
                onSelectCustomBaseMode = onSelectCustomBaseMode,
                customResetMinutes = customResetMinutes,
                onSelectCustomResetMinutes = onSelectCustomResetMinutes,
                customTeamBracket = customTeamBracket,
                onSelectCustomTeamBracket = onSelectCustomTeamBracket,
                customAllowArmadasInFfa = customAllowArmadasInFfa,
                onToggleAllowArmadasInFfa = onToggleAllowArmadasInFfa,
                customBotsEnabled = customBotsEnabled,
                onToggleBotsEnabled = onToggleBotsEnabled,
                customBotCount = customBotCount,
                onSelectCustomBotCount = onSelectCustomBotCount,
                customBotSkillLevel = customBotSkillLevel,
                onSelectCustomBotSkillLevel = onSelectCustomBotSkillLevel,
                customScoreCap = customScoreCap,
                onSelectCustomScoreCap = onSelectCustomScoreCap,
                customBulletSizeMult = customBulletSizeMult,
                onSelectBulletSizeMult = onSelectCustomBulletSizeMult,
                customSpeedMult = customSpeedMult,
                onSelectSpeedMult = onSelectCustomSpeedMult,
                customRechargeMult = customRechargeMult,
                onSelectRechargeMult = onSelectCustomRechargeMult,
                customDamageMult = customDamageMult,
                onSelectDamageMult = onSelectCustomDamageMult,
                customCrateSpawnMult = customCrateSpawnMult,
                onSelectCrateSpawnMult = onSelectCustomCrateSpawnMult,
                customDamageTakenScalePct = customDamageTakenScalePct,
                onSelectCustomDamageTakenScalePct = onUpdateCustomDamageTakenScale,
                customWeaponFluxCostScalePct = customWeaponFluxCostScalePct,
                onSelectCustomWeaponFluxCostScalePct = onUpdateCustomWeaponFluxCostScale,
                onReturnToRankedFfa = {
                  onDeployOrStageRanked(RankedGameMode.SECTOR_CONVERGENCE, 20)
                  onIncrementCustomGamesRefreshTick()
                },
                onJoinCustomInstance = { instanceId ->
                  val known =
                    publicCustomGames.firstOrNull {
                      it.instanceId.equals(instanceId, ignoreCase = true)
                    }
                  if (known != null && engine != null) {
                    if (engine.activeCustomGames.none { it.instanceId.equals(known.instanceId, ignoreCase = true) }) {
                      engine.activeCustomGames.add(0, known)
                    }
                  }
                  onDeployOrStageExistingCustom(instanceId)
                },
                onLaunchCustomGame = onLaunchCustomGame,
              )
            }
            targetCardNode == NexusCoreNode.SUBSPACE_COMMS -> {
              SubspaceCommsDiscordDeck(
                selectedChannelKey = selectedChannelKey,
                onSelectChannel = onSelectChannel,
                messages = messages,
                streamState = streamState,
                isStreaming = isStreaming,
                lastFailedQuestion = lastFailedQuestion,
                listState = listState,
                inputQuery = inputQuery,
                onInputQueryChange = onInputQueryChange,
                onSubmitAiQuery = onSubmitAiQuery,
                onSendStarterQuestion = onSendQuestion,
                onRetryLastQuestion = onRetryLastQuestion,
                callsign = callsign,
                cleanSyndicateTag = cleanSyndicateTag,
                syndicateName = syndicateName,
                isLocalArchitect = isLocalArchitect,
                isInMatch = isInMatch,
                activeRankedModeTitle = activeRankedMode.title,
                activeArmadaName = activeArmadaName,
                armadaSlots = armadaSlots,
                canAccessSyndicateServer = canAccessSyndicateServer,
                syndicateCustomChannels = syndicateCustomChannels,
                syndicateInvitedGuests = syndicateInvitedGuests,
                showChannelManager = showChannelManager,
                onToggleChannelManager = onToggleChannelManager,
                newChannelDraft = newChannelDraft,
                onNewChannelDraftChange = onNewChannelDraftChange,
                onAddSyndicateChannel = onAddSyndicateChannel,
                onDeleteSyndicateChannel = onDeleteSyndicateChannel,
                showGuestManager = showGuestManager,
                onToggleGuestManager = onToggleGuestManager,
                guestCallsignDraft = guestCallsignDraft,
                onGuestDraftChange = onGuestDraftChange,
                onInviteGuest = onInviteGuest,
                onRevokeGuest = onRevokeGuest,
                onOpenSyndicateCitadel = onOpenSyndicateCitadel,
                lobbyMessages = lobbyMessages,
                localEchoMessages = localEchoMessages,
                channelChatInput = channelChatInput,
                onChannelChatInputChange = onChannelChatInputChange,
                onSendToChannel = onSendToChannel,
                onSendVoiceHeartbeat = onSendVoiceHeartbeat,
                bugCategory = bugCategory,
                onSelectBugCategory = onSelectBugCategory,
                bugDescription = bugDescription,
                onBugDescriptionChange = onBugDescriptionChange,
                bugStatusBanner = bugStatusBanner,
                onSubmitBug = onSubmitBug,
                activeCustomRoomCode = engine?.currentCustomGame?.let { extractCustomRoomCode(it) },
                activeCustomModeTitle = engine?.currentCustomGame?.baseMode?.title,
                onShareRiftFrameBuild = onShareRiftFrameBuild,
                onLoadSharedBuild = onLoadSharedBuild,
                onAskAiAboutBuild = { prompt ->
                  onSelectChannel("#nexus-ai")
                  onSendQuestion(prompt)
                },
                onPostStrikeWingInvite = onPostStrikeWingInvite,
                onPostPrivateRoomInvite = onPostPrivateRoomInvite,
                onJoinSharedStrikeWing = onJoinSharedStrikeWing,
                onJoinCustomRoomCode = onJoinCustomRoomCode,
                onlineHumanPilots = onlineHumanPilots,
                totalOnlinePilots = totalOnlinePilots,
                showOnlineRosterPane = showOnlineRosterPane,
                onToggleOnlineRosterPane = onToggleOnlineRosterPane,
                onAddOnlinePilotFriend = onAddOnlinePilotFriend,
                onInviteOnlinePilotToWing = onInviteOnlinePilotToWing,
              )
            }
            renderModuleForNode != null -> {
              Box(modifier = Modifier.fillMaxSize().testTag("nexus_embedded_module_container")) {
                renderModuleForNode(targetCardNode)
              }
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

@Composable
private fun OrbitalHubSpatialStage(
  callsign: String,
  syndicateTag: String,
  syndicateName: String = "UNAFFILIATED",
  liveResonance: Int,
  riftShards: Long,
  activeShipId: Int,
  bestShipId: Int = 1,
  chargedShipId: Int = 0,
  onChargeShip: (Int) -> Unit = {},
  activeShipSummary: String,
  armadaCount: Int,
  activeArmadaName: String,
  armadaSlots: List<ArmadaMemberSlot> = emptyList(),
  onlineHumanPilots: List<LivePilotState> = emptyList(),
  onResolvePilotBestShipId: (uid: String, fallbackShipId: Int) -> Int = { _, fb -> fb },
  onInviteOnlinePilotToWing: (LivePilotState) -> Unit = {},
  onAddOnlinePilotFriend: (String) -> Unit = {},
  onPostStrikeWingInvite: () -> Unit = {},
  squadronStatusBanner: String? = null,
  sideBayMode: RiftHubSideBayMode = RiftHubSideBayMode.HUB_MAIN,
  onChangeSideBayMode: (RiftHubSideBayMode) -> Unit = {},
  overwatchPrompt: OverwatchCircularPrompt = OverwatchCircularPrompt.None,
  onChangeOverwatchPrompt: (OverwatchCircularPrompt) -> Unit = {},
  squadronFilter: SquadronPilotFilter = SquadronPilotFilter.ALL,
  onSelectSquadronFilter: (SquadronPilotFilter) -> Unit = {},
  squadronSort: SquadronPilotSort = SquadronPilotSort.RESONANCE,
  onToggleSquadronSort: () -> Unit = {},
  selectedSquadronPilotUid: String = "",
  onSelectSquadronPilotUid: (String) -> Unit = {},
  activeRankedMode: RankedGameMode = RankedGameMode.SECTOR_CONVERGENCE,
  skirmishBracket: Int = 3,
  onSelectSkirmishBracket: (Int) -> Unit = {},
  riftControlBracket: Int = 5,
  onSelectRiftControlBracket: (Int) -> Unit = {},
  syndicateWarBracket: Int = 5,
  onSelectSyndicateWarBracket: (Int) -> Unit = {},
  humanPilotCountByMode: (RankedGameMode) -> Int = { 0 },
  canQueueSyndicateWars: Pair<Boolean, String> = Pair(false, "Requires enlisted Syndicate"),
  onStageRanked: (RankedGameMode, Int) -> Unit = { _, _ -> },
  publicCustomGames: List<CustomGameConfig> = emptyList(),
  customIsPrivateRoom: Boolean = false,
  onTogglePrivateRoom: (Boolean) -> Unit = {},
  generatedRoomCode: String = "RFT101",
  joinRoomCodeDraft: String = "",
  onJoinRoomCodeDraftChange: (String) -> Unit = {},
  roomCodeBanner: String? = null,
  unlockedPrivateCodes: List<String> = emptyList(),
  onJoinByRoomCode: () -> Unit = {},
  customBaseMode: RankedGameMode = RankedGameMode.SECTOR_CONVERGENCE,
  onSelectCustomBaseMode: (RankedGameMode) -> Unit = {},
  onJoinCustomInstance: (String) -> Unit = {},
  onQuickCreateCustomGame: () -> Unit = {},
  customDamageTakenScalePct: Int = 100,
  onSelectCustomDamageTakenScalePct: (Int) -> Unit = {},
  customWeaponFluxCostScalePct: Int = 100,
  onSelectCustomWeaponFluxCostScalePct: (Int) -> Unit = {},
  customBotCount: Int = 12,
  onSelectCustomBotCount: (Int) -> Unit = {},
  customBotSkillLevel: Int = 1,
  onSelectCustomBotSkillLevel: (Int) -> Unit = {},
  customResetMinutes: Int = 10,
  onSelectCustomResetMinutes: (Int) -> Unit = {},
  customScoreCap: Int = 50,
  onSelectCustomScoreCap: (Int) -> Unit = {},
  customBulletSizeMult: Float = 1.0f,
  onSelectCustomBulletSizeMult: (Float) -> Unit = {},
  customSpeedMult: Float = 1.0f,
  onSelectCustomSpeedMult: (Float) -> Unit = {},
  customDamageMult: Float = 1.0f,
  onSelectCustomDamageMult: (Float) -> Unit = {},
  customRechargeMult: Float = 1.0f,
  onSelectCustomRechargeMult: (Float) -> Unit = {},
  customCrateSpawnMult: Float = 1.0f,
  onSelectCustomCrateSpawnMult: (Float) -> Unit = {},
  customAllowArmadasInFfa: Boolean = true,
  onToggleAllowArmadasInFfa: (Boolean) -> Unit = {},
  onOpenSurvivalSpreadsheet: () -> Unit = {},
  onOpenShipwrightStudio: () -> Unit = {},
  onOpenFullSquadronModal: () -> Unit = {},
  @Suppress("UNUSED_PARAMETER") syndicateWarRecord: SyndicateWarRecord,
  syndicateDirectory: List<SyndicateInfo> = emptyList(),
  syndicateStatusMessage: String? = null,
  onJoinOfficialSyndicate: (SyndicateInfo) -> Unit = {},
  onLeaveCurrentSyndicate: () -> Unit = {},
  onCharterCustomSyndicate: (tag: String, name: String, motto: String) -> Unit = { _, _, _ -> },
  onClaimDailyDirectiveSlot: (Int) -> Unit = {},
  onRerollDailyDirectiveSlot: (Int) -> Unit = {},
  onClaimDailySweepBonus: () -> Unit = {},
  onUnlockOrEquipCosmeticItem: (CosmeticCatalogItem) -> Pair<Boolean, String> = { false to "" },
  onUnlockOrSelectFrameSlotId: (String) -> Pair<Boolean, String> = { false to "" },
  activeCustomSlotId: String = "Frame A",
  activeCustomSlot: CustomShipLoadoutSlot = CustomShipLoadoutSlot(),
  pointBudget: Int = 100,
  onSaveCustomSlot: (CustomShipLoadoutSlot) -> Unit = {},
  onShareActiveRiftFrameBuild: () -> Unit = {},
  cloudLeaderboardEntries: List<LeaderboardEntry> = emptyList(),
  onRenamePilotCallsign: (String) -> Unit = {},
  onResetLocalPilotTelemetry: () -> Unit = {},
  helmInitialSubDeck: Int = 0,
  sfxEnabled: Boolean = true,
  musicEnabled: Boolean = true,
  musicBpm: Int = 128,
  musicVolume: Float = 0.7f,
  sfxVolume: Float = 0.85f,
  cameraZoom: Float = 1.0f,
  playfieldTextScale: Float = 1.0f,
  onToggleSfx: (Boolean) -> Unit = {},
  onToggleMusic: (Boolean) -> Unit = {},
  onChangeMusicBpm: (Int) -> Unit = {},
  onChangeMusicVolume: (Float) -> Unit = {},
  onChangeSfxVolume: (Float) -> Unit = {},
  onChangeCameraZoom: (Float) -> Unit = {},
  onChangePlayfieldTextScale: (Float) -> Unit = {},
  onOpenChangelogModal: () -> Unit = {},
  onOpenAudioSynthForge: () -> Unit = {},
  updateStatusLabel: String = "v1.0.$CURRENT_RIFT_BUILD_VERSION UP TO DATE",
  hasUpdateAvailable: Boolean = false,
  onCheckOrApplyUpdate: () -> Unit = {},
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
  val density = LocalDensity.current
  val headless = remember { isHeadlessGlEnvironment() }
  val localPrefs = remember(context) { LocalPilotPrefs(context) }
  var renameDraftState by remember(callsign) { mutableStateOf(callsign) }
  var storePurchaseBanner by remember { mutableStateOf<String?>(null) }
  var liveShardsBonus by remember(riftShards) { mutableStateOf(0L) }
  var dossierRefreshNonce by remember { mutableIntStateOf(0) }
  var hubTutorialStep by remember { mutableIntStateOf(0) }
  val syncEquippedCosmeticsNow: () -> Unit = {
    val uid = currentUserId.ifBlank { null }
    val equippedTrim = localPrefs.getEquippedCosmeticId("HULL_TRIM", uid).coerceIn(0, 5)
    val equippedFinisher = localPrefs.getEquippedCosmeticId("KILL_FINISHER", uid).coerceIn(0, 4)
    val equippedWarp = localPrefs.getEquippedCosmeticId("WARP_SIGNATURE", uid).coerceIn(0, 3)
    val equippedHalo = localPrefs.getEquippedCosmeticId("HVT_HALO", uid).coerceIn(0, 3)
    if (RiftSpriteBaker.activeHullTrimStyle != equippedTrim) {
      RiftSpriteBaker.activeHullTrimStyle = equippedTrim
      RiftVisualForgeState.visualRevision++
    }
    engine?.localPlayer?.let { lp ->
      lp.equippedHullSkin = equippedTrim
      lp.equippedKillFinisher = equippedFinisher
      lp.equippedWarpStyle = equippedWarp
      lp.equippedHvtHaloStyle = equippedHalo
    }
  }
  LaunchedEffect(currentUserId) { syncEquippedCosmeticsNow() }
  val handleUnlockOrEquipCosmetic: (CosmeticCatalogItem) -> Pair<Boolean, String> = { item ->
    val res = onUnlockOrEquipCosmeticItem(item)
    syncEquippedCosmeticsNow()
    res
  }
  val triggerHubGuidedTourOrFlight: () -> Unit = {
    if (isInMatch) {
      onOpenHowToPlayTutorial()
    } else {
      hubTutorialStep = 1
    }
  }
  LaunchedEffect(hubTutorialStep) {
    when (hubTutorialStep) {
      1 -> {
        onChangeOverwatchPrompt(OverwatchCircularPrompt.None)
        onChangeSideBayMode(RiftHubSideBayMode.HUB_MAIN)
      }
      2 -> {
        onChangeSideBayMode(RiftHubSideBayMode.CONDUIT_BAY)
        onChangeOverwatchPrompt(OverwatchCircularPrompt.ResonanceLeaderboard)
      }
      3 -> {
        onChangeSideBayMode(RiftHubSideBayMode.HANGAR_BAY)
        onChangeOverwatchPrompt(OverwatchCircularPrompt.ShipInspect(activeShipId.coerceIn(1, 10)))
      }
      4 -> {
        onChangeOverwatchPrompt(OverwatchCircularPrompt.None)
        onChangeSideBayMode(RiftHubSideBayMode.RIFT_FRAME_BAY)
      }
      5 -> {
        onChangeOverwatchPrompt(OverwatchCircularPrompt.None)
        onChangeSideBayMode(RiftHubSideBayMode.SQUADRON_BAY)
      }
      6 -> {
        onChangeOverwatchPrompt(OverwatchCircularPrompt.None)
        onChangeSideBayMode(RiftHubSideBayMode.GAME_MODES_BAY)
      }
    }
  }

  // Live 1-second ticker for the 1-hour public match reset / active match countdown
  val nowEpochMs by
    produceState(initialValue = System.currentTimeMillis(), key1 = headless) {
      if (!headless) {
        while (true) {
          kotlinx.coroutines.delay(1000L)
          value = System.currentTimeMillis()
        }
      }
    }

  // Continuous 2.5D ship rotation + orbiting Flux icon phase shared across Hangar Bay, Circular Prompt & Rift Hub
  val shipSpinDeg =
    if (headless) {
      -28f
    } else {
      val transition = rememberInfiniteTransition(label = "hub_25d_ship_spin")
      val deg by
        transition.animateFloat(
          initialValue = 0f,
          targetValue = 360f,
          animationSpec =
            infiniteRepeatable(
              animation = tween(durationMillis = 14000, easing = LinearEasing),
              repeatMode = RepeatMode.Restart,
            ),
          label = "hub_25d_ship_spin_deg",
        )
      deg
    }
  val fluxOrbitRad =
    if (headless) {
      0.85f
    } else {
      val transition = rememberInfiniteTransition(label = "hub_flux_orbit")
      val rad by
        transition.animateFloat(
          initialValue = 0f,
          targetValue = (2.0 * PI).toFloat(),
          animationSpec =
            infiniteRepeatable(
              animation = tween(durationMillis = 4800, easing = LinearEasing),
              repeatMode = RepeatMode.Restart,
            ),
          label = "hub_flux_orbit_rad",
        )
      rad
    }

  // Digitizing laser scanline sweep whenever side-bay mode dissolves
  val digitizeScanProgress = remember { Animatable(1f) }
  LaunchedEffect(sideBayMode) {
    if (!headless) {
      digitizeScanProgress.snapTo(0f)
      digitizeScanProgress.animateTo(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 360, easing = FastOutSlowInEasing),
      )
    }
  }

  val shardText =
    if (RiftBillingManager.TESTER_INFINITE_SHARDS_ACTIVE) "999,999"
    else String.format(Locale.US, "%,d", (riftShards + liveShardsBonus).coerceAtLeast(0L))
  val formattedLiveResonance =
    remember(liveResonance) { String.format(Locale.US, "%,d", liveResonance) }
  val cleanHubSynTag = syndicateTag.trim().uppercase(Locale.US)
  val hasRealHubSyndicate =
    cleanHubSynTag.isNotEmpty() && cleanHubSynTag !in LocalPilotPrefs.NON_PLAYER_SYNDICATE_TAGS
  val pilotIdentityLabel =
    if (hasRealHubSyndicate) "[$cleanHubSynTag] $callsign" else callsign
  val cleanSyndicateDisplayTitle =
    if (!hasRealHubSyndicate) {
      "UNAFFILIATED"
    } else {
      syndicateName.trim().takeIf {
        it.isNotBlank() &&
          !it.equals("GUEST", ignoreCase = true) &&
          !it.equals("NONE", ignoreCase = true) &&
          !it.equals("UNAFFILIATED", ignoreCase = true) &&
          !it.contains("Drifter", ignoreCase = true)
      } ?: "[$cleanHubSynTag]"
    }

  val customShipRev = RiftSpriteBaker.customShip9Version
  val shipResonanceMap =
    remember(currentUserId, liveResonance, chargedShipId, sideBayMode) {
      localPrefs.getAllShipResonances(currentUserId.ifBlank { null })
    }
  fun resolveLocalShipResonance(shipId: Int): Int {
    val clamped = shipId.coerceIn(1, 10)
    val fromMap = shipResonanceMap[clamped] ?: 0
    val fromEngine = engine?.getShipResonance(clamped) ?: 0
    return maxOf(fromMap, fromEngine)
  }

  val hasAnyShipResonance =
    liveResonance > 0 || (1..10).any { resolveLocalShipResonance(it) > 0 }
  val clampedShipId = activeShipId.coerceIn(1, 10)
  val resolvedBestShipId =
    if (!hasAnyShipResonance || bestShipId !in 1..10) 0 else bestShipId
  val hasChargedShip = chargedShipId in 1..10
  val isHangarSelected =
    hasChargedShip ||
      activeShipSummary.startsWith("CHARGED|", ignoreCase = true) ||
      activeShipSummary.startsWith("LOCKED|", ignoreCase = true) ||
      engine?.isShipLockedForMatch == true ||
      resolvedBestShipId == 0 ||
      clampedShipId != resolvedBestShipId
  val hangarIndicatedShipId =
    when {
      hasChargedShip -> chargedShipId
      isHangarSelected -> clampedShipId
      else -> resolvedBestShipId.coerceIn(1, 10)
    }

  val indicatedShipSpec =
    remember(hangarIndicatedShipId, customShipRev) { RiftCodex.getShipById(hangarIndicatedShipId) }
  val hangarPrimaryStat =
    remember(hangarIndicatedShipId, indicatedShipSpec.name) {
      if (hangarIndicatedShipId == 10) {
        "RIFT FRAME"
      } else {
        indicatedShipSpec.name.replace(Regex("^[0-9]+-"), "").trim().uppercase(Locale.US)
      }
    }
  val hangarBadgeText =
    when {
      hasChargedShip || isHangarSelected || resolvedBestShipId == 0 -> "CHARGED"
      else -> "BEST"
    }

  // Enriched Online Pilot Directory for Squadron Bay (ALL / SYNDICATE / FRIENDS)
  val friendLookupSet =
    remember(sideBayMode, currentUserId, onlineHumanPilots.size) {
      localPrefs
        .getFriendsList()
        .flatMap { listOf(it.uid.trim().lowercase(Locale.US), it.callsign.trim().lowercase(Locale.US)) }
        .filter { it.isNotBlank() }
        .toSet()
    }
  val squadronUidSet =
    remember(armadaSlots) {
      armadaSlots
        .flatMap { listOf(it.uid.trim().lowercase(Locale.US), it.callsign.trim().lowercase(Locale.US)) }
        .filter { it.isNotBlank() }
        .toSet()
    }
  val allSquadronOnlinePilots =
    remember(onlineHumanPilots, currentUserId, syndicateTag, friendLookupSet, squadronUidSet) {
      val seen = HashSet<String>()
      val items = ArrayList<SquadronOnlinePilotItem>()
      for (p in onlineHumanPilots) {
        val uid = p.userId.ifBlank { p.id }.trim()
        if (uid.isBlank() || uid == currentUserId) continue
        if (!seen.add(uid.lowercase(Locale.US))) continue
        val lockedHull = p.shipId.toInt().coerceIn(1, 10)
        val bestHull = onResolvePilotBestShipId(uid, lockedHull).coerceIn(1, 10)
        val cleanTag =
          p.syndicateTag
            .trim()
            .uppercase(Locale.US)
            .takeIf { it !in setOf("GUEST", "RFT", "NONE", "SOLO", "UNAFFILIATED", "FREE", "BOT") }
            .orEmpty()
        val pRes = (if (p.liveResonance > 0L) p.liveResonance else p.bounty).toInt().coerceAtLeast(0)
        val normCall = p.callsign.trim().lowercase(Locale.US)
        val isFriend = uid.lowercase(Locale.US) in friendLookupSet || normCall in friendLookupSet
        val isSquad = uid.lowercase(Locale.US) in squadronUidSet || normCall in squadronUidSet
        items.add(
          SquadronOnlinePilotItem(
            uid = uid,
            callsign = p.callsign.ifBlank { "PILOT" },
            syndicateTag = cleanTag,
            bestShipId = bestHull,
            lockedShipId = lockedHull,
            resonance = pRes,
            sectorLabel = p.sectorId.replace("_", " ").uppercase(Locale.US),
            isFriend = isFriend,
            isInSquadron = isSquad,
            rawLivePilot = p,
          )
        )
      }
      items.sortedWith(compareByDescending<SquadronOnlinePilotItem> { it.isInSquadron }.thenByDescending { it.resonance })
    }
  val filteredSquadronPilots =
    remember(allSquadronOnlinePilots, squadronFilter, squadronSort, syndicateTag) {
      val base =
        when (squadronFilter) {
          SquadronPilotFilter.ALL -> allSquadronOnlinePilots
          SquadronPilotFilter.SYNDICATE ->
            if (syndicateTag.isBlank()) emptyList()
            else allSquadronOnlinePilots.filter { it.syndicateTag.equals(syndicateTag, ignoreCase = true) }
          SquadronPilotFilter.FRIENDS -> allSquadronOnlinePilots.filter { it.isFriend }
        }
      when (squadronSort) {
        SquadronPilotSort.RESONANCE ->
          base.sortedWith(
            compareByDescending<SquadronOnlinePilotItem> { it.isInSquadron }
              .thenByDescending { it.resonance }
          )
        SquadronPilotSort.ACTIVITY ->
          base.sortedWith(
            compareByDescending<SquadronOnlinePilotItem> { it.isInSquadron }
              .thenByDescending {
                val sec = it.sectorLabel.trim().uppercase(Locale.US)
                sec.isNotBlank() && sec != "RIFT HUB" && sec != "ORBIT" && sec != "NEXUS ORBIT"
              }
              .thenBy { it.sectorLabel }
              .thenByDescending { it.resonance }
          )
      }
    }
  val overwatchMatchedPilots =
    remember(
      allSquadronOnlinePilots,
      activeOverwatchInstance.key,
      isInMatch,
      engine?.hasJoinedMatch,
      callsign,
      syndicateTag,
      hangarIndicatedShipId,
      resolvedBestShipId,
      liveResonance,
      currentUserId,
    ) {
      val normTarget = activeOverwatchInstance.key.trim().uppercase(Locale.US)
      val normTargetSpaced = normTarget.replace("_", " ")
      val remotes =
        allSquadronOnlinePilots.filter { p ->
          val rawSec = p.rawLivePilot.sectorId.trim().uppercase(Locale.US)
          rawSec == normTarget ||
            p.sectorLabel == normTargetSpaced ||
            (normTarget == "SECTOR_CONVERGENCE" && (rawSec == "SECTOR_CONVERGENCE" || rawSec == "CONVERGENCE"))
        }
      val selfInThisRift =
        engine?.hasJoinedMatch == true &&
          !engine.isEphemeralPracticeRift &&
          engine.activeMatchSectorTag().equals(activeOverwatchInstance.key, ignoreCase = true)
      if (selfInThisRift) {
        val selfEntry =
          SquadronOnlinePilotItem(
            uid = currentUserId.ifBlank { "local_self" },
            callsign = callsign,
            syndicateTag = syndicateTag,
            bestShipId = resolvedBestShipId,
            lockedShipId = hangarIndicatedShipId,
            resonance = liveResonance.coerceAtLeast(0),
            sectorLabel = activeOverwatchInstance.modeTitle.uppercase(Locale.US),
            isFriend = false,
            isInSquadron = true,
            rawLivePilot =
              LivePilotState(
                id = currentUserId.ifBlank { "local_self" },
                userId = currentUserId.ifBlank { "local_self" },
                callsign = callsign,
                syndicateTag = syndicateTag,
                sectorId = activeOverwatchInstance.key,
                shipId = hangarIndicatedShipId.toLong(),
                liveResonance = liveResonance.toLong().coerceAtLeast(0L),
                bounty = liveResonance.toLong().coerceAtLeast(0L),
              ),
          )
        listOf(selfEntry) + remotes
      } else {
        remotes
      }
    }
  val activeInspectedPilot: SquadronOnlinePilotItem? =
    remember(overwatchPrompt, selectedSquadronPilotUid, filteredSquadronPilots, allSquadronOnlinePilots) {
      when (val p = overwatchPrompt) {
        is OverwatchCircularPrompt.PilotInspect -> p.pilot
        else ->
          allSquadronOnlinePilots.firstOrNull { it.uid == selectedSquadronPilotUid }
            ?: filteredSquadronPilots.firstOrNull()
            ?: allSquadronOnlinePilots.firstOrNull()
      }
    }

  val conduitTier =
    remember(liveResonance) { RiftProgressionCatalog.getConduitTierForResonance(liveResonance) }
  val directives =
    remember(localPrefs, currentUserId, liveResonance) {
      localPrefs.getDailyDirectives(currentUserId.ifBlank { null })
    }
  val claimableDirectives = directives.count { it.isComplete && !it.claimed }
  val completedDirectives = directives.count { it.isComplete }
  val conduitBadge =
    if (claimableDirectives > 0) "$claimableDirectives CLAIM"
    else "$completedDirectives/${directives.size.coerceAtLeast(1)} QUESTS"
  val conduitPrimaryStat = conduitTier.title.uppercase(Locale.US)

  val peakResonance =
    maxOf(liveResonance, localPrefs.getPeakResonance(currentUserId.ifBlank { null }))

  val syncHealthy =
    isNetworkOnline &&
      (!isGoogleAuthenticated || cloudSyncHealth.lastError.isNullOrBlank() || cloudSyncHealth.isConnected)
  val onlineShortLabel =
    when {
      !isNetworkOnline -> "OFFLINE"
      !isGoogleAuthenticated -> "LOCAL"
      !syncHealthy -> "SYNC $totalOnlinePilots"
      else -> "ONLINE $totalOnlinePilots"
    }
  val onlineAccent =
    when {
      !isNetworkOnline -> Color(0xFFFB7185)
      isGoogleAuthenticated && syncHealthy -> Color(0xFF00F5D4)
      else -> Color(0xFFFACC15)
    }

  val liveRemainingSec =
    remember(activeOverwatchInstance, nowEpochMs, engine?.hasJoinedMatch, engine?.publicMatchDurationSec) {
      val cfg = activeOverwatchInstance.customConfig
      when {
        engine?.hasJoinedMatch == true &&
          engine.activeMatchSectorTag().equals(activeOverwatchInstance.key, ignoreCase = true) ->
          (engine.publicMatchCycleLimitSec - engine.publicMatchDurationSec).roundToInt().coerceAtLeast(0)
        cfg != null ->
          if (cfg.matchDurationMinutes <= 0) 3600 else cfg.matchDurationMinutes.coerceIn(1, 120) * 60
        activeOverwatchInstance.remainingSeconds in 1..3600 && headless ->
          activeOverwatchInstance.remainingSeconds
        else -> (3600 - ((nowEpochMs / 1000L) % 3600L).toInt()).coerceIn(1, 3600)
      }
    }
  val liveAvgResonance =
    remember(activeOverwatchInstance) {
      when {
        activeOverwatchInstance.avgResonance > 0 -> activeOverwatchInstance.avgResonance
        activeOverwatchInstance.pilotCount > 0 ->
          (activeOverwatchInstance.combinedResonance / activeOverwatchInstance.pilotCount).coerceAtLeast(0)
        else -> activeOverwatchInstance.combinedResonance.coerceAtLeast(0)
      }
    }

  BoxWithConstraints(
    modifier = Modifier.fillMaxSize(),
  ) {
    val safeW = maxWidth
    val safeH = maxHeight
    val aspect = safeW.value / safeH.value.coerceAtLeast(1f)
    val isTabletDeck = safeH >= 480.dp || aspect < 1.76f
    val outerPadH = 4.dp
    val wingStackGap = 5.dp
    val minPodH = minOf(74.dp, (safeH * 0.215f).coerceAtLeast(52.dp))
    val cornerPodHeight = (safeH * 0.215f).coerceIn(minPodH, 86.dp)
    val bayHeaderHeight = 48.dp

    // Capped Overwatch Stage Diameter (`minOf(safeH * 0.94f, safeW * 0.48f)`) so left/right wings never narrow below 25% viewport width
    val minWingReserveTotal = 280.dp
    val overwatchStageDiameterDp =
      minOf(safeH * 0.94f, safeW * 0.48f, (safeW - minWingReserveTotal).coerceAtLeast(156.dp))
    val sideSpanEachDp = ((safeW - overwatchStageDiameterDp) * 0.5f - outerPadH).coerceAtLeast(128.dp)
    val wingWidthDp = sideSpanEachDp
    val openCornerAnchorDp = outerPadH + wingWidthDp + 4.dp
    val openCornerMaxWidthDp =
      ((safeW * 0.5f) - openCornerAnchorDp - overwatchStageDiameterDp * 0.08f).coerceIn(112.dp, 224.dp)

    var stageRootOrigin by remember { mutableStateOf(Offset.Zero) }
    var activeDraggedFrameItem by remember { mutableStateOf<DraggedRiftFrameItem?>(null) }
    var dragPointerRootOffset by remember { mutableStateOf(Offset.Zero) }
    var primarySocketRootBounds by remember { mutableStateOf(Rect.Zero) }
    var secondarySocketRootBounds by remember { mutableStateOf(Rect.Zero) }
    var apertureCenterRoot by remember { mutableStateOf(Offset.Zero) }
    var apertureRadiusPx by remember { mutableFloatStateOf(0f) }
    var frameRenameDraft by
      remember(activeCustomSlotId, activeCustomSlot.customName) {
        mutableStateOf(activeCustomSlot.customName.ifBlank { activeCustomSlotId })
      }
    val effectiveOverwatchPrompt =
      if (overwatchPrompt == OverwatchCircularPrompt.None && sideBayMode == RiftHubSideBayMode.RIFT_FRAME_BAY) {
        OverwatchCircularPrompt.RiftFrameDrydock
      } else {
        overwatchPrompt
      }

    Box(
      modifier =
        Modifier.fillMaxSize()
          .onGloballyPositioned { coords ->
            stageRootOrigin = coords.positionInRoot()
          },
    ) {
      // 1. CIRCULAR OVERWATCH CENTERPIECE
      val centerFocused = focusedOrbitalIndex == 0 || hubTutorialStep in listOf(1, 2, 4)
      val tutorialPulseAlpha = 0.55f + 0.45f * (0.5f + 0.5f * kotlin.math.sin(fluxOrbitRad * 2f))
      RoundRipplingOverwatchAperture(
        modifier =
          Modifier.align(Alignment.Center)
            .size(overwatchStageDiameterDp)
            .then(
              if (hubTutorialStep in listOf(1, 2, 4)) {
                Modifier.border(
                  2.dp,
                  Color(0xFFFFD700).copy(alpha = tutorialPulseAlpha),
                  CircleShape,
                )
              } else Modifier
            ),
        isFocused = centerFocused,
        isTabletDeck = isTabletDeck,
        drawHeadlessSafeZoneFallback = headless,
        activeInstance = activeOverwatchInstance,
        overwatchPrompt = effectiveOverwatchPrompt,
        chargedShipId = chargedShipId,
        bestShipId = resolvedBestShipId,
        activeShipId = hangarIndicatedShipId,
        activeCustomSlot = activeCustomSlot,
        activeCustomSlotId = activeCustomSlotId,
        activeCustomSlotName = activeCustomSlot.customName,
        pointBudget = pointBudget.coerceAtLeast(100),
        activeDraggedFrameItem = activeDraggedFrameItem,
        dragPointerRootOffset = dragPointerRootOffset,
        frameRenameDraft = frameRenameDraft,
        onFrameRenameDraftChange = { frameRenameDraft = it },
        currentUserId = currentUserId,
        callsign = callsign,
        syndicateTag = syndicateTag,
        liveResonance = liveResonance,
        cloudLeaderboardEntries = cloudLeaderboardEntries,
        onlineHumanPilots = onlineHumanPilots,
        syndicateDirectory = syndicateDirectory,
        shipSpinDeg = shipSpinDeg,
        fluxOrbitRad = fluxOrbitRad,
        renameDraft = renameDraftState,
        onRenameDraftChange = { renameDraftState = it },
        resolveShipResonance = { resolveLocalShipResonance(it) },
        onChargeShip = { chosenShip ->
          onChargeShip(chosenShip)
          onChangeOverwatchPrompt(OverwatchCircularPrompt.None)
        },
        onCustomizeRiftFrame = {
          onChangeOverwatchPrompt(OverwatchCircularPrompt.None)
          onChangeSideBayMode(RiftHubSideBayMode.RIFT_FRAME_BAY)
        },
        onSaveCustomSlot = onSaveCustomSlot,
        onOpenFrameRenamePrompt = {
          frameRenameDraft = activeCustomSlot.customName.ifBlank { activeCustomSlotId }
          onChangeOverwatchPrompt(OverwatchCircularPrompt.FrameSlotRename(activeCustomSlotId, frameRenameDraft))
        },
        onSaveFrameSlotRename = { rawName ->
          val clean = rawName.trim().take(18)
          if (clean.isNotBlank()) {
            onSaveCustomSlot(
              activeCustomSlot
                .copy(slotId = activeCustomSlotId, customName = clean)
                .clampToPointBudget(pointBudget.coerceAtLeast(100))
            )
            onChangeOverwatchPrompt(OverwatchCircularPrompt.None)
          }
        },
        onUpdatePrimarySocketBounds = { primarySocketRootBounds = it },
        onUpdateSecondarySocketBounds = { secondarySocketRootBounds = it },
        onDismissPrompt = { onChangeOverwatchPrompt(OverwatchCircularPrompt.None) },
        onSquadUpPilot = { pilotItem ->
          onInviteOnlinePilotToWing(pilotItem.rawLivePilot)
        },
        onAddFriendPilot = { pilotItem ->
          onAddOnlinePilotFriend(pilotItem.callsign)
        },
        onConfirmActionPrompt = { confirmPrompt ->
          confirmPrompt.onConfirm()
          onChangeOverwatchPrompt(OverwatchCircularPrompt.None)
        },
        onExecuteInfoStageAction = { infoPrompt ->
          infoPrompt.onPrimaryAction?.invoke()
          onChangeOverwatchPrompt(OverwatchCircularPrompt.None)
        },
        onSaveCallsignRename = { rawDraft ->
          val clean = rawDraft.trim().uppercase(Locale.US).replace(Regex("[^A-Z0-9_\\-]"), "").take(14)
          if (clean.length >= 2) {
            onRenamePilotCallsign(clean)
            onChangeOverwatchPrompt(OverwatchCircularPrompt.None)
          }
        },
        onOverwatchTransformGesture = onOverwatchTransformGesture,
        onResetOverwatchCamera = onResetOverwatchCamera,
        onApertureRootBoundsChanged = { cx, cy, rPx ->
          apertureCenterRoot = Offset(cx, cy)
          apertureRadiusPx = rPx
          onApertureRootBoundsChanged(cx, cy, rPx)
        },
      )

      // 2. PORT FLANKING HARDWARE WING (All Non-Full-Screen Windows Digitize Into Flanking Side-Bays)
      HubDigitizingScanlineBox(
        modifier =
          Modifier.align(Alignment.CenterStart)
            .padding(start = outerPadH, top = 2.dp, bottom = 2.dp)
            .width(wingWidthDp)
            .fillMaxHeight()
            .then(
              if (hubTutorialStep in listOf(2, 3, 4, 6)) {
                Modifier.border(
                  1.5.dp,
                  Color(0xFFFFD700).copy(alpha = tutorialPulseAlpha),
                  RoundedCornerShape(10.dp),
                )
              } else Modifier
            ),
        scanProgress = digitizeScanProgress.value,
        isRightSide = false,
      ) {
        AnimatedContent(
          targetState = sideBayMode,
          modifier = Modifier.fillMaxSize(),
          transitionSpec = {
            (fadeIn(tween(220, easing = FastOutSlowInEasing)) +
                scaleIn(initialScale = 0.95f, animationSpec = tween(220, easing = FastOutSlowInEasing)))
              .togetherWith(
                fadeOut(tween(160, easing = FastOutSlowInEasing)) +
                  scaleOut(targetScale = 0.96f, animationSpec = tween(160, easing = FastOutSlowInEasing))
              )
              .using(SizeTransform(clip = false))
          },
          label = "port_wing_digitize",
        ) { mode ->
          when (mode) {
            RiftHubSideBayMode.HUB_MAIN -> {
              Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(wingStackGap),
              ) {
                PortTopPilotPod(
                  heightDp = cornerPodHeight,
                  isTabletDeck = isTabletDeck,
                  pilotIdentityLabel = pilotIdentityLabel,
                  conduitRankLabel = conduitPrimaryStat,
                  isGoogleAuthenticated = isGoogleAuthenticated,
                  isStreaming = isStreaming,
                  onClickCallsign = onClickCallsign,
                  onSwitchAccount = onSwitchAccount,
                )

                RiftVerticalCard(
                  modifier = Modifier.weight(1f).fillMaxWidth(),
                  title = "HANGAR DECK",
                  subtitle = "Choose ships and Rift Frame",
                  accent = if (hasChargedShip) Color(0xFF00F5D4) else Color(0xFF38BDF8),
                  secondaryAccent = if (hasChargedShip) Color(0xFF00F5D4) else Color(0xFFFFD700),
                  isRightColumn = false,
                  isHighlighted = focusedOrbitalIndex == 1 || hasChargedShip,
                  badgeText = "$hangarPrimaryStat • $hangarBadgeText",
                  badgeColor = if (hasChargedShip) Color(0xFF00F5D4) else Color(0xFFFACC15),
                  isTabletDeck = isTabletDeck,
                  testTag = "nexus_node_hangar_deck",
                  visualContent = { sizeDp ->
                    Spinning25dShipWithOrbitingFlux(
                      shipId = hangarIndicatedShipId,
                      shipSpinDeg = shipSpinDeg,
                      fluxOrbitRad = fluxOrbitRad,
                      isCharged = hasChargedShip,
                      isBestShip = resolvedBestShipId in 1..10 && hangarIndicatedShipId == resolvedBestShipId,
                      isSelected = isHangarSelected,
                      sizeDp = sizeDp,
                    )
                  },
                  onClick = {
                    onChangeOverwatchPrompt(OverwatchCircularPrompt.None)
                    onChangeSideBayMode(RiftHubSideBayMode.HANGAR_BAY)
                  },
                )

                RiftVerticalCard(
                  modifier = Modifier.weight(1f).fillMaxWidth(),
                  title = "RESONANCE",
                  subtitle = "Leaderboards, ranks and quests",
                  accent = Color(0xFFFFD700),
                  secondaryAccent = Color(0xFF00F5D4),
                  isRightColumn = false,
                  isHighlighted = focusedOrbitalIndex == 2 || claimableDirectives > 0,
                  badgeText = "LIVE $formattedLiveResonance • $conduitPrimaryStat",
                  badgeColor = Color(0xFFFFD700),
                  showBadgeResonanceIcon = true,
                  isTabletDeck = isTabletDeck,
                  testTag = "nexus_node_resonance",
                  visualContent = { sizeDp ->
                    Box(
                      modifier =
                        Modifier.size(sizeDp)
                          .drawBehind {
                            val r = size.minDimension * 0.46f
                            drawCircle(
                              brush =
                                Brush.radialGradient(
                                  colors =
                                    listOf(
                                      Color(0xFFFFD700).copy(alpha = 0.28f),
                                      Color(0xFF00F5D4).copy(alpha = 0.12f),
                                      Color.Transparent,
                                    ),
                                  center = center,
                                  radius = r * 1.15f,
                                ),
                              radius = r * 1.15f,
                              center = center,
                            )
                            drawCircle(
                              brush =
                                Brush.sweepGradient(
                                  0.0f to Color(0xFFFFD700).copy(alpha = 0.85f),
                                  0.5f to Color(0xFF00F5D4).copy(alpha = 0.72f),
                                  1.0f to Color(0xFFFFD700).copy(alpha = 0.85f),
                                  center = center,
                                ),
                              radius = r,
                              center = center,
                              style = Stroke(width = 1.4.dp.toPx()),
                            )
                          },
                      contentAlignment = Alignment.Center,
                    ) {
                      ResonanceCrestIcon(sizeDp = (sizeDp * 0.64f).coerceAtLeast(24.dp))
                    }
                  },
                  onClick = {
                    onChangeSideBayMode(RiftHubSideBayMode.CONDUIT_BAY)
                    onChangeOverwatchPrompt(OverwatchCircularPrompt.ResonanceLeaderboard)
                  },
                )

                PortBottomTelemetryPod(
                  heightDp = cornerPodHeight,
                  isTabletDeck = isTabletDeck,
                  formattedLiveResonance = formattedLiveResonance,
                  conduitBadge = conduitBadge,
                  shardText = shardText,
                  onlineShortLabel = onlineShortLabel,
                  onlineAccent = onlineAccent,
                  onClickResonance = onClickResonance,
                  onClickShards = onClickShards,
                  onClickOnline = onClickOnline,
                )
              }
            }
            RiftHubSideBayMode.HANGAR_BAY -> {
              HangarVerticalWingBay(
                ships = remember(customShipRev) { RiftCodex.getAllTenShips().filter { it.id in 1..5 } },
                isRightSide = false,
                isTabletDeck = isTabletDeck,
                headerHeight = bayHeaderHeight,
                wingStackGap = wingStackGap,
                chargedShipId = chargedShipId,
                bestShipId = resolvedBestShipId,
                inspectedShipId = (overwatchPrompt as? OverwatchCircularPrompt.ShipInspect)?.shipId ?: 0,
                shipSpinDeg = shipSpinDeg,
                fluxOrbitRad = fluxOrbitRad,
                shipResonanceFor = { resolveLocalShipResonance(it) },
                onBackToHub = {
                  onChangeOverwatchPrompt(OverwatchCircularPrompt.None)
                  onChangeSideBayMode(RiftHubSideBayMode.HUB_MAIN)
                },
                onOpenStudio = {
                  onChangeOverwatchPrompt(OverwatchCircularPrompt.None)
                  onChangeSideBayMode(RiftHubSideBayMode.RIFT_FRAME_BAY)
                },
                onTapShip = { shipId ->
                  onChangeOverwatchPrompt(OverwatchCircularPrompt.ShipInspect(shipId))
                },
              )
            }
            RiftHubSideBayMode.RIFT_FRAME_BAY -> {
              RiftFrameLoadoutSideBay(
                isTabletDeck = isTabletDeck,
                headerHeight = bayHeaderHeight,
                wingStackGap = wingStackGap,
                activeCustomSlot = activeCustomSlot,
                activeCustomSlotId = activeCustomSlotId,
                liveResonance = liveResonance,
                pointBudget = pointBudget.coerceAtLeast(100),
                isCharged = chargedShipId == 10,
                localPrefs = localPrefs,
                currentUserId = currentUserId,
                onBackToHub = {
                  onChangeOverwatchPrompt(OverwatchCircularPrompt.None)
                  onChangeSideBayMode(RiftHubSideBayMode.HUB_MAIN)
                },
                onSelectFrameSlot = { slotId -> onUnlockOrSelectFrameSlotId(slotId) },
                onSaveCustomSlot = onSaveCustomSlot,
                onPreviewWeaponStage = { infoPrompt ->
                  onChangeOverwatchPrompt(infoPrompt)
                },
                onPreviewInOverwatch = {
                  onChangeOverwatchPrompt(OverwatchCircularPrompt.None)
                },
                onChargeRiftFrame = {
                  onChargeShip(10)
                  onChangeOverwatchPrompt(OverwatchCircularPrompt.None)
                  onChangeSideBayMode(RiftHubSideBayMode.HUB_MAIN)
                },
                onStartDragItem = { dragged, startRoot ->
                  onChangeOverwatchPrompt(OverwatchCircularPrompt.None)
                  activeDraggedFrameItem = dragged
                  dragPointerRootOffset = startRoot
                },
                onUpdateDragRootOffset = { rootPos ->
                  dragPointerRootOffset = rootPos
                },
                onEndDragItem = {
                  val dragged = activeDraggedFrameItem
                  if (dragged != null) {
                    val insideAperture =
                      apertureRadiusPx > 0f &&
                        (dragPointerRootOffset - apertureCenterRoot).getDistance() <= apertureRadiusPx * 1.05f
                    val budget = pointBudget.coerceAtLeast(100)
                    if (dragged.isPrimary) {
                      if (primarySocketRootBounds.inflate(32f).contains(dragPointerRootOffset) || insideAperture) {
                        onSaveCustomSlot(
                          activeCustomSlot
                            .copy(slotId = activeCustomSlotId, primaryArchetype = dragged.itemId)
                            .clampToPointBudget(budget)
                        )
                      }
                    } else {
                      if (secondarySocketRootBounds.inflate(32f).contains(dragPointerRootOffset) || insideAperture) {
                        onSaveCustomSlot(
                          activeCustomSlot
                            .copy(slotId = activeCustomSlotId, secondaryType = dragged.itemId)
                            .clampToPointBudget(budget)
                        )
                      }
                    }
                  }
                  activeDraggedFrameItem = null
                },
                onOpenFrameRenamePrompt = {
                  frameRenameDraft = activeCustomSlot.customName.ifBlank { activeCustomSlotId }
                  onChangeOverwatchPrompt(
                    OverwatchCircularPrompt.FrameSlotRename(activeCustomSlotId, frameRenameDraft)
                  )
                },
              )
            }
            RiftHubSideBayMode.SQUADRON_BAY -> {
              SquadronLeftPilotListBay(
                isTabletDeck = isTabletDeck,
                headerHeight = bayHeaderHeight,
                wingStackGap = wingStackGap,
                filter = squadronFilter,
                onSelectFilter = onSelectSquadronFilter,
                sortMode = squadronSort,
                onToggleSort = onToggleSquadronSort,
                pilots = filteredSquadronPilots,
                selectedPilotUid = activeInspectedPilot?.uid.orEmpty(),
                squadronStatusBanner = squadronStatusBanner,
                onBackToHub = {
                  onChangeOverwatchPrompt(OverwatchCircularPrompt.None)
                  onChangeSideBayMode(RiftHubSideBayMode.HUB_MAIN)
                },
                onBroadcastLfg = onPostStrikeWingInvite,
                onAddFriendCallsign = onAddOnlinePilotFriend,
                onTapPilot = { pilotItem ->
                  onSelectSquadronPilotUid(pilotItem.uid)
                  onChangeOverwatchPrompt(OverwatchCircularPrompt.PilotInspect(pilotItem))
                },
              )
            }
            RiftHubSideBayMode.SYNDICATE_BAY -> {
              SyndicateLeftDirectoryBay(
                isTabletDeck = isTabletDeck,
                headerHeight = bayHeaderHeight,
                wingStackGap = wingStackGap,
                currentTag = syndicateTag,
                syndicateDirectory = syndicateDirectory,
                syndicateWarRecord = syndicateWarRecord,
                statusMessage = syndicateStatusMessage,
                onBackToHub = {
                  onChangeOverwatchPrompt(OverwatchCircularPrompt.None)
                  onChangeSideBayMode(RiftHubSideBayMode.HUB_MAIN)
                },
                onInspectSyndicate = { syn ->
                  val isCurrent = syndicateTag.isNotBlank() && syn.tag.equals(syndicateTag, ignoreCase = true)
                  onChangeOverwatchPrompt(
                    OverwatchCircularPrompt.ActionConfirm(
                      kicker = "SYNDICATE CITADEL",
                      title = "[${syn.tag.uppercase(Locale.US)}] ${syn.name.uppercase(Locale.US)}",
                      subtitle =
                        "${syn.memberCount.coerceAtLeast(1)} PILOTS • ${String.format(Locale.US, "%,d", syn.combinedBounty.coerceAtLeast(0L))} RESONANCE\n${syn.motto.ifBlank { "Sector Syndicate" }}",
                      confirmLabel = if (isCurrent) "LEAVE" else "JOIN",
                      accentHex = if (isCurrent) 0xFFFB7185L else 0xFFFFD700L,
                      onConfirm = {
                        if (isCurrent) onLeaveCurrentSyndicate() else onJoinOfficialSyndicate(syn)
                      },
                    )
                  )
                },
              )
            }
            RiftHubSideBayMode.GAME_MODES_BAY -> {
              GameModesLeftRankedBay(
                isTabletDeck = isTabletDeck,
                headerHeight = bayHeaderHeight,
                wingStackGap = wingStackGap,
                activeRankedMode = activeRankedMode,
                skirmishBracket = skirmishBracket,
                onSelectSkirmishBracket = onSelectSkirmishBracket,
                riftControlBracket = riftControlBracket,
                onSelectRiftControlBracket = onSelectRiftControlBracket,
                syndicateWarBracket = syndicateWarBracket,
                onSelectSyndicateWarBracket = onSelectSyndicateWarBracket,
                humanPilotCountByMode = humanPilotCountByMode,
                canQueueSyndicateWars = canQueueSyndicateWars,
                overwatchInstances = overwatchInstances,
                onSelectOverwatchKey = onSelectOverwatchKey,
                onBackToHub = {
                  onChangeOverwatchPrompt(OverwatchCircularPrompt.None)
                  onChangeSideBayMode(RiftHubSideBayMode.HUB_MAIN)
                },
                onInspectMode = { mode, bracket, canDeploy ->
                  val modeSummary =
                    when (mode) {
                      RankedGameMode.SECTOR_CONVERGENCE ->
                        "Open free-for-all sector dogfighting anchored by the central 5x Rift Station Safe Harbor. Build per-ship and overall Resonance across 60-minute sector cycles."
                      RankedGameMode.FLUX_CORE ->
                        "Capture and hold the golden Flux Core Singularity Orb in the center of the sector to earn continuous Resonance while defending against rival teams."
                      RankedGameMode.RIFT_SKIRMISH ->
                        "${bracket}v${bracket} Squadron team battles matched against the closest-Resonance opponent squad within a strict 15-second matchmaking window."
                      RankedGameMode.RIFT_CONTROL ->
                        "${bracket}v${bracket} dual-ring territory control over Alpha and Omega beacons. No Safe Zone is active—rotations and stacked Barriers decide victory."
                      RankedGameMode.STASIS_BREACH ->
                        "Eliminated pilots are detained in the rival Brig Reactor. Crack the enemy Brig shield to trigger a team-wide prison break or sweep all opposing pilots."
                      RankedGameMode.SYNDICATE_WARS ->
                        "${bracket}v${bracket} chartered Syndicate warfare for official war record standings and sector dominance."
                    }
                  onChangeOverwatchPrompt(
                    OverwatchCircularPrompt.InfoStageInspect(
                      kicker = "RANKED GAME MODE",
                      title = mode.title.uppercase(Locale.US),
                      badge = if (bracket in 2..10) "${bracket}V${bracket}" else "OPEN SECTOR",
                      summary = modeSummary,
                      tacticalNote =
                        if (!canDeploy) canQueueSyndicateWars.second
                        else "Use the top-right RIFT IN > control to launch immediately with 100-Point hull parity.",
                      accentHex = 0xFF00F5D4L,
                      showStationPreview =
                        mode == RankedGameMode.SECTOR_CONVERGENCE || mode == RankedGameMode.FLUX_CORE,
                      codexModeIndex = mode.ordinal,
                      primaryActionLabel = "",
                      onPrimaryAction = null,
                    )
                  )
                },
              )
            }
            RiftHubSideBayMode.CONDUIT_BAY -> {
              ConduitLeftDirectivesBay(
                isTabletDeck = isTabletDeck,
                headerHeight = bayHeaderHeight,
                wingStackGap = wingStackGap,
                liveResonance = liveResonance,
                conduitTier = conduitTier,
                directives = directives,
                isSweepClaimed = localPrefs.isDailySweepBonusClaimed(currentUserId.ifBlank { null }),
                onBackToHub = {
                  onChangeOverwatchPrompt(OverwatchCircularPrompt.None)
                  onChangeSideBayMode(RiftHubSideBayMode.HUB_MAIN)
                },
                onOpenLeaderboards = {
                  onChangeOverwatchPrompt(OverwatchCircularPrompt.ResonanceLeaderboard)
                },
                onClaimDirective = onClaimDailyDirectiveSlot,
                onRerollDirective = onRerollDailyDirectiveSlot,
                onClaimSweep = onClaimDailySweepBonus,
              )
            }
            RiftHubSideBayMode.SHARD_STORE_BAY -> {
              ShardStoreLeftTreasuryBay(
                isTabletDeck = isTabletDeck,
                headerHeight = bayHeaderHeight,
                wingStackGap = wingStackGap,
                shardText = shardText,
                liveResonance = liveResonance,
                activeCustomSlotId = activeCustomSlotId,
                purchaseStatusMessage = storePurchaseBanner,
                localPrefs = localPrefs,
                currentUserId = currentUserId,
                onBackToHub = {
                  onChangeOverwatchPrompt(OverwatchCircularPrompt.None)
                  onChangeSideBayMode(RiftHubSideBayMode.HUB_MAIN)
                },
                onInspectFrameB = { gate, isOwned, isSelected ->
                  onChangeOverwatchPrompt(
                    OverwatchCircularPrompt.InfoStageInspect(
                      kicker = "PERMANENT SHARD STORE BAY",
                      title = "${gate.slotId.uppercase(Locale.US)} • ${gate.archetypeTitle.uppercase(Locale.US)}",
                      badge =
                        when {
                          isSelected -> "ACTIVE BAY"
                          isOwned -> "OWNED"
                          else -> "${gate.shardCost} SHARDS"
                        },
                      summary =
                        "Permanent extra 100-Point Rift Frame loadout bay that stays unlocked regardless of Live Resonance fluctuations.",
                      tacticalNote =
                        "Frame C, Frame D, and Frame E are earned exclusively through active Live Resonance.",
                      accentHex = if (isSelected) 0xFF00F5D4L else 0xFFC084FCL,
                      shipId = 10,
                      primaryActionLabel =
                        when {
                          isSelected -> "OPEN RIFT FRAME >"
                          isOwned -> "SELECT BAY"
                          else -> "BUY ${gate.shardCost}"
                        },
                      onPrimaryAction = {
                        if (isSelected) {
                          onChangeSideBayMode(RiftHubSideBayMode.RIFT_FRAME_BAY)
                        } else {
                          val res = onUnlockOrSelectFrameSlotId("Frame B")
                          storePurchaseBanner = res.second
                        }
                      },
                    )
                  )
                },
                onInspectPaidSkin = { skin, isOwned, isEquipped ->
                  val skinStyleId = skin.id.coerceIn(0, 5)
                  onChangeOverwatchPrompt(
                    OverwatchCircularPrompt.InfoStageInspect(
                      kicker = "PAID SHIP SKIN",
                      title = skin.name.uppercase(Locale.US),
                      badge =
                        when {
                          isEquipped -> "EQUIPPED"
                          isOwned -> "OWNED"
                          else -> "${skin.shardCost} SHARDS"
                        },
                      summary = skin.summary,
                      tacticalNote =
                        "Permanent hull livery across your Hangar fleet. Finishers, Warp Trails, and Halos remain strictly Ranked Live Resonance rewards.",
                      accentHex = if (isOwned) 0xFF00F5D4L else 0xFFFACC15L,
                      shipId = hangarIndicatedShipId,
                      previewHullSkinId = skinStyleId,
                      primaryActionLabel =
                        when {
                          isEquipped -> ""
                          isOwned -> "EQUIP"
                          else -> "BUY ${skin.shardCost}"
                        },
                      onPrimaryAction =
                        if (isEquipped) null
                        else ({
                          val res = handleUnlockOrEquipCosmetic(skin)
                          storePurchaseBanner = res.second
                          if (res.first) {
                            onChangeOverwatchPrompt(
                              OverwatchCircularPrompt.InfoStageInspect(
                                kicker = "PAID SHIP SKIN",
                                title = skin.name.uppercase(Locale.US),
                                badge = "EQUIPPED",
                                summary = skin.summary,
                                tacticalNote =
                                  "Permanent hull livery across your Hangar fleet. Finishers, Warp Trails, and Halos remain strictly Ranked Live Resonance rewards.",
                                accentHex = 0xFF00F5D4L,
                                shipId = hangarIndicatedShipId,
                                previewHullSkinId = skinStyleId,
                              )
                            )
                          }
                        }),
                    )
                  )
                },
                onOpenRiftFrameBay = {
                  onChangeOverwatchPrompt(OverwatchCircularPrompt.None)
                  onChangeSideBayMode(RiftHubSideBayMode.RIFT_FRAME_BAY)
                },
                onOpenConduitQuests = {
                  onChangeSideBayMode(RiftHubSideBayMode.CONDUIT_BAY)
                  onChangeOverwatchPrompt(OverwatchCircularPrompt.ResonanceLeaderboard)
                },
              )
            }
            RiftHubSideBayMode.DOSSIER_BAY -> {
              DossierLeftIdentityBay(
                isTabletDeck = isTabletDeck,
                headerHeight = bayHeaderHeight,
                wingStackGap = wingStackGap,
                callsign = callsign,
                syndicateTag = syndicateTag,
                liveResonance = liveResonance,
                peakResonance = peakResonance,
                localPrefs = localPrefs,
                currentUserId = currentUserId,
                dossierRefreshNonce = dossierRefreshNonce,
                onBackToHub = {
                  onChangeOverwatchPrompt(OverwatchCircularPrompt.None)
                  onChangeSideBayMode(RiftHubSideBayMode.HUB_MAIN)
                },
                onOpenRenamePrompt = {
                  renameDraftState = callsign
                  onChangeOverwatchPrompt(OverwatchCircularPrompt.CallsignRename(callsign))
                },
                onOpenResetPrompt = {
                  onChangeOverwatchPrompt(
                    OverwatchCircularPrompt.ActionConfirm(
                      kicker = "DESTRUCTIVE ACTION",
                      title = "RESET PILOT RECORD?",
                      subtitle = "Clears local match telemetry and career counters on this device.",
                      confirmLabel = "RESET",
                      accentHex = 0xFFFB7185L,
                      onConfirm = {
                        onResetLocalPilotTelemetry()
                        dossierRefreshNonce++
                      },
                    )
                  )
                },
                onOpenSwitchAccountPrompt = {
                  onChangeOverwatchPrompt(
                    OverwatchCircularPrompt.ActionConfirm(
                      kicker = "PILOT ACCOUNT",
                      title = "SWITCH ACCOUNT?",
                      subtitle = "Signs out of your current pilot session and opens the Rift sign-in screen.",
                      confirmLabel = "SWITCH",
                      accentHex = 0xFFFACC15L,
                      onConfirm = onSwitchAccount,
                    )
                  )
                },
                onOpenSurvivalSpreadsheet = onOpenSurvivalSpreadsheet,
              )
            }
            RiftHubSideBayMode.HELM_CODEX_BAY -> {
              HelmLeftSystemsBay(
                isTabletDeck = isTabletDeck,
                headerHeight = bayHeaderHeight,
                wingStackGap = wingStackGap,
                sfxEnabled = sfxEnabled,
                musicEnabled = musicEnabled,
                musicBpm = musicBpm,
                musicVolume = musicVolume,
                sfxVolume = sfxVolume,
                cameraZoom = cameraZoom,
                playfieldTextScale = playfieldTextScale,
                onToggleSfx = onToggleSfx,
                onToggleMusic = onToggleMusic,
                onChangeMusicBpm = onChangeMusicBpm,
                onChangeMusicVolume = onChangeMusicVolume,
                onChangeSfxVolume = onChangeSfxVolume,
                onChangeCameraZoom = onChangeCameraZoom,
                onChangePlayfieldTextScale = onChangePlayfieldTextScale,
                onOpenChangelogModal = onOpenChangelogModal,
                onOpenAudioSynthForge = onOpenAudioSynthForge,
                onOpenStarForge = onOpenShipwrightStudio,
                onStartTutorial = triggerHubGuidedTourOrFlight,
                isNetworkOnline = isNetworkOnline,
                isGoogleAuthenticated = isGoogleAuthenticated,
                cloudSyncHealth = cloudSyncHealth,
                updateStatusLabel = updateStatusLabel,
                hasUpdateAvailable = hasUpdateAvailable,
                onCheckOrApplyUpdate = onCheckOrApplyUpdate,
                onOpenSwitchAccountPrompt = {
                  onChangeOverwatchPrompt(
                    OverwatchCircularPrompt.ActionConfirm(
                      kicker = "PILOT ACCOUNT",
                      title = "SWITCH ACCOUNT?",
                      subtitle = "Signs out of your current pilot session and opens the Rift sign-in screen.",
                      confirmLabel = "SWITCH",
                      accentHex = 0xFFFACC15L,
                      onConfirm = onSwitchAccount,
                    )
                  )
                },
                onBackToHub = {
                  onChangeOverwatchPrompt(OverwatchCircularPrompt.None)
                  onChangeSideBayMode(RiftHubSideBayMode.HUB_MAIN)
                },
                onOpenSliderWorkbench = { wb ->
                  onChangeOverwatchPrompt(wb)
                },
              )
            }
            RiftHubSideBayMode.OVERWATCH_INSPECT_BAY -> {
              OverwatchInspectLeftInfoBay(
                isTabletDeck = isTabletDeck,
                headerHeight = bayHeaderHeight,
                wingStackGap = wingStackGap,
                activeInstance = activeOverwatchInstance,
                liveRemainingSec = liveRemainingSec,
                liveAvgResonance = liveAvgResonance,
                realHumanCount = maxOf(activeOverwatchInstance.pilotCount, overwatchMatchedPilots.size),
                onBackToHub = {
                  onChangeOverwatchPrompt(OverwatchCircularPrompt.None)
                  onChangeSideBayMode(RiftHubSideBayMode.HUB_MAIN)
                },
                onRiftIn = { onQuickZoneIntoOverwatch(activeOverwatchInstance) },
                onOpenActiveRifts = {
                  onChangeOverwatchPrompt(OverwatchCircularPrompt.None)
                  onChangeSideBayMode(RiftHubSideBayMode.GAME_MODES_BAY)
                },
              )
            }
          }
        }
      }

      // 3. STARBOARD FLANKING HARDWARE WING (All Non-Full-Screen Windows Digitize Into Flanking Side-Bays)
      HubDigitizingScanlineBox(
        modifier =
          Modifier.align(Alignment.CenterEnd)
            .padding(end = outerPadH, top = 2.dp, bottom = 2.dp)
            .width(wingWidthDp)
            .fillMaxHeight()
            .then(
              if (hubTutorialStep in listOf(3, 4, 5, 6)) {
                Modifier.border(
                  1.5.dp,
                  Color(0xFF00F5D4).copy(alpha = tutorialPulseAlpha),
                  RoundedCornerShape(10.dp),
                )
              } else Modifier
            ),
        scanProgress = digitizeScanProgress.value,
        isRightSide = true,
      ) {
        AnimatedContent(
          targetState = sideBayMode,
          modifier = Modifier.fillMaxSize(),
          transitionSpec = {
            (fadeIn(tween(220, easing = FastOutSlowInEasing)) +
                scaleIn(initialScale = 0.95f, animationSpec = tween(220, easing = FastOutSlowInEasing)))
              .togetherWith(
                fadeOut(tween(160, easing = FastOutSlowInEasing)) +
                  scaleOut(targetScale = 0.96f, animationSpec = tween(160, easing = FastOutSlowInEasing))
              )
              .using(SizeTransform(clip = false))
          },
          label = "starboard_wing_digitize",
        ) { mode ->
          when (mode) {
            RiftHubSideBayMode.HUB_MAIN -> {
              val activeRiftsLiveCount =
                remember(overwatchInstances, totalOnlinePilots) {
                  maxOf(overwatchInstances.sumOf { it.pilotCount }, totalOnlinePilots).coerceAtLeast(0)
                }
              Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(wingStackGap),
              ) {
                StarboardTopCommandPod(
                  heightDp = cornerPodHeight,
                  isTabletDeck = isTabletDeck,
                  isInMatch = isInMatch,
                  helmFocused = false,
                  onOpenHowToPlayTutorial = triggerHubGuidedTourOrFlight,
                  onOpenHelmSystems = { onSelectNode(7, NexusCoreNode.HELM_SYSTEMS) },
                  onLeaveMatch = onLeaveMatch,
                  onPrimaryRightAction = {
                    if (isInMatch) onCloseModal() else onEnterPracticeRift()
                  },
                )

                RiftVerticalCard(
                  modifier = Modifier.weight(1f).fillMaxWidth(),
                  title = "SUBSPACE COMMS",
                  subtitle = "Live chat, voice, AI",
                  accent = Color(0xFF00F5D4),
                  secondaryAccent = if (unreadCommsCount > 0) Color(0xFFFB7185) else Color(0xFF38BDF8),
                  isRightColumn = true,
                  isHighlighted = focusedOrbitalIndex == 3 || unreadCommsCount > 0,
                  badgeText = if (unreadCommsCount > 0) "$unreadCommsCount NEW" else null,
                  badgeColor = Color(0xFFFB7185),
                  isTabletDeck = isTabletDeck,
                  testTag = "nexus_node_subspace_comms",
                  visualContent = { sizeDp ->
                    SubspaceWaveformEmblem(
                      sizeDp = sizeDp,
                      hasUnread = unreadCommsCount > 0,
                    )
                  },
                  onClick = { onSelectNode(3, NexusCoreNode.SUBSPACE_COMMS) },
                )

                RiftVerticalCard(
                  modifier = Modifier.weight(1f).fillMaxWidth(),
                  title = "ACTIVE RIFTS",
                  subtitle = "Ranked modes, custom arenas",
                  accent = Color(0xFFFFD700),
                  secondaryAccent = Color(0xFF00F5D4),
                  isRightColumn = true,
                  isHighlighted = focusedOrbitalIndex == 4,
                  badgeText = if (activeRiftsLiveCount == 1) "1 PILOT" else "$activeRiftsLiveCount LIVE",
                  badgeColor = Color(0xFF00F5D4),
                  isTabletDeck = isTabletDeck,
                  testTag = "nexus_node_convergence_gate",
                  visualContent = { sizeDp ->
                    ActiveRiftsEmblem(sizeDp = sizeDp)
                  },
                  onClick = {
                    onChangeOverwatchPrompt(OverwatchCircularPrompt.None)
                    onChangeSideBayMode(RiftHubSideBayMode.GAME_MODES_BAY)
                  },
                )

                StarboardBottomUplinkPod(
                  heightDp = cornerPodHeight,
                  isTabletDeck = isTabletDeck,
                  syndicateTag = syndicateTag,
                  syndicateDisplayName = cleanSyndicateDisplayTitle,
                  syndicateOnlineCount = syndicateOnlineCount,
                  strikeWingOnlineCount = strikeWingOnlineCount,
                  armadaCount = armadaCount,
                  activeArmadaName = activeArmadaName,
                  onClickSyn = onClickSyn,
                  onClickWing = { onChangeSideBayMode(RiftHubSideBayMode.SQUADRON_BAY) },
                )
              }
            }
            RiftHubSideBayMode.HANGAR_BAY -> {
              HangarVerticalWingBay(
                ships = remember(customShipRev) { RiftCodex.getAllTenShips().filter { it.id in 6..10 } },
                isRightSide = true,
                isTabletDeck = isTabletDeck,
                headerHeight = bayHeaderHeight,
                wingStackGap = wingStackGap,
                chargedShipId = chargedShipId,
                bestShipId = resolvedBestShipId,
                inspectedShipId = (overwatchPrompt as? OverwatchCircularPrompt.ShipInspect)?.shipId ?: 0,
                shipSpinDeg = shipSpinDeg,
                fluxOrbitRad = fluxOrbitRad,
                shipResonanceFor = { resolveLocalShipResonance(it) },
                onBackToHub = {
                  onChangeOverwatchPrompt(OverwatchCircularPrompt.None)
                  onChangeSideBayMode(RiftHubSideBayMode.HUB_MAIN)
                },
                onOpenStudio = {
                  onChangeOverwatchPrompt(OverwatchCircularPrompt.None)
                  onChangeSideBayMode(RiftHubSideBayMode.RIFT_FRAME_BAY)
                },
                onTapShip = { shipId ->
                  onChangeOverwatchPrompt(OverwatchCircularPrompt.ShipInspect(shipId))
                },
              )
            }
            RiftHubSideBayMode.RIFT_FRAME_BAY -> {
              RiftFrameCosmeticsSideBay(
                isTabletDeck = isTabletDeck,
                headerHeight = bayHeaderHeight,
                wingStackGap = wingStackGap,
                shardText = shardText,
                liveResonance = liveResonance,
                shipSpinDeg = shipSpinDeg,
                fluxOrbitRad = fluxOrbitRad,
                isCharged = chargedShipId == 10,
                localPrefs = localPrefs,
                currentUserId = currentUserId,
                onShareBuild = onShareActiveRiftFrameBuild,
                onInspectCosmetic = { item, isUnlocked, isEquipped ->
                  val isRankedReward = RiftProgressionCatalog.isRankedCosmeticCategory(item.category)
                  val previewSkin = if (item.category == "HULL_TRIM") item.id.coerceIn(0, 5) else null
                  val previewFinisher = if (item.category == "KILL_FINISHER") item.id.coerceIn(0, 4) else null
                  val previewWarp = if (item.category == "WARP_SIGNATURE") item.id.coerceIn(0, 3) else null
                  val previewHalo = if (item.category == "HVT_HALO") item.id.coerceIn(0, 3) else null
                  onChangeOverwatchPrompt(
                    if (isRankedReward) {
                      OverwatchCircularPrompt.InfoStageInspect(
                        kicker = "RANKED LIVE RESONANCE REWARD",
                        title = item.name.uppercase(Locale.US),
                        badge =
                          when {
                            isEquipped -> "EQUIPPED"
                            isUnlocked -> "ACTIVE"
                            else -> "${formatOverwatchResonance(item.freeAtResonance)} LIVE"
                          },
                        summary = item.summary,
                        tacticalNote =
                          if (item.freeAtResonance <= 0) "Standard Ranked equipment."
                          else "Active while maintaining ${formatOverwatchResonance(item.freeAtResonance)} Live Resonance. Never sold for Shards.",
                        accentHex = if (isUnlocked) 0xFF00F5D4L else 0xFFFFD700L,
                        shipId = hangarIndicatedShipId,
                        showResonanceCrest = true,
                        previewFinisherId = previewFinisher,
                        previewWarpStyleId = previewWarp,
                        previewHaloStyleId = previewHalo,
                        primaryActionLabel = if (!isEquipped && isUnlocked) "EQUIP" else "",
                        onPrimaryAction =
                          if (!isEquipped && isUnlocked) ({
                            val res = handleUnlockOrEquipCosmetic(item)
                            if (res.first) {
                              onChangeOverwatchPrompt(
                                OverwatchCircularPrompt.InfoStageInspect(
                                  kicker = "RANKED LIVE RESONANCE REWARD",
                                  title = item.name.uppercase(Locale.US),
                                  badge = "EQUIPPED",
                                  summary = item.summary,
                                  tacticalNote =
                                    if (item.freeAtResonance <= 0) "Standard Ranked equipment."
                                    else "Active while maintaining ${formatOverwatchResonance(item.freeAtResonance)} Live Resonance. Never sold for Shards.",
                                  accentHex = 0xFF00F5D4L,
                                  shipId = hangarIndicatedShipId,
                                  showResonanceCrest = true,
                                  previewFinisherId = previewFinisher,
                                  previewWarpStyleId = previewWarp,
                                  previewHaloStyleId = previewHalo,
                                )
                              )
                            }
                          })
                          else null,
                      )
                    } else {
                      OverwatchCircularPrompt.InfoStageInspect(
                        kicker = "SHIP SKIN",
                        title = item.name.uppercase(Locale.US),
                        badge =
                          when {
                            isEquipped -> "EQUIPPED"
                            isUnlocked -> "OWNED"
                            else -> "${item.shardCost} SHARDS"
                          },
                        summary = item.summary,
                        tacticalNote =
                          if (item.shardCost <= 0L) "Standard anodized hull finish."
                          else "Permanent Ship Skin unlock (${item.shardCost} Rift Shards).",
                        accentHex = if (isUnlocked) 0xFF00F5D4L else 0xFFFFD700L,
                        shipId = hangarIndicatedShipId,
                        previewHullSkinId = previewSkin,
                        primaryActionLabel =
                          when {
                            isEquipped -> ""
                            isUnlocked -> "EQUIP"
                            else -> "BUY ${item.shardCost}"
                          },
                        onPrimaryAction =
                          if (isEquipped) null
                          else ({
                            val res = handleUnlockOrEquipCosmetic(item)
                            storePurchaseBanner = res.second
                            if (res.first) {
                              onChangeOverwatchPrompt(
                                OverwatchCircularPrompt.InfoStageInspect(
                                  kicker = "SHIP SKIN",
                                  title = item.name.uppercase(Locale.US),
                                  badge = "EQUIPPED",
                                  summary = item.summary,
                                  tacticalNote =
                                    if (item.shardCost <= 0L) "Standard anodized hull finish."
                                    else "Permanent Ship Skin unlock (${item.shardCost} Rift Shards).",
                                  accentHex = 0xFF00F5D4L,
                                  shipId = hangarIndicatedShipId,
                                  previewHullSkinId = previewSkin,
                                )
                              )
                            }
                          }),
                      )
                    }
                  )
                },
              )
            }
            RiftHubSideBayMode.SQUADRON_BAY -> {
              SquadronRightPlayfieldShipBay(
                isTabletDeck = isTabletDeck,
                headerHeight = bayHeaderHeight,
                wingStackGap = wingStackGap,
                localCallsign = callsign,
                localSyndicateTag = syndicateTag,
                localShipId = hangarIndicatedShipId,
                localShipResonance = resolveLocalShipResonance(hangarIndicatedShipId),
                isLocalShipCharged = hasChargedShip,
                inspectedPilot = activeInspectedPilot,
                armadaName = activeArmadaName,
                armadaSlots = armadaSlots,
                shipSpinDeg = shipSpinDeg,
                fluxOrbitRad = fluxOrbitRad,
                onLeaveSquadron = { engine?.leaveArmadaToSolo() },
                onInspectPilotPrompt = { pilotItem ->
                  onChangeOverwatchPrompt(OverwatchCircularPrompt.PilotInspect(pilotItem))
                },
                onOpenFullSquadronModal = onOpenFullSquadronModal,
              )
            }
            RiftHubSideBayMode.SYNDICATE_BAY -> {
              SyndicateRightCitadelBay(
                isTabletDeck = isTabletDeck,
                headerHeight = bayHeaderHeight,
                wingStackGap = wingStackGap,
                syndicateTag = syndicateTag,
                syndicateName = cleanSyndicateDisplayTitle,
                syndicateOnlineCount = syndicateOnlineCount,
                syndicateWarRecord = syndicateWarRecord,
                currentUserId = currentUserId,
                callsign = callsign,
                localShipId = hangarIndicatedShipId,
                liveResonance = liveResonance.toLong(),
                kills = 0L,
                localPrefs = localPrefs,
                onLeaveCurrentSyndicate = {
                  onChangeOverwatchPrompt(
                    OverwatchCircularPrompt.ActionConfirm(
                      kicker = "SYNDICATE CITADEL",
                      title = "LEAVE [$syndicateTag]?",
                      subtitle = "Returns your pilot identity to unaffiliated status until you join or charter another Syndicate.",
                      confirmLabel = "LEAVE",
                      accentHex = 0xFFFB7185L,
                      onConfirm = onLeaveCurrentSyndicate,
                    )
                  )
                },
                onCharterCustomSyndicate = onCharterCustomSyndicate,
                onOpenCrestBuilder = { tag ->
                  onChangeOverwatchPrompt(OverwatchCircularPrompt.SyndicateCrestBuilder(tag))
                },
              )
            }
            RiftHubSideBayMode.GAME_MODES_BAY -> {
              GameModesRightCustomBay(
                isTabletDeck = isTabletDeck,
                headerHeight = bayHeaderHeight,
                wingStackGap = wingStackGap,
                publicCustomGames = publicCustomGames,
                engineCustomGames = engine?.activeCustomGames.orEmpty(),
                unlockedPrivateCodes = unlockedPrivateCodes,
                customIsPrivateRoom = customIsPrivateRoom,
                onTogglePrivateRoom = onTogglePrivateRoom,
                generatedRoomCode = generatedRoomCode,
                joinRoomCodeDraft = joinRoomCodeDraft,
                onJoinRoomCodeDraftChange = onJoinRoomCodeDraftChange,
                roomCodeBanner = roomCodeBanner,
                onJoinByRoomCode = onJoinByRoomCode,
                customBaseMode = customBaseMode,
                onSelectCustomBaseMode = onSelectCustomBaseMode,
                customDamageTakenScalePct = customDamageTakenScalePct,
                onSelectCustomDamageTakenScalePct = onSelectCustomDamageTakenScalePct,
                customWeaponFluxCostScalePct = customWeaponFluxCostScalePct,
                onSelectCustomWeaponFluxCostScalePct = onSelectCustomWeaponFluxCostScalePct,
                customBotCount = customBotCount,
                onSelectCustomBotCount = onSelectCustomBotCount,
                customBotSkillLevel = customBotSkillLevel,
                onSelectCustomBotSkillLevel = onSelectCustomBotSkillLevel,
                customResetMinutes = customResetMinutes,
                onSelectCustomResetMinutes = onSelectCustomResetMinutes,
                customScoreCap = customScoreCap,
                onSelectCustomScoreCap = onSelectCustomScoreCap,
                customBulletSizeMult = customBulletSizeMult,
                onSelectCustomBulletSizeMult = onSelectCustomBulletSizeMult,
                customSpeedMult = customSpeedMult,
                onSelectCustomSpeedMult = onSelectCustomSpeedMult,
                customDamageMult = customDamageMult,
                onSelectCustomDamageMult = onSelectCustomDamageMult,
                customRechargeMult = customRechargeMult,
                onSelectCustomRechargeMult = onSelectCustomRechargeMult,
                customCrateSpawnMult = customCrateSpawnMult,
                onSelectCustomCrateSpawnMult = onSelectCustomCrateSpawnMult,
                customAllowArmadasInFfa = customAllowArmadasInFfa,
                onToggleAllowArmadasInFfa = onToggleAllowArmadasInFfa,
                onJoinCustomInstance = onJoinCustomInstance,
                onQuickCreateCustomGame = onQuickCreateCustomGame,
                onOpenSliderWorkbench = { wb ->
                  onChangeOverwatchPrompt(wb)
                },
              )
            }
            RiftHubSideBayMode.CONDUIT_BAY -> {
              ConduitRankTrackSideBay(
                isTabletDeck = isTabletDeck,
                headerHeight = bayHeaderHeight,
                wingStackGap = wingStackGap,
                liveResonance = liveResonance,
                onInspectTierStage = { infoPrompt ->
                  onChangeOverwatchPrompt(infoPrompt)
                },
              )
            }
            RiftHubSideBayMode.SHARD_STORE_BAY -> {
              ShardStoreRightPacksBay(
                isTabletDeck = isTabletDeck,
                headerHeight = bayHeaderHeight,
                wingStackGap = wingStackGap,
                currentUserId = currentUserId,
                callsign = callsign,
                onInspectSkuPack = { sku, launchPurchase ->
                  onChangeOverwatchPrompt(
                    OverwatchCircularPrompt.InfoStageInspect(
                      kicker = "SHARD STORE PACK",
                      title = "${sku.title.uppercase(Locale.US)} • ${sku.displayPriceUsd}",
                      badge = sku.badgeLabel,
                      summary =
                        if (sku.bonusShards > 0L)
                          "Includes ${formatOverwatchResonance(sku.shardAmount.toInt())} base Shards plus ${sku.bonusShards} bonus Shards (${formatOverwatchResonance(sku.totalShards.toInt())} total)."
                        else
                          "Includes ${formatOverwatchResonance(sku.totalShards.toInt())} Rift Shards.",
                      tacticalNote =
                        if (RiftBillingManager.TESTER_INFINITE_SHARDS_ACTIVE)
                          "Tester Treasury active: 999,999 non-depleting Shards unlocked; billing handled by ${RiftBillingManager.STUDIO_LEGAL_NAME}."
                        else
                          "Processed via Google Play Billing (${RiftBillingManager.STUDIO_LEGAL_NAME}).",
                      accentHex = 0xFFFACC15L,
                      primaryActionLabel = "BUY ${sku.displayPriceUsd}",
                      onPrimaryAction = launchPurchase,
                    )
                  )
                },
                onPurchaseResultBanner = { msg, credited ->
                  storePurchaseBanner = msg
                  if (credited > 0L) {
                    liveShardsBonus += credited
                    val uidOrNull = currentUserId.ifBlank { null }
                    localPrefs.setRiftShards(localPrefs.getRiftShards(uidOrNull) + credited, uidOrNull)
                  }
                },
              )
            }
            RiftHubSideBayMode.DOSSIER_BAY -> {
              DossierRightCareerBay(
                isTabletDeck = isTabletDeck,
                headerHeight = bayHeaderHeight,
                wingStackGap = wingStackGap,
                bestShipId = resolvedBestShipId,
                shipSpinDeg = shipSpinDeg,
                fluxOrbitRad = fluxOrbitRad,
                shipResonanceFor = { resolveLocalShipResonance(it) },
                localPrefs = localPrefs,
                currentUserId = currentUserId,
                liveResonance = liveResonance,
                dossierRefreshNonce = dossierRefreshNonce,
                cloudLeaderboardEntries = cloudLeaderboardEntries,
                onInspectShip = { shipId ->
                  onChangeOverwatchPrompt(OverwatchCircularPrompt.ShipInspect(shipId))
                },
                onOpenSurvivalSpreadsheet = onOpenSurvivalSpreadsheet,
              )
            }
            RiftHubSideBayMode.HELM_CODEX_BAY -> {
              HelmRightCodexBay(
                isTabletDeck = isTabletDeck,
                headerHeight = bayHeaderHeight,
                wingStackGap = wingStackGap,
                initialSubDeck = helmInitialSubDeck,
                shipSpinDeg = shipSpinDeg,
                fluxOrbitRad = fluxOrbitRad,
                onInspectCodexStage = { infoPrompt ->
                  onChangeOverwatchPrompt(infoPrompt)
                },
                onInspectShipStage = { shipId ->
                  onChangeOverwatchPrompt(OverwatchCircularPrompt.ShipInspect(shipId, hideChargeButton = true))
                },
              )
            }
            RiftHubSideBayMode.OVERWATCH_INSPECT_BAY -> {
              OverwatchInspectRightPilotsBay(
                isTabletDeck = isTabletDeck,
                headerHeight = bayHeaderHeight,
                wingStackGap = wingStackGap,
                pilots = overwatchMatchedPilots,
                selectedPilotUid = activeInspectedPilot?.uid.orEmpty(),
                currentUserId = currentUserId,
                onTapPilot = { pilotItem ->
                  onSelectSquadronPilotUid(pilotItem.uid)
                  onChangeOverwatchPrompt(OverwatchCircularPrompt.PilotInspect(pilotItem))
                },
                onOpenSquadronBay = {
                  onChangeOverwatchPrompt(OverwatchCircularPrompt.None)
                  onChangeSideBayMode(RiftHubSideBayMode.SQUADRON_BAY)
                },
              )
            }
          }
        }
      }

      // 4. OPEN-SPACE CORNER CONTROLS OUTSIDE OVERWATCH (Top-Left Option #1 Terminal + Chevrons, Top-Right RIFT IN, Bottom-Left/Right Confirm/Back)
      OverwatchOpenSpaceCornerOverlays(
        openCornerAnchorDp = openCornerAnchorDp,
        openCornerMaxWidthDp = openCornerMaxWidthDp,
        isTabletDeck = isTabletDeck,
        isFocused = centerFocused,
        overwatchInstances = overwatchInstances,
        activeInstance = activeOverwatchInstance,
        overwatchZoom = overwatchZoom,
        liveRemainingSec = liveRemainingSec,
        liveAvgResonance = liveAvgResonance,
        activeShipTitle = hangarPrimaryStat,
        activeShipState = hangarBadgeText,
        sideBayMode = sideBayMode,
        overwatchPrompt = overwatchPrompt,
        chargedShipId = chargedShipId,
        renameDraft = renameDraftState,
        density = density,
        onSelectOverwatchKey = onSelectOverwatchKey,
        onResetOverwatchCamera = onResetOverwatchCamera,
        onQuickZoneIntoOverwatch = onQuickZoneIntoOverwatch,
        onToggleOverwatchInspectBay = {
          onChangeOverwatchPrompt(OverwatchCircularPrompt.None)
          if (sideBayMode == RiftHubSideBayMode.OVERWATCH_INSPECT_BAY) {
            onChangeSideBayMode(RiftHubSideBayMode.HUB_MAIN)
          } else {
            onChangeSideBayMode(RiftHubSideBayMode.OVERWATCH_INSPECT_BAY)
          }
        },
        onToggleAllModesBay = {
          onChangeOverwatchPrompt(OverwatchCircularPrompt.None)
          if (sideBayMode == RiftHubSideBayMode.GAME_MODES_BAY) {
            onChangeSideBayMode(RiftHubSideBayMode.HUB_MAIN)
          } else {
            onChangeSideBayMode(RiftHubSideBayMode.GAME_MODES_BAY)
          }
        },
        onBottomLeftBackOrCancel = {
          if (overwatchPrompt != OverwatchCircularPrompt.None) {
            onChangeOverwatchPrompt(OverwatchCircularPrompt.None)
          } else if (sideBayMode != RiftHubSideBayMode.HUB_MAIN) {
            onChangeSideBayMode(RiftHubSideBayMode.HUB_MAIN)
          }
        },
        onBottomRightConfirmOrCharge = {
          when (val p = overwatchPrompt) {
            is OverwatchCircularPrompt.ShipInspect -> {
              if (!p.hideChargeButton) {
                onChargeShip(p.shipId)
                onChangeOverwatchPrompt(OverwatchCircularPrompt.None)
                onChangeSideBayMode(RiftHubSideBayMode.HUB_MAIN)
              } else {
                onChangeOverwatchPrompt(OverwatchCircularPrompt.None)
              }
            }
            is OverwatchCircularPrompt.PilotInspect -> {
              onInviteOnlinePilotToWing(p.pilot.rawLivePilot)
              onChangeOverwatchPrompt(OverwatchCircularPrompt.None)
            }
            is OverwatchCircularPrompt.ActionConfirm -> {
              p.onConfirm()
              onChangeOverwatchPrompt(OverwatchCircularPrompt.None)
            }
            is OverwatchCircularPrompt.CallsignRename -> {
              val clean = renameDraftState.trim().uppercase(Locale.US).replace(Regex("[^A-Z0-9_\\-]"), "").take(14)
              if (clean.length >= 2) {
                onRenamePilotCallsign(clean)
                onChangeOverwatchPrompt(OverwatchCircularPrompt.None)
              }
            }
            is OverwatchCircularPrompt.SyndicateCrestBuilder -> {
              onChangeOverwatchPrompt(OverwatchCircularPrompt.None)
            }
            is OverwatchCircularPrompt.InfoStageInspect -> {
              p.onPrimaryAction?.invoke()
              onChangeOverwatchPrompt(OverwatchCircularPrompt.None)
            }
            OverwatchCircularPrompt.ResonanceLeaderboard -> {
              onChangeOverwatchPrompt(OverwatchCircularPrompt.None)
            }
            OverwatchCircularPrompt.RiftFrameDrydock -> {
              onChargeShip(10)
              onChangeOverwatchPrompt(OverwatchCircularPrompt.None)
              onChangeSideBayMode(RiftHubSideBayMode.HUB_MAIN)
            }
            is OverwatchCircularPrompt.FrameSlotRename -> {
              val clean = frameRenameDraft.trim().take(18)
              if (clean.isNotBlank()) {
                onSaveCustomSlot(
                  activeCustomSlot
                    .copy(slotId = activeCustomSlotId, customName = clean)
                    .clampToPointBudget(pointBudget.coerceAtLeast(100))
                )
              }
              onChangeOverwatchPrompt(OverwatchCircularPrompt.None)
            }
            is OverwatchCircularPrompt.SliderWorkbench -> {
              onChangeOverwatchPrompt(OverwatchCircularPrompt.None)
            }
            OverwatchCircularPrompt.None -> {
              if (sideBayMode == RiftHubSideBayMode.RIFT_FRAME_BAY) {
                onChargeShip(10)
                onChangeSideBayMode(RiftHubSideBayMode.HUB_MAIN)
              }
            }
          }
        },
      )

      if (hubTutorialStep in 1..6) {
        HubGuidedTutorialCoachmarkCard(
          modifier =
            Modifier.align(Alignment.BottomCenter)
              .padding(bottom = 8.dp, start = 12.dp, end = 12.dp)
              .widthIn(min = 276.dp, max = if (isTabletDeck) 468.dp else 404.dp),
          step = hubTutorialStep,
          isTabletDeck = isTabletDeck,
          pulseAlpha = tutorialPulseAlpha,
          onBack = {
            if (hubTutorialStep > 1) {
              hubTutorialStep--
            } else {
              hubTutorialStep = 0
              onChangeOverwatchPrompt(OverwatchCircularPrompt.None)
              onChangeSideBayMode(RiftHubSideBayMode.HUB_MAIN)
            }
          },
          onSkipToFlight = {
            hubTutorialStep = 0
            onChangeOverwatchPrompt(OverwatchCircularPrompt.None)
            onChangeSideBayMode(RiftHubSideBayMode.HUB_MAIN)
            onEnterPracticeRift()
          },
          onNextOrStartFlight = {
            if (hubTutorialStep < 6) {
              hubTutorialStep++
            } else {
              hubTutorialStep = 0
              onChangeOverwatchPrompt(OverwatchCircularPrompt.None)
              onChangeSideBayMode(RiftHubSideBayMode.HUB_MAIN)
              onEnterPracticeRift()
            }
          },
          onDismissTour = {
            hubTutorialStep = 0
            onChangeOverwatchPrompt(OverwatchCircularPrompt.None)
            onChangeSideBayMode(RiftHubSideBayMode.HUB_MAIN)
          },
        )
      }

      val draggedItem = activeDraggedFrameItem
      if (draggedItem != null) {
        val ghostBmp =
          remember(draggedItem.isPrimary, draggedItem.itemId, RiftSpriteBaker.spriteVisualRevision) {
            if (draggedItem.isPrimary) {
              RiftSpriteBaker.getPrimaryWeaponIconBitmap(draggedItem.itemId).asImageBitmap()
            } else {
              RiftSpriteBaker.getSecondaryAbilityIconBitmap(draggedItem.itemId).asImageBitmap()
            }
          }
        val localX = dragPointerRootOffset.x - stageRootOrigin.x
        val localY = dragPointerRootOffset.y - stageRootOrigin.y
        Column(
          modifier =
            Modifier.offset {
                IntOffset(
                  (localX - with(density) { 42.dp.toPx() }).roundToInt(),
                  (localY - with(density) { 48.dp.toPx() }).roundToInt(),
                )
              }
              .clip(RoundedCornerShape(10.dp))
              .background(Color(0xEB071426))
              .border(1.5.dp, draggedItem.accentColor, RoundedCornerShape(10.dp))
              .padding(horizontal = 8.dp, vertical = 5.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
          Image(
            bitmap = ghostBmp,
            contentDescription = draggedItem.label,
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(34.dp),
          )
          Text(
            text = "${draggedItem.label.uppercase(Locale.US)} • ${draggedItem.pointCost}P",
            color = Color.White,
            fontSize = 7.6.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
          )
        }
      }
    }
  }
}

@Composable
private fun HubGuidedTutorialCoachmarkCard(
  modifier: Modifier = Modifier,
  step: Int,
  isTabletDeck: Boolean,
  pulseAlpha: Float,
  onBack: () -> Unit,
  onSkipToFlight: () -> Unit,
  onNextOrStartFlight: () -> Unit,
  onDismissTour: () -> Unit,
) {
  BackHandler(onBack = onDismissTour)
  val (title, summary) =
    when (step) {
      1 ->
        "OVERWATCH & LIVE SECTORS" to
          "Pinch 1.0x to 6.0x inside Overwatch to inspect live combat, tap the top-left chevrons to switch Rifts, tap the sensor strip to inspect human pilots, or tap RIFT IN > to launch."
      2 ->
        "RESONANCE & LEADERBOARDS" to
          "Live Resonance ranks top pilots across Global, Per-Ship, and Syndicate Leaderboards and unlocks Ranked rewards (Frame C-E, Finishers, Halos, and Warps) while held."
      3 ->
        "HANGAR DECK & 100-PT PARITY" to
          "All 10 starfighters share an identical 100-Point stat and weapon budget from 0 Resonance. Tap CHARGE on any ship to lock your hull with orbiting Flux icons across all modes."
      4 ->
        "RIFT FRAME DRAG & DROP DRYDOCK" to
          "Drag Primary Weapons from the left shelf and Secondary or Crate modules from the right shelf onto Ship 10's Overwatch sockets, then slide the 5 hull stat bars."
      5 ->
        "SYNDICATES, SQUADRON & RIFT AI" to
          "Form a 5-pilot Squadron with live team voice, charter a real-player Syndicate, or open Subspace Comms for sector channels and tactical Rift AI."
      else ->
        "ACTIVE RIFTS & FLIGHT TRAINING" to
          "Queue into 6 Competitive Modes or tune Custom Rift rules in Overwatch. Tap START FLIGHT TRAINING > below to launch Phase 2 live arena flight!"
    }
  val isFinalStep = step >= 6

  Surface(
    modifier =
      modifier
        .border(
          1.4.dp,
          Brush.horizontalGradient(
            listOf(
              Color(0xFFFFD700).copy(alpha = pulseAlpha),
              Color(0xFF00F5D4),
              Color(0xFFFFD700).copy(alpha = pulseAlpha),
            )
          ),
          RoundedCornerShape(12.dp),
        )
        .testTag("hub_guided_tutorial_card"),
    color = Color(0xF5040A16),
    shape = RoundedCornerShape(12.dp),
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "HUB GUIDE • STEP $step OF 6",
          color = Color(0xFFFFD700),
          fontSize = if (isTabletDeck) 8.6.sp else 7.8.sp,
          fontWeight = FontWeight.Black,
          fontFamily = FontFamily.Monospace,
        )
        Row(
          horizontalArrangement = Arrangement.spacedBy(4.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          for (i in 1..6) {
            Box(
              modifier =
                Modifier.size(width = if (i == step) 14.dp else 6.dp, height = 5.dp)
                  .clip(RoundedCornerShape(50))
                  .background(
                    when {
                      i == step -> Color(0xFFFFD700)
                      i < step -> Color(0xFF00F5D4)
                      else -> Color(0xFF334155)
                    }
                  ),
            )
          }
        }
      }

      Text(
        text = title,
        color = Color.White,
        fontSize = if (isTabletDeck) 10.6.sp else 9.6.sp,
        fontWeight = FontWeight.Black,
        fontFamily = FontFamily.Monospace,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Text(
        text = summary,
        color = Color(0xFFE2E8F0),
        fontSize = if (isTabletDeck) 8.8.sp else 8.0.sp,
        lineHeight = if (isTabletDeck) 11.8.sp else 10.8.sp,
        fontFamily = FontFamily.SansSerif,
      )

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Box(
          modifier =
            Modifier.minimumInteractiveComponentSize()
              .height(48.dp)
              .clickable { onBack() }
              .padding(vertical = 7.dp)
              .testTag("hub_tutorial_btn_back"),
          contentAlignment = Alignment.Center,
        ) {
          Box(
            modifier =
              Modifier.clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF0B192C))
                .border(1.dp, Color(0xFF38BDF8), RoundedCornerShape(6.dp))
                .padding(horizontal = 9.dp, vertical = 5.dp),
          ) {
            Text(
              text = if (step == 1) "CLOSE" else "< BACK",
              color = Color(0xFFCBD5E1),
              fontSize = 7.8.sp,
              fontWeight = FontWeight.Black,
              fontFamily = FontFamily.Monospace,
            )
          }
        }

        if (!isFinalStep) {
          Box(
            modifier =
              Modifier.minimumInteractiveComponentSize()
                .height(48.dp)
                .clickable { onSkipToFlight() }
                .padding(vertical = 7.dp)
                .testTag("hub_tutorial_btn_skip_to_flight"),
            contentAlignment = Alignment.Center,
          ) {
            Box(
              modifier =
                Modifier.clip(RoundedCornerShape(6.dp))
                  .background(Color(0xFF092234))
                  .border(1.dp, Color(0xFF00F5D4), RoundedCornerShape(6.dp))
                  .padding(horizontal = 9.dp, vertical = 5.dp),
            ) {
              Text(
                text = "SKIP TO FLIGHT >",
                color = Color(0xFF00F5D4),
                fontSize = 7.8.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
              )
            }
          }
        }

        Box(
          modifier =
            Modifier.minimumInteractiveComponentSize()
              .height(48.dp)
              .clickable { onNextOrStartFlight() }
              .padding(vertical = 7.dp)
              .testTag("hub_tutorial_btn_next"),
          contentAlignment = Alignment.Center,
        ) {
          Box(
            modifier =
              Modifier.clip(RoundedCornerShape(6.dp))
                .background(if (isFinalStep) Color(0xFF0E392D) else Color(0xFF2B2208))
                .border(
                  1.2.dp,
                  if (isFinalStep) Color(0xFF00F5D4) else Color(0xFFFFD700),
                  RoundedCornerShape(6.dp),
                )
                .padding(horizontal = 10.dp, vertical = 5.dp),
          ) {
            Text(
              text = if (isFinalStep) "START FLIGHT TRAINING >" else "NEXT >",
              color = if (isFinalStep) Color(0xFF00F5D4) else Color(0xFFFEF08A),
              fontSize = 8.0.sp,
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
private fun HubDigitizingScanlineBox(
  modifier: Modifier,
  scanProgress: Float,
  isRightSide: Boolean,
  content: @Composable BoxScope.() -> Unit,
) {
  Box(
    modifier =
      modifier.drawWithContent {
        drawContent()
        if (scanProgress < 0.99f) {
          val sweepY = size.height * scanProgress
          val alpha = (1f - scanProgress).coerceIn(0f, 1f)
          val bandH = (size.height * 0.16f).coerceAtLeast(18.dp.toPx())
          drawRect(
            brush =
              Brush.verticalGradient(
                colors =
                  listOf(
                    Color.Transparent,
                    Color(0xFF00F5D4).copy(alpha = 0.28f * alpha),
                    Color(0xFF38BDF8).copy(alpha = 0.14f * alpha),
                    Color.Transparent,
                  ),
                startY = (sweepY - bandH * 0.5f).coerceAtLeast(0f),
                endY = (sweepY + bandH * 0.5f).coerceAtMost(size.height),
              ),
          )
          drawLine(
            brush =
              Brush.horizontalGradient(
                if (!isRightSide) {
                  listOf(Color.White.copy(alpha = 0.85f * alpha), Color(0xFF00F5D4).copy(alpha = 0.90f * alpha), Color.Transparent)
                } else {
                  listOf(Color.Transparent, Color(0xFF00F5D4).copy(alpha = 0.90f * alpha), Color.White.copy(alpha = 0.85f * alpha))
                }
              ),
            start = Offset(0f, sweepY),
            end = Offset(size.width, sweepY),
            strokeWidth = 1.8.dp.toPx(),
          )
        }
      },
    content = content,
  )
}

@Composable
private fun DedicatedCompactSplitBay(
  modifier: Modifier,
  title: String,
  primaryStat: String,
  badgeText: String,
  detailCue: String,
  accent: Color,
  secondaryAccent: Color,
  isTabletDeck: Boolean,
  testTag: String,
  onClick: () -> Unit,
) {
  BoxWithConstraints(
    modifier =
      modifier
        .minimumInteractiveComponentSize()
        .heightIn(min = 52.dp)
        .chamferedSciFiPodSurface(
          isRightSide = true,
          primaryBorder = accent,
          secondaryBorder = secondaryAccent,
        )
        .clickable { onClick() }
        .padding(horizontal = if (isTabletDeck) 9.dp else 7.dp, vertical = 5.dp)
        .testTag(testTag),
  ) {
    val showDetail = maxHeight >= 76.dp
    Column(
      modifier = Modifier.fillMaxSize(),
      verticalArrangement = Arrangement.Center,
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = title,
          color = Color.White,
          fontSize = if (isTabletDeck) 10.6.sp else 9.0.sp,
          fontWeight = FontWeight.Black,
          fontFamily = FontFamily.Monospace,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
          modifier = Modifier.weight(1f, fill = false).padding(end = 3.dp),
        )
        Text(
          text = badgeText,
          color = secondaryAccent,
          fontSize = if (isTabletDeck) 8.0.sp else 7.2.sp,
          fontWeight = FontWeight.Black,
          fontFamily = FontFamily.Monospace,
          maxLines = 1,
          style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
        )
      }
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = primaryStat,
        color = accent,
        fontSize = if (isTabletDeck) 9.4.sp else 8.2.sp,
        fontWeight = FontWeight.ExtraBold,
        fontFamily = FontFamily.Monospace,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
      )
      if (showDetail) {
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = detailCue,
          color = Color(0xFF94A3B8),
          fontSize = if (isTabletDeck) 7.8.sp else 7.0.sp,
          fontWeight = FontWeight.SemiBold,
          fontFamily = FontFamily.Monospace,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
        )
      }
    }
  }
}

internal fun Modifier.chamferedSciFiPodSurface(
  isRightSide: Boolean,
  primaryBorder: Color,
  secondaryBorder: Color,
): Modifier =
  this.drawBehind {
    val w = size.width
    val h = size.height
    if (w <= 8f || h <= 8f) return@drawBehind
    drawRect(
      brush =
        Brush.horizontalGradient(
          colors =
            if (!isRightSide) {
              listOf(Color(0xC6050D1C), Color(0x96071326), Color(0x5208162B))
            } else {
              listOf(Color(0x5208162B), Color(0x96071326), Color(0xC6050D1C))
            }
        )
    )
    // Subtle top holographic glint hairline (no boxy octagonal border)
    drawLine(
      brush =
        Brush.horizontalGradient(
          colors =
            if (!isRightSide) {
              listOf(primaryBorder.copy(alpha = 0.44f), secondaryBorder.copy(alpha = 0.22f), Color.Transparent)
            } else {
              listOf(Color.Transparent, secondaryBorder.copy(alpha = 0.22f), primaryBorder.copy(alpha = 0.44f))
            }
        ),
      start = Offset(0f, 0.5.dp.toPx()),
      end = Offset(w, 0.5.dp.toPx()),
      strokeWidth = 0.75.dp.toPx(),
    )
    // Inner Overwatch-facing luminous 2.dp accent rail
    val railX = if (!isRightSide) w - 1.dp.toPx() else 1.dp.toPx()
    drawLine(
      brush =
        Brush.verticalGradient(
          listOf(
            primaryBorder.copy(alpha = 0.92f),
            secondaryBorder.copy(alpha = 0.72f),
            primaryBorder.copy(alpha = 0.36f),
          )
        ),
      start = Offset(railX, 2.dp.toPx()),
      end = Offset(railX, (h - 2.dp.toPx()).coerceAtLeast(2.dp.toPx())),
      strokeWidth = 2.0.dp.toPx(),
      cap = StrokeCap.Round,
    )
  }

@Composable
private fun PortTopPilotPod(
  heightDp: Dp,
  @Suppress("UNUSED_PARAMETER") isTabletDeck: Boolean,
  pilotIdentityLabel: String,
  conduitRankLabel: String = "",
  isGoogleAuthenticated: Boolean,
  isStreaming: Boolean,
  onClickCallsign: () -> Unit,
  onSwitchAccount: () -> Unit,
) {
  Row(
    modifier =
      Modifier.fillMaxWidth()
        .height(heightDp)
        .chamferedSciFiPodSurface(
          isRightSide = false,
          primaryBorder = Color(0xFF00F5D4),
          secondaryBorder = Color(0xFF38BDF8),
        )
        .padding(start = 9.dp, end = 7.dp, top = 4.dp, bottom = 5.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Row(
      modifier =
        Modifier.weight(1f)
          .fillMaxHeight()
          .minimumInteractiveComponentSize()
          .clickable { onClickCallsign() }
          .padding(end = 6.dp)
          .testTag("nexus_top_callsign"),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
      SwirlingRiftVortexGlyph(
        modifier = Modifier.size(26.dp),
        isStreaming = isStreaming,
      )
      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.Center,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
          Box(
            modifier =
              Modifier.size(7.dp)
                .clip(CircleShape)
                .background(if (isGoogleAuthenticated) Color(0xFF00F5D4) else Color(0xFFFACC15))
          )
          Text(
            text = pilotIdentityLabel,
            color = Color(0xFFF8FAFC),
            fontSize = 12.0.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
          )
        }
        Spacer(modifier = Modifier.height(2.5.dp))
        Text(
          text =
            if (conduitRankLabel.isNotBlank()) "DOSSIER • $conduitRankLabel"
            else "PILOT DOSSIER >",
          color = Color(0xFF38BDF8),
          fontSize = 9.2.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.Monospace,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
        )
      }
    }

    Box(
      modifier =
        Modifier.width(1.dp)
          .fillMaxHeight(0.68f)
          .background(Color(0x4438BDF8))
    )

    Box(
      modifier =
        Modifier.minimumInteractiveComponentSize()
          .size(48.dp)
          .clickable { onSwitchAccount() }
          .testTag("nexus_top_switch_account"),
      contentAlignment = Alignment.Center,
    ) {
      Canvas(modifier = Modifier.size(20.dp)) {
        val w = size.width
        val h = size.height
        val accent = if (isGoogleAuthenticated) Color(0xFF00F5D4) else Color(0xFFFACC15)
        drawArc(
          color = accent,
          startAngle = -145f,
          sweepAngle = 125f,
          useCenter = false,
          topLeft = Offset(w * 0.10f, h * 0.12f),
          size = Size(w * 0.80f, h * 0.76f),
          style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round),
        )
        drawArc(
          color = Color(0xFF38BDF8),
          startAngle = 35f,
          sweepAngle = 125f,
          useCenter = false,
          topLeft = Offset(w * 0.10f, h * 0.12f),
          size = Size(w * 0.80f, h * 0.76f),
          style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round),
        )
        drawCircle(
          color = Color.White,
          radius = 2.6.dp.toPx(),
          center = Offset(w * 0.5f, h * 0.5f),
        )
      }
    }
  }
}

@Composable
private fun PortBottomTelemetryPod(
  heightDp: Dp,
  @Suppress("UNUSED_PARAMETER") isTabletDeck: Boolean,
  formattedLiveResonance: String,
  conduitBadge: String = "",
  shardText: String,
  onlineShortLabel: String,
  onlineAccent: Color,
  onClickResonance: () -> Unit,
  onClickShards: () -> Unit,
  onClickOnline: () -> Unit,
) {
  Row(
    modifier =
      Modifier.fillMaxWidth()
        .height(heightDp)
        .chamferedSciFiPodSurface(
          isRightSide = false,
          primaryBorder = Color(0xFFFFD700),
          secondaryBorder = Color(0xFF00F5D4),
        )
        .padding(horizontal = 6.dp, vertical = 4.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Column(
      modifier =
        Modifier.weight(1.0f)
          .fillMaxHeight()
          .minimumInteractiveComponentSize()
          .clickable { onClickResonance() }
          .padding(horizontal = 3.dp)
          .testTag("nexus_top_resonance"),
      verticalArrangement = Arrangement.Center,
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        ResonanceCrestIcon(sizeDp = 15.dp)
        Text(
          text = formattedLiveResonance,
          color = Color(0xFFFFD700),
          fontSize = 11.0.sp,
          fontWeight = FontWeight.Black,
          fontFamily = FontFamily.Monospace,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
        )
      }
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = conduitBadge.ifBlank { "CONDUIT >" },
        color = Color(0xFFFACC15),
        fontSize = 8.6.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
      )
    }

    Box(modifier = Modifier.width(1.dp).fillMaxHeight(0.68f).background(Color(0x3D38BDF8)))

    Column(
      modifier =
        Modifier.weight(1.18f)
          .fillMaxHeight()
          .minimumInteractiveComponentSize()
          .clickable { onClickShards() }
          .padding(horizontal = 3.dp)
          .testTag("nexus_top_shards"),
      verticalArrangement = Arrangement.Center,
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        RiftShardGemIcon(sizeDp = 15.dp)
        Text(
          text = shardText,
          color = Color(0xFF38BDF8),
          fontSize = 10.6.sp,
          fontWeight = FontWeight.Black,
          fontFamily = FontFamily.Monospace,
          maxLines = 1,
          softWrap = false,
          style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
        )
      }
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = "SHARD STORE >",
        color = Color(0xFF00F5D4),
        fontSize = 8.6.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace,
        maxLines = 1,
        softWrap = false,
        style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
      )
    }

    Box(modifier = Modifier.width(1.dp).fillMaxHeight(0.68f).background(Color(0x3D38BDF8)))

    Column(
      modifier =
        Modifier.weight(0.92f)
          .fillMaxHeight()
          .minimumInteractiveComponentSize()
          .clickable { onClickOnline() }
          .padding(horizontal = 3.dp)
          .testTag("nexus_top_online"),
      verticalArrangement = Arrangement.Center,
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        Box(
          modifier =
            Modifier.size(6.5.dp)
              .clip(CircleShape)
              .background(onlineAccent)
        )
        Text(
          text = onlineShortLabel,
          color = onlineAccent,
          fontSize = 10.0.sp,
          fontWeight = FontWeight.Black,
          fontFamily = FontFamily.Monospace,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
        )
      }
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = "ROSTER >",
        color = Color(0xFF94A3B8),
        fontSize = 8.6.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
      )
    }
  }
}

@Composable
private fun StarboardTopCommandPod(
  heightDp: Dp,
  @Suppress("UNUSED_PARAMETER") isTabletDeck: Boolean,
  isInMatch: Boolean,
  helmFocused: Boolean,
  onOpenHowToPlayTutorial: () -> Unit,
  onOpenHelmSystems: () -> Unit,
  onLeaveMatch: () -> Unit,
  onPrimaryRightAction: () -> Unit,
) {
  Row(
    modifier =
      Modifier.fillMaxWidth()
        .height(heightDp)
        .chamferedSciFiPodSurface(
          isRightSide = true,
          primaryBorder = Color(0xFF00F5D4),
          secondaryBorder = if (isInMatch) Color(0xFFFB7185) else Color(0xFFFACC15),
        )
        .padding(horizontal = 6.dp, vertical = 4.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Column(
      modifier =
        Modifier.weight(if (isInMatch) 0.92f else 1.0f)
          .fillMaxHeight()
          .minimumInteractiveComponentSize()
          .clickable { onOpenHowToPlayTutorial() }
          .testTag("nexus_btn_how_to_play"),
      verticalArrangement = Arrangement.Center,
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Text(
        text = if (isInMatch) "TUTORIAL" else "TUTORIAL >",
        color = Color(0xFFFACC15),
        fontSize = 10.8.sp,
        fontWeight = FontWeight.Black,
        fontFamily = FontFamily.Monospace,
        maxLines = 1,
        style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
      )
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = if (isInMatch) "FLIGHT GUIDE" else "HUB + FLIGHT",
        color = Color(0xFFCBD5E1),
        fontSize = 8.8.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace,
        maxLines = 1,
        style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
      )
    }

    Box(modifier = Modifier.width(1.dp).fillMaxHeight(0.68f).background(Color(0x3D38BDF8)))

    Column(
      modifier =
        Modifier.weight(if (isInMatch) 0.98f else 1.05f)
          .fillMaxHeight()
          .minimumInteractiveComponentSize()
          .clickable { onOpenHelmSystems() }
          .testTag("nexus_node_helm_systems"),
      verticalArrangement = Arrangement.Center,
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Text(
        text = if (!isInMatch) "HELM SYSTEMS" else "HELM",
        color = if (helmFocused) Color.White else Color(0xFF38BDF8),
        fontSize = 10.8.sp,
        fontWeight = FontWeight.Black,
        fontFamily = FontFamily.Monospace,
        maxLines = 1,
        style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
      )
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = "CODEX & FORGE",
        color = Color(0xFF00F5D4),
        fontSize = 8.8.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace,
        maxLines = 1,
        style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
      )
    }

    if (isInMatch) {
      Box(modifier = Modifier.width(1.dp).fillMaxHeight(0.68f).background(Color(0x55FB7185)))
      Column(
        modifier =
          Modifier.weight(0.92f)
            .fillMaxHeight()
            .minimumInteractiveComponentSize()
            .clickable { onLeaveMatch() }
            .testTag("nexus_leave_match_button"),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Text(
          text = "RIFT OUT",
          color = Color(0xFFFB7185),
          fontSize = 10.2.sp,
          fontWeight = FontWeight.Black,
          fontFamily = FontFamily.Monospace,
          maxLines = 1,
          style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = "EXIT RIFT",
          color = Color(0xFFCBD5E1),
          fontSize = 8.4.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.Monospace,
          maxLines = 1,
          style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
        )
      }

      Box(modifier = Modifier.width(1.dp).fillMaxHeight(0.68f).background(Color(0x4400F5D4)))

      Column(
        modifier =
          Modifier.weight(0.96f)
            .fillMaxHeight()
            .minimumInteractiveComponentSize()
            .clickable { onPrimaryRightAction() }
            .testTag("nexus_close_button"),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Text(
          text = "RETURN >",
          color = Color(0xFF00F5D4),
          fontSize = 10.2.sp,
          fontWeight = FontWeight.Black,
          fontFamily = FontFamily.Monospace,
          maxLines = 1,
          style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = "ACTIVE MATCH",
          color = Color(0xFFCBD5E1),
          fontSize = 8.4.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.Monospace,
          maxLines = 1,
          style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
        )
      }
    }
  }
}

@Composable
private fun StarboardBottomUplinkPod(
  heightDp: Dp,
  @Suppress("UNUSED_PARAMETER") isTabletDeck: Boolean,
  syndicateTag: String = "",
  syndicateDisplayName: String = "",
  syndicateOnlineCount: Int,
  strikeWingOnlineCount: Int,
  armadaCount: Int,
  activeArmadaName: String = "",
  onClickSyn: () -> Unit,
  onClickWing: () -> Unit,
) {
  Row(
    modifier =
      Modifier.fillMaxWidth()
        .height(heightDp)
        .chamferedSciFiPodSurface(
          isRightSide = true,
          primaryBorder = Color(0xFF38BDF8),
          secondaryBorder = Color(0xFFFACC15),
        )
        .padding(horizontal = 6.dp, vertical = 4.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Column(
      modifier =
        Modifier.weight(1f)
          .fillMaxHeight()
          .minimumInteractiveComponentSize()
          .clickable { onClickSyn() }
          .padding(horizontal = 4.dp)
          .testTag("nexus_top_syn"),
      verticalArrangement = Arrangement.Center,
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      val cleanPodSynTag = syndicateTag.trim().uppercase(Locale.US)
      val hasPodSyndicate =
        cleanPodSynTag.isNotEmpty() && cleanPodSynTag !in LocalPilotPrefs.NON_PLAYER_SYNDICATE_TAGS
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        Box(
          modifier =
            Modifier.size(6.5.dp)
              .clip(CircleShape)
              .background(
                if (hasPodSyndicate && syndicateOnlineCount > 0) Color(0xFFFFD700)
                else Color(0xFF64748B)
              )
        )
        Text(
          text =
            if (hasPodSyndicate) "[$cleanPodSynTag] • ${syndicateOnlineCount.coerceAtLeast(1)} ON"
            else "SYNDICATES",
          color = Color(0xFFFACC15),
          fontSize = 10.4.sp,
          fontWeight = FontWeight.Black,
          fontFamily = FontFamily.Monospace,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
        )
      }
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text =
          if (hasPodSyndicate) syndicateDisplayName.ifBlank { "[$cleanPodSynTag]" }
          else "CHARTER / JOIN >",
        color = Color(0xFFCBD5E1),
        fontSize = 8.6.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
      )
    }

    Box(modifier = Modifier.width(1.dp).fillMaxHeight(0.68f).background(Color(0x3D38BDF8)))

    Column(
      modifier =
        Modifier.weight(1f)
          .fillMaxHeight()
          .minimumInteractiveComponentSize()
          .clickable { onClickWing() }
          .padding(horizontal = 4.dp)
          .testTag("nexus_top_wing"),
      verticalArrangement = Arrangement.Center,
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        Box(
          modifier =
            Modifier.size(6.5.dp)
              .clip(CircleShape)
              .background(if (strikeWingOnlineCount > 0) Color(0xFF00F5D4) else Color(0xFF38BDF8))
        )
        Text(
          text = "SQUADRON • ${maxOf(strikeWingOnlineCount, armadaCount)}/5",
          color = Color(0xFF00F5D4),
          fontSize = 10.4.sp,
          fontWeight = FontWeight.Black,
          fontFamily = FontFamily.Monospace,
          maxLines = 1,
          style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
        )
      }
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = activeArmadaName.ifBlank { "SQUAD UP >" },
        color = Color(0xFF38BDF8),
        fontSize = 8.6.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
      )
    }
  }
}

@Composable
internal fun HubDigitizedReturnBar(
  heightDp: Dp,
  isRightSide: Boolean,
  @Suppress("UNUSED_PARAMETER") isTabletDeck: Boolean,
  sectionTitle: String,
  accent: Color,
  trailingLabel: String? = null,
  trailingTag: String? = null,
  onTrailingClick: (() -> Unit)? = null,
  onBackToHub: (() -> Unit)? = null,
) {
  Row(
    modifier =
      Modifier.fillMaxWidth()
        .height(heightDp)
        .drawBehind {
          val w = size.width
          val h = size.height
          if (w <= 4f || h <= 4f) return@drawBehind
          val outerPlate = Color(0xB8030812)
          val midPlate = Color(0x88061224)
          val innerFade = Color(0x42071528)
          drawRect(
            brush =
              Brush.horizontalGradient(
                colors =
                  if (!isRightSide) listOf(outerPlate, midPlate, innerFade)
                  else listOf(innerFade, midPlate, outerPlate),
              ),
          )
          // 0.75.dp top holographic glint hairline
          drawLine(
            brush =
              Brush.horizontalGradient(
                colors =
                  if (!isRightSide) {
                    listOf(Color.White.copy(alpha = 0.24f), accent.copy(alpha = 0.38f), Color(0xFF00F5D4).copy(alpha = 0.28f))
                  } else {
                    listOf(Color(0xFF00F5D4).copy(alpha = 0.28f), accent.copy(alpha = 0.38f), Color.White.copy(alpha = 0.24f))
                  },
              ),
            start = Offset(0f, 0.5.dp.toPx()),
            end = Offset(w, 0.5.dp.toPx()),
            strokeWidth = 0.75.dp.toPx(),
          )
          // 2.dp luminous Overwatch-facing inner vertical accent bar
          val innerX = if (!isRightSide) w - 1.dp.toPx() else 1.dp.toPx()
          drawLine(
            brush =
              Brush.verticalGradient(
                listOf(
                  accent.copy(alpha = 0.22f),
                  accent.copy(alpha = 0.90f),
                  Color(0xFF00F5D4).copy(alpha = 0.75f),
                  accent.copy(alpha = 0.22f),
                )
              ),
            start = Offset(innerX, 2.dp.toPx()),
            end = Offset(innerX, (h - 2.dp.toPx()).coerceAtLeast(3.dp.toPx())),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round,
          )
        }
        .padding(horizontal = 6.dp, vertical = 2.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    if (onBackToHub != null) {
      Box(
        modifier =
          Modifier.minimumInteractiveComponentSize()
            .fillMaxHeight()
            .clickable { onBackToHub() }
            .padding(horizontal = 6.dp)
            .testTag("nexus_btn_back_to_hub"),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          text = "< RIFT HUB",
          color = Color(0xFF00F5D4),
          fontSize = 10.6.sp,
          fontWeight = FontWeight.Black,
          fontFamily = FontFamily.Monospace,
          maxLines = 1,
          style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
        )
      }
      Box(modifier = Modifier.width(1.dp).height(20.dp).background(Color(0x4438BDF8)))
    }

    Text(
      text = sectionTitle,
      color = Color.White,
      fontSize = 11.4.sp,
      fontWeight = FontWeight.Black,
      fontFamily = FontFamily.Monospace,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
      modifier = Modifier.weight(1f).padding(horizontal = 6.dp),
    )

    if (trailingLabel != null && onTrailingClick != null) {
      Box(modifier = Modifier.width(1.dp).height(20.dp).background(Color(0x4438BDF8)))
      Box(
        modifier =
          Modifier.minimumInteractiveComponentSize()
            .fillMaxHeight()
            .clickable { onTrailingClick() }
            .padding(horizontal = 6.dp)
            .let { mod -> if (trailingTag != null) mod.testTag(trailingTag) else mod },
        contentAlignment = Alignment.Center,
      ) {
        Text(
          text = trailingLabel,
          color = Color(0xFFFACC15),
          fontSize = 9.8.sp,
          fontWeight = FontWeight.Black,
          fontFamily = FontFamily.Monospace,
          maxLines = 1,
          style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
        )
      }
    }
  }
}

/**
 * Renders the open-space corner regions outside the curved Circular Overwatch aperture:
 * 1. Top-Left (between Port Wing & Overwatch): Up/Down sector chevrons + 2-line tappable sensor readout.
 * 2. Top-Right: Primary `RIFT IN >` launch trigger + active ship status badge.
 * (Outer bottom-left/bottom-right prompt buttons are intentionally suppressed whenever an Overwatch
 * prompt is active so inner prompt buttons are never duplicated outside the Overwatch circle.)
 */
@Composable
private fun BoxScope.OverwatchOpenSpaceCornerOverlays(
  openCornerAnchorDp: Dp,
  openCornerMaxWidthDp: Dp,
  @Suppress("UNUSED_PARAMETER") isTabletDeck: Boolean,
  isFocused: Boolean,
  overwatchInstances: List<OverwatchRiftInstance>,
  activeInstance: OverwatchRiftInstance,
  @Suppress("UNUSED_PARAMETER") favoriteOverwatchKey: String = "",
  overwatchZoom: Float,
  liveRemainingSec: Int,
  liveAvgResonance: Int,
  activeShipTitle: String,
  activeShipState: String,
  sideBayMode: RiftHubSideBayMode,
  @Suppress("UNUSED_PARAMETER") overwatchPrompt: OverwatchCircularPrompt,
  @Suppress("UNUSED_PARAMETER") chargedShipId: Int,
  @Suppress("UNUSED_PARAMETER") renameDraft: String,
  @Suppress("UNUSED_PARAMETER") density: androidx.compose.ui.unit.Density,
  onSelectOverwatchKey: (String) -> Unit,
  @Suppress("UNUSED_PARAMETER") onToggleFavoriteOverwatchKey: (String) -> Unit = {},
  onResetOverwatchCamera: () -> Unit,
  onQuickZoneIntoOverwatch: (OverwatchRiftInstance) -> Unit,
  onToggleOverwatchInspectBay: () -> Unit = {},
  @Suppress("UNUSED_PARAMETER") onToggleAllModesBay: () -> Unit,
  @Suppress("UNUSED_PARAMETER") onBottomLeftBackOrCancel: () -> Unit,
  @Suppress("UNUSED_PARAMETER") onBottomRightConfirmOrCharge: () -> Unit,
) {
  val totalInstances = overwatchInstances.size.coerceAtLeast(1)
  val currentIdx =
    remember(overwatchInstances, activeInstance.key) {
      overwatchInstances.indexOfFirst { it.key == activeInstance.key }.let { if (it < 0) 0 else it }
    }
  val countdownText = remember(liveRemainingSec) { formatOverwatchTimerMmSs(liveRemainingSec) }
  val humanCount = activeInstance.pilotCount.coerceAtLeast(0)
  val modeHeaderLine = activeInstance.modeTitle.uppercase(Locale.US)
  val isInspectBayOpen = sideBayMode == RiftHubSideBayMode.OVERWATCH_INSPECT_BAY

  // 1. TOP-LEFT OPEN SPACE: Simple Up Chevron + Tappable Option #1 Readout + Simple Down Chevron
  Column(
    modifier =
      Modifier.align(Alignment.TopStart)
        .padding(start = openCornerAnchorDp, top = 0.dp)
        .widthIn(max = openCornerMaxWidthDp),
    horizontalAlignment = Alignment.Start,
    verticalArrangement = Arrangement.spacedBy(0.dp),
  ) {
    Box(
      modifier =
        Modifier.minimumInteractiveComponentSize()
          .size(width = 48.dp, height = 48.dp)
          .clickable {
            val prevIdx = if (currentIdx <= 0) totalInstances - 1 else currentIdx - 1
            onSelectOverwatchKey(overwatchInstances[prevIdx].key)
          }
          .testTag("nexus_overwatch_prev"),
      contentAlignment = Alignment.CenterStart,
    ) {
      Canvas(modifier = Modifier.padding(start = 8.dp).size(width = 16.dp, height = 9.dp)) {
        val p =
          Path().apply {
            moveTo(0f, size.height)
            lineTo(size.width * 0.5f, 0f)
            lineTo(size.width, size.height)
          }
        drawPath(p, color = Color(0xFF00F5D4), style = Stroke(width = 2.0.dp.toPx(), cap = StrokeCap.Round))
      }
    }

    Column(
      modifier =
        Modifier.minimumInteractiveComponentSize()
          .drawBehind {
            drawRect(color = if (isInspectBayOpen) Color(0xD807192E) else Color(0xBD020611))
            drawLine(
              brush = Brush.verticalGradient(listOf(Color(0xFF00F5D4), Color(0xFFFFD700))),
              start = Offset(0f, 0f),
              end = Offset(0f, size.height),
              strokeWidth = if (isInspectBayOpen) 2.6.dp.toPx() else 2.0.dp.toPx(),
            )
          }
          .clickable { onToggleOverwatchInspectBay() }
          .padding(start = 8.dp, end = 7.dp, top = 4.dp, bottom = 4.dp)
          .testTag("nexus_overwatch_terminal_readout"),
      verticalArrangement = Arrangement.spacedBy(2.5.dp),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
      ) {
        Text(
          text = modeHeaderLine,
          color = if (isInspectBayOpen) Color(0xFF00F5D4) else Color.White,
          fontSize = 9.8.sp,
          fontWeight = FontWeight.Black,
          fontFamily = FontFamily.Monospace,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.weight(1f, fill = false),
          style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
        )
        Text(
          text = if (isInspectBayOpen) "[<]" else "[>]",
          color = Color(0xFF38BDF8),
          fontSize = 8.2.sp,
          fontWeight = FontWeight.Black,
          fontFamily = FontFamily.Monospace,
          maxLines = 1,
          style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
        )
      }

      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        Text(
          text = "$countdownText LEFT ·",
          color = Color(0xFF00F5D4),
          fontSize = 8.6.sp,
          fontWeight = FontWeight.ExtraBold,
          fontFamily = FontFamily.Monospace,
          maxLines = 1,
          style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
        )
        ResonanceCrestIcon(sizeDp = 10.5.dp)
        Text(
          text = "${formatOverwatchResonance(liveAvgResonance)} ·",
          color = Color(0xFFFFD700),
          fontSize = 8.6.sp,
          fontWeight = FontWeight.Black,
          fontFamily = FontFamily.Monospace,
          maxLines = 1,
          style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
        )
        Text(
          text = if (humanCount == 1) "1 PILOT" else "$humanCount PILOTS",
          color = if (humanCount > 0) Color(0xFF38BDF8) else Color(0xFF94A3B8),
          fontSize = 8.4.sp,
          fontWeight = FontWeight.ExtraBold,
          fontFamily = FontFamily.Monospace,
          maxLines = 1,
          style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
        )
        if (overwatchZoom > 1.03f) {
          Box(
            modifier =
              Modifier.clickable { onResetOverwatchCamera() }
                .padding(horizontal = 3.dp, vertical = 1.dp)
                .testTag("nexus_overwatch_zoom_reset"),
          ) {
            Text(
              text = "[1x]",
              color = Color(0xFF00F5D4),
              fontSize = 8.0.sp,
              fontWeight = FontWeight.Black,
              fontFamily = FontFamily.Monospace,
              maxLines = 1,
            )
          }
        }
      }
    }

    Box(
      modifier =
        Modifier.minimumInteractiveComponentSize()
          .size(width = 48.dp, height = 48.dp)
          .clickable {
            val nextIdx = (currentIdx + 1) % totalInstances
            onSelectOverwatchKey(overwatchInstances[nextIdx].key)
          }
          .testTag("nexus_overwatch_next"),
      contentAlignment = Alignment.CenterStart,
    ) {
      Canvas(modifier = Modifier.padding(start = 8.dp).size(width = 16.dp, height = 9.dp)) {
        val p =
          Path().apply {
            moveTo(0f, 0f)
            lineTo(size.width * 0.5f, size.height)
            lineTo(size.width, 0f)
          }
        drawPath(p, color = Color(0xFF00F5D4), style = Stroke(width = 2.0.dp.toPx(), cap = StrokeCap.Round))
      }
    }
  }

  // 2. TOP-RIGHT OPEN SPACE: Primary [ RIFT IN > ] Launch Trigger + Active Ship Badge
  val riftInGlow =
    remember(isFocused) {
      Shadow(
        color = if (isFocused) Color.White else Color(0xFF00F5D4),
        offset = Offset.Zero,
        blurRadius = 9f,
      )
    }
  Column(
    modifier =
      Modifier.align(Alignment.TopEnd)
        .padding(end = openCornerAnchorDp, top = 3.dp)
        .widthIn(max = openCornerMaxWidthDp),
    horizontalAlignment = Alignment.End,
    verticalArrangement = Arrangement.spacedBy(3.dp),
  ) {
    Box(
      modifier =
        Modifier.minimumInteractiveComponentSize()
          .height(48.dp)
          .drawBehind {
            val w = size.width
            val h = size.height
            val cut = 9.dp.toPx()
            val p =
              Path().apply {
                moveTo(cut, 0f)
                lineTo(w, 0f)
                lineTo(w, h - cut)
                lineTo(w - cut, h)
                lineTo(0f, h)
                lineTo(0f, cut)
                close()
              }
            drawPath(
              path = p,
              brush = Brush.verticalGradient(listOf(Color(0xFF0D2D46), Color(0xFF091528), Color(0xFF030711))),
            )
            drawPath(
              path = p,
              brush = Brush.horizontalGradient(listOf(Color(0xFF00F5D4), Color(0xFFFFD700), Color(0xFF38BDF8))),
              style = Stroke(width = if (isFocused) 1.7.dp.toPx() else 1.35.dp.toPx()),
            )
          }
          .clickable { onQuickZoneIntoOverwatch(activeInstance) }
          .padding(horizontal = 12.dp)
          .testTag("nexus_overwatch_rift_in"),
      contentAlignment = Alignment.Center,
    ) {
      Text(
        text = "RIFT IN >",
        color = Color.White,
        fontSize = 11.4.sp,
        fontWeight = FontWeight.Black,
        fontFamily = FontFamily.Monospace,
        letterSpacing = 0.8.sp,
        maxLines = 1,
        style = TextStyle(shadow = riftInGlow, platformStyle = PlatformTextStyle(includeFontPadding = false)),
      )
    }

    Box(
      modifier =
        Modifier.clip(RoundedCornerShape(5.dp))
          .background(Color(0xCC060D1A))
          .border(0.9.dp, if (activeShipState == "CHARGED") Color(0xFF00F5D4) else Color(0xFF38BDF8), RoundedCornerShape(5.dp))
          .padding(horizontal = 7.dp, vertical = 3.dp),
    ) {
      Text(
        text = "$activeShipTitle • $activeShipState",
        color = if (activeShipState == "CHARGED") Color(0xFF00F5D4) else Color(0xFFFACC15),
        fontSize = 8.4.sp,
        fontWeight = FontWeight.Black,
        fontFamily = FontFamily.Monospace,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

/**
 * Signature Rift Hub Centerpiece — Circular Overwatch Viewport:
 */
@Composable
private fun RoundRipplingOverwatchAperture(
  modifier: Modifier,
  isFocused: Boolean,
  isTabletDeck: Boolean = false,
  drawHeadlessSafeZoneFallback: Boolean,
  activeInstance: OverwatchRiftInstance,
  overwatchPrompt: OverwatchCircularPrompt = OverwatchCircularPrompt.None,
  chargedShipId: Int = 0,
  bestShipId: Int = 0,
  activeShipId: Int = 1,
  activeCustomSlot: CustomShipLoadoutSlot = CustomShipLoadoutSlot(),
  activeCustomSlotId: String = "Frame A",
  activeCustomSlotName: String = "",
  pointBudget: Int = 100,
  activeDraggedFrameItem: DraggedRiftFrameItem? = null,
  dragPointerRootOffset: Offset = Offset.Zero,
  frameRenameDraft: String = "",
  onFrameRenameDraftChange: (String) -> Unit = {},
  currentUserId: String = "",
  callsign: String = "",
  syndicateTag: String = "",
  liveResonance: Int = 0,
  cloudLeaderboardEntries: List<LeaderboardEntry> = emptyList(),
  onlineHumanPilots: List<LivePilotState> = emptyList(),
  syndicateDirectory: List<SyndicateInfo> = emptyList(),
  shipSpinDeg: Float = 0f,
  fluxOrbitRad: Float = 0f,
  renameDraft: String = "",
  onRenameDraftChange: (String) -> Unit = {},
  resolveShipResonance: (Int) -> Int = { 0 },
  onChargeShip: (Int) -> Unit = {},
  onCustomizeRiftFrame: () -> Unit = {},
  onSaveCustomSlot: (CustomShipLoadoutSlot) -> Unit = {},
  onOpenFrameRenamePrompt: () -> Unit = {},
  onSaveFrameSlotRename: (String) -> Unit = {},
  onUpdatePrimarySocketBounds: (Rect) -> Unit = {},
  onUpdateSecondarySocketBounds: (Rect) -> Unit = {},
  onDismissPrompt: () -> Unit = {},
  onSquadUpPilot: (SquadronOnlinePilotItem) -> Unit = {},
  onAddFriendPilot: (SquadronOnlinePilotItem) -> Unit = {},
  onConfirmActionPrompt: (OverwatchCircularPrompt.ActionConfirm) -> Unit = {},
  onExecuteInfoStageAction: (OverwatchCircularPrompt.InfoStageInspect) -> Unit = {},
  onSaveCallsignRename: (String) -> Unit = {},
  onOverwatchTransformGesture:
    (centroidRootX: Float, centroidRootY: Float, panDxPx: Float, panDyPx: Float, zoomChange: Float) -> Unit,
  onResetOverwatchCamera: () -> Unit,
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
          animationSpec = infiniteRepeatable(tween(5200, easing = LinearEasing), RepeatMode.Restart),
          label = "round_ripple_rad",
        )
      p
    }

  val energyNorm = (activeInstance.combinedResonance / 1600f).coerceIn(0.24f, 1.0f)

  var lensBoxSize by remember { mutableStateOf(IntSize.Zero) }
  var gestureSurfaceRootOrigin by remember { mutableStateOf(Offset.Zero) }
  val lensRadiusFraction = 0.476f
  val gestureCircleDiameterDp =
    with(density) {
      val minDim = min(lensBoxSize.width, lensBoxSize.height).toFloat()
      val rPx = if (minDim > 8f) (minDim * lensRadiusFraction).coerceAtLeast(48f) else 132.dp.toPx()
      (rPx * 2f).toDp()
    }

  Box(
    modifier =
      modifier.onGloballyPositioned { coords ->
        lensBoxSize = coords.size
        val rootPos = coords.positionInRoot()
        val w = coords.size.width.toFloat()
        val h = coords.size.height.toFloat()
        if (w > 8f && h > 8f) {
          val cx = rootPos.x + w * 0.5f
          val cy = rootPos.y + h * 0.5f
          val radius = (min(w, h) * lensRadiusFraction).coerceAtLeast(48f)
          onApertureRootBoundsChanged(cx, cy, radius)
        }
      },
    contentAlignment = Alignment.Center,
  ) {
      Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        if (w <= 8f || h <= 8f) return@Canvas
        val cx = w * 0.5f
        val cy = h * 0.5f
        val center = Offset(cx, cy)
        val apertureR = (min(w, h) * lensRadiusFraction).coerceAtLeast(48f)

        // Headless JVM / Robolectric Orbital Overwatch Schematic Fallback
        if (drawHeadlessSafeZoneFallback) {
          drawCircle(color = Color(0xFF030711), radius = apertureR, center = center)
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
              brush =
                Brush.linearGradient(
                  listOf(Color(0xFFFFD700), Color(0xFF00F5D4)),
                  start = innerPt,
                  end = outerPt,
                ),
              start = innerPt,
              end = outerPt,
              strokeWidth = 1.6.dp.toPx(),
            )
          }
          drawCircle(color = Color(0xFF091528), radius = safeR * 0.42f, center = center)
          drawCircle(
            color = Color(0xFF00F5D4),
            radius = safeR * 0.20f,
            center = center,
            style = Stroke(width = 1.3.dp.toPx()),
          )
        }

        // 0. Subtle Perimeter Radial Reticle Lines & Range Ring Inside the Overwatch Aperture (0.84r .. 0.985r)
        drawCircle(
          color = Color(0x1E00F5D4),
          radius = apertureR * 0.91f,
          center = center,
          style = Stroke(width = 0.75.dp.toPx()),
        )
        for (ret in 0 until 12) {
          val rad = ret * (PI.toFloat() / 6f)
          val isMajorReticle = ret % 3 == 0
          val rIn = if (isMajorReticle) apertureR * 0.845f else apertureR * 0.905f
          val rOut = apertureR * 0.982f
          drawLine(
            color = if (isMajorReticle) Color(0x3000F5D4) else Color(0x1E38BDF8),
            start = Offset(cx + cos(rad) * rIn, cy + sin(rad) * rIn),
            end = Offset(cx + cos(rad) * rOut, cy + sin(rad) * rOut),
            strokeWidth = if (isMajorReticle) 0.95.dp.toPx() else 0.7.dp.toPx(),
            cap = StrokeCap.Round,
          )
        }

        // 1. Sleek Sculpted 2.5D Gunmetal/Cyan (#091528 / #0B192E + #00F5D4 / #38BDF8) Outer Radar Collar (1.002r .. 1.044r)
        val outerCollarMidR = apertureR * 1.023f
        val outerCollarWidth = (apertureR * 0.042f).coerceAtLeast(4.5.dp.toPx())
        drawCircle(
          color = Color(0xE002050B),
          radius = apertureR * 1.046f,
          center = Offset(cx, cy + 1.8.dp.toPx()),
          style = Stroke(width = 2.4.dp.toPx()),
        )
        drawCircle(
          brush =
            Brush.verticalGradient(
              colors =
                listOf(
                  Color(0xFF0B192E),
                  Color(0xFF091528),
                  Color(0xFF060D1A),
                  Color(0xFF030711),
                  Color(0xFF0B192E),
                ),
              startY = cy - apertureR * 1.046f,
              endY = cy + apertureR * 1.046f,
            ),
          radius = outerCollarMidR,
          center = center,
          style = Stroke(width = outerCollarWidth),
        )
        drawCircle(
          brush =
            Brush.verticalGradient(
              listOf(
                Color.White.copy(alpha = 0.44f),
                Color(0xFF00F5D4).copy(alpha = 0.58f),
                Color(0xFF38BDF8).copy(alpha = 0.36f),
                Color(0xFF030711),
              ),
              startY = cy - apertureR * 1.044f,
              endY = cy + apertureR * 1.044f,
            ),
          radius = apertureR * 1.044f,
          center = center,
          style = Stroke(width = 1.25.dp.toPx()),
        )

        // 2. Four Diagonal Optical Lock Clamps (45, 135, 225, 315 deg) + Port/Starboard Coupling Nodes
        val clampMidR = apertureR * 1.026f
        val clampRect = Rect(cx - clampMidR, cy - clampMidR, cx + clampMidR, cy + clampMidR)
        val outerClampLipR = apertureR * 1.044f
        for (diag in intArrayOf(45, 135, 225, 315)) {
          drawArc(
            color = Color(0xFF091528),
            startAngle = diag - 9f,
            sweepAngle = 18f,
            useCenter = false,
            topLeft = clampRect.topLeft,
            size = clampRect.size,
            style = Stroke(width = outerCollarWidth * 0.90f, cap = StrokeCap.Butt),
          )
          drawArc(
            color = if (diag == 135 || diag == 315) Color(0xFFFFD700).copy(alpha = 0.90f) else Color(0xFFFB7185).copy(alpha = 0.78f),
            startAngle = diag - 7.5f,
            sweepAngle = 15f,
            useCenter = false,
            topLeft = Offset(cx - outerClampLipR, cy - outerClampLipR),
            size = Size(outerClampLipR * 2f, outerClampLipR * 2f),
            style = Stroke(width = 1.35.dp.toPx(), cap = StrokeCap.Round),
          )
        }
        for (wingSide in intArrayOf(-1, 1)) {
          val nodeX = cx + wingSide * (apertureR * 1.032f)
          drawCircle(color = Color(0xFF060D1A), radius = 3.8.dp.toPx(), center = Offset(nodeX, cy))
          drawCircle(
            color = Color(0xFF00F5D4),
            radius = 3.8.dp.toPx(),
            center = Offset(nodeX, cy),
            style = Stroke(width = 1.2.dp.toPx()),
          )
          drawCircle(color = Color.White, radius = 1.4.dp.toPx(), center = Offset(nodeX, cy))
        }

        // 3. Harmonic Polar Resonance Matrix (1.006r .. 1.030r) Reacting to Combined Sector Resonance
        val numBands = 2
        val numSegs = 44
        val segSweepDeg = 360f / numSegs.toFloat()
        val segGapDeg = 1.7f
        val lightDirRad = ripplePhase * 0.75f
        val secondaryLightRad = -ripplePhase * 0.5f + 2.1f
        for (band in 0 until numBands) {
          val rMid = apertureR * (1.028f - band * 0.015f)
          val bandW = (apertureR * 0.011f).coerceAtLeast(1.1f)
          val arcRect = Rect(cx - rMid, cy - rMid, cx + rMid, cy + rMid)
          for (seg in 0 until numSegs) {
            val segAngleRad = ((seg + 0.5f) / numSegs.toFloat()) * (2f * PI.toFloat())
            val inwardWave =
              0.5f + 0.5f * sin(band * 1.1f - ripplePhase * 3.0f + sin(segAngleRad * 3f) * 0.5f)
            val primaryLit = (0.5f + 0.5f * cos(segAngleRad - lightDirRad)).pow(2.0f)
            val goldLit = (0.5f + 0.5f * cos(segAngleRad - secondaryLightRad)).pow(2.4f)
            val alpha =
              ((0.15f + 0.50f * primaryLit + 0.34f * goldLit) * inwardWave * (0.58f + 0.42f * energyNorm))
                .coerceIn(0.06f, 0.86f)
            val col =
              when {
                goldLit > primaryLit * 1.06f -> Color(0xFFFFD700)
                band == 1 -> Color(0xFF00F5D4)
                else -> Color(0xFF38BDF8)
              }
            drawArc(
              color = col.copy(alpha = alpha),
              startAngle = seg * segSweepDeg + segGapDeg * 0.5f,
              sweepAngle = (segSweepDeg - segGapDeg).coerceAtLeast(1.3f),
              useCenter = false,
              topLeft = arcRect.topLeft,
              size = arcRect.size,
              style = Stroke(width = bandW, cap = StrokeCap.Butt),
            )
          }
        }

        // 4. Calibrated 72-Tick Radar Azimuth Dial (4 Solar-Gold Cardinals, 8 Electric-Cyan Bearings, 60 Vernier Ticks)
        for (t in 0 until 72) {
          val ang = (t * PI / 36.0).toFloat()
          val isCardinal = t % 18 == 0
          val isSubCardinal = t % 6 == 0
          val r0 =
            when {
              isCardinal -> apertureR * 0.956f
              isSubCardinal -> apertureR * 0.972f
              else -> apertureR * 0.985f
            }
          val r1 =
            when {
              isCardinal -> apertureR * 1.022f
              isSubCardinal -> apertureR * 1.012f
              else -> apertureR * 1.003f
            }
          val col =
            when {
              isCardinal -> Color(0xFFFFD700).copy(alpha = 0.95f)
              isSubCardinal -> Color(0xFF00F5D4).copy(alpha = 0.76f)
              else -> Color(0xFF38BDF8).copy(alpha = 0.36f)
            }
          val sw =
            when {
              isCardinal -> 1.65.dp.toPx()
              isSubCardinal -> 1.15.dp.toPx()
              else -> 0.65.dp.toPx()
            }
          drawLine(
            color = col,
            start = Offset(cx + cos(ang) * r0, cy + sin(ang) * r0),
            end = Offset(cx + cos(ang) * r1, cy + sin(ang) * r1),
            strokeWidth = sw,
            cap = StrokeCap.Round,
          )
        }

        // 5. Rotating Anodized Inner Aperture Lip + Top-Left Optical Specular Meniscus Highlights
        rotate(degrees = (lightDirRad * 180f / PI.toFloat())) {
          drawCircle(
            brush =
              Brush.sweepGradient(
                0.00f to Color(0xFF00F5D4).copy(alpha = if (isFocused) 0.98f else 0.88f),
                0.24f to Color(0xFF38BDF8).copy(alpha = 0.54f),
                0.50f to Color(0xFFFFD700).copy(alpha = 0.92f),
                0.76f to Color(0xFF091528).copy(alpha = 0.64f),
                1.00f to Color(0xFF00F5D4).copy(alpha = if (isFocused) 0.98f else 0.88f),
                center = center,
              ),
            radius = apertureR,
            center = center,
            style = Stroke(width = if (isFocused) 2.25.dp.toPx() else 1.8.dp.toPx()),
          )
        }

        val meniscusShiftDeg = if (headless) 0f else sin(ripplePhase) * 5f
        val meniscusLightDeg = -134f + meniscusShiftDeg
        val dispersionR = apertureR * 0.975f
        drawArc(
          color = Color(0xFF00F5D4).copy(alpha = if (isFocused) 0.42f else 0.32f),
          startAngle = meniscusLightDeg - 50f,
          sweepAngle = 100f,
          useCenter = false,
          topLeft = Offset(cx - dispersionR, cy - dispersionR),
          size = Size(dispersionR * 2f, dispersionR * 2f),
          style = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round),
        )
        val specularR = apertureR * 0.989f
        drawArc(
          color = Color(0xFFF8FAFC).copy(alpha = if (isFocused) 0.92f else 0.80f),
          startAngle = meniscusLightDeg - 36f,
          sweepAngle = 72f,
          useCenter = false,
          topLeft = Offset(cx - specularR, cy - specularR),
          size = Size(specularR * 2f, specularR * 2f),
          style = Stroke(width = 1.4.dp.toPx(), cap = StrokeCap.Round),
        )
        drawArc(
          color = Color.White.copy(alpha = if (isFocused) 0.98f else 0.90f),
          startAngle = meniscusLightDeg - 10f,
          sweepAngle = 20f,
          useCenter = false,
          topLeft = Offset(cx - specularR, cy - specularR),
          size = Size(specularR * 2f, specularR * 2f),
          style = Stroke(width = 2.1.dp.toPx(), cap = StrokeCap.Round),
        )
        val causticOuterR = apertureR * 0.950f
        drawArc(
          color = Color(0xFFFFD700).copy(alpha = 0.28f + 0.16f * energyNorm),
          startAngle = meniscusLightDeg + 150f,
          sweepAngle = 60f,
          useCenter = false,
          topLeft = Offset(cx - causticOuterR, cy - causticOuterR),
          size = Size(causticOuterR * 2f, causticOuterR * 2f),
          style = Stroke(width = 1.25.dp.toPx(), cap = StrokeCap.Round),
        )
      }

      // Interactive Circular Overwatch Touch Surface + Confined Circular Glass Prompts
      Box(
        modifier =
          Modifier.size(gestureCircleDiameterDp)
            .clip(CircleShape)
            .onGloballyPositioned { coords ->
              gestureSurfaceRootOrigin = coords.positionInRoot()
            }
            .pointerInput(activeInstance.key, overwatchPrompt) {
              if (overwatchPrompt == OverwatchCircularPrompt.None) {
                detectTapGestures(
                  onDoubleTap = { onResetOverwatchCamera() },
                )
              }
            }
            .pointerInput(activeInstance.key, overwatchPrompt) {
              if (overwatchPrompt == OverwatchCircularPrompt.None) {
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
            }
            .testTag("nexus_overwatch_gesture_surface"),
        contentAlignment = Alignment.Center,
      ) {
        if (overwatchPrompt != OverwatchCircularPrompt.None) {
          OverwatchCircularGlassPromptOverlay(
            prompt = overwatchPrompt,
            isTabletDeck = isTabletDeck,
            chargedShipId = chargedShipId,
            bestShipId = bestShipId,
            activeShipId = activeShipId,
            activeCustomSlot = activeCustomSlot,
            activeCustomSlotId = activeCustomSlotId,
            activeCustomSlotName = activeCustomSlotName,
            pointBudget = pointBudget,
            activeDraggedFrameItem = activeDraggedFrameItem,
            dragPointerRootOffset = dragPointerRootOffset,
            frameRenameDraft = frameRenameDraft,
            onFrameRenameDraftChange = onFrameRenameDraftChange,
            currentUserId = currentUserId,
            callsign = callsign,
            syndicateTag = syndicateTag,
            liveResonance = liveResonance,
            cloudLeaderboardEntries = cloudLeaderboardEntries,
            onlineHumanPilots = onlineHumanPilots,
            syndicateDirectory = syndicateDirectory,
            shipSpinDeg = shipSpinDeg,
            fluxOrbitRad = fluxOrbitRad,
            renameDraft = renameDraft,
            onRenameDraftChange = onRenameDraftChange,
            resolveShipResonance = resolveShipResonance,
            onChargeShip = onChargeShip,
            onCustomizeRiftFrame = onCustomizeRiftFrame,
            onSaveCustomSlot = onSaveCustomSlot,
            onOpenFrameRenamePrompt = onOpenFrameRenamePrompt,
            onSaveFrameSlotRename = onSaveFrameSlotRename,
            onUpdatePrimarySocketBounds = onUpdatePrimarySocketBounds,
            onUpdateSecondarySocketBounds = onUpdateSecondarySocketBounds,
            onDismissPrompt = onDismissPrompt,
            onSquadUpPilot = onSquadUpPilot,
            onAddFriendPilot = onAddFriendPilot,
            onConfirmActionPrompt = onConfirmActionPrompt,
            onExecuteInfoStageAction = onExecuteInfoStageAction,
            onSaveCallsignRename = onSaveCallsignRename,
          )
        }
      }
    }
}

@Composable
private fun OverwatchCircularGlassPromptOverlay(
  prompt: OverwatchCircularPrompt,
  isTabletDeck: Boolean,
  chargedShipId: Int,
  bestShipId: Int,
  activeShipId: Int = 1,
  activeCustomSlot: CustomShipLoadoutSlot = CustomShipLoadoutSlot(),
  activeCustomSlotId: String = "Frame A",
  activeCustomSlotName: String = "",
  pointBudget: Int = 100,
  activeDraggedFrameItem: DraggedRiftFrameItem? = null,
  dragPointerRootOffset: Offset = Offset.Zero,
  frameRenameDraft: String = "",
  onFrameRenameDraftChange: (String) -> Unit = {},
  currentUserId: String = "",
  callsign: String = "",
  syndicateTag: String = "",
  liveResonance: Int = 0,
  cloudLeaderboardEntries: List<LeaderboardEntry> = emptyList(),
  onlineHumanPilots: List<LivePilotState> = emptyList(),
  syndicateDirectory: List<SyndicateInfo> = emptyList(),
  shipSpinDeg: Float,
  fluxOrbitRad: Float,
  renameDraft: String = "",
  onRenameDraftChange: (String) -> Unit = {},
  resolveShipResonance: (Int) -> Int,
  onChargeShip: (Int) -> Unit,
  onCustomizeRiftFrame: () -> Unit = {},
  onSaveCustomSlot: (CustomShipLoadoutSlot) -> Unit = {},
  onOpenFrameRenamePrompt: () -> Unit = {},
  onSaveFrameSlotRename: (String) -> Unit = {},
  onUpdatePrimarySocketBounds: (Rect) -> Unit = {},
  onUpdateSecondarySocketBounds: (Rect) -> Unit = {},
  onDismissPrompt: () -> Unit,
  onSquadUpPilot: (SquadronOnlinePilotItem) -> Unit,
  onAddFriendPilot: (SquadronOnlinePilotItem) -> Unit,
  onConfirmActionPrompt: (OverwatchCircularPrompt.ActionConfirm) -> Unit = {},
  onExecuteInfoStageAction: (OverwatchCircularPrompt.InfoStageInspect) -> Unit = {},
  onSaveCallsignRename: (String) -> Unit = {},
) {
  val density = LocalDensity.current
  Box(
    modifier =
      Modifier.fillMaxSize()
        .clip(CircleShape)
        .drawBehind {
          val r = size.minDimension * 0.5f
          drawCircle(
            brush =
              Brush.radialGradient(
                colors =
                  listOf(
                    Color(0xEB091528),
                    Color(0xF2060D1A),
                    Color(0xFA02050B),
                  ),
                center = center,
                radius = r.coerceAtLeast(12f),
              ),
            radius = r,
            center = center,
          )
          val innerRingR = r * 0.94f
          drawCircle(
            brush =
              Brush.sweepGradient(
                0.00f to Color(0xFF00F5D4).copy(alpha = 0.85f),
                0.35f to Color(0xFF38BDF8).copy(alpha = 0.48f),
                0.65f to Color(0xFFFFD700).copy(alpha = 0.72f),
                1.00f to Color(0xFF00F5D4).copy(alpha = 0.85f),
                center = center,
              ),
            radius = innerRingR,
            center = center,
            style = Stroke(width = 1.35.dp.toPx()),
          )
          drawArc(
            color = Color.White.copy(alpha = 0.65f),
            startAngle = -152f,
            sweepAngle = 64f,
            useCenter = false,
            topLeft = Offset(center.x - innerRingR, center.y - innerRingR),
            size = Size(innerRingR * 2f, innerRingR * 2f),
            style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round),
          )
        }
        .padding(all = 16.dp)
        .testTag("overwatch_circular_glass_prompt"),
    contentAlignment = Alignment.Center,
  ) {
    when (prompt) {
      OverwatchCircularPrompt.None -> Unit
      OverwatchCircularPrompt.ResonanceLeaderboard -> {
        OverwatchResonanceLeaderboardView(
          currentUserId = currentUserId,
          localCallsign = callsign,
          localSyndicateTag = syndicateTag,
          localLiveResonance = liveResonance,
          localActiveShipId = activeShipId,
          resolveLocalShipResonance = resolveShipResonance,
          cloudLeaderboardEntries = cloudLeaderboardEntries,
          onlineHumanPilots = onlineHumanPilots,
          syndicateDirectory = syndicateDirectory,
          onDismissPrompt = onDismissPrompt,
        )
      }
      is OverwatchCircularPrompt.ShipInspect -> {
        val ship = remember(prompt.shipId, RiftSpriteBaker.customShip9Version) {
          RiftCodex.getShipById(prompt.shipId.coerceIn(1, 10))
        }
        val cleanName =
          remember(ship.id, ship.name) {
            if (ship.id == 10) "RIFT FRAME"
            else ship.name.replace(Regex("^[0-9]+-"), "").trim().uppercase(Locale.US)
          }
        val customFrameSubline =
          remember(ship.id, activeCustomSlotId, activeCustomSlotName) {
            if (ship.id == 10) {
              val cleanCustom = activeCustomSlotName.trim()
              if (cleanCustom.isNotBlank() && !cleanCustom.equals("Rift Frame", ignoreCase = true)) {
                "${activeCustomSlotId.uppercase(Locale.US)} • ${cleanCustom.uppercase(Locale.US)}"
              } else {
                activeCustomSlotId.uppercase(Locale.US)
              }
            } else {
              ""
            }
          }
        val shipRes = resolveShipResonance(ship.id)
        val isAlreadyCharged = chargedShipId == ship.id
        val isBest = bestShipId in 1..10 && bestShipId == ship.id
        val chargeFontSp = 10.6.sp
        val fluxExactDp = with(density) { chargeFontSp.toDp() }

        Column(
          modifier = Modifier.fillMaxWidth(0.84f),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
          Spinning25dShipWithOrbitingFlux(
            shipId = ship.id,
            shipSpinDeg = shipSpinDeg,
            fluxOrbitRad = fluxOrbitRad,
            isCharged = isAlreadyCharged,
            isBestShip = isBest,
            isSelected = true,
            sizeDp = 50.dp,
          )

          Text(
            text = cleanName,
            color = Color.White,
            fontSize = 11.4.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
          )

          if (customFrameSubline.isNotBlank()) {
            Text(
              text = customFrameSubline,
              color = Color(0xFFC084FC),
              fontSize = 8.4.sp,
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.Monospace,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
            )
          }

          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            Text(
              text = ship.role.uppercase(Locale.US),
              color = Color(0xFF38BDF8),
              fontSize = 8.4.sp,
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.Monospace,
              maxLines = 1,
            )
            Text("•", color = Color(0xFF64748B), fontSize = 8.0.sp)
            ResonanceCrestIcon(sizeDp = 12.dp)
            Text(
              text = formatOverwatchResonance(shipRes),
              color = Color(0xFFFFD700),
              fontSize = 9.2.sp,
              fontWeight = FontWeight.Black,
              fontFamily = FontFamily.Monospace,
              maxLines = 1,
            )
          }

          Text(
            text = "${ship.primaryTitle} + ${ship.secondaryTitle}",
            color = Color(0xFFCBD5E1),
            fontSize = 8.0.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )

          // 6 Core Performance Bars (Firepower, Flux Pool, Flux Regen, Top Speed, Thrust, Turn Rate)
          val firepowerScore =
            remember(ship.primaryDamage, ship.primaryStreams, ship.primaryCooldown, ship.bombDamage) {
              val dps = (ship.primaryDamage * ship.primaryStreams) / ship.primaryCooldown.coerceAtLeast(0.08f)
              val combined = dps + if (ship.hasBomb) ship.bombDamage * 0.25f else 0f
              (combined / 1450f).coerceIn(0.22f, 1f)
            }
          Row(
            modifier = Modifier.fillMaxWidth(0.92f),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            CircularPromptStatBar(
              modifier = Modifier.weight(1f),
              label = "FIRE",
              fraction = firepowerScore,
              color = Color(0xFFFB7185),
            )
            CircularPromptStatBar(
              modifier = Modifier.weight(1f),
              label = "FLUX",
              fraction = (ship.maxFluxPts / 25f).coerceIn(0.18f, 1f),
              color = Color(0xFF00F5D4),
            )
          }
          Row(
            modifier = Modifier.fillMaxWidth(0.92f),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            CircularPromptStatBar(
              modifier = Modifier.weight(1f),
              label = "REGEN",
              fraction = (ship.rechargePts / 25f).coerceIn(0.18f, 1f),
              color = Color(0xFF38BDF8),
            )
            CircularPromptStatBar(
              modifier = Modifier.weight(1f),
              label = "SPEED",
              fraction = (ship.speedPts / 25f).coerceIn(0.18f, 1f),
              color = Color(0xFFFACC15),
            )
          }
          Row(
            modifier = Modifier.fillMaxWidth(0.92f),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            CircularPromptStatBar(
              modifier = Modifier.weight(1f),
              label = "THRUST",
              fraction = (ship.thrustPts / 25f).coerceIn(0.18f, 1f),
              color = Color(0xFFA855F7),
            )
            CircularPromptStatBar(
              modifier = Modifier.weight(1f),
              label = "TURN",
              fraction = (ship.turnPts / 25f).coerceIn(0.18f, 1f),
              color = Color(0xFF34D399),
            )
          }

          if (ship.id == 10) {
            Box(
              modifier =
                Modifier.minimumInteractiveComponentSize()
                  .height(48.dp)
                  .clickable { onCustomizeRiftFrame() }
                  .padding(vertical = 4.dp)
                  .testTag("overwatch_customize_rift_frame_btn"),
              contentAlignment = Alignment.Center,
            ) {
              Box(
                modifier =
                  Modifier.clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF1A1038))
                    .border(1.1.dp, Color(0xFFC084FC), RoundedCornerShape(14.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
              ) {
                Text(
                  text = "CUSTOMIZE FRAME >",
                  color = Color(0xFFE9D5FF),
                  fontSize = 8.4.sp,
                  fontWeight = FontWeight.Black,
                  fontFamily = FontFamily.Monospace,
                  maxLines = 1,
                )
              }
            }
          }

          // Primary [ CHARGE (Flux Icon) ] (hidden in Codex inspect-only preview) + [ CLOSE ] Row (>= 48dp touch targets)
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            if (!prompt.hideChargeButton) {
              Box(
                modifier =
                  Modifier.minimumInteractiveComponentSize()
                    .height(48.dp)
                    .clickable { onChargeShip(ship.id) }
                    .padding(vertical = 5.dp)
                    .testTag("overwatch_ship_charge_button"),
                contentAlignment = Alignment.Center,
              ) {
                Row(
                  modifier =
                    Modifier.clip(RoundedCornerShape(18.dp))
                      .background(
                        Brush.horizontalGradient(
                          if (isAlreadyCharged) listOf(Color(0xFF0A363D), Color(0xFF091528))
                          else listOf(Color(0xFF0D3554), Color(0xFF0B192E))
                        )
                      )
                      .border(
                        1.35.dp,
                        Brush.horizontalGradient(listOf(Color(0xFF00F5D4), Color(0xFFFFD700))),
                        RoundedCornerShape(18.dp),
                      )
                      .padding(horizontal = 12.dp, vertical = 6.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                  Text(
                    text = if (isAlreadyCharged) "CHARGED" else "CHARGE",
                    color = Color.White,
                    fontSize = chargeFontSp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.6.sp,
                    maxLines = 1,
                    style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
                  )
                  FluxToroidIcon(sizeDp = fluxExactDp)
                }
              }
            }

            Box(
              modifier =
                Modifier.minimumInteractiveComponentSize()
                  .height(48.dp)
                  .clickable { onDismissPrompt() }
                  .padding(vertical = 5.dp)
                  .testTag("overwatch_prompt_close_button"),
              contentAlignment = Alignment.Center,
            ) {
              Box(
                modifier =
                  Modifier.clip(CircleShape)
                    .background(Color(0xCC060D1A))
                    .border(1.dp, Color(0xFF64748B), CircleShape)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center,
              ) {
                Text(
                  text = "CLOSE",
                  color = Color(0xFFCBD5E1),
                  fontSize = 8.0.sp,
                  fontWeight = FontWeight.Black,
                  fontFamily = FontFamily.Monospace,
                )
              }
            }
          }
        }
      }

      is OverwatchCircularPrompt.PilotInspect -> {
        val pilot = prompt.pilot
        val tagPrefix = if (pilot.syndicateTag.isNotBlank()) "[${pilot.syndicateTag}] " else ""
        Column(
          modifier = Modifier.fillMaxWidth(0.84f),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(if (isTabletDeck) 5.dp else 3.dp),
        ) {
          Spinning25dShipWithOrbitingFlux(
            shipId = pilot.lockedShipId,
            shipSpinDeg = shipSpinDeg,
            fluxOrbitRad = fluxOrbitRad,
            isCharged = pilot.isInSquadron,
            isBestShip = pilot.bestShipId == pilot.lockedShipId,
            isSelected = true,
            sizeDp = if (isTabletDeck) 64.dp else 46.dp,
          )

          Text(
            text = "$tagPrefix${pilot.callsign}",
            color = Color.White,
            fontSize = if (isTabletDeck) 11.4.sp else 9.8.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )

          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            ResonanceCrestIcon(sizeDp = 12.dp)
            Text(
              text = formatOverwatchResonance(pilot.resonance),
              color = Color(0xFFFFD700),
              fontSize = if (isTabletDeck) 9.6.sp else 8.6.sp,
              fontWeight = FontWeight.Black,
              fontFamily = FontFamily.Monospace,
            )
            Text("•", color = Color(0xFF64748B), fontSize = 7.6.sp)
            Text(
              text = pilot.sectorLabel.ifBlank { "RIFT HUB" },
              color = Color(0xFF00F5D4),
              fontSize = if (isTabletDeck) 8.4.sp else 7.4.sp,
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.Monospace,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }

          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
          ) {
            Box(
              modifier =
                Modifier.minimumInteractiveComponentSize()
                  .height(48.dp)
                  .clickable { onSquadUpPilot(pilot) }
                  .padding(vertical = 5.dp)
                  .testTag("overwatch_pilot_squad_up_button"),
              contentAlignment = Alignment.Center,
            ) {
              Box(
                modifier =
                  Modifier.clip(RoundedCornerShape(16.dp))
                    .background(Brush.horizontalGradient(listOf(Color(0xFF0D3554), Color(0xFF091528))))
                    .border(1.25.dp, Color(0xFF00F5D4), RoundedCornerShape(16.dp))
                    .padding(horizontal = 11.dp, vertical = 6.dp),
              ) {
                Text(
                  text = if (pilot.isInSquadron) "IN SQUADRON" else "SQUAD UP",
                  color = Color.White,
                  fontSize = if (isTabletDeck) 9.8.sp else 8.6.sp,
                  fontWeight = FontWeight.Black,
                  fontFamily = FontFamily.Monospace,
                )
              }
            }

            if (!pilot.isFriend) {
              Box(
                modifier =
                  Modifier.minimumInteractiveComponentSize()
                    .height(48.dp)
                    .clickable { onAddFriendPilot(pilot) }
                    .padding(vertical = 5.dp)
                    .testTag("overwatch_pilot_add_friend_button"),
                contentAlignment = Alignment.Center,
              ) {
                Box(
                  modifier =
                    Modifier.clip(RoundedCornerShape(16.dp))
                      .background(Color(0xFF091528))
                      .border(1.dp, Color(0xFFFACC15), RoundedCornerShape(16.dp))
                      .padding(horizontal = 9.dp, vertical = 6.dp),
                ) {
                  Text(
                    text = "+ FRIEND",
                    color = Color(0xFFFFD700),
                    fontSize = 8.0.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                  )
                }
              }
            }

            Box(
              modifier =
                Modifier.minimumInteractiveComponentSize()
                  .height(48.dp)
                  .clickable { onDismissPrompt() }
                  .padding(vertical = 5.dp)
                  .testTag("overwatch_pilot_close_button"),
              contentAlignment = Alignment.Center,
            ) {
              Box(
                modifier =
                  Modifier.clip(CircleShape)
                    .background(Color(0xCC060D1A))
                    .border(1.dp, Color(0xFF64748B), CircleShape)
                    .padding(horizontal = 9.dp, vertical = 6.dp),
              ) {
                Text(
                  text = "CLOSE",
                  color = Color(0xFFCBD5E1),
                  fontSize = 7.8.sp,
                  fontWeight = FontWeight.Black,
                  fontFamily = FontFamily.Monospace,
                )
              }
            }
          }
        }
      }

      is OverwatchCircularPrompt.ActionConfirm -> {
        val accentCol = Color(prompt.accentHex)
        Column(
          modifier = Modifier.fillMaxWidth(0.84f),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(if (isTabletDeck) 6.dp else 4.dp),
        ) {
          Text(
            text = prompt.kicker.uppercase(Locale.US),
            color = accentCol,
            fontSize = if (isTabletDeck) 8.8.sp else 7.6.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 0.7.sp,
            maxLines = 1,
          )
          Text(
            text = prompt.title,
            color = Color.White,
            fontSize = if (isTabletDeck) 11.4.sp else 9.8.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
          )
          Text(
            text = prompt.subtitle,
            color = Color(0xFFCBD5E1),
            fontSize = if (isTabletDeck) 8.4.sp else 7.4.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.Center,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
          )
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Box(
              modifier =
                Modifier.minimumInteractiveComponentSize()
                  .height(48.dp)
                  .clickable { onConfirmActionPrompt(prompt) }
                  .padding(vertical = 5.dp)
                  .testTag("overwatch_action_confirm_button"),
              contentAlignment = Alignment.Center,
            ) {
              Box(
                modifier =
                  Modifier.clip(RoundedCornerShape(16.dp))
                    .background(Brush.horizontalGradient(listOf(Color(0xFF0D3554), Color(0xFF091528))))
                    .border(1.25.dp, accentCol, RoundedCornerShape(16.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
              ) {
                Text(
                  text = prompt.confirmLabel,
                  color = Color.White,
                  fontSize = if (isTabletDeck) 9.8.sp else 8.6.sp,
                  fontWeight = FontWeight.Black,
                  fontFamily = FontFamily.Monospace,
                )
              }
            }
            Box(
              modifier =
                Modifier.minimumInteractiveComponentSize()
                  .height(48.dp)
                  .clickable { onDismissPrompt() }
                  .padding(vertical = 5.dp)
                  .testTag("overwatch_action_cancel_button"),
              contentAlignment = Alignment.Center,
            ) {
              Box(
                modifier =
                  Modifier.clip(CircleShape)
                    .background(Color(0xCC060D1A))
                    .border(1.dp, Color(0xFF64748B), CircleShape)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
              ) {
                Text(
                  text = "CANCEL",
                  color = Color(0xFFCBD5E1),
                  fontSize = 7.8.sp,
                  fontWeight = FontWeight.Black,
                  fontFamily = FontFamily.Monospace,
                )
              }
            }
          }
        }
      }

      is OverwatchCircularPrompt.CallsignRename -> {
        Column(
          modifier = Modifier.fillMaxWidth(0.84f),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(if (isTabletDeck) 6.dp else 4.dp),
        ) {
          Text(
            text = "PILOT CALLSIGN",
            color = Color(0xFF00F5D4),
            fontSize = if (isTabletDeck) 8.8.sp else 7.6.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
          )
          Box(
            modifier =
              Modifier.fillMaxWidth(0.86f)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xEE02050B))
                .border(1.1.dp, Color(0xFF00F5D4), RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 7.dp),
          ) {
            BasicTextField(
              value = renameDraft,
              onValueChange = { raw ->
                onRenameDraftChange(raw.uppercase(Locale.US).replace(Regex("[^A-Z0-9_\\-]"), "").take(14))
              },
              singleLine = true,
              cursorBrush = SolidColor(Color(0xFF00F5D4)),
              textStyle =
                TextStyle(
                  color = Color.White,
                  fontSize = if (isTabletDeck) 11.2.sp else 9.8.sp,
                  fontWeight = FontWeight.Black,
                  fontFamily = FontFamily.Monospace,
                  textAlign = TextAlign.Center,
                ),
              modifier = Modifier.fillMaxWidth().testTag("overwatch_callsign_rename_input"),
            )
          }
          Text(
            text = "2-14 CHARS • A-Z, 0-9, -, _",
            color = Color(0xFF94A3B8),
            fontSize = 7.2.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
          )
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Box(
              modifier =
                Modifier.minimumInteractiveComponentSize()
                  .height(48.dp)
                  .clickable { onSaveCallsignRename(renameDraft) }
                  .padding(vertical = 5.dp)
                  .testTag("overwatch_callsign_save_button"),
              contentAlignment = Alignment.Center,
            ) {
              Box(
                modifier =
                  Modifier.clip(RoundedCornerShape(16.dp))
                    .background(Brush.horizontalGradient(listOf(Color(0xFF0D3554), Color(0xFF091528))))
                    .border(1.25.dp, Color(0xFF00F5D4), RoundedCornerShape(16.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
              ) {
                Text(
                  text = "SAVE NAME",
                  color = Color.White,
                  fontSize = if (isTabletDeck) 9.6.sp else 8.4.sp,
                  fontWeight = FontWeight.Black,
                  fontFamily = FontFamily.Monospace,
                )
              }
            }
            Box(
              modifier =
                Modifier.minimumInteractiveComponentSize()
                  .height(48.dp)
                  .clickable { onDismissPrompt() }
                  .padding(vertical = 5.dp)
                  .testTag("overwatch_callsign_cancel_button"),
              contentAlignment = Alignment.Center,
            ) {
              Box(
                modifier =
                  Modifier.clip(CircleShape)
                    .background(Color(0xCC060D1A))
                    .border(1.dp, Color(0xFF64748B), CircleShape)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
              ) {
                Text(
                  text = "CANCEL",
                  color = Color(0xFFCBD5E1),
                  fontSize = 8.4.sp,
                  fontWeight = FontWeight.Black,
                  fontFamily = FontFamily.Monospace,
                )
              }
            }
          }
        }
      }

      is OverwatchCircularPrompt.InfoStageInspect -> {
        val accentCol = Color(prompt.accentHex)
        val stationBmp =
          remember(RiftSpriteBaker.spriteVisualRevision, RiftVisualForgeState.visualRevision) {
            RiftSpriteBaker.safeZoneStationBitmap.asImageBitmap()
          }
        Column(
          modifier = Modifier.fillMaxWidth(0.86f).testTag("overwatch_info_stage_overlay"),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(if (isTabletDeck) 5.dp else 3.5.dp),
        ) {
          Text(
            text = prompt.kicker.uppercase(Locale.US),
            color = accentCol,
            fontSize = if (isTabletDeck) 9.2.sp else 8.2.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 0.7.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )

          when {
            prompt.shipId in 1..10 -> {
              val hasLiveCosmeticPreview =
                prompt.previewHullSkinId != null ||
                  prompt.previewFinisherId != null ||
                  prompt.previewWarpStyleId != null ||
                  prompt.previewHaloStyleId != null
              Spinning25dShipWithOrbitingFlux(
                shipId = prompt.shipId,
                shipSpinDeg = shipSpinDeg,
                fluxOrbitRad = fluxOrbitRad,
                isCharged = chargedShipId == prompt.shipId,
                isBestShip = bestShipId in 1..10 && bestShipId == prompt.shipId,
                isSelected = true,
                sizeDp = if (hasLiveCosmeticPreview) 72.dp else 52.dp,
                trimStyleOverride = prompt.previewHullSkinId,
                previewFinisherId = prompt.previewFinisherId,
                previewWarpStyleId = prompt.previewWarpStyleId,
                previewHaloStyleId = prompt.previewHaloStyleId,
              )
            }
            prompt.buffTypeIndex != Int.MIN_VALUE -> {
              BuffIconPreview(
                buffType = prompt.buffTypeIndex,
                sizeDp = 48,
                accentColor = accentCol,
              )
            }
            prompt.riftAnomalyId.isNotBlank() -> {
              val animPhase = ((fluxOrbitRad / (2.0 * PI)).toFloat() % 1f).let { if (it < 0f) it + 1f else it }
              Canvas(modifier = Modifier.size(72.dp)) {
                drawIntoCanvas { canvas ->
                  RiftSpriteBaker.drawAnimatedCodexRiftPreview(
                    canvas = canvas.nativeCanvas,
                    riftId = prompt.riftAnomalyId,
                    cx = size.width * 0.5f,
                    cy = size.height * 0.5f,
                    radiusPx = min(size.width, size.height) * 0.40f,
                    pulsePhase = animPhase,
                    spinAngle = shipSpinDeg * 0.01745f,
                  )
                }
              }
            }
            prompt.codexModeIndex >= 0 -> {
              RankedModeDistinctEmblem(
                modeIndex = prompt.codexModeIndex,
                sizeDp = 50.dp,
              )
            }
            prompt.showStationPreview -> {
              Image(
                bitmap = stationBmp,
                contentDescription = "Rift Station Safe Harbor",
                contentScale = ContentScale.Fit,
                modifier =
                  Modifier.size(52.dp)
                    .graphicsLayer { rotationZ = shipSpinDeg * 0.25f },
              )
            }
            prompt.codexBasicIndex >= 0 -> {
              CodexBasicEmblem(
                basicIndex = prompt.codexBasicIndex,
                sizeDp = 50.dp,
              )
            }
            prompt.showResonanceCrest -> {
              ResonanceCrestIcon(sizeDp = 44.dp)
            }
          }

          Text(
            text = prompt.title,
            color = Color.White,
            fontSize = 11.4.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
          )

          if (prompt.badge.isNotBlank()) {
            Row(
              modifier =
                Modifier.clip(RoundedCornerShape(5.dp))
                  .background(accentCol.copy(alpha = 0.16f))
                  .border(1.dp, accentCol.copy(alpha = 0.85f), RoundedCornerShape(5.dp))
                  .padding(horizontal = 7.dp, vertical = 2.5.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              if (prompt.showResonanceCrest) {
                ResonanceCrestIcon(sizeDp = 11.dp)
              }
              Text(
                text = prompt.badge,
                color = accentCol,
                fontSize = 8.6.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
              )
            }
          }

          Column(
            modifier =
              Modifier.fillMaxWidth()
                .heightIn(max = if (isTabletDeck) 118.dp else 82.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp),
          ) {
            Text(
              text = prompt.summary,
              color = Color(0xFFE2E8F0),
              fontSize = if (isTabletDeck) 9.4.sp else 8.4.sp,
              lineHeight = if (isTabletDeck) 12.6.sp else 11.2.sp,
              fontFamily = FontFamily.SansSerif,
              textAlign = TextAlign.Center,
            )
            if (prompt.tacticalNote.isNotBlank()) {
              Text(
                text = prompt.tacticalNote,
                color = Color(0xFFCBD5E1),
                fontSize = if (isTabletDeck) 9.6.sp else 8.6.sp,
                lineHeight = if (isTabletDeck) 12.6.sp else 11.2.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.Center,
              )
            }
          }

          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            if (prompt.primaryActionLabel.isNotBlank() && prompt.onPrimaryAction != null) {
              Box(
                modifier =
                  Modifier.minimumInteractiveComponentSize()
                    .height(48.dp)
                    .clickable { onExecuteInfoStageAction(prompt) }
                    .padding(vertical = 5.dp)
                    .testTag("overwatch_info_primary_button"),
                contentAlignment = Alignment.Center,
              ) {
                Box(
                  modifier =
                    Modifier.clip(RoundedCornerShape(16.dp))
                      .background(Brush.horizontalGradient(listOf(Color(0xFF0D3554), Color(0xFF091528))))
                      .border(1.25.dp, accentCol, RoundedCornerShape(16.dp))
                      .padding(horizontal = 12.dp, vertical = 6.dp),
                ) {
                  Text(
                    text = prompt.primaryActionLabel,
                    color = Color.White,
                    fontSize = if (isTabletDeck) 9.8.sp else 8.8.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                  )
                }
              }
            }
            Box(
              modifier =
                Modifier.minimumInteractiveComponentSize()
                  .height(48.dp)
                  .clickable { onDismissPrompt() }
                  .padding(vertical = 5.dp)
                  .testTag("overwatch_info_close_button"),
              contentAlignment = Alignment.Center,
            ) {
              Box(
                modifier =
                  Modifier.clip(CircleShape)
                    .background(Color(0xCC060D1A))
                    .border(1.dp, Color(0xFF64748B), CircleShape)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
              ) {
                Text(
                  text = "CLOSE",
                  color = Color(0xFFCBD5E1),
                  fontSize = 8.4.sp,
                  fontWeight = FontWeight.Black,
                  fontFamily = FontFamily.Monospace,
                )
              }
            }
          }
        }
      }

      is OverwatchCircularPrompt.SyndicateCrestBuilder -> {
        val context = LocalContext.current
        val localPrefs = remember(context) { LocalPilotPrefs(context) }
        val palettes = remember {
          listOf(
            Triple("#0B1528", "#FACC15", "#00F5D4"),
            Triple("#1A070C", "#EF4444", "#F59E0B"),
            Triple("#0F0722", "#E879F9", "#00F5D4"),
            Triple("#051914", "#10B981", "#38BDF8"),
            Triple("#061528", "#38BDF8", "#E0F2FE"),
          )
        }
        var crestDraft by
          remember(prompt.syndicateTag) {
            mutableStateOf(
              localPrefs.getSyndicateCrestJson(prompt.syndicateTag)?.let { raw ->
                runCatching { SyndicateCrestRecipe.fromJson(org.json.JSONObject(raw)) }.getOrNull()
              } ?: RiftVisualForgeState.activeSyndicateCrestRecipe
            )
          }
        Column(
          modifier = Modifier.fillMaxWidth(0.86f).testTag("overwatch_syndicate_crest_builder"),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(if (isTabletDeck) 6.dp else 4.dp),
        ) {
          Text(
            text = "[${prompt.syndicateTag}] SYNDICATE CREST",
            color = Color(0xFF00F5D4),
            fontSize = if (isTabletDeck) 9.4.sp else 8.4.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
          )
          Canvas(modifier = Modifier.size(if (isTabletDeck) 78.dp else 62.dp)) {
            drawIntoCanvas { canvas ->
              RiftSpriteBaker.drawSyndicateCrestOnCanvas(
                c = canvas.nativeCanvas,
                cx = size.width * 0.5f,
                cy = size.height * 0.5f,
                r = min(size.width, size.height) * 0.44f,
                recipe = crestDraft,
                tagText = prompt.syndicateTag,
              )
            }
          }
          Row(
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Box(
              modifier =
                Modifier.minimumInteractiveComponentSize()
                  .height(48.dp)
                  .clickable {
                    crestDraft = crestDraft.copy(shieldShape = (crestDraft.shieldShape + 1) % 4)
                  }
                  .padding(vertical = 7.dp),
              contentAlignment = Alignment.Center,
            ) {
              Box(
                modifier =
                  Modifier.clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF0D253B))
                    .border(1.dp, Color(0xFF38BDF8), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 5.dp),
              ) {
                Text(
                  text = "SHAPE: ${crestDraft.shieldShapeLabel.uppercase(Locale.US)}",
                  color = Color.White,
                  fontSize = 7.0.sp,
                  fontWeight = FontWeight.Black,
                  fontFamily = FontFamily.Monospace,
                )
              }
            }
            Box(
              modifier =
                Modifier.minimumInteractiveComponentSize()
                  .height(48.dp)
                  .clickable {
                    crestDraft = crestDraft.copy(emblemGlyph = (crestDraft.emblemGlyph + 1) % 6)
                  }
                  .padding(vertical = 7.dp),
              contentAlignment = Alignment.Center,
            ) {
              Box(
                modifier =
                  Modifier.clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF0D253B))
                    .border(1.dp, Color(0xFF00F5D4), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 5.dp),
              ) {
                Text(
                  text = "GLYPH: ${crestDraft.emblemGlyphLabel.uppercase(Locale.US)}",
                  color = Color.White,
                  fontSize = 7.0.sp,
                  fontWeight = FontWeight.Black,
                  fontFamily = FontFamily.Monospace,
                )
              }
            }
          }
          Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Box(
              modifier =
                Modifier.minimumInteractiveComponentSize()
                  .height(48.dp)
                  .clickable {
                    val curIdx = palettes.indexOfFirst { it.second == crestDraft.trimHex }
                    val next = palettes[(curIdx + 1) % palettes.size]
                    crestDraft = crestDraft.copy(primaryHex = next.first, trimHex = next.second, glowHex = next.third)
                  }
                  .padding(vertical = 7.dp),
              contentAlignment = Alignment.Center,
            ) {
              Box(
                modifier =
                  Modifier.clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF091528))
                    .border(1.dp, Color(0xFFFACC15), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 5.dp),
              ) {
                Text("COLORS", color = Color(0xFFFACC15), fontSize = 7.2.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
              }
            }
            Box(
              modifier =
                Modifier.minimumInteractiveComponentSize()
                  .height(48.dp)
                  .clickable {
                    RiftVisualForgeState.updateSyndicateCrest(crestDraft, context)
                    localPrefs.setSyndicateCrestJson(prompt.syndicateTag, crestDraft.toJson().toString())
                    onDismissPrompt()
                  }
                  .padding(vertical = 7.dp)
                  .testTag("overwatch_crest_save_button"),
              contentAlignment = Alignment.Center,
            ) {
              Box(
                modifier =
                  Modifier.clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF065F46))
                    .border(1.2.dp, Color(0xFF00F5D4), RoundedCornerShape(14.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
              ) {
                Text("SAVE CREST", color = Color.White, fontSize = 7.6.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
              }
            }
            Box(
              modifier =
                Modifier.minimumInteractiveComponentSize()
                  .height(48.dp)
                  .clickable { onDismissPrompt() }
                  .padding(vertical = 7.dp),
              contentAlignment = Alignment.Center,
            ) {
              Box(
                modifier =
                  Modifier.clip(CircleShape)
                    .background(Color(0xCC060D1A))
                    .border(1.dp, Color(0xFF64748B), CircleShape)
                    .padding(horizontal = 9.dp, vertical = 5.dp),
              ) {
                Text("CLOSE", color = Color(0xFFCBD5E1), fontSize = 7.4.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
              }
            }
          }
        }
      }

      OverwatchCircularPrompt.RiftFrameDrydock -> {
        OverwatchRiftFrameDrydockView(
          isTabletDeck = isTabletDeck,
          activeCustomSlot = activeCustomSlot,
          activeCustomSlotId = activeCustomSlotId,
          pointBudget = pointBudget.coerceAtLeast(100),
          isCharged = chargedShipId == 10,
          shipSpinDeg = shipSpinDeg,
          fluxOrbitRad = fluxOrbitRad,
          activeDraggedFrameItem = activeDraggedFrameItem,
          dragPointerRootOffset = dragPointerRootOffset,
          onSaveCustomSlot = onSaveCustomSlot,
          onOpenFrameRenamePrompt = onOpenFrameRenamePrompt,
          onChargeRiftFrame = { onChargeShip(10) },
          onUpdatePrimarySocketBounds = onUpdatePrimarySocketBounds,
          onUpdateSecondarySocketBounds = onUpdateSecondarySocketBounds,
        )
      }

      is OverwatchCircularPrompt.FrameSlotRename -> {
        val keyboardController = LocalSoftwareKeyboardController.current
        Column(
          modifier = Modifier.fillMaxWidth(0.84f).testTag("overwatch_frame_rename_overlay"),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(if (isTabletDeck) 7.dp else 5.dp),
        ) {
          Text(
            text = "RIFT FRAME CALLSIGN // ${prompt.slotId.uppercase(Locale.US)}",
            color = Color(0xFF00F5D4),
            fontSize = if (isTabletDeck) 9.2.sp else 8.2.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
          )
          Box(
            modifier =
              Modifier.fillMaxWidth(0.88f)
                .height(38.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF0B1528))
                .border(1.2.dp, Color(0xFF00F5D4), RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp),
            contentAlignment = Alignment.CenterStart,
          ) {
            if (frameRenameDraft.isBlank()) {
              Text(
                text = prompt.slotId.uppercase(Locale.US),
                color = Color(0xFF94A3B8),
                fontSize = 9.8.sp,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
              )
            }
            BasicTextField(
              value = frameRenameDraft,
              onValueChange = { onFrameRenameDraftChange(it.take(18)) },
              singleLine = true,
              cursorBrush = SolidColor(Color(0xFF00F5D4)),
              textStyle =
                TextStyle(
                  color = Color.White,
                  fontSize = 10.6.sp,
                  fontWeight = FontWeight.Black,
                  fontFamily = FontFamily.Monospace,
                ),
              keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
              keyboardActions =
                KeyboardActions(
                  onDone = {
                    keyboardController?.hide()
                    onSaveFrameSlotRename(frameRenameDraft)
                  }
                ),
              modifier = Modifier.fillMaxWidth().testTag("overwatch_frame_rename_input"),
            )
          }
          Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Box(
              modifier =
                Modifier.minimumInteractiveComponentSize()
                  .height(48.dp)
                  .clickable {
                    keyboardController?.hide()
                    onSaveFrameSlotRename(frameRenameDraft)
                  }
                  .padding(vertical = 6.dp)
                  .testTag("overwatch_frame_rename_save"),
              contentAlignment = Alignment.Center,
            ) {
              Box(
                modifier =
                  Modifier.clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF065F46))
                    .border(1.2.dp, Color(0xFF00F5D4), RoundedCornerShape(14.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
              ) {
                Text(
                  text = "SAVE NAME",
                  color = Color.White,
                  fontSize = 8.4.sp,
                  fontWeight = FontWeight.Black,
                  fontFamily = FontFamily.Monospace,
                )
              }
            }
            Box(
              modifier =
                Modifier.minimumInteractiveComponentSize()
                  .height(48.dp)
                  .clickable {
                    keyboardController?.hide()
                    onDismissPrompt()
                  }
                  .padding(vertical = 6.dp)
                  .testTag("overwatch_frame_rename_cancel"),
              contentAlignment = Alignment.Center,
            ) {
              Box(
                modifier =
                  Modifier.clip(CircleShape)
                    .background(Color(0xCC060D1A))
                    .border(1.dp, Color(0xFF64748B), CircleShape)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
              ) {
                Text(
                  text = "CANCEL",
                  color = Color(0xFFCBD5E1),
                  fontSize = 8.2.sp,
                  fontWeight = FontWeight.Black,
                  fontFamily = FontFamily.Monospace,
                )
              }
            }
          }
        }
      }

      is OverwatchCircularPrompt.SliderWorkbench -> {
        OverwatchSliderWorkbenchView(
          prompt = prompt,
          isTabletDeck = isTabletDeck,
          onDismissPrompt = onDismissPrompt,
        )
      }
    }
  }
}

@Composable
private fun OverwatchRiftFrameDrydockView(
  isTabletDeck: Boolean,
  activeCustomSlot: CustomShipLoadoutSlot,
  activeCustomSlotId: String,
  pointBudget: Int,
  isCharged: Boolean,
  shipSpinDeg: Float,
  fluxOrbitRad: Float,
  activeDraggedFrameItem: DraggedRiftFrameItem?,
  dragPointerRootOffset: Offset,
  onSaveCustomSlot: (CustomShipLoadoutSlot) -> Unit,
  onOpenFrameRenamePrompt: () -> Unit,
  onChargeRiftFrame: () -> Unit,
  onUpdatePrimarySocketBounds: (Rect) -> Unit,
  onUpdateSecondarySocketBounds: (Rect) -> Unit,
) {
  val effectiveBudget = pointBudget.coerceAtLeast(100)
  val usedPoints = activeCustomSlot.totalAllocatedPoints()
  val budgetFrac = (usedPoints.toFloat() / effectiveBudget.toFloat()).coerceIn(0f, 1f)
  val primaryOpt =
    remember(activeCustomSlot.primaryArchetype) {
      CustomShipLoadoutSlot.PRIMARY_OPTIONS.firstOrNull {
        it.id.equals(activeCustomSlot.primaryArchetype, ignoreCase = true)
      } ?: CustomShipLoadoutSlot.PRIMARY_OPTIONS.first()
    }
  val secondaryOpt =
    remember(activeCustomSlot.secondaryType, RiftSpriteBaker.customShip9Version) {
      CustomShipLoadoutSlot.getAllAvailableSecondaryOptions().firstOrNull {
        it.id.equals(activeCustomSlot.secondaryType, ignoreCase = true)
      } ?: CustomShipLoadoutSlot.getAllAvailableSecondaryOptions().first()
    }
  val sortedPrimaries =
    remember {
      CustomShipLoadoutSlot.PRIMARY_OPTIONS.sortedBy { it.pointCost }
    }
  val primaryBmp =
    remember(primaryOpt.id, RiftSpriteBaker.spriteVisualRevision) {
      RiftSpriteBaker.getPrimaryWeaponIconBitmap(primaryOpt.id).asImageBitmap()
    }
  val secondaryBmp =
    remember(secondaryOpt.id, RiftSpriteBaker.spriteVisualRevision) {
      RiftSpriteBaker.getSecondaryAbilityIconBitmap(secondaryOpt.id).asImageBitmap()
    }

  var priBounds by remember { mutableStateOf(Rect.Zero) }
  var secBounds by remember { mutableStateOf(Rect.Zero) }
  val isPriDragActive = activeDraggedFrameItem?.isPrimary == true
  val isSecDragActive = activeDraggedFrameItem?.isPrimary == false
  val isPriHovered = isPriDragActive && priBounds.inflate(28f).contains(dragPointerRootOffset)
  val isSecHovered = isSecDragActive && secBounds.inflate(28f).contains(dragPointerRootOffset)

  val displayTitle =
    remember(activeCustomSlotId, activeCustomSlot.customName) {
      val cleanCustom = activeCustomSlot.customName.trim()
      if (cleanCustom.isNotBlank() && !cleanCustom.equals(activeCustomSlotId, ignoreCase = true)) {
        "${activeCustomSlotId.uppercase(Locale.US)} • ${cleanCustom.uppercase(Locale.US)}"
      } else {
        activeCustomSlotId.uppercase(Locale.US)
      }
    }

  Column(
    modifier = Modifier.fillMaxWidth(0.92f).testTag("overwatch_rift_frame_drydock"),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(if (isTabletDeck) 4.dp else 2.5.dp),
  ) {
    // 1. Top Slot Identity + 100-Point Budget Readout
    Row(
      modifier =
        Modifier.clip(RoundedCornerShape(8.dp))
          .background(Color(0xCC061121))
          .clickable { onOpenFrameRenamePrompt() }
          .padding(horizontal = 8.dp, vertical = 3.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Text(
        text = displayTitle,
        color = Color.White,
        fontSize = if (isTabletDeck) 9.0.sp else 8.0.sp,
        fontWeight = FontWeight.Black,
        fontFamily = FontFamily.Monospace,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Text(
        text = "$usedPoints/$effectiveBudget PTS",
        color = if (usedPoints >= effectiveBudget) Color(0xFFFFD700) else Color(0xFF00F5D4),
        fontSize = if (isTabletDeck) 8.6.sp else 7.6.sp,
        fontWeight = FontWeight.Black,
        fontFamily = FontFamily.Monospace,
        maxLines = 1,
      )
    }

    // 2. Central Drydock Row: [PRIMARY MOUNT]  (Ship #10 + 100-Pt Ring)  [SECONDARY / BUFF]
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
      val priBorderCol =
        when {
          isPriHovered -> Color(0xFFFFD700)
          isPriDragActive -> Color(0xFF00F5D4)
          else -> Color(0x9938BDF8)
        }
      Column(
        modifier =
          Modifier.width(if (isTabletDeck) 84.dp else 72.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(if (isPriHovered) Color(0xCC0D2E42) else Color(0xB8071326))
            .border(if (isPriDragActive) 1.6.dp else 1.dp, priBorderCol, RoundedCornerShape(9.dp))
            .onGloballyPositioned { coords ->
              val b = coords.boundsInRoot()
              priBounds = b
              onUpdatePrimarySocketBounds(b)
            }
            .padding(horizontal = 4.dp, vertical = 4.dp)
            .testTag("overwatch_drydock_primary_socket"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(1.5.dp),
      ) {
        Text(
          text = "PRIMARY",
          color = priBorderCol,
          fontSize = 6.8.sp,
          fontWeight = FontWeight.Black,
          fontFamily = FontFamily.Monospace,
          maxLines = 1,
        )
        Image(
          bitmap = primaryBmp,
          contentDescription = primaryOpt.title,
          contentScale = ContentScale.Fit,
          modifier = Modifier.size(if (isTabletDeck) 30.dp else 25.dp),
        )
        Text(
          text = primaryOpt.shortLabel.uppercase(Locale.US),
          color = Color.White,
          fontSize = 7.2.sp,
          fontWeight = FontWeight.Black,
          fontFamily = FontFamily.Monospace,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = "${primaryOpt.pointCost} PTS",
          color = Color(0xFF00F5D4),
          fontSize = 6.8.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.Monospace,
          maxLines = 1,
        )
      }

      Box(
        modifier =
          Modifier.size(if (isTabletDeck) 82.dp else 68.dp)
            .drawBehind {
              val r = size.minDimension * 0.47f
              val arcTopLeft = Offset(center.x - r, center.y - r)
              val arcSize = Size(r * 2f, r * 2f)
              drawArc(
                color = Color(0x441E293B),
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = arcTopLeft,
                size = arcSize,
                style = Stroke(width = 3.2.dp.toPx(), cap = StrokeCap.Round),
              )
              drawArc(
                brush =
                  Brush.sweepGradient(
                    0f to Color(0xFF00F5D4),
                    0.5f to Color(0xFF38BDF8),
                    1f to Color(0xFFFFD700),
                    center = center,
                  ),
                startAngle = 135f,
                sweepAngle = 270f * budgetFrac,
                useCenter = false,
                topLeft = arcTopLeft,
                size = arcSize,
                style = Stroke(width = 3.4.dp.toPx(), cap = StrokeCap.Round),
              )
            },
        contentAlignment = Alignment.Center,
      ) {
        Spinning25dShipWithOrbitingFlux(
          shipId = 10,
          shipSpinDeg = shipSpinDeg,
          fluxOrbitRad = fluxOrbitRad,
          isCharged = isCharged,
          isBestShip = false,
          isSelected = true,
          sizeDp = if (isTabletDeck) 58.dp else 48.dp,
        )
      }

      val secBorderCol =
        when {
          isSecHovered -> Color(0xFFFFD700)
          isSecDragActive -> Color(0xFFC084FC)
          else -> Color(0x99C084FC)
        }
      Column(
        modifier =
          Modifier.width(if (isTabletDeck) 84.dp else 72.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(if (isSecHovered) Color(0xCC24123D) else Color(0xB8071326))
            .border(if (isSecDragActive) 1.6.dp else 1.dp, secBorderCol, RoundedCornerShape(9.dp))
            .onGloballyPositioned { coords ->
              val b = coords.boundsInRoot()
              secBounds = b
              onUpdateSecondarySocketBounds(b)
            }
            .padding(horizontal = 4.dp, vertical = 4.dp)
            .testTag("overwatch_drydock_secondary_socket"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(1.5.dp),
      ) {
        Text(
          text = "MOD / BUFF",
          color = secBorderCol,
          fontSize = 6.8.sp,
          fontWeight = FontWeight.Black,
          fontFamily = FontFamily.Monospace,
          maxLines = 1,
        )
        Image(
          bitmap = secondaryBmp,
          contentDescription = secondaryOpt.title,
          contentScale = ContentScale.Fit,
          modifier = Modifier.size(if (isTabletDeck) 30.dp else 25.dp),
        )
        Text(
          text = secondaryOpt.shortLabel.uppercase(Locale.US),
          color = Color.White,
          fontSize = 7.2.sp,
          fontWeight = FontWeight.Black,
          fontFamily = FontFamily.Monospace,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = "${secondaryOpt.pointCost} PTS",
          color = Color(0xFFC084FC),
          fontSize = 6.8.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.Monospace,
          maxLines = 1,
        )
      }
    }

    // 3. Six Compact Interactive Drag-and-Tap Allocation Bars (FIRE, FLUX, REGEN, SPEED, THRUST, TURN)
    val priIndex =
      sortedPrimaries.indexOfFirst { it.id.equals(primaryOpt.id, ignoreCase = true) }.coerceAtLeast(0)
    DrydockInteractiveStatBar(
      label = "FIRE",
      valueText = "${primaryOpt.pointCost}P",
      fraction =
        if (sortedPrimaries.size <= 1) 1f
        else priIndex.toFloat() / (sortedPrimaries.size - 1).toFloat(),
      barColor = Color(0xFFFB7185),
      onScrubFraction = { frac ->
        val idx =
          (frac * (sortedPrimaries.size - 1).toFloat())
            .roundToInt()
            .coerceIn(0, sortedPrimaries.lastIndex)
        val chosen = sortedPrimaries[idx]
        if (!chosen.id.equals(activeCustomSlot.primaryArchetype, ignoreCase = true)) {
          onSaveCustomSlot(
            activeCustomSlot
              .copy(slotId = activeCustomSlotId, primaryArchetype = chosen.id)
              .clampToPointBudget(effectiveBudget)
          )
        }
      },
    )
    DrydockInteractiveStatBar(
      label = "FLUX",
      valueText = "${activeCustomSlot.maxFluxPts}",
      fraction = (activeCustomSlot.maxFluxPts / 30f).coerceIn(0f, 1f),
      barColor = Color(0xFF38BDF8),
      onScrubFraction = { frac ->
        val otherPts =
          activeCustomSlot.primaryPointCost() +
            activeCustomSlot.secondaryPointCost() +
            activeCustomSlot.rechargePts +
            activeCustomSlot.speedPts +
            activeCustomSlot.thrustPts +
            activeCustomSlot.turnPts
        val maxAllowed = (effectiveBudget - otherPts).coerceIn(0, 30)
        val next = (frac * 30f).roundToInt().coerceIn(0, maxAllowed)
        if (next != activeCustomSlot.maxFluxPts) {
          onSaveCustomSlot(activeCustomSlot.copy(slotId = activeCustomSlotId, maxFluxPts = next))
        }
      },
    )
    DrydockInteractiveStatBar(
      label = "REGEN",
      valueText = "${activeCustomSlot.rechargePts}",
      fraction = (activeCustomSlot.rechargePts / 30f).coerceIn(0f, 1f),
      barColor = Color(0xFF00F5D4),
      onScrubFraction = { frac ->
        val otherPts =
          activeCustomSlot.primaryPointCost() +
            activeCustomSlot.secondaryPointCost() +
            activeCustomSlot.maxFluxPts +
            activeCustomSlot.speedPts +
            activeCustomSlot.thrustPts +
            activeCustomSlot.turnPts
        val maxAllowed = (effectiveBudget - otherPts).coerceIn(0, 30)
        val next = (frac * 30f).roundToInt().coerceIn(0, maxAllowed)
        if (next != activeCustomSlot.rechargePts) {
          onSaveCustomSlot(activeCustomSlot.copy(slotId = activeCustomSlotId, rechargePts = next))
        }
      },
    )
    DrydockInteractiveStatBar(
      label = "SPEED",
      valueText = "${activeCustomSlot.speedPts}",
      fraction = (activeCustomSlot.speedPts / 30f).coerceIn(0f, 1f),
      barColor = Color(0xFFFFD700),
      onScrubFraction = { frac ->
        val otherPts =
          activeCustomSlot.primaryPointCost() +
            activeCustomSlot.secondaryPointCost() +
            activeCustomSlot.maxFluxPts +
            activeCustomSlot.rechargePts +
            activeCustomSlot.thrustPts +
            activeCustomSlot.turnPts
        val maxAllowed = (effectiveBudget - otherPts).coerceIn(0, 30)
        val next = (frac * 30f).roundToInt().coerceIn(0, maxAllowed)
        if (next != activeCustomSlot.speedPts) {
          onSaveCustomSlot(activeCustomSlot.copy(slotId = activeCustomSlotId, speedPts = next))
        }
      },
    )
    DrydockInteractiveStatBar(
      label = "THRUST",
      valueText = "${activeCustomSlot.thrustPts}",
      fraction = (activeCustomSlot.thrustPts / 30f).coerceIn(0f, 1f),
      barColor = Color(0xFF34D399),
      onScrubFraction = { frac ->
        val otherPts =
          activeCustomSlot.primaryPointCost() +
            activeCustomSlot.secondaryPointCost() +
            activeCustomSlot.maxFluxPts +
            activeCustomSlot.rechargePts +
            activeCustomSlot.speedPts +
            activeCustomSlot.turnPts
        val maxAllowed = (effectiveBudget - otherPts).coerceIn(0, 30)
        val next = (frac * 30f).roundToInt().coerceIn(0, maxAllowed)
        if (next != activeCustomSlot.thrustPts) {
          onSaveCustomSlot(activeCustomSlot.copy(slotId = activeCustomSlotId, thrustPts = next))
        }
      },
    )
    DrydockInteractiveStatBar(
      label = "TURN",
      valueText = "${activeCustomSlot.turnPts}",
      fraction = (activeCustomSlot.turnPts / 30f).coerceIn(0f, 1f),
      barColor = Color(0xFFC084FC),
      onScrubFraction = { frac ->
        val otherPts =
          activeCustomSlot.primaryPointCost() +
            activeCustomSlot.secondaryPointCost() +
            activeCustomSlot.maxFluxPts +
            activeCustomSlot.rechargePts +
            activeCustomSlot.speedPts +
            activeCustomSlot.thrustPts
        val maxAllowed = (effectiveBudget - otherPts).coerceIn(0, 30)
        val next = (frac * 30f).roundToInt().coerceIn(0, maxAllowed)
        if (next != activeCustomSlot.turnPts) {
          onSaveCustomSlot(activeCustomSlot.copy(slotId = activeCustomSlotId, turnPts = next))
        }
      },
    )

    // 4. Charge Rift Frame Action
    Box(
      modifier =
        Modifier.minimumInteractiveComponentSize()
          .clickable { onChargeRiftFrame() }
          .padding(top = 1.dp)
          .testTag("overwatch_drydock_charge_button"),
      contentAlignment = Alignment.Center,
    ) {
      Box(
        modifier =
          Modifier.clip(RoundedCornerShape(14.dp))
            .background(
              Brush.horizontalGradient(
                if (isCharged) listOf(Color(0xFF065F46), Color(0xFF09253A))
                else listOf(Color(0xFF0D3554), Color(0xFF091528))
              )
            )
            .border(1.2.dp, if (isCharged) Color(0xFF00F5D4) else Color(0xFFFFD700), RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 4.dp),
      ) {
        Text(
          text = if (isCharged) "RIFT FRAME CHARGED" else "CHARGE RIFT FRAME",
          color = Color.White,
          fontSize = if (isTabletDeck) 8.4.sp else 7.6.sp,
          fontWeight = FontWeight.Black,
          fontFamily = FontFamily.Monospace,
          maxLines = 1,
        )
      }
    }
  }
}

@Composable
private fun DrydockInteractiveStatBar(
  label: String,
  valueText: String,
  fraction: Float,
  barColor: Color,
  onScrubFraction: (Float) -> Unit,
) {
  var trackWidthPx by remember { mutableFloatStateOf(1f) }
  Row(
    modifier = Modifier.fillMaxWidth(0.86f).height(15.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(5.dp),
  ) {
    Text(
      text = label,
      color = Color(0xFFCBD5E1),
      fontSize = 7.0.sp,
      fontWeight = FontWeight.Black,
      fontFamily = FontFamily.Monospace,
      modifier = Modifier.width(34.dp),
      maxLines = 1,
    )
    Box(
      modifier =
        Modifier.weight(1f)
          .fillMaxHeight()
          .onGloballyPositioned { trackWidthPx = it.size.width.toFloat().coerceAtLeast(1f) }
          .pointerInput(label) {
            detectTapGestures { pos ->
              onScrubFraction((pos.x / trackWidthPx).coerceIn(0f, 1f))
            }
          }
          .pointerInput(label) {
            detectHorizontalDragGestures { change, _ ->
              change.consume()
              onScrubFraction((change.position.x / trackWidthPx).coerceIn(0f, 1f))
            }
          }
          .drawBehind {
            val trackH = 6.dp.toPx()
            val yTop = (size.height - trackH) * 0.5f
            drawRect(
              color = Color(0xCC040B16),
              topLeft = Offset(0f, yTop),
              size = Size(size.width, trackH),
            )
            val fillW = (size.width * fraction.coerceIn(0f, 1f)).coerceAtLeast(2.dp.toPx())
            drawRect(
              brush = Brush.horizontalGradient(listOf(barColor.copy(alpha = 0.55f), barColor)),
              topLeft = Offset(0f, yTop),
              size = Size(fillW, trackH),
            )
            drawCircle(
              color = Color.White,
              radius = 3.6.dp.toPx(),
              center = Offset(fillW.coerceIn(3.dp.toPx(), size.width - 3.dp.toPx()), size.height * 0.5f),
            )
          },
    )
    Text(
      text = valueText,
      color = barColor,
      fontSize = 7.2.sp,
      fontWeight = FontWeight.Black,
      fontFamily = FontFamily.Monospace,
      modifier = Modifier.width(24.dp),
      textAlign = TextAlign.End,
      maxLines = 1,
    )
  }
}

@Composable
private fun OverwatchSliderWorkbenchView(
  prompt: OverwatchCircularPrompt.SliderWorkbench,
  isTabletDeck: Boolean,
  onDismissPrompt: () -> Unit,
) {
  val accent = Color(prompt.accentHex)
  val safeMin = prompt.minValue
  val safeMax = prompt.maxValue.coerceAtLeast(safeMin + 0.0001f)
  val span = safeMax - safeMin
  var liveVal by remember(prompt.key) { mutableFloatStateOf(prompt.currentValue.coerceIn(safeMin, safeMax)) }

  fun snapAndCommit(rawFrac: Float) {
    val clampedFrac = rawFrac.coerceIn(0f, 1f)
    val rawValue = safeMin + clampedFrac * span
    val snapped =
      if (prompt.stepSize > 0.0001f) {
        val steps = ((rawValue - safeMin) / prompt.stepSize).roundToInt()
        (safeMin + steps * prompt.stepSize).coerceIn(safeMin, safeMax)
      } else {
        rawValue.coerceIn(safeMin, safeMax)
      }
    liveVal = snapped
    prompt.onValueChange(snapped)
  }

  fun angleOffsetToFraction(pos: Offset, boxSize: IntSize): Float {
    val cx = boxSize.width * 0.5f
    val cy = boxSize.height * 0.5f
    val deg = Math.toDegrees(atan2((pos.y - cy).toDouble(), (pos.x - cx).toDouble())).toFloat()
    val shifted = ((deg - 135f) % 360f + 360f) % 360f
    return when {
      shifted <= 270f -> (shifted / 270f).coerceIn(0f, 1f)
      shifted < 315f -> 1f
      else -> 0f
    }
  }

  val formattedValue =
    remember(liveVal, prompt.key, prompt.unitSuffix) {
      prompt.valueFormatter?.invoke(liveVal)
        ?: run {
          val isWhole = prompt.stepSize >= 1f && abs(liveVal - liveVal.roundToInt()) < 0.01f
          val numStr =
            if (isWhole) "${liveVal.roundToInt()}"
            else String.format(Locale.US, "%.2f", liveVal)
          if (prompt.unitSuffix.isNotBlank()) "$numStr${prompt.unitSuffix}" else numStr
        }
    }
  val normFrac = ((liveVal - safeMin) / span).coerceIn(0f, 1f)
  var dialBoxSize by remember { mutableStateOf(IntSize.Zero) }
  var linearWidthPx by remember { mutableFloatStateOf(1f) }

  Column(
    modifier = Modifier.fillMaxWidth(0.88f).testTag("overwatch_slider_workbench"),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(if (isTabletDeck) 5.dp else 3.dp),
  ) {
    Text(
      text = prompt.title.uppercase(Locale.US),
      color = accent,
      fontSize = if (isTabletDeck) 9.8.sp else 8.6.sp,
      fontWeight = FontWeight.Black,
      fontFamily = FontFamily.Monospace,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
    Text(
      text = prompt.subtitle,
      color = Color(0xFFCBD5E1),
      fontSize = if (isTabletDeck) 8.2.sp else 7.4.sp,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )

    // 270-Degree Circular Arc Dial
    Box(
      modifier =
        Modifier.size(if (isTabletDeck) 114.dp else 94.dp)
          .onGloballyPositioned { dialBoxSize = it.size }
          .pointerInput(prompt.key) {
            detectTapGestures { pos ->
              if (dialBoxSize.width > 8) snapAndCommit(angleOffsetToFraction(pos, dialBoxSize))
            }
          }
          .pointerInput(prompt.key) {
            detectDragGestures { change, _ ->
              change.consume()
              if (dialBoxSize.width > 8) snapAndCommit(angleOffsetToFraction(change.position, dialBoxSize))
            }
          }
          .testTag("overwatch_slider_arc_dial"),
      contentAlignment = Alignment.Center,
    ) {
      Canvas(modifier = Modifier.fillMaxSize()) {
        val r = size.minDimension * 0.43f
        val topLeft = Offset(center.x - r, center.y - r)
        val arcSize = Size(r * 2f, r * 2f)
        drawArc(
          color = Color(0x661E293B),
          startAngle = 135f,
          sweepAngle = 270f,
          useCenter = false,
          topLeft = topLeft,
          size = arcSize,
          style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round),
        )
        drawArc(
          brush =
            Brush.sweepGradient(
              0f to accent.copy(alpha = 0.72f),
              0.5f to accent,
              1f to Color(0xFFFFD700),
              center = center,
            ),
          startAngle = 135f,
          sweepAngle = 270f * normFrac,
          useCenter = false,
          topLeft = topLeft,
          size = arcSize,
          style = Stroke(width = 6.5.dp.toPx(), cap = StrokeCap.Round),
        )
        val knobRad = Math.toRadians((135.0 + 270.0 * normFrac)).toFloat()
        val knobCenter = Offset(center.x + cos(knobRad) * r, center.y + sin(knobRad) * r)
        drawCircle(color = accent.copy(alpha = 0.36f), radius = 8.dp.toPx(), center = knobCenter)
        drawCircle(color = Color.White, radius = 4.6.dp.toPx(), center = knobCenter)
      }
      Text(
        text = formattedValue,
        color = Color.White,
        fontSize = if (isTabletDeck) 14.sp else 12.sp,
        fontWeight = FontWeight.Black,
        fontFamily = FontFamily.Monospace,
        textAlign = TextAlign.Center,
        maxLines = 1,
      )
    }

    // Wide Horizontal Fine-Scrub Track
    Box(
      modifier =
        Modifier.fillMaxWidth(0.84f)
          .height(22.dp)
          .onGloballyPositioned { linearWidthPx = it.size.width.toFloat().coerceAtLeast(1f) }
          .pointerInput(prompt.key) {
            detectTapGestures { pos ->
              snapAndCommit(pos.x / linearWidthPx)
            }
          }
          .pointerInput(prompt.key) {
            detectHorizontalDragGestures { change, _ ->
              change.consume()
              snapAndCommit(change.position.x / linearWidthPx)
            }
          }
          .drawBehind {
            val hBar = 7.dp.toPx()
            val yTop = (size.height - hBar) * 0.5f
            drawRect(
              color = Color(0xCC050D1C),
              topLeft = Offset(0f, yTop),
              size = Size(size.width, hBar),
            )
            val fillW = (size.width * normFrac).coerceAtLeast(2.dp.toPx())
            drawRect(
              brush = Brush.horizontalGradient(listOf(accent.copy(alpha = 0.55f), accent)),
              topLeft = Offset(0f, yTop),
              size = Size(fillW, hBar),
            )
            drawCircle(
              color = Color.White,
              radius = 5.dp.toPx(),
              center = Offset(fillW.coerceIn(5.dp.toPx(), size.width - 5.dp.toPx()), size.height * 0.5f),
            )
          }
          .testTag("overwatch_slider_horizontal_track"),
    )

    // Quick Preset Chips Row
    if (prompt.presets.isNotEmpty()) {
      Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        prompt.presets.forEach { (presetLabel, presetVal) ->
          val isSelected = abs(liveVal - presetVal) <= (prompt.stepSize * 0.45f).coerceAtLeast(0.01f)
          Box(
            modifier =
              Modifier.clip(RoundedCornerShape(8.dp))
                .background(if (isSelected) accent.copy(alpha = 0.25f) else Color(0xCC060D1A))
                .border(1.dp, if (isSelected) accent else Color(0x66475569), RoundedCornerShape(8.dp))
                .clickable {
                  liveVal = presetVal.coerceIn(safeMin, safeMax)
                  prompt.onValueChange(liveVal)
                }
                .padding(horizontal = 7.dp, vertical = 4.dp),
          ) {
            Text(
              text = presetLabel,
              color = if (isSelected) Color.White else Color(0xFFCBD5E1),
              fontSize = 7.4.sp,
              fontWeight = FontWeight.Black,
              fontFamily = FontFamily.Monospace,
              maxLines = 1,
            )
          }
        }
      }
    }

    // Reset Default + Done Actions
    Row(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Box(
        modifier =
          Modifier.minimumInteractiveComponentSize()
            .clickable {
              liveVal = prompt.defaultValue.coerceIn(safeMin, safeMax)
              prompt.onValueChange(liveVal)
            }
            .testTag("overwatch_slider_reset_default"),
        contentAlignment = Alignment.Center,
      ) {
        Box(
          modifier =
            Modifier.clip(RoundedCornerShape(12.dp))
              .background(Color(0xCC060D1A))
              .border(1.dp, Color(0xFFFACC15), RoundedCornerShape(12.dp))
              .padding(horizontal = 9.dp, vertical = 4.dp),
        ) {
          Text(
            text = "RESET DEFAULT",
            color = Color(0xFFFACC15),
            fontSize = 7.4.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
          )
        }
      }
      Box(
        modifier =
          Modifier.minimumInteractiveComponentSize()
            .clickable { onDismissPrompt() }
            .testTag("overwatch_slider_done_button"),
        contentAlignment = Alignment.Center,
      ) {
        Box(
          modifier =
            Modifier.clip(RoundedCornerShape(12.dp))
              .background(Color(0xFF065F46))
              .border(1.1.dp, accent, RoundedCornerShape(12.dp))
              .padding(horizontal = 11.dp, vertical = 4.dp),
        ) {
          Text(
            text = "DONE",
            color = Color.White,
            fontSize = 7.6.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
          )
        }
      }
    }
  }
}

@Composable
internal fun CircularPromptStatBar(
  modifier: Modifier = Modifier,
  label: String,
  fraction: Float,
  color: Color,
) {
  Row(
    modifier = modifier,
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(4.dp),
  ) {
    Text(
      text = label,
      color = Color(0xFF94A3B8),
      fontSize = 7.0.sp,
      fontWeight = FontWeight.Black,
      fontFamily = FontFamily.Monospace,
      maxLines = 1,
    )
    Box(
      modifier =
        Modifier.weight(1f)
          .height(4.5.dp)
          .clip(RoundedCornerShape(3.dp))
          .background(Color(0xFF02050B))
          .border(0.6.dp, Color(0x5538BDF8), RoundedCornerShape(3.dp)),
    ) {
      Box(
        modifier =
          Modifier.fillMaxWidth(fraction.coerceIn(0.08f, 1f))
            .fillMaxHeight()
            .background(color),
      )
    }
  }
}



private enum class OverwatchLeaderboardTab {
  GLOBAL_TOP,
  BY_SHIP,
  SYNDICATES,
}

private data class OverwatchPilotStandingRow(
  val uidKey: String,
  val callsign: String,
  val syndicateTag: String,
  val shipId: Int,
  val resonance: Int,
  val isLocalPilot: Boolean,
)

private data class OverwatchSyndicateStandingRow(
  val tag: String,
  val name: String,
  val memberCount: Int,
  val combinedResonance: Long,
  val isLocalSyndicate: Boolean,
)

private fun isHumanPilotEntryValid(uid: String, callsign: String): Boolean {
  val normUid = uid.trim().lowercase(Locale.US)
  val normCall = callsign.trim().uppercase(Locale.US)
  if (normCall.isBlank()) return false
  if (normUid.startsWith("bot_") || normUid.contains("bot_pilot")) return false
  if (normCall.startsWith("[BOT]") || normCall.startsWith("BOT_") || normCall.startsWith("RFT-")) return false
  return true
}

private fun canonicalLeaderboardShipLabel(shipId: Int): String {
  val clamped = shipId.coerceIn(1, 10)
  if (clamped == 10) return "RIFT FRAME"
  return RiftCodex.getShipById(clamped).name.replace(Regex("^[0-9]+-"), "").trim().uppercase(Locale.US)
}

@Composable
private fun OverwatchResonanceLeaderboardView(
  currentUserId: String,
  localCallsign: String,
  localSyndicateTag: String,
  localLiveResonance: Int,
  localActiveShipId: Int,
  resolveLocalShipResonance: (Int) -> Int,
  cloudLeaderboardEntries: List<LeaderboardEntry>,
  onlineHumanPilots: List<LivePilotState>,
  syndicateDirectory: List<SyndicateInfo>,
  onDismissPrompt: () -> Unit,
) {
  var selectedTab by remember { mutableStateOf(OverwatchLeaderboardTab.GLOBAL_TOP) }
  var selectedShipFilter by remember { mutableIntStateOf(localActiveShipId.coerceIn(1, 10)) }

  val cleanLocalTag =
    remember(localSyndicateTag) {
      localSyndicateTag
        .trim()
        .uppercase(Locale.US)
        .takeIf { it !in setOf("GUEST", "RFT", "NONE", "SOLO", "UNAFFILIATED", "FREE", "BOT") }
        .orEmpty()
    }

  val globalStandings =
    remember(
      cloudLeaderboardEntries,
      onlineHumanPilots,
      currentUserId,
      localCallsign,
      cleanLocalTag,
      localLiveResonance,
      localActiveShipId,
    ) {
      val byPilotKey = LinkedHashMap<String, OverwatchPilotStandingRow>()
      val normSelfUid = currentUserId.trim().lowercase(Locale.US)
      val normSelfCall = localCallsign.trim().uppercase(Locale.US)

      for (entry in cloudLeaderboardEntries) {
        val rawUid = entry.userId.ifBlank { entry.id }.trim()
        if (!isHumanPilotEntryValid(rawUid, entry.callsign)) continue
        val key = rawUid.lowercase(Locale.US).ifBlank { entry.callsign.trim().lowercase(Locale.US) }
        val isSelf =
          (normSelfUid.isNotBlank() && key == normSelfUid) ||
            (normSelfCall.isNotBlank() && entry.callsign.trim().equals(normSelfCall, ignoreCase = true))
        val cleanTag =
          entry.syndicateTag
            .trim()
            .uppercase(Locale.US)
            .takeIf { it !in setOf("GUEST", "RFT", "NONE", "SOLO", "UNAFFILIATED", "FREE", "BOT") }
            .orEmpty()
        val score = maxOf(entry.pvpBounty, entry.allTimeBounty).toInt().coerceAtLeast(0)
        val ship = entry.shipId.toInt().coerceIn(1, 10)
        val prev = byPilotKey[key]
        if (prev == null || score > prev.resonance) {
          byPilotKey[key] =
            OverwatchPilotStandingRow(
              uidKey = key,
              callsign = entry.callsign.trim().uppercase(Locale.US),
              syndicateTag = if (isSelf && cleanLocalTag.isNotBlank()) cleanLocalTag else cleanTag,
              shipId = ship,
              resonance = if (isSelf) maxOf(score, localLiveResonance) else score,
              isLocalPilot = isSelf,
            )
        }
      }

      for (live in onlineHumanPilots) {
        val rawUid = live.userId.ifBlank { live.id }.trim()
        if (!isHumanPilotEntryValid(rawUid, live.callsign)) continue
        val key = rawUid.lowercase(Locale.US).ifBlank { live.callsign.trim().lowercase(Locale.US) }
        val isSelf =
          (normSelfUid.isNotBlank() && key == normSelfUid) ||
            (normSelfCall.isNotBlank() && live.callsign.trim().equals(normSelfCall, ignoreCase = true))
        val cleanTag =
          live.syndicateTag
            .trim()
            .uppercase(Locale.US)
            .takeIf { it !in setOf("GUEST", "RFT", "NONE", "SOLO", "UNAFFILIATED", "FREE", "BOT") }
            .orEmpty()
        val score = (if (live.liveResonance > 0L) live.liveResonance else live.bounty).toInt().coerceAtLeast(0)
        val ship = live.shipId.toInt().coerceIn(1, 10)
        val prev = byPilotKey[key]
        if (prev == null || score >= prev.resonance) {
          byPilotKey[key] =
            OverwatchPilotStandingRow(
              uidKey = key,
              callsign = live.callsign.trim().uppercase(Locale.US),
              syndicateTag = if (isSelf && cleanLocalTag.isNotBlank()) cleanLocalTag else cleanTag.ifBlank { prev?.syndicateTag.orEmpty() },
              shipId = ship,
              resonance = if (isSelf) maxOf(score, localLiveResonance) else maxOf(score, prev?.resonance ?: 0),
              isLocalPilot = isSelf || prev?.isLocalPilot == true,
            )
        }
      }

      if (normSelfCall.isNotBlank() && byPilotKey.values.none { it.isLocalPilot }) {
        val selfKey = normSelfUid.ifBlank { "local_self" }
        byPilotKey[selfKey] =
          OverwatchPilotStandingRow(
            uidKey = selfKey,
            callsign = normSelfCall,
            syndicateTag = cleanLocalTag,
            shipId = localActiveShipId.coerceIn(1, 10),
            resonance = localLiveResonance.coerceAtLeast(0),
            isLocalPilot = true,
          )
      }

      byPilotKey.values
        .sortedWith(
          compareByDescending<OverwatchPilotStandingRow> { it.resonance }
            .thenByDescending { it.isLocalPilot }
            .thenBy { it.callsign }
        )
        .take(25)
    }

  val shipStandings =
    remember(
      globalStandings,
      selectedShipFilter,
      localCallsign,
      cleanLocalTag,
      currentUserId,
      localLiveResonance,
    ) {
      val localShipRes = resolveLocalShipResonance(selectedShipFilter).coerceAtLeast(0)
      val filtered =
        globalStandings
          .mapNotNull { row ->
            when {
              row.isLocalPilot ->
                if (localShipRes > 0 || row.shipId == selectedShipFilter) {
                  row.copy(
                    shipId = selectedShipFilter,
                    resonance = if (localShipRes > 0) localShipRes else row.resonance,
                  )
                } else {
                  null
                }
              row.shipId == selectedShipFilter -> row
              else -> null
            }
          }
          .toMutableList()
      if (localCallsign.isNotBlank() && localShipRes > 0 && filtered.none { it.isLocalPilot }) {
        filtered.add(
          OverwatchPilotStandingRow(
            uidKey = currentUserId.ifBlank { "local_self" },
            callsign = localCallsign.trim().uppercase(Locale.US),
            syndicateTag = cleanLocalTag,
            shipId = selectedShipFilter,
            resonance = localShipRes,
            isLocalPilot = true,
          )
        )
      }
      filtered
        .sortedWith(
          compareByDescending<OverwatchPilotStandingRow> { it.resonance }
            .thenByDescending { it.isLocalPilot }
            .thenBy { it.callsign }
        )
        .take(25)
    }

  val syndicateStandings =
    remember(syndicateDirectory, globalStandings, cleanLocalTag) {
      val byTag = LinkedHashMap<String, OverwatchSyndicateStandingRow>()
      for (syn in syndicateDirectory) {
        if (!syn.isRealPlayerFoundedSyndicate()) continue
        val tag = syn.tag.trim().uppercase(Locale.US)
        if (tag.isBlank()) continue
        byTag[tag] =
          OverwatchSyndicateStandingRow(
            tag = tag,
            name = syn.name.trim().ifBlank { "[$tag] SYNDICATE" }.uppercase(Locale.US),
            memberCount = syn.memberCount.coerceAtLeast(1),
            combinedResonance = syn.combinedBounty.coerceAtLeast(0L),
            isLocalSyndicate = cleanLocalTag.isNotBlank() && tag == cleanLocalTag,
          )
      }
      val pilotGrouped = globalStandings.filter { it.syndicateTag.isNotBlank() }.groupBy { it.syndicateTag }
      for ((tag, members) in pilotGrouped) {
        val summed = members.sumOf { it.resonance.toLong().coerceAtLeast(0L) }
        val existing = byTag[tag]
        byTag[tag] =
          if (existing != null) {
            existing.copy(
              memberCount = maxOf(existing.memberCount, members.size),
              combinedResonance = maxOf(existing.combinedResonance, summed),
              isLocalSyndicate = existing.isLocalSyndicate || (cleanLocalTag.isNotBlank() && tag == cleanLocalTag),
            )
          } else {
            OverwatchSyndicateStandingRow(
              tag = tag,
              name = "[$tag] SYNDICATE",
              memberCount = members.size.coerceAtLeast(1),
              combinedResonance = summed,
              isLocalSyndicate = cleanLocalTag.isNotBlank() && tag == cleanLocalTag,
            )
          }
      }
      byTag.values
        .sortedWith(
          compareByDescending<OverwatchSyndicateStandingRow> { it.combinedResonance }
            .thenByDescending { it.memberCount }
            .thenBy { it.tag }
        )
        .take(25)
    }

  Column(
    modifier = Modifier.fillMaxWidth(0.88f).testTag("overwatch_resonance_leaderboard"),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(4.dp),
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
      ResonanceCrestIcon(sizeDp = 15.dp)
      Text(
        text = "RESONANCE LEADERBOARDS",
        color = Color(0xFFFFD700),
        fontSize = 10.6.sp,
        fontWeight = FontWeight.Black,
        fontFamily = FontFamily.Monospace,
        letterSpacing = 0.6.sp,
        maxLines = 1,
      )
    }

    // Top Filter Tabs (>= 48.dp touch targets): GLOBAL TOP | BY SHIP | SYNDICATES
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(4.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      val tabs =
        listOf(
          Triple(OverwatchLeaderboardTab.GLOBAL_TOP, "GLOBAL TOP", "overwatch_lb_tab_global"),
          Triple(OverwatchLeaderboardTab.BY_SHIP, "BY SHIP", "overwatch_lb_tab_ship"),
          Triple(OverwatchLeaderboardTab.SYNDICATES, "SYNDICATES", "overwatch_lb_tab_syndicates"),
        )
      tabs.forEach { (tab, label, tag) ->
        val active = selectedTab == tab
        Box(
          modifier =
            Modifier.weight(1f)
              .minimumInteractiveComponentSize()
              .height(48.dp)
              .clickable { selectedTab = tab }
              .padding(vertical = 6.dp)
              .testTag(tag),
          contentAlignment = Alignment.Center,
        ) {
          Box(
            modifier =
              Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(if (active) Color(0xFF0D3554) else Color(0xCC060D1A))
                .border(
                  1.dp,
                  if (active) Color(0xFF00F5D4) else Color(0x5538BDF8),
                  RoundedCornerShape(12.dp),
                )
                .padding(horizontal = 4.dp, vertical = 5.dp),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text = label,
              color = if (active) Color.White else Color(0xFF94A3B8),
              fontSize = 8.0.sp,
              fontWeight = FontWeight.Black,
              fontFamily = FontFamily.Monospace,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
      }
    }

    // Compact Hull Selector Row when BY SHIP is active (>= 48.dp touch targets)
    if (selectedTab == OverwatchLeaderboardTab.BY_SHIP) {
      Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        for (hullId in 1..10) {
          val selectedHull = selectedShipFilter == hullId
          val shortHull = canonicalLeaderboardShipLabel(hullId)
          Box(
            modifier =
              Modifier.minimumInteractiveComponentSize()
                .height(48.dp)
                .clickable { selectedShipFilter = hullId }
                .padding(vertical = 7.dp)
                .testTag("overwatch_lb_ship_chip_$hullId"),
            contentAlignment = Alignment.Center,
          ) {
            Box(
              modifier =
                Modifier.clip(RoundedCornerShape(10.dp))
                  .background(if (selectedHull) Color(0xFF132E4A) else Color(0xB8040B16))
                  .border(
                    1.dp,
                    if (selectedHull) Color(0xFFFFD700) else Color(0x4438BDF8),
                    RoundedCornerShape(10.dp),
                  )
                  .padding(horizontal = 7.dp, vertical = 4.dp),
            ) {
              Text(
                text = "$hullId $shortHull",
                color = if (selectedHull) Color(0xFFFFD700) else Color(0xFFCBD5E1),
                fontSize = 7.8.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
              )
            }
          }
        }
      }
    }

    // Scrollable Holographic Standings List (#1..#25)
    Column(
      modifier =
        Modifier.fillMaxWidth()
          .heightIn(min = 68.dp, max = if (selectedTab == OverwatchLeaderboardTab.BY_SHIP) 96.dp else 136.dp)
          .verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
      when (selectedTab) {
        OverwatchLeaderboardTab.GLOBAL_TOP,
        OverwatchLeaderboardTab.BY_SHIP -> {
          val rows = if (selectedTab == OverwatchLeaderboardTab.GLOBAL_TOP) globalStandings else shipStandings
          if (rows.isEmpty()) {
            Text(
              text = "No pilots logged on ${canonicalLeaderboardShipLabel(selectedShipFilter)} yet.",
              color = Color(0xFF94A3B8),
              fontSize = 8.6.sp,
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.Monospace,
              textAlign = TextAlign.Center,
              modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            )
          } else {
            rows.forEachIndexed { idx, row ->
              val rankNum = idx + 1
              val rowAccent =
                when {
                  rankNum == 1 -> Color(0xFFFACC15)
                  row.isLocalPilot -> Color(0xFF00F5D4)
                  else -> Color(0xFFE2E8F0)
                }
              val synPrefix = if (row.syndicateTag.isNotBlank()) "[${row.syndicateTag}] " else ""
              Row(
                modifier =
                  Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                      when {
                        rankNum == 1 -> Color(0x38FACC15)
                        row.isLocalPilot -> Color(0x2E00F5D4)
                        else -> Color(0x99040A16)
                      }
                    )
                    .padding(horizontal = 7.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Text(
                  text = "#$rankNum",
                  color = if (rankNum == 1) Color(0xFFFACC15) else Color(0xFF38BDF8),
                  fontSize = 8.4.sp,
                  fontWeight = FontWeight.Black,
                  fontFamily = FontFamily.Monospace,
                  modifier = Modifier.width(26.dp),
                )
                Column(modifier = Modifier.weight(1f).padding(end = 4.dp)) {
                  Text(
                    text = "$synPrefix${row.callsign}",
                    color = rowAccent,
                    fontSize = 8.8.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                  Text(
                    text = canonicalLeaderboardShipLabel(row.shipId),
                    color = Color(0xFF94A3B8),
                    fontSize = 7.4.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                }
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                  ResonanceCrestIcon(sizeDp = 11.dp)
                  Text(
                    text = formatOverwatchResonance(row.resonance),
                    color = if (rankNum == 1) Color(0xFFFACC15) else Color(0xFF00F5D4),
                    fontSize = 8.8.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                  )
                }
              }
            }
          }
        }
        OverwatchLeaderboardTab.SYNDICATES -> {
          if (syndicateStandings.isEmpty()) {
            Text(
              text = "No chartered player Syndicates ranked yet.",
              color = Color(0xFF94A3B8),
              fontSize = 8.6.sp,
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.Monospace,
              textAlign = TextAlign.Center,
              modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            )
          } else {
            syndicateStandings.forEachIndexed { idx, syn ->
              val rankNum = idx + 1
              val synAccent =
                when {
                  rankNum == 1 -> Color(0xFFFACC15)
                  syn.isLocalSyndicate -> Color(0xFF00F5D4)
                  else -> Color.White
                }
              Row(
                modifier =
                  Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                      when {
                        rankNum == 1 -> Color(0x38FACC15)
                        syn.isLocalSyndicate -> Color(0x2E00F5D4)
                        else -> Color(0x99040A16)
                      }
                    )
                    .padding(horizontal = 7.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Text(
                  text = "#$rankNum",
                  color = if (rankNum == 1) Color(0xFFFACC15) else Color(0xFF38BDF8),
                  fontSize = 8.4.sp,
                  fontWeight = FontWeight.Black,
                  fontFamily = FontFamily.Monospace,
                  modifier = Modifier.width(26.dp),
                )
                Column(modifier = Modifier.weight(1f).padding(end = 4.dp)) {
                  Text(
                    text = "[${syn.tag}] ${syn.name}",
                    color = synAccent,
                    fontSize = 8.6.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                  Text(
                    text = if (syn.memberCount == 1) "1 PILOT" else "${syn.memberCount} PILOTS",
                    color = Color(0xFF94A3B8),
                    fontSize = 7.4.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                  )
                }
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                  ResonanceCrestIcon(sizeDp = 11.dp)
                  Text(
                    text = String.format(Locale.US, "%,d", syn.combinedResonance),
                    color = if (rankNum == 1) Color(0xFFFACC15) else Color(0xFF00F5D4),
                    fontSize = 8.8.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                  )
                }
              }
            }
          }
        }
      }
    }

    Box(
      modifier =
        Modifier.minimumInteractiveComponentSize()
          .height(48.dp)
          .clickable { onDismissPrompt() }
          .padding(vertical = 6.dp)
          .testTag("overwatch_leaderboard_close_button"),
      contentAlignment = Alignment.Center,
    ) {
      Box(
        modifier =
          Modifier.clip(CircleShape)
            .background(Color(0xCC060D1A))
            .border(1.dp, Color(0xFF64748B), CircleShape)
            .padding(horizontal = 12.dp, vertical = 5.dp),
      ) {
        Text(
          text = "CLOSE",
          color = Color(0xFFCBD5E1),
          fontSize = 8.4.sp,
          fontWeight = FontWeight.Black,
          fontFamily = FontFamily.Monospace,
        )
      }
    }
  }
}

@Composable
private fun ConduitLeftDirectivesBay(
  isTabletDeck: Boolean,
  headerHeight: Dp,
  wingStackGap: Dp,
  liveResonance: Int,
  conduitTier: com.example.engine.ConduitRankTier,
  directives: List<DailyConduitDirective>,
  isSweepClaimed: Boolean,
  onBackToHub: () -> Unit,
  onOpenLeaderboards: () -> Unit = {},
  onClaimDirective: (Int) -> Unit,
  onRerollDirective: (Int) -> Unit,
  onClaimSweep: () -> Unit,
) {
  val nextMilestone = remember(liveResonance) { RiftProgressionCatalog.getNextConduitTierThreshold(liveResonance) }
  val tierProgress = remember(liveResonance) { RiftProgressionCatalog.getConduitProgressFraction(liveResonance) }
  val allClaimed = directives.size >= 3 && directives.all { it.claimed }

  Column(
    modifier = Modifier.fillMaxSize().testTag("conduit_left_directives_bay"),
    verticalArrangement = Arrangement.spacedBy(wingStackGap),
  ) {
    HubDigitizedReturnBar(
      heightDp = headerHeight,
      isRightSide = false,
      isTabletDeck = isTabletDeck,
      sectionTitle = "RIFT CONDUIT",
      accent = Color(0xFFFFD700),
      trailingLabel = "STANDINGS",
      trailingTag = "conduit_open_leaderboards",
      onTrailingClick = onOpenLeaderboards,
      onBackToHub = onBackToHub,
    )

    Column(
      modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(wingStackGap),
    ) {
      // Rank Tier Progress Pod
      Column(
        modifier =
          Modifier.fillMaxWidth()
            .chamferedSciFiPodSurface(
              isRightSide = false,
              primaryBorder = Color(0xFFFFD700),
              secondaryBorder = Color(0xFF00F5D4),
            )
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = conduitTier.title.uppercase(Locale.US),
            color = Color.White,
            fontSize = 10.0.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
          )
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            ResonanceCrestIcon(sizeDp = 12.dp)
            Text(
              text = String.format(Locale.US, "%,d", liveResonance),
              color = Color(0xFFFFD700),
              fontSize = 9.8.sp,
              fontWeight = FontWeight.Black,
              fontFamily = FontFamily.Monospace,
            )
          }
        }
        CircularPromptStatBar(
          modifier = Modifier.fillMaxWidth(),
          label = "NEXT",
          fraction = tierProgress,
          color = Color(0xFF00F5D4),
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
          Text(
            text = "NEXT TIER: ${nextMilestone.second} @",
            color = Color(0xFF94A3B8),
            fontSize = 8.6.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
          )
          ResonanceCrestIcon(sizeDp = 10.5.dp)
          Text(
            text = "${nextMilestone.first}",
            color = Color(0xFFFFD700),
            fontSize = 8.6.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
          )
        }
      }

      directives.forEach { dir ->
        Column(
          modifier =
            Modifier.fillMaxWidth()
              .chamferedSciFiPodSurface(
                isRightSide = false,
                primaryBorder = if (dir.isComplete && !dir.claimed) Color(0xFF00F5D4) else Color(0xFF38BDF8),
                secondaryBorder = Color(0xFF091528),
              )
              .padding(horizontal = 8.dp, vertical = 6.dp),
          verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
          Text(
            text = dir.title.uppercase(Locale.US),
            color = Color.White,
            fontSize = 10.0.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
          )
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
              text = "${dir.progressValue}/${dir.targetValue} •",
              color = Color(0xFFCBD5E1),
              fontSize = 8.8.sp,
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.Monospace,
              maxLines = 1,
            )
            RiftShardGemIcon(sizeDp = 11.dp)
            Text(
              text = "+${dir.shardReward} •",
              color = Color(0xFF38BDF8),
              fontSize = 8.8.sp,
              fontWeight = FontWeight.Black,
              fontFamily = FontFamily.Monospace,
              maxLines = 1,
            )
            ResonanceCrestIcon(sizeDp = 11.dp)
            Text(
              text = "+${dir.resonanceReward}",
              color = Color(0xFFFFD700),
              fontSize = 8.8.sp,
              fontWeight = FontWeight.Black,
              fontFamily = FontFamily.Monospace,
              maxLines = 1,
            )
          }
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Box(
              modifier =
                Modifier.minimumInteractiveComponentSize()
                  .height(48.dp)
                  .clickable(enabled = dir.isComplete && !dir.claimed) { onClaimDirective(dir.slotIndex) }
                  .padding(vertical = 6.dp)
                  .testTag("conduit_claim_slot_${dir.slotIndex}"),
              contentAlignment = Alignment.CenterStart,
            ) {
              Box(
                modifier =
                  Modifier.clip(RoundedCornerShape(6.dp))
                    .background(if (dir.isComplete && !dir.claimed) Color(0xFF0D3554) else Color(0xFF091528))
                    .border(
                      1.dp,
                      if (dir.isComplete && !dir.claimed) Color(0xFF00F5D4) else Color(0xFF475569),
                      RoundedCornerShape(6.dp),
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp),
              ) {
                Text(
                  text = when {
                    dir.claimed -> "CLAIMED"
                    dir.isComplete -> "CLAIM >"
                    else -> "IN PROGRESS"
                  },
                  color = if (dir.isComplete && !dir.claimed) Color.White else Color(0xFF94A3B8),
                  fontSize = 7.6.sp,
                  fontWeight = FontWeight.Black,
                  fontFamily = FontFamily.Monospace,
                )
              }
            }
            if (!dir.claimed && !dir.isComplete) {
              Box(
                modifier =
                  Modifier.minimumInteractiveComponentSize()
                    .height(48.dp)
                    .clickable { onRerollDirective(dir.slotIndex) }
                    .padding(vertical = 6.dp)
                    .testTag("conduit_reroll_slot_${dir.slotIndex}"),
                contentAlignment = Alignment.CenterEnd,
              ) {
                Text(
                  text = "REROLL",
                  color = Color(0xFFFACC15),
                  fontSize = 7.6.sp,
                  fontWeight = FontWeight.Black,
                  fontFamily = FontFamily.Monospace,
                )
              }
            }
          }
        }
      }

      if (allClaimed && !isSweepClaimed) {
        Box(
          modifier =
            Modifier.fillMaxWidth()
              .minimumInteractiveComponentSize()
              .height(48.dp)
              .clickable { onClaimSweep() }
              .padding(vertical = 4.dp)
              .testTag("conduit_claim_sweep_bonus"),
          contentAlignment = Alignment.Center,
        ) {
          Box(
            modifier =
              Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF0D3554))
                .border(1.2.dp, Color(0xFFFFD700), RoundedCornerShape(8.dp))
                .padding(vertical = 6.dp),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text = "CLAIM DAILY SWEEP BONUS >",
              color = Color(0xFFFFD700),
              fontSize = 8.2.sp,
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
private fun OverwatchInspectLeftInfoBay(
  isTabletDeck: Boolean,
  headerHeight: Dp,
  wingStackGap: Dp,
  activeInstance: OverwatchRiftInstance,
  liveRemainingSec: Int,
  liveAvgResonance: Int,
  realHumanCount: Int,
  onBackToHub: () -> Unit,
  onRiftIn: () -> Unit,
  onOpenActiveRifts: () -> Unit,
) {
  val countdownText = remember(liveRemainingSec) { formatOverwatchTimerMmSs(liveRemainingSec) }
  val customCfg = activeInstance.customConfig
  val modeSummary =
    when {
      customCfg != null ->
        "Player-created Custom Rift (${customCfg.baseMode.name.replace("_", " ")}). Damage Taken ${(customCfg.damageTakenScalePct)}% • Weapon Flux Cost ${(customCfg.weaponFluxCostScalePct)}% • Speed ${String.format(Locale.US, "%.2fx", customCfg.speedMult)}."
      activeInstance.rankedMode == RankedGameMode.FLUX_CORE ->
        "Two teams battle around the central Safe Zone to secure and escort the Flux Core while contesting Supply Crates and Playfield Rifts."
      else ->
        "Open free-for-all sector combat around the central Safe Zone. Build Resonance, harvest Supply Crates, and ride Playfield Rifts."
    }

  Column(
    modifier = Modifier.fillMaxSize().testTag("overwatch_inspect_left_info_bay"),
    verticalArrangement = Arrangement.spacedBy(wingStackGap),
  ) {
    HubDigitizedReturnBar(
      heightDp = headerHeight,
      isRightSide = false,
      isTabletDeck = isTabletDeck,
      sectionTitle = "RIFT INTEL",
      accent = Color(0xFF00F5D4),
      trailingLabel = "ACTIVE RIFTS >",
      trailingTag = "overwatch_inspect_open_modes",
      onTrailingClick = onOpenActiveRifts,
      onBackToHub = onBackToHub,
    )

    Column(
      modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(wingStackGap),
    ) {
      Column(
        modifier =
          Modifier.fillMaxWidth()
            .chamferedSciFiPodSurface(
              isRightSide = false,
              primaryBorder = Color(0xFF00F5D4),
              secondaryBorder = Color(0xFFFFD700),
            )
            .padding(horizontal = 9.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
      ) {
        Text(
          text = activeInstance.modeTitle.uppercase(Locale.US),
          color = Color.White,
          fontSize = if (isTabletDeck) 10.6.sp else 9.4.sp,
          fontWeight = FontWeight.Black,
          fontFamily = FontFamily.Monospace,
        )
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
          Text(
            text = "$countdownText LEFT ·",
            color = Color(0xFF00F5D4),
            fontSize = 8.2.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = FontFamily.Monospace,
          )
          ResonanceCrestIcon(sizeDp = 12.dp)
          Text(
            text = "${formatOverwatchResonance(liveAvgResonance)} AVG",
            color = Color(0xFFFFD700),
            fontSize = 8.8.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
          )
        }
        Text(
          text =
            if (realHumanCount == 1) "1 REAL PILOT ACTIVE IN SECTOR"
            else "$realHumanCount REAL PILOTS ACTIVE IN SECTOR",
          color = if (realHumanCount > 0) Color(0xFF38BDF8) else Color(0xFF94A3B8),
          fontSize = 7.8.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.Monospace,
        )
      }

      Column(
        modifier =
          Modifier.fillMaxWidth()
            .chamferedSciFiPodSurface(
              isRightSide = false,
              primaryBorder = Color(0xFF38BDF8),
              secondaryBorder = Color(0xFF091528),
            )
            .padding(horizontal = 9.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        Text(
          text = if (customCfg != null) "CUSTOM SANDBOX PARAMETERS" else "SECTOR OBJECTIVE",
          color = Color(0xFF38BDF8),
          fontSize = 7.8.sp,
          fontWeight = FontWeight.Black,
          fontFamily = FontFamily.Monospace,
        )
        Text(
          text = modeSummary,
          color = Color(0xFFCBD5E1),
          fontSize = 7.6.sp,
          lineHeight = 11.0.sp,
          fontFamily = FontFamily.SansSerif,
        )
      }

      Box(
        modifier =
          Modifier.fillMaxWidth()
            .minimumInteractiveComponentSize()
            .height(48.dp)
            .clickable { onRiftIn() }
            .padding(vertical = 3.dp)
            .testTag("overwatch_inspect_rift_in"),
        contentAlignment = Alignment.Center,
      ) {
        Box(
          modifier =
            Modifier.fillMaxWidth()
              .clip(RoundedCornerShape(8.dp))
              .background(Brush.horizontalGradient(listOf(Color(0xFF0D3349), Color(0xFF091A2E))))
              .border(
                1.2.dp,
                Brush.horizontalGradient(listOf(Color(0xFF00F5D4), Color(0xFFFFD700))),
                RoundedCornerShape(8.dp),
              )
              .padding(vertical = 8.dp),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = "RIFT IN >",
            color = Color.White,
            fontSize = if (isTabletDeck) 10.2.sp else 9.2.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
          )
        }
      }
    }
  }
}

@Composable
private fun OverwatchInspectRightPilotsBay(
  isTabletDeck: Boolean,
  headerHeight: Dp,
  wingStackGap: Dp,
  pilots: List<SquadronOnlinePilotItem>,
  selectedPilotUid: String,
  currentUserId: String,
  onTapPilot: (SquadronOnlinePilotItem) -> Unit,
  onOpenSquadronBay: () -> Unit,
) {
  Column(
    modifier = Modifier.fillMaxSize().testTag("overwatch_inspect_right_pilots_bay"),
    verticalArrangement = Arrangement.spacedBy(wingStackGap),
  ) {
    HubDigitizedReturnBar(
      heightDp = headerHeight,
      isRightSide = true,
      isTabletDeck = isTabletDeck,
      sectionTitle = "PILOTS (${pilots.size})",
      accent = Color(0xFF38BDF8),
      trailingLabel = "SQUADRON >",
      trailingTag = "overwatch_inspect_open_squadron",
      onTrailingClick = onOpenSquadronBay,
    )

    Column(
      modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(wingStackGap),
    ) {
      if (pilots.isEmpty()) {
        Box(
          modifier =
            Modifier.fillMaxWidth()
              .chamferedSciFiPodSurface(
                isRightSide = true,
                primaryBorder = Color(0xFF38BDF8),
                secondaryBorder = Color(0xFF091528),
              )
              .padding(10.dp),
        ) {
          Text(
            text = "NO OTHER HUMAN PILOTS IN THIS RIFT YET. TAP RIFT IN TO CLAIM SECTOR CONTROL.",
            color = Color(0xFF94A3B8),
            fontSize = 7.6.sp,
            lineHeight = 10.8.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
          )
        }
      } else {
        pilots.forEach { pilot ->
          val isSelected = selectedPilotUid.isNotBlank() && selectedPilotUid == pilot.uid
          val isSelf = currentUserId.isNotBlank() && pilot.uid == currentUserId
          val shipId = (if (pilot.lockedShipId in 1..10) pilot.lockedShipId else pilot.bestShipId).coerceIn(1, 10)
          val shipName =
            remember(shipId) {
              RiftCodex.getShipById(shipId).name.replace(Regex("^[0-9]+-"), "").trim().uppercase(Locale.US)
            }
          val displayCallsign =
            if (pilot.syndicateTag.isNotBlank()) "[${pilot.syndicateTag}] ${pilot.callsign}"
            else pilot.callsign

          Row(
            modifier =
              Modifier.fillMaxWidth()
                .minimumInteractiveComponentSize()
                .chamferedSciFiPodSurface(
                  isRightSide = true,
                  primaryBorder = if (isSelected) Color(0xFF00F5D4) else Color(0xFF38BDF8),
                  secondaryBorder = if (pilot.isInSquadron) Color(0xFFFFD700) else Color(0xFF091528),
                )
                .clickable { if (!isSelf) onTapPilot(pilot) }
                .padding(horizontal = 8.dp, vertical = 5.dp)
                .testTag("overwatch_pilot_${pilot.uid}"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
          ) {
            Spinning25dShipWithOrbitingFlux(
              shipId = shipId,
              shipSpinDeg = 0f,
              fluxOrbitRad = 0f,
              isCharged = pilot.isInSquadron,
              isBestShip = true,
              isSelected = isSelected,
              sizeDp = if (isTabletDeck) 44.dp else 36.dp,
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
              Text(
                text = if (isSelf) "$displayCallsign (YOU)" else displayCallsign,
                color = Color.White,
                fontSize = if (isTabletDeck) 8.8.sp else 7.8.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
              ) {
                ResonanceCrestIcon(sizeDp = 10.5.dp)
                Text(
                  text = "${formatOverwatchResonance(pilot.resonance)} • $shipName",
                  color = Color(0xFFFFD700),
                  fontSize = 8.0.sp,
                  fontWeight = FontWeight.Black,
                  fontFamily = FontFamily.Monospace,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
            }
            if (!isSelf) {
              Text(
                text = if (pilot.isInSquadron) "SQUAD" else "INSPECT >",
                color = if (pilot.isInSquadron) Color(0xFF00F5D4) else Color(0xFF38BDF8),
                fontSize = 8.0.sp,
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

@Composable
internal fun HelmStepperControlRow(
  label: String,
  valueText: String,
  onMinus: () -> Unit,
  onPlus: () -> Unit,
  onTapRow: (() -> Unit)? = null,
) {
  Row(
    modifier =
      Modifier.fillMaxWidth()
        .height(48.dp)
        .chamferedSciFiPodSurface(
          isRightSide = false,
          primaryBorder = Color(0xFF38BDF8),
          secondaryBorder = Color(0xFF091528),
        )
        .let { mod -> if (onTapRow != null) mod.clickable { onTapRow() } else mod }
        .padding(horizontal = 6.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      text = label,
      color = Color(0xFFE2E8F0),
      fontSize = 8.6.sp,
      fontWeight = FontWeight.Black,
      fontFamily = FontFamily.Monospace,
    )
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
      Box(
        modifier =
          Modifier.minimumInteractiveComponentSize()
            .size(48.dp)
            .clickable { onMinus() },
        contentAlignment = Alignment.Center,
      ) {
        Text("[-]", color = Color(0xFF00F5D4), fontSize = 9.6.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
      }
      Text(
        text = valueText,
        color = Color(0xFFFFD700),
        fontSize = 9.0.sp,
        fontWeight = FontWeight.Black,
        fontFamily = FontFamily.Monospace,
        modifier = Modifier.let { mod -> if (onTapRow != null) mod.clickable { onTapRow() } else mod },
      )
      Box(
        modifier =
          Modifier.minimumInteractiveComponentSize()
            .size(48.dp)
            .clickable { onPlus() },
        contentAlignment = Alignment.Center,
      ) {
        Text("[+]", color = Color(0xFF00F5D4), fontSize = 9.6.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
      }
    }
  }
}



/**
 * Explicit `#060D1A` Sci-Fi Tablet Warp-Off Confirmation Dialog triggered strictly when intentionally
 * tapping `LEAVE MATCH` from an active Rift instance (never shown when browsing Hub modules or `[TUTORIAL]`).
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
            text = "CONFIRM RIFT OUT // LEAVE ACTIVE RIFT",
            color = Color(0xFFFB7185),
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 0.6.sp,
          )
        }

        Text(
          text = "Sure you want to Rift Out from your active Rift match and return to peaceful Rift Station orbit?",
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
              text = "STAY IN RIFT",
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
              text = "RIFT OUT",
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
private fun NexusSwitchAccountConfirmationModal(
  onCancel: () -> Unit,
  onConfirmSwitchAccount: () -> Unit,
) {
  Box(
    modifier =
      Modifier.fillMaxSize()
        .background(Color(0xCC02050B))
        .clickable(onClick = onCancel)
        .testTag("nexus_switch_account_confirm_modal"),
    contentAlignment = Alignment.Center,
  ) {
    Surface(
      modifier =
        Modifier.widthIn(min = 300.dp, max = 460.dp)
          .fillMaxWidth(0.76f)
          .clickable(enabled = false) {}
          .border(
            1.4.dp,
            Brush.linearGradient(listOf(Color(0xFFFACC15), Color(0xFF00F5D4), Color(0xFF38BDF8))),
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
            text = "CONFIRM ACCOUNT SWITCH",
            color = Color(0xFFFEF08A),
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 0.6.sp,
          )
        }

        Text(
          text =
            "Switch to another Pilot account? Your current cloud progress is saved, and you will return to the Pilot sign-in screen.",
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
                .clickable { onCancel() }
                .padding(horizontal = 14.dp, vertical = 7.dp)
                .testTag("switch_account_cancel_button"),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text = "CANCEL",
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
                .background(Color(0xFF2B2208))
                .border(1.2.dp, Color(0xFFFACC15), RoundedCornerShape(8.dp))
                .clickable { onConfirmSwitchAccount() }
                .padding(horizontal = 14.dp, vertical = 7.dp)
                .testTag("switch_account_confirm_button"),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text = "SWITCH ACCOUNT",
              color = Color(0xFFFEF08A),
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
  @Suppress("UNUSED_PARAMETER") syndicateSubDeck: SyndicateCitadelSubDeck,
  @Suppress("UNUSED_PARAMETER") onSelectSyndicateSubDeck: (SyndicateCitadelSubDeck) -> Unit,
  @Suppress("UNUSED_PARAMETER") archiveSubDeck: ArchiveCodexSubDeck,
  @Suppress("UNUSED_PARAMETER") onSelectArchiveSubDeck: (ArchiveCodexSubDeck) -> Unit,
  @Suppress("UNUSED_PARAMETER") onOpenCodexInHelm: () -> Unit = {},
  @Suppress("UNUSED_PARAMETER") armadaCount: Int,
  showClearAi: Boolean,
  isInMatch: Boolean,
  @Suppress("UNUSED_PARAMETER") onSelectHubCard: (NexusCoreNode) -> Unit = {},
  onBackToHub: () -> Unit,
  onClearAi: () -> Unit,
  onLeaveMatch: () -> Unit,
  onEnterPracticeRift: () -> Unit,
  onCloseToArena: () -> Unit,
) {
  val (title, accent) =
    when (activeNode) {
      NexusCoreNode.SUBSPACE_COMMS -> "SUBSPACE COMMS" to Color(0xFF00F5D4)
      NexusCoreNode.CONVERGENCE_GATE -> "CONVERGENCE GATE" to Color(0xFF00F5D4)
      NexusCoreNode.HANGAR_DECK -> "HANGAR DECK" to Color(0xFF38BDF8)
      NexusCoreNode.RIFT_CONDUIT -> "RIFT CONDUIT" to Color(0xFFE879F9)
      NexusCoreNode.SYNDICATE_CITADEL -> "SYNDICATE CITADEL" to Color(0xFFFACC15)
      NexusCoreNode.ARCHIVE_CODEX -> "PILOT DOSSIER" to Color(0xFF34D399)
      NexusCoreNode.HELM_SYSTEMS -> "HELM SYSTEMS" to Color(0xFF94A3B8)
      NexusCoreNode.STAR_FORGE -> "STAR FORGE" to Color(0xFFC084FC)
      NexusCoreNode.ORBITAL_HUB -> "RIFT HUB" to Color(0xFF00F5D4)
    }

  // Clean, stripped-down full-screen header (zero legacy multi-tab switcher strip)
  Row(
    modifier =
      Modifier.fillMaxWidth()
        .height(48.dp)
        .clip(RoundedCornerShape(10.dp))
        .background(
          Brush.verticalGradient(
            listOf(Color(0xFF10203A), Color(0xFF071122), Color(0xFF040A15))
          )
        )
        .border(
          1.15.dp,
          Brush.horizontalGradient(
            listOf(accent.copy(alpha = 0.80f), Color(0x66475569), Color(0x9900F5D4))
          ),
          RoundedCornerShape(10.dp),
        )
        .padding(horizontal = 8.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Row(
      modifier = Modifier.weight(1f).fillMaxHeight(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Box(
        modifier =
          Modifier.minimumInteractiveComponentSize()
            .height(48.dp)
            .clickable { onBackToHub() }
            .padding(vertical = 6.dp)
            .testTag("nexus_btn_back_to_hub"),
        contentAlignment = Alignment.Center,
      ) {
        Box(
          modifier =
            Modifier.clip(RoundedCornerShape(7.dp))
              .background(Brush.verticalGradient(listOf(Color(0xFF122C47), Color(0xFF08182B))))
              .border(1.dp, Color(0xFF00F5D4), RoundedCornerShape(7.dp))
              .padding(horizontal = 10.dp, vertical = 5.5.dp),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = "< RIFT HUB",
            color = Color(0xFF00F5D4),
            fontSize = 9.4.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
          )
        }
      }
      GlassTerminalHeaderGlyph(node = activeNode, accent = accent, sizeDp = 22)
      Text(
        text = title,
        color = accent,
        fontSize = 11.4.sp,
        fontWeight = FontWeight.Black,
        fontFamily = FontFamily.Monospace,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }

    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
      if (showClearAi) {
        Box(
          modifier =
            Modifier.minimumInteractiveComponentSize()
              .height(48.dp)
              .clickable { onClearAi() }
              .padding(vertical = 6.dp)
              .testTag("nexus_clear_button"),
          contentAlignment = Alignment.Center,
        ) {
          Box(
            modifier =
              Modifier.clip(RoundedCornerShape(7.dp))
                .background(Color(0xFF131F33))
                .border(1.dp, Color(0xFF64748B), RoundedCornerShape(7.dp))
                .padding(horizontal = 9.dp, vertical = 5.dp),
            contentAlignment = Alignment.Center,
          ) {
            Text("CLEAR AI", color = Color(0xFFCBD5E1), fontSize = 8.8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
          }
        }
      }
      if (isInMatch) {
        Box(
          modifier =
            Modifier.minimumInteractiveComponentSize()
              .height(48.dp)
              .clickable { onLeaveMatch() }
              .padding(vertical = 6.dp)
              .testTag("nexus_leave_match_button"),
          contentAlignment = Alignment.Center,
        ) {
          Box(
            modifier =
              Modifier.clip(RoundedCornerShape(7.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFF3B1020), Color(0xFF1E0710))))
                .border(1.1.dp, Color(0xFFFB7185), RoundedCornerShape(7.dp))
                .padding(horizontal = 9.dp, vertical = 5.dp),
            contentAlignment = Alignment.Center,
          ) {
            Text("RIFT OUT", color = Color(0xFFFFCDD5), fontSize = 8.8.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
          }
        }
      }
      Box(
        modifier =
          Modifier.minimumInteractiveComponentSize()
            .height(48.dp)
            .clickable {
              if (isInMatch) {
                onCloseToArena()
              } else {
                onEnterPracticeRift()
              }
            }
            .padding(vertical = 6.dp)
            .testTag("nexus_close_button"),
        contentAlignment = Alignment.Center,
      ) {
        Box(
          modifier =
            Modifier.clip(RoundedCornerShape(7.dp))
              .background(Brush.verticalGradient(listOf(Color(0xFF0D3349), Color(0xFF061B2B))))
              .border(1.1.dp, Color(0xFF00F5D4), RoundedCornerShape(7.dp))
              .padding(horizontal = 10.dp, vertical = 5.dp),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = if (isInMatch) "RETURN TO RIFT >" else "TUTORIAL >",
            color = Color(0xFFECFEFF),
            fontSize = 9.6.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
          )
        }
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
  val pillShape = CutCornerShape(topStart = 6.dp, topEnd = 3.dp, bottomEnd = 6.dp, bottomStart = 3.dp)
  Box(
    modifier =
      Modifier.minimumInteractiveComponentSize()
        .height(48.dp)
        .clickable { onClick() }
        .padding(vertical = 6.dp)
        .testTag(tag),
    contentAlignment = Alignment.Center,
  ) {
    Box(
      modifier =
        Modifier.clip(pillShape)
          .background(
            Brush.verticalGradient(
              if (selected) {
                listOf(accent.copy(alpha = 0.34f), Color(0xFF09182B))
              } else {
                listOf(Color(0xFF132238), Color(0xFF081120))
              }
            )
          )
          .border(1.1.dp, if (selected) accent else Color(0xFF475569), pillShape)
          .padding(horizontal = 9.dp, vertical = 4.5.dp),
      contentAlignment = Alignment.Center,
    ) {
      Text(
        text = label,
        color = if (selected) Color.White else Color(0xFFCBD5E1),
        fontSize = 8.8.sp,
        fontWeight = FontWeight.ExtraBold,
        fontFamily = FontFamily.Monospace,
      )
    }
  }
}

@Composable
private fun ConvergenceGateZeroScrollDeck(
  gateTab: ConvergenceGateTab,
  onSelectGateTab: (ConvergenceGateTab) -> Unit,
  activeArmadaName: String,
  armadaSize: Int,
  syndicateTag: String,
  hangarShipSummaryLabel: String = "VIPER (BEST)",
  activeShipId: Int = 1,
  bestShipId: Int = 1,
  onSelectShipInPlace: (Int) -> Unit = {},
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
  publicCustomGames: List<CustomGameConfig> = emptyList(),
  onlineHumanPilots: List<LivePilotState> = emptyList(),
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
  customBotCount: Int = 12,
  onSelectCustomBotCount: (Int) -> Unit = {},
  customBotSkillLevel: Int = 1,
  onSelectCustomBotSkillLevel: (Int) -> Unit = {},
  customScoreCap: Int = 50,
  onSelectCustomScoreCap: (Int) -> Unit = {},
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
  customDamageTakenScalePct: Int = 100,
  onSelectCustomDamageTakenScalePct: (Int) -> Unit = {},
  customWeaponFluxCostScalePct: Int = 100,
  onSelectCustomWeaponFluxCostScalePct: (Int) -> Unit = {},
  onReturnToRankedFfa: () -> Unit,
  onJoinCustomInstance: (String) -> Unit,
  onLaunchCustomGame: () -> Unit,
) {
  var showGateInlineShipPicker by remember { mutableStateOf(false) }
  val allGateShips = remember(RiftSpriteBaker.customShip9Version) { RiftCodex.getAllNineShips() }

  Column(
    modifier = Modifier.fillMaxSize().testTag("nexus_armada_and_ranked_deck"),
    verticalArrangement = Arrangement.spacedBy(6.dp),
  ) {
    // Sculpted Sub-Bar: Official Ranked vs Custom Rift Gate + Squadron Chip + In-Place Clickable Ship Selector Pill
    val gateSubBarShape = CutCornerShape(topStart = 9.dp, topEnd = 5.dp, bottomEnd = 9.dp, bottomStart = 5.dp)
    Row(
      modifier =
        Modifier.fillMaxWidth()
          .heightIn(min = 48.dp)
          .clip(gateSubBarShape)
          .background(
            Brush.verticalGradient(listOf(Color(0xFF102038), Color(0xFF071120), Color(0xFF040A14)))
          )
          .border(
            1.1.dp,
            Brush.horizontalGradient(listOf(Color(0x8800F5D4), Color(0x55475569), Color(0x8838BDF8))),
            gateSubBarShape,
          )
          .padding(horizontal = 8.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Row(
        modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
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
            Modifier.minimumInteractiveComponentSize()
              .height(48.dp)
              .clickable { onOpenStrikeWing() }
              .padding(vertical = 6.dp)
              .testTag("nexus_btn_manage_armada"),
          contentAlignment = Alignment.Center,
        ) {
          val squadronText =
            if (activeArmadaName.isNotBlank()) "SQUADRON: [$activeArmadaName] ($armadaSize/5)"
            else "SQUADRON: SOLO (1/5)"
          Box(
            modifier =
              Modifier.clip(RoundedCornerShape(6.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFF132D4A), Color(0xFF09182B))))
                .border(1.dp, Color(0xFF38BDF8), RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp, vertical = 4.5.dp),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              squadronText,
              color = Color(0xFF7DD3FC),
              fontSize = 8.2.sp,
              fontWeight = FontWeight.ExtraBold,
              fontFamily = FontFamily.Monospace,
              maxLines = 1,
            )
          }
        }
      }

      Spacer(modifier = Modifier.width(6.dp))
      Box(
        modifier =
          Modifier.minimumInteractiveComponentSize()
            .height(48.dp)
            .clickable { showGateInlineShipPicker = !showGateInlineShipPicker }
            .padding(vertical = 6.dp)
            .testTag("gate_ship_selector_pill"),
        contentAlignment = Alignment.Center,
      ) {
        Box(
          modifier =
            Modifier.clip(CutCornerShape(topStart = 6.dp, topEnd = 3.dp, bottomEnd = 6.dp, bottomStart = 3.dp))
              .background(
                Brush.verticalGradient(
                  if (showGateInlineShipPicker) {
                    listOf(Color(0xFF28220A), Color(0xFF121006))
                  } else {
                    listOf(Color(0xFF0E2C3B), Color(0xFF071624))
                  }
                )
              )
              .border(
                1.1.dp,
                if (showGateInlineShipPicker) Color(0xFFFACC15) else Color(0xFF00F5D4),
                CutCornerShape(topStart = 6.dp, topEnd = 3.dp, bottomEnd = 6.dp, bottomStart = 3.dp),
              )
              .padding(horizontal = 8.5.dp, vertical = 4.5.dp),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text =
              if (showGateInlineShipPicker) "SHIP: $hangarShipSummaryLabel [CLOSE]"
              else "SHIP: $hangarShipSummaryLabel [CHANGE]",
            color = if (showGateInlineShipPicker) Color(0xFFFEF08A) else Color(0xFF00F5D4),
            fontSize = 8.2.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }
    }

    if (showGateInlineShipPicker) {
      Row(
        modifier =
          Modifier.fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFF0B182C), Color(0xFF050B16))))
            .border(1.1.dp, Color(0xFF00F5D4).copy(alpha = 0.72f), RoundedCornerShape(8.dp))
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp)
            .testTag("gate_inline_ship_picker_row"),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "DEPLOY HULL:",
          color = Color(0xFF94A3B8),
          fontSize = 8.2.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.Monospace,
        )
        allGateShips.forEach { ship ->
          val isActiveHull = ship.id == activeShipId.coerceIn(1, 10)
          val isBestHull = ship.id == bestShipId.coerceIn(1, 10)
          val cleanShipTitle =
            ship.name.replace(Regex("^[0-9]+-"), "").trim().uppercase(Locale.US)
          Box(
            modifier =
              Modifier.minimumInteractiveComponentSize()
                .height(48.dp)
                .clickable {
                  onSelectShipInPlace(ship.id)
                  showGateInlineShipPicker = false
                }
                .padding(vertical = 6.dp)
                .testTag("gate_inline_ship_option_${ship.id}"),
            contentAlignment = Alignment.Center,
          ) {
            Box(
              modifier =
                Modifier.clip(RoundedCornerShape(6.dp))
                  .background(
                    Brush.verticalGradient(
                      when {
                        isActiveHull -> listOf(Color(0xFF123847), Color(0xFF081B26))
                        isBestHull -> listOf(Color(0xFF2B230B), Color(0xFF141005))
                        else -> listOf(Color(0xFF112036), Color(0xFF08111F))
                      }
                    )
                  )
                  .border(
                    1.1.dp,
                    when {
                      isActiveHull -> Color(0xFF00F5D4)
                      isBestHull -> Color(0xFFFACC15)
                      else -> Color(0xFF334155)
                    },
                    RoundedCornerShape(6.dp),
                  )
                  .padding(horizontal = 8.5.dp, vertical = 4.5.dp),
              contentAlignment = Alignment.Center,
            ) {
              Text(
                text = if (isBestHull) "$cleanShipTitle (BEST)" else cleanShipTitle,
                color =
                  when {
                    isActiveHull -> Color(0xFF00F5D4)
                    isBestHull -> Color(0xFFFEF08A)
                    else -> Color(0xFFE2E8F0)
                  },
                fontSize = 8.2.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
              )
            }
          }
        }
      }
    }

    if (gateTab == ConvergenceGateTab.OFFICIAL_RANKED) {
      // Scrollable Mode List with SECTOR CONVERGENCE prominently at the top
      val orderedModes = remember {
        listOf(RankedGameMode.SECTOR_CONVERGENCE) +
          RankedGameMode.entries.filter { it != RankedGameMode.SECTOR_CONVERGENCE }
      }
      Column(
        modifier =
          Modifier.weight(1f)
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(7.dp),
      ) {
        orderedModes.forEach { mode ->
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
    } else {
      // Fixed 2-Column Custom Rift Gate: Left = 6-Char Private Join Code + Live Public Custom Game List, Right = 10-Rule Sandbox Cloner
      val activeCustom = remember(engine?.currentCustomGame, customRefreshTick) { engine?.currentCustomGame }
      val visibleCustomGames =
        remember(
          publicCustomGames,
          engine?.activeCustomGames?.size,
          customRefreshTick,
          unlockedPrivateCodes.size,
        ) {
          val byKey = linkedMapOf<String, CustomGameConfig>()
          for (cloudCfg in publicCustomGames) {
            byKey[cloudCfg.instanceId.uppercase(Locale.US)] = cloudCfg
          }
          for (localCfg in engine?.activeCustomGames.orEmpty()) {
            val k = localCfg.instanceId.uppercase(Locale.US)
            if (!byKey.containsKey(k)) {
              byKey[k] = localCfg
            }
          }
          byKey.values.filter { g ->
            val code = extractCustomRoomCode(g)
            !isPrivateCustomGame(g) ||
              activeCustom?.instanceId.equals(g.instanceId, ignoreCase = true) ||
              code in unlockedPrivateCodes
          }
        }
      val publicCount = visibleCustomGames.count { !isPrivateCustomGame(it) }

      Row(
        modifier = Modifier.weight(1f).fillMaxWidth().testTag("nexus_custom_games_section"),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        // Left Pane: Private Game Join Code + Public Custom Game List
        val customPaneShape = CutCornerShape(topStart = 12.dp, topEnd = 6.dp, bottomEnd = 12.dp, bottomStart = 6.dp)
        Column(
          modifier =
            Modifier.weight(0.46f)
              .fillMaxHeight()
              .clip(customPaneShape)
              .background(
                Brush.verticalGradient(listOf(Color(0xE60B182C), Color(0xE6060E1C), Color(0xF0040812)))
              )
              .border(
                1.15.dp,
                Brush.verticalGradient(listOf(Color(0x9900F5D4), Color(0x5538BDF8), Color(0x6600F5D4))),
                customPaneShape,
              )
              .padding(8.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Text(
            text = "PRIVATE GAME JOIN CODE (6-CHAR)",
            color = Color(0xFF00F5D4),
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
          )
          Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier =
                Modifier.weight(1f)
                  .height(38.dp)
                  .clip(RoundedCornerShape(7.dp))
                  .background(Brush.verticalGradient(listOf(Color(0xFF030812), Color(0xFF081224))))
                  .border(1.dp, Color(0xFF38BDF8), RoundedCornerShape(7.dp))
                  .padding(horizontal = 9.dp),
              contentAlignment = Alignment.CenterStart,
            ) {
              if (joinRoomCodeDraft.isEmpty()) {
                Text("ENTER 6-CHAR JOIN CODE...", color = Color(0xFF64748B), fontSize = 8.5.sp, fontFamily = FontFamily.Monospace)
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
              border = BorderStroke(1.1.dp, Color(0xFF00F5D4)),
              shape = CutCornerShape(topStart = 6.dp, topEnd = 3.dp, bottomEnd = 6.dp, bottomStart = 3.dp),
              contentPadding = PaddingValues(horizontal = 11.dp, vertical = 4.dp),
              modifier = Modifier.minimumInteractiveComponentSize().heightIn(min = 38.dp).testTag("nexus_btn_join_room_code"),
            ) {
              Text("JOIN CODE", fontSize = 8.5.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
            }
          }
          if (roomCodeBanner != null) {
            Text(roomCodeBanner, color = Color(0xFF34D399), fontSize = 7.8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
          }

          if (activeCustom != null && engine?.hasJoinedMatch == true) {
            val activeCode = extractCustomRoomCode(activeCustom)
            Row(
              modifier =
                Modifier.fillMaxWidth()
                  .clip(RoundedCornerShape(7.dp))
                  .background(Brush.horizontalGradient(listOf(Color(0xFF083226), Color(0xFF051C16))))
                  .border(1.dp, Color(0xFF10B981), RoundedCornerShape(7.dp))
                  .padding(horizontal = 7.dp, vertical = 4.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = "ACTIVE [$activeCode]: ${activeCustom.title}",
                color = Color(0xFF34D399),
                fontSize = 8.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
              )
              Box(
                modifier =
                  Modifier.minimumInteractiveComponentSize()
                    .clickable { onReturnToRankedFfa() }
                    .padding(vertical = 4.dp)
                    .testTag("nexus_btn_return_official_ranked"),
                contentAlignment = Alignment.Center,
              ) {
                Box(
                  modifier =
                    Modifier.clip(RoundedCornerShape(5.dp))
                      .background(Color(0xFF0F172A))
                      .border(1.dp, Color(0xFF34D399), RoundedCornerShape(5.dp))
                      .padding(horizontal = 7.dp, vertical = 4.dp),
                ) {
                  Text("RANKED FFA", color = Color(0xFF34D399), fontSize = 7.6.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
              }
            }
          }

          Text(
            text = "PUBLIC CUSTOM GAME LIST ($publicCount PUBLIC)",
            color = Color(0xFF94A3B8),
            fontSize = 8.2.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
          )
          LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth().testTag("public_custom_game_list"),
            verticalArrangement = Arrangement.spacedBy(5.dp),
          ) {
            if (visibleCustomGames.isEmpty()) {
              item {
                Text(
                  "No public custom games active right now. Create a Public Custom Game on the right or enter a 6-character Private Join Code above.",
                  color = Color(0xFF64748B),
                  fontSize = 8.4.sp,
                  fontFamily = FontFamily.Monospace,
                )
              }
            }
            items(visibleCustomGames, key = { it.instanceId }) { game ->
              val isJoined =
                engine?.hasJoinedMatch == true &&
                  activeCustom?.instanceId.equals(game.instanceId, ignoreCase = true)
              val cloudSectorId = "CUSTOM_${game.instanceId}"
              val cloudHumansInRoom =
                onlineHumanPilots.count { p -> p.sectorId.equals(cloudSectorId, ignoreCase = true) }
              val localCount = engine?.getHumanPilotCountForCustomGame(game.instanceId) ?: 0
              val count = maxOf(cloudHumansInRoom, localCount)
              val code = extractCustomRoomCode(game)
              val isPrivateEntry = isPrivateCustomGame(game)
              Row(
                modifier =
                  Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(7.dp))
                    .background(
                      Brush.verticalGradient(
                        if (isJoined) listOf(Color(0xE60F2D42), Color(0xE6071726))
                        else listOf(Color(0xCC0B1628), Color(0xCC050B16))
                      )
                    )
                    .border(
                      1.dp,
                      when {
                        isJoined -> Color(0xFF00F5D4)
                        isPrivateEntry -> Color(0xFFFACC15)
                        else -> Color(0xFF334155)
                      },
                      RoundedCornerShape(7.dp),
                    )
                    .padding(horizontal = 7.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Column(modifier = Modifier.weight(1f).padding(end = 6.dp)) {
                  val visBadge = if (isPrivateEntry) "[PRIVATE] " else "[PUBLIC] "
                  Text(
                    text = "$visBadge${game.title} • $count PILOTS",
                    color =
                      when {
                        isJoined -> Color(0xFF00F5D4)
                        isPrivateEntry -> Color(0xFFFACC15)
                        else -> Color.White
                      },
                    fontSize = 8.2.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                  val customScaleTag =
                    if (game.damageTakenScalePct != 100 || game.weaponFluxCostScalePct != 100)
                      " • DMG TAKEN ${game.damageTakenScalePct}% • FLUX COST ${game.weaponFluxCostScalePct}%"
                    else ""
                  val botInfo =
                    if (game.botsEnabled && game.botCount > 0) {
                      val skillLbl =
                        when (game.botSkillLevel) {
                          0 -> "CADET"
                          2 -> "VET"
                          3 -> "ACE"
                          else -> "REG"
                        }
                      "${game.botCount} ($skillLbl)"
                    } else "OFF"
                  Text(
                    text =
                      "${game.baseMode.title} • HOST: ${game.hostCallsign} • CODE: $code • BOTS: $botInfo • CAP: ${game.scoreCap}$customScaleTag",
                    color = Color(0xFF94A3B8),
                    fontSize = 7.4.sp,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                }
                Box(
                  modifier =
                    Modifier.minimumInteractiveComponentSize()
                      .clickable { onJoinCustomInstance(game.instanceId) }
                      .padding(vertical = 4.dp)
                      .testTag("nexus_join_custom_${game.instanceId}"),
                  contentAlignment = Alignment.Center,
                ) {
                  Box(
                    modifier =
                      Modifier.clip(RoundedCornerShape(6.dp))
                        .background(
                          Brush.verticalGradient(
                            if (isJoined) listOf(Color(0xFF065F46), Color(0xFF043F2E))
                            else listOf(Color(0xFF0E7490), Color(0xFF084C61))
                          )
                        )
                        .border(1.dp, Color(0xFF00F5D4), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.5.dp, vertical = 5.dp),
                  ) {
                    Text(
                      text = if (isJoined) "RE-ENTER >" else "JOIN >",
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
        }

        // Right Pane: 10-Rule Custom Rift Sandbox Cloner (Public or Private 6-Char Room Code)
        Column(
          modifier =
            Modifier.weight(0.55f)
              .fillMaxHeight()
              .clip(customPaneShape)
              .background(
                Brush.verticalGradient(listOf(Color(0xE60D172C), Color(0xE6060E1C), Color(0xF0040812)))
              )
              .border(
                1.15.dp,
                Brush.verticalGradient(listOf(Color(0x99C084FC), Color(0x55475569), Color(0x7700F5D4))),
                customPaneShape,
              )
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
                Modifier.minimumInteractiveComponentSize()
                  .clickable { onShareRoomToLfg() }
                  .padding(vertical = 4.dp)
                  .testTag("nexus_btn_share_room_lfg"),
              contentAlignment = Alignment.Center,
            ) {
              Box(
                modifier =
                  Modifier.clip(RoundedCornerShape(6.dp))
                    .background(Brush.verticalGradient(listOf(Color(0xFF132D42), Color(0xFF091826))))
                    .border(1.dp, Color(0xFF34D399), RoundedCornerShape(6.dp))
                    .padding(horizontal = 7.5.dp, vertical = 4.5.dp),
              ) {
                Text("SHARE TO #SQUADRON-LFG", color = Color(0xFF34D399), fontSize = 7.6.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
              }
            }
          }

          Column(
            modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(vertical = 2.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
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
              "FFA SQUADRONS:",
              listOf(true to "Squadrons ON", false to "Solo Only"),
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
              "BOT COUNT:",
              listOf(0 to "0 Bots", 4 to "4 Bots", 8 to "8 Bots", 12 to "12 Bots", 19 to "19 Bots"),
              if (customBotsEnabled) customBotCount else 0,
              onSelectCustomBotCount,
            )
            CustomRuleChipRow(
              "BOT SKILL:",
              listOf(0 to "Cadet", 1 to "Regular", 2 to "Veteran", 3 to "Ace"),
              customBotSkillLevel,
              onSelectCustomBotSkillLevel,
            )
            CustomRuleChipRow(
              "SCORE CAP:",
              listOf(25 to "25 Pts", 50 to "50 Pts", 100 to "100 Pts", 200 to "200 Pts"),
              customScoreCap,
              onSelectCustomScoreCap,
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
            CustomFivePercentScaleRow(
              label = "DMG TAKEN:",
              valuePct = customDamageTakenScalePct,
              minusTag = "custom_damage_taken_scale_minus",
              valueTag = "custom_damage_taken_scale_value",
              plusTag = "custom_damage_taken_scale_plus",
              onSelectPct = onSelectCustomDamageTakenScalePct,
            )
            CustomFivePercentScaleRow(
              label = "FLUX COST:",
              valuePct = customWeaponFluxCostScalePct,
              minusTag = "custom_weapon_flux_cost_minus",
              valueTag = "custom_weapon_flux_cost_value",
              plusTag = "custom_weapon_flux_cost_plus",
              onSelectPct = onSelectCustomWeaponFluxCostScalePct,
            )
          }

          Button(
            onClick = onLaunchCustomGame,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0E7490), contentColor = Color.White),
            border = BorderStroke(1.2.dp, Color(0xFF00F5D4)),
            shape = CutCornerShape(topStart = 8.dp, topEnd = 4.dp, bottomEnd = 8.dp, bottomStart = 4.dp),
            modifier =
              Modifier.fillMaxWidth()
                .minimumInteractiveComponentSize()
                .heightIn(min = 42.dp)
                .testTag("nexus_btn_launch_custom_game"),
          ) {
            Text(
              text =
                if (customIsPrivateRoom) "RIFT IN PRIVATE [$generatedRoomCode] >"
                else "RIFT IN PUBLIC CUSTOM >",
              fontSize = 8.8.sp,
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
      RankedGameMode.SECTOR_CONVERGENCE -> "20-Pilot Free-For-All or 1-5 Squadron • Rift Station Safe Zone • +100 Shards for Top Resonance."
      RankedGameMode.RIFT_SKIRMISH -> "Best-of-3 Team Rounds • Opposite Warp Rifts • Locks Active Hull on Entry."
      RankedGameMode.FLUX_CORE -> "20-Pilot Carrier Free-For-All • +1 Overcharge Tier / 5s • 30s Safe-Zone Eject."
      RankedGameMode.STASIS_BREACH -> "10v10 Stasis Jailbreak • No Safe Zone • Destroy 8,000 Flux Jail Core to free allies."
      RankedGameMode.RIFT_CONTROL -> "Dual-Rift Tug-of-War • No Safe Zone • Hold uncontested 30s to lock Rifts."
      RankedGameMode.SYNDICATE_WARS -> "[$syndicateTag] Syndicate Ladder • ${syndicateWarRecord.warTrophies} War Trophies (${syndicateWarRecord.warWins}W-${syndicateWarRecord.warLosses}L)."
    }
  val isLocked = mode == RankedGameMode.SYNDICATE_WARS && !canQueueSyndicateWars.first
  val isPrimaryTopMode = mode == RankedGameMode.SECTOR_CONVERGENCE
  val modeCardShape = CutCornerShape(topStart = 12.dp, topEnd = 6.dp, bottomEnd = 12.dp, bottomStart = 6.dp)

  Column(
    modifier =
      Modifier.fillMaxWidth()
        .heightIn(min = if (isPrimaryTopMode) 94.dp else 86.dp)
        .clip(modeCardShape)
        .background(
          Brush.verticalGradient(
            when {
              isCurrentActive -> listOf(Color(0xF0112A46), Color(0xEB081729), Color(0xF2040B16))
              isPrimaryTopMode -> listOf(Color(0xEB0E223C), Color(0xE8071324), Color(0xF0040A14))
              else -> listOf(Color(0xE30B182C), Color(0xE0060E1C), Color(0xEB030812))
            }
          )
        )
        .border(
          if (isCurrentActive || isPrimaryTopMode) 1.5.dp else 1.1.dp,
          Brush.horizontalGradient(
            if (isCurrentActive || isPrimaryTopMode) {
              listOf(accent, Color.White.copy(alpha = 0.55f), accent.copy(alpha = 0.85f))
            } else {
              listOf(accent.copy(alpha = 0.65f), Color(0xFF334155), accent.copy(alpha = 0.45f))
            }
          ),
          modeCardShape,
        )
        .drawBehind {
          drawLine(
            brush =
              Brush.horizontalGradient(
                listOf(Color.Transparent, accent.copy(alpha = 0.35f), Color.Transparent)
              ),
            start = Offset(16.dp.toPx(), 1.2.dp.toPx()),
            end = Offset(size.width - 16.dp.toPx(), 1.2.dp.toPx()),
            strokeWidth = 1.dp.toPx(),
          )
        }
        .clickable(enabled = !isLocked) { onSelectRiftMode() }
        .padding(horizontal = 12.dp, vertical = 8.dp)
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
          text = title,
          color = accent,
          fontSize = if (isPrimaryTopMode) 10.8.sp else 10.0.sp,
          fontWeight = FontWeight.Black,
          fontFamily = FontFamily.Monospace,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Box(
          modifier =
            Modifier.clip(RoundedCornerShape(4.dp))
              .background(accent.copy(alpha = 0.18f))
              .border(0.8.dp, accent.copy(alpha = 0.45f), RoundedCornerShape(4.dp))
              .padding(horizontal = 5.5.dp, vertical = 1.5.dp),
        ) {
          Text(mode.teamFormatBadge, color = accent, fontSize = 7.4.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        }
      }
      Box(
        modifier =
          Modifier.clip(RoundedCornerShape(5.dp))
            .background(Color(0xB3030812))
            .border(0.8.dp, if (humanCount > 0) Color(0x8834D399) else Color(0x55475569), RoundedCornerShape(5.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
      ) {
        Text(
          text = "$humanCount PILOTS",
          color = if (humanCount > 0) Color(0xFF34D399) else Color(0xFF94A3B8),
          fontSize = 8.0.sp,
          fontWeight = FontWeight.ExtraBold,
          fontFamily = FontFamily.Monospace,
        )
      }
    }

    Text(
      text = desc,
      color = Color(0xFFCBD5E1),
      fontSize = 8.6.sp,
      lineHeight = 11.8.sp,
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
              text = if (isCurrentActive) "ACTIVE RIFT" else "TAP TO RIFT IN",
              color = if (isCurrentActive) accent else Color(0xFF94A3B8),
              fontSize = 7.8.sp,
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
            containerColor = accent.copy(alpha = 0.28f),
            contentColor = Color.White,
            disabledContainerColor = Color(0x551E293B),
            disabledContentColor = Color(0xFF64748B),
          ),
        border = BorderStroke(1.1.dp, if (isLocked) Color(0xFF475569) else accent),
        shape = CutCornerShape(topStart = 7.dp, topEnd = 3.dp, bottomEnd = 7.dp, bottomStart = 3.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
        modifier =
          Modifier.minimumInteractiveComponentSize()
            .heightIn(min = 34.dp)
            .testTag("nexus_deploy_${mode.id}"),
      ) {
        Text(
          text =
            when {
              isLocked -> "SYNDICATE REQ"
              armadaSize >= 2 -> "RIFT IN ($armadaSize) >"
              else -> "RIFT IN >"
            },
          fontSize = 8.2.sp,
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
      Modifier.minimumInteractiveComponentSize()
        .clickable { onClick() }
        .padding(vertical = 4.dp),
    contentAlignment = Alignment.Center,
  ) {
    Box(
      modifier =
        Modifier.clip(RoundedCornerShape(5.dp))
          .background(
            Brush.verticalGradient(
              if (selected) listOf(accent.copy(alpha = 0.34f), Color(0xFF0A1628))
              else listOf(Color(0xFF132036), Color(0xFF091120))
            )
          )
          .border(1.dp, if (selected) accent else Color(0xFF334155), RoundedCornerShape(5.dp))
          .padding(horizontal = 7.dp, vertical = 3.5.dp),
    ) {
      Text(label, color = if (selected) Color.White else Color(0xFF94A3B8), fontSize = 7.6.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace)
    }
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
          Modifier.minimumInteractiveComponentSize()
            .clickable { onSelect(value) }
            .padding(vertical = 3.dp),
        contentAlignment = Alignment.Center,
      ) {
        Box(
          modifier =
            Modifier.clip(RoundedCornerShape(5.dp))
              .background(
                Brush.verticalGradient(
                  if (isSel) listOf(Color(0x4000F5D4), Color(0xFF081A28))
                  else listOf(Color(0xFF132036), Color(0xFF091120))
                )
              )
              .border(1.dp, if (isSel) Color(0xFF00F5D4) else Color(0xFF334155), RoundedCornerShape(5.dp))
              .padding(horizontal = 7.dp, vertical = 3.5.dp),
        ) {
          Text(
            text,
            color = if (isSel) Color.White else Color(0xFF94A3B8),
            fontSize = 7.5.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = FontFamily.Monospace,
          )
        }
      }
    }
  }
}

@Composable
private fun CustomFivePercentScaleRow(
  label: String,
  valuePct: Int,
  minusTag: String,
  valueTag: String,
  plusTag: String,
  onSelectPct: (Int) -> Unit,
) {
  val snapped = snapCustomFivePercentScale(valuePct)
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
    Box(
      modifier =
        Modifier.minimumInteractiveComponentSize()
          .heightIn(min = 48.dp)
          .clickable(enabled = snapped > 0) { onSelectPct(snapped - 5) }
          .padding(vertical = 3.dp)
          .testTag(minusTag),
      contentAlignment = Alignment.Center,
    ) {
      Box(
        modifier =
          Modifier.clip(RoundedCornerShape(5.dp))
            .background(Color(0xFF132036))
            .border(1.dp, if (snapped > 0) Color(0xFF00F5D4) else Color(0xFF334155), RoundedCornerShape(5.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
      ) {
        Text(
          "-5%",
          color = if (snapped > 0) Color(0xFF00F5D4) else Color(0xFF64748B),
          fontSize = 7.5.sp,
          fontWeight = FontWeight.Black,
          fontFamily = FontFamily.Monospace,
        )
      }
    }
    Box(
      modifier =
        Modifier.clip(RoundedCornerShape(5.dp))
          .background(Color(0xFF081A28))
          .border(1.dp, if (snapped == 100) Color(0xFF38BDF8) else Color(0xFFFACC15), RoundedCornerShape(5.dp))
          .padding(horizontal = 9.dp, vertical = 4.dp)
          .testTag(valueTag),
      contentAlignment = Alignment.Center,
    ) {
      Text(
        "$snapped%",
        color = if (snapped == 100) Color.White else Color(0xFFFFD700),
        fontSize = 7.8.sp,
        fontWeight = FontWeight.Black,
        fontFamily = FontFamily.Monospace,
      )
    }
    Box(
      modifier =
        Modifier.minimumInteractiveComponentSize()
          .heightIn(min = 48.dp)
          .clickable(enabled = snapped < 100) { onSelectPct(snapped + 5) }
          .padding(vertical = 3.dp)
          .testTag(plusTag),
      contentAlignment = Alignment.Center,
    ) {
      Box(
        modifier =
          Modifier.clip(RoundedCornerShape(5.dp))
            .background(Color(0xFF132036))
            .border(1.dp, if (snapped < 100) Color(0xFF00F5D4) else Color(0xFF334155), RoundedCornerShape(5.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
      ) {
        Text(
          "+5%",
          color = if (snapped < 100) Color(0xFF00F5D4) else Color(0xFF64748B),
          fontSize = 7.5.sp,
          fontWeight = FontWeight.Black,
          fontFamily = FontFamily.Monospace,
        )
      }
    }
    listOf(0, 50, 100).forEach { preset ->
      val isSel = snapped == preset
      Box(
        modifier =
          Modifier.minimumInteractiveComponentSize()
            .heightIn(min = 48.dp)
            .clickable { onSelectPct(preset) }
            .padding(vertical = 3.dp),
        contentAlignment = Alignment.Center,
      ) {
        Box(
          modifier =
            Modifier.clip(RoundedCornerShape(5.dp))
              .background(if (isSel) Color(0x4000F5D4) else Color(0xFF0F172A))
              .border(1.dp, if (isSel) Color(0xFF00F5D4) else Color(0xFF334155), RoundedCornerShape(5.dp))
              .padding(horizontal = 6.5.dp, vertical = 3.5.dp),
        ) {
          Text(
            if (preset == 100) "100% STD" else "$preset%",
            color = if (isSel) Color.White else Color(0xFF94A3B8),
            fontSize = 7.2.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = FontFamily.Monospace,
          )
        }
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
  callsign: String = "COMMANDER",
  cleanSyndicateTag: String,
  syndicateName: String,
  isLocalArchitect: Boolean,
  isInMatch: Boolean = false,
  activeRankedModeTitle: String = "Sector Convergence",
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
  onSendVoiceHeartbeat: (String, String) -> Unit = { _, _ -> },
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

  val commsPaneShape = CutCornerShape(topStart = 12.dp, topEnd = 6.dp, bottomEnd = 12.dp, bottomStart = 6.dp)
  Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    // 1. Left Channel Directory
    Column(
      modifier =
        Modifier.width(192.dp)
          .fillMaxHeight()
          .clip(commsPaneShape)
          .background(
            Brush.verticalGradient(listOf(Color(0xD90A1526), Color(0xD9050B16), Color(0xE6030710)))
          )
          .border(
            1.1.dp,
            Brush.verticalGradient(listOf(Color(0x6638BDF8), Color(0x33475569), Color(0x5500F5D4))),
            commsPaneShape,
          )
          .padding(7.dp)
          .verticalScroll(rememberScrollState())
          .testTag("nexus_channel_directory"),
      verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
      ChannelCategoryHeader("// RIFT INTEL")
      ChannelNavItem("RIFT AI", "AI", selectedChannelKey == "#nexus-ai", Color(0xFF00F5D4), { onSelectChannel("#nexus-ai") }, "nexus_ch_nexus_ai")
      ChannelNavItem("#announcements", "DEV", selectedChannelKey == "#announcements", Color(0xFFFACC15), { onSelectChannel("#announcements") }, "nexus_ch_announcements")
      ChannelNavItem("#patch-log", "v1.0.$CURRENT_RIFT_BUILD_VERSION", selectedChannelKey == "#patch-log", Color(0xFF38BDF8), { onSelectChannel("#patch-log") }, "nexus_ch_change_log")
      ChannelNavItem("#bug-reports", "CLOUD", selectedChannelKey == "#bug-reports", Color(0xFFFB7185), { onSelectChannel("#bug-reports") }, "nexus_ch_bug_report")
      ChannelNavItem("#rules", "FAIR", selectedChannelKey == "#rules", Color(0xFFA78BFA), { onSelectChannel("#rules") }, "nexus_ch_rules")

      Spacer(modifier = Modifier.height(3.dp))
      ChannelCategoryHeader("// COMMUNITY")
      ChannelNavItem("#sector-lounge", "$totalOnlinePilots", selectedChannelKey == "#sector-lounge", Color(0xFF38BDF8), { onSelectChannel("#sector-lounge") }, "nexus_ch_main_lobby")
      ChannelNavItem("#shipwright-meta", "100PT", selectedChannelKey == "#shipwright-meta", Color(0xFFC084FC), { onSelectChannel("#shipwright-meta") }, "nexus_ch_shipwright_meta")
      ChannelNavItem("#squadron-lfg", "LFG", selectedChannelKey == "#strike-lfg", Color(0xFF34D399), { onSelectChannel("#strike-lfg") }, "nexus_ch_strike_lfg")

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
                .minimumInteractiveComponentSize()
                .clickable { onToggleChannelManager() }
                .padding(vertical = 4.dp)
                .testTag("nexus_btn_manage_syndicate_channels"),
            contentAlignment = Alignment.Center,
          ) {
            Box(
              modifier =
                Modifier.fillMaxWidth()
                  .clip(RoundedCornerShape(5.dp))
                  .background(Brush.verticalGradient(listOf(Color(0xFF262010), Color(0xFF141108))))
                  .border(1.dp, Color(0x88F59E0B), RoundedCornerShape(5.dp))
                  .padding(vertical = 4.5.dp),
              contentAlignment = Alignment.Center,
            ) {
              Text("+ CH (${syndicateCustomChannels.size}/5)", color = Color(0xFFFBBF24), fontSize = 7.6.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
          }
          Box(
            modifier =
              Modifier.weight(1f)
                .minimumInteractiveComponentSize()
                .clickable { onToggleGuestManager() }
                .padding(vertical = 4.dp)
                .testTag("nexus_btn_syndicate_guest_pass"),
            contentAlignment = Alignment.Center,
          ) {
            Box(
              modifier =
                Modifier.fillMaxWidth()
                  .clip(RoundedCornerShape(5.dp))
                  .background(Brush.verticalGradient(listOf(Color(0xFF102438), Color(0xFF081320))))
                  .border(1.dp, Color(0x8838BDF8), RoundedCornerShape(5.dp))
                  .padding(vertical = 4.5.dp),
              contentAlignment = Alignment.Center,
            ) {
              Text("GUESTS (${syndicateInvitedGuests.size})", color = Color(0xFF38BDF8), fontSize = 7.6.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
          }
        }
      } else {
        ChannelNavItem("Syndicate Locked", "ENLIST", selectedChannelKey == "syndicate:locked", Color(0xFF64748B), { onSelectChannel("syndicate:locked") }, "nexus_ch_syndicate_locked")
      }

      Spacer(modifier = Modifier.height(3.dp))
      ChannelCategoryHeader("// SQUADRON")
      ChannelNavItem(
        label = "#squadron",
        badge = "${armadaSlots.size.coerceAtLeast(1)}/5",
        selected = selectedChannelKey.equals(strikeWingKey, ignoreCase = true),
        accent = Color(0xFF60A5FA),
        onClick = { onSelectChannel(strikeWingKey) },
        tag = "nexus_ch_armada_chat",
      )

      Spacer(modifier = Modifier.height(4.dp))
      ChannelCategoryHeader("// VOICE COMMS")
      SubspaceTeamVoiceChatDeck(
        callsign = callsign,
        activeArmadaName = activeArmadaName,
        isInMatch = isInMatch,
        activeRankedModeTitle = activeRankedModeTitle,
        strikeWingKey = strikeWingKey,
        onSendVoiceHeartbeat = onSendVoiceHeartbeat,
      )
    }

    // 2. Center Live Channel Feed & Composer
    Column(
      modifier =
        Modifier.weight(1f)
          .fillMaxHeight()
          .clip(commsPaneShape)
          .background(
            Brush.verticalGradient(listOf(Color(0xCC0A1526), Color(0xD9050B16), Color(0xE6030710)))
          )
          .border(
            1.1.dp,
            Brush.verticalGradient(listOf(Color(0x6600F5D4), Color(0x33475569), Color(0x5538BDF8))),
            commsPaneShape,
          )
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
            modifier =
              Modifier.fillMaxWidth()
                .heightIn(min = 44.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Brush.verticalGradient(listOf(Color(0xCC0D1E36), Color(0xCC07101F))))
                .border(1.dp, Color(0x5538BDF8), RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = "#rift-ai // TACTICAL CONSOLE",
              color = Color(0xFF00F5D4),
              fontSize = 9.5.sp,
              fontWeight = FontWeight.Black,
              fontFamily = FontFamily.Monospace,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.weight(1f).padding(end = 8.dp),
            )
            OnlineRosterToggleButton(onlineHumanPilots.size, showOnlineRosterPane, onToggleOnlineRosterPane)
          }

          Row(
            modifier =
              Modifier.fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            NEXUS_STARTER_CHIPS.forEachIndexed { idx, chipPrompt ->
              Box(
                modifier =
                  Modifier.minimumInteractiveComponentSize()
                    .clickable(enabled = !isStreaming) { onSendStarterQuestion(chipPrompt) }
                    .padding(vertical = 2.dp)
                    .testTag("nexus_quick_chip_$idx"),
                contentAlignment = Alignment.Center,
              ) {
                Box(
                  modifier =
                    Modifier.clip(RoundedCornerShape(10.dp))
                      .background(Brush.verticalGradient(listOf(Color(0xE6102846), Color(0xE6081529))))
                      .border(1.dp, Color(0x9938BDF8), RoundedCornerShape(10.dp))
                      .padding(horizontal = 11.dp, vertical = 5.dp),
                ) {
                  Text(
                    text = chipPrompt,
                    color = if (isStreaming) Color(0xFF94A3B8) else Color(0xFFF0F9FF),
                    fontSize = 8.8.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    softWrap = false,
                  )
                }
              }
            }
          }

          LazyColumn(
            state = listState,
            modifier =
              Modifier.weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0x66030712))
                .border(1.dp, Color(0x3838BDF8), RoundedCornerShape(8.dp))
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
                  .height(38.dp)
                  .clip(RoundedCornerShape(8.dp))
                  .background(Brush.verticalGradient(listOf(Color(0xFF061121), Color(0xFF0B192E))))
                  .border(1.dp, if (isStreaming) Color(0x77475569) else Color(0xFF00F5D4), RoundedCornerShape(8.dp))
                  .padding(horizontal = 10.dp)
                  .testTag("nexus_query_input_box"),
              contentAlignment = Alignment.CenterStart,
            ) {
              if (inputQuery.isEmpty()) {
                Text(stringResource(R.string.nexus_input_placeholder), color = Color(0xFFE2E8F0), fontSize = 9.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
                  containerColor = Color(0xFF0284C7),
                  contentColor = Color.White,
                  disabledContainerColor = Color(0xAA0F243C),
                  disabledContentColor = Color(0xFFE2E8F0),
                ),
              border = BorderStroke(1.2.dp, if (inputQuery.isNotBlank() && !isStreaming) Color(0xFF00F5D4) else Color(0x9938BDF8)),
              shape = CutCornerShape(topStart = 7.dp, topEnd = 3.dp, bottomEnd = 7.dp, bottomStart = 3.dp),
              contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
              modifier = Modifier.minimumInteractiveComponentSize().heightIn(min = 38.dp).testTag("nexus_send_button"),
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
                .background(Brush.verticalGradient(listOf(Color(0x990D1A30), Color(0xCC060C18))))
                .border(1.1.dp, Color(0x88F59E0B), RoundedCornerShape(10.dp))
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
              border = BorderStroke(1.1.dp, Color(0xFFF59E0B)),
              shape = CutCornerShape(topStart = 8.dp, topEnd = 4.dp, bottomEnd = 8.dp, bottomStart = 4.dp),
              modifier = Modifier.minimumInteractiveComponentSize().heightIn(min = 42.dp),
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
              isAnnouncements -> "#announcements // ARCHITECT & RIFT SECTOR DISPATCH"
              isShipwright -> "#shipwright-meta // 100-PT RIFT FRAME LOADOUT EXCHANGE"
              isLfg -> "#squadron-lfg // SQUADRON & PRIVATE RIFT RECRUITMENT"
              isSyndicateCh -> "[$cleanSyndicateTag] ${selectedChannelKey.substringAfterLast(':')} // $syndicateName"
              isStrikeWingCh -> "#squadron // [${activeArmadaName.ifBlank { "SQUADRON" }}] ENCRYPTED COMMS"
              else -> "#sector-lounge // LIVE CROSS-SECTOR PILOT FEED"
            }

          Row(
            modifier =
              Modifier.fillMaxWidth()
                .heightIn(min = 44.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Brush.verticalGradient(listOf(Color(0xCC0D1E36), Color(0xCC07101F))))
                .border(1.dp, Color(0x5538BDF8), RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp, vertical = 2.dp),
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
                ActionHeaderChip("POST SQUADRON INVITE", Color(0xFF34D399), "nexus_btn_post_wing_lfg", onPostStrikeWingInvite)
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
                .background(Color(0x66030712))
                .border(1.dp, Color(0x2838BDF8), RoundedCornerShape(8.dp))
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            if (isAnnouncements) {
              item(key = "pinned_architect_dispatch") {
                Column(
                  modifier =
                    Modifier.fillMaxWidth()
                      .clip(RoundedCornerShape(8.dp))
                      .background(Brush.verticalGradient(listOf(Color(0xD9241D0A), Color(0xD9120E05))))
                      .border(1.dp, Color(0xFFFACC15), RoundedCornerShape(8.dp))
                      .padding(8.dp),
                  verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                  Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    DevArchitectBadge()
                    Text("ARCHITECT // BUILD #$CURRENT_RIFT_BUILD_VERSION RIFT DISPATCH", color = Color(0xFFFACC15), fontSize = 9.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                  }
                  Text(
                    "Welcome to RIFT (Threshold Dynamics Studio, LLC). Use the Rift Hub Overwatch radar to inspect active matches and Rift In, or open any module deck around the aperture. All 10 ships operate at fair 100-Point Parity from 0 Resonance — share Rift Frame builds in #shipwright-meta, form Squadrons in #squadron-lfg, and post feedback in #bug-reports.",
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
                  color = Color(0xFFCBD5E1),
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
              TacticalQuickPingChip("FOCUS RESONANCE LEADER", Color(0xFFFB7185), "nexus_quick_ping_attack_hvt") {
                onSendToChannel(selectedChannelKey, "[TACTICAL PING] CONVERGE ON HIGH-RESONANCE TARGET NOW!")
              }
              TacticalQuickPingChip("FORM ON ME", Color(0xFF38BDF8), "nexus_quick_ping_form_on_me") {
                onSendToChannel(selectedChannelKey, "[TACTICAL PING] FORM UP ON MY SQUADRON!")
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
            val composerChannelTitle =
              when {
                isLfg -> "#squadron-lfg"
                isStrikeWingCh -> "#squadron"
                else -> selectedChannelKey
              }
            ChannelInputComposer(
              placeholder = "Transmit to $composerChannelTitle…",
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
          Modifier.width(188.dp)
            .fillMaxHeight()
            .clip(commsPaneShape)
            .background(
              Brush.verticalGradient(listOf(Color(0xD90A1826), Color(0xD9050C16), Color(0xE6030710)))
            )
            .border(
              1.1.dp,
              Brush.verticalGradient(listOf(Color(0x7710B981), Color(0x33475569), Color(0x5500F5D4))),
              commsPaneShape,
            )
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
                color = Color(0xFFCBD5E1),
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
                  .background(Brush.verticalGradient(listOf(Color(0xD90D1C32), Color(0xD907101E))))
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
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
              ) {
                Text(
                  text = "${pilot.sectorId} •",
                  color = Color(0xFF94A3B8),
                  fontSize = 8.0.sp,
                  fontFamily = FontFamily.Monospace,
                  maxLines = 1,
                )
                ResonanceCrestIcon(sizeDp = 10.dp)
                Text(
                  text = "$pilotRes",
                  color = Color(0xFFFFD700),
                  fontSize = 8.0.sp,
                  fontWeight = FontWeight.Black,
                  fontFamily = FontFamily.Monospace,
                  maxLines = 1,
                )
              }
              Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(
                  modifier =
                    Modifier.weight(1f)
                      .minimumInteractiveComponentSize()
                      .clickable { onAddOnlinePilotFriend(pilot.callsign) }
                      .padding(vertical = 3.dp),
                  contentAlignment = Alignment.Center,
                ) {
                  Box(
                    modifier =
                      Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .background(Brush.verticalGradient(listOf(Color(0xFF12466B), Color(0xFF09253B))))
                        .border(0.8.dp, Color(0x8838BDF8), RoundedCornerShape(4.dp))
                        .padding(vertical = 3.5.dp),
                    contentAlignment = Alignment.Center,
                  ) {
                    Text("+ FRIEND", color = Color(0xFF7DD3FC), fontSize = 7.2.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                  }
                }
                Box(
                  modifier =
                    Modifier.weight(1f)
                      .minimumInteractiveComponentSize()
                      .clickable { onInviteOnlinePilotToWing(pilot) }
                      .padding(vertical = 3.dp),
                  contentAlignment = Alignment.Center,
                ) {
                  Box(
                    modifier =
                      Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .background(Brush.verticalGradient(listOf(Color(0xFF085C46), Color(0xFF043326))))
                        .border(0.8.dp, Color(0x8834D399), RoundedCornerShape(4.dp))
                        .padding(vertical = 3.5.dp),
                    contentAlignment = Alignment.Center,
                  ) {
                    Text("+ SQUADRON", color = Color(0xFF6EE7B7), fontSize = 7.0.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
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
private fun OnlineRosterToggleButton(count: Int, expanded: Boolean, onToggle: () -> Unit) {
  Box(
    modifier =
      Modifier.minimumInteractiveComponentSize()
        .clickable { onToggle() }
        .padding(vertical = 4.dp)
        .testTag("nexus_btn_toggle_online_roster"),
    contentAlignment = Alignment.Center,
  ) {
    Box(
      modifier =
        Modifier.clip(RoundedCornerShape(6.dp))
          .background(
            Brush.verticalGradient(
              if (expanded) listOf(Color(0xFF0A382A), Color(0xFF051F17))
              else listOf(Color(0xFF122036), Color(0xFF091220))
            )
          )
          .border(1.dp, Color(0xFF10B981), RoundedCornerShape(6.dp))
          .padding(horizontal = 8.dp, vertical = 4.5.dp),
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
}

@Composable
private fun ActionHeaderChip(label: String, accent: Color, tag: String, onClick: () -> Unit) {
  Box(
    modifier =
      Modifier.minimumInteractiveComponentSize()
        .clickable { onClick() }
        .padding(vertical = 4.dp)
        .testTag(tag),
    contentAlignment = Alignment.Center,
  ) {
    Box(
      modifier =
        Modifier.clip(RoundedCornerShape(6.dp))
          .background(Brush.verticalGradient(listOf(accent.copy(alpha = 0.28f), Color(0xFF091426))))
          .border(1.dp, accent, RoundedCornerShape(6.dp))
          .padding(horizontal = 8.dp, vertical = 4.5.dp),
    ) {
      Text(label, color = Color.White, fontSize = 7.6.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
    }
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
        .background(
          Brush.verticalGradient(
            if (isDev) listOf(Color(0xD9241E0A), Color(0xD9120E05))
            else listOf(Color(0xB30E1C33), Color(0xB3070F1E))
          )
        )
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
        ActionHeaderChip("ANALYZE IN RIFT AI", Color(0xFF00F5D4), "btn_ai_build_${msg.id}") {
          onAskAiAboutBuild("Evaluate this shared 100-Pt Rift Frame loadout and suggest tactical matchups: ${msg.messageText}")
        }
      }
    } else if (roomCode != null) {
      ActionHeaderChip("ENTER PRIVATE RIFT [$roomCode]", Color(0xFFFACC15), "btn_join_room_${msg.id}") {
        onJoinCustomRoomCode(roomCode)
      }
    } else if (msg.isArmadaRecruitBanner && msg.armadaName.isNotBlank()) {
      ActionHeaderChip(
        "JOIN SQUADRON [${msg.armadaName}] (${msg.armadaCurrentSize}/${msg.armadaMaxSize})",
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
        .minimumInteractiveComponentSize()
        .heightIn(min = 38.dp)
        .clip(RoundedCornerShape(6.dp))
        .background(
          if (selected) {
            Brush.horizontalGradient(listOf(accent.copy(alpha = 0.28f), Color(0xCC091526)))
          } else {
            Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
          }
        )
        .border(1.dp, if (selected) accent else Color.Transparent, RoundedCornerShape(6.dp))
        .clickable { onClick() }
        .padding(horizontal = 7.dp, vertical = 5.dp)
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
        .background(Brush.verticalGradient(listOf(Color(0xF0101D34), Color(0xF0081020))))
        .border(1.dp, Color(0xFFF59E0B), RoundedCornerShape(8.dp))
        .padding(8.dp),
    verticalArrangement = Arrangement.spacedBy(5.dp),
  ) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
      Text("[$syndicateTag] CLOUD SYNDICATE CHANNELS (${channels.size}/5)", color = Color(0xFFFACC15), fontSize = 8.8.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
      Box(
        modifier = Modifier.minimumInteractiveComponentSize().clickable { onClose() }.padding(horizontal = 6.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center,
      ) {
        Text("CLOSE", color = Color(0xFFCBD5E1), fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
      }
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
            Box(
              modifier = Modifier.minimumInteractiveComponentSize().clickable { onDeleteChannel(ch) }.padding(horizontal = 4.dp),
              contentAlignment = Alignment.Center,
            ) {
              Text("X", color = Color(0xFFEF4444), fontSize = 8.2.sp, fontWeight = FontWeight.Black)
            }
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
        .background(Brush.verticalGradient(listOf(Color(0xF0101D34), Color(0xF0081020))))
        .border(1.dp, Color(0xFF38BDF8), RoundedCornerShape(8.dp))
        .padding(8.dp),
    verticalArrangement = Arrangement.spacedBy(5.dp),
  ) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
      Text("[$syndicateTag] SYNDICATE GUEST PASSES (${invitedGuests.size})", color = Color(0xFF38BDF8), fontSize = 8.8.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
      Box(
        modifier = Modifier.minimumInteractiveComponentSize().clickable { onClose() }.padding(horizontal = 6.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center,
      ) {
        Text("CLOSE", color = Color(0xFFCBD5E1), fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
      }
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
            Box(
              modifier = Modifier.minimumInteractiveComponentSize().clickable { onRevoke(guest) }.padding(horizontal = 4.dp),
              contentAlignment = Alignment.Center,
            ) {
              Text("X", color = Color(0xFFEF4444), fontSize = 8.2.sp, fontWeight = FontWeight.Black)
            }
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
      Modifier.minimumInteractiveComponentSize()
        .clickable { onClick() }
        .padding(vertical = 4.dp)
        .testTag(tag),
    contentAlignment = Alignment.Center,
  ) {
    Box(
      modifier =
        Modifier.clip(RoundedCornerShape(6.dp))
          .background(Brush.verticalGradient(listOf(accent.copy(alpha = 0.24f), Color(0xFF081222))))
          .border(1.dp, accent.copy(alpha = 0.78f), RoundedCornerShape(6.dp))
          .padding(horizontal = 8.5.dp, vertical = 4.5.dp),
    ) {
      Text(label, color = accent, fontSize = 8.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
    }
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
          .height(38.dp)
          .clip(RoundedCornerShape(8.dp))
          .background(Brush.verticalGradient(listOf(Color(0xFF030812), Color(0xFF081324))))
          .border(1.dp, Color(0xFF38BDF8), RoundedCornerShape(8.dp))
          .padding(horizontal = 10.dp),
      contentAlignment = Alignment.CenterStart,
    ) {
      if (value.isEmpty()) {
        Text(placeholder, color = Color(0xFFCBD5E1), fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
      colors =
        ButtonDefaults.buttonColors(
          containerColor = Color(0xFF0E7490),
          contentColor = Color.White,
          disabledContainerColor = Color(0x661E293B),
          disabledContentColor = Color(0xFFCBD5E1),
        ),
      border = BorderStroke(1.1.dp, if (value.isNotBlank()) Color(0xFF00F5D4) else Color(0x66475569)),
      shape = CutCornerShape(topStart = 7.dp, topEnd = 3.dp, bottomEnd = 7.dp, bottomStart = 3.dp),
      contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
      modifier = Modifier.minimumInteractiveComponentSize().heightIn(min = 38.dp),
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
      "#patch-log // RIFT v1.0.$CURRENT_RIFT_BUILD_VERSION (BUILD #$CURRENT_RIFT_BUILD_VERSION)",
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
      "#rules // OFFICIAL RIFT SECTOR DIRECTIVES & FAIR-PLAY CODE",
      color = Color(0xFFA78BFA),
      fontSize = 10.5.sp,
      fontWeight = FontWeight.Black,
      fontFamily = FontFamily.Monospace,
    )
    listOf(
      "1. HUMAN-ONLY TELEMETRY: Online pilot counters strictly count real human pilots — AI bots carry [BOT] and never inflate counts or leaderboards.",
      "2. 100% NON-PAY-TO-WIN PARITY: All 10 Hulls (including Rift Frame at full 100-Point Budget) are unlocked from 0 Resonance.",
      "3. SYNDICATE WARS ELIGIBILITY: Queuing for Ranked Syndicate Wars requires an enlisted real-player Syndicate and 100% same-Syndicate Squadron.",
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
            Modifier.minimumInteractiveComponentSize()
              .clickable { onSelectCategory(cat) }
              .padding(vertical = 4.dp),
          contentAlignment = Alignment.Center,
        ) {
          Box(
            modifier =
              Modifier.clip(RoundedCornerShape(6.dp))
                .background(
                  Brush.verticalGradient(
                    if (sel) listOf(Color(0xFF5C0A22), Color(0xFF2E0511))
                    else listOf(Color(0xFF132036), Color(0xFF091120))
                  )
                )
                .border(1.dp, if (sel) Color(0xFFFB7185) else Color(0xFF334155), RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp),
          ) {
            Text(cat, color = if (sel) Color.White else Color(0xFF94A3B8), fontSize = 7.6.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
          }
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
        .background(Brush.verticalGradient(listOf(Color(0xD9112440), Color(0xD9081326))))
        .border(1.dp, Color(0x8838BDF8), RoundedCornerShape(10.dp))
        .padding(10.dp),
    verticalArrangement = Arrangement.spacedBy(4.dp),
  ) {
    Text("RIFT AI TELEMETRY LINK ESTABLISHED", color = Color(0xFF00F5D4), fontSize = 10.5.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace)
    Text(
      "Ask RIFT AI anything — exact weapon dimensions, Newtonian drift & bounce raycasts, 100-point Rift Frame theorycrafting, Eclipse Stasis vs Disruptor EMP, Paired Phase Rifts, or Peak Resonance records.",
      color = Color(0xFFF8FAFC),
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
          .background(
            Brush.verticalGradient(
              if (isUser) listOf(Color(0xCC123357), Color(0xCC0A1D36))
              else listOf(Color(0xCC0D1B30), Color(0xCC07101E))
            )
          )
          .border(1.dp, if (isUser) Color(0x8838BDF8) else Color(0x8800F5D4), shape)
          .padding(horizontal = 10.dp, vertical = 7.dp),
      verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
          if (isUser) "PILOT QUERY" else "RIFT // TACTICAL AI",
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
        Text("Scanning RIFT sector physics & records…", color = Color(0xFF94A3B8), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
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
        .background(Brush.verticalGradient(listOf(Color(0xD9330D17), Color(0xD91A060B))))
        .border(1.dp, Color(0xFFFF3B4E), RoundedCornerShape(10.dp))
        .padding(horizontal = 10.dp, vertical = 7.dp)
        .testTag("nexus_error_banner"),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Column(modifier = Modifier.weight(1f)) {
      Text("RIFT AI LINK INTERRUPTED", color = Color(0xFFFF3B4E), fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace)
      Text(errorMessage, color = Color(0xFFFECDD3), fontSize = 9.5.sp)
    }
    if (canRetry) {
      Button(
        onClick = onRetry,
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4C0519), contentColor = Color(0xFFFECDD3)),
        border = BorderStroke(1.dp, Color(0xFFFF3B4E)),
        shape = RoundedCornerShape(7.dp),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
        modifier = Modifier.minimumInteractiveComponentSize().heightIn(min = 36.dp).testTag("nexus_retry_button"),
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

@Composable
private fun SubspaceTeamVoiceChatDeck(
  callsign: String,
  activeArmadaName: String,
  isInMatch: Boolean,
  activeRankedModeTitle: String,
  strikeWingKey: String,
  onSendVoiceHeartbeat: (String, String) -> Unit,
) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current
  var voiceScopeTeamMatch by remember { mutableStateOf(isInMatch) }
  var micActive by remember { mutableStateOf(!RiftCalibrationState.teamVoiceMuted && RiftCalibrationState.squadronVoiceEnabled) }
  var deafened by remember { mutableStateOf(false) }
  var pushToTalk by remember { mutableStateOf(RiftCalibrationState.teamVoicePushToTalk) }
  var micLevel by remember { mutableFloatStateOf(0f) }
  var permissionDeniedBanner by remember { mutableStateOf(false) }

  val targetVoiceKey =
    if (voiceScopeTeamMatch) {
      "voice:team:${activeRankedModeTitle.lowercase(Locale.US).replace(" ", "_")}"
    } else {
      strikeWingKey
    }

  val micPermissionLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
      if (granted) {
        permissionDeniedBanner = false
        deafened = false
        micActive = true
        RiftCalibrationState.teamVoiceMuted = false
        RiftCalibrationState.savePersistedLayout(context)
        onSendVoiceHeartbeat(targetVoiceKey, "[VOICE-STATE] $callsign joined voice comms (MIC LIVE)")
      } else {
        micActive = false
        RiftCalibrationState.teamVoiceMuted = true
        permissionDeniedBanner = true
      }
    }

  // Strictly halt microphone capture when app pauses/stops in background
  DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event ->
      if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
        micActive = false
        micLevel = 0f
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose {
      lifecycleOwner.lifecycle.removeObserver(observer)
      micActive = false
    }
  }

  LaunchedEffect(micActive, deafened) {
    if (!micActive || deafened || isHeadlessGlEnvironment()) {
      micLevel = 0f
      return@LaunchedEffect
    }
    val hasPerm =
      ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
        PackageManager.PERMISSION_GRANTED
    if (!hasPerm) {
      micActive = false
      micLevel = 0f
      return@LaunchedEffect
    }
    withContext(Dispatchers.IO) {
      val sampleRate = 16000
      val minBuf =
        AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
          )
          .coerceAtLeast(1024)
      var recorder: AudioRecord? = null
      try {
        recorder =
          AudioRecord(
            MediaRecorder.AudioSource.VOICE_COMMUNICATION,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            minBuf,
          )
        if (recorder.state == AudioRecord.STATE_INITIALIZED) {
          recorder.startRecording()
          val buf = ShortArray(512)
          while (isActive) {
            val read = recorder.read(buf, 0, buf.size)
            if (read > 0) {
              var sumSq = 0.0
              for (k in 0 until read) {
                val s = buf[k].toDouble() / 32768.0
                sumSq += s * s
              }
              val rms = sqrt(sumSq / read.toDouble()).toFloat()
              micLevel = (rms * 4.5f).coerceIn(0.05f, 1.0f)
            }
            delay(65L)
          }
        }
      } catch (_: SecurityException) {
        micLevel = 0f
      } catch (_: Throwable) {
        micLevel = 0f
      } finally {
        try {
          recorder?.stop()
        } catch (_: Throwable) {}
        try {
          recorder?.release()
        } catch (_: Throwable) {}
        micLevel = 0f
      }
    }
  }

  Column(
    modifier =
      Modifier.fillMaxWidth()
        .clip(RoundedCornerShape(7.dp))
        .background(Brush.verticalGradient(listOf(Color(0xFF091528), Color(0xFF030711))))
        .border(
          1.dp,
          if (micActive && !deafened) Color(0xFF00F5D4) else Color(0xFF334155),
          RoundedCornerShape(7.dp),
        )
        .padding(6.dp)
        .testTag("subspace_voice_chat_deck"),
    verticalArrangement = Arrangement.spacedBy(4.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(4.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Box(
        modifier =
          Modifier.weight(1f)
            .minimumInteractiveComponentSize()
            .clickable { voiceScopeTeamMatch = false }
            .padding(vertical = 3.dp)
            .testTag("subspace_voice_scope_squadron"),
        contentAlignment = Alignment.Center,
      ) {
        Box(
          modifier =
            Modifier.fillMaxWidth()
              .clip(RoundedCornerShape(5.dp))
              .background(if (!voiceScopeTeamMatch) Color(0x3800F5D4) else Color(0x6602050B))
              .border(
                1.dp,
                if (!voiceScopeTeamMatch) Color(0xFF00F5D4) else Color(0xFF334155),
                RoundedCornerShape(5.dp),
              )
              .padding(vertical = 4.dp),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = "SQUADRON",
            color = if (!voiceScopeTeamMatch) Color.White else Color(0xFF94A3B8),
            fontSize = 7.4.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
          )
        }
      }
      Box(
        modifier =
          Modifier.weight(1f)
            .minimumInteractiveComponentSize()
            .clickable { voiceScopeTeamMatch = true }
            .padding(vertical = 3.dp)
            .testTag("subspace_voice_scope_team"),
        contentAlignment = Alignment.Center,
      ) {
        Box(
          modifier =
            Modifier.fillMaxWidth()
              .clip(RoundedCornerShape(5.dp))
              .background(if (voiceScopeTeamMatch) Color(0x3838BDF8) else Color(0x6602050B))
              .border(
                1.dp,
                if (voiceScopeTeamMatch) Color(0xFF38BDF8) else Color(0xFF334155),
                RoundedCornerShape(5.dp),
              )
              .padding(vertical = 4.dp),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = "TEAM MATCH",
            color = if (voiceScopeTeamMatch) Color.White else Color(0xFF94A3B8),
            fontSize = 7.4.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
          )
        }
      }
    }

    Text(
      text =
        if (voiceScopeTeamMatch) "CH: ${activeRankedModeTitle.uppercase(Locale.US)}"
        else "CH: ${activeArmadaName.ifBlank { "SOLO SQUADRON" }.uppercase(Locale.US)}",
      color = Color(0xFF38BDF8),
      fontSize = 7.2.sp,
      fontWeight = FontWeight.Bold,
      fontFamily = FontFamily.Monospace,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )

    // Live Mic Input Meter Bar
    Box(
      modifier =
        Modifier.fillMaxWidth()
          .height(5.dp)
          .clip(RoundedCornerShape(3.dp))
          .background(Color(0xFF02050B))
          .border(0.6.dp, Color(0x4438BDF8), RoundedCornerShape(3.dp)),
    ) {
      val fillFrac = if (micActive && !deafened) micLevel.coerceIn(0.08f, 1f) else 0f
      if (fillFrac > 0f) {
        Box(
          modifier =
            Modifier.fillMaxWidth(fillFrac)
              .fillMaxHeight()
              .background(
                Brush.horizontalGradient(listOf(Color(0xFF00F5D4), Color(0xFFFACC15)))
              )
        )
      }
    }

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(4.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Box(
        modifier =
          Modifier.weight(1.1f)
            .minimumInteractiveComponentSize()
            .clickable {
              if (micActive) {
                micActive = false
                RiftCalibrationState.teamVoiceMuted = true
                RiftCalibrationState.savePersistedLayout(context)
                onSendVoiceHeartbeat(targetVoiceKey, "[VOICE-STATE] $callsign muted mic")
              } else {
                val granted =
                  ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                    PackageManager.PERMISSION_GRANTED
                if (granted) {
                  permissionDeniedBanner = false
                  deafened = false
                  micActive = true
                  RiftCalibrationState.teamVoiceMuted = false
                  RiftCalibrationState.savePersistedLayout(context)
                  onSendVoiceHeartbeat(
                    targetVoiceKey,
                    "[VOICE-STATE] $callsign joined voice comms (MIC LIVE)",
                  )
                } else {
                  micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
              }
            }
            .padding(vertical = 3.dp)
            .testTag("subspace_voice_mic_toggle"),
        contentAlignment = Alignment.Center,
      ) {
        Box(
          modifier =
            Modifier.fillMaxWidth()
              .clip(RoundedCornerShape(5.dp))
              .background(
                if (micActive && !deafened) Color(0xFF065F46) else Color(0xFF0B192E)
              )
              .border(
                1.dp,
                if (micActive && !deafened) Color(0xFF00F5D4) else Color(0xFF38BDF8),
                RoundedCornerShape(5.dp),
              )
              .padding(vertical = 5.dp),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = if (micActive && !deafened) "MIC LIVE" else "MIC MUTED",
            color = Color.White,
            fontSize = 7.2.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
          )
        }
      }

      Box(
        modifier =
          Modifier.weight(0.9f)
            .minimumInteractiveComponentSize()
            .clickable {
              val nextPtt = !pushToTalk
              pushToTalk = nextPtt
              RiftCalibrationState.teamVoicePushToTalk = nextPtt
              RiftCalibrationState.savePersistedLayout(context)
            }
            .padding(vertical = 3.dp)
            .testTag("subspace_voice_ptt_toggle"),
        contentAlignment = Alignment.Center,
      ) {
        Box(
          modifier =
            Modifier.fillMaxWidth()
              .clip(RoundedCornerShape(5.dp))
              .background(if (pushToTalk) Color(0x38FACC15) else Color(0xFF060D1A))
              .border(
                1.dp,
                if (pushToTalk) Color(0xFFFACC15) else Color(0xFF475569),
                RoundedCornerShape(5.dp),
              )
              .padding(vertical = 5.dp),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = if (pushToTalk) "PTT ON" else "OPEN MIC",
            color = if (pushToTalk) Color(0xFFFACC15) else Color(0xFFCBD5E1),
            fontSize = 6.9.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
          )
        }
      }

      Box(
        modifier =
          Modifier.weight(0.9f)
            .minimumInteractiveComponentSize()
            .clickable {
              deafened = !deafened
              if (deafened) {
                micActive = false
                RiftCalibrationState.teamVoiceMuted = true
                RiftCalibrationState.savePersistedLayout(context)
              }
            }
            .padding(vertical = 3.dp)
            .testTag("subspace_voice_deafen_toggle"),
        contentAlignment = Alignment.Center,
      ) {
        Box(
          modifier =
            Modifier.fillMaxWidth()
              .clip(RoundedCornerShape(5.dp))
              .background(if (deafened) Color(0xFF4C0519) else Color(0xFF060D1A))
              .border(
                1.dp,
                if (deafened) Color(0xFFFB7185) else Color(0xFF475569),
                RoundedCornerShape(5.dp),
              )
              .padding(vertical = 5.dp),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = if (deafened) "DEAFENED" else "DEAFEN",
            color = if (deafened) Color(0xFFFECDD3) else Color(0xFFCBD5E1),
            fontSize = 6.9.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
          )
        }
      }
    }

    if (permissionDeniedBanner) {
      Text(
        text = "MIC PERMISSION REQUIRED FOR LIVE VOICE",
        color = Color(0xFFFB7185),
        fontSize = 6.8.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace,
      )
    }
  }
}

