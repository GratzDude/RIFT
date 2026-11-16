package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.LeaderboardEntry
import com.example.engine.CustomHullVisualRecipe
import com.example.engine.RiftCalibrationState
import com.example.engine.RiftCodex
import com.example.engine.RiftGameEngine
import com.example.engine.RiftProgressionCatalog
import com.example.engine.SpecialBuffRegistryEntry
import java.util.Locale
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class NexusRole {
  USER,
  NEXUS,
}

data class NexusChatMessage(
  val id: Long,
  val role: NexusRole,
  val text: String,
  val isStreaming: Boolean = false,
)

sealed interface NexusStreamState {
  data object Idle : NexusStreamState
  data object Loading : NexusStreamState
  data class Streaming(val partialText: String) : NexusStreamState
  data class Success(val text: String) : NexusStreamState
  data class Error(val message: String) : NexusStreamState
}

/**
 * 100% On-Device NEXUS Tactical Intelligence Engine (`#nexus-ai`), Hull Forge, and Buff Forge.
 * Operates with zero cloud LLM calls, zero network latency, and full live access to RIFT physics,
 * Rift Conduit Resonance, 100-Point Ship Parity specs, Ranked/Custom Mode rules, and Sworn Pilot Leaderboards.
 */
class NexusAiViewModel : ViewModel() {

  private val _messages = MutableStateFlow<List<NexusChatMessage>>(emptyList())
  val messages: StateFlow<List<NexusChatMessage>> = _messages.asStateFlow()

  private val _streamState = MutableStateFlow<NexusStreamState>(NexusStreamState.Idle)
  val streamState: StateFlow<NexusStreamState> = _streamState.asStateFlow()

  private var nextMessageId = 1L
  private var activeJob: Job? = null
  var lastFailedQuestion: String? = null
    private set

  private var cachedEngine: RiftGameEngine? = null
  private var cachedPvpLeaderboard: List<LeaderboardEntry> = emptyList()
  private var cachedAllTimeLeaderboard: List<LeaderboardEntry> = emptyList()

  val isBusy: Boolean
    get() =
      _streamState.value is NexusStreamState.Loading ||
        _streamState.value is NexusStreamState.Streaming

  fun askNexus(
    userQuestion: String,
    engine: RiftGameEngine,
    pvpLeaderboard: List<LeaderboardEntry> = emptyList(),
    allTimeLeaderboard: List<LeaderboardEntry> = emptyList(),
    onStatsMutated: () -> Unit = {},
  ) {
    cachedEngine = engine
    cachedPvpLeaderboard = pvpLeaderboard
    cachedAllTimeLeaderboard = allTimeLeaderboard
    val responseText =
      synthesizeOnDeviceResponse(
        rawQuestion = userQuestion,
        engine = engine,
        pvpLeaderboard = pvpLeaderboard,
        allTimeLeaderboard = allTimeLeaderboard,
        liveTelemetrySnapshot = "",
      )
    streamLocalResponse(userQuestion, responseText, onStatsMutated)
  }

  fun askNexus(rawQuestion: String, liveTelemetrySnapshot: String) {
    val responseText =
      synthesizeOnDeviceResponse(
        rawQuestion = rawQuestion,
        engine = cachedEngine,
        pvpLeaderboard = cachedPvpLeaderboard,
        allTimeLeaderboard = cachedAllTimeLeaderboard,
        liveTelemetrySnapshot = liveTelemetrySnapshot,
      )
    streamLocalResponse(rawQuestion, responseText)
  }

  private fun streamLocalResponse(
    rawQuestion: String,
    fullAnswer: String,
    onStatsMutated: () -> Unit = {},
  ) {
    val question = rawQuestion.trim()
    if (question.isEmpty() || isBusy) return

    lastFailedQuestion = null
    val priorCompletedMessages = _messages.value.filter { !it.isStreaming && it.text.isNotBlank() }

    val userMsg =
      NexusChatMessage(
        id = nextMessageId++,
        role = NexusRole.USER,
        text = question,
        isStreaming = false,
      )
    val streamingNexusId = nextMessageId++
    val placeholderNexusMsg =
      NexusChatMessage(
        id = streamingNexusId,
        role = NexusRole.NEXUS,
        text = "",
        isStreaming = true,
      )
    _messages.value = priorCompletedMessages + userMsg + placeholderNexusMsg
    _streamState.value = NexusStreamState.Loading

    activeJob =
      viewModelScope.launch {
        try {
          val words = fullAnswer.split(" ")
          val accumulated = StringBuilder()
          val chunkSize = 4
          var idx = 0
          while (idx < words.size) {
            val end = (idx + chunkSize).coerceAtMost(words.size)
            val piece = words.subList(idx, end).joinToString(" ")
            if (accumulated.isNotEmpty()) accumulated.append(" ")
            accumulated.append(piece)
            val partial = accumulated.toString()
            _streamState.value = NexusStreamState.Streaming(partial)
            updateStreamingMessage(streamingNexusId, partial, stillStreaming = true)
            idx = end
            delay(12)
          }
          val finalText = fullAnswer.trim()
          updateStreamingMessage(streamingNexusId, finalText, stillStreaming = false)
          _streamState.value = NexusStreamState.Success(finalText)
          onStatsMutated()
        } catch (e: CancellationException) {
          throw e
        } catch (e: Exception) {
          removeStreamingMessage(streamingNexusId)
          lastFailedQuestion = question
          _streamState.value =
            NexusStreamState.Error(e.localizedMessage ?: "Local NEXUS telemetry interrupted.")
        }
      }
  }

  fun retryLastQuestion(liveTelemetrySnapshot: String = "") {
    val failed = lastFailedQuestion ?: return
    val eng = cachedEngine
    if (eng != null) {
      askNexus(failed, eng, cachedPvpLeaderboard, cachedAllTimeLeaderboard)
    } else {
      askNexus(failed, liveTelemetrySnapshot)
    }
  }

  fun clearConversation() {
    activeJob?.cancel()
    activeJob = null
    lastFailedQuestion = null
    _messages.value = emptyList()
    _streamState.value = NexusStreamState.Idle
  }

  fun forgeCustomHullRecipe(
    prompt: String,
    variationSalt: Int = 0,
    onResult: ((CustomHullVisualRecipe) -> Unit)? = null,
  ): CustomHullVisualRecipe {
    val recipe = RiftCalibrationState.generateLocalHullRecipe(prompt, variationSalt)
    onResult?.invoke(recipe)
    return recipe
  }

  fun forgeCustomBuffSpec(
    prompt: String,
    onResult: ((SpecialBuffRegistryEntry) -> Unit)? = null,
  ): SpecialBuffRegistryEntry {
    val entry = RiftCalibrationState.forgeBuffFromPrompt(prompt)
    onResult?.invoke(entry)
    return entry
  }

  private fun synthesizeOnDeviceResponse(
    rawQuestion: String,
    engine: RiftGameEngine?,
    pvpLeaderboard: List<LeaderboardEntry>,
    allTimeLeaderboard: List<LeaderboardEntry>,
    liveTelemetrySnapshot: String,
  ): String {
    val q = rawQuestion.trim()
    val lower = q.lowercase(Locale.US)

    // 1. Non-RIFT Topic Guardrail
    val nonRiftKeywords =
      listOf(
        "weather",
        "forecast",
        "president",
        "election",
        "politics",
        "recipe",
        "bake",
        "cook",
        "pizza",
        "pasta",
        "homework",
        "algebra",
        "calculus",
        "essay",
        "fortnite",
        "minecraft",
        "valorant",
        "bitcoin",
        "crypto",
        "stock market",
      )
    val riftContextKeywords =
      listOf(
        "rift",
        "ship",
        "flux",
        "shard",
        "resonance",
        "conduit",
        "directive",
        "viper",
        "ricochet",
        "suppressor",
        "goliath",
        "twinfang",
        "disruptor",
        "valkyrie",
        "manta",
        "eclipse",
        "interdictor",
        "frame",
        "bounty",
        "syndicate",
        "armada",
        "strike wing",
        "stasis",
        "skirmish",
        "convergence",
        "crate",
        "buff",
        "laser",
        "pixel",
        "bomb",
        "mine",
        "seeker",
        "repel",
        "cloak",
        "emp",
        "leaderboard",
        "pilot",
        "drifter",
        "build",
        "spectat",
        "megafat",
      )
    if (nonRiftKeywords.any { lower.contains(it) } && riftContextKeywords.none { lower.contains(it) }) {
      return "RIFT CORE TELEMETRY BOUNDARY // Query falls outside RIFT sector parameters. Ask about RIFT ships, weapons, Flux math, Rift Conduit Resonance, Ranked Modes, Custom Games, or Sworn Pilot Leaderboards."
    }

    // 2. Rift Conduit / Resonance / Starting Bounty / Daily Directives Queries
    if (
      lower.contains("resonance") ||
        lower.contains("conduit") ||
        lower.contains("directive") ||
        lower.contains("starting bounty")
    ) {
      val liveRes = engine?.liveResonance ?: 0
      val peakRes = engine?.peakResonance ?: 0
      val tier = RiftProgressionCatalog.getConduitTierForResonance(liveRes)
      return buildString {
        appendLine("NEXUS RIFT CONDUIT TELEMETRY // LIVE & PEAK RESONANCE")
        appendLine(
          "• Active Resonance: Live $liveRes (${tier.title}) | Peak $peakRes | Starting Bounty: ${tier.startingBounty} BTY | Shards: ${engine?.riftShards ?: 0L}❖"
        )
        appendLine("• Starting Bounty Ladder (100 -> 500 Max): Drift Initiate (0..149 = 100 BTY), Vanguard (150..349 = 150 BTY), Interceptor (350..649 = 225 BTY), Phantom (650..999 = 300 BTY), Rift Commander (1000..1499 = 400 BTY), Apex Archon (1500+ = 500 BTY).")
        appendLine("• Ranked Gain/Loss Math: Human PvP kills (+18 RES, +28 on HVT shutdown), Ranked match wins (+30 RES), accuracy bonus on life end; deaths/losses adjust Live Resonance while Peak Resonance permanently unlocks cosmetics & Frame B–E. Bot kill Resonance tapers to 0 at 500+ Live Resonance.")
        append("• 3 Daily Directives: Earn 40❖ + 50❖ + 60❖ plus the +100❖ 3/3 Sweep Chest = 250❖/day (with 1 free directive reroll per UTC day).")
      }
    }

    // 3. Pixel / Laser Geometry Queries
    if (lower.contains("pixel") || (lower.contains("suppressor") && lower.contains("laser"))) {
      val projScale = RiftCalibrationState.projectileScale
      val sup = RiftCodex.getShipById(3)
      val shaftLen = sup.primarySpeed * 0.036f
      val outerW = 6.4f * projScale
      val innerW = 2.8f * projScale
      val totalLen = shaftLen + outerW
      return buildString {
        appendLine("NEXUS PIXEL TELEMETRY // SUPPRESSOR & PRIMARY BOLT GEOMETRY")
        appendLine(
          String.format(
            Locale.US,
            "• Suppressor Rapid Twin-Ion Speed: %.0f px/s (%.0f Flux cost, %.3fs interval, %.0f Flux damage per bolt).",
            sup.primarySpeed,
            sup.primaryCost,
            sup.primaryCooldown,
            sup.primaryDamage,
          )
        )
        appendLine(
          String.format(
            Locale.US,
            "• Shaft Vector Length: %.2f world px (primarySpeed %.0f * 0.036s motion trail).",
            shaftLen,
            sup.primarySpeed,
          )
        )
        appendLine(
          String.format(
            Locale.US,
            "• Stroke Widths (at %.2fx Projectile Scale): Outer Glow = %.2f world px | Inner White Core = %.2f world px.",
            projScale,
            outerW,
            innerW,
          )
        )
        appendLine(
          String.format(
            Locale.US,
            "• Total Visual Footprint (with round endcaps): %.2f world px (%.2f screen px at default 0.65x zoom).",
            totalLen,
            totalLen * 0.65f,
          )
        )
        append(
          "• Ship Hull Hitbox: 128x128 offscreen sprite rendered at 88x88 world px with a 36.0 world px physical collision radius (72.0 px diameter)."
        )
      }
    }

    // 4. Leaderboard / Top Pilots / Bounties / Executioner Queries
    if (
      lower.contains("leaderboard") ||
        lower.contains("highest bounty") ||
        lower.contains("top pilot") ||
        lower.contains("executioner") ||
        lower.contains("peak bounty") ||
        lower.contains("record")
    ) {
      val swornPvp =
        pvpLeaderboard
          .filter {
            val tag = it.syndicateTag.trim().uppercase(Locale.US)
            tag != "RFT" &&
              tag != "GUEST" &&
              tag != "BOT" &&
              !it.callsign.contains("[BOT]") &&
              !it.userId.startsWith("dev_")
          }
          .sortedByDescending { it.pvpBounty }
          .take(5)
      val swornAllTime =
        allTimeLeaderboard
          .filter {
            val tag = it.syndicateTag.trim().uppercase(Locale.US)
            tag != "RFT" &&
              tag != "GUEST" &&
              tag != "BOT" &&
              !it.callsign.contains("[BOT]") &&
              !it.userId.startsWith("dev_")
          }
          .sortedByDescending { it.allTimeBounty }
          .take(5)

      return buildString {
        appendLine("NEXUS SWORN PILOT LEADERBOARD TELEMETRY // (AI Bots & [RFT] Drifters Excluded)")
        if (engine != null) {
          val p = engine.localPlayer
          val isSworn =
            p.syndicateTag.trim().uppercase(Locale.US) !in setOf("RFT", "GUEST")
          val kd =
            if (p.deaths > 0) {
              String.format(Locale.US, "%.2f", p.kills.toFloat() / p.deaths)
            } else {
              "${p.kills}.00"
            }
          appendLine(
            "• Your Live Status: [${p.syndicateTag}] ${p.callsign} (${if (isSworn) "Sworn Pilot" else "Drifter"}) | Hull: ${p.spec.name} | Live Resonance: ${engine.liveResonance} | Live Bounty: ${p.bounty} BTY | Session K/D: ${p.kills}K/${p.deaths}D ($kd) | Shards: ${engine.riftShards}❖"
          )
        }
        if (swornAllTime.isNotEmpty()) {
          appendLine("• Top All-Time Sworn Pilot Bounties:")
          swornAllTime.forEachIndexed { i, entry ->
            val tag = if (entry.syndicateTag.isNotBlank()) "[${entry.syndicateTag}] " else ""
            appendLine(
              "  #${i + 1} $tag${entry.callsign} — Peak ${entry.allTimeBounty} BTY (${entry.allTimeKills}K / ${entry.allTimeDeaths}D)"
            )
          }
        } else if (swornPvp.isNotEmpty()) {
          appendLine("• Top PvP Sworn Pilot Bounties:")
          swornPvp.forEachIndexed { i, entry ->
            val tag = if (entry.syndicateTag.isNotBlank()) "[${entry.syndicateTag}] " else ""
            appendLine(
              "  #${i + 1} $tag${entry.callsign} — PvP Peak ${entry.pvpBounty} BTY (${entry.pvpKills}K / ${entry.pvpDeaths}D)"
            )
          }
        } else if (liveTelemetrySnapshot.contains("Top Cloud Sworn Pilot")) {
          val snippet =
            liveTelemetrySnapshot
              .substringAfter("[6. REAL-PLAYER-ONLY", "")
              .take(600)
              .trim()
          if (snippet.isNotEmpty()) appendLine(snippet)
        } else {
          appendLine(
            "• Only Google-authenticated Sworn Pilots are recorded on Cloud Leaderboards. AI Bots and ephemeral [RFT] Drifters are strictly excluded."
          )
        }
      }.trim()
    }

    // 5. 100-Point Rift Frame Theorycrafting / Build Queries
    if (
      lower.contains("theorycraft") ||
        lower.contains("rift frame") ||
        lower.contains("100-pt") ||
        lower.contains("100 pt") ||
        lower.contains("sniper build") ||
        lower.contains("custom ship")
    ) {
      val budget = engine?.getCustomizationPointsTotal() ?: 100
      return buildString {
        appendLine("NEXUS 100-POINT RIFT FRAME THEORYCRAFT // (Active Budget: $budget Pts • Open at 0 Resonance)")
        appendLine("1) 100-PT HYPER-RAIL PHASE SNIPER (Frame A):")
        appendLine("   • Hull Points (65P): Speed 25P | Thrust 8P | Turn 18P | Max Flux 0P (2,200 Flux) | Recharge 14P (352 Flux/s)")
        appendLine("   • Weapons (35P): 1-Bounce Rail-Laser (15P • 26 Flux • 225 Flux DMG @ 1,280 px/s) + Phase Dash (20P • 320 Flux • 0.65s CD • +35% Next-Shot Rail Overcharge)")
        appendLine("2) 100-PT ECLIPSE STASIS ARCHON (Frame E):")
        appendLine("   • Hull Points (66P): Speed 12P | Thrust 12P | Turn 11P | Max Flux 18P (3,640 Flux) | Recharge 13P (344 Flux/s)")
        appendLine("   • Weapons (34P): Ricochet Twin-Pulse (12P • 20 Flux • 2x92 Flux DMG) + Two-Way Stasis (22P • 320 Flux • 6.0s 275px two-way Stasis field, 8.5s CD)")
        append(
          "• Tip: All 10 hulls are open from 0 Resonance. Save up to 5 independent builds across Frame A through Frame E in Hangar Deck & Rift Conduit."
        )
      }
    }

    // 6. Specific Ship Queries (Ships 1..10)
    val matchedShips =
      RiftCodex.SHIPS.filter { spec ->
        val nameLower = spec.name.lowercase(Locale.US)
        lower.contains(nameLower) ||
          (spec.id == 9 && (lower.contains("interdictor") || lower.contains("stasis") || lower.contains("eclipse")))
      }
    if (matchedShips.isNotEmpty()) {
      return buildString {
        matchedShips.forEachIndexed { idx, s ->
          if (idx > 0) appendLine()
          val parityPts =
            s.speedPts + s.thrustPts + s.turnPts + s.maxFluxPts + s.rechargePts + s.primaryPts + s.secondaryPts
          appendLine(
            "NEXUS HULL DOSSIER // ${s.name.uppercase(Locale.US)} (${s.role} • ${parityPts}P Parity)"
          )
          appendLine(
            String.format(
              Locale.US,
              "• Point Allocation: SPD %dP (%.0f px/s, Burn %.0f px/s) | THR %dP (%.0f px/s²) | TURN %dP (%.2f rad/s) | MAX FLUX %dP (%.0f Flux) | RECHARGE %dP (%.0f Flux/s)",
              s.speedPts,
              s.topSpeed,
              s.afterburnerSpeed,
              s.thrustPts,
              s.thrust,
              s.turnPts,
              s.turnRate,
              s.maxFluxPts,
              s.maxFlux,
              s.rechargePts,
              s.rechargeRate,
            )
          )
          appendLine(
            String.format(
              Locale.US,
              "• Primary [%s • %dP]: %.0f Flux cost | %.2fs CD | %.0f Flux damage | %.0f px/s (%d stream%s, %d bounce%s)",
              s.primaryTitle,
              s.primaryPts,
              s.primaryCost,
              s.primaryCooldown,
              s.primaryDamage,
              s.primarySpeed,
              s.primaryStreams,
              if (s.primaryStreams == 1) "" else "s",
              s.primaryBounces,
              if (s.primaryBounces == 1) "" else "s",
            )
          )
          if (s.id == 9) {
            appendLine(
              String.format(
                Locale.US,
                "• Secondary [%s • %dP]: 320 Flux cost | %.1fs CD | Anchors a %.0fpx radius (%.0fpx diameter) two-way Stasis field for %.1fs. Ships bounce off both sides with 0 Flux wall damage; reflects ordnance inside and outside.",
                s.secondaryTitle,
                s.secondaryPts,
                RiftCalibrationState.interdictorBubbleCooldown,
                RiftCalibrationState.interdictorBubbleRadius,
                RiftCalibrationState.interdictorBubbleRadius * 2f,
                RiftCalibrationState.interdictorBubbleDuration,
              )
            )
          } else if (s.bombType == "CLOAK_EMP") {
            appendLine(
              String.format(
                Locale.US,
                "• Secondary [%s • %dP]: 20 Flux entry + %.0f Flux/s cloak drain (50%% cloaked afterburner drain, 4.5s CD, floors at 150 Flux). Uncloaking manually fires a 0 Flux %.0fpx 360° EMP pulse that drains %.0f%% enemy Max Flux (up to 850 Flux) and freezes recharge for %.1fs.",
                s.secondaryTitle,
                s.secondaryPts,
                RiftCalibrationState.cloakDrainPerSec,
                RiftCalibrationState.empPulseRadius,
                RiftCalibrationState.empFluxDrainPct * 100f,
                RiftCalibrationState.empFreezeSec,
              )
            )
          } else {
            appendLine(
              String.format(
                Locale.US,
                "• Secondary [%s • %dP]: %.0f Flux cost | %.2fs CD | %.0f Flux damage | %.0f px/s recoil",
                s.secondaryTitle,
                s.secondaryPts,
                s.bombCost,
                s.bombCooldown,
                s.bombDamage,
                s.bombRecoil,
              )
            )
          }
          append("• Tactical Note: ${s.tacticalLore}")
        }
      }
    }

    // 7. Game Mode & Custom Game Queries
    if (
      lower.contains("mode") ||
        lower.contains("convergence") ||
        lower.contains("skirmish") ||
        lower.contains("tdm") ||
        lower.contains("deathmatch") ||
        lower.contains("flux core") ||
        lower.contains("stasis breach") ||
        lower.contains("jail") ||
        lower.contains("rift control") ||
        lower.contains("tug") ||
        lower.contains("syndicate war") ||
        lower.contains("custom game") ||
        lower.contains("megafat") ||
        lower.contains("spectat")
    ) {
      return buildString {
        appendLine("NEXUS SECTOR MODES // 6 RANKED MODES + CLONEABLE CUSTOM RIFTS")
        appendLine("1) SECTOR CONVERGENCE (FFA): 20-pilot open arena (60m cycle). #1 Bounty with >50% presence wins +100❖. Ship switching allowed.")
        appendLine("2) RIFT SKIRMISH (Team Deathmatch): 2v2–5v5 Best-of-3 rounds. Ship Lock ON. Eliminated pilots use the Living-Teammate Spectator Box (prev/next pills or Gamepad LB/RB). +75❖.")
        appendLine("3) FLUX CORE: Hold the center Flux Core for +15 Bounty/s and up to Tier 10 Overcharge (+50% DMG/Recharge, +40% Speed, 2x pulsing Gold radar beacon). Ship switching allowed.")
        appendLine("4) STASIS BREACH (10v10 Circular Stasis Jail): Eliminated enemies warp into your team's circular Stasis Jail. Prisoners deal 100% weapon damage to the center Stasis Core to detonate a breakout shockwave. +100❖.")
        appendLine("5) RIFT CONTROL (Tug-of-War): Contest Rift Alpha & Rift Omega with NO Safe Zone anywhere on the map and a Graphical Dual-Rift Tug-of-War Top HUD Bar. +85❖.")
        appendLine("6) SYNDICATE WARS: Ranked Syndicate-vs-Syndicate ladder for 100% same-Syndicate Strike Wings. Ship Lock ON. Awards +75❖/+100❖ & War Trophies.")
        append("• CUSTOM RIFTS: Clone any mode with public or 6-char private room codes, MegaFat Bullets, AI Bots ON/OFF, and live Calibration sync.")
      }
    }

    // 8. Buffs, Supply Crates & Hull Stat Upgrades Queries
    if (
      lower.contains("buff") ||
        lower.contains("crate") ||
        lower.contains("upgrade") ||
        lower.contains("powerup") ||
        lower.contains("turn radius") ||
        lower.contains("overcharge")
    ) {
      return buildString {
        appendLine("NEXUS SUPPLY CRATE & BUFF TELEMETRY //")
        appendLine("• 5 Stackable Hull Stat Upgrades (up to 3x stacks each):")
        appendLine("  - SPD (+8% Top Speed/stack) | THR (+8% Thrust/stack) | RCH (+8% Flux Recharge/stack)")
        appendLine("  - TURN (+12% Turn Rate/stack) | MAX FLUX (+10% Max Flux & instant +10% Flux heal/stack)")
        appendLine("  - Plus BNC (+30 Bouncing Primary Shots) & MULTI (+25 Multi-Shot Ammo).")
        appendLine("• 4th Right-Hand Special Buff Slot Pickups:")
        append(
          "  - Seeker Torpedo (1x • 880 Flux homing 2.5s), Shatter Cone (2x • 3 waves of bouncing shards), Nova Ring (1x • 16 radial ricochet bolts), Stasis Barrier (1x • 45.0s tethered cyan hard-light forcefield), Hyper Surge (1x • 0 Flux speed burn), Holo Phantom (1x • decoy clone), and custom Forged Buffs."
        )
      }
    }

    // 9. General / Meta / Live Status Summary
    val modeTitle =
      engine?.currentCustomGame?.let { "Custom Rift: ${it.title} (${it.baseMode.title})" }
        ?: engine?.activeRankedMode?.title
        ?: "Sector Convergence"
    return buildString {
      appendLine("NEXUS TACTICAL TELEMETRY // ON-DEVICE CORE READY")
      if (engine != null) {
        val p = engine.localPlayer
        val shipSpec = p.spec
        val tier = RiftProgressionCatalog.getConduitTierForResonance(engine.liveResonance)
        appendLine(
          String.format(
            Locale.US,
            "• Active Hull: %s (%s) | Flux: %.0f / %.0f (%.0f Flux/s) | Bounty: %d BTY | Resonance: %d (%s) | Shards: %d❖",
            shipSpec.name,
            shipSpec.role,
            p.flux,
            p.maxFlux,
            shipSpec.rechargeRate,
            p.bounty,
            engine.liveResonance,
            tier.title,
            engine.riftShards,
          )
        )
        val isSworn =
          p.syndicateTag.trim().uppercase(Locale.US) !in setOf("RFT", "GUEST")
        appendLine(
          "• Active Sector: $modeTitle | Pilot Status: ${if (isSworn) "Sworn Pilot" else "[RFT] Drifter"}"
        )
      }
      appendLine("• All 10 Ships (Open at 0 Resonance • 100-Point Parity): Viper, Ricochet, Suppressor, Goliath, Twinfang, Disruptor (36 Flux/s Cloak + 0 Flux EMP), Valkyrie, Manta, Eclipse (6.0s 275px Two-Way Stasis, 320 Flux, 8.5s CD), and modular Rift Frame (100 Pts + Frame A–E).")
      append(
        "• Ask about any ship's exact pixel/Flux math, Rift Conduit Resonance & 250❖ Daily Directives, 100-pt Rift Frame builds, Paired Phase Rifts, the 6 Ranked Modes, or Sworn Pilot Leaderboards."
      )
    }
  }

  private fun updateStreamingMessage(targetId: Long, newText: String, stillStreaming: Boolean) {
    _messages.value =
      _messages.value.map { msg ->
        if (msg.id == targetId) {
          msg.copy(text = newText, isStreaming = stillStreaming)
        } else {
          msg
        }
      }
  }

  private fun removeStreamingMessage(targetId: Long) {
    _messages.value = _messages.value.filterNot { it.id == targetId }
  }
}
