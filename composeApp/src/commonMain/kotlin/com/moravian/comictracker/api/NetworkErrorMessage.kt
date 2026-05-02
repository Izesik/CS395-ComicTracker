package com.moravian.comictracker.network

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

private fun Throwable.messageChain(): String {
    val messages = mutableListOf<String>()
    var current: Throwable? = this
    while (current != null) {
        current.message?.let(messages::add)
        current = current.cause
    }
    return messages.joinToString(separator = " ")
}
