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

// Home feed content filtering: You'd think ComicVine would have a "mature content" flag or something, but ggs

/** Publishers whose content is suitable for the home feed. */
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

/** Title terms that indicate content not suitable for the home feed. */
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

/**
 * Client for the ComicVine REST API.
 *
 * All requests include the API key from build configuration and request
 * only the fields needed for each use case to minimise payload size.
 */
class ComicVineApi {
    private val client: HttpClient = createJsonClient()

    /**
     * Searches for series (volumes) matching [query].
     *
     * @param limit Maximum number of results to return.
     * @return List of matching [ComicVineVolume] results.
     */
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

    /**
     * Returns a curated list of recently updated, home-safe series.
     *
     * @param limit Maximum number of series to return.
     * @return Filtered list of [ComicVineVolume] results suitable for the home feed.
     */
    suspend fun getPopularSeries(limit: Int = 12): List<ComicVineVolume> {
        return getHomeSafeSeries(limit, HOME_SERIES_MAX_PAGES)
    }

    /**
     * Returns recent issues from home-safe series, sorted by cover date descending.
     *
     * @param limit Maximum number of issues to return.
     * @return Filtered and sorted list of [ComicVineIssueSummary].
     */
    suspend fun getRecentIssues(limit: Int = 30): List<ComicVineIssueSummary> {
        return getHomeSafeSeries(HOME_ISSUE_VOLUME_COUNT, HOME_SERIES_MAX_PAGES)
            .flatMap { series -> getRecentIssuesByVolume(series.id, HOME_ISSUES_PER_VOLUME) }
            .filter(::isHomeSafeIssue)
            .sortedByDescending { it.coverDate.orEmpty() }
            .take(limit)
    }

    /**
     * Fetches the full detail for a single series by its ComicVine [id].
     *
     * @return The matching [ComicVineVolume] with description and creator fields populated.
     */
    suspend fun getVolume(id: Int): ComicVineVolume {
        val response = client.get("$COMICVINE_BASE_URL/volume/4050-$id/") {
            comicVineParameters()
            parameter("field_list", VOLUME_DETAIL_FIELDS)
        }
        if (!response.status.isSuccess()) throw Exception("ComicVine error: ${response.status}")
        return response.body<ComicVineSingleResponse<ComicVineVolume>>().results
    }

    /**
     * Returns up to [limit] issues belonging to the series with ComicVine [volumeId].
     *
     * @return Issues sorted by issue number ascending.
     */
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

    /**
     * Fetches the full detail for a single issue by its ComicVine [id].
     *
     * @return The matching [ComicVineIssue] with credits and characters populated.
     */
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
        /** Minimal fields requested for series list and search endpoints. */
        const val VOLUME_LIST_FIELDS = "id,name,publisher,start_year,image,count_of_issues"
        /** Full fields requested for the series detail endpoint. */
        const val VOLUME_DETAIL_FIELDS = "id,name,publisher,start_year,image,description,count_of_issues"
        /** Minimal fields requested for issue list endpoints. */
        const val ISSUE_LIST_FIELDS = "id,volume,issue_number,cover_date,image"
        /** Full fields requested for the issue detail endpoint. */
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

/**
 * Client for the Metron REST API, used for UPC barcode lookups.
 *
 * Requires HTTP Basic Auth credentials from build configuration.
 * Barcode results include a ComicVine ID that is used to open the issue detail screen.
 */
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

    /**
     * Searches for issues matching the given [upc] barcode string.
     *
     * @return Paged response containing zero or more [MetronIssueSummary] results.
     */
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

    /**
     * Fetches the full detail for a Metron issue by its internal [id].
     *
     * Used when a UPC search result does not yet include a ComicVine ID.
     *
     * @return The matching [MetronIssue] which includes the ComicVine cross-reference ID.
     */
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

/** Wrapper for single-object ComicVine API responses (as opposed to paged list responses). */
@kotlinx.serialization.Serializable
data class ComicVineSingleResponse<T>(
    /** ComicVine API status message. */
    val error: String = "",
    /** The single result object. */
    val results: T,
    /** ComicVine numeric status code (1 = OK). */
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
