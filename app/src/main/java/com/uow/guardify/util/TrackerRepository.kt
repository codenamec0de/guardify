package com.uow.guardify.util

import com.uow.guardify.api.RetrofitClient
import com.uow.guardify.model.TrackerDetail
import com.uow.guardify.model.TrackerInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object TrackerRepository {
    
    private var cachedTrackers: Map<Int, TrackerDetail>? = null
    
    suspend fun getTrackersForApp(packageName: String): Result<List<TrackerInfo>> {
        return withContext(Dispatchers.IO) {
            try {
                // First, get all trackers if not cached
                if (cachedTrackers == null) {
                    val trackersResponse = RetrofitClient.exodusApi.getAllTrackers()
                    if (trackersResponse.isSuccessful) {
                        cachedTrackers = trackersResponse.body()?.trackers?.mapKeys { 
                            it.key.toIntOrNull() ?: 0 
                        } ?: emptyMap()
                    }
                }
                
                // Get reports for the app
                val response = RetrofitClient.exodusApi.getAppReports(packageName)
                
                if (response.isSuccessful) {
                    val reports = response.body()?.reports
                    if (reports.isNullOrEmpty()) {
                        return@withContext Result.success(emptyList())
                    }
                    
                    // Get the most recent report
                    val latestReport = reports.maxByOrNull { it.creationDate ?: "" }
                    val trackerIds = latestReport?.trackers ?: emptyList()
                    
                    // Map tracker IDs to tracker info
                    val trackers = trackerIds.mapNotNull { trackerId ->
                        cachedTrackers?.get(trackerId)?.let { detail ->
                            TrackerInfo(
                                id = detail.id ?: trackerId,
                                name = detail.name ?: "Unknown Tracker",
                                description = detail.description,
                                creationDate = detail.creationDate,
                                codeSignature = detail.codeSignature,
                                networkSignature = detail.networkSignature,
                                website = detail.website,
                                categories = detail.categories
                            )
                        }
                    }
                    
                    Result.success(trackers)
                } else {
                    Result.failure(Exception("API error: ${response.code()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    suspend fun preloadTrackers(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val response = RetrofitClient.exodusApi.getAllTrackers()
                if (response.isSuccessful) {
                    cachedTrackers = response.body()?.trackers?.mapKeys { 
                        it.key.toIntOrNull() ?: 0 
                    } ?: emptyMap()
                    true
                } else {
                    false
                }
            } catch (e: Exception) {
                false
            }
        }
    }
    
    fun clearCache() {
        cachedTrackers = null
    }
}
