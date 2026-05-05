package com.moravian.comictracker.network

import com.moravian.comictracker.AppLog
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BasicAuthCredentials
import io.ktor.client.plugins.auth.providers.basic
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

private const val METRON_BASE_URL = "https://metron.cloud/api"
private const val COMICVINE_BASE_URL = "https://comicvine.gamespot.com/api"
private const val COMICVINE_MAX_LIST_LIMIT = 100
private const val HOME_SERIES_MAX_PAGES = 5
private const val HOME_ISSUE_VOLUME_COUNT = 18
private const val HOME_ISSUES_PER_VOLUME = 2

private val HOME_SAFE_PUBLISHERS = setOf(
    "archie comics",
    "boom! studios",
    "dark horse comics",
    "dc comics",
    "dell",
    "dynamite entertainment",
    "idw publishing",
    "image comics",
    "marvel",
    "marvel comics",
    "oni press",
    "scholastic",
    "valiant comics",
    "viz media"
)

private val HOME_BLOCKED_TITLE_TERMS = listOf(
    "adult",
    "after dark",
    "bad girl",
    "barbarella",
    "belladonna",
    "bondage",
    "cavewoman",
    "crossed",
    "dejah thoris",
    "erotic",
    "eros comix",
    "grimm fairy tales",
    "hellina",
    "hellwitch",
    "hentai",
    "jungle fantasy",
    "la muerta",
    "lady death",
    "lookers",
    "mature",
    "nude",
    "pin-up",
    "pinup",
    "playboy",
    "porn",
    "sex",
    "tarot: witch of the black rose",
    "threshold",
    "zombie tramp"
)

class ComicVineApi {
    private val client: HttpClient = createJsonClient()

    suspend fun searchVolumes(query: String, limit: Int = 20): List<ComicVineVolume> {
        val response = client.get("$COMICVINE_BASE_URL/search/") {
            comicVineParameters()
            parameter("query", query)
            parameter("resources", "volume")
            parameter("field_list", VOLUME_LIST_FIELDS)
            parameter("limit", limit)
        }
        if (!response.status.isSuccess()) throw Exception("ComicVine error: ${response.status}")
        return response.body<ComicVinePagedResponse<ComicVineVolume>>().results
    }

    suspend fun getPopularSeries(limit: Int = 12): List<ComicVineVolume> {
        return getHomeSafeSeries(limit, HOME_SERIES_MAX_PAGES)
    }

    suspend fun getRecentIssues(limit: Int = 30): List<ComicVineIssueSummary> {
        return getHomeSafeSeries(HOME_ISSUE_VOLUME_COUNT, HOME_SERIES_MAX_PAGES)
            .flatMap { series -> getRecentIssuesByVolume(series.id, HOME_ISSUES_PER_VOLUME) }
            .filter(::isHomeSafeIssue)
            .sortedByDescending { it.coverDate.orEmpty() }
            .take(limit)
    }

    suspend fun getVolume(id: Int): ComicVineVolume {
        val response = client.get("$COMICVINE_BASE_URL/volume/4050-$id/") {
            comicVineParameters()
            parameter("field_list", VOLUME_DETAIL_FIELDS)
        }
        if (!response.status.isSuccess()) throw Exception("ComicVine error: ${response.status}")
        return response.body<ComicVineSingleResponse<ComicVineVolume>>().results
    }

    suspend fun getIssuesByVolume(volumeId: Int, limit: Int = 100): List<ComicVineIssueSummary> {
        val response = client.get("$COMICVINE_BASE_URL/issues/") {
            comicVineParameters()
            parameter("field_list", ISSUE_LIST_FIELDS)
            parameter("filter", "volume:$volumeId")
            parameter("sort", "issue_number:asc")
            parameter("limit", limit)
        }
        if (!response.status.isSuccess()) throw Exception("ComicVine error: ${response.status}")
        return response.body<ComicVinePagedResponse<ComicVineIssueSummary>>().results
    }

    suspend fun getIssue(id: Int): ComicVineIssue {
        val response = client.get("$COMICVINE_BASE_URL/issue/4000-$id/") {
            comicVineParameters()
            parameter("field_list", ISSUE_DETAIL_FIELDS)
        }
        if (!response.status.isSuccess()) throw Exception("ComicVine error: ${response.status}")
        return response.body<ComicVineSingleResponse<ComicVineIssue>>().results
    }

    private suspend fun getHomeSafeSeries(limit: Int, maxPages: Int): List<ComicVineVolume> {
        val series = mutableListOf<ComicVineVolume>()
        var offset = 0

        repeat(maxPages) {
            val response = client.get("$COMICVINE_BASE_URL/volumes/") {
                comicVineParameters()
                parameter("field_list", VOLUME_LIST_FIELDS)
                parameter("sort", "date_last_updated:desc")
                parameter("limit", COMICVINE_MAX_LIST_LIMIT)
                parameter("offset", offset)
            }
            if (!response.status.isSuccess()) throw Exception("ComicVine error: ${response.status}")

            val page = response.body<ComicVinePagedResponse<ComicVineVolume>>().results
            series += page.filter(::isHomeSafeSeries)
            if (series.size >= limit || page.size < COMICVINE_MAX_LIST_LIMIT) return series.take(limit)

            offset += COMICVINE_MAX_LIST_LIMIT
        }

        return series.take(limit)
    }

    private suspend fun getRecentIssuesByVolume(volumeId: Int, limit: Int): List<ComicVineIssueSummary> {
        val response = client.get("$COMICVINE_BASE_URL/issues/") {
            comicVineParameters()
            parameter("field_list", ISSUE_LIST_FIELDS)
            parameter("filter", "volume:$volumeId")
            parameter("sort", "cover_date:desc")
            parameter("limit", limit)
        }
        if (!response.status.isSuccess()) throw Exception("ComicVine error: ${response.status}")
        return response.body<ComicVinePagedResponse<ComicVineIssueSummary>>().results
    }

    private fun io.ktor.client.request.HttpRequestBuilder.comicVineParameters() {
        val apiKey = MetronConfig.COMICVINE_API_KEY
        if (apiKey.isBlank()) throw IllegalStateException("ComicVine API key is missing")
        parameter("api_key", apiKey)
        parameter("format", "json")
    }

    private companion object {
        const val VOLUME_LIST_FIELDS = "id,name,publisher,start_year,image,count_of_issues"
        const val VOLUME_DETAIL_FIELDS = "id,name,publisher,start_year,image,description,count_of_issues"
        const val ISSUE_LIST_FIELDS = "id,volume,issue_number,cover_date,image"
        const val ISSUE_DETAIL_FIELDS =
            "id,volume,issue_number,cover_date,store_date,image,description,person_credits,character_credits"
    }
}

private fun isHomeSafeSeries(series: ComicVineVolume): Boolean {
    val title = series.name.normalizedHomeFeedText()
    val publisher = series.publisher?.name?.normalizedHomeFeedText()

    if (HOME_BLOCKED_TITLE_TERMS.any { it in title }) return false
    return publisher == null || publisher in HOME_SAFE_PUBLISHERS
}

private fun isHomeSafeIssue(issue: ComicVineIssueSummary): Boolean {
    val volumeTitle = issue.volume?.name?.normalizedHomeFeedText().orEmpty()

    if (volumeTitle.isBlank()) return false
    return HOME_BLOCKED_TITLE_TERMS.none { it in volumeTitle }
}

private fun String.normalizedHomeFeedText(): String = lowercase().trim()

class MetronApi {
    private val client: HttpClient = createJsonClient {
        install(Auth) {
            basic {
                credentials {
                    BasicAuthCredentials(
                        username = MetronConfig.USERNAME,
                        password = MetronConfig.PASSWORD
                    )
                }
                sendWithoutRequest { true }
            }
        }
    }

    suspend fun searchByUpc(upc: String): MetronPagedResponse<MetronIssueSummary> {
        AppLog.d(TAG, "GET $METRON_BASE_URL/issue/?upc=$upc")
        val response = client.get("$METRON_BASE_URL/issue/") {
            parameter("upc", upc)
        }
        AppLog.d(TAG, "Metron UPC lookup status=${response.status.value} ${response.status.description}")
        if (!response.status.isSuccess()) {
            throw Exception("Metron error: ${response.status}")
        }

        val body = response.body<MetronPagedResponse<MetronIssueSummary>>()
        AppLog.d(
            TAG,
            "Metron UPC lookup returned count=${body.count}, results=${body.results.map { "id=${it.id}, cvId=${it.cvId}" }}"
        )
        return body
    }

    suspend fun getIssue(id: Int): MetronIssue {
        AppLog.d(TAG, "GET $METRON_BASE_URL/issue/$id/")
        val response = client.get("$METRON_BASE_URL/issue/$id/")
        AppLog.d(TAG, "Metron issue status=${response.status.value} ${response.status.description}")
        if (!response.status.isSuccess()) throw Exception("Metron error: ${response.status}")
        val issue = response.body<MetronIssue>()
        AppLog.d(TAG, "Metron issue id=${issue.id}, cvId=${issue.cvId}")
        return issue
    }

    private companion object {
        const val TAG = "ComicTrackerBarcode"
    }
}

@kotlinx.serialization.Serializable
data class ComicVineSingleResponse<T>(
    val error: String = "",
    val results: T,
    @kotlinx.serialization.SerialName("status_code") val statusCode: Int = 0
)

private fun createJsonClient(block: HttpClientConfigBuilder = {}): HttpClient =
    createHttpClient {
        block()
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                coerceInputValues = true
            })
        }
    }

private typealias HttpClientConfigBuilder = io.ktor.client.HttpClientConfig<*>.() -> Unit
