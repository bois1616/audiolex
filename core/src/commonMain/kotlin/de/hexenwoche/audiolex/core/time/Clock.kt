package de.hexenwoche.audiolex.core.time

/**
 * Injectable time source (ADR-0008): UTC epoch millis only, no timezone or
 * calendar logic -- SRS scheduling only ever needs differences between two
 * timestamps, not absolute wall-clock accuracy.
 */
interface Clock {
    fun nowEpochMillis(): Long
}

expect fun systemClock(): Clock
