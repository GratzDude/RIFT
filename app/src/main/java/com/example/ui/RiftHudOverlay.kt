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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
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
  val armadaBadge =
    if (engine.activeArmadaName.isNotEmpty()) {
      "[${engine.activeArmadaName} ${engine.getCurrentArmadaSize()}/5]"
    } else {
      "[SOLO]"
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
    // 0. Middle-Left Auto-Scaling [⚙ CALIBRATION] Glass Pill (1:1 WYSIWYG Unlock Outline)
    val calPadH = (8.0f * textScale).dp
    val calPadV = (5.5f * textScale).dp
    Box(
      modifier =
        Modifier.align(Alignment.CenterStart)
          .offset {
            IntOffset(
              RiftCalibrationState.calibrationBtnOffset.x.roundToInt(),
              RiftCalibrationState.calibrationBtnOffset.y.roundToInt(),
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
                  RiftCalibrationState.calibrationBtnOffset += dragAmount
                }
              }
            } else {
              Modifier
            }
          )
          .padding(start = 6.dp)
          .wrapContentSize()
          .clip(RoundedCornerShape(11.dp))
          .background(Color(0xCC0B192C))
          .border(
            width = 1.1.dp,
            color = Color(0xFF38BDF8),
            shape = RoundedCornerShape(11.dp),
          )
          .wysiwygUnlockOutline(enabled = uiUnlocked, cornerRadius = 11.dp)
          .clickable { RiftCalibrationState.isCalibrationModalOpen = true }
          .padding(horizontal = calPadH, vertical = calPadV)
          .testTag("hud_btn_calibration_anchor"),
      contentAlignment = Alignment.Center,
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy((4.5f * textScale).dp),
      ) {
        Text("⚙", color = Color(0xFF38BDF8), fontSize = (11.5f * textScale).sp)
        Column(verticalArrangement = Arrangement.spacedBy((1.2f * textScale).dp)) {
          Text(
            text = "CALIBRATION",
            color = Color.White,
            fontSize = (7.5f * textScale).sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = FontFamily.Monospace,
            style = WingTelemetryTextStyle,
          )
          Text(
            text = if (uiUnlocked) "UNLOCKED" else "LOCKED",
            color = if (uiUnlocked) Color(0xFFFACC15) else Color(0xFF38BDF8),
            fontSize = (6.5f * textScale).sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            style = WingTelemetryTextStyle,
          )
        }
      }
    }

    // 1. Left-Anchored Auto-Scaling Top Navigation Bar (Strictly Constrained Left of Killboard)
    Box(
      modifier =
        Modifier.align(Alignment.TopStart)
          .padding(start = 6.dp, top = 6.dp, end = 256.dp)
          .offset {
            IntOffset(
              RiftCalibrationState.topBarOffset.x.roundToInt(),
              RiftCalibrationState.topBarOffset.y.roundToInt(),
            )
          }
          .clipToBounds()
    ) {
      Row(
        modifier =
          Modifier.wrapContentSize()
            .then(
              if (uiUnlocked) {
                Modifier.pointerInput(Unit) {
                  detectDragGestures(
                    onDragEnd = { RiftCalibrationState.savePersistedLayout(context) },
                    onDragCancel = { RiftCalibrationState.savePersistedLayout(context) },
                  ) { change, dragAmount ->
                    change.consume()
                    RiftCalibrationState.topBarOffset += dragAmount
                  }
                }
              } else {
                Modifier.horizontalScroll(rememberScrollState())
              }
            )
            .graphicsLayer {
              scaleX = RiftCalibrationState.topBarScale
              scaleY = RiftCalibrationState.topBarScale
              transformOrigin = TransformOrigin(0f, 0f)
            }
            .background(Color(0x47080E18), RoundedCornerShape(10.5.dp))
            .border(0.85.dp, Color(0x5238BDF8), RoundedCornerShape(10.5.dp))
            .wysiwygUnlockOutline(enabled = uiUnlocked, cornerRadius = 10.5.dp)
            .padding(horizontal = (4.5f * textScale).dp, vertical = (3.5f * textScale).dp)
            .testTag("hud_top_nav_bar"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy((2.8f * textScale).dp),
      ) {
        val (_, _, lvlFrac) = engine.getXpProgressInCurrentLevel()
        TopPilotLevelBadge(
          pilotLevel = engine.pilotLevel,
          xpProgressFraction = lvlFrac,
          riftShards = engine.riftShards,
          textScale = textScale,
          onClick = onOpenDossier,
        )
        TopPillAction(
          label = "🌐 NEXUS",
          textColor = Color(0xFF00F5D4),
          borderColor = Color(0xAA00F5D4),
          textScale = textScale,
          onClick = onOpenNexus,
          tag = "hud_pill_nexus",
        )
        TopPillAction(
          label = "🚀 HANGAR",
          textColor = Color(0xFF38BDF8),
          borderColor = Color(0x8838BDF8),
          textScale = textScale,
          onClick = onOpenHangar,
          tag = "hud_pill_hangar",
        )
        TopPillAction(
          label = "📖 CODEX",
          textColor = Color(0xFF34D399),
          borderColor = Color(0x8834D399),
          textScale = textScale,
          onClick = onOpenCodex,
          tag = "hud_pill_codex",
        )
        TopPillAction(
          label = "🏛️ SYNDICATE",
          textColor = Color(0xFFF59E0B),
          borderColor = Color(0x88F59E0B),
          textScale = textScale,
          onClick = onOpenSyndicateCommand,
          tag = "hud_pill_syndicates",
        )
        TopPillAction(
          label = "👥 ARMADA",
          textColor = Color(0xFF60A5FA),
          borderColor = Color(0x883B82F6),
          textScale = textScale,
          onClick = onOpenArmadaFleet,
          tag = "hud_pill_armada",
        )
        TopPillAction(
          label = if (unreadCommsCount > 0) "💬 COMMS ($unreadCommsCount)" else "💬 COMMS",
          textColor = if (unreadCommsCount > 0) Color(0xFF10B981) else Color(0xFF80D8FF),
          borderColor = if (unreadCommsCount > 0) Color(0xAA10B981) else Color(0x6680D8FF),
          textScale = textScale,
          onClick = onOpenComms,
          tag = "hud_pill_chat",
        )
        TopPillAction(
          label = "📋 DOSSIER",
          textColor = Color(0xFFFFD700),
          borderColor = Color(0x88FFD700),
          textScale = textScale,
          onClick = onOpenDossier,
          tag = "hud_pill_dossier",
        )
        TopPillAction(
          label = "⚙️ SETTINGS",
          textColor = Color(0xFFE2E8F0),
          borderColor = Color(0x668A99AD),
          textScale = textScale,
          onClick = onOpenSettings,
          tag = "hud_pill_settings",
        )
        TopPillAction(
          label = "🔍 $zoomFormatted",
          textColor = Color(0xFF94A3B8),
          borderColor = Color(0x4438BDF8),
          textScale = textScale,
          onClick = onCycleCameraZoom,
          tag = "hud_pill_zoom",
        )
      }
    }

    // 1B. Top-Center Active Ranked Mode Telemetry Pill (Skirmish, Flux Core, Stasis Breach, Syndicate Wars)
    if (engine.activeRankedMode != RankedGameMode.SECTOR_CONVERGENCE) {
      val mode = engine.activeRankedMode
      val modeAccent =
        when (mode) {
          RankedGameMode.RIFT_SKIRMISH -> Color(0xFFF59E0B)
          RankedGameMode.FLUX_CORE -> Color(0xFF00F5D4)
          RankedGameMode.STASIS_BREACH -> Color(0xFFA855F7)
          RankedGameMode.SYNDICATE_WARS -> Color(0xFFEF4444)
          RankedGameMode.SECTOR_CONVERGENCE -> Color(0xFF38BDF8)
        }
      val modeSummaryText =
        when (mode) {
          RankedGameMode.RIFT_SKIRMISH ->
            "⚔️ RIFT SKIRMISH (${engine.activeTeamSizeBracket}v${engine.activeTeamSizeBracket}) • RND ${engine.skirmishCurrentRound} • WEST ${engine.skirmishWestRoundWins} - ${engine.skirmishEastRoundWins} EAST (${engine.skirmishWestKills}-${engine.skirmishEastKills} KILLS)"
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
          RankedGameMode.STASIS_BREACH -> {
            val jailedCount = engine.ships.count { it.isInStasisBrig }
            "🔒 STASIS BREACH (10v10) • WEST REACTOR ${engine.westBrigGeneratorHp.toInt()}⚡ | EAST REACTOR ${engine.eastBrigGeneratorHp.toInt()}⚡ • BRIG: $jailedCount"
          }
          RankedGameMode.SYNDICATE_WARS ->
            "🏛️ SYNDICATE WARS (${engine.activeTeamSizeBracket}v${engine.activeTeamSizeBracket}) • [${player.syndicateTag}] ${engine.skirmishWestRoundWins} - ${engine.skirmishEastRoundWins} [${engine.rivalSyndicateWarTag}] (RND ${engine.skirmishCurrentRound})"
          RankedGameMode.SECTOR_CONVERGENCE -> mode.title
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
        )
      }
    }

    // 2. Centered Compact 4-Slot Permanent Hull Upgrade Buff Pill (BOUNCE, SPD, THR, RCH)
    Box(
      modifier =
        Modifier.align(Alignment.TopCenter)
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
              Modifier
            }
          )
          .graphicsLayer {
            scaleX = RiftCalibrationState.buffBarScale
            scaleY = RiftCalibrationState.buffBarScale
            transformOrigin = TransformOrigin(0.5f, 0f)
          }
          .padding(top = 42.dp)
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
            .padding(top = 74.dp)
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

    // 5. Bottom-Center EVE Echoes Console: Auto-Scaling Symmetric Telemetry Wings + Circular Radar Minimap
    // Uses SubcomposeLayout so BOTH wings auto-scale with user Text Size (zero hardcoded width/height)
    // while matching each other 1:1 in width and height alongside the embedded frameless swirling NEXUS vortex!
    // When UI Unlock is active, NO outer rectangular wrapper box is drawn; amber dashed outlines trace
    // directly around the two rounded Wing pills and the circular Radar border!
    Box(
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
    ) {
      SymmetricBottomConsoleLayout(
        engine = engine,
        player = player,
        speed = speed,
        sectorCode = sectorCode,
        armadaBadge = armadaBadge,
        textScale = textScale,
        uiUnlocked = uiUnlocked,
        isNexusStreaming = isNexusStreaming,
        onOpenNexus = onOpenNexus,
        frameTick = tickRead,
        rootScreenSize = rootScreenSize,
      )
    }

    // 5b. Independent Floating [BRAKE] Button (1:1 WYSIWYG labels & circle size in UI Unlock Mode)
    val brakingActive = engine.autoBraking && speed > 0
    Box(
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
          .size(48.dp)
          .wysiwygUnlockOutline(enabled = uiUnlocked, isCircle = true)
          .clip(CircleShape)
          .background(if (brakingActive) Color(0xCC3B1D0A) else Color(0xCC0E2947))
          .border(
            width = 1.4.dp,
            color = if (brakingActive) Color(0xFFF59E0B) else Color(0xFF38BDF8),
            shape = CircleShape,
          )
          .clickable(enabled = !uiUnlocked) { onEngageBrake() }
          .testTag("hud_brake_button"),
      contentAlignment = Alignment.Center,
    ) {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
      ) {
        Text(
          text = "BRAKE",
          color = if (brakingActive) Color(0xFFFACC15) else Color(0xFF38BDF8),
          fontSize = 8.2.sp,
          fontWeight = FontWeight.ExtraBold,
          fontFamily = FontFamily.SansSerif,
          style = CompactCircleTextStyle,
          maxLines = 1,
        )
        Text(
          text = if (brakingActive) "ACTIVE" else "STOP",
          color = Color.White,
          fontSize = 7.2.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.Monospace,
          style = CompactCircleTextStyle,
          maxLines = 1,
        )
      }
    }

    // 5c. Independent Floating [MULTI ON/OFF] Button — Strictly visible ONLY on Native Multi-Shot Hulls (Suppressor, Twinfang, or Rift Frame with Native Multi-Shot)
    val isNativeMulti = player.spec.supportsNativeMultifire
    val showMultiButton =
      isNativeMulti ||
        uiUnlocked ||
        RiftCalibrationState.forceAllButtonsVisible
    if (showMultiButton) {
      val multiCharges = player.multiShotCharges
      val topMultiLabel =
        when {
          player.spec.isParallelTwin -> "TWIN MULTI"
          else -> "SPLIT MULTI"
        }
      Box(
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
            .size(50.dp)
            .wysiwygUnlockOutline(enabled = uiUnlocked, isCircle = true)
            .clip(CircleShape)
            .background(
              when {
                player.multifireOn && isNativeMulti -> Color(0xCC2E2608)
                isNativeMulti -> Color(0xCC0E223B)
                else -> Color(0x88091322)
              }
            )
            .border(
              width = 1.4.dp,
              color =
                when {
                  player.multifireOn && isNativeMulti -> Color(0xFFFACC15)
                  isNativeMulti -> Color(0xFF38BDF8)
                  else -> Color(0xFF334155)
                },
              shape = CircleShape,
            )
            .clickable(enabled = !uiUnlocked) { onToggleMultifire() }
            .testTag("hud_multifire_button"),
        contentAlignment = Alignment.Center,
      ) {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center,
        ) {
          Text(
            text = topMultiLabel,
            color =
              when {
                player.multifireOn && isNativeMulti -> Color(0xFFFACC15)
                isNativeMulti -> Color(0xFF38BDF8)
                else -> Color(0xFF64748B)
              },
            fontSize = 6.6.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = FontFamily.SansSerif,
            style = CompactCircleTextStyle,
            maxLines = 1,
          )
          val subLabel =
            when {
              isNativeMulti && multiCharges > 0 -> "${if (player.multifireOn) "ON" else "OFF"}•[$multiCharges]"
              isNativeMulti -> if (player.multifireOn) "ON (0❖)" else "OFF"
              else -> "OFF"
            }
          Text(
            text = subLabel,
            color = if (isNativeMulti) Color.White else Color(0xFF64748B),
            fontSize = 7.0.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            style = CompactCircleTextStyle,
            maxLines = 1,
          )
        }
      }
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
  onOpenNexus: () -> Unit,
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

  val padStart = (13f * textScale).dp
  val padEnd = (11f * textScale).dp
  val padVert = (8f * textScale).dp
  val lineGap = (2.5f * textScale).dp
  val consoleGap = 8.dp

  @Composable
  fun LeftWingInner(fillEqualSpace: Boolean) {
    Row(
      modifier =
        (if (fillEqualSpace) Modifier.fillMaxSize() else Modifier.wrapContentSize())
          .background(Color(0xF6080E18), RoundedCornerShape(50))
          .border(1.dp, Color(0x5938BDF8), RoundedCornerShape(50))
          .wysiwygUnlockOutline(enabled = uiUnlocked && fillEqualSpace, cornerRadius = 28.dp)
          .padding(start = padStart, end = (6f * textScale).dp, top = padVert, bottom = padVert),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement =
        if (fillEqualSpace) Arrangement.SpaceBetween else Arrangement.spacedBy((6f * textScale).dp),
    ) {
      Column(
        verticalArrangement = Arrangement.spacedBy(lineGap),
      ) {
        Text(
          text = "SPEED $speed",
          color = Color(0xFF38BDF8),
          fontSize = (10.5f * textScale).sp,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.Monospace,
          style = WingTelemetryTextStyle,
          maxLines = 1,
        )
        Text(
          text = "SECTOR $sectorCode $armadaBadge".uppercase(Locale.US),
          color = Color(0xFF94A3B8),
          fontSize = (8.8f * textScale).sp,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.Monospace,
          style = WingTelemetryTextStyle,
          maxLines = 1,
        )
      }
      SwirlingRiftHudOrb(
        isStreaming = isNexusStreaming,
        onClick = onOpenNexus,
      )
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
          .padding(start = padStart, end = padEnd, top = padVert, bottom = padVert),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(
        verticalArrangement = Arrangement.spacedBy(lineGap),
      ) {
        Text(
          text = "FLUX ${player.flux.toInt()}⚡ (${(fluxPct * 100).toInt()}%)",
          color = fluxColor,
          fontSize = (10.5f * textScale).sp,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.Monospace,
          style = WingTelemetryTextStyle,
          maxLines = 1,
        )
        Text(
          text = "BOUNTY ${player.bounty}",
          color = Color(0xFFFACC15),
          fontSize = (9.5f * textScale).sp,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.Monospace,
          style = WingTelemetryTextStyle,
          maxLines = 1,
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

  val smoothedPositions = remember { mutableMapOf<String, Offset>() }

  Box(
    modifier =
      Modifier.zIndex(if (isExpanded || expandScale > 1.02f) 15f else 2f)
        .size(122.dp)
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
        .clip(CircleShape)
        .background(if (isExpanded) Color(0xE6040B16) else Color(0x99040B16))
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
      val unusedTick = frameTick
      val w = size.width
      val h = size.height
      val drawSize = w * 0.88f
      val ox = (w - drawSize) * 0.5f + panOffsetX
      val oy = (h - drawSize) * 0.5f + panOffsetY
      val scale = drawSize / RiftGameEngine.WORLD_SIZE

      if (radarBmp != null) {
        drawImage(
          image = radarBmp,
          dstOffset = IntOffset(ox.toInt(), oy.toInt()),
          dstSize = IntSize(drawSize.toInt(), drawSize.toInt()),
          alpha = 0.55f,
        )
      }

      drawCircle(
        color = Color(0x2638BDF8),
        radius = w * 0.28f,
        center = Offset(w * 0.5f, h * 0.5f),
        style = Stroke(width = 1.0f),
      )

      for ((idx, g) in engine.greens.withIndex()) {
        val key = "buff_$idx"
        val targetOffset = Offset(ox + g.x * scale, oy + g.y * scale)
        val prevOffset = smoothedPositions[key] ?: targetOffset
        val distJump = hypot(targetOffset.x - prevOffset.x, targetOffset.y - prevOffset.y)
        val lerped =
          if (distJump > 18f) {
            targetOffset
          } else {
            Offset(
              prevOffset.x + (targetOffset.x - prevOffset.x) * 0.35f,
              prevOffset.y + (targetOffset.y - prevOffset.y) * 0.35f,
            )
          }
        smoothedPositions[key] = lerped
        drawRect(
          color = Color(0xFF10B981),
          topLeft = Offset(lerped.x - 1.3f, lerped.y - 1.3f),
          size = Size(2.6f, 2.6f),
        )
      }

      for (anom in engine.anomalies) {
        val ax = ox + anom.x * scale
        val ay = oy + anom.y * scale
        val aColor =
          when (anom.type) {
            AnomalyType.PLASMA_RIFT -> Color(0xFFEF4444)
            AnomalyType.REPULSOR_RIFT -> Color.White
            AnomalyType.GRAVITY_RIFT -> Color(0xFFA855F7)
            AnomalyType.PHASE_RIFT_A -> Color(0xFF2DD4BF)
            AnomalyType.PHASE_RIFT_B -> Color(0xFF38BDF8)
          }
        drawCircle(
          color = aColor.copy(alpha = 0.85f),
          radius = 3.6f,
          center = Offset(ax, ay),
        )
        drawCircle(
          color = Color.White.copy(alpha = 0.5f),
          radius = 5.2f,
          center = Offset(ax, ay),
          style = Stroke(width = 0.8f),
        )
      }

      val nowMs = System.currentTimeMillis()
      val pulsePhase = ((nowMs % 1200L) / 1200f)
      val p = engine.localPlayer

      for (bw in engine.brickWalls) {
        val ax = ox + (if (bw.w >= bw.h) bw.x else bw.x + bw.w * 0.5f) * scale
        val ay = oy + (if (bw.w >= bw.h) bw.y + bw.h * 0.5f else bw.y) * scale
        val bx = ox + (if (bw.w >= bw.h) bw.x + bw.w else bw.x + bw.w * 0.5f) * scale
        val by = oy + (if (bw.w >= bw.h) bw.y + bw.h * 0.5f else bw.y + bw.h) * scale
        drawLine(
          color = Color(0xFF22D3EE),
          start = Offset(ax, ay),
          end = Offset(bx, by),
          strokeWidth = 2.2f,
        )
      }

      for (dc in engine.decoys) {
        val dx = ox + dc.x * scale
        val dy = oy + dc.y * scale
        val isFriendlyDecoy =
          dc.ownerId == p.id ||
            (p.armadaName.isNotEmpty() && dc.armadaName == p.armadaName) ||
            (p.syndicateTag != "GUEST" && dc.syndicateTag == p.syndicateTag)
        val dotFill = if (isFriendlyDecoy) Color(0xFF38BDF8) else Color(0xFFEF4444)
        drawCircle(
          color = dotFill,
          radius = 3.2f,
          center = Offset(dx, dy),
        )
        drawCircle(
          color = Color.White,
          radius = 3.2f,
          center = Offset(dx, dy),
          style = Stroke(width = 1.1f),
        )
      }

      for (s in engine.ships) {
        if (s.respawnTimer > 0f) {
          smoothedPositions.remove(s.id)
          continue
        }
        val isFriendly = engine.areAllied(p, s)
        if (s.isCloaked && !isFriendly && !s.isLocalPlayer) {
          smoothedPositions.remove(s.id)
          continue
        }
        val targetPos = Offset(ox + s.x * scale, oy + s.y * scale)
        val prevPos = smoothedPositions[s.id] ?: targetPos
        val jump = hypot(targetPos.x - prevPos.x, targetPos.y - prevPos.y)
        val smoothPos =
          if (jump > 24f) {
            targetPos
          } else {
            Offset(
              prevPos.x + (targetPos.x - prevPos.x) * 0.32f,
              prevPos.y + (targetPos.y - prevPos.y) * 0.32f,
            )
          }
        smoothedPositions[s.id] = smoothPos

        val sx = smoothPos.x
        val sy = smoothPos.y

        if (s.bounty >= RiftGameEngine.HVT_BOUNTY_THRESHOLD) {
          val pingR = 4.5f + pulsePhase * 10f
          drawCircle(
            color = Color(0xFFFFD700).copy(alpha = (1f - pulsePhase).coerceIn(0.20f, 0.95f)),
            radius = pingR,
            center = smoothPos,
            style = Stroke(width = 1.8f),
          )
        }

        if (s.isLocalPlayer) {
          val r = 5.4f
          val a = s.angle
          trianglePath.reset()
          trianglePath.moveTo(sx + cos(a) * r, sy + sin(a) * r)
          trianglePath.lineTo(sx + cos(a + 2.4f) * r * 0.85f, sy + sin(a + 2.4f) * r * 0.85f)
          trianglePath.lineTo(sx + cos(a - 2.4f) * r * 0.85f, sy + sin(a - 2.4f) * r * 0.85f)
          trianglePath.close()
          drawPath(path = trianglePath, color = if (s.isCloaked) Color(0xFFD8B4FE) else Color.White)
        } else {
          val dotFill = if (isFriendly) Color(0xFF3B82F6) else Color(0xFFEF4444)
          val dotRadius = if (s.isHuman) 4.0f else 3.3f
          drawCircle(
            color = dotFill,
            radius = dotRadius,
            center = smoothPos,
          )
          drawCircle(
            color = Color.White,
            radius = dotRadius,
            center = smoothPos,
            style = Stroke(width = 1.3f),
          )
        }
      }

      val fluxPct = (p.flux / p.maxFlux).coerceIn(0f, 1f)
      val critFlash =
        if (fluxPct < 0.25f || p.isIdleDraining) {
          0.35f + 0.65f * (0.5f + 0.5f * sin((nowMs % 320L) / 320f * 6.28318f))
        } else {
          1.0f
        }
      val ringColor =
        when {
          p.isIdleDraining -> Color(0xFFEF4444).copy(alpha = critFlash)
          fluxPct >= 0.75f -> {
            val t = ((fluxPct - 0.75f) / 0.25f).coerceIn(0f, 1f)
            lerp(Color(0xFF34D399), Color(0xFF00E676), t)
          }
          fluxPct >= 0.25f -> {
            val t = ((fluxPct - 0.25f) / 0.50f).coerceIn(0f, 1f)
            lerp(Color(0xFFF59E0B), Color(0xFF10B981), t)
          }
          else -> Color(0xFFEF4444).copy(alpha = critFlash)
        }

      val strokeW = 4.2.dp.toPx()
      val inset = strokeW * 0.6f
      val arcSize = Size(w - inset * 2f, h - inset * 2f)

      drawCircle(
        color = ringColor.copy(alpha = 0.28f),
        radius = (w - inset * 2f) * 0.5f,
        center = Offset(w * 0.5f, h * 0.5f),
        style = Stroke(width = strokeW),
      )

      drawArc(
        color = ringColor,
        startAngle = -90f,
        sweepAngle = (360f * fluxPct).coerceAtLeast(18f),
        useCenter = false,
        topLeft = Offset(inset, inset),
        size = arcSize,
        style = Stroke(width = strokeW, cap = StrokeCap.Round),
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
  val rchPct = (player.rechargeOverchargeStacks * pctPerStack * 100f).roundToInt()

  // Strictly 4 Permanent-Until-Death Hull Upgrades ONLY (BOUNCE, SPD, THR, RCH)
  val slots =
    listOf(
      PermanentUpgradeSlot(
        iconType = 9,
        label = "BOUNCE",
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
        .background(Color(0x59080E18))
        .border(0.85.dp, Color(0x4D38BDF8), RoundedCornerShape(9.dp))
        .wysiwygUnlockOutline(enabled = uiUnlocked, cornerRadius = 9.dp)
        .padding(
          horizontal = (7.0f * textScale).dp,
          vertical = (3.0f * textScale).dp,
        ),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy((7.0f * textScale).dp),
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
