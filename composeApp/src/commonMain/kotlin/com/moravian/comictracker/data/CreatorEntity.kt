package com.moravian.comictracker.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    foreignKeys = [ForeignKey(
        entity = ComicIssueEntity::class,
        parentColumns = ["id"],
        childColumns = ["issueId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("issueId")]
)
data class CreatorEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val issueId: Long,
    val comicvineId: Int,
    val name: String,
    val role: String? = null,
)