package com.moravian.comictracker.network

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

class MetronApi {
    private val client: HttpClient = createHttpClient {
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
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                coerceInputValues = true
            })
        }
    }

    suspend fun searchSeries(
        query: String,
        limit: Int = 20
    ): MetronPagedResponse<MetronSeriesSummary> {
        val response = client.get("$METRON_BASE_URL/series/") {
            parameter("name", query)
            parameter("limit", limit)
        }
        if (!response.status.isSuccess()) throw Exception("Metron error: ${response.status}")
        return response.body()
    }

    suspend fun searchByUpc(upc: String): MetronPagedResponse<MetronIssueSummary> {
        val response = client.get("$METRON_BASE_URL/issue/") {
            parameter("upc", upc)
        }
        if (!response.status.isSuccess()) throw Exception("Metron error: ${response.status}")
        return response.body()
    }

    // Ongoing series sorted by most recently modified — best proxy for "active/popular"
    suspend fun getPopularSeries(limit: Int = 30): MetronPagedResponse<MetronSeriesSummary> {
        val response = client.get("$METRON_BASE_URL/series/") {
            parameter("series_type", 1)   // 1 = Ongoing Series
            parameter("ordering", "-modified")
            parameter("limit", limit)
        }
        if (!response.status.isSuccess()) throw Exception("Metron error: ${response.status}")
        return response.body()
    }

    // Most recently cover-dated issues
    suspend fun getRecentIssues(limit: Int = 30): MetronPagedResponse<MetronIssueSummary> {
        val response = client.get("$METRON_BASE_URL/issue/") {
            parameter("ordering", "-cover_date")
            parameter("limit", limit)
        }
        if (!response.status.isSuccess()) throw Exception("Metron error: ${response.status}")
        return response.body()
    }
}
