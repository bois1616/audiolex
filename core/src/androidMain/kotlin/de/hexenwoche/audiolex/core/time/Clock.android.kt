package de.hexenwoche.audiolex.core.time

actual fun systemClock(): Clock = object : Clock {
    override fun nowEpochMillis(): Long = System.currentTimeMillis()
}
