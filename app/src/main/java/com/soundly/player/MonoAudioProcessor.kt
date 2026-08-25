package com.soundly.player

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer

/**
 * Audio processor that mixes stereo to mono in real-time.
 * It keeps 2 channels in the output but makes them identical (L=R)
 * to ensure compatibility with all hardware.
 */
@UnstableApi
class MonoAudioProcessor : BaseAudioProcessor() {

    private var isMonoEnabled = false

    fun setEnabled(enabled: Boolean) {
        isMonoEnabled = enabled
    }

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT) {
            throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
        }
        // We keep the same format (Stereo) but we will modify the samples
        return inputAudioFormat
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remaining = inputBuffer.remaining()
        if (remaining == 0) return

        val outputBuffer = replaceOutputBuffer(remaining)

        if (!isMonoEnabled) {
            // Just copy the buffer (Pass-through)
            outputBuffer.put(inputBuffer)
        } else {
            // Stereo to Mono mixing
            while (inputBuffer.hasRemaining()) {
                val left = inputBuffer.short
                val right = if (inputBuffer.hasRemaining()) inputBuffer.short else left
                
                // Mix: (L + R) / 2
                // We use Int to avoid overflow before dividing
                val mixed = ((left.toInt() + right.toInt()) / 2).toShort()
                
                // Output as Stereo (both channels identical)
                outputBuffer.putShort(mixed)
                outputBuffer.putShort(mixed)
            }
        }
        outputBuffer.flip()
    }
}
