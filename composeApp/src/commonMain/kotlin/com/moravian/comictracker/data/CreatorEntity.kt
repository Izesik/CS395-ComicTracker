package com.moravian.comictracker.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Represents a creator credit attached to a comic issue in the user's collection.
 *
 * Cascade-deletes when the parent [ComicIssueEntity] is removed.
 *
 * @property id Auto-generated primary key.
 * @property issueId Foreign key referencing the owning [ComicIssueEntity].
 * @property comicvineId Numeric ID from the ComicVine API, used to de-duplicate creators across issues.
 * @property name Display name of the creator.
 * @property role The creator's role on this issue (e.g. "Writer", "Penciler").
 */
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
