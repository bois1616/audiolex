package de.hexenwoche.audiolex.core.audio

import kotlin.test.Test
import kotlin.test.assertIs

/**
 * The format line for WAV imports (Backlog M4 "Eigene Störgeräusche", AC4):
 * a readable PCM16 WAV at 22050 Hz mono is the only thing that passes;
 * everything else is rejected with enough detail for a helpful message --
 * never converted (ADR-0010).
 */
class OwnNoiseImportTest {

    private fun wav(sampleRate: Int, channels: Int): ByteArray {
        // One second of a simple tone, encoded by the app's own writer --
        // WavFile.encode writes whatever format the buffer carries, so this
        // produces valid WAVs at "wrong" formats too.
        val frames = sampleRate
        val samples = ShortArray(frames * channels) { i ->
            (10_000 * kotlin.math.sin((i / channels) * 0.05)).toInt().toShort()
        }
        return WavFile.encode(PcmBuffer(samples, sampleRate, channels))
    }

    @Test
    fun `a mono 22050 Hz PCM16 WAV is accepted`() {
        assertIs<OwnNoiseImportCheck.Ok>(checkOwnNoiseImport(wav(22050, 1)))
    }

    @Test
    fun `the recording format itself is accepted`() {
        // The app's own recordings are the reference case: recorded WAVs
        // must always pass, otherwise record-then-export-then-import would
        // reject its own files.
        val recorded = WavFile.encode(PcmBuffer(ShortArray(22050) { 100 }, RECORDING_SAMPLE_RATE, RECORDING_CHANNELS))

        assertIs<OwnNoiseImportCheck.Ok>(checkOwnNoiseImport(recorded))
    }

    @Test
    fun `a wrong sample rate is rejected with the actual rate`() {
        val result = checkOwnNoiseImport(wav(44100, 1))

        val wrongFormat = assertIs<OwnNoiseImportCheck.WrongFormat>(result)
        kotlin.test.assertEquals(44100, wrongFormat.sampleRate)
        kotlin.test.assertEquals(1, wrongFormat.channels)
    }

    @Test
    fun `a stereo file is rejected with the actual channel count`() {
        val result = checkOwnNoiseImport(wav(22050, 2))

        val wrongFormat = assertIs<OwnNoiseImportCheck.WrongFormat>(result)
        kotlin.test.assertEquals(22050, wrongFormat.sampleRate)
        kotlin.test.assertEquals(2, wrongFormat.channels)
    }

    @Test
    fun `garbage bytes are rejected as not a WAV`() {
        assertIs<OwnNoiseImportCheck.NotAWav>(
            checkOwnNoiseImport("Dies ist keine WAV-Datei.".encodeToByteArray()),
        )
    }

    @Test
    fun `an empty file is rejected as not a WAV`() {
        assertIs<OwnNoiseImportCheck.NotAWav>(checkOwnNoiseImport(ByteArray(0)))
    }

    @Test
    fun `an 8-bit PCM WAV is rejected instead of downgraded`() {
        // Hand-built minimal WAV: the app's writer only produces 16-bit, so
        // the unsupported-depth case is assembled directly.
        val dataSize = 4
        val bytes = ByteArray(44 + dataSize)
        fun ascii(offset: Int, s: String) {
            for (i in s.indices) bytes[offset + i] = s[i].code.toByte()
        }
        fun le32(offset: Int, v: Int) {
            bytes[offset] = (v and 0xFF).toByte()
            bytes[offset + 1] = ((v shr 8) and 0xFF).toByte()
            bytes[offset + 2] = ((v shr 16) and 0xFF).toByte()
            bytes[offset + 3] = ((v shr 24) and 0xFF).toByte()
        }
        fun le16(offset: Int, v: Int) {
            bytes[offset] = (v and 0xFF).toByte()
            bytes[offset + 1] = ((v shr 8) and 0xFF).toByte()
        }
        ascii(0, "RIFF"); le32(4, 36 + dataSize); ascii(8, "WAVE")
        ascii(12, "fmt "); le32(16, 16); le16(20, 1); le16(22, 1); le32(24, 22050)
        le32(28, 22050); le16(32, 1); le16(34, 8) // 8 bits per sample
        ascii(36, "data"); le32(40, dataSize)

        assertIs<OwnNoiseImportCheck.NotAWav>(checkOwnNoiseImport(bytes))
    }
}
