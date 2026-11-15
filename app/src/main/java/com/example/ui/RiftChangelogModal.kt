package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

const val CURRENT_RIFT_BUILD_VERSION = 56

data class RiftVersionLogEntry(
  val buildNumber: Int,
  val versionTag: String,
  val releaseDate: String,
  val headline: String,
  val badgeLabel: String,
  val accentColorHex: String,
  val highlights: List<String>,
)

val RIFT_FULL_CHANGELOG: List<RiftVersionLogEntry> =
  listOf(
    RiftVersionLogEntry(
      buildNumber = 56,
      versionTag = "v56.0 // BUILD #56",
      releaseDate = "Nov 15, 2026",
      headline = "NEXUS FORGE Studio, 9-Tab Persistent Shell, 10-Ship 100-Pt Parity & 3D Playfield Rifts",
      badgeLabel = "LATEST RELEASE",
      accentColorHex = "#00F5D4",
      highlights =
        listOf(
          "Launched NEXUS FORGE across Ship & Cosmetic Turntable, Track A–C Soundtrack & SFX Lab (60–180 BPM), Custom Buff Forge, Syndicate Crest Forge, and Interactive Playfield & HUD Studio.",
          "Unified all 9 header tabs inside a single persistent NEXUS Master Window frame with seamless embedded panes.",
          "Added 10th Ship parity (Eclipse 6.0s Stasis Bubble & Disruptor Cloak/EMP rebalance), 8,000 Flux Stasis Breach Jail Cores, and Graphical Dual-Rift Tug-of-War HUD.",
          "Added Living-Teammate Spectator switcher, pure vector FluxEmblem HUD readouts, and 100% real-player Syndicate War ladders.",
        ),
    ),
    RiftVersionLogEntry(
      buildNumber = 47,
      versionTag = "v1.47 // BUILD #47",
      releaseDate = "Nov 15, 2026",
      headline = "NEXUS Boot Flow, 3D Rifts, Fixed Action Buttons & Procedural Battle Music",
      badgeLabel = "CORE ARENA",
      accentColorHex = "#38BDF8",
      highlights =
        listOf(
          "Updated boot flow to enter Unjoined NEXUS Orbit first until explicit mode deployment.",
          "Upgraded circular minimap Flux capacitor arc, fixed combat action buttons, and 4-layer 3D singularities.",
        ),
    ),
    RiftVersionLogEntry(
      buildNumber = 30,
      versionTag = "v1.30 // BUILD #30",
      releaseDate = "Nov 14, 2026",
      headline = "Rift Control Tug-of-War, Custom 10-Rule Sandbox & Pre-Match Ship Lock",
      badgeLabel = "MODES & SANDBOX",
      accentColorHex = "#A78BFA",
      highlights =
        listOf(
          "Added Rift Control 2-Rift Tug-of-War, Custom Game 10-Rule Sandbox cloner, and 128x128 3D metallic ship sprites.",
        ),
    ),
    RiftVersionLogEntry(
      buildNumber = 20,
      versionTag = "v1.20 // BUILD #20",
      releaseDate = "Nov 13, 2026",
      headline = "Native Twin-Stick Controller Support & UGC Safety Filters",
      badgeLabel = "CONTROLLER",
      accentColorHex = "#FFD700",
      highlights =
        listOf(
          "Added Bluetooth & USB Gamepad twin-stick flight/turret controls, Pilot Reporting/Blocking, and persistent release signing.",
        ),
    ),
    RiftVersionLogEntry(
      buildNumber = 1,
      versionTag = "v1.00 // BUILD #1",
      releaseDate = "Nov 10, 2026",
      headline = "RIFT Core Newtonian Engine & Landscape Touch HUD Launch",
      badgeLabel = "FOUNDATION",
      accentColorHex = "#00E676",
      highlights =
        listOf(
          "Launched zero-friction Newtonian drift, >80% Afterburner ignition, Unified Flux defense/weapon pool, Paired Phase Rifts, and Firestore telemetry.",
        ),
    ),
  )

private enum class ChangelogFilter { ALL_VERSIONS, NEW_ONLY }

@Composable
fun RiftChangelogModal(
  lastSeenBuild: Int,
  onAcknowledgeAndContinue: () -> Unit,
) {
  BackHandler { onAcknowledgeAndContinue() }
  val hasNew = lastSeenBuild in 1 until CURRENT_RIFT_BUILD_VERSION
  val isFreshOrUpdated = lastSeenBuild < CURRENT_RIFT_BUILD_VERSION
  var selectedFilter by remember(lastSeenBuild) { mutableStateOf(ChangelogFilter.ALL_VERSIONS) }

  val newUpdates = remember(lastSeenBuild) {
    when {
      lastSeenBuild <= 0 -> listOf(RIFT_FULL_CHANGELOG.first())
      lastSeenBuild < CURRENT_RIFT_BUILD_VERSION -> RIFT_FULL_CHANGELOG.filter { it.buildNumber > lastSeenBuild }
      else -> emptyList()
    }
  }
  val previousUpdates = remember(lastSeenBuild) {
    when {
      lastSeenBuild <= 0 -> RIFT_FULL_CHANGELOG.drop(1)
      lastSeenBuild < CURRENT_RIFT_BUILD_VERSION -> RIFT_FULL_CHANGELOG.filter { it.buildNumber <= lastSeenBuild }
      else -> RIFT_FULL_CHANGELOG
    }
  }

  Box(
    modifier =
      Modifier.fillMaxSize()
        .background(Brush.radialGradient(listOf(Color(0xFF0A172B), Color(0xFF040812), Color.Black), radius = 1400f))
        .windowInsetsPadding(WindowInsets.safeDrawing)
        .padding(horizontal = 16.dp, vertical = 10.dp)
        .testTag("rift_changelog_modal"),
    contentAlignment = Alignment.Center,
  ) {
    Surface(
      modifier = Modifier.fillMaxSize().border(1.5.dp, Color(0xFF00F5D4), RoundedCornerShape(16.dp)),
      color = Color(0xF4060D1A),
      shape = RoundedCornerShape(16.dp),
      tonalElevation = 10.dp,
    ) {
      Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "📡 RIFT // SECTOR TRANSMISSION & VERSION CHANGELOG",
              color = Color(0xFF00F5D4),
              fontSize = 15.sp,
              fontWeight = FontWeight.ExtraBold,
              fontFamily = FontFamily.Monospace,
            )
            Text(
              text =
                if (hasNew) "CURRENT VERSION: v54.0 (BUILD #$CURRENT_RIFT_BUILD_VERSION) • Changes since Build #$lastSeenBuild"
                else "CURRENT VERSION: v54.0 (BUILD #$CURRENT_RIFT_BUILD_VERSION) • Chronological Patch Archive",
              color = Color(0xFFFFD700),
              fontSize = 11.sp,
              fontWeight = FontWeight.SemiBold,
              fontFamily = FontFamily.Monospace,
            )
          }
          Spacer(modifier = Modifier.width(12.dp))
          Button(
            onClick = onAcknowledgeAndContinue,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00F5D4), contentColor = Color(0xFF030B14)),
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            modifier = Modifier.heightIn(min = 48.dp).testTag("changelog_btn_continue"),
          ) {
            Text(
              text = "[ACKNOWLEDGE & CONTINUE ▸]",
              fontSize = 12.sp,
              fontWeight = FontWeight.ExtraBold,
              fontFamily = FontFamily.Monospace,
            )
          }
        }

        Row(
          modifier =
            Modifier.fillMaxWidth()
              .background(Color(0xFF0B1629), RoundedCornerShape(10.dp))
              .border(1.dp, Color(0x3300F5D4), RoundedCornerShape(10.dp))
              .padding(horizontal = 10.dp, vertical = 6.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            FilterTabPill(
              label = "❖ ALL VERSIONS (${RIFT_FULL_CHANGELOG.size})",
              selected = selectedFilter == ChangelogFilter.ALL_VERSIONS,
              accentColor = Color(0xFF00F5D4),
              testTag = "changelog_tab_all",
              onClick = { selectedFilter = ChangelogFilter.ALL_VERSIONS },
            )
            if (hasNew) {
              FilterTabPill(
                label = "🆕 NEW SINCE LAST VISIT (${newUpdates.size})",
                selected = selectedFilter == ChangelogFilter.NEW_ONLY,
                accentColor = Color(0xFFFFD700),
                testTag = "changelog_tab_new",
                onClick = { selectedFilter = ChangelogFilter.NEW_ONLY },
              )
            }
          }
          Text(
            text = "LATEST: BUILD #$CURRENT_RIFT_BUILD_VERSION",
            color = Color(0xFF00F5D4),
            fontSize = 10.5.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = FontFamily.Monospace,
          )
        }

        LazyColumn(
          modifier = Modifier.weight(1f).fillMaxWidth().testTag("changelog_list"),
          verticalArrangement = Arrangement.spacedBy(10.dp),
          contentPadding = PaddingValues(bottom = 8.dp),
        ) {
          if (isFreshOrUpdated && newUpdates.isNotEmpty()) {
            items(items = newUpdates, key = { "new_${it.buildNumber}" }) { entry ->
              VersionLogCard(
                entry = entry,
                statusBadge = if (entry.buildNumber == CURRENT_RIFT_BUILD_VERSION) "LATEST BUILD" else "NEW UPDATE",
                highlighted = true,
              )
            }
          }
          if (selectedFilter == ChangelogFilter.ALL_VERSIONS && previousUpdates.isNotEmpty()) {
            items(items = previousUpdates, key = { "prev_${it.buildNumber}" }) { entry ->
              VersionLogCard(
                entry = entry,
                statusBadge = if (entry.buildNumber == CURRENT_RIFT_BUILD_VERSION) "LATEST BUILD" else "ARCHIVED",
                highlighted = entry.buildNumber == CURRENT_RIFT_BUILD_VERSION,
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun FilterTabPill(
  label: String,
  selected: Boolean,
  accentColor: Color,
  testTag: String,
  onClick: () -> Unit,
) {
  Box(
    modifier =
      Modifier.clip(RoundedCornerShape(8.dp))
        .background(if (selected) accentColor.copy(alpha = 0.22f) else Color(0xFF102038))
        .border(if (selected) 1.5.dp else 1.dp, if (selected) accentColor else Color(0x337C9BB5), RoundedCornerShape(8.dp))
        .clickable(onClick = onClick)
        .padding(horizontal = 12.dp, vertical = 8.dp)
        .testTag(testTag),
    contentAlignment = Alignment.Center,
  ) {
    Text(label, color = if (selected) accentColor else Color(0xFF9BB4CA), fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
  }
}

@Composable
private fun VersionLogCard(entry: RiftVersionLogEntry, statusBadge: String, highlighted: Boolean) {
  val accentColor = remember(entry.accentColorHex) {
    val cleaned = entry.accentColorHex.removePrefix("#")
    val v = cleaned.toLongOrNull(16) ?: 0x00F5D4L
    if (cleaned.length <= 6) Color(0xFF000000L or v) else Color(v)
  }
  Column(
    modifier =
      Modifier.fillMaxWidth()
        .background(if (highlighted) Color(0xFF0C1A30) else Color(0xFF081222), RoundedCornerShape(12.dp))
        .border(if (highlighted) 1.5.dp else 1.dp, if (highlighted) accentColor else accentColor.copy(alpha = 0.42f), RoundedCornerShape(12.dp))
        .padding(12.dp)
        .testTag("changelog_card_build_${entry.buildNumber}"),
    verticalArrangement = Arrangement.spacedBy(6.dp),
  ) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
      Text("${entry.versionTag} • ${entry.releaseDate} • ${entry.badgeLabel}", color = accentColor, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace)
      Text(statusBadge, color = if (highlighted) Color(0xFF00F5D4) else Color(0xFF7C9BB5), fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace)
    }
    Text(entry.headline, color = Color.White, fontSize = 13.5.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.SansSerif)
    entry.highlights.forEach { bullet ->
      Text("▸ $bullet", color = Color(0xFFD5E6F7), fontSize = 12.sp, lineHeight = 16.sp, fontFamily = FontFamily.SansSerif)
    }
  }
}
