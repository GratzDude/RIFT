package com.example.engine

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.SoundPool
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/**
 * Zero-Latency Procedural Retro Space Audio Synthesizer + Ambient Battle Soundtrack.
 * Pre-generates crisp 16-bit PCM WAV buffers at boot for SoundPool SFX and runs a lightweight
 * background streaming AudioTrack thread for procedural dark synthwave ambient battle music.
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

            // 4-Chord Tactical Space Synth Progression (D minor -> Bb major -> G minor -> A minor)
            // Audible Bass Fundamentals & Octaves in Hz (D3/D2, Bb2/Bb1, G2/G1, A2/A1)
            val bassRoots = doubleArrayOf(146.83, 116.54, 98.00, 110.00)
            // Power Fifths in Hz (A3, F3, D3, E3)
            val fifths = doubleArrayOf(220.00, 174.61, 146.83, 164.81)
            // Warm Mid-Range Synth Pad Thirds (F4, D4, Bb3, C4)
            val thirds = doubleArrayOf(349.23, 293.66, 233.08, 261.63)
            // High Harmonic Shimmer (A4, F4, D4, E4)
            val shimmers = doubleArrayOf(440.00, 349.23, 293.66, 329.63)
            // 4-Step 16th-Note Arpeggio Ratios per chord (Root, Third, Fifth, Octave)
            val arpSteps =
              arrayOf(
                doubleArrayOf(293.66, 349.23, 440.00, 587.33),
                doubleArrayOf(233.08, 293.66, 349.23, 466.16),
                doubleArrayOf(196.00, 233.08, 293.66, 392.00),
                doubleArrayOf(220.00, 261.63, 329.63, 440.00),
              )

            val pcmChunk = ShortArray(chunkSamples)
            var globalSampleIndex = 0L
            var phaseSub = 0.0
            var phaseFifth = 0.0
            var phasePad1 = 0.0
            var phasePad2 = 0.0
            var phaseArp = 0.0
            var phasePulse = 0.0
            val twoPi = 2.0 * PI
            val invRate = 1.0 / musicSampleRate.toDouble()

            while (!isReleased) {
              if (!musicEnabled) {
                for (i in 0 until chunkSamples) pcmChunk[i] = 0
                track.write(pcmChunk, 0, chunkSamples)
                Thread.sleep(60L)
                continue
              }

              for (i in 0 until chunkSamples) {
                val tSec = globalSampleIndex * invRate
                globalSampleIndex++

                // Change chord every 4.0 seconds (16-second 4-bar cycle at 90 BPM)
                val chordIdx = ((tSec / 4.0).toInt()) and 3
                val rootHz = bassRoots[chordIdx]
                val fifthHz = fifths[chordIdx]
                val thirdHz = thirds[chordIdx]
                val shimmerHz = shimmers[chordIdx]

                // Smooth chord crossfade envelope at bar boundaries
                val posInBar = tSec % 4.0
                val barEnv =
                  when {
                    posInBar < 0.16 -> (posInBar / 0.16)
                    posInBar > 3.84 -> ((4.0 - posInBar) / 0.16)
                    else -> 1.0
                  }.coerceIn(0.25, 1.0)

                // Layer 1: Rich Harmonic Bass Drone (fundamental + octave + 2nd harmonic)
                phaseSub = (phaseSub + twoPi * rootHz * invRate) % twoPi
                phaseFifth = (phaseFifth + twoPi * fifthHz * invRate) % twoPi
                val subLayer =
                  0.48 * sin(phaseSub) +
                    0.32 * sin(phaseSub * 2.0) +
                    0.20 * sin(phaseFifth)

                // Layer 2: Warm Mid-Range Synth Pad with slow LFO modulation (233 - 440 Hz)
                phasePad1 = (phasePad1 + twoPi * (thirdHz * 1.003) * invRate) % twoPi
                phasePad2 = (phasePad2 + twoPi * (shimmerHz * 0.997) * invRate) % twoPi
                val lfo = 0.62 + 0.38 * sin(twoPi * 0.22 * tSec)
                val padLayer =
                  (0.45 * sin(phasePad1) +
                    0.35 * sin(phasePad2) +
                    0.20 * sin(phasePad1 * 2.0)) * lfo

                // Layer 3: Crisp 90 BPM 8th-Note Space Synth Arpeggio (0.3333s per step)
                val stepDur = 60.0 / 90.0 / 2.0 // 0.3333s
                val stepIndex = ((tSec / stepDur).toInt()) and 3
                val posInStep = (tSec % stepDur) / stepDur
                val arpEnv = (1.0 - posInStep).coerceIn(0.0, 1.0) * (1.0 - posInStep * 0.65)
                val arpHz = arpSteps[chordIdx][stepIndex]
                phaseArp = (phaseArp + twoPi * arpHz * invRate) % twoPi
                val arpLayer = (0.70 * sin(phaseArp) + 0.30 * sin(phaseArp * 2.0)) * arpEnv

                // Layer 4: Punchy Tactical Beat Pulse on every beat (90 BPM -> 0.6667s)
                val beatPeriod = 60.0 / 90.0
                val beatPos = tSec % beatPeriod
                val pulseEnv = if (beatPos < 0.18) (1.0 - beatPos / 0.18) * (1.0 - beatPos / 0.18) else 0.0
                val pulseFreq = 165.0 - 75.0 * (beatPos / 0.18).coerceIn(0.0, 1.0)
                phasePulse = (phasePulse + twoPi * pulseFreq * invRate) % twoPi
                val pulseLayer = (0.65 * sin(phasePulse) + 0.35 * sin(phasePulse * 2.0)) * pulseEnv

                // Balanced 0.22 master gain so music is clearly audible on speakers while keeping SFX crisp
                val mixed =
                  ((subLayer * 0.34 + padLayer * 0.30 + arpLayer * 0.22 + pulseLayer * 0.20) * barEnv) * 0.22
                pcmChunk[i] = (mixed.coerceIn(-1.0, 1.0) * 32767.0).toInt().toShort()
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

  fun playLaser() {
    if (enabled) soundPool.play(sfxLaser, 0.36f, 0.36f, 1, 0, 1.0f + (Random.nextFloat() - 0.5f) * 0.08f)
  }

  fun playBomb() {
    if (enabled) soundPool.play(sfxBomb, 0.65f, 0.65f, 2, 0, 1.0f)
  }

  fun playExplosion() {
    if (enabled) soundPool.play(sfxExplosion, 0.85f, 0.85f, 3, 0, 0.92f + Random.nextFloat() * 0.16f)
  }

  fun playPrize() {
    if (enabled) soundPool.play(sfxPrize, 0.55f, 0.55f, 2, 0, 1.0f)
  }

  fun playPortal() {
    if (enabled) soundPool.play(sfxPortal, 0.60f, 0.60f, 2, 0, 1.0f)
  }

  fun playRepel() {
    if (enabled) soundPool.play(sfxRepel, 0.75f, 0.75f, 3, 0, 1.0f)
  }

  fun playWarp() {
    if (enabled) soundPool.play(sfxWarp, 0.65f, 0.65f, 2, 0, 1.0f)
  }

  fun playThor() {
    if (enabled) soundPool.play(sfxThor, 0.90f, 0.90f, 3, 0, 1.0f)
  }

  fun playShotgun() {
    if (enabled) soundPool.play(sfxShotgun, 0.72f, 0.72f, 2, 0, 0.96f + Random.nextFloat() * 0.08f)
  }

  fun playMissileLockBeep() {
    if (enabled) soundPool.play(sfxMissileBeep, 0.60f, 0.60f, 2, 0, 1.0f)
  }

  fun playMutedClick() {
    if (enabled) soundPool.play(sfxMutedClick, 0.45f, 0.45f, 1, 0, 1.0f)
  }

  fun playBounce() {
    if (enabled) soundPool.play(sfxBounce, 0.42f, 0.42f, 1, 0, 0.95f + Random.nextFloat() * 0.12f)
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
}
