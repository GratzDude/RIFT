package com.example.engine

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import java.util.concurrent.Executors
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

class RiftSoundSynthesizer {
  var enabled: Boolean = true
  private val audioExecutor = Executors.newSingleThreadExecutor()
  private val sampleRate = 22050

  private val laserBuffer: ShortArray = buildSweepBuffer(880.0, 210.0, 0.085, 0.25)
  private val bombBuffer: ShortArray = buildSweepBuffer(220.0, 48.0, 0.22, 0.42)
  private val bounceBuffer: ShortArray = buildSweepBuffer(145.0, 42.0, 0.09, 0.35)
  private val prizeBuffer: ShortArray = buildChordArpeggioBuffer()
  private val explosionBuffer: ShortArray = buildNoiseExplosionBuffer(0.36, 0.45)
  private val repelBuffer: ShortArray = buildSweepBuffer(540.0, 95.0, 0.24, 0.38)
  private val warpBuffer: ShortArray = buildSweepBuffer(240.0, 980.0, 0.25, 0.32)
  private val mutedClickBuffer: ShortArray = buildSweepBuffer(180.0, 110.0, 0.04, 0.16)
  private val shotgunBuffer: ShortArray = buildNoiseExplosionBuffer(0.14, 0.38)
  private val missileLockBeepBuffer: ShortArray = buildSweepBuffer(1320.0, 1480.0, 0.065, 0.28)

  private fun buildSweepBuffer(
    startFreq: Double,
    endFreq: Double,
    durationSec: Double,
    volume: Double,
  ): ShortArray {
    val count = (sampleRate * durationSec).toInt().coerceAtLeast(1)
    val out = ShortArray(count)
    var phase = 0.0
    for (i in 0 until count) {
      val t = i.toDouble() / count.toDouble()
      val freq = startFreq + (endFreq - startFreq) * t
      phase += (2.0 * PI * freq) / sampleRate
      val env = (1.0 - t) * exp(-2.2 * t)
      val sample = (sin(phase) * volume * env * Short.MAX_VALUE).toInt()
      out[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
    }
    return out
  }

  private fun buildChordArpeggioBuffer(): ShortArray {
    val notes = doubleArrayOf(523.25, 659.25, 783.99, 1046.50)
    val noteSamples = (sampleRate * 0.05).toInt()
    val out = ShortArray(noteSamples * notes.size)
    var idx = 0
    for (freq in notes) {
      var phase = 0.0
      for (i in 0 until noteSamples) {
        val env = 1.0 - (i.toDouble() / noteSamples)
        phase += (2.0 * PI * freq) / sampleRate
        val s = (sin(phase) * 0.28 * env * Short.MAX_VALUE).toInt()
        out[idx++] = s.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
      }
    }
    return out
  }

  private fun buildNoiseExplosionBuffer(durationSec: Double, volume: Double): ShortArray {
    val count = (sampleRate * durationSec).toInt().coerceAtLeast(1)
    val out = ShortArray(count)
    var filtered = 0.0
    for (i in 0 until count) {
      val t = i.toDouble() / count.toDouble()
      val white = Random.nextDouble(-1.0, 1.0)
      filtered = filtered * 0.82 + white * 0.18
      val env = (1.0 - t) * exp(-3.0 * t)
      val s = (filtered * volume * env * Short.MAX_VALUE).toInt()
      out[i] = s.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
    }
    return out
  }

  private fun playPcmAsync(buffer: ShortArray) {
    if (!enabled) return
    audioExecutor.execute {
      try {
        val byteCount = buffer.size * 2
        val track =
          AudioTrack.Builder()
            .setAudioAttributes(
              AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            )
            .setAudioFormat(
              AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(sampleRate)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build()
            )
            .setBufferSizeInBytes(byteCount)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()
        track.write(buffer, 0, buffer.size)
        track.play()
        Thread.sleep((buffer.size * 1000L / sampleRate) + 25L)
        track.release()
      } catch (_: Exception) {}
    }
  }

  fun playLaser() = playPcmAsync(laserBuffer)
  fun playBomb() = playPcmAsync(bombBuffer)
  fun playBounce() = playPcmAsync(bounceBuffer)
  fun playPrize() = playPcmAsync(prizeBuffer)
  fun playExplosion() = playPcmAsync(explosionBuffer)
  fun playRepel() = playPcmAsync(repelBuffer)
  fun playWarp() = playPcmAsync(warpBuffer)
  fun playMutedClick() = playPcmAsync(mutedClickBuffer)
  fun playShotgun() = playPcmAsync(shotgunBuffer)
  fun playMissileLockBeep() = playPcmAsync(missileLockBeepBuffer)

  fun release() {
    try {
      audioExecutor.shutdownNow()
    } catch (_: Exception) {}
  }
}
