package de.hexenwoche.audiolex.core.audio

/**
 * Minimal WAV (RIFF/PCM) reader, platform-free (ADR-0003): the corpus format
 * is uncompressed 16-bit PCM, so no platform decoder is needed anywhere.
 * Walks chunks instead of assuming fixed offsets, since some encoders insert
 * extra chunks (e.g. LIST) between "fmt " and "data".
 */
object WavFile {

    fun decode(bytes: ByteArray): PcmBuffer {
        require(bytes.size >= 12) { "not a WAV file: too short" }
        require(bytes.readAscii(0, 4) == "RIFF") { "not a RIFF file" }
        require(bytes.readAscii(8, 4) == "WAVE") { "not a WAVE file" }

        var sampleRate = -1
        var channels = -1
        var bitsPerSample = -1
        var dataOffset = -1
        var dataSize = -1

        var offset = 12
        while (offset + 8 <= bytes.size) {
            val chunkId = bytes.readAscii(offset, 4)
            val chunkSize = bytes.readLeInt(offset + 4)
            val chunkDataStart = offset + 8
            require(chunkSize >= 0 && chunkDataStart + chunkSize <= bytes.size) {
                "corrupt WAV: chunk '$chunkId' size $chunkSize exceeds file bounds"
            }

            when (chunkId) {
                "fmt " -> {
                    require(chunkSize >= 16) { "corrupt WAV: fmt chunk too small ($chunkSize bytes)" }
                    val audioFormat = bytes.readLeShort(chunkDataStart)
                    require(audioFormat == 1) { "only uncompressed PCM is supported, got format $audioFormat" }
                    channels = bytes.readLeShort(chunkDataStart + 2)
                    sampleRate = bytes.readLeInt(chunkDataStart + 4)
                    bitsPerSample = bytes.readLeShort(chunkDataStart + 14)
                }
                "data" -> {
                    dataOffset = chunkDataStart
                    dataSize = chunkSize
                }
            }

            // Chunks are word-aligned: a chunk with odd size has one pad byte.
            offset = chunkDataStart + chunkSize + (chunkSize and 1)
        }

        require(sampleRate > 0 && channels > 0 && bitsPerSample > 0) { "missing fmt chunk" }
        require(bitsPerSample == 16) { "only 16-bit PCM is supported, got ${bitsPerSample}-bit" }
        require(dataOffset >= 0) { "missing data chunk" }

        val clampedDataSize = minOf(dataSize, bytes.size - dataOffset)
        val sampleCount = clampedDataSize / 2
        val samples = ShortArray(sampleCount) { i -> bytes.readLeShort(dataOffset + i * 2).toShort() }

        return PcmBuffer(samples, sampleRate, channels)
    }
}

private fun ByteArray.readAscii(offset: Int, length: Int): String =
    buildString(length) { for (i in 0 until length) append(this@readAscii[offset + i].toInt().toChar()) }

private fun ByteArray.readLeShort(offset: Int): Int =
    (this[offset].toInt() and 0xFF) or ((this[offset + 1].toInt() and 0xFF) shl 8)

private fun ByteArray.readLeInt(offset: Int): Int =
    (this[offset].toInt() and 0xFF) or
        ((this[offset + 1].toInt() and 0xFF) shl 8) or
        ((this[offset + 2].toInt() and 0xFF) shl 16) or
        ((this[offset + 3].toInt() and 0xFF) shl 24)
