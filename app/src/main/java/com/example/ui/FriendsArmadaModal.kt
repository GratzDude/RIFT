package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ArmadaMemberSlot
import com.example.data.FriendPilotEntry

@Composable
fun FriendsArmadaModal(
  activeArmadaName: String,
  armadaTierLabel: String,
  armadaSlots: List<ArmadaMemberSlot>,
  friendsList: List<FriendPilotEntry>,
  statusBanner: String?,
  onCreateOrRenameArmada: (String) -> Unit,
  onInviteFriendToArmada: (FriendPilotEntry) -> Unit,
  onRequestJoinFriendArmada: (FriendPilotEntry) -> Unit,
  onRemoveArmadaMember: (String) -> Unit,
  onSimulateRosterShift4To2: () -> Unit,
  onLeaveArmadaToSolo: () -> Unit,
  onAddFriendByCallsign: (String) -> Unit,
  onCloseModal: () -> Unit,
) {
  var armadaNameInput by remember(activeArmadaName) {
    mutableStateOf(activeArmadaName.ifEmpty { "APEX_5" })
  }
  var newFriendInput by remember { mutableStateOf("") }

  Box(
    modifier =
      Modifier.fillMaxSize()
        .background(Color(0xB3000000))
        .padding(horizontal = 24.dp, vertical = 14.dp)
        .testTag("friends_armada_modal"),
    contentAlignment = Alignment.Center,
  ) {
    Surface(
      modifier =
        Modifier.fillMaxSize()
          .border(1.dp, Color(0x6600F5D4), RoundedCornerShape(16.dp)),
      color = Color(0xE60A0F18), // #0A0F18 with 90% opacity glassmorphism
      shape = RoundedCornerShape(16.dp),
    ) {
      Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // Header Row
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Column {
            Text(
              text = "👥 FRIENDS LIST & FLUID 5-MAN ARMADA COMMAND",
              color = Color(0xFF00F5D4),
              fontSize = 15.sp,
              fontWeight = FontWeight.ExtraBold,
              fontFamily = FontFamily.Monospace,
            )
            Text(
              text = "ACTIVE TELEMETRY TIER: $armadaTierLabel • FRIENDLY FIRE DISABLED FOR ARMADA & SYNDICATE",
              color = Color(0xFFFFE800),
              fontSize = 10.sp,
              fontFamily = FontFamily.Monospace,
            )
          }

          Button(
            onClick = onCloseModal,
            colors =
              ButtonDefaults.buttonColors(
                containerColor = Color(0xFF28111B),
                contentColor = Color(0xFFFF3B4E),
              ),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.height(36.dp).testTag("friends_close_button"),
          ) {
            Text(
              text = "✕ CLOSE",
              fontSize = 11.sp,
              fontWeight = FontWeight.ExtraBold,
              fontFamily = FontFamily.Monospace,
            )
          }
        }

        if (!statusBanner.isNullOrBlank()) {
          Spacer(modifier = Modifier.height(6.dp))
          Box(
            modifier =
              Modifier.fillMaxWidth()
                .background(Color(0xFF102338), RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 5.dp)
          ) {
            Text(
              text = statusBanner,
              color = Color(0xFF00F5D4),
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.Monospace,
            )
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
          modifier = Modifier.weight(1f).fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
          // Left Column: Active Named Armada Roster (2 to 5 Players or [SOLO])
          Column(
            modifier =
              Modifier.width(310.dp)
                .fillMaxHeight()
                .background(Color(0x99111C2D), RoundedCornerShape(12.dp))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Text(
              text =
                if (activeArmadaName.isEmpty()) {
                  "⚡ CURRENT STATUS: [SOLO] PILOT"
                } else {
                  "🛡️ ARMADA [$activeArmadaName] (${armadaSlots.size}/5)"
                },
              color = if (activeArmadaName.isEmpty()) Color(0xFF90CAF9) else Color(0xFFFFE800),
              fontSize = 12.sp,
              fontWeight = FontWeight.ExtraBold,
              fontFamily = FontFamily.Monospace,
            )

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(6.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              OutlinedTextField(
                value = armadaNameInput,
                onValueChange = { raw ->
                  armadaNameInput = raw.uppercase().replace(Regex("[^A-Z0-9_-]"), "").take(10)
                },
                singleLine = true,
                label = { Text("ARMADA TAG", fontSize = 9.sp, fontFamily = FontFamily.Monospace) },
                textStyle =
                  TextStyle(
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                  ),
                colors =
                  OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF00F5D4),
                    unfocusedBorderColor = Color(0xFF223B59),
                  ),
                modifier = Modifier.weight(1f).testTag("armada_name_input"),
              )

              Button(
                onClick = { onCreateOrRenameArmada(armadaNameInput) },
                colors =
                  ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF00F5D4),
                    contentColor = Color(0xFF04101E),
                  ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.height(44.dp).testTag("form_armada_button"),
              ) {
                Text(
                  text = if (activeArmadaName.isEmpty()) "FORM" else "RENAME",
                  fontSize = 10.sp,
                  fontWeight = FontWeight.ExtraBold,
                  fontFamily = FontFamily.Monospace,
                )
              }
            }

            LazyColumn(
              modifier = Modifier.weight(1f).fillMaxWidth(),
              verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              if (armadaSlots.isEmpty()) {
                item {
                  Box(
                    modifier =
                      Modifier.fillMaxWidth()
                        .background(Color(0x660B1524), RoundedCornerShape(8.dp))
                        .padding(10.dp)
                  ) {
                    Text(
                      text =
                        "Flying [SOLO] in the FFA Rift.\nInvite friends on the right to form a 2-to-5-man Named Armada.",
                      color = Color(0xFFB0BEC5),
                      fontSize = 10.sp,
                      fontFamily = FontFamily.Monospace,
                    )
                  }
                }
              } else {
                items(armadaSlots, key = { it.uid }) { slot ->
                  Row(
                    modifier =
                      Modifier.fillMaxWidth()
                        .background(Color(0x990D1B30), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                  ) {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                      ShipSpritePreview(shipId = slot.shipId, sizeDp = 24)
                      Column {
                        Text(
                          text = "[${slot.syndicateTag}] ${slot.callsign}",
                          color = if (slot.isLeader) Color(0xFFFFE800) else Color.White,
                          fontSize = 11.sp,
                          fontWeight = FontWeight.ExtraBold,
                          fontFamily = FontFamily.Monospace,
                        )
                        Text(
                          text = if (slot.isLeader) "ARMADA COMMANDER" else "WINGMAN ACTIVE",
                          color = Color(0xFF00F5D4),
                          fontSize = 8.5.sp,
                          fontFamily = FontFamily.Monospace,
                        )
                      }
                    }

                    if (!slot.isLeader) {
                      Button(
                        onClick = { onRemoveArmadaMember(slot.callsign) },
                        colors =
                          ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2D1420),
                            contentColor = Color(0xFFFF3B4E),
                          ),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.height(30.dp),
                      ) {
                        Text(
                          text = "DROP",
                          fontSize = 9.sp,
                          fontWeight = FontWeight.Bold,
                          fontFamily = FontFamily.Monospace,
                        )
                      }
                    }
                  }
                }
              }
            }

            // Adaptive Roster Shifting & Solo Controls
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              Button(
                onClick = onSimulateRosterShift4To2,
                colors =
                  ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF162B46),
                    contentColor = Color(0xFFFFE800),
                  ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f).height(38.dp).testTag("simulate_roster_shift_button"),
              ) {
                Text(
                  text = "⚡ SHIFT 4➔2 TIER",
                  fontSize = 9.5.sp,
                  fontWeight = FontWeight.ExtraBold,
                  fontFamily = FontFamily.Monospace,
                )
              }

              if (activeArmadaName.isNotEmpty()) {
                Button(
                  onClick = onLeaveArmadaToSolo,
                  colors =
                    ButtonDefaults.buttonColors(
                      containerColor = Color(0xFF281520),
                      contentColor = Color(0xFFFF3B4E),
                    ),
                  shape = RoundedCornerShape(8.dp),
                  modifier = Modifier.height(38.dp).testTag("leave_armada_solo_button"),
                ) {
                  Text(
                    text = "FLY [SOLO]",
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace,
                  )
                }
              }
            }
          }

          // Right Column: Friends List, Live Presence & Armada Invites
          Column(
            modifier =
              Modifier.weight(1f)
                .fillMaxHeight()
                .background(Color(0x99111C2D), RoundedCornerShape(12.dp))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Text(
                text = "ONLINE FRIENDS & SYNDICATE PRESENCE (${friendsList.count { it.isOnline }} ONLINE)",
                color = Color(0xFF00F5D4),
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
              )

              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                OutlinedTextField(
                  value = newFriendInput,
                  onValueChange = { raw ->
                    newFriendInput = raw.uppercase().replace(Regex("[^A-Z0-9_-]"), "").take(14)
                  },
                  singleLine = true,
                  placeholder = {
                    Text("ADD CALLSIGN", fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                  },
                  textStyle =
                    TextStyle(
                      color = Color.White,
                      fontSize = 11.sp,
                      fontFamily = FontFamily.Monospace,
                    ),
                  colors =
                    OutlinedTextFieldDefaults.colors(
                      focusedBorderColor = Color(0xFF00F5D4),
                      unfocusedBorderColor = Color(0xFF223B59),
                    ),
                  modifier = Modifier.width(150.dp).height(46.dp),
                )

                Button(
                  onClick = {
                    if (newFriendInput.length >= 3) {
                      onAddFriendByCallsign(newFriendInput)
                      newFriendInput = ""
                    }
                  },
                  colors =
                    ButtonDefaults.buttonColors(
                      containerColor = Color(0xFF00F5D4),
                      contentColor = Color(0xFF04101E),
                    ),
                  shape = RoundedCornerShape(8.dp),
                  modifier = Modifier.height(40.dp),
                ) {
                  Text(
                    text = "+ ADD",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace,
                  )
                }
              }
            }

            LazyColumn(
              modifier = Modifier.weight(1f).fillMaxWidth(),
              verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              items(friendsList, key = { it.uid }) { friend ->
                Row(
                  modifier =
                    Modifier.fillMaxWidth()
                      .background(Color(0x990D1B30), RoundedCornerShape(10.dp))
                      .padding(horizontal = 12.dp, vertical = 8.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f),
                  ) {
                    Box(
                      modifier =
                        Modifier.size(10.dp)
                          .clip(CircleShape)
                          .background(if (friend.isOnline) Color(0xFF00E676) else Color(0xFF546E7A))
                    )
                    ShipSpritePreview(shipId = friend.shipId, sizeDp = 28)
                    Column {
                      Text(
                        text =
                          if (friend.currentArmadaName.isNotEmpty()) {
                            "[${friend.currentArmadaName}] [${friend.syndicateTag}] ${friend.callsign}"
                          } else {
                            "[SOLO] [${friend.syndicateTag}] ${friend.callsign}"
                          },
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace,
                      )
                      Text(
                        text = friend.statusText,
                        color = if (friend.isOnline) Color(0xFF90CAF9) else Color(0xFF607D8B),
                        fontSize = 9.5.sp,
                        fontFamily = FontFamily.Monospace,
                      )
                    }
                  }

                  if (friend.isOnline) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                      if (friend.currentArmadaName.isNotEmpty() && friend.currentArmadaCount < 5) {
                        Button(
                          onClick = { onRequestJoinFriendArmada(friend) },
                          colors =
                            ButtonDefaults.buttonColors(
                              containerColor = Color(0xFF18314F),
                              contentColor = Color(0xFFFFE800),
                            ),
                          shape = RoundedCornerShape(8.dp),
                          modifier = Modifier.height(34.dp),
                        ) {
                          Text(
                            text = "REQUEST INVITE [${friend.currentArmadaName}]",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace,
                          )
                        }
                      }

                      Button(
                        onClick = { onInviteFriendToArmada(friend) },
                        colors =
                          ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF00F5D4),
                            contentColor = Color(0xFF04101E),
                          ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(34.dp).testTag("invite_friend_${friend.callsign}"),
                      ) {
                        Text(
                          text = "+ INVITE TO ARMADA",
                          fontSize = 9.5.sp,
                          fontWeight = FontWeight.ExtraBold,
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
  }
}
