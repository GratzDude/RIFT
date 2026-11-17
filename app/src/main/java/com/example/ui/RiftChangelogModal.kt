package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

const val CURRENT_RIFT_BUILD_VERSION = 58

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
      buildNumber = 58,
      versionTag = "v1.0.58 // BUILD #58",
      releaseDate = "Nov 16, 2026",
      headline = "NEXUS Ring-Space 3D Shader Overhaul, Central Nexus Hub Station & 7 Living 3D Rifts",
      badgeLabel = "LATEST RELEASE",
      accentColorHex = "#00F5D4",
      highlights =
        listOf(
          "Rebranded outer game identity to NEXUS (developed by Threshold Dynamics Studio, LLC) with a 3D Singularity-Wing emblem while keeping all in-world Rifts, Rift Control, Rift Shards (❖), and Rift Frame systems intact.",
          "Rebuilt NEXUS CORE with a GPU AGSL RuntimeShader 3D Ring-Space containment sphere (with FBM fallback on API 24-32), a rotating 3D NEXUS HUB Orbital Station at center, and 7 frameless swirling 3D Rifts with zero rectangular node boxes or wireframe lines.",
          "Streamlined match staging in CONVERGENCE GATE so every Ranked Mode and 6-character Custom Rift transitions cleanly into HANGAR DECK hull selection before entering the Rift.",
        ),
    ),
    RiftVersionLogEntry(
      buildNumber = 57,
      versionTag = "v1.0.57 // BUILD #57",
      releaseDate = "Nov 15, 2026",
      headline = "Cloud SUBSPACE COMMS Hub, Resonance Conduit & 95% Glass Terminals",
      badgeLabel = "SUBSPACE COMMS",
      accentColorHex = "#38BDF8",
      highlights =
        listOf(
          "Launched live Firestore-synced SUBSPACE COMMS with #announcements ([DEV // ARCHITECT] badge), #bug-reports cloud telemetry, #shipwright-meta clickable Rift Frame cards, #strike-lfg room/wing invites, and Live Online Pilot Roster.",
          "Added RIFT CONDUIT Live & Peak Resonance progression (100-500 Starting Bounty scaling), 3 Daily ❖ Directives, and all 10 hulls unlocked from 0 Resonance.",
          "Added 6-character Private & Public Custom Rift Gate room codes, MegaFat Bullets custom sandbox rule, and cloud-synced Syndicate Crests and verified Sworn Pilot Strike Wing lookup.",
        ),
    ),
    RiftVersionLogEntry(
      buildNumber = 56,
      versionTag = "v1.0.56 // BUILD #56",
      releaseDate = "Nov 15, 2026",
      headline = "STAR FORGE Studio, 10-Ship 100-Pt Parity & 3D Playfield Rifts",
      badgeLabel = "STAR FORGE",
      accentColorHex = "#A855F7",
      highlights =
        listOf(
          "Launched STAR FORGE across Ship & Cosmetic Turntable, Track A-C Soundtrack & SFX Lab (60-180 BPM), Custom Buff Forge, Syndicate Crest Forge, and Interactive Playfield & HUD Studio.",
          "Added 10th Ship parity (Eclipse 6.0s Stasis & Disruptor Cloak/EMP rebalance), 8,000 Flux Stasis Breach Jail Cores, and Graphical Dual-Rift Tug-of-War HUD.",
          "Added Living-Teammate Spectator switcher, pure vector FluxEmblem HUD readouts, and 100% real-player Syndicate War ladders.",
        ),
    ),
    RiftVersionLogEntry(
      buildNumber = 47,
      versionTag = "v1.0.47 // BUILD #47",
      releaseDate = "Nov 15, 2026",
      headline = "NEXUS Boot Flow, 3D Rifts, Fixed Action Buttons & Procedural Battle Music",
      badgeLabel = "CORE ARENA",
      accentColorHex = "#60A5FA",
      highlights =
        listOf(
          "Updated boot flow to enter Unjoined NEXUS Orbit first until explicit mode deployment.",
          "Upgraded circular minimap Flux capacitor arc, fixed combat action buttons, and 4-layer 3D singularities.",
        ),
    ),
    RiftVersionLogEntry(
      buildNumber = 30,
      versionTag = "v1.0.30 // BUILD #30",
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
      versionTag = "v1.0.20 // BUILD #20",
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
      versionTag = "v1.0.1 // BUILD #1",
      releaseDate = "Nov 10, 2026",
      headline = "NEXUS Core Newtonian RIFT Engine & Landscape Touch HUD Launch",
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

  val newUpdates =
    remember(lastSeenBuild) {
      when {
        lastSeenBuild <= 0 -> listOf(RIFT_FULL_CHANGELOG.first())
        lastSeenBuild < CURRENT_RIFT_BUILD_VERSION ->
          RIFT_FULL_CHANGELOG.filter { it.buildNumber > lastSeenBuild }
        else -> emptyList()
      }
    }
  val previousUpdates =
    remember(lastSeenBuild) {
      when {
        lastSeenBuild <= 0 -> RIFT_FULL_CHANGELOG.drop(1)
        lastSeenBuild < CURRENT_RIFT_BUILD_VERSION ->
          RIFT_FULL_CHANGELOG.filter { it.buildNumber <= lastSeenBuild }
        else -> RIFT_FULL_CHANGELOG
      }
    }

  Box(
    modifier =
      Modifier.fillMaxSize()
        .background(Color(0xFF02050B))
        .windowInsetsPadding(WindowInsets.safeDrawing)
        .padding(horizontal = 16.dp, vertical = 10.dp)
        .testTag("rift_changelog_modal"),
    contentAlignment = Alignment.Center,
  ) {
    Surface(
      modifier =
        Modifier.fillMaxSize()
          .border(
            1.5.dp,
            Brush.linearGradient(listOf(Color(0xFF00F5D4), Color(0xFFA855F7), Color(0xFF38BDF8))),
            RoundedCornerShape(16.dp),
          ),
      color = Color(0xF2060D1A),
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
          Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Canvas(modifier = Modifier.size(28.dp)) {
              val w = size.width
              val h = size.height
              val cx = w * 0.5f
              val cy = h * 0.5f
              val r = minOf(w, h) * 0.44f
              drawCircle(
                brush =
                  Brush.radialGradient(
                    colors = listOf(Color(0x5500F5D4), Color(0x33A855F7), Color.Transparent),
                    center = Offset(cx, cy),
                    radius = r * 1.15f,
                  ),
                radius = r * 1.15f,
                center = Offset(cx, cy),
              )
              for (i in 0 until 3) {
                drawArc(
                  color = if (i % 2 == 0) Color(0xFF00F5D4) else Color(0xFFA855F7),
                  startAngle = i * 120f + 18f,
                  sweepAngle = 82f,
                  useCenter = false,
                  topLeft = Offset(cx - r, cy - r),
                  size = Size(r * 2f, r * 2f),
                  style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round),
                )
              }
              drawArc(
                color = Color(0xFFFACC15),
                startAngle = -20f,
                sweepAngle = 220f,
                useCenter = false,
                topLeft = Offset(cx - r * 0.92f, cy - r * 0.42f),
                size = Size(r * 1.84f, r * 0.84f),
                style = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round),
              )
              drawCircle(Color.White, radius = r * 0.22f, center = Offset(cx, cy))
            }
            Column {
              Text(
                text = "NEXUS // RING SPACE TRANSMISSION & VERSION LOG",
                color = Color(0xFF00F5D4),
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
              )
              Text(
                text =
                  if (hasNew) {
                    "CURRENT VERSION: v1.0.$CURRENT_RIFT_BUILD_VERSION (BUILD #$CURRENT_RIFT_BUILD_VERSION) • Changes since Build #$lastSeenBuild • Threshold Dynamics Studio, LLC"
                  } else {
                    "CURRENT VERSION: v1.0.$CURRENT_RIFT_BUILD_VERSION (BUILD #$CURRENT_RIFT_BUILD_VERSION) • Threshold Dynamics Studio, LLC"
                  },
                color = Color(0xFFFFD700),
                fontSize = 10.8.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace,
              )
            }
          }
          Spacer(modifier = Modifier.width(12.dp))
          Button(
            onClick = onAcknowledgeAndContinue,
            colors =
              ButtonDefaults.buttonColors(
                containerColor = Color(0xFF00F5D4),
                contentColor = Color(0xFF030B14),
              ),
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            modifier = Modifier.heightIn(min = 48.dp).testTag("changelog_btn_continue"),
          ) {
            Text(
              text = "[ACKNOWLEDGE & CONTINUE]",
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
          Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            FilterTabPill(
              label = "ALL VERSIONS (${RIFT_FULL_CHANGELOG.size})",
              selected = selectedFilter == ChangelogFilter.ALL_VERSIONS,
              accentColor = Color(0xFF00F5D4),
              testTag = "changelog_tab_all",
              onClick = { selectedFilter = ChangelogFilter.ALL_VERSIONS },
            )
            if (hasNew) {
              FilterTabPill(
                label = "NEW SINCE LAST VISIT (${newUpdates.size})",
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
                statusBadge =
                  if (entry.buildNumber == CURRENT_RIFT_BUILD_VERSION) "LATEST BUILD" else "NEW UPDATE",
                highlighted = true,
              )
            }
          }
          if (selectedFilter == ChangelogFilter.ALL_VERSIONS && previousUpdates.isNotEmpty()) {
            items(items = previousUpdates, key = { "prev_${it.buildNumber}" }) { entry ->
              VersionLogCard(
                entry = entry,
                statusBadge =
                  if (entry.buildNumber == CURRENT_RIFT_BUILD_VERSION) "LATEST BUILD" else "ARCHIVED",
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
        .border(
          if (selected) 1.5.dp else 1.dp,
          if (selected) accentColor else Color(0x337C9BB5),
          RoundedCornerShape(8.dp),
        )
        .clickable(onClick = onClick)
        .padding(horizontal = 12.dp, vertical = 8.dp)
        .testTag(testTag),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      label,
      color = if (selected) accentColor else Color(0xFF9BB4CA),
      fontSize = 11.sp,
      fontWeight = FontWeight.Bold,
      fontFamily = FontFamily.Monospace,
    )
  }
}

@Composable
private fun VersionLogCard(entry: RiftVersionLogEntry, statusBadge: String, highlighted: Boolean) {
  val accentColor =
    remember(entry.accentColorHex) {
      val cleaned = entry.accentColorHex.removePrefix("#")
      val v = cleaned.toLongOrNull(16) ?: 0x00F5D4L
      if (cleaned.length <= 6) Color(0xFF000000L or v) else Color(v)
    }
  Column(
    modifier =
      Modifier.fillMaxWidth()
        .background(
          if (highlighted) Color(0xFF0C1A30) else Color(0xFF081222),
          RoundedCornerShape(12.dp),
        )
        .border(
          if (highlighted) 1.5.dp else 1.dp,
          if (highlighted) accentColor else accentColor.copy(alpha = 0.42f),
          RoundedCornerShape(12.dp),
        )
        .padding(12.dp)
        .testTag("changelog_card_build_${entry.buildNumber}"),
    verticalArrangement = Arrangement.spacedBy(6.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        "${entry.versionTag} • ${entry.releaseDate} • ${entry.badgeLabel}",
        color = accentColor,
        fontSize = 11.sp,
        fontWeight = FontWeight.ExtraBold,
        fontFamily = FontFamily.Monospace,
      )
      Text(
        statusBadge,
        color = if (highlighted) Color(0xFF00F5D4) else Color(0xFF7C9BB5),
        fontSize = 10.sp,
        fontWeight = FontWeight.ExtraBold,
        fontFamily = FontFamily.Monospace,
      )
    }
    Text(
      entry.headline,
      color = Color.White,
      fontSize = 13.5.sp,
      fontWeight = FontWeight.Bold,
      fontFamily = FontFamily.SansSerif,
    )
    entry.highlights.forEach { bullet ->
      Text(
        "> $bullet",
        color = Color(0xFFD5E6F7),
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontFamily = FontFamily.SansSerif,
      )
    }
  }
}
