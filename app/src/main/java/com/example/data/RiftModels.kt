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
  val shipId: Long = 1L,
  val freq: Long = 0L,
  val x: Double = 2000.0,
  val y: Double = 2000.0,
  val vx: Double = 0.0,
  val vy: Double = 0.0,
  val angle: Double = 0.0,
  val energy: Long = 2400L,
  val maxEnergy: Long = 2400L,
  val bounty: Long = 100L,
  val lastPingType: String = "NONE",
  val lastPingX: Double = 0.0,
  val lastPingY: Double = 0.0,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

data class SyndicateInfo(
  val id: String,
  val tag: String,
  val name: String,
  val foundryOrigin: String,
  val motto: String,
  val memberCount: Int,
  val combinedBounty: Long,
  val isCustom: Boolean = false,
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

data class SyndicateChatMessage(
  val id: String,
  val senderUid: String,
  val senderCallsign: String,
  val senderSyndicateTag: String,
  val timestampLabel: String,
  val messageText: String,
  val isArmadaRecruitBanner: Boolean = false,
  val armadaName: String = "",
  val armadaCurrentSize: Int = 1,
  val armadaMaxSize: Int = 5,
  val armadaObjective: String = "",
  val requestSent: Boolean = false,
)
