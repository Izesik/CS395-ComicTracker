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
data class MetronSeriesSummary(
    val id: Int,
    val name: String = "",
    val publisher: MetronPublisherRef? = null,
    @SerialName("year_began") val yearBegan: Int? = null,
    val image: String? = null,
    @SerialName("cv_id") val cvId: Int? = null
)

@Serializable
data class MetronIssueSummary(
    val id: Int,
    val series: MetronSeriesRef? = null,
    val number: String = "",
    @SerialName("cover_date") val coverDate: String? = null,
    val image: String? = null,
    val upc: String? = null,
    @SerialName("cv_id") val cvId: Int? = null
)

@Serializable
data class MetronSeries(
    val id: Int,
    val name: String = "",
    val publisher: MetronPublisherRef? = null,
    @SerialName("year_began") val yearBegan: Int? = null,
    val image: String? = null,
    val description: String? = null,
    @SerialName("cv_id") val cvId: Int? = null
)

@Serializable
data class MetronIssue(
    val id: Int,
    val publisher: MetronPublisherRef? = null,
    val series: MetronSeriesRef? = null,
    val number: String = "",
    @SerialName("cover_date") val coverDate: String? = null,
    @SerialName("store_date") val storeDate: String? = null,
    val image: String? = null,
    val description: String? = null,
    val credits: List<MetronCredit> = emptyList(),
    val characters: List<MetronCharacter> = emptyList(),
    @SerialName("cv_id") val cvId: Int? = null
)

@Serializable
data class MetronCredit(
    val id: Int,
    val creator: String,
    val role: List<MetronRole> = emptyList()
)

@Serializable
data class MetronRole(
    val id: Int,
    val name: String
)

@Serializable
data class MetronCharacter(
    val id: Int,
    val name: String
)

@Serializable
data class MetronPublisherRef(
    val id: Int,
    val name: String
)

@Serializable
data class MetronSeriesRef(
    val id: Int = 0,
    val name: String = "",
    val volume: Int? = null,
    @SerialName("year_began") val yearBegan: Int? = null
)
