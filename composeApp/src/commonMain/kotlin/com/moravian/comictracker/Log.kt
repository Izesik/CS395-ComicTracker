package com.moravian.comictracker

internal expect object AppLog {
    fun d(
        tag: String,
        message: String,
    )

    fun e(
        tag: String,
        message: String,
        throwable: Throwable? = null,
    )
}
