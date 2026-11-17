package com.example.ui

import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
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
import androidx.compose.ui.unit.*
import androidx.compose.ui.zIndex
import com.example.data.RiftBillingManager
import com.example.engine.*
import java.util.Locale
import kotlin.math.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val CompactCircleTextStyle = TextStyle(
 platformStyle = PlatformTextStyle(includeFontPadding = false),
 lineHeight = 8.5.sp,
 textAlign = TextAlign.Center,
)

private val WingTelemetryTextStyle = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))

private fun Modifier.wysiwygUnlockOutline(enabled: Boolean, isCircle: Boolean = false, cornerRadius: Dp = 10.dp): Modifier {
 if (!enabled) return this
 return this.drawWithContent {
  drawContent()
  val strokePx = 1.6.dp.toPx()
  val dash = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()), 0f)
  val amber = Color(0xFFFACC15)
  if (isCircle) {
   val r = (size.minDimension - strokePx) * 0.5f
   val cx = size.width * 0.5f; val cy = size.height * 0.5f
   drawCircle(amber, r, Offset(cx, cy), style = Stroke(width = strokePx, pathEffect = dash))
   val bx = cx + r * 0.707f; val by = cy - r * 0.707f
   drawCircle(Color(0xEB1E1B08), 5.5.dp.toPx(), Offset(bx, by))
   drawCircle(amber, 5.5.dp.toPx(), Offset(bx, by), style = Stroke(width = 1.2.dp.toPx()))
   drawCircle(amber, 1.8.dp.toPx(), Offset(bx, by))
  } else {
   val cr = cornerRadius.toPx()
   drawRoundRect(
    amber, Offset(strokePx * 0.5f, strokePx * 0.5f),
    Size((size.width - strokePx).coerceAtLeast(1f), (size.height - strokePx).coerceAtLeast(1f)),
    CornerRadius(cr, cr), Stroke(width = strokePx, pathEffect = dash),
   )
  }
 }
}

private const val MINIMAP_FLUX_PLASMA_AGSL =
 """
 uniform float2 uRes;
 uniform float uTime;
 uniform float uSweepFrac;
 uniform float uRIn;
 uniform float uROut;
 uniform float3 uColA;
 uniform float3 uColB;

 half4 main(float2 fragCoord) {
   float2 p = fragCoord - 0.5 * uRes;
   float r = length(p);
   if (r < uRIn || r > uROut) return half4(0.0);
   float ang = atan(p.y, p.x) + 1.5707963;
   if (ang < 0.0) ang += 6.2831853;
   float normAng = ang / 6.2831853;
   if (normAng > uSweepFrac) return half4(0.0);

   float bandT = clamp((r - uRIn) / max(uROut - uRIn, 0.001), 0.0, 1.0);
   float tube = sin(bandT * 3.1415926);
   float helix1 = 0.5 + 0.5 * sin(ang * 9.0 - uTime * 4.2 + bandT * 4.5);
   float helix2 = 0.5 + 0.5 * cos(ang * 14.0 + uTime * 3.1 - bandT * 3.8);
   float plasma = clamp(0.45 * helix1 + 0.35 * helix2 + 0.35 * tube, 0.0, 1.0);

   half3 baseCol = mix(half3(uColA), half3(uColB), half(plasma));
   float spec = pow(tube, 2.6) * (0.45 + 0.55 * helix1);
   float headGlow = exp(-28.0 * max(uSweepFrac - normAng, 0.0));
   half3 finalCol = baseCol + half3(0.85, 0.98, 1.0) * half(spec * 0.55 + headGlow * 0.65);
   half alpha = half(clamp(tube * (0.78 + 0.22 * plasma), 0.0, 1.0));
   return half4(finalCol * alpha, alpha);
 }
 """

@Composable
fun FluxEmblem(sizeDp: Dp = 13.dp, modifier: Modifier = Modifier) {
 val visualRev = RiftVisualForgeState.visualRevision
 val fluxRec = remember(visualRev) { RiftVisualForgeState.getRecipe(ForgeElementTarget.CURRENCY_FLUX_ICON) }
 val ribbonCol = remember(fluxRec) { if (fluxRec.isFactoryDefault) Color(0xFF38BDF8) else Color(fluxRec.trimColorInt) }
 val deepCol = remember(fluxRec) { if (fluxRec.isFactoryDefault) Color(0xFF0284C7) else Color(fluxRec.primaryColorInt) }
 val coreCol = remember(fluxRec) { if (fluxRec.isFactoryDefault) Color(0xFF00F5D4) else Color(fluxRec.glowColorInt) }
 Canvas(modifier = Modifier.size(sizeDp).then(modifier)) {
  val cx = size.width * 0.5f; val cy = size.height * 0.5f; val r = size.minDimension * 0.46f
  if (r <= 0.5f) return@Canvas
  drawCircle(coreCol.copy(alpha = 0.24f), r * 1.08f, Offset(cx, cy))
  drawCircle(Color(0xFF040D1A), r * 0.94f, Offset(cx, cy))
  drawCircle(deepCol.copy(alpha = 0.65f), r * 0.92f, Offset(cx, cy), style = Stroke(width = (r * 0.14f).coerceAtLeast(0.8f)))

  val outerArcR = r * 0.80f
  val innerArcR = r * 0.58f
  val outerStroke = (r * 0.20f).coerceAtLeast(1.0f)
  val innerStroke = (r * 0.15f).coerceAtLeast(0.85f)
  for (i in 0 until 3) {
   val baseDeg = i * 120f - 25f
   drawArc(
    color = deepCol,
    startAngle = baseDeg,
    sweepAngle = 86f,
    useCenter = false,
    topLeft = Offset(cx - outerArcR, cy - outerArcR),
    size = Size(outerArcR * 2f, outerArcR * 2f),
    style = Stroke(width = outerStroke, cap = StrokeCap.Round),
   )
   drawArc(
    color = ribbonCol,
    startAngle = baseDeg + 8f,
    sweepAngle = 68f,
    useCenter = false,
    topLeft = Offset(cx - outerArcR, cy - outerArcR),
    size = Size(outerArcR * 2f, outerArcR * 2f),
    style = Stroke(width = outerStroke * 0.58f, cap = StrokeCap.Round),
   )
   drawArc(
    color = coreCol.copy(alpha = 0.92f),
    startAngle = baseDeg + 52f,
    sweepAngle = 72f,
    useCenter = false,
    topLeft = Offset(cx - innerArcR, cy - innerArcR),
    size = Size(innerArcR * 2f, innerArcR * 2f),
    style = Stroke(width = innerStroke, cap = StrokeCap.Round),
   )
   val nodeRad = Math.toRadians((baseDeg + 74f).toDouble())
   val nx = cx + cos(nodeRad).toFloat() * outerArcR
   val ny = cy + sin(nodeRad).toFloat() * outerArcR
   drawCircle(Color.White, (r * 0.12f).coerceAtLeast(0.8f), Offset(nx, ny))
  }

  drawCircle(
   brush = Brush.radialGradient(
    colors = listOf(Color.White, coreCol, ribbonCol.copy(alpha = 0.45f), Color.Transparent),
    center = Offset(cx - r * 0.08f, cy - r * 0.08f),
    radius = r * 0.48f,
   ),
   radius = r * 0.46f,
   center = Offset(cx, cy),
  )
 }
}

@Composable
fun FluxCrystalIcon(sizeDp: Dp = 13.dp, modifier: Modifier = Modifier) {
 FluxEmblem(sizeDp = sizeDp, modifier = modifier)
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
 onOpenProgression: () -> Unit = onOpenDossier,
 onOpenSettings: () -> Unit,
 onCycleCameraZoom: () -> Unit = {},
 isNexusStreaming: Boolean = false,
 isModalOpen: Boolean = false,
 onOpenNexus: () -> Unit = {},
 onOpenModes: () -> Unit = onOpenNexus,
 onEngageBrake: () -> Unit,
 onToggleMultifire: () -> Unit,
) {
 val uiUnlocked = RiftCalibrationState.isUiUnlockedForDragging
 if (isModalOpen && !uiUnlocked) return
 val context = LocalContext.current
 val player = engine.localPlayer
 val tickRead = frameTick
 val visualRev = RiftVisualForgeState.visualRevision
 val textScale = engine.playfieldTextScale.coerceIn(0.65f, 1.45f)

 val speed = hypot(player.vx, player.vy).toInt()
 val sectorX = ((player.x / 1000f).toInt()).coerceIn(0, 7)
 val sectorY = ((player.y / 1000f).toInt()).coerceIn(0, 7)
 val sectorCode = "${('A' + sectorX)}${sectorY + 1}"
 val tagCore = player.syndicateTag.ifBlank { "RFT" }
 val armadaBadge = if (engine.activeArmadaName.isNotEmpty()) "[$tagCore] • ${engine.activeArmadaName} ${engine.getCurrentArmadaSize()}/5" else "[$tagCore] • SOLO"

 val (_, _, xpProgressFraction) = engine.getXpProgressInCurrentLevel()
 var lastObservedPilotLevel by remember { mutableIntStateOf(engine.pilotLevel) }
 var levelUpBannerLevel by remember { mutableIntStateOf(0) }
 LaunchedEffect(engine.pilotLevel) {
  if (engine.pilotLevel > lastObservedPilotLevel) {
   levelUpBannerLevel = engine.pilotLevel
   lastObservedPilotLevel = engine.pilotLevel
   delay(2600L)
   if (levelUpBannerLevel == engine.pilotLevel) levelUpBannerLevel = 0
  } else if (engine.pilotLevel < lastObservedPilotLevel) {
   lastObservedPilotLevel = engine.pilotLevel
   levelUpBannerLevel = 0
  }
 }

 var isSpectatorCardCollapsed by remember { mutableStateOf(false) }
 LaunchedEffect(player.deaths) { isSpectatorCardCollapsed = false }

 var rootScreenSize by remember { mutableStateOf(IntSize(1920, 1080)) }
 var bottomConsoleWidthPx by remember { mutableIntStateOf(0) }
 val density = LocalDensity.current
 val bottomConsoleSpreadDp = remember(bottomConsoleWidthPx, RiftCalibrationState.bottomConsoleScale, density) {
  val measuredDp = if (bottomConsoleWidthPx > 0) with(density) { (bottomConsoleWidthPx * RiftCalibrationState.bottomConsoleScale).toDp().value } else 0f
  max(480f, measuredDp + 64f).dp
 }

 Box(
  modifier = Modifier.fillMaxSize().onGloballyPositioned { coords ->
   if (coords.size.width > 0 && coords.size.height > 0) rootScreenSize = coords.size
  }
 ) {
  val topBarRec = remember(visualRev) { RiftVisualForgeState.getRecipe(ForgeElementTarget.HUD_TOP_BAR) }
  val topBarEffScale = (RiftCalibrationState.topBarScale * topBarRec.pixelScale).coerceIn(0.6f, 1.8f)
  if (engine.activeRankedMode != RankedGameMode.SECTOR_CONVERGENCE || engine.isCustomGameActive() || uiUnlocked) {
   val mode = engine.effectiveModeRules()
   val topBarDragMod =
    Modifier.align(Alignment.TopCenter)
     .padding(top = 6.dp)
     .offset { IntOffset(RiftCalibrationState.topBarOffset.x.roundToInt(), RiftCalibrationState.topBarOffset.y.roundToInt()) }
     .graphicsLayer { scaleX = topBarEffScale; scaleY = topBarEffScale; transformOrigin = TransformOrigin(0.5f, 0f) }
     .then(
      if (uiUnlocked) {
       Modifier.pointerInput(Unit) {
        detectDragGestures(
         onDragEnd = { RiftCalibrationState.savePersistedLayout(context) },
         onDragCancel = { RiftCalibrationState.savePersistedLayout(context) },
        ) { change, dragAmount -> change.consume(); RiftCalibrationState.topBarOffset += dragAmount }
       }.wysiwygUnlockOutline(enabled = true, cornerRadius = 9.dp)
      } else Modifier
     )
   if (mode == RankedGameMode.RIFT_CONTROL && !uiUnlocked) {
    RiftControlTugOfWarHudBar(engine, textScale, onOpenNexus, topBarDragMod.zIndex(11f), topBarRec)
   } else {
    val defaultAccent = when (mode) {
     RankedGameMode.RIFT_SKIRMISH -> Color(0xFFF59E0B)
     RankedGameMode.FLUX_CORE -> Color(0xFF00F5D4)
     RankedGameMode.STASIS_BREACH -> Color(0xFFA855F7)
     RankedGameMode.RIFT_CONTROL -> Color(0xFF38BDF8)
     RankedGameMode.SYNDICATE_WARS -> Color(0xFFEF4444)
     RankedGameMode.SECTOR_CONVERGENCE -> Color(0xFF38BDF8)
    }
    val modeAccent = if (topBarRec.isFactoryDefault) defaultAccent else Color(topBarRec.trimColorInt)
    val topBarBg = if (topBarRec.isFactoryDefault) Color(0xD9060E1C) else Color(topBarRec.primaryColorInt).copy(alpha = topBarRec.surfaceOpacity.coerceIn(0.25f, 0.98f))
    val baseModeSummaryText = when (mode) {
     RankedGameMode.RIFT_SKIRMISH ->
      "RIFT SKIRMISH (${engine.activeTeamSizeBracket}v${engine.activeTeamSizeBracket}) • RND ${engine.skirmishCurrentRound} (${engine.skirmishRoundTimerSec.toInt()}s) • WEST ${engine.skirmishWestRoundWins} - ${engine.skirmishEastRoundWins} EAST"
     RankedGameMode.FLUX_CORE -> {
      val carrierShip = engine.ships.firstOrNull {
       it.id == engine.fluxCoreCarrierEntityId || (engine.fluxCoreOrb.carrierShipId != -1 && it.shipId == engine.fluxCoreOrb.carrierShipId)
      }
      val carrierLabel = when {
       engine.fluxCoreCarrierEntityId == player.id -> "CARRIER: YOU (TIER ${engine.fluxCoreOrb.overchargeTier}/10)"
       carrierShip != null -> "CARRIER: ${carrierShip.callsign} (TIER ${engine.fluxCoreOrb.overchargeTier}/10)"
       else -> "ORB AT (${engine.fluxCoreOrb.x.toInt()},${engine.fluxCoreOrb.y.toInt()})"
      }
      "FLUX CORE • $carrierLabel • HOLD ${engine.fluxCoreOrb.holdDurationSec.toInt()}s"
     }
     RankedGameMode.STASIS_BREACH ->
      "STASIS BREACH (10v10) • WEST CORE ${engine.westBrigGeneratorHp.toInt()} FLUX | EAST CORE ${engine.eastBrigGeneratorHp.toInt()} FLUX"
     RankedGameMode.RIFT_CONTROL -> {
      val mins = (engine.riftControlMatchTimerSec.toInt().coerceAtLeast(0)) / 60
      val secs = (engine.riftControlMatchTimerSec.toInt().coerceAtLeast(0)) % 60
      String.format(Locale.US, "RIFT CONTROL (%dv%d) • %d:%02d • WEST %d - %d EAST", engine.activeTeamSizeBracket, engine.activeTeamSizeBracket, mins, secs, engine.riftControlWestScore.toInt(), engine.riftControlEastScore.toInt())
     }
     RankedGameMode.SYNDICATE_WARS ->
      "SYNDICATE WARS (${engine.activeTeamSizeBracket}v${engine.activeTeamSizeBracket}) • [${player.syndicateTag}] ${engine.skirmishWestRoundWins} - ${engine.skirmishEastRoundWins} [${engine.rivalSyndicateWarTag}] • ${engine.skirmishRoundTimerSec.toInt()}s"
     RankedGameMode.SECTOR_CONVERGENCE -> "SECTOR CONVERGENCE • TOP STATUS BAR"
    }
    val modeSummaryText = if (engine.isCustomGameActive()) "CUSTOM: ${engine.currentCustomGame?.title ?: "Custom Match"} • $baseModeSummaryText" else baseModeSummaryText

    Box(
     modifier = topBarDragMod.zIndex(11f).clip(RoundedCornerShape(9.dp))
      .background(topBarBg).border((1.0f * topBarRec.borderWeight).dp, modeAccent, RoundedCornerShape(9.dp))
      .clickable(enabled = !uiUnlocked) { onOpenNexus() }.padding(horizontal = (8f * textScale).dp, vertical = (3.5f * textScale).dp)
      .testTag("hud_ranked_mode_banner"),
     contentAlignment = Alignment.Center,
    ) {
     Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
      if (mode == RankedGameMode.FLUX_CORE) FluxEmblem(sizeDp = (10f * textScale).dp)
      Text(modeSummaryText, color = modeAccent, fontSize = (8.2f * textScale).sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, style = WingTelemetryTextStyle, maxLines = 1, overflow = TextOverflow.Ellipsis)
     }
    }
   }
  }

  if (levelUpBannerLevel > 1) {
   Box(
    modifier = Modifier.align(Alignment.TopCenter).padding(top = 48.dp).zIndex(22f).clip(RoundedCornerShape(10.dp))
     .background(Brush.horizontalGradient(listOf(Color(0xEE081B33), Color(0xEE1A2B12), Color(0xEE081B33))))
     .border(1.3.dp, Color(0xFFFFD700), RoundedCornerShape(10.dp))
     .clickable { onOpenProgression() }.padding(horizontal = (12f * textScale).dp, vertical = (4.5f * textScale).dp)
     .testTag("hud_level_up_banner"),
    contentAlignment = Alignment.Center,
   ) {
    Text(
     if (levelUpBannerLevel == 20) "PILOT LVL 20 REACHED — FULL 100-PT RIFT FRAME UNLOCKED"
     else "PILOT LEVEL $levelUpBannerLevel UNLOCKED — TAP TO VIEW REWARDS",
     color = Color(0xFFFFD700), fontSize = (9.5f * textScale).sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, style = WingTelemetryTextStyle, maxLines = 1,
    )
   }
  }

  val killFeedRec = remember(visualRev) { RiftVisualForgeState.getRecipe(ForgeElementTarget.HUD_KILL_FEED) }
  val killFeedEffScale = (RiftCalibrationState.killLogScale * killFeedRec.pixelScale).coerceIn(0.55f, 1.85f)
  Column(
   modifier = Modifier.align(Alignment.TopEnd)
    .offset { IntOffset(RiftCalibrationState.killLogOffset.x.roundToInt(), RiftCalibrationState.killLogOffset.y.roundToInt()) }
    .then(
     if (uiUnlocked) {
      Modifier.pointerInput(Unit) {
       detectDragGestures(
        onDragEnd = { RiftCalibrationState.savePersistedLayout(context) },
        onDragCancel = { RiftCalibrationState.savePersistedLayout(context) },
       ) { change, dragAmount -> change.consume(); RiftCalibrationState.killLogOffset += dragAmount }
      }.wysiwygUnlockOutline(enabled = true, cornerRadius = 8.dp)
     } else Modifier
    )
    .graphicsLayer {
     scaleX = killFeedEffScale; scaleY = killFeedEffScale
     transformOrigin = TransformOrigin(1f, 0f)
    }
    .padding(top = 6.dp, end = 8.dp).width(236.dp)
    .then(
     if (!killFeedRec.isFactoryDefault) {
      Modifier.clip(RoundedCornerShape(8.dp))
       .background(Color(killFeedRec.primaryColorInt).copy(alpha = (killFeedRec.surfaceOpacity * 0.65f).coerceIn(0.15f, 0.85f)))
       .border((0.9f * killFeedRec.borderWeight).dp, Color(killFeedRec.trimColorInt).copy(alpha = 0.65f), RoundedCornerShape(8.dp))
     } else Modifier
    )
    .padding(horizontal = 4.dp, vertical = 2.dp)
    .testTag("hud_kill_log_window"),
   verticalArrangement = Arrangement.spacedBy(2.dp),
  ) {
   if (uiUnlocked && engine.killFeed.isEmpty()) {
    Text("TACTICAL KILL FEED", color = Color(0xFF7DD3FC), fontSize = 7.4.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
   }
   engine.killFeed.take(8).forEachIndexed { index, entry ->
    KillFeedRow(entry = entry, alpha = ((8 - index) / 8.0f).coerceIn(0.15f, 1.0f))
   }
  }

  val safeZoneActive = engine.hasActiveSafeZone()
  val shutdownBanner = engine.highBountyShutdownBanner
  val hasShutdown = !shutdownBanner.isNullOrBlank() && engine.highBountyShutdownTimer > 0f
  val hasIdleDrain = player.isIdleDraining && player.respawnTimer <= 0f
  val hasToast = !toastMessage.isNullOrBlank()
  val distFromCenter = hypot(player.x - 4000f, player.y - 4000f)
  val safeZoneHalfSpan = (engine.safeZone.width() * 0.5f) * RiftCalibrationState.safeZoneScale
  val hasSafeLockout = safeZoneActive && player.safeZoneLockoutTimer > 0f && player.respawnTimer <= 0f && distFromCenter <= safeZoneHalfSpan + 260f
  val hasSafeEviction = safeZoneActive && player.inSafeZone && player.respawnTimer <= 0f && player.safeZoneStayTimer >= 3.0f

  if (hasShutdown || hasIdleDrain || hasToast || hasSafeLockout || hasSafeEviction || uiUnlocked) {
   val warnRec = remember(visualRev) { RiftVisualForgeState.getRecipe(ForgeElementTarget.HUD_WARNING_BANNER) }
   val warnScale = (RiftCalibrationState.idleWarningBannerScale * warnRec.pixelScale).coerceIn(0.6f, 1.8f)
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
     promptTitle = shutdownBanner ?: ""; promptSubtitle = null
     promptBg = Color(0xEE241404); promptBorder = Color(0xFFF59E0B).copy(alpha = pulseWave); promptTextColor = Color(0xFFFEF08A)
    }
    hasIdleDrain -> {
     promptTitle = "[!] IDLE FLUX DRAIN — ENGAGE THRUSTERS"
     promptSubtitle = "DRAINING -$drainPctInt% MAX FLUX/SEC • FLUX AT $fluxPctInt%"
     promptBg = Color(0xE62A060B); promptBorder = Color(0xFFEF4444).copy(alpha = pulseWave); promptTextColor = Color(0xFFFECACA)
    }
    hasToast -> {
     promptTitle = toastMessage ?: ""; promptSubtitle = null
     promptBg = Color(0xCC080E18); promptBorder = Color(0xAA38BDF8); promptTextColor = Color(0xFFFACC15)
    }
    hasSafeLockout -> {
     promptTitle = String.format(Locale.US, "SAFE ZONE LOCKED: %.1fs", player.safeZoneLockoutTimer); promptSubtitle = null
     promptBg = Color(0xD924070B); promptBorder = Color(0xFFEF4444).copy(alpha = pulseWave); promptTextColor = Color(0xFFFCA5A5)
    }
    hasSafeEviction -> {
     promptTitle = String.format(Locale.US, "SAFE ZONE EVICTION TELEPORT IN %.1fs", evictionRemSec); promptSubtitle = null
     promptBg = Color(0xD9051824)
     promptBorder = if (evictionRemSec <= 5f) Color(0xFFFACC15) else Color(0xFF38BDF8)
     promptTextColor = promptBorder
    }
    else -> {
     promptTitle = "SYSTEM PROMPTS / TOASTS"; promptSubtitle = null
     promptBg = Color(0xCC080E18); promptBorder = Color(0x8838BDF8); promptTextColor = Color(0xFFFACC15)
    }
   }

   val effectiveWarnBg = if (warnRec.isFactoryDefault) promptBg else Color(warnRec.primaryColorInt).copy(alpha = warnRec.surfaceOpacity.coerceIn(0.25f, 0.98f))
   val effectiveWarnBorder = if (warnRec.isFactoryDefault) promptBorder else Color(warnRec.trimColorInt).copy(alpha = pulseWave)
   val effectiveWarnText = if (warnRec.isFactoryDefault) promptTextColor else Color(warnRec.glowColorInt)
   val combinedWarnOffset = RiftCalibrationState.toastContainerOffset + RiftCalibrationState.idleWarningBannerOffset

   Box(
    modifier = Modifier.align(Alignment.TopCenter).padding(top = if (levelUpBannerLevel > 1) 76.dp else 44.dp)
     .offset { IntOffset(combinedWarnOffset.x.roundToInt(), combinedWarnOffset.y.roundToInt()) }
     .graphicsLayer { scaleX = warnScale; scaleY = warnScale }
     .then(
      if (uiUnlocked) {
       Modifier.pointerInput(Unit) {
        detectDragGestures(
         onDragEnd = { RiftCalibrationState.savePersistedLayout(context) },
         onDragCancel = { RiftCalibrationState.savePersistedLayout(context) },
        ) { change, dragAmount -> change.consume(); RiftCalibrationState.idleWarningBannerOffset += dragAmount }
       }
      } else Modifier
     )
     .wrapContentSize().clip(RoundedCornerShape(10.dp)).background(effectiveWarnBg)
     .border((1.2f * warnRec.borderWeight).dp, effectiveWarnBorder, RoundedCornerShape(10.dp))
     .wysiwygUnlockOutline(enabled = uiUnlocked, cornerRadius = 10.dp)
     .padding(horizontal = (11f * textScale).dp, vertical = (5f * textScale).dp)
     .testTag("hud_tactical_toast"),
    contentAlignment = Alignment.Center,
   ) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy((1.5f * textScale).dp)) {
     Text(promptTitle, color = effectiveWarnText, fontSize = (11.0f * textScale).sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
     if (promptSubtitle != null) {
      Text(promptSubtitle, color = Color(0xFFF87171), fontSize = (8.5f * textScale).sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, maxLines = 1, overflow = TextOverflow.Ellipsis)
     }
    }
   }
  }

  val radarScope = rememberCoroutineScope()
  var isRadarExpanded by remember { mutableStateOf(false) }
  var radarExpandCollapseJob by remember { mutableStateOf<Job?>(null) }
  val radarExpandScale by animateFloatAsState(
   targetValue = if (isRadarExpanded) 1.75f else 1.0f,
   animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow),
   label = "minimap_expand_scale",
  )
  val isRadarOnTop = isRadarExpanded || radarExpandScale > 1.02f

  Column(
   modifier = Modifier.align(Alignment.BottomCenter)
    .offset { IntOffset(RiftCalibrationState.bottomConsoleOffset.x.roundToInt(), RiftCalibrationState.bottomConsoleOffset.y.roundToInt()) }
    .then(
     if (uiUnlocked) {
      Modifier.pointerInput(Unit) {
       detectDragGestures(
        onDragEnd = { RiftCalibrationState.savePersistedLayout(context) },
        onDragCancel = { RiftCalibrationState.savePersistedLayout(context) },
       ) { change, dragAmount -> change.consume(); RiftCalibrationState.bottomConsoleOffset += dragAmount }
      }
     } else Modifier
    )
    .graphicsLayer {
     scaleX = RiftCalibrationState.bottomConsoleScale; scaleY = RiftCalibrationState.bottomConsoleScale
     transformOrigin = TransformOrigin(0.5f, 1f)
    }
    .onSizeChanged { bottomConsoleWidthPx = it.width }
    .padding(bottom = 8.dp).testTag("hud_bottom_console"),
   horizontalAlignment = Alignment.CenterHorizontally,
  ) {
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
    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
    label = "buff_bar_tap_enlarge",
   )

   val buffRec = remember(visualRev) { RiftVisualForgeState.getRecipe(ForgeElementTarget.HUD_BUFF_STRIP) }
   val buffStripEffScale = (RiftCalibrationState.buffBarScale * buffRec.pixelScale * buffBarTapScale).coerceIn(0.55f, 2.2f)
   Box(
    modifier = Modifier.zIndex(if ((isBuffBarEnlarged || buffBarTapScale > 1.02f) && !isRadarOnTop) 18f else 4f)
     .offset { IntOffset(RiftCalibrationState.buffBarOffset.x.roundToInt(), RiftCalibrationState.buffBarOffset.y.roundToInt()) }
     .then(
      if (uiUnlocked) {
       Modifier.pointerInput(Unit) {
        detectDragGestures(
         onDragEnd = { RiftCalibrationState.savePersistedLayout(context) },
         onDragCancel = { RiftCalibrationState.savePersistedLayout(context) },
        ) { change, dragAmount -> change.consume(); RiftCalibrationState.buffBarOffset += dragAmount }
       }
      } else {
       Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { buffBarBoostTrigger++ }
      }
     )
     .graphicsLayer {
      scaleX = buffStripEffScale
      scaleY = buffStripEffScale
      transformOrigin = TransformOrigin(0.5f, 1f)
     }
     .padding(bottom = 4.dp).testTag("hud_active_powerups_dock"),
    contentAlignment = Alignment.Center,
   ) {
    ActivePowerupsDock(player, engine.buffVersion, tickRead, textScale, uiUnlocked, buffRec)
   }

   SymmetricBottomConsoleLayout(
    engine = engine, player = player, speed = speed, sectorCode = sectorCode,
    armadaBadge = armadaBadge, xpProgressFraction = xpProgressFraction,
    textScale = textScale, uiUnlocked = uiUnlocked, isNexusStreaming = isNexusStreaming,
    onOpenHangar = onOpenHangar, onOpenNexus = onOpenNexus, onOpenModes = onOpenModes,
    onOpenProgression = onOpenProgression, frameTick = tickRead, rootScreenSize = rootScreenSize,
    isRadarExpanded = isRadarExpanded, radarExpandScale = radarExpandScale,
    onToggleRadarExpand = {
     radarExpandCollapseJob?.cancel()
     if (isRadarExpanded) isRadarExpanded = false
     else {
      isRadarExpanded = true
      radarExpandCollapseJob = radarScope.launch { delay(3000L); isRadarExpanded = false }
     }
    },
    modifier = Modifier.zIndex(if (isRadarOnTop) 40f else 1f),
   )
  }

  val brakeRec = remember(visualRev) { RiftVisualForgeState.getRecipe(ForgeElementTarget.HUD_BRAKE_BTN) }
  val brakeEffScale = (RiftCalibrationState.bottomConsoleScale * brakeRec.pixelScale).coerceIn(0.55f, 2.0f)
  val brakingActive = engine.autoBraking && speed > 0
  UnifiedThreeRingCockpitButton(
   title = "BRAKE",
   sub = if (brakingActive) "ACTIVE" else "STOP",
   strokeColor = when {
    brakingActive -> Color(0xFF00F5D4)
    !brakeRec.isFactoryDefault -> Color(brakeRec.trimColorInt)
    else -> Color(0xFF38BDF8)
   },
   fillColor = when {
    brakingActive -> Color(0xFF0284C7)
    !brakeRec.isFactoryDefault -> Color(brakeRec.primaryColorInt).copy(alpha = brakeRec.surfaceOpacity.coerceIn(0.25f, 0.98f))
    else -> Color(0xCC09182C)
   },
   subColor = when {
    brakingActive -> Color(0xFFFEF08A)
    !brakeRec.isFactoryDefault -> Color(brakeRec.glowColorInt)
    else -> Color(0xFFE0F2FE)
   },
   borderWeight = brakeRec.borderWeight,
   sizeDp = 50.dp,
   modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 14.dp, end = bottomConsoleSpreadDp)
    .offset { IntOffset(RiftCalibrationState.brakeBtnOffset.x.roundToInt(), RiftCalibrationState.brakeBtnOffset.y.roundToInt()) }
    .graphicsLayer {
     scaleX = brakeEffScale; scaleY = brakeEffScale
     transformOrigin = TransformOrigin(0.5f, 0.5f)
    }
    .then(
     if (uiUnlocked) {
      Modifier.pointerInput(Unit) {
       detectDragGestures(
        onDragEnd = { RiftCalibrationState.savePersistedLayout(context) },
        onDragCancel = { RiftCalibrationState.savePersistedLayout(context) },
       ) { change, dragAmount -> change.consume(); RiftCalibrationState.brakeBtnOffset += dragAmount }
      }
     } else Modifier
    )
    .wysiwygUnlockOutline(enabled = uiUnlocked, isCircle = true)
    .clickable(enabled = !uiUnlocked) { onEngageBrake() }
    .testTag("hud_brake_button"),
  )

  val isNativeMulti = player.spec.supportsNativeMultifire
  if (isNativeMulti || uiUnlocked || RiftCalibrationState.forceAllButtonsVisible) {
   val multiRec = remember(visualRev) { RiftVisualForgeState.getRecipe(ForgeElementTarget.HUD_MULTI_BTN) }
   val multiEffScale = (RiftCalibrationState.bottomConsoleScale * multiRec.pixelScale).coerceIn(0.55f, 2.0f)
   val multiCharges = player.multiShotCharges
   val topMultiLabel = if (player.spec.isParallelTwin) "TWIN" else "MULTI"
   val subLabel = when {
    isNativeMulti && multiCharges > 0 -> "${if (player.multifireOn) "ON" else "OFF"}•[$multiCharges]"
    isNativeMulti -> if (player.multifireOn) "ON" else "OFF"
    else -> "OFF"
   }
   UnifiedThreeRingCockpitButton(
    title = topMultiLabel, sub = subLabel,
    strokeColor = when {
     player.multifireOn && isNativeMulti -> if (multiRec.isFactoryDefault) Color(0xFFFACC15) else Color(multiRec.glowColorInt)
     !multiRec.isFactoryDefault -> Color(multiRec.trimColorInt)
     isNativeMulti -> Color(0xFF38BDF8)
     else -> Color(0xFF475569)
    },
    fillColor = when {
     player.multifireOn && isNativeMulti -> Color(0xFF713F12)
     !multiRec.isFactoryDefault -> Color(multiRec.primaryColorInt).copy(alpha = multiRec.surfaceOpacity.coerceIn(0.25f, 0.98f))
     isNativeMulti -> Color(0xCC09182C)
     else -> Color(0xFF1E293B)
    },
    subColor = when {
     player.multifireOn && isNativeMulti -> Color(0xFFFEF08A)
     !multiRec.isFactoryDefault -> Color(multiRec.glowColorInt)
     isNativeMulti -> Color.White
     else -> Color(0xFF94A3B8)
    },
    borderWeight = multiRec.borderWeight,
    sizeDp = 50.dp,
    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 14.dp, start = bottomConsoleSpreadDp)
     .offset { IntOffset(RiftCalibrationState.multiBtnOffset.x.roundToInt(), RiftCalibrationState.multiBtnOffset.y.roundToInt()) }
     .graphicsLayer {
      scaleX = multiEffScale; scaleY = multiEffScale
      transformOrigin = TransformOrigin(0.5f, 0.5f)
     }
     .then(
      if (uiUnlocked) {
       Modifier.pointerInput(Unit) {
        detectDragGestures(
         onDragEnd = { RiftCalibrationState.savePersistedLayout(context) },
         onDragCancel = { RiftCalibrationState.savePersistedLayout(context) },
        ) { change, dragAmount -> change.consume(); RiftCalibrationState.multiBtnOffset += dragAmount }
       }
      } else Modifier
     )
     .wysiwygUnlockOutline(enabled = uiUnlocked, isCircle = true)
     .clickable(enabled = !uiUnlocked) { onToggleMultifire() }
     .testTag("hud_multifire_button"),
   )
  }

  if (engine.isActivelySpectatingTeammates() || uiUnlocked) {
   val specRec = remember(visualRev) { RiftVisualForgeState.getRecipe(ForgeElementTarget.HUD_SPECTATOR_BOX) }
   val specScale = specRec.pixelScale.coerceIn(0.6f, 1.8f)
   val specBg = if (specRec.isFactoryDefault) Color(0xEE06101F) else Color(specRec.primaryColorInt).copy(alpha = specRec.surfaceOpacity.coerceIn(0.25f, 0.98f))
   val specBorder = if (specRec.isFactoryDefault) Color(0xAA38BDF8) else Color(specRec.trimColorInt)
   val specGlow = if (specRec.isFactoryDefault) Color(0xFF00F5D4) else Color(specRec.glowColorInt)
   val livingAllies = engine.getLivingSpectatableTeammates()
   val activeTarget = livingAllies.firstOrNull { it.id == engine.spectatingTargetShipId } ?: livingAllies.firstOrNull()
   Box(
    modifier = Modifier.align(Alignment.BottomEnd)
     .padding(end = 12.dp, bottom = 104.dp)
     .offset { IntOffset(RiftCalibrationState.spectatorBoxOffset.x.roundToInt(), RiftCalibrationState.spectatorBoxOffset.y.roundToInt()) }
     .graphicsLayer { scaleX = specScale; scaleY = specScale; transformOrigin = TransformOrigin(1f, 1f) }
     .then(
      if (uiUnlocked) {
       Modifier.pointerInput(Unit) {
        detectDragGestures(
         onDragEnd = { RiftCalibrationState.savePersistedLayout(context) },
         onDragCancel = { RiftCalibrationState.savePersistedLayout(context) },
        ) { change, dragAmount -> change.consume(); RiftCalibrationState.spectatorBoxOffset += dragAmount }
       }.wysiwygUnlockOutline(enabled = true, cornerRadius = 12.dp)
      } else Modifier
     )
     .zIndex(35f),
   ) {
    if (uiUnlocked && !engine.isActivelySpectatingTeammates()) {
     Column(
      modifier = Modifier.widthIn(min = 216.dp, max = 310.dp).clip(RoundedCornerShape(12.dp)).background(specBg)
       .border((1.2f * specRec.borderWeight).dp, specBorder, RoundedCornerShape(12.dp)).padding(horizontal = 9.dp, vertical = 7.dp)
       .testTag("hud_ally_spectator_box"),
      verticalArrangement = Arrangement.spacedBy(3.dp),
     ) {
      Text("TEAMMATE SPECTATOR SWITCHER", color = specGlow, fontSize = (8.2f * textScale).sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, style = WingTelemetryTextStyle)
      Text("DRAG TO POSITION INDEPENDENT SPECTATOR PANEL", color = Color(0xFFBAE6FD), fontSize = (7.0f * textScale).sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, style = WingTelemetryTextStyle)
     }
    } else if (isSpectatorCardCollapsed) {
     Row(
      modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(specBg)
       .border((1.2f * specRec.borderWeight).dp, specGlow, RoundedCornerShape(10.dp))
       .clickable(enabled = !uiUnlocked) { isSpectatorCardCollapsed = false }.padding(horizontal = 10.dp, vertical = 7.dp)
       .testTag("pill_expand_spectator_box"),
      verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
     ) {
      Text("SPECTATING: ${activeTarget?.callsign ?: "ALLY"}", color = specGlow, fontSize = (8.4f * textScale).sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, style = WingTelemetryTextStyle, maxLines = 1)
      Text("[^]", color = Color(0xFFE0F2FE), fontSize = (8.2f * textScale).sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace, style = WingTelemetryTextStyle)
     }
    } else {
     AllySpectatorSelectorCard(
      livingAllies = livingAllies, activeTarget = activeTarget, respawnTimerSec = player.respawnTimer,
      isRoundEliminated = engine.isSkirmishRulesActive() && player.respawnTimer > 0f,
      isStasisCaptured = player.isInStasisBrig, textScale = textScale,
      bgColor = specBg, borderColor = specBorder, accentColor = specGlow, borderWeight = specRec.borderWeight,
      onSelectAlly = { allyId -> engine.selectSpectatorTarget(allyId) },
      onCyclePrev = { engine.cycleSpectatorTarget(-1) },
      onCycleNext = { engine.cycleSpectatorTarget(1) },
      onCollapse = { isSpectatorCardCollapsed = true },
     )
    }
   }
  }
 }
}

@Composable
private fun RiftControlTugOfWarHudBar(
 engine: RiftGameEngine,
 textScale: Float,
 onClick: () -> Unit,
 modifier: Modifier = Modifier,
 topBarRec: VisualElementRecipe = RiftVisualForgeState.getRecipe(ForgeElementTarget.HUD_TOP_BAR),
) {
 val totalSec = engine.riftControlMatchTimerSec.toInt().coerceAtLeast(0)
 val timerFormatted = String.format(Locale.US, "%d:%02d", totalSec / 60, totalSec % 60)
 val westScore = engine.riftControlWestScore.toInt().coerceIn(0, 1000)
 val eastScore = engine.riftControlEastScore.toInt().coerceIn(0, 1000)
 val barBg = if (topBarRec.isFactoryDefault) Color(0xEA050D1A) else Color(topBarRec.primaryColorInt).copy(alpha = topBarRec.surfaceOpacity.coerceIn(0.25f, 0.98f))
 val barBorder = if (topBarRec.isFactoryDefault) Color(0xFF38BDF8) else Color(topBarRec.trimColorInt)

 Row(
  modifier = modifier.clip(RoundedCornerShape(10.dp)).background(barBg)
   .border((1.1f * topBarRec.borderWeight).dp, barBorder, RoundedCornerShape(10.dp)).clickable { onClick() }
   .padding(horizontal = (8f * textScale).dp, vertical = (4f * textScale).dp)
   .testTag("hud_rift_control_tug_bar"),
  verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy((8f * textScale).dp),
 ) {
  RiftTugGaugeWing("α ALPHA", "WEST $westScore", (engine.riftControlWestScore / 1000f).coerceIn(0f, 1f), Color(0xFF38BDF8), engine.riftAlphaControlSec, engine.riftAlphaLockedBy, engine.riftAlphaWestCount, engine.riftAlphaEastCount, false, textScale)
  Column(
   modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(Color(0xFF0A192F))
    .border(0.8.dp, Color(0x6638BDF8), RoundedCornerShape(6.dp))
    .padding(horizontal = (6f * textScale).dp, vertical = (2f * textScale).dp),
   horizontalAlignment = Alignment.CenterHorizontally,
  ) {
   Text("${engine.activeTeamSizeBracket}v${engine.activeTeamSizeBracket}", color = Color(0xFF00F5D4), fontSize = (7.2f * textScale).sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, style = WingTelemetryTextStyle, maxLines = 1)
   Text(timerFormatted, color = if (totalSec <= 30) Color(0xFFFACC15) else Color.White, fontSize = (8.6f * textScale).sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, style = WingTelemetryTextStyle, maxLines = 1)
  }
  RiftTugGaugeWing("Ω OMEGA", "$eastScore EAST", (engine.riftControlEastScore / 1000f).coerceIn(0f, 1f), Color(0xFFF87171), engine.riftOmegaControlSec, engine.riftOmegaLockedBy, engine.riftOmegaWestCount, engine.riftOmegaEastCount, true, textScale)
 }
}

@Composable
private fun RiftTugGaugeWing(
 riftTitle: String,
 teamScoreLabel: String,
 teamScoreFraction: Float,
 teamScoreColor: Color,
 controlSec: Float,
 lockedBy: Int,
 westCount: Int,
 eastCount: Int,
 alignEnd: Boolean,
 textScale: Float,
) {
 val isContested = lockedBy == -1 && westCount > 0 && westCount == eastCount
 val statusText = when {
  lockedBy == 0 -> "WEST LOCK"
  lockedBy == 1 -> "EAST LOCK"
  isContested -> "CONTESTED"
  abs(controlSec) >= 1f -> "${((abs(controlSec) / 30f) * 100f).roundToInt().coerceIn(1, 100)}%"
  else -> "OPEN"
 }
 val statusColor = when {
  lockedBy == 0 -> Color(0xFF00F5D4)
  lockedBy == 1 -> Color(0xFFF43F5E)
  isContested -> Color(0xFFFACC15)
  controlSec < -0.5f -> Color(0xFF38BDF8)
  controlSec > 0.5f -> Color(0xFFF87171)
  else -> Color(0xFF94A3B8)
 }

 Column(
  modifier = Modifier.width((116f * textScale).dp),
  horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start,
  verticalArrangement = Arrangement.spacedBy(2.dp),
 ) {
  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
   if (alignEnd) {
    Text(teamScoreLabel, color = teamScoreColor, fontSize = (7.4f * textScale).sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, style = WingTelemetryTextStyle, maxLines = 1)
    Text("$riftTitle • $statusText", color = statusColor, fontSize = (7.0f * textScale).sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace, style = WingTelemetryTextStyle, maxLines = 1)
   } else {
    Text("$riftTitle • $statusText", color = statusColor, fontSize = (7.0f * textScale).sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace, style = WingTelemetryTextStyle, maxLines = 1)
    Text(teamScoreLabel, color = teamScoreColor, fontSize = (7.4f * textScale).sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, style = WingTelemetryTextStyle, maxLines = 1)
   }
  }

  Canvas(modifier = Modifier.fillMaxWidth().height((8.5f * textScale).dp).clip(RoundedCornerShape(4.dp))) {
   val w = size.width; val h = size.height; val gaugeH = h * 0.68f; val midX = w * 0.5f
   drawRoundRect(Color(0xFF0F1E36), Offset.Zero, Size(w, gaugeH), CornerRadius(3.dp.toPx(), 3.dp.toPx()))
   val norm = when (lockedBy) { 0 -> -1f; 1 -> 1f; else -> (controlSec / 30f).coerceIn(-1f, 1f) }
   if (norm < -0.01f) {
    val fillW = abs(norm) * midX
    drawRoundRect(if (lockedBy == 0) Color(0xFF00F5D4) else Color(0xFF38BDF8), Offset(midX - fillW, 0f), Size(fillW, gaugeH), CornerRadius(2.dp.toPx(), 2.dp.toPx()))
   } else if (norm > 0.01f) {
    val fillW = norm * midX
    drawRoundRect(if (lockedBy == 1) Color(0xFFF43F5E) else Color(0xFFEF4444), Offset(midX, 0f), Size(fillW, gaugeH), CornerRadius(2.dp.toPx(), 2.dp.toPx()))
   }
   if (isContested) drawCircle(Color(0xFFFACC15), gaugeH * 0.52f, Offset(midX, gaugeH * 0.5f))
   else drawLine(Color(0xCCE2E8F0), Offset(midX, 0f), Offset(midX, gaugeH), 1.2.dp.toPx())
   val scoreTrackY = h - 1.8.dp.toPx()
   drawRect(Color(0x441E293B), Offset(0f, scoreTrackY), Size(w, 1.8.dp.toPx()))
   val scoreW = w * teamScoreFraction
   drawRect(teamScoreColor, Offset(if (alignEnd) w - scoreW else 0f, scoreTrackY), Size(scoreW, 1.8.dp.toPx()))
  }
 }
}

@Composable
private fun AllySpectatorSelectorCard(
 livingAllies: List<ShipEntity>,
 activeTarget: ShipEntity?,
 respawnTimerSec: Float,
 isRoundEliminated: Boolean,
 isStasisCaptured: Boolean,
 textScale: Float,
 bgColor: Color = Color(0xEE06101F),
 borderColor: Color = Color(0xAA38BDF8),
 accentColor: Color = Color(0xFF00F5D4),
 borderWeight: Float = 1.0f,
 onSelectAlly: (String) -> Unit,
 onCyclePrev: () -> Unit,
 onCycleNext: () -> Unit,
 onCollapse: () -> Unit,
) {
 val statusLabel = when {
  isStasisCaptured -> "IN STASIS BRIG"
  isRoundEliminated -> "ROUND SIDELINED"
  else -> String.format(Locale.US, "RESPAWN %.1fs", respawnTimerSec.coerceAtLeast(0f))
 }

 Column(
  modifier = Modifier.widthIn(min = 216.dp, max = 310.dp).clip(RoundedCornerShape(12.dp)).background(bgColor)
   .border((1.2f * borderWeight).dp, borderColor, RoundedCornerShape(12.dp)).padding(horizontal = 9.dp, vertical = 7.dp)
   .testTag("hud_ally_spectator_box"),
  verticalArrangement = Arrangement.spacedBy(6.dp),
 ) {
  Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
   Column {
    Text("TEAMMATE SPECTATOR (${livingAllies.size})", color = accentColor, fontSize = (8.2f * textScale).sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, style = WingTelemetryTextStyle)
    Text(statusLabel, color = Color(0xFFFACC15), fontSize = (7.2f * textScale).sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, style = WingTelemetryTextStyle)
   }
   Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
    if (livingAllies.size > 1) {
     Box(
      modifier = Modifier.size(26.dp).clip(RoundedCornerShape(6.dp)).background(Color(0xFF0D213B)).border(0.9.dp, Color(0xFF38BDF8), RoundedCornerShape(6.dp)).clickable { onCyclePrev() }.testTag("btn_spectate_prev"),
      contentAlignment = Alignment.Center,
     ) { Text("<", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, style = CompactCircleTextStyle) }
     Box(
      modifier = Modifier.size(26.dp).clip(RoundedCornerShape(6.dp)).background(Color(0xFF0D213B)).border(0.9.dp, Color(0xFF38BDF8), RoundedCornerShape(6.dp)).clickable { onCycleNext() }.testTag("btn_spectate_next"),
      contentAlignment = Alignment.Center,
     ) { Text(">", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, style = CompactCircleTextStyle) }
    }
    Box(
     modifier = Modifier.size(26.dp).clip(RoundedCornerShape(6.dp)).background(Color(0xFF1A1026)).border(0.9.dp, Color(0xFF94A3B8), RoundedCornerShape(6.dp)).clickable { onCollapse() }.testTag("btn_collapse_spectator_box"),
     contentAlignment = Alignment.Center,
    ) { Text("X", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, style = CompactCircleTextStyle) }
   }
  }

  Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
   livingAllies.forEach { ally ->
    val isSelected = ally.id == activeTarget?.id
    val allyBmp = remember(ally.shipId, RiftSpriteBaker.customShip9Version) { RiftSpriteBaker.getShipSprite(ally.shipId, isAllied = true).asImageBitmap() }
    val allyFluxPct = (ally.flux / ally.maxFlux.coerceAtLeast(1f)).coerceIn(0f, 1f)
    val allyFluxCol = when { allyFluxPct >= 0.65f -> Color(0xFF10B981); allyFluxPct >= 0.30f -> Color(0xFFF59E0B); else -> Color(0xFFEF4444) }

    Column(
     modifier = Modifier.width(74.dp).clip(RoundedCornerShape(8.dp))
      .background(if (isSelected) Color(0xFF0D2847) else Color(0xFF081526))
      .border(if (isSelected) 1.4.dp else 0.8.dp, if (isSelected) Color(0xFF00F5D4) else Color(0x5538BDF8), RoundedCornerShape(8.dp))
      .clickable { onSelectAlly(ally.id) }.padding(horizontal = 5.dp, vertical = 4.dp)
      .testTag("spectator_ally_card_${ally.id}"),
     horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.5.dp),
    ) {
     Canvas(modifier = Modifier.size(30.dp)) {
      drawImage(allyBmp, dstSize = IntSize(size.width.roundToInt().coerceAtLeast(1), size.height.roundToInt().coerceAtLeast(1)))
     }
     Text(ally.callsign, color = if (isSelected) Color(0xFF00F5D4) else Color(0xFFE2E8F0), fontSize = 7.2.sp, fontWeight = FontWeight.ExtraBold, style = WingTelemetryTextStyle, maxLines = 1, overflow = TextOverflow.Ellipsis)
     Box(modifier = Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(1.5.dp)).background(Color(0xFF1E293B))) {
      Box(modifier = Modifier.fillMaxWidth(allyFluxPct).height(3.dp).background(allyFluxCol, RoundedCornerShape(1.5.dp)))
     }
    }
   }
  }

  Text("CURRENTLY SPECTATING: [${activeTarget?.callsign ?: "---"}]", color = Color(0xFFBAE6FD), fontSize = (7.3f * textScale).sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, style = WingTelemetryTextStyle, maxLines = 1, overflow = TextOverflow.Ellipsis)
 }
}

@Composable
private fun SymmetricBottomConsoleLayout(
 engine: RiftGameEngine,
 player: ShipEntity,
 speed: Int,
 sectorCode: String,
 armadaBadge: String,
 xpProgressFraction: Float,
 textScale: Float,
 uiUnlocked: Boolean,
 isNexusStreaming: Boolean,
 onOpenHangar: () -> Unit,
 onOpenNexus: () -> Unit,
 onOpenModes: () -> Unit,
 onOpenProgression: () -> Unit,
 frameTick: Int,
 rootScreenSize: IntSize,
 isRadarExpanded: Boolean,
 radarExpandScale: Float,
 onToggleRadarExpand: () -> Unit,
 modifier: Modifier = Modifier,
) {
 val padStart = (8f * textScale).dp
 val padVert = (5.5f * textScale).dp
 val lineGap = (1.6f * textScale).dp
 val consoleGap = 8.dp
 val visualRev = RiftVisualForgeState.visualRevision
 val consoleRec = remember(visualRev) { RiftVisualForgeState.getRecipe(ForgeElementTarget.HUD_MINIMAP_CONSOLE) }
 val consoleBgColor = remember(consoleRec) {
  if (consoleRec.isFactoryDefault) Color(0xF6080E18)
  else Color(consoleRec.primaryColorInt).copy(alpha = consoleRec.surfaceOpacity.coerceIn(0.4f, 0.98f))
 }
 val consoleBorderColor = remember(consoleRec) {
  if (consoleRec.isFactoryDefault) Color(0x5938BDF8) else Color(consoleRec.trimColorInt).copy(alpha = 0.65f)
 }
 val shipBmp = remember(player.shipId, RiftSpriteBaker.customShip9Version) { RiftSpriteBaker.getShipSprite(player.shipId, isAllied = true).asImageBitmap() }

 @Composable
 fun LeftWingInner(fillEqualSpace: Boolean) {
  Row(
   modifier = (if (fillEqualSpace) Modifier.fillMaxSize() else Modifier.wrapContentSize())
    .background(consoleBgColor, RoundedCornerShape(50))
    .border(1.dp, consoleBorderColor, RoundedCornerShape(50))
    .wysiwygUnlockOutline(enabled = uiUnlocked && fillEqualSpace, cornerRadius = 28.dp)
    .padding(start = (5f * textScale).dp, end = (6.5f * textScale).dp, top = padVert, bottom = padVert),
   verticalAlignment = Alignment.CenterVertically,
   horizontalArrangement = if (fillEqualSpace) Arrangement.SpaceBetween else Arrangement.spacedBy((5.5f * textScale).dp),
  ) {
   Box(
    modifier = Modifier.size(36.dp).clickable(enabled = !uiUnlocked) { onOpenHangar() }.testTag("hud_btn_hangar_ship"),
    contentAlignment = Alignment.Center,
   ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
     val cx = size.width * 0.5f; val cy = size.height * 0.5f
     val ringCol = if (engine.isShipLockedForMatch) Color(0xFFF59E0B) else Color(0xFF00F5D4)
     drawOval(ringCol.copy(alpha = 0.24f), Offset(cx - size.width * 0.44f, cy + size.height * 0.16f), Size(size.width * 0.88f, size.height * 0.28f))
     drawOval(ringCol.copy(alpha = 0.85f), Offset(cx - size.width * 0.44f, cy + size.height * 0.16f), Size(size.width * 0.88f, size.height * 0.28f), style = Stroke(width = 1.1.dp.toPx()))
     val pad = 1.dp.toPx()
     val dstSize = (size.minDimension - pad * 2f).toInt().coerceAtLeast(1)
     drawImage(shipBmp, dstOffset = IntOffset(pad.toInt(), (pad * 0.4f).toInt()), dstSize = IntSize(dstSize, dstSize))
    }
    if (engine.isShipLockedForMatch) {
     Box(
      modifier = Modifier.align(Alignment.TopEnd).size(10.dp).clip(CircleShape).background(Color(0xEE1E1604)).border(0.8.dp, Color(0xFFF59E0B), CircleShape),
      contentAlignment = Alignment.Center,
     ) {
      Canvas(modifier = Modifier.size(6.5.dp)) {
       val w = size.width; val h = size.height
       drawArc(Color(0xFFFBBF24), 180f, 180f, false, Offset(w * 0.22f, h * 0.10f), Size(w * 0.56f, h * 0.50f), style = Stroke(width = 1.dp.toPx(), cap = StrokeCap.Round))
       drawRoundRect(Color(0xFFF59E0B), Offset(w * 0.16f, h * 0.42f), Size(w * 0.68f, h * 0.48f), CornerRadius(1.dp.toPx(), 1.dp.toPx()))
      }
     }
    }
   }

   Column(verticalArrangement = Arrangement.spacedBy(0.5.dp)) {
    Text("SPEED $speed", color = Color(0xFF00F5D4), fontSize = (8.4f * textScale).sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, style = WingTelemetryTextStyle, maxLines = 1)
    Text("SECTOR $sectorCode", color = Color(0xFFE2E8F0), fontSize = (7.6f * textScale).sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, style = WingTelemetryTextStyle, maxLines = 1)
    Text(armadaBadge.uppercase(Locale.US), color = Color(0xFFFACC15), fontSize = (7.2f * textScale).sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace, style = WingTelemetryTextStyle, maxLines = 1)
   }

   Box(
    modifier = Modifier.size(32.dp).clip(RoundedCornerShape(9.dp))
     .background(Brush.verticalGradient(listOf(Color(0xFF142842), Color(0xFF071222))))
     .border(1.2.dp, Color(0xFF38BDF8), RoundedCornerShape(9.dp))
     .clickable(enabled = !uiUnlocked) { onOpenModes() }.testTag("hud_btn_modes"),
    contentAlignment = Alignment.Center,
   ) {
    Canvas(modifier = Modifier.size(20.dp)) {
     val w = size.width; val h = size.height; val cx = w * 0.5f; val cy = h * 0.5f
     val diamond = Path().apply {
      moveTo(cx, h * 0.06f); lineTo(w * 0.94f, cy); lineTo(cx, h * 0.94f); lineTo(w * 0.06f, cy); close()
     }
     drawPath(diamond, Color(0x2800F5D4))
     drawPath(diamond, Color(0x8838BDF8), style = Stroke(width = 1.dp.toPx()))
     val bladeStroke = 2.2.dp.toPx()
     drawLine(Color(0xFF00F5D4), Offset(w * 0.22f, h * 0.78f), Offset(w * 0.82f, h * 0.18f), bladeStroke, StrokeCap.Round)
     drawLine(Color(0xFFFACC15), Offset(w * 0.20f, h * 0.58f), Offset(w * 0.42f, h * 0.80f), 1.8.dp.toPx(), StrokeCap.Round)
     drawLine(Color(0xFF38BDF8), Offset(w * 0.78f, h * 0.78f), Offset(w * 0.18f, h * 0.18f), bladeStroke, StrokeCap.Round)
     drawLine(Color(0xFFFACC15), Offset(w * 0.58f, h * 0.80f), Offset(w * 0.80f, h * 0.58f), 1.8.dp.toPx(), StrokeCap.Round)
     drawCircle(Color.White, 2.0.dp.toPx(), Offset(cx, cy))
    }
   }
  }
 }

 @Composable
 fun RightWingInner(fillEqualSpace: Boolean) {
  val shardDisplay = if (RiftBillingManager.TESTER_INFINITE_SHARDS_ACTIVE) "INF" else engine.riftShards.toString()
  Row(
   modifier = (if (fillEqualSpace) Modifier.fillMaxSize() else Modifier.wrapContentSize())
    .background(consoleBgColor, RoundedCornerShape(50))
    .border(1.dp, consoleBorderColor, RoundedCornerShape(50))
    .wysiwygUnlockOutline(enabled = uiUnlocked && fillEqualSpace, cornerRadius = 28.dp)
    .padding(start = padStart, end = (5.5f * textScale).dp, top = padVert, bottom = padVert),
   verticalAlignment = Alignment.CenterVertically,
   horizontalArrangement = if (fillEqualSpace) Arrangement.SpaceBetween else Arrangement.spacedBy((5.5f * textScale).dp),
  ) {
   Column(verticalArrangement = Arrangement.spacedBy(lineGap)) {
    Text(
     "BTY ${player.bounty} • K/D ${engine.allTimeKills}/${engine.allTimeDeaths}",
     color = Color(0xFFFACC15), fontSize = (8.0f * textScale).sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, style = WingTelemetryTextStyle, maxLines = 1,
     modifier = Modifier.clickable(enabled = !uiUnlocked) { onOpenNexus() }.testTag("hud_bounty_kd_readout"),
    )
    Row(
     verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy((4f * textScale).dp),
     modifier = Modifier.clickable(enabled = !uiUnlocked) { onOpenProgression() }.testTag("hud_console_progression_link"),
    ) {
     Text("RES ${engine.liveResonance} • ❖ $shardDisplay", color = Color(0xFF00F5D4), fontSize = (7.4f * textScale).sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace, style = WingTelemetryTextStyle, maxLines = 1)
     Box(modifier = Modifier.width((26f * textScale).dp).height((2.6f * textScale).dp).clip(RoundedCornerShape(1.3.dp)).background(Color(0xFF142844))) {
      Box(modifier = Modifier.fillMaxWidth(xpProgressFraction.coerceIn(0.08f, 1f)).height((2.6f * textScale).dp).background(Color(0xFF00F5D4), RoundedCornerShape(1.3.dp)))
     }
    }
   }
   val nexusBtnRec = remember(visualRev) { RiftVisualForgeState.getRecipe(ForgeElementTarget.HUD_NEXUS_BUTTON) }
   Box(
    modifier = Modifier
     .offset { IntOffset(RiftCalibrationState.nexusBtnOffset.x.roundToInt(), RiftCalibrationState.nexusBtnOffset.y.roundToInt()) }
     .graphicsLayer {
      val ns = nexusBtnRec.pixelScale.coerceIn(0.65f, 1.65f)
      scaleX = ns; scaleY = ns
     }
     .testTag("hud_btn_nexus"),
   ) {
    SwirlingRiftHudOrb(isStreaming = isNexusStreaming, onClick = onOpenNexus)
   }
  }
 }

 SubcomposeLayout(modifier = modifier) { constraints ->
  val unbounded = Constraints()
  val probeLeft = subcompose("probe_left") { LeftWingInner(false) }.first().measure(unbounded)
  val probeRight = subcompose("probe_right") { RightWingInner(false) }.first().measure(unbounded)
  val sharedWingW = max(probeLeft.width, probeRight.width)
  val sharedWingH = max(probeLeft.height, probeRight.height)
  val wingFixedConstraints = Constraints.fixed(sharedWingW, sharedWingH)

  val leftPlaceable = subcompose("left_wing") { LeftWingInner(true) }.first().measure(wingFixedConstraints)
  val radarPlaceable = subcompose("radar") {
   CircularRadarMinimap(engine, frameTick, rootScreenSize, isRadarExpanded, radarExpandScale, onToggleRadarExpand, uiUnlocked)
  }.first().measure(unbounded)
  val rightPlaceable = subcompose("right_wing") { RightWingInner(true) }.first().measure(wingFixedConstraints)

  val gapPx = consoleGap.roundToPx()
  val totalW = (leftPlaceable.width + gapPx + radarPlaceable.width + gapPx + rightPlaceable.width).coerceAtMost(constraints.maxWidth)
  val totalH = max(radarPlaceable.height, sharedWingH)

  layout(totalW, totalH) {
   val wingY = (totalH - sharedWingH) / 2
   val radarY = (totalH - radarPlaceable.height) / 2
   leftPlaceable.placeRelative(0, wingY, zIndex = 1f)
   val radarX = leftPlaceable.width + gapPx
   val rightX = radarX + radarPlaceable.width + gapPx
   rightPlaceable.placeRelative(rightX, wingY, zIndex = 1f)
   radarPlaceable.placeRelative(radarX, radarY, zIndex = if (isRadarExpanded) 30f else 2f)
  }
 }
}

@Composable
private fun CircularRadarMinimap(
 engine: RiftGameEngine,
 frameTick: Int,
 rootScreenSize: IntSize,
 isExpanded: Boolean,
 expandScale: Float,
 onToggleExpand: () -> Unit,
 uiUnlocked: Boolean = false,
) {
 var panOffsetX by remember { mutableFloatStateOf(0f) }
 var panOffsetY by remember { mutableFloatStateOf(0f) }
 LaunchedEffect(isExpanded) {
  if (!isExpanded) {
   panOffsetX = 0f
   panOffsetY = 0f
  }
 }
 val trianglePath = remember { Path() }
 var radarCenterOnScreen by remember { mutableStateOf(Offset(960f, 980f)) }

 val normX = if (rootScreenSize.width > 0) (radarCenterOnScreen.x / rootScreenSize.width.toFloat()).coerceIn(0f, 1f) else 0.5f
 val normY = if (rootScreenSize.height > 0) (radarCenterOnScreen.y / rootScreenSize.height.toFloat()).coerceIn(0f, 1f) else 1.0f
 val intelligentPivotX = when { normX < 0.28f -> 0.0f; normX > 0.72f -> 1.0f; else -> normX }
 val intelligentPivotY = when { normY > 0.65f -> 1.0f; normY < 0.35f -> 0.0f; else -> normY }

 var continuousFrameNanos by remember { mutableLongStateOf(0L) }
 LaunchedEffect(Unit) { while (true) { withFrameNanos { continuousFrameNanos = it } } }
 val innerClipPath = remember { Path() }
 val fluxAgslShader = remember {
  if (Build.VERSION.SDK_INT >= 33) {
   runCatching { RuntimeShader(MINIMAP_FLUX_PLASMA_AGSL) }.getOrNull()
  } else null
 }

 Box(
  modifier = Modifier.zIndex(if (isExpanded || expandScale > 1.02f) 35f else 2f).size(132.dp)
   .onGloballyPositioned { coords ->
    val pos = coords.positionInRoot()
    radarCenterOnScreen = Offset(pos.x + coords.size.width * 0.5f, pos.y + coords.size.height * 0.5f)
   }
   .graphicsLayer {
    scaleX = expandScale; scaleY = expandScale
    transformOrigin = TransformOrigin(intelligentPivotX, intelligentPivotY)
   }
   .wysiwygUnlockOutline(enabled = uiUnlocked, isCircle = true)
   .then(
    if (!uiUnlocked) {
     Modifier.pointerInput(Unit) { detectTapGestures(onTap = { onToggleExpand() }) }
      .pointerInput(Unit) {
       detectDragGestures(
        onDragEnd = { panOffsetX = 0f; panOffsetY = 0f },
        onDragCancel = { panOffsetX = 0f; panOffsetY = 0f },
        onDrag = { change, dragAmount ->
         change.consume()
         panOffsetX = (panOffsetX + dragAmount.x).coerceIn(-24f, 24f)
         panOffsetY = (panOffsetY + dragAmount.y).coerceIn(-24f, 24f)
        },
       )
      }
    } else Modifier
   )
   .testTag("hud_circular_radar"),
 ) {
  Canvas(modifier = Modifier.fillMaxSize()) {
   val tickKeepAlive = frameTick.toLong() + continuousFrameNanos
   if (tickKeepAlive == Long.MIN_VALUE) return@Canvas
   val w = size.width; val h = size.height; val center = Offset(w * 0.5f, h * 0.5f)
   val outerBezelRadius = (min(w, h) * 0.5f - 1.5.dp.toPx()).coerceAtLeast(24f)
   val fluxRingRadius = (outerBezelRadius - 5.2.dp.toPx()).coerceAtLeast(20f)
   val playfieldRadius = (fluxRingRadius - 6.8.dp.toPx()).coerceAtLeast(14f)
   val maxBlipRadius = (playfieldRadius - 2.6.dp.toPx()).coerceAtLeast(8f)
   val radarScale = playfieldRadius / engine.arenaRadius.coerceAtLeast(1000f)

   fun clampToRadar(rawX: Float, rawY: Float): Offset {
    val dx = rawX - center.x; val dy = rawY - center.y; val dist = hypot(dx, dy)
    return if (dist <= maxBlipRadius || dist < 0.001f) Offset(rawX, rawY)
    else Offset(center.x + dx * (maxBlipRadius / dist), center.y + dy * (maxBlipRadius / dist))
   }
   fun worldToRadar(wx: Float, wy: Float): Offset = clampToRadar(
    center.x + panOffsetX + (wx - engine.arenaCenterX) * radarScale,
    center.y + panOffsetY + (wy - engine.arenaCenterY) * radarScale,
   )

   // Outer obsidian-glass bezel (eliminates bezel/tip clipping)
   drawCircle(Color(0xE8030812), outerBezelRadius, center)
   drawCircle(Color(0x4438BDF8), outerBezelRadius, center, style = Stroke(width = 1.0.dp.toPx()))

   // Inner 1:1 circular playfield scope
   drawCircle(if (isExpanded) Color(0xF5040B16) else Color(0xCC040B16), playfieldRadius, center)
   innerClipPath.reset()
   innerClipPath.addOval(Rect(center.x - playfieldRadius, center.y - playfieldRadius, center.x + playfieldRadius, center.y + playfieldRadius))

   val nowMs = System.currentTimeMillis()
   val pulsePhase = ((nowMs % 1200L) / 1200f)
   val goldPulse = 0.5f + 0.5f * sin(((nowMs % 640L) / 640f) * 6.28318f)
   val p = engine.localPlayer

   clipPath(innerClipPath) {
    val arenaCenterOnRadar = Offset(center.x + panOffsetX, center.y + panOffsetY)
    drawCircle(Color(0x2238BDF8), playfieldRadius * 0.55f, arenaCenterOnRadar, style = Stroke(width = 1.0f))
    drawLine(Color(0x1838BDF8), Offset(arenaCenterOnRadar.x - playfieldRadius, arenaCenterOnRadar.y), Offset(arenaCenterOnRadar.x + playfieldRadius, arenaCenterOnRadar.y), 1f)
    drawLine(Color(0x1838BDF8), Offset(arenaCenterOnRadar.x, arenaCenterOnRadar.y - playfieldRadius), Offset(arenaCenterOnRadar.x, arenaCenterOnRadar.y + playfieldRadius), 1f)

    if (engine.hasActiveSafeZone()) {
     val szCenter = Offset(
      center.x + panOffsetX + (4000f - engine.arenaCenterX) * radarScale,
      center.y + panOffsetY + (4000f - engine.arenaCenterY) * radarScale,
     )
     val szRadius = ((engine.safeZone.width() * 0.5f) * RiftCalibrationState.safeZoneScale * radarScale).coerceAtLeast(3.2f)
     drawCircle(Color(0x2600F5D4), szRadius, szCenter)
     drawCircle(Color(0xAA00F5D4), szRadius, szCenter, style = Stroke(width = 1.2f))
    }

    for (wall in engine.walls) {
     val wx = wall.x + wall.w * 0.5f
     val wy = wall.y + wall.h * 0.5f
     val wPt = Offset(
      center.x + panOffsetX + (wx - engine.arenaCenterX) * radarScale,
      center.y + panOffsetY + (wy - engine.arenaCenterY) * radarScale,
     )
     val halfSpan = max(wall.w, wall.h) * 0.5f * radarScale
     drawCircle(Color(0x66475569), halfSpan.coerceIn(1.2f, 4.5f), wPt)
    }

    for (g in engine.greens) drawCircle(Color(0xFF10B981).copy(alpha = 0.55f), 1.1f, worldToRadar(g.x, g.y))

    if (engine.isStasisBreachRulesActive()) {
     drawCircle(Color(0xFF38BDF8), 4.2f, worldToRadar(engine.westBrigReactorX, engine.westBrigReactorY), style = Stroke(1.3f))
     drawCircle(Color(0xFFF43F5E), 4.2f, worldToRadar(engine.eastBrigReactorX, engine.eastBrigReactorY), style = Stroke(1.3f))
    } else if (engine.isRiftControlRulesActive()) {
     val aCol = when (engine.riftAlphaLockedBy) { 0 -> Color(0xFF00F5D4); 1 -> Color(0xFFF43F5E); else -> Color(0xFF38BDF8) }
     val oCol = when (engine.riftOmegaLockedBy) { 0 -> Color(0xFF00F5D4); 1 -> Color(0xFFF43F5E); else -> Color(0xFF38BDF8) }
     drawCircle(aCol, 4.6f, worldToRadar(engine.riftAlphaX, engine.riftAlphaY), style = Stroke(1.5f))
     drawCircle(oCol, 4.6f, worldToRadar(engine.riftOmegaX, engine.riftOmegaY), style = Stroke(1.5f))
    }

    for (anom in engine.anomalies) {
     val aColor = when (anom.type) {
      AnomalyType.PLASMA_RIFT -> Color(0xFFEF4444)
      AnomalyType.REPULSOR_RIFT -> Color.White
      AnomalyType.GRAVITY_RIFT -> Color(0xFFA855F7)
      AnomalyType.PHASE_RIFT_A -> Color(0xFF2DD4BF)
      AnomalyType.PHASE_RIFT_B -> Color(0xFF38BDF8)
     }
     drawCircle(aColor.copy(alpha = 0.82f), 2.6f, worldToRadar(anom.x, anom.y))
    }

    for (bw in engine.brickWalls) {
     val aPt = worldToRadar(if (bw.w >= bw.h) bw.x else bw.x + bw.w * 0.5f, if (bw.w >= bw.h) bw.y + bw.h * 0.5f else bw.y)
     val bPt = worldToRadar(if (bw.w >= bw.h) bw.x + bw.w else bw.x + bw.w * 0.5f, if (bw.w >= bw.h) bw.y + bw.h * 0.5f else bw.y + bw.h)
     drawLine(Color(0xFF00F5D4), aPt, bPt, strokeWidth = 2.2f)
    }

    for (dc in engine.decoys) {
     val isFriendlyDecoy = dc.ownerId == p.id || (p.armadaName.isNotEmpty() && dc.armadaName == p.armadaName) || (p.syndicateTag != "GUEST" && dc.syndicateTag == p.syndicateTag)
     drawCircle(if (isFriendlyDecoy) Color(0xFF38BDF8) else Color(0xFFEF4444), 2.2f, worldToRadar(dc.x, dc.y))
    }

    if (engine.isFluxCoreRulesActive() && engine.fluxCoreCarrierEntityId.isEmpty()) {
     val orbPt = worldToRadar(engine.fluxCoreOrb.x, engine.fluxCoreOrb.y)
     val orbHaloR = 4.4f + 4.8f * goldPulse
     drawCircle(Color(0xFFFACC15).copy(alpha = (0.85f - 0.50f * goldPulse).coerceIn(0.25f, 0.90f)), orbHaloR, orbPt, style = Stroke(1.5f))
     drawCircle(Color(0xFFFFD700), 4.4f, orbPt)
     drawCircle(Color.White, 4.4f, orbPt, style = Stroke(1.1f))
    }

    val radarShips = if (engine.hasJoinedMatch) engine.ships else listOf(p)
    for (s in radarShips) {
     if (s.respawnTimer > 0f) continue
     val isFriendly = engine.areAllied(p, s)
     val isBallCarrier = engine.hasJoinedMatch && engine.isFluxCoreRulesActive() &&
      (s.id == engine.fluxCoreCarrierEntityId || (engine.fluxCoreOrb.carrierShipId != -1 && s.shipId == engine.fluxCoreOrb.carrierShipId))
     if (s.isCloaked && !isFriendly && !s.isLocalPlayer && !isBallCarrier) continue

     val pt = worldToRadar(s.x, s.y)
     val sx = pt.x; val sy = pt.y
     if (isBallCarrier) {
      // 2x pulsing Gold Flux Core carrier beacon
      val carrierHaloR = 6.2f + 5.8f * goldPulse
      drawCircle(Color(0xFFFACC15).copy(alpha = (0.92f - 0.52f * goldPulse).coerceIn(0.30f, 0.95f)), carrierHaloR, pt, style = Stroke(1.8f))
      drawCircle(lerp(Color(0xFFFFD700), Color(0xFFFACC15), goldPulse), 6.2f, pt)
      drawCircle(Color.White, 6.2f, pt, style = Stroke(1.25f))
      continue
     }

     if (s.bounty >= RiftGameEngine.HVT_BOUNTY_THRESHOLD) {
      val pingR = 3.2f + pulsePhase * 6.5f
      drawCircle(Color(0xFFFFD700).copy(alpha = (1f - pulsePhase).coerceIn(0.20f, 0.90f)), pingR, pt, style = Stroke(1.3f))
     }

     if (s.isLocalPlayer) {
      val r = 4.2f; val a = s.angle
      trianglePath.reset()
      trianglePath.moveTo(sx + cos(a) * r, sy + sin(a) * r)
      trianglePath.lineTo(sx + cos(a + 2.4f) * r * 0.85f, sy + sin(a + 2.4f) * r * 0.85f)
      trianglePath.lineTo(sx + cos(a - 2.4f) * r * 0.85f, sy + sin(a - 2.4f) * r * 0.85f)
      trianglePath.close()
      drawPath(trianglePath, if (s.isCloaked) Color(0xFFD8B4FE) else Color.White)
     } else {
      val isSpectatedTarget = engine.isActivelySpectatingTeammates() && s.id == engine.spectatingTargetShipId
      val dotFill = when { isSpectatedTarget -> Color(0xFF00F5D4); isFriendly -> Color(0xFF3B82F6); else -> Color(0xFFEF4444) }
      val dotRadius = when { isSpectatedTarget -> 4.0f; s.isHuman -> 3.0f; else -> 2.2f }
      drawCircle(dotFill, dotRadius, pt)
      if (s.isHuman || isSpectatedTarget) drawCircle(Color.White, dotRadius, pt, style = Stroke(if (isSpectatedTarget) 1.3f else 1.0f))
     }
    }
   }

   drawCircle(Color(0x6638BDF8), playfieldRadius, center, style = Stroke(width = 1.1.dp.toPx()))

   // 3D AGSL Swirling Plasma Flux Ring wrapping the Circular Minimap
   val fluxPct = (p.flux / p.maxFlux.coerceAtLeast(1f)).coerceIn(0f, 1f)
   val critFlash = if (fluxPct < 0.20f || p.isIdleDraining) 0.38f + 0.62f * (0.5f + 0.5f * sin((nowMs % 320L) / 320f * 6.28318f)) else 1.0f
   val ringPrimary = when {
    p.isIdleDraining || fluxPct < 0.20f -> Color(0xFFEF4444).copy(alpha = critFlash)
    fluxPct >= 0.70f -> Color(0xFF00F5D4)
    fluxPct >= 0.35f -> Color(0xFF38BDF8)
    else -> Color(0xFFF59E0B)
   }
   val ringSecondary = when {
    p.isIdleDraining || fluxPct < 0.20f -> Color(0xFFF97316).copy(alpha = critFlash)
    fluxPct >= 0.70f -> Color(0xFF38BDF8)
    fluxPct >= 0.35f -> Color(0xFF10B981)
    else -> Color(0xFFEF4444)
   }

   val trackStrokeW = 5.0.dp.toPx()
   val arcStrokeW = 4.6.dp.toPx()
   val arcTopLeft = Offset(center.x - fluxRingRadius, center.y - fluxRingRadius)
   val arcSize = Size(fluxRingRadius * 2f, fluxRingRadius * 2f)
   drawCircle(Color(0xBF071120), fluxRingRadius, center, style = Stroke(width = trackStrokeW))
   for (tick in 0 until 12) {
    val tickRad = Math.toRadians(-90.0 + tick * 30.0)
    val rIn = fluxRingRadius - trackStrokeW * 0.48f; val rOut = fluxRingRadius + trackStrokeW * 0.48f
    drawLine(Color(0x5538BDF8), Offset(center.x + rIn * cos(tickRad).toFloat(), center.y + rIn * sin(tickRad).toFloat()), Offset(center.x + rOut * cos(tickRad).toFloat(), center.y + rOut * sin(tickRad).toFloat()), 0.9.dp.toPx())
   }

   if (fluxPct > 0.005f) {
    val sweepDeg = 360f * fluxPct
    drawArc(ringPrimary.copy(alpha = 0.24f), -90f, sweepDeg, false, arcTopLeft, arcSize, style = Stroke(width = arcStrokeW * 1.75f, cap = StrokeCap.Round))
    var drewAgslRing = false
    if (Build.VERSION.SDK_INT >= 33 && fluxAgslShader != null) {
     val timeSec = ((nowMs % 60000L) / 1000f)
     drewAgslRing = runCatching {
      fluxAgslShader.setFloatUniform("uRes", w, h)
      fluxAgslShader.setFloatUniform("uTime", timeSec)
      fluxAgslShader.setFloatUniform("uSweepFrac", fluxPct.coerceIn(0f, 1f))
      fluxAgslShader.setFloatUniform("uRIn", fluxRingRadius - arcStrokeW * 0.56f)
      fluxAgslShader.setFloatUniform("uROut", fluxRingRadius + arcStrokeW * 0.56f)
      fluxAgslShader.setFloatUniform("uColA", ringSecondary.red, ringSecondary.green, ringSecondary.blue)
      fluxAgslShader.setFloatUniform("uColB", ringPrimary.red, ringPrimary.green, ringPrimary.blue)
      drawRect(brush = ShaderBrush(fluxAgslShader), topLeft = Offset.Zero, size = Size(w, h), alpha = critFlash)
      true
     }.getOrDefault(false)
    }
    if (!drewAgslRing) {
     drawArc(ringSecondary, -90f, sweepDeg, false, arcTopLeft, arcSize, style = Stroke(width = arcStrokeW, cap = StrokeCap.Round))
     drawArc(ringPrimary, -90f, sweepDeg, false, arcTopLeft, arcSize, style = Stroke(width = arcStrokeW * 0.58f, cap = StrokeCap.Round))
    }

    val tipRad = Math.toRadians(-90.0 + sweepDeg.toDouble())
    val tipCenter = Offset(center.x + fluxRingRadius * cos(tipRad).toFloat(), center.y + fluxRingRadius * sin(tipRad).toFloat())
    drawCircle(ringPrimary.copy(alpha = 0.38f), 4.6.dp.toPx(), tipCenter)
    drawCircle(Color.White, 2.5.dp.toPx(), tipCenter)
    drawCircle(ringPrimary, 2.5.dp.toPx(), tipCenter, style = Stroke(1.0.dp.toPx()))
   }
  }

  val playerFluxPct = (engine.localPlayer.flux / engine.localPlayer.maxFlux.coerceAtLeast(1f)).coerceIn(0f, 1f)
  val badgeColor = when {
   engine.localPlayer.isIdleDraining || playerFluxPct < 0.20f -> Color(0xFFEF4444)
   playerFluxPct >= 0.70f -> Color(0xFF00F5D4)
   playerFluxPct >= 0.35f -> Color(0xFF38BDF8)
   else -> Color(0xFFF59E0B)
  }
  Box(
   modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 1.dp).clip(RoundedCornerShape(6.dp))
    .background(Color(0xF2040B16)).border(1.dp, badgeColor.copy(alpha = 0.88f), RoundedCornerShape(6.dp))
    .padding(horizontal = 5.dp, vertical = 1.dp),
   contentAlignment = Alignment.Center,
  ) {
   Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.5.dp)) {
    FluxEmblem(sizeDp = 9.5.dp)
    Text("${engine.localPlayer.flux.roundToInt()}", color = badgeColor, fontSize = 7.0.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, style = CompactCircleTextStyle, maxLines = 1)
   }
  }
 }
}

@Composable
private fun UnifiedThreeRingCockpitButton(
 title: String,
 sub: String,
 strokeColor: Color,
 fillColor: Color,
 subColor: Color,
 borderWeight: Float = 1.0f,
 sizeDp: Dp = 50.dp,
 modifier: Modifier = Modifier,
) {
 Box(modifier = modifier.size(sizeDp), contentAlignment = Alignment.Center) {
  Canvas(modifier = Modifier.fillMaxSize()) {
   val cx = size.width * 0.5f; val cy = size.height * 0.5f; val r = size.minDimension * 0.47f
   drawCircle(Color(0xEB070F1E), r * 1.05f, Offset(cx, cy))
   drawCircle(strokeColor, r, Offset(cx, cy), style = Stroke(width = (2.0f * borderWeight).dp.toPx()))
   for (tick in 0 until 4) {
    val a = tick * (Math.PI.toFloat() * 0.5f)
    drawLine(strokeColor, Offset(cx + cos(a) * (r * 0.88f), cy + sin(a) * (r * 0.88f)), Offset(cx + cos(a) * (r * 1.03f), cy + sin(a) * (r * 1.03f)), 1.3.dp.toPx(), StrokeCap.Round)
   }
   drawCircle(fillColor, r * 0.86f, Offset(cx, cy))
   drawCircle(Color.White.copy(alpha = 0.55f), r * 0.82f, Offset(cx, cy), style = Stroke(width = 1.0.dp.toPx()))
   val specR = r * 0.74f
   drawArc(Color.White.copy(alpha = 0.45f), 205f, 130f, false, Offset(cx - specR, cy - specR), Size(specR * 2f, specR * 2f), style = Stroke(width = 1.4.dp.toPx(), cap = StrokeCap.Round))
  }
  Text(title, color = Color.White, fontSize = 8.0.sp, fontWeight = FontWeight.Black, style = CompactCircleTextStyle, maxLines = 1, modifier = Modifier.offset(y = (-6).dp))
  Box(
   modifier = Modifier.align(Alignment.BottomCenter).offset(y = (-2).dp).clip(RoundedCornerShape(5.dp))
    .background(Color(0xF0040A16)).border(1.dp, strokeColor, RoundedCornerShape(5.dp)).padding(horizontal = 5.dp, vertical = 1.dp),
   contentAlignment = Alignment.Center,
  ) { Text(sub, color = subColor, fontSize = 6.8.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace, style = CompactCircleTextStyle, maxLines = 1) }
 }
}

@Composable
private fun KillFeedRow(entry: KillFeedEntry, alpha: Float) {
 val cleanKillerTag = if (entry.killerTag in RiftGameEngine.LEGACY_FOUNDRY_TAGS || entry.killerTag.isEmpty()) "GUEST" else entry.killerTag
 val cleanVictimTag = if (entry.victimTag in RiftGameEngine.LEGACY_FOUNDRY_TAGS || entry.victimTag.isEmpty()) "GUEST" else entry.victimTag
 val isHighBountyShutdown = entry.victimBounty > 1000
 val rowDropShadow = if (isHighBountyShutdown) Shadow(Color(0xFFF59E0B).copy(alpha = alpha), Offset(0f, 1f), 8f) else Shadow(Color(0xF2000000).copy(alpha = alpha), Offset(0f, 1.5f), 3.5f)
 val rowTextStyle = TextStyle(lineHeight = 11.5.sp, shadow = rowDropShadow)
 val killerColor = (when { isHighBountyShutdown -> Color(0xFFFEF08A); entry.killerIsLocal -> Color(0xFF38BDF8); entry.killerIsAllied -> Color(0xFF60A5FA); else -> Color(0xFFF87171) }).copy(alpha = alpha)
 val victimColor = (when { isHighBountyShutdown -> Color(0xFFF59E0B); entry.victimIsLocal -> Color(0xFF38BDF8); entry.victimIsAllied -> Color(0xFF60A5FA); else -> Color(0xFFF87171) }).copy(alpha = alpha)
 val bountyColor = (when { isHighBountyShutdown -> Color(0xFFF59E0B); entry.wasHvt -> Color(0xFFFFD700); else -> Color(0xFFFACC15) }).copy(alpha = alpha)

 Row(modifier = Modifier.fillMaxWidth().padding(top = 1.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
  Text("[$cleanKillerTag] ${entry.killerName}", color = killerColor, fontSize = 7.2.sp, fontWeight = FontWeight.ExtraBold, style = rowTextStyle, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
  Text(">>", color = (if (isHighBountyShutdown) Color(0xFFF59E0B) else Color(0xFFCBD5E1)).copy(alpha = 0.85f * alpha), fontSize = 6.8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, style = rowTextStyle)
  Text("[$cleanVictimTag] ${entry.victimName}", color = victimColor, fontSize = 7.2.sp, fontWeight = FontWeight.ExtraBold, style = rowTextStyle, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
  Text(if (isHighBountyShutdown || entry.wasHvt) "HVT ${entry.victimBounty}" else "${entry.victimBounty}", color = bountyColor, fontSize = 7.2.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace, style = rowTextStyle, maxLines = 1)
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
 buffRec: VisualElementRecipe = RiftVisualForgeState.getRecipe(ForgeElementTarget.HUD_BUFF_STRIP),
) {
 val unusedTick = frameTick + buffVersion
 val pctPerStack = RiftCalibrationState.overchargeSurgePctPerStack
 val spdPct = (player.speedOverchargeStacks * pctPerStack * 100f).roundToInt()
 val thrPct = (player.thrustOverchargeStacks * pctPerStack * 100f).roundToInt()
 val turnPct = player.turnOverchargeStacks * 12
 val maxFluxPct = player.maxFluxOverchargeStacks * 10
 val rchPct = (player.rechargeOverchargeStacks * pctPerStack * 100f).roundToInt()
 val dockBg = if (buffRec.isFactoryDefault) Color(0xD9080E18) else Color(buffRec.primaryColorInt).copy(alpha = buffRec.surfaceOpacity.coerceIn(0.25f, 0.98f))
 val dockBorder = if (buffRec.isFactoryDefault) Color(0x4D38BDF8) else Color(buffRec.trimColorInt)

 val slots = listOf(
  PermanentUpgradeSlot(9, "BNC", when { player.hasBouncingBulletsUpgrade -> "ON"; player.spec.primaryBounces > 0 -> "NAT"; else -> "OFF" }, player.hasBouncingBulletsUpgrade || player.spec.primaryBounces > 0, Color(0xFFA855F7)),
  PermanentUpgradeSlot(-10, "SPD", "+$spdPct%", player.speedOverchargeStacks > 0, Color(0xFF38BDF8)),
  PermanentUpgradeSlot(-11, "THR", "+$thrPct%", player.thrustOverchargeStacks > 0, Color(0xFFF59E0B)),
  PermanentUpgradeSlot(-13, "TURN", "+$turnPct%", player.turnOverchargeStacks > 0, Color(0xFFEC4899)),
  PermanentUpgradeSlot(-14, "MAX", "+$maxFluxPct%", player.maxFluxOverchargeStacks > 0, Color(0xFF00F5D4)),
  PermanentUpgradeSlot(-12, "RCH", "+$rchPct%", player.rechargeOverchargeStacks > 0, Color(0xFF10B981)),
 )

 Row(
  modifier = Modifier.wrapContentSize().clip(RoundedCornerShape(9.dp)).background(dockBg)
   .border((0.85f * buffRec.borderWeight).dp, dockBorder, RoundedCornerShape(9.dp))
   .wysiwygUnlockOutline(enabled = uiUnlocked, cornerRadius = 9.dp)
   .padding(horizontal = (6.0f * textScale).dp, vertical = (2.6f * textScale).dp),
  verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy((5.5f * textScale).dp),
 ) {
  slots.forEach { slot -> PermanentUpgradeBadge(slot = slot, textScale = textScale) }
 }
}

@Composable
private fun PermanentUpgradeBadge(slot: PermanentUpgradeSlot, textScale: Float) {
 val bmp = remember(slot.iconType) { RiftSpriteBaker.getBuffIconBitmap(slot.iconType).asImageBitmap() }
 val labelColor = if (slot.isActive) slot.accentColor else Color(0xFF64748B)
 Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy((2.5f * textScale).dp)) {
  Box(modifier = Modifier.size((14.0f * textScale).dp), contentAlignment = Alignment.Center) {
   Canvas(modifier = Modifier.fillMaxSize()) {
    val strokeWidthPx = (if (slot.isActive) 1.1.dp else 0.7.dp).toPx()
    val radius = (size.minDimension - strokeWidthPx) / 2f
    if (slot.isActive) {
     drawCircle(slot.accentColor.copy(alpha = 0.25f), radius, style = Stroke(strokeWidthPx * 1.4f))
     drawCircle(slot.accentColor, radius, style = Stroke(strokeWidthPx))
    } else drawCircle(Color(0x3D475569), radius, style = Stroke(strokeWidthPx))
   }
   if (slot.iconType == -14) FluxEmblem(sizeDp = (9.5f * textScale).dp)
   else {
    Canvas(modifier = Modifier.size((10.0f * textScale).dp)) {
     drawImage(bmp, IntOffset.Zero, IntSize(size.width.toInt(), size.height.toInt()), alpha = if (slot.isActive) 1.0f else 0.28f)
    }
   }
  }
  Text("${slot.label} ${slot.valueText}", color = labelColor, fontSize = (6.8f * textScale).sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace, style = WingTelemetryTextStyle, maxLines = 1)
 }
}
