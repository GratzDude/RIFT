package com.example.engine

import com.example.data.LeaderboardEntry
import com.example.data.LocalPilotPrefs
import com.example.data.PeakBountyRunSnapshot
import com.example.data.SyndicateServiceEntry
import com.example.ui.CURRENT_RIFT_BUILD_VERSION
import java.util.Locale
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.roundToInt

/**
 * Dynamically compiles the complete, authoritative RIFT (Threshold Dynamics Studio, LLC) Engine,
 * 160-Slot 256x256 Normal-Mapped 2.5D Relighting & AGSL Volumetric Shader Architecture,
 * 960x540 Borderless 4-Shelf Holographic Rift Hub & Interactive Overwatch Workbenches,
 * 5x Rift Station Safe Harbor, 100-Point Ship Parity, Live-Resonance vs. Shard Store Economy,
 * Daily Directives, Supply Registry, 440px Gravity Rift Implosion, Unstable Rift Convergence,
 * Crystal Asteroids, Station Defense Novas, Helm Systems, Pilot Dossier, and Sworn Pilot Snapshot for `#rift-ai`.
 */
object RiftNexusKnowledge {

  const val NEXUS_SYSTEM_INSTRUCTION: String =
    """
You are RIFT AI, the tactical intelligence stationed inside `SUBSPACE COMMS` (`#rift-ai` channel) in RIFT (developed by Threshold Dynamics Studio, LLC), a mobile-first competitive 2.5D Newtonian space dogfighter on Android.

CRITICAL DOMAIN & KNOWLEDGE BOUNDARY (NON-NEGOTIABLE):
1. EXCLUSIVE RIFT SECTOR KNOWLEDGE BASE: Your ONLY knowledge base is RIFT itself:
   - Viewport & Borderless 4-Shelf Holographic `RIFT HUB`: `960x540` logical viewport lock (`min(widthPx / 960f, heightPx / 540f)`, `fontScale = 1f`) framing the central `1.0x..6.0x` circular pinch-to-zoom & pan `Overwatch` lens. Left Wing shelves: `HANGAR DECK` (all 10 numberless hulls open from 0 Resonance: Viper, Ricochet, Suppressor, Goliath, Twinfang, Disruptor, Valkyrie, Manta, Eclipse, and Ship #10 `Rift Frame` at full 100-Point parity; displays `CHARGED` vs `BEST` status, never `LOCKED`) + `RESONANCE` (front-and-center Live Resonance, Global Top / By Ship / Syndicates Overwatch Leaderboards, Conduit Rank Ladder & 3 Daily Directives paying up to 250 Rift Shards/day). Right Wing shelves: full-screen `SUBSPACE COMMS` (`#rift-ai`, `#shipwright-meta`, `#patch-log` + team voice) + `ACTIVE RIFTS` (6 Official Ranked Modes + Public Custom Rifts Browser, 6-char Join Codes, `2x3` base-mode chip grid `FFA`, `SKIRMISH`, `FLUX CORE`, `BREACH`, `CONTROL`, `SYN WARS`, and `5%`-stepped `0%..100%` rules).
   - Interactive Overwatch Workbenches & Corner Pods: Drag-and-Drop Overwatch `Rift Frame Drydock` (9 Primary + 23 Secondary pre-baked hardware icons, Primary & Secondary drop sockets, 5 hull allocation sliders `FLUX`, `REGEN`, `SPEED`, `THRUST`, `TURN` within exact 100-Point parity, `Frame A` through `Frame E` slots, and `CHARGE RIFT FRAME`), 270-degree circular Overwatch Slider & Dial Workbench (Helm Controls/Display/Audio & Custom Rift rules), Embedded 3-Track Procedural Music & SFX Synth Deck (Tracks A/B/C across 5 synth archetypes), `108dp`/`118dp` 10-Hull Overwatch Cosmetic Showcase, `Pilot Dossier` (Conduit Rank Tier `DRIFT INITIATE`, `VANGUARD`, `INTERCEPTOR`, `PHANTOM`, `RIFT COMMANDER`, `APEX ARCHON` with smooth tier progress bar, Live / Peak / Starting Match Resonance, 10-Ship Resonance Breakdown across all 10 numberless hulls from Viper through Rift Frame, Career & Ship Survival telemetry with `0%`-safe Accuracy when `0` shots are fired, 1st-free then `250`-Shard Callsign rename, and Resonance-only HTML, CSV, and JSON Telemetry Sheet exports; RIFT uses 100% Live Resonance and Daily Directives exclusively for progression with zero legacy numerical rank tracks or experience bars), `Shard Store`, human-founded `Syndicates` (Overwatch Crest Builder) & 5-Pilot `Squadron`, `Tactical Codex`, and full-screen `1:1 STAR FORGE` (`Playfield Studio` bypassing the 960x540 scaler for true 1:1 world inspection).
   - Strict Live-Resonance Ranked Economy vs. Shard Store: `Frame A` and default cosmetics (`id == 0`) are always active from 0 Resonance. `Frame B` (250 Rift Shards), `Ship Skins 1..5` (350..850 Rift Shards across all 10 hulls), and post-1st-free Callsign renames (250 Rift Shards) are permanent Rift Shard Store unlocks (`999,999` non-depleting tester Rift Shards while commercial Play Billing is dormant). `Frame C` (650), `Frame D` (1100), `Frame E` (1600), `Kill Finishers 1..4` (200/550/1050/1650), `Warp Signatures 1..3` (350/750/1300), and `Resonance Leader Halos 1..3` (500/1000/1750) are NEVER purchasable with Rift Shards -- they activate strictly while `liveResonance >= threshold` and non-destructively fall back to `Frame A` / default `0` if `liveResonance` drops.
   - 160-Slot `256x256` Normal-Mapped 2.5D Relighting & Multiplayer Cosmetic Sync: Pre-allocated 160-slot hardware `PBR_RELIGHT_AGSL` `RuntimeShader` pool (API 33+) with zero-allocation Canvas directional crescent fallback (API 24-32) relights all 10 `256x256` starfighters (Skins `0..5`), the `384x384` (`180px` world radius, `5x` ship-scale) `Rift Station`, `160x160` Supply Pods, and `224x224` Space Obstacles in real time from nearby Rifts, Rift Station, Flux Core, explosions, Kill Finishers, and airborne Goliath Siege Bombs while keeping weapon projectile graphics untouched and zero flat-white hit flashes. Live multiplayer syncs cosmetics via Firestore-safe `__C<skin><fin><warp><halo>` tags validated against remote Live Resonance alongside Resonance-gated AI bot cosmetics.
   - Volumetric Rifts, Obstacles & Complete Combat Mechanics: 100% soft volumetric storm-cloud Playfield Rifts centered at `(cx, cy)` with fractal branching perimeter lightning and zero rigid lines, spokes, or central balls (Crimson/Magenta Plasma Rift, pure Cobalt-to-Ice-Blue Repulsor Rift with supersonic inner-core slingshot, Indigo/Violet Gravity Rift with swept `0.92x` visible-disc weapon absorption up to `440px` max and `>= 260px` tidal crush at `950/s` dragging Crystal Asteroids, Stations, Debris, and Stasis Barriers to implosion + Supply Crate + `20.0s` respawn + 16-bolt 360-degree Station Overload Nova, Teal/Cyan Phase Rifts A & B, and Multi-Spectrum `10.0s` Unstable Rift convergence dealing full-sector insta-kill outside Safe Harbor), seamless FILL-quad Quintic FBM outer `STORM_BOUNDARY_AGSL` wall + `220px` proximity lightning, `2,400` Flux Crystal Asteroids, 10-hit Orbital/Asteroid Station 16-bolt Defense Nova (`+10%` trigger credit on enemy eliminations vs `-15%` self-backfire penalty), Goliath (`#4` / `SIEGE` Rift Frame) Secondary button switching live to solar-orange `DETONATE` (`0` Flux) while a Siege Bomb is airborne for instant finger-down mid-air detonation even during reload cooldown or at `0` remaining Flux, Glancing Wall 360-degree Barrel Roll (`0.48s`), post-life Ranked Accuracy Resonance callouts (`HULL LOST • +4 ACC` at `>= 45%` accuracy or `HULL LOST • -2 ACC` at `< 15%` accuracy with `8+` shots fired), Final 1v1 Overdrive (`180 BPM`), and `<= 15.0s` Resonance matchmaking.
2. REFUSE NON-RIFT TOPICS IN CHARACTER: If the user asks about anything outside RIFT, briefly decline in character: "RIFT AI TELEMETRY BOUNDARY // Query falls outside RIFT sector parameters. Ask about RIFT ships, 100-point Rift Frame builds, 2.5D relighting & physics stats, Rift Conduit Resonance, Playfield Rifts, 5x Rift Station, Rift Hub Overwatch, Squadrons, Ranked Modes, buffs, or Pilot Dossier records."
3. PINPOINT NUMERICAL & PIXEL PRECISION: Cite exact live engine numbers, world-pixel dimensions, DPS, Flux costs (HP + weapon energy, floored at 1 Flux on self-drain), Rift Shards vs. Live Resonance economy gates, Per-Ship, Live & Peak Resonance, custom frames (`Frame A-E`), Eclipse two-way `Stasis` (6.0s duration, 275px radius, 320 Flux cost, 8.5s CD), Gravity Rift 440px swept growth & >= 260px 950/s tidal implosion, Unstable Rift 10.0s convergence, 5x (180px) Rift Station, Ranked Mode & Custom Rifts telemetry, Calibration overrides, and Sworn Pilot Leaderboard records. Never use Unicode emojis or three-letter Resonance abbreviations; always write "Resonance", "Flux", and "Rift Shards" in full plain English.
4. ANDROID TOUCH HUD & TWIN-STICK CONTROLLER TERMINOLOGY ONLY: Support native Android Landscape Touch HUD (Left Movement Stick, Manual [BRAKE], Right Primary Fire Stick, Secondary, Special Buff, Repel, Native Multi-Shot, Collapsible Living-Teammate Spectator Switcher, Graphical Dual-Rift Tug-of-War Top Bar in Rift Control, and Bottom Console with 3D AGSL Plasma Flux Ring wrapping the 1:1 circular minimap plus Conduit Rank progress bar) AND Bluetooth/USB Twin-Stick Controllers (including LB/RB teammate spectator cycling). Never mention PC keyboard shortcuts.
5. PURE TEXT Q&A & THEORYCRAFTING: Deliver clear, structured plain-English tactical answers and 100-point Rift Frame theorycrafting breakdowns (all 10 ships and full 100-point parity open from 0 Resonance).
"""

  fun buildLiveSystemPrompt(
    engine: RiftGameEngine,
    localPrefs: LocalPilotPrefs,
    currentUserId: String,
    callsign: String,
    syndicateTag: String,
    syndicateName: String,
    cloudLeaderboard: List<LeaderboardEntry>,
    allTimePeakSnapshot: PeakBountyRunSnapshot?,
    pvpPeakSnapshot: PeakBountyRunSnapshot?,
    syndicateHistory: List<SyndicateServiceEntry>,
  ): String =
    compileLiveSnapshot(
      engine = engine,
      localPrefs = localPrefs,
      currentUserId = currentUserId,
      callsign = callsign,
      syndicateTag = syndicateTag,
      syndicateName = syndicateName,
      cloudLeaderboard = cloudLeaderboard,
      allTimePeakSnapshot = allTimePeakSnapshot,
      pvpPeakSnapshot = pvpPeakSnapshot,
      syndicateHistory = syndicateHistory,
    )

  fun compileLiveSnapshot(
    engine: RiftGameEngine,
    localPrefs: LocalPilotPrefs,
    currentUserId: String,
    callsign: String,
    syndicateTag: String,
    syndicateName: String,
    cloudLeaderboard: List<LeaderboardEntry>,
    allTimePeakSnapshot: PeakBountyRunSnapshot?,
    pvpPeakSnapshot: PeakBountyRunSnapshot?,
    syndicateHistory: List<SyndicateServiceEntry>,
  ): String {
    val sb = StringBuilder(19_000)
    val camScale = engine.effectiveCameraScale()
    val projScale = RiftCalibrationState.projectileScale
    val shipVisScale = RiftCalibrationState.shipScale
    val crateVisScale = RiftCalibrationState.crateScale
    val anomVisScale = RiftCalibrationState.anomalyScale
    val customSpdMult = engine.currentCustomGame?.speedMult ?: 1.0f
    val customDmgMult = engine.currentCustomGame?.damageMult ?: 1.0f

    sb.appendLine(
      "=== [LIVE RIFT GROUND-TRUTH TELEMETRY SNAPSHOT -- RIFT v1.0.$CURRENT_RIFT_BUILD_VERSION (BUILD #$CURRENT_RIFT_BUILD_VERSION)] ==="
    )

    sb.appendLine()
    sb.appendLine(
      "[1. 960X540 HOLOGRAPHIC RIFT HUB, INTERACTIVE OVERWATCH WORKBENCHES, 160-SLOT 2.5D RELIGHTING & WORLD SPECS]"
    )
    sb.appendLine(
      "- 960x540 Logical Viewport & Borderless 4-Shelf Holographic RIFT HUB: Locked to min(widthPx / 960f, heightPx / 540f) with fontScale = 1f (bypassed strictly inside full-screen 1:1 STAR FORGE). Left Wing shelves: HANGAR DECK (all 10 numberless hulls open at 0 Resonance including Ship #10 Rift Frame; displays CHARGED vs BEST ship status, never LOCKED) and RESONANCE (front-and-center Live Resonance, Global Top / By Ship / Syndicates Overwatch Leaderboards, Conduit Rank Ladder & 3 Daily Directives). Right Wing shelves: full-screen SUBSPACE COMMS (#rift-ai, #shipwright-meta, #patch-log + team voice) and ACTIVE RIFTS (6 Official Ranked Modes + Public Custom Rifts Browser, 6-char Join Codes, 2x3 base-mode chip grid FFA / SKIRMISH / FLUX CORE / BREACH / CONTROL / SYN WARS, and 5%-stepped 0%..100% Damage Taken & Weapon Flux Cost scales) -- framing the central 1.0x..6.0x circular Overwatch lens (sector switcher auto-hidden while workbenches are open; world nametags hidden below 1.55x zoom) + [RIFT IN]."
    )
    sb.appendLine(
      "- Interactive Overwatch Workbenches & Pods: 1) Drag-and-Drop Overwatch RIFT FRAME DRYDOCK (9 Primary + 23 Secondary pre-baked hardware icons, Primary & Secondary drop sockets, 5 hull sliders FLUX / REGEN / SPEED / THRUST / TURN within exact 100-Point parity, Frame A-E slots, and CHARGE RIFT FRAME; #shipwright-meta shared builds load straight into this Drydock); 2) 270-Degree Circular Overwatch Slider & Dial Workbench (Helm Controls/Display/Audio & Custom Rift tuning); 3) Embedded 3-Track Procedural Music & SFX Synth Deck (Tracks A/B/C across 5 synth archetypes); 4) 108dp/118dp 10-Hull Overwatch Cosmetic Showcase cycling all 10 ships across Skins 0..5; 5) Corner Pods: Callsign -> PILOT DOSSIER (Conduit Rank Tier DRIFT INITIATE through APEX ARCHON with progress bar, Live / Peak / Starting Match Resonance, 10-Ship Resonance Breakdown across all 10 numberless hulls Viper through Rift Frame, Career & Ship Survival telemetry with 0%-safe Accuracy when 0 shots are fired, 1st-free then 250-Shard Callsign rename, and Resonance-only HTML, CSV, and JSON Telemetry Sheet exports; RIFT uses 100% Live Resonance & Daily Directives exclusively with zero legacy rank tracks or experience bars), Resonance Crest -> RIFT CONDUIT, Shard Gem -> SHARD STORE (999,999 non-depleting tester Rift Shards while Play Billing is dormant, preserved across profile resets), Syndicate/Squadron -> human-founded SYNDICATES (Overwatch Crest Builder) & 5-Pilot SQUADRON, plus Tactical Codex & 1:1 STAR FORGE."
    )
    sb.appendLine(
      "- World Arena & Seamless FILL-Quad Quintic FBM Storm Wall: 8000 x 8000 world pixels (8x8 sectors A1..H8, each sector = 1000 x 1000 world px; circular perimeter radius = ${engine.arenaRadius.toInt()} world px). Outer boundary renders a seamless FILL-quad Quintic FBM GPU AGSL volumetric Storm Wall (STORM_BOUNDARY_AGSL) driven by monotonic elapsed seconds with organic sheet lightning and live 220px proximity branching arc lightning."
    )
    sb.appendLine(
      "- Wall-Less Circular Safe-Zone Harbor & 5x Rift Station: Safe-Zone radius = ${engine.getSafeZoneRadius().toInt()} world px centered at (${engine.arenaCenterX.toInt()}, ${engine.arenaCenterY.toInt()}), anchored by a 384x384 normal-mapped 5x ship-scale (180px world radius) Rift Station with a revolving cyan/gold Ops Holo-Ring (editable live in 1:1 STAR FORGE). Active ONLY in SECTOR CONVERGENCE and FLUX CORE (live hasActiveSafeZone() = ${engine.hasActiveSafeZone()}, 15s max stay / weapon-lockout); completely disabled in RIFT CONTROL, STASIS BREACH, RIFT SKIRMISH, and SYNDICATE WARS. Open-Space Anti-AFK Flux drain is likewise restricted strictly to SECTOR CONVERGENCE and FLUX CORE."
    )
    sb.appendLine(
      String.format(
        Locale.US,
        "- Camera Scale Formula: effectiveCameraScale = 0.65 (base) * cameraZoom (%.2fx) * cameraZoomScale (%.2fx) = %.4fx live (1 world px = %.4f screen px).",
        engine.cameraZoom,
        RiftCalibrationState.cameraZoomScale,
        camScale,
        camScale,
      )
    )
    sb.appendLine(
      String.format(
        Locale.US,
        "- 160-Slot 256x256 Normal-Mapped 2.5D PBR-Lite Relighting & Hitboxes: Offscreen pre-baked tangent-space normal + metallic specular maps across all 10 starfighter hulls (256x256 px, Skins 0..5), the Rift Station (384x384 px, 180px world radius), Supply Pods (160x160 px), and 16 Space Obstacle variants (224x224 px). Relit per frame via a 160-slot PBR_RELIGHT_AGSL RuntimeShader pool (API 33+) or zero-allocation Canvas directional crescent fallback (API 24-32) from nearby Rifts, Rift Station, Flux Core, explosions, Kill Finishers, and airborne Goliath Siege Bombs (weapon projectile graphics untouched; zero flat-white ship hit flashes). Ship physical collision radius = 36.0 world px (72.0 world px diameter); live world render quad = %.2f x %.2f world px (%.2f screen px at live zoom). Multiplayer syncs cosmetics via Firestore-safe __C<skin><fin><warp><halo> tags validated against remote Live Resonance.",
        88f * shipVisScale,
        88f * shipVisScale,
        88f * shipVisScale * camScale,
      )
    )
    sb.appendLine(
      String.format(
        Locale.US,
        "- Primary Laser / Bolt Exact Pixel Formula: Shaft Vector Length = primarySpeed * 0.036 world px; Outer Colored Glow Width = %.2f world px; Inner White Core Width = %.2f world px; swept segment-vs-AABB collision prevents point-blank wall bleed.",
        6.4f * projScale,
        2.8f * projScale,
      )
    )
    sb.appendLine(
      String.format(
        Locale.US,
        "- Secondary Warhead, Combat Maneuver & Ability Specs (scaled by projectileScale %.2fx, Custom speedMult %.2fx, Custom damageMult %.2fx): Viper Phase Dash = 135 world px spatial blink + %.0f px/s dodge burst (320 Flux cost, 0.65s CD, +%.0f%% next-shot rail overcharge); Goliath (#4 / SIEGE Rift Frame) Siege Bomb = %.1f world px radius whose Secondary button switches live to glowing solar-orange DETONATE (0 Flux) while airborne for instant finger-down mid-air detonation even during reload cooldown or at 0 remaining Flux; Bounce & Prox Flak Bomb = %.1f world px radius (Prox Flak fuse = %.0f world px); Disruptor Cloak & 360-degree EMP = 20 Flux entry, %.0f Flux/s cloak drain (50%% cloaked afterburner drain, 4.5s CD, 150 Flux safety floor), 0 Flux manual decloak %.0f world px EMP draining %.0f%% Max Flux (capped at 850 Flux) + %.1fs recharge freeze; Proximity Mine = %.1f world px radius (max %d mines, 30s life); Fast Predictive Seeker Torpedo = %.1f world px radius (%.0f px/s cruise * speedMult, proportional intercept lead, %.1fs flight); Stackable Stasis Barrier = %.0f world px Tethered Cyan Hard-Light Forcefield (%.1fs duration, deploys within 78px stack as BARRIER xN); SWARM = 5 autonomous zig-zagging hunter drones (210 Flux dmg * damageMult each, 560 px/s * speedMult, 6.5s life); Eclipse Stasis (INTERDICTOR_DOME) = %.0f world px radius (%.0f world px diameter) two-way Stasis active for %.1fs (320 Flux cost, %.1fs CD); Glancing Wall 360-Degree Barrel Roll = 0.48s evasive roll triggered on tangential wall/obstacle graze.",
        projScale,
        customSpdMult,
        customDmgMult,
        RiftCalibrationState.phaseDashImpulse,
        RiftCalibrationState.phaseOverchargeBonusPct * 100f,
        24f * projScale,
        17f * projScale,
        RiftCalibrationState.proxFuseRadius,
        RiftCalibrationState.cloakDrainPerSec,
        RiftCalibrationState.empPulseRadius,
        RiftCalibrationState.empFluxDrainPct * 100f,
        RiftCalibrationState.empFreezeSec,
        18f * projScale,
        RiftCalibrationState.maxMinesPerShip,
        19f * projScale,
        RiftCalibrationState.seekerSpeed,
        RiftCalibrationState.seekerFlightTime,
        RiftCalibrationState.stasisBarrierLength,
        RiftCalibrationState.stasisBarrierDuration,
        RiftCalibrationState.interdictorBubbleRadius,
        RiftCalibrationState.interdictorBubbleRadius * 2f,
        RiftCalibrationState.interdictorBubbleDuration,
        RiftCalibrationState.interdictorBubbleCooldown,
      )
    )
    sb.appendLine(
      String.format(
        Locale.US,
        "- Destructible Crystal Asteroids & 10-Hit Station Defense Novas: Crystal Asteroids have 2,400 Flux integrity with an on-rock circular integrity ring; shattering one drops a guaranteed Supply Crate and schedules a 20.0s respawn (%d pending respawns). Orbital & Asteroid Stations track weapon impacts with a 10-segment charge ring and unleash a 16-bolt 360-degree Station Defense Nova every 10 hits (+10%% Resonance credit to the triggering pilot on enemy eliminations, -15%% Resonance self-backfire penalty if killed by your own triggered Nova; also detonates as a 16-bolt Station Overload Nova when imploded by a >= 260px Gravity Rift). Supply Crate world size = %.2f world px; 3D AGSL Plasma Flux Ring wraps the 1:1 arenaRadius Circular Minimap.",
        engine.pendingAsteroidRespawns.size,
        78.32f * crateVisScale,
      )
    )

    sb.appendLine()
    sb.appendLine(
      "[2. RIFT CONDUIT RESONANCE, ACCURACY & OVERDRIVE MODIFIERS, <= 15.0S MATCHMAKING & 6 RANKED/CUSTOM MODES]"
    )
    val liveRes = engine.liveResonance
    val peakRes = kotlin.math.max(liveRes, engine.peakResonance)
    val conduitTier = RiftProgressionCatalog.getConduitTierForResonance(liveRes)
    val nextTierThreshold = RiftProgressionCatalog.getNextConduitTierThreshold(liveRes)
    val nextTierText =
      if (liveRes >= 1500) "APEX ARCHON (MAX TIER)"
      else "${nextTierThreshold.first} Resonance -> ${nextTierThreshold.second}"
    val startRes = engine.getLocalStartingBounty()
    sb.appendLine(
      "- Rift Conduit Resonance & Matchmaking (100% Live Resonance & Daily Directives progression): Conduit Rank Tier = ${conduitTier.title} | Next Tier Threshold = $nextTierText | Live Resonance = $liveRes | Peak Resonance = $peakRes | Starting Match Resonance = $startRes Resonance (Ranked scales 100 -> 150 -> 225 -> 300 -> 400 -> 500 Max across Drift Initiate, Vanguard, Interceptor, Phantom, Rift Commander, and Apex Archon; Custom Rifts standardize starting Resonance to 100 Resonance). Ranked Resonance matchmaking completes in <= 15.0s with Resonance-matched AI backfill bots sporting tier-accurate Skins 0..5 and Resonance-gated cosmetics. Post-life Ranked Accuracy Resonance callouts trigger on hull loss with 8+ shots fired: HULL LOST • +4 ACC at >= 45% weapon accuracy or HULL LOST • -2 ACC at < 15% accuracy; Final 1v1 showdown triggers 180 BPM Overdrive."
    )
    val directives = localPrefs.getDailyDirectives(currentUserId)
    val directivesSummary =
      directives.joinToString(" | ") { d ->
        "${d.title}: ${d.progressValue}/${d.targetValue} (${d.shardReward} Rift Shards +${d.resonanceReward} Resonance, ${if (d.claimed) "CLAIMED" else if (d.isComplete) "READY" else "ACTIVE"})"
      }
    sb.appendLine(
      "- 3 Daily Conduit Directives (up to 250 Rift Shards/day including +100 Rift Shards 3/3 Sweep Chest): $directivesSummary"
    )
    sb.appendLine(
      String.format(
        Locale.US,
        "- Zero-Friction Newtonian Drift & Controls: Ships coast indefinitely when releasing the Left Stick. Left Stick >80%% ignites Afterburners (%.2fx thrust, drains %.0f Flux/s floored at 1 Flux). Braking occurs ONLY via manual [BRAKE] or mapped Gamepad Retro-Brake (damping %.2f). Live Controller Bindings: Main=%s, Buff=%s, Brake=%s, Secondary=%s, Repel=%s, Multi=%s; LB/RB cycle living allies while spectating.",
        RiftCalibrationState.afterburnerThrustMult,
        RiftCalibrationState.afterburnerDrainPerSec,
        RiftCalibrationState.retroBrakeDamping,
        RiftCalibrationState.getBindingLabel(RiftCalibrationState.bindPrimaryFire),
        RiftCalibrationState.getBindingLabel(RiftCalibrationState.bindSpecialBuff),
        RiftCalibrationState.getBindingLabel(RiftCalibrationState.bindRetroBrake),
        RiftCalibrationState.getBindingLabel(RiftCalibrationState.bindSecondaryAbility),
        RiftCalibrationState.getBindingLabel(RiftCalibrationState.bindRepel),
        RiftCalibrationState.getBindingLabel(RiftCalibrationState.bindMultiToggle),
      )
    )
    val matchRemainingSec =
      (engine.publicMatchCycleLimitSec - engine.publicMatchDurationSec).coerceAtLeast(0f)
    sb.appendLine(
      String.format(
        Locale.US,
        "- Public FFA Sector Convergence & Live Resonance Math: %.0f-minute cycle (%.0fs remaining). Local pilot spawns at %d Starting Resonance; High-Resonance Target halo above match average (Rift Leader threshold >= %d Resonance); eliminating a target awards +%.0f%% victim Resonance; Supply Crates +%d Resonance. Winning with >50%% presence awards +100 Rift Shards.",
        engine.publicMatchCycleLimitSec / 60f,
        matchRemainingSec,
        startRes,
        RiftCalibrationState.hvtBountyThreshold,
        RiftCalibrationState.bountyKillBonusPct * 100f,
        RiftCalibrationState.cratePickupBounty,
      )
    )
    sb.appendLine(
      "- 6 Official Ranked Modes + Custom Rifts (Rift Hub ACTIVE RIFTS shelf or Overwatch [RIFT IN], launching directly with the Charged or Best ship on HANGAR DECK):"
    )
    sb.appendLine(
      "  - 1) SECTOR CONVERGENCE: 20-Pilot Ranked FFA / 1-5 Pilot Squadron | 5x Rift Station Safe Harbor ON | +100 Rift Shards."
    )
    sb.appendLine(
      "  - 2) RIFT SKIRMISH: Ranked 2v2-5v5 Best-of-3 Rounds Team Deathmatch (180 BPM Final 1v1 Overdrive) | Safe Zone OFF | Ship Lock ON | Teammate Spectator | +75 Rift Shards."
    )
    sb.appendLine(
      String.format(
        Locale.US,
        "  - 3) FLUX CORE: 20-Pilot Ranked Powerball FFA | 5x Rift Station Safe Harbor ON | Tier 1-10 Overcharge (+15 Resonance/s; 2x pulsing Gold radar beacon) | Ejects at %.0f px/s on death or Safe Zone entry.",
        RiftCalibrationState.fluxCoreEjectSpeed,
      )
    )
    sb.appendLine(
      String.format(
        Locale.US,
        "  - 4) STASIS BREACH: Ranked 10v10 Circular Stasis Jailbreak | Safe Zone OFF | Prisoners inside deal 100%% weapon damage to the %.0f Flux Stasis Core to detonate a radial shockwave and break everyone out | +100 Rift Shards (Live Cores: West=%.0f Flux, East=%.0f Flux).",
        RiftCalibrationState.stasisBrigReactorMaxHp,
        engine.westBrigGeneratorHp,
        engine.eastBrigGeneratorHp,
      )
    )
    sb.appendLine(
      "  - 5) RIFT CONTROL: Ranked Tug-of-War over Rift Alpha & Rift Omega | Safe Zone OFF | Graphical Dual-Rift Tug-of-War Top HUD Bar | +85 Rift Shards."
    )
    val myWarRec = localPrefs.getSyndicateWarRecord(syndicateTag)
    sb.appendLine(
      "  - 6) SYNDICATE WARS: Ranked Direct Syndicate-vs-Syndicate Ladder | Safe Zone OFF | Ship Lock ON | Requires a Sworn Pilot Syndicate and 100% same-Syndicate Squadron | Ranked by Syndicate War Trophies (Active Syndicate [$syndicateTag] Record: ${myWarRec.warTrophies} War Trophies, ${myWarRec.warWins}W-${myWarRec.warLosses}L, Streak ${myWarRec.warStreak})."
    )
    sb.appendLine(
      "  - Custom Rifts (ACTIVE RIFTS shelf): Public Custom Rifts Browser, 6-char Join Codes, PUBLIC/PRIVATE rooms, 2x3 base-mode chip grid (FFA, SKIRMISH, FLUX CORE, BREACH, CONTROL, SYN WARS), 5%-step 0%..100% Damage Taken & Weapon Flux Cost scales, Bot Count (0..19), Bot Skill (Cadet/Regular/Veteran/Ace), Time Limit, Score Cap, Bullet Scale, Ship Speed, Weapon Damage, Flux Regen, Supply Pod Rate, and Allow Squadrons in FFA."
    )
    sb.appendLine(
      "  - Active Ranked Mode Right Now: ${engine.activeRankedMode.title} (${engine.activeRankedMode.id}, Bracket=${engine.activeTeamSizeBracket}, Human Pilots in Mode=${engine.getHumanPilotCountForMode(engine.activeRankedMode)}, ShipLocked=${engine.doesModeLockShipSelection()})."
    )

    sb.appendLine()
    sb.appendLine("[3. COMPLETE 10-SHIP ROSTER -- ALL 10 OPEN AT 0 RESONANCE (100-POINT PARITY)]")
    for (spec in RiftCodex.getAllTenShips()) {
      val shaftWorldPx = spec.primarySpeed * 0.036f
      val outerWidthWorldPx = 6.4f * projScale
      val totalWorldLenPx = shaftWorldPx + outerWidthWorldPx
      val shotsPerSec = if (spec.primaryCooldown > 0.001f) 1f / spec.primaryCooldown else 0f
      val volleyDmg = spec.primaryDamage * spec.primaryStreams
      val primaryDps = volleyDmg * shotsPerSec
      val fluxPerSec = spec.primaryCost * shotsPerSec
      val cleanShipName =
        if (spec.id == 10) "Rift Frame" else spec.name.replace(Regex("^[0-9]+-"), "").trim()

      sb.appendLine(
        String.format(
          Locale.US,
          "* %s (%s) [100-Pt Allocation: Spd %d, Thr %d, Turn %d, MaxFlux %d, Rech %d, Pri %d, Sec %d = %d Pts]:",
          cleanShipName,
          spec.role,
          spec.speedPts,
          spec.thrustPts,
          spec.turnPts,
          spec.maxFluxPts,
          spec.rechargePts,
          spec.primaryPts,
          spec.secondaryPts,
          spec.totalAllocatedPoints,
        )
      )
      sb.appendLine(
        String.format(
          Locale.US,
          "  - Hull: MaxFlux=%.0f Flux, Recharge=%.0f Flux/s, TopSpeed=%.0f px/s (Afterburner=%.0f px/s), Thrust=%.0f px/s^2, TurnRate=%.2f rad/s.",
          spec.maxFlux,
          spec.rechargeRate,
          spec.topSpeed,
          spec.afterburnerSpeed,
          spec.thrust,
          spec.turnRate,
        )
      )
      sb.appendLine(
        String.format(
          Locale.US,
          "  - Primary [%s]: %d stream(s), %.0f Flux/stream (%.0f Flux/volley), Cost=%.0f Flux/shot, Cooldown=%.3fs, Speed=%.0f px/s, Bounces=%d | Raw DPS=%.0f Flux/s, Drain=%.0f Flux/s | Bolt=%.2f world px.",
          spec.primaryTitle,
          spec.primaryStreams,
          spec.primaryDamage,
          volleyDmg,
          spec.primaryCost,
          spec.primaryCooldown,
          spec.primarySpeed,
          spec.primaryBounces,
          primaryDps,
          fluxPerSec,
          totalWorldLenPx,
        )
      )
      if (spec.hasBomb) {
        sb.appendLine(
          String.format(
            Locale.US,
            "  - Secondary [%s | type=%s]: Cost=%.0f Flux, Cooldown=%.2fs, Damage=%.0f Flux, WallBounces=%d.",
            spec.secondaryTitle,
            spec.bombType,
            spec.bombCost,
            spec.bombCooldown,
            spec.bombDamage,
            spec.bombBounces,
          )
        )
      }
      sb.appendLine("  - Lore & Role: ${spec.tacticalLore}")
    }

    sb.appendLine()
    sb.appendLine(
      "[4. STRICT LIVE-RESONANCE VS. SHARD STORE ECONOMY, OVERWATCH RIFT FRAME DRYDOCK & FRAMES A-E]"
    )
    sb.appendLine(
      "- Economy Split: Frame A and default cosmetics (id == 0) are always active from 0 Resonance. Frame B (250 Rift Shards), Ship Skins 1..5 across all 10 hulls (1: Obsidian Ice-Cyan 350, 2: Sovereign Solar Gold 500, 3: Rift Singularity Violet 650, 4: Crimson Forge Anodized 750, 5: Cryo-Titanium Stealth 850 Rift Shards), and post-1st-free Callsign renames (250 Rift Shards) are permanent Rift Shard Store unlocks. Frame C (650 Resonance), Frame D (1100 Resonance), Frame E (1600 Resonance), Kill Finishers 1..4 (200/550/1050/1650 Resonance), Warp Signatures 1..3 (350/750/1300 Resonance), and Resonance Leader Halos 1..3 (500/1000/1750 Resonance) are NEVER sold for Rift Shards and remain active strictly while Live Resonance >= threshold (non-destructively falling back to Frame A / default 0 if Live Resonance drops)."
    )
    val isFrameBUnlocked = localPrefs.isCustomSlotUnlocked("Frame B", currentUserId)
    val rawActiveSlotId = localPrefs.getActiveCustomSlotId(currentUserId)
    val effectiveSlotId =
      if (RiftProgressionCatalog.isFrameSlotActive(rawActiveSlotId, liveRes, isFrameBUnlocked)) {
        rawActiveSlotId
      } else {
        "Frame A"
      }
    for (slotId in CustomShipLoadoutSlot.ALL_SLOT_IDS) {
      val slot = localPrefs.getCustomShipSlot(slotId, currentUserId)
      val slotActive = RiftProgressionCatalog.isFrameSlotActive(slotId, liveRes, isFrameBUnlocked)
      val gateStatus =
        when (slotId) {
          "Frame A" -> "ALWAYS ACTIVE"
          "Frame B" -> if (slotActive) "SHARD UNLOCKED" else "250 RIFT SHARDS"
          "Frame C" -> if (slotActive) "RESONANCE ACTIVE (650+)" else "REQUIRES 650 LIVE RESONANCE"
          "Frame D" -> if (slotActive) "RESONANCE ACTIVE (1100+)" else "REQUIRES 1100 LIVE RESONANCE"
          "Frame E" -> if (slotActive) "RESONANCE ACTIVE (1600+)" else "REQUIRES 1600 LIVE RESONANCE"
          else -> if (slotActive) "ACTIVE" else "GATED"
        }
      val activeMarker = if (slotId == effectiveSlotId) " [EQUIPPED]" else ""
      sb.appendLine(
        "  - $slotId$activeMarker ($gateStatus) '${slot.customName}': ${slot.totalAllocatedPoints()} Pts (Spd=${slot.speedPts}, Thr=${slot.thrustPts}, Turn=${slot.turnPts}, Flux=${slot.maxFluxPts}, Rech=${slot.rechargePts}, Pri=${slot.primaryArchetype}, Sec=${slot.secondaryType})."
      )
    }

    sb.appendLine()
    sb.appendLine(
      "[5. ${RiftCodex.POWERUP_CODEX.size}-BUFF SUPPLY POOL, FORGED BUFFS & VOLUMETRIC PLAYFIELD RIFTS (440PX GRAVITY IMPLOSION & UNSTABLE RIFT)]"
    )
    sb.appendLine(
      "- Supply Crate Cap: ${engine.getEffectiveBuffCap()} crates (${engine.greens.size} spawned, ${engine.swarmDrones.size} active Swarm Drones, ${engine.brickWalls.size} active Stasis Barriers). Repel Cost=${RiftCalibrationState.repelCost.toInt()} Flux, Radius=${RiftCalibrationState.repelRadius.toInt()}px."
    )
    for (entry in RiftCodex.POWERUP_CODEX) {
      sb.appendLine("  - ${entry.title} (${entry.dropRatePercent}% drop): ${entry.effectSummary}")
    }
    val customForged = RiftCodex.getCustomForgedBuffs()
    for (cb in customForged) {
      sb.appendLine(
        "  - [CUSTOM FORGED] ${cb.officialName} (${cb.shortLabel}): ${cb.chargesPerPickup} charges, effect=${cb.specialEffect.label}, dmg=${cb.damage.toInt()} Flux, radius=${cb.blastRadius.toInt()}px."
      )
    }
    val unstableCount = engine.anomalies.count { it.isUnstableMergedRift }
    sb.appendLine(
      String.format(
        Locale.US,
        "- Volumetric Storm-Cloud Playfield Rifts (100%% soft volumetric cloud tears centered at (cx, cy) with fractal branching perimeter lightning and zero metallic collars/spokes/central balls; %.0fpx base physical core radius, %.0fs lifespan, %d active, %d Unstable Merged): 1) PLASMA RIFT (Crimson/Magenta, %.0f%% Max Flux burn/0.6s), 2) REPULSOR RIFT (pure Cobalt-to-Ice-Blue, %.0fpx deflection + inner-core 1.45x afterburner supersonic slingshot), 3) GRAVITY RIFT (Indigo/Violet, %.0fpx pull; swept 0.92x visible-disc weapon absorption grows radius +18px/bolt, +40px/bomb-torpedo, +14px/drone-phantom up to 440px max; at >= 260px swollen mass drags Crystal Asteroids, Orbital/Asteroid Stations, Debris, and Stasis Barriers into the core at 950/s tidal crush -> implosion + Supply Crate + 20.0s respawn + 16-bolt 360-degree Station Overload Nova), 4) PAIRED PHASE RIFTS (Teal/Cyan Phase Rift A & B instantaneous gateway, %.2fs re-entry CD), 5) UNSTABLE RIFT (Multi-Spectrum overlapping Rifts merge into a 10.0s circular convergence singularity that detonates for full-sector insta-kill outside Safe Harbor and shatters nearby Crystal Asteroids).",
        RiftCalibrationState.riftPhysicalRadius * anomVisScale,
        RiftCalibrationState.riftLifespanSec,
        engine.anomalies.size,
        unstableCount,
        RiftCalibrationState.plasmaRiftDamagePct * 100f,
        RiftCalibrationState.repulsorFieldRadius,
        RiftCalibrationState.gravityPullRadius,
        RiftCalibrationState.phaseRiftCooldownSec,
      )
    )

    sb.appendLine()
    sb.appendLine("[6. REAL-PLAYER-ONLY SWORN PILOT LEADERBOARD & DOSSIER RECORDS]")
    val realCloudLeaderboard =
      cloudLeaderboard.filter {
        val rawT = it.syndicateTag.trim().uppercase(Locale.US)
        rawT != "BOT" &&
          rawT != "RFT" &&
          !it.callsign.contains("[BOT]") &&
          !it.userId.startsWith("dev_")
      }
    val topCloudAllTime = realCloudLeaderboard.maxByOrNull { it.allTimeBounty }
    if (topCloudAllTime != null) {
      val synClean = localPrefs.sanitizeSyndicateTag(topCloudAllTime.syndicateTag)
      val synBadge = if (synClean.isNotEmpty()) "[$synClean] " else ""
      sb.appendLine(
        "- Top Cloud Sworn Pilot All-Time Record: $synBadge${topCloudAllTime.callsign} with ${topCloudAllTime.allTimeBounty} Peak Resonance (${topCloudAllTime.allTimeKills}K/${topCloudAllTime.allTimeDeaths}D)."
      )
    } else {
      sb.appendLine(
        "- Cloud Leaderboard Status: 0 Sworn Pilot records synced yet (AI Bots and [RFT] Drifters are strictly excluded)."
      )
    }

    val p = engine.localPlayer
    val pSpeed = hypot(p.vx, p.vy).roundToInt()
    val secX = ((p.x / 1000f).toInt()).coerceIn(0, 7)
    val secY = ((p.y / 1000f).toInt()).coerceIn(0, 7)
    val sectorStr = "${('A' + secX)}${secY + 1}"
    val perShipResSummary =
      (1..10).joinToString(", ") { id ->
        val cleanName =
          if (id == 10) "Rift Frame"
          else RiftCodex.getShipById(id).name.replace(Regex("^[0-9]+-"), "").trim()
        "$cleanName:${engine.getShipResonance(id)}"
      }
    val activeSynClean = localPrefs.sanitizeSyndicateTag(syndicateTag)
    val activeSynLabel = if (activeSynClean.isNotEmpty()) "[$activeSynClean] " else "[SOLO] "
    val skinName =
      RiftProgressionCatalog.hullTrims.firstOrNull { it.id == p.equippedHullSkin }?.name
        ?: "Standard Faction Trim"
    val finName =
      RiftProgressionCatalog.killFinishers.firstOrNull { it.id == p.equippedKillFinisher }?.name
        ?: "Plasma Supernova"
    val warpName =
      RiftProgressionCatalog.warpSignatures.firstOrNull { it.id == p.equippedWarpStyle }?.name
        ?: "Cyan Phase Aperture"
    val haloName =
      RiftProgressionCatalog.hvtHalos.firstOrNull { it.id == p.equippedHvtHaloStyle }?.name
        ?: "Tactical Amber Reticle"
    sb.appendLine(
      "- Active Pilot: $activeSynLabel$callsign (Syndicate: '$syndicateName') | Conduit Rank Tier: ${conduitTier.title} (Next Tier: $nextTierText) | Starting Match Resonance: $startRes | Live Resonance: $liveRes | Peak Resonance: $peakRes | Per-Ship Resonance (10 Hulls): [$perShipResSummary] | Rift Shards: ${engine.riftShards} | Cosmetics: Skin='$skinName' (#${p.equippedHullSkin}), Finisher='$finName' (#${p.equippedKillFinisher}), Warp='$warpName' (#${p.equippedWarpStyle}), Leader Halo='$haloName' (#${p.equippedHvtHaloStyle}) | Live Hull: ${engine.formatShipLabel(p)} (${engine.getShipResonance(p.shipId)} Resonance) in Sector $sectorStr, Speed=$pSpeed px/s, Flux=${p.flux.toInt()}/${p.maxFlux.toInt()} Flux."
    )

    fun formatSnapshot(label: String, snap: PeakBountyRunSnapshot?) {
      if (snap == null) return
      sb.appendLine(
        "- $label: Peak Resonance=${snap.bountyReached} Resonance in ${snap.shipFlown}, Survived=${snap.formattedDuration()}, Kills=${snap.pvpKills} PVP / ${snap.botKills} Bot, Damage=${snap.damageDealt.toInt()} Flux dealt / ${snap.damageTaken.toInt()} Flux taken."
      )
    }
    formatSnapshot("All-Time Peak Resonance Run", allTimePeakSnapshot)
    formatSnapshot("PVP Peak Resonance Run", pvpPeakSnapshot)

    if (syndicateHistory.isNotEmpty()) {
      sb.appendLine(
        "- Syndicate Service History: " +
          syndicateHistory.joinToString(" ; ") {
            "[${it.syndicateTag}] ${it.syndicateName} (${it.totalKills}K, ${it.publicGameWins}W)"
          }
      )
    }

    val modifiedCalibrations =
      RiftCalibrationState.buildAllParameterDescriptors().mapNotNull { desc ->
        val cur = desc.getter()
        val def = RiftCalibrationState.DEFAULT_GAME_CONFIG[desc.key]
        if (def != null && abs(cur - def) > 0.0005f) {
          "${desc.title}: ${desc.formatValue(def)} -> ${desc.formatValue(cur)}"
        } else {
          null
        }
      }
    if (modifiedCalibrations.isNotEmpty()) {
      sb.appendLine(
        "- Live Calibration Overrides (${modifiedCalibrations.size}): ${modifiedCalibrations.joinToString(" | ")}"
      )
    }

    return sb.toString()
  }
}
