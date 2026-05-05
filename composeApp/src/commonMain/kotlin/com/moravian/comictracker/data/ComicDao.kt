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

    @Query("SELECT * FROM SeriesEntity WHERE comicvineId = :comicvineId LIMIT 1")
    suspend fun getSeriesByComicVineId(comicvineId: Int): SeriesEntity?

    @Query("SELECT * FROM ComicIssueEntity WHERE comicvineId = :comicvineId LIMIT 1")
    suspend fun getIssueByComicVineId(comicvineId: Int): ComicIssueEntity?

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

    @Insert
    suspend fun insertCreator(creator: CreatorEntity): Long

    @Query("SELECT * FROM CreatorEntity WHERE issueId = :issueId")
    fun getCreatorsForIssue(issueId: Long): Flow<List<CreatorEntity>>

    @Query("""
        SELECT ce.* FROM CreatorEntity ce
        INNER JOIN ComicIssueEntity cie ON ce.issueId = cie.id
        WHERE cie.seriesId = :localSeriesId
        GROUP BY ce.comicvineId
    """)
    fun getCreatorsForSeries(localSeriesId: Long): Flow<List<CreatorEntity>>
}
