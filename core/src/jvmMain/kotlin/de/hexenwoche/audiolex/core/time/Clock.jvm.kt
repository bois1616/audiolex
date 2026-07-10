package de.hexenwoche.audiolex.core.time

import java.time.ZoneId

actual fun systemClock(): Clock = object : Clock {
    override fun nowEpochMillis(): Long = System.currentTimeMillis()
    override fun zoneId(): String = ZoneId.systemDefault().id
}
