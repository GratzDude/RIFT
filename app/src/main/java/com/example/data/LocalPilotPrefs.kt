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
    if (enabled) {
      editor.putString(KEY_SYNDICATE_TAG, "RFT")
      editor.putString(KEY_SYNDICATE_NAME, "Rift Drifters")
    } else {
      val rawTag = (prefs.getString(KEY_SYNDICATE_TAG, "") ?: "").trim().uppercase(Locale.US)
      if (rawTag in NON_PLAYER_SYNDICATE_TAGS) {
        editor.putString(KEY_SYNDICATE_TAG, "")
        editor.putString(KEY_SYNDICATE_NAME, "Unaffiliated")
      }
    }
    editor.apply()
  }

  fun isSwornPilot(isGoogleAuthenticated: Boolean): Boolean =
    isGoogleAuthenticated && !isGuestBypassEnabled()

  fun generateRandomDrifterCallsign(): String = DRIFTER_NAMES.random()

  fun isDrifterOrEphemeralUid(uid: String? = null): Boolean {
    val clean = uid?.trim().orEmpty()
    if (
      clean.startsWith("drifter_", ignoreCase = true) ||
        clean.startsWith("dev_", ignoreCase = true) ||
        clean.startsWith("guest_", ignoreCase = true)
    ) {
      return true
    }
    return isGuestBypassEnabled() && clean.isEmpty()
  }

  private fun canUseLegacyUnscopedFallback(uid: String?): Boolean {
    if (isDrifterOrEphemeralUid(uid)) return false
    val clean = uid?.trim().orEmpty()
    if (clean.isEmpty()) return !isGuestBypassEnabled()
    val owner = prefs.getString(KEY_PRIMARY_SWORN_UID, null)?.trim().orEmpty()
    if (owner.isEmpty()) {
      prefs.edit().putString(KEY_PRIMARY_SWORN_UID, clean).apply()
      return true
    }
    return owner == clean
  }

  /**
   * Starts a brand-new ephemeral Drifter session on launch or in-app swap:
   * assigns a fresh session UID, locks Drifter tag to [RFT], and picks a random Drifter callsign
   * WITHOUT touching any Sworn Pilot UID-scoped or unscoped progression keys.
   */
  fun startFreshDrifterSession(): String {
    val freshUid = "drifter_" + UUID.randomUUID().toString().replace("-", "").take(12)
    val randomCallsign = generateRandomDrifterCallsign()
    prefs
      .edit()
      .putString(KEY_DEVICE_UID, freshUid)
      .putBoolean(KEY_GUEST_BYPASS, true)
      .putString(KEY_DRIFTER_CALLSIGN, randomCallsign)
      .putString("${KEY_CALLSIGN}_$freshUid", randomCallsign)
      .putString("${KEY_SYNDICATE_TAG}_$freshUid", "RFT")
      .putString("${KEY_SYNDICATE_NAME}_$freshUid", "Rift Drifters")
      .putString("${KEY_ARMADA_NAME}_$freshUid", "")
      .putLong("${KEY_PVP_KILLS}_$freshUid", 0L)
      .putLong("${KEY_PVP_DEATHS}_$freshUid", 0L)
      .putLong("${KEY_PVP_BOUNTY_PEAK}_$freshUid", 100L)
      .putLong("${KEY_ALL_KILLS}_$freshUid", 0L)
      .putLong("${KEY_ALL_DEATHS}_$freshUid", 0L)
      .putLong("${KEY_ALL_BOUNTY_PEAK}_$freshUid", 100L)
      .putLong("${KEY_CONVERGENCES_WON}_$freshUid", 0L)
      .putLong("${KEY_HVT_ELIMINATIONS}_$freshUid", 0L)
      .putLong("${KEY_GREENS_COLLECTED}_$freshUid", 0L)
      .apply()
    return randomCallsign
  }

  fun getShortPilotTag(uid: String): String {
    val clean = uid.trim().ifEmpty { getOrCreatePermanentDeviceUid() }
    val alphabet = "0123456789ABCDEFGHJKLMNPQRSTUVWXYZ"
    var h = clean.hashCode().toLong() and 0xFFFFFFFFL
    if (h == 0L) h = 0x5A3C91L
    val sb = StringBuilder(6)
    for (i in 0 until 6) {
      val idx = ((h xor (i * 31L)) % alphabet.length).toInt().let { if (it < 0) -it else it }
      sb.append(alphabet[idx % alphabet.length])
      h = (h * 1664525L + 1013904223L) and 0xFFFFFFFFL
    }
    return "#RFT-$sb"
  }

  /**
   * Clears unscoped callsign and initial-choice markers when switching accounts or signing out so
   * another Google account on the same tablet never inherits a previous pilot's callsign.
   */
  fun clearUnscopedCallsignOnAccountSwitch() {
    prefs
      .edit()
      .remove(KEY_CALLSIGN)
      .remove(KEY_INITIAL_CALLSIGN_CHOSEN)
      .apply()
  }

  fun getCallsign(uid: String? = null): String {
    if (isGuestBypassEnabled() || isDrifterOrEphemeralUid(uid)) {
      val drifterName =
        (if (!uid.isNullOrBlank()) prefs.getString(uidKey(KEY_CALLSIGN, uid), null) else null)
          ?: prefs.getString(KEY_DRIFTER_CALLSIGN, null)
      if (!drifterName.isNullOrBlank() && drifterName != "COMMANDER") return drifterName
      return generateRandomDrifterCallsign().also {
        val ed = prefs.edit().putString(KEY_DRIFTER_CALLSIGN, it)
        if (!uid.isNullOrBlank()) ed.putString(uidKey(KEY_CALLSIGN, uid), it)
        ed.apply()
      }
    }
    fun isChosenSwornName(s: String?): Boolean {
      if (s.isNullOrBlank()) return false
      val u = s.trim().uppercase(Locale.US)
      return u != "COMMANDER" && !u.startsWith("RFT-") && !u.startsWith("#RFT")
    }
    // Signed-in Sworn Pilot: strictly read uid-scoped callsign only (zero unscoped fallback or copy)
    if (!uid.isNullOrBlank()) {
      val perUid = prefs.getString(uidKey(KEY_CALLSIGN, uid), null)
      return perUid?.takeIf { isChosenSwornName(it) } ?: "COMMANDER"
    }
    val global = prefs.getString(KEY_CALLSIGN, null)
    return global?.takeIf { isChosenSwornName(it) } ?: "COMMANDER"
  }

  fun setCallsign(rawCallsign: String, uid: String? = null): String {
    val sanitizedRaw =
      rawCallsign
        .trim()
        .uppercase(Locale.US)
        .replace(Regex("[^A-Z0-9_\\-]"), "")
        .take(14)
    val cleaned =
      if (sanitizedRaw.isEmpty() || UgcModeration.containsProfanity(sanitizedRaw)) {
        if (isGuestBypassEnabled()) generateRandomDrifterCallsign() else "COMMANDER"
      } else {
        sanitizedRaw
      }
    val editor = prefs.edit()
    if (isGuestBypassEnabled() || isDrifterOrEphemeralUid(uid)) {
      editor.putString(KEY_DRIFTER_CALLSIGN, cleaned)
      if (!uid.isNullOrBlank()) {
        editor.putString(uidKey(KEY_CALLSIGN, uid), cleaned)
      }
    } else if (!uid.isNullOrBlank()) {
      editor.putString(uidKey(KEY_CALLSIGN, uid), cleaned)
      editor.remove(KEY_CALLSIGN)
    } else {
      editor.putString(KEY_CALLSIGN, cleaned)
    }
    editor.apply()
    return cleaned
  }

  fun getCallsignChangeCount(uid: String? = null): Int =
    if (!uid.isNullOrBlank()) {
      prefs.getInt(uidKey(KEY_CALLSIGN_CHANGE_COUNT, uid), 0)
    } else {
      prefs.getInt(KEY_CALLSIGN_CHANGE_COUNT, 0)
    }

  fun incrementCallsignChangeCount(uid: String? = null) {
    val next = getCallsignChangeCount(uid) + 1
    val ed = prefs.edit()
    if (!uid.isNullOrBlank()) {
      ed.putInt(uidKey(KEY_CALLSIGN_CHANGE_COUNT, uid), next)
    } else {
      ed.putInt(KEY_CALLSIGN_CHANGE_COUNT, next)
    }
    ed.apply()
  }

  fun hasChosenInitialCallsign(uid: String? = null): Boolean {
    if (isGuestBypassEnabled() || isDrifterOrEphemeralUid(uid)) return false
    val explicitChosen =
      if (!uid.isNullOrBlank()) {
        prefs.getBoolean(uidKey(KEY_INITIAL_CALLSIGN_CHOSEN, uid), false)
      } else {
        prefs.getBoolean(KEY_INITIAL_CALLSIGN_CHOSEN, false)
      }
    if (explicitChosen) return true
    val existing = getCallsign(uid)
    val upper = existing.trim().uppercase(Locale.US)
    if (upper.isNotBlank() && upper != "COMMANDER" && !upper.startsWith("RFT-") && !upper.startsWith("#RFT")) {
      setHasChosenInitialCallsign(true, uid)
      return true
    }
    return false
  }

  fun setHasChosenInitialCallsign(chosen: Boolean, uid: String? = null) {
    val editor = prefs.edit()
    if (!uid.isNullOrBlank()) {
      editor.putBoolean(uidKey(KEY_INITIAL_CALLSIGN_CHOSEN, uid), chosen)
      editor.remove(KEY_INITIAL_CALLSIGN_CHOSEN)
    } else {
      editor.putBoolean(KEY_INITIAL_CALLSIGN_CHOSEN, chosen)
    }
    editor.apply()
  }

  /**
   * Changes a Sworn Pilot's callsign:
   * - 1st change (`getCallsignChangeCount(uid) == 0`) is FREE (0 Rift Shards).
   * - Subsequent changes cost 250 Rift Shards.
   * Returns Triple(success, resultCallsignOrErrorMessage, updatedShards).
   */
  fun changeSwornPilotCallsign(
    newCallsign: String,
    currentShards: Long,
    uid: String? = null,
  ): Triple<Boolean, String, Long> {
    if (isGuestBypassEnabled()) {
      return Triple(false, "Drifters use temporary [RFT] callsigns. Sign in to claim a permanent name.", currentShards)
    }
    val sanitized =
      newCallsign
        .trim()
        .uppercase(Locale.US)
        .replace(Regex("[^A-Z0-9_\\-]"), "")
        .take(14)
    if (sanitized.length < 3) {
      return Triple(false, "Callsign must be 3 to 14 characters.", currentShards)
    }
    if (UgcModeration.containsProfanity(sanitized)) {
      return Triple(false, "Callsign contains blocked words.", currentShards)
    }
    val currentName = getCallsign(uid)
    if (sanitized.equals(currentName, ignoreCase = true) && hasChosenInitialCallsign(uid)) {
      return Triple(true, sanitized, currentShards)
    }
    val changeCount = getCallsignChangeCount(uid)
    val cost = if (changeCount == 0) 0L else 250L
    if (currentShards < cost) {
      return Triple(false, "Need 250 Rift Shards to change callsign.", currentShards)
    }
    val nextShards = (currentShards - cost).coerceAtLeast(0L)
    if (cost > 0L) {
      setRiftShards(nextShards, uid)
    }
    val saved = setCallsign(sanitized, uid)
    incrementCallsignChangeCount(uid)
    setHasChosenInitialCallsign(true, uid)
    return Triple(true, saved, nextShards)
  }

  fun sanitizeSyndicateTag(raw: String): String {
    val cleanedFull =
      raw
        .trim()
        .uppercase(Locale.US)
        .replace(Regex("[^A-Z0-9_\\-]"), "")
    if (cleanedFull.isEmpty() || cleanedFull in NON_PLAYER_SYNDICATE_TAGS) {
      return ""
    }
    val trimmed = cleanedFull.take(3)
    return if (trimmed in NON_PLAYER_SYNDICATE_TAGS) "" else trimmed
  }

  fun isGhostSyndicateName(rawName: String): Boolean {
    val clean = rawName.trim()
    if (clean.isEmpty()) return true
    return clean.equals("Rift Drifters", ignoreCase = true) ||
      clean.equals("Guest Tracking Framework", ignoreCase = true) ||
      clean.equals("GUE Syndicate", ignoreCase = true) ||
      clean.equals("GUES Syndicate", ignoreCase = true) ||
      clean.equals("NOV Syndicate", ignoreCase = true) ||
      clean.equals("NOVA VANGUARD", ignoreCase = true) ||
      clean.contains("Kestrel", ignoreCase = true) ||
      clean.contains("Aurelia", ignoreCase = true) ||
      clean.contains("Vortex", ignoreCase = true) ||
      clean.contains("Rustweld", ignoreCase = true)
  }

  fun isHumanFoundedSyndicateRecord(syn: SyndicateInfo): Boolean {
    if (!syn.isCustom) return false
    if (!syn.isRealPlayerFoundedSyndicate()) return false
    val cleanTag = sanitizeSyndicateTag(syn.tag)
    if (cleanTag.length !in 1..3 || cleanTag in NON_PLAYER_SYNDICATE_TAGS) return false
    val cleanOwner = syn.ownerId.trim()
    if (
      cleanOwner.isEmpty() ||
        cleanOwner.startsWith("dev_", ignoreCase = true) ||
        cleanOwner.startsWith("drifter_", ignoreCase = true) ||
        cleanOwner.startsWith("guest_", ignoreCase = true) ||
        cleanOwner.startsWith("bot_", ignoreCase = true)
    ) {
      return false
    }
    if (isGhostSyndicateName(syn.name)) return false
    return true
  }

  fun getSyndicateTag(uid: String? = null): String {
    if (isGuestBypassEnabled() || isDrifterOrEphemeralUid(uid)) return "RFT"
    val fallbackTag = if (canUseLegacyUnscopedFallback(uid)) (prefs.getString(KEY_SYNDICATE_TAG, "") ?: "") else ""
    val fallbackName = if (canUseLegacyUnscopedFallback(uid)) (prefs.getString(KEY_SYNDICATE_NAME, "") ?: "") else ""
    val raw = (prefs.getString(uidKey(KEY_SYNDICATE_TAG, uid), fallbackTag) ?: "").trim().uppercase(Locale.US)
    val rawName = (prefs.getString(uidKey(KEY_SYNDICATE_NAME, uid), fallbackName) ?: "").trim()
    val clean = sanitizeSyndicateTag(raw)
    if (clean.isEmpty() || raw in NON_PLAYER_SYNDICATE_TAGS || isGhostSyndicateName(rawName)) {
      if (raw.isNotEmpty() || (rawName.isNotEmpty() && !rawName.equals("Unaffiliated", ignoreCase = true))) {
        val ed =
          prefs
            .edit()
            .putString(uidKey(KEY_SYNDICATE_TAG, uid), "")
            .putString(uidKey(KEY_SYNDICATE_NAME, uid), "Unaffiliated")
        if (canUseLegacyUnscopedFallback(uid)) {
          ed.putString(KEY_SYNDICATE_TAG, "").putString(KEY_SYNDICATE_NAME, "Unaffiliated")
        }
        ed.apply()
      }
      return ""
    }
    return clean
  }

  fun getSyndicateName(uid: String? = null): String {
    if (isGuestBypassEnabled() || isDrifterOrEphemeralUid(uid)) return "Rift Drifters"
    val tag = getSyndicateTag(uid)
    if (tag.isBlank()) return "Unaffiliated"
    val fallbackName =
      if (canUseLegacyUnscopedFallback(uid)) (prefs.getString(KEY_SYNDICATE_NAME, "Unaffiliated") ?: "Unaffiliated")
      else "Unaffiliated"
    val raw = (prefs.getString(uidKey(KEY_SYNDICATE_NAME, uid), fallbackName) ?: "Unaffiliated").trim()
    return if (isGhostSyndicateName(raw)) {
      val ed =
        prefs
          .edit()
          .putString(uidKey(KEY_SYNDICATE_TAG, uid), "")
          .putString(uidKey(KEY_SYNDICATE_NAME, uid), "Unaffiliated")
      if (canUseLegacyUnscopedFallback(uid)) {
        ed.putString(KEY_SYNDICATE_TAG, "").putString(KEY_SYNDICATE_NAME, "Unaffiliated")
      }
      ed.apply()
      "Unaffiliated"
    } else {
      raw
    }
  }

  fun getSyndicateLockUntilMs(uid: String? = null): Long {
    val fallback = if (canUseLegacyUnscopedFallback(uid)) prefs.getLong(KEY_SYNDICATE_LOCK_UNTIL_MS, 0L) else 0L
    return prefs.getLong(uidKey(KEY_SYNDICATE_LOCK_UNTIL_MS, uid), fallback)
  }

  fun getRemainingSyndicateLockDays(nowMs: Long = System.currentTimeMillis(), uid: String? = null): Int {
    val lockUntil = getSyndicateLockUntilMs(uid)
    if (lockUntil <= nowMs) return 0
    val diffMs = lockUntil - nowMs
    return ceil(diffMs.toDouble() / (24.0 * 3600.0 * 1000.0)).toInt()
  }

  fun getRemainingSyndicateLockFormatted(nowMs: Long = System.currentTimeMillis(), uid: String? = null): String {
    val lockUntil = getSyndicateLockUntilMs(uid)
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
    uid: String? = null,
  ): Boolean {
    if (isGuestBypassEnabled() || isDrifterOrEphemeralUid(uid)) return false
    val cleanTag = sanitizeSyndicateTag(tag)
    if (cleanTag.isEmpty() || isGhostSyndicateName(name)) {
      return false
    }
    if (apply15DayLock && getRemainingSyndicateLockDays(uid = uid) > 0) {
      return false
    }
    val now = System.currentTimeMillis()
    val cleanName = name.trim().take(28).ifEmpty { "$cleanTag Syndicate" }
    val lockUntil =
      if (apply15DayLock) {
        now + FIFTEEN_DAYS_MS
      } else {
        getSyndicateLockUntilMs(uid)
      }
    val ed =
      prefs
        .edit()
        .putString(uidKey(KEY_SYNDICATE_TAG, uid), cleanTag)
        .putString(uidKey(KEY_SYNDICATE_NAME, uid), cleanName)
        .putLong(uidKey(KEY_SYNDICATE_LOCK_UNTIL_MS, uid), lockUntil)
    if (canUseLegacyUnscopedFallback(uid)) {
      ed
        .putString(KEY_SYNDICATE_TAG, cleanTag)
        .putString(KEY_SYNDICATE_NAME, cleanName)
        .putLong(KEY_SYNDICATE_LOCK_UNTIL_MS, lockUntil)
    }
    ed.apply()
    appendSyndicateServiceTransition(
      newTag = cleanTag,
      newName = cleanName,
      newRole = role,
      nowMs = now,
      uid = uid,
    )
    return true
  }

  fun leaveSyndicate(uid: String? = null): Boolean {
    if (getRemainingSyndicateLockDays(uid = uid) > 0) {
      return false
    }
    val now = System.currentTimeMillis()
    val lockUntil = now + FIFTEEN_DAYS_MS
    val ed =
      prefs
        .edit()
        .putString(uidKey(KEY_SYNDICATE_TAG, uid), "")
        .putString(uidKey(KEY_SYNDICATE_NAME, uid), "Unaffiliated")
        .putLong(uidKey(KEY_SYNDICATE_LOCK_UNTIL_MS, uid), lockUntil)
    if (canUseLegacyUnscopedFallback(uid)) {
      ed
        .putString(KEY_SYNDICATE_TAG, "")
        .putString(KEY_SYNDICATE_NAME, "Unaffiliated")
        .putLong(KEY_SYNDICATE_LOCK_UNTIL_MS, lockUntil)
    }
    ed.apply()
    appendSyndicateServiceTransition(
      newTag = "UNAFFILIATED",
      newName = "Unaffiliated",
      newRole = "Pilot",
      nowMs = now,
      uid = uid,
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

  // Active Named Squadron / Armada (SOLO = "", or up-to-14-char Squadron tag e.g. "ASTRAL_FLEET")
  fun getActiveArmadaName(): String = prefs.getString(KEY_ARMADA_NAME, "") ?: ""

  fun setActiveArmadaName(armadaName: String) {
    val rawClean = armadaName.trim().uppercase(Locale.US).replace(Regex("[^A-Z0-9_\\-]"), "").take(14)
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
    if (isGuestBypassEnabled()) return emptyList()
    val tag = getSyndicateTag()
    if (tag.uppercase(Locale.US) in NON_PLAYER_SYNDICATE_TAGS) {
      return emptyList()
    }
    val officers = getSyndicateOfficers(tag)
    val isFounder =
      getAllSyndicates().any {
        it.tag.equals(tag, ignoreCase = true) &&
          (it.ownerId.isBlank() || it.ownerId == currentUid)
      } || getSyndicateServiceHistory().lastOrNull { it.isActive }?.role?.contains("Founder", ignoreCase = true) == true
    val localRole =
      when {
        isFounder -> "Leader"
        officers.any { it.equals(callsign, ignoreCase = true) } -> "Officer"
        else -> "Pilot"
      }
    val localEntry =
      SyndicateMemberEntry(
        uid = currentUid,
        callsign = callsign,
        tag = tag,
        shipId = shipId,
        shipName = cleanShipNameForId(shipId),
        role = localRole,
        bountyContribution = bounty.coerceAtLeast(getAllTimeBountyPeak()),
        kills = kills,
        isOnline = true,
        isLocalPlayer = true,
      )
    val ownerUid =
      getAllSyndicates().firstOrNull { it.tag.equals(tag, ignoreCase = true) }?.ownerId.orEmpty()
    val kickedMembers = getKickedSyndicateMembers(tag)
    val realSyndicateFriends =
      getFriendsList()
        .filter {
          it.syndicateTag.equals(tag, ignoreCase = true) &&
            !it.callsign.equals(callsign, ignoreCase = true) &&
            kickedMembers.none { kicked -> kicked.equals(it.callsign, ignoreCase = true) }
        }
        .map { friend ->
          val friendRole =
            when {
              ownerUid.isNotBlank() && friend.uid == ownerUid -> "Leader"
              officers.any { it.equals(friend.callsign, ignoreCase = true) } -> "Officer"
              else -> "Pilot"
            }
          SyndicateMemberEntry(
            uid = friend.uid,
            callsign = friend.callsign,
            tag = tag,
            shipId = friend.shipId,
            shipName = cleanShipNameForId(friend.shipId),
            role = friendRole,
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

  fun getPreferredShipId(uid: String? = null): Int {
    val fallback = if (canUseLegacyUnscopedFallback(uid)) prefs.getInt(KEY_SHIP_ID, 1) else 1
    return prefs.getInt(uidKey(KEY_SHIP_ID, uid), fallback).coerceIn(1, 10)
  }

  fun setPreferredShipId(shipId: Int, uid: String? = null) {
    val clamped = shipId.coerceIn(1, 10)
    val ed = prefs.edit().putInt(uidKey(KEY_SHIP_ID, uid), clamped)
    if (canUseLegacyUnscopedFallback(uid)) {
      ed.putInt(KEY_SHIP_ID, clamped)
    }
    ed.apply()
  }

  fun getLastShipId(uid: String? = null): Int = getPreferredShipId(uid)

  fun setLastShipId(shipId: Int, uid: String? = null) = setPreferredShipId(shipId, uid)

  /**
   * Always-NEXUS Boot Law:
   * Cold boot and post-auth entry always open straight into unjoined NEXUS CORE.
   */
  fun getBootToNexusHub(): Boolean = true

  fun setBootToNexusHub(@Suppress("UNUSED_PARAMETER") enabled: Boolean) {
    prefs.edit().putBoolean(KEY_BOOT_TO_NEXUS_HUB, true).apply()
    userSettingsPrefs.edit().putBoolean(KEY_BOOT_TO_NEXUS_HUB, true).apply()
  }

  fun getSfxEnabled(): Boolean = userSettingsPrefs.getBoolean(KEY_SFX_ENABLED, true)

  fun setSfxEnabled(enabled: Boolean) {
    userSettingsPrefs.edit().putBoolean(KEY_SFX_ENABLED, enabled).apply()
  }

  fun getMusicEnabled(): Boolean {
    val fallback = prefs.getBoolean(KEY_MUSIC_ENABLED, true)
    return userSettingsPrefs.getBoolean(KEY_MUSIC_ENABLED, fallback)
  }

  fun setMusicEnabled(enabled: Boolean) {
    prefs.edit().putBoolean(KEY_MUSIC_ENABLED, enabled).apply()
    userSettingsPrefs.edit().putBoolean(KEY_MUSIC_ENABLED, enabled).apply()
  }

  fun getMusicBpm(): Int =
    userSettingsPrefs.getInt(KEY_MUSIC_BPM, 118).coerceIn(60, 180)

  fun setMusicBpm(bpm: Int) {
    userSettingsPrefs.edit().putInt(KEY_MUSIC_BPM, bpm.coerceIn(60, 180)).apply()
  }

  fun getMusicVolume(): Float =
    userSettingsPrefs.getFloat(KEY_MUSIC_VOLUME, 0.70f).coerceIn(0f, 1f)

  fun setMusicVolume(vol: Float) {
    userSettingsPrefs.edit().putFloat(KEY_MUSIC_VOLUME, vol.coerceIn(0f, 1f)).apply()
  }

  fun getSfxVolume(): Float =
    userSettingsPrefs.getFloat(KEY_SFX_VOLUME, 0.85f).coerceIn(0f, 1f)

  fun setSfxVolume(vol: Float) {
    userSettingsPrefs.edit().putFloat(KEY_SFX_VOLUME, vol.coerceIn(0f, 1f)).apply()
  }

  fun getMusicArchetypeId(): String =
    (userSettingsPrefs.getString(KEY_MUSIC_ARCHETYPE_ID, "DARK_SYNTHWAVE") ?: "DARK_SYNTHWAVE")
      .trim()
      .ifEmpty { "DARK_SYNTHWAVE" }

  fun setMusicArchetypeId(id: String) {
    val clean = id.trim().uppercase(Locale.US).ifEmpty { "DARK_SYNTHWAVE" }
    userSettingsPrefs.edit().putString(KEY_MUSIC_ARCHETYPE_ID, clean).apply()
  }

  fun getMusicForgePrompt(): String =
    userSettingsPrefs.getString(KEY_MUSIC_FORGE_PROMPT, "") ?: ""

  fun setMusicForgePrompt(prompt: String) {
    userSettingsPrefs.edit().putString(KEY_MUSIC_FORGE_PROMPT, prompt.trim().take(180)).apply()
  }

  /**
   * Best Ship Calculator:
   * Returns the ship ID (1..10) with the highest earned per-ship Resonance,
   * tie-breaking with PvP/All-Time Peak Snapshots and fallbackShipId (1..10).
   */
  fun getBestShipId(uid: String? = null, fallbackShipId: Int = getLastShipId()): Int {
    ensureShipResonancesMigrated(uid)
    var topShipId = -1
    var topShipRes = 0
    for (id in 1..10) {
      val res = getShipResonanceRaw(id, uid)
      if (res > topShipRes) {
        topShipRes = res
        topShipId = id
      }
    }
    if (topShipId in 1..10 && topShipRes > 0) {
      return topShipId
    }
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

  fun getPvpKills(uid: String? = null): Long =
    prefs.getLong(uidKey(KEY_PVP_KILLS, uid), if (canUseLegacyUnscopedFallback(uid)) prefs.getLong(KEY_PVP_KILLS, 0L) else 0L)

  fun getPvpDeaths(uid: String? = null): Long =
    prefs.getLong(uidKey(KEY_PVP_DEATHS, uid), if (canUseLegacyUnscopedFallback(uid)) prefs.getLong(KEY_PVP_DEATHS, 0L) else 0L)

  fun getPvpBountyPeak(uid: String? = null): Long =
    prefs.getLong(uidKey(KEY_PVP_BOUNTY_PEAK, uid), if (canUseLegacyUnscopedFallback(uid)) prefs.getLong(KEY_PVP_BOUNTY_PEAK, 100L) else 100L)

  fun getAllTimeKills(uid: String? = null): Long =
    prefs.getLong(uidKey(KEY_ALL_KILLS, uid), if (canUseLegacyUnscopedFallback(uid)) prefs.getLong(KEY_ALL_KILLS, 0L) else 0L)

  fun getAllTimeDeaths(uid: String? = null): Long =
    prefs.getLong(uidKey(KEY_ALL_DEATHS, uid), if (canUseLegacyUnscopedFallback(uid)) prefs.getLong(KEY_ALL_DEATHS, 0L) else 0L)

  fun getAllTimeBountyPeak(uid: String? = null): Long =
    prefs.getLong(uidKey(KEY_ALL_BOUNTY_PEAK, uid), if (canUseLegacyUnscopedFallback(uid)) prefs.getLong(KEY_ALL_BOUNTY_PEAK, 100L) else 100L)

  fun getConvergencesWon(uid: String? = null): Long =
    prefs.getLong(uidKey(KEY_CONVERGENCES_WON, uid), if (canUseLegacyUnscopedFallback(uid)) prefs.getLong(KEY_CONVERGENCES_WON, 0L) else 0L)

  fun getPublicGameWins(uid: String? = null): Long = getConvergencesWon(uid)

  fun getHvtEliminations(uid: String? = null): Long =
    prefs.getLong(uidKey(KEY_HVT_ELIMINATIONS, uid), if (canUseLegacyUnscopedFallback(uid)) prefs.getLong(KEY_HVT_ELIMINATIONS, 0L) else 0L)

  fun getGreensCollected(uid: String? = null): Long =
    prefs.getLong(uidKey(KEY_GREENS_COLLECTED, uid), if (canUseLegacyUnscopedFallback(uid)) prefs.getLong(KEY_GREENS_COLLECTED, 0L) else 0L)

  // 5 XP Earning Sources & Pilot Level Progression (Levels 1..100) persisted per UID
  private fun uidKey(baseKey: String, uid: String? = null): String {
    val activeUid = uid?.takeIf { it.isNotBlank() } ?: getOrCreatePermanentDeviceUid()
    return "${baseKey}_$activeUid"
  }

  fun getXpFromBuffs(uid: String? = null): Long =
    prefs.getLong(uidKey(KEY_XP_BUFFS, uid), if (canUseLegacyUnscopedFallback(uid)) prefs.getLong(KEY_XP_BUFFS, 0L) else 0L)

  fun getXpFromDamage(uid: String? = null): Long =
    prefs.getLong(uidKey(KEY_XP_DAMAGE, uid), if (canUseLegacyUnscopedFallback(uid)) prefs.getLong(KEY_XP_DAMAGE, 0L) else 0L)

  fun getXpFromAccuracy(uid: String? = null): Long =
    prefs.getLong(uidKey(KEY_XP_ACCURACY, uid), if (canUseLegacyUnscopedFallback(uid)) prefs.getLong(KEY_XP_ACCURACY, 0L) else 0L)

  fun getXpFromBountyEarned(uid: String? = null): Long =
    prefs.getLong(uidKey(KEY_XP_BOUNTY_EARNED, uid), if (canUseLegacyUnscopedFallback(uid)) prefs.getLong(KEY_XP_BOUNTY_EARNED, 0L) else 0L)

  fun getXpFromBountyKilled(uid: String? = null): Long =
    prefs.getLong(uidKey(KEY_XP_BOUNTY_KILLED, uid), if (canUseLegacyUnscopedFallback(uid)) prefs.getLong(KEY_XP_BOUNTY_KILLED, 0L) else 0L)

  fun getCombatXp(uid: String? = null): Long = getXpFromDamage(uid)

  fun getSurvivalXp(uid: String? = null): Long = getXpFromBountyEarned(uid)

  fun getObjectiveXp(uid: String? = null): Long = getXpFromBountyKilled(uid)

  fun getSupportCratesXp(uid: String? = null): Long = getXpFromBuffs(uid)

  fun getDailyDirectivesXp(uid: String? = null): Long = getXpFromAccuracy(uid)

  fun awardDailyDirectivesXp(amount: Long, uid: String? = null) {
    if (amount <= 0L) return
    val next = getXpFromAccuracy(uid) + amount
    val ed = prefs.edit().putLong(uidKey(KEY_XP_ACCURACY, uid), next)
    if (canUseLegacyUnscopedFallback(uid)) {
      ed.putLong(KEY_XP_ACCURACY, next)
    }
    ed.apply()
  }

  fun getMatchesPlayed(uid: String? = null): Long {
    val raw = prefs.getLong(uidKey(KEY_MATCHES_PLAYED, uid), 0L).coerceAtLeast(0L)
    val wins = getConvergencesWon(uid)
    val losses = prefs.getLong(uidKey(KEY_MATCHES_LOST, uid), 0L).coerceAtLeast(0L)
    return kotlin.math.max(raw, wins + losses)
  }

  fun getMatchesLost(uid: String? = null): Long =
    prefs.getLong(uidKey(KEY_MATCHES_LOST, uid), 0L).coerceAtLeast(0L)

  fun getCareerAssists(uid: String? = null): Long =
    prefs.getLong(uidKey(KEY_CAREER_ASSISTS, uid), 0L).coerceAtLeast(0L)

  fun getCareerShotsFired(uid: String? = null): Long {
    val raw = prefs.getLong(uidKey(KEY_CAREER_SHOTS_FIRED, uid), 0L).coerceAtLeast(0L)
    val snapFired = (getAllTimePeakBountySnapshot(uid)?.shotsFired ?: 0).toLong().coerceAtLeast(0L)
    return kotlin.math.max(raw, snapFired)
  }

  fun getCareerShotsHit(uid: String? = null): Long {
    val raw = prefs.getLong(uidKey(KEY_CAREER_SHOTS_HIT, uid), 0L).coerceAtLeast(0L)
    val snapHit = (getAllTimePeakBountySnapshot(uid)?.shotsHit ?: 0).toLong().coerceAtLeast(0L)
    return kotlin.math.max(raw, snapHit).coerceAtMost(getCareerShotsFired(uid))
  }

  fun getRiftCaptures(uid: String? = null): Long =
    prefs.getLong(uidKey(KEY_RIFT_CAPTURES, uid), 0L).coerceAtLeast(0L)

  fun recordExtraCareerCounters(
    addMatchesPlayed: Long = 0L,
    addMatchesLost: Long = 0L,
    addAssists: Long = 0L,
    addShotsFired: Long = 0L,
    addShotsHit: Long = 0L,
    addRiftCaptures: Long = 0L,
    uid: String? = null,
  ) {
    val nextPlayed = (getMatchesPlayed(uid) + addMatchesPlayed.coerceAtLeast(0L)).coerceAtLeast(0L)
    val nextLost = (getMatchesLost(uid) + addMatchesLost.coerceAtLeast(0L)).coerceAtLeast(0L)
    val nextAssists = (getCareerAssists(uid) + addAssists.coerceAtLeast(0L)).coerceAtLeast(0L)
    val nextFired = (getCareerShotsFired(uid) + addShotsFired.coerceAtLeast(0L)).coerceAtLeast(0L)
    val nextHit = (getCareerShotsHit(uid) + addShotsHit.coerceAtLeast(0L)).coerceIn(0L, nextFired)
    val nextCaps = (getRiftCaptures(uid) + addRiftCaptures.coerceAtLeast(0L)).coerceAtLeast(0L)
    prefs
      .edit()
      .putLong(uidKey(KEY_MATCHES_PLAYED, uid), kotlin.math.max(nextPlayed, getConvergencesWon(uid) + nextLost))
      .putLong(uidKey(KEY_MATCHES_LOST, uid), nextLost)
      .putLong(uidKey(KEY_CAREER_ASSISTS, uid), nextAssists)
      .putLong(uidKey(KEY_CAREER_SHOTS_FIRED, uid), nextFired)
      .putLong(uidKey(KEY_CAREER_SHOTS_HIT, uid), nextHit)
      .putLong(uidKey(KEY_RIFT_CAPTURES, uid), nextCaps)
      .apply()
  }

  fun getPilotCareerStatsSummary(uid: String? = null): PilotCareerStatsSummary {
    val totalXp = getTotalXp(uid)
    val level = computeLevelFromTotalXp(totalXp)
    val (intoLvl, spanLvl, fracLvl) = getXpProgressInLevel(totalXp)
    val wins = getConvergencesWon(uid)
    val losses = getMatchesLost(uid)
    val played = kotlin.math.max(getMatchesPlayed(uid), wins + losses)
    val winPct = if (played > 0L) ((wins.toDouble() / played.toDouble()) * 100.0).toInt().coerceIn(0, 100) else 0
    val rawFired = getCareerShotsFired(uid)
    val rawHit = getCareerShotsHit(uid)
    val elims = getAllTimeKills(uid)
    val deaths = getAllTimeDeaths(uid)
    val hasAnyCareerActivity = played > 0L || elims > 0L || deaths > 0L || totalXp > 0L
    val fired = if (rawFired <= 0L || !hasAnyCareerActivity) 0L else rawFired
    val hit = if (fired <= 0L) 0L else rawHit.coerceIn(0L, fired)
    val accPct = if (fired > 0L) ((hit.toDouble() / fired.toDouble()) * 100.0).toInt().coerceIn(0, 100) else 0
    return PilotCareerStatsSummary(
      pilotLevel = level,
      totalXp = totalXp,
      xpIntoLevel = intoLvl,
      xpSpanForLevel = spanLvl,
      levelProgressFraction = fracLvl,
      combatXp = getCombatXp(uid),
      survivalXp = getSurvivalXp(uid),
      objectiveXp = getObjectiveXp(uid),
      supportCratesXp = getSupportCratesXp(uid),
      dailyDirectivesXp = getDailyDirectivesXp(uid),
      matchesPlayed = played,
      wins = wins,
      losses = losses,
      winPercent = winPct,
      eliminations = getAllTimeKills(uid),
      deaths = getAllTimeDeaths(uid),
      assists = getCareerAssists(uid),
      shotsFired = fired,
      shotsHit = hit,
      accuracyPercent = accPct,
      cratePickups = getGreensCollected(uid),
      riftCaptures = getRiftCaptures(uid),
    )
  }

  fun getTotalXp(uid: String? = null): Long =
    getXpFromBuffs(uid) +
      getXpFromDamage(uid) +
      getXpFromAccuracy(uid) +
      getXpFromBountyEarned(uid) +
      getXpFromBountyKilled(uid)

  fun getPilotLevel(uid: String? = null): Int =
    computeLevelFromTotalXp(getTotalXp(uid))

  @Suppress("UNUSED_PARAMETER")
  fun getCustomizationPointsTotal(uid: String? = null): Int = 100

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

  // Custom Modular Ship Loadout Slots (Frame A through Frame E) with seamless legacy migration
  fun normalizeCustomSlotId(rawSlotId: String?): String =
    when (rawSlotId?.trim()?.uppercase(Locale.US)) {
      "FRAME B", "9B", "B" -> "Frame B"
      "FRAME C", "9C", "C" -> "Frame C"
      "FRAME D", "9D", "D" -> "Frame D"
      "FRAME E", "9E", "E" -> "Frame E"
      else -> "Frame A"
    }

  private fun legacySlotKeySuffix(cleanSlotId: String): String =
    when (cleanSlotId) {
      "Frame B" -> "9B"
      "Frame C" -> "9C"
      "Frame D" -> "9D"
      "Frame E" -> "9E"
      else -> "9A"
    }

  private fun isPersistedUnlockFlag(baseKey: String, uid: String?): Boolean {
    val fallback = if (canUseLegacyUnscopedFallback(uid)) prefs.getBoolean(baseKey, false) else false
    return if (!uid.isNullOrBlank()) {
      prefs.getBoolean(uidKey(baseKey, uid), fallback)
    } else {
      fallback
    }
  }

  fun getActiveCustomSlotId(uid: String? = null): String {
    val fallback =
      if (canUseLegacyUnscopedFallback(uid)) prefs.getString(KEY_ACTIVE_CUSTOM_SLOT_ID, "Frame A")
      else "Frame A"
    val raw =
      (if (!uid.isNullOrBlank()) prefs.getString(uidKey(KEY_ACTIVE_CUSTOM_SLOT_ID, uid), fallback) else fallback)
        ?: "Frame A"
    val clean = normalizeCustomSlotId(raw)
    return if (isCustomSlotUnlocked(clean, uid)) clean else "Frame A"
  }

  fun setActiveCustomSlotId(slotId: String, uid: String? = null) {
    val requested = normalizeCustomSlotId(slotId)
    val clean = if (isCustomSlotUnlocked(requested, uid)) requested else "Frame A"
    val ed = prefs.edit().putString(uidKey(KEY_ACTIVE_CUSTOM_SLOT_ID, uid), clean)
    if (canUseLegacyUnscopedFallback(uid)) {
      ed.putString(KEY_ACTIVE_CUSTOM_SLOT_ID, clean)
    }
    ed.apply()
  }

  fun isCustomSlotUnlocked(slotId: String, uid: String? = null): Boolean {
    val norm = normalizeCustomSlotId(slotId)
    if (norm == "Frame A") return true
    val gate = com.example.engine.RiftProgressionCatalog.getFrameSlotGate(norm)
    return when (norm) {
      "Frame B" ->
        isPersistedUnlockFlag(KEY_SLOT_B_UNLOCKED, uid) ||
          isPersistedUnlockFlag("cosmetic_unlocked_FRAME_SLOT_PURCHASE_2", uid)
      "Frame C",
      "Frame D",
      "Frame E" -> getLiveResonance(uid) >= gate.freeAtResonance
      else -> false
    }
  }

  fun setCustomSlotUnlocked(slotId: String, unlocked: Boolean, uid: String? = null) {
    val norm = normalizeCustomSlotId(slotId)
    if (norm != "Frame B") return
    val ed =
      prefs
        .edit()
        .putBoolean(uidKey(KEY_SLOT_B_UNLOCKED, uid), unlocked)
        .putBoolean(uidKey("cosmetic_unlocked_FRAME_SLOT_PURCHASE_2", uid), unlocked)
    if (canUseLegacyUnscopedFallback(uid)) {
      ed
        .putBoolean(KEY_SLOT_B_UNLOCKED, unlocked)
        .putBoolean("cosmetic_unlocked_FRAME_SLOT_PURCHASE_2", unlocked)
    }
    ed.apply()
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
    val nextBuffs = kotlin.math.max(getXpFromBuffs(uid), xpFromBuffs)
    val nextDmg = kotlin.math.max(getXpFromDamage(uid), xpFromDamage)
    val nextAcc = kotlin.math.max(getXpFromAccuracy(uid), xpFromAccuracy)
    val nextEarned = kotlin.math.max(getXpFromBountyEarned(uid), xpFromBountyEarned)
    val nextKilled = kotlin.math.max(getXpFromBountyKilled(uid), xpFromBountyKilled)
    val nextMilestone = kotlin.math.max(getHighestMilestoneLevelClaimed(uid), highestMilestoneLevelClaimed)
    val ed =
      prefs
        .edit()
        .putLong(uidKey(KEY_XP_BUFFS, uid), nextBuffs)
        .putLong(uidKey(KEY_XP_DAMAGE, uid), nextDmg)
        .putLong(uidKey(KEY_XP_ACCURACY, uid), nextAcc)
        .putLong(uidKey(KEY_XP_BOUNTY_EARNED, uid), nextEarned)
        .putLong(uidKey(KEY_XP_BOUNTY_KILLED, uid), nextKilled)
        .putLong(uidKey(KEY_RIFT_SHARDS, uid), cleanShards)
        .putInt(uidKey(KEY_HIGHEST_MILESTONE_LVL, uid), nextMilestone)
    if (canUseLegacyUnscopedFallback(uid)) {
      ed
        .putLong(KEY_XP_BUFFS, nextBuffs)
        .putLong(KEY_XP_DAMAGE, nextDmg)
        .putLong(KEY_XP_ACCURACY, nextAcc)
        .putLong(KEY_XP_BOUNTY_EARNED, nextEarned)
        .putLong(KEY_XP_BOUNTY_KILLED, nextKilled)
        .putLong(KEY_RIFT_SHARDS, cleanShards)
        .putInt(KEY_HIGHEST_MILESTONE_LVL, nextMilestone)
    }
    ed.apply()
  }

  fun claimPendingMilestoneShards(currentLevel: Int, uid: String? = null): Long {
    val highestClaimed = getHighestMilestoneLevelClaimed(uid).coerceAtLeast(1)
    val clampedLevel = currentLevel.coerceIn(1, 100)
    val reward =
      com.example.engine.RiftProgressionCatalog.calculateUnclaimedMilestoneShards(
        highestClaimedLevel = highestClaimed,
        currentLevel = clampedLevel,
      )
    if (reward > 0L || clampedLevel > highestClaimed) {
      val nextShards = getRiftShards(uid) + reward
      val ed =
        prefs
          .edit()
          .putLong(uidKey(KEY_RIFT_SHARDS, uid), nextShards)
          .putInt(uidKey(KEY_HIGHEST_MILESTONE_LVL, uid), clampedLevel)
      if (canUseLegacyUnscopedFallback(uid)) {
        ed
          .putLong(KEY_RIFT_SHARDS, nextShards)
          .putInt(KEY_HIGHEST_MILESTONE_LVL, clampedLevel)
      }
      ed.apply()
    }
    return reward
  }

  fun isCosmeticUnlocked(
    category: String,
    itemId: Int,
    @Suppress("UNUSED_PARAMETER") pilotLevel: Int = getPilotLevel(),
    uid: String? = null,
  ): Boolean {
    val cat = category.trim().uppercase(Locale.US)
    if (cat == "FRAME_SLOT_PURCHASE") {
      return when (itemId) {
        0, 1 -> true
        2 ->
          isPersistedUnlockFlag(KEY_SLOT_B_UNLOCKED, uid) ||
            isPersistedUnlockFlag("cosmetic_unlocked_FRAME_SLOT_PURCHASE_2", uid)
        else -> false
      }
    }
    if (itemId <= 0) return true
    return when (cat) {
      "HULL_TRIM",
      "SHIP_SKIN",
      "SHIP_SKIN_PURCHASE" ->
        isPersistedUnlockFlag("cosmetic_unlocked_SHIP_SKIN_PURCHASE_$itemId", uid) ||
          isPersistedUnlockFlag("cosmetic_unlocked_HULL_TRIM_$itemId", uid)
      "KILL_FINISHER",
      "WARP_SIGNATURE",
      "HVT_HALO" -> {
        val item = com.example.engine.RiftProgressionCatalog.getCosmeticItem(cat, itemId)
        item != null && getLiveResonance(uid) >= item.freeAtResonance
      }
      else -> isPersistedUnlockFlag("cosmetic_unlocked_${cat}_$itemId", uid)
    }
  }

  fun setCosmeticUnlocked(
    category: String,
    itemId: Int,
    unlocked: Boolean = true,
    uid: String? = null,
  ) {
    val cat = category.trim().uppercase(Locale.US)
    val ed = prefs.edit()
    when {
      cat == "FRAME_SLOT_PURCHASE" && itemId == 2 -> {
        val purchaseKey = "cosmetic_unlocked_FRAME_SLOT_PURCHASE_2"
        ed
          .putBoolean(uidKey(purchaseKey, uid), unlocked)
          .putBoolean(uidKey(KEY_SLOT_B_UNLOCKED, uid), unlocked)
        if (canUseLegacyUnscopedFallback(uid)) {
          ed
            .putBoolean(purchaseKey, unlocked)
            .putBoolean(KEY_SLOT_B_UNLOCKED, unlocked)
        }
      }
      cat == "HULL_TRIM" || cat == "SHIP_SKIN" || cat == "SHIP_SKIN_PURCHASE" -> {
        val skinKey = "cosmetic_unlocked_SHIP_SKIN_PURCHASE_$itemId"
        val trimKey = "cosmetic_unlocked_HULL_TRIM_$itemId"
        ed
          .putBoolean(uidKey(skinKey, uid), unlocked)
          .putBoolean(uidKey(trimKey, uid), unlocked)
        if (canUseLegacyUnscopedFallback(uid)) {
          ed
            .putBoolean(skinKey, unlocked)
            .putBoolean(trimKey, unlocked)
        }
      }
      else -> {
        val baseKey = "cosmetic_unlocked_${cat}_$itemId"
        ed.putBoolean(uidKey(baseKey, uid), unlocked)
        if (canUseLegacyUnscopedFallback(uid)) {
          ed.putBoolean(baseKey, unlocked)
        }
      }
    }
    ed.apply()
  }

  fun getEquippedCosmeticId(category: String, uid: String? = null): Int {
    val cat = category.trim().uppercase(Locale.US)
    val baseKey = "cosmetic_equipped_$cat"
    val fallback = if (canUseLegacyUnscopedFallback(uid)) prefs.getInt(baseKey, 0) else 0
    val rawId =
      (if (!uid.isNullOrBlank()) prefs.getInt(uidKey(baseKey, uid), fallback) else fallback)
        .coerceAtLeast(0)
    if (rawId <= 0) return 0
    return if (isCosmeticUnlocked(cat, rawId, uid = uid)) rawId else 0
  }

  fun setEquippedCosmeticId(category: String, itemId: Int, uid: String? = null) {
    val cat = category.trim().uppercase(Locale.US)
    val baseKey = "cosmetic_equipped_$cat"
    val cleanId = itemId.coerceAtLeast(0)
    val ed = prefs.edit().putInt(uidKey(baseKey, uid), cleanId)
    if (canUseLegacyUnscopedFallback(uid)) {
      ed.putInt(baseKey, cleanId)
    }
    ed.apply()
  }

  // =========================================================================
  // RIFT CONDUIT PER-SHIP & OVERALL RESONANCE + 3 DAILY DIRECTIVES (250 SHARDS / DAY)
  // =========================================================================
  private fun shipResonanceBaseKey(shipId: Int): String =
    "${KEY_SHIP_RESONANCE_PREFIX}${shipId.coerceIn(1, 10)}"

  private fun getShipResonanceRaw(shipId: Int, uid: String? = null): Int {
    val base = shipResonanceBaseKey(shipId)
    val fallback = if (canUseLegacyUnscopedFallback(uid)) prefs.getInt(base, 0) else 0
    return prefs.getInt(uidKey(base, uid), fallback).coerceAtLeast(0)
  }

  private fun ensureShipResonancesMigrated(uid: String? = null) {
    ensureResonanceMigrated(uid)
    val migKey = uidKey(KEY_SHIP_RESONANCE_MIGRATED, uid)
    if (prefs.getBoolean(migKey, false)) return
    var sum = 0
    for (id in 1..10) {
      sum += getShipResonanceRaw(id, uid)
    }
    val fallbackLive = if (canUseLegacyUnscopedFallback(uid)) prefs.getInt(KEY_LIVE_RESONANCE, 0) else 0
    val existingLive =
      prefs
        .getInt(uidKey(KEY_LIVE_RESONANCE, uid), fallbackLive)
        .coerceAtLeast(0)
    val ed = prefs.edit().putBoolean(migKey, true)
    if (sum == 0 && existingLive > 0) {
      val seedShipId = getLastShipId(uid).coerceIn(1, 10)
      val base = shipResonanceBaseKey(seedShipId)
      ed.putInt(uidKey(base, uid), existingLive)
      if (canUseLegacyUnscopedFallback(uid)) {
        ed.putInt(base, existingLive)
      }
    }
    ed.apply()
  }

  fun getShipResonance(shipId: Int, uid: String? = null): Int {
    ensureShipResonancesMigrated(uid)
    return getShipResonanceRaw(shipId.coerceIn(1, 10), uid)
  }

  fun getAllShipResonances(uid: String? = null): Map<Int, Int> {
    ensureShipResonancesMigrated(uid)
    val out = LinkedHashMap<Int, Int>(10)
    for (id in 1..10) {
      out[id] = getShipResonanceRaw(id, uid)
    }
    return out
  }

  fun setShipResonance(shipId: Int, value: Int, uid: String? = null): Pair<Int, Int> {
    ensureShipResonancesMigrated(uid)
    val cleanId = shipId.coerceIn(1, 10)
    val cleanVal = value.coerceAtLeast(0)
    val base = shipResonanceBaseKey(cleanId)
    val edShip = prefs.edit().putInt(uidKey(base, uid), cleanVal)
    if (canUseLegacyUnscopedFallback(uid)) {
      edShip.putInt(base, cleanVal)
    }
    edShip.apply()
    var totalLive = 0
    for (id in 1..10) {
      totalLive += if (id == cleanId) cleanVal else getShipResonanceRaw(id, uid)
    }
    val nextPeak = kotlin.math.max(getPeakResonance(uid), totalLive)
    val edTot =
      prefs
        .edit()
        .putInt(uidKey(KEY_LIVE_RESONANCE, uid), totalLive)
        .putInt(uidKey(KEY_PEAK_RESONANCE, uid), nextPeak)
    if (canUseLegacyUnscopedFallback(uid)) {
      edTot.putInt(KEY_LIVE_RESONANCE, totalLive).putInt(KEY_PEAK_RESONANCE, nextPeak)
    }
    edTot.apply()
    return cleanVal to totalLive
  }

  fun applyShipResonanceDelta(shipId: Int, delta: Int, uid: String? = null): Triple<Int, Int, Int> {
    ensureShipResonancesMigrated(uid)
    val cleanId = shipId.coerceIn(1, 10)
    val currentShipRes = getShipResonanceRaw(cleanId, uid)
    val nextShipRes = (currentShipRes + delta).coerceAtLeast(0)
    val (savedShipRes, totalLive) = setShipResonance(cleanId, nextShipRes, uid)
    return Triple(savedShipRes, totalLive, getPeakResonance(uid))
  }

  private fun ensureResonanceMigrated(uid: String? = null) {
    val scopedKey = uidKey(KEY_RESONANCE_MIGRATED, uid)
    if (prefs.getBoolean(scopedKey, false)) return
    val hasLiveKey =
      prefs.contains(uidKey(KEY_LIVE_RESONANCE, uid)) ||
        (canUseLegacyUnscopedFallback(uid) && prefs.contains(KEY_LIVE_RESONANCE))
    val lvl = getPilotLevel(uid)
    val careerKills = getAllTimeKills(uid)
    if (!hasLiveKey && (careerKills > 0L || lvl > 1)) {
      val seeded = ((lvl - 1) * 18).coerceAtLeast(0)
      val ed =
        prefs
          .edit()
          .putInt(uidKey(KEY_LIVE_RESONANCE, uid), seeded)
          .putInt(uidKey(KEY_PEAK_RESONANCE, uid), seeded)
          .putBoolean(scopedKey, true)
      if (canUseLegacyUnscopedFallback(uid)) {
        ed.putInt(KEY_LIVE_RESONANCE, seeded).putInt(KEY_PEAK_RESONANCE, seeded)
      }
      ed.apply()
    } else {
      prefs.edit().putBoolean(scopedKey, true).apply()
    }
  }

  fun getLiveResonance(uid: String? = null): Int {
    ensureShipResonancesMigrated(uid)
    var sum = 0
    for (id in 1..10) {
      sum += getShipResonanceRaw(id, uid)
    }
    val fallback = if (canUseLegacyUnscopedFallback(uid)) prefs.getInt(KEY_LIVE_RESONANCE, 0) else 0
    val rawLive =
      prefs
        .getInt(uidKey(KEY_LIVE_RESONANCE, uid), fallback)
        .coerceAtLeast(0)
    return kotlin.math.max(sum, rawLive)
  }

  fun setLiveResonance(value: Int, uid: String? = null) {
    ensureShipResonancesMigrated(uid)
    val clean = value.coerceAtLeast(0)
    var sum = 0
    for (id in 1..10) {
      sum += getShipResonanceRaw(id, uid)
    }
    val ed = prefs.edit()
    val allowLegacy = canUseLegacyUnscopedFallback(uid)
    if (clean > sum) {
      val diff = clean - sum
      val targetShip = getBestShipId(uid, getLastShipId(uid)).coerceIn(1, 10)
      val base = shipResonanceBaseKey(targetShip)
      val updatedShipRes = getShipResonanceRaw(targetShip, uid) + diff
      ed.putInt(uidKey(base, uid), updatedShipRes)
      if (allowLegacy) ed.putInt(base, updatedShipRes)
    } else if (clean < sum && sum > 0) {
      var remainingDrop = sum - clean
      val activeShip = getLastShipId(uid).coerceIn(1, 10)
      val activeRes = getShipResonanceRaw(activeShip, uid)
      val dropActive = kotlin.math.min(activeRes, remainingDrop)
      if (dropActive > 0) {
        val base = shipResonanceBaseKey(activeShip)
        ed.putInt(uidKey(base, uid), activeRes - dropActive)
        if (allowLegacy) ed.putInt(base, activeRes - dropActive)
        remainingDrop -= dropActive
      }
      if (remainingDrop > 0) {
        for (id in 1..10) {
          if (id == activeShip || remainingDrop <= 0) continue
          val r = getShipResonanceRaw(id, uid)
          val d = kotlin.math.min(r, remainingDrop)
          if (d > 0) {
            val base = shipResonanceBaseKey(id)
            ed.putInt(uidKey(base, uid), r - d)
            if (allowLegacy) ed.putInt(base, r - d)
            remainingDrop -= d
          }
        }
      }
    }
    val nextPeak = kotlin.math.max(getPeakResonance(uid), clean)
    ed
      .putInt(uidKey(KEY_LIVE_RESONANCE, uid), clean)
      .putInt(uidKey(KEY_PEAK_RESONANCE, uid), nextPeak)
    if (allowLegacy) {
      ed.putInt(KEY_LIVE_RESONANCE, clean).putInt(KEY_PEAK_RESONANCE, nextPeak)
    }
    ed.apply()
  }

  fun getPeakResonance(uid: String? = null): Int {
    ensureResonanceMigrated(uid)
    val fallbackPeak = if (canUseLegacyUnscopedFallback(uid)) prefs.getInt(KEY_PEAK_RESONANCE, 0) else 0
    val fallbackLive = if (canUseLegacyUnscopedFallback(uid)) prefs.getInt(KEY_LIVE_RESONANCE, 0) else 0
    val rawPeak =
      prefs
        .getInt(uidKey(KEY_PEAK_RESONANCE, uid), fallbackPeak)
        .coerceAtLeast(0)
    val live =
      prefs
        .getInt(uidKey(KEY_LIVE_RESONANCE, uid), fallbackLive)
        .coerceAtLeast(0)
    return kotlin.math.max(rawPeak, live)
  }

  fun setPeakResonance(value: Int, uid: String? = null) {
    ensureResonanceMigrated(uid)
    val clean = kotlin.math.max(getPeakResonance(uid), value.coerceAtLeast(0))
    prefs
      .edit()
      .putInt(KEY_PEAK_RESONANCE, clean)
      .putInt(uidKey(KEY_PEAK_RESONANCE, uid), clean)
      .apply()
  }

  fun applyResonanceDelta(delta: Int, uid: String? = null): Pair<Int, Int> {
    val activeShip = getLastShipId().coerceIn(1, 10)
    val (_, nextLive, nextPeak) = applyShipResonanceDelta(activeShip, delta, uid)
    return nextLive to nextPeak
  }

  private fun currentUtcDayIndex(): Long = System.currentTimeMillis() / 86_400_000L

  private fun ensureDailyDirectivesCurrentForToday(uid: String? = null): Long {
    val today = currentUtcDayIndex()
    val dayKey = uidKey(KEY_DAILY_DIRECTIVES_UTC_DAY, uid)
    val savedDay = prefs.getLong(dayKey, -1L)
    if (savedDay != today) {
      val ed = prefs.edit().putLong(dayKey, today)
      for (slot in 0..2) {
        ed.putInt(uidKey("${KEY_DAILY_DIRECTIVE_PROGRESS_PREFIX}$slot", uid), 0)
        ed.putBoolean(uidKey("${KEY_DAILY_DIRECTIVE_CLAIMED_PREFIX}$slot", uid), false)
        ed.putInt(uidKey("${KEY_DAILY_DIRECTIVE_REROLL_SALT_PREFIX}$slot", uid), 0)
      }
      ed.putBoolean(uidKey(KEY_DAILY_DIRECTIVE_REROLL_USED, uid), false)
      ed.putBoolean(uidKey(KEY_DAILY_SWEEP_CLAIMED, uid), false)
      ed.apply()
    }
    return today
  }

  fun getDailyDirectives(uid: String? = null): List<com.example.engine.DailyConduitDirective> {
    val today = ensureDailyDirectivesCurrentForToday(uid)
    val salts =
      intArrayOf(
        prefs.getInt(uidKey("${KEY_DAILY_DIRECTIVE_REROLL_SALT_PREFIX}0", uid), 0),
        prefs.getInt(uidKey("${KEY_DAILY_DIRECTIVE_REROLL_SALT_PREFIX}1", uid), 0),
        prefs.getInt(uidKey("${KEY_DAILY_DIRECTIVE_REROLL_SALT_PREFIX}2", uid), 0),
      )
    val baseList = com.example.engine.RiftProgressionCatalog.generateDailyDirectivesForDay(today, salts)
    return baseList.map { dir ->
      val prog =
        prefs
          .getInt(uidKey("${KEY_DAILY_DIRECTIVE_PROGRESS_PREFIX}${dir.slotIndex}", uid), 0)
          .coerceIn(0, dir.targetValue)
      val isClaimed =
        prefs.getBoolean(uidKey("${KEY_DAILY_DIRECTIVE_CLAIMED_PREFIX}${dir.slotIndex}", uid), false)
      dir.copy(progressValue = prog, claimed = isClaimed)
    }
  }

  fun recordDailyDirectiveProgress(
    metric: com.example.engine.DailyDirectiveMetric,
    amount: Int,
    isAbsolutePeak: Boolean = false,
    uid: String? = null,
  ) {
    if (amount <= 0) return
    val current = getDailyDirectives(uid)
    val ed = prefs.edit()
    var mutated = false
    for (dir in current) {
      if (dir.metric == metric && !dir.claimed) {
        val nextProg =
          if (isAbsolutePeak) {
            kotlin.math.max(dir.progressValue, amount).coerceAtMost(dir.targetValue)
          } else {
            (dir.progressValue + amount).coerceAtMost(dir.targetValue)
          }
        if (nextProg != dir.progressValue) {
          ed.putInt(uidKey("${KEY_DAILY_DIRECTIVE_PROGRESS_PREFIX}${dir.slotIndex}", uid), nextProg)
          mutated = true
        }
      }
    }
    if (mutated) {
      ed.apply()
    }
  }

  fun canRerollDailyDirectiveToday(uid: String? = null): Boolean {
    ensureDailyDirectivesCurrentForToday(uid)
    return !prefs.getBoolean(uidKey(KEY_DAILY_DIRECTIVE_REROLL_USED, uid), false)
  }

  fun rerollDailyDirective(slotIndex: Int, uid: String? = null): Boolean {
    if (slotIndex !in 0..2) return false
    ensureDailyDirectivesCurrentForToday(uid)
    if (!canRerollDailyDirectiveToday(uid)) return false
    val dir = getDailyDirectives(uid).firstOrNull { it.slotIndex == slotIndex } ?: return false
    if (dir.claimed) return false
    val saltKey = uidKey("${KEY_DAILY_DIRECTIVE_REROLL_SALT_PREFIX}$slotIndex", uid)
    val nextSalt = prefs.getInt(saltKey, 0) + 1
    prefs
      .edit()
      .putInt(saltKey, nextSalt)
      .putInt(uidKey("${KEY_DAILY_DIRECTIVE_PROGRESS_PREFIX}$slotIndex", uid), 0)
      .putBoolean(uidKey(KEY_DAILY_DIRECTIVE_REROLL_USED, uid), true)
      .apply()
    return true
  }

  fun claimDailyDirectiveReward(slotIndex: Int, uid: String? = null): Pair<Long, Int>? {
    if (slotIndex !in 0..2) return null
    val dir = getDailyDirectives(uid).firstOrNull { it.slotIndex == slotIndex } ?: return null
    if (!dir.isComplete || dir.claimed) return null
    val nextShards = getRiftShards(uid) + dir.shardReward
    setRiftShards(nextShards, uid)
    applyResonanceDelta(dir.resonanceReward, uid)
    awardDailyDirectivesXp(120L, uid)
    prefs
      .edit()
      .putBoolean(uidKey("${KEY_DAILY_DIRECTIVE_CLAIMED_PREFIX}$slotIndex", uid), true)
      .apply()
    return dir.shardReward to dir.resonanceReward
  }

  fun isDailySweepBonusClaimed(uid: String? = null): Boolean {
    ensureDailyDirectivesCurrentForToday(uid)
    return prefs.getBoolean(uidKey(KEY_DAILY_SWEEP_CLAIMED, uid), false)
  }

  fun claimDailySweepBonus(uid: String? = null): Pair<Long, Int>? {
    val all = getDailyDirectives(uid)
    if (all.size < 3 || all.any { !it.claimed } || isDailySweepBonusClaimed(uid)) return null
    val sweepShards = 100L
    val sweepRes = 25
    setRiftShards(getRiftShards(uid) + sweepShards, uid)
    applyResonanceDelta(sweepRes, uid)
    awardDailyDirectivesXp(240L, uid)
    prefs.edit().putBoolean(uidKey(KEY_DAILY_SWEEP_CLAIMED, uid), true).apply()
    return sweepShards to sweepRes
  }

  // Adaptive Squadron Tier Kills (1 = SOLO, 2 = 2-Pilot, 3 = 3-Pilot, 4 = 4-Pilot, 5 = 5-Pilot Squadron)
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
    uid: String? = null,
  ) {
    val ed =
      prefs
        .edit()
        .putLong(uidKey(KEY_PVP_KILLS, uid), pvpKills)
        .putLong(uidKey(KEY_PVP_DEATHS, uid), pvpDeaths)
        .putLong(uidKey(KEY_PVP_BOUNTY_PEAK, uid), pvpBountyPeak)
        .putLong(uidKey(KEY_ALL_KILLS, uid), allTimeKills)
        .putLong(uidKey(KEY_ALL_DEATHS, uid), allTimeDeaths)
        .putLong(uidKey(KEY_ALL_BOUNTY_PEAK, uid), allTimeBountyPeak)
        .putLong(uidKey(KEY_CONVERGENCES_WON, uid), convergencesWon)
        .putLong(uidKey(KEY_HVT_ELIMINATIONS, uid), hvtEliminations)
        .putLong(uidKey(KEY_GREENS_COLLECTED, uid), greensCollected)
    if (canUseLegacyUnscopedFallback(uid)) {
      ed
        .putLong(KEY_PVP_KILLS, pvpKills)
        .putLong(KEY_PVP_DEATHS, pvpDeaths)
        .putLong(KEY_PVP_BOUNTY_PEAK, pvpBountyPeak)
        .putLong(KEY_ALL_KILLS, allTimeKills)
        .putLong(KEY_ALL_DEATHS, allTimeDeaths)
        .putLong(KEY_ALL_BOUNTY_PEAK, allTimeBountyPeak)
        .putLong(KEY_CONVERGENCES_WON, convergencesWon)
        .putLong(KEY_HVT_ELIMINATIONS, hvtEliminations)
        .putLong(KEY_GREENS_COLLECTED, greensCollected)
    }
    ed.apply()
  }

  private fun isVerifiedRealFriend(entry: FriendPilotEntry): Boolean {
    val cleanUid = entry.uid.trim()
    val cleanCallsign = entry.callsign.trim()
    val cleanTag = entry.syndicateTag.trim().uppercase(Locale.US)
    if (cleanUid.isBlank() || cleanCallsign.length < 2) return false
    if (
      cleanUid.startsWith("friend_custom_", ignoreCase = true) ||
        cleanUid.startsWith("bot_", ignoreCase = true) ||
        cleanUid.startsWith("drifter_", ignoreCase = true) ||
        cleanUid.startsWith("dev_", ignoreCase = true)
    ) {
      return false
    }
    if (cleanTag == "BOT" || cleanTag == "RFT") return false
    if (cleanCallsign.contains("[BOT]", ignoreCase = true)) return false
    if (
      entry.statusText.contains("Sector B4", ignoreCase = true) ||
        entry.statusText.contains("Sector E5", ignoreCase = true)
    ) {
      return false
    }
    return true
  }

  private fun persistVerifiedFriendsList(list: List<FriendPilotEntry>) {
    val arr = JSONArray()
    for (f in list) {
      if (!isVerifiedRealFriend(f)) continue
      arr.put(
        JSONObject().apply {
          put("uid", f.uid)
          put("callsign", f.callsign)
          put("syndicateTag", f.syndicateTag)
          put("shipId", f.shipId.coerceIn(1, 10))
          put("statusText", f.statusText)
          put("isOnline", f.isOnline)
          put("currentArmadaName", f.currentArmadaName)
          put("currentArmadaCount", f.currentArmadaCount.coerceIn(1, 5))
          put("bounty", f.bounty.coerceAtLeast(100))
        }
      )
    }
    prefs.edit().putString(KEY_CUSTOM_FRIENDS_JSON, arr.toString()).apply()
  }

  fun getFriendsList(): List<FriendPilotEntry> {
    val customRaw = prefs.getString(KEY_CUSTOM_FRIENDS_JSON, "[]") ?: "[]"
    val verifiedList = mutableListOf<FriendPilotEntry>()
    var purgedStale = false
    try {
      val arr = JSONArray(customRaw)
      for (i in 0 until arr.length()) {
        val obj = arr.getJSONObject(i)
        val entry =
          FriendPilotEntry(
            uid = obj.optString("uid", "").trim(),
            callsign = obj.optString("callsign", "").trim(),
            syndicateTag = sanitizeSyndicateTag(obj.optString("syndicateTag", "")),
            shipId = obj.optInt("shipId", 1).coerceIn(1, 10),
            statusText = obj.optString("statusText", "Verified Sworn Pilot"),
            isOnline = obj.optBoolean("isOnline", false),
            currentArmadaName = obj.optString("currentArmadaName", "").take(14),
            currentArmadaCount = obj.optInt("currentArmadaCount", 1).coerceIn(1, 5),
            bounty = obj.optInt("bounty", 100).coerceAtLeast(100),
          )
        if (isVerifiedRealFriend(entry) && verifiedList.none { it.uid == entry.uid }) {
          verifiedList.add(entry)
        } else {
          purgedStale = true
        }
      }
    } catch (_: Exception) {}

    if (purgedStale) {
      persistVerifiedFriendsList(verifiedList)
    }
    return verifiedList
  }

  fun saveVerifiedFriendPilot(entry: FriendPilotEntry) {
    if (!isVerifiedRealFriend(entry)) return
    val current = getFriendsList().toMutableList()
    val existingIdx =
      current.indexOfFirst {
        it.uid == entry.uid || it.callsign.equals(entry.callsign, ignoreCase = true)
      }
    if (existingIdx >= 0) {
      current[existingIdx] = entry
    } else {
      current.add(entry)
    }
    persistVerifiedFriendsList(current)
  }

  fun removeFriendPilot(uidOrCallsign: String) {
    val clean = uidOrCallsign.trim()
    if (clean.isEmpty()) return
    val remaining =
      getFriendsList().filterNot {
        it.uid == clean || it.callsign.equals(clean, ignoreCase = true)
      }
    persistVerifiedFriendsList(remaining)
  }

  /**
   * Returns an already-verified Sworn Pilot friend matching [rawCallsign], or null.
   * Never fabricates fake local pilots; new pilot discovery goes through Firestore lookup.
   */
  fun addFriendByCallsign(rawCallsign: String): FriendPilotEntry? {
    val clean =
      rawCallsign
        .trim()
        .uppercase(Locale.US)
        .replace(Regex("[^A-Z0-9_\\-]"), "")
        .take(14)
    if (clean.length < 2 || UgcModeration.containsProfanity(clean)) return null
    return getFriendsList().firstOrNull { it.callsign.equals(clean, ignoreCase = true) }
  }

  fun getAllSyndicates(): List<SyndicateInfo> {
    val customRaw = prefs.getString(KEY_CUSTOM_SYNDICATES_JSON, "[]") ?: "[]"
    val dedupedByTag = linkedMapOf<String, SyndicateInfo>()
    var purgedStaleEntries = false
    try {
      val arr = JSONArray(customRaw)
      for (i in 0 until arr.length()) {
        val obj = arr.getJSONObject(i)
        val parsedTag = sanitizeSyndicateTag(obj.optString("tag", ""))
        val parsedOwner = obj.optString("ownerId", "").trim()
        val syn =
          SyndicateInfo(
            id = obj.optString("id", "syn_custom_$i"),
            ownerId = parsedOwner,
            tag = parsedTag,
            name = obj.optString("name", "$parsedTag Syndicate").trim(),
            foundryOrigin = obj.optString("foundryOrigin", "Player Founded Syndicate"),
            motto = obj.optString("motto", "Forged in the Convergence"),
            memberCount = obj.optInt("memberCount", 1).coerceAtLeast(1),
            combinedBounty = obj.optLong("combinedBounty", 100L).coerceAtLeast(100L),
            isCustom = obj.optBoolean("isCustom", false),
          )
        if (isHumanFoundedSyndicateRecord(syn)) {
          val existing = dedupedByTag[parsedTag]
          if (existing == null) {
            dedupedByTag[parsedTag] = syn
          } else {
            purgedStaleEntries = true
            dedupedByTag[parsedTag] =
              existing.copy(
                memberCount = kotlin.math.max(existing.memberCount, syn.memberCount),
                combinedBounty = kotlin.math.max(existing.combinedBounty, syn.combinedBounty),
              )
          }
        } else {
          purgedStaleEntries = true
        }
      }
    } catch (_: Exception) {}

    val cleanedList = dedupedByTag.values.toList()
    if (purgedStaleEntries) {
      persistSyndicatesJson(cleanedList)
    }
    return cleanedList
  }

  private fun persistSyndicatesJson(list: Collection<SyndicateInfo>) {
    val dedupedByTag = linkedMapOf<String, SyndicateInfo>()
    for (syn in list) {
      val cleanTag = sanitizeSyndicateTag(syn.tag)
      val normalized = syn.copy(tag = cleanTag)
      if (!isHumanFoundedSyndicateRecord(normalized)) continue
      val existing = dedupedByTag[cleanTag]
      dedupedByTag[cleanTag] =
        if (existing != null) {
          normalized.copy(
            memberCount = kotlin.math.max(existing.memberCount, normalized.memberCount),
            combinedBounty = kotlin.math.max(existing.combinedBounty, normalized.combinedBounty),
          )
        } else {
          normalized
        }
    }
    val arr = JSONArray()
    for (syn in dedupedByTag.values) {
      arr.put(
        JSONObject().apply {
          put("id", syn.id)
          put("ownerId", syn.ownerId)
          put("tag", syn.tag)
          put("name", syn.name)
          put("foundryOrigin", syn.foundryOrigin)
          put("motto", syn.motto)
          put("memberCount", syn.memberCount)
          put("combinedBounty", syn.combinedBounty)
          put("isCustom", true)
        }
      )
    }
    prefs.edit().putString(KEY_CUSTOM_SYNDICATES_JSON, arr.toString()).apply()
  }

  fun getSyndicatesList(): List<SyndicateInfo> = getAllSyndicates()

  fun mergeCloudSyndicates(cloudSyndicates: List<SyndicateInfo>) {
    val mergedByTag = linkedMapOf<String, SyndicateInfo>()
    for (local in getAllSyndicates()) {
      if (isHumanFoundedSyndicateRecord(local)) {
        mergedByTag[local.tag.uppercase(Locale.US)] = local
      }
    }
    for (cloud in cloudSyndicates) {
      val cleanTag = sanitizeSyndicateTag(cloud.tag)
      val normalized =
        cloud.copy(
          id = cloud.id.ifBlank { "syn_cloud_$cleanTag" },
          tag = cleanTag,
          isCustom = cloud.isCustom,
        )
      if (isHumanFoundedSyndicateRecord(normalized)) {
        val existing = mergedByTag[cleanTag]
        mergedByTag[cleanTag] =
          if (existing != null) {
            normalized.copy(
              memberCount = kotlin.math.max(existing.memberCount, normalized.memberCount),
              combinedBounty = kotlin.math.max(existing.combinedBounty, normalized.combinedBounty),
            )
          } else {
            normalized
          }
      }
    }
    persistSyndicatesJson(mergedByTag.values)
  }

  fun createCustomSyndicate(
    tag: String,
    name: String,
    motto: String,
    ownerUid: String = "",
  ): Result<SyndicateInfo> {
    val cleanOwner = ownerUid.trim()
    if (
      isGuestBypassEnabled() ||
        cleanOwner.isEmpty() ||
        cleanOwner.startsWith("dev_") ||
        cleanOwner.startsWith("drifter_") ||
        cleanOwner.startsWith("guest_") ||
        cleanOwner.startsWith("bot_")
    ) {
      return Result.failure(
        IllegalStateException("Only Google-authenticated Sworn Pilots can found Syndicates.")
      )
    }
    val now = System.currentTimeMillis()
    val remainingDays = getRemainingSyndicateLockDays(now)
    if (remainingDays > 0) {
      return Result.failure(
        IllegalStateException(
          "15-Day Syndicate Lock active (${getRemainingSyndicateLockFormatted(now)} remaining)."
        )
      )
    }

    val rawCleanedTag = tag.trim().uppercase(Locale.US).replace(Regex("[^A-Z0-9_\\-]"), "")
    if (rawCleanedTag in NON_PLAYER_SYNDICATE_TAGS) {
      return Result.failure(IllegalArgumentException("[$rawCleanedTag] is a reserved system tag."))
    }
    val cleanTag = sanitizeSyndicateTag(tag)
    if (cleanTag.length !in 1..3) {
      return Result.failure(IllegalArgumentException("Syndicate Tag must be 1 to 3 characters."))
    }
    if (cleanTag in NON_PLAYER_SYNDICATE_TAGS) {
      return Result.failure(IllegalArgumentException("[$cleanTag] is a reserved system tag."))
    }
    if (UgcModeration.containsProfanity(cleanTag)) {
      return Result.failure(IllegalArgumentException("Syndicate Tag contains prohibited language."))
    }
    val cleanName = name.trim().replace(Regex("[^a-zA-Z0-9 _\\-]"), "").take(26)
    if (cleanName.length < 3 || isGhostSyndicateName(cleanName)) {
      return Result.failure(IllegalArgumentException("Syndicate Name must be a unique 3+ character name."))
    }
    if (UgcModeration.containsProfanity(cleanName)) {
      return Result.failure(IllegalArgumentException("Syndicate Name contains prohibited language."))
    }

    val existing = getAllSyndicates()
    if (existing.any { it.tag.equals(cleanTag, ignoreCase = true) }) {
      return Result.failure(IllegalArgumentException("Tag [$cleanTag] is already registered."))
    }

    val cleanMotto =
      UgcModeration.sanitizeUgcText(motto.trim().take(60)).ifEmpty { "Forged in the RIFT Convergence" }

    val newSyndicate =
      SyndicateInfo(
        id = "syn_custom_$now",
        ownerId = cleanOwner,
        tag = cleanTag,
        name = cleanName,
        foundryOrigin = "Sworn Pilot Syndicate",
        motto = cleanMotto,
        memberCount = 1,
        combinedBounty = getAllTimeBountyPeak().coerceAtLeast(100L),
        isCustom = true,
      )

    persistSyndicatesJson(existing + newSyndicate)
    joinOfficialSyndicate(cleanTag, cleanName, apply15DayLock = true, role = "Founder / Commander")
    return Result.success(newSyndicate)
  }

  fun getSyndicateCrestJson(tag: String = getSyndicateTag()): String? {
    val cleanTag = sanitizeSyndicateTag(tag)
    if (cleanTag.isEmpty() || cleanTag in NON_PLAYER_SYNDICATE_TAGS) return null
    return prefs.getString("${KEY_SYNDICATE_CREST_PREFIX}$cleanTag", null)?.takeIf { it.isNotBlank() }
  }

  fun setSyndicateCrestJson(tag: String, crestJson: String) {
    val cleanTag = sanitizeSyndicateTag(tag)
    if (cleanTag.isEmpty() || cleanTag in NON_PLAYER_SYNDICATE_TAGS) return
    if (crestJson.isBlank()) return
    prefs.edit().putString("${KEY_SYNDICATE_CREST_PREFIX}$cleanTag", crestJson.trim()).apply()
  }

  fun isSyndicateInviteOnly(tag: String = getSyndicateTag()): Boolean {
    val cleanTag = sanitizeSyndicateTag(tag)
    if (cleanTag.isEmpty() || cleanTag in NON_PLAYER_SYNDICATE_TAGS) return false
    return prefs.getBoolean("${KEY_SYNDICATE_INVITE_ONLY_PREFIX}$cleanTag", false)
  }

  fun setSyndicateInviteOnly(tag: String, inviteOnly: Boolean) {
    val cleanTag = sanitizeSyndicateTag(tag)
    if (cleanTag.isEmpty() || cleanTag in NON_PLAYER_SYNDICATE_TAGS) return
    prefs.edit().putBoolean("${KEY_SYNDICATE_INVITE_ONLY_PREFIX}$cleanTag", inviteOnly).apply()
  }

  fun getSyndicateOfficers(tag: String = getSyndicateTag()): Set<String> {
    val cleanTag = sanitizeSyndicateTag(tag)
    if (cleanTag.isEmpty() || cleanTag in NON_PLAYER_SYNDICATE_TAGS) return emptySet()
    val raw = prefs.getString("${KEY_SYNDICATE_OFFICERS_PREFIX}$cleanTag", "") ?: ""
    return raw.split(",").map { it.trim().uppercase(Locale.US) }.filter { it.isNotEmpty() }.toSet()
  }

  fun toggleSyndicateOfficer(tag: String, callsign: String): Boolean {
    val cleanTag = sanitizeSyndicateTag(tag)
    val cleanCallsign = callsign.trim().uppercase(Locale.US).replace(Regex("[^A-Z0-9_\\-]"), "").take(14)
    if (cleanTag.isEmpty() || cleanCallsign.isEmpty()) return false
    val current = getSyndicateOfficers(cleanTag).toMutableSet()
    val nowOfficer =
      if (current.any { it.equals(cleanCallsign, ignoreCase = true) }) {
        current.removeAll { it.equals(cleanCallsign, ignoreCase = true) }
        false
      } else {
        current.add(cleanCallsign)
        true
      }
    prefs.edit().putString("${KEY_SYNDICATE_OFFICERS_PREFIX}$cleanTag", current.take(10).joinToString(",")).apply()
    return nowOfficer
  }

  fun getKickedSyndicateMembers(tag: String = getSyndicateTag()): Set<String> {
    val cleanTag = sanitizeSyndicateTag(tag)
    if (cleanTag.isEmpty() || cleanTag in NON_PLAYER_SYNDICATE_TAGS) return emptySet()
    val raw = prefs.getString("syndicate_kicked_members_$cleanTag", "") ?: ""
    return raw.split(",").map { it.trim().uppercase(Locale.US) }.filter { it.isNotEmpty() }.toSet()
  }

  fun kickSyndicateMember(tag: String, callsign: String) {
    val cleanTag = sanitizeSyndicateTag(tag)
    val cleanCallsign = callsign.trim().uppercase(Locale.US).replace(Regex("[^A-Z0-9_\\-]"), "").take(14)
    if (cleanTag.isEmpty() || cleanCallsign.isEmpty()) return
    val officers = getSyndicateOfficers(cleanTag).filterNot { it.equals(cleanCallsign, ignoreCase = true) }
    val kicked = (getKickedSyndicateMembers(cleanTag) + cleanCallsign).take(30)
    prefs
      .edit()
      .putString("${KEY_SYNDICATE_OFFICERS_PREFIX}$cleanTag", officers.joinToString(","))
      .putString("syndicate_kicked_members_$cleanTag", kicked.joinToString(","))
      .apply()
    revokeGuestFromSyndicateChannels(cleanTag, cleanCallsign)
  }

  fun mergeCloudSyndicateMeta(meta: CloudSyndicateMeta) {
    val cleanTag = sanitizeSyndicateTag(meta.tag)
    if (cleanTag.isEmpty() || cleanTag in NON_PLAYER_SYNDICATE_TAGS) return
    if (meta.customChannels.isNotEmpty()) {
      setSyndicateCustomChannels(cleanTag, meta.customChannels)
    }
    val sanitizedGuests =
      meta.invitedGuests
        .map { it.trim().take(20) }
        .filter { it.isNotEmpty() && !it.startsWith("__") }
    val sanitizedOfficers =
      meta.officerCallsigns
        .map { it.trim().uppercase(Locale.US).take(14) }
        .filter { it.isNotEmpty() }
    prefs
      .edit()
      .putString("${KEY_SYNDICATE_GUESTS_PREFIX}$cleanTag", sanitizedGuests.joinToString(","))
      .putBoolean("${KEY_SYNDICATE_INVITE_ONLY_PREFIX}$cleanTag", meta.inviteOnly)
      .putString("${KEY_SYNDICATE_OFFICERS_PREFIX}$cleanTag", sanitizedOfficers.joinToString(","))
      .apply()
    if (meta.crestJson.isNotBlank()) {
      setSyndicateCrestJson(cleanTag, meta.crestJson)
    }
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
    var mutated = false
    if (!raw.isNullOrBlank()) {
      try {
        val arr = JSONArray(raw)
        for (i in 0 until arr.length()) {
          val entry = SyndicateServiceEntry.fromJson(arr.getJSONObject(i))
          val cleanTag = sanitizeSyndicateTag(entry.syndicateTag)
          val cleanName = if (cleanTag.isEmpty() || isGhostSyndicateName(entry.syndicateName)) "Unaffiliated" else entry.syndicateName
          val normalized =
            if (cleanTag != entry.syndicateTag || cleanName != entry.syndicateName) {
              mutated = true
              entry.copy(syndicateTag = cleanTag, syndicateName = cleanName)
            } else {
              entry
            }
          parsed.add(normalized)
        }
      } catch (_: Exception) {}
    }
    if (mutated && parsed.isNotEmpty()) {
      saveSyndicateServiceHistory(parsed, uid)
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
    return userSettingsPrefs.getFloat(KEY_PLAYFIELD_TEXT_SCALE, fallback).coerceIn(0.75f, 1.75f)
  }

  fun setPlayfieldTextScale(scale: Float) {
    val clamped = scale.coerceIn(0.75f, 1.75f)
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
    val cleanTag = syndicateTag.trim().uppercase(Locale.US)
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
    val cleanTag = syndicateTag.trim().uppercase(Locale.US)
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
        .lowercase(Locale.US)
        .removePrefix("#")
        .replace(Regex("[^a-z0-9_-]"), "-")
        .replace(Regex("-+"), "-")
        .trim('-')
        .take(18)
    return if (stripped.isEmpty()) "" else "#$stripped"
  }

  fun getSyndicateInvitedGuests(syndicateTag: String = getSyndicateTag()): Set<String> {
    val cleanTag = syndicateTag.trim().uppercase(Locale.US)
    if (cleanTag.isEmpty() || cleanTag == "SOLO") return emptySet()
    val raw = prefs.getString("${KEY_SYNDICATE_GUESTS_PREFIX}$cleanTag", "") ?: ""
    return raw.split(",").map { it.trim() }.filter { it.isNotEmpty() && !it.startsWith("__") }.toSet()
  }

  fun inviteGuestToSyndicateChannels(syndicateTag: String, guestCallsign: String) {
    val cleanTag = syndicateTag.trim().uppercase(Locale.US)
    val cleanGuest = guestCallsign.trim().take(20)
    if (cleanTag.isEmpty() || cleanTag == "SOLO" || cleanGuest.isEmpty()) return
    val existing = getSyndicateInvitedGuests(cleanTag).toMutableList()
    if (existing.none { it.equals(cleanGuest, ignoreCase = true) }) {
      existing.add(cleanGuest)
      prefs.edit().putString("${KEY_SYNDICATE_GUESTS_PREFIX}$cleanTag", existing.joinToString(",")).apply()
    }
  }

  fun revokeGuestFromSyndicateChannels(syndicateTag: String, guestCallsign: String) {
    val cleanTag = syndicateTag.trim().uppercase(Locale.US)
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
    if (isGuestBypassEnabled()) return false
    val targetClean = sanitizeSyndicateTag(targetSyndicateTag)
    if (targetClean.isEmpty() || targetClean in NON_PLAYER_SYNDICATE_TAGS) return false
    val pilotClean = sanitizeSyndicateTag(pilotSyndicateTag)
    if (pilotClean == targetClean) return true
    return getSyndicateInvitedGuests(targetClean).any {
      it.equals(pilotCallsign.trim(), ignoreCase = true)
    }
  }

  fun getSyndicateWarRecord(tag: String = getSyndicateTag()): SyndicateWarRecord {
    val cleanTag = tag.trim().uppercase(Locale.US).ifEmpty { "SOLO" }
    val raw = prefs.getString(KEY_SYNDICATE_WAR_RECORDS_JSON, "{}") ?: "{}"
    try {
      val root = JSONObject(raw)
      val obj = root.optJSONObject(cleanTag)
      if (obj != null) {
        return SyndicateWarRecord.fromJson(cleanTag, obj)
      }
    } catch (_: Exception) {}
    return SyndicateWarRecord(cleanTag, warTrophies = 1000, warWins = 0, warLosses = 0, warStreak = 0)
  }

  fun recordSyndicateWarOutcome(
    tag: String = getSyndicateTag(),
    won: Boolean,
    trophyDelta: Int = if (won) 25 else -15,
  ): SyndicateWarRecord {
    val cleanTag = tag.trim().uppercase(Locale.US).ifEmpty { "SOLO" }
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
   * custom friends, custom Syndicates, Syndicate service history, custom Syndicate channels/guests/crests,
   * Syndicate War Trophy records, and custom modular ship slots (Frame A..E) while resetting Rift Shards to 150.
   * Preserves Sworn Pilot callsigns and all user audio/BPM/Music Forge preferences in userSettingsPrefs.
   */
  fun wipeAllStatsArmadasAndSyndicates(uid: String? = null) {
    val preservedCallsign = if (!isGuestBypassEnabled()) getCallsign(uid) else null
    val preservedChangeCount = getCallsignChangeCount(uid)
    val preservedChosenFlag = hasChosenInitialCallsign(uid)

    val dynamicKeysToRemove =
      prefs.all.keys.filter { key ->
        key.startsWith(KEY_SYNDICATE_CHANNELS_PREFIX) ||
          key.startsWith("syndicate_channels_") ||
          key.startsWith(KEY_SYNDICATE_GUESTS_PREFIX) ||
          key.startsWith("syndicate_guests_") ||
          key.startsWith(KEY_SYNDICATE_CREST_PREFIX) ||
          key.startsWith("syndicate_war_record_") ||
          key.startsWith(KEY_CUSTOM_SLOT_PREFIX) ||
          key.startsWith(KEY_SHIP_SURVIVAL_PREFIX) ||
          key.startsWith("ship_survival_") ||
          key.startsWith(KEY_RECENT_SHIP_LIVES_JSON) ||
          key.startsWith("recent_ship_lives") ||
          key.startsWith("recent_completed_lives") ||
          key.startsWith(KEY_ALL_TIME_PEAK_SNAPSHOT) ||
          key.startsWith(KEY_PVP_PEAK_SNAPSHOT) ||
          key.startsWith(KEY_MATCHES_PLAYED) ||
          key.startsWith(KEY_MATCHES_LOST) ||
          key.startsWith(KEY_CAREER_ASSISTS) ||
          key.startsWith(KEY_CAREER_SHOTS_FIRED) ||
          key.startsWith(KEY_CAREER_SHOTS_HIT) ||
          key.startsWith(KEY_RIFT_CAPTURES) ||
          key.startsWith(KEY_SHIP_RESONANCE_PREFIX)
      }

    val editor = prefs.edit()
    for (k in dynamicKeysToRemove) {
      editor.remove(k)
    }

    // Combat stats (both unscoped and UID-scoped)
    editor
      .putLong(KEY_PVP_KILLS, 0L)
      .putLong(uidKey(KEY_PVP_KILLS, uid), 0L)
      .putLong(KEY_PVP_DEATHS, 0L)
      .putLong(uidKey(KEY_PVP_DEATHS, uid), 0L)
      .putLong(KEY_PVP_BOUNTY_PEAK, 100L)
      .putLong(uidKey(KEY_PVP_BOUNTY_PEAK, uid), 100L)
      .putLong(KEY_ALL_KILLS, 0L)
      .putLong(uidKey(KEY_ALL_KILLS, uid), 0L)
      .putLong(KEY_ALL_DEATHS, 0L)
      .putLong(uidKey(KEY_ALL_DEATHS, uid), 0L)
      .putLong(KEY_ALL_BOUNTY_PEAK, 100L)
      .putLong(uidKey(KEY_ALL_BOUNTY_PEAK, uid), 100L)
      .putLong(KEY_CONVERGENCES_WON, 0L)
      .putLong(uidKey(KEY_CONVERGENCES_WON, uid), 0L)
      .putLong(KEY_HVT_ELIMINATIONS, 0L)
      .putLong(uidKey(KEY_HVT_ELIMINATIONS, uid), 0L)
      .putLong(KEY_GREENS_COLLECTED, 0L)
      .putLong(uidKey(KEY_GREENS_COLLECTED, uid), 0L)
      .putLong(KEY_MATCHES_PLAYED, 0L)
      .putLong(uidKey(KEY_MATCHES_PLAYED, uid), 0L)
      .putLong(KEY_MATCHES_LOST, 0L)
      .putLong(uidKey(KEY_MATCHES_LOST, uid), 0L)
      .putLong(KEY_CAREER_ASSISTS, 0L)
      .putLong(uidKey(KEY_CAREER_ASSISTS, uid), 0L)
      .putLong(KEY_CAREER_SHOTS_FIRED, 0L)
      .putLong(uidKey(KEY_CAREER_SHOTS_FIRED, uid), 0L)
      .putLong(KEY_CAREER_SHOTS_HIT, 0L)
      .putLong(uidKey(KEY_CAREER_SHOTS_HIT, uid), 0L)
      .putLong(KEY_RIFT_CAPTURES, 0L)
      .putLong(uidKey(KEY_RIFT_CAPTURES, uid), 0L)

    // 5 XP sources + progression + Rift Shards (150)
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
      .putInt(KEY_LIVE_RESONANCE, 0)
      .putInt(uidKey(KEY_LIVE_RESONANCE, uid), 0)
      .putInt(KEY_PEAK_RESONANCE, 0)
      .putInt(uidKey(KEY_PEAK_RESONANCE, uid), 0)
      .putBoolean(uidKey(KEY_RESONANCE_MIGRATED, uid), true)
      .putLong(uidKey(KEY_DAILY_DIRECTIVES_UTC_DAY, uid), -1L)
      .putBoolean(uidKey(KEY_DAILY_DIRECTIVE_REROLL_USED, uid), false)
      .putBoolean(uidKey(KEY_DAILY_SWEEP_CLAIMED, uid), false)
    for (slot in 0..2) {
      editor.putInt(uidKey("${KEY_DAILY_DIRECTIVE_PROGRESS_PREFIX}$slot", uid), 0)
      editor.putBoolean(uidKey("${KEY_DAILY_DIRECTIVE_CLAIMED_PREFIX}$slot", uid), false)
      editor.putInt(uidKey("${KEY_DAILY_DIRECTIVE_REROLL_SALT_PREFIX}$slot", uid), 0)
    }

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
      .putString(KEY_SYNDICATE_TAG, "")
      .putString(KEY_SYNDICATE_NAME, "Unaffiliated")
      .putString(KEY_SYNDICATE_WAR_RECORDS_JSON, "{}")
      .putBoolean(KEY_SLOT_B_UNLOCKED, false)
      .putBoolean(uidKey(KEY_SLOT_B_UNLOCKED, uid), false)
      .putBoolean(KEY_SLOT_C_UNLOCKED, false)
      .putBoolean(uidKey(KEY_SLOT_C_UNLOCKED, uid), false)
      .putBoolean(KEY_SLOT_D_UNLOCKED, false)
      .putBoolean(uidKey(KEY_SLOT_D_UNLOCKED, uid), false)
      .putBoolean(KEY_SLOT_E_UNLOCKED, false)
      .putBoolean(uidKey(KEY_SLOT_E_UNLOCKED, uid), false)
      .putString(KEY_ACTIVE_CUSTOM_SLOT_ID, "Frame A")
      .putString(uidKey(KEY_ACTIVE_CUSTOM_SLOT_ID, uid), "Frame A")
      .putBoolean(KEY_SWORN_PRACTICE_TUTORIAL_DONE, false)
      .putBoolean(uidKey(KEY_SWORN_PRACTICE_TUTORIAL_DONE, uid), false)

    if (!preservedCallsign.isNullOrBlank() && preservedCallsign != "COMMANDER") {
      editor.putString(KEY_CALLSIGN, preservedCallsign)
      if (!uid.isNullOrBlank()) {
        editor.putString(uidKey(KEY_CALLSIGN, uid), preservedCallsign)
      }
    }
    editor.putInt(KEY_CALLSIGN_CHANGE_COUNT, preservedChangeCount)
    editor.putBoolean(KEY_INITIAL_CALLSIGN_CHOSEN, preservedChosenFlag)
    if (!uid.isNullOrBlank()) {
      editor.putInt(uidKey(KEY_CALLSIGN_CHANGE_COUNT, uid), preservedChangeCount)
      editor.putBoolean(uidKey(KEY_INITIAL_CALLSIGN_CHOSEN, uid), preservedChosenFlag)
    }
    editor.apply()

    // Reset Custom Modular Ship Slots (Frame A through Frame E) to default state
    for (slotId in com.example.engine.CustomShipLoadoutSlot.ALL_SLOT_IDS) {
      saveCustomShipSlot(com.example.engine.CustomShipLoadoutSlot.defaultSlot(slotId), uid)
    }

    clearHubTapTelemetryHistory()
    userSettingsPrefs.edit().putString(KEY_HUB_TAP_BUFFER, "").apply()
  }

  /**
   * Resets all local pilot progression, combat stats, Syndicate/Armada affiliation, custom friends,
   * and Rift Shards back to a clean Level 1, 0 XP, 0 Kills, 0 Shards, Solo, Unaffiliated state,
   * while preserving the Sworn Pilot's permanent callsign, rename counter, and user audio/BPM/visual settings.
   */
  fun resetAllLocalPilotData(uid: String? = null) {
    val guestBypass = isGuestBypassEnabled()
    val preservedCallsign = if (!guestBypass) getCallsign(uid) else null
    val preservedChangeCount = getCallsignChangeCount(uid)
    val preservedChosenFlag = if (!guestBypass) hasChosenInitialCallsign(uid) else false
    val preservedBootNexus = getBootToNexusHub()
    val preservedMusicEnabled = getMusicEnabled()
    val preservedSfxEnabled = getSfxEnabled()
    val preservedMusicBpm = getMusicBpm()
    val preservedMusicVol = getMusicVolume()
    val preservedSfxVol = getSfxVolume()
    val preservedMusicArch = getMusicArchetypeId()
    val preservedMusicPrompt = getMusicForgePrompt()
    val preservedZoom = getCameraZoom()
    val preservedTextScale = getPlayfieldTextScale()
    val seenBuild = getLastSeenChangelogBuild()

    val allCallsignEntries =
      if (!guestBypass) {
        prefs.all.filter { (k, v) ->
          (k == KEY_CALLSIGN ||
            k.startsWith("${KEY_CALLSIGN}_") ||
            k == KEY_CALLSIGN_CHANGE_COUNT ||
            k.startsWith("${KEY_CALLSIGN_CHANGE_COUNT}_") ||
            k == KEY_INITIAL_CALLSIGN_CHOSEN ||
            k.startsWith("${KEY_INITIAL_CALLSIGN_CHOSEN}_")) && v != null
        }
      } else {
        emptyMap()
      }

    prefs.edit().clear().apply()
    clearHubTapTelemetryHistory()
    userSettingsPrefs.edit().putString(KEY_HUB_TAP_BUFFER, "").apply()
    setLastSeenChangelogBuild(seenBuild)
    setBootToNexusHub(preservedBootNexus)
    setMusicEnabled(preservedMusicEnabled)
    setSfxEnabled(preservedSfxEnabled)
    setMusicBpm(preservedMusicBpm)
    setMusicVolume(preservedMusicVol)
    setSfxVolume(preservedSfxVol)
    setMusicArchetypeId(preservedMusicArch)
    setMusicForgePrompt(preservedMusicPrompt)
    setCameraZoom(preservedZoom)
    setPlayfieldTextScale(preservedTextScale)
    setGuestBypassEnabled(guestBypass)
    getOrCreatePermanentDeviceUid()

    if (!guestBypass) {
      val restoreEditor = prefs.edit()
      for ((k, v) in allCallsignEntries) {
        when (v) {
          is String -> restoreEditor.putString(k, v)
          is Int -> restoreEditor.putInt(k, v)
          is Boolean -> restoreEditor.putBoolean(k, v)
        }
      }
      if (!preservedCallsign.isNullOrBlank() && preservedCallsign != "COMMANDER") {
        restoreEditor.putString(KEY_CALLSIGN, preservedCallsign)
        if (!uid.isNullOrBlank()) {
          restoreEditor.putString(uidKey(KEY_CALLSIGN, uid), preservedCallsign)
        }
      }
      restoreEditor.putInt(KEY_CALLSIGN_CHANGE_COUNT, preservedChangeCount)
      restoreEditor.putBoolean(KEY_INITIAL_CALLSIGN_CHOSEN, preservedChosenFlag)
      if (!uid.isNullOrBlank()) {
        restoreEditor.putInt(uidKey(KEY_CALLSIGN_CHANGE_COUNT, uid), preservedChangeCount)
        restoreEditor.putBoolean(uidKey(KEY_INITIAL_CALLSIGN_CHOSEN, uid), preservedChosenFlag)
      }
      restoreEditor.apply()
    }
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
      9 -> "Eclipse"
      else -> "Rift Frame"
    }

  fun getFavoriteOverwatchRiftKey(): String =
    (userSettingsPrefs.getString("favorite_overwatch_rift_key", "") ?: "").trim()

  fun setFavoriteOverwatchRiftKey(key: String) {
    userSettingsPrefs.edit().putString("favorite_overwatch_rift_key", key.trim()).apply()
  }

  fun hasCompletedSwornPracticeTutorial(uid: String? = null): Boolean {
    if (isGuestBypassEnabled()) return false
    val key = uidKey(KEY_SWORN_PRACTICE_TUTORIAL_DONE, uid)
    return prefs.getBoolean(key, false)
  }

  fun setCompletedSwornPracticeTutorial(completed: Boolean, uid: String? = null) {
    if (isGuestBypassEnabled()) return
    val editor = prefs.edit().putBoolean(KEY_SWORN_PRACTICE_TUTORIAL_DONE, completed)
    if (!uid.isNullOrBlank()) {
      editor.putBoolean(uidKey(KEY_SWORN_PRACTICE_TUTORIAL_DONE, uid), completed)
    }
    editor.apply()
  }

  /**
   * Returns true whenever entry should bypass NEXUS and deploy directly into the solo Practice Rift:
   * - Every [RFT] Drifter session (!isGoogleAuthenticated || isGuestBypassEnabled())
   * - First-time Sworn Pilots (!hasCompletedSwornPracticeTutorial(uid))
   */
  fun shouldAutoEnterPracticeRiftOnEntry(
    isGoogleAuthenticated: Boolean,
    uid: String? = null,
  ): Boolean {
    if (!isGoogleAuthenticated || isGuestBypassEnabled()) return true
    return !hasCompletedSwornPracticeTutorial(uid)
  }

  fun exportCloudProgressionJson(uid: String): String {
    if (uid.isBlank() || isDrifterOrEphemeralUid(uid)) return ""
    val obj = JSONObject()
    obj.put("uid", uid)
    obj.put("xpBuffs", getXpFromBuffs(uid))
    obj.put("xpDamage", getXpFromDamage(uid))
    obj.put("xpAccuracy", getXpFromAccuracy(uid))
    obj.put("xpEarned", getXpFromBountyEarned(uid))
    obj.put("xpKilled", getXpFromBountyKilled(uid))
    obj.put("liveRes", getLiveResonance(uid))
    obj.put("peakRes", getPeakResonance(uid))
    obj.put("shards", getRiftShards(uid))
    obj.put("milestoneLvl", getHighestMilestoneLevelClaimed(uid))
    obj.put("extraSlots", getUnlockedExtraSlots(uid))
    obj.put("shipId", getPreferredShipId(uid))
    obj.put("activeSlot", getActiveCustomSlotId(uid))
    obj.put("pvpKills", getPvpKills(uid))
    obj.put("pvpDeaths", getPvpDeaths(uid))
    obj.put("pvpPeak", getPvpBountyPeak(uid))
    obj.put("allKills", getAllTimeKills(uid))
    obj.put("allDeaths", getAllTimeDeaths(uid))
    obj.put("allPeak", getAllTimeBountyPeak(uid))
    obj.put("wins", getConvergencesWon(uid))
    obj.put("hvts", getHvtEliminations(uid))
    obj.put("greens", getGreensCollected(uid))
    obj.put("matchesPlayed", getMatchesPlayed(uid))
    obj.put("matchesLost", getMatchesLost(uid))
    obj.put("assists", getCareerAssists(uid))
    obj.put("shotsFired", getCareerShotsFired(uid))
    obj.put("shotsHit", getCareerShotsHit(uid))
    obj.put("riftCaptures", getRiftCaptures(uid))
    val shipResObj = JSONObject()
    for (id in 1..10) {
      shipResObj.put(id.toString(), getShipResonance(id, uid))
    }
    obj.put("shipRes", shipResObj)
    val slotsObj = JSONObject()
    for (slotId in com.example.engine.CustomShipLoadoutSlot.ALL_SLOT_IDS) {
      slotsObj.put(slotId, getCustomShipSlot(slotId, uid).toJsonString())
    }
    obj.put("slots", slotsObj)
    obj.put("shipSurvival", exportShipSurvivalJsonObject(uid))
    obj.put("updatedAtMs", System.currentTimeMillis())
    return obj.toString()
  }

  fun mergeCloudProgressionJson(uid: String, rawJson: String): Boolean {
    if (uid.isBlank() || isDrifterOrEphemeralUid(uid) || rawJson.isBlank()) return false
    return try {
      val obj = JSONObject(rawJson)
      val mergedXpBuffs = kotlin.math.max(getXpFromBuffs(uid), obj.optLong("xpBuffs", 0L))
      val mergedXpDamage = kotlin.math.max(getXpFromDamage(uid), obj.optLong("xpDamage", 0L))
      val mergedXpAcc = kotlin.math.max(getXpFromAccuracy(uid), obj.optLong("xpAccuracy", 0L))
      val mergedXpEarned = kotlin.math.max(getXpFromBountyEarned(uid), obj.optLong("xpEarned", 0L))
      val mergedXpKilled = kotlin.math.max(getXpFromBountyKilled(uid), obj.optLong("xpKilled", 0L))
      val mergedShards = kotlin.math.max(getRiftShards(uid), obj.optLong("shards", 0L))
      val mergedMilestone = kotlin.math.max(getHighestMilestoneLevelClaimed(uid), obj.optInt("milestoneLvl", 1))
      savePilotProgression(
        uid = uid,
        xpFromBuffs = mergedXpBuffs,
        xpFromDamage = mergedXpDamage,
        xpFromAccuracy = mergedXpAcc,
        xpFromBountyEarned = mergedXpEarned,
        xpFromBountyKilled = mergedXpKilled,
        riftShards = mergedShards,
        highestMilestoneLevelClaimed = mergedMilestone,
      )
      setUnlockedExtraSlots(kotlin.math.max(getUnlockedExtraSlots(uid), obj.optInt("extraSlots", 0)), uid)

      val shipResObj = obj.optJSONObject("shipRes")
      var mergedTotalRes = 0
      if (shipResObj != null) {
        val allowLegacy = canUseLegacyUnscopedFallback(uid)
        val ed = prefs.edit()
        for (id in 1..10) {
          val cloudR = shipResObj.optInt(id.toString(), 0).coerceAtLeast(0)
          val localR = getShipResonanceRaw(id, uid)
          val bestR = kotlin.math.max(localR, cloudR)
          mergedTotalRes += bestR
          val base = shipResonanceBaseKey(id)
          ed.putInt(uidKey(base, uid), bestR)
          if (allowLegacy) ed.putInt(base, bestR)
        }
        ed.putBoolean(uidKey(KEY_SHIP_RESONANCE_MIGRATED, uid), true)
        ed.putBoolean(uidKey(KEY_RESONANCE_MIGRATED, uid), true)
        ed.apply()
      }
      val cloudLive = obj.optInt("liveRes", 0).coerceAtLeast(0)
      val cloudPeak = obj.optInt("peakRes", 0).coerceAtLeast(0)
      val bestLive = kotlin.math.max(getLiveResonance(uid), kotlin.math.max(mergedTotalRes, cloudLive))
      if (bestLive > getLiveResonance(uid)) {
        setLiveResonance(bestLive, uid)
      }
      val bestPeak = kotlin.math.max(getPeakResonance(uid), kotlin.math.max(bestLive, cloudPeak))
      val edPeak = prefs.edit().putInt(uidKey(KEY_PEAK_RESONANCE, uid), bestPeak)
      if (canUseLegacyUnscopedFallback(uid)) {
        edPeak.putInt(KEY_PEAK_RESONANCE, bestPeak)
      }
      edPeak.apply()

      saveCombatStats(
        pvpKills = kotlin.math.max(getPvpKills(uid), obj.optLong("pvpKills", 0L)),
        pvpDeaths = kotlin.math.max(getPvpDeaths(uid), obj.optLong("pvpDeaths", 0L)),
        pvpBountyPeak = kotlin.math.max(getPvpBountyPeak(uid), obj.optLong("pvpPeak", 100L)),
        allTimeKills = kotlin.math.max(getAllTimeKills(uid), obj.optLong("allKills", 0L)),
        allTimeDeaths = kotlin.math.max(getAllTimeDeaths(uid), obj.optLong("allDeaths", 0L)),
        allTimeBountyPeak = kotlin.math.max(getAllTimeBountyPeak(uid), obj.optLong("allPeak", 100L)),
        convergencesWon = kotlin.math.max(getConvergencesWon(uid), obj.optLong("wins", 0L)),
        hvtEliminations = kotlin.math.max(getHvtEliminations(uid), obj.optLong("hvts", 0L)),
        greensCollected = kotlin.math.max(getGreensCollected(uid), obj.optLong("greens", 0L)),
        uid = uid,
      )
      val mergedPlayed = kotlin.math.max(getMatchesPlayed(uid), obj.optLong("matchesPlayed", 0L))
      val mergedLost = kotlin.math.max(getMatchesLost(uid), obj.optLong("matchesLost", 0L))
      val mergedAssists = kotlin.math.max(getCareerAssists(uid), obj.optLong("assists", 0L))
      val mergedFired = kotlin.math.max(getCareerShotsFired(uid), obj.optLong("shotsFired", 0L))
      val mergedHit = kotlin.math.max(getCareerShotsHit(uid), obj.optLong("shotsHit", 0L)).coerceAtMost(mergedFired)
      val mergedCaps = kotlin.math.max(getRiftCaptures(uid), obj.optLong("riftCaptures", 0L))
      prefs
        .edit()
        .putLong(uidKey(KEY_MATCHES_PLAYED, uid), kotlin.math.max(mergedPlayed, getConvergencesWon(uid) + mergedLost))
        .putLong(uidKey(KEY_MATCHES_LOST, uid), mergedLost)
        .putLong(uidKey(KEY_CAREER_ASSISTS, uid), mergedAssists)
        .putLong(uidKey(KEY_CAREER_SHOTS_FIRED, uid), mergedFired)
        .putLong(uidKey(KEY_CAREER_SHOTS_HIT, uid), mergedHit)
        .putLong(uidKey(KEY_RIFT_CAPTURES, uid), mergedCaps)
        .apply()

      val cloudShipId = obj.optInt("shipId", 0)
      if (cloudShipId in 1..10 && !prefs.contains(uidKey(KEY_SHIP_ID, uid))) {
        setPreferredShipId(cloudShipId, uid)
      }
      val slotsObj = obj.optJSONObject("slots")
      if (slotsObj != null) {
        for (slotId in com.example.engine.CustomShipLoadoutSlot.ALL_SLOT_IDS) {
          val baseKey = "${KEY_CUSTOM_SLOT_PREFIX}_$slotId"
          if (!prefs.contains(uidKey(baseKey, uid))) {
            val slotJson = slotsObj.optString(slotId, "")
            if (slotJson.isNotBlank()) {
              saveCustomShipSlot(com.example.engine.CustomShipLoadoutSlot.fromJsonString(slotJson, slotId), uid)
            }
          }
        }
      }
      obj.optJSONObject("shipSurvival")?.let { survObj ->
        mergeShipSurvivalJsonObject(uid, survObj)
      }
      true
    } catch (_: Exception) {
      false
    }
  }

  private fun survivalKey(suffix: String, shipId: Int, uid: String?): String =
    uidKey("${KEY_SHIP_SURVIVAL_PREFIX}${shipId.coerceIn(1, 10)}_$suffix", uid)

  fun recordCompletedShipLife(record: CompletedShipLifeRecord, uid: String? = null) {
    val cleanDuration = record.aliveDurationSec.coerceIn(0f, 86_400f)
    if (cleanDuration < 0.25f) return
    val sid = record.shipId.coerceIn(1, 10)
    val ed = prefs.edit()
    if (record.isLocalPilot) {
      val prevLives = prefs.getInt(survivalKey("pilot_lives", sid, uid), 0).coerceAtLeast(0)
      val prevTotal = prefs.getFloat(survivalKey("pilot_total", sid, uid), 0f).coerceAtLeast(0f)
      val prevMax = prefs.getFloat(survivalKey("pilot_max", sid, uid), 0f).coerceAtLeast(0f)
      val prevMin = prefs.getFloat(survivalKey("pilot_min", sid, uid), 0f).coerceAtLeast(0f)
      val nextLives = prevLives + 1
      val nextTotal = prevTotal + cleanDuration
      val nextMax = kotlin.math.max(prevMax, cleanDuration)
      val nextMin = if (prevLives == 0 || prevMin <= 0f) cleanDuration else kotlin.math.min(prevMin, cleanDuration)
      ed.putInt(survivalKey("pilot_lives", sid, uid), nextLives)
      ed.putFloat(survivalKey("pilot_total", sid, uid), nextTotal)
      ed.putFloat(survivalKey("pilot_last", sid, uid), cleanDuration)
      ed.putFloat(survivalKey("pilot_max", sid, uid), nextMax)
      ed.putFloat(survivalKey("pilot_min", sid, uid), nextMin)

      val overallKeyPrefix = uidKey("${KEY_SHIP_SURVIVAL_PREFIX}overall", uid)
      val prevOverallLives = prefs.getInt("${overallKeyPrefix}_lives", 0).coerceAtLeast(0)
      val prevOverallTotal = prefs.getFloat("${overallKeyPrefix}_total", 0f).coerceAtLeast(0f)
      val prevOverallMax = prefs.getFloat("${overallKeyPrefix}_max", 0f).coerceAtLeast(0f)
      ed.putInt("${overallKeyPrefix}_lives", prevOverallLives + 1)
      ed.putFloat("${overallKeyPrefix}_total", prevOverallTotal + cleanDuration)
      ed.putFloat("${overallKeyPrefix}_last", cleanDuration)
      ed.putFloat("${overallKeyPrefix}_max", kotlin.math.max(prevOverallMax, cleanDuration))
    } else if (!record.isHuman) {
      val prevBotLives = prefs.getInt(survivalKey("bot_lives", sid, uid), 0).coerceAtLeast(0)
      val prevBotTotal = prefs.getFloat(survivalKey("bot_total", sid, uid), 0f).coerceAtLeast(0f)
      val prevBotMax = prefs.getFloat(survivalKey("bot_max", sid, uid), 0f).coerceAtLeast(0f)
      ed.putInt(survivalKey("bot_lives", sid, uid), prevBotLives + 1)
      ed.putFloat(survivalKey("bot_total", sid, uid), prevBotTotal + cleanDuration)
      ed.putFloat(survivalKey("bot_last", sid, uid), cleanDuration)
      ed.putFloat(survivalKey("bot_max", sid, uid), kotlin.math.max(prevBotMax, cleanDuration))
    }

    val recentKey = uidKey(KEY_RECENT_SHIP_LIVES_JSON, uid)
    val existingRecent = getRecentCompletedShipLives(uid).toMutableList()
    existingRecent.add(0, record.copy(shipId = sid, shipName = cleanShipNameForId(sid), aliveDurationSec = cleanDuration))
    val trimmed = existingRecent.take(60)
    val arr = JSONArray()
    trimmed.forEach { arr.put(it.toJson()) }
    ed.putString(recentKey, arr.toString())
    ed.apply()
  }

  fun getRecentCompletedShipLives(uid: String? = null): List<CompletedShipLifeRecord> {
    val raw = prefs.getString(uidKey(KEY_RECENT_SHIP_LIVES_JSON, uid), "[]") ?: "[]"
    return try {
      val arr = JSONArray(raw)
      buildList {
        for (i in 0 until arr.length()) {
          val obj = arr.optJSONObject(i) ?: continue
          add(CompletedShipLifeRecord.fromJson(obj))
        }
      }
    } catch (_: Exception) {
      emptyList()
    }
  }

  fun getShipSurvivalEntry(shipId: Int, uid: String? = null): ShipSurvivalTelemetryEntry {
    val sid = shipId.coerceIn(1, 10)
    val pLives = prefs.getInt(survivalKey("pilot_lives", sid, uid), 0).coerceAtLeast(0)
    val pTotal = prefs.getFloat(survivalKey("pilot_total", sid, uid), 0f).coerceAtLeast(0f)
    val pLast = prefs.getFloat(survivalKey("pilot_last", sid, uid), 0f).coerceAtLeast(0f)
    val pMax = prefs.getFloat(survivalKey("pilot_max", sid, uid), 0f).coerceAtLeast(0f)
    val pMin = prefs.getFloat(survivalKey("pilot_min", sid, uid), 0f).coerceAtLeast(0f)
    val bLives = prefs.getInt(survivalKey("bot_lives", sid, uid), 0).coerceAtLeast(0)
    val bTotal = prefs.getFloat(survivalKey("bot_total", sid, uid), 0f).coerceAtLeast(0f)
    val bLast = prefs.getFloat(survivalKey("bot_last", sid, uid), 0f).coerceAtLeast(0f)
    val bMax = prefs.getFloat(survivalKey("bot_max", sid, uid), 0f).coerceAtLeast(0f)
    return ShipSurvivalTelemetryEntry(
      shipId = sid,
      shipName = cleanShipNameForId(sid),
      completedLives = pLives,
      totalAliveSec = pTotal,
      avgSurvivalSec = if (pLives > 0) pTotal / pLives.toFloat() else 0f,
      lastSurvivalSec = pLast,
      maxSurvivalSec = pMax,
      minSurvivalSec = pMin,
      botCompletedLives = bLives,
      botTotalAliveSec = bTotal,
      botAvgSurvivalSec = if (bLives > 0) bTotal / bLives.toFloat() else 0f,
      botLastSurvivalSec = bLast,
      botMaxSurvivalSec = bMax,
    )
  }

  fun getPilotSurvivalTelemetrySummary(uid: String? = null): PilotSurvivalTelemetrySummary {
    val entries = (1..10).map { getShipSurvivalEntry(it, uid) }
    val pLives = entries.sumOf { it.completedLives }
    val pTotal = entries.map { it.totalAliveSec }.sum()
    val overallPrefix = uidKey("${KEY_SHIP_SURVIVAL_PREFIX}overall", uid)
    val pLast = prefs.getFloat("${overallPrefix}_last", entries.maxOfOrNull { it.lastSurvivalSec } ?: 0f).coerceAtLeast(0f)
    val pMax = kotlin.math.max(
      prefs.getFloat("${overallPrefix}_max", 0f).coerceAtLeast(0f),
      entries.maxOfOrNull { it.maxSurvivalSec } ?: 0f,
    )
    val bLives = entries.sumOf { it.botCompletedLives }
    val bTotal = entries.map { it.botTotalAliveSec }.sum()
    val bMax = entries.maxOfOrNull { it.botMaxSurvivalSec } ?: 0f
    return PilotSurvivalTelemetrySummary(
      pilotCompletedLives = pLives,
      pilotTotalAliveSec = pTotal,
      pilotAvgSurvivalSec = if (pLives > 0) pTotal / pLives.toFloat() else 0f,
      pilotLastSurvivalSec = pLast,
      pilotMaxSurvivalSec = pMax,
      botCompletedLives = bLives,
      botTotalAliveSec = bTotal,
      botAvgSurvivalSec = if (bLives > 0) bTotal / bLives.toFloat() else 0f,
      botMaxSurvivalSec = bMax,
      perShip = entries,
      recentLives = getRecentCompletedShipLives(uid),
    )
  }

  private fun exportShipSurvivalJsonObject(uid: String): JSONObject {
    val summary = getPilotSurvivalTelemetrySummary(uid)
    val root = JSONObject()
    root.put("pilotLives", summary.pilotCompletedLives)
    root.put("pilotTotalSec", summary.pilotTotalAliveSec.toDouble())
    root.put("pilotAvgSec", summary.pilotAvgSurvivalSec.toDouble())
    root.put("pilotLastSec", summary.pilotLastSurvivalSec.toDouble())
    root.put("pilotMaxSec", summary.pilotMaxSurvivalSec.toDouble())
    root.put("botLives", summary.botCompletedLives)
    root.put("botTotalSec", summary.botTotalAliveSec.toDouble())
    root.put("botAvgSec", summary.botAvgSurvivalSec.toDouble())
    val shipsObj = JSONObject()
    for (entry in summary.perShip) {
      if (entry.completedLives > 0 || entry.botCompletedLives > 0) {
        shipsObj.put(
          entry.shipId.toString(),
          JSONObject().apply {
            put("pLives", entry.completedLives)
            put("pTotal", entry.totalAliveSec.toDouble())
            put("pLast", entry.lastSurvivalSec.toDouble())
            put("pMax", entry.maxSurvivalSec.toDouble())
            put("pMin", entry.minSurvivalSec.toDouble())
            put("bLives", entry.botCompletedLives)
            put("bTotal", entry.botTotalAliveSec.toDouble())
            put("bMax", entry.botMaxSurvivalSec.toDouble())
          },
        )
      }
    }
    root.put("ships", shipsObj)
    return root
  }

  private fun mergeShipSurvivalJsonObject(uid: String, survObj: JSONObject) {
    val shipsObj = survObj.optJSONObject("ships") ?: return
    val ed = prefs.edit()
    var totalPilotLives = 0
    var totalPilotSec = 0f
    var bestPilotMax = 0f
    for (sid in 1..10) {
      val sObj = shipsObj.optJSONObject(sid.toString())
      val cur = getShipSurvivalEntry(sid, uid)
      if (sObj != null) {
        val cLives = sObj.optInt("pLives", 0).coerceAtLeast(0)
        val cTotal = sObj.optDouble("pTotal", 0.0).toFloat().coerceAtLeast(0f)
        val cLast = sObj.optDouble("pLast", 0.0).toFloat().coerceAtLeast(0f)
        val cMax = sObj.optDouble("pMax", 0.0).toFloat().coerceAtLeast(0f)
        val cMin = sObj.optDouble("pMin", 0.0).toFloat().coerceAtLeast(0f)
        if (cLives > cur.completedLives) {
          ed.putInt(survivalKey("pilot_lives", sid, uid), cLives)
          ed.putFloat(survivalKey("pilot_total", sid, uid), kotlin.math.max(cur.totalAliveSec, cTotal))
          if (cur.lastSurvivalSec <= 0f && cLast > 0f) {
            ed.putFloat(survivalKey("pilot_last", sid, uid), cLast)
          }
        }
        val mergedMax = kotlin.math.max(cur.maxSurvivalSec, cMax)
        if (mergedMax > 0f) ed.putFloat(survivalKey("pilot_max", sid, uid), mergedMax)
        val mergedMin =
          when {
            cur.minSurvivalSec > 0f && cMin > 0f -> kotlin.math.min(cur.minSurvivalSec, cMin)
            cur.minSurvivalSec > 0f -> cur.minSurvivalSec
            else -> cMin
          }
        if (mergedMin > 0f) ed.putFloat(survivalKey("pilot_min", sid, uid), mergedMin)

        val bLives = sObj.optInt("bLives", 0).coerceAtLeast(0)
        val bTotal = sObj.optDouble("bTotal", 0.0).toFloat().coerceAtLeast(0f)
        val bMax = sObj.optDouble("bMax", 0.0).toFloat().coerceAtLeast(0f)
        if (bLives > cur.botCompletedLives) {
          ed.putInt(survivalKey("bot_lives", sid, uid), bLives)
          ed.putFloat(survivalKey("bot_total", sid, uid), kotlin.math.max(cur.botTotalAliveSec, bTotal))
        }
        if (bMax > cur.botMaxSurvivalSec) {
          ed.putFloat(survivalKey("bot_max", sid, uid), bMax)
        }
        totalPilotLives += kotlin.math.max(cur.completedLives, cLives)
        totalPilotSec += kotlin.math.max(cur.totalAliveSec, cTotal)
        bestPilotMax = kotlin.math.max(bestPilotMax, mergedMax)
      } else {
        totalPilotLives += cur.completedLives
        totalPilotSec += cur.totalAliveSec
        bestPilotMax = kotlin.math.max(bestPilotMax, cur.maxSurvivalSec)
      }
    }
    val overallPrefix = uidKey("${KEY_SHIP_SURVIVAL_PREFIX}overall", uid)
    if (totalPilotLives > prefs.getInt("${overallPrefix}_lives", 0)) {
      ed.putInt("${overallPrefix}_lives", totalPilotLives)
      ed.putFloat("${overallPrefix}_total", totalPilotSec)
    }
    if (bestPilotMax > prefs.getFloat("${overallPrefix}_max", 0f)) {
      ed.putFloat("${overallPrefix}_max", bestPilotMax)
    }
    ed.apply()
  }

  fun exportSurvivalTelemetryCsv(
    callsign: String,
    liveResonance: Int,
    uid: String? = null,
  ): String {
    val summary = getPilotSurvivalTelemetrySummary(uid)
    val sb = StringBuilder()
    sb.appendLine("# RIFT SHIP SURVIVAL & COMBAT TELEMETRY EXPORT (BUILD #103)")
    sb.appendLine("# Pilot,${callsign.replace(",", " ")},LiveResonance,$liveResonance,PilotLives,${summary.pilotCompletedLives},PilotAvgSec,${String.format(java.util.Locale.US, "%.2f", summary.pilotAvgSurvivalSec)},BotLives,${summary.botCompletedLives},BotAvgSec,${String.format(java.util.Locale.US, "%.2f", summary.botAvgSurvivalSec)}")
    sb.appendLine()
    sb.appendLine("Section,ShipId,ShipName,PilotLives,PilotAvgSec,PilotLastSec,PilotMaxSec,PilotMinSec,PilotTotalSec,BotLives,BotAvgSec,BotMaxSec,ShipResonance")
    for (entry in summary.perShip) {
      val res = getShipResonance(entry.shipId, uid)
      sb.appendLine(
        java.lang.String.format(
          java.util.Locale.US,
          "PER_SHIP,%d,%s,%d,%.2f,%.2f,%.2f,%.2f,%.2f,%d,%.2f,%.2f,%d",
          entry.shipId,
          entry.shipName,
          entry.completedLives,
          entry.avgSurvivalSec,
          entry.lastSurvivalSec,
          entry.maxSurvivalSec,
          entry.minSurvivalSec,
          entry.totalAliveSec,
          entry.botCompletedLives,
          entry.botAvgSurvivalSec,
          entry.botMaxSurvivalSec,
          res,
        )
      )
    }
    sb.appendLine()
    sb.appendLine("RecentLifeTimestampMs,Actor,Callsign,ShipId,ShipName,Mode,CustomGame,DamageTakenScalePct,WeaponFluxCostScalePct,AliveSec,EndReason,Elims,DamageDealt,DamageTaken")
    for (rec in summary.recentLives) {
      val actor = when {
        rec.isLocalPilot -> "PILOT"
        rec.isHuman -> "REMOTE_PILOT"
        else -> "AI_BOT"
      }
      sb.appendLine(
        java.lang.String.format(
          java.util.Locale.US,
          "%d,%s,%s,%d,%s,%s,%s,%d,%d,%.2f,%s,%d,%.1f,%.1f",
          rec.timestampMs,
          actor,
          rec.callsign.replace(",", " "),
          rec.shipId,
          rec.shipName,
          rec.modeId,
          if (rec.isCustomGame) "YES" else "NO",
          rec.damageTakenScalePct,
          rec.weaponFluxCostScalePct,
          rec.aliveDurationSec,
          rec.endReason,
          rec.killsInLife,
          rec.damageDealtInLife,
          rec.damageTakenInLife,
        )
      )
    }
    val hubTaps = getHubTapTelemetryHistory()
    sb.appendLine()
    sb.appendLine("HubTapTimestampMs,TapXNorm,TapYNorm,ActiveHubView,TargetControlTag")
    for (tap in hubTaps) {
      sb.appendLine(
        java.lang.String.format(
          java.util.Locale.US,
          "%d,%.3f,%.3f,%s,%s",
          tap.timestampMs,
          tap.tapXNorm,
          tap.tapYNorm,
          tap.activeHubView.replace(",", " "),
          tap.targetControlTag.replace(",", " "),
        )
      )
    }
    return sb.toString()
  }

  fun exportSurvivalTelemetryJson(
    callsign: String,
    liveResonance: Int,
    uid: String? = null,
  ): String {
    val summary = getPilotSurvivalTelemetrySummary(uid)
    val career = getPilotCareerStatsSummary(uid)
    val hubTaps = getHubTapTelemetryHistory()
    val root = JSONObject()
    root.put("schemaVersion", 115)
    root.put("exportedAtMs", System.currentTimeMillis())
    root.put("callsign", callsign)
    root.put("liveResonance", liveResonance)
    root.put("pilotLevel", career.pilotLevel)
    root.put("totalXp", career.totalXp)
    root.put("combatXp", career.combatXp)
    root.put("survivalXp", career.survivalXp)
    root.put("objectiveXp", career.objectiveXp)
    root.put("supportCratesXp", career.supportCratesXp)
    root.put("dailyDirectivesXp", career.dailyDirectivesXp)
    root.put("matchesPlayed", career.matchesPlayed)
    root.put("wins", career.wins)
    root.put("losses", career.losses)
    root.put("winPercent", career.winPercent)
    root.put("eliminations", career.eliminations)
    root.put("deaths", career.deaths)
    root.put("assists", career.assists)
    root.put("accuracyPercent", career.accuracyPercent)
    root.put("cratePickups", career.cratePickups)
    root.put("riftCaptures", career.riftCaptures)
    root.put("pilotCompletedLives", summary.pilotCompletedLives)
    root.put("pilotTotalAliveSec", summary.pilotTotalAliveSec.toDouble())
    root.put("pilotAvgSurvivalSec", summary.pilotAvgSurvivalSec.toDouble())
    root.put("pilotLastSurvivalSec", summary.pilotLastSurvivalSec.toDouble())
    root.put("pilotMaxSurvivalSec", summary.pilotMaxSurvivalSec.toDouble())
    root.put("botCompletedLives", summary.botCompletedLives)
    root.put("botTotalAliveSec", summary.botTotalAliveSec.toDouble())
    root.put("botAvgSurvivalSec", summary.botAvgSurvivalSec.toDouble())
    root.put("botMaxSurvivalSec", summary.botMaxSurvivalSec.toDouble())
    val shipsArr = JSONArray()
    for (entry in summary.perShip) {
      shipsArr.put(
        JSONObject().apply {
          put("shipId", entry.shipId)
          put("shipName", entry.shipName)
          put("shipResonance", getShipResonance(entry.shipId, uid))
          put("completedLives", entry.completedLives)
          put("avgSurvivalSec", entry.avgSurvivalSec.toDouble())
          put("lastSurvivalSec", entry.lastSurvivalSec.toDouble())
          put("maxSurvivalSec", entry.maxSurvivalSec.toDouble())
          put("minSurvivalSec", entry.minSurvivalSec.toDouble())
          put("totalAliveSec", entry.totalAliveSec.toDouble())
          put("botCompletedLives", entry.botCompletedLives)
          put("botAvgSurvivalSec", entry.botAvgSurvivalSec.toDouble())
          put("botMaxSurvivalSec", entry.botMaxSurvivalSec.toDouble())
        }
      )
    }
    root.put("perShip", shipsArr)
    val recentArr = JSONArray()
    summary.recentLives.forEach { recentArr.put(it.toJson()) }
    root.put("recentCompletedLives", recentArr)
    val tapsArr = JSONArray()
    hubTaps.forEach { tapsArr.put(it.toJson()) }
    root.put("hubTapTelemetry", tapsArr)
    return root.toString(2)
  }

  fun exportSurvivalTelemetryHtmlSpreadsheet(
    callsign: String,
    liveResonance: Int,
    uid: String? = null,
  ): String {
    val summary = getPilotSurvivalTelemetrySummary(uid)
    val career = getPilotCareerStatsSummary(uid)
    val hubTaps = getHubTapTelemetryHistory()
    fun esc(raw: String): String =
      raw
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
    val fmt = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US)
    val nowStr = fmt.format(java.util.Date())
    val sb = StringBuilder()
    sb.appendLine("<!DOCTYPE html><html lang=\"en\"><head><meta charset=\"UTF-8\"/>")
    sb.appendLine("<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\"/>")
    sb.appendLine("<title>Rift Full Telemetry Sheet - ${esc(callsign)}</title>")
    sb.appendLine("<style>")
    sb.appendLine("body{background:#02050B;color:#E2E8F0;font-family:ui-monospace,SFMono-Regular,Menlo,Consolas,monospace;margin:0;padding:16px;}")
    sb.appendLine("h1{color:#00F5D4;font-size:17px;margin:0 0 4px 0;letter-spacing:1px;}")
    sb.appendLine("h2{color:#38BDF8;font-size:13px;margin:16px 0 8px 0;letter-spacing:0.8px;}")
    sb.appendLine(".sub{color:#94A3B8;font-size:11px;margin-bottom:12px;}")
    sb.appendLine(".kpis{display:grid;grid-template-columns:repeat(auto-fit,minmax(145px,1fr));gap:8px;margin-bottom:14px;}")
    sb.appendLine(".kpi{background:#091528;border:1px solid #1E3A5F;border-radius:8px;padding:9px;}")
    sb.appendLine(".kpi .lbl{color:#94A3B8;font-size:10px;}")
    sb.appendLine(".kpi .val{color:#FFD700;font-size:15px;font-weight:800;margin-top:3px;}")
    sb.appendLine(".filters{display:flex;flex-wrap:wrap;gap:8px;align-items:center;background:#081426;border:1px solid #284B73;border-radius:8px;padding:9px 10px;margin-bottom:14px;}")
    sb.appendLine(".filters label{color:#94A3B8;font-size:10px;font-weight:700;display:flex;align-items:center;gap:5px;}")
    sb.appendLine(".filters select,.filters input,.filters button{background:#040A16;color:#E2E8F0;border:1px solid #38BDF8;border-radius:6px;padding:5px 8px;font-family:inherit;font-size:11px;}")
    sb.appendLine(".filters button{background:#0B253A;color:#00F5D4;font-weight:800;cursor:pointer;}")
    sb.appendLine(".count{color:#00F5D4;font-size:11px;font-weight:700;margin-left:auto;}")
    sb.appendLine("table{width:100%;border-collapse:collapse;background:#060D1A;border:1px solid #1E3A5F;font-size:11px;}")
    sb.appendLine("th{background:#0D1F38;color:#00F5D4;text-align:left;padding:7px 8px;border-bottom:1px solid #38BDF8;position:sticky;top:0;}")
    sb.appendLine("td{padding:6px 8px;border-bottom:1px solid #13233A;color:#E2E8F0;}")
    sb.appendLine("tr:nth-child(even) td{background:#081224;}")
    sb.appendLine(".num{text-align:right;font-variant-numeric:tabular-nums;}")
    sb.appendLine(".warn{color:#FB7185;font-weight:700;}")
    sb.appendLine(".good{color:#34D399;font-weight:700;}")
    sb.appendLine("</style></head><body>")
    sb.appendLine("<h1>RIFT FULL PILOT, SURVIVAL &amp; HUB TAP TELEMETRY SHEET</h1>")
    sb.appendLine("<div class=\"sub\">PILOT: ${esc(callsign)} &bull; LEVEL ${career.pilotLevel} (${career.totalXp} XP) &bull; LIVE RESONANCE: $liveResonance &bull; GENERATED: ${esc(nowStr)}</div>")
    sb.appendLine("<div class=\"kpis\">")
    sb.appendLine("<div class=\"kpi\"><div class=\"lbl\">PILOT AVG SURVIVAL</div><div class=\"val\">${formatSurvivalDurationSec(summary.pilotAvgSurvivalSec)}</div></div>")
    sb.appendLine("<div class=\"kpi\"><div class=\"lbl\">LATEST COMPLETED LIFE</div><div class=\"val\">${formatSurvivalDurationSec(summary.pilotLastSurvivalSec)}</div></div>")
    sb.appendLine("<div class=\"kpi\"><div class=\"lbl\">LONGEST PILOT LIFE</div><div class=\"val\">${formatSurvivalDurationSec(summary.pilotMaxSurvivalSec)}</div></div>")
    sb.appendLine("<div class=\"kpi\"><div class=\"lbl\">MATCHES / WIN %</div><div class=\"val\">${career.matchesPlayed} (${career.winPercent}%)</div></div>")
    sb.appendLine("<div class=\"kpi\"><div class=\"lbl\">ELIMS / DEATHS / ASSISTS</div><div class=\"val\">${career.eliminations} / ${career.deaths} / ${career.assists}</div></div>")
    sb.appendLine("<div class=\"kpi\"><div class=\"lbl\">ACCURACY / CRATES / CAPS</div><div class=\"val\">${career.accuracyPercent}% &bull; ${career.cratePickups} &bull; ${career.riftCaptures}</div></div>")
    sb.appendLine("</div>")
    sb.appendLine("<div class=\"filters\">")
    sb.appendLine("<label>HULL <select id=\"fHull\" onchange=\"applyFilters()\"><option value=\"ALL\">ALL HULLS</option>")
    for (entry in summary.perShip) {
      sb.appendLine("<option value=\"${esc(entry.shipName.uppercase(java.util.Locale.US))}\">${esc(entry.shipName)}</option>")
    }
    sb.appendLine("</select></label>")
    sb.appendLine("<label>ACTOR <select id=\"fActor\" onchange=\"applyFilters()\"><option value=\"ALL\">ALL ACTORS</option><option value=\"PILOT\">LOCAL PILOT</option><option value=\"BOT\">AI BOTS</option><option value=\"REMOTE\">REMOTE PILOTS</option></select></label>")
    sb.appendLine("<label>SESSION <select id=\"fSession\" onchange=\"applyFilters()\"><option value=\"ALL\">ALL SESSIONS</option><option value=\"PUBLIC\">PUBLIC RIFTS</option><option value=\"CUSTOM\">CUSTOM SANDBOX</option></select></label>")
    sb.appendLine("<label>HUB VIEW <select id=\"fHubView\" onchange=\"applyFilters()\"><option value=\"ALL\">ALL HUB VIEWS</option>")
    hubTaps.map { it.activeHubView.uppercase(java.util.Locale.US) }.distinct().sorted().forEach { v ->
      sb.appendLine("<option value=\"${esc(v)}\">${esc(v)}</option>")
    }
    sb.appendLine("</select></label>")
    sb.appendLine("<label>SEARCH <input id=\"fSearch\" type=\"text\" placeholder=\"Mode, callsign, control tag...\" oninput=\"applyFilters()\"/></label>")
    sb.appendLine("<button type=\"button\" onclick=\"resetFilters()\">RESET</button>")
    sb.appendLine("<span id=\"fCount\" class=\"count\">SHOWING ${summary.perShip.size} HULLS &bull; ${summary.recentLives.size} LIVES &bull; ${hubTaps.size} HUB TAPS</span>")
    sb.appendLine("</div>")
    sb.appendLine("<h2>PER-SHIP SURVIVAL BREAKDOWN (10 HULLS)</h2>")
    sb.appendLine("<table id=\"hullTable\"><thead><tr><th>HULL</th><th class=\"num\">RESONANCE</th><th class=\"num\">PILOT LIVES</th><th class=\"num\">AVG ALIVE</th><th class=\"num\">LAST LIFE</th><th class=\"num\">MAX LIFE</th><th class=\"num\">MIN LIFE</th><th class=\"num\">BOT LIVES</th><th class=\"num\">BOT AVG</th></tr></thead><tbody>")
    for (entry in summary.perShip) {
      val res = getShipResonance(entry.shipId, uid)
      val avgClass = when {
        entry.completedLives == 0 -> ""
        entry.avgSurvivalSec < 12f -> " warn"
        entry.avgSurvivalSec >= 30f -> " good"
        else -> ""
      }
      val hullKey = esc(entry.shipName.uppercase(java.util.Locale.US))
      sb.appendLine(
        "<tr data-hull=\"$hullKey\"><td>${esc(entry.shipName)}</td>" +
          "<td class=\"num\">$res</td>" +
          "<td class=\"num\">${entry.completedLives}</td>" +
          "<td class=\"num$avgClass\">${formatSurvivalDurationSec(entry.avgSurvivalSec)}</td>" +
          "<td class=\"num\">${formatSurvivalDurationSec(entry.lastSurvivalSec)}</td>" +
          "<td class=\"num\">${formatSurvivalDurationSec(entry.maxSurvivalSec)}</td>" +
          "<td class=\"num\">${formatSurvivalDurationSec(entry.minSurvivalSec)}</td>" +
          "<td class=\"num\">${entry.botCompletedLives}</td>" +
          "<td class=\"num\">${formatSurvivalDurationSec(entry.botAvgSurvivalSec)}</td></tr>"
      )
    }
    sb.appendLine("</tbody></table>")
    sb.appendLine("<h2>RECENT COMPLETED LIVES LOG (LATEST ${summary.recentLives.size})</h2>")
    sb.appendLine("<table id=\"lifeTable\"><thead><tr><th>TIME</th><th>ACTOR</th><th>HULL</th><th>MODE</th><th class=\"num\">DMG TAKEN %</th><th class=\"num\">FLUX COST %</th><th class=\"num\">ALIVE</th><th>END REASON</th><th class=\"num\">ELIMS</th><th class=\"num\">DMG DEALT</th><th class=\"num\">DMG TAKEN</th></tr></thead><tbody>")
    for (rec in summary.recentLives) {
      val actor = when {
        rec.isLocalPilot -> "PILOT"
        rec.isHuman -> "REMOTE"
        else -> "BOT"
      }
      val sess = if (rec.isCustomGame) "CUSTOM" else "PUBLIC"
      val hullKey = esc(rec.shipName.uppercase(java.util.Locale.US))
      val tStr = fmt.format(java.util.Date(rec.timestampMs))
      val aliveCls = if (rec.aliveDurationSec < 12f) " warn" else if (rec.aliveDurationSec >= 30f) " good" else ""
      sb.appendLine(
        "<tr data-hull=\"$hullKey\" data-actor=\"$actor\" data-session=\"$sess\"><td>${esc(tStr)}</td><td>$actor (${esc(rec.callsign)})</td><td>${esc(rec.shipName)}</td>" +
          "<td>${esc(rec.modeId)}${if (rec.isCustomGame) " [CUSTOM]" else ""}</td>" +
          "<td class=\"num\">${rec.damageTakenScalePct}%</td>" +
          "<td class=\"num\">${rec.weaponFluxCostScalePct}%</td>" +
          "<td class=\"num$aliveCls\">${formatSurvivalDurationSec(rec.aliveDurationSec)}</td>" +
          "<td>${esc(rec.endReason)}</td>" +
          "<td class=\"num\">${rec.killsInLife}</td>" +
          "<td class=\"num\">${rec.damageDealtInLife.toInt()}</td>" +
          "<td class=\"num\">${rec.damageTakenInLife.toInt()}</td></tr>"
      )
    }
    sb.appendLine("</tbody></table>")
    sb.appendLine("<h2>RIFT HUB NON-TEXT TAP TELEMETRY (LATEST ${hubTaps.size})</h2>")
    sb.appendLine("<table id=\"tapTable\"><thead><tr><th>TIME</th><th>ACTIVE HUB VIEW</th><th>TARGET CONTROL TAG</th><th class=\"num\">TAP X (0..1)</th><th class=\"num\">TAP Y (0..1)</th></tr></thead><tbody>")
    for (tap in hubTaps) {
      val tStr = fmt.format(java.util.Date(tap.timestampMs))
      val vKey = esc(tap.activeHubView.uppercase(java.util.Locale.US))
      sb.appendLine(
        "<tr data-hubview=\"$vKey\"><td>${esc(tStr)}</td><td>${esc(tap.activeHubView)}</td><td>${esc(tap.targetControlTag)}</td>" +
          "<td class=\"num\">${String.format(java.util.Locale.US, "%.3f", tap.tapXNorm)}</td>" +
          "<td class=\"num\">${String.format(java.util.Locale.US, "%.3f", tap.tapYNorm)}</td></tr>"
      )
    }
    sb.appendLine("</tbody></table>")
    sb.appendLine("<script>")
    sb.appendLine("function applyFilters(){")
    sb.appendLine("var hull=document.getElementById('fHull').value;")
    sb.appendLine("var actor=document.getElementById('fActor').value;")
    sb.appendLine("var sess=document.getElementById('fSession').value;")
    sb.appendLine("var hv=document.getElementById('fHubView').value;")
    sb.appendLine("var q=(document.getElementById('fSearch').value||'').toUpperCase();")
    sb.appendLine("var hc=0,lc=0,tc=0;")
    sb.appendLine("var hRows=document.querySelectorAll('#hullTable tbody tr');")
    sb.appendLine("for(var i=0;i<hRows.length;i++){var r=hRows[i];var ok=(hull==='ALL'||r.getAttribute('data-hull')===hull)&&(q===''||r.textContent.toUpperCase().indexOf(q)>=0);r.style.display=ok?'':'none';if(ok)hc++;}")
    sb.appendLine("var lRows=document.querySelectorAll('#lifeTable tbody tr');")
    sb.appendLine("for(var j=0;j<lRows.length;j++){var lr=lRows[j];var okL=(hull==='ALL'||lr.getAttribute('data-hull')===hull)&&(actor==='ALL'||lr.getAttribute('data-actor')===actor)&&(sess==='ALL'||lr.getAttribute('data-session')===sess)&&(q===''||lr.textContent.toUpperCase().indexOf(q)>=0);lr.style.display=okL?'':'none';if(okL)lc++;}")
    sb.appendLine("var tRows=document.querySelectorAll('#tapTable tbody tr');")
    sb.appendLine("for(var k=0;k<tRows.length;k++){var tr=tRows[k];var okT=(hv==='ALL'||tr.getAttribute('data-hubview')===hv)&&(q===''||tr.textContent.toUpperCase().indexOf(q)>=0);tr.style.display=okT?'':'none';if(okT)tc++;}")
    sb.appendLine("document.getElementById('fCount').textContent='SHOWING '+hc+' HULLS \\u2022 '+lc+' LIVES \\u2022 '+tc+' HUB TAPS';")
    sb.appendLine("}")
    sb.appendLine("function resetFilters(){document.getElementById('fHull').value='ALL';document.getElementById('fActor').value='ALL';document.getElementById('fSession').value='ALL';document.getElementById('fHubView').value='ALL';document.getElementById('fSearch').value='';applyFilters();}")
    sb.appendLine("</script></body></html>")
    return sb.toString()
  }

  fun getPersistedCommsMessages(): List<SyndicateChatMessage> {
    val raw = userSettingsPrefs.getString(KEY_PERSISTED_COMMS_JSON, "[]") ?: "[]"
    val out = mutableListOf<SyndicateChatMessage>()
    try {
      val arr = JSONArray(raw)
      for (i in 0 until arr.length()) {
        val o = arr.optJSONObject(i) ?: continue
        val label = o.optString("timestampLabel", "").trim()
        if (label.startsWith("#WING-") || label.startsWith("#VOICE-")) continue
        val id = o.optString("id", "").trim()
        if (id.isEmpty()) continue
        out.add(
          SyndicateChatMessage(
            id = id,
            senderUid = o.optString("senderUid", ""),
            senderCallsign = o.optString("senderCallsign", "PILOT"),
            senderSyndicateTag = sanitizeSyndicateTag(o.optString("senderSyndicateTag", "")),
            timestampLabel = label.ifEmpty { "#sector-lounge" },
            messageText = o.optString("messageText", ""),
            isArmadaRecruitBanner = o.optBoolean("isArmadaRecruitBanner", false),
            armadaName = o.optString("armadaName", ""),
            armadaCurrentSize = o.optInt("armadaCurrentSize", 1).coerceIn(1, 5),
            armadaMaxSize = o.optInt("armadaMaxSize", 5).coerceIn(1, 5),
            armadaObjective = o.optString("armadaObjective", ""),
            requestSent = o.optBoolean("requestSent", false),
            createdAtMs = o.optLong("createdAtMs", 0L),
          )
        )
      }
    } catch (_: Exception) {}
    return out
  }

  fun savePersistedCommsMessages(messages: List<SyndicateChatMessage>) {
    val filtered =
      messages
        .filter { m ->
          val lbl = m.timestampLabel.trim().uppercase(Locale.US)
          !lbl.startsWith("#WING-") && !lbl.startsWith("#VOICE-") && m.messageText.isNotBlank()
        }
        .distinctBy { it.id }
        .takeLast(120)
    val arr = JSONArray()
    for (m in filtered) {
      arr.put(
        JSONObject().apply {
          put("id", m.id)
          put("senderUid", m.senderUid)
          put("senderCallsign", m.senderCallsign)
          put("senderSyndicateTag", m.senderSyndicateTag)
          put("timestampLabel", m.timestampLabel)
          put("messageText", m.messageText)
          put("isArmadaRecruitBanner", m.isArmadaRecruitBanner)
          put("armadaName", m.armadaName)
          put("armadaCurrentSize", m.armadaCurrentSize)
          put("armadaMaxSize", m.armadaMaxSize)
          put("armadaObjective", m.armadaObjective)
          put("requestSent", m.requestSent)
          put("createdAtMs", m.createdAtMs)
        }
      )
    }
    userSettingsPrefs.edit().putString(KEY_PERSISTED_COMMS_JSON, arr.toString()).apply()
  }

  fun getLastReadCommsTimestampMs(): Long =
    userSettingsPrefs.getLong(KEY_LAST_READ_COMMS_MS, 0L).coerceAtLeast(0L)

  fun setLastReadCommsTimestampMs(timestampMs: Long = System.currentTimeMillis()) {
    userSettingsPrefs.edit().putLong(KEY_LAST_READ_COMMS_MS, timestampMs.coerceAtLeast(0L)).apply()
  }

  fun recordHubTapTelemetry(
    tapXNorm: Float,
    tapYNorm: Float,
    activeHubView: String,
    targetControlTag: String,
  ) {
    val cleanRecord =
      HubTapTelemetryRecord(
        timestampMs = System.currentTimeMillis(),
        tapXNorm = tapXNorm.coerceIn(0f, 1f),
        tapYNorm = tapYNorm.coerceIn(0f, 1f),
        activeHubView = activeHubView.trim().take(32).ifEmpty { "HUB_MAIN" },
        targetControlTag = targetControlTag.trim().take(40).ifEmpty { "BACKGROUND" },
      )
    val existing = getHubTapTelemetryHistory().toMutableList()
    existing.add(0, cleanRecord)
    val capped = existing.take(120)
    val arr = JSONArray()
    capped.forEach { arr.put(it.toJson()) }
    userSettingsPrefs.edit().putString(KEY_HUB_TAP_HISTORY_JSON, arr.toString()).apply()
  }

  fun getHubTapTelemetryHistory(): List<HubTapTelemetryRecord> {
    val raw = userSettingsPrefs.getString(KEY_HUB_TAP_HISTORY_JSON, "[]") ?: "[]"
    return try {
      val arr = JSONArray(raw)
      buildList {
        for (i in 0 until arr.length()) {
          val obj = arr.optJSONObject(i) ?: continue
          add(HubTapTelemetryRecord.fromJson(obj))
        }
      }
    } catch (_: Exception) {
      emptyList()
    }
  }

  fun clearHubTapTelemetryHistory() {
    userSettingsPrefs.edit().putString(KEY_HUB_TAP_HISTORY_JSON, "[]").apply()
  }

  fun recordRiftHubTap(xNorm: Float, yNorm: Float, nodeName: String, activeHubView: String = "HUB_MAIN") {
    recordHubTapTelemetry(
      tapXNorm = xNorm,
      tapYNorm = yNorm,
      activeHubView = activeHubView,
      targetControlTag = nodeName,
    )
    val entry =
      String.format(
        Locale.US,
        "%s:%.2f,%.2f",
        nodeName.take(10),
        xNorm.coerceIn(0f, 1f),
        yNorm.coerceIn(0f, 1f),
      )
    val existing =
      (userSettingsPrefs.getString(KEY_HUB_TAP_BUFFER, "") ?: "")
        .split(";")
        .filter { it.isNotBlank() }
        .toMutableList()
    existing.add(entry)
    val capped = existing.takeLast(24)
    userSettingsPrefs.edit().putString(KEY_HUB_TAP_BUFFER, capped.joinToString(";")).apply()
  }

  fun drainRiftHubTapSummary(minCountToFlush: Int = 8): String? {
    val existing =
      (userSettingsPrefs.getString(KEY_HUB_TAP_BUFFER, "") ?: "")
        .split(";")
        .filter { it.isNotBlank() }
    if (existing.size < minCountToFlush) return null
    userSettingsPrefs.edit().putString(KEY_HUB_TAP_BUFFER, "").apply()
    return existing.takeLast(12).joinToString(";").take(235)
  }

  companion object {
    val NON_PLAYER_SYNDICATE_TAGS: Set<String> =
      setOf(
        "",
        "GUEST",
        "GUE",
        "GUES",
        "NOV",
        "NOVA",
        "RFT",
        "BOT",
        "SOLO",
        "FREE",
        "NONE",
        "UNAFFILIATED",
        "KSTL",
        "AURL",
        "VRTX",
        "RUST",
      )

    val DRIFTER_NAMES: List<String> =
      listOf(
        "VORTEX",
        "DRIFTER",
        "NOMAD",
        "PHANTOM",
        "STRIDER",
        "SPECTRE",
        "NOVA",
        "COMET",
        "PULSAR",
        "QUASAR",
        "KESTREL",
        "CORSAIR",
        "RAVEN",
        "FALCON",
        "SHADOW",
        "VECTOR",
        "ZENITH",
        "CYPHER",
        "ASTRAL",
        "HORIZON",
      )

    private const val PREFS_NAME = "rift_permanent_pilot_prefs"
    private const val USER_SETTINGS_PREFS_NAME = "rift_user_settings"
    private const val KEY_PRIMARY_SWORN_UID = "primary_sworn_uid_v90"
    private const val KEY_DRIFTER_CALLSIGN = "drifter_ephemeral_callsign"
    private const val KEY_PERSISTED_COMMS_JSON = "persisted_subspace_comms_v90"
    private const val KEY_HUB_TAP_BUFFER = "rift_hub_tap_buffer_v90"
    private const val KEY_BLOCKED_PILOT_UIDS = "ugc_blocked_pilot_uids"
    private const val KEY_REPORTED_PILOTS_JSON = "ugc_reported_pilots_json"
    private const val KEY_DEVICE_UID = "permanent_device_uid"
    private const val KEY_GUEST_BYPASS = "guest_bypass_enabled"
    private const val KEY_CALLSIGN = "pilot_callsign"
    private const val KEY_CALLSIGN_CHANGE_COUNT = "pilot_callsign_change_count"
    private const val KEY_INITIAL_CALLSIGN_CHOSEN = "pilot_initial_callsign_chosen"
    private const val KEY_SYNDICATE_TAG = "syndicate_tag"
    private const val KEY_SYNDICATE_NAME = "syndicate_name"
    private const val KEY_SYNDICATE_LOCK_UNTIL_MS = "syndicate_lock_until_ms"
    private const val KEY_ARMADA_NAME = "active_armada_name"
    private const val KEY_CUSTOM_SYNDICATES_JSON = "custom_syndicates_json"
    private const val KEY_CUSTOM_FRIENDS_JSON = "custom_friends_json"
    private const val KEY_SHIP_ID = "preferred_ship_id"
    private const val KEY_MODE = "preferred_mode"
    private const val KEY_BOOT_TO_NEXUS_HUB = "boot_to_nexus_hub_v30"
    private const val KEY_SFX_ENABLED = "sfx_enabled"
    private const val KEY_MUSIC_ENABLED = "music_enabled"
    private const val KEY_MUSIC_BPM = "music_bpm"
    private const val KEY_MUSIC_VOLUME = "music_volume"
    private const val KEY_SFX_VOLUME = "sfx_volume"
    private const val KEY_MUSIC_ARCHETYPE_ID = "music_archetype_id"
    private const val KEY_MUSIC_FORGE_PROMPT = "music_forge_prompt"
    private const val KEY_SYNDICATE_CHANNELS_PREFIX = "syndicate_custom_channels_"
    private const val KEY_SYNDICATE_GUESTS_PREFIX = "syndicate_invited_guests_"
    private const val KEY_SYNDICATE_CREST_PREFIX = "syndicate_crest_json_"
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

    // Pilot Level (1..100), 5 XP Sources & Rift Shards Currency Keys
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
    private const val KEY_SLOT_D_UNLOCKED = "custom_slot_9d_unlocked"
    private const val KEY_SLOT_E_UNLOCKED = "custom_slot_9e_unlocked"
    private const val KEY_CUSTOM_SLOT_PREFIX = "custom_ship_slot_json"
    private const val KEY_ALL_TIME_PEAK_SNAPSHOT = "all_time_peak_bounty_snapshot_json"
    private const val KEY_PVP_PEAK_SNAPSHOT = "pvp_peak_bounty_snapshot_json"
    private const val KEY_SYNDICATE_HISTORY_JSON = "syndicate_service_history_json"
    private const val KEY_FIRST_COMMISSION_MS = "pilot_first_commission_ms"
    private const val KEY_LIVE_RESONANCE = "conduit_live_resonance"
    private const val KEY_PEAK_RESONANCE = "conduit_peak_resonance"
    private const val KEY_RESONANCE_MIGRATED = "conduit_resonance_migrated_v57"
    private const val KEY_SHIP_RESONANCE_PREFIX = "conduit_ship_resonance_v83_"
    private const val KEY_SHIP_RESONANCE_MIGRATED = "conduit_ship_resonance_migrated_v83"
    private const val KEY_DAILY_DIRECTIVES_UTC_DAY = "daily_directives_utc_day"
    private const val KEY_DAILY_DIRECTIVE_PROGRESS_PREFIX = "daily_directive_prog_"
    private const val KEY_DAILY_DIRECTIVE_CLAIMED_PREFIX = "daily_directive_claimed_"
    private const val KEY_DAILY_DIRECTIVE_REROLL_SALT_PREFIX = "daily_directive_salt_"
    private const val KEY_DAILY_DIRECTIVE_REROLL_USED = "daily_directive_reroll_used"
    private const val KEY_DAILY_SWEEP_CLAIMED = "daily_directive_sweep_claimed"
    private const val KEY_SWORN_PRACTICE_TUTORIAL_DONE = "sworn_practice_tutorial_done_v79"
    private const val KEY_SHIP_SURVIVAL_PREFIX = "ship_survival_v103_"
    private const val KEY_RECENT_SHIP_LIVES_JSON = "recent_ship_lives_v103_json"
    private const val KEY_HUB_TAP_HISTORY_JSON = "rift_hub_tap_history_v115_json"
    private const val KEY_LAST_READ_COMMS_MS = "last_read_subspace_comms_ms_v115"
    private const val KEY_SYNDICATE_INVITE_ONLY_PREFIX = "syndicate_invite_only_v115_"
    private const val KEY_SYNDICATE_OFFICERS_PREFIX = "syndicate_officers_v115_"
    private const val KEY_MATCHES_PLAYED = "career_matches_played_v115"
    private const val KEY_MATCHES_LOST = "career_matches_lost_v115"
    private const val KEY_CAREER_ASSISTS = "career_assists_v115"
    private const val KEY_CAREER_SHOTS_FIRED = "career_shots_fired_v115"
    private const val KEY_CAREER_SHOTS_HIT = "career_shots_hit_v115"
    private const val KEY_RIFT_CAPTURES = "career_rift_captures_v115"

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
