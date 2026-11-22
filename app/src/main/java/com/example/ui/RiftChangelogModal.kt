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

const val CURRENT_RIFT_BUILD_VERSION = 105

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
      buildNumber = 105,
      versionTag = "v1.0.105 // BUILD #105",
      releaseDate = "Nov 22, 2026",
      headline = "GitHub Firebase Startup Crash Fix, Preserved Package Identity & Optional Landscape Feature Flag",
      badgeLabel = "LATEST RELEASE",
      accentColorHex = "#00F5D4",
      highlights =
        listOf(
          "Early Safe Firebase & Uncaught Exception Guard: Added RiftApplication registered in AndroidManifest.xml so FirebaseApp initializes early with format-valid fallback FirebaseOptions (applicationId, apiKey, projectId, and gcmSenderId) before any FirebaseAuth, FirebaseFirestore, RiftRepository, or RiftViewModel call runs on builds without google-services.json, paired with process-wide uncaught exception and coroutine startup guards in RiftApplication and MainActivity.",
          "Preserved Package Identity Across GitHub Releases: Stopped overwriting applicationId to com.game.rift in .github/workflows/build-apk.yml so GitHub APK and Play Store AAB builds preserve applicationId = com.aistudio.riftarena.vxpqzm and match Google Play Console and Firebase resources cleanly.",
          "Optional Landscape Hardware Feature Declaration: Added android.hardware.screen.landscape with android:required=false in AndroidManifest.xml so Google Play Console does not treat landscape sensor orientation as a restrictive hardware requirement.",
        ),
    ),
    RiftVersionLogEntry(
      buildNumber = 104,
      versionTag = "v1.0.104 // BUILD #104",
      releaseDate = "Nov 22, 2026",
      headline = "Play Console Version Code 104 Sync & Defensive First-Launch Crash Protection",
      badgeLabel = "LATEST RELEASE",
      accentColorHex = "#00F5D4",
      highlights =
        listOf(
          "Play Console Version Code 104 Build Pipeline Fix: Removed the release workflow override that replaced app/build.gradle.kts versionCode and versionName with the workflow run number (73), ensuring RIFT-release.aab and RIFT.apk retain versionCode 104 (v1.0.104) for clean Google Play Console uploads.",
          "Release Package Firebase Resource Alignment: Aligned the release package identifier in CI Google Services processing so Play Store and direct APK builds bundle matching Firebase initialization resources without duplicate string conflicts.",
          "Defensive Startup & First-Launch Crash Protection: Guarded MainActivity window setup, Firebase Auth / App Check initialization, Credential Manager, audio synthesizer boot, sprite prewarming, and saved profile hydration with safe try/catch fallbacks so device-specific runtime errors on first launch never close the app.",
        ),
    ),
    RiftVersionLogEntry(
      buildNumber = 103,
      versionTag = "v1.0.103 // BUILD #103",
      releaseDate = "Nov 22, 2026",
      headline = "4-Card Rift Hub, Tall Clickable Info Pills, Scrollable Side-Bays, Rift Frame, Star Forge Shader/Splat Lab in Helm Systems, Option #1 Overwatch Inspector, Custom 5% Adjusters & Survival Telemetry",
      badgeLabel = "LATEST RELEASE",
      accentColorHex = "#00F5D4",
      highlights =
        listOf(
          "4-Card Rift Hub, ~2x Taller Clickable Info Pills & Vertically Scrollable Side-Bays: Streamlined Rift Hub peripheral hardware cards from 6 to 4 (Hangar Deck and Rift Frame on the left wing; Subspace Comms and Active Rifts on the right wing), doubled the height of the top and bottom info pills with direct click-through triggers (Callsign to Pilot Dossier, Resonance to Rift Conduit Rank & Daily Directives, Rift Shards to Shard Store, and Syndicate / Squadron to Syndicate & Squadron with Resonance/Activity sorting), made flanking side-bays vertically scrollable beside the 1.0x to 6.0x Overwatch viewport, renamed All Modes to Active Rifts, kept Subspace Comms full-screen with the old multi-tab top bar stripped out, and kept CHARGE [Flux icon] locking ships without auto-launching.",
          "Clean Hangar Ship Names, Dedicated Rift Frame Bay & Star Forge Shader/Splat Lab in Helm Systems: Stripped numeric prefixes from ship names across Hangar, Overwatch prompts, and Dossier (displaying VIPER, BASTION, and RIFT FRAME cleanly), wired Rift Frame from both the left-wing Hub card and Ship #10 in Hangar to the Frame A-E 100-Point loadout and Resonance-unlocked cosmetic buff bays, relocated Developer Star Forge 1:1 into Helm Systems alongside audio/display controls, cloud sync health, update check, account switcher, and Tactical Codex, and expanded Star Forge with AGSL shader and soft Gaussian/splat tuning controls on the 1:1 playfield (180px safe-zone core) with single-tap clean exit back to Rift Hub.",
          "Option #1 Overwatch Readout & Tappable Match Inspector: Removed RIFT-01, RIFT #01/#02/#03, and [FAV] tags from Overwatch, cycling strictly between public Sector Convergence, public Flux Core, and your own eligible Custom Game via top-left up/down chevrons with Line 1 match name and Line 2 MM:SS LEFT, Resonance crest value, and real human pilot count; tapping the Overwatch readout opens scrollable match intel on the left and every real human pilot in that Rift on the right with tap-to-inspect circular Overwatch dossiers and Squad Up.",
          "Custom Game 5% Damage Taken & Weapon Flux Cost Adjusters + Filterable Browser HTML Survival Spreadsheet: Added 5%-increment Damage Taken and Weapon Flux Cost sandbox scale adjusters (0% to 100%, default 100%) strictly inside Custom Games, tracked per-ship and session alive durations from spawn to elimination or match end across all 10 hulls and AI bots, and added an interactive filterable HTML telemetry spreadsheet plus 1-click CSV/JSON export in Pilot Dossier.",
        ),
    ),
    RiftVersionLogEntry(
      buildNumber = 102,
      versionTag = "v1.0.102 // BUILD #102",
      releaseDate = "Nov 21, 2026",
      headline = "True Top-to-Bottom Overwatch, Terminal Readout, Filtered Public/Custom Overwatch, Non-Launching Ship Charge & Clean Full-Screen Subspace Comms",
      badgeLabel = "LATEST RELEASE",
      accentColorHex = "#00F5D4",
      highlights =
        listOf(
          "True Top-to-Bottom Edge-to-Edge Overwatch Stage & Open-Corner Hardware Ergonomics: Removed horizontal bars and dead vertical padding above and below Overwatch so the live circular Overwatch radar viewport (1.0x to 6.0x pinch-to-zoom, drag pan, double-tap reset, gunmetal/cyan #091528, #00F5D4, and #38BDF8 bezel, 72 calibrated degree ticks, and top-left optical specular meniscus highlight) spans top-to-bottom across phones and 8.8-inch tablets with >= 48dp touch targets.",
          "Terminal-Style Top-Left Overwatch Readout, Up/Down Chevrons & Strict Public/Custom Match Filter: Framed crisp monospace terminal telemetry between simple up and down chevrons in the upper-left open space outside Overwatch showing active game mode/name, live 1-hour reset or match countdown, average Resonance, and real human player count, placed Rift In opposite in the upper-right open space, placed contextual Back/Cancel and Charge/Confirm controls in the lower-left and lower-right open spaces, and filtered Overwatch strictly to public Sector Convergence, public Flux Core, and your own active Custom Game only when its base mode is Sector Convergence or Flux Core.",
          "Non-Launching CHARGE [Flux Icon] Ship Lock Returning Cleanly to Rift Hub: Tapping CHARGE [Flux icon] (matched 1:1 to CHARGE text scale) locks that ship across all game modes with orbiting Flux icons and dissolves cleanly back to the Rift Hub without automatically launching into the match currently shown in Overwatch.",
          "Unified Side-Bay & Circular Overwatch Glass Prompt Flow + Clean Full-Screen Subspace Comms Header: Routed all non-full-screen Hub views (Hangar, Squadron, Syndicate, Game Mode Selector, Rift Conduit, Pilot Dossier, and Helm Systems / Tactical Codex) into flanking side-bays and circular Overwatch glass prompts with a persistent top-left Rift Hub return button, while keeping Subspace Comms full-screen and stripping out the old top multi-tab navigation bar.",
        ),
    ),
    RiftVersionLogEntry(
      buildNumber = 101,
      versionTag = "v1.0.101 // BUILD #101",
      releaseDate = "Nov 21, 2026",
      headline = "Edge-to-Edge Overwatch Rift Hub, Digitizing Side-Bays, Circular Glass Prompts & Charge Flux Ship Lock",
      badgeLabel = "LATEST RELEASE",
      accentColorHex = "#00F5D4",
      highlights =
        listOf(
          "Edge-to-Edge Overwatch Centerpiece & Sculpted Gunmetal/Cyan Hardware Terminal: Expanded the live circular Overwatch stage (1.0x to 6.0x pinch-to-zoom, drag pan, double-tap reset, Favorite pin, Highest Combined Resonance default, Rift In, and All Modes) edge-to-edge across the full viewport with zero outer margin frame, framed by a sculpted gunmetal and cyan radar bezel (#091528 and #0B192E with #00F5D4 and #38BDF8 accents), 72 calibrated degree ticks, radial reticle lines, top-left specular meniscus highlights, >= 48dp hardware bays in deep abyssal slate (#02050B, #030711, and #060D1A), and an anchored top-left Rift Hub return trigger.",
          "Digitizing 10-Ship Hangar Side-Bays, Circular Glass Ship Prompt & Charge Flux Orbiting Lock: Tapping Hangar Deck smoothly digitizes the flanking bays beside Overwatch into 10 slowly spinning 2.5D ships (5 on the left and 5 on the right). Tapping any ship opens a circular glass stats prompt confined inside the Overwatch lens with a CHARGE [Flux icon] button matched 1:1 to the CHARGE text scale that locks that ship across all game modes until another ship is charged, rings the ship with rotating Flux icons, and carries that charged ship with its orbiting Flux icons back into the Rift Hub view.",
          "Clean Squadron & Syndicate Split, Circular Pilot Dossier & Playfield-Style Right Formation: Split Squadron and Syndicate into dedicated Rift Hub hardware bays. Tapping Squadron digitizes the left bay into a filterable online pilot roster (All, Syndicate, and Friends) displaying each pilot's Best Ship 2.5D graphic before their callsign, opens a circular glass dossier with Squad Up controls inside Overwatch when a pilot is tapped, and renders locked-in 2.5D ship graphics on the right with ship Resonance above and callsign below matching 2.5D playfield style.",
          "Digitizing Game Mode Selector Flanking Bays & Full-Screen Subspace Comms With Voice Controls: Wired All Modes into smooth digitizing flanking bays beside Overwatch for all 6 competitive modes, Public Custom Game rooms, and 6-character Private Join Codes in plain-English Rift terminology, while keeping Subspace Comms full-screen with Rift AI and runtime-permission Team Match and Squadron voice chat controls preserved.",
        ),
    ),
    RiftVersionLogEntry(
      buildNumber = 100,
      versionTag = "v1.0.100 // BUILD #100",
      releaseDate = "Nov 21, 2026",
      headline = "Overwatch-Anchored Radial 2.5D/3D Rift Hub UI Redesign & Multi-Device Touch Polish",
      badgeLabel = "LATEST RELEASE",
      accentColorHex = "#00F5D4",
      highlights =
        listOf(
          "Expanded Dimensional Overwatch Centerpiece & 72-Tick Vernier Bezel: Enlarged the live circular Overwatch viewport (1.0x to 6.0x pinch-to-zoom, drag pan, double-tap reset, Favorite pin, and Highest Combined Resonance default) inside a precision multi-stepped 3D titanium collar with 72 radial azimuth ticks, 4 optical lock clamps, inward-rippling polar matrix rings, meniscus specular and chromatic dispersion highlights, and arc-contoured top sector selector and bottom Rift In / All Modes cradles.",
          "Orbital C-Arc Wing Geometry & Concave-Contoured 2.5D/3D Module Plates: Staggered and arc-contoured the six surrounding Rift Hub module plates (Ships & Builds, Star Forge, Comms & Rift AI, Rift Conduit, Syndicate & Squadron, and Pilot Dossier) around the circular Overwatch equator with concave inner bezels, under-chassis ambient occlusion shadows, stepped obsidian readout wells, illuminated coupling manifolds, and 24-tick 3D turret emblem sockets.",
          "Concentric Radial Bridge Chassis & Sculpted Internal Hub Decks: Anchored the Rift Hub bridge backdrop directly to the Overwatch center with polar astrogation gridlines, dual beveled bulkhead rings, and six dual-walled plasma power conduits, while upgrading Convergence Gate and Subspace Comms & Rift AI panels to matching multi-layered 2.5D hardware bevels.",
          "Universal >= 48dp Touch Targets & Phone / 8.8-Inch Tablet Fit: Tuned responsive wing-to-center proportions, cradle widths, and >= 48dp interactive hit targets via BoxWithConstraints and WindowInsets.safeDrawing so compact phones, ultrawide phones, foldables, and 8.8-inch tablets render with generous clearance and zero clipping.",
        ),
    ),
    RiftVersionLogEntry(
      buildNumber = 99,
      versionTag = "v1.0.99 // BUILD #99",
      releaseDate = "Nov 21, 2026",
      headline = "Sculpted 2.5D/3D Rift Hub Redesign Framing the Dimensional Overwatch Centerpiece",
      badgeLabel = "LATEST RELEASE",
      accentColorHex = "#00F5D4",
      highlights =
        listOf(
          "Dimensional Overwatch Centerpiece Bezel & Zero-Overlap 3-Tier Bay: Framed the live circular Overwatch viewport (1.0x to 6.0x pinch-to-zoom, drag pan, double-tap reset, Favorite pin, and Highest Combined Resonance default) inside a dedicated 3-tier bay with a sculpted titanium collar, 36 radial azimuth ticks, optical meniscus specular rim highlights, top instance selector bridge, and bottom Rift In plus All Modes launch actuators.",
          "Sculpted 2.5D/3D Radial-Coupled Bridge Module Plates: Replaced flat rectangular Rift Hub cards with six contoured multi-layer hardware plates (Ships & Builds, Star Forge, Comms & Rift AI, Rift Conduit, Syndicate & Squadron, and Pilot Dossier) featuring 3D turret emblem sockets, beveled specular rims, illuminated Overwatch coupling conduits, and distinct non-duplicated live readouts.",
          "Armored Top Command Crown & >= 48dp Touch Ergonomics: Redesigned the Rift Hub top command bar with recessed dark-glass telemetry wells, tactile beveled hardware switches for Pilot Dossier, Switch Account, Rift Conduit, Syndicate, Squadron, Guide, Helm Systems, Rift Out, and Practice Rift, and generous >= 48dp touch targets throughout.",
          "Responsive Multi-Form-Factor Bridge Scaling: Tuned Rift Hub wing-to-viewport proportions, typography, and spacing via BoxWithConstraints and WindowInsets.safeDrawing so both compact landscape phones and 8.8-inch tablets render with crisp hierarchy and zero clipping.",
        ),
    ),
    RiftVersionLogEntry(
      buildNumber = 98,
      versionTag = "v1.0.98 // BUILD #98",
      releaseDate = "Nov 21, 2026",
      headline = "2.5D Arena & HUD Team Readability, Destructive Action Confirmation Guards & In-Place Pill Controls",
      badgeLabel = "LATEST RELEASE",
      accentColorHex = "#00F5D4",
      highlights =
        listOf(
          "2.5D Grounded Tactical Rings & Allied Overhead Flux Bars: Added perspective-grounded under-ship tactical rings in the 2.5D arena featuring an Emerald-Cyan dual ring and forward heading chevron for your ship, Sky-Blue rings and overhead Flux micro-bars for allied ships, and Crimson-Coral rings with Solar-Gold leader accents for enemy ships.",
          "Relative Your Team vs Enemy Team Scoreboards & Off-Screen Edge Chevrons: Updated the top ranked mode banner and Rift Control tug-of-war bar to display Your Team in Sky-Blue on the left and Enemy Team in Crimson on the right relative to your frequency, paired with HUD-safe off-screen directional chevrons and distance readouts for allies and active mode objectives.",
          "Confirmation Guards on Reset Pilot & Switch Account: Added confirmation dialogs before executing Reset Pilot in Pilot Dossier or Switch Account in Rift Hub and Helm Systems to prevent accidental single-tap progress resets or account sign-outs.",
          "In-Place Ship & Frame Pill Controls Plus Unified Callsign Rename: Kept Hangar ship selection always valid without deselecting on re-tap, made Frame A to E pills on Ship 10 interactive in place, made the Convergence Gate Ship pill selectable in place across all 10 hulls, and unified Callsign rename rules across Hangar and Pilot Dossier (first rename free per account, 250 Rift Shards thereafter).",
        ),
    ),
    RiftVersionLogEntry(
      buildNumber = 97,
      versionTag = "v1.0.97 // BUILD #97",
      releaseDate = "Nov 20, 2026",
      headline = "Smooth Rift Hub Card Transitions, Squadron Sorting, Live Multi-Player Combat Sync & Cloud Custom Game Directory",
      badgeLabel = "LATEST RELEASE",
      accentColorHex = "#00F5D4",
      highlights =
        listOf(
          "Smooth Rift Hub 6-Card Slide & Fade Transitions: Added directional slide and fade transitions when entering, exiting, or switching directly between the 6 Rift Hub cards (Ships, Forge, Comms, Conduit, Syndicate, and Dossier) via quick-switch header pills without occluding the live Overwatch aperture.",
          "Squadron Management Sorting by Activity or Resonance: Added interactive Activity and Resonance sort toggles in Squadron Command so pilots can organize Squadron members and linked Sworn Pilots by live combat/online status or Resonance level with real-time Resonance badges.",
          "Continuous Real-Time Multi-Player Position, Weapon & Hit Sync: Fixed a timestamp merge bug where initial null server timestamps froze remote pilots at Rift Station Safe Harbor, replaced concurrent presence writes with a single-flight conflated ~9Hz telemetry channel, and added reflection-safe pilot deserialization plus deduplicated HIT combat reconciliation.",
          "Cloud Public Custom Game List & 6-Character Private Join Codes: Added Firestore-synced custom game rooms (/custom_games/{roomCode}) with deterministic sector IDs, a live Public Custom Game List in Convergence Gate and Overwatch, and instant Private Custom Game lookup and entry via 6-character Join Codes.",
        ),
    ),
    RiftVersionLogEntry(
      buildNumber = 96,
      versionTag = "v1.0.96 // BUILD #96",
      releaseDate = "Nov 20, 2026",
      headline = "Official RIFT Winged Vortex Crest Adaptive & Multi-Density App Icon",
      badgeLabel = "PREVIOUS BUILD",
      accentColorHex = "#00F5D4",
      highlights =
        listOf(
          "High-Definition RIFT Winged Vortex Crest Adaptive Icon: Installed the official gunmetal, gold, and cyan-magenta swirling vortex crest artwork inside the 66dp Material You safe zone of ic_launcher_foreground.xml over a seamless #03060B deep-space background.",
          "Full Multi-Density Square & Circular PNG Launcher Suite: Generated crisp PNG32 ic_launcher.png and circular-masked RGBA ic_launcher_round.png icons across mdpi, hdpi, xhdpi, xxhdpi, and xxxhdpi densities for universal Android launcher and Play Store compatibility.",
        ),
    ),
    RiftVersionLogEntry(
      buildNumber = 95,
      versionTag = "v1.0.95 // BUILD #95",
      releaseDate = "Nov 20, 2026",
      headline = "Rift Hub Card Refinement, Rift AI & Squadron Naming Standardization, and Direct Rift In Mode Flow",
      badgeLabel = "PREVIOUS BUILD",
      accentColorHex = "#00F5D4",
      highlights =
        listOf(
          "Refined 6-Card Rift Hub Layout: Top-left Hangar Deck displays the active or locked ship name beneath the title, overlays the pilot's Best Ship (or Ship #1 if none), and shows strictly BEST or LOCKED in the top-right badge; middle-left Starforge features a plain-English sub-label; bottom-left Subspace Comms hosts live chat and Rift AI.",
          "Clean Right-Dock Telemetry & Syndicate Identity: Top-right Rift Conduit shows Conduit Rank alongside the Resonance crest icon without redundant Shard counts; middle-right displays the pilot's active Syndicate name with Squadron status; bottom-right anchors Pilot Dossier.",
          "Rift Hub, Rift AI & Squadron Terminology Standardization: Standardized all hub headers, navigation pills, Subspace Comms channels, tactical pings, and fleet invites on Rift Hub, Rift AI, Rift Station, and Squadron in plain English.",
          "Streamlined Rift In Mode Selection Flow: Consolidated Overwatch around a single clean Rift In action that opens the scrollable Convergence Gate mode list with Sector Convergence at the top, entering the chosen Rift directly with the ship indicated on Hangar Deck (BEST or LOCKED).",
        ),
    ),
    RiftVersionLogEntry(
      buildNumber = 94,
      versionTag = "v1.0.94 // BUILD #94",
      releaseDate = "Nov 20, 2026",
      headline = "Droplet Reaction Visuals: Inward-Blooming Ingress Plumes, Reactive Precipitates & Filament Trails, and Refractive Lens Meniscus Rims",
      badgeLabel = "PREVIOUS BUILD",
      accentColorHex = "#00F5D4",
      highlights =
        listOf(
          "Localized Inward-Blooming Plasma Ingress Plumes: Playfield Rifts, Unstable Merged Rifts, Eclipse Stasis spheres, Stasis Breach containment domes, Rift Control capture zones, Rift Station Safe Harbor, Star Forge stage fields, and Codex previews now emit localized teardrop and bifurcated plasma plumes blooming inward along discrete angular ingress vectors.",
          "Reactive Crystalline Precipitates & Glowing Filament Trails: Gravity Rift and Unstable Rift projectile, ship-debris, and obstacle absorptions, Stasis dome reflections, Rift plasma or slingshot contacts, and outer Storm Wall strikes now nucleate faceted crystalline precipitate clusters and curved cyan, gold, and violet plasma filament trails along the reaction wake with zero solid white ship hit-flashes.",
          "Refractive Lens & Optical Meniscus Rim Highlights: Added crisp top-left optical meniscus specular crescents, counter-lit internal caustic refraction arcs, and gravitational throat lensing highlights across circular Rift collars, Stasis and Rift Control containment rims, Flux Core orbs, Rift Station Safe Harbor, Star Forge circular cores, and the Rift Hub Overwatch aperture, paired with plain-English Star Core and Resonance labels.",
        ),
    ),
    RiftVersionLogEntry(
      buildNumber = 93,
      versionTag = "v1.0.93 // BUILD #93",
      releaseDate = "Nov 20, 2026",
      headline = "Per-Account Callsign Isolation, Cross-Device Cloud Callsign Carry-Over, Live Matchmaking Queue ViewModel & Plain-English Resonance Targets",
      badgeLabel = "PREVIOUS BUILD",
      accentColorHex = "#00F5D4",
      highlights =
        listOf(
          "Strict Per-Account Callsign Isolation Across Shared Devices: Signed-in Sworn Pilot accounts now read and write callsigns and initial callsign flags strictly under per-UID keys without inheriting unscoped callsigns from previous accounts, and switching accounts or signing out automatically clears unscoped callsign state.",
          "Automatic Multi-Device Cloud Callsign Carry-Over: Signing into a Google account on another phone or tablet automatically syncs that pilot's saved cloud dossier callsign onto the device and active starfighter unless the pilot explicitly renamed their callsign during the current session.",
          "Live Matchmaking Queue ViewModel & Lobby Pilot Roster: Added a dedicated MatchmakingQueueViewModel managing ranked queue status transitions (Balanced +/- 250 RES window -> Open Resonance Bracket -> Match Locked / Cancelled / Timed Out), Strike Wing Resonance averages, live lobby pilot chips, and the 15.0-second ceiling.",
          "Switch Account Reset & Plain-English Resonance Target Wording: Wired Switch Account in Rift Hub and Helm Systems to cleanly cancel active queues, scrub unscoped session callsign keys, and return to the Auth Command Tablet, while standardizing Subspace Comms, Rift AI, and Syndicate Citadel on plain-English Resonance target phrasing.",
        ),
    ),
    RiftVersionLogEntry(
      buildNumber = 92,
      versionTag = "v1.0.92 // BUILD #92",
      releaseDate = "Nov 20, 2026",
      headline = "RIFT Title Artwork Loading Screen, Automatic On-Open Update Checker, Prominent Switch Account Flow & Complete Plain-English Resonance Scrub",
      badgeLabel = "PREVIOUS BUILD",
      accentColorHex = "#00F5D4",
      highlights =
        listOf(
          "Full-Screen RIFT Title Artwork Loading Screen & Auth Backdrop: Bundled high-definition RIFT title key art (metallic neon-trimmed RIFT emblem over a crackling purple-cyan plasma rift sphere with dogfighting starfighters) as a crisp responsive startup loading screen with plain-English initialization progress, echoed across the Auth Command Tablet backdrop.",
          "Automatic On-Open Update Check (Play Store + Sideload Releases): Added automatic launch-time update checking that triggers official Google Play In-App Updates on Play Store installs and checks GitHub Releases (GratzDude/RIFT) on direct APK installs with a non-blocking update banner and 1-tap release download link, plus manual update check controls in Helm Systems.",
          "Prominent Switch Account Button in Rift Hub & Helm Systems: Surfaced a dedicated Switch Account button in the Rift Hub top bar and at the top of Helm Systems (Audio & Account) that clears Google credentials, signs out of Firebase, resets Drifter guest bypass, and returns cleanly to the Auth screen for both Sworn Pilots and Drifters.",
          "Final Plain-English Resonance Scrub Across Comms, AI & Modals: Replaced remaining legacy HVT, Bounty/BTY, and Dyson phrasing across Subspace Comms quick pings, Rift AI starter chips and telemetry, Syndicate Citadel, Pilot Dossier, Helm Systems previews, and arena ship labels with plain-English Resonance and Rift Leader terminology.",
        ),
    ),
    RiftVersionLogEntry(
      buildNumber = 91,
      versionTag = "v1.0.91 // BUILD #91",
      releaseDate = "Nov 20, 2026",
      headline = "Silk-Smooth Remote Ship Motion: C1-Continuous Velocity Dead-Reckoning, Critically Damped Frame Smoothing & 95ms Active-Match Heartbeats",
      badgeLabel = "PREVIOUS BUILD",
      accentColorHex = "#00F5D4",
      highlights =
        listOf(
          "C1-Continuous Target & Error-Offset Tracking (Zero Packet Snapping): Incoming remote pilot heartbeats now update smooth target coordinates, velocity, heading, and turret aim while capturing continuous positional and velocity offsets relative to the live ship state so packet arrivals never yank coordinates or cause short-distance micro-hops.",
          "Closed-Form Critically Damped Dead-Reckoning Every Frame: Remote human ships integrate velocity continuously every simulation frame (x += vx * dt, y += vy * dt) paired with closed-form critically damped spring-damper decay (omega = 14.5 rad/s, zero overshoot), shortest-arc heading/aim smoothing, post-bounce offset alignment, and safe arenaRadius clamping, snapping immediately only on large warps (> 650px), fresh spawn/respawn, or sector entry.",
          "95ms Active-Match Heartbeats & Instant Burst Sync: Tightened active combat and matchmaking queue heartbeat cadence to 95ms (keeping 1,200ms in unjoined hub orbit) and wired instant out-of-band heartbeat pushes on spawn/respawn, afterburner ignition/release, manual brake engagement, Phase Dash, Hyper Surge / speed boosts, secondary ability fire, and sector changes.",
        ),
    ),
    RiftVersionLogEntry(
      buildNumber = 90,
      versionTag = "v1.0.90 // BUILD #90",
      releaseDate = "Nov 20, 2026",
      headline = "Rift Hub & Rift Station Rebrand, Cohesive Rift & Star Forge Visuals, Account/Drifter Switcher, Context-Aware Strike Wing Invites, Per-UID Cloud Resonance & Level Persistence, Above-Average Resonance Glow, Team Voice & Adaptive Bot Resonance",
      badgeLabel = "PREVIOUS BUILD",
      accentColorHex = "#00F5D4",
      highlights =
        listOf(
          "Rift Hub, Rift Station, Rift In / Rift Out & Plain-English Scrub: Standardized nomenclature across Auth, Rift Hub, Rift Station, Rift AI, Hangar, Star Forge, Pilot Dossier, Syndicates, Rift Conduit, Codex, and 2.5D HUD with plain-English labels and zero HVT, Dyson, Bounty/BTY, or developer jargon.",
          "In-App Account & Drifter Switcher + Context-Aware Strike Wing Invites: Added instant in-app switching between Google accounts, Sworn Pilot, and Drifter without uninstalling, plus real-time Strike Wing invites displaying a centered modal in Rift Hub, a compact top-left banner in public FFA Rifts, and automatic rejection during team matches.",
          "Per-UID Resonance & Pilot Level Persistence + Cloud Progression Sync: Isolated all 5 XP sources, Pilot Level, per-ship Resonance (1..10), Live/Peak Resonance, Rift Shards, combat stats, and Frame A-E loadouts per Sworn UID so swapping to Drifter or switching accounts never resets progression, backed by monotonic Firestore cloud sync.",
          "Above-Average Resonance Glow & Cohesive Rift / Star Core Visuals: Replaced HVT ship labels with a dynamic golden-cyan halo on every pilot above the active match's average Resonance, and unified Playfield Rifts and the central Star Forge Star Core with multi-layered swirling plasma filaments matching the outer Rift storm wall and Stasis spheres.",
          "Persisted Discord-Style Subspace Comms, Team Voice Chat, Adaptive Bot Skill & Hub Tap Telemetry: Persisted Subspace Comms locally and via Firestore across restarts, added runtime-permission Team Voice mic controls and speaking status in team matches and Strike Wing comms, scaled AI bot Resonance and combat skill dynamically to player Resonance with aggregated cloud bot telemetry, and added non-text Rift Hub screen tap logging.",
        ),
    ),
    RiftVersionLogEntry(
      buildNumber = 89,
      versionTag = "v1.0.89 // BUILD #89",
      releaseDate = "Nov 20, 2026",
      headline = "Smooth Per-Frame Remote Ship Dead-Reckoning, 110ms Active Combat Heartbeats & Post-Death Instance Sync Stability",
      badgeLabel = "PREVIOUS BUILD",
      accentColorHex = "#00F5D4",
      highlights =
        listOf(
          "Smooth Per-Frame Remote Ship Dead-Reckoning & Damped Interpolation: Replaced snapshot step-snapping on remote human ships with continuous per-frame velocity dead-reckoning (x += vx * dt, y += vy * dt) paired with frame-rate-independent damped position, velocity, heading, and turret-aim blending toward extrapolated network targets, snapping immediately only on large jumps (> 550px), sector entry, or respawn while clamping inside arenaRadius.",
          "110ms Active-Combat Heartbeats & Zero-Latency Lifecycle Broadcasts: Tightened active combat and matchmaking queue heartbeat cadence to 110ms (1,200ms in hub orbit) and wired instant out-of-band heartbeats on local ship elimination, respawn completion, match deployment, and round transitions.",
          "Post-Death & Round-Transition Rift Instance Sync Stability: Locked activeSectorInstanceOverride, currentCustomGame, activeRankedMode, and activeMatchSectorTag() across ship eliminations, respawns, Hangar hull swaps, and round resets while holding remote human respawns for authoritative spawn coordinates and safeguarding active sector pilots against stale lobby snapshot overwrites.",
        ),
    ),
    RiftVersionLogEntry(
      buildNumber = 88,
      versionTag = "v1.0.88 // BUILD #88",
      releaseDate = "Nov 19, 2026",
      headline = "Real-Time Multi-Device Presence & PvP Roster Sync Fix, Purge [GUE]/[NOV] Ghost Syndicates & Full Resonance Copy Scrub",
      badgeLabel = "PREVIOUS BUILD",
      accentColorHex = "#00F5D4",
      highlights =
        listOf(
          "Monotonic Multi-Device Presence & Combat Event Sync: Switched remote pilot freshness and combat event stream tracking in RiftRepository to monotonic SystemClock.elapsedRealtime() so cross-device clock skew and server timestamp resolution never drop active pilots after 45s or across app restarts.",
          "Instant Sector Alignment & Global Subspace Comms: Deploying into a match, leaving a match, restoring a suspended match, or opening Subspace Comms immediately aligns viewModel.setActiveSector(...) and fires pushLivePilotHeartbeatNow() so connected devices share canonical sector IDs, live combat events, and Subspace Comms.",
          "Zero-Wipe In-Place PvP Roster Updates: Remote pilots joining or updating in an active match now spawn and update cleanly in-place via replaceBotWithHuman without calling rebuildRosterForMode or resetting arena obstacles, timers, or local ship state.",
          "Human-Founded Syndicates Only (Purged [GUE] & [NOV] Ghosts): Added GUE, GUES, GUEST, NOV, and NOVA to reserved Syndicate tag filters, stopped truncating default GUEST tags into [GUE]/[GUES], removed preset NOVA VANGUARD defaults, and purged non-human Syndicate entries across local storage and cloud directory streams deduplicated by uppercase 1-3 char tag.",
          "100% Resonance Terminology Scrub: Replaced all remaining user-facing Bounty and BTY copy across Pilot Dossier, Syndicate Citadel, Rift Conduit, NEXUS AI Knowledge, Achievement descriptions, and 2.5D combat Kill Feed / FCT callouts with Resonance (RES).",
        ),
    ),
    RiftVersionLogEntry(
      buildNumber = 87,
      versionTag = "v1.0.87 // BUILD #87",
      releaseDate = "Nov 19, 2026",
      headline = "Multi-Device Live Online Sync, 5 Combat & Ranked Mode Fixes, Animated Minimap Flux Symbol, Chakram Disc & All Crate Buffs on Rift Frame",
      badgeLabel = "PREVIOUS BUILD",
      accentColorHex = "#00F5D4",
      highlights =
        listOf(
          "Multi-Device Live Online Presence, Matchmaking & Synchronized PvP Combat (RiftModels, RiftRepository, RiftViewModel, RiftGameEngine, MainActivity, firestore.rules): Aligned /sector_pilots list queries and security rules on visibility == 'public', added @PropertyName annotations on is-prefixed LivePilotState booleans, upgraded cloudReadyFlow with a reactive FirebaseAuth.AuthStateListener, fast-path single-write sector_pilots heartbeats (220ms active / 1.2s lobby) with self-healing schema fallback, canonical Overwatch & Resonance Matchmaking sector keys, deterministic cross-device West/East team placement, velocity dead-reckoning + remote secondary ability replication, real-time /sector_combat_events PvP hit/kill sync, /sector_instances Flux Core / Rift Control / Stasis Breach objective host sync, and live CloudSyncHealth diagnostics in NEXUS and Helm Systems.",
          "Station 10-Hit Defense Nova & Unstable Rift 10s Convergence Alert (RiftGameEngine, RiftHudOverlay, RiftArenaView): Restored Orbital and Asteroid Stations so shooting a station only increments that individual station's 1..10 counter and fires its 16-bolt 360-degree Station Defense Nova strictly on the 10th hit with zero per-shot salvo or neighbor cascade fire; added warp audio, trauma, and a top-priority pulsing 10.0s UNSTABLE RIFT CONVERGENCE countdown banner whenever two Playfield Rifts merge.",
          "Stasis Breach 3:00 Capture Scoring & AI Pathing, Rift Control 15s Lock & Live Timer, and Deathmatch Alive Counters (RiftGameEngine, RiftHudOverlay): Added forward lane spawns outside Stasis Jail spheres and AI Stasis Jail ray avoidance so bots engage immediately, plus a 3:00 Stasis Breach timer tracking team and personal Stasis Captures (instant win on Total Stasis Lockdown of all 10 enemies); wired live frameTick recomposition and a 15-second capture lock into RiftControlTugOfWarHudBar; added live WEST X/N ALIVE | EAST Y/N ALIVE counters to Rift Skirmish and Syndicate Wars.",
          "Animated Minimap Flux Symbol, Chakram Bouncing Disc Buff & All Crate Buffs on Rift Frame (RiftHudOverlay, ShipSpecs, RiftSpriteBaker, RiftArenaView, HangarModal, CodexModal): Replaced the plain leading dot on the circular minimap Flux gauge with a continuously spinning 3D Swirling Plasma Toroid Flux symbol; added the 8-slice spiked pizza-colored CHAKRAM disc Supply Crate buff (buffTypeIndex = 15, 4 ricochets, 560 Flux cleave); unlocked every Supply Crate buff as an equippable Rift Frame Secondary within the 100-Point Parity system.",
        ),
    ),
    RiftVersionLogEntry(
      buildNumber = 86,
      versionTag = "v1.0.86 // BUILD #86",
      releaseDate = "Nov 19, 2026",
      headline = "Flux Core Minimap Yellow-Dot Clarity — Suppress HVT Yellow/Gold Minimap Rings During Flux Core",
      badgeLabel = "LATEST RELEASE",
      accentColorHex = "#00F5D4",
      highlights =
        listOf(
          "Flux Core Minimap Yellow-Dot Clarity (RiftHudOverlay): Suppressed HVT yellow/gold minimap pulse rings on ships inside CircularRadarMinimap whenever engine.isFluxCoreRulesActive() is true so the Flux Core orb / carrier remains the sole yellow/gold indicator on the minimap, while preserving HVT minimap indicators intact across all non-Flux-Core modes.",
        ),
    ),
    RiftVersionLogEntry(
      buildNumber = 85,
      versionTag = "v1.0.85 // BUILD #85",
      releaseDate = "Nov 19, 2026",
      headline = "Canonical 3D Swirling Plasma Toroid Flux Icon Inside the Flux Core Orb, Carrier Containment Ring & 1:1 Star Forge Previews",
      badgeLabel = "FLUX CORE ICON",
      accentColorHex = "#00F5D4",
      highlights =
        listOf(
          "Canonical Flux Icon Inside the Live Flux Core Orb & Carrier Containment Indicator (RiftArenaView): Replaced the plain circle core inside drawFreeFluxCoreOrb with the canonical 3D Swirling Plasma Toroid / Tri-Arc Vortex Coil Flux icon (RiftSpriteBaker.drawFluxPrismIcon) centered and rotating cleanly inside the Flux Core's cyan capture ring, golden containment collar, pulsing containment aura, and FLUX CORE callout, and added an orbiting canonical Flux Core orb on the Flux Core carrier ship's containment ring.",
          "1:1 Star Forge Playfield & Element Inspector Parity for Flux Core (NexusForgeModal): Updated both the 1:1 Star Forge playfield Flux Core specimen and the Element Reforge Inspector preview (ForgeElementTarget.FLUX_CORE_ORB) via drawFluxCoreOrbSpecimen to render the exact same canonical 3D Swirling Plasma Toroid Flux icon, capture ring, containment collar, and pulsing aura 1:1 with live playfield rendering.",
        ),
    ),
    RiftVersionLogEntry(
      buildNumber = 84,
      versionTag = "v1.0.84 // BUILD #84",
      releaseDate = "Nov 19, 2026",
      headline = "Clean Single-Action Exit From 1:1 Star Forge to NEXUS Hub & Center Shifting Multicolored Dyson Sun Emitting Holographic 'STAR FORGE' Graphic Letters",
      badgeLabel = "DYSON SUN & HUB",
      accentColorHex = "#00F5D4",
      highlights =
        listOf(
          "Clean Single-Action Exit From 1:1 Star Forge Back to NEXUS Hub (NexusForgeModal, MainActivity): Wired the 1:1 STAR FORGE top-bar [< NEXUS HUB] exit control, 120Hz Test Flight HUD/bar exit controls, and system Back navigation to exit STAR FORGE straight back to the NEXUS Hub (ORBITAL_HUB) in a single action without dropping into an intermediate sub-tab view, while ensuring Back inside 1:1 STAR FORGE cleanly dismisses any open glass overlay or Test Flight session first and clears all overlay flags on exit.",
          "Center Shifting Multicolored Dyson Sun & Upward Holographic 'STAR FORGE' Graphic Letters (NexusForgeModal): Upgraded the Safe-Zone center (4000, 4000) 180px Dyson Sun centerpiece on the 1:1 STAR FORGE playfield with a continuously phase-shifting multicolored stellar photosphere, solar prominences, and 3D-occluded counter-rotating 5-stop metallic Dyson rings emitting upward volumetric projector rays, rising plasma motes, and custom beveled 3D holographic vector graphic letters spelling 'STAR FORGE', preserving 1-tap access to the Glass Dyson Creation Overlay without mutating live ranked state.",
        ),
    ),
    RiftVersionLogEntry(
      buildNumber = 83,
      versionTag = "v1.0.83 // BUILD #83",
      releaseDate = "Nov 19, 2026",
      headline = "1:1 Dyson Sun Star Forge & Supply Crate Buff Studio, Smooth Circular 2.5D Metallic Rifts & Nexus Station, Per-Ship Solar-Gold Resonance & Distinct Hangar Borders, 15s Resonance Matchmaking & Rift In, Plain-English Auth + Offline/Online Sync & Dynamic Aspect-Ratio Scaling",
      badgeLabel = "DYSON FORGE & RES",
      accentColorHex = "#00F5D4",
      highlights =
        listOf(
          "Plain-English Auth Screen, Tagline #2 & Seamless Offline/Online Multi-Device Sync (AuthScreen, strings.xml, RiftRepository, RiftViewModel, MainActivity, NexusAiConsoleModal): Rewrote the Auth screen in welcoming plain English featuring Tagline #2 ('Master the Rifts. Build Your Resonance. Rule the Sector.'), 'NEXUS STATION • LIVE SAFE HARBOR', and a live ONLINE / OFFLINE connectivity badge powered by NetworkConnectivityMonitor; enabled instant offline startup and Drifter/cached-pilot entry without network hangs, and implemented local-monotonic-time presence tracking in RiftRepository + active-sector initialization in RiftViewModel so online players sync reliably across devices regardless of device clock skew.",
          "Full-Screen 1:1 Dyson Sun Star Forge & Clickable Playfield + 15 Supply Crate Buff Specimens (NexusForgeModal, RiftSpriteBaker, MainActivity): Routed STAR FORGE directly into the unscaled 1:1 Playfield Forge centered on a 180px Dyson Sun with rotating 5-stop metallic Dyson rings emitting 'STAR FORGE' that opens the Glass Dyson Creation Overlay on tap; staged every playfield object (Nexus Station, Rifts, Space Obstacles, Barriers, Ships, Weapons, Flux Core) plus all 15 Supply Crate buffs as individually clickable 1:1 specimens with live glass inspectors, while isolating [120HZ TEST FLIGHT] onto a dedicated sandbox engine so ranked live state is never mutated.",
          "Smooth Circular 2.5D Metallic-Collared Rifts & Unified Nexus Station Naming (RiftSpriteBaker, RiftArenaView, CodexModal, RiftNexusKnowledge): Re-engineered Gravity, Repulsor, Phase, and Unstable Rift rendering into smooth circular 2.5D spatial wells framed by high-definition 5-stop metallic collars, specular arcs, and concentric plasma depth lenses, and unified all Safe-Zone central station naming and Codex/AI references to Nexus Station.",
          "Per-Ship & Overall Solar-Gold Resonance, Distinct Hangar Borders, 'Rift In' & <=15s Resonance Matchmaking (LocalPilotPrefs, RiftGameEngine, RiftArenaView, HangarModal, NexusAiConsoleModal, RiftHudOverlay, MainActivity): Replaced playfield/HUD bounty telemetry with Solar-Gold Resonance backed by persisted per-ship Resonance (Hulls 1-10) earned from combat performance, Kills/Assists, and objective captures; removed legacy hull crown text in Hangar Deck in favor of per-ship Solar-Gold Resonance badges and visually distinct Best Ship (Solar-Gold Harmonic Crown double-bezel) vs Selected/Locked (Cyan NEXUS Conduit) borders; updated the Hub deployment CTA to 'Rift In'; and added Resonance-based team matchmaking capped at a hard 15.0s maximum queue (widening to mismatched opponents after 7.5s or cancelling cleanly with 'No other available teams' at 15.0s).",
          "Dynamic Multi-Resolution & Aspect-Ratio Scaling (MainActivity, AuthScreen, NexusAiConsoleModal, RiftHudOverlay): Upgraded DeviceAwareModalAutoScaler and responsive cockpit/Auth layouts to adapt cleanly across ultra-wide phones, standard handsets, foldables, and tablets while bypassing scaling for the 1:1 native-pixel Star Forge stage.",
        ),
    ),
    RiftVersionLogEntry(
      buildNumber = 82,
      versionTag = "v1.0.82 // BUILD #82",
      releaseDate = "Nov 19, 2026",
      headline = "NEXUS Hub 2-Line Module Cards, Codex Sub-Deck in Helm Systems, Enlarged Clickable Top Bar, Unified Syndicate Terminology & Active-Match-Safe How-To-Play Tutorial Routing",
      badgeLabel = "HUB & CODEX",
      accentColorHex = "#00F5D4",
      highlights =
        listOf(
          "Codex Sub-Deck in Helm Systems & Streamlined 2-Line Hub Module Cards (SettingsModal, NexusAiConsoleModal, MainActivity): Added a dedicated [CODEX] sub-deck tab inside HELM SYSTEMS rendering CodexModal and routed all Codex launches to HELM SYSTEMS; renamed the bottom-right Hub card to PILOT DOSSIER (opening straight into Pilot Dossier), renamed the middle-right card to two-line SYNDICATE / STRIKE WING, and removed the third secondaryDetail line across all 6 SciFiTabletModuleCard modules so each card showcases only its metallic emblem, bold title, and primary live telemetry.",
          "Unified Syndicate & Strike Wing Terminology (NexusAiConsoleModal): Purged all legacy group wording across NEXUS Hub cards, Syndicate Wars mode descriptions, and Syndicate server channel badges in favor of Syndicate and Strike Wing terminology.",
          "Enlarged Clickable NEXUS Top Bar & Safe [? HOW TO PLAY] Practice Rift Routing (NexusAiConsoleModal, MainActivity): Enlarged top-bar telemetry typography and wired clickable quick-navigation segments (Callsign -> Pilot Dossier, Resonance -> Rift Conduit, Rift Shards -> Rift Conduit, SYN -> Syndicate Citadel Syndicates, WING -> Syndicate Citadel Strike Wing, ONLINE -> Subspace Comms); routed [? HOW TO PLAY] directly into the 8-step in-arena Practice Rift tutorial with active-match suspension/restoration and explicit LEAVE MATCH warp-off confirmation so active matches are never falsely terminated.",
        ),
    ),
    RiftVersionLogEntry(
      buildNumber = 81,
      versionTag = "v1.0.81 // BUILD #81",
      releaseDate = "Nov 19, 2026",
      headline = "Instant SurfaceSyncGroup Startup & Landscape Window Sync, Off-Main-Thread Sprite Prewarming & Asynchronous SFX WAV Synthesis",
      badgeLabel = "STARTUP SYNC",
      accentColorHex = "#00F5D4",
      highlights =
        listOf(
          "Zero-Stall SurfaceSyncGroup First-Frame Commit & Landscape Window Sync (MainActivity, AndroidManifest, themes.xml, colors.xml): Eliminated the 1,000ms SurfaceSyncGroup startup timeout (wmsSync-VRI[MainActivity] & VRI[MainActivity]) by configuring an immediate #060D1A window background, invoking enableEdgeToEdge() before super.onCreate, avoiding redundant mid-launch orientation/inset transactions, and committing lightweight initial Choreographer frames before mounting heavy cockpit surfaces.",
          "Off-Main-Thread Procedural Sprite & Emblem Prewarming (RiftSpriteBaker, MainActivity): Added thread-safe cache synchronization and prewarmStartupAssets(...) executed on Dispatchers.Default right after Frame #1 commits—pre-baking all ship hulls, spacetime-tear Rifts, DS9 Safe-Zone station, 16 224x224 Space Obstacle variants, and all 6 192x192 NEXUS Hub metallic module emblems off the UI thread.",
          "Asynchronous Procedural SFX WAV Synthesis & SoundPool Loading (RiftSoundSynthesizer): Offloaded all 12 procedural combat WAV file syntheses, cache writes, and SoundPool.load calls onto a dedicated background daemon executor with synchronized lifecycle release safety, while preserving immediate background/minimized audio silence.",
        ),
    ),
    RiftVersionLogEntry(
      buildNumber = 80,
      versionTag = "v1.0.80 // BUILD #80",
      releaseDate = "Nov 19, 2026",
      headline = "Spacetime-Tear Playfield & Unstable Rifts, Any-Rift Insta-Kill Convergence, Supply Pod Hazard Clearance, Station Counter-Attack Attribution, Disruptor Needler & Full Zero-Flash Purge",
      badgeLabel = "SPACETIME RIFTS",
      accentColorHex = "#00F5D4",
      highlights =
        listOf(
          "Cohesive Spacetime-Tear Rifts & Zero Solid-White Hit Sprites (RiftSpriteBaker, CodexModal): Upgraded all Playfield Rifts and Unstable Merged Rifts into volatile spacetime tears featuring dark abyssal storm interiors, multi-layered plasma storm turbulence matching the outer storm boundary, jagged angular fracture rims, branching rim lightning arcs, calm Unstable Rift spin speed, and re-baked riftAnomalyIconBitmaps—while removing flashShipBitmaps, customShip9FlashBitmap, and bakeSolidWhiteFlashBitmap so ships never flash solid white.",
          "Any-Rift Convergence Into 10.0s Insta-Kill Unstable Rift + Resonance Loss (RiftGameEngine, ShipSpecs, CodexModal): Enabled ANY two overlapping active Playfield Rifts (including Paired Phase Rifts) to fuse into a calm-spinning 10.0s Unstable Rift broadcasting 'UNSTABLE RIFT CONVERGENCE // RETURN TO SAFE ZONE'; at 0.0s detonation, all ships outside the Safe Zone are insta-killed and lose -25 Live Resonance (floored at 0).",
          "Supply Pod Hazard Clearance & Station Counter-Attack Trigger Credit/Penalty (RiftGameEngine): Guaranteed Supply Crates never spawn overlapping obstacles, placed Stasis Barriers, or active Playfield Rifts; tracked triggeredByShipId on all station retaliatory, cascade, and overload defense bolts—awarding 10% of normal kill Bounty & Live Resonance when a station counter-attack you triggered eliminates an enemy, and applying a Bounty & Live Resonance penalty if you die to a station attack triggered by your own fire.",
          "Disruptor Needler Naming & Unstable Rift Codex Sync (ShipSpecs, CodexModal): Standardized Disruptor's primary weapon title to 'Needler' across ship specs and custom Frame hardpoints, and synchronized all Unstable Rift codex copy with zero legacy blast terminology.",
        ),
    ),
    RiftVersionLogEntry(
      buildNumber = 79,
      versionTag = "v1.0.79 // BUILD #79",
      releaseDate = "Nov 19, 2026",
      headline = "In-Arena First-Time Sworn & Drifter Practice Rift Tutorial With Manual Skip/Next, Jumping Golden Rift Outline Highlighter & Stationary Top-Left Add Bot",
      badgeLabel = "PRACTICE BRIEFING",
      accentColorHex = "#00F5D4",
      highlights =
        listOf(
          "Direct-to-Practice-Rift Entry for Every [RFT] Drifter & First-Time Sworn Pilot (MainActivity, LocalPilotPrefs, RiftGameEngine): Bypassed NEXUS on entry for every [RFT] Drifter session and first-time Sworn Pilot commission, zoning directly into a zero-bot, zero-remote solo Practice Rift with zero career/Resonance/Bounty/Directive mutations and zero Safe-Zone eviction or Anti-AFK Flux drain.",
          "Manual-Only 8-Step In-Arena Practice Rift Briefing (RiftGameEngine, RiftHudOverlay): Retired the legacy practice_flight_checklist strip and added a crisp #060D1A Sci-Fi Tablet tutorial panel featuring strictly manual [SKIP] and [NEXT] controls walking through Left Stick Drift & Boost, Right Stick & Shared Flux Ring, Supply Pod Buff Crates, Destructible Obstacles & Stations, Active Playfield Rifts, Center Safe-Zone Harbor, Free Flight Practice, and AI Combat Sparring / NEXUS Exit.",
          "Jumping Golden Rift-Styled Target Outline & Offscreen Directional Pointer (RiftArenaView, RiftHudOverlay): Added a rotating multi-arc Golden Rift outline (#FFD700 / #FACC15) with harmonic lock-in pulse and corner chevrons that jumps cleanly across Left Stick, Right Stick + Shared Flux Ring, nearest Supply Pod, nearest Obstacle, nearest Playfield Rift, Safe-Zone Harbor, and the top-left [+ ADD BOT] button—paired with a golden cockpit edge pointer whenever a world target is offscreen.",
          "Stationary Non-Jumping Top-Left [+ ADD BOT] Spawner (RiftHudOverlay, RiftGameEngine): Added a fixed top-left [+ ADD BOT] button inside Practice Rift that deploys live sparring AI bots on demand without mutating career stats or shifting layout coordinates.",
        ),
    ),
    RiftVersionLogEntry(
      buildNumber = 78,
      versionTag = "v1.0.78 // BUILD #78",
      releaseDate = "Nov 19, 2026",
      headline = "3 New & Stacked Buff Crates (Tractor Beam, Proximity-Lock Infinite Missile, Continuous Lance Beam), Mode-Specific AI Bot Brains, Deathmatch-Only Spectating & 1v1 Escalation With 10s Ready-Up",
      badgeLabel = "BUFFS & 1V1 DUEL",
      accentColorHex = "#38BDF8",
      highlights =
        listOf(
          "Smooth Beveled Buff Pods & Stacked Crate Scaling (RiftSpriteBaker, RiftArenaView, RiftGameEngine, ShipSpecs): Smoothed Supply Pod housings and buff badges with rounded bevels, calm radial powerup glows, and non-spiky iconography; stacking N Barrier crates deploys N distinct wall segments, N Nova crates unleash N staggered 360-degree Nova waves, and additional Lance stacks scale beam width and range.",
          "3 New Special Buff Crates (RiftGameEngine, RiftArenaView, RiftSpriteBaker, ShipSpecs): Added Toggleable Tractor Beam (gravimetric laser web slowing and pulling a caught hull toward your ship while draining Flux; tap again to shut off and clear), Proximity-Lock Infinite Missile (infinite duration and bounces with a visible influence radius that locks onto entering hulls and dynamically transfers lock when another ship enters), and Toggleable Continuous Lance Beam (drains Flux, stops on first ship or obstacle hit without penetrating, ticks damage every 0.5s, scales wider/longer with stacks, and clears on toggle off).",
          "Mode-Specific AI Bot Brains & Deathmatch-Only Spectating (RiftGameEngine): Implemented dedicated tactical AI brains for Sector Convergence (HVT and crate hunting with safe positioning), Flux Core (loose-core sprints, carrier intercepts, and carrier kiting/crate runs avoiding Safe Zone), Stasis Breach (imprisoned bots firing on the brig core to break out while free bots hunt/defend), Rift Control (pushing Alpha/Omega capture zones and contesting enemies), and Rift Skirmish / Syndicate Wars (squad coordination, low-Flux caution, and focus-fire), while restricting teammate spectating strictly to round-based Deathmatch.",
          "1v1 Deathmatch Escalation & 10-Second Between-Round Ready-Up (RiftGameEngine, RiftArenaView, RiftSoundSynthesizer, RiftHudOverlay): Narrowing a Deathmatch round to 1v1 triggers an unobtrusive Rift convergence alert, smoothly cross-blends the playfield into the bioluminescent abyssal-garden state, and slews music tempo up to 180 BPM; enforced a 10.0s Ready-Up countdown between rounds that smoothly restores visuals, tempo, and round staging.",
        ),
    ),
    RiftVersionLogEntry(
      buildNumber = 76,
      versionTag = "v1.0.76 // BUILD #76",
      releaseDate = "Nov 18, 2026",
      headline = "Live Online Player Visibility, Ship-Style Directional Obstacle Hit Ripples + Retaliatory & Cascading Station Counter-Attacks, and Vertical Animated Instrument Hub Cards",
      badgeLabel = "STATION SALVOS & HUB",
      accentColorHex = "#00F5D4",
      highlights =
        listOf(
          "Live Online Player Visibility & Instant Match Heartbeat Sync (MainActivity): Unified sectorPilotsState and allOnlineHumansState into a deduplicated live pilot stream fed into RiftGameEngine.updateRemotePilots across active matches and Hub Overwatch, paired with immediate syncNativeLiveState heartbeats upon Ranked deployment, Custom Rift join/create, NEXUS_LOBBY presence, and match exit.",
          "Retaliatory & Cascading Station Defense Counter-Attacks (RiftGameEngine): Added lastHitAngle and lastAttackerShipId on WallRect plus triggerStationAttackResponse(...) — firing a targeted 5-bolt retaliatory NOVA_BOLT defense salvo (-0.12..+0.12 rad spread, speedMult/damageMult scaled) on primary bullet, bomb/explosion AoE, Seeker Torpedo, and Swarm Drone impacts, chaining up to 2 neighboring stations within 1,400px for 3-bolt cascading defense bursts, and unleashing the 16-bolt 360° Overload Defense Nova every 10 hits.",
          "Ship-Style Directional Impact Ripples on Obstacles (RiftArenaView): Removed obstacleHitFlashPaint white-washing completely in drawSpaceObstacle3D and replaced it with expanding directional impact ripple arcs and radial armor sparks centered on the impact rim at lastHitAngle.",
          "Vertical Animated Instrument Readout Hub Cards (NexusAiConsoleModal): Restyled SciFiTabletModuleCard into a vertical stack featuring a top ~50% slow-moving animated instrument Canvas (16s orbital sweep arc, calibrated 24-tick ring, and subtle reticle pulse) framing the inset metallic RiftSpriteBaker emblem above full-width unclipped 2–3 word bold titles, status pills, and live pilot telemetry.",
        ),
    ),
    RiftVersionLogEntry(
      buildNumber = 75,
      versionTag = "v1.0.75 // BUILD #75",
      releaseDate = "Nov 18, 2026",
      headline = "RiftSpriteBaker Static Initialization Order Fix (AuthScreen & Startup Crash Resolution)",
      badgeLabel = "BAKER INIT FIX",
      accentColorHex = "#00F5D4",
      highlights =
        listOf(
          "Resolved a startup ExceptionInInitializerError in AuthSafeHarborRadarShowcase caused by Kotlin top-to-bottom object <clinit> ordering in RiftSpriteBaker, where buildRiftAnomalyIconMap() invoked draw3dPlayfieldRiftColors() before sharedRiftFillPaint, sharedRiftStrokePaint, sharedRiftRibbonPath, sharedRiftSpinePath, and sharedRiftArcRect were initialized.",
          "Hoisted all reusable Paint, Path, RectF, and sprite cache instances to the top of RiftSpriteBaker ahead of init {}, unified deterministic Bitmap baking inside init {}, and skipped redundant full-sprite re-bakes in RiftVisualForgeState when all recipes are already at factory default.",
          "Hardened RiftSoundSynthesizer to automatically release superseded synthesizer instances and pace non-blocking AudioTrack sinks, verified across ExampleRobolectricTest and the full 14-test RiftAuditConsistencyTest suite.",
        ),
    ),
    RiftVersionLogEntry(
      buildNumber = 74,
      versionTag = "v1.0.74 // BUILD #74",
      releaseDate = "Nov 18, 2026",
      headline = "Inner Sub-Modal #060D1A Sci-Fi Tablet Restyle, Enlarged Readable Typography (9.5sp–14sp) & Unified Custom Resonance/Shard/Flux Iconography Across Hangar, Conduit, Citadel, Dossier & Helm Systems",
      badgeLabel = "TABLET MODALS",
      accentColorHex = "#00F5D4",
      highlights =
        listOf(
          "Unified all inner sub-modals (HangarModal, RiftShardsStoreModal, SyndicatesModal + ArmadaFleetModal, DossierModal, and SettingsModal) into the #060D1A / #081224 Sci-Fi Command Tablet shell with 1.dp #38BDF8 / #00F5D4 / #FACC15 borders, retiring legacy brown #1A1405 header/vault pills and semi-transparent #99111C2D containers.",
          "Enlarged sub-modal typography from tiny 7.sp–8.8.sp micro-labels up to crisp, high-contrast 9.5.sp–14.sp hierarchy across HANGAR DECK ship cards & 100-Pt Rift Frame builder, RIFT CONDUIT Daily Directives & Cosmetic cards, SYNDICATE CITADEL (36.dp–44.dp forged Crests & 1–5 Strike Wing rows), PILOT DOSSIER Hall of Fame & Executioner telemetry, and HELM SYSTEMS audio/display/gamepad presets.",
          "Standardized custom ResonanceCrestIcon, RiftShardGemIcon, and FluxEmblem readouts across all 5 sub-modals — eliminating plain RES badges, decorative unicode stars/arrows, and duplicate diamond glyphs beside prismatic Rift Shard gems while preserving 100% of Composable signatures and testTags.",
        ),
    ),
    RiftVersionLogEntry(
      buildNumber = 73,
      versionTag = "v1.0.73 // BUILD #73",
      releaseDate = "Nov 18, 2026",
      headline = "Live Playfield Visual Overhaul: 224x224 5-Stop Metallic & Basalt Obstacles in RiftArenaView, App-Icon Ribbon Playfield Rifts, Contained 3D Supply Pods & Multi-Plane Mode-Tinted Deep-Space Parallax",
      badgeLabel = "PLAYFIELD 3D",
      accentColorHex = "#00F5D4",
      highlights =
        listOf(
          "Replaced raw rectangular box obstacles in RiftArenaView.drawSpaceObstacle3D with crisp 224x224 5-stop metallic & basalt sprites (RiftSpriteBaker.getSpaceObstacleBitmap across 4 seed variants) for Concentric Twin-Ring Orbital Stations, Craggy Basalt Asteroid Hangar Outposts, Rift-Veined Basalt Asteroids, and Sheared Starship Hull Debris — complete with 2.5D camera-offset drop shadows, specular hit-flash pulses, dynamic point-light rim arcs, circular Flux integrity rings, and 10-segment Station Defense Nova gauges.",
          "Upgraded draw3dPlayfieldRiftColors to the official NEXUS App-Icon Ribbon Vortex aesthetic: 5 wide curved spiral plasma ribbons separated by #02040A event-horizon void lanes inside a beveled 16-tick metallic containment collar, synced 1:1 across the live arena, Hub Overwatch, Codex, and STAR FORGE.",
          "Replaced oversized fuzzy green Supply Crate glow blobs with rotating 3D hexagonal Gunmetal/Emerald Containment Supply Pods (supplyPodFrameBitmap wired 1:1 into STAR FORGE), upgraded barrier endcap pylons to 5-stop beveled hexagonal emitters, and added mode-tinted volumetric nebulae + distant Orbital Ring Megastructure & 28 Deep-Parallax Micro-Asteroid planes in RiftArenaView.",
        ),
    ),
    RiftVersionLogEntry(
      buildNumber = 72,
      versionTag = "v1.0.72 // BUILD #72",
      releaseDate = "Nov 18, 2026",
      headline = "Gameplay-Honest Sci-Fi Tablet AuthScreen, Codex Field Manual & #nexus-ai Ground-Truth Sync (5x DS9 Station, 440px Gravity Rift Implosion & Pinch-to-Zoom Overwatch) + Play Console privacy.html Sync",
      badgeLabel = "TABLET AUTH & CODEX",
      accentColorHex = "#00F5D4",
      highlights =
        listOf(
          "Rebuilt AuthScreen in the unified #060D1A Sci-Fi Command Tablet aesthetic (retiring the legacy NexusSpaceGlView 3D splash view in favor of a live circular Safe-Harbor radar schematic showcasing real 128x128 ship sprites + the 5x DS9 Safe-Zone Station sprite, animated 3-Tier Winged Vortex Crest, enlarged typography, and zero emojis).",
          "Synchronized ShipSpecs (RIFT_ANOMALIES_CODEX), CodexModal, RiftNexusKnowledge, and NexusAiViewModel (#nexus-ai) with exact current physics & Hub architecture: 5x (180px) DS9 Safe-Zone Station with Ops Holo-Ring in STAR FORGE, 440px Gravity Rift swept weapon growth (0.92x disc, +18px/+40px/+14px) + >= 260px 950/s Asteroid/Station Tidal Implosion + 360° Overload Nova, and the 6-card NEXUS Tablet Hub with 1.0x..6.0x Pinch-to-Zoom Highest-Resonance Overwatch.",
          "Updated privacy.html to reflect NEXUS (Threshold Dynamics Studio, LLC), Play Console package com.aistudio.riftarena.vxpqzm, Google Sworn Pilot cloud sync vs. strictly local-only [RFT] Drifter sessions, and UGC reporting/blocking controls.",
        ),
    ),
    RiftVersionLogEntry(
      buildNumber = 71,
      versionTag = "v1.0.71 // BUILD #71",
      releaseDate = "Nov 18, 2026",
      headline = "6 Large NEXUS-Station Metallic Hub Module Graphics, Bold 2–3 Word Card Titles + Live Telemetry, Enlarged Hub Typography, Clean 1.0x Overwatch Labels & Firestore liveResonance Sync",
      badgeLabel = "METALLIC CARDS",
      accentColorHex = "#00F5D4",
      highlights =
        listOf(
          "Upgraded all 6 NEXUS Sci-Fi Tablet cards with large 192x192 5-stop metallic NEXUS-station-styled graphics (bakeNexusHubModuleEmblem in RiftSpriteBaker), bold 2–3 word titles (SHIPS & BUILDS, RANKS & QUESTS, SKINS & AUDIO, CHAT & NEXUS AI, CLANS & SQUADS, STATS & CODEX), enlarged typography across the Top Bar / Cards / Overwatch pills, and live per-card pilot telemetry.",
          "Decluttered 1.0x Hub Overwatch by hiding world text/nametags below 1.55x zoom (fading them in smoothly as you pinch-zoom closer), and whitelisted liveResonance, RIFT_CONTROL, NEXUS_LOBBY, and CUSTOM_* across RiftRepository and firestore.rules.",
        ),
    ),
    RiftVersionLogEntry(
      buildNumber = 70,
      versionTag = "v1.0.70 // BUILD #70",
      releaseDate = "Nov 18, 2026",
      headline = "Strict Minimized/Background Audio & Render Silence + Interactive Pinch-to-Zoom & Pan Round Overwatch Aperture",
      badgeLabel = "AUDIO & ZOOM",
      accentColorHex = "#00F5D4",
      highlights =
        listOf(
          "Added strict Activity & window lifecycle audio/render suspension (MainActivity onResume/onPause/onStop + Compose LifecycleEventObserver + RiftSoundSynthesizer.setAppForeground + RiftArenaView.onWindowVisibilityChanged) so AudioTrack procedural battle music and SoundPool SFX pause, flush, and stay 100% silent whenever the app is minimized, backgrounded, or screen-locked, resuming cleanly on return.",
          "Added smooth 2-finger pinch-to-zoom (1.0x .. 6.0x anchored to pinch centroid), 1-finger drag panning (clamped within arenaRadius), circular canvas clipping, and double-tap / [1.0x] reset to the NEXUS Hub's Round Overwatch Aperture, plus quieted background FLUX_CORE bot pickup toasts during unjoined Hub Overwatch.",
        ),
    ),
    RiftVersionLogEntry(
      buildNumber = 69,
      versionTag = "v1.0.69 // BUILD #69",
      releaseDate = "Nov 17, 2026",
      headline = "Round Rippling Dynamic-Lit Digitization Into Full-Playfield Highest-Resonance Live Overwatch (RIFT # + Favorite Quick Zone-In) & 5x DS9 Metallic Safe-Zone Station in STAR FORGE",
      badgeLabel = "HUB OVERWATCH",
      accentColorHex = "#00F5D4",
      highlights =
        listOf(
          "Upgraded the Safe-Zone center (SECTOR CONVERGENCE & FLUX CORE) to a 5x ship-scale (180px world radius) Deep Space Nine (DS9) metallic station sprite built with 5-stop ship armor shading, 6 curved docking pylons, inner promenade windows, and a shrunk revolving cyan/gold holographic ring around Ops, wired live into STAR FORGE.",
          "Replaced the static ORBITAL_HUB 3D station scene with a Round Rippling Dynamic-Lit Digitized Aperture looking straight down at full-playfield scale into a live active match (RiftArenaView Orbital Overwatch), auto-selecting the pilot's [FAV] Rift or the active game with the Highest Combined Pilot Resonance (Game Type • RIFT #XX • Combined RES), with < / > instance switching and 1-tap [QUICK ZONE IN].",
          "Added liveResonance telemetry to LivePilotState cloud heartbeats and persistent favorite_overwatch_rift_key storage in LocalPilotPrefs, while pausing Hub Overwatch simulation and rendering cleanly whenever sub-modals are open or the pilot deploys into combat.",
        ),
    ),
    RiftVersionLogEntry(
      buildNumber = 68,
      versionTag = "v1.0.68 // BUILD #68",
      releaseDate = "Nov 17, 2026",
      headline = "Sci-Fi Tablet Terminal Digitizing Inward Into Abyssal-Garden 3D Safe-Zone Station Holding Back Outer Storm Space + Persistent Gravity Rift Mass Fix",
      badgeLabel = "TABLET SHELL",
      accentColorHex = "#38BDF8",
      highlights =
        listOf(
          "Rebuilt the NEXUS opening screen as a unified #060D1A Sci-Fi Command Tablet whose center viewport digitizes inward (dissolving holographic grid cells, voxel matrix tiles, and stepped depth telemetry) directly into the 3D Safe-Zone Sanctuary.",
          "Replaced the tie-dye center vortex in NexusSpaceGlView (GLES30 VSYNC) with an abyssal-garden 3D Safe-Zone Sanctuary: calm volumetric #020814 abyssal space with angled light shafts and caustic shimmer inside, anchored by the 3D Multi-Ring NEXUS Orbital Station projecting 8 golden-cyan 3D stabilizer beams + 8 outward-bracing emitter chevrons to hold a 3D Containment Shield Ring open against dark encroaching outer storm clouds, with real 128x128 Playfield ships patrolling and an unobstructed lower-center [ PLAY // ENTER THE RIFT ] trigger.",
          "Fixed the frameTick calibration sync bug in MainActivity.kt and RiftCalibrationState.kt that was resetting grown Gravity Rifts and Unstable Rifts back to 180px every 0.35s.",
        ),
    ),
    RiftVersionLogEntry(
      buildNumber = 67,
      versionTag = "v1.0.67 // BUILD #67",
      releaseDate = "Nov 17, 2026",
      headline = "Google Play Console Release Version Sync (versionCode 67 / v1.0.67)",
      badgeLabel = "PLAY CONSOLE SYNC",
      accentColorHex = "#38BDF8",
      highlights =
        listOf(
          "Incremented Android release versionCode to 67 (v1.0.67) in 1:1 sync with CURRENT_RIFT_BUILD_VERSION for Google Play Console publishing while preserving com.aistudio.riftarena.vxpqzm and all BUILD #66 Breached-Terminal NEXUS, 3-Tier Winged Vortex Crest, Safe-Harbor 3D Station, and Gravity Rift systems intact.",
        ),
    ),
    RiftVersionLogEntry(
      buildNumber = 66,
      versionTag = "v1.0.66 // BUILD #66",
      releaseDate = "Nov 17, 2026",
      headline = "Breached-Terminal NEXUS Hub + Giant Center PLAY Rift, 3-Tier Winged Vortex Crest, [? HOW TO PLAY] Schematic, Safe-Harbor 3D Station, Seamless Storm Wall & Gravity Rift Implosion",
      badgeLabel = "BREACHED NEXUS",
      accentColorHex = "#38BDF8",
      highlights =
        listOf(
          "Rebuilt the NEXUS main screen as a full-screen #060D1A Tactical Cockpit Terminal physically punched open at center (~52% width) by a colossal VSYNC-smooth GLES30 Rift Singularity with real 128x128 Playfield ships banking into the breach, bold PLAY // ENTER THE RIFT callout, and plain-English Left/Right Tactical Terminal Columns.",
          "Added the official 3-Tier Gunmetal & Solar-Gold Winged Vortex Crest across ic_launcher_foreground.xml (108dp vector adaptive icon inside the 66dp safe zone) and live-spinning NexusMergedBrandEmblem, paired with crisp ResonanceCrestIcon and RiftShardGemIcon header readouts.",
          "Added the zero-cartoon 3-Panel Animated Vector [? HOW TO PLAY] Cockpit Schematic (1. LEFT STICK: DRIFT & BOOST, 2. RIGHT STICK: SHARED FLUX, 3. RIFTS & SAFE HARBOR in <= 6 words per caption, auto-shown once for first-time pilots) plus a dismissible live 3-step Flight Checklist inside PRACTICE RIFT.",
          "Reforged the Safe Zone (active strictly in SECTOR CONVERGENCE & FLUX CORE) into a calm sanctuary anchored by the multi-ring 3D NEXUS Orbital Station projecting 8 golden-cyan stabilizer beams to brace the outer shield rim against inward-pressing volatile Rift tendrils, and eliminated the West ±π seam in STORM_BOUNDARY_AGSL using seamless 2D world-space direction vectors.",
          "Upgraded Gravity Rifts to absorb weapons across 0.92x visible physicalRadius via swept segment checks (+18px bullet, +40px bomb/torpedo, +14px drone/phantom up to 440px with violet accretion pulses & crackling rim lightning), and enabled swollen (>= 260px) Gravity Rifts to drag Asteroids, Orbital/Asteroid Stations, Debris, and Stasis Barriers straight into the core with zero minStop brake—dealing 950/s tidal crush damage on circular integrity rings until implosion (+ Supply Crate, 20s respawn, and 360° overload Station Defense Nova).",
          "Standardized explicit precision highp float and precision highp int qualifiers across all OpenGL ES 3.0 vertex/fragment shaders and even-aligned 0.62x linear fboPlasma dimensions for 100% mobile GPU link reliability.",
        ),
    ),
    RiftVersionLogEntry(
      buildNumber = 65,
      versionTag = "v1.0.65 // BUILD #65",
      releaseDate = "Nov 17, 2026",
      headline = "Choreographer Hardware VSYNC & Paused Background Arena, 3D Station Holographic Text Ring, Inward-Sucking Feathered Rift Throats, Minimal Top Bar & Ephemeral Solo PRACTICE RIFT",
      badgeLabel = "VSYNC & PRACTICE",
      accentColorHex = "#38BDF8",
      highlights =
        listOf(
          "Replaced manual render-thread sleep timing in NexusSpaceGlView with Choreographer hardware VSYNC pacing (eglSwapInterval(1) + EMA dt smoothing), split-res 0.62x linear plasma FBO upsampled with full 1.0x crisp starlight/station/ships, projection-dirty Compose gating, and 100% paused background RiftArenaView while NEXUS is open.",
          "Added a 3D Station-Projected Revolving Cylindrical Holographic Text Ring (\"NEXUS // SUBSPACE COMMS & AI • [N] ONLINE\") rendered on a 64-segment 3D cylinder with 2-pass GL_CULL_FACE hardware depth occlusion passing physically in front of and behind the command spire, leaving the center station uncovered by 2D text.",
          "Reforged all 7 surrounding Rifts into inward-sucking gravitational vortex throats that pull surrounding Ring-Space plasma spirals into feathered 0.42..0.88 polar FBM event horizons dissolving into pure #000000 button-text cores with zero sharp circular sticker edges.",
          "Streamlined the NEXUS top horizon bar (SYN / WING / ONLINE telemetry on the left; PRACTICE RIFT > on the right) and introduced the on-demand ephemeral solo PRACTICE RIFT flight sandbox (0 AI bots, 0 remote players, 0 career/Resonance/Bounty/Directive stat mutations, disabled Anti-AFK drain, 1-tap 1-10 HUD hull hot-swap with Flux refill, and instant teardown upon returning to NEXUS).",
        ),
    ),
    RiftVersionLogEntry(
      buildNumber = 64,
      versionTag = "v1.0.64 // BUILD #64",
      releaseDate = "Nov 17, 2026",
      headline = "Haze-Free High-Contrast 3D NEXUS Station, Full-Screen Ring-Space Rift Tapestry with Subtle Starlight & Zero Rift Bobbing",
      badgeLabel = "RING SPACE 3D",
      accentColorHex = "#38BDF8",
      highlights =
        listOf(
          "Removed all gamma washout, quarter-res bloom fog, and UI gradient veils over the central 3D NEXUS Orbital Space Station and live Playfield ship traffic, upgrading to half-res high-threshold (>1.05) HDR bloom and crisp Blinn-Phong metallic contrast.",
          "Extended swirling logarithmic Rift spiral arms and domain-warped Ring-Space FBM plasma currents edge-to-edge across 100% of the background so all 7 Rifts interweave across the heart of the NEXUS with subtle multi-depth starlight shining through.",
          "Eliminated vertical Rift bobbing and autonomous camera wobble so all 7 round 3D Rifts and the central 3D Station hold rock-steady coordinates while preserving 1-finger 3D look-around parallax.",
        ),
    ),
    RiftVersionLogEntry(
      buildNumber = 63,
      versionTag = "v1.0.63 // BUILD #63",
      releaseDate = "Nov 17, 2026",
      headline = "3D Jita-Style Central NEXUS Space Station + Live Real-Ship Traffic, Deep #010308 Dark Space, 7 Round Outward-Bleeding 3D Rifts, Merged NEXUS Brand Emblem, Custom Resonance & Shard Icons & Cloud Identity Sync",
      badgeLabel = "3D STATION & ICONS",
      accentColorHex = "#00F5D4",
      highlights =
        listOf(
          "Upgraded NexusSpaceGlView (OpenGL ES 3.0 + hardware depth buffer) to deep #010308 Abyssal Dark Space anchored by a central 3D rotating Jita-style multi-ring NEXUS Orbital Space Station (SUBSPACE COMMS & #nexus-ai) and live 3D-projected real Playfield ship traffic (max(5, onlineHumanPilots.size)) banking along undocking/docking corridors and warping through surrounding Rifts.",
          "Reforged the 7 surrounding module portals (CONVERGENCE GATE through HELM SYSTEMS) into round (~1:1) Playfield-spirit 3D Singularities that bleed logarithmic swirling plasma arms outward into dark space around pure #000000 event-horizon button-text cores with >85% reduced pulse.",
          "Unified the Tri-Concept NEXUS Brand Emblem (Winged-N + 3D Ring-Space Gyro + Swirling Singularity) across AuthScreen, NEXUS, HUD, and Changelog; added the custom Solar-Gold & Cyan Harmonic Winged Tuning Crest Resonance icon and single jewel-grade 3D Faceted Prismatic Gem Rift Shard icon (eliminating plain RES and duplicate ❖ text); enforced Always-NEXUS boot; and fixed Sworn Pilot Firestore callsign/tag sync so returning Google pilots never see a false callsign modal or [RFT]/[GUEST] tags.",
        ),
    ),
    RiftVersionLogEntry(
      buildNumber = 62,
      versionTag = "v1.0.62 // BUILD #62",
      releaseDate = "Nov 17, 2026",
      headline = "Native OpenGL ES 3.0 3D Volumetric NEXUS Space, Multi-Pass HDR Bloom & 8 Crackling Spacetime Rift Tears with Pure #000000 Cores",
      badgeLabel = "GLES30 PIPELINE",
      accentColorHex = "#00F5D4",
      highlights =
        listOf(
          "Built NexusSpaceGlView on native OpenGL ES 3.0 (TextureView + EGL14 + GLES30) rendering raymarched 3D FBM volumetric NEXUS space, 260 3D parallax GL_POINTS space motes, half-resolution bright-pass HDR Gaussian bloom, and ACES filmic tone-mapping.",
          "Removed all 2D circles, obstacle sprites, and mechanical Hub station graphics in favor of 8 crackling jagged 3D Spacetime Rift Tears ripped directly into volumetric space (central primary multicolored NEXUS Tear + 7 surrounding module Tears) with gravitational ray-pinch lensing and branching electric lightning.",
          "Enforced strict pure #000000 void interiors (alpha-masked against both 3D space motes and HDR bloom) holding crisp diegetic button text projected 1:1 in 3D perspective, with 1-finger 3D look-around sway, 3D camera dive transitions, and matching OpenGL ES 3.0 Splash & Rift Tear brand emblem on AuthScreen.",
        ),
    ),
    RiftVersionLogEntry(
      buildNumber = 61,
      versionTag = "v1.0.61 // BUILD #61",
      releaseDate = "Nov 16, 2026",
      headline = "GPU AGSL Rift-Cloud Storm Boundary & Proximity Lightning, Wall-less 3D Nexus Safe-Zone Rift, Unstable Rift & Swarm Shaders + Regression Suite",
      badgeLabel = "AGSL STORM & SAFE RIFT",
      accentColorHex = "#00F5D4",
      highlights =
        listOf(
          "Upgraded the outer circular arenaRadius perimeter with a native GPU AGSL RuntimeShader (and multi-octave procedural fallback) billowing 3D Rift Storm Cloud Wall that bleeds mist inward and strikes live branching plasma lightning arcs onto any ship within 220px.",
          "Rebuilt the central Safe Zone (active only in SECTOR CONVERGENCE and FLUX CORE) as a wall-less circular 3D Dimensional Rift Aperture looking down into the glowing rotating Nexus Hub Station with crisp zoom-compensated world overhead banners.",
          "Added multicolored 3D Unstable Rift convergence rendering + live countdown ring, 5-drone zig-zagging Swarm fuselage shaders, circular Asteroid Flux integrity rings, 10-segment Station Nova charge arcs, BARRIER xN stack callouts, #nexus-ai telemetry sync, and full Stage 5 regression unit tests.",
        ),
    ),
    RiftVersionLogEntry(
      buildNumber = 60,
      versionTag = "v1.0.60 // BUILD #60",
      releaseDate = "Nov 16, 2026",
      headline = "Wall-less Circular Safe-Zone Physics, Unstable Rift 10.0s Doomsday Merge, 5-Drone Swarm, Destructible Asteroids, 10-Hit Station Novas & Stacked Anti-Bleed Barriers",
      badgeLabel = "COMBAT & RIFTS",
      accentColorHex = "#FACC15",
      highlights =
        listOf(
          "Removed Safe-Zone box walls in favor of a wall-less circular Safe-Zone Rift active strictly in SECTOR CONVERGENCE and FLUX CORE (with Open-Space Anti-AFK Flux drain restricted to those two modes), and tied Seeker/Missile/Bomb/Swarm speeds and turn rates directly to Custom Game speedMult.",
          "Added live overlapping Rift fusion into a 10.0s Unstable Rift that detonates a 75% Max Flux Doomsday blast shattering nearby asteroids, Max-Size 420px Gravity Rift obstacle/barrier pull, Repulsor inner-core supersonic slingshot, and fast predictive Seeker lead-homing.",
          "Added the 5-Drone zig-zagging SWARM Special Buff (0 Flux), 2,400-Flux destructible Crystal Asteroids (dropping a Supply Crate + 20s respawn), 10-Hit Orbital/Asteroid Station 360° 16-bolt Defense Novas, N× Stasis Barrier stacking, and exact swept segment raycasting fixing point-blank barrier bleed.",
        ),
    ),
    RiftVersionLogEntry(
      buildNumber = 59,
      versionTag = "v1.0.59 // BUILD #59",
      releaseDate = "Nov 16, 2026",
      headline = "STAR FORGE Full-Screen 1:1 Playfield Studio, 95% Glass Tap-to-Edit AGSL Inspector & Independent Spectator HUD Wiring",
      badgeLabel = "STAR FORGE 1:1",
      accentColorHex = "#C084FC",
      highlights =
        listOf(
          "Rebuilt STAR FORGE's Playfield Studio into an edge-to-edge 1:1 Frozen Playfield Showcase with Scenic Camera Bookmarks, true world-scale 3D AGSL/FBM Safe Zone Nexus Aperture, Playfield Rift Storm Wall, Eclipse Stasis, obstacles, Rifts, ordnance, and live 120Hz Test Flight.",
          "Added a 95% Obsidian-Glass Tap-to-Edit Element Reforge Inspector across all 36 World & HUD targets with GPU AGSL/FBM shader turbulence, rim-light depth, detail density, palette swatches, and bound engine physics sliders.",
          "Separated HUD_SPECTATOR_BOX onto its own persisted spectatorBoxOffset independent of toastContainerOffset and wired forged VisualElementRecipes live across Top Bar, Spectator Switcher, Kill Feed, Buff Strip, Tactical Alert Banner, Retro-Brake, and Multi-Shot HUD controls.",
        ),
    ),
    RiftVersionLogEntry(
      buildNumber = 58,
      versionTag = "v1.0.58 // BUILD #58",
      releaseDate = "Nov 16, 2026",
      headline = "NEXUS Ring-Space 3D Shader Overhaul, Central Nexus Hub Station & 7 Living 3D Rifts",
      badgeLabel = "RING SPACE 3D",
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
            NexusMergedBrandEmblem(modifier = Modifier.size(30.dp))
            Column {
              Text(
                text = "RIFT // SECTOR TRANSMISSION & VERSION LOG",
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
