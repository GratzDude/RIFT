package com.example.ui

import android.app.Activity
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.LocalPilotPrefs
import com.example.data.RiftBillingManager
import com.example.data.ShardPurchaseResult
import com.example.data.ShardVaultSkuPack
import com.example.engine.CosmeticCatalogItem
import com.example.engine.DailyConduitDirective
import com.example.engine.RiftCodex
import com.example.engine.RiftProgressionCatalog
import com.example.engine.RiftSpriteBaker
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.launch

private val ConduitTabletAbyss = Color(0xFF060D1A)
private val ConduitTabletPanel = Color(0xFF081224)
private val ConduitTabletElevated = Color(0xFF0C1B33)
private val ConduitElectricCyan = Color(0xFF38BDF8)
private val ConduitNexusTeal = Color(0xFF00F5D4)
private val ConduitSolarGold = Color(0xFFFACC15)

private fun parseHexColorSafe(hex: String, fallback: Color = ConduitNexusTeal): Color {
  val cleaned = hex.trim().removePrefix("#")
  val rgb = cleaned.toLongOrNull(16) ?: return fallback
  return if (cleaned.length == 8) Color(rgb) else Color(0xFF000000L or rgb)
}

@Composable
fun RiftShardsStoreModal(
  pilotLevel: Int = 1,
  currentLevelXp: Long = 0L,
  nextLevelXpSpan: Long = 250L,
  levelProgressFraction: Float = 0f,
  totalCustomizationPoints: Int = 100,
  spentCustomizationPoints: Int = 0,
  riftShards: Long = 150L,
  publicGameWins: Long = 0L,
  matchPresencePct: Int = 100,
  unlockedExtraSlots: Int = 0,
  aiShipDesignTokens: Int = 0,
  onTopUpRiftShards: (Long) -> Unit = {},
  onRespecCustomizationPoints: () -> Boolean = { false },
  onAllocateCustomizationPoint: () -> Boolean = { false },
  onUnlockExtraSlot: () -> Boolean = { false },
  onPurchaseAiDesignToken: () -> Boolean = { false },
  onSimulateFfaWinPayout: () -> Unit = {},
  onSimulatePilotLevelUp: () -> Unit = {},
  onSpendRiftShards: ((Long) -> Boolean)? = null,
  onOpenHangarShip10: () -> Unit = {},
  onCloseModal: () -> Unit = {},
  liveResonance: Int = -1,
  peakResonance: Int = -1,
  startingBounty: Int = -1,
  dailyDirectives: List<DailyConduitDirective>? = null,
  canRerollDirectiveToday: Boolean? = null,
  dailySweepClaimed: Boolean? = null,
  activeShipId: Int = 1,
  equippedCosmeticByCategory: ((String) -> Int)? = null,
  isCosmeticUnlocked: ((String, CosmeticCatalogItem) -> Boolean)? = null,
  isFrameSlotUnlocked: ((String) -> Boolean)? = null,
  activeCustomSlotId: String = "Frame A",
  onClaimDailyDirective: ((Int) -> Long)? = null,
  onRerollDailyDirective: ((Int) -> Boolean)? = null,
  onClaimDailySweep: (() -> Long)? = null,
  onUnlockOrEquipCosmetic: ((String, CosmeticCatalogItem) -> Pair<Boolean, String>)? = null,
  onUnlockOrSelectFrameSlot: ((String) -> Pair<Boolean, String>)? = null,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val localPrefs = remember(context) { LocalPilotPrefs(context) }
  val isCompactPhone = LocalConfiguration.current.screenHeightDp < 480
  val isTesterInfiniteShards = RiftBillingManager.TESTER_INFINITE_SHARDS_ACTIVE
  val displayedShards = RiftBillingManager.effectiveShardBalance(riftShards)

  var selectedSubDeck by remember { mutableIntStateOf(0) }
  var refreshTick by remember { mutableIntStateOf(0) }
  var previewShipId by remember(activeShipId) { mutableIntStateOf(activeShipId.coerceIn(1, 10)) }
  var previewCosmeticId by remember { mutableIntStateOf(0) }
  var previewFrameSlotId by remember(activeCustomSlotId) { mutableStateOf(activeCustomSlotId) }

  val effectiveLiveRes =
    if (liveResonance >= 0) liveResonance
    else remember(refreshTick) { localPrefs.getLiveResonance() }
  val effectivePeakRes =
    maxOf(
      effectiveLiveRes,
      if (peakResonance >= 0) peakResonance
      else remember(refreshTick) { localPrefs.getPeakResonance() },
    )
  val effectiveStartBounty =
    if (startingBounty >= 100) startingBounty
    else RiftProgressionCatalog.computeStartingBountyForResonance(effectiveLiveRes)

  val activeTier = remember(effectiveLiveRes) {
    RiftProgressionCatalog.getConduitTierForResonance(effectiveLiveRes)
  }
  val nextTierThreshold = remember(effectiveLiveRes) {
    RiftProgressionCatalog.getNextConduitTierThreshold(effectiveLiveRes)
  }
  val tierProgress = remember(effectiveLiveRes) {
    RiftProgressionCatalog.getConduitProgressFraction(effectiveLiveRes)
  }

  val resolvedDirectives =
    dailyDirectives ?: remember(refreshTick) { localPrefs.getDailyDirectives() }
  val resolvedCanReroll =
    canRerollDirectiveToday ?: remember(refreshTick) { localPrefs.canRerollDailyDirectiveToday() }
  val resolvedSweepClaimed =
    dailySweepClaimed ?: remember(refreshTick) { localPrefs.isDailySweepBonusClaimed() }

  var statusBanner by remember {
    mutableStateOf(
      if (isTesterInfiniteShards) {
        "RIFT CONDUIT ONLINE // TESTER TREASURY: 999,999 SHARDS ACTIVE • ALL 10 HULLS OPEN AT 0 RESONANCE"
      } else {
        "RIFT CONDUIT ONLINE // ALL 10 HULLS OPEN AT 0 RESONANCE • COSMETICS & FRAME B–E UNLOCK BY PEAK RESONANCE OR SHARDS"
      }
    )
  }

  fun trySpendShards(cost: Long): Boolean {
    if (cost <= 0L) return true
    if (!RiftBillingManager.canAffordShardSpend(displayedShards, cost)) return false
    if (onSpendRiftShards != null) return onSpendRiftShards(cost)
    if (!isTesterInfiniteShards) {
      onTopUpRiftShards(-cost)
    }
    return true
  }

  fun spendCostNote(cost: Long): String =
    if (isTesterInfiniteShards) "TESTER 0 SHARDS DEPLETED" else "-$cost SHARDS"

  fun queryEquippedCosmetic(categoryKey: String): Int =
    equippedCosmeticByCategory?.invoke(categoryKey)
      ?: localPrefs.getEquippedCosmeticId(categoryKey)

  fun queryCosmeticUnlocked(categoryKey: String, item: CosmeticCatalogItem): Boolean =
    isCosmeticUnlocked?.invoke(categoryKey, item)
      ?: (effectivePeakRes >= item.freeAtResonance ||
        localPrefs.isCosmeticUnlocked(categoryKey, item.id, pilotLevel))

  fun queryFrameUnlocked(slotId: String): Boolean {
    if (slotId.equals("Frame A", ignoreCase = true)) return true
    if (isFrameSlotUnlocked != null) return isFrameSlotUnlocked(slotId)
    val gate = RiftProgressionCatalog.getFrameSlotGate(slotId)
    return effectivePeakRes >= gate.freeAtResonance ||
      localPrefs.isCustomSlotUnlocked(slotId)
  }

  val activeCategoryKey =
    when (selectedSubDeck) {
      1 -> "KILL_FINISHER"
      2 -> "HULL_TRIM"
      3 -> "WARP_SIGNATURE"
      4 -> "HVT_HALO"
      else -> ""
    }

  LaunchedEffect(selectedSubDeck, refreshTick) {
    if (activeCategoryKey.isNotEmpty()) {
      previewCosmeticId = queryEquippedCosmetic(activeCategoryKey)
    } else if (selectedSubDeck == 5) {
      previewShipId = 10
    }
  }

  // Single 60fps animation phase for the Live Holo-Preview Stage
  var holoPhase by remember { mutableFloatStateOf(0f) }
  LaunchedEffect(Unit) {
    var lastNs = 0L
    while (true) {
      withFrameNanos { nowNs ->
        val dt = if (lastNs == 0L) 0.016f else ((nowNs - lastNs) / 1_000_000_000f).coerceIn(0.004f, 0.05f)
        lastNs = nowNs
        holoPhase = (holoPhase + dt * 2.1f) % (PI.toFloat() * 200f)
      }
    }
  }

  Column(
    modifier =
      Modifier.fillMaxSize()
        .background(ConduitTabletAbyss)
        .padding(if (isCompactPhone) 6.dp else 8.dp)
        .testTag("rift_shards_store_modal"),
    verticalArrangement = Arrangement.spacedBy(if (isCompactPhone) 5.dp else 6.dp),
  ) {
    // 1. Top Conduit Telemetry Strip (#081224 Sci-Fi Command Tablet)
    Row(
      modifier =
        Modifier.fillMaxWidth()
          .height(if (isCompactPhone) 38.dp else 42.dp)
          .background(ConduitTabletPanel, RoundedCornerShape(9.dp))
          .border(1.dp, Color(activeTier.accentColorHex).copy(alpha = 0.70f), RoundedCornerShape(9.dp))
          .padding(horizontal = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.weight(1f),
      ) {
        ResonanceCrestIcon(sizeDp = 16.dp)
        Box(
          modifier =
            Modifier.background(ConduitTabletAbyss, RoundedCornerShape(5.dp))
              .border(1.dp, Color(activeTier.accentColorHex), RoundedCornerShape(5.dp))
              .padding(horizontal = 7.dp, vertical = 2.dp)
        ) {
          Text(
            text = activeTier.title,
            color = Color(activeTier.accentColorHex),
            fontSize = if (isCompactPhone) 9.8.sp else 10.8.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
          )
        }
        ConduitMetricInline("LIVE RESONANCE", "$effectiveLiveRes", ConduitNexusTeal)
        ConduitMetricInline("PEAK RESONANCE", "$effectivePeakRes", Color(0xFFC084FC))
        ConduitMetricInline("START RESONANCE", "$effectiveStartBounty RES", ConduitSolarGold)
        if (!isCompactPhone) {
          Column(modifier = Modifier.width(138.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
              text =
                if (effectiveLiveRes >= 1500) "APEX TIER LOCKED (500 MAX)"
                else "NEXT TIER: $effectiveLiveRes / $nextTierThreshold",
              color = Color(0xFF94A3B8),
              fontSize = 9.5.sp,
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.Monospace,
              maxLines = 1,
            )
            Box(
              modifier =
                Modifier.fillMaxWidth()
                  .height(4.dp)
                  .clip(RoundedCornerShape(2.dp))
                  .background(ConduitTabletAbyss)
            ) {
              Box(
                modifier =
                  Modifier.fillMaxWidth(tierProgress.coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .background(Color(activeTier.accentColorHex), RoundedCornerShape(2.dp))
              )
            }
          }
        }
      }

      Row(
        modifier =
          Modifier.background(ConduitTabletAbyss, RoundedCornerShape(7.dp))
            .border(1.dp, ConduitSolarGold, RoundedCornerShape(7.dp))
            .clickable { selectedSubDeck = 6 }
            .padding(horizontal = 10.dp, vertical = 4.dp)
            .testTag("store_rift_shards_balance"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
      ) {
        RiftShardGemIcon(sizeDp = 16.dp)
        Text(
          text =
            if (isTesterInfiniteShards) {
              if (isCompactPhone) "999,999" else "999,999 TESTER"
            } else {
              "$displayedShards SHARDS"
            },
          color = ConduitSolarGold,
          fontSize = if (isCompactPhone) 10.5.sp else 11.5.sp,
          fontWeight = FontWeight.ExtraBold,
          fontFamily = FontFamily.Monospace,
        )
      }
    }

    // 2. 7 Sub-Deck Pills (Indices 0..6)
    Row(
      modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(6.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      val subDecks =
        listOf(
          "DAILY DIRECTIVES (250)",
          "KILL FINISHERS",
          "3D HULL TRIMS",
          "WARP SIGNATURES",
          "RESONANCE HALOS",
          "RIFT FRAME BAYS (A–E)",
          "SHARD VAULT",
        )
      subDecks.forEachIndexed { idx, label ->
        val selected = selectedSubDeck == idx
        val isVaultPill = idx == 6
        val showShardIcon = idx == 0 || idx == 6
        val bgCol =
          when {
            selected && isVaultPill -> ConduitSolarGold
            selected -> ConduitNexusTeal
            else -> ConduitTabletPanel
          }
        val borderCol =
          when {
            selected && isVaultPill -> ConduitSolarGold
            selected -> ConduitNexusTeal
            isVaultPill -> ConduitSolarGold.copy(alpha = 0.70f)
            else -> ConduitElectricCyan.copy(alpha = 0.45f)
          }
        val txtCol =
          when {
            selected -> ConduitTabletAbyss
            isVaultPill -> ConduitSolarGold
            else -> Color.White
          }
        Row(
          modifier =
            Modifier.height(if (isCompactPhone) 28.dp else 31.dp)
              .clip(RoundedCornerShape(7.dp))
              .background(bgCol)
              .border(1.dp, borderCol, RoundedCornerShape(7.dp))
              .clickable { selectedSubDeck = idx }
              .padding(horizontal = if (isCompactPhone) 8.dp else 10.dp)
              .testTag("progression_pillar_tab_$idx"),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          if (showShardIcon) {
            RiftShardGemIcon(sizeDp = 13.dp)
          }
          Text(
            text = label,
            color = txtCol,
            fontSize = if (isCompactPhone) 9.5.sp else 10.2.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
          )
        }
      }
    }

    // Status Feedback Strip
    Box(
      modifier =
        Modifier.fillMaxWidth()
          .background(ConduitTabletPanel, RoundedCornerShape(6.dp))
          .border(1.dp, ConduitNexusTeal.copy(alpha = 0.45f), RoundedCornerShape(6.dp))
          .padding(horizontal = 10.dp, vertical = 3.5.dp)
    ) {
      Text(
        text = statusBanner.replace("\u2756", " SHARDS").replace("  ", " "),
        color = ConduitNexusTeal,
        fontSize = if (isCompactPhone) 9.5.sp else 10.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }

    // 3. Zero-Scroll Two-Column Conduit Body (Left 0.42f Live 60fps Holo-Stage + Right 0.58f Interactive Deck)
    Row(
      modifier = Modifier.weight(1f).fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      ConduitHoloPreviewStage(
        modifier = Modifier.weight(0.42f).fillMaxHeight(),
        holoPhase = holoPhase,
        selectedSubDeck = selectedSubDeck,
        previewShipId = previewShipId,
        previewCosmeticId = previewCosmeticId,
        previewFrameSlotId = previewFrameSlotId,
        equippedTrimId = queryEquippedCosmetic("HULL_TRIM"),
        equippedWarpId = queryEquippedCosmetic("WARP_SIGNATURE"),
        equippedHaloId = queryEquippedCosmetic("HVT_HALO"),
        equippedFinisherId = queryEquippedCosmetic("KILL_FINISHER"),
        liveResonance = effectiveLiveRes,
        peakResonance = effectivePeakRes,
        startingBounty = effectiveStartBounty,
        publicGameWins = publicGameWins,
        onCycleShip = { delta ->
          val next = ((previewShipId - 1 + delta + 10) % 10) + 1
          previewShipId = next
        },
      )

      Box(
        modifier =
          Modifier.weight(0.58f)
            .fillMaxHeight()
            .background(ConduitTabletPanel, RoundedCornerShape(10.dp))
            .border(1.dp, ConduitElectricCyan.copy(alpha = 0.48f), RoundedCornerShape(10.dp))
            .padding(if (isCompactPhone) 8.dp else 10.dp)
      ) {
        when (selectedSubDeck) {
          0 -> {
            DailyDirectivesDeck(
              directives = resolvedDirectives,
              canRerollToday = resolvedCanReroll,
              sweepBonusClaimed = resolvedSweepClaimed,
              liveResonance = effectiveLiveRes,
              isCompactPhone = isCompactPhone,
              onClaimDirective = { slotIdx ->
                val earned =
                  if (onClaimDailyDirective != null) {
                    onClaimDailyDirective(slotIdx)
                  } else {
                    val amt = localPrefs.claimDailyDirectiveReward(slotIdx)?.first ?: 0L
                    if (amt > 0L) onTopUpRiftShards(amt)
                    amt
                  }
                refreshTick++
                if (earned > 0L) {
                  statusBanner = "DIRECTIVE COMPLETE // CREDITED +$earned RIFT SHARDS TO VAULT"
                }
              },
              onRerollDirective = { slotIdx ->
                val ok =
                  onRerollDailyDirective?.invoke(slotIdx)
                    ?: localPrefs.rerollDailyDirective(slotIdx)
                refreshTick++
                statusBanner =
                  if (ok) "DAILY DIRECTIVE REROLLED // NEW TACTICAL CONTRACT ASSIGNED"
                  else "DAILY REROLL UNAVAILABLE // LIMIT 1 REROLL PER UTC DAY ON ACTIVE CONTRACTS"
              },
              onClaimSweep = {
                var autoClaimedEarned = 0L
                for (dir in resolvedDirectives) {
                  if (dir.isComplete && !dir.claimed) {
                    autoClaimedEarned +=
                      if (onClaimDailyDirective != null) {
                        onClaimDailyDirective(dir.slotIndex)
                      } else {
                        val amt = localPrefs.claimDailyDirectiveReward(dir.slotIndex)?.first ?: 0L
                        if (amt > 0L) onTopUpRiftShards(amt)
                        amt
                      }
                  }
                }
                val bonus =
                  if (onClaimDailySweep != null) {
                    onClaimDailySweep()
                  } else {
                    val amt = localPrefs.claimDailySweepBonus()?.first ?: 0L
                    if (amt > 0L) onTopUpRiftShards(amt)
                    amt
                  }
                refreshTick++
                val totalCredited = autoClaimedEarned + bonus
                if (totalCredited > 0L) {
                  statusBanner =
                    "3/3 DAILY CONDUIT SWEEP CLAIMED // +$totalCredited RIFT SHARDS CREDITED!"
                }
              },
            )
          }

          1 -> {
            ConduitCosmeticListDeck(
              title = "3D KILL FINISHERS // DETONATION & EXECUTIONER VFX",
              categoryKey = "KILL_FINISHER",
              items = RiftProgressionCatalog.killFinishers,
              previewedId = previewCosmeticId,
              equippedId = queryEquippedCosmetic("KILL_FINISHER"),
              isItemUnlocked = { queryCosmeticUnlocked("KILL_FINISHER", it) },
              onPreviewItem = { previewCosmeticId = it.id },
              onUnlockOrEquip = { item ->
                previewCosmeticId = item.id
                if (onUnlockOrEquipCosmetic != null) {
                  val (_, msg) = onUnlockOrEquipCosmetic("KILL_FINISHER", item)
                  refreshTick++
                  statusBanner = msg
                } else {
                  val unlocked = queryCosmeticUnlocked("KILL_FINISHER", item)
                  if (unlocked) {
                    localPrefs.setEquippedCosmeticId("KILL_FINISHER", item.id)
                    refreshTick++
                    statusBanner = "EQUIPPED KILL FINISHER: ${item.name.uppercase(Locale.US)}"
                  } else if (trySpendShards(item.shardCost)) {
                    localPrefs.setCosmeticUnlocked("KILL_FINISHER", item.id, true)
                    localPrefs.setEquippedCosmeticId("KILL_FINISHER", item.id)
                    refreshTick++
                    statusBanner =
                      "UNLOCKED & EQUIPPED ${item.name.uppercase(Locale.US)} (${spendCostNote(item.shardCost)})"
                  } else {
                    statusBanner =
                      "REQUIRES ${item.freeAtResonance} PEAK RESONANCE (FREE) OR ${item.shardCost} RIFT SHARDS"
                  }
                }
              },
            )
          }

          2 -> {
            ConduitCosmeticListDeck(
              title = "3D HULL TRIMS // CHASSIS BEVEL & REACTOR SEAM FINISHES",
              categoryKey = "HULL_TRIM",
              items = RiftProgressionCatalog.hullTrims,
              previewedId = previewCosmeticId,
              equippedId = queryEquippedCosmetic("HULL_TRIM"),
              isItemUnlocked = { queryCosmeticUnlocked("HULL_TRIM", it) },
              onPreviewItem = { previewCosmeticId = it.id },
              onUnlockOrEquip = { item ->
                previewCosmeticId = item.id
                if (onUnlockOrEquipCosmetic != null) {
                  val (_, msg) = onUnlockOrEquipCosmetic("HULL_TRIM", item)
                  refreshTick++
                  statusBanner = msg
                } else {
                  val unlocked = queryCosmeticUnlocked("HULL_TRIM", item)
                  if (unlocked) {
                    localPrefs.setEquippedCosmeticId("HULL_TRIM", item.id)
                    RiftSpriteBaker.activeHullTrimStyle = item.id.coerceIn(0, 3)
                    refreshTick++
                    statusBanner = "EQUIPPED 3D HULL TRIM: ${item.name.uppercase(Locale.US)}"
                  } else if (trySpendShards(item.shardCost)) {
                    localPrefs.setCosmeticUnlocked("HULL_TRIM", item.id, true)
                    localPrefs.setEquippedCosmeticId("HULL_TRIM", item.id)
                    RiftSpriteBaker.activeHullTrimStyle = item.id.coerceIn(0, 3)
                    refreshTick++
                    statusBanner =
                      "UNLOCKED & EQUIPPED ${item.name.uppercase(Locale.US)} (${spendCostNote(item.shardCost)})"
                  } else {
                    statusBanner =
                      "REQUIRES ${item.freeAtResonance} PEAK RESONANCE (FREE) OR ${item.shardCost} RIFT SHARDS"
                  }
                }
              },
            )
          }

          3 -> {
            ConduitCosmeticListDeck(
              title = "WARP SIGNATURES // SECTOR ARRIVAL APERTURE VFX",
              categoryKey = "WARP_SIGNATURE",
              items = RiftProgressionCatalog.warpSignatures,
              previewedId = previewCosmeticId,
              equippedId = queryEquippedCosmetic("WARP_SIGNATURE"),
              isItemUnlocked = { queryCosmeticUnlocked("WARP_SIGNATURE", it) },
              onPreviewItem = { previewCosmeticId = it.id },
              onUnlockOrEquip = { item ->
                previewCosmeticId = item.id
                if (onUnlockOrEquipCosmetic != null) {
                  val (_, msg) = onUnlockOrEquipCosmetic("WARP_SIGNATURE", item)
                  refreshTick++
                  statusBanner = msg
                } else {
                  val unlocked = queryCosmeticUnlocked("WARP_SIGNATURE", item)
                  if (unlocked) {
                    localPrefs.setEquippedCosmeticId("WARP_SIGNATURE", item.id)
                    refreshTick++
                    statusBanner = "EQUIPPED WARP SIGNATURE: ${item.name.uppercase(Locale.US)}"
                  } else if (trySpendShards(item.shardCost)) {
                    localPrefs.setCosmeticUnlocked("WARP_SIGNATURE", item.id, true)
                    localPrefs.setEquippedCosmeticId("WARP_SIGNATURE", item.id)
                    refreshTick++
                    statusBanner =
                      "UNLOCKED & EQUIPPED ${item.name.uppercase(Locale.US)} (${spendCostNote(item.shardCost)})"
                  } else {
                    statusBanner =
                      "REQUIRES ${item.freeAtResonance} PEAK RESONANCE (FREE) OR ${item.shardCost} RIFT SHARDS"
                  }
                }
              },
            )
          }

          4 -> {
            ConduitCosmeticListDeck(
              title = "RESONANCE HALOS // #1 SECTOR RESONANCE LEADER RINGS",
              categoryKey = "HVT_HALO",
              items = RiftProgressionCatalog.hvtHalos,
              previewedId = previewCosmeticId,
              equippedId = queryEquippedCosmetic("HVT_HALO"),
              isItemUnlocked = { queryCosmeticUnlocked("HVT_HALO", it) },
              onPreviewItem = { previewCosmeticId = it.id },
              onUnlockOrEquip = { item ->
                previewCosmeticId = item.id
                if (onUnlockOrEquipCosmetic != null) {
                  val (_, msg) = onUnlockOrEquipCosmetic("HVT_HALO", item)
                  refreshTick++
                  statusBanner = msg
                } else {
                  val unlocked = queryCosmeticUnlocked("HVT_HALO", item)
                  if (unlocked) {
                    localPrefs.setEquippedCosmeticId("HVT_HALO", item.id)
                    refreshTick++
                    statusBanner = "EQUIPPED RESONANCE HALO: ${item.name.uppercase(Locale.US)}"
                  } else if (trySpendShards(item.shardCost)) {
                    localPrefs.setCosmeticUnlocked("HVT_HALO", item.id, true)
                    localPrefs.setEquippedCosmeticId("HVT_HALO", item.id)
                    refreshTick++
                    statusBanner =
                      "UNLOCKED & EQUIPPED ${item.name.uppercase(Locale.US)} (${spendCostNote(item.shardCost)})"
                  } else {
                    statusBanner =
                      "REQUIRES ${item.freeAtResonance} PEAK RESONANCE (FREE) OR ${item.shardCost} RIFT SHARDS"
                  }
                }
              },
            )
          }

          5 -> {
            ConduitFrameBaysDeck(
              activeSlotId = activeCustomSlotId,
              spentCustomizationPoints = spentCustomizationPoints,
              totalCustomizationPoints = totalCustomizationPoints,
              aiShipDesignTokens = aiShipDesignTokens,
              isTesterInfiniteShards = isTesterInfiniteShards,
              isFrameUnlocked = ::queryFrameUnlocked,
              onPreviewSlot = { slotId ->
                previewShipId = 10
                previewFrameSlotId = slotId
              },
              onUnlockOrSelectSlot = { slotId ->
                previewShipId = 10
                previewFrameSlotId = slotId
                if (onUnlockOrSelectFrameSlot != null) {
                  val (_, msg) = onUnlockOrSelectFrameSlot(slotId)
                  refreshTick++
                  statusBanner = msg
                } else {
                  val gate = RiftProgressionCatalog.getFrameSlotGate(slotId)
                  if (queryFrameUnlocked(slotId)) {
                    localPrefs.setActiveCustomSlotId(slotId)
                    refreshTick++
                    onOpenHangarShip10()
                  } else if (trySpendShards(gate.shardCost)) {
                    localPrefs.setCustomSlotUnlocked(slotId, true)
                    localPrefs.setActiveCustomSlotId(slotId)
                    refreshTick++
                    statusBanner =
                      "UNLOCKED ${slotId.uppercase(Locale.US)} (${spendCostNote(gate.shardCost)})"
                  } else {
                    statusBanner =
                      "REQUIRES ${gate.freeAtResonance} PEAK RESONANCE (FREE) OR ${gate.shardCost} RIFT SHARDS"
                  }
                }
              },
              onRespecPoints = {
                if (onRespecCustomizationPoints()) {
                  refreshTick++
                  statusBanner = "RIFT FRAME RESPEC COMPLETE (${spendCostNote(50L)})"
                } else {
                  statusBanner = "RESPEC REQUIRES >0 ALLOCATED POINTS (50 RIFT SHARDS)"
                }
              },
              onBuyForgeVoucher = {
                if (onPurchaseAiDesignToken()) {
                  refreshTick++
                  statusBanner = "ACQUIRED HULL FORGE SYNTHESIS VOUCHER (${spendCostNote(150L)})"
                } else {
                  statusBanner = "HULL FORGE VOUCHER REQUIRES 150 RIFT SHARDS"
                }
              },
              onOpenHangarShip10 = onOpenHangarShip10,
            )
          }

          else -> {
            ShardVaultTreasuryDeck(
              isCompactPhone = isCompactPhone,
              isTesterInfiniteShards = isTesterInfiniteShards,
              onSelectSkuPack = { pack ->
                scope.launch {
                  val result =
                    RiftBillingManager.launchShardPackPurchaseFlow(
                      activity = context as? Activity,
                      sku = pack,
                      currentUid = localPrefs.getOrCreatePermanentDeviceUid(),
                      callsign = localPrefs.getCallsign(),
                    )
                  when (result) {
                    is ShardPurchaseResult.VerifiedAndCredited -> {
                      onTopUpRiftShards(result.shardsCredited)
                      refreshTick++
                      statusBanner =
                        "VERIFIED PLAY PURCHASE [${result.productId.uppercase(Locale.US)}] // +${result.shardsCredited} SHARDS CREDITED"
                    }
                    is ShardPurchaseResult.TesterModeDormantNotice -> {
                      statusBanner = result.message
                    }
                    is ShardPurchaseResult.BillingUnavailable -> {
                      statusBanner = result.reason
                    }
                  }
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
private fun ShardVaultTreasuryDeck(
  isCompactPhone: Boolean,
  isTesterInfiniteShards: Boolean,
  onSelectSkuPack: (ShardVaultSkuPack) -> Unit,
) {
  val rows = remember { RiftBillingManager.OFFICIAL_SKU_CATALOG.chunked(2) }
  val studioUpper = RiftBillingManager.STUDIO_LEGAL_NAME.uppercase(Locale.US)

  Column(
    modifier = Modifier.fillMaxSize().testTag("shard_vault_treasury_deck"),
    verticalArrangement = Arrangement.spacedBy(if (isCompactPhone) 5.dp else 7.dp),
  ) {
    // Top Studio / Tester Treasury Status Banner
    Column(
      modifier =
        Modifier.fillMaxWidth()
          .background(ConduitTabletAbyss, RoundedCornerShape(8.dp))
          .border(
            1.dp,
            if (isTesterInfiniteShards) ConduitNexusTeal else ConduitSolarGold,
            RoundedCornerShape(8.dp),
          )
          .padding(horizontal = 10.dp, vertical = 5.dp)
          .testTag("shard_vault_tester_banner"),
      verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        RiftShardGemIcon(sizeDp = 15.dp)
        Text(
          text =
            if (isTesterInfiniteShards) {
              "BETA TESTER TREASURY ACTIVE // INFINITE 999,999 SHARDS ENABLED"
            } else {
              "OFFICIAL GOOGLE PLAY BILLING TREASURY // $studioUpper"
            },
          color = if (isTesterInfiniteShards) ConduitNexusTeal else ConduitSolarGold,
          fontSize = if (isCompactPhone) 9.8.sp else 10.5.sp,
          fontWeight = FontWeight.Black,
          fontFamily = FontFamily.Monospace,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
      Text(
        text =
          if (isTesterInfiniteShards) {
            "GOOGLE PLAY BILLING ($studioUpper) STANDING BY FOR LAUNCH • ALL UNLOCKS DEPLETE 0 SHARDS"
          } else {
            "CONSUMABLE RIFT SHARD PACKS VERIFIED VIA FIRESTORE PURCHASE TOKEN LEDGER • ZERO PAY-TO-WIN"
          },
        color = Color(0xFFCBD5E1),
        fontSize = 9.5.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }

    // Zero-scroll 2x2 SKU Pack Grid
    Column(
      modifier = Modifier.weight(1f).fillMaxWidth(),
      verticalArrangement = Arrangement.spacedBy(if (isCompactPhone) 6.dp else 8.dp),
    ) {
      rows.forEach { rowPacks ->
        Row(
          modifier = Modifier.weight(1f).fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(if (isCompactPhone) 6.dp else 8.dp),
        ) {
          rowPacks.forEach { pack ->
            val accent = parseHexColorSafe(pack.accentHex, ConduitElectricCyan)
            Column(
              modifier =
                Modifier.weight(1f)
                  .fillMaxHeight()
                  .clip(RoundedCornerShape(9.dp))
                  .background(
                    Brush.verticalGradient(
                      listOf(
                        accent.copy(alpha = 0.14f),
                        ConduitTabletPanel,
                        ConduitTabletAbyss,
                      )
                    )
                  )
                  .border(1.2.dp, accent.copy(alpha = 0.72f), RoundedCornerShape(9.dp))
                  .padding(if (isCompactPhone) 7.dp else 9.dp)
                  .testTag("shard_vault_sku_${pack.productId}"),
              verticalArrangement = Arrangement.SpaceBetween,
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
                  RiftShardGemIcon(sizeDp = if (isCompactPhone) 16.dp else 18.dp)
                  Column {
                    Text(
                      text = pack.title,
                      color = Color.White,
                      fontSize = if (isCompactPhone) 10.5.sp else 11.5.sp,
                      fontWeight = FontWeight.Black,
                      fontFamily = FontFamily.Monospace,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                      text = "SKU // ${pack.productId}",
                      color = Color(0xFF94A3B8),
                      fontSize = 9.5.sp,
                      fontWeight = FontWeight.Bold,
                      fontFamily = FontFamily.Monospace,
                      maxLines = 1,
                    )
                  }
                }

                Box(
                  modifier =
                    Modifier.background(ConduitTabletAbyss, RoundedCornerShape(5.dp))
                      .border(1.dp, accent, RoundedCornerShape(5.dp))
                      .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                  Text(
                    text = pack.badgeLabel,
                    color = accent,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                  )
                }
              }

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
              ) {
                Column {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                  ) {
                    RiftShardGemIcon(sizeDp = 16.dp)
                    Text(
                      text = "${pack.totalShards}",
                      color = ConduitSolarGold,
                      fontSize = if (isCompactPhone) 14.sp else 17.sp,
                      fontWeight = FontWeight.Black,
                      fontFamily = FontFamily.Monospace,
                    )
                  }
                  Text(
                    text =
                      if (pack.bonusShards > 0L) {
                        "${pack.shardAmount} BASE + ${pack.bonusShards} BONUS"
                      } else {
                        "${pack.shardAmount} BASE RIFT SHARDS"
                      },
                    color = Color(0xFF94A3B8),
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                  )
                }

                Text(
                  text = pack.displayPriceUsd,
                  color = Color.White,
                  fontSize = if (isCompactPhone) 11.5.sp else 13.5.sp,
                  fontWeight = FontWeight.Black,
                  fontFamily = FontFamily.Monospace,
                )
              }

              Button(
                onClick = { onSelectSkuPack(pack) },
                colors =
                  ButtonDefaults.buttonColors(
                    containerColor =
                      if (isTesterInfiniteShards) ConduitTabletAbyss else accent,
                    contentColor =
                      if (isTesterInfiniteShards) ConduitNexusTeal else ConduitTabletAbyss,
                  ),
                border =
                  BorderStroke(
                    1.dp,
                    if (isTesterInfiniteShards) ConduitNexusTeal else accent,
                  ),
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                modifier =
                  Modifier.fillMaxWidth()
                    .height(if (isCompactPhone) 27.dp else 30.dp)
                    .testTag("btn_buy_sku_${pack.productId}"),
              ) {
                Text(
                  text =
                    if (isTesterInfiniteShards) {
                      if (isCompactPhone) "INACTIVE — INFINITE TESTER"
                      else "PLAY BILLING INACTIVE — 999,999 TESTER"
                    } else {
                      "PURCHASE ${pack.displayPriceUsd}"
                    },
                  fontSize = if (isCompactPhone) 9.5.sp else 10.sp,
                  fontWeight = FontWeight.Black,
                  fontFamily = FontFamily.Monospace,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
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
private fun ConduitHoloPreviewStage(
  modifier: Modifier = Modifier,
  holoPhase: Float,
  selectedSubDeck: Int,
  previewShipId: Int,
  previewCosmeticId: Int,
  previewFrameSlotId: String,
  equippedTrimId: Int,
  equippedWarpId: Int,
  equippedHaloId: Int,
  equippedFinisherId: Int,
  liveResonance: Int,
  peakResonance: Int,
  startingBounty: Int,
  publicGameWins: Long,
  onCycleShip: (Int) -> Unit,
) {
  val customVer = RiftSpriteBaker.customShip9Version
  val effectiveTrim =
    if (selectedSubDeck == 2) previewCosmeticId.coerceIn(0, 3) else equippedTrimId.coerceIn(0, 3)
  val shipSpec = remember(previewShipId, customVer) { RiftCodex.getShipById(previewShipId.coerceIn(1, 10)) }
  val shipBitmap =
    remember(previewShipId, effectiveTrim, customVer) {
      RiftSpriteBaker.getShipSprite(
        shipId = previewShipId.coerceIn(1, 10),
        isAllied = true,
        isHitFlashing = false,
        trimStyleOverride = effectiveTrim,
      ).asImageBitmap()
    }

  val stageAccent =
    when (selectedSubDeck) {
      1 -> Color(
        RiftProgressionCatalog.killFinishers
          .firstOrNull { it.id == previewCosmeticId }
          ?.accentColorHex ?: 0xFF00E5FF.toInt()
      )
      2 -> Color(
        RiftProgressionCatalog.hullTrims
          .firstOrNull { it.id == previewCosmeticId }
          ?.accentColorHex ?: 0xFF38BDF8.toInt()
      )
      3 -> Color(
        RiftProgressionCatalog.warpSignatures
          .firstOrNull { it.id == previewCosmeticId }
          ?.accentColorHex ?: 0xFF00F5D4.toInt()
      )
      4 -> Color(
        RiftProgressionCatalog.hvtHalos
          .firstOrNull { it.id == previewCosmeticId }
          ?.accentColorHex ?: 0xFFFFD700.toInt()
      )
      5 -> Color(0xFFEC4899)
      6 -> ConduitSolarGold
      else -> ConduitNexusTeal
    }

  val stageModeLabel =
    when (selectedSubDeck) {
      1 -> {
        val fin = RiftProgressionCatalog.killFinishers.firstOrNull { it.id == previewCosmeticId }?.name ?: "Plasma Supernova"
        "KILL FINISHER // ${fin.uppercase(Locale.US)}"
      }
      2 -> {
        val tr = RiftProgressionCatalog.hullTrims.firstOrNull { it.id == previewCosmeticId }?.name ?: "Factory Alloy"
        "3D HULL TRIM // ${tr.uppercase(Locale.US)}"
      }
      3 -> {
        val wp = RiftProgressionCatalog.warpSignatures.firstOrNull { it.id == previewCosmeticId }?.name ?: "Standard Phase"
        "WARP APERTURE // ${wp.uppercase(Locale.US)}"
      }
      4 -> {
        val hl = RiftProgressionCatalog.hvtHalos.firstOrNull { it.id == previewCosmeticId }?.name ?: "Hunter Amber Ring"
        "LEADER HALO // ${hl.uppercase(Locale.US)}"
      }
      5 -> "RIFT FRAME // ${previewFrameSlotId.uppercase(Locale.US)} (100-PT PARITY)"
      6 -> "SHARD VAULT // ${RiftBillingManager.STUDIO_LEGAL_NAME.uppercase(Locale.US)}"
      else -> "LIVE CONDUIT HOLO-STAGE // 60FPS TELEMETRY"
    }

  Column(
    modifier =
      modifier
        .background(ConduitTabletPanel, RoundedCornerShape(10.dp))
        .border(1.dp, stageAccent.copy(alpha = 0.60f), RoundedCornerShape(10.dp))
        .padding(9.dp),
    verticalArrangement = Arrangement.SpaceBetween,
  ) {
    // Stage Top Bar with Hull Switcher
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = stageModeLabel,
        color = stageAccent,
        fontSize = 9.8.sp,
        fontWeight = FontWeight.ExtraBold,
        fontFamily = FontFamily.Monospace,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.weight(1f).padding(end = 6.dp),
      )
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        Box(
          modifier =
            Modifier.clip(RoundedCornerShape(5.dp))
              .background(ConduitTabletAbyss)
              .border(1.dp, ConduitElectricCyan, RoundedCornerShape(5.dp))
              .clickable { onCycleShip(-1) }
              .padding(horizontal = 7.dp, vertical = 2.dp)
              .testTag("conduit_holo_prev_ship")
        ) {
          Text("<", color = ConduitElectricCyan, fontSize = 9.8.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
        }
        Text(
          text = shipSpec.name.uppercase(Locale.US),
          color = Color.White,
          fontSize = 9.8.sp,
          fontWeight = FontWeight.Black,
          fontFamily = FontFamily.Monospace,
        )
        Box(
          modifier =
            Modifier.clip(RoundedCornerShape(5.dp))
              .background(ConduitTabletAbyss)
              .border(1.dp, ConduitElectricCyan, RoundedCornerShape(5.dp))
              .clickable { onCycleShip(1) }
              .padding(horizontal = 7.dp, vertical = 2.dp)
              .testTag("conduit_holo_next_ship")
        ) {
          Text(">", color = ConduitElectricCyan, fontSize = 9.8.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
        }
      }
    }

    // Center Animated 60fps Holo-Stage Canvas + 3D Banking Ship Sprite
    Box(
      modifier =
        Modifier.weight(1f)
          .fillMaxWidth()
          .padding(vertical = 6.dp)
          .clip(RoundedCornerShape(9.dp))
          .background(
            Brush.radialGradient(
              colors = listOf(stageAccent.copy(alpha = 0.16f), ConduitTabletAbyss, Color(0xFF030710))
            )
          )
          .border(1.dp, Color(0xFF1E3A5F), RoundedCornerShape(9.dp)),
      contentAlignment = Alignment.Center,
    ) {
      Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val cx = w * 0.5f
        val cy = h * 0.50f
        val pedY = h * 0.80f

        // Holo scan-cone & pedestal rings
        val conePath =
          Path().apply {
            moveTo(cx - w * 0.30f, pedY)
            lineTo(cx - w * 0.42f, h * 0.12f)
            lineTo(cx + w * 0.42f, h * 0.12f)
            lineTo(cx + w * 0.30f, pedY)
            close()
          }
        drawPath(
          path = conePath,
          brush =
            Brush.verticalGradient(
              colors = listOf(stageAccent.copy(alpha = 0.03f), stageAccent.copy(alpha = 0.22f)),
              startY = h * 0.12f,
              endY = pedY,
            ),
        )
        drawOval(
          color = stageAccent.copy(alpha = 0.25f),
          topLeft = Offset(cx - w * 0.34f, pedY - h * 0.08f),
          size = Size(w * 0.68f, h * 0.16f),
        )
        drawOval(
          color = stageAccent,
          topLeft = Offset(cx - w * 0.34f, pedY - h * 0.08f),
          size = Size(w * 0.68f, h * 0.16f),
          style = Stroke(width = 1.6.dp.toPx()),
        )

        val activeHalo = if (selectedSubDeck == 4) previewCosmeticId else equippedHaloId
        val activeWarp = if (selectedSubDeck == 3) previewCosmeticId else equippedWarpId
        val activeFinisher = if (selectedSubDeck == 1) previewCosmeticId else equippedFinisherId

        // Warp Signature Rift Aperture Rings (emphasized on Warp tab, subtle ambient otherwise)
        if (selectedSubDeck == 3 || activeWarp > 0) {
          val warpAlpha = if (selectedSubDeck == 3) 0.78f else 0.28f
          val warpR = size.minDimension * (0.24f + 0.07f * ((sin(holoPhase * 1.4f) + 1f) * 0.5f))
          drawCircle(
            color = stageAccent.copy(alpha = warpAlpha),
            radius = warpR,
            center = Offset(cx, cy),
            style = Stroke(width = 2.dp.toPx()),
          )
          for (i in 0 until 8) {
            val a = holoPhase * (if (activeWarp % 2 == 0) 1.1f else -1.3f) + (i * PI.toFloat() / 4f)
            drawLine(
              color = stageAccent.copy(alpha = warpAlpha * 0.85f),
              start = Offset(cx + cos(a) * (warpR * 0.68f), cy + sin(a) * (warpR * 0.68f)),
              end = Offset(cx + cos(a) * (warpR * 1.14f), cy + sin(a) * (warpR * 1.14f)),
              strokeWidth = 1.8.dp.toPx(),
            )
          }
        }

        // HVT Crown Halo Nodes around Ship
        val haloR = size.minDimension * 0.31f
        val haloAlpha = if (selectedSubDeck == 4) 0.92f else 0.40f
        drawCircle(
          color = stageAccent.copy(alpha = haloAlpha * 0.55f),
          radius = haloR,
          center = Offset(cx, cy),
          style = Stroke(width = 1.4.dp.toPx()),
        )
        val nodeCount = 4 + (activeHalo.coerceIn(0, 3) * 2)
        for (i in 0 until nodeCount) {
          val ang = holoPhase * 0.9f + (i * 2f * PI.toFloat() / nodeCount)
          val nx = cx + cos(ang) * haloR
          val ny = cy + sin(ang) * haloR
          drawCircle(color = stageAccent.copy(alpha = haloAlpha), radius = 3.2.dp.toPx(), center = Offset(nx, ny))
        }

        // Orbiting 4-point Rift Shard crystals on Sub-Deck 6 (Shard Vault)
        if (selectedSubDeck == 6) {
          val orbitR = size.minDimension * 0.36f
          for (cIdx in 0 until 6) {
            val ca = -holoPhase * 0.8f + (cIdx * 2f * PI.toFloat() / 6f)
            val sx = cx + cos(ca) * orbitR
            val sy = cy + sin(ca) * (orbitR * 0.72f)
            val cr = 5.5.dp.toPx()
            val shardPath =
              Path().apply {
                moveTo(sx, sy - cr)
                lineTo(sx + cr * 0.7f, sy)
                lineTo(sx, sy + cr)
                lineTo(sx - cr * 0.7f, sy)
                close()
              }
            val shardCol = if (cIdx % 2 == 0) ConduitSolarGold else Color(0xFFC084FC)
            drawPath(shardPath, shardCol.copy(alpha = 0.85f))
          }
        }

        // Kill Finisher Shockwave Pulse on Sub-Deck 1
        if (selectedSubDeck == 1) {
          val cyc = (holoPhase * 0.55f) % 1f
          val ringR =
            if (activeFinisher == 1) {
              size.minDimension * (0.42f * (1f - cyc).coerceIn(0.08f, 1f))
            } else {
              size.minDimension * (0.10f + 0.34f * cyc)
            }
          drawCircle(
            color = stageAccent.copy(alpha = (1f - cyc).coerceIn(0.15f, 0.9f)),
            radius = ringR,
            center = Offset(cx, cy),
            style = Stroke(width = 2.6.dp.toPx()),
          )
          for (sIdx in 0 until 10) {
            val sa = (sIdx * 2f * PI.toFloat() / 10f) + holoPhase * 0.4f
            drawCircle(
              color = Color.White.copy(alpha = (1f - cyc) * 0.85f),
              radius = 2.4.dp.toPx(),
              center = Offset(cx + cos(sa) * ringR, cy + sin(sa) * ringR),
            )
          }
        }
      }

      val bobOffsetPx = (sin(holoPhase * 1.25f) * 6f).roundToInt()
      val bankRotation = sin(holoPhase * 0.75f) * 11f
      Image(
        bitmap = shipBitmap,
        contentDescription = shipSpec.name,
        filterQuality = FilterQuality.High,
        modifier =
          Modifier.size(72.dp)
            .offset { IntOffset(0, bobOffsetPx - 4) }
            .graphicsLayer {
              rotationZ = bankRotation
            },
      )
    }

    // Bottom Compact Loadout & Fairness Readout
    Row(
      modifier =
        Modifier.fillMaxWidth()
          .background(ConduitTabletAbyss, RoundedCornerShape(7.dp))
          .border(1.dp, Color(0xFF1E3A5F), RoundedCornerShape(7.dp))
          .padding(horizontal = 8.dp, vertical = 5.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column {
        Text(
          text = "STARTING RESONANCE: $startingBounty RES (SCALES 100->500)",
          color = ConduitSolarGold,
          fontSize = 9.5.sp,
          fontWeight = FontWeight.ExtraBold,
          fontFamily = FontFamily.Monospace,
        )
        Text(
          text = "CONVERGENCES WON: $publicGameWins • ALL 10 HULLS OPEN AT 0",
          color = Color(0xFF94A3B8),
          fontSize = 9.5.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.Monospace,
        )
      }
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        ResonanceCrestIcon(sizeDp = 14.dp)
        Text(
          text = "PEAK $peakResonance",
          color = Color(0xFFC084FC),
          fontSize = 10.sp,
          fontWeight = FontWeight.Black,
          fontFamily = FontFamily.Monospace,
        )
      }
    }
  }
}

@Composable
private fun DailyDirectivesDeck(
  directives: List<DailyConduitDirective>,
  canRerollToday: Boolean,
  sweepBonusClaimed: Boolean,
  liveResonance: Int,
  isCompactPhone: Boolean,
  onClaimDirective: (Int) -> Unit,
  onRerollDirective: (Int) -> Unit,
  onClaimSweep: () -> Unit,
) {
  val completedCount = directives.count { it.isComplete }
  val allClaimedOrDone = directives.size >= 3 && directives.all { it.isComplete }

  Column(
    modifier = Modifier.fillMaxSize(),
    verticalArrangement = Arrangement.spacedBy(6.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        RiftShardGemIcon(sizeDp = 15.dp)
        Text(
          text = "3 DAILY DIRECTIVES // 250 DAILY OUTPUT",
          color = ConduitSolarGold,
          fontSize = if (isCompactPhone) 10.5.sp else 11.5.sp,
          fontWeight = FontWeight.ExtraBold,
          fontFamily = FontFamily.Monospace,
        )
      }
      Text(
        text = if (canRerollToday) "1 FREE REROLL READY" else "DAILY REROLL USED",
        color = if (canRerollToday) ConduitNexusTeal else Color(0xFF94A3B8),
        fontSize = 9.5.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace,
      )
    }

    LazyColumn(
      modifier = Modifier.weight(1f).fillMaxWidth(),
      verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      items(directives, key = { "${it.slotIndex}_${it.directiveId}" }) { dir ->
        Row(
          modifier =
            Modifier.fillMaxWidth()
              .background(
                if (dir.claimed) ConduitTabletElevated else ConduitTabletAbyss,
                RoundedCornerShape(8.dp),
              )
              .border(
                1.dp,
                when {
                  dir.claimed -> Color(0xFF10B981)
                  dir.isComplete -> ConduitSolarGold
                  else -> ConduitElectricCyan.copy(alpha = 0.45f)
                },
                RoundedCornerShape(8.dp),
              )
              .padding(horizontal = 10.dp, vertical = 6.dp)
              .testTag("daily_directive_card_${dir.slotIndex}"),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Column(modifier = Modifier.weight(1f).padding(end = 8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Text(
                text = dir.title,
                color = Color.White,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
              )
              Text(
                text = "${dir.progressValue.coerceAtMost(dir.targetValue)} / ${dir.targetValue}",
                color = if (dir.isComplete) Color(0xFF00E676) else ConduitElectricCyan,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
              )
            }
            Text(
              text = dir.subtitle,
              color = Color(0xFF94A3B8),
              fontSize = 9.8.sp,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Box(
              modifier =
                Modifier.fillMaxWidth()
                  .height(4.dp)
                  .clip(RoundedCornerShape(2.dp))
                  .background(ConduitTabletPanel)
            ) {
              Box(
                modifier =
                  Modifier.fillMaxWidth(dir.progressFraction)
                    .fillMaxHeight()
                    .background(
                      if (dir.isComplete) Color(0xFF00E676) else ConduitNexusTeal,
                      RoundedCornerShape(2.dp),
                    )
              )
            }
          }

          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
          ) {
            if (!dir.isComplete && !dir.claimed && canRerollToday) {
              Box(
                modifier =
                  Modifier.clip(RoundedCornerShape(6.dp))
                    .background(ConduitTabletPanel)
                    .border(1.dp, ConduitElectricCyan, RoundedCornerShape(6.dp))
                    .clickable { onRerollDirective(dir.slotIndex) }
                    .padding(horizontal = 8.dp, vertical = 5.dp)
                    .testTag("btn_reroll_directive_${dir.slotIndex}")
              ) {
                Text(
                  text = "REROLL",
                  color = ConduitElectricCyan,
                  fontSize = 9.5.sp,
                  fontWeight = FontWeight.ExtraBold,
                  fontFamily = FontFamily.Monospace,
                )
              }
            }

            when {
              dir.claimed -> {
                Box(
                  modifier =
                    Modifier.background(ConduitTabletPanel, RoundedCornerShape(6.dp))
                      .border(1.dp, Color(0xFF00E676), RoundedCornerShape(6.dp))
                      .padding(horizontal = 9.dp, vertical = 5.dp)
                ) {
                  Text(
                    text = "CLAIMED",
                    color = Color(0xFF00E676),
                    fontSize = 9.8.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace,
                  )
                }
              }

              dir.isComplete -> {
                Button(
                  onClick = { onClaimDirective(dir.slotIndex) },
                  colors =
                    ButtonDefaults.buttonColors(
                      containerColor = ConduitSolarGold,
                      contentColor = ConduitTabletAbyss,
                    ),
                  shape = RoundedCornerShape(6.dp),
                  contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                  modifier = Modifier.height(30.dp).testTag("btn_claim_directive_${dir.slotIndex}"),
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                  ) {
                    RiftShardGemIcon(sizeDp = 13.dp)
                    Text(
                      text = "CLAIM +${dir.shardReward}",
                      fontSize = 9.8.sp,
                      fontWeight = FontWeight.Black,
                      fontFamily = FontFamily.Monospace,
                    )
                  }
                }
              }

              else -> {
                Row(
                  modifier =
                    Modifier.background(ConduitTabletPanel, RoundedCornerShape(6.dp))
                      .border(1.dp, ConduitSolarGold.copy(alpha = 0.75f), RoundedCornerShape(6.dp))
                      .padding(horizontal = 8.dp, vertical = 5.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                  RiftShardGemIcon(sizeDp = 13.dp)
                  Text(
                    text = "+${dir.shardReward}",
                    color = ConduitSolarGold,
                    fontSize = 9.8.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace,
                  )
                }
              }
            }
          }
        }
      }

      // 3/3 Daily Conduit Sweep Bonus Chest (+100 Shards)
      item(key = "daily_sweep_chest") {
        Row(
          modifier =
            Modifier.fillMaxWidth()
              .background(
                if (sweepBonusClaimed) ConduitTabletElevated else ConduitTabletAbyss,
                RoundedCornerShape(8.dp),
              )
              .border(
                1.2.dp,
                if (sweepBonusClaimed) Color(0xFF10B981) else ConduitSolarGold,
                RoundedCornerShape(8.dp),
              )
              .padding(horizontal = 10.dp, vertical = 7.dp)
              .testTag("daily_sweep_bonus_card"),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text(
              text = "DAILY CONDUIT SWEEP BONUS ($completedCount / 3 DIRECTIVES)",
              color = ConduitSolarGold,
              fontSize = 11.sp,
              fontWeight = FontWeight.ExtraBold,
              fontFamily = FontFamily.Monospace,
            )
            Text(
              text = "Complete all 3 Daily Directives to unlock the +100 Shard Vault Sweep (250/day total).",
              color = Color(0xFFCBD5E1),
              fontSize = 9.5.sp,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }

          when {
            sweepBonusClaimed -> {
              Text(
                text = "SWEEP CLAIMED",
                color = Color(0xFF00E676),
                fontSize = 9.8.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
              )
            }

            allClaimedOrDone -> {
              Button(
                onClick = onClaimSweep,
                colors =
                  ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF00E676),
                    contentColor = ConduitTabletAbyss,
                  ),
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                modifier = Modifier.height(30.dp).testTag("btn_claim_daily_sweep"),
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                  RiftShardGemIcon(sizeDp = 13.dp)
                  Text(
                    text = "CLAIM +100",
                    fontSize = 9.8.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                  )
                }
              }
            }

            else -> {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
              ) {
                RiftShardGemIcon(sizeDp = 13.dp)
                Text(
                  text = "+100 AT 3/3",
                  color = ConduitSolarGold,
                  fontSize = 9.8.sp,
                  fontWeight = FontWeight.ExtraBold,
                  fontFamily = FontFamily.Monospace,
                )
              }
            }
          }
        }
      }

      // Compact 6-Tier Conduit Resonance & Starting Bounty Ladder
      item(key = "conduit_tier_ladder") {
        Column(
          modifier =
            Modifier.fillMaxWidth()
              .background(ConduitTabletAbyss, RoundedCornerShape(8.dp))
              .border(1.dp, ConduitElectricCyan.copy(alpha = 0.42f), RoundedCornerShape(8.dp))
              .padding(8.dp),
          verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            ResonanceCrestIcon(sizeDp = 14.dp)
            Text(
              text = "LIVE RESONANCE RANK TIERS & STARTING RESONANCE SCALING",
              color = ConduitElectricCyan,
              fontSize = 9.8.sp,
              fontWeight = FontWeight.ExtraBold,
              fontFamily = FontFamily.Monospace,
            )
          }
          Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
          ) {
            RiftProgressionCatalog.CONDUIT_RANK_TIERS.forEach { tier ->
              val isCurrent = liveResonance in tier.minResonance..tier.maxResonance
              val tc = Color(tier.accentColorHex)
              Column(
                modifier =
                  Modifier.background(
                    if (isCurrent) ConduitTabletElevated else ConduitTabletPanel,
                    RoundedCornerShape(6.dp),
                  )
                    .border(1.dp, if (isCurrent) tc else tc.copy(alpha = 0.40f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
              ) {
                Text(
                  text = tier.title,
                  color = tc,
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Black,
                  fontFamily = FontFamily.Monospace,
                )
                Text(
                  text = "${tier.minResonance}+ RES • ${tier.startingBounty} START RES",
                  color = Color.White,
                  fontSize = 9.5.sp,
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
}

@Composable
private fun ConduitCosmeticListDeck(
  title: String,
  categoryKey: String,
  items: List<CosmeticCatalogItem>,
  previewedId: Int,
  equippedId: Int,
  isItemUnlocked: (CosmeticCatalogItem) -> Boolean,
  onPreviewItem: (CosmeticCatalogItem) -> Unit,
  onUnlockOrEquip: (CosmeticCatalogItem) -> Unit,
) {
  Column(
    modifier = Modifier.fillMaxSize(),
    verticalArrangement = Arrangement.spacedBy(6.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = title,
        color = ConduitNexusTeal,
        fontSize = 11.sp,
        fontWeight = FontWeight.ExtraBold,
        fontFamily = FontFamily.Monospace,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.weight(1f),
      )
      Text(
        text = "ZERO COMBAT ADVANTAGE",
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
      items(items, key = { "${categoryKey}_${it.id}" }) { item ->
        val accent = Color(item.accentColorHex)
        val unlocked = isItemUnlocked(item)
        val isEquipped = equippedId == item.id
        val isPreviewing = previewedId == item.id

        Row(
          modifier =
            Modifier.fillMaxWidth()
              .clip(RoundedCornerShape(8.dp))
              .background(
                when {
                  isEquipped || isPreviewing -> ConduitTabletElevated
                  else -> ConduitTabletAbyss
                }
              )
              .border(
                width = if (isEquipped || isPreviewing) 1.4.dp else 1.dp,
                color = if (isEquipped || isPreviewing) accent else accent.copy(alpha = 0.42f),
                shape = RoundedCornerShape(8.dp),
              )
              .clickable { onPreviewItem(item) }
              .padding(horizontal = 10.dp, vertical = 7.dp)
              .testTag("cosmetic_item_${categoryKey}_${item.id}"),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Row(
            modifier = Modifier.weight(1f).padding(end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp),
          ) {
            Box(
              modifier =
                Modifier.size(18.dp)
                  .clip(CircleShape)
                  .background(accent.copy(alpha = 0.25f))
                  .border(1.5.dp, accent, CircleShape)
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                Text(
                  text = item.name.uppercase(Locale.US),
                  color = Color.White,
                  fontSize = 11.5.sp,
                  fontWeight = FontWeight.ExtraBold,
                )
                if (item.freeAtResonance > 0) {
                  ResonanceCrestIcon(sizeDp = 12.dp)
                }
                Text(
                  text =
                    if (item.freeAtResonance <= 0) "DEFAULT UNLOCK"
                    else "FREE AT ${item.freeAtResonance} PEAK",
                  color = accent,
                  fontSize = 9.5.sp,
                  fontWeight = FontWeight.Bold,
                  fontFamily = FontFamily.Monospace,
                )
              }
              Text(
                text = item.summary,
                color = Color(0xFF94A3B8),
                fontSize = 9.8.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }

          when {
            isEquipped -> {
              Box(
                modifier =
                  Modifier.background(ConduitTabletPanel, RoundedCornerShape(6.dp))
                    .border(1.dp, Color(0xFF00E676), RoundedCornerShape(6.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
              ) {
                Text(
                  text = "EQUIPPED",
                  color = Color(0xFF00E676),
                  fontSize = 9.8.sp,
                  fontWeight = FontWeight.ExtraBold,
                  fontFamily = FontFamily.Monospace,
                )
              }
            }

            unlocked -> {
              Button(
                onClick = { onUnlockOrEquip(item) },
                colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = ConduitTabletAbyss),
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                modifier = Modifier.height(30.dp),
              ) {
                Text(
                  text = "EQUIP",
                  fontSize = 9.8.sp,
                  fontWeight = FontWeight.Black,
                  fontFamily = FontFamily.Monospace,
                )
              }
            }

            else -> {
              Button(
                onClick = { onUnlockOrEquip(item) },
                colors =
                  ButtonDefaults.buttonColors(
                    containerColor = ConduitTabletPanel,
                    contentColor = ConduitSolarGold,
                  ),
                border = BorderStroke(1.dp, ConduitSolarGold),
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                modifier = Modifier.height(30.dp),
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                  RiftShardGemIcon(sizeDp = 13.dp)
                  Text(
                    text = "UNLOCK ${item.shardCost}",
                    fontSize = 9.8.sp,
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
}

@Composable
private fun ConduitFrameBaysDeck(
  activeSlotId: String,
  spentCustomizationPoints: Int,
  totalCustomizationPoints: Int,
  aiShipDesignTokens: Int,
  isTesterInfiniteShards: Boolean,
  isFrameUnlocked: (String) -> Boolean,
  onPreviewSlot: (String) -> Unit,
  onUnlockOrSelectSlot: (String) -> Unit,
  onRespecPoints: () -> Unit,
  onBuyForgeVoucher: () -> Unit,
  onOpenHangarShip10: () -> Unit,
) {
  val availablePts = (totalCustomizationPoints - spentCustomizationPoints).coerceAtLeast(0)

  Column(
    modifier = Modifier.fillMaxSize(),
    verticalArrangement = Arrangement.spacedBy(6.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = "RIFT FRAME BAYS (A–E) // FULL 100-PT PARITY AT 0",
        color = Color(0xFFEC4899),
        fontSize = 11.sp,
        fontWeight = FontWeight.ExtraBold,
        fontFamily = FontFamily.Monospace,
      )
      Box(
        modifier =
          Modifier.clip(RoundedCornerShape(6.dp))
            .background(ConduitTabletAbyss)
            .border(1.dp, Color(0xFFC084FC), RoundedCornerShape(6.dp))
            .clickable { onOpenHangarShip10() }
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .testTag("conduit_btn_open_hangar_ship10")
      ) {
        Text(
          text = "OPEN IN HANGAR DECK >",
          color = Color(0xFFE9D5FF),
          fontSize = 9.5.sp,
          fontWeight = FontWeight.Black,
          fontFamily = FontFamily.Monospace,
        )
      }
    }

    LazyColumn(
      modifier = Modifier.weight(1f).fillMaxWidth(),
      verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
      items(RiftProgressionCatalog.frameSlotGates, key = { it.slotId }) { gate ->
        val unlocked = isFrameUnlocked(gate.slotId)
        val isActive = activeSlotId.equals(gate.slotId, ignoreCase = true)

        Row(
          modifier =
            Modifier.fillMaxWidth()
              .clip(RoundedCornerShape(8.dp))
              .background(if (isActive || unlocked) ConduitTabletElevated else ConduitTabletAbyss)
              .border(
                1.dp,
                when {
                  isActive -> Color(0xFFEC4899)
                  unlocked -> Color(0xFF10B981)
                  else -> ConduitSolarGold.copy(alpha = 0.65f)
                },
                RoundedCornerShape(8.dp),
              )
              .clickable { onPreviewSlot(gate.slotId) }
              .padding(horizontal = 10.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Column(modifier = Modifier.weight(1f).padding(end = 8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              Text(
                text = "${gate.slotId.uppercase(Locale.US)} // ${gate.archetypeTitle.uppercase(Locale.US)}",
                color = if (unlocked) Color(0xFF00E676) else ConduitSolarGold,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
              )
              ResonanceCrestIcon(sizeDp = 12.dp)
              Text(
                text =
                  if (gate.freeAtResonance <= 0) "OPEN AT 0"
                  else "FREE AT ${gate.freeAtResonance} PEAK",
                color = ConduitElectricCyan,
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
              )
            }
            Text(
              text = gate.summary,
              color = Color(0xFF94A3B8),
              fontSize = 9.8.sp,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }

          if (unlocked) {
            Button(
              onClick = { onUnlockOrSelectSlot(gate.slotId) },
              colors =
                ButtonDefaults.buttonColors(
                  containerColor = ConduitTabletAbyss,
                  contentColor = if (isActive) Color.White else Color(0xFF00E676),
                ),
              border = BorderStroke(1.dp, if (isActive) Color(0xFFEC4899) else Color(0xFF00E676)),
              shape = RoundedCornerShape(6.dp),
              contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
              modifier = Modifier.height(29.dp),
            ) {
              Text(
                text = if (isActive) "ACTIVE BAY" else "SELECT BAY",
                fontSize = 9.8.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
              )
            }
          } else {
            Button(
              onClick = { onUnlockOrSelectSlot(gate.slotId) },
              colors =
                ButtonDefaults.buttonColors(
                  containerColor = ConduitTabletPanel,
                  contentColor = ConduitSolarGold,
                ),
              border = BorderStroke(1.dp, ConduitSolarGold),
              shape = RoundedCornerShape(6.dp),
              contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
              modifier = Modifier.height(29.dp),
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
              ) {
                RiftShardGemIcon(sizeDp = 13.dp)
                Text(
                  text = "UNLOCK ${gate.shardCost}",
                  fontSize = 9.8.sp,
                  fontWeight = FontWeight.ExtraBold,
                  fontFamily = FontFamily.Monospace,
                )
              }
            }
          }
        }
      }

      item(key = "frame_forge_utilities") {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Row(
            modifier =
              Modifier.weight(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(ConduitTabletAbyss)
                .border(1.dp, ConduitElectricCyan, RoundedCornerShape(8.dp))
                .clickable { onRespecPoints() }
                .padding(horizontal = 8.dp, vertical = 6.dp)
                .testTag("btn_respec_custom_pts"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Column(modifier = Modifier.weight(1f).padding(end = 6.dp)) {
              Text(
                text = "RIFT FRAME RESPEC",
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
              )
              Text(
                text = "$availablePts / $totalCustomizationPoints PTS FREE",
                color = Color(0xFF94A3B8),
                fontSize = 9.5.sp,
                fontFamily = FontFamily.Monospace,
              )
            }
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              RiftShardGemIcon(sizeDp = 14.dp)
              Text(
                text = if (isTesterInfiniteShards) "0 TEST" else "50",
                color = ConduitElectricCyan,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
              )
            }
          }

          Row(
            modifier =
              Modifier.weight(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(ConduitTabletAbyss)
                .border(1.dp, Color(0xFFC084FC), RoundedCornerShape(8.dp))
                .clickable { onBuyForgeVoucher() }
                .padding(horizontal = 8.dp, vertical = 6.dp)
                .testTag("btn_buy_ai_design_token"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Column(modifier = Modifier.weight(1f).padding(end = 6.dp)) {
              Text(
                text = "HULL FORGE VOUCHER",
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
              )
              Text(
                text = "$aiShipDesignTokens VOUCHERS HELD",
                color = Color(0xFF94A3B8),
                fontSize = 9.5.sp,
                fontFamily = FontFamily.Monospace,
              )
            }
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              RiftShardGemIcon(sizeDp = 14.dp)
              Text(
                text = if (isTesterInfiniteShards) "0 TEST" else "150",
                color = Color(0xFFC084FC),
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

@Composable
private fun ConduitMetricInline(label: String, value: String, color: Color) {
  Column {
    Text(
      text = label,
      color = Color(0xFF94A3B8),
      fontSize = 9.5.sp,
      fontWeight = FontWeight.Bold,
      fontFamily = FontFamily.Monospace,
      maxLines = 1,
    )
    Text(
      text = value,
      color = color,
      fontSize = 11.sp,
      fontWeight = FontWeight.ExtraBold,
      fontFamily = FontFamily.Monospace,
      maxLines = 1,
    )
  }
}
