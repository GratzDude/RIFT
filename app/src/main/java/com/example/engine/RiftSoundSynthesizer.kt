package com.example.engine

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.SoundPool
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random
import org.json.JSONArray
import org.json.JSONObject

enum class MusicSynthArchetype(
  val id: String,
  val label: String,
  val defaultBpm: Int,
  val summary: String,
) {
  DARK_SYNTHWAVE(
    id = "DARK_SYNTHWAVE",
    label = "Dark Synthwave",
    defaultBpm = 118,
    summary = "Driving minor bassline + pulsing neon arpeggio",
  ),
  CYBER_INDUSTRIAL(
    id = "CYBER_INDUSTRIAL",
    label = "Industrial Bass",
    defaultBpm = 132,
    summary = "Aggressive Phrygian sawtooth + heavy gated percussion",
  ),
  COSMIC_AMBIENT(
    id = "COSMIC_AMBIENT",
    label = "Deep-Space Pad",
    defaultBpm = 84,
    summary = "Floating Lydian synth pads + sub-drone, no drums",
  ),
  RETRO_CHIPTUNE(
    id = "RETRO_CHIPTUNE",
    label = "Arcade Chiptune",
    defaultBpm = 142,
    summary = "Crisp pulse-wave arcade lead + rapid 16th-note arp",
  ),
  SINGULARITY_OVERDRIVE(
    id = "SINGULARITY_OVERDRIVE",
    label = "Singularity Drive",
    defaultBpm = 156,
    summary = "High-tension harmonic minor polyrhythm + punchy kick",
  );

  companion object {
    fun fromId(rawId: String?): MusicSynthArchetype =
      entries.firstOrNull { it.id.equals(rawId?.trim(), ignoreCase = true) } ?: DARK_SYNTHWAVE
  }
}

data class MusicForgeRecipe(
  val archetype: MusicSynthArchetype = MusicSynthArchetype.DARK_SYNTHWAVE,
  val bpm: Int = 118,
  val rootMidiNote: Int = 38, // D2 default
  val scaleIntervals: IntArray = intArrayOf(0, 2, 3, 5, 7, 8, 10), // Natural Minor
  val leadWaveform: Int = 0, // 0=DetunedSaw, 1=PulseSquare, 2=WarmSinePad, 3=FmBell
  val arpPattern: Int = 0, // 0=UpDown, 1=OctaveJump, 2=TripletRoll, 3=SparseAmbient
  val drumsEnabled: Boolean = true,
  val bassDrive: Float = 0.80f,
  val brightness: Float = 0.65f,
  val customPromptSummary: String = "Dark Synthwave • 118 BPM",
) {
  fun toJson(): JSONObject =
    JSONObject().apply {
      put("archetype", archetype.id)
      put("bpm", bpm.coerceIn(60, 180))
      put("rootMidiNote", rootMidiNote.coerceIn(28, 60))
      val scaleArr = JSONArray()
      scaleIntervals.forEach { scaleArr.put(it) }
      put("scaleIntervals", scaleArr)
      put("leadWaveform", leadWaveform.coerceIn(0, 3))
      put("arpPattern", arpPattern.coerceIn(0, 3))
      put("drumsEnabled", drumsEnabled)
      put("bassDrive", ((bassDrive.coerceIn(0.2f, 1.2f) * 100f).roundToInt() / 100.0))
      put("brightness", ((brightness.coerceIn(0.2f, 1.0f) * 100f).roundToInt() / 100.0))
      put("customPromptSummary", customPromptSummary.take(120))
    }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is MusicForgeRecipe) return false
    return archetype == other.archetype &&
      bpm == other.bpm &&
      rootMidiNote == other.rootMidiNote &&
      scaleIntervals.contentEquals(other.scaleIntervals) &&
      leadWaveform == other.leadWaveform &&
      arpPattern == other.arpPattern &&
      drumsEnabled == other.drumsEnabled &&
      bassDrive == other.bassDrive &&
      brightness == other.brightness &&
      customPromptSummary == other.customPromptSummary
  }

  override fun hashCode(): Int {
    var result = archetype.hashCode()
    result = 31 * result + bpm
    result = 31 * result + rootMidiNote
    result = 31 * result + scaleIntervals.contentHashCode()
    result = 31 * result + leadWaveform
    result = 31 * result + arpPattern
    result = 31 * result + drumsEnabled.hashCode()
    result = 31 * result + bassDrive.hashCode()
    result = 31 * result + brightness.hashCode()
    result = 31 * result + customPromptSummary.hashCode()
    return result
  }

  companion object {
    fun fromJson(obj: JSONObject?): MusicForgeRecipe {
      if (obj == null) return RiftSoundSynthesizer.buildArchetypeRecipe(MusicSynthArchetype.DARK_SYNTHWAVE, 118)
      val arch = MusicSynthArchetype.fromId(obj.optString("archetype", "DARK_SYNTHWAVE"))
      val bpm = obj.optInt("bpm", arch.defaultBpm).coerceIn(60, 180)
      val scaleArr = obj.optJSONArray("scaleIntervals")
      val intervals =
        if (scaleArr != null && scaleArr.length() > 0) {
          IntArray(scaleArr.length()) { idx -> scaleArr.optInt(idx, 0).coerceIn(0, 11) }
        } else {
          intArrayOf(0, 2, 3, 5, 7, 8, 10)
        }
      return MusicForgeRecipe(
        archetype = arch,
        bpm = bpm,
        rootMidiNote = obj.optInt("rootMidiNote", 38).coerceIn(28, 60),
        scaleIntervals = intervals,
        leadWaveform = obj.optInt("leadWaveform", 0).coerceIn(0, 3),
        arpPattern = obj.optInt("arpPattern", 0).coerceIn(0, 3),
        drumsEnabled = obj.optBoolean("drumsEnabled", arch != MusicSynthArchetype.COSMIC_AMBIENT),
        bassDrive = obj.optDouble("bassDrive", 0.80).toFloat().coerceIn(0.2f, 1.2f),
        brightness = obj.optDouble("brightness", 0.65).toFloat().coerceIn(0.2f, 1.0f),
        customPromptSummary =
          obj.optString("customPromptSummary", "${arch.label} • $bpm BPM")
            .ifBlank { "${arch.label} • $bpm BPM" }
            .take(120),
      )
    }
  }
}

/**
 * 100% On-Device Procedural Combat SFX Recipe for `NEXUS FORGE` (`Soundtrack & SFX Forge` bay).
 * Controls live PCM synthesis of lasers, explosions, Repel/shield ripples, and pickup chimes.
 */
data class SfxForgeRecipe(
  val laserPitchMultiplier: Float = 1.0f, // 0.55f .. 1.75f
  val laserHarmonicDrive: Float = 0.45f, // 0.10f .. 1.00f
  val explosionSubBass: Float = 0.85f, // 0.25f .. 1.40f
  val explosionDebrisCrunch: Float = 0.35f, // 0.05f .. 1.00f
  val shieldRippleResonance: Float = 0.70f, // 0.20f .. 1.35f
  val uiChimeBrightness: Float = 0.65f, // 0.20f .. 1.25f
  val promptUsed: String = "Tactical Standard SFX",
) {
  fun toJson(): JSONObject =
    JSONObject().apply {
      put("laserPitchMultiplier", ((laserPitchMultiplier.coerceIn(0.55f, 1.75f) * 100f).roundToInt() / 100.0))
      put("laserHarmonicDrive", ((laserHarmonicDrive.coerceIn(0.10f, 1.00f) * 100f).roundToInt() / 100.0))
      put("explosionSubBass", ((explosionSubBass.coerceIn(0.25f, 1.40f) * 100f).roundToInt() / 100.0))
      put("explosionDebrisCrunch", ((explosionDebrisCrunch.coerceIn(0.05f, 1.00f) * 100f).roundToInt() / 100.0))
      put("shieldRippleResonance", ((shieldRippleResonance.coerceIn(0.20f, 1.35f) * 100f).roundToInt() / 100.0))
      put("uiChimeBrightness", ((uiChimeBrightness.coerceIn(0.20f, 1.25f) * 100f).roundToInt() / 100.0))
      put("promptUsed", promptUsed.take(120))
    }

  companion object {
    fun fromJson(obj: JSONObject?): SfxForgeRecipe {
      if (obj == null) return SfxForgeRecipe()
      return SfxForgeRecipe(
        laserPitchMultiplier = obj.optDouble("laserPitchMultiplier", 1.0).toFloat().coerceIn(0.55f, 1.75f),
        laserHarmonicDrive = obj.optDouble("laserHarmonicDrive", 0.45).toFloat().coerceIn(0.10f, 1.00f),
        explosionSubBass = obj.optDouble("explosionSubBass", 0.85).toFloat().coerceIn(0.25f, 1.40f),
        explosionDebrisCrunch = obj.optDouble("explosionDebrisCrunch", 0.35).toFloat().coerceIn(0.05f, 1.00f),
        shieldRippleResonance = obj.optDouble("shieldRippleResonance", 0.70).toFloat().coerceIn(0.20f, 1.35f),
        uiChimeBrightness = obj.optDouble("uiChimeBrightness", 0.65).toFloat().coerceIn(0.20f, 1.25f),
        promptUsed = obj.optString("promptUsed", "Tactical Standard SFX").ifBlank { "Tactical Standard SFX" }.take(120),
      )
    }

    fun synthesizeFromPrompt(rawPrompt: String, variationSalt: Int = 0): SfxForgeRecipe {
      val cleaned = rawPrompt.trim()
      if (cleaned.isEmpty()) return SfxForgeRecipe()
      val lower = cleaned.lowercase(Locale.US)
      val hash = abs((lower + "#$variationSalt").hashCode())
      fun hasAny(vararg w: String): Boolean = w.any { lower.contains(it) }

      val pitch =
        when {
          hasAny("heavy", "deep", "cannon", "bass", "sub", "railgun", "thump", "low") -> 0.72f
          hasAny("chirp", "high", "needle", "crystal", "arcade", "8-bit", "8bit", "sharp", "crisp") -> 1.34f
          hasAny("stealth", "silenced", "soft", "muted", "suppressed") -> 0.86f
          else -> 0.92f + (hash % 25) * 0.01f
        }

      val drive =
        when {
          hasAny("gritty", "overdrive", "acid", "industrial", "distorted", "plasma", "heavy", "8-bit") -> 0.82f
          hasAny("clean", "pure", "glass", "crystal", "stealth", "soft") -> 0.24f
          else -> 0.48f
        }

      val subBass =
        when {
          hasAny("seismic", "sub", "bass", "boom", "heavy", "nuclear", "cinematic", "cannon") -> 1.25f
          hasAny("light", "arcade", "minimal", "stealth", "crisp") -> 0.58f
          else -> 0.88f
        }

      val crunch =
        when {
          hasAny("crunch", "debris", "shrapnel", "metallic", "industrial", "gritty") -> 0.78f
          hasAny("smooth", "clean", "sub", "pure", "soft") -> 0.18f
          else -> 0.38f
        }

      val shieldRes =
        when {
          hasAny("resonant", "quantum", "phase", "ripple", "magnetic", "sci-fi", "glass", "crystal") -> 1.15f
          hasAny("tight", "dry", "minimal", "stealth") -> 0.42f
          else -> 0.74f
        }

      val chime =
        when {
          hasAny("bright", "sparkle", "crystal", "bell", "chime", "arcade", "gold") -> 1.08f
          hasAny("dark", "warm", "deep", "stealth", "muted") -> 0.42f
          else -> 0.68f
        }

      return SfxForgeRecipe(
        laserPitchMultiplier = pitch.coerceIn(0.55f, 1.75f),
        laserHarmonicDrive = drive.coerceIn(0.10f, 1.00f),
        explosionSubBass = subBass.coerceIn(0.25f, 1.40f),
        explosionDebrisCrunch = crunch.coerceIn(0.05f, 1.00f),
        shieldRippleResonance = shieldRes.coerceIn(0.20f, 1.35f),
        uiChimeBrightness = chime.coerceIn(0.20f, 1.25f),
        promptUsed = cleaned.take(120),
      )
    }
  }
}

/**
 * Zero-Latency Procedural Space Audio Synthesizer + Real-Time 60–180 BPM Battle Soundtrack
 * with 3 persistent Track Slots (`Track A`, `Track B`, `Track C`) and live procedural SFX forging.
 */
class RiftSoundSynthesizer(
  cacheDir: File = File(System.getProperty("java.io.tmpdir") ?: "/data/local/tmp"),
) {
  private val sfxCacheDir: File = cacheDir
  private val sampleRate = 22050
  private val soundPool: SoundPool

  @Volatile var enabled: Boolean = true
  var soundEnabled: Boolean
    get() = enabled
    set(value) {
      enabled = value
    }

  @Volatile var musicEnabled: Boolean = true
  @Volatile var sfxVolume: Float = 0.85f
  @Volatile var musicVolume: Float = 0.70f
  @Volatile var musicBpm: Int = 128
  @Volatile
  var currentMusicRecipe: MusicForgeRecipe =
    trackSlots.getOrNull(activeTrackSlotIndex)
      ?: buildArchetypeRecipe(MusicSynthArchetype.DARK_SYNTHWAVE, 128)

  val musicTrackSlots: SnapshotStateList<MusicForgeRecipe>
    get() = trackSlots

  var currentTrackSlot: Int
    get() = activeTrackSlotIndex
    set(value) {
      selectTrackSlot(value)
    }

  var currentSfxRecipe: SfxForgeRecipe
    get() = sharedSfxRecipe
    set(value) {
      applySfxRecipe(value)
    }

  @Volatile private var appForegroundFlag: Boolean = true
  val isAppForeground: Boolean
    get() = appForegroundFlag

  @Volatile private var activeAudioTrack: AudioTrack? = null
  @Volatile private var isReleased: Boolean = false
  private var ambientMusicThread: Thread? = null
  private var sfxBakeGeneration: Int = 0

  @Volatile private var sfxLaser: Int = 0
  @Volatile private var sfxBomb: Int = 0
  @Volatile private var sfxExplosion: Int = 0
  @Volatile private var sfxPrize: Int = 0
  @Volatile private var sfxPortal: Int = 0
  @Volatile private var sfxRepel: Int = 0
  @Volatile private var sfxWarp: Int = 0
  @Volatile private var sfxThor: Int = 0
  @Volatile private var sfxShotgun: Int = 0
  @Volatile private var sfxMissileBeep: Int = 0
  @Volatile private var sfxMutedClick: Int = 0
  @Volatile private var sfxBounce: Int = 0

  init {
    appForegroundFlag = globalForegroundState
    val attrs =
      AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_GAME)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()
    soundPool = SoundPool.Builder().setMaxStreams(12).setAudioAttributes(attrs).build()
    if (!appForegroundFlag) {
      runCatching { soundPool.autoPause() }
    }

    musicBpm = currentMusicRecipe.bpm.coerceIn(60, 180)
    bakeAllSfxBuffers(sharedSfxRecipe)

    val superseded = activeInstance
    if (superseded != null && superseded !== this) {
      runCatching { superseded.release() }
    }
    activeInstance = this
    startAmbientMusicLoop()
  }

  fun setAppForeground(foreground: Boolean) {
    appForegroundFlag = foreground
    runCatching {
      if (!foreground) {
        soundPool.autoPause()
        activeAudioTrack?.let { tr ->
          if (tr.playState == AudioTrack.PLAYSTATE_PLAYING) {
            tr.pause()
            tr.flush()
          }
        }
      } else if (!isReleased) {
        soundPool.autoResume()
        if (musicEnabled && musicVolume > 0.001f) {
          activeAudioTrack?.let { tr ->
            if (tr.playState != AudioTrack.PLAYSTATE_PLAYING) {
              tr.play()
            }
          }
        }
      }
    }
  }

  private fun bakeAllSfxBuffers(recipe: SfxForgeRecipe) {
    val gen = ++sfxBakeGeneration
    val pitch = recipe.laserPitchMultiplier.coerceIn(0.55f, 1.75f).toDouble()
    val drive = recipe.laserHarmonicDrive.coerceIn(0.10f, 1.00f).toDouble()
    val subBass = recipe.explosionSubBass.coerceIn(0.25f, 1.40f).toDouble()
    val crunch = recipe.explosionDebrisCrunch.coerceIn(0.05f, 1.00f).toDouble()
    val ripple = recipe.shieldRippleResonance.coerceIn(0.20f, 1.35f).toDouble()
    val chimeBright = recipe.uiChimeBrightness.coerceIn(0.20f, 1.25f).toDouble()

    val oldIds =
      intArrayOf(
        sfxLaser,
        sfxBomb,
        sfxExplosion,
        sfxPrize,
        sfxPortal,
        sfxRepel,
        sfxWarp,
        sfxThor,
        sfxShotgun,
        sfxMissileBeep,
        sfxMutedClick,
        sfxBounce,
      )

    sfxLaser =
      loadWav(
        sfxCacheDir,
        "sfx_laser_$gen.wav",
        synthSweep(
          fStart = 980.0 * pitch,
          fEnd = 220.0 * pitch,
          durationSec = 0.085,
          amp = 0.45,
          addNoise = (drive * 0.14).coerceIn(0.0, 0.35),
          harmonicDrive = drive,
        ),
      )
    sfxBomb =
      loadWav(
        sfxCacheDir,
        "sfx_bomb_$gen.wav",
        synthSweep(
          fStart = 320.0 * (0.75 + pitch * 0.25),
          fEnd = 62.0 / subBass.coerceIn(0.6, 1.35),
          durationSec = 0.24,
          amp = 0.78,
          addNoise = (0.20 + crunch * 0.25).coerceIn(0.05, 0.55),
          harmonicDrive = drive * 0.8,
        ),
      )
    sfxExplosion =
      loadWav(
        sfxCacheDir,
        "sfx_exp_$gen.wav",
        synthExplosionBurst(
          durationSec = 0.38,
          amp = 0.88,
          subBass = subBass,
          debrisCrunch = crunch,
        ),
      )
    sfxPrize =
      loadWav(
        sfxCacheDir,
        "sfx_prize_$gen.wav",
        synthArpeggio(
          notes =
            doubleArrayOf(
              523.25 * (0.85 + chimeBright * 0.22),
              659.25 * (0.85 + chimeBright * 0.22),
              783.99 * (0.85 + chimeBright * 0.22),
              1046.5 * (0.85 + chimeBright * 0.22),
            ),
          durationSec = 0.16,
          amp = 0.52,
          brightness = chimeBright,
        ),
      )
    sfxPortal =
      loadWav(
        sfxCacheDir,
        "sfx_portal_$gen.wav",
        synthSweep(
          fStart = 240.0 * ripple.coerceIn(0.65, 1.3),
          fEnd = 880.0 * ripple.coerceIn(0.65, 1.3),
          durationSec = 0.22,
          amp = 0.56,
          harmonicDrive = ripple * 0.45,
        ),
      )
    sfxRepel =
      loadWav(
        sfxCacheDir,
        "sfx_repel_$gen.wav",
        synthSweep(
          fStart = 150.0,
          fEnd = 740.0 * ripple.coerceIn(0.65, 1.35),
          durationSec = 0.25,
          amp = 0.74,
          addNoise = 0.16,
          harmonicDrive = ripple * 0.55,
        ),
      )
    sfxWarp =
      loadWav(
        sfxCacheDir,
        "sfx_warp_$gen.wav",
        synthSweep(660.0 * ripple.coerceIn(0.7, 1.25), 180.0, 0.20, 0.55),
      )
    sfxThor =
      loadWav(
        sfxCacheDir,
        "sfx_thor_$gen.wav",
        synthSweep(
          fStart = 140.0,
          fEnd = 42.0 / subBass.coerceIn(0.7, 1.3),
          durationSec = 0.34,
          amp = 0.90,
          addNoise = (0.28 + crunch * 0.30).coerceIn(0.1, 0.65),
          harmonicDrive = drive,
        ),
      )
    sfxShotgun =
      loadWav(
        sfxCacheDir,
        "sfx_shotgun_$gen.wav",
        synthSweep(
          fStart = 420.0 * pitch.coerceIn(0.75, 1.35),
          fEnd = 95.0,
          durationSec = 0.14,
          amp = 0.75,
          addNoise = (0.38 + crunch * 0.30).coerceIn(0.15, 0.72),
          harmonicDrive = drive,
        ),
      )
    sfxMissileBeep =
      loadWav(
        sfxCacheDir,
        "sfx_lock_beep_$gen.wav",
        synthArpeggio(
          notes = doubleArrayOf(1320.0 * (0.85 + chimeBright * 0.2), 1760.0 * (0.85 + chimeBright * 0.2)),
          durationSec = 0.11,
          amp = 0.55,
          brightness = chimeBright,
        ),
      )
    sfxMutedClick =
      loadWav(sfxCacheDir, "sfx_muted_click_$gen.wav", synthSweep(190.0, 110.0, 0.055, 0.38))
    sfxBounce =
      loadWav(
        sfxCacheDir,
        "sfx_bounce_$gen.wav",
        synthSweep(360.0 * ripple.coerceIn(0.75, 1.25), 580.0 * ripple.coerceIn(0.75, 1.25), 0.065, 0.48),
      )

    for (oldId in oldIds) {
      if (oldId > 0) {
        try {
          soundPool.unload(oldId)
        } catch (_: Throwable) {}
      }
    }
    // Clean up older cached wav generations quietly
    if (gen > 1) {
      val prev = gen - 1
      val prefixes =
        arrayOf(
          "sfx_laser_",
          "sfx_bomb_",
          "sfx_exp_",
          "sfx_prize_",
          "sfx_portal_",
          "sfx_repel_",
          "sfx_warp_",
          "sfx_thor_",
          "sfx_shotgun_",
          "sfx_lock_beep_",
          "sfx_muted_click_",
          "sfx_bounce_",
        )
      for (p in prefixes) {
        try {
          File(sfxCacheDir, "$p$prev.wav").delete()
        } catch (_: Throwable) {}
      }
    }
  }

  @JvmName("updateMusicEnabled")
  fun setMusicEnabled(enabledState: Boolean) {
    musicEnabled = enabledState
  }

  fun setMusicBpmLive(bpm: Int) {
    val clamped = bpm.coerceIn(60, 180)
    musicBpm = clamped
    val prev = currentMusicRecipe
    val baseTitle = prev.customPromptSummary.substringBefore("•").trim().ifEmpty { prev.archetype.label }
    val drumSuffix = if (!prev.drumsEnabled && !baseTitle.contains("Pad", ignoreCase = true)) " (No Drums)" else ""
    val updated =
      prev.copy(
        bpm = clamped,
        customPromptSummary = "$baseTitle • $clamped BPM$drumSuffix",
      )
    currentMusicRecipe = updated
    val slotIdx = activeTrackSlotIndex.coerceIn(0, 2)
    if (slotIdx in trackSlots.indices) {
      trackSlots[slotIdx] = updated
    }
  }

  fun setMusicVolumeLive(vol: Float) {
    musicVolume = vol.coerceIn(0f, 1f)
  }

  fun setSfxVolumeLive(vol: Float) {
    sfxVolume = vol.coerceIn(0f, 1f)
  }

  /**
   * Switches active playback instantaneously between Track A (`0`), Track B (`1`), and Track C (`2`).
   */
  fun selectTrackSlot(index: Int): MusicForgeRecipe {
    val clampedIdx = index.coerceIn(0, 2)
    activeTrackSlotIndex = clampedIdx
    val slotRecipe =
      trackSlots.getOrNull(clampedIdx)
        ?: buildArchetypeRecipe(MusicSynthArchetype.DARK_SYNTHWAVE, 128)
    musicBpm = slotRecipe.bpm.coerceIn(60, 180)
    currentMusicRecipe = slotRecipe
    return slotRecipe
  }

  /**
   * Saves the active procedural soundtrack recipe into `Track A` (`0`), `Track B` (`1`), or `Track C` (`2`).
   */
  fun saveCurrentMusicToSlot(
    index: Int = activeTrackSlotIndex,
    customTitle: String? = null,
  ): MusicForgeRecipe {
    val clampedIdx = index.coerceIn(0, 2)
    val slotPrefix = "Track ${'A' + clampedIdx}"
    val base = currentMusicRecipe
    val summary =
      if (!customTitle.isNullOrBlank()) {
        "$slotPrefix: ${customTitle.trim()} • ${base.bpm} BPM"
      } else if (base.customPromptSummary.startsWith("Track ")) {
        "$slotPrefix: ${base.customPromptSummary.substringAfter(":", base.customPromptSummary).trim()}"
      } else {
        "$slotPrefix: ${base.customPromptSummary}"
      }
    val saved = base.copy(customPromptSummary = summary.take(120))
    while (trackSlots.size < 3) {
      trackSlots.add(buildDefaultTrackSlots()[trackSlots.size])
    }
    trackSlots[clampedIdx] = saved
    activeTrackSlotIndex = clampedIdx
    currentMusicRecipe = saved
    return saved
  }

  fun applyMusicArchetype(
    archetype: MusicSynthArchetype,
    overrideBpm: Int? = null,
  ): MusicForgeRecipe {
    val targetBpm = (overrideBpm ?: archetype.defaultBpm).coerceIn(60, 180)
    val recipe = buildArchetypeRecipe(archetype, targetBpm)
    musicBpm = targetBpm
    currentMusicRecipe = recipe
    val slotIdx = activeTrackSlotIndex.coerceIn(0, 2)
    if (slotIdx in trackSlots.indices) {
      val slotPrefix = "Track ${'A' + slotIdx}"
      trackSlots[slotIdx] = recipe.copy(customPromptSummary = "$slotPrefix: ${recipe.customPromptSummary}")
    }
    return recipe
  }

  fun synthesizeMusicFromPrompt(
    prompt: String,
    currentBpm: Int? = null,
    saveToActiveSlot: Boolean = true,
  ): MusicForgeRecipe {
    val forged = forgeMusicFromPrompt(prompt, currentBpm)
    if (saveToActiveSlot) {
      return saveCurrentMusicToSlot(activeTrackSlotIndex)
    }
    return forged
  }

  /**
   * 100% On-Device Deterministic Prompt-to-Synth Forge (Zero External AI / Zero Network Calls).
   * Parses plain-English tempo, scale, waveform, arpeggio, root key, and percussion descriptors
   * and updates the live PCM synthesizer within <50ms.
   */
  fun forgeMusicFromPrompt(
    prompt: String,
    currentBpm: Int? = null,
  ): MusicForgeRecipe {
    val cleaned = prompt.trim()
    if (cleaned.isEmpty()) {
      return applyMusicArchetype(currentMusicRecipe.archetype, currentBpm)
    }
    val lower = cleaned.lowercase(Locale.US)
    val hashSalt = abs(lower.hashCode())

    // 1. Archetype & Scale / Mood Classification
    val isIndustrial =
      containsAnyKeyword(lower, "dark", "aggressive", "industrial", "evil", "metal", "acid", "gritty", "cyber", "cyberpunk")
    val isCosmic =
      containsAnyKeyword(lower, "dreamy", "cosmic", "ethereal", "nebula", "float", "floating", "pad", "calm", "chill", "ambient", "zero-g")
    val isRetro =
      containsAnyKeyword(lower, "retro", "8-bit", "8bit", "arcade", "chip", "chiptune", "pixel", "nes", "gameboy")
    val isHeroic =
      containsAnyKeyword(lower, "heroic", "epic", "anthem", "triumph", "glory", "uplifting", "soaring")
    val isOverdrive =
      containsAnyKeyword(lower, "hyper", "overdrive", "singularity", "boss", "polyrhythm", "bell", "quantum", "chaos")

    val resolvedArchetype =
      when {
        isRetro -> MusicSynthArchetype.RETRO_CHIPTUNE
        isCosmic && !isIndustrial && !isOverdrive -> MusicSynthArchetype.COSMIC_AMBIENT
        isOverdrive -> MusicSynthArchetype.SINGULARITY_OVERDRIVE
        isIndustrial -> MusicSynthArchetype.CYBER_INDUSTRIAL
        else -> MusicSynthArchetype.DARK_SYNTHWAVE
      }

    val scaleIntervals: IntArray
    val moodDescriptor: String
    when {
      isIndustrial -> {
        scaleIntervals = intArrayOf(0, 1, 3, 5, 7, 8, 10) // Phrygian
        moodDescriptor = if (lower.contains("acid")) "Phrygian Acid" else "Industrial Phrygian"
      }
      isCosmic && !isOverdrive -> {
        scaleIntervals = intArrayOf(0, 2, 4, 6, 7, 9, 11) // Lydian
        moodDescriptor = "Cosmic Lydian"
      }
      isHeroic -> {
        scaleIntervals = intArrayOf(0, 2, 3, 5, 7, 9, 10) // Dorian
        moodDescriptor = "Heroic Dorian"
      }
      isOverdrive -> {
        scaleIntervals = intArrayOf(0, 2, 3, 5, 7, 8, 11) // Harmonic Minor
        moodDescriptor = "Singularity Harmonic"
      }
      isRetro -> {
        scaleIntervals = intArrayOf(0, 2, 3, 5, 7, 9, 10) // Arcade Dorian Minor
        moodDescriptor = "8-Bit Arcade"
      }
      else -> {
        scaleIntervals = intArrayOf(0, 2, 3, 5, 7, 8, 10) // Natural Minor
        moodDescriptor = "Neon Synthwave"
      }
    }

    // 2. Tempo / BPM Extraction (60..180 BPM)
    val explicitBpmMatch =
      Regex("\\b(6[0-9]|[7-9][0-9]|1[0-7][0-9]|180)\\s*(?:bpm)?\\b").find(lower)
    val explicitBpm = explicitBpmMatch?.groupValues?.getOrNull(1)?.toIntOrNull()

    val resolvedBpm =
      when {
        explicitBpm != null -> explicitBpm.coerceIn(60, 180)
        containsAnyKeyword(lower, "fast", "intense", "hyper", "overdrive", "boss", "frantic", "rave", "speed", "rapid") ->
          148 + (hashSalt % 18)
        containsAnyKeyword(lower, "slow", "calm", "chill", "ambient", "space", "float", "drift", "meditative", "downtempo", "zero-g") ->
          76 + (hashSalt % 17)
        containsAnyKeyword(lower, "mid", "steady", "groove", "cruise") ->
          110 + (hashSalt % 15)
        currentBpm != null -> currentBpm.coerceIn(60, 180)
        else -> resolvedArchetype.defaultBpm
      }

    // 3. Lead Waveform Selection (0=DetunedSaw, 1=PulseSquare, 2=WarmSinePad, 3=FmBell)
    val leadWaveform =
      when {
        containsAnyKeyword(lower, "bell", "fm", "crystal", "glass", "chime") -> 3
        isRetro || containsAnyKeyword(lower, "square", "pulse", "chip", "8-bit", "8bit") -> 1
        (isCosmic && !isIndustrial) || containsAnyKeyword(lower, "pad", "warm", "sine", "smooth", "soft") -> 2
        else -> 0
      }

    val waveLabel =
      when (leadWaveform) {
        1 -> "Pulse"
        2 -> "Pad"
        3 -> "FM Bell"
        else -> "Saw"
      }

    // 4. Arpeggio Pattern Selection (0=UpDown, 1=OctaveJump, 2=TripletRoll, 3=SparseAmbient)
    val arpPattern =
      when {
        containsAnyKeyword(lower, "sparse", "drone", "pad only", "minimal", "float") -> 3
        containsAnyKeyword(lower, "triplet", "poly", "polyrhythm", "swirl", "spiral", "boss") || isOverdrive -> 2
        containsAnyKeyword(lower, "octave", "acid", "bounce", "arcade", "8-bit", "chip") || isRetro -> 1
        else -> 0
      }

    // 5. Drums & Bass Drive Selection
    val disableDrums =
      containsAnyKeyword(lower, "no drums", "no drum", "pad only", "beatless", "drumless", "no beat", "ambient") &&
        !containsAnyKeyword(lower, "with drums", "heavy drums")
    val heavyDrums =
      containsAnyKeyword(lower, "heavy drums", "techno", "percussion", "kick", "drums", "industrial", "hard")

    val drumsEnabled =
      when {
        disableDrums -> false
        heavyDrums -> true
        resolvedArchetype == MusicSynthArchetype.COSMIC_AMBIENT -> false
        else -> true
      }

    val bassDrive =
      when {
        heavyDrums || isIndustrial || lower.contains("acid") || lower.contains("bass") -> 1.0f
        !drumsEnabled -> 0.52f
        isOverdrive -> 0.92f
        else -> 0.80f
      }

    val brightness =
      when {
        containsAnyKeyword(lower, "bright", "neon", "laser", "acid", "arcade", "bell") -> 0.88f
        containsAnyKeyword(lower, "warm", "deep", "mellow", "sub", "dark") && !lower.contains("acid") -> 0.50f
        else -> 0.68f
      }

    // 6. Musical Root Note (C2=36 .. A2=45)
    val rootMidiNote =
      when {
        Regex("\\bc#\\b|\\bdb\\b").containsMatchIn(lower) -> 37
        Regex("\\bc\\b").containsMatchIn(lower) -> 36
        Regex("\\bd#\\b|\\beb\\b").containsMatchIn(lower) -> 39
        Regex("\\be\\b").containsMatchIn(lower) -> 40
        Regex("\\bf#\\b|\\bgb\\b").containsMatchIn(lower) -> 42
        Regex("\\bf\\b").containsMatchIn(lower) -> 41
        Regex("\\bg\\b").containsMatchIn(lower) -> 43
        Regex("\\ba\\b").containsMatchIn(lower) -> 45
        else -> intArrayOf(36, 38, 40, 41, 43, 45)[hashSalt % 6]
      }

    val drumNote = if (!drumsEnabled) " (No Drums)" else ""
    val summary = "Forged: $moodDescriptor $waveLabel • $resolvedBpm BPM$drumNote"

    val recipe =
      MusicForgeRecipe(
        archetype = resolvedArchetype,
        bpm = resolvedBpm,
        rootMidiNote = rootMidiNote,
        scaleIntervals = scaleIntervals,
        leadWaveform = leadWaveform,
        arpPattern = arpPattern,
        drumsEnabled = drumsEnabled,
        bassDrive = bassDrive,
        brightness = brightness,
        customPromptSummary = summary,
      )

    musicBpm = resolvedBpm
    currentMusicRecipe = recipe
    val slotIdx = activeTrackSlotIndex.coerceIn(0, 2)
    if (slotIdx in trackSlots.indices) {
      val slotPrefix = "Track ${'A' + slotIdx}"
      trackSlots[slotIdx] = recipe.copy(customPromptSummary = "$slotPrefix: $moodDescriptor $waveLabel • $resolvedBpm BPM$drumNote")
    }
    return recipe
  }

  /**
   * Applies a `SfxForgeRecipe` and immediately re-synthesizes all procedural combat WAV buffers on-device.
   */
  fun applySfxRecipe(recipe: SfxForgeRecipe) {
    sharedSfxRecipe = recipe
    if (!isReleased) {
      bakeAllSfxBuffers(recipe)
    }
  }

  fun synthesizeSfxFromPrompt(rawPrompt: String, variationSalt: Int = 0): SfxForgeRecipe {
    val forged = SfxForgeRecipe.synthesizeFromPrompt(rawPrompt, variationSalt)
    applySfxRecipe(forged)
    return forged
  }

  // 1-Tap Audition Triggers for `Soundtrack & SFX Forge`
  fun previewForgedLaser() {
    if (!isAppForeground || isReleased) return
    val v = sfxVolume.coerceAtLeast(0.45f).coerceIn(0f, 1f)
    soundPool.play(sfxLaser, v, v, 2, 0, 1.0f)
  }

  fun previewForgedExplosion() {
    if (!isAppForeground || isReleased) return
    val v = sfxVolume.coerceAtLeast(0.45f).coerceIn(0f, 1f)
    soundPool.play(sfxExplosion, v, v, 3, 0, 1.0f)
  }

  fun previewForgedShieldHit() {
    if (!isAppForeground || isReleased) return
    val v = sfxVolume.coerceAtLeast(0.45f).coerceIn(0f, 1f)
    soundPool.play(sfxRepel, v, v, 3, 0, 1.0f)
  }

  fun previewForgedPrizeChime() {
    if (!isAppForeground || isReleased) return
    val v = sfxVolume.coerceAtLeast(0.45f).coerceIn(0f, 1f)
    soundPool.play(sfxPrize, v, v, 2, 0, 1.0f)
  }

  fun exportAudioForgeObject(): JSONObject = exportAudioForgeJson()

  fun importAudioForgeObject(obj: JSONObject?) = importAudioForgeJson(obj)

  private fun startAmbientMusicLoop() {
    val musicSampleRate = 22050
    val chunkSamples = 1024
    val minBuf =
      AudioTrack.getMinBufferSize(
        musicSampleRate,
        AudioFormat.CHANNEL_OUT_MONO,
        AudioFormat.ENCODING_PCM_16BIT,
      ).coerceAtLeast(chunkSamples * 4)

    ambientMusicThread =
      Thread(
        {
          var track: AudioTrack? = null
          try {
            val musicAttrs =
              AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
            val format =
              AudioFormat.Builder()
                .setSampleRate(musicSampleRate)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build()
            track =
              AudioTrack.Builder()
                .setAudioAttributes(musicAttrs)
                .setAudioFormat(format)
                .setBufferSizeInBytes(minBuf)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
            activeAudioTrack = track
            if (isAppForeground && musicEnabled) {
              track.play()
            }

            val progressionScaleIndices = intArrayOf(0, 5, 3, 4)
            val upDownDegrees = intArrayOf(0, 2, 4, 7, 4, 2, 0, 4)
            val octaveJumpDegrees = intArrayOf(0, 7, 2, 9, 4, 11, 2, 7)
            val tripletDegrees = intArrayOf(0, 2, 4, 6, 4, 2)
            val sparseDegrees = intArrayOf(0, 4, 7, 9)

            val pcmChunk = ShortArray(chunkSamples)
            var sampleInSixteenth = 0
            var sixteenthStepCounter = 0L
            var globalSampleCounter = 0L

            var phaseBassA = 0.0
            var phaseBassB = 0.0
            var phasePad1 = 0.0
            var phasePad2 = 0.0
            var phaseLeadA = 0.0
            var phaseLeadB = 0.0
            var phaseKick = 0.0
            var hatFilterState = 0.0
            var smoothedGain = musicVolume.toDouble()

            val twoPi = 2.0 * PI
            val invRate = 1.0 / musicSampleRate.toDouble()
            val chunkDurationNs = (chunkSamples * 1_000_000_000L) / musicSampleRate
            val maxAheadBufferNs = chunkDurationNs * 6L
            var virtualPlaybackHeadNs = System.nanoTime()

            while (!isReleased) {
              if (!isAppForeground) {
                if (track.playState == AudioTrack.PLAYSTATE_PLAYING) {
                  runCatching {
                    track.pause()
                    track.flush()
                  }
                }
                Thread.sleep(60L)
                virtualPlaybackHeadNs = System.nanoTime()
                continue
              }
              val volTarget = musicVolume.coerceIn(0f, 1f).toDouble()
              if (!musicEnabled || volTarget <= 0.001) {
                if (track.playState == AudioTrack.PLAYSTATE_PLAYING) {
                  runCatching {
                    track.pause()
                    track.flush()
                  }
                }
                Thread.sleep(50L)
                virtualPlaybackHeadNs = System.nanoTime()
                continue
              }

              val recipe = currentMusicRecipe
              val liveBpm = musicBpm.coerceIn(60, 180)
              val samplesPerSixteenth =
                (musicSampleRate * 60.0 / (liveBpm * 4.0)).toInt().coerceAtLeast(64)
              val scale =
                if (recipe.scaleIntervals.isNotEmpty()) recipe.scaleIntervals
                else intArrayOf(0, 2, 3, 5, 7, 8, 10)
              val bassDrive = recipe.bassDrive.coerceIn(0.2f, 1.2f).toDouble()
              val bright = recipe.brightness.coerceIn(0.2f, 1.0f).toDouble()

              for (i in 0 until chunkSamples) {
                globalSampleCounter++
                smoothedGain += (volTarget - smoothedGain) * 0.004

                sampleInSixteenth++
                if (sampleInSixteenth >= samplesPerSixteenth) {
                  sampleInSixteenth = 0
                  sixteenthStepCounter++
                }
                val posIn16th =
                  (sampleInSixteenth.toDouble() / samplesPerSixteenth.toDouble()).coerceIn(0.0, 1.0)

                val stepInBar = (sixteenthStepCounter % 16L).toInt()
                val chordBarIdx = ((sixteenthStepCounter / 16L) % 4L).toInt()
                val chordScaleDeg = progressionScaleIndices[chordBarIdx]

                val chordRootMidi = midiFromScaleDegree(recipe.rootMidiNote, scale, chordScaleDeg)
                val chordThirdMidi = midiFromScaleDegree(recipe.rootMidiNote + 12, scale, chordScaleDeg + 2)
                val chordFifthMidi = midiFromScaleDegree(recipe.rootMidiNote + 12, scale, chordScaleDeg + 4)

                val bassOctaveBoost =
                  if (recipe.drumsEnabled && (stepInBar % 4 == 2)) 12 else 0
                val bassHz = midiToHz(chordRootMidi + bassOctaveBoost)
                val padThirdHz = midiToHz(chordThirdMidi)
                val padFifthHz = midiToHz(chordFifthMidi)

                phaseBassA = (phaseBassA + twoPi * bassHz * invRate) % twoPi
                phaseBassB = (phaseBassB + twoPi * (bassHz * 0.5) * invRate) % twoPi
                val bassSaw = (phaseBassA / PI) - 1.0
                val bassGate =
                  if (recipe.drumsEnabled) {
                    (1.0 - posIn16th * 0.55).coerceIn(0.28, 1.0)
                  } else {
                    0.85
                  }
                val bassSample =
                  (0.58 * sin(phaseBassB) + 0.42 * sin(phaseBassA) + 0.34 * bassDrive * bassSaw) *
                    bassGate *
                    bassDrive

                val tSec = globalSampleCounter * invRate
                phasePad1 = (phasePad1 + twoPi * (padThirdHz * 1.003) * invRate) % twoPi
                phasePad2 = (phasePad2 + twoPi * (padFifthHz * 0.997) * invRate) % twoPi
                val padLfo = 0.65 + 0.35 * sin(twoPi * 0.25 * tSec)
                val padSample =
                  (0.48 * sin(phasePad1) + 0.38 * sin(phasePad2) + 0.18 * bright * sin(phasePad1 * 2.0)) *
                    padLfo

                val arpDegOffset: Int
                val arpTriggerActive: Boolean
                when (recipe.arpPattern) {
                  1 -> {
                    arpDegOffset = octaveJumpDegrees[(sixteenthStepCounter % octaveJumpDegrees.size).toInt()]
                    arpTriggerActive = true
                  }
                  2 -> {
                    arpDegOffset = tripletDegrees[(sixteenthStepCounter % tripletDegrees.size).toInt()]
                    arpTriggerActive = true
                  }
                  3 -> {
                    arpDegOffset = sparseDegrees[((sixteenthStepCounter / 4L) % sparseDegrees.size).toInt()]
                    arpTriggerActive = (stepInBar % 4 == 0)
                  }
                  else -> {
                    arpDegOffset = upDownDegrees[(sixteenthStepCounter % upDownDegrees.size).toInt()]
                    arpTriggerActive = true
                  }
                }

                val arpMidi = midiFromScaleDegree(recipe.rootMidiNote + 24, scale, chordScaleDeg + arpDegOffset)
                val arpHz = midiToHz(arpMidi)
                phaseLeadA = (phaseLeadA + twoPi * arpHz * invRate) % twoPi
                phaseLeadB = (phaseLeadB + twoPi * (arpHz * 1.007) * invRate) % twoPi

                val arpEnv =
                  if (recipe.arpPattern == 3) {
                    if (arpTriggerActive) sin(posIn16th * PI).coerceIn(0.0, 1.0) * 0.75 else 0.22
                  } else {
                    val decayShape = (1.0 - posIn16th).coerceIn(0.0, 1.0)
                    decayShape * (0.35 + 0.65 * decayShape)
                  }

                val rawLeadWave =
                  when (recipe.leadWaveform) {
                    1 -> {
                      val sq1 = if (phaseLeadA < PI * 0.65) 0.75 else -0.75
                      val sq2 = if (phaseLeadB < PI) 0.25 else -0.25
                      sq1 + sq2
                    }
                    2 -> {
                      0.72 * sin(phaseLeadA) + 0.28 * bright * sin(phaseLeadB * 2.0)
                    }
                    3 -> {
                      val modIndex = 1.65 * bright * arpEnv
                      sin(phaseLeadA + modIndex * sin(phaseLeadA * 2.0))
                    }
                    else -> {
                      val sawA = (phaseLeadA / PI) - 1.0
                      val sawB = (phaseLeadB / PI) - 1.0
                      0.45 * (sawA + sawB) * bright + 0.35 * sin(phaseLeadA)
                    }
                  }
                val leadSample = rawLeadWave * arpEnv

                var drumSample = 0.0
                if (recipe.drumsEnabled) {
                  val stepInBeat = stepInBar % 4
                  if (stepInBeat == 0) {
                    val kickEnv = (1.0 - posIn16th).coerceIn(0.0, 1.0).pow(2.2)
                    val kickFreq = 52.0 + 135.0 * (1.0 - posIn16th).pow(3.0)
                    phaseKick = (phaseKick + twoPi * kickFreq * invRate) % twoPi
                    drumSample += sin(phaseKick) * kickEnv * 0.95
                  } else if (stepInBeat == 2) {
                    val hatEnv = (1.0 - posIn16th).coerceIn(0.0, 1.0).pow(3.2)
                    val white = Random.nextDouble(-1.0, 1.0)
                    hatFilterState = hatFilterState * 0.35 + white * 0.65
                    val highPass = white - hatFilterState
                    drumSample += highPass * hatEnv * (0.38 + 0.22 * bright)
                  } else if (recipe.archetype == MusicSynthArchetype.SINGULARITY_OVERDRIVE && stepInBeat == 3) {
                    val ghostEnv = (1.0 - posIn16th).coerceIn(0.0, 1.0).pow(4.0)
                    val white = Random.nextDouble(-1.0, 1.0)
                    drumSample += white * ghostEnv * 0.22
                  }
                }

                val rawMix =
                  bassSample * 0.32 +
                    padSample * (if (recipe.drumsEnabled) 0.24 else 0.38) +
                    leadSample * 0.24 +
                    drumSample * 0.28
                val gained = (rawMix * 0.38 * smoothedGain).coerceIn(-0.98, 0.98)
                pcmChunk[i] = (gained * 32767.0).toInt().toShort()
              }

              if (!isAppForeground) {
                continue
              }
              if (track.playState != AudioTrack.PLAYSTATE_PLAYING) {
                runCatching { track.play() }
              }
              val written = track.write(pcmChunk, 0, chunkSamples)
              val nowNs = System.nanoTime()
              if (written <= 0) {
                Thread.sleep(45L)
                virtualPlaybackHeadNs = System.nanoTime()
              } else {
                if (virtualPlaybackHeadNs < nowNs) {
                  virtualPlaybackHeadNs = nowNs
                }
                virtualPlaybackHeadNs += chunkDurationNs
                val aheadNs = virtualPlaybackHeadNs - nowNs
                if (aheadNs > maxAheadBufferNs) {
                  val sleepMs = ((aheadNs - maxAheadBufferNs) / 1_000_000L).coerceIn(15L, 50L)
                  Thread.sleep(sleepMs)
                }
              }
            }
          } catch (_: Throwable) {
          } finally {
            if (activeAudioTrack === track) {
              activeAudioTrack = null
            }
            try {
              track?.stop()
            } catch (_: Throwable) {}
            try {
              track?.release()
            } catch (_: Throwable) {}
          }
        },
        "RiftAmbientBattleMusic",
      ).apply {
        isDaemon = true
        priority = Thread.MIN_PRIORITY + 1
        start()
      }
  }

  private fun playSfxScaled(soundId: Int, baseVol: Float, priority: Int, rate: Float = 1.0f) {
    if (!isAppForeground || isReleased) return
    val v = (baseVol * sfxVolume.coerceIn(0f, 1f)).coerceIn(0f, 1f)
    if (enabled && v > 0.001f && soundId > 0) {
      soundPool.play(soundId, v, v, priority, 0, rate.coerceIn(0.5f, 2.0f))
    }
  }

  fun playLaser() {
    playSfxScaled(sfxLaser, 0.36f, 1, 1.0f + (Random.nextFloat() - 0.5f) * 0.08f)
  }

  fun playBomb() {
    playSfxScaled(sfxBomb, 0.65f, 2, 1.0f)
  }

  fun playExplosion() {
    playSfxScaled(sfxExplosion, 0.85f, 3, 0.92f + Random.nextFloat() * 0.16f)
  }

  fun playPrize() {
    playSfxScaled(sfxPrize, 0.55f, 2, 1.0f)
  }

  fun playPortal() {
    playSfxScaled(sfxPortal, 0.60f, 2, 1.0f)
  }

  fun playRepel() {
    playSfxScaled(sfxRepel, 0.75f, 3, 1.0f)
  }

  fun playWarp() {
    playSfxScaled(sfxWarp, 0.65f, 2, 1.0f)
  }

  fun playThor() {
    playSfxScaled(sfxThor, 0.90f, 3, 1.0f)
  }

  fun playShotgun() {
    playSfxScaled(sfxShotgun, 0.72f, 2, 0.96f + Random.nextFloat() * 0.08f)
  }

  fun playMissileLockBeep() {
    playSfxScaled(sfxMissileBeep, 0.60f, 2, 1.0f)
  }

  fun playMutedClick() {
    playSfxScaled(sfxMutedClick, 0.45f, 1, 1.0f)
  }

  fun playBounce() {
    playSfxScaled(sfxBounce, 0.42f, 1, 0.95f + Random.nextFloat() * 0.12f)
  }

  fun release() {
    isReleased = true
    activeAudioTrack = null
    if (activeInstance === this) {
      activeInstance = null
    }
    try {
      ambientMusicThread?.interrupt()
    } catch (_: Throwable) {}
    soundPool.release()
  }

  private fun synthSweep(
    fStart: Double,
    fEnd: Double,
    durationSec: Double,
    amp: Double,
    addNoise: Double = 0.0,
    harmonicDrive: Double = 0.0,
  ): ShortArray {
    val count = (sampleRate * durationSec).toInt().coerceAtLeast(1)
    val pcm = ShortArray(count)
    var phase = 0.0
    for (i in 0 until count) {
      val t = i.toDouble() / count
      val env = (1.0 - t).coerceIn(0.0, 1.0)
      val freq = (fStart + (fEnd - fStart) * t).coerceIn(28.0, 9500.0)
      phase += 2.0 * PI * freq / sampleRate
      val fundamental = sin(phase)
      val harmonic = if (harmonicDrive > 0.01) sin(phase * 2.0) * 0.38 * harmonicDrive else 0.0
      val tone = (fundamental + harmonic).coerceIn(-1.1, 1.1)
      val noise = if (addNoise > 0.0) (Random.nextDouble(-1.0, 1.0) * addNoise) else 0.0
      val sample = ((tone * (1.0 - addNoise * 0.65) + noise) * env * amp).coerceIn(-1.0, 1.0)
      pcm[i] = (sample * 32767.0).toInt().toShort()
    }
    return pcm
  }

  private fun synthExplosionBurst(
    durationSec: Double,
    amp: Double,
    subBass: Double = 0.85,
    debrisCrunch: Double = 0.35,
  ): ShortArray {
    val count = (sampleRate * durationSec).toInt().coerceAtLeast(1)
    val pcm = ShortArray(count)
    var lp = 0.0
    var subPhase = 0.0
    val filterAlpha = (0.20 + debrisCrunch * 0.38).coerceIn(0.12, 0.72)
    for (i in 0 until count) {
      val t = i.toDouble() / count
      val env = (1.0 - t) * (1.0 - t)
      val raw = Random.nextDouble(-1.0, 1.0)
      lp = lp * (1.0 - filterAlpha) + raw * filterAlpha
      val subFreq = 44.0 + 95.0 * (1.0 - t).pow(2.4)
      subPhase += 2.0 * PI * subFreq / sampleRate
      val subBoom = sin(subPhase) * (1.0 - t).pow(1.8) * 0.45 * subBass
      val sample = ((lp * 0.78 + subBoom) * env * amp).coerceIn(-1.0, 1.0)
      pcm[i] = (sample * 32767.0).toInt().toShort()
    }
    return pcm
  }

  private fun synthArpeggio(
    notes: DoubleArray,
    durationSec: Double,
    amp: Double,
    brightness: Double = 0.65,
  ): ShortArray {
    val count = (sampleRate * durationSec).toInt().coerceAtLeast(1)
    val pcm = ShortArray(count)
    var phase = 0.0
    for (i in 0 until count) {
      val t = i.toDouble() / count
      val idx = (t * notes.size).toInt().coerceIn(0, notes.lastIndex)
      val freq = notes[idx].coerceIn(80.0, 8800.0)
      val env = (1.0 - t * 0.6).coerceIn(0.0, 1.0)
      phase += 2.0 * PI * freq / sampleRate
      val shimmer = sin(phase * 2.0) * 0.25 * brightness.coerceIn(0.0, 1.25)
      val sample = ((sin(phase) * 0.85 + shimmer) * env * amp).coerceIn(-1.0, 1.0)
      pcm[i] = (sample * 32767.0).toInt().toShort()
    }
    return pcm
  }

  private fun loadWav(cacheDir: File, fileName: String, pcm: ShortArray): Int {
    if (!cacheDir.exists()) {
      cacheDir.mkdirs()
    }
    val file = File(cacheDir, fileName)
    val byteData = ByteBuffer.allocate(44 + pcm.size * 2).order(ByteOrder.LITTLE_ENDIAN)
    val dataSize = pcm.size * 2
    byteData.put("RIFF".toByteArray(Charsets.US_ASCII))
    byteData.putInt(36 + dataSize)
    byteData.put("WAVE".toByteArray(Charsets.US_ASCII))
    byteData.put("fmt ".toByteArray(Charsets.US_ASCII))
    byteData.putInt(16)
    byteData.putShort(1)
    byteData.putShort(1)
    byteData.putInt(sampleRate)
    byteData.putInt(sampleRate * 2)
    byteData.putShort(2)
    byteData.putShort(16)
    byteData.put("data".toByteArray(Charsets.US_ASCII))
    byteData.putInt(dataSize)
    for (s in pcm) {
      byteData.putShort(s)
    }
    FileOutputStream(file).use { it.write(byteData.array()) }
    return soundPool.load(file.absolutePath, 1)
  }

  companion object {
    @Volatile var activeInstance: RiftSoundSynthesizer? = null
    @Volatile private var globalForegroundState: Boolean = true

    fun isAppCurrentlyForeground(): Boolean = globalForegroundState

    fun notifyAppForegroundState(foreground: Boolean) {
      globalForegroundState = foreground
      activeInstance?.setAppForeground(foreground)
    }

    private fun buildDefaultTrackSlots(): List<MusicForgeRecipe> =
      listOf(
        buildArchetypeRecipe(MusicSynthArchetype.DARK_SYNTHWAVE, 128)
          .copy(customPromptSummary = "Track A: Dark Cyber-Pulse • 128 BPM"),
        buildArchetypeRecipe(MusicSynthArchetype.COSMIC_AMBIENT, 96)
          .copy(customPromptSummary = "Track B: Zero-G Drift Pad • 96 BPM"),
        buildArchetypeRecipe(MusicSynthArchetype.SINGULARITY_OVERDRIVE, 148)
          .copy(customPromptSummary = "Track C: Singularity Acid • 148 BPM"),
      )

    val trackSlots: SnapshotStateList<MusicForgeRecipe> =
      mutableStateListOf<MusicForgeRecipe>().apply { addAll(buildDefaultTrackSlots()) }

    var activeTrackSlotIndex by mutableIntStateOf(0)
    var sharedSfxRecipe by mutableStateOf(SfxForgeRecipe())

    fun resetAudioForgeToDefaults() {
      val defaults = buildDefaultTrackSlots()
      trackSlots.clear()
      trackSlots.addAll(defaults)
      activeTrackSlotIndex = 0
      sharedSfxRecipe = SfxForgeRecipe()
      activeInstance?.let { inst ->
        inst.selectTrackSlot(0)
        inst.applySfxRecipe(sharedSfxRecipe)
      }
    }

    fun exportAudioForgeJson(): JSONObject {
      val slotsArr = JSONArray()
      for (i in 0 until 3) {
        val rec = trackSlots.getOrNull(i) ?: buildDefaultTrackSlots()[i]
        slotsArr.put(rec.toJson())
      }
      return JSONObject().apply {
        put("activeTrackSlotIndex", activeTrackSlotIndex.coerceIn(0, 2))
        put("trackSlots", slotsArr)
        put("sfxRecipe", sharedSfxRecipe.toJson())
      }
    }

    fun importAudioForgeJson(obj: JSONObject?) {
      if (obj == null) return
      val defaults = buildDefaultTrackSlots()
      val slotsArr = obj.optJSONArray("trackSlots")
      if (slotsArr != null && slotsArr.length() > 0) {
        val loaded =
          (0 until 3).map { idx ->
            val item = slotsArr.optJSONObject(idx)
            if (item != null) MusicForgeRecipe.fromJson(item) else defaults[idx]
          }
        trackSlots.clear()
        trackSlots.addAll(loaded)
      }
      val selIdx = obj.optInt("activeTrackSlotIndex", 0).coerceIn(0, 2)
      activeTrackSlotIndex = selIdx
      val sfxObj = obj.optJSONObject("sfxRecipe")
      val restoredSfx = SfxForgeRecipe.fromJson(sfxObj)
      sharedSfxRecipe = restoredSfx

      activeInstance?.let { inst ->
        inst.selectTrackSlot(selIdx)
        inst.applySfxRecipe(restoredSfx)
      }
    }

    private fun containsAnyKeyword(lowerText: String, vararg keywords: String): Boolean =
      keywords.any { lowerText.contains(it) }

    private fun midiToHz(midiNote: Int): Double =
      440.0 * 2.0.pow((midiNote - 69) / 12.0)

    private fun midiFromScaleDegree(baseMidi: Int, scaleIntervals: IntArray, degree: Int): Int {
      val len = scaleIntervals.size.coerceAtLeast(1)
      val octaves = Math.floorDiv(degree, len)
      val idx = Math.floorMod(degree, len)
      return (baseMidi + octaves * 12 + scaleIntervals[idx]).coerceIn(24, 96)
    }

    fun buildArchetypeRecipe(
      archetype: MusicSynthArchetype,
      bpm: Int = archetype.defaultBpm,
    ): MusicForgeRecipe {
      val clampedBpm = bpm.coerceIn(60, 180)
      return when (archetype) {
        MusicSynthArchetype.DARK_SYNTHWAVE ->
          MusicForgeRecipe(
            archetype = archetype,
            bpm = clampedBpm,
            rootMidiNote = 38, // D2
            scaleIntervals = intArrayOf(0, 2, 3, 5, 7, 8, 10), // Natural Minor
            leadWaveform = 0, // DetunedSaw
            arpPattern = 0, // UpDown
            drumsEnabled = true,
            bassDrive = 0.80f,
            brightness = 0.68f,
            customPromptSummary = "${archetype.label} • $clampedBpm BPM",
          )

        MusicSynthArchetype.CYBER_INDUSTRIAL ->
          MusicForgeRecipe(
            archetype = archetype,
            bpm = clampedBpm,
            rootMidiNote = 40, // E2
            scaleIntervals = intArrayOf(0, 1, 3, 5, 7, 8, 10), // Phrygian
            leadWaveform = 0, // DetunedSaw
            arpPattern = 1, // OctaveJump
            drumsEnabled = true,
            bassDrive = 0.98f,
            brightness = 0.80f,
            customPromptSummary = "${archetype.label} • $clampedBpm BPM",
          )

        MusicSynthArchetype.COSMIC_AMBIENT ->
          MusicForgeRecipe(
            archetype = archetype,
            bpm = clampedBpm,
            rootMidiNote = 41, // F2
            scaleIntervals = intArrayOf(0, 2, 4, 6, 7, 9, 11), // Lydian
            leadWaveform = 2, // WarmSinePad
            arpPattern = 3, // SparseAmbient
            drumsEnabled = false,
            bassDrive = 0.52f,
            brightness = 0.48f,
            customPromptSummary = "${archetype.label} • $clampedBpm BPM",
          )

        MusicSynthArchetype.RETRO_CHIPTUNE ->
          MusicForgeRecipe(
            archetype = archetype,
            bpm = clampedBpm,
            rootMidiNote = 45, // A2
            scaleIntervals = intArrayOf(0, 2, 3, 5, 7, 9, 10), // Dorian Minor
            leadWaveform = 1, // PulseSquare
            arpPattern = 1, // OctaveJump
            drumsEnabled = true,
            bassDrive = 0.76f,
            brightness = 0.88f,
            customPromptSummary = "${archetype.label} • $clampedBpm BPM",
          )

        MusicSynthArchetype.SINGULARITY_OVERDRIVE ->
          MusicForgeRecipe(
            archetype = archetype,
            bpm = clampedBpm,
            rootMidiNote = 38, // D2
            scaleIntervals = intArrayOf(0, 2, 3, 5, 7, 8, 11), // Harmonic Minor
            leadWaveform = 3, // FmBell
            arpPattern = 2, // TripletRoll
            drumsEnabled = true,
            bassDrive = 0.94f,
            brightness = 0.86f,
            customPromptSummary = "${archetype.label} • $clampedBpm BPM",
          )
      }
    }
  }
}
