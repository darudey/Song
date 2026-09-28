package com.example.player

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Generates high-quality harmonic audio tracks (rich synth pads, basslines & melodies)
 * locally as fallback or offline cache when network stream is unreachable.
 */
object AudioSynthesizer {

    suspend fun generateTrackWav(context: Context, songId: String, durationSec: Int = 45): File = withContext(Dispatchers.IO) {
        val cacheDir = File(context.filesDir, "audio_cache").apply { if (!exists()) mkdirs() }
        val wavFile = File(cacheDir, "${songId}_generated.wav")

        if (wavFile.exists() && wavFile.length() > 10000) {
            return@withContext wavFile
        }

        val sampleRate = 44100
        val numSamples = sampleRate * durationSec
        val buffer = ShortArray(numSamples)

        // Musical scale (Pentatonic/Minor scale frequencies)
        val seed = songId.hashCode().toLong()
        val baseFreq = when ((seed % 5).toInt()) {
            0 -> 220.0 // A3
            1 -> 261.63 // C4
            2 -> 293.66 // D4
            3 -> 329.63 // E4
            else -> 196.0 // G3
        }

        val notes = listOf(
            baseFreq,
            baseFreq * 1.189, // Minor 3rd
            baseFreq * 1.334, // 4th
            baseFreq * 1.498, // 5th
            baseFreq * 1.781, // Minor 7th
            baseFreq * 2.0    // Octave
        )

        val beatDurationSamples = (sampleRate * 0.5).toInt() // 120 BPM

        for (i in 0 until numSamples) {
            val beatIndex = (i / beatDurationSamples) % notes.size
            val currentNoteFreq = notes[beatIndex]
            val t = i.toDouble() / sampleRate
            val beatTime = (i % beatDurationSamples).toDouble() / sampleRate

            // Envelope (Attack & Decay)
            val envelope = exp(-beatTime * 3.0)

            // Melody Harmonic Lead
            val lead = sin(2.0 * PI * currentNoteFreq * t) * 0.4 +
                    sin(2.0 * PI * currentNoteFreq * 2.0 * t) * 0.15

            // Warm Bass Pad
            val bass = sin(2.0 * PI * (baseFreq * 0.5) * t) * 0.35

            // Sub-harmonic chord
            val chord = sin(2.0 * PI * (notes[(beatIndex + 2) % notes.size]) * t) * 0.2

            // Combine and normalize
            val sampleVal = ((lead * envelope + bass + chord) * 0.7 * Short.MAX_VALUE).toInt()
            buffer[i] = sampleVal.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }

        writeWavFile(wavFile, buffer, sampleRate)
        wavFile
    }

    private fun writeWavFile(file: File, samples: ShortArray, sampleRate: Int) {
        val totalAudioLen = (samples.size * 2).toLong()
        val totalDataLen = totalAudioLen + 36
        val channels = 1
        val byteRate = (sampleRate * channels * 2).toLong()

        val header = ByteArray(44)
        val bb = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)

        // RIFF header
        bb.put('R'.code.toByte())
        bb.put('I'.code.toByte())
        bb.put('F'.code.toByte())
        bb.put('F'.code.toByte())
        bb.putInt(totalDataLen.toInt())
        bb.put('W'.code.toByte())
        bb.put('A'.code.toByte())
        bb.put('V'.code.toByte())
        bb.put('E'.code.toByte())

        // fmt subchunk
        bb.put('f'.code.toByte())
        bb.put('m'.code.toByte())
        bb.put('t'.code.toByte())
        bb.put(' '.code.toByte())
        bb.putInt(16) // Subchunk1Size for PCM
        bb.putShort(1) // AudioFormat 1 = PCM
        bb.putShort(channels.toShort())
        bb.putInt(sampleRate)
        bb.putInt(byteRate.toInt())
        bb.putShort((channels * 2).toShort()) // BlockAlign
        bb.putShort(16) // BitsPerSample

        // data subchunk
        bb.put('d'.code.toByte())
        bb.put('a'.code.toByte())
        bb.put('t'.code.toByte())
        bb.put('a'.code.toByte())
        bb.putInt(totalAudioLen.toInt())

        FileOutputStream(file).use { fos ->
            fos.write(header)
            val byteBuffer = ByteBuffer.allocate(samples.size * 2).order(ByteOrder.LITTLE_ENDIAN)
            for (s in samples) {
                byteBuffer.putShort(s)
            }
            fos.write(byteBuffer.array())
            fos.flush()
        }
    }
}
