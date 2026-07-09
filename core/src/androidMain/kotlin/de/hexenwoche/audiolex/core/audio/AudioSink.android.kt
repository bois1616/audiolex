package de.hexenwoche.audiolex.core.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.delay

actual fun createAudioSink(): AudioSink = AndroidAudioSink()

/**
 * Same sink with an explicit L/R-swap override -- groundwork for the
 * "Kanäle tauschen" user setting (Opus-Review 2026-07-07, Befund 1), not
 * currently wired into app code. [swapChannels] should be `false` for the
 * verified-correct Galaxy A53 default; see [swapStereoChannels] doc.
 */
fun createAudioSinkWithSwapOverride(swapChannels: Boolean): AudioSink =
    AndroidAudioSink(swapChannels)

/** Minimal AudioTrack sink (static mode, fine for single-word playback). */
private class AndroidAudioSink(private val swapChannels: Boolean = false) : AudioSink {
    override suspend fun play(buffer: PcmBuffer) {
        val playable = if (buffer.channels == 2 && swapChannels) buffer.swapStereoChannels() else buffer

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
            // Static mode plays asynchronously; wait until done so the
            // interface contract (suspends until playback finished) holds.
            // delay() (not Thread.sleep) is a cancellation point: if the
            // caller cancels, the `finally` below stops the track
            // immediately instead of letting it play out in the background.
            val durationMillis = playable.frameCount * 1000L / playable.sampleRate
            delay(durationMillis + 50)
        } finally {
            track.stop()
            track.release()
        }
    }

    override fun close() = Unit
}

/**
 * Swaps L/R in interleaved stereo PCM.
 *
 * Not applied by default. An earlier fix (backlog M1, commit `67ea1e2`)
 * assumed the Galaxy A53 played standard L/R-interleaved stereo out of the
 * wrong ear and swapped it here to compensate. The Opus-Review 2026-07-07
 * (Befund 1) found that evidence contaminated (silent channel by test
 * construction, an unreset system balance override, and a BT mono-mix that
 * made the "confirmation" unfalsifiable) and called for a clean re-test.
 * Re-tested 2026-07-08 with the balance confirmed centered, a wired USB-C
 * headset, and a known-state toggle: with swap OFF, "left" and "right"
 * playback each came out of the correct physical ear, with silence on the
 * other -- i.e. the device was never broken, and this function was the bug.
 * Kept only as groundwork for a possible future "Kanäle tauschen"
 * accessibility setting; not wired into [createAudioSink] anymore.
 */
internal fun PcmBuffer.swapStereoChannels(): PcmBuffer {
    require(channels == 2) { "expected stereo, got $channels channels" }
    val swapped = ShortArray(samples.size)
    for (frame in 0 until frameCount) {
        swapped[frame * 2] = samples[frame * 2 + 1]
        swapped[frame * 2 + 1] = samples[frame * 2]
    }
    return PcmBuffer(swapped, sampleRate, channels)
}
