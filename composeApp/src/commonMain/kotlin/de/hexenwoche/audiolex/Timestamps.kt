package de.hexenwoche.audiolex

/**
 * Formats a UTC epoch-millis timestamp with its stored zone id (Sitzungs-
 * historie, Szenario S12) as "TT.MM.JJJJ HH:MM" -- display-only formatting,
 * kept out of :core since it's neither SRS logic nor date-library-free
 * (ADR-0008: the Clock stays UTC-millis-only, conversion happens here).
 */
expect fun formatTimestamp(epochMillis: Long, zoneId: String): String
