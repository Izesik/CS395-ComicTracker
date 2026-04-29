package com.moravian.comictracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class SeriesEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val comicvineId: Int,
    val title: String,
    val publisher: String? = null,
    val coverImageUrl: String? = null
)