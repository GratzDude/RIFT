package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.RiftApplication
import com.example.data.LeaderboardEntry
import com.example.data.LocalPilotPrefs
import com.example.engine.CustomHullVisualRecipe
import com.example.engine.RiftCalibrationState
import com.example.engine.RiftCodex
import com.example.engine.RiftGameEngine
import com.example.engine.RiftNexusKnowledge
import com.example.engine.RiftProgressionCatalog
import com.example.engine.SpecialBuffRegistryEntry
import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.content
import java.util.Locale
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.max
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

private const val TAG = "NexusAiViewModel"

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
 * Live Firebase AI Logic (`gemini-4-hydrogen` primary + `gemini-3.8-flash` cloud fallback +
 * offline on-device tactical responder) Tactical Intelligence Engine for `#rift-ai`,
 * backed by App Check attestation and grounded in real-time RIFT sector telemetry.
 */
class NexusAiViewModel : ViewModel() {

  private val primaryModel: GenerativeModel by lazy {
    Firebase.ai.generativeModel(
      modelName = "gemini-4-hydrogen",
      systemInstruction = content { text(RiftNexusKnowledge.NEXUS_SYSTEM_INSTRUCTION) },
    )
  }

  private val fallbackCloudModel: GenerativeModel by lazy {
    Firebase.ai.generativeModel(
      modelName = "gemini-3.8-flash",
      systemInstruction = content { text(RiftNexusKnowledge.NEXUS_SYSTEM_INSTRUCTION) },
    )
  }

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
    val fallbackSnapshot = buildEngineQuickSnapshot(engine, pvpLeaderboard, allTimeLeaderboard)
    streamGeminiResponse(
      rawQuestion = userQuestion,
      liveTelemetrySnapshot = fallbackSnapshot,
      onStatsMutated = onStatsMutated,
    )
  }

  fun askNexus(rawQuestion: String, liveTelemetrySnapshot: String) {
    val effectiveSnapshot =
      liveTelemetrySnapshot.ifBlank {
        cachedEngine?.let { eng ->
          buildEngineQuickSnapshot(eng, cachedPvpLeaderboard, cachedAllTimeLeaderboard)
        }.orEmpty()
      }
    streamGeminiResponse(
      rawQuestion = rawQuestion,
      liveTelemetrySnapshot = effectiveSnapshot,
    )
  }

  private suspend fun collectModelStream(
    targetModel: GenerativeModel,
    historyContents: List<Content>,
    promptWithGroundTruth: String,
    streamingNexusId: Long,
    accumulatedText: StringBuilder,
  ) {
    withTimeout(45_000L) {
      val stream =
        if (historyContents.isEmpty()) {
          targetModel.generateContentStream(promptWithGroundTruth)
        } else {
          val chat = targetModel.startChat(history = historyContents)
          chat.sendMessageStream(promptWithGroundTruth)
        }
      stream.collect { chunk ->
        val chunkText = chunk.text.orEmpty()
        if (chunkText.isNotEmpty()) {
          accumulatedText.append(chunkText)
          val partial = accumulatedText.toString()
          _streamState.value = NexusStreamState.Streaming(partial)
          updateStreamingMessage(streamingNexusId, partial, stillStreaming = true)
        }
      }
    }
  }

  private fun streamGeminiResponse(
    rawQuestion: String,
    liveTelemetrySnapshot: String,
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

    if (RiftApplication.isUsingFallbackFirebase) {
      val offlineReply = buildOfflineTacticalResponse(question, liveTelemetrySnapshot)
      updateStreamingMessage(streamingNexusId, offlineReply, stillStreaming = false)
      _streamState.value = NexusStreamState.Success(offlineReply)
      onStatsMutated()
      return
    }

    activeJob =
      viewModelScope.launch {
        val accumulatedText = StringBuilder()
        val historyContents =
          priorCompletedMessages.takeLast(10).map { msg ->
            content(role = if (msg.role == NexusRole.USER) "user" else "model") {
              text(msg.text)
            }
          }
        val promptWithGroundTruth =
          if (liveTelemetrySnapshot.isNotBlank()) {
            buildString(liveTelemetrySnapshot.length + question.length + 128) {
              appendLine(liveTelemetrySnapshot)
              appendLine()
              appendLine("=== [PILOT QUERY] ===")
              append(question)
            }
          } else {
            question
          }

        var retryWithFallbackCloud = false
        try {
          collectModelStream(
            targetModel = primaryModel,
            historyContents = historyContents,
            promptWithGroundTruth = promptWithGroundTruth,
            streamingNexusId = streamingNexusId,
            accumulatedText = accumulatedText,
          )
        } catch (e: TimeoutCancellationException) {
          Log.w(TAG, "Primary gemini-4-hydrogen stream timed out", e)
          if (accumulatedText.isEmpty()) {
            retryWithFallbackCloud = true
          }
        } catch (e: CancellationException) {
          throw e
        } catch (e: Exception) {
          Log.w(TAG, "Primary gemini-4-hydrogen stream failed; retrying gemini-3.8-flash", e)
          if (accumulatedText.isEmpty()) {
            retryWithFallbackCloud = true
          }
        }

        if (retryWithFallbackCloud && accumulatedText.isEmpty()) {
          try {
            collectModelStream(
              targetModel = fallbackCloudModel,
              historyContents = historyContents,
              promptWithGroundTruth = promptWithGroundTruth,
              streamingNexusId = streamingNexusId,
              accumulatedText = accumulatedText,
            )
          } catch (e: TimeoutCancellationException) {
            Log.w(TAG, "Fallback gemini-3.8-flash stream timed out; using offline tactical responder", e)
          } catch (e: CancellationException) {
            throw e
          } catch (e: Exception) {
            Log.w(TAG, "Fallback gemini-3.8-flash stream failed; using offline tactical responder", e)
          }
        }

        val finalText = accumulatedText.toString().trim()
        if (finalText.isNotBlank()) {
          updateStreamingMessage(streamingNexusId, finalText, stillStreaming = false)
          _streamState.value = NexusStreamState.Success(finalText)
          onStatsMutated()
        } else {
          val offlineReply = buildOfflineTacticalResponse(question, liveTelemetrySnapshot)
          updateStreamingMessage(streamingNexusId, offlineReply, stillStreaming = false)
          _streamState.value = NexusStreamState.Success(offlineReply)
          onStatsMutated()
        }
      }
  }

  fun retryLastQuestion(liveTelemetrySnapshot: String = "") {
    val failed = lastFailedQuestion ?: return
    if (liveTelemetrySnapshot.isNotBlank()) {
      askNexus(failed, liveTelemetrySnapshot)
    } else {
      val eng = cachedEngine
      if (eng != null) {
        askNexus(failed, eng, cachedPvpLeaderboard, cachedAllTimeLeaderboard)
      } else {
        askNexus(failed, "")
      }
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

  private fun buildEngineQuickSnapshot(
    engine: RiftGameEngine,
    pvpLeaderboard: List<LeaderboardEntry>,
    allTimeLeaderboard: List<LeaderboardEntry>,
  ): String {
    val p = engine.localPlayer
    val liveRes = engine.liveResonance
    val peakRes = max(liveRes, engine.peakResonance)
    val conduitTier = RiftProgressionCatalog.getConduitTierForResonance(liveRes)
    val nextTier = RiftProgressionCatalog.getNextConduitTierThreshold(liveRes)
    val startRes = engine.getLocalStartingBounty()
    val mergedBoard =
      (allTimeLeaderboard + pvpLeaderboard)
        .distinctBy { "${it.userId}|${it.callsign}" }
        .filter {
          val tag = it.syndicateTag.trim().uppercase(Locale.US)
          tag !in LocalPilotPrefs.NON_PLAYER_SYNDICATE_TAGS && !it.callsign.contains("[BOT]")
        }
    return buildString {
      appendLine("=== [LIVE RIFT TELEMETRY SUMMARY — BUILD #$CURRENT_RIFT_BUILD_VERSION] ===")
      appendLine(
        "Pilot: [${p.syndicateTag}] ${p.callsign} | Conduit Rank Tier: ${conduitTier.title} (Next: ${if (liveRes >= 1500) "MAX TIER" else "${nextTier.first} -> ${nextTier.second}"}) | Active Hull: ${engine.formatShipLabel(p)} (${p.spec.role}) | Flux: ${p.flux.toInt()}/${p.maxFlux.toInt()} | Live Resonance: $liveRes | Peak Resonance: $peakRes | Starting Match Resonance: $startRes | Rift Shards: ${engine.riftShards}"
      )
      appendLine(
        "Active Mode: ${engine.currentCustomGame?.let { "Custom Rift ${it.title} (${it.baseMode.title})" } ?: engine.activeRankedMode.title} | Safe Harbor Active: ${engine.hasActiveSafeZone()}"
      )
      if (mergedBoard.isNotEmpty()) {
        appendLine("Top Sworn Pilots:")
        mergedBoard.take(5).forEachIndexed { idx, entry ->
          val tag = if (entry.syndicateTag.isNotBlank()) "[${entry.syndicateTag}] " else ""
          appendLine("  #${idx + 1} $tag${entry.callsign} — ${entry.allTimeBounty} Peak Resonance")
        }
      }
    }
  }

  private fun buildOfflineTacticalResponse(
    question: String,
    liveTelemetrySnapshot: String,
  ): String {
    val q = question.trim().lowercase(Locale.US)
    val eng = cachedEngine
    val allShips = RiftCodex.getAllTenShips()
    fun cleanName(id: Int, raw: String): String =
      if (id == 10) "Rift Frame" else raw.replace(Regex("^#?[0-9]+[-\\s]*"), "").trim()

    fun hasAny(vararg terms: String): Boolean = terms.any { q.contains(it) }

    val sb = StringBuilder()
    sb.appendLine("RIFT AI // LOCAL SECTOR TELEMETRY (BUILD #$CURRENT_RIFT_BUILD_VERSION)")

    // 1. Goliath / Siege Bomb / Detonate air-burst
    if (hasAny("goliath", "siege", "detonate", "air-burst", "airburst", "remote bomb")) {
      val goliath = RiftCodex.getShipById(4)
      val radiusPx = 24f * RiftCalibrationState.projectileScale
      sb.appendLine()
      sb.appendLine("GOLIATH & SIEGE BOMB AIR-BURST:")
      sb.appendLine(
        "- Hull: Max Flux ${goliath.maxFlux.toInt()}, Recharge ${goliath.rechargeRate.toInt()} Flux/s, Top Speed ${goliath.topSpeed.toInt()} px/s (Afterburner ${goliath.afterburnerSpeed.toInt()} px/s)."
      )
      sb.appendLine(
        "- Siege Bomb: ${goliath.bombCost.toInt()} Flux launch cost, ${String.format(Locale.US, "%.2f", goliath.bombCooldown)}s reload, ${goliath.bombDamage.toInt()} Flux blast damage, ${String.format(Locale.US, "%.1f", radiusPx)} world px warhead radius."
      )
      sb.appendLine(
        "- Live 0-Flux Detonate Button: While your Siege Bomb is airborne (on Goliath or a SIEGE Rift Frame), the Secondary button stays at full active opacity, glows blazing solar-orange, and switches to DETONATE (0 Flux) even during reload cooldown or at 0 remaining Flux. Tap it on finger-down for instant mid-air detonation without entering aim-drag mode."
      )
      return sb.toString().trim()
    }

    // 2. Disruptor / Cloak / EMP
    if (hasAny("disruptor", "cloak", "emp", "stealth", "decloak")) {
      val dis = RiftCodex.getShipById(6)
      sb.appendLine()
      sb.appendLine("DISRUPTOR CLOAK & 0-FLUX EMP:")
      sb.appendLine(
        "- Hull: Max Flux ${dis.maxFlux.toInt()}, Recharge ${dis.rechargeRate.toInt()} Flux/s, Top Speed ${dis.topSpeed.toInt()} px/s."
      )
      sb.appendLine(
        "- Cloak Entry & Drain: 20 Flux entry cost, ${RiftCalibrationState.cloakDrainPerSec.toInt()} Flux/s cloak drain (50% cloaked afterburner drain), 4.5s cooldown, and a 150 Flux safety floor."
      )
      sb.appendLine(
        "- Manual Decloak EMP: Re-pressing Secondary while cloaked fires a 0-Flux ${RiftCalibrationState.empPulseRadius.toInt()} world px 360-degree EMP draining ${(RiftCalibrationState.empFluxDrainPct * 100f).toInt()}% Max Flux (capped at 850 Flux) plus a ${String.format(Locale.US, "%.1f", RiftCalibrationState.empFreezeSec)}s recharge freeze."
      )
      return sb.toString().trim()
    }

    // 3. Eclipse / Stasis / Interdictor Dome
    if (hasAny("eclipse", "stasis", "interdictor", "dome", "bubble")) {
      val ecl = RiftCodex.getShipById(9)
      val rad = RiftCalibrationState.interdictorBubbleRadius.toInt()
      sb.appendLine()
      sb.appendLine("ECLIPSE TWO-WAY STASIS:")
      sb.appendLine(
        "- Hull: Max Flux ${ecl.maxFlux.toInt()}, Recharge ${ecl.rechargeRate.toInt()} Flux/s, Top Speed ${ecl.topSpeed.toInt()} px/s."
      )
      sb.appendLine(
        "- Stasis Field: ${rad} world px radius (${rad * 2} world px diameter) two-way containment field lasting ${String.format(Locale.US, "%.1f", RiftCalibrationState.interdictorBubbleDuration)}s at ${ecl.bombCost.toInt()} Flux cost (${String.format(Locale.US, "%.1f", RiftCalibrationState.interdictorBubbleCooldown)}s cooldown). Projectiles cannot cross the boundary in either direction, isolating duels or shielding your Squadron."
      )
      return sb.toString().trim()
    }

    // 4. Specific named ship lookup (Viper, Ricochet, Suppressor, Twinfang, Valkyrie, Manta)
    val matchedShip =
      allShips.firstOrNull { spec ->
        val cname = cleanName(spec.id, spec.name).lowercase(Locale.US)
        spec.id != 10 && q.contains(cname)
      }
    if (matchedShip != null) {
      val cname = cleanName(matchedShip.id, matchedShip.name)
      val shotsPerSec = if (matchedShip.primaryCooldown > 0.001f) 1f / matchedShip.primaryCooldown else 0f
      val volleyDmg = matchedShip.primaryDamage * matchedShip.primaryStreams
      val rawDps = volleyDmg * shotsPerSec
      val boltLen = matchedShip.primarySpeed * 0.036f + 6.4f * RiftCalibrationState.projectileScale
      sb.appendLine()
      sb.appendLine("$cname (${matchedShip.role}) — 100-POINT PARITY SPEC:")
      sb.appendLine(
        "- Allocation: Speed ${matchedShip.speedPts}, Thrust ${matchedShip.thrustPts}, Turn ${matchedShip.turnPts}, Max Flux ${matchedShip.maxFluxPts}, Regen ${matchedShip.rechargePts}, Primary ${matchedShip.primaryPts}, Secondary ${matchedShip.secondaryPts} (${matchedShip.totalAllocatedPoints} Pts)."
      )
      sb.appendLine(
        "- Hull: Max Flux ${matchedShip.maxFlux.toInt()}, Recharge ${matchedShip.rechargeRate.toInt()} Flux/s, Top Speed ${matchedShip.topSpeed.toInt()} px/s (Afterburner ${matchedShip.afterburnerSpeed.toInt()} px/s), Thrust ${matchedShip.thrust.toInt()} px/s^2, Turn ${String.format(Locale.US, "%.2f", matchedShip.turnRate)} rad/s."
      )
      sb.appendLine(
        "- Primary [${matchedShip.primaryTitle}]: ${matchedShip.primaryStreams} stream(s), ${matchedShip.primaryDamage.toInt()} Flux/stream (${volleyDmg.toInt()} Flux/volley), Cost ${matchedShip.primaryCost.toInt()} Flux, Cooldown ${String.format(Locale.US, "%.3f", matchedShip.primaryCooldown)}s, Speed ${matchedShip.primarySpeed.toInt()} px/s, Bounces ${matchedShip.primaryBounces}, Raw DPS ${rawDps.toInt()} Flux/s, Bolt ${String.format(Locale.US, "%.1f", boltLen)} world px."
      )
      if (matchedShip.hasBomb) {
        sb.appendLine(
          "- Secondary [${matchedShip.secondaryTitle}]: Cost ${matchedShip.bombCost.toInt()} Flux, Cooldown ${String.format(Locale.US, "%.2f", matchedShip.bombCooldown)}s, Damage ${matchedShip.bombDamage.toInt()} Flux, Wall Bounces ${matchedShip.bombBounces}."
        )
      } else {
        sb.appendLine("- Secondary: None (all 100 points invested into hull and Primary firepower).")
      }
      sb.appendLine("- Tactical Profile: ${matchedShip.tacticalLore}")
      return sb.toString().trim()
    }

    // 5. Rift Frame / 100-Point Drydock / Frame A-E builds
    if (hasAny("rift frame", "drydock", "frame a", "frame b", "frame c", "frame d", "frame e", "100-point", "100 point", "custom ship", "build")) {
      sb.appendLine()
      sb.appendLine("100-POINT RIFT FRAME DRYDOCK & FRAMES A-E:")
      sb.appendLine(
        "- All 10 ships including Ship #10 Rift Frame are open immediately at 0 Resonance with full 100-Point parity."
      )
      sb.appendLine(
        "- Overwatch Drydock: Drag-and-drop from 9 Primary + 23 Secondary pre-baked hardware icons into the Primary and Secondary sockets, then tune the 5 hull sliders (FLUX, REGEN, SPEED, THRUST, TURN) inside the exact 100-Point budget and tap CHARGE RIFT FRAME."
      )
      sb.appendLine(
        "- Slot Economy: Frame A is always active from 0 Resonance; Frame B is a permanent 250 Rift Shard unlock; Frame C (650), Frame D (1,100), and Frame E (1,600) activate strictly while Live Resonance meets their threshold and fall back cleanly to Frame A if Live Resonance drops."
      )
      return sb.toString().trim()
    }

    // 6. Playfield Rifts / Gravity Implosion / Unstable Rift / Storm Wall
    if (hasAny("rift", "gravity", "repulsor", "phase", "unstable", "storm wall", "plasma rift", "implosion", "anomaly")) {
      sb.appendLine()
      sb.appendLine("VOLUMETRIC PLAYFIELD RIFTS & OUTER STORM WALL:")
      sb.appendLine(
        "- Plasma Rift (Crimson/Magenta): Thermal burn draining ${(RiftCalibrationState.plasmaRiftDamagePct * 100f).toInt()}% Max Flux per 0.6s inside the storm core."
      )
      sb.appendLine(
        "- Repulsor Rift (Cobalt-to-Ice-Blue): ${RiftCalibrationState.repulsorFieldRadius.toInt()} world px outward deflection plus inner-core 1.45x afterburner supersonic slingshot."
      )
      sb.appendLine(
        "- Gravity Rift (Indigo/Violet): ${RiftCalibrationState.gravityPullRadius.toInt()} world px pull. Swept 0.92x visible-disc weapon absorption grows the Rift (+18px/bolt, +40px/bomb or torpedo, +14px/drone) up to 440px max; at >= 260px it pulls Crystal Asteroids, Stations, Debris, and Stasis Barriers at 950/s tidal crush to implosion, dropping a Supply Crate (20.0s respawn) and firing a 16-bolt 360-degree Station Overload Nova."
      )
      sb.appendLine(
        "- Paired Phase Rifts (Teal/Cyan A & B): Instant warp gateway with ${String.format(Locale.US, "%.2f", RiftCalibrationState.phaseRiftCooldownSec)}s re-entry cooldown."
      )
      sb.appendLine(
        "- Unstable Rift (Multi-Spectrum): Overlapping Rifts merge into a 10.0s convergence singularity that detonates for full-sector insta-kill outside Rift Station Safe Harbor and shatters nearby Crystal Asteroids. Outer 8000x8000 arena perimeter renders the seamless STORM_BOUNDARY_AGSL cloud wall with 220px proximity arc lightning."
      )
      return sb.toString().trim()
    }

    // 7. Rift Station Safe Harbor / Defense Nova / Crystal Asteroids
    if (hasAny("station", "safe harbor", "safe zone", "nova", "asteroid", "crystal", "backfire")) {
      sb.appendLine()
      sb.appendLine("5X RIFT STATION SAFE HARBOR, CRYSTAL ASTEROIDS & 10-HIT DEFENSE NOVAS:")
      sb.appendLine(
        "- Center Rift Station: 5x ship-scale (180 world px radius) normal-mapped metallic hub anchoring Safe Harbor in Sector Convergence and Flux Core (15s max stay, weapons disarmed inside, disabled in Rift Skirmish, Stasis Breach, Rift Control, and Syndicate Wars)."
      )
      sb.appendLine(
        "- Crystal Asteroids: 2,400 Flux integrity with an on-rock integrity ring; shattering one guarantees a Supply Crate and queues a 20.0s respawn."
      )
      sb.appendLine(
        "- 10-Hit Orbital & Asteroid Station Defense Nova: Every 10 weapon impacts unleash a 16-bolt 360-degree Defense Nova (+10% Resonance trigger credit on enemy eliminations, -15% Resonance self-backfire penalty if eliminated by your own triggered Nova)."
      )
      return sb.toString().trim()
    }

    // 8. Modes / Custom Rifts
    if (hasAny("mode", "ranked", "convergence", "skirmish", "flux core", "breach", "control", "syndicate war", "custom")) {
      sb.appendLine()
      sb.appendLine("6 OFFICIAL RANKED MODES & CUSTOM RIFTS:")
      sb.appendLine("- 1. Sector Convergence: 20-Pilot Ranked FFA / 1-5 Pilot Squadron | Safe Harbor ON | +100 Rift Shards.")
      sb.appendLine("- 2. Rift Skirmish: 2v2-5v5 Best-of-3 Team Deathmatch (180 BPM Final 1v1 Overdrive, Ship Lock ON, Teammate Spectator) | Safe Harbor OFF | +75 Rift Shards.")
      sb.appendLine("- 3. Flux Core: 20-Pilot Powerball FFA | Safe Harbor ON | Tier 1-10 Overcharge (+15 Resonance/s) | Core ejects on death or Safe Harbor entry.")
      sb.appendLine("- 4. Stasis Breach: 10v10 Circular Stasis Jailbreak | Safe Harbor OFF | Prisoners damage the ${RiftCalibrationState.stasisBrigReactorMaxHp.toInt()} Flux Stasis Core at 100% to trigger a breakout shockwave | +100 Rift Shards.")
      sb.appendLine("- 5. Rift Control: Ranked Tug-of-War over Rift Alpha & Rift Omega with dual-rift HUD bar | Safe Harbor OFF | +85 Rift Shards.")
      sb.appendLine("- 6. Syndicate Wars: Direct Syndicate-vs-Syndicate Ladder (Ship Lock ON, 100% same-Syndicate Squadron) ranked by Syndicate War Trophies.")
      sb.appendLine("- Custom Rifts: Public/Private rooms with 6-character Join Codes, 2x3 base-mode chips, 5%-stepped 0%..100% Damage Taken & Weapon Flux Cost scales, Bot Count/Skill, and custom combat multipliers.")
      return sb.toString().trim()
    }

    // 9. Weapons / DPS / Buffs / Crates / Pixel specs
    if (hasAny("weapon", "dps", "damage", "pixel", "bolt", "buff", "crate", "supply", "repel", "swarm", "barrier")) {
      val projScale = RiftCalibrationState.projectileScale
      sb.appendLine()
      sb.appendLine("WEAPON PIXEL SPECS, SUPPLY CRATES & COMBAT MANEUVERS:")
      sb.appendLine(
        "- Primary Bolt Geometry: Shaft Length = primarySpeed * 0.036 world px, Outer Glow = ${String.format(Locale.US, "%.2f", 6.4f * projScale)} world px, Inner Core = ${String.format(Locale.US, "%.2f", 2.8f * projScale)} world px with swept segment-vs-AABB collision."
      )
      sb.appendLine(
        "- Repel Shockwave: ${RiftCalibrationState.repelCost.toInt()} Flux cost, ${RiftCalibrationState.repelRadius.toInt()} world px 360-degree push. Glancing Wall Barrel Roll triggers a 0.48s evasive 360-degree roll on tangential wall graze."
      )
      sb.appendLine(
        "- Supply Pool (${RiftCodex.POWERUP_CODEX.size} Buffs): " +
          RiftCodex.POWERUP_CODEX.take(6).joinToString(", ") { "${it.title} (${it.dropRatePercent}%)" } +
          ", plus Multi-Shot, Seeker Torpedo, Swarm Drones, Stackable Stasis Barrier, and Overcharge stacks."
      )
      return sb.toString().trim()
    }

    // 10. Economy / Shards / Cosmetics / Conduit Tiers / Accuracy Resonance / Leaderboard / My Stats
    if (hasAny("resonance", "conduit", "shard", "store", "cosmetic", "skin", "halo", "finisher", "warp", "leaderboard", "top", "accuracy", "directive", "stat", "profile", "dossier", "my ")) {
      if (eng != null) {
        val p = eng.localPlayer
        val liveRes = eng.liveResonance
        val peakRes = max(liveRes, eng.peakResonance)
        val tier = RiftProgressionCatalog.getConduitTierForResonance(liveRes)
        val next = RiftProgressionCatalog.getNextConduitTierThreshold(liveRes)
        val startRes = eng.getLocalStartingBounty()
        sb.appendLine()
        sb.appendLine("ACTIVE PILOT STATUS:")
        sb.appendLine(
          "- Pilot: ${p.callsign} | Conduit Rank Tier: ${tier.title} (${if (liveRes >= 1500) "MAX TIER" else "Next: ${next.first} Resonance -> ${next.second}"})"
        )
        sb.appendLine(
          "- Resonance: Live $liveRes | Peak $peakRes | Starting Match Resonance $startRes | Active Hull: ${eng.formatShipLabel(p)} (${eng.getShipResonance(p.shipId)} Resonance) | Rift Shards: ${eng.riftShards}"
        )
      }
      val realBoard =
        (cachedAllTimeLeaderboard + cachedPvpLeaderboard)
          .distinctBy { "${it.userId}|${it.callsign}" }
          .filter {
            val tag = it.syndicateTag.trim().uppercase(Locale.US)
            tag !in LocalPilotPrefs.NON_PLAYER_SYNDICATE_TAGS && !it.callsign.contains("[BOT]")
          }
          .sortedByDescending { it.allTimeBounty }
      if (realBoard.isNotEmpty()) {
        sb.appendLine()
        sb.appendLine("TOP SWORN PILOTS:")
        realBoard.take(5).forEachIndexed { idx, entry ->
          val tag = if (entry.syndicateTag.isNotBlank()) "[${entry.syndicateTag}] " else ""
          sb.appendLine("  #${idx + 1} $tag${entry.callsign} — ${entry.allTimeBounty} Peak Resonance (${entry.allTimeKills}K/${entry.allTimeDeaths}D)")
        }
      }
      sb.appendLine()
      sb.appendLine("RESONANCE ECONOMY & DAILY DIRECTIVES:")
      sb.appendLine(
        "- Progression uses 100% Live Resonance & 3 Daily Directives (up to 250 Rift Shards/day including the +100 Shard 3/3 Sweep Chest). Conduit Rank Tiers scale Starting Match Resonance from 100 (Drift Initiate) up to 500 (Apex Archon at 1,500+ Live Resonance)."
      )
      sb.appendLine(
        "- Post-Life Ranked Accuracy Resonance: On hull loss after 8+ shots fired, >= 45% accuracy awards HULL LOST • +4 ACC and < 15% accuracy applies HULL LOST • -2 ACC."
      )
      sb.appendLine(
        "- Economy Split: Frame B (250 Shards), Ship Skins 1..5 (350..850 Shards), and post-1st-free Callsign renames (250 Shards) are permanent Rift Shard Store unlocks. Frame C/D/E (650/1100/1600), Kill Finishers 1..4 (200/550/1050/1650), Warp Signatures 1..3 (350/750/1300), and Leader Halos 1..3 (500/1000/1750) activate strictly while Live Resonance meets their threshold."
      )
      return sb.toString().trim()
    }

    // 11. All 10 Ships roster overview
    if (hasAny("ship", "hull", "roster", "all 10", "best ship", "compare")) {
      sb.appendLine()
      sb.appendLine("10-SHIP ROSTER (ALL 10 OPEN AT 0 RESONANCE — 100-POINT PARITY):")
      for (spec in allShips) {
        val cname = cleanName(spec.id, spec.name)
        sb.appendLine(
          "- $cname (${spec.role}): Flux ${spec.maxFlux.toInt()}, Speed ${spec.topSpeed.toInt()} px/s, Primary ${spec.primaryTitle}, Secondary ${if (spec.hasBomb) spec.secondaryTitle else "None"}."
        )
      }
      return sb.toString().trim()
    }

    // 12. General RIFT tactical brief (grounded in live snapshot & engine)
    sb.appendLine()
    if (eng != null) {
      val p = eng.localPlayer
      val liveRes = eng.liveResonance
      val peakRes = max(liveRes, eng.peakResonance)
      val tier = RiftProgressionCatalog.getConduitTierForResonance(liveRes)
      sb.appendLine(
        "Active Telemetry: ${p.callsign} flying ${eng.formatShipLabel(p)} | Conduit Tier: ${tier.title} | Live Resonance: $liveRes (Peak $peakRes) | Rift Shards: ${eng.riftShards}."
      )
    } else if (liveTelemetrySnapshot.isNotBlank()) {
      val activeLine =
        liveTelemetrySnapshot
          .lineSequence()
          .firstOrNull { it.contains("Active Pilot:") || it.startsWith("Pilot:") }
      if (!activeLine.isNullOrBlank()) {
        sb.appendLine(activeLine.trim())
      }
    }
    sb.appendLine(
      "Local tactical database online. Ask about any of the 10 starfighters (Viper through Rift Frame), 100-Point Drydock builds, Goliath 0-Flux DETONATE air-bursts, Disruptor Cloak & EMP, Eclipse two-way Stasis, Playfield Rifts & 440px Gravity implosion, 5x Rift Station Safe Harbor & 10-Hit Defense Novas, the 6 Ranked Modes, Custom Rifts, or Live-Resonance vs. Shard Store unlocks."
    )
    return sb.toString().trim()
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
}
