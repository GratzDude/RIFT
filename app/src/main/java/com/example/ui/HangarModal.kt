package com.example.ui

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.RiftCodex
import com.example.engine.RiftSpriteBaker
import com.example.engine.ShipSpec

@Composable
fun HangarModal(
  initialCallsign: String,
  syndicateTag: String,
  syndicateName: String,
  selectedShipId: Int,
  selectedMode: String,
  pilotLevel: Int = 1,
  xpProgressFraction: Float = 0f,
  customizationPointsTotal: Int = 1,
  spentCustomizationPoints: Int = 0,
  riftShards: Long = 150L,
  onOpenRiftShardsStore: () -> Unit = {},
  onOpenSyndicatesModal: () -> Unit,
  onDeployPilot: (callsign: String, shipId: Int, mode: String, asGuest: Boolean) -> Unit,
  onCloseModal: () -> Unit,
) {
  var callsignInput by remember(initialCallsign) { mutableStateOf(initialCallsign) }
  var inspectedShipId by remember(selectedShipId) { mutableIntStateOf(selectedShipId) }
  var activeMode by remember(selectedMode) { mutableStateOf(selectedMode) }
  var callsignFocused by remember { mutableStateOf(false) }

  val focusManager = LocalFocusManager.current
  val keyboardController = LocalSoftwareKeyboardController.current
  val currentSpec = remember(inspectedShipId) { RiftCodex.getShipById(inspectedShipId) }
  val availableCustomPts = (customizationPointsTotal - spentCustomizationPoints).coerceAtLeast(0)

  Box(
    modifier =
      Modifier.fillMaxSize()
        .background(Color(0xB3000000))
        .imePadding()
        .padding(horizontal = 20.dp, vertical = 10.dp)
        .testTag("hangar_modal"),
    contentAlignment = Alignment.Center,
  ) {
    Surface(
      modifier =
        Modifier.fillMaxSize()
          .border(1.dp, Color(0x6600F0FF), RoundedCornerShape(16.dp)),
      color = Color(0xE60A0F18),
      shape = RoundedCornerShape(16.dp),
    ) {
      Column(modifier = Modifier.fillMaxSize().padding(14.dp)) {
        // 1. Header Row (Includes [LVL X] XP Progress Bar, Customization Points, Rift Shards & [+ GET RIFT SHARDS])
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Column {
            Text(
              text = "🚀 SYNDICATE SHIPYARD HANGAR",
              color = Color.White,
              fontSize = 18.sp,
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.SansSerif,
              letterSpacing = 1.2.sp,
            )
            Text(
              text = "SELECT CHASSIS (1–8) • CUSTOMIZE CALLSIGN • CHOOSE ENGAGEMENT THEATER",
              color = Color(0xFF00F0FF),
              fontSize = 11.sp,
              fontWeight = FontWeight.Medium,
              fontFamily = FontFamily.Monospace,
            )
          }

          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            // Compact [LVL X] Badge + Thin XP Progress Bar + Customization Points
            Column(
              modifier =
                Modifier.background(Color(0xFF0E1D34), RoundedCornerShape(8.dp))
                  .border(1.dp, Color(0xFF00F0FF), RoundedCornerShape(8.dp))
                  .padding(horizontal = 10.dp, vertical = 4.dp)
                  .testTag("hangar_header_level_badge"),
              horizontalAlignment = Alignment.CenterHorizontally,
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                Text(
                  text = "LVL $pilotLevel",
                  color = Color(0xFF00F0FF),
                  fontSize = 11.5.sp,
                  fontWeight = FontWeight.ExtraBold,
                  fontFamily = FontFamily.Monospace,
                )
                Text(
                  text = "• PTS: $availableCustomPts/$customizationPointsTotal",
                  color = Color(0xFF00E676),
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold,
                  fontFamily = FontFamily.Monospace,
                )
              }
              Spacer(modifier = Modifier.height(2.dp))
              Box(
                modifier =
                  Modifier.width(96.dp)
                    .height(3.5.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFF142844))
              ) {
                Box(
                  modifier =
                    Modifier.fillMaxWidth(xpProgressFraction.coerceIn(0f, 1f))
                      .fillMaxHeight()
                      .background(Color(0xFF00E676), RoundedCornerShape(2.dp))
                )
              }
            }

            // Rift Shards Balance + [+ GET RIFT SHARDS] Button
            Row(
              modifier =
                Modifier.clip(RoundedCornerShape(8.dp))
                  .background(Color(0xFF1A1405))
                  .border(1.dp, Color(0xFFFFD700), RoundedCornerShape(8.dp))
                  .clickable { onOpenRiftShardsStore() }
                  .padding(horizontal = 10.dp, vertical = 6.dp)
                  .testTag("hangar_btn_get_rift_shards"),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              Text(
                text = "❖ $riftShards",
                color = Color(0xFFFFD700),
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
              )
              Box(
                modifier =
                  Modifier.background(Color(0xFFFFD700), RoundedCornerShape(5.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
              ) {
                Text(
                  text = "+ GET RIFT SHARDS",
                  color = Color(0xFF05080E),
                  fontSize = 9.5.sp,
                  fontWeight = FontWeight.ExtraBold,
                  fontFamily = FontFamily.SansSerif,
                )
              }
            }

            Button(
              onClick = onCloseModal,
              colors =
                ButtonDefaults.buttonColors(
                  containerColor = Color(0xFF28111B),
                  contentColor = Color(0xFFFF3B4E),
                ),
              border = BorderStroke(1.dp, Color(0xFFFF3B4E)),
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier.height(36.dp).testTag("hangar_close_button"),
            ) {
              Text(
                text = "✕ CLOSE",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.SansSerif,
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 2. Pilot Identity Bar
        Row(
          modifier =
            Modifier.fillMaxWidth()
              .background(Color(0xFF0B192E), RoundedCornerShape(10.dp))
              .border(1.dp, Color(0xFF1E3859), RoundedCornerShape(10.dp))
              .padding(horizontal = 10.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f),
          ) {
            // Syndicate Badge (clickable to open Syndicates Modal)
            Box(
              modifier =
                Modifier.clip(RoundedCornerShape(8.dp))
                  .background(Color(0xFF142844))
                  .border(1.dp, Color(0xFFFFD700), RoundedCornerShape(8.dp))
                  .clickable { onOpenSyndicatesModal() }
                  .padding(horizontal = 10.dp, vertical = 7.dp)
                  .testTag("hangar_syndicate_badge"),
            ) {
              Column {
                Text(
                  text = "SYNDICATE [LOCKED]",
                  color = Color(0xFF8A99AD),
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold,
                  fontFamily = FontFamily.SansSerif,
                )
                Text(
                  text = "[$syndicateTag] $syndicateName",
                  color = Color(0xFFFFD700),
                  fontSize = 13.5.sp,
                  fontWeight = FontWeight.Medium,
                  fontFamily = FontFamily.SansSerif,
                )
              }
            }

            OutlinedTextField(
              value = callsignInput,
              onValueChange = { raw ->
                val cleaned =
                  raw.filter { it.isLetterOrDigit() || it == '_' || it == '-' }.take(14).uppercase()
                callsignInput = cleaned
              },
              modifier =
                Modifier.width(180.dp)
                  .onFocusChanged { callsignFocused = it.isFocused }
                  .testTag("hangar_callsign_input"),
              label = {
                Text(
                  "CALLSIGN",
                  color = Color(0xFF00F0FF),
                  fontSize = 10.sp,
                  fontFamily = FontFamily.SansSerif,
                )
              },
              textStyle =
                TextStyle(
                  color = Color.White,
                  fontSize = 13.sp,
                  fontFamily = FontFamily.Monospace,
                  fontWeight = FontWeight.Bold,
                ),
              singleLine = true,
              keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
              keyboardActions =
                KeyboardActions(
                  onDone = {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                  }
                ),
              colors =
                OutlinedTextFieldDefaults.colors(
                  focusedBorderColor = Color(0xFF00F0FF),
                  unfocusedBorderColor = Color(0xFF264B75),
                  focusedContainerColor = Color(0xFF0E1F36),
                  unfocusedContainerColor = Color(0xFF091424),
                ),
            )

            // Done keyboard dismissal button
            if (callsignFocused) {
              Button(
                onClick = {
                  focusManager.clearFocus()
                  keyboardController?.hide()
                },
                colors =
                  ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF13362B),
                    contentColor = Color(0xFF00E676),
                  ),
                border = BorderStroke(1.dp, Color(0xFF00E676)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.height(44.dp).testTag("hangar_keyboard_done_button"),
              ) {
                Text("✓ DONE", fontSize = 11.sp, fontWeight = FontWeight.Bold)
              }
            }
          }

          // Game Mode Selector
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            ModeSelectorTab(
              title = "⚔️ FFA",
              statusSub = "FREE-FOR-ALL",
              selected = activeMode == "FFA",
              onClick = { activeMode = "FFA" },
              tag = "hangar_mode_ffa",
            )
            ModeSelectorTab(
              title = "🏛️ SYNDICATE",
              statusSub = "FACTION WAR",
              selected = activeMode == "SYNDICATE_WAR",
              onClick = { activeMode = "SYNDICATE_WAR" },
              tag = "hangar_mode_syndicate",
            )
            ModeSelectorTab(
              title = "🌀 CONVERGENCE",
              statusSub = "FLEET SWEEP",
              selected = activeMode == "CONVERGENCE",
              onClick = { activeMode = "CONVERGENCE" },
              tag = "hangar_mode_convergence",
            )
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 3. Main Workspace: Ship Selector + Spec Inspector
        Row(
          modifier = Modifier.fillMaxWidth().weight(1f),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          // Left: Vertical Ship Carousel (1–8)
          LazyColumn(
            modifier =
              Modifier.width(220.dp)
                .fillMaxHeight()
                .background(Color(0xFF081426), RoundedCornerShape(10.dp))
                .border(1.dp, Color(0xFF1B3454), RoundedCornerShape(10.dp))
                .padding(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            items(RiftCodex.SHIPS) { ship ->
              val isInspected = ship.id == inspectedShipId
              Row(
                modifier =
                  Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isInspected) Color(0xFF143054) else Color(0xFF0C192E))
                    .border(
                      1.dp,
                      if (isInspected) Color(0xFF00F0FF) else Color(0x331B3454),
                      RoundedCornerShape(8.dp),
                    )
                    .clickable { inspectedShipId = ship.id }
                    .padding(horizontal = 8.dp, vertical = 6.dp)
                    .testTag("ship_card_${ship.id}"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                ShipSpritePreview(shipId = ship.id, sizeDp = 40)
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = "${ship.id}. ${ship.name}",
                    color = if (isInspected) Color(0xFF00F0FF) else Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif,
                  )
                  Text(
                    text = ship.role,
                    color = Color(0xFF8A99AD),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.SansSerif,
                  )
                }
              }
            }
          }

          // Right: Ship Detailed Specs + Deploy Action
          ShipDetailInspectorPane(
            currentSpec = currentSpec,
            modifier = Modifier.weight(1f).fillMaxHeight(),
            onDeployPilot = {
              val validName = callsignInput.ifEmpty { "PILOT_7" }
              onDeployPilot(validName, inspectedShipId, activeMode, false)
            },
          )
        }
      }
    }
  }
}

@Composable
private fun ShipDetailInspectorPane(
  currentSpec: ShipSpec,
  modifier: Modifier,
  onDeployPilot: () -> Unit,
) {
  Column(
    modifier =
      modifier
        .background(Color(0xFF081426), RoundedCornerShape(10.dp))
        .border(1.dp, Color(0xFF1B3454), RoundedCornerShape(10.dp))
        .padding(12.dp),
    verticalArrangement = Arrangement.SpaceBetween,
  ) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          ShipSpritePreview(shipId = currentSpec.id, sizeDp = 48)
          Column {
            Text(
              text = "${currentSpec.id}. ${currentSpec.name.uppercase()}",
              color = Color.White,
              fontSize = 18.sp,
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.SansSerif,
            )
            Text(
              text = "${currentSpec.syndicateTitle} • ${currentSpec.role}",
              color = Color(0xFFFFD700),
              fontSize = 11.5.sp,
              fontWeight = FontWeight.Medium,
              fontFamily = FontFamily.Monospace,
            )
          }
        }

        Button(
          onClick = onDeployPilot,
          colors =
            ButtonDefaults.buttonColors(
              containerColor = Color(0xFF00F0FF),
              contentColor = Color(0xFF030712),
            ),
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier.height(40.dp).testTag("hangar_deploy_button"),
        ) {
          Text(
            text = "⚡ DEPLOY SHIP",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.SansSerif,
          )
        }
      }

      // Weapons Cards
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        WeaponCard(
          modifier = Modifier.weight(1f),
          title = "PRIMARY WEAPON ARRAY",
          name = "${currentSpec.name.uppercase()} PRIMARY",
          details = "DMG: ${currentSpec.primaryDamage.toInt()} • COST: ${currentSpec.primaryCost.toInt()}❖ • STREAMS: ${currentSpec.primaryStreams}",
          accent = Color(0xFF00F0FF),
        )
        WeaponCard(
          modifier = Modifier.weight(1f),
          title = "SECONDARY ORDNANCE",
          name = if (currentSpec.hasBomb) "${currentSpec.bombType} WARHEAD" else "NONE (PURE GUNSHIP)",
          details =
            if (currentSpec.hasBomb) {
              "DMG: ${currentSpec.bombDamage.toInt()} • COST: ${currentSpec.bombCost.toInt()}❖ • BOUNCES: ${currentSpec.bombBounces}"
            } else {
              currentSpec.weaponSummary
            },
          accent = if (currentSpec.hasBomb) Color(0xFFFFD700) else Color(0xFF8A99AD),
        )
      }

      // Stat Bars
      StatBarRow(
        label = "TOP SPEED",
        valueText = "${currentSpec.topSpeed.toInt()} PX/S (BURN ${currentSpec.afterburnerSpeed.toInt()})",
        fraction = (currentSpec.topSpeed / 560f).coerceIn(0.2f, 1f),
        barColor = Color(0xFF00F0FF),
      )
      StatBarRow(
        label = "THRUST",
        valueText = "${currentSpec.thrust.toInt()} N/S²",
        fraction = (currentSpec.thrust / 950f).coerceIn(0.2f, 1f),
        barColor = Color(0xFF00F0FF),
      )
      StatBarRow(
        label = "TURN RATE",
        valueText = "${String.format(java.util.Locale.US, "%.1f", currentSpec.turnRate)} RAD/S",
        fraction = (currentSpec.turnRate / 5.5f).coerceIn(0.2f, 1f),
        barColor = Color(0xFFFFD700),
      )
      StatBarRow(
        label = "MAX FLUX",
        valueText = "${currentSpec.maxFlux.toInt()}❖ FLUX",
        fraction = (currentSpec.maxFlux / 4400f).coerceIn(0.2f, 1f),
        barColor = Color(0xFF00F0FF),
      )
      StatBarRow(
        label = "FLUX RECHARGE",
        valueText = "${currentSpec.rechargeRate.toInt()}❖ FLUX/SEC",
        fraction = (currentSpec.rechargeRate / 420f).coerceIn(0.2f, 1f),
        barColor = Color(0xFFFFD700),
      )
    }
  }
}

@Composable
fun ShipSpritePreview(shipId: Int, sizeDp: Int) {
  val bmp = remember(shipId) { RiftSpriteBaker.getShipSprite(shipId).asImageBitmap() }
  Canvas(modifier = Modifier.size(sizeDp.dp)) {
    drawImage(
      image = bmp,
      dstOffset = IntOffset.Zero,
      dstSize = IntSize(size.width.toInt(), size.height.toInt()),
    )
  }
}

@Composable
private fun WeaponCard(
  modifier: Modifier = Modifier,
  title: String,
  name: String,
  details: String,
  accent: Color,
) {
  Column(
    modifier =
      modifier
        .background(Color(0xFF0D1E36), RoundedCornerShape(8.dp))
        .border(1.dp, accent, RoundedCornerShape(8.dp))
        .padding(8.dp)
  ) {
    Text(
      text = title,
      color = Color(0xFF8A99AD),
      fontSize = 10.sp,
      fontWeight = FontWeight.Bold,
      fontFamily = FontFamily.SansSerif,
    )
    Text(
      text = name,
      color = accent,
      fontSize = 13.sp,
      fontWeight = FontWeight.Medium,
      fontFamily = FontFamily.SansSerif,
    )
    Text(
      text = details,
      color = Color.White,
      fontSize = 10.5.sp,
      fontFamily = FontFamily.Monospace,
    )
  }
}

@Composable
private fun StatBarRow(
  label: String,
  valueText: String,
  fraction: Float,
  barColor: Color,
) {
  Column(modifier = Modifier.fillMaxWidth()) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = label,
        color = Color(0xFF8A99AD),
        fontSize = 10.5.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.SansSerif,
      )
      Text(
        text = valueText,
        color = barColor,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        fontFamily = FontFamily.Monospace,
      )
    }
    Spacer(modifier = Modifier.height(2.dp))
    Box(
      modifier =
        Modifier.fillMaxWidth()
          .height(6.dp)
          .clip(RoundedCornerShape(3.dp))
          .background(Color(0xFF132642))
    ) {
      Box(
        modifier =
          Modifier.fillMaxWidth(fraction)
            .fillMaxHeight()
            .background(barColor, RoundedCornerShape(3.dp))
      )
    }
  }
}

@Composable
private fun ModeSelectorTab(
  title: String,
  statusSub: String,
  selected: Boolean,
  onClick: () -> Unit,
  tag: String,
) {
  Box(
    modifier =
      Modifier.clip(RoundedCornerShape(8.dp))
        .background(if (selected) Color(0xFFFFD700) else Color(0xFF0E1B2E))
        .border(
          1.dp,
          if (selected) Color(0xFFFFD700) else Color(0x33FFFFFF),
          RoundedCornerShape(8.dp),
        )
        .clickable { onClick() }
        .padding(horizontal = 9.dp, vertical = 5.dp)
        .testTag(tag),
  ) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Text(
        text = title,
        color = if (selected) Color(0xFF05080E) else Color.White,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.SansSerif,
      )
      Text(
        text = statusSub,
        color = if (selected) Color(0xFF05080E) else Color(0xFF8A99AD),
        fontSize = 8.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace,
      )
    }
  }
}
