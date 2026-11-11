package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.LabelPlacement
import com.example.engine.RiftCalibrationState
import com.example.engine.RiftGameEngine
import com.example.engine.RiftSpriteBaker
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Full-screen live calibration & test screen with side-by-side visual previews
 * dynamically scaled in real-time beneath/beside each slider.
 */
@Composable
fun RiftCalibrationDialog(
  engine: RiftGameEngine? = null,
  onDismiss: () -> Unit,
) {
  val context = LocalContext.current
  Box(
    modifier =
      Modifier.fillMaxSize()
        .background(Color(0xF2050B16)) // Dark translucent glass, arena faintly visible behind
        .padding(horizontal = 16.dp, vertical = 10.dp)
        .testTag("full_screen_calibration_panel"),
  ) {
    Column(
      modifier = Modifier.fillMaxSize(),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      // 1. Top Header Bar with status, quick toggles, and close button
      Row(
        modifier =
          Modifier.fillMaxWidth()
            .height(42.dp)
            .background(Color(0x660B192C), RoundedCornerShape(10.dp))
            .border(1.dp, Color(0x6638BDF8), RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Box(
            modifier =
              Modifier.size(24.dp)
                .clip(CircleShape)
                .background(Color(0x3338BDF8))
                .border(1.dp, Color(0xFF38BDF8), CircleShape),
            contentAlignment = Alignment.Center,
          ) {
            Text("⚙", color = Color(0xFF38BDF8), fontSize = 12.sp)
          }
          Text(
            text = "RIFT LIVE CALIBRATION // FULL-SCREEN SIZE TUNER",
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = FontFamily.SansSerif,
          )
        }

        // Toggles & Actions
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
          // Lock / Unlock Direct Dragging Switch
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Text(
              text = if (RiftCalibrationState.isUiUnlockedForDragging) "🔓 DRAG MODE" else "🔒 UI LOCKED",
              color = if (RiftCalibrationState.isUiUnlockedForDragging) Color(0xFFFACC15) else Color(0xFF38BDF8),
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.Monospace,
            )
            Switch(
              checked = RiftCalibrationState.isUiUnlockedForDragging,
              onCheckedChange = { RiftCalibrationState.isUiUnlockedForDragging = it },
              colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFFFACC15),
                checkedTrackColor = Color(0x88FACC15),
                uncheckedThumbColor = Color(0xFF94A3B8),
                uncheckedTrackColor = Color(0x33475569),
              ),
              modifier = Modifier.height(24.dp),
            )
          }

          // Force All Buttons Switch
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Text(
              text = "ALL BUTTONS",
              color = if (RiftCalibrationState.forceAllButtonsVisible) Color(0xFF38BDF8) else Color(0xFF64748B),
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.Monospace,
            )
            Switch(
              checked = RiftCalibrationState.forceAllButtonsVisible,
              onCheckedChange = { RiftCalibrationState.forceAllButtonsVisible = it },
              colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFF38BDF8),
                checkedTrackColor = Color(0x8838BDF8),
                uncheckedThumbColor = Color(0xFF94A3B8),
                uncheckedTrackColor = Color(0x33475569),
              ),
              modifier = Modifier.height(24.dp),
            )
          }

          // Full 9th Ship (100 Pts + All Slots) Override Switch
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.testTag("calibration_toggle_full_ninth_ship"),
          ) {
            Text(
              text = "🛸 FULL 9TH SHIP (100 PTS)",
              color = if (RiftCalibrationState.unlockFullNinthShipOverride) Color(0xFF00E676) else Color(0xFF94A3B8),
              fontSize = 10.sp,
              fontWeight = FontWeight.ExtraBold,
              fontFamily = FontFamily.Monospace,
            )
            Switch(
              checked = RiftCalibrationState.unlockFullNinthShipOverride,
              onCheckedChange = {
                RiftCalibrationState.unlockFullNinthShipOverride = it
                RiftCalibrationState.savePersistedLayout(context)
                engine?.syncLocalPlayerCustomShipSpec()
              },
              colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFF00E676),
                checkedTrackColor = Color(0x8800E676),
                uncheckedThumbColor = Color(0xFF94A3B8),
                uncheckedTrackColor = Color(0x33475569),
              ),
              modifier = Modifier.height(24.dp),
            )
          }

          // Quick Force Convergence / New FFA Map Button in Header
          Box(
            modifier =
              Modifier.height(28.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0x440284C7))
                .border(1.dp, Color(0xFF38BDF8), RoundedCornerShape(6.dp))
                .clickable {
                  engine?.concludePublicFfaMatchCycle(forceSimulateWinWithPresence = false)
                }
                .padding(horizontal = 8.dp)
                .testTag("calibration_btn_force_convergence_top"),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text = "🌀 FORCE CONVERGENCE",
              color = Color(0xFFE0F2FE),
              fontSize = 9.sp,
              fontWeight = FontWeight.ExtraBold,
              fontFamily = FontFamily.Monospace,
            )
          }

          // Reset to 1.0x button
          Box(
            modifier =
              Modifier.height(28.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0x33475569))
                .border(0.8.dp, Color(0x8894A3B8), RoundedCornerShape(6.dp))
                .clickable { RiftCalibrationState.resetAll(context) }
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center,
          ) {
            Text("RESET 1.0x", color = Color(0xFFE2E8F0), fontSize = 9.sp, fontWeight = FontWeight.Bold)
          }

          // Close button
          Box(
            modifier =
              Modifier.size(28.dp)
                .clip(CircleShape)
                .background(Color(0x33EF4444))
                .border(1.dp, Color(0x88EF4444), CircleShape)
                .clickable { onDismiss() },
            contentAlignment = Alignment.Center,
          ) {
            Text("✕", color = Color(0xFFF87171), fontSize = 13.sp, fontWeight = FontWeight.Bold)
          }
        }
      }

      // 2. Main Two-Column Scrollable Calibration Content
      Row(
        modifier = Modifier.weight(1f).fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        // Left Column: In-World Entities (Camera, Ships, Anomalies, Crates)
        Column(
          modifier =
            Modifier.weight(1f)
              .fillMaxHeight()
              .verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          SectionHeader(title = "PLAYFIELD & IN-WORLD ENTITIES", color = Color(0xFF38BDF8))

          // 0a. Full 9th Custom Modular Ship Override Card
          Row(
            modifier =
              Modifier.fillMaxWidth()
                .background(
                  if (RiftCalibrationState.unlockFullNinthShipOverride) Color(0x55062C1E) else Color(0x550B1626),
                  RoundedCornerShape(10.dp),
                )
                .border(
                  1.2.dp,
                  if (RiftCalibrationState.unlockFullNinthShipOverride) Color(0xFF00E676) else Color(0x5538BDF8),
                  RoundedCornerShape(10.dp),
                )
                .clickable {
                  RiftCalibrationState.unlockFullNinthShipOverride = !RiftCalibrationState.unlockFullNinthShipOverride
                  RiftCalibrationState.savePersistedLayout(context)
                  engine?.syncLocalPlayerCustomShipSpec()
                }
                .padding(horizontal = 10.dp, vertical = 8.dp)
                .testTag("calibration_card_full_ninth_ship"),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "🛸 Unlock Full 9th Custom Modular Ship (100 Pts + Slots 9A/9B/9C)",
                color = if (RiftCalibrationState.unlockFullNinthShipOverride) Color(0xFF00E676) else Color.White,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.SansSerif,
              )
              Text(
                text = "Bypasses Level 20 gate & grants all 100 Customization Points + Slots 9A/9B/9C for immediate testing",
                color = Color(0xFF94A3B8),
                fontSize = 8.5.sp,
                fontFamily = FontFamily.SansSerif,
              )
            }
            Switch(
              checked = RiftCalibrationState.unlockFullNinthShipOverride,
              onCheckedChange = {
                RiftCalibrationState.unlockFullNinthShipOverride = it
                RiftCalibrationState.savePersistedLayout(context)
                engine?.syncLocalPlayerCustomShipSpec()
              },
              colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFF00E676),
                checkedTrackColor = Color(0x8800E676),
                uncheckedThumbColor = Color(0xFF94A3B8),
                uncheckedTrackColor = Color(0x33475569),
              ),
              modifier = Modifier.height(24.dp),
            )
          }

          // 0b. Sector Convergence & New Procedural FFA Match Control Card
          val matchSec = engine?.publicMatchDurationSec ?: 0f
          val matchMins = (matchSec / 60f).toInt()
          val matchRemSecs = (matchSec % 60f).toInt()
          val presencePct = ((engine?.getMatchPresenceRatio() ?: 1f) * 100f).toInt().coerceIn(0, 100)
          Column(
            modifier =
              Modifier.fillMaxWidth()
                .background(Color(0x550D1E36), RoundedCornerShape(10.dp))
                .border(1.2.dp, Color(0xFF38BDF8), RoundedCornerShape(10.dp))
                .padding(horizontal = 10.dp, vertical = 8.dp)
                .testTag("calibration_card_sector_convergence"),
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "🌀 Sector Convergence // Procedural New FFA Match Reset",
                  color = Color(0xFF38BDF8),
                  fontSize = 11.5.sp,
                  fontWeight = FontWeight.ExtraBold,
                  fontFamily = FontFamily.SansSerif,
                )
                Text(
                  text = "At 60:00 (or manual trigger), #1 Bounty (>50% presence) wins +100❖ & arena resets into a new procedural map",
                  color = Color(0xFF94A3B8),
                  fontSize = 8.sp,
                  fontFamily = FontFamily.SansSerif,
                )
              }
              Text(
                text = String.format(Locale.US, "%02d:%02d / 60:00 (%d%% PRESENCE)", matchMins, matchRemSecs, presencePct),
                color = Color(0xFFFACC15),
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
              )
            }
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Box(
                modifier =
                  Modifier.weight(1f)
                    .height(28.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF0284C7))
                    .border(1.dp, Color(0xFF38BDF8), RoundedCornerShape(6.dp))
                    .clickable {
                      engine?.concludePublicFfaMatchCycle(forceSimulateWinWithPresence = false)
                    }
                    .testTag("btn_force_convergence_new_map"),
                contentAlignment = Alignment.Center,
              ) {
                Text(
                  text = "🌀 FORCE CONVERGENCE / NEW FFA MATCH",
                  color = Color.White,
                  fontSize = 8.5.sp,
                  fontWeight = FontWeight.ExtraBold,
                  fontFamily = FontFamily.Monospace,
                )
              }
              Box(
                modifier =
                  Modifier.weight(1f)
                    .height(28.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF065F46))
                    .border(1.dp, Color(0xFF34D399), RoundedCornerShape(6.dp))
                    .clickable {
                      engine?.concludePublicFfaMatchCycle(forceSimulateWinWithPresence = true)
                    }
                    .testTag("btn_force_convergence_win"),
                contentAlignment = Alignment.Center,
              ) {
                Text(
                  text = "🏆 FORCE CONVERGENCE (SIMULATE >50% WIN +100❖)",
                  color = Color(0xFFA7F3D0),
                  fontSize = 8.sp,
                  fontWeight = FontWeight.ExtraBold,
                  fontFamily = FontFamily.Monospace,
                )
              }
            }
          }

          // 1. Playfield Camera Zoom
          CalibrationCardWithPreview(
            title = "Playfield Camera Scale (Zoom)",
            value = RiftCalibrationState.cameraZoomScale,
            range = 0.35f..2.2f,
            step = 0.05f,
            onValueChange = { RiftCalibrationState.cameraZoomScale = it },
            description = "Lower = see wider playfield; Higher = closer view",
          ) {
            CameraZoomPreviewBox(scale = RiftCalibrationState.cameraZoomScale)
          }

          // 2. Ship Hull Scale
          CalibrationCardWithPreview(
            title = "Ship Hull Scale",
            value = RiftCalibrationState.shipScale,
            range = 0.4f..2.0f,
            step = 0.05f,
            onValueChange = { RiftCalibrationState.shipScale = it },
            description = "3D metallic pre-rendered ship sprite size",
          ) {
            ShipPreviewBox(scale = RiftCalibrationState.shipScale)
          }

          // 3. Playfield Rift Obstacles Scale
          CalibrationCardWithPreview(
            title = "Playfield Rift Obstacles Scale",
            value = RiftCalibrationState.anomalyScale,
            range = 0.3f..2.0f,
            step = 0.05f,
            onValueChange = { RiftCalibrationState.anomalyScale = it },
            description = "Plasma Rift, Repulsor Rift, Gravity Rift & Phase Rifts (5x default)",
          ) {
            AnomalyPreviewBox(scale = RiftCalibrationState.anomalyScale)
          }

          // 4. Buff Prize Crate Scale & Spawner System
          BuffPrizeCrateTuningCard(engine = engine)

          // 5. Safe Zone Base Pad, Boundary & Anti-Camp Idle Teleport
          SafeZoneTuningCard(engine = engine)

          // 6. Turret Reticle & Aim Telegraph Cone
          CalibrationCardWithPreview(
            title = "Turret Aim Telegraph & Reticle Scale",
            value = RiftCalibrationState.aimTelegraphScale,
            range = 0.4f..2.0f,
            step = 0.05f,
            onValueChange = { RiftCalibrationState.aimTelegraphScale = it },
            description = "Spread cone arc radius & raycast bounce trajectory length",
          ) {
            AimTelegraphPreviewBox(scale = RiftCalibrationState.aimTelegraphScale)
          }

          // 7. Bullets, Bombs & Mines Scale
          CalibrationCardWithPreview(
            title = "Bullets, Bombs & Mines Scale",
            value = RiftCalibrationState.projectileScale,
            range = 0.4f..2.0f,
            step = 0.05f,
            onValueChange = { RiftCalibrationState.projectileScale = it },
            description = "Bullet beam thickness, bomb radius, and mine ring size",
          ) {
            ProjectilePreviewBox(scale = RiftCalibrationState.projectileScale)
          }

          // 8. Floating Combat Text (FCT) Scale
          CalibrationCardWithPreview(
            title = "Floating Combat Text (FCT) Scale",
            value = RiftCalibrationState.fctScale,
            range = 0.4f..2.0f,
            step = 0.05f,
            onValueChange = { RiftCalibrationState.fctScale = it },
            description = "Damage, crit, and shield floating popup text size",
          ) {
            FctPreviewBox(scale = RiftCalibrationState.fctScale)
          }

          // ================= SHIP LABELS & CALLSIGN PLACEMENT =================
          SectionHeader(title = "SHIP LABELS & CALLSIGN PLACEMENT", color = Color(0xFFFACC15))

          // Bounty Placement
          PlacementSelectorCard(
            title = "Bounty Placement",
            current = RiftCalibrationState.bountyPlacement,
            options = listOf(
              LabelPlacement.TOP,
              LabelPlacement.BOTTOM,
              LabelPlacement.LEFT,
              LabelPlacement.RIGHT,
              LabelPlacement.CONSOLIDATED_TOP,
            ),
            onSelect = { RiftCalibrationState.bountyPlacement = it },
          )

          // Faction Tag Placement
          PlacementSelectorCard(
            title = "Faction Tag Placement",
            current = RiftCalibrationState.tagPlacement,
            options = listOf(
              LabelPlacement.TOP,
              LabelPlacement.BOTTOM,
              LabelPlacement.LEFT,
              LabelPlacement.RIGHT,
            ),
            onSelect = { RiftCalibrationState.tagPlacement = it },
          )

          // Callsign / Name Placement
          PlacementSelectorCard(
            title = "Callsign / Name Placement",
            current = RiftCalibrationState.namePlacement,
            options = listOf(
              LabelPlacement.TOP,
              LabelPlacement.BOTTOM,
              LabelPlacement.LEFT,
              LabelPlacement.RIGHT,
            ),
            onSelect = { RiftCalibrationState.namePlacement = it },
          )

          // Label Text Font Scale
          CalibrationCardWithPreview(
            title = "Label Text Font Scale",
            value = RiftCalibrationState.labelTextScale,
            range = 0.5f..2.0f,
            step = 0.05f,
            onValueChange = { RiftCalibrationState.labelTextScale = it },
            description = "Font size of ship bounty, tag, and name callsign",
          ) {
            ShipPreviewBox(scale = RiftCalibrationState.shipScale)
          }

          // Label Distance / Radial Spacing
          CalibrationCardWithPreview(
            title = "Label Radial Distance Spacing",
            value = RiftCalibrationState.labelDistanceOffset,
            range = 0.5f..2.5f,
            step = 0.05f,
            onValueChange = { RiftCalibrationState.labelDistanceOffset = it },
            description = "Radial spacing offset from ship hull perimeter",
          ) {
            ShipPreviewBox(scale = RiftCalibrationState.shipScale)
          }
        }

        // Right Column: Heads-Up Display (HUD) Components
        Column(
          modifier =
            Modifier.weight(1f)
              .fillMaxHeight()
              .verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          SectionHeader(title = "HEADS-UP DISPLAY (HUD) COMPONENTS", color = Color(0xFFF59E0B))

          // 5. Top Navigation Bar
          CalibrationCardWithPreview(
            title = "Top Navigation Bar Scale",
            value = RiftCalibrationState.topBarScale,
            range = 0.5f..1.8f,
            step = 0.05f,
            onValueChange = { RiftCalibrationState.topBarScale = it },
            description = "Left-anchored pill bar (Hangar, Codex, etc.)",
          ) {
            TopBarPreviewBox(scale = RiftCalibrationState.topBarScale)
          }

          // 6. Buff Bar Scale
          CalibrationCardWithPreview(
            title = "Buff Bar Scale",
            value = RiftCalibrationState.buffBarScale,
            range = 0.5f..2.0f,
            step = 0.05f,
            onValueChange = { RiftCalibrationState.buffBarScale = it },
            description = "Top-center active pilot upgrades dock",
          ) {
            BuffBarPreviewBox(scale = RiftCalibrationState.buffBarScale)
          }

          // 7. Kill Log Window
          CalibrationCardWithPreview(
            title = "Kill Log Window Scale",
            value = RiftCalibrationState.killLogScale,
            range = 0.5f..1.8f,
            step = 0.05f,
            onValueChange = { RiftCalibrationState.killLogScale = it },
            description = "Top-right 8-row glass combat feed",
          ) {
            KillLogPreviewBox(scale = RiftCalibrationState.killLogScale)
          }

          // 8. Bottom Console, Minimap & Playfield Idle Flux Drain
          BottomConsoleAndIdleDrainTuningCard(engine = engine)

          // 9. Right Combat Button Arc
          CalibrationCardWithPreview(
            title = "Right Combat Button Arc Scale",
            value = RiftCalibrationState.combatArcScale,
            range = 0.5f..1.8f,
            step = 0.05f,
            onValueChange = { RiftCalibrationState.combatArcScale = it },
            description = "4-Input Cluster: Fire Stick, Contextual Secondary, Repel & Special Buff Weapon",
          ) {
            CombatButtonsPreviewBox(scale = RiftCalibrationState.combatArcScale)
          }

          // 10. Left Movement Stick
          CalibrationCardWithPreview(
            title = "Left Movement Thumbstick Scale",
            value = RiftCalibrationState.leftStickScale,
            range = 0.5f..1.8f,
            step = 0.05f,
            onValueChange = { RiftCalibrationState.leftStickScale = it },
            description = "Virtual thrust & afterburner stick",
          ) {
            ThumbstickPreviewBox(scale = RiftCalibrationState.leftStickScale)
          }
        }
      }
    }
  }
}

@Composable
private fun SectionHeader(title: String, color: Color) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(6.dp),
    modifier = Modifier.padding(top = 2.dp, bottom = 2.dp),
  ) {
    Box(modifier = Modifier.size(6.dp).background(color, CircleShape))
    Text(
      text = title,
      color = color,
      fontSize = 10.5.sp,
      fontWeight = FontWeight.ExtraBold,
      fontFamily = FontFamily.Monospace,
    )
  }
}

@Composable
private fun PlacementSelectorCard(
  title: String,
  current: LabelPlacement,
  options: List<LabelPlacement>,
  onSelect: (LabelPlacement) -> Unit,
) {
  Column(
    modifier =
      Modifier.fillMaxWidth()
        .background(Color(0x550B1626), RoundedCornerShape(10.dp))
        .border(1.dp, Color(0x3338BDF8), RoundedCornerShape(10.dp))
        .padding(horizontal = 10.dp, vertical = 7.dp),
    verticalArrangement = Arrangement.spacedBy(6.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = title,
        color = Color(0xFFE2E8F0),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
      )
      Text(
        text = current.label,
        color = Color(0xFFFACC15),
        fontSize = 10.sp,
        fontWeight = FontWeight.ExtraBold,
        fontFamily = FontFamily.Monospace,
      )
    }

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      options.forEach { opt ->
        val isSelected = current == opt
        Box(
          modifier =
            Modifier.weight(1f)
              .height(26.dp)
              .clip(RoundedCornerShape(6.dp))
              .background(if (isSelected) Color(0x6638BDF8) else Color(0x221E293B))
              .border(
                1.dp,
                if (isSelected) Color(0xFF38BDF8) else Color(0x33475569),
                RoundedCornerShape(6.dp),
              )
              .clickable { onSelect(opt) },
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = when (opt) {
              LabelPlacement.TOP -> "TOP"
              LabelPlacement.BOTTOM -> "BTM"
              LabelPlacement.LEFT -> "LFT"
              LabelPlacement.RIGHT -> "RGT"
              LabelPlacement.CONSOLIDATED_TOP -> "CONS▲"
              LabelPlacement.CONSOLIDATED_BOTTOM -> "CONS▼"
            },
            color = if (isSelected) Color.White else Color(0xFF94A3B8),
            fontSize = 9.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
          )
        }
      }
    }
  }
}

/**
 * Universal calibration card presenting slider controls on top and a live visual preview below.
 */
@Composable
private fun CalibrationCardWithPreview(
  title: String,
  value: Float,
  range: ClosedFloatingPointRange<Float>,
  step: Float,
  onValueChange: (Float) -> Unit,
  description: String,
  previewContent: @Composable () -> Unit,
) {
  Column(
    modifier =
      Modifier.fillMaxWidth()
        .background(Color(0x550B1626), RoundedCornerShape(10.dp))
        .border(1.dp, Color(0x3338BDF8), RoundedCornerShape(10.dp))
        .padding(horizontal = 10.dp, vertical = 7.dp),
    verticalArrangement = Arrangement.spacedBy(4.dp),
  ) {
    // Title & Numeric Readout
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = title,
          color = Color(0xFFE2E8F0),
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.SansSerif,
        )
        if (description.isNotEmpty()) {
          Text(
            text = description,
            color = Color(0xFF64748B),
            fontSize = 8.sp,
            fontFamily = FontFamily.SansSerif,
          )
        }
      }

      Text(
        text = String.format(Locale.US, "%.2fx", value),
        color = Color(0xFFFACC15),
        fontSize = 12.sp,
        fontWeight = FontWeight.ExtraBold,
        fontFamily = FontFamily.Monospace,
      )
    }

    // Slider Row
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Box(
        modifier =
          Modifier.size(22.dp)
            .clip(CircleShape)
            .background(Color(0x3338BDF8))
            .clickable {
              val next = (value - step).coerceIn(range.start, range.endInclusive)
              onValueChange((next * 100).roundToInt() / 100f)
            },
        contentAlignment = Alignment.Center,
      ) {
        Text("-", color = Color(0xFF38BDF8), fontSize = 13.sp, fontWeight = FontWeight.Bold)
      }

      Slider(
        value = value,
        onValueChange = onValueChange,
        valueRange = range,
        modifier = Modifier.weight(1f).height(22.dp),
        colors = SliderDefaults.colors(
          thumbColor = Color(0xFF38BDF8),
          activeTrackColor = Color(0xFF38BDF8),
          inactiveTrackColor = Color(0x33475569),
        ),
      )

      Box(
        modifier =
          Modifier.size(22.dp)
            .clip(CircleShape)
            .background(Color(0x3338BDF8))
            .clickable {
              val next = (value + step).coerceIn(range.start, range.endInclusive)
              onValueChange((next * 100).roundToInt() / 100f)
            },
        contentAlignment = Alignment.Center,
      ) {
        Text("+", color = Color(0xFF38BDF8), fontSize = 13.sp, fontWeight = FontWeight.Bold)
      }
    }

    // Live Visual Preview Frame
    Box(
      modifier =
        Modifier.fillMaxWidth()
          .height(58.dp)
          .clip(RoundedCornerShape(8.dp))
          .background(Color(0x66030712))
          .border(0.8.dp, Color(0x2238BDF8), RoundedCornerShape(8.dp)),
      contentAlignment = Alignment.Center,
    ) {
      previewContent()
    }
  }
}

/* =========================================================================================
   LIVE VISUAL ITEM PREVIEW COMPONENTS (SCALED DYNAMICALLY IN REAL-TIME)
   ========================================================================================= */

@Composable
private fun CameraZoomPreviewBox(scale: Float) {
  Canvas(modifier = Modifier.size(140.dp, 52.dp)) {
    val cx = size.width / 2f
    val cy = size.height / 2f

    // Grid lines showing relative playfield coverage
    val gridDist = 18f / scale.coerceIn(0.4f, 2.0f)
    var x = cx % gridDist
    while (x < size.width) {
      drawLine(Color(0x2238BDF8), Offset(x, 0f), Offset(x, size.height), 0.8f)
      x += gridDist
    }
    var y = cy % gridDist
    while (y < size.height) {
      drawLine(Color(0x2238BDF8), Offset(0f, y), Offset(size.width, y), 0.8f)
      y += gridDist
    }

    // Center ship marker
    val shipR = 5f * scale.coerceIn(0.5f, 1.8f)
    drawCircle(Color(0xFF38BDF8), shipR, Offset(cx, cy))
    drawCircle(Color.White, shipR * 0.4f, Offset(cx, cy))

    // Camera field of view frustum frame
    val fovW = (size.width * 0.72f) / scale
    val fovH = (size.height * 0.72f) / scale
    drawRect(
      color = Color(0x66FACC15),
      topLeft = Offset(cx - fovW / 2f, cy - fovH / 2f),
      size = Size(fovW, fovH),
      style = Stroke(width = 1.0f),
    )
  }
}

@Composable
private fun ShipPreviewBox(
  scale: Float,
  bountyPos: LabelPlacement = RiftCalibrationState.bountyPlacement,
  tagPos: LabelPlacement = RiftCalibrationState.tagPlacement,
  namePos: LabelPlacement = RiftCalibrationState.namePlacement,
  labelTextScale: Float = RiftCalibrationState.labelTextScale,
  labelDistOffset: Float = RiftCalibrationState.labelDistanceOffset,
) {
  val bmp = remember { RiftSpriteBaker.getShipSprite(shipId = 1, isAllied = true).asImageBitmap() }
  Box(
    modifier = Modifier.fillMaxWidth().height(66.dp),
    contentAlignment = Alignment.Center,
  ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val cx = size.width / 2f
      val cy = size.height / 2f
      val basePx = 28.dp.toPx()
      val drawPx = basePx * scale
      val left = cx - drawPx / 2f
      val top = cy - drawPx / 2f

      // Draw metallic ship sprite
      drawImage(
        image = bmp,
        dstOffset = IntOffset(left.toInt(), top.toInt()),
        dstSize = IntSize(drawPx.toInt(), drawPx.toInt()),
      )

      // Turret aiming indicator line
      val turretAngle = -PI.toFloat() * 0.25f
      val tx1 = cx + cos(turretAngle) * (drawPx * 0.42f)
      val ty1 = cy + sin(turretAngle) * (drawPx * 0.42f)
      val tx2 = cx + cos(turretAngle) * (drawPx * 0.78f)
      val ty2 = cy + sin(turretAngle) * (drawPx * 0.78f)
      drawLine(Color(0xFF38BDF8), Offset(tx1, ty1), Offset(tx2, ty2), 1.5f)
    }

    val textSp = (7.5.sp * labelTextScale)

    val isConsolidated = (bountyPos == LabelPlacement.CONSOLIDATED_TOP ||
      bountyPos == LabelPlacement.CONSOLIDATED_BOTTOM ||
      tagPos == LabelPlacement.CONSOLIDATED_TOP ||
      tagPos == LabelPlacement.CONSOLIDATED_BOTTOM)

    if (isConsolidated) {
      val isTop = (bountyPos == LabelPlacement.CONSOLIDATED_TOP || tagPos == LabelPlacement.CONSOLIDATED_TOP)
      Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = if (isTop) Alignment.TopCenter else Alignment.BottomCenter,
      ) {
        Text(
          text = "[VOID] VIPER • ★ 787",
          color = Color(0xFFFACC15),
          fontSize = textSp,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.Monospace,
          modifier = Modifier.padding(
            top = if (isTop) 3.dp else 0.dp,
            bottom = if (!isTop) 3.dp else 0.dp,
          ),
        )
      }
    } else {
      // Clock positions (TOP, BOTTOM, LEFT, RIGHT)
      listOf(LabelPlacement.TOP, LabelPlacement.BOTTOM, LabelPlacement.LEFT, LabelPlacement.RIGHT).forEach { pos ->
        val hasBounty = bountyPos == pos
        val hasTag = tagPos == pos
        val hasName = namePos == pos
        if (hasBounty || hasTag || hasName) {
          val alignment = when (pos) {
            LabelPlacement.TOP -> Alignment.TopCenter
            LabelPlacement.BOTTOM -> Alignment.BottomCenter
            LabelPlacement.LEFT -> Alignment.CenterStart
            LabelPlacement.RIGHT -> Alignment.CenterEnd
            else -> Alignment.Center
          }
          val paddingMod = when (pos) {
            LabelPlacement.TOP -> Modifier.padding(top = 2.dp)
            LabelPlacement.BOTTOM -> Modifier.padding(bottom = 2.dp)
            LabelPlacement.LEFT -> Modifier.padding(start = 10.dp)
            LabelPlacement.RIGHT -> Modifier.padding(end = 10.dp)
            else -> Modifier
          }

          Box(
            modifier = Modifier.fillMaxSize().then(paddingMod),
            contentAlignment = alignment,
          ) {
            Column(
              horizontalAlignment = when (pos) {
                LabelPlacement.LEFT -> Alignment.End
                LabelPlacement.RIGHT -> Alignment.Start
                else -> Alignment.CenterHorizontally
              },
            ) {
              if (hasBounty) {
                Text(
                  text = "★ 787",
                  color = Color(0xFFFACC15),
                  fontSize = textSp * 1.05f,
                  fontWeight = FontWeight.ExtraBold,
                  fontFamily = FontFamily.Monospace,
                )
              }
              if (hasTag && hasName) {
                Text(
                  text = "[VOID] VIPER",
                  color = Color(0xFF38BDF8),
                  fontSize = textSp,
                  fontWeight = FontWeight.Bold,
                  fontFamily = FontFamily.SansSerif,
                )
              } else {
                if (hasTag) {
                  Text(
                    text = "[VOID]",
                    color = Color(0xFF60A5FA),
                    fontSize = textSp * 0.95f,
                    fontWeight = FontWeight.Bold,
                  )
                }
                if (hasName) {
                  Text(
                    text = "VIPER",
                    color = Color(0xFF38BDF8),
                    fontSize = textSp,
                    fontWeight = FontWeight.Bold,
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
private fun AnomalyPreviewBox(scale: Float) {
  Canvas(modifier = Modifier.size(110.dp, 52.dp)) {
    val cx = size.width / 2f
    val cy = size.height / 2f
    val baseR = 16.dp.toPx()
    val r = baseR * scale

    // Singularity halo
    drawCircle(Color(0x44818CF8), r * 1.5f, Offset(cx, cy))
    drawCircle(Color(0xFFA855F7), r, Offset(cx, cy), style = Stroke(width = 2.0f))
    drawCircle(Color(0xFF030712), r * 0.85f, Offset(cx, cy))
    drawCircle(Color(0xFFC084FC), r * 0.35f, Offset(cx, cy), style = Stroke(width = 1.5f))
  }
}

@Composable
private fun CratePreviewBox(scale: Float) {
  val bmp = remember { RiftSpriteBaker.getBuffIconBitmap(1).asImageBitmap() }
  Canvas(modifier = Modifier.size(90.dp, 52.dp)) {
    val basePx = 28.dp.toPx()
    val drawPx = basePx * scale
    val left = (size.width - drawPx) / 2f
    val top = (size.height - drawPx) / 2f

    // Glowing halo
    drawCircle(Color(0x5510B981), drawPx * 0.72f, Offset(size.width / 2f, size.height / 2f))
    drawImage(
      image = bmp,
      dstOffset = IntOffset(left.toInt(), top.toInt()),
      dstSize = IntSize(drawPx.toInt(), drawPx.toInt()),
    )
  }
}

@Composable
private fun BuffPrizeCrateTuningCard(
  engine: RiftGameEngine?,
) {
  Column(
    modifier =
      Modifier.fillMaxWidth()
        .background(Color(0x550B1626), RoundedCornerShape(10.dp))
        .border(1.dp, Color(0x3310B981), RoundedCornerShape(10.dp))
        .padding(horizontal = 10.dp, vertical = 8.dp),
    verticalArrangement = Arrangement.spacedBy(6.dp),
  ) {
    // 1. Header with visual preview and scale slider
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = "Supply Crate & Spawner System (Public FFA)",
          color = Color(0xFF34D399),
          fontSize = 11.5.sp,
          fontWeight = FontWeight.Bold,
        )
        Text(
          text = "3D Supply Crate scale, Public FFA cap & non-repeating spawner",
          color = Color(0xFF94A3B8),
          fontSize = 8.sp,
        )
      }
      Text(
        text = String.format(Locale.US, "%.2fx", RiftCalibrationState.crateScale),
        color = Color(0xFFFACC15),
        fontSize = 12.sp,
        fontWeight = FontWeight.ExtraBold,
        fontFamily = FontFamily.Monospace,
      )
    }

    // Scale Slider & Live Preview
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Box(
        modifier =
          Modifier.size(22.dp)
            .clip(CircleShape)
            .background(Color(0x3338BDF8))
            .clickable {
              val next = (RiftCalibrationState.crateScale - 0.05f).coerceIn(0.4f, 2.0f)
              RiftCalibrationState.crateScale = (next * 100).roundToInt() / 100f
            },
        contentAlignment = Alignment.Center,
      ) {
        Text("-", color = Color(0xFF38BDF8), fontSize = 13.sp, fontWeight = FontWeight.Bold)
      }

      Slider(
        value = RiftCalibrationState.crateScale,
        onValueChange = { RiftCalibrationState.crateScale = it },
        valueRange = 0.4f..2.0f,
        modifier = Modifier.weight(1f).height(22.dp),
        colors = SliderDefaults.colors(
          thumbColor = Color(0xFF10B981),
          activeTrackColor = Color(0xFF10B981),
          inactiveTrackColor = Color(0x33475569),
        ),
      )

      Box(
        modifier =
          Modifier.size(22.dp)
            .clip(CircleShape)
            .background(Color(0x3338BDF8))
            .clickable {
              val next = (RiftCalibrationState.crateScale + 0.05f).coerceIn(0.4f, 2.0f)
              RiftCalibrationState.crateScale = (next * 100).roundToInt() / 100f
            },
        contentAlignment = Alignment.Center,
      ) {
        Text("+", color = Color(0xFF38BDF8), fontSize = 13.sp, fontWeight = FontWeight.Bold)
      }
    }

    // Live Visual Preview Frame
    Box(
      modifier =
        Modifier.fillMaxWidth()
          .height(52.dp)
          .clip(RoundedCornerShape(8.dp))
          .background(Color(0x66030712))
          .border(0.8.dp, Color(0x2210B981), RoundedCornerShape(8.dp)),
      contentAlignment = Alignment.Center,
    ) {
      CratePreviewBox(scale = RiftCalibrationState.crateScale)
    }

    // 2. Active Telemetry Status Banner
    val activeCount = engine?.greens?.size ?: 0
    val effectiveCap = engine?.getEffectiveBuffCap() ?: (RiftCalibrationState.buffTargetCountOverride ?: 12)
    val modeName = if (RiftCalibrationState.buffTargetCountOverride != null) "CUSTOM OVERRIDE" else RiftCalibrationState.buffModeOverride

    Row(
      modifier =
        Modifier.fillMaxWidth()
          .clip(RoundedCornerShape(6.dp))
          .background(Color(0x3310B981))
          .border(0.8.dp, Color(0x6610B981), RoundedCornerShape(6.dp))
          .padding(horizontal = 8.dp, vertical = 4.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = "ACTIVE CRATES: $activeCount / $effectiveCap",
        color = Color(0xFFE2E8F0),
        fontSize = 9.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace,
      )
      Text(
        text = "[$modeName]",
        color = Color(0xFF34D399),
        fontSize = 8.5.sp,
        fontWeight = FontWeight.ExtraBold,
        fontFamily = FontFamily.Monospace,
      )
    }

    // 3. Public FFA Supply Crate Spawner Preset Chips
    Text(
      text = "PUBLIC FFA SUPPLY CRATE PRESETS",
      color = Color(0xFF94A3B8),
      fontSize = 8.5.sp,
      fontWeight = FontWeight.Bold,
      fontFamily = FontFamily.Monospace,
    )
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      val isDefaultFfa = RiftCalibrationState.buffTargetCountOverride == null
      Box(
        modifier =
          Modifier.weight(1f)
            .height(24.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(if (isDefaultFfa) Color(0x6610B981) else Color(0x221E293B))
            .border(1.dp, if (isDefaultFfa) Color(0xFF10B981) else Color(0x33475569), RoundedCornerShape(5.dp))
            .clickable {
              RiftCalibrationState.buffModeOverride = "FFA"
              RiftCalibrationState.buffTargetCountOverride = null
            },
        contentAlignment = Alignment.Center,
      ) {
        Text(
          text = "PUBLIC FFA DEFAULT (${RiftCalibrationState.buffCapFfa} CRATES)",
          color = if (isDefaultFfa) Color.White else Color(0xFF94A3B8),
          fontSize = 7.5.sp,
          fontWeight = if (isDefaultFfa) FontWeight.Bold else FontWeight.Normal,
        )
      }
      Box(
        modifier =
          Modifier.weight(1f)
            .height(24.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(if (!isDefaultFfa) Color(0x660284C7) else Color(0x221E293B))
            .border(1.dp, if (!isDefaultFfa) Color(0xFF38BDF8) else Color(0x33475569), RoundedCornerShape(5.dp))
            .clickable {
              RiftCalibrationState.buffModeOverride = "CUSTOM"
              if (RiftCalibrationState.buffTargetCountOverride == null) {
                RiftCalibrationState.buffTargetCountOverride = RiftCalibrationState.buffCapFfa
              }
            },
        contentAlignment = Alignment.Center,
      ) {
        Text(
          text = "CUSTOM OVERRIDE (${RiftCalibrationState.buffTargetCountOverride ?: RiftCalibrationState.buffCapFfa})",
          color = if (!isDefaultFfa) Color.White else Color(0xFF94A3B8),
          fontSize = 7.5.sp,
          fontWeight = if (!isDefaultFfa) FontWeight.Bold else FontWeight.Normal,
        )
      }
    }

    // 4. Target Spawn Count Slider (0 .. 60)
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = "Target Spawn Count",
        color = Color(0xFFE2E8F0),
        fontSize = 9.5.sp,
        fontWeight = FontWeight.SemiBold,
      )
      val countText = if (RiftCalibrationState.buffTargetCountOverride == null) "AUTO ($effectiveCap)" else "${RiftCalibrationState.buffTargetCountOverride} BUFFS"
      Text(
        text = countText,
        color = if (RiftCalibrationState.buffTargetCountOverride == 0) Color(0xFFEF4444) else Color(0xFFFACC15),
        fontSize = 9.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace,
      )
    }
    Slider(
      value = (RiftCalibrationState.buffTargetCountOverride ?: effectiveCap).toFloat(),
      onValueChange = { RiftCalibrationState.buffTargetCountOverride = it.roundToInt() },
      valueRange = 0f..60f,
      steps = 59,
      modifier = Modifier.fillMaxWidth().height(20.dp),
      colors = SliderDefaults.colors(
        thumbColor = Color(0xFF38BDF8),
        activeTrackColor = Color(0xFF38BDF8),
        inactiveTrackColor = Color(0x33475569),
      ),
    )

    // 5. Respawn Frequency Slider (0.0x .. 3.0x) with Zero Respawn mode
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = "Respawn Frequency Rate",
        color = Color(0xFFE2E8F0),
        fontSize = 9.5.sp,
        fontWeight = FontWeight.SemiBold,
      )
      val freq = RiftCalibrationState.buffRespawnFrequency
      val freqText = when {
        freq <= 0.01f -> "🚫 ZERO RESPAWN"
        freq in 0.95f..1.05f -> "1.00x (STANDARD)"
        freq >= 2.5f -> "${String.format(Locale.US, "%.1fx", freq)} (HYPER-RAPID)"
        else -> String.format(Locale.US, "%.2fx", freq)
      }
      Text(
        text = freqText,
        color = if (freq <= 0.01f) Color(0xFFEF4444) else Color(0xFFFACC15),
        fontSize = 9.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace,
      )
    }
    Slider(
      value = RiftCalibrationState.buffRespawnFrequency,
      onValueChange = { RiftCalibrationState.buffRespawnFrequency = (it * 10).roundToInt() / 10f },
      valueRange = 0.0f..3.0f,
      modifier = Modifier.fillMaxWidth().height(20.dp),
      colors = SliderDefaults.colors(
        thumbColor = if (RiftCalibrationState.buffRespawnFrequency <= 0.01f) Color(0xFFEF4444) else Color(0xFFFACC15),
        activeTrackColor = if (RiftCalibrationState.buffRespawnFrequency <= 0.01f) Color(0xFFEF4444) else Color(0xFFFACC15),
        inactiveTrackColor = Color(0x33475569),
      ),
    )

    // 6. Base Respawn Delay Window (0.5s .. 8.0s)
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = "Base Respawn Delay Window",
        color = Color(0xFFE2E8F0),
        fontSize = 9.5.sp,
        fontWeight = FontWeight.SemiBold,
      )
      Text(
        text = "${String.format(Locale.US, "%.1fs", RiftCalibrationState.buffBaseRespawnDelay)} (±0.75s jitter)",
        color = Color(0xFF34D399),
        fontSize = 9.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace,
      )
    }
    Slider(
      value = RiftCalibrationState.buffBaseRespawnDelay,
      onValueChange = { RiftCalibrationState.buffBaseRespawnDelay = (it * 10).roundToInt() / 10f },
      valueRange = 0.5f..8.0f,
      modifier = Modifier.fillMaxWidth().height(20.dp),
      colors = SliderDefaults.colors(
        thumbColor = Color(0xFF34D399),
        activeTrackColor = Color(0xFF34D399),
        inactiveTrackColor = Color(0x33475569),
      ),
    )

    // 7. Public FFA Concurrent Supply Crate Cap Slider
    CompactCapSlider("Public FFA Supply Crate Cap", RiftCalibrationState.buffCapFfa, 10..80) {
      RiftCalibrationState.buffCapFfa = it
    }
  }
}

@Composable
private fun CompactCapSlider(
  label: String,
  value: Int,
  range: ClosedRange<Int>,
  onValueChange: (Int) -> Unit,
) {
  Row(
    modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(label, color = Color(0xFF94A3B8), fontSize = 8.5.sp)
    Text("$value", color = Color(0xFFFACC15), fontSize = 8.5.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
  }
  Slider(
    value = value.toFloat(),
    onValueChange = { onValueChange(it.roundToInt()) },
    valueRange = range.start.toFloat()..range.endInclusive.toFloat(),
    modifier = Modifier.fillMaxWidth().height(16.dp),
    colors = SliderDefaults.colors(
      thumbColor = Color(0xFF10B981),
      activeTrackColor = Color(0xFF10B981),
      inactiveTrackColor = Color(0x33475569),
    ),
  )
}

@Composable
private fun SafeZoneTuningCard(
  engine: RiftGameEngine?,
) {
  Column(
    modifier =
      Modifier.fillMaxWidth()
        .background(Color(0x550B1626), RoundedCornerShape(10.dp))
        .border(1.dp, Color(0x3338BDF8), RoundedCornerShape(10.dp))
        .padding(horizontal = 10.dp, vertical = 8.dp),
    verticalArrangement = Arrangement.spacedBy(6.dp),
  ) {
    // 1. Safe Zone Scale Slider
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = "Safe Zone Base Pad & Anti-Camp Teleport",
          color = Color(0xFFE2E8F0),
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold,
        )
        Text(
          text = "Central Safe Zone pad size & >15s continuous stay eviction teleport",
          color = Color(0xFF64748B),
          fontSize = 8.sp,
        )
      }
      Text(
        text = String.format(Locale.US, "%.2fx", RiftCalibrationState.safeZoneScale),
        color = Color(0xFFFACC15),
        fontSize = 12.sp,
        fontWeight = FontWeight.ExtraBold,
        fontFamily = FontFamily.Monospace,
      )
    }

    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Box(
        modifier =
          Modifier.size(22.dp)
            .clip(CircleShape)
            .background(Color(0x3338BDF8))
            .clickable {
              val next = (RiftCalibrationState.safeZoneScale - 0.05f).coerceIn(0.4f, 2.0f)
              RiftCalibrationState.safeZoneScale = (next * 100).roundToInt() / 100f
            },
        contentAlignment = Alignment.Center,
      ) {
        Text("-", color = Color(0xFF38BDF8), fontSize = 13.sp, fontWeight = FontWeight.Bold)
      }

      Slider(
        value = RiftCalibrationState.safeZoneScale,
        onValueChange = { RiftCalibrationState.safeZoneScale = it },
        valueRange = 0.4f..2.0f,
        modifier = Modifier.weight(1f).height(22.dp),
        colors = SliderDefaults.colors(
          thumbColor = Color(0xFF38BDF8),
          activeTrackColor = Color(0xFF38BDF8),
          inactiveTrackColor = Color(0x33475569),
        ),
      )

      Box(
        modifier =
          Modifier.size(22.dp)
            .clip(CircleShape)
            .background(Color(0x3338BDF8))
            .clickable {
              val next = (RiftCalibrationState.safeZoneScale + 0.05f).coerceIn(0.4f, 2.0f)
              RiftCalibrationState.safeZoneScale = (next * 100).roundToInt() / 100f
            },
        contentAlignment = Alignment.Center,
      ) {
        Text("+", color = Color(0xFF38BDF8), fontSize = 13.sp, fontWeight = FontWeight.Bold)
      }
    }

    // Live Visual Preview Frame for Safe Zone Pad + Eviction Timer Badge
    Box(
      modifier =
        Modifier.fillMaxWidth()
          .height(58.dp)
          .clip(RoundedCornerShape(8.dp))
          .background(Color(0x66030712))
          .border(0.8.dp, Color(0x2238BDF8), RoundedCornerShape(8.dp)),
      contentAlignment = Alignment.Center,
    ) {
      SafeZonePreviewBox(scale = RiftCalibrationState.safeZoneScale)
    }

    // 2. Safe Zone Idle Teleport Limit Slider (>15.0s default)
    val p = engine?.localPlayer
    val curStay = p?.safeZoneStayTimer ?: 0f
    val remStay = (RiftCalibrationState.safeZoneIdleLimitSec - curStay).coerceAtLeast(0f)

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = "Safe Zone Idle Teleport Limit",
        color = Color(0xFFE2E8F0),
        fontSize = 9.5.sp,
        fontWeight = FontWeight.SemiBold,
      )
      Text(
        text = String.format(Locale.US, "%.1fs MAX STAY", RiftCalibrationState.safeZoneIdleLimitSec),
        color = Color(0xFF38BDF8),
        fontSize = 9.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace,
      )
    }
    Slider(
      value = RiftCalibrationState.safeZoneIdleLimitSec,
      onValueChange = { RiftCalibrationState.safeZoneIdleLimitSec = (it * 2).roundToInt() / 2f },
      valueRange = 3.0f..60.0f,
      modifier = Modifier.fillMaxWidth().height(20.dp),
      colors = SliderDefaults.colors(
        thumbColor = Color(0xFF38BDF8),
        activeTrackColor = Color(0xFF38BDF8),
        inactiveTrackColor = Color(0x33475569),
      ),
    )

    // Live Telemetry & Test Evict Button
    Row(
      modifier =
        Modifier.fillMaxWidth()
          .clip(RoundedCornerShape(6.dp))
          .background(Color(0x33082032))
          .border(0.8.dp, Color(0x5538BDF8), RoundedCornerShape(6.dp))
          .padding(horizontal = 8.dp, vertical = 4.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = if (p?.inSafeZone == true) {
          String.format(Locale.US, "IN SAFE ZONE • EVICT WARP IN %.1fs", remStay)
        } else {
          String.format(Locale.US, "OUTSIDE SAFE ZONE • LIMIT %.1fs", RiftCalibrationState.safeZoneIdleLimitSec)
        },
        color = if (p?.inSafeZone == true && remStay <= 5f) Color(0xFFFACC15) else Color(0xFF38BDF8),
        fontSize = 8.5.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace,
      )
      if (engine != null && p != null) {
        Box(
          modifier =
            Modifier.clip(RoundedCornerShape(4.dp))
              .background(Color(0x4438BDF8))
              .border(0.8.dp, Color(0xFF38BDF8), RoundedCornerShape(4.dp))
              .clickable {
                val dest = engine.findRandomOpenPositionOutsideSafeZone()
                p.x = dest.first
                p.y = dest.second
                p.vx = 0f
                p.vy = 0f
                p.safeZoneStayTimer = 0f
                p.inSafeZone = false
              }
              .padding(horizontal = 6.dp, vertical = 2.dp),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = "TEST WARP",
            color = Color.White,
            fontSize = 7.5.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
          )
        }
      }
    }
  }
}

@Composable
private fun BottomConsoleAndIdleDrainTuningCard(
  engine: RiftGameEngine?,
) {
  Column(
    modifier =
      Modifier.fillMaxWidth()
        .background(Color(0x550B1626), RoundedCornerShape(10.dp))
        .border(1.dp, Color(0x33EF4444), RoundedCornerShape(10.dp))
        .padding(horizontal = 10.dp, vertical = 8.dp),
    verticalArrangement = Arrangement.spacedBy(6.dp),
  ) {
    // 1. Bottom Console & Minimap Scale Slider
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = "Bottom Console, Minimap & Idle Flux Drain",
          color = Color(0xFFE2E8F0),
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold,
        )
        Text(
          text = "Radar minimap, outer Flux ring, & >15s playfield AFK Flux drain",
          color = Color(0xFF94A3B8),
          fontSize = 8.sp,
        )
      }
      Text(
        text = String.format(Locale.US, "%.2fx", RiftCalibrationState.bottomConsoleScale),
        color = Color(0xFFFACC15),
        fontSize = 12.sp,
        fontWeight = FontWeight.ExtraBold,
        fontFamily = FontFamily.Monospace,
      )
    }

    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Box(
        modifier =
          Modifier.size(22.dp)
            .clip(CircleShape)
            .background(Color(0x3338BDF8))
            .clickable {
              val next = (RiftCalibrationState.bottomConsoleScale - 0.05f).coerceIn(0.5f, 1.8f)
              RiftCalibrationState.bottomConsoleScale = (next * 100).roundToInt() / 100f
            },
        contentAlignment = Alignment.Center,
      ) {
        Text("-", color = Color(0xFF38BDF8), fontSize = 13.sp, fontWeight = FontWeight.Bold)
      }

      Slider(
        value = RiftCalibrationState.bottomConsoleScale,
        onValueChange = { RiftCalibrationState.bottomConsoleScale = it },
        valueRange = 0.5f..1.8f,
        modifier = Modifier.weight(1f).height(22.dp),
        colors = SliderDefaults.colors(
          thumbColor = Color(0xFF38BDF8),
          activeTrackColor = Color(0xFF38BDF8),
          inactiveTrackColor = Color(0x33475569),
        ),
      )

      Box(
        modifier =
          Modifier.size(22.dp)
            .clip(CircleShape)
            .background(Color(0x3338BDF8))
            .clickable {
              val next = (RiftCalibrationState.bottomConsoleScale + 0.05f).coerceIn(0.5f, 1.8f)
              RiftCalibrationState.bottomConsoleScale = (next * 100).roundToInt() / 100f
            },
        contentAlignment = Alignment.Center,
      ) {
        Text("+", color = Color(0xFF38BDF8), fontSize = 13.sp, fontWeight = FontWeight.Bold)
      }
    }

    // Live Visual Preview Frame for Bottom Console
    Box(
      modifier =
        Modifier.fillMaxWidth()
          .height(52.dp)
          .clip(RoundedCornerShape(8.dp))
          .background(Color(0x66030712))
          .border(0.8.dp, Color(0x2238BDF8), RoundedCornerShape(8.dp)),
      contentAlignment = Alignment.Center,
    ) {
      ConsolePreviewBox(scale = RiftCalibrationState.bottomConsoleScale)
    }

    // 2. Playfield Idle Flux Drain Timeout Slider (>15.0s default)
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = "Playfield Idle Timeout (AFK Trigger)",
        color = Color(0xFFE2E8F0),
        fontSize = 9.5.sp,
        fontWeight = FontWeight.SemiBold,
      )
      Text(
        text = String.format(Locale.US, "%.1fs STATIONARY", RiftCalibrationState.playfieldIdleLimitSec),
        color = Color(0xFFFACC15),
        fontSize = 9.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace,
      )
    }
    Slider(
      value = RiftCalibrationState.playfieldIdleLimitSec,
      onValueChange = { RiftCalibrationState.playfieldIdleLimitSec = (it * 2).roundToInt() / 2f },
      valueRange = 3.0f..60.0f,
      modifier = Modifier.fillMaxWidth().height(20.dp),
      colors = SliderDefaults.colors(
        thumbColor = Color(0xFFFACC15),
        activeTrackColor = Color(0xFFFACC15),
        inactiveTrackColor = Color(0x33475569),
      ),
    )

    // 3. Playfield Idle Flux Drain Rate Slider (10% maxFlux/sec default)
    val drainPct = (RiftCalibrationState.playfieldIdleDrainPctPerSec * 100f).roundToInt()
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = "Idle Flux Drain Rate (% Max Flux / Sec)",
        color = Color(0xFFE2E8F0),
        fontSize = 9.5.sp,
        fontWeight = FontWeight.SemiBold,
      )
      Text(
        text = "-$drainPct% / SEC",
        color = Color(0xFFEF4444),
        fontSize = 9.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace,
      )
    }
    Slider(
      value = RiftCalibrationState.playfieldIdleDrainPctPerSec,
      onValueChange = { RiftCalibrationState.playfieldIdleDrainPctPerSec = (it * 100).roundToInt() / 100f },
      valueRange = 0.01f..0.50f,
      modifier = Modifier.fillMaxWidth().height(20.dp),
      colors = SliderDefaults.colors(
        thumbColor = Color(0xFFEF4444),
        activeTrackColor = Color(0xFFEF4444),
        inactiveTrackColor = Color(0x33475569),
      ),
    )

    // 4. Idle Warning Banner Scale Slider + Live Warning Banner & Red Ring Preview
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = "Idle Warning Banner Scale",
        color = Color(0xFFE2E8F0),
        fontSize = 9.5.sp,
        fontWeight = FontWeight.SemiBold,
      )
      Text(
        text = String.format(Locale.US, "%.2fx", RiftCalibrationState.idleWarningBannerScale),
        color = Color(0xFFFACC15),
        fontSize = 9.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace,
      )
    }
    Slider(
      value = RiftCalibrationState.idleWarningBannerScale,
      onValueChange = { RiftCalibrationState.idleWarningBannerScale = (it * 20).roundToInt() / 20f },
      valueRange = 0.5f..1.8f,
      modifier = Modifier.fillMaxWidth().height(20.dp),
      colors = SliderDefaults.colors(
        thumbColor = Color(0xFFEF4444),
        activeTrackColor = Color(0xFFEF4444),
        inactiveTrackColor = Color(0x33475569),
      ),
    )

    // Live Visual Preview of Flashing Red Minimap Ring + Warning Banner
    Box(
      modifier =
        Modifier.fillMaxWidth()
          .height(64.dp)
          .clip(RoundedCornerShape(8.dp))
          .background(Color(0x66030712))
          .border(0.8.dp, Color(0x44EF4444), RoundedCornerShape(8.dp)),
      contentAlignment = Alignment.Center,
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        // Red Flashing Minimap Outer Flux Ring Preview
        Box(
          modifier =
            Modifier.size(36.dp)
              .clip(CircleShape)
              .background(Color(0xF0080E18))
              .border(2.5.dp, Color(0xFFEF4444), CircleShape),
          contentAlignment = Alignment.Center,
        ) {
          Text("FLUX", color = Color(0xFFEF4444), fontSize = 6.5.sp, fontWeight = FontWeight.ExtraBold)
        }

        // Scaled Warning Banner Preview
        Box(
          modifier =
            Modifier.graphicsLayer {
              scaleX = RiftCalibrationState.idleWarningBannerScale.coerceIn(0.5f, 1.35f)
              scaleY = RiftCalibrationState.idleWarningBannerScale.coerceIn(0.5f, 1.35f)
              transformOrigin = TransformOrigin(0.5f, 0.5f)
            }
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xE62A060B))
            .border(1.dp, Color(0xFFEF4444), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
          contentAlignment = Alignment.Center,
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
              text = "IDLE FLUX DRAIN — ENGAGE THRUSTERS",
              color = Color(0xFFFECACA),
              fontSize = 7.sp,
              fontWeight = FontWeight.ExtraBold,
              fontFamily = FontFamily.Monospace,
            )
            Text(
              text = "DRAINING -$drainPct% MAX FLUX/SEC",
              color = Color(0xFFF87171),
              fontSize = 6.sp,
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.Monospace,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun SafeZonePreviewBox(scale: Float) {
  Box(
    modifier = Modifier.size(160.dp, 52.dp),
    contentAlignment = Alignment.Center,
  ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val cx = size.width / 2f
      val cy = size.height / 2f
      val baseW = 108.dp.toPx()
      val baseH = 34.dp.toPx()
      val w = (baseW * scale).coerceAtMost(size.width - 8f)
      val h = (baseH * scale).coerceAtMost(size.height - 6f)
      val left = cx - w / 2f
      val top = cy - h / 2f

      // Safe zone fill & dashed border
      drawRect(Color(0x2210B981), Offset(left, top), Size(w, h))
      drawRect(Color(0x8810B981), Offset(left, top), Size(w, h), style = Stroke(1.2f))

      // Corner brackets
      val bLen = 8f * scale
      drawLine(Color(0xFF34D399), Offset(left, top), Offset(left + bLen, top), 1.5f)
      drawLine(Color(0xFF34D399), Offset(left, top), Offset(left, top + bLen), 1.5f)
      drawLine(Color(0xFF34D399), Offset(left + w - bLen, top), Offset(left + w, top), 1.5f)
      drawLine(Color(0xFF34D399), Offset(left + w, top), Offset(left + w, top + bLen), 1.5f)
      drawLine(Color(0xFF34D399), Offset(left, top + h - bLen), Offset(left, top + h), 1.5f)
      drawLine(Color(0xFF34D399), Offset(left, top + h), Offset(left + bLen, top + h), 1.5f)
      drawLine(Color(0xFF34D399), Offset(left + w - bLen, top + h), Offset(left + w, top + h), 1.5f)
      drawLine(Color(0xFF34D399), Offset(left + w, top + h - bLen), Offset(left + w, top + h), 1.5f)
    }
    Text(
      text = String.format(Locale.US, "SAFE ZONE • %.0fs MAX", RiftCalibrationState.safeZoneIdleLimitSec),
      color = Color(0xFF34D399),
      fontSize = 7.sp,
      fontWeight = FontWeight.Bold,
      fontFamily = FontFamily.Monospace,
    )
  }
}

@Composable
private fun AimTelegraphPreviewBox(scale: Float) {
  Canvas(modifier = Modifier.size(140.dp, 52.dp)) {
    val startX = 20f
    val startY = size.height / 2f
    val baseConeR = 70.dp.toPx()
    val coneR = (baseConeR * scale).coerceAtMost(size.width - 24f)

    // Ship origin dot
    drawCircle(Color(0xFF38BDF8), 4.5f, Offset(startX, startY))

    // Spread cone arc
    drawArc(
      color = Color(0x3338BDF8),
      startAngle = -22f,
      sweepAngle = 44f,
      useCenter = true,
      topLeft = Offset(startX - coneR, startY - coneR),
      size = Size(coneR * 2f, coneR * 2f),
    )

    // Aim guide raycast
    drawLine(Color(0xFF38BDF8), Offset(startX, startY), Offset(startX + coneR, startY), 1.4f)
    drawCircle(Color(0xFFF59E0B), 3f * scale, Offset(startX + coneR * 0.75f, startY))
  }
}

@Composable
private fun ProjectilePreviewBox(scale: Float) {
  Canvas(modifier = Modifier.size(150.dp, 52.dp)) {
    val cy = size.height / 2f

    // 1. Bullet Laser Beam
    val bStartX = 14f
    val bEndX = 52f
    val stroke = 3.5f * scale
    drawLine(Color(0xFF38BDF8), Offset(bStartX, cy), Offset(bEndX, cy), stroke)
    drawLine(Color.White, Offset(bStartX + 8f, cy), Offset(bEndX, cy), stroke * 0.45f)

    // 2. Amber Bomb
    val bombX = 82f
    val bombR = 8.5f * scale
    drawCircle(Color(0xFFF59E0B), bombR, Offset(bombX, cy))
    drawCircle(Color.White, bombR * 0.45f, Offset(bombX, cy))

    // 3. Proximity Mine with Warning Ring
    val mineX = 124f
    val mineR = 6.5f * scale
    drawCircle(Color(0x44EF4444), (mineR + 7f) * scale, Offset(mineX, cy))
    drawCircle(Color(0xFFEF4444), (mineR + 7f) * scale, Offset(mineX, cy), style = Stroke(0.8f))
    drawCircle(Color(0xFFEF4444), mineR, Offset(mineX, cy))
    drawCircle(Color.White, mineR * 0.4f, Offset(mineX, cy))
  }
}

@Composable
private fun FctPreviewBox(scale: Float) {
  Row(
    modifier =
      Modifier.graphicsLayer {
        scaleX = scale
        scaleY = scale
        transformOrigin = TransformOrigin(0.5f, 0.5f)
      },
    horizontalArrangement = Arrangement.spacedBy(10.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      text = "+185 CRIT",
      color = Color(0xFFFACC15),
      fontSize = 11.sp,
      fontWeight = FontWeight.ExtraBold,
      fontFamily = FontFamily.Monospace,
    )
    Text(
      text = "-45 SHIELD",
      color = Color(0xFF38BDF8),
      fontSize = 9.sp,
      fontWeight = FontWeight.Bold,
      fontFamily = FontFamily.SansSerif,
    )
  }
}

@Composable
private fun TopBarPreviewBox(scale: Float) {
  Row(
    modifier =
      Modifier.graphicsLayer {
        scaleX = scale
        scaleY = scale
        transformOrigin = TransformOrigin(0.5f, 0.5f)
      }
      .height(20.dp)
      .background(Color(0x33080E18), RoundedCornerShape(10.dp))
      .border(0.8.dp, Color(0x3D38BDF8), RoundedCornerShape(10.dp))
      .padding(horizontal = 4.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(3.dp),
  ) {
    MiniPillPreview("🚀 HANGAR", Color(0xFF38BDF8))
    MiniPillPreview("📖 CODEX", Color(0xFF34D399))
    MiniPillPreview("📋 DOSSIER", Color(0xFFFFD700))
  }
}

@Composable
private fun MiniPillPreview(label: String, color: Color) {
  Box(
    modifier =
      Modifier.clip(RoundedCornerShape(8.dp))
        .background(Color(0x33080E18))
        .border(0.8.dp, color.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
        .padding(horizontal = 5.dp, vertical = 2.dp),
  ) {
    Text(text = label, color = color, fontSize = 7.sp, fontWeight = FontWeight.Bold)
  }
}

@Composable
private fun BuffBarPreviewBox(scale: Float) {
  Row(
    modifier =
      Modifier.graphicsLayer {
        scaleX = scale
        scaleY = scale
        transformOrigin = TransformOrigin(0.5f, 0.5f)
      }
      .clip(RoundedCornerShape(8.dp))
      .background(Color(0x33080E18))
      .border(0.8.dp, Color(0x3D38BDF8), RoundedCornerShape(8.dp))
      .padding(horizontal = 6.dp, vertical = 2.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(4.dp),
  ) {
    listOf(
      Color(0xFF38BDF8) to "x2",
      Color(0xFFF59E0B) to "x1",
      Color(0xFFFACC15) to "MULTI",
      Color(0xFFA855F7) to "x1",
    ).forEach { (color, text) ->
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
          modifier = Modifier.size(13.dp).clip(CircleShape).border(1.dp, color, CircleShape),
          contentAlignment = Alignment.Center,
        ) {
          Box(modifier = Modifier.size(5.dp).background(color, CircleShape))
        }
        Text(text = text, color = color, fontSize = 5.5.sp, fontWeight = FontWeight.Bold)
      }
    }
  }
}

@Composable
private fun KillLogPreviewBox(scale: Float) {
  Column(
    modifier =
      Modifier.graphicsLayer {
        scaleX = scale
        scaleY = scale
        transformOrigin = TransformOrigin(0.5f, 0.5f)
      }
      .width(180.dp)
      .clip(RoundedCornerShape(6.dp))
      .background(Color(0x33080E18))
      .border(0.8.dp, Color(0x3338BDF8), RoundedCornerShape(6.dp))
      .padding(horizontal = 5.dp, vertical = 3.dp),
    verticalArrangement = Arrangement.spacedBy(2.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text("[GUEST] VIPER", color = Color(0xFF38BDF8), fontSize = 6.8.sp, fontWeight = FontWeight.Bold)
      Text("⚡", color = Color(0xFF94A3B8), fontSize = 6.sp)
      Text("[RIVAL] ZERO", color = Color(0xFFF87171), fontSize = 6.8.sp, fontWeight = FontWeight.Bold)
      Text("+100", color = Color(0xFFFACC15), fontSize = 6.8.sp, fontWeight = FontWeight.Bold)
    }
  }
}

@Composable
private fun ConsolePreviewBox(scale: Float) {
  Row(
    modifier =
      Modifier.graphicsLayer {
        scaleX = scale
        scaleY = scale
        transformOrigin = TransformOrigin(0.5f, 0.5f)
      },
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(6.dp),
  ) {
    // Left Wing
    Box(
      modifier =
        Modifier.height(26.dp)
          .clip(RoundedCornerShape(13.dp))
          .background(Color(0xD9080E18))
          .border(0.8.dp, Color(0x4438BDF8), RoundedCornerShape(13.dp))
          .padding(horizontal = 8.dp),
      contentAlignment = Alignment.Center,
    ) {
      Text("SPD 420  SEC E4", color = Color(0xFF38BDF8), fontSize = 7.sp, fontWeight = FontWeight.Bold)
    }

    // Radar circle
    Box(
      modifier =
        Modifier.size(32.dp)
          .clip(CircleShape)
          .background(Color(0xF0080E18))
          .border(1.dp, Color(0xFF38BDF8), CircleShape),
      contentAlignment = Alignment.Center,
    ) {
      Canvas(modifier = Modifier.fillMaxSize()) {
        drawCircle(Color(0x3338BDF8), size.width * 0.35f, Offset(size.width / 2f, size.height / 2f), style = Stroke(0.8f))
        drawCircle(Color(0xFF38BDF8), 2.2f, Offset(size.width / 2f, size.height / 2f))
      }
    }

    // Right Wing
    Box(
      modifier =
        Modifier.height(26.dp)
          .clip(RoundedCornerShape(13.dp))
          .background(Color(0xD9080E18))
          .border(0.8.dp, Color(0x4438BDF8), RoundedCornerShape(13.dp))
          .padding(horizontal = 8.dp),
      contentAlignment = Alignment.Center,
    ) {
      Text("FLUX 2400⚡ (100%)", color = Color(0xFF10B981), fontSize = 7.sp, fontWeight = FontWeight.Bold)
    }
  }
}

@Composable
private fun CombatButtonsPreviewBox(scale: Float) {
  Row(
    modifier =
      Modifier.graphicsLayer {
        scaleX = scale
        scaleY = scale
        transformOrigin = TransformOrigin(0.5f, 0.5f)
      },
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    // [BOMB 180⚡] Button
    Box(
      modifier =
        Modifier.size(32.dp)
          .clip(CircleShape)
          .background(Color(0x995F3E08))
          .border(1.dp, Color(0xFFF59E0B), CircleShape),
      contentAlignment = Alignment.Center,
    ) {
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("BOMB", color = Color.White, fontSize = 6.5.sp, fontWeight = FontWeight.Bold)
        Text("180⚡", color = Color(0xFFFDE047), fontSize = 5.5.sp)
      }
    }

    // [FIRE] Stick Button
    Box(
      modifier =
        Modifier.size(42.dp)
          .clip(CircleShape)
          .background(Color(0x99410C16))
          .border(1.2.dp, Color(0xFFEF4444), CircleShape),
      contentAlignment = Alignment.Center,
    ) {
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("FIRE", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.ExtraBold)
        Text("28⚡", color = Color(0xFFFECACA), fontSize = 6.sp)
      }
    }
  }
}

@Composable
private fun ThumbstickPreviewBox(scale: Float) {
  Canvas(
    modifier =
      Modifier.size(48.dp)
        .graphicsLayer {
          scaleX = scale
          scaleY = scale
          transformOrigin = TransformOrigin(0.5f, 0.5f)
        },
  ) {
    val cx = size.width / 2f
    val cy = size.height / 2f
    val r = size.minDimension * 0.44f

    drawCircle(Color(0x66081222), r, Offset(cx, cy))
    drawCircle(Color(0xAA38BDF8), r, Offset(cx, cy), style = Stroke(1.2f))
    drawCircle(Color(0x55F59E0B), r * 0.8f, Offset(cx, cy), style = Stroke(0.8f))
    drawCircle(Color(0xFF38BDF8), r * 0.42f, Offset(cx, cy))
  }
}
