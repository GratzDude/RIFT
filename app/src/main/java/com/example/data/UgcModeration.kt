package com.example.data

import java.util.Locale

/**
 * Lightweight on-device UGC moderation helper for pilot callsigns, Syndicate metadata,
 * Armada tags, and Syndicate Comms chat messages.
 */
object UgcModeration {
  private val BLOCKED_WORDS =
    listOf(
      "fuck",
      "shit",
      "bitch",
      "asshole",
      "bastard",
      "cunt",
      "dickhead",
      "motherfucker",
      "nigger",
      "nigga",
      "faggot",
      "retard",
      "slut",
      "whore",
      "nazi",
      "hitler",
      "kys",
    )

  private val WHOLE_WORD_REGEX =
    Regex(
      "\\b(${BLOCKED_WORDS.joinToString("|") { Regex.escape(it) }})\\b",
      RegexOption.IGNORE_CASE,
    )

  private val COMPACT_TOKEN_REGEX =
    Regex(
      "(${BLOCKED_WORDS.filter { it.length >= 4 }.joinToString("|") { Regex.escape(it) }})",
      RegexOption.IGNORE_CASE,
    )

  fun containsProfanity(raw: String): Boolean {
    if (raw.isBlank()) return false
    if (WHOLE_WORD_REGEX.containsMatchIn(raw)) return true
    val stripped = raw.lowercase(Locale.US).replace(Regex("[^a-z]"), "")
    return BLOCKED_WORDS.any { word ->
      word.length >= 4 && stripped.contains(word)
    }
  }

  fun sanitizeUgcText(raw: String): String {
    if (raw.isBlank()) return raw
    var cleaned = WHOLE_WORD_REGEX.replace(raw) { match -> "*".repeat(match.value.length.coerceAtLeast(3)) }
    if (containsProfanity(cleaned)) {
      cleaned = COMPACT_TOKEN_REGEX.replace(cleaned) { match -> "*".repeat(match.value.length.coerceAtLeast(3)) }
    }
    return cleaned
  }

  fun sanitizeIdentifier(raw: String, fallback: String): String {
    if (raw.isBlank() || containsProfanity(raw)) return fallback
    return raw
  }
}
