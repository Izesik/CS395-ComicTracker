package com.moravian.comictracker.network

// Orchestrates Metron (primary) and ComicVine (fallback).
// All public methods return ComicVine-compatible types so existing screens need no changes.
// Metron results include cv_id — when present we hand that ID to the existing CV detail screens.
class ComicRepository {
    private val metron = MetronApi()
    private val comicVine = ComicVineApi()

    // Search series: Metron first (mapped to CV search result via cv_id), fall back to ComicVine.
    suspend fun searchSeries(query: String): List<ComicVineSearchResult> {
        return try {
            val metronResults = metron.searchSeries(query).results
            val mapped = metronResults.mapNotNull { it.toComicVineSearchResult() }
            if (mapped.isNotEmpty()) mapped else comicVine.search(query).results
        } catch (_: Exception) {
            comicVine.search(query).results
        }
    }

    // UPC barcode lookup: Metron first (they index UPCs natively), fall back to ComicVine.
    // Returns a ComicVine issue ID on success, or null if not found in either.
    suspend fun lookupByUpc(upc: String): Int? {
        return try {
            val metronIssue = metron.searchByUpc(upc).results.firstOrNull()
            metronIssue?.cvId ?: comicVine.searchByUpc(upc).results.firstOrNull()?.id
        } catch (_: Exception) {
            try {
                comicVine.searchByUpc(upc).results.firstOrNull()?.id
            } catch (_: Exception) {
                null
            }
        }
    }

    // Home screen: Metron ongoing series → ComicVineVolume (using cv_id for navigation)
    suspend fun getHomeVolumes(): List<ComicVineVolume> {
        return try {
            val mapped = metron.getPopularSeries().results.mapNotNull { it.toComicVineVolume() }
            if (mapped.size >= 8) mapped else comicVine.getPopularVolumes().results
        } catch (_: Exception) {
            comicVine.getPopularVolumes().results
        }
    }

    // Home screen: Metron recent issues → ComicVineIssue (using cv_id for navigation)
    suspend fun getHomeIssues(): List<ComicVineIssue> {
        return try {
            val mapped = metron.getRecentIssues().results.mapNotNull { it.toComicVineIssue() }
            if (mapped.size >= 8) mapped else comicVine.getPopularIssues().results
        } catch (_: Exception) {
            comicVine.getPopularIssues().results
        }
    }

    private fun MetronSeriesSummary.toComicVineVolume(): ComicVineVolume? {
        val cvId = cvId ?: return null
        return ComicVineVolume(
            id = cvId,
            name = name,
            startYear = yearBegan?.toString(),
            image = image?.let { ComicVineImage(mediumUrl = it, smallUrl = it) },
            publisher = publisher?.let { ComicVinePublisherRef(id = it.id, name = it.name) }
        )
    }

    private fun MetronIssueSummary.toComicVineIssue(): ComicVineIssue? {
        val cvId = cvId ?: return null
        return ComicVineIssue(
            id = cvId,
            name = series?.name,
            issueNumber = number.ifBlank { "?" },
            coverDate = coverDate,
            image = image?.let { ComicVineImage(mediumUrl = it, smallUrl = it) },
            volume = series?.let { ComicVineVolumeRef(id = it.id, name = it.name) }
        )
    }

    private fun MetronSeriesSummary.toComicVineSearchResult(): ComicVineSearchResult? {
        val cvId = cvId ?: return null
        return ComicVineSearchResult(
            id = cvId,
            name = name,
            resourceType = "volume",
            image = image?.let { ComicVineImage(mediumUrl = it, smallUrl = it) },
            publisher = publisher?.let { ComicVinePublisherRef(id = it.id, name = it.name) },
            startYear = yearBegan?.toString()
        )
    }
}
