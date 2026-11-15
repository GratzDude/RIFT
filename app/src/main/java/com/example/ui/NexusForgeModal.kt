package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.ViewGroup
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.LocalPilotPrefs
import com.example.engine.*
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin

enum class NexusForgeTab(val label: String, val accent: Color) {
 SHIP_COSMETICS("🛸 SHIP & COSMETICS", Color(0xFF38BDF8)),
 AUDIO_SFX("🎵 AUDIO & SFX LAB", Color(0xFF00F5D4)),
 BUFF_LAB("🧪 BUFF FORGE", Color(0xFF34D399)),
 SYNDICATE_CREST("🛡️ SYNDICATE CREST", Color(0xFFFACC15)),
 DEV_STUDIO_STAGE("🛠️ DEV STUDIO STAGE", Color(0xFFC084FC)),
}

private data class StageElementHotspot(
 val target: ForgeElementTarget,
 val normX: Float,
 val normY: Float,
 val hitRadiusNorm: Float = 0.055f,
)

private val FORGE_COLOR_SWATCHES = listOf(
 "#040914", "#0B1528", "#1E293B", "#00F5D4", "#38BDF8", "#A855F7",
 "#EC4899", "#FACC15", "#F97316", "#EF4444", "#10B981", "#E0F2FE",
)

@Composable
private fun ForgeChip(
 text: String,
 bg: Color,
 border: Color,
 textColor: Color = Color.White,
 fontSize: TextUnit = 8.2.sp,
 radius: Dp = 6.dp,
 px: Dp = 8.dp,
 py: Dp = 4.dp,
 borderWidth: Dp = 1.dp,
 enabled: Boolean = true,
 mono: Boolean = true,
 bold: Boolean = true,
 modifier: Modifier = Modifier,
 tag: String? = null,
 onClick: () -> Unit,
) {
 val m = modifier
  .clip(RoundedCornerShape(radius))
  .background(bg)
  .border(borderWidth, border, RoundedCornerShape(radius))
  .clickable(enabled = enabled, onClick = onClick)
  .padding(horizontal = px, vertical = py)
  .let { if (tag != null) it.testTag(tag) else it }
 Box(modifier = m, contentAlignment = Alignment.Center) {
  Text(
   text = text,
   color = textColor,
   fontSize = fontSize,
   fontWeight = if (bold) FontWeight.Black else FontWeight.SemiBold,
   fontFamily = if (mono) FontFamily.Monospace else FontFamily.SansSerif,
   maxLines = 1,
  )
 }
}

@Composable
fun NexusForgeModal(
 engine: RiftGameEngine,
 sfx: RiftSoundSynthesizer,
 localPrefs: LocalPilotPrefs,
 currentUserId: String,
 callsign: String,
 syndicateTag: String,
 pilotLevel: Int,
 riftShards: Long,
 selectedShipId: Int,
 activeCustomSlot: CustomShipLoadoutSlot,
 slot9BUnlocked: Boolean,
 slot9CUnlocked: Boolean,
 slot9DUnlocked: Boolean,
 slot9EUnlocked: Boolean,
 unlockFullNinthShipOverride: Boolean,
 sfxEnabled: Boolean,
 musicEnabled: Boolean,
 musicBpm: Int,
 musicVolume: Float,
 sfxVolume: Float,
 initialTab: NexusForgeTab = NexusForgeTab.SHIP_COSMETICS,
 isFullScreenStage: Boolean = false,
 onToggleFullScreenStage: (Boolean) -> Unit = {},
 onSelectShipForDeploy: (Int) -> Unit = {},
 onSaveCustomSlot: (CustomShipLoadoutSlot) -> Unit = {},
 onSwitchCustomSlot: (String) -> Unit = {},
 onUnlockCustomSlot: (String) -> Boolean = { false },
 onForgeAiHullDesign: (String, String) -> Pair<Boolean, String> = { _, _ -> Pair(false, "") },
 onSpendRiftShards: (Long) -> Boolean = { false },
 onToggleSfx: () -> Unit = {},
 onToggleMusic: () -> Unit = {},
 onChangeMusicBpm: (Int) -> Unit = {},
 onChangeMusicVolume: (Float) -> Unit = {},
 onChangeSfxVolume: (Float) -> Unit = {},
 onSelectMusicArchetype: (MusicSynthArchetype) -> Unit = {},
 onForgeCustomMusicPrompt: (String) -> Unit = {},
 onEnterHudDragArenaMode: () -> Unit = {},
 onSyncCalibrationToTesters: () -> Unit = {},
 onShowToast: (String) -> Unit = {},
) {
 val context = LocalContext.current
 val isCompactPhone = LocalConfiguration.current.screenHeightDp < 480
 val availableTabs = remember {
  if (RiftVisualForgeState.SHOW_DEV_FORGE) NexusForgeTab.entries
  else NexusForgeTab.entries.filter { it != NexusForgeTab.DEV_STUDIO_STAGE }
 }
 var activeTab by remember(initialTab) {
  mutableStateOf(if (initialTab in availableTabs) initialTab else NexusForgeTab.SHIP_COSMETICS)
 }
 val visualRev = RiftVisualForgeState.visualRevision
 val calibRev = RiftCalibrationState.calibrationRevision
 val shardBitmap = remember(visualRev, RiftSpriteBaker.spriteVisualRevision) {
  RiftSpriteBaker.riftShardIconBitmap.asImageBitmap()
 }

 DisposableEffect(Unit) { onDispose { onToggleFullScreenStage(false) } }

 Column(
  modifier = Modifier.fillMaxSize().padding(if (isCompactPhone) 4.dp else 8.dp).testTag("nexus_forge_modal"),
  verticalArrangement = Arrangement.spacedBy(if (isCompactPhone) 5.dp else 7.dp),
 ) {
  Row(
   modifier = Modifier.fillMaxWidth(),
   horizontalArrangement = Arrangement.SpaceBetween,
   verticalAlignment = Alignment.CenterVertically,
  ) {
   Row(
    modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState()),
    horizontalArrangement = Arrangement.spacedBy(6.dp),
    verticalAlignment = Alignment.CenterVertically,
   ) {
    availableTabs.forEach { tab ->
     val isSel = activeTab == tab
     ForgeChip(
      text = tab.label,
      bg = if (isSel) tab.accent.copy(alpha = 0.24f) else Color(0xCC091324),
      border = if (isSel) tab.accent else tab.accent.copy(alpha = 0.45f),
      textColor = if (isSel) Color.White else tab.accent,
      fontSize = if (isCompactPhone) 8.8.sp else 9.8.sp,
      radius = 8.dp,
      px = if (isCompactPhone) 8.dp else 11.dp,
      py = if (isCompactPhone) 4.dp else 6.dp,
      borderWidth = if (isSel) 1.3.dp else 1.dp,
      tag = "forge_subtab_${tab.name.lowercase(Locale.US)}",
     ) {
      activeTab = tab
      if (tab != NexusForgeTab.DEV_STUDIO_STAGE && isFullScreenStage) onToggleFullScreenStage(false)
     }
    }
   }
   Spacer(modifier = Modifier.width(8.dp))
   Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
    if (activeTab == NexusForgeTab.DEV_STUDIO_STAGE) {
     ForgeChip(
      text = if (isFullScreenStage) "⊡ EXIT FULL STAGE" else "⛶ EXPAND STAGE",
      bg = if (isFullScreenStage) Color(0xFF3B1366) else Color(0xFF0E1D34),
      border = Color(0xFFC084FC),
      textColor = Color(0xFFE9D5FF),
      fontSize = 8.5.sp,
      radius = 7.dp,
      tag = "forge_btn_toggle_fullscreen_stage",
     ) { onToggleFullScreenStage(!isFullScreenStage) }
    }
    Row(
     modifier = Modifier.clip(RoundedCornerShape(7.dp))
      .background(Color(0xFF160E2C))
      .border(1.dp, Color(0xFFA855F7), RoundedCornerShape(7.dp))
      .padding(horizontal = 8.dp, vertical = 4.dp),
     verticalAlignment = Alignment.CenterVertically,
     horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
     Image(bitmap = shardBitmap, contentDescription = "Rift Shards", modifier = Modifier.size(13.dp))
     Text("❖ $riftShards • LVL $pilotLevel", color = Color(0xFFE9D5FF), fontSize = 9.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
    }
   }
  }

  Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
   when (activeTab) {
    NexusForgeTab.SHIP_COSMETICS -> ShipAndCosmeticTurntableBay(
     localPrefs, currentUserId, pilotLevel, riftShards, selectedShipId, activeCustomSlot,
     slot9BUnlocked, slot9CUnlocked, slot9DUnlocked, slot9EUnlocked, unlockFullNinthShipOverride,
     isCompactPhone, onSelectShipForDeploy, onSaveCustomSlot, onSwitchCustomSlot,
     onUnlockCustomSlot, onForgeAiHullDesign, onSpendRiftShards, onShowToast,
    )
    NexusForgeTab.AUDIO_SFX -> SoundtrackAndSfxForgeBay(
     context, sfx, sfxEnabled, musicEnabled, musicBpm, musicVolume, sfxVolume, isCompactPhone,
     onToggleSfx, onToggleMusic, onChangeMusicBpm, onChangeMusicVolume, onChangeSfxVolume,
     onSelectMusicArchetype, onForgeCustomMusicPrompt, onShowToast,
    )
    NexusForgeTab.BUFF_LAB -> CustomBuffForgeBay(context, engine, sfx, calibRev, onShowToast)
    NexusForgeTab.SYNDICATE_CREST -> SyndicateCrestForgeBay(context, sfx, syndicateTag, callsign, visualRev, onShowToast)
    NexusForgeTab.DEV_STUDIO_STAGE -> DevPlayfieldAndHudStudioBay(
     context, engine, sfx, visualRev, calibRev,
     onEnterHudDragArenaMode, onSyncCalibrationToTesters, onShowToast,
    )
   }
  }
 }
}

@Composable
private fun ShipAndCosmeticTurntableBay(
 localPrefs: LocalPilotPrefs,
 currentUserId: String,
 pilotLevel: Int,
 riftShards: Long,
 selectedShipId: Int,
 activeCustomSlot: CustomShipLoadoutSlot,
 slot9BUnlocked: Boolean,
 slot9CUnlocked: Boolean,
 slot9DUnlocked: Boolean,
 slot9EUnlocked: Boolean,
 unlockFullNinthShipOverride: Boolean,
 isCompactPhone: Boolean,
 onSelectShipForDeploy: (Int) -> Unit,
 onSaveCustomSlot: (CustomShipLoadoutSlot) -> Unit,
 onSwitchCustomSlot: (String) -> Unit,
 onUnlockCustomSlot: (String) -> Boolean,
 onForgeAiHullDesign: (String, String) -> Pair<Boolean, String>,
 onSpendRiftShards: (Long) -> Boolean,
 onShowToast: (String) -> Unit,
) {
 var inspectedShipId by remember(selectedShipId) { mutableIntStateOf(selectedShipId.coerceIn(1, 10)) }
 var turntableAngleDeg by remember { mutableFloatStateOf(-90f) }
 var autoSpin by remember { mutableStateOf(true) }
 var cosmeticCategory by remember { mutableStateOf("HULL_TRIM") }
 var cosmeticRefreshTick by remember { mutableIntStateOf(0) }
 var hullPromptDraft by remember(activeCustomSlot.slotId) { mutableStateOf("") }
 var hullTitleDraft by remember(activeCustomSlot.customName) { mutableStateOf(activeCustomSlot.customName) }
 var hullStatusBanner by remember { mutableStateOf<String?>(null) }

 val infiniteTransition = rememberInfiniteTransition(label = "ship_turntable_spin")
 val spinPhase by infiniteTransition.animateFloat(
  initialValue = 0f,
  targetValue = 360f,
  animationSpec = infiniteRepeatable(tween(11000, easing = LinearEasing), RepeatMode.Restart),
  label = "turntable_deg",
 )

 val spriteVer = RiftSpriteBaker.customShip9Version
 val allShips = remember(activeCustomSlot, spriteVer) { RiftCodex.getAllNineShips() }
 val currentSpec = allShips.firstOrNull { it.id == inspectedShipId } ?: allShips.first()
 val cleanShipName = if (currentSpec.id == 10) "RIFT FRAME" else currentSpec.name.replace(Regex("^[0-9]+-"), "").uppercase(Locale.US)
 val equippedTrimId = remember(cosmeticRefreshTick, currentUserId) { localPrefs.getEquippedCosmeticId("HULL_TRIM", currentUserId) }
 val activeTrimStyleIndex = equippedTrimId.coerceIn(0, 3)
 RiftSpriteBaker.activeHullTrimStyle = activeTrimStyleIndex

 Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
  Column(
   modifier = Modifier.weight(0.46f).fillMaxHeight()
    .background(Color(0xFF071120), RoundedCornerShape(10.dp))
    .border(1.dp, Color(0xFF1E3A5F), RoundedCornerShape(10.dp))
    .padding(10.dp),
   verticalArrangement = Arrangement.SpaceBetween,
  ) {
   Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
    Column(modifier = Modifier.weight(1f)) {
     Text("360° HOLOGRAPHIC TURNTABLE", color = Color(0xFF00F5D4), fontSize = 9.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
     Text("$cleanShipName • ${currentSpec.role.uppercase(Locale.US)}", color = Color.White, fontSize = if (isCompactPhone) 12.sp else 14.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
    ForgeChip(
     text = if (autoSpin) "⟳ AUTO-SPIN" else "✋ DRAG SPIN",
     bg = if (autoSpin) Color(0xFF0E4F5C) else Color(0xFF132238),
     border = Color(0xFF00F5D4),
     textColor = Color(0xFF00F5D4),
     px = 7.dp,
    ) { autoSpin = !autoSpin }
   }

   val effectiveHeadingDeg = if (autoSpin) spinPhase else turntableAngleDeg
   Box(
    modifier = Modifier.fillMaxWidth().weight(1f).padding(vertical = 6.dp)
     .clip(RoundedCornerShape(10.dp))
     .background(Color(0xFF040914))
     .border(1.dp, Color(0x5538BDF8), RoundedCornerShape(10.dp))
     .pointerInput(Unit) {
      detectDragGestures { change, dragAmount ->
       change.consume()
       autoSpin = false
       turntableAngleDeg = (turntableAngleDeg + dragAmount.x * 0.85f) % 360f
      }
     }
     .testTag("forge_ship_turntable_canvas"),
    contentAlignment = Alignment.Center,
   ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
     val cx = size.width * 0.5f
     val cy = size.height * 0.52f
     val ringR = size.minDimension * 0.38f
     drawCircle(
      brush = Brush.radialGradient(listOf(Color(0x3800F5D4), Color(0x1438BDF8), Color.Transparent), center = Offset(cx, cy), radius = ringR * 1.35f),
      radius = ringR * 1.35f,
      center = Offset(cx, cy),
     )
     drawCircle(Color(0x4438BDF8), ringR, Offset(cx, cy), style = Stroke(1.5.dp.toPx()))
     drawCircle(Color(0x8800F5D4), ringR * 0.72f, Offset(cx, cy), style = Stroke(1.dp.toPx()))
     for (tick in 0 until 24) {
      val a = (tick * 15f + effectiveHeadingDeg * 0.25f) * (PI.toFloat() / 180f)
      val r0 = if (tick % 3 == 0) ringR * 0.88f else ringR * 0.94f
      drawLine(Color(0x667DD3FC), Offset(cx + cos(a) * r0, cy + sin(a) * r0), Offset(cx + cos(a) * ringR, cy + sin(a) * ringR), 1.2f)
     }
     drawIntoCanvas { canvas ->
      val nc = canvas.nativeCanvas
      nc.save()
      nc.translate(cx, cy)
      nc.rotate(effectiveHeadingDeg)
      val shipRadius = (size.minDimension * 0.27f).coerceIn(28f, 78f)
      if (currentSpec.id == 10) {
       val bmp = RiftSpriteBaker.customShip9AlliedBitmap
       nc.drawBitmap(bmp, null, android.graphics.RectF(-shipRadius, -shipRadius, shipRadius, shipRadius), null)
      } else {
       RiftSpriteBaker.drawMetallicShipOnCanvas(nc, 0f, 0f, shipRadius, currentSpec, isAllied = true, hullTrimStyle = activeTrimStyleIndex)
      }
      nc.restore()
     }
    }
    Text("DRAG HORIZONTALLY TO ROTATE 360°", color = Color(0xFF64748B), fontSize = 8.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 5.dp))
   }

   Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
    Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
     allShips.forEach { ship ->
      val shortLbl = if (ship.id == 10) "10 FRAME" else "${ship.id} ${ship.name.replace(Regex("^[0-9]+-"), "").take(6).uppercase(Locale.US)}"
      val sel = ship.id == inspectedShipId
      ForgeChip(
       text = shortLbl,
       bg = if (sel) Color(0xFF0E3A5D) else Color(0xFF0A1426),
       border = if (sel) Color(0xFF00F5D4) else Color(0xFF334155),
       textColor = if (sel) Color.White else Color(0xFF94A3B8),
       px = 7.dp,
      ) { inspectedShipId = ship.id }
     }
    }
    Button(
     onClick = { onSelectShipForDeploy(inspectedShipId) },
     colors = ButtonDefaults.buttonColors(containerColor = if (inspectedShipId == selectedShipId) Color(0xFF15803D) else Color(0xFF0284C7)),
     border = BorderStroke(1.dp, Color(0xFF00F5D4)),
     shape = RoundedCornerShape(8.dp),
     modifier = Modifier.fillMaxWidth().height(34.dp).testTag("forge_btn_equip_ship"),
    ) {
     Text(
      text = if (inspectedShipId == selectedShipId) "✓ $cleanShipName EQUIPPED FOR DEPLOY" else "🚀 EQUIP $cleanShipName FOR DEPLOY",
      color = Color.White,
      fontSize = 10.sp,
      fontWeight = FontWeight.Black,
      fontFamily = FontFamily.Monospace,
     )
    }
   }
  }

  Column(
   modifier = Modifier.weight(0.54f).fillMaxHeight()
    .background(Color(0xFF071120), RoundedCornerShape(10.dp))
    .border(1.dp, Color(0xFF1E3A5F), RoundedCornerShape(10.dp))
    .padding(10.dp)
    .verticalScroll(rememberScrollState()),
   verticalArrangement = Arrangement.spacedBy(9.dp),
  ) {
   Column(
    modifier = Modifier.fillMaxWidth().background(Color(0xFF120B24), RoundedCornerShape(8.dp)).border(1.dp, Color(0xFFA855F7), RoundedCornerShape(8.dp)).padding(9.dp),
    verticalArrangement = Arrangement.spacedBy(6.dp),
   ) {
    val normSlot = CustomShipLoadoutSlot.normalizeSlotId(activeCustomSlot.slotId)
    val budget = RiftProgressionCatalog.getRiftFramePointBudget(pilotLevel, unlockFullNinthShipOverride)
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
     Text("✨ RIFT FRAME MODULAR HULL FORGE ($normSlot)", color = Color(0xFFE9D5FF), fontSize = 10.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
     Text("${activeCustomSlot.totalAllocatedPoints()}/$budget PTS", color = Color(0xFF00F5D4), fontSize = 9.5.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
    }
    Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
     val frames = listOf("Frame A" to true, "Frame B" to slot9BUnlocked, "Frame C" to slot9CUnlocked, "Frame D" to slot9DUnlocked, "Frame E" to slot9EUnlocked)
     frames.forEach { (slotName, unlocked) ->
      val isAct = normSlot.equals(slotName, ignoreCase = true)
      val gate = RiftProgressionCatalog.getFrameSlotGate(slotName)
      ForgeChip(
       text = if (unlocked) slotName.uppercase(Locale.US) else "🔒 ${slotName.uppercase(Locale.US)} (❖${gate.shardCost})",
       bg = if (isAct) Color(0xFF4C1D95) else Color(0xFF0B1324),
       border = if (isAct) Color(0xFFEC4899) else Color(0xFF475569),
       textColor = if (isAct) Color.White else Color(0xFFCBD5E1),
      ) {
       inspectedShipId = 10
       if (unlocked) onSwitchCustomSlot(slotName)
       else if (!onUnlockCustomSlot(slotName)) onShowToast("Requires Pilot Level ${gate.freeAtLevel} or ❖ ${gate.shardCost} Rift Shards")
      }
     }
    }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
     ForgeTextInputBox(hullTitleDraft, { hullTitleDraft = it.take(18) }, "Hull Name...", Modifier.weight(0.38f))
     ForgeTextInputBox(hullPromptDraft, { hullPromptDraft = it.take(120) }, "Describe hull (e.g. obsidian stealth delta wing with gold trim)...", Modifier.weight(0.62f))
    }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
     Row(modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
      listOf("Obsidian Stealth Wing", "Sovereign Gold Dreadnought", "Bio-Cyan Twin Manta", "Amethyst Rift Interceptor").forEach { preset ->
       ForgeChip(preset, Color(0xFF1E1436), Color(0x66C084FC), Color(0xFFD8B4FE), fontSize = 7.8.sp, radius = 5.dp, px = 6.dp, py = 3.dp, mono = false, bold = false) {
        hullPromptDraft = preset
       }
      }
     }
     Spacer(modifier = Modifier.width(6.dp))
     val isFirstFree = activeCustomSlot.aiGenerationsCount == 0
     Box(
      modifier = Modifier.clip(RoundedCornerShape(7.dp))
       .background(Brush.horizontalGradient(listOf(Color(0xFF6D28D9), Color(0xFFBE185D))))
       .border(1.dp, Color(0xFFEC4899), RoundedCornerShape(7.dp))
       .clickable {
        inspectedShipId = 10
        val (ok, msg) = onForgeAiHullDesign(hullPromptDraft.ifBlank { "Sleek obsidian cyan interceptor" }, hullTitleDraft)
        hullStatusBanner = msg
        if (ok) onSaveCustomSlot(RiftCodex.activeCustomShipSlot)
       }
       .padding(horizontal = 10.dp, vertical = 6.dp)
       .testTag("forge_btn_synthesize_hull"),
     ) {
      Text(if (isFirstFree) "✨ FORGE HULL (FREE)" else "✨ FORGE (❖ 150)", color = Color.White, fontSize = 8.5.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
     }
    }
    if (!hullStatusBanner.isNullOrBlank()) {
     Text(hullStatusBanner!!, color = Color(0xFF67E8F9), fontSize = 8.5.sp, fontFamily = FontFamily.Monospace)
    }
   }

   Column(
    modifier = Modifier.fillMaxWidth().background(Color(0xFF0A1628), RoundedCornerShape(8.dp)).border(1.dp, Color(0xFF1E3A5F), RoundedCornerShape(8.dp)).padding(9.dp),
    verticalArrangement = Arrangement.spacedBy(7.dp),
   ) {
    Text("🎨 ZERO-ADVANTAGE COSMETIC VAULT", color = Color(0xFF00F5D4), fontSize = 10.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
    Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
     listOf("HULL_TRIM" to "HULL TRIMS", "KILL_FINISHER" to "KILL FINISHERS", "WARP_SIGNATURE" to "WARP TRAILS", "HVT_HALO" to "HVT CROWNS").forEach { (cat, title) ->
      val sel = cosmeticCategory == cat
      ForgeChip(title, if (sel) Color(0xFF0E4F5C) else Color(0xFF07101E), if (sel) Color(0xFF00F5D4) else Color(0xFF334155), if (sel) Color.White else Color(0xFF94A3B8)) {
       cosmeticCategory = cat
      }
     }
    }
    val items: List<CosmeticCatalogItem> = remember(cosmeticCategory) {
     when (cosmeticCategory) {
      "HULL_TRIM" -> RiftProgressionCatalog.hullTrims
      "KILL_FINISHER" -> RiftProgressionCatalog.killFinishers
      "WARP_SIGNATURE" -> RiftProgressionCatalog.warpSignatures
      "HVT_HALO" -> RiftProgressionCatalog.hvtHalos
      else -> RiftProgressionCatalog.hullTrims
     }
    }
    val equippedForCat = remember(cosmeticCategory, cosmeticRefreshTick, currentUserId) {
     localPrefs.getEquippedCosmeticId(cosmeticCategory, currentUserId)
    }
    for (item in items) {
     val unlockedByLvl = pilotLevel >= item.freeAtLevel || item.shardCost <= 0L
     val unlockedSaved = remember(cosmeticCategory, item.id, cosmeticRefreshTick, pilotLevel, currentUserId) {
      localPrefs.isCosmeticUnlocked(cosmeticCategory, item.id, pilotLevel, currentUserId)
     }
     val isUnlocked = unlockedByLvl || unlockedSaved
     val isEquipped = equippedForCat == item.id
     Row(
      modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(7.dp))
       .background(if (isEquipped) Color(0xFF0D283A) else Color(0xFF060E1B))
       .border(1.dp, if (isEquipped) Color(0xFF00F5D4) else Color(0xFF1E293B), RoundedCornerShape(7.dp))
       .padding(horizontal = 9.dp, vertical = 6.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
     ) {
      Row(modifier = Modifier.weight(1f).padding(end = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
       Box(modifier = Modifier.size(16.dp).clip(CircleShape).background(Color(item.accentColorHex)).border(1.dp, Color.White.copy(alpha = 0.7f), CircleShape))
       Column {
        Text(item.name, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Text("${item.summary} • Free at Lv ${item.freeAtLevel}", color = Color(0xFF94A3B8), fontSize = 8.2.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
       }
      }
      ForgeChip(
       text = when { isEquipped -> "✓ EQUIPPED"; isUnlocked -> "EQUIP"; else -> "UNLOCK ❖${item.shardCost}" },
       bg = when { isEquipped -> Color(0xFF065F46); isUnlocked -> Color(0xFF0E7490); else -> Color(0xFF3B1366) },
       border = when { isEquipped -> Color(0xFF34D399); isUnlocked -> Color(0xFF00F5D4); else -> Color(0xFFA855F7) },
       fontSize = 8.sp,
      ) {
       if (isEquipped) {
        if (item.id != 0) { localPrefs.setEquippedCosmeticId(cosmeticCategory, 0, currentUserId); cosmeticRefreshTick++ }
       } else if (isUnlocked) {
        localPrefs.setCosmeticUnlocked(cosmeticCategory, item.id, true, currentUserId)
        localPrefs.setEquippedCosmeticId(cosmeticCategory, item.id, currentUserId)
        cosmeticRefreshTick++
        onShowToast("✓ Equipped ${item.name}")
       } else if (riftShards >= item.shardCost && onSpendRiftShards(item.shardCost)) {
        localPrefs.setCosmeticUnlocked(cosmeticCategory, item.id, true, currentUserId)
        localPrefs.setEquippedCosmeticId(cosmeticCategory, item.id, currentUserId)
        cosmeticRefreshTick++
        onShowToast("❖ Unlocked & Equipped ${item.name}")
       } else {
        onShowToast("Requires Lv ${item.freeAtLevel} or ❖ ${item.shardCost} Rift Shards")
       }
      }
     }
    }
   }
  }
 }
}

@Composable
private fun SoundtrackAndSfxForgeBay(
 context: Context,
 sfx: RiftSoundSynthesizer,
 sfxEnabled: Boolean,
 musicEnabled: Boolean,
 musicBpm: Int,
 musicVolume: Float,
 sfxVolume: Float,
 isCompactPhone: Boolean,
 onToggleSfx: () -> Unit,
 onToggleMusic: () -> Unit,
 onChangeMusicBpm: (Int) -> Unit,
 onChangeMusicVolume: (Float) -> Unit,
 onChangeSfxVolume: (Float) -> Unit,
 onSelectMusicArchetype: (MusicSynthArchetype) -> Unit,
 onForgeCustomMusicPrompt: (String) -> Unit,
 onShowToast: (String) -> Unit,
) {
 var activeSlotIdx by remember { mutableIntStateOf(sfx.currentTrackSlot) }
 var musicPromptDraft by remember { mutableStateOf("") }
 var sfxPromptDraft by remember { mutableStateOf("") }
 var sfxRecipeState by remember { mutableStateOf(sfx.currentSfxRecipe) }
 var drumsOn by remember { mutableStateOf(sfx.currentMusicRecipe.drumsEnabled) }

 Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
  Column(
   modifier = Modifier.weight(0.50f).fillMaxHeight()
    .background(Color(0xFF071120), RoundedCornerShape(10.dp))
    .border(1.dp, Color(0xFF1E3A5F), RoundedCornerShape(10.dp))
    .padding(10.dp)
    .verticalScroll(rememberScrollState()),
   verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
   Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
    Column(modifier = Modifier.weight(1f)) {
     Text("🎶 PROCEDURAL SOUNDTRACK LAB (60–180 BPM)", color = Color(0xFF00F5D4), fontSize = 10.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
     Text(sfx.currentMusicRecipe.customPromptSummary.replace("\u26A1", "Flux"), color = Color(0xFF7DD3FC), fontSize = 8.8.sp, fontFamily = FontFamily.Monospace, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
    ForgeChip(
     text = if (musicEnabled) "🎵 MUSIC ON" else "🔇 MUSIC OFF",
     bg = if (musicEnabled) Color(0xFF0E4F5C) else Color(0xFF361620),
     border = if (musicEnabled) Color(0xFF00F5D4) else Color(0xFFEF4444),
     fontSize = 8.5.sp,
     radius = 7.dp,
    ) { onToggleMusic() }
   }

   Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
    listOf(0 to "TRACK A", 1 to "TRACK B", 2 to "TRACK C").forEach { (idx, title) ->
     val sel = activeSlotIdx == idx
     val slotRec = sfx.musicTrackSlots.getOrNull(idx) ?: sfx.currentMusicRecipe
     Box(
      modifier = Modifier.weight(1f).clip(RoundedCornerShape(7.dp))
       .background(if (sel) Color(0xFF0E3A5D) else Color(0xFF0A1424))
       .border(1.dp, if (sel) Color(0xFF00F5D4) else Color(0xFF334155), RoundedCornerShape(7.dp))
       .clickable {
        sfx.selectTrackSlot(idx)
        activeSlotIdx = idx
        drumsOn = sfx.currentMusicRecipe.drumsEnabled
        onChangeMusicBpm(sfx.musicBpm)
        RiftCalibrationState.savePersistedLayout(context)
       }
       .padding(horizontal = 6.dp, vertical = 5.dp)
       .testTag("forge_music_slot_$idx"),
     ) {
      Column {
       Text(title, color = if (sel) Color(0xFF00F5D4) else Color.White, fontSize = 8.8.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
       Text("${slotRec.bpm} BPM", color = Color(0xFF94A3B8), fontSize = 7.8.sp, fontFamily = FontFamily.Monospace)
      }
     }
    }
    ForgeChip("💾 SAVE", Color(0xFF143D34), Color(0xFF34D399), Color(0xFF6EE7B7), fontSize = 8.5.sp, radius = 7.dp, py = 8.dp) {
     sfx.saveCurrentMusicToSlot(activeSlotIdx)
     RiftCalibrationState.savePersistedLayout(context)
     onShowToast("💾 Saved to Track ${('A' + activeSlotIdx)}")
    }
   }

   Column(
    modifier = Modifier.fillMaxWidth().background(Color(0xFF050C18), RoundedCornerShape(8.dp)).border(1.dp, Color(0xFF1E293B), RoundedCornerShape(8.dp)).padding(8.dp),
    verticalArrangement = Arrangement.spacedBy(4.dp),
   ) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
     Text("TEMPO: $musicBpm BPM", color = Color(0xFFFBBF24), fontSize = 9.5.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
     ForgeChip(
      text = if (drumsOn) "🥁 DRUMS: ON" else "🥁 DRUMS: AMBIENT",
      bg = if (drumsOn) Color(0xFF1E3A5F) else Color(0xFF1F172A),
      border = Color(0xFF38BDF8),
      fontSize = 8.sp,
      radius = 5.dp,
      px = 7.dp,
      py = 2.5.dp,
     ) {
      drumsOn = !drumsOn
      sfx.currentMusicRecipe = sfx.currentMusicRecipe.copy(drumsEnabled = drumsOn)
      sfx.saveCurrentMusicToSlot(activeSlotIdx)
      RiftCalibrationState.savePersistedLayout(context)
     }
    }
    Slider(
     value = musicBpm.coerceIn(60, 180).toFloat(),
     onValueChange = { onChangeMusicBpm(it.roundToInt().coerceIn(60, 180)) },
     onValueChangeFinished = { RiftCalibrationState.savePersistedLayout(context) },
     valueRange = 60f..180f,
     colors = SliderDefaults.colors(thumbColor = Color(0xFFF59E0B), activeTrackColor = Color(0xFFF59E0B), inactiveTrackColor = Color(0xFF132642)),
     modifier = Modifier.fillMaxWidth().height(24.dp),
    )
    Text("MUSIC VOLUME: ${(musicVolume * 100f).roundToInt()}%", color = Color(0xFF00F5D4), fontSize = 8.8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
    Slider(
     value = musicVolume.coerceIn(0f, 1f),
     onValueChange = { onChangeMusicVolume(it.coerceIn(0f, 1f)) },
     valueRange = 0f..1f,
     colors = SliderDefaults.colors(thumbColor = Color(0xFF00F5D4), activeTrackColor = Color(0xFF00F5D4), inactiveTrackColor = Color(0xFF132642)),
     modifier = Modifier.fillMaxWidth().height(22.dp),
    )
   }

   Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
    MusicSynthArchetype.entries.forEach { arch ->
     val sel = sfx.currentMusicRecipe.archetype == arch
     ForgeChip(
      text = "${arch.label.replace("\u26A1", "")} (${arch.defaultBpm})",
      bg = if (sel) Color(0xFF0E4F5C) else Color(0xFF0A1426),
      border = if (sel) Color(0xFF00F5D4) else Color(0xFF334155),
      textColor = if (sel) Color(0xFF00F5D4) else Color(0xFFE2E8F0),
      mono = false,
     ) {
      onSelectMusicArchetype(arch)
      sfx.saveCurrentMusicToSlot(activeSlotIdx)
      drumsOn = sfx.currentMusicRecipe.drumsEnabled
      RiftCalibrationState.savePersistedLayout(context)
     }
    }
   }

   Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
    ForgeTextInputBox(musicPromptDraft, { musicPromptDraft = it.take(120) }, "Prompt track (e.g. dark cyber bassline 142 bpm, calm cosmic pad)...", Modifier.weight(1f))
    ForgeChip(
     text = "🎶 FORGE",
     bg = Color(0xFF4C1D95),
     border = Color(0xFFC084FC),
     fontSize = 8.8.sp,
     radius = 7.dp,
     px = 10.dp,
     py = 8.dp,
     tag = "forge_btn_synthesize_music",
    ) {
     onForgeCustomMusicPrompt(musicPromptDraft.ifBlank { "Driving dark synthwave pulse 128 bpm" })
     sfx.saveCurrentMusicToSlot(activeSlotIdx)
     drumsOn = sfx.currentMusicRecipe.drumsEnabled
     RiftCalibrationState.savePersistedLayout(context)
    }
   }
  }

  Column(
   modifier = Modifier.weight(0.50f).fillMaxHeight()
    .background(Color(0xFF071120), RoundedCornerShape(10.dp))
    .border(1.dp, Color(0xFF1E3A5F), RoundedCornerShape(10.dp))
    .padding(10.dp)
    .verticalScroll(rememberScrollState()),
   verticalArrangement = Arrangement.spacedBy(7.dp),
  ) {
   Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
    Column(modifier = Modifier.weight(1f)) {
     Text("🔊 WEAPON & COMBAT SFX SYNTHESIZER", color = Color(0xFF38BDF8), fontSize = 10.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
     Text(sfxRecipeState.promptUsed.replace("\u26A1", "Flux"), color = Color(0xFF94A3B8), fontSize = 8.5.sp, fontFamily = FontFamily.Monospace, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
     ForgeChip(
      text = if (sfxEnabled) "🔊 SFX ON" else "🔇 MUTED",
      bg = if (sfxEnabled) Color(0xFF103328) else Color(0xFF361620),
      border = if (sfxEnabled) Color(0xFF34D399) else Color(0xFFEF4444),
      fontSize = 8.sp,
      px = 7.dp,
     ) { onToggleSfx() }
     ForgeChip("↺ RESET", Color(0xFF1E293B), Color(0xFF64748B), Color(0xFFE2E8F0), fontSize = 8.sp, px = 7.dp) {
      sfx.applySfxRecipe(SfxForgeRecipe())
      sfxRecipeState = sfx.currentSfxRecipe
      RiftCalibrationState.savePersistedLayout(context)
      sfx.previewForgedPrizeChime()
     }
    }
   }

   Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
    listOf(
     "🔫 LASER" to { sfx.previewForgedLaser() },
     "💥 BOOM" to { sfx.previewForgedExplosion() },
     "🛡️ SHIELD" to { sfx.previewForgedShieldHit() },
     "❖ CHIME" to { sfx.previewForgedPrizeChime() },
    ).forEach { (lbl, trigger) ->
     ForgeChip(lbl, Color(0xFF0C1E36), Color(0xFF38BDF8), Color(0xFFE0F2FE), fontSize = if (isCompactPhone) 7.8.sp else 8.4.sp, radius = 7.dp, py = 6.dp, modifier = Modifier.weight(1f)) {
      trigger()
     }
    }
   }

   Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
    ForgeTextInputBox(sfxPromptDraft, { sfxPromptDraft = it.take(110) }, "Prompt SFX (e.g. punchy overdrive laser & deep sub-bass boom)...", Modifier.weight(1f))
    ForgeChip("🔊 FORGE SFX", Color(0xFF0284C7), Color(0xFF00F5D4), fontSize = 8.4.sp, radius = 7.dp, px = 10.dp, py = 8.dp) {
     sfxRecipeState = sfx.synthesizeSfxFromPrompt(sfxPromptDraft.ifBlank { "Punchy overdrive plasma & deep sub boom" })
     RiftCalibrationState.savePersistedLayout(context)
     sfx.previewForgedLaser()
    }
   }

   CompactForgeSliderRow("SFX Master Volume", sfxVolume, 0f..1f, "${(sfxVolume * 100f).roundToInt()}%", Color(0xFF38BDF8), onChange = onChangeSfxVolume)

   fun updateSfx(mutator: (SfxForgeRecipe) -> SfxForgeRecipe) {
    val next = mutator(sfxRecipeState)
    sfxRecipeState = next
    sfx.applySfxRecipe(next)
    RiftCalibrationState.savePersistedLayout(context)
   }
   CompactForgeSliderRow("Laser Pitch Multiplier", sfxRecipeState.laserPitchMultiplier, 0.60f..1.60f, String.format(Locale.US, "%.2fx", sfxRecipeState.laserPitchMultiplier), Color(0xFF00F5D4), onChange = { v -> updateSfx { it.copy(laserPitchMultiplier = v) } })
   CompactForgeSliderRow("Laser Harmonic Drive", sfxRecipeState.laserHarmonicDrive, 0.50f..2.00f, String.format(Locale.US, "%.2fx", sfxRecipeState.laserHarmonicDrive), Color(0xFF00F5D4), onChange = { v -> updateSfx { it.copy(laserHarmonicDrive = v) } })
   CompactForgeSliderRow("Explosion Sub-Bass", sfxRecipeState.explosionSubBass, 0.50f..2.00f, String.format(Locale.US, "%.2fx", sfxRecipeState.explosionSubBass), Color(0xFFF97316), onChange = { v -> updateSfx { it.copy(explosionSubBass = v) } })
   CompactForgeSliderRow("Shield Ripple Resonance", sfxRecipeState.shieldRippleResonance, 0.50f..2.00f, String.format(Locale.US, "%.2fx", sfxRecipeState.shieldRippleResonance), Color(0xFFA855F7), onChange = { v -> updateSfx { it.copy(shieldRippleResonance = v) } })
  }
 }
}

@Composable
private fun CustomBuffForgeBay(
 context: Context,
 engine: RiftGameEngine,
 sfx: RiftSoundSynthesizer,
 calibRev: Int,
 onShowToast: (String) -> Unit,
) {
 var buffPromptDraft by remember { mutableStateOf("Singularity gravity pulse pulling hostile hulls inward") }
 var buffNameDraft by remember { mutableStateOf("VOID LANCE") }
 var previewSalt by remember { mutableIntStateOf(1) }
 var registryTick by remember { mutableIntStateOf(0) }
 var draftEntry by remember {
  mutableStateOf(SpecialBuffRegistryEntry.synthesizeFromPrompt("Singularity gravity pulse pulling hostile hulls inward", RiftCodex.nextCustomBuffTypeIndex()))
 }
 val previewBitmap = remember(draftEntry) { RiftSpriteBaker.bakeCustomBuffIconBitmap(draftEntry, 96).asImageBitmap() }
 val customBuffList = remember(calibRev, registryTick) { RiftCodex.getCustomForgedBuffs() }

 Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
  Column(
   modifier = Modifier.weight(0.54f).fillMaxHeight()
    .background(Color(0xFF071120), RoundedCornerShape(10.dp))
    .border(1.dp, Color(0xFF1E3A5F), RoundedCornerShape(10.dp))
    .padding(10.dp)
    .verticalScroll(rememberScrollState()),
   verticalArrangement = Arrangement.spacedBy(7.dp),
  ) {
   Text("🧪 ON-DEVICE SPECIAL BUFF SYNTHESIZER", color = Color(0xFF34D399), fontSize = 10.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
   Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
    Box(
     modifier = Modifier.size(64.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFF040914)).border(1.dp, Color(0xFF34D399), RoundedCornerShape(10.dp)),
     contentAlignment = Alignment.Center,
    ) {
     Image(bitmap = previewBitmap, contentDescription = draftEntry.officialName, filterQuality = FilterQuality.High, modifier = Modifier.size(54.dp))
    }
    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
     ForgeTextInputBox(buffNameDraft, {
      buffNameDraft = it.take(18).uppercase(Locale.US)
      val clean = buffNameDraft.ifBlank { "CUSTOM BUFF" }
      draftEntry = draftEntry.copy(officialName = clean, shortLabel = clean.take(8))
     }, "Buff Title...")
     Text("Effect: ${draftEntry.specialEffect.name} • ${draftEntry.damage.roundToInt()} DMG • 0 Flux • +${draftEntry.chargesPerPickup} Charges", color = Color(0xFF94A3B8), fontSize = 8.4.sp, fontFamily = FontFamily.Monospace)
    }
   }
   ForgeTextInputBox(buffPromptDraft, { buffPromptDraft = it.take(140) }, "Describe tactical crate buff (e.g. crimson EMP shockwave, cyan stasis wall)...", Modifier.fillMaxWidth())
   Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
    listOf("Gravity Pull Vortex", "360° Repulsor Wave", "Arcing EMP Lance", "Hard-Light Barrier", "Ricochet Scatter Volley").forEach { preset ->
     ForgeChip(preset, Color(0xFF0C2233), Color(0x6634D399), Color(0xFFA7F3D0), fontSize = 8.sp, radius = 5.dp, px = 7.dp, py = 3.5.dp, mono = false, bold = false) {
      buffPromptDraft = preset
      previewSalt++
      val synth = SpecialBuffRegistryEntry.synthesizeFromPrompt(preset, RiftCodex.nextCustomBuffTypeIndex())
      buffNameDraft = synth.officialName
      draftEntry = synth
     }
    }
   }
   CompactForgeSliderRow("Projectile / Pulse Damage", draftEntry.damage, 40f..1400f, "${draftEntry.damage.roundToInt()} DMG", Color(0xFF34D399), onChange = { draftEntry = draftEntry.copy(damage = it) })
   CompactForgeSliderRow("Blast / Field Radius", draftEntry.blastRadius, 60f..520f, "${draftEntry.blastRadius.roundToInt()} px", Color(0xFF38BDF8), onChange = { draftEntry = draftEntry.copy(blastRadius = it) })
   CompactForgeSliderRow("Projectile Velocity", draftEntry.projectileSpeed, 160f..1300f, "${draftEntry.projectileSpeed.roundToInt()} px/s", Color(0xFFFACC15), onChange = { draftEntry = draftEntry.copy(projectileSpeed = it) })

   Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    Button(
     onClick = {
      previewSalt++
      val synth = SpecialBuffRegistryEntry.synthesizeFromPrompt(buffPromptDraft.ifBlank { "Plasma burst nova" }, RiftCodex.nextCustomBuffTypeIndex())
      buffNameDraft = synth.officialName
      draftEntry = synth
     },
     colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
     border = BorderStroke(1.dp, Color(0xFF38BDF8)),
     shape = RoundedCornerShape(8.dp),
     modifier = Modifier.weight(0.42f).height(34.dp),
    ) {
     Text("🎲 SYNTHESIZE", color = Color.White, fontSize = 8.8.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
    }
    Button(
     onClick = {
      val nextIdx = RiftCodex.nextCustomBuffTypeIndex()
      val cleanTitle = buffNameDraft.ifBlank { draftEntry.officialName }.take(18).uppercase(Locale.US)
      val committed = draftEntry.copy(id = "FORGED_BUFF_${System.currentTimeMillis()}", buffTypeIndex = nextIdx, officialName = cleanTitle, shortLabel = cleanTitle.take(8), isCustomForged = true)
      RiftCodex.registerCustomBuff(committed)
      RiftCalibrationState.savePersistedLayout(context)
      engine.grantSpecialBuffWeapon(engine.localPlayer, committed.id, committed.chargesPerPickup)
      sfx.playPrize()
      registryTick++
      onShowToast("🧪 Forged & Equipped [${committed.officialName}] in Special Buff Slot")
     },
     colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
     border = BorderStroke(1.dp, Color(0xFF6EE7B7)),
     shape = RoundedCornerShape(8.dp),
     modifier = Modifier.weight(0.58f).height(34.dp).testTag("forge_btn_commit_custom_buff"),
    ) {
     Text("➕ ADD TO CRATES & EQUIP", color = Color.White, fontSize = 8.8.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
    }
   }
  }

  Column(
   modifier = Modifier.weight(0.46f).fillMaxHeight()
    .background(Color(0xFF071120), RoundedCornerShape(10.dp))
    .border(1.dp, Color(0xFF1E3A5F), RoundedCornerShape(10.dp))
    .padding(10.dp)
    .verticalScroll(rememberScrollState()),
   verticalArrangement = Arrangement.spacedBy(7.dp),
  ) {
   Text("📦 FORGED CRATE BUFF ROSTER (${customBuffList.size})", color = Color(0xFF00F5D4), fontSize = 10.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
   if (customBuffList.isEmpty()) {
    Box(
     modifier = Modifier.fillMaxWidth().background(Color(0xFF040914), RoundedCornerShape(8.dp)).border(1.dp, Color(0xFF1E293B), RoundedCornerShape(8.dp)).padding(14.dp),
     contentAlignment = Alignment.Center,
    ) {
     Text("No custom buffs forged yet. Synthesize a tactical buff on the left to inject it into Supply Crates and equip it immediately.", color = Color(0xFF94A3B8), fontSize = 9.5.sp, lineHeight = 13.sp)
    }
   } else {
    for (entry in customBuffList) {
     val iconBmp = remember(entry) { RiftSpriteBaker.bakeCustomBuffIconBitmap(entry, 64).asImageBitmap() }
     Row(
      modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color(0xFF050C18)).border(1.dp, Color(entry.strokeColorInt), RoundedCornerShape(8.dp)).padding(8.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
     ) {
      Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
       Image(bitmap = iconBmp, contentDescription = entry.officialName, modifier = Modifier.size(34.dp))
       Column {
        Text(entry.officialName, color = Color.White, fontSize = 9.8.sp, fontWeight = FontWeight.Bold)
        Text("${entry.specialEffect.name} • ${entry.damage.roundToInt()} DMG • 0 Flux", color = Color(0xFF94A3B8), fontSize = 8.sp, fontFamily = FontFamily.Monospace)
       }
      }
      Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
       ForgeChip("EQUIP", Color(0xFF0E4F5C), Color(0xFF00F5D4), fontSize = 7.8.sp, radius = 5.dp, px = 7.dp) {
        engine.grantSpecialBuffWeapon(engine.localPlayer, entry.id, entry.chargesPerPickup)
        sfx.playPrize()
        onShowToast("Equipped [${entry.officialName}] for Test Flight")
       }
       ForgeChip("✕", Color(0xFF361620), Color(0xFFEF4444), Color(0xFFFCA5A5), fontSize = 8.sp, radius = 5.dp, px = 6.dp) {
        RiftCodex.removeCustomBuff(entry.id)
        RiftCalibrationState.savePersistedLayout(context)
        registryTick++
       }
      }
     }
    }
   }
  }
 }
}

@Composable
private fun SyndicateCrestForgeBay(
 context: Context,
 sfx: RiftSoundSynthesizer,
 syndicateTag: String,
 callsign: String,
 visualRev: Int,
 onShowToast: (String) -> Unit,
) {
 val crest = RiftVisualForgeState.activeSyndicateCrestRecipe
 var promptDraft by remember(visualRev) { mutableStateOf(crest.promptUsed) }
 var variationSalt by remember { mutableIntStateOf(0) }
 val cleanBadgeTag = remember(syndicateTag, callsign) {
  val c = RiftGameEngine.cleanTag(syndicateTag)
  if (c.isBlank() || c in setOf("GUEST", "FREE", "RFT", "BOT")) callsign.take(4).uppercase(Locale.US) else c
 }

 Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
  Column(
   modifier = Modifier.weight(0.42f).fillMaxHeight()
    .background(Color(0xFF071120), RoundedCornerShape(10.dp))
    .border(1.dp, Color(0xFF1E3A5F), RoundedCornerShape(10.dp))
    .padding(10.dp),
   horizontalAlignment = Alignment.CenterHorizontally,
   verticalArrangement = Arrangement.SpaceBetween,
  ) {
   Text("🛡️ SYNDICATE HERALDRY PREVIEW", color = Color(0xFFFACC15), fontSize = 10.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
   Box(
    modifier = Modifier.fillMaxWidth().weight(1f).padding(vertical = 8.dp)
     .clip(RoundedCornerShape(10.dp)).background(Color(0xFF040914)).border(1.dp, Color(0x55FACC15), RoundedCornerShape(10.dp)),
    contentAlignment = Alignment.Center,
   ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
     val cx = size.width * 0.5f
     val cy = size.height * 0.5f
     val r = (size.minDimension * 0.36f).coerceIn(36f, 110f)
     drawIntoCanvas { canvas -> RiftSpriteBaker.drawSyndicateCrestOnCanvas(canvas.nativeCanvas, cx, cy, r, crest, cleanBadgeTag) }
    }
   }
   Text("${crest.shieldShapeLabel.uppercase(Locale.US)} • ${crest.emblemGlyphLabel.uppercase(Locale.US)}", color = Color(0xFFE2E8F0), fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
  }

  Column(
   modifier = Modifier.weight(0.58f).fillMaxHeight()
    .background(Color(0xFF071120), RoundedCornerShape(10.dp))
    .border(1.dp, Color(0xFF1E3A5F), RoundedCornerShape(10.dp))
    .padding(10.dp)
    .verticalScroll(rememberScrollState()),
   verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
   Text("✨ ON-DEVICE CREST SYNTHESIZER", color = Color(0xFF00F5D4), fontSize = 10.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
   Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
    ForgeTextInputBox(promptDraft, { promptDraft = it.take(110) }, "Describe crest (e.g. sovereign gold falcon shield, crimson twin blades)...", Modifier.weight(1f))
    ForgeChip("✨ FORGE CREST", Color(0xFFB45309), Color(0xFFFACC15), fontSize = 8.6.sp, radius = 7.dp, px = 10.dp, py = 8.dp, tag = "forge_btn_synthesize_crest") {
     variationSalt++
     RiftVisualForgeState.reforgeSyndicateCrestFromPrompt(promptDraft.ifBlank { "Sovereign Gold & Cyan Aegis" }, variationSalt, context)
     sfx.playPrize()
     onShowToast("🛡️ Syndicate Crest Forged & Saved")
    }
   }
   Text("SHIELD CHASSIS:", color = Color(0xFF94A3B8), fontSize = 8.5.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
   Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
    listOf(0 to "Hexagon Aegis", 1 to "Chevron Kite", 2 to "Octagon Bastion", 3 to "Diamond Sovereign").forEach { (idx, name) ->
     val sel = crest.shieldShape == idx
     ForgeChip(name, if (sel) Color(0xFF3B2A0A) else Color(0xFF0A1426), if (sel) Color(0xFFFACC15) else Color(0xFF334155), if (sel) Color(0xFFFEF08A) else Color(0xFFCBD5E1), fontSize = 8.4.sp, mono = false) {
      RiftVisualForgeState.updateSyndicateCrest(crest.copy(shieldShape = idx), context)
     }
    }
   }
   Text("CENTRAL EMBLEM:", color = Color(0xFF94A3B8), fontSize = 8.5.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
   Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
    listOf(0 to "Rift Star", 1 to "Twin Blades", 2 to "Crown Spire", 3 to "Vortex Eye", 4 to "Winged Falcon", 5 to "Tactician Visor").forEach { (idx, name) ->
     val sel = crest.emblemGlyph == idx
     ForgeChip(name, if (sel) Color(0xFF0E4F5C) else Color(0xFF0A1426), if (sel) Color(0xFF00F5D4) else Color(0xFF334155), if (sel) Color.White else Color(0xFFCBD5E1), fontSize = 8.4.sp, mono = false) {
      RiftVisualForgeState.updateSyndicateCrest(crest.copy(emblemGlyph = idx), context)
     }
    }
   }
   ForgeSwatchPickerRow("TRIM RIM", crest.trimHex) { hex -> RiftVisualForgeState.updateSyndicateCrest(crest.copy(trimHex = hex), context) }
   ForgeSwatchPickerRow("AURA GLOW", crest.glowHex) { hex -> RiftVisualForgeState.updateSyndicateCrest(crest.copy(glowHex = hex), context) }
  }
 }
}

@Composable
private fun DevPlayfieldAndHudStudioBay(
 context: Context,
 engine: RiftGameEngine,
 sfx: RiftSoundSynthesizer,
 visualRev: Int,
 calibRev: Int,
 onEnterHudDragArenaMode: () -> Unit,
 onSyncCalibrationToTesters: () -> Unit,
 onShowToast: (String) -> Unit,
) {
 var isTestFlightMode by remember { mutableStateOf(false) }
 var layerFilter by remember { mutableStateOf<ForgeElementLayer?>(null) }
 var selectedTarget by remember { mutableStateOf(ForgeElementTarget.SAFE_ZONE_WALL) }
 var showCombatSandboxDrawer by remember { mutableStateOf(false) }
 var showImportJsonDialog by remember { mutableStateOf(false) }
 var importJsonDraft by remember { mutableStateOf("") }
 var customReforgePrompt by remember(selectedTarget) { mutableStateOf("") }
 var variationSalt by remember { mutableIntStateOf(0) }
 var sandboxShipFilter by remember { mutableIntStateOf(0) }

 val infiniteTransition = rememberInfiniteTransition(label = "dev_stage_anim")
 val spinPhase by infiniteTransition.animateFloat(
  initialValue = 0f,
  targetValue = (PI * 2.0).toFloat(),
  animationSpec = infiniteRepeatable(tween(4200, easing = LinearEasing), RepeatMode.Restart),
  label = "stage_rift_spin",
 )

 val selectedRecipe = remember(selectedTarget, visualRev) { RiftVisualForgeState.getRecipe(selectedTarget) }
 val boundDescriptors = remember(selectedTarget, calibRev, visualRev) { RiftVisualForgeState.getCalibrationDescriptorsForTarget(selectedTarget) }
 val canUndoSelected = remember(selectedTarget, visualRev) { RiftVisualForgeState.canUndoTarget(selectedTarget) }
 val filteredTargets = remember(layerFilter) { ForgeElementTarget.entries.filter { layerFilter == null || it.layer == layerFilter } }

 val stageHotspots = remember {
  listOf(
   StageElementHotspot(ForgeElementTarget.BACKGROUND_NEBULA, 0.10f, 0.16f),
   StageElementHotspot(ForgeElementTarget.ARENA_PERIMETER_RING, 0.50f, 0.09f),
   StageElementHotspot(ForgeElementTarget.SAFE_ZONE_PAD, 0.50f, 0.50f),
   StageElementHotspot(ForgeElementTarget.SAFE_ZONE_WALL, 0.50f, 0.36f),
   StageElementHotspot(ForgeElementTarget.PLACED_STASIS_BARRIER, 0.35f, 0.44f),
   StageElementHotspot(ForgeElementTarget.OBSTACLE_ORBITAL_STATION, 0.22f, 0.30f),
   StageElementHotspot(ForgeElementTarget.OBSTACLE_ASTEROID_STATION, 0.78f, 0.30f),
   StageElementHotspot(ForgeElementTarget.OBSTACLE_SPACE_ASTEROID, 0.25f, 0.64f),
   StageElementHotspot(ForgeElementTarget.OBSTACLE_FLOATING_DEBRIS, 0.74f, 0.64f),
   StageElementHotspot(ForgeElementTarget.RIFT_PLASMA, 0.17f, 0.48f),
   StageElementHotspot(ForgeElementTarget.RIFT_REPULSOR, 0.83f, 0.48f),
   StageElementHotspot(ForgeElementTarget.RIFT_GRAVITY, 0.34f, 0.22f),
   StageElementHotspot(ForgeElementTarget.RIFT_PHASE_PAIR, 0.66f, 0.22f),
   StageElementHotspot(ForgeElementTarget.SUPPLY_CRATE, 0.40f, 0.66f),
   StageElementHotspot(ForgeElementTarget.PROJECTILE_PRIMARY_BOLT, 0.60f, 0.54f),
   StageElementHotspot(ForgeElementTarget.PROJECTILE_PROXIMITY_MINE, 0.61f, 0.68f),
   StageElementHotspot(ForgeElementTarget.ECLIPSE_STASIS_BUBBLE, 0.36f, 0.80f),
   StageElementHotspot(ForgeElementTarget.FLUX_CORE_ORB, 0.50f, 0.76f),
   StageElementHotspot(ForgeElementTarget.STASIS_BRIG_JAIL, 0.65f, 0.80f),
   StageElementHotspot(ForgeElementTarget.SHIP_AURA_LABELS, 0.50f, 0.61f),
   StageElementHotspot(ForgeElementTarget.HUD_TOP_BAR, 0.50f, 0.035f),
   StageElementHotspot(ForgeElementTarget.HUD_NEXUS_BUTTON, 0.88f, 0.055f),
   StageElementHotspot(ForgeElementTarget.HUD_KILL_FEED, 0.90f, 0.20f),
   StageElementHotspot(ForgeElementTarget.HUD_WARNING_BANNER, 0.50f, 0.16f),
   StageElementHotspot(ForgeElementTarget.HUD_SPECTATOR_BOX, 0.50f, 0.26f),
   StageElementHotspot(ForgeElementTarget.HUD_MINIMAP_CONSOLE, 0.50f, 0.93f),
   StageElementHotspot(ForgeElementTarget.HUD_BUFF_STRIP, 0.50f, 0.86f),
   StageElementHotspot(ForgeElementTarget.HUD_LEFT_STICK, 0.12f, 0.83f),
   StageElementHotspot(ForgeElementTarget.HUD_FIRE_STICK, 0.88f, 0.83f),
   StageElementHotspot(ForgeElementTarget.HUD_SECONDARY_BTN, 0.77f, 0.76f),
   StageElementHotspot(ForgeElementTarget.HUD_SPECIAL_BUFF_BTN, 0.82f, 0.67f),
   StageElementHotspot(ForgeElementTarget.HUD_REPEL_BTN, 0.92f, 0.66f),
   StageElementHotspot(ForgeElementTarget.HUD_BRAKE_BTN, 0.23f, 0.88f),
   StageElementHotspot(ForgeElementTarget.HUD_MULTI_BTN, 0.75f, 0.90f),
   StageElementHotspot(ForgeElementTarget.CURRENCY_FLUX_ICON, 0.36f, 0.94f),
   StageElementHotspot(ForgeElementTarget.CURRENCY_SHARD_ICON, 0.64f, 0.94f),
  )
 }

 Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
  Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
   ForgeChip("🛠️ INSPECT & REFORGE", if (!isTestFlightMode) Color(0xFF4C1D95) else Color(0xFF0E1B2E), Color(0xFFC084FC), fontSize = 8.4.sp, radius = 7.dp, px = 9.dp, py = 5.dp, tag = "dev_forge_mode_inspect") {
    isTestFlightMode = false
   }
   ForgeChip("🚀 TEST FLIGHT", if (isTestFlightMode) Color(0xFF065F46) else Color(0xFF0E1B2E), Color(0xFF34D399), Color(0xFFA7F3D0), fontSize = 8.4.sp, radius = 7.dp, px = 9.dp, py = 5.dp, tag = "dev_forge_mode_test_flight") {
    if (!engine.hasJoinedMatch) engine.deployToRankedMode(engine.activeRankedMode, engine.activeTeamSizeBracket)
    engine.isInLobbyStaging = false
    isTestFlightMode = true
   }
   listOf(null to "ALL (36)", ForgeElementLayer.WORLD to "WORLD (20)", ForgeElementLayer.HUD to "HUD (16)").forEach { (flt, lbl) ->
    val sel = layerFilter == flt
    ForgeChip(lbl, if (sel) Color(0xFF0E4F5C) else Color(0xFF091324), if (sel) Color(0xFF00F5D4) else Color(0xFF334155), if (sel) Color.White else Color(0xFF94A3B8), fontSize = 8.sp, px = 7.dp) {
     layerFilter = flt
    }
   }
   ForgeChip(
    text = if (RiftVisualForgeState.pointLightEnabled) "💡 RIM LIGHT: ON" else "💡 RIM LIGHT: OFF",
    bg = if (RiftVisualForgeState.pointLightEnabled) Color(0xFF3B2A0A) else Color(0xFF091324),
    border = Color(0xFFFACC15),
    textColor = Color(0xFFFDE68A),
    fontSize = 8.sp,
    px = 7.dp,
   ) {
    RiftVisualForgeState.pointLightEnabled = !RiftVisualForgeState.pointLightEnabled
    RiftVisualForgeState.visualRevision++
    RiftCalibrationState.savePersistedLayout(context)
   }
   ForgeChip(
    text = if (RiftVisualForgeState.pointLightTestOrbWorldPos != null) "🔮 ORB: ACTIVE" else "🔮 TEST ORB",
    bg = if (RiftVisualForgeState.pointLightTestOrbWorldPos != null) Color(0xFF3B1366) else Color(0xFF091324),
    border = Color(0xFFE879F9),
    textColor = Color(0xFFF5D0FE),
    fontSize = 8.sp,
    px = 7.dp,
   ) {
    RiftVisualForgeState.pointLightTestOrbWorldPos = if (RiftVisualForgeState.pointLightTestOrbWorldPos == null) Offset(engine.localPlayer.x + 220f, engine.localPlayer.y - 160f) else null
    RiftVisualForgeState.visualRevision++
   }
   ForgeChip("⚔️ COMBAT STATS", if (showCombatSandboxDrawer) Color(0xFF0E4F5C) else Color(0xFF091324), Color(0xFF38BDF8), Color(0xFFBAE6FD), fontSize = 8.sp, px = 7.dp) {
    showCombatSandboxDrawer = !showCombatSandboxDrawer
   }
   ForgeChip("🔓 DRAG HUD ON ARENA", Color(0xFF143D34), Color(0xFF00F5D4), Color(0xFF6EE7B7), fontSize = 8.sp, px = 7.dp, tag = "dev_forge_btn_live_hud_drag") {
    onEnterHudDragArenaMode()
   }
   ForgeChip("💾 SAVE DEFAULTS", Color(0xFF065F46), Color(0xFF34D399), fontSize = 8.sp, tag = "dev_forge_btn_save_defaults") {
    RiftCalibrationState.savePersistedLayout(context)
    sfx.playPrize()
    onShowToast("💾 Saved All Visual, Audio, Crest & Physics Defaults")
   }
   ForgeChip("📋 COPY BUNDLE", Color(0xFF1E293B), Color(0xFF94A3B8), Color(0xFFE2E8F0), fontSize = 8.sp, px = 7.dp) {
    val (promptOut, modCount) = RiftCalibrationState.generateAiStudioDefaultsPrompt()
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    cm?.setPrimaryClip(ClipData.newPlainText("RIFT_FORGE_BUNDLE", promptOut))
    onShowToast("📋 Copied NEXUS FORGE Bundle ($modCount Modified Settings) to Clipboard")
   }
   ForgeChip("📥 IMPORT", Color(0xFF1E293B), Color(0xFF60A5FA), Color(0xFFBFDBFE), fontSize = 8.sp, px = 7.dp) {
    showImportJsonDialog = !showImportJsonDialog
   }
   ForgeChip("📡 SYNC TESTERS", Color(0xFF172554), Color(0xFF38BDF8), Color(0xFF7DD3FC), fontSize = 8.sp, px = 7.dp) {
    onSyncCalibrationToTesters()
   }
  }

  if (showImportJsonDialog) {
   Row(
    modifier = Modifier.fillMaxWidth().background(Color(0xFF0A1426), RoundedCornerShape(8.dp)).border(1.dp, Color(0xFF60A5FA), RoundedCornerShape(8.dp)).padding(6.dp),
    horizontalArrangement = Arrangement.spacedBy(6.dp),
    verticalAlignment = Alignment.CenterVertically,
   ) {
    ForgeTextInputBox(importJsonDraft, { importJsonDraft = it }, "Paste exported NEXUS FORGE JSON bundle...", Modifier.weight(1f))
    ForgeChip("APPLY JSON", Color(0xFF0284C7), Color(0xFF38BDF8), px = 10.dp, py = 7.dp) {
     if (RiftCalibrationState.importDevForgeBundleJson(importJsonDraft, engine, context)) {
      showImportJsonDialog = false
      importJsonDraft = ""
      onShowToast("✓ Imported NEXUS FORGE Bundle Live")
     } else onShowToast("⚠️ Invalid JSON Bundle")
    }
   }
  }

  Row(modifier = Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
   Box(
    modifier = Modifier.weight(0.56f).fillMaxHeight().clip(RoundedCornerShape(10.dp)).background(Color(0xFF030814)).border(1.2.dp, Color(0xFF38BDF8), RoundedCornerShape(10.dp)),
   ) {
    if (isTestFlightMode) {
     DisposableEffect(Unit) {
      val prevStaging = engine.isInLobbyStaging
      engine.isInLobbyStaging = false
      onDispose { engine.isInLobbyStaging = prevStaging }
     }
     AndroidView(
      modifier = Modifier.fillMaxSize().testTag("dev_forge_test_flight_arena"),
      factory = { ctx ->
       RiftArenaView(context = ctx, engine = engine).apply {
        isModalOverlayOpen = false
        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        requestFocus()
       }
      },
      update = { view -> view.isModalOverlayOpen = false },
     )
     Row(
      modifier = Modifier.align(Alignment.TopStart).padding(6.dp).clip(RoundedCornerShape(7.dp))
       .background(Color(0xCC040914)).border(1.dp, Color(0xFF00F5D4), RoundedCornerShape(7.dp))
       .padding(horizontal = 6.dp, vertical = 3.dp).horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(4.dp),
      verticalAlignment = Alignment.CenterVertically,
     ) {
      Text("FLY:", color = Color(0xFF00F5D4), fontSize = 7.8.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
      for (shipId in 1..10) {
       val active = engine.localPlayer.shipId == shipId
       ForgeChip("$shipId", if (active) Color(0xFF0284C7) else Color(0xFF0F172A), Color.Transparent, fontSize = 7.8.sp, radius = 4.dp, px = 5.dp, py = 2.dp, borderWidth = 0.dp) {
        engine.switchLocalPlayerShip(shipId, forceFromHangar = true)
       }
      }
     }
    } else {
     val visibleHotspots = remember(layerFilter) { stageHotspots.filter { layerFilter == null || it.target.layer == layerFilter } }
     Canvas(
      modifier = Modifier.fillMaxSize()
       .pointerInput(visibleHotspots) {
        detectTapGestures { tapOffset ->
         val w = size.width.toFloat().coerceAtLeast(1f)
         val h = size.height.toFloat().coerceAtLeast(1f)
         val nx = tapOffset.x / w
         val ny = tapOffset.y / h
         val hit = visibleHotspots.minByOrNull { hs -> hypot(nx - hs.normX, ny - hs.normY) }
         if (hit != null && hypot(nx - hit.normX, ny - hit.normY) <= 0.12f) selectedTarget = hit.target
        }
       }
       .pointerInput(RiftVisualForgeState.pointLightTestOrbWorldPos != null) {
        if (RiftVisualForgeState.pointLightTestOrbWorldPos != null) {
         detectDragGestures { change, drag ->
          change.consume()
          val cur = RiftVisualForgeState.pointLightTestOrbWorldPos ?: Offset(4000f, 4000f)
          RiftVisualForgeState.pointLightTestOrbWorldPos = Offset((cur.x + drag.x * 6f).coerceIn(800f, 7200f), (cur.y + drag.y * 6f).coerceIn(800f, 7200f))
          RiftVisualForgeState.visualRevision++
         }
        }
       }
       .testTag("dev_forge_interactive_stage_canvas"),
     ) {
      val w = size.width
      val h = size.height
      drawRect(color = Color(RiftVisualForgeState.getRecipe(ForgeElementTarget.BACKGROUND_NEBULA).primaryColorInt))
      val step = 36.dp.toPx()
      var gx = step
      while (gx < w) { drawLine(Color(0x1638BDF8), Offset(gx, 0f), Offset(gx, h), 1f); gx += step }
      var gy = step
      while (gy < h) { drawLine(Color(0x1638BDF8), Offset(0f, gy), Offset(w, gy), 1f); gy += step }

      val perimRec = RiftVisualForgeState.getRecipe(ForgeElementTarget.ARENA_PERIMETER_RING)
      drawCircle(Color(perimRec.glowColorInt).copy(alpha = 0.45f), size.minDimension * 0.44f, Offset(w * 0.5f, h * 0.5f), style = Stroke(2.2.dp.toPx() * perimRec.borderWeight))

      val padRec = RiftVisualForgeState.getRecipe(ForgeElementTarget.SAFE_ZONE_PAD)
      val wallRec = RiftVisualForgeState.getRecipe(ForgeElementTarget.SAFE_ZONE_WALL)
      val szSpan = size.minDimension * 0.24f * padRec.pixelScale.coerceIn(0.6f, 1.3f)
      val szTop = Offset(w * 0.5f - szSpan * 0.5f, h * 0.5f - szSpan * 0.5f)
      drawRoundRect(Color(padRec.primaryColorInt).copy(alpha = padRec.surfaceOpacity), szTop, Size(szSpan, szSpan), CornerRadius(8f, 8f))
      drawRoundRect(Color(wallRec.glowColorInt), szTop, Size(szSpan, szSpan), CornerRadius(8f, 8f), style = Stroke(2.4.dp.toPx() * wallRec.borderWeight))

      drawIntoCanvas { canvas ->
       val nc = canvas.nativeCanvas
       RiftSpriteBaker.draw3dPlayfieldRift(nc, AnomalyType.PLASMA_RIFT, w * 0.17f, h * 0.48f, 16.dp.toPx(), spinPhase)
       RiftSpriteBaker.draw3dPlayfieldRift(nc, AnomalyType.REPULSOR_RIFT, w * 0.83f, h * 0.48f, 16.dp.toPx(), spinPhase)
       RiftSpriteBaker.draw3dPlayfieldRift(nc, AnomalyType.GRAVITY_RIFT, w * 0.34f, h * 0.22f, 15.dp.toPx(), spinPhase)
       RiftSpriteBaker.draw3dPlayfieldRift(nc, AnomalyType.PHASE_RIFT_A, w * 0.66f, h * 0.22f, 15.dp.toPx(), spinPhase)

       fun drawObsMini(type: SpaceObstacleType, nx: Float, ny: Float) {
        val bmp = RiftSpriteBaker.getSpaceObstacleBitmap(type, 0)
        val rad = 16.dp.toPx()
        nc.drawBitmap(bmp, null, android.graphics.RectF(w * nx - rad, h * ny - rad, w * nx + rad, h * ny + rad), null)
       }
       drawObsMini(SpaceObstacleType.ORBITAL_STATION, 0.22f, 0.30f)
       drawObsMini(SpaceObstacleType.ASTEROID_STATION, 0.78f, 0.30f)
       drawObsMini(SpaceObstacleType.CRYSTAL_ASTEROID, 0.25f, 0.64f)
       drawObsMini(SpaceObstacleType.FLOATING_DEBRIS, 0.74f, 0.64f)
       RiftSpriteBaker.drawFluxPrismIcon(nc, w * 0.36f, h * 0.94f, 9.dp.toPx(), alpha = 255)
      }

      visibleHotspots.forEach { hs ->
       val hx = w * hs.normX
       val hy = h * hs.normY
       val isSel = hs.target == selectedTarget
       val rec = RiftVisualForgeState.getRecipe(hs.target)
       val nodeColor = if (hs.target.layer == ForgeElementLayer.WORLD) Color(rec.glowColorInt) else Color(rec.trimColorInt)
       if (isSel) {
        drawCircle(Color(0x55FFFFFF), 16.dp.toPx(), Offset(hx, hy))
        drawCircle(Color(0xFF00F5D4), 16.dp.toPx(), Offset(hx, hy), style = Stroke(2.2.dp.toPx()))
       } else {
        drawCircle(nodeColor.copy(alpha = 0.22f), 9.dp.toPx(), Offset(hx, hy))
        drawCircle(nodeColor.copy(alpha = 0.85f), 9.dp.toPx(), Offset(hx, hy), style = Stroke(1.2.dp.toPx()))
       }
      }
     }

     Row(
      modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Color(0xD9040914)).padding(horizontal = 6.dp, vertical = 4.dp).horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(4.dp),
     ) {
      filteredTargets.forEach { target ->
       val sel = target == selectedTarget
       ForgeChip(target.title, if (sel) Color(0xFF4C1D95) else Color(0xFF0D182C), if (sel) Color(0xFF00F5D4) else Color(0xFF334155), if (sel) Color.White else Color(0xFFCBD5E1), fontSize = 7.6.sp, radius = 5.dp, px = 6.dp, py = 3.dp) {
        selectedTarget = target
       }
      }
     }
    }
   }

   Column(
    modifier = Modifier.weight(0.44f).fillMaxHeight()
     .background(Color(0xFF071120), RoundedCornerShape(10.dp))
     .border(1.dp, Color(0xFF1E3A5F), RoundedCornerShape(10.dp))
     .padding(9.dp)
     .verticalScroll(rememberScrollState()),
    verticalArrangement = Arrangement.spacedBy(7.dp),
   ) {
    if (showCombatSandboxDrawer) {
     Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
      Text("⚔️ LIVE COMBAT & PHYSICS SANDBOX", color = Color(0xFF38BDF8), fontSize = 9.8.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
      ForgeChip("BACK TO OBJECT", Color(0xFF1E293B), Color(0xFF334155), Color(0xFF00F5D4), fontSize = 7.8.sp, radius = 5.dp, px = 7.dp, py = 3.dp) {
       showCombatSandboxDrawer = false
      }
     }
     Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
      (listOf(0 to "GLOBAL") + (1..10).map { it to "SHIP $it" }).forEach { (id, lbl) ->
       val sel = sandboxShipFilter == id
       ForgeChip(lbl, if (sel) Color(0xFF0E4F5C) else Color(0xFF0A1426), Color(0xFF38BDF8), fontSize = 7.8.sp, radius = 5.dp, px = 6.dp, py = 3.dp) {
        sandboxShipFilter = id
       }
      }
     }
     val combatDescs = remember(sandboxShipFilter, calibRev, visualRev) { RiftVisualForgeState.getCombatSandboxDescriptors(sandboxShipFilter) }
     combatDescs.forEach { desc -> BoundCalibrationDescriptorSlider(desc, engine, context) }
    } else {
     Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
      Column(modifier = Modifier.weight(1f).padding(end = 6.dp)) {
       Text("${selectedTarget.layer.name} // ${selectedTarget.title.uppercase(Locale.US)}", color = Color(0xFF00F5D4), fontSize = 9.6.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, maxLines = 1, overflow = TextOverflow.Ellipsis)
       Text(selectedTarget.subtitle, color = Color(0xFF94A3B8), fontSize = 8.2.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
      }
      Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
       ForgeChip("↶ UNDO", if (canUndoSelected) Color(0xFF1E3A5F) else Color(0xFF0F172A), if (canUndoSelected) Color(0xFF38BDF8) else Color(0xFF334155), if (canUndoSelected) Color.White else Color(0xFF64748B), fontSize = 7.8.sp, px = 7.dp, enabled = canUndoSelected, tag = "dev_forge_btn_undo_target") {
        RiftVisualForgeState.undoTarget(selectedTarget, engine, context)
       }
       ForgeChip("↺ RESET", Color(0xFF361620), Color(0xFFEF4444), Color(0xFFFCA5A5), fontSize = 7.8.sp, px = 7.dp, tag = "dev_forge_btn_reset_target") {
        RiftVisualForgeState.resetTargetToDefault(selectedTarget, engine, context)
        onShowToast("↺ Reset ${selectedTarget.title} to Factory Default")
       }
      }
     }

     Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
      selectedTarget.presetPrompts.forEach { (chipTitle, chipPrompt) ->
       ForgeChip("✨ $chipTitle", Color(0xFF171033), Color(0x99C084FC), Color(0xFFE9D5FF), fontSize = 8.sp, px = 7.dp, mono = false) {
        variationSalt++
        RiftVisualForgeState.reforgeTargetFromPrompt(selectedTarget, chipPrompt, variationSalt, recordUndo = true, context = context)
        sfx.playMutedClick()
       }
      }
     }

     Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
      ForgeTextInputBox(customReforgePrompt, { customReforgePrompt = it.take(120) }, "Describe look for ${selectedTarget.title}...", Modifier.weight(1f))
      Box(
       modifier = Modifier.clip(RoundedCornerShape(7.dp))
        .background(Brush.horizontalGradient(listOf(Color(0xFF6D28D9), Color(0xFF0284C7))))
        .border(1.dp, Color(0xFF00F5D4), RoundedCornerShape(7.dp))
        .clickable {
         variationSalt++
         val prompt = customReforgePrompt.ifBlank { selectedTarget.presetPrompts.firstOrNull()?.second ?: "Obsidian & Cyan" }
         RiftVisualForgeState.reforgeTargetFromPrompt(selectedTarget, prompt, variationSalt, recordUndo = true, context = context)
         sfx.playPrize()
        }
        .padding(horizontal = 9.dp, vertical = 8.dp)
        .testTag("dev_forge_btn_reforge_prompt"),
      ) {
       Text("REFORGE", color = Color.White, fontSize = 8.4.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
      }
     }

     Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
      Text("GEOMETRY:", color = Color(0xFF94A3B8), fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
      for (st in 0..3) {
       val sel = selectedRecipe.geometryStyle == st
       ForgeChip("STYLE ${st + 1}", if (sel) Color(0xFF0E4F5C) else Color(0xFF0B1426), if (sel) Color(0xFF00F5D4) else Color(0xFF334155), fontSize = 7.8.sp, radius = 5.dp, py = 3.dp) {
        RiftVisualForgeState.updateRecipe(selectedTarget, selectedRecipe.copy(geometryStyle = st), recordUndo = true, context = context)
       }
      }
     }

     ForgeSwatchPickerRow("BASE", selectedRecipe.primaryHex) { hex -> RiftVisualForgeState.updateRecipe(selectedTarget, selectedRecipe.copy(primaryHex = hex), recordUndo = true, context = context) }
     ForgeSwatchPickerRow("TRIM", selectedRecipe.trimHex) { hex -> RiftVisualForgeState.updateRecipe(selectedTarget, selectedRecipe.copy(trimHex = hex), recordUndo = true, context = context) }
     ForgeSwatchPickerRow("GLOW", selectedRecipe.glowHex) { hex -> RiftVisualForgeState.updateRecipe(selectedTarget, selectedRecipe.copy(glowHex = hex), recordUndo = true, context = context) }

     val saveLayout = { RiftCalibrationState.savePersistedLayout(context) }
     CompactForgeSliderRow("Element Scale", selectedRecipe.pixelScale, 0.50f..2.00f, String.format(Locale.US, "%.2fx", selectedRecipe.pixelScale), Color(0xFF00F5D4), { v -> RiftVisualForgeState.updateRecipe(selectedTarget, selectedRecipe.copy(pixelScale = v), recordUndo = false) }, saveLayout)
     CompactForgeSliderRow("Border Weight", selectedRecipe.borderWeight, 0.40f..2.50f, String.format(Locale.US, "%.2fx", selectedRecipe.borderWeight), Color(0xFF38BDF8), { v -> RiftVisualForgeState.updateRecipe(selectedTarget, selectedRecipe.copy(borderWeight = v), recordUndo = false) }, saveLayout)
     CompactForgeSliderRow("Glow Intensity", selectedRecipe.glowIntensity, 0.00f..2.50f, String.format(Locale.US, "%.2fx", selectedRecipe.glowIntensity), Color(0xFFC084FC), { v -> RiftVisualForgeState.updateRecipe(selectedTarget, selectedRecipe.copy(glowIntensity = v), recordUndo = false) }, saveLayout)
     CompactForgeSliderRow("Surface Opacity", selectedRecipe.surfaceOpacity, 0.15f..1.00f, "${(selectedRecipe.surfaceOpacity * 100f).roundToInt()}%", Color(0xFF34D399), { v -> RiftVisualForgeState.updateRecipe(selectedTarget, selectedRecipe.copy(surfaceOpacity = v), recordUndo = false) }, saveLayout)

     val hudOff = RiftVisualForgeState.getHudOffsetForTarget(selectedTarget)
     if (hudOff != null) {
      Row(
       modifier = Modifier.fillMaxWidth().background(Color(0xFF050C18), RoundedCornerShape(6.dp)).border(1.dp, Color(0xFF1E293B), RoundedCornerShape(6.dp)).padding(6.dp),
       horizontalArrangement = Arrangement.SpaceBetween,
       verticalAlignment = Alignment.CenterVertically,
      ) {
       Text("HUD OFFSET: (${hudOff.x.roundToInt()}, ${hudOff.y.roundToInt()}) px", color = Color(0xFF7DD3FC), fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
       Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        listOf("◀" to Offset(-10f, 0f), "▶" to Offset(10f, 0f), "▲" to Offset(0f, -10f), "▼" to Offset(0f, 10f)).forEach { (sym, delta) ->
         ForgeChip(sym, Color(0xFF132642), Color.Transparent, fontSize = 8.sp, radius = 4.dp, px = 6.dp, py = 2.dp, borderWidth = 0.dp) {
          RiftVisualForgeState.setHudOffsetForTarget(selectedTarget, Offset(hudOff.x + delta.x, hudOff.y + delta.y), recordUndo = true)
          RiftCalibrationState.savePersistedLayout(context)
         }
        }
       }
      }
     }

     if (boundDescriptors.isNotEmpty()) {
      Text("⚙️ BOUND ENGINE ATTRIBUTES (${boundDescriptors.size})", color = Color(0xFFFACC15), fontSize = 8.8.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
      boundDescriptors.forEach { desc -> BoundCalibrationDescriptorSlider(desc, engine, context) }
     }
    }
   }
  }
 }
}

@Composable
private fun BoundCalibrationDescriptorSlider(desc: CalibrationParamDescriptor, engine: RiftGameEngine, context: Context) {
 var localVal by remember(desc.key, RiftCalibrationState.calibrationRevision) { mutableFloatStateOf(desc.getter()) }
 val fmtVal = if (desc.decimals <= 0) "${localVal.roundToInt()} ${desc.unit}".trim() else String.format(Locale.US, "%.${desc.decimals}f %s", localVal, desc.unit).trim()
 CompactForgeSliderRow(
  title = desc.title,
  value = localVal.coerceIn(desc.min, desc.max),
  range = desc.min..desc.max,
  valueText = fmtVal,
  accent = Color(0xFFFACC15),
  onChange = { raw -> localVal = raw; desc.setter(raw); RiftCalibrationState.applyCalibrationChangesLive(engine, null) },
  onFinished = { RiftCalibrationState.applyCalibrationChangesLive(engine, context) },
 )
}

@Composable
private fun ForgeTextInputBox(value: String, onValueChange: (String) -> Unit, placeholder: String, modifier: Modifier = Modifier) {
 Box(
  modifier = modifier.height(34.dp).clip(RoundedCornerShape(7.dp)).background(Color(0xFF040914)).border(1.dp, Color(0xFF284B73), RoundedCornerShape(7.dp)).padding(horizontal = 9.dp),
  contentAlignment = Alignment.CenterStart,
 ) {
  if (value.isEmpty()) Text(placeholder, color = Color(0xFF64748B), fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
  BasicTextField(
   value = value,
   onValueChange = onValueChange,
   singleLine = true,
   cursorBrush = SolidColor(Color(0xFF00F5D4)),
   textStyle = TextStyle(color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Medium, fontFamily = FontFamily.SansSerif),
   modifier = Modifier.fillMaxWidth(),
  )
 }
}

@Composable
private fun ForgeSwatchPickerRow(label: String, selectedHex: String, onSelectHex: (String) -> Unit) {
 Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
  Text("$label:", color = Color(0xFF94A3B8), fontSize = 7.8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, modifier = Modifier.width(42.dp))
  FORGE_COLOR_SWATCHES.forEach { hex ->
   val isSel = selectedHex.equals(hex, ignoreCase = true)
   val colInt = VisualElementRecipe.parseHexColorSafe(hex, 0xFF38BDF8.toInt())
   Box(
    modifier = Modifier.size(18.dp).clip(CircleShape).background(Color(colInt))
     .border(if (isSel) 2.dp else 1.dp, if (isSel) Color.White else Color(0x88475569), CircleShape)
     .clickable { onSelectHex(hex) },
   )
  }
 }
}

@Composable
private fun CompactForgeSliderRow(
 title: String,
 value: Float,
 range: ClosedFloatingPointRange<Float>,
 valueText: String,
 accent: Color,
 onChange: (Float) -> Unit,
 onFinished: () -> Unit = {},
) {
 Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(1.dp)) {
  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
   Text(title, color = Color(0xFFCBD5E1), fontSize = 8.4.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
   Text(valueText, color = accent, fontSize = 8.4.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
  }
  Slider(
   value = value.coerceIn(range.start, range.endInclusive),
   onValueChange = onChange,
   onValueChangeFinished = onFinished,
   valueRange = range,
   colors = SliderDefaults.colors(thumbColor = accent, activeTrackColor = accent, inactiveTrackColor = Color(0xFF132642)),
   modifier = Modifier.fillMaxWidth().height(20.dp),
  )
 }
}
