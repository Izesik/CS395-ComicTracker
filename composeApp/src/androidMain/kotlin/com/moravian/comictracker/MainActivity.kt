package com.moravian.comictracker

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.room.Room
import androidx.room.RoomDatabase
import com.moravian.comictracker.data.ComicTrackerDatabase
import com.moravian.comictracker.data.getComicTrackerDatabase

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            App(getComicTrackerDatabase(getDatabaseBuilder(this)))
        }
    }
}

fun getDatabaseBuilder(context: Context): RoomDatabase.Builder<ComicTrackerDatabase> {
    val appContext = context.applicationContext
    val dbFile = appContext.getDatabasePath("comictracker.db")
    return Room.databaseBuilder<ComicTrackerDatabase>(
        context = appContext,
        name = dbFile.absolutePath,
    )
}
