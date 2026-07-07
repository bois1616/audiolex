package de.hexenwoche.audiolex.core.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack

actual fun createAudioSink(): AudioSink = AndroidAudioSink()

/**
 * Minimal AudioTrack sink (static mode, fine for single-word playback).
 *
 * Verified on a Galaxy A53 (backlog M1): standard L/R-interleaved stereo
 * PCM played out of the *wrong* ear, confirmed independently over both a
 * Bluetooth hearing aid and a wired USB-C headset (two different words,
 * one per ear -- "left" consistently came out the right side and vice
 * versa on both output paths). [swapStereoChannels] corrects this before
 * handing PCM to AudioTrack. Since it reproduces across two unrelated
 * output paths on this device, it's treated as a device/OEM audio-stack
 * quirk rather than something specific to one accessory; the upstream PCM
 * (from [toStereoWithGain]) already uses the documented standard layout.
 * If this doesn't reproduce on other Android devices, revisit rather than
 * assume it's universal.
 */
private class AndroidAudioSink : AudioSink {
    override fun play(buffer: PcmBuffer) {
        val playable = if (buffer.channels == 2) buffer.swapStereoChannels() else buffer

        val channelMask = if (playable.channels == 1) {
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
                    .setSampleRate(playable.sampleRate)
                    .setChannelMask(channelMask)
                    .build()
            )
            .setTransferMode(AudioTrack.MODE_STATIC)
            .setBufferSizeInBytes(playable.samples.size * 2)
            .build()
        try {
            track.write(playable.samples, 0, playable.samples.size)
            track.play()
            // Static mode plays asynchronously; block until done so the
            // interface contract (blocking play) holds.
            val durationMillis = playable.frameCount * 1000L / playable.sampleRate
            Thread.sleep(durationMillis + 50)
        } finally {
            track.release()
        }
    }

    override fun close() = Unit
}

/** Swaps L/R in interleaved stereo PCM; see [AndroidAudioSink] doc for why. */
internal fun PcmBuffer.swapStereoChannels(): PcmBuffer {
    require(channels == 2) { "expected stereo, got $channels channels" }
    val swapped = ShortArray(samples.size)
    for (frame in 0 until frameCount) {
        swapped[frame * 2] = samples[frame * 2 + 1]
        swapped[frame * 2 + 1] = samples[frame * 2]
    }
    return PcmBuffer(swapped, sampleRate, channels)
}
