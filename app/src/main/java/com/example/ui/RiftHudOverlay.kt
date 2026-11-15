package com.example.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.engine.AnomalyType
import com.example.engine.KillFeedEntry
import com.example.engine.RankedGameMode
import com.example.engine.RiftCalibrationState
import com.example.engine.RiftGameEngine
import com.example.engine.RiftSpriteBaker
import com.example.engine.ShipEntity
import java.util.Locale
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val CompactCircleTextStyle =
  TextStyle(
    platformStyle = PlatformTextStyle(includeFontPadding = false),
    lineHeight = 8.5.sp,
    textAlign = TextAlign.Center,
  )

private val WingTelemetryTextStyle =
  TextStyle(
    platformStyle = PlatformTextStyle(includeFontPadding = false),
  )

private fun Modifier.wysiwygUnlockOutline(
  enabled: Boolean,
  isCircle: Boolean = false,
  cornerRadius: Dp = 10.dp,
): Modifier {
  if (!enabled) return this
  return this.drawWithContent {
    drawContent()
    val strokePx = 1.6.dp.toPx()
    val dash = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()), 0f)
    val amber = Color(0xFFFACC15)
    if (isCircle) {
      val r = (size.minDimension - strokePx) * 0.5f
      val cx = size.width * 0.5f
      val cy = size.height * 0.5f
      drawCircle(
        color = amber,
        radius = r,
        center = Offset(cx, cy),
        style = Stroke(width = strokePx, pathEffect = dash),
      )
      val bx = cx + r * 0.707f
      val by = cy - r * 0.707f
      drawCircle(color = Color(0xEB1E1B08), radius = 5.5.dp.toPx(), center = Offset(bx, by))
      drawCircle(
        color = amber,
        radius = 5.5.dp.toPx(),
        center = Offset(bx, by),
        style = Stroke(width = 1.2.dp.toPx()),
      )
      drawCircle(color = amber, radius = 1.8.dp.toPx(), center = Offset(bx, by))
    } else {
      val cr = cornerRadius.toPx()
      drawRoundRect(
        color = amber,
        topLeft = Offset(strokePx * 0.5f, strokePx * 0.5f),
        size = Size((size.width - strokePx).coerceAtLeast(1f), (size.height - strokePx).coerceAtLeast(1f)),
        cornerRadius = CornerRadius(cr, cr),
        style = Stroke(width = strokePx, pathEffect = dash),
      )
    }
  }
}

@Composable
fun RiftHudOverlay(
  engine: RiftGameEngine,
  fps: Int,
  sfxEnabled: Boolean,
  unreadCommsCount: Int,
  toastMessage: String?,
  frameTick: Int,
  cameraZoom: Float = engine.cameraZoom,
  onOpenHangar: () -> Unit,
  onOpenCodex: () -> Unit,
  onOpenSyndicateCommand: () -> Unit,
  onOpenArmadaFleet: () -> Unit,
  onOpenComms: () -> Unit,
  onOpenDossier: () -> Unit,
  onOpenSettings: () -> Unit,
  onCycleCameraZoom: () -> Unit = {},
  isNexusStreaming: Boolean = false,
  onOpenNexus: () -> Unit = {},
  onOpenModes: () -> Unit = onOpenNexus,
  onEngageBrake: () -> Unit,
  onToggleMultifire: () -> Unit,
) {
  val context = LocalContext.current
  val player = engine.localPlayer
  val tickRead = frameTick
  val uiUnlocked = RiftCalibrationState.isUiUnlockedForDragging
  val textScale = engine.playfieldTextScale.coerceIn(0.65f, 1.45f)

  val speed = hypot(player.vx, player.vy).toInt()
  val sectorX = ((player.x / 1000f).toInt()).coerceIn(0, 7)
  val sectorY = ((player.y / 1000f).toInt()).coerceIn(0, 7)
  val sectorCode = "${('A' + sectorX)}${sectorY + 1}"
  val tagCore = player.syndicateTag.ifBlank { "RFT" }
  val armadaBadge =
    if (engine.activeArmadaName.isNotEmpty()) {
      "[$tagCore] • ${engine.activeArmadaName} ${engine.getCurrentArmadaSize()}/5"
    } else {
      "[$tagCore] • SOLO"
    }
  val zoomFormatted = String.format(Locale.US, "%.2fx", cameraZoom).replace(".00x", ".0x")

  var rootScreenSize by remember { mutableStateOf(IntSize(1920, 1080)) }
  var bottomConsoleWidthPx by remember { mutableIntStateOf(0) }
  val density = LocalDensity.current
  val bottomConsoleSpreadDp =
    remember(bottomConsoleWidthPx, RiftCalibrationState.bottomConsoleScale, density) {
      val measuredDp =
        if (bottomConsoleWidthPx > 0) {
          with(density) { (bottomConsoleWidthPx * RiftCalibrationState.bottomConsoleScale).toDp().value }
        } else {
          0f
        }
      max(480f, measuredDp + 64f).dp
    }

  Box(
    modifier =
      Modifier.fillMaxSize()
        .onGloballyPositioned { coords ->
          if (coords.size.width > 0 && coords.size.height > 0) {
            rootScreenSize = coords.size
          }
        }
  ) {
    // 1. Top-Center Active Ranked Mode / Custom Game Telemetry Pill
    if (engine.activeRankedMode != RankedGameMode.SECTOR_CONVERGENCE || engine.isCustomGameActive()) {
      val mode = engine.effectiveModeRules()
      val modeAccent =
        when (mode) {
          RankedGameMode.RIFT_SKIRMISH -> Color(0xFFF59E0B)
          RankedGameMode.FLUX_CORE -> Color(0xFF00F5D4)
          RankedGameMode.STASIS_BREACH -> Color(0xFFA855F7)
          RankedGameMode.RIFT_CONTROL -> Color(0xFF38BDF8)
          RankedGameMode.SYNDICATE_WARS -> Color(0xFFEF4444)
          RankedGameMode.SECTOR_CONVERGENCE -> Color(0xFF38BDF8)
        }
      fun formatRiftState(controlSec: Float, lockedBy: Int, westCnt: Int, eastCnt: Int): String {
        val pct = ((kotlin.math.abs(controlSec) / 30f) * 100f).toInt().coerceIn(0, 100)
        return when {
          lockedBy == 0 -> "✓ WEST CAPTURED ($pct%)"
          lockedBy == 1 -> "✓ EAST CAPTURED ($pct%)"
          westCnt > 0 && westCnt == eastCnt -> "⚔️ CONTESTED (${westCnt}v${eastCnt})"
          controlSec < -0.2f -> "WEST $pct% (${kotlin.math.abs(controlSec).toInt()}s/30s)"
          controlSec > 0.2f -> "EAST $pct% (${kotlin.math.abs(controlSec).toInt()}s/30s)"
          else -> "OPEN (0%)"
        }
      }
      val baseModeSummaryText =
        when (mode) {
          RankedGameMode.RIFT_SKIRMISH ->
            "⚔️ RIFT SKIRMISH (${engine.activeTeamSizeBracket}v${engine.activeTeamSizeBracket}) • RND ${engine.skirmishCurrentRound} (${engine.skirmishRoundTimerSec.toInt()}s) • WEST ${engine.skirmishWestRoundWins} - ${engine.skirmishEastRoundWins} EAST"
          RankedGameMode.FLUX_CORE -> {
            val carrierShip =
              engine.ships.firstOrNull {
                it.id == engine.fluxCoreCarrierEntityId ||
                  (engine.fluxCoreOrb.carrierShipId != -1 && it.shipId == engine.fluxCoreOrb.carrierShipId)
              }
            val carrierLabel =
              when {
                engine.fluxCoreCarrierEntityId == player.id ->
                  "CARRIER: YOU (TIER ${engine.fluxCoreOrb.overchargeTier}/10)"
                carrierShip != null ->
                  "CARRIER: ${carrierShip.callsign} (TIER ${engine.fluxCoreOrb.overchargeTier}/10)"
                else -> "ORB AT (${engine.fluxCoreOrb.x.toInt()},${engine.fluxCoreOrb.y.toInt()})"
              }
            "⚡ FLUX CORE • $carrierLabel • HOLD ${engine.fluxCoreOrb.holdDurationSec.toInt()}s"
          }
          RankedGameMode.STASIS_BREACH ->
            "🔒 STASIS BREACH (10v10) • WEST CORE ${engine.westBrigGeneratorHp.toInt()}⚡ | EAST CORE ${engine.eastBrigGeneratorHp.toInt()}⚡"
          RankedGameMode.RIFT_CONTROL ->
            "🎯 RIFT CONTROL (${engine.activeTeamSizeBracket}v${engine.activeTeamSizeBracket}) • ${engine.riftControlMatchTimerSec.toInt()}s • ALPHA: ${formatRiftState(engine.riftAlphaControlSec, engine.riftAlphaLockedBy, engine.riftAlphaWestCount, engine.riftAlphaEastCount)} | OMEGA: ${formatRiftState(engine.riftOmegaControlSec, engine.riftOmegaLockedBy, engine.riftOmegaWestCount, engine.riftOmegaEastCount)}"
          RankedGameMode.SYNDICATE_WARS ->
            "🏛️ SYNDICATE WARS (${engine.activeTeamSizeBracket}v${engine.activeTeamSizeBracket}) • [${player.syndicateTag}] ${engine.skirmishWestRoundWins} - ${engine.skirmishEastRoundWins} [${engine.rivalSyndicateWarTag}] • ${engine.skirmishRoundTimerSec.toInt()}s"
          RankedGameMode.SECTOR_CONVERGENCE -> mode.title
        }
      val modeSummaryText =
        if (engine.isCustomGameActive()) {
          "⚙️ CUSTOM: ${engine.currentCustomGame?.title ?: "Custom Match"} • $baseModeSummaryText"
        } else {
          baseModeSummaryText
        }

      Box(
        modifier =
          Modifier.align(Alignment.TopCenter)
            .padding(top = 6.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(Color(0xD9060E1C))
            .border(1.dp, modeAccent, RoundedCornerShape(9.dp))
            .clickable { onOpenNexus() }
            .padding(horizontal = (8f * textScale).dp, vertical = (3.5f * textScale).dp)
            .testTag("hud_ranked_mode_banner"),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          text = modeSummaryText,
          color = modeAccent,
          fontSize = (8.2f * textScale).sp,
          fontWeight = FontWeight.Black,
          fontFamily = FontFamily.Monospace,
          style = WingTelemetryTextStyle,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }

    // 3. Right-Anchored Borderless Killboard (Floats cleanly over raw space with crisp text drop-shadows & >1,000 Bounty gold glow)
    Column(
      modifier =
        Modifier.align(Alignment.TopEnd)
          .offset {
            IntOffset(
              RiftCalibrationState.killLogOffset.x.roundToInt(),
              RiftCalibrationState.killLogOffset.y.roundToInt(),
            )
          }
          .then(
            if (uiUnlocked) {
              Modifier.pointerInput(Unit) {
                detectDragGestures(
                  onDragEnd = { RiftCalibrationState.savePersistedLayout(context) },
                  onDragCancel = { RiftCalibrationState.savePersistedLayout(context) },
                ) { change, dragAmount ->
                  change.consume()
                  RiftCalibrationState.killLogOffset += dragAmount
                }
              }.wysiwygUnlockOutline(enabled = true, cornerRadius = 8.dp)
            } else {
              Modifier
            }
          )
          .graphicsLayer {
            scaleX = RiftCalibrationState.killLogScale
            scaleY = RiftCalibrationState.killLogScale
            transformOrigin = TransformOrigin(1f, 0f)
          }
          .padding(top = 6.dp, end = 8.dp)
          .width(236.dp)
          .padding(horizontal = 4.dp, vertical = 2.dp)
          .testTag("hud_kill_log_window"),
      verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
      val entries = engine.killFeed.take(8)
      entries.forEachIndexed { index, entry ->
        val stepAlpha = ((8 - index) / 8.0f).coerceIn(0.15f, 1.0f)
        KillFeedRow(entry = entry, alpha = stepAlpha)
      }
    }

    // 4. Unified System Prompts / Tactical Toasts Container
    // Routes ALL in-game alerts (High-Bounty Shutdowns, Idle Flux Drain, Safe Zone Lockout, Safe Zone Eviction, & Toasts)
    // into the single player-positioned draggable glass container!
    val shutdownBanner = engine.highBountyShutdownBanner
    val hasShutdown = !shutdownBanner.isNullOrBlank() && engine.highBountyShutdownTimer > 0f
    val hasIdleDrain = player.isIdleDraining && player.respawnTimer <= 0f
    val hasToast = !toastMessage.isNullOrBlank()
    val hasSafeLockout = player.safeZoneLockoutTimer > 0f && player.respawnTimer <= 0f
    val hasSafeEviction = player.inSafeZone && player.respawnTimer <= 0f && player.safeZoneStayTimer >= 3.0f

    val showUnifiedPromptBox =
      hasShutdown || hasIdleDrain || hasToast || hasSafeLockout || hasSafeEviction || uiUnlocked

    if (showUnifiedPromptBox) {
      val pulseWave = 0.55f + 0.45f * (0.5f + 0.5f * sin((System.currentTimeMillis() % 440L) / 440f * 6.28318f))
      val fluxPctInt = ((player.flux / player.maxFlux).coerceIn(0f, 1f) * 100f).roundToInt()
      val drainPctInt = (RiftCalibrationState.playfieldIdleDrainPctPerSec * 100f).roundToInt()
      val evictionRemSec = (RiftCalibrationState.safeZoneIdleLimitSec - player.safeZoneStayTimer).coerceAtLeast(0f)

      val promptTitle: String
      val promptSubtitle: String?
      val promptBg: Color
      val promptBorder: Color
      val promptTextColor: Color

      when {
        hasShutdown -> {
          promptTitle = shutdownBanner ?: ""
          promptSubtitle = null
          promptBg = Color(0xEE241404)
          promptBorder = Color(0xFFF59E0B).copy(alpha = pulseWave)
          promptTextColor = Color(0xFFFEF08A)
        }
        hasIdleDrain -> {
          promptTitle = "⚠ IDLE FLUX DRAIN — ENGAGE THRUSTERS ⚠"
          promptSubtitle = "DRAINING -$drainPctInt% MAX FLUX/SEC • FLUX AT $fluxPctInt%"
          promptBg = Color(0xE62A060B)
          promptBorder = Color(0xFFEF4444).copy(alpha = pulseWave)
          promptTextColor = Color(0xFFFECACA)
        }
        hasToast -> {
          promptTitle = toastMessage ?: ""
          promptSubtitle = null
          promptBg = Color(0xCC080E18)
          promptBorder = Color(0xAA38BDF8)
          promptTextColor = Color(0xFFFACC15)
        }
        hasSafeLockout -> {
          promptTitle = String.format(Locale.US, "⛔ SAFE ZONE LOCKED: %.1fs", player.safeZoneLockoutTimer)
          promptSubtitle = null
          promptBg = Color(0xD924070B)
          promptBorder = Color(0xFFEF4444).copy(alpha = pulseWave)
          promptTextColor = Color(0xFFFCA5A5)
        }
        hasSafeEviction -> {
          promptTitle = String.format(Locale.US, "SAFE ZONE EVICTION TELEPORT IN %.1fs", evictionRemSec)
          promptSubtitle = null
          promptBg = Color(0xD9051824)
          promptBorder = if (evictionRemSec <= 5f) Color(0xFFFACC15) else Color(0xFF38BDF8)
          promptTextColor = if (evictionRemSec <= 5f) Color(0xFFFACC15) else Color(0xFF38BDF8)
        }
        else -> {
          promptTitle = "❖ SYSTEM PROMPTS / TOASTS"
          promptSubtitle = null
          promptBg = Color(0xCC080E18)
          promptBorder = Color(0x8838BDF8)
          promptTextColor = Color(0xFFFACC15)
        }
      }

      Box(
        modifier =
          Modifier.align(Alignment.TopCenter)
            .padding(top = 38.dp)
            .offset {
              IntOffset(
                RiftCalibrationState.toastContainerOffset.x.roundToInt(),
                RiftCalibrationState.toastContainerOffset.y.roundToInt(),
              )
            }
            .then(
              if (uiUnlocked) {
                Modifier.pointerInput(Unit) {
                  detectDragGestures(
                    onDragEnd = { RiftCalibrationState.savePersistedLayout(context) },
                    onDragCancel = { RiftCalibrationState.savePersistedLayout(context) },
                  ) { change, dragAmount ->
                    change.consume()
                    RiftCalibrationState.toastContainerOffset += dragAmount
                  }
                }
              } else {
                Modifier
              }
            )
            .wrapContentSize()
            .clip(RoundedCornerShape(10.dp))
            .background(promptBg)
            .border(1.2.dp, promptBorder, RoundedCornerShape(10.dp))
            .wysiwygUnlockOutline(enabled = uiUnlocked, cornerRadius = 10.dp)
            .padding(
              horizontal = (11f * textScale).dp,
              vertical = (5f * textScale).dp,
            )
            .testTag("hud_tactical_toast"),
        contentAlignment = Alignment.Center,
      ) {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy((1.5f * textScale).dp),
        ) {
          Text(
            text = promptTitle,
            color = promptTextColor,
            fontSize = (11.0f * textScale).sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = FontFamily.SansSerif,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          if (promptSubtitle != null) {
            Text(
              text = promptSubtitle,
              color = Color(0xFFF87171),
              fontSize = (8.5f * textScale).sp,
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.Monospace,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
      }
    }

    // 5. Bottom-Center Tactical Console: Auto-Scaling Symmetric Telemetry Wings + Circular Radar Minimap
    // Uses SubcomposeLayout so BOTH wings auto-scale with user Text Size (zero hardcoded width/height)
    // while matching each other 1:1 in width and height alongside the embedded frameless swirling NEXUS vortex!
    // When UI Unlock is active, NO outer rectangular wrapper box is drawn; amber dashed outlines trace
    // directly around the two rounded Wing pills and the circular Radar border!
    // 5. Bottom-Center Console + Buff Bar Directly Above Circular Radar Minimap
    Column(
      modifier =
        Modifier.align(Alignment.BottomCenter)
          .offset {
            IntOffset(
              RiftCalibrationState.bottomConsoleOffset.x.roundToInt(),
              RiftCalibrationState.bottomConsoleOffset.y.roundToInt(),
            )
          }
          .then(
            if (uiUnlocked) {
              Modifier.pointerInput(Unit) {
                detectDragGestures(
                  onDragEnd = { RiftCalibrationState.savePersistedLayout(context) },
                  onDragCancel = { RiftCalibrationState.savePersistedLayout(context) },
                ) { change, dragAmount ->
                  change.consume()
                  RiftCalibrationState.bottomConsoleOffset += dragAmount
                }
              }
            } else {
              Modifier
            }
          )
          .graphicsLayer {
            scaleX = RiftCalibrationState.bottomConsoleScale
            scaleY = RiftCalibrationState.bottomConsoleScale
            transformOrigin = TransformOrigin(0.5f, 1f)
          }
          .onSizeChanged { bottomConsoleWidthPx = it.width }
          .padding(bottom = 8.dp)
          .testTag("hud_bottom_console"),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      // Buff Bar directly above Center Circular Radar Minimap (BNC, SPD, THR, TURN, MAX⚡, RCH)
      var buffBarBoostTrigger by remember { mutableIntStateOf(0) }
      var isBuffBarEnlarged by remember { mutableStateOf(false) }
      LaunchedEffect(buffBarBoostTrigger) {
        if (buffBarBoostTrigger > 0) {
          isBuffBarEnlarged = true
          delay(1250L)
          isBuffBarEnlarged = false
        }
      }
      val buffBarTapScale by animateFloatAsState(
        targetValue = if (isBuffBarEnlarged) 1.48f else 1.0f,
        animationSpec =
          spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
          ),
        label = "buff_bar_tap_enlarge",
      )

      Box(
        modifier =
          Modifier.zIndex(if (isBuffBarEnlarged || buffBarTapScale > 1.02f) 18f else 4f)
            .offset {
              IntOffset(
                RiftCalibrationState.buffBarOffset.x.roundToInt(),
                RiftCalibrationState.buffBarOffset.y.roundToInt(),
              )
            }
            .then(
              if (uiUnlocked) {
                Modifier.pointerInput(Unit) {
                  detectDragGestures(
                    onDragEnd = { RiftCalibrationState.savePersistedLayout(context) },
                    onDragCancel = { RiftCalibrationState.savePersistedLayout(context) },
                  ) { change, dragAmount ->
                    change.consume()
                    RiftCalibrationState.buffBarOffset += dragAmount
                  }
                }
              } else {
                Modifier.clickable(
                  interactionSource = remember { MutableInteractionSource() },
                  indication = null,
                ) {
                  buffBarBoostTrigger++
                }
              }
            )
            .graphicsLayer {
              scaleX = RiftCalibrationState.buffBarScale * buffBarTapScale
              scaleY = RiftCalibrationState.buffBarScale * buffBarTapScale
              transformOrigin = TransformOrigin(0.5f, 1f)
            }
            .padding(bottom = 4.dp)
            .testTag("hud_active_powerups_dock"),
        contentAlignment = Alignment.Center,
      ) {
        ActivePowerupsDock(
          player = player,
          buffVersion = engine.buffVersion,
          frameTick = tickRead,
          textScale = textScale,
          uiUnlocked = uiUnlocked,
        )
      }

      SymmetricBottomConsoleLayout(
        engine = engine,
        player = player,
        speed = speed,
        sectorCode = sectorCode,
        armadaBadge = armadaBadge,
        textScale = textScale,
        uiUnlocked = uiUnlocked,
        isNexusStreaming = isNexusStreaming,
        onOpenHangar = onOpenHangar,
        onOpenNexus = onOpenNexus,
        onOpenModes = onOpenModes,
        frameTick = tickRead,
        rootScreenSize = rootScreenSize,
      )
    }

    // 5b. Independent Floating [BRAKE] Button — Unified 3-Ring Tactile Cockpit Button (1:1 WYSIWYG labels & circle size in UI Unlock Mode)
    val brakingActive = engine.autoBraking && speed > 0
    UnifiedThreeRingCockpitButton(
      title = "BRAKE",
      sub = if (brakingActive) "ACTIVE" else "STOP",
      strokeColor = if (brakingActive) Color(0xFFF59E0B) else Color(0xFF38BDF8),
      fillColor = if (brakingActive) Color(0xFF78350F) else Color(0xFF0C4A6E),
      subColor = if (brakingActive) Color(0xFFFDE047) else Color(0xFFE0F2FE),
      sizeDp = 50.dp,
      modifier =
        Modifier.align(Alignment.BottomCenter)
          .padding(bottom = 14.dp, end = bottomConsoleSpreadDp)
          .offset {
            IntOffset(
              RiftCalibrationState.brakeBtnOffset.x.roundToInt(),
              RiftCalibrationState.brakeBtnOffset.y.roundToInt(),
            )
          }
          .graphicsLayer {
            scaleX = RiftCalibrationState.bottomConsoleScale
            scaleY = RiftCalibrationState.bottomConsoleScale
            transformOrigin = TransformOrigin(0.5f, 0.5f)
          }
          .then(
            if (uiUnlocked) {
              Modifier.pointerInput(Unit) {
                detectDragGestures(
                  onDragEnd = { RiftCalibrationState.savePersistedLayout(context) },
                  onDragCancel = { RiftCalibrationState.savePersistedLayout(context) },
                ) { change, dragAmount ->
                  change.consume()
                  RiftCalibrationState.brakeBtnOffset += dragAmount
                }
              }
            } else {
              Modifier
            }
          )
          .wysiwygUnlockOutline(enabled = uiUnlocked, isCircle = true)
          .clickable(enabled = !uiUnlocked) { onEngageBrake() }
          .testTag("hud_brake_button"),
    )

    // 5c. Independent Floating [MULTI ON/OFF] Button — Unified 3-Ring Tactile Cockpit Button (Strictly visible ONLY on Native Multi-Shot Hulls)
    val isNativeMulti = player.spec.supportsNativeMultifire
    val showMultiButton =
      isNativeMulti ||
        uiUnlocked ||
        RiftCalibrationState.forceAllButtonsVisible
    if (showMultiButton) {
      val multiCharges = player.multiShotCharges
      val topMultiLabel =
        when {
          player.spec.isParallelTwin -> "TWIN"
          else -> "MULTI"
        }
      val subLabel =
        when {
          isNativeMulti && multiCharges > 0 -> "${if (player.multifireOn) "ON" else "OFF"}•[$multiCharges]"
          isNativeMulti -> if (player.multifireOn) "ON" else "OFF"
          else -> "OFF"
        }
      UnifiedThreeRingCockpitButton(
        title = topMultiLabel,
        sub = subLabel,
        strokeColor =
          when {
            player.multifireOn && isNativeMulti -> Color(0xFFFACC15)
            isNativeMulti -> Color(0xFF38BDF8)
            else -> Color(0xFF475569)
          },
        fillColor =
          when {
            player.multifireOn && isNativeMulti -> Color(0xFF713F12)
            isNativeMulti -> Color(0xFF0C4A6E)
            else -> Color(0xFF1E293B)
          },
        subColor =
          when {
            player.multifireOn && isNativeMulti -> Color(0xFFFEF08A)
            isNativeMulti -> Color.White
            else -> Color(0xFF94A3B8)
          },
        sizeDp = 50.dp,
        modifier =
          Modifier.align(Alignment.BottomCenter)
            .padding(bottom = 14.dp, start = bottomConsoleSpreadDp)
            .offset {
              IntOffset(
                RiftCalibrationState.multiBtnOffset.x.roundToInt(),
                RiftCalibrationState.multiBtnOffset.y.roundToInt(),
              )
            }
            .graphicsLayer {
              scaleX = RiftCalibrationState.bottomConsoleScale
              scaleY = RiftCalibrationState.bottomConsoleScale
              transformOrigin = TransformOrigin(0.5f, 0.5f)
            }
            .then(
              if (uiUnlocked) {
                Modifier.pointerInput(Unit) {
                  detectDragGestures(
                    onDragEnd = { RiftCalibrationState.savePersistedLayout(context) },
                    onDragCancel = { RiftCalibrationState.savePersistedLayout(context) },
                  ) { change, dragAmount ->
                    change.consume()
                    RiftCalibrationState.multiBtnOffset += dragAmount
                  }
                }
              } else {
                Modifier
              }
            )
            .wysiwygUnlockOutline(enabled = uiUnlocked, isCircle = true)
            .clickable(enabled = !uiUnlocked) { onToggleMultifire() }
            .testTag("hud_multifire_button"),
      )
    }

    // Centered Calibration Modal
    if (RiftCalibrationState.isCalibrationModalOpen) {
      RiftCalibrationDialog(
        engine = engine,
        onDismiss = { RiftCalibrationState.isCalibrationModalOpen = false },
      )
    }
  }
}

@Composable
private fun SymmetricBottomConsoleLayout(
  engine: RiftGameEngine,
  player: ShipEntity,
  speed: Int,
  sectorCode: String,
  armadaBadge: String,
  textScale: Float,
  uiUnlocked: Boolean,
  isNexusStreaming: Boolean,
  onOpenHangar: () -> Unit,
  onOpenNexus: () -> Unit,
  onOpenModes: () -> Unit,
  frameTick: Int,
  rootScreenSize: IntSize,
) {
  val fluxPct = (player.flux / player.maxFlux).coerceIn(0f, 1f)
  val fluxColor =
    when {
      fluxPct > 0.75f -> Color(0xFF10B981)
      fluxPct >= 0.25f -> Color(0xFFF59E0B)
      else -> Color(0xFFEF4444)
    }

  val padStart = (8f * textScale).dp
  val padVert = (5.5f * textScale).dp
  val lineGap = (1.2f * textScale).dp
  val consoleGap = 8.dp

  val shipBmp =
    remember(player.shipId, RiftSpriteBaker.customShip9Version) {
      RiftSpriteBaker.getShipSprite(player.shipId, isAllied = true).asImageBitmap()
    }

  @Composable
  fun LeftWingInner(fillEqualSpace: Boolean) {
    Row(
      modifier =
        (if (fillEqualSpace) Modifier.fillMaxSize() else Modifier.wrapContentSize())
          .background(Color(0xF6080E18), RoundedCornerShape(50))
          .border(1.dp, Color(0x5938BDF8), RoundedCornerShape(50))
          .wysiwygUnlockOutline(enabled = uiUnlocked && fillEqualSpace, cornerRadius = 28.dp)
          .padding(start = (5f * textScale).dp, end = (6.5f * textScale).dp, top = padVert, bottom = padVert),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement =
        if (fillEqualSpace) Arrangement.SpaceBetween else Arrangement.spacedBy((5.5f * textScale).dp),
    ) {
      // Outer Left: Frameless 3D Ship Sprite over a Holographic Emitter Ring (No circle border or cut-off "HANGAR" text)
      Box(
        modifier =
          Modifier.size(36.dp)
            .clickable(enabled = !uiUnlocked) { onOpenHangar() }
            .testTag("hud_btn_hangar_ship"),
        contentAlignment = Alignment.Center,
      ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
          val cx = size.width * 0.5f
          val cy = size.height * 0.5f
          val ringCol = if (engine.isShipLockedForMatch) Color(0xFFF59E0B) else Color(0xFF00F5D4)
          drawOval(
            color = ringCol.copy(alpha = 0.24f),
            topLeft = Offset(cx - size.width * 0.44f, cy + size.height * 0.16f),
            size = Size(size.width * 0.88f, size.height * 0.28f),
          )
          drawOval(
            color = ringCol.copy(alpha = 0.85f),
            topLeft = Offset(cx - size.width * 0.44f, cy + size.height * 0.16f),
            size = Size(size.width * 0.88f, size.height * 0.28f),
            style = Stroke(width = 1.1.dp.toPx()),
          )
          val pad = 1.dp.toPx()
          val dstSize = (size.minDimension - pad * 2f).toInt().coerceAtLeast(1)
          drawImage(
            image = shipBmp,
            dstOffset = IntOffset(pad.toInt(), (pad * 0.4f).toInt()),
            dstSize = IntSize(dstSize, dstSize),
          )
        }
        if (engine.isShipLockedForMatch) {
          Box(
            modifier =
              Modifier.align(Alignment.TopEnd)
                .size(10.dp)
                .clip(CircleShape)
                .background(Color(0xEE1E1604))
                .border(0.8.dp, Color(0xFFF59E0B), CircleShape),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text = "🔒",
              fontSize = 6.sp,
              style = CompactCircleTextStyle,
            )
          }
        }
      }

      // Center Left: Compact 3-Line Stacked Telemetry (SPEED -> SECTOR -> [TAG] • ARMADA)
      Column(
        verticalArrangement = Arrangement.spacedBy(0.5.dp),
      ) {
        Text(
          text = "SPEED $speed",
          color = Color(0xFF00F5D4),
          fontSize = (8.4f * textScale).sp,
          fontWeight = FontWeight.Black,
          fontFamily = FontFamily.Monospace,
          style = WingTelemetryTextStyle,
          maxLines = 1,
        )
        Text(
          text = "SECTOR $sectorCode",
          color = Color(0xFFE2E8F0),
          fontSize = (7.6f * textScale).sp,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.Monospace,
          style = WingTelemetryTextStyle,
          maxLines = 1,
        )
        Text(
          text = armadaBadge.uppercase(Locale.US),
          color = Color(0xFFFACC15),
          fontSize = (7.2f * textScale).sp,
          fontWeight = FontWeight.ExtraBold,
          fontFamily = FontFamily.Monospace,
          style = WingTelemetryTextStyle,
          maxLines = 1,
        )
      }

      // Inner Left (next to Minimap): Beveled Crossed-Swords Combat Mode Selection Emblem Button
      Box(
        modifier =
          Modifier.size(32.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(
              androidx.compose.ui.graphics.Brush.verticalGradient(
                colors = listOf(Color(0xFF142842), Color(0xFF071222))
              )
            )
            .border(1.2.dp, Color(0xFF38BDF8), RoundedCornerShape(9.dp))
            .clickable(enabled = !uiUnlocked) { onOpenModes() }
            .testTag("hud_btn_modes"),
        contentAlignment = Alignment.Center,
      ) {
        Canvas(modifier = Modifier.size(20.dp)) {
          val w = size.width
          val h = size.height
          val cx = w * 0.5f
          val cy = h * 0.5f
          // Outer tactical diamond frame
          val diamond = Path().apply {
            moveTo(cx, h * 0.06f)
            lineTo(w * 0.94f, cy)
            lineTo(cx, h * 0.94f)
            lineTo(w * 0.06f, cy)
            close()
          }
          drawPath(diamond, color = Color(0x2800F5D4))
          drawPath(diamond, color = Color(0x8838BDF8), style = Stroke(width = 1.dp.toPx()))

          // Crossed Energy Blades
          val bladeStroke = 2.2.dp.toPx()
          drawLine(
            color = Color(0xFF00F5D4),
            start = Offset(w * 0.22f, h * 0.78f),
            end = Offset(w * 0.82f, h * 0.18f),
            strokeWidth = bladeStroke,
            cap = StrokeCap.Round,
          )
          drawLine(
            color = Color(0xFFFACC15),
            start = Offset(w * 0.20f, h * 0.58f),
            end = Offset(w * 0.42f, h * 0.80f),
            strokeWidth = 1.8.dp.toPx(),
            cap = StrokeCap.Round,
          )
          drawLine(
            color = Color(0xFF38BDF8),
            start = Offset(w * 0.78f, h * 0.78f),
            end = Offset(w * 0.18f, h * 0.18f),
            strokeWidth = bladeStroke,
            cap = StrokeCap.Round,
          )
          drawLine(
            color = Color(0xFFFACC15),
            start = Offset(w * 0.58f, h * 0.80f),
            end = Offset(w * 0.80f, h * 0.58f),
            strokeWidth = 1.8.dp.toPx(),
            cap = StrokeCap.Round,
          )
          drawCircle(
            color = Color.White,
            radius = 2.0.dp.toPx(),
            center = Offset(cx, cy),
          )
        }
      }
    }
  }

  @Composable
  fun RightWingInner(fillEqualSpace: Boolean) {
    Row(
      modifier =
        (if (fillEqualSpace) Modifier.fillMaxSize() else Modifier.wrapContentSize())
          .background(Color(0xF6080E18), RoundedCornerShape(50))
          .border(1.dp, Color(0x5938BDF8), RoundedCornerShape(50))
          .wysiwygUnlockOutline(enabled = uiUnlocked && fillEqualSpace, cornerRadius = 28.dp)
          .padding(start = padStart, end = (5.5f * textScale).dp, top = padVert, bottom = padVert),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement =
        if (fillEqualSpace) Arrangement.SpaceBetween else Arrangement.spacedBy((5.5f * textScale).dp),
    ) {
      // Inner Right (next to Minimap): Compact 2-Line FLUX & BTY • K/D Readout with custom FluxCrystalIcon
      Column(
        verticalArrangement = Arrangement.spacedBy(lineGap),
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(3.dp),
        ) {
          FluxCrystalIcon(sizeDp = (12.5f * textScale).dp)
          Text(
            text = "⚡ ${player.flux.toInt()} (${(fluxPct * 100).toInt()}%)",
            color = fluxColor,
            fontSize = (8.8f * textScale).sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
            style = WingTelemetryTextStyle,
            maxLines = 1,
          )
        }
        Text(
          text = "BTY ${player.bounty} • K/D ${engine.allTimeKills}/${engine.allTimeDeaths}",
          color = Color(0xFFFACC15),
          fontSize = (7.8f * textScale).sp,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.Monospace,
          style = WingTelemetryTextStyle,
          maxLines = 1,
          modifier =
            Modifier.clickable(enabled = !uiUnlocked) { onOpenNexus() }
              .testTag("hud_bounty_kd_readout"),
        )
      }

      // Outer Right: Swirling NEXUS Vortex Orb
      Box(modifier = Modifier.testTag("hud_btn_nexus")) {
        SwirlingRiftHudOrb(
          isStreaming = isNexusStreaming,
          onClick = onOpenNexus,
        )
      }
    }
  }

  SubcomposeLayout { constraints ->
    val unbounded = Constraints()
    val probeLeft = subcompose("probe_left") { LeftWingInner(fillEqualSpace = false) }
      .first()
      .measure(unbounded)
    val probeRight = subcompose("probe_right") { RightWingInner(fillEqualSpace = false) }
      .first()
      .measure(unbounded)

    val sharedWingW = max(probeLeft.width, probeRight.width)
    val sharedWingH = max(probeLeft.height, probeRight.height)
    val wingFixedConstraints = Constraints.fixed(sharedWingW, sharedWingH)

    val leftPlaceable = subcompose("left_wing") { LeftWingInner(fillEqualSpace = true) }
      .first()
      .measure(wingFixedConstraints)
    val radarPlaceable = subcompose("radar") {
      CircularRadarMinimap(
        engine = engine,
        frameTick = frameTick,
        rootScreenSize = rootScreenSize,
        uiUnlocked = uiUnlocked,
      )
    }.first().measure(unbounded)
    val rightPlaceable = subcompose("right_wing") { RightWingInner(fillEqualSpace = true) }
      .first()
      .measure(wingFixedConstraints)

    val gapPx = consoleGap.roundToPx()
    val totalW =
      (leftPlaceable.width + gapPx + radarPlaceable.width + gapPx + rightPlaceable.width)
        .coerceAtMost(constraints.maxWidth)
    val totalH = max(radarPlaceable.height, sharedWingH)

    layout(totalW, totalH) {
      val wingY = (totalH - sharedWingH) / 2
      val radarY = (totalH - radarPlaceable.height) / 2
      leftPlaceable.placeRelative(0, wingY, zIndex = 1f)
      val radarX = leftPlaceable.width + gapPx
      val rightX = radarX + radarPlaceable.width + gapPx
      rightPlaceable.placeRelative(rightX, wingY, zIndex = 1f)
      radarPlaceable.placeRelative(radarX, radarY, zIndex = 2f)
    }
  }
}

@Composable
private fun CircularRadarMinimap(
  engine: RiftGameEngine,
  frameTick: Int,
  rootScreenSize: IntSize,
  uiUnlocked: Boolean = false,
) {
  var panOffsetX by remember { mutableFloatStateOf(0f) }
  var panOffsetY by remember { mutableFloatStateOf(0f) }
  val radarBmp = remember(engine.radarBgBitmap) { engine.radarBgBitmap?.asImageBitmap() }
  val trianglePath = remember { Path() }

  // 1.5s Tap-to-Expand (~1.65x diameter) state & intelligent screen-edge anchoring
  var isExpanded by remember { mutableStateOf(false) }
  val scope = rememberCoroutineScope()
  var expandCollapseJob by remember { mutableStateOf<Job?>(null) }
  var radarCenterOnScreen by remember { mutableStateOf(Offset(960f, 980f)) }

  val expandScale by animateFloatAsState(
    targetValue = if (isExpanded) 1.65f else 1.0f,
    animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow),
    label = "minimap_expand_scale",
  )

  val normX =
    if (rootScreenSize.width > 0) {
      (radarCenterOnScreen.x / rootScreenSize.width.toFloat()).coerceIn(0f, 1f)
    } else {
      0.5f
    }
  val normY =
    if (rootScreenSize.height > 0) {
      (radarCenterOnScreen.y / rootScreenSize.height.toFloat()).coerceIn(0f, 1f)
    } else {
      1.0f
    }
  val intelligentPivotX =
    when {
      normX < 0.28f -> 0.0f
      normX > 0.72f -> 1.0f
      else -> normX
    }
  val intelligentPivotY =
    when {
      normY > 0.65f -> 1.0f
      normY < 0.35f -> 0.0f
      else -> normY
    }

  var continuousFrameNanos by remember { mutableLongStateOf(0L) }
  LaunchedEffect(Unit) {
    while (true) {
      withFrameNanos { continuousFrameNanos = it }
    }
  }

  val innerClipPath = remember { Path() }

  Box(
    modifier =
      Modifier.zIndex(if (isExpanded || expandScale > 1.02f) 15f else 2f)
        .size(128.dp)
        .onGloballyPositioned { coords ->
          val pos = coords.positionInRoot()
          radarCenterOnScreen =
            Offset(
              pos.x + coords.size.width * 0.5f,
              pos.y + coords.size.height * 0.5f,
            )
        }
        .graphicsLayer {
          scaleX = expandScale
          scaleY = expandScale
          transformOrigin = TransformOrigin(intelligentPivotX, intelligentPivotY)
        }
        .wysiwygUnlockOutline(enabled = uiUnlocked, isCircle = true)
        .then(
          if (!uiUnlocked) {
            Modifier.pointerInput(Unit) {
              detectTapGestures(
                onTap = {
                  expandCollapseJob?.cancel()
                  isExpanded = true
                  expandCollapseJob =
                    scope.launch {
                      delay(1500L)
                      isExpanded = false
                    }
                }
              )
            }.pointerInput(Unit) {
              detectDragGestures(
                onDragEnd = {
                  panOffsetX = 0f
                  panOffsetY = 0f
                },
                onDragCancel = {
                  panOffsetX = 0f
                  panOffsetY = 0f
                },
                onDrag = { change, dragAmount ->
                  change.consume()
                  panOffsetX = (panOffsetX + dragAmount.x).coerceIn(-24f, 24f)
                  panOffsetY = (panOffsetY + dragAmount.y).coerceIn(-24f, 24f)
                },
              )
            }
          } else {
            Modifier
          }
        )
        .testTag("hud_circular_radar"),
  ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val unusedTick = frameTick + continuousFrameNanos
      val w = size.width
      val h = size.height
      val center = Offset(w * 0.5f, h * 0.5f)
      val radarRadius = w * 0.5f
      val playfieldRadius = (radarRadius - 12.dp.toPx()).coerceAtLeast(16f)
      val maxBlipRadius = (playfieldRadius - 3.5.dp.toPx()).coerceAtLeast(8f)
      val drawSize = playfieldRadius * 2f
      val ox = center.x - playfieldRadius + panOffsetX
      val oy = center.y - playfieldRadius + panOffsetY
      val radarScale = playfieldRadius / engine.arenaRadius.coerceAtLeast(1000f)

      fun clampToRadar(rawX: Float, rawY: Float): Offset {
        val dx = rawX - center.x
        val dy = rawY - center.y
        val dist = hypot(dx, dy)
        return if (dist <= maxBlipRadius || dist < 0.001f) {
          Offset(rawX, rawY)
        } else {
          val ratio = maxBlipRadius / dist
          Offset(center.x + dx * ratio, center.y + dy * ratio)
        }
      }

      fun worldToRadar(wx: Float, wy: Float): Offset =
        clampToRadar(
          center.x + panOffsetX + (wx - engine.arenaCenterX) * radarScale,
          center.y + panOffsetY + (wy - engine.arenaCenterY) * radarScale,
        )

      // Draw circular playfield background and clipped world map strictly inside playfieldRadius
      drawCircle(
        color = if (isExpanded) Color(0xE6040B16) else Color(0xB3040B16),
        radius = playfieldRadius,
        center = center,
      )

      innerClipPath.reset()
      innerClipPath.addOval(
        Rect(
          left = center.x - playfieldRadius,
          top = center.y - playfieldRadius,
          right = center.x + playfieldRadius,
          bottom = center.y + playfieldRadius,
        )
      )

      clipPath(innerClipPath) {
        if (radarBmp != null) {
          drawImage(
            image = radarBmp,
            dstOffset = IntOffset(ox.toInt(), oy.toInt()),
            dstSize = IntSize(drawSize.toInt(), drawSize.toInt()),
            alpha = 0.62f,
          )
        }

        drawCircle(
          color = Color(0x2638BDF8),
          radius = playfieldRadius * 0.55f,
          center = center,
          style = Stroke(width = 1.0f),
        )
      }

      // Inner radar border rim
      drawCircle(
        color = Color(0x5538BDF8),
        radius = playfieldRadius,
        center = center,
        style = Stroke(width = 1.1.dp.toPx()),
      )

      // Discreet Micro-Markers for Supply Crates (1:1 circular playfield alignment)
      for (g in engine.greens) {
        val pt = worldToRadar(g.x, g.y)
        drawCircle(
          color = Color(0xFF10B981).copy(alpha = 0.55f),
          radius = 1.1f,
          center = pt,
        )
      }

      // Mode-Specific Objectives on Radar (Stasis Jails or Rift Control Zones)
      if (engine.isStasisBreachRulesActive()) {
        val wPt = worldToRadar(engine.westBrigReactorX, engine.westBrigReactorY)
        val ePt = worldToRadar(engine.eastBrigReactorX, engine.eastBrigReactorY)
        drawCircle(color = Color(0xFF38BDF8), radius = 4.2f, center = wPt, style = Stroke(width = 1.3f))
        drawCircle(color = Color(0xFFF43F5E), radius = 4.2f, center = ePt, style = Stroke(width = 1.3f))
      } else if (engine.isRiftControlRulesActive()) {
        val aPt = worldToRadar(engine.riftAlphaX, engine.riftAlphaY)
        val oPt = worldToRadar(engine.riftOmegaX, engine.riftOmegaY)
        drawCircle(color = Color(0xFF00F5D4), radius = 4.2f, center = aPt, style = Stroke(width = 1.4f))
        drawCircle(color = Color(0xFF00F5D4), radius = 4.2f, center = oPt, style = Stroke(width = 1.4f))
      }

      for (anom in engine.anomalies) {
        val pt = worldToRadar(anom.x, anom.y)
        val aColor =
          when (anom.type) {
            AnomalyType.PLASMA_RIFT -> Color(0xFFEF4444)
            AnomalyType.REPULSOR_RIFT -> Color.White
            AnomalyType.GRAVITY_RIFT -> Color(0xFFA855F7)
            AnomalyType.PHASE_RIFT_A -> Color(0xFF2DD4BF)
            AnomalyType.PHASE_RIFT_B -> Color(0xFF38BDF8)
          }
        drawCircle(
          color = aColor.copy(alpha = 0.80f),
          radius = 2.6f,
          center = pt,
        )
      }

      val nowMs = System.currentTimeMillis()
      val pulsePhase = ((nowMs % 1200L) / 1200f)
      val p = engine.localPlayer

      for (bw in engine.brickWalls) {
        val aPt =
          worldToRadar(
            if (bw.w >= bw.h) bw.x else bw.x + bw.w * 0.5f,
            if (bw.w >= bw.h) bw.y + bw.h * 0.5f else bw.y,
          )
        val bPt =
          worldToRadar(
            if (bw.w >= bw.h) bw.x + bw.w else bw.x + bw.w * 0.5f,
            if (bw.w >= bw.h) bw.y + bw.h * 0.5f else bw.y + bw.h,
          )
        drawLine(
          color = Color(0xFF00F5D4),
          start = aPt,
          end = bPt,
          strokeWidth = 2.2f,
        )
      }

      for (dc in engine.decoys) {
        val pt = worldToRadar(dc.x, dc.y)
        val isFriendlyDecoy =
          dc.ownerId == p.id ||
            (p.armadaName.isNotEmpty() && dc.armadaName == p.armadaName) ||
            (p.syndicateTag != "GUEST" && dc.syndicateTag == p.syndicateTag)
        val dotFill = if (isFriendlyDecoy) Color(0xFF38BDF8) else Color(0xFFEF4444)
        drawCircle(
          color = dotFill,
          radius = 2.2f,
          center = pt,
        )
      }

      // Free Flux Core Orb on Minimap when unclaimed
      val goldPulse = 0.5f + 0.5f * sin(((nowMs % 640L) / 640f) * 6.28318f)
      if (engine.isFluxCoreRulesActive() && engine.fluxCoreCarrierEntityId.isEmpty()) {
        val orbPt = worldToRadar(engine.fluxCoreOrb.x, engine.fluxCoreOrb.y)
        val orbHaloR = (4.2f + 4.8f * goldPulse).coerceAtMost(maxBlipRadius - hypot(orbPt.x - center.x, orbPt.y - center.y) + 2.5f)
        if (orbHaloR > 2.0f) {
          drawCircle(
            color = Color(0xFFFACC15).copy(alpha = (0.85f - 0.50f * goldPulse).coerceIn(0.25f, 0.90f)),
            radius = orbHaloR,
            center = orbPt,
            style = Stroke(width = 1.5f),
          )
        }
        drawCircle(
          color = Color(0xFFFFD700),
          radius = 4.2f,
          center = orbPt,
        )
        drawCircle(
          color = Color.White,
          radius = 4.2f,
          center = orbPt,
          style = Stroke(width = 1.1f),
        )
      }

      val radarShips = if (engine.hasJoinedMatch) engine.ships else listOf(p)
      for (s in radarShips) {
        if (s.respawnTimer > 0f) continue
        val isFriendly = engine.areAllied(p, s)
        val isBallCarrier =
          engine.hasJoinedMatch &&
            engine.isFluxCoreRulesActive() &&
            (s.id == engine.fluxCoreCarrierEntityId ||
              (engine.fluxCoreOrb.carrierShipId != -1 && s.shipId == engine.fluxCoreOrb.carrierShipId))
        if (s.isCloaked && !isFriendly && !s.isLocalPlayer && !isBallCarrier) continue

        val pt = worldToRadar(s.x, s.y)
        val sx = pt.x
        val sy = pt.y

        if (isBallCarrier) {
          // 2x Normal Ship Size (radius = 6.0f vs 3.0f normal) Pulsing Gold Flux Core Carrier Beacon
          val carrierHaloR =
            (6.0f + 5.5f * goldPulse).coerceAtMost(maxBlipRadius - hypot(sx - center.x, sy - center.y) + 3.0f)
          if (carrierHaloR > 3.0f) {
            drawCircle(
              color = Color(0xFFFACC15).copy(alpha = (0.92f - 0.52f * goldPulse).coerceIn(0.30f, 0.95f)),
              radius = carrierHaloR,
              center = pt,
              style = Stroke(width = 1.8f),
            )
          }
          val carrierFill = lerp(Color(0xFFFFD700), Color(0xFFFACC15), goldPulse)
          drawCircle(
            color = carrierFill,
            radius = 6.0f,
            center = pt,
          )
          drawCircle(
            color = Color.White,
            radius = 6.0f,
            center = pt,
            style = Stroke(width = 1.2f),
          )
          continue
        }

        if (s.bounty >= RiftGameEngine.HVT_BOUNTY_THRESHOLD) {
          val pingR = (3.2f + pulsePhase * 6.5f).coerceAtMost(maxBlipRadius - hypot(sx - center.x, sy - center.y) + 2.5f)
          if (pingR > 2.0f) {
            drawCircle(
              color = Color(0xFFFFD700).copy(alpha = (1f - pulsePhase).coerceIn(0.20f, 0.90f)),
              radius = pingR,
              center = pt,
              style = Stroke(width = 1.3f),
            )
          }
        }

        if (s.isLocalPlayer) {
          val r = 4.2f
          val a = s.angle
          trianglePath.reset()
          trianglePath.moveTo(sx + cos(a) * r, sy + sin(a) * r)
          trianglePath.lineTo(sx + cos(a + 2.4f) * r * 0.85f, sy + sin(a + 2.4f) * r * 0.85f)
          trianglePath.lineTo(sx + cos(a - 2.4f) * r * 0.85f, sy + sin(a - 2.4f) * r * 0.85f)
          trianglePath.close()
          drawPath(path = trianglePath, color = if (s.isCloaked) Color(0xFFD8B4FE) else Color.White)
        } else {
          val dotFill = if (isFriendly) Color(0xFF3B82F6) else Color(0xFFEF4444)
          val dotRadius = if (s.isHuman) 3.0f else 2.2f
          drawCircle(
            color = dotFill,
            radius = dotRadius,
            center = pt,
          )
          if (s.isHuman) {
            drawCircle(
              color = Color.White,
              radius = dotRadius,
              center = pt,
              style = Stroke(width = 1.0f),
            )
          }
        }
      }

      // High-precision Unclipped Flux Ring at fluxRingRadius = radarRadius - 5.5.dp.toPx()
      val fluxPct = (p.flux / p.maxFlux.coerceAtLeast(1f)).coerceIn(0f, 1f)
      val critFlash =
        if (fluxPct < 0.20f || p.isIdleDraining) {
          0.35f + 0.65f * (0.5f + 0.5f * sin((nowMs % 320L) / 320f * 6.28318f))
        } else {
          1.0f
        }
      val ringColor =
        when {
          p.isIdleDraining || fluxPct < 0.20f -> Color(0xFFEF4444).copy(alpha = critFlash)
          fluxPct >= 0.75f -> {
            val t = ((fluxPct - 0.75f) / 0.25f).coerceIn(0f, 1f)
            lerp(Color(0xFF10B981), Color(0xFF00F5D4), t)
          }
          fluxPct >= 0.40f -> {
            val t = ((fluxPct - 0.40f) / 0.35f).coerceIn(0f, 1f)
            lerp(Color(0xFF10B981), Color(0xFF34D399), t)
          }
          else -> {
            val t = ((fluxPct - 0.20f) / 0.20f).coerceIn(0f, 1f)
            lerp(Color(0xFFF59E0B), Color(0xFF10B981), t)
          }
        }

      val fluxRingRadius = (radarRadius - 5.5.dp.toPx()).coerceAtLeast(24f)
      val trackStrokeW = 4.5.dp.toPx()
      val arcStrokeW = 4.2.dp.toPx()
      val arcTopLeft = Offset(center.x - fluxRingRadius, center.y - fluxRingRadius)
      val arcSize = Size(fluxRingRadius * 2f, fluxRingRadius * 2f)

      // 1. Subtle dark recessed track circle (#99091322, stroke 4.5.dp)
      drawCircle(
        color = Color(0x99091322),
        radius = fluxRingRadius,
        center = center,
        style = Stroke(width = trackStrokeW),
      )

      // 12 subtle 30-degree micro-tick marks along the Flux track
      for (tick in 0 until 12) {
        val tickRad = Math.toRadians(-90.0 + tick * 30.0)
        val rIn = fluxRingRadius - trackStrokeW * 0.45f
        val rOut = fluxRingRadius + trackStrokeW * 0.45f
        drawLine(
          color = Color(0x6638BDF8),
          start = Offset(center.x + rIn * cos(tickRad).toFloat(), center.y + rIn * sin(tickRad).toFloat()),
          end = Offset(center.x + rOut * cos(tickRad).toFloat(), center.y + rOut * sin(tickRad).toFloat()),
          strokeWidth = 1.0.dp.toPx(),
        )
      }

      // 2. Smooth continuous glowing Flux arc (strokeWidth = 4.2.dp, StrokeCap.Round)
      if (fluxPct > 0.005f) {
        val sweepDeg = 360f * fluxPct
        drawArc(
          color = ringColor.copy(alpha = 0.28f),
          startAngle = -90f,
          sweepAngle = sweepDeg,
          useCenter = false,
          topLeft = arcTopLeft,
          size = arcSize,
          style = Stroke(width = arcStrokeW * 1.9f, cap = StrokeCap.Round),
        )
        drawArc(
          color = ringColor,
          startAngle = -90f,
          sweepAngle = sweepDeg,
          useCenter = false,
          topLeft = arcTopLeft,
          size = arcSize,
          style = Stroke(width = arcStrokeW, cap = StrokeCap.Round),
        )

        // 3. Crisp 2-layer glowing beacon dot at the moving tip (never clips because fluxRingRadius is inset by 5.5.dp!)
        val tipRad = Math.toRadians(-90.0 + sweepDeg.toDouble())
        val tipCenter =
          Offset(
            center.x + fluxRingRadius * cos(tipRad).toFloat(),
            center.y + fluxRingRadius * sin(tipRad).toFloat(),
          )
        drawCircle(
          color = ringColor.copy(alpha = 0.35f),
          radius = 5.5.dp.toPx(),
          center = tipCenter,
        )
        drawCircle(
          color = Color(0xFF040B16),
          radius = 4.2.dp.toPx(),
          center = tipCenter,
        )
        drawCircle(
          color = Color.White,
          radius = 3.2.dp.toPx(),
          center = tipCenter,
        )
        drawCircle(
          color = ringColor,
          radius = 3.2.dp.toPx(),
          center = tipCenter,
          style = Stroke(width = 1.0.dp.toPx()),
        )
      }
    }

    // 4. Compact non-overlapping "⚡ XX%" micro-badge cleanly docked at the bottom rim
    val playerFluxPct = (engine.localPlayer.flux / engine.localPlayer.maxFlux.coerceAtLeast(1f)).coerceIn(0f, 1f)
    val badgeColor =
      when {
        engine.localPlayer.isIdleDraining || playerFluxPct < 0.20f -> Color(0xFFEF4444)
        playerFluxPct >= 0.75f -> Color(0xFF00F5D4)
        playerFluxPct >= 0.40f -> Color(0xFF10B981)
        else -> Color(0xFFF59E0B)
      }
    Box(
      modifier =
        Modifier.align(Alignment.BottomCenter)
          .padding(bottom = 1.dp)
          .clip(RoundedCornerShape(6.dp))
          .background(Color(0xEE040B16))
          .border(1.dp, badgeColor.copy(alpha = 0.85f), RoundedCornerShape(6.dp))
          .padding(horizontal = 4.5.dp, vertical = 1.dp),
      contentAlignment = Alignment.Center,
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
      ) {
        FluxCrystalIcon(sizeDp = 9.dp)
        Text(
          text = "⚡ ${(playerFluxPct * 100f).roundToInt()}%",
          color = badgeColor,
          fontSize = 6.8.sp,
          fontWeight = FontWeight.Black,
          fontFamily = FontFamily.Monospace,
          style = CompactCircleTextStyle,
          maxLines = 1,
        )
      }
    }
  }
}

/**
 * Renders the custom RIFT Flux Reactor Crystal Icon (`RiftSpriteBaker.fluxIconBitmap`) in Compose HUD elements.
 */
@Composable
fun FluxCrystalIcon(
  sizeDp: Dp = 13.dp,
  modifier: Modifier = Modifier,
) {
  val bmp = remember { RiftSpriteBaker.fluxIconBitmap.asImageBitmap() }
  Canvas(modifier = modifier.size(sizeDp)) {
    drawImage(
      image = bmp,
      dstSize = IntSize(size.width.roundToInt().coerceAtLeast(1), size.height.roundToInt().coerceAtLeast(1)),
    )
  }
}

/**
 * Unified 3-Ring Tactile Cockpit Button for `[BRAKE]` and `[MULTI]`, matching the right-hand combat arc:
 * - Outer Ring 1: Dark-obsidian 3D bezel ring with 4 cardinal ticks
 * - Inner Ring 2: Recessed inner tech ring
 * - Inner Ring 3: Glowing filled inner button cap with top specular highlight
 * - Bold centered white title + docked bottom pill badge
 */
@Composable
private fun UnifiedThreeRingCockpitButton(
  title: String,
  sub: String,
  strokeColor: Color,
  fillColor: Color,
  subColor: Color,
  sizeDp: Dp = 50.dp,
  modifier: Modifier = Modifier,
) {
  Box(
    modifier = modifier.size(sizeDp),
    contentAlignment = Alignment.Center,
  ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val cx = size.width * 0.5f
      val cy = size.height * 0.5f
      val r = size.minDimension * 0.47f

      // 1. Outer Ring 1: Dark-obsidian 3D bezel ring with 4 cardinal ticks
      drawCircle(
        color = Color(0xEB070F1E),
        radius = r * 1.05f,
        center = Offset(cx, cy),
      )
      drawCircle(
        color = strokeColor,
        radius = r,
        center = Offset(cx, cy),
        style = Stroke(width = 2.0.dp.toPx()),
      )
      for (tick in 0 until 4) {
        val a = tick * (Math.PI.toFloat() * 0.5f)
        drawLine(
          color = strokeColor,
          start = Offset(cx + cos(a) * (r * 0.88f), cy + sin(a) * (r * 0.88f)),
          end = Offset(cx + cos(a) * (r * 1.03f), cy + sin(a) * (r * 1.03f)),
          strokeWidth = 1.3.dp.toPx(),
          cap = StrokeCap.Round,
        )
      }

      // 2. Inner Ring 2: Recessed inner tech ring + filled cap
      drawCircle(
        color = fillColor.copy(alpha = 0.82f),
        radius = r * 0.86f,
        center = Offset(cx, cy),
      )
      drawCircle(
        color = Color.White.copy(alpha = 0.55f),
        radius = r * 0.82f,
        center = Offset(cx, cy),
        style = Stroke(width = 1.0.dp.toPx()),
      )

      // 3. Inner Ring 3: Top specular highlight arc
      val specR = r * 0.74f
      drawArc(
        color = Color.White.copy(alpha = 0.45f),
        startAngle = 205f,
        sweepAngle = 130f,
        useCenter = false,
        topLeft = Offset(cx - specR, cy - specR),
        size = Size(specR * 2f, specR * 2f),
        style = Stroke(width = 1.4.dp.toPx(), cap = StrokeCap.Round),
      )
    }

    // Upper-center bold white title
    Text(
      text = title,
      color = Color.White,
      fontSize = 8.0.sp,
      fontWeight = FontWeight.Black,
      fontFamily = FontFamily.SansSerif,
      style = CompactCircleTextStyle,
      maxLines = 1,
      modifier = Modifier.offset(y = (-6).dp),
    )

    // Docked bottom pill badge
    Box(
      modifier =
        Modifier.align(Alignment.BottomCenter)
          .offset(y = (-2).dp)
          .clip(RoundedCornerShape(5.dp))
          .background(Color(0xF0040A16))
          .border(1.dp, strokeColor, RoundedCornerShape(5.dp))
          .padding(horizontal = 5.dp, vertical = 1.dp),
      contentAlignment = Alignment.Center,
    ) {
      Text(
        text = sub,
        color = subColor,
        fontSize = 6.8.sp,
        fontWeight = FontWeight.ExtraBold,
        fontFamily = FontFamily.Monospace,
        style = CompactCircleTextStyle,
        maxLines = 1,
      )
    }
  }
}

@Composable
private fun TopPilotLevelBadge(
  pilotLevel: Int,
  xpProgressFraction: Float,
  riftShards: Long,
  textScale: Float,
  onClick: () -> Unit,
) {
  Column(
    modifier =
      Modifier.wrapContentSize()
        .clip(RoundedCornerShape(7.5.dp))
        .background(Color(0x4D0E1D34))
        .border(0.75.dp, Color(0xBB00F0FF), RoundedCornerShape(7.5.dp))
        .clickable { onClick() }
        .padding(horizontal = (5.5f * textScale).dp, vertical = (2.8f * textScale).dp)
        .testTag("hud_pill_pilot_level"),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy((3.0f * textScale).dp),
    ) {
      Text(
        text = "LVL $pilotLevel",
        color = Color(0xFF00F0FF),
        fontSize = (7.9f * textScale).sp,
        fontWeight = FontWeight.ExtraBold,
        fontFamily = FontFamily.Monospace,
        style = WingTelemetryTextStyle,
        maxLines = 1,
      )
      Text(
        text = "❖$riftShards",
        color = Color(0xFFFFD700),
        fontSize = (7.4f * textScale).sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace,
        style = WingTelemetryTextStyle,
        maxLines = 1,
      )
    }
    Spacer(modifier = Modifier.height((1.8f * textScale).dp))
    Box(
      modifier =
        Modifier.width((42f * textScale).dp)
          .height((2.6f * textScale).dp)
          .clip(RoundedCornerShape(1.3.dp))
          .background(Color(0xFF142844))
    ) {
      Box(
        modifier =
          Modifier.fillMaxWidth(xpProgressFraction.coerceIn(0f, 1f))
            .height((2.6f * textScale).dp)
            .background(Color(0xFF00E676), RoundedCornerShape(1.3.dp))
      )
    }
  }
}

@Composable
private fun TopPillAction(
  label: String,
  textColor: Color,
  borderColor: Color,
  textScale: Float,
  onClick: () -> Unit,
  tag: String,
) {
  Box(
    modifier =
      Modifier.wrapContentSize()
        .clip(RoundedCornerShape(7.5.dp))
        .background(Color(0x3D111C2D))
        .border(0.7.dp, borderColor.copy(alpha = 0.65f), RoundedCornerShape(7.5.dp))
        .clickable { onClick() }
        .padding(horizontal = (5.2f * textScale).dp, vertical = (4.2f * textScale).dp)
        .testTag(tag),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      text = label,
      color = textColor,
      fontSize = (8.1f * textScale).sp,
      fontWeight = FontWeight.Bold,
      fontFamily = FontFamily.SansSerif,
      style = WingTelemetryTextStyle,
      maxLines = 1,
    )
  }
}

@Composable
private fun KillFeedRow(
  entry: KillFeedEntry,
  alpha: Float,
) {
  val cleanKillerTag =
    if (entry.killerTag in RiftGameEngine.LEGACY_FOUNDRY_TAGS || entry.killerTag.isEmpty()) "GUEST" else entry.killerTag
  val cleanVictimTag =
    if (entry.victimTag in RiftGameEngine.LEGACY_FOUNDRY_TAGS || entry.victimTag.isEmpty()) "GUEST" else entry.victimTag

  val isHighBountyShutdown = entry.victimBounty > 1000
  val rowDropShadow =
    if (isHighBountyShutdown) {
      Shadow(
        color = Color(0xFFF59E0B).copy(alpha = alpha),
        offset = Offset(0f, 1f),
        blurRadius = 8f,
      )
    } else {
      Shadow(
        color = Color(0xF2000000).copy(alpha = alpha),
        offset = Offset(0f, 1.5f),
        blurRadius = 3.5f,
      )
    }
  val rowTextStyle =
    TextStyle(
      lineHeight = 11.5.sp,
      shadow = rowDropShadow,
    )

  val killerColor =
    when {
      isHighBountyShutdown -> Color(0xFFFEF08A)
      entry.killerIsLocal -> Color(0xFF38BDF8)
      entry.killerIsAllied -> Color(0xFF60A5FA)
      else -> Color(0xFFF87171)
    }.copy(alpha = alpha)

  val victimColor =
    when {
      isHighBountyShutdown -> Color(0xFFF59E0B)
      entry.victimIsLocal -> Color(0xFF38BDF8)
      entry.victimIsAllied -> Color(0xFF60A5FA)
      else -> Color(0xFFF87171)
    }.copy(alpha = alpha)

  val bountyColor =
    (when {
      isHighBountyShutdown -> Color(0xFFF59E0B)
      entry.wasHvt -> Color(0xFFFFD700)
      else -> Color(0xFFFACC15)
    }).copy(alpha = alpha)

  Row(
    modifier =
      Modifier.fillMaxWidth()
        .padding(top = 1.dp, bottom = 4.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(3.dp),
  ) {
    Text(
      text = "[$cleanKillerTag] ${entry.killerName}",
      color = killerColor,
      fontSize = 7.2.sp,
      fontWeight = FontWeight.ExtraBold,
      fontFamily = FontFamily.SansSerif,
      style = rowTextStyle,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      modifier = Modifier.weight(1f, fill = false),
    )
    Text(
      text = "⚔",
      color = (if (isHighBountyShutdown) Color(0xFFF59E0B) else Color(0xFFCBD5E1)).copy(alpha = 0.85f * alpha),
      fontSize = 6.8.sp,
      fontWeight = FontWeight.Bold,
      style = rowTextStyle,
    )
    Text(
      text = "[$cleanVictimTag] ${entry.victimName}",
      color = victimColor,
      fontSize = 7.2.sp,
      fontWeight = FontWeight.ExtraBold,
      fontFamily = FontFamily.SansSerif,
      style = rowTextStyle,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      modifier = Modifier.weight(1f, fill = false),
    )
    Text(
      text =
        when {
          isHighBountyShutdown -> "★${entry.victimBounty}"
          entry.wasHvt -> "★${entry.victimBounty}"
          else -> "${entry.victimBounty}"
        },
      color = bountyColor,
      fontSize = 7.2.sp,
      fontWeight = FontWeight.ExtraBold,
      fontFamily = FontFamily.Monospace,
      style = rowTextStyle,
      maxLines = 1,
    )
  }
}

data class PermanentUpgradeSlot(
  val iconType: Int,
  val label: String,
  val valueText: String,
  val isActive: Boolean,
  val accentColor: Color,
)

@Composable
private fun ActivePowerupsDock(
  player: ShipEntity,
  buffVersion: Int,
  frameTick: Int,
  textScale: Float,
  uiUnlocked: Boolean,
) {
  val unusedTick = frameTick + buffVersion
  val pctPerStack = RiftCalibrationState.overchargeSurgePctPerStack
  val spdPct = (player.speedOverchargeStacks * pctPerStack * 100f).roundToInt()
  val thrPct = (player.thrustOverchargeStacks * pctPerStack * 100f).roundToInt()
  val turnPct = player.turnOverchargeStacks * 12
  val maxFluxPct = player.maxFluxOverchargeStacks * 10
  val rchPct = (player.rechargeOverchargeStacks * pctPerStack * 100f).roundToInt()

  // All 6 Permanent-Until-Death Hull Upgrades (BNC, SPD, THR, TURN, MAX⚡, RCH)
  val slots =
    listOf(
      PermanentUpgradeSlot(
        iconType = 9,
        label = "BNC",
        valueText =
          when {
            player.hasBouncingBulletsUpgrade -> "ON"
            player.spec.primaryBounces > 0 -> "NAT"
            else -> "OFF"
          },
        isActive = player.hasBouncingBulletsUpgrade || player.spec.primaryBounces > 0,
        accentColor = Color(0xFFA855F7),
      ),
      PermanentUpgradeSlot(
        iconType = -10,
        label = "SPD",
        valueText = "+$spdPct%",
        isActive = player.speedOverchargeStacks > 0,
        accentColor = Color(0xFF38BDF8),
      ),
      PermanentUpgradeSlot(
        iconType = -11,
        label = "THR",
        valueText = "+$thrPct%",
        isActive = player.thrustOverchargeStacks > 0,
        accentColor = Color(0xFFF59E0B),
      ),
      PermanentUpgradeSlot(
        iconType = -13,
        label = "TURN",
        valueText = "+$turnPct%",
        isActive = player.turnOverchargeStacks > 0,
        accentColor = Color(0xFFEC4899),
      ),
      PermanentUpgradeSlot(
        iconType = -14,
        label = "MAX⚡",
        valueText = "+$maxFluxPct%",
        isActive = player.maxFluxOverchargeStacks > 0,
        accentColor = Color(0xFF00F5D4),
      ),
      PermanentUpgradeSlot(
        iconType = -12,
        label = "RCH",
        valueText = "+$rchPct%",
        isActive = player.rechargeOverchargeStacks > 0,
        accentColor = Color(0xFF10B981),
      ),
    )

  Row(
    modifier =
      Modifier.wrapContentSize()
        .clip(RoundedCornerShape(9.dp))
        .background(Color(0xD9080E18))
        .border(0.85.dp, Color(0x4D38BDF8), RoundedCornerShape(9.dp))
        .wysiwygUnlockOutline(enabled = uiUnlocked, cornerRadius = 9.dp)
        .padding(
          horizontal = (6.0f * textScale).dp,
          vertical = (2.6f * textScale).dp,
        ),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy((5.5f * textScale).dp),
  ) {
    slots.forEach { slot ->
      PermanentUpgradeBadge(slot = slot, textScale = textScale)
    }
  }
}

@Composable
private fun PermanentUpgradeBadge(
  slot: PermanentUpgradeSlot,
  textScale: Float,
) {
  val bmp = remember(slot.iconType) { RiftSpriteBaker.getBuffIconBitmap(slot.iconType).asImageBitmap() }
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy((3.0f * textScale).dp),
  ) {
    Box(
      modifier = Modifier.size((14.0f * textScale).dp),
      contentAlignment = Alignment.Center,
    ) {
      Canvas(modifier = Modifier.fillMaxSize()) {
        val strokeWidthPx = (if (slot.isActive) 1.1.dp else 0.7.dp).toPx()
        val radius = (size.minDimension - strokeWidthPx) / 2f
        if (slot.isActive) {
          drawCircle(
            color = slot.accentColor.copy(alpha = 0.25f),
            radius = radius,
            style = Stroke(width = strokeWidthPx * 1.4f),
          )
          drawCircle(
            color = slot.accentColor,
            radius = radius,
            style = Stroke(width = strokeWidthPx),
          )
        } else {
          drawCircle(
            color = Color(0x3D475569),
            radius = radius,
            style = Stroke(width = strokeWidthPx),
          )
        }
      }
      Canvas(modifier = Modifier.size((10.0f * textScale).dp)) {
        drawImage(
          image = bmp,
          dstOffset = IntOffset.Zero,
          dstSize = IntSize(size.width.toInt(), size.height.toInt()),
          alpha = if (slot.isActive) 1.0f else 0.28f,
        )
      }
    }

    Text(
      text = "${slot.label} ${slot.valueText}",
      color = if (slot.isActive) slot.accentColor else Color(0xFF64748B),
      fontSize = (6.8f * textScale).sp,
      fontWeight = FontWeight.ExtraBold,
      fontFamily = FontFamily.Monospace,
      style = WingTelemetryTextStyle,
      maxLines = 1,
    )
  }
}
