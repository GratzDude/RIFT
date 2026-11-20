package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.LivePilotState
import com.example.engine.RankedGameMode
import com.example.engine.RiftCalibrationState
import com.example.engine.RiftGameEngine
import java.util.Locale
import kotlin.math.abs
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class MatchmakingQueueStatus {
  IDLE,
  SEARCHING_BALANCED,
  EXPANDING_BRACKET,
  MATCH_FOUND,
  CANCELLED,
  TIMED_OUT,
}

data class LobbyPilotEntry(
  val uid: String,
  val callsign: String,
  val syndicateTag: String,
  val armadaName: String,
  val shipId: Int,
  val resonance: Int,
  val isLocalPilot: Boolean,
  val isWingmate: Boolean,
  val isEligibleOpponent: Boolean,
)

data class MatchmakingQueueUiState(
  val status: MatchmakingQueueStatus = MatchmakingQueueStatus.IDLE,
  val modeTitle: String = "",
  val riftLabel: String = "",
  val targetSectorKey: String = "",
  val rankedMode: RankedGameMode = RankedGameMode.SECTOR_CONVERGENCE,
  val requiresOpponentTeam: Boolean = false,
  val teamAvgResonance: Int = 0,
  val elapsedSec: Float = 0f,
  val maxDurationSec: Float = MAX_QUEUE_DURATION_SEC,
  val lobbyPilots: List<LobbyPilotEntry> = emptyList(),
  val eligibleOpponentCount: Int = 0,
  val balancedOpponentCount: Int = 0,
  val noticeMessage: String? = null,
) {
  val isQueueActive: Boolean
    get() =
      status == MatchmakingQueueStatus.SEARCHING_BALANCED ||
        status == MatchmakingQueueStatus.EXPANDING_BRACKET

  val remainingSec: Float
    get() = (maxDurationSec - elapsedSec).coerceIn(0f, maxDurationSec)

  val progressFraction: Float
    get() = (elapsedSec / maxDurationSec.coerceAtLeast(0.1f)).coerceIn(0f, 1f)

  val statusTransitionLabel: String
    get() =
      when (status) {
        MatchmakingQueueStatus.SEARCHING_BALANCED -> "BALANCED WINDOW (+/- 250 RES)"
        MatchmakingQueueStatus.EXPANDING_BRACKET -> "OPEN BRACKET (ACCEPTING ANY AVAILABLE TEAM)"
        MatchmakingQueueStatus.MATCH_FOUND -> "MATCH LOCKED // RIFTING IN"
        MatchmakingQueueStatus.CANCELLED -> "QUEUE CANCELLED"
        MatchmakingQueueStatus.TIMED_OUT -> "NO OTHER AVAILABLE TEAMS"
        MatchmakingQueueStatus.IDLE -> "STANDBY"
      }

  companion object {
    const val MAX_QUEUE_DURATION_SEC = 15.0f
    const val BRACKET_EXPANSION_SEC = 7.5f
    const val BALANCED_RESONANCE_WINDOW = 250
  }
}

/**
 * Lifecycle-aware ViewModel managing Ranked Resonance matchmaking queue state,
 * live lobby pilot tracking, Resonance bracket expansion (0-7.5s +/-250 RES -> 7.5-15.0s Open),
 * and clean status transitions (IDLE -> SEARCHING_BALANCED -> EXPANDING_BRACKET -> MATCH_FOUND / CANCELLED / TIMED_OUT).
 */
class MatchmakingQueueViewModel : ViewModel() {

  private val _uiState = MutableStateFlow(MatchmakingQueueUiState())
  val uiState: StateFlow<MatchmakingQueueUiState> = _uiState.asStateFlow()

  private var queueTimerJob: Job? = null
  private var pendingLaunchAction: (() -> Unit)? = null
  private var pendingTimeoutNoticeCallback: ((String) -> Unit)? = null

  private var latestUserId: String = ""
  private var latestCallsign: String = "COMMANDER"
  private var latestSyndicateTag: String = ""
  private var latestArmadaName: String = ""
  private var latestShipId: Int = 1
  private var latestLocalResonance: Int = 0
  private var latestIsDeviceOnline: Boolean = true
  private var latestMergedPilots: List<LivePilotState> = emptyList()

  /**
   * Streams live local & remote pilot presence into the matchmaking state machine so lobby
   * rosters, Strike Wing Resonance averages, and opponent match checks react in real time.
   */
  fun updateLobbySnapshot(
    currentUserId: String,
    localCallsign: String,
    localSyndicateTag: String,
    localArmadaName: String,
    localShipId: Int,
    localLiveResonance: Int,
    isDeviceOnline: Boolean,
    mergedLivePilots: List<LivePilotState>,
  ) {
    latestUserId = currentUserId
    latestCallsign = localCallsign.ifBlank { "PILOT" }
    latestSyndicateTag = RiftGameEngine.cleanTag(localSyndicateTag)
    latestArmadaName = localArmadaName.trim()
    latestShipId = localShipId.coerceIn(1, 10)
    latestLocalResonance = localLiveResonance.coerceAtLeast(0)
    latestIsDeviceOnline = isDeviceOnline
    latestMergedPilots = mergedLivePilots

    val cur = _uiState.value
    if (!cur.isQueueActive) return

    val computed =
      evaluateLobbyForTarget(
        targetSectorKey = cur.targetSectorKey,
        rankedMode = cur.rankedMode,
      )

    _uiState.value =
      cur.copy(
        teamAvgResonance = computed.teamAvgResonance,
        lobbyPilots = computed.lobbyPilots,
        eligibleOpponentCount = computed.eligibleOpponentCount,
        balancedOpponentCount = computed.balancedOpponentCount,
      )

    val readyToMatchNow =
      when (cur.status) {
        MatchmakingQueueStatus.SEARCHING_BALANCED -> computed.balancedOpponentCount > 0
        MatchmakingQueueStatus.EXPANDING_BRACKET -> computed.eligibleOpponentCount > 0
        else -> false
      }
    if (readyToMatchNow) {
      completeMatchFoundAndLaunch()
    }
  }

  /**
   * Enqueues or immediately dispatches Resonance-based matchmaking depending on mode rules,
   * device connectivity, and live lobby opponent availability.
   */
  fun enqueueOrExecute(
    modeTitle: String,
    riftLabel: String,
    targetSectorKey: String,
    rankedMode: RankedGameMode,
    isCustomInstance: Boolean,
    onNoTeamsNotice: (String) -> Unit = {},
    onExecuteLaunch: () -> Unit,
  ) {
    queueTimerJob?.cancel()
    queueTimerJob = null

    val isTeamRankedMode =
      !isCustomInstance &&
        rankedMode in
          setOf(
            RankedGameMode.SYNDICATE_WARS,
            RankedGameMode.RIFT_SKIRMISH,
            RankedGameMode.RIFT_CONTROL,
            RankedGameMode.FLUX_CORE,
            RankedGameMode.STASIS_BREACH,
          )
    val requiresOpponentTeam =
      !isCustomInstance &&
        (rankedMode == RankedGameMode.SYNDICATE_WARS ||
          (!RiftCalibrationState.botsEnabled && isTeamRankedMode))

    if (!isTeamRankedMode || (!latestIsDeviceOnline && !requiresOpponentTeam)) {
      pendingLaunchAction = null
      pendingTimeoutNoticeCallback = null
      _uiState.value =
        MatchmakingQueueUiState(
          status = MatchmakingQueueStatus.MATCH_FOUND,
          modeTitle = modeTitle,
          riftLabel = riftLabel,
          targetSectorKey = targetSectorKey,
          rankedMode = rankedMode,
        )
      _uiState.value = MatchmakingQueueUiState(status = MatchmakingQueueStatus.IDLE)
      onExecuteLaunch()
      return
    }

    val computed =
      evaluateLobbyForTarget(
        targetSectorKey = targetSectorKey,
        rankedMode = rankedMode,
      )

    if (computed.balancedOpponentCount > 0) {
      pendingLaunchAction = null
      pendingTimeoutNoticeCallback = null
      _uiState.value =
        MatchmakingQueueUiState(
          status = MatchmakingQueueStatus.MATCH_FOUND,
          modeTitle = modeTitle,
          riftLabel = riftLabel,
          targetSectorKey = targetSectorKey,
          rankedMode = rankedMode,
          requiresOpponentTeam = requiresOpponentTeam,
          teamAvgResonance = computed.teamAvgResonance,
          lobbyPilots = computed.lobbyPilots,
          eligibleOpponentCount = computed.eligibleOpponentCount,
          balancedOpponentCount = computed.balancedOpponentCount,
        )
      _uiState.value = MatchmakingQueueUiState(status = MatchmakingQueueStatus.IDLE)
      onExecuteLaunch()
      return
    }

    if (!latestIsDeviceOnline && requiresOpponentTeam) {
      pendingLaunchAction = null
      pendingTimeoutNoticeCallback = null
      val msg = "No other available teams"
      _uiState.value =
        MatchmakingQueueUiState(
          status = MatchmakingQueueStatus.TIMED_OUT,
          modeTitle = modeTitle,
          riftLabel = riftLabel,
          targetSectorKey = targetSectorKey,
          rankedMode = rankedMode,
          requiresOpponentTeam = requiresOpponentTeam,
          teamAvgResonance = computed.teamAvgResonance,
          lobbyPilots = computed.lobbyPilots,
          noticeMessage = msg,
        )
      onNoTeamsNotice(msg)
      return
    }

    pendingLaunchAction = onExecuteLaunch
    pendingTimeoutNoticeCallback = onNoTeamsNotice
    _uiState.value =
      MatchmakingQueueUiState(
        status = MatchmakingQueueStatus.SEARCHING_BALANCED,
        modeTitle = modeTitle,
        riftLabel = riftLabel,
        targetSectorKey = targetSectorKey,
        rankedMode = rankedMode,
        requiresOpponentTeam = requiresOpponentTeam,
        teamAvgResonance = computed.teamAvgResonance,
        elapsedSec = 0f,
        lobbyPilots = computed.lobbyPilots,
        eligibleOpponentCount = computed.eligibleOpponentCount,
        balancedOpponentCount = computed.balancedOpponentCount,
        noticeMessage = null,
      )

    queueTimerJob =
      viewModelScope.launch {
        var elapsed = 0f
        while (elapsed < MatchmakingQueueUiState.MAX_QUEUE_DURATION_SEC) {
          delay(100L)
          val activeState = _uiState.value
          if (!activeState.isQueueActive) return@launch

          elapsed =
            (elapsed + 0.1f).coerceAtMost(MatchmakingQueueUiState.MAX_QUEUE_DURATION_SEC)
          val nextStatus =
            if (elapsed < MatchmakingQueueUiState.BRACKET_EXPANSION_SEC) {
              MatchmakingQueueStatus.SEARCHING_BALANCED
            } else {
              MatchmakingQueueStatus.EXPANDING_BRACKET
            }
          val liveEval =
            evaluateLobbyForTarget(
              targetSectorKey = activeState.targetSectorKey,
              rankedMode = activeState.rankedMode,
            )

          _uiState.value =
            activeState.copy(
              status = nextStatus,
              elapsedSec = elapsed,
              teamAvgResonance = liveEval.teamAvgResonance,
              lobbyPilots = liveEval.lobbyPilots,
              eligibleOpponentCount = liveEval.eligibleOpponentCount,
              balancedOpponentCount = liveEval.balancedOpponentCount,
            )

          val foundMatch =
            if (nextStatus == MatchmakingQueueStatus.SEARCHING_BALANCED) {
              liveEval.balancedOpponentCount > 0
            } else {
              liveEval.eligibleOpponentCount > 0
            }
          if (foundMatch) {
            completeMatchFoundAndLaunch()
            return@launch
          }
        }

        val finalState = _uiState.value
        if (finalState.isQueueActive) {
          val finalEval =
            evaluateLobbyForTarget(
              targetSectorKey = finalState.targetSectorKey,
              rankedMode = finalState.rankedMode,
            )
          if (finalEval.eligibleOpponentCount > 0 || !finalState.requiresOpponentTeam) {
            completeMatchFoundAndLaunch()
          } else {
            val msg = "No other available teams"
            val notify = pendingTimeoutNoticeCallback
            pendingLaunchAction = null
            pendingTimeoutNoticeCallback = null
            _uiState.value =
              finalState.copy(
                status = MatchmakingQueueStatus.TIMED_OUT,
                elapsedSec = MatchmakingQueueUiState.MAX_QUEUE_DURATION_SEC,
                lobbyPilots = finalEval.lobbyPilots,
                eligibleOpponentCount = finalEval.eligibleOpponentCount,
                balancedOpponentCount = finalEval.balancedOpponentCount,
                noticeMessage = msg,
              )
            notify?.invoke(msg)
          }
        }
      }
  }

  /**
   * Immediately deploys into the match when bots / open-slot fill is allowed.
   */
  fun deployNow() {
    val cur = _uiState.value
    if (!cur.isQueueActive || cur.requiresOpponentTeam) return
    completeMatchFoundAndLaunch()
  }

  /**
   * Cancels the active matchmaking queue and returns status to CANCELLED -> IDLE.
   */
  fun cancelQueue() {
    queueTimerJob?.cancel()
    queueTimerJob = null
    pendingLaunchAction = null
    pendingTimeoutNoticeCallback = null
    _uiState.value =
      _uiState.value.copy(
        status = MatchmakingQueueStatus.CANCELLED,
        elapsedSec = 0f,
        noticeMessage = null,
      )
  }

  fun dismissNotice() {
    val cur = _uiState.value
    if (cur.noticeMessage != null || cur.status == MatchmakingQueueStatus.TIMED_OUT) {
      _uiState.value = MatchmakingQueueUiState(status = MatchmakingQueueStatus.IDLE)
    }
  }

  fun resetForAccountSwitch() {
    queueTimerJob?.cancel()
    queueTimerJob = null
    pendingLaunchAction = null
    pendingTimeoutNoticeCallback = null
    _uiState.value = MatchmakingQueueUiState(status = MatchmakingQueueStatus.IDLE)
  }

  private fun completeMatchFoundAndLaunch() {
    queueTimerJob?.cancel()
    queueTimerJob = null
    val launch = pendingLaunchAction
    pendingLaunchAction = null
    pendingTimeoutNoticeCallback = null
    _uiState.value = _uiState.value.copy(status = MatchmakingQueueStatus.MATCH_FOUND, noticeMessage = null)
    _uiState.value = MatchmakingQueueUiState(status = MatchmakingQueueStatus.IDLE)
    launch?.invoke()
  }

  private data class EvaluatedLobbySnapshot(
    val teamAvgResonance: Int,
    val lobbyPilots: List<LobbyPilotEntry>,
    val eligibleOpponentCount: Int,
    val balancedOpponentCount: Int,
  )

  private fun evaluateLobbyForTarget(
    targetSectorKey: String,
    rankedMode: RankedGameMode,
  ): EvaluatedLobbySnapshot {
    val wingResValues = mutableListOf(latestLocalResonance)
    if (latestIsDeviceOnline && latestArmadaName.isNotBlank()) {
      for (st in latestMergedPilots) {
        val rUid = st.userId.ifEmpty { st.id }
        if (
          rUid.isNotBlank() &&
            rUid != latestUserId &&
            st.armadaName.trim().equals(latestArmadaName, ignoreCase = true)
        ) {
          val pRes = (if (st.liveResonance > 0L) st.liveResonance else st.bounty).toInt().coerceAtLeast(0)
          wingResValues.add(pRes)
        }
      }
    }
    val teamAvgRes = (wingResValues.sum() / wingResValues.size.coerceAtLeast(1)).coerceAtLeast(0)

    val entries = mutableListOf<LobbyPilotEntry>()
    entries.add(
      LobbyPilotEntry(
        uid = latestUserId.ifBlank { "local" },
        callsign = latestCallsign.uppercase(Locale.US),
        syndicateTag = latestSyndicateTag,
        armadaName = latestArmadaName,
        shipId = latestShipId,
        resonance = latestLocalResonance,
        isLocalPilot = true,
        isWingmate = true,
        isEligibleOpponent = false,
      )
    )

    var eligibleOpponents = 0
    var balancedOpponents = 0

    if (latestIsDeviceOnline) {
      for (st in latestMergedPilots) {
        val rUid = st.userId.ifEmpty { st.id }
        if (rUid.isBlank() || rUid == latestUserId) continue
        val sameSector = st.sectorId.equals(targetSectorKey, ignoreCase = true)
        val sameWing =
          latestArmadaName.isNotBlank() &&
            st.armadaName.trim().equals(latestArmadaName, ignoreCase = true)
        if (!sameSector && !sameWing) continue

        val cleanRemoteSyn = RiftGameEngine.cleanTag(st.syndicateTag)
        val sameSyndicateInWar =
          rankedMode == RankedGameMode.SYNDICATE_WARS &&
            latestSyndicateTag.isNotBlank() &&
            cleanRemoteSyn == latestSyndicateTag
        val isOpponent = sameSector && !sameWing && !sameSyndicateInWar
        val pilotRes =
          (if (st.liveResonance > 0L) st.liveResonance else st.bounty).toInt().coerceAtLeast(0)

        if (isOpponent) {
          eligibleOpponents++
          if (abs(pilotRes - teamAvgRes) <= MatchmakingQueueUiState.BALANCED_RESONANCE_WINDOW) {
            balancedOpponents++
          }
        }

        entries.add(
          LobbyPilotEntry(
            uid = rUid,
            callsign = st.callsign.ifBlank { "PILOT" }.uppercase(Locale.US),
            syndicateTag = cleanRemoteSyn,
            armadaName = st.armadaName.trim(),
            shipId = st.shipId.toInt().coerceIn(1, 10),
            resonance = pilotRes,
            isLocalPilot = false,
            isWingmate = sameWing,
            isEligibleOpponent = isOpponent,
          )
        )
      }
    }

    return EvaluatedLobbySnapshot(
      teamAvgResonance = teamAvgRes,
      lobbyPilots = entries,
      eligibleOpponentCount = eligibleOpponents,
      balancedOpponentCount = balancedOpponents,
    )
  }
}
