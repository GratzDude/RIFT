package com.example.ui

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ArmadaMemberSlot
import com.example.data.FriendPilotEntry
import com.example.data.SyndicateChatMessage
import com.example.engine.ShipEntity
import kotlinx.coroutines.delay

@Composable
fun SyndicateChatModal(
  syndicateTag: String,
  syndicateName: String,
  activeArmadaName: String,
  activeArmadaCount: Int,
  armadaTierLabel: String = "SOLO",
  armadaSlots: List<ArmadaMemberSlot> = emptyList(),
  friendsList: List<FriendPilotEntry> = emptyList(),
  chatMessages: List<SyndicateChatMessage>,
  activeShipsInSector: List<ShipEntity>,
  onSendChatMessage: (String) -> Unit,
  onPostArmadaRecruitBanner: () -> Unit,
  onRequestInviteToArmada: (armadaName: String, leaderCallsign: String) -> Unit,
  onInviteFriendToArmada: (FriendPilotEntry) -> Unit = {},
  onRequestJoinFriendArmada: (FriendPilotEntry) -> Unit = {},
  onSimulateRosterShift4To2: () -> Unit = {},
  onLeaveArmadaToSolo: () -> Unit = {},
  onCloseModal: () -> Unit,
) {
  var drawerTab by remember { mutableIntStateOf(0) } // 0 = COMMS & QUICK-PING, 1 = ARMADA & FRIENDS
  var messageInput by remember { mutableStateOf("") }
  var inputFocused by remember { mutableStateOf(false) }

  // Phase 4.3: 10-second anti-spam cooldown state map for [⇥ REQUEST INVITE] & [+ INVITE TO ARMADA]
  val requestCooldownSec = remember { mutableStateMapOf<String, Int>() }

  LaunchedEffect(requestCooldownSec.keys.toList()) {
    while (requestCooldownSec.isNotEmpty()) {
      delay(1000L)
      val keys = requestCooldownSec.keys.toList()
      for (k in keys) {
        val rem = (requestCooldownSec[k] ?: 0) - 1
        if (rem <= 0) {
          requestCooldownSec.remove(k)
        } else {
          requestCooldownSec[k] = rem
        }
      }
    }
  }

  val focusManager = LocalFocusManager.current
  val keyboardController = LocalSoftwareKeyboardController.current

  val inSpaceArmadas =
    remember(activeShipsInSector) {
      activeShipsInSector
        .filter { it.armadaName.isNotEmpty() }
        .groupBy { it.armadaName }
    }

  // Phase 4.3: Left-Anchored Slide-Over Glass Drawer (width = min(380.dp, 85vw), rgba(8, 14, 26, 0.88) = 0xE0080E1A)
  // Keeping 65%+ of the live combat arena visible!
  Box(
    modifier =
      Modifier.fillMaxSize()
        .imePadding()
        .testTag("syndicate_chat_modal"),
  ) {
    // Right 65% transparent tap-to-close backdrop so arena remains 100% live and visible
    Box(
      modifier =
        Modifier.fillMaxSize()
          .clickable { onCloseModal() }
    )

    // Left Slide-Over Glass Drawer
    Surface(
      modifier =
        Modifier.align(Alignment.CenterStart)
          .widthIn(max = 380.dp)
          .fillMaxWidth(0.85f)
          .fillMaxHeight()
          .border(1.dp, Color(0x6638BDF8), RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp)),
      color = Color(0xE0080E1A), // rgba(8, 14, 26, 0.88)
      shape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp),
    ) {
      Column(
        modifier = Modifier.fillMaxSize().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        // 1. Compact Drawer Header
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "💬 [$syndicateTag] TACTICAL DRAWER",
              color = Color.White,
              fontSize = 15.sp,
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.SansSerif,
              letterSpacing = 1.1.sp,
            )
            val armadaStatus =
              if (activeArmadaName.isNotEmpty()) "[$activeArmadaName $activeArmadaCount/5 • $armadaTierLabel]" else "[SOLO]"
            Text(
              text = "$syndicateName • $armadaStatus",
              color = Color(0xFF38BDF8),
              fontSize = 10.5.sp,
              fontWeight = FontWeight.Medium,
              fontFamily = FontFamily.SansSerif,
            )
          }

          Button(
            onClick = onCloseModal,
            colors =
              ButtonDefaults.buttonColors(
                containerColor = Color(0xFF28111B),
                contentColor = Color(0xFFEF4444),
              ),
            border = BorderStroke(1.dp, Color(0xFFEF4444)),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.height(32.dp).testTag("chat_close_button"),
          ) {
            Text(
              text = "✕",
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.SansSerif,
            )
          }
        }

        // Mode Switcher inside the Slide-Over Glass Drawer: Comms vs. Armada & Friends
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          DrawerModePill(
            label = "💬 SYNDICATE COMMS",
            selected = drawerTab == 0,
            onClick = { drawerTab = 0 },
            modifier = Modifier.weight(1f),
            tag = "drawer_tab_comms",
          )
          DrawerModePill(
            label = "🛡️ ARMADA & FRIENDS (${friendsList.count { it.isOnline }})",
            selected = drawerTab == 1,
            onClick = { drawerTab = 1 },
            modifier = Modifier.weight(1f),
            tag = "drawer_tab_armada_friends",
          )
        }

        // 2. Phase 4.3: 1-Tap Tactical Quick-Ping Bar at top of drawer for zero-typing combat coordination
        val pingScroll = rememberScrollState()
        Row(
          modifier =
            Modifier.fillMaxWidth()
              .horizontalScroll(pingScroll),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          QuickPingChip(
            label = "🎯 ATTACK HVT",
            accent = Color(0xFFF59E0B),
            onClick = { onSendChatMessage("🎯 [QUICK-PING] CONVERGE & ATTACK SECTOR HVT NOW!") },
            tag = "quick_ping_attack_hvt",
          )
          QuickPingChip(
            label = "🛡️ FORM ON ME",
            accent = Color(0xFF38BDF8),
            onClick = { onSendChatMessage("🛡️ [QUICK-PING] FORM UP ON MY TRANSPONDER COORDINATES!") },
            tag = "quick_ping_form_on_me",
          )
          QuickPingChip(
            label = "⚡ LOW FLUX",
            accent = Color(0xFFEF4444),
            onClick = { onSendChatMessage("⚡ [QUICK-PING] CRITICAL LOW FLUX — COVER MY RETREAT TO SAFE ZONE!") },
            tag = "quick_ping_low_flux",
          )
          QuickPingChip(
            label = "🤝 NEED ARMADA",
            accent = Color(0xFF10B981),
            onClick = { onPostArmadaRecruitBanner() },
            tag = "post_armada_recruit_button",
          )
        }

        if (drawerTab == 0) {
          // 3A. Scrollable Comms Feed + In-Space Armada Telemetry Cards
          LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            if (inSpaceArmadas.isNotEmpty()) {
              item {
                Text(
                  text = "📡 IN-SPACE ARMADA TRANSPONDERS",
                  color = Color(0xFF94A3B8),
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold,
                  fontFamily = FontFamily.SansSerif,
                )
              }
              items(inSpaceArmadas.entries.toList(), key = { "telemetry_${it.key}" }) { (armName, members) ->
                val leader = members.first()
                val count = members.size.coerceIn(2, 5)
                val isMine = armName.equals(activeArmadaName, ignoreCase = true)
                val cdKey = "tele_$armName"
                val remSec = requestCooldownSec[cdKey] ?: 0
                val isCoolingDown = remSec > 0

                Row(
                  modifier =
                    Modifier.fillMaxWidth()
                      .background(Color(0xB30D1B30), RoundedCornerShape(8.dp))
                      .border(1.dp, Color(0x3338BDF8), RoundedCornerShape(8.dp))
                      .padding(8.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = "[$armName] ($count/5) • [${leader.syndicateTag}]",
                      color = Color(0xFF38BDF8),
                      fontSize = 11.sp,
                      fontWeight = FontWeight.Bold,
                      fontFamily = FontFamily.SansSerif,
                    )
                    Text(
                      text = members.joinToString(", ") { it.callsign },
                      color = Color(0xFF94A3B8),
                      fontSize = 9.5.sp,
                      fontFamily = FontFamily.Monospace,
                    )
                  }

                  // Phase 4.3 Inbound Join Request ([⇥ REQUEST INVITE]): Deep Indigo background with crisp Cyan border (#38BDF8)
                  Button(
                    onClick = {
                      requestCooldownSec[cdKey] = 10
                      onRequestInviteToArmada(armName, leader.callsign)
                    },
                    enabled = !isMine && !isCoolingDown && count < 5,
                    colors =
                      ButtonDefaults.buttonColors(
                        containerColor =
                          when {
                            isMine -> Color(0xFF1E3E24)
                            isCoolingDown -> Color(0xFF332409)
                            else -> Color(0xFF121A3A)
                          },
                        contentColor =
                          when {
                            isMine -> Color(0xFF10B981)
                            isCoolingDown -> Color(0xFFF59E0B)
                            else -> Color(0xFF38BDF8)
                          },
                        disabledContainerColor =
                          if (isMine) Color(0xFF1E3E24) else Color(0xFF332409),
                        disabledContentColor =
                          if (isMine) Color(0xFF10B981) else Color(0xFFF59E0B),
                      ),
                    border =
                      BorderStroke(
                        1.dp,
                        when {
                          isMine -> Color(0xFF10B981)
                          isCoolingDown -> Color(0xFFF59E0B)
                          else -> Color(0xFF38BDF8)
                        },
                      ),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.height(30.dp),
                  ) {
                    Text(
                      text =
                        when {
                          isMine -> "✓ ACTIVE"
                          isCoolingDown -> "⏳ INVITE SENT - ${remSec}s"
                          else -> "⇥ REQUEST INVITE"
                        },
                      fontSize = 8.5.sp,
                      fontWeight = FontWeight.Bold,
                      fontFamily = FontFamily.SansSerif,
                    )
                  }
                }
              }
            }

            items(chatMessages, key = { it.id }) { msg ->
              if (msg.isArmadaRecruitBanner) {
                val isAlreadyInThisArmada =
                  activeArmadaName.equals(msg.armadaName, ignoreCase = true)
                val cdKey = "banner_${msg.id}"
                val remSec = requestCooldownSec[cdKey] ?: 0
                val isCoolingDown = remSec > 0

                Column(
                  modifier =
                    Modifier.fillMaxWidth()
                      .background(Color(0xCC132944), RoundedCornerShape(9.dp))
                      .border(1.dp, Color(0x8838BDF8), RoundedCornerShape(9.dp))
                      .padding(9.dp),
                  verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    Text(
                      text = "🛡️ ARMADA [${msg.armadaName}] (${msg.armadaCurrentSize}/${msg.armadaMaxSize})",
                      color = Color(0xFFF59E0B),
                      fontSize = 11.5.sp,
                      fontWeight = FontWeight.Bold,
                      fontFamily = FontFamily.SansSerif,
                    )
                    Text(
                      text = "[${msg.senderSyndicateTag}] ${msg.senderCallsign}",
                      color = Color(0xFF38BDF8),
                      fontSize = 10.sp,
                      fontFamily = FontFamily.Monospace,
                    )
                  }
                  Text(
                    text = msg.armadaObjective,
                    color = Color.White,
                    fontSize = 10.5.sp,
                    fontFamily = FontFamily.SansSerif,
                  )

                  Button(
                    onClick = {
                      requestCooldownSec[cdKey] = 10
                      onRequestInviteToArmada(msg.armadaName, msg.senderCallsign)
                    },
                    enabled = !isAlreadyInThisArmada && !isCoolingDown && msg.armadaCurrentSize < msg.armadaMaxSize,
                    colors =
                      ButtonDefaults.buttonColors(
                        containerColor =
                          when {
                            isAlreadyInThisArmada -> Color(0xFF1E3E24)
                            isCoolingDown -> Color(0xFF332409)
                            else -> Color(0xFF121A3A)
                          },
                        contentColor =
                          when {
                            isAlreadyInThisArmada -> Color(0xFF10B981)
                            isCoolingDown -> Color(0xFFF59E0B)
                            else -> Color(0xFF38BDF8)
                          },
                        disabledContainerColor =
                          if (isAlreadyInThisArmada) Color(0xFF1E3E24) else Color(0xFF332409),
                        disabledContentColor =
                          if (isAlreadyInThisArmada) Color(0xFF10B981) else Color(0xFFF59E0B),
                      ),
                    border =
                      BorderStroke(
                        1.dp,
                        when {
                          isAlreadyInThisArmada -> Color(0xFF10B981)
                          isCoolingDown -> Color(0xFFF59E0B)
                          else -> Color(0xFF38BDF8)
                        },
                      ),
                    shape = RoundedCornerShape(7.dp),
                    modifier =
                      Modifier.fillMaxWidth()
                        .height(34.dp)
                        .testTag("request_invite_banner_${msg.armadaName}"),
                  ) {
                    Text(
                      text =
                        when {
                          isAlreadyInThisArmada -> "✓ IN ARMADA [${msg.armadaName}]"
                          isCoolingDown -> "⏳ INVITE SENT - ${remSec}s"
                          else -> "⇥ REQUEST INVITE"
                        },
                      fontSize = 10.sp,
                      fontWeight = FontWeight.Bold,
                      fontFamily = FontFamily.SansSerif,
                    )
                  }
                }
              } else {
                Column(
                  modifier =
                    Modifier.fillMaxWidth()
                      .background(Color(0x990B1626), RoundedCornerShape(8.dp))
                      .padding(horizontal = 9.dp, vertical = 6.dp)
                ) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                  ) {
                    Text(
                      text = "[${msg.senderSyndicateTag}] ${msg.senderCallsign}",
                      color = Color(0xFF38BDF8),
                      fontSize = 11.sp,
                      fontWeight = FontWeight.Bold,
                      fontFamily = FontFamily.SansSerif,
                    )
                    Text(
                      text = msg.timestampLabel,
                      color = Color(0xFF94A3B8),
                      fontSize = 9.sp,
                      fontFamily = FontFamily.Monospace,
                    )
                  }
                  Spacer(modifier = Modifier.height(2.dp))
                  Text(
                    text = msg.messageText,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.SansSerif,
                  )
                }
              }
            }
          }

          // 4. Chat Input Bar with [✓ DONE] Keyboard Compensation Button
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            OutlinedTextField(
              value = messageInput,
              onValueChange = { messageInput = it.take(120) },
              singleLine = true,
              keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
              keyboardActions =
                KeyboardActions(
                  onSend = {
                    if (messageInput.isNotBlank()) {
                      onSendChatMessage(messageInput.trim())
                      messageInput = ""
                    }
                    focusManager.clearFocus()
                    keyboardController?.hide()
                  }
                ),
              placeholder = {
                Text(
                  "Comms to [$syndicateTag]...",
                  fontSize = 10.sp,
                  fontFamily = FontFamily.SansSerif,
                )
              },
              textStyle =
                TextStyle(
                  color = Color.White,
                  fontSize = 11.5.sp,
                  fontFamily = FontFamily.SansSerif,
                ),
              colors =
                OutlinedTextFieldDefaults.colors(
                  focusedBorderColor = Color(0xFF38BDF8),
                  unfocusedBorderColor = Color(0xFF223B59),
                ),
              modifier =
                Modifier.weight(1f)
                  .onFocusChanged { inputFocused = it.isFocused }
                  .testTag("syndicate_chat_input"),
            )

            if (inputFocused) {
              Button(
                onClick = {
                  focusManager.clearFocus()
                  keyboardController?.hide()
                },
                colors =
                  ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF10B981),
                    contentColor = Color(0xFF05080E),
                  ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.height(44.dp).testTag("syndicate_chat_done_button"),
              ) {
                Text(
                  text = "✓ DONE",
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold,
                  fontFamily = FontFamily.SansSerif,
                )
              }
            }

            Button(
              onClick = {
                if (messageInput.isNotBlank()) {
                  onSendChatMessage(messageInput.trim())
                  messageInput = ""
                }
                focusManager.clearFocus()
                keyboardController?.hide()
              },
              colors =
                ButtonDefaults.buttonColors(
                  containerColor = Color(0xFF38BDF8),
                  contentColor = Color(0xFF05080E),
                ),
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier.height(44.dp).testTag("syndicate_chat_send_button"),
            ) {
              Text(
                text = "SEND",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.SansSerif,
              )
            }
          }
        } else {
          // 3B. Live Slide-Over Armada & Friends Roster inside the Left Drawer
          LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            item {
              Row(
                modifier =
                  Modifier.fillMaxWidth()
                    .background(Color(0x990D1B30), RoundedCornerShape(8.dp))
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text =
                      if (activeArmadaName.isEmpty()) {
                        "⚡ STATUS: [SOLO] PILOT"
                      } else {
                        "🛡️ [$activeArmadaName] (${armadaSlots.size}/5) • $armadaTierLabel"
                      },
                    color = if (activeArmadaName.isEmpty()) Color(0xFF38BDF8) else Color(0xFFF59E0B),
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif,
                  )
                  Text(
                    text = "Dynamic Tier Re-Bucketing (SOLO / 2-MAN / 3-MAN / 4-MAN / 5-MAN)",
                    color = Color(0xFF94A3B8),
                    fontSize = 9.sp,
                    fontFamily = FontFamily.SansSerif,
                  )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                  Button(
                    onClick = onSimulateRosterShift4To2,
                    colors =
                      ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF162B46),
                        contentColor = Color(0xFFF59E0B),
                      ),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.height(28.dp),
                  ) {
                    Text("4➔2 TIER", fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
                  }
                  if (activeArmadaName.isNotEmpty()) {
                    Button(
                      onClick = onLeaveArmadaToSolo,
                      colors =
                        ButtonDefaults.buttonColors(
                          containerColor = Color(0xFF281520),
                          contentColor = Color(0xFFEF4444),
                        ),
                      shape = RoundedCornerShape(6.dp),
                      modifier = Modifier.height(28.dp),
                    ) {
                      Text("SOLO", fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
                    }
                  }
                }
              }
            }

            items(friendsList, key = { "drawer_friend_${it.uid}" }) { friend ->
              val reqKey = "dreq_${friend.uid}"
              val invKey = "dinv_${friend.uid}"
              val reqRem = requestCooldownSec[reqKey] ?: 0
              val invRem = requestCooldownSec[invKey] ?: 0

              Column(
                modifier =
                  Modifier.fillMaxWidth()
                    .background(Color(0x990D1B30), RoundedCornerShape(8.dp))
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  Box(
                    modifier =
                      Modifier.size(8.dp)
                        .clip(CircleShape)
                        .background(if (friend.isOnline) Color(0xFF10B981) else Color(0xFF546E7A))
                  )
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text =
                        if (friend.currentArmadaName.isNotEmpty()) {
                          "[${friend.currentArmadaName}] [${friend.syndicateTag}] ${friend.callsign}"
                        } else {
                          "[SOLO] [${friend.syndicateTag}] ${friend.callsign}"
                        },
                      color = Color.White,
                      fontSize = 11.5.sp,
                      fontWeight = FontWeight.Bold,
                      fontFamily = FontFamily.SansSerif,
                    )
                    Text(
                      text = friend.statusText.uppercase(),
                      color = Color(0xFF94A3B8),
                      fontSize = 9.5.sp,
                      fontFamily = FontFamily.SansSerif,
                    )
                  }
                }

                if (friend.isOnline) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                  ) {
                    if (friend.currentArmadaName.isNotEmpty() && friend.currentArmadaCount < 5) {
                      val reqLocked = reqRem > 0
                      Button(
                        onClick = {
                          requestCooldownSec[reqKey] = 10
                          onRequestJoinFriendArmada(friend)
                        },
                        enabled = !reqLocked,
                        colors =
                          ButtonDefaults.buttonColors(
                            containerColor = if (reqLocked) Color(0xFF332409) else Color(0xFF121A3A),
                            contentColor = if (reqLocked) Color(0xFFF59E0B) else Color(0xFF38BDF8),
                            disabledContainerColor = Color(0xFF332409),
                            disabledContentColor = Color(0xFFF59E0B),
                          ),
                        border =
                          BorderStroke(
                            1.dp,
                            if (reqLocked) Color(0xFFF59E0B) else Color(0xFF38BDF8),
                          ),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.weight(1f).height(30.dp),
                      ) {
                        Text(
                          text = if (reqLocked) "⏳ INVITE SENT - ${reqRem}s" else "⇥ REQUEST INVITE",
                          fontSize = 8.5.sp,
                          fontWeight = FontWeight.Bold,
                        )
                      }
                    }

                    val invLocked = invRem > 0
                    Button(
                      onClick = {
                        requestCooldownSec[invKey] = 10
                        onInviteFriendToArmada(friend)
                      },
                      enabled = !invLocked,
                      colors =
                        ButtonDefaults.buttonColors(
                          containerColor = if (invLocked) Color(0xFF332409) else Color(0xFF10B981),
                          contentColor = if (invLocked) Color(0xFFF59E0B) else Color(0xFF05080E),
                          disabledContainerColor = Color(0xFF332409),
                          disabledContentColor = Color(0xFFF59E0B),
                        ),
                      shape = RoundedCornerShape(6.dp),
                      modifier = Modifier.weight(1f).height(30.dp),
                    ) {
                      Text(
                        text = if (invLocked) "⏳ INVITE SENT - ${invRem}s" else "+ INVITE TO ARMADA",
                        fontSize = 8.5.sp,
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
  }
}

@Composable
private fun DrawerModePill(
  label: String,
  selected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  tag: String,
) {
  Box(
    modifier =
      modifier
        .clip(RoundedCornerShape(8.dp))
        .background(if (selected) Color(0xFF38BDF8) else Color(0xFF102036))
        .border(
          1.dp,
          if (selected) Color(0xFF38BDF8) else Color(0x4438BDF8),
          RoundedCornerShape(8.dp),
        )
        .clickable { onClick() }
        .padding(vertical = 6.dp)
        .testTag(tag),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      text = label,
      color = if (selected) Color(0xFF05080E) else Color.White,
      fontSize = 10.sp,
      fontWeight = FontWeight.Bold,
      fontFamily = FontFamily.SansSerif,
    )
  }
}

@Composable
private fun QuickPingChip(
  label: String,
  accent: Color,
  onClick: () -> Unit,
  tag: String,
) {
  Box(
    modifier =
      Modifier.clip(RoundedCornerShape(8.dp))
        .background(Color(0xFF102036))
        .border(1.dp, accent, RoundedCornerShape(8.dp))
        .clickable { onClick() }
        .padding(horizontal = 8.dp, vertical = 5.dp)
        .testTag(tag),
  ) {
    Text(
      text = label,
      color = accent,
      fontSize = 9.5.sp,
      fontWeight = FontWeight.Bold,
      fontFamily = FontFamily.SansSerif,
    )
  }
}
