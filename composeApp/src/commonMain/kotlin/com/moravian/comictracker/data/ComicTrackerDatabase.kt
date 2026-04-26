package com.moravian.comictracker.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [SeriesEntity::class, ComicIssueEntity::class, CreatorEntity::class], version = 1)
abstract class ComicTrackerDatabase : RoomDatabase() {
    abstract fun comicDao(): ComicDao
}

