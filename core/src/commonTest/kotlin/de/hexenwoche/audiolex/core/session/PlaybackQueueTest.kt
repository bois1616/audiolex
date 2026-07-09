package de.hexenwoche.audiolex.core.session

import de.hexenwoche.audiolex.core.audio.AudioSink
import de.hexenwoche.audiolex.core.audio.PcmBuffer
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class PlaybackQueueTest {

    /**
     * Records which buffers actually ran to completion vs. got interrupted
     * mid-"play". [play] suspends on a per-call gate instead of a real
     * delay, so the test controls exactly when (and whether) a call
     * finishes, without depending on wall-clock timing. Because [play] is a
     * genuine suspend function, cancellation (PlaybackQueue's whole point)
     * interrupts the `await()` below directly -- no nested blocking call
     * involved, so this runs cleanly under a virtual-time TestScope.
     */
    private class GatedSink : AudioSink {
        val completed = mutableListOf<Int>()
        val started = mutableListOf<Int>()
        private val gates = mutableMapOf<Int, CompletableDeferred<Unit>>()

        fun gateFor(tag: Int): CompletableDeferred<Unit> =
            gates.getOrPut(tag) { CompletableDeferred() }

        override suspend fun play(buffer: PcmBuffer) {
            val tag = buffer.samples[0].toInt()
            started += tag
            gateFor(tag).await()
            completed += tag
        }

        override fun close() = Unit
    }

    private fun bufferTagged(tag: Int) = PcmBuffer(shortArrayOf(tag.toShort()), 22050, channels = 1)

    @Test
    fun secondPlayCancelsFirstBeforeItCompletes() = runTest {
        val sink = GatedSink()
        val queue = PlaybackQueue(sink, this)

        queue.play(bufferTagged(1))
        runCurrent()
        sink.gateFor(2).complete(Unit) // second call's gate is pre-opened so it finishes once started
        queue.play(bufferTagged(2))
        advanceUntilIdle()

        assertEquals(listOf(1, 2), sink.started)
        assertEquals(listOf(2), sink.completed, "first playback should have been cancelled, not completed")
    }

    @Test
    fun stopCancelsInFlightPlaybackWithoutStartingAnother() = runTest {
        val sink = GatedSink()
        val queue = PlaybackQueue(sink, this)

        queue.play(bufferTagged(1))
        runCurrent()
        queue.stop()
        advanceUntilIdle()

        assertEquals(listOf(1), sink.started)
        assertEquals(emptyList(), sink.completed)
    }

    @Test
    fun singlePlayRunsToCompletion() = runTest {
        val sink = GatedSink()
        val queue = PlaybackQueue(sink, this)

        sink.gateFor(1).complete(Unit)
        queue.play(bufferTagged(1))
        advanceUntilIdle()

        assertEquals(listOf(1), sink.completed)
    }

    private class FailingSink(private val error: Throwable) : AudioSink {
        override suspend fun play(buffer: PcmBuffer) = throw error
        override fun close() = Unit
    }

    @Test
    fun sinkFailureIsReportedViaOnErrorNotThrown() = runTest {
        val error = IllegalStateException("device disconnected")
        val errors = mutableListOf<Throwable>()
        val queue = PlaybackQueue(FailingSink(error), this, onError = { errors += it })

        queue.play(bufferTagged(1))
        advanceUntilIdle()

        assertEquals(listOf<Throwable>(error), errors)
    }

    @Test
    fun cancellingForNextPlayDoesNotReportAsError() = runTest {
        val sink = GatedSink()
        val errors = mutableListOf<Throwable>()
        val queue = PlaybackQueue(sink, this, onError = { errors += it })

        queue.play(bufferTagged(1))
        runCurrent()
        sink.gateFor(2).complete(Unit)
        queue.play(bufferTagged(2))
        advanceUntilIdle()

        assertEquals(emptyList(), errors, "cancellation for the cutover is not a playback failure")
    }
}
