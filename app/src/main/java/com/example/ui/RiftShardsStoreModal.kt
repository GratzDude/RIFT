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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
  onCloseModal: () -> Unit,
) {
  var statusBanner by remember {
    mutableStateOf(
      "❖ RIFT SHARDS TREASURY ONLINE • EARN +100❖ PER QUALIFYING FFA WIN (>50% PRESENCE) & +50❖ EVERY 5 PILOT LEVELS"
    )
  }
  val availableCustomPts = (totalCustomizationPoints - spentCustomizationPoints).coerceAtLeast(0)

  Box(
    modifier =
      Modifier.fillMaxSize()
        .background(Color(0xCC000000))
        .padding(horizontal = 24.dp, vertical = 12.dp)
        .testTag("rift_shards_store_modal"),
    contentAlignment = Alignment.Center,
  ) {
    Surface(
      modifier =
        Modifier.fillMaxSize()
          .border(1.2.dp, Color(0xFFFFD700), RoundedCornerShape(16.dp)),
      color = Color(0xF208101C),
      shape = RoundedCornerShape(16.dp),
    ) {
      Column(
        modifier = Modifier.fillMaxSize().padding(14.dp),
        verticalArrangement = Arrangement.SpaceBetween,
      ) {
        // 1. Header Row
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Column {
            Text(
              text = "❖ RIFT SHARDS TREASURY & CUSTOMIZATION FOUNDRY",
              color = Color(0xFFFFD700),
              fontSize = 17.sp,
              fontWeight = FontWeight.ExtraBold,
              fontFamily = FontFamily.SansSerif,
              letterSpacing = 1.1.sp,
            )
            Text(
              text = "PERMANENT UID CURRENCY • 100❖ PER FFA WIN (>50% PRESENCE) • 50❖ EVERY 5 PILOT LEVELS",
              color = Color(0xFF00F0FF),
              fontSize = 10.5.sp,
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.Monospace,
            )
          }

          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Box(
              modifier =
                Modifier.background(Color(0xFF1A1405), RoundedCornerShape(8.dp))
                  .border(1.dp, Color(0xFFFFD700), RoundedCornerShape(8.dp))
                  .padding(horizontal = 12.dp, vertical = 6.dp)
                  .testTag("store_rift_shards_balance"),
            ) {
              Text(
                text = "❖ $riftShards RIFT SHARDS",
                color = Color(0xFFFFD700),
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
              )
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
              modifier = Modifier.height(36.dp).testTag("rift_shards_store_close_button"),
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

        // 2. Status Banner + Live Telemetry Bar
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Box(
            modifier =
              Modifier.fillMaxWidth()
                .background(Color(0xFF0E2238), RoundedCornerShape(8.dp))
                .border(1.dp, Color(0x6600F0FF), RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp),
          ) {
            Text(
              text = statusBanner,
              color = Color(0xFF00E676),
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.Monospace,
            )
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            StoreSummaryChip(
              modifier = Modifier.weight(1f),
              label = "PILOT LEVEL (1–100)",
              value =
                if (pilotLevel >= 100) {
                  "LVL 100 (MAX)"
                } else {
                  "LVL $pilotLevel ($currentLevelXp / $nextLevelXpSpan XP)"
                },
              accent = Color(0xFF00F0FF),
              progressFraction = levelProgressFraction,
            )
            StoreSummaryChip(
              modifier = Modifier.weight(1f),
              label = "CUSTOMIZATION POINTS",
              value = "$availableCustomPts AVAIL ($totalCustomizationPoints / 100 EARNED)",
              accent = Color(0xFF00E676),
            )
            StoreSummaryChip(
              modifier = Modifier.weight(1f),
              label = "PUBLIC GAME WINS",
              value = "🏆 $publicGameWins WINS ($matchPresencePct% PRESENCE)",
              accent = Color(0xFFFFD700),
            )
            StoreSummaryChip(
              modifier = Modifier.weight(1f),
              label = "SLOTS & AI VOUCHERS",
              value = "+$unlockedExtraSlots SLOTS • $aiShipDesignTokens AI PASS",
              accent = Color(0xFFC084FC),
            )
          }
        }

        // 3. Simulated Top-Up Packs & Foundry Unlocks (2 Columns)
        Row(
          modifier = Modifier.fillMaxWidth().weight(1f).padding(vertical = 6.dp),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          // Left Column: Simulated Instant Shard Top-Ups + Live Progression/FFA Win Simulators
          Column(
            modifier =
              Modifier.weight(1f)
                .fillMaxHeight()
                .background(Color(0xFF0A1628), RoundedCornerShape(10.dp))
                .border(1.dp, Color(0xFF1E3859), RoundedCornerShape(10.dp))
                .padding(10.dp),
            verticalArrangement = Arrangement.SpaceBetween,
          ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
              Text(
                text = "1. SIMULATED RIFT SHARDS TOP-UP PACKS (FREE TEST GRANTS)",
                color = Color(0xFFFFD700),
                fontSize = 11.5.sp,
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
                  subText = "POPULAR TEST",
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
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
              Text(
                text = "2. LIVE FFA MATCH WIN (>50% PRESENCE) & LEVEL-UP VERIFICATION",
                color = Color(0xFF00F0FF),
                fontSize = 11.5.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.SansSerif,
              )

              StoreActionRow(
                title = "🏆 SIMULATE 60M PUBLIC FFA WIN (>50% PRESENCE)",
                subtitle = "Verifies >30m/60m presence rule -> Awards +100❖ Shards & +1 Public Game Win",
                costLabel = "+100❖ & +1 WIN",
                accent = Color(0xFFFFD700),
                tag = "btn_simulate_ffa_win",
                onClick = {
                  onSimulateFfaWinPayout()
                  statusBanner =
                    "🏆 PUBLIC FFA VICTORY VERIFIED (>50% PRESENCE): +100❖ RIFT SHARDS & +1 PUBLIC GAME WIN!"
                },
              )

              StoreActionRow(
                title = "⬆️ SIMULATE +1 PILOT LEVEL UP (LVL 1–100)",
                subtitle = "Grants +1 Customization Point & +50❖ Bonus Shards every 5th Level",
                costLabel = "+1 LVL / +1 PT",
                accent = Color(0xFF00F0FF),
                tag = "btn_simulate_level_up",
                onClick = {
                  onSimulatePilotLevelUp()
                  statusBanner =
                    "★ PILOT LEVEL ADVANCED! EARNED +1 CUSTOMIZATION POINT (5-LVL MILESTONES AWARD +50❖)."
                },
              )
            }
          }

          // Right Column: Respecs, Extra Hull Slots, AI Ship Design Synthesis & Point Allocation
          Column(
            modifier =
              Modifier.weight(1f)
                .fillMaxHeight()
                .background(Color(0xFF0A1628), RoundedCornerShape(10.dp))
                .border(1.dp, Color(0xFF1E3859), RoundedCornerShape(10.dp))
                .padding(10.dp),
            verticalArrangement = Arrangement.SpaceBetween,
          ) {
            Text(
              text = "3. CUSTOMIZATION RESPEC, EXTRA SLOTS & AI SHIP DESIGN",
              color = Color(0xFFC084FC),
              fontSize = 11.5.sp,
              fontWeight = FontWeight.ExtraBold,
              fontFamily = FontFamily.SansSerif,
            )

            StoreActionRow(
              title = "⚡ ALLOCATE +1 CUSTOMIZATION POINT ($availableCustomPts AVAIL)",
              subtitle = "Spend 1 earned Level Point on hull tuning ($spentCustomizationPoints / $totalCustomizationPoints allocated)",
              costLabel = "1 CUSTOM PT",
              accent = Color(0xFF00E676),
              tag = "btn_allocate_custom_pt",
              onClick = {
                if (onAllocateCustomizationPoint()) {
                  statusBanner = "✓ ALLOCATED 1 CUSTOMIZATION POINT TO ACTIVE HULL SPEC!"
                } else {
                  statusBanner = "⚠️ NO UNSPENT CUSTOMIZATION POINTS AVAILABLE — LEVEL UP OR RESPEC!"
                }
              },
            )

            StoreActionRow(
              title = "🔄 FULL CUSTOMIZATION POINT RESPEC",
              subtitle = "Refunds all $spentCustomizationPoints spent Customization Points back to Available pool",
              costLabel = "50❖ SHARDS",
              accent = Color(0xFF38BDF8),
              tag = "btn_respec_custom_pts",
              onClick = {
                if (onRespecCustomizationPoints()) {
                  statusBanner =
                    "✓ RESPEC COMPLETE (-50❖): ALL $totalCustomizationPoints CUSTOMIZATION POINTS REFUNDED!"
                } else {
                  statusBanner =
                    "⚠️ CANNOT RESPEC: NEED 50❖ RIFT SHARDS & AT LEAST 1 SPENT CUSTOMIZATION POINT."
                }
              },
            )

            StoreActionRow(
              title = "🛸 UNLOCK EXTRA CUSTOM HULL BAY SLOT (+$unlockedExtraSlots UNLOCKED)",
              subtitle = "Unlocks Slot 9B or Slot 9C on the 9th Custom Modular Chassis",
              costLabel = "300❖ SHARDS",
              accent = Color(0xFFFFD700),
              tag = "btn_unlock_extra_slot",
              onClick = {
                if (onUnlockExtraSlot()) {
                  statusBanner = "✓ UNLOCKED EXTRA CUSTOM HULL BAY SLOT (-300❖ RIFT SHARDS)!"
                } else {
                  statusBanner = "⚠️ INSUFFICIENT RIFT SHARDS OR ALL SLOTS (9A/9B/9C) UNLOCKED (300❖)."
                }
              },
            )

            StoreActionRow(
              title = "🤖 AI SHIP DESIGN SYNTHESIS VOUCHER ($aiShipDesignTokens HELD)",
              subtitle = "Reserves an AI Ship Design synthesis voucher for custom hull fabrication",
              costLabel = "150❖ SHARDS",
              accent = Color(0xFFC084FC),
              tag = "btn_buy_ai_design_token",
              onClick = {
                if (onPurchaseAiDesignToken()) {
                  statusBanner = "✓ ACQUIRED AI SHIP DESIGN SYNTHESIS VOUCHER (-150❖ RIFT SHARDS)!"
                } else {
                  statusBanner = "⚠️ INSUFFICIENT RIFT SHARDS (REQUIRES 150❖ — TAP A TOP-UP PACK)."
                }
              },
            )
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
      fontSize = 9.sp,
      fontWeight = FontWeight.Bold,
      fontFamily = FontFamily.SansSerif,
    )
    Text(
      text = value,
      color = accent,
      fontSize = 11.5.sp,
      fontWeight = FontWeight.ExtraBold,
      fontFamily = FontFamily.Monospace,
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
      fontSize = 9.sp,
      fontWeight = FontWeight.Bold,
      fontFamily = FontFamily.SansSerif,
    )
    Text(
      text = amountText,
      color = accent,
      fontSize = 16.sp,
      fontWeight = FontWeight.ExtraBold,
      fontFamily = FontFamily.Monospace,
    )
    Text(
      text = subText,
      color = Color.White,
      fontSize = 8.5.sp,
      fontWeight = FontWeight.Bold,
      fontFamily = FontFamily.Monospace,
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
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.SansSerif,
      )
      Text(
        text = subtitle,
        color = Color(0xFF94A3B8),
        fontSize = 9.sp,
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
        fontSize = 10.sp,
        fontWeight = FontWeight.ExtraBold,
        fontFamily = FontFamily.Monospace,
      )
    }
  }
}
