package com.moravian.comictracker

import androidx.compose.ui.window.ComposeUIViewController
import androidx.room.Room
import androidx.room.RoomDatabase
import com.moravian.comictracker.data.ComicTrackerDatabase
import com.moravian.comictracker.data.getComicTrackerDatabase
import platform.Foundation.NSHomeDirectory

fun MainViewController() = ComposeUIViewController { App(getComicTrackerDatabase(getDatabaseBuilder())) }


fun getDatabaseBuilder(): RoomDatabase.Builder<ComicTrackerDatabase> {
    val dbFilePath = NSHomeDirectory() + "/Documents/survey.db"

    return Room.databaseBuilder<ComicTrackerDatabase>(
        name = dbFilePath,
    )
}