package com.example.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.engine.RiftCodex
import com.example.engine.RiftGameEngine
import com.example.engine.ShipEntity
import kotlinx.coroutines.delay

private val Mono = FontFamily.Monospace
private val Sans = FontFamily.SansSerif

@Composable
private fun SyndicateInputBox(
  value: String,
  onValueChange: (String) -> Unit,
  placeholder: String,
  borderColor: Color,
  textColor: Color,
  height: Dp,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  fontFamily: FontFamily = Mono,
  fontSizeSp: Float = 11.5f,
  testTag: String,
  onFocusChange: (Boolean) -> Unit,
  onDone: () -> Unit,
) {
  Box(
    modifier = modifier.height(height).clip(RoundedCornerShape(8.dp)).background(Color(0xFF050B14)).border(1.dp, borderColor, RoundedCornerShape(8.dp)).padding(horizontal = 10.dp),
    contentAlignment = Alignment.CenterStart,
  ) {
    if (value.isEmpty()) Text(placeholder, fontSize = 10.sp, color = Color(0xFF64748B), fontFamily = fontFamily, maxLines = 1, overflow = TextOverflow.Ellipsis)
    BasicTextField(
      value = value,
      onValueChange = onValueChange,
      enabled = enabled,
      readOnly = !enabled,
      singleLine = true,
      cursorBrush = SolidColor(borderColor),
      keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
      keyboardActions = KeyboardActions(onDone = { onDone() }),
      textStyle = TextStyle(color = textColor, fontSize = fontSizeSp.sp, fontWeight = FontWeight.Bold, fontFamily = fontFamily),
      modifier = Modifier.fillMaxWidth().onFocusChanged { onFocusChange(it.isFocused) }.testTag(testTag),
    )
  }
}

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
  onCloseModal: () -> Unit = {},
) {
  val isCompactPhone = LocalConfiguration.current.screenHeightDp < 480
  val context = LocalContext.current
  val localPrefs = remember(context) { LocalPilotPrefs(context) }
  val isDrifter = remember(currentSyndicateTag) { localPrefs.isGuestBypassEnabled() }
  var activeTab by remember { mutableIntStateOf(0) }
  val isGuestSyndicate = currentSyndicateTag.isBlank() || currentSyndicateTag in setOf("GUEST", "FREE", "RFT")

  Column(modifier = Modifier.fillMaxSize().imePadding().padding(8.dp).testTag("syndicates_modal")) {
    Row(modifier = Modifier.fillMaxWidth().height(if (isCompactPhone) 28.dp else 34.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
      Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        val displayTag = currentSyndicateTag.ifBlank { "SOLO" }
        Text(
          text = if (isCompactPhone) "🏛️ SYNDICATE // [$displayTag] $currentSyndicateName" else "🏛️ SYNDICATE COMMAND // [$displayTag] $currentSyndicateName",
          color = Color.White, fontSize = if (isCompactPhone) 12.5.sp else 15.5.sp, fontWeight = FontWeight.Black, fontFamily = Sans, letterSpacing = 0.8.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
        val isWarnBadge = remainingLockDays > 0 || isGuestSyndicate
        Box(
          modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(if (isWarnBadge) Color(0xFF2B1C08) else Color(0xFF0A192F))
            .border(1.dp, if (isWarnBadge) Color(0xFFF59E0B) else Color(0xFF38BDF8), RoundedCornerShape(6.dp)).padding(horizontal = 6.dp, vertical = 2.dp),
        ) {
          Text(
            text = when { remainingLockDays > 0 -> "🔒 LOCK: $remainingLockFormatted"; isGuestSyndicate -> "🛰️ UNAFFILIATED PILOT"; else -> "🟢 UNLOCKED (15d LOCK)" },
            color = if (isWarnBadge) Color(0xFFF59E0B) else Color(0xFF38BDF8), fontSize = if (isCompactPhone) 8.2.sp else 9.5.sp, fontWeight = FontWeight.Bold, fontFamily = Mono, maxLines = 1,
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(if (isCompactPhone) 4.dp else 7.dp))

    Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
      SyndicateTabButton("1. DIRECTORY & JOIN", activeTab == 0, { activeTab = 0 }, "syndicate_tab_directory")
      SyndicateTabButton(if (isCompactPhone) "2. ROSTER" else "2. MY SYNDICATE ROSTER", activeTab == 1, { activeTab = 1 }, "syndicate_tab_roster")
      SyndicateTabButton(if (isCompactPhone) "3. BOUNTIES & MEDALS" else "3. SYNDICATE BOUNTIES & MEDALS", activeTab == 2, { activeTab = 2 }, "syndicate_tab_bounties")
      SyndicateTabButton("4. CREATE SYNDICATE", activeTab == 3, { activeTab = 3 }, "syndicate_tab_create")
    }

    if (isDrifter) {
      Spacer(modifier = Modifier.height(if (isCompactPhone) 4.dp else 6.dp))
      Box(modifier = Modifier.fillMaxWidth().background(Color(0xFF2B1C08), RoundedCornerShape(8.dp)).border(1.2.dp, Color(0xFFF59E0B), RoundedCornerShape(8.dp)).padding(horizontal = 10.dp, vertical = if (isCompactPhone) 4.dp else 6.dp).testTag("syndicate_drifter_lock_banner")) {
        Text("🔒 DRIFTERS WEAR THE [RFT] TAG. SIGN IN AS A SWORN PILOT TO JOIN OR FOUND A SYNDICATE.", color = Color(0xFFFACC15), fontSize = if (isCompactPhone) 9.sp else 10.5.sp, fontWeight = FontWeight.ExtraBold, fontFamily = Mono, maxLines = 1, overflow = TextOverflow.Ellipsis)
      }
    } else if (isGuestSyndicate && !isCompactPhone) {
      Spacer(modifier = Modifier.height(5.dp))
      Box(modifier = Modifier.fillMaxWidth().background(Color(0xFF1F1608), RoundedCornerShape(8.dp)).border(1.dp, Color(0x88F59E0B), RoundedCornerShape(8.dp)).padding(horizontal = 10.dp, vertical = 5.dp)) {
        Text("ℹ️ [GUEST] SYNDICATE FRAMEWORK: Offers zero perks or shared vault benefits. Join or charter an active player Syndicate below to break free from [GUEST] (subject to 15-day allegiance lock).", color = Color(0xFFFDE68A), fontSize = 10.sp, fontWeight = FontWeight.Medium, fontFamily = Sans)
      }
    }

    if (!statusBannerMessage.isNullOrBlank()) {
      Spacer(modifier = Modifier.height(4.dp))
      Box(modifier = Modifier.fillMaxWidth().background(Color(0xFF102338), RoundedCornerShape(8.dp)).border(1.dp, Color(0x5538BDF8), RoundedCornerShape(8.dp)).padding(horizontal = 10.dp, vertical = 4.dp)) {
        Text(statusBannerMessage, color = Color(0xFF38BDF8), fontSize = if (isCompactPhone) 9.5.sp else 11.sp, fontWeight = FontWeight.Medium, fontFamily = Sans, maxLines = 1, overflow = TextOverflow.Ellipsis)
      }
    }

    Spacer(modifier = Modifier.height(if (isCompactPhone) 4.dp else 7.dp))

    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
      when (activeTab) {
        0 -> DirectoryAndJoinPane(currentSyndicateTag, remainingLockDays, remainingLockFormatted, directoryList, onJoinSyndicate) { activeTab = 3 }
        1 -> MySyndicateRosterPane(currentSyndicateTag, currentSyndicateName, remainingLockDays, remainingLockFormatted, rosterMembers, onLeaveSyndicate)
        2 -> BountyCalloutsAndAchievementsPane(activeShipsInSector, achievements)
        else -> CreateSyndicatePane(remainingLockDays, remainingLockFormatted, onCreateCustomSyndicate)
      }
    }
  }
}

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
  onSimulateRosterShift4To2: () -> Unit = {},
  onLeaveArmadaToSolo: () -> Unit,
  onAddFriendByCallsign: (String) -> Unit,
  onBlockPilot: (uid: String, callsign: String) -> Unit = { _, _ -> },
  onReportPilot: (uid: String, callsign: String) -> Unit = { _, _ -> },
  onCloseModal: () -> Unit = {},
) {
  val isCompactPhone = LocalConfiguration.current.screenHeightDp < 480
  val realArmadaSlots = remember(armadaSlots) {
    armadaSlots.filter { !it.callsign.contains("[BOT]", ignoreCase = true) && !it.syndicateTag.equals("BOT", ignoreCase = true) }
  }
  val isDeployedInArmada = activeArmadaName.isNotEmpty() && realArmadaSlots.isNotEmpty()

  Column(modifier = Modifier.fillMaxSize().imePadding().padding(8.dp).testTag("armada_fleet_modal")) {
    Row(modifier = Modifier.fillMaxWidth().height(if (isCompactPhone) 28.dp else 34.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
      Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
          text = if (isCompactPhone) "👥 ARMADA FLEET DECK" else "👥 ARMADA FLEET DECK // 1-TO-5-PLAYER STRIKE TEAM COMMAND",
          color = Color.White, fontSize = if (isCompactPhone) 12.5.sp else 15.5.sp, fontWeight = FontWeight.Black, fontFamily = Sans, letterSpacing = 0.8.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
        Box(
          modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(if (isDeployedInArmada) Color(0xFF06281E) else Color(0xFF0A192F))
            .border(1.dp, if (isDeployedInArmada) Color(0xFF10B981) else Color(0xFF38BDF8), RoundedCornerShape(6.dp)).padding(horizontal = 6.dp, vertical = 2.dp),
        ) {
          Text(
            text = if (isDeployedInArmada) "🟢 [$activeArmadaName] (${realArmadaSlots.size}/5 • $armadaTierLabel)" else "🛰️ [SOLO] INDEPENDENT",
            color = if (isDeployedInArmada) Color(0xFF10B981) else Color(0xFF60A5FA), fontSize = if (isCompactPhone) 8.5.sp else 9.5.sp, fontWeight = FontWeight.Bold, fontFamily = Mono, maxLines = 1,
          )
        }
      }
    }

    if (!statusBannerMessage.isNullOrBlank()) {
      Spacer(modifier = Modifier.height(4.dp))
      Box(modifier = Modifier.fillMaxWidth().background(Color(0xFF102338), RoundedCornerShape(8.dp)).border(1.dp, Color(0x553B82F6), RoundedCornerShape(8.dp)).padding(horizontal = 10.dp, vertical = 4.dp)) {
        Text(statusBannerMessage, color = Color(0xFF60A5FA), fontSize = if (isCompactPhone) 9.5.sp else 11.sp, fontWeight = FontWeight.Medium, fontFamily = Sans, maxLines = 1, overflow = TextOverflow.Ellipsis)
      }
    }

    Spacer(modifier = Modifier.height(if (isCompactPhone) 4.dp else 8.dp))

    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
      ArmadaAndFriendsTabPane(
        activeArmadaName = activeArmadaName,
        armadaTierLabel = armadaTierLabel,
        armadaSlots = realArmadaSlots,
        friendsList = friendsList,
        blockedPilotUids = blockedPilotUids,
        onCreateOrRenameArmada = onCreateOrRenameArmada,
        onInviteFriendToArmada = onInviteFriendToArmada,
        onRequestJoinFriendArmada = onRequestJoinFriendArmada,
        onRemoveArmadaMember = onRemoveArmadaMember,
        onLeaveArmadaToSolo = onLeaveArmadaToSolo,
        onAddFriendByCallsign = onAddFriendByCallsign,
        onBlockPilot = onBlockPilot,
        onReportPilot = onReportPilot,
      )
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
  onSelectCreateTab: () -> Unit = {},
) {
  val context = LocalContext.current
  val localPrefs = remember(context) { LocalPilotPrefs(context) }
  var rankingMode by remember { mutableIntStateOf(0) }

  val mergedDirectory = remember(directoryList, currentSyndicateTag, rankingMode) {
    val byTag = linkedMapOf<String, SyndicateInfo>()
    for (item in directoryList) {
      val clean = localPrefs.sanitizeSyndicateTag(item.tag)
      val normalized = item.copy(tag = clean)
      if (clean.isNotEmpty() && normalized.isRealPlayerFoundedSyndicate()) byTag[clean] = normalized
    }
    val mapped = byTag.values.map { syn -> syn to localPrefs.getSyndicateWarRecord(syn.tag) }
    if (rankingMode == 0) {
      mapped.sortedWith(compareByDescending<Pair<SyndicateInfo, SyndicateWarRecord>> { it.second.warTrophies }.thenByDescending { it.second.warWins }.thenByDescending { it.first.combinedBounty })
    } else {
      mapped.sortedWith(compareByDescending<Pair<SyndicateInfo, SyndicateWarRecord>> { it.first.combinedBounty }.thenByDescending { it.first.memberCount }.thenByDescending { it.second.warTrophies })
    }
  }

  Column(modifier = Modifier.fillMaxSize().background(Color(0x99111C2D), RoundedCornerShape(12.dp)).padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
      Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
          modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(if (rankingMode == 0) Color(0xFFF59E0B) else Color(0xCC0D1B30))
            .border(1.dp, if (rankingMode == 0) Color(0xFFF59E0B) else Color(0x66F59E0B), RoundedCornerShape(8.dp))
            .clickable { rankingMode = 0 }.padding(horizontal = 10.dp, vertical = 6.dp).testTag("syndicate_rank_mode_war_trophies"),
        ) {
          Text("🏆 SYNDICATE WARS LADDER", color = if (rankingMode == 0) Color(0xFF05080E) else Color(0xFFFBBF24), fontSize = 10.5.sp, fontWeight = FontWeight.ExtraBold, fontFamily = Sans)
        }
        Box(
          modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(if (rankingMode == 1) Color(0xFF00F0FF) else Color(0xCC0D1B30))
            .border(1.dp, if (rankingMode == 1) Color(0xFF00F0FF) else Color(0x6600F0FF), RoundedCornerShape(8.dp))
            .clickable { rankingMode = 1 }.padding(horizontal = 10.dp, vertical = 6.dp).testTag("syndicate_rank_mode_collective_prestige"),
        ) {
          Text("📊 COLLECTIVE MEMBER PRESTIGE", color = if (rankingMode == 1) Color(0xFF05080E) else Color(0xFF00F0FF), fontSize = 10.5.sp, fontWeight = FontWeight.ExtraBold, fontFamily = Sans)
        }
      }
      Text(if (rankingMode == 0) "RANKED BY SYNDICATE WAR TROPHIES" else "RANKED BY COMBINED MEMBER BOUNTY & ROSTER SIZE", color = Color(0xFF94A3B8), fontSize = 9.5.sp, fontWeight = FontWeight.Bold, fontFamily = Mono)
    }

    if (mergedDirectory.isEmpty()) {
      Box(modifier = Modifier.weight(1f).fillMaxWidth().background(Color(0xCC0D1B30), RoundedCornerShape(10.dp)).border(1.dp, Color(0x4438BDF8), RoundedCornerShape(10.dp)).padding(18.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("NO SWORN SYNDICATES CHARTERED IN THIS SECTOR YET", color = Color(0xFF00F5D4), fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, fontFamily = Sans)
          Text("Every Syndicate in RIFT is 100% real-player-founded. Open Tab 4 to charter the first Syndicate!", color = Color(0xFF94A3B8), fontSize = 11.sp, fontFamily = Sans)
          Button(onClick = onSelectCreateTab, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700), contentColor = Color(0xFF05080E)), shape = RoundedCornerShape(8.dp), modifier = Modifier.height(34.dp)) {
            Text("🏛️ CHARTER FIRST SYNDICATE", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, fontFamily = Sans)
          }
        }
      }
    } else {
      LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(mergedDirectory, key = { it.first.tag }) { (syn, warRecord) ->
          val isMember = syn.tag.equals(currentSyndicateTag, ignoreCase = true)
          Row(
            modifier = Modifier.fillMaxWidth().background(if (isMember) Color(0xCC142C4C) else Color(0x990D1B30), RoundedCornerShape(10.dp))
              .border(1.dp, if (isMember) Color(0xFFFFD700) else Color(0x3338BDF8), RoundedCornerShape(10.dp)).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("[${syn.tag}] ${syn.name}", color = if (isMember) Color(0xFFFFD700) else Color(0xFF00F0FF), fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = Sans)
                Text("🏆 ${warRecord.warTrophies} WAR TROPHIES • ${warRecord.warWins}W - ${warRecord.warLosses}L • 🔥 ${warRecord.warStreak} STREAK", color = Color(0xFFFBBF24), fontSize = 11.5.sp, fontWeight = FontWeight.ExtraBold, fontFamily = Mono)
              }
              Spacer(modifier = Modifier.height(3.dp))
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("${syn.memberCount} PILOTS • ${syn.combinedBounty} TOTAL BOUNTY", color = Color(0xFF38BDF8), fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = Mono)
                Text("// ${syn.motto.uppercase()}", color = Color(0xFF8A99AD), fontSize = 10.5.sp, fontFamily = Sans)
              }
            }
            if (isMember) {
              Box(modifier = Modifier.background(Color(0xFF1E3E24), RoundedCornerShape(8.dp)).padding(horizontal = 12.dp, vertical = 8.dp)) {
                Text("✓ ENLISTED", color = Color(0xFF00E676), fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = Sans)
              }
            } else {
              val isDrifterMode = localPrefs.isGuestBypassEnabled()
              Button(
                onClick = { onJoinSyndicate(syn) },
                enabled = remainingLockDays <= 0 && !isDrifterMode,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00F0FF), contentColor = Color(0xFF05080E), disabledContainerColor = Color(0xFF1A283B), disabledContentColor = Color(0xFF8A99AD)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.height(38.dp).testTag("join_syndicate_${syn.tag}"),
              ) {
                Text(when { isDrifterMode -> "SWORN PILOT ONLY"; remainingLockDays > 0 -> "LOCKED ($remainingLockFormatted)"; else -> "JOIN [${syn.tag}]" }, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = Sans)
              }
            }
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
  val humanRoster = remember(rosterMembers) {
    rosterMembers.filter { !it.callsign.contains("[BOT]", ignoreCase = true) && !it.tag.equals("BOT", ignoreCase = true) }
  }
  Column(modifier = Modifier.fillMaxSize().background(Color(0x99111C2D), RoundedCornerShape(12.dp)).padding(12.dp)) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
      val displayTag = currentSyndicateTag.ifBlank { "SOLO" }
      Text("ACTIVE ROSTER // [$displayTag] $currentSyndicateName (${humanRoster.size} MEMBERS)", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = Sans)
      if (currentSyndicateTag != "FREE" && currentSyndicateTag != "GUEST") {
        Button(
          onClick = onLeaveSyndicate,
          enabled = remainingLockDays <= 0,
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2B121D), contentColor = Color(0xFFFF3B4E), disabledContainerColor = Color(0xFF1A283B), disabledContentColor = Color(0xFF8A99AD)),
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier.height(34.dp).testTag("leave_syndicate_button"),
        ) {
          Text(if (remainingLockDays > 0) "LEAVE LOCKED ($remainingLockFormatted)" else "LEAVE SYNDICATE ➔ [GUEST] (15d 00h LOCK)", fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = Sans)
        }
      }
    }
    Spacer(modifier = Modifier.height(8.dp))
    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
      items(humanRoster, key = { it.uid }) { member ->
        val shipSpec = RiftCodex.getShipById(member.shipId)
        Row(modifier = Modifier.fillMaxWidth().background(Color(0x990D1B30), RoundedCornerShape(8.dp)).padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ShipSpritePreview(shipId = member.shipId, sizeDp = 30)
            Column {
              Text("[$currentSyndicateTag] ${member.callsign} • ${member.role}", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = Sans)
              Text("HULL: ${shipSpec.name.uppercase()} (${shipSpec.role.uppercase()})", color = Color(0xFF8A99AD), fontSize = 11.sp, fontFamily = Sans)
            }
          }
          Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("KILLS: ${member.kills}", color = Color(0xFF00F0FF), fontSize = 14.sp, fontWeight = FontWeight.Medium, fontFamily = Mono)
            Text("BOUNTY: ${member.bountyContribution}", color = Color(0xFFFFD700), fontSize = 14.sp, fontWeight = FontWeight.Medium, fontFamily = Mono)
          }
        }
      }
    }
  }
}

@Composable
private fun BountyCalloutsAndAchievementsPane(activeShipsInSector: List<ShipEntity>, achievements: List<PilotAchievement>) {
  val hvts = remember(activeShipsInSector) { activeShipsInSector.filter { it.bounty >= RiftGameEngine.HVT_BOUNTY_THRESHOLD }.sortedByDescending { it.bounty } }
  Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
    Column(modifier = Modifier.weight(1f).fillMaxSize().background(Color(0x99111C2D), RoundedCornerShape(12.dp)).padding(10.dp)) {
      Text("★ SECTOR HIGH-VALUE TARGETS (BOUNTY >= 250)", color = Color(0xFFFFD700), fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = Sans)
      Text("ELIMINATE HVTS FOR MASSIVE BOUNTY PAYOUTS & GOLD SONAR PINGS.", color = Color(0xFF8A99AD), fontSize = 11.sp, fontFamily = Sans)
      Spacer(modifier = Modifier.height(8.dp))
      LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        items(hvts, key = { it.id }) { hvt ->
          Row(modifier = Modifier.fillMaxWidth().background(Color(0x991A1812), RoundedCornerShape(8.dp)).padding(8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              ShipSpritePreview(shipId = hvt.shipId, sizeDp = 28)
              Column {
                val armTag = if (hvt.armadaName.isNotEmpty()) "[${hvt.armadaName}]" else "[SOLO]"
                Text("★ $armTag [${hvt.syndicateTag}] ${hvt.callsign}", color = Color(0xFFFFD700), fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = Sans)
                Text("HULL: ${hvt.spec.name.uppercase()} • SECTOR (${(hvt.x / 500f).toInt()},${(hvt.y / 500f).toInt()})", color = Color(0xFF8A99AD), fontSize = 11.sp, fontFamily = Sans)
              }
            }
            Text("${hvt.bounty} BTY", color = Color(0xFFFF3B4E), fontSize = 14.sp, fontWeight = FontWeight.Medium, fontFamily = Mono)
          }
        }
      }
    }

    Column(modifier = Modifier.weight(1f).fillMaxSize().background(Color(0x99111C2D), RoundedCornerShape(12.dp)).padding(10.dp)) {
      Text("🏆 PILOT COMBAT ACHIEVEMENTS", color = Color(0xFF00F0FF), fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = Sans)
      Spacer(modifier = Modifier.height(8.dp))
      LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        items(achievements, key = { it.id }) { ach ->
          Row(
            modifier = Modifier.fillMaxWidth().background(if (ach.unlocked) Color(0x990E2938) else Color(0x990D1B30), RoundedCornerShape(8.dp)).padding(8.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(if (ach.unlocked) "✓ ${ach.title}" else "🔒 ${ach.title}", color = if (ach.unlocked) Color(0xFF00E676) else Color(0xFF00F0FF), fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = Sans)
              Text(ach.description.uppercase(), color = Color(0xFF8A99AD), fontSize = 10.5.sp, fontFamily = Sans)
            }
            Text(ach.progressText, color = Color(0xFFFFD700), fontSize = 13.sp, fontWeight = FontWeight.Medium, fontFamily = Mono)
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
  val isCompactPhone = LocalConfiguration.current.screenHeightDp < 480
  val context = LocalContext.current
  val localPrefs = remember(context) { LocalPilotPrefs(context) }
  val isDrifter = remember { localPrefs.isGuestBypassEnabled() }
  val isCooldownLocked = remainingLockDays > 0 || isDrifter

  var tagInput by remember { mutableStateOf("NV!") }
  var nameInput by remember { mutableStateOf("NOVA VANGUARD") }
  var mottoInput by remember { mutableStateOf("Strike from the event horizon.") }
  var anyFieldFocused by remember { mutableStateOf(false) }

  val focusManager = LocalFocusManager.current
  val keyboardController = LocalSoftwareKeyboardController.current
  val inputBoxHeight = if (isCompactPhone) 34.dp else 38.dp
  val dismissIme = { focusManager.clearFocus(); keyboardController?.hide() }

  Column(
    modifier = Modifier.fillMaxSize().background(Color(0x99111C2D), RoundedCornerShape(10.dp)).padding(if (isCompactPhone) 8.dp else 12.dp).verticalScroll(rememberScrollState()),
    verticalArrangement = Arrangement.spacedBy(if (isCompactPhone) 6.dp else 8.dp),
  ) {
    if (isCooldownLocked) {
      Box(modifier = Modifier.fillMaxWidth().background(Color(0xFF2B1C08), RoundedCornerShape(8.dp)).border(1.2.dp, Color(0xFFF59E0B), RoundedCornerShape(8.dp)).padding(horizontal = 10.dp, vertical = if (isCompactPhone) 5.dp else 8.dp).testTag("syndicate_charter_locked_banner")) {
        Text(
          text = if (isDrifter) "🔒 DRIFTERS WEAR THE [RFT] TAG. SIGN IN AS A SWORN PILOT TO JOIN OR FOUND A SYNDICATE." else "🔒 Syndicate chartering locked until 15-day cooldown expiry ($remainingLockFormatted REMAINING)",
          color = Color(0xFFF59E0B), fontSize = if (isCompactPhone) 10.sp else 12.sp, fontWeight = FontWeight.Bold, fontFamily = Sans,
        )
      }
    }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
      Column(modifier = Modifier.weight(1f)) {
        Text("CHARTER NEW OFFICIAL SYNDICATE (STRICT 15d 00h ALLEGIANCE LOCK)", color = Color.White, fontSize = if (isCompactPhone) 12.sp else 15.sp, fontWeight = FontWeight.Bold, fontFamily = Sans)
        Text("CREATING A SYNDICATE REGISTERS YOUR 1–3 CHAR [TAG] (A-Z, 0-9, SYMBOLS) AND LOCKS ALLEGIANCE FOR 15d 00h.", color = Color(0xFF8A99AD), fontSize = if (isCompactPhone) 9.sp else 10.5.sp, fontFamily = Sans)
      }
      if (anyFieldFocused && !isCooldownLocked) {
        Button(
          onClick = { dismissIme() },
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676), contentColor = Color(0xFF05080E)),
          border = BorderStroke(1.2.dp, Color(0xFF00E676)), shape = RoundedCornerShape(8.dp), contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
          modifier = Modifier.height(if (isCompactPhone) 30.dp else 34.dp).shadow(2.dp, RoundedCornerShape(8.dp)).testTag("create_syndicate_done_button"),
        ) {
          Text("✓ DONE", fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = Sans)
        }
      }
    }

    Column(modifier = Modifier.fillMaxWidth().alpha(if (isCooldownLocked) 0.4f else 1.0f), verticalArrangement = Arrangement.spacedBy(if (isCompactPhone) 6.dp else 8.dp)) {
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(modifier = Modifier.width(if (isCompactPhone) 185.dp else 210.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
          Text("TAG (1–3 CHARS: A-Z, 0-9, SYMBOLS)", color = Color(0xFFFACC15), fontSize = 8.5.sp, fontWeight = FontWeight.Bold, fontFamily = Mono)
          SyndicateInputBox(
            value = tagInput,
            onValueChange = { if (!isCooldownLocked) tagInput = localPrefs.sanitizeSyndicateTag(it) },
            placeholder = "e.g. VX!, Ω7, ★",
            borderColor = Color(0xFFFFD700),
            textColor = Color(0xFFFFD700),
            height = inputBoxHeight,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isCooldownLocked,
            fontFamily = Mono,
            fontSizeSp = 12f,
            testTag = "create_syndicate_tag_input",
            onFocusChange = { anyFieldFocused = it },
            onDone = { dismissIme() },
          )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
          Text("SYNDICATE NAME", color = Color(0xFF00F5D4), fontSize = 8.5.sp, fontWeight = FontWeight.Bold, fontFamily = Mono)
          SyndicateInputBox(
            value = nameInput,
            onValueChange = { if (!isCooldownLocked) nameInput = it.take(24) },
            placeholder = "Enter Syndicate Name…",
            borderColor = Color(0xFF00F5D4),
            textColor = Color.White,
            height = inputBoxHeight,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isCooldownLocked,
            fontFamily = Sans,
            fontSizeSp = 12f,
            testTag = "create_syndicate_name_input",
            onFocusChange = { anyFieldFocused = it },
            onDone = { dismissIme() },
          )
        }
      }

      Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text("SYNDICATE DOCTRINE / MOTTO", color = Color(0xFF38BDF8), fontSize = 8.5.sp, fontWeight = FontWeight.Bold, fontFamily = Mono)
        SyndicateInputBox(
          value = mottoInput,
          onValueChange = { if (!isCooldownLocked) mottoInput = it.take(60) },
          placeholder = "Enter Syndicate Doctrine or Motto…",
          borderColor = Color(0xFF38BDF8),
          textColor = Color(0xFFD6E4F0),
          height = inputBoxHeight,
          modifier = Modifier.fillMaxWidth(),
          enabled = !isCooldownLocked,
          fontFamily = Sans,
          fontSizeSp = 11.5f,
          testTag = "create_syndicate_motto_input",
          onFocusChange = { anyFieldFocused = it },
          onDone = { dismissIme() },
        )
      }

      val canSubmit = !isCooldownLocked && tagInput.isNotEmpty() && nameInput.length >= 3
      Button(
        onClick = { onCreateCustomSyndicate(tagInput, nameInput, mottoInput) },
        enabled = canSubmit,
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700), contentColor = Color(0xFF05080E), disabledContainerColor = Color(0xFF1A283B), disabledContentColor = Color(0xFF8A99AD)),
        border = BorderStroke(1.2.dp, if (canSubmit) Color(0xFFFFD700) else Color(0xFF334155)),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.height(if (isCompactPhone) 38.dp else 44.dp).fillMaxWidth().shadow(2.dp, RoundedCornerShape(8.dp)).testTag("create_syndicate_submit_button"),
      ) {
        Text(
          text = when { isDrifter -> "🔒 SIGN IN AS A SWORN PILOT TO CHARTER A SYNDICATE"; isCooldownLocked -> "🔒 SYNDICATE CHARTERING LOCKED: COOLDOWN ACTIVE ($remainingLockFormatted REMAINING)"; else -> "🏛️ CHARTER [$tagInput] ${nameInput.uppercase()} & LOCK FOR 15d 00h" },
          fontSize = if (isCompactPhone) 10.5.sp else 12.sp, fontWeight = FontWeight.Bold, fontFamily = Sans,
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
  onLeaveArmadaToSolo: () -> Unit,
  onAddFriendByCallsign: (String) -> Unit,
  onBlockPilot: (uid: String, callsign: String) -> Unit = { _, _ -> },
  onReportPilot: (uid: String, callsign: String) -> Unit = { _, _ -> },
) {
  val isCompactPhone = LocalConfiguration.current.screenHeightDp < 480
  val visibleFriends = remember(friendsList, blockedPilotUids) { friendsList.filter { it.uid !in blockedPilotUids } }
  var armadaNameInput by remember(activeArmadaName) { mutableStateOf(activeArmadaName.ifEmpty { "APEX_5" }) }
  var newFriendInput by remember { mutableStateOf("") }
  var inputFocused by remember { mutableStateOf(false) }

  val buttonCooldownSec = remember { mutableStateMapOf<String, Int>() }
  LaunchedEffect(buttonCooldownSec.keys.toList()) {
    while (buttonCooldownSec.isNotEmpty()) {
      delay(1000L)
      for (k in buttonCooldownSec.keys.toList()) {
        val rem = (buttonCooldownSec[k] ?: 0) - 1
        if (rem <= 0) buttonCooldownSec.remove(k) else buttonCooldownSec[k] = rem
      }
    }
  }

  val focusManager = LocalFocusManager.current
  val keyboardController = LocalSoftwareKeyboardController.current
  val inputBoxHeight = if (isCompactPhone) 34.dp else 38.dp
  val dismissIme = { focusManager.clearFocus(); keyboardController?.hide() }

  Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(if (isCompactPhone) 8.dp else 12.dp)) {
    Column(
      modifier = Modifier.width(if (isCompactPhone) 268.dp else 305.dp).fillMaxHeight().background(Color(0x99111C2D), RoundedCornerShape(10.dp)).padding(if (isCompactPhone) 8.dp else 12.dp).verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(if (isCompactPhone) 6.dp else 8.dp),
    ) {
      Text(
        text = if (activeArmadaName.isEmpty()) "🛰️ [STATUS: OPERATING INDEPENDENTLY]" else "🛡️ [STATUS: DEPLOYED IN ARMADA: $activeArmadaName] (${armadaSlots.size}/5 • $armadaTierLabel)",
        color = if (activeArmadaName.isEmpty()) Color(0xFF60A5FA) else Color(0xFF10B981), fontSize = if (isCompactPhone) 10.sp else 11.5.sp, fontWeight = FontWeight.Bold, fontFamily = Sans,
      )
      Text("ARMADA TAG", color = Color(0xFF38BDF8), fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = Mono)

      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        SyndicateInputBox(
          value = armadaNameInput,
          onValueChange = { armadaNameInput = it.uppercase().replace(Regex("[^A-Z0-9_-]"), "").take(10) },
          placeholder = "ARMADA TAG",
          borderColor = Color(0xFF38BDF8),
          textColor = Color.White,
          height = inputBoxHeight,
          modifier = Modifier.weight(1f),
          fontFamily = Mono,
          fontSizeSp = 11f,
          testTag = "armada_name_input",
          onFocusChange = { inputFocused = it },
          onDone = { dismissIme() },
        )
        Button(
          onClick = { dismissIme(); onCreateOrRenameArmada(armadaNameInput) },
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981), contentColor = Color(0xFF05080E)),
          border = BorderStroke(1.2.dp, Color(0xFF34D399)), shape = RoundedCornerShape(8.dp), contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
          modifier = Modifier.height(inputBoxHeight).shadow(2.dp, RoundedCornerShape(8.dp)).testTag("form_armada_button"),
        ) {
          Text(if (activeArmadaName.isEmpty()) "CREATE ARMADA" else "JOIN / RENAME", fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = Sans)
        }
      }

      Column(modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (armadaSlots.isEmpty()) {
          Box(modifier = Modifier.fillMaxWidth().background(Color(0x660B1524), RoundedCornerShape(8.dp)).padding(8.dp)) {
            Text("FLYING [SOLO] IN THE FFA RIFT.\nINVITE FRIENDS ON THE RIGHT TO FORM A 2-TO-5-MAN NAMED ARMADA.", color = Color(0xFF8A99AD), fontSize = if (isCompactPhone) 9.5.sp else 10.5.sp, fontFamily = Sans)
          }
        } else {
          armadaSlots.forEach { slot ->
            Row(modifier = Modifier.fillMaxWidth().background(Color(0x990D1B30), RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ShipSpritePreview(shipId = slot.shipId, sizeDp = 24)
                Column {
                  Text("[${slot.syndicateTag}] ${slot.callsign}", color = if (slot.isLeader) Color(0xFFFFD700) else Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = Sans)
                  Text(if (slot.isLeader) "ARMADA COMMANDER" else "WINGMAN ACTIVE", color = Color(0xFF00F0FF), fontSize = 8.8.sp, fontFamily = Sans)
                }
              }
              if (!slot.isLeader) {
                Button(
                  onClick = { onRemoveArmadaMember(slot.callsign) },
                  colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2D1420), contentColor = Color(0xFFFF3B4E)),
                  border = BorderStroke(1.2.dp, Color(0xFFFF3B4E)), shape = RoundedCornerShape(8.dp), contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                  modifier = Modifier.height(28.dp).shadow(2.dp, RoundedCornerShape(8.dp)),
                ) {
                  Text("DROP", fontSize = 8.5.sp, fontWeight = FontWeight.Bold, fontFamily = Sans)
                }
              }
            }
          }
        }
      }

      if (activeArmadaName.isNotEmpty()) {
        Button(
          onClick = onLeaveArmadaToSolo,
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF281520), contentColor = Color(0xFFEF4444)),
          border = BorderStroke(1.2.dp, Color(0xFFEF4444)), shape = RoundedCornerShape(8.dp), contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
          modifier = Modifier.fillMaxWidth().height(if (isCompactPhone) 32.dp else 36.dp).shadow(2.dp, RoundedCornerShape(8.dp)).testTag("leave_armada_solo_button"),
        ) {
          Text("LEAVE ARMADA", fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = Sans)
        }
      }
    }

    Column(
      modifier = Modifier.weight(1f).fillMaxHeight().background(Color(0x99111C2D), RoundedCornerShape(10.dp)).padding(if (isCompactPhone) 8.dp else 12.dp),
      verticalArrangement = Arrangement.spacedBy(if (isCompactPhone) 6.dp else 8.dp),
    ) {
      Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
          text = if (isCompactPhone) "FRIENDS & RECRUITMENT (${visibleFriends.count { it.isOnline }} ONLINE)" else "ONLINE FRIENDS & ARMADA RECRUITMENT (${visibleFriends.count { it.isOnline }} ONLINE)",
          color = Color.White, fontSize = if (isCompactPhone) 11.5.sp else 13.5.sp, fontWeight = FontWeight.Bold, fontFamily = Sans, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          SyndicateInputBox(
            value = newFriendInput,
            onValueChange = { newFriendInput = it.uppercase().replace(Regex("[^A-Z0-9_-]"), "").take(14) },
            placeholder = "SEARCH / ADD CALLSIGN",
            borderColor = Color(0xFF00F5D4),
            textColor = Color.White,
            height = inputBoxHeight,
            modifier = Modifier.width(if (isCompactPhone) 145.dp else 165.dp),
            fontFamily = Mono,
            fontSizeSp = 10.5f,
            testTag = "friend_search_input",
            onFocusChange = { inputFocused = it },
            onDone = { dismissIme() },
          )
          Button(
            onClick = {
              if (newFriendInput.length >= 3) { onAddFriendByCallsign(newFriendInput); newFriendInput = "" }
              dismissIme()
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00F0FF), contentColor = Color(0xFF05080E)),
            border = BorderStroke(1.2.dp, Color(0xFF00F5D4)), shape = RoundedCornerShape(8.dp), contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
            modifier = Modifier.height(inputBoxHeight).shadow(2.dp, RoundedCornerShape(8.dp)),
          ) {
            Text("+ ADD", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, fontFamily = Sans)
          }
          if (inputFocused) {
            Button(
              onClick = { dismissIme() },
              colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676), contentColor = Color(0xFF05080E)),
              border = BorderStroke(1.2.dp, Color(0xFF00E676)), shape = RoundedCornerShape(8.dp), contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
              modifier = Modifier.height(inputBoxHeight).shadow(2.dp, RoundedCornerShape(8.dp)).testTag("friend_input_done_button"),
            ) {
              Text("✓ DONE", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, fontFamily = Sans)
            }
          }
        }
      }

      LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(5.dp)) {
        items(visibleFriends, key = { it.uid }) { friend ->
          val reqKey = "req_${friend.uid}"; val invKey = "inv_${friend.uid}"
          val reqRem = buttonCooldownSec[reqKey] ?: 0; val invRem = buttonCooldownSec[invKey] ?: 0
          Row(
            modifier = Modifier.fillMaxWidth().background(Color(0x990D1B30), RoundedCornerShape(9.dp)).padding(horizontal = 10.dp, vertical = if (isCompactPhone) 6.dp else 8.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
              Box(modifier = Modifier.size(9.dp).clip(CircleShape).background(if (friend.isOnline) Color(0xFF00E676) else Color(0xFF546E7A)))
              ShipSpritePreview(shipId = friend.shipId, sizeDp = 26)
              Column {
                val badge = if (friend.currentArmadaName.isNotEmpty()) "[${friend.currentArmadaName}]" else "[SOLO]"
                Text("$badge [${friend.syndicateTag}] ${friend.callsign}", color = Color.White, fontSize = if (isCompactPhone) 11.5.sp else 13.sp, fontWeight = FontWeight.Bold, fontFamily = Sans)
                Text(friend.statusText.uppercase(), color = if (friend.isOnline) Color(0xFF8A99AD) else Color(0xFF607D8B), fontSize = if (isCompactPhone) 9.5.sp else 10.5.sp, fontFamily = Sans)
              }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
              if (friend.isOnline) {
                if (friend.currentArmadaName.isNotEmpty() && friend.currentArmadaCount < 5) {
                  val reqLocked = reqRem > 0
                  Button(
                    onClick = { buttonCooldownSec[reqKey] = 10; onRequestJoinFriendArmada(friend) },
                    enabled = !reqLocked,
                    colors = ButtonDefaults.buttonColors(containerColor = if (reqLocked) Color(0xFF332409) else Color(0xFF121A3A), contentColor = if (reqLocked) Color(0xFFF59E0B) else Color(0xFF38BDF8), disabledContainerColor = Color(0xFF332409), disabledContentColor = Color(0xFFF59E0B)),
                    border = BorderStroke(1.2.dp, if (reqLocked) Color(0xFFF59E0B) else Color(0xFF38BDF8)),
                    shape = RoundedCornerShape(8.dp), contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(if (isCompactPhone) 30.dp else 34.dp).shadow(2.dp, RoundedCornerShape(8.dp)).testTag("request_join_friend_${friend.callsign}"),
                  ) {
                    Text(if (reqLocked) "⏳ INVITE SENT - ${reqRem}s" else "⇥ REQUEST INVITE", fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = Sans)
                  }
                }
                val invLocked = invRem > 0
                Button(
                  onClick = { buttonCooldownSec[invKey] = 10; onInviteFriendToArmada(friend) },
                  enabled = !invLocked,
                  colors = ButtonDefaults.buttonColors(containerColor = if (invLocked) Color(0xFF332409) else Color(0xFF10B981), contentColor = if (invLocked) Color(0xFFF59E0B) else Color(0xFF05080E), disabledContainerColor = Color(0xFF332409), disabledContentColor = Color(0xFFF59E0B)),
                  border = BorderStroke(1.2.dp, if (invLocked) Color(0xFFF59E0B) else Color(0xFF34D399)),
                  shape = RoundedCornerShape(8.dp), contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                  modifier = Modifier.height(if (isCompactPhone) 30.dp else 34.dp).shadow(2.dp, RoundedCornerShape(8.dp)).testTag("invite_friend_${friend.callsign}"),
                ) {
                  Text(if (invLocked) "⏳ INVITE SENT - ${invRem}s" else "+ INVITE TO ARMADA", fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = Sans)
                }
              }
              Button(
                onClick = { onReportPilot(friend.uid, friend.callsign) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2B1C08), contentColor = Color(0xFFF59E0B)),
                border = BorderStroke(1.2.dp, Color(0xFFF59E0B)), shape = RoundedCornerShape(8.dp), contentPadding = PaddingValues(horizontal = 8.dp),
                modifier = Modifier.height(if (isCompactPhone) 30.dp else 34.dp).shadow(2.dp, RoundedCornerShape(8.dp)).testTag("report_armada_friend_${friend.uid}"),
              ) { Text("⚑", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
              Button(
                onClick = { onBlockPilot(friend.uid, friend.callsign) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF28111B), contentColor = Color(0xFFEF4444)),
                border = BorderStroke(1.2.dp, Color(0xFFEF4444)), shape = RoundedCornerShape(8.dp), contentPadding = PaddingValues(horizontal = 8.dp),
                modifier = Modifier.height(if (isCompactPhone) 30.dp else 34.dp).shadow(2.dp, RoundedCornerShape(8.dp)).testTag("block_armada_friend_${friend.uid}"),
              ) { Text("🚫", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun SyndicateTabButton(label: String, selected: Boolean, onClick: () -> Unit, tag: String) {
  val isCompactPhone = LocalConfiguration.current.screenHeightDp < 480
  Box(
    modifier = Modifier.shadow(2.dp, RoundedCornerShape(8.dp)).clip(RoundedCornerShape(8.dp))
      .background(if (selected) Color(0xFFFFD700) else Color(0x99111C2D))
      .border(1.2.dp, if (selected) Color(0xFFFFD700) else Color(0x4438BDF8), RoundedCornerShape(8.dp))
      .clickable { onClick() }.padding(horizontal = if (isCompactPhone) 8.dp else 10.dp, vertical = if (isCompactPhone) 4.dp else 7.dp).testTag(tag),
  ) {
    Text(label, color = if (selected) Color(0xFF05080E) else Color.White, fontSize = if (isCompactPhone) 9.sp else 10.sp, fontWeight = FontWeight.Bold, fontFamily = Sans, maxLines = 1)
  }
}
