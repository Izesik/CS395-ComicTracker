package com.moravian.comictracker.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ComicVineListResponse<T>(
    val error: String,
    @SerialName("status_code") val statusCode: Int,
    val limit: Int,
    val offset: Int,
    @SerialName("number_of_page_results") val pageResults: Int,
    @SerialName("number_of_total_results") val totalResults: Int,
    val results: List<T>
)

@Serializable
data class ComicVineSingleResponse<T>(
    val error: String,
    @SerialName("status_code") val statusCode: Int,
    val results: T
)

@Serializable
data class ComicVineVolume(
    val id: Int,
    val name: String,
    @SerialName("start_year") val startYear: String? = null,
    @SerialName("count_of_issues") val issueCount: Int = 0,
    val deck: String? = null,
    val description: String? = null,
    val image: ComicVineImage? = null,
    val publisher: ComicVinePublisherRef? = null,
    val characters: List<ComicVineCharacterRef> = emptyList()
)

@Serializable
data class ComicVineIssue(
    val id: Int,
    val name: String? = null,
    @SerialName("issue_number") val issueNumber: String,
    @SerialName("cover_date") val coverDate: String? = null,
    @SerialName("store_date") val storeDate: String? = null,
    val description: String? = null,
    val image: ComicVineImage? = null,
    val volume: ComicVineVolumeRef? = null
)

@Serializable
data class ComicVinePublisher(
    val id: Int,
    val name: String,
    val description: String? = null,
    val image: ComicVineImage? = null
)

@Serializable
data class ComicVineImage(
    @SerialName("medium_url") val mediumUrl: String? = null,
    @SerialName("small_url") val smallUrl: String? = null,
    @SerialName("original_url") val originalUrl: String? = null
)

@Serializable
data class ComicVinePublisherRef(
    val id: Int,
    val name: String
)

@Serializable
data class ComicVineCharacterRef(
    val id: Int,
    val name: String
)

@Serializable
data class ComicVineVolumeRef(
    val id: Int,
    val name: String
)

@Serializable
data class ComicVineSearchResult(
    val id: Int,
    val name: String,
    @SerialName("resource_type") val resourceType: String,
    val image: ComicVineImage? = null,
    val publisher: ComicVinePublisherRef? = null,
    @SerialName("start_year") val startYear: String? = null,
    @SerialName("issue_number") val issueNumber: String? = null,
    val volume: ComicVineVolumeRef? = null
)
