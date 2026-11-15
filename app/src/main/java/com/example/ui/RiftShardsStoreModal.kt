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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.LocalPilotPrefs
import com.example.engine.CosmeticCatalogItem
import com.example.engine.RiftProgressionCatalog

/**
 * Dedicated NEXUS `PROGRESSION` Hub & 3D Rift Shard Foundry.
 * Renders as a frameless interior pane inside the persistent NEXUS Master Window Shell.
 */
@Composable
fun RiftShardsStoreModal(
  pilotLevel: Int,
  currentLevelXp: Long,
  nextLevelXpSpan: Long,
  levelProgressFraction: Float,
  totalCustomizationPoints: Int,
  spentCustomizationPoints: Int,
  riftShards: Long,
  publicGameWins: Long,
  matchPresencePct: Int,
  unlockedExtraSlots: Int,
  aiShipDesignTokens: Int,
  onTopUpRiftShards: (Long) -> Unit,
  onRespecCustomizationPoints: () -> Boolean,
  onAllocateCustomizationPoint: () -> Boolean,
  onUnlockExtraSlot: () -> Boolean,
  onPurchaseAiDesignToken: () -> Boolean,
  onSimulateFfaWinPayout: () -> Unit,
  onSimulatePilotLevelUp: () -> Unit,
  onSpendRiftShards: ((Long) -> Boolean)? = null,
  onOpenHangarShip10: () -> Unit = {},
  onCloseModal: () -> Unit = {},
) {
  val context = LocalContext.current
  val localPrefs = remember(context) { LocalPilotPrefs(context) }
  val isCompactPhone = LocalConfiguration.current.screenHeightDp < 480

  var selectedPillarTab by remember { mutableIntStateOf(0) }
  var refreshTick by remember { mutableIntStateOf(0) }

  val rankTitle = remember(pilotLevel) { RiftProgressionCatalog.getRankTitleForLevel(pilotLevel) }
  val (nextRankLevel, nextRankTitle) =
    remember(pilotLevel) { RiftProgressionCatalog.getNextRankMilestoneInfo(pilotLevel) }
  val availableCustomPts = (totalCustomizationPoints - spentCustomizationPoints).coerceAtLeast(0)
  val isRiftFrameUnlocked = pilotLevel >= 20

  val unclaimedMilestoneShards =
    remember(pilotLevel, refreshTick) {
      val highestClaimed = localPrefs.getHighestMilestoneLevelClaimed().coerceAtLeast(1)
      RiftProgressionCatalog.calculateUnclaimedMilestoneShards(highestClaimed, pilotLevel)
    }

  var statusBanner by remember {
    mutableStateOf(
      "❖ PROGRESSION & 3D RIFT SHARD FOUNDRY • EVERY COSMETIC & RIFT FRAME BAY UNLOCKS 100% FREE BY LEVEL OR EARLY VIA ❖"
    )
  }

  fun trySpendShards(cost: Long): Boolean {
    if (cost <= 0L) return true
    if (riftShards < cost) return false
    if (onSpendRiftShards != null) {
      return onSpendRiftShards(cost)
    }
    onTopUpRiftShards(-cost)
    return true
  }

  Column(
    modifier =
      Modifier.fillMaxSize()
        .padding(8.dp)
        .testTag("rift_shards_store_modal"),
    verticalArrangement = Arrangement.spacedBy(if (isCompactPhone) 5.dp else 7.dp),
  ) {
    // 1. Career Progression & Treasury Header Row (Frameless inside NEXUS Master Window)
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text =
            if (isCompactPhone) "❖ PROGRESSION // LVL $pilotLevel $rankTitle"
            else "❖ CAREER PROGRESSION & 3D RIFT SHARD FOUNDRY // LVL $pilotLevel $rankTitle",
          color = Color(0xFFFFD700),
          fontSize = if (isCompactPhone) 12.5.sp else 15.5.sp,
          fontWeight = FontWeight.ExtraBold,
          fontFamily = FontFamily.SansSerif,
          letterSpacing = 0.8.sp,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text =
            if (pilotLevel >= 100) {
              "MAX RANK ACHIEVED: APEX ARCHON • FULL 100-PT RIFT FRAME UNLOCKED"
            } else {
              "NEXT RANK: LVL $nextRankLevel $nextRankTitle • +30❖ EVERY LEVEL • +150❖ EVERY 5 LEVELS (+300❖ AT LVL 20)"
            },
          color = Color(0xFF00F0FF),
          fontSize = if (isCompactPhone) 8.5.sp else 10.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.Monospace,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }

      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        if (unclaimedMilestoneShards > 0L) {
          Button(
            onClick = {
              val claimed = localPrefs.claimPendingMilestoneShards(pilotLevel)
              if (claimed > 0L) {
                onTopUpRiftShards(claimed)
                refreshTick++
                statusBanner = "✓ CLAIMED +${claimed}❖ LEVEL MILESTONE REWARDS TO YOUR TREASURY!"
              }
            },
            colors =
              ButtonDefaults.buttonColors(
                containerColor = Color(0xFF00E676),
                contentColor = Color(0xFF04120B),
              ),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
            modifier = Modifier.height(if (isCompactPhone) 28.dp else 32.dp),
          ) {
            Text(
              text = "CLAIM +${unclaimedMilestoneShards}❖",
              fontSize = 9.5.sp,
              fontWeight = FontWeight.ExtraBold,
              fontFamily = FontFamily.Monospace,
            )
          }
        }

        Box(
          modifier =
            Modifier.background(Color(0xFF1A1405), RoundedCornerShape(8.dp))
              .border(1.dp, Color(0xFFFFD700), RoundedCornerShape(8.dp))
              .padding(horizontal = 10.dp, vertical = 5.dp)
              .testTag("store_rift_shards_balance"),
        ) {
          Text(
            text = "❖ $riftShards RIFT SHARDS",
            color = Color(0xFFFFD700),
            fontSize = if (isCompactPhone) 11.sp else 12.5.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = FontFamily.Monospace,
          )
        }
      }
    }

    // 2. Live Career KPI Strip + Status Banner
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      StoreSummaryChip(
        modifier = Modifier.weight(1.15f),
        label = "PILOT LEVEL (1–100)",
        value =
          if (pilotLevel >= 100) {
            "LVL 100 • $rankTitle"
          } else {
            "LVL $pilotLevel ($currentLevelXp / $nextLevelXpSpan XP)"
          },
        accent = Color(0xFF00F0FF),
        progressFraction = levelProgressFraction,
      )
      StoreSummaryChip(
        modifier = Modifier.weight(1.1f),
        label = "RIFT FRAME PARITY BUDGET",
        value =
          if (isRiftFrameUnlocked) {
            "UNLOCKED: 100 / 100 PTS"
          } else {
            "LOCKS AT LVL 20 (100 PTS)"
          },
        accent = if (isRiftFrameUnlocked) Color(0xFF00E676) else Color(0xFFF59E0B),
      )
      StoreSummaryChip(
        modifier = Modifier.weight(0.95f),
        label = "PUBLIC GAME WINS",
        value = "🏆 $publicGameWins WINS ($matchPresencePct%)",
        accent = Color(0xFFFFD700),
      )
      StoreSummaryChip(
        modifier = Modifier.weight(0.95f),
        label = "FRAMES & VOUCHERS",
        value = "+$unlockedExtraSlots BAYS • $aiShipDesignTokens VOUCHERS",
        accent = Color(0xFFC084FC),
      )
    }

    Box(
      modifier =
        Modifier.fillMaxWidth()
          .background(Color(0xFF0E2238), RoundedCornerShape(7.dp))
          .border(1.dp, Color(0x5500F0FF), RoundedCornerShape(7.dp))
          .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
      Text(
        text = statusBanner,
        color = Color(0xFF00E676),
        fontSize = if (isCompactPhone) 9.sp else 10.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }

    // 3. 6-Pillar Career Progression & 3D Cosmetic Foundry Sub-Tab Bar
    Row(
      modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      val tabs =
        listOf(
          "1. LEVEL MILESTONES & GRANTS",
          "2. RIFT FRAMES (A–E) & FORGE",
          "3. KILL FINISHERS",
          "4. 3D HULL TRIMS",
          "5. WARP SIGNATURES",
          "6. HVT CROWN HALOS",
        )
      tabs.forEachIndexed { idx, label ->
        val selected = selectedPillarTab == idx
        Box(
          modifier =
            Modifier.clip(RoundedCornerShape(8.dp))
              .background(if (selected) Color(0xFFFFD700) else Color(0xCC0D1B30))
              .border(
                1.dp,
                if (selected) Color(0xFFFFD700) else Color(0x5538BDF8),
                RoundedCornerShape(8.dp),
              )
              .clickable { selectedPillarTab = idx }
              .padding(
                horizontal = if (isCompactPhone) 8.dp else 10.dp,
                vertical = if (isCompactPhone) 4.dp else 6.dp,
              )
              .testTag("progression_pillar_tab_$idx"),
        ) {
          Text(
            text = label,
            color = if (selected) Color(0xFF05080E) else Color.White,
            fontSize = if (isCompactPhone) 8.8.sp else 9.8.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = FontFamily.SansSerif,
            maxLines = 1,
          )
        }
      }
    }

    // 4. Active Pillar Body
    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
      when (selectedPillarTab) {
        0 -> {
          Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            // Left: Lv 1-100 Career Milestone Ladder
            Column(
              modifier =
                Modifier.weight(1.1f)
                  .fillMaxHeight()
                  .background(Color(0xFF0A1628), RoundedCornerShape(10.dp))
                  .border(1.dp, Color(0xFF1E3859), RoundedCornerShape(10.dp))
                  .padding(10.dp),
              verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              Text(
                text = "CAREER RANK & LEVEL MILESTONE ROADMAP (LV 1–100)",
                color = Color(0xFF00F0FF),
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.SansSerif,
              )
              val milestones =
                remember {
                  listOf(
                    Triple(10, "VANGUARD", "Obsidian Ice-Cyan Hull Trim + +180❖ Milestone"),
                    Triple(15, "VANGUARD II", "Gravity Implosion Kill Finisher + +180❖"),
                    Triple(20, "INTERCEPTOR", "UNLOCKS SHIP 10 RIFT FRAME (FULL 100 PTS) + Frame A + +330❖"),
                    Triple(30, "INTERCEPTOR III", "Sovereign Solar Gold 3D Hull Trim + +180❖"),
                    Triple(35, "PHANTOM", "Frame B (Scatter Brawler) + Solar Crown Finisher + +180❖"),
                    Triple(50, "DREADNOUGHT", "Frame C (Bank Specialist) + +180❖"),
                    Triple(70, "RIFT COMMANDER", "Commander Prestige + +180❖"),
                    Triple(75, "RIFT COMMANDER II", "Frame D (Stealth Phantom) + Crimson Nova Warp + +180❖"),
                    Triple(90, "SOVEREIGN ACE", "Sovereign Prestige + +180❖"),
                    Triple(100, "APEX ARCHON", "Frame E (Stasis Archon) + Maximum Career Mastery"),
                  )
                }
              LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(5.dp),
              ) {
                items(milestones, key = { it.first }) { (lvl, rank, rewardDesc) ->
                  val reached = pilotLevel >= lvl
                  Row(
                    modifier =
                      Modifier.fillMaxWidth()
                        .background(
                          if (reached) Color(0xCC0C2922) else Color(0x990D1B30),
                          RoundedCornerShape(8.dp),
                        )
                        .border(
                          1.dp,
                          if (lvl == 20) Color(0xFFFFD700)
                          else if (reached) Color(0xFF10B981)
                          else Color(0x3338BDF8),
                          RoundedCornerShape(8.dp),
                        )
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                  ) {
                    Column(modifier = Modifier.weight(1f)) {
                      Text(
                        text = "LVL $lvl • $rank",
                        color = if (lvl == 20) Color(0xFFFFD700) else Color.White,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace,
                      )
                      Text(
                        text = rewardDesc,
                        color = Color(0xFF94A3B8),
                        fontSize = 9.sp,
                        fontFamily = FontFamily.SansSerif,
                      )
                    }
                    Text(
                      text = if (reached) "✓ UNLOCKED" else "LVL $lvl",
                      color = if (reached) Color(0xFF00E676) else Color(0xFF38BDF8),
                      fontSize = 9.5.sp,
                      fontWeight = FontWeight.ExtraBold,
                      fontFamily = FontFamily.Monospace,
                    )
                  }
                }
              }
            }

            // Right: Treasury Grants & Progression Verification Controls
            Column(
              modifier =
                Modifier.weight(0.95f)
                  .fillMaxHeight()
                  .background(Color(0xFF0A1628), RoundedCornerShape(10.dp))
                  .border(1.dp, Color(0xFF1E3859), RoundedCornerShape(10.dp))
                  .padding(10.dp)
                  .verticalScroll(rememberScrollState()),
              verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Text(
                text = "RIFT SHARDS TREASURY GRANTS & CAREER PROGRESSION",
                color = Color(0xFFFFD700),
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.SansSerif,
              )

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                ShardPackCard(
                  modifier = Modifier.weight(1f),
                  title = "SHARD CACHE",
                  amountText = "+100❖",
                  subText = "INSTANT GRANT",
                  accent = Color(0xFF00F0FF),
                  tag = "btn_topup_100",
                  onClick = {
                    onTopUpRiftShards(100L)
                    statusBanner = "✓ CREDITED +100❖ RIFT SHARDS TO PERMANENT PILOT ACCOUNT!"
                  },
                )
                ShardPackCard(
                  modifier = Modifier.weight(1f),
                  title = "SYNDICATE VAULT",
                  amountText = "+500❖",
                  subText = "FOUNDRY PACK",
                  accent = Color(0xFF00E676),
                  tag = "btn_topup_500",
                  onClick = {
                    onTopUpRiftShards(500L)
                    statusBanner = "✓ CREDITED +500❖ RIFT SHARDS TO PERMANENT PILOT ACCOUNT!"
                  },
                )
                ShardPackCard(
                  modifier = Modifier.weight(1f),
                  title = "FOUNDRY TREASURY",
                  amountText = "+2,500❖",
                  subText = "FULL UNLOCK",
                  accent = Color(0xFFFFD700),
                  tag = "btn_topup_2500",
                  onClick = {
                    onTopUpRiftShards(2500L)
                    statusBanner = "✓ CREDITED +2,500❖ RIFT SHARDS TO PERMANENT PILOT ACCOUNT!"
                  },
                )
              }

              StoreActionRow(
                title = "🏆 VERIFY 60M PUBLIC FFA WIN (>50% PRESENCE)",
                subtitle = "Awards +100❖ Rift Shards & +1 Public Game Win",
                costLabel = "+100❖ & +1 WIN",
                accent = Color(0xFFFFD700),
                tag = "btn_simulate_ffa_win",
                onClick = {
                  onSimulateFfaWinPayout()
                  statusBanner =
                    "🏆 PUBLIC FFA VICTORY CREDITED: +100❖ RIFT SHARDS & +1 PUBLIC GAME WIN!"
                },
              )

              StoreActionRow(
                title = "⬆️ ADVANCE +1 PILOT LEVEL (LVL 1–100)",
                subtitle = "Awards +30❖ per level (+150❖ every 5th level, +300❖ at Lv 20)",
                costLabel = "+1 LVL",
                accent = Color(0xFF00F0FF),
                tag = "btn_simulate_level_up",
                onClick = {
                  onSimulatePilotLevelUp()
                  refreshTick++
                  statusBanner =
                    "★ PILOT LEVEL ADVANCED! CHECK LEVEL MILESTONES FOR SHARD & COSMETIC UNLOCKS."
                },
              )
            }
          }
        }

        1 -> {
          // Pillar 2: Rift Frame Bays (Frame A through Frame E) & Modular Forge Vouchers
          Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Column(
              modifier =
                Modifier.weight(1.15f)
                  .fillMaxHeight()
                  .background(Color(0xFF0A1628), RoundedCornerShape(10.dp))
                  .border(1.dp, Color(0xFF1E3859), RoundedCornerShape(10.dp))
                  .padding(10.dp),
              verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              Text(
                text = "SHIP 10 RIFT FRAME LOADOUT BAYS (FRAME A – FRAME E • FULL 100-PT PARITY)",
                color = Color(0xFFFFD700),
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.SansSerif,
              )
              LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                items(RiftProgressionCatalog.frameSlotGates, key = { it.slotId }) { gate ->
                  val unlocked =
                    remember(pilotLevel, unlockedExtraSlots, refreshTick) {
                      if (gate.slotId == "Frame A") pilotLevel >= 20
                      else localPrefs.isCustomSlotUnlocked(gate.slotId) || pilotLevel >= gate.freeAtLevel
                    }
                  Row(
                    modifier =
                      Modifier.fillMaxWidth()
                        .background(
                          if (unlocked) Color(0xCC0D2821) else Color(0x990D1B30),
                          RoundedCornerShape(8.dp),
                        )
                        .border(
                          1.dp,
                          if (unlocked) Color(0xFF10B981) else Color(0x55FFD700),
                          RoundedCornerShape(8.dp),
                        )
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                  ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                      Text(
                        text = "${gate.slotId.uppercase()} // ${gate.archetypeTitle}",
                        color = if (unlocked) Color(0xFF00E676) else Color(0xFFFFD700),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace,
                      )
                      Text(
                        text = gate.summary,
                        color = Color(0xFF94A3B8),
                        fontSize = 9.sp,
                        fontFamily = FontFamily.SansSerif,
                      )
                    }
                    if (unlocked) {
                      Box(
                        modifier =
                          Modifier.clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF063929))
                            .border(1.dp, Color(0xFF00E676), RoundedCornerShape(6.dp))
                            .clickable {
                              localPrefs.setActiveCustomSlotId(gate.slotId)
                              onOpenHangarShip10()
                            }
                            .padding(horizontal = 9.dp, vertical = 5.dp),
                      ) {
                        Text(
                          text = "✓ READY",
                          color = Color(0xFF00E676),
                          fontSize = 9.5.sp,
                          fontWeight = FontWeight.ExtraBold,
                          fontFamily = FontFamily.Monospace,
                        )
                      }
                    } else if (gate.shardCost > 0L) {
                      Box(
                        modifier =
                          Modifier.clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF281E07))
                            .border(1.dp, Color(0xFFFFD700), RoundedCornerShape(6.dp))
                            .clickable {
                              if (trySpendShards(gate.shardCost)) {
                                localPrefs.setCustomSlotUnlocked(gate.slotId, true)
                                refreshTick++
                                statusBanner =
                                  "✓ UNLOCKED ${gate.slotId.uppercase()} (-${gate.shardCost}❖ RIFT SHARDS)!"
                              } else {
                                statusBanner =
                                  "⚠️ NEED ${gate.shardCost}❖ RIFT SHARDS OR REACH LVL ${gate.freeAtLevel} FOR FREE."
                              }
                            }
                            .padding(horizontal = 9.dp, vertical = 5.dp),
                      ) {
                        Text(
                          text = "UNLOCK ${gate.shardCost}❖",
                          color = Color(0xFFFFD700),
                          fontSize = 9.5.sp,
                          fontWeight = FontWeight.ExtraBold,
                          fontFamily = FontFamily.Monospace,
                        )
                      }
                    } else {
                      Text(
                        text = "FREE AT LVL ${gate.freeAtLevel}",
                        color = Color(0xFF38BDF8),
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace,
                      )
                    }
                  }
                }
              }
            }

            Column(
              modifier =
                Modifier.weight(0.9f)
                  .fillMaxHeight()
                  .background(Color(0xFF0A1628), RoundedCornerShape(10.dp))
                  .border(1.dp, Color(0xFF1E3859), RoundedCornerShape(10.dp))
                  .padding(10.dp)
                  .verticalScroll(rememberScrollState()),
              verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Text(
                text = "MODULAR TUNING, RESPEC & HULL FORGE TOOLS",
                color = Color(0xFFC084FC),
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.SansSerif,
              )

              StoreActionRow(
                title = "🛸 UNLOCK NEXT RIFT FRAME BAY (+$unlockedExtraSlots UNLOCKED)",
                subtitle = "Unlocks the next locked Rift Frame bay (Frame B – Frame E)",
                costLabel = "NEXT BAY",
                accent = Color(0xFFFFD700),
                tag = "btn_unlock_extra_slot",
                onClick = {
                  if (onUnlockExtraSlot()) {
                    refreshTick++
                    statusBanner = "✓ UNLOCKED NEXT RIFT FRAME LOADOUT BAY!"
                  } else {
                    statusBanner =
                      "⚠️ INSUFFICIENT RIFT SHARDS OR ALL RIFT FRAME BAYS (A–E) ARE ALREADY UNLOCKED."
                  }
                },
              )

              StoreActionRow(
                title = "🔄 FULL CUSTOMIZATION POINT RESPEC",
                subtitle = "Refunds all $spentCustomizationPoints spent points back to Available ($availableCustomPts avail)",
                costLabel = "50❖ SHARDS",
                accent = Color(0xFF38BDF8),
                tag = "btn_respec_custom_pts",
                onClick = {
                  if (onRespecCustomizationPoints()) {
                    statusBanner =
                      "✓ RESPEC COMPLETE (-50❖): ALL $totalCustomizationPoints CUSTOMIZATION POINTS REFUNDED!"
                  } else {
                    statusBanner =
                      "⚠️ NEED 50❖ RIFT SHARDS & AT LEAST 1 SPENT CUSTOMIZATION POINT."
                  }
                },
              )

              StoreActionRow(
                title = "🛠️ ALLOCATE +1 TUNING POINT ($availableCustomPts AVAIL)",
                subtitle = "Allocates 1 earned tuning point ($spentCustomizationPoints / $totalCustomizationPoints allocated)",
                costLabel = "1 PT",
                accent = Color(0xFF00E676),
                tag = "btn_allocate_custom_pt",
                onClick = {
                  if (onAllocateCustomizationPoint()) {
                    statusBanner = "✓ ALLOCATED 1 TUNING POINT TO ACTIVE HULL SPEC!"
                  } else {
                    statusBanner = "⚠️ NO UNSPENT TUNING POINTS AVAILABLE — LEVEL UP OR RESPEC!"
                  }
                },
              )

              StoreActionRow(
                title = "🎨 HULL FORGE SYNTHESIS VOUCHER ($aiShipDesignTokens HELD)",
                subtitle = "Acquires an on-device procedural 3D Hull Forge voucher",
                costLabel = "150❖ SHARDS",
                accent = Color(0xFFC084FC),
                tag = "btn_buy_ai_design_token",
                onClick = {
                  if (onPurchaseAiDesignToken()) {
                    statusBanner = "✓ ACQUIRED HULL FORGE VOUCHER (-150❖ RIFT SHARDS)!"
                  } else {
                    statusBanner = "⚠️ INSUFFICIENT RIFT SHARDS (REQUIRES 150❖)."
                  }
                },
              )
            }
          }
        }

        2 ->
          CosmeticPillarGrid(
            title = "3D KILL FINISHERS — DETONATION & EXECUTIONER VFX",
            categoryKey = "KILL_FINISHER",
            items = RiftProgressionCatalog.killFinishers,
            pilotLevel = pilotLevel,
            localPrefs = localPrefs,
            refreshTick = refreshTick,
            onTrySpendShards = ::trySpendShards,
            onRefresh = { refreshTick++ },
            onBanner = { statusBanner = it },
          )

        3 ->
          CosmeticPillarGrid(
            title = "3D HULL TRIMS — CHASSIS BEVEL & REACTOR SEAM FINISHES",
            categoryKey = "HULL_TRIM",
            items = RiftProgressionCatalog.hullTrims,
            pilotLevel = pilotLevel,
            localPrefs = localPrefs,
            refreshTick = refreshTick,
            onTrySpendShards = ::trySpendShards,
            onRefresh = { refreshTick++ },
            onBanner = { statusBanner = it },
          )

        4 ->
          CosmeticPillarGrid(
            title = "SECTOR WARP-IN SIGNATURES — ARRIVAL APERTURE VFX",
            categoryKey = "WARP_SIGNATURE",
            items = RiftProgressionCatalog.warpSignatures,
            pilotLevel = pilotLevel,
            localPrefs = localPrefs,
            refreshTick = refreshTick,
            onTrySpendShards = ::trySpendShards,
            onRefresh = { refreshTick++ },
            onBanner = { statusBanner = it },
          )

        else ->
          CosmeticPillarGrid(
            title = "HVT CROWN HALOS — #1 SECTOR BOUNTY LEADER RINGS",
            categoryKey = "HVT_HALO",
            items = RiftProgressionCatalog.hvtHalos,
            pilotLevel = pilotLevel,
            localPrefs = localPrefs,
            refreshTick = refreshTick,
            onTrySpendShards = ::trySpendShards,
            onRefresh = { refreshTick++ },
            onBanner = { statusBanner = it },
          )
      }
    }
  }
}

@Composable
private fun CosmeticPillarGrid(
  title: String,
  categoryKey: String,
  items: List<CosmeticCatalogItem>,
  pilotLevel: Int,
  localPrefs: LocalPilotPrefs,
  refreshTick: Int,
  onTrySpendShards: (Long) -> Boolean,
  onRefresh: () -> Unit,
  onBanner: (String) -> Unit,
) {
  val equippedId =
    remember(categoryKey, refreshTick) { localPrefs.getEquippedCosmeticId(categoryKey) }

  Column(
    modifier =
      Modifier.fillMaxSize()
        .background(Color(0xFF0A1628), RoundedCornerShape(10.dp))
        .border(1.dp, Color(0xFF1E3859), RoundedCornerShape(10.dp))
        .padding(10.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = title,
        color = Color(0xFF00F0FF),
        fontSize = 11.5.sp,
        fontWeight = FontWeight.ExtraBold,
        fontFamily = FontFamily.SansSerif,
      )
      Text(
        text = "ZERO COMBAT ADVANTAGE • 100% COSMETIC",
        color = Color(0xFF94A3B8),
        fontSize = 9.5.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace,
      )
    }

    LazyColumn(
      modifier = Modifier.weight(1f).fillMaxWidth(),
      verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      items(items, key = { it.id }) { item ->
        val accentColor = Color(item.accentColorHex)
        val isUnlocked =
          remember(item.id, pilotLevel, refreshTick) {
            localPrefs.isCosmeticUnlocked(categoryKey, item.id, pilotLevel)
          }
        val isEquipped = equippedId == item.id

        Row(
          modifier =
            Modifier.fillMaxWidth()
              .background(
                if (isEquipped) Color(0xCC112A3E) else Color(0x990D1B30),
                RoundedCornerShape(9.dp),
              )
              .border(
                1.2.dp,
                if (isEquipped) accentColor else accentColor.copy(alpha = 0.4f),
                RoundedCornerShape(9.dp),
              )
              .padding(horizontal = 12.dp, vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Row(
            modifier = Modifier.weight(1f).padding(end = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Box(
              modifier =
                Modifier.size(22.dp)
                  .clip(CircleShape)
                  .background(accentColor.copy(alpha = 0.25f))
                  .border(1.5.dp, accentColor, CircleShape)
            )
            Column {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Text(
                  text = item.name.uppercase(),
                  color = Color.White,
                  fontSize = 12.sp,
                  fontWeight = FontWeight.ExtraBold,
                  fontFamily = FontFamily.SansSerif,
                )
                Text(
                  text =
                    if (item.freeAtLevel <= 1) "DEFAULT UNLOCK"
                    else "FREE AT LVL ${item.freeAtLevel}",
                  color = accentColor,
                  fontSize = 9.5.sp,
                  fontWeight = FontWeight.Bold,
                  fontFamily = FontFamily.Monospace,
                )
              }
              Text(
                text = item.summary,
                color = Color(0xFF94A3B8),
                fontSize = 10.sp,
                fontFamily = FontFamily.SansSerif,
              )
            }
          }

          if (isEquipped) {
            Box(
              modifier =
                Modifier.background(Color(0xFF063929), RoundedCornerShape(7.dp))
                  .border(1.dp, Color(0xFF00E676), RoundedCornerShape(7.dp))
                  .padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
              Text(
                text = "✓ EQUIPPED",
                color = Color(0xFF00E676),
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
              )
            }
          } else if (isUnlocked) {
            Button(
              onClick = {
                localPrefs.setEquippedCosmeticId(categoryKey, item.id)
                onRefresh()
                onBanner("✓ EQUIPPED ${item.name.uppercase()}!")
              },
              colors =
                ButtonDefaults.buttonColors(
                  containerColor = accentColor,
                  contentColor = Color(0xFF05080E),
                ),
              shape = RoundedCornerShape(7.dp),
              contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
              modifier = Modifier.height(32.dp),
            ) {
              Text(
                text = "EQUIP",
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
              )
            }
          } else {
            Button(
              onClick = {
                if (onTrySpendShards(item.shardCost)) {
                  localPrefs.setCosmeticUnlocked(categoryKey, item.id, true)
                  localPrefs.setEquippedCosmeticId(categoryKey, item.id)
                  onRefresh()
                  onBanner("✓ UNLOCKED & EQUIPPED ${item.name.uppercase()} (-${item.shardCost}❖)!")
                } else {
                  onBanner(
                    "⚠️ NEED ${item.shardCost}❖ FOR EARLY UNLOCK (OR REACH LVL ${item.freeAtLevel} FREE)."
                  )
                }
              },
              colors =
                ButtonDefaults.buttonColors(
                  containerColor = Color(0xFF281E07),
                  contentColor = Color(0xFFFFD700),
                ),
              border = BorderStroke(1.dp, Color(0xFFFFD700)),
              shape = RoundedCornerShape(7.dp),
              contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
              modifier = Modifier.height(32.dp),
            ) {
              Text(
                text = "UNLOCK ${item.shardCost}❖",
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

@Composable
private fun StoreSummaryChip(
  modifier: Modifier = Modifier,
  label: String,
  value: String,
  accent: Color,
  progressFraction: Float? = null,
) {
  Column(
    modifier =
      modifier
        .background(Color(0xFF0E1B2E), RoundedCornerShape(8.dp))
        .border(1.dp, accent.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
        .padding(horizontal = 8.dp, vertical = 5.dp),
  ) {
    Text(
      text = label,
      color = Color(0xFF8A99AD),
      fontSize = 8.5.sp,
      fontWeight = FontWeight.Bold,
      fontFamily = FontFamily.SansSerif,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
    Text(
      text = value,
      color = accent,
      fontSize = 10.8.sp,
      fontWeight = FontWeight.ExtraBold,
      fontFamily = FontFamily.Monospace,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
    if (progressFraction != null) {
      Spacer(modifier = Modifier.height(3.dp))
      Box(
        modifier =
          Modifier.fillMaxWidth()
            .height(4.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(Color(0xFF162C46))
      ) {
        Box(
          modifier =
            Modifier.fillMaxWidth(progressFraction.coerceIn(0f, 1f))
              .fillMaxHeight()
              .background(accent, RoundedCornerShape(2.dp))
        )
      }
    }
  }
}

@Composable
private fun ShardPackCard(
  modifier: Modifier = Modifier,
  title: String,
  amountText: String,
  subText: String,
  accent: Color,
  tag: String,
  onClick: () -> Unit,
) {
  Column(
    modifier =
      modifier
        .clip(RoundedCornerShape(8.dp))
        .background(Color(0xFF10233E))
        .border(1.2.dp, accent, RoundedCornerShape(8.dp))
        .clickable { onClick() }
        .padding(horizontal = 8.dp, vertical = 8.dp)
        .testTag(tag),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Text(
      text = title,
      color = Color(0xFF8A99AD),
      fontSize = 8.5.sp,
      fontWeight = FontWeight.Bold,
      fontFamily = FontFamily.SansSerif,
      maxLines = 1,
    )
    Text(
      text = amountText,
      color = accent,
      fontSize = 15.sp,
      fontWeight = FontWeight.ExtraBold,
      fontFamily = FontFamily.Monospace,
    )
    Text(
      text = subText,
      color = Color.White,
      fontSize = 8.sp,
      fontWeight = FontWeight.Bold,
      fontFamily = FontFamily.Monospace,
      maxLines = 1,
    )
  }
}

@Composable
private fun StoreActionRow(
  title: String,
  subtitle: String,
  costLabel: String,
  accent: Color,
  tag: String,
  onClick: () -> Unit,
) {
  Row(
    modifier =
      Modifier.fillMaxWidth()
        .clip(RoundedCornerShape(8.dp))
        .background(Color(0xFF0F2038))
        .border(1.dp, accent.copy(alpha = 0.65f), RoundedCornerShape(8.dp))
        .clickable { onClick() }
        .padding(horizontal = 10.dp, vertical = 6.dp)
        .testTag(tag),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
      Text(
        text = title,
        color = Color.White,
        fontSize = 10.5.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.SansSerif,
      )
      Text(
        text = subtitle,
        color = Color(0xFF94A3B8),
        fontSize = 8.8.sp,
        fontFamily = FontFamily.SansSerif,
      )
    }
    Box(
      modifier =
        Modifier.background(accent.copy(alpha = 0.18f), RoundedCornerShape(6.dp))
          .border(1.dp, accent, RoundedCornerShape(6.dp))
          .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
      Text(
        text = costLabel,
        color = accent,
        fontSize = 9.5.sp,
        fontWeight = FontWeight.ExtraBold,
        fontFamily = FontFamily.Monospace,
      )
    }
  }
}
