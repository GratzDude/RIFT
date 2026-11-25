package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Base64
import android.webkit.WebView
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.LeaderboardEntry
import com.example.data.LocalPilotPrefs
import com.example.data.PeakBountyRunSnapshot
import com.example.data.PilotCareerStatsSummary
import com.example.data.PilotSurvivalTelemetrySummary
import com.example.data.RiftBillingManager
import com.example.data.SyndicateInfo
import com.example.data.SyndicateServiceEntry
import com.example.data.SyndicateWarRecord
import com.example.data.formatSurvivalDurationSec
import com.example.engine.RiftCodex
import com.example.engine.RiftProgressionCatalog
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val DossierTabletBg = Color(0xFF060D1A)
private val DossierCardBg = Color(0xFF081224)
private val DossierRowBg = Color(0xFF0A162B)
private val DossierCyan = Color(0xFF38BDF8)
private val DossierTeal = Color(0xFF00F5D4)
private val DossierGold = Color(0xFFFACC15)

@Composable
fun DossierModal(
  permanentUid: String,
  callsign: String,
  syndicateTag: String,
  activeArmadaName: String,
  activeArmadaSize: Int,
  shipId: Int,
  pvpKills: Long,
  pvpDeaths: Long,
  pvpBountyPeak: Long,
  allTimeKills: Long,
  allTimeDeaths: Long,
  allTimeBountyPeak: Long,
  convergencesWon: Long,
  pilotLevel: Int = 1,
  currentLevelXp: Long = 0L,
  nextLevelXpSpan: Long = 250L,
  levelProgressFraction: Float = 0f,
  customizationPointsTotal: Int = 100,
  spentCustomizationPoints: Int = 0,
  riftShards: Long = 150L,
  xpFromBuffs: Long = 0L,
  xpFromDamage: Long = 0L,
  xpFromAccuracy: Long = 0L,
  xpFromBountyEarned: Long = 0L,
  xpFromBountyKilled: Long = 0L,
  matchPresencePct: Int = 100,
  fps: Int,
  cloudLeaderboardEntries: List<LeaderboardEntry>,
  allTimePeakSnapshot: PeakBountyRunSnapshot? = null,
  pvpPeakSnapshot: PeakBountyRunSnapshot? = null,
  currentLifeSnapshot: PeakBountyRunSnapshot? = null,
  syndicateServiceHistory: List<SyndicateServiceEntry> = emptyList(),
  onOpenRiftShardsStore: () -> Unit = {},
  onResetLocalPilotData: (() -> Unit)? = null,
  onCallsignChanged: (String) -> Unit = {},
  onUpdateRiftShards: (Long) -> Unit = {},
  onCloseModal: () -> Unit = {},
  liveResonance: Int = -1,
  peakResonance: Int = -1,
  startingBounty: Int = -1,
  onRenameCallsign: ((String) -> Pair<Boolean, String>)? = null,
) {
  var showResetPilotConfirmDialog by remember { mutableStateOf(false) }
  var showHtmlSpreadsheetOverlay by remember { mutableStateOf(false) }
  var telemetryExportStatusMsg by remember { mutableStateOf<String?>(null) }
  BackHandler {
    when {
      showHtmlSpreadsheetOverlay -> showHtmlSpreadsheetOverlay = false
      showResetPilotConfirmDialog -> showResetPilotConfirmDialog = false
      else -> onCloseModal()
    }
  }
  val isCompactPhone = LocalConfiguration.current.screenHeightDp < 480
  val context = LocalContext.current
  val localPrefs = remember(context) { LocalPilotPrefs(context) }
  val uidOrNull = permanentUid.ifBlank { null }
  val isDrifter = remember(permanentUid, syndicateTag) { localPrefs.isGuestBypassEnabled() }
  val shortTag = remember(permanentUid) { localPrefs.getShortPilotTag(permanentUid) }
  val isTesterInfiniteShards = RiftBillingManager.TESTER_INFINITE_SHARDS_ACTIVE
  val shardDisplayText = if (isTesterInfiniteShards) "999,999" else "$riftShards"

  val effectiveLiveRes =
    if (liveResonance >= 0) liveResonance
    else remember(permanentUid) { localPrefs.getLiveResonance(permanentUid) }
  val effectivePeakRes =
    maxOf(
      effectiveLiveRes,
      if (peakResonance >= 0) peakResonance
      else remember(permanentUid) { localPrefs.getPeakResonance(permanentUid) },
    )
  val effectiveStartBounty =
    if (startingBounty >= 100) startingBounty
    else RiftProgressionCatalog.computeStartingBountyForResonance(effectiveLiveRes)
  val conduitTier = remember(effectiveLiveRes) {
    RiftProgressionCatalog.getConduitTierForResonance(effectiveLiveRes)
  }
  val conduitProgress = remember(effectiveLiveRes) {
    RiftProgressionCatalog.getConduitProgressFraction(effectiveLiveRes)
  }
  val nextTierThreshold = remember(effectiveLiveRes) {
    RiftProgressionCatalog.getNextConduitTierThreshold(effectiveLiveRes)
  }

  var showCallsignEditor by remember { mutableStateOf(false) }
  var newCallsignInput by remember(callsign) { mutableStateOf(callsign) }
  var callsignStatusMsg by remember { mutableStateOf<String?>(null) }
  var callsignChangeCount by remember(permanentUid) {
    mutableIntStateOf(localPrefs.getCallsignChangeCount(permanentUid))
  }

  var dossierSection by remember { mutableIntStateOf(0) }
  var activeTab by remember { mutableIntStateOf(1) }
  var selectedArmadaTier by remember { mutableIntStateOf(0) }
  var sortMode by remember { mutableIntStateOf(0) }
  val survivalSummary =
    remember(permanentUid, allTimeKills, allTimeDeaths, convergencesWon, effectiveLiveRes, dossierSection) {
      localPrefs.getPilotSurvivalTelemetrySummary(uidOrNull)
    }
  val careerSummary =
    remember(permanentUid, allTimeKills, allTimeDeaths, convergencesWon, effectiveLiveRes, dossierSection) {
      localPrefs.getPilotCareerStatsSummary(uidOrNull)
    }

  val unifiedLeaderboard =
    remember(
      permanentUid,
      callsign,
      syndicateTag,
      activeArmadaName,
      activeArmadaSize,
      shipId,
      pvpKills,
      pvpDeaths,
      pvpBountyPeak,
      allTimeKills,
      allTimeDeaths,
      allTimeBountyPeak,
      convergencesWon,
      effectiveLiveRes,
      cloudLeaderboardEntries,
      activeTab,
      selectedArmadaTier,
      sortMode,
      isDrifter,
    ) {
      val byUid = linkedMapOf<String, LeaderboardEntry>()
      val isRealSwornSelf =
        !isDrifter &&
          permanentUid.isNotBlank() &&
          !permanentUid.startsWith("dev_") &&
          !permanentUid.startsWith("drifter_") &&
          !syndicateTag.equals("RFT", ignoreCase = true) &&
          !syndicateTag.equals("BOT", ignoreCase = true) &&
          !callsign.contains("[BOT]", ignoreCase = true)
      if (isRealSwornSelf) {
        val shipSpec = RiftCodex.getShipById(shipId)
        val localArmadaBadge = if (activeArmadaName.isNotEmpty()) activeArmadaName else "SOLO"
        val formattedLocalCallsign =
          if (syndicateTag.isNotBlank()) "[$localArmadaBadge] [$syndicateTag] $callsign"
          else "[$localArmadaBadge] $callsign"
        byUid[permanentUid] =
          LeaderboardEntry(
            id = permanentUid,
            userId = permanentUid,
            callsign = formattedLocalCallsign,
            syndicateTag = syndicateTag,
            armadaTag = localArmadaBadge,
            armadaTierSize = activeArmadaSize.coerceIn(1, 5),
            shipId = shipId.toLong(),
            foundry = shipSpec.role,
            pvpKills = pvpKills,
            pvpDeaths = pvpDeaths,
            pvpBounty = maxOf(pvpBountyPeak, effectiveStartBounty.toLong()),
            allTimeKills = allTimeKills,
            allTimeDeaths = allTimeDeaths,
            allTimeBounty = maxOf(allTimeBountyPeak, effectiveStartBounty.toLong()),
            convergencesWon = convergencesWon,
          )
      }

      for (entry in cloudLeaderboardEntries) {
        val uidKey = entry.userId.ifEmpty { entry.id }.trim()
        if (
          uidKey.isNotEmpty() &&
            uidKey != permanentUid &&
            !uidKey.startsWith("dev_") &&
            !uidKey.startsWith("drifter_") &&
            !uidKey.startsWith("bot_", ignoreCase = true) &&
            !entry.callsign.contains("[BOT]", ignoreCase = true) &&
            !entry.syndicateTag.equals("BOT", ignoreCase = true)
        ) {
          val cleanCallsign =
            if (entry.callsign.startsWith("[")) entry.callsign
            else {
              val armBadge = entry.armadaTag.ifBlank { "SOLO" }
              if (entry.syndicateTag.isNotBlank()) "[$armBadge] [${entry.syndicateTag}] ${entry.callsign}"
              else "[$armBadge] ${entry.callsign}"
            }
          byUid[uidKey] = entry.copy(callsign = cleanCallsign)
        }
      }

      byUid.values
        .filter { e ->
          !e.syndicateTag.equals("RFT", ignoreCase = true) &&
            !e.syndicateTag.equals("BOT", ignoreCase = true) &&
            !e.callsign.startsWith("DRIFTER", ignoreCase = true) &&
            !e.callsign.contains("[RFT]", ignoreCase = true) &&
            !e.callsign.contains("[BOT]", ignoreCase = true)
        }
        .filter { e ->
          e.userId == permanentUid ||
            e.id == permanentUid ||
            selectedArmadaTier == 0 ||
            e.armadaTierSize.toInt() == selectedArmadaTier
        }
        .sortedByDescending { e ->
          val isSelfRow = e.userId == permanentUid || e.id == permanentUid
          val k = if (activeTab == 0) e.pvpKills else e.allTimeKills
          val bty = if (activeTab == 0) e.pvpBounty else e.allTimeBounty
          val estRes =
            if (isSelfRow) effectiveLiveRes.toLong()
            else (e.pvpKills * 18L + e.convergencesWon * 30L + (bty - 100L).coerceAtLeast(0L) / 4L)
          when (sortMode) {
            1 -> e.pvpKills * 100_000L + k * 100L + bty
            2 -> e.convergencesWon * 100_000L + bty
            else -> estRes * 10_000L + bty + k * 25L
          }
        }
    }

  val displayedPeakSnapshot =
    if (activeTab == 0) pvpPeakSnapshot else (allTimePeakSnapshot ?: currentLifeSnapshot)

  Box(modifier = Modifier.fillMaxSize()) {
  Column(
    modifier =
      Modifier.fillMaxSize()
        .background(DossierTabletBg, RoundedCornerShape(12.dp))
        .padding(8.dp)
        .testTag("dossier_modal")
  ) {
    val tierDesc =
      if (activeArmadaName.isEmpty() || activeArmadaSize <= 1) "[SOLO]"
      else "${activeArmadaSize}P [$activeArmadaName]"
    val syndicatePrefix = if (syndicateTag.isNotBlank()) "[$syndicateTag] " else ""

    // Top Header
    Row(
      modifier = Modifier.fillMaxWidth().height(if (isCompactPhone) 30.dp else 34.dp),
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
            if (isCompactPhone) "PILOT DOSSIER"
            else "PILOT DOSSIER // HALL OF FAME & SERVICE RECORD",
          color = Color.White,
          fontSize = if (isCompactPhone) 12.5.sp else 14.sp,
          fontWeight = FontWeight.Black,
          letterSpacing = 0.6.sp,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Box(
          modifier =
            Modifier.clip(RoundedCornerShape(6.dp))
              .background(DossierCardBg)
              .border(1.dp, DossierCyan, RoundedCornerShape(6.dp))
              .padding(horizontal = 7.dp, vertical = 2.5.dp)
        ) {
          Text(
            text = "$shortTag • ${syndicatePrefix}$callsign • $tierDesc",
            color = DossierGold,
            fontSize = if (isCompactPhone) 9.5.sp else 10.2.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }

      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        if (onResetLocalPilotData != null) {
          Button(
            onClick = { showResetPilotConfirmDialog = true },
            colors =
              ButtonDefaults.buttonColors(
                containerColor = DossierCardBg,
                contentColor = DossierGold,
              ),
            border = BorderStroke(1.2.dp, DossierGold),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 9.dp, vertical = 2.dp),
            modifier =
              Modifier.height(if (isCompactPhone) 28.dp else 32.dp)
                .shadow(2.dp, RoundedCornerShape(8.dp))
                .testTag("dossier_reset_pilot_button"),
          ) {
            Text(
              text = "RESET PILOT",
              fontSize = 9.6.sp,
              fontWeight = FontWeight.ExtraBold,
              fontFamily = FontFamily.Monospace,
            )
          }
        }
        Row(
          modifier =
            Modifier.height(if (isCompactPhone) 28.dp else 32.dp)
              .shadow(2.dp, RoundedCornerShape(8.dp))
              .clip(RoundedCornerShape(8.dp))
              .background(DossierTabletBg)
              .border(1.2.dp, Color(0xFFC084FC), RoundedCornerShape(8.dp))
              .clickable { onOpenRiftShardsStore() }
              .padding(horizontal = 9.dp)
              .testTag("dossier_btn_get_rift_shards"),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          RiftShardGemIcon(sizeDp = 15.dp)
          Text(
            text = shardDisplayText,
            color = Color(0xFFF3E8FF),
            fontSize = if (isCompactPhone) 10.sp else 11.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = FontFamily.Monospace,
          )
          Box(
            modifier =
              Modifier.background(DossierTeal.copy(alpha = 0.18f), RoundedCornerShape(4.dp))
                .border(1.dp, DossierTeal, RoundedCornerShape(4.dp))
                .padding(horizontal = 5.dp, vertical = 1.5.dp)
          ) {
            Text(
              text = "RIFT CONDUIT",
              color = DossierTeal,
              fontSize = 9.5.sp,
              fontWeight = FontWeight.ExtraBold,
              fontFamily = FontFamily.Monospace,
            )
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(if (isCompactPhone) 3.dp else 4.dp))

    // Sworn Pilot / Drifter Identity Strip
    if (isDrifter) {
      Row(
        modifier =
          Modifier.fillMaxWidth()
            .background(DossierCardBg, RoundedCornerShape(8.dp))
            .border(1.dp, DossierGold, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = if (isCompactPhone) 4.dp else 5.dp)
            .testTag("dossier_drifter_banner"),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text =
            "DRIFTER SESSION ([RFT] GUEST) — Link Google in HELM SYSTEMS to become a Sworn Pilot, lock your callsign, and rank on Leaderboards.",
          color = DossierGold,
          fontSize = if (isCompactPhone) 9.5.sp else 10.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.Monospace,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    } else {
      Column(
        modifier =
          Modifier.fillMaxWidth()
            .background(DossierCardBg, RoundedCornerShape(8.dp))
            .border(1.dp, DossierTeal.copy(alpha = 0.72f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = if (isCompactPhone) 4.dp else 5.dp)
            .testTag("dossier_sworn_pilot_banner"),
        verticalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.weight(1f),
          ) {
            ResonanceCrestIcon(sizeDp = 14.dp)
            Text(
              text = "SWORN PILOT — Cross-Device Cloud Synced • PILOT ID: $shortTag",
              color = DossierTeal,
              fontSize = if (isCompactPhone) 9.5.sp else 10.2.sp,
              fontWeight = FontWeight.Black,
              fontFamily = FontFamily.Monospace,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
          Row(
            modifier =
              Modifier.shadow(2.dp, RoundedCornerShape(8.dp))
                .clip(RoundedCornerShape(8.dp))
                .background(DossierTabletBg)
                .border(1.2.dp, DossierCyan, RoundedCornerShape(8.dp))
                .clickable { showCallsignEditor = !showCallsignEditor }
                .padding(horizontal = 9.dp, vertical = 3.5.dp)
                .testTag("dossier_btn_change_callsign"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
          ) {
            if (callsignChangeCount > 0) {
              RiftShardGemIcon(sizeDp = 13.dp)
            }
            Text(
              text =
                if (callsignChangeCount == 0) "CHANGE CALLSIGN (1ST FREE)"
                else "CHANGE CALLSIGN (250)",
              color = Color.White,
              fontSize = 9.5.sp,
              fontWeight = FontWeight.ExtraBold,
              fontFamily = FontFamily.Monospace,
            )
          }
        }
        if (showCallsignEditor) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Box(
              modifier =
                Modifier.weight(1f)
                  .height(if (isCompactPhone) 34.dp else 36.dp)
                  .clip(RoundedCornerShape(8.dp))
                  .background(Color(0xFF040914))
                  .border(1.dp, DossierTeal, RoundedCornerShape(8.dp))
                  .padding(horizontal = 10.dp),
              contentAlignment = Alignment.CenterStart,
            ) {
              BasicTextField(
                value = newCallsignInput,
                onValueChange = {
                  newCallsignInput =
                    it.uppercase(Locale.US)
                      .filter { c -> c.isLetterOrDigit() || c == '_' || c == '-' }
                      .take(14)
                },
                singleLine = true,
                cursorBrush = SolidColor(DossierTeal),
                textStyle =
                  TextStyle(
                    color = Color.White,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                  ),
                modifier = Modifier.fillMaxWidth().testTag("dossier_callsign_input"),
              )
            }
            Button(
              onClick = {
                if (onRenameCallsign != null) {
                  val (ok, statusMessage) = onRenameCallsign(newCallsignInput)
                  callsignChangeCount = localPrefs.getCallsignChangeCount(permanentUid)
                  callsignStatusMsg = statusMessage
                  if (ok) {
                    showCallsignEditor = false
                  }
                } else {
                  val (success, updatedCallsignOrReason, remainingShards) =
                    localPrefs.changeSwornPilotCallsign(newCallsignInput, riftShards, permanentUid)
                  if (success) {
                    callsignChangeCount = localPrefs.getCallsignChangeCount(permanentUid)
                    onCallsignChanged(updatedCallsignOrReason)
                    onUpdateRiftShards(remainingShards)
                    callsignStatusMsg = "Callsign updated to $updatedCallsignOrReason"
                    showCallsignEditor = false
                  } else {
                    callsignStatusMsg = updatedCallsignOrReason
                  }
                }
              },
              colors =
                ButtonDefaults.buttonColors(
                  containerColor = DossierTabletBg,
                  contentColor = DossierTeal,
                ),
              border = BorderStroke(1.2.dp, DossierTeal),
              shape = RoundedCornerShape(8.dp),
              contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
              modifier =
                Modifier.height(if (isCompactPhone) 34.dp else 36.dp)
                  .shadow(2.dp, RoundedCornerShape(8.dp))
                  .testTag("dossier_btn_confirm_callsign"),
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
              ) {
                if (callsignChangeCount > 0) {
                  RiftShardGemIcon(sizeDp = 13.dp)
                }
                Text(
                  text = if (callsignChangeCount == 0) "APPLY (FREE)" else "APPLY (250)",
                  fontSize = 9.8.sp,
                  fontWeight = FontWeight.Black,
                  fontFamily = FontFamily.Monospace,
                )
              }
            }
          }
        }
        if (callsignStatusMsg != null) {
          Text(
            text = callsignStatusMsg!!,
            color = DossierGold,
            fontSize = 9.5.sp,
            fontFamily = FontFamily.Monospace,
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(if (isCompactPhone) 3.dp else 4.dp))

    // Rift Conduit Resonance Telemetry Card
    val tierColor = Color(conduitTier.accentColorHex)
    Column(
      modifier =
        Modifier.fillMaxWidth()
          .background(DossierCardBg, RoundedCornerShape(8.dp))
          .border(1.dp, tierColor.copy(alpha = 0.65f), RoundedCornerShape(8.dp))
          .padding(horizontal = 10.dp, vertical = if (isCompactPhone) 4.dp else 6.dp)
          .testTag("dossier_career_level_card"),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Box(
            modifier =
              Modifier.background(tierColor.copy(alpha = 0.20f), RoundedCornerShape(5.dp))
                .border(1.dp, tierColor, RoundedCornerShape(5.dp))
                .padding(horizontal = 7.dp, vertical = 2.dp)
          ) {
            Text(
              text = conduitTier.title,
              color = tierColor,
              fontSize = if (isCompactPhone) 9.8.sp else 10.8.sp,
              fontWeight = FontWeight.ExtraBold,
              fontFamily = FontFamily.Monospace,
            )
          }
          ResonanceCrestIcon(sizeDp = 15.dp)
          Text(
            text = "LIVE: $effectiveLiveRes • PEAK: $effectivePeakRes",
            color = Color.White,
            fontSize = if (isCompactPhone) 9.8.sp else 10.8.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
          )
        }
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Text(
            text = "PILOT LVL ${careerSummary.pilotLevel} (${careerSummary.xpIntoLevel}/${careerSummary.xpSpanForLevel} XP)",
            color = DossierCyan,
            fontSize = if (isCompactPhone) 9.6.sp else 10.4.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = FontFamily.Monospace,
          )
          Text(
            text = "START RESONANCE: $effectiveStartBounty",
            color = DossierGold,
            fontSize = if (isCompactPhone) 9.8.sp else 10.8.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = FontFamily.Monospace,
          )
          if (!isCompactPhone) {
            Text(
              text =
                if (effectiveLiveRes >= 1500) "APEX TIER (500 MAX START)"
                else "NEXT TIER AT $nextTierThreshold",
              color = DossierTeal,
              fontSize = 9.8.sp,
              fontWeight = FontWeight.ExtraBold,
              fontFamily = FontFamily.Monospace,
            )
          }
        }
      }
      Spacer(modifier = Modifier.height(3.dp))
      Box(
        modifier =
          Modifier.fillMaxWidth()
            .height(if (isCompactPhone) 4.dp else 5.dp)
            .clip(RoundedCornerShape(2.5.dp))
            .background(Color(0xFF132642))
      ) {
        Box(
          modifier =
            Modifier.fillMaxWidth(conduitProgress.coerceIn(0f, 1f))
              .fillMaxHeight()
              .background(tierColor, RoundedCornerShape(2.5.dp))
        )
      }
    }

    Spacer(modifier = Modifier.height(if (isCompactPhone) 4.dp else 5.dp))

    // Section Switcher
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Row(
        modifier = Modifier.weight(1f, fill = false).horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
      ) {
        DossierSectionButton(
          label = if (isCompactPhone) "LEADERBOARDS" else "SWORN LEADERBOARDS",
          selected = dossierSection == 0,
          accentColor = DossierTeal,
          testTag = "dossier_tab_leaderboards",
        ) {
          dossierSection = 0
        }
        DossierSectionButton(
          label = if (isCompactPhone) "PEAK RESONANCE RUN" else "PEAK RESONANCE & EXECUTIONER",
          selected = dossierSection == 1,
          accentColor = DossierGold,
          testTag = "dossier_tab_peak_snapshot",
        ) {
          dossierSection = 1
        }
        DossierSectionButton(
          label =
            if (isCompactPhone) "SERVICE (${syndicateServiceHistory.size})"
            else "SYNDICATE SERVICE RECORD (${syndicateServiceHistory.size})",
          selected = dossierSection == 2,
          accentColor = Color(0xFF34D399),
          testTag = "dossier_tab_syndicate_history",
        ) {
          dossierSection = 2
        }
        DossierSectionButton(
          label =
            if (isCompactPhone) "SURVIVAL (${survivalSummary.pilotCompletedLives})"
            else "SHIP SURVIVAL & EXPORT (${survivalSummary.pilotCompletedLives})",
          selected = dossierSection == 3,
          accentColor = DossierCyan,
          testTag = "dossier_tab_survival_telemetry",
        ) {
          dossierSection = 3
        }
        DossierSectionButton(
          label =
            if (isCompactPhone) "CAREER LVL ${careerSummary.pilotLevel}"
            else "CAREER & 5-XP (LVL ${careerSummary.pilotLevel})",
          selected = dossierSection == 4,
          accentColor = Color(0xFFC084FC),
          testTag = "dossier_tab_career_xp",
        ) {
          dossierSection = 4
        }
      }
      if (dossierSection == 0 || dossierSection == 1) {
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
          DossierTabChip("ALL-TIME", activeTab == 1) { activeTab = 1 }
          DossierTabChip("PVP ONLY", activeTab == 0) { activeTab = 0 }
        }
      }
    }

    Spacer(modifier = Modifier.height(if (isCompactPhone) 4.dp else 5.dp))

    when (dossierSection) {
      0 -> {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
          DossierStatBox(
            Modifier.weight(1f),
            if (isCompactPhone) "ALL-TIME K/D" else "ALL-TIME KILLS / DEATHS",
            "$allTimeKills K / $allTimeDeaths D",
            DossierTeal,
            isCompactPhone,
          )
          DossierStatBox(
            Modifier.weight(1f),
            if (isCompactPhone) "PVP K/D" else "PVP KILLS / DEATHS",
            "$pvpKills K / $pvpDeaths D",
            DossierGold,
            isCompactPhone,
          )
          DossierStatBox(
            Modifier.weight(1f).clickable { dossierSection = 1 },
            if (isCompactPhone) "PEAK RESONANCE >" else "PEAK RESONANCE (TAP FOR RUN)",
            "$allTimeBountyPeak",
            DossierGold,
            isCompactPhone,
          )
          DossierStatBox(
            Modifier.weight(1f).clickable { dossierSection = 4 },
            if (isCompactPhone) "CAREER LVL >" else "PILOT LEVEL & 5-XP >",
            "LVL ${careerSummary.pilotLevel} (${careerSummary.winPercent}% W)",
            Color(0xFFC084FC),
            isCompactPhone,
          )
          DossierStatBox(
            Modifier.weight(1f).clickable { dossierSection = 2 },
            "SYNDICATE TENURE",
            if (
              syndicateTag.isNotBlank() &&
                syndicateTag.uppercase(Locale.US) !in LocalPilotPrefs.NON_PLAYER_SYNDICATE_TAGS
            ) {
              "[$syndicateTag] >"
            } else {
              "SOLO >"
            },
            Color(0xFF34D399),
            isCompactPhone,
          )
          DossierStatBox(
            Modifier.weight(1f).clickable { dossierSection = 3 },
            if (isCompactPhone) "AVG ALIVE >" else "AVG SHIP ALIVE >",
            if (survivalSummary.pilotCompletedLives > 0)
              "${formatSurvivalDurationSec(survivalSummary.pilotAvgSurvivalSec)} (${survivalSummary.pilotCompletedLives})"
            else "0.0s (0)",
            DossierCyan,
            isCompactPhone,
          )
        }

        Spacer(modifier = Modifier.height(if (isCompactPhone) 4.dp else 5.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            DossierTabChip("RESONANCE", sortMode == 0) { sortMode = 0 }
            DossierTabChip("PVP KILLS", sortMode == 1) { sortMode = 1 }
            DossierTabChip("CONVERGENCES WON", sortMode == 2) { sortMode = 2 }
          }
          Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            DossierTabChip("ALL TIERS", selectedArmadaTier == 0) { selectedArmadaTier = 0 }
            DossierTabChip("SOLO", selectedArmadaTier == 1) { selectedArmadaTier = 1 }
            DossierTabChip("2P", selectedArmadaTier == 2) { selectedArmadaTier = 2 }
            DossierTabChip("3P", selectedArmadaTier == 3) { selectedArmadaTier = 3 }
            DossierTabChip("4P", selectedArmadaTier == 4) { selectedArmadaTier = 4 }
            DossierTabChip("5P", selectedArmadaTier == 5) { selectedArmadaTier = 5 }
          }
        }

        Spacer(modifier = Modifier.height(if (isCompactPhone) 4.dp else 5.dp))

        if (unifiedLeaderboard.isEmpty()) {
          Box(
            modifier =
              Modifier.weight(1f)
                .fillMaxWidth()
                .background(DossierCardBg, RoundedCornerShape(10.dp))
                .border(1.dp, DossierCyan.copy(alpha = 0.48f), RoundedCornerShape(10.dp))
                .padding(16.dp),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text =
                if (isDrifter || permanentUid.startsWith("dev_"))
                  "NO SWORN PILOTS RECORDED IN THIS TIER YET — LINK GOOGLE IN HELM SYSTEMS TO RANK ON OFFICIAL LEADERBOARDS."
                else "NO SWORN PILOTS RECORDED IN THIS TIER YET.",
              color = Color(0xFFCBD5E1),
              fontSize = 11.5.sp,
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.Monospace,
            )
          }
        } else {
          LazyColumn(
            modifier =
              Modifier.weight(1f)
                .fillMaxWidth()
                .background(DossierCardBg, RoundedCornerShape(10.dp))
                .border(1.dp, DossierCyan.copy(alpha = 0.48f), RoundedCornerShape(10.dp))
                .padding(if (isCompactPhone) 6.dp else 8.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
          ) {
            itemsIndexed(unifiedLeaderboard, key = { _, item -> item.userId.ifEmpty { item.id } }) { index, entry ->
              val isSelf = entry.userId == permanentUid || entry.id == permanentUid
              val kills = if (activeTab == 0) entry.pvpKills else entry.allTimeKills
              val deaths = if (activeTab == 0) entry.pvpDeaths else entry.allTimeDeaths
              val bounty = if (activeTab == 0) entry.pvpBounty else entry.allTimeBounty
              val tierBadge =
                if (entry.armadaTierSize <= 1) "SOLO" else "${entry.armadaTierSize}P SQUADRON"
              val rowRes =
                if (isSelf) effectiveLiveRes.toLong()
                else (entry.pvpKills * 18L + entry.convergencesWon * 30L + (bounty - 100L).coerceAtLeast(0L) / 4L)
              Row(
                modifier =
                  Modifier.fillMaxWidth()
                    .background(
                      if (isSelf) Color(0xFF0E2542) else DossierRowBg,
                      RoundedCornerShape(8.dp),
                    )
                    .border(
                      1.dp,
                      if (isSelf) DossierTeal else DossierCyan.copy(alpha = 0.28f),
                      RoundedCornerShape(8.dp),
                    )
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                  Text(
                    text = "#${index + 1}",
                    color = if (index < 3) DossierGold else Color(0xFF94A3B8),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.width(32.dp),
                  )
                  ShipSpritePreview(shipId = entry.shipId.toInt(), sizeDp = 28)
                  Column {
                    Text(
                      text = if (isSelf) "${entry.callsign} (YOU)" else entry.callsign,
                      color = if (isSelf) DossierTeal else Color.White,
                      fontSize = 12.sp,
                      fontWeight = FontWeight.Bold,
                    )
                    Text(
                      text = "WING: $tierBadge • HULL: ${RiftCodex.getShipById(entry.shipId.toInt()).name.uppercase(Locale.US)}",
                      color = Color(0xFF94A3B8),
                      fontSize = 10.sp,
                    )
                  }
                }
                Row(
                  horizontalArrangement = Arrangement.spacedBy(12.dp),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                  ) {
                    ResonanceCrestIcon(sizeDp = 14.dp)
                    Text(
                      text = "$rowRes",
                      color = Color(0xFFFEF08A),
                      fontSize = 11.5.sp,
                      fontWeight = FontWeight.Bold,
                      fontFamily = FontFamily.Monospace,
                    )
                  }
                  Text(
                    text = "WINS: ${entry.convergencesWon}",
                    color = Color(0xFF00E676),
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                  )
                  Text(
                    text = "K/D: $kills/$deaths",
                    color = DossierTeal,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                  )
                  Text(
                    text = "PEAK: $bounty",
                    color = DossierGold,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                  )
                }
              }
            }
          }
        }
      }

      1 ->
        PeakBountyAndExecutionerSection(
          Modifier.weight(1f).fillMaxWidth(),
          activeTab == 0,
          displayedPeakSnapshot,
          currentLifeSnapshot,
        )

      2 ->
        SyndicateServiceRecordSection(
          Modifier.weight(1f).fillMaxWidth(),
          syndicateTag,
          syndicateServiceHistory,
        )

      3 ->
        ShipSurvivalTelemetrySection(
          modifier = Modifier.weight(1f).fillMaxWidth(),
          summary = survivalSummary,
          localPrefs = localPrefs,
          uidOrNull = uidOrNull,
          callsign = callsign,
          liveResonance = effectiveLiveRes,
          statusBanner = telemetryExportStatusMsg,
          onUpdateStatusBanner = { telemetryExportStatusMsg = it },
          onOpenHtmlSpreadsheetModal = { showHtmlSpreadsheetOverlay = true },
        )

      4 ->
        PilotCareerAndXpSection(
          modifier = Modifier.weight(1f).fillMaxWidth(),
          career = careerSummary,
          isCompactPhone = isCompactPhone,
        )
    }
  }

  if (showHtmlSpreadsheetOverlay) {
    SurvivalTelemetrySpreadsheetModal(
      callsign = callsign,
      liveResonance = effectiveLiveRes,
      uidOrNull = uidOrNull,
      localPrefs = localPrefs,
      onClose = { showHtmlSpreadsheetOverlay = false },
    )
  }

  if (showResetPilotConfirmDialog && onResetLocalPilotData != null) {
    Box(
      modifier =
        Modifier.fillMaxSize()
          .background(Color(0xD802050B))
          .clickable { showResetPilotConfirmDialog = false }
          .testTag("dossier_reset_confirm_modal"),
      contentAlignment = Alignment.Center,
    ) {
      Surface(
        modifier =
          Modifier.widthIn(min = 290.dp, max = 460.dp)
            .fillMaxWidth(0.78f)
            .clickable(enabled = false) {}
            .border(
              1.4.dp,
              Brush.linearGradient(listOf(Color(0xFFFB7185), DossierGold, DossierTeal)),
              RoundedCornerShape(14.dp),
            ),
        color = DossierTabletBg,
        shape = RoundedCornerShape(14.dp),
      ) {
        Column(
          modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Text(
            text = "CONFIRM PILOT TELEMETRY RESET",
            color = Color(0xFFFB7185),
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 0.6.sp,
          )
          Text(
            text =
              "Reset your local combat stats, ship Resonance, and progression back to 0 Resonance? Your Sworn Pilot callsign will be preserved, and you will leave your current Squadron.",
            color = Color(0xFFE2E8F0),
            fontSize = 10.5.sp,
            lineHeight = 14.5.sp,
            fontWeight = FontWeight.SemiBold,
          )
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Box(
              modifier =
                Modifier.minimumInteractiveComponentSize()
                  .clip(RoundedCornerShape(8.dp))
                  .background(DossierCardBg)
                  .border(1.dp, DossierTeal, RoundedCornerShape(8.dp))
                  .clickable { showResetPilotConfirmDialog = false }
                  .padding(horizontal = 14.dp, vertical = 7.dp)
                  .testTag("dossier_reset_cancel_btn"),
              contentAlignment = Alignment.Center,
            ) {
              Text(
                text = "[CANCEL]",
                color = DossierTeal,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
              )
            }
            Box(
              modifier =
                Modifier.minimumInteractiveComponentSize()
                  .clip(RoundedCornerShape(8.dp))
                  .background(Color(0xFF310E1B))
                  .border(1.2.dp, Color(0xFFFB7185), RoundedCornerShape(8.dp))
                  .clickable {
                    showResetPilotConfirmDialog = false
                    onResetLocalPilotData()
                  }
                  .padding(horizontal = 14.dp, vertical = 7.dp)
                  .testTag("dossier_reset_confirm_btn"),
              contentAlignment = Alignment.Center,
            ) {
              Text(
                text = "[CONFIRM RESET]",
                color = Color(0xFFFFB4C0),
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
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

@Composable
private fun PeakBountyAndExecutionerSection(
  modifier: Modifier = Modifier,
  isPvpOnlyMode: Boolean,
  snapshot: PeakBountyRunSnapshot?,
  currentLifeSnapshot: PeakBountyRunSnapshot?,
) {
  val dateFormatter = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US) }
  Column(
    modifier =
      modifier
        .verticalScroll(rememberScrollState())
        .background(DossierCardBg, RoundedCornerShape(10.dp))
        .border(1.dp, DossierCyan.copy(alpha = 0.48f), RoundedCornerShape(10.dp))
        .padding(10.dp)
        .testTag("dossier_peak_bounty_section"),
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    val headerLabel =
      if (isPvpOnlyMode) "HALL OF FAME // HIGHEST RESONANCE RUN SNAPSHOT (PVP ONLY)"
      else "HALL OF FAME // HIGHEST RESONANCE RUN SNAPSHOT (ALL-TIME)"
    if (snapshot == null) {
      Box(
        modifier =
          Modifier.fillMaxWidth()
            .background(DossierTabletBg, RoundedCornerShape(10.dp))
            .border(1.dp, DossierGold.copy(alpha = 0.55f), RoundedCornerShape(10.dp))
            .padding(18.dp),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          text =
            if (isPvpOnlyMode)
              "NO PVP PEAK RESONANCE SNAPSHOT RECORDED YET — ENGAGE HOSTILE HUMAN PILOTS IN RANKED CONVERGENCE TO RECORD YOUR BEST PVP LIFE."
            else
              "NO PEAK RESONANCE RUN SNAPSHOT RECORDED YET — DEPLOY INTO THE SECTOR TO RECORD YOUR FIRST LIFE TELEMETRY.",
          color = Color(0xFFCBD5E1),
          fontSize = 11.5.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.Monospace,
        )
      }
    } else {
      Column(
        modifier =
          Modifier.fillMaxWidth()
            .background(DossierTabletBg, RoundedCornerShape(10.dp))
            .border(1.2.dp, DossierGold.copy(alpha = 0.80f), RoundedCornerShape(10.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            ShipSpritePreview(shipId = snapshot.shipId.coerceIn(1, 10), sizeDp = 38)
            Column {
              Text(headerLabel, color = DossierGold, fontSize = 12.5.sp, fontWeight = FontWeight.ExtraBold)
              Text(
                text = "HULL FLOWN: ${snapshot.shipFlown.uppercase(Locale.US)} • RECORDED: ${dateFormatter.format(Date(snapshot.recordedAtMs))}",
                color = DossierCyan,
                fontSize = 10.2.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
              )
            }
          }
          Row(
            modifier =
              Modifier.background(DossierCardBg, RoundedCornerShape(8.dp))
                .border(1.dp, DossierGold, RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            ResonanceCrestIcon(sizeDp = 15.dp)
            Text(
              text = "${snapshot.bountyReached} RESONANCE",
              color = DossierGold,
              fontSize = 13.5.sp,
              fontWeight = FontWeight.ExtraBold,
              fontFamily = FontFamily.Monospace,
            )
          }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          DossierStatBox(Modifier.weight(1f), "TIME SURVIVED", snapshot.formattedDuration(), DossierCyan)
          DossierStatBox(Modifier.weight(1f), "PVP / BOT KILLS", "${snapshot.pvpKills} PVP • ${snapshot.botKills} BOT", DossierTeal)
          DossierStatBox(Modifier.weight(1f), "LEADER SHUTDOWNS", "${snapshot.hvtShutdowns} LEADERS", DossierGold)
          DossierStatBox(Modifier.weight(1f), "CRATES / BUFFS", "${snapshot.buffsCollected} COLLECTED", Color(0xFF34D399))
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          DossierStatBox(Modifier.weight(1f), "DAMAGE DEALT", "${snapshot.damageDealt.toInt()}", Color(0xFF00E676), showFluxEmblem = true)
          DossierStatBox(Modifier.weight(1f), "DAMAGE TAKEN", "${snapshot.damageTaken.toInt()}", Color(0xFFF87171), showFluxEmblem = true)
          DossierStatBox(Modifier.weight(1f), "SHOTS FIRED / HIT", "${snapshot.shotsHit} / ${snapshot.shotsFired}", DossierGold)
          DossierStatBox(Modifier.weight(1f), "COMBAT ACCURACY", "${snapshot.accuracyPercent()}%", Color(0xFFC084FC))
        }

        val exec = snapshot.executioner
        val isConv = exec.survivedToConvergence
        val isLive = !isConv && exec.killerCallsign.isBlank()
        val execBorder =
          when {
            isConv -> Color(0xFF00E676)
            isLive -> DossierCyan
            else -> Color(0xFFFF3B4E)
          }
        val execBg =
          when {
            isConv -> Color(0xFF06241C)
            isLive -> Color(0xFF081C33)
            else -> Color(0xFF220C16)
          }

        Column(
          modifier =
            Modifier.fillMaxWidth()
              .background(execBg, RoundedCornerShape(10.dp))
              .border(1.dp, execBorder, RoundedCornerShape(10.dp))
              .padding(10.dp)
              .testTag("dossier_executioner_card"),
          verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          when {
            isConv -> {
              Text(
                text = "RUN OUTCOME: SURVIVED TO SECTOR CONVERGENCE",
                color = Color(0xFF00E676),
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
              )
              Text(
                text = "Hull remained intact through the 60-minute Sector Convergence cycle.",
                color = Color(0xFFD1FAE5),
                fontSize = 10.5.sp,
              )
            }

            isLive -> {
              Text(
                text = "RUN STATUS: ACTIVE UNDEFEATED STREAK IN SECTOR",
                color = DossierCyan,
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
              )
              Text(
                text = "You are currently flying this record-setting sortie. Executioner telemetry locks if eliminated or at Sector Convergence.",
                color = Color(0xFFBAE6FD),
                fontSize = 10.5.sp,
              )
            }

            else -> {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Column {
                  Text(
                    text = "TERMINATED BY: [${exec.killerSyndicateTag}] ${exec.killerCallsign}",
                    color = Color(0xFFFF3B4E),
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace,
                  )
                  Text(
                    text = "EXECUTIONER LOADOUT: ${exec.killerShip} • ${exec.killingWeapon}",
                    color = Color.White,
                    fontSize = 10.8.sp,
                    fontWeight = FontWeight.Bold,
                  )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                  MetricRightColumn("KILLER RESONANCE AT KILL", "${exec.killerBountyAtKill}", DossierGold)
                  MetricRightColumn("RESONANCE EARNED FROM YOU", "+${exec.bountyEarnedFromYou}", Color(0xFFFF3B4E))
                }
              }
            }
          }
        }
      }
    }

    if (currentLifeSnapshot != null) {
      Row(
        modifier =
          Modifier.fillMaxWidth()
            .background(DossierTabletBg, RoundedCornerShape(10.dp))
            .border(1.dp, DossierCyan.copy(alpha = 0.55f), RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "CURRENT ACTIVE SORTIE (${currentLifeSnapshot.shipFlown}):",
          color = DossierCyan,
          fontSize = 10.8.sp,
          fontWeight = FontWeight.ExtraBold,
          fontFamily = FontFamily.Monospace,
        )
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          FluxEmblem(sizeDp = 13.dp)
          Text(
            text =
              "RESONANCE: ${currentLifeSnapshot.bountyReached} • SURVIVED: ${currentLifeSnapshot.formattedDuration()} • KILLS: ${currentLifeSnapshot.pvpKills + currentLifeSnapshot.botKills} • DMG: ${currentLifeSnapshot.damageDealt.toInt()} • ACC: ${currentLifeSnapshot.accuracyPercent()}%",
            color = Color.White,
            fontSize = 10.2.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
          )
        }
      }
    }
  }
}

@Composable
private fun SyndicateServiceRecordSection(
  modifier: Modifier = Modifier,
  activeSyndicateTag: String = "GUEST",
  entries: List<SyndicateServiceEntry>,
) {
  val context = LocalContext.current
  val localPrefs = remember(context) { LocalPilotPrefs(context) }
  val dateFormatter = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US) }
  val nowMs = System.currentTimeMillis()
  val ordered = remember(entries) { entries.reversed() }

  val cleanActiveTag = remember(activeSyndicateTag, localPrefs) {
    localPrefs.sanitizeSyndicateTag(activeSyndicateTag).ifEmpty { "SOLO" }
  }
  val myWarRecord = remember(cleanActiveTag, entries) { localPrefs.getSyndicateWarRecord(cleanActiveTag) }

  val (topByWarTrophies, topByCollectiveBounty) =
    remember(cleanActiveTag, entries) {
      val byTag = linkedMapOf<String, SyndicateInfo>()
      for (custom in localPrefs.getAllSyndicates()) {
        if (localPrefs.isHumanFoundedSyndicateRecord(custom)) {
          byTag[custom.tag.uppercase(Locale.US)] = custom
        }
      }
      val allPairs = byTag.values.map { syn -> syn to localPrefs.getSyndicateWarRecord(syn.tag) }
      val warSorted =
        allPairs
          .sortedWith(
            compareByDescending<Pair<SyndicateInfo, SyndicateWarRecord>> { it.second.warTrophies }
              .thenByDescending { it.second.warWins }
          )
          .take(4)
      val bountySorted =
        allPairs
          .sortedWith(
            compareByDescending<Pair<SyndicateInfo, SyndicateWarRecord>> { it.first.combinedBounty }
              .thenByDescending { it.first.memberCount }
          )
          .take(4)
      warSorted to bountySorted
    }

  Column(
    modifier =
      modifier
        .background(DossierCardBg, RoundedCornerShape(10.dp))
        .border(1.dp, DossierCyan.copy(alpha = 0.48f), RoundedCornerShape(10.dp))
        .padding(10.dp)
        .testTag("dossier_syndicate_history_section"),
    verticalArrangement = Arrangement.spacedBy(6.dp),
  ) {
    Column(
      modifier =
        Modifier.fillMaxWidth()
          .background(DossierTabletBg, RoundedCornerShape(10.dp))
          .border(1.dp, DossierGold.copy(alpha = 0.72f), RoundedCornerShape(10.dp))
          .padding(horizontal = 10.dp, vertical = 8.dp)
          .testTag("dossier_syndicate_war_performance_card"),
      verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "SYNDICATE WARS PERFORMANCE // [$cleanActiveTag]",
          color = DossierGold,
          fontSize = 11.5.sp,
          fontWeight = FontWeight.ExtraBold,
        )
        Text(
          text = "${myWarRecord.warTrophies} WAR TROPHIES • ${myWarRecord.warWins}W - ${myWarRecord.warLosses}L • STREAK ${myWarRecord.warStreak}",
          color = Color(0xFF00E676),
          fontSize = 10.5.sp,
          fontWeight = FontWeight.ExtraBold,
          fontFamily = FontFamily.Monospace,
        )
      }
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Column(
          modifier =
            Modifier.weight(1f)
              .background(DossierCardBg, RoundedCornerShape(8.dp))
              .border(1.dp, DossierGold.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
              .padding(7.dp),
          verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
          Text(
            text = "SYNDICATE WAR TROPHIES (TOP 4)",
            color = DossierGold,
            fontSize = 9.8.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = FontFamily.Monospace,
          )
          if (topByWarTrophies.isEmpty()) {
            Text(
              text = "No player Syndicates chartered yet",
              color = Color(0xFF94A3B8),
              fontSize = 9.8.sp,
              fontFamily = FontFamily.Monospace,
            )
          } else {
            topByWarTrophies.forEachIndexed { idx, (syn, rec) ->
              Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                  text = "#${idx + 1} [${syn.tag}] ${syn.name}",
                  color = if (syn.tag == cleanActiveTag) DossierTeal else Color.White,
                  fontSize = 9.8.sp,
                  fontWeight = FontWeight.Bold,
                  fontFamily = FontFamily.Monospace,
                )
                Text(
                  text = "${rec.warTrophies} (${rec.warWins}W-${rec.warLosses}L)",
                  color = DossierGold,
                  fontSize = 9.8.sp,
                  fontWeight = FontWeight.Bold,
                  fontFamily = FontFamily.Monospace,
                )
              }
            }
          }
        }
        Column(
          modifier =
            Modifier.weight(1f)
              .background(DossierCardBg, RoundedCornerShape(8.dp))
              .border(1.dp, DossierCyan.copy(alpha = 0.48f), RoundedCornerShape(8.dp))
              .padding(7.dp),
          verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
          Text(
            text = "COLLECTIVE MEMBER RESONANCE (TOP 4)",
            color = DossierTeal,
            fontSize = 9.8.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = FontFamily.Monospace,
          )
          if (topByCollectiveBounty.isEmpty()) {
            Text(
              text = "No player Syndicates chartered yet",
              color = Color(0xFF94A3B8),
              fontSize = 9.8.sp,
              fontFamily = FontFamily.Monospace,
            )
          } else {
            topByCollectiveBounty.forEachIndexed { idx, (syn, _) ->
              Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                  text = "#${idx + 1} [${syn.tag}] ${syn.name}",
                  color = if (syn.tag == cleanActiveTag) DossierGold else Color.White,
                  fontSize = 9.8.sp,
                  fontWeight = FontWeight.Bold,
                  fontFamily = FontFamily.Monospace,
                )
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                  ResonanceCrestIcon(sizeDp = 12.dp)
                  Text(
                    text = "${syn.combinedBounty} (${syn.memberCount}P)",
                    color = DossierCyan,
                    fontSize = 9.8.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                  )
                }
              }
            }
          }
        }
      }
    }

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = "CHRONOLOGICAL SYNDICATE SERVICE RECORD (MOST RECENT FIRST)",
        color = Color(0xFF34D399),
        fontSize = 11.5.sp,
        fontWeight = FontWeight.ExtraBold,
      )
      Text(
        text = "TOTAL COMMISSIONS: ${ordered.size}",
        color = DossierGold,
        fontSize = 10.2.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace,
      )
    }

    LazyColumn(
      modifier = Modifier.weight(1f).fillMaxWidth(),
      verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      itemsIndexed(ordered) { _, item ->
        val isActive = item.isActive
        Row(
          modifier =
            Modifier.fillMaxWidth()
              .background(if (isActive) Color(0xFF08241E) else DossierRowBg, RoundedCornerShape(9.dp))
              .border(
                1.dp,
                if (isActive) Color(0xFF10B981) else DossierCyan.copy(alpha = 0.32f),
                RoundedCornerShape(9.dp),
              )
              .padding(horizontal = 12.dp, vertical = 8.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1.4f)) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Box(
                modifier =
                  Modifier.background(
                    if (isActive) Color(0xFF10B981) else DossierTabletBg,
                    RoundedCornerShape(5.dp),
                  )
                    .border(
                      1.dp,
                      if (isActive) Color(0xFF10B981) else DossierCyan.copy(alpha = 0.45f),
                      RoundedCornerShape(5.dp),
                    )
                    .padding(horizontal = 6.dp, vertical = 2.dp)
              ) {
                Text(
                  text = if (isActive) "ACTIVE" else "HONORABLE DISCHARGE",
                  color = if (isActive) Color(0xFF04130E) else Color(0xFFCBD5E1),
                  fontSize = 9.5.sp,
                  fontWeight = FontWeight.ExtraBold,
                  fontFamily = FontFamily.Monospace,
                )
              }
              Text(
                text = "[${item.syndicateTag}] ${item.syndicateName}",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
              )
            }
            val joinedStr = dateFormatter.format(Date(item.joinedTimestamp))
            val leftStr = if (isActive) "PRESENT" else dateFormatter.format(Date(item.leftTimestamp))
            Text(
              text = "ROLE: ${item.role.uppercase(Locale.US)} • COMMISSION: $joinedStr -> $leftStr",
              color = Color(0xFF94A3B8),
              fontSize = 10.sp,
              fontFamily = FontFamily.Monospace,
            )
          }
          Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            MetricRightColumn("TENURE", item.formatTenure(nowMs), DossierCyan)
            MetricRightColumn("BANNER KILLS", "${item.totalKills}", DossierTeal)
            MetricRightColumn("PEAK RESONANCE", "${item.peakBountyAchieved}", DossierGold)
            MetricRightColumn("PUBLIC WINS", "${item.publicGameWins}", Color(0xFF00E676))
          }
        }
      }
    }
  }
}

@Composable
private fun MetricRightColumn(label: String, value: String, valueColor: Color) {
  Column(horizontalAlignment = Alignment.End) {
    Text(
      text = label,
      color = Color(0xFF94A3B8),
      fontSize = 9.5.sp,
      fontWeight = FontWeight.Bold,
      fontFamily = FontFamily.Monospace,
    )
    Text(
      text = value,
      color = valueColor,
      fontSize = 11.8.sp,
      fontWeight = FontWeight.ExtraBold,
      fontFamily = FontFamily.Monospace,
    )
  }
}

@Composable
private fun DossierSectionButton(
  label: String,
  selected: Boolean,
  accentColor: Color,
  testTag: String,
  onClick: () -> Unit,
) {
  val isCompactPhone = LocalConfiguration.current.screenHeightDp < 480
  Box(
    modifier =
      Modifier.shadow(2.dp, RoundedCornerShape(8.dp))
        .clip(RoundedCornerShape(8.dp))
        .background(if (selected) accentColor.copy(alpha = 0.22f) else DossierCardBg)
        .border(1.2.dp, if (selected) accentColor else accentColor.copy(alpha = 0.50f), RoundedCornerShape(8.dp))
        .clickable { onClick() }
        .padding(
          horizontal = if (isCompactPhone) 9.dp else 11.dp,
          vertical = if (isCompactPhone) 4.5.dp else 6.dp,
        )
        .testTag(testTag)
  ) {
    Text(
      text = label,
      color = if (selected) Color.White else accentColor,
      fontSize = if (isCompactPhone) 9.8.sp else 10.5.sp,
      fontWeight = FontWeight.ExtraBold,
      fontFamily = FontFamily.Monospace,
      maxLines = 1,
    )
  }
}

@Composable
private fun DossierStatBox(
  modifier: Modifier = Modifier,
  label: String,
  value: String,
  color: Color,
  isCompactPhone: Boolean = false,
  showFluxEmblem: Boolean = false,
) {
  Column(
    modifier =
      modifier
        .background(DossierCardBg, RoundedCornerShape(8.dp))
        .border(1.dp, color.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
        .padding(
          horizontal = if (isCompactPhone) 7.dp else 9.dp,
          vertical = if (isCompactPhone) 4.dp else 5.5.dp,
        )
  ) {
    Text(
      text = label,
      color = Color(0xFF94A3B8),
      fontSize = if (isCompactPhone) 9.2.sp else 9.8.sp,
      fontWeight = FontWeight.Bold,
      fontFamily = FontFamily.Monospace,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
      if (showFluxEmblem) {
        FluxEmblem(sizeDp = 13.dp)
      }
      Text(
        text = value,
        color = color,
        fontSize = if (isCompactPhone) 11.sp else 12.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
private fun DossierTabChip(label: String, selected: Boolean, onClick: () -> Unit) {
  val isCompactPhone = LocalConfiguration.current.screenHeightDp < 480
  Box(
    modifier =
      Modifier.shadow(2.dp, RoundedCornerShape(8.dp))
        .clip(RoundedCornerShape(8.dp))
        .background(if (selected) DossierTeal.copy(alpha = 0.22f) else DossierCardBg)
        .border(1.2.dp, if (selected) DossierTeal else DossierCyan.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
        .clickable { onClick() }
        .padding(
          horizontal = if (isCompactPhone) 8.dp else 9.dp,
          vertical = if (isCompactPhone) 4.dp else 5.dp,
        )
  ) {
    Text(
      text = label,
      color = if (selected) Color.White else DossierTeal,
      fontSize = if (isCompactPhone) 9.5.sp else 9.8.sp,
      fontWeight = FontWeight.Bold,
      fontFamily = FontFamily.Monospace,
      maxLines = 1,
    )
  }
}

fun copySurvivalPayloadToClipboard(context: Context, label: String, payload: String) {
  runCatching {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    cm?.setPrimaryClip(ClipData.newPlainText(label, payload))
  }
}

fun shareOrOpenSurvivalExport(
  context: Context,
  fileName: String,
  mimeType: String,
  payload: String,
  preferBrowserView: Boolean = false,
): String {
  copySurvivalPayloadToClipboard(context, fileName, payload)
  val writtenFile =
    runCatching {
      val f = File(context.cacheDir, fileName)
      f.writeText(payload, Charsets.UTF_8)
      f
    }.getOrNull()

  if (preferBrowserView) {
    val openedBrowser =
      runCatching {
        val b64 = Base64.encodeToString(payload.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
        val viewIntent =
          Intent(Intent.ACTION_VIEW, Uri.parse("data:text/html;charset=utf-8;base64,$b64")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
          }
        context.startActivity(viewIntent)
        true
      }.getOrDefault(false)
    if (openedBrowser) {
      return "OPENED HTML SPREADSHEET IN BROWSER (AND COPIED TO CLIPBOARD)"
    }
  }

  runCatching {
    val shareIntent =
      Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_SUBJECT, fileName)
        putExtra(Intent.EXTRA_TEXT, payload)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
    val chooser =
      Intent.createChooser(shareIntent, "Export $fileName").apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
    context.startActivity(chooser)
  }
  val pathHint = writtenFile?.name ?: fileName
  return "COPIED $pathHint TO CLIPBOARD & READY TO SHARE"
}

@Composable
private fun ShipSurvivalTelemetrySection(
  modifier: Modifier = Modifier,
  summary: PilotSurvivalTelemetrySummary,
  localPrefs: LocalPilotPrefs,
  uidOrNull: String?,
  callsign: String,
  liveResonance: Int,
  statusBanner: String?,
  onUpdateStatusBanner: (String) -> Unit,
  onOpenHtmlSpreadsheetModal: () -> Unit,
) {
  val context = LocalContext.current
  val isCompactPhone = LocalConfiguration.current.screenHeightDp < 480
  val dateFormatter = remember { SimpleDateFormat("HH:mm:ss", Locale.US) }

  Column(
    modifier =
      modifier
        .background(DossierCardBg, RoundedCornerShape(10.dp))
        .border(1.dp, DossierCyan.copy(alpha = 0.55f), RoundedCornerShape(10.dp))
        .padding(if (isCompactPhone) 7.dp else 10.dp)
        .testTag("dossier_survival_telemetry_section"),
    verticalArrangement = Arrangement.spacedBy(6.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
        Text(
          text = "SHIP ALIVE DURATION & TELEMETRY EXPORT",
          color = DossierTeal,
          fontSize = if (isCompactPhone) 10.8.sp else 12.sp,
          fontWeight = FontWeight.Black,
          fontFamily = FontFamily.Monospace,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text =
            "Tracks seconds alive from spawn or respawn until elimination, round end, match end, ship switch, or Rift Out.",
          color = Color(0xFF94A3B8),
          fontSize = if (isCompactPhone) 8.6.sp else 9.4.sp,
          fontFamily = FontFamily.Monospace,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }

      Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        SurvivalExportActionButton(
          label = "HTML SPREADSHEET",
          accent = DossierTeal,
          testTag = "dossier_open_html_telemetry_btn",
        ) {
          val html = localPrefs.exportSurvivalTelemetryHtmlSpreadsheet(callsign, liveResonance, uidOrNull)
          runCatching { File(context.cacheDir, "rift_ship_survival_telemetry.html").writeText(html, Charsets.UTF_8) }
          onOpenHtmlSpreadsheetModal()
        }
        SurvivalExportActionButton(
          label = "EXPORT CSV",
          accent = DossierGold,
          testTag = "dossier_export_csv_btn",
        ) {
          val csv = localPrefs.exportSurvivalTelemetryCsv(callsign, liveResonance, uidOrNull)
          onUpdateStatusBanner(
            shareOrOpenSurvivalExport(
              context = context,
              fileName = "rift_ship_survival_telemetry.csv",
              mimeType = "text/csv",
              payload = csv,
            )
          )
        }
        SurvivalExportActionButton(
          label = "EXPORT JSON",
          accent = DossierCyan,
          testTag = "dossier_export_json_btn",
        ) {
          val json = localPrefs.exportSurvivalTelemetryJson(callsign, liveResonance, uidOrNull)
          onUpdateStatusBanner(
            shareOrOpenSurvivalExport(
              context = context,
              fileName = "rift_ship_survival_telemetry.json",
              mimeType = "application/json",
              payload = json,
            )
          )
        }
      }
    }

    if (!statusBanner.isNullOrBlank()) {
      Text(
        text = statusBanner,
        color = DossierGold,
        fontSize = 9.5.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace,
        modifier = Modifier.testTag("dossier_telemetry_export_status"),
      )
    }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
      DossierStatBox(
        Modifier.weight(1f),
        "PILOT AVG ALIVE",
        formatSurvivalDurationSec(summary.pilotAvgSurvivalSec),
        DossierTeal,
        isCompactPhone,
      )
      DossierStatBox(
        Modifier.weight(1f),
        "LAST LIFE ALIVE",
        formatSurvivalDurationSec(summary.pilotLastSurvivalSec),
        DossierCyan,
        isCompactPhone,
      )
      DossierStatBox(
        Modifier.weight(1f),
        "LONGEST LIFE",
        formatSurvivalDurationSec(summary.pilotMaxSurvivalSec),
        DossierGold,
        isCompactPhone,
      )
      DossierStatBox(
        Modifier.weight(1f),
        "COMPLETED LIVES",
        "${summary.pilotCompletedLives}",
        Color(0xFF00E676),
        isCompactPhone,
      )
      DossierStatBox(
        Modifier.weight(1f),
        "AI BOT AVG ALIVE",
        "${formatSurvivalDurationSec(summary.botAvgSurvivalSec)} (${summary.botCompletedLives})",
        Color(0xFFC084FC),
        isCompactPhone,
      )
    }

    LazyColumn(
      modifier =
        Modifier.weight(1f)
          .fillMaxWidth()
          .background(DossierTabletBg, RoundedCornerShape(8.dp))
          .border(1.dp, DossierCyan.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
          .padding(6.dp)
          .testTag("dossier_survival_per_ship_list"),
      verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      itemsIndexed(summary.perShip, key = { _, item -> "ship_surv_${item.shipId}" }) { _, entry ->
        val shipRes = localPrefs.getShipResonance(entry.shipId, uidOrNull)
        Row(
          modifier =
            Modifier.fillMaxWidth()
              .background(DossierRowBg, RoundedCornerShape(7.dp))
              .border(
                1.dp,
                if (entry.completedLives > 0) DossierTeal.copy(alpha = 0.45f) else DossierCyan.copy(alpha = 0.22f),
                RoundedCornerShape(7.dp),
              )
              .padding(horizontal = 10.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1.2f),
          ) {
            ShipSpritePreview(shipId = entry.shipId.coerceIn(1, 10), sizeDp = 26)
            Column {
              Text(
                text = entry.shipName,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
              )
              Text(
                text = "RESONANCE: $shipRes • PILOT LIVES: ${entry.completedLives}",
                color = Color(0xFF94A3B8),
                fontSize = 9.2.sp,
                fontFamily = FontFamily.Monospace,
              )
            }
          }
          Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            MetricRightColumn("AVG ALIVE", formatSurvivalDurationSec(entry.avgSurvivalSec), DossierTeal)
            MetricRightColumn("LAST", formatSurvivalDurationSec(entry.lastSurvivalSec), DossierCyan)
            MetricRightColumn("MAX", formatSurvivalDurationSec(entry.maxSurvivalSec), DossierGold)
            MetricRightColumn(
              "BOT AVG",
              if (entry.botCompletedLives > 0)
                "${formatSurvivalDurationSec(entry.botAvgSurvivalSec)} (${entry.botCompletedLives})"
              else "--",
              Color(0xFFC084FC),
            )
          }
        }
      }

      if (summary.recentLives.isNotEmpty()) {
        item(key = "recent_lives_header") {
          Text(
            text = "RECENT COMPLETED LIVES (${summary.recentLives.size})",
            color = DossierGold,
            fontSize = 9.8.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(top = 6.dp, bottom = 2.dp),
          )
        }
        itemsIndexed(
          summary.recentLives.take(16),
          key = { idx, rec -> "recent_life_${rec.timestampMs}_$idx" },
        ) { _, rec ->
          val actorTag =
            when {
              rec.isLocalPilot -> "YOU"
              rec.isHuman -> "PILOT"
              else -> "BOT"
            }
          val customScaleNote =
            if (rec.isCustomGame) " • DMG TAKEN ${rec.damageTakenScalePct}% • FLUX COST ${rec.weaponFluxCostScalePct}%"
            else ""
          Row(
            modifier =
              Modifier.fillMaxWidth()
                .background(Color(0xFF071120), RoundedCornerShape(6.dp))
                .border(0.8.dp, Color(0xFF1E3A5F), RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp, vertical = 4.5.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text =
                "${dateFormatter.format(Date(rec.timestampMs))} [$actorTag] ${rec.callsign} • ${rec.shipName} • ${rec.modeId}$customScaleNote",
              color = Color(0xFFCBD5E1),
              fontSize = 9.0.sp,
              fontFamily = FontFamily.Monospace,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.weight(1f).padding(end = 8.dp),
            )
            Text(
              text = "${formatSurvivalDurationSec(rec.aliveDurationSec)} (${rec.endReason})",
              color = if (rec.isLocalPilot) DossierTeal else Color(0xFFC084FC),
              fontSize = 9.2.sp,
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
private fun SurvivalExportActionButton(
  label: String,
  accent: Color,
  testTag: String,
  onClick: () -> Unit,
) {
  Box(
    modifier =
      Modifier.minimumInteractiveComponentSize()
        .heightIn(min = 48.dp)
        .clickable { onClick() }
        .padding(vertical = 5.dp)
        .testTag(testTag),
    contentAlignment = Alignment.Center,
  ) {
    Box(
      modifier =
        Modifier.clip(RoundedCornerShape(7.dp))
          .background(accent.copy(alpha = 0.16f))
          .border(1.1.dp, accent, RoundedCornerShape(7.dp))
          .padding(horizontal = 10.dp, vertical = 6.dp),
      contentAlignment = Alignment.Center,
    ) {
      Text(
        text = label,
        color = Color.White,
        fontSize = 9.2.sp,
        fontWeight = FontWeight.Black,
        fontFamily = FontFamily.Monospace,
        maxLines = 1,
      )
    }
  }
}

@Composable
fun SurvivalTelemetrySpreadsheetModal(
  callsign: String,
  liveResonance: Int,
  uidOrNull: String?,
  localPrefs: LocalPilotPrefs,
  onClose: () -> Unit,
) {
  val context = LocalContext.current
  var exportBanner by remember { mutableStateOf<String?>(null) }
  val htmlSpreadsheet =
    remember(callsign, liveResonance, uidOrNull) {
      localPrefs.exportSurvivalTelemetryHtmlSpreadsheet(callsign, liveResonance, uidOrNull)
    }

  Box(
    modifier =
      Modifier.fillMaxSize()
        .background(Color(0xEB02050B))
        .clickable(onClick = onClose)
        .padding(10.dp)
        .testTag("dossier_html_spreadsheet_modal"),
    contentAlignment = Alignment.Center,
  ) {
    Surface(
      modifier =
        Modifier.fillMaxSize()
          .clickable(enabled = false) {}
          .border(
            1.4.dp,
            Brush.linearGradient(listOf(DossierTeal, DossierCyan, DossierGold)),
            RoundedCornerShape(12.dp),
          ),
      color = DossierTabletBg,
      shape = RoundedCornerShape(12.dp),
    ) {
      Column(
        modifier = Modifier.fillMaxSize().padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text(
              text = "HTML TELEMETRY SPREADSHEET // SHIP SURVIVAL & COMBAT LOG",
              color = DossierTeal,
              fontSize = 11.5.sp,
              fontWeight = FontWeight.Black,
              fontFamily = FontFamily.Monospace,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            if (!exportBanner.isNullOrBlank()) {
              Text(
                text = exportBanner!!,
                color = DossierGold,
                fontSize = 9.2.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }

          Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            SurvivalExportActionButton(
              label = "OPEN BROWSER / SHARE",
              accent = DossierTeal,
              testTag = "survival_modal_browser_btn",
            ) {
              exportBanner =
                shareOrOpenSurvivalExport(
                  context = context,
                  fileName = "rift_ship_survival_telemetry.html",
                  mimeType = "text/html",
                  payload = htmlSpreadsheet,
                  preferBrowserView = true,
                )
            }
            SurvivalExportActionButton(
              label = "COPY CSV",
              accent = DossierGold,
              testTag = "survival_modal_copy_csv_btn",
            ) {
              val csv = localPrefs.exportSurvivalTelemetryCsv(callsign, liveResonance, uidOrNull)
              copySurvivalPayloadToClipboard(context, "rift_ship_survival_telemetry.csv", csv)
              exportBanner = "CSV TELEMETRY COPIED TO CLIPBOARD (${csv.lines().size} LINES)"
            }
            SurvivalExportActionButton(
              label = "COPY JSON",
              accent = DossierCyan,
              testTag = "survival_modal_copy_json_btn",
            ) {
              val json = localPrefs.exportSurvivalTelemetryJson(callsign, liveResonance, uidOrNull)
              copySurvivalPayloadToClipboard(context, "rift_ship_survival_telemetry.json", json)
              exportBanner = "JSON TELEMETRY COPIED TO CLIPBOARD"
            }
            SurvivalExportActionButton(
              label = "CLOSE",
              accent = Color(0xFFFB7185),
              testTag = "survival_modal_close_btn",
            ) {
              onClose()
            }
          }
        }

        Box(
          modifier =
            Modifier.weight(1f)
              .fillMaxWidth()
              .clip(RoundedCornerShape(8.dp))
              .background(Color(0xFF02050B))
              .border(1.dp, DossierCyan.copy(alpha = 0.45f), RoundedCornerShape(8.dp)),
        ) {
          AndroidView(
            modifier = Modifier.fillMaxSize().testTag("survival_html_webview"),
            factory = { ctx ->
              WebView(ctx).apply {
                setBackgroundColor(android.graphics.Color.parseColor("#02050B"))
                settings.javaScriptEnabled = true
                settings.allowFileAccess = false
                settings.allowContentAccess = false
                settings.builtInZoomControls = true
                settings.displayZoomControls = false
                settings.useWideViewPort = true
                settings.loadWithOverviewMode = true
              }
            },
            update = { webView ->
              webView.loadDataWithBaseURL(
                null,
                htmlSpreadsheet,
                "text/html",
                "UTF-8",
                null,
              )
            },
          )
        }
      }
    }
  }
}

@Composable
private fun PilotCareerAndXpSection(
  modifier: Modifier = Modifier,
  career: PilotCareerStatsSummary,
  isCompactPhone: Boolean = false,
) {
  Column(
    modifier =
      modifier
        .verticalScroll(rememberScrollState())
        .background(DossierCardBg, RoundedCornerShape(10.dp))
        .border(1.dp, Color(0xFFC084FC).copy(alpha = 0.58f), RoundedCornerShape(10.dp))
        .padding(if (isCompactPhone) 8.dp else 10.dp)
        .testTag("dossier_career_xp_section"),
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    Column(
      modifier =
        Modifier.fillMaxWidth()
          .background(DossierTabletBg, RoundedCornerShape(10.dp))
          .border(1.2.dp, Color(0xFFC084FC), RoundedCornerShape(10.dp))
          .padding(horizontal = 12.dp, vertical = 9.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp),
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
          Box(
            modifier =
              Modifier.background(Color(0xFF2E1250), RoundedCornerShape(6.dp))
                .border(1.dp, Color(0xFFC084FC), RoundedCornerShape(6.dp))
                .padding(horizontal = 9.dp, vertical = 3.dp)
          ) {
            Text(
              text = "PILOT LEVEL ${career.pilotLevel}",
              color = Color(0xFFF3E8FF),
              fontSize = if (isCompactPhone) 11.sp else 12.5.sp,
              fontWeight = FontWeight.Black,
              fontFamily = FontFamily.Monospace,
            )
          }
          Text(
            text = "TOTAL CAREER XP: ${career.totalXp}",
            color = DossierTeal,
            fontSize = if (isCompactPhone) 10.5.sp else 11.5.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = FontFamily.Monospace,
          )
        }
        Text(
          text = "${career.xpIntoLevel} / ${career.xpSpanForLevel} XP TO LEVEL ${career.pilotLevel + 1}",
          color = DossierGold,
          fontSize = if (isCompactPhone) 9.8.sp else 10.8.sp,
          fontWeight = FontWeight.ExtraBold,
          fontFamily = FontFamily.Monospace,
        )
      }
      Box(
        modifier =
          Modifier.fillMaxWidth()
            .height(7.dp)
            .clip(RoundedCornerShape(3.5.dp))
            .background(Color(0xFF132642))
      ) {
        Box(
          modifier =
            Modifier.fillMaxWidth(career.levelProgressFraction.coerceIn(0f, 1f))
              .fillMaxHeight()
              .background(
                Brush.horizontalGradient(listOf(DossierTeal, DossierCyan, Color(0xFFC084FC))),
                RoundedCornerShape(3.5.dp),
              )
        )
      }
    }

    Text(
      text = "5-CHANNEL CAREER XP BREAKDOWN",
      color = DossierCyan,
      fontSize = if (isCompactPhone) 10.2.sp else 11.2.sp,
      fontWeight = FontWeight.Black,
      fontFamily = FontFamily.Monospace,
    )
    val xpSources =
      listOf(
        Triple("COMBAT XP", career.combatXp, DossierTeal to "Eliminations & Leader shutdowns"),
        Triple("SURVIVAL XP", career.survivalXp, DossierCyan to "Active arena survival endurance"),
        Triple("OBJECTIVE XP", career.objectiveXp, DossierGold to "Rift captures, Flux Core & wins"),
        Triple("SUPPORT & CRATES", career.supportCratesXp, Color(0xFF34D399) to "Assists, Brig rescues & Crates"),
        Triple("DAILY DIRECTIVES", career.dailyDirectivesXp, Color(0xFFC084FC) to "Completed Conduit Directives"),
      )
    Row(
      modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      val safeTotalXp = career.totalXp.coerceAtLeast(1L)
      xpSources.forEach { (label, xpVal, accentAndDesc) ->
        val (accent, desc) = accentAndDesc
        val sharePct = ((xpVal * 100L) / safeTotalXp).toInt().coerceIn(0, 100)
        Column(
          modifier =
            Modifier.widthIn(min = 148.dp, max = 196.dp)
              .background(DossierTabletBg, RoundedCornerShape(8.dp))
              .border(1.dp, accent.copy(alpha = 0.65f), RoundedCornerShape(8.dp))
              .padding(horizontal = 9.dp, vertical = 7.dp),
          verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = label,
              color = accent,
              fontSize = 9.4.sp,
              fontWeight = FontWeight.Black,
              fontFamily = FontFamily.Monospace,
              maxLines = 1,
            )
            Text(
              text = "$sharePct%",
              color = Color(0xFF94A3B8),
              fontSize = 9.0.sp,
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.Monospace,
            )
          }
          Text(
            text = "$xpVal XP",
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = FontFamily.Monospace,
          )
          Text(
            text = desc,
            color = Color(0xFFCBD5E1),
            fontSize = 9.2.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }
    }

    Text(
      text = "LIFETIME SORTIE & COMBAT TELEMETRY",
      color = DossierGold,
      fontSize = if (isCompactPhone) 10.2.sp else 11.2.sp,
      fontWeight = FontWeight.Black,
      fontFamily = FontFamily.Monospace,
    )
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
      DossierStatBox(
        Modifier.weight(1f),
        "MATCHES PLAYED",
        "${career.matchesPlayed}",
        DossierCyan,
        isCompactPhone,
      )
      DossierStatBox(
        Modifier.weight(1f),
        "WINS / LOSSES (WIN%)",
        "${career.wins}W - ${career.losses}L (${career.winPercent}%)",
        Color(0xFF00E676),
        isCompactPhone,
      )
      DossierStatBox(
        Modifier.weight(1f),
        "KILLS / DEATHS / ASSISTS",
        "${career.eliminations} / ${career.deaths} / ${career.assists}",
        DossierTeal,
        isCompactPhone,
      )
    }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
      DossierStatBox(
        Modifier.weight(1f),
        "SHOTS HIT / FIRED (ACC%)",
        "${career.shotsHit} / ${career.shotsFired} (${career.accuracyPercent}%)",
        Color(0xFFC084FC),
        isCompactPhone,
      )
      DossierStatBox(
        Modifier.weight(1f),
        "SUPPLY CRATES LOOTED",
        "${career.cratePickups}",
        Color(0xFF34D399),
        isCompactPhone,
      )
      DossierStatBox(
        Modifier.weight(1f),
        "RIFT & BRIG CAPTURES",
        "${career.riftCaptures}",
        DossierGold,
        isCompactPhone,
      )
    }
  }
}
