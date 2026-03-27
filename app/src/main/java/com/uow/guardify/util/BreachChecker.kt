package com.uow.guardify.util

import android.content.Context
import com.uow.guardify.api.HIBPApiService
import com.uow.guardify.api.HIBPBreach
import com.uow.guardify.data.GuardifyDatabase
import com.uow.guardify.data.entity.BreachResultEntity
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Checks emails against the Have I Been Pwned API and caches results in Room.
 */
object BreachChecker {

    data class BreachResult(
        val email: String,
        val breaches: List<BreachInfo>,
        val checkedAt: Long
    ) {
        val breachCount: Int get() = breaches.size
        val isClean: Boolean get() = breaches.isEmpty()

        val severity: String
            get() = when {
                breaches.any { it.severity == "HIGH" } -> "HIGH"
                breaches.any { it.severity == "MEDIUM" } -> "MEDIUM"
                breaches.isEmpty() -> "NONE"
                else -> "LOW"
            }
    }

    data class BreachInfo(
        val name: String,
        val title: String,
        val domain: String,
        val breachDate: String,
        val description: String,
        val dataExposed: List<String>,
        val pwnCount: Long,
        val severity: String
    )

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val api: HIBPApiService = Retrofit.Builder()
        .baseUrl(HIBPApiService.BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(HIBPApiService::class.java)

    /**
     * Check an email against HIBP and cache results.
     * Requires a valid HIBP API key.
     */
    suspend fun check(context: Context, email: String, apiKey: String): BreachResult {
        val response = api.getBreaches(email.trim(), apiKey)

        val breaches = when (response.code()) {
            200 -> response.body()?.map { mapBreach(it) } ?: emptyList()
            404 -> emptyList() // No breaches found — clean!
            else -> throw BreachCheckException(
                "HIBP returned ${response.code()}: ${response.message()}"
            )
        }

        val now = System.currentTimeMillis()
        val result = BreachResult(email = email.trim(), breaches = breaches, checkedAt = now)

        // Cache to Room
        val db = GuardifyDatabase.getInstance(context)
        db.breachResultDao().deleteByEmail(email.trim())
        if (breaches.isNotEmpty()) {
            db.breachResultDao().insertAll(breaches.map { breach ->
                BreachResultEntity(
                    email = email.trim(),
                    breachName = breach.name,
                    breachDate = breach.breachDate,
                    dataExposed = breach.dataExposed.joinToString(", "),
                    description = breach.title,
                    severity = breach.severity,
                    checkedAt = now
                )
            })
        }

        return result
    }

    /**
     * Load cached results from Room (no network call).
     */
    suspend fun getCached(context: Context, email: String): BreachResult? {
        val db = GuardifyDatabase.getInstance(context)
        val entities = db.breachResultDao().getByEmail(email.trim())
        if (entities.isEmpty()) return null

        return BreachResult(
            email = email.trim(),
            breaches = entities.map { entity ->
                BreachInfo(
                    name = entity.breachName,
                    title = entity.description,
                    domain = "",
                    breachDate = entity.breachDate,
                    description = entity.description,
                    dataExposed = entity.dataExposed.split(", ").filter { it.isNotBlank() },
                    pwnCount = 0,
                    severity = entity.severity
                )
            },
            checkedAt = entities.first().checkedAt
        )
    }

    private fun mapBreach(hibp: HIBPBreach): BreachInfo {
        val sensitiveData = listOf(
            "Passwords", "Credit cards", "Bank account numbers",
            "Social security numbers", "Phone numbers"
        )
        val hasSensitive = hibp.DataClasses.any { it in sensitiveData }
        val severity = when {
            hasSensitive && hibp.PwnCount > 1_000_000 -> "HIGH"
            hasSensitive || hibp.PwnCount > 500_000 -> "MEDIUM"
            else -> "LOW"
        }

        return BreachInfo(
            name = hibp.Name,
            title = hibp.Title,
            domain = hibp.Domain,
            breachDate = hibp.BreachDate,
            description = cleanHtml(hibp.Description),
            dataExposed = hibp.DataClasses,
            pwnCount = hibp.PwnCount,
            severity = severity
        )
    }

    private fun cleanHtml(html: String): String {
        return html.replace(Regex("<[^>]*>"), "").trim()
    }

    class BreachCheckException(message: String) : Exception(message)
}
