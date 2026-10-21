package it.maicol07.gamerlogue.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * `users/{id}/stats`. Ratings are on the server's 0–100 scale; platform figures count each entry on
 * every one of its platforms, so they don't add up.
 */
@Serializable
data class UserStats(
    @SerialName("status_counts") val statusCounts: Map<String, Int> = emptyMap(),
    @SerialName("completion_status_counts") val completionStatusCounts: Map<String, Int> = emptyMap(),
    @SerialName("owned_count") val ownedCount: Int = 0,
    @SerialName("total_played_time") val totalPlayedTime: Int = 0,
    @SerialName("average_rating") val averageRating: Double? = null,
    /** 11 buckets: bucket N counts ratings in [10N, 10N + 10), 100 in the last. */
    @SerialName("rating_histogram") val ratingHistogram: List<Int> = emptyList(),
    val platforms: List<PlatformStats> = emptyList(),
    @SerialName("completed_by_year") val completedByYear: Map<String, Int> = emptyMap(),
    @SerialName("started_by_year") val startedByYear: Map<String, Int> = emptyMap(),
    @SerialName("reviews_count") val reviewsCount: Int = 0,
) {
    @Serializable
    data class PlatformStats(
        @SerialName("platform_id") val platformId: Int,
        @SerialName("entries_count") val entriesCount: Int,
        @SerialName("played_time") val playedTime: Int,
    )
}
