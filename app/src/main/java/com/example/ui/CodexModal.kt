package com.example.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.RiftCodex
import com.example.engine.RiftSpriteBaker
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun CodexModal(
 isNexusStreaming: Boolean = false,
 onOpenNexus: () -> Unit = {},
 onCloseModal: () -> Unit = {},
) {
 val context = LocalContext.current
 val isCompactPhone = LocalConfiguration.current.screenHeightDp < 480
 var selectedTab by remember { mutableIntStateOf(0) }

 Column(
  modifier = Modifier.fillMaxSize().padding(8.dp).testTag("codex_modal"),
  verticalArrangement = Arrangement.spacedBy(if (isCompactPhone) 6.dp else 8.dp),
 ) {
  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
   Column(modifier = Modifier.weight(1f, fill = false).padding(end = 8.dp)) {
    Text(
     if (isCompactPhone) "📖 RIFT CODEX" else "📖 RIFT CODEX: TACTICAL FIELD MANUAL",
     color = Color.White, fontSize = if (isCompactPhone) 13.sp else 15.5.sp, fontWeight = FontWeight.ExtraBold,
     letterSpacing = 0.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
    )
    if (!isCompactPhone) {
     Text(
      "SUPPLY BUFF POOL • PLAYFIELD RIFT ANOMALIES • TWIN-STICK CONTROLLER & 10-SHIP PARITY",
      color = Color(0xFF00F5D4), fontSize = 9.5.sp, fontWeight = FontWeight.Medium, fontFamily = FontFamily.Monospace,
      maxLines = 1, overflow = TextOverflow.Ellipsis,
     )
    }
   }

   Row(
    modifier = Modifier.background(Color(0xFF050B14), RoundedCornerShape(8.dp))
     .border(1.dp, Color(0xFF1E3A5F), RoundedCornerShape(8.dp)).padding(2.dp)
     .horizontalScroll(rememberScrollState()),
    horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically,
   ) {
    CodexTabPill("SUPPLY BUFFS", true, selectedTab == 0, Color(0xFF38BDF8), isCompactPhone, "codex_tab_supply_buffs") { selectedTab = 0 }
    CodexTabPill("🌀 PLAYFIELD RIFTS", false, selectedTab == 1, Color(0xFFA855F7), isCompactPhone, "codex_tab_playfield_rifts") { selectedTab = 1 }
    CodexTabPill("🎮 CONTROLLER", false, selectedTab == 2, Color(0xFF34D399), isCompactPhone, "codex_tab_controller") { selectedTab = 2 }
   }
  }

  when (selectedTab) {
   0 -> {
    LazyColumn(
     modifier = Modifier.weight(1f).fillMaxWidth().background(Color(0xFF081426), RoundedCornerShape(10.dp))
      .border(1.dp, Color(0xFF1B3454), RoundedCornerShape(10.dp)).padding(if (isCompactPhone) 6.dp else 8.dp),
     verticalArrangement = Arrangement.spacedBy(if (isCompactPhone) 6.dp else 8.dp),
    ) {
     item(key = "supply_buffs_highlights") {
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(if (isCompactPhone) 5.dp else 8.dp)) {
       CodexHighlightCard(
        Modifier.weight(1f), "🎯 SPECIAL BUFF SLOT (0 FLUX)",
        "Nova Ring, Shatter Cone, Seeker Torpedo, Holo Phantom, Stasis Barrier & Hyper Surge equip to the 4th button at 0 Flux cost.",
        Color(0xFF092318), Color(0xFF10B981), Color(0xFFD1FAE5), isCompactPhone,
       )
       CodexHighlightCard(
        Modifier.weight(1f), "❖ HULL ENHANCERS",
        "Bouncing Bullets (+1 bounce), Speed / Thrust / Reactor Overcharge (+8% up to 3/3), Multi-Shot (+25), Repel (+1), & Full Flux.",
        Color(0xFF261908), Color(0xFFF59E0B), Color(0xFFFEF3C7), isCompactPhone,
       )
       CodexHighlightCard(
        Modifier.weight(1f), "⚖️ 100-PT HULL PARITY",
        "All 9 standard ships (Viper through Eclipse) share the Universal Floor + 100 Pts. Rift Frame unlocks the full 100-Pt budget at Level 20.",
        Color(0xFF0D1E36), Color(0xFF38BDF8), Color(0xFFE0F2FE), isCompactPhone,
       )
      }
     }

     itemsIndexed(RiftCodex.POWERUP_CODEX) { _, entry ->
      val buffType = entry.buffTypeIndex
      val accentColor = when (buffType) {
       0 -> Color(0xFFF59E0B); 1 -> Color(0xFFFB7185); 2 -> Color(0xFFEF4444); 3 -> Color(0xFF38BDF8)
       4 -> Color(0xFF22D3EE); 5 -> Color(0xFFA855F7); 6 -> Color(0xFF10B981); 7 -> Color(0xFF60A5FA)
       8 -> Color(0xFFFACC15); 9 -> Color(0xFFC084FC); -10 -> Color(0xFF38BDF8); -11 -> Color(0xFFF59E0B)
       -12 -> Color(0xFF10B981); else -> Color(0xFF38BDF8)
      }

      Row(
       modifier = Modifier.fillMaxWidth().background(Color(0xFF0E1F38), RoundedCornerShape(8.dp))
        .border(1.dp, accentColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp)).padding(if (isCompactPhone) 8.dp else 10.dp),
       verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(if (isCompactPhone) 10.dp else 12.dp),
      ) {
       BuffIconPreview(buffType = buffType, sizeDp = if (isCompactPhone) 46 else 54, accentColor = accentColor)
       Column(modifier = Modifier.weight(1f)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
         Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          Text(entry.title.replace("\u26A1", "Flux"), color = accentColor, fontSize = if (isCompactPhone) 12.sp else 13.5.sp, fontWeight = FontWeight.Bold)
          Box(modifier = Modifier.background(Color(0x22FFFFFF), RoundedCornerShape(4.dp)).padding(horizontal = 5.dp, vertical = 2.dp)) {
           Text(entry.category.replace("\u26A1", "FLUX"), color = Color(0xFFCBD5E1), fontSize = 8.5.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
          }
         }
         Box(modifier = Modifier.background(Color(0x26FFD700), RoundedCornerShape(6.dp)).border(1.dp, Color(0xFFFFD700), RoundedCornerShape(6.dp)).padding(horizontal = 7.dp, vertical = 2.dp)) {
          Text("DROP ${entry.dropRatePercent}%", color = Color(0xFFFFD700), fontSize = if (isCompactPhone) 9.5.sp else 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
         }
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(entry.effectSummary.replace("\u26A1", " Flux"), color = Color.White, fontSize = if (isCompactPhone) 10.5.sp else 11.5.sp)
        Spacer(modifier = Modifier.height(1.dp))
        Text("TACTICAL: ${entry.tacticalNote.replace("\u26A1", " Flux")}", color = Color(0xFF94A3B8), fontSize = if (isCompactPhone) 9.5.sp else 10.sp, lineHeight = if (isCompactPhone) 12.sp else 13.sp)
       }
      }
     }
    }
   }

   1 -> {
    LazyColumn(
     modifier = Modifier.weight(1f).fillMaxWidth().background(Color(0xFF081426), RoundedCornerShape(10.dp))
      .border(1.dp, Color(0xFF1B3454), RoundedCornerShape(10.dp)).padding(if (isCompactPhone) 6.dp else 8.dp)
      .testTag("codex_playfield_rifts_list"),
     verticalArrangement = Arrangement.spacedBy(if (isCompactPhone) 6.dp else 8.dp),
    ) {
     item(key = "playfield_rifts_highlights") {
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(if (isCompactPhone) 5.dp else 8.dp)) {
       CodexHighlightCard(
        Modifier.weight(1f), "⏱ 45S RIFT CYCLES",
        "Playfield Rifts relocate every 45s outside the Safe Zone and never spawn inside Stasis Breach Jails or Rift Control rings.",
        Color(0xFF1B102E), Color(0xFFA855F7), Color(0xFFE9D5FF), isCompactPhone,
       )
       CodexHighlightCard(
        Modifier.weight(1f), "⚫ GRAVITY & PHANTOMS",
        "Gravity Rifts expand as they absorb bullets, bombs, and Holo Phantoms up to maximum singularity scale. Phantoms react to all 4 Rifts.",
        Color(0xFF0A2229), Color(0xFF2DD4BF), Color(0xFFCCFBF1), isCompactPhone,
       )
       CodexHighlightCard(
        Modifier.weight(1f), "📡 PAIRED PHASE RIFTS",
        "Radar colors: Crimson = Plasma Rift, White = Repulsor Rift, Violet = Gravity Rift, Teal/Cyan = Paired Phase Rifts (Phase Rift A & B).",
        Color(0xFF241016), Color(0xFFEF4444), Color(0xFFFECACA), isCompactPhone,
       )
      }
     }

     items(RiftCodex.RIFT_ANOMALIES_CODEX, key = { it.id }) { rift ->
      val accentColor = runCatching { Color(android.graphics.Color.parseColor(rift.accentHex)) }.getOrDefault(Color(0xFF38BDF8))
      Row(
       modifier = Modifier.fillMaxWidth().background(Color(0xFF0E1F38), RoundedCornerShape(8.dp))
        .border(1.dp, accentColor.copy(alpha = 0.55f), RoundedCornerShape(8.dp)).padding(if (isCompactPhone) 8.dp else 10.dp),
       verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(if (isCompactPhone) 10.dp else 12.dp),
      ) {
       RiftAnomalyIconPreview(riftId = rift.id, sizeDp = if (isCompactPhone) 56 else 66, accentColor = accentColor)
       Column(modifier = Modifier.weight(1f)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
         Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          Text(rift.title.replace("\u26A1", "Flux"), color = accentColor, fontSize = if (isCompactPhone) 12.sp else 13.5.sp, fontWeight = FontWeight.Bold)
          Box(modifier = Modifier.background(Color(0x22FFFFFF), RoundedCornerShape(4.dp)).padding(horizontal = 5.dp, vertical = 2.dp)) {
           Text(rift.classification, color = Color(0xFFCBD5E1), fontSize = 8.5.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
          }
         }
         Box(modifier = Modifier.background(accentColor.copy(alpha = 0.16f), RoundedCornerShape(6.dp)).border(1.dp, accentColor, RoundedCornerShape(6.dp)).padding(horizontal = 7.dp, vertical = 2.dp)) {
          Text(rift.specBadge.replace("\u26A1", " FLUX"), color = accentColor, fontSize = if (isCompactPhone) 9.5.sp else 10.5.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
         }
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(rift.effectSummary.replace("\u26A1", " Flux"), color = Color.White, fontSize = if (isCompactPhone) 10.5.sp else 11.5.sp)
        Spacer(modifier = Modifier.height(1.dp))
        Text("TACTICAL: ${rift.tacticalNote.replace("\u26A1", " Flux")}", color = Color(0xFF94A3B8), fontSize = if (isCompactPhone) 9.5.sp else 10.sp, lineHeight = if (isCompactPhone) 12.sp else 13.sp)
       }
      }
     }
    }
   }

   else -> {
    Column(modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(if (isCompactPhone) 6.dp else 8.dp)) {
     Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(if (isCompactPhone) 5.dp else 8.dp)) {
      CodexHighlightCard(
       Modifier.weight(1f), "🕹️ L-STICK: FLIGHT & DRIFT",
       "360° Newtonian thrust (>80% ignites Afterburners; release coasts indefinitely). Fires along heading when R-Stick is neutral.",
       Color(0xFF082032), Color(0xFF00F5D4), Color(0xFFE0F2FE), isCompactPhone,
      )
      CodexHighlightCard(
       Modifier.weight(1f), "🎯 R-STICK: 360° TURRET",
       "Deflect Right Stick at any time to aim 360° independently of your flight vector. Release to return aim to ship heading.",
       Color(0xFF1E1035), Color(0xFFC084FC), Color(0xFFF3E8FF), isCompactPhone,
      )
      CodexHighlightCard(
       Modifier.weight(1f), "🎮 LIVE BUTTON REMAPPER",
       "Default: RT=Main, R1=Buff, LT=Brake, X=Secondary, A=Repel, Y=Multi-Shot. Tap any slot below to rebind live!",
       Color(0xFF092318), Color(0xFF34D399), Color(0xFFD1FAE5), isCompactPhone,
      )
     }
     ControllerDiagramAndMapperCard(context = context)
    }
   }
  }
 }
}

@Composable
private fun CodexTabPill(
 label: String,
 showFluxEmblem: Boolean,
 selected: Boolean,
 accentColor: Color,
 isCompactPhone: Boolean,
 testTag: String,
 onClick: () -> Unit,
) {
 Box(
  modifier = Modifier.height(if (isCompactPhone) 24.dp else 30.dp).clip(RoundedCornerShape(6.dp))
   .background(
    if (selected) Brush.verticalGradient(listOf(accentColor.copy(alpha = 0.34f), accentColor.copy(alpha = 0.16f)))
    else Brush.verticalGradient(listOf(Color(0xFF0A1424), Color(0xFF060C16)))
   )
   .border(1.dp, if (selected) accentColor else Color(0xFF1E293B), RoundedCornerShape(6.dp))
   .clickable(onClick = onClick).padding(horizontal = if (isCompactPhone) 8.dp else 10.dp).testTag(testTag),
  contentAlignment = Alignment.Center,
 ) {
  Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
   if (showFluxEmblem) FluxEmblem(sizeDp = if (isCompactPhone) 10.dp else 11.dp)
   Text(label, color = if (selected) Color.White else Color(0xFF94A3B8), fontSize = if (isCompactPhone) 9.5.sp else 10.5.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace, maxLines = 1)
  }
 }
}

@Composable
private fun CodexHighlightCard(
 modifier: Modifier = Modifier,
 title: String,
 body: String,
 bgColor: Color,
 borderColor: Color,
 bodyColor: Color,
 isCompactPhone: Boolean,
) {
 Box(
  modifier = modifier.background(bgColor, RoundedCornerShape(8.dp)).border(1.dp, borderColor, RoundedCornerShape(8.dp))
   .padding(horizontal = if (isCompactPhone) 6.dp else 10.dp, vertical = if (isCompactPhone) 5.dp else 7.dp),
 ) {
  Column {
   Text(title, color = borderColor, fontSize = if (isCompactPhone) 9.5.sp else 10.5.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
   Text(body, color = bodyColor, fontSize = if (isCompactPhone) 8.5.sp else 9.5.sp, lineHeight = if (isCompactPhone) 11.sp else 12.5.sp, maxLines = if (isCompactPhone) 2 else 4, overflow = TextOverflow.Ellipsis)
  }
 }
}

@Composable
fun BuffIconPreview(buffType: Int, sizeDp: Int, accentColor: Color) {
 val bmp = remember(buffType) { RiftSpriteBaker.getBuffIconBitmap(buffType).asImageBitmap() }
 Box(
  modifier = Modifier.size(sizeDp.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFF030712)).border(1.5.dp, accentColor, RoundedCornerShape(8.dp)),
  contentAlignment = Alignment.Center,
 ) {
  Canvas(modifier = Modifier.size((sizeDp - 6).dp)) {
   drawImage(image = bmp, dstOffset = IntOffset.Zero, dstSize = IntSize(size.width.toInt(), size.height.toInt()))
  }
 }
}

@Composable
fun RiftAnomalyIconPreview(riftId: String, sizeDp: Int, accentColor: Color) {
 val infiniteTransition = rememberInfiniteTransition(label = "codex_rift_preview_$riftId")
 val spinAngle by infiniteTransition.animateFloat(
  initialValue = 0f, targetValue = (2.0 * PI).toFloat(),
  animationSpec = infiniteRepeatable(tween(3600, easing = LinearEasing), RepeatMode.Restart),
  label = "rift_spin_angle",
 )
 val pulsePhase by infiniteTransition.animateFloat(
  initialValue = 0f, targetValue = 1f,
  animationSpec = infiniteRepeatable(tween(1600, easing = LinearEasing), RepeatMode.Restart),
  label = "rift_pulse_phase",
 )
 val linkPath = remember { Path() }

 Box(
  modifier = Modifier.size(sizeDp.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFF030712)).border(1.5.dp, accentColor, RoundedCornerShape(8.dp)),
  contentAlignment = Alignment.Center,
 ) {
  Canvas(modifier = Modifier.fillMaxSize().padding(3.dp)) {
   val w = size.width; val h = size.height; val minDim = size.minDimension; val cx = w * 0.5f; val cy = h * 0.5f
   if (riftId == "phase_rift" || riftId == "phase_rift_pair") {
    val ax = w * 0.33f; val ay = h * 0.35f; val bx = w * 0.68f; val by = h * 0.66f
    val midX = (ax + bx) * 0.5f + sin(spinAngle * 2f) * (minDim * 0.06f)
    val midY = (ay + by) * 0.5f - sin(spinAngle * 2f) * (minDim * 0.06f)
    linkPath.reset(); linkPath.moveTo(ax, ay); linkPath.quadraticTo(midX, midY, bx, by)
    drawPath(linkPath, Brush.linearGradient(listOf(Color(0x662DD4BF), Color(0x6638BDF8)), Offset(ax, ay), Offset(bx, by)), style = Stroke(width = 4.0.dp.toPx(), cap = StrokeCap.Round))
    drawPath(linkPath, Brush.linearGradient(listOf(Color(0xFF2DD4BF), Color(0xFF38BDF8)), Offset(ax, ay), Offset(bx, by)), style = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round))
   }

   drawIntoCanvas { composeCanvas ->
    val nc = composeCanvas.nativeCanvas
    when (riftId) {
     "plasma_rift" -> RiftSpriteBaker.draw3dPlayfieldRift(
      nc, cx, cy, minDim * 0.38f, spinAngle,
      android.graphics.Color.rgb(239, 68, 68), android.graphics.Color.rgb(217, 70, 239), android.graphics.Color.rgb(255, 228, 230),
      darkVoidCenter = false, inwardSpiral = false, pulsePhaseOverride = pulsePhase,
     )
     "repulsor_rift" -> RiftSpriteBaker.draw3dPlayfieldRift(
      nc, cx, cy, minDim * 0.38f, -spinAngle * 1.15f,
      android.graphics.Color.rgb(56, 189, 248), android.graphics.Color.rgb(186, 230, 253), android.graphics.Color.rgb(255, 255, 255),
      darkVoidCenter = false, inwardSpiral = false, pulsePhaseOverride = pulsePhase,
     )
     "gravity_rift" -> RiftSpriteBaker.draw3dPlayfieldRift(
      nc, cx, cy, minDim * 0.38f, spinAngle * 1.35f,
      android.graphics.Color.rgb(168, 85, 247), android.graphics.Color.rgb(99, 102, 241), android.graphics.Color.rgb(233, 213, 255),
      darkVoidCenter = true, inwardSpiral = true, pulsePhaseOverride = pulsePhase,
     )
     else -> {
      val nodeR = minDim * 0.22f
      RiftSpriteBaker.draw3dPlayfieldRift(
       nc, w * 0.33f, h * 0.35f, nodeR, spinAngle,
       android.graphics.Color.rgb(45, 212, 191), android.graphics.Color.rgb(56, 189, 248), android.graphics.Color.rgb(240, 253, 250),
       darkVoidCenter = true, inwardSpiral = false, pulsePhaseOverride = pulsePhase,
      )
      RiftSpriteBaker.draw3dPlayfieldRift(
       nc, w * 0.68f, h * 0.66f, nodeR, -spinAngle,
       android.graphics.Color.rgb(56, 189, 248), android.graphics.Color.rgb(168, 85, 247), android.graphics.Color.rgb(240, 253, 250),
       darkVoidCenter = true, inwardSpiral = false, pulsePhaseOverride = pulsePhase,
      )
     }
    }
   }
  }
 }
}
