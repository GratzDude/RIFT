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

            // 4-Chord Dark Space Synth Progression (D minor -> Bb major -> G minor -> A minor)
            // Fundamental frequencies in Hz (D2, Bb1, G1, A1)
            val roots = doubleArrayOf(73.42, 58.27, 49.00, 55.00)
            // Fifths in Hz (A2, F2, D2, E2)
            val fifths = doubleArrayOf(110.00, 87.31, 73.42, 82.41)
            // Minor/Major Thirds an octave up for atmospheric pad (F3, D3, Bb2, C3)
            val thirds = doubleArrayOf(174.61, 146.83, 116.54, 130.81)
            // High atmospheric shimmer (A3, F3, D3, E3)
            val shimmers = doubleArrayOf(220.00, 174.61, 146.83, 164.81)

            val pcmChunk = ShortArray(chunkSamples)
            var globalSampleIndex = 0L
            var phaseSub = 0.0
            var phaseFifth = 0.0
            var phasePad1 = 0.0
            var phasePad2 = 0.0
            var phasePulse = 0.0
            val twoPi = 2.0 * PI
            val invRate = 1.0 / musicSampleRate.toDouble()

            while (!isReleased) {
              if (!musicEnabled || !enabled) {
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
                val rootHz = roots[chordIdx]
                val fifthHz = fifths[chordIdx]
                val thirdHz = thirds[chordIdx]
                val shimmerHz = shimmers[chordIdx]

                // Smooth chord crossfade envelope at bar boundaries
                val posInBar = tSec % 4.0
                val barEnv =
                  when {
                    posInBar < 0.18 -> (posInBar / 0.18)
                    posInBar > 3.82 -> ((4.0 - posInBar) / 0.18)
                    else -> 1.0
                  }.coerceIn(0.15, 1.0)

                // Layer 1: Warm Sub-Bass Drone (root + octave + fifth)
                phaseSub = (phaseSub + twoPi * rootHz * invRate) % twoPi
                phaseFifth = (phaseFifth + twoPi * fifthHz * invRate) % twoPi
                val subLayer = 0.52 * sin(phaseSub) + 0.26 * sin(phaseSub * 2.0) + 0.22 * sin(phaseFifth)

                // Layer 2: Atmospheric Minor-Key Synth Pad with slow LFO filter modulation
                phasePad1 = (phasePad1 + twoPi * (thirdHz * 1.002) * invRate) % twoPi
                phasePad2 = (phasePad2 + twoPi * (shimmerHz * 0.998) * invRate) % twoPi
                val lfo = 0.55 + 0.45 * sin(twoPi * 0.18 * tSec)
                val padLayer = (0.45 * sin(phasePad1) + 0.35 * sin(phasePad2) + 0.20 * sin(phasePad1 * 0.5)) * lfo

                // Layer 3: Soft Tactical Heartbeat Pulse on Beats 1 and 3 (90 BPM -> 1.5 beats/sec, beat = 0.6667s)
                val beatPeriod = 60.0 / 90.0 // 0.6667s per beat
                val twoBeatPeriod = beatPeriod * 2.0 // 1.3333s (hits on beats 1 and 3)
                val beatPos = tSec % twoBeatPeriod
                val pulseEnv = if (beatPos < 0.22) (1.0 - beatPos / 0.22) * (1.0 - beatPos / 0.22) else 0.0
                val pulseFreq = 95.0 - 42.0 * (beatPos / 0.22).coerceIn(0.0, 1.0)
                phasePulse = (phasePulse + twoPi * pulseFreq * invRate) % twoPi
                val pulseLayer = sin(phasePulse) * pulseEnv * 0.55

                // Mix at subtle 0.12f master gain so combat SFX remain crisp in the foreground
                val mixed = ((subLayer * 0.46 + padLayer * 0.34 + pulseLayer * 0.28) * barEnv) * 0.12
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
