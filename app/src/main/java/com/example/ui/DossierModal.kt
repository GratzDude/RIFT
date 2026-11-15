package com.example.ui

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import com.example.data.LeaderboardEntry
import com.example.data.LocalPilotPrefs
import com.example.data.PeakBountyRunSnapshot
import com.example.data.SyndicateInfo
import com.example.data.SyndicateServiceEntry
import com.example.data.SyndicateWarRecord
import com.example.engine.RiftCodex
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
  customizationPointsTotal: Int = 1,
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
) {
  BackHandler { onCloseModal() }
  val isCompactPhone = LocalConfiguration.current.screenHeightDp < 480
  val context = LocalContext.current
  val localPrefs = remember(context) { LocalPilotPrefs(context) }
  val isDrifter = remember(permanentUid, syndicateTag) { localPrefs.isGuestBypassEnabled() }
  val shortTag = remember(permanentUid) { localPrefs.getShortPilotTag(permanentUid) }

  var showCallsignEditor by remember { mutableStateOf(false) }
  var newCallsignInput by remember(callsign) { mutableStateOf(callsign) }
  var callsignStatusMsg by remember { mutableStateOf<String?>(null) }
  var callsignChangeCount by remember(permanentUid) {
    mutableIntStateOf(localPrefs.getCallsignChangeCount(permanentUid))
  }

  // Top-level Dossier view: 0 = LEADERBOARD, 1 = PEAK BOUNTY & EXECUTIONER, 2 = SYNDICATE SERVICE RECORD
  var dossierSection by remember { mutableIntStateOf(0) }
  var activeTab by remember { mutableIntStateOf(1) } // 0 = PVP ONLY, 1 = ALL-TIME
  var selectedArmadaTier by remember { mutableIntStateOf(0) } // 0 = ALL, 1 = SOLO, 2..5 = 2..5-MAN ARMADA
  var sortMode by remember { mutableIntStateOf(0) } // 0 = BOUNTY, 1 = PUBLIC WINS, 2 = K/D

  // Deduplicate strictly by permanent userId and enforce 100% Real-Player-Only Sworn Pilot Leaderboards
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
          if (syndicateTag.isNotBlank()) {
            "[$localArmadaBadge] [$syndicateTag] $callsign"
          } else {
            "[$localArmadaBadge] $callsign"
          }
        val localEntry =
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
            pvpBounty = pvpBountyPeak,
            allTimeKills = allTimeKills,
            allTimeDeaths = allTimeDeaths,
            allTimeBounty = allTimeBountyPeak,
            convergencesWon = convergencesWon,
          )
        byUid[permanentUid] = localEntry
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
            if (entry.callsign.startsWith("[")) {
              entry.callsign
            } else {
              val armBadge = entry.armadaTag.ifBlank { "SOLO" }
              if (entry.syndicateTag.isNotBlank()) {
                "[$armBadge] [${entry.syndicateTag}] ${entry.callsign}"
              } else {
                "[$armBadge] ${entry.callsign}"
              }
            }
          byUid[uidKey] = entry.copy(callsign = cleanCallsign)
        }
      }

      byUid.values
        .filter { entry ->
          !entry.syndicateTag.equals("RFT", ignoreCase = true) &&
            !entry.syndicateTag.equals("BOT", ignoreCase = true) &&
            !entry.callsign.startsWith("DRIFTER", ignoreCase = true) &&
            !entry.callsign.contains("[RFT]", ignoreCase = true) &&
            !entry.callsign.contains("[BOT]", ignoreCase = true)
        }
        .filter { entry ->
          val isSelf = entry.userId == permanentUid || entry.id == permanentUid
          isSelf || selectedArmadaTier == 0 || entry.armadaTierSize.toInt() == selectedArmadaTier
        }
        .sortedByDescending { entry ->
          val k = if (activeTab == 0) entry.pvpKills else entry.allTimeKills
          val d = (if (activeTab == 0) entry.pvpDeaths else entry.allTimeDeaths).coerceAtLeast(1L)
          val bty = if (activeTab == 0) entry.pvpBounty else entry.allTimeBounty
          when (sortMode) {
            1 -> entry.convergencesWon * 100_000L + bty
            2 -> ((k.toDouble() / d.toDouble()) * 10_000.0).toLong() + k * 100L
            else -> bty + k * 25L
          }
        }
    }

  val availableCustomPts = (customizationPointsTotal - spentCustomizationPoints).coerceAtLeast(0)
  val displayedPeakSnapshot =
    if (activeTab == 0) {
      pvpPeakSnapshot
    } else {
      allTimePeakSnapshot ?: currentLifeSnapshot
    }

  Column(
    modifier =
      Modifier.fillMaxSize()
        .padding(8.dp)
        .testTag("dossier_modal")
  ) {
    // 1. Unified Compact Embedded Header Row
    val tierDesc =
      if (activeArmadaName.isEmpty() || activeArmadaSize <= 1) {
        "[SOLO]"
      } else {
        "${activeArmadaSize}P [$activeArmadaName]"
      }
    val syndicatePrefix = if (syndicateTag.isNotBlank()) "[$syndicateTag] " else ""
    Row(
      modifier = Modifier.fillMaxWidth().height(if (isCompactPhone) 28.dp else 34.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Row(
        modifier = Modifier.weight(1f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Text(
          text = if (isCompactPhone) "📋 PILOT DOSSIER" else "📋 PILOT DOSSIER, HALL OF FAME & SERVICE RECORD",
          color = Color.White,
          fontSize = if (isCompactPhone) 12.5.sp else 15.sp,
          fontWeight = FontWeight.Black,
          fontFamily = FontFamily.SansSerif,
          letterSpacing = 0.8.sp,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Box(
          modifier =
            Modifier.clip(RoundedCornerShape(6.dp))
              .background(Color(0xFF0A192F))
              .border(1.dp, Color(0xFF38BDF8), RoundedCornerShape(6.dp))
              .padding(horizontal = 6.dp, vertical = 2.dp),
        ) {
          Text(
            text = "$shortTag • ${syndicatePrefix}$callsign • $tierDesc",
            color = Color(0xFFFFD700),
            fontSize = if (isCompactPhone) 8.5.sp else 9.5.sp,
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
            onClick = onResetLocalPilotData,
            colors =
              ButtonDefaults.buttonColors(
                containerColor = Color(0xFF1E293B),
                contentColor = Color(0xFFF59E0B),
              ),
            border = BorderStroke(1.2.dp, Color(0xFFF59E0B)),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
            modifier =
              Modifier.height(if (isCompactPhone) 28.dp else 32.dp)
                .shadow(2.dp, RoundedCornerShape(8.dp))
                .testTag("dossier_reset_pilot_button"),
          ) {
            Text(
              text = "↺ RESET PILOT",
              fontSize = 9.sp,
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.SansSerif,
            )
          }
        }

        Row(
          modifier =
            Modifier.height(if (isCompactPhone) 28.dp else 32.dp)
              .shadow(2.dp, RoundedCornerShape(8.dp))
              .clip(RoundedCornerShape(8.dp))
              .background(Color(0xFF1A1405))
              .border(1.2.dp, Color(0xFFFFD700), RoundedCornerShape(8.dp))
              .clickable { onOpenRiftShardsStore() }
              .padding(horizontal = 8.dp)
              .testTag("dossier_btn_get_rift_shards"),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
          Text(
            text = "❖ $riftShards",
            color = Color(0xFFFFD700),
            fontSize = if (isCompactPhone) 10.sp else 11.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = FontFamily.Monospace,
          )
          Box(
            modifier =
              Modifier.background(Color(0xFFFFD700), RoundedCornerShape(4.dp))
                .padding(horizontal = 5.dp, vertical = 1.5.dp),
          ) {
            Text(
              text = if (isCompactPhone) "PROGRESSION" else "PROGRESSION FOUNDRY",
              color = Color(0xFF05080E),
              fontSize = 8.5.sp,
              fontWeight = FontWeight.ExtraBold,
              fontFamily = FontFamily.SansSerif,
            )
          }
        }
      }
    }

        Spacer(modifier = Modifier.height(if (isCompactPhone) 3.dp else 5.dp))

        // 1B. Sworn Pilot vs [RFT] Drifter Callsign Management Banner
        if (isDrifter) {
          Row(
            modifier =
              Modifier.fillMaxWidth()
                .background(Color(0xCC1E1408), RoundedCornerShape(8.dp))
                .border(1.dp, Color(0xFFF59E0B), RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp, vertical = if (isCompactPhone) 3.dp else 5.dp)
                .testTag("dossier_drifter_banner"),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = "DRIFTER SESSION ([RFT] GUEST) — Sign in with Google to become a Sworn Pilot, customize your callsign, and rank on Leaderboards.",
              color = Color(0xFFFBBF24),
              fontSize = if (isCompactPhone) 8.2.sp else 9.sp,
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
                .background(Color(0xCC0A192F), RoundedCornerShape(8.dp))
                .border(1.dp, Color(0xFF00F5D4), RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp, vertical = if (isCompactPhone) 3.dp else 5.dp)
                .testTag("dossier_sworn_pilot_banner"),
            verticalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text =
                  if (isCompactPhone) {
                    "★ SWORN PILOT • ID: $shortTag • LVL $pilotLevel ($availableCustomPts/$customizationPointsTotal PTS AVAIL)"
                  } else {
                    "★ SWORN PILOT — Cross-Device Synced • PILOT ID: $shortTag"
                  },
                color = Color(0xFF00F5D4),
                fontSize = if (isCompactPhone) 8.5.sp else 9.2.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              val changeLabel =
                if (callsignChangeCount == 0) "✏️ CHANGE CALLSIGN (1ST FREE)"
                else "✏️ CHANGE CALLSIGN (250❖)"
              Box(
                modifier =
                  Modifier.shadow(2.dp, RoundedCornerShape(8.dp))
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF0E3A5A))
                    .border(1.2.dp, Color(0xFF38BDF8), RoundedCornerShape(8.dp))
                    .clickable { showCallsignEditor = !showCallsignEditor }
                    .padding(horizontal = 8.dp, vertical = 3.dp)
                    .testTag("dossier_btn_change_callsign"),
              ) {
                Text(
                  text = changeLabel,
                  color = Color.White,
                  fontSize = 8.sp,
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
                      .height(if (isCompactPhone) 34.dp else 38.dp)
                      .clip(RoundedCornerShape(8.dp))
                      .background(Color(0xFF050B14))
                      .border(1.dp, Color(0xFF00F5D4), RoundedCornerShape(8.dp))
                      .padding(horizontal = 10.dp),
                  contentAlignment = Alignment.CenterStart,
                ) {
                  BasicTextField(
                    value = newCallsignInput,
                    onValueChange = { raw ->
                      newCallsignInput =
                        raw.uppercase().filter { it.isLetterOrDigit() || it == '_' || it == '-' }.take(14)
                    },
                    singleLine = true,
                    cursorBrush = SolidColor(Color(0xFF00F5D4)),
                    textStyle =
                      TextStyle(
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                      ),
                    modifier = Modifier.fillMaxWidth().testTag("dossier_callsign_input"),
                  )
                }
                Button(
                  onClick = {
                    val (success, updatedCallsign, remainingShards) =
                      localPrefs.changeSwornPilotCallsign(newCallsignInput, riftShards, permanentUid)
                    if (success) {
                      callsignChangeCount = localPrefs.getCallsignChangeCount(permanentUid)
                      onCallsignChanged(updatedCallsign)
                      onUpdateRiftShards(remainingShards)
                      callsignStatusMsg = "✓ Callsign updated to $updatedCallsign"
                      showCallsignEditor = false
                    } else {
                      callsignStatusMsg = "✕ Requires 3–14 chars & 250❖ after 1st free change"
                    }
                  },
                  border = BorderStroke(1.2.dp, Color(0xFF00F5D4)),
                  shape = RoundedCornerShape(8.dp),
                  contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                  modifier =
                    Modifier.height(if (isCompactPhone) 34.dp else 38.dp)
                      .shadow(2.dp, RoundedCornerShape(8.dp))
                      .testTag("dossier_btn_confirm_callsign"),
                ) {
                  Text(
                    text = if (callsignChangeCount == 0) "APPLY (FREE)" else "APPLY (250❖)",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                  )
                }
              }
            }
            if (callsignStatusMsg != null) {
              Text(
                text = callsignStatusMsg!!,
                color = Color(0xFFFACC15),
                fontSize = 8.2.sp,
                fontFamily = FontFamily.Monospace,
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(if (isCompactPhone) 3.dp else 5.dp))

        // 2. Career Level & XP Progress Card
        Column(
          modifier =
            Modifier.fillMaxWidth()
              .background(Color(0xCC0D1B30), RoundedCornerShape(8.dp))
              .border(1.dp, Color(0x6600F0FF), RoundedCornerShape(8.dp))
              .padding(horizontal = 8.dp, vertical = if (isCompactPhone) 3.dp else 5.dp)
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
                  Modifier.background(Color(0xFF00F0FF), RoundedCornerShape(5.dp))
                    .padding(horizontal = 6.dp, vertical = 1.5.dp),
              ) {
                Text(
                  text = "PILOT LVL $pilotLevel / 100",
                  color = Color(0xFF040914),
                  fontSize = if (isCompactPhone) 9.sp else 10.sp,
                  fontWeight = FontWeight.ExtraBold,
                  fontFamily = FontFamily.Monospace,
                )
              }

              val xpNeededForNext = (nextLevelXpSpan - currentLevelXp).coerceAtLeast(0L)
              Text(
                text =
                  if (pilotLevel >= 100) {
                    "MAX LEVEL 100 ACHIEVED"
                  } else {
                    "XP: $currentLevelXp / $nextLevelXpSpan ($xpNeededForNext XP TO LVL ${pilotLevel + 1})"
                  },
                color = Color.White,
                fontSize = if (isCompactPhone) 8.8.sp else 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
              )
            }

            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Text(
                text = "CUSTOMIZATION PTS: $availableCustomPts AVAIL ($customizationPointsTotal / 100)",
                color = Color(0xFF00E676),
                fontSize = if (isCompactPhone) 8.8.sp else 10.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
              )
              if (!isCompactPhone) {
                Text(
                  text = "RIFT SHARDS: ❖ $riftShards",
                  color = Color(0xFFFFD700),
                  fontSize = 10.sp,
                  fontWeight = FontWeight.ExtraBold,
                  fontFamily = FontFamily.Monospace,
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(2.dp))

          Box(
            modifier =
              Modifier.fillMaxWidth()
                .height(if (isCompactPhone) 3.dp else 4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color(0xFF142844))
          ) {
            Box(
              modifier =
                Modifier.fillMaxWidth(levelProgressFraction.coerceIn(0f, 1f))
                  .fillMaxHeight()
                  .background(Color(0xFF00E676), RoundedCornerShape(2.dp))
            )
          }

          if (!isCompactPhone) {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              XpSourceChip(Modifier.weight(1f), "1. BUFFS COLLECTED", xpFromBuffs, Color(0xFF10B981))
              XpSourceChip(Modifier.weight(1f), "2. DAMAGE DONE", xpFromDamage, Color(0xFF38BDF8))
              XpSourceChip(Modifier.weight(1f), "3. ACCURACY", xpFromAccuracy, Color(0xFFF59E0B))
              XpSourceChip(Modifier.weight(1f), "4. BOUNTY EARNED", xpFromBountyEarned, Color(0xFFFFD700))
              XpSourceChip(Modifier.weight(1f), "5. BOUNTY KILLED", xpFromBountyKilled, Color(0xFFF87171))
            }
          }
        }

        Spacer(modifier = Modifier.height(if (isCompactPhone) 3.dp else 5.dp))

        // 3. Primary Section Navigation Sub-Tabs
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            DossierSectionButton(
              label = if (isCompactPhone) "🏆 LEADERBOARDS" else "🏆 ARMADA LEADERBOARDS",
              selected = dossierSection == 0,
              accentColor = Color(0xFF00F0FF),
              testTag = "dossier_tab_leaderboards",
              onClick = { dossierSection = 0 },
            )
            DossierSectionButton(
              label = if (isCompactPhone) "💀 PEAK BOUNTY RUN" else "💀 PEAK BOUNTY & EXECUTIONER",
              selected = dossierSection == 1,
              accentColor = Color(0xFFFFD700),
              testTag = "dossier_tab_peak_snapshot",
              onClick = { dossierSection = 1 },
            )
            DossierSectionButton(
              label =
                if (isCompactPhone) "🏛️ SERVICE (${syndicateServiceHistory.size})"
                else "🏛️ SYNDICATE SERVICE RECORD (${syndicateServiceHistory.size})",
              selected = dossierSection == 2,
              accentColor = Color(0xFF34D399),
              testTag = "dossier_tab_syndicate_history",
              onClick = { dossierSection = 2 },
            )
          }

          // Shared All-Time vs PvP Only Filter for Leaderboards & Peak Bounty Snapshot
          if (dossierSection != 2) {
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
              DossierTabChip(
                label = "🌐 ALL-TIME",
                selected = activeTab == 1,
                onClick = { activeTab = 1 },
              )
              DossierTabChip(
                label = "⚔️ PVP ONLY",
                selected = activeTab == 0,
                onClick = { activeTab = 0 },
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(if (isCompactPhone) 3.dp else 5.dp))

        when (dossierSection) {
          0 -> {
            // Summary KPI Cards + Compact Clickable Hall of Fame Peak Run Banner
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
              DossierStatBox(
                modifier = Modifier.weight(1f),
                label = if (isCompactPhone) "ALL-TIME K/D" else "ALL-TIME KILLS / DEATHS",
                value = "$allTimeKills K / $allTimeDeaths D",
                color = Color(0xFF00F0FF),
                isCompactPhone = isCompactPhone,
              )
              DossierStatBox(
                modifier = Modifier.weight(1f),
                label = if (isCompactPhone) "PVP K/D" else "PVP KILLS / DEATHS",
                value = "$pvpKills K / $pvpDeaths D",
                color = Color(0xFFFFD700),
                isCompactPhone = isCompactPhone,
              )
              DossierStatBox(
                modifier = Modifier.weight(1f).clickable { dossierSection = 1 },
                label = if (isCompactPhone) "PEAK BOUNTY ↗" else "PEAK BOUNTY (TAP FOR RUN)",
                value = "$allTimeBountyPeak BTY ↗",
                color = Color(0xFFFFD700),
                isCompactPhone = isCompactPhone,
              )
              DossierStatBox(
                modifier = Modifier.weight(1f),
                label = if (isCompactPhone) "PUBLIC WINS" else "PUBLIC GAME WINS",
                value = "🏆 $convergencesWon WINS",
                color = Color(0xFF00E676),
                isCompactPhone = isCompactPhone,
              )
              DossierStatBox(
                modifier = Modifier.weight(1f).clickable { dossierSection = 2 },
                label = "SYNDICATE TENURE",
                value = "[$syndicateTag] ↗",
                color = Color(0xFF34D399),
                isCompactPhone = isCompactPhone,
              )
            }

            Spacer(modifier = Modifier.height(if (isCompactPhone) 3.dp else 5.dp))

            // Sort + Armada Tier Filter Row
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                DossierTabChip("SORT: BOUNTY", sortMode == 0) { sortMode = 0 }
                DossierTabChip("SORT: PUBLIC WINS", sortMode == 1) { sortMode = 1 }
                DossierTabChip("SORT: K/D", sortMode == 2) { sortMode = 2 }
              }

              Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                DossierTabChip("ALL TIERS", selectedArmadaTier == 0) { selectedArmadaTier = 0 }
                DossierTabChip("SOLO", selectedArmadaTier == 1) { selectedArmadaTier = 1 }
                DossierTabChip("2-MAN", selectedArmadaTier == 2) { selectedArmadaTier = 2 }
                DossierTabChip("3-MAN", selectedArmadaTier == 3) { selectedArmadaTier = 3 }
                DossierTabChip("4-MAN", selectedArmadaTier == 4) { selectedArmadaTier = 4 }
                DossierTabChip("5-MAN", selectedArmadaTier == 5) { selectedArmadaTier = 5 }
              }
            }

            Spacer(modifier = Modifier.height(if (isCompactPhone) 3.dp else 5.dp))

            if (unifiedLeaderboard.isEmpty()) {
              Box(
                modifier =
                  Modifier.weight(1f)
                    .fillMaxWidth()
                    .background(Color(0x99111C2D), RoundedCornerShape(10.dp))
                    .border(1.dp, Color(0x4438BDF8), RoundedCornerShape(10.dp))
                    .padding(16.dp),
                contentAlignment = Alignment.Center,
              ) {
                Text(
                  text =
                    if (isDrifter || permanentUid.startsWith("dev_")) {
                      "NO SWORN PILOTS RECORDED IN THIS TIER YET — SIGN IN WITH GOOGLE TO RANK ON OFFICIAL LEADERBOARDS."
                    } else {
                      "NO SWORN PILOTS RECORDED IN THIS TIER YET."
                    },
                  color = Color(0xFF94A3B8),
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
                    .background(Color(0x99111C2D), RoundedCornerShape(10.dp))
                    .padding(if (isCompactPhone) 6.dp else 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
              ) {
                itemsIndexed(unifiedLeaderboard, key = { _, item -> item.userId.ifEmpty { item.id } }) { index, entry ->
                  val isSelf = entry.userId == permanentUid || entry.id == permanentUid
                  val kills = if (activeTab == 0) entry.pvpKills else entry.allTimeKills
                  val deaths = if (activeTab == 0) entry.pvpDeaths else entry.allTimeDeaths
                  val bounty = if (activeTab == 0) entry.pvpBounty else entry.allTimeBounty
                  val tierBadge =
                    if (entry.armadaTierSize <= 1) "SOLO" else "${entry.armadaTierSize}-MAN ARMADA"

                  Row(
                    modifier =
                      Modifier.fillMaxWidth()
                        .background(
                          if (isSelf) Color(0xCC143152) else Color(0x990D1B30),
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
                        color = if (index < 3) Color(0xFFFFD700) else Color(0xFF8A99AD),
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.width(30.dp),
                      )
                      ShipSpritePreview(shipId = entry.shipId.toInt(), sizeDp = 26)
                      Column {
                        Text(
                          text = if (isSelf) "${entry.callsign} (YOU — PINNED)" else entry.callsign,
                          color = if (isSelf) Color(0xFF00F0FF) else Color.White,
                          fontSize = 12.5.sp,
                          fontWeight = FontWeight.Bold,
                          fontFamily = FontFamily.SansSerif,
                        )
                        Text(
                          text =
                            "TIER: $tierBadge • HULL: ${RiftCodex.getShipById(entry.shipId.toInt()).name.uppercase()}",
                          color = Color(0xFF8A99AD),
                          fontSize = 10.sp,
                          fontFamily = FontFamily.SansSerif,
                        )
                      }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                      Text(
                        text = "PUBLIC WINS: 🏆 ${entry.convergencesWon}",
                        color = Color(0xFF00E676),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                      )
                      Text(
                        text = "K/D: $kills / $deaths",
                        color = Color(0xFF00F0FF),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.Monospace,
                      )
                      Text(
                        text = "PEAK BOUNTY: $bounty",
                        color = Color(0xFFFFD700),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.Monospace,
                      )
                    }
                  }
                }
              }
            }
          }

          1 -> {
            // DOSSIER UPGRADE 1: HALL OF FAME — PEAK BOUNTY RUN SNAPSHOT & EXECUTIONER TELEMETRY
            PeakBountyAndExecutionerSection(
              modifier = Modifier.weight(1f).fillMaxWidth(),
              isPvpOnlyMode = activeTab == 0,
              snapshot = displayedPeakSnapshot,
              currentLifeSnapshot = currentLifeSnapshot,
            )
          }

          2 -> {
            // DOSSIER UPGRADE 2: SYNDICATE AFFILIATION SERVICE RECORD & SYNDICATE WAR RANKINGS
            SyndicateServiceRecordSection(
              modifier = Modifier.weight(1f).fillMaxWidth(),
              activeSyndicateTag = syndicateTag,
              entries = syndicateServiceHistory,
            )
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
        .background(Color(0x99111C2D), RoundedCornerShape(12.dp))
        .padding(10.dp)
        .testTag("dossier_peak_bounty_section"),
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    val headerLabel =
      if (isPvpOnlyMode) {
        "🏆 HALL OF FAME: HIGHEST BOUNTY RUN SNAPSHOT (PVP ONLY)"
      } else {
        "🏆 HALL OF FAME: HIGHEST BOUNTY RUN SNAPSHOT (ALL-TIME)"
      }

    if (snapshot == null) {
      Box(
        modifier =
          Modifier.fillMaxWidth()
            .background(Color(0xCC0D1B30), RoundedCornerShape(10.dp))
            .border(1.dp, Color(0x44FFD700), RoundedCornerShape(10.dp))
            .padding(18.dp),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          text =
            if (isPvpOnlyMode) {
              "NO PVP PEAK BOUNTY SNAPSHOT RECORDED YET — ENGAGE HOSTILE HUMAN PILOTS IN PUBLIC FFA CONVERGENCE TO RECORD YOUR BEST PVP LIFE!"
            } else {
              "NO PEAK BOUNTY RUN SNAPSHOT RECORDED YET — LAUNCH INTO THE SECTOR TO RECORD YOUR FIRST LIFE TELEMETRY!"
            },
          color = Color(0xFF94A3B8),
          fontSize = 11.5.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.Monospace,
        )
      }
    } else {
      // Main Hall of Fame Snapshot Card
      Column(
        modifier =
          Modifier.fillMaxWidth()
            .background(Color(0xE60B172A), RoundedCornerShape(12.dp))
            .border(1.2.dp, Color(0xAAFFD700), RoundedCornerShape(12.dp))
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
            ShipSpritePreview(shipId = snapshot.shipId.coerceIn(1, 10), sizeDp = 36)
            Column {
              Text(
                text = headerLabel,
                color = Color(0xFFFFD700),
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.SansSerif,
              )
              Text(
                text =
                  "SHIP FLOWN: ${snapshot.shipFlown.uppercase(Locale.US)} • RECORDED: ${dateFormatter.format(Date(snapshot.recordedAtMs))}",
                color = Color(0xFF38BDF8),
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
              )
            }
          }

          Box(
            modifier =
              Modifier.background(Color(0xFF2B2106), RoundedCornerShape(8.dp))
                .border(1.dp, Color(0xFFFFD700), RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp),
          ) {
            Text(
              text = "👑 ${snapshot.bountyReached} BOUNTY",
              color = Color(0xFFFFD700),
              fontSize = 15.sp,
              fontWeight = FontWeight.ExtraBold,
              fontFamily = FontFamily.Monospace,
            )
          }
        }

        // Row 1 of Snapshot Metrics
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          DossierStatBox(
            modifier = Modifier.weight(1f),
            label = "TIME SURVIVED",
            value = snapshot.formattedDuration(),
            color = Color(0xFF38BDF8),
          )
          DossierStatBox(
            modifier = Modifier.weight(1f),
            label = "PVP / BOT KILLS",
            value = "${snapshot.pvpKills} PVP • ${snapshot.botKills} BOT",
            color = Color(0xFF00F0FF),
          )
          DossierStatBox(
            modifier = Modifier.weight(1f),
            label = "HVT SHUTDOWNS",
            value = "👑 ${snapshot.hvtShutdowns} HVTs",
            color = Color(0xFFFFD700),
          )
          DossierStatBox(
            modifier = Modifier.weight(1f),
            label = "CRATES / BUFFS",
            value = "📦 ${snapshot.buffsCollected} COLLECTED",
            color = Color(0xFF34D399),
          )
        }

        // Row 2 of Snapshot Metrics
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          DossierStatBox(
            modifier = Modifier.weight(1f),
            label = "DAMAGE DEALT",
            value = "${snapshot.damageDealt.toInt()} FLUX",
            color = Color(0xFF00E676),
          )
          DossierStatBox(
            modifier = Modifier.weight(1f),
            label = "DAMAGE TAKEN",
            value = "${snapshot.damageTaken.toInt()} FLUX",
            color = Color(0xFFF87171),
          )
          DossierStatBox(
            modifier = Modifier.weight(1f),
            label = "SHOTS FIRED / HIT",
            value = "${snapshot.shotsHit} / ${snapshot.shotsFired}",
            color = Color(0xFFF59E0B),
          )
          DossierStatBox(
            modifier = Modifier.weight(1f),
            label = "COMBAT ACCURACY",
            value = "${snapshot.accuracyPercent()}%",
            color = Color(0xFFC084FC),
          )
        }

        // Executioner ("TERMINATED BY") Breakdown Card
        val exec = snapshot.executioner
        val isConvergenceSurvival = exec.survivedToConvergence
        val isOngoingLiveStreak = !isConvergenceSurvival && exec.killerCallsign.isBlank()
        val execBorder =
          when {
            isConvergenceSurvival -> Color(0xFF00E676)
            isOngoingLiveStreak -> Color(0xFF38BDF8)
            else -> Color(0xFFFF3B4E)
          }
        val execBg =
          when {
            isConvergenceSurvival -> Color(0xCC06281E)
            isOngoingLiveStreak -> Color(0xCC0A2239)
            else -> Color(0xCC2A0D16)
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
            isConvergenceSurvival -> {
              Text(
                text = "🌀 RUN OUTCOME: SURVIVED TO SECTOR CONVERGENCE",
                color = Color(0xFF00E676),
                fontSize = 12.5.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
              )
              Text(
                text = "Never terminated! Hull remained intact through the 60-minute Public FFA Sector Convergence reset.",
                color = Color(0xFFD1FAE5),
                fontSize = 10.5.sp,
                fontFamily = FontFamily.SansSerif,
              )
            }
            isOngoingLiveStreak -> {
              Text(
                text = "🟢 RUN STATUS: ACTIVE UNDEFEATED STREAK IN SECTOR",
                color = Color(0xFF38BDF8),
                fontSize = 12.5.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
              )
              Text(
                text = "You are currently flying this record-setting life right now! Executioner telemetry will lock if terminated or at Sector Convergence.",
                color = Color(0xFFBAE6FD),
                fontSize = 10.5.sp,
                fontFamily = FontFamily.SansSerif,
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
                    text = "💀 TERMINATED BY: [${exec.killerSyndicateTag}] ${exec.killerCallsign}",
                    color = Color(0xFFFF3B4E),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace,
                  )
                  Text(
                    text = "EXECUTIONER LOADOUT: ${exec.killerShip} • ${exec.killingWeapon}",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif,
                  )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                  Column(horizontalAlignment = Alignment.End) {
                    Text(
                      text = "KILLER BOUNTY AT KILL",
                      color = Color(0xFF94A3B8),
                      fontSize = 9.sp,
                      fontWeight = FontWeight.Bold,
                    )
                    Text(
                      text = "${exec.killerBountyAtKill} BTY",
                      color = Color(0xFFFFD700),
                      fontSize = 12.sp,
                      fontWeight = FontWeight.ExtraBold,
                      fontFamily = FontFamily.Monospace,
                    )
                  }
                  Column(horizontalAlignment = Alignment.End) {
                    Text(
                      text = "BOUNTY EARNED FROM YOU",
                      color = Color(0xFF94A3B8),
                      fontSize = 9.sp,
                      fontWeight = FontWeight.Bold,
                    )
                    Text(
                      text = "+${exec.bountyEarnedFromYou} BTY",
                      color = Color(0xFFFF3B4E),
                      fontSize = 12.sp,
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

    // Current Active Life Telemetry Bar
    if (currentLifeSnapshot != null) {
      Row(
        modifier =
          Modifier.fillMaxWidth()
            .background(Color(0xCC0D1B30), RoundedCornerShape(10.dp))
            .border(1.dp, Color(0x5538BDF8), RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "🛰️ CURRENT ACTIVE SORTIE (${currentLifeSnapshot.shipFlown}):",
          color = Color(0xFF38BDF8),
          fontSize = 11.sp,
          fontWeight = FontWeight.ExtraBold,
          fontFamily = FontFamily.Monospace,
        )
        Text(
          text =
            "BOUNTY: ${currentLifeSnapshot.bountyReached} • SURVIVED: ${currentLifeSnapshot.formattedDuration()} • KILLS: ${currentLifeSnapshot.pvpKills + currentLifeSnapshot.botKills} • DMG: ${currentLifeSnapshot.damageDealt.toInt()} FLUX • ACC: ${currentLifeSnapshot.accuracyPercent()}%",
          color = Color.White,
          fontSize = 10.5.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.Monospace,
        )
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

  val cleanActiveTag =
    remember(activeSyndicateTag) {
      activeSyndicateTag.trim().uppercase().ifEmpty { "GUEST" }
    }
  val myWarRecord =
    remember(cleanActiveTag, entries) {
      localPrefs.getSyndicateWarRecord(cleanActiveTag)
    }

  val (topByWarTrophies, topByCollectiveBounty) =
    remember(cleanActiveTag, entries) {
      val byTag = linkedMapOf<String, SyndicateInfo>()
      for (custom in localPrefs.getAllSyndicates()) {
        if (custom.isRealPlayerFoundedSyndicate()) {
          byTag[custom.tag.uppercase()] = custom
        }
      }
      val allPairs =
        byTag.values.map { syn ->
          syn to localPrefs.getSyndicateWarRecord(syn.tag)
        }
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
        .background(Color(0x99111C2D), RoundedCornerShape(12.dp))
        .padding(10.dp)
        .testTag("dossier_syndicate_history_section"),
    verticalArrangement = Arrangement.spacedBy(6.dp),
  ) {
    // 1. Syndicate Wars Performance Card + 2-Column Player Syndicate Ranking Comparison
    Column(
      modifier =
        Modifier.fillMaxWidth()
          .background(Color(0xCC0A1829), RoundedCornerShape(10.dp))
          .border(1.dp, Color(0x88F59E0B), RoundedCornerShape(10.dp))
          .padding(horizontal = 10.dp, vertical = 7.dp)
          .testTag("dossier_syndicate_war_performance_card"),
      verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "🏆 SYNDICATE WARS PERFORMANCE // [$cleanActiveTag]",
          color = Color(0xFFFBBF24),
          fontSize = 11.sp,
          fontWeight = FontWeight.ExtraBold,
          fontFamily = FontFamily.SansSerif,
        )
        Text(
          text =
            "🏆 ${myWarRecord.warTrophies} WAR TROPHIES • ${myWarRecord.warWins}W - ${myWarRecord.warLosses}L • 🔥 ${myWarRecord.warStreak} STREAK",
          color = Color(0xFF00E676),
          fontSize = 10.5.sp,
          fontWeight = FontWeight.ExtraBold,
          fontFamily = FontFamily.Monospace,
        )
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        // Column 1: Syndicate War Trophies Ladder
        Column(
          modifier =
            Modifier.weight(1f)
              .background(Color(0x9906101E), RoundedCornerShape(8.dp))
              .border(0.8.dp, Color(0x55F59E0B), RoundedCornerShape(8.dp))
              .padding(6.dp),
          verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
          Text(
            text = "⚔️ SYNDICATE WAR TROPHIES (TOP 4)",
            color = Color(0xFFFBBF24),
            fontSize = 9.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = FontFamily.Monospace,
          )
          if (topByWarTrophies.isEmpty()) {
            Text(
              text = "No player Syndicates founded yet",
              color = Color(0xFF64748B),
              fontSize = 8.5.sp,
              fontFamily = FontFamily.Monospace,
            )
          } else {
            topByWarTrophies.forEachIndexed { idx, (syn, rec) ->
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Text(
                  text = "#${idx + 1} [${syn.tag}] ${syn.name}",
                  color = if (syn.tag == cleanActiveTag) Color(0xFF00F0FF) else Color.White,
                  fontSize = 8.8.sp,
                  fontWeight = FontWeight.Bold,
                  fontFamily = FontFamily.Monospace,
                )
                Text(
                  text = "🏆 ${rec.warTrophies} (${rec.warWins}W-${rec.warLosses}L)",
                  color = Color(0xFFFBBF24),
                  fontSize = 8.8.sp,
                  fontWeight = FontWeight.Bold,
                  fontFamily = FontFamily.Monospace,
                )
              }
            }
          }
        }

        // Column 2: Collective Member Bounty Prestige Ladder
        Column(
          modifier =
            Modifier.weight(1f)
              .background(Color(0x9906101E), RoundedCornerShape(8.dp))
              .border(0.8.dp, Color(0x5500F0FF), RoundedCornerShape(8.dp))
              .padding(6.dp),
          verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
          Text(
            text = "📊 COLLECTIVE MEMBER BOUNTY (TOP 4)",
            color = Color(0xFF00F0FF),
            fontSize = 9.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = FontFamily.Monospace,
          )
          if (topByCollectiveBounty.isEmpty()) {
            Text(
              text = "No player Syndicates founded yet",
              color = Color(0xFF64748B),
              fontSize = 8.5.sp,
              fontFamily = FontFamily.Monospace,
            )
          } else {
            topByCollectiveBounty.forEachIndexed { idx, (syn, _) ->
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Text(
                  text = "#${idx + 1} [${syn.tag}] ${syn.name}",
                  color = if (syn.tag == cleanActiveTag) Color(0xFFFFD700) else Color.White,
                  fontSize = 8.8.sp,
                  fontWeight = FontWeight.Bold,
                  fontFamily = FontFamily.Monospace,
                )
                Text(
                  text = "${syn.combinedBounty} BTY (${syn.memberCount}P)",
                  color = Color(0xFF38BDF8),
                  fontSize = 8.8.sp,
                  fontWeight = FontWeight.Bold,
                  fontFamily = FontFamily.Monospace,
                )
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
        text = "🏛️ CHRONOLOGICAL SYNDICATE SERVICE RECORD (MOST RECENT FIRST)",
        color = Color(0xFF34D399),
        fontSize = 12.sp,
        fontWeight = FontWeight.ExtraBold,
        fontFamily = FontFamily.SansSerif,
      )
      Text(
        text = "TOTAL COMMISSIONS: ${ordered.size}",
        color = Color(0xFFFFD700),
        fontSize = 10.5.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace,
      )
    }

    LazyColumn(
      modifier = Modifier.weight(1f).fillMaxWidth(),
      verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      itemsIndexed(ordered) { idx, item ->
        val isActive = item.isActive
        Row(
          modifier =
            Modifier.fillMaxWidth()
              .background(
                if (isActive) Color(0xCC0B2822) else Color(0x990D1B30),
                RoundedCornerShape(9.dp),
              )
              .border(
                1.dp,
                if (isActive) Color(0xFF10B981) else Color(0x33FFFFFF),
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
                      if (isActive) Color(0xFF10B981) else Color(0xFF1E293B),
                      RoundedCornerShape(5.dp),
                    )
                    .padding(horizontal = 6.dp, vertical = 2.dp),
              ) {
                Text(
                  text = if (isActive) "ACTIVE" else "HONORABLE DISCHARGE",
                  color = if (isActive) Color(0xFF04130E) else Color(0xFF94A3B8),
                  fontSize = 8.5.sp,
                  fontWeight = FontWeight.ExtraBold,
                  fontFamily = FontFamily.Monospace,
                )
              }
              Text(
                text = "[${item.syndicateTag}] ${item.syndicateName}",
                color = Color.White,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.SansSerif,
              )
            }
            val joinedStr = dateFormatter.format(Date(item.joinedTimestamp))
            val leftStr =
              if (isActive) "PRESENT" else dateFormatter.format(Date(item.leftTimestamp))
            Text(
              text = "ROLE: ${item.role.uppercase(Locale.US)} • COMMISSION: $joinedStr → $leftStr",
              color = Color(0xFF94A3B8),
              fontSize = 10.sp,
              fontFamily = FontFamily.Monospace,
            )
          }

          Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(horizontalAlignment = Alignment.End) {
              Text("TENURE", color = Color(0xFF94A3B8), fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
              Text(
                text = item.formatTenure(nowMs),
                color = Color(0xFF38BDF8),
                fontSize = 11.5.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
              )
            }
            Column(horizontalAlignment = Alignment.End) {
              Text("BANNER KILLS", color = Color(0xFF94A3B8), fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
              Text(
                text = "${item.totalKills}",
                color = Color(0xFF00F0FF),
                fontSize = 11.5.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
              )
            }
            Column(horizontalAlignment = Alignment.End) {
              Text("PEAK BOUNTY", color = Color(0xFF94A3B8), fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
              Text(
                text = "${item.peakBountyAchieved}",
                color = Color(0xFFFFD700),
                fontSize = 11.5.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
              )
            }
            Column(horizontalAlignment = Alignment.End) {
              Text("PUBLIC WINS", color = Color(0xFF94A3B8), fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
              Text(
                text = "🏆 ${item.publicGameWins}",
                color = Color(0xFF00E676),
                fontSize = 11.5.sp,
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
        .background(if (selected) accentColor else Color(0xCC101B2D))
        .border(
          1.2.dp,
          if (selected) accentColor else accentColor.copy(alpha = 0.55f),
          RoundedCornerShape(8.dp),
        )
        .clickable { onClick() }
        .padding(
          horizontal = if (isCompactPhone) 8.dp else 10.dp,
          vertical = if (isCompactPhone) 4.dp else 6.dp,
        )
        .testTag(testTag),
  ) {
    Text(
      text = label,
      color = if (selected) Color(0xFF05080E) else accentColor,
      fontSize = if (isCompactPhone) 9.sp else 10.sp,
      fontWeight = FontWeight.ExtraBold,
      fontFamily = FontFamily.SansSerif,
      maxLines = 1,
    )
  }
}

@Composable
private fun XpSourceChip(
  modifier: Modifier = Modifier,
  label: String,
  xpValue: Long,
  color: Color,
) {
  Row(
    modifier =
      modifier
        .background(Color(0xFF081426), RoundedCornerShape(6.dp))
        .border(0.8.dp, color.copy(alpha = 0.55f), RoundedCornerShape(6.dp))
        .padding(horizontal = 6.dp, vertical = 3.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Text(
      text = label,
      color = Color(0xFF94A3B8),
      fontSize = 8.sp,
      fontWeight = FontWeight.Bold,
      fontFamily = FontFamily.SansSerif,
      maxLines = 1,
    )
    Text(
      text = "+$xpValue XP",
      color = color,
      fontSize = 9.sp,
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
) {
  Column(
    modifier =
      modifier
        .background(Color(0x99111C2D), RoundedCornerShape(8.dp))
        .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
        .padding(
          horizontal = if (isCompactPhone) 6.dp else 8.dp,
          vertical = if (isCompactPhone) 3.dp else 5.dp,
        )
  ) {
    Text(
      text = label,
      color = Color(0xFF8A99AD),
      fontSize = if (isCompactPhone) 7.5.sp else 8.5.sp,
      fontWeight = FontWeight.Bold,
      fontFamily = FontFamily.SansSerif,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
    Text(
      text = value,
      color = color,
      fontSize = if (isCompactPhone) 10.5.sp else 12.5.sp,
      fontWeight = FontWeight.Bold,
      fontFamily = FontFamily.Monospace,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
  }
}

@Composable
private fun DossierTabChip(
  label: String,
  selected: Boolean,
  onClick: () -> Unit,
) {
  val isCompactPhone = LocalConfiguration.current.screenHeightDp < 480
  Box(
    modifier =
      Modifier.shadow(2.dp, RoundedCornerShape(8.dp))
        .clip(RoundedCornerShape(8.dp))
        .background(if (selected) Color(0xFF00F0FF) else Color(0x99111C2D))
        .border(
          1.2.dp,
          if (selected) Color(0xFF00F0FF) else Color(0x4438BDF8),
          RoundedCornerShape(8.dp),
        )
        .clickable { onClick() }
        .padding(
          horizontal = if (isCompactPhone) 7.dp else 8.dp,
          vertical = if (isCompactPhone) 3.5.dp else 5.dp,
        ),
  ) {
    Text(
      text = label,
      color = if (selected) Color(0xFF05080E) else Color(0xFF00F0FF),
      fontSize = if (isCompactPhone) 8.2.sp else 9.sp,
      fontWeight = FontWeight.Bold,
      fontFamily = FontFamily.SansSerif,
      maxLines = 1,
    )
  }
}
