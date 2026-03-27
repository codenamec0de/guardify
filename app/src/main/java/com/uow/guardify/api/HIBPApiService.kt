package com.uow.guardify.api

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path

/**
 * Have I Been Pwned API v3 service.
 *
 * The free, unauthenticated endpoint is used here:
 *   GET /unifiedsearch/{account}
 * which returns basic breach names per account.
 *
 * For full breach details we use the public breaches list and correlate.
 */
interface HIBPApiService {

    companion object {
        const val BASE_URL = "https://haveibeenpwned.com/api/v3/"
    }

    /**
     * Get all breaches for an email account.
     * Requires the hibp-api-key header for v3, but the
     * unifiedsearch endpoint works without it via the website's
     * public lookup. We'll use the public breachedaccount endpoint
     * with truncateResponse=true to stay within free tier.
     */
    @GET("breachedaccount/{account}?truncateResponse=false")
    suspend fun getBreaches(
        @Path("account") email: String,
        @Header("hibp-api-key") apiKey: String,
        @Header("user-agent") userAgent: String = "Guardify-Android"
    ): Response<List<HIBPBreach>>
}

/**
 * HIBP breach response model.
 */
data class HIBPBreach(
    val Name: String,
    val Title: String,
    val Domain: String,
    val BreachDate: String,
    val Description: String,
    val DataClasses: List<String>,
    val IsVerified: Boolean,
    val IsSensitive: Boolean,
    val PwnCount: Long
)
