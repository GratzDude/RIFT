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
 * Operates with zero cloud LLM calls, zero network latency, and full live access to NEXUS physics,
 * the 6-Card `#060D1A` Sci-Fi Command Tablet & `1.0x..6.0x` Pinch-to-Zoom Overwatch, `5x` (`180px`)
 * `DS9` Safe-Zone Station, `440px` Gravity Rift Tidal Implosion, Rift Conduit Resonance, 100-Point
 * Ship Parity specs, Ranked/Custom Mode telemetry, and Sworn Pilot Leaderboards.
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

    // 1. Non-NEXUS Topic Guardrail
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
        "nexus",
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
        "swarm",
        "drone",
        "unstable",
        "gravity",
        "implos",
        "tidal",
        "asteroid",
        "station",
        "ds9",
        "nova",
        "slingshot",
        "storm",
        "lightning",
        "safe zone",
        "safe harbor",
        "overwatch",
        "pinch",
        "zoom",
        "tablet",
        "hub",
        "barrier",
        "forge",
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
      return "NEXUS CORE TELEMETRY BOUNDARY // Query falls outside NEXUS sector parameters. Ask about NEXUS ships, weapons, Flux math, Playfield Rifts, 440px Gravity Rift Tidal Implosion, Unstable Rift convergence, 5x DS9 Safe-Zone Station, Hub Overwatch, Swarm Drones, Destructible Asteroids, Rift Conduit Resonance, Ranked Modes, or Sworn Pilot Leaderboards."
    }

    // 2. NEXUS Sci-Fi Command Tablet Hub & Pinch-to-Zoom Live Overwatch Queries
    if (
      lower.contains("overwatch") ||
        lower.contains("pinch") ||
        lower.contains("quick zone") ||
        (lower.contains("tablet") && !lower.contains("privacy")) ||
        (lower.contains("hub") && !lower.contains("safe"))
    ) {
      return buildString {
        appendLine("NEXUS SCI-FI COMMAND TABLET & LIVE OVERWATCH TELEMETRY // (BUILD #72)")
        appendLine(
          "- Unified #060D1A Command Tablet: Frames 6 large 192x192 5-stop metallic station-styled module cards — Left Wing: SHIPS & BUILDS (Hangar Deck & 100-Pt parity), RANKS & QUESTS (Rift Conduit & Daily Directives), SKINS & AUDIO (Star Forge 36-target 1:1 lab & track BPM); Right Wing: CHAT & NEXUS AI (Subspace Comms & #nexus-ai), CLANS & SQUADS (Syndicate Citadel & [TAG]/Strike Wing), STATS & CODEX (Dossier, Ladders & Field Manual)."
        )
        appendLine(
          "- Round Rippling Dynamic-Lit Overwatch Aperture: Masks #060D1A tablet glass outside a circular viewport whose concentric polar voxel/matrix rings ripple inward into a live top-down match."
        )
        append(
          "- Interactive 1.0x..6.0x Pinch-to-Zoom & Pan: Defaults to 1.0x full-playfield fit (hiding world text/nametags below 1.55x zoom for a clean radar view and fading them in smoothly as you zoom closer), auto-selects your [FAV] Rift or the active game with Highest Combined Pilot Resonance (Game Type • RIFT #XX • Combined RES), and supports < / > cycling, double-tap / [1.0x] reset, [QUICK ZONE IN], and [ALL MODES]."
        )
      }
    }

    // 3. Playfield Rifts, 440px Gravity Rift Implosion, Unstable Rift Convergence, Repulsor Slingshot & Storm Wall Queries
    if (
      lower.contains("unstable") ||
        lower.contains("doomsday") ||
        lower.contains("slingshot") ||
        lower.contains("repulsor") ||
        lower.contains("gravity rift") ||
        lower.contains("implos") ||
        lower.contains("tidal") ||
        lower.contains("440") ||
        lower.contains("phase rift") ||
        lower.contains("plasma rift") ||
        lower.contains("storm") ||
        lower.contains("boundary") ||
        lower.contains("lightning")
    ) {
      return buildString {
        appendLine("NEXUS SPATIAL ANOMALY & BOUNDARY TELEMETRY //")
        appendLine(
          String.format(
            Locale.US,
            "- Gravity Rift (%.0fpx Pull • Up to 440px Swept Growth & >= 260px Tidal Implosion): Swept weapon absorption across 0.92x visible radius grows the singularity (+18px per primary bolt, +40px per bomb/torpedo, +14px per Swarm drone/Holo Phantom) up to 440px max with crackling rim lightning. Once swollen >= 260px, it drags nearby Crystal Asteroids, Orbital/Asteroid Stations, Debris, and Stasis Barriers straight into the core at 950/s tidal crush damage -> implosion + guaranteed Supply Crate + 20.0s respawn + 16-bolt 360-degree Overload Nova on Stations.",
            RiftCalibrationState.gravityPullRadius,
          )
        )
        appendLine(
          "- Unstable Rift Convergence (10.0s Doomsday): Whenever two active non-partnered Playfield Rifts overlap or expand into contact, they fuse into a swirling multicolored Unstable Rift with a 10.0s convergence timer. At 0.0s it unleashes a Doomsday shockwave dealing 75% of each nearby ship's Max Flux, shatters nearby Crystal Asteroids, and spawns fresh sector Rifts."
        )
        appendLine(
          String.format(
            Locale.US,
            "- Repulsor Rift (%.0fpx Deflection + Supersonic Inner Slingshot): Deflects outer trajectories, while penetrating its inner core launches ships at 1.45x Afterburner speed in a supersonic slingshot burst.",
            RiftCalibrationState.repulsorFieldRadius,
          )
        )
        appendLine(
          String.format(
            Locale.US,
            "- Plasma Rift & Paired Phase Rifts: Plasma Rift sears hulls for %.0f%% Max Flux per 0.6s tick; Paired Phase Rifts (Phase Rift A & Phase Rift B) instantaneously warp ships and bouncing weapon trajectories across sectors (%.2fs re-entry CD).",
            RiftCalibrationState.plasmaRiftDamagePct * 100f,
            RiftCalibrationState.phaseRiftCooldownSec,
          )
        )
        append(
          "- Rippling 3D Rift-Cloud Storm Boundary: The circular 4,000px arena edge renders a seamless AGSL storm cloud wall bleeding 260px inward with live branching arc lightning striking ships within 220px of the edge."
        )
      }
    }

    // 4. 5x DS9 Safe-Zone Station, Destructible Crystal Asteroids, 10-Hit Station Defense Novas & Anti-AFK Queries
    if (
      lower.contains("asteroid") ||
        lower.contains("station") ||
        lower.contains("ds9") ||
        (lower.contains("nova") && !lower.contains("nova ring")) ||
        lower.contains("safe zone") ||
        lower.contains("safe harbor") ||
        lower.contains("afk")
    ) {
      return buildString {
        appendLine("NEXUS 5X DS9 SAFE HARBOR, CRYSTAL ASTEROID & STATION NOVA TELEMETRY //")
        appendLine(
          "- 5x Ship-Scale (180px) DS9 Safe-Zone Station & Holo-Ring: Active ONLY in Sector Convergence and Flux Core (completely disabled in Rift Control, Stasis Breach, Rift Skirmish, and Syndicate Wars, alongside Open-Space Anti-AFK). Anchored at sector center by a 5x ship-size (180px world radius) Deep Space Nine metallic station sprite with 5-stop ship armor shading, 6 curved docking pylons, a shrunk revolving cyan/gold Ops Holo-Ring, and radial stabilizer beams bracing the Safe-Zone rim — all live-editable inside STAR FORGE (15s max stay / weapon-lockout; entering ejects the Flux Core at 1,650 px/s)."
        )
        appendLine(
          "- Destructible Crystal Asteroids (2,400 Flux): Display an on-rock circular Flux integrity ring when damaged. Reducing a Crystal Asteroid to 0 Flux (via weapons, Unstable Rift blast, or >= 260px Gravity Rift 950/s tidal crush) shatters it, drops a guaranteed Supply Crate, and queues a 20.0s respawn."
        )
        append(
          "- 10-Hit Orbital/Asteroid Station Defense Nova: Sector stations display a 10-segment cyan-to-magenta charge ring. Every 10 projectile impacts (or tidal implosion inside a >= 260px Gravity Rift) unleash a 16-bolt 360-degree Overload/Defense Nova damaging ships on ALL frequencies."
        )
      }
    }

    // 5. Rift Conduit / Resonance / Starting Bounty / Daily Directives Queries
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
          "- Active Resonance: Live $liveRes (${tier.title}) | Peak $peakRes | Ranked Starting Bounty: ${tier.startingBounty} BTY (Custom Rifts start at 100 BTY) | Shards: ${engine?.riftShards ?: 0L}❖"
        )
        appendLine("- Starting Bounty Ladder (100 -> 500 Max): Drift Initiate (0..149 = 100 BTY), Vanguard (150..349 = 150 BTY), Interceptor (350..649 = 225 BTY), Phantom (650..999 = 300 BTY), Rift Commander (1000..1499 = 400 BTY), Apex Archon (1500+ = 500 BTY).")
        appendLine("- Ranked Gain/Loss Math: Human PvP kills (+18 RES, +28 on HVT shutdown), Ranked match wins (+30 RES), accuracy bonus on life end; deaths/losses adjust Live Resonance while Peak Resonance permanently unlocks cosmetics & Frame B-E. Tester Mode provides 999,999❖ non-depleting Rift Shards while commercial Play Billing is dormant.")
        append("- 3 Daily Directives: Earn 40❖ + 50❖ + 60❖ plus the +100❖ 3/3 Sweep Chest = 250❖/day (with 1 free directive reroll per UTC day).")
      }
    }

    // 6. Pixel / Laser Geometry Queries
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
            "- Suppressor Rapid Twin-Ion Speed: %.0f px/s (%.0f Flux cost, %.3fs interval, %.0f Flux damage per bolt).",
            sup.primarySpeed,
            sup.primaryCost,
            sup.primaryCooldown,
            sup.primaryDamage,
          )
        )
        appendLine(
          String.format(
            Locale.US,
            "- Shaft Vector Length: %.2f world px (primarySpeed %.0f * 0.036s motion trail).",
            shaftLen,
            sup.primarySpeed,
          )
        )
        appendLine(
          String.format(
            Locale.US,
            "- Stroke Widths (at %.2fx Projectile Scale): Outer Glow = %.2f world px | Inner White Core = %.2f world px.",
            projScale,
            outerW,
            innerW,
          )
        )
        appendLine(
          String.format(
            Locale.US,
            "- Total Visual Footprint (with round endcaps): %.2f world px (%.2f screen px at default 0.65x zoom).",
            totalLen,
            totalLen * 0.65f,
          )
        )
        append(
          "- Ship Hull Hitbox & Swept Raycast: 128x128 offscreen sprite rendered at 88x88 world px with a 36.0 world px physical collision radius (72.0 px diameter) and continuous swept segment-vs-AABB anti-bleed wall detection."
        )
      }
    }

    // 7. Leaderboard / Top Pilots / Bounties / Executioner Queries
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
            "- Your Live Status: [${p.syndicateTag}] ${p.callsign} (${if (isSworn) "Sworn Pilot" else "Drifter"}) | Hull: ${p.spec.name} | Live Resonance: ${engine.liveResonance} | Live Bounty: ${p.bounty} BTY | Session K/D: ${p.kills}K/${p.deaths}D ($kd) | Shards: ${engine.riftShards}❖"
          )
        }
        if (swornAllTime.isNotEmpty()) {
          appendLine("- Top All-Time Sworn Pilot Bounties:")
          swornAllTime.forEachIndexed { i, entry ->
            val tag = if (entry.syndicateTag.isNotBlank()) "[${entry.syndicateTag}] " else ""
            appendLine(
              "  #${i + 1} $tag${entry.callsign} — Peak ${entry.allTimeBounty} BTY (${entry.allTimeKills}K / ${entry.allTimeDeaths}D)"
            )
          }
        } else if (swornPvp.isNotEmpty()) {
          appendLine("- Top PvP Sworn Pilot Bounties:")
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
            "- Only Google-authenticated Sworn Pilots are recorded on Cloud Leaderboards. AI Bots and local [RFT] Drifters are strictly excluded."
          )
        }
      }.trim()
    }

    // 8. 100-Point Rift Frame Theorycrafting / Build Queries
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
        appendLine("NEXUS 100-POINT RIFT FRAME THEORYCRAFT // (Active Budget: $budget Pts | Open at 0 Resonance)")
        appendLine("1) 100-PT HYPER-RAIL PHASE SNIPER (Frame A):")
        appendLine("   - Hull Points (65P): Speed 25P | Thrust 8P | Turn 18P | Max Flux 0P (2,200 Flux) | Recharge 14P (352 Flux/s)")
        appendLine("   - Weapons (35P): 1-Bounce Rail-Laser (15P | 26 Flux | 225 Flux DMG @ 1,280 px/s) + Phase Dash (20P | 320 Flux | 0.65s CD | +35% Next-Shot Rail Overcharge)")
        appendLine("2) 100-PT ECLIPSE STASIS ARCHON (Frame E):")
        appendLine("   - Hull Points (66P): Speed 12P | Thrust 12P | Turn 11P | Max Flux 18P (3,640 Flux) | Recharge 13P (344 Flux/s)")
        appendLine("   - Weapons (34P): Ricochet Twin-Pulse (12P | 20 Flux | 2x92 Flux DMG) + Two-Way Stasis (22P | 320 Flux | 6.0s 275px two-way Stasis, 8.5s CD)")
        append(
          "- Tip: All 10 hulls are open from 0 Resonance inside SHIPS & BUILDS (Hangar Deck). Tap any hull to ignite the Swirling NEXUS lock-in (tap again to deselect) and save up to 5 builds across Frame A through Frame E."
        )
      }
    }

    // 9. Specific Ship Queries (Ships 1..10)
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
            "NEXUS HULL DOSSIER // ${s.name.uppercase(Locale.US)} (${s.role} | ${parityPts}P Parity)"
          )
          appendLine(
            String.format(
              Locale.US,
              "- Point Allocation: SPD %dP (%.0f px/s, Burn %.0f px/s) | THR %dP (%.0f px/s^2) | TURN %dP (%.2f rad/s) | MAX FLUX %dP (%.0f Flux) | RECHARGE %dP (%.0f Flux/s)",
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
              "- Primary [%s | %dP]: %.0f Flux cost | %.2fs CD | %.0f Flux damage | %.0f px/s (%d stream%s, %d bounce%s)",
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
                "- Secondary [%s | %dP]: 320 Flux cost | %.1fs CD | Anchors a %.0fpx radius (%.0fpx diameter) two-way Stasis for %.1fs. Ships bounce off both sides with 0 Flux wall damage; reflects ordnance inside and outside.",
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
                "- Secondary [%s | %dP]: 20 Flux entry + %.0f Flux/s cloak drain (50%% cloaked afterburner drain, 4.5s CD, floors at 150 Flux). Uncloaking manually fires a 0 Flux %.0fpx 360-degree EMP pulse that drains %.0f%% enemy Max Flux (up to 850 Flux) and freezes recharge for %.1fs.",
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
                "- Secondary [%s | %dP]: %.0f Flux cost | %.2fs CD | %.0f Flux damage | %.0f px/s recoil",
                s.secondaryTitle,
                s.secondaryPts,
                s.bombCost,
                s.bombCooldown,
                s.bombDamage,
                s.bombRecoil,
              )
            )
          }
          append("- Tactical Note: ${s.tacticalLore}")
        }
      }
    }

    // 10. Game Mode & Custom Game Queries
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
        appendLine("1) SECTOR CONVERGENCE (FFA): 20-pilot open arena (60m cycle) with 5x (180px) DS9 Safe-Zone Harbor ON. #1 Bounty with >50% presence wins +100❖.")
        appendLine("2) RIFT SKIRMISH (Team Deathmatch): 2v2-5v5 Best-of-3 rounds. Safe Zone OFF, Ship Lock ON. Eliminated pilots use the Living-Teammate Spectator Box (prev/next pills or Gamepad LB/RB). +75❖.")
        appendLine("3) FLUX CORE: Hold the center Flux Core for +15 Bounty/s and up to Tier 10 Overcharge (+50% DMG/Recharge, +40% Speed, 2x pulsing Gold radar beacon). 5x DS9 Safe-Zone Harbor ON (entering ejects the Core at 1,650 px/s).")
        appendLine("4) STASIS BREACH (10v10 Circular Stasis Jail): Safe Zone OFF. Eliminated enemies warp into your team's circular Stasis Jail; prisoners deal 100% weapon damage to the 8,000 Flux Stasis Core to detonate a breakout shockwave. +100❖.")
        appendLine("5) RIFT CONTROL (Tug-of-War): Contest Rift Alpha & Rift Omega with NO Safe Zone anywhere on the map and a Graphical Dual-Rift Tug-of-War Top HUD Bar. +85❖.")
        appendLine("6) SYNDICATE WARS: Ranked Syndicate-vs-Syndicate ladder for 100% same-Syndicate Strike Wings. Safe Zone OFF, Ship Lock ON. Awards +75❖/+100❖ & War Trophies.")
        append("- OVERWATCH & CUSTOM RIFTS: Deploy in 1 tap via [QUICK ZONE IN // RIFT #XX] on the NEXUS Tablet Overwatch aperture, or open [ALL MODES] -> Hangar Hull Lock-In -> Enter Rift. Custom Rifts support 6-char room codes, MegaFat Bullets, AI Bots ON/OFF, standardized 100 starting bounty, and full speedMult/damageMult scaling across ships, primary blasters, Seeker Torpedoes, ordnance, and Swarm Drones.")
      }
    }

    // 11. Buffs, Swarm Drones, Stacked Barriers & Supply Crates Queries
    if (
      lower.contains("buff") ||
        lower.contains("crate") ||
        lower.contains("swarm") ||
        lower.contains("drone") ||
        lower.contains("barrier") ||
        lower.contains("seeker") ||
        lower.contains("upgrade") ||
        lower.contains("powerup") ||
        lower.contains("turn radius") ||
        lower.contains("overcharge")
    ) {
      return buildString {
        appendLine("NEXUS 12-BUFF SUPPLY CRATE & SPECIAL ORDNANCE TELEMETRY //")
        appendLine("- 5 Stackable Hull Stat Upgrades (up to 3x stacks each) + Primary Ammo:")
        appendLine("  - SPD (+8% Top Speed/stack) | THR (+8% Thrust/stack) | RCH (+8% Flux Recharge/stack) | TURN (+12% Turn Rate/stack) | MAX FLUX (+10% Max Flux & instant +10% Flux heal/stack)")
        appendLine("  - BNC (+30 Bouncing Primary Shots) & MULTI (+25 Multi-Shot Ammo).")
        appendLine("- 4th Right-Hand Special Buff Slot Pickups (including SWARM):")
        append(
          "  - SWARM (8% drop | 5 autonomous zig-zagging hunter drones, 210 Flux dmg each, 560 px/s, 6.5s life), Fast Predictive Seeker Torpedo (1x | high-G predictive lead homing, 880 Flux dmg), Shatter Cone (2x | 3 waves of bouncing shards), Nova Ring (1x | 16 radial ricochet bolts), Stackable Stasis Barrier (1x | 45.0s tethered cyan hard-light forcefield; deploying within 78px stacks into BARRIER xN), Hyper Surge (1x | 0 Flux speed burn), Holo Phantom (1x | decoy clone), and custom Forged Buffs."
        )
      }
    }

    // 12. General / Meta / Live Status Summary
    val modeTitle =
      engine?.currentCustomGame?.let { "Custom Rift: ${it.title} (${it.baseMode.title})" }
        ?: engine?.activeRankedMode?.title
        ?: "Sector Convergence"
    return buildString {
      appendLine("NEXUS TACTICAL TELEMETRY // SCI-FI COMMAND TABLET CORE READY (BUILD #72)")
      if (engine != null) {
        val p = engine.localPlayer
        val shipSpec = p.spec
        val tier = RiftProgressionCatalog.getConduitTierForResonance(engine.liveResonance)
        appendLine(
          String.format(
            Locale.US,
            "- Active Hull: %s (%s) | Flux: %.0f / %.0f (%.0f Flux/s) | Bounty: %d BTY | Resonance: %d (%s) | Shards: %d❖",
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
          "- Active Sector: $modeTitle | 5x DS9 Safe Harbor Active: ${engine.hasActiveSafeZone()} | Pilot Status: ${if (isSworn) "Sworn Pilot" else "[RFT] Drifter"}"
        )
      }
      appendLine("- NEXUS Tablet Hub: 6 Metallic Station Cards (SHIPS & BUILDS, RANKS & QUESTS, SKINS & AUDIO, CHAT & NEXUS AI, CLANS & SQUADS, STATS & CODEX) + Round Pinch-to-Zoom (1.0x..6.0x) Highest-Resonance Live Overwatch.")
      appendLine("- All 10 Ships (Open at 0 Resonance | 100-Point Parity): Viper, Ricochet, Suppressor, Goliath, Twinfang, Disruptor (36 Flux/s Cloak + 0 Flux EMP), Valkyrie, Manta, Eclipse (6.0s 275px Two-Way Stasis, 320 Flux, 8.5s CD), and modular Rift Frame (100 Pts + Frame A-E).")
      append(
        "- Ask about any ship's exact pixel/Flux math, 440px Gravity Rift swept growth & >= 260px 950/s Tidal Implosion, 5x (180px) DS9 Safe-Zone Station, Unstable Rift 10.0s Doomsday merges, 5-drone SWARM, 2,400 Flux Crystal Asteroids, 10-hit Station Novas, or Sworn Pilot Leaderboards."
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
