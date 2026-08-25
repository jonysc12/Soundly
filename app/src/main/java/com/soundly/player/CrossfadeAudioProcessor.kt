package com.soundly.player

import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer

/**
 * Placeholder for future low-level effects. 
 * Crossfade is now handled via Dual-Player orchestration for better stability.
 */
@UnstableApi
class CrossfadeAudioProcessor : BaseAudioProcessor() {
    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        return inputAudioFormat
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val outputBuffer = replaceOutputBuffer(inputBuffer.remaining())
        outputBuffer.put(inputBuffer)
        outputBuffer.flip()
    }
}
