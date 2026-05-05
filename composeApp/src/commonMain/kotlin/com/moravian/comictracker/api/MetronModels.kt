package com.moravian.comictracker.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Paginated list response from the Metron API. */
@Serializable
data class MetronPagedResponse<T>(
    /** Total number of results available on the server. */
    val count: Int,
    /** URL of the next page, or null if this is the last page. */
    val next: String? = null,
    /** URL of the previous page, or null if this is the first page. */
    val previous: String? = null,
    /** Results on this page. */
    val results: List<T>
)

/** Lightweight issue summary returned by Metron UPC search results. */
@Serializable
data class MetronIssueSummary(
    /** Metron internal issue ID. */
    val id: Int,
    /** Corresponding ComicVine issue ID, used to look up full issue data. */
    @SerialName("cv_id") val cvId: Int? = null
)

/** Full issue detail from the Metron API. */
@Serializable
data class MetronIssue(
    /** Metron internal issue ID. */
    val id: Int,
    /** Corresponding ComicVine issue ID, used to look up full issue data. */
    @SerialName("cv_id") val cvId: Int? = null
)

/** Paginated list response from the ComicVine API. */
@Serializable
data class ComicVinePagedResponse<T>(
    /** ComicVine API status message (typically "OK"). */
    val error: String = "",
    /** Results on this page. */
    val results: List<T> = emptyList(),
    /** ComicVine numeric status code (1 = OK). */
    @SerialName("status_code") val statusCode: Int = 0
)

/**
 * A comic series (volume) from the ComicVine API.
 *
 * @property id ComicVine numeric volume ID.
 * @property name Series title.
 * @property publisher Publisher information, if available.
 * @property startYear Year the series began publication.
 * @property image Cover image URLs at various sizes.
 * @property description HTML description of the series.
 * @property countOfIssues Total number of issues in this volume.
 */
@Serializable
data class ComicVineVolume(
    val id: Int,
    val name: String = "",
    val publisher: ComicVinePublisher? = null,
    @SerialName("start_year") val startYear: String? = null,
    val image: ComicVineImage? = null,
    val description: String? = null,
    @SerialName("count_of_issues") val countOfIssues: Int? = null
)

/**
 * Lightweight issue summary used in list and search results.
 *
 * @property id ComicVine numeric issue ID.
 * @property volume Parent series reference.
 * @property issueNumber Issue number as a string (may contain decimals like "1.5").
 * @property coverDate Publication cover date (YYYY-MM-DD format).
 * @property image Cover image URLs at various sizes.
 */
@Serializable
data class ComicVineIssueSummary(
    val id: Int,
    val volume: ComicVineVolumeRef? = null,
    @SerialName("issue_number") val issueNumber: String = "",
    @SerialName("cover_date") val coverDate: String? = null,
    val image: ComicVineImage? = null
)

/**
 * Full issue detail from the ComicVine API.
 *
 * @property id ComicVine numeric issue ID.
 * @property volume Parent series reference.
 * @property issueNumber Issue number as a string.
 * @property coverDate Publication cover date.
 * @property storeDate Date the issue appeared in stores.
 * @property image Cover image URLs at various sizes.
 * @property description HTML description of the issue.
 * @property personCredits Creator credits (writer, artist, etc.).
 * @property characterCredits Characters appearing in this issue.
 */
@Serializable
data class ComicVineIssue(
    val id: Int,
    val volume: ComicVineVolumeRef? = null,
    @SerialName("issue_number") val issueNumber: String = "",
    @SerialName("cover_date") val coverDate: String? = null,
    @SerialName("store_date") val storeDate: String? = null,
    val image: ComicVineImage? = null,
    val description: String? = null,
    @SerialName("person_credits") val personCredits: List<ComicVineCredit> = emptyList(),
    @SerialName("character_credits") val characterCredits: List<ComicVineCharacter> = emptyList()
)

/** Publisher information embedded in series results. */
@Serializable
data class ComicVinePublisher(
    /** ComicVine numeric publisher ID. */
    val id: Int,
    /** Publisher display name. */
    val name: String = ""
)

/** Minimal series reference embedded inside issue results. */
@Serializable
data class ComicVineVolumeRef(
    /** ComicVine numeric volume ID. */
    val id: Int = 0,
    /** Series title. */
    val name: String = ""
)

/** A single creator credit on a comic issue. */
@Serializable
data class ComicVineCredit(
    /** ComicVine numeric person ID. */
    val id: Int = 0,
    /** Creator display name. */
    val name: String = "",
    /** Role description (e.g. "Writer", "Penciler"). */
    val role: String? = null
)

/** A character appearing in a comic issue. */
@Serializable
data class ComicVineCharacter(
    /** ComicVine numeric character ID. */
    val id: Int = 0,
    /** Character display name. */
    val name: String = ""
)

/** A set of image URLs for a comic cover at multiple resolutions. */
@Serializable
data class ComicVineImage(
    @SerialName("medium_url") val mediumUrl: String? = null,
    @SerialName("small_url") val smallUrl: String? = null,
    @SerialName("super_url") val superUrl: String? = null,
    @SerialName("original_url") val originalUrl: String? = null
)

/** Returns the best available cover URL, preferring medium quality over small, super, and original. */
fun ComicVineImage.coverUrl(): String? = mediumUrl ?: smallUrl ?: superUrl ?: originalUrl
