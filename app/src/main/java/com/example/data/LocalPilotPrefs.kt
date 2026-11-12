package com.example.data

import android.content.Context
import android.content.SharedPreferences
import java.util.Locale
import java.util.UUID
import kotlin.math.ceil
import org.json.JSONArray
import org.json.JSONObject

class LocalPilotPrefs(context: Context) {
  private val prefs: SharedPreferences =
    context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

  /**
   * Returns a permanent device-bound UID stored in local storage so guest stats and leaderboard
   * identity follow the player forever across app launches without requiring Google OAuth popups.
   */
  fun getOrCreatePermanentDeviceUid(): String {
    val existing = prefs.getString(KEY_DEVICE_UID, null)
    if (!existing.isNullOrBlank()) return existing
    val generated = "dev_" + UUID.randomUUID().toString().replace("-", "").take(16)
    prefs.edit().putString(KEY_DEVICE_UID, generated).apply()
    return generated
  }

  fun isGuestBypassEnabled(): Boolean = prefs.getBoolean(KEY_GUEST_BYPASS, false)

  fun setGuestBypassEnabled(enabled: Boolean) {
    val editor = prefs.edit().putBoolean(KEY_GUEST_BYPASS, enabled)
    if (enabled && !prefs.contains(KEY_SYNDICATE_TAG)) {
      editor.putString(KEY_SYNDICATE_TAG, "GUEST")
      editor.putString(KEY_SYNDICATE_NAME, "Guest Tracking Framework")
    }
    editor.apply()
  }

  fun getCallsign(): String {
    return prefs.getString(KEY_CALLSIGN, "COMMANDER") ?: "COMMANDER"
  }

  fun setCallsign(rawCallsign: String): String {
    val cleaned =
      rawCallsign
        .trim()
        .uppercase()
        .replace(Regex("[^A-Z0-9_\\-]"), "")
        .take(14)
        .ifEmpty { "COMMANDER" }
    prefs.edit().putString(KEY_CALLSIGN, cleaned).apply()
    return cleaned
  }

  fun getSyndicateTag(): String {
    val raw = prefs.getString(KEY_SYNDICATE_TAG, "GUEST") ?: "GUEST"
    return if (raw.uppercase() in setOf("KSTL", "AURL", "VRTX", "RUST")) "GUEST" else raw
  }

  fun getSyndicateName(): String {
    val raw = prefs.getString(KEY_SYNDICATE_NAME, "Guest Tracking Framework") ?: "Guest Tracking Framework"
    return if (
      raw.contains("Kestrel", ignoreCase = true) ||
        raw.contains("Aurelia", ignoreCase = true) ||
        raw.contains("Vortex", ignoreCase = true) ||
        raw.contains("Rustweld", ignoreCase = true)
    ) {
      "Guest Tracking Framework"
    } else {
      raw
    }
  }

  fun getSyndicateLockUntilMs(): Long {
    return prefs.getLong(KEY_SYNDICATE_LOCK_UNTIL_MS, 0L)
  }

  fun getRemainingSyndicateLockDays(nowMs: Long = System.currentTimeMillis()): Int {
    val lockUntil = getSyndicateLockUntilMs()
    if (lockUntil <= nowMs) return 0
    val diffMs = lockUntil - nowMs
    return ceil(diffMs.toDouble() / (24.0 * 3600.0 * 1000.0)).toInt()
  }

  fun getRemainingSyndicateLockFormatted(nowMs: Long = System.currentTimeMillis()): String {
    val lockUntil = getSyndicateLockUntilMs()
    if (lockUntil <= nowMs) return "00d 00h"
    val diffMs = lockUntil - nowMs
    val totalHours = ceil(diffMs.toDouble() / (3600.0 * 1000.0)).toInt()
    val days = totalHours / 24
    val hours = totalHours % 24
    return String.format(Locale.US, "%02dd %02dh", days, hours)
  }

  fun joinOfficialSyndicate(
    tag: String,
    name: String,
    apply15DayLock: Boolean = true,
    role: String = "Pilot",
  ): Boolean {
    if (apply15DayLock && getRemainingSyndicateLockDays() > 0) {
      return false
    }
    val now = System.currentTimeMillis()
    val cleanTag = tag.uppercase().take(4).ifEmpty { "GUEST" }
    val cleanName = name.take(28).ifEmpty { "Guest Tracking Framework" }
    val lockUntil =
      if (apply15DayLock) {
        now + FIFTEEN_DAYS_MS
      } else {
        getSyndicateLockUntilMs()
      }
    prefs
      .edit()
      .putString(KEY_SYNDICATE_TAG, cleanTag)
      .putString(KEY_SYNDICATE_NAME, cleanName)
      .putLong(KEY_SYNDICATE_LOCK_UNTIL_MS, lockUntil)
      .apply()
    appendSyndicateServiceTransition(
      newTag = cleanTag,
      newName = cleanName,
      newRole = role,
      nowMs = now,
    )
    return true
  }

  fun leaveSyndicate(): Boolean {
    if (getRemainingSyndicateLockDays() > 0) {
      return false
    }
    val now = System.currentTimeMillis()
    val lockUntil = now + FIFTEEN_DAYS_MS
    prefs
      .edit()
      .putString(KEY_SYNDICATE_TAG, "GUEST")
      .putString(KEY_SYNDICATE_NAME, "Guest Tracking Framework")
      .putLong(KEY_SYNDICATE_LOCK_UNTIL_MS, lockUntil)
      .apply()
    appendSyndicateServiceTransition(
      newTag = "GUEST",
      newName = "Guest Tracking Framework",
      newRole = "Pilot",
      nowMs = now,
    )
    return true
  }

  fun getCameraZoom(): Float {
    return prefs.getFloat(KEY_CAMERA_ZOOM, 1.0f).coerceIn(0.5f, 1.5f)
  }

  fun setCameraZoom(zoom: Float) {
    prefs.edit().putFloat(KEY_CAMERA_ZOOM, zoom.coerceIn(0.5f, 1.5f)).apply()
  }

  // Active Named Armada (SOLO = "", or named up-to-5-man Armada e.g. "APEX_STRIKE")
  fun getActiveArmadaName(): String = prefs.getString(KEY_ARMADA_NAME, "") ?: ""

  fun setActiveArmadaName(armadaName: String) {
    val clean = armadaName.trim().uppercase().replace(Regex("[^A-Z0-9_\\-]"), "").take(12)
    prefs.edit().putString(KEY_ARMADA_NAME, clean).apply()
  }

  fun getMySyndicateRoster(
    currentUid: String,
    callsign: String,
    shipId: Int,
    bounty: Long,
    kills: Long,
  ): List<SyndicateMemberEntry> {
    val tag = getSyndicateTag()
    return listOf(
      SyndicateMemberEntry(
        uid = currentUid,
        callsign = callsign,
        tag = tag,
        shipId = shipId,
        shipName = "Hull #$shipId",
        role = "SYNDICATE COMMANDER (YOU)",
        bountyContribution = bounty.coerceAtLeast(getAllTimeBountyPeak()),
        kills = kills,
        isOnline = true,
        isLocalPlayer = true,
      ),
      SyndicateMemberEntry(
        uid = "${tag}_wing_1",
        callsign = "VANGUARD_01",
        tag = tag,
        shipId = ((shipId % 8) + 1),
        shipName = "Hull #${(shipId % 8) + 1}",
        role = "STRIKE WINGMAN",
        bountyContribution = 340L,
        kills = 14L,
        isOnline = true,
      ),
      SyndicateMemberEntry(
        uid = "${tag}_wing_2",
        callsign = "TRENCH_GUARD",
        tag = tag,
        shipId = 4,
        shipName = "Goliath",
        role = "HEAVY SIEGE",
        bountyContribution = 290L,
        kills = 11L,
        isOnline = true,
      ),
      SyndicateMemberEntry(
        uid = "${tag}_wing_3",
        callsign = "PHASE_RUNNER",
        tag = tag,
        shipId = 6,
        shipName = "Disruptor",
        role = "EMP INTERCEPTOR",
        bountyContribution = 245L,
        kills = 9L,
        isOnline = true,
      ),
      SyndicateMemberEntry(
        uid = "${tag}_wing_4",
        callsign = "SOLARIS_IX",
        tag = tag,
        shipId = 2,
        shipName = "Ricochet",
        role = "BOUNCE BOMBER",
        bountyContribution = 215L,
        kills = 7L,
        isOnline = false,
      ),
    )
  }

  fun getPreferredShipId(): Int = prefs.getInt(KEY_SHIP_ID, 1).coerceIn(1, 9)

  fun setPreferredShipId(shipId: Int) {
    prefs.edit().putInt(KEY_SHIP_ID, shipId.coerceIn(1, 9)).apply()
  }

  fun getPreferredMode(): String = "FFA"

  fun setPreferredMode(mode: String) {
    prefs.edit().putString(KEY_MODE, "FFA").apply()
  }

  fun getPvpKills(): Long = prefs.getLong(KEY_PVP_KILLS, 0L)
  fun getPvpDeaths(): Long = prefs.getLong(KEY_PVP_DEATHS, 0L)
  fun getPvpBountyPeak(): Long = prefs.getLong(KEY_PVP_BOUNTY_PEAK, 100L)
  fun getAllTimeKills(): Long = prefs.getLong(KEY_ALL_KILLS, 0L)
  fun getAllTimeDeaths(): Long = prefs.getLong(KEY_ALL_DEATHS, 0L)
  fun getAllTimeBountyPeak(): Long = prefs.getLong(KEY_ALL_BOUNTY_PEAK, 100L)
  fun getConvergencesWon(): Long = prefs.getLong(KEY_CONVERGENCES_WON, 0L)
  fun getPublicGameWins(): Long = getConvergencesWon()
  fun getHvtEliminations(): Long = prefs.getLong(KEY_HVT_ELIMINATIONS, 0L)
  fun getGreensCollected(): Long = prefs.getLong(KEY_GREENS_COLLECTED, 0L)

  // Phase 3: 5 XP Earning Sources & Pilot Level Progression (Levels 1..100) persisted per UID
  private fun uidKey(baseKey: String, uid: String? = null): String {
    val activeUid = uid?.takeIf { it.isNotBlank() } ?: getOrCreatePermanentDeviceUid()
    return "${baseKey}_$activeUid"
  }

  fun getXpFromBuffs(uid: String? = null): Long =
    prefs.getLong(uidKey(KEY_XP_BUFFS, uid), prefs.getLong(KEY_XP_BUFFS, 0L))

  fun getXpFromDamage(uid: String? = null): Long =
    prefs.getLong(uidKey(KEY_XP_DAMAGE, uid), prefs.getLong(KEY_XP_DAMAGE, 0L))

  fun getXpFromAccuracy(uid: String? = null): Long =
    prefs.getLong(uidKey(KEY_XP_ACCURACY, uid), prefs.getLong(KEY_XP_ACCURACY, 0L))

  fun getXpFromBountyEarned(uid: String? = null): Long =
    prefs.getLong(uidKey(KEY_XP_BOUNTY_EARNED, uid), prefs.getLong(KEY_XP_BOUNTY_EARNED, 0L))

  fun getXpFromBountyKilled(uid: String? = null): Long =
    prefs.getLong(uidKey(KEY_XP_BOUNTY_KILLED, uid), prefs.getLong(KEY_XP_BOUNTY_KILLED, 0L))

  fun getTotalXp(uid: String? = null): Long =
    getXpFromBuffs(uid) +
      getXpFromDamage(uid) +
      getXpFromAccuracy(uid) +
      getXpFromBountyEarned(uid) +
      getXpFromBountyKilled(uid)

  fun getPilotLevel(uid: String? = null): Int =
    computeLevelFromTotalXp(getTotalXp(uid))

  fun getCustomizationPointsTotal(uid: String? = null): Int =
    getPilotLevel(uid).coerceIn(1, 100)

  fun getSpentCustomizationPoints(uid: String? = null): Int =
    prefs.getInt(uidKey(KEY_SPENT_CUSTOM_POINTS, uid), prefs.getInt(KEY_SPENT_CUSTOM_POINTS, 0))
      .coerceIn(0, getCustomizationPointsTotal(uid))

  fun setSpentCustomizationPoints(points: Int, uid: String? = null) {
    val clamped = points.coerceIn(0, 100)
    prefs
      .edit()
      .putInt(KEY_SPENT_CUSTOM_POINTS, clamped)
      .putInt(uidKey(KEY_SPENT_CUSTOM_POINTS, uid), clamped)
      .apply()
  }

  fun getRiftShards(uid: String? = null): Long =
    prefs.getLong(uidKey(KEY_RIFT_SHARDS, uid), prefs.getLong(KEY_RIFT_SHARDS, 150L))

  fun setRiftShards(shards: Long, uid: String? = null) {
    val clean = shards.coerceAtLeast(0L)
    prefs
      .edit()
      .putLong(KEY_RIFT_SHARDS, clean)
      .putLong(uidKey(KEY_RIFT_SHARDS, uid), clean)
      .apply()
  }

  fun getHighestMilestoneLevelClaimed(uid: String? = null): Int =
    prefs.getInt(uidKey(KEY_HIGHEST_MILESTONE_LVL, uid), prefs.getInt(KEY_HIGHEST_MILESTONE_LVL, 0))

  fun getUnlockedExtraSlots(uid: String? = null): Int =
    prefs.getInt(uidKey(KEY_UNLOCKED_EXTRA_SLOTS, uid), prefs.getInt(KEY_UNLOCKED_EXTRA_SLOTS, 0))

  fun setUnlockedExtraSlots(slots: Int, uid: String? = null) {
    val clean = slots.coerceAtLeast(0)
    prefs
      .edit()
      .putInt(KEY_UNLOCKED_EXTRA_SLOTS, clean)
      .putInt(uidKey(KEY_UNLOCKED_EXTRA_SLOTS, uid), clean)
      .apply()
  }

  fun getAiShipDesignTokens(uid: String? = null): Int =
    prefs.getInt(uidKey(KEY_AI_DESIGN_TOKENS, uid), prefs.getInt(KEY_AI_DESIGN_TOKENS, 0))

  fun setAiShipDesignTokens(tokens: Int, uid: String? = null) {
    val clean = tokens.coerceAtLeast(0)
    prefs
      .edit()
      .putInt(KEY_AI_DESIGN_TOKENS, clean)
      .putInt(uidKey(KEY_AI_DESIGN_TOKENS, uid), clean)
      .apply()
  }

  // Phase 4: 9th Custom Modular Ship Loadout Slots (9A, 9B, 9C) & Slot Unlock Persistence
  fun getActiveCustomSlotId(uid: String? = null): String {
    val raw = prefs.getString(uidKey(KEY_ACTIVE_CUSTOM_SLOT_ID, uid), prefs.getString(KEY_ACTIVE_CUSTOM_SLOT_ID, "9A")) ?: "9A"
    return if (raw in setOf("9A", "9B", "9C")) raw else "9A"
  }

  fun setActiveCustomSlotId(slotId: String, uid: String? = null) {
    val clean = if (slotId in setOf("9A", "9B", "9C")) slotId else "9A"
    prefs
      .edit()
      .putString(KEY_ACTIVE_CUSTOM_SLOT_ID, clean)
      .putString(uidKey(KEY_ACTIVE_CUSTOM_SLOT_ID, uid), clean)
      .apply()
  }

  fun isCustomSlotUnlocked(slotId: String, uid: String? = null): Boolean {
    return when (slotId) {
      "9A" -> true
      "9B" ->
        prefs.getBoolean(uidKey(KEY_SLOT_9B_UNLOCKED, uid), prefs.getBoolean(KEY_SLOT_9B_UNLOCKED, false)) ||
          getUnlockedExtraSlots(uid) >= 1
      "9C" ->
        prefs.getBoolean(uidKey(KEY_SLOT_9C_UNLOCKED, uid), prefs.getBoolean(KEY_SLOT_9C_UNLOCKED, false)) ||
          getUnlockedExtraSlots(uid) >= 2
      else -> true
    }
  }

  fun setCustomSlotUnlocked(slotId: String, unlocked: Boolean, uid: String? = null) {
    val key =
      when (slotId) {
        "9B" -> KEY_SLOT_9B_UNLOCKED
        "9C" -> KEY_SLOT_9C_UNLOCKED
        else -> return
      }
    prefs
      .edit()
      .putBoolean(key, unlocked)
      .putBoolean(uidKey(key, uid), unlocked)
      .apply()
  }

  fun getCustomShipSlot(slotId: String, uid: String? = null): com.example.engine.CustomShipLoadoutSlot {
    val cleanId = if (slotId in setOf("9A", "9B", "9C")) slotId else "9A"
    val baseKey = "${KEY_CUSTOM_SLOT_PREFIX}_$cleanId"
    val raw = prefs.getString(uidKey(baseKey, uid), prefs.getString(baseKey, null))
    return com.example.engine.CustomShipLoadoutSlot.fromJsonString(raw, cleanId)
  }

  fun saveCustomShipSlot(slot: com.example.engine.CustomShipLoadoutSlot, uid: String? = null) {
    val cleanId = if (slot.slotId in setOf("9A", "9B", "9C")) slot.slotId else "9A"
    val baseKey = "${KEY_CUSTOM_SLOT_PREFIX}_$cleanId"
    val json = slot.toJsonString()
    prefs
      .edit()
      .putString(baseKey, json)
      .putString(uidKey(baseKey, uid), json)
      .apply()
  }

  fun savePilotProgression(
    uid: String? = null,
    xpFromBuffs: Long,
    xpFromDamage: Long,
    xpFromAccuracy: Long,
    xpFromBountyEarned: Long,
    xpFromBountyKilled: Long,
    riftShards: Long,
    highestMilestoneLevelClaimed: Int,
  ) {
    val cleanShards = riftShards.coerceAtLeast(0L)
    prefs
      .edit()
      .putLong(KEY_XP_BUFFS, xpFromBuffs)
      .putLong(uidKey(KEY_XP_BUFFS, uid), xpFromBuffs)
      .putLong(KEY_XP_DAMAGE, xpFromDamage)
      .putLong(uidKey(KEY_XP_DAMAGE, uid), xpFromDamage)
      .putLong(KEY_XP_ACCURACY, xpFromAccuracy)
      .putLong(uidKey(KEY_XP_ACCURACY, uid), xpFromAccuracy)
      .putLong(KEY_XP_BOUNTY_EARNED, xpFromBountyEarned)
      .putLong(uidKey(KEY_XP_BOUNTY_EARNED, uid), xpFromBountyEarned)
      .putLong(KEY_XP_BOUNTY_KILLED, xpFromBountyKilled)
      .putLong(uidKey(KEY_XP_BOUNTY_KILLED, uid), xpFromBountyKilled)
      .putLong(KEY_RIFT_SHARDS, cleanShards)
      .putLong(uidKey(KEY_RIFT_SHARDS, uid), cleanShards)
      .putInt(KEY_HIGHEST_MILESTONE_LVL, highestMilestoneLevelClaimed)
      .putInt(uidKey(KEY_HIGHEST_MILESTONE_LVL, uid), highestMilestoneLevelClaimed)
      .apply()
  }

  // Adaptive Armada Tier Kills (1 = SOLO, 2 = 2-Man, 3 = 3-Man, 4 = 4-Man, 5 = 5-Man Armada)
  fun getArmadaTierKills(tierSize: Int): Long {
    val clamped = tierSize.coerceIn(1, 5)
    return prefs.getLong("armada_tier_${clamped}_kills", 0L)
  }

  fun getArmadaTierBountyPeak(tierSize: Int): Long {
    val clamped = tierSize.coerceIn(1, 5)
    return prefs.getLong("armada_tier_${clamped}_bounty", 100L)
  }

  fun recordArmadaTierStat(tierSize: Int, addKills: Long, currentBounty: Long) {
    val clamped = tierSize.coerceIn(1, 5)
    val newKills = getArmadaTierKills(clamped) + addKills
    val newPeak = kotlin.math.max(getArmadaTierBountyPeak(clamped), currentBounty)
    prefs
      .edit()
      .putLong("armada_tier_${clamped}_kills", newKills)
      .putLong("armada_tier_${clamped}_bounty", newPeak)
      .apply()
  }

  fun saveCombatStats(
    pvpKills: Long,
    pvpDeaths: Long,
    pvpBountyPeak: Long,
    allTimeKills: Long,
    allTimeDeaths: Long,
    allTimeBountyPeak: Long,
    convergencesWon: Long,
    hvtEliminations: Long,
    greensCollected: Long,
  ) {
    prefs
      .edit()
      .putLong(KEY_PVP_KILLS, pvpKills)
      .putLong(KEY_PVP_DEATHS, pvpDeaths)
      .putLong(KEY_PVP_BOUNTY_PEAK, pvpBountyPeak)
      .putLong(KEY_ALL_KILLS, allTimeKills)
      .putLong(KEY_ALL_DEATHS, allTimeDeaths)
      .putLong(KEY_ALL_BOUNTY_PEAK, allTimeBountyPeak)
      .putLong(KEY_CONVERGENCES_WON, convergencesWon)
      .putLong(KEY_HVT_ELIMINATIONS, hvtEliminations)
      .putLong(KEY_GREENS_COLLECTED, greensCollected)
      .apply()
  }

  fun getFriendsList(): List<FriendPilotEntry> {
    val tag = getSyndicateTag()
    val defaults =
      listOf(
        FriendPilotEntry(
          uid = "friend_viper_ace",
          callsign = "VIPER_ACE",
          syndicateTag = tag,
          shipId = 1,
          statusText = "In Rift (Sector D4) • Flying [APEX_4] Armada",
          isOnline = true,
          currentArmadaName = "APEX_4",
          currentArmadaCount = 4,
          bounty = 320,
        ),
        FriendPilotEntry(
          uid = "friend_solaris_prime",
          callsign = "SOLARIS_PRIME",
          syndicateTag = "SOLR",
          shipId = 4,
          statusText = "In Rift (Sector C3) • Flying [GOLD_WING] Armada",
          isOnline = true,
          currentArmadaName = "GOLD_WING",
          currentArmadaCount = 2,
          bounty = 275,
        ),
        FriendPilotEntry(
          uid = "friend_chrono_x",
          callsign = "CHRONO_X",
          syndicateTag = tag,
          shipId = 6,
          statusText = "In Rift (Safe Zone) • [SOLO] Available for Armada",
          isOnline = true,
          currentArmadaName = "",
          currentArmadaCount = 1,
          bounty = 190,
        ),
        FriendPilotEntry(
          uid = "friend_valkyrie_z",
          callsign = "VALKYRIE_Z",
          syndicateTag = "VOID",
          shipId = 7,
          statusText = "In Hangar • Tuning 7-Valkyrie Loadout",
          isOnline = true,
          currentArmadaName = "",
          currentArmadaCount = 1,
          bounty = 150,
        ),
        FriendPilotEntry(
          uid = "friend_manta_ray",
          callsign = "MANTA_RAY",
          syndicateTag = tag,
          shipId = 8,
          statusText = "Offline • Last seen 2h ago",
          isOnline = false,
          currentArmadaName = "",
          currentArmadaCount = 1,
          bounty = 100,
        ),
      )

    val customRaw = prefs.getString(KEY_CUSTOM_FRIENDS_JSON, "[]") ?: "[]"
    val customList = mutableListOf<FriendPilotEntry>()
    try {
      val arr = JSONArray(customRaw)
      for (i in 0 until arr.length()) {
        val obj = arr.getJSONObject(i)
        customList.add(
          FriendPilotEntry(
            uid = obj.optString("uid", "friend_custom_$i"),
            callsign = obj.optString("callsign", "WINGMAN_$i"),
            syndicateTag = obj.optString("syndicateTag", tag),
            shipId = obj.optInt("shipId", 1),
            statusText = obj.optString("statusText", "In Rift (Sector E5) • [SOLO] Online"),
            isOnline = true,
            currentArmadaName = obj.optString("currentArmadaName", ""),
            currentArmadaCount = obj.optInt("currentArmadaCount", 1),
            bounty = obj.optInt("bounty", 140),
          )
        )
      }
    } catch (_: Exception) {}

    return customList + defaults
  }

  fun addFriendByCallsign(rawCallsign: String): FriendPilotEntry? {
    val clean =
      rawCallsign
        .trim()
        .uppercase()
        .replace(Regex("[^A-Z0-9_\\-]"), "")
        .take(14)
    if (clean.length < 3) return null
    val existing = getFriendsList()
    if (existing.any { it.callsign.equals(clean, ignoreCase = true) }) {
      return existing.first { it.callsign.equals(clean, ignoreCase = true) }
    }

    val created =
      FriendPilotEntry(
        uid = "friend_custom_${System.currentTimeMillis()}",
        callsign = clean,
        syndicateTag = getSyndicateTag(),
        shipId = ((clean.hashCode().and(0x7FFFFFFF)) % 8) + 1,
        statusText = "In Rift (Sector B4) • [SOLO] Ready for Armada",
        isOnline = true,
        currentArmadaName = "",
        currentArmadaCount = 1,
        bounty = 160,
      )

    val customRaw = prefs.getString(KEY_CUSTOM_FRIENDS_JSON, "[]") ?: "[]"
    val arr =
      try {
        JSONArray(customRaw)
      } catch (_: Exception) {
        JSONArray()
      }
    arr.put(
      JSONObject().apply {
        put("uid", created.uid)
        put("callsign", created.callsign)
        put("syndicateTag", created.syndicateTag)
        put("shipId", created.shipId)
        put("statusText", created.statusText)
        put("currentArmadaName", created.currentArmadaName)
        put("currentArmadaCount", created.currentArmadaCount)
        put("bounty", created.bounty)
      }
    )
    prefs.edit().putString(KEY_CUSTOM_FRIENDS_JSON, arr.toString()).apply()
    return created
  }

  fun getAllSyndicates(): List<SyndicateInfo> {
    val defaults =
      listOf(
        SyndicateInfo(
          id = "syn_apex",
          tag = "APEX",
          name = "Apex Strike Vanguard",
          foundryOrigin = "Core Sector Coalition",
          motto = "Precision Rail Dominance & Ballistic Angles",
          memberCount = 142,
          combinedBounty = 48_900L,
        ),
        SyndicateInfo(
          id = "syn_solaris",
          tag = "SOLR",
          name = "Solaris Heavy Guard",
          foundryOrigin = "Outer Rim Bastion",
          motto = "Heavy Siege Dreadnoughts & Trench Breakers",
          memberCount = 118,
          combinedBounty = 44_250L,
        ),
        SyndicateInfo(
          id = "syn_eclipse",
          tag = "ECLP",
          name = "Eclipse Stealth Collective",
          foundryOrigin = "Deep Rift Enclave",
          motto = "Tactical Cloak, EMP Disruption & Interceptor Swarms",
          memberCount = 131,
          combinedBounty = 46_100L,
        ),
        SyndicateInfo(
          id = "syn_void",
          tag = "VOID",
          name = "Void Horizon Ironworks",
          foundryOrigin = "Frontier Mining Guild",
          motto = "Proximity Flak Barrages & Chokepoint Minefields",
          memberCount = 96,
          combinedBounty = 39_800L,
        ),
        SyndicateInfo(
          id = "syn_nova",
          tag = "NOVA",
          name = "Nova Core Raiders",
          foundryOrigin = "Independent Sector Fleet",
          motto = "Zero-Friction Drift & Center Trench Control",
          memberCount = 84,
          combinedBounty = 35_400L,
        ),
      )

    val customRaw = prefs.getString(KEY_CUSTOM_SYNDICATES_JSON, "[]") ?: "[]"
    val customList = mutableListOf<SyndicateInfo>()
    try {
      val arr = JSONArray(customRaw)
      for (i in 0 until arr.length()) {
        val obj = arr.getJSONObject(i)
        customList.add(
          SyndicateInfo(
            id = obj.optString("id", "syn_custom_$i"),
            tag = obj.optString("tag", "NOVA"),
            name = obj.optString("name", "Custom Syndicate"),
            foundryOrigin = obj.optString("foundryOrigin", "Player Registered Syndicate"),
            motto = obj.optString("motto", "Forged in the Convergence"),
            memberCount = obj.optInt("memberCount", 1),
            combinedBounty = obj.optLong("combinedBounty", 1000L),
            isCustom = true,
          )
        )
      }
    } catch (_: Exception) {}

    return customList + defaults
  }

  fun createCustomSyndicate(tag: String, name: String, motto: String): Result<SyndicateInfo> {
    val now = System.currentTimeMillis()
    val remainingDays = getRemainingSyndicateLockDays(now)
    if (remainingDays > 0) {
      return Result.failure(
        IllegalStateException(
          "15-Day Syndicate Lock active (${getRemainingSyndicateLockFormatted(now)} remaining)."
        )
      )
    }

    val cleanTag = tag.trim().uppercase().replace(Regex("[^A-Z0-9]"), "")
    if (cleanTag.length !in 3..4) {
      return Result.failure(IllegalArgumentException("Syndicate Tag must be 3 to 4 letters/digits."))
    }
    val cleanName = name.trim().replace(Regex("[^a-zA-Z0-9 _\\-]"), "").take(26)
    if (cleanName.length < 3) {
      return Result.failure(IllegalArgumentException("Syndicate Name must be at least 3 characters."))
    }

    val existing = getAllSyndicates()
    if (existing.any { it.tag.equals(cleanTag, ignoreCase = true) }) {
      return Result.failure(IllegalArgumentException("Tag [$cleanTag] is already registered."))
    }

    val newSyndicate =
      SyndicateInfo(
        id = "syn_custom_$now",
        tag = cleanTag,
        name = cleanName,
        foundryOrigin = "Player Founded Syndicate",
        motto = motto.trim().take(60).ifEmpty { "Forged in the RIFT Convergence" },
        memberCount = 1,
        combinedBounty = getAllTimeBountyPeak().coerceAtLeast(250L),
        isCustom = true,
      )

    val customRaw = prefs.getString(KEY_CUSTOM_SYNDICATES_JSON, "[]") ?: "[]"
    val arr =
      try {
        JSONArray(customRaw)
      } catch (_: Exception) {
        JSONArray()
      }
    arr.put(
      JSONObject().apply {
        put("id", newSyndicate.id)
        put("tag", newSyndicate.tag)
        put("name", newSyndicate.name)
        put("foundryOrigin", newSyndicate.foundryOrigin)
        put("motto", newSyndicate.motto)
        put("memberCount", newSyndicate.memberCount)
        put("combinedBounty", newSyndicate.combinedBounty)
      }
    )

    prefs.edit().putString(KEY_CUSTOM_SYNDICATES_JSON, arr.toString()).apply()
    joinOfficialSyndicate(cleanTag, cleanName, apply15DayLock = true, role = "Founder / Commander")
    return Result.success(newSyndicate)
  }

  fun getAllTimePeakBountySnapshot(uid: String? = null): PeakBountyRunSnapshot? {
    val raw = prefs.getString(uidKey(KEY_ALL_TIME_PEAK_SNAPSHOT, uid), prefs.getString(KEY_ALL_TIME_PEAK_SNAPSHOT, null))
    return PeakBountyRunSnapshot.fromJsonString(raw)
  }

  fun saveAllTimePeakBountySnapshot(snapshot: PeakBountyRunSnapshot, uid: String? = null) {
    val json = snapshot.toJsonString()
    prefs
      .edit()
      .putString(KEY_ALL_TIME_PEAK_SNAPSHOT, json)
      .putString(uidKey(KEY_ALL_TIME_PEAK_SNAPSHOT, uid), json)
      .apply()
  }

  fun getPvpPeakBountySnapshot(uid: String? = null): PeakBountyRunSnapshot? {
    val raw = prefs.getString(uidKey(KEY_PVP_PEAK_SNAPSHOT, uid), prefs.getString(KEY_PVP_PEAK_SNAPSHOT, null))
    return PeakBountyRunSnapshot.fromJsonString(raw)
  }

  fun savePvpPeakBountySnapshot(snapshot: PeakBountyRunSnapshot, uid: String? = null) {
    val json = snapshot.toJsonString()
    prefs
      .edit()
      .putString(KEY_PVP_PEAK_SNAPSHOT, json)
      .putString(uidKey(KEY_PVP_PEAK_SNAPSHOT, uid), json)
      .apply()
  }

  /**
   * Evaluates a completed (or live milestone) life snapshot against historical All-Time and PvP-Only
   * peak bounty snapshots, saving whenever it sets or updates the record (including Executioner telemetry on death).
   */
  fun maybeRecordPeakBountySnapshot(
    snapshot: PeakBountyRunSnapshot,
    hadPvp: Boolean,
    isFinalizedLifeEnd: Boolean = true,
    uid: String? = null,
  ) {
    val existingAll = getAllTimePeakBountySnapshot(uid)
    val shouldSaveAll =
      existingAll == null ||
        snapshot.bountyReached > existingAll.bountyReached ||
        (isFinalizedLifeEnd &&
          snapshot.bountyReached == existingAll.bountyReached &&
          existingAll.executioner.killerCallsign.isBlank() &&
          !existingAll.executioner.survivedToConvergence)
    if (shouldSaveAll) {
      saveAllTimePeakBountySnapshot(snapshot, uid)
    }

    if (hadPvp || snapshot.pvpKills > 0) {
      val existingPvp = getPvpPeakBountySnapshot(uid)
      val shouldSavePvp =
        existingPvp == null ||
          snapshot.bountyReached > existingPvp.bountyReached ||
          (isFinalizedLifeEnd &&
            snapshot.bountyReached == existingPvp.bountyReached &&
            existingPvp.executioner.killerCallsign.isBlank() &&
            !existingPvp.executioner.survivedToConvergence)
      if (shouldSavePvp) {
        savePvpPeakBountySnapshot(snapshot, uid)
      }
    }
  }

  fun getSyndicateServiceHistory(uid: String? = null): List<SyndicateServiceEntry> {
    val raw =
      prefs.getString(uidKey(KEY_SYNDICATE_HISTORY_JSON, uid), prefs.getString(KEY_SYNDICATE_HISTORY_JSON, null))
    val parsed = mutableListOf<SyndicateServiceEntry>()
    if (!raw.isNullOrBlank()) {
      try {
        val arr = JSONArray(raw)
        for (i in 0 until arr.length()) {
          parsed.add(SyndicateServiceEntry.fromJson(arr.getJSONObject(i)))
        }
      } catch (_: Exception) {}
    }
    if (parsed.isEmpty()) {
      val currentTag = getSyndicateTag()
      val currentName = getSyndicateName()
      val isCustomFounder =
        getAllSyndicates().any { it.isCustom && it.tag.equals(currentTag, ignoreCase = true) }
      val seed =
        SyndicateServiceEntry(
          syndicateTag = currentTag,
          syndicateName = currentName,
          role = if (isCustomFounder) "Founder / Commander" else "Pilot",
          joinedTimestamp = prefs.getLong(KEY_FIRST_COMMISSION_MS, 0L).let {
            if (it > 0L) it else {
              val now = System.currentTimeMillis()
              prefs.edit().putLong(KEY_FIRST_COMMISSION_MS, now).apply()
              now
            }
          },
          leftTimestamp = 0L,
          totalKills = getAllTimeKills(),
          peakBountyAchieved = getAllTimeBountyPeak(),
          publicGameWins = getConvergencesWon(),
        )
      saveSyndicateServiceHistory(listOf(seed), uid)
      return listOf(seed)
    }
    return parsed
  }

  fun saveSyndicateServiceHistory(entries: List<SyndicateServiceEntry>, uid: String? = null) {
    val arr = JSONArray()
    entries.forEach { arr.put(it.toJson()) }
    val json = arr.toString()
    prefs
      .edit()
      .putString(KEY_SYNDICATE_HISTORY_JSON, json)
      .putString(uidKey(KEY_SYNDICATE_HISTORY_JSON, uid), json)
      .apply()
  }

  private fun appendSyndicateServiceTransition(
    newTag: String,
    newName: String,
    newRole: String,
    nowMs: Long = System.currentTimeMillis(),
    uid: String? = null,
  ) {
    val currentList = getSyndicateServiceHistory(uid).toMutableList()
    val activeIdx = currentList.indexOfLast { it.leftTimestamp <= 0L }
    if (activeIdx >= 0) {
      val active = currentList[activeIdx]
      if (active.syndicateTag.equals(newTag, ignoreCase = true) && active.role == newRole) {
        return
      }
      currentList[activeIdx] = active.copy(leftTimestamp = nowMs)
    }
    currentList.add(
      SyndicateServiceEntry(
        syndicateTag = newTag,
        syndicateName = newName,
        role = newRole,
        joinedTimestamp = nowMs,
        leftTimestamp = 0L,
        totalKills = 0L,
        peakBountyAchieved = 100L,
        publicGameWins = 0L,
      )
    )
    saveSyndicateServiceHistory(currentList, uid)
  }

  fun recordSyndicateContribution(
    addKills: Long = 0L,
    currentBounty: Long = 100L,
    addPublicWins: Long = 0L,
    uid: String? = null,
  ) {
    val currentList = getSyndicateServiceHistory(uid).toMutableList()
    val activeIdx = currentList.indexOfLast { it.leftTimestamp <= 0L }
    if (activeIdx >= 0) {
      val active = currentList[activeIdx]
      currentList[activeIdx] =
        active.copy(
          totalKills = (active.totalKills + addKills).coerceAtLeast(0L),
          peakBountyAchieved = kotlin.math.max(active.peakBountyAchieved, currentBounty),
          publicGameWins = (active.publicGameWins + addPublicWins).coerceAtLeast(0L),
        )
      saveSyndicateServiceHistory(currentList, uid)
    }
  }

  fun getPlayfieldTextScale(): Float = prefs.getFloat(KEY_PLAYFIELD_TEXT_SCALE, 1.0f)

  fun setPlayfieldTextScale(scale: Float) {
    prefs.edit().putFloat(KEY_PLAYFIELD_TEXT_SCALE, scale.coerceIn(0.75f, 2.0f)).apply()
  }

  companion object {
    private const val PREFS_NAME = "rift_permanent_pilot_prefs"
    private const val KEY_DEVICE_UID = "permanent_device_uid"
    private const val KEY_GUEST_BYPASS = "guest_bypass_enabled"
    private const val KEY_CALLSIGN = "pilot_callsign"
    private const val KEY_SYNDICATE_TAG = "syndicate_tag"
    private const val KEY_SYNDICATE_NAME = "syndicate_name"
    private const val KEY_SYNDICATE_LOCK_UNTIL_MS = "syndicate_lock_until_ms"
    private const val KEY_ARMADA_NAME = "active_armada_name"
    private const val KEY_CUSTOM_SYNDICATES_JSON = "custom_syndicates_json"
    private const val KEY_CUSTOM_FRIENDS_JSON = "custom_friends_json"
    private const val KEY_SHIP_ID = "preferred_ship_id"
    private const val KEY_MODE = "preferred_mode"
    private const val KEY_PVP_KILLS = "pvp_kills"
    private const val KEY_PVP_DEATHS = "pvp_deaths"
    private const val KEY_PVP_BOUNTY_PEAK = "pvp_bounty_peak"
    private const val KEY_ALL_KILLS = "all_time_kills"
    private const val KEY_ALL_DEATHS = "all_time_deaths"
    private const val KEY_ALL_BOUNTY_PEAK = "all_time_bounty_peak"
    private const val KEY_CONVERGENCES_WON = "convergences_won"
    private const val KEY_HVT_ELIMINATIONS = "hvt_eliminations"
    private const val KEY_GREENS_COLLECTED = "greens_collected"
    private const val KEY_CAMERA_ZOOM = "camera_zoom_level"
    private const val KEY_PLAYFIELD_TEXT_SCALE = "playfield_text_scale"

    // Phase 3: Pilot Level (1..100), 5 XP Sources & Rift Shards Currency Keys
    private const val KEY_XP_BUFFS = "xp_source_buffs"
    private const val KEY_XP_DAMAGE = "xp_source_damage"
    private const val KEY_XP_ACCURACY = "xp_source_accuracy"
    private const val KEY_XP_BOUNTY_EARNED = "xp_source_bounty_earned"
    private const val KEY_XP_BOUNTY_KILLED = "xp_source_bounty_killed"
    private const val KEY_RIFT_SHARDS = "rift_shards_balance"
    private const val KEY_HIGHEST_MILESTONE_LVL = "highest_milestone_level_claimed"
    private const val KEY_SPENT_CUSTOM_POINTS = "spent_customization_points"
    private const val KEY_UNLOCKED_EXTRA_SLOTS = "unlocked_extra_hull_slots"
    private const val KEY_AI_DESIGN_TOKENS = "ai_ship_design_tokens"
    private const val KEY_ACTIVE_CUSTOM_SLOT_ID = "active_custom_slot_id"
    private const val KEY_SLOT_9B_UNLOCKED = "custom_slot_9b_unlocked"
    private const val KEY_SLOT_9C_UNLOCKED = "custom_slot_9c_unlocked"
    private const val KEY_CUSTOM_SLOT_PREFIX = "custom_ship_slot_json"
    private const val KEY_ALL_TIME_PEAK_SNAPSHOT = "all_time_peak_bounty_snapshot_json"
    private const val KEY_PVP_PEAK_SNAPSHOT = "pvp_peak_bounty_snapshot_json"
    private const val KEY_SYNDICATE_HISTORY_JSON = "syndicate_service_history_json"
    private const val KEY_FIRST_COMMISSION_MS = "pilot_first_commission_ms"

    const val FIFTEEN_DAYS_MS = 15L * 24L * 3600L * 1000L

    /**
     * Cumulative XP required to reach the start of Level [level] (1..100).
     * Level 1 starts at 0 XP; Level 2 requires 250 XP; each subsequent level step requires +50 XP more.
     */
    fun xpForLevelStart(level: Int): Long {
      val clamped = level.coerceIn(1, 100)
      if (clamped <= 1) return 0L
      val n = (clamped - 1).toLong()
      return n * 250L + (n * (n - 1L) / 2L) * 50L
    }

    /**
     * Computes Pilot Level (1..100) from cumulative total XP.
     */
    fun computeLevelFromTotalXp(totalXp: Long): Int {
      val cleanXp = totalXp.coerceAtLeast(0L)
      for (lvl in 100 downTo 1) {
        if (cleanXp >= xpForLevelStart(lvl)) {
          return lvl
        }
      }
      return 1
    }

    /**
     * Returns (xpIntoCurrentLevel, xpSpanForCurrentLevel, progressFraction 0f..1f).
     */
    fun getXpProgressInLevel(totalXp: Long): Triple<Long, Long, Float> {
      val cleanXp = totalXp.coerceAtLeast(0L)
      val lvl = computeLevelFromTotalXp(cleanXp)
      if (lvl >= 100) {
        val capXp = xpForLevelStart(100)
        return Triple(cleanXp - capXp, 0L, 1.0f)
      }
      val startXp = xpForLevelStart(lvl)
      val nextXp = xpForLevelStart(lvl + 1)
      val span = (nextXp - startXp).coerceAtLeast(1L)
      val into = (cleanXp - startXp).coerceAtLeast(0L)
      val frac = (into.toFloat() / span.toFloat()).coerceIn(0f, 1f)
      return Triple(into, span, frac)
    }
  }
}
