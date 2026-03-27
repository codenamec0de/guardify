package com.uow.guardify.api

import com.uow.guardify.model.ExodusReportResponse
import com.uow.guardify.model.ExodusTrackerResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface ExodusApiService {
    
    @GET("api/search/{package_name}/details")
    suspend fun getAppReports(
        @Path("package_name") packageName: String
    ): Response<ExodusReportResponse>
    
    @GET("api/trackers")
    suspend fun getAllTrackers(): Response<ExodusTrackerResponse>
    
    companion object {
        const val BASE_URL = "https://reports.exodus-privacy.eu.org/"
    }
}
