package de.hexenwoche.audiolex

import de.hexenwoche.audiolex.core.audio.OutputSetup
import de.hexenwoche.audiolex.core.audio.PcmBuffer
import de.hexenwoche.audiolex.core.audio.StereoGain
import de.hexenwoche.audiolex.core.audio.toStereoWithGain
import de.hexenwoche.audiolex.core.settings.ChannelMode

/**
 * Applies the user's channel selection (Backlog M4 "Kopfhörer-Bogen Batch B",
 * ADR-0011 point 5) -- shared by [LernmodusScreen] and [PruefmodusScreen] so
 * the "only wirksam in the headphone setup" gate lives in exactly one place,
 * same pattern as [mixWithOptionalNoise] for the noise overlay. Called inside
 * the [de.hexenwoche.audiolex.core.session.PlaybackQueue] producer, after
 * decode and after the noise mix, before the buffer reaches the sink.
 *
 * A no-op -- [speech] comes back unchanged, still mono -- unless *both* hold:
 * the user picked [ChannelMode.NUR_LINKS]/[ChannelMode.NUR_RECHTS] *and*
 * [outputSetup] is the detected stereo-headphone setup. [ChannelMode.BEIDE]
 * and the hearing-aid setup both take this early-return path, never reaching
 * [toStereoWithGain] -- the buffer must stay bit-identical to before this
 * feature existed there (ADR-0007/ADR-0011: the hearing aid sums stereo back
 * to mono anyway, so widening it would be pointless at best, misleading at
 * worst).
 */
internal fun applyChannelMode(
    speech: PcmBuffer,
    channelMode: ChannelMode,
    outputSetup: OutputSetup,
): PcmBuffer {
    if (outputSetup != OutputSetup.STEREO_KOPFHOERER) return speech
    val gain = when (channelMode) {
        ChannelMode.BEIDE -> return speech
        ChannelMode.NUR_LINKS -> StereoGain.LEFT_ONLY
        ChannelMode.NUR_RECHTS -> StereoGain.RIGHT_ONLY
    }
    return speech.toStereoWithGain(gain)
}
