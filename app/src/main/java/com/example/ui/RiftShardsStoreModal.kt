package com.example.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
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
 val (nextRankLevel, nextRankTitle) = remember(pilotLevel) { RiftProgressionCatalog.getNextRankMilestoneInfo(pilotLevel) }
 val availableCustomPts = (totalCustomizationPoints - spentCustomizationPoints).coerceAtLeast(0)
 val isRiftFrameUnlocked = pilotLevel >= 20

 val unclaimedMilestoneShards = remember(pilotLevel, refreshTick) {
  val highestClaimed = localPrefs.getHighestMilestoneLevelClaimed().coerceAtLeast(1)
  RiftProgressionCatalog.calculateUnclaimedMilestoneShards(highestClaimed, pilotLevel)
 }

 var statusBanner by remember {
  mutableStateOf("❖ PROGRESSION & 3D RIFT SHARD FOUNDRY • EVERY COSMETIC & RIFT FRAME BAY UNLOCKS 100% FREE BY LEVEL OR EARLY VIA ❖")
 }

 fun trySpendShards(cost: Long): Boolean {
  if (cost <= 0L) return true
  if (riftShards < cost) return false
  if (onSpendRiftShards != null) return onSpendRiftShards(cost)
  onTopUpRiftShards(-cost)
  return true
 }

 Column(
  modifier = Modifier.fillMaxSize().padding(8.dp).testTag("rift_shards_store_modal"),
  verticalArrangement = Arrangement.spacedBy(if (isCompactPhone) 5.dp else 7.dp),
 ) {
  Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
   Column(modifier = Modifier.weight(1f)) {
    Text(
     if (isCompactPhone) "❖ PROGRESSION // LVL $pilotLevel $rankTitle" else "❖ CAREER PROGRESSION & 3D RIFT SHARD FOUNDRY // LVL $pilotLevel $rankTitle",
     color = Color(0xFFFFD700), fontSize = if (isCompactPhone) 12.5.sp else 15.5.sp, fontWeight = FontWeight.ExtraBold,
     letterSpacing = 0.8.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
    )
    Text(
     if (pilotLevel >= 100) "MAX RANK ACHIEVED: APEX ARCHON • FULL 100-PT RIFT FRAME UNLOCKED"
     else "NEXT RANK: LVL $nextRankLevel $nextRankTitle • +30❖ EVERY LEVEL • +150❖ EVERY 5 LEVELS (+300❖ AT LVL 20)",
     color = Color(0xFF00F0FF), fontSize = if (isCompactPhone) 8.5.sp else 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, maxLines = 1, overflow = TextOverflow.Ellipsis,
    )
   }

   Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
      colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676), contentColor = Color(0xFF04120B)),
      shape = RoundedCornerShape(8.dp), contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
      modifier = Modifier.height(if (isCompactPhone) 28.dp else 32.dp),
     ) { Text("CLAIM +${unclaimedMilestoneShards}❖", fontSize = 9.5.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace) }
    }

    Box(
     modifier = Modifier.background(Color(0xFF1A1405), RoundedCornerShape(8.dp))
      .border(1.dp, Color(0xFFFFD700), RoundedCornerShape(8.dp))
      .padding(horizontal = 10.dp, vertical = 5.dp).testTag("store_rift_shards_balance"),
    ) {
     Text("❖ $riftShards RIFT SHARDS", color = Color(0xFFFFD700), fontSize = if (isCompactPhone) 11.sp else 12.5.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace)
    }
   }
  }

  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
   StoreSummaryChip(
    Modifier.weight(1.15f), "PILOT LEVEL (1–100)",
    if (pilotLevel >= 100) "LVL 100 • $rankTitle" else "LVL $pilotLevel ($currentLevelXp / $nextLevelXpSpan XP)",
    Color(0xFF00F0FF), levelProgressFraction,
   )
   StoreSummaryChip(
    Modifier.weight(1.1f), "RIFT FRAME PARITY BUDGET",
    if (isRiftFrameUnlocked) "UNLOCKED: 100 / 100 PTS" else "LOCKS AT LVL 20 (100 PTS)",
    if (isRiftFrameUnlocked) Color(0xFF00E676) else Color(0xFFF59E0B),
   )
   StoreSummaryChip(Modifier.weight(0.95f), "PUBLIC GAME WINS", "🏆 $publicGameWins WINS ($matchPresencePct%)", Color(0xFFFFD700))
   StoreSummaryChip(Modifier.weight(0.95f), "FRAMES & VOUCHERS", "+$unlockedExtraSlots BAYS • $aiShipDesignTokens VOUCHERS", Color(0xFFC084FC))
  }

  Box(
   modifier = Modifier.fillMaxWidth().background(Color(0xFF0E2238), RoundedCornerShape(7.dp))
    .border(1.dp, Color(0x5500F0FF), RoundedCornerShape(7.dp)).padding(horizontal = 10.dp, vertical = 4.dp),
  ) {
   Text(statusBanner, color = Color(0xFF00E676), fontSize = if (isCompactPhone) 9.sp else 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, maxLines = 1, overflow = TextOverflow.Ellipsis)
  }

  Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
   val tabs = listOf(
    "1. LEVEL MILESTONES & GRANTS", "2. RIFT FRAMES (A–E) & FORGE",
    "3. KILL FINISHERS", "4. 3D HULL TRIMS", "5. WARP SIGNATURES", "6. HVT CROWN HALOS",
   )
   tabs.forEachIndexed { idx, label ->
    val selected = selectedPillarTab == idx
    Box(
     modifier = Modifier.clip(RoundedCornerShape(8.dp))
      .background(if (selected) Color(0xFFFFD700) else Color(0xCC0D1B30))
      .border(1.dp, if (selected) Color(0xFFFFD700) else Color(0x5538BDF8), RoundedCornerShape(8.dp))
      .clickable { selectedPillarTab = idx }
      .padding(horizontal = if (isCompactPhone) 8.dp else 10.dp, vertical = if (isCompactPhone) 4.dp else 6.dp)
      .testTag("progression_pillar_tab_$idx"),
    ) {
     Text(label, color = if (selected) Color(0xFF05080E) else Color.White, fontSize = if (isCompactPhone) 8.8.sp else 9.8.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
    }
   }
  }

  Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
   when (selectedPillarTab) {
    0 -> {
     Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
      Column(
       modifier = Modifier.weight(1.1f).fillMaxHeight().background(Color(0xFF0A1628), RoundedCornerShape(10.dp))
        .border(1.dp, Color(0xFF1E3859), RoundedCornerShape(10.dp)).padding(10.dp),
       verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
       Text("CAREER RANK & LEVEL MILESTONE ROADMAP (LV 1–100)", color = Color(0xFF00F0FF), fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
       val milestones = remember {
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
       LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(5.dp)) {
        items(milestones, key = { it.first }) { (lvl, rank, rewardDesc) ->
         val reached = pilotLevel >= lvl
         Row(
          modifier = Modifier.fillMaxWidth()
           .background(if (reached) Color(0xCC0C2922) else Color(0x990D1B30), RoundedCornerShape(8.dp))
           .border(1.dp, if (lvl == 20) Color(0xFFFFD700) else if (reached) Color(0xFF10B981) else Color(0x3338BDF8), RoundedCornerShape(8.dp))
           .padding(horizontal = 10.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween,
         ) {
          Column(modifier = Modifier.weight(1f)) {
           Text("LVL $lvl • $rank", color = if (lvl == 20) Color(0xFFFFD700) else Color.White, fontSize = 10.5.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace)
           Text(rewardDesc, color = Color(0xFF94A3B8), fontSize = 9.sp)
          }
          Text(if (reached) "✓ UNLOCKED" else "LVL $lvl", color = if (reached) Color(0xFF00E676) else Color(0xFF38BDF8), fontSize = 9.5.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace)
         }
        }
       }
      }

      Column(
       modifier = Modifier.weight(0.95f).fillMaxHeight().background(Color(0xFF0A1628), RoundedCornerShape(10.dp))
        .border(1.dp, Color(0xFF1E3859), RoundedCornerShape(10.dp)).padding(10.dp).verticalScroll(rememberScrollState()),
       verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
       Text("RIFT SHARDS TREASURY GRANTS & CAREER PROGRESSION", color = Color(0xFFFFD700), fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
       Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        ShardPackCard(Modifier.weight(1f), "SHARD CACHE", "+100❖", "INSTANT GRANT", Color(0xFF00F0FF), "btn_topup_100") {
         onTopUpRiftShards(100L); statusBanner = "✓ CREDITED +100❖ RIFT SHARDS TO PERMANENT PILOT ACCOUNT!"
        }
        ShardPackCard(Modifier.weight(1f), "SYNDICATE VAULT", "+500❖", "FOUNDRY PACK", Color(0xFF00E676), "btn_topup_500") {
         onTopUpRiftShards(500L); statusBanner = "✓ CREDITED +500❖ RIFT SHARDS TO PERMANENT PILOT ACCOUNT!"
        }
        ShardPackCard(Modifier.weight(1f), "FOUNDRY TREASURY", "+2,500❖", "FULL UNLOCK", Color(0xFFFFD700), "btn_topup_2500") {
         onTopUpRiftShards(2500L); statusBanner = "✓ CREDITED +2,500❖ RIFT SHARDS TO PERMANENT PILOT ACCOUNT!"
        }
       }

       StoreActionRow("🏆 VERIFY 60M PUBLIC FFA WIN (>50% PRESENCE)", "Awards +100❖ Rift Shards & +1 Public Game Win", "+100❖ & +1 WIN", Color(0xFFFFD700), "btn_simulate_ffa_win") {
        onSimulateFfaWinPayout()
        statusBanner = "🏆 PUBLIC FFA VICTORY CREDITED: +100❖ RIFT SHARDS & +1 PUBLIC GAME WIN!"
       }
       StoreActionRow("⬆️ ADVANCE +1 PILOT LEVEL (LVL 1–100)", "Awards +30❖ per level (+150❖ every 5th level, +300❖ at Lv 20)", "+1 LVL", Color(0xFF00F0FF), "btn_simulate_level_up") {
        onSimulatePilotLevelUp(); refreshTick++
        statusBanner = "★ PILOT LEVEL ADVANCED! CHECK LEVEL MILESTONES FOR SHARD & COSMETIC UNLOCKS."
       }
      }
     }
    }

    1 -> {
     Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
      Column(
       modifier = Modifier.weight(1.15f).fillMaxHeight().background(Color(0xFF0A1628), RoundedCornerShape(10.dp))
        .border(1.dp, Color(0xFF1E3859), RoundedCornerShape(10.dp)).padding(10.dp),
       verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
       Text("SHIP 10 RIFT FRAME LOADOUT BAYS (FRAME A – FRAME E • FULL 100-PT PARITY)", color = Color(0xFFFFD700), fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
       LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        items(RiftProgressionCatalog.frameSlotGates, key = { it.slotId }) { gate ->
         val unlocked = remember(pilotLevel, unlockedExtraSlots, refreshTick) {
          if (gate.slotId == "Frame A") pilotLevel >= 20 else localPrefs.isCustomSlotUnlocked(gate.slotId) || pilotLevel >= gate.freeAtLevel
         }
         Row(
          modifier = Modifier.fillMaxWidth()
           .background(if (unlocked) Color(0xCC0D2821) else Color(0x990D1B30), RoundedCornerShape(8.dp))
           .border(1.dp, if (unlocked) Color(0xFF10B981) else Color(0x55FFD700), RoundedCornerShape(8.dp))
           .padding(horizontal = 10.dp, vertical = 7.dp),
          verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween,
         ) {
          Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
           Text("${gate.slotId.uppercase()} // ${gate.archetypeTitle}", color = if (unlocked) Color(0xFF00E676) else Color(0xFFFFD700), fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace)
           Text(gate.summary, color = Color(0xFF94A3B8), fontSize = 9.sp)
          }
          if (unlocked) {
           Box(
            modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(Color(0xFF063929)).border(1.dp, Color(0xFF00E676), RoundedCornerShape(6.dp))
             .clickable { localPrefs.setActiveCustomSlotId(gate.slotId); onOpenHangarShip10() }.padding(horizontal = 9.dp, vertical = 5.dp),
           ) { Text("✓ READY", color = Color(0xFF00E676), fontSize = 9.5.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace) }
          } else if (gate.shardCost > 0L) {
           Box(
            modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(Color(0xFF281E07)).border(1.dp, Color(0xFFFFD700), RoundedCornerShape(6.dp))
             .clickable {
              if (trySpendShards(gate.shardCost)) {
               localPrefs.setCustomSlotUnlocked(gate.slotId, true); refreshTick++
               statusBanner = "✓ UNLOCKED ${gate.slotId.uppercase()} (-${gate.shardCost}❖ RIFT SHARDS)!"
              } else statusBanner = "⚠️ NEED ${gate.shardCost}❖ RIFT SHARDS OR REACH LVL ${gate.freeAtLevel} FOR FREE."
             }
             .padding(horizontal = 9.dp, vertical = 5.dp),
           ) { Text("UNLOCK ${gate.shardCost}❖", color = Color(0xFFFFD700), fontSize = 9.5.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace) }
          } else {
           Text("FREE AT LVL ${gate.freeAtLevel}", color = Color(0xFF38BDF8), fontSize = 9.5.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace)
          }
         }
        }
       }
      }

      Column(
       modifier = Modifier.weight(0.9f).fillMaxHeight().background(Color(0xFF0A1628), RoundedCornerShape(10.dp))
        .border(1.dp, Color(0xFF1E3859), RoundedCornerShape(10.dp)).padding(10.dp).verticalScroll(rememberScrollState()),
       verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
       Text("MODULAR TUNING, RESPEC & HULL FORGE TOOLS", color = Color(0xFFC084FC), fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
       StoreActionRow("🛸 UNLOCK NEXT RIFT FRAME BAY (+$unlockedExtraSlots UNLOCKED)", "Unlocks the next locked Rift Frame bay (Frame B – Frame E)", "NEXT BAY", Color(0xFFFFD700), "btn_unlock_extra_slot") {
        if (onUnlockExtraSlot()) { refreshTick++; statusBanner = "✓ UNLOCKED NEXT RIFT FRAME LOADOUT BAY!" }
        else statusBanner = "⚠️ INSUFFICIENT RIFT SHARDS OR ALL RIFT FRAME BAYS (A–E) ARE ALREADY UNLOCKED."
       }
       StoreActionRow("🔄 FULL CUSTOMIZATION POINT RESPEC", "Refunds all $spentCustomizationPoints spent points back to Available ($availableCustomPts avail)", "50❖ SHARDS", Color(0xFF38BDF8), "btn_respec_custom_pts") {
        if (onRespecCustomizationPoints()) statusBanner = "✓ RESPEC COMPLETE (-50❖): ALL $totalCustomizationPoints CUSTOMIZATION POINTS REFUNDED!"
        else statusBanner = "⚠️ NEED 50❖ RIFT SHARDS & AT LEAST 1 SPENT CUSTOMIZATION POINT."
       }
       StoreActionRow("🛠️ ALLOCATE +1 TUNING POINT ($availableCustomPts AVAIL)", "Allocates 1 earned tuning point ($spentCustomizationPoints / $totalCustomizationPoints allocated)", "1 PT", Color(0xFF00E676), "btn_allocate_custom_pt") {
        if (onAllocateCustomizationPoint()) statusBanner = "✓ ALLOCATED 1 TUNING POINT TO ACTIVE HULL SPEC!"
        else statusBanner = "⚠️ NO UNSPENT TUNING POINTS AVAILABLE — LEVEL UP OR RESPEC!"
       }
       StoreActionRow("🎨 HULL FORGE SYNTHESIS VOUCHER ($aiShipDesignTokens HELD)", "Acquires an on-device procedural 3D Hull Forge voucher", "150❖ SHARDS", Color(0xFFC084FC), "btn_buy_ai_design_token") {
        if (onPurchaseAiDesignToken()) statusBanner = "✓ ACQUIRED HULL FORGE VOUCHER (-150❖ RIFT SHARDS)!"
        else statusBanner = "⚠️ INSUFFICIENT RIFT SHARDS (REQUIRES 150❖)."
       }
      }
     }
    }

    2 -> CosmeticPillarGrid("3D KILL FINISHERS — DETONATION & EXECUTIONER VFX", "KILL_FINISHER", RiftProgressionCatalog.killFinishers, pilotLevel, localPrefs, refreshTick, ::trySpendShards, { refreshTick++ }) { statusBanner = it }
    3 -> CosmeticPillarGrid("3D HULL TRIMS — CHASSIS BEVEL & REACTOR SEAM FINISHES", "HULL_TRIM", RiftProgressionCatalog.hullTrims, pilotLevel, localPrefs, refreshTick, ::trySpendShards, { refreshTick++ }) { statusBanner = it }
    4 -> CosmeticPillarGrid("SECTOR WARP-IN SIGNATURES — ARRIVAL APERTURE VFX", "WARP_SIGNATURE", RiftProgressionCatalog.warpSignatures, pilotLevel, localPrefs, refreshTick, ::trySpendShards, { refreshTick++ }) { statusBanner = it }
    else -> CosmeticPillarGrid("HVT CROWN HALOS — #1 SECTOR BOUNTY LEADER RINGS", "HVT_HALO", RiftProgressionCatalog.hvtHalos, pilotLevel, localPrefs, refreshTick, ::trySpendShards, { refreshTick++ }) { statusBanner = it }
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
 val equippedId = remember(categoryKey, refreshTick) { localPrefs.getEquippedCosmeticId(categoryKey) }
 Column(
  modifier = Modifier.fillMaxSize().background(Color(0xFF0A1628), RoundedCornerShape(10.dp))
   .border(1.dp, Color(0xFF1E3859), RoundedCornerShape(10.dp)).padding(10.dp),
  verticalArrangement = Arrangement.spacedBy(8.dp),
 ) {
  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
   Text(title, color = Color(0xFF00F0FF), fontSize = 11.5.sp, fontWeight = FontWeight.ExtraBold)
   Text("ZERO COMBAT ADVANTAGE • 100% COSMETIC", color = Color(0xFF94A3B8), fontSize = 9.5.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
  }

  LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
   items(items, key = { it.id }) { item ->
    val accentColor = Color(item.accentColorHex)
    val isUnlocked = remember(item.id, pilotLevel, refreshTick) { localPrefs.isCosmeticUnlocked(categoryKey, item.id, pilotLevel) }
    val isEquipped = equippedId == item.id

    Row(
     modifier = Modifier.fillMaxWidth()
      .background(if (isEquipped) Color(0xCC112A3E) else Color(0x990D1B30), RoundedCornerShape(9.dp))
      .border(1.2.dp, if (isEquipped) accentColor else accentColor.copy(alpha = 0.4f), RoundedCornerShape(9.dp))
      .padding(horizontal = 12.dp, vertical = 8.dp),
     verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween,
    ) {
     Row(modifier = Modifier.weight(1f).padding(end = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
      Box(modifier = Modifier.size(22.dp).clip(CircleShape).background(accentColor.copy(alpha = 0.25f)).border(1.5.dp, accentColor, CircleShape))
      Column {
       Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(item.name.uppercase(), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
        Text(if (item.freeAtLevel <= 1) "DEFAULT UNLOCK" else "FREE AT LVL ${item.freeAtLevel}", color = accentColor, fontSize = 9.5.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
       }
       Text(item.summary, color = Color(0xFF94A3B8), fontSize = 10.sp)
      }
     }

     if (isEquipped) {
      Box(modifier = Modifier.background(Color(0xFF063929), RoundedCornerShape(7.dp)).border(1.dp, Color(0xFF00E676), RoundedCornerShape(7.dp)).padding(horizontal = 12.dp, vertical = 6.dp)) {
       Text("✓ EQUIPPED", color = Color(0xFF00E676), fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace)
      }
     } else if (isUnlocked) {
      Button(
       onClick = { localPrefs.setEquippedCosmeticId(categoryKey, item.id); onRefresh(); onBanner("✓ EQUIPPED ${item.name.uppercase()}!") },
       colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = Color(0xFF05080E)),
       shape = RoundedCornerShape(7.dp), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp), modifier = Modifier.height(32.dp),
      ) { Text("EQUIP", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace) }
     } else {
      Button(
       onClick = {
        if (onTrySpendShards(item.shardCost)) {
         localPrefs.setCosmeticUnlocked(categoryKey, item.id, true)
         localPrefs.setEquippedCosmeticId(categoryKey, item.id)
         onRefresh()
         onBanner("✓ UNLOCKED & EQUIPPED ${item.name.uppercase()} (-${item.shardCost}❖)!")
        } else onBanner("⚠️ NEED ${item.shardCost}❖ FOR EARLY UNLOCK (OR REACH LVL ${item.freeAtLevel} FREE).")
       },
       colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF281E07), contentColor = Color(0xFFFFD700)),
       border = BorderStroke(1.dp, Color(0xFFFFD700)), shape = RoundedCornerShape(7.dp),
       contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp), modifier = Modifier.height(32.dp),
      ) { Text("UNLOCK ${item.shardCost}❖", fontSize = 9.5.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace) }
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
  modifier = modifier.background(Color(0xFF0E1B2E), RoundedCornerShape(8.dp))
   .border(1.dp, accent.copy(alpha = 0.45f), RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 5.dp),
 ) {
  Text(label, color = Color(0xFF8A99AD), fontSize = 8.5.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
  Text(value, color = accent, fontSize = 10.8.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace, maxLines = 1, overflow = TextOverflow.Ellipsis)
  if (progressFraction != null) {
   Spacer(modifier = Modifier.height(3.dp))
   Box(modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(Color(0xFF162C46))) {
    Box(modifier = Modifier.fillMaxWidth(progressFraction.coerceIn(0f, 1f)).fillMaxHeight().background(accent, RoundedCornerShape(2.dp)))
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
  modifier = modifier.clip(RoundedCornerShape(8.dp)).background(Color(0xFF10233E)).border(1.2.dp, accent, RoundedCornerShape(8.dp))
   .clickable { onClick() }.padding(horizontal = 8.dp, vertical = 8.dp).testTag(tag),
  horizontalAlignment = Alignment.CenterHorizontally,
 ) {
  Text(title, color = Color(0xFF8A99AD), fontSize = 8.5.sp, fontWeight = FontWeight.Bold, maxLines = 1)
  Text(amountText, color = accent, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace)
  Text(subText, color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, maxLines = 1)
 }
}

@Composable
private fun StoreActionRow(title: String, subtitle: String, costLabel: String, accent: Color, tag: String, onClick: () -> Unit) {
 Row(
  modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color(0xFF0F2038))
   .border(1.dp, accent.copy(alpha = 0.65f), RoundedCornerShape(8.dp)).clickable { onClick() }
   .padding(horizontal = 10.dp, vertical = 6.dp).testTag(tag),
  verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween,
 ) {
  Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
   Text(title, color = Color.White, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
   Text(subtitle, color = Color(0xFF94A3B8), fontSize = 8.8.sp)
  }
  Box(modifier = Modifier.background(accent.copy(alpha = 0.18f), RoundedCornerShape(6.dp)).border(1.dp, accent, RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
   Text(costLabel, color = accent, fontSize = 9.5.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace)
  }
 }
}
