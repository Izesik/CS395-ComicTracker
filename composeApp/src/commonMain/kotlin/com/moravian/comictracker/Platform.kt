package com.moravian.comictracker

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform