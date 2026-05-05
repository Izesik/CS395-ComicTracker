package com.moravian.comictracker.network

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig

/**
 * Creates a platform-specific [HttpClient] with the given [config] applied.
 *
 * Android uses the OkHttp engine; iOS uses the native Darwin engine.
 * Actuals are in androidMain and iosMain respectively.
 */
expect fun createHttpClient(config: HttpClientConfig<*>.() -> Unit): HttpClient
