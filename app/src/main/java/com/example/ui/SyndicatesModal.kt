package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ArmadaMemberSlot
import com.example.data.FriendPilotEntry
import com.example.data.LocalPilotPrefs
import com.example.data.PilotAchievement
import com.example.data.SyndicateInfo
import com.example.data.SyndicateMemberEntry
import com.example.data.SyndicateWarRecord
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
  val isCompactPhone = LocalConfiguration.current.screenHeightDp < 480
  val context = LocalContext.current
  val localPrefs = remember(context) { LocalPilotPrefs(context) }
  val isDrifter = remember(currentSyndicateTag) { localPrefs.isGuestBypassEnabled() }
  var activeTab by remember { mutableIntStateOf(0) }
  val isGuestSyndicate =
    currentSyndicateTag == "GUEST" || currentSyndicateTag == "FREE" || currentSyndicateTag == "RFT"

  val outerPadding =
    if (isCompactPhone) {
      PaddingValues(start = 8.dp, end = 8.dp, top = 40.dp, bottom = 4.dp)
    } else {
      PaddingValues(horizontal = 20.dp, vertical = 10.dp)
    }
  val innerPadding = if (isCompactPhone) 8.dp else 14.dp

  Box(
    modifier =
      Modifier.fillMaxSize()
        .background(Color(0xB3000000))
        .imePadding()
        .padding(outerPadding)
        .testTag("syndicates_modal"),
    contentAlignment = Alignment.Center,
  ) {
    Surface(
      modifier =
        Modifier.fillMaxSize()
          .border(1.2.dp, Color(0xFF00F5D4), RoundedCornerShape(12.dp)),
      color = Color(0xF2060D1A),
      shape = RoundedCornerShape(12.dp),
    ) {
      Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
        // 1. Unified 30.dp Compact Header Row
        Row(
          modifier = Modifier.fillMaxWidth().height(if (isCompactPhone) 30.dp else 36.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Text(
              text =
                if (isCompactPhone) "🏛️ SYNDICATE // [$currentSyndicateTag] $currentSyndicateName"
                else "🏛️ SYNDICATE COMMAND // [$currentSyndicateTag] $currentSyndicateName",
              color = Color.White,
              fontSize = if (isCompactPhone) 12.5.sp else 16.sp,
              fontWeight = FontWeight.Black,
              fontFamily = FontFamily.SansSerif,
              letterSpacing = 0.8.sp,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Box(
              modifier =
                Modifier.clip(RoundedCornerShape(6.dp))
                  .background(
                    if (remainingLockDays > 0 || isGuestSyndicate) Color(0xFF2B1C08)
                    else Color(0xFF0A192F)
                  )
                  .border(
                    1.dp,
                    if (remainingLockDays > 0 || isGuestSyndicate) Color(0xFFF59E0B)
                    else Color(0xFF38BDF8),
                    RoundedCornerShape(6.dp),
                  )
                  .padding(horizontal = 6.dp, vertical = 2.dp),
            ) {
              Text(
                text =
                  if (remainingLockDays > 0) {
                    "🔒 LOCK: $remainingLockFormatted"
                  } else if (isGuestSyndicate) {
                    "⚠️ [GUEST] ZERO PERKS"
                  } else {
                    "🟢 UNLOCKED (15d LOCK)"
                  },
                color =
                  if (remainingLockDays > 0 || isGuestSyndicate) {
                    Color(0xFFF59E0B)
                  } else {
                    Color(0xFF38BDF8)
                  },
                fontSize = if (isCompactPhone) 8.2.sp else 9.5.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
              )
            }
          }

          Button(
            onClick = onCloseModal,
            colors =
              ButtonDefaults.buttonColors(
                containerColor = Color(0xFF28111B),
                contentColor = Color(0xFFEF4444),
              ),
            border = BorderStroke(1.2.dp, Color(0xFFEF4444)),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
            modifier =
              Modifier.height(if (isCompactPhone) 28.dp else 32.dp)
                .shadow(2.dp, RoundedCornerShape(8.dp))
                .testTag("syndicates_close_button"),
          ) {
            Text(
              text = "✕ CLOSE",
              fontSize = 9.5.sp,
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.SansSerif,
            )
          }
        }

        Spacer(modifier = Modifier.height(if (isCompactPhone) 4.dp else 7.dp))

        // 4 Dedicated Syndicate Command Tabs (Decoupled from Armada Fleet Deck)
        Row(
          modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          SyndicateTabButton(
            label = if (isCompactPhone) "1. DIRECTORY & JOIN" else "1. DIRECTORY & JOIN",
            selected = activeTab == 0,
            onClick = { activeTab = 0 },
            tag = "syndicate_tab_directory",
          )
          SyndicateTabButton(
            label = if (isCompactPhone) "2. ROSTER" else "2. MY SYNDICATE ROSTER",
            selected = activeTab == 1,
            onClick = { activeTab = 1 },
            tag = "syndicate_tab_roster",
          )
          SyndicateTabButton(
            label = if (isCompactPhone) "3. BOUNTIES & MEDALS" else "3. SYNDICATE BOUNTIES & MEDALS",
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

        // Drifter [RFT] Syndicate Lockout Banner or Guest Syndicate Placeholder Notice Banner
        if (isDrifter) {
          Spacer(modifier = Modifier.height(if (isCompactPhone) 4.dp else 6.dp))
          Box(
            modifier =
              Modifier.fillMaxWidth()
                .background(Color(0xFF2B1C08), RoundedCornerShape(8.dp))
                .border(1.2.dp, Color(0xFFF59E0B), RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = if (isCompactPhone) 4.dp else 6.dp)
                .testTag("syndicate_drifter_lock_banner")
          ) {
            Text(
              text =
                "🔒 DRIFTERS WEAR THE [RFT] TAG. SIGN IN AS A SWORN PILOT TO JOIN OR FOUND A SYNDICATE.",
              color = Color(0xFFFACC15),
              fontSize = if (isCompactPhone) 9.sp else 10.5.sp,
              fontWeight = FontWeight.ExtraBold,
              fontFamily = FontFamily.Monospace,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        } else if (isGuestSyndicate && !isCompactPhone) {
          Spacer(modifier = Modifier.height(5.dp))
          Box(
            modifier =
              Modifier.fillMaxWidth()
                .background(Color(0xFF1F1608), RoundedCornerShape(8.dp))
                .border(1.dp, Color(0x88F59E0B), RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 5.dp)
          ) {
            Text(
              text =
                "ℹ️ [GUEST] SYNDICATE FRAMEWORK: Offers zero perks or shared vault benefits. Join or charter an active player Syndicate below to break free from [GUEST] (subject to 15-day allegiance lock).",
              color = Color(0xFFFDE68A),
              fontSize = 10.sp,
              fontWeight = FontWeight.Medium,
              fontFamily = FontFamily.SansSerif,
            )
          }
        }

        if (!statusBannerMessage.isNullOrBlank()) {
          Spacer(modifier = Modifier.height(4.dp))
          Box(
            modifier =
              Modifier.fillMaxWidth()
                .background(Color(0xFF102338), RoundedCornerShape(8.dp))
                .border(1.dp, Color(0x5538BDF8), RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 4.dp)
          ) {
            Text(
              text = statusBannerMessage,
              color = Color(0xFF38BDF8),
              fontSize = if (isCompactPhone) 9.5.sp else 11.sp,
              fontWeight = FontWeight.Medium,
              fontFamily = FontFamily.SansSerif,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }

        Spacer(modifier = Modifier.height(if (isCompactPhone) 4.dp else 7.dp))

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
 * Dedicated [👥 ARMADA FLEET] Modal
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
  val isCompactPhone = LocalConfiguration.current.screenHeightDp < 480
  val isDeployedInArmada = activeArmadaName.isNotEmpty() && armadaSlots.size >= 1

  val outerPadding =
    if (isCompactPhone) {
      PaddingValues(start = 8.dp, end = 8.dp, top = 40.dp, bottom = 4.dp)
    } else {
      PaddingValues(horizontal = 20.dp, vertical = 10.dp)
    }
  val innerPadding = if (isCompactPhone) 8.dp else 14.dp

  Box(
    modifier =
      Modifier.fillMaxSize()
        .background(Color(0xB3000000))
        .imePadding()
        .padding(outerPadding)
        .testTag("armada_fleet_modal"),
    contentAlignment = Alignment.Center,
  ) {
    Surface(
      modifier =
        Modifier.fillMaxSize()
          .border(1.2.dp, Color(0xFF38BDF8), RoundedCornerShape(12.dp)),
      color = Color(0xF2060D1A),
      shape = RoundedCornerShape(12.dp),
    ) {
      Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
        // 1. Unified 30.dp Compact Header Row with Explicit Armada Membership Status Badge
        Row(
          modifier = Modifier.fillMaxWidth().height(if (isCompactPhone) 30.dp else 36.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Text(
              text =
                if (isCompactPhone) "👥 ARMADA FLEET DECK"
                else "👥 ARMADA FLEET DECK // 1-TO-5-PLAYER STRIKE TEAM COMMAND",
              color = Color.White,
              fontSize = if (isCompactPhone) 12.5.sp else 16.sp,
              fontWeight = FontWeight.Black,
              fontFamily = FontFamily.SansSerif,
              letterSpacing = 0.8.sp,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Box(
              modifier =
                Modifier.clip(RoundedCornerShape(6.dp))
                  .background(if (isDeployedInArmada) Color(0xFF06281E) else Color(0xFF0A192F))
                  .border(
                    1.dp,
                    if (isDeployedInArmada) Color(0xFF10B981) else Color(0xFF38BDF8),
                    RoundedCornerShape(6.dp),
                  )
                  .padding(horizontal = 6.dp, vertical = 2.dp),
            ) {
              Text(
                text =
                  if (isDeployedInArmada) {
                    "🟢 [$activeArmadaName] (${armadaSlots.size}/5 • $armadaTierLabel)"
                  } else {
                    "⚡ [SOLO] INDEPENDENT"
                  },
                color = if (isDeployedInArmada) Color(0xFF10B981) else Color(0xFF60A5FA),
                fontSize = if (isCompactPhone) 8.5.sp else 9.5.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
              )
            }
          }

          Button(
            onClick = onCloseModal,
            colors =
              ButtonDefaults.buttonColors(
                containerColor = Color(0xFF28111B),
                contentColor = Color(0xFFEF4444),
              ),
            border = BorderStroke(1.2.dp, Color(0xFFEF4444)),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
            modifier =
              Modifier.height(if (isCompactPhone) 28.dp else 32.dp)
                .shadow(2.dp, RoundedCornerShape(8.dp))
                .testTag("armada_modal_close_button"),
          ) {
            Text(
              text = "✕ CLOSE",
              fontSize = 9.5.sp,
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.SansSerif,
            )
          }
        }

        if (!statusBannerMessage.isNullOrBlank()) {
          Spacer(modifier = Modifier.height(4.dp))
          Box(
            modifier =
              Modifier.fillMaxWidth()
                .background(Color(0xFF102338), RoundedCornerShape(8.dp))
                .border(1.dp, Color(0x553B82F6), RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 4.dp)
          ) {
            Text(
              text = statusBannerMessage,
              color = Color(0xFF60A5FA),
              fontSize = if (isCompactPhone) 9.5.sp else 11.sp,
              fontWeight = FontWeight.Medium,
              fontFamily = FontFamily.SansSerif,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }

        Spacer(modifier = Modifier.height(if (isCompactPhone) 4.dp else 8.dp))

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
  val context = LocalContext.current
  val localPrefs = remember(context) { LocalPilotPrefs(context) }
  // 0 = SYNDICATE WARS (DIRECT H2H), 1 = COLLECTIVE MEMBER PRESTIGE
  var rankingMode by remember { mutableIntStateOf(0) }

  val mergedDirectory =
    remember(directoryList, currentSyndicateTag, rankingMode) {
      val byTag = linkedMapOf<String, SyndicateInfo>()
      val officialLadder =
        listOf(
          SyndicateInfo(
            id = "syn_aex",
            tag = "AEX",
            name = "Apex Vanguard",
            foundryOrigin = "Sector Command Syndicate",
            motto = "First into the Rift, last to yield.",
            memberCount = 18,
            combinedBounty = 14850L,
            isCustom = false,
          ),
          SyndicateInfo(
            id = "syn_obs",
            tag = "OBS",
            name = "Obsidian Core",
            foundryOrigin = "Heavy Siege Syndicate",
            motto = "Unbreakable containment across the sector.",
            memberCount = 15,
            combinedBounty = 12400L,
            isCustom = false,
          ),
          SyndicateInfo(
            id = "syn_sol",
            tag = "SOL",
            name = "Solaris Prime",
            foundryOrigin = "High-Velocity Strike Fleet",
            motto = "Velocity is the ultimate shield.",
            memberCount = 14,
            combinedBounty = 11650L,
            isCustom = false,
          ),
          SyndicateInfo(
            id = "syn_vtx",
            tag = "VTX",
            name = "Vortex Sentinels",
            foundryOrigin = "Stasis Breach Guardians",
            motto = "Hold the line at the singularity core.",
            memberCount = 12,
            combinedBounty = 10900L,
            isCustom = false,
          ),
        )
      for (item in directoryList) {
        val clean = localPrefs.sanitizeSyndicateTag(item.tag)
        if (clean.isNotEmpty() && !clean.equals("RFT", ignoreCase = true)) {
          byTag[clean] = item.copy(tag = clean)
        }
      }
      for (seed in officialLadder) {
        if (!byTag.containsKey(seed.tag)) {
          byTag[seed.tag] = seed
        }
      }
      val mapped =
        byTag.values.map { syn ->
          syn to localPrefs.getSyndicateWarRecord(syn.tag)
        }
      if (rankingMode == 0) {
        mapped.sortedWith(
          compareByDescending<Pair<SyndicateInfo, SyndicateWarRecord>> { it.second.warTrophies }
            .thenByDescending { it.second.warWins }
            .thenByDescending { it.first.combinedBounty }
        )
      } else {
        mapped.sortedWith(
          compareByDescending<Pair<SyndicateInfo, SyndicateWarRecord>> { it.first.combinedBounty }
            .thenByDescending { it.first.memberCount }
            .thenByDescending { it.second.warTrophies }
        )
      }
    }

  Column(
    modifier =
      Modifier.fillMaxSize()
        .background(Color(0x99111C2D), RoundedCornerShape(12.dp))
        .padding(10.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    // Segmented Ranking Mode Toggle: Direct H2H Syndicate Wars vs Collective Member Prestige
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
          modifier =
            Modifier.clip(RoundedCornerShape(8.dp))
              .background(if (rankingMode == 0) Color(0xFFF59E0B) else Color(0xCC0D1B30))
              .border(
                1.dp,
                if (rankingMode == 0) Color(0xFFF59E0B) else Color(0x66F59E0B),
                RoundedCornerShape(8.dp),
              )
              .clickable { rankingMode = 0 }
              .padding(horizontal = 10.dp, vertical = 6.dp)
              .testTag("syndicate_rank_mode_direct_h2h"),
        ) {
          Text(
            text = "🏆 SYNDICATE WARS (DIRECT H2H)",
            color = if (rankingMode == 0) Color(0xFF05080E) else Color(0xFFFBBF24),
            fontSize = 10.5.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = FontFamily.SansSerif,
          )
        }
        Box(
          modifier =
            Modifier.clip(RoundedCornerShape(8.dp))
              .background(if (rankingMode == 1) Color(0xFF00F0FF) else Color(0xCC0D1B30))
              .border(
                1.dp,
                if (rankingMode == 1) Color(0xFF00F0FF) else Color(0x6600F0FF),
                RoundedCornerShape(8.dp),
              )
              .clickable { rankingMode = 1 }
              .padding(horizontal = 10.dp, vertical = 6.dp)
              .testTag("syndicate_rank_mode_collective_prestige"),
        ) {
          Text(
            text = "📊 COLLECTIVE MEMBER PRESTIGE",
            color = if (rankingMode == 1) Color(0xFF05080E) else Color(0xFF00F0FF),
            fontSize = 10.5.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = FontFamily.SansSerif,
          )
        }
      }

      Text(
        text =
          if (rankingMode == 0) {
            "RANKED BY DIRECT H2H WAR TROPHIES (1–5 STRIKE FLEET & 10v10 STASIS BREACH)"
          } else {
            "RANKED BY COMBINED MEMBER BOUNTY & ROSTER SIZE"
          },
        color = Color(0xFF94A3B8),
        fontSize = 9.5.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace,
      )
    }

    LazyColumn(
      modifier = Modifier.weight(1f).fillMaxWidth(),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      items(mergedDirectory, key = { it.first.tag }) { (syn, warRecord) ->
        val isMember = syn.tag.equals(currentSyndicateTag, ignoreCase = true)
        Row(
          modifier =
            Modifier.fillMaxWidth()
              .background(
                if (isMember) Color(0xCC142C4C) else Color(0x990D1B30),
                RoundedCornerShape(10.dp),
              )
              .border(
                1.dp,
                if (isMember) Color(0xFFFFD700) else Color(0x3338BDF8),
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
                text =
                  "🏆 ${warRecord.warTrophies} WAR TROPHIES • ${warRecord.warWins}W - ${warRecord.warLosses}L • 🔥 ${warRecord.warStreak} STREAK",
                color = Color(0xFFFBBF24),
                fontSize = 11.5.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
              )
            }
            Spacer(modifier = Modifier.height(3.dp))
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Text(
                text = "${syn.memberCount} PILOTS • ${syn.combinedBounty} TOTAL BOUNTY",
                color = Color(0xFF38BDF8),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
              )
              Text(
                text = "// ${syn.motto.uppercase()}",
                color = Color(0xFF8A99AD),
                fontSize = 10.5.sp,
                fontFamily = FontFamily.SansSerif,
              )
            }
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
            val isDrifterMode = localPrefs.isGuestBypassEnabled()
            Button(
              onClick = { onJoinSyndicate(syn) },
              enabled = remainingLockDays <= 0 && !isDrifterMode,
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
                  if (isDrifterMode) {
                    "SWORN PILOT ONLY"
                  } else if (remainingLockDays > 0) {
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

  Column(
    modifier =
      Modifier.fillMaxSize()
        .background(Color(0x99111C2D), RoundedCornerShape(10.dp))
        .padding(if (isCompactPhone) 8.dp else 12.dp)
        .verticalScroll(rememberScrollState()),
    verticalArrangement = Arrangement.spacedBy(if (isCompactPhone) 6.dp else 8.dp),
  ) {
    if (isCooldownLocked) {
      Box(
        modifier =
          Modifier.fillMaxWidth()
            .background(Color(0xFF2B1C08), RoundedCornerShape(8.dp))
            .border(1.2.dp, Color(0xFFF59E0B), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = if (isCompactPhone) 5.dp else 8.dp)
            .testTag("syndicate_charter_locked_banner")
      ) {
        Text(
          text =
            if (isDrifter) {
              "🔒 DRIFTERS WEAR THE [RFT] TAG. SIGN IN AS A SWORN PILOT TO JOIN OR FOUND A SYNDICATE."
            } else {
              "🔒 Syndicate chartering locked until 15-day cooldown expiry ($remainingLockFormatted REMAINING)"
            },
          color = Color(0xFFF59E0B),
          fontSize = if (isCompactPhone) 10.sp else 12.sp,
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
          fontSize = if (isCompactPhone) 12.sp else 15.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.SansSerif,
        )
        Text(
          text =
            "CREATING A SYNDICATE REGISTERS YOUR 1–3 CHAR [TAG] (A-Z, 0-9, SYMBOLS) AND LOCKS ALLEGIANCE FOR 15d 00h.",
          color = Color(0xFF8A99AD),
          fontSize = if (isCompactPhone) 9.sp else 10.5.sp,
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
          border = BorderStroke(1.2.dp, Color(0xFF00E676)),
          shape = RoundedCornerShape(8.dp),
          contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
          modifier =
            Modifier.height(if (isCompactPhone) 30.dp else 34.dp)
              .shadow(2.dp, RoundedCornerShape(8.dp))
              .testTag("create_syndicate_done_button"),
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

    Column(
      modifier =
        Modifier.fillMaxWidth()
          .alpha(if (isCooldownLocked) 0.4f else 1.0f),
      verticalArrangement = Arrangement.spacedBy(if (isCompactPhone) 6.dp else 8.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Column(
          modifier = Modifier.width(if (isCompactPhone) 185.dp else 210.dp),
          verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
          Text(
            text = "TAG (1–3 CHARS: A-Z, 0-9, SYMBOLS)",
            color = Color(0xFFFACC15),
            fontSize = 8.5.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
          )
          Box(
            modifier =
              Modifier.fillMaxWidth()
                .height(inputBoxHeight)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF050B14))
                .border(1.dp, Color(0xFFFFD700), RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp),
            contentAlignment = Alignment.CenterStart,
          ) {
            if (tagInput.isEmpty()) {
              Text(
                text = "e.g. VX!, Ω7, ★",
                fontSize = 10.sp,
                color = Color(0xFF64748B),
                fontFamily = FontFamily.Monospace,
              )
            }
            BasicTextField(
              value = tagInput,
              onValueChange = { raw ->
                if (!isCooldownLocked) {
                  tagInput = localPrefs.sanitizeSyndicateTag(raw)
                }
              },
              enabled = !isCooldownLocked,
              readOnly = isCooldownLocked,
              singleLine = true,
              cursorBrush = SolidColor(Color(0xFFFFD700)),
              keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
              keyboardActions =
                KeyboardActions(
                  onDone = {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                  }
                ),
              textStyle =
                TextStyle(
                  color = Color(0xFFFFD700),
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Bold,
                  fontFamily = FontFamily.Monospace,
                ),
              modifier =
                Modifier.fillMaxWidth()
                  .onFocusChanged { anyFieldFocused = it.isFocused }
                  .testTag("create_syndicate_tag_input"),
            )
          }
        }

        Column(
          modifier = Modifier.weight(1f),
          verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
          Text(
            text = "SYNDICATE NAME",
            color = Color(0xFF00F5D4),
            fontSize = 8.5.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
          )
          Box(
            modifier =
              Modifier.fillMaxWidth()
                .height(inputBoxHeight)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF050B14))
                .border(1.dp, Color(0xFF00F5D4), RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp),
            contentAlignment = Alignment.CenterStart,
          ) {
            if (nameInput.isEmpty()) {
              Text(
                text = "Enter Syndicate Name…",
                fontSize = 10.sp,
                color = Color(0xFF64748B),
                fontFamily = FontFamily.SansSerif,
              )
            }
            BasicTextField(
              value = nameInput,
              onValueChange = { raw ->
                if (!isCooldownLocked) {
                  nameInput = raw.take(24)
                }
              },
              enabled = !isCooldownLocked,
              readOnly = isCooldownLocked,
              singleLine = true,
              cursorBrush = SolidColor(Color(0xFF00F5D4)),
              keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
              keyboardActions =
                KeyboardActions(
                  onDone = {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                  }
                ),
              textStyle =
                TextStyle(
                  color = Color.White,
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Bold,
                  fontFamily = FontFamily.SansSerif,
                ),
              modifier =
                Modifier.fillMaxWidth()
                  .onFocusChanged { anyFieldFocused = it.isFocused }
                  .testTag("create_syndicate_name_input"),
            )
          }
        }
      }

      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp),
      ) {
        Text(
          text = "SYNDICATE DOCTRINE / MOTTO",
          color = Color(0xFF38BDF8),
          fontSize = 8.5.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.Monospace,
        )
        Box(
          modifier =
            Modifier.fillMaxWidth()
              .height(inputBoxHeight)
              .clip(RoundedCornerShape(8.dp))
              .background(Color(0xFF050B14))
              .border(1.dp, Color(0xFF38BDF8), RoundedCornerShape(8.dp))
              .padding(horizontal = 10.dp),
          contentAlignment = Alignment.CenterStart,
        ) {
          if (mottoInput.isEmpty()) {
            Text(
              text = "Enter Syndicate Doctrine or Motto…",
              fontSize = 10.sp,
              color = Color(0xFF64748B),
              fontFamily = FontFamily.SansSerif,
            )
          }
          BasicTextField(
            value = mottoInput,
            onValueChange = { raw ->
              if (!isCooldownLocked) {
                mottoInput = raw.take(60)
              }
            },
            enabled = !isCooldownLocked,
            readOnly = isCooldownLocked,
            singleLine = true,
            cursorBrush = SolidColor(Color(0xFF38BDF8)),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions =
              KeyboardActions(
                onDone = {
                  focusManager.clearFocus()
                  keyboardController?.hide()
                }
              ),
            textStyle =
              TextStyle(
                color = Color(0xFFD6E4F0),
                fontSize = 11.5.sp,
                fontFamily = FontFamily.SansSerif,
              ),
            modifier =
              Modifier.fillMaxWidth()
                .onFocusChanged { anyFieldFocused = it.isFocused }
                .testTag("create_syndicate_motto_input"),
          )
        }
      }

      Button(
        onClick = { onCreateCustomSyndicate(tagInput, nameInput, mottoInput) },
        enabled = !isCooldownLocked && tagInput.isNotEmpty() && nameInput.length >= 3,
        colors =
          ButtonDefaults.buttonColors(
            containerColor = Color(0xFFFFD700),
            contentColor = Color(0xFF05080E),
            disabledContainerColor = Color(0xFF1A283B),
            disabledContentColor = Color(0xFF8A99AD),
          ),
        border =
          BorderStroke(
            1.2.dp,
            if (!isCooldownLocked && tagInput.isNotEmpty() && nameInput.length >= 3) Color(0xFFFFD700)
            else Color(0xFF334155),
          ),
        shape = RoundedCornerShape(8.dp),
        modifier =
          Modifier.height(if (isCompactPhone) 38.dp else 44.dp)
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(8.dp))
            .testTag("create_syndicate_submit_button"),
      ) {
        Text(
          text =
            if (isDrifter) {
              "🔒 SIGN IN AS A SWORN PILOT TO CHARTER A SYNDICATE"
            } else if (isCooldownLocked) {
              "🔒 SYNDICATE CHARTERING LOCKED: COOLDOWN ACTIVE ($remainingLockFormatted REMAINING)"
            } else {
              "🏛️ CHARTER [$tagInput] ${nameInput.uppercase()} & LOCK FOR 15d 00h"
            },
          fontSize = if (isCompactPhone) 10.5.sp else 12.sp,
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
  val isCompactPhone = LocalConfiguration.current.screenHeightDp < 480
  val visibleFriends =
    remember(friendsList, blockedPilotUids) {
      friendsList.filter { it.uid !in blockedPilotUids }
    }
  var armadaNameInput by remember(activeArmadaName) {
    mutableStateOf(activeArmadaName.ifEmpty { "APEX_5" })
  }
  var newFriendInput by remember { mutableStateOf("") }
  var inputFocused by remember { mutableStateOf(false) }

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
  val inputBoxHeight = if (isCompactPhone) 34.dp else 38.dp

  Row(
    modifier = Modifier.fillMaxSize(),
    horizontalArrangement = Arrangement.spacedBy(if (isCompactPhone) 8.dp else 12.dp),
  ) {
    // Left Column: Active Named Armada Roster (2 to 5 Players or [SOLO]) — Scrollable on Phone Landscape
    Column(
      modifier =
        Modifier.width(if (isCompactPhone) 268.dp else 305.dp)
          .fillMaxHeight()
          .background(Color(0x99111C2D), RoundedCornerShape(10.dp))
          .padding(if (isCompactPhone) 8.dp else 12.dp)
          .verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(if (isCompactPhone) 6.dp else 8.dp),
    ) {
      Text(
        text =
          if (activeArmadaName.isEmpty()) {
            "⚡ [STATUS: OPERATING INDEPENDENTLY]"
          } else {
            "🛡️ [STATUS: DEPLOYED IN ARMADA: $activeArmadaName] (${armadaSlots.size}/5)"
          },
        color = if (activeArmadaName.isEmpty()) Color(0xFF60A5FA) else Color(0xFF10B981),
        fontSize = if (isCompactPhone) 10.sp else 11.5.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.SansSerif,
      )

      Text(
        text = "ARMADA TAG",
        color = Color(0xFF38BDF8),
        fontSize = 8.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace,
      )

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Box(
          modifier =
            Modifier.weight(1f)
              .height(inputBoxHeight)
              .clip(RoundedCornerShape(8.dp))
              .background(Color(0xFF050B14))
              .border(1.dp, Color(0xFF38BDF8), RoundedCornerShape(8.dp))
              .padding(horizontal = 10.dp),
          contentAlignment = Alignment.CenterStart,
        ) {
          if (armadaNameInput.isEmpty()) {
            Text(
              text = "ARMADA TAG",
              fontSize = 9.5.sp,
              color = Color(0xFF64748B),
              fontFamily = FontFamily.Monospace,
            )
          }
          BasicTextField(
            value = armadaNameInput,
            onValueChange = { raw ->
              armadaNameInput = raw.uppercase().replace(Regex("[^A-Z0-9_-]"), "").take(10)
            },
            singleLine = true,
            cursorBrush = SolidColor(Color(0xFF00F5D4)),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions =
              KeyboardActions(
                onDone = {
                  focusManager.clearFocus()
                  keyboardController?.hide()
                }
              ),
            textStyle =
              TextStyle(
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
              ),
            modifier =
              Modifier.fillMaxWidth()
                .onFocusChanged { inputFocused = it.isFocused }
                .testTag("armada_name_input"),
          )
        }

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
          border = BorderStroke(1.2.dp, Color(0xFF34D399)),
          shape = RoundedCornerShape(8.dp),
          contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
          modifier =
            Modifier.height(inputBoxHeight)
              .shadow(2.dp, RoundedCornerShape(8.dp))
              .testTag("form_armada_button"),
        ) {
          Text(
            text = if (activeArmadaName.isEmpty()) "CREATE ARMADA" else "JOIN / RENAME",
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.SansSerif,
          )
        }
      }

      Column(
        modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        if (armadaSlots.isEmpty()) {
          Box(
            modifier =
              Modifier.fillMaxWidth()
                .background(Color(0x660B1524), RoundedCornerShape(8.dp))
                .padding(8.dp)
          ) {
            Text(
              text =
                "FLYING [SOLO] IN THE FFA RIFT.\nINVITE FRIENDS ON THE RIGHT TO FORM A 2-TO-5-MAN NAMED ARMADA.",
              color = Color(0xFF8A99AD),
              fontSize = if (isCompactPhone) 9.5.sp else 10.5.sp,
              fontFamily = FontFamily.SansSerif,
            )
          }
        } else {
          armadaSlots.forEach { slot ->
            Row(
              modifier =
                Modifier.fillMaxWidth()
                  .background(Color(0x990D1B30), RoundedCornerShape(8.dp))
                  .padding(horizontal = 8.dp, vertical = 6.dp),
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
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif,
                  )
                  Text(
                    text = if (slot.isLeader) "ARMADA COMMANDER" else "WINGMAN ACTIVE",
                    color = Color(0xFF00F0FF),
                    fontSize = 8.8.sp,
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
                  border = BorderStroke(1.2.dp, Color(0xFFFF3B4E)),
                  shape = RoundedCornerShape(8.dp),
                  contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                  modifier = Modifier.height(28.dp).shadow(2.dp, RoundedCornerShape(8.dp)),
                ) {
                  Text(
                    text = "DROP",
                    fontSize = 8.5.sp,
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
          border = BorderStroke(1.2.dp, Color(0xFFFFD700)),
          shape = RoundedCornerShape(8.dp),
          contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
          modifier =
            Modifier.weight(1f)
              .height(if (isCompactPhone) 32.dp else 36.dp)
              .shadow(2.dp, RoundedCornerShape(8.dp))
              .testTag("simulate_roster_shift_button"),
        ) {
          Text(
            text = "⚡ SHIFT 4➔2 TIER",
            fontSize = 9.sp,
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
            border = BorderStroke(1.2.dp, Color(0xFFEF4444)),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
            modifier =
              Modifier.height(if (isCompactPhone) 32.dp else 36.dp)
                .shadow(2.dp, RoundedCornerShape(8.dp))
                .testTag("leave_armada_solo_button"),
          ) {
            Text(
              text = "LEAVE ARMADA",
              fontSize = 9.sp,
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
          .background(Color(0x99111C2D), RoundedCornerShape(10.dp))
          .padding(if (isCompactPhone) 8.dp else 12.dp),
      verticalArrangement = Arrangement.spacedBy(if (isCompactPhone) 6.dp else 8.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Text(
          text =
            if (isCompactPhone) "FRIENDS & RECRUITMENT (${visibleFriends.count { it.isOnline }} ONLINE)"
            else "ONLINE FRIENDS & ARMADA RECRUITMENT (${visibleFriends.count { it.isOnline }} ONLINE)",
          color = Color.White,
          fontSize = if (isCompactPhone) 11.5.sp else 13.5.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.SansSerif,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.weight(1f),
        )

        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Box(
            modifier =
              Modifier.width(if (isCompactPhone) 145.dp else 165.dp)
                .height(inputBoxHeight)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF050B14))
                .border(1.dp, Color(0xFF00F5D4), RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp),
            contentAlignment = Alignment.CenterStart,
          ) {
            if (newFriendInput.isEmpty()) {
              Text(
                text = "SEARCH / ADD CALLSIGN",
                fontSize = 8.5.sp,
                color = Color(0xFF64748B),
                fontFamily = FontFamily.SansSerif,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
            BasicTextField(
              value = newFriendInput,
              onValueChange = { raw ->
                newFriendInput = raw.uppercase().replace(Regex("[^A-Z0-9_-]"), "").take(14)
              },
              singleLine = true,
              cursorBrush = SolidColor(Color(0xFF00F5D4)),
              keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
              keyboardActions =
                KeyboardActions(
                  onDone = {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                  }
                ),
              textStyle =
                TextStyle(
                  color = Color.White,
                  fontSize = 10.5.sp,
                  fontFamily = FontFamily.Monospace,
                ),
              modifier =
                Modifier.fillMaxWidth()
                  .onFocusChanged { inputFocused = it.isFocused }
                  .testTag("friend_search_input"),
            )
          }

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
            border = BorderStroke(1.2.dp, Color(0xFF00F5D4)),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
            modifier =
              Modifier.height(inputBoxHeight)
                .shadow(2.dp, RoundedCornerShape(8.dp)),
          ) {
            Text(
              text = "+ ADD",
              fontSize = 9.5.sp,
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
              border = BorderStroke(1.2.dp, Color(0xFF00E676)),
              shape = RoundedCornerShape(8.dp),
              contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
              modifier =
                Modifier.height(inputBoxHeight)
                  .shadow(2.dp, RoundedCornerShape(8.dp))
                  .testTag("friend_input_done_button"),
            ) {
              Text(
                text = "✓ DONE",
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.SansSerif,
              )
            }
          }
        }
      }

      LazyColumn(
        modifier = Modifier.weight(1f).fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(5.dp),
      ) {
        items(visibleFriends, key = { it.uid }) { friend ->
          val reqKey = "req_${friend.uid}"
          val invKey = "inv_${friend.uid}"
          val reqRem = buttonCooldownSec[reqKey] ?: 0
          val invRem = buttonCooldownSec[invKey] ?: 0

          Row(
            modifier =
              Modifier.fillMaxWidth()
                .background(Color(0x990D1B30), RoundedCornerShape(9.dp))
                .padding(horizontal = 10.dp, vertical = if (isCompactPhone) 6.dp else 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              modifier = Modifier.weight(1f),
            ) {
              Box(
                modifier =
                  Modifier.size(9.dp)
                    .clip(CircleShape)
                    .background(if (friend.isOnline) Color(0xFF00E676) else Color(0xFF546E7A))
              )
              ShipSpritePreview(shipId = friend.shipId, sizeDp = 26)
              Column {
                Text(
                  text =
                    if (friend.currentArmadaName.isNotEmpty()) {
                      "[${friend.currentArmadaName}] [${friend.syndicateTag}] ${friend.callsign}"
                    } else {
                      "[SOLO] [${friend.syndicateTag}] ${friend.callsign}"
                    },
                  color = Color.White,
                  fontSize = if (isCompactPhone) 11.5.sp else 13.sp,
                  fontWeight = FontWeight.Bold,
                  fontFamily = FontFamily.SansSerif,
                )
                Text(
                  text = friend.statusText.uppercase(),
                  color = if (friend.isOnline) Color(0xFF8A99AD) else Color(0xFF607D8B),
                  fontSize = if (isCompactPhone) 9.5.sp else 10.5.sp,
                  fontFamily = FontFamily.SansSerif,
                )
              }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
              if (friend.isOnline) {
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
                        1.2.dp,
                        if (reqLocked) Color(0xFFF59E0B) else Color(0xFF38BDF8),
                      ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier =
                      Modifier.height(if (isCompactPhone) 30.dp else 34.dp)
                        .shadow(2.dp, RoundedCornerShape(8.dp))
                        .testTag("request_join_friend_${friend.callsign}"),
                  ) {
                    Text(
                      text =
                        if (reqLocked) {
                          "⏳ INVITE SENT - ${reqRem}s"
                        } else {
                          "⇥ REQUEST INVITE"
                        },
                      fontSize = 9.sp,
                      fontWeight = FontWeight.Bold,
                      fontFamily = FontFamily.SansSerif,
                    )
                  }
                }

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
                  border =
                    BorderStroke(
                      1.2.dp,
                      if (invLocked) Color(0xFFF59E0B) else Color(0xFF34D399),
                    ),
                  shape = RoundedCornerShape(8.dp),
                  contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                  modifier =
                    Modifier.height(if (isCompactPhone) 30.dp else 34.dp)
                      .shadow(2.dp, RoundedCornerShape(8.dp))
                      .testTag("invite_friend_${friend.callsign}"),
                ) {
                  Text(
                    text =
                      if (invLocked) {
                        "⏳ INVITE SENT - ${invRem}s"
                      } else {
                        "+ INVITE TO ARMADA"
                      },
                    fontSize = 9.sp,
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
                border = BorderStroke(1.2.dp, Color(0xFFF59E0B)),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 8.dp),
                modifier =
                  Modifier.height(if (isCompactPhone) 30.dp else 34.dp)
                    .shadow(2.dp, RoundedCornerShape(8.dp))
                    .testTag("report_armada_friend_${friend.uid}"),
              ) {
                Text("⚑", fontSize = 10.sp, fontWeight = FontWeight.Bold)
              }

              Button(
                onClick = { onBlockPilot(friend.uid, friend.callsign) },
                colors =
                  ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF28111B),
                    contentColor = Color(0xFFEF4444),
                  ),
                border = BorderStroke(1.2.dp, Color(0xFFEF4444)),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 8.dp),
                modifier =
                  Modifier.height(if (isCompactPhone) 30.dp else 34.dp)
                    .shadow(2.dp, RoundedCornerShape(8.dp))
                    .testTag("block_armada_friend_${friend.uid}"),
              ) {
                Text("🚫", fontSize = 10.sp, fontWeight = FontWeight.Bold)
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
  val isCompactPhone = LocalConfiguration.current.screenHeightDp < 480
  Box(
    modifier =
      Modifier.shadow(2.dp, RoundedCornerShape(8.dp))
        .clip(RoundedCornerShape(8.dp))
        .background(if (selected) Color(0xFFFFD700) else Color(0x99111C2D))
        .border(
          1.2.dp,
          if (selected) Color(0xFFFFD700) else Color(0x4438BDF8),
          RoundedCornerShape(8.dp),
        )
        .clickable { onClick() }
        .padding(
          horizontal = if (isCompactPhone) 8.dp else 10.dp,
          vertical = if (isCompactPhone) 4.dp else 7.dp,
        )
        .testTag(tag),
  ) {
    Text(
      text = label,
      color = if (selected) Color(0xFF05080E) else Color.White,
      fontSize = if (isCompactPhone) 9.sp else 10.sp,
      fontWeight = FontWeight.Bold,
      fontFamily = FontFamily.SansSerif,
      maxLines = 1,
    )
  }
}
