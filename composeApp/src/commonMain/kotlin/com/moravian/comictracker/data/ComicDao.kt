package com.moravian.comictracker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ComicDao {
    @Insert
    suspend fun insertSeries(series: SeriesEntity): Long

    @Insert
    suspend fun insertComicIssue(issue: ComicIssueEntity): Long

    @Query("SELECT * FROM SeriesEntity")
    fun getAllSeries(): Flow<List<SeriesEntity>>

    @Query("SELECT * FROM SeriesEntity WHERE metronId = :metronId LIMIT 1")
    suspend fun getSeriesByMetronId(metronId: Int): SeriesEntity?

    @Query("SELECT * FROM ComicIssueEntity WHERE metronId = :metronId LIMIT 1")
    suspend fun getIssueByMetronId(metronId: Int): ComicIssueEntity?

    @Query("SELECT * FROM ComicIssueEntity WHERE seriesId = :seriesId")
    fun getComicIssuesForSeries(seriesId: Long): Flow<List<ComicIssueEntity>>

    @Query("UPDATE ComicIssueEntity SET readStatus = :status WHERE id = :issueId")
    suspend fun updateReadStatus(issueId: Long, status: ReadStatus)

    @Query("DELETE FROM ComicIssueEntity WHERE id = :issueId")
    suspend fun deleteComicIssue(issueId: Long)

    @Query("DELETE FROM SeriesEntity WHERE id = :seriesId")
    suspend fun deleteSeries(seriesId: Long)

    @Query("SELECT * FROM SeriesEntity WHERE title LIKE '%' || :query || '%'")
    fun searchSeries(query: String): Flow<List<SeriesEntity>>
}
