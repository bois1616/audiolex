package de.hexenwoche.audiolex.core.time

/** Settable clock for deterministic time-dependent tests (ADR-0008). */
class FakeClock(var now: Long, private val zone: String = "Europe/Berlin") : Clock {
    override fun nowEpochMillis(): Long = now
    override fun zoneId(): String = zone
}
