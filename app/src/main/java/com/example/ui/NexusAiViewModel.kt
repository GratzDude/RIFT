package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.engine.RiftNexusKnowledge
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.content
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

class NexusAiViewModel : ViewModel() {

  // Lazy initialization required per Firebase AI Logic contract so Firebase.ai is never evaluated eagerly at construction
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

  val isBusy: Boolean
    get() = _streamState.value is NexusStreamState.Loading || _streamState.value is NexusStreamState.Streaming

  fun askNexus(rawQuestion: String, liveTelemetrySnapshot: String) {
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
        val accumulated = StringBuilder()
        try {
          // Build multi-turn chat history (last 10 messages to keep token latency crisp) + fresh live telemetry
          val recentHistory: List<Content> =
            priorCompletedMessages.takeLast(10).map { msg ->
              val roleTag = if (msg.role == NexusRole.USER) "user" else "model"
              content(roleTag) { text(msg.text) }
            }

          val groundedTurnContent =
            content("user") {
              text(
                buildString {
                  appendLine(liveTelemetrySnapshot)
                  appendLine()
                  appendLine("=== [PILOT QUERY TO NEXUS] ===")
                  append(question)
                }
              )
            }

          withTimeout(60_000L) {
            model.generateContentStream(recentHistory + groundedTurnContent).collect { chunk ->
              val chunkText = chunk.text.orEmpty()
              if (chunkText.isNotEmpty()) {
                accumulated.append(chunkText)
                val partial = accumulated.toString()
                _streamState.value = NexusStreamState.Streaming(partial)
                updateStreamingBubble(streamingNexusId, partial, stillStreaming = true)
              }
            }
          }

          val finalText = accumulated.toString().trim()
          if (finalText.isNotEmpty()) {
            updateStreamingBubble(streamingNexusId, finalText, stillStreaming = false)
            _streamState.value = NexusStreamState.Success(finalText)
          } else {
            removeBubble(streamingNexusId)
            lastFailedQuestion = question
            _streamState.value = NexusStreamState.Error("NEXUS returned an empty telemetry packet. Tap Retry.")
          }
        } catch (e: TimeoutCancellationException) {
          Log.e(TAG, "NEXUS streaming timed out after 60s", e)
          if (accumulated.isNotBlank()) {
            updateStreamingBubble(streamingNexusId, accumulated.toString(), stillStreaming = false)
          } else {
            removeBubble(streamingNexusId)
          }
          lastFailedQuestion = question
          _streamState.value =
            NexusStreamState.Error("Rift Core link timed out after 60 seconds. Tap Retry to re-transmit.")
        } catch (e: CancellationException) {
          // Always rethrow CancellationException to preserve cooperative coroutine cancellation
          throw e
        } catch (e: Exception) {
          Log.e(TAG, "NEXUS Firebase AI streaming failed", e)
          if (accumulated.isNotBlank()) {
            updateStreamingBubble(streamingNexusId, accumulated.toString(), stillStreaming = false)
          } else {
            removeBubble(streamingNexusId)
          }
          lastFailedQuestion = question
          _streamState.value =
            NexusStreamState.Error(e.localizedMessage ?: "Rift Core AI link failed.")
        }
      }
  }

  fun retryLastQuestion(liveTelemetrySnapshot: String) {
    val failed = lastFailedQuestion ?: return
    askNexus(failed, liveTelemetrySnapshot)
  }

  fun clearConversation() {
    activeJob?.cancel()
    activeJob = null
    lastFailedQuestion = null
    _messages.value = emptyList()
    _streamState.value = NexusStreamState.Idle
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
