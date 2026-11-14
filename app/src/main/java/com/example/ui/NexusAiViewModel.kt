package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.LeaderboardEntry
import com.example.engine.CustomHullVisualRecipe
import com.example.engine.RiftCalibrationState
import com.example.engine.RiftCodex
import com.example.engine.RiftGameEngine
import com.example.engine.SpecialBuffRegistryEntry
import java.util.Locale
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.roundToInt
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
 * 100% On-Device NEXUS Tactical Intelligence Engine (`#nexus-ai`), AI Hull Forge, and AI Buff Forge.
 * Operates with zero cloud LLM calls, zero network latency, and full live access to RIFT physics,
 * 100-Point Ship Parity specs, Ranked/Custom Mode rules, and Sworn Pilot Leaderboards.
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
            updateStreamingBubble(streamingNexusId, partial, stillStreaming = true)
            idx = end
            delay(12)
          }
          val finalText = fullAnswer.trim()
          updateStreamingBubble(streamingNexusId, finalText, stillStreaming = false)
          _streamState.value = NexusStreamState.Success(finalText)
          onStatsMutated()
        } catch (e: CancellationException) {
          throw e
        } catch (e: Exception) {
          removeBubble(streamingNexusId)
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
      )
    if (nonRiftKeywords.any { lower.contains(it) } && riftContextKeywords.none { lower.contains(it) }) {
      return "RIFT CORE TELEMETRY BOUNDARY // Query falls outside RIFT sector parameters. Ask about RIFT ships, weapons, Flux (⚡) math, Ranked Modes, Custom Games, or Sworn Pilot Leaderboards."
    }

    // 2. Pixel / Laser Geometry Queries
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
            "• Suppressor Rotary Pulse Speed: %.0f px/s (15⚡ cost, %.3fs interval, %.0f⚡ damage per bolt).",
            sup.primarySpeed,
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

    // 3. Leaderboard / Top Pilots / Bounties / Executioner Queries
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
            tag != "RFT" && tag != "GUEST" && !it.userId.startsWith("dev_")
          }
          .sortedByDescending { it.pvpBounty }
          .take(5)
      val swornAllTime =
        allTimeLeaderboard
          .filter {
            val tag = it.syndicateTag.trim().uppercase(Locale.US)
            tag != "RFT" && tag != "GUEST" && !it.userId.startsWith("dev_")
          }
          .sortedByDescending { it.allTimeBounty }
          .take(5)

      return buildString {
        appendLine("NEXUS SWORN PILOT LEADERBOARD TELEMETRY // ([RFT] Drifters Excluded)")
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
            "• Your Live Status: [${p.syndicateTag}] ${p.callsign} (${if (isSworn) "Sworn Pilot" else "Drifter"}) | Hull: ${p.spec.name} | Live Bounty: ★ ${p.bounty} | Session K/D: ${p.kills}K/${p.deaths}D ($kd) | Shards: ${engine.riftShards}❖"
          )
        }
        if (swornAllTime.isNotEmpty()) {
          appendLine("• Top All-Time Sworn Pilot Bounties:")
          swornAllTime.forEachIndexed { i, entry ->
            val tag = if (entry.syndicateTag.isNotBlank()) "[${entry.syndicateTag}] " else ""
            appendLine(
              "  #${i + 1} $tag${entry.callsign} — Peak ★ ${entry.allTimeBounty} (${entry.allTimeKills}K / ${entry.allTimeDeaths}D)"
            )
          }
        } else if (swornPvp.isNotEmpty()) {
          appendLine("• Top PvP Sworn Pilot Bounties:")
          swornPvp.forEachIndexed { i, entry ->
            val tag = if (entry.syndicateTag.isNotBlank()) "[${entry.syndicateTag}] " else ""
            appendLine(
              "  #${i + 1} $tag${entry.callsign} — PvP Peak ★ ${entry.pvpBounty} (${entry.pvpKills}K / ${entry.pvpDeaths}D)"
            )
          }
        } else if (liveTelemetrySnapshot.contains("TOP 5 ALL-TIME")) {
          val snippet =
            liveTelemetrySnapshot
              .substringAfter("[6. CLOUD LEADERBOARD", "")
              .take(600)
              .trim()
          if (snippet.isNotEmpty()) appendLine(snippet)
        } else {
          appendLine(
            "• Only Google-authenticated Sworn Pilots are recorded on Cloud Leaderboards. Ephemeral [RFT] Drifters are excluded from permanent rankings."
          )
        }
      }.trim()
    }

    // 4. 100-Point Rift Frame Theorycrafting / Build Queries
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
        appendLine("NEXUS 100-POINT RIFT FRAME THEORYCRAFT // (Active Budget: $budget Pts)")
        appendLine("1) 100-PT HYPER-RAIL PHASE SNIPER (Frame A):")
        appendLine("   • Hull Points (60P): Speed 16P (692 px/s) | Thrust 12P (870 px/s²) | Turn 8P (4.84 rad/s) | Max Flux 12P (3,100⚡) | Recharge 12P (410⚡/s)")
        appendLine("   • Weapons (40P): Hyper-Velocity Dual Rail (15P • 25⚡ • 285⚡ DMG @ 1,120 px/s) + Phase Dash (25P • 300⚡ • 2.5s CD • +35% Next-Shot Rail Overcharge = 385⚡ burst)")
        appendLine("2) 100-PT ECLIPSE BUBBLE BRAWLER (Frame B):")
        appendLine("   • Hull Points (60P): Speed 10P | Thrust 12P | Turn 10P | Max Flux 16P (3,500⚡) | Recharge 12P (410⚡/s)")
        appendLine("   • Weapons (40P): Parallel Twin-Stream (15P • 22⚡ • 2x135⚡ DMG) + Eclipse Stasis Bubble (25P • 380⚡ • 8.0s 275px two-way dome, 12.0s CD)")
        append(
          "• Tip: Every hull stat starts on the universal Baseline Hull Floor (500 px/s, 450 px/s², 3.4 rad/s, 1,900⚡ Max Flux, 230⚡/s Recharge) and scales up to 30P per stat."
        )
      }
    }

    // 5. Specific Ship Queries (Ships 1..10)
    val matchedShips =
      RiftCodex.SHIPS.filter { spec ->
        val nameLower = spec.name.lowercase(Locale.US)
        lower.contains(nameLower) ||
          (spec.id == 9 && (lower.contains("interdictor") || lower.contains("bubble") || lower.contains("eclipse")))
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
              "• Point Allocation: SPD %dP (%.0f px/s, Burn %.0f px/s) | THR %dP (%.0f px/s²) | TURN %dP (%.2f rad/s) | MAX FLUX %dP (%.0f⚡) | RECHARGE %dP (%.0f⚡/s)",
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
              "• Primary [%s • %dP]: %.0f⚡ cost | %.2fs CD | %.0f⚡ damage | %.0f px/s (%d stream%s, %d bounce%s)",
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
                "• Secondary [%s • %dP]: 380⚡ cost | %.1fs CD | Anchors a %.0fpx radius (%.0fpx diameter) two-way Stasis Bubble for %.1fs. Ships bounce off both sides with 0⚡ damage; traps enemies inside for close-quarters dogfights.",
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
                "• Secondary [%s • %dP]: Drains %.0f⚡/s while cloaked (floors at 150⚡). Uncloaking manually fires a 0⚡ %.0fpx 360° EMP pulse that drains %.0f%% enemy Max Flux and freezes recharge for %.1fs.",
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
                "• Secondary [%s • %dP]: %.0f⚡ cost | %.2fs CD | %.0f⚡ damage | %.0f px/s recoil",
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

    // 6. Game Mode & Custom Game Queries
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
        lower.contains("custom game")
    ) {
      return buildString {
        appendLine("NEXUS SECTOR MODES // 6 RANKED MODES + CLONEABLE CUSTOM GAMES")
        appendLine("1) FREE-FOR-ALL (Sector Convergence): 20-pilot open arena (60m cycle). #1 Bounty with >50% presence wins +100❖.")
        appendLine("2) TEAM DEATHMATCH (Rift Skirmish): 2v2–5v5 Best-of-3 rounds (3:00 round timer). No respawn until round ends; eliminated pilots spectate their highest-bounty living ally. +75❖.")
        appendLine("3) FLUX CORE (Kill The Man With The Ball): Hold the center Flux Core for +15 Bounty/s and up to Tier 10 Overcharge (+50% DMG/Recharge, +40% Speed). Carrier cannot enter Safe Zone or Cloak.")
        appendLine("4) STASIS BREACH (10v10 Circular Stasis Jail): Eliminated enemies warp into your team's 360px circular Stasis Bubble Jail (West 1400,4000 / East 6600,4000). Only prisoners inside can damage the 4,000⚡ center Stasis Core; destroying it triggers a radial shockwave that frees all prisoners and damages nearby outside enemies!")
        appendLine("5) RIFT CONTROL (Tug-of-War): Contest Rift Alpha (2600,4000) & Rift Omega (5400,4000). Hold uncontested for 30s to lock a Rift (+1 pt/s). Locking both Rifts wins immediately, or highest score at 3:00 wins +90❖.")
        appendLine("6) SYNDICATE WARS: Ranked Clan-vs-Clan ladder for 100% same-Syndicate Armadas (+125❖ & War Trophies).")
        append("• CUSTOM GAMES: Clone any mode in NEXUS with up to 10 custom rules (Allow Armadas in FFA, AI Bots ON/OFF, Fat Bullets, Speed/Damage multipliers). Main Ranked Modes are locked to 100% factory defaults; Custom Games unlock live Calibration sliders and [📡 SYNC TO ALL]!")
      }
    }

    // 7. Buffs, Supply Crates & Hull Stat Upgrades Queries
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
        appendLine("• 5 Stackable Hull Stat Upgrades (Shown on Buff Bar above Minimap, up to 3x stacks each):")
        appendLine("  - SPD (+8% Top Speed/stack) | THR (+8% Thrust/stack) | RCH (+8% Flux Recharge/stack)")
        appendLine("  - TURN (+12% Turn Rate/stack) | MAX ⚡ (+10% Max Flux & instant +10%⚡ heal/stack)")
        appendLine("  - Plus BNC (+30 Bouncing Primary Shots) & MULTI (+25 Multi-Shot Ammo).")
        appendLine("• 4th Right-Hand Special Buff Slot Pickups:")
        append(
          "  - Seeker Torpedo (1x • 780⚡ homing 2.5s), Shatter Cone (2x • 3 waves of 9 bouncing shards), Nova Ring (2x • 24 radial ricochet bolts), Stasis Barrier (2x • 120px hard-light wall), Hyper Surge (2x • 0⚡ 1.65x speed burn), Holo Phantom (2x • decoy clone), and custom AI-Forged Buffs!"
        )
      }
    }

    // 8. General / Meta / Live Status Summary
    val p = engine?.localPlayer
    val shipSpec = p?.spec ?: RiftCodex.getShipById(1)
    val modeTitle =
      engine?.currentCustomGame?.let { "Custom Game: ${it.title} (${it.baseMode.title})" }
        ?: engine?.activeRankedMode?.title
        ?: "Free-For-All"
    return buildString {
      appendLine("NEXUS TACTICAL TELEMETRY // ON-DEVICE CORE READY")
      if (p != null && engine != null) {
        appendLine(
          String.format(
            Locale.US,
            "• Active Hull: %s (%s) | Flux: %.0f⚡ / %.0f⚡ (%.0f⚡/s) | Bounty: ★ %d | Shards: %d❖",
            shipSpec.name,
            shipSpec.role,
            p.flux,
            p.maxFlux,
            shipSpec.rechargeRate,
            p.bounty,
            engine.riftShards,
          )
        )
        val isSworn =
          p.syndicateTag.trim().uppercase(Locale.US) !in setOf("RFT", "GUEST")
        appendLine(
          "• Active Sector: $modeTitle | Pilot Status: ${if (isSworn) "Sworn Pilot" else "[RFT] Drifter"}"
        )
      }
      appendLine("• All 10 Ships (100-Point Parity): Viper, Ricochet, Suppressor, Goliath, Twinfang, Disruptor, Valkyrie, Manta, Eclipse (8.0s 275px Stasis Bubble, 380⚡, 12s CD), and modular Rift Frame (Frame A/B/C).")
      append(
        "• Ask about any ship's exact pixel/Flux math, 100-pt Rift Frame builds, the 6 Ranked Modes (including Stasis Breach & Rift Control), Custom Game [📡 SYNC TO ALL], or Sworn Pilot Leaderboards."
      )
    }
  }

  private fun updateStreamingBubble(targetId: Long, newText: String, stillStreaming: Boolean) {
    _messages.value =
      _messages.value.map { msg ->
        if (msg.id == targetId) {
          msg.copy(text = newText, isStreaming = stillStreaming)
        } else {
          msg
        }
      }
  }

  private fun removeBubble(targetId: Long) {
    _messages.value = _messages.value.filterNot { it.id == targetId }
  }
}
