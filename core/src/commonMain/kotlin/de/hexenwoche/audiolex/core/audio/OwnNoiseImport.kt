package de.hexenwoche.audiolex.core.audio

/**
 * Validation of a WAV file offered for import as an own noise (Backlog M4
 * "Eigene Störgeräusche", AC4), platform-free in `:core`: the picker hands
 * over raw bytes, this function draws the format line. The rule is the one
 * ADR-0010 drew for noise in general -- 22050 Hz mono PCM16, the recording
 * format the app itself produces (ADR-0012 point 3) and the only format
 * `mixWithNoise` accepts without resampling, which stays out of scope.
 * Rejection instead of conversion: the verdict carries enough detail for the
 * screen to say what is wrong and what to do, in its own words.
 */
sealed interface OwnNoiseImportCheck {
    /** The file is a readable PCM16 WAV at 22050 Hz mono -- importable as-is. */
    data object Ok : OwnNoiseImportCheck

    /**
     * Not readable as a PCM16 WAV at all: not a WAV, truncated, or an
     * encoding [WavFile.decode] doesn't support (compressed, wrong bit
     * depth). Deliberately one bucket: the user-facing consequence is the
     * same either way -- pick a different file or record in the app.
     */
    data object NotAWav : OwnNoiseImportCheck

    /**
     * A readable PCM16 WAV, but not the required 22050 Hz mono. Carries what
     * the file actually has so the screen can say it concretely. No
     * resampling or downmix happens on import (ADR-0010).
     */
    class WrongFormat(val sampleRate: Int, val channels: Int) : OwnNoiseImportCheck
}

fun checkOwnNoiseImport(bytes: ByteArray): OwnNoiseImportCheck {
    val buffer = try {
        WavFile.decode(bytes)
    } catch (e: IllegalArgumentException) {
        return OwnNoiseImportCheck.NotAWav
    }
    if (buffer.sampleRate != RECORDING_SAMPLE_RATE || buffer.channels != RECORDING_CHANNELS) {
        return OwnNoiseImportCheck.WrongFormat(buffer.sampleRate, buffer.channels)
    }
    return OwnNoiseImportCheck.Ok
}
