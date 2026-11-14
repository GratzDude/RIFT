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
  private val userSettingsPrefs: SharedPreferences =
    context.applicationContext.getSharedPreferences(USER_SETTINGS_PREFS_NAME, Context.MODE_PRIVATE)

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
      editor.putString(KEY_SYNDICATE_TAG, "")
      editor.putString(KEY_SYNDICATE_NAME, "Unaffiliated")
    }
    editor.apply()
  }

  fun getCallsign(): String {
    return prefs.getString(KEY_CALLSIGN, "COMMANDER") ?: "COMMANDER"
  }

  fun setCallsign(rawCallsign: String): String {
    val sanitizedRaw =
      rawCallsign
        .trim()
        .uppercase()
        .replace(Regex("[^A-Z0-9_\\-]"), "")
        .take(14)
    val cleaned =
      if (sanitizedRaw.isEmpty() || UgcModeration.containsProfanity(sanitizedRaw)) {
        "COMMANDER"
      } else {
        sanitizedRaw
      }
    prefs.edit().putString(KEY_CALLSIGN, cleaned).apply()
    return cleaned
  }

  fun getSyndicateTag(): String {
    val raw = (prefs.getString(KEY_SYNDICATE_TAG, "") ?: "").trim().uppercase()
    return if (raw in setOf("GUEST", "UNAFFILIATED", "NONE", "SOLO", "KSTL", "AURL", "VRTX", "RUST")) {
      ""
    } else {
      raw
    }
  }

  fun getSyndicateName(): String {
    val tag = getSyndicateTag()
    if (tag.isBlank()) return "Unaffiliated"
    val raw = (prefs.getString(KEY_SYNDICATE_NAME, "Unaffiliated") ?: "Unaffiliated").trim()
    return if (
      raw.isEmpty() ||
        raw.equals("Guest Tracking Framework", ignoreCase = true) ||
        raw.contains("Kestrel", ignoreCase = true) ||
        raw.contains("Aurelia", ignoreCase = true) ||
        raw.contains("Vortex", ignoreCase = true) ||
        raw.contains("Rustweld", ignoreCase = true)
    ) {
      "Unaffiliated"
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
    val cleanTag = tag.trim().uppercase().take(4)
    val cleanName = name.trim().take(28).ifEmpty { if (cleanTag.isEmpty()) "Unaffiliated" else "$cleanTag Syndicate" }
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
      newTag = cleanTag.ifEmpty { "UNAFFILIATED" },
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
      .putString(KEY_SYNDICATE_TAG, "")
      .putString(KEY_SYNDICATE_NAME, "Unaffiliated")
      .putLong(KEY_SYNDICATE_LOCK_UNTIL_MS, lockUntil)
      .apply()
    appendSyndicateServiceTransition(
      newTag = "UNAFFILIATED",
      newName = "Unaffiliated",
      newRole = "Pilot",
      nowMs = now,
    )
    return true
  }

  fun getCameraZoom(): Float {
    val fallback = prefs.getFloat(KEY_CAMERA_ZOOM, 1.0f)
    return userSettingsPrefs.getFloat(KEY_CAMERA_ZOOM, fallback).coerceIn(0.5f, 1.5f)
  }

  fun setCameraZoom(zoom: Float) {
    val clamped = zoom.coerceIn(0.5f, 1.5f)
    userSettingsPrefs.edit().putFloat(KEY_CAMERA_ZOOM, clamped).apply()
  }

  // Active Named Armada (SOLO = "", or named up-to-5-man Armada e.g. "APEX_STRIKE")
  fun getActiveArmadaName(): String = prefs.getString(KEY_ARMADA_NAME, "") ?: ""

  fun setActiveArmadaName(armadaName: String) {
    val rawClean = armadaName.trim().uppercase().replace(Regex("[^A-Z0-9_\\-]"), "").take(12)
    val clean = if (UgcModeration.containsProfanity(rawClean)) "APEX_5" else rawClean
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
    if (tag.isBlank()) return emptyList()
    val localEntry =
      SyndicateMemberEntry(
        uid = currentUid,
        callsign = callsign,
        tag = tag,
        shipId = shipId,
        shipName = cleanShipNameForId(shipId),
        role = "SYNDICATE COMMANDER (YOU)",
        bountyContribution = bounty.coerceAtLeast(getAllTimeBountyPeak()),
        kills = kills,
        isOnline = true,
        isLocalPlayer = true,
      )
    val realSyndicateFriends =
      getFriendsList()
        .filter {
          it.syndicateTag.equals(tag, ignoreCase = true) &&
            !it.callsign.equals(callsign, ignoreCase = true)
        }
        .map { friend ->
          SyndicateMemberEntry(
            uid = friend.uid,
            callsign = friend.callsign,
            tag = tag,
            shipId = friend.shipId,
            shipName = cleanShipNameForId(friend.shipId),
            role = "SYNDICATE WINGMAN",
            bountyContribution = friend.bounty.toLong(),
            kills = 0L,
            isOnline = friend.isOnline,
            isLocalPlayer = false,
          )
        }
    return listOf(localEntry) + realSyndicateFriends
  }

  fun getSyndicateMembers(
    currentUid: String,
    callsign: String,
    shipId: Int,
    bounty: Long,
    kills: Long,
  ): List<SyndicateMemberEntry> =
    getMySyndicateRoster(currentUid, callsign, shipId, bounty, kills)

  fun getPreferredShipId(): Int = prefs.getInt(KEY_SHIP_ID, 1).coerceIn(1, 10)

  fun setPreferredShipId(shipId: Int) {
    prefs.edit().putInt(KEY_SHIP_ID, shipId.coerceIn(1, 10)).apply()
  }

  fun getLastShipId(): Int = getPreferredShipId()

  fun setLastShipId(shipId: Int) = setPreferredShipId(shipId)

  /**
   * Startup Boot Preference:
   * true (default) = open NEXUS Command Hub on startup.
   * false = fast-boot straight into the arena with the Changelog popup & Quick-Join bar.
   */
  fun getBootToNexusHub(): Boolean {
    val fallback = prefs.getBoolean(KEY_BOOT_TO_NEXUS_HUB, true)
    return userSettingsPrefs.getBoolean(KEY_BOOT_TO_NEXUS_HUB, fallback)
  }

  fun setBootToNexusHub(enabled: Boolean) {
    prefs.edit().putBoolean(KEY_BOOT_TO_NEXUS_HUB, enabled).apply()
    userSettingsPrefs.edit().putBoolean(KEY_BOOT_TO_NEXUS_HUB, enabled).apply()
  }

  /**
   * Best Ship Avatar Calculator:
   * Returns the ship ID (1..10) from the pilot's PvP Peak Bounty Snapshot or All-Time Peak Bounty Snapshot
   * (whichever has the higher bountyReached), falling back to getLastShipId() / fallbackShipId (1..10).
   */
  fun getBestShipId(uid: String? = null, fallbackShipId: Int = getLastShipId()): Int {
    val pvpSnap = getPvpPeakBountySnapshot(uid)
    val allSnap = getAllTimePeakBountySnapshot(uid)
    val bestSnap =
      when {
        pvpSnap != null && allSnap != null ->
          if (pvpSnap.bountyReached >= allSnap.bountyReached) pvpSnap else allSnap
        pvpSnap != null -> pvpSnap
        allSnap != null -> allSnap
        else -> null
      }
    val resolvedId = bestSnap?.shipId?.takeIf { it in 1..10 } ?: fallbackShipId.takeIf { it in 1..10 } ?: getLastShipId()
    return resolvedId.coerceIn(1, 10)
  }

  fun getPreferredMode(): String = prefs.getString(KEY_MODE, "SECTOR_CONVERGENCE") ?: "SECTOR_CONVERGENCE"

  fun setPreferredMode(mode: String) {
    val clean = mode.trim().uppercase(Locale.US).ifEmpty { "SECTOR_CONVERGENCE" }
    prefs.edit().putString(KEY_MODE, clean).apply()
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
    prefs.getLong(uidKey(KEY_RIFT_SHARDS, uid), prefs.getLong(KEY_RIFT_SHARDS, 0L))

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

  // Custom Modular Ship Loadout Slots (Frame A, Frame B, Frame C) with seamless legacy migration
  fun normalizeCustomSlotId(rawSlotId: String?): String =
    when (rawSlotId?.trim()?.uppercase(Locale.US)) {
      "FRAME B", "9B", "B" -> "Frame B"
      "FRAME C", "9C", "C" -> "Frame C"
      else -> "Frame A"
    }

  private fun legacySlotKeySuffix(cleanSlotId: String): String =
    when (cleanSlotId) {
      "Frame B" -> "9B"
      "Frame C" -> "9C"
      else -> "9A"
    }

  fun getActiveCustomSlotId(uid: String? = null): String {
    val raw =
      prefs.getString(
        uidKey(KEY_ACTIVE_CUSTOM_SLOT_ID, uid),
        prefs.getString(KEY_ACTIVE_CUSTOM_SLOT_ID, "Frame A"),
      ) ?: "Frame A"
    return normalizeCustomSlotId(raw)
  }

  fun setActiveCustomSlotId(slotId: String, uid: String? = null) {
    val clean = normalizeCustomSlotId(slotId)
    prefs
      .edit()
      .putString(KEY_ACTIVE_CUSTOM_SLOT_ID, clean)
      .putString(uidKey(KEY_ACTIVE_CUSTOM_SLOT_ID, uid), clean)
      .apply()
  }

  fun isCustomSlotUnlocked(slotId: String, uid: String? = null): Boolean {
    return when (normalizeCustomSlotId(slotId)) {
      "Frame A" -> true
      "Frame B" ->
        prefs.getBoolean(uidKey(KEY_SLOT_B_UNLOCKED, uid), prefs.getBoolean(KEY_SLOT_B_UNLOCKED, false)) ||
          getUnlockedExtraSlots(uid) >= 1
      "Frame C" ->
        prefs.getBoolean(uidKey(KEY_SLOT_C_UNLOCKED, uid), prefs.getBoolean(KEY_SLOT_C_UNLOCKED, false)) ||
          getUnlockedExtraSlots(uid) >= 2
      else -> true
    }
  }

  fun setCustomSlotUnlocked(slotId: String, unlocked: Boolean, uid: String? = null) {
    val key =
      when (normalizeCustomSlotId(slotId)) {
        "Frame B" -> KEY_SLOT_B_UNLOCKED
        "Frame C" -> KEY_SLOT_C_UNLOCKED
        else -> return
      }
    prefs
      .edit()
      .putBoolean(key, unlocked)
      .putBoolean(uidKey(key, uid), unlocked)
      .apply()
  }

  fun getCustomShipSlot(slotId: String, uid: String? = null): com.example.engine.CustomShipLoadoutSlot {
    val cleanId = normalizeCustomSlotId(slotId)
    val baseKey = "${KEY_CUSTOM_SLOT_PREFIX}_$cleanId"
    val legacyBaseKey = "${KEY_CUSTOM_SLOT_PREFIX}_${legacySlotKeySuffix(cleanId)}"
    val raw =
      prefs.getString(uidKey(baseKey, uid), prefs.getString(baseKey, null))
        ?: prefs.getString(uidKey(legacyBaseKey, uid), prefs.getString(legacyBaseKey, null))
    return com.example.engine.CustomShipLoadoutSlot.fromJsonString(raw, cleanId)
  }

  fun saveCustomShipSlot(slot: com.example.engine.CustomShipLoadoutSlot, uid: String? = null) {
    val cleanId = normalizeCustomSlotId(slot.slotId)
    val normalizedSlot = slot.copy(slotId = cleanId)
    val baseKey = "${KEY_CUSTOM_SLOT_PREFIX}_$cleanId"
    val json = normalizedSlot.toJsonString()
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

    return customList
  }

  fun addFriendByCallsign(rawCallsign: String): FriendPilotEntry? {
    val clean =
      rawCallsign
        .trim()
        .uppercase()
        .replace(Regex("[^A-Z0-9_\\-]"), "")
        .take(14)
    if (clean.length < 3 || UgcModeration.containsProfanity(clean)) return null
    val existing = getFriendsList()
    if (existing.any { it.callsign.equals(clean, ignoreCase = true) }) {
      return existing.first { it.callsign.equals(clean, ignoreCase = true) }
    }

    val created =
      FriendPilotEntry(
        uid = "friend_custom_${System.currentTimeMillis()}",
        callsign = clean,
        syndicateTag = getSyndicateTag(),
        shipId = ((clean.hashCode().and(0x7FFFFFFF)) % 9) + 1,
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
    val customRaw = prefs.getString(KEY_CUSTOM_SYNDICATES_JSON, "[]") ?: "[]"
    val customList = mutableListOf<SyndicateInfo>()
    try {
      val arr = JSONArray(customRaw)
      for (i in 0 until arr.length()) {
        val obj = arr.getJSONObject(i)
        val parsedTag = obj.optString("tag", "").trim().uppercase()
        if (parsedTag.isNotEmpty()) {
          customList.add(
            SyndicateInfo(
              id = obj.optString("id", "syn_custom_$i"),
              tag = parsedTag,
              name = obj.optString("name", "$parsedTag Syndicate"),
              foundryOrigin = obj.optString("foundryOrigin", "Player Registered Syndicate"),
              motto = obj.optString("motto", "Forged in the Convergence"),
              memberCount = obj.optInt("memberCount", 1),
              combinedBounty = obj.optLong("combinedBounty", 1000L),
              isCustom = true,
            )
          )
        }
      }
    } catch (_: Exception) {}

    return customList
  }

  fun getSyndicatesList(): List<SyndicateInfo> = getAllSyndicates()

  fun mergeCloudSyndicates(cloudSyndicates: List<SyndicateInfo>) {
    if (cloudSyndicates.isEmpty()) return
    val mergedByTag = linkedMapOf<String, SyndicateInfo>()
    for (local in getAllSyndicates()) {
      mergedByTag[local.tag.uppercase()] = local
    }
    for (cloud in cloudSyndicates) {
      val cleanTag = cloud.tag.trim().uppercase()
      if (cleanTag.length in 2..4) {
        mergedByTag[cleanTag] =
          cloud.copy(
            id = cloud.id.ifBlank { "syn_cloud_$cleanTag" },
            tag = cleanTag,
            isCustom = true,
          )
      }
    }
    val arr = JSONArray()
    for (syn in mergedByTag.values) {
      arr.put(
        JSONObject().apply {
          put("id", syn.id)
          put("tag", syn.tag)
          put("name", syn.name)
          put("foundryOrigin", syn.foundryOrigin)
          put("motto", syn.motto)
          put("memberCount", syn.memberCount)
          put("combinedBounty", syn.combinedBounty)
        }
      )
    }
    prefs.edit().putString(KEY_CUSTOM_SYNDICATES_JSON, arr.toString()).apply()
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
    if (UgcModeration.containsProfanity(cleanTag)) {
      return Result.failure(IllegalArgumentException("Syndicate Tag contains prohibited language."))
    }
    val cleanName = name.trim().replace(Regex("[^a-zA-Z0-9 _\\-]"), "").take(26)
    if (cleanName.length < 3) {
      return Result.failure(IllegalArgumentException("Syndicate Name must be at least 3 characters."))
    }
    if (UgcModeration.containsProfanity(cleanName)) {
      return Result.failure(IllegalArgumentException("Syndicate Name contains prohibited language."))
    }

    val existing = getAllSyndicates()
    if (existing.any { it.tag.equals(cleanTag, ignoreCase = true) }) {
      return Result.failure(IllegalArgumentException("Tag [$cleanTag] is already registered."))
    }

    val cleanMotto = UgcModeration.sanitizeUgcText(motto.trim().take(60)).ifEmpty { "Forged in the RIFT Convergence" }

    val newSyndicate =
      SyndicateInfo(
        id = "syn_custom_$now",
        tag = cleanTag,
        name = cleanName,
        foundryOrigin = "Player Founded Syndicate",
        motto = cleanMotto,
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

  fun getPlayfieldTextScale(): Float {
    val fallback = prefs.getFloat(KEY_PLAYFIELD_TEXT_SCALE, 1.0f)
    return userSettingsPrefs.getFloat(KEY_PLAYFIELD_TEXT_SCALE, fallback).coerceIn(0.75f, 2.0f)
  }

  fun setPlayfieldTextScale(scale: Float) {
    val clamped = scale.coerceIn(0.75f, 2.0f)
    userSettingsPrefs.edit().putFloat(KEY_PLAYFIELD_TEXT_SCALE, clamped).apply()
  }

  fun getLastSeenChangelogBuild(): Int = userSettingsPrefs.getInt(KEY_LAST_SEEN_CHANGELOG_BUILD, 0)

  fun setLastSeenChangelogBuild(buildNumber: Int) {
    userSettingsPrefs.edit().putInt(KEY_LAST_SEEN_CHANGELOG_BUILD, buildNumber).apply()
  }

  fun getBlockedPilotUids(): Set<String> {
    val raw = prefs.getString(KEY_BLOCKED_PILOT_UIDS, "") ?: ""
    return raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
  }

  fun blockPilot(uid: String) {
    if (uid.isBlank()) return
    val next = getBlockedPilotUids() + uid.trim()
    prefs.edit().putString(KEY_BLOCKED_PILOT_UIDS, next.joinToString(",")).apply()
  }

  fun unblockPilot(uid: String) {
    val next = getBlockedPilotUids() - uid.trim()
    prefs.edit().putString(KEY_BLOCKED_PILOT_UIDS, next.joinToString(",")).apply()
  }

  fun reportPilot(uid: String, callsign: String, reason: String = "In-Game Comms / Conduct"): Int {
    val raw = prefs.getString(KEY_REPORTED_PILOTS_JSON, "[]") ?: "[]"
    val arr =
      try {
        JSONArray(raw)
      } catch (_: Exception) {
        JSONArray()
      }
    arr.put(
      JSONObject().apply {
        put("uid", uid)
        put("callsign", callsign)
        put("reason", reason)
        put("timestampMs", System.currentTimeMillis())
      }
    )
    prefs.edit().putString(KEY_REPORTED_PILOTS_JSON, arr.toString()).apply()
    return arr.length()
  }

  /**
   * Returns up to 5 private custom channels for [syndicateTag], always starting with "#general" at index 0.
   * Defaults to listOf("#general", "#tactics", "#war-room").
   */
  fun getSyndicateCustomChannels(syndicateTag: String = getSyndicateTag()): List<String> {
    val cleanTag = syndicateTag.trim().uppercase()
    if (cleanTag.isEmpty() || cleanTag == "SOLO") {
      return listOf("#general", "#tactics", "#war-room")
    }
    val raw = prefs.getString("${KEY_SYNDICATE_CHANNELS_PREFIX}$cleanTag", null)
    if (raw.isNullOrBlank()) {
      return listOf("#general", "#tactics", "#war-room")
    }
    val parsed = mutableListOf<String>()
    try {
      val arr = JSONArray(raw)
      for (i in 0 until arr.length()) {
        val ch = normalizeChannelName(arr.optString(i, ""))
        if (ch.isNotEmpty() && parsed.none { it.equals(ch, ignoreCase = true) }) {
          parsed.add(ch)
        }
      }
    } catch (_: Exception) {}
    parsed.removeAll { it.equals("#general", ignoreCase = true) }
    parsed.add(0, "#general")
    return parsed.take(5)
  }

  /**
   * Saves up to 5 clean `#channel-name` strings for [syndicateTag], always keeping `"#general"` at index 0.
   */
  fun setSyndicateCustomChannels(syndicateTag: String, channels: List<String>) {
    val cleanTag = syndicateTag.trim().uppercase()
    if (cleanTag.isEmpty()) return
    val sanitized = mutableListOf("#general")
    for (raw in channels) {
      val norm = normalizeChannelName(raw)
      if (norm.isNotEmpty() && sanitized.none { it.equals(norm, ignoreCase = true) }) {
        sanitized.add(norm)
      }
      if (sanitized.size >= 5) break
    }
    val arr = JSONArray()
    sanitized.forEach { arr.put(it) }
    prefs.edit().putString("${KEY_SYNDICATE_CHANNELS_PREFIX}$cleanTag", arr.toString()).apply()
  }

  private fun normalizeChannelName(raw: String): String {
    val stripped =
      raw
        .trim()
        .lowercase()
        .removePrefix("#")
        .replace(Regex("[^a-z0-9_-]"), "-")
        .replace(Regex("-+"), "-")
        .trim('-')
        .take(18)
    return if (stripped.isEmpty()) "" else "#$stripped"
  }

  fun getSyndicateInvitedGuests(syndicateTag: String = getSyndicateTag()): Set<String> {
    val cleanTag = syndicateTag.trim().uppercase()
    if (cleanTag.isEmpty() || cleanTag == "SOLO") return emptySet()
    val raw = prefs.getString("${KEY_SYNDICATE_GUESTS_PREFIX}$cleanTag", "") ?: ""
    return raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
  }

  fun inviteGuestToSyndicateChannels(syndicateTag: String, guestCallsign: String) {
    val cleanTag = syndicateTag.trim().uppercase()
    val cleanGuest = guestCallsign.trim().take(20)
    if (cleanTag.isEmpty() || cleanTag == "SOLO" || cleanGuest.isEmpty()) return
    val existing = getSyndicateInvitedGuests(cleanTag).toMutableList()
    if (existing.none { it.equals(cleanGuest, ignoreCase = true) }) {
      existing.add(cleanGuest)
      prefs.edit().putString("${KEY_SYNDICATE_GUESTS_PREFIX}$cleanTag", existing.joinToString(",")).apply()
    }
  }

  fun revokeGuestFromSyndicateChannels(syndicateTag: String, guestCallsign: String) {
    val cleanTag = syndicateTag.trim().uppercase()
    val cleanGuest = guestCallsign.trim()
    if (cleanTag.isEmpty() || cleanGuest.isEmpty()) return
    val remaining =
      getSyndicateInvitedGuests(cleanTag).filterNot { it.equals(cleanGuest, ignoreCase = true) }
    prefs.edit().putString("${KEY_SYNDICATE_GUESTS_PREFIX}$cleanTag", remaining.joinToString(",")).apply()
  }

  fun canAccessSyndicateChannels(
    pilotSyndicateTag: String,
    pilotCallsign: String,
    targetSyndicateTag: String,
  ): Boolean {
    val targetClean = targetSyndicateTag.trim().uppercase()
    if (targetClean.isEmpty() || targetClean == "SOLO") return false
    if (pilotSyndicateTag.trim().equals(targetClean, ignoreCase = true)) return true
    return getSyndicateInvitedGuests(targetClean).any {
      it.equals(pilotCallsign.trim(), ignoreCase = true)
    }
  }

  fun getSyndicateWarRecord(tag: String = getSyndicateTag()): SyndicateWarRecord {
    val cleanTag = tag.trim().uppercase().ifEmpty { "SOLO" }
    val raw = prefs.getString(KEY_SYNDICATE_WAR_RECORDS_JSON, "{}") ?: "{}"
    try {
      val root = JSONObject(raw)
      val obj = root.optJSONObject(cleanTag)
      if (obj != null) {
        return SyndicateWarRecord.fromJson(cleanTag, obj)
      }
    } catch (_: Exception) {}
    return when (cleanTag) {
      "AEX" -> SyndicateWarRecord("AEX", warTrophies = 1480, warWins = 42, warLosses = 14, warStreak = 4)
      "VTX" -> SyndicateWarRecord("VTX", warTrophies = 1390, warWins = 38, warLosses = 17, warStreak = 2)
      "KST" -> SyndicateWarRecord("KST", warTrophies = 1315, warWins = 35, warLosses = 19, warStreak = 1)
      "OBS" -> SyndicateWarRecord("OBS", warTrophies = 1260, warWins = 31, warLosses = 20, warStreak = 3)
      "SOL" -> SyndicateWarRecord("SOL", warTrophies = 1195, warWins = 29, warLosses = 22, warStreak = 0)
      "RFT" -> SyndicateWarRecord("RFT", warTrophies = 1140, warWins = 26, warLosses = 21, warStreak = 1)
      else -> SyndicateWarRecord(cleanTag, warTrophies = 1000, warWins = 0, warLosses = 0, warStreak = 0)
    }
  }

  fun recordSyndicateWarOutcome(
    tag: String = getSyndicateTag(),
    won: Boolean,
    trophyDelta: Int = if (won) 25 else -15,
  ): SyndicateWarRecord {
    val cleanTag = tag.trim().uppercase().ifEmpty { "SOLO" }
    val current = getSyndicateWarRecord(cleanTag)
    val signedDelta = if (won) kotlin.math.abs(trophyDelta) else -kotlin.math.abs(trophyDelta)
    val updated =
      SyndicateWarRecord(
        tag = cleanTag,
        warTrophies = (current.warTrophies + signedDelta).coerceAtLeast(0),
        warWins = current.warWins + (if (won) 1 else 0),
        warLosses = current.warLosses + (if (won) 0 else 1),
        warStreak = if (won) current.warStreak + 1 else 0,
      )
    val raw = prefs.getString(KEY_SYNDICATE_WAR_RECORDS_JSON, "{}") ?: "{}"
    val root =
      try {
        JSONObject(raw)
      } catch (_: Exception) {
        JSONObject()
      }
    root.put(cleanTag, updated.toJson())
    prefs.edit().putString(KEY_SYNDICATE_WAR_RECORDS_JSON, root.toString()).apply()
    return updated
  }

  /**
   * Master Fresh-Start Wipe:
   * Clears all combat stats, 5 XP sources, peak bounty snapshots, Armada tiers, active Armada,
   * custom friends, custom Syndicates, Syndicate service history, custom Syndicate channels/guests,
   * Syndicate War H2H records, and custom modular ship slots (Frame A/B/C) while resetting Rift Shards to 150❖.
   */
  fun wipeAllStatsArmadasAndSyndicates(uid: String? = null) {
    val dynamicKeysToRemove =
      prefs.all.keys.filter { key ->
        key.startsWith(KEY_SYNDICATE_CHANNELS_PREFIX) ||
          key.startsWith("syndicate_channels_") ||
          key.startsWith(KEY_SYNDICATE_GUESTS_PREFIX) ||
          key.startsWith("syndicate_guests_") ||
          key.startsWith("syndicate_war_record_") ||
          key.startsWith(KEY_CUSTOM_SLOT_PREFIX)
      }

    val editor = prefs.edit()
    for (k in dynamicKeysToRemove) {
      editor.remove(k)
    }

    // Combat stats
    editor
      .putLong(KEY_PVP_KILLS, 0L)
      .putLong(KEY_PVP_DEATHS, 0L)
      .putLong(KEY_PVP_BOUNTY_PEAK, 100L)
      .putLong(KEY_ALL_KILLS, 0L)
      .putLong(KEY_ALL_DEATHS, 0L)
      .putLong(KEY_ALL_BOUNTY_PEAK, 100L)
      .putLong(KEY_CONVERGENCES_WON, 0L)
      .putLong(KEY_HVT_ELIMINATIONS, 0L)
      .putLong(KEY_GREENS_COLLECTED, 0L)

    // 5 XP sources + progression + Rift Shards (150❖)
    editor
      .putLong(KEY_XP_BUFFS, 0L)
      .putLong(uidKey(KEY_XP_BUFFS, uid), 0L)
      .putLong(KEY_XP_DAMAGE, 0L)
      .putLong(uidKey(KEY_XP_DAMAGE, uid), 0L)
      .putLong(KEY_XP_ACCURACY, 0L)
      .putLong(uidKey(KEY_XP_ACCURACY, uid), 0L)
      .putLong(KEY_XP_BOUNTY_EARNED, 0L)
      .putLong(uidKey(KEY_XP_BOUNTY_EARNED, uid), 0L)
      .putLong(KEY_XP_BOUNTY_KILLED, 0L)
      .putLong(uidKey(KEY_XP_BOUNTY_KILLED, uid), 0L)
      .putLong(KEY_RIFT_SHARDS, 150L)
      .putLong(uidKey(KEY_RIFT_SHARDS, uid), 150L)
      .putInt(KEY_HIGHEST_MILESTONE_LVL, 1)
      .putInt(uidKey(KEY_HIGHEST_MILESTONE_LVL, uid), 1)
      .putInt(KEY_SPENT_CUSTOM_POINTS, 0)
      .putInt(uidKey(KEY_SPENT_CUSTOM_POINTS, uid), 0)
      .putInt(KEY_UNLOCKED_EXTRA_SLOTS, 0)
      .putInt(uidKey(KEY_UNLOCKED_EXTRA_SLOTS, uid), 0)
      .putInt(KEY_AI_DESIGN_TOKENS, 0)
      .putInt(uidKey(KEY_AI_DESIGN_TOKENS, uid), 0)

    // Peak bounty snapshots
    editor
      .remove(KEY_ALL_TIME_PEAK_SNAPSHOT)
      .remove(uidKey(KEY_ALL_TIME_PEAK_SNAPSHOT, uid))
      .remove(KEY_PVP_PEAK_SNAPSHOT)
      .remove(uidKey(KEY_PVP_PEAK_SNAPSHOT, uid))

    // Armada Tier 1..5 kills and bounty peaks
    for (tier in 1..5) {
      editor.putLong("armada_tier_${tier}_kills", 0L)
      editor.putLong("armada_tier_${tier}_bounty", 100L)
    }

    // Active Armada, custom friends, custom Syndicates, Syndicate history, Syndicate War records
    editor
      .putString(KEY_ARMADA_NAME, "")
      .putString(KEY_CUSTOM_FRIENDS_JSON, "[]")
      .putString(KEY_CUSTOM_SYNDICATES_JSON, "[]")
      .putString(KEY_SYNDICATE_HISTORY_JSON, "[]")
      .putString(uidKey(KEY_SYNDICATE_HISTORY_JSON, uid), "[]")
      .putLong(KEY_SYNDICATE_LOCK_UNTIL_MS, 0L)
      .putString(KEY_SYNDICATE_TAG, "GUEST")
      .putString(KEY_SYNDICATE_NAME, "Independent Pilot")
      .putString(KEY_SYNDICATE_WAR_RECORDS_JSON, "{}")
      .putBoolean(KEY_SLOT_B_UNLOCKED, false)
      .putBoolean(uidKey(KEY_SLOT_B_UNLOCKED, uid), false)
      .putBoolean(KEY_SLOT_C_UNLOCKED, false)
      .putBoolean(uidKey(KEY_SLOT_C_UNLOCKED, uid), false)
      .putString(KEY_ACTIVE_CUSTOM_SLOT_ID, "Frame A")
      .putString(uidKey(KEY_ACTIVE_CUSTOM_SLOT_ID, uid), "Frame A")
      .apply()

    // Reset Custom Modular Ship Slots (Frame A, Frame B, Frame C) to default unallocated state
    for (slotId in listOf("Frame A", "Frame B", "Frame C")) {
      saveCustomShipSlot(com.example.engine.CustomShipLoadoutSlot(slotId = slotId), uid)
    }
  }

  /**
   * Resets all local pilot progression, combat stats, Syndicate/Armada affiliation, custom friends,
   * and Rift Shards back to a clean Level 1, 0 XP, 0 Kills, 0 Shards, Solo, Unaffiliated state.
   */
  fun resetAllLocalPilotData() {
    val seenBuild = getLastSeenChangelogBuild()
    val guestBypass = isGuestBypassEnabled()
    prefs.edit().clear().apply()
    userSettingsPrefs.edit().clear().apply()
    setLastSeenChangelogBuild(seenBuild)
    setGuestBypassEnabled(guestBypass)
    getOrCreatePermanentDeviceUid()
  }

  private fun cleanShipNameForId(shipId: Int): String =
    when (shipId) {
      1 -> "Viper"
      2 -> "Ricochet"
      3 -> "Suppressor"
      4 -> "Goliath"
      5 -> "Twinfang"
      6 -> "Disruptor"
      7 -> "Valkyrie"
      8 -> "Manta"
      9 -> "Interdictor"
      else -> "Rift Frame"
    }

  companion object {
    private const val PREFS_NAME = "rift_permanent_pilot_prefs"
    private const val USER_SETTINGS_PREFS_NAME = "rift_user_settings"
    private const val KEY_BLOCKED_PILOT_UIDS = "ugc_blocked_pilot_uids"
    private const val KEY_REPORTED_PILOTS_JSON = "ugc_reported_pilots_json"
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
    private const val KEY_BOOT_TO_NEXUS_HUB = "boot_to_nexus_hub"
    private const val KEY_SYNDICATE_CHANNELS_PREFIX = "syndicate_custom_channels_"
    private const val KEY_SYNDICATE_GUESTS_PREFIX = "syndicate_invited_guests_"
    private const val KEY_SYNDICATE_WAR_RECORDS_JSON = "syndicate_war_records_json"
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
    private const val KEY_LAST_SEEN_CHANGELOG_BUILD = "last_seen_changelog_build"

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
    private const val KEY_SLOT_B_UNLOCKED = "custom_slot_9b_unlocked"
    private const val KEY_SLOT_C_UNLOCKED = "custom_slot_9c_unlocked"
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

