package com.moravian.comictracker.network

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

private const val BASE_URL = "https://comicvine.gamespot.com/api"

// Restrict list results to Everyone (1) and Teen (2); excludes Mature (3)
// ComicVine got some bad stuff so for the project lets just exclude it
private const val SAFE_RATING = "content_rating:1|2"

// Combines an optional caller-specific filter with the NSFW filter
private fun safeFilter(additional: String = "") =
    if (additional.isEmpty()) SAFE_RATING else "$additional,$SAFE_RATING"

class ComicVineApi {
    private val client: HttpClient = createHttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                coerceInputValues = true
            })
        }
    }

    private fun HttpRequestBuilder.defaults() {
        parameter("format", "json")
        parameter("api_key", ApiConfig.API_KEY)
    }

    suspend fun search(
        query: String,
        resources: String = "volume",
        limit: Int = 20,
        offset: Int = 0
    ): ComicVineListResponse<ComicVineSearchResult> =
        client.get("$BASE_URL/search/") {
            defaults()
            parameter("query", query)
            parameter("resources", resources)
            parameter("limit", limit)
            parameter("offset", offset)
            parameter("filter", safeFilter())
            parameter("field_list", "id,name,resource_type,image,publisher,start_year,issue_number,volume")
        }.body()

    // Single-resource endpoints fetch by ID — no list filter needed
    suspend fun getVolume(id: Int): ComicVineSingleResponse<ComicVineVolume> =
        client.get("$BASE_URL/volume/4050-$id/") {
            defaults()
            parameter("field_list", "id,name,start_year,count_of_issues,deck,description,image,publisher,characters")
        }.body()

    suspend fun getIssuesByVolume(
        volumeId: Int,
        limit: Int = 100,
        offset: Int = 0
    ): ComicVineListResponse<ComicVineIssue> =
        client.get("$BASE_URL/issues/") {
            defaults()
            parameter("filter", safeFilter("volume:$volumeId"))
            parameter("limit", limit)
            parameter("offset", offset)
            parameter("field_list", "id,name,issue_number,cover_date,store_date,image,volume")
        }.body()

    suspend fun getIssue(id: Int): ComicVineSingleResponse<ComicVineIssue> =
        client.get("$BASE_URL/issue/4000-$id/") {
            defaults()
            parameter("field_list", "id,name,issue_number,cover_date,store_date,deck,description,image,volume,characters")
        }.body()

    suspend fun getPublisher(id: Int): ComicVineSingleResponse<ComicVinePublisher> =
        client.get("$BASE_URL/publisher/4010-$id/") {
            defaults()
            parameter("field_list", "id,name,description,image")
        }.body()

    suspend fun getPopularVolumes(limit: Int = 30): ComicVineListResponse<ComicVineVolume> =
        client.get("$BASE_URL/volumes/") {
            defaults()
            parameter("sort", "count_of_issues:desc")
            parameter("limit", limit)
            parameter("field_list", "id,name,start_year,count_of_issues,image,publisher,content_rating")
        }.body()

    suspend fun getPopularIssues(limit: Int = 30): ComicVineListResponse<ComicVineIssue> =
        client.get("$BASE_URL/issues/") {
            defaults()
            parameter("filter", "has_staff_review:true")
            parameter("sort", "date_last_updated:desc")
            parameter("limit", limit)
            parameter("field_list", "id,name,issue_number,cover_date,image,volume,content_rating")
        }.body()

    suspend fun searchByUpc(upc: String): ComicVineListResponse<ComicVineIssue> =
        client.get("$BASE_URL/issues/") {
            defaults()
            parameter("filter", safeFilter("upc:$upc"))
            parameter("field_list", "id,name,issue_number,cover_date,image,volume")
        }.body()
}
