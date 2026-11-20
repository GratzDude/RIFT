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
    val perUid = if (!uid.isNullOrBlank()) prefs.getString(uidKey(KEY_CALLSIGN, uid), null) else null
    val global = if (canUseLegacyUnscopedFallback(uid)) prefs.getString(KEY_CALLSIGN, null) else null
    fun isChosenSwornName(s: String?): Boolean {
      if (s.isNullOrBlank()) return false
      val u = s.trim().uppercase(Locale.US)
      return u != "COMMANDER" && !u.startsWith("RFT-") && !u.startsWith("#RFT")
    }
    val chosenPerUid = perUid?.takeIf { isChosenSwornName(it) }
    val chosenGlobal = global?.takeIf { isChosenSwornName(it) }
    val resolved = chosenPerUid ?: chosenGlobal
    if (resolved != null) {
      if (!uid.isNullOrBlank() && chosenPerUid != resolved) {
        val editor = prefs.edit().putString(uidKey(KEY_CALLSIGN, uid), resolved)
        if (canUseLegacyUnscopedFallback(uid)) {
          editor.putString(KEY_CALLSIGN, resolved)
        }
        editor.apply()
      }
      return resolved
    }
    return "COMMANDER"
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
    } else if (canUseLegacyUnscopedFallback(uid)) {
      editor.putString(KEY_CALLSIGN, cleaned)
    }
    if (!uid.isNullOrBlank()) {
      editor.putString(uidKey(KEY_CALLSIGN, uid), cleaned)
    }
    editor.apply()
    return cleaned
  }

  fun getCallsignChangeCount(uid: String? = null): Int =
    prefs.getInt(
      uidKey(KEY_CALLSIGN_CHANGE_COUNT, uid),
      if (canUseLegacyUnscopedFallback(uid)) prefs.getInt(KEY_CALLSIGN_CHANGE_COUNT, 0) else 0,
    )

  fun incrementCallsignChangeCount(uid: String? = null) {
    val next = getCallsignChangeCount(uid) + 1
    val ed = prefs.edit().putInt(uidKey(KEY_CALLSIGN_CHANGE_COUNT, uid), next)
    if (canUseLegacyUnscopedFallback(uid)) {
      ed.putInt(KEY_CALLSIGN_CHANGE_COUNT, next)
    }
    ed.apply()
  }

  fun hasChosenInitialCallsign(uid: String? = null): Boolean {
    if (isGuestBypassEnabled() || isDrifterOrEphemeralUid(uid)) return false
    val explicitChosen =
      prefs.getBoolean(
        uidKey(KEY_INITIAL_CALLSIGN_CHOSEN, uid),
        if (canUseLegacyUnscopedFallback(uid)) prefs.getBoolean(KEY_INITIAL_CALLSIGN_CHOSEN, false) else false,
      )
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
    val editor = prefs.edit().putBoolean(KEY_INITIAL_CALLSIGN_CHOSEN, chosen)
    if (!uid.isNullOrBlank()) {
      editor.putBoolean(uidKey(KEY_INITIAL_CALLSIGN_CHOSEN, uid), chosen)
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

  // Active Named Strike Wing / Armada (SOLO = "", or up-to-14-char Strike Wing tag e.g. "ASTRAL_FLEET")
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
    val norm = normalizeCustomSlotId(slotId)
    if (norm == "Frame A") return true
    val lvl = getPilotLevel(uid)
    val peakRes = getPeakResonance(uid)
    val gate = com.example.engine.RiftProgressionCatalog.getFrameSlotGate(norm)
    return when (norm) {
      "Frame B" ->
        prefs.getBoolean(uidKey(KEY_SLOT_B_UNLOCKED, uid), prefs.getBoolean(KEY_SLOT_B_UNLOCKED, false)) ||
          getUnlockedExtraSlots(uid) >= 1 ||
          lvl >= 35 ||
          peakRes >= gate.freeAtResonance
      "Frame C" ->
        prefs.getBoolean(uidKey(KEY_SLOT_C_UNLOCKED, uid), prefs.getBoolean(KEY_SLOT_C_UNLOCKED, false)) ||
          getUnlockedExtraSlots(uid) >= 2 ||
          lvl >= 50 ||
          peakRes >= gate.freeAtResonance
      "Frame D" ->
        prefs.getBoolean(uidKey(KEY_SLOT_D_UNLOCKED, uid), prefs.getBoolean(KEY_SLOT_D_UNLOCKED, false)) ||
          getUnlockedExtraSlots(uid) >= 3 ||
          lvl >= 75 ||
          peakRes >= gate.freeAtResonance
      "Frame E" ->
        prefs.getBoolean(uidKey(KEY_SLOT_E_UNLOCKED, uid), prefs.getBoolean(KEY_SLOT_E_UNLOCKED, false)) ||
          getUnlockedExtraSlots(uid) >= 4 ||
          lvl >= 100 ||
          peakRes >= gate.freeAtResonance
      else -> true
    }
  }

  fun setCustomSlotUnlocked(slotId: String, unlocked: Boolean, uid: String? = null) {
    val key =
      when (normalizeCustomSlotId(slotId)) {
        "Frame B" -> KEY_SLOT_B_UNLOCKED
        "Frame C" -> KEY_SLOT_C_UNLOCKED
        "Frame D" -> KEY_SLOT_D_UNLOCKED
        "Frame E" -> KEY_SLOT_E_UNLOCKED
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
    pilotLevel: Int = getPilotLevel(),
    uid: String? = null,
  ): Boolean {
    if (itemId <= 0) return true
    val cat = category.trim().uppercase(Locale.US)
    val item = com.example.engine.RiftProgressionCatalog.getCosmeticItem(cat, itemId)
    if (item != null && (pilotLevel >= item.freeAtLevel || getPeakResonance(uid) >= item.freeAtResonance)) {
      return true
    }
    val baseKey = "cosmetic_unlocked_${cat}_$itemId"
    return prefs.getBoolean(uidKey(baseKey, uid), prefs.getBoolean(baseKey, false))
  }

  fun setCosmeticUnlocked(
    category: String,
    itemId: Int,
    unlocked: Boolean = true,
    uid: String? = null,
  ) {
    val cat = category.trim().uppercase(Locale.US)
    val baseKey = "cosmetic_unlocked_${cat}_$itemId"
    prefs
      .edit()
      .putBoolean(baseKey, unlocked)
      .putBoolean(uidKey(baseKey, uid), unlocked)
      .apply()
  }

  fun getEquippedCosmeticId(category: String, uid: String? = null): Int {
    val cat = category.trim().uppercase(Locale.US)
    val baseKey = "cosmetic_equipped_$cat"
    return prefs.getInt(uidKey(baseKey, uid), prefs.getInt(baseKey, 0)).coerceAtLeast(0)
  }

  fun setEquippedCosmeticId(category: String, itemId: Int, uid: String? = null) {
    val cat = category.trim().uppercase(Locale.US)
    val baseKey = "cosmetic_equipped_$cat"
    val cleanId = itemId.coerceAtLeast(0)
    prefs
      .edit()
      .putInt(baseKey, cleanId)
      .putInt(uidKey(baseKey, uid), cleanId)
      .apply()
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
    prefs.edit().putBoolean(uidKey(KEY_DAILY_SWEEP_CLAIMED, uid), true).apply()
    return sweepShards to sweepRes
  }

  // Adaptive Armada Tier Kills (1 = SOLO, 2 = 2-Man, 3 = 3-Man, 4 = 4-Man, 5 = 5-Man Strike Wing)
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

  fun mergeCloudSyndicateMeta(meta: CloudSyndicateMeta) {
    val cleanTag = sanitizeSyndicateTag(meta.tag)
    if (cleanTag.isEmpty() || cleanTag in NON_PLAYER_SYNDICATE_TAGS) return
    if (meta.customChannels.isNotEmpty()) {
      setSyndicateCustomChannels(cleanTag, meta.customChannels)
    }
    val sanitizedGuests =
      meta.invitedGuests
        .map { it.trim().take(20) }
        .filter { it.isNotEmpty() }
    prefs
      .edit()
      .putString("${KEY_SYNDICATE_GUESTS_PREFIX}$cleanTag", sanitizedGuests.joinToString(","))
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
    return raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
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
      true
    } catch (_: Exception) {
      false
    }
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

  fun recordRiftHubTap(xNorm: Float, yNorm: Float, nodeName: String) {
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
