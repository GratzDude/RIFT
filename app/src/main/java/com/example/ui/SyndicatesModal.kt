package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
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
import com.example.engine.RiftSpriteBaker
import com.example.engine.RiftVisualForgeState
import com.example.engine.ShipEntity
import com.example.engine.SyndicateCrestRecipe
import java.util.Locale
import kotlinx.coroutines.delay
import org.json.JSONObject

private val Mono = FontFamily.Monospace
private val Sans = FontFamily.SansSerif
private val CitadelTabletBg = Color(0xFF060D1A)
private val CitadelCardBg = Color(0xFF081224)
private val CitadelRowBg = Color(0xFF0A172D)
private val CitadelCyan = Color(0xFF38BDF8)
private val CitadelTeal = Color(0xFF00F5D4)
private val CitadelGold = Color(0xFFFACC15)

private val NON_SYNDICATE_TAGS =
  LocalPilotPrefs.NON_PLAYER_SYNDICATE_TAGS + RiftGameEngine.LEGACY_FOUNDRY_TAGS

@Composable
private fun SyndicateCrestBadge(
  tag: String,
  localPrefs: LocalPilotPrefs,
  currentPlayerTag: String,
  sizeDp: Dp = 36.dp,
) {
  val visualRev = RiftVisualForgeState.visualRevision
  val activePlayerCrest = RiftVisualForgeState.activeSyndicateCrestRecipe
  val normalizedTag = tag.trim().take(4).uppercase(Locale.US)
  val crestBitmap =
    remember(normalizedTag, currentPlayerTag, visualRev, activePlayerCrest) {
      val savedRaw =
        if (normalizedTag.isNotEmpty() && normalizedTag !in NON_SYNDICATE_TAGS) {
          localPrefs.getSyndicateCrestJson(normalizedTag)
        } else {
          null
        }
      val resolvedRecipe =
        when {
          !savedRaw.isNullOrBlank() ->
            runCatching { SyndicateCrestRecipe.fromJson(JSONObject(savedRaw)) }
              .getOrElse {
                SyndicateCrestRecipe.synthesizeFromPrompt(
                  "Syndicate $normalizedTag crest",
                  variationSalt = normalizedTag.hashCode(),
                )
              }
          normalizedTag.isNotEmpty() &&
            normalizedTag.equals(currentPlayerTag.trim(), ignoreCase = true) -> activePlayerCrest
          else ->
            SyndicateCrestRecipe.synthesizeFromPrompt(
              "Syndicate ${normalizedTag.ifEmpty { "NEXUS" }} crest",
              variationSalt = normalizedTag.ifEmpty { "NEXUS" }.hashCode(),
            )
        }
      RiftSpriteBaker.bakeSyndicateCrestBitmap(
          recipe = resolvedRecipe,
          tagText = normalizedTag,
          sizePx = 128,
        )
        .asImageBitmap()
    }

  Image(
    bitmap = crestBitmap,
    contentDescription = "Syndicate Crest $normalizedTag",
    filterQuality = FilterQuality.High,
    modifier = Modifier.size(sizeDp),
  )
}

@Composable
private fun VectorCitadelEmblem(accent: Color, sizeDp: Dp = 18.dp) {
  val shieldPath = remember { Path() }
  Canvas(modifier = Modifier.size(sizeDp)) {
    val cx = size.width * 0.5f
    val cy = size.height * 0.5f
    val r = size.minDimension * 0.44f
    shieldPath.reset()
    shieldPath.moveTo(cx, cy - r)
    shieldPath.lineTo(cx + r * 0.86f, cy - r * 0.42f)
    shieldPath.lineTo(cx + r * 0.68f, cy + r * 0.44f)
    shieldPath.lineTo(cx, cy + r)
    shieldPath.lineTo(cx - r * 0.68f, cy + r * 0.44f)
    shieldPath.lineTo(cx - r * 0.86f, cy - r * 0.42f)
    shieldPath.close()
    drawPath(shieldPath, accent.copy(alpha = 0.20f))
    drawPath(shieldPath, accent, style = Stroke(width = 1.5.dp.toPx()))
    drawCircle(Color.White, radius = r * 0.22f, center = Offset(cx, cy))
  }
}

@Composable
private fun VectorPadlockGlyph(color: Color, sizeDp: Dp = 12.dp) {
  Canvas(modifier = Modifier.size(sizeDp)) {
    val w = size.width
    val h = size.height
    val shackleW = w * 0.48f
    val shackleLeft = (w - shackleW) * 0.5f
    drawArc(
      color = color,
      startAngle = 180f,
      sweepAngle = 180f,
      useCenter = false,
      topLeft = Offset(shackleLeft, h * 0.10f),
      size = Size(shackleW, h * 0.46f),
      style = Stroke(width = 1.4.dp.toPx(), cap = StrokeCap.Round),
    )
    drawRoundRect(
      color = color,
      topLeft = Offset(w * 0.20f, h * 0.44f),
      size = Size(w * 0.60f, h * 0.46f),
      cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx()),
      style = Stroke(width = 1.4.dp.toPx()),
    )
  }
}

@Composable
private fun VectorStrikeWingGlyph(accent: Color, sizeDp: Dp = 18.dp) {
  val wingPath = remember { Path() }
  Canvas(modifier = Modifier.size(sizeDp)) {
    val w = size.width
    val h = size.height
    wingPath.reset()
    wingPath.moveTo(w * 0.50f, h * 0.10f)
    wingPath.lineTo(w * 0.92f, h * 0.78f)
    wingPath.lineTo(w * 0.50f, h * 0.60f)
    wingPath.lineTo(w * 0.08f, h * 0.78f)
    wingPath.close()
    drawPath(wingPath, accent.copy(alpha = 0.22f))
    drawPath(wingPath, accent, style = Stroke(width = 1.4.dp.toPx()))
  }
}

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
    modifier =
      modifier
        .height(height)
        .clip(RoundedCornerShape(8.dp))
        .background(Color(0xFF040914))
        .border(1.dp, borderColor, RoundedCornerShape(8.dp))
        .padding(horizontal = 10.dp),
    contentAlignment = Alignment.CenterStart,
  ) {
    if (value.isEmpty()) {
      Text(
        text = placeholder,
        fontSize = 10.2.sp,
        color = Color(0xFF64748B),
        fontFamily = fontFamily,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
    BasicTextField(
      value = value,
      onValueChange = onValueChange,
      enabled = enabled,
      readOnly = !enabled,
      singleLine = true,
      cursorBrush = SolidColor(borderColor),
      keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
      keyboardActions = KeyboardActions(onDone = { onDone() }),
      textStyle =
        TextStyle(
          color = textColor,
          fontSize = fontSizeSp.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = fontFamily,
        ),
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

  val isRealEnlistedSyndicate =
    !isDrifter && currentSyndicateTag.trim().uppercase(Locale.US) !in NON_SYNDICATE_TAGS

  Column(
    modifier =
      Modifier.fillMaxSize()
        .imePadding()
        .background(CitadelTabletBg, RoundedCornerShape(12.dp))
        .padding(if (isCompactPhone) 6.dp else 8.dp)
        .testTag("syndicates_modal")
  ) {
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
        if (isRealEnlistedSyndicate) {
          SyndicateCrestBadge(
            tag = currentSyndicateTag,
            localPrefs = localPrefs,
            currentPlayerTag = currentSyndicateTag,
            sizeDp = if (isCompactPhone) 28.dp else 34.dp,
          )
        } else {
          VectorCitadelEmblem(
            accent = CitadelTeal,
            sizeDp = if (isCompactPhone) 20.dp else 24.dp,
          )
        }
        val headerLabel =
          if (isRealEnlistedSyndicate) {
            "SYNDICATE CITADEL // [$currentSyndicateTag] $currentSyndicateName"
          } else if (isDrifter) {
            "SYNDICATE CITADEL // [RFT] RIFT DRIFTER"
          } else {
            "SYNDICATE CITADEL // INDEPENDENT PILOT"
          }
        Text(
          text = headerLabel,
          color = Color.White,
          fontSize = if (isCompactPhone) 12.5.sp else 14.sp,
          fontWeight = FontWeight.Black,
          fontFamily = Sans,
          letterSpacing = 0.6.sp,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        val isWarnBadge = remainingLockDays > 0 || !isRealEnlistedSyndicate
        Row(
          modifier =
            Modifier.clip(RoundedCornerShape(6.dp))
              .background(CitadelCardBg)
              .border(
                1.dp,
                if (isWarnBadge) CitadelGold else CitadelCyan,
                RoundedCornerShape(6.dp),
              )
              .padding(horizontal = 7.dp, vertical = 2.5.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
          if (remainingLockDays > 0) {
            VectorPadlockGlyph(color = CitadelGold, sizeDp = 11.dp)
          } else {
            Box(
              modifier =
                Modifier.size(6.dp)
                  .clip(CircleShape)
                  .background(if (isRealEnlistedSyndicate) Color(0xFF10B981) else CitadelGold)
            )
          }
          Text(
            text =
              when {
                remainingLockDays > 0 -> "LOCK: $remainingLockFormatted"
                !isRealEnlistedSyndicate -> "UNAFFILIATED PILOT"
                else -> "ENLISTED (15d LOCK)"
              },
            color = if (isWarnBadge) CitadelGold else CitadelCyan,
            fontSize = if (isCompactPhone) 9.5.sp else 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = Mono,
            maxLines = 1,
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(if (isCompactPhone) 4.dp else 6.dp))

    Row(
      modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      SyndicateTabButton(
        label = "1. DIRECTORY & JOIN",
        selected = activeTab == 0,
        onClick = { activeTab = 0 },
        tag = "syndicate_tab_directory",
      )
      SyndicateTabButton(
        label = "2. SYNDICATE ROSTER",
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

    if (isDrifter) {
      Spacer(modifier = Modifier.height(if (isCompactPhone) 3.dp else 5.dp))
      Row(
        modifier =
          Modifier.fillMaxWidth()
            .background(CitadelCardBg, RoundedCornerShape(8.dp))
            .border(1.2.dp, CitadelGold, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = if (isCompactPhone) 4.dp else 5.dp)
            .testTag("syndicate_drifter_lock_banner"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        VectorPadlockGlyph(color = CitadelGold, sizeDp = 12.dp)
        Text(
          text =
            "DRIFTERS WEAR THE [RFT] TAG. SIGN IN AS A SWORN PILOT IN HELM SYSTEMS TO JOIN OR CHARTER A SYNDICATE.",
          color = CitadelGold,
          fontSize = if (isCompactPhone) 9.5.sp else 10.2.sp,
          fontWeight = FontWeight.ExtraBold,
          fontFamily = Mono,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    } else if (!isRealEnlistedSyndicate && !isCompactPhone) {
      Spacer(modifier = Modifier.height(4.dp))
      Box(
        modifier =
          Modifier.fillMaxWidth()
            .background(CitadelCardBg, RoundedCornerShape(8.dp))
            .border(1.dp, CitadelGold.copy(alpha = 0.65f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
      ) {
        Text(
          text =
            "INDEPENDENT PILOT STATUS: Join or charter a real-player Syndicate below to unlock Syndicate Wars & forged Syndicate Crests (15-day allegiance lock).",
          color = Color(0xFFFEF08A),
          fontSize = 10.sp,
          fontWeight = FontWeight.Medium,
          fontFamily = Sans,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }

    if (!statusBannerMessage.isNullOrBlank()) {
      Spacer(modifier = Modifier.height(3.dp))
      Box(
        modifier =
          Modifier.fillMaxWidth()
            .background(CitadelCardBg, RoundedCornerShape(8.dp))
            .border(1.dp, CitadelCyan.copy(alpha = 0.60f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
      ) {
        Text(
          text = statusBannerMessage,
          color = CitadelCyan,
          fontSize = if (isCompactPhone) 9.6.sp else 10.5.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = Sans,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }

    Spacer(modifier = Modifier.height(if (isCompactPhone) 4.dp else 6.dp))

    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
      when (activeTab) {
        0 ->
          DirectoryAndJoinPane(
            currentSyndicateTag = currentSyndicateTag,
            isDrifter = isDrifter,
            localPrefs = localPrefs,
            remainingLockDays = remainingLockDays,
            remainingLockFormatted = remainingLockFormatted,
            directoryList = directoryList,
            onJoinSyndicate = onJoinSyndicate,
            onSelectCreateTab = { activeTab = 3 },
          )
        1 ->
          MySyndicateRosterPane(
            currentSyndicateTag = currentSyndicateTag,
            currentSyndicateName = currentSyndicateName,
            isDrifter = isDrifter,
            isRealEnlistedSyndicate = isRealEnlistedSyndicate,
            localPrefs = localPrefs,
            remainingLockDays = remainingLockDays,
            remainingLockFormatted = remainingLockFormatted,
            rosterMembers = rosterMembers,
            onLeaveSyndicate = onLeaveSyndicate,
            onOpenDirectoryTab = { activeTab = 0 },
            onOpenCharterTab = { activeTab = 3 },
          )
        2 ->
          BountyCalloutsAndAchievementsPane(
            activeShipsInSector = activeShipsInSector,
            achievements = achievements,
          )
        else ->
          CreateSyndicatePane(
            currentPlayerTag = currentSyndicateTag,
            isDrifter = isDrifter,
            localPrefs = localPrefs,
            remainingLockDays = remainingLockDays,
            remainingLockFormatted = remainingLockFormatted,
            onCreateCustomSyndicate = onCreateCustomSyndicate,
          )
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
  val realArmadaSlots =
    remember(armadaSlots) {
      armadaSlots.filter {
        !it.callsign.contains("[BOT]", ignoreCase = true) &&
          !it.syndicateTag.equals("BOT", ignoreCase = true) &&
          !it.uid.startsWith("bot_", ignoreCase = true)
      }
    }
  val isDeployedInArmada = activeArmadaName.isNotEmpty() && realArmadaSlots.isNotEmpty()

  Column(
    modifier =
      Modifier.fillMaxSize()
        .imePadding()
        .background(CitadelTabletBg, RoundedCornerShape(12.dp))
        .padding(if (isCompactPhone) 6.dp else 8.dp)
        .testTag("armada_fleet_modal")
  ) {
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
        VectorStrikeWingGlyph(
          accent = if (isDeployedInArmada) Color(0xFF10B981) else CitadelCyan,
          sizeDp = if (isCompactPhone) 18.dp else 22.dp,
        )
        Text(
          text =
            if (isCompactPhone) "STRIKE WING COMMAND (1–5 PILOTS)"
            else "STRIKE WING COMMAND // 1–5 SWORN PILOT TACTICAL FORMATION",
          color = Color.White,
          fontSize = if (isCompactPhone) 12.5.sp else 14.sp,
          fontWeight = FontWeight.Black,
          fontFamily = Sans,
          letterSpacing = 0.6.sp,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Row(
          modifier =
            Modifier.clip(RoundedCornerShape(6.dp))
              .background(CitadelCardBg)
              .border(
                1.dp,
                if (isDeployedInArmada) Color(0xFF10B981) else CitadelCyan,
                RoundedCornerShape(6.dp),
              )
              .padding(horizontal = 7.dp, vertical = 2.5.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
          Box(
            modifier =
              Modifier.size(6.dp)
                .clip(CircleShape)
                .background(if (isDeployedInArmada) Color(0xFF10B981) else CitadelCyan)
          )
          Text(
            text =
              if (isDeployedInArmada) {
                "[$activeArmadaName] (${realArmadaSlots.size}/5 • $armadaTierLabel)"
              } else {
                "[SOLO] INDEPENDENT"
              },
            color = if (isDeployedInArmada) Color(0xFF34D399) else CitadelCyan,
            fontSize = if (isCompactPhone) 9.5.sp else 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = Mono,
            maxLines = 1,
          )
        }
      }
    }

    if (!statusBannerMessage.isNullOrBlank()) {
      Spacer(modifier = Modifier.height(4.dp))
      Box(
        modifier =
          Modifier.fillMaxWidth()
            .background(CitadelCardBg, RoundedCornerShape(8.dp))
            .border(1.dp, CitadelCyan.copy(alpha = 0.60f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
      ) {
        Text(
          text = statusBannerMessage,
          color = CitadelCyan,
          fontSize = if (isCompactPhone) 9.6.sp else 10.8.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = Sans,
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
  isDrifter: Boolean,
  localPrefs: LocalPilotPrefs,
  remainingLockDays: Int,
  remainingLockFormatted: String,
  directoryList: List<SyndicateInfo>,
  onJoinSyndicate: (SyndicateInfo) -> Unit,
  onSelectCreateTab: () -> Unit = {},
) {
  val isCompactPhone = LocalConfiguration.current.screenHeightDp < 480
  var rankingMode by remember { mutableIntStateOf(0) }

  val mergedDirectory =
    remember(directoryList, currentSyndicateTag, rankingMode) {
      val byTag = linkedMapOf<String, SyndicateInfo>()
      for (item in directoryList) {
        val clean = localPrefs.sanitizeSyndicateTag(item.tag)
        val normalized = item.copy(tag = clean)
        if (clean.isNotEmpty() && localPrefs.isHumanFoundedSyndicateRecord(normalized)) {
          val existing = byTag[clean]
          if (
            existing == null ||
              normalized.memberCount > existing.memberCount ||
              normalized.combinedBounty > existing.combinedBounty
          ) {
            byTag[clean] = normalized
          }
        }
      }
      val mapped = byTag.values.map { syn -> syn to localPrefs.getSyndicateWarRecord(syn.tag) }
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
        .background(CitadelCardBg, RoundedCornerShape(10.dp))
        .border(1.dp, CitadelCyan.copy(alpha = 0.48f), RoundedCornerShape(10.dp))
        .padding(if (isCompactPhone) 8.dp else 10.dp),
    verticalArrangement = Arrangement.spacedBy(if (isCompactPhone) 6.dp else 8.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
          modifier =
            Modifier.clip(RoundedCornerShape(8.dp))
              .background(if (rankingMode == 0) CitadelGold.copy(alpha = 0.22f) else CitadelTabletBg)
              .border(
                1.dp,
                if (rankingMode == 0) CitadelGold else CitadelGold.copy(alpha = 0.50f),
                RoundedCornerShape(8.dp),
              )
              .clickable { rankingMode = 0 }
              .padding(horizontal = 10.dp, vertical = 5.dp)
              .testTag("syndicate_rank_mode_war_trophies")
        ) {
          Text(
            text = "SYNDICATE WARS LADDER",
            color = if (rankingMode == 0) Color.White else CitadelGold,
            fontSize = if (isCompactPhone) 9.8.sp else 10.8.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = Mono,
          )
        }
        Box(
          modifier =
            Modifier.clip(RoundedCornerShape(8.dp))
              .background(if (rankingMode == 1) CitadelTeal.copy(alpha = 0.22f) else CitadelTabletBg)
              .border(
                1.dp,
                if (rankingMode == 1) CitadelTeal else CitadelCyan.copy(alpha = 0.50f),
                RoundedCornerShape(8.dp),
              )
              .clickable { rankingMode = 1 }
              .padding(horizontal = 10.dp, vertical = 5.dp)
              .testTag("syndicate_rank_mode_collective_prestige")
        ) {
          Text(
            text = "COLLECTIVE MEMBER PRESTIGE",
            color = if (rankingMode == 1) Color.White else CitadelTeal,
            fontSize = if (isCompactPhone) 9.8.sp else 10.8.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = Mono,
          )
        }
      }
      if (!isCompactPhone) {
        Text(
          text =
            if (rankingMode == 0) "RANKED BY SYNDICATE WAR TROPHIES"
            else "RANKED BY COMBINED MEMBER RESONANCE & ROSTER SIZE",
          color = Color(0xFF94A3B8),
          fontSize = 9.6.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = Mono,
        )
      }
    }

    if (mergedDirectory.isEmpty()) {
      Box(
        modifier =
          Modifier.weight(1f)
            .fillMaxWidth()
            .background(CitadelTabletBg, RoundedCornerShape(10.dp))
            .border(1.dp, CitadelCyan.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
            .padding(16.dp),
        contentAlignment = Alignment.Center,
      ) {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          VectorCitadelEmblem(accent = CitadelTeal, sizeDp = 30.dp)
          Text(
            text = "NO SWORN SYNDICATES CHARTERED IN THIS SECTOR YET",
            color = CitadelTeal,
            fontSize = 13.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = Sans,
          )
          Text(
            text =
              "Every Syndicate in NEXUS is 100% real-player-founded. Open Tab 4 to charter the first Syndicate!",
            color = Color(0xFFCBD5E1),
            fontSize = 11.sp,
            fontFamily = Sans,
            textAlign = TextAlign.Center,
          )
          Button(
            onClick = onSelectCreateTab,
            colors =
              ButtonDefaults.buttonColors(
                containerColor = CitadelGold,
                contentColor = Color(0xFF05080E),
              ),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.height(34.dp),
          ) {
            Text(
              text = "CHARTER FIRST SYNDICATE",
              fontSize = 10.5.sp,
              fontWeight = FontWeight.Bold,
              fontFamily = Mono,
            )
          }
        }
      }
    } else {
      LazyColumn(
        modifier = Modifier.weight(1f).fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        items(mergedDirectory, key = { it.first.tag }) { (syn, warRecord) ->
          val isMember =
            !isDrifter && syn.tag.equals(currentSyndicateTag, ignoreCase = true)
          Row(
            modifier =
              Modifier.fillMaxWidth()
                .background(
                  if (isMember) Color(0xFF0E2542) else CitadelRowBg,
                  RoundedCornerShape(10.dp),
                )
                .border(
                  1.dp,
                  if (isMember) CitadelGold else CitadelCyan.copy(alpha = 0.38f),
                  RoundedCornerShape(10.dp),
                )
                .padding(horizontal = 10.dp, vertical = if (isCompactPhone) 8.dp else 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Row(
              modifier = Modifier.weight(1f),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              SyndicateCrestBadge(
                tag = syn.tag,
                localPrefs = localPrefs,
                currentPlayerTag = currentSyndicateTag,
                sizeDp = if (isCompactPhone) 36.dp else 44.dp,
              )
              Column(modifier = Modifier.weight(1f)) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  Text(
                    text = "[${syn.tag}] ${syn.name}",
                    color = if (isMember) CitadelGold else CitadelTeal,
                    fontSize = if (isCompactPhone) 12.5.sp else 14.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = Sans,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                  Text(
                    text =
                      "${warRecord.warTrophies} WAR TROPHIES • ${warRecord.warWins}W-${warRecord.warLosses}L • STREAK ${warRecord.warStreak}",
                    color = CitadelGold,
                    fontSize = if (isCompactPhone) 9.8.sp else 10.8.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = Mono,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                  Text(
                    text = "${syn.memberCount} PILOTS • ${syn.combinedBounty} TOTAL RESONANCE",
                    color = CitadelCyan,
                    fontSize = if (isCompactPhone) 9.8.sp else 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = Mono,
                  )
                  Text(
                    text = "// ${syn.motto.uppercase(Locale.US)}",
                    color = Color(0xFF94A3B8),
                    fontSize = if (isCompactPhone) 9.6.sp else 10.2.sp,
                    fontFamily = Sans,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                }
              }
            }
            if (isMember) {
              Box(
                modifier =
                  Modifier.background(Color(0xFF062E22), RoundedCornerShape(8.dp))
                    .border(1.dp, Color(0xFF10B981), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
              ) {
                Text(
                  text = "ENLISTED",
                  color = Color(0xFF00E676),
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold,
                  fontFamily = Mono,
                )
              }
            } else {
              Button(
                onClick = { onJoinSyndicate(syn) },
                enabled = remainingLockDays <= 0 && !isDrifter,
                colors =
                  ButtonDefaults.buttonColors(
                    containerColor = CitadelTeal,
                    contentColor = Color(0xFF05080E),
                    disabledContainerColor = Color(0xFF132238),
                    disabledContentColor = Color(0xFF94A3B8),
                  ),
                shape = RoundedCornerShape(8.dp),
                modifier =
                  Modifier.height(if (isCompactPhone) 32.dp else 36.dp)
                    .testTag("join_syndicate_${syn.tag}"),
              ) {
                Text(
                  text =
                    when {
                      isDrifter -> "SWORN PILOT ONLY"
                      remainingLockDays > 0 -> "LOCKED ($remainingLockFormatted)"
                      else -> "JOIN [${syn.tag}]"
                    },
                  fontSize = 10.sp,
                  fontWeight = FontWeight.ExtraBold,
                  fontFamily = Mono,
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
private fun MySyndicateRosterPane(
  currentSyndicateTag: String,
  currentSyndicateName: String,
  isDrifter: Boolean,
  isRealEnlistedSyndicate: Boolean,
  localPrefs: LocalPilotPrefs,
  remainingLockDays: Int,
  remainingLockFormatted: String,
  rosterMembers: List<SyndicateMemberEntry>,
  onLeaveSyndicate: () -> Unit,
  onOpenDirectoryTab: () -> Unit,
  onOpenCharterTab: () -> Unit,
) {
  val isCompactPhone = LocalConfiguration.current.screenHeightDp < 480

  if (!isRealEnlistedSyndicate) {
    Box(
      modifier =
        Modifier.fillMaxSize()
          .background(CitadelCardBg, RoundedCornerShape(10.dp))
          .border(1.dp, CitadelCyan.copy(alpha = 0.48f), RoundedCornerShape(10.dp))
          .padding(16.dp),
      contentAlignment = Alignment.Center,
    ) {
      Column(
        modifier =
          Modifier.fillMaxWidth(0.84f)
            .background(CitadelTabletBg, RoundedCornerShape(12.dp))
            .border(1.dp, CitadelCyan.copy(alpha = 0.55f), RoundedCornerShape(12.dp))
            .padding(horizontal = 18.dp, vertical = if (isCompactPhone) 12.dp else 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(if (isCompactPhone) 6.dp else 9.dp),
      ) {
        VectorCitadelEmblem(accent = CitadelTeal, sizeDp = 30.dp)
        Text(
          text = "NO ACTIVE SYNDICATE ALLEGIANCE",
          color = CitadelTeal,
          fontSize = if (isCompactPhone) 13.sp else 14.sp,
          fontWeight = FontWeight.Black,
          fontFamily = Sans,
          letterSpacing = 0.6.sp,
        )
        Text(
          text =
            if (isDrifter) {
              "[RFT] Rift Drifters and Independent Pilots operate solo with zero Syndicate command obligations. Sign in as a Sworn Pilot in HELM SYSTEMS, then join an existing player Syndicate in Tab 1 or charter your own in Tab 4."
            } else {
              "Independent Pilots operate solo until pledging to a player-founded Syndicate. Browse active Syndicates in Tab 1 or charter your own 1–3 character Syndicate in Tab 4."
            },
          color = Color(0xFFCBD5E1),
          fontSize = if (isCompactPhone) 10.sp else 11.sp,
          fontFamily = Sans,
          textAlign = TextAlign.Center,
          lineHeight = if (isCompactPhone) 13.5.sp else 15.sp,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          Button(
            onClick = onOpenDirectoryTab,
            colors =
              ButtonDefaults.buttonColors(
                containerColor = Color(0xFF0284C7),
                contentColor = Color.White,
              ),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.height(34.dp),
          ) {
            Text("BROWSE DIRECTORY", fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = Mono)
          }
          Button(
            onClick = onOpenCharterTab,
            colors =
              ButtonDefaults.buttonColors(
                containerColor = CitadelGold,
                contentColor = Color(0xFF05080E),
              ),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.height(34.dp),
          ) {
            Text("CHARTER SYNDICATE", fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = Mono)
          }
        }
      }
    }
    return
  }

  val humanRoster =
    remember(rosterMembers) {
      rosterMembers.filter {
        !it.callsign.contains("[BOT]", ignoreCase = true) &&
          !it.tag.equals("BOT", ignoreCase = true) &&
          !it.uid.startsWith("bot_", ignoreCase = true)
      }
    }

  Column(
    modifier =
      Modifier.fillMaxSize()
        .background(CitadelCardBg, RoundedCornerShape(10.dp))
        .border(1.dp, CitadelCyan.copy(alpha = 0.48f), RoundedCornerShape(10.dp))
        .padding(if (isCompactPhone) 8.dp else 12.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Row(
        modifier = Modifier.weight(1f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        SyndicateCrestBadge(
          tag = currentSyndicateTag,
          localPrefs = localPrefs,
          currentPlayerTag = currentSyndicateTag,
          sizeDp = if (isCompactPhone) 36.dp else 44.dp,
        )
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text =
              "ACTIVE ROSTER // [$currentSyndicateTag] $currentSyndicateName (${humanRoster.size} MEMBERS)",
            color = Color.White,
            fontSize = if (isCompactPhone) 12.5.sp else 14.sp,
            fontWeight = FontWeight.Black,
            fontFamily = Sans,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Text(
            text = "Customize your Syndicate Crest in STAR FORGE",
            color = CitadelTeal,
            fontSize = if (isCompactPhone) 9.6.sp else 10.2.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = Mono,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }

      Button(
        onClick = onLeaveSyndicate,
        enabled = remainingLockDays <= 0,
        colors =
          ButtonDefaults.buttonColors(
            containerColor = Color(0xFF280E1A),
            contentColor = Color(0xFFFF3B4E),
            disabledContainerColor = Color(0xFF132238),
            disabledContentColor = Color(0xFF94A3B8),
          ),
        border = BorderStroke(1.dp, if (remainingLockDays <= 0) Color(0xFFFF3B4E) else Color(0xFF334155)),
        shape = RoundedCornerShape(8.dp),
        modifier =
          Modifier.height(if (isCompactPhone) 32.dp else 34.dp).testTag("leave_syndicate_button"),
      ) {
        Text(
          text =
            if (remainingLockDays > 0) "LEAVE LOCKED ($remainingLockFormatted)"
            else "LEAVE SYNDICATE (15d 00h LOCK)",
          fontSize = 9.8.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = Mono,
        )
      }
    }

    Spacer(modifier = Modifier.height(8.dp))

    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
      items(humanRoster, key = { it.uid }) { member ->
        val shipSpec = RiftCodex.getShipById(member.shipId)
        Row(
          modifier =
            Modifier.fillMaxWidth()
              .background(CitadelRowBg, RoundedCornerShape(8.dp))
              .border(1.dp, CitadelCyan.copy(alpha = 0.30f), RoundedCornerShape(8.dp))
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
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = Sans,
              )
              Text(
                text =
                  "HULL: ${shipSpec.name.uppercase(Locale.US)} (${shipSpec.role.uppercase(Locale.US)})",
                color = Color(0xFF94A3B8),
                fontSize = 10.2.sp,
                fontFamily = Mono,
              )
            }
          }
          Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(
              text = "KILLS: ${member.kills}",
              color = CitadelTeal,
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              fontFamily = Mono,
            )
            Text(
              text = "RESONANCE: ${member.bountyContribution}",
              color = CitadelGold,
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              fontFamily = Mono,
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
  Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
    Column(
      modifier =
        Modifier.weight(1f)
          .fillMaxSize()
          .background(CitadelCardBg, RoundedCornerShape(10.dp))
          .border(1.dp, CitadelGold.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
          .padding(10.dp)
    ) {
      Text(
        text = "HVT // SECTOR HIGH-VALUE TARGETS (RESONANCE >= 250)",
        color = CitadelGold,
        fontSize = 12.5.sp,
        fontWeight = FontWeight.Black,
        fontFamily = Sans,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Text(
        text = "ELIMINATE HVTS FOR RESONANCE SHUTDOWN PAYOUTS & GOLD RADAR PINGS.",
        color = Color(0xFF94A3B8),
        fontSize = 10.sp,
        fontFamily = Sans,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Spacer(modifier = Modifier.height(6.dp))
      if (hvts.isEmpty()) {
        Box(
          modifier =
            Modifier.weight(1f)
              .fillMaxWidth()
              .background(CitadelTabletBg, RoundedCornerShape(8.dp))
              .border(1.dp, CitadelCyan.copy(alpha = 0.30f), RoundedCornerShape(8.dp))
              .padding(12.dp),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = "NO SWORN PILOTS ABOVE 250 RESONANCE IN ACTIVE SECTOR.",
            color = Color(0xFF94A3B8),
            fontSize = 10.2.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = Mono,
            textAlign = TextAlign.Center,
          )
        }
      } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          items(hvts, key = { it.id }) { hvt ->
            Row(
              modifier =
                Modifier.fillMaxWidth()
                  .background(CitadelRowBg, RoundedCornerShape(8.dp))
                  .border(1.dp, CitadelGold.copy(alpha = 0.60f), RoundedCornerShape(8.dp))
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
                  val cleanHvtSyn =
                    hvt.syndicateTag.trim().uppercase(Locale.US).let { raw ->
                      if (raw in NON_SYNDICATE_TAGS) ""
                      else raw.take(3).let { if (it in NON_SYNDICATE_TAGS) "" else it }
                    }
                  val synPrefix = if (cleanHvtSyn.isNotEmpty()) "[$cleanHvtSyn] " else ""
                  Text(
                    text = "HVT // $armTag $synPrefix${hvt.callsign}",
                    color = CitadelGold,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = Sans,
                  )
                  Text(
                    text =
                      "HULL: ${hvt.spec.name.uppercase(Locale.US)} • SECTOR (${(hvt.x / 500f).toInt()},${(hvt.y / 500f).toInt()})",
                    color = Color(0xFF94A3B8),
                    fontSize = 10.sp,
                    fontFamily = Mono,
                  )
                }
              }
              Text(
                text = "${hvt.bounty} RES",
                color = Color(0xFFFF3B4E),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = Mono,
              )
            }
          }
        }
      }
    }

    Column(
      modifier =
        Modifier.weight(1f)
          .fillMaxSize()
          .background(CitadelCardBg, RoundedCornerShape(10.dp))
          .border(1.dp, CitadelTeal.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
          .padding(10.dp)
    ) {
      Text(
        text = "PILOT COMBAT ACHIEVEMENTS",
        color = CitadelTeal,
        fontSize = 12.5.sp,
        fontWeight = FontWeight.Black,
        fontFamily = Sans,
      )
      Spacer(modifier = Modifier.height(6.dp))
      LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        items(achievements, key = { it.id }) { ach ->
          Row(
            modifier =
              Modifier.fillMaxWidth()
                .background(
                  if (ach.unlocked) Color(0xFF08241E) else CitadelRowBg,
                  RoundedCornerShape(8.dp),
                )
                .border(
                  1.dp,
                  if (ach.unlocked) Color(0xFF10B981) else CitadelCyan.copy(alpha = 0.35f),
                  RoundedCornerShape(8.dp),
                )
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = if (ach.unlocked) "[UNLOCKED] ${ach.title}" else "[LOCKED] ${ach.title}",
                color = if (ach.unlocked) Color(0xFF00E676) else CitadelTeal,
                fontSize = 11.8.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = Sans,
              )
              Text(
                text = ach.description.uppercase(Locale.US),
                color = Color(0xFF94A3B8),
                fontSize = 9.8.sp,
                fontFamily = Sans,
              )
            }
            Text(
              text = ach.progressText,
              color = CitadelGold,
              fontSize = 11.5.sp,
              fontWeight = FontWeight.Bold,
              fontFamily = Mono,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun CreateSyndicatePane(
  currentPlayerTag: String,
  isDrifter: Boolean,
  localPrefs: LocalPilotPrefs,
  remainingLockDays: Int,
  remainingLockFormatted: String,
  onCreateCustomSyndicate: (tag: String, name: String, motto: String) -> Unit,
) {
  val isCompactPhone = LocalConfiguration.current.screenHeightDp < 480
  val isCooldownLocked = remainingLockDays > 0 || isDrifter

  var tagInput by remember { mutableStateOf("") }
  var nameInput by remember { mutableStateOf("") }
  var mottoInput by remember { mutableStateOf("") }
  var anyFieldFocused by remember { mutableStateOf(false) }

  val focusManager = LocalFocusManager.current
  val keyboardController = LocalSoftwareKeyboardController.current
  val inputBoxHeight = 36.dp
  val dismissIme = {
    focusManager.clearFocus()
    keyboardController?.hide()
  }

  Row(
    modifier =
      Modifier.fillMaxSize()
        .background(CitadelCardBg, RoundedCornerShape(10.dp))
        .border(1.dp, CitadelCyan.copy(alpha = 0.48f), RoundedCornerShape(10.dp))
        .padding(if (isCompactPhone) 8.dp else 12.dp),
    horizontalArrangement = Arrangement.spacedBy(if (isCompactPhone) 10.dp else 14.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    // Left Column (0.32f): Live Forged Crest Preview Pedestal + 15d 00h Allegiance Lock Summary
    Column(
      modifier =
        Modifier.weight(0.32f)
          .fillMaxHeight()
          .background(CitadelTabletBg, RoundedCornerShape(10.dp))
          .border(1.dp, CitadelTeal.copy(alpha = 0.58f), RoundedCornerShape(10.dp))
          .padding(horizontal = 10.dp, vertical = if (isCompactPhone) 6.dp else 10.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.SpaceEvenly,
    ) {
      Text(
        text = "LIVE FORGED CREST",
        color = CitadelTeal,
        fontSize = if (isCompactPhone) 9.5.sp else 10.2.sp,
        fontWeight = FontWeight.Black,
        fontFamily = Mono,
      )
      Box(
        modifier =
          Modifier.size(if (isCompactPhone) 68.dp else 78.dp)
            .background(Color(0xFF040914), RoundedCornerShape(12.dp))
            .border(1.dp, CitadelCyan.copy(alpha = 0.65f), RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center,
      ) {
        SyndicateCrestBadge(
          tag = tagInput.ifBlank { "TAG" },
          localPrefs = localPrefs,
          currentPlayerTag = currentPlayerTag,
          sizeDp = if (isCompactPhone) 60.dp else 68.dp,
        )
      }
      Text(
        text = "[${tagInput.ifBlank { "TAG" }}] ${nameInput.ifBlank { "SYNDICATE" }.uppercase(Locale.US)}",
        color = CitadelGold,
        fontSize = if (isCompactPhone) 9.8.sp else 10.8.sp,
        fontWeight = FontWeight.ExtraBold,
        fontFamily = Mono,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Box(
        modifier =
          Modifier.fillMaxWidth()
            .background(CitadelCardBg, RoundedCornerShape(6.dp))
            .border(1.dp, CitadelGold.copy(alpha = 0.50f), RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          text = "15d 00h ALLEGIANCE LOCK\nFORGE CREST IN STAR FORGE",
          color = Color(0xFFE2E8F0),
          fontSize = if (isCompactPhone) 9.sp else 9.5.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = Mono,
          textAlign = TextAlign.Center,
          lineHeight = 11.5.sp,
        )
      }
    }

    // Right Column (0.68f): Zero-Scroll Tag/Name Row + Motto Row + Charter Submit Button
    Column(
      modifier = Modifier.weight(0.68f).fillMaxHeight(),
      verticalArrangement = Arrangement.SpaceBetween,
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "CHARTER OFFICIAL PLAYER SYNDICATE",
            color = Color.White,
            fontSize = if (isCompactPhone) 12.sp else 13.8.sp,
            fontWeight = FontWeight.Black,
            fontFamily = Sans,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          if (!isDrifter && remainingLockDays > 0) {
            Text(
              text = "ALLEGIANCE COOLDOWN ACTIVE: $remainingLockFormatted REMAINING",
              color = CitadelGold,
              fontSize = 9.6.sp,
              fontWeight = FontWeight.Bold,
              fontFamily = Mono,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          } else {
            Text(
              text = "REGISTERS YOUR 1–3 CHAR [TAG] TO CLOUD DIRECTORY & SYNDICATE WARS",
              color = Color(0xFF94A3B8),
              fontSize = if (isCompactPhone) 9.2.sp else 9.8.sp,
              fontFamily = Mono,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
        if (anyFieldFocused && !isCooldownLocked) {
          Button(
            onClick = { dismissIme() },
            colors =
              ButtonDefaults.buttonColors(
                containerColor = Color(0xFF00E676),
                contentColor = Color(0xFF05080E),
              ),
            border = BorderStroke(1.2.dp, Color(0xFF00E676)),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
            modifier =
              Modifier.height(30.dp)
                .shadow(2.dp, RoundedCornerShape(8.dp))
                .testTag("create_syndicate_done_button"),
          ) {
            Text("DONE", fontSize = 9.8.sp, fontWeight = FontWeight.Bold, fontFamily = Mono)
          }
        }
      }

      Column(
        modifier = Modifier.fillMaxWidth().alpha(if (isCooldownLocked) 0.45f else 1.0f),
        verticalArrangement = Arrangement.spacedBy(if (isCompactPhone) 5.dp else 7.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Column(
            modifier = Modifier.weight(0.36f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
          ) {
            Text(
              text = "TAG (1–3 CHARS)",
              color = CitadelGold,
              fontSize = 9.6.sp,
              fontWeight = FontWeight.Bold,
              fontFamily = Mono,
              maxLines = 1,
            )
            SyndicateInputBox(
              value = tagInput,
              onValueChange = { if (!isCooldownLocked) tagInput = localPrefs.sanitizeSyndicateTag(it) },
              placeholder = "e.g. VEX, A7",
              borderColor = CitadelGold,
              textColor = CitadelGold,
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
          Column(
            modifier = Modifier.weight(0.64f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
          ) {
            Text(
              text = "SYNDICATE NAME",
              color = CitadelTeal,
              fontSize = 9.6.sp,
              fontWeight = FontWeight.Bold,
              fontFamily = Mono,
              maxLines = 1,
            )
            SyndicateInputBox(
              value = nameInput,
              onValueChange = { if (!isCooldownLocked) nameInput = it.take(24) },
              placeholder = "Enter Syndicate Name...",
              borderColor = CitadelTeal,
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

        Column(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
          Text(
            text = "SYNDICATE DOCTRINE / MOTTO",
            color = CitadelCyan,
            fontSize = 9.6.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = Mono,
            maxLines = 1,
          )
          SyndicateInputBox(
            value = mottoInput,
            onValueChange = { if (!isCooldownLocked) mottoInput = it.take(60) },
            placeholder = "Enter Syndicate Doctrine or Motto...",
            borderColor = CitadelCyan,
            textColor = Color(0xFFE2E8F0),
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
      }

      val canSubmit =
        !isDrifter && remainingLockDays <= 0 && tagInput.isNotBlank() && nameInput.length >= 3
      val previewTag = tagInput.ifBlank { "TAG" }
      val previewName = nameInput.ifBlank { "SYNDICATE" }.uppercase(Locale.US)
      Button(
        onClick = {
          dismissIme()
          onCreateCustomSyndicate(tagInput, nameInput, mottoInput)
        },
        enabled = canSubmit,
        colors =
          ButtonDefaults.buttonColors(
            containerColor = CitadelGold,
            contentColor = Color(0xFF05080E),
            disabledContainerColor = Color(0xFF132238),
            disabledContentColor = Color(0xFF94A3B8),
          ),
        border = BorderStroke(1.2.dp, if (canSubmit) CitadelGold else Color(0xFF334155)),
        shape = RoundedCornerShape(8.dp),
        modifier =
          Modifier.height(if (isCompactPhone) 36.dp else 40.dp)
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(8.dp))
            .testTag("create_syndicate_submit_button"),
      ) {
        Text(
          text =
            "CHARTER [$previewTag] $previewName & LOCK FOR 15d 00h",
          fontSize = if (isCompactPhone) 10.5.sp else 11.8.sp,
          fontWeight = FontWeight.Black,
          fontFamily = Mono,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
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
  val visibleFriends =
    remember(friendsList, blockedPilotUids) {
      friendsList.filter { friend ->
        friend.uid !in blockedPilotUids &&
          !friend.uid.startsWith("friend_custom_") &&
          !friend.uid.startsWith("bot_") &&
          !friend.callsign.contains("[BOT]", ignoreCase = true)
      }
    }
  var armadaNameInput by
    remember(activeArmadaName) { mutableStateOf(activeArmadaName.ifEmpty { "APEX_WING" }) }
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
  val dismissIme = {
    focusManager.clearFocus()
    keyboardController?.hide()
  }

  Row(
    modifier = Modifier.fillMaxSize(),
    horizontalArrangement = Arrangement.spacedBy(if (isCompactPhone) 8.dp else 12.dp),
  ) {
    Column(
      modifier =
        Modifier.width(if (isCompactPhone) 272.dp else 310.dp)
          .fillMaxHeight()
          .background(CitadelCardBg, RoundedCornerShape(10.dp))
          .border(1.dp, CitadelCyan.copy(alpha = 0.48f), RoundedCornerShape(10.dp))
          .padding(if (isCompactPhone) 8.dp else 12.dp)
          .verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(if (isCompactPhone) 6.dp else 8.dp),
    ) {
      Text(
        text =
          if (activeArmadaName.isEmpty()) "[STATUS: OPERATING INDEPENDENTLY]"
          else "[STRIKE WING: $activeArmadaName] (${armadaSlots.size}/5 • $armadaTierLabel)",
        color = if (activeArmadaName.isEmpty()) CitadelCyan else Color(0xFF34D399),
        fontSize = if (isCompactPhone) 10.5.sp else 11.8.sp,
        fontWeight = FontWeight.Black,
        fontFamily = Sans,
      )
      Text(
        text = "STRIKE WING TAG (UP TO 14 CHARS)",
        color = CitadelCyan,
        fontSize = 9.5.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = Mono,
      )

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        SyndicateInputBox(
          value = armadaNameInput,
          onValueChange = {
            armadaNameInput =
              it.uppercase(Locale.US).replace(Regex("[^A-Z0-9_-]"), "").take(14)
          },
          placeholder = "STRIKE WING TAG",
          borderColor = CitadelCyan,
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
          onClick = {
            dismissIme()
            onCreateOrRenameArmada(armadaNameInput)
          },
          colors =
            ButtonDefaults.buttonColors(
              containerColor = Color(0xFF10B981),
              contentColor = Color(0xFF05080E),
            ),
          border = BorderStroke(1.2.dp, Color(0xFF34D399)),
          shape = RoundedCornerShape(8.dp),
          contentPadding = PaddingValues(horizontal = 9.dp, vertical = 2.dp),
          modifier =
            Modifier.height(inputBoxHeight)
              .shadow(2.dp, RoundedCornerShape(8.dp))
              .testTag("form_armada_button"),
        ) {
          Text(
            text = if (activeArmadaName.isEmpty()) "FORM WING" else "RENAME WING",
            fontSize = 9.8.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = Mono,
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
                .background(CitadelTabletBg, RoundedCornerShape(8.dp))
                .border(1.dp, CitadelCyan.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                .padding(9.dp)
          ) {
            Text(
              text =
                "FLYING [SOLO] IN OPEN SECTOR.\nLINK SWORN PILOTS ON THE RIGHT TO FORM A 1–5 PILOT STRIKE WING.",
              color = Color(0xFFCBD5E1),
              fontSize = if (isCompactPhone) 9.6.sp else 10.2.sp,
              fontFamily = Sans,
            )
          }
        } else {
          armadaSlots.forEach { slot ->
            Row(
              modifier =
                Modifier.fillMaxWidth()
                  .background(CitadelRowBg, RoundedCornerShape(8.dp))
                  .border(1.dp, CitadelCyan.copy(alpha = 0.32f), RoundedCornerShape(8.dp))
                  .padding(horizontal = 8.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                ShipSpritePreview(shipId = slot.shipId, sizeDp = 26)
                Column {
                  Text(
                    text = "[${slot.syndicateTag}] ${slot.callsign}",
                    color = if (slot.isLeader) CitadelGold else Color.White,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = Sans,
                  )
                  Text(
                    text = if (slot.isLeader) "WING COMMANDER" else "WINGMAN ACTIVE",
                    color = CitadelTeal,
                    fontSize = 9.6.sp,
                    fontFamily = Mono,
                  )
                }
              }
              if (!slot.isLeader) {
                Button(
                  onClick = { onRemoveArmadaMember(slot.callsign) },
                  colors =
                    ButtonDefaults.buttonColors(
                      containerColor = Color(0xFF280E1A),
                      contentColor = Color(0xFFFF3B4E),
                    ),
                  border = BorderStroke(1.2.dp, Color(0xFFFF3B4E)),
                  shape = RoundedCornerShape(8.dp),
                  contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                  modifier = Modifier.height(28.dp).shadow(2.dp, RoundedCornerShape(8.dp)),
                ) {
                  Text("DROP", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, fontFamily = Mono)
                }
              }
            }
          }
        }
      }

      if (activeArmadaName.isNotEmpty()) {
        Button(
          onClick = onLeaveArmadaToSolo,
          colors =
            ButtonDefaults.buttonColors(
              containerColor = Color(0xFF280E1A),
              contentColor = Color(0xFFEF4444),
            ),
          border = BorderStroke(1.2.dp, Color(0xFFEF4444)),
          shape = RoundedCornerShape(8.dp),
          contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
          modifier =
            Modifier.fillMaxWidth()
              .height(if (isCompactPhone) 32.dp else 36.dp)
              .shadow(2.dp, RoundedCornerShape(8.dp))
              .testTag("leave_armada_solo_button"),
        ) {
          Text("LEAVE STRIKE WING", fontSize = 9.8.sp, fontWeight = FontWeight.Bold, fontFamily = Mono)
        }
      }
    }

    Column(
      modifier =
        Modifier.weight(1f)
          .fillMaxHeight()
          .background(CitadelCardBg, RoundedCornerShape(10.dp))
          .border(1.dp, CitadelTeal.copy(alpha = 0.48f), RoundedCornerShape(10.dp))
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
            if (isCompactPhone) {
              "SWORN PILOTS (${visibleFriends.count { it.isOnline }} ONLINE)"
            } else {
              "VERIFIED SWORN PILOTS & STRIKE WING LINK (${visibleFriends.count { it.isOnline }} ONLINE)"
            },
          color = Color.White,
          fontSize = if (isCompactPhone) 11.5.sp else 13.sp,
          fontWeight = FontWeight.Black,
          fontFamily = Sans,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.weight(1f),
        )
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          SyndicateInputBox(
            value = newFriendInput,
            onValueChange = {
              newFriendInput =
                it.uppercase(Locale.US).replace(Regex("[^A-Z0-9_-]"), "").take(14)
            },
            placeholder = "SEARCH SWORN PILOT CALLSIGN",
            borderColor = CitadelTeal,
            textColor = Color.White,
            height = inputBoxHeight,
            modifier = Modifier.width(if (isCompactPhone) 158.dp else 192.dp),
            fontFamily = Mono,
            fontSizeSp = 10.5f,
            testTag = "friend_search_input",
            onFocusChange = { inputFocused = it },
            onDone = { dismissIme() },
          )
          Button(
            onClick = {
              if (newFriendInput.length >= 3) {
                onAddFriendByCallsign(newFriendInput)
                newFriendInput = ""
              }
              dismissIme()
            },
            colors =
              ButtonDefaults.buttonColors(
                containerColor = CitadelTeal,
                contentColor = Color(0xFF05080E),
              ),
            border = BorderStroke(1.2.dp, CitadelTeal),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
            modifier = Modifier.height(inputBoxHeight).shadow(2.dp, RoundedCornerShape(8.dp)),
          ) {
            Text("LINK PILOT", fontSize = 9.8.sp, fontWeight = FontWeight.Bold, fontFamily = Mono)
          }
          if (inputFocused) {
            Button(
              onClick = { dismissIme() },
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
              Text("DONE", fontSize = 9.8.sp, fontWeight = FontWeight.Bold, fontFamily = Mono)
            }
          }
        }
      }

      if (visibleFriends.isEmpty()) {
        Box(
          modifier =
            Modifier.weight(1f)
              .fillMaxWidth()
              .background(CitadelTabletBg, RoundedCornerShape(8.dp))
              .border(1.dp, CitadelCyan.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
              .padding(14.dp),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text =
              "NO LINKED SWORN PILOTS YET.\nENTER A VERIFIED CLOUD PILOT CALLSIGN ABOVE AND TAP [LINK PILOT] TO RECRUIT.",
            color = Color(0xFFCBD5E1),
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = Mono,
            textAlign = TextAlign.Center,
            lineHeight = 14.5.sp,
          )
        }
      } else {
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
                  .background(CitadelRowBg, RoundedCornerShape(9.dp))
                  .border(1.dp, CitadelCyan.copy(alpha = 0.30f), RoundedCornerShape(9.dp))
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
                      .background(if (friend.isOnline) Color(0xFF00E676) else Color(0xFF64748B))
                )
                ShipSpritePreview(shipId = friend.shipId, sizeDp = 28)
                Column {
                  val badge =
                    if (friend.currentArmadaName.isNotEmpty()) "[${friend.currentArmadaName}]"
                    else "[SOLO]"
                  Text(
                    text = "$badge [${friend.syndicateTag}] ${friend.callsign}",
                    color = Color.White,
                    fontSize = if (isCompactPhone) 11.8.sp else 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = Sans,
                  )
                  Text(
                    text = friend.statusText.uppercase(Locale.US),
                    color = if (friend.isOnline) Color(0xFF94A3B8) else Color(0xFF64748B),
                    fontSize = if (isCompactPhone) 9.8.sp else 10.5.sp,
                    fontFamily = Mono,
                  )
                }
              }

              Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
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
                          containerColor = if (reqLocked) CitadelTabletBg else Color(0xFF0E203D),
                          contentColor = if (reqLocked) CitadelGold else CitadelCyan,
                          disabledContainerColor = CitadelTabletBg,
                          disabledContentColor = CitadelGold,
                        ),
                      border =
                        BorderStroke(
                          1.2.dp,
                          if (reqLocked) CitadelGold else CitadelCyan,
                        ),
                      shape = RoundedCornerShape(8.dp),
                      contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                      modifier =
                        Modifier.height(if (isCompactPhone) 30.dp else 34.dp)
                          .shadow(2.dp, RoundedCornerShape(8.dp))
                          .testTag("request_join_friend_${friend.callsign}"),
                    ) {
                      Text(
                        text = if (reqLocked) "SENT (${reqRem}s)" else "JOIN WING",
                        fontSize = 9.6.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = Mono,
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
                        containerColor = if (invLocked) CitadelTabletBg else Color(0xFF10B981),
                        contentColor = if (invLocked) CitadelGold else Color(0xFF05080E),
                        disabledContainerColor = CitadelTabletBg,
                        disabledContentColor = CitadelGold,
                      ),
                    border =
                      BorderStroke(
                        1.2.dp,
                        if (invLocked) CitadelGold else Color(0xFF34D399),
                      ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier =
                      Modifier.height(if (isCompactPhone) 30.dp else 34.dp)
                        .shadow(2.dp, RoundedCornerShape(8.dp))
                        .testTag("invite_friend_${friend.callsign}"),
                  ) {
                    Text(
                      text = if (invLocked) "SENT (${invRem}s)" else "+ INVITE",
                      fontSize = 9.6.sp,
                      fontWeight = FontWeight.Bold,
                      fontFamily = Mono,
                    )
                  }
                }
                Button(
                  onClick = { onReportPilot(friend.uid, friend.callsign) },
                  colors =
                    ButtonDefaults.buttonColors(
                      containerColor = CitadelTabletBg,
                      contentColor = CitadelGold,
                    ),
                  border = BorderStroke(1.2.dp, CitadelGold),
                  shape = RoundedCornerShape(8.dp),
                  contentPadding = PaddingValues(horizontal = 8.dp),
                  modifier =
                    Modifier.height(if (isCompactPhone) 30.dp else 34.dp)
                      .shadow(2.dp, RoundedCornerShape(8.dp))
                      .testTag("report_armada_friend_${friend.uid}"),
                ) {
                  Text("RPT", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, fontFamily = Mono)
                }
                Button(
                  onClick = { onBlockPilot(friend.uid, friend.callsign) },
                  colors =
                    ButtonDefaults.buttonColors(
                      containerColor = Color(0xFF280E1A),
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
                  Text("BLK", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, fontFamily = Mono)
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
        .background(if (selected) CitadelTeal.copy(alpha = 0.24f) else CitadelCardBg)
        .border(
          1.2.dp,
          if (selected) CitadelTeal else CitadelCyan.copy(alpha = 0.48f),
          RoundedCornerShape(8.dp),
        )
        .clickable { onClick() }
        .padding(
          horizontal = if (isCompactPhone) 9.dp else 12.dp,
          vertical = if (isCompactPhone) 4.5.dp else 6.dp,
        )
        .testTag(tag)
  ) {
    Text(
      text = label,
      color = if (selected) Color.White else Color(0xFFE2E8F0),
      fontSize = if (isCompactPhone) 10.sp else 11.2.sp,
      fontWeight = FontWeight.ExtraBold,
      fontFamily = Mono,
      maxLines = 1,
    )
  }
}
