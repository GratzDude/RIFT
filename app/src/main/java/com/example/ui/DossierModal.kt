package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.LeaderboardEntry
import com.example.data.PeakBountyRunSnapshot
import com.example.data.SyndicateServiceEntry
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
  onCloseModal: () -> Unit,
) {
  BackHandler { onCloseModal() }

  // Top-level Dossier view: 0 = LEADERBOARD, 1 = PEAK BOUNTY & EXECUTIONER, 2 = SYNDICATE SERVICE RECORD
  var dossierSection by remember { mutableIntStateOf(0) }
  var activeTab by remember { mutableIntStateOf(1) } // 0 = PVP ONLY, 1 = ALL-TIME
  var selectedArmadaTier by remember { mutableIntStateOf(0) } // 0 = ALL, 1 = SOLO, 2..5 = 2..5-MAN ARMADA
  var sortMode by remember { mutableIntStateOf(0) } // 0 = BOUNTY, 1 = PUBLIC WINS, 2 = K/D

  // Deduplicate strictly by permanent userId so editing Callsign never creates ghost rows
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
    ) {
      val byUid = linkedMapOf<String, LeaderboardEntry>()
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

      for (entry in cloudLeaderboardEntries) {
        val uidKey = entry.userId.ifEmpty { entry.id }
        if (uidKey.isNotEmpty() && uidKey != permanentUid) {
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

  Box(
    modifier =
      Modifier.fillMaxSize()
        .background(Color(0xB3000000))
        .clickable { onCloseModal() }
        .padding(horizontal = 18.dp, vertical = 8.dp)
        .testTag("dossier_modal"),
    contentAlignment = Alignment.Center,
  ) {
    Surface(
      modifier =
        Modifier.fillMaxSize()
          .clickable(enabled = false) {}
          .border(1.dp, Color(0x6600F0FF), RoundedCornerShape(16.dp)),
      color = Color(0xE60A0F18),
      shape = RoundedCornerShape(16.dp),
    ) {
      Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        // 1. Header Row
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Column {
            Text(
              text = "📋 PILOT DOSSIER, HALL OF FAME & SERVICE RECORD",
              color = Color.White,
              fontSize = 16.sp,
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.SansSerif,
              letterSpacing = 1.0.sp,
            )
            val tierDesc =
              if (activeArmadaName.isEmpty() || activeArmadaSize <= 1) {
                "[SOLO] TIER"
              } else {
                "${activeArmadaSize}-MAN ARMADA [$activeArmadaName] TIER"
              }
            val syndicatePrefix = if (syndicateTag.isNotBlank()) "[$syndicateTag] " else ""
            Text(
              text = "UID: $permanentUid • ${syndicatePrefix}$callsign • ACTIVE: $tierDesc • PRESENCE: $matchPresencePct%",
              color = Color(0xFFFFD700),
              fontSize = 10.sp,
              fontWeight = FontWeight.Medium,
              fontFamily = FontFamily.Monospace,
            )
          }

          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            if (onResetLocalPilotData != null) {
              Button(
                onClick = onResetLocalPilotData,
                colors =
                  ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1E293B),
                    contentColor = Color(0xFFF59E0B),
                  ),
                border = BorderStroke(1.dp, Color(0xFFF59E0B)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.height(36.dp).testTag("dossier_reset_pilot_button"),
              ) {
                Text(
                  text = "↺ RESET PILOT",
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold,
                  fontFamily = FontFamily.SansSerif,
                )
              }
            }

            Row(
              modifier =
                Modifier.clip(RoundedCornerShape(8.dp))
                  .background(Color(0xFF1A1405))
                  .border(1.dp, Color(0xFFFFD700), RoundedCornerShape(8.dp))
                  .clickable { onOpenRiftShardsStore() }
                  .padding(horizontal = 10.dp, vertical = 6.dp)
                  .testTag("dossier_btn_get_rift_shards"),
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
              modifier = Modifier.height(36.dp).testTag("dossier_close_button"),
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

        Spacer(modifier = Modifier.height(5.dp))

        // 2. Career Level & XP Progress Card
        Column(
          modifier =
            Modifier.fillMaxWidth()
              .background(Color(0xCC0D1B30), RoundedCornerShape(10.dp))
              .border(1.dp, Color(0x6600F0FF), RoundedCornerShape(10.dp))
              .padding(horizontal = 10.dp, vertical = 5.dp)
              .testTag("dossier_career_level_card"),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Box(
                modifier =
                  Modifier.background(Color(0xFF00F0FF), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 2.dp),
              ) {
                Text(
                  text = "PILOT LVL $pilotLevel / 100",
                  color = Color(0xFF040914),
                  fontSize = 10.5.sp,
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
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
              )
            }

            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
              Text(
                text = "CUSTOMIZATION PTS: $availableCustomPts AVAIL ($customizationPointsTotal / 100)",
                color = Color(0xFF00E676),
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
              )
              Text(
                text = "RIFT SHARDS: ❖ $riftShards",
                color = Color(0xFFFFD700),
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
              )
            }
          }

          Spacer(modifier = Modifier.height(3.dp))

          Box(
            modifier =
              Modifier.fillMaxWidth()
                .height(4.dp)
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

        Spacer(modifier = Modifier.height(5.dp))

        // 3. Primary Section Navigation Sub-Tabs
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            DossierSectionButton(
              label = "🏆 ARMADA LEADERBOARDS",
              selected = dossierSection == 0,
              accentColor = Color(0xFF00F0FF),
              testTag = "dossier_tab_leaderboards",
              onClick = { dossierSection = 0 },
            )
            DossierSectionButton(
              label = "💀 PEAK BOUNTY & EXECUTIONER",
              selected = dossierSection == 1,
              accentColor = Color(0xFFFFD700),
              testTag = "dossier_tab_peak_snapshot",
              onClick = { dossierSection = 1 },
            )
            DossierSectionButton(
              label = "🏛️ SYNDICATE SERVICE RECORD (${syndicateServiceHistory.size})",
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

        Spacer(modifier = Modifier.height(5.dp))

        when (dossierSection) {
          0 -> {
            // Summary KPI Cards + Compact Clickable Hall of Fame Peak Run Banner
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              DossierStatBox(
                modifier = Modifier.weight(1f),
                label = "ALL-TIME KILLS / DEATHS",
                value = "$allTimeKills K / $allTimeDeaths D",
                color = Color(0xFF00F0FF),
              )
              DossierStatBox(
                modifier = Modifier.weight(1f),
                label = "PVP KILLS / DEATHS",
                value = "$pvpKills K / $pvpDeaths D",
                color = Color(0xFFFFD700),
              )
              DossierStatBox(
                modifier = Modifier.weight(1f).clickable { dossierSection = 1 },
                label = "PEAK BOUNTY (TAP FOR RUN)",
                value = "$allTimeBountyPeak BTY ↗",
                color = Color(0xFFFFD700),
              )
              DossierStatBox(
                modifier = Modifier.weight(1f),
                label = "PUBLIC GAME WINS",
                value = "🏆 $convergencesWon WINS",
                color = Color(0xFF00E676),
              )
              DossierStatBox(
                modifier = Modifier.weight(1f).clickable { dossierSection = 2 },
                label = "SYNDICATE TENURE",
                value = "[$syndicateTag] ↗",
                color = Color(0xFF34D399),
              )
            }

            Spacer(modifier = Modifier.height(5.dp))

            // Sort + Armada Tier Filter Row
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
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

            Spacer(modifier = Modifier.height(5.dp))

            LazyColumn(
              modifier =
                Modifier.weight(1f)
                  .fillMaxWidth()
                  .background(Color(0x99111C2D), RoundedCornerShape(12.dp))
                  .padding(8.dp),
              verticalArrangement = Arrangement.spacedBy(5.dp),
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
            // DOSSIER UPGRADE 2: SYNDICATE AFFILIATION SERVICE RECORD
            SyndicateServiceRecordSection(
              modifier = Modifier.weight(1f).fillMaxWidth(),
              entries = syndicateServiceHistory,
            )
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
            ShipSpritePreview(shipId = snapshot.shipId.coerceIn(1, 9), sizeDp = 36)
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
            value = "${snapshot.damageDealt.toInt()} ❖",
            color = Color(0xFF00E676),
          )
          DossierStatBox(
            modifier = Modifier.weight(1f),
            label = "DAMAGE TAKEN",
            value = "${snapshot.damageTaken.toInt()} ❖",
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
          text = "⚡ CURRENT ACTIVE SORTIE (${currentLifeSnapshot.shipFlown}):",
          color = Color(0xFF38BDF8),
          fontSize = 11.sp,
          fontWeight = FontWeight.ExtraBold,
          fontFamily = FontFamily.Monospace,
        )
        Text(
          text =
            "BOUNTY: ${currentLifeSnapshot.bountyReached} • SURVIVED: ${currentLifeSnapshot.formattedDuration()} • KILLS: ${currentLifeSnapshot.pvpKills + currentLifeSnapshot.botKills} • DMG: ${currentLifeSnapshot.damageDealt.toInt()}❖ • ACC: ${currentLifeSnapshot.accuracyPercent()}%",
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
  entries: List<SyndicateServiceEntry>,
) {
  val dateFormatter = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US) }
  val nowMs = System.currentTimeMillis()
  val ordered = remember(entries) { entries.reversed() }

  Column(
    modifier =
      modifier
        .background(Color(0x99111C2D), RoundedCornerShape(12.dp))
        .padding(10.dp)
        .testTag("dossier_syndicate_history_section"),
    verticalArrangement = Arrangement.spacedBy(6.dp),
  ) {
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
  Box(
    modifier =
      Modifier.clip(RoundedCornerShape(8.dp))
        .background(if (selected) accentColor else Color(0xCC101B2D))
        .border(
          1.dp,
          if (selected) accentColor else accentColor.copy(alpha = 0.45f),
          RoundedCornerShape(8.dp),
        )
        .clickable { onClick() }
        .padding(horizontal = 10.dp, vertical = 6.dp)
        .testTag(testTag),
  ) {
    Text(
      text = label,
      color = if (selected) Color(0xFF05080E) else accentColor,
      fontSize = 10.sp,
      fontWeight = FontWeight.ExtraBold,
      fontFamily = FontFamily.SansSerif,
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
) {
  Column(
    modifier =
      modifier
        .background(Color(0x99111C2D), RoundedCornerShape(10.dp))
        .padding(horizontal = 8.dp, vertical = 5.dp)
  ) {
    Text(
      text = label,
      color = Color(0xFF8A99AD),
      fontSize = 8.5.sp,
      fontWeight = FontWeight.Bold,
      fontFamily = FontFamily.SansSerif,
    )
    Text(
      text = value,
      color = color,
      fontSize = 12.5.sp,
      fontWeight = FontWeight.Bold,
      fontFamily = FontFamily.Monospace,
    )
  }
}

@Composable
private fun DossierTabChip(
  label: String,
  selected: Boolean,
  onClick: () -> Unit,
) {
  Box(
    modifier =
      Modifier.clip(RoundedCornerShape(8.dp))
        .background(if (selected) Color(0xFF00F0FF) else Color(0x99111C2D))
        .border(
          1.dp,
          if (selected) Color(0xFF00F0FF) else Color(0x33FFFFFF),
          RoundedCornerShape(8.dp),
        )
        .clickable { onClick() }
        .padding(horizontal = 8.dp, vertical = 5.dp),
  ) {
    Text(
      text = label,
      color = if (selected) Color(0xFF05080E) else Color(0xFF00F0FF),
      fontSize = 9.sp,
      fontWeight = FontWeight.Bold,
      fontFamily = FontFamily.SansSerif,
    )
  }
}
