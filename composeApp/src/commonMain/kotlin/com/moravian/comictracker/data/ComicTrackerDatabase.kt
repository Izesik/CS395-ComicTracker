package com.moravian.comictracker.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [SeriesEntity::class, ComicIssueEntity::class], version = 2)
@TypeConverters(Converters::class)
abstract class ComicTrackerDatabase : RoomDatabase() {
    abstract fun comicDao(): ComicDao
}

