package com.moravian.comictracker

import androidx.compose.ui.window.ComposeUIViewController
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.room.RoomDatabase
import com.moravian.comictracker.data.ComicTrackerDatabase
import com.moravian.comictracker.data.UserPreferencesRepository
import com.moravian.comictracker.data.getComicTrackerDatabase
import kotlinx.io.files.Path
import okio.Path.Companion.toPath
import platform.Foundation.NSHomeDirectory

fun MainViewController() = ComposeUIViewController {
    App(
        database = getComicTrackerDatabase(getDatabaseBuilder()),
        prefsRepository = UserPreferencesRepository(iosPrefsDataStore)
    )
}.apply {
    this.view.setNeedsLayout()
}

// Allows for the app to run in iOS emulator
private val iosPrefsDataStore by lazy {
    PreferenceDataStoreFactory.createWithPath {
        (NSHomeDirectory() + "/Documents/user_prefs.preferences_pb").toPath()
    }
}

fun getDatabaseBuilder(): RoomDatabase.Builder<ComicTrackerDatabase> {
    val dbFilePath = NSHomeDirectory() + "/Documents/survey.db"
    return Room.databaseBuilder<ComicTrackerDatabase>(
        name = dbFilePath,
    )
}
