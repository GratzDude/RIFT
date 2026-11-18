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
 * Dynamically compiles the complete, authoritative NEXUS (Threshold Dynamics Studio, LLC) Engine,
 * AGSL Shader & Pixel-Rendering, Unified #060D1A Sci-Fi Command Tablet Hub & Pinch-to-Zoom Overwatch,
 * 5x DS9 Metallic Safe-Zone Station, 100-Point Ship Math, Rift Conduit Resonance, Daily Directives,
 * 12-Buff Supply Registry, 440px Gravity Rift Implosion, Unstable Rift Convergence, Crystal Asteroids,
 * Station Novas, Calibration, Pilot Dossier, and Cloud Leaderboard Ground-Truth Snapshot for `#nexus-ai`.
 */
object RiftNexusKnowledge {

  const val NEXUS_SYSTEM_INSTRUCTION: String =
    """
You are NEXUS, the omniscient tactical intelligence stationed inside the `CHAT & NEXUS AI` (`#nexus-ai` channel) module of the `#060D1A` Sci-Fi Command Tablet in NEXUS (developed by Threshold Dynamics Studio, LLC), a mobile-first competitive 120Hz 2D Newtonian space arena game on Android.

CRITICAL DOMAIN & KNOWLEDGE BOUNDARY (NON-NEGOTIABLE):
1. EXCLUSIVE NEXUS & RIFT-SECTOR KNOWLEDGE BASE: Your ONLY knowledge base is NEXUS itself — its Newtonian physics, GPU AGSL shaders & pixel-level rendering math, 10 ship classes (all 10 ships unlocked from 0 Resonance: Viper, Ricochet, Suppressor, Goliath, Twinfang, Disruptor, Valkyrie, Manta, Eclipse, and modular custom ship Rift Frame at full 100-Point parity + Frame A through Frame E), Rift Conduit Live Resonance & Peak Resonance (scaling Starting Bounty from 100 to 500 Max + 3 Daily Directives paying up to 250❖/day, plus 999,999❖ non-depleting Tester Shards while commercial Play Billing is dormant), 12-item Supply Buff pool (including 5-drone zig-zagging SWARM and N-stacking Stasis Barriers) + custom forged buffs, Playfield Rifts (Plasma Rift, Repulsor Rift with supersonic inner-core slingshot, Gravity Rift with swept 0.92x visible-disc weapon absorption — +18px per primary bolt, +40px per bomb/torpedo, +14px per drone/phantom up to 440px max with crackling rim lightning — and >= 260px swollen mass dragging Crystal Asteroids, Orbital/Asteroid Stations, Debris, and Stasis Barriers into the core at 950/s tidal crush damage -> implosion + Supply Crate + 20.0s respawn + 16-bolt 360-degree Overload Nova on Stations, Paired Phase Rifts A & B, and live 10.0s Unstable Rift convergence dealing 75% Max Flux Doomsday detonation + shattering Crystal Asteroids), destructible 2,400 Flux Crystal Asteroids (guaranteed Supply Crate drop + 20.0s respawn), 10-hit Orbital/Asteroid Station 16-bolt 360-degree Defense Novas, wall-less circular Safe-Zone Harbor anchored by a 5x ship-scale (180px world radius) DS9 Metallic Station sprite with a revolving cyan/gold Ops Holo-Ring & radial stabilizer beams editable live in `STAR FORGE` (active ONLY in Sector Convergence and Flux Core with 15s max stay / weapon-lockout; disabled in Rift Control, Stasis Breach, Rift Skirmish, and Syndicate Wars, alongside Open-Space Anti-AFK), Rippling 3D Rift-Cloud Storm Boundary + 220px proximity arc lightning, the unified `#060D1A` Sci-Fi Command Tablet `NEXUS` Hub (`SHIPS & BUILDS`, `RANKS & QUESTS`, `SKINS & AUDIO`, `CHAT & NEXUS AI`, `CLANS & SQUADS`, `STATS & CODEX` + Top Bar `[? HOW TO PLAY]`, `[HELM SYSTEMS]`, `[PRACTICE RIFT >]`) framing a Round Rippling Dynamic-Lit Pinch-to-Zoom (`1.0x..6.0x`) & Pan Orbital Overwatch aperture auto-selecting `[FAV]` or Highest Combined Pilot Resonance (`Game Type • RIFT #XX`) + `[QUICK ZONE IN]` & `[ALL MODES]`, Hangar Swirling NEXUS hull lock-in & `APEX` halo, all 6 Official Ranked Modes, Custom Rifts (`speedMult` & `damageMult` missile/ordnance scaling + `MegaFat Bullets` + `100` custom starting bounty), Syndicate War Trophies, Pilot Dossier & Executioner telemetry, and Sworn Pilot Cloud Leaderboard records.
2. REFUSE NON-NEXUS TOPICS IN CHARACTER: If the user asks about anything outside NEXUS, briefly decline in character: "NEXUS CORE TELEMETRY BOUNDARY // Query falls outside NEXUS sector parameters. Submit a query regarding NEXUS ships, pixel/physics telemetry, custom builds, Rift Conduit Resonance, Playfield Rifts, 5x DS9 Safe-Zone Station, Hub Overwatch, Ranked Modes, buffs, or bounty records."
3. PINPOINT NUMERICAL & PIXEL PRECISION: Cite exact live engine numbers, world-pixel dimensions, DPS, Flux costs (HP + weapon energy, floored at 1 Flux on self-drain), Rift Shards (❖) economy, Live & Peak Resonance, custom frames (`Frame A-E`), Eclipse two-way `Stasis` (6.0s duration, 275px radius, 320 Flux cost, 8.5s CD), Gravity Rift 440px swept growth & >= 260px 950/s tidal implosion, Unstable Rift 10.0s timer & 75% Max Flux Doomsday blast, 5x (180px) DS9 Safe-Zone Station, Ranked Mode telemetry, Calibration overrides, and Sworn Pilot Leaderboard records. Never use decorative emojis or literal star icons; write "Flux" for ship energy/HP and ❖ strictly for Rift Shards.
4. ANDROID TOUCH HUD & TWIN-STICK CONTROLLER TERMINOLOGY ONLY: Support native Android Landscape Touch HUD (Left Movement Stick, Manual [BRAKE], Right Primary Fire Stick, Secondary, Special Buff, Repel, Native Multi-Shot, Collapsible Living-Teammate Spectator Box, Graphical Dual-Rift Tug-of-War Top Bar in Rift Control, and Bottom Console with 3D AGSL Plasma Flux Ring wrapping the 1:1 circular minimap) AND Bluetooth/USB Twin-Stick Controllers (including LB/RB teammate spectator cycling). Never mention PC keyboard shortcuts.
5. PURE TEXT Q&A & THEORYCRAFTING: Deliver clear, structured tactical answers and 100-point Rift Frame theorycrafting breakdowns (all 10 ships and full 100-point parity open from 0 Resonance).
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
    val sb = StringBuilder(17_000)
    val camScale = engine.effectiveCameraScale()
    val projScale = RiftCalibrationState.projectileScale
    val shipVisScale = RiftCalibrationState.shipScale
    val crateVisScale = RiftCalibrationState.crateScale
    val anomVisScale = RiftCalibrationState.anomalyScale
    val customSpdMult = engine.currentCustomGame?.speedMult ?: 1.0f
    val customDmgMult = engine.currentCustomGame?.damageMult ?: 1.0f

    sb.appendLine("=== [LIVE NEXUS GROUND-TRUTH TELEMETRY SNAPSHOT — BUILD #72] ===")

    sb.appendLine()
    sb.appendLine("[1. SCI-FI COMMAND TABLET HUB, OVERWATCH, 5X DS9 SAFE HARBOR & PIXEL/SHADER SPECS]")
    sb.appendLine(
      "- NEXUS Sci-Fi Command Tablet Hub: #060D1A tablet shell with 6 192x192 5-stop metallic station-styled module cards — Left Wing: SHIPS & BUILDS (HANGAR DECK), RANKS & QUESTS (RIFT CONDUIT), SKINS & AUDIO (STAR FORGE); Right Wing: CHAT & NEXUS AI (SUBSPACE COMMS), CLANS & SQUADS (SYNDICATE CITADEL), STATS & CODEX (ARCHIVE & CODEX) — framing a center Round Rippling Dynamic-Lit Digitized Aperture with live 1.0x..6.0x 2-finger pinch-to-zoom & 1-finger pan Orbital Overwatch (world nametags hidden below 1.55x zoom for a clean radar view), auto-selecting [FAV] or the active game with Highest Combined Pilot Resonance (Game Type • RIFT #XX) + 1-tap [QUICK ZONE IN] and [ALL MODES]."
    )
    sb.appendLine(
      "- World Arena & Rippling Rift-Cloud Storm Wall: 8000 x 8000 world pixels (8x8 sectors A1..H8, each sector = 1000 x 1000 world px; circular perimeter radius = ${engine.arenaRadius.toInt()} world px). Outer boundary renders a seamless GPU AGSL 3D Rift-Cloud Storm Wall bleeding 260px inward with live branching proximity arc lightning striking ships within 220px of the perimeter."
    )
    sb.appendLine(
      "- Wall-Less Circular Safe-Zone Harbor & 5x DS9 Metallic Station: Safe-Zone radius = ${engine.getSafeZoneRadius().toInt()} world px centered at (${engine.arenaCenterX.toInt()}, ${engine.arenaCenterY.toInt()}), anchored by a 5x ship-scale (180px world radius) Deep Space Nine (DS9) metallic station sprite with a shrunk revolving cyan/gold Ops Holo-Ring and radial stabilizer beams (editable live in STAR FORGE). Active ONLY in SECTOR CONVERGENCE and FLUX CORE (live hasActiveSafeZone() = ${engine.hasActiveSafeZone()}, 15s max stay / weapon-lockout); completely disabled in RIFT CONTROL, STASIS BREACH, RIFT SKIRMISH, and SYNDICATE WARS. Open-Space Anti-AFK Flux drain is likewise restricted strictly to SECTOR CONVERGENCE and FLUX CORE."
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
        "- Ship Hull Rendering & Hitbox: Offscreen procedural 3D metallic bitmap = 128x128 px; physical collision radius = 36.0 world px (72.0 world px diameter); live world render quad = %.2f x %.2f world px (%.2f screen px at live zoom).",
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
        "- Secondary Warhead & Ability Specs (scaled by projectileScale %.2fx, Custom speedMult %.2fx, Custom damageMult %.2fx): Viper Phase Dash = 135 world px spatial blink + %.0f px/s dodge burst (320 Flux cost, 0.65s CD, +%.0f%% next-shot rail overcharge); Siege Bomb = %.1f world px radius; Bounce & Prox Flak Bomb = %.1f world px radius (Prox Flak fuse = %.0f world px); Disruptor Cloak & 360-deg EMP = 20 Flux entry, %.0f Flux/s cloak drain (50%% cloaked afterburner drain, 4.5s CD, 150 Flux safety floor), 0 Flux manual decloak %.0f world px EMP draining %.0f%% Max Flux (capped at 850 Flux) + %.1fs recharge freeze; Proximity Mine = %.1f world px radius (max %d mines, 30s life); Fast Predictive Seeker Torpedo = %.1f world px radius (%.0f px/s cruise * speedMult, proportional intercept lead, %.1fs flight); Stackable Stasis Barrier = %.0f world px Tethered Cyan Hard-Light Forcefield (%.1fs duration, deploys within 78px stack as BARRIER xN); SWARM = 5 autonomous zig-zagging hunter drones (210 Flux dmg * damageMult each, 560 px/s * speedMult, 6.5s life); Eclipse Stasis (INTERDICTOR_DOME) = %.0f world px radius (%.0f world px diameter) two-way Stasis active for %.1fs (320 Flux cost, %.1fs CD).",
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
        "- Destructible Crystal Asteroids & 10-Hit Station Defense Novas: Crystal Asteroids have 2,400 Flux integrity with an on-rock circular integrity ring; shattering one drops a guaranteed Supply Crate and schedules a 20.0s respawn (%d pending respawns). Orbital & Asteroid Stations track weapon impacts with a 10-segment charge ring and unleash a 16-bolt 360-degree Station Defense Nova every 10 hits (or when imploded by a >= 260px Gravity Rift, damaging all frequencies). Supply Crate world size = %.2f world px; 3D AGSL Plasma Flux Ring wraps the 1:1 arenaRadius Circular Minimap.",
        engine.pendingAsteroidRespawns.size,
        78.32f * crateVisScale,
      )
    )

    sb.appendLine()
    sb.appendLine("[2. RIFT CONDUIT RESONANCE, STARTING BOUNTY (100-500) & RANKED/CUSTOM MODES]")
    val liveRes = engine.liveResonance
    val peakRes = engine.peakResonance
    val conduitTier = RiftProgressionCatalog.getConduitTierForResonance(liveRes)
    val startBty = engine.getLocalStartingBounty()
    sb.appendLine(
      "- Rift Conduit Resonance: Live Resonance = $liveRes (${conduitTier.title}) | Peak Resonance = $peakRes | Effective Starting Bounty = $startBty (Ranked scales 100 -> 150 -> 225 -> 300 -> 400 -> 500 Max across Drift Initiate, Vanguard, Interceptor, Phantom, Rift Commander, and Apex Archon; Custom Rifts standardize starting bounty to 100)."
    )
    val directives = localPrefs.getDailyDirectives(currentUserId)
    val directivesSummary =
      directives.joinToString(" | ") { d ->
        "${d.title}: ${d.progressValue}/${d.targetValue} (${d.shardReward}❖ +${d.resonanceReward} RES, ${if (d.claimed) "CLAIMED" else if (d.isComplete) "READY" else "ACTIVE"})"
      }
    sb.appendLine("- 3 Daily Conduit Directives (up to 250❖/day including +100❖ 3/3 Sweep Chest): $directivesSummary")
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
    val matchRemainingSec = (engine.publicMatchCycleLimitSec - engine.publicMatchDurationSec).coerceAtLeast(0f)
    sb.appendLine(
      String.format(
        Locale.US,
        "- Public FFA Sector Convergence & Bounty Math: %.0f-minute cycle (%.0fs remaining). Local pilot spawns at %d Starting Bounty; HVT crown at >= %d Bounty; killing awards +%.0f%% victim Bounty; Supply Crates +%d Bounty. Winning with >50%% presence awards +100❖ Rift Shards.",
        engine.publicMatchCycleLimitSec / 60f,
        matchRemainingSec,
        startBty,
        RiftCalibrationState.hvtBountyThreshold,
        RiftCalibrationState.bountyKillBonusPct * 100f,
        RiftCalibrationState.cratePickupBounty,
      )
    )
    sb.appendLine("- 6 Official Ranked Modes + Cloneable Custom Rifts (Hub [QUICK ZONE IN] or [ALL MODES] -> Hangar Hull Lock-In -> Enter Rift):")
    sb.appendLine("  - 1) SECTOR CONVERGENCE: 20-Pilot Ranked FFA / 1-5 Strike Wing Bounty | 5x DS9 Safe-Zone Harbor ON | +100❖.")
    sb.appendLine("  - 2) RIFT SKIRMISH: Ranked 2v2-5v5 Best-of-3 Rounds Team Deathmatch | Safe Zone OFF | Ship Lock ON | Teammate Spectator | +75❖.")
    sb.appendLine(
      String.format(
        Locale.US,
        "  - 3) FLUX CORE: 20-Pilot Ranked Powerball FFA | 5x DS9 Safe-Zone Harbor ON | Tier 1-10 Overcharge (+15 Bounty/s; 2x pulsing Gold radar beacon) | Ejects at %.0f px/s on death or Safe Zone entry.",
        RiftCalibrationState.fluxCoreEjectSpeed,
      )
    )
    sb.appendLine(
      String.format(
        Locale.US,
        "  - 4) STASIS BREACH: Ranked 10v10 Circular Stasis Jailbreak | Safe Zone OFF | Prisoners inside deal 100%% weapon damage to the %.0f Flux Stasis Core to detonate a radial shockwave and break everyone out | +100❖ (Live Cores: West=%.0f Flux, East=%.0f Flux).",
        RiftCalibrationState.stasisBrigReactorMaxHp,
        engine.westBrigGeneratorHp,
        engine.eastBrigGeneratorHp,
      )
    )
    sb.appendLine("  - 5) RIFT CONTROL: Ranked Tug-of-War over Rift Alpha & Rift Omega | Safe Zone OFF | Graphical Dual-Rift Tug-of-War Top HUD Bar | +85❖.")
    val myWarRec = localPrefs.getSyndicateWarRecord(syndicateTag)
    sb.appendLine(
      "  - 6) SYNDICATE WARS: Ranked Direct Syndicate-vs-Syndicate Ladder | Safe Zone OFF | Ship Lock ON | Requires a Sworn Pilot Syndicate and 100% same-Syndicate Strike Wing | Ranked by Syndicate War Trophies (Active Syndicate [$syndicateTag] Record: ${myWarRec.warTrophies} War Trophies, ${myWarRec.warWins}W-${myWarRec.warLosses}L, Streak ${myWarRec.warStreak})."
    )
    sb.appendLine(
      "  - Active Ranked Mode Right Now: ${engine.activeRankedMode.title} (${engine.activeRankedMode.id}, Bracket=${engine.activeTeamSizeBracket}, Human Pilots in Mode=${engine.getHumanPilotCountForMode(engine.activeRankedMode)}, ShipLocked=${engine.doesModeLockShipSelection()})."
    )

    sb.appendLine()
    sb.appendLine("[3. COMPLETE 10-SHIP ROSTER — ALL 10 OPEN AT 0 RESONANCE (100-POINT PARITY)]")
    for (spec in RiftCodex.getAllTenShips()) {
      val shaftWorldPx = spec.primarySpeed * 0.036f
      val outerWidthWorldPx = 6.4f * projScale
      val totalWorldLenPx = shaftWorldPx + outerWidthWorldPx
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
    sb.appendLine("[4. RIFT FRAME 100-POINT PARITY (OPEN AT 0 RESONANCE) & SAVED FRAMES A-E]")
    sb.appendLine("- All 10 ships including Rift Frame (100/100 Customization Points + Frame A) are open immediately at 0 Resonance. Peak Resonance unlocks 3D Hull Trims, Kill Finishers, Warp Signatures, HVT Crown Halos, and extra Loadout Bays Frame B-E.")
    val activeSlotId = localPrefs.getActiveCustomSlotId(currentUserId)
    for (slotId in CustomShipLoadoutSlot.ALL_SLOT_IDS) {
      val slot = localPrefs.getCustomShipSlot(slotId, currentUserId)
      val unlocked = slotId == "Frame A" || localPrefs.isCustomSlotUnlocked(slotId, currentUserId)
      val activeMarker = if (slotId == activeSlotId) " [ACTIVE]" else ""
      sb.appendLine(
        "  - $slotId$activeMarker (${if (unlocked) "UNLOCKED" else "LOCKED"}) '${slot.customName}': ${slot.totalAllocatedPoints()} Pts (Spd=${slot.speedPts}, Thr=${slot.thrustPts}, Turn=${slot.turnPts}, Flux=${slot.maxFluxPts}, Rech=${slot.rechargePts}, Pri=${slot.primaryArchetype}, Sec=${slot.secondaryType})."
      )
    }

    sb.appendLine()
    sb.appendLine("[5. 12-BUFF SUPPLY POOL, FORGED BUFFS & PLAYFIELD RIFTS (440PX GRAVITY IMPLOSION & UNSTABLE RIFT)]")
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
        "- Playfield Rifts (%.0fpx base physical core radius, %.0fs lifespan, %d active, %d Unstable Merged): 1) PLASMA RIFT (%.0f%% Max Flux burn/0.6s), 2) REPULSOR RIFT (%.0fpx deflection + inner-core 1.45x afterburner supersonic slingshot), 3) GRAVITY RIFT (%.0fpx pull; swept 0.92x visible-disc weapon absorption grows radius +18px/bolt, +40px/bomb-torpedo, +14px/drone-phantom up to 440px max with crackling rim lightning; at >= 260px swollen mass drags Crystal Asteroids, Orbital/Asteroid Stations, Debris, and Stasis Barriers into the core at 950/s tidal crush -> implosion + Supply Crate + 20.0s respawn + 16-bolt 360-deg Station Overload Nova), 4) PAIRED PHASE RIFTS (Phase Rift A & B instantaneous gateway, %.2fs re-entry CD), 5) UNSTABLE RIFT (overlapping Rifts merge into a 10.0s convergence singularity that detonates for 75%% Max Flux Doomsday blast and shatters nearby Crystal Asteroids).",
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
        val t = RiftGameEngine.cleanTag(it.syndicateTag)
        t != "BOT" && t != "RFT" && t != "GUEST" && !it.callsign.contains("[BOT]") && !it.userId.startsWith("dev_")
      }
    val topCloudAllTime = realCloudLeaderboard.maxByOrNull { it.allTimeBounty }
    if (topCloudAllTime != null) {
      sb.appendLine(
        "- Top Cloud Sworn Pilot All-Time Bounty: [${RiftGameEngine.cleanTag(topCloudAllTime.syndicateTag)}] ${topCloudAllTime.callsign} with ${topCloudAllTime.allTimeBounty} Bounty (${topCloudAllTime.allTimeKills}K/${topCloudAllTime.allTimeDeaths}D)."
      )
    } else {
      sb.appendLine("- Cloud Leaderboard Status: 0 Sworn Pilot records synced yet (AI Bots and [RFT] Drifters are strictly excluded).")
    }

    val p = engine.localPlayer
    val pSpeed = hypot(p.vx, p.vy).roundToInt()
    val secX = ((p.x / 1000f).toInt()).coerceIn(0, 7)
    val secY = ((p.y / 1000f).toInt()).coerceIn(0, 7)
    val sectorStr = "${('A' + secX)}${secY + 1}"
    sb.appendLine(
      "- Active Pilot: [$syndicateTag] $callsign (Syndicate: '$syndicateName') | Live Resonance: $liveRes (${conduitTier.title}) | Peak Resonance: $peakRes | Starting Bounty: $startBty | Shards: ${engine.riftShards}❖ | Live Hull: ${engine.formatShipLabel(p)} in Sector $sectorStr, Speed=$pSpeed px/s, Flux=${p.flux.toInt()}/${p.maxFlux.toInt()} Flux, Bounty=${p.bounty}."
    )

    fun formatSnapshot(label: String, snap: PeakBountyRunSnapshot?) {
      if (snap == null) return
      sb.appendLine(
        "- $label: Peak Bounty=${snap.bountyReached} in ${snap.shipFlown}, Survived=${snap.formattedDuration()}, Kills=${snap.pvpKills} PVP / ${snap.botKills} Bot, Damage=${snap.damageDealt.toInt()} Flux dealt / ${snap.damageTaken.toInt()} Flux taken."
      )
    }
    formatSnapshot("All-Time Peak Bounty Run", allTimePeakSnapshot)
    formatSnapshot("PVP Peak Bounty Run", pvpPeakSnapshot)

    if (syndicateHistory.isNotEmpty()) {
      sb.appendLine(
        "- Syndicate Service History: " +
          syndicateHistory.joinToString(" ; ") { "[${it.syndicateTag}] ${it.syndicateName} (${it.totalKills}K, ${it.publicGameWins}W)" }
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
      sb.appendLine("- Live Calibration Overrides (${modifiedCalibrations.size}): ${modifiedCalibrations.joinToString(" | ")}")
    }

    return sb.toString()
  }
}
