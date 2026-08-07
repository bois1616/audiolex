package de.hexenwoche.audiolex

/**
 * Formats a UTC epoch-millis timestamp with its stored zone id (Sitzungs-
 * historie, Szenario S12) as "TT.MM.JJJJ HH:MM" -- display-only formatting,
 * kept out of :core since it's neither SRS logic nor date-library-free
 * (ADR-0008: the Clock stays UTC-millis-only, conversion happens here).
 */
expect fun formatTimestamp(epochMillis: Long, zoneId: String): String

/**
 * The timestamp part of a backup's file name, as `2026-08-07-1432` (Backlog
 * "Sicherung eigener Aufnahmen", AC1: a second backup must not overwrite the
 * first). Sortable order rather than the German display order above, and no
 * characters that need escaping in a file name; local time, because the name
 * is read by a person deciding which file is the recent one.
 */
expect fun formatBackupTimestamp(epochMillis: Long): String
