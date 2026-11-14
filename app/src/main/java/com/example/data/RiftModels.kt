package com.example.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

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

@IgnoreExtraProperties
data class LivePilotState(
  @DocumentId val id: String = "",
  val userId: String = "",
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
  val isFiringPrimary: Boolean = false,
  val isAfterburning: Boolean = false,
  val isCloaked: Boolean = false,
  val energy: Long = 2400L,
  val maxEnergy: Long = 2400L,
  val bounty: Long = 100L,
  val lastPingType: String = "NONE",
  val lastPingX: Double = 0.0,
  val lastPingY: Double = 0.0,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class SyndicateInfo(
  @DocumentId val id: String = "",
  val tag: String = "",
  val name: String = "",
  val foundryOrigin: String = "Independent",
  val motto: String = "",
  val memberCount: Int = 1,
  val combinedBounty: Long = 100L,
  val isCustom: Boolean = false,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

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
  val statusText: String, // e.g. "In Rift (Sector D4)", "In Hangar", "In Armada [NOVA_5]"
  val isOnline: Boolean,
  val currentArmadaName: String, // "" if SOLO
  val currentArmadaCount: Int,
  val bounty: Int,
)

data class ArmadaMemberSlot(
  val uid: String,
  val callsign: String,
  val syndicateTag: String,
  val shipId: Int,
  val isLeader: Boolean = false,
)

@IgnoreExtraProperties
data class SyndicateChatMessage(
  @DocumentId val id: String = "",
  val senderUid: String = "",
  val senderCallsign: String = "PILOT",
  val senderSyndicateTag: String = "",
  val timestampLabel: String = "JUST NOW",
  val messageText: String = "",
  val isArmadaRecruitBanner: Boolean = false,
  val armadaName: String = "",
  val armadaCurrentSize: Int = 1,
  val armadaMaxSize: Int = 5,
  val armadaObjective: String = "",
  val requestSent: Boolean = false,
  val createdAtMs: Long = System.currentTimeMillis(),
  @ServerTimestamp val createdAt: Timestamp? = null,
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
  fun toJson(): org.json.JSONObject =
    org.json.JSONObject().apply {
      put("survivedToConvergence", survivedToConvergence)
      put("killerCallsign", killerCallsign)
      put("killerSyndicateTag", killerSyndicateTag)
      put("killerShip", killerShip)
      put("killingWeapon", killingWeapon)
      put("killerBountyAtKill", killerBountyAtKill)
      put("bountyEarnedFromYou", bountyEarnedFromYou)
    }

  companion object {
    fun fromJson(obj: org.json.JSONObject?): ExecutionerTelemetry {
      if (obj == null) return ExecutionerTelemetry()
      val rawTag = obj.optString("killerSyndicateTag", "GUEST").uppercase()
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
    return java.util.Locale.US.let { String.format(it, "%dm %02ds", mins, secs) }
  }

  fun toJsonString(): String =
    org.json.JSONObject().apply {
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
        val obj = org.json.JSONObject(raw)
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
  val role: String = "Pilot", // "Founder / Commander", "Officer", or "Pilot"
  val joinedTimestamp: Long,
  val leftTimestamp: Long = 0L, // 0L = ACTIVE
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
    return String.format(java.util.Locale.US, "%dd %dh %dm", days, hours, mins)
  }

  fun toJson(): org.json.JSONObject =
    org.json.JSONObject().apply {
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
    fun fromJson(obj: org.json.JSONObject): SyndicateServiceEntry {
      val rawTag = obj.optString("syndicateTag", "GUEST").uppercase()
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

