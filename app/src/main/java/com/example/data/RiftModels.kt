package com.example.data

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import java.util.Locale
import org.json.JSONObject

object RiftCommunityRoles {
  val VERIFIED_DEV_CALLSIGNS: Set<String> = setOf("GRATZ", "GRATZDUDE", "ARCHITECT")
  val VERIFIED_DEV_UIDS: MutableSet<String> = mutableSetOf()

  fun isDeveloperArchitect(uid: String, callsign: String): Boolean {
    val cleanUid = uid.trim()
    if (cleanUid.isNotBlank() && cleanUid in VERIFIED_DEV_UIDS) return true
    val cleanCallsign = callsign.trim().uppercase(Locale.US)
    return cleanCallsign in VERIFIED_DEV_CALLSIGNS
  }
}

@IgnoreExtraProperties
data class PilotDossier(
  @DocumentId val id: String = "",
  val userId: String = "",
  val callsign: String = "COMMANDER",
  val syndicateTag: String = "GUEST",
  val syndicateName: String = "Guest Tracking Framework",
  val preferredShipId: Long = 1L,
  val preferredFoundry: String = "Independent",
  val preferredMode: String = "FFA",
  val pvpKills: Long = 0L,
  val pvpDeaths: Long = 0L,
  val pvpBountyPeak: Long = 100L,
  val allTimeKills: Long = 0L,
  val allTimeDeaths: Long = 0L,
  val allTimeBountyPeak: Long = 100L,
  val convergencesWon: Long = 0L,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class LeaderboardEntry(
  @DocumentId val id: String = "",
  val userId: String = "",
  val visibility: String = "public",
  val callsign: String = "COMMANDER",
  val syndicateTag: String = "GUEST",
  val armadaTag: String = "SOLO",
  val armadaTierSize: Int = 1,
  val shipId: Long = 1L,
  val foundry: String = "Independent",
  val pvpKills: Long = 0L,
  val pvpDeaths: Long = 0L,
  val pvpBounty: Long = 100L,
  val allTimeKills: Long = 0L,
  val allTimeDeaths: Long = 0L,
  val allTimeBounty: Long = 100L,
  val convergencesWon: Long = 0L,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class LivePilotState(
  @DocumentId val id: String = "",
  val userId: String = "",
  val visibility: String = "public",
  val sectorId: String = "warzone_10v10",
  val callsign: String = "COMMANDER",
  val syndicateTag: String = "",
  val armadaName: String = "",
  val shipId: Long = 1L,
  val freq: Long = 0L,
  val x: Double = 2000.0,
  val y: Double = 2000.0,
  val vx: Double = 0.0,
  val vy: Double = 0.0,
  val angle: Double = 0.0,
  val weaponAngle: Double = 0.0,
  @get:PropertyName("isFiringPrimary") @PropertyName("isFiringPrimary") val isFiringPrimary: Boolean = false,
  @get:PropertyName("isAfterburning") @PropertyName("isAfterburning") val isAfterburning: Boolean = false,
  @get:PropertyName("isCloaked") @PropertyName("isCloaked") val isCloaked: Boolean = false,
  @get:PropertyName("isRespawning") @PropertyName("isRespawning") val isRespawning: Boolean = false,
  @get:PropertyName("isInStasisBrig") @PropertyName("isInStasisBrig") val isInStasisBrig: Boolean = false,
  val secondarySeq: Long = 0L,
  val secondaryType: String = "NONE",
  val secondaryAim: Double = 0.0,
  val energy: Long = 2400L,
  val maxEnergy: Long = 2400L,
  val bounty: Long = 100L,
  val liveResonance: Long = 0L,
  val lastPingType: String = "NONE",
  val lastPingX: Double = 0.0,
  val lastPingY: Double = 0.0,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
  @get:Exclude val localSyncSeq: Long = 0L,
)

data class CloudSyncHealth(
  val isConnected: Boolean = false,
  val lastHeartbeatOkMs: Long = 0L,
  val lastSnapshotOkMs: Long = 0L,
  val statusLabel: String = "STANDBY",
  val lastError: String? = null,
)

@Keep
@IgnoreExtraProperties
data class SectorCombatEvent(
  @DocumentId val id: String = "",
  val senderUid: String = "",
  val targetUid: String = "",
  val sectorId: String = "SECTOR_CONVERGENCE",
  val eventType: String = "HIT",
  val damage: Long = 0L,
  val bountyAward: Long = 0L,
  val weaponLabel: String = "",
  val senderCallsign: String = "PILOT",
  val senderTag: String = "",
  val senderShipId: Long = 1L,
  val createdAtMs: Long = 0L,
  @ServerTimestamp val createdAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class SectorInstanceState(
  @DocumentId val id: String = "",
  val sectorId: String = "SECTOR_CONVERGENCE",
  val hostUid: String = "",
  val fluxCarrierUid: String = "",
  val fluxOrbX: Double = 4000.0,
  val fluxOrbY: Double = 3680.0,
  val fluxOrbVx: Double = 0.0,
  val fluxOrbVy: Double = 0.0,
  val fluxTier: Long = 0L,
  val fluxHoldSec: Double = 0.0,
  val rcWestScore: Double = 0.0,
  val rcEastScore: Double = 0.0,
  val rcTimerSec: Double = 180.0,
  val rcAlphaCtrl: Double = 0.0,
  val rcAlphaLock: Long = -1L,
  val rcOmegaCtrl: Double = 0.0,
  val rcOmegaLock: Long = -1L,
  val sbTimerSec: Double = 180.0,
  val sbWestCaps: Long = 0L,
  val sbEastCaps: Long = 0L,
  val sbWestCoreHp: Double = 40000.0,
  val sbEastCoreHp: Double = 40000.0,
  val jailedUidsCsv: String = "",
  val updatedAtMs: Long = 0L,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class SyndicateInfo(
  @DocumentId val id: String = "",
  val ownerId: String = "",
  val tag: String = "",
  val name: String = "",
  val foundryOrigin: String = "Independent",
  val motto: String = "",
  val memberCount: Int = 1,
  val combinedBounty: Long = 100L,
  @get:PropertyName("isCustom") @PropertyName("isCustom") val isCustom: Boolean = false,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
) {
  fun isRealPlayerFoundedSyndicate(): Boolean {
    val cleanId = id.trim().lowercase(Locale.US)
    val cleanTag = tag.trim().uppercase(Locale.US)
    val cleanOwner = ownerId.trim()
    if (!isCustom) return false
    if (cleanId in setOf("syn_aex", "syn_obs", "syn_sol", "syn_vtx", "syn_rft")) return false
    if (
      cleanOwner.isEmpty() ||
        cleanOwner.startsWith("dev_", ignoreCase = true) ||
        cleanOwner.startsWith("drifter_", ignoreCase = true) ||
        cleanOwner.startsWith("guest_", ignoreCase = true) ||
        cleanOwner.startsWith("bot_", ignoreCase = true)
    ) {
      return false
    }
    if (cleanTag.length !in 1..4) return false
    if (
      cleanTag in
        setOf(
          "BOT",
          "RFT",
          "GUEST",
          "FREE",
          "SOLO",
          "UNAFFILIATED",
          "NONE",
          "KSTL",
          "AURL",
          "VRTX",
          "RUST",
        )
    ) {
      return false
    }
    return true
  }

  fun isValidRealPlayerSyndicate(): Boolean = isRealPlayerFoundedSyndicate()
}

data class SyndicateMemberEntry(
  val uid: String,
  val callsign: String,
  val tag: String,
  val shipId: Int,
  val shipName: String,
  val role: String,
  val bountyContribution: Long,
  val kills: Long,
  val isOnline: Boolean,
  val isLocalPlayer: Boolean = false,
)

data class PilotAchievement(
  val id: String,
  val title: String,
  val description: String,
  val unlocked: Boolean,
  val progressText: String,
)

data class FriendPilotEntry(
  val uid: String,
  val callsign: String,
  val syndicateTag: String,
  val shipId: Int,
  val statusText: String,
  val isOnline: Boolean,
  val currentArmadaName: String,
  val currentArmadaCount: Int,
  val bounty: Int,
)

data class ArmadaMemberSlot(
  val uid: String,
  val callsign: String,
  val syndicateTag: String,
  val shipId: Int,
  val isLeader: Boolean = false,
  val resonance: Int = 0,
  val isOnline: Boolean = true,
  val activityLabel: String = "ONLINE",
)

@Keep
@IgnoreExtraProperties
data class CloudCustomGameRoom(
  @DocumentId val id: String = "",
  val roomCode: String = "",
  val instanceId: String = "",
  val hostUid: String = "",
  val hostCallsign: String = "PILOT",
  val title: String = "",
  val visibility: String = "public",
  @get:PropertyName("privateRoom") @PropertyName("privateRoom") val privateRoom: Boolean = false,
  val baseMode: String = "SECTOR_CONVERGENCE",
  val teamSizeBracket: Int = 5,
  val matchDurationMinutes: Int = 10,
  val allowArmadasInFfa: Boolean = true,
  val botsEnabled: Boolean = true,
  val bulletSizeMult: Double = 1.0,
  val damageMult: Double = 1.0,
  val speedMult: Double = 1.0,
  val healthMult: Double = 1.0,
  val rechargeMult: Double = 1.0,
  val crateSpawnMult: Double = 1.0,
  val createdAtMs: Long = 0L,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class SyndicateChatMessage(
  @DocumentId val id: String = "",
  val senderUid: String = "",
  val senderCallsign: String = "PILOT",
  val senderSyndicateTag: String = "",
  val timestampLabel: String = "JUST NOW",
  val messageText: String = "",
  @get:PropertyName("isArmadaRecruitBanner") @PropertyName("isArmadaRecruitBanner") val isArmadaRecruitBanner: Boolean = false,
  val armadaName: String = "",
  val armadaCurrentSize: Int = 1,
  val armadaMaxSize: Int = 5,
  val armadaObjective: String = "",
  val requestSent: Boolean = false,
  val createdAtMs: Long = System.currentTimeMillis(),
  @ServerTimestamp val createdAt: Timestamp? = null,
) {
  fun normalizedChannelKey(): String {
    val raw = timestampLabel.trim().uppercase(Locale.US)
    return when {
      raw.isEmpty() || raw == "JUST NOW" || raw == "LIVE" || raw == "#MAIN-LOBBY" || raw == "MAIN-LOBBY" ->
        "#SECTOR-LOUNGE"
      raw == "#CHANGE-LOG" || raw == "CHANGE-LOG" || raw == "#PATCH-LOG" -> "#PATCH-LOG"
      raw == "#BUG-REPORT" || raw == "BUG-REPORT" || raw == "#BUG-REPORTS" -> "#BUG-REPORTS"
      raw == "#ARMADA-CHAT" || raw == "ARMADA-CHAT" ->
        if (armadaName.isNotBlank()) "STRIKE:${armadaName.trim().uppercase(Locale.US)}".take(24)
        else "STRIKE:SQUAD"
      raw.startsWith("SYNDICATE:") -> {
        val sub = raw.removePrefix("SYNDICATE:").trim().let { if (it.startsWith("#")) it else "#$it" }
        val tag = senderSyndicateTag.trim().uppercase(Locale.US).take(4).ifEmpty { "SYN" }
        "SYN:$tag:$sub".take(24)
      }
      raw.startsWith("SYN:") || raw.startsWith("STRIKE:") -> raw.take(24)
      raw.startsWith("#") -> raw.take(24)
      else -> "#$raw".take(24)
    }
  }

  fun resolvedChannelKey(): String = normalizedChannelKey().lowercase(Locale.US)

  fun isBugReportCard(): Boolean =
    resolvedChannelKey() == "#bug-reports" || armadaObjective.startsWith("BUG|", ignoreCase = true)

  fun isBuildShareCard(): Boolean = armadaObjective.startsWith("BUILD|", ignoreCase = true)

  fun isPrivateRoomInviteCard(): Boolean = armadaObjective.startsWith("ROOM|", ignoreCase = true)

  fun parsedBuildShareRecipeJson(): String? {
    if (!isBuildShareCard()) return null
    val fromObjective = armadaObjective.substringAfter("|", "").trim()
    if (fromObjective.startsWith("{") && fromObjective.endsWith("}")) return fromObjective
    val braceStart = messageText.indexOf('{')
    val braceEnd = messageText.lastIndexOf('}')
    if (braceStart >= 0 && braceEnd > braceStart) {
      return messageText.substring(braceStart, braceEnd + 1).trim()
    }
    return fromObjective.takeIf { it.isNotEmpty() }
  }

  fun parsedRoomCode(): String? {
    if (!isPrivateRoomInviteCard()) return null
    val parts = armadaObjective.split("|")
    val fromObj =
      parts
        .getOrNull(1)
        ?.trim()
        ?.uppercase(Locale.US)
        ?.replace(Regex("[^A-Z0-9]"), "")
        ?.take(6)
        .orEmpty()
    if (fromObj.length in 4..6) return fromObj
    val regexMatch = Regex("\\b([A-Z0-9]{6})\\b").find(messageText.uppercase(Locale.US))
    return regexMatch?.groupValues?.getOrNull(1)
  }
}

data class SectorBugReport(
  val id: String = "",
  val reporterUid: String = "",
  val reporterCallsign: String = "",
  val syndicateTag: String = "",
  val category: String = "PHYSICS / COLLISION",
  val description: String = "",
  val buildVersion: Int = 57,
  val activeMode: String = "NEXUS_LOBBY",
  val activeShipId: Int = 1,
  val screenDpSummary: String = "",
  val createdAtMs: Long = System.currentTimeMillis(),
)

data class CloudSyndicateMeta(
  val tag: String = "",
  val ownerId: String = "",
  val crestJson: String = "",
  val customChannels: List<String> = listOf("#general"),
  val invitedGuests: Set<String> = emptySet(),
  val updatedAtMs: Long = 0L,
)

data class ExecutionerTelemetry(
  val survivedToConvergence: Boolean = false,
  val killerCallsign: String = "",
  val killerSyndicateTag: String = "GUEST",
  val killerShip: String = "",
  val killingWeapon: String = "",
  val killerBountyAtKill: Int = 0,
  val bountyEarnedFromYou: Int = 0,
) {
  fun toJson(): JSONObject =
    JSONObject().apply {
      put("survivedToConvergence", survivedToConvergence)
      put("killerCallsign", killerCallsign)
      put("killerSyndicateTag", killerSyndicateTag)
      put("killerShip", killerShip)
      put("killingWeapon", killingWeapon)
      put("killerBountyAtKill", killerBountyAtKill)
      put("bountyEarnedFromYou", bountyEarnedFromYou)
    }

  companion object {
    fun fromJson(obj: JSONObject?): ExecutionerTelemetry {
      if (obj == null) return ExecutionerTelemetry()
      val rawTag = obj.optString("killerSyndicateTag", "GUEST").uppercase(Locale.US)
      val cleanTag = if (rawTag in setOf("KSTL", "AURL", "VRTX", "RUST", "")) "GUEST" else rawTag
      return ExecutionerTelemetry(
        survivedToConvergence = obj.optBoolean("survivedToConvergence", false),
        killerCallsign = obj.optString("killerCallsign", ""),
        killerSyndicateTag = cleanTag,
        killerShip = stripShipNumberPrefix(obj.optString("killerShip", "")),
        killingWeapon = obj.optString("killingWeapon", ""),
        killerBountyAtKill = obj.optInt("killerBountyAtKill", 0),
        bountyEarnedFromYou = obj.optInt("bountyEarnedFromYou", 0),
      )
    }
  }
}

private fun stripShipNumberPrefix(raw: String): String =
  raw
    .trim()
    .replace(Regex("^\\d+\\s*-\\s*"), "")
    .replace("9A", "Frame A")
    .replace("9B", "Frame B")
    .replace("9C", "Frame C")
    .replace("9D", "Frame D")
    .replace("9E", "Frame E")

data class CurrentLifeStats(
  var bountyReached: Int = 100,
  var shipId: Int = 1,
  var shipFlown: String = "Viper",
  var timeSurvivedSec: Float = 0f,
  var pvpKills: Int = 0,
  var botKills: Int = 0,
  var hvtShutdowns: Int = 0,
  var damageDealt: Float = 0f,
  var damageTaken: Float = 0f,
  var shotsFired: Int = 0,
  var shotsHit: Int = 0,
  var buffsCollected: Int = 0,
) {
  fun accuracyPercent(): Int {
    if (shotsFired <= 0) return 0
    return ((shotsHit.toFloat() / shotsFired.toFloat()) * 100f).toInt().coerceIn(0, 100)
  }

  fun toSnapshot(
    executioner: ExecutionerTelemetry = ExecutionerTelemetry(),
    recordedAtMs: Long = System.currentTimeMillis(),
  ): PeakBountyRunSnapshot =
    PeakBountyRunSnapshot(
      bountyReached = bountyReached,
      shipId = shipId,
      shipFlown = shipFlown,
      timeSurvivedSec = timeSurvivedSec,
      pvpKills = pvpKills,
      botKills = botKills,
      hvtShutdowns = hvtShutdowns,
      damageDealt = damageDealt,
      damageTaken = damageTaken,
      shotsFired = shotsFired,
      shotsHit = shotsHit,
      buffsCollected = buffsCollected,
      recordedAtMs = recordedAtMs,
      executioner = executioner,
    )
}

data class PeakBountyRunSnapshot(
  val bountyReached: Int = 100,
  val shipId: Int = 1,
  val shipFlown: String = "Viper",
  val timeSurvivedSec: Float = 0f,
  val pvpKills: Int = 0,
  val botKills: Int = 0,
  val hvtShutdowns: Int = 0,
  val damageDealt: Float = 0f,
  val damageTaken: Float = 0f,
  val shotsFired: Int = 0,
  val shotsHit: Int = 0,
  val buffsCollected: Int = 0,
  val recordedAtMs: Long = System.currentTimeMillis(),
  val executioner: ExecutionerTelemetry = ExecutionerTelemetry(),
) {
  fun accuracyPercent(): Int {
    if (shotsFired <= 0) return 0
    return ((shotsHit.toFloat() / shotsFired.toFloat()) * 100f).toInt().coerceIn(0, 100)
  }

  fun formattedDuration(): String {
    val totalSec = timeSurvivedSec.toInt().coerceAtLeast(0)
    val mins = totalSec / 60
    val secs = totalSec % 60
    return String.format(Locale.US, "%dm %02ds", mins, secs)
  }

  fun toJsonString(): String =
    JSONObject().apply {
      put("bountyReached", bountyReached)
      put("shipId", shipId)
      put("shipFlown", shipFlown)
      put("timeSurvivedSec", timeSurvivedSec.toDouble())
      put("pvpKills", pvpKills)
      put("botKills", botKills)
      put("hvtShutdowns", hvtShutdowns)
      put("damageDealt", damageDealt.toDouble())
      put("damageTaken", damageTaken.toDouble())
      put("shotsFired", shotsFired)
      put("shotsHit", shotsHit)
      put("buffsCollected", buffsCollected)
      put("recordedAtMs", recordedAtMs)
      put("executioner", executioner.toJson())
    }.toString()

  companion object {
    fun fromJsonString(raw: String?): PeakBountyRunSnapshot? {
      if (raw.isNullOrBlank()) return null
      return try {
        val obj = JSONObject(raw)
        PeakBountyRunSnapshot(
          bountyReached = obj.optInt("bountyReached", 100),
          shipId = obj.optInt("shipId", 1),
          shipFlown = stripShipNumberPrefix(obj.optString("shipFlown", "Viper")).ifEmpty { "Viper" },
          timeSurvivedSec = obj.optDouble("timeSurvivedSec", 0.0).toFloat(),
          pvpKills = obj.optInt("pvpKills", 0),
          botKills = obj.optInt("botKills", 0),
          hvtShutdowns = obj.optInt("hvtShutdowns", 0),
          damageDealt = obj.optDouble("damageDealt", 0.0).toFloat(),
          damageTaken = obj.optDouble("damageTaken", 0.0).toFloat(),
          shotsFired = obj.optInt("shotsFired", 0),
          shotsHit = obj.optInt("shotsHit", 0),
          buffsCollected = obj.optInt("buffsCollected", 0),
          recordedAtMs = obj.optLong("recordedAtMs", System.currentTimeMillis()),
          executioner = ExecutionerTelemetry.fromJson(obj.optJSONObject("executioner")),
        )
      } catch (_: Exception) {
        null
      }
    }
  }
}

data class SyndicateServiceEntry(
  val syndicateTag: String,
  val syndicateName: String,
  val role: String = "Pilot",
  val joinedTimestamp: Long,
  val leftTimestamp: Long = 0L,
  val totalKills: Long = 0L,
  val peakBountyAchieved: Long = 100L,
  val publicGameWins: Long = 0L,
) {
  val isActive: Boolean
    get() = leftTimestamp <= 0L

  fun formatTenure(nowMs: Long = System.currentTimeMillis()): String {
    val endMs = if (leftTimestamp > 0L) leftTimestamp else nowMs
    val elapsedMs = (endMs - joinedTimestamp).coerceAtLeast(0L)
    val totalMinutes = elapsedMs / 60_000L
    val days = totalMinutes / (24L * 60L)
    val hours = (totalMinutes / 60L) % 24L
    val mins = totalMinutes % 60L
    return String.format(Locale.US, "%dd %dh %dm", days, hours, mins)
  }

  fun toJson(): JSONObject =
    JSONObject().apply {
      put("syndicateTag", syndicateTag)
      put("syndicateName", syndicateName)
      put("role", role)
      put("joinedTimestamp", joinedTimestamp)
      put("leftTimestamp", leftTimestamp)
      put("totalKills", totalKills)
      put("peakBountyAchieved", peakBountyAchieved)
      put("publicGameWins", publicGameWins)
    }

  companion object {
    fun fromJson(obj: JSONObject): SyndicateServiceEntry {
      val rawTag = obj.optString("syndicateTag", "GUEST").uppercase(Locale.US)
      val cleanTag = if (rawTag in setOf("KSTL", "AURL", "VRTX", "RUST", "")) "GUEST" else rawTag
      val rawName = obj.optString("syndicateName", "Guest Tracking Framework")
      val cleanName =
        if (
          rawName.contains("Kestrel", ignoreCase = true) ||
            rawName.contains("Aurelia", ignoreCase = true) ||
            rawName.contains("Vortex", ignoreCase = true) ||
            rawName.contains("Rustweld", ignoreCase = true)
        ) {
          "Guest Tracking Framework"
        } else {
          rawName
        }
      return SyndicateServiceEntry(
        syndicateTag = cleanTag,
        syndicateName = cleanName,
        role = obj.optString("role", "Pilot"),
        joinedTimestamp = obj.optLong("joinedTimestamp", System.currentTimeMillis()),
        leftTimestamp = obj.optLong("leftTimestamp", 0L),
        totalKills = obj.optLong("totalKills", 0L),
        peakBountyAchieved = obj.optLong("peakBountyAchieved", 100L),
        publicGameWins = obj.optLong("publicGameWins", 0L),
      )
    }
  }
}

data class SyndicateWarRecord(
  val tag: String,
  val warTrophies: Int = 1000,
  val warWins: Int = 0,
  val warLosses: Int = 0,
  val warStreak: Int = 0,
) {
  fun toJson(): JSONObject =
    JSONObject().apply {
      put("tag", tag)
      put("warTrophies", warTrophies)
      put("warWins", warWins)
      put("warLosses", warLosses)
      put("warStreak", warStreak)
    }

  companion object {
    fun fromJson(tag: String, obj: JSONObject): SyndicateWarRecord =
      SyndicateWarRecord(
        tag = obj.optString("tag", tag),
        warTrophies = obj.optInt("warTrophies", 1000),
        warWins = obj.optInt("warWins", 0),
        warLosses = obj.optInt("warLosses", 0),
        warStreak = obj.optInt("warStreak", 0),
      )
  }
}
