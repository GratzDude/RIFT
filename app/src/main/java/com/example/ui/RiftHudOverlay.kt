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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.engine.AnomalyType
import com.example.engine.RiftCodex
import com.example.engine.KillFeedEntry
import com.example.engine.RiftCalibrationState
import com.example.engine.RiftGameEngine
import com.example.engine.RiftSpriteBaker
import com.example.engine.ShipEntity
import java.util.Locale
import kotlin.math.cos
import kotlin.math.hypot
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
    lineHeight = 12.sp,
  )

data class ActiveBuffBadgeData(
  val buffType: Int,
  val label: String,
  val countText: String,
  val accentColor: Color,
)

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
  onEngageBrake: () -> Unit,
  onToggleMultifire: () -> Unit,
) {
  val context = LocalContext.current
  val player = engine.localPlayer
  val tickRead = frameTick
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

  Box(
    modifier =
      Modifier.fillMaxSize()
        .onGloballyPositioned { coords ->
          if (coords.size.width > 0 && coords.size.height > 0) {
            rootScreenSize = coords.size
          }
        }
  ) {
    // 0. Middle-Left Anchored [⚙ CALIBRATION] Button (Independently Draggable & Persisted in UI Unlock Mode)
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
            if (RiftCalibrationState.isUiUnlockedForDragging) {
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
          .clip(RoundedCornerShape(12.dp))
          .background(Color(0xCC0B192C))
          .border(
            width = if (RiftCalibrationState.isUiUnlockedForDragging) 1.6.dp else 1.2.dp,
            color = if (RiftCalibrationState.isUiUnlockedForDragging) Color(0xFFFACC15) else Color(0xFF38BDF8),
            shape = RoundedCornerShape(12.dp),
          )
          .clickable { RiftCalibrationState.isCalibrationModalOpen = true }
          .padding(horizontal = 8.dp, vertical = 6.dp)
          .testTag("hud_btn_calibration_anchor"),
      contentAlignment = Alignment.Center,
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        Text("⚙", color = Color(0xFF38BDF8), fontSize = 12.sp)
        Column {
          Text(
            text = "CALIBRATION",
            color = Color.White,
            fontSize = 7.5.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = FontFamily.Monospace,
          )
          Text(
            text = if (RiftCalibrationState.isUiUnlockedForDragging) "UNLOCKED • DRAG" else "LOCKED",
            color = if (RiftCalibrationState.isUiUnlockedForDragging) Color(0xFFFACC15) else Color(0xFF38BDF8),
            fontSize = 6.5.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
          )
        }
      }
    }

    // 1. Left-Anchored Top Navigation Bar (Scalable & Draggable in Calibration Mode)
    Row(
      modifier =
        Modifier.align(Alignment.TopStart)
          .offset {
            IntOffset(
              RiftCalibrationState.topBarOffset.x.roundToInt(),
              RiftCalibrationState.topBarOffset.y.roundToInt(),
            )
          }
          .then(
            if (RiftCalibrationState.isUiUnlockedForDragging) {
              Modifier.pointerInput(Unit) {
                detectDragGestures(
                  onDragEnd = { RiftCalibrationState.savePersistedLayout(context) },
                  onDragCancel = { RiftCalibrationState.savePersistedLayout(context) },
                ) { change, dragAmount ->
                  change.consume()
                  RiftCalibrationState.topBarOffset += dragAmount
                }
              }.border(1.dp, Color(0xAA38BDF8), RoundedCornerShape(10.dp))
            } else {
              Modifier
            }
          )
          .graphicsLayer {
            scaleX = RiftCalibrationState.topBarScale
            scaleY = RiftCalibrationState.topBarScale
            transformOrigin = TransformOrigin(0f, 0f)
          }
          .padding(start = 6.dp, top = 6.dp)
          .height(21.dp)
          .background(Color(0x33080E18), RoundedCornerShape(10.dp))
          .border(0.8.dp, Color(0x3D38BDF8), RoundedCornerShape(10.dp))
          .padding(horizontal = 4.dp)
          .testTag("hud_top_nav_bar"),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
      val (_, _, lvlFrac) = engine.getXpProgressInCurrentLevel()
      TopPilotLevelBadge(
        pilotLevel = engine.pilotLevel,
        xpProgressFraction = lvlFrac,
        riftShards = engine.riftShards,
        onClick = onOpenDossier,
      )
      TopPillAction(
        label = "🚀 HANGAR",
        textColor = Color(0xFF38BDF8),
        borderColor = Color(0x8838BDF8),
        onClick = onOpenHangar,
        tag = "hud_pill_hangar",
      )
      TopPillAction(
        label = "📖 CODEX",
        textColor = Color(0xFF34D399),
        borderColor = Color(0x8834D399),
        onClick = onOpenCodex,
        tag = "hud_pill_codex",
      )
      TopPillAction(
        label = "🏛️ SYNDICATE",
        textColor = Color(0xFFF59E0B),
        borderColor = Color(0x88F59E0B),
        onClick = onOpenSyndicateCommand,
        tag = "hud_pill_syndicates",
      )
      TopPillAction(
        label = "👥 ARMADA",
        textColor = Color(0xFF60A5FA),
        borderColor = Color(0x883B82F6),
        onClick = onOpenArmadaFleet,
        tag = "hud_pill_armada",
      )
      TopPillAction(
        label = if (unreadCommsCount > 0) "💬 COMMS ($unreadCommsCount)" else "💬 COMMS",
        textColor = if (unreadCommsCount > 0) Color(0xFF10B981) else Color(0xFF80D8FF),
        borderColor = if (unreadCommsCount > 0) Color(0xAA10B981) else Color(0x6680D8FF),
        onClick = onOpenComms,
        tag = "hud_pill_chat",
      )
      TopPillAction(
        label = "📋 DOSSIER",
        textColor = Color(0xFFFFD700),
        borderColor = Color(0x88FFD700),
        onClick = onOpenDossier,
        tag = "hud_pill_dossier",
      )
      TopPillAction(
        label = "⚙️ SETTINGS",
        textColor = Color(0xFFE2E8F0),
        borderColor = Color(0x668A99AD),
        onClick = onOpenSettings,
        tag = "hud_pill_settings",
      )
      TopPillAction(
        label = "🔍 $zoomFormatted",
        textColor = Color(0xFF94A3B8),
        borderColor = Color(0x4438BDF8),
        onClick = onCycleCameraZoom,
        tag = "hud_pill_zoom",
      )
    }

    // 2. Centered Active Powerups Dock (Scalable & Draggable in Calibration Mode)
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
            if (RiftCalibrationState.isUiUnlockedForDragging) {
              Modifier.pointerInput(Unit) {
                detectDragGestures(
                  onDragEnd = { RiftCalibrationState.savePersistedLayout(context) },
                  onDragCancel = { RiftCalibrationState.savePersistedLayout(context) },
                ) { change, dragAmount ->
                  change.consume()
                  RiftCalibrationState.buffBarOffset += dragAmount
                }
              }.border(1.dp, Color(0xAAFACC15), RoundedCornerShape(10.dp))
            } else {
              Modifier
            }
          )
          .graphicsLayer {
            scaleX = RiftCalibrationState.buffBarScale
            scaleY = RiftCalibrationState.buffBarScale
            transformOrigin = TransformOrigin(0.5f, 0f)
          }
          .padding(top = 28.dp)
          .testTag("hud_active_powerups_dock"),
      contentAlignment = Alignment.Center,
    ) {
      ActivePowerupsDock(
        player = player,
        buffVersion = engine.buffVersion,
        frameTick = tickRead,
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
            if (RiftCalibrationState.isUiUnlockedForDragging) {
              Modifier.pointerInput(Unit) {
                detectDragGestures(
                  onDragEnd = { RiftCalibrationState.savePersistedLayout(context) },
                  onDragCancel = { RiftCalibrationState.savePersistedLayout(context) },
                ) { change, dragAmount ->
                  change.consume()
                  RiftCalibrationState.killLogOffset += dragAmount
                }
              }.border(1.dp, Color(0xAA38BDF8), RoundedCornerShape(8.dp))
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

    // 4. System Prompts / Tactical Toasts Container (Independently Draggable & Persisted; visible preview box in UI Unlock mode)
    val showToastBox = !toastMessage.isNullOrBlank() || RiftCalibrationState.isUiUnlockedForDragging
    if (showToastBox) {
      val displayToast =
        if (!toastMessage.isNullOrBlank()) {
          toastMessage
        } else {
          "❖ SYSTEM PROMPTS / TOASTS — DRAG TO REPOSITION"
        }
      Box(
        modifier =
          Modifier.align(Alignment.TopCenter)
            .padding(top = 58.dp)
            .offset {
              IntOffset(
                RiftCalibrationState.toastContainerOffset.x.roundToInt(),
                RiftCalibrationState.toastContainerOffset.y.roundToInt(),
              )
            }
            .then(
              if (RiftCalibrationState.isUiUnlockedForDragging) {
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
            .background(
              if (RiftCalibrationState.isUiUnlockedForDragging && toastMessage.isNullOrBlank()) {
                Color(0xD91E1B08)
              } else {
                Color(0xCC080E18)
              },
              RoundedCornerShape(10.dp),
            )
            .border(
              width = if (RiftCalibrationState.isUiUnlockedForDragging) 1.4.dp else 1.dp,
              color = if (RiftCalibrationState.isUiUnlockedForDragging) Color(0xFFFACC15) else Color(0x8838BDF8),
              shape = RoundedCornerShape(10.dp),
            )
            .padding(horizontal = 10.dp, vertical = 4.dp)
            .testTag("hud_tactical_toast"),
      ) {
        Text(
          text = displayToast,
          color = Color(0xFFFACC15),
          fontSize = 11.sp,
          fontWeight = FontWeight.ExtraBold,
          fontFamily = FontFamily.SansSerif,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }

    // 4b. High-Bounty Shutdown (>1,000 Bounty) Arena Announcement Banner
    val shutdownBanner = engine.highBountyShutdownBanner
    if (!shutdownBanner.isNullOrBlank() && engine.highBountyShutdownTimer > 0f) {
      val pulse = 0.65f + 0.35f * (0.5f + 0.5f * sin((System.currentTimeMillis() % 450L) / 450f * 6.28318f))
      Box(
        modifier =
          Modifier.align(Alignment.TopCenter)
            .padding(top = 86.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xF0241404))
            .border(1.8.dp, Color(0xFFF59E0B).copy(alpha = pulse), RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("hud_high_bounty_shutdown_banner"),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          text = shutdownBanner,
          color = Color(0xFFFEF08A),
          fontSize = 11.5.sp,
          fontWeight = FontWeight.ExtraBold,
          fontFamily = FontFamily.Monospace,
          style =
            TextStyle(
              shadow =
                Shadow(
                  color = Color(0xFFF59E0B),
                  offset = Offset(0f, 0f),
                  blurRadius = 12f,
                )
            ),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }

    // 5. Bottom-Center EVE Echoes Console: Clean Telemetry Wings + 1.5s Tap-to-Expand Minimap
    Row(
      modifier =
        Modifier.align(Alignment.BottomCenter)
          .offset {
            IntOffset(
              RiftCalibrationState.bottomConsoleOffset.x.roundToInt(),
              RiftCalibrationState.bottomConsoleOffset.y.roundToInt(),
            )
          }
          .then(
            if (RiftCalibrationState.isUiUnlockedForDragging) {
              Modifier.pointerInput(Unit) {
                detectDragGestures(
                  onDragEnd = { RiftCalibrationState.savePersistedLayout(context) },
                  onDragCancel = { RiftCalibrationState.savePersistedLayout(context) },
                ) { change, dragAmount ->
                  change.consume()
                  RiftCalibrationState.bottomConsoleOffset += dragAmount
                }
              }.border(1.dp, Color(0xAA38BDF8), RoundedCornerShape(28.dp))
            } else {
              Modifier
            }
          )
          .graphicsLayer {
            scaleX = RiftCalibrationState.bottomConsoleScale
            scaleY = RiftCalibrationState.bottomConsoleScale
            transformOrigin = TransformOrigin(0.5f, 1f)
          }
          .padding(bottom = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      // Clean Left Wing: SPEED and SECTOR readouts only
      Row(
        modifier =
          Modifier.zIndex(1f)
            .height(54.dp)
            .background(Color(0xF6080E18), RoundedCornerShape(27.dp))
            .border(1.dp, Color(0x5938BDF8), RoundedCornerShape(27.dp))
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(
          modifier = Modifier.widthIn(min = 104.dp, max = 138.dp),
          verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
          Text(
            text = "SPEED $speed",
            color = Color(0xFF38BDF8),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            style = WingTelemetryTextStyle,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Text(
            text = "SECTOR $sectorCode $armadaBadge".uppercase(),
            color = Color(0xFF94A3B8),
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            style = WingTelemetryTextStyle,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }

      // Center Translucent Radar Minimap with 1.5s Tap-to-Expand (~1.65x) & Intelligent Screen-Edge Anchoring
      CircularRadarMinimap(
        engine = engine,
        frameTick = tickRead,
        rootScreenSize = rootScreenSize,
      )

      // Clean Right Wing: FLUX (⚡) and BOUNTY readouts only
      Row(
        modifier =
          Modifier.zIndex(1f)
            .height(54.dp)
            .background(Color(0xF6080E18), RoundedCornerShape(27.dp))
            .border(1.dp, Color(0x5938BDF8), RoundedCornerShape(27.dp))
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(
          modifier = Modifier.widthIn(min = 104.dp, max = 138.dp),
          verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
          val fluxPct = (player.flux / player.maxFlux).coerceIn(0f, 1f)
          val fluxColor =
            when {
              fluxPct > 0.75f -> Color(0xFF10B981)
              fluxPct >= 0.25f -> Color(0xFFF59E0B)
              else -> Color(0xFFEF4444)
            }
          Text(
            text = "FLUX ${player.flux.toInt()}⚡ (${(fluxPct * 100).toInt()}%)",
            color = fluxColor,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            style = WingTelemetryTextStyle,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Text(
            text = "BOUNTY ${player.bounty}",
            color = Color(0xFFFACC15),
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            style = WingTelemetryTextStyle,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }
    }

    // 5b. Independent Floating [BRAKE] Button (Uncoupled from Console, Independently Draggable & Persisted)
    val brakingActive = engine.autoBraking && speed > 0
    Box(
      modifier =
        Modifier.align(Alignment.BottomCenter)
          .padding(bottom = 14.dp, end = 480.dp)
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
            if (RiftCalibrationState.isUiUnlockedForDragging) {
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
          .clip(CircleShape)
          .background(if (brakingActive) Color(0xCC3B1D0A) else Color(0xCC0E2947))
          .border(
            width = if (RiftCalibrationState.isUiUnlockedForDragging) 1.8.dp else 1.4.dp,
            color =
              when {
                RiftCalibrationState.isUiUnlockedForDragging -> Color(0xFFFACC15)
                brakingActive -> Color(0xFFF59E0B)
                else -> Color(0xFF38BDF8)
              },
            shape = CircleShape,
          )
          .clickable(enabled = !RiftCalibrationState.isUiUnlockedForDragging) { onEngageBrake() }
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
          text =
            when {
              RiftCalibrationState.isUiUnlockedForDragging -> "DRAG"
              brakingActive -> "ACTIVE"
              else -> "STOP"
            },
          color = if (RiftCalibrationState.isUiUnlockedForDragging) Color(0xFFFACC15) else Color.White,
          fontSize = 7.2.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.Monospace,
          style = CompactCircleTextStyle,
          maxLines = 1,
        )
      }
    }

    // 5c. Independent Floating [MULTI ON/OFF] Button — Strictly visible ONLY on Native Multi-Shot Hulls (3-Suppressor, 5-Twinfang, or 9-Custom Ship with Native Multi-Shot)
    val isNativeMulti = player.spec.supportsNativeMultifire
    val showMultiButton =
      isNativeMulti ||
        RiftCalibrationState.isUiUnlockedForDragging ||
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
            .padding(bottom = 14.dp, start = 480.dp)
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
              if (RiftCalibrationState.isUiUnlockedForDragging) {
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
            .clip(CircleShape)
            .background(
              when {
                player.multifireOn && isNativeMulti -> Color(0xCC2E2608)
                isNativeMulti -> Color(0xCC0E223B)
                else -> Color(0x88091322)
              }
            )
            .border(
              width = if (RiftCalibrationState.isUiUnlockedForDragging) 1.8.dp else 1.4.dp,
              color =
                when {
                  RiftCalibrationState.isUiUnlockedForDragging -> Color(0xFFFACC15)
                  player.multifireOn && isNativeMulti -> Color(0xFFFACC15)
                  isNativeMulti -> Color(0xFF38BDF8)
                  else -> Color(0xFF334155)
                },
              shape = CircleShape,
            )
            .clickable(enabled = !RiftCalibrationState.isUiUnlockedForDragging) { onToggleMultifire() }
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
              RiftCalibrationState.isUiUnlockedForDragging -> "DRAG"
              isNativeMulti && multiCharges > 0 -> "${if (player.multifireOn) "ON" else "OFF"}•[$multiCharges]"
              isNativeMulti -> if (player.multifireOn) "ON (0❖)" else "OFF"
              else -> "OFF"
            }
          Text(
            text = subLabel,
            color =
              when {
                RiftCalibrationState.isUiUnlockedForDragging -> Color(0xFFFACC15)
                isNativeMulti -> Color.White
                else -> Color(0xFF64748B)
              },
            fontSize = 7.0.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            style = CompactCircleTextStyle,
            maxLines = 1,
          )
        }
      }
    }

    // Playfield Idle Flux Drain Warning Banner ("IDLE FLUX DRAIN — ENGAGE THRUSTERS") & Safe Zone 15s Lockout HUD Badge
    val showIdleWarningBanner = (player.isIdleDraining && player.respawnTimer <= 0f) || RiftCalibrationState.isUiUnlockedForDragging
    if (showIdleWarningBanner) {
      val pulseAlpha = 0.55f + 0.45f * (0.5f + 0.5f * sin((System.currentTimeMillis() % 420L) / 420f * 6.28318f))
      val fluxPctInt = ((player.flux / player.maxFlux).coerceIn(0f, 1f) * 100f).roundToInt()
      val drainPctInt = (RiftCalibrationState.playfieldIdleDrainPctPerSec * 100f).roundToInt()
      Box(
        modifier =
          Modifier.align(Alignment.BottomCenter)
            .padding(bottom = 142.dp)
            .offset {
              IntOffset(
                RiftCalibrationState.idleWarningBannerOffset.x.roundToInt(),
                RiftCalibrationState.idleWarningBannerOffset.y.roundToInt(),
              )
            }
            .graphicsLayer {
              scaleX = RiftCalibrationState.idleWarningBannerScale
              scaleY = RiftCalibrationState.idleWarningBannerScale
              transformOrigin = TransformOrigin(0.5f, 1.0f)
            }
            .then(
              if (RiftCalibrationState.isUiUnlockedForDragging) {
                Modifier.border(1.5.dp, Color(0xFFFACC15), RoundedCornerShape(10.dp))
                  .pointerInput(Unit) {
                    detectDragGestures(
                      onDragEnd = { RiftCalibrationState.savePersistedLayout(context) },
                      onDragCancel = { RiftCalibrationState.savePersistedLayout(context) },
                    ) { change, dragAmount ->
                      change.consume()
                      RiftCalibrationState.idleWarningBannerOffset += dragAmount
                    }
                  }
              } else {
                Modifier
              }
            )
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xE62A060B))
            .border(1.5.dp, Color(0xFFEF4444).copy(alpha = pulseAlpha), RoundedCornerShape(10.dp))
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .testTag("hud_idle_flux_drain_banner"),
        contentAlignment = Alignment.Center,
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Text(
            text = "⚠ IDLE FLUX DRAIN — ENGAGE THRUSTERS ⚠",
            color = Color(0xFFFECACA),
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = FontFamily.Monospace,
          )
          Text(
            text = "DRAINING -$drainPctInt% MAX FLUX/SEC • FLUX AT $fluxPctInt%",
            color = Color(0xFFF87171),
            fontSize = 8.5.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
          )
        }
      }
    } else if (player.safeZoneLockoutTimer > 0f && player.respawnTimer <= 0f) {
      val pulse = 0.5f + 0.5f * sin((System.currentTimeMillis() % 480L) / 480f * 6.28318f)
      val borderColor = if (pulse > 0.5f) Color(0xFFEF4444) else Color(0xFFF59E0B)
      Box(
        modifier =
          Modifier.align(Alignment.BottomCenter)
            .padding(bottom = 142.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xD924070B))
            .border(1.2.dp, borderColor, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .testTag("hud_safe_zone_lockout_badge"),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          text = String.format(Locale.US, "⛔ SAFE ZONE LOCKED: %.1fs", player.safeZoneLockoutTimer),
          color = borderColor,
          fontSize = 9.5.sp,
          fontWeight = FontWeight.ExtraBold,
          fontFamily = FontFamily.Monospace,
        )
      }
    } else if (player.inSafeZone && player.respawnTimer <= 0f && player.safeZoneStayTimer >= 3.0f) {
      val remSec = (RiftCalibrationState.safeZoneIdleLimitSec - player.safeZoneStayTimer).coerceAtLeast(0f)
      Box(
        modifier =
          Modifier.align(Alignment.BottomCenter)
            .padding(bottom = 142.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xD9051824))
            .border(1.dp, if (remSec <= 5f) Color(0xFFFACC15) else Color(0xFF38BDF8), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          text = String.format(Locale.US, "SAFE ZONE EVICTION TELEPORT IN %.1fs", remSec),
          color = if (remSec <= 5f) Color(0xFFFACC15) else Color(0xFF38BDF8),
          fontSize = 9.5.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.Monospace,
        )
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
private fun CircularRadarMinimap(
  engine: RiftGameEngine,
  frameTick: Int,
  rootScreenSize: IntSize,
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

  // Butter-Smooth Minimap Interpolation State Map (prevents integer step jitter)
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
        .clip(CircleShape)
        .background(if (isExpanded) Color(0xE6040B16) else Color(0x99040B16))
        .then(
          if (!RiftCalibrationState.isUiUnlockedForDragging) {
            Modifier.pointerInput(Unit) {
              detectTapGestures(
                onTap = {
                  expandCollapseJob?.cancel()
                  isExpanded = true
                  expandCollapseJob =
                    scope.launch {
                      delay(1500L) // Stays expanded at 1.65x for 1.5 seconds, then automatically transitions back
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
      // Map fills the circular radar evenly (with pan support) so no rectangular box edge appears inside the circle
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

      // Subtle concentric radar range ring
      drawCircle(
        color = Color(0x2638BDF8),
        radius = w * 0.28f,
        center = Offset(w * 0.5f, h * 0.5f),
        style = Stroke(width = 1.0f),
      )

      // Draw Glowing Buff Icons as interpolated emerald blips on minimap
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

      // Draw Playfield Rift Obstacles on Minimap Radar
      for (anom in engine.anomalies) {
        val ax = ox + anom.x * scale
        val ay = oy + anom.y * scale
        val aColor = when (anom.type) {
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

      // Draw active Brick Energy Walls on Minimap
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

      // Draw active Decoy Hologram Clones on Minimap (fools enemy radar)
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

      // Minimap Player Indicators (Strictly on Minimap):
      // - Enemies: Solid Red dots (#EF4444) with a crisp White outer circle ring (#FFFFFF)
      // - Armada Members / Syndicate Allies: Solid Blue dots (#3B82F6) with a crisp White outer circle ring (#FFFFFF)
      // - High-Value Targets (HVT): Pulsing Gold Sonar Minimap Rings (#FFD700)
      // - Cloaked Enemies (s.isCloaked): Completely invisible on enemy minimap radar!
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

        // HVT Pulsing Gold Sonar Ring
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
          // Local Player: Crisp Directional White Triangle (Violet tint when Stealth Cloaked)
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

      // Dynamic Minimap Outer-Ring Flux Indicator:
      // - Steady vibrant Emerald Green (#10B981) when Flux > 75%
      // - Smooth transition through Amber (#F59E0B) (25%..75%)
      // - Pulses/flashes bright Critical Red (#EF4444) when Flux < 25% or when Idle Flux Drain is active
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

      // Background outer track ring
      drawCircle(
        color = ringColor.copy(alpha = 0.28f),
        radius = (w - inset * 2f) * 0.5f,
        center = Offset(w * 0.5f, h * 0.5f),
        style = Stroke(width = strokeW),
      )

      // Active Flux Capacitor Ring around minimap perimeter
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
  onClick: () -> Unit,
) {
  Column(
    modifier =
      Modifier.clip(RoundedCornerShape(8.dp))
        .background(Color(0x3B0E1D34))
        .border(0.7.dp, Color(0xAA00F0FF), RoundedCornerShape(8.dp))
        .clickable { onClick() }
        .padding(horizontal = 4.5.dp, vertical = 1.dp)
        .testTag("hud_pill_pilot_level"),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
      Text(
        text = "LVL $pilotLevel",
        color = Color(0xFF00F0FF),
        fontSize = 6.8.sp,
        fontWeight = FontWeight.ExtraBold,
        fontFamily = FontFamily.Monospace,
        lineHeight = 7.0.sp,
        maxLines = 1,
      )
      Text(
        text = "❖$riftShards",
        color = Color(0xFFFFD700),
        fontSize = 6.4.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace,
        lineHeight = 7.0.sp,
        maxLines = 1,
      )
    }
    Spacer(modifier = Modifier.height(1.dp))
    Box(
      modifier =
        Modifier.width(38.dp)
          .height(2.2.dp)
          .clip(RoundedCornerShape(1.1.dp))
          .background(Color(0xFF142844))
    ) {
      Box(
        modifier =
          Modifier.fillMaxWidth(xpProgressFraction.coerceIn(0f, 1f))
            .height(2.2.dp)
            .background(Color(0xFF00E676), RoundedCornerShape(1.1.dp))
      )
    }
  }
}

@Composable
private fun TopPillAction(
  label: String,
  textColor: Color,
  borderColor: Color,
  onClick: () -> Unit,
  tag: String,
) {
  Box(
    modifier =
      Modifier.clip(RoundedCornerShape(8.dp))
        .background(Color(0x2B111C2D))
        .border(0.6.dp, borderColor.copy(alpha = 0.55f), RoundedCornerShape(8.dp))
        .clickable { onClick() }
        .padding(horizontal = 4.dp, vertical = 1.dp)
        .testTag(tag),
  ) {
    Text(
      text = label,
      color = textColor,
      fontSize = 7.2.sp,
      fontWeight = FontWeight.Bold,
      fontFamily = FontFamily.SansSerif,
      lineHeight = 7.5.sp,
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

data class PowerupSocketSlot(
  val buffType: Int,
  val label: String,
  val isActive: Boolean,
  val countText: String,
  val accentColor: Color,
)

@Composable
private fun ActivePowerupsDock(
  player: ShipEntity,
  buffVersion: Int,
  frameTick: Int,
) {
  // Reading buffVersion and frameTick ensures reactive recomposition when crates are collected or ticks occur
  val unusedTick = frameTick + buffVersion

  var isExpanded by remember { mutableStateOf(false) }
  val scope = rememberCoroutineScope()
  var autoShrinkJob by remember { mutableStateOf<Job?>(null) }

  val scale by animateFloatAsState(
    targetValue = 1.0f,
    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
    label = "buff_dock_scale",
  )

  val regEntry = RiftCodex.getBuffRegistryEntry(player.specialWeaponType)
  val specialSlotBuffId = regEntry?.buffTypeIndex ?: 0
  val hasSpecialOrClassState =
    player.specialWeaponCharges > 0 ||
      player.rocketBoostTimer > 0f ||
      player.isCloaked ||
      player.sniperOverchargeReady
  val specialCountText =
    when {
      player.rocketBoostTimer > 0f -> String.format(Locale.US, "%.1fs", player.rocketBoostTimer)
      player.isCloaked -> "CLOAK"
      player.sniperOverchargeReady -> "+35%"
      player.specialWeaponCharges > 0 -> "[${player.specialWeaponCharges}]"
      else -> "—"
    }
  val specialLabelText =
    when {
      player.rocketBoostTimer > 0f -> "SURGE"
      player.isCloaked -> "STEALTH"
      player.sniperOverchargeReady -> "PHASE"
      player.specialWeaponCharges > 0 -> regEntry?.shortLabel ?: "SPECIAL"
      else -> "SPECIAL"
    }

  // Evaluated directly per recomposition so values are never stale
  val sockets = listOf(
    PowerupSocketSlot(
      buffType = 8,
      label = "MULTI",
      isActive = (player.spec.supportsNativeMultifire && player.multifireOn) || player.multiShotCharges > 0,
      countText =
        when {
          player.spec.supportsNativeMultifire && player.multiShotCharges > 0 -> "[${player.multiShotCharges}]★"
          player.multiShotCharges > 0 -> "[${player.multiShotCharges}]"
          player.spec.supportsNativeMultifire -> if (player.multifireOn) "ON" else "1x"
          else -> "0"
        },
      accentColor = Color(0xFFFACC15),
    ),
    PowerupSocketSlot(
      buffType = 9,
      label = "BOUNCE",
      isActive = player.bouncingBulletsShots > 0 || player.spec.primaryBounces > 0,
      countText =
        when {
          player.bouncingBulletsShots > 0 -> "[${player.bouncingBulletsShots}]"
          player.spec.primaryBounces > 0 -> "NAT"
          else -> "0"
        },
      accentColor = Color(0xFFA855F7),
    ),
    PowerupSocketSlot(
      buffType = 10,
      label = "SURGE +8%",
      isActive = player.overchargeSurgeStacks > 0,
      countText = if (player.overchargeSurgeStacks > 0) "x${player.overchargeSurgeStacks}" else "—",
      accentColor = Color(0xFFF59E0B),
    ),
    PowerupSocketSlot(
      buffType = 7,
      label = "REPEL",
      isActive = player.repels > 0,
      countText = if (player.repels > 0) "[${player.repels}]" else "0",
      accentColor = Color(0xFF38BDF8),
    ),
    PowerupSocketSlot(
      buffType = if (player.rocketBoostTimer > 0f) 5 else specialSlotBuffId,
      label = specialLabelText,
      isActive = hasSpecialOrClassState,
      countText = specialCountText,
      accentColor = Color(0xFF22D3EE),
    ),
  )

  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier =
      Modifier.graphicsLayer {
        scaleX = scale
        scaleY = scale
        transformOrigin = TransformOrigin(0.5f, 0f)
      },
  ) {
    Row(
      modifier =
        Modifier.clip(RoundedCornerShape(8.dp))
          .background(if (isExpanded) Color(0xF2081324) else Color(0x33080E18))
          .border(
            width = if (isExpanded) 1.0.dp else 0.8.dp,
            color = if (isExpanded) Color(0xFF38BDF8) else Color(0x3D38BDF8),
            shape = RoundedCornerShape(8.dp),
          )
          .clickable {
            autoShrinkJob?.cancel()
            isExpanded = true
            autoShrinkJob =
              scope.launch {
                delay(3500L) // Stays enlarged for 3.5 seconds, then smoothly shrinks back
                isExpanded = false
              }
          }
          .padding(
            horizontal = if (isExpanded) 7.dp else 5.dp,
            vertical = if (isExpanded) 2.5.dp else 1.5.dp,
          ),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(if (isExpanded) 6.dp else 4.5.dp),
    ) {
      sockets.forEach { slot ->
        PowerupSocketIndicator(slot = slot, isExpanded = isExpanded)
      }
    }

    if (isExpanded) {
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = "11-BUFF TELEMETRY • MULTI:${player.multiShotCharges} • BOUNCE:${player.bouncingBulletsShots} • SURGE:+${player.overchargeSurgeStacks * 8}%",
        color = Color(0xFF94A3B8),
        fontSize = 5.5.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace,
      )
    }
  }
}

@Composable
private fun PowerupSocketIndicator(slot: PowerupSocketSlot, isExpanded: Boolean) {
  val bmp = remember(slot.buffType) { RiftSpriteBaker.getBuffIconBitmap(slot.buffType).asImageBitmap() }
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
  ) {
    Box(
      modifier = Modifier.size(if (isExpanded) 18.dp else 14.dp),
      contentAlignment = Alignment.Center,
    ) {
      Canvas(modifier = Modifier.fillMaxSize()) {
        val strokeWidthPx = (if (slot.isActive) (if (isExpanded) 1.2.dp else 1.0.dp) else (if (isExpanded) 0.8.dp else 0.6.dp)).toPx()
        val radius = (size.minDimension - strokeWidthPx) / 2f
        if (slot.isActive) {
          // Radiant glowing halo
          drawCircle(
            color = slot.accentColor.copy(alpha = 0.28f),
            radius = radius,
            style = Stroke(width = strokeWidthPx * 1.5f),
          )
          // Radiant active status ring
          drawCircle(
            color = slot.accentColor,
            radius = radius,
            style = Stroke(width = strokeWidthPx),
          )
        } else {
          // Dimmed inactive socket track ring
          drawCircle(
            color = Color(0x33475569),
            radius = radius,
            style = Stroke(width = strokeWidthPx),
          )
        }
      }

      // Buff icon bitmap (full opacity if active, 22% alpha if inactive socket)
      Canvas(modifier = Modifier.size(if (isExpanded) 13.dp else 10.dp)) {
        drawImage(
          image = bmp,
          dstOffset = IntOffset.Zero,
          dstSize = IntSize(size.width.toInt(), size.height.toInt()),
          alpha = if (slot.isActive) 1.0f else 0.22f,
        )
      }
    }

    // Stack count or active badge
    Text(
      text = slot.countText,
      color = if (slot.isActive) slot.accentColor else Color(0xFF475569),
      fontSize = if (isExpanded) 6.8.sp else 5.8.sp,
      fontWeight = FontWeight.ExtraBold,
      fontFamily = FontFamily.Monospace,
      lineHeight = if (isExpanded) 7.5.sp else 6.5.sp,
    )

    if (isExpanded) {
      Text(
        text = slot.label,
        color = if (slot.isActive) slot.accentColor else Color(0xFF64748B),
        fontSize = 4.8.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace,
        lineHeight = 5.5.sp,
      )
    }
  }
}


