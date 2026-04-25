package com.moravian.comictracker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ComicDao {
    // Insert a new series into the database
    @Insert
    suspend fun insertSeries(series: SeriesEntity)

    // Insert a new comic issue into the database
    @Insert
    suspend fun insertComicIssue(issue: ComicIssueEntity): Long

    // Get all series from the database
    @Query("SELECT * FROM SeriesEntity")
    fun getAllSeries(): Flow<List<SeriesEntity>>

    // Get all comic issues for a specific series
    @Query("SELECT * FROM ComicIssueEntity WHERE seriesId = :seriesId")
    fun getComicIssuesForSeries(seriesId: Long): Flow<List<ComicIssueEntity>>

    // Update the read status of a comic issue
    @Query("UPDATE ComicIssueEntity SET isRead = :isRead WHERE id = :issueId")
    suspend fun updateComicIssueReadStatus(issueId: Long, isRead: Boolean)

    // Delete a comic issue from the database
    @Query("DELETE FROM ComicIssueEntity WHERE id = :issueId")
    suspend fun deleteComicIssue(issueId: Long)

    // Delete a series and all its associated comic issues from the database
    @Query("DELETE FROM SeriesEntity WHERE id = :seriesId")
    suspend fun deleteSeries(seriesId: Long)
}