package de.hexenwoche.audiolex.core.srs

import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes

/**
 * Rating scale of the exam mode. MVP: each rating maps directly to a fixed
 * repetition interval (ADR-0005). German UI labels: Sofort, Bald, Später, Gut, Perfekt.
 */
enum class ReviewRating(val interval: Duration) {
    AGAIN(1.minutes),
    SOON(10.minutes),
    LATER(1.days),
    GOOD(7.days),
    PERFECT(30.days),
}
