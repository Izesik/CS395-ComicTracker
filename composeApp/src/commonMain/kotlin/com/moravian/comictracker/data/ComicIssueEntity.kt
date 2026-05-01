package com.moravian.comictracker.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    foreignKeys = [ForeignKey(
        entity = SeriesEntity::class,
        parentColumns = ["id"],
        childColumns = ["seriesId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("seriesId")]
)
data class ComicIssueEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val comicvineId: Int,
    val seriesId: Long,
    val issueNumber: Int,
    val title: String,
    val readStatus: ReadStatus = ReadStatus.TO_READ,
    val coverImageUrl: String? = null
)
