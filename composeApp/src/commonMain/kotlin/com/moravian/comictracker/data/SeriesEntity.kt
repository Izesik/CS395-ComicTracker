package com.moravian.comictracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a comic series saved to the user's local collection.
 *
 * @property id Auto-generated primary key.
 * @property comicvineId Numeric ID from the ComicVine API used to correlate with remote data.
 * @property title Display name of the series.
 * @property publisher Publisher name, if available.
 * @property coverImageUrl URL of the series cover image for display.
 */
@Entity
data class SeriesEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val comicvineId: Int,
    val title: String,
    val publisher: String? = null,
    val coverImageUrl: String? = null
)
