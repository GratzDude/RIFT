package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.LeaderboardEntry
import com.example.data.LocalPilotPrefs
import com.example.engine.CustomHullVisualRecipe
import com.example.engine.RiftCalibrationState
import com.example.engine.RiftGameEngine
import com.example.engine.RiftNexusKnowledge
import com.example.engine.SpecialBuffRegistryEntry
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.content
import java.util.Locale
import kotlin.coroutines.cancellation.CancellationException
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
 * Live Firebase AI Logic (`gemini-3.8-flash`) Tactical Intelligence Engine for `#rift-ai`,
 * backed by App Check attestation and grounded in real-time RIFT sector telemetry.
 */
class NexusAiViewModel : ViewModel() {

  private val model by lazy {
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

    activeJob =
      viewModelScope.launch {
        val accumulatedText = StringBuilder()
        try {
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

          withTimeout(60_000L) {
            val stream =
              if (historyContents.isEmpty()) {
                model.generateContentStream(promptWithGroundTruth)
              } else {
                val chat = model.startChat(history = historyContents)
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

          val finalText = accumulatedText.toString().trim()
          if (finalText.isNotBlank()) {
            updateStreamingMessage(streamingNexusId, finalText, stillStreaming = false)
            _streamState.value = NexusStreamState.Success(finalText)
            onStatsMutated()
          } else {
            removeStreamingMessage(streamingNexusId)
            lastFailedQuestion = question
            _streamState.value = NexusStreamState.Error("Rift AI returned an empty response.")
          }
        } catch (e: TimeoutCancellationException) {
          Log.e(TAG, "Rift AI streaming timed out", e)
          if (accumulatedText.isNotBlank()) {
            val partial = accumulatedText.toString().trim()
            updateStreamingMessage(streamingNexusId, partial, stillStreaming = false)
            _streamState.value = NexusStreamState.Success(partial)
          } else {
            removeStreamingMessage(streamingNexusId)
            lastFailedQuestion = question
            _streamState.value =
              NexusStreamState.Error("Rift AI request timed out after 60 seconds. Tap retry to transmit again.")
          }
        } catch (e: CancellationException) {
          throw e
        } catch (e: Exception) {
          Log.e(TAG, "Firebase AI generation failed", e)
          removeStreamingMessage(streamingNexusId)
          lastFailedQuestion = question
          _streamState.value =
            NexusStreamState.Error(
              e.localizedMessage ?: "Rift AI link failed. Verify connectivity and retry."
            )
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
        "Pilot: [${p.syndicateTag}] ${p.callsign} | Active Hull: ${p.spec.name} (${p.spec.role}) | Flux: ${p.flux.toInt()}/${p.maxFlux.toInt()} | Live Resonance: ${engine.liveResonance} | Peak Resonance: ${engine.peakResonance} | Shards: ${engine.riftShards}"
      )
      appendLine(
        "Active Mode: ${engine.currentCustomGame?.let { "Custom Rift ${it.title} (${it.baseMode.title})" } ?: engine.activeRankedMode.title} | Safe Harbor Active: ${engine.hasActiveSafeZone()}"
      )
      if (mergedBoard.isNotEmpty()) {
        appendLine("Top Sworn Pilots:")
        mergedBoard.take(5).forEachIndexed { idx, entry ->
          val tag = if (entry.syndicateTag.isNotBlank()) "[${entry.syndicateTag}] " else ""
          appendLine("  #${idx + 1} $tag${entry.callsign} — ${entry.allTimeBounty} Resonance")
        }
      }
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
