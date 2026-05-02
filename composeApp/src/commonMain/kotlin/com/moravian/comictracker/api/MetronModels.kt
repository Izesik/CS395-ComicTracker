package com.moravian.comictracker.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MetronPagedResponse<T>(
    val count: Int,
    val next: String? = null,
    val previous: String? = null,
    val results: List<T>
)

@Serializable
data class MetronIssueSummary(
    val id: Int,
    @SerialName("cv_id") val cvId: Int? = null
)

@Serializable
data class MetronIssue(
    val id: Int,
    @SerialName("cv_id") val cvId: Int? = null
)

@Serializable
data class ComicVinePagedResponse<T>(
    val error: String = "",
    val results: List<T> = emptyList(),
    @SerialName("status_code") val statusCode: Int = 0
)

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

@Serializable
data class ComicVineIssueSummary(
    val id: Int,
    val volume: ComicVineVolumeRef? = null,
    @SerialName("issue_number") val issueNumber: String = "",
    @SerialName("cover_date") val coverDate: String? = null,
    val image: ComicVineImage? = null
)

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

@Serializable
data class ComicVinePublisher(
    val id: Int,
    val name: String = ""
)

@Serializable
data class ComicVineVolumeRef(
    val id: Int = 0,
    val name: String = ""
)

@Serializable
data class ComicVineCredit(
    val id: Int = 0,
    val name: String = "",
    val role: String? = null
)

@Serializable
data class ComicVineCharacter(
    val id: Int = 0,
    val name: String = ""
)

@Serializable
data class ComicVineImage(
    @SerialName("medium_url") val mediumUrl: String? = null,
    @SerialName("small_url") val smallUrl: String? = null,
    @SerialName("super_url") val superUrl: String? = null,
    @SerialName("original_url") val originalUrl: String? = null
)

fun ComicVineImage.coverUrl(): String? = mediumUrl ?: smallUrl ?: superUrl ?: originalUrl
