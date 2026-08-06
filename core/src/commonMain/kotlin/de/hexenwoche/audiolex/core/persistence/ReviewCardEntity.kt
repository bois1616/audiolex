package de.hexenwoche.audiolex.core.persistence

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.Upsert
import de.hexenwoche.audiolex.core.srs.ReviewCard
import de.hexenwoche.audiolex.core.srs.ReviewRating

/**
 * Room-mapped mirror of [ReviewCard] -- [ReviewCard] itself (in core/srs)
 * stays free of persistence annotations so the SRS logic remains testable
 * without Room (ReviewQueueTest/ExamSessionTest/FixedIntervalSchedulerTest
 * don't need to change for this).
 */
@Entity
@TypeConverters(ReviewRatingConverter::class)
data class ReviewCardEntity(
    @PrimaryKey val wordId: String,
    val dueAtEpochMillis: Long,
    val lastRating: ReviewRating?,
    val repetitions: Int,
)

fun ReviewCardEntity.toDomain(): ReviewCard =
    ReviewCard(wordId, dueAtEpochMillis, lastRating, repetitions)

fun ReviewCard.toEntity(): ReviewCardEntity =
    ReviewCardEntity(wordId, dueAtEpochMillis, lastRating, repetitions)

@Dao
interface ReviewCardDao {
    @Upsert
    suspend fun upsert(card: ReviewCardEntity)

    /** Batch upsert in a single transaction, used by [allOrSeed]'s seeding step. */
    @Upsert
    suspend fun upsertAll(cards: List<ReviewCardEntity>)

    @Query("SELECT * FROM ReviewCardEntity")
    suspend fun all(): List<ReviewCardEntity>
}

/**
 * Stores [ReviewRating] by its enum name rather than ordinal: reordering or
 * inserting a rating in [ReviewRating] would silently reinterpret already
 * stored values under an ordinal mapping. Name-based storage costs a few
 * bytes but survives that kind of change.
 */
class ReviewRatingConverter {
    @TypeConverter
    fun toName(rating: ReviewRating?): String? = rating?.name

    @TypeConverter
    fun fromName(name: String?): ReviewRating? = name?.let { ReviewRating.valueOf(it) }
}
