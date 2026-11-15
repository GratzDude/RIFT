package com.example.engine

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.SoundPool
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

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
}

/**
 * Zero-Latency Procedural Space Audio Synthesizer + Real-Time 60–180 BPM Battle Soundtrack.
 * Pre-generates crisp 16-bit PCM WAV buffers at boot for SoundPool SFX and runs a phase-continuous
 * streaming AudioTrack thread that responds instantaneously to live BPM, volume, genre archetype,
 * and 100% local on-device NEXUS Music Forge prompts with zero network calls and zero audio dropouts.
 */
class RiftSoundSynthesizer(
  cacheDir: File = File(System.getProperty("java.io.tmpdir") ?: "/data/local/tmp"),
) {
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
  @Volatile var musicBpm: Int = 118
  @Volatile var currentMusicRecipe: MusicForgeRecipe = buildArchetypeRecipe(MusicSynthArchetype.DARK_SYNTHWAVE, 118)

  @Volatile private var isReleased: Boolean = false
  private var ambientMusicThread: Thread? = null

  private val sfxLaser: Int
  private val sfxBomb: Int
  private val sfxExplosion: Int
  private val sfxPrize: Int
  private val sfxPortal: Int
  private val sfxRepel: Int
  private val sfxWarp: Int
  private val sfxThor: Int
  private val sfxShotgun: Int
  private val sfxMissileBeep: Int
  private val sfxMutedClick: Int
  private val sfxBounce: Int

  init {
    val attrs =
      AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_GAME)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()
    soundPool = SoundPool.Builder().setMaxStreams(10).setAudioAttributes(attrs).build()

    sfxLaser = loadWav(cacheDir, "sfx_laser.wav", synthSweep(980.0, 220.0, 0.085, 0.45))
    sfxBomb = loadWav(cacheDir, "sfx_bomb.wav", synthSweep(320.0, 65.0, 0.24, 0.75, addNoise = 0.25))
    sfxExplosion = loadWav(cacheDir, "sfx_exp.wav", synthNoiseBurst(0.36, 0.85))
    sfxPrize = loadWav(cacheDir, "sfx_prize.wav", synthArpeggio(doubleArrayOf(523.25, 659.25, 783.99, 1046.5), 0.16, 0.50))
    sfxPortal = loadWav(cacheDir, "sfx_portal.wav", synthSweep(240.0, 880.0, 0.22, 0.55))
    sfxRepel = loadWav(cacheDir, "sfx_repel.wav", synthSweep(150.0, 740.0, 0.25, 0.70, addNoise = 0.18))
    sfxWarp = loadWav(cacheDir, "sfx_warp.wav", synthSweep(660.0, 180.0, 0.20, 0.55))
    sfxThor = loadWav(cacheDir, "sfx_thor.wav", synthSweep(140.0, 42.0, 0.34, 0.90, addNoise = 0.40))
    sfxShotgun = loadWav(cacheDir, "sfx_shotgun.wav", synthSweep(420.0, 95.0, 0.14, 0.75, addNoise = 0.55))
    sfxMissileBeep = loadWav(cacheDir, "sfx_lock_beep.wav", synthArpeggio(doubleArrayOf(1320.0, 1760.0), 0.11, 0.55))
    sfxMutedClick = loadWav(cacheDir, "sfx_muted_click.wav", synthSweep(190.0, 110.0, 0.055, 0.38))
    sfxBounce = loadWav(cacheDir, "sfx_bounce.wav", synthSweep(360.0, 580.0, 0.065, 0.48))

    startAmbientMusicLoop()
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
    currentMusicRecipe =
      prev.copy(
        bpm = clamped,
        customPromptSummary = "$baseTitle • $clamped BPM$drumSuffix",
      )
  }

  fun setMusicVolumeLive(vol: Float) {
    musicVolume = vol.coerceIn(0f, 1f)
  }

  fun setSfxVolumeLive(vol: Float) {
    sfxVolume = vol.coerceIn(0f, 1f)
  }

  fun applyMusicArchetype(
    archetype: MusicSynthArchetype,
    overrideBpm: Int? = null,
  ): MusicForgeRecipe {
    val targetBpm = (overrideBpm ?: archetype.defaultBpm).coerceIn(60, 180)
    val recipe = buildArchetypeRecipe(archetype, targetBpm)
    musicBpm = targetBpm
    currentMusicRecipe = recipe
    return recipe
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
      containsAnyKeyword(lower, "dreamy", "cosmic", "ethereal", "nebula", "float", "floating", "pad", "calm", "chill", "ambient")
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
          148 + (hashSalt % 18) // 148..165 BPM
        containsAnyKeyword(lower, "slow", "calm", "chill", "ambient", "space", "float", "drift", "meditative", "downtempo") ->
          76 + (hashSalt % 17) // 76..92 BPM
        containsAnyKeyword(lower, "mid", "steady", "groove", "cruise") ->
          110 + (hashSalt % 15) // 110..124 BPM
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
    return recipe
  }

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
            track.play()

            // Chord root scale degree offsets across a 4-bar tactical progression (i -> VI -> iv -> v / VII)
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

            while (!isReleased) {
              val volTarget = musicVolume.coerceIn(0f, 1f).toDouble()
              if (!musicEnabled || volTarget <= 0.001) {
                for (i in 0 until chunkSamples) pcmChunk[i] = 0
                track.write(pcmChunk, 0, chunkSamples)
                Thread.sleep(50L)
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

                // 16 sixteenth-notes per 4/4 bar; chord advances every bar (4 bars total)
                val stepInBar = (sixteenthStepCounter % 16L).toInt()
                val chordBarIdx = ((sixteenthStepCounter / 16L) % 4L).toInt()
                val chordScaleDeg = progressionScaleIndices[chordBarIdx]

                val chordRootMidi = midiFromScaleDegree(recipe.rootMidiNote, scale, chordScaleDeg)
                val chordThirdMidi = midiFromScaleDegree(recipe.rootMidiNote + 12, scale, chordScaleDeg + 2)
                val chordFifthMidi = midiFromScaleDegree(recipe.rootMidiNote + 12, scale, chordScaleDeg + 4)

                // Bassline: off-beat octave bounce on Industrial/Synthwave/Overdrive, deep sub-drone on Ambient
                val bassOctaveBoost =
                  if (recipe.drumsEnabled && (stepInBar % 4 == 2)) 12 else 0
                val bassHz = midiToHz(chordRootMidi + bassOctaveBoost)
                val padThirdHz = midiToHz(chordThirdMidi)
                val padFifthHz = midiToHz(chordFifthMidi)

                // Layer 1: Sculpted Bass (Sine sub + sawtooth harmonic grit scaled by bassDrive)
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

                // Layer 2: Lush Stereo-Detuned Space Pad
                val tSec = globalSampleCounter * invRate
                phasePad1 = (phasePad1 + twoPi * (padThirdHz * 1.003) * invRate) % twoPi
                phasePad2 = (phasePad2 + twoPi * (padFifthHz * 0.997) * invRate) % twoPi
                val padLfo = 0.65 + 0.35 * sin(twoPi * 0.25 * tSec)
                val padSample =
                  (0.48 * sin(phasePad1) + 0.38 * sin(phasePad2) + 0.18 * bright * sin(phasePad1 * 2.0)) *
                    padLfo

                // Layer 3: Patterned Arpeggio & Lead Waveform Synthesizer
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
                    // Sparse ambient: triggers gently every 4 sixteenth-notes (quarter-note shimmer)
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
                      // Crisp Arcade Pulse Square (25%/50% duty blend)
                      val sq1 = if (phaseLeadA < PI * 0.65) 0.75 else -0.75
                      val sq2 = if (phaseLeadB < PI) 0.25 else -0.25
                      sq1 + sq2
                    }
                    2 -> {
                      // Warm Sine + Octave Shimmer Pad
                      0.72 * sin(phaseLeadA) + 0.28 * bright * sin(phaseLeadB * 2.0)
                    }
                    3 -> {
                      // Glassy FM Bell Synth
                      val modIndex = 1.65 * bright * arpEnv
                      sin(phaseLeadA + modIndex * sin(phaseLeadA * 2.0))
                    }
                    else -> {
                      // Detuned Dual-Sawtooth Synthwave Lead
                      val sawA = (phaseLeadA / PI) - 1.0
                      val sawB = (phaseLeadB / PI) - 1.0
                      0.45 * (sawA + sawB) * bright + 0.35 * sin(phaseLeadA)
                    }
                  }
                val leadSample = rawLeadWave * arpEnv

                // Layer 4: Punchy Tactical Drums (4-on-the-floor Kick + Crisp Off-beat Hi-Hat)
                var drumSample = 0.0
                if (recipe.drumsEnabled) {
                  val stepInBeat = stepInBar % 4
                  if (stepInBeat == 0) {
                    // Kick on every quarter-note beat
                    val kickEnv = (1.0 - posIn16th).coerceIn(0.0, 1.0).pow(2.2)
                    val kickFreq = 52.0 + 135.0 * (1.0 - posIn16th).pow(3.0)
                    phaseKick = (phaseKick + twoPi * kickFreq * invRate) % twoPi
                    drumSample += sin(phaseKick) * kickEnv * 0.95
                  } else if (stepInBeat == 2) {
                    // Crisp off-beat hi-hat / snare tick
                    val hatEnv = (1.0 - posIn16th).coerceIn(0.0, 1.0).pow(3.2)
                    val white = Random.nextDouble(-1.0, 1.0)
                    hatFilterState = hatFilterState * 0.35 + white * 0.65
                    val highPass = white - hatFilterState
                    drumSample += highPass * hatEnv * (0.38 + 0.22 * bright)
                  } else if (recipe.archetype == MusicSynthArchetype.SINGULARITY_OVERDRIVE && stepInBeat == 3) {
                    // Ghost 16th-note percussion tick in Overdrive
                    val ghostEnv = (1.0 - posIn16th).coerceIn(0.0, 1.0).pow(4.0)
                    val white = Random.nextDouble(-1.0, 1.0)
                    drumSample += white * ghostEnv * 0.22
                  }
                }

                // Master mix scaled smoothly by user musicVolume (0.0..1.0)
                val rawMix =
                  bassSample * 0.32 +
                    padSample * (if (recipe.drumsEnabled) 0.24 else 0.38) +
                    leadSample * 0.24 +
                    drumSample * 0.28
                val gained = (rawMix * 0.38 * smoothedGain).coerceIn(-0.98, 0.98)
                pcmChunk[i] = (gained * 32767.0).toInt().toShort()
              }

              track.write(pcmChunk, 0, chunkSamples)
            }
          } catch (_: Throwable) {
            // AudioTrack gracefully exits if interrupted or released
          } finally {
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
    val v = (baseVol * sfxVolume.coerceIn(0f, 1f)).coerceIn(0f, 1f)
    if (enabled && v > 0.001f) {
      soundPool.play(soundId, v, v, priority, 0, rate)
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
  ): ShortArray {
    val count = (sampleRate * durationSec).toInt().coerceAtLeast(1)
    val pcm = ShortArray(count)
    var phase = 0.0
    for (i in 0 until count) {
      val t = i.toDouble() / count
      val env = (1.0 - t).coerceIn(0.0, 1.0)
      val freq = fStart + (fEnd - fStart) * t
      phase += 2.0 * PI * freq / sampleRate
      val tone = sin(phase)
      val noise = if (addNoise > 0.0) (Random.nextDouble(-1.0, 1.0) * addNoise) else 0.0
      val sample = ((tone * (1.0 - addNoise) + noise) * env * amp).coerceIn(-1.0, 1.0)
      pcm[i] = (sample * 32767.0).toInt().toShort()
    }
    return pcm
  }

  private fun synthNoiseBurst(durationSec: Double, amp: Double): ShortArray {
    val count = (sampleRate * durationSec).toInt().coerceAtLeast(1)
    val pcm = ShortArray(count)
    var lp = 0.0
    for (i in 0 until count) {
      val t = i.toDouble() / count
      val env = (1.0 - t) * (1.0 - t)
      val raw = Random.nextDouble(-1.0, 1.0)
      lp = lp * 0.72 + raw * 0.28
      val sample = (lp * env * amp).coerceIn(-1.0, 1.0)
      pcm[i] = (sample * 32767.0).toInt().toShort()
    }
    return pcm
  }

  private fun synthArpeggio(notes: DoubleArray, durationSec: Double, amp: Double): ShortArray {
    val count = (sampleRate * durationSec).toInt().coerceAtLeast(1)
    val pcm = ShortArray(count)
    var phase = 0.0
    for (i in 0 until count) {
      val t = i.toDouble() / count
      val idx = (t * notes.size).toInt().coerceIn(0, notes.lastIndex)
      val freq = notes[idx]
      val env = (1.0 - t * 0.6).coerceIn(0.0, 1.0)
      phase += 2.0 * PI * freq / sampleRate
      val sample = (sin(phase) * env * amp).coerceIn(-1.0, 1.0)
      pcm[i] = (sample * 32767.0).toInt().toShort()
    }
    return pcm
  }

  private fun loadWav(cacheDir: File, fileName: String, pcm: ShortArray): Int {
    val file = File(cacheDir, fileName)
    val byteData = ByteBuffer.allocate(44 + pcm.size * 2).order(ByteOrder.LITTLE_ENDIAN)
    val dataSize = pcm.size * 2
    byteData.put("RIFF".toByteArray(Charsets.US_ASCII))
    byteData.putInt(36 + dataSize)
    byteData.put("WAVE".toByteArray(Charsets.US_ASCII))
    byteData.put("fmt ".toByteArray(Charsets.US_ASCII))
    byteData.putInt(16) // PCM chunk size
    byteData.putShort(1) // PCM format
    byteData.putShort(1) // Mono
    byteData.putInt(sampleRate)
    byteData.putInt(sampleRate * 2)
    byteData.putShort(2) // Block align
    byteData.putShort(16) // Bits per sample
    byteData.put("data".toByteArray(Charsets.US_ASCII))
    byteData.putInt(dataSize)
    for (s in pcm) {
      byteData.putShort(s)
    }
    FileOutputStream(file).use { it.write(byteData.array()) }
    return soundPool.load(file.absolutePath, 1)
  }

  companion object {
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
