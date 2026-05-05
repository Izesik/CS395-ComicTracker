package com.moravian.comictracker.data

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import kotlinx.coroutines.Dispatchers
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.IO

/**
 * Room database that holds all locally persisted collection data.
 *
 * Contains [SeriesEntity], [ComicIssueEntity], and [CreatorEntity] tables.
 * Use [getComicTrackerDatabase] to build an instance.
 */
@Database(entities = [SeriesEntity::class, ComicIssueEntity::class, CreatorEntity::class], version = 5)
@ConstructedBy(ComicTrackerConstructor::class)
abstract class ComicTrackerDatabase : RoomDatabase() {
    /** Returns the DAO for all collection read/write operations. */
    abstract fun comicDao(): ComicDao
}

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object ComicTrackerConstructor : RoomDatabaseConstructor<ComicTrackerDatabase>

/**
 * Builds and returns a [ComicTrackerDatabase] from the given [builder].
 *
 * Applies the bundled SQLite driver and a coroutine dispatcher for query execution.
 * Destructive migration is enabled so schema upgrades never crash the app during development.
 */
fun getComicTrackerDatabase(
    builder: androidx.room.RoomDatabase.Builder<ComicTrackerDatabase>,
): ComicTrackerDatabase = builder
    .fallbackToDestructiveMigration(true)
    .setDriver(BundledSQLiteDriver())
    .setQueryCoroutineContext(Dispatchers.IO)
    .build()
