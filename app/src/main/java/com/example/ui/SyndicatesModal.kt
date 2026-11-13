package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.draw.alpha
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
import com.example.data.PilotAchievement
import com.example.data.SyndicateInfo
import com.example.data.SyndicateMemberEntry
import com.example.engine.RiftCodex
import com.example.engine.RiftGameEngine
import com.example.engine.ShipEntity
import kotlinx.coroutines.delay

@Composable
fun SyndicatesModal(
  currentSyndicateTag: String,
  currentSyndicateName: String,
  remainingLockDays: Int,
  remainingLockFormatted: String,
  directoryList: List<SyndicateInfo>,
  rosterMembers: List<SyndicateMemberEntry>,
  activeShipsInSector: List<ShipEntity>,
  achievements: List<PilotAchievement>,
  statusBannerMessage: String?,
  onJoinSyndicate: (SyndicateInfo) -> Unit,
  onLeaveSyndicate: () -> Unit,
  onCreateCustomSyndicate: (tag: String, name: String, motto: String) -> Unit,
  onCloseModal: () -> Unit,
) {
  var activeTab by remember { mutableIntStateOf(0) }
  val isGuestSyndicate = currentSyndicateTag == "GUEST" || currentSyndicateTag == "FREE"

  Box(
    modifier =
      Modifier.fillMaxSize()
        .background(Color(0xB3000000))
        .imePadding()
        .padding(horizontal = 20.dp, vertical = 10.dp)
        .testTag("syndicates_modal"),
    contentAlignment = Alignment.Center,
  ) {
    Surface(
      modifier =
        Modifier.fillMaxSize()
          .border(1.dp, Color(0x6638BDF8), RoundedCornerShape(16.dp)),
      color = Color(0xE60A0F18), // #0A0F18 with 90% opacity glassmorphism
      shape = RoundedCornerShape(16.dp),
    ) {
      Column(modifier = Modifier.fillMaxSize().padding(14.dp)) {
        // 1. Header — Dedicated exclusively to Official Syndicate Allegiance & Standings
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Column {
            Text(
              text = "🏛️ SYNDICATE COMMAND // [$currentSyndicateTag] $currentSyndicateName",
              color = Color.White,
              fontSize = 18.sp,
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.SansSerif,
              letterSpacing = 1.2.sp,
            )
            Text(
              text =
                if (remainingLockDays > 0) {
                  "🔒 15-DAY SYNDICATE ALLEGIANCE LOCK ACTIVE: $remainingLockFormatted REMAINING"
                } else if (isGuestSyndicate) {
                  "⚠️ PLACEHOLDER [GUEST] SYNDICATE (ZERO PERKS / ZERO VAULT) • JOIN OR CHARTER TO UNLOCK"
                } else {
                  "🟢 ALLEGIANCE UNLOCKED (15d 00h COOLDOWN ON SWITCH)"
                },
              color =
                if (remainingLockDays > 0 || isGuestSyndicate) {
                  Color(0xFFF59E0B)
                } else {
                  Color(0xFF38BDF8)
                },
              fontSize = 11.5.sp,
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
            modifier = Modifier.height(36.dp).testTag("syndicates_close_button"),
          ) {
            Text(
              text = "✕ CLOSE",
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.SansSerif,
            )
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 4 Dedicated Syndicate Command Tabs (Decoupled from Armada Fleet Deck)
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          SyndicateTabButton(
            label = "1. DIRECTORY & JOIN",
            selected = activeTab == 0,
            onClick = { activeTab = 0 },
            tag = "syndicate_tab_directory",
          )
          SyndicateTabButton(
            label = "2. MY SYNDICATE ROSTER",
            selected = activeTab == 1,
            onClick = { activeTab = 1 },
            tag = "syndicate_tab_roster",
          )
          SyndicateTabButton(
            label = "3. SYNDICATE BOUNTIES & MEDALS",
            selected = activeTab == 2,
            onClick = { activeTab = 2 },
            tag = "syndicate_tab_bounties",
          )
          SyndicateTabButton(
            label = "4. CREATE SYNDICATE",
            selected = activeTab == 3,
            onClick = { activeTab = 3 },
            tag = "syndicate_tab_create",
          )
        }

        // Guest Syndicate Placeholder Notice Banner
        if (isGuestSyndicate) {
          Spacer(modifier = Modifier.height(6.dp))
          Box(
            modifier =
              Modifier.fillMaxWidth()
                .background(Color(0xFF1F1608), RoundedCornerShape(8.dp))
                .border(1.dp, Color(0x88F59E0B), RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp)
          ) {
            Text(
              text =
                "ℹ️ [GUEST] SYNDICATE FRAMEWORK: Offers zero perks or shared vault benefits. Join or charter an active player Syndicate below to break free from [GUEST] (subject to 15-day allegiance lock).",
              color = Color(0xFFFDE68A),
              fontSize = 11.sp,
              fontWeight = FontWeight.Medium,
              fontFamily = FontFamily.SansSerif,
            )
          }
        }

        if (!statusBannerMessage.isNullOrBlank()) {
          Spacer(modifier = Modifier.height(6.dp))
          Box(
            modifier =
              Modifier.fillMaxWidth()
                .background(Color(0xFF102338), RoundedCornerShape(8.dp))
                .border(1.dp, Color(0x5538BDF8), RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp)
          ) {
            Text(
              text = statusBannerMessage,
              color = Color(0xFF38BDF8),
              fontSize = 12.sp,
              fontWeight = FontWeight.Medium,
              fontFamily = FontFamily.SansSerif,
            )
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Tab Body
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
          when (activeTab) {
            0 ->
              DirectoryAndJoinPane(
                currentSyndicateTag = currentSyndicateTag,
                remainingLockDays = remainingLockDays,
                remainingLockFormatted = remainingLockFormatted,
                directoryList = directoryList,
                onJoinSyndicate = onJoinSyndicate,
              )
            1 ->
              MySyndicateRosterPane(
                currentSyndicateTag = currentSyndicateTag,
                currentSyndicateName = currentSyndicateName,
                remainingLockDays = remainingLockDays,
                remainingLockFormatted = remainingLockFormatted,
                rosterMembers = rosterMembers,
                onLeaveSyndicate = onLeaveSyndicate,
              )
            2 ->
              BountyCalloutsAndAchievementsPane(
                activeShipsInSector = activeShipsInSector,
                achievements = achievements,
              )
            else ->
              CreateSyndicatePane(
                remainingLockDays = remainingLockDays,
                remainingLockFormatted = remainingLockFormatted,
                onCreateCustomSyndicate = onCreateCustomSyndicate,
              )
          }
        }
      }
    }
  }
}

/**
 * Requirement 1: Dedicated [👥 ARMADA FLEET] Modal
 * Completely separated from Syndicate Command, dedicated exclusively to managing your 1-to-5-player strike team (Armada)
 * with explicit status indicators ([STATUS: DEPLOYED IN ARMADA: NOVA_STRIKE] vs. [STATUS: OPERATING INDEPENDENTLY])
 * and unambiguous [CREATE ARMADA], [JOIN ARMADA], [LEAVE ARMADA], and [REQUEST INVITE] actions.
 */
@Composable
fun ArmadaFleetModal(
  currentSyndicateTag: String,
  activeArmadaName: String,
  armadaTierLabel: String,
  armadaSlots: List<ArmadaMemberSlot>,
  friendsList: List<FriendPilotEntry>,
  blockedPilotUids: Set<String> = emptySet(),
  statusBannerMessage: String?,
  onCreateOrRenameArmada: (String) -> Unit,
  onInviteFriendToArmada: (FriendPilotEntry) -> Unit,
  onRequestJoinFriendArmada: (FriendPilotEntry) -> Unit,
  onRemoveArmadaMember: (String) -> Unit,
  onSimulateRosterShift4To2: () -> Unit,
  onLeaveArmadaToSolo: () -> Unit,
  onAddFriendByCallsign: (String) -> Unit,
  onBlockPilot: (uid: String, callsign: String) -> Unit = { _, _ -> },
  onReportPilot: (uid: String, callsign: String) -> Unit = { _, _ -> },
  onCloseModal: () -> Unit,
) {
  val isDeployedInArmada = activeArmadaName.isNotEmpty() && armadaSlots.size >= 1

  Box(
    modifier =
      Modifier.fillMaxSize()
        .background(Color(0xB3000000))
        .imePadding()
        .padding(horizontal = 20.dp, vertical = 10.dp)
        .testTag("armada_fleet_modal"),
    contentAlignment = Alignment.Center,
  ) {
    Surface(
      modifier =
        Modifier.fillMaxSize()
          .border(1.dp, Color(0x663B82F6), RoundedCornerShape(16.dp)),
      color = Color(0xE60A0F18),
      shape = RoundedCornerShape(16.dp),
    ) {
      Column(modifier = Modifier.fillMaxSize().padding(14.dp)) {
        // 1. Header with Explicit Armada Membership Status Indicator
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "👥 ARMADA FLEET DECK // 1-TO-5-PLAYER STRIKE TEAM COMMAND",
              color = Color.White,
              fontSize = 18.sp,
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.SansSerif,
              letterSpacing = 1.2.sp,
            )
            Text(
              text =
                if (isDeployedInArmada) {
                  "🟢 [STATUS: DEPLOYED IN ARMADA: $activeArmadaName] (${armadaSlots.size}/5 PILOTS • $armadaTierLabel)"
                } else {
                  "⚡ [STATUS: OPERATING INDEPENDENTLY] (SOLO RIFT • FRIENDLY FIRE ACTIVE AGAINST NON-ALLIES)"
                },
              color = if (isDeployedInArmada) Color(0xFF10B981) else Color(0xFF60A5FA),
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.Monospace,
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
            modifier = Modifier.height(36.dp).testTag("armada_modal_close_button"),
          ) {
            Text(
              text = "✕ CLOSE",
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.SansSerif,
            )
          }
        }

        if (!statusBannerMessage.isNullOrBlank()) {
          Spacer(modifier = Modifier.height(6.dp))
          Box(
            modifier =
              Modifier.fillMaxWidth()
                .background(Color(0xFF102338), RoundedCornerShape(8.dp))
                .border(1.dp, Color(0x553B82F6), RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp)
          ) {
            Text(
              text = statusBannerMessage,
              color = Color(0xFF60A5FA),
              fontSize = 12.sp,
              fontWeight = FontWeight.Medium,
              fontFamily = FontFamily.SansSerif,
            )
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
          ArmadaAndFriendsTabPane(
            activeArmadaName = activeArmadaName,
            armadaTierLabel = armadaTierLabel,
            armadaSlots = armadaSlots,
            friendsList = friendsList,
            blockedPilotUids = blockedPilotUids,
            onCreateOrRenameArmada = onCreateOrRenameArmada,
            onInviteFriendToArmada = onInviteFriendToArmada,
            onRequestJoinFriendArmada = onRequestJoinFriendArmada,
            onRemoveArmadaMember = onRemoveArmadaMember,
            onSimulateRosterShift4To2 = onSimulateRosterShift4To2,
            onLeaveArmadaToSolo = onLeaveArmadaToSolo,
            onAddFriendByCallsign = onAddFriendByCallsign,
            onBlockPilot = onBlockPilot,
            onReportPilot = onReportPilot,
          )
        }
      }
    }
  }
}

@Composable
private fun DirectoryAndJoinPane(
  currentSyndicateTag: String,
  remainingLockDays: Int,
  remainingLockFormatted: String,
  directoryList: List<SyndicateInfo>,
  onJoinSyndicate: (SyndicateInfo) -> Unit,
) {
  LazyColumn(
    modifier =
      Modifier.fillMaxSize()
        .background(Color(0x99111C2D), RoundedCornerShape(12.dp))
        .padding(10.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    items(directoryList, key = { it.tag }) { syn ->
      val isMember = syn.tag == currentSyndicateTag
      Row(
        modifier =
          Modifier.fillMaxWidth()
            .background(
              if (isMember) Color(0xCC142C4C) else Color(0x990D1B30),
              RoundedCornerShape(10.dp),
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Text(
              text = "[${syn.tag}] ${syn.name}",
              color = if (isMember) Color(0xFFFFD700) else Color(0xFF00F0FF),
              fontSize = 15.sp,
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.SansSerif,
            )
            Text(
              text = "${syn.memberCount} PILOTS • ${syn.combinedBounty} TOTAL BOUNTY",
              color = Color(0xFFFFD700),
              fontSize = 13.sp,
              fontWeight = FontWeight.Medium,
              fontFamily = FontFamily.Monospace,
            )
          }
          Spacer(modifier = Modifier.height(3.dp))
          Text(
            text = syn.motto.uppercase(),
            color = Color(0xFF8A99AD),
            fontSize = 11.sp,
            fontFamily = FontFamily.SansSerif,
          )
        }

        if (isMember) {
          Box(
            modifier =
              Modifier.background(Color(0xFF1E3E24), RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp)
          ) {
            Text(
              text = "✓ ENLISTED",
              color = Color(0xFF00E676),
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.SansSerif,
            )
          }
        } else {
          Button(
            onClick = { onJoinSyndicate(syn) },
            enabled = remainingLockDays <= 0,
            colors =
              ButtonDefaults.buttonColors(
                containerColor = Color(0xFF00F0FF),
                contentColor = Color(0xFF05080E),
                disabledContainerColor = Color(0xFF1A283B),
                disabledContentColor = Color(0xFF8A99AD),
              ),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.height(38.dp).testTag("join_syndicate_${syn.tag}"),
          ) {
            Text(
              text =
                if (remainingLockDays > 0) {
                  "LOCKED ($remainingLockFormatted)"
                } else {
                  "JOIN [${syn.tag}]"
                },
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.SansSerif,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun MySyndicateRosterPane(
  currentSyndicateTag: String,
  currentSyndicateName: String,
  remainingLockDays: Int,
  remainingLockFormatted: String,
  rosterMembers: List<SyndicateMemberEntry>,
  onLeaveSyndicate: () -> Unit,
) {
  Column(
    modifier =
      Modifier.fillMaxSize()
        .background(Color(0x99111C2D), RoundedCornerShape(12.dp))
        .padding(12.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Text(
        text = "ACTIVE ROSTER // [$currentSyndicateTag] $currentSyndicateName (${rosterMembers.size} MEMBERS)",
        color = Color.White,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.SansSerif,
      )

      if (currentSyndicateTag != "FREE" && currentSyndicateTag != "GUEST") {
        Button(
          onClick = onLeaveSyndicate,
          enabled = remainingLockDays <= 0,
          colors =
            ButtonDefaults.buttonColors(
              containerColor = Color(0xFF2B121D),
              contentColor = Color(0xFFFF3B4E),
              disabledContainerColor = Color(0xFF1A283B),
              disabledContentColor = Color(0xFF8A99AD),
            ),
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier.height(34.dp).testTag("leave_syndicate_button"),
        ) {
          Text(
            text =
              if (remainingLockDays > 0) {
                "LEAVE LOCKED ($remainingLockFormatted)"
              } else {
                "LEAVE SYNDICATE ➔ [GUEST] (15d 00h LOCK)"
              },
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.SansSerif,
          )
        }
      }
    }
    Spacer(modifier = Modifier.height(8.dp))
    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
      items(rosterMembers, key = { it.uid }) { member ->
        val shipSpec = RiftCodex.getShipById(member.shipId)
        Row(
          modifier =
            Modifier.fillMaxWidth()
              .background(Color(0x990D1B30), RoundedCornerShape(8.dp))
              .padding(horizontal = 12.dp, vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            ShipSpritePreview(shipId = member.shipId, sizeDp = 30)
            Column {
              Text(
                text = "[$currentSyndicateTag] ${member.callsign} • ${member.role}",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.SansSerif,
              )
              Text(
                text = "HULL: ${shipSpec.name.uppercase()} (${shipSpec.role.uppercase()})",
                color = Color(0xFF8A99AD),
                fontSize = 11.sp,
                fontFamily = FontFamily.SansSerif,
              )
            }
          }
          Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(
              text = "KILLS: ${member.kills}",
              color = Color(0xFF00F0FF),
              fontSize = 14.sp,
              fontWeight = FontWeight.Medium,
              fontFamily = FontFamily.Monospace,
            )
            Text(
              text = "BOUNTY: ${member.bountyContribution}",
              color = Color(0xFFFFD700),
              fontSize = 14.sp,
              fontWeight = FontWeight.Medium,
              fontFamily = FontFamily.Monospace,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun BountyCalloutsAndAchievementsPane(
  activeShipsInSector: List<ShipEntity>,
  achievements: List<PilotAchievement>,
) {
  val hvts =
    remember(activeShipsInSector) {
      activeShipsInSector
        .filter { it.bounty >= RiftGameEngine.HVT_BOUNTY_THRESHOLD }
        .sortedByDescending { it.bounty }
    }

  Row(
    modifier = Modifier.fillMaxSize(),
    horizontalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    // Left Half: Sector High-Value Targets (Bounty >= 250)
    Column(
      modifier =
        Modifier.weight(1f)
          .fillMaxSize()
          .background(Color(0x99111C2D), RoundedCornerShape(12.dp))
          .padding(10.dp)
    ) {
      Text(
        text = "★ SECTOR HIGH-VALUE TARGETS (BOUNTY >= 250)",
        color = Color(0xFFFFD700),
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.SansSerif,
      )
      Text(
        text = "ELIMINATE HVTS FOR MASSIVE BOUNTY PAYOUTS & GOLD SONAR PINGS.",
        color = Color(0xFF8A99AD),
        fontSize = 11.sp,
        fontFamily = FontFamily.SansSerif,
      )
      Spacer(modifier = Modifier.height(8.dp))
      LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        items(hvts, key = { it.id }) { hvt ->
          Row(
            modifier =
              Modifier.fillMaxWidth()
                .background(Color(0x991A1812), RoundedCornerShape(8.dp))
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              ShipSpritePreview(shipId = hvt.shipId, sizeDp = 28)
              Column {
                val armTag = if (hvt.armadaName.isNotEmpty()) "[${hvt.armadaName}]" else "[SOLO]"
                Text(
                  text = "★ $armTag [${hvt.syndicateTag}] ${hvt.callsign}",
                  color = Color(0xFFFFD700),
                  fontSize = 13.sp,
                  fontWeight = FontWeight.Bold,
                  fontFamily = FontFamily.SansSerif,
                )
                Text(
                  text =
                    "HULL: ${hvt.spec.name.uppercase()} • SECTOR (${(hvt.x / 500f).toInt()},${(hvt.y / 500f).toInt()})",
                  color = Color(0xFF8A99AD),
                  fontSize = 11.sp,
                  fontFamily = FontFamily.SansSerif,
                )
              }
            }
            Text(
              text = "${hvt.bounty} BTY",
              color = Color(0xFFFF3B4E),
              fontSize = 14.sp,
              fontWeight = FontWeight.Medium,
              fontFamily = FontFamily.Monospace,
            )
          }
        }
      }
    }

    // Right Half: Pilot Achievements
    Column(
      modifier =
        Modifier.weight(1f)
          .fillMaxSize()
          .background(Color(0x99111C2D), RoundedCornerShape(12.dp))
          .padding(10.dp)
    ) {
      Text(
        text = "🏆 PILOT COMBAT ACHIEVEMENTS",
        color = Color(0xFF00F0FF),
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.SansSerif,
      )
      Spacer(modifier = Modifier.height(8.dp))
      LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        items(achievements, key = { it.id }) { ach ->
          Row(
            modifier =
              Modifier.fillMaxWidth()
                .background(
                  if (ach.unlocked) Color(0x990E2938) else Color(0x990D1B30),
                  RoundedCornerShape(8.dp),
                )
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = if (ach.unlocked) "✓ ${ach.title}" else "🔒 ${ach.title}",
                color = if (ach.unlocked) Color(0xFF00E676) else Color(0xFF00F0FF),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.SansSerif,
              )
              Text(
                text = ach.description.uppercase(),
                color = Color(0xFF8A99AD),
                fontSize = 10.5.sp,
                fontFamily = FontFamily.SansSerif,
              )
            }
            Text(
              text = ach.progressText,
              color = Color(0xFFFFD700),
              fontSize = 13.sp,
              fontWeight = FontWeight.Medium,
              fontFamily = FontFamily.Monospace,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun CreateSyndicatePane(
  remainingLockDays: Int,
  remainingLockFormatted: String,
  onCreateCustomSyndicate: (tag: String, name: String, motto: String) -> Unit,
) {
  // P0 CRITICAL FIX: Pre-Mount Form Lockout on 15-Day Syndicate Cooldown!
  // Check remainingLockDays > 0 BEFORE enabling form inputs.
  val isCooldownLocked = remainingLockDays > 0

  var tagInput by remember { mutableStateOf("NOVA") }
  var nameInput by remember { mutableStateOf("NOVA VANGUARD") }
  var mottoInput by remember { mutableStateOf("Strike from the event horizon.") }
  var anyFieldFocused by remember { mutableStateOf(false) }

  val focusManager = LocalFocusManager.current
  val keyboardController = LocalSoftwareKeyboardController.current

  Column(
    modifier =
      Modifier.fillMaxSize()
        .background(Color(0x99111C2D), RoundedCornerShape(12.dp))
        .padding(14.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    // Prominent Amber Lock Banner at the top of the tab when 15-day cooldown is active
    if (isCooldownLocked) {
      Box(
        modifier =
          Modifier.fillMaxWidth()
            .background(Color(0xFF2B1C08), RoundedCornerShape(10.dp))
            .border(1.5.dp, Color(0xFFF59E0B), RoundedCornerShape(10.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .testTag("syndicate_charter_locked_banner")
      ) {
        Text(
          text = "🔒 Syndicate chartering locked until 15-day cooldown expiry ($remainingLockFormatted REMAINING)",
          color = Color(0xFFF59E0B),
          fontSize = 13.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.SansSerif,
        )
      }
    }

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = "CHARTER NEW OFFICIAL SYNDICATE (STRICT 15d 00h ALLEGIANCE LOCK)",
          color = Color.White,
          fontSize = 16.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.SansSerif,
        )
        Text(
          text =
            "CREATING A SYNDICATE REGISTERS YOUR 3–4 LETTER [TAG] AND LOCKS ALLEGIANCE FOR 15d 00h.",
          color = Color(0xFF8A99AD),
          fontSize = 11.sp,
          fontFamily = FontFamily.SansSerif,
        )
      }

      if (anyFieldFocused && !isCooldownLocked) {
        Button(
          onClick = {
            focusManager.clearFocus()
            keyboardController?.hide()
          },
          colors =
            ButtonDefaults.buttonColors(
              containerColor = Color(0xFF00E676),
              contentColor = Color(0xFF05080E),
            ),
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier.height(38.dp).testTag("create_syndicate_done_button"),
        ) {
          Text(
            text = "✓ DONE",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.SansSerif,
          )
        }
      }
    }

    // Form container drops opacity to 0.4f and disables all inputs when locked so soft keyboard never opens on tap
    Column(
      modifier =
        Modifier.fillMaxWidth()
          .alpha(if (isCooldownLocked) 0.4f else 1.0f),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        OutlinedTextField(
          value = tagInput,
          onValueChange = { raw ->
            if (!isCooldownLocked) {
              tagInput = raw.uppercase().replace(Regex("[^A-Z0-9]"), "").take(4)
            }
          },
          enabled = !isCooldownLocked,
          readOnly = isCooldownLocked,
          singleLine = true,
          keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
          keyboardActions =
            KeyboardActions(
              onDone = {
                focusManager.clearFocus()
                keyboardController?.hide()
              }
            ),
          label = { Text("TAG (3-4 CHARS)", fontSize = 10.sp, fontFamily = FontFamily.SansSerif) },
          textStyle =
            TextStyle(
              color = Color(0xFFFFD700),
              fontSize = 14.sp,
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.Monospace,
            ),
          colors =
            OutlinedTextFieldDefaults.colors(
              focusedBorderColor = Color(0xFFFFD700),
              unfocusedBorderColor = Color(0xFF2B4C73),
              disabledBorderColor = Color(0xFF1E2D42),
              disabledTextColor = Color(0xFF8A99AD),
              disabledLabelColor = Color(0xFF8A99AD),
            ),
          modifier =
            Modifier.width(160.dp)
              .onFocusChanged { anyFieldFocused = it.isFocused }
              .testTag("create_syndicate_tag_input"),
        )

        OutlinedTextField(
          value = nameInput,
          onValueChange = { raw ->
            if (!isCooldownLocked) {
              nameInput = raw.take(24)
            }
          },
          enabled = !isCooldownLocked,
          readOnly = isCooldownLocked,
          singleLine = true,
          keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
          keyboardActions =
            KeyboardActions(
              onDone = {
                focusManager.clearFocus()
                keyboardController?.hide()
              }
            ),
          label = { Text("SYNDICATE NAME", fontSize = 10.sp, fontFamily = FontFamily.SansSerif) },
          textStyle =
            TextStyle(
              color = Color.White,
              fontSize = 14.sp,
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.SansSerif,
            ),
          colors =
            OutlinedTextFieldDefaults.colors(
              focusedBorderColor = Color(0xFF00F0FF),
              unfocusedBorderColor = Color(0xFF2B4C73),
              disabledBorderColor = Color(0xFF1E2D42),
              disabledTextColor = Color(0xFF8A99AD),
              disabledLabelColor = Color(0xFF8A99AD),
            ),
          modifier =
            Modifier.weight(1f)
              .onFocusChanged { anyFieldFocused = it.isFocused }
              .testTag("create_syndicate_name_input"),
        )
      }

      OutlinedTextField(
        value = mottoInput,
        onValueChange = { raw ->
          if (!isCooldownLocked) {
            mottoInput = raw.take(60)
          }
        },
        enabled = !isCooldownLocked,
        readOnly = isCooldownLocked,
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions =
          KeyboardActions(
            onDone = {
              focusManager.clearFocus()
              keyboardController?.hide()
            }
          ),
        label = { Text("SYNDICATE DOCTRINE / MOTTO", fontSize = 10.sp, fontFamily = FontFamily.SansSerif) },
        textStyle =
          TextStyle(
            color = Color(0xFFD6E4F0),
            fontSize = 12.sp,
            fontFamily = FontFamily.SansSerif,
          ),
        colors =
          OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color(0xFF00F0FF),
            unfocusedBorderColor = Color(0xFF2B4C73),
            disabledBorderColor = Color(0xFF1E2D42),
            disabledTextColor = Color(0xFF8A99AD),
            disabledLabelColor = Color(0xFF8A99AD),
          ),
        modifier =
          Modifier.fillMaxWidth()
            .onFocusChanged { anyFieldFocused = it.isFocused }
            .testTag("create_syndicate_motto_input"),
      )

      Button(
        onClick = { onCreateCustomSyndicate(tagInput, nameInput, mottoInput) },
        enabled = !isCooldownLocked && tagInput.length >= 3 && nameInput.length >= 3,
        colors =
          ButtonDefaults.buttonColors(
            containerColor = Color(0xFFFFD700),
            contentColor = Color(0xFF05080E),
            disabledContainerColor = Color(0xFF1A283B),
            disabledContentColor = Color(0xFF8A99AD),
          ),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.height(46.dp).fillMaxWidth().testTag("create_syndicate_submit_button"),
      ) {
        Text(
          text =
            if (isCooldownLocked) {
              "🔒 SYNDICATE CHARTERING LOCKED: COOLDOWN ACTIVE ($remainingLockFormatted REMAINING)"
            } else {
              "🏛️ CHARTER [$tagInput] ${nameInput.uppercase()} & LOCK FOR 15d 00h"
            },
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.SansSerif,
        )
      }
    }
  }
}

@Composable
private fun ArmadaAndFriendsTabPane(
  activeArmadaName: String,
  armadaTierLabel: String,
  armadaSlots: List<ArmadaMemberSlot>,
  friendsList: List<FriendPilotEntry>,
  blockedPilotUids: Set<String> = emptySet(),
  onCreateOrRenameArmada: (String) -> Unit,
  onInviteFriendToArmada: (FriendPilotEntry) -> Unit,
  onRequestJoinFriendArmada: (FriendPilotEntry) -> Unit,
  onRemoveArmadaMember: (String) -> Unit,
  onSimulateRosterShift4To2: () -> Unit,
  onLeaveArmadaToSolo: () -> Unit,
  onAddFriendByCallsign: (String) -> Unit,
  onBlockPilot: (uid: String, callsign: String) -> Unit = { _, _ -> },
  onReportPilot: (uid: String, callsign: String) -> Unit = { _, _ -> },
) {
  val visibleFriends =
    remember(friendsList, blockedPilotUids) {
      friendsList.filter { it.uid !in blockedPilotUids }
    }
  var armadaNameInput by remember(activeArmadaName) {
    mutableStateOf(activeArmadaName.ifEmpty { "APEX_5" })
  }
  var newFriendInput by remember { mutableStateOf("") }
  var inputFocused by remember { mutableStateOf(false) }

  // P3: 10-second anti-spam cooldown state map for Semantic Invite & Request buttons
  val buttonCooldownSec = remember { mutableStateMapOf<String, Int>() }

  LaunchedEffect(buttonCooldownSec.keys.toList()) {
    while (buttonCooldownSec.isNotEmpty()) {
      delay(1000L)
      val keys = buttonCooldownSec.keys.toList()
      for (k in keys) {
        val rem = (buttonCooldownSec[k] ?: 0) - 1
        if (rem <= 0) {
          buttonCooldownSec.remove(k)
        } else {
          buttonCooldownSec[k] = rem
        }
      }
    }
  }

  val focusManager = LocalFocusManager.current
  val keyboardController = LocalSoftwareKeyboardController.current

  Row(
    modifier = Modifier.fillMaxSize(),
    horizontalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    // Left Column: Active Named Armada Roster (2 to 5 Players or [SOLO])
    Column(
      modifier =
        Modifier.width(305.dp)
          .fillMaxHeight()
          .background(Color(0x99111C2D), RoundedCornerShape(12.dp))
          .padding(12.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Text(
        text =
          if (activeArmadaName.isEmpty()) {
            "⚡ [STATUS: OPERATING INDEPENDENTLY]"
          } else {
            "🛡️ [STATUS: DEPLOYED IN ARMADA: $activeArmadaName] (${armadaSlots.size}/5)"
          },
        color = if (activeArmadaName.isEmpty()) Color(0xFF60A5FA) else Color(0xFF10B981),
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.SansSerif,
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
          keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
          keyboardActions =
            KeyboardActions(
              onDone = {
                focusManager.clearFocus()
                keyboardController?.hide()
              }
            ),
          label = { Text("ARMADA TAG", fontSize = 9.sp, fontFamily = FontFamily.SansSerif) },
          textStyle =
            TextStyle(
              color = Color.White,
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.Monospace,
            ),
          colors =
            OutlinedTextFieldDefaults.colors(
              focusedBorderColor = Color(0xFF38BDF8),
              unfocusedBorderColor = Color(0xFF223B59),
            ),
          modifier =
            Modifier.weight(1f)
              .onFocusChanged { inputFocused = it.isFocused }
              .testTag("armada_name_input"),
        )

        Button(
          onClick = {
            focusManager.clearFocus()
            keyboardController?.hide()
            onCreateOrRenameArmada(armadaNameInput)
          },
          colors =
            ButtonDefaults.buttonColors(
              containerColor = Color(0xFF10B981),
              contentColor = Color(0xFF05080E),
            ),
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier.height(44.dp).testTag("form_armada_button"),
        ) {
          Text(
            text = if (activeArmadaName.isEmpty()) "CREATE ARMADA" else "JOIN / RENAME",
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.SansSerif,
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
                  "FLYING [SOLO] IN THE FFA RIFT.\nINVITE FRIENDS ON THE RIGHT TO FORM A 2-TO-5-MAN NAMED ARMADA.",
                color = Color(0xFF8A99AD),
                fontSize = 10.5.sp,
                fontFamily = FontFamily.SansSerif,
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
                    color = if (slot.isLeader) Color(0xFFFFD700) else Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif,
                  )
                  Text(
                    text = if (slot.isLeader) "ARMADA COMMANDER" else "WINGMAN ACTIVE",
                    color = Color(0xFF00F0FF),
                    fontSize = 9.5.sp,
                    fontFamily = FontFamily.SansSerif,
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
                    fontFamily = FontFamily.SansSerif,
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
              contentColor = Color(0xFFFFD700),
            ),
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier.weight(1f).height(38.dp).testTag("simulate_roster_shift_button"),
        ) {
          Text(
            text = "⚡ SHIFT 4➔2 TIER",
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.SansSerif,
          )
        }

        if (activeArmadaName.isNotEmpty()) {
          Button(
            onClick = onLeaveArmadaToSolo,
            colors =
              ButtonDefaults.buttonColors(
                containerColor = Color(0xFF281520),
                contentColor = Color(0xFFEF4444),
              ),
            border = BorderStroke(1.dp, Color(0xFFEF4444)),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.height(38.dp).testTag("leave_armada_solo_button"),
          ) {
            Text(
              text = "LEAVE ARMADA",
              fontSize = 9.5.sp,
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.SansSerif,
            )
          }
        }
      }
    }

    // Right Column: Friends List, [✓ DONE] Keyboard Bar & P3 Semantic Invite Buttons with 10s Anti-Spam Cooldown
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
          text = "ONLINE FRIENDS & ARMADA RECRUITMENT (${visibleFriends.count { it.isOnline }} ONLINE)",
          color = Color.White,
          fontSize = 14.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.SansSerif,
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
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions =
              KeyboardActions(
                onDone = {
                  focusManager.clearFocus()
                  keyboardController?.hide()
                }
              ),
            placeholder = {
              Text("SEARCH / ADD CALLSIGN", fontSize = 9.sp, fontFamily = FontFamily.SansSerif)
            },
            textStyle =
              TextStyle(
                color = Color.White,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
              ),
            colors =
              OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF00F0FF),
                unfocusedBorderColor = Color(0xFF223B59),
              ),
            modifier =
              Modifier.width(155.dp)
                .height(46.dp)
                .onFocusChanged { inputFocused = it.isFocused }
                .testTag("friend_search_input"),
          )

          Button(
            onClick = {
              if (newFriendInput.length >= 3) {
                onAddFriendByCallsign(newFriendInput)
                newFriendInput = ""
              }
              focusManager.clearFocus()
              keyboardController?.hide()
            },
            colors =
              ButtonDefaults.buttonColors(
                containerColor = Color(0xFF00F0FF),
                contentColor = Color(0xFF05080E),
              ),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.height(40.dp),
          ) {
            Text(
              text = "+ ADD",
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.SansSerif,
            )
          }

          if (inputFocused) {
            Button(
              onClick = {
                focusManager.clearFocus()
                keyboardController?.hide()
              },
              colors =
                ButtonDefaults.buttonColors(
                  containerColor = Color(0xFF00E676),
                  contentColor = Color(0xFF05080E),
                ),
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier.height(40.dp).testTag("friend_input_done_button"),
            ) {
              Text(
                text = "✓ DONE",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.SansSerif,
              )
            }
          }
        }
      }

      LazyColumn(
        modifier = Modifier.weight(1f).fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        items(visibleFriends, key = { it.uid }) { friend ->
          val reqKey = "req_${friend.uid}"
          val invKey = "inv_${friend.uid}"
          val reqRem = buttonCooldownSec[reqKey] ?: 0
          val invRem = buttonCooldownSec[invKey] ?: 0

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
                  fontSize = 13.sp,
                  fontWeight = FontWeight.Bold,
                  fontFamily = FontFamily.SansSerif,
                )
                Text(
                  text = friend.statusText.uppercase(),
                  color = if (friend.isOnline) Color(0xFF8A99AD) else Color(0xFF607D8B),
                  fontSize = 10.5.sp,
                  fontFamily = FontFamily.SansSerif,
                )
              }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
              if (friend.isOnline) {
                // Phase 4.3 Inbound Join Request ([⇥ REQUEST INVITE]): Deep Indigo background with crisp Cyan border (#38BDF8)
                if (friend.currentArmadaName.isNotEmpty() && friend.currentArmadaCount < 5) {
                  val reqLocked = reqRem > 0
                  Button(
                    onClick = {
                      buttonCooldownSec[reqKey] = 10
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
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(34.dp).testTag("request_join_friend_${friend.callsign}"),
                  ) {
                    Text(
                      text =
                        if (reqLocked) {
                          "⏳ INVITE SENT - ${reqRem}s"
                        } else {
                          "⇥ REQUEST INVITE"
                        },
                      fontSize = 9.5.sp,
                      fontWeight = FontWeight.Bold,
                      fontFamily = FontFamily.SansSerif,
                    )
                  }
                }

                // Phase 4.3 Outbound Recruit ([+ INVITE TO ARMADA]): Bright Emerald Green fill (#10B981, dark text #05080E)
                val invLocked = invRem > 0
                Button(
                  onClick = {
                    buttonCooldownSec[invKey] = 10
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
                  shape = RoundedCornerShape(8.dp),
                  modifier = Modifier.height(34.dp).testTag("invite_friend_${friend.callsign}"),
                ) {
                  Text(
                    text =
                      if (invLocked) {
                        "⏳ INVITE SENT - ${invRem}s"
                      } else {
                        "+ INVITE TO ARMADA"
                      },
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif,
                  )
                }
              }

              Button(
                onClick = { onReportPilot(friend.uid, friend.callsign) },
                colors =
                  ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2B1C08),
                    contentColor = Color(0xFFF59E0B),
                  ),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 8.dp),
                modifier = Modifier.height(34.dp).testTag("report_armada_friend_${friend.uid}"),
              ) {
                Text("⚑", fontSize = 11.sp, fontWeight = FontWeight.Bold)
              }

              Button(
                onClick = { onBlockPilot(friend.uid, friend.callsign) },
                colors =
                  ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF28111B),
                    contentColor = Color(0xFFEF4444),
                  ),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 8.dp),
                modifier = Modifier.height(34.dp).testTag("block_armada_friend_${friend.uid}"),
              ) {
                Text("🚫", fontSize = 11.sp, fontWeight = FontWeight.Bold)
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun SyndicateTabButton(
  label: String,
  selected: Boolean,
  onClick: () -> Unit,
  tag: String,
) {
  Box(
    modifier =
      Modifier.clip(RoundedCornerShape(8.dp))
        .background(if (selected) Color(0xFFFFD700) else Color(0x99111C2D))
        .border(
          1.dp,
          if (selected) Color(0xFFFFD700) else Color(0x33FFFFFF),
          RoundedCornerShape(8.dp),
        )
        .clickable { onClick() }
        .padding(horizontal = 10.dp, vertical = 7.dp)
        .testTag(tag),
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
