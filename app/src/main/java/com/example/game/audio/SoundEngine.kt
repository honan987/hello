package com.example.game.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.*
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Real procedural PCM Audio Engine using Android AudioTrack.
 * Generates cozy piano BGM, romantic date melody, rain/nature ambience, and crisp SFX.
 */
class SoundEngine {
    private val sampleRate = 22050
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Volatile var bgmVolume: Float = 0.55f
    @Volatile var sfxVolume: Float = 0.75f
    @Volatile var isMuted: Boolean = false
    @Volatile var isDateMode: Boolean = false
    @Volatile var isRainy: Boolean = false

    private var bgmJob: Job? = null

    fun startBgm() {
        if (bgmJob?.isActive == true) return
        bgmJob = scope.launch {
            val minBuf = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(4096)

            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(minBuf)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            try {
                track.play()
                // Gentle cottagecore piano arpeggios (C5, E5, G5, B5 -> F4, A4, C5, E5 -> G4, B4, D5, F#5 -> A4, C5, E5, G5)
                val cozyNotes = doubleArrayOf(
                    523.25, 659.25, 783.99, 987.77,
                    349.23, 440.00, 523.25, 659.25,
                    392.00, 493.88, 587.33, 739.99,
                    440.00, 523.25, 659.25, 783.99
                )
                val dateNotes = doubleArrayOf(
                    523.25, 659.25, 783.99, 1046.50, 987.77, 783.99,
                    440.00, 523.25, 659.25, 880.00, 783.99, 659.25
                )

                val noteSamples = (sampleRate * 0.48).toInt()
                val buf = ShortArray(noteSamples)
                var step = 0
                var rainSeed = 1234567

                while (isActive) {
                    val notes = if (isDateMode) dateNotes else cozyNotes
                    val freq = notes[step % notes.size]
                    val bassFreq = freq * 0.5
                    val vol = if (isMuted) 0f else bgmVolume * 0.22f

                    for (i in 0 until noteSamples) {
                        val t = i.toDouble() / sampleRate
                        val env = exp(-4.5 * t) * (1.0 - exp(-90.0 * t))
                        var sample = (sin(2.0 * PI * freq * t) * 0.72 +
                            sin(4.0 * PI * freq * t) * 0.14 +
                            sin(2.0 * PI * bassFreq * t) * 0.32) * env

                        if (isRainy) {
                            rainSeed = rainSeed * 1664525 + 1013904223
                            val noise = ((rainSeed ushr 16) and 0xFFFF) / 32768.0 - 1.0
                            sample += noise * 0.08
                        }
                        val pcm = (sample * vol * Short.MAX_VALUE).toInt()
                            .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                        buf[i] = pcm.toShort()
                    }
                    track.write(buf, 0, buf.size)
                    step++
                }
            } finally {
                runCatching {
                    track.stop()
                    track.release()
                }
            }
        }
    }

    fun stopBgm() {
        bgmJob?.cancel()
        bgmJob = null
    }

    fun playHoe() = playToneSequence(listOf(190.0 to 0.05, 130.0 to 0.07), 0.35f)
    fun playWater() = playNoiseBurst(0.16, 0.28f)
    fun playSeed() = playToneSequence(listOf(580.0 to 0.04, 720.0 to 0.05), 0.30f)
    fun playHarvest() = playToneSequence(listOf(523.25 to 0.06, 659.25 to 0.06, 783.99 to 0.10), 0.42f)
    fun playCoin() = playToneSequence(listOf(987.77 to 0.05, 1318.51 to 0.11), 0.38f)
    fun playHeart() = playToneSequence(listOf(659.25 to 0.07, 880.0 to 0.08, 1046.5 to 0.14), 0.40f)
    fun playStep() = playToneSequence(listOf(140.0 to 0.03), 0.12f)
    fun playUiClick() = playToneSequence(listOf(660.0 to 0.035), 0.22f)
    fun playDialogue() = playToneSequence(listOf(480.0 to 0.03, 540.0 to 0.03), 0.20f)

    private fun playToneSequence(tones: List<Pair<Double, Double>>, gain: Float) {
        if (isMuted || sfxVolume <= 0.01f) return
        scope.launch {
            val totalSamples = tones.sumOf { (it.second * sampleRate).toInt() }
            if (totalSamples <= 0) return@launch
            val pcm = ShortArray(totalSamples)
            var offset = 0
            val vol = sfxVolume * gain
            for ((freq, dur) in tones) {
                val count = (dur * sampleRate).toInt()
                for (i in 0 until count) {
                    val t = i.toDouble() / sampleRate
                    val env = exp(-8.0 * t)
                    val v = sin(2.0 * PI * freq * t) * env * vol * Short.MAX_VALUE
                    pcm[offset + i] = v.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                }
                offset += count
            }
            playBufferOnce(pcm)
        }
    }

    private fun playNoiseBurst(durSec: Double, gain: Float) {
        if (isMuted || sfxVolume <= 0.01f) return
        scope.launch {
            val count = (durSec * sampleRate).toInt()
            val pcm = ShortArray(count)
            var seed = (System.nanoTime() and 0x7FFFFFFFL).toInt()
            val vol = sfxVolume * gain
            for (i in 0 until count) {
                seed = seed * 1664525 + 1013904223
                val noise = ((seed ushr 16) and 0xFFFF) / 32768.0 - 1.0
                val env = 1.0 - i.toDouble() / count
                pcm[i] = (noise * env * vol * Short.MAX_VALUE).toInt()
                    .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }
            playBufferOnce(pcm)
        }
    }

    private fun playBufferOnce(pcm: ShortArray) {
        runCatching {
            val track = AudioTrack.Builder()
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
                .setBufferSizeInBytes(pcm.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()
            track.write(pcm, 0, pcm.size)
            track.play()
            Thread.sleep((pcm.size * 1000L / sampleRate) + 40L)
            track.stop()
            track.release()
        }
    }
}
