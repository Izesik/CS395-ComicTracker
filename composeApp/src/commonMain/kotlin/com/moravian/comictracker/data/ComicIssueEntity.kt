package com.moravian.comictracker.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Represents a single comic issue saved to the user's local collection.
 *
 * Cascade-deletes when the parent [SeriesEntity] is removed.
 *
 * @property id Auto-generated primary key.
 * @property comicvineId Numeric ID from the ComicVine API.
 * @property seriesId Foreign key referencing the owning [SeriesEntity].
 * @property issueNumber Issue number as an integer for sorting.
 * @property title Display title (typically "#N").
 * @property readStatus Current read progress tracked by the user.
 * @property coverImageUrl URL of the issue cover image for display.
 */
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
