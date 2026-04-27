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

    //searches volumes or issues based on whatever keyword the user types in
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
            parameter("field_list", "id,name,resource_type,image,publisher,start_year,issue_number,volume")
        }.body()

    //gets the details for one volume
    suspend fun getVolume(id: Int): ComicVineSingleResponse<ComicVineVolume> =
        client.get("$BASE_URL/volume/4050-$id/") {
            defaults()
            parameter("field_list", "id,name,start_year,count_of_issues,deck,description,image,publisher,characters")
        }.body()

    //pulls all the issues that belong to one volume
    suspend fun getIssuesByVolume(
        volumeId: Int,
        limit: Int = 100,
        offset: Int = 0
    ): ComicVineListResponse<ComicVineIssue> =
        client.get("$BASE_URL/issues/") {
            defaults()
            parameter("filter", "volume:$volumeId")
            parameter("limit", limit)
            parameter("offset", offset)
            parameter("field_list", "id,name,issue_number,cover_date,store_date,image,volume")
        }.body()

    //gets the details for one specific issue
    suspend fun getIssue(id: Int): ComicVineSingleResponse<ComicVineIssue> =
        client.get("$BASE_URL/issue/4000-$id/") {
            defaults()
            parameter("field_list", "id,name,issue_number,cover_date,store_date,description,image,volume")
        }.body()

    //gets publisher info when we need it
    suspend fun getPublisher(id: Int): ComicVineSingleResponse<ComicVinePublisher> =
        client.get("$BASE_URL/publisher/4010-$id/") {
            defaults()
            parameter("field_list", "id,name,description,image")
        }.body()

    //grabs the most recently added Marvel volumes (series) for the home screen
    suspend fun getLatestMarvelVolumes(limit: Int = 10): ComicVineListResponse<ComicVineVolume> =
        client.get("$BASE_URL/volumes/") {
            defaults()
            parameter("filter", "publisher:4010-31")
            parameter("limit", limit)
            parameter("field_list", "id,name,start_year,count_of_issues,image,publisher")
        }.body()

    //looks up an issue by UPC for the barcode scan feature
    suspend fun searchByUpc(upc: String): ComicVineListResponse<ComicVineIssue> =
        client.get("$BASE_URL/issues/") {
            defaults()
            parameter("filter", "upc:$upc")
            parameter("field_list", "id,name,issue_number,cover_date,image,volume")
        }.body()
}
