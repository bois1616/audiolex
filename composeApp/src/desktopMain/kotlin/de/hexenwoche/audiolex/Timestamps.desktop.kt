package de.hexenwoche.audiolex

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")

actual fun formatTimestamp(epochMillis: Long, zoneId: String): String =
    Instant.ofEpochMilli(epochMillis).atZone(ZoneId.of(zoneId)).format(formatter)
