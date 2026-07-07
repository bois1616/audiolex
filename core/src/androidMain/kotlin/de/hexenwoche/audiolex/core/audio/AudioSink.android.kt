package de.hexenwoche.audiolex.core.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack

actual fun createAudioSink(): AudioSink = AndroidAudioSink()

/**
 * Minimal AudioTrack sink (static mode, fine for single-word playback).
 * TODO(M1): verify on the Galaxy A53 with hearing aid; move to streaming
 * mode if corpus items ever exceed a few seconds.
 */
private class AndroidAudioSink : AudioSink {
    override fun play(buffer: PcmBuffer) {
        val channelMask = if (buffer.channels == 1) {
            AudioFormat.CHANNEL_OUT_MONO
        } else {
            AudioFormat.CHANNEL_OUT_STEREO
        }
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(buffer.sampleRate)
                    .setChannelMask(channelMask)
                    .build()
            )
            .setTransferMode(AudioTrack.MODE_STATIC)
            .setBufferSizeInBytes(buffer.samples.size * 2)
            .build()
        try {
            track.write(buffer.samples, 0, buffer.samples.size)
            track.play()
            // Static mode plays asynchronously; block until done so the
            // interface contract (blocking play) holds.
            val durationMillis = buffer.frameCount * 1000L / buffer.sampleRate
            Thread.sleep(durationMillis + 50)
        } finally {
            track.release()
        }
    }

    override fun close() = Unit
}
