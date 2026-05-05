package com.moravian.comictracker.network

/**
 * Converts a network [Throwable] into a user-facing error message.
 *
 * Detects common failure modes (missing API key, DNS failure, timeout) and
 * returns a plain-English string. Falls back to the raw exception message or
 * [fallback] if no known pattern is matched.
 *
 * @param serviceName Display name of the service that failed (e.g. "ComicVine").
 * @param fallback Message to use when no specific error pattern is recognised.
 */
fun Throwable.toUserFacingNetworkMessage(serviceName: String, fallback: String): String {
    val details = messageChain().lowercase()
    return when {
        "api key is missing" in details ->
            "$serviceName API key is missing. Add it to local.properties and rebuild the app."
        "unable to resolve host" in details ||
            "no address associated with hostname" in details ||
            "nodename nor servname provided" in details ||
            "host is unresolved" in details ->
            "$serviceName is unreachable. Check your internet connection or DNS, then try again."
        "timed out" in details || "timeout" in details ->
            "$serviceName took too long to respond. Check your connection and try again."
        else -> message ?: fallback
    }
}

/** Collects the messages from this throwable and all its causes into a single string. */
private fun Throwable.messageChain(): String {
    val messages = mutableListOf<String>()
    var current: Throwable? = this
    while (current != null) {
        current.message?.let(messages::add)
        current = current.cause
    }
    return messages.joinToString(separator = " ")
}
