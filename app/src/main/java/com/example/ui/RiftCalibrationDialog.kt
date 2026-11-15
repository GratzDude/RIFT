package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.RiftRepository
import com.example.engine.BuffAimMode
import com.example.engine.CalibrationParamDescriptor
import com.example.engine.CustomBuffEffect
import com.example.engine.LabelPlacement
import com.example.engine.RiftCalibrationState
import com.example.engine.RiftCodex
import com.example.engine.RiftGameEngine
import com.example.engine.RiftSpriteBaker
import com.example.engine.SpecialBuffRegistryEntry
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

private val CALIBRATION_TAB_TITLES =
  listOf(
    "🎛️ HUD & Arena Scale",
    "🚀 Flight & Safe Zone",
    "🛸 Ship Stats",
    "🔫 Weapons",
    "✨ Special Buffs",
    "🌀 Playfield Rifts",
    "🤖 AI Bot Behavior",
    "🔧 Engine Variables",
    "🧪 AI Buff Forge",
  )

private fun resetAllHudElementOffsets(context: Context) {
  RiftCalibrationState.topBarOffset = Offset.Zero
  RiftCalibrationState.buffBarOffset = Offset.Zero
  RiftCalibrationState.killLogOffset = Offset.Zero
  RiftCalibrationState.bottomConsoleOffset = Offset.Zero
  RiftCalibrationState.combatArcOffset = Offset.Zero
  RiftCalibrationState.leftStickOffset = Offset.Zero
  RiftCalibrationState.fireStickOffset = Offset.Zero
  RiftCalibrationState.bombBtnOffset = Offset.Zero
  RiftCalibrationState.repelBtnOffset = Offset.Zero
  RiftCalibrationState.burstBtnOffset = Offset.Zero
  RiftCalibrationState.brakeBtnOffset = Offset.Zero
  RiftCalibrationState.multiBtnOffset = Offset.Zero
  RiftCalibrationState.calibrationBtnOffset = Offset.Zero
  RiftCalibrationState.nexusBtnOffset = Offset.Zero
  RiftCalibrationState.toastContainerOffset = Offset.Zero
  RiftCalibrationState.calibrationRevision++
  RiftCalibrationState.savePersistedLayout(context)
}

@Composable
fun RiftCalibrationDialog(
  engine: RiftGameEngine? = null,
  onDismiss: () -> Unit,
  onSyncToAllTesters: (() -> Unit)? = null,
  onEnterHudDragArenaMode: (() -> Unit)? = null,
  onReturnToNexusCalibration: (() -> Unit)? = null,
) {
  val context = LocalContext.current
  val isCompactPhone = LocalConfiguration.current.screenHeightDp < 480
  val coroutineScope = rememberCoroutineScope()
  val isCustomGame = engine?.currentCustomGame != null

  DisposableEffect(Unit) {
    RiftCalibrationState.ensureBaselineCaptured()
    onDispose {
      RiftCalibrationState.isDraggingAnySlider = false
      RiftCalibrationState.activeDraggingParamKey = null
    }
  }

  val uiUnlocked = RiftCalibrationState.isUiUnlockedForDragging

  BackHandler {
    RiftCalibrationState.isDraggingAnySlider = false
    RiftCalibrationState.activeDraggingParamKey = null
    if (uiUnlocked) {
      RiftCalibrationState.isUiUnlockedForDragging = false
      RiftCalibrationState.savePersistedLayout(context)
      if (onReturnToNexusCalibration != null) {
        onReturnToNexusCalibration()
      } else {
        onDismiss()
      }
    } else {
      onDismiss()
    }
  }

  // MODE A: Live HUD Drag Mode — compact floating top-center control pill over open arena
  if (uiUnlocked) {
    Box(
      modifier =
        Modifier.fillMaxSize()
          .padding(top = 8.dp, start = 12.dp, end = 12.dp)
          .testTag("full_screen_calibration_panel"),
      contentAlignment = Alignment.TopCenter,
    ) {
      Surface(
        modifier =
          Modifier.border(1.2.dp, Color(0xFF00E676), RoundedCornerShape(12.dp)),
        color = Color(0xEB050E1C),
        shape = RoundedCornerShape(12.dp),
      ) {
        Row(
          modifier =
            Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
              .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = "🔓 DRAG ANY HUD BUTTON OR STICK DIRECTLY ON ARENA",
            color = Color(0xFF00E676),
            fontSize = if (isCompactPhone) 9.5.sp else 10.5.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
          )
          CalibrationHeaderActionBtn(
            label = "✓ LOCK & RETURN TO NEXUS",
            bgColor = Color(0xFF063A28),
            borderColor = Color(0xFF00E676),
            textColor = Color(0xFF6EE7B7),
            isCompactPhone = isCompactPhone,
            testTag = "calibration_lock_return_button",
            onClick = {
              RiftCalibrationState.isUiUnlockedForDragging = false
              RiftCalibrationState.savePersistedLayout(context)
              if (onReturnToNexusCalibration != null) {
                onReturnToNexusCalibration()
              } else {
                onDismiss()
              }
            },
          )
          CalibrationHeaderActionBtn(
            label = "↺ RESET HUD OFFSETS",
            bgColor = Color(0xFF2B1810),
            borderColor = Color(0xFFF59E0B),
            textColor = Color(0xFFFBBF24),
            isCompactPhone = isCompactPhone,
            testTag = "calibration_reset_hud_offsets_button",
            onClick = {
              resetAllHudElementOffsets(context)
              engine?.onPrizeToast("↺ HUD OFFSETS RESET TO DEFAULT POSITIONS")
            },
          )
        }
      }
    }
    return
  }

  // MODE B: Seamless frameless interior pane inside persistent NEXUS Master Window
  var activeTab by remember { mutableIntStateOf(0) }
  var selectedShipFilter by remember { mutableIntStateOf(engine?.localPlayer?.spec?.id ?: 1) }
  var showGlobalWeaponsInTab3 by remember { mutableStateOf(true) }
  var copiedPromptModalText by remember { mutableStateOf<String?>(null) }
  var copiedModCount by remember { mutableIntStateOf(0) }
  var statusBanner by remember { mutableStateOf<String?>(null) }
  var confirmMasterWipe by remember { mutableStateOf(false) }

  val rev = RiftCalibrationState.calibrationRevision
  val isLivePeeking = RiftCalibrationState.isDraggingAnySlider
  val activeDragKey = RiftCalibrationState.activeDraggingParamKey
  val paneAlpha by
    animateFloatAsState(
      targetValue = if (isLivePeeking) 0.45f else 1.0f,
      animationSpec = tween(durationMillis = 120),
      label = "calibrationPaneAlpha",
    )

  val allDescriptors =
    remember(rev) {
      RiftCalibrationState.buildAllParameterDescriptors()
    }
  val hiddenKeys = RiftCalibrationState.hiddenCalibrationKeys
  val showHidden = RiftCalibrationState.showHiddenParameters

  val filteredDescriptors =
    remember(rev, activeTab, selectedShipFilter, showGlobalWeaponsInTab3, hiddenKeys, showHidden) {
      allDescriptors.filter { desc ->
        if (desc.tabIndex != activeTab) return@filter false
        if (!showHidden && hiddenKeys.contains(desc.key)) return@filter false
        when (activeTab) {
          2 -> selectedShipFilter == 0 || desc.shipIdFilter == selectedShipFilter
          3 ->
            if (desc.shipIdFilter == 0) {
              showGlobalWeaponsInTab3
            } else {
              selectedShipFilter == 0 || desc.shipIdFilter == selectedShipFilter
            }
          else -> true
        }
      }
    }

  Box(
    modifier =
      Modifier.fillMaxSize()
        .padding(8.dp)
        .graphicsLayer { alpha = paneAlpha }
        .testTag("full_screen_calibration_panel"),
  ) {
    Column(
      modifier = Modifier.fillMaxSize(),
      verticalArrangement = Arrangement.spacedBy(if (isCompactPhone) 4.dp else 6.dp),
    ) {
      // 1. INTERIOR HEADER ROW 1 (Frameless inside persistent NEXUS Master Window; zero redundant close button)
      Row(
        modifier =
          Modifier.fillMaxWidth()
            .then(if (isCompactPhone) Modifier.height(28.dp) else Modifier),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f, fill = false).padding(end = 6.dp)) {
          Text(
            text =
              if (isCompactPhone) "⚙️ LIVE CALIBRATION & FORGE"
              else "⚙️ RIFT LIVE CALIBRATION & AI BUFF FORGE",
            color = Color.White,
            fontSize = if (isCompactPhone) 12.5.sp else 14.5.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = FontFamily.SansSerif,
            letterSpacing = 0.6.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          if (!isCompactPhone || isLivePeeking) {
            Text(
              text =
                if (isLivePeeking) {
                  "👁 LIVE PEEK ACTIVE — ARENA VISIBLE THROUGH GLASS"
                } else {
                  "100% LIVE-WIRED BALANCE TUNING • ZOOM & TEXT SIZE LIVE EXCLUSIVELY IN SETTINGS"
                },
              color = if (isLivePeeking) Color(0xFF00E676) else Color(0xFF00F5D4),
              fontSize = if (isCompactPhone) 8.5.sp else 9.5.sp,
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.Monospace,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }

        Row(
          modifier = Modifier.weight(1f, fill = false).horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(5.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          // [📡 SYNC TO ALL]
          CalibrationHeaderActionBtn(
            label = "📡 SYNC TO ALL",
            bgColor = if (isCustomGame) Color(0xFF06283D) else Color(0xFF1E293B),
            borderColor = if (isCustomGame) Color(0xFF00F5D4) else Color(0xFF475569),
            textColor = if (isCustomGame) Color(0xFF00F5D4) else Color(0xFF64748B),
            isCompactPhone = isCompactPhone,
            testTag = "calibration_sync_to_all_button",
            onClick = {
              confirmMasterWipe = false
              if (!isCustomGame) {
                statusBanner =
                  "🔒 MAIN RANKED GAMES USE FACTORY DEFAULTS ONLY. JOIN A CUSTOM GAME IN COMMAND & LOBBY TO SYNC SETTINGS."
              } else {
                RiftCalibrationState.applyCalibrationChangesLive(engine, context)
                onSyncToAllTesters?.invoke()
                val payloadJson = RiftCalibrationState.exportGameplaySyncPayloadJson()
                val cGameId = engine?.currentCustomGame?.instanceId ?: "custom"
                val author = engine?.localPlayer?.callsign ?: "PILOT"
                coroutineScope.launch {
                  RiftRepository().broadcastCustomGameCalibration(cGameId, author, payloadJson)
                }
                statusBanner =
                  "📡 SYNCED LIVE CALIBRATION TO ALL PILOTS IN CUSTOM GAME (${engine?.currentCustomGame?.title})!"
                engine?.onPrizeToast("📡 CALIBRATION SYNCED TO ALL TESTERS!")
              }
            },
          )

          // [🧨 WIPE STATS / ARMADAS / SYNDICATES] (2-step confirmation)
          CalibrationHeaderActionBtn(
            label =
              if (!confirmMasterWipe) {
                if (isCompactPhone) "🧨 WIPE DATA" else "🧨 WIPE STATS / ARMADAS / SYNDICATES"
              } else {
                "⚠️ CONFIRM WIPE"
              },
            bgColor = if (!confirmMasterWipe) Color(0xFF2D0A14) else Color(0xFF7F1D1D),
            borderColor = Color(0xFFFF3B4E),
            textColor = Color(0xFFFF8A98),
            isCompactPhone = isCompactPhone,
            testTag = "calibration_wipe_all_data_button",
            onClick = {
              if (!confirmMasterWipe) {
                confirmMasterWipe = true
                statusBanner =
                  "⚠️ TAP [CONFIRM WIPE] AGAIN TO ZERO ALL STATS, DELETE ALL ARMADAS & DELETE ALL SYNDICATES."
              } else {
                val prefs = com.example.data.LocalPilotPrefs(context)
                engine?.performMasterFreshStartWipe(prefs) ?: prefs.wipeAllStatsArmadasAndSyndicates()
                confirmMasterWipe = false
                statusBanner =
                  "🧨 FRESH START COMPLETE: ALL STATS, ARMADAS, AND SYNDICATES WIPED TO ZERO!"
              }
            },
          )

          // [↺ RESET ALL TO DEFAULTS]
          CalibrationHeaderActionBtn(
            label = if (isCompactPhone) "↺ RESET ALL" else "↺ RESET TO DEFAULTS",
            bgColor = Color(0xFF2B1810),
            borderColor = Color(0xFFF59E0B),
            textColor = Color(0xFFFBBF24),
            isCompactPhone = isCompactPhone,
            testTag = "calibration_reset_all_button",
            onClick = {
              confirmMasterWipe = false
              RiftCalibrationState.resetAll(context, engine)
              statusBanner = "↺ RESTORED ALL CALIBRATION SLIDERS & SHIP STATS TO FACTORY DEFAULTS."
              engine?.onPrizeToast("↺ CALIBRATION RESET TO FACTORY DEFAULTS")
            },
          )

          // [📋 COPY AS AI STUDIO DEFAULTS]
          CalibrationHeaderActionBtn(
            label = if (isCompactPhone) "📋 COPY DEFAULTS" else "📋 COPY AS AI STUDIO DEFAULTS",
            bgColor = Color(0xFF092A22),
            borderColor = Color(0xFF10B981),
            textColor = Color(0xFF34D399),
            isCompactPhone = isCompactPhone,
            testTag = "calibration_copy_defaults_button",
            onClick = {
              confirmMasterWipe = false
              val (promptText, modCount) = RiftCalibrationState.generateAiStudioDefaultsPrompt()
              val clipboard =
                context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
              clipboard?.setPrimaryClip(
                ClipData.newPlainText("RIFT AI Studio Defaults", promptText)
              )
              val toastMsg =
                "✅ Copied AI Studio Default Prompt to Clipboard! ($modCount settings modified)"
              Toast.makeText(context, toastMsg, Toast.LENGTH_LONG).show()
              engine?.onPrizeToast(toastMsg)
              copiedModCount = modCount
              copiedPromptModalText = promptText
              statusBanner = toastMsg
            },
          )
        }
      }

      // MAIN RANKED GAME FACTORY-DEFAULT LOCK vs CUSTOM GAME BANNER
      Box(
        modifier =
          Modifier.fillMaxWidth()
            .background(
              if (isCustomGame) Color(0xFF052E24) else Color(0xFF291A05),
              RoundedCornerShape(7.dp),
            )
            .border(
              1.dp,
              if (isCustomGame) Color(0xFF00F5D4) else Color(0xFFF59E0B),
              RoundedCornerShape(7.dp),
            )
            .padding(
              horizontal = if (isCompactPhone) 8.dp else 10.dp,
              vertical = if (isCompactPhone) 2.5.dp else 5.dp,
            )
            .testTag("calibration_mode_lock_banner"),
      ) {
        Text(
          text =
            if (!isCustomGame) {
              "🔒 MAIN RANKED GAME: Gameplay stats locked to factory defaults. Join/create a Custom Game in COMMAND & LOBBY to test live changes."
            } else {
              "🧪 CUSTOM GAME ACTIVE (${engine?.currentCustomGame?.title}): Live Calibration sliders & [📡 SYNC TO ALL] unlocked!"
            },
          color = if (isCustomGame) Color(0xFF00F5D4) else Color(0xFFFBBF24),
          fontSize = if (isCompactPhone) 8.8.sp else 10.sp,
          fontWeight = FontWeight.ExtraBold,
          fontFamily = FontFamily.Monospace,
          maxLines = if (isCompactPhone) 1 else 2,
          overflow = TextOverflow.Ellipsis,
        )
      }

      // 2. HEADER ROW 2: Quick Toggles Strip
      Row(
        modifier =
          Modifier.fillMaxWidth()
            .then(if (isCompactPhone) Modifier.height(28.dp) else Modifier)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        val hiddenCount = hiddenKeys.size
        CalibrationToggleChip(
          label =
            if (showHidden) {
              "☑ SHOW HIDDEN ($hiddenCount)"
            } else {
              "☐ SHOW HIDDEN ($hiddenCount)"
            },
          active = showHidden,
          activeColor = Color(0xFFF59E0B),
          isCompactPhone = isCompactPhone,
          testTag = "calibration_toggle_show_hidden",
          onClick = {
            RiftCalibrationState.showHiddenParameters = !showHidden
            RiftCalibrationState.calibrationRevision++
          },
        )

        if (hiddenCount > 0) {
          CalibrationToggleChip(
            label = "👁 UNHIDE ALL ($hiddenCount)",
            active = false,
            activeColor = Color(0xFF38BDF8),
            isCompactPhone = isCompactPhone,
            testTag = "calibration_btn_unhide_all",
            onClick = {
              RiftCalibrationState.unhideAllParameters(context)
              statusBanner = "✓ UNHID ALL $hiddenCount CALIBRATION PARAMETERS."
            },
          )
        }

        if (activeTab in 0..7) {
          CalibrationToggleChip(
            label = "↺ RESET THIS TAB",
            active = false,
            activeColor = Color(0xFFF59E0B),
            isCompactPhone = isCompactPhone,
            testTag = "calibration_btn_reset_tab",
            onClick = {
              RiftCalibrationState.resetTab(activeTab, context, engine)
              statusBanner = "↺ RESET ${CALIBRATION_TAB_TITLES[activeTab]} TO DEFAULTS."
            },
          )
        }

        CalibrationToggleChip(
          label = "🌀 FORCE CONVERGENCE",
          active = false,
          activeColor = Color(0xFF00F5D4),
          isCompactPhone = isCompactPhone,
          testTag = "calibration_btn_force_convergence",
          onClick = {
            engine?.concludePublicFfaMatchCycle(forceSimulateWinWithPresence = false)
            statusBanner = "🌀 FORCED NEW PUBLIC FFA SECTOR CONVERGENCE & PROCEDURAL MAP!"
          },
        )

        CalibrationToggleChip(
          label =
            if (RiftCalibrationState.botsEnabled) {
              "🤖 AI BOTS: ON"
            } else {
              "🤖 AI BOTS: OFF"
            },
          active = RiftCalibrationState.botsEnabled,
          activeColor = Color(0xFF10B981),
          isCompactPhone = isCompactPhone,
          testTag = "calibration_toggle_bots",
          onClick = {
            if (!isCustomGame) {
              statusBanner =
                "🔒 AI BOTS ARE ALWAYS ENABLED IN MAIN RANKED GAMES. JOIN A CUSTOM GAME TO TOGGLE BOTS OFF."
            } else {
              RiftCalibrationState.botsEnabled = !RiftCalibrationState.botsEnabled
              RiftCalibrationState.applyCalibrationChangesLive(engine, context)
              statusBanner =
                if (RiftCalibrationState.botsEnabled) {
                  "🤖 AI BOTS ENABLED IN CUSTOM GAME."
                } else {
                  "🤖 AI BOTS REMOVED — HUMAN PILOTS ONLY!"
                }
            }
          },
        )

        CalibrationToggleChip(
          label = "🔒 HUD DRAG: OFF",
          active = false,
          activeColor = Color(0xFF00E676),
          isCompactPhone = isCompactPhone,
          testTag = "calibration_toggle_hud_drag",
          onClick = {
            RiftCalibrationState.isUiUnlockedForDragging = true
            RiftCalibrationState.isCalibrationModalOpen = true
            RiftCalibrationState.savePersistedLayout(context)
            onEnterHudDragArenaMode?.invoke()
          },
        )

        CalibrationToggleChip(
          label =
            if (RiftCalibrationState.forceAllButtonsVisible) {
              "👁 ALL HUD BTNS: ON"
            } else {
              "👁 ALL HUD BTNS: AUTO"
            },
          active = RiftCalibrationState.forceAllButtonsVisible,
          activeColor = Color(0xFF38BDF8),
          isCompactPhone = isCompactPhone,
          testTag = "calibration_toggle_all_buttons",
          onClick = {
            RiftCalibrationState.forceAllButtonsVisible =
              !RiftCalibrationState.forceAllButtonsVisible
            RiftCalibrationState.calibrationRevision++
          },
        )

        CalibrationToggleChip(
          label =
            if (RiftCalibrationState.unlockFullNinthShipOverride) {
              "🛸 RIFT FRAME 100P: ON"
            } else {
              "🛸 RIFT FRAME 100P: OFF"
            },
          active = RiftCalibrationState.unlockFullNinthShipOverride,
          activeColor = Color(0xFFC084FC),
          isCompactPhone = isCompactPhone,
          testTag = "calibration_toggle_ninth_ship",
          onClick = {
            RiftCalibrationState.unlockFullNinthShipOverride =
              !RiftCalibrationState.unlockFullNinthShipOverride
            RiftCalibrationState.savePersistedLayout(context)
            engine?.syncLocalPlayerCustomShipSpec()
          },
        )
      }

      if (statusBanner != null) {
        Box(
          modifier =
            Modifier.fillMaxWidth()
              .background(Color(0xFF0B2538), RoundedCornerShape(7.dp))
              .border(1.dp, Color(0xFF38BDF8), RoundedCornerShape(7.dp))
              .padding(horizontal = 8.dp, vertical = 3.dp),
        ) {
          Text(
            text = statusBanner ?: "",
            color = Color(0xFF7DD3FC),
            fontSize = if (isCompactPhone) 9.sp else 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }

      // 3. 9 ORGANIZED CATEGORY TABS
      Row(
        modifier =
          Modifier.fillMaxWidth()
            .then(if (isCompactPhone) Modifier.height(28.dp) else Modifier)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        CALIBRATION_TAB_TITLES.forEachIndexed { idx, title ->
          val isForgeTab = idx == 8
          val selected = activeTab == idx
          val accent = if (isForgeTab) Color(0xFFC084FC) else Color(0xFF00F5D4)
          Box(
            modifier =
              Modifier.height(if (isCompactPhone) 26.dp else 32.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(
                  if (selected) Brush.verticalGradient(listOf(accent, accent))
                  else Brush.verticalGradient(listOf(Color(0xFF102038), Color(0xFF091324)))
                )
                .border(1.dp, if (selected) accent else accent.copy(alpha = 0.45f), RoundedCornerShape(7.dp))
                .clickable { activeTab = idx }
                .padding(horizontal = if (isCompactPhone) 8.dp else 10.dp)
                .testTag("calibration_tab_$idx"),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text = title,
              color = if (selected) Color(0xFF040914) else Color.White,
              fontSize = if (isCompactPhone) 9.2.sp else 10.sp,
              fontWeight = FontWeight.ExtraBold,
              fontFamily = FontFamily.SansSerif,
              maxLines = 1,
            )
          }
        }
      }

      // 4. OPTIONAL SUB-BAR FOR TABS 0, 2 & 3
      if (activeTab == 0) {
        ShipLabelClockPlacementBar(
          isCompactPhone = isCompactPhone,
          onPlacementChanged = {
            RiftCalibrationState.applyCalibrationChangesLive(engine, context)
          },
        )
      } else if (activeTab == 2 || activeTab == 3) {
        Row(
          modifier =
            Modifier.fillMaxWidth()
              .then(if (isCompactPhone) Modifier.height(26.dp) else Modifier)
              .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(5.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          if (activeTab == 3) {
            CalibrationToggleChip(
              label = if (showGlobalWeaponsInTab3) "☑ CLASS MECHANICS" else "☐ CLASS MECHANICS",
              active = showGlobalWeaponsInTab3,
              activeColor = Color(0xFF34D399),
              isCompactPhone = isCompactPhone,
              testTag = "calibration_toggle_global_weapons",
              onClick = { showGlobalWeaponsInTab3 = !showGlobalWeaponsInTab3 },
            )
          }
          CalibrationToggleChip(
            label = "ALL SHIPS",
            active = selectedShipFilter == 0,
            activeColor = Color(0xFFFFD700),
            isCompactPhone = isCompactPhone,
            testTag = "calibration_ship_filter_0",
            onClick = { selectedShipFilter = 0 },
          )
          for (sId in 1..10) {
            val sName = if (sId == 10) "Rift Frame" else RiftCodex.getShipById(sId).name
            CalibrationToggleChip(
              label = sName,
              active = selectedShipFilter == sId,
              activeColor = Color(0xFF00F5D4),
              isCompactPhone = isCompactPhone,
              testTag = "calibration_ship_filter_$sId",
              onClick = { selectedShipFilter = sId },
            )
          }
        }
      }

      // 5. MAIN TAB CONTENT
      if (activeTab == 8) {
        AiBuffAndMechanicForgeTab(
          modifier = Modifier.weight(1f).fillMaxWidth(),
          engine = engine,
          onStatusMessage = { msg -> statusBanner = msg },
        )
      } else {
        LazyColumn(
          modifier =
            Modifier.weight(1f)
              .fillMaxWidth()
              .background(
                if (isLivePeeking) Color(0x22091424) else Color(0x88091424),
                RoundedCornerShape(10.dp),
              )
              .padding(8.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          if (filteredDescriptors.isEmpty()) {
            item {
              Box(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                contentAlignment = Alignment.Center,
              ) {
                Text(
                  text =
                    "ALL PARAMETERS IN THIS VIEW ARE CURRENTLY HIDDEN — CHECK [☐ SHOW HIDDEN] ABOVE TO REVEAL THEM.",
                  color = Color(0xFFF59E0B),
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  fontFamily = FontFamily.Monospace,
                )
              }
            }
          } else {
            items(filteredDescriptors, key = { it.key }) { desc ->
              val isHidden = hiddenKeys.contains(desc.key)
              val rowAlpha =
                when {
                  !isLivePeeking -> 1.0f
                  activeDragKey == desc.key -> 1.0f
                  else -> 0.55f
                }
              CalibrationParameterRow(
                descriptor = desc,
                isHidden = isHidden,
                rowAlpha = rowAlpha,
                isActivelyDragged = isLivePeeking && activeDragKey == desc.key,
                defaultValue = RiftCalibrationState.DEFAULT_GAME_CONFIG[desc.key],
                onValueChanged = { newVal ->
                  desc.setter(newVal)
                  RiftCalibrationState.applyCalibrationChangesLive(engine, null)
                },
                onDragFinished = {
                  RiftCalibrationState.isDraggingAnySlider = false
                  RiftCalibrationState.activeDraggingParamKey = null
                  RiftCalibrationState.applyCalibrationChangesLive(engine, context)
                },
                onToggleHidden = { hide ->
                  RiftCalibrationState.toggleParameterHidden(desc.key, hide, context)
                },
              )
            }
          }
        }
      }
    }

    // Selectable Copy-Prompt Fallback Modal
    if (copiedPromptModalText != null) {
      CopyPromptFallbackDialog(
        promptText = copiedPromptModalText ?: "",
        modifiedCount = copiedModCount,
        onCopyAgain = {
          val txt = copiedPromptModalText ?: return@CopyPromptFallbackDialog
          val clipboard =
            context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
          clipboard?.setPrimaryClip(ClipData.newPlainText("RIFT AI Studio Defaults", txt))
          Toast.makeText(
            context,
            "✅ Copied AI Studio Default Prompt to Clipboard! ($copiedModCount settings modified)",
            Toast.LENGTH_SHORT,
          ).show()
        },
        onClose = { copiedPromptModalText = null },
      )
    }
  }
}

@Composable
private fun ShipLabelClockPlacementBar(
  isCompactPhone: Boolean = false,
  onPlacementChanged: () -> Unit,
) {
  Row(
    modifier =
      Modifier.fillMaxWidth()
        .background(Color(0xCC060E1A), RoundedCornerShape(8.dp))
        .border(1.dp, Color(0x4438BDF8), RoundedCornerShape(8.dp))
        .padding(
          horizontal = if (isCompactPhone) 8.dp else 10.dp,
          vertical = if (isCompactPhone) 3.dp else 6.dp,
        )
        .horizontalScroll(rememberScrollState()),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      text =
        if (isCompactPhone) "SHIP LABEL POS:"
        else "IN-SPACE LABEL CLOCK ORIENTATION (Camera Zoom & Text Size live in SETTINGS):",
      color = Color(0xFF94A3B8),
      fontSize = if (isCompactPhone) 8.8.sp else 9.5.sp,
      fontWeight = FontWeight.ExtraBold,
      fontFamily = FontFamily.Monospace,
      modifier = Modifier.padding(end = 8.dp),
    )

    Row(
      horizontalArrangement = Arrangement.spacedBy(6.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      val placements = LabelPlacement.entries
      CalibrationToggleChip(
        label = "BOUNTY POS: ${RiftCalibrationState.bountyPlacement.label}",
        active = false,
        activeColor = Color(0xFFFFD700),
        isCompactPhone = isCompactPhone,
        testTag = "calibration_cycle_bounty_pos",
        onClick = {
          val nextIdx = (placements.indexOf(RiftCalibrationState.bountyPlacement) + 1) % placements.size
          RiftCalibrationState.bountyPlacement = placements[nextIdx]
          onPlacementChanged()
        },
      )
      CalibrationToggleChip(
        label = "CALLSIGN POS: ${RiftCalibrationState.namePlacement.label}",
        active = false,
        activeColor = Color(0xFF38BDF8),
        isCompactPhone = isCompactPhone,
        testTag = "calibration_cycle_name_pos",
        onClick = {
          val nextIdx = (placements.indexOf(RiftCalibrationState.namePlacement) + 1) % placements.size
          RiftCalibrationState.namePlacement = placements[nextIdx]
          RiftCalibrationState.tagPlacement = placements[nextIdx]
          onPlacementChanged()
        },
      )
    }
  }
}

@Composable
private fun CalibrationParameterRow(
  descriptor: CalibrationParamDescriptor,
  isHidden: Boolean,
  rowAlpha: Float = 1.0f,
  isActivelyDragged: Boolean = false,
  defaultValue: Float?,
  onValueChanged: (Float) -> Unit,
  onDragFinished: () -> Unit,
  onToggleHidden: (Boolean) -> Unit,
) {
  val currentVal = descriptor.getter()
  val modifiedDefault = defaultValue?.takeIf { abs(currentVal - it) > 0.0005f }
  val isModified = modifiedDefault != null
  val borderColor =
    when {
      isActivelyDragged -> Color(0xFF00F5D4)
      isHidden -> Color(0xFFF59E0B)
      isModified -> Color(0xFF10B981)
      else -> Color(0x3338BDF8)
    }

  Column(
    modifier =
      Modifier.fillMaxWidth()
        .graphicsLayer { alpha = rowAlpha }
        .background(
          when {
            isActivelyDragged -> Color(0xF20D1B2E)
            isHidden -> Color(0xCC241A08)
            else -> Color(0xCC0D1B2E)
          },
          RoundedCornerShape(9.dp),
        )
        .border(if (isActivelyDragged) 1.5.dp else 1.dp, borderColor, RoundedCornerShape(9.dp))
        .padding(horizontal = 10.dp, vertical = 6.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Text(
            text = descriptor.title,
            color = Color.White,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = FontFamily.SansSerif,
          )
          Text(
            text = descriptor.formatValue(currentVal),
            color = if (isModified) Color(0xFF34D399) else Color(0xFF38BDF8),
            fontSize = 11.5.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = FontFamily.Monospace,
          )
          if (modifiedDefault != null) {
            Text(
              text = "(Default: ${descriptor.formatValue(modifiedDefault)})",
              color = Color(0xFF94A3B8),
              fontSize = 9.5.sp,
              fontFamily = FontFamily.Monospace,
            )
          }
        }
        Text(
          text = descriptor.description,
          color = Color(0xFF94A3B8),
          fontSize = 9.5.sp,
          fontFamily = FontFamily.SansSerif,
        )
      }

      Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        if (modifiedDefault != null) {
          Box(
            modifier =
              Modifier.clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF1E293B))
                .border(1.dp, Color(0xFF34D399), RoundedCornerShape(6.dp))
                .clickable {
                  onValueChanged(modifiedDefault)
                  onDragFinished()
                }
                .padding(horizontal = 7.dp, vertical = 4.dp),
          ) {
            Text(
              text = "↺",
              color = Color(0xFF34D399),
              fontSize = 10.sp,
              fontWeight = FontWeight.ExtraBold,
            )
          }
        }

        Box(
          modifier =
            Modifier.clip(RoundedCornerShape(6.dp))
              .background(if (isHidden) Color(0xFF422806) else Color(0xFF122036))
              .border(
                1.dp,
                if (isHidden) Color(0xFFF59E0B) else Color(0x44FFFFFF),
                RoundedCornerShape(6.dp),
              )
              .clickable { onToggleHidden(!isHidden) }
              .padding(horizontal = 8.dp, vertical = 4.dp)
              .testTag("hide_checkbox_${descriptor.key}"),
        ) {
          Text(
            text = if (isHidden) "☑ Hidden" else "☐ Hide",
            color = if (isHidden) Color(0xFFFBBF24) else Color(0xFFCBD5E1),
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
          )
        }
      }
    }

    // Step [-] + Live Peek Slider + Step [+]
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      StepButton(
        symbol = "−",
        onClick = {
          val stepped = (currentVal - descriptor.step).coerceIn(descriptor.min, descriptor.max)
          onValueChanged(stepped)
          onDragFinished()
        },
      )

      Slider(
        value = currentVal.coerceIn(descriptor.min, descriptor.max),
        onValueChange = { raw ->
          RiftCalibrationState.activeDraggingParamKey = descriptor.key
          RiftCalibrationState.isDraggingAnySlider = true
          val snapped =
            if (descriptor.step > 0f) {
              val steps = ((raw - descriptor.min) / descriptor.step).roundToInt()
              (descriptor.min + steps * descriptor.step).coerceIn(descriptor.min, descriptor.max)
            } else {
              raw.coerceIn(descriptor.min, descriptor.max)
            }
          onValueChanged(snapped)
        },
        onValueChangeFinished = {
          onDragFinished()
        },
        valueRange = descriptor.min..descriptor.max,
        colors =
          SliderDefaults.colors(
            thumbColor = Color(0xFF00F0FF),
            activeTrackColor = Color(0xFF38BDF8),
            inactiveTrackColor = Color(0xFF1E293B),
          ),
        modifier = Modifier.weight(1f).height(26.dp),
      )

      StepButton(
        symbol = "+",
        onClick = {
          val stepped = (currentVal + descriptor.step).coerceIn(descriptor.min, descriptor.max)
          onValueChanged(stepped)
          onDragFinished()
        },
      )
    }
  }
}

@Composable
private fun AiBuffAndMechanicForgeTab(
  modifier: Modifier = Modifier,
  engine: RiftGameEngine?,
  onStatusMessage: (String) -> Unit,
) {
  val context = LocalContext.current
  var promptText by remember {
    mutableStateOf(
      "Create a buff called Gravity Well that fires a slow violet orb that pulls nearby enemies inward for 3 seconds, 2 charges"
    )
  }
  var buffName by remember { mutableStateOf("GRAVITY WELL") }
  var shortLabel by remember { mutableStateOf("GRAVITY") }
  var colorHex by remember { mutableStateOf("#C084FC") }
  var charges by remember { mutableIntStateOf(2) }
  var aimMode by remember { mutableStateOf(BuffAimMode.AIM_DIRECTIONAL) }
  var specialEffect by remember { mutableStateOf(CustomBuffEffect.PULL) }
  var projectileSpeed by remember { mutableFloatStateOf(340f) }
  var flightDuration by remember { mutableFloatStateOf(3.0f) }
  var damage by remember { mutableFloatStateOf(360f) }
  var blastRadius by remember { mutableFloatStateOf(260f) }
  var wallBounces by remember { mutableIntStateOf(2) }
  var forgedListVersion by remember { mutableIntStateOf(0) }

  val customBuffs =
    remember(forgedListVersion, RiftCalibrationState.calibrationRevision) {
      RiftCodex.getCustomForgedBuffs()
    }

  fun applySynthesized(entry: SpecialBuffRegistryEntry) {
    buffName = entry.officialName
    shortLabel = entry.shortLabel
    colorHex = entry.strokeColorHex
    charges = entry.chargesPerPickup
    aimMode = entry.aimMode
    specialEffect = entry.specialEffect
    projectileSpeed = entry.projectileSpeed
    flightDuration = entry.flightDuration
    damage = entry.damage
    blastRadius = entry.blastRadius
    wallBounces = entry.wallBounces
  }

  val isCompactPhone = LocalConfiguration.current.screenHeightDp < 480
  val swatchColors = listOf("#C084FC", "#22D3EE", "#34D399", "#FB7185", "#FACC15", "#FB923C")

  Row(
    modifier =
      modifier
        .background(Color(0x88091424), RoundedCornerShape(10.dp))
        .padding(if (isCompactPhone) 6.dp else 8.dp),
    horizontalArrangement = Arrangement.spacedBy(if (isCompactPhone) 8.dp else 10.dp),
  ) {
    // Left Column: Natural-Language Prompt + Parametric Controls + [GENERATE & INJECT INTO GAME]
    Column(
      modifier =
        Modifier.weight(1.35f)
          .fillMaxHeight()
          .verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(if (isCompactPhone) 5.dp else 7.dp),
    ) {
      Text(
        text = "🧪 IN-GAME AI BUFF & MECHANIC FORGE",
        color = Color(0xFFC084FC),
        fontSize = if (isCompactPhone) 11.5.sp else 12.5.sp,
        fontWeight = FontWeight.ExtraBold,
        fontFamily = FontFamily.SansSerif,
      )
      if (!isCompactPhone) {
        Text(
          text = "Describe any custom Special Buff in plain English or fine-tune its physics below. Clicking [GENERATE & INJECT INTO GAME] registers it into Supply Crates, exposes sliders in Tab 5, and equips it immediately into your 4th Right-Hand Special Buff Slot!",
          color = Color(0xFF94A3B8),
          fontSize = 9.5.sp,
        )
      }

      // Quick Preset Chips
      Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
      ) {
        val presets =
          listOf(
            "🌀 Gravity Well" to "Create a buff called Gravity Well that fires a slow violet orb that pulls nearby enemies inward for 3 seconds, 2 charges",
            "✨ Ion Lance" to "Create a buff called Ion Lance that fires a fast cyan bounce bolt with EMP drain, 3 charges",
            "❄️ Cryo Web" to "Create a buff called Cryo Web that launches a frost snare orb that slows enemy ships in a wide radius for 3.5 seconds, 2 charges",
            "☀️ Solar Pulse" to "Create a buff called Solar Pulse that fires an amber shockwave orb that pushes enemies away with 620 damage, 2 charges",
          )
        presets.forEach { (chipTitle, chipPrompt) ->
          CalibrationToggleChip(
            label = chipTitle,
            active = false,
            activeColor = Color(0xFFC084FC),
            isCompactPhone = isCompactPhone,
            testTag = "forge_preset_${chipTitle.hashCode()}",
            onClick = {
              promptText = chipPrompt
              val syn =
                SpecialBuffRegistryEntry.synthesizeFromPrompt(
                  chipPrompt,
                  RiftCodex.nextCustomBuffTypeIndex(),
                )
              applySynthesized(syn)
            },
          )
        }
      }

      // Unclipped Custom BasicTextField 1: AI Buff Prompt (34.dp height)
      Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
          text = "AI BUFF PROMPT:",
          color = Color(0xFFC084FC),
          fontSize = 8.5.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.Monospace,
        )
        Box(
          modifier =
            Modifier.fillMaxWidth()
              .height(34.dp)
              .clip(RoundedCornerShape(8.dp))
              .background(Color(0xFF050C18))
              .border(1.dp, Color(0x99C084FC), RoundedCornerShape(8.dp))
              .padding(horizontal = 10.dp),
          contentAlignment = Alignment.CenterStart,
        ) {
          if (promptText.isEmpty()) {
            Text(
              text = "Describe custom Special Buff mechanic...",
              color = Color(0xFF64748B),
              fontSize = 11.sp,
              fontFamily = FontFamily.SansSerif,
              maxLines = 1,
            )
          }
          BasicTextField(
            value = promptText,
            onValueChange = { promptText = it },
            singleLine = true,
            textStyle =
              TextStyle(
                color = Color.White,
                fontSize = 11.sp,
                fontFamily = FontFamily.SansSerif,
              ),
            cursorBrush = SolidColor(Color(0xFF00F5D4)),
            modifier = Modifier.fillMaxWidth().testTag("forge_prompt_input"),
          )
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Bottom,
      ) {
        CalibrationHeaderActionBtn(
          label = if (isCompactPhone) "✨ PARSE" else "✨ PARSE PROMPT → STATS",
          bgColor = Color(0xFF23123A),
          borderColor = Color(0xFFC084FC),
          textColor = Color(0xFFE9D5FF),
          isCompactPhone = isCompactPhone,
          testTag = "forge_parse_prompt_button",
          onClick = {
            val syn =
              SpecialBuffRegistryEntry.synthesizeFromPrompt(
                promptText,
                RiftCodex.nextCustomBuffTypeIndex(),
              )
            applySynthesized(syn)
            onStatusMessage("✨ PARSED PROMPT INTO [${syn.officialName}] (${syn.specialEffect.label}).")
          },
        )

        // Unclipped Custom BasicTextField 2: Buff Name (34.dp height)
        Column(
          modifier = Modifier.weight(1f),
          verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
          Text(
            text = "BUFF NAME:",
            color = Color(0xFF94A3B8),
            fontSize = 8.5.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
          )
          Box(
            modifier =
              Modifier.fillMaxWidth()
                .height(34.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF050C18))
                .border(1.dp, Color(0x7738BDF8), RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp),
            contentAlignment = Alignment.CenterStart,
          ) {
            if (buffName.isEmpty()) {
              Text(
                text = "GRAVITY WELL",
                color = Color(0xFF64748B),
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
              )
            }
            BasicTextField(
              value = buffName,
              onValueChange = { buffName = it.uppercase(Locale.US).take(20) },
              singleLine = true,
              textStyle =
                TextStyle(
                  color = Color.White,
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  fontFamily = FontFamily.Monospace,
                ),
              cursorBrush = SolidColor(Color(0xFF00F5D4)),
              modifier = Modifier.fillMaxWidth(),
            )
          }
        }

        // Unclipped Custom BasicTextField 3: HUD Label (34.dp height)
        Column(
          modifier = Modifier.width(if (isCompactPhone) 92.dp else 108.dp),
          verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
          Text(
            text = "HUD LABEL:",
            color = Color(0xFF94A3B8),
            fontSize = 8.5.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
          )
          Box(
            modifier =
              Modifier.fillMaxWidth()
                .height(34.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF050C18))
                .border(1.dp, Color(0x7738BDF8), RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp),
            contentAlignment = Alignment.CenterStart,
          ) {
            if (shortLabel.isEmpty()) {
              Text(
                text = "GRAVITY",
                color = Color(0xFF64748B),
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
              )
            }
            BasicTextField(
              value = shortLabel,
              onValueChange = { shortLabel = it.uppercase(Locale.US).take(8) },
              singleLine = true,
              textStyle =
                TextStyle(
                  color = Color(0xFF00F5D4),
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  fontFamily = FontFamily.Monospace,
                ),
              cursorBrush = SolidColor(Color(0xFF00F5D4)),
              modifier = Modifier.fillMaxWidth(),
            )
          }
        }
      }

      // Color Swatches + Aim Mode + Special Effect Row
      Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text("COLOR:", color = Color(0xFF94A3B8), fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
        swatchColors.forEach { hex ->
          val parsedColor =
            runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(Color.Cyan)
          Box(
            modifier =
              Modifier.size(24.dp)
                .clip(CircleShape)
                .background(parsedColor)
                .border(
                  width = if (colorHex.equals(hex, ignoreCase = true)) 2.5.dp else 1.dp,
                  color = if (colorHex.equals(hex, ignoreCase = true)) Color.White else Color.Black,
                  shape = CircleShape,
                )
                .clickable { colorHex = hex }
          )
        }

        Spacer(modifier = Modifier.width(6.dp))
        Text("AIM:", color = Color(0xFF94A3B8), fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
        BuffAimMode.entries.forEach { mode ->
          CalibrationToggleChip(
            label = mode.name.removePrefix("AIM_").removePrefix("TAP_"),
            active = aimMode == mode,
            activeColor = Color(0xFF38BDF8),
            testTag = "forge_aim_${mode.name}",
            onClick = { aimMode = mode },
          )
        }
      }

      // Special Effect Selector Chips
      Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text("MECHANIC:", color = Color(0xFF94A3B8), fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
        CustomBuffEffect.entries.forEach { eff ->
          CalibrationToggleChip(
            label = eff.name,
            active = specialEffect == eff,
            activeColor = Color(0xFFC084FC),
            testTag = "forge_eff_${eff.name}",
            onClick = { specialEffect = eff },
          )
        }
      }

      // Compact Parametric Sliders
      ForgeMiniSlider("Charges Per Pickup", "$charges charges", charges.toFloat(), 1f, 8f) {
        charges = it.roundToInt().coerceIn(1, 8)
      }
      ForgeMiniSlider("Projectile Speed", "${projectileSpeed.roundToInt()} px/s", projectileSpeed, 140f, 1500f) {
        projectileSpeed = it
      }
      ForgeMiniSlider("Flight / Effect Duration", String.format(Locale.US, "%.1f s", flightDuration), flightDuration, 0.5f, 12.0f) {
        flightDuration = it
      }
      ForgeMiniSlider("Flux Damage", "${damage.roundToInt()} Flux", damage, 0f, 2000f) {
        damage = it
      }
      ForgeMiniSlider("Blast / Field Radius", "${blastRadius.roundToInt()} px", blastRadius, 60f, 550f) {
        blastRadius = it
      }

      Button(
        onClick = {
          val cleanTitle = buffName.trim().ifEmpty { "GRAVITY WELL" }
          val cleanShort = shortLabel.trim().ifEmpty { cleanTitle.take(7) }
          val nextIdx = RiftCodex.nextCustomBuffTypeIndex()
          val idSlug =
            "FORGED_" +
              cleanTitle.replace(Regex("[^A-Z0-9]"), "_").trim('_').ifEmpty { "BUFF" } +
              "_$nextIdx"
          val summary =
            "${specialEffect.label} • ${charges}x Charges • ${String.format(Locale.US, "%.1fs", flightDuration)} • ${damage.toInt()} Flux DMG (${blastRadius.toInt()}px)."
          val entry =
            SpecialBuffRegistryEntry(
              id = idSlug,
              buffTypeIndex = nextIdx,
              officialName = cleanTitle,
              shortLabel = cleanShort,
              chargesPerPickup = charges,
              aimMode = aimMode,
              strokeColorHex = colorHex,
              fillColorArgb = SpecialBuffRegistryEntry.fillColorFromStroke(colorHex),
              summary = summary,
              isCustomForged = true,
              projectileSpeed = projectileSpeed,
              flightDuration = flightDuration,
              damage = damage,
              blastRadius = blastRadius,
              specialEffect = specialEffect,
              wallBounces = wallBounces,
              promptUsed = promptText.take(160),
            )
          RiftCodex.registerCustomBuff(entry)
          engine?.equipAndTestCustomForgedBuff(entry)
          RiftCalibrationState.applyCalibrationChangesLive(engine, context)
          forgedListVersion++
          val msg =
            "✨ FORGED & EQUIPPED [${entry.officialName}] (${entry.shortLabel} [${entry.chargesPerPickup}]) IN 4TH RIGHT-HAND BUFF SLOT!"
          onStatusMessage(msg)
        },
        colors =
          ButtonDefaults.buttonColors(
            containerColor = Color(0xFF581C87),
            contentColor = Color.White,
          ),
        border = BorderStroke(1.2.dp, Color(0xFFE879F9)),
        shape = RoundedCornerShape(10.dp),
        modifier =
          Modifier.fillMaxWidth()
            .height(44.dp)
            .testTag("forge_inject_buff_button"),
      ) {
        Text(
          text = "✨ GENERATE & INJECT INTO GAME (EQUIP NOW + ADD TO CRATES)",
          fontSize = 11.sp,
          fontWeight = FontWeight.ExtraBold,
          fontFamily = FontFamily.SansSerif,
        )
      }
    }

    // Right Column: Active Custom Forged Buffs Registry List
    Column(
      modifier =
        Modifier.weight(0.9f)
          .fillMaxHeight()
          .background(Color(0xCC0D1B2E), RoundedCornerShape(10.dp))
          .border(1.dp, Color(0x44C084FC), RoundedCornerShape(10.dp))
          .padding(8.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Text(
        text = "📦 INJECTED FORGED BUFFS (${customBuffs.size})",
        color = Color(0xFFE9D5FF),
        fontSize = 11.sp,
        fontWeight = FontWeight.ExtraBold,
        fontFamily = FontFamily.Monospace,
      )

      if (customBuffs.isEmpty()) {
        Box(
          modifier = Modifier.fillMaxSize(),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = "No custom buffs injected yet.\nTap [GENERATE & INJECT INTO GAME] to register your first custom mechanic into Supply Crates & your Special Buff button!",
            color = Color(0xFF94A3B8),
            fontSize = 10.sp,
          )
        }
      } else {
        LazyColumn(
          modifier = Modifier.weight(1f).fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          items(customBuffs, key = { it.id }) { item ->
            val iconBmp =
              remember(item.id, item.strokeColorHex, item.shortLabel) {
                RiftSpriteBaker.getBuffIconBitmap(item.buffTypeIndex).asImageBitmap()
              }
            Column(
              modifier =
                Modifier.fillMaxWidth()
                  .background(Color(0xCC081220), RoundedCornerShape(8.dp))
                  .border(
                    1.dp,
                    runCatching { Color(android.graphics.Color.parseColor(item.strokeColorHex)) }
                      .getOrDefault(Color(0xFFC084FC)),
                    RoundedCornerShape(8.dp),
                  )
                  .padding(8.dp),
              verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  Image(
                    bitmap = iconBmp,
                    contentDescription = item.officialName,
                    modifier = Modifier.size(28.dp),
                  )
                  Column {
                    Text(
                      text = "${item.officialName} (${item.shortLabel} [${item.chargesPerPickup}])",
                      color = Color.White,
                      fontSize = 11.sp,
                      fontWeight = FontWeight.ExtraBold,
                    )
                    Text(
                      text = item.summary.replace("⚡", " Flux"),
                      color = Color(0xFF94A3B8),
                      fontSize = 9.sp,
                    )
                  }
                }
              }

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                CalibrationToggleChip(
                  label = "✨ EQUIP NOW",
                  active = false,
                  activeColor = Color(0xFF34D399),
                  testTag = "forge_equip_${item.id}",
                  onClick = {
                    engine?.equipAndTestCustomForgedBuff(item)
                    onStatusMessage("✨ EQUIPPED [${item.officialName}] INTO 4TH SPECIAL BUFF SLOT!")
                  },
                )
                Spacer(modifier = Modifier.width(6.dp))
                CalibrationToggleChip(
                  label = "🗑 REMOVE",
                  active = false,
                  activeColor = Color(0xFFFF3B4E),
                  testTag = "forge_remove_${item.id}",
                  onClick = {
                    RiftCodex.removeCustomBuff(item.id)
                    RiftCalibrationState.applyCalibrationChangesLive(engine, context)
                    forgedListVersion++
                    onStatusMessage("🗑 REMOVED [${item.officialName}] FROM BUFF_REGISTRY.")
                  },
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
private fun ForgeMiniSlider(
  label: String,
  valueText: String,
  value: Float,
  min: Float,
  max: Float,
  onChange: (Float) -> Unit,
) {
  Column(modifier = Modifier.fillMaxWidth()) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Text(label, color = Color(0xFFCBD5E1), fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
      Text(valueText, color = Color(0xFF38BDF8), fontSize = 9.5.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace)
    }
    Slider(
      value = value.coerceIn(min, max),
      onValueChange = onChange,
      valueRange = min..max,
      colors =
        SliderDefaults.colors(
          thumbColor = Color(0xFFE879F9),
          activeTrackColor = Color(0xFFC084FC),
          inactiveTrackColor = Color(0xFF1E293B),
        ),
      modifier = Modifier.fillMaxWidth().height(22.dp),
    )
  }
}

@Composable
private fun CopyPromptFallbackDialog(
  promptText: String,
  modifiedCount: Int,
  onCopyAgain: () -> Unit,
  onClose: () -> Unit,
) {
  Box(
    modifier =
      Modifier.fillMaxSize()
        .background(Color(0xCC000000))
        .clickable { onClose() }
        .padding(24.dp),
    contentAlignment = Alignment.Center,
  ) {
    Surface(
      modifier =
        Modifier.fillMaxSize()
          .clickable(enabled = false) {}
          .border(1.2.dp, Color(0xFF10B981), RoundedCornerShape(14.dp)),
      color = Color(0xF5071220),
      shape = RoundedCornerShape(14.dp),
    ) {
      Column(
        modifier = Modifier.fillMaxSize().padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column {
            Text(
              text = "✅ AI STUDIO DEFAULT PROMPT READY ($modifiedCount SETTINGS MODIFIED)",
              color = Color(0xFF34D399),
              fontSize = 13.5.sp,
              fontWeight = FontWeight.ExtraBold,
              fontFamily = FontFamily.Monospace,
            )
            Text(
              text = "Copied to Android Clipboard! Paste directly into the AI Studio chat box to bake these values in permanently.",
              color = Color(0xFF94A3B8),
              fontSize = 10.sp,
            )
          }

          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CalibrationHeaderActionBtn(
              label = "📋 COPY AGAIN",
              bgColor = Color(0xFF092A22),
              borderColor = Color(0xFF10B981),
              textColor = Color(0xFF34D399),
              testTag = "copy_modal_copy_again_btn",
              onClick = onCopyAgain,
            )
            CalibrationHeaderActionBtn(
              label = "✕ DONE",
              bgColor = Color(0xFF2D1019),
              borderColor = Color(0xFFFF3B4E),
              textColor = Color(0xFFFF3B4E),
              testTag = "copy_modal_close_btn",
              onClick = onClose,
            )
          }
        }

        SelectionContainer(
          modifier =
            Modifier.weight(1f)
              .fillMaxWidth()
              .background(Color(0xFF040A12), RoundedCornerShape(8.dp))
              .border(1.dp, Color(0x4438BDF8), RoundedCornerShape(8.dp))
              .padding(10.dp)
              .verticalScroll(rememberScrollState()),
        ) {
          Text(
            text = promptText,
            color = Color(0xFFE2E8F0),
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
          )
        }
      }
    }
  }
}

@Composable
private fun StepButton(
  symbol: String,
  onClick: () -> Unit,
) {
  Box(
    modifier =
      Modifier.size(28.dp)
        .clip(RoundedCornerShape(6.dp))
        .background(Color(0xFF122338))
        .border(1.dp, Color(0x5538BDF8), RoundedCornerShape(6.dp))
        .clickable { onClick() },
    contentAlignment = Alignment.Center,
  ) {
    Text(
      text = symbol,
      color = Color(0xFF38BDF8),
      fontSize = 14.sp,
      fontWeight = FontWeight.ExtraBold,
      fontFamily = FontFamily.Monospace,
    )
  }
}

@Composable
private fun CalibrationHeaderActionBtn(
  label: String,
  bgColor: Color,
  borderColor: Color,
  textColor: Color,
  isCompactPhone: Boolean = false,
  testTag: String,
  onClick: () -> Unit,
) {
  Box(
    modifier =
      Modifier.height(if (isCompactPhone) 28.dp else 38.dp)
        .clip(RoundedCornerShape(8.dp))
        .background(
          Brush.verticalGradient(
            listOf(bgColor, bgColor.copy(alpha = 0.72f))
          )
        )
        .border(1.dp, borderColor, RoundedCornerShape(8.dp))
        .clickable { onClick() }
        .padding(horizontal = if (isCompactPhone) 8.dp else 10.dp)
        .testTag(testTag),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      text = label,
      color = textColor,
      fontSize = if (isCompactPhone) 9.2.sp else 10.sp,
      fontWeight = FontWeight.ExtraBold,
      fontFamily = FontFamily.SansSerif,
      maxLines = 1,
    )
  }
}

@Composable
private fun CalibrationToggleChip(
  label: String,
  active: Boolean,
  activeColor: Color,
  isCompactPhone: Boolean = false,
  testTag: String,
  onClick: () -> Unit,
) {
  Box(
    modifier =
      Modifier.height(if (isCompactPhone) 24.dp else 28.dp)
        .clip(RoundedCornerShape(7.dp))
        .background(
          if (active) {
            Brush.verticalGradient(
              listOf(activeColor.copy(alpha = 0.30f), activeColor.copy(alpha = 0.16f))
            )
          } else {
            Brush.verticalGradient(listOf(Color(0xFF102038), Color(0xFF091324)))
          }
        )
        .border(
          1.dp,
          if (active) activeColor else activeColor.copy(alpha = 0.45f),
          RoundedCornerShape(7.dp),
        )
        .clickable { onClick() }
        .padding(horizontal = if (isCompactPhone) 7.dp else 9.dp)
        .testTag(testTag),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      text = label,
      color = if (active) activeColor else Color(0xFFE2E8F0),
      fontSize = if (isCompactPhone) 8.8.sp else 9.5.sp,
      fontWeight = FontWeight.ExtraBold,
      fontFamily = FontFamily.Monospace,
      maxLines = 1,
    )
  }
}
