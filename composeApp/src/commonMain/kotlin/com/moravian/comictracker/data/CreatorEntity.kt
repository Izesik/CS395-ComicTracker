package com.moravian.comictracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class CreatorEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long,
    val name: String,
)
