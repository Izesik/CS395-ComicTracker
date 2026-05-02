package com.moravian.comictracker

import platform.Foundation.NSLog

internal actual object AppLog {
    actual fun d(tag: String, message: String) {
        NSLog("$tag: $message")
    }

    actual fun e(tag: String, message: String, throwable: Throwable?) {
        NSLog("$tag: $message ${throwable?.message.orEmpty()}")
    }
}
