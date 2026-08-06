package de.hexenwoche.audiolex.core.audio

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The Desktop [AudioSource] itself needs a real line/device and isn't
 * jvm-unit-testable (Backlog Batch A DoD note) -- but the little-endian
 * byte<->sample conversion it uses per chunk is pure logic, so it's
 * extracted as `internal` and tested directly here, no audio device needed.
 */
class AudioSourceJvmTest {

    @Test
    fun decodesPositiveAndNegativeSamples() {
        // 0x0064 = 100 (little-endian: 0x64, 0x00); 0xFF9C = -100 (0x9C, 0xFF).
        val bytes = byteArrayOf(0x64, 0x00, 0x9C.toByte(), 0xFF.toByte())
        val samples = bytes.toPcm16LittleEndian(bytes.size)
        assertEquals(listOf<Short>(100, -100), samples.toList())
    }

    @Test
    fun decodesExtremeValues() {
        val bytes = byteArrayOf(
            0xFF.toByte(), 0x7F, // Short.MAX_VALUE (32767)
            0x00, 0x80.toByte(), // Short.MIN_VALUE (-32768)
        )
        val samples = bytes.toPcm16LittleEndian(bytes.size)
        assertEquals(listOf(Short.MAX_VALUE, Short.MIN_VALUE), samples.toList())
    }

    @Test
    fun honorsByteCountIgnoringTrailingBytes() {
        // A partial chunk read: only the first 4 of 6 bytes are valid.
        val bytes = byteArrayOf(1, 0, 2, 0, 0xFF.toByte(), 0xFF.toByte())
        val samples = bytes.toPcm16LittleEndian(4)
        assertEquals(listOf<Short>(1, 2), samples.toList())
    }
}
