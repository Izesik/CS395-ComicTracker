package com.moravian.comictracker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** Data access object for all local collection operations. */
@Dao
interface ComicDao {

    /** Inserts a series and returns its generated row ID. */
    @Insert
    suspend fun insertSeries(series: SeriesEntity): Long

    /** Inserts an issue and returns its generated row ID. */
    @Insert
    suspend fun insertComicIssue(issue: ComicIssueEntity): Long

    /** Inserts a creator credit and returns its generated row ID. */
    @Insert
    suspend fun insertCreator(creator: CreatorEntity): Long

    /** Emits the full list of saved series, updating reactively on any change. */
    @Query("SELECT * FROM SeriesEntity")
    fun getAllSeries(): Flow<List<SeriesEntity>>

    /** Returns the saved series matching [comicvineId], or null if not in the collection. */
    @Query("SELECT * FROM SeriesEntity WHERE comicvineId = :comicvineId LIMIT 1")
    suspend fun getSeriesByComicVineId(comicvineId: Int): SeriesEntity?

    /** Returns the saved issue matching [comicvineId], or null if not in the collection. */
    @Query("SELECT * FROM ComicIssueEntity WHERE comicvineId = :comicvineId LIMIT 1")
    suspend fun getIssueByComicVineId(comicvineId: Int): ComicIssueEntity?

    /** Emits all issues belonging to the series with [seriesId], updating reactively. */
    @Query("SELECT * FROM ComicIssueEntity WHERE seriesId = :seriesId")
    fun getComicIssuesForSeries(seriesId: Long): Flow<List<ComicIssueEntity>>

    /** Emits all creator credits across every issue in [localSeriesId], de-duplicated by ComicVine ID. */
    @Query("""
        SELECT ce.* FROM CreatorEntity ce
        INNER JOIN ComicIssueEntity cie ON ce.issueId = cie.id
        WHERE cie.seriesId = :localSeriesId
        GROUP BY ce.comicvineId
    """)
    fun getCreatorsForSeries(localSeriesId: Long): Flow<List<CreatorEntity>>

    /** Emits all creator credits for the issue with [issueId], updating reactively. */
    @Query("SELECT * FROM CreatorEntity WHERE issueId = :issueId")
    fun getCreatorsForIssue(issueId: Long): Flow<List<CreatorEntity>>

    /** Updates the [ReadStatus] for the issue with [issueId]. */
    @Query("UPDATE ComicIssueEntity SET readStatus = :status WHERE id = :issueId")
    suspend fun updateReadStatus(issueId: Long, status: ReadStatus)

    /** Permanently deletes the issue with [issueId] (cascades to its creator credits). */
    @Query("DELETE FROM ComicIssueEntity WHERE id = :issueId")
    suspend fun deleteComicIssue(issueId: Long)

    /** Permanently deletes the series with [seriesId] (cascades to its issues and creators). */
    @Query("DELETE FROM SeriesEntity WHERE id = :seriesId")
    suspend fun deleteSeries(seriesId: Long)

    /** Emits saved series whose titles contain [query], updating reactively. */
    @Query("SELECT * FROM SeriesEntity WHERE title LIKE '%' || :query || '%'")
    fun searchSeries(query: String): Flow<List<SeriesEntity>>
}
