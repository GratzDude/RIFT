package com.example.engine

import com.example.data.LeaderboardEntry
import com.example.data.LocalPilotPrefs
import com.example.data.PeakBountyRunSnapshot
import com.example.data.SyndicateServiceEntry
import java.util.Locale
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.roundToInt

/**
 * Dynamically compiles the complete, authoritative RIFT Engine, Pixel-Rendering,
 * 100-Point Ship Math, Buff Registry, Calibration, Pilot Dossier, and Cloud Leaderboard
 * Ground-Truth Snapshot for NEXUS (Omniscient Rift Core Tactical Intelligence).
 */
object RiftNexusKnowledge {

  const val NEXUS_SYSTEM_INSTRUCTION: String =
    """
You are NEXUS, the omniscient Rift Core tactical intelligence embedded inside RIFT, a mobile-first competitive 2D Newtonian space arena game on Android.

CRITICAL DOMAIN & KNOWLEDGE BOUNDARY (NON-NEGOTIABLE):
1. EXCLUSIVE RIFT-ONLY KNOWLEDGE BASE: Your ONLY knowledge base is RIFT itself — its physics, pixel-level rendering math, 10 ship classes (9 standard hulls: Viper, Ricochet, Suppressor, Goliath, Twinfang, Disruptor, Valkyrie, Manta, and Interdictor + #10 modular custom ship: Rift Frame), 100-point Rift Frame modular builder & AI Hull Forge, Supply Buff pool + custom AI-forged buffs, 4 Playfield Rifts, Safe Zone & AFK rules, Calibration parameters, Syndicates, Named Armadas, live sector pilots, Pilot Dossier & Executioner telemetry, and Cloud Leaderboard bounty records.
2. REFUSE NON-RIFT TOPICS IN CHARACTER: If the user asks about anything outside RIFT (real-world news, weather, politics, coding/homework unrelated to RIFT's own mechanics, other video games, recipes, trivia), briefly decline in character: "RIFT CORE TELEMETRY BOUNDARY // Query falls outside RIFT sector parameters. Submit a query regarding RIFT ships, pixel/physics telemetry, custom builds, buffs, or bounty records."
3. PINPOINT NUMERICAL & PIXEL PRECISION: Every turn, you receive a [LIVE RIFT GROUND-TRUTH TELEMETRY SNAPSHOT] containing the exact live engine numbers, world-pixel dimensions, zoom-scaled screen-pixel dimensions, DPS, Flux (⚡) costs, Rift Shards (❖) economy, custom slots (Frame A / Frame B / Frame C), Calibration overrides, Executioner snapshots, and Cloud Leaderboard records. Always cite exact numbers from that snapshot — never guess or approximate when exact values exist. Always use ⚡ for Flux (HP + weapon energy) and ❖ strictly for Rift Shards (permanent progression currency).
4. ANDROID TOUCH HUD & TWIN-STICK CONTROLLER TERMINOLOGY ONLY: RIFT supports both the native Android Landscape Touch HUD (Left Movement Stick with zero-friction coasting on release and >80% Afterburner, Independent Manual HUD [BRAKE] Stop button, Right Primary Fire Stick, Secondary Button, Repel Button, 4th Elastic Special Buff Button, and Native Multi-Shot button) AND Plug-and-Play Bluetooth/USB Twin-Stick Controllers (Left Stick permanently locked to 360° flight with >80% Afterburner and zero-friction drift on release; Right Stick permanently locked to 360° independent turret aiming; when Right Stick is neutral, all weapons fire along the Left-Stick ship heading; default ergonomic button bindings: RT/R2 Main Attack, R1/RB Special Buff Attack, LT/L2 Manual Retro-Brake, X Instant 1-Press Secondary Ability/Warhead, A Repel Shockwave, Y Native Multi-Shot toggle, plus a WoW-style live controller button remapper in Settings & Codex; D-Pad Camera Zoom & Hangar 1..10 ship navigation, Start/Select modals). NEVER mention PC keyboard keys or shortcuts.
5. PURE TEXT Q&A & THEORYCRAFTING: Deliver clear, structured, tactical text answers and 100-point Rift Frame theorycrafting breakdowns (showing exact point costs summing to the pilot's target budget such as 20 pts at Lv 20 or 100 pts at Lv 100, plus resulting Speed, Thrust, Turn Rate, Max Flux, Recharge, Primary, and Secondary stats). Never prefix ship names with legacy numbers (say "Viper", "Disruptor", "Interdictor", "Rift Frame", never "1-Viper" or "6-Disruptor"). Keep formatting crisp and scannable on a landscape mobile tactical console.
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
    val sb = StringBuilder(14_000)
    val camScale = engine.effectiveCameraScale()
    val projScale = RiftCalibrationState.projectileScale
    val shipVisScale = RiftCalibrationState.shipScale
    val crateVisScale = RiftCalibrationState.crateScale
    val anomVisScale = RiftCalibrationState.anomalyScale

    sb.appendLine("=== [LIVE RIFT GROUND-TRUTH TELEMETRY SNAPSHOT] ===")

    // 1. RENDERING, PIXEL DIMENSIONS & WORLD GEOMETRY
    sb.appendLine()
    sb.appendLine("[1. EXACT PIXEL, SPRITE & ARENA RENDERING SPECIFICATIONS]")
    sb.appendLine("- World Arena Size: 8000 x 8000 world pixels (8x8 sectors A1..H8, each sector = 1000 x 1000 world px).")
    sb.appendLine("- Central Safe Zone Rect: x=[3600..4400], y=[3600..4400] (800 x 800 world px centered at (4000, 4000), spanning D4/E4/D5/E5).")
    sb.appendLine(
      String.format(
        Locale.US,
        "- Camera Scale Formula: effectiveCameraScale = 0.65 (base) * cameraZoom (%.2fx) * cameraZoomScale (%.2fx) = %.4fx live (1 world px = %.4f screen px; at factory default 1.0x zoom, 1 world px = 0.65 screen px).",
        engine.cameraZoom,
        RiftCalibrationState.cameraZoomScale,
        camScale,
        camScale,
      )
    )
    sb.appendLine(
      String.format(
        Locale.US,
        "- Ship Hull Rendering & Hitbox: Offscreen procedural 3D metallic bitmap = 128x128 px; physical collision radius = 36.0 world px (72.0 world px hitbox diameter); live world render quad = %.2f x %.2f world px (88.0 * shipScale %.2fx = %.2f screen px at live zoom, 57.20 screen px at default 0.65x zoom); Radial Flux Capacitor Ring radius = %.2f world px (%.2f world px diameter).",
        88f * shipVisScale,
        88f * shipVisScale,
        shipVisScale,
        88f * shipVisScale * camScale,
        44f * shipVisScale,
        88f * shipVisScale,
      )
    )
    sb.appendLine(
      String.format(
        Locale.US,
        "- Primary Laser / Bolt Exact Pixel Formula (RiftArenaView step 8): Every primary projectile is rendered as a dual-pass StrokeCap.ROUND vector line from (x, y) to (x - vx * 0.036, y - vy * 0.036): Shaft Vector Length = primarySpeed * 0.036 world px (36ms motion trail); Outer Colored Glow Stroke Width = %.2f world px (6.4 * projectileScale %.2fx); Inner White Core Stroke Width = %.2f world px (2.8 * projectileScale %.2fx); Total Visual Footprint Length (including round endcaps of +3.2*projectileScale on each tip) = (primarySpeed * 0.036) + (6.4 * projectileScale) world px.",
        6.4f * projScale,
        projScale,
        2.8f * projScale,
        projScale,
      )
    )
    sb.appendLine(
      String.format(
        Locale.US,
        "- Secondary Warhead & Ability Pixel Radii (scaled by projectileScale %.2fx): Viper Phase Dash = 135 world px wall-safe spatial blink + %.0f px/s supersonic dodge burst (0.42s combat dodge i-frames + 0.65s phase shield + +%.0f%% next-shot rail overcharge, 300⚡ cost, 2.5s CD); Siege Bomb = %.1f world px radius (24px base, 48px dia, 540 px/s + 25%% ship vel); Bounce Bomb & Proximity Flak Bomb = %.1f world px radius (17px base, 34px dia, 640 px/s + 25%% ship vel; Prox Flak fuse trigger = %.0f world px, red sensor ring = %.1f world px, bursts into 8 shrapnel bolts); Disruptor 360° AoE EMP Pulse = %.0f world px radial shockwave centered on Disruptor's hull upon uncloaking (0⚡ decloak EMP cost while Cloak drains %.0f⚡/s; drains %.0f%% Max Flux & disables recharge for %.1fs; forced uncloak from enemy fire or <=150⚡ floor breaks stealth cleanly without firing EMP); Proximity Mine = %.1f world px radius (18px base, 36px dia, stationary, max %d active mines, 30s life); Seeker Torpedo = %.1f world px radius (%.0f px/s, %.1fs flight = %.0f world px max flight path); Stasis Barrier = %.0f world px hard-light span (%.1fs duration); Interdictor Stasis Bubble (INTERDICTOR_DOME) = %.0f world px radius (%.0f world px diameter) two-way hard-light containment sphere active for %.1fs (600⚡ cost, %.1fs CD — nothing can enter or leave; bounces ships with 0⚡ wall collision damage and reflects bullets/bombs both inside and outside).",
        projScale,
        RiftCalibrationState.phaseDashImpulse,
        RiftCalibrationState.phaseOverchargeBonusPct * 100f,
        24f * projScale,
        17f * projScale,
        RiftCalibrationState.proxFuseRadius,
        45f * projScale,
        RiftCalibrationState.empPulseRadius,
        RiftCalibrationState.cloakDrainPerSec,
        RiftCalibrationState.empFluxDrainPct * 100f,
        RiftCalibrationState.empFreezeSec,
        18f * projScale,
        RiftCalibrationState.maxMinesPerShip,
        19f * projScale,
        RiftCalibrationState.seekerSpeed,
        RiftCalibrationState.seekerFlightTime,
        RiftCalibrationState.seekerSpeed * RiftCalibrationState.seekerFlightTime,
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
        "- Supply Crates, Walls & World Labels: Supply Crate offscreen icon = 68x68 px, world render size = %.2f world px (88 * 0.89 * crateScale %.2fx = 89%% of ship visual size); Arena Walls = 64x64 px dark obsidian bulkhead tiles with cyan energy conduit seams & 3D mitered bevels; World Label Fonts = 1:1 physical screen pixels (Nametag %.0fpx, Bounty %.0fpx, Floating Combat Text %.0fpx * textScale %.2fx); Circular Radar Minimap = 122dp diameter (expands 1.65x for 1.5s on tap).",
        78.32f * crateVisScale,
        crateVisScale,
        RiftCalibrationState.nametagFontSizePx,
        RiftCalibrationState.bountyFontSizePx,
        RiftCalibrationState.fctFontSizePx,
        engine.playfieldTextScale * RiftCalibrationState.labelTextScale,
      )
    )

    // 2. PHYSICS, UNIFIED FLUX, SAFE ZONE & MATCH RULES
    sb.appendLine()
    sb.appendLine("[2. NEWTONIAN PHYSICS, UNIFIED FLUX CAPACITOR, TOUCH/GAMEPAD CONTROLS & SAFE ZONE RULES]")
    sb.appendLine(
      String.format(
        Locale.US,
        "- Zero-Friction Newtonian Drift & Dual Touch/Gamepad Controls: Ships coast indefinitely on (vx, vy) with zero friction when releasing the Left Stick (both Touch and Bluetooth/USB Gamepad). Left Stick >80%% radius ignites Afterburners (%.2fx thrust, ~1.32x top speed, drains %.0f⚡/s floored safely at 1⚡). Braking occurs ONLY via the independent manual HUD [BRAKE] button or mapped Gamepad Retro-Brake trigger/button (retro-braking damping %.2f). On Gamepad, Left Stick is permanently locked to 360° flight & ship heading, and Right Stick is permanently locked to 360° independent turret aiming (when Right Stick is neutral, Main Attack, Secondary Ability, and Special Buff Attack fire straight along the Left-Stick heading; Deadzones: Left %.0f%%, Right %.0f%%; Idle Touch Ring Opacity: %.0f%%). Live Controller Bindings (customizable via WoW-style remapper in Settings & Codex): Main Attack=%s (default RT/R2), Buff Attack=%s (default R1/RB), Manual Retro-Brake=%s (default LT/L2), Instant Secondary Ability=%s (default X BTN), Repel=%s (default A BTN), Multi-Shot Toggle=%s (default Y BTN); D-Pad steps Camera Zoom & navigates Hangar 1..10 ships; Start/Select toggle Hangar & Calibration.",
        RiftCalibrationState.afterburnerThrustMult,
        RiftCalibrationState.afterburnerDrainPerSec,
        RiftCalibrationState.retroBrakeDamping,
        RiftCalibrationState.gamepadDeadzoneLeft * 100f,
        RiftCalibrationState.gamepadDeadzoneRight * 100f,
        RiftCalibrationState.gamepadIdleTouchOpacity * 100f,
        RiftCalibrationState.getBindingLabel(RiftCalibrationState.bindPrimaryFire),
        RiftCalibrationState.getBindingLabel(RiftCalibrationState.bindSpecialBuff),
        RiftCalibrationState.getBindingLabel(RiftCalibrationState.bindRetroBrake),
        RiftCalibrationState.getBindingLabel(RiftCalibrationState.bindSecondaryAbility),
        RiftCalibrationState.getBindingLabel(RiftCalibrationState.bindRepel),
        RiftCalibrationState.getBindingLabel(RiftCalibrationState.bindMultiToggle),
      )
    )
    sb.appendLine(
      String.format(
        Locale.US,
        "- Collisions & Unified Flux (⚡ = HP + Ammo): Wall bounce elasticity = %.2f with ZERO wall collision damage. Ships pass through other ships without hull-to-hull collision (Stasis Barriers physically block ships & bounce shots). Firing weapons/afterburners/cloak/repel drains Flux but floors at 1⚡ (cannot self-kill). Enemy damage reducing Flux <= 0⚡ destroys the ship, anchoring a stationary %.1fs death camera at death coordinates before Safe Zone respawn (%.2fs hit-stop freeze on lethal blasts).",
        RiftCalibrationState.wallElasticity,
        RiftCalibrationState.respawnDelaySec,
        RiftCalibrationState.hitStopDurationSec,
      )
    )
    sb.appendLine(
      String.format(
        Locale.US,
        "- Safe Zone & Anti-AFK Rules: Inside Safe Zone, weapons are locked and Flux recharges at %.2fx speed, capped at %.0fs stay before automatic arena eviction teleport. Exiting grants a %.1fs Exit Phase Shield AND triggers a %.1fs Safe Zone Re-Entry Repulsion Lockout (amber barrier knocks locked-out ships away at %.0f px/s). In open space, %.0fs of zero input triggers AFK Flux Overload draining %.0f%% Max Flux/sec.",
        RiftCalibrationState.safeZoneRechargeMult,
        RiftCalibrationState.safeZoneIdleLimitSec,
        RiftCalibrationState.exitPhaseShieldSec,
        RiftCalibrationState.safeZoneReentryLockoutTime,
        RiftCalibrationState.safeZoneRepelForce,
        RiftCalibrationState.playfieldIdleLimitSec,
        RiftCalibrationState.playfieldIdleDrainPctPerSec * 100f,
      )
    )
    val matchRemainingSec = (engine.publicMatchCycleLimitSec - engine.publicMatchDurationSec).coerceAtLeast(0f)
    sb.appendLine(
      String.format(
        Locale.US,
        "- Public FFA Sector Convergence & Bounty Math: %.0f-minute (%.0fs) match cycle (%.0fs remaining in current cycle). Ships spawn at 100 Bounty. High-Value Target (HVT) gold radar crown at >= %d Bounty; >1,000 Bounty triggers arena-wide gold shutdown alerts. Killing an enemy awards +%.0f%% of victim's Bounty; Supply Crates award +%d Bounty. Winning Sector Convergence with >50%% match presence awards Rift Shards.",
        engine.publicMatchCycleLimitSec / 60f,
        engine.publicMatchCycleLimitSec,
        matchRemainingSec,
        RiftCalibrationState.hvtBountyThreshold,
        RiftCalibrationState.bountyKillBonusPct * 100f,
        RiftCalibrationState.cratePickupBounty,
      )
    )

    // 3. ALL 10 SHIPS LIVE STATS + PER-SHIP LASER PIXEL DIMENSIONS
    sb.appendLine()
    sb.appendLine("[3. COMPLETE 10-SHIP ROSTER (9 STANDARD HULLS 1..9 + #10 RIFT FRAME) — LIVE STATS, DPS & EXACT LASER PIXEL DIMENSIONS]")
    for (spec in RiftCodex.getAllTenShips()) {
      val shaftWorldPx = spec.primarySpeed * 0.036f
      val outerWidthWorldPx = 6.4f * projScale
      val coreWidthWorldPx = 2.8f * projScale
      val totalWorldLenPx = shaftWorldPx + outerWidthWorldPx
      val shaftScreenPxDef = shaftWorldPx * 0.65f
      val totalScreenPxDef = totalWorldLenPx * 0.65f
      val shaftScreenPxLive = shaftWorldPx * camScale
      val totalScreenPxLive = totalWorldLenPx * camScale
      val outerScreenPxLive = outerWidthWorldPx * camScale
      val coreScreenPxLive = coreWidthWorldPx * camScale
      val shotsPerSec = if (spec.primaryCooldown > 0.001f) 1f / spec.primaryCooldown else 0f
      val volleyDmg = spec.primaryDamage * spec.primaryStreams
      val primaryDps = volleyDmg * shotsPerSec
      val fluxPerSec = spec.primaryCost * shotsPerSec

      sb.appendLine(
        String.format(
          Locale.US,
          "* %s (%s) [100-Pt Allocation: Spd %d, Thr %d, Turn %d, MaxFlux %d, Rech %d, Pri %d, Sec %d = %d Pts]:",
          spec.name,
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
          "  - Hull: MaxFlux=%.0f⚡, Recharge=%.0f⚡/s, TopSpeed=%.0f px/s (Afterburner=%.0f px/s), Thrust=%.0f px/s², TurnRate=%.2f rad/s, InitialRepels=%d.",
          spec.maxFlux,
          spec.rechargeRate,
          spec.topSpeed,
          spec.afterburnerSpeed,
          spec.thrust,
          spec.turnRate,
          spec.initialRepels,
        )
      )
      sb.appendLine(
        String.format(
          Locale.US,
          "  - Primary [%s]: %d stream(s)%s, %.0f⚡/stream (%.0f⚡/volley), Cost=%.0f⚡/shot, Cooldown=%.3fs (%.2f shots/s), Speed=%.0f px/s, Spread=%.3f rad, WallBounces=%d | Raw DPS=%.0f⚡/s, Drain=%.0f⚡/s (Net Sustain=%.0f⚡/s)%s.",
          spec.primaryTitle,
          spec.primaryStreams,
          if (spec.isParallelTwin) " parallel-twin" else "",
          spec.primaryDamage,
          volleyDmg,
          spec.primaryCost,
          spec.primaryCooldown,
          shotsPerSec,
          spec.primarySpeed,
          spec.primarySpreadRad,
          spec.primaryBounces,
          primaryDps,
          fluxPerSec,
          spec.rechargeRate - fluxPerSec,
          if (spec.supportsNativeMultifire) {
            if (spec.id == 5 || spec.isParallelTwin) " [Native Twin Multi-Shot Toggle: 0⚡ extra cost, splits 2x135⚡ into 4x67.5⚡ fan = 270⚡ total]"
            else " [Native Split Multi-Shot Toggle: 0⚡ extra cost, toggles 1x100% focused beam vs 3x33.3% (80⚡ each) split streams]"
          } else "",
        )
      )
      sb.appendLine(
        String.format(
          Locale.US,
          "  - Exact Laser/Bolt Pixel Size for %s: World Shaft=%.2f px, World Total Length (with round glow caps)=%.2f px, World Width=%.2f px outer glow / %.2f px white core | Screen Pixels @ 0.65x default zoom: %.2f px shaft (%.2f px total) x %.2f px glow (%.2f px core) | Screen Pixels @ %.2fx live zoom: %.2f px shaft (%.2f px total) x %.2f px glow (%.2f px core).",
          spec.name,
          shaftWorldPx,
          totalWorldLenPx,
          outerWidthWorldPx,
          coreWidthWorldPx,
          shaftScreenPxDef,
          totalScreenPxDef,
          outerWidthWorldPx * 0.65f,
          coreWidthWorldPx * 0.65f,
          camScale,
          shaftScreenPxLive,
          totalScreenPxLive,
          outerScreenPxLive,
          coreScreenPxLive,
        )
      )
      if (spec.hasBomb) {
        sb.appendLine(
          String.format(
            Locale.US,
            "  - Secondary [%s | type=%s]: Cost=%.0f⚡, Cooldown=%.2fs, Damage=%.0f⚡, WallBounces=%d, Recoil=%.0f px/s.",
            spec.secondaryTitle,
            spec.bombType,
            spec.bombCost,
            spec.bombCooldown,
            spec.bombDamage,
            spec.bombBounces,
            spec.bombRecoil,
          )
        )
      } else {
        sb.appendLine("  - Secondary: ${spec.secondaryTitle} (No Secondary Bomb Button shown on HUD).")
      }
      sb.appendLine("  - Lore & Role: ${spec.tacticalLore}")
    }

    // 4. RIFT FRAME 100-POINT PARITY SYSTEM & PLAYER'S SAVED LOADOUT FRAMES A/B/C
    sb.appendLine()
    sb.appendLine("[4. RIFT FRAME 100-POINT PARITY MATH, OPTIONS & PLAYER SAVED FRAMES A–C]")
    sb.appendLine("- Unlock & Progression: Pilot Levels 1–19 are intro tiers. Level 20 unlocks Rift Frame with 20 Customization Points. Each level from Lv 20 to Lv 100 grants +1 point up to 100 Points at Lv 100 (exact parity with standard hulls, which are all built on the exact same 100-point budget). Calibration also offers a '100-Pt Override' testing toggle.")
    sb.appendLine("- Universal Baseline Hull Floor (0 pts) + Linear Per-Point Scaling (0..30 pts max per stat):")
    sb.appendLine("  • Top Speed = 380 + 8 * pts px/s (0 pts = 380 px/s -> 30 pts = 620 px/s; Afterburner = TopSpeed * 1.32)")
    sb.appendLine("  • Thrust = 600 + 15 * pts px/s² (0 pts = 600 -> 30 pts = 1,050 px/s²)")
    sb.appendLine("  • Turn Rate = 3.20 + 0.10 * pts rad/s (0 pts = 3.20 -> 30 pts = 6.20 rad/s)")
    sb.appendLine("  • Max Flux = 2200 + 80 * pts ⚡ (0 pts = 2,200⚡ -> 30 pts = 4,600⚡)")
    sb.appendLine("  • Flux Recharge = 240 + 8 * pts ⚡/s (0 pts = 240⚡/s -> 30 pts = 480⚡/s)")
    sb.appendLine("- Primary Weapon Archetypes (9 choices):")
    for (pri in CustomShipLoadoutSlot.PRIMARY_OPTIONS) {
      sb.appendLine("  • ${pri.id} (${pri.title}) — ${pri.pointCost} Pts: ${pri.summary}")
    }
    sb.appendLine("- Secondary / Class Abilities (13 choices, including INTERDICTOR_DOME 25 Pts):")
    for (sec in CustomShipLoadoutSlot.SECONDARY_OPTIONS) {
      sb.appendLine("  • ${sec.id} (${sec.title}) — ${sec.pointCost} Pts [${sec.fluxCostLabel}]: ${sec.summary}")
    }
    sb.appendLine("- Rift Shards Economy (❖) & AI Hull Forge: Respec Slot = 50❖ Shards; Unlock Extra Loadout Frame (Frame B or Frame C) = 300❖ Shards; AI Hull Forge = 1st procedural hull synthesis per slot is FREE, subsequent re-designs cost 150❖ Shards (or 1 AI Design Token). Silhouettes: RAZOR_INTERCEPTOR, CRESCENT_SCYTHE, BASTION_DREADNOUGHT, MANTA_STINGRAY, X_VALKYRIE, PINCER_MANDIBLE.")

    val activeSlotId = localPrefs.getActiveCustomSlotId(currentUserId)
    val frameBUnlocked = localPrefs.isCustomSlotUnlocked("Frame B", currentUserId)
    val frameCUnlocked = localPrefs.isCustomSlotUnlocked("Frame C", currentUserId)
    for (slotId in listOf("Frame A", "Frame B", "Frame C")) {
      val slot = localPrefs.getCustomShipSlot(slotId, currentUserId)
      val unlockedState =
        when (slotId) {
          "Frame A" -> "UNLOCKED"
          "Frame B" -> if (frameBUnlocked) "UNLOCKED" else "LOCKED (300 Shards)"
          else -> if (frameCUnlocked) "UNLOCKED" else "LOCKED (300 Shards)"
        }
      val activeMarker = if (slotId == activeSlotId) " [ACTIVE]" else ""
      sb.appendLine(
        "  • Player Saved Loadout $slotId$activeMarker ($unlockedState) '${slot.customName}': ${slot.totalAllocatedPoints()} Pts allocated (Spd=${slot.speedPts}, Thr=${slot.thrustPts}, Turn=${slot.turnPts}, Flux=${slot.maxFluxPts}, Rech=${slot.rechargePts}, Pri=${slot.primaryArchetype}[${slot.primaryPts}P], Sec=${slot.secondaryType}[${slot.secondaryPts}P], Hull=${slot.visualRecipe.silhouette} '${slot.visualRecipe.promptUsed}')."
      )
    }

    // 5. OFFICIAL SUPPLY BUFF POOL + CUSTOM AI-FORGED BUFFS + 4 PLAYFIELD RIFTS
    sb.appendLine()
    sb.appendLine("[5. OFFICIAL SUPPLY BUFF POOL (BUCKET A & BUCKET B), AI-FORGED BUFFS & 4 PLAYFIELD RIFTS]")
    sb.appendLine(
      "- Supply Crate Cap in 20-Pilot FFA: ${engine.getEffectiveBuffCap()} crates active (${engine.greens.size} currently spawned on map). Repel Cost=${RiftCalibrationState.repelCost.toInt()}⚡, Radius=${RiftCalibrationState.repelRadius.toInt()}px, Push=${RiftCalibrationState.repelPushForce.toInt()} px/s."
    )
    for (entry in RiftCodex.POWERUP_CODEX) {
      sb.appendLine("  • [#${entry.buffTypeIndex}] ${entry.title} (${entry.dropRatePercent}% drop | ${entry.category}): ${entry.effectSummary} ${entry.tacticalNote}")
    }
    val customForged = RiftCodex.getCustomForgedBuffs()
    if (customForged.isEmpty()) {
      sb.appendLine("  • Custom AI-Forged Buffs in BUFF_REGISTRY: None currently forged (pilots can forge custom buffs in Calibration -> AI Buff & Mechanic Forge).")
    } else {
      for (cb in customForged) {
        sb.appendLine(
          "  • [CUSTOM FORGED #${cb.buffTypeIndex}] ${cb.officialName} (HUD: ${cb.shortLabel} [X]): ${cb.chargesPerPickup} charges, aim=${cb.aimMode.name}, effect=${cb.specialEffect.label}, speed=${cb.projectileSpeed.toInt()} px/s, duration=${cb.flightDuration}s, dmg=${cb.damage.toInt()}⚡, radius=${cb.blastRadius.toInt()}px, bounces=${cb.wallBounces}."
        )
      }
    }
    sb.appendLine(
      String.format(
        Locale.US,
        "- 4 Playfield Rifts (Spatial Anomalies — %.0fpx physical core radius = 5x 36px ship radius, %.0fs lifespan, %d currently active in arena):",
        RiftCalibrationState.riftPhysicalRadius * anomVisScale,
        RiftCalibrationState.riftLifespanSec,
        engine.anomalies.size,
      )
    )
    sb.appendLine(
      String.format(
        Locale.US,
        "  1) PLASMA RIFT (Crimson): Burns %.0f%% Max Flux per 0.6s tick on contact and can destroy ships if Flux drops <= 0⚡.",
        RiftCalibrationState.plasmaRiftDamagePct * 100f,
      )
    )
    sb.appendLine(
      String.format(
        Locale.US,
        "  2) REPULSOR RIFT (White/Emerald): Pushes ships & ordnance outward within %.0fpx radius at %.0f px/s².",
        RiftCalibrationState.repulsorFieldRadius,
        RiftCalibrationState.repulsorBaseForce,
      )
    )
    sb.appendLine(
      String.format(
        Locale.US,
        "  3) GRAVITY RIFT (Violet/Black Singularity): Pulls ships & projectiles inward within %.0fpx radius at %.0f px/s²; ships reaching dead-stop at the core gain %.2fs pull-escape immunity.",
        RiftCalibrationState.gravityPullRadius,
        RiftCalibrationState.gravityPullForce,
        RiftCalibrationState.gravityStopImmunitySec,
      )
    )
    sb.appendLine(
      String.format(
        Locale.US,
        "  4) PHASE RIFT A & B (Paired Teal/Cyan Gateways): Instantaneously warps ships, projectiles, and aim-guide raycasts between Node A and Node B while preserving momentum (%.2fs re-entry cooldown).",
        RiftCalibrationState.phaseRiftCooldownSec,
      )
    )

    // 6. HIGHEST BOUNTY EVER RECORDED (CLOUD GLOBAL LEADERBOARD + LOCAL DOSSIER + LIVE ARENA)
    sb.appendLine()
    sb.appendLine("[6. HIGHEST BOUNTY RECORDS (GLOBAL CLOUD LEADERBOARD, PILOT DOSSIER & LIVE SECTOR)]")

    val topCloudAllTime = cloudLeaderboard.maxByOrNull { it.allTimeBounty }
    val topCloudPvp = cloudLeaderboard.maxByOrNull { it.pvpBounty }
    val liveTopShip = engine.ships.maxByOrNull { it.bounty }

    // Determine single overall highest recorded bounty across Cloud Leaderboard, Local Dossier Peak, and Live Sector
    data class BountyRecordCandidate(val source: String, val callsign: String, val tag: String, val bounty: Long, val detail: String)
    val candidates = mutableListOf<BountyRecordCandidate>()
    if (topCloudAllTime != null) {
      val sName = RiftCodex.getShipById(topCloudAllTime.shipId.toInt()).name
      candidates +=
        BountyRecordCandidate(
          source = "Cloud Global All-Time Leaderboard",
          callsign = topCloudAllTime.callsign,
          tag = RiftGameEngine.cleanTag(topCloudAllTime.syndicateTag),
          bounty = topCloudAllTime.allTimeBounty,
          detail = "Ship $sName, ${topCloudAllTime.allTimeKills} All-Time Kills, ${topCloudAllTime.convergencesWon} Convergences Won",
        )
    }
    candidates +=
      BountyRecordCandidate(
        source = "Local Pilot All-Time Career Record",
        callsign = callsign,
        tag = syndicateTag,
        bounty = engine.allTimeBountyPeak,
        detail = "${engine.allTimeKills} All-Time Kills, ${engine.convergencesWon} Convergences Won",
      )
    if (liveTopShip != null) {
      candidates +=
        BountyRecordCandidate(
          source = "Current Live Arena Sector Leader",
          callsign = liveTopShip.callsign,
          tag = RiftGameEngine.cleanTag(liveTopShip.syndicateTag),
          bounty = liveTopShip.bounty.toLong(),
          detail = "Flying ${engine.formatShipLabel(liveTopShip)} (${if (liveTopShip.isHuman) "Human Pilot" else "AI Bot"})",
        )
    }
    val overallHighest = candidates.maxByOrNull { it.bounty }
    if (overallHighest != null) {
      sb.appendLine(
        "- OVERALL HIGHEST INDIVIDUAL BOUNTY RECORD KNOWN: [${overallHighest.tag}] ${overallHighest.callsign} with ${overallHighest.bounty} Bounty (${overallHighest.source} — ${overallHighest.detail})."
      )
    }
    if (topCloudPvp != null) {
      val sName = RiftCodex.getShipById(topCloudPvp.shipId.toInt()).name
      sb.appendLine(
        "- Highest Cloud PVP-Only Bounty Record: [${RiftGameEngine.cleanTag(topCloudPvp.syndicateTag)}] ${topCloudPvp.callsign} with ${topCloudPvp.pvpBounty} PVP Bounty (${topCloudPvp.pvpKills} PVP Kills, Ship $sName)."
      )
    } else {
      sb.appendLine(
        "- Cloud Leaderboard Status: ${cloudLeaderboard.size} cloud entries synced (Local PVP Peak = ${engine.pvpBountyPeak} Bounty by [$syndicateTag] $callsign)."
      )
    }
    if (cloudLeaderboard.isNotEmpty()) {
      sb.appendLine("- Top Cloud Global Leaderboard Entries (up to Top 10 by All-Time Bounty):")
      cloudLeaderboard
        .sortedByDescending { it.allTimeBounty }
        .take(10)
        .forEachIndexed { idx, entry ->
          val cleanT = RiftGameEngine.cleanTag(entry.syndicateTag)
          val shipTitle = RiftCodex.getShipById(entry.shipId.toInt()).name
          sb.appendLine(
            "  #${idx + 1}: [$cleanT] ${entry.callsign} — All-Time Peak: ${entry.allTimeBounty} BTY (${entry.allTimeKills}K/${entry.allTimeDeaths}D) | PVP Peak: ${entry.pvpBounty} BTY (${entry.pvpKills}K/${entry.pvpDeaths}D) | Ship: $shipTitle | Armada: ${entry.armadaTag} | Wins: ${entry.convergencesWon}"
          )
        }
    }

    // 7. LOCAL PILOT DOSSIER, PEAK BOUNTY SNAPSHOTS & EXECUTIONER TELEMETRY
    sb.appendLine()
    sb.appendLine("[7. LOCAL PILOT DOSSIER, PROGRESSION, EXECUTIONER SNAPSHOTS & SYNDICATES]")
    val p = engine.localPlayer
    val pSpeed = hypot(p.vx, p.vy).roundToInt()
    val secX = ((p.x / 1000f).toInt()).coerceIn(0, 7)
    val secY = ((p.y / 1000f).toInt()).coerceIn(0, 7)
    val sectorStr = "${('A' + secX)}${secY + 1}"
    val (curXp, spanXp, _) = engine.getXpProgressInCurrentLevel()
    sb.appendLine(
      "- Active Pilot: [$syndicateTag] $callsign (Syndicate: '$syndicateName', Lock Remaining: ${localPrefs.getRemainingSyndicateLockFormatted()}) | Armada: ${if (engine.activeArmadaName.isEmpty()) "SOLO (Operating Independently)" else "[${engine.activeArmadaName}] (${engine.getCurrentArmadaSize()}/5 • ${engine.getCurrentArmadaTierLabel()} — Friendly Fire OFF among Armada/Syndicate allies)"}."
    )
    sb.appendLine(
      "- Pilot Progression: Level ${engine.pilotLevel}/100 ($curXp / $spanXp XP in current level) | Customization Budget: ${engine.getCustomizationPointsTotal()} Pts | Rift Shards: ${engine.riftShards}❖ | XP Breakdown (5 Sources): Buffs=${engine.xpFromBuffs.toLong()}, Damage=${engine.xpFromDamage.toLong()}, Accuracy=${engine.xpFromAccuracy.toLong()}, BountyEarned=${engine.xpFromBountyEarned.toLong()}, BountyKilled=${engine.xpFromBountyKilled.toLong()}."
    )
    sb.appendLine(
      "- Career Combat Record: All-Time Peak Bounty=${engine.allTimeBountyPeak}, All-Time K/D=${engine.allTimeKills}/${engine.allTimeDeaths}, PVP Peak Bounty=${engine.pvpBountyPeak}, PVP K/D=${engine.pvpKills}/${engine.pvpDeaths}, HVT Eliminations=${engine.hvtEliminations}, Supply Crates=${engine.greensCollected}, Sector Convergences Won=${engine.convergencesWon}."
    )
    sb.appendLine(
      "- Live Ship Right Now: Flying ${engine.formatShipLabel(p)} in Sector $sectorStr (${p.x.toInt()}, ${p.y.toInt()}), Speed=$pSpeed px/s, Flux=${p.flux.toInt()}/${p.maxFlux.toInt()}⚡, Current Bounty=${p.bounty}, Repels=${p.repels}, SpecialSlot=${p.specialWeaponType}(${p.specialWeaponCharges}), MultiShotCrateShots=${p.multiShotCharges}, BouncingBulletsUpgrade=${p.hasBouncingBulletsUpgrade}, SpeedOvercharge=${p.speedOverchargeStacks}/3, ThrustOvercharge=${p.thrustOverchargeStacks}/3, ReactorOvercharge=${p.rechargeOverchargeStacks}/3."
    )

    fun formatSnapshot(label: String, snap: PeakBountyRunSnapshot?) {
      if (snap == null) {
        sb.appendLine("- $label: No completed run snapshot recorded yet.")
        return
      }
      val exec = snap.executioner
      val execStr =
        when {
          exec.survivedToConvergence -> "SURVIVED TO 1-HOUR SECTOR CONVERGENCE"
          exec.killerCallsign.isNotBlank() ->
            "Terminated by [${exec.killerSyndicateTag}] ${exec.killerCallsign} flying ${exec.killerShip} using ${exec.killingWeapon} (Killer Bounty at kill: ${exec.killerBountyAtKill}, Bounty earned from you: +${exec.bountyEarnedFromYou})"
          else -> "Run still active or unrecorded termination"
        }
      sb.appendLine(
        "- $label: Peak Bounty=${snap.bountyReached} in ${snap.shipFlown}, Survived=${snap.formattedDuration()}, Kills=${snap.pvpKills} PVP / ${snap.botKills} Bot (${snap.hvtShutdowns} HVT shutdowns), Damage=${snap.damageDealt.toInt()}⚡ dealt / ${snap.damageTaken.toInt()}⚡ taken, Accuracy=${snap.accuracyPercent()}% (${snap.shotsHit}/${snap.shotsFired}), Buffs=${snap.buffsCollected} | Executioner: $execStr."
      )
    }
    formatSnapshot("All-Time Peak Bounty Run Snapshot", allTimePeakSnapshot)
    formatSnapshot("PVP-Only Peak Bounty Run Snapshot", pvpPeakSnapshot)

    if (syndicateHistory.isNotEmpty()) {
      val histSummary =
        syndicateHistory.joinToString(" ; ") { h ->
          "[${h.syndicateTag}] ${h.syndicateName} (${h.role}, ${if (h.isActive) "ACTIVE" else "PAST"}, Tenure=${h.formatTenure()}, Kills=${h.totalKills}, PeakBty=${h.peakBountyAchieved}, Wins=${h.publicGameWins})"
        }
      sb.appendLine("- Pilot Syndicate Service History: $histSummary")
    }
    val syndicatesDir =
      localPrefs.getAllSyndicates().joinToString(", ") { s ->
        "[${s.tag}] ${s.name} (${s.memberCount} pilots, ${s.combinedBounty} combined syndicate bounty)"
      }
    sb.appendLine("- Known Sector Syndicates Directory (15-day switch cooldown lock): $syndicatesDir")

    // 8. LIVE SECTOR ROSTER & ACTIVE CALIBRATION OVERRIDES
    sb.appendLine()
    sb.appendLine("[8. LIVE ARENA SECTOR TOP PILOTS & CALIBRATION OVERRIDES]")
    val topLiveShips = engine.ships.sortedByDescending { it.bounty }.take(8)
    sb.appendLine("- Active Ships in Sector (${engine.ships.size} total; HVT >= ${RiftCalibrationState.hvtBountyThreshold} BTY):")
    for ((idx, s) in topLiveShips.withIndex()) {
      val sTag = if (s.armadaName.isNotEmpty()) "[${s.armadaName}]" else "[${RiftGameEngine.cleanTag(s.syndicateTag)}]"
      val hvtFlag = if (s.bounty >= RiftCalibrationState.hvtBountyThreshold) " [👑 HVT]" else ""
      sb.appendLine(
        "  #${idx + 1}: $sTag ${s.callsign} (${if (s.isLocalPlayer) "YOU" else if (s.isHuman) "HUMAN" else "AI BOT"}) — ${s.bounty} BTY$hvtFlag | ${engine.formatShipLabel(s)} | ${s.flux.toInt()}/${s.maxFlux.toInt()}⚡"
      )
    }

    val modifiedCalibrations =
      RiftCalibrationState.buildAllParameterDescriptors().mapNotNull { desc ->
        val cur = desc.getter()
        val def = RiftCalibrationState.DEFAULT_GAME_CONFIG[desc.key]
        if (def != null && abs(cur - def) > 0.0005f) {
          "${desc.title} (${desc.key}): default ${desc.formatValue(def)} -> live ${desc.formatValue(cur)}"
        } else {
          null
        }
      }
    if (modifiedCalibrations.isEmpty()) {
      sb.appendLine("- Live Calibration Status: All sliders currently at factory defaults.")
    } else {
      sb.appendLine("- Live Calibration Overrides Active (${modifiedCalibrations.size}): ${modifiedCalibrations.joinToString(" | ")}")
    }

    return sb.toString()
  }
}
