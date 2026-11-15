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
 * Ground-Truth Snapshot for NEXUS (100% On-Device Tactical Intelligence).
 */
object RiftNexusKnowledge {

  const val NEXUS_SYSTEM_INSTRUCTION: String =
    """
You are NEXUS, the omniscient Rift Core tactical intelligence embedded inside the **NEXUS Command Hub (`#nexus-ai` channel)** of RIFT, a mobile-first competitive 2D Newtonian space arena game on Android.

CRITICAL DOMAIN & KNOWLEDGE BOUNDARY (NON-NEGOTIABLE):
1. EXCLUSIVE RIFT-ONLY KNOWLEDGE BASE: Your ONLY knowledge base is RIFT itself — its physics, pixel-level rendering math, 10 ship classes (9 standard hulls: Viper, Ricochet, Suppressor, Goliath, Twinfang, Disruptor, Valkyrie, Manta, and Eclipse + modular custom ship: Rift Frame), 100-point Rift Frame modular builder & AI Hull Forge (`Frame A` through `Frame E`), Supply Buff pool + custom AI-forged buffs, 4 Playfield Rifts (Plasma Rift, Repulsor Rift, Gravity Rift, and Paired Phase Rifts — Node A & B), Safe Zone & AFK rules (with NO Safe Zone in Rift Control), Calibration parameters, the persistent 9-tab NEXUS Master Window (`COMMAND & LOBBY`, `HANGAR`, `PROGRESSION`, `ARMADA FLEET`, `SYNDICATES`, `DOSSIER`, `CODEX`, `SETTINGS`, `CALIBRATE`), all 6 Official Ranked Modes (`SECTOR CONVERGENCE`, `RIFT SKIRMISH`, `FLUX CORE`, `STASIS BREACH`, `RIFT CONTROL`, and `SYNDICATE WARS` with strict human-only pilot counts and ship lock strictly on `RIFT_SKIRMISH` and `SYNDICATE_WARS`), Syndicate War Trophies, Pilot Dossier & Executioner telemetry, and Sworn Pilot Cloud Leaderboard bounty records.
2. REFUSE NON-RIFT TOPICS IN CHARACTER: If the user asks about anything outside RIFT, briefly decline in character: "RIFT CORE TELEMETRY BOUNDARY // Query falls outside RIFT sector parameters. Submit a query regarding RIFT ships, pixel/physics telemetry, custom builds, Ranked Modes, buffs, or bounty records."
3. PINPOINT NUMERICAL & PIXEL PRECISION: Cite exact live engine numbers, world-pixel dimensions, DPS, Flux costs (HP + weapon energy, floored at 1 Flux on self-drain), Rift Shards (❖) economy, custom frames (`Frame A–E`), Ranked Mode telemetry, Calibration overrides, and Sworn Pilot Leaderboard records. Never use the lightning emoji; write "Flux" for ship energy/HP and ❖ strictly for Rift Shards.
4. ANDROID TOUCH HUD & TWIN-STICK CONTROLLER TERMINOLOGY ONLY: Support native Android Landscape Touch HUD (Left Movement Stick, Manual [BRAKE], Right Primary Fire Stick, Secondary, Special Buff, Repel, Native Multi-Shot, Interactive Living-Teammate Spectator Selector, Graphical Dual-Rift Tug-of-War Top Bar in Rift Control, Top LVL + XP badge, and Bottom Console with circular minimap) AND Bluetooth/USB Twin-Stick Controllers (including LB/RB teammate spectator cycling). Never mention PC keyboard shortcuts.
5. PURE TEXT Q&A & THEORYCRAFTING: Deliver clear, structured tactical answers and 100-point Rift Frame theorycrafting breakdowns (full 100-point parity unlocked at Pilot Level 20).
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

    sb.appendLine()
    sb.appendLine("[1. EXACT PIXEL, SPRITE & ARENA RENDERING SPECIFICATIONS]")
    sb.appendLine("- World Arena Size: 8000 x 8000 world pixels (8x8 sectors A1..H8, each sector = 1000 x 1000 world px; circular perimeter radius = ${engine.arenaRadius.toInt()} world px).")
    sb.appendLine("- Central Safe Zone Rect: x=[3600..4400], y=[3600..4400] (800 x 800 world px centered at (4000, 4000), rendered with Tethered Cyan Hard-Light Forcefields; completely disabled in RIFT CONTROL where hasActiveSafeZone() == false).")
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
        "- Primary Laser / Bolt Exact Pixel Formula: Shaft Vector Length = primarySpeed * 0.036 world px; Outer Colored Glow Width = %.2f world px; Inner White Core Width = %.2f world px.",
        6.4f * projScale,
        2.8f * projScale,
      )
    )
    sb.appendLine(
      String.format(
        Locale.US,
        "- Secondary Warhead & Ability Specs (scaled by projectileScale %.2fx): Viper Phase Dash = 135 world px spatial blink + %.0f px/s dodge burst (300 Flux cost, 2.5s CD, +%.0f%% next-shot rail overcharge); Siege Bomb = %.1f world px radius; Bounce & Prox Flak Bomb = %.1f world px radius (Prox Flak fuse = %.0f world px); Disruptor Cloak & 360° EMP = 20 Flux entry, %.0f Flux/s cloak drain (50%% cloaked afterburner drain, 4.5s CD), 0 Flux manual decloak %.0f world px EMP draining %.0f%% Max Flux (capped at 850 Flux) + %.1fs recharge freeze (forced uncloak <=150 Flux or damage skips EMP); Proximity Mine = %.1f world px radius (max %d mines, 30s life); Seeker Torpedo = %.1f world px radius (%.0f px/s, %.1fs flight); Stasis Barrier = %.0f world px Tethered Cyan Hard-Light Forcefield (%.1fs duration); Eclipse Stasis Bubble (INTERDICTOR_DOME) = %.0f world px radius (%.0f world px diameter) two-way sphere active for %.1fs (320 Flux cost, %.1fs CD).",
        projScale,
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
        "- Supply Crates & Interior Obstacles: Supply Crate world size = %.2f world px; Interior Obstacles = sleek pitch-black #040914 obsidian/reactor & 3D station geometry; Circular Radar Minimap aligns 1:1 with arenaRadius, highlights the Flux Core carrier at 2x pulsing Gold (#FFD700), and renders above the Buff Bar when expanded.",
        78.32f * crateVisScale,
      )
    )

    sb.appendLine()
    sb.appendLine("[2. NEWTONIAN PHYSICS, UNIFIED FLUX, SPECTATOR & RANKED MODES]")
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
    sb.appendLine(
      String.format(
        Locale.US,
        "- Collisions & Unified Flux (Flux = HP + Weapon Energy): Wall bounce elasticity = %.2f with zero wall damage. Self-drain floors at 1 Flux. Enemy damage <= 0 Flux destroys the ship (%.1fs respawn timer; interactive living-teammate spectator box in team modes).",
        RiftCalibrationState.wallElasticity,
        RiftCalibrationState.respawnDelaySec,
      )
    )
    val matchRemainingSec = (engine.publicMatchCycleLimitSec - engine.publicMatchDurationSec).coerceAtLeast(0f)
    sb.appendLine(
      String.format(
        Locale.US,
        "- Public FFA Sector Convergence & Bounty Math: %.0f-minute cycle (%.0fs remaining). Ships spawn at 100 Bounty; HVT crown at >= %d Bounty; killing awards +%.0f%% victim Bounty; Supply Crates +%d Bounty. Winning with >50%% presence awards +100❖ Rift Shards.",
        engine.publicMatchCycleLimitSec / 60f,
        matchRemainingSec,
        RiftCalibrationState.hvtBountyThreshold,
        RiftCalibrationState.bountyKillBonusPct * 100f,
        RiftCalibrationState.cratePickupBounty,
      )
    )
    sb.appendLine("- 6 Official Ranked Modes (Ship selection locked strictly in RIFT SKIRMISH & SYNDICATE WARS):")
    sb.appendLine("  • 1) SECTOR CONVERGENCE: 20-Pilot Ranked FFA / 1-5 Armada Bounty • 60-minute cycle • +100❖.")
    sb.appendLine("  • 2) RIFT SKIRMISH: Ranked 2v2–5v5 Best-of-3 Rounds Team Deathmatch • Ship Lock ON • Interactive Teammate Spectator • +75❖.")
    sb.appendLine(
      String.format(
        Locale.US,
        "  • 3) FLUX CORE: 20-Pilot Ranked Powerball FFA • Tier 1–10 Overcharge (+15 Bounty/s; 2x pulsing Gold radar beacon) • Ejects at %.0f px/s on death or Safe Zone entry.",
        RiftCalibrationState.fluxCoreEjectSpeed,
      )
    )
    sb.appendLine(
      String.format(
        Locale.US,
        "  • 4) STASIS BREACH: Ranked 10v10 Circular Stasis Jailbreak • Prisoners inside deal 100%% weapon damage to the %.0f Flux Stasis Core to detonate a radial shockwave and break everyone out • +100❖ (Live Cores: West=%.0f Flux, East=%.0f Flux).",
        RiftCalibrationState.stasisBrigReactorMaxHp,
        engine.westBrigGeneratorHp,
        engine.eastBrigGeneratorHp,
      )
    )
    sb.appendLine("  • 5) RIFT CONTROL: Ranked Tug-of-War over Rift Alpha & Rift Omega • NO Safe Zone (hasActiveSafeZone() == false) • Graphical Dual-Rift Tug-of-War Top HUD Bar • +90❖.")
    val myWarRec = localPrefs.getSyndicateWarRecord(syndicateTag)
    sb.appendLine(
      "  • 6) SYNDICATE WARS: Ranked Direct Syndicate-vs-Syndicate Ladder • Ship Lock ON • Requires a Sworn Pilot Syndicate and 100% same-Syndicate Armada • Ranked by Syndicate War Trophies (Active Syndicate [$syndicateTag] Record: ${myWarRec.warTrophies} War Trophies, ${myWarRec.warWins}W-${myWarRec.warLosses}L, Streak ${myWarRec.warStreak})."
    )
    sb.appendLine(
      "  • Active Ranked Mode Right Now: ${engine.activeRankedMode.title} (${engine.activeRankedMode.id}, Bracket=${engine.activeTeamSizeBracket}, Human Pilots in Mode=${engine.getHumanPilotCountForMode(engine.activeRankedMode)}, ShipLocked=${engine.doesModeLockShipSelection()})."
    )

    sb.appendLine()
    sb.appendLine("[3. COMPLETE 10-SHIP ROSTER — 100-POINT PARITY LIVE STATS & LASER PIXEL DIMENSIONS]")
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
          "  - Hull: MaxFlux=%.0f Flux, Recharge=%.0f Flux/s, TopSpeed=%.0f px/s (Afterburner=%.0f px/s), Thrust=%.0f px/s², TurnRate=%.2f rad/s.",
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
    sb.appendLine("[4. RIFT FRAME 100-POINT PARITY (FULL 100 PTS AT LVL 20) & SAVED FRAMES A–E]")
    sb.appendLine("- Pilot Level 20 unlocks Rift Frame with the full 100-Point Customization Budget (matching every standard hull). Levels 1–100 grant Rift Shards, 3D Hull Trims, Kill Finishers, Warp-In Signatures, HVT Halos, and Loadout Frames A–E.")
    val activeSlotId = localPrefs.getActiveCustomSlotId(currentUserId)
    for (slotId in CustomShipLoadoutSlot.ALL_SLOT_IDS) {
      val slot = localPrefs.getCustomShipSlot(slotId, currentUserId)
      val unlocked = slotId == "Frame A" || localPrefs.isCustomSlotUnlocked(slotId, currentUserId)
      val activeMarker = if (slotId == activeSlotId) " [ACTIVE]" else ""
      sb.appendLine(
        "  • $slotId$activeMarker (${if (unlocked) "UNLOCKED" else "LOCKED"}) '${slot.customName}': ${slot.totalAllocatedPoints()} Pts (Spd=${slot.speedPts}, Thr=${slot.thrustPts}, Turn=${slot.turnPts}, Flux=${slot.maxFluxPts}, Rech=${slot.rechargePts}, Pri=${slot.primaryArchetype}, Sec=${slot.secondaryType})."
      )
    }

    sb.appendLine()
    sb.appendLine("[5. SUPPLY BUFF POOL, AI-FORGED BUFFS & 4 PLAYFIELD RIFTS]")
    sb.appendLine(
      "- Supply Crate Cap: ${engine.getEffectiveBuffCap()} crates (${engine.greens.size} spawned). Repel Cost=${RiftCalibrationState.repelCost.toInt()} Flux, Radius=${RiftCalibrationState.repelRadius.toInt()}px."
    )
    for (entry in RiftCodex.POWERUP_CODEX) {
      sb.appendLine("  • ${entry.title} (${entry.dropRatePercent}% drop): ${entry.effectSummary.replace("⚡", " Flux")}")
    }
    val customForged = RiftCodex.getCustomForgedBuffs()
    for (cb in customForged) {
      sb.appendLine(
        "  • [CUSTOM FORGED] ${cb.officialName} (${cb.shortLabel}): ${cb.chargesPerPickup} charges, effect=${cb.specialEffect.label}, dmg=${cb.damage.toInt()} Flux, radius=${cb.blastRadius.toInt()}px."
      )
    }
    sb.appendLine(
      String.format(
        Locale.US,
        "- 4 Playfield Rifts (%.0fpx physical core radius, %.0fs lifespan, %d active): 1) PLASMA RIFT (%.0f%% Max Flux burn/0.6s), 2) REPULSOR RIFT (%.0fpx push), 3) GRAVITY RIFT (%.0fpx pull), 4) PAIRED PHASE RIFTS (Phase Rift A & B instantaneous gateway, %.2fs re-entry CD).",
        RiftCalibrationState.riftPhysicalRadius * anomVisScale,
        RiftCalibrationState.riftLifespanSec,
        engine.anomalies.size,
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
    val (curXp, spanXp, _) = engine.getXpProgressInCurrentLevel()
    sb.appendLine(
      "- Active Pilot: [$syndicateTag] $callsign (Syndicate: '$syndicateName') | Level ${engine.pilotLevel}/100 ($curXp/$spanXp XP) | Shards: ${engine.riftShards}❖ | Live Hull: ${engine.formatShipLabel(p)} in Sector $sectorStr, Speed=$pSpeed px/s, Flux=${p.flux.toInt()}/${p.maxFlux.toInt()} Flux, Bounty=${p.bounty}."
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
