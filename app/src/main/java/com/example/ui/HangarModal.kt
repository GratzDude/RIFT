package com.example.ui

import android.view.KeyEvent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.CustomShipLoadoutSlot
import com.example.engine.RiftCalibrationState
import com.example.engine.RiftCodex
import com.example.engine.RiftSpriteBaker
import com.example.engine.ShipSpec
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

private val HangarElectricCyan = Color(0xFF38BDF8)
private val HangarDeltaPositive = Color(0xFF10B981)
private val HangarDeltaNegative = Color(0xFFEF4444)

/**
 * Anchors Hangar stat bars to the Universal Baseline Hull Floor:
 * 0 pts = 10% visual bar width, 30 pts = 100% visual bar width.
 */
private fun baselineAnchoredStatFraction(pts: Int): Float =
  0.10f + (pts.coerceIn(0, 30) / 30f) * 0.90f

private fun cleanShipDisplayTitle(ship: ShipSpec): String =
  if (ship.id == 10 && ship.name.equals("Rift Frame", ignoreCase = true)) {
    "RIFT FRAME"
  } else {
    ship.name.replace(Regex("^[0-9]+-"), "").uppercase(Locale.US)
  }

@Composable
fun HangarModal(
  initialCallsign: String,
  syndicateTag: String = "GUEST",
  syndicateName: String = "Independent Pilot",
  selectedShipId: Int,
  selectedMode: String,
  pilotLevel: Int = 1,
  xpProgressFraction: Float = 0f,
  customizationPointsTotal: Int = 1,
  spentCustomizationPoints: Int = 0,
  riftShards: Long = 0L,
  unlockFullNinthShipOverride: Boolean = false,
  activeCustomSlot: CustomShipLoadoutSlot = RiftCodex.activeCustomShipSlot,
  slot9BUnlocked: Boolean = false,
  slot9CUnlocked: Boolean = false,
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
  onDeployPilot: (callsign: String, shipId: Int, mode: String, asGuest: Boolean) -> Unit,
  onCloseModal: () -> Unit,
) {
  var callsignInput by remember(initialCallsign) { mutableStateOf(initialCallsign) }
  var currentShipId by remember(selectedShipId) { mutableIntStateOf(selectedShipId.coerceIn(1, 10)) }
  val currentMode = "FFA"
  var showPilotIdentityBar by remember { mutableStateOf(false) }

  // Watch customShip9Version so the 3x3 matrix and inspector re-render immediately when AI Hull Forge updates Rift Frame
  val spriteVersion = RiftSpriteBaker.customShip9Version
  val allNineShips = remember(activeCustomSlot, spriteVersion) { RiftCodex.getAllNineShips() }

  var showFullNinthShipForgeModal by remember { mutableStateOf(false) }
  var forgeStatusBanner by remember { mutableStateOf<String?>(null) }
  val hangarFocusRequester = remember { FocusRequester() }

  LaunchedEffect(showFullNinthShipForgeModal) {
    if (!showPilotIdentityBar) {
      runCatching { hangarFocusRequester.requestFocus() }
    }
  }

  BackHandler(enabled = showFullNinthShipForgeModal) {
    showFullNinthShipForgeModal = false
  }

  val inspectedShip =
    remember(currentShipId, allNineShips) {
      allNineShips.find { it.id == currentShipId } ?: allNineShips.first()
    }
  val equippedShip =
    remember(selectedShipId, allNineShips) {
      allNineShips.find { it.id == selectedShipId } ?: allNineShips.first()
    }

  val isNinthUnlocked = pilotLevel >= 20 || unlockFullNinthShipOverride
  val effectivePointBudget =
    if (unlockFullNinthShipOverride) 100 else pilotLevel.coerceIn(1, 100)
  val pointsAllocatedInSlot = activeCustomSlot.totalAllocatedPoints()
  val pointsRemainingInSlot = (effectivePointBudget - pointsAllocatedInSlot).coerceAtLeast(0)

  Box(
    modifier =
      Modifier.fillMaxSize()
        .background(Color(0xEB040812))
        .focusRequester(hangarFocusRequester)
        .focusable()
        .onPreviewKeyEvent { keyEvent ->
          val native = keyEvent.nativeKeyEvent
          if (native.action == KeyEvent.ACTION_DOWN && native.repeatCount == 0) {
            RiftCalibrationState.gamepadConnected = true
            when (native.keyCode) {
              KeyEvent.KEYCODE_BUTTON_B -> {
                if (showFullNinthShipForgeModal) {
                  showFullNinthShipForgeModal = false
                } else {
                  onCloseModal()
                }
                true
              }

              KeyEvent.KEYCODE_DPAD_LEFT -> {
                if (!showFullNinthShipForgeModal && !showPilotIdentityBar) {
                  currentShipId = if (currentShipId > 1) currentShipId - 1 else 10
                  true
                } else {
                  false
                }
              }

              KeyEvent.KEYCODE_DPAD_RIGHT -> {
                if (!showFullNinthShipForgeModal && !showPilotIdentityBar) {
                  currentShipId = if (currentShipId < 10) currentShipId + 1 else 1
                  true
                } else {
                  false
                }
              }

              KeyEvent.KEYCODE_DPAD_UP -> {
                if (!showFullNinthShipForgeModal && !showPilotIdentityBar) {
                  currentShipId =
                    when {
                      currentShipId == 10 -> 8
                      currentShipId in 4..9 -> currentShipId - 3
                      else -> 10
                    }
                  true
                } else {
                  false
                }
              }

              KeyEvent.KEYCODE_DPAD_DOWN -> {
                if (!showFullNinthShipForgeModal && !showPilotIdentityBar) {
                  currentShipId =
                    when {
                      currentShipId in 1..6 -> currentShipId + 3
                      currentShipId in 7..9 -> 10
                      else -> 2
                    }
                  true
                } else {
                  false
                }
              }

              KeyEvent.KEYCODE_BUTTON_A,
              KeyEvent.KEYCODE_DPAD_CENTER -> {
                if (!showFullNinthShipForgeModal && !showPilotIdentityBar) {
                  if (currentShipId == 10 && !isNinthUnlocked) {
                    showFullNinthShipForgeModal = true
                  } else {
                    val cleanCallsign =
                      com.example.data.UgcModeration.sanitizeIdentifier(
                        callsignInput.trim(),
                        "COMMANDER",
                      )
                    onDeployPilot(cleanCallsign, currentShipId, currentMode, false)
                  }
                  true
                } else {
                  false
                }
              }

              KeyEvent.KEYCODE_BUTTON_X -> {
                if (!showFullNinthShipForgeModal && currentShipId == 10) {
                  showFullNinthShipForgeModal = true
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
        .clickable(enabled = true, onClick = {})
        .padding(horizontal = 10.dp, vertical = 6.dp),
    contentAlignment = Alignment.Center,
  ) {
    Surface(
      modifier =
        Modifier.fillMaxWidth(0.99f)
          .fillMaxHeight(0.97f)
          .border(1.5.dp, Color(0xFF00F0FF), RoundedCornerShape(14.dp)),
      color = Color(0xFF070E1A),
      shape = RoundedCornerShape(14.dp),
    ) {
      Column(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp)) {
        // Compact Top Hangar Header Bar
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Text(
              text = "🚀 HANGAR // 3x3 SHIP MATRIX",
              color = Color(0xFF00F0FF),
              fontSize = 13.sp,
              fontWeight = FontWeight.Black,
              fontFamily = FontFamily.Monospace,
            )

            Box(
              modifier =
                Modifier.testTag("btn_toggle_pilot_identity")
                  .clip(RoundedCornerShape(6.dp))
                  .background(
                    if (showPilotIdentityBar) Color(0xFF1E293B) else Color(0xFF0D182A)
                  )
                  .border(1.dp, HangarElectricCyan, RoundedCornerShape(6.dp))
                  .clickable { showPilotIdentityBar = !showPilotIdentityBar }
                  .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
              Text(
                text = "🪪 [$syndicateTag] $callsignInput • PUBLIC FFA ▾",
                color = HangarElectricCyan,
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
              )
            }
          }

          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            SwirlingRiftHeaderPill(
              onClick = onOpenNexus,
              tag = "btn_hangar_nexus_ai",
              isStreaming = isNexusStreaming,
            )

            Column(
              modifier =
                Modifier.background(Color(0xFF0E1B2E), RoundedCornerShape(6.dp))
                  .border(1.dp, Color(0xFF00F0FF), RoundedCornerShape(6.dp))
                  .padding(horizontal = 7.dp, vertical = 2.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
              ) {
                Text(
                  text =
                    if (unlockFullNinthShipOverride) "LVL $pilotLevel (100 PT OVERRIDE)"
                    else "LVL $pilotLevel",
                  color = Color(0xFF00F0FF),
                  fontSize = 9.sp,
                  fontWeight = FontWeight.Black,
                  fontFamily = FontFamily.Monospace,
                )
                Text(
                  text = "$pointsRemainingInSlot/$effectivePointBudget PTS",
                  color = Color(0xFF22C55E),
                  fontSize = 8.5.sp,
                  fontWeight = FontWeight.Bold,
                  fontFamily = FontFamily.Monospace,
                )
              }
              Box(
                modifier =
                  Modifier.width(86.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFF1E293B))
              ) {
                Box(
                  modifier =
                    Modifier.fillMaxHeight()
                      .fillMaxWidth(xpProgressFraction.coerceIn(0.04f, 1f))
                      .background(Color(0xFF00F0FF))
                )
              }
            }

            Box(
              modifier =
                Modifier.testTag("btn_hangar_get_rift_shards")
                  .clip(RoundedCornerShape(6.dp))
                  .background(
                    Brush.horizontalGradient(
                      listOf(Color(0xFF3B0764), Color(0xFF6D28D9))
                    )
                  )
                  .border(1.dp, Color(0xFFC084FC), RoundedCornerShape(6.dp))
                  .clickable { onOpenRiftShardsStore() }
                  .padding(horizontal = 8.dp, vertical = 4.dp),
              contentAlignment = Alignment.Center,
            ) {
              Text(
                text = "❖ $riftShards [+ GET RIFT SHARDS]",
                color = Color(0xFFF3E8FF),
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
              )
            }

            Button(
              onClick = onCloseModal,
              colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
              contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
              modifier = Modifier.height(26.dp).testTag("btn_close_hangar"),
            ) {
              Text(
                "✕ RESUME",
                color = Color.White,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
              )
            }
          }
        }

        if (showPilotIdentityBar) {
          Spacer(modifier = Modifier.height(4.dp))
          Row(
            modifier =
              Modifier.fillMaxWidth()
                .background(Color(0xFF0B1526), RoundedCornerShape(8.dp))
                .border(1.dp, Color(0xFF1E3A5F), RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Text(
              text = "CALLSIGN:",
              color = Color(0xFF94A3B8),
              fontSize = 8.5.sp,
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.Monospace,
            )
            Box(
              modifier =
                Modifier.width(124.dp)
                  .background(Color(0xFF050A14), RoundedCornerShape(5.dp))
                  .border(1.dp, Color(0xFF334155), RoundedCornerShape(5.dp))
                  .padding(horizontal = 6.dp, vertical = 4.dp)
            ) {
              BasicTextField(
                value = callsignInput,
                onValueChange = {
                  if (it.length <= 14) {
                    callsignInput = it.uppercase(Locale.US)
                  }
                },
                singleLine = true,
                textStyle =
                  TextStyle(
                    color = Color.White,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                  ),
                cursorBrush = SolidColor(Color(0xFF00F0FF)),
                modifier = Modifier.fillMaxWidth().testTag("input_pilot_callsign"),
              )
            }

            Box(
              modifier =
                Modifier.testTag("btn_hangar_manage_syndicate")
                  .clip(RoundedCornerShape(5.dp))
                  .background(Color(0xFF1E1035))
                  .border(1.dp, Color(0xFFA855F7), RoundedCornerShape(5.dp))
                  .clickable { onOpenSyndicatesModal() }
                  .padding(horizontal = 7.dp, vertical = 4.dp)
            ) {
              Text(
                text = "🏛️ [$syndicateTag] SYNDICATE",
                color = Color(0xFFE9D5FF),
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
              )
            }

            Text(
              text = "ARENA MODE:",
              color = Color(0xFF94A3B8),
              fontSize = 8.5.sp,
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.Monospace,
            )
            Box(
              modifier =
                Modifier.testTag("mode_chip_FFA")
                  .clip(RoundedCornerShape(5.dp))
                  .background(Color(0xFF0284C7))
                  .border(1.dp, HangarElectricCyan, RoundedCornerShape(5.dp))
                  .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
              Text(
                text = "PUBLIC FFA (60M SECTOR CONVERGENCE)",
                color = Color.White,
                fontSize = 8.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(6.dp))

        if (showFullNinthShipForgeModal) {
          NinthShipModularForgePanel(
            spec = allNineShips.first { it.id == 10 },
            activeSlot = activeCustomSlot,
            slot9BUnlocked = slot9BUnlocked || unlockFullNinthShipOverride,
            slot9CUnlocked = slot9CUnlocked || unlockFullNinthShipOverride,
            pilotLevel = pilotLevel,
            unlockFullNinthShipOverride = unlockFullNinthShipOverride,
            effectivePointBudget = effectivePointBudget,
            pointsRemaining = pointsRemainingInSlot,
            riftShards = riftShards,
            statusBanner = forgeStatusBanner,
            onStatusBannerChange = { forgeStatusBanner = it },
            onSaveSlot = { updated ->
              onSaveCustomSlot(updated)
            },
            onSwitchSlot = { slotId ->
              onSwitchCustomSlot(slotId)
              forgeStatusBanner = "✓ SWITCHED ACTIVE LOADOUT TO ${slotId.uppercase(Locale.US)}"
            },
            onUnlockSlot = { slotId ->
              if (onUnlockCustomSlot(slotId)) {
                forgeStatusBanner = "✓ UNLOCKED ${slotId.uppercase(Locale.US)} (-300❖ RIFT SHARDS)!"
              } else {
                forgeStatusBanner = "⚠️ NEED 300❖ RIFT SHARDS TO UNLOCK ${slotId.uppercase(Locale.US)} (TAP [+ GET RIFT SHARDS])."
              }
            },
            onRespecSlot = {
              if (onRespecCustomSlot()) {
                forgeStatusBanner = "✓ RESPEC COMPLETE (-50❖): ALL ALLOCATED POINTS RESET TO 0!"
              } else {
                forgeStatusBanner = "⚠️ NEED 50❖ RIFT SHARDS & >0 ALLOCATED POINTS TO RESPEC."
              }
            },
            onForgeAiHull = { prompt, hullName ->
              val (_, msg) = onForgeAiHullDesign(prompt, hullName)
              forgeStatusBanner = msg
            },
            onToggleDevLevel20Or100 = {
              onToggleDevLevel20Or100()
              forgeStatusBanner =
                "⚡ DEV OVERRIDE TOGGLED: RIFT FRAME UNLOCKED WITH 100 CUSTOMIZATION POINTS!"
            },
            onDeployNinthShip = {
              if (isNinthUnlocked) {
                currentShipId = 10
                val finalCallsign =
                  com.example.data.UgcModeration.sanitizeIdentifier(
                    callsignInput.trim(),
                    "COMMANDER",
                  )
                onDeployPilot(finalCallsign, 10, currentMode, false)
              } else {
                forgeStatusBanner =
                  "🔒 UNLOCKS AT PILOT LEVEL 20 (OR TAP [DEV TEST: TOGGLE LVL 20 / LVL 100] ABOVE)."
              }
            },
            onBackToMatrix = { showFullNinthShipForgeModal = false },
          )
        } else {
          Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            // LEFT / MAIN: 3x3 Standard Ship Matrix (1..9) + Full-Width Ship #10 Rift Frame Modular Forge Bar
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
                    val shipIndex = rowIndex * 3 + colIndex
                    val ship = allNineShips[shipIndex]
                    val isSelected = currentShipId == ship.id
                    val isEquipped = selectedShipId == ship.id
                    val isNinthCell = ship.id == 10

                    MatrixShipComparisonCell(
                      modifier = Modifier.weight(1f).fillMaxHeight(),
                      ship = ship,
                      spriteVersion = spriteVersion,
                      isSelected = isSelected,
                      isEquipped = isEquipped,
                      isNinthCell = isNinthCell,
                      isNinthUnlocked = isNinthUnlocked,
                      activeSlotId = CustomShipLoadoutSlot.normalizeSlotId(activeCustomSlot.slotId),
                      pointsAllocated = pointsAllocatedInSlot,
                      pointBudget = effectivePointBudget,
                      onSelect = {
                        currentShipId = ship.id
                      },
                      onOpenNinthForge = {
                        currentShipId = 10
                        showFullNinthShipForgeModal = true
                      },
                    )
                  }
                }
              }

              // Ship #10: Full-Width Rift Frame Modular Forge Banner directly beneath the 3x3 standard ship matrix
              val tenthShip = allNineShips.first { it.id == 10 }
              val isTenthSelected = currentShipId == 10
              val isTenthEquipped = selectedShipId == 10
              val normalizedSlot = CustomShipLoadoutSlot.normalizeSlotId(activeCustomSlot.slotId)
              Row(
                modifier =
                  Modifier.fillMaxWidth()
                    .height(54.dp)
                    .testTag("hangar_ship_card_10")
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (isTenthSelected) Color(0xFF1B1033) else Color(0xFF140D26))
                    .border(
                      if (isTenthSelected || isTenthEquipped) 1.5.dp else 1.dp,
                      when {
                        isTenthSelected -> Color(0xFF00F0FF)
                        isTenthEquipped -> Color(0xFF22C55E)
                        else -> Color(0xFFC084FC).copy(alpha = 0.75f)
                      },
                      RoundedCornerShape(9.dp),
                    )
                    .clickable { currentShipId = 10 }
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                  modifier = Modifier.weight(0.45f),
                ) {
                  Box(
                    modifier =
                      Modifier.size(40.dp)
                        .background(Color(0xFF050A14), RoundedCornerShape(7.dp))
                        .border(1.dp, Color(0xFFC084FC).copy(alpha = 0.7f), RoundedCornerShape(7.dp)),
                    contentAlignment = Alignment.Center,
                  ) {
                    ShipSpritePreview(shipId = 10, sizeDp = 36)
                  }
                  Column {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                      Text(
                        text = "RIFT FRAME",
                        color = Color.White,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.SansSerif,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                      )
                      if (isTenthEquipped) {
                        Text(
                          text = "★",
                          color = Color(0xFF22C55E),
                          fontSize = 12.sp,
                          fontWeight = FontWeight.Black,
                        )
                      }
                    }
                    Text(
                      text =
                        if (isNinthUnlocked) {
                          "$normalizedSlot • $pointsAllocatedInSlot/$effectivePointBudget PTS"
                        } else {
                          "🔒 LVL 20 GATE • $normalizedSlot ($pointsAllocatedInSlot/$effectivePointBudget PTS)"
                        },
                      color = if (isNinthUnlocked) Color(0xFFC084FC) else Color(0xFFF59E0B),
                      fontSize = 10.5.sp,
                      fontWeight = FontWeight.Bold,
                      fontFamily = FontFamily.SansSerif,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                    )
                  }
                }

                // Compact 3-Bar (SPD / THR / FLX) Preview
                Column(
                  modifier = Modifier.weight(0.32f).padding(horizontal = 6.dp),
                  verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                  CardMatrixStatBar("SPD", tenthShip.speedPts, tenthShip.topSpeed.roundToInt().toString())
                  CardMatrixStatBar("THR", tenthShip.thrustPts, tenthShip.thrust.roundToInt().toString())
                  CardMatrixStatBar("FLX", tenthShip.maxFluxPts, tenthShip.maxFlux.roundToInt().toString())
                }

                // Direct [🛠️ FORGE & AI HULL] Button
                Box(
                  modifier =
                    Modifier.testTag("btn_matrix_open_9th_forge")
                      .clip(RoundedCornerShape(6.dp))
                      .background(Color(0xFF4C1D95))
                      .border(1.dp, Color(0xFFC084FC), RoundedCornerShape(6.dp))
                      .clickable {
                        currentShipId = 10
                        showFullNinthShipForgeModal = true
                      }
                      .padding(horizontal = 8.dp, vertical = 6.dp),
                  contentAlignment = Alignment.Center,
                ) {
                  Text(
                    text = "🛠️ FORGE & AI HULL",
                    color = Color.White,
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                  )
                }
              }
            }

            // RIGHT: High-Readability Inspector Panel with Monochrome Cyan Bars + Emerald/Crimson Comparison Deltas
            ShipComparisonInspectorPanel(
              modifier = Modifier.weight(0.38f).fillMaxHeight(),
              ship = inspectedShip,
              equippedShip = equippedShip,
              spriteVersion = spriteVersion,
              isCurrentShip = inspectedShip.id == selectedShipId,
              isNinthUnlocked = isNinthUnlocked,
              pilotLevel = pilotLevel,
              unlockFullNinthShipOverride = unlockFullNinthShipOverride,
              activeSlot = activeCustomSlot,
              pointsRemaining = pointsRemainingInSlot,
              effectivePointBudget = effectivePointBudget,
              onOpenNinthShipForge = { showFullNinthShipForgeModal = true },
              onToggleDevLevel20Or100 = onToggleDevLevel20Or100,
              onDeployShip = {
                if (inspectedShip.id == 10 && !isNinthUnlocked) {
                  showFullNinthShipForgeModal = true
                } else {
                  val finalCallsign = callsignInput.trim().ifEmpty { "COMMANDER" }
                  onDeployPilot(finalCallsign, inspectedShip.id, currentMode, false)
                }
              },
            )
          }
        }
      }
    }
  }
}

@Composable
private fun MatrixShipComparisonCell(
  modifier: Modifier = Modifier,
  ship: ShipSpec,
  spriteVersion: Int,
  isSelected: Boolean,
  isEquipped: Boolean,
  isNinthCell: Boolean,
  isNinthUnlocked: Boolean,
  activeSlotId: String,
  pointsAllocated: Int,
  pointBudget: Int,
  onSelect: () -> Unit,
  onOpenNinthForge: () -> Unit,
) {
  val accentColor =
    if (isNinthCell) {
      Color(0xFFC084FC)
    } else {
      Color(ship.accentColorHex)
    }

  val borderColor =
    when {
      isSelected -> Color(0xFF00F0FF)
      isEquipped -> Color(0xFF22C55E)
      isNinthCell -> Color(0xFFC084FC).copy(alpha = 0.7f)
      else -> Color(0xFF1E293B)
    }

  val bgColor =
    when {
      isSelected -> Color(0xFF11223D)
      isNinthCell -> Color(0xFF140D26)
      else -> Color(0xFF0A1322)
    }

  val cleanName = cleanShipDisplayTitle(ship)

  Column(
    modifier =
      modifier
        .testTag("hangar_ship_card_${ship.id}")
        .clip(RoundedCornerShape(9.dp))
        .background(bgColor)
        .border(if (isSelected || isEquipped) 1.5.dp else 1.dp, borderColor, RoundedCornerShape(9.dp))
        .clickable { onSelect() }
        .padding(horizontal = 8.dp, vertical = 6.dp),
    verticalArrangement = Arrangement.SpaceBetween,
  ) {
    // Top Row: Sprite + Enlarged Bold Name (15sp) + Role Subtitle (11sp)
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        modifier = Modifier.weight(1f),
      ) {
        val spriteBitmap =
          remember(ship.id, spriteVersion) {
            RiftSpriteBaker.getShipSprite(
                shipId = ship.id,
                isAllied = true,
                isHitFlashing = false,
              )
              .asImageBitmap()
          }
        Box(
          modifier =
            Modifier.size(34.dp)
              .background(Color(0xFF050A14), RoundedCornerShape(7.dp))
              .border(1.dp, accentColor.copy(alpha = 0.55f), RoundedCornerShape(7.dp)),
          contentAlignment = Alignment.Center,
        ) {
          Image(
            bitmap = spriteBitmap,
            contentDescription = cleanName,
            filterQuality = FilterQuality.High,
            modifier = Modifier.size(28.dp),
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
              fontSize = 15.sp,
              fontWeight = FontWeight.Black,
              fontFamily = FontFamily.SansSerif,
              lineHeight = 16.sp,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            if (isEquipped) {
              Text(
                text = "★",
                color = Color(0xFF22C55E),
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
              )
            }
          }
          Text(
            text =
              if (isNinthCell) {
                if (isNinthUnlocked) "${activeSlotId.uppercase(Locale.US)} • $pointsAllocated/$pointBudget PTS"
                else "🔒 LVL 20 GATE"
              } else {
                ship.role.uppercase(Locale.US)
              },
            color = if (isNinthCell && !isNinthUnlocked) Color(0xFFF59E0B) else accentColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.SansSerif,
            lineHeight = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }

      if (isNinthCell) {
        Box(
          modifier =
            Modifier.testTag("btn_matrix_open_9th_forge")
              .clip(RoundedCornerShape(5.dp))
              .background(Color(0xFF4C1D95))
              .border(1.dp, Color(0xFFC084FC), RoundedCornerShape(5.dp))
              .clickable { onOpenNinthForge() }
              .padding(horizontal = 6.dp, vertical = 3.dp)
        ) {
          Text(
            text = "🛠️ FORGE",
            color = Color.White,
            fontSize = 8.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
          )
        }
      }
    }

    // Decluttered 3-Bar Core Hull Preview (SPD / THR / FLX) — Monochrome Cyan, Anchored at Baseline Floor (0 pts = 10%)
    Column(
      modifier = Modifier.fillMaxWidth(),
      verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
      CardMatrixStatBar("SPD", ship.speedPts, ship.topSpeed.roundToInt().toString())
      CardMatrixStatBar("THR", ship.thrustPts, ship.thrust.roundToInt().toString())
      CardMatrixStatBar("FLX", ship.maxFluxPts, ship.maxFlux.roundToInt().toString())
    }
  }
}

@Composable
private fun CardMatrixStatBar(
  label: String,
  pts: Int,
  valueText: String,
) {
  val fraction = baselineAnchoredStatFraction(pts)
  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(5.dp),
  ) {
    Text(
      text = label,
      color = Color(0xFF94A3B8),
      fontSize = 8.5.sp,
      fontWeight = FontWeight.Bold,
      fontFamily = FontFamily.Monospace,
      modifier = Modifier.width(24.dp),
    )
    Box(
      modifier =
        Modifier.weight(1f)
          .height(5.dp)
          .clip(RoundedCornerShape(2.5.dp))
          .background(Color(0xFF1E293B))
    ) {
      Box(
        modifier =
          Modifier.fillMaxHeight()
            .fillMaxWidth(fraction)
            .background(HangarElectricCyan, RoundedCornerShape(2.5.dp))
      )
    }
    Text(
      text = valueText,
      color = Color(0xFFE2E8F0),
      fontSize = 8.5.sp,
      fontWeight = FontWeight.Bold,
      fontFamily = FontFamily.Monospace,
      modifier = Modifier.width(30.dp),
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
  val fraction = baselineAnchoredStatFraction(pts)
  val diff = value - equippedValue
  val hasMeaningfulDelta = showDelta && (if (isDecimal) abs(diff) >= 0.05f else abs(diff) >= 1f)

  val valueStr =
    if (isDecimal) String.format(Locale.US, "%.1f", value)
    else "${value.roundToInt()}"
  val deltaStr =
    if (!hasMeaningfulDelta) {
      ""
    } else if (isDecimal) {
      val sign = if (diff > 0f) "+" else ""
      " ($sign${String.format(Locale.US, "%.1f", diff)})"
    } else {
      val sign = if (diff > 0f) "+" else ""
      " ($sign${diff.roundToInt()})"
    }

  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(6.dp),
  ) {
    Text(
      text = label,
      color = Color(0xFFCBD5E1),
      fontSize = 13.sp,
      fontWeight = FontWeight.Bold,
      fontFamily = FontFamily.Monospace,
      modifier = Modifier.width(34.dp),
    )
    Box(
      modifier =
        Modifier.weight(1f)
          .height(6.dp)
          .clip(RoundedCornerShape(3.dp))
          .background(Color(0xFF1E293B))
    ) {
      Box(
        modifier =
          Modifier.fillMaxHeight()
            .fillMaxWidth(fraction)
            .background(HangarElectricCyan, RoundedCornerShape(3.dp))
      )
    }
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.End,
      modifier = Modifier.widthIn(min = 78.dp),
    ) {
      Text(
        text = valueStr,
        color = Color.White,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace,
      )
      if (deltaStr.isNotEmpty()) {
        Text(
          text = deltaStr,
          color = if (diff > 0f) HangarDeltaPositive else HangarDeltaNegative,
          fontSize = 12.sp,
          fontWeight = FontWeight.ExtraBold,
          fontFamily = FontFamily.Monospace,
        )
      }
    }
  }
}

@Composable
private fun ShipComparisonInspectorPanel(
  modifier: Modifier = Modifier,
  ship: ShipSpec,
  equippedShip: ShipSpec,
  spriteVersion: Int,
  isCurrentShip: Boolean,
  isNinthUnlocked: Boolean,
  pilotLevel: Int,
  unlockFullNinthShipOverride: Boolean,
  activeSlot: CustomShipLoadoutSlot,
  pointsRemaining: Int,
  effectivePointBudget: Int,
  onOpenNinthShipForge: () -> Unit,
  onToggleDevLevel20Or100: () -> Unit,
  onDeployShip: () -> Unit,
) {
  val isNinth = ship.id == 10
  val accentColor =
    if (isNinth) Color(0xFFC084FC)
    else Color(ship.accentColorHex)
  val cleanTitle = cleanShipDisplayTitle(ship)
  val normalizedSlotLabel = CustomShipLoadoutSlot.normalizeSlotId(activeSlot.slotId).uppercase(Locale.US)

  Column(
    modifier =
      modifier
        .background(Color(0xFF0B1526), RoundedCornerShape(10.dp))
        .border(1.dp, Color(0xFF1E3A5F), RoundedCornerShape(10.dp))
        .padding(10.dp),
    verticalArrangement = Arrangement.SpaceBetween,
  ) {
    Column(
      modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
      // Header + Sprite (18sp Ship Title)
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp),
      ) {
        val largeSprite =
          remember(ship.id, spriteVersion) {
            RiftSpriteBaker.getShipSprite(
                shipId = ship.id,
                isAllied = true,
                isHitFlashing = false,
              )
              .asImageBitmap()
          }
        Box(
          modifier =
            Modifier.size(50.dp)
              .background(Color(0xFF050A14), RoundedCornerShape(8.dp))
              .border(1.5.dp, accentColor, RoundedCornerShape(8.dp)),
          contentAlignment = Alignment.Center,
        ) {
          Image(
            bitmap = largeSprite,
            contentDescription = cleanTitle,
            filterQuality = FilterQuality.High,
            modifier = Modifier.size(42.dp),
          )
        }

        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = cleanTitle,
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.SansSerif,
            lineHeight = 20.sp,
          )
          Text(
            text = ship.role.uppercase(Locale.US),
            color = accentColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.SansSerif,
          )
        }
      }

      if (isNinth) {
        // Rift Frame Status & Quick Editor Entry Card
        Column(
          modifier =
            Modifier.fillMaxWidth()
              .background(Color(0xFF170F2E), RoundedCornerShape(8.dp))
              .border(1.dp, Color(0xFFC084FC), RoundedCornerShape(8.dp))
              .padding(8.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text =
                if (isNinthUnlocked) "✨ RIFT FRAME ($normalizedSlotLabel)"
                else "🔒 UNLOCKS AT PILOT LEVEL 20",
              color = if (isNinthUnlocked) Color(0xFF4ADE80) else Color(0xFFFBBF24),
              fontSize = 12.sp,
              fontWeight = FontWeight.Black,
              fontFamily = FontFamily.SansSerif,
            )
            Text(
              text = "${activeSlot.totalAllocatedPoints()}/$effectivePointBudget PTS",
              color = Color(0xFFC084FC),
              fontSize = 12.sp,
              fontWeight = FontWeight.Black,
              fontFamily = FontFamily.Monospace,
            )
          }

          Text(
            text =
              "Universal Baseline Hull Floor + Progressive Point Growth (Lv 20 = 20 Pts .. Lv 100 = 100 Pts). Full competitive parity with standard hulls.",
            color = Color(0xFFCBD5E1),
            fontSize = 12.5.sp,
            lineHeight = 16.sp,
          )

          Button(
            onClick = onOpenNinthShipForge,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6D28D9)),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 5.dp),
            modifier = Modifier.fillMaxWidth().height(34.dp).testTag("btn_open_ninth_ship_editor"),
          ) {
            Text(
              text = "🛠️ CUSTOMIZE RIFT FRAME & AI FORGE",
              color = Color.White,
              fontSize = 11.sp,
              fontWeight = FontWeight.Black,
              fontFamily = FontFamily.Monospace,
            )
          }

          Box(
            modifier =
              Modifier.fillMaxWidth()
                .testTag("btn_inspector_dev_toggle_9th")
                .clip(RoundedCornerShape(6.dp))
                .background(
                  if (unlockFullNinthShipOverride) Color(0xFF064E3B) else Color(0xFF1E293B)
                )
                .border(
                  1.dp,
                  if (unlockFullNinthShipOverride) Color(0xFF34D399) else Color(0xFF64748B),
                  RoundedCornerShape(6.dp),
                )
                .clickable { onToggleDevLevel20Or100() }
                .padding(vertical = 5.dp),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text =
                if (unlockFullNinthShipOverride) "⚡ DEV TEST: FULL 100 PTS ACTIVE (TAP TO OFF)"
                else "⚡ DEV TEST: TOGGLE LVL 20 / 100 PTS",
              color = if (unlockFullNinthShipOverride) Color(0xFF6EE7B7) else Color(0xFFE2E8F0),
              fontSize = 10.sp,
              fontWeight = FontWeight.Black,
              fontFamily = FontFamily.Monospace,
            )
          }
        }
      } else {
        Text(
          text = ship.tacticalLore,
          color = Color(0xFFCBD5E1),
          fontSize = 13.sp,
          lineHeight = 17.sp,
        )
      }

      // Primary & Class Ability Cards (13sp–14sp High-Contrast Readability)
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Column(
          modifier =
            Modifier.fillMaxWidth()
              .background(Color(0xFF070E1A), RoundedCornerShape(7.dp))
              .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(7.dp))
              .padding(horizontal = 8.dp, vertical = 6.dp),
          verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
          Text(
            text = "PRIMARY WEAPON (${ship.primaryPts} PTS)",
            color = HangarElectricCyan,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
          )
          Text(
            text = ship.primaryTitle,
            color = Color.White,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.SansSerif,
          )
          Text(
            text = "${ship.primaryDamage.toInt()} DMG • ${ship.primaryCost.toInt()}⚡ FLUX",
            color = Color(0xFF94A3B8),
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
          )
        }

        Column(
          modifier =
            Modifier.fillMaxWidth()
              .background(Color(0xFF070E1A), RoundedCornerShape(7.dp))
              .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(7.dp))
              .padding(horizontal = 8.dp, vertical = 6.dp),
          verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
          Text(
            text = "CLASS ABILITY (${ship.secondaryPts} PTS)",
            color = if (ship.hasBomb) Color(0xFFF59E0B) else Color(0xFF34D399),
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
          )
          Text(
            text = ship.secondaryTitle,
            color = Color.White,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.SansSerif,
          )
          Text(
            text =
              when {
                ship.bombType == "INTERDICTOR_DOME" ->
                  "600⚡ FLUX • 290PX TWO-WAY STASIS DOME (15.0S DURATION, 18.0S CD)"
                ship.bombType == "PHASE_DASH" -> "300⚡ • 2.5s CD • BLINK + I-FRAMES + 35% RAIL"
                ship.bombType == "CLOAK_EMP" -> "180⚡/s CLOAK • 0⚡ DECLOAK 360° EMP PULSE"
                ship.supportsNativeMultifire -> "INNATE SPLIT MULTI-SHOT (0⚡ EXTRA COST)"
                ship.hasBomb -> "${ship.bombDamage.toInt()} DMG • ${ship.bombCost.toInt()}⚡ FLUX"
                else -> "STARTING REPEL CHARGES: ${ship.initialRepels}"
              },
            color = Color(0xFF94A3B8),
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
          )
        }
      }

      // Detailed 5-Stat Readout Bars (Baseline-Anchored, Monochrome Cyan + Emerald/Crimson Deltas vs. Equipped Hull)
      Column(
        modifier =
          Modifier.fillMaxWidth()
            .background(Color(0xFF070E1A), RoundedCornerShape(8.dp))
            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(8.dp))
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
      ) {
        val showComparison = !isCurrentShip
        if (showComparison) {
          Text(
            text = "COMPARING VS EQUIPPED ${cleanShipDisplayTitle(equippedShip)}",
            color = Color(0xFF94A3B8),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
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

    Button(
      onClick = onDeployShip,
      colors =
        ButtonDefaults.buttonColors(
          containerColor =
            when {
              isNinth && !isNinthUnlocked -> Color(0xFF475569)
              isCurrentShip -> Color(0xFF16A34A)
              else -> Color(0xFF0284C7)
            }
        ),
      shape = RoundedCornerShape(8.dp),
      modifier = Modifier.fillMaxWidth().height(38.dp).testTag("btn_deploy_pilot"),
    ) {
      Text(
        text =
          when {
            isNinth && !isNinthUnlocked -> "🔒 LVL 20 GATE (TAP TO INSPECT FORGE)"
            isCurrentShip -> "⚡ RE-DEPLOY $cleanTitle"
            else -> "⚡ DEPLOY $cleanTitle"
          },
        color = Color.White,
        fontSize = 12.sp,
        fontWeight = FontWeight.Black,
        fontFamily = FontFamily.Monospace,
      )
    }
  }
}

@Composable
private fun NinthShipModularForgePanel(
  spec: ShipSpec,
  activeSlot: CustomShipLoadoutSlot,
  slot9BUnlocked: Boolean,
  slot9CUnlocked: Boolean,
  pilotLevel: Int,
  unlockFullNinthShipOverride: Boolean,
  effectivePointBudget: Int,
  pointsRemaining: Int,
  riftShards: Long,
  statusBanner: String?,
  onStatusBannerChange: (String?) -> Unit,
  onSaveSlot: (CustomShipLoadoutSlot) -> Unit,
  onSwitchSlot: (String) -> Unit,
  onUnlockSlot: (String) -> Unit,
  onRespecSlot: () -> Unit,
  onForgeAiHull: (String, String) -> Unit,
  onToggleDevLevel20Or100: () -> Unit,
  onDeployNinthShip: () -> Unit,
  onBackToMatrix: () -> Unit,
) {
  val normalizedCurrentSlot = CustomShipLoadoutSlot.normalizeSlotId(activeSlot.slotId)
  var hullNameInput by
    remember(normalizedCurrentSlot, activeSlot.visualRecipe.hullName) {
      mutableStateOf(activeSlot.visualRecipe.hullName)
    }
  var aiPromptInput by
    remember(normalizedCurrentSlot, activeSlot.visualRecipe.promptUsed) {
      mutableStateOf(activeSlot.visualRecipe.promptUsed)
    }

  val isUnlocked = pilotLevel >= 20 || unlockFullNinthShipOverride
  val spriteVersion = RiftSpriteBaker.customShip9Version

  fun adjustStatPoints(currentVal: Int, delta: Int, applyNewVal: (Int) -> CustomShipLoadoutSlot) {
    val maxAddition = pointsRemaining.coerceAtLeast(0)
    val clampedDelta =
      if (delta > 0) delta.coerceAtMost(maxAddition).coerceAtMost(30 - currentVal)
      else delta.coerceAtLeast(-currentVal)
    if (clampedDelta != 0) {
      val nextVal = (currentVal + clampedDelta).coerceIn(0, 30)
      onSaveSlot(applyNewVal(nextVal))
    }
  }

  Column(
    modifier = Modifier.fillMaxSize(),
    verticalArrangement = Arrangement.spacedBy(6.dp),
  ) {
    // Subheader Bar: Back to 3x3 Matrix + Saved Loadout Tabs (Frame A, Frame B, Frame C) + Respec (50❖) + Dev Test Toggle
    Row(
      modifier =
        Modifier.fillMaxWidth()
          .background(Color(0xFF0E172A), RoundedCornerShape(8.dp))
          .border(1.dp, Color(0xFF334155), RoundedCornerShape(8.dp))
          .padding(horizontal = 8.dp, vertical = 5.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Box(
          modifier =
            Modifier.testTag("btn_back_to_3x3_matrix")
              .clip(RoundedCornerShape(6.dp))
              .background(Color(0xFF1E293B))
              .border(1.dp, HangarElectricCyan, RoundedCornerShape(6.dp))
              .clickable { onBackToMatrix() }
              .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
          Text(
            text = "◀ 3x3 MATRIX",
            color = HangarElectricCyan,
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
          )
        }

        // Loadout Slot Tabs: Frame A (Free), Frame B (300❖), Frame C (300❖)
        val slots =
          listOf(
            Triple("Frame A", true, "FRAME A"),
            Triple("Frame B", slot9BUnlocked, if (slot9BUnlocked) "FRAME B" else "🔒 FRAME B (300❖)"),
            Triple("Frame C", slot9CUnlocked, if (slot9CUnlocked) "FRAME C" else "🔒 FRAME C (300❖)"),
          )
        slots.forEach { (slotId, unlocked, label) ->
          val isCurrentSlot = normalizedCurrentSlot == slotId
          val safeTag = slotId.replace(" ", "_")
          Box(
            modifier =
              Modifier.testTag("btn_custom_slot_$safeTag")
                .clip(RoundedCornerShape(6.dp))
                .background(
                  when {
                    isCurrentSlot -> Color(0xFF581C87)
                    unlocked -> Color(0xFF0F172A)
                    else -> Color(0xFF1C1917)
                  }
                )
                .border(
                  1.dp,
                  when {
                    isCurrentSlot -> Color(0xFFC084FC)
                    unlocked -> Color(0xFF475569)
                    else -> Color(0xFFF59E0B)
                  },
                  RoundedCornerShape(6.dp),
                )
                .clickable {
                  if (unlocked) onSwitchSlot(slotId) else onUnlockSlot(slotId)
                }
                .padding(horizontal = 8.dp, vertical = 4.dp)
          ) {
            Text(
              text = label,
              color =
                when {
                  isCurrentSlot -> Color.White
                  unlocked -> Color(0xFFCBD5E1)
                  else -> Color(0xFFFBBF24)
                },
              fontSize = 8.5.sp,
              fontWeight = FontWeight.Black,
              fontFamily = FontFamily.Monospace,
            )
          }
        }

        Box(
          modifier =
            Modifier.testTag("btn_hangar_respec_points")
              .clip(RoundedCornerShape(6.dp))
              .background(Color(0xFF31102F))
              .border(1.dp, Color(0xFFF43F5E), RoundedCornerShape(6.dp))
              .clickable { onRespecSlot() }
              .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
          Text(
            text = "🔄 RESPEC (50❖)",
            color = Color(0xFFFDA4AF),
            fontSize = 8.5.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
          )
        }
      }

      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Box(
          modifier =
            Modifier.testTag("btn_dev_toggle_lvl20_100")
              .clip(RoundedCornerShape(6.dp))
              .background(
                if (unlockFullNinthShipOverride) Color(0xFF065F46) else Color(0xFF1E293B)
              )
              .border(
                1.dp,
                if (unlockFullNinthShipOverride) Color(0xFF34D399) else HangarElectricCyan,
                RoundedCornerShape(6.dp),
              )
              .clickable { onToggleDevLevel20Or100() }
              .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
          Text(
            text =
              if (unlockFullNinthShipOverride) "⚡ DEV TEST: 100 PTS ON"
              else "⚡ DEV TEST: TOGGLE LVL 20 / LVL 100",
            color = if (unlockFullNinthShipOverride) Color(0xFFA7F3D0) else HangarElectricCyan,
            fontSize = 8.5.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
          )
        }

        Button(
          onClick = onDeployNinthShip,
          colors =
            ButtonDefaults.buttonColors(
              containerColor = if (isUnlocked) Color(0xFF16A34A) else Color(0xFF475569)
            ),
          contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
          modifier = Modifier.height(26.dp).testTag("btn_deploy_9th_ship"),
        ) {
          Text(
            text = if (isUnlocked) "⚡ DEPLOY RIFT FRAME" else "🔒 LVL 20 GATE",
            color = Color.White,
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
          )
        }
      }
    }

    if (statusBanner != null) {
      Row(
        modifier =
          Modifier.fillMaxWidth()
            .background(Color(0xFF0D2137), RoundedCornerShape(6.dp))
            .border(1.dp, Color(0xFF00F0FF), RoundedCornerShape(6.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = statusBanner,
          color = Color(0xFF00F0FF),
          fontSize = 9.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.Monospace,
        )
        Text(
          text = "✕",
          color = Color(0xFF94A3B8),
          fontSize = 9.sp,
          modifier = Modifier.clickable { onStatusBannerChange(null) },
        )
      }
    }

    Row(
      modifier = Modifier.fillMaxSize(),
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      // LEFT COLUMN: 5 Core Stat Sliders (0..30 Pts, Monochrome Cyan) + Primary Weapon Tier + Class Ability Slot
      Column(
        modifier =
          Modifier.weight(0.54f)
            .fillMaxHeight()
            .background(Color(0xFF0A1322), RoundedCornerShape(10.dp))
            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(10.dp))
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
            text = "⚙️ RIFT FRAME MATRIX (UNIVERSAL FLOOR + 0..30/STAT)",
            color = Color(0xFF00F0FF),
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
          )
          Text(
            text =
              "FREE: $pointsRemaining / $effectivePointBudget PTS (USED: ${activeSlot.totalAllocatedPoints()})",
            color = if (pointsRemaining > 0) Color(0xFF4ADE80) else Color(0xFFFACC15),
            fontSize = 8.5.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
          )
        }

        AttributePointRow(
          label = "TOP SPEED",
          pts = activeSlot.speedPts,
          statReadout = "${spec.topSpeed.toInt()} PX/S (380 + 8/pt)",
          tagPrefix = "stat_speed",
          onDelta = { d ->
            adjustStatPoints(activeSlot.speedPts, d) { activeSlot.copy(speedPts = it) }
          },
        )
        AttributePointRow(
          label = "THRUST",
          pts = activeSlot.thrustPts,
          statReadout = "${spec.thrust.toInt()} N/S² (600 + 15/pt)",
          tagPrefix = "stat_thrust",
          onDelta = { d ->
            adjustStatPoints(activeSlot.thrustPts, d) { activeSlot.copy(thrustPts = it) }
          },
        )
        AttributePointRow(
          label = "TURN RATE",
          pts = activeSlot.turnPts,
          statReadout = "${(spec.turnRate * 10f).roundToInt() / 10f} RAD/S (3.2 + 0.1/pt)",
          tagPrefix = "stat_turn",
          onDelta = { d ->
            adjustStatPoints(activeSlot.turnPts, d) { activeSlot.copy(turnPts = it) }
          },
        )
        AttributePointRow(
          label = "MAX FLUX",
          pts = activeSlot.maxFluxPts,
          statReadout = "${spec.maxFlux.toInt()} ⚡ (2200 + 80/pt)",
          tagPrefix = "stat_flux",
          onDelta = { d ->
            adjustStatPoints(activeSlot.maxFluxPts, d) { activeSlot.copy(maxFluxPts = it) }
          },
        )
        AttributePointRow(
          label = "FLUX RECHARGE",
          pts = activeSlot.rechargePts,
          statReadout = "${spec.rechargeRate.toInt()} ⚡/S (240 + 8/pt)",
          tagPrefix = "stat_recharge",
          onDelta = { d ->
            adjustStatPoints(activeSlot.rechargePts, d) { activeSlot.copy(rechargePts = it) }
          },
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
          text = "🔫 PRIMARY WEAPON TIER (${activeSlot.primaryPts} PTS INVESTED)",
          color = HangarElectricCyan,
          fontSize = 9.sp,
          fontWeight = FontWeight.Black,
          fontFamily = FontFamily.Monospace,
        )
        val primaries = CustomShipLoadoutSlot.PRIMARY_OPTIONS
        for (rowIdx in 0 until 3) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
          ) {
            for (colIdx in 0 until 3) {
              val opt = primaries[rowIdx * 3 + colIdx]
              val key = opt.id
              val cost = opt.pointCost
              val label = "${opt.title} (${cost}P)"
              val selected = activeSlot.primaryArchetype == key
              val candidateSlot = activeSlot.copy(primaryArchetype = key)
              val canAfford = candidateSlot.totalAllocatedPoints() <= effectivePointBudget
              Box(
                modifier =
                  Modifier.weight(1f)
                    .testTag("btn_primary_arch_$key")
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                      when {
                        selected -> Color(0xFF0284C7)
                        canAfford -> Color(0xFF0F172A)
                        else -> Color(0xFF141824)
                      }
                    )
                    .border(
                      1.dp,
                      when {
                        selected -> HangarElectricCyan
                        canAfford -> Color(0xFF334155)
                        else -> Color(0xFF1E293B)
                      },
                      RoundedCornerShape(6.dp),
                    )
                    .clickable {
                      if (canAfford || selected) {
                        onSaveSlot(candidateSlot)
                      } else {
                        onStatusBannerChange(
                          "⚠️ NEED $cost PTS FOR ${opt.title} (FREE SOME STAT POINTS OR LEVEL UP)."
                        )
                      }
                    }
                    .padding(vertical = 5.dp, horizontal = 4.dp),
                contentAlignment = Alignment.Center,
              ) {
                Text(
                  text = label,
                  color = if (selected) Color.White else if (canAfford) Color(0xFFCBD5E1) else Color(0xFF64748B),
                  fontSize = 7.5.sp,
                  fontWeight = FontWeight.Black,
                  fontFamily = FontFamily.Monospace,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
            }
          }
        }

        Text(
          text =
            "💣 CLASS / SECONDARY SLOT (${activeSlot.secondaryPts} PTS • 4TH BUFF SLOT STAYS SEPARATE)",
          color = Color(0xFFF59E0B),
          fontSize = 9.sp,
          fontWeight = FontWeight.Black,
          fontFamily = FontFamily.Monospace,
        )
        val secondaries = CustomShipLoadoutSlot.SECONDARY_OPTIONS
        secondaries.chunked(3).forEach { rowOptions ->
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
          ) {
            rowOptions.forEach { secOpt ->
              val secKey = secOpt.id
              val secCost = secOpt.pointCost
              val secLabel = "${secOpt.title} (${secCost}P)"
              val isSelected = activeSlot.secondaryType == secKey
              val candidateSlot = activeSlot.copy(secondaryType = secKey)
              val canAfford = candidateSlot.totalAllocatedPoints() <= effectivePointBudget
              Box(
                modifier =
                  Modifier.weight(1f)
                    .testTag("btn_secondary_type_$secKey")
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                      when {
                        isSelected -> Color(0xFFB45309)
                        canAfford -> Color(0xFF0F172A)
                        else -> Color(0xFF141824)
                      }
                    )
                    .border(
                      1.dp,
                      when {
                        isSelected -> Color(0xFFFBBF24)
                        canAfford -> Color(0xFF334155)
                        else -> Color(0xFF1E293B)
                      },
                      RoundedCornerShape(6.dp),
                    )
                    .clickable {
                      if (canAfford || isSelected) {
                        onSaveSlot(candidateSlot)
                      } else {
                        onStatusBannerChange(
                          "⚠️ NEED $secCost PTS FOR ${secOpt.title} (FREE SOME STAT POINTS OR LEVEL UP)."
                        )
                      }
                    }
                    .padding(vertical = 5.dp, horizontal = 4.dp),
                contentAlignment = Alignment.Center,
              ) {
                Text(
                  text = secLabel,
                  color = if (isSelected) Color.White else if (canAfford) Color(0xFFCBD5E1) else Color(0xFF64748B),
                  fontSize = 7.5.sp,
                  fontWeight = FontWeight.Black,
                  fontFamily = FontFamily.Monospace,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
            }
          }
        }
      }

      // RIGHT COLUMN: AI Hull Forge (<2KB JSON Visual Recipe + Live 3D Preview)
      Column(
        modifier =
          Modifier.weight(0.46f)
            .fillMaxHeight()
            .background(Color(0xFF100B20), RoundedCornerShape(10.dp))
            .border(1.dp, Color(0xFF6D28D9), RoundedCornerShape(10.dp))
            .padding(9.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        val recipe = activeSlot.visualRecipe
        val recipeBytes = remember(recipe) { recipe.toJsonString().toByteArray().size }
        val isFirstFree = activeSlot.aiGenerationsCount == 0

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = "🎨 AI HULL FORGE (PROCEDURAL 3D SHADER)",
            color = Color(0xFFC084FC),
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
          )
          Text(
            text = "RECIPE: ${recipeBytes}B (<2KB)",
            color = Color(0xFF4ADE80),
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
          )
        }

        Row(
          modifier =
            Modifier.fillMaxWidth()
              .background(Color(0xFF06040D), RoundedCornerShape(8.dp))
              .border(1.dp, Color(0xFF4C1D95), RoundedCornerShape(8.dp))
              .padding(8.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          val alliedBmp =
            remember(spriteVersion, recipe) {
              RiftSpriteBaker.getShipSprite(10, isAllied = true, isHitFlashing = false)
                .asImageBitmap()
            }
          val hostileBmp =
            remember(spriteVersion, recipe) {
              RiftSpriteBaker.getShipSprite(10, isAllied = false, isHitFlashing = false)
                .asImageBitmap()
            }

          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
              modifier =
                Modifier.size(56.dp)
                  .background(Color.Black, RoundedCornerShape(8.dp))
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
            Text(
              text = "ALLIED (36x36)",
              color = HangarElectricCyan,
              fontSize = 7.sp,
              fontFamily = FontFamily.Monospace,
            )
          }

          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
              modifier =
                Modifier.size(56.dp)
                  .background(Color.Black, RoundedCornerShape(8.dp))
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
            Text(
              text = "HOSTILE (36x36)",
              color = Color(0xFFEF4444),
              fontSize = 7.sp,
              fontFamily = FontFamily.Monospace,
            )
          }

          Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
          ) {
            Text(
              text = "SILHOUETTE: ${recipe.silhouette}",
              color = Color.White,
              fontSize = 8.5.sp,
              fontWeight = FontWeight.Black,
              fontFamily = FontFamily.Monospace,
            )
            Text(
              text =
                "SWEEP: ${recipe.wingSweep} • PLATES: ${recipe.chassisPlates} • ENG: ${recipe.engineStyle}",
              color = Color(0xFFCBD5E1),
              fontSize = 7.5.sp,
              fontFamily = FontFamily.Monospace,
            )
            Text(
              text =
                "PALETTE: ${recipe.primaryHex} / ${recipe.trimHex} / ${recipe.coreGlowHex}",
              color = Color(0xFFC084FC),
              fontSize = 7.5.sp,
              fontFamily = FontFamily.Monospace,
            )
            Text(
              text = "✓ GUARDRAILS: 36PX HITBOX • L>=0.32 CONTRAST • TEAM RING",
              color = Color(0xFF4ADE80),
              fontSize = 7.sp,
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.Monospace,
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
            fontSize = 8.5.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
          )
          Box(
            modifier =
              Modifier.weight(1f)
                .background(Color(0xFF06040D), RoundedCornerShape(6.dp))
                .border(1.dp, Color(0xFF4C1D95), RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp, vertical = 5.dp)
          ) {
            BasicTextField(
              value = hullNameInput,
              onValueChange = { if (it.length <= 18) hullNameInput = it },
              singleLine = true,
              textStyle =
                TextStyle(
                  color = Color.White,
                  fontSize = 9.5.sp,
                  fontWeight = FontWeight.Bold,
                  fontFamily = FontFamily.Monospace,
                ),
              cursorBrush = SolidColor(Color(0xFFC084FC)),
              modifier = Modifier.fillMaxWidth().testTag("input_custom_hull_name"),
            )
          }
        }

        Text(
          text = "DESCRIBE YOUR CUSTOM SHIP HULL:",
          color = Color(0xFFCBD5E1),
          fontSize = 8.5.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.Monospace,
        )
        Box(
          modifier =
            Modifier.fillMaxWidth()
              .height(44.dp)
              .background(Color(0xFF06040D), RoundedCornerShape(6.dp))
              .border(1.dp, Color(0xFF6D28D9), RoundedCornerShape(6.dp))
              .padding(horizontal = 8.dp, vertical = 5.dp)
        ) {
          if (aiPromptInput.isEmpty()) {
            Text(
              text = "Describe your custom ship hull (e.g., 'crimson bio-manta stealth scythe with gold plasma core')...",
              color = Color(0xFF64748B),
              fontSize = 8.sp,
              fontFamily = FontFamily.Monospace,
            )
          }
          BasicTextField(
            value = aiPromptInput,
            onValueChange = { if (it.length <= 140) aiPromptInput = it },
            textStyle =
              TextStyle(
                color = Color.White,
                fontSize = 8.5.sp,
                fontFamily = FontFamily.Monospace,
              ),
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
                  .clip(RoundedCornerShape(5.dp))
                  .background(Color(0xFF1E1035))
                  .border(1.dp, Color(0xFF7C3AED), RoundedCornerShape(5.dp))
                  .clickable {
                    hullNameInput = chipTitle.take(18).uppercase(Locale.US)
                    aiPromptInput = chipPrompt
                  }
                  .padding(horizontal = 6.dp, vertical = 3.dp)
            ) {
              Text(
                text = "✨ $chipTitle",
                color = Color(0xFFE9D5FF),
                fontSize = 7.5.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
              )
            }
          }
        }

        Button(
          onClick = { onForgeAiHull(aiPromptInput, hullNameInput) },
          colors =
            ButtonDefaults.buttonColors(
              containerColor = Color(0xFF7C3AED)
            ),
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier.fillMaxWidth().height(34.dp).testTag("btn_forge_ai_ship_design"),
        ) {
          Text(
            text =
              if (isFirstFree) "🎨 FORGE AI SHIP DESIGN (1ST DESIGN FREE!)"
              else "🎨 FORGE AI SHIP DESIGN (150❖ RIFT SHARDS)",
            color = Color.White,
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
          )
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
        .background(Color(0xFF060C17), RoundedCornerShape(6.dp))
        .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(6.dp))
        .padding(horizontal = 6.dp, vertical = 4.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Column(modifier = Modifier.width(126.dp)) {
      Text(
        text = label,
        color = Color.White,
        fontSize = 9.sp,
        fontWeight = FontWeight.Black,
        fontFamily = FontFamily.Monospace,
      )
      Text(
        text = statReadout,
        color = HangarElectricCyan,
        fontSize = 7.8.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace,
      )
    }

    // Baseline-Anchored 0..30 Progress Bar (0 pts = 10% visual width .. 30 pts = 100%)
    Box(
      modifier =
        Modifier.weight(1f)
          .padding(horizontal = 6.dp)
          .height(6.dp)
          .clip(RoundedCornerShape(3.dp))
          .background(Color(0xFF1E293B))
    ) {
      Box(
        modifier =
          Modifier.fillMaxHeight()
            .fillMaxWidth(baselineAnchoredStatFraction(pts))
            .background(HangarElectricCyan, RoundedCornerShape(3.dp))
      )
    }

    Text(
      text = "$pts/30",
      color = Color.White,
      fontSize = 8.5.sp,
      fontWeight = FontWeight.Black,
      fontFamily = FontFamily.Monospace,
      modifier = Modifier.width(34.dp),
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
        .background(Color(0xFF1E293B))
        .border(1.dp, Color(0xFF475569), RoundedCornerShape(4.dp))
        .clickable { onClick() }
        .padding(horizontal = 5.dp, vertical = 2.dp),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      text = label,
      color = Color.White,
      fontSize = 8.sp,
      fontWeight = FontWeight.Black,
      fontFamily = FontFamily.Monospace,
    )
  }
}

@Composable
fun ShipSpritePreview(
  shipId: Int,
  sizeDp: Int = 32,
  isAllied: Boolean = true,
) {
  val customVer = RiftSpriteBaker.customShip9Version
  val shipName = remember(shipId) { RiftCodex.getShipById(shipId.coerceIn(1, 10)).name }
  val bmp =
    remember(shipId, isAllied, customVer) {
      RiftSpriteBaker.getShipSprite(
          shipId = shipId.coerceIn(1, 10),
          isAllied = isAllied,
          isHitFlashing = false,
        )
        .asImageBitmap()
    }
  Image(
    bitmap = bmp,
    contentDescription = shipName,
    filterQuality = FilterQuality.High,
    modifier = Modifier.size(sizeDp.dp),
  )
}
