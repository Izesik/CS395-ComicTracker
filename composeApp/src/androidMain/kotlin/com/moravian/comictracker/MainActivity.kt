package com.moravian.comictracker

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.room.RoomDatabase
import com.moravian.comictracker.data.ComicTrackerDatabase
import com.moravian.comictracker.data.UserPreferencesRepository
import com.moravian.comictracker.data.getComicTrackerDatabase
import okio.Path.Companion.toPath

/**
 * Single Activity that hosts the entire Compose UI tree.
 *
 * Constructs the shared [ComicTrackerDatabase] and [UserPreferencesRepository] instances
 * and passes them into [App]. Configuration changes listed in the manifest are handled
 * internally without recreating this Activity.
 */
class MainActivity : ComponentActivity() {
    private val prefsDataStore by lazy {
        PreferenceDataStoreFactory.createWithPath {
            filesDir.resolve("user_prefs.preferences_pb").absolutePath.toPath()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        setContent {
            App(
                database = getComicTrackerDatabase(getDatabaseBuilder(this)),
                prefsRepository = UserPreferencesRepository(prefsDataStore)
            )
        }
    }
}

/** Creates a Room [RoomDatabase.Builder] for [ComicTrackerDatabase] using the app's files directory. */
fun getDatabaseBuilder(context: Context): RoomDatabase.Builder<ComicTrackerDatabase> {
    val appContext = context.applicationContext
    val dbFile = appContext.getDatabasePath("comictracker.db")
    return Room.databaseBuilder<ComicTrackerDatabase>(
        context = appContext,
        name = dbFile.absolutePath,
    )
}
