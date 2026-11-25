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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
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
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
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

private enum class RiftHubSideBayMode {
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

private enum class SquadronPilotFilter {
  ALL,
  SYNDICATE,
  FRIENDS,
}

private enum class SquadronPilotSort {
  RESONANCE,
  ACTIVITY,
}

private data class SquadronOnlinePilotItem(
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

private sealed interface OverwatchCircularPrompt {
  data object None : OverwatchCircularPrompt
  data class ShipInspect(val shipId: Int) : OverwatchCircularPrompt
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
    val primaryActionLabel: String = "",
    val onPrimaryAction: (() -> Unit)? = null,
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

private fun formatOverwatchResonance(value: Int): String =
  String.format(Locale.US, "%,d", value.coerceAtLeast(0))

private fun formatOverwatchTimerMmSs(totalSeconds: Int): String {
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
private fun Spinning25dShipWithOrbitingFlux(
  shipId: Int,
  shipSpinDeg: Float,
  fluxOrbitRad: Float,
  isCharged: Boolean,
  isBestShip: Boolean = false,
  isSelected: Boolean = false,
  sizeDp: Dp = 54.dp,
  orbitingFluxCount: Int = 5,
  modifier: Modifier = Modifier,
) {
  val clampedId = shipId.coerceIn(1, 10)
  val spriteRev = RiftSpriteBaker.spriteVisualRevision
  val customRev = RiftSpriteBaker.customShip9Version
  val trimStyle = RiftSpriteBaker.activeHullTrimStyle
  val visualRev = RiftVisualForgeState.visualRevision
  val shipBmp =
    remember(clampedId, spriteRev, customRev, trimStyle) {
      RiftSpriteBaker.getShipSprite(shipId = clampedId, isAllied = true, isHitFlashing = false).asImageBitmap()
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
    val hullFraction = if (isCharged) 0.66f else 0.72f
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

    // 3. Orbiting 3D Flux Icons Ringing the Charged Ship
    if (isCharged && orbitingFluxCount > 0) {
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
  cloudLeaderboardEntries: List<LeaderboardEntry> = emptyList(),
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
  var customBotCount by remember { mutableIntStateOf(engine?.currentCustomGame?.botCount ?: 12) }
  var customBotSkillLevel by remember { mutableIntStateOf(engine?.currentCustomGame?.botSkillLevel ?: 1) }
  var customScoreCap by remember { mutableIntStateOf(engine?.currentCustomGame?.scoreCap ?: 50) }
  var customBulletSizeMult by remember { mutableFloatStateOf(1.0f) }
  var customSpeedMult by remember { mutableFloatStateOf(1.0f) }
  var customRechargeMult by remember { mutableFloatStateOf(1.0f) }
  var customDamageMult by remember { mutableFloatStateOf(1.0f) }
  var customCrateSpawnMult by remember { mutableFloatStateOf(1.0f) }
  var customDamageTakenScalePct by remember {
    mutableIntStateOf(snapCustomFivePercentScale(engine?.currentCustomGame?.damageTakenScalePct ?: 100))
  }
  var customWeaponFluxCostScalePct by remember {
    mutableIntStateOf(snapCustomFivePercentScale(engine?.currentCustomGame?.weaponFluxCostScalePct ?: 100))
  }
  var customGamesRefreshTick by remember { mutableIntStateOf(0) }
  var showHubSurvivalSpreadsheetModal by remember { mutableStateOf(false) }

  fun updateCustomDamageTakenScale(rawPct: Int) {
    val snapped = snapCustomFivePercentScale(rawPct)
    customDamageTakenScalePct = snapped
    engine?.currentCustomGame?.let { activeCfg ->
      activeCfg.damageTakenScalePct = snapped
    }
  }

  fun updateCustomWeaponFluxCostScale(rawPct: Int) {
    val snapped = snapCustomFivePercentScale(rawPct)
    customWeaponFluxCostScalePct = snapped
    engine?.currentCustomGame?.let { activeCfg ->
      activeCfg.weaponFluxCostScalePct = snapped
    }
  }

  LaunchedEffect(engine?.currentCustomGame?.instanceId) {
    engine?.currentCustomGame?.let { activeCfg ->
      customDamageTakenScalePct = snapCustomFivePercentScale(activeCfg.damageTakenScalePct)
      customWeaponFluxCostScalePct = snapCustomFivePercentScale(activeCfg.weaponFluxCostScalePct)
      customBotCount = activeCfg.botCount.coerceIn(0, 19)
      customBotSkillLevel = activeCfg.botSkillLevel.coerceIn(0, 3)
      customScoreCap = activeCfg.scoreCap.coerceIn(5, 500)
    }
  }

  var showChannelManager by remember { mutableStateOf(false) }
  var newChannelDraft by remember { mutableStateOf("") }
  var showGuestManager by remember { mutableStateOf(false) }
  var guestCallsignDraft by remember { mutableStateOf("") }

  var bugCategory by remember { mutableStateOf("PHYSICS / COLLISION") }
  var bugDescription by remember { mutableStateOf("") }
  var bugStatusBanner by remember { mutableStateOf<String?>(null) }

  var focusedOrbitalIndex by remember { mutableIntStateOf(0) }
  var sideBayMode by remember { mutableStateOf(parseSideBayModeKey(requestedSideBayKey)) }
  var overwatchPrompt by remember { mutableStateOf<OverwatchCircularPrompt>(OverwatchCircularPrompt.None) }
  var squadronFilter by remember { mutableStateOf(SquadronPilotFilter.ALL) }
  var squadronSort by remember { mutableStateOf(SquadronPilotSort.RESONANCE) }
  var selectedSquadronPilotUid by remember { mutableStateOf("") }
  var squadronStatusBanner by remember { mutableStateOf<String?>(null) }

  LaunchedEffect(requestedSideBayNonce, requestedSideBayKey) {
    if (requestedSideBayNonce > 0) {
      overwatchPrompt = OverwatchCircularPrompt.None
      sideBayMode = parseSideBayModeKey(requestedSideBayKey)
    }
  }

  val localPrefs = remember(context) { LocalPilotPrefs(context) }
  val chargedPrefs =
    remember(context) {
      context.getSharedPreferences("rift_hub_charged_ship_prefs", android.content.Context.MODE_PRIVATE)
    }
  val chargedPrefsKey =
    remember(currentUserId) {
      "charged_ship_${currentUserId.trim().ifBlank { "local" }}"
    }
  var chargedShipId by
    remember(chargedPrefsKey) {
      mutableIntStateOf(chargedPrefs.getInt(chargedPrefsKey, 0).let { if (it in 1..10) it else 0 })
    }

  // Strictly locks/charges the ship across all game modes WITHOUT launching or auto-Rifting into Overwatch
  fun chargeShipAcrossAllModes(targetShipId: Int) {
    val clamped = targetShipId.coerceIn(1, 10)
    chargedShipId = clamped
    chargedPrefs.edit().putInt(chargedPrefsKey, clamped).apply()
    localPrefs.setPreferredShipId(clamped)
    onOpenHangarForShip(clamped)
    engine?.let { eng ->
      if (eng.hasJoinedMatch && !eng.isHubOverwatchActive) {
        eng.isShipLockedForMatch = false
        eng.switchLocalPlayerShip(clamped, forceFromHangar = true)
      }
    }
  }

  // Keep any persisted Charged ship locked across all game modes on mount / user switch (never auto-launching)
  LaunchedEffect(chargedShipId, currentUserId) {
    if (chargedShipId in 1..10 && activeShipId.coerceIn(1, 10) != chargedShipId) {
      localPrefs.setPreferredShipId(chargedShipId)
      onOpenHangarForShip(chargedShipId)
    }
  }

  // Reset Overwatch circular prompt when leaving ORBITAL_HUB
  LaunchedEffect(activeNode) {
    if (activeNode != NexusCoreNode.ORBITAL_HUB) {
      overwatchPrompt = OverwatchCircularPrompt.None
    }
  }

  // Hardware/System Back cleanly dismisses HTML spreadsheet first, circular Overwatch prompt second, then dissolves side-bay back to Rift Hub
  BackHandler(
    enabled =
      showHubSurvivalSpreadsheetModal ||
        (activeNode == NexusCoreNode.ORBITAL_HUB &&
          (overwatchPrompt != OverwatchCircularPrompt.None || sideBayMode != RiftHubSideBayMode.HUB_MAIN))
  ) {
    when {
      showHubSurvivalSpreadsheetModal -> showHubSurvivalSpreadsheetModal = false
      overwatchPrompt != OverwatchCircularPrompt.None -> overwatchPrompt = OverwatchCircularPrompt.None
      else -> sideBayMode = RiftHubSideBayMode.HUB_MAIN
    }
  }

  var showWarpOffConfirmModal by remember { mutableStateOf(false) }
  var showSwitchAccountConfirmModal by remember { mutableStateOf(false) }
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

  val resolvedBestShipForGate = bestShipId.coerceIn(1, 10)
  val clampedActiveShipForGate = activeShipId.coerceIn(1, 10)
  val isGateShipLocked =
    chargedShipId in 1..10 ||
      activeShipSummary.startsWith("LOCKED|", ignoreCase = true) ||
      engine?.isShipLockedForMatch == true ||
      clampedActiveShipForGate != resolvedBestShipForGate
  val gateDeployShipId =
    when {
      chargedShipId in 1..10 -> chargedShipId
      isGateShipLocked -> clampedActiveShipForGate
      else -> resolvedBestShipForGate
    }
  val gateDeployShipName =
    remember(gateDeployShipId, RiftSpriteBaker.customShip9Version) {
      RiftCodex.getShipById(gateDeployShipId)
        .name
        .replace(Regex("^[0-9]+-"), "")
        .trim()
        .uppercase(Locale.US)
    }
  val gateShipStatusSummary =
    "$gateDeployShipName (${
      when {
        chargedShipId in 1..10 -> "CHARGED"
        isGateShipLocked -> "LOCKED"
        else -> "BEST"
      }
    })"

  fun deployOrStageRankedSelection(mode: RankedGameMode, bracket: Int) {
    if (chargedShipId in 1..10) {
      onOpenHangarForShip(chargedShipId)
    }
    if (onStageRankedModeViaHangar != null) {
      onStageRankedModeViaHangar(mode, bracket)
      return
    }
    onDeployToRankedMode(mode, bracket)
  }

  fun deployOrStageExistingCustom(instanceId: String) {
    if (chargedShipId in 1..10) {
      onOpenHangarForShip(chargedShipId)
    }
    if (onStageExistingCustomInstanceViaHangar != null) {
      onStageExistingCustomInstanceViaHangar(instanceId)
      return
    }
    engine?.let { eng ->
      eng.isShipLockedForMatch = false
      eng.switchLocalPlayerShip(gateDeployShipId, forceFromHangar = true)
      eng.joinCustomGame(instanceId)
      RiftCalibrationState.applyCalibrationChangesLive(eng, context)
    }
    customGamesRefreshTick++
    onCloseModal()
  }

  fun deployOrStageNewCustomConfig(cfg: CustomGameConfig) {
    if (chargedShipId in 1..10) {
      onOpenHangarForShip(chargedShipId)
    }
    if (onStageCustomGameViaHangar != null) {
      onStageCustomGameViaHangar(cfg)
      return
    }
    engine?.let { eng ->
      eng.isShipLockedForMatch = false
      eng.switchLocalPlayerShip(gateDeployShipId, forceFromHangar = true)
      eng.createAndJoinCustomGame(cfg)
      RiftCalibrationState.botsEnabled = cfg.botsEnabled
      RiftCalibrationState.applyCalibrationChangesLive(eng, context)
    }
    customGamesRefreshTick++
    onCloseModal()
  }

  fun joinCustomByRoomCode(rawCode: String) {
    val cleanCode = rawCode.trim().uppercase(Locale.US).replace(Regex("[^A-Z0-9]"), "").take(8)
    if (cleanCode.length < 4) {
      roomCodeBanner = "ENTER A VALID 6-CHARACTER JOIN CODE."
      return
    }
    if (cleanCode !in unlockedPrivateCodes) unlockedPrivateCodes.add(cleanCode)
    val combinedKnown =
      buildList {
        addAll(publicCustomGames)
        addAll(engine?.activeCustomGames.orEmpty())
      }
    val matching =
      combinedKnown.firstOrNull { candidate ->
        extractCustomRoomCode(candidate).equals(cleanCode, ignoreCase = true) ||
          candidate.instanceId.equals("priv_$cleanCode", ignoreCase = true) ||
          candidate.instanceId.equals("pub_$cleanCode", ignoreCase = true)
      }
    if (matching != null) {
      engine?.let { eng ->
        if (eng.activeCustomGames.none { it.instanceId.equals(matching.instanceId, ignoreCase = true) }) {
          eng.activeCustomGames.add(0, matching)
        }
      }
      roomCodeBanner = "JOINING ROOM [$cleanCode] // ${matching.title}"
      customGamesRefreshTick++
      deployOrStageExistingCustom(matching.instanceId)
      return
    }
    if (onLookupCustomRoomByCode != null) {
      roomCodeBanner = "SEARCHING SECTOR FOR JOIN CODE [$cleanCode]..."
      onLookupCustomRoomByCode(cleanCode) { cloudMatch, statusMsg ->
        if (cloudMatch != null) {
          val resolvedCode = extractCustomRoomCode(cloudMatch)
          if (resolvedCode !in unlockedPrivateCodes) unlockedPrivateCodes.add(resolvedCode)
          engine?.let { eng ->
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
          deployOrStageExistingCustom(cloudMatch.instanceId)
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
          damageTakenScalePct = customDamageTakenScalePct,
          weaponFluxCostScalePct = customWeaponFluxCostScalePct,
          botCount = if (customBotsEnabled) customBotCount else 0,
          botSkillLevel = customBotSkillLevel,
          scoreCap = customScoreCap,
        )
      roomCodeBanner = "ENTERING PRIVATE RIFT [$cleanCode]"
      deployOrStageNewCustomConfig(created)
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
  var modalRootOrigin by remember { mutableStateOf(Offset.Zero) }

  val resolvedOverwatchInstances =
    remember(overwatchInstances, callsign, engine?.currentCustomGame, engine?.hasJoinedMatch) {
      val allowedModes = setOf(RankedGameMode.SECTOR_CONVERGENCE, RankedGameMode.FLUX_CORE)
      val filtered =
        overwatchInstances.filter { inst ->
          val cfg = inst.customConfig
          if (cfg == null) {
            inst.rankedMode in allowedModes
          } else {
            val isOwnCustom =
              cfg.hostCallsign.equals(callsign, ignoreCase = true) ||
                (engine?.hasJoinedMatch == true &&
                  engine.currentCustomGame?.instanceId?.equals(cfg.instanceId, ignoreCase = true) == true)
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
    remember(resolvedOverwatchInstances, selectedOverwatchKey, favoriteOverwatchKey) {
      resolvedOverwatchInstances.firstOrNull { it.key == selectedOverwatchKey }
        ?: favoriteOverwatchKey
          .takeIf { it.isNotBlank() }
          ?.let { fav -> resolvedOverwatchInstances.firstOrNull { it.key == fav } }
        ?: resolvedOverwatchInstances.maxByOrNull { it.combinedResonance }
        ?: resolvedOverwatchInstances.first()
    }

  LaunchedEffect(activeNode, focusedOrbitalIndex, glViewRef, liveTrafficShipIds) {
    val view = glViewRef ?: return@LaunchedEffect
    view.updateOnlinePilotShips(liveTrafficShipIds)
    view.dimFactor = 0.44f
    view.focusedTearIndex = -1
  }

  var lastCardNodeForSlide by remember { mutableStateOf(NexusCoreNode.HANGAR_DECK) }
  if (activeNode != NexusCoreNode.ORBITAL_HUB) {
    lastCardNodeForSlide = activeNode
  }
  val enterDirectionSign = if (isLeftSideHubCard(lastCardNodeForSlide)) -1f else 1f
  val terminalScale by
    animateFloatAsState(
      targetValue = if (activeNode == NexusCoreNode.ORBITAL_HUB) 0.95f else 1.0f,
      animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
      label = "terminal_emerge_scale",
    )
  val terminalAlpha by
    animateFloatAsState(
      targetValue = if (activeNode == NexusCoreNode.ORBITAL_HUB) 0.0f else 1.0f,
      animationSpec = tween(durationMillis = 210, easing = FastOutSlowInEasing),
      label = "terminal_emerge_alpha",
    )
  val terminalSlideFraction by
    animateFloatAsState(
      targetValue = if (activeNode == NexusCoreNode.ORBITAL_HUB) 0.08f * enterDirectionSign else 0.0f,
      animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
      label = "terminal_slide_fraction",
    )
  val hubStageAlpha by
    animateFloatAsState(
      targetValue = if (activeNode == NexusCoreNode.ORBITAL_HUB) 1.0f else 0.0f,
      animationSpec = tween(durationMillis = 210, easing = FastOutSlowInEasing),
      label = "hub_stage_alpha",
    )

  // Full-bleed container BEFORE safeDrawing:
  // On ORBITAL_HUB, renders the edge-to-edge abyssal slate (#02050B / #030711 / #060D1A) & gunmetal (#091528 / #0B192E)
  // sci-fi hardware terminal across 100% of the physical viewport outside the circular Overwatch aperture (zero outer margin frame).
  Box(
    modifier =
      Modifier.fillMaxSize()
        .onGloballyPositioned { coords ->
          modalRootOrigin = coords.positionInRoot()
        }
        .drawBehind {
          when {
            isCalibrationPeekActive -> drawRect(Color(0x1802050B))
            activeNode == NexusCoreNode.ORBITAL_HUB -> {
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

              // 1. Edge-to-edge deep abyssal slate chassis base (#02050B -> #030711 -> #060D1A)
              drawRect(
                brush =
                  Brush.verticalGradient(
                    colors = listOf(Color(0xFF060D1A), Color(0xFF030711), Color(0xFF02050B))
                  ),
                size = size,
              )

              // 2. Gunmetal/cyan Overwatch-anchored radial illumination (#0B192E -> #091528 -> #060D1A -> #02050B)
              drawRect(
                brush =
                  Brush.radialGradient(
                    colors =
                      listOf(
                        Color(0xFF0B192E),
                        Color(0xFF091528),
                        Color(0xFF060D1A),
                        Color(0xFF02050B),
                      ),
                    center = Offset(localCx, localCy),
                    radius = maxOf(w, h) * 0.64f,
                  ),
                size = size,
              )

              // 3. Calibrated polar astrogation reticle lines radiating from Overwatch collar to viewport edges
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

              // 4. Concentric beveled gunmetal/cyan bulkhead rings anchoring the full-bleed hardware stage
              val ring1 = cutR * 1.10f
              val ring2 = cutR * 1.28f
              val ring3 = cutR * 1.52f
              drawCircle(
                brush =
                  Brush.verticalGradient(
                    listOf(Color(0x4500F5D4), Color(0x2438BDF8), Color(0x32FACC15)),
                    startY = localCy - ring1,
                    endY = localCy + ring1,
                  ),
                radius = ring1,
                center = Offset(localCx, localCy),
                style = Stroke(width = 1.25.dp.toPx()),
              )
              drawCircle(
                brush =
                  Brush.verticalGradient(
                    listOf(Color(0x28FFD700), Color(0x1838BDF8), Color(0x2600F5D4)),
                    startY = localCy - ring2,
                    endY = localCy + ring2,
                  ),
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

              // 5. Four dual-walled power-bus conduits coupling the 4 flanking hardware bays to the Overwatch bezel
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
                drawLine(
                  color = Color(0xC002050B),
                  start = p0,
                  end = p1,
                  strokeWidth = 4.0.dp.toPx(),
                  cap = StrokeCap.Round,
                )
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
            else -> drawRect(Color(0xFF02050B))
          }
        }
        .onPreviewKeyEvent { keyEvent ->
          if (activeNode != NexusCoreNode.ORBITAL_HUB) return@onPreviewKeyEvent false
          val native = keyEvent.nativeKeyEvent
          if (native.action == KeyEvent.ACTION_DOWN && native.repeatCount == 0) {
            val totalTargets = 5
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
                when (focusedOrbitalIndex.coerceIn(0, 4)) {
                  0 -> onQuickZoneIntoOverwatch(activeOverwatchInstance)
                  1 -> {
                    overwatchPrompt = OverwatchCircularPrompt.None
                    sideBayMode = RiftHubSideBayMode.HANGAR_BAY
                  }
                  2 -> {
                    overwatchPrompt = OverwatchCircularPrompt.None
                    sideBayMode = RiftHubSideBayMode.RIFT_FRAME_BAY
                  }
                  3 -> onSelectNode(NexusCoreNode.SUBSPACE_COMMS)
                  else -> {
                    overwatchPrompt = OverwatchCircularPrompt.None
                    sideBayMode = RiftHubSideBayMode.GAME_MODES_BAY
                  }
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
        isFullScreenForgeStage && (renderModuleForNode != null || embeddedModuleContent != null) -> {
          Box(modifier = Modifier.fillMaxSize().testTag("nexus_embedded_module_container")) {
            if (renderModuleForNode != null) {
              renderModuleForNode(activeNode)
            } else {
              embeddedModuleContent?.invoke()
            }
          }
        }
        activeNode == NexusCoreNode.ORBITAL_HUB -> {
          Box(
            modifier =
              Modifier.fillMaxSize().graphicsLayer {
                alpha = hubStageAlpha
              }
          ) {
            OrbitalHubSpatialStage(
              callsign = callsign,
              syndicateTag = cleanSyndicateTag,
              syndicateName = syndicateName,
              liveResonance = resolvedLiveResonance,
              riftShards = riftShards,
              activeShipId = gateDeployShipId,
              bestShipId = bestShipId,
              chargedShipId = chargedShipId,
              onChargeShip = { shipToCharge -> chargeShipAcrossAllModes(shipToCharge) },
              activeShipSummary = activeShipSummary,
              armadaCount = armadaSlots.size.coerceAtLeast(1),
              activeArmadaName = activeArmadaName,
              armadaSlots = armadaSlots,
              onlineHumanPilots = onlineHumanPilots,
              onResolvePilotBestShipId = onResolvePilotBestShipId,
              onInviteOnlinePilotToWing = { targetPilot ->
                onInviteOnlinePilotToWing(targetPilot)
                squadronStatusBanner = "SQUADRON INVITE SENT TO ${targetPilot.callsign.uppercase(Locale.US)}"
              },
              onAddOnlinePilotFriend = { targetCallsign ->
                onAddOnlinePilotFriend(targetCallsign)
                squadronStatusBanner = "FRIEND REQUEST DISPATCHED TO ${targetCallsign.uppercase(Locale.US)}"
              },
              onPostStrikeWingInvite = {
                onPostStrikeWingInvite()
                squadronStatusBanner = "BROADCASTED SQUADRON INVITE TO #SQUADRON-LFG"
              },
              squadronStatusBanner = squadronStatusBanner,
              sideBayMode = sideBayMode,
              onChangeSideBayMode = { nextBay ->
                overwatchPrompt = OverwatchCircularPrompt.None
                sideBayMode = nextBay
              },
              overwatchPrompt = overwatchPrompt,
              onChangeOverwatchPrompt = { nextPrompt -> overwatchPrompt = nextPrompt },
              squadronFilter = squadronFilter,
              onSelectSquadronFilter = { squadronFilter = it },
              squadronSort = squadronSort,
              onToggleSquadronSort = {
                squadronSort =
                  if (squadronSort == SquadronPilotSort.RESONANCE) SquadronPilotSort.ACTIVITY
                  else SquadronPilotSort.RESONANCE
              },
              selectedSquadronPilotUid = selectedSquadronPilotUid,
              onSelectSquadronPilotUid = { selectedSquadronPilotUid = it },
              activeRankedMode = activeRankedMode,
              skirmishBracket = skirmishBracket,
              onSelectSkirmishBracket = { skirmishBracket = it.coerceIn(2, 5) },
              riftControlBracket = riftControlBracket,
              onSelectRiftControlBracket = { riftControlBracket = it },
              syndicateWarBracket = syndicateWarBracket,
              onSelectSyndicateWarBracket = { syndicateWarBracket = it },
              humanPilotCountByMode = humanPilotCountByMode,
              canQueueSyndicateWars = canQueueSyndicateWars,
              onStageRanked = { mode, bracket -> deployOrStageRankedSelection(mode, bracket) },
              publicCustomGames = publicCustomGames,
              customIsPrivateRoom = customIsPrivateRoom,
              onTogglePrivateRoom = {
                customIsPrivateRoom = it
                generatedRoomCode = generateSixCharRoomCode()
              },
              generatedRoomCode = generatedRoomCode,
              joinRoomCodeDraft = joinRoomCodeDraft,
              onJoinRoomCodeDraftChange = { raw ->
                joinRoomCodeDraft = raw.uppercase(Locale.US).replace(Regex("[^A-Z0-9]"), "").take(8)
              },
              roomCodeBanner = roomCodeBanner,
              unlockedPrivateCodes = unlockedPrivateCodes,
              onJoinByRoomCode = { joinCustomByRoomCode(joinRoomCodeDraft) },
              customBaseMode = customBaseMode,
              onSelectCustomBaseMode = { customBaseMode = it },
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
                deployOrStageExistingCustom(instanceId)
              },
              onQuickCreateCustomGame = {
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
                    damageTakenScalePct = customDamageTakenScalePct,
                    weaponFluxCostScalePct = customWeaponFluxCostScalePct,
                    botCount = if (customBotsEnabled) customBotCount else 0,
                    botSkillLevel = customBotSkillLevel,
                    scoreCap = customScoreCap,
                  )
                generatedRoomCode = generateSixCharRoomCode(System.currentTimeMillis() + 7919L)
                deployOrStageNewCustomConfig(cfg)
              },
              customDamageTakenScalePct = customDamageTakenScalePct,
              onSelectCustomDamageTakenScalePct = { updateCustomDamageTakenScale(it) },
              customWeaponFluxCostScalePct = customWeaponFluxCostScalePct,
              onSelectCustomWeaponFluxCostScalePct = { updateCustomWeaponFluxCostScale(it) },
              onOpenSurvivalSpreadsheet = { showHubSurvivalSpreadsheetModal = true },
              onOpenFullCustomGate = {
                overwatchPrompt = OverwatchCircularPrompt.None
                gateTab = ConvergenceGateTab.CUSTOM_RIFT_GATE
                onSelectNode(NexusCoreNode.CONVERGENCE_GATE)
              },
              onOpenShipwrightStudio = {
                onOpenForge()
              },
              onOpenFullSquadronModal = {
                overwatchPrompt = OverwatchCircularPrompt.None
                sideBayMode = RiftHubSideBayMode.SQUADRON_BAY
              },
              syndicateWarRecord = syndicateWarRecord,
              syndicateDirectory = syndicateDirectory,
              syndicateStatusMessage = syndicateStatusMessage,
              onJoinOfficialSyndicate = onJoinOfficialSyndicate,
              onLeaveCurrentSyndicate = onLeaveCurrentSyndicate,
              onCharterCustomSyndicate = onCharterCustomSyndicate,
              onClaimDailyDirectiveSlot = { slotIdx -> onClaimDailyDirectiveSlot(slotIdx) },
              onRerollDailyDirectiveSlot = { slotIdx -> onRerollDailyDirectiveSlot(slotIdx) },
              onClaimDailySweepBonus = { onClaimDailySweepBonus() },
              onUnlockOrEquipCosmeticItem = { item ->
                onUnlockOrEquipCosmeticItem(item.category, item)
              },
              onUnlockOrSelectFrameSlotId = { slotId ->
                onUnlockOrSelectFrameSlotId(slotId)
              },
              activeCustomSlotId = activeCustomSlotId,
              activeCustomSlot = activeCustomSlot,
              pointBudget = pointBudget,
              onSaveCustomSlot = onSaveCustomSlot,
              onShareActiveRiftFrameBuild = onShareActiveRiftFrameBuild,
              cloudLeaderboardEntries = cloudLeaderboardEntries,
              onRenamePilotCallsign = { updated -> onRenamePilotCallsign(updated) },
              onResetLocalPilotTelemetry = onResetLocalPilotTelemetry,
              helmInitialSubDeck = helmInitialSubDeck,
              sfxEnabled = sfxEnabled,
              musicEnabled = musicEnabled,
              musicBpm = musicBpm,
              musicVolume = musicVolume,
              sfxVolume = sfxVolume,
              cameraZoom = cameraZoom,
              playfieldTextScale = playfieldTextScale,
              onToggleSfx = { _ -> onToggleSfx() },
              onToggleMusic = { _ -> onToggleMusic() },
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
              onQuickZoneIntoOverwatch = { inst ->
                if (chargedShipId in 1..10) {
                  onOpenHangarForShip(chargedShipId)
                }
                onQuickZoneIntoOverwatch(inst)
              },
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
              onEnterPracticeRift = {
                if (chargedShipId in 1..10) {
                  onOpenHangarForShip(chargedShipId)
                }
                onEnterPracticeRift()
              },
              onOpenHowToPlayTutorial = {
                if (chargedShipId in 1..10) {
                  onOpenHangarForShip(chargedShipId)
                }
                onOpenHowToPlayTutorial()
              },
              onCloseModal = onCloseModal,
              onClickCallsign = {
                overwatchPrompt = OverwatchCircularPrompt.None
                sideBayMode = RiftHubSideBayMode.DOSSIER_BAY
              },
              onClickResonance = {
                overwatchPrompt = OverwatchCircularPrompt.None
                sideBayMode = RiftHubSideBayMode.CONDUIT_BAY
              },
              onClickShards = {
                overwatchPrompt = OverwatchCircularPrompt.None
                sideBayMode = RiftHubSideBayMode.SHARD_STORE_BAY
              },
              onClickSyn = {
                overwatchPrompt = OverwatchCircularPrompt.None
                sideBayMode = RiftHubSideBayMode.SYNDICATE_BAY
              },
              onClickWing = {
                overwatchPrompt = OverwatchCircularPrompt.None
                sideBayMode = RiftHubSideBayMode.SQUADRON_BAY
              },
              onClickOnline = {
                focusedOrbitalIndex = 3
                onSelectNode(NexusCoreNode.SUBSPACE_COMMS)
              },
              onOpenPilotDossier = {
                focusedOrbitalIndex = 6
                overwatchPrompt = OverwatchCircularPrompt.None
                sideBayMode = RiftHubSideBayMode.DOSSIER_BAY
              },
              onSelectNode = { idx, node ->
                focusedOrbitalIndex = idx
                when (node) {
                  NexusCoreNode.HANGAR_DECK -> {
                    overwatchPrompt = OverwatchCircularPrompt.None
                    sideBayMode = RiftHubSideBayMode.HANGAR_BAY
                  }
                  NexusCoreNode.CONVERGENCE_GATE -> {
                    overwatchPrompt = OverwatchCircularPrompt.None
                    sideBayMode = RiftHubSideBayMode.GAME_MODES_BAY
                  }
                  NexusCoreNode.RIFT_CONDUIT -> {
                    overwatchPrompt = OverwatchCircularPrompt.None
                    sideBayMode = RiftHubSideBayMode.CONDUIT_BAY
                  }
                  NexusCoreNode.SYNDICATE_CITADEL -> {
                    overwatchPrompt = OverwatchCircularPrompt.None
                    sideBayMode =
                      if (syndicateSubDeck == SyndicateCitadelSubDeck.STRIKE_WING) {
                        RiftHubSideBayMode.SQUADRON_BAY
                      } else {
                        RiftHubSideBayMode.SYNDICATE_BAY
                      }
                  }
                  NexusCoreNode.ARCHIVE_CODEX -> {
                    overwatchPrompt = OverwatchCircularPrompt.None
                    sideBayMode =
                      if (archiveSubDeck == ArchiveCodexSubDeck.TACTICAL_CODEX) {
                        RiftHubSideBayMode.HELM_CODEX_BAY
                      } else {
                        RiftHubSideBayMode.DOSSIER_BAY
                      }
                  }
                  NexusCoreNode.HELM_SYSTEMS -> {
                    overwatchPrompt = OverwatchCircularPrompt.None
                    sideBayMode = RiftHubSideBayMode.HELM_CODEX_BAY
                  }
                  else -> onSelectNode(node)
                }
              },
              onSwitchAccount = { showSwitchAccountConfirmModal = true },
            )
          }
        }
        else -> {
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
                    NexusCoreNode.HANGAR_DECK -> {
                      overwatchPrompt = OverwatchCircularPrompt.None
                      sideBayMode = RiftHubSideBayMode.HANGAR_BAY
                      onSelectNode(NexusCoreNode.ORBITAL_HUB)
                    }
                    else -> {
                      if (nextCardNode != activeNode) {
                        onSelectNode(nextCardNode)
                      }
                    }
                  }
                },
                onBackToHub = {
                  overwatchPrompt = OverwatchCircularPrompt.None
                  sideBayMode = RiftHubSideBayMode.HUB_MAIN
                  onSelectNode(NexusCoreNode.ORBITAL_HUB)
                },
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
                        onSelectGateTab = { gateTab = it },
                        activeArmadaName = activeArmadaName,
                        armadaSize = armadaSlots.size.coerceAtLeast(1),
                        syndicateTag = cleanSyndicateTag,
                        hangarShipSummaryLabel = gateShipStatusSummary,
                        activeShipId = gateDeployShipId,
                        bestShipId = resolvedBestShipForGate,
                        onSelectShipInPlace = { chosenShipId -> chargeShipAcrossAllModes(chosenShipId) },
                        onOpenStrikeWing = {
                          overwatchPrompt = OverwatchCircularPrompt.None
                          sideBayMode = RiftHubSideBayMode.SQUADRON_BAY
                          onSelectNode(NexusCoreNode.ORBITAL_HUB)
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
                        onStageRanked = { mode, bracket -> deployOrStageRankedSelection(mode, bracket) },
                        engine = engine,
                        publicCustomGames = publicCustomGames,
                        onlineHumanPilots = onlineHumanPilots,
                        customIsPrivateRoom = customIsPrivateRoom,
                        onTogglePrivateRoom = {
                          customIsPrivateRoom = it
                          generatedRoomCode = generateSixCharRoomCode()
                        },
                        generatedRoomCode = generatedRoomCode,
                        joinRoomCodeDraft = joinRoomCodeDraft,
                        onJoinRoomCodeDraftChange = { raw ->
                          joinRoomCodeDraft = raw.uppercase(Locale.US).replace(Regex("[^A-Z0-9]"), "").take(8)
                        },
                        roomCodeBanner = roomCodeBanner,
                        unlockedPrivateCodes = unlockedPrivateCodes,
                        onJoinByRoomCode = { joinCustomByRoomCode(joinRoomCodeDraft) },
                        onShareRoomToLfg = {
                          val activeRoom = engine?.currentCustomGame
                          val shareCode =
                            if (activeRoom != null && engine.hasJoinedMatch) extractCustomRoomCode(activeRoom)
                            else generatedRoomCode
                          val shareMode =
                            if (activeRoom != null && engine.hasJoinedMatch) activeRoom.baseMode.title
                            else customBaseMode.title
                          onPostPrivateRoomInvite(shareCode, shareMode)
                          roomCodeBanner = "SHARED JOIN CODE [$shareCode] TO #SQUADRON-LFG"
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
                        onToggleBotsEnabled = {
                          customBotsEnabled = it
                          if (it && customBotCount <= 0) customBotCount = 12
                        },
                        customBotCount = customBotCount,
                        onSelectCustomBotCount = { count ->
                          customBotCount = count.coerceIn(0, 19)
                          customBotsEnabled = count > 0
                        },
                        customBotSkillLevel = customBotSkillLevel,
                        onSelectCustomBotSkillLevel = { customBotSkillLevel = it.coerceIn(0, 3) },
                        customScoreCap = customScoreCap,
                        onSelectCustomScoreCap = { customScoreCap = it.coerceIn(5, 500) },
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
                        customDamageTakenScalePct = customDamageTakenScalePct,
                        onSelectCustomDamageTakenScalePct = { updateCustomDamageTakenScale(it) },
                        customWeaponFluxCostScalePct = customWeaponFluxCostScalePct,
                        onSelectCustomWeaponFluxCostScalePct = { updateCustomWeaponFluxCostScale(it) },
                        onReturnToRankedFfa = {
                          deployOrStageRankedSelection(RankedGameMode.SECTOR_CONVERGENCE, 20)
                          customGamesRefreshTick++
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
                          deployOrStageExistingCustom(instanceId)
                        },
                        onLaunchCustomGame = {
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
                              damageTakenScalePct = customDamageTakenScalePct,
                              weaponFluxCostScalePct = customWeaponFluxCostScalePct,
                              botCount = if (customBotsEnabled) customBotCount else 0,
                              botSkillLevel = customBotSkillLevel,
                              scoreCap = customScoreCap,
                            )
                          generatedRoomCode = generateSixCharRoomCode(System.currentTimeMillis() + 7919L)
                          deployOrStageNewCustomConfig(cfg)
                        },
                      )
                    }
                    targetCardNode == NexusCoreNode.SUBSPACE_COMMS -> {
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
                          overwatchPrompt = OverwatchCircularPrompt.None
                          sideBayMode = RiftHubSideBayMode.SYNDICATE_BAY
                          onSelectNode(NexusCoreNode.ORBITAL_HUB)
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
                        onSendVoiceHeartbeat = { voiceKey, payload ->
                          onSendChannelMessage(voiceKey, payload)
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
                        activeCustomRoomCode = engine?.currentCustomGame?.let { extractCustomRoomCode(it) },
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
      if (showSwitchAccountConfirmModal) {
        NexusSwitchAccountConfirmationModal(
          onCancel = { showSwitchAccountConfirmModal = false },
          onConfirmSwitchAccount = {
            showSwitchAccountConfirmModal = false
            onSwitchAccount()
          },
        )
      }
      if (showHubSurvivalSpreadsheetModal) {
        SurvivalTelemetrySpreadsheetModal(
          callsign = callsign,
          liveResonance = resolvedLiveResonance,
          uidOrNull = currentUserId.ifBlank { null },
          localPrefs = localPrefs,
          onClose = { showHubSurvivalSpreadsheetModal = false },
        )
      }
    }
  }
}

private enum class HubWingSlot {
  TOP,
  MIDDLE,
  BOTTOM,
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
  onOpenSurvivalSpreadsheet: () -> Unit = {},
  onOpenFullCustomGate: () -> Unit = {},
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
  onUnlockOrEquipCosmeticItem: (CosmeticCatalogItem) -> Unit = {},
  onUnlockOrSelectFrameSlotId: (String) -> Unit = {},
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
    else String.format(Locale.US, "%,d", riftShards)
  val formattedLiveResonance =
    remember(liveResonance) { String.format(Locale.US, "%,d", liveResonance) }
  val pilotIdentityLabel =
    if (syndicateTag.isNotBlank()) "[$syndicateTag] $callsign" else callsign

  val activeCrestRecipe = RiftVisualForgeState.activeSyndicateCrestRecipe
  val spriteRev = RiftSpriteBaker.spriteVisualRevision
  val customShipRev = RiftSpriteBaker.customShip9Version
  val hullTrim = RiftSpriteBaker.activeHullTrimStyle
  val clampedShipId = activeShipId.coerceIn(1, 10)
  val resolvedBestShipId = bestShipId.coerceIn(1, 10)
  val hasChargedShip = chargedShipId in 1..10
  val isHangarLocked =
    hasChargedShip ||
      activeShipSummary.startsWith("LOCKED|", ignoreCase = true) ||
      engine?.isShipLockedForMatch == true ||
      clampedShipId != resolvedBestShipId
  val hangarIndicatedShipId =
    when {
      hasChargedShip -> chargedShipId
      isHangarLocked -> clampedShipId
      else -> resolvedBestShipId
    }

  val indicatedShipSpec =
    remember(hangarIndicatedShipId, customShipRev) { RiftCodex.getShipById(hangarIndicatedShipId) }
  val hangarPrimaryStat =
    remember(indicatedShipSpec.name) {
      indicatedShipSpec.name.replace(Regex("^[0-9]+-"), "").trim().uppercase(Locale.US)
    }
  val hangarBadgeText =
    when {
      hasChargedShip -> "CHARGED"
      isHangarLocked -> "LOCKED"
      else -> "BEST"
    }

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

  val forgePrimaryStat = "1:1 ARENA & HULL TRIM"
  val forgeBadge = "STUDIO"

  val commsBadge =
    if (unreadCommsCount > 0) "$unreadCommsCount NEW" else "VOICE + AI"
  val commsPrimaryStat =
    if (unreadCommsCount > 0) "$unreadCommsCount UNREAD MESSAGES"
    else "SECTOR CHAT, VOICE & RIFT AI"

  val cleanSyndicateDisplayTitle =
    remember(syndicateTag, syndicateName) {
      syndicateName.trim().takeIf { raw ->
        syndicateTag.isNotBlank() &&
          raw.isNotBlank() &&
          raw.uppercase(Locale.US) !in
            setOf("UNAFFILIATED", "GUEST", "NONE", "SOLO", "FREE", "RFT", "DRIFTER")
      }?.uppercase(Locale.US) ?: "UNAFFILIATED"
    }
  val citadelBadge = if (syndicateTag.isNotBlank()) "[$syndicateTag]" else "SOLO"
  val squadronBadgeText = "${armadaCount.coerceIn(1, 5)}/5"
  val squadronPrimaryStat =
    if (activeArmadaName.isNotBlank()) "[$activeArmadaName]"
    else "${maxOf(strikeWingOnlineCount, armadaCount)}/5 READY"

  val peakResonance =
    maxOf(liveResonance, localPrefs.getPeakResonance(currentUserId.ifBlank { null }))
  val totalWins = localPrefs.getConvergencesWon()
  val archiveBadge = if (totalWins > 0L) "${totalWins}W" else "RECORD"
  val archivePrimaryStat = "PEAK ${String.format(Locale.US, "%,d", peakResonance)}"

  val hangarEmblem =
    remember(hangarIndicatedShipId, spriteRev, customShipRev, hullTrim) {
      RiftSpriteBaker.getNexusHubModuleEmblemBitmap(
        moduleKey = "HANGAR",
        activeShipId = hangarIndicatedShipId,
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
    val outerPadH = if (isTabletDeck) 5.dp else 3.dp
    val wingStackGap = if (isTabletDeck) 6.dp else 4.dp
    val minPodH = minOf(if (isTabletDeck) 84.dp else 68.dp, (safeH * 0.215f).coerceAtLeast(52.dp))
    val cornerPodHeight = (safeH * 0.215f).coerceIn(minPodH, if (isTabletDeck) 96.dp else 82.dp)
    val bayHeaderHeight = if (isTabletDeck) 52.dp else 48.dp

    // True Top-to-Bottom Edge-to-Edge Overwatch Stage + 100% Full-Width Flanking Side-Bays
    val minWingReserveTotal = if (isTabletDeck) 312.dp else 264.dp
    val overwatchStageDiameterDp =
      minOf(safeH, (safeW - minWingReserveTotal).coerceAtLeast(156.dp))
    val sideSpanEachDp = ((safeW - overwatchStageDiameterDp) * 0.5f - outerPadH).coerceAtLeast(128.dp)
    val wingWidthDp = sideSpanEachDp
    val openCornerAnchorDp = outerPadH + wingWidthDp + 4.dp
    val openCornerMaxWidthDp =
      ((safeW * 0.5f) - openCornerAnchorDp - overwatchStageDiameterDp * 0.08f).coerceIn(112.dp, 224.dp)

    Box(modifier = Modifier.fillMaxSize()) {
      // 1. TRUE TOP-TO-BOTTOM EDGE-TO-EDGE CIRCULAR OVERWATCH CENTERPIECE (0dp top/bottom dead padding)
      val centerFocused = focusedOrbitalIndex == 0
      RoundRipplingOverwatchAperture(
        modifier =
          Modifier.align(Alignment.Center)
            .size(overwatchStageDiameterDp),
        isFocused = centerFocused,
        isTabletDeck = isTabletDeck,
        drawHeadlessSafeZoneFallback = headless,
        activeInstance = activeOverwatchInstance,
        overwatchPrompt = overwatchPrompt,
        chargedShipId = chargedShipId,
        bestShipId = resolvedBestShipId,
        shipSpinDeg = shipSpinDeg,
        fluxOrbitRad = fluxOrbitRad,
        renameDraft = renameDraftState,
        onRenameDraftChange = { renameDraftState = it },
        resolveShipResonance = { resolveLocalShipResonance(it) },
        onChargeShip = { chosenShip ->
          onChargeShip(chosenShip)
          onChangeOverwatchPrompt(OverwatchCircularPrompt.None)
          onChangeSideBayMode(RiftHubSideBayMode.HUB_MAIN)
        },
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
        onApertureRootBoundsChanged = onApertureRootBoundsChanged,
      )

      // 2. PORT FLANKING HARDWARE WING (All Non-Full-Screen Windows Digitize Into Flanking Side-Bays)
      HubDigitizingScanlineBox(
        modifier =
          Modifier.align(Alignment.CenterStart)
            .padding(start = outerPadH, top = 2.dp, bottom = 2.dp)
            .width(wingWidthDp)
            .fillMaxHeight(),
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
              val frameShortSlot = activeCustomSlot.slotId.uppercase(Locale.US)
              val framePointsUsed = activeCustomSlot.totalAllocatedPoints()
              val isRiftFrameCharged = chargedShipId == 10
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

                SculptedHubModulePlate(
                  modifier = Modifier.weight(1f).fillMaxWidth(),
                  title = "HANGAR DECK",
                  primaryStat = hangarPrimaryStat,
                  detailCue =
                    if (hasChargedShip) "CHARGED • ORBITING FLUX LOCK"
                    else "10 SHIPS • TAP TO CHARGE HULL",
                  emblemBitmap = hangarEmblem,
                  accent = if (hasChargedShip) Color(0xFF00F5D4) else Color(0xFF38BDF8),
                  secondaryAccent = if (hasChargedShip) Color(0xFF00F5D4) else Color(0xFFFFD700),
                  badgeText = hangarBadgeText,
                  isFocused = focusedOrbitalIndex == 1,
                  isRightColumn = false,
                  slotPosition = HubWingSlot.TOP,
                  isTabletDeck = isTabletDeck,
                  testTag = "nexus_node_hangar_deck",
                  customEmblemContent = { sizeDp ->
                    Spinning25dShipWithOrbitingFlux(
                      shipId = hangarIndicatedShipId,
                      shipSpinDeg = shipSpinDeg,
                      fluxOrbitRad = fluxOrbitRad,
                      isCharged = hasChargedShip,
                      isBestShip = hangarIndicatedShipId == resolvedBestShipId,
                      isSelected = isHangarLocked,
                      sizeDp = sizeDp,
                    )
                  },
                  onClick = {
                    onChangeOverwatchPrompt(OverwatchCircularPrompt.None)
                    onChangeSideBayMode(RiftHubSideBayMode.HANGAR_BAY)
                  },
                )

                SculptedHubModulePlate(
                  modifier = Modifier.weight(1f).fillMaxWidth(),
                  title = "RIFT FRAME",
                  primaryStat = "$frameShortSlot • $framePointsUsed/${pointBudget.coerceAtLeast(100)} PT",
                  detailCue = "${activeCustomSlot.customName.uppercase(Locale.US)} • COSMETICS",
                  emblemBitmap = forgeEmblem,
                  accent = if (isRiftFrameCharged) Color(0xFF00F5D4) else Color(0xFFFFD700),
                  secondaryAccent = Color(0xFF38BDF8),
                  badgeText = if (isRiftFrameCharged) "CHARGED" else frameShortSlot.removePrefix("FRAME ").trim(),
                  isFocused = focusedOrbitalIndex == 2,
                  isRightColumn = false,
                  slotPosition = HubWingSlot.BOTTOM,
                  isTabletDeck = isTabletDeck,
                  testTag = "nexus_node_rift_frame",
                  customEmblemContent = { sizeDp ->
                    Spinning25dShipWithOrbitingFlux(
                      shipId = 10,
                      shipSpinDeg = shipSpinDeg,
                      fluxOrbitRad = fluxOrbitRad,
                      isCharged = isRiftFrameCharged,
                      isBestShip = resolvedBestShipId == 10,
                      isSelected = activeShipId == 10,
                      sizeDp = sizeDp,
                    )
                  },
                  onClick = {
                    onChangeOverwatchPrompt(OverwatchCircularPrompt.None)
                    onChangeSideBayMode(RiftHubSideBayMode.RIFT_FRAME_BAY)
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
              HangarDigitizedWingBay(
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
                  onChangeOverwatchPrompt(OverwatchCircularPrompt.ShipInspect(10))
                  onChangeSideBayMode(RiftHubSideBayMode.RIFT_FRAME_BAY)
                },
                onTapShip = { shipId ->
                  onChangeOverwatchPrompt(OverwatchCircularPrompt.ShipInspect(shipId))
                },
              )
            }
            RiftHubSideBayMode.RIFT_FRAME_BAY -> {
              RiftFrameLeftLoadoutBay(
                isTabletDeck = isTabletDeck,
                headerHeight = bayHeaderHeight,
                wingStackGap = wingStackGap,
                activeCustomSlot = activeCustomSlot,
                activeCustomSlotId = activeCustomSlotId,
                pointBudget = pointBudget.coerceAtLeast(100),
                isCharged = chargedShipId == 10,
                shipResonance = resolveLocalShipResonance(10),
                localPrefs = localPrefs,
                currentUserId = currentUserId,
                onBackToHub = {
                  onChangeOverwatchPrompt(OverwatchCircularPrompt.None)
                  onChangeSideBayMode(RiftHubSideBayMode.HUB_MAIN)
                },
                onSelectFrameSlot = onUnlockOrSelectFrameSlotId,
                onSaveCustomSlot = onSaveCustomSlot,
                onPreviewInOverwatch = {
                  onChangeOverwatchPrompt(OverwatchCircularPrompt.ShipInspect(10))
                },
                onChargeRiftFrame = {
                  onChargeShip(10)
                  onChangeOverwatchPrompt(OverwatchCircularPrompt.None)
                  onChangeSideBayMode(RiftHubSideBayMode.HUB_MAIN)
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
                      else ->
                        "High-intensity competitive 2.5D space combat inside the Storm Wall. Outlast rival pilots, control Supply Crates, and master Playfield Rifts."
                    }
                  onChangeOverwatchPrompt(
                    OverwatchCircularPrompt.InfoStageInspect(
                      kicker = "RANKED GAME MODE",
                      title = mode.title.uppercase(Locale.US),
                      badge = if (bracket in 2..10) "${bracket}V${bracket}" else "OPEN SECTOR",
                      summary = modeSummary,
                      tacticalNote =
                        if (!canDeploy) canQueueSyndicateWars.second
                        else "Matches launch in 15 seconds or less with all 10 hulls on 100-Point parity.",
                      accentHex = 0xFF00F5D4L,
                      showStationPreview =
                        mode == RankedGameMode.SECTOR_CONVERGENCE || mode == RankedGameMode.FLUX_CORE,
                      primaryActionLabel = if (canDeploy) "RIFT IN >" else "",
                      onPrimaryAction = if (canDeploy) ({ onStageRanked(mode, bracket) }) else null,
                    )
                  )
                },
                onDeployMode = onStageRanked,
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
                onClaimDirective = onClaimDailyDirectiveSlot,
                onRerollDirective = onRerollDailyDirectiveSlot,
                onClaimSweep = onClaimDailySweepBonus,
              )
            }
            RiftHubSideBayMode.SHARD_STORE_BAY -> {
              ShardStoreLeftSummaryBay(
                isTabletDeck = isTabletDeck,
                headerHeight = bayHeaderHeight,
                wingStackGap = wingStackGap,
                shardText = shardText,
                liveResonance = liveResonance,
                peakResonance = peakResonance,
                activeCustomSlotId = activeCustomSlotId,
                localPrefs = localPrefs,
                currentUserId = currentUserId,
                onBackToHub = {
                  onChangeOverwatchPrompt(OverwatchCircularPrompt.None)
                  onChangeSideBayMode(RiftHubSideBayMode.HUB_MAIN)
                },
                onSelectFrameSlot = onUnlockOrSelectFrameSlotId,
                onInspectFrameSlot = { gate, slotId, isUnlocked, isSelected ->
                  onChangeOverwatchPrompt(
                    OverwatchCircularPrompt.InfoStageInspect(
                      kicker = "RIFT FRAME BAY",
                      title = "${gate.slotId.uppercase(Locale.US)} • ${gate.archetypeTitle.uppercase(Locale.US)}",
                      badge =
                        when {
                          isSelected -> "ACTIVE BAY"
                          isUnlocked -> "UNLOCKED"
                          else -> "${gate.shardCost} SHARDS"
                        },
                      summary =
                        "Dedicated 100-Point modular Rift Frame loadout bay for custom primary weapons, Crate-tier secondary abilities, and custom stat allocation.",
                      tacticalNote =
                        if (gate.freeAtResonance <= 0) "Open immediately at 0 Resonance."
                        else "Unlocks free at ${gate.freeAtResonance} Peak Resonance or instantly for ${gate.shardCost} Rift Shards.",
                      accentHex = if (isSelected) 0xFF00F5D4L else 0xFFC084FCL,
                      shipId = 10,
                      primaryActionLabel =
                        when {
                          isSelected -> "OPEN RIFT FRAME >"
                          isUnlocked -> "SELECT BAY"
                          else -> "UNLOCK (${gate.shardCost}S)"
                        },
                      onPrimaryAction = {
                        if (isSelected) {
                          onChangeSideBayMode(RiftHubSideBayMode.RIFT_FRAME_BAY)
                        } else {
                          onUnlockOrSelectFrameSlotId(slotId)
                        }
                      },
                    )
                  )
                },
                onOpenRiftFrameBay = {
                  onChangeOverwatchPrompt(OverwatchCircularPrompt.None)
                  onChangeSideBayMode(RiftHubSideBayMode.RIFT_FRAME_BAY)
                },
                onOpenConduitQuests = {
                  onChangeOverwatchPrompt(OverwatchCircularPrompt.None)
                  onChangeSideBayMode(RiftHubSideBayMode.CONDUIT_BAY)
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
                      onConfirm = onResetLocalPilotTelemetry,
                    )
                  )
                },
                onSwitchAccount = onSwitchAccount,
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
                onStartTutorial = onOpenHowToPlayTutorial,
                isNetworkOnline = isNetworkOnline,
                isGoogleAuthenticated = isGoogleAuthenticated,
                cloudSyncHealth = cloudSyncHealth,
                updateStatusLabel = updateStatusLabel,
                hasUpdateAvailable = hasUpdateAvailable,
                onCheckOrApplyUpdate = onCheckOrApplyUpdate,
                onSwitchAccount = onSwitchAccount,
                onBackToHub = {
                  onChangeOverwatchPrompt(OverwatchCircularPrompt.None)
                  onChangeSideBayMode(RiftHubSideBayMode.HUB_MAIN)
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
            .fillMaxHeight(),
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
                  onOpenHowToPlayTutorial = onOpenHowToPlayTutorial,
                  onOpenHelmSystems = { onSelectNode(7, NexusCoreNode.HELM_SYSTEMS) },
                  onLeaveMatch = onLeaveMatch,
                  onPrimaryRightAction = {
                    if (isInMatch) onCloseModal() else onEnterPracticeRift()
                  },
                )

                SculptedHubModulePlate(
                  modifier = Modifier.weight(1f).fillMaxWidth(),
                  title = "SUBSPACE COMMS",
                  primaryStat = commsPrimaryStat,
                  detailCue = "FULL-SCREEN CHAT, VOICE & RIFT AI",
                  emblemBitmap = commsEmblem,
                  accent = Color(0xFF00F5D4),
                  secondaryAccent = if (unreadCommsCount > 0) Color(0xFFFB7185) else Color(0xFF38BDF8),
                  badgeText = commsBadge,
                  isFocused = focusedOrbitalIndex == 3,
                  isRightColumn = true,
                  slotPosition = HubWingSlot.TOP,
                  isTabletDeck = isTabletDeck,
                  testTag = "nexus_node_subspace_comms",
                  onClick = { onSelectNode(3, NexusCoreNode.SUBSPACE_COMMS) },
                )

                SculptedHubModulePlate(
                  modifier = Modifier.weight(1f).fillMaxWidth(),
                  title = "ACTIVE RIFTS",
                  primaryStat = activeOverwatchInstance.modeTitle.uppercase(Locale.US),
                  detailCue = "6 MODES • PUBLIC & CUSTOM RIFTS",
                  emblemBitmap = conduitEmblem,
                  accent = Color(0xFFFFD700),
                  secondaryAccent = Color(0xFF00F5D4),
                  badgeText = if (activeRiftsLiveCount == 1) "1 PILOT" else "$activeRiftsLiveCount PILOTS",
                  isFocused = focusedOrbitalIndex == 4,
                  isRightColumn = true,
                  slotPosition = HubWingSlot.BOTTOM,
                  isTabletDeck = isTabletDeck,
                  testTag = "nexus_node_convergence_gate",
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
              HangarDigitizedWingBay(
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
                  onChangeOverwatchPrompt(OverwatchCircularPrompt.ShipInspect(10))
                  onChangeSideBayMode(RiftHubSideBayMode.RIFT_FRAME_BAY)
                },
                onTapShip = { shipId ->
                  if (shipId == 10) {
                    onChangeOverwatchPrompt(OverwatchCircularPrompt.ShipInspect(10))
                    onChangeSideBayMode(RiftHubSideBayMode.RIFT_FRAME_BAY)
                  } else {
                    onChangeOverwatchPrompt(OverwatchCircularPrompt.ShipInspect(shipId))
                  }
                },
              )
            }
            RiftHubSideBayMode.RIFT_FRAME_BAY -> {
              RiftFrameRightCosmeticsBay(
                isTabletDeck = isTabletDeck,
                headerHeight = bayHeaderHeight,
                wingStackGap = wingStackGap,
                shardText = shardText,
                peakResonance = peakResonance,
                shipSpinDeg = shipSpinDeg,
                fluxOrbitRad = fluxOrbitRad,
                isCharged = chargedShipId == 10,
                localPrefs = localPrefs,
                currentUserId = currentUserId,
                onShareBuild = onShareActiveRiftFrameBuild,
                onInspectCosmetic = { item, isUnlocked, isEquipped ->
                  onChangeOverwatchPrompt(
                    OverwatchCircularPrompt.InfoStageInspect(
                      kicker = item.category.replace("_", " "),
                      title = item.name.uppercase(Locale.US),
                      badge =
                        when {
                          isEquipped -> "EQUIPPED"
                          isUnlocked -> "OWNED"
                          else -> "${item.shardCost} SHARDS"
                        },
                      summary = item.summary,
                      tacticalNote =
                        if (item.freeAtResonance > 0)
                          "Unlocks free at ${item.freeAtResonance} Peak Resonance or instantly with ${item.shardCost} Rift Shards."
                        else "Instant cosmetic unlock (${item.shardCost} Rift Shards).",
                      accentHex = if (isUnlocked) 0xFF00F5D4L else 0xFFFFD700L,
                      shipId = hangarIndicatedShipId,
                      primaryActionLabel =
                        when {
                          isEquipped -> ""
                          isUnlocked -> "EQUIP"
                          else -> "UNLOCK (${item.shardCost}S)"
                        },
                      onPrimaryAction = if (isEquipped) null else ({ onUnlockOrEquipCosmeticItem(item) }),
                    )
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
                onOpenCrestForge = onOpenShipwrightStudio,
              )
            }
            RiftHubSideBayMode.GAME_MODES_BAY -> {
              GameModesRightCustomBay(
                isTabletDeck = isTabletDeck,
                headerHeight = bayHeaderHeight,
                wingStackGap = wingStackGap,
                chargedShipId = hangarIndicatedShipId,
                isShipCharged = hasChargedShip,
                shipSpinDeg = shipSpinDeg,
                fluxOrbitRad = fluxOrbitRad,
                onSwitchToHangarBay = { onChangeSideBayMode(RiftHubSideBayMode.HANGAR_BAY) },
                onOpenFullCustomGate = onOpenFullCustomGate,
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
                onJoinCustomInstance = onJoinCustomInstance,
                onQuickCreateCustomGame = onQuickCreateCustomGame,
              )
            }
            RiftHubSideBayMode.CONDUIT_BAY,
            RiftHubSideBayMode.SHARD_STORE_BAY -> {
              ConduitRightVaultBay(
                isTabletDeck = isTabletDeck,
                headerHeight = bayHeaderHeight,
                wingStackGap = wingStackGap,
                shardText = shardText,
                activeCustomSlotId = activeCustomSlotId,
                localPrefs = localPrefs,
                currentUserId = currentUserId,
                onSelectFrameSlot = onUnlockOrSelectFrameSlotId,
                onInspectCosmetic = { item, isUnlocked, isEquipped ->
                  onChangeOverwatchPrompt(
                    OverwatchCircularPrompt.InfoStageInspect(
                      kicker = "SHARD STORE • ${item.category.replace("_", " ")}",
                      title = item.name.uppercase(Locale.US),
                      badge =
                        when {
                          isEquipped -> "ACTIVE"
                          isUnlocked -> "OWNED"
                          else -> "${item.shardCost} SHARDS"
                        },
                      summary = item.summary,
                      tacticalNote =
                        if (item.freeAtResonance > 0)
                          "Unlocks free at ${item.freeAtResonance} Peak Resonance or instantly with ${item.shardCost} Rift Shards ($shardText available)."
                        else "Unlock instantly with ${item.shardCost} Rift Shards ($shardText available).",
                      accentHex = if (isUnlocked) 0xFF00F5D4L else 0xFFFFD700L,
                      shipId = hangarIndicatedShipId,
                      primaryActionLabel =
                        when {
                          isEquipped -> ""
                          isUnlocked -> "EQUIP"
                          else -> "UNLOCK (${item.shardCost}S)"
                        },
                      onPrimaryAction = if (isEquipped) null else ({ onUnlockOrEquipCosmeticItem(item) }),
                    )
                  )
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
                cloudLeaderboardEntries = cloudLeaderboardEntries,
                onOpenSurvivalSpreadsheet = onOpenSurvivalSpreadsheet,
                onInspectShip = { shipId ->
                  onChangeOverwatchPrompt(OverwatchCircularPrompt.ShipInspect(shipId))
                },
              )
            }
            RiftHubSideBayMode.HELM_CODEX_BAY -> {
              HelmRightCodexBay(
                isTabletDeck = isTabletDeck,
                headerHeight = bayHeaderHeight,
                wingStackGap = wingStackGap,
                initialSubDeck = helmInitialSubDeck,
                onInspectCodexStage = { infoPrompt ->
                  onChangeOverwatchPrompt(infoPrompt)
                },
                onInspectShipStage = { shipId ->
                  onChangeOverwatchPrompt(OverwatchCircularPrompt.ShipInspect(shipId))
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
              onChargeShip(p.shipId)
              onChangeOverwatchPrompt(OverwatchCircularPrompt.None)
              onChangeSideBayMode(RiftHubSideBayMode.HUB_MAIN)
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
            is OverwatchCircularPrompt.InfoStageInspect -> {
              p.onPrimaryAction?.invoke()
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

private fun Modifier.chamferedSciFiPodSurface(
  isRightSide: Boolean,
  primaryBorder: Color,
  secondaryBorder: Color,
): Modifier =
  this.drawBehind {
    val w = size.width
    val h = size.height
    if (w <= 8f || h <= 8f) return@drawBehind
    val drop = 2.0.dp.toPx()
    val outerCut = 10.dp.toPx().coerceAtMost(h * 0.28f)
    val innerCut = 5.dp.toPx()
    val bodyH = (h - drop).coerceAtLeast(8f)

    fun podPath(yOff: Float): Path =
      Path().apply {
        if (!isRightSide) {
          moveTo(outerCut, yOff)
          lineTo(w - innerCut, yOff)
          lineTo(w, yOff + innerCut)
          lineTo(w, yOff + bodyH - innerCut)
          lineTo(w - innerCut, yOff + bodyH)
          lineTo(outerCut, yOff + bodyH)
          lineTo(0f, yOff + bodyH - outerCut)
          lineTo(0f, yOff + outerCut)
          close()
        } else {
          moveTo(innerCut, yOff)
          lineTo(w - outerCut, yOff)
          lineTo(w, yOff + outerCut)
          lineTo(w, yOff + bodyH - outerCut)
          lineTo(w - outerCut, yOff + bodyH)
          lineTo(innerCut, yOff + bodyH)
          lineTo(0f, yOff + bodyH - innerCut)
          lineTo(0f, yOff + innerCut)
          close()
        }
      }

    val shadow = podPath(drop)
    val body = podPath(0f)
    drawPath(shadow, color = Color(0xE0010308))
    drawPath(
      path = body,
      brush =
        Brush.verticalGradient(
          listOf(Color(0xFA0B192E), Color(0xF8060D1A), Color(0xFC02050B))
        ),
    )
    drawPath(
      path = body,
      brush =
        Brush.horizontalGradient(
          if (!isRightSide) {
            listOf(primaryBorder.copy(alpha = 0.88f), Color(0x661E293B), secondaryBorder.copy(alpha = 0.80f))
          } else {
            listOf(secondaryBorder.copy(alpha = 0.80f), Color(0x661E293B), primaryBorder.copy(alpha = 0.88f))
          }
        ),
      style = Stroke(width = 1.15.dp.toPx()),
    )
    // Top-left optical specular catchlight
    drawLine(
      brush =
        Brush.horizontalGradient(
          listOf(
            Color.White.copy(alpha = 0.46f),
            primaryBorder.copy(alpha = 0.44f),
            Color.Transparent,
          )
        ),
      start = Offset((if (!isRightSide) outerCut else innerCut) + 4.dp.toPx(), 1.3.dp.toPx()),
      end = Offset(w * 0.68f, 1.3.dp.toPx()),
      strokeWidth = 1.1.dp.toPx(),
    )
  }

@Composable
private fun PortTopPilotPod(
  heightDp: Dp,
  isTabletDeck: Boolean,
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
        modifier = Modifier.size(if (isTabletDeck) 30.dp else 24.dp),
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
            fontSize = if (isTabletDeck) 12.6.sp else 11.0.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
          )
        }
        Spacer(modifier = Modifier.height(3.dp))
        Text(
          text =
            if (conduitRankLabel.isNotBlank()) "DOSSIER • $conduitRankLabel"
            else "PILOT DOSSIER >",
          color = Color(0xFF38BDF8),
          fontSize = if (isTabletDeck) 8.8.sp else 7.8.sp,
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

    Column(
      modifier =
        Modifier.minimumInteractiveComponentSize()
          .fillMaxHeight()
          .clickable { onSwitchAccount() }
          .padding(start = 7.dp, end = 3.dp)
          .testTag("nexus_top_switch_account"),
      verticalArrangement = Arrangement.Center,
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Text(
        text = "SWITCH",
        color = Color(0xFFFACC15),
        fontSize = if (isTabletDeck) 9.4.sp else 8.4.sp,
        fontWeight = FontWeight.Black,
        fontFamily = FontFamily.Monospace,
        maxLines = 1,
        style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
      )
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = if (isGoogleAuthenticated) "CLOUD ID" else "ACCOUNT",
        color = Color(0xFF94A3B8),
        fontSize = if (isTabletDeck) 7.8.sp else 7.0.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace,
        maxLines = 1,
        style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
      )
    }
  }
}

@Composable
private fun PortBottomTelemetryPod(
  heightDp: Dp,
  isTabletDeck: Boolean,
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
        Modifier.weight(1.02f)
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
        ResonanceCrestIcon(sizeDp = if (isTabletDeck) 16.dp else 14.dp)
        Text(
          text = formattedLiveResonance,
          color = Color(0xFFFFD700),
          fontSize = if (isTabletDeck) 11.4.sp else 9.8.sp,
          fontWeight = FontWeight.Black,
          fontFamily = FontFamily.Monospace,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
        )
      }
      Spacer(modifier = Modifier.height(2.5.dp))
      Text(
        text = conduitBadge.ifBlank { "CONDUIT >" },
        color = Color(0xFFFACC15),
        fontSize = if (isTabletDeck) 8.2.sp else 7.2.sp,
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
        Modifier.weight(1.06f)
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
        RiftShardGemIcon(sizeDp = if (isTabletDeck) 16.dp else 14.dp)
        Text(
          text = shardText,
          color = Color(0xFF38BDF8),
          fontSize = if (isTabletDeck) 11.2.sp else 9.6.sp,
          fontWeight = FontWeight.Black,
          fontFamily = FontFamily.Monospace,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
        )
      }
      Spacer(modifier = Modifier.height(2.5.dp))
      Text(
        text = "SHARD STORE >",
        color = Color(0xFF00F5D4),
        fontSize = if (isTabletDeck) 8.0.sp else 7.0.sp,
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
        Modifier.weight(0.95f)
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
          fontSize = if (isTabletDeck) 10.0.sp else 8.8.sp,
          fontWeight = FontWeight.Black,
          fontFamily = FontFamily.Monospace,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
        )
      }
      Spacer(modifier = Modifier.height(2.5.dp))
      Text(
        text = "ROSTER >",
        color = Color(0xFF94A3B8),
        fontSize = if (isTabletDeck) 8.0.sp else 7.0.sp,
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
  isTabletDeck: Boolean,
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
        fontSize = if (isTabletDeck) 11.0.sp else 9.8.sp,
        fontWeight = FontWeight.Black,
        fontFamily = FontFamily.Monospace,
        maxLines = 1,
        style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
      )
      Spacer(modifier = Modifier.height(2.5.dp))
      Text(
        text = "SOLO BRIEFING",
        color = Color(0xFFCBD5E1),
        fontSize = if (isTabletDeck) 8.6.sp else 7.6.sp,
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
        text = if (isTabletDeck && !isInMatch) "HELM SYSTEMS" else "HELM",
        color = if (helmFocused) Color.White else Color(0xFF38BDF8),
        fontSize = if (isTabletDeck) 11.0.sp else 9.8.sp,
        fontWeight = FontWeight.Black,
        fontFamily = FontFamily.Monospace,
        maxLines = 1,
        style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
      )
      Spacer(modifier = Modifier.height(2.5.dp))
      Text(
        text = if (isTabletDeck) "CODEX & FORGE" else "CODEX >",
        color = Color(0xFF00F5D4),
        fontSize = if (isTabletDeck) 8.6.sp else 7.6.sp,
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
          fontSize = if (isTabletDeck) 10.4.sp else 9.2.sp,
          fontWeight = FontWeight.Black,
          fontFamily = FontFamily.Monospace,
          maxLines = 1,
          style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
        )
        Spacer(modifier = Modifier.height(2.5.dp))
        Text(
          text = "EXIT RIFT",
          color = Color(0xFFCBD5E1),
          fontSize = if (isTabletDeck) 8.2.sp else 7.4.sp,
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
          text = if (isTabletDeck) "RETURN >" else "RIFT >",
          color = Color(0xFF00F5D4),
          fontSize = if (isTabletDeck) 10.6.sp else 9.4.sp,
          fontWeight = FontWeight.Black,
          fontFamily = FontFamily.Monospace,
          maxLines = 1,
          style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
        )
        Spacer(modifier = Modifier.height(2.5.dp))
        Text(
          text = "ACTIVE MATCH",
          color = Color(0xFFCBD5E1),
          fontSize = if (isTabletDeck) 8.2.sp else 7.4.sp,
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
  isTabletDeck: Boolean,
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
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        Box(
          modifier =
            Modifier.size(6.5.dp)
              .clip(CircleShape)
              .background(if (syndicateOnlineCount > 0) Color(0xFFFFD700) else Color(0xFF64748B))
        )
        Text(
          text =
            if (syndicateTag.isNotBlank()) "[$syndicateTag] • $syndicateOnlineCount ON"
            else if (isTabletDeck) "SYNDICATE • $syndicateOnlineCount"
            else "SYN • $syndicateOnlineCount",
          color = Color(0xFFFACC15),
          fontSize = if (isTabletDeck) 10.6.sp else 9.2.sp,
          fontWeight = FontWeight.Black,
          fontFamily = FontFamily.Monospace,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
        )
      }
      Spacer(modifier = Modifier.height(2.5.dp))
      Text(
        text = syndicateDisplayName.ifBlank { "SYNDICATE >" },
        color = Color(0xFF94A3B8),
        fontSize = if (isTabletDeck) 8.2.sp else 7.2.sp,
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
          text =
            if (isTabletDeck) "SQUADRON • ${maxOf(strikeWingOnlineCount, armadaCount)}/5"
            else "SQUAD • ${maxOf(strikeWingOnlineCount, armadaCount)}/5",
          color = Color(0xFF00F5D4),
          fontSize = if (isTabletDeck) 10.6.sp else 9.2.sp,
          fontWeight = FontWeight.Black,
          fontFamily = FontFamily.Monospace,
          maxLines = 1,
          style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
        )
      }
      Spacer(modifier = Modifier.height(2.5.dp))
      Text(
        text = activeArmadaName.ifBlank { "SQUAD UP >" },
        color = Color(0xFF38BDF8),
        fontSize = if (isTabletDeck) 8.2.sp else 7.2.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
      )
    }
  }
}

/**
 * Overwatch-Anchored Concave-Contoured 2.5D Sci-Fi Hardware Blade:
 * - Deep abyssal/gunmetal slate (#02050B / #030711 / #060D1A / #091528 / #0B192E) beveled chassis with top-left specular catchlight.
 * - Supports optional `customEmblemContent` so `HANGAR DECK` carries the Charged 2.5D ship + orbiting Flux icons onto Rift Hub.
 */
@Composable
private fun SculptedHubModulePlate(
  modifier: Modifier,
  title: String,
  primaryStat: String,
  detailCue: String,
  emblemBitmap: androidx.compose.ui.graphics.ImageBitmap,
  accent: Color,
  secondaryAccent: Color,
  badgeText: String?,
  isFocused: Boolean,
  isRightColumn: Boolean,
  slotPosition: HubWingSlot,
  isTabletDeck: Boolean,
  testTag: String,
  customEmblemContent: (@Composable (Dp) -> Unit)? = null,
  onClick: () -> Unit,
) {
  val phaseSeedRad = remember(title) { ((abs(title.hashCode()) % 360) * (PI / 180.0)).toFloat() }
  val titleGlow =
    remember(accent, isFocused) {
      Shadow(
        color = if (isFocused) Color.White.copy(alpha = 0.55f) else accent.copy(alpha = 0.34f),
        offset = Offset.Zero,
        blurRadius = 5f,
      )
    }

  BoxWithConstraints(
    modifier =
      modifier
        .minimumInteractiveComponentSize()
        .heightIn(min = 52.dp)
        .clickable { onClick() }
        .testTag(testTag),
  ) {
    val showDetailLine = maxHeight >= 96.dp
    val emblemSizeDp = (maxHeight * 0.68f).coerceIn(42.dp, if (isTabletDeck) 74.dp else 54.dp)

    Canvas(modifier = Modifier.fillMaxSize()) {
      val w = size.width
      val h = size.height
      if (w <= 8f || h <= 8f) return@Canvas
      val drop = 2.4.dp.toPx()
      val outerChamfer = 12.dp.toPx().coerceAtMost(h * 0.24f)
      val innerSlope = 13.dp.toPx().coerceAtMost(w * 0.08f)
      val concaveBow = 10.dp.toPx().coerceAtMost(w * 0.06f)
      val cornerCut = 5.dp.toPx()

      fun buildPlatePath(yOffset: Float, hBody: Float): Path =
        Path().apply {
          if (!isRightColumn) {
            when (slotPosition) {
              HubWingSlot.TOP -> {
                moveTo(outerChamfer, yOffset)
                lineTo(w - cornerCut, yOffset)
                lineTo(w, yOffset + cornerCut)
                lineTo(w - innerSlope, yOffset + hBody - cornerCut)
                lineTo(w - innerSlope - cornerCut, yOffset + hBody)
                lineTo(outerChamfer, yOffset + hBody)
                lineTo(0f, yOffset + hBody - outerChamfer)
                lineTo(0f, yOffset + outerChamfer)
                close()
              }
              HubWingSlot.MIDDLE -> {
                moveTo(outerChamfer, yOffset)
                lineTo(w - cornerCut, yOffset)
                lineTo(w, yOffset + cornerCut)
                quadraticTo(
                  w - concaveBow,
                  yOffset + hBody * 0.5f,
                  w,
                  yOffset + hBody - cornerCut,
                )
                lineTo(w - cornerCut, yOffset + hBody)
                lineTo(outerChamfer, yOffset + hBody)
                lineTo(0f, yOffset + hBody - outerChamfer)
                lineTo(0f, yOffset + outerChamfer)
                close()
              }
              HubWingSlot.BOTTOM -> {
                moveTo(outerChamfer, yOffset)
                lineTo(w - innerSlope - cornerCut, yOffset)
                lineTo(w - innerSlope, yOffset + cornerCut)
                lineTo(w, yOffset + hBody - cornerCut)
                lineTo(w - cornerCut, yOffset + hBody)
                lineTo(outerChamfer, yOffset + hBody)
                lineTo(0f, yOffset + hBody - outerChamfer)
                lineTo(0f, yOffset + outerChamfer)
                close()
              }
            }
          } else {
            when (slotPosition) {
              HubWingSlot.TOP -> {
                moveTo(cornerCut, yOffset)
                lineTo(w - outerChamfer, yOffset)
                lineTo(w, yOffset + outerChamfer)
                lineTo(w, yOffset + hBody - outerChamfer)
                lineTo(w - outerChamfer, yOffset + hBody)
                lineTo(innerSlope + cornerCut, yOffset + hBody)
                lineTo(innerSlope, yOffset + hBody - cornerCut)
                lineTo(0f, yOffset + cornerCut)
                close()
              }
              HubWingSlot.MIDDLE -> {
                moveTo(cornerCut, yOffset)
                lineTo(w - outerChamfer, yOffset)
                lineTo(w, yOffset + outerChamfer)
                lineTo(w, yOffset + hBody - outerChamfer)
                lineTo(w - outerChamfer, yOffset + hBody)
                lineTo(cornerCut, yOffset + hBody)
                lineTo(0f, yOffset + hBody - cornerCut)
                quadraticTo(
                  concaveBow,
                  yOffset + hBody * 0.5f,
                  0f,
                  yOffset + cornerCut,
                )
                close()
              }
              HubWingSlot.BOTTOM -> {
                moveTo(innerSlope + cornerCut, yOffset)
                lineTo(w - outerChamfer, yOffset)
                lineTo(w, yOffset + outerChamfer)
                lineTo(w, yOffset + hBody - outerChamfer)
                lineTo(w - outerChamfer, yOffset + hBody)
                lineTo(cornerCut, yOffset + hBody)
                lineTo(0f, yOffset + hBody - cornerCut)
                lineTo(innerSlope, yOffset + cornerCut)
                close()
              }
            }
          }
        }

      val bodyH = (h - drop).coerceAtLeast(8f)
      val shadowPath = buildPlatePath(drop, bodyH)
      val platePath = buildPlatePath(0f, bodyH)

      drawPath(shadowPath, color = Color(0xE0010308))
      drawPath(
        path = platePath,
        brush =
          Brush.verticalGradient(
            colors =
              if (isFocused) {
                listOf(Color(0xFA102442), Color(0xF8091528), Color(0xFC02050B))
              } else {
                listOf(Color(0xFA0B192E), Color(0xF8060D1A), Color(0xFC02050B))
              }
          ),
      )
      drawPath(
        path = platePath,
        color = Color(0x9902050B),
        style = Stroke(width = 3.2.dp.toPx()),
      )
      drawPath(
        path = platePath,
        brush =
          Brush.horizontalGradient(
            if (isRightColumn) {
              listOf(
                accent.copy(alpha = if (isFocused) 1f else 0.88f),
                Color(0x661E293B),
                secondaryAccent.copy(alpha = 0.74f),
              )
            } else {
              listOf(
                secondaryAccent.copy(alpha = 0.74f),
                Color(0x661E293B),
                accent.copy(alpha = if (isFocused) 1f else 0.88f),
              )
            }
          ),
        style = Stroke(width = if (isFocused) 1.65.dp.toPx() else 1.2.dp.toPx()),
      )

      val specStartX = (if (isRightColumn) cornerCut else outerChamfer) + 4.dp.toPx()
      drawLine(
        brush =
          Brush.horizontalGradient(
            listOf(
              Color.White.copy(alpha = if (isFocused) 0.58f else 0.44f),
              accent.copy(alpha = 0.44f),
              Color.Transparent,
            )
          ),
        start = Offset(specStartX, 1.3.dp.toPx()),
        end = Offset(w * 0.72f, 1.3.dp.toPx()),
        strokeWidth = 1.15.dp.toPx(),
      )

      val midInnerEdgeX =
        if (!isRightColumn) {
          when (slotPosition) {
            HubWingSlot.MIDDLE -> w - concaveBow * 0.5f - 2.0.dp.toPx()
            else -> w - innerSlope * 0.5f - 2.0.dp.toPx()
          }
        } else {
          when (slotPosition) {
            HubWingSlot.MIDDLE -> concaveBow * 0.5f + 2.0.dp.toPx()
            else -> innerSlope * 0.5f + 2.0.dp.toPx()
          }
        }
      drawLine(
        brush = Brush.verticalGradient(listOf(Color.Transparent, accent, secondaryAccent, Color.Transparent)),
        start = Offset(midInnerEdgeX, bodyH * 0.22f),
        end = Offset(midInnerEdgeX, bodyH * 0.78f),
        strokeWidth = 2.2.dp.toPx(),
        cap = StrokeCap.Round,
      )
    }

    val innerClearanceDp = if (isTabletDeck) 15.dp else 13.dp
    Row(
      modifier =
        Modifier.fillMaxSize()
          .padding(
            start = if (isRightColumn) innerClearanceDp else 9.dp,
            end = if (isRightColumn) 9.dp else innerClearanceDp,
            top = 4.dp,
            bottom = 6.dp,
          ),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(if (isTabletDeck) 10.dp else 7.dp),
    ) {
      if (!isRightColumn) {
        if (customEmblemContent != null) {
          customEmblemContent(emblemSizeDp)
        } else {
          HubMetallicEmblemPedestal(
            emblemBitmap = emblemBitmap,
            contentDescription = title,
            accent = accent,
            secondaryAccent = secondaryAccent,
            phaseSeedRad = phaseSeedRad,
            sizeDp = emblemSizeDp,
          )
        }
      }

      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(if (isTabletDeck) 3.5.dp else 2.5.dp),
        horizontalAlignment = Alignment.Start,
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Text(
            text = title,
            color = Color.White,
            fontSize = if (isTabletDeck) 12.8.sp else 11.0.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style =
              TextStyle(
                shadow = titleGlow,
                platformStyle = PlatformTextStyle(includeFontPadding = false),
              ),
            modifier = Modifier.weight(1f, fill = false).padding(end = 4.dp),
          )

          if (badgeText != null) {
            Text(
              text = badgeText,
              color = secondaryAccent,
              fontSize = if (isTabletDeck) 9.4.sp else 8.4.sp,
              fontWeight = FontWeight.Black,
              fontFamily = FontFamily.Monospace,
              maxLines = 1,
              softWrap = false,
              style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
            )
          }
        }

        Text(
          text = primaryStat,
          color = accent,
          fontSize = if (isTabletDeck) 11.0.sp else 9.8.sp,
          fontWeight = FontWeight.ExtraBold,
          fontFamily = FontFamily.Monospace,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
        )

        if (showDetailLine) {
          Text(
            text = detailCue,
            color = Color(0xFFCBD5E1),
            fontSize = if (isTabletDeck) 9.4.sp else 8.4.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
          )
        }
      }

      if (isRightColumn) {
        if (customEmblemContent != null) {
          customEmblemContent(emblemSizeDp)
        } else {
          HubMetallicEmblemPedestal(
            emblemBitmap = emblemBitmap,
            contentDescription = title,
            accent = accent,
            secondaryAccent = secondaryAccent,
            phaseSeedRad = phaseSeedRad,
            sizeDp = emblemSizeDp,
          )
        }
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
  sizeDp: Dp = 56.dp,
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
    modifier = Modifier.size(sizeDp),
    contentAlignment = Alignment.Center,
  ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val minDim = size.minDimension
      if (minDim <= 6f) return@Canvas
      val cx = center.x
      val cy = center.y
      val outerR = minDim * 0.485f
      val pulse = 0.88f + 0.12f * sin(instrumentPhaseRad * 2f)

      // 1. Dual-Bevel 3D Dark-Titanium Turret Socket Well
      drawCircle(
        color = Color(0xCC010308),
        radius = outerR,
        center = Offset(cx, cy + 1.4.dp.toPx()),
      )
      drawCircle(
        brush =
          Brush.radialGradient(
            colors =
              listOf(
                Color(0xFF0E203B),
                Color(0xFF040914),
                Color(0xFF193052),
              ),
            center = center,
            radius = outerR,
          ),
        radius = outerR,
        center = center,
      )
      drawCircle(
        brush =
          Brush.radialGradient(
            colors =
              listOf(
                accent.copy(alpha = 0.30f * pulse),
                secondaryAccent.copy(alpha = 0.14f * pulse),
                Color.Transparent,
              ),
            center = center,
            radius = outerR * 0.96f,
          ),
        radius = outerR * 0.96f,
        center = center,
      )
      drawCircle(
        brush =
          Brush.verticalGradient(
            listOf(
              Color.White.copy(alpha = 0.42f),
              accent.copy(alpha = 0.62f),
              Color(0xFF02050B),
            )
          ),
        radius = outerR,
        center = center,
        style = Stroke(width = 1.35.dp.toPx()),
      )

      // 2. Calibrated 24-Tick Instrument Vernier Ring
      val tickInnerMajor = outerR * 0.84f
      val tickInnerMinor = outerR * 0.90f
      for (idx in 0 until 24) {
        val ang = idx * ((2.0 * PI) / 24.0).toFloat() + instrumentPhaseRad * 0.20f
        val isMajor = idx % 6 == 0
        val r0 = if (isMajor) tickInnerMajor else tickInnerMinor
        drawLine(
          color =
            if (isMajor) accent.copy(alpha = 0.78f)
            else secondaryAccent.copy(alpha = 0.32f),
          start = Offset(cx + cos(ang) * r0, cy + sin(ang) * r0),
          end = Offset(cx + cos(ang) * (outerR * 0.97f), cy + sin(ang) * (outerR * 0.97f)),
          strokeWidth = if (isMajor) 1.25.dp.toPx() else 0.75.dp.toPx(),
          cap = StrokeCap.Round,
        )
      }

      // 3. Upper-Left Optical Specular Rim + Slow-Moving Orbital Sweep Arc
      val specR = outerR * 0.94f
      drawArc(
        color = Color.White.copy(alpha = 0.45f),
        startAngle = -148f,
        sweepAngle = 52f,
        useCenter = false,
        topLeft = Offset(cx - specR, cy - specR),
        size = Size(specR * 2f, specR * 2f),
        style = Stroke(width = 1.0.dp.toPx(), cap = StrokeCap.Round),
      )
      val sweepDeg = (instrumentPhaseRad * (180f / PI.toFloat())) % 360f
      val ringR = outerR * 0.91f
      drawArc(
        color = accent.copy(alpha = 0.88f),
        startAngle = sweepDeg,
        sweepAngle = 56f,
        useCenter = false,
        topLeft = Offset(cx - ringR, cy - ringR),
        size = Size(ringR * 2f, ringR * 2f),
        style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round),
      )
    }

    Image(
      bitmap = emblemBitmap,
      contentDescription = contentDescription,
      modifier = Modifier.fillMaxSize(0.76f),
      contentScale = ContentScale.Fit,
    )
  }
}

@Composable
private fun HubDigitizedReturnBar(
  heightDp: Dp,
  isRightSide: Boolean,
  isTabletDeck: Boolean,
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
        .chamferedSciFiPodSurface(
          isRightSide = isRightSide,
          primaryBorder = accent,
          secondaryBorder = Color(0xFF00F5D4),
        )
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
          fontSize = if (isTabletDeck) 10.8.sp else 9.6.sp,
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
      fontSize = if (isTabletDeck) 10.8.sp else 9.6.sp,
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
          fontSize = if (isTabletDeck) 9.8.sp else 8.8.sp,
          fontWeight = FontWeight.Black,
          fontFamily = FontFamily.Monospace,
          maxLines = 1,
          style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
        )
      }
    }
  }
}

@Composable
private fun HangarDigitizedWingBay(
  ships: List<ShipSpec>,
  isRightSide: Boolean,
  isTabletDeck: Boolean,
  headerHeight: Dp,
  wingStackGap: Dp,
  chargedShipId: Int,
  bestShipId: Int,
  inspectedShipId: Int,
  shipSpinDeg: Float,
  fluxOrbitRad: Float,
  shipResonanceFor: (Int) -> Int,
  onBackToHub: () -> Unit,
  onOpenStudio: () -> Unit,
  onTapShip: (Int) -> Unit,
) {
  Column(
    modifier = Modifier.fillMaxSize(),
    verticalArrangement = Arrangement.spacedBy(wingStackGap),
  ) {
    if (!isRightSide) {
      HubDigitizedReturnBar(
        heightDp = headerHeight,
        isRightSide = false,
        isTabletDeck = isTabletDeck,
        sectionTitle = "HULLS 01-05",
        accent = Color(0xFF00F5D4),
        onBackToHub = onBackToHub,
      )
    } else {
      HubDigitizedReturnBar(
        heightDp = headerHeight,
        isRightSide = true,
        isTabletDeck = isTabletDeck,
        sectionTitle = "HULLS 06-10",
        accent = Color(0xFF38BDF8),
        trailingLabel = "RIFT FRAME >",
        trailingTag = "hangar_bay_open_studio",
        onTrailingClick = onOpenStudio,
      )
    }

    Column(
      modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(wingStackGap),
    ) {
      ships.forEach { ship ->
        val isCharged = chargedShipId == ship.id
        val isBest = bestShipId == ship.id
        val isInspected = inspectedShipId == ship.id
        val cleanName = remember(ship.name) { ship.name.replace(Regex("^[0-9]+-"), "").trim().uppercase(Locale.US) }
        val shipRes = shipResonanceFor(ship.id)
        val primaryBorder =
          when {
            isCharged -> Color(0xFF00F5D4)
            isInspected -> Color.White
            isBest -> Color(0xFFFFD700)
            else -> Color(0xFF38BDF8)
          }
        val secondaryBorder =
          when {
            isCharged -> Color(0xFF38BDF8)
            isBest -> Color(0xFFFACC15)
            else -> Color(0xFF091528)
          }

        Row(
          modifier =
            Modifier.fillMaxWidth()
              .minimumInteractiveComponentSize()
              .heightIn(min = if (isTabletDeck) 72.dp else 54.dp)
              .chamferedSciFiPodSurface(
                isRightSide = isRightSide,
                primaryBorder = primaryBorder,
                secondaryBorder = secondaryBorder,
              )
              .clickable { onTapShip(ship.id) }
              .padding(horizontal = 8.dp, vertical = 4.dp)
              .testTag("hangar_bay_ship_${ship.id}"),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          if (!isRightSide) {
            Spinning25dShipWithOrbitingFlux(
              shipId = ship.id,
              shipSpinDeg = shipSpinDeg,
              fluxOrbitRad = fluxOrbitRad,
              isCharged = isCharged,
              isBestShip = isBest,
              isSelected = isInspected,
              sizeDp = if (isTabletDeck) 56.dp else 44.dp,
            )
          }

          Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = cleanName,
                color = Color.White,
                fontSize = if (isTabletDeck) 11.4.sp else 10.0.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
                modifier = Modifier.weight(1f, fill = false).padding(end = 4.dp),
              )
              if (isCharged || isBest) {
                Text(
                  text = if (isCharged) "CHARGED" else "BEST",
                  color = if (isCharged) Color(0xFF00F5D4) else Color(0xFFFFD700),
                  fontSize = if (isTabletDeck) 9.0.sp else 8.2.sp,
                  fontWeight = FontWeight.Black,
                  fontFamily = FontFamily.Monospace,
                  maxLines = 1,
                  style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
                )
              } else {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                  ResonanceCrestIcon(sizeDp = 11.dp)
                  Text(
                    text = "$shipRes",
                    color = Color(0xFFFACC15),
                    fontSize = if (isTabletDeck) 9.0.sp else 8.2.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
                  )
                }
              }
            }
            Text(
              text = "${ship.role.uppercase(Locale.US)} • ${ship.primaryShortLabel}/${ship.secondaryShortLabel}",
              color = if (isCharged) Color(0xFF00F5D4) else Color(0xFF38BDF8),
              fontSize = if (isTabletDeck) 9.4.sp else 8.4.sp,
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.Monospace,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
            )
          }

          if (isRightSide) {
            Spinning25dShipWithOrbitingFlux(
              shipId = ship.id,
              shipSpinDeg = shipSpinDeg,
              fluxOrbitRad = fluxOrbitRad,
              isCharged = isCharged,
              isBestShip = isBest,
              isSelected = isInspected,
              sizeDp = if (isTabletDeck) 56.dp else 44.dp,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun SquadronLeftPilotListBay(
  isTabletDeck: Boolean,
  headerHeight: Dp,
  wingStackGap: Dp,
  filter: SquadronPilotFilter,
  onSelectFilter: (SquadronPilotFilter) -> Unit,
  sortMode: SquadronPilotSort = SquadronPilotSort.RESONANCE,
  onToggleSort: () -> Unit = {},
  pilots: List<SquadronOnlinePilotItem>,
  selectedPilotUid: String,
  squadronStatusBanner: String?,
  onBackToHub: () -> Unit,
  onBroadcastLfg: () -> Unit,
  onTapPilot: (SquadronOnlinePilotItem) -> Unit,
) {
  val spriteRev = RiftSpriteBaker.spriteVisualRevision
  val customRev = RiftSpriteBaker.customShip9Version

  Column(
    modifier = Modifier.fillMaxSize(),
    verticalArrangement = Arrangement.spacedBy(wingStackGap),
  ) {
    HubDigitizedReturnBar(
      heightDp = headerHeight,
      isRightSide = false,
      isTabletDeck = isTabletDeck,
      sectionTitle = "ONLINE (${pilots.size})",
      accent = Color(0xFF00F5D4),
      trailingLabel = "LFG PING",
      trailingTag = "squadron_bay_lfg_ping",
      onTrailingClick = onBroadcastLfg,
      onBackToHub = onBackToHub,
    )

    // Filterable Bar: ALL • SYNDICATE • FRIENDS + Sort Toggle (RESONANCE / ACTIVITY) (>= 48dp touch targets)
    Row(
      modifier =
        Modifier.fillMaxWidth()
          .height(48.dp)
          .chamferedSciFiPodSurface(
            isRightSide = false,
            primaryBorder = Color(0xFF38BDF8),
            secondaryBorder = Color(0xFF00F5D4),
          )
          .padding(horizontal = 4.dp),
      horizontalArrangement = Arrangement.spacedBy(3.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      SquadronPilotFilter.entries.forEach { tab ->
        val selected = filter == tab
        val label =
          when (tab) {
            SquadronPilotFilter.ALL -> "ALL"
            SquadronPilotFilter.SYNDICATE -> if (isTabletDeck) "SYNDICATE" else "SYN"
            SquadronPilotFilter.FRIENDS -> if (isTabletDeck) "FRIENDS" else "FRND"
          }
        Box(
          modifier =
            Modifier.weight(1f)
              .fillMaxHeight()
              .minimumInteractiveComponentSize()
              .clickable { onSelectFilter(tab) }
              .padding(vertical = 5.dp)
              .testTag("squadron_filter_${tab.name.lowercase(Locale.US)}"),
          contentAlignment = Alignment.Center,
        ) {
          Box(
            modifier =
              Modifier.fillMaxSize()
                .clip(RoundedCornerShape(6.dp))
                .background(
                  if (selected) Color(0x3800F5D4) else Color(0x6602050B)
                )
                .border(
                  1.dp,
                  if (selected) Color(0xFF00F5D4) else Color(0x4438BDF8),
                  RoundedCornerShape(6.dp),
                ),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text = label,
              color = if (selected) Color.White else Color(0xFF94A3B8),
              fontSize = if (isTabletDeck) 8.4.sp else 7.4.sp,
              fontWeight = FontWeight.Black,
              fontFamily = FontFamily.Monospace,
              maxLines = 1,
            )
          }
        }
      }

      Box(
        modifier =
          Modifier.weight(1.05f)
            .fillMaxHeight()
            .minimumInteractiveComponentSize()
            .clickable { onToggleSort() }
            .padding(vertical = 5.dp)
            .testTag("squadron_sort_toggle"),
        contentAlignment = Alignment.Center,
      ) {
        Box(
          modifier =
            Modifier.fillMaxSize()
              .clip(RoundedCornerShape(6.dp))
              .background(Color(0x33FFD700))
              .border(1.dp, Color(0xFFFFD700), RoundedCornerShape(6.dp)),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text =
              when (sortMode) {
                SquadronPilotSort.RESONANCE -> if (isTabletDeck) "SORT: TOP" else "TOP v"
                SquadronPilotSort.ACTIVITY -> if (isTabletDeck) "SORT: LIVE" else "LIVE v"
              },
            color = Color(0xFFFFD700),
            fontSize = if (isTabletDeck) 8.8.sp else 7.8.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
          )
        }
      }
    }

    if (!squadronStatusBanner.isNullOrBlank()) {
      Text(
        text = squadronStatusBanner,
        color = Color(0xFF00F5D4),
        fontSize = 7.8.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(horizontal = 4.dp),
      )
    }

    LazyColumn(
      modifier =
        Modifier.weight(1f)
          .fillMaxWidth()
          .chamferedSciFiPodSurface(
            isRightSide = false,
            primaryBorder = Color(0xFF091528),
            secondaryBorder = Color(0xFF38BDF8),
          )
          .padding(6.dp),
      verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
      if (pilots.isEmpty()) {
        item(key = "empty_squadron_pilots") {
          Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            Text(
              text =
                when (filter) {
                  SquadronPilotFilter.ALL -> "NO OTHER ONLINE PILOTS DETECTED"
                  SquadronPilotFilter.SYNDICATE -> "NO SYNDICATE MATES ONLINE"
                  SquadronPilotFilter.FRIENDS -> "NO VERIFIED FRIENDS ONLINE"
                },
              color = Color(0xFF94A3B8),
              fontSize = 8.4.sp,
              fontWeight = FontWeight.Black,
              fontFamily = FontFamily.Monospace,
              textAlign = TextAlign.Center,
            )
            Text(
              text = "Tap LFG PING above to broadcast a Squadron invite across active sectors.",
              color = Color(0xFF64748B),
              fontSize = 7.8.sp,
              fontFamily = FontFamily.SansSerif,
              textAlign = TextAlign.Center,
            )
          }
        }
      }

      items(pilots, key = { it.uid }) { pilot ->
        val isSelected = pilot.uid == selectedPilotUid
        val bestShipBmp =
          remember(pilot.bestShipId, spriteRev, customRev) {
            RiftSpriteBaker.getShipSprite(shipId = pilot.bestShipId, isAllied = true).asImageBitmap()
          }
        val displayIdentity =
          if (pilot.syndicateTag.isNotBlank()) "[${pilot.syndicateTag}] ${pilot.callsign}"
          else pilot.callsign

        Row(
          modifier =
            Modifier.fillMaxWidth()
              .minimumInteractiveComponentSize()
              .heightIn(min = 50.dp)
              .clip(RoundedCornerShape(8.dp))
              .background(
                if (isSelected) {
                  Brush.horizontalGradient(listOf(Color(0xFF0E2A47), Color(0xFF091528)))
                } else {
                  Brush.horizontalGradient(listOf(Color(0xFF091528), Color(0xFF030711)))
                }
              )
              .border(
                if (isSelected) 1.3.dp else 0.9.dp,
                if (isSelected) Color(0xFF00F5D4) else Color(0x4438BDF8),
                RoundedCornerShape(8.dp),
              )
              .clickable { onTapPilot(pilot) }
              .padding(horizontal = 7.dp, vertical = 5.dp)
              .testTag("squadron_pilot_row_${pilot.uid.take(16)}"),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Box(
            modifier =
              Modifier.size(if (isTabletDeck) 40.dp else 34.dp)
                .clip(CircleShape)
                .background(Color(0xFF02050B))
                .border(1.dp, Color(0xFFFFD700), CircleShape),
            contentAlignment = Alignment.Center,
          ) {
            Image(
              bitmap = bestShipBmp,
              contentDescription = "${pilot.callsign} best ship",
              modifier = Modifier.fillMaxSize(0.80f),
              contentScale = ContentScale.Fit,
            )
          }

          Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
          ) {
            Text(
              text = displayIdentity,
              color = Color.White,
              fontSize = if (isTabletDeck) 9.8.sp else 8.6.sp,
              fontWeight = FontWeight.Black,
              fontFamily = FontFamily.Monospace,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
            )
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              ResonanceCrestIcon(sizeDp = 11.dp)
              Text(
                text = "${pilot.resonance} • ${pilot.sectorLabel}",
                color = Color(0xFFFFD700),
                fontSize = 8.4.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }

          if (pilot.isInSquadron) {
            Text(
              text = "SQUAD",
              color = Color(0xFF00F5D4),
              fontSize = 7.4.sp,
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
private fun SquadronRightPlayfieldShipBay(
  isTabletDeck: Boolean,
  headerHeight: Dp,
  wingStackGap: Dp,
  localCallsign: String,
  localSyndicateTag: String,
  localShipId: Int,
  localShipResonance: Int,
  isLocalShipCharged: Boolean,
  inspectedPilot: SquadronOnlinePilotItem?,
  armadaName: String,
  armadaSlots: List<ArmadaMemberSlot>,
  shipSpinDeg: Float,
  fluxOrbitRad: Float,
  onInspectPilotPrompt: (SquadronOnlinePilotItem) -> Unit,
  onOpenFullSquadronModal: () -> Unit,
) {
  val targetShipId = (inspectedPilot?.lockedShipId ?: localShipId).coerceIn(1, 10)
  val targetResonance = inspectedPilot?.resonance ?: localShipResonance
  val targetName =
    if (inspectedPilot != null) {
      if (inspectedPilot.syndicateTag.isNotBlank()) "[${inspectedPilot.syndicateTag}] ${inspectedPilot.callsign}"
      else inspectedPilot.callsign
    } else {
      if (localSyndicateTag.isNotBlank()) "[$localSyndicateTag] $localCallsign" else localCallsign
    }
  val targetShipSpec = remember(targetShipId, RiftSpriteBaker.customShip9Version) { RiftCodex.getShipById(targetShipId) }
  val cleanShipTitle = remember(targetShipSpec.name) { targetShipSpec.name.replace(Regex("^[0-9]+-"), "").trim().uppercase(Locale.US) }
  val spriteRev = RiftSpriteBaker.spriteVisualRevision
  val customRev = RiftSpriteBaker.customShip9Version
  val trimStyle = RiftSpriteBaker.activeHullTrimStyle
  val lockedShipBmp =
    remember(targetShipId, spriteRev, customRev, trimStyle) {
      RiftSpriteBaker.getShipSprite(shipId = targetShipId, isAllied = true).asImageBitmap()
    }

  Column(
    modifier = Modifier.fillMaxSize(),
    verticalArrangement = Arrangement.spacedBy(wingStackGap),
  ) {
    HubDigitizedReturnBar(
      heightDp = headerHeight,
      isRightSide = true,
      isTabletDeck = isTabletDeck,
      sectionTitle = if (armadaName.isNotBlank()) "SQUAD [$armadaName]" else "LOCKED HULL",
      accent = Color(0xFF38BDF8),
      trailingLabel = "ROSTER >",
      trailingTag = "squadron_bay_open_full_roster",
      onTrailingClick = onOpenFullSquadronModal,
    )

    Column(
      modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(wingStackGap),
    ) {
      // Playfield-Style 2.5D Locked-In Ship Stage: Ship Resonance ABOVE, 2.5D Ship Graphic CENTER, Pilot Name BELOW
      Box(
        modifier =
          Modifier.fillMaxWidth()
            .heightIn(min = if (isTabletDeck) 216.dp else 172.dp)
            .chamferedSciFiPodSurface(
              isRightSide = true,
              primaryBorder = Color(0xFF00F5D4),
              secondaryBorder = Color(0xFFFFD700),
            )
            .clickable(enabled = inspectedPilot != null) {
              if (inspectedPilot != null) onInspectPilotPrompt(inspectedPilot)
            }
            .padding(10.dp)
            .testTag("squadron_playfield_ship_stage"),
        contentAlignment = Alignment.Center,
      ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
          val cx = size.width * 0.5f
          val cy = size.height * 0.5f
          val ringR = min(size.width, size.height) * 0.34f
          drawCircle(
            brush =
              Brush.radialGradient(
                colors = listOf(Color(0x2800F5D4), Color(0x10091528), Color.Transparent),
                center = Offset(cx, cy),
                radius = ringR * 1.35f,
              ),
            radius = ringR * 1.35f,
            center = Offset(cx, cy),
          )
          drawCircle(
            color = if (inspectedPilot?.isInSquadron == true || inspectedPilot == null) Color(0xAA00F5D4) else Color(0xAA38BDF8),
            radius = ringR,
            center = Offset(cx, cy),
            style = Stroke(width = 1.6.dp.toPx()),
          )
          drawCircle(
            color = Color(0x44FFD700),
            radius = ringR * 0.76f,
            center = Offset(cx, cy),
            style = Stroke(width = 0.9.dp.toPx()),
          )
        }

        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(if (isTabletDeck) 8.dp else 5.dp),
        ) {
          Row(
            modifier =
              Modifier.clip(RoundedCornerShape(8.dp))
                .background(Color(0xD902050B))
                .border(1.dp, Color(0xAAFFD700), RoundedCornerShape(8.dp))
                .padding(horizontal = 9.dp, vertical = 3.5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            ResonanceCrestIcon(sizeDp = if (isTabletDeck) 15.dp else 13.dp)
            Text(
              text = String.format(Locale.US, "%,d", targetResonance),
              color = Color(0xFFFFD700),
              fontSize = if (isTabletDeck) 12.0.sp else 10.4.sp,
              fontWeight = FontWeight.Black,
              fontFamily = FontFamily.Monospace,
            )
          }

          if (inspectedPilot == null && isLocalShipCharged) {
            Spinning25dShipWithOrbitingFlux(
              shipId = targetShipId,
              shipSpinDeg = shipSpinDeg,
              fluxOrbitRad = fluxOrbitRad,
              isCharged = true,
              sizeDp = if (isTabletDeck) 104.dp else 78.dp,
            )
          } else {
            Image(
              bitmap = lockedShipBmp,
              contentDescription = cleanShipTitle,
              contentScale = ContentScale.Fit,
              modifier =
                Modifier.size(if (isTabletDeck) 98.dp else 74.dp)
                  .graphicsLayer { rotationZ = shipSpinDeg * 0.35f },
            )
          }

          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
              text = targetName,
              color = Color.White,
              fontSize = if (isTabletDeck) 11.4.sp else 10.0.sp,
              fontWeight = FontWeight.Black,
              fontFamily = FontFamily.Monospace,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Text(
              text = "LOCKED // $cleanShipTitle",
              color = Color(0xFF00F5D4),
              fontSize = if (isTabletDeck) 8.8.sp else 7.8.sp,
              fontWeight = FontWeight.ExtraBold,
              fontFamily = FontFamily.Monospace,
              maxLines = 1,
            )
          }
        }
      }

      // Active Squadron Slot Strip (1..5/5)
      Row(
        modifier =
          Modifier.fillMaxWidth()
            .height(headerHeight)
            .chamferedSciFiPodSurface(
              isRightSide = true,
              primaryBorder = Color(0xFF00F5D4),
              secondaryBorder = Color(0xFF38BDF8),
            )
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Text(
          text = "SQUADRON ${armadaSlots.size.coerceAtLeast(1)}/5",
          color = Color(0xFF00F5D4),
          fontSize = if (isTabletDeck) 9.4.sp else 8.2.sp,
          fontWeight = FontWeight.Black,
          fontFamily = FontFamily.Monospace,
        )
        Text(
          text =
            if (inspectedPilot != null) "TAP SHIP FOR SQUAD UP"
            else "SELECT PILOT ON LEFT",
          color = Color(0xFFFACC15),
          fontSize = if (isTabletDeck) 8.4.sp else 7.4.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.Monospace,
          maxLines = 1,
        )
      }
    }
  }
}

@Composable
private fun GameModesLeftRankedBay(
  isTabletDeck: Boolean,
  headerHeight: Dp,
  wingStackGap: Dp,
  activeRankedMode: RankedGameMode,
  skirmishBracket: Int,
  onSelectSkirmishBracket: (Int) -> Unit,
  riftControlBracket: Int,
  onSelectRiftControlBracket: (Int) -> Unit,
  syndicateWarBracket: Int,
  onSelectSyndicateWarBracket: (Int) -> Unit,
  humanPilotCountByMode: (RankedGameMode) -> Int,
  canQueueSyndicateWars: Pair<Boolean, String>,
  overwatchInstances: List<OverwatchRiftInstance>,
  onSelectOverwatchKey: (String) -> Unit,
  onBackToHub: () -> Unit,
  onInspectMode: (RankedGameMode, Int, Boolean) -> Unit = { _, _, _ -> },
  onDeployMode: (RankedGameMode, Int) -> Unit,
) {
  val orderedModes = remember {
    listOf(RankedGameMode.SECTOR_CONVERGENCE) +
      RankedGameMode.entries.filter { it != RankedGameMode.SECTOR_CONVERGENCE }
  }
  Column(
    modifier = Modifier.fillMaxSize().testTag("game_modes_left_ranked_bay"),
    verticalArrangement = Arrangement.spacedBy(wingStackGap),
  ) {
    HubDigitizedReturnBar(
      heightDp = headerHeight,
      isRightSide = false,
      isTabletDeck = isTabletDeck,
      sectionTitle = "6 RANKED MODES",
      accent = Color(0xFF00F5D4),
      onBackToHub = onBackToHub,
    )

    Column(
      modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(wingStackGap),
    ) {
      orderedModes.forEach { mode ->
        val isActive = activeRankedMode == mode
        val humans = humanPilotCountByMode(mode).coerceAtLeast(0)
        val bracketOptions: List<Int> =
          when (mode) {
            RankedGameMode.RIFT_SKIRMISH -> listOf(2, 3, 4, 5)
            RankedGameMode.RIFT_CONTROL, RankedGameMode.SYNDICATE_WARS -> listOf(3, 5, 10)
            else -> emptyList()
          }
        val activeBracket =
          when (mode) {
            RankedGameMode.RIFT_SKIRMISH -> skirmishBracket
            RankedGameMode.RIFT_CONTROL -> riftControlBracket
            RankedGameMode.SYNDICATE_WARS -> syndicateWarBracket
            RankedGameMode.STASIS_BREACH -> 10
            else -> 20
          }
        val canDeploy = mode != RankedGameMode.SYNDICATE_WARS || canQueueSyndicateWars.first

        val isOverwatchEligible =
          mode == RankedGameMode.SECTOR_CONVERGENCE || mode == RankedGameMode.FLUX_CORE

        Column(
          modifier =
            Modifier.fillMaxWidth()
              .chamferedSciFiPodSurface(
                isRightSide = false,
                primaryBorder = if (isActive) Color(0xFF00F5D4) else Color(0xFF38BDF8),
                secondaryBorder = if (isActive) Color(0xFFFFD700) else Color(0xFF091528),
              )
              .clickable {
                if (isOverwatchEligible) {
                  overwatchInstances
                    .firstOrNull { it.customConfig == null && it.rankedMode == mode }
                    ?.let { onSelectOverwatchKey(it.key) }
                }
                onInspectMode(mode, activeBracket, canDeploy)
              }
              .padding(horizontal = 8.dp, vertical = 6.dp)
              .testTag("game_modes_card_${mode.name.lowercase(Locale.US)}"),
          verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.weight(1f).padding(end = 6.dp)) {
              Text(
                text = mode.title.uppercase(Locale.US),
                color = Color.White,
                fontSize = if (isTabletDeck) 10.6.sp else 9.4.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              val subLine =
                when {
                  mode == RankedGameMode.SYNDICATE_WARS && !canDeploy -> canQueueSyndicateWars.second
                  isOverwatchEligible -> "${humans}P LIVE • TAP FOR INTEL & PREVIEW"
                  else -> "${humans}P LIVE • TAP FOR RULES • <=15S QUEUE"
                }
              Text(
                text = subLine,
                color = if (!canDeploy) Color(0xFFFB7185) else Color(0xFF38BDF8),
                fontSize = if (isTabletDeck) 8.8.sp else 8.0.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }

            Box(
              modifier =
                Modifier.minimumInteractiveComponentSize()
                  .height(48.dp)
                  .clickable(enabled = canDeploy) { onDeployMode(mode, activeBracket) }
                  .padding(vertical = 6.dp)
                  .testTag("game_modes_deploy_${mode.name.lowercase(Locale.US)}"),
              contentAlignment = Alignment.Center,
            ) {
              Box(
                modifier =
                  Modifier.clip(RoundedCornerShape(6.dp))
                    .background(
                      Brush.verticalGradient(
                        if (canDeploy) listOf(Color(0xFF0D2D46), Color(0xFF061626))
                        else listOf(Color(0xFF1E293B), Color(0xFF0F172A))
                      )
                    )
                    .border(
                      1.dp,
                      if (canDeploy) Color(0xFF00F5D4) else Color(0xFF475569),
                      RoundedCornerShape(6.dp),
                    )
                    .padding(horizontal = 9.dp, vertical = 5.dp),
                contentAlignment = Alignment.Center,
              ) {
                Text(
                  text = if (canDeploy) "RIFT IN >" else "LOCKED",
                  color = if (canDeploy) Color.White else Color(0xFF64748B),
                  fontSize = 8.8.sp,
                  fontWeight = FontWeight.Black,
                  fontFamily = FontFamily.Monospace,
                  maxLines = 1,
                )
              }
            }
          }

          if (bracketOptions.isNotEmpty()) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(4.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              bracketOptions.forEach { b ->
                val sel = activeBracket == b
                Box(
                  modifier =
                    Modifier.minimumInteractiveComponentSize()
                      .clickable {
                        when (mode) {
                          RankedGameMode.RIFT_SKIRMISH -> onSelectSkirmishBracket(b)
                          RankedGameMode.RIFT_CONTROL -> onSelectRiftControlBracket(b)
                          RankedGameMode.SYNDICATE_WARS -> onSelectSyndicateWarBracket(b)
                          else -> {}
                        }
                      }
                      .padding(vertical = 3.dp)
                      .testTag("game_modes_bracket_${mode.name.lowercase(Locale.US)}_${b}v${b}"),
                  contentAlignment = Alignment.Center,
                ) {
                  Box(
                    modifier =
                      Modifier.clip(RoundedCornerShape(5.dp))
                        .background(if (sel) Color(0x3800F5D4) else Color(0x6602050B))
                        .border(
                          1.dp,
                          if (sel) Color(0xFF00F5D4) else Color(0xFF334155),
                          RoundedCornerShape(5.dp),
                        )
                        .padding(horizontal = 7.dp, vertical = 3.5.dp),
                  ) {
                    Text(
                      text = "${b}v${b}",
                      color = if (sel) Color.White else Color(0xFF94A3B8),
                      fontSize = 7.6.sp,
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

@Composable
private fun GameModesRightCustomBay(
  isTabletDeck: Boolean,
  headerHeight: Dp,
  wingStackGap: Dp,
  chargedShipId: Int,
  isShipCharged: Boolean,
  shipSpinDeg: Float,
  fluxOrbitRad: Float,
  onSwitchToHangarBay: () -> Unit,
  onOpenFullCustomGate: () -> Unit,
  publicCustomGames: List<CustomGameConfig>,
  engineCustomGames: List<CustomGameConfig>,
  unlockedPrivateCodes: List<String>,
  customIsPrivateRoom: Boolean,
  onTogglePrivateRoom: (Boolean) -> Unit,
  generatedRoomCode: String,
  joinRoomCodeDraft: String,
  onJoinRoomCodeDraftChange: (String) -> Unit,
  roomCodeBanner: String?,
  onJoinByRoomCode: () -> Unit,
  customBaseMode: RankedGameMode,
  onSelectCustomBaseMode: (RankedGameMode) -> Unit,
  customDamageTakenScalePct: Int = 100,
  onSelectCustomDamageTakenScalePct: (Int) -> Unit = {},
  customWeaponFluxCostScalePct: Int = 100,
  onSelectCustomWeaponFluxCostScalePct: (Int) -> Unit = {},
  onJoinCustomInstance: (String) -> Unit,
  onQuickCreateCustomGame: () -> Unit,
) {
  val shipSpec = remember(chargedShipId, RiftSpriteBaker.customShip9Version) {
    RiftCodex.getShipById(chargedShipId.coerceIn(1, 10))
  }
  val cleanHullName = remember(shipSpec.name) {
    shipSpec.name.replace(Regex("^[0-9]+-"), "").trim().uppercase(Locale.US)
  }
  val visibleCustomRooms =
    remember(publicCustomGames, engineCustomGames, unlockedPrivateCodes) {
      (publicCustomGames + engineCustomGames)
        .distinctBy { it.instanceId.uppercase(Locale.US) }
        .filter { g ->
          !isPrivateCustomGame(g) ||
            extractCustomRoomCode(g).uppercase(Locale.US) in unlockedPrivateCodes
        }
    }

  Column(
    modifier = Modifier.fillMaxSize().testTag("game_modes_right_custom_bay"),
    verticalArrangement = Arrangement.spacedBy(wingStackGap),
  ) {
    HubDigitizedReturnBar(
      heightDp = headerHeight,
      isRightSide = true,
      isTabletDeck = isTabletDeck,
      sectionTitle = "CUSTOM RIFTS",
      accent = Color(0xFF38BDF8),
      trailingLabel = "10-RULE LAB >",
      trailingTag = "game_modes_open_full_custom",
      onTrailingClick = onOpenFullCustomGate,
    )

    Column(
      modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(wingStackGap),
    ) {
      // Charged Hull Quick-Switch Strip
      Row(
        modifier =
          Modifier.fillMaxWidth()
            .height(48.dp)
            .chamferedSciFiPodSurface(
              isRightSide = true,
              primaryBorder = if (isShipCharged) Color(0xFF00F5D4) else Color(0xFFFFD700),
              secondaryBorder = Color(0xFF38BDF8),
            )
            .clickable { onSwitchToHangarBay() }
            .padding(horizontal = 7.dp)
            .testTag("game_modes_charged_hull_bar"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          modifier = Modifier.weight(1f),
        ) {
          Spinning25dShipWithOrbitingFlux(
            shipId = chargedShipId,
            shipSpinDeg = shipSpinDeg,
            fluxOrbitRad = fluxOrbitRad,
            isCharged = isShipCharged,
            isBestShip = !isShipCharged,
            sizeDp = 36.dp,
          )
          Column {
            Text(
              text = if (isShipCharged) "CHARGED: $cleanHullName" else "DEFAULT: $cleanHullName",
              color = Color.White,
              fontSize = 8.4.sp,
              fontWeight = FontWeight.Black,
              fontFamily = FontFamily.Monospace,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Text(
              text = "TAP TO CHARGE ANOTHER HULL",
              color = Color(0xFF00F5D4),
              fontSize = 7.2.sp,
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.Monospace,
              maxLines = 1,
            )
          }
        }
        Text(
          text = "HANGAR >",
          color = Color(0xFFFACC15),
          fontSize = 8.0.sp,
          fontWeight = FontWeight.Black,
          fontFamily = FontFamily.Monospace,
        )
      }

      // 6-Char Private Join Code Entry
      Column(
        modifier =
          Modifier.fillMaxWidth()
            .chamferedSciFiPodSurface(
              isRightSide = true,
              primaryBorder = Color(0xFFFACC15),
              secondaryBorder = Color(0xFF38BDF8),
            )
            .padding(horizontal = 7.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
          Box(
            modifier =
              Modifier.weight(1f)
                .height(34.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF02050B))
                .border(1.dp, Color(0xFF38BDF8), RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.CenterStart,
          ) {
            if (joinRoomCodeDraft.isEmpty()) {
              Text(
                text = "ENTER 6-CHAR JOIN CODE",
                color = Color(0xFF64748B),
                fontSize = 8.0.sp,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
              )
            }
            BasicTextField(
              value = joinRoomCodeDraft,
              onValueChange = { raw ->
                onJoinRoomCodeDraftChange(
                  raw.uppercase(Locale.US).filter { it.isLetterOrDigit() }.take(6)
                )
              },
              singleLine = true,
              cursorBrush = SolidColor(Color(0xFF00F5D4)),
              textStyle =
                TextStyle(
                  color = Color.White,
                  fontSize = 9.2.sp,
                  fontWeight = FontWeight.Black,
                  fontFamily = FontFamily.Monospace,
                ),
              keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
              keyboardActions = KeyboardActions(onDone = { onJoinByRoomCode() }),
              modifier = Modifier.fillMaxWidth().testTag("hub_custom_room_code_input"),
            )
          }
          Box(
            modifier =
              Modifier.minimumInteractiveComponentSize()
                .height(48.dp)
                .clickable { onJoinByRoomCode() }
                .padding(vertical = 6.dp)
                .testTag("hub_btn_join_room_code"),
            contentAlignment = Alignment.Center,
          ) {
            Box(
              modifier =
                Modifier.clip(RoundedCornerShape(6.dp))
                  .background(Brush.verticalGradient(listOf(Color(0xFF282008), Color(0xFF141004))))
                  .border(1.dp, Color(0xFFFACC15), RoundedCornerShape(6.dp))
                  .padding(horizontal = 8.dp, vertical = 5.dp),
            ) {
              Text(
                text = "JOIN CODE",
                color = Color(0xFFFFD700),
                fontSize = 7.8.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
              )
            }
          }
        }
        if (!roomCodeBanner.isNullOrBlank()) {
          Text(
            text = roomCodeBanner,
            color = Color(0xFF00F5D4),
            fontSize = 7.2.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }

      // Public Custom Game List
      Column(
        modifier =
          Modifier.fillMaxWidth()
            .chamferedSciFiPodSurface(
              isRightSide = true,
              primaryBorder = Color(0xFF38BDF8),
              secondaryBorder = Color(0xFF091528),
            )
            .padding(6.dp)
            .testTag("hub_public_custom_game_list"),
        verticalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        if (visibleCustomRooms.isEmpty()) {
          Text(
            text = "NO PUBLIC CUSTOM RIFTS OPEN • CREATE ONE BELOW OR ENTER A JOIN CODE",
            color = Color(0xFF94A3B8),
            fontSize = 7.8.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(4.dp),
          )
        } else {
          visibleCustomRooms.forEach { game ->
            val code = extractCustomRoomCode(game)
            val priv = isPrivateCustomGame(game)
            Row(
              modifier =
                Modifier.fillMaxWidth()
                  .clip(RoundedCornerShape(6.dp))
                  .background(Color(0xCC060D1A))
                  .border(
                    1.dp,
                    if (priv) Color(0xFFFACC15) else Color(0xFF334155),
                    RoundedCornerShape(6.dp),
                  )
                  .padding(horizontal = 6.dp, vertical = 3.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column(modifier = Modifier.weight(1f).padding(end = 5.dp)) {
                Text(
                  text = "${if (priv) "[PRIV] " else ""}${game.title}",
                  color = Color.White,
                  fontSize = 8.0.sp,
                  fontWeight = FontWeight.Black,
                  fontFamily = FontFamily.Monospace,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
                val scaleSuffix =
                  if (game.damageTakenScalePct != 100 || game.weaponFluxCostScalePct != 100)
                    " • DMG ${game.damageTakenScalePct}% • FLUX ${game.weaponFluxCostScalePct}%"
                  else ""
                val botLabel =
                  if (game.botsEnabled && game.botCount > 0) " • ${game.botCount}B" else " • 0B"
                Text(
                  text = "${game.baseMode.title} • CODE $code$botLabel$scaleSuffix",
                  color = Color(0xFF38BDF8),
                  fontSize = 7.2.sp,
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
                    .testTag("hub_join_custom_${game.instanceId}"),
                contentAlignment = Alignment.Center,
              ) {
                Box(
                  modifier =
                    Modifier.clip(RoundedCornerShape(5.dp))
                      .background(Color(0xFF0D2D46))
                      .border(1.dp, Color(0xFF00F5D4), RoundedCornerShape(5.dp))
                      .padding(horizontal = 7.dp, vertical = 4.dp),
                ) {
                  Text(
                    text = "JOIN >",
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
      }

      // 5% Increment Custom Sandbox Scales (Damage Taken & Weapon Flux Cost: 0%..100%, Default 100%)
      Column(
        modifier =
          Modifier.fillMaxWidth()
            .chamferedSciFiPodSurface(
              isRightSide = true,
              primaryBorder = Color(0xFF38BDF8),
              secondaryBorder = Color(0xFF00F5D4),
            )
            .padding(horizontal = 6.dp, vertical = 2.dp)
            .testTag("custom_sandbox_5pct_scales_pod"),
        verticalArrangement = Arrangement.spacedBy(1.dp),
      ) {
        HubCustomFivePercentStepperRow(
          label = "DMG TAKEN",
          valuePct = customDamageTakenScalePct,
          minusTag = "hub_custom_damage_taken_minus",
          valueTag = "hub_custom_damage_taken_value",
          plusTag = "hub_custom_damage_taken_plus",
          onMinus = { onSelectCustomDamageTakenScalePct(customDamageTakenScalePct - 5) },
          onPlus = { onSelectCustomDamageTakenScalePct(customDamageTakenScalePct + 5) },
        )
        HubCustomFivePercentStepperRow(
          label = "FLUX COST",
          valuePct = customWeaponFluxCostScalePct,
          minusTag = "hub_custom_weapon_flux_minus",
          valueTag = "hub_custom_weapon_flux_value",
          plusTag = "hub_custom_weapon_flux_plus",
          onMinus = { onSelectCustomWeaponFluxCostScalePct(customWeaponFluxCostScalePct - 5) },
          onPlus = { onSelectCustomWeaponFluxCostScalePct(customWeaponFluxCostScalePct + 5) },
        )
      }

      // Quick Custom Rift Creator Pod
      Row(
        modifier =
          Modifier.fillMaxWidth()
            .height(48.dp)
            .chamferedSciFiPodSurface(
              isRightSide = true,
              primaryBorder = Color(0xFF00F5D4),
              secondaryBorder = Color(0xFFFACC15),
            )
            .padding(horizontal = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Box(
          modifier =
            Modifier.minimumInteractiveComponentSize()
              .fillMaxHeight()
              .clickable { onTogglePrivateRoom(!customIsPrivateRoom) }
              .padding(horizontal = 4.dp)
              .testTag("hub_custom_toggle_visibility"),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = if (customIsPrivateRoom) "PRIV [$generatedRoomCode]" else "PUBLIC",
            color = if (customIsPrivateRoom) Color(0xFFFACC15) else Color(0xFF00F5D4),
            fontSize = 7.8.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
          )
        }

        Box(
          modifier =
            Modifier.minimumInteractiveComponentSize()
              .fillMaxHeight()
              .clickable {
                val modes =
                  listOf(
                    RankedGameMode.SECTOR_CONVERGENCE,
                    RankedGameMode.RIFT_SKIRMISH,
                    RankedGameMode.FLUX_CORE,
                    RankedGameMode.STASIS_BREACH,
                    RankedGameMode.RIFT_CONTROL,
                  )
                val nextIdx = (modes.indexOf(customBaseMode) + 1) % modes.size
                onSelectCustomBaseMode(modes[nextIdx])
              }
              .padding(horizontal = 4.dp)
              .testTag("hub_custom_cycle_base_mode"),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = customBaseMode.title.uppercase(Locale.US).take(11),
            color = Color(0xFFE2E8F0),
            fontSize = 7.6.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
          )
        }

        Box(
          modifier =
            Modifier.minimumInteractiveComponentSize()
              .fillMaxHeight()
              .clickable { onQuickCreateCustomGame() }
              .padding(vertical = 6.dp)
              .testTag("hub_btn_quick_create_custom"),
          contentAlignment = Alignment.Center,
        ) {
          Box(
            modifier =
              Modifier.clip(RoundedCornerShape(6.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFF0D2D46), Color(0xFF061626))))
                .border(1.dp, Color(0xFF00F5D4), RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp, vertical = 5.dp),
          ) {
            Text(
              text = "CREATE RIFT >",
              color = Color.White,
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
}

/**
 * Renders the four open-space corner regions outside the curved top-to-bottom Overwatch circle:
 * 1. Top-Left (between Port Wing & Overwatch): Simple up chevron above (`nexus_overwatch_prev`),
 *    Option #1 2-line tappable sensor readout (`nexus_overwatch_terminal_readout` -> opens `OVERWATCH_INSPECT_BAY`):
 *      - Line 1: Active match name (`SECTOR CONVERGENCE`, `FLUX CORE`, or player's Custom Rift title)
 *      - Line 2: `MM:SS LEFT · [ResonanceCrestIcon] <value> · <real player count>` (+ `[1x]` reset when zoomed)
 *    and simple down chevron below (`nexus_overwatch_next`).
 * 2. Top-Right (opposite top-left terminal readout): Primary `RIFT IN >` launch trigger (`nexus_overwatch_rift_in`)
 *    plus compact `ACTIVE RIFTS` actuator (`overwatch_btn_active_rifts`).
 * 3. Bottom-Left: Contextual `CANCEL` / `< RIFT HUB` return trigger.
 * 4. Bottom-Right: Contextual `CHARGE [Flux icon]` / `SQUAD UP` / `CONFIRM` trigger.
 */
@Composable
private fun BoxScope.OverwatchOpenSpaceCornerOverlays(
  openCornerAnchorDp: Dp,
  openCornerMaxWidthDp: Dp,
  isTabletDeck: Boolean,
  isFocused: Boolean,
  overwatchInstances: List<OverwatchRiftInstance>,
  activeInstance: OverwatchRiftInstance,
  @Suppress("UNUSED_PARAMETER") favoriteOverwatchKey: String = "",
  overwatchZoom: Float,
  liveRemainingSec: Int,
  liveAvgResonance: Int,
  sideBayMode: RiftHubSideBayMode,
  overwatchPrompt: OverwatchCircularPrompt,
  chargedShipId: Int,
  @Suppress("UNUSED_PARAMETER") renameDraft: String,
  density: androidx.compose.ui.unit.Density,
  onSelectOverwatchKey: (String) -> Unit,
  @Suppress("UNUSED_PARAMETER") onToggleFavoriteOverwatchKey: (String) -> Unit = {},
  onResetOverwatchCamera: () -> Unit,
  onQuickZoneIntoOverwatch: (OverwatchRiftInstance) -> Unit,
  onToggleOverwatchInspectBay: () -> Unit = {},
  onToggleAllModesBay: () -> Unit,
  onBottomLeftBackOrCancel: () -> Unit,
  onBottomRightConfirmOrCharge: () -> Unit,
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
    // Simple Up Chevron Above (>= 48dp touch target)
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

    // Option #1 2-Line Tappable Sensor Readout (tap opens Match & Pilot Inspector side-bays)
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
          fontSize = if (isTabletDeck) 10.2.sp else 8.8.sp,
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
          fontSize = if (isTabletDeck) 8.2.sp else 7.4.sp,
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
          fontSize = if (isTabletDeck) 8.8.sp else 7.6.sp,
          fontWeight = FontWeight.ExtraBold,
          fontFamily = FontFamily.Monospace,
          maxLines = 1,
          style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
        )
        ResonanceCrestIcon(sizeDp = if (isTabletDeck) 11.dp else 9.5.dp)
        Text(
          text = "${formatOverwatchResonance(liveAvgResonance)} ·",
          color = Color(0xFFFFD700),
          fontSize = if (isTabletDeck) 8.8.sp else 7.6.sp,
          fontWeight = FontWeight.Black,
          fontFamily = FontFamily.Monospace,
          maxLines = 1,
          style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
        )
        Text(
          text = if (humanCount == 1) "1 PILOT" else "$humanCount PILOTS",
          color = if (humanCount > 0) Color(0xFF38BDF8) else Color(0xFF94A3B8),
          fontSize = if (isTabletDeck) 8.6.sp else 7.5.sp,
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
              fontSize = 7.4.sp,
              fontWeight = FontWeight.Black,
              fontFamily = FontFamily.Monospace,
              maxLines = 1,
            )
          }
        }
      }
    }

    // Simple Down Chevron Below (>= 48dp touch target)
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

  // 2. TOP-RIGHT OPEN SPACE: Primary [ RIFT IN > ] Launch Trigger + Compact [ ACTIVE RIFTS ]
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
        fontSize = if (isTabletDeck) 11.8.sp else 10.4.sp,
        fontWeight = FontWeight.Black,
        fontFamily = FontFamily.Monospace,
        letterSpacing = 0.8.sp,
        maxLines = 1,
        style = TextStyle(shadow = riftInGlow, platformStyle = PlatformTextStyle(includeFontPadding = false)),
      )
    }

    Box(
      modifier =
        Modifier.minimumInteractiveComponentSize()
          .height(48.dp)
          .clickable { onToggleAllModesBay() }
          .padding(vertical = 6.dp)
          .testTag("overwatch_btn_active_rifts"),
      contentAlignment = Alignment.CenterEnd,
    ) {
      val isModesOpen = sideBayMode == RiftHubSideBayMode.GAME_MODES_BAY
      Box(
        modifier =
          Modifier.clip(RoundedCornerShape(6.dp))
            .background(Color(0xD9060D1A))
            .border(1.dp, if (isModesOpen) Color(0xFF00F5D4) else Color(0xFF38BDF8), RoundedCornerShape(6.dp))
            .padding(horizontal = 9.dp, vertical = 5.dp),
      ) {
        Text(
          text = if (isModesOpen) "CLOSE MODES" else "ACTIVE RIFTS",
          color = if (isModesOpen) Color(0xFF00F5D4) else Color(0xFFE2E8F0),
          fontSize = if (isTabletDeck) 9.8.sp else 8.8.sp,
          fontWeight = FontWeight.Black,
          fontFamily = FontFamily.Monospace,
          maxLines = 1,
        )
      }
    }
  }

  // 3. LOWER-LEFT OPEN SPACE: Contextual Cancel ONLY when an Overwatch glass prompt is open (zero duplicate < RIFT HUB)
  val showBottomLeft = overwatchPrompt != OverwatchCircularPrompt.None
  if (showBottomLeft) {
    Box(
      modifier =
        Modifier.align(Alignment.BottomStart)
          .padding(start = openCornerAnchorDp, bottom = 3.dp)
          .minimumInteractiveComponentSize()
          .height(48.dp)
          .clickable { onBottomLeftBackOrCancel() }
          .padding(vertical = 5.dp)
          .testTag("overwatch_corner_back_cancel"),
      contentAlignment = Alignment.CenterStart,
    ) {
      Box(
        modifier =
          Modifier.clip(RoundedCornerShape(8.dp))
            .background(Color(0xE0060D1A))
            .border(1.1.dp, Color(0xFF64748B), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
      ) {
        Text(
          text = "CANCEL",
          color = Color(0xFFE2E8F0),
          fontSize = if (isTabletDeck) 9.8.sp else 8.8.sp,
          fontWeight = FontWeight.Black,
          fontFamily = FontFamily.Monospace,
          maxLines = 1,
        )
      }
    }
  }

  // 4. LOWER-RIGHT OPEN SPACE: Contextual Squad Up / Confirm / Save ONLY (zero duplicate CHARGE buttons)
  val showBottomRight =
    overwatchPrompt is OverwatchCircularPrompt.PilotInspect ||
      overwatchPrompt is OverwatchCircularPrompt.ActionConfirm ||
      overwatchPrompt is OverwatchCircularPrompt.CallsignRename
  if (showBottomRight) {
    val confirmFontSp = if (isTabletDeck) 10.4.sp else 9.2.sp
    Box(
      modifier =
        Modifier.align(Alignment.BottomEnd)
          .padding(end = openCornerAnchorDp, bottom = 3.dp)
          .minimumInteractiveComponentSize()
          .height(48.dp)
          .clickable { onBottomRightConfirmOrCharge() }
          .padding(vertical = 5.dp)
          .testTag("overwatch_corner_confirm_charge"),
      contentAlignment = Alignment.CenterEnd,
    ) {
      Row(
        modifier =
          Modifier.clip(RoundedCornerShape(8.dp))
            .background(Brush.horizontalGradient(listOf(Color(0xFF0D3554), Color(0xFF091528))))
            .border(
              1.25.dp,
              Brush.horizontalGradient(listOf(Color(0xFF00F5D4), Color(0xFFFFD700))),
              RoundedCornerShape(8.dp),
            )
            .padding(horizontal = 11.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
      ) {
        when (overwatchPrompt) {
          is OverwatchCircularPrompt.PilotInspect -> {
            Text(
              text = if (overwatchPrompt.pilot.isInSquadron) "IN SQUADRON" else "SQUAD UP",
              color = Color.White,
              fontSize = confirmFontSp,
              fontWeight = FontWeight.Black,
              fontFamily = FontFamily.Monospace,
              maxLines = 1,
            )
          }
          is OverwatchCircularPrompt.ActionConfirm -> {
            Text(
              text = overwatchPrompt.confirmLabel,
              color = Color.White,
              fontSize = confirmFontSp,
              fontWeight = FontWeight.Black,
              fontFamily = FontFamily.Monospace,
              maxLines = 1,
            )
          }
          is OverwatchCircularPrompt.CallsignRename -> {
            Text(
              text = "SAVE NAME",
              color = Color.White,
              fontSize = confirmFontSp,
              fontWeight = FontWeight.Black,
              fontFamily = FontFamily.Monospace,
              maxLines = 1,
            )
          }
          else -> Unit
        }
      }
    }
  }
}

/**
 * Signature Rift Hub Centerpiece — True Top-to-Bottom Edge-to-Edge Circular Overwatch Viewport:
 * Zero horizontal bars or vertical padding above/below the circle (`lensRadiusFraction = 0.476f`,
 * `1.0x .. 6.0x` pinch-to-zoom, drag pan, double-tap reset) framed by a sculpted circular 2.5D radar
 * bezel (`0.955r .. 1.044r`) with 72 calibrated degree ticks in solar gold (#FFD700) & electric cyan (#00F5D4),
 * subtle perimeter radial reticle lines, harmonic Resonance arcs, and top-left optical specular meniscus highlights.
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
  bestShipId: Int = 1,
  shipSpinDeg: Float = 0f,
  fluxOrbitRad: Float = 0f,
  renameDraft: String = "",
  onRenameDraftChange: (String) -> Unit = {},
  resolveShipResonance: (Int) -> Int = { 0 },
  onChargeShip: (Int) -> Unit = {},
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
            shipSpinDeg = shipSpinDeg,
            fluxOrbitRad = fluxOrbitRad,
            renameDraft = renameDraft,
            onRenameDraftChange = onRenameDraftChange,
            resolveShipResonance = resolveShipResonance,
            onChargeShip = onChargeShip,
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
  shipSpinDeg: Float,
  fluxOrbitRad: Float,
  renameDraft: String = "",
  onRenameDraftChange: (String) -> Unit = {},
  resolveShipResonance: (Int) -> Int,
  onChargeShip: (Int) -> Unit,
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
        .padding(all = if (isTabletDeck) 22.dp else 14.dp)
        .testTag("overwatch_circular_glass_prompt"),
    contentAlignment = Alignment.Center,
  ) {
    when (prompt) {
      OverwatchCircularPrompt.None -> Unit
      is OverwatchCircularPrompt.ShipInspect -> {
        val ship = remember(prompt.shipId, RiftSpriteBaker.customShip9Version) {
          RiftCodex.getShipById(prompt.shipId.coerceIn(1, 10))
        }
        val cleanName = remember(ship.name) {
          ship.name.replace(Regex("^[0-9]+-"), "").trim().uppercase(Locale.US)
        }
        val shipRes = resolveShipResonance(ship.id)
        val isAlreadyCharged = chargedShipId == ship.id
        val isBest = bestShipId == ship.id
        val chargeFontSp = if (isTabletDeck) 11.4.sp else 10.0.sp
        val fluxExactDp = with(density) { chargeFontSp.toDp() }

        Column(
          modifier = Modifier.fillMaxWidth(0.84f),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(if (isTabletDeck) 4.dp else 2.dp),
        ) {
          Spinning25dShipWithOrbitingFlux(
            shipId = ship.id,
            shipSpinDeg = shipSpinDeg,
            fluxOrbitRad = fluxOrbitRad,
            isCharged = isAlreadyCharged,
            isBestShip = isBest,
            isSelected = true,
            sizeDp = if (isTabletDeck) 66.dp else 46.dp,
          )

          Text(
            text = cleanName,
            color = Color.White,
            fontSize = if (isTabletDeck) 11.6.sp else 9.8.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
          )

          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            Text(
              text = ship.role.uppercase(Locale.US),
              color = Color(0xFF38BDF8),
              fontSize = if (isTabletDeck) 8.6.sp else 7.6.sp,
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.Monospace,
              maxLines = 1,
            )
            Text("•", color = Color(0xFF64748B), fontSize = 7.6.sp)
            ResonanceCrestIcon(sizeDp = 12.dp)
            Text(
              text = formatOverwatchResonance(shipRes),
              color = Color(0xFFFFD700),
              fontSize = if (isTabletDeck) 9.4.sp else 8.4.sp,
              fontWeight = FontWeight.Black,
              fontFamily = FontFamily.Monospace,
              maxLines = 1,
            )
          }

          Text(
            text = "${ship.primaryTitle} + ${ship.secondaryTitle}",
            color = Color(0xFFCBD5E1),
            fontSize = if (isTabletDeck) 8.2.sp else 7.2.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )

          // Compact 2x2 Hull Telemetry Bars
          Row(
            modifier = Modifier.fillMaxWidth(0.90f),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            CircularPromptStatBar(
              modifier = Modifier.weight(1f),
              label = "FLUX",
              fraction = (ship.maxFluxPts / 25f).coerceIn(0.18f, 1f),
              color = Color(0xFF00F5D4),
            )
            CircularPromptStatBar(
              modifier = Modifier.weight(1f),
              label = "RECH",
              fraction = (ship.rechargePts / 25f).coerceIn(0.18f, 1f),
              color = Color(0xFF38BDF8),
            )
          }
          Row(
            modifier = Modifier.fillMaxWidth(0.90f),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            CircularPromptStatBar(
              modifier = Modifier.weight(1f),
              label = "SPD",
              fraction = (ship.speedPts / 25f).coerceIn(0.18f, 1f),
              color = Color(0xFFFACC15),
            )
            CircularPromptStatBar(
              modifier = Modifier.weight(1f),
              label = "TURN",
              fraction = (ship.turnPts / 25f).coerceIn(0.18f, 1f),
              color = Color(0xFF34D399),
            )
          }

          // Primary [ CHARGE (Flux Icon) ] + [ X ] Row (>= 48dp touch targets)
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
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
              Spinning25dShipWithOrbitingFlux(
                shipId = prompt.shipId,
                shipSpinDeg = shipSpinDeg,
                fluxOrbitRad = fluxOrbitRad,
                isCharged = chargedShipId == prompt.shipId,
                isBestShip = bestShipId == prompt.shipId,
                isSelected = true,
                sizeDp = if (isTabletDeck) 62.dp else 46.dp,
              )
            }
            prompt.buffTypeIndex != Int.MIN_VALUE -> {
              BuffIconPreview(
                buffType = prompt.buffTypeIndex,
                sizeDp = if (isTabletDeck) 56 else 44,
                accentColor = accentCol,
              )
            }
            prompt.riftAnomalyId.isNotBlank() -> {
              RiftAnomalyIconPreview(
                riftId = prompt.riftAnomalyId,
                sizeDp = if (isTabletDeck) 60 else 48,
                accentColor = accentCol,
              )
            }
            prompt.showStationPreview -> {
              Image(
                bitmap = stationBmp,
                contentDescription = "Rift Station Safe Harbor",
                contentScale = ContentScale.Fit,
                modifier =
                  Modifier.size(if (isTabletDeck) 60.dp else 46.dp)
                    .graphicsLayer { rotationZ = shipSpinDeg * 0.25f },
              )
            }
          }

          Text(
            text = prompt.title,
            color = Color.White,
            fontSize = if (isTabletDeck) 11.8.sp else 10.2.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
          )

          if (prompt.badge.isNotBlank()) {
            Box(
              modifier =
                Modifier.clip(RoundedCornerShape(5.dp))
                  .background(accentCol.copy(alpha = 0.16f))
                  .border(1.dp, accentCol.copy(alpha = 0.85f), RoundedCornerShape(5.dp))
                  .padding(horizontal = 7.dp, vertical = 2.dp),
            ) {
              Text(
                text = prompt.badge,
                color = accentCol,
                fontSize = if (isTabletDeck) 8.6.sp else 7.8.sp,
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
                color = Color(0xFF94A3B8),
                fontSize = if (isTabletDeck) 8.6.sp else 7.8.sp,
                lineHeight = if (isTabletDeck) 11.6.sp else 10.4.sp,
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
    }
  }
}

@Composable
private fun CircularPromptStatBar(
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

@Composable
private fun SyndicateLeftDirectoryBay(
  isTabletDeck: Boolean,
  headerHeight: Dp,
  wingStackGap: Dp,
  currentTag: String,
  syndicateDirectory: List<SyndicateInfo>,
  statusMessage: String?,
  onBackToHub: () -> Unit,
  onInspectSyndicate: (SyndicateInfo) -> Unit,
) {
  val cleanSyndicates =
    remember(syndicateDirectory) {
      syndicateDirectory
        .filter { it.isRealPlayerFoundedSyndicate() }
        .distinctBy { it.tag.trim().uppercase(Locale.US) }
        .sortedByDescending { it.combinedBounty }
    }
  Column(
    modifier = Modifier.fillMaxSize().testTag("syndicate_left_directory_bay"),
    verticalArrangement = Arrangement.spacedBy(wingStackGap),
  ) {
    HubDigitizedReturnBar(
      heightDp = headerHeight,
      isRightSide = false,
      isTabletDeck = isTabletDeck,
      sectionTitle = "SYNDICATES (${cleanSyndicates.size})",
      accent = Color(0xFFFACC15),
      onBackToHub = onBackToHub,
    )
    if (!statusMessage.isNullOrBlank()) {
      Text(
        text = statusMessage,
        color = Color(0xFF00F5D4),
        fontSize = 7.6.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace,
        maxLines = 2,
        modifier = Modifier.padding(horizontal = 6.dp),
      )
    }
    Column(
      modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(wingStackGap),
    ) {
      if (cleanSyndicates.isEmpty()) {
        Box(
          modifier =
            Modifier.fillMaxWidth()
              .chamferedSciFiPodSurface(
                isRightSide = false,
                primaryBorder = Color(0xFF38BDF8),
                secondaryBorder = Color(0xFF091528),
              )
              .padding(10.dp),
        ) {
          Text(
            text = "NO HUMAN-CHARTERED SYNDICATES YET. FOUND ONE ON THE RIGHT WING.",
            color = Color(0xFF94A3B8),
            fontSize = if (isTabletDeck) 8.6.sp else 7.6.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
          )
        }
      } else {
        cleanSyndicates.forEach { syn ->
          val isMember = currentTag.isNotBlank() && syn.tag.equals(currentTag, ignoreCase = true)
          Row(
            modifier =
              Modifier.fillMaxWidth()
                .minimumInteractiveComponentSize()
                .chamferedSciFiPodSurface(
                  isRightSide = false,
                  primaryBorder = if (isMember) Color(0xFFFFD700) else Color(0xFF38BDF8),
                  secondaryBorder = if (isMember) Color(0xFF00F5D4) else Color(0xFF091528),
                )
                .clickable { onInspectSyndicate(syn) }
                .padding(horizontal = 8.dp, vertical = 6.dp)
                .testTag("syndicate_row_${syn.tag.lowercase(Locale.US)}"),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.weight(1f).padding(end = 6.dp)) {
              Text(
                text = "[${syn.tag.uppercase(Locale.US)}] ${syn.name.uppercase(Locale.US)}",
                color = Color.White,
                fontSize = if (isTabletDeck) 9.4.sp else 8.2.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
              ) {
                ResonanceCrestIcon(sizeDp = 11.dp)
                Text(
                  text = "${String.format(Locale.US, "%,d", syn.combinedBounty.coerceAtLeast(0L))} • ${syn.memberCount.coerceAtLeast(1)}P",
                  color = Color(0xFFFFD700),
                  fontSize = 8.4.sp,
                  fontWeight = FontWeight.Bold,
                  fontFamily = FontFamily.Monospace,
                  maxLines = 1,
                )
              }
            }
            Text(
              text = if (isMember) "ENLISTED" else "INSPECT >",
              color = if (isMember) Color(0xFF00F5D4) else Color(0xFF38BDF8),
              fontSize = 7.6.sp,
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
private fun SyndicateRightCitadelBay(
  isTabletDeck: Boolean,
  headerHeight: Dp,
  wingStackGap: Dp,
  syndicateTag: String,
  syndicateName: String,
  syndicateOnlineCount: Int,
  syndicateWarRecord: SyndicateWarRecord,
  onLeaveCurrentSyndicate: () -> Unit,
  onCharterCustomSyndicate: (tag: String, name: String, motto: String) -> Unit,
  onOpenCrestForge: () -> Unit,
) {
  var draftTag by remember { mutableStateOf("") }
  var draftName by remember { mutableStateOf("") }
  var draftMotto by remember { mutableStateOf("") }

  Column(
    modifier = Modifier.fillMaxSize().testTag("syndicate_right_citadel_bay"),
    verticalArrangement = Arrangement.spacedBy(wingStackGap),
  ) {
    Row(
      modifier =
        Modifier.fillMaxWidth()
          .height(headerHeight)
          .chamferedSciFiPodSurface(
            isRightSide = true,
            primaryBorder = Color(0xFFFACC15),
            secondaryBorder = Color(0xFF38BDF8),
          )
          .padding(horizontal = 8.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = if (syndicateTag.isNotBlank()) "[$syndicateTag] CITADEL" else "CHARTER CITADEL",
        color = Color(0xFFFFD700),
        fontSize = if (isTabletDeck) 9.6.sp else 8.4.sp,
        fontWeight = FontWeight.Black,
        fontFamily = FontFamily.Monospace,
        maxLines = 1,
      )
      Box(
        modifier =
          Modifier.minimumInteractiveComponentSize()
            .clickable { onOpenCrestForge() }
            .padding(horizontal = 4.dp, vertical = 6.dp)
            .testTag("syndicate_open_crest_forge"),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          text = "CREST >",
          color = Color(0xFF00F5D4),
          fontSize = 7.8.sp,
          fontWeight = FontWeight.Black,
          fontFamily = FontFamily.Monospace,
        )
      }
    }

    Column(
      modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(wingStackGap),
    ) {
      Column(
        modifier =
          Modifier.fillMaxWidth()
            .chamferedSciFiPodSurface(
              isRightSide = true,
              primaryBorder = Color(0xFF38BDF8),
              secondaryBorder = Color(0xFF091528),
            )
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
      ) {
        Text(
          text = if (syndicateTag.isNotBlank()) syndicateName else "UNAFFILIATED PILOT",
          color = Color.White,
          fontSize = if (isTabletDeck) 9.6.sp else 8.4.sp,
          fontWeight = FontWeight.Black,
          fontFamily = FontFamily.Monospace,
        )
        Text(
          text = "ONLINE: $syndicateOnlineCount • WARS: ${syndicateWarRecord.warWins}W-${syndicateWarRecord.warLosses}L",
          color = Color(0xFF38BDF8),
          fontSize = 7.6.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.Monospace,
        )
        if (syndicateTag.isNotBlank()) {
          Box(
            modifier =
              Modifier.minimumInteractiveComponentSize()
                .height(48.dp)
                .clickable { onLeaveCurrentSyndicate() }
                .padding(vertical = 5.dp)
                .testTag("syndicate_leave_button"),
            contentAlignment = Alignment.CenterStart,
          ) {
            Box(
              modifier =
                Modifier.clip(RoundedCornerShape(6.dp))
                  .background(Color(0x33FB7185))
                  .border(1.dp, Color(0xFFFB7185), RoundedCornerShape(6.dp))
                  .padding(horizontal = 9.dp, vertical = 5.dp),
            ) {
              Text(
                text = "LEAVE [$syndicateTag]",
                color = Color(0xFFFB7185),
                fontSize = 7.8.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
              )
            }
          }
        }
      }

      // Charter Human-Founded Syndicate Box
      Column(
        modifier =
          Modifier.fillMaxWidth()
            .chamferedSciFiPodSurface(
              isRightSide = true,
              primaryBorder = Color(0xFFFFD700),
              secondaryBorder = Color(0xFF00F5D4),
            )
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        Text(
          text = "FOUND NEW SYNDICATE (1-3 TAG)",
          color = Color(0xFFFFD700),
          fontSize = 8.0.sp,
          fontWeight = FontWeight.Black,
          fontFamily = FontFamily.Monospace,
        )
        BasicTextField(
          value = draftTag,
          onValueChange = { draftTag = it.uppercase(Locale.US).replace(Regex("[^A-Z0-9]"), "").take(3) },
          singleLine = true,
          cursorBrush = SolidColor(Color(0xFF00F5D4)),
          textStyle = TextStyle(color = Color.White, fontSize = 8.8.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace),
          decorationBox = { inner ->
            Box(
              modifier =
                Modifier.fillMaxWidth()
                  .clip(RoundedCornerShape(5.dp))
                  .background(Color(0xDD02050B))
                  .border(1.dp, Color(0xFF38BDF8), RoundedCornerShape(5.dp))
                  .padding(horizontal = 7.dp, vertical = 5.dp),
            ) {
              if (draftTag.isEmpty()) {
                Text("TAG (E.G. VTX)", color = Color(0xFF64748B), fontSize = 8.0.sp, fontFamily = FontFamily.Monospace)
              }
              inner()
            }
          },
          modifier = Modifier.testTag("syndicate_charter_tag_input"),
        )
        BasicTextField(
          value = draftName,
          onValueChange = { draftName = it.take(22) },
          singleLine = true,
          cursorBrush = SolidColor(Color(0xFF00F5D4)),
          textStyle = TextStyle(color = Color.White, fontSize = 8.8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace),
          decorationBox = { inner ->
            Box(
              modifier =
                Modifier.fillMaxWidth()
                  .clip(RoundedCornerShape(5.dp))
                  .background(Color(0xDD02050B))
                  .border(1.dp, Color(0xFF38BDF8), RoundedCornerShape(5.dp))
                  .padding(horizontal = 7.dp, vertical = 5.dp),
            ) {
              if (draftName.isEmpty()) {
                Text("SYNDICATE NAME", color = Color(0xFF64748B), fontSize = 8.0.sp, fontFamily = FontFamily.Monospace)
              }
              inner()
            }
          },
          modifier = Modifier.testTag("syndicate_charter_name_input"),
        )
        BasicTextField(
          value = draftMotto,
          onValueChange = { draftMotto = it.take(36) },
          singleLine = true,
          cursorBrush = SolidColor(Color(0xFF00F5D4)),
          textStyle = TextStyle(color = Color(0xFFCBD5E1), fontSize = 8.2.sp, fontFamily = FontFamily.Monospace),
          decorationBox = { inner ->
            Box(
              modifier =
                Modifier.fillMaxWidth()
                  .clip(RoundedCornerShape(5.dp))
                  .background(Color(0xDD02050B))
                  .border(1.dp, Color(0x5538BDF8), RoundedCornerShape(5.dp))
                  .padding(horizontal = 7.dp, vertical = 5.dp),
            ) {
              if (draftMotto.isEmpty()) {
                Text("SECTOR MOTTO", color = Color(0xFF64748B), fontSize = 7.8.sp, fontFamily = FontFamily.Monospace)
              }
              inner()
            }
          },
          modifier = Modifier.testTag("syndicate_charter_motto_input"),
        )
        val canCharter = draftTag.trim().length in 1..3 && draftName.trim().length >= 3
        Box(
          modifier =
            Modifier.fillMaxWidth()
              .minimumInteractiveComponentSize()
              .height(48.dp)
              .clickable(enabled = canCharter) {
                onCharterCustomSyndicate(draftTag.trim(), draftName.trim(), draftMotto.trim())
                draftTag = ""
                draftName = ""
                draftMotto = ""
              }
              .padding(vertical = 5.dp)
              .testTag("syndicate_charter_submit_button"),
          contentAlignment = Alignment.Center,
        ) {
          Box(
            modifier =
              Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(if (canCharter) Color(0xFF0D2D46) else Color(0xFF0F172A))
                .border(1.dp, if (canCharter) Color(0xFFFFD700) else Color(0xFF475569), RoundedCornerShape(6.dp))
                .padding(vertical = 6.dp),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text = "CHARTER SYNDICATE >",
              color = if (canCharter) Color.White else Color(0xFF64748B),
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
private fun ConduitLeftDirectivesBay(
  isTabletDeck: Boolean,
  headerHeight: Dp,
  wingStackGap: Dp,
  liveResonance: Int,
  conduitTier: com.example.engine.ConduitRankTier,
  directives: List<DailyConduitDirective>,
  isSweepClaimed: Boolean,
  onBackToHub: () -> Unit,
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
      sectionTitle = "RANKS & QUESTS",
      accent = Color(0xFFFFD700),
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
            fontSize = if (isTabletDeck) 9.8.sp else 8.6.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
          )
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            ResonanceCrestIcon(sizeDp = 12.dp)
            Text(
              text = String.format(Locale.US, "%,d", liveResonance),
              color = Color(0xFFFFD700),
              fontSize = 9.2.sp,
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
            fontSize = 8.0.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
          )
          ResonanceCrestIcon(sizeDp = 10.dp)
          Text(
            text = "${nextMilestone.first}",
            color = Color(0xFFFFD700),
            fontSize = 8.0.sp,
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
            fontSize = if (isTabletDeck) 9.8.sp else 8.8.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
          )
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
              text = "${dir.progressValue}/${dir.targetValue} • +${dir.shardReward} SHARDS •",
              color = Color(0xFF38BDF8),
              fontSize = 8.2.sp,
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.Monospace,
              maxLines = 1,
            )
            ResonanceCrestIcon(sizeDp = 10.dp)
            Text(
              text = "+${dir.resonanceReward}",
              color = Color(0xFFFFD700),
              fontSize = 8.2.sp,
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
private fun ConduitRightVaultBay(
  isTabletDeck: Boolean,
  headerHeight: Dp,
  wingStackGap: Dp,
  shardText: String,
  activeCustomSlotId: String,
  localPrefs: LocalPilotPrefs,
  currentUserId: String,
  onSelectFrameSlot: (String) -> Unit,
  onInspectCosmetic: (CosmeticCatalogItem, Boolean, Boolean) -> Unit,
) {
  val uidOrNull = currentUserId.ifBlank { null }
  val allCosmetics = remember {
    RiftProgressionCatalog.hullTrims +
      RiftProgressionCatalog.killFinishers +
      RiftProgressionCatalog.warpSignatures
  }

  Column(
    modifier = Modifier.fillMaxSize().testTag("conduit_right_vault_bay"),
    verticalArrangement = Arrangement.spacedBy(wingStackGap),
  ) {
    Row(
      modifier =
        Modifier.fillMaxWidth()
          .height(headerHeight)
          .chamferedSciFiPodSurface(
            isRightSide = true,
            primaryBorder = Color(0xFFFFD700),
            secondaryBorder = Color(0xFF00F5D4),
          )
          .padding(horizontal = 8.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = "COSMETIC VAULT",
        color = Color.White,
        fontSize = if (isTabletDeck) 9.4.sp else 8.2.sp,
        fontWeight = FontWeight.Black,
        fontFamily = FontFamily.Monospace,
      )
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        RiftShardGemIcon(sizeDp = 12.dp)
        Text(
          text = shardText,
          color = Color(0xFF00F5D4),
          fontSize = 8.2.sp,
          fontWeight = FontWeight.Black,
          fontFamily = FontFamily.Monospace,
        )
      }
    }

    // Rift Frame Loadout Slots A..E
    Row(
      modifier =
        Modifier.fillMaxWidth()
          .height(48.dp)
          .chamferedSciFiPodSurface(
            isRightSide = true,
            primaryBorder = Color(0xFF38BDF8),
            secondaryBorder = Color(0xFF091528),
          )
          .padding(horizontal = 4.dp),
      horizontalArrangement = Arrangement.spacedBy(3.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      val normalizedActiveSlot = localPrefs.normalizeCustomSlotId(activeCustomSlotId)
      listOf("A", "B", "C", "D", "E").forEach { slot ->
        val fullSlot = localPrefs.normalizeCustomSlotId(slot)
        val sel = normalizedActiveSlot == fullSlot
        Box(
          modifier =
            Modifier.weight(1f)
              .fillMaxHeight()
              .minimumInteractiveComponentSize()
              .clickable { onSelectFrameSlot(fullSlot) }
              .padding(vertical = 6.dp)
              .testTag("conduit_frame_slot_$slot"),
          contentAlignment = Alignment.Center,
        ) {
          Box(
            modifier =
              Modifier.fillMaxSize()
                .clip(RoundedCornerShape(5.dp))
                .background(if (sel) Color(0x3800F5D4) else Color(0x6602050B))
                .border(1.dp, if (sel) Color(0xFF00F5D4) else Color(0x4438BDF8), RoundedCornerShape(5.dp)),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text = slot,
              color = if (sel) Color.White else Color(0xFF94A3B8),
              fontSize = 8.2.sp,
              fontWeight = FontWeight.Black,
              fontFamily = FontFamily.Monospace,
            )
          }
        }
      }
    }

    Column(
      modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(wingStackGap),
    ) {
      allCosmetics.forEach { item ->
        val unlocked = localPrefs.isCosmeticUnlocked(item.category, item.id, uid = uidOrNull)
        val equipped = localPrefs.getEquippedCosmeticId(item.category, uidOrNull) == item.id
        Row(
          modifier =
            Modifier.fillMaxWidth()
              .minimumInteractiveComponentSize()
              .chamferedSciFiPodSurface(
                isRightSide = true,
                primaryBorder = if (equipped) Color(0xFF00F5D4) else Color(0xFF38BDF8),
                secondaryBorder = if (unlocked) Color(0xFFFFD700) else Color(0xFF091528),
              )
              .clickable { onInspectCosmetic(item, unlocked, equipped) }
              .padding(horizontal = 8.dp, vertical = 6.dp)
              .testTag("conduit_cosmetic_${item.category.lowercase(Locale.US)}_${item.id}"),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f).padding(end = 6.dp)) {
            Text(
              text = item.name.uppercase(Locale.US),
              color = Color.White,
              fontSize = if (isTabletDeck) 8.8.sp else 7.8.sp,
              fontWeight = FontWeight.Black,
              fontFamily = FontFamily.Monospace,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Text(
              text = item.category.replace("_", " "),
              color = Color(0xFF94A3B8),
              fontSize = 7.0.sp,
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.Monospace,
            )
          }
          Text(
            text = when {
              equipped -> "ACTIVE"
              unlocked -> "EQUIP >"
              else -> "${item.shardCost}S"
            },
            color = when {
              equipped -> Color(0xFF00F5D4)
              unlocked -> Color(0xFFFACC15)
              else -> Color(0xFFCBD5E1)
            },
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
private fun DossierLeftIdentityBay(
  isTabletDeck: Boolean,
  headerHeight: Dp,
  wingStackGap: Dp,
  callsign: String,
  syndicateTag: String,
  liveResonance: Int,
  peakResonance: Int,
  localPrefs: LocalPilotPrefs,
  currentUserId: String,
  onBackToHub: () -> Unit,
  onOpenRenamePrompt: () -> Unit,
  onOpenResetPrompt: () -> Unit,
  onSwitchAccount: () -> Unit,
  onOpenSurvivalSpreadsheet: () -> Unit = {},
) {
  val context = LocalContext.current
  val uidOrNull = currentUserId.ifBlank { null }
  var exportStatusBanner by remember { mutableStateOf<String?>(null) }
  val allKills = remember(currentUserId, liveResonance) { localPrefs.getAllTimeKills(uidOrNull) }
  val allDeaths = remember(currentUserId, liveResonance) { localPrefs.getAllTimeDeaths(uidOrNull) }
  val pvpKills = remember(currentUserId, liveResonance) { localPrefs.getPvpKills(uidOrNull) }
  val wins = remember(currentUserId, liveResonance) { localPrefs.getConvergencesWon() }
  val survivalSummary = remember(currentUserId, liveResonance, allKills, allDeaths, wins) {
    localPrefs.getPilotSurvivalTelemetrySummary(uidOrNull)
  }
  val kdText =
    remember(allKills, allDeaths) {
      if (allDeaths <= 0L) String.format(Locale.US, "%.1f", allKills.toDouble())
      else String.format(Locale.US, "%.2f", allKills.toDouble() / allDeaths.toDouble())
    }

  Column(
    modifier = Modifier.fillMaxSize().testTag("dossier_left_identity_bay"),
    verticalArrangement = Arrangement.spacedBy(wingStackGap),
  ) {
    HubDigitizedReturnBar(
      heightDp = headerHeight,
      isRightSide = false,
      isTabletDeck = isTabletDeck,
      sectionTitle = "PILOT DOSSIER",
      accent = Color(0xFF00F5D4),
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
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        Text(
          text = if (syndicateTag.isNotBlank()) "[$syndicateTag] $callsign" else callsign,
          color = Color.White,
          fontSize = if (isTabletDeck) 10.6.sp else 9.4.sp,
          fontWeight = FontWeight.Black,
          fontFamily = FontFamily.Monospace,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
          ResonanceCrestIcon(sizeDp = 13.dp)
          Text(
            text = "LIVE ${String.format(Locale.US, "%,d", liveResonance)} • PEAK ${String.format(Locale.US, "%,d", peakResonance)}",
            color = Color(0xFFFFD700),
            fontSize = 8.6.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
          )
        }
        Text(
          text = "ELIMS: $allKills (PVP $pvpKills) • K/D: $kdText • WINS: $wins",
          color = Color(0xFF38BDF8),
          fontSize = 7.6.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.Monospace,
        )
      }

      // Ship Alive Duration Summary + 1-Click HTML / CSV / JSON Telemetry Export Pod
      Column(
        modifier =
          Modifier.fillMaxWidth()
            .chamferedSciFiPodSurface(
              isRightSide = false,
              primaryBorder = Color(0xFF38BDF8),
              secondaryBorder = Color(0xFF00F5D4),
            )
            .padding(8.dp)
            .testTag("dossier_hub_survival_summary_pod"),
        verticalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        Text(
          text = "SHIP ALIVE DURATION (${survivalSummary.pilotCompletedLives} LIVES)",
          color = Color(0xFF00F5D4),
          fontSize = 7.8.sp,
          fontWeight = FontWeight.Black,
          fontFamily = FontFamily.Monospace,
          maxLines = 1,
        )
        Text(
          text =
            "AVG ${formatSurvivalDurationSec(survivalSummary.pilotAvgSurvivalSec)} • LAST ${formatSurvivalDurationSec(survivalSummary.pilotLastSurvivalSec)} • MAX ${formatSurvivalDurationSec(survivalSummary.pilotMaxSurvivalSec)}",
          color = Color.White,
          fontSize = 7.5.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.Monospace,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text =
            "AI BOT AVG: ${formatSurvivalDurationSec(survivalSummary.botAvgSurvivalSec)} (${survivalSummary.botCompletedLives} LIVES)",
          color = Color(0xFFC084FC),
          fontSize = 7.2.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.Monospace,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(4.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          HubTelemetryMiniExportBtn(
            modifier = Modifier.weight(1.25f),
            label = "HTML TABLE",
            accent = Color(0xFF00F5D4),
            testTag = "hub_dossier_open_html_btn",
          ) {
            onOpenSurvivalSpreadsheet()
          }
          HubTelemetryMiniExportBtn(
            modifier = Modifier.weight(1f),
            label = "CSV",
            accent = Color(0xFFFACC15),
            testTag = "hub_dossier_export_csv_btn",
          ) {
            val csv = localPrefs.exportSurvivalTelemetryCsv(callsign, liveResonance, uidOrNull)
            exportStatusBanner =
              shareOrOpenSurvivalExport(
                context = context,
                fileName = "rift_ship_survival_telemetry.csv",
                mimeType = "text/csv",
                payload = csv,
              )
          }
          HubTelemetryMiniExportBtn(
            modifier = Modifier.weight(1f),
            label = "JSON",
            accent = Color(0xFF38BDF8),
            testTag = "hub_dossier_export_json_btn",
          ) {
            val json = localPrefs.exportSurvivalTelemetryJson(callsign, liveResonance, uidOrNull)
            exportStatusBanner =
              shareOrOpenSurvivalExport(
                context = context,
                fileName = "rift_ship_survival_telemetry.json",
                mimeType = "application/json",
                payload = json,
              )
          }
        }
        if (!exportStatusBanner.isNullOrBlank()) {
          Text(
            text = exportStatusBanner!!,
            color = Color(0xFFFACC15),
            fontSize = 7.0.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }

      // Rename Callsign Trigger -> Opens Circular Glass Rename Prompt in Overwatch
      Box(
        modifier =
          Modifier.fillMaxWidth()
            .minimumInteractiveComponentSize()
            .height(48.dp)
            .clickable { onOpenRenamePrompt() }
            .padding(vertical = 4.dp)
            .testTag("dossier_rename_callsign_button"),
        contentAlignment = Alignment.Center,
      ) {
        Box(
          modifier =
            Modifier.fillMaxWidth()
              .clip(RoundedCornerShape(7.dp))
              .background(Color(0xFF0D2D46))
              .border(1.dp, Color(0xFF00F5D4), RoundedCornerShape(7.dp))
              .padding(vertical = 6.dp),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = "RENAME CALLSIGN >",
            color = Color.White,
            fontSize = 8.2.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
          )
        }
      }

      // Switch Account Trigger
      Box(
        modifier =
          Modifier.fillMaxWidth()
            .minimumInteractiveComponentSize()
            .height(48.dp)
            .clickable { onSwitchAccount() }
            .padding(vertical = 4.dp)
            .testTag("dossier_switch_account_button"),
        contentAlignment = Alignment.Center,
      ) {
        Box(
          modifier =
            Modifier.fillMaxWidth()
              .clip(RoundedCornerShape(7.dp))
              .background(Color(0xFF091528))
              .border(1.dp, Color(0xFF38BDF8), RoundedCornerShape(7.dp))
              .padding(vertical = 6.dp),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = "SWITCH PILOT ACCOUNT",
            color = Color(0xFFE2E8F0),
            fontSize = 8.0.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
          )
        }
      }

      // Reset Local Telemetry Trigger -> Opens Circular Glass Confirmation in Overwatch
      Box(
        modifier =
          Modifier.fillMaxWidth()
            .minimumInteractiveComponentSize()
            .height(48.dp)
            .clickable { onOpenResetPrompt() }
            .padding(vertical = 4.dp)
            .testTag("dossier_reset_pilot_button"),
        contentAlignment = Alignment.Center,
      ) {
        Box(
          modifier =
            Modifier.fillMaxWidth()
              .clip(RoundedCornerShape(7.dp))
              .background(Color(0x26FB7185))
              .border(1.dp, Color(0xFFFB7185), RoundedCornerShape(7.dp))
              .padding(vertical = 6.dp),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = "RESET LOCAL RECORD",
            color = Color(0xFFFB7185),
            fontSize = 8.0.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
          )
        }
      }
    }
  }
}

@Composable
private fun HubTelemetryMiniExportBtn(
  modifier: Modifier = Modifier,
  label: String,
  accent: Color,
  testTag: String,
  onClick: () -> Unit,
) {
  Box(
    modifier =
      modifier
        .minimumInteractiveComponentSize()
        .height(48.dp)
        .clickable { onClick() }
        .padding(vertical = 6.dp)
        .testTag(testTag),
    contentAlignment = Alignment.Center,
  ) {
    Box(
      modifier =
        Modifier.fillMaxSize()
          .clip(RoundedCornerShape(6.dp))
          .background(accent.copy(alpha = 0.16f))
          .border(1.dp, accent, RoundedCornerShape(6.dp)),
      contentAlignment = Alignment.Center,
    ) {
      Text(
        text = label,
        color = Color.White,
        fontSize = 7.2.sp,
        fontWeight = FontWeight.Black,
        fontFamily = FontFamily.Monospace,
        maxLines = 1,
      )
    }
  }
}

@Composable
private fun HubCustomFivePercentStepperRow(
  label: String,
  valuePct: Int,
  minusTag: String,
  valueTag: String,
  plusTag: String,
  onMinus: () -> Unit,
  onPlus: () -> Unit,
) {
  val snapped = snapCustomFivePercentScale(valuePct)
  Row(
    modifier = Modifier.fillMaxWidth().height(48.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      text = label,
      color = Color(0xFFCBD5E1),
      fontSize = 7.4.sp,
      fontWeight = FontWeight.Black,
      fontFamily = FontFamily.Monospace,
      maxLines = 1,
    )
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
      Box(
        modifier =
          Modifier.minimumInteractiveComponentSize()
            .size(48.dp)
            .clickable(enabled = snapped > 0) { onMinus() }
            .testTag(minusTag),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          text = "[-5%]",
          color = if (snapped > 0) Color(0xFF00F5D4) else Color(0xFF475569),
          fontSize = 7.6.sp,
          fontWeight = FontWeight.Black,
          fontFamily = FontFamily.Monospace,
        )
      }
      Text(
        text = "$snapped%",
        color = if (snapped == 100) Color(0xFFE2E8F0) else Color(0xFFFFD700),
        fontSize = 8.0.sp,
        fontWeight = FontWeight.Black,
        fontFamily = FontFamily.Monospace,
        modifier = Modifier.width(34.dp).testTag(valueTag),
        textAlign = TextAlign.Center,
      )
      Box(
        modifier =
          Modifier.minimumInteractiveComponentSize()
            .size(48.dp)
            .clickable(enabled = snapped < 100) { onPlus() }
            .testTag(plusTag),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          text = "[+5%]",
          color = if (snapped < 100) Color(0xFF00F5D4) else Color(0xFF475569),
          fontSize = 7.6.sp,
          fontWeight = FontWeight.Black,
          fontFamily = FontFamily.Monospace,
        )
      }
    }
  }
}

@Composable
private fun DossierRightCareerBay(
  isTabletDeck: Boolean,
  headerHeight: Dp,
  wingStackGap: Dp,
  bestShipId: Int,
  shipSpinDeg: Float,
  fluxOrbitRad: Float,
  shipResonanceFor: (Int) -> Int,
  localPrefs: LocalPilotPrefs,
  currentUserId: String,
  cloudLeaderboardEntries: List<LeaderboardEntry>,
  onOpenSurvivalSpreadsheet: () -> Unit = {},
  onInspectShip: (Int) -> Unit,
) {
  var careerTab by remember { mutableIntStateOf(0) }
  val uidOrNull = currentUserId.ifBlank { null }
  val survivalSummary = remember(currentUserId, careerTab, bestShipId) {
    localPrefs.getPilotSurvivalTelemetrySummary(uidOrNull)
  }
  val survivalByShip = remember(survivalSummary) {
    survivalSummary.perShip.associateBy { it.shipId }
  }
  val shipsSorted = remember(bestShipId) {
    RiftCodex.getAllTenShips().sortedByDescending { shipResonanceFor(it.id) }
  }
  val realHumanLeaders = remember(cloudLeaderboardEntries) {
    cloudLeaderboardEntries
      .filter { e ->
        val uid = e.userId.ifBlank { e.id }.trim()
        uid.isNotBlank() &&
          !uid.startsWith("bot_", ignoreCase = true) &&
          !e.callsign.equals("COMMANDER", ignoreCase = true)
      }
      .distinctBy { (it.userId.ifBlank { it.id }).lowercase(Locale.US) }
      .sortedByDescending { it.allTimeBounty }
      .take(20)
  }

  Column(
    modifier = Modifier.fillMaxSize().testTag("dossier_right_career_bay"),
    verticalArrangement = Arrangement.spacedBy(wingStackGap),
  ) {
    Row(
      modifier =
        Modifier.fillMaxWidth()
          .height(headerHeight)
          .chamferedSciFiPodSurface(
            isRightSide = true,
            primaryBorder = Color(0xFF00F5D4),
            secondaryBorder = Color(0xFFFFD700),
          )
          .padding(horizontal = 4.dp),
      horizontalArrangement = Arrangement.spacedBy(3.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      listOf(
        Triple(0, "HULL RANK", "dossier_tab_ship_res"),
        Triple(1, "SURVIVAL", "dossier_tab_survival"),
        Triple(2, "FAME", "dossier_tab_hall_of_fame"),
      ).forEach { (tabIdx, label, tag) ->
        val sel = careerTab == tabIdx
        Box(
          modifier =
            Modifier.weight(1f)
              .fillMaxHeight()
              .minimumInteractiveComponentSize()
              .clickable { careerTab = tabIdx }
              .padding(vertical = 5.dp)
              .testTag(tag),
          contentAlignment = Alignment.Center,
        ) {
          Box(
            modifier =
              Modifier.fillMaxSize()
                .clip(RoundedCornerShape(6.dp))
                .background(if (sel) Color(0x3800F5D4) else Color(0x6602050B))
                .border(1.dp, if (sel) Color(0xFF00F5D4) else Color(0x4438BDF8), RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text = label,
              color = if (sel) Color.White else Color(0xFF94A3B8),
              fontSize = 8.4.sp,
              fontWeight = FontWeight.Black,
              fontFamily = FontFamily.Monospace,
              maxLines = 1,
            )
          }
        }
      }
    }

    Column(
      modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(wingStackGap),
    ) {
      when (careerTab) {
        0 -> {
          shipsSorted.forEach { ship ->
            val res = shipResonanceFor(ship.id)
            val isBest = ship.id == bestShipId
            val cleanName = ship.name.replace(Regex("^[0-9]+-"), "").trim().uppercase(Locale.US)
            val surv = survivalByShip[ship.id]
            Row(
              modifier =
                Modifier.fillMaxWidth()
                  .minimumInteractiveComponentSize()
                  .chamferedSciFiPodSurface(
                    isRightSide = true,
                    primaryBorder = if (isBest) Color(0xFFFFD700) else Color(0xFF38BDF8),
                    secondaryBorder = Color(0xFF091528),
                  )
                  .clickable { onInspectShip(ship.id) }
                  .padding(horizontal = 8.dp, vertical = 4.dp)
                  .testTag("dossier_ship_row_${ship.id}"),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f),
              ) {
                Spinning25dShipWithOrbitingFlux(
                  shipId = ship.id,
                  shipSpinDeg = shipSpinDeg,
                  fluxOrbitRad = fluxOrbitRad,
                  isCharged = false,
                  isBestShip = isBest,
                  sizeDp = 32.dp,
                )
                Column {
                  Text(
                    text = cleanName,
                    color = Color.White,
                    fontSize = if (isTabletDeck) 8.8.sp else 7.8.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                  )
                  val subLine =
                    when {
                      isBest && (surv?.completedLives ?: 0) > 0 ->
                        "BEST • AVG ${formatSurvivalDurationSec(surv!!.avgSurvivalSec)}"
                      isBest -> "BEST HULL"
                      (surv?.completedLives ?: 0) > 0 ->
                        "AVG ${formatSurvivalDurationSec(surv!!.avgSurvivalSec)} (${surv.completedLives}L)"
                      else -> "0 LIVES RECORDED"
                    }
                  Text(
                    text = subLine,
                    color = if (isBest) Color(0xFFFFD700) else Color(0xFF38BDF8),
                    fontSize = 6.8.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                  )
                }
              }
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                ResonanceCrestIcon(sizeDp = 11.dp)
                Text(
                  text = String.format(Locale.US, "%,d", res),
                  color = Color(0xFFFFD700),
                  fontSize = 8.8.sp,
                  fontWeight = FontWeight.Black,
                  fontFamily = FontFamily.Monospace,
                )
              }
            }
          }
        }

        1 -> {
          Box(
            modifier =
              Modifier.fillMaxWidth()
                .minimumInteractiveComponentSize()
                .height(48.dp)
                .clickable { onOpenSurvivalSpreadsheet() }
                .padding(vertical = 4.dp)
                .testTag("dossier_right_open_html_spreadsheet_btn"),
            contentAlignment = Alignment.Center,
          ) {
            Box(
              modifier =
                Modifier.fillMaxSize()
                  .clip(RoundedCornerShape(6.dp))
                  .background(Color(0xFF0D2D46))
                  .border(1.dp, Color(0xFF00F5D4), RoundedCornerShape(6.dp)),
              contentAlignment = Alignment.Center,
            ) {
              Text(
                text = "OPEN HTML TELEMETRY SPREADSHEET >",
                color = Color(0xFF00F5D4),
                fontSize = 7.6.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
              )
            }
          }

          survivalSummary.perShip.forEach { entry ->
            Row(
              modifier =
                Modifier.fillMaxWidth()
                  .minimumInteractiveComponentSize()
                  .chamferedSciFiPodSurface(
                    isRightSide = true,
                    primaryBorder = if (entry.completedLives > 0) Color(0xFF00F5D4) else Color(0xFF38BDF8),
                    secondaryBorder = Color(0xFF091528),
                  )
                  .clickable { onInspectShip(entry.shipId) }
                  .padding(horizontal = 8.dp, vertical = 5.dp)
                  .testTag("dossier_survival_row_${entry.shipId}"),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column(modifier = Modifier.weight(1f).padding(end = 4.dp)) {
                Text(
                  text = "${entry.shipName} (${entry.completedLives}L)",
                  color = Color.White,
                  fontSize = 7.8.sp,
                  fontWeight = FontWeight.Black,
                  fontFamily = FontFamily.Monospace,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
                val botPart =
                  if (entry.botCompletedLives > 0)
                    " • BOT ${formatSurvivalDurationSec(entry.botAvgSurvivalSec)}"
                  else ""
                Text(
                  text =
                    "LAST ${formatSurvivalDurationSec(entry.lastSurvivalSec)} • MAX ${formatSurvivalDurationSec(entry.maxSurvivalSec)}$botPart",
                  color = Color(0xFF94A3B8),
                  fontSize = 6.9.sp,
                  fontFamily = FontFamily.Monospace,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
              Text(
                text = "AVG ${formatSurvivalDurationSec(entry.avgSurvivalSec)}",
                color = Color(0xFF00F5D4),
                fontSize = 7.8.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
              )
            }
          }
        }

        else -> {
          if (realHumanLeaders.isEmpty()) {
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
                text = "REAL-PLAYER HALL OF FAME SYNCS WHEN ONLINE.",
                color = Color(0xFF94A3B8),
                fontSize = 7.8.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
              )
            }
          } else {
            realHumanLeaders.forEachIndexed { idx, entry ->
              val cleanTag =
                entry.syndicateTag.trim().uppercase(Locale.US).takeIf {
                  it !in setOf("GUEST", "RFT", "NONE", "SOLO", "UNAFFILIATED", "FREE", "BOT")
                }.orEmpty()
              val label = if (cleanTag.isNotEmpty()) "[${cleanTag}] ${entry.callsign}" else entry.callsign
              Row(
                modifier =
                  Modifier.fillMaxWidth()
                    .chamferedSciFiPodSurface(
                      isRightSide = true,
                      primaryBorder = if (idx == 0) Color(0xFFFFD700) else Color(0xFF38BDF8),
                      secondaryBorder = Color(0xFF091528),
                    )
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text(
                  text = "#${idx + 1} $label",
                  color = Color.White,
                  fontSize = 8.0.sp,
                  fontWeight = FontWeight.Black,
                  fontFamily = FontFamily.Monospace,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                  modifier = Modifier.weight(1f).padding(end = 6.dp),
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                  ResonanceCrestIcon(sizeDp = 11.dp)
                  Text(
                    text = String.format(Locale.US, "%,d", entry.allTimeBounty.coerceAtLeast(0L)),
                    color = Color(0xFFFFD700),
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
    }
  }
}

@Composable
private fun HelmLeftSystemsBay(
  isTabletDeck: Boolean,
  headerHeight: Dp,
  wingStackGap: Dp,
  sfxEnabled: Boolean,
  musicEnabled: Boolean,
  musicBpm: Int,
  musicVolume: Float,
  sfxVolume: Float,
  cameraZoom: Float,
  playfieldTextScale: Float,
  onToggleSfx: (Boolean) -> Unit,
  onToggleMusic: (Boolean) -> Unit,
  onChangeMusicBpm: (Int) -> Unit,
  onChangeMusicVolume: (Float) -> Unit,
  onChangeSfxVolume: (Float) -> Unit,
  onChangeCameraZoom: (Float) -> Unit,
  onChangePlayfieldTextScale: (Float) -> Unit,
  onOpenChangelogModal: () -> Unit,
  onOpenAudioSynthForge: () -> Unit,
  onOpenStarForge: () -> Unit = {},
  onStartTutorial: () -> Unit = {},
  isNetworkOnline: Boolean = true,
  isGoogleAuthenticated: Boolean = true,
  cloudSyncHealth: CloudSyncHealth = CloudSyncHealth(),
  updateStatusLabel: String = "",
  hasUpdateAvailable: Boolean = false,
  onCheckOrApplyUpdate: () -> Unit = {},
  onSwitchAccount: () -> Unit = {},
  onBackToHub: () -> Unit,
) {
  Column(
    modifier = Modifier.fillMaxSize().testTag("helm_left_systems_bay"),
    verticalArrangement = Arrangement.spacedBy(wingStackGap),
  ) {
    HubDigitizedReturnBar(
      heightDp = headerHeight,
      isRightSide = false,
      isTabletDeck = isTabletDeck,
      sectionTitle = "HELM SYSTEMS",
      accent = Color(0xFF00F5D4),
      trailingLabel = "PATCH LOG",
      trailingTag = "helm_open_changelog",
      onTrailingClick = onOpenChangelogModal,
      onBackToHub = onBackToHub,
    )

    Column(
      modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(wingStackGap),
    ) {
      // Solo Flight Tutorial Launch Row + Star Forge 1:1 Entry
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        Box(
          modifier =
            Modifier.weight(0.95f)
              .minimumInteractiveComponentSize()
              .height(48.dp)
              .clickable { onStartTutorial() }
              .padding(vertical = 2.dp)
              .testTag("helm_start_tutorial"),
          contentAlignment = Alignment.Center,
        ) {
          Box(
            modifier =
              Modifier.fillMaxSize()
                .clip(RoundedCornerShape(8.dp))
                .background(Brush.horizontalGradient(listOf(Color(0xFF0D2D46), Color(0xFF091A2E))))
                .border(1.2.dp, Color(0xFFFACC15), RoundedCornerShape(8.dp))
                .padding(horizontal = 6.dp),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text = "TUTORIAL >",
              color = Color(0xFFFACC15),
              fontSize = if (isTabletDeck) 9.2.sp else 8.4.sp,
              fontWeight = FontWeight.Black,
              fontFamily = FontFamily.Monospace,
              maxLines = 1,
            )
          }
        }
        Box(
          modifier =
            Modifier.weight(1.25f)
              .minimumInteractiveComponentSize()
              .height(48.dp)
              .clickable { onOpenStarForge() }
              .padding(vertical = 2.dp)
              .testTag("helm_open_star_forge"),
          contentAlignment = Alignment.Center,
        ) {
          Box(
            modifier =
              Modifier.fillMaxSize()
                .clip(RoundedCornerShape(8.dp))
                .background(Brush.horizontalGradient(listOf(Color(0xFF23103C), Color(0xFF0D253E))))
                .border(
                  1.2.dp,
                  Brush.horizontalGradient(listOf(Color(0xFFC084FC), Color(0xFF00F5D4), Color(0xFFFFD700))),
                  RoundedCornerShape(8.dp),
                )
                .padding(horizontal = 6.dp),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text = "STAR FORGE 1:1 >",
              color = Color(0xFFF3E8FF),
              fontSize = if (isTabletDeck) 9.0.sp else 8.2.sp,
              fontWeight = FontWeight.Black,
              fontFamily = FontFamily.Monospace,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        Box(
          modifier =
            Modifier.weight(1f)
              .minimumInteractiveComponentSize()
              .height(48.dp)
              .chamferedSciFiPodSurface(
                isRightSide = false,
                primaryBorder = if (musicEnabled) Color(0xFF00F5D4) else Color(0xFF475569),
                secondaryBorder = Color(0xFF091528),
              )
              .clickable { onToggleMusic(!musicEnabled) }
              .testTag("helm_toggle_music"),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = if (musicEnabled) "MUSIC: ON" else "MUSIC: OFF",
            color = if (musicEnabled) Color(0xFF00F5D4) else Color(0xFF94A3B8),
            fontSize = 8.0.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
          )
        }
        Box(
          modifier =
            Modifier.weight(1f)
              .minimumInteractiveComponentSize()
              .height(48.dp)
              .chamferedSciFiPodSurface(
                isRightSide = false,
                primaryBorder = if (sfxEnabled) Color(0xFF00F5D4) else Color(0xFF475569),
                secondaryBorder = Color(0xFF091528),
              )
              .clickable { onToggleSfx(!sfxEnabled) }
              .testTag("helm_toggle_sfx"),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = if (sfxEnabled) "SFX: ON" else "SFX: OFF",
            color = if (sfxEnabled) Color(0xFF00F5D4) else Color(0xFF94A3B8),
            fontSize = 8.0.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
          )
        }
      }

      HelmStepperControlRow(
        label = "MUSIC VOL",
        valueText = "${(musicVolume * 100f).roundToInt()}%",
        onMinus = { onChangeMusicVolume((musicVolume - 0.10f).coerceIn(0f, 1f)) },
        onPlus = { onChangeMusicVolume((musicVolume + 0.10f).coerceIn(0f, 1f)) },
      )
      HelmStepperControlRow(
        label = "SFX VOL",
        valueText = "${(sfxVolume * 100f).roundToInt()}%",
        onMinus = { onChangeSfxVolume((sfxVolume - 0.10f).coerceIn(0f, 1f)) },
        onPlus = { onChangeSfxVolume((sfxVolume + 0.10f).coerceIn(0f, 1f)) },
      )
      HelmStepperControlRow(
        label = "SYNTH BPM",
        valueText = "$musicBpm",
        onMinus = { onChangeMusicBpm((musicBpm - 6).coerceIn(96, 168)) },
        onPlus = { onChangeMusicBpm((musicBpm + 6).coerceIn(96, 168)) },
      )
      HelmStepperControlRow(
        label = "ARENA ZOOM",
        valueText = String.format(Locale.US, "%.2fx", cameraZoom),
        onMinus = { onChangeCameraZoom((cameraZoom - 0.08f).coerceIn(0.65f, 1.45f)) },
        onPlus = { onChangeCameraZoom((cameraZoom + 0.08f).coerceIn(0.65f, 1.45f)) },
      )
      HelmStepperControlRow(
        label = "HUD TEXT",
        valueText = String.format(Locale.US, "%.2fx", playfieldTextScale),
        onMinus = { onChangePlayfieldTextScale((playfieldTextScale - 0.08f).coerceIn(0.80f, 1.35f)) },
        onPlus = { onChangePlayfieldTextScale((playfieldTextScale + 0.08f).coerceIn(0.80f, 1.35f)) },
      )

      Box(
        modifier =
          Modifier.fillMaxWidth()
            .minimumInteractiveComponentSize()
            .height(48.dp)
            .clickable { onOpenAudioSynthForge() }
            .padding(vertical = 2.dp)
            .testTag("helm_open_audio_synth_forge"),
        contentAlignment = Alignment.Center,
      ) {
        Box(
          modifier =
            Modifier.fillMaxWidth()
              .clip(RoundedCornerShape(7.dp))
              .background(Color(0xFF0D2D46))
              .border(1.dp, Color(0xFFFFD700), RoundedCornerShape(7.dp))
              .padding(vertical = 6.dp),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = "AUDIO SYNTH LAB >",
            color = Color(0xFFFFD700),
            fontSize = 8.0.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
          )
        }
      }

      // Cloud Sync Health Pod
      val syncHealthy = isNetworkOnline && cloudSyncHealth.isConnected
      Column(
        modifier =
          Modifier.fillMaxWidth()
            .chamferedSciFiPodSurface(
              isRightSide = false,
              primaryBorder = if (syncHealthy) Color(0xFF00F5D4) else Color(0xFFFACC15),
              secondaryBorder = Color(0xFF091528),
            )
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .testTag("helm_cloud_sync_health_pod"),
        verticalArrangement = Arrangement.spacedBy(2.dp),
      ) {
        Text(
          text =
            when {
              !isNetworkOnline -> "CLOUD SYNC • OFFLINE LOCAL"
              syncHealthy -> "CLOUD SYNC • LIVE CONNECTED"
              isGoogleAuthenticated -> "CLOUD SYNC • RECONNECTING"
              else -> "CLOUD SYNC • LOCAL PILOT"
            },
          color = if (syncHealthy) Color(0xFF00F5D4) else Color(0xFFFACC15),
          fontSize = 7.8.sp,
          fontWeight = FontWeight.Black,
          fontFamily = FontFamily.Monospace,
        )
        Text(
          text = cloudSyncHealth.statusLabel.ifBlank { "Realtime sector presence & profile uplink." },
          color = Color(0xFF94A3B8),
          fontSize = 7.0.sp,
          fontFamily = FontFamily.Monospace,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
      }

      // App Update Check & Account Switch Row
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        Box(
          modifier =
            Modifier.weight(1f)
              .minimumInteractiveComponentSize()
              .height(48.dp)
              .chamferedSciFiPodSurface(
                isRightSide = false,
                primaryBorder = if (hasUpdateAvailable) Color(0xFFFFD700) else Color(0xFF38BDF8),
                secondaryBorder = Color(0xFF091528),
              )
              .clickable { onCheckOrApplyUpdate() }
              .padding(horizontal = 4.dp)
              .testTag("helm_check_update"),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text =
              when {
                hasUpdateAvailable -> "APPLY UPDATE >"
                updateStatusLabel.isNotBlank() -> updateStatusLabel.uppercase(Locale.US)
                else -> "CHECK UPDATE"
              },
            color = if (hasUpdateAvailable) Color(0xFFFFD700) else Color(0xFF38BDF8),
            fontSize = 7.4.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
        Box(
          modifier =
            Modifier.weight(1f)
              .minimumInteractiveComponentSize()
              .height(48.dp)
              .chamferedSciFiPodSurface(
                isRightSide = false,
                primaryBorder = Color(0xFFFACC15),
                secondaryBorder = Color(0xFF091528),
              )
              .clickable { onSwitchAccount() }
              .padding(horizontal = 4.dp)
              .testTag("helm_switch_account"),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = "SWITCH ACCOUNT",
            color = Color(0xFFFACC15),
            fontSize = 7.4.sp,
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
private fun RiftFrameLeftLoadoutBay(
  isTabletDeck: Boolean,
  headerHeight: Dp,
  wingStackGap: Dp,
  activeCustomSlot: CustomShipLoadoutSlot,
  activeCustomSlotId: String,
  pointBudget: Int,
  isCharged: Boolean,
  shipResonance: Int,
  localPrefs: LocalPilotPrefs,
  currentUserId: String,
  onBackToHub: () -> Unit,
  onSelectFrameSlot: (String) -> Unit,
  onSaveCustomSlot: (CustomShipLoadoutSlot) -> Unit,
  onPreviewInOverwatch: () -> Unit,
  onChargeRiftFrame: () -> Unit,
) {
  val density = LocalDensity.current
  val uidOrNull = currentUserId.ifBlank { null }
  val normalizedActiveId = localPrefs.normalizeCustomSlotId(activeCustomSlotId)
  var draftSlot by remember(activeCustomSlot, normalizedActiveId) {
    mutableStateOf(activeCustomSlot.copy(slotId = normalizedActiveId))
  }
  val primaryOptions = remember { CustomShipLoadoutSlot.PRIMARY_OPTIONS }
  val secondaryOptions = remember(RiftSpriteBaker.customShip9Version) {
    CustomShipLoadoutSlot.getAllAvailableSecondaryOptions()
  }
  val usedPoints = draftSlot.totalAllocatedPoints()
  val remainingPoints = pointBudget - usedPoints

  fun commitDraft(updated: CustomShipLoadoutSlot) {
    if (updated.totalAllocatedPoints() <= pointBudget) {
      draftSlot = updated
      onSaveCustomSlot(updated)
    }
  }

  Column(
    modifier = Modifier.fillMaxSize().testTag("rift_frame_left_loadout_bay"),
    verticalArrangement = Arrangement.spacedBy(wingStackGap),
  ) {
    HubDigitizedReturnBar(
      heightDp = headerHeight,
      isRightSide = false,
      isTabletDeck = isTabletDeck,
      sectionTitle = "RIFT FRAME • ${usedPoints}/${pointBudget}P",
      accent = Color(0xFFC084FC),
      trailingLabel = "PREVIEW >",
      trailingTag = "rift_frame_preview_overwatch",
      onTrailingClick = onPreviewInOverwatch,
      onBackToHub = onBackToHub,
    )

    // Frame A..E Loadout Selector Pills
    Row(
      modifier =
        Modifier.fillMaxWidth()
          .height(48.dp)
          .chamferedSciFiPodSurface(
            isRightSide = false,
            primaryBorder = Color(0xFFC084FC),
            secondaryBorder = Color(0xFF00F5D4),
          )
          .padding(horizontal = 4.dp),
      horizontalArrangement = Arrangement.spacedBy(3.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      listOf("A", "B", "C", "D", "E").forEach { shortKey ->
        val fullSlotId = localPrefs.normalizeCustomSlotId(shortKey)
        val isSelected = normalizedActiveId == fullSlotId
        val isUnlocked = localPrefs.isCustomSlotUnlocked(fullSlotId, uidOrNull)
        Box(
          modifier =
            Modifier.weight(1f)
              .fillMaxHeight()
              .minimumInteractiveComponentSize()
              .clickable { onSelectFrameSlot(fullSlotId) }
              .padding(vertical = 5.dp)
              .testTag("rift_frame_slot_$shortKey"),
          contentAlignment = Alignment.Center,
        ) {
          Box(
            modifier =
              Modifier.fillMaxSize()
                .clip(RoundedCornerShape(6.dp))
                .background(
                  when {
                    isSelected -> Color(0x44C084FC)
                    isUnlocked -> Color(0x66060D1A)
                    else -> Color(0x6602050B)
                  }
                )
                .border(
                  1.dp,
                  when {
                    isSelected -> Color(0xFF00F5D4)
                    isUnlocked -> Color(0xFF38BDF8)
                    else -> Color(0x66FACC15)
                  },
                  RoundedCornerShape(6.dp),
                ),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text = if (isUnlocked) shortKey else "${shortKey}*",
              color =
                when {
                  isSelected -> Color.White
                  isUnlocked -> Color(0xFFE2E8F0)
                  else -> Color(0xFFFACC15)
                },
              fontSize = 8.4.sp,
              fontWeight = FontWeight.Black,
              fontFamily = FontFamily.Monospace,
            )
          }
        }
      }
    }

    Column(
      modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(wingStackGap),
    ) {
      // Parity Budget Readout
      Row(
        modifier =
          Modifier.fillMaxWidth()
            .chamferedSciFiPodSurface(
              isRightSide = false,
              primaryBorder = if (remainingPoints == 0) Color(0xFF00F5D4) else Color(0xFFFFD700),
              secondaryBorder = Color(0xFF091528),
            )
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = draftSlot.customName.ifBlank { "RIFT FRAME" }.uppercase(Locale.US),
          color = Color.White,
          fontSize = if (isTabletDeck) 9.0.sp else 8.0.sp,
          fontWeight = FontWeight.Black,
          fontFamily = FontFamily.Monospace,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.weight(1f).padding(end = 6.dp),
        )
        Text(
          text = if (remainingPoints == 0) "100P PARITY LOCKED" else "${remainingPoints}P OPEN",
          color = if (remainingPoints == 0) Color(0xFF00F5D4) else Color(0xFFFFD700),
          fontSize = 7.6.sp,
          fontWeight = FontWeight.Black,
          fontFamily = FontFamily.Monospace,
        )
      }

      // Primary Weapon Selector
      val curPriIdx = primaryOptions.indexOfFirst { it.id == draftSlot.primaryArchetype }.let { if (it < 0) 0 else it }
      val curPri = primaryOptions[curPriIdx]
      HelmStepperControlRow(
        label = "PRI: ${curPri.shortLabel}",
        valueText = "${curPri.pointCost}P",
        onMinus = {
          val prev = primaryOptions[if (curPriIdx <= 0) primaryOptions.lastIndex else curPriIdx - 1]
          val nextSlot = draftSlot.copy(primaryArchetype = prev.id)
          if (nextSlot.totalAllocatedPoints() <= pointBudget) commitDraft(nextSlot)
        },
        onPlus = {
          val next = primaryOptions[(curPriIdx + 1) % primaryOptions.size]
          val nextSlot = draftSlot.copy(primaryArchetype = next.id)
          if (nextSlot.totalAllocatedPoints() <= pointBudget) commitDraft(nextSlot)
        },
      )

      // Secondary Ability Selector
      val curSecIdx = secondaryOptions.indexOfFirst { it.id == draftSlot.secondaryType }.let { if (it < 0) 0 else it }
      val curSec = secondaryOptions[curSecIdx]
      HelmStepperControlRow(
        label = "SEC: ${curSec.shortLabel}",
        valueText = "${curSec.pointCost}P",
        onMinus = {
          val prev = secondaryOptions[if (curSecIdx <= 0) secondaryOptions.lastIndex else curSecIdx - 1]
          val nextSlot = draftSlot.copy(secondaryType = prev.id)
          if (nextSlot.totalAllocatedPoints() <= pointBudget) commitDraft(nextSlot)
        },
        onPlus = {
          val next = secondaryOptions[(curSecIdx + 1) % secondaryOptions.size]
          val nextSlot = draftSlot.copy(secondaryType = next.id)
          if (nextSlot.totalAllocatedPoints() <= pointBudget) commitDraft(nextSlot)
        },
      )

      // 5 Hull Stat Point Steppers (Speed, Thrust, Turn, Max Flux, Recharge)
      HelmStepperControlRow(
        label = "TOP SPEED",
        valueText = "${draftSlot.speedPts}P",
        onMinus = { commitDraft(draftSlot.copy(speedPts = (draftSlot.speedPts - 2).coerceAtLeast(0))) },
        onPlus = {
          val step = minOf(2, remainingPoints.coerceAtLeast(0))
          if (step > 0) commitDraft(draftSlot.copy(speedPts = (draftSlot.speedPts + step).coerceAtMost(45)))
        },
      )
      HelmStepperControlRow(
        label = "THRUST",
        valueText = "${draftSlot.thrustPts}P",
        onMinus = { commitDraft(draftSlot.copy(thrustPts = (draftSlot.thrustPts - 2).coerceAtLeast(0))) },
        onPlus = {
          val step = minOf(2, remainingPoints.coerceAtLeast(0))
          if (step > 0) commitDraft(draftSlot.copy(thrustPts = (draftSlot.thrustPts + step).coerceAtMost(45)))
        },
      )
      HelmStepperControlRow(
        label = "TURN RATE",
        valueText = "${draftSlot.turnPts}P",
        onMinus = { commitDraft(draftSlot.copy(turnPts = (draftSlot.turnPts - 2).coerceAtLeast(0))) },
        onPlus = {
          val step = minOf(2, remainingPoints.coerceAtLeast(0))
          if (step > 0) commitDraft(draftSlot.copy(turnPts = (draftSlot.turnPts + step).coerceAtMost(45)))
        },
      )
      HelmStepperControlRow(
        label = "MAX FLUX",
        valueText = "${draftSlot.maxFluxPts}P",
        onMinus = { commitDraft(draftSlot.copy(maxFluxPts = (draftSlot.maxFluxPts - 2).coerceAtLeast(0))) },
        onPlus = {
          val step = minOf(2, remainingPoints.coerceAtLeast(0))
          if (step > 0) commitDraft(draftSlot.copy(maxFluxPts = (draftSlot.maxFluxPts + step).coerceAtMost(45)))
        },
      )
      HelmStepperControlRow(
        label = "FLUX RECHARGE",
        valueText = "${draftSlot.rechargePts}P",
        onMinus = { commitDraft(draftSlot.copy(rechargePts = (draftSlot.rechargePts - 2).coerceAtLeast(0))) },
        onPlus = {
          val step = minOf(2, remainingPoints.coerceAtLeast(0))
          if (step > 0) commitDraft(draftSlot.copy(rechargePts = (draftSlot.rechargePts + step).coerceAtMost(45)))
        },
      )

      // Charge Rift Frame Button
      val chargeFontSp = if (isTabletDeck) 9.4.sp else 8.4.sp
      val fluxDp = with(density) { chargeFontSp.toDp() }
      Box(
        modifier =
          Modifier.fillMaxWidth()
            .minimumInteractiveComponentSize()
            .height(48.dp)
            .clickable {
              onSaveCustomSlot(draftSlot)
              onChargeRiftFrame()
            }
            .padding(vertical = 3.dp)
            .testTag("rift_frame_charge_btn"),
        contentAlignment = Alignment.Center,
      ) {
        Row(
          modifier =
            Modifier.fillMaxWidth()
              .clip(RoundedCornerShape(8.dp))
              .background(Brush.horizontalGradient(listOf(Color(0xFF1A103D), Color(0xFF0D3349))))
              .border(
                1.2.dp,
                Brush.horizontalGradient(listOf(Color(0xFFC084FC), Color(0xFF00F5D4), Color(0xFFFFD700))),
                RoundedCornerShape(8.dp),
              )
              .padding(horizontal = 10.dp, vertical = 7.dp),
          horizontalArrangement = Arrangement.Center,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = if (isCharged) "RIFT FRAME CHARGED " else "CHARGE RIFT FRAME ",
            color = Color.White,
            fontSize = chargeFontSp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
          )
          FluxToroidIcon(sizeDp = fluxDp)
        }
      }
    }
  }
}

@Composable
private fun RiftFrameRightCosmeticsBay(
  isTabletDeck: Boolean,
  headerHeight: Dp,
  wingStackGap: Dp,
  shardText: String,
  peakResonance: Int,
  shipSpinDeg: Float,
  fluxOrbitRad: Float,
  isCharged: Boolean,
  localPrefs: LocalPilotPrefs,
  currentUserId: String,
  onShareBuild: () -> Unit,
  onInspectCosmetic: (CosmeticCatalogItem, Boolean, Boolean) -> Unit,
) {
  val uidOrNull = currentUserId.ifBlank { null }
  var categoryFilter by remember { mutableStateOf("ALL") }
  val allCosmetics = remember {
    RiftProgressionCatalog.hullTrims +
      RiftProgressionCatalog.killFinishers +
      RiftProgressionCatalog.warpSignatures
  }
  val filteredCosmetics =
    remember(categoryFilter, allCosmetics) {
      when (categoryFilter) {
        "TRIM" -> RiftProgressionCatalog.hullTrims
        "FINISH" -> RiftProgressionCatalog.killFinishers
        "WARP" -> RiftProgressionCatalog.warpSignatures
        else -> allCosmetics
      }
    }

  Column(
    modifier = Modifier.fillMaxSize().testTag("rift_frame_right_cosmetics_bay"),
    verticalArrangement = Arrangement.spacedBy(wingStackGap),
  ) {
    HubDigitizedReturnBar(
      heightDp = headerHeight,
      isRightSide = true,
      isTabletDeck = isTabletDeck,
      sectionTitle = "COSMETIC BUFFS",
      accent = Color(0xFFFFD700),
      trailingLabel = "SHARE >",
      trailingTag = "rift_frame_share_build",
      onTrailingClick = onShareBuild,
    )

    // Live 2.5D Rift Frame Preview + Peak Resonance & Shards Readout
    Row(
      modifier =
        Modifier.fillMaxWidth()
          .chamferedSciFiPodSurface(
            isRightSide = true,
            primaryBorder = Color(0xFFC084FC),
            secondaryBorder = Color(0xFF00F5D4),
          )
          .padding(horizontal = 8.dp, vertical = 5.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Spinning25dShipWithOrbitingFlux(
        shipId = 10,
        shipSpinDeg = shipSpinDeg,
        fluxOrbitRad = fluxOrbitRad,
        isCharged = isCharged,
        isBestShip = false,
        isSelected = true,
        sizeDp = if (isTabletDeck) 52.dp else 42.dp,
      )
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
          text = "RESONANCE COSMETICS",
          color = Color.White,
          fontSize = if (isTabletDeck) 8.8.sp else 7.8.sp,
          fontWeight = FontWeight.Black,
          fontFamily = FontFamily.Monospace,
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
          ResonanceCrestIcon(sizeDp = 10.dp)
          Text(
            text = "PEAK ${formatOverwatchResonance(peakResonance)}",
            color = Color(0xFFFFD700),
            fontSize = 7.4.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
          )
          RiftShardGemIcon(sizeDp = 10.dp)
          Text(
            text = shardText,
            color = Color(0xFF00F5D4),
            fontSize = 7.4.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
          )
        }
      }
    }

    // Cosmetic Category Filter Row (>= 48dp touch targets)
    Row(
      modifier =
        Modifier.fillMaxWidth()
          .height(48.dp)
          .chamferedSciFiPodSurface(
            isRightSide = true,
            primaryBorder = Color(0xFF38BDF8),
            secondaryBorder = Color(0xFF091528),
          )
          .padding(horizontal = 4.dp),
      horizontalArrangement = Arrangement.spacedBy(3.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      listOf("ALL", "TRIM", "FINISH", "WARP").forEach { tab ->
        val sel = categoryFilter == tab
        Box(
          modifier =
            Modifier.weight(1f)
              .fillMaxHeight()
              .minimumInteractiveComponentSize()
              .clickable { categoryFilter = tab }
              .padding(vertical = 6.dp)
              .testTag("rift_frame_cosmetic_tab_${tab.lowercase(Locale.US)}"),
          contentAlignment = Alignment.Center,
        ) {
          Box(
            modifier =
              Modifier.fillMaxSize()
                .clip(RoundedCornerShape(5.dp))
                .background(if (sel) Color(0x38C084FC) else Color(0x6602050B))
                .border(1.dp, if (sel) Color(0xFFC084FC) else Color(0x4438BDF8), RoundedCornerShape(5.dp)),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text = tab,
              color = if (sel) Color.White else Color(0xFF94A3B8),
              fontSize = 7.6.sp,
              fontWeight = FontWeight.Black,
              fontFamily = FontFamily.Monospace,
            )
          }
        }
      }
    }

    Column(
      modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(wingStackGap),
    ) {
      filteredCosmetics.forEach { item ->
        val unlockedByResonance = item.freeAtResonance > 0 && peakResonance >= item.freeAtResonance
        val unlocked = unlockedByResonance || localPrefs.isCosmeticUnlocked(item.category, item.id, uid = uidOrNull)
        val equipped = localPrefs.getEquippedCosmeticId(item.category, uidOrNull) == item.id
        Row(
          modifier =
            Modifier.fillMaxWidth()
              .minimumInteractiveComponentSize()
              .chamferedSciFiPodSurface(
                isRightSide = true,
                primaryBorder = if (equipped) Color(0xFF00F5D4) else Color(0xFF38BDF8),
                secondaryBorder = if (unlocked) Color(0xFFFFD700) else Color(0xFF091528),
              )
              .clickable { onInspectCosmetic(item, unlocked, equipped) }
              .padding(horizontal = 8.dp, vertical = 6.dp)
              .testTag("rift_frame_cosmetic_${item.category.lowercase(Locale.US)}_${item.id}"),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f).padding(end = 6.dp)) {
            Text(
              text = item.name.uppercase(Locale.US),
              color = Color.White,
              fontSize = if (isTabletDeck) 8.6.sp else 7.8.sp,
              fontWeight = FontWeight.Black,
              fontFamily = FontFamily.Monospace,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Text(
              text =
                if (item.freeAtResonance > 0)
                  "${item.category.replace("_", " ")} • FREE @ ${item.freeAtResonance} PEAK"
                else item.category.replace("_", " "),
              color = Color(0xFF94A3B8),
              fontSize = 7.8.sp,
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.Monospace,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
          Text(
            text =
              when {
                equipped -> "ACTIVE"
                unlocked -> "EQUIP >"
                else -> "${item.shardCost}S"
              },
            color =
              when {
                equipped -> Color(0xFF00F5D4)
                unlocked -> Color(0xFFFACC15)
                else -> Color(0xFFCBD5E1)
              },
            fontSize = 8.4.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
          )
        }
      }
    }
  }
}

@Composable
private fun ShardStoreLeftSummaryBay(
  isTabletDeck: Boolean,
  headerHeight: Dp,
  wingStackGap: Dp,
  shardText: String,
  liveResonance: Int,
  peakResonance: Int,
  activeCustomSlotId: String,
  localPrefs: LocalPilotPrefs,
  currentUserId: String,
  onBackToHub: () -> Unit,
  onSelectFrameSlot: (String) -> Unit,
  onInspectFrameSlot: (com.example.engine.FrameSlotGateInfo, String, Boolean, Boolean) -> Unit =
    { _, slotId, _, _ -> onSelectFrameSlot(slotId) },
  onOpenRiftFrameBay: () -> Unit,
  onOpenConduitQuests: () -> Unit,
) {
  val uidOrNull = currentUserId.ifBlank { null }
  val normalizedActiveId = localPrefs.normalizeCustomSlotId(activeCustomSlotId)

  Column(
    modifier = Modifier.fillMaxSize().testTag("shard_store_left_summary_bay"),
    verticalArrangement = Arrangement.spacedBy(wingStackGap),
  ) {
    HubDigitizedReturnBar(
      heightDp = headerHeight,
      isRightSide = false,
      isTabletDeck = isTabletDeck,
      sectionTitle = "SHARD STORE",
      accent = Color(0xFF00F5D4),
      trailingLabel = "QUESTS >",
      trailingTag = "shard_vault_open_quests",
      onTrailingClick = onOpenConduitQuests,
      onBackToHub = onBackToHub,
    )

    Column(
      modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(wingStackGap),
    ) {
      // Live Shards & Resonance Balance Card
      Column(
        modifier =
          Modifier.fillMaxWidth()
            .chamferedSciFiPodSurface(
              isRightSide = false,
              primaryBorder = Color(0xFF00F5D4),
              secondaryBorder = Color(0xFFFFD700),
            )
            .padding(horizontal = 9.dp, vertical = 7.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          RiftShardGemIcon(sizeDp = 16.dp)
          Text(
            text = "$shardText RIFT SHARDS",
            color = Color(0xFF00F5D4),
            fontSize = if (isTabletDeck) 10.8.sp else 9.6.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
          )
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
          ResonanceCrestIcon(sizeDp = 12.dp)
          Text(
            text = "LIVE ${formatOverwatchResonance(liveResonance)} • PEAK ${formatOverwatchResonance(peakResonance)}",
            color = Color(0xFFFFD700),
            fontSize = 8.4.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
          )
        }
        Text(
          text = "Tap any Frame Bay below or Cosmetic on the right to preview & unlock in Overwatch.",
          color = Color(0xFFCBD5E1),
          fontSize = 8.0.sp,
          fontFamily = FontFamily.SansSerif,
        )
      }

      // Frame Slots A..E Unlock Ledger
      RiftProgressionCatalog.frameSlotGates.forEach { gate ->
        val slotId = localPrefs.normalizeCustomSlotId(gate.slotId)
        val unlocked = localPrefs.isCustomSlotUnlocked(slotId, uidOrNull) || peakResonance >= gate.freeAtResonance
        val selected = normalizedActiveId == slotId
        Row(
          modifier =
            Modifier.fillMaxWidth()
              .minimumInteractiveComponentSize()
              .chamferedSciFiPodSurface(
                isRightSide = false,
                primaryBorder = if (selected) Color(0xFF00F5D4) else Color(0xFF38BDF8),
                secondaryBorder = if (unlocked) Color(0xFFC084FC) else Color(0xFFFACC15),
              )
              .clickable { onInspectFrameSlot(gate, slotId, unlocked, selected) }
              .padding(horizontal = 8.dp, vertical = 6.dp)
              .testTag("shard_vault_slot_${slotId.takeLast(1)}"),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f).padding(end = 6.dp)) {
            Text(
              text = "${gate.slotId.uppercase(Locale.US)} • ${gate.archetypeTitle.uppercase(Locale.US)}",
              color = Color.White,
              fontSize = 8.8.sp,
              fontWeight = FontWeight.Black,
              fontFamily = FontFamily.Monospace,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Text(
              text =
                if (gate.freeAtResonance <= 0) "OPEN AT 0 • 100P PARITY"
                else "FREE @ ${gate.freeAtResonance} PEAK OR ${gate.shardCost} SHARDS",
              color = Color(0xFF94A3B8),
              fontSize = 7.8.sp,
              fontFamily = FontFamily.Monospace,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
          Text(
            text =
              when {
                selected -> "ACTIVE"
                unlocked -> "SELECT >"
                else -> "${gate.shardCost}S"
              },
            color =
              when {
                selected -> Color(0xFF00F5D4)
                unlocked -> Color(0xFFC084FC)
                else -> Color(0xFFFFD700)
              },
            fontSize = 8.4.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
          )
        }
      }

      Box(
        modifier =
          Modifier.fillMaxWidth()
            .minimumInteractiveComponentSize()
            .height(48.dp)
            .clickable { onOpenRiftFrameBay() }
            .padding(vertical = 3.dp)
            .testTag("shard_vault_open_rift_frame"),
        contentAlignment = Alignment.Center,
      ) {
        Box(
          modifier =
            Modifier.fillMaxWidth()
              .clip(RoundedCornerShape(7.dp))
              .background(Color(0xFF1A1038))
              .border(1.dp, Color(0xFFC084FC), RoundedCornerShape(7.dp))
              .padding(vertical = 7.dp),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = "CONFIGURE RIFT FRAME >",
            color = Color(0xFFE9D5FF),
            fontSize = 8.0.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
          )
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
private fun HelmStepperControlRow(
  label: String,
  valueText: String,
  onMinus: () -> Unit,
  onPlus: () -> Unit,
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

@Composable
private fun HelmRightCodexBay(
  isTabletDeck: Boolean,
  headerHeight: Dp,
  wingStackGap: Dp,
  @Suppress("UNUSED_PARAMETER") initialSubDeck: Int = 0,
  onInspectCodexStage: (OverwatchCircularPrompt.InfoStageInspect) -> Unit = {},
  onInspectShipStage: (Int) -> Unit = {},
) {
  var selectedCategory by remember { mutableIntStateOf(0) }
  val categories = remember {
    listOf(
      "BASICS" to Color(0xFF00F5D4),
      "MODES" to Color(0xFFFACC15),
      "BUFFS" to Color(0xFF34D399),
      "RIFTS" to Color(0xFFA855F7),
      "SHIPS" to Color(0xFF38BDF8),
    )
  }

  val howToCards = remember {
    listOf(
      Triple(
        "TWIN-STICK FLIGHT & SHARED FLUX",
        "FLIGHT",
        "Left stick controls thrust and heading; right stick or touch controls primary fire, secondary ordnance, brake, and Special Crate buffs. Your Flux ring powers BOTH shields and weapons—feather shots so your reactor recharges.",
      ),
      Triple(
        "PER-SHIP RESONANCE & 100-PT PARITY",
        "PARITY",
        "All 10 hulls (#1 Viper through #10 modular Rift Frame) are open at 0 Resonance on an identical 100-Point stat and weapon budget. Your highest-Resonance hull is highlighted in gold as your Best Ship.",
      ),
      Triple(
        "5X RIFT STATION SAFE HARBOR",
        "STATION",
        "Sector Convergence and Flux Core feature a central 5x ship-scale Rift Station Safe Zone. Weapons lock inside, and staying beyond 15 seconds (or firing before entry) triggers a 15-second Safe-Zone lockout.",
      ),
      Triple(
        "SYNDICATES, SQUADRONS & SHARD STORE",
        "SOCIAL",
        "Found or join human-chartered Syndicates, form up to 5-pilot Squadrons, queue into <=15s Resonance matchmaking, and unlock hull cosmetics & Frame bays in the Shard Store.",
      ),
    )
  }

  val modeCards = remember {
    listOf(
      Triple(
        "1. SECTOR CONVERGENCE",
        "OPEN 60M",
        "Always-running open sector dogfighting around the central Rift Station Safe Harbor. Harvest Supply Crates, ride Playfield Rifts, and build per-ship Resonance.",
      ),
      Triple(
        "2. FLUX CORE",
        "OBJECTIVE",
        "Capture and hold the golden Flux Core Singularity Orb in the center of the sector to earn continuous Resonance while broadcasting a radar beacon to rival pilots.",
      ),
      Triple(
        "3. RIFT SKIRMISH",
        "TEAM PVP",
        "Coordinated 2v2 to 5v5 Squadron battles matched against the closest-Resonance opponent squad within 15 seconds.",
      ),
      Triple(
        "4. RIFT CONTROL",
        "2 RINGS",
        "Contest and hold the Alpha and Omega Rift Control rings to accumulate team dominance points with no central Safe Zone.",
      ),
      Triple(
        "5. STASIS BREACH",
        "RESCUE",
        "Eliminated pilots are detained in the rival Brig Reactor. Crack the enemy Brig shield to free captured teammates or capture all rivals for an instant sweep.",
      ),
      Triple(
        "6. CUSTOM GAMES & SOLO TUTORIAL",
        "SANDBOX",
        "Host public or private room-code Custom Rifts with custom damage, Flux cost, and speed multipliers, or launch the Solo Tutorial anytime.",
      ),
    )
  }

  val allShips = remember(RiftSpriteBaker.customShip9Version) { RiftCodex.getAllTenShips() }

  Column(
    modifier = Modifier.fillMaxSize().testTag("helm_right_codex_bay"),
    verticalArrangement = Arrangement.spacedBy(wingStackGap),
  ) {
    Row(
      modifier =
        Modifier.fillMaxWidth()
          .height(headerHeight)
          .chamferedSciFiPodSurface(
            isRightSide = true,
            primaryBorder = Color(0xFFFFD700),
            secondaryBorder = Color(0xFF00F5D4),
          )
          .padding(horizontal = 8.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = "TACTICAL CODEX",
        color = Color(0xFFFFD700),
        fontSize = if (isTabletDeck) 10.4.sp else 9.2.sp,
        fontWeight = FontWeight.Black,
        fontFamily = FontFamily.Monospace,
      )
      Text(
        text = "TAP TO PREVIEW",
        color = Color(0xFF00F5D4),
        fontSize = 8.0.sp,
        fontWeight = FontWeight.Black,
        fontFamily = FontFamily.Monospace,
      )
    }

    // 5-Category Codex Selector Row (>= 48dp touch targets)
    Row(
      modifier =
        Modifier.fillMaxWidth()
          .height(48.dp)
          .chamferedSciFiPodSurface(
            isRightSide = true,
            primaryBorder = Color(0xFF38BDF8),
            secondaryBorder = Color(0xFF091528),
          )
          .padding(horizontal = 3.dp),
      horizontalArrangement = Arrangement.spacedBy(2.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      categories.forEachIndexed { idx, (label, col) ->
        val sel = selectedCategory == idx
        Box(
          modifier =
            Modifier.weight(1f)
              .fillMaxHeight()
              .minimumInteractiveComponentSize()
              .clickable { selectedCategory = idx }
              .padding(vertical = 5.dp)
              .testTag("helm_codex_tab_${idx}"),
          contentAlignment = Alignment.Center,
        ) {
          Box(
            modifier =
              Modifier.fillMaxSize()
                .clip(RoundedCornerShape(5.dp))
                .background(if (sel) col.copy(alpha = 0.24f) else Color(0x6602050B))
                .border(1.dp, if (sel) col else Color(0x4438BDF8), RoundedCornerShape(5.dp)),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text = label,
              color = if (sel) Color.White else Color(0xFF94A3B8),
              fontSize = if (isTabletDeck) 8.2.sp else 7.4.sp,
              fontWeight = FontWeight.Black,
              fontFamily = FontFamily.Monospace,
              maxLines = 1,
            )
          }
        }
      }
    }

    Column(
      modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(wingStackGap),
    ) {
      when (selectedCategory) {
        0 -> {
          howToCards.forEachIndexed { idx, (heading, badge, body) ->
            Column(
              modifier =
                Modifier.fillMaxWidth()
                  .minimumInteractiveComponentSize()
                  .chamferedSciFiPodSurface(
                    isRightSide = true,
                    primaryBorder = Color(0xFF00F5D4),
                    secondaryBorder = Color(0xFF091528),
                  )
                  .clickable {
                    onInspectCodexStage(
                      OverwatchCircularPrompt.InfoStageInspect(
                        kicker = "CODEX • FLIGHT BASICS",
                        title = heading,
                        badge = badge,
                        summary = body,
                        accentHex = 0xFF00F5D4L,
                        showStationPreview = idx == 2,
                        shipId = if (idx == 1) 1 else 0,
                      )
                    )
                  }
                  .padding(8.dp),
              verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text(
                  text = heading,
                  color = Color.White,
                  fontSize = if (isTabletDeck) 9.4.sp else 8.4.sp,
                  fontWeight = FontWeight.Black,
                  fontFamily = FontFamily.Monospace,
                  modifier = Modifier.weight(1f).padding(end = 6.dp),
                )
                Text(
                  text = "VIEW >",
                  color = Color(0xFF00F5D4),
                  fontSize = 7.8.sp,
                  fontWeight = FontWeight.Black,
                  fontFamily = FontFamily.Monospace,
                )
              }
              Text(
                text = body,
                color = Color(0xFFCBD5E1),
                fontSize = if (isTabletDeck) 8.6.sp else 7.8.sp,
                lineHeight = 11.0.sp,
                fontFamily = FontFamily.SansSerif,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }
        }
        1 -> {
          modeCards.forEachIndexed { idx, (heading, badge, body) ->
            Column(
              modifier =
                Modifier.fillMaxWidth()
                  .minimumInteractiveComponentSize()
                  .chamferedSciFiPodSurface(
                    isRightSide = true,
                    primaryBorder = Color(0xFFFACC15),
                    secondaryBorder = Color(0xFF091528),
                  )
                  .clickable {
                    onInspectCodexStage(
                      OverwatchCircularPrompt.InfoStageInspect(
                        kicker = "CODEX • GAME MODES",
                        title = heading,
                        badge = badge,
                        summary = body,
                        accentHex = 0xFFFACC15L,
                        showStationPreview = idx <= 1,
                      )
                    )
                  }
                  .padding(8.dp),
              verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text(
                  text = heading,
                  color = Color.White,
                  fontSize = if (isTabletDeck) 9.4.sp else 8.4.sp,
                  fontWeight = FontWeight.Black,
                  fontFamily = FontFamily.Monospace,
                  modifier = Modifier.weight(1f).padding(end = 6.dp),
                )
                Text(
                  text = badge,
                  color = Color(0xFFFACC15),
                  fontSize = 7.8.sp,
                  fontWeight = FontWeight.Black,
                  fontFamily = FontFamily.Monospace,
                )
              }
              Text(
                text = body,
                color = Color(0xFFCBD5E1),
                fontSize = if (isTabletDeck) 8.6.sp else 7.8.sp,
                lineHeight = 11.0.sp,
                fontFamily = FontFamily.SansSerif,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }
        }
        2 -> {
          RiftCodex.POWERUP_CODEX.forEach { buff ->
            Row(
              modifier =
                Modifier.fillMaxWidth()
                  .minimumInteractiveComponentSize()
                  .chamferedSciFiPodSurface(
                    isRightSide = true,
                    primaryBorder = Color(0xFF34D399),
                    secondaryBorder = Color(0xFF091528),
                  )
                  .clickable {
                    onInspectCodexStage(
                      OverwatchCircularPrompt.InfoStageInspect(
                        kicker = "SUPPLY CRATE BUFF",
                        title = buff.title.uppercase(Locale.US),
                        badge = "${buff.dropRatePercent}% DROP",
                        summary = buff.effectSummary,
                        tacticalNote = buff.tacticalNote,
                        accentHex = 0xFF34D399L,
                        buffTypeIndex = buff.buffTypeIndex,
                      )
                    )
                  }
                  .padding(horizontal = 8.dp, vertical = 6.dp),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              BuffIconPreview(
                buffType = buff.buffTypeIndex,
                sizeDp = 34,
                accentColor = Color(0xFF34D399),
              )
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = buff.title.uppercase(Locale.US),
                  color = Color.White,
                  fontSize = if (isTabletDeck) 9.2.sp else 8.2.sp,
                  fontWeight = FontWeight.Black,
                  fontFamily = FontFamily.Monospace,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
                Text(
                  text = buff.effectSummary,
                  color = Color(0xFFCBD5E1),
                  fontSize = 7.8.sp,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
              Text(
                text = "VIEW >",
                color = Color(0xFF34D399),
                fontSize = 7.8.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
              )
            }
          }
        }
        3 -> {
          // Rift Station Safe Harbor + Obstacles entry first
          Row(
            modifier =
              Modifier.fillMaxWidth()
                .minimumInteractiveComponentSize()
                .chamferedSciFiPodSurface(
                  isRightSide = true,
                  primaryBorder = Color(0xFF00F5D4),
                  secondaryBorder = Color(0xFFFFD700),
                )
                .clickable {
                  onInspectCodexStage(
                    OverwatchCircularPrompt.InfoStageInspect(
                      kicker = "SAFE HARBOR & OBSTACLES",
                      title = "5X RIFT STATION & SECTOR OBSTACLES",
                      badge = "180PX SAFE ZONE",
                      summary =
                        "Rift Station anchors Sector Convergence & Flux Core (15s max stay, weapons locked inside). Crystal Asteroids (2,400 Flux) drop Supply Crates on shatter and respawn in 20s.",
                      tacticalNote =
                        "Orbital & Asteroid Stations fire a 16-bolt 360° Defense Nova every 10 hits or when imploded by a >= 260px Gravity Rift.",
                      accentHex = 0xFF00F5D4L,
                      showStationPreview = true,
                    )
                  )
                }
                .padding(horizontal = 8.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.weight(1f).padding(end = 6.dp)) {
              Text(
                text = "RIFT STATION & SECTOR OBSTACLES",
                color = Color.White,
                fontSize = if (isTabletDeck) 9.2.sp else 8.2.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
              )
              Text(
                text = "180px Safe Zone • 2,400-Flux Asteroids • 10-Hit Station Nova",
                color = Color(0xFF00F5D4),
                fontSize = 7.8.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
            Text(
              text = "VIEW >",
              color = Color(0xFFFFD700),
              fontSize = 7.8.sp,
              fontWeight = FontWeight.Black,
              fontFamily = FontFamily.Monospace,
            )
          }

          RiftCodex.RIFT_ANOMALIES_CODEX.forEach { rift ->
            val accentCol =
              runCatching { Color(android.graphics.Color.parseColor(rift.accentHex)) }
                .getOrDefault(Color(0xFFA855F7))
            Row(
              modifier =
                Modifier.fillMaxWidth()
                  .minimumInteractiveComponentSize()
                  .chamferedSciFiPodSurface(
                    isRightSide = true,
                    primaryBorder = accentCol,
                    secondaryBorder = Color(0xFF091528),
                  )
                  .clickable {
                    onInspectCodexStage(
                      OverwatchCircularPrompt.InfoStageInspect(
                        kicker = "SPACETIME-TEAR RIFT",
                        title = rift.title.uppercase(Locale.US),
                        badge = rift.specBadge,
                        summary = rift.effectSummary,
                        tacticalNote = rift.tacticalNote,
                        accentHex = 0xFFA855F7L,
                        riftAnomalyId = rift.id,
                      )
                    )
                  }
                  .padding(horizontal = 8.dp, vertical = 6.dp),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              RiftAnomalyIconPreview(
                riftId = rift.id,
                sizeDp = 36,
                accentColor = accentCol,
              )
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = rift.title.uppercase(Locale.US),
                  color = Color.White,
                  fontSize = if (isTabletDeck) 9.2.sp else 8.2.sp,
                  fontWeight = FontWeight.Black,
                  fontFamily = FontFamily.Monospace,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
                Text(
                  text = rift.effectSummary,
                  color = Color(0xFFCBD5E1),
                  fontSize = 7.8.sp,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
              Text(
                text = "VIEW >",
                color = accentCol,
                fontSize = 7.8.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
              )
            }
          }
        }
        else -> {
          allShips.forEach { ship ->
            val cleanShipName = ship.name.replace(Regex("^[0-9]+-"), "").trim().uppercase(Locale.US)
            Row(
              modifier =
                Modifier.fillMaxWidth()
                  .minimumInteractiveComponentSize()
                  .chamferedSciFiPodSurface(
                    isRightSide = true,
                    primaryBorder = Color(0xFF38BDF8),
                    secondaryBorder = Color(0xFF091528),
                  )
                  .clickable { onInspectShipStage(ship.id) }
                  .padding(horizontal = 8.dp, vertical = 6.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column(modifier = Modifier.weight(1f).padding(end = 6.dp)) {
                Text(
                  text = "#${ship.id} $cleanShipName",
                  color = Color.White,
                  fontSize = if (isTabletDeck) 9.2.sp else 8.2.sp,
                  fontWeight = FontWeight.Black,
                  fontFamily = FontFamily.Monospace,
                  maxLines = 1,
                )
                Text(
                  text = "${ship.role.uppercase(Locale.US)} • ${ship.primaryTitle} + ${ship.secondaryTitle}",
                  color = Color(0xFF38BDF8),
                  fontSize = 7.8.sp,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
              Text(
                text = "STAGE >",
                color = Color(0xFF00F5D4),
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
              text = "[RIFT OUT]",
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
              text = "[CANCEL]",
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
              text = "[SWITCH ACCOUNT]",
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
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Row(
              modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState()),
              horizontalArrangement = Arrangement.spacedBy(6.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              NEXUS_STARTER_CHIPS.forEachIndexed { idx, chipPrompt ->
                Box(
                  modifier =
                    Modifier.minimumInteractiveComponentSize()
                      .clickable(enabled = !isStreaming) { onSendStarterQuestion(chipPrompt) }
                      .padding(vertical = 4.dp)
                      .testTag("nexus_quick_chip_$idx"),
                  contentAlignment = Alignment.Center,
                ) {
                  Box(
                    modifier =
                      Modifier.clip(RoundedCornerShape(10.dp))
                        .background(Brush.verticalGradient(listOf(Color(0xCC10233B), Color(0xCC071222))))
                        .border(1.dp, Color(0x7738BDF8), RoundedCornerShape(10.dp))
                        .padding(horizontal = 9.dp, vertical = 4.5.dp),
                  ) {
                    Text(chipPrompt, color = if (isStreaming) Color(0xFF64748B) else Color(0xFFE0F2FE), fontSize = 8.5.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                  }
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
                  .background(Brush.verticalGradient(listOf(Color(0xFF030812), Color(0xFF081324))))
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
              border = BorderStroke(1.1.dp, if (inputQuery.isNotBlank() && !isStreaming) Color(0xFF00F5D4) else Color(0x44475569)),
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
      colors =
        ButtonDefaults.buttonColors(
          containerColor = Color(0xFF0E7490),
          contentColor = Color.White,
          disabledContainerColor = Color(0x661E293B),
          disabledContentColor = Color(0xFF64748B),
        ),
      border = BorderStroke(1.1.dp, if (value.isNotBlank()) Color(0xFF00F5D4) else Color(0x44475569)),
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
        .background(Brush.verticalGradient(listOf(Color(0xB30F1F38), Color(0xB3071020))))
        .border(1.dp, Color(0x66B388FF), RoundedCornerShape(10.dp))
        .padding(10.dp),
    verticalArrangement = Arrangement.spacedBy(4.dp),
  ) {
    Text("RIFT AI TELEMETRY LINK ESTABLISHED", color = Color(0xFF00F5D4), fontSize = 10.5.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace)
    Text(
      "Ask RIFT AI anything — exact weapon dimensions, Newtonian drift & bounce raycasts, 100-point Rift Frame theorycrafting, Eclipse Stasis vs Disruptor EMP, Paired Phase Rifts, or Peak Resonance records.",
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
  var micActive by remember { mutableStateOf(false) }
  var deafened by remember { mutableStateOf(false) }
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
        onSendVoiceHeartbeat(targetVoiceKey, "[VOICE-STATE] $callsign joined voice comms (MIC LIVE)")
      } else {
        micActive = false
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
          Modifier.weight(1.25f)
            .minimumInteractiveComponentSize()
            .clickable {
              if (micActive) {
                micActive = false
                onSendVoiceHeartbeat(targetVoiceKey, "[VOICE-STATE] $callsign muted mic")
              } else {
                val granted =
                  ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                    PackageManager.PERMISSION_GRANTED
                if (granted) {
                  permissionDeniedBanner = false
                  deafened = false
                  micActive = true
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
            fontSize = 7.4.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
          )
        }
      }

      Box(
        modifier =
          Modifier.weight(0.95f)
            .minimumInteractiveComponentSize()
            .clickable {
              deafened = !deafened
              if (deafened) micActive = false
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
            fontSize = 7.2.sp,
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

