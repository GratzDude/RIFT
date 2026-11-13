package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.height
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

const val CURRENT_RIFT_BUILD_VERSION = 20

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
      buildNumber = 20,
      versionTag = "v1.20 // BUILD #20",
      releaseDate = "Nov 13, 2026",
      headline = "Pre-Auth Changelog Transmission & Native Twin-Stick Controller Support",
      badgeLabel = "LATEST RELEASE",
      accentColorHex = "#00F5D4",
      highlights =
        listOf(
          "Added pre-sign-in Sector Transmission & Full Version Changelog modal tracking updates since your last session alongside the complete historical archive.",
          "Added plug-and-play Bluetooth & USB Gamepad support: Left Stick 360° Newtonian drift and >80% Afterburner ignition, plus Right Stick 360° independent turret aim.",
          "Mapped full controller combat & HUD navigation: Right Trigger / R1 Primary Fire, Left Trigger Secondary Warhead, L1 / X Special Buff, A / R3 Repel, B / L3 Manual Retro-Brake, Y Multi-Shot toggle, and D-Pad Hangar 3x3 matrix selection.",
          "Added live Gamepad Deadzone and Idle Touch Ring Auto-Dim sliders to Calibration.",
        ),
    ),
    RiftVersionLogEntry(
      buildNumber = 19,
      versionTag = "v1.19 // BUILD #19",
      releaseDate = "Nov 13, 2026",
      headline = "UGC Moderation, Pilot Blocking & High-Contrast Arena Polish",
      badgeLabel = "MODERATION & VISUALS",
      accentColorHex = "#FFD700",
      highlights =
        listOf(
          "Added User-Generated Content (UGC) profanity sanitization across Pilot Callsigns, Syndicate Tags, Custom Hull Names, and Syndicate Comms.",
          "Added in-game Pilot Reporting (⚑ REPORT) and one-tap Pilot Blocking (🚫 BLOCK) inside Syndicate Comms and Armada rosters.",
          "Upgraded arena visual scaling, high-contrast pilot nametag/bounty legibility, dark obsidian bulkhead wall seams, and 3-tier parallax starfield depth.",
        ),
    ),
    RiftVersionLogEntry(
      buildNumber = 18,
      versionTag = "v1.18 // BUILD #18",
      releaseDate = "Nov 13, 2026",
      headline = "Official Privacy Policy & Persistent Release Signing",
      badgeLabel = "PLATFORM & COMPLIANCE",
      accentColorHex = "#38BDF8",
      highlights =
        listOf(
          "Published Play Store compliant Privacy Policy covering anonymous Guest UIDs, optional Google Sign-In cloud sync, and UGC moderation workflows.",
          "Configured persistent Android release keystore signing so seamless over-the-air APK updates preserve local pilot data and preferences.",
        ),
    ),
    RiftVersionLogEntry(
      buildNumber = 17,
      versionTag = "v1.17 // BUILD #17",
      releaseDate = "Nov 12, 2026",
      headline = "Standardized 9-Ship Nomenclature & Personal Cockpit Settings",
      badgeLabel = "STANDARDIZATION",
      accentColorHex = "#00E676",
      highlights =
        listOf(
          "Standardized all 9 ship class names across HUD, Hangar, Codex, and telemetry: Viper, Ricochet, Suppressor, Goliath, Twinfang, Disruptor, Valkyrie, Manta, and Rift Frame.",
          "Separated personal User Settings (Camera Zoom 0.50x–1.50x and Playfield Text Scale 75%–200%) into dedicated persistent device storage.",
          "Enforced zero-Flux (0❖) 360° AoE EMP detonation when Disruptor breaks Stealth Cloak, plus wall-safe Phase Dash spatial step for Viper and Rift Frame.",
        ),
    ),
    RiftVersionLogEntry(
      buildNumber = 16,
      versionTag = "v1.16 // BUILD #16",
      releaseDate = "Nov 12, 2026",
      headline = "NEXUS Omniscient Rift Core AI & Firebase App Check",
      badgeLabel = "NEXUS AI",
      accentColorHex = "#B388FF",
      highlights =
        listOf(
          "Integrated NEXUS, the live tactical AI core bound directly to real-time arena telemetry, 100-point Rift Frame theorycrafting, and Cloud Leaderboard records.",
          "Enabled Firebase App Check protection alongside live AI Buff Forge and AI Hull Forge visual synthesis.",
        ),
    ),
    RiftVersionLogEntry(
      buildNumber = 15,
      versionTag = "v1.15 // BUILD #15",
      releaseDate = "Nov 12, 2026",
      headline = "Peak Bounty Executioner Telemetry & Syndicate Service Record",
      badgeLabel = "CAREER DOSSIER",
      accentColorHex = "#FFAB40",
      highlights =
        listOf(
          "Added Hall of Fame Peak Bounty Run snapshots tracking Time Survived, PVP/Bot Kills, Shot Accuracy, Damage Dealt/Taken, and full Executioner telemetry.",
          "Added persistent Syndicate Affiliation Service Record history and Independent Syndicate directory (SOLR, APEX, NOVA, VOID, ECLP) with 15-day allegiance lock.",
        ),
    ),
    RiftVersionLogEntry(
      buildNumber = 12,
      versionTag = "v1.12 // BUILD #12",
      releaseDate = "Nov 11, 2026",
      headline = "9th Ship (Rift Frame) Modular Builder & FFA Convergence",
      badgeLabel = "MAJOR EXPANSION",
      accentColorHex = "#FF5252",
      highlights =
        listOf(
          "Introduced the 9th Ship (Rift Frame) featuring 3 saveable slots (Frame A, Frame B, Frame C), a 100-point parity matrix across 8 base hulls, and procedural AI Hull Forge silhouettes.",
          "Added 4 Playfield Rifts (Plasma Rift, Repulsor Rift, Gravity Rift, Phase Rift) documented in the Codex.",
          "Launched Named Armadas (2–5 pilots with friendly fire off) and 60-minute Public FFA Convergence cycles.",
        ),
    ),
    RiftVersionLogEntry(
      buildNumber = 1,
      versionTag = "v1.00 // BUILD #1",
      releaseDate = "Nov 10, 2026",
      headline = "RIFT Core Newtonian Engine & Landscape Touch HUD Launch",
      badgeLabel = "FOUNDATION",
      accentColorHex = "#00F5D4",
      highlights =
        listOf(
          "Launched the 60+ FPS 2D Newtonian space combat arena featuring pure zero-friction drift on Left Stick release, >80% Afterburner ignition, and an independent manual [BRAKE] HUD button.",
          "Implemented Unified Flux (❖) power management where hull defense and weapon discharge draw from a single pool, paired with elastic 0.65 wall ricochets.",
          "Built Safe Zone disarmament protections, 15-second Re-Entry Lockout barriers, and real-time Firestore cloud sector telemetry.",
        ),
    ),
  )

private enum class ChangelogFilter {
  ALL_VERSIONS,
  NEW_ONLY,
}

@Composable
fun RiftChangelogModal(
  lastSeenBuild: Int,
  onAcknowledgeAndContinue: () -> Unit,
) {
  BackHandler { onAcknowledgeAndContinue() }

  val hasNewSincePriorSession = lastSeenBuild in 1 until CURRENT_RIFT_BUILD_VERSION
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
        .background(
          Brush.radialGradient(
            colors = listOf(Color(0xFF0A172B), Color(0xFF040812), Color(0xFF000000)),
            radius = 1400f,
          )
        )
        .windowInsetsPadding(WindowInsets.safeDrawing)
        .padding(horizontal = 16.dp, vertical = 10.dp)
        .testTag("rift_changelog_modal"),
    contentAlignment = Alignment.Center,
  ) {
    Surface(
      modifier =
        Modifier.fillMaxSize()
          .border(1.5.dp, Color(0xFF00F5D4), RoundedCornerShape(16.dp)),
      color = Color(0xF4060D1A),
      shape = RoundedCornerShape(16.dp),
      tonalElevation = 10.dp,
    ) {
      Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        // Header Row
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "📡 RIFT // SECTOR TRANSMISSION & FULL VERSION CHANGELOG",
              color = Color(0xFF00F5D4),
              fontSize = 15.sp,
              fontWeight = FontWeight.ExtraBold,
              fontFamily = FontFamily.Monospace,
              letterSpacing = 0.8.sp,
            )
            Spacer(modifier = Modifier.height(2.dp))
            val subtitle =
              if (hasNewSincePriorSession) {
                "CURRENT VERSION: v1.20 (BUILD #$CURRENT_RIFT_BUILD_VERSION) • Showing changes since Build #$lastSeenBuild + Complete Version Archive"
              } else {
                "CURRENT VERSION: v1.20 (BUILD #$CURRENT_RIFT_BUILD_VERSION) • Complete Chronological Patch Archive (Builds #$CURRENT_RIFT_BUILD_VERSION ➔ #1)"
              }
            Text(
              text = subtitle,
              color = Color(0xFFFFD700),
              fontSize = 11.sp,
              fontWeight = FontWeight.SemiBold,
              fontFamily = FontFamily.Monospace,
            )
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
            modifier =
              Modifier.heightIn(min = 48.dp)
                .testTag("changelog_btn_continue"),
          ) {
            Text(
              text = "[⚡ ACKNOWLEDGE & CONTINUE ▸]",
              fontSize = 12.sp,
              fontWeight = FontWeight.ExtraBold,
              fontFamily = FontFamily.Monospace,
            )
          }
        }

        // Filter / Jump Bar
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
              label = "⚡ ALL VERSIONS (${RIFT_FULL_CHANGELOG.size})",
              selected = selectedFilter == ChangelogFilter.ALL_VERSIONS,
              accentColor = Color(0xFF00F5D4),
              testTag = "changelog_tab_all",
              onClick = { selectedFilter = ChangelogFilter.ALL_VERSIONS },
            )
            if (hasNewSincePriorSession) {
              FilterTabPill(
                label = "🆕 NEW SINCE LAST VISIT (${newUpdates.size})",
                selected = selectedFilter == ChangelogFilter.NEW_ONLY,
                accentColor = Color(0xFFFFD700),
                testTag = "changelog_tab_new",
                onClick = { selectedFilter = ChangelogFilter.NEW_ONLY },
              )
            }
          }

          Box(
            modifier =
              Modifier.background(Color(0x1F00F5D4), RoundedCornerShape(6.dp))
                .border(1.dp, Color(0xFF00F5D4), RoundedCornerShape(6.dp))
                .padding(horizontal = 10.dp, vertical = 4.dp),
          ) {
            Text(
              text = "LATEST: BUILD #$CURRENT_RIFT_BUILD_VERSION",
              color = Color(0xFF00F5D4),
              fontSize = 10.5.sp,
              fontWeight = FontWeight.ExtraBold,
              fontFamily = FontFamily.Monospace,
            )
          }
        }

        // Scrollable Version List
        LazyColumn(
          modifier = Modifier.weight(1f).fillMaxWidth().testTag("changelog_list"),
          verticalArrangement = Arrangement.spacedBy(10.dp),
          contentPadding = PaddingValues(bottom = 8.dp),
        ) {
          if (isFreshOrUpdated && newUpdates.isNotEmpty()) {
            item(key = "section_new_updates") {
              SectionBanner(
                text = "⚡ CHANGES SINCE YOUR LAST VERSION (NEW UPDATES)",
                borderColor = Color(0xFFFFD700),
                backgroundColor = Color(0x1FFFD700),
                textColor = Color(0xFFFFD700),
              )
            }

            items(items = newUpdates, key = { "new_${it.buildNumber}" }) { entry ->
              val statusLabel =
                if (entry.buildNumber == CURRENT_RIFT_BUILD_VERSION) {
                  "⚡ LATEST BUILD"
                } else {
                  "🆕 NEW SINCE LAST LAUNCH"
                }
              VersionLogCard(
                entry = entry,
                statusBadge = statusLabel,
                highlighted = true,
              )
            }
          }

          if (selectedFilter == ChangelogFilter.ALL_VERSIONS && previousUpdates.isNotEmpty()) {
            item(key = "section_previous_updates") {
              SectionBanner(
                text =
                  if (isFreshOrUpdated) {
                    "📜 PREVIOUS VERSIONS — COMPLETE HISTORICAL LOG"
                  } else {
                    "📜 COMPLETE HISTORICAL LOG (BUILDS #$CURRENT_RIFT_BUILD_VERSION ➔ #1)"
                  },
                borderColor = Color(0x6600F5D4),
                backgroundColor = Color(0x1400F5D4),
                textColor = Color(0xFF00F5D4),
              )
            }

            items(items = previousUpdates, key = { "prev_${it.buildNumber}" }) { entry ->
              val statusLabel =
                if (entry.buildNumber == CURRENT_RIFT_BUILD_VERSION) {
                  "⚡ LATEST BUILD"
                } else {
                  "ARCHIVED BUILD"
                }
              VersionLogCard(
                entry = entry,
                statusBadge = statusLabel,
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
          width = if (selected) 1.5.dp else 1.dp,
          color = if (selected) accentColor else Color(0x337C9BB5),
          shape = RoundedCornerShape(8.dp),
        )
        .clickable(onClick = onClick)
        .padding(horizontal = 12.dp, vertical = 8.dp)
        .testTag(testTag),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      text = label,
      color = if (selected) accentColor else Color(0xFF9BB4CA),
      fontSize = 11.sp,
      fontWeight = FontWeight.Bold,
      fontFamily = FontFamily.Monospace,
    )
  }
}

@Composable
private fun SectionBanner(
  text: String,
  borderColor: Color,
  backgroundColor: Color,
  textColor: Color,
) {
  Box(
    modifier =
      Modifier.fillMaxWidth()
        .background(backgroundColor, RoundedCornerShape(8.dp))
        .border(1.dp, borderColor, RoundedCornerShape(8.dp))
        .padding(horizontal = 12.dp, vertical = 6.dp),
  ) {
    Text(
      text = text,
      color = textColor,
      fontSize = 11.5.sp,
      fontWeight = FontWeight.ExtraBold,
      fontFamily = FontFamily.Monospace,
      letterSpacing = 0.6.sp,
    )
  }
}

@Composable
private fun VersionLogCard(
  entry: RiftVersionLogEntry,
  statusBadge: String,
  highlighted: Boolean,
) {
  val accentColor = remember(entry.accentColorHex) { parseHexColor(entry.accentColorHex) }
  val borderColor = if (highlighted) accentColor else accentColor.copy(alpha = 0.42f)
  val cardBg = if (highlighted) Color(0xFF0C1A30) else Color(0xFF081222)

  Column(
    modifier =
      Modifier.fillMaxWidth()
        .background(cardBg, RoundedCornerShape(12.dp))
        .border(if (highlighted) 1.5.dp else 1.dp, borderColor, RoundedCornerShape(12.dp))
        .padding(12.dp)
        .testTag("changelog_card_build_${entry.buildNumber}"),
    verticalArrangement = Arrangement.spacedBy(6.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Box(
          modifier =
            Modifier.background(accentColor.copy(alpha = 0.18f), RoundedCornerShape(6.dp))
              .border(1.dp, accentColor, RoundedCornerShape(6.dp))
              .padding(horizontal = 8.dp, vertical = 3.dp),
        ) {
          Text(
            text = entry.versionTag,
            color = accentColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = FontFamily.Monospace,
          )
        }

        Text(
          text = "• ${entry.releaseDate}",
          color = Color(0xFF8EA8C3),
          fontSize = 11.sp,
          fontFamily = FontFamily.Monospace,
        )

        Box(
          modifier =
            Modifier.background(Color(0xFF13243E), RoundedCornerShape(5.dp))
              .border(1.dp, Color(0x448EA8C3), RoundedCornerShape(5.dp))
              .padding(horizontal = 6.dp, vertical = 2.dp),
        ) {
          Text(
            text = entry.badgeLabel,
            color = Color(0xFFD0E3F5),
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
          )
        }
      }

      val isLatestOrNew = statusBadge.startsWith("⚡") || statusBadge.startsWith("🆕")
      val statusColor =
        when {
          statusBadge.startsWith("⚡") -> Color(0xFF00F5D4)
          statusBadge.startsWith("🆕") -> Color(0xFFFFD700)
          else -> Color(0xFF7C9BB5)
        }
      Box(
        modifier =
          Modifier.background(
              if (isLatestOrNew) statusColor.copy(alpha = 0.16f) else Color(0xFF0D1829),
              RoundedCornerShape(6.dp),
            )
            .border(1.dp, statusColor.copy(alpha = 0.7f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
      ) {
        Text(
          text = statusBadge,
          color = statusColor,
          fontSize = 10.sp,
          fontWeight = FontWeight.ExtraBold,
          fontFamily = FontFamily.Monospace,
        )
      }
    }

    Text(
      text = entry.headline,
      color = Color.White,
      fontSize = 13.5.sp,
      fontWeight = FontWeight.Bold,
      fontFamily = FontFamily.SansSerif,
    )

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
      entry.highlights.forEach { bullet ->
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.Start,
          verticalAlignment = Alignment.Top,
        ) {
          Text(
            text = "▸ ",
            color = accentColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
          )
          Text(
            text = bullet,
            color = Color(0xFFD5E6F7),
            fontSize = 12.sp,
            lineHeight = 16.sp,
            fontFamily = FontFamily.SansSerif,
          )
        }
      }
    }
  }
}

private fun parseHexColor(hex: String): Color {
  val cleaned = hex.removePrefix("#")
  val value = cleaned.toLongOrNull(16) ?: return Color(0xFF00F5D4)
  return if (cleaned.length <= 6) {
    Color(0xFF000000L or value)
  } else {
    Color(value)
  }
}
