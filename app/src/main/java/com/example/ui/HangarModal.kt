package com.example.ui

import android.graphics.RuntimeShader
import android.os.Build
import android.view.KeyEvent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.RiftBillingManager
import com.example.data.UgcModeration
import com.example.engine.CustomShipLoadoutSlot
import com.example.engine.RankedGameMode
import com.example.engine.RiftCalibrationState
import com.example.engine.RiftCodex
import com.example.engine.RiftProgressionCatalog
import com.example.engine.RiftSpriteBaker
import com.example.engine.ShipSpec
import java.util.Locale
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private val HangarTabletAbyss = Color(0xFF060D1A)
private val HangarTabletPanel = Color(0xFF081224)
private val HangarTabletElevated = Color(0xFF0C1B33)
private val HangarElectricCyan = Color(0xFF38BDF8)
private val HangarNexusTeal = Color(0xFF00F5D4)
private val HangarSolarGold = Color(0xFFFACC15)
private val HangarDeltaPositive = Color(0xFF10B981)
private val HangarDeltaNegative = Color(0xFFEF4444)
private val Mono = FontFamily.Monospace
private val Sans = FontFamily.SansSerif

private fun baselineAnchoredStatFraction(pts: Int): Float =
  0.10f + (pts.coerceIn(0, 30) / 30f) * 0.90f

private fun cleanShipDisplayTitle(ship: ShipSpec): String =
  if (ship.id == 10 && ship.name.equals("Rift Frame", ignoreCase = true)) "RIFT FRAME"
  else ship.name.replace(Regex("^[0-9]+-"), "").uppercase(Locale.US)

private fun DrawScope.drawCornerTicks(
  tLen: Float,
  sW: Float,
  leftCol: Color,
  rightCol: Color = leftCol,
) {
  drawLine(leftCol, Offset(0f, 0f), Offset(tLen, 0f), sW)
  drawLine(leftCol, Offset(0f, 0f), Offset(0f, tLen), sW)
  drawLine(leftCol, Offset(0f, size.height), Offset(tLen, size.height), sW)
  drawLine(leftCol, Offset(0f, size.height - tLen), Offset(0f, size.height), sW)
  drawLine(rightCol, Offset(size.width - tLen, 0f), Offset(size.width, 0f), sW)
  drawLine(rightCol, Offset(size.width, 0f), Offset(size.width, tLen), sW)
  drawLine(rightCol, Offset(size.width - tLen, size.height), Offset(size.width, size.height), sW)
  drawLine(rightCol, Offset(size.width, size.height - tLen), Offset(size.width, size.height), sW)
}

private fun DrawScope.drawMiniSwirlingRiftGlyph(accent: Color) {
  val cx = size.width * 0.5f
  val cy = size.height * 0.5f
  val r = size.minDimension * 0.44f
  drawCircle(
    brush = Brush.radialGradient(listOf(Color.White, accent, Color.Transparent)),
    radius = r,
    center = Offset(cx, cy),
  )
  drawArc(
    color = accent,
    startAngle = 15f,
    sweepAngle = 145f,
    useCenter = false,
    topLeft = Offset(cx - r, cy - r),
    size = Size(r * 2f, r * 2f),
    style = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round),
  )
  drawArc(
    color = Color.White.copy(alpha = 0.85f),
    startAngle = 195f,
    sweepAngle = 145f,
    useCenter = false,
    topLeft = Offset(cx - r * 0.78f, cy - r * 0.78f),
    size = Size(r * 1.56f, r * 1.56f),
    style = Stroke(width = 1.3.dp.toPx(), cap = StrokeCap.Round),
  )
}

private const val HANGAR_NEXUS_SINGULARITY_AGSL =
  """
  uniform float2 uRes;
  uniform float uTime;

  half4 main(float2 fragCoord) {
    float2 uv = (fragCoord - 0.5 * uRes) / max(min(uRes.x, uRes.y) * 0.5, 1.0);
    float r = length(uv);
    if (r > 1.0) return half4(0.0);
    float a = atan(uv.y, uv.x);
    float swirl = a * 5.0 - r * 9.5 - uTime * 2.6;
    float arm = 0.5 + 0.5 * sin(swirl);
    float counter = 0.5 + 0.5 * cos(a * 3.0 + r * 7.0 + uTime * 1.9);
    float ring = exp(-26.0 * (r - 0.56) * (r - 0.56));
    float core = exp(-9.5 * r * r);
    half3 cyan = half3(0.0, 0.96, 0.83);
    half3 violet = half3(0.66, 0.33, 0.97);
    half3 magenta = half3(0.93, 0.28, 0.60);
    half3 gold = half3(0.98, 0.80, 0.08);
    half3 col = mix(cyan, violet, half(arm));
    col = mix(col, magenta, half(counter * 0.55));
    col += gold * half(ring * 0.85) + half3(1.0) * half(core * 0.72);
    float fade = smoothstep(1.0, 0.12, r);
    half alpha = half(clamp(fade * (0.42 + 0.44 * arm + 0.45 * ring + 0.55 * core), 0.0, 0.96));
    return half4(col * alpha, alpha);
  }
  """

@Composable
private fun SwirlingNexusLockBackdrop(modifier: Modifier = Modifier, phase: Float) {
  val agslShader = remember {
    if (Build.VERSION.SDK_INT >= 33) {
      runCatching { RuntimeShader(HANGAR_NEXUS_SINGULARITY_AGSL) }.getOrNull()
    } else null
  }
  Canvas(modifier = modifier) {
    val cx = size.width * 0.5f
    val cy = size.height * 0.5f
    val r = size.minDimension * 0.49f
    if (r <= 1f) return@Canvas
    var drewShader = false
    if (Build.VERSION.SDK_INT >= 33 && agslShader != null) {
      drewShader =
        runCatching {
            agslShader.setFloatUniform("uRes", size.width, size.height)
            agslShader.setFloatUniform("uTime", phase)
            drawCircle(brush = ShaderBrush(agslShader), radius = r, center = Offset(cx, cy))
            true
          }
          .getOrDefault(false)
    }
    if (!drewShader) {
      drawCircle(
        brush =
          Brush.radialGradient(
            colors =
              listOf(
                Color.White.copy(alpha = 0.82f),
                HangarNexusTeal.copy(alpha = 0.68f),
                Color(0xFFA855F7).copy(alpha = 0.52f),
                Color(0xFFEC4899).copy(alpha = 0.25f),
                Color.Transparent,
              ),
            center = Offset(cx, cy),
            radius = r,
          ),
        radius = r,
        center = Offset(cx, cy),
      )
      val degBase = Math.toDegrees(phase.toDouble()).toFloat()
      val armColors =
        listOf(
          HangarNexusTeal,
          Color(0xFFA855F7),
          Color(0xFFEC4899),
          HangarSolarGold,
          HangarElectricCyan,
        )
      for (arm in 0 until 5) {
        val startDeg = degBase * 1.4f + arm * 72f
        val armR = r * (0.62f + (arm % 2) * 0.18f)
        drawArc(
          color = armColors[arm].copy(alpha = 0.85f),
          startAngle = startDeg,
          sweepAngle = 52f,
          useCenter = false,
          topLeft = Offset(cx - armR, cy - armR),
          size = Size(armR * 2f, armR * 2f),
          style = Stroke(width = (r * 0.11f).coerceAtLeast(1.4f), cap = StrokeCap.Round),
        )
      }
    }
  }
}

private fun DrawScope.drawBestShipResonanceCrownHalo(phase: Float) {
  val cx = size.width * 0.5f
  val cy = size.height * 0.5f
  val outerR = size.minDimension * 0.48f
  val innerR = outerR * 0.82f
  if (outerR <= 2f) return
  val deg = Math.toDegrees(phase.toDouble()).toFloat()
  val strokeOuter = (outerR * 0.085f).coerceAtLeast(1.2f)
  val strokeInner = (outerR * 0.07f).coerceAtLeast(1.0f)
  for (seg in 0 until 3) {
    drawArc(
      color = Color(0xFFFFD700).copy(alpha = 0.94f),
      startAngle = deg + seg * 120f,
      sweepAngle = 76f,
      useCenter = false,
      topLeft = Offset(cx - outerR, cy - outerR),
      size = Size(outerR * 2f, outerR * 2f),
      style = Stroke(width = strokeOuter, cap = StrokeCap.Round),
    )
    drawArc(
      color = Color(0xFFF59E0B).copy(alpha = 0.88f),
      startAngle = -deg * 1.25f + 30f + seg * 120f,
      sweepAngle = 64f,
      useCenter = false,
      topLeft = Offset(cx - innerR, cy - innerR),
      size = Size(innerR * 2f, innerR * 2f),
      style = Stroke(width = strokeInner, cap = StrokeCap.Round),
    )
  }
}

private fun DrawScope.drawDistinctHullCardBorders(
  cornerRadiusPx: Float,
  isBestShip: Boolean,
  isLocked: Boolean,
  isSelected: Boolean,
  isEquipped: Boolean,
  phase: Float,
) {
  val w = size.width
  val h = size.height
  if (w <= 8f || h <= 8f) return

  // 1. BEST SHIP BORDER: Solar-Gold Harmonic Double-Bezel + 4 Golden Crown Corner Finials
  if (isBestShip) {
    val goldBright = Color(0xFFFFD700)
    val goldDeep = Color(0xFFF59E0B)
    val outerPad = 1.0.dp.toPx()
    val innerPad = 3.6.dp.toPx()
    drawRoundRect(
      color = goldBright,
      topLeft = Offset(outerPad, outerPad),
      size = Size(w - outerPad * 2f, h - outerPad * 2f),
      cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadiusPx, cornerRadiusPx),
      style = Stroke(width = 1.7.dp.toPx()),
    )
    drawRoundRect(
      color = goldDeep.copy(alpha = 0.72f),
      topLeft = Offset(innerPad, innerPad),
      size = Size((w - innerPad * 2f).coerceAtLeast(2f), (h - innerPad * 2f).coerceAtLeast(2f)),
      cornerRadius = androidx.compose.ui.geometry.CornerRadius((cornerRadiusPx - 2.dp.toPx()).coerceAtLeast(2f)),
      style = Stroke(width = 0.9.dp.toPx()),
    )
    val finialArm = 8.dp.toPx()
    val sw = 2.0.dp.toPx()
    // 4 Crown Corner Finials
    drawLine(goldBright, Offset(outerPad, outerPad + finialArm), Offset(outerPad, outerPad), sw, StrokeCap.Round)
    drawLine(goldBright, Offset(outerPad, outerPad), Offset(outerPad + finialArm, outerPad), sw, StrokeCap.Round)
    drawLine(goldBright, Offset(w - outerPad - finialArm, outerPad), Offset(w - outerPad, outerPad), sw, StrokeCap.Round)
    drawLine(goldBright, Offset(w - outerPad, outerPad), Offset(w - outerPad, outerPad + finialArm), sw, StrokeCap.Round)
    drawLine(goldBright, Offset(outerPad, h - outerPad - finialArm), Offset(outerPad, h - outerPad), sw, StrokeCap.Round)
    drawLine(goldBright, Offset(outerPad, h - outerPad), Offset(outerPad + finialArm, h - outerPad), sw, StrokeCap.Round)
    drawLine(goldBright, Offset(w - outerPad - finialArm, h - outerPad), Offset(w - outerPad, h - outerPad), sw, StrokeCap.Round)
    drawLine(goldBright, Offset(w - outerPad, h - outerPad - finialArm), Offset(w - outerPad, h - outerPad), sw, StrokeCap.Round)
  }

  // 2. SELECTED / LOCKED / EQUIPPED BORDER: Cyan/Teal Swirling NEXUS Conduit Rail (inset if also Best Ship)
  if (isLocked || isSelected || isEquipped) {
    val inset = if (isBestShip) 5.2.dp.toPx() else 1.1.dp.toPx()
    val conduitColor = if (isLocked) HangarNexusTeal else HangarElectricCyan
    val dash =
      androidx.compose.ui.graphics.PathEffect.dashPathEffect(
        floatArrayOf(12.dp.toPx(), 4.5.dp.toPx()),
        phase * 16f,
      )
    val cr = (cornerRadiusPx - inset * 0.65f).coerceAtLeast(3f)
    drawRoundRect(
      color = conduitColor,
      topLeft = Offset(inset, inset),
      size = Size((w - inset * 2f).coerceAtLeast(2f), (h - inset * 2f).coerceAtLeast(2f)),
      cornerRadius = androidx.compose.ui.geometry.CornerRadius(cr, cr),
      style = Stroke(width = if (isLocked) 1.9.dp.toPx() else 1.3.dp.toPx(), pathEffect = if (isLocked) dash else null),
    )
    // Mid-edge Cyan NEXUS Conduit LockChevrons so Selected/Locked never resembles Best Ship corners
    val midX = w * 0.5f
    val midY = h * 0.5f
    val notch = 6.dp.toPx()
    drawLine(conduitColor, Offset(midX - notch, inset), Offset(midX + notch, inset), 2.4.dp.toPx(), StrokeCap.Round)
    drawLine(conduitColor, Offset(midX - notch, h - inset), Offset(midX + notch, h - inset), 2.4.dp.toPx(), StrokeCap.Round)
    drawLine(conduitColor, Offset(inset, midY - notch), Offset(inset, midY + notch), 2.4.dp.toPx(), StrokeCap.Round)
    drawLine(conduitColor, Offset(w - inset, midY - notch), Offset(w - inset, midY + notch), 2.4.dp.toPx(), StrokeCap.Round)
  }
}

@Composable
private fun ShipResonanceGoldBadge(
  shipResonance: Int,
  isBestShip: Boolean = false,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier =
      modifier
        .clip(RoundedCornerShape(4.dp))
        .background(Color(0xFF141004))
        .border(
          width = if (isBestShip) 1.1.dp else 0.85.dp,
          color = if (isBestShip) Color(0xFFFFD700) else Color(0xFFF59E0B).copy(alpha = 0.78f),
          shape = RoundedCornerShape(4.dp),
        )
        .padding(horizontal = 4.5.dp, vertical = 1.5.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(2.5.dp),
  ) {
    Text(
      text = "$shipResonance RES",
      color = Color(0xFFFFD700),
      fontSize = 8.4.sp,
      fontWeight = FontWeight.Black,
      fontFamily = Mono,
      maxLines = 1,
    )
  }
}

@Composable
fun HangarModal(
  initialCallsign: String,
  syndicateTag: String = "GUEST",
  syndicateName: String = "Independent Pilot",
  selectedShipId: Int,
  bestShipId: Int = 1,
  selectedMode: String,
  pilotLevel: Int = 1,
  xpProgressFraction: Float = 0f,
  customizationPointsTotal: Int = 100,
  spentCustomizationPoints: Int = 0,
  riftShards: Long = 0L,
  unlockFullNinthShipOverride: Boolean = false,
  activeCustomSlot: CustomShipLoadoutSlot = RiftCodex.activeCustomShipSlot,
  slot9BUnlocked: Boolean = false,
  slot9CUnlocked: Boolean = false,
  slot9DUnlocked: Boolean = false,
  slot9EUnlocked: Boolean = false,
  onOpenRiftShardsStore: () -> Unit = {},
  onOpenSyndicatesModal: () -> Unit = {},
  onSaveCustomSlot: (CustomShipLoadoutSlot) -> Unit = {},
  onSwitchCustomSlot: (String) -> Unit = {},
  onUnlockCustomSlot: (String) -> Boolean = { false },
  onRespecCustomSlot: () -> Boolean = { false },
  onForgeAiHullDesign: (String, String) -> Pair<Boolean, String> = { _, _ -> Pair(false, "") },
  onToggleDevLevel20Or100: () -> Unit = {},
  isNexusStreaming: Boolean = false,
  onOpenNexus: () -> Unit = {},
  onOpenShipForge: () -> Unit = {},
  onDeployPilot: (callsign: String, shipId: Int, mode: String, asGuest: Boolean) -> Unit,
  onCloseModal: () -> Unit = {},
  liveResonance: Int = 0,
  peakResonance: Int = 0,
  stagedTargetTitle: String? = null,
  stagedHullLockNotice: Boolean = false,
  onCancelStagedModeToGate: (() -> Unit)? = null,
  shipResonances: Map<Int, Int> = emptyMap(),
) {
  val isCompactPhone = LocalConfiguration.current.screenHeightDp < 480
  var callsignInput by remember(initialCallsign) { mutableStateOf(initialCallsign) }
  var currentShipId by remember(selectedShipId) { mutableIntStateOf(selectedShipId.coerceIn(1, 10)) }
  var lockedShipId by remember(selectedShipId) { mutableStateOf<Int?>(selectedShipId.coerceIn(1, 10)) }
  val clampedBestShipId = bestShipId.coerceIn(1, 10)
  val currentMode = selectedMode.ifBlank { "SECTOR_CONVERGENCE" }
  var showPilotIdentityBar by remember { mutableStateOf(false) }

  val nexusTransition = rememberInfiniteTransition(label = "hangar_nexus_vortex")
  val nexusSpinPhase by
    nexusTransition.animateFloat(
      initialValue = 0f,
      targetValue = 6.2831855f,
      animationSpec = infiniteRepeatable(animation = tween(3600, easing = LinearEasing), repeatMode = RepeatMode.Restart),
      label = "hangar_nexus_phase",
    )

  fun handleHullTap(targetId: Int) {
    val safeId = targetId.coerceIn(1, 10)
    currentShipId = safeId
    lockedShipId = if (lockedShipId == safeId) null else safeId
  }

  val spriteVersion = RiftSpriteBaker.customShip9Version
  val allTenShips = remember(activeCustomSlot, spriteVersion) { RiftCodex.getAllNineShips() }

  var showFullNinthShipForgeModal by remember { mutableStateOf(false) }
  var forgeStatusBanner by remember { mutableStateOf<String?>(null) }
  val hangarFocusRequester = remember { FocusRequester() }

  LaunchedEffect(showFullNinthShipForgeModal) {
    if (!showPilotIdentityBar) runCatching { hangarFocusRequester.requestFocus() }
  }

  BackHandler {
    when {
      showFullNinthShipForgeModal -> showFullNinthShipForgeModal = false
      onCancelStagedModeToGate != null -> onCancelStagedModeToGate()
      else -> onCloseModal()
    }
  }

  val inspectedShip = remember(currentShipId, allTenShips) {
    allTenShips.find { it.id == currentShipId } ?: allTenShips.first()
  }
  val equippedShip = remember(selectedShipId, allTenShips) {
    allTenShips.find { it.id == selectedShipId } ?: allTenShips.first()
  }
  val lockedShipSpec = remember(lockedShipId, allTenShips) {
    lockedShipId?.let { id -> allTenShips.find { it.id == id } }
  }
  val effectivePointBudget = 100
  val pointsAllocatedInSlot = activeCustomSlot.totalAllocatedPoints()
  val pointsRemainingInSlot = (effectivePointBudget - pointsAllocatedInSlot).coerceAtLeast(0)
  val effectivePeakRes = maxOf(liveResonance, peakResonance)
  val startBounty = RiftProgressionCatalog.computeStartingBountyForResonance(liveResonance)
  val isTesterInfiniteShards = RiftBillingManager.TESTER_INFINITE_SHARDS_ACTIVE

  Column(
    modifier =
      Modifier.fillMaxSize()
        .background(HangarTabletAbyss)
        .focusRequester(hangarFocusRequester)
        .focusable()
        .onPreviewKeyEvent { keyEvent ->
          val native = keyEvent.nativeKeyEvent
          if (native.action == KeyEvent.ACTION_DOWN && native.repeatCount == 0) {
            RiftCalibrationState.gamepadConnected = true
            when (native.keyCode) {
              KeyEvent.KEYCODE_BUTTON_START, KeyEvent.KEYCODE_BACK -> {
                when {
                  showFullNinthShipForgeModal -> showFullNinthShipForgeModal = false
                  onCancelStagedModeToGate != null -> onCancelStagedModeToGate()
                  else -> onCloseModal()
                }
                true
              }
              KeyEvent.KEYCODE_DPAD_LEFT ->
                if (!showFullNinthShipForgeModal && !showPilotIdentityBar) {
                  val nextId = if (currentShipId > 1) currentShipId - 1 else 10
                  currentShipId = nextId
                  lockedShipId = nextId
                  true
                } else false
              KeyEvent.KEYCODE_DPAD_RIGHT ->
                if (!showFullNinthShipForgeModal && !showPilotIdentityBar) {
                  val nextId = if (currentShipId < 10) currentShipId + 1 else 1
                  currentShipId = nextId
                  lockedShipId = nextId
                  true
                } else false
              KeyEvent.KEYCODE_DPAD_UP ->
                if (!showFullNinthShipForgeModal && !showPilotIdentityBar) {
                  val nextId =
                    when {
                      currentShipId == 10 -> 8
                      currentShipId in 4..9 -> currentShipId - 3
                      else -> 10
                    }
                  currentShipId = nextId
                  lockedShipId = nextId
                  true
                } else false
              KeyEvent.KEYCODE_DPAD_DOWN ->
                if (!showFullNinthShipForgeModal && !showPilotIdentityBar) {
                  val nextId =
                    when {
                      currentShipId in 1..6 -> currentShipId + 3
                      currentShipId in 7..9 -> 10
                      else -> 2
                    }
                  currentShipId = nextId
                  lockedShipId = nextId
                  true
                } else false
              KeyEvent.KEYCODE_BUTTON_A, KeyEvent.KEYCODE_DPAD_CENTER ->
                if (!showFullNinthShipForgeModal && !showPilotIdentityBar) {
                  val deployId = lockedShipId
                  if (deployId != null) {
                    onDeployPilot(
                      UgcModeration.sanitizeIdentifier(callsignInput.trim(), "COMMANDER"),
                      deployId,
                      currentMode,
                      false,
                    )
                  } else {
                    lockedShipId = currentShipId
                  }
                  true
                } else false
              KeyEvent.KEYCODE_BUTTON_X ->
                if (!showFullNinthShipForgeModal && currentShipId == 10) {
                  showFullNinthShipForgeModal = true
                  true
                } else false
              else -> false
            }
          } else false
        }
        .padding(if (isCompactPhone) 4.dp else 8.dp)
        .testTag("hangar_modal")
  ) {
    // Top Sci-Fi Command Tablet Header Bar
    Row(
      modifier =
        Modifier.fillMaxWidth()
          .height(if (isCompactPhone) 32.dp else 36.dp)
          .background(HangarTabletPanel, RoundedCornerShape(8.dp))
          .border(1.dp, HangarElectricCyan.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
          .padding(horizontal = 8.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Text(
          text =
            when {
              stagedTargetTitle != null && isCompactPhone -> "HANGAR // LAUNCH STAGING"
              stagedTargetTitle != null -> "HANGAR DECK // RIFT LAUNCH STAGING"
              isCompactPhone -> "HANGAR DECK // 10 HULLS"
              else -> "HANGAR DECK // 10-HULL PARITY MATRIX"
            },
          color = HangarNexusTeal,
          fontSize = if (isCompactPhone) 12.5.sp else 14.sp,
          fontWeight = FontWeight.Black,
          fontFamily = Mono,
          maxLines = 1,
        )
        Box(
          modifier =
            Modifier.testTag("btn_toggle_pilot_identity")
              .clip(RoundedCornerShape(7.dp))
              .background(if (showPilotIdentityBar) HangarTabletElevated else HangarTabletAbyss)
              .border(1.dp, HangarElectricCyan, RoundedCornerShape(7.dp))
              .clickable { showPilotIdentityBar = !showPilotIdentityBar }
              .padding(horizontal = 8.dp, vertical = 3.5.dp)
        ) {
          Text(
            text =
              if (showPilotIdentityBar) "ID: [$syndicateTag] $callsignInput [^]"
              else "ID: [$syndicateTag] $callsignInput [v]",
            color = HangarElectricCyan,
            fontSize = if (isCompactPhone) 9.5.sp else 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = Mono,
            maxLines = 1,
          )
        }
      }

      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Row(
          modifier =
            Modifier.background(HangarTabletAbyss, RoundedCornerShape(7.dp))
              .border(1.dp, HangarNexusTeal, RoundedCornerShape(7.dp))
              .padding(horizontal = 8.dp, vertical = 3.5.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          ResonanceCrestIcon(sizeDp = 15.dp)
          Text(
            text = "$liveResonance RES",
            color = Color(0xFFFFD700),
            fontSize = if (isCompactPhone) 9.5.sp else 10.sp,
            fontWeight = FontWeight.Black,
            fontFamily = Mono,
          )
          Text(
            text = "$pointsRemainingInSlot/$effectivePointBudget PTS",
            color = Color(0xFF22C55E),
            fontSize = if (isCompactPhone) 9.5.sp else 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = Mono,
          )
        }

        Box(
          modifier =
            Modifier.testTag("btn_hangar_open_nexus_forge")
              .clip(RoundedCornerShape(7.dp))
              .background(HangarTabletAbyss)
              .border(1.dp, Color(0xFFC084FC), RoundedCornerShape(7.dp))
              .clickable { onOpenShipForge() }
              .padding(horizontal = 8.dp, vertical = 4.dp),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = if (isCompactPhone) "STAR FORGE" else "STAR FORGE & COSMETICS",
            color = Color(0xFFE9D5FF),
            fontSize = if (isCompactPhone) 9.5.sp else 10.sp,
            fontWeight = FontWeight.Black,
            fontFamily = Mono,
          )
        }

        Row(
          modifier =
            Modifier.testTag("btn_hangar_get_rift_shards")
              .clip(RoundedCornerShape(7.dp))
              .background(HangarTabletAbyss)
              .border(1.dp, HangarSolarGold, RoundedCornerShape(7.dp))
              .clickable { onOpenRiftShardsStore() }
              .padding(horizontal = 8.dp, vertical = 4.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
          RiftShardGemIcon(sizeDp = 15.dp)
          val shardBadgeCopy =
            when {
              isTesterInfiniteShards && isCompactPhone -> "999,999"
              isTesterInfiniteShards -> "999,999 [CONDUIT]"
              isCompactPhone -> "$riftShards"
              else -> "$riftShards [CONDUIT]"
            }
          Text(
            text = shardBadgeCopy,
            color = HangarSolarGold,
            fontSize = if (isCompactPhone) 9.5.sp else 10.sp,
            fontWeight = FontWeight.Black,
            fontFamily = Mono,
          )
        }
      }
    }

    // Launch Staging Strip when routed from CONVERGENCE GATE or #strike-lfg Room Code
    if (stagedTargetTitle != null) {
      Spacer(modifier = Modifier.height(4.dp))
      Row(
        modifier =
          Modifier.fillMaxWidth()
            .height(if (isCompactPhone) 30.dp else 32.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
              Brush.horizontalGradient(
                listOf(HangarTabletAbyss, HangarTabletPanel, HangarTabletElevated)
              )
            )
            .border(1.dp, HangarNexusTeal, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp)
            .testTag("hangar_staged_rift_banner"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.weight(1f),
        ) {
          Canvas(modifier = Modifier.size(15.dp)) {
            drawMiniSwirlingRiftGlyph(HangarNexusTeal)
          }
          Text(
            text = "DEPLOYING TO: ${stagedTargetTitle.uppercase(Locale.US)}",
            color = HangarNexusTeal,
            fontSize = if (isCompactPhone) 9.8.sp else 10.8.sp,
            fontWeight = FontWeight.Black,
            fontFamily = Mono,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          if (stagedHullLockNotice) {
            Box(
              modifier =
                Modifier.background(HangarTabletAbyss, RoundedCornerShape(5.dp))
                  .border(1.dp, HangarSolarGold, RoundedCornerShape(5.dp))
                  .padding(horizontal = 7.dp, vertical = 2.dp)
            ) {
              Text(
                text = "HULL LOCKS FOR MATCH",
                color = HangarSolarGold,
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Black,
                fontFamily = Mono,
                maxLines = 1,
              )
            }
          }
        }

        if (onCancelStagedModeToGate != null) {
          Box(
            modifier =
              Modifier.testTag("btn_hangar_change_staged_rift")
                .clip(RoundedCornerShape(6.dp))
                .background(HangarTabletAbyss)
                .border(1.dp, HangarElectricCyan, RoundedCornerShape(6.dp))
                .clickable { onCancelStagedModeToGate() }
                .padding(horizontal = 8.dp, vertical = 3.dp),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text = "< CHANGE RIFT",
              color = HangarElectricCyan,
              fontSize = 9.5.sp,
              fontWeight = FontWeight.Black,
              fontFamily = Mono,
              maxLines = 1,
            )
          }
        }
      }
    }

    if (showPilotIdentityBar) {
      Spacer(modifier = Modifier.height(4.dp))
      val activeModeTitle =
        stagedTargetTitle?.uppercase(Locale.US)
          ?: RankedGameMode.fromId(currentMode).title.uppercase(Locale.US)
      Row(
        modifier =
          Modifier.fillMaxWidth()
            .background(HangarTabletPanel, RoundedCornerShape(8.dp))
            .border(1.dp, HangarElectricCyan.copy(alpha = 0.55f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Text(
          text = "CALLSIGN:",
          color = Color(0xFF94A3B8),
          fontSize = 9.5.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = Mono,
        )
        Box(
          modifier =
            Modifier.width(142.dp)
              .height(if (isCompactPhone) 34.dp else 38.dp)
              .background(HangarTabletAbyss, RoundedCornerShape(8.dp))
              .border(1.dp, HangarNexusTeal, RoundedCornerShape(8.dp))
              .padding(horizontal = 10.dp),
          contentAlignment = Alignment.CenterStart,
        ) {
          BasicTextField(
            value = callsignInput,
            onValueChange = { if (it.length <= 14) callsignInput = it.uppercase(Locale.US) },
            singleLine = true,
            textStyle =
              TextStyle(
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = Mono,
              ),
            cursorBrush = SolidColor(HangarNexusTeal),
            modifier = Modifier.fillMaxWidth().testTag("input_pilot_callsign"),
          )
        }
        Box(
          modifier =
            Modifier.testTag("btn_hangar_manage_syndicate")
              .clip(RoundedCornerShape(8.dp))
              .background(HangarTabletAbyss)
              .border(1.dp, Color(0xFFC084FC), RoundedCornerShape(8.dp))
              .clickable { onOpenSyndicatesModal() }
              .padding(horizontal = 9.dp, vertical = 5.dp)
        ) {
          Text(
            text = "[$syndicateTag] SYNDICATE CITADEL",
            color = Color(0xFFE9D5FF),
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Black,
            fontFamily = Mono,
          )
        }
        Text(
          text = "TARGET RIFT:",
          color = Color(0xFF94A3B8),
          fontSize = 9.5.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = Mono,
        )
        Box(
          modifier =
            Modifier.testTag("mode_chip_FFA")
              .clip(RoundedCornerShape(8.dp))
              .background(HangarTabletAbyss)
              .border(1.dp, HangarNexusTeal, RoundedCornerShape(8.dp))
              .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
          Text(
            text = activeModeTitle,
            color = HangarNexusTeal,
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Black,
            fontFamily = Mono,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(if (isCompactPhone) 4.dp else 6.dp))

    if (showFullNinthShipForgeModal) {
      NinthShipModularForgePanel(
        spec = allTenShips.first { it.id == 10 },
        activeSlot = activeCustomSlot,
        slot9BUnlocked = slot9BUnlocked || effectivePeakRes >= 300,
        slot9CUnlocked = slot9CUnlocked || effectivePeakRes >= 650,
        slot9DUnlocked = slot9DUnlocked || effectivePeakRes >= 1100,
        slot9EUnlocked = slot9EUnlocked || effectivePeakRes >= 1600,
        effectivePointBudget = effectivePointBudget,
        pointsRemaining = pointsRemainingInSlot,
        isTesterInfiniteShards = isTesterInfiniteShards,
        statusBanner = forgeStatusBanner,
        onStatusBannerChange = { forgeStatusBanner = it },
        onSaveSlot = { onSaveCustomSlot(it) },
        onSwitchSlot = { slotId ->
          onSwitchCustomSlot(slotId)
          forgeStatusBanner = "SWITCHED ACTIVE LOADOUT TO ${slotId.uppercase(Locale.US)}"
        },
        onUnlockSlot = { slotId ->
          val gate = RiftProgressionCatalog.getFrameSlotGate(slotId)
          forgeStatusBanner =
            if (onUnlockCustomSlot(slotId)) {
              if (isTesterInfiniteShards) {
                "UNLOCKED ${slotId.uppercase(Locale.US)} (TESTER 999,999 SHARDS — 0 DEPLETED)"
              } else {
                "UNLOCKED ${slotId.uppercase(Locale.US)} (-${gate.shardCost} RIFT SHARDS)"
              }
            } else {
              "REQUIRES ${gate.freeAtResonance} PEAK RESONANCE (FREE) OR ${gate.shardCost} RIFT SHARDS"
            }
        },
        onRespecSlot = {
          forgeStatusBanner =
            if (onRespecCustomSlot()) {
              if (isTesterInfiniteShards) {
                "RESPEC COMPLETE (TESTER 0 SHARDS DEPLETED): ALL ALLOCATED POINTS RESET TO 0"
              } else {
                "RESPEC COMPLETE (-50 SHARDS): ALL ALLOCATED POINTS RESET TO 0"
              }
            } else {
              "ALLOCATE >0 POINTS FIRST (REQUIRES 50 RIFT SHARDS TO RESPEC)"
            }
        },
        onForgeAiHull = { prompt, hullName ->
          forgeStatusBanner = onForgeAiHullDesign(prompt, hullName).second
        },
        onDeployNinthShip = {
          currentShipId = 10
          onDeployPilot(
            UgcModeration.sanitizeIdentifier(callsignInput.trim(), "COMMANDER"),
            10,
            currentMode,
            false,
          )
        },
        onBackToMatrix = { showFullNinthShipForgeModal = false },
      )
    } else {
      Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Column(
          modifier = Modifier.weight(0.62f).fillMaxHeight(),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          for (rowIndex in 0 until 3) {
            Row(
              modifier = Modifier.fillMaxWidth().weight(1f),
              horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              for (colIndex in 0 until 3) {
                val ship = allTenShips[rowIndex * 3 + colIndex]
                MatrixShipComparisonCell(
                  modifier = Modifier.weight(1f).fillMaxHeight(),
                  ship = ship,
                  shipResonance = shipResonances[ship.id] ?: 0,
                  spriteVersion = spriteVersion,
                  isSelected = currentShipId == ship.id,
                  isLocked = lockedShipId == ship.id,
                  isBestShip = clampedBestShipId == ship.id,
                  isEquipped = selectedShipId == ship.id,
                  nexusSpinPhase = nexusSpinPhase,
                  isCompactPhone = isCompactPhone,
                  onSelect = { handleHullTap(ship.id) },
                )
              }
            }
          }

          // Ship #10: Rift Frame (100% Open at 0 Resonance with Full 100-Pt Parity)
          val isTenthSelected = currentShipId == 10
          val isTenthLocked = lockedShipId == 10
          val isTenthBest = clampedBestShipId == 10
          val isTenthEquipped = selectedShipId == 10
          val tenthShipResonance = shipResonances[10] ?: 0
          val normalizedSlot = CustomShipLoadoutSlot.normalizeSlotId(activeCustomSlot.slotId)
          Box(
            modifier =
              Modifier.fillMaxWidth()
                .height(60.dp)
                .testTag("hangar_ship_card_10")
                .clip(RoundedCornerShape(9.dp))
                .background(
                  Brush.horizontalGradient(
                    listOf(
                      if (isTenthLocked) HangarTabletElevated else if (isTenthSelected) HangarTabletPanel else HangarTabletAbyss,
                      HangarTabletPanel,
                    )
                  )
                )
                .border(
                  width = 1.dp,
                  color = Color(0xFF332052),
                  shape = RoundedCornerShape(9.dp),
                )
                .clickable { handleHullTap(10) }
          ) {
            Canvas(modifier = Modifier.matchParentSize()) {
              drawDistinctHullCardBorders(
                cornerRadiusPx = 9.dp.toPx(),
                isBestShip = isTenthBest,
                isLocked = isTenthLocked,
                isSelected = isTenthSelected,
                isEquipped = isTenthEquipped,
                phase = nexusSpinPhase,
              )
            }
            Row(
              modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 5.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(0.50f),
              ) {
                Box(
                  modifier =
                    Modifier.size(44.dp)
                      .background(HangarTabletAbyss, RoundedCornerShape(7.dp))
                      .border(
                        1.dp,
                        when {
                          isTenthBest -> Color(0xFFFFD700)
                          isTenthLocked -> HangarNexusTeal
                          else -> Color(0xFFC084FC)
                        },
                        RoundedCornerShape(7.dp),
                      ),
                  contentAlignment = Alignment.Center,
                ) {
                  if (isTenthLocked) {
                    SwirlingNexusLockBackdrop(modifier = Modifier.fillMaxSize(), phase = nexusSpinPhase)
                  } else {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                      drawCircle(
                        brush = Brush.radialGradient(listOf(Color(0x66EC4899), Color.Transparent)),
                        radius = size.minDimension * 0.48f,
                      )
                    }
                  }
                  if (isTenthBest) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                      drawBestShipResonanceCrownHalo(nexusSpinPhase)
                    }
                  }
                  ShipSpritePreview(shipId = 10, sizeDp = 36)
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                  ) {
                    Text(
                      text = "RIFT FRAME (MODULAR)",
                      color = Color.White,
                      fontSize = if (isCompactPhone) 12.sp else 13.5.sp,
                      fontWeight = FontWeight.Black,
                      fontFamily = Sans,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                    )
                    ShipResonanceGoldBadge(shipResonance = tenthShipResonance, isBestShip = isTenthBest)
                    Text(
                      text =
                        when {
                          isTenthLocked -> "LOCKED"
                          isTenthEquipped -> "EQUIPPED"
                          else -> "TAP LOCK"
                        },
                      color =
                        when {
                          isTenthLocked -> HangarNexusTeal
                          isTenthEquipped -> HangarElectricCyan
                          else -> Color(0xFF94A3B8)
                        },
                      fontSize = 9.2.sp,
                      fontWeight = FontWeight.Black,
                      fontFamily = Mono,
                    )
                  }
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                  ) {
                    ResonanceCrestIcon(sizeDp = 13.dp)
                    Text(
                      text = "$normalizedSlot • $pointsAllocatedInSlot/$effectivePointBudget PTS (OPEN AT 0)",
                      color = Color(0xFFF472B6),
                      fontSize = 9.5.sp,
                      fontWeight = FontWeight.Bold,
                      fontFamily = Mono,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                    )
                  }
                }
              }

              Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                listOf("FRAME A", "FRAME B", "FRAME C", "FRAME D", "FRAME E").forEach { frameLbl ->
                  val isActiveFrame = normalizedSlot.equals(frameLbl, ignoreCase = true)
                  Box(
                    modifier =
                      Modifier.clip(RoundedCornerShape(5.dp))
                        .background(if (isActiveFrame) HangarTabletElevated else HangarTabletAbyss)
                        .border(
                          1.dp,
                          if (isActiveFrame) Color(0xFFEC4899) else Color(0xFF334155),
                          RoundedCornerShape(5.dp),
                        )
                        .padding(horizontal = 5.dp, vertical = 3.dp),
                    contentAlignment = Alignment.Center,
                  ) {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                      ShipSpritePreview(shipId = 10, sizeDp = 14)
                      Text(
                        text = frameLbl.removePrefix("FRAME "),
                        color = if (isActiveFrame) Color.White else Color(0xFF94A3B8),
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = Mono,
                      )
                    }
                  }
                }
              }

              Box(
                modifier =
                  Modifier.testTag("btn_matrix_open_9th_forge")
                    .clip(RoundedCornerShape(6.dp))
                    .background(HangarTabletAbyss)
                    .border(1.dp, Color(0xFFEC4899), RoundedCornerShape(6.dp))
                    .clickable {
                      currentShipId = 10
                      lockedShipId = 10
                      showFullNinthShipForgeModal = true
                    }
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center,
              ) {
                Text(
                  text = "CUSTOMIZE FRAME",
                  color = Color(0xFFFCE7F3),
                  fontSize = 9.5.sp,
                  fontWeight = FontWeight.Black,
                  fontFamily = Mono,
                )
              }
            }
          }
        }

        ShipComparisonInspectorPanel(
          modifier = Modifier.weight(0.38f).fillMaxHeight(),
          ship = inspectedShip,
          shipResonance = shipResonances[inspectedShip.id] ?: 0,
          equippedShip = equippedShip,
          lockedShip = lockedShipSpec,
          isBestShip = inspectedShip.id == clampedBestShipId,
          nexusSpinPhase = nexusSpinPhase,
          spriteVersion = spriteVersion,
          isCurrentShip = inspectedShip.id == selectedShipId,
          isLaunchStaging = stagedTargetTitle != null,
          isCompactPhone = isCompactPhone,
          activeSlot = activeCustomSlot,
          effectivePointBudget = effectivePointBudget,
          onOpenNinthShipForge = { showFullNinthShipForgeModal = true },
          onOpenShipForge = onOpenShipForge,
          onDeployShip = {
            val targetId = lockedShipId ?: return@ShipComparisonInspectorPanel
            onDeployPilot(
              UgcModeration.sanitizeIdentifier(callsignInput.trim(), "COMMANDER"),
              targetId,
              currentMode,
              false,
            )
          },
        )
      }
    }
  }
}

@Composable
private fun MatrixShipComparisonCell(
  modifier: Modifier = Modifier,
  ship: ShipSpec,
  shipResonance: Int = 0,
  spriteVersion: Int,
  isSelected: Boolean,
  isLocked: Boolean = false,
  isBestShip: Boolean = false,
  isEquipped: Boolean,
  nexusSpinPhase: Float = 0f,
  isCompactPhone: Boolean = false,
  onSelect: () -> Unit,
) {
  val accentColor = Color(ship.accentColorHex)
  val bgColor =
    when {
      isLocked -> HangarTabletElevated
      isSelected -> HangarTabletPanel
      else -> HangarTabletAbyss
    }
  val cleanName = cleanShipDisplayTitle(ship)

  Box(
    modifier =
      modifier
        .testTag("hangar_ship_card_${ship.id}")
        .clip(RoundedCornerShape(8.dp))
        .background(bgColor)
        .border(1.dp, Color(0xFF1E3A5F), RoundedCornerShape(8.dp))
        .clickable { onSelect() }
  ) {
    Canvas(modifier = Modifier.matchParentSize()) {
      drawDistinctHullCardBorders(
        cornerRadiusPx = 8.dp.toPx(),
        isBestShip = isBestShip,
        isLocked = isLocked,
        isSelected = isSelected,
        isEquipped = isEquipped,
        phase = nexusSpinPhase,
      )
    }
    Column(
      modifier =
        Modifier.fillMaxSize()
          .padding(
            horizontal = if (isCompactPhone) 6.dp else 8.dp,
            vertical = if (isCompactPhone) 4.dp else 6.dp,
          ),
      verticalArrangement = if (isCompactPhone) Arrangement.Center else Arrangement.SpaceBetween,
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(if (isCompactPhone) 5.dp else 7.dp),
          modifier = Modifier.weight(1f),
        ) {
          val spriteBitmap =
            remember(ship.id, spriteVersion) {
              RiftSpriteBaker.getShipSprite(ship.id, isAllied = true, isHitFlashing = false).asImageBitmap()
            }
          Box(
            modifier =
              Modifier.size(if (isCompactPhone) 34.dp else 38.dp)
                .background(HangarTabletAbyss, RoundedCornerShape(7.dp))
                .border(
                  1.dp,
                  when {
                    isBestShip -> Color(0xFFFFD700)
                    isLocked -> HangarNexusTeal
                    else -> accentColor.copy(alpha = 0.60f)
                  },
                  RoundedCornerShape(7.dp),
                ),
            contentAlignment = Alignment.Center,
          ) {
            if (isLocked) {
              SwirlingNexusLockBackdrop(modifier = Modifier.fillMaxSize(), phase = nexusSpinPhase)
            } else {
              Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                  brush = Brush.radialGradient(listOf(accentColor.copy(alpha = 0.36f), Color.Transparent)),
                  radius = size.minDimension * 0.48f,
                )
              }
            }
            if (isBestShip) {
              Canvas(modifier = Modifier.fillMaxSize()) {
                drawBestShipResonanceCrownHalo(nexusSpinPhase)
              }
            }
            Image(
              bitmap = spriteBitmap,
              contentDescription = cleanName,
              filterQuality = FilterQuality.High,
              modifier = Modifier.size(if (isCompactPhone) 28.dp else 30.dp),
            )
          }
          Column(modifier = Modifier.weight(1f)) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              Text(
                text = cleanName,
                color = Color.White,
                fontSize = if (isCompactPhone) 11.sp else 13.5.sp,
                fontWeight = FontWeight.Black,
                fontFamily = Sans,
                lineHeight = if (isCompactPhone) 12.sp else 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              ShipResonanceGoldBadge(shipResonance = shipResonance, isBestShip = isBestShip)
            }
            Text(
              text = ship.role.uppercase(Locale.US),
              color = accentColor,
              fontSize = if (isCompactPhone) 9.5.sp else 10.2.sp,
              fontWeight = FontWeight.Bold,
              fontFamily = Sans,
              lineHeight = if (isCompactPhone) 10.5.sp else 11.5.sp,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }

        Box(
          modifier =
            Modifier.clip(RoundedCornerShape(4.dp))
              .background(HangarTabletAbyss)
              .border(
                1.dp,
                when {
                  isLocked -> HangarNexusTeal
                  isEquipped -> HangarElectricCyan
                  else -> Color(0xFF334155)
                },
                RoundedCornerShape(4.dp),
              )
              .padding(horizontal = 5.dp, vertical = 1.5.dp)
        ) {
          Text(
            text =
              when {
                isLocked -> "LOCKED"
                isEquipped -> "EQUIPPED"
                else -> "SELECT"
              },
            color =
              when {
                isLocked -> HangarNexusTeal
                isEquipped -> HangarElectricCyan
                else -> Color(0xFF94A3B8)
              },
            fontSize = if (isCompactPhone) 8.8.sp else 9.2.sp,
            fontWeight = FontWeight.Black,
            fontFamily = Mono,
          )
        }
      }

      if (!isCompactPhone) {
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(3.dp)) {
          CardMatrixStatBar("SPD", ship.speedPts, ship.topSpeed.roundToInt().toString())
          CardMatrixStatBar("THR", ship.thrustPts, ship.thrust.roundToInt().toString())
          CardMatrixStatBar("FLX", ship.maxFluxPts, ship.maxFlux.roundToInt().toString())
        }
      }
    }
  }
}

@Composable
private fun CardMatrixStatBar(label: String, pts: Int, valueText: String) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(5.dp),
  ) {
    Text(
      text = label,
      color = Color(0xFF94A3B8),
      fontSize = 9.5.sp,
      fontWeight = FontWeight.Bold,
      fontFamily = Mono,
      modifier = Modifier.width(26.dp),
    )
    Box(
      modifier =
        Modifier.weight(1f)
          .height(5.dp)
          .clip(RoundedCornerShape(2.5.dp))
          .background(HangarTabletPanel)
    ) {
      Box(
        modifier =
          Modifier.fillMaxHeight()
            .fillMaxWidth(baselineAnchoredStatFraction(pts))
            .background(HangarElectricCyan, RoundedCornerShape(2.5.dp))
      )
    }
    Text(
      text = valueText,
      color = Color(0xFFE2E8F0),
      fontSize = 9.5.sp,
      fontWeight = FontWeight.Bold,
      fontFamily = Mono,
      modifier = Modifier.width(34.dp),
    )
  }
}

@Composable
private fun InspectorStatComparisonRow(
  label: String,
  pts: Int,
  value: Float,
  equippedValue: Float,
  showDelta: Boolean,
  isDecimal: Boolean = false,
) {
  val diff = value - equippedValue
  val hasMeaningfulDelta = showDelta && (if (isDecimal) abs(diff) >= 0.05f else abs(diff) >= 1f)
  val valueStr = if (isDecimal) String.format(Locale.US, "%.1f", value) else "${value.roundToInt()}"
  val deltaStr =
    if (!hasMeaningfulDelta) ""
    else {
      val sign = if (diff > 0f) "+" else ""
      if (isDecimal) " ($sign${String.format(Locale.US, "%.1f", diff)})" else " ($sign${diff.roundToInt()})"
    }
  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(6.dp),
  ) {
    Text(
      text = label,
      color = Color(0xFFCBD5E1),
      fontSize = 12.sp,
      fontWeight = FontWeight.Bold,
      fontFamily = Mono,
      modifier = Modifier.width(34.dp),
    )
    Box(
      modifier =
        Modifier.weight(1f)
          .height(6.dp)
          .clip(RoundedCornerShape(3.dp))
          .background(HangarTabletPanel)
    ) {
      Box(
        modifier =
          Modifier.fillMaxHeight()
            .fillMaxWidth(baselineAnchoredStatFraction(pts))
            .background(HangarElectricCyan, RoundedCornerShape(3.dp))
      )
    }
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.End,
      modifier = Modifier.widthIn(min = 76.dp),
    ) {
      Text(text = valueStr, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = Mono)
      if (deltaStr.isNotEmpty()) {
        Text(
          text = deltaStr,
          color = if (diff > 0f) HangarDeltaPositive else HangarDeltaNegative,
          fontSize = 11.sp,
          fontWeight = FontWeight.ExtraBold,
          fontFamily = Mono,
        )
      }
    }
  }
}

@Composable
private fun ShipComparisonInspectorPanel(
  modifier: Modifier = Modifier,
  ship: ShipSpec,
  shipResonance: Int = 0,
  equippedShip: ShipSpec,
  lockedShip: ShipSpec? = null,
  isBestShip: Boolean = false,
  nexusSpinPhase: Float = 0f,
  spriteVersion: Int,
  isCurrentShip: Boolean,
  isLaunchStaging: Boolean = false,
  isCompactPhone: Boolean = false,
  activeSlot: CustomShipLoadoutSlot,
  effectivePointBudget: Int,
  onOpenNinthShipForge: () -> Unit,
  onOpenShipForge: () -> Unit,
  onDeployShip: () -> Unit,
) {
  val isTenth = ship.id == 10
  val isInspectedLocked = lockedShip?.id == ship.id
  val accentColor = if (isTenth) Color(0xFFC084FC) else Color(ship.accentColorHex)
  val cleanTitle = cleanShipDisplayTitle(ship)
  val normalizedSlotLabel = CustomShipLoadoutSlot.normalizeSlotId(activeSlot.slotId).uppercase(Locale.US)

  Column(
    modifier =
      modifier
        .background(HangarTabletPanel, RoundedCornerShape(10.dp))
        .border(1.dp, HangarElectricCyan.copy(alpha = 0.50f), RoundedCornerShape(10.dp))
        .padding(9.dp),
    verticalArrangement = Arrangement.SpaceBetween,
  ) {
    Column(
      modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Row(
        modifier =
          Modifier.fillMaxWidth()
            .background(HangarTabletAbyss, RoundedCornerShape(9.dp))
            .border(
              1.dp,
              if (isInspectedLocked) HangarNexusTeal else accentColor.copy(alpha = 0.55f),
              RoundedCornerShape(9.dp),
            )
            .padding(horizontal = 8.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        val largeSprite =
          remember(ship.id, spriteVersion) {
            RiftSpriteBaker.getShipSprite(ship.id, isAllied = true, isHitFlashing = false).asImageBitmap()
          }
        val holoPrimary = if (isTenth) Color(0xFFA855F7) else HangarNexusTeal
        Box(modifier = Modifier.size(width = 72.dp, height = 62.dp), contentAlignment = Alignment.Center) {
          if (isInspectedLocked) {
            SwirlingNexusLockBackdrop(
              modifier = Modifier.size(58.dp).offset(y = (-3).dp),
              phase = nexusSpinPhase,
            )
          }
          Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val baseCx = w * 0.5f
            val baseCy = h * 0.80f
            val gridCol = holoPrimary.copy(alpha = 0.16f)
            drawLine(gridCol, Offset(w * 0.10f, baseCy), Offset(w * 0.90f, baseCy), 1f)
            drawLine(gridCol, Offset(baseCx, h * 0.12f), Offset(baseCx, h * 0.95f), 1f)
            val conePath =
              Path().apply {
                moveTo(baseCx - w * 0.36f, baseCy)
                lineTo(baseCx - w * 0.44f, h * 0.14f)
                lineTo(baseCx + w * 0.44f, h * 0.14f)
                lineTo(baseCx + w * 0.36f, baseCy)
                close()
              }
            drawPath(
              conePath,
              Brush.verticalGradient(
                listOf(holoPrimary.copy(alpha = 0.04f), holoPrimary.copy(alpha = 0.24f)),
                startY = h * 0.14f,
                endY = baseCy,
              ),
            )
            drawOval(
              holoPrimary.copy(alpha = 0.25f),
              Offset(baseCx - w * 0.42f, baseCy - h * 0.12f),
              Size(w * 0.84f, h * 0.24f),
            )
            drawOval(
              holoPrimary,
              Offset(baseCx - w * 0.42f, baseCy - h * 0.12f),
              Size(w * 0.84f, h * 0.24f),
              style = Stroke(width = 1.6.dp.toPx()),
            )
            drawOval(
              Color.White.copy(alpha = 0.75f),
              Offset(baseCx - w * 0.24f, baseCy - h * 0.07f),
              Size(w * 0.48f, h * 0.14f),
              style = Stroke(width = 1.dp.toPx()),
            )
            if (isBestShip) {
              drawBestShipResonanceCrownHalo(nexusSpinPhase)
            }
          }
          Image(
            bitmap = largeSprite,
            contentDescription = cleanTitle,
            filterQuality = FilterQuality.High,
            modifier = Modifier.size(46.dp).offset(y = (-4).dp),
          )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
          ) {
            Text(
              text = if (isInspectedLocked) "NEXUS LOCK // ACTIVE" else "HOLO-INSPECTOR // STANDBY",
              color = if (isInspectedLocked) HangarNexusTeal else Color(0xFF94A3B8),
              fontSize = 9.5.sp,
              fontWeight = FontWeight.Black,
              fontFamily = Mono,
            )
            ShipResonanceGoldBadge(shipResonance = shipResonance, isBestShip = isBestShip)
          }
          Text(
            text = cleanTitle,
            color = Color.White,
            fontSize = 17.sp,
            fontWeight = FontWeight.Black,
            fontFamily = Sans,
            lineHeight = 19.sp,
          )
          Text(
            text = ship.role.uppercase(Locale.US),
            color = accentColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = Sans,
          )
        }
      }

      if (isTenth) {
        Column(
          modifier =
            Modifier.fillMaxWidth()
              .background(HangarTabletAbyss, RoundedCornerShape(8.dp))
              .border(1.dp, Color(0xFFC084FC), RoundedCornerShape(8.dp))
              .padding(8.dp),
          verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
              ResonanceCrestIcon(sizeDp = 14.dp)
              Text(
                text = "RIFT FRAME ($normalizedSlotLabel) // OPEN AT 0",
                color = Color(0xFF4ADE80),
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Black,
                fontFamily = Mono,
              )
            }
            Text(
              text = "${activeSlot.totalAllocatedPoints()}/$effectivePointBudget PTS",
              color = Color(0xFFC084FC),
              fontSize = 10.5.sp,
              fontWeight = FontWeight.Black,
              fontFamily = Mono,
            )
          }
          Text(
            text = "Full 100-Point Parity Budget unlocked from Day 1. Zero pay-to-win — pure tactical specialization across Frame A–E.",
            color = Color(0xFFCBD5E1),
            fontSize = 11.5.sp,
            lineHeight = 15.sp,
          )
          Button(
            onClick = onOpenNinthShipForge,
            colors = ButtonDefaults.buttonColors(containerColor = HangarTabletElevated),
            border = BorderStroke(1.dp, Color(0xFFC084FC)),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
            modifier = Modifier.fillMaxWidth().height(34.dp).testTag("btn_open_ninth_ship_editor"),
          ) {
            Text(
              text = "CUSTOMIZE RIFT FRAME & HULL FORGE",
              color = Color.White,
              fontSize = 10.5.sp,
              fontWeight = FontWeight.Black,
              fontFamily = Mono,
            )
          }
        }
      } else {
        Text(
          text = ship.tacticalLore,
          color = Color(0xFFCBD5E1),
          fontSize = 11.8.sp,
          lineHeight = 15.5.sp,
        )
      }

      Box(
        modifier =
          Modifier.fillMaxWidth()
            .testTag("btn_inspector_open_turntable_forge")
            .clip(RoundedCornerShape(7.dp))
            .background(HangarTabletAbyss)
            .border(1.dp, Color(0xFFC084FC), RoundedCornerShape(7.dp))
            .clickable { onOpenShipForge() }
            .padding(vertical = 5.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          text = "360° TURNTABLE & COSMETIC VAULT [STAR FORGE]",
          color = Color(0xFFE9D5FF),
          fontSize = 9.8.sp,
          fontWeight = FontWeight.Black,
          fontFamily = Mono,
        )
      }

      Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Column(
          modifier =
            Modifier.fillMaxWidth()
              .background(HangarTabletAbyss, RoundedCornerShape(7.dp))
              .border(1.dp, Color(0xFF1E3A5F), RoundedCornerShape(7.dp))
              .padding(horizontal = 8.dp, vertical = 5.dp),
          verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
          Text(
            text = "PRIMARY WEAPON (${ship.primaryPts} PTS)",
            color = HangarElectricCyan,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            fontFamily = Mono,
          )
          Text(
            text = ship.primaryTitle,
            color = Color.White,
            fontSize = 12.5.sp,
            lineHeight = 15.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = Sans,
          )
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
          ) {
            FluxEmblem(sizeDp = 13.dp)
            Text(
              text = "${ship.primaryDamage.toInt()} DMG • ${ship.primaryCost.toInt()} FLUX",
              color = Color(0xFF94A3B8),
              fontSize = 10.8.sp,
              lineHeight = 14.sp,
              fontFamily = Mono,
            )
          }
        }
        Column(
          modifier =
            Modifier.fillMaxWidth()
              .background(HangarTabletAbyss, RoundedCornerShape(7.dp))
              .border(1.dp, Color(0xFF1E3A5F), RoundedCornerShape(7.dp))
              .padding(horizontal = 8.dp, vertical = 5.dp),
          verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
          Text(
            text = "CLASS ABILITY (${ship.secondaryPts} PTS)",
            color = if (ship.hasBomb) HangarSolarGold else Color(0xFF34D399),
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            fontFamily = Mono,
          )
          Text(
            text = ship.secondaryTitle,
            color = Color.White,
            fontSize = 12.5.sp,
            lineHeight = 15.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = Sans,
          )
          val secDesc =
            when {
              ship.bombType == "INTERDICTOR_DOME" ->
                "320 FLUX • 275PX TWO-WAY STASIS (6.0S DURATION, 8.5S CD)"
              ship.bombType == "PHASE_DASH" ->
                "300 FLUX • 2.5S CD • BLINK + I-FRAMES + 35% RAIL"
              ship.bombType == "CLOAK_EMP" ->
                "36 FLUX/S CLOAK (20 ENTRY) • 0 FLUX 360° EMP (-25% MAX FLUX, 2.0S FREEZE)"
              ship.supportsNativeMultifire ->
                "INNATE SPLIT MULTI-SHOT (0 FLUX EXTRA COST)"
              ship.hasBomb ->
                "${ship.bombDamage.toInt()} DMG • ${ship.bombCost.toInt()} FLUX"
              else ->
                "STARTING REPEL CHARGES: ${ship.initialRepels}"
            }
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
          ) {
            FluxEmblem(sizeDp = 13.dp)
            Text(
              text = secDesc,
              color = Color(0xFF94A3B8),
              fontSize = 10.5.sp,
              lineHeight = 14.sp,
              softWrap = true,
              fontFamily = Mono,
            )
          }
        }
      }

      Column(
        modifier =
          Modifier.fillMaxWidth()
            .background(HangarTabletAbyss, RoundedCornerShape(8.dp))
            .border(1.dp, Color(0xFF1E3A5F), RoundedCornerShape(8.dp))
            .padding(7.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        val showComparison = !isCurrentShip
        if (showComparison) {
          Text(
            text = "COMPARING VS EQUIPPED ${cleanShipDisplayTitle(equippedShip)}",
            color = Color(0xFF94A3B8),
            fontSize = 9.8.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = Mono,
          )
        }
        InspectorStatComparisonRow("SPD", ship.speedPts, ship.topSpeed, equippedShip.topSpeed, showComparison)
        InspectorStatComparisonRow("THR", ship.thrustPts, ship.thrust, equippedShip.thrust, showComparison)
        InspectorStatComparisonRow("TRN", ship.turnPts, ship.turnRate, equippedShip.turnRate, showComparison, isDecimal = true)
        InspectorStatComparisonRow("FLX", ship.maxFluxPts, ship.maxFlux, equippedShip.maxFlux, showComparison)
        InspectorStatComparisonRow("RCH", ship.rechargePts, ship.rechargeRate, equippedShip.rechargeRate, showComparison)
      }
    }

    Spacer(modifier = Modifier.height(6.dp))

    val hasLockedHull = lockedShip != null
    val deployLabelTitle = lockedShip?.let { cleanShipDisplayTitle(it) } ?: cleanTitle
    val ctaContainerColor =
      when {
        !hasLockedHull -> HangarTabletAbyss
        isLaunchStaging -> Color(0xFF059669)
        isCurrentShip -> Color(0xFF16A34A)
        else -> Color(0xFF0284C7)
      }
    val ctaBorderColor =
      when {
        !hasLockedHull -> Color(0xFF475569)
        isLaunchStaging -> HangarNexusTeal
        isCurrentShip -> Color(0xFF4ADE80)
        else -> HangarNexusTeal
      }

    Button(
      onClick = onDeployShip,
      enabled = hasLockedHull,
      colors =
        ButtonDefaults.buttonColors(
          containerColor = ctaContainerColor,
          disabledContainerColor = HangarTabletAbyss,
          contentColor = Color.White,
          disabledContentColor = Color(0xFF94A3B8),
        ),
      border = BorderStroke(1.4.dp, ctaBorderColor),
      shape = RoundedCornerShape(8.dp),
      modifier =
        Modifier.fillMaxWidth()
          .height(40.dp)
          .shadow(4.dp, RoundedCornerShape(8.dp))
          .testTag("btn_deploy_pilot"),
    ) {
      Text(
        text = if (hasLockedHull) "ENTER RIFT // $deployLabelTitle" else "TAP HULL TO LOCK IN FOR RIFT",
        color = if (hasLockedHull) Color.White else Color(0xFF94A3B8),
        fontSize = if (isCompactPhone) 12.sp else 13.sp,
        fontWeight = FontWeight.Black,
        fontFamily = Mono,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

private data class FrameSlotChipSpec(
  val slotId: String,
  val unlocked: Boolean,
  val shortLetter: String,
  val freeResonance: Int,
  val shardCost: Int,
)

@Composable
private fun NinthShipModularForgePanel(
  spec: ShipSpec,
  activeSlot: CustomShipLoadoutSlot,
  slot9BUnlocked: Boolean,
  slot9CUnlocked: Boolean,
  slot9DUnlocked: Boolean = false,
  slot9EUnlocked: Boolean = false,
  effectivePointBudget: Int,
  pointsRemaining: Int,
  isTesterInfiniteShards: Boolean = false,
  statusBanner: String?,
  onStatusBannerChange: (String?) -> Unit,
  onSaveSlot: (CustomShipLoadoutSlot) -> Unit,
  onSwitchSlot: (String) -> Unit,
  onUnlockSlot: (String) -> Unit,
  onRespecSlot: () -> Unit,
  onForgeAiHull: (String, String) -> Unit,
  onDeployNinthShip: () -> Unit,
  onBackToMatrix: () -> Unit,
) {
  val normalizedCurrentSlot = CustomShipLoadoutSlot.normalizeSlotId(activeSlot.slotId)
  var hullNameInput by remember(normalizedCurrentSlot, activeSlot.visualRecipe.hullName) {
    mutableStateOf(activeSlot.visualRecipe.hullName)
  }
  var aiPromptInput by remember(normalizedCurrentSlot, activeSlot.visualRecipe.promptUsed) {
    mutableStateOf(activeSlot.visualRecipe.promptUsed)
  }
  val spriteVersion = RiftSpriteBaker.customShip9Version

  fun adjustStatPoints(currentVal: Int, delta: Int, applyNewVal: (Int) -> CustomShipLoadoutSlot) {
    val maxAddition = pointsRemaining.coerceAtLeast(0)
    val clampedDelta =
      if (delta > 0) delta.coerceAtMost(maxAddition).coerceAtMost(30 - currentVal)
      else delta.coerceAtLeast(-currentVal)
    if (clampedDelta != 0) onSaveSlot(applyNewVal((currentVal + clampedDelta).coerceIn(0, 30)))
  }

  Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
    Row(
      modifier =
        Modifier.fillMaxWidth()
          .background(HangarTabletPanel, RoundedCornerShape(8.dp))
          .border(1.dp, HangarElectricCyan.copy(alpha = 0.50f), RoundedCornerShape(8.dp))
          .padding(horizontal = 8.dp, vertical = 5.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState()),
      ) {
        Box(
          modifier =
            Modifier.testTag("btn_back_to_3x3_matrix")
              .clip(RoundedCornerShape(6.dp))
              .background(HangarTabletAbyss)
              .border(1.dp, HangarElectricCyan, RoundedCornerShape(6.dp))
              .clickable { onBackToMatrix() }
              .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
          Text(
            text = "< 10-HULL MATRIX",
            color = HangarElectricCyan,
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Black,
            fontFamily = Mono,
          )
        }
        val slots =
          listOf(
            FrameSlotChipSpec("Frame A", true, "A", 0, 0),
            FrameSlotChipSpec("Frame B", slot9BUnlocked, "B", 300, 250),
            FrameSlotChipSpec("Frame C", slot9CUnlocked, "C", 650, 400),
            FrameSlotChipSpec("Frame D", slot9DUnlocked, "D", 1100, 600),
            FrameSlotChipSpec("Frame E", slot9EUnlocked, "E", 1600, 600),
          )
        slots.forEach { slotSpec ->
          val isCurrentSlot = normalizedCurrentSlot == slotSpec.slotId
          val safeTag = slotSpec.slotId.replace(" ", "_")
          Row(
            modifier =
              Modifier.testTag("btn_custom_slot_$safeTag")
                .clip(RoundedCornerShape(6.dp))
                .background(
                  when {
                    isCurrentSlot -> HangarTabletElevated
                    else -> HangarTabletAbyss
                  }
                )
                .border(
                  1.dp,
                  when {
                    isCurrentSlot -> Color(0xFFC084FC)
                    slotSpec.unlocked -> Color(0xFF475569)
                    else -> HangarSolarGold
                  },
                  RoundedCornerShape(6.dp),
                )
                .clickable {
                  if (slotSpec.unlocked) onSwitchSlot(slotSpec.slotId)
                  else onUnlockSlot(slotSpec.slotId)
                }
                .padding(horizontal = 7.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp),
          ) {
            if (slotSpec.unlocked) {
              Text(
                text = "FRAME ${slotSpec.shortLetter}",
                color = if (isCurrentSlot) Color.White else Color(0xFFCBD5E1),
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Black,
                fontFamily = Mono,
              )
            } else {
              Text(
                text = slotSpec.shortLetter,
                color = HangarSolarGold,
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Black,
                fontFamily = Mono,
              )
              ResonanceCrestIcon(sizeDp = 12.dp)
              Text(
                text = "${slotSpec.freeResonance}",
                color = HangarNexusTeal,
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Black,
                fontFamily = Mono,
              )
              RiftShardGemIcon(sizeDp = 12.dp)
              Text(
                text = "${slotSpec.shardCost}",
                color = HangarSolarGold,
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Black,
                fontFamily = Mono,
              )
            }
          }
        }
        Row(
          modifier =
            Modifier.testTag("btn_hangar_respec_points")
              .clip(RoundedCornerShape(6.dp))
              .background(HangarTabletAbyss)
              .border(1.dp, Color(0xFFF43F5E), RoundedCornerShape(6.dp))
              .clickable { onRespecSlot() }
              .padding(horizontal = 8.dp, vertical = 4.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          RiftShardGemIcon(sizeDp = 13.dp)
          Text(
            text = if (isTesterInfiniteShards) "RESPEC (0 TEST)" else "RESPEC (50)",
            color = Color(0xFFFDA4AF),
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Black,
            fontFamily = Mono,
          )
        }
      }

      Spacer(modifier = Modifier.width(6.dp))

      Button(
        onClick = onDeployNinthShip,
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
        border = BorderStroke(1.dp, HangarNexusTeal),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
        modifier = Modifier.height(30.dp).testTag("btn_deploy_9th_ship"),
      ) {
        Text(
          text = "ENTER RIFT // RIFT FRAME >",
          color = Color.White,
          fontSize = 10.sp,
          fontWeight = FontWeight.Black,
          fontFamily = Mono,
        )
      }
    }

    if (statusBanner != null) {
      Row(
        modifier =
          Modifier.fillMaxWidth()
            .background(HangarTabletAbyss, RoundedCornerShape(6.dp))
            .border(1.dp, HangarNexusTeal, RoundedCornerShape(6.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(statusBanner, color = HangarNexusTeal, fontSize = 9.5.sp, fontWeight = FontWeight.Bold, fontFamily = Mono)
        Text(
          text = "[X]",
          color = Color(0xFF94A3B8),
          fontSize = 9.5.sp,
          fontFamily = Mono,
          modifier = Modifier.clickable { onStatusBannerChange(null) },
        )
      }
    }

    Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
      Column(
        modifier =
          Modifier.weight(0.54f)
            .fillMaxHeight()
            .background(HangarTabletPanel, RoundedCornerShape(10.dp))
            .border(1.dp, HangarElectricCyan.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
            .padding(9.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = "RIFT FRAME MATRIX (0..30 / STAT)",
            color = HangarNexusTeal,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            fontFamily = Mono,
          )
          Text(
            text = "FREE: $pointsRemaining / $effectivePointBudget PTS (USED: ${activeSlot.totalAllocatedPoints()})",
            color = if (pointsRemaining > 0) Color(0xFF4ADE80) else HangarSolarGold,
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Black,
            fontFamily = Mono,
          )
        }

        AttributePointRow("TOP SPEED", activeSlot.speedPts, "${spec.topSpeed.toInt()} PX/S (380 + 8/pt)", "stat_speed") {
          adjustStatPoints(activeSlot.speedPts, it) { v -> activeSlot.copy(speedPts = v) }
        }
        AttributePointRow("THRUST", activeSlot.thrustPts, "${spec.thrust.toInt()} N/S² (600 + 15/pt)", "stat_thrust") {
          adjustStatPoints(activeSlot.thrustPts, it) { v -> activeSlot.copy(thrustPts = v) }
        }
        AttributePointRow(
          "TURN RATE",
          activeSlot.turnPts,
          "${(spec.turnRate * 10f).roundToInt() / 10f} RAD/S (3.2 + 0.1/pt)",
          "stat_turn",
        ) {
          adjustStatPoints(activeSlot.turnPts, it) { v -> activeSlot.copy(turnPts = v) }
        }
        AttributePointRow("MAX FLUX", activeSlot.maxFluxPts, "${spec.maxFlux.toInt()} FLUX (2200 + 80/pt)", "stat_flux") {
          adjustStatPoints(activeSlot.maxFluxPts, it) { v -> activeSlot.copy(maxFluxPts = v) }
        }
        AttributePointRow(
          "FLUX RECHARGE",
          activeSlot.rechargePts,
          "${spec.rechargeRate.toInt()} FLUX/S (240 + 8/pt)",
          "stat_recharge",
        ) {
          adjustStatPoints(activeSlot.rechargePts, it) { v -> activeSlot.copy(rechargePts = v) }
        }

        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = "PRIMARY WEAPON TIER (${activeSlot.primaryPts} PTS INVESTED)",
          color = HangarElectricCyan,
          fontSize = 9.8.sp,
          fontWeight = FontWeight.Black,
          fontFamily = Mono,
        )
        val primaries = CustomShipLoadoutSlot.PRIMARY_OPTIONS
        for (rowIdx in 0 until 3) {
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            for (colIdx in 0 until 3) {
              val opt = primaries[rowIdx * 3 + colIdx]
              val selected = activeSlot.primaryArchetype == opt.id
              val candidateSlot = activeSlot.copy(primaryArchetype = opt.id)
              val canAfford = candidateSlot.totalAllocatedPoints() <= effectivePointBudget
              Box(
                modifier =
                  Modifier.weight(1f)
                    .testTag("btn_primary_arch_${opt.id}")
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                      when {
                        selected -> Color(0xFF0284C7)
                        else -> HangarTabletAbyss
                      }
                    )
                    .border(
                      1.dp,
                      when {
                        selected -> HangarNexusTeal
                        canAfford -> Color(0xFF334155)
                        else -> Color(0xFF1E293B)
                      },
                      RoundedCornerShape(6.dp),
                    )
                    .clickable {
                      if (canAfford || selected) onSaveSlot(candidateSlot)
                      else onStatusBannerChange("NEED ${opt.pointCost} PTS FOR ${opt.title} (FREE SOME STAT POINTS FIRST).")
                    }
                    .padding(vertical = 5.dp, horizontal = 4.dp),
                contentAlignment = Alignment.Center,
              ) {
                Text(
                  text = "${opt.title} (${opt.pointCost}P)",
                  color =
                    when {
                      selected -> Color.White
                      canAfford -> Color(0xFFCBD5E1)
                      else -> Color(0xFF64748B)
                    },
                  fontSize = 9.5.sp,
                  lineHeight = 11.sp,
                  textAlign = TextAlign.Center,
                  fontWeight = FontWeight.Black,
                  fontFamily = Mono,
                  maxLines = 2,
                  softWrap = true,
                )
              }
            }
          }
        }

        Text(
          text = "CLASS & CRATE ABILITY SLOT (${activeSlot.secondaryPts} PTS • 4TH BUFF SLOT STAYS SEPARATE)",
          color = HangarSolarGold,
          fontSize = 9.8.sp,
          fontWeight = FontWeight.Black,
          fontFamily = Mono,
        )
        CustomShipLoadoutSlot.getAllAvailableSecondaryOptions().chunked(2).forEach { rowOptions ->
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            rowOptions.forEach { secOpt ->
              val isSelected = activeSlot.secondaryType == secOpt.id
              val candidateSlot = activeSlot.copy(secondaryType = secOpt.id)
              val canAfford = candidateSlot.totalAllocatedPoints() <= effectivePointBudget
              Box(
                modifier =
                  Modifier.weight(1f)
                    .testTag("btn_secondary_type_${secOpt.id}")
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                      when {
                        isSelected -> HangarTabletElevated
                        else -> HangarTabletAbyss
                      }
                    )
                    .border(
                      1.dp,
                      when {
                        isSelected -> HangarSolarGold
                        canAfford -> Color(0xFF334155)
                        else -> Color(0xFF1E293B)
                      },
                      RoundedCornerShape(6.dp),
                    )
                    .clickable {
                      if (canAfford || isSelected) onSaveSlot(candidateSlot)
                      else onStatusBannerChange("NEED ${secOpt.pointCost} PTS FOR ${secOpt.title} (FREE SOME STAT POINTS FIRST).")
                    }
                    .padding(vertical = 5.dp, horizontal = 5.dp),
                contentAlignment = Alignment.Center,
              ) {
                Text(
                  text = "${secOpt.title} (${secOpt.pointCost}P)",
                  color =
                    when {
                      isSelected -> HangarSolarGold
                      canAfford -> Color(0xFFCBD5E1)
                      else -> Color(0xFF64748B)
                    },
                  fontSize = 9.5.sp,
                  lineHeight = 11.sp,
                  textAlign = TextAlign.Center,
                  fontWeight = FontWeight.Black,
                  fontFamily = Mono,
                  maxLines = 2,
                  softWrap = true,
                )
              }
            }
          }
        }
      }

      Column(
        modifier =
          Modifier.weight(0.46f)
            .fillMaxHeight()
            .background(HangarTabletPanel, RoundedCornerShape(10.dp))
            .border(1.dp, Color(0xFFC084FC).copy(alpha = 0.65f), RoundedCornerShape(10.dp))
            .padding(9.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        val recipe = activeSlot.visualRecipe
        val isFirstFree = activeSlot.aiGenerationsCount == 0
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = "HULL FORGE (PROCEDURAL 3D CHASSIS)",
            color = Color(0xFFC084FC),
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            fontFamily = Mono,
          )
          Text(
            text = "LIVE 3D PREVIEW",
            color = Color(0xFF4ADE80),
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = Mono,
          )
        }
        Row(
          modifier =
            Modifier.fillMaxWidth()
              .background(HangarTabletAbyss, RoundedCornerShape(8.dp))
              .border(1.dp, Color(0xFFC084FC).copy(alpha = 0.45f), RoundedCornerShape(8.dp))
              .padding(8.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          val alliedBmp =
            remember(spriteVersion, recipe) {
              RiftSpriteBaker.getShipSprite(10, isAllied = true, isHitFlashing = false).asImageBitmap()
            }
          val hostileBmp =
            remember(spriteVersion, recipe) {
              RiftSpriteBaker.getShipSprite(10, isAllied = false, isHitFlashing = false).asImageBitmap()
            }
          Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Box(
              modifier =
                Modifier.size(56.dp)
                  .background(HangarTabletPanel, RoundedCornerShape(8.dp))
                  .border(1.dp, HangarElectricCyan, RoundedCornerShape(8.dp)),
              contentAlignment = Alignment.Center,
            ) {
              Image(
                bitmap = alliedBmp,
                contentDescription = "Allied Custom Hull",
                filterQuality = FilterQuality.High,
                modifier = Modifier.size(48.dp),
              )
            }
            Text("ALLIED HULL", color = HangarElectricCyan, fontSize = 9.5.sp, fontWeight = FontWeight.Bold, fontFamily = Mono)
          }
          Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Box(
              modifier =
                Modifier.size(56.dp)
                  .background(HangarTabletPanel, RoundedCornerShape(8.dp))
                  .border(1.dp, Color(0xFFEF4444), RoundedCornerShape(8.dp)),
              contentAlignment = Alignment.Center,
            ) {
              Image(
                bitmap = hostileBmp,
                contentDescription = "Hostile Custom Hull",
                filterQuality = FilterQuality.High,
                modifier = Modifier.size(48.dp),
              )
            }
            Text("HOSTILE HULL", color = Color(0xFFEF4444), fontSize = 9.5.sp, fontWeight = FontWeight.Bold, fontFamily = Mono)
          }
          Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
              text = "SILHOUETTE: ${recipe.silhouette.replace('_', ' ')}",
              color = Color.White,
              fontSize = 9.8.sp,
              fontWeight = FontWeight.Black,
              fontFamily = Mono,
            )
            Text(
              text = "SWEEP: ${(recipe.wingSweep * 100f).roundToInt()}% • PLATES: ${recipe.chassisPlates} • ENG: ${recipe.engineStyle.replace('_', ' ')}",
              color = Color(0xFFCBD5E1),
              fontSize = 9.5.sp,
              fontFamily = Mono,
            )
            Text(
              text = "FINISH: CUSTOM 3D METALLIC & CORE GLOW",
              color = Color(0xFFC084FC),
              fontSize = 9.5.sp,
              fontFamily = Mono,
            )
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Text(
            text = "HULL NAME:",
            color = Color(0xFFCBD5E1),
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = Mono,
          )
          Box(
            modifier =
              Modifier.weight(1f)
                .height(34.dp)
                .background(HangarTabletAbyss, RoundedCornerShape(8.dp))
                .border(1.dp, Color(0xFFC084FC), RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp),
            contentAlignment = Alignment.CenterStart,
          ) {
            BasicTextField(
              value = hullNameInput,
              onValueChange = { if (it.length <= 18) hullNameInput = it },
              singleLine = true,
              textStyle =
                TextStyle(
                  color = Color.White,
                  fontSize = 10.5.sp,
                  fontWeight = FontWeight.Bold,
                  fontFamily = Mono,
                ),
              cursorBrush = SolidColor(Color(0xFFC084FC)),
              modifier = Modifier.fillMaxWidth().testTag("input_custom_hull_name"),
            )
          }
        }

        Text(
          text = "DESCRIBE YOUR CUSTOM SHIP HULL:",
          color = Color(0xFFCBD5E1),
          fontSize = 9.5.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = Mono,
        )
        Box(
          modifier =
            Modifier.fillMaxWidth()
              .height(48.dp)
              .background(HangarTabletAbyss, RoundedCornerShape(8.dp))
              .border(1.dp, Color(0xFFC084FC).copy(alpha = 0.65f), RoundedCornerShape(8.dp))
              .padding(horizontal = 10.dp, vertical = 6.dp),
          contentAlignment = Alignment.CenterStart,
        ) {
          if (aiPromptInput.isEmpty()) {
            Text(
              text = "Describe your custom ship hull (e.g., 'crimson bio-manta stealth scythe with gold plasma core')...",
              color = Color(0xFF64748B),
              fontSize = 9.5.sp,
              fontFamily = Mono,
            )
          }
          BasicTextField(
            value = aiPromptInput,
            onValueChange = { if (it.length <= 140) aiPromptInput = it },
            textStyle = TextStyle(color = Color.White, fontSize = 9.8.sp, fontFamily = Mono),
            cursorBrush = SolidColor(Color(0xFFC084FC)),
            modifier = Modifier.fillMaxSize().testTag("input_ai_hull_prompt"),
          )
        }

        Row(
          modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
          val presets =
            listOf(
              "Obsidian Razor Interceptor" to
                "Aggressive forward-swept obsidian razor interceptor with neon cyan quad plasma thrusters",
              "Bio-Manta Emerald Wraith" to
                "Organic curved bio-manta stingray with emerald bioluminescent wings and gold core",
              "Solar Bastion Dreadnought" to
                "Heavy armored golden dreadnought catamaran with twin fusion cannons and solar trim",
              "Crimson Scythe Valkyrie" to
                "Swept-wing crimson X-foil valkyrie striker with violet warp rings and titanium plates",
            )
          presets.forEachIndexed { idx, (chipTitle, chipPrompt) ->
            Box(
              modifier =
                Modifier.testTag("chip_ai_prompt_$idx")
                  .clip(RoundedCornerShape(8.dp))
                  .background(HangarTabletAbyss)
                  .border(1.dp, Color(0xFFC084FC), RoundedCornerShape(8.dp))
                  .clickable {
                    hullNameInput = chipTitle.take(18).uppercase(Locale.US)
                    aiPromptInput = chipPrompt
                  }
                  .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
              Text(chipTitle, color = Color(0xFFE9D5FF), fontSize = 9.5.sp, fontWeight = FontWeight.Bold, fontFamily = Mono)
            }
          }
        }

        Button(
          onClick = { onForgeAiHull(aiPromptInput, hullNameInput) },
          colors = ButtonDefaults.buttonColors(containerColor = HangarTabletElevated),
          border = BorderStroke(1.2.dp, Color(0xFFC084FC)),
          shape = RoundedCornerShape(8.dp),
          modifier =
            Modifier.fillMaxWidth()
              .height(36.dp)
              .shadow(2.dp, RoundedCornerShape(8.dp))
              .testTag("btn_forge_ai_ship_design"),
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            if (!isFirstFree) {
              RiftShardGemIcon(sizeDp = 14.dp)
            }
            Text(
              text =
                when {
                  isFirstFree -> "FORGE HULL DESIGN (1ST DESIGN FREE)"
                  isTesterInfiniteShards -> "FORGE HULL DESIGN (150 • TESTER 0 DEPLETED)"
                  else -> "FORGE HULL DESIGN (150 RIFT SHARDS)"
                },
              color = Color.White,
              fontSize = 10.sp,
              fontWeight = FontWeight.Black,
              fontFamily = Mono,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun AttributePointRow(
  label: String,
  pts: Int,
  statReadout: String,
  tagPrefix: String,
  onDelta: (Int) -> Unit,
) {
  Row(
    modifier =
      Modifier.fillMaxWidth()
        .background(HangarTabletAbyss, RoundedCornerShape(6.dp))
        .border(1.dp, Color(0xFF1E3A5F), RoundedCornerShape(6.dp))
        .padding(horizontal = 7.dp, vertical = 4.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Column(modifier = Modifier.width(142.dp)) {
      Text(label, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black, fontFamily = Mono)
      Text(statReadout, color = HangarElectricCyan, fontSize = 9.5.sp, fontWeight = FontWeight.Bold, fontFamily = Mono)
    }
    Box(
      modifier =
        Modifier.weight(1f)
          .padding(horizontal = 6.dp)
          .height(6.dp)
          .clip(RoundedCornerShape(3.dp))
          .background(HangarTabletPanel)
    ) {
      Box(
        modifier =
          Modifier.fillMaxHeight()
            .fillMaxWidth(baselineAnchoredStatFraction(pts))
            .background(HangarElectricCyan, RoundedCornerShape(3.dp))
      )
    }
    Text(
      "$pts/30",
      color = Color.White,
      fontSize = 9.5.sp,
      fontWeight = FontWeight.Black,
      fontFamily = Mono,
      modifier = Modifier.width(38.dp),
    )
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
      PointStepBtn("-5", "${tagPrefix}_m5") { onDelta(-5) }
      PointStepBtn("-1", "${tagPrefix}_m1") { onDelta(-1) }
      PointStepBtn("+1", "${tagPrefix}_p1") { onDelta(1) }
      PointStepBtn("+5", "${tagPrefix}_p5") { onDelta(5) }
    }
  }
}

@Composable
private fun PointStepBtn(label: String, tag: String, onClick: () -> Unit) {
  Box(
    modifier =
      Modifier.testTag(tag)
        .clip(RoundedCornerShape(4.dp))
        .background(HangarTabletPanel)
        .border(1.dp, Color(0xFF475569), RoundedCornerShape(4.dp))
        .clickable { onClick() }
        .padding(horizontal = 6.dp, vertical = 3.dp),
    contentAlignment = Alignment.Center,
  ) {
    Text(label, color = Color.White, fontSize = 9.5.sp, fontWeight = FontWeight.Black, fontFamily = Mono)
  }
}

@Composable
fun ShipSpritePreview(shipId: Int, sizeDp: Int = 32, isAllied: Boolean = true) {
  val customVer = RiftSpriteBaker.customShip9Version
  val trimVer = RiftSpriteBaker.activeHullTrimStyle
  val shipName = remember(shipId) { RiftCodex.getShipById(shipId.coerceIn(1, 10)).name }
  val bmp =
    remember(shipId, isAllied, customVer, trimVer) {
      RiftSpriteBaker.getShipSprite(shipId.coerceIn(1, 10), isAllied = isAllied, isHitFlashing = false).asImageBitmap()
    }
  Image(
    bitmap = bmp,
    contentDescription = shipName,
    filterQuality = FilterQuality.High,
    modifier = Modifier.size(sizeDp.dp),
  )
}
