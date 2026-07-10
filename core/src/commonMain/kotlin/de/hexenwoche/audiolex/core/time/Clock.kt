package de.hexenwoche.audiolex.core.time

/**
 * Injectable time source (ADR-0008): UTC epoch millis, no calendar logic --
 * SRS scheduling only ever needs differences between two timestamps, not
 * absolute wall-clock accuracy. [zoneId] is carried alongside for reporting
 * only (Sitzungshistorie, Szenario S12): a stored timestamp's zone is kept
 * as a plain string and converted purely at display time, never used in SRS
 * due-date math.
 */
interface Clock {
    fun nowEpochMillis(): Long
    fun zoneId(): String
}

expect fun systemClock(): Clock
