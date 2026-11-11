package com.example.ui

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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.engine.RiftCodex

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
  onOpenRiftShardsStore: () -> Unit = {},
  onCloseModal: () -> Unit,
) {
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
      val localEntry =
        LeaderboardEntry(
          id = permanentUid,
          userId = permanentUid,
          callsign = "[$localArmadaBadge] [$syndicateTag] $callsign",
          syndicateTag = syndicateTag,
          armadaTag = localArmadaBadge,
          armadaTierSize = activeArmadaSize.coerceIn(1, 5),
          shipId = shipId.toLong(),
          foundry = shipSpec.foundry,
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
          byUid[uidKey] = entry
        }
      }

      val sectorAces =
        listOf(
          LeaderboardEntry(
            id = "ace_kestrel_1",
            userId = "ace_kestrel_1",
            callsign = "VORTEX ACE",
            syndicateTag = "APEX 4",
            armadaTag = "APEX 4",
            armadaTierSize = 4,
            shipId = 1L,
            foundry = "Kestrel",
            pvpKills = 18L,
            pvpDeaths = 4L,
            pvpBounty = 480L,
            allTimeKills = 64L,
            allTimeDeaths = 12L,
            allTimeBounty = 690L,
            convergencesWon = 5L,
          ),
          LeaderboardEntry(
            id = "ace_aurelia_4",
            userId = "ace_aurelia_4",
            callsign = "AURELIA PRIME",
            syndicateTag = "GOLD WING",
            armadaTag = "GOLD WING",
            armadaTierSize = 2,
            shipId = 4L,
            foundry = "Aurelia",
            pvpKills = 14L,
            pvpDeaths = 5L,
            pvpBounty = 410L,
            allTimeKills = 52L,
            allTimeDeaths = 15L,
            allTimeBounty = 540L,
            convergencesWon = 4L,
          ),
          LeaderboardEntry(
            id = "ace_vortex_6",
            userId = "ace_vortex_6",
            callsign = "CHRONO X",
            syndicateTag = "GUEST",
            armadaTag = "SOLO",
            armadaTierSize = 1,
            shipId = 6L,
            foundry = "Vortex",
            pvpKills = 11L,
            pvpDeaths = 6L,
            pvpBounty = 340L,
            allTimeKills = 41L,
            allTimeDeaths = 14L,
            allTimeBounty = 460L,
            convergencesWon = 3L,
          ),
          LeaderboardEntry(
            id = "ace_rustweld_7",
            userId = "ace_rustweld_7",
            callsign = "VALKYRIE Z",
            syndicateTag = "VOID 3",
            armadaTag = "VOID 3",
            armadaTierSize = 3,
            shipId = 7L,
            foundry = "Rustweld",
            pvpKills = 9L,
            pvpDeaths = 5L,
            pvpBounty = 290L,
            allTimeKills = 36L,
            allTimeDeaths = 11L,
            allTimeBounty = 390L,
            convergencesWon = 2L,
          ),
          LeaderboardEntry(
            id = "ace_nova_5",
            userId = "ace_nova_5",
            callsign = "[NOVA_5] [NOVA] HYPERION",
            syndicateTag = "NOVA",
            armadaTag = "NOVA_5",
            armadaTierSize = 5,
            shipId = 5L,
            foundry = "Vortex",
            pvpKills = 16L,
            pvpDeaths = 5L,
            pvpBounty = 440L,
            allTimeKills = 58L,
            allTimeDeaths = 13L,
            allTimeBounty = 610L,
            convergencesWon = 4L,
          ),
        )
      for (ace in sectorAces) {
        if (!byUid.containsKey(ace.userId)) {
          byUid[ace.userId] = ace
        }
      }

      byUid.values
        .filter { entry ->
          val isSelf = entry.userId == permanentUid || entry.id == permanentUid
          isSelf || selectedArmadaTier == 0 || entry.armadaTierSize == selectedArmadaTier
        }
        .sortedByDescending { entry ->
          val k = if (activeTab == 0) entry.pvpKills else entry.allTimeKills
          val d = (if (activeTab == 0) entry.pvpDeaths else entry.allTimeDeaths).coerceAtLeast(1L)
          val bty = if (activeTab == 0) entry.pvpBounty else entry.allTimeBounty
          when (sortMode) {
            1 -> entry.convergencesWon * 100_000L + bty // Sort by Public Game Wins first
            2 -> ((k.toDouble() / d.toDouble()) * 10_000.0).toLong() + k * 100L // Sort by K/D Ratio
            else -> bty + k * 25L // Sort by Bounty & Kills
          }
        }
    }

  val availableCustomPts = (customizationPointsTotal - spentCustomizationPoints).coerceAtLeast(0)

  Box(
    modifier =
      Modifier.fillMaxSize()
        .background(Color(0xB3000000))
        .padding(horizontal = 20.dp, vertical = 8.dp)
        .testTag("dossier_modal"),
    contentAlignment = Alignment.Center,
  ) {
    Surface(
      modifier =
        Modifier.fillMaxSize()
          .border(1.dp, Color(0x6600F0FF), RoundedCornerShape(16.dp)),
      color = Color(0xE60A0F18), // #0A0F18 with 90% opacity glassmorphism
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
              text = "📋 PILOT DOSSIER & ARMADA LEADERBOARDS",
              color = Color.White,
              fontSize = 17.sp,
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.SansSerif,
              letterSpacing = 1.2.sp,
            )
            val tierDesc =
              if (activeArmadaName.isEmpty() || activeArmadaSize <= 1) {
                "[SOLO] TIER"
              } else {
                "${activeArmadaSize}-MAN ARMADA [$activeArmadaName] TIER"
              }
            Text(
              text = "UID: $permanentUid • [$syndicateTag] $callsign • ACTIVE: $tierDesc • PRESENCE: $matchPresencePct%",
              color = Color(0xFFFFD700),
              fontSize = 10.5.sp,
              fontWeight = FontWeight.Medium,
              fontFamily = FontFamily.Monospace,
            )
          }

          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            // [+ GET RIFT SHARDS] Store & Top-Up Modal Button
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
              modifier = Modifier.height(34.dp).testTag("dossier_close_button"),
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

        // 2. Career Level & XP Progress Card (Levels 1–100, Customization Points, Rift Shards & 5 XP Sources)
        Column(
          modifier =
            Modifier.fillMaxWidth()
              .background(Color(0xCC0D1B30), RoundedCornerShape(10.dp))
              .border(1.dp, Color(0x6600F0FF), RoundedCornerShape(10.dp))
              .padding(horizontal = 10.dp, vertical = 6.dp)
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
                  fontSize = 11.sp,
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
                fontSize = 10.5.sp,
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
                fontSize = 10.5.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
              )
              Text(
                text = "RIFT SHARDS: ❖ $riftShards",
                color = Color(0xFFFFD700),
                fontSize = 10.5.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
              )
            }
          }

          Spacer(modifier = Modifier.height(4.dp))

          // Thin XP Progress Bar
          Box(
            modifier =
              Modifier.fillMaxWidth()
                .height(5.dp)
                .clip(RoundedCornerShape(2.5.dp))
                .background(Color(0xFF142844))
          ) {
            Box(
              modifier =
                Modifier.fillMaxWidth(levelProgressFraction.coerceIn(0f, 1f))
                  .fillMaxHeight()
                  .background(Color(0xFF00E676), RoundedCornerShape(2.5.dp))
            )
          }

          Spacer(modifier = Modifier.height(5.dp))

          // 5 XP Sources Breakdown Row
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            XpSourceChip(
              modifier = Modifier.weight(1f),
              label = "1. BUFFS COLLECTED",
              xpValue = xpFromBuffs,
              color = Color(0xFF10B981),
            )
            XpSourceChip(
              modifier = Modifier.weight(1f),
              label = "2. DAMAGE DONE",
              xpValue = xpFromDamage,
              color = Color(0xFF38BDF8),
            )
            XpSourceChip(
              modifier = Modifier.weight(1f),
              label = "3. ACCURACY ACHIEVED",
              xpValue = xpFromAccuracy,
              color = Color(0xFFF59E0B),
            )
            XpSourceChip(
              modifier = Modifier.weight(1f),
              label = "4. BOUNTY EARNED",
              xpValue = xpFromBountyEarned,
              color = Color(0xFFFFD700),
            )
            XpSourceChip(
              modifier = Modifier.weight(1f),
              label = "5. BOUNTY KILLED",
              xpValue = xpFromBountyKilled,
              color = Color(0xFFF87171),
            )
          }
        }

        Spacer(modifier = Modifier.height(5.dp))

        // 3. Summary KPI Cards for Local Pilot (Includes Prominent PUBLIC GAME WINS Card)
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
            modifier = Modifier.weight(1f),
            label = "PEAK BOUNTY RECORD",
            value = "$allTimeBountyPeak BTY",
            color = Color(0xFFFFD700),
          )
          DossierStatBox(
            modifier = Modifier.weight(1f),
            label = "PUBLIC GAME WINS",
            value = "🏆 $convergencesWon WINS",
            color = Color(0xFF00E676),
          )
          DossierStatBox(
            modifier = Modifier.weight(1f),
            label = "ACTIVE ARMADA TIER",
            value = if (activeArmadaSize <= 1) "SOLO RIFT" else "${activeArmadaSize}-MAN FLEET",
            color = Color(0xFF38BDF8),
          )
        }

        Spacer(modifier = Modifier.height(5.dp))

        // 4. Leaderboard Mode Toggle Tabs + Sort Selector + Adaptive Armada Roster Tier Filter
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
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
            DossierTabChip(
              label = "SORT: BOUNTY",
              selected = sortMode == 0,
              onClick = { sortMode = 0 },
            )
            DossierTabChip(
              label = "SORT: PUBLIC WINS",
              selected = sortMode == 1,
              onClick = { sortMode = 1 },
            )
            DossierTabChip(
              label = "SORT: K/D",
              selected = sortMode == 2,
              onClick = { sortMode = 2 },
            )
          }

          Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            DossierTabChip(
              label = "ALL TIERS",
              selected = selectedArmadaTier == 0,
              onClick = { selectedArmadaTier = 0 },
            )
            DossierTabChip(
              label = "SOLO",
              selected = selectedArmadaTier == 1,
              onClick = { selectedArmadaTier = 1 },
            )
            DossierTabChip(
              label = "2-MAN",
              selected = selectedArmadaTier == 2,
              onClick = { selectedArmadaTier = 2 },
            )
            DossierTabChip(
              label = "3-MAN",
              selected = selectedArmadaTier == 3,
              onClick = { selectedArmadaTier = 3 },
            )
            DossierTabChip(
              label = "4-MAN",
              selected = selectedArmadaTier == 4,
              onClick = { selectedArmadaTier = 4 },
            )
            DossierTabChip(
              label = "5-MAN",
              selected = selectedArmadaTier == 5,
              onClick = { selectedArmadaTier = 5 },
            )
          }
        }

        Spacer(modifier = Modifier.height(5.dp))

        // 5. Leaderboard Table (Pinned/Highlighted Local Player Row + PUBLIC WINS Column)
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
                    text = "TIER: $tierBadge • HULL #${entry.shipId} (${entry.foundry.uppercase()})",
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
      fontSize = 9.sp,
      fontWeight = FontWeight.Bold,
      fontFamily = FontFamily.SansSerif,
    )
    Text(
      text = value,
      color = color,
      fontSize = 13.sp,
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
