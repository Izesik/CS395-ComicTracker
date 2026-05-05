package com.moravian.comictracker.data

/** Tracks how far the user has read a given comic issue. */
enum class ReadStatus {
    /** The issue has been saved but not yet started. */
    TO_READ,
    /** The issue is currently being read. */
    READING,
    /** The issue has been fully read. */
    READ,
    /** The user stopped reading this issue without finishing it. */
    DROPPED
}
