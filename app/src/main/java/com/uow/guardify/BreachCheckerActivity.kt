package com.uow.guardify

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.uow.guardify.util.BreachChecker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat

class BreachCheckerActivity : AppCompatActivity() {

    private lateinit var btnBack: ImageButton
    private lateinit var etEmail: TextInputEditText
    private lateinit var etApiKey: TextInputEditText
    private lateinit var btnCheck: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var tvStatus: TextView
    private lateinit var cardSummary: CardView
    private lateinit var tvBreachCount: TextView
    private lateinit var tvBreachLabel: TextView
    private lateinit var breachList: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_breach_checker)

        initViews()
        setupListeners()
        prefillEmail()
        loadCachedResults()
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        etEmail = findViewById(R.id.etEmail)
        etApiKey = findViewById(R.id.etApiKey)
        btnCheck = findViewById(R.id.btnCheck)
        progressBar = findViewById(R.id.progressBar)
        tvStatus = findViewById(R.id.tvStatus)
        cardSummary = findViewById(R.id.cardSummary)
        tvBreachCount = findViewById(R.id.tvBreachCount)
        tvBreachLabel = findViewById(R.id.tvBreachLabel)
        breachList = findViewById(R.id.breachList)
    }

    private fun setupListeners() {
        btnBack.setOnClickListener {
            finish()
            @Suppress("DEPRECATION")
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }

        btnCheck.setOnClickListener {
            performCheck()
        }
    }

    private fun prefillEmail() {
        FirebaseAuth.getInstance().currentUser?.email?.let {
            etEmail.setText(it)
        }
        // Load saved API key
        val prefs = getSharedPreferences("guardify_prefs", MODE_PRIVATE)
        prefs.getString("hibp_api_key", null)?.let {
            etApiKey.setText(it)
        }
    }

    private fun loadCachedResults() {
        val email = FirebaseAuth.getInstance().currentUser?.email ?: return

        lifecycleScope.launch {
            val cached = withContext(Dispatchers.IO) {
                BreachChecker.getCached(this@BreachCheckerActivity, email)
            }
            if (cached != null) {
                showResults(cached)
            }
        }
    }

    private fun performCheck() {
        val email = etEmail.text?.toString()?.trim()
        val apiKey = etApiKey.text?.toString()?.trim()

        if (email.isNullOrBlank()) {
            etEmail.error = "Enter an email address"
            return
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.error = "Enter a valid email"
            return
        }
        if (apiKey.isNullOrBlank()) {
            etApiKey.error = "HIBP API key is required"
            return
        }

        // Save API key for next time
        getSharedPreferences("guardify_prefs", MODE_PRIVATE)
            .edit().putString("hibp_api_key", apiKey).apply()

        btnCheck.isEnabled = false
        progressBar.visibility = View.VISIBLE
        tvStatus.visibility = View.VISIBLE
        tvStatus.text = "Checking breaches..."
        tvStatus.setTextColor(ContextCompat.getColor(this, R.color.text_secondary_dark))
        cardSummary.visibility = View.GONE
        breachList.removeAllViews()

        lifecycleScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    BreachChecker.check(this@BreachCheckerActivity, email, apiKey)
                }
                progressBar.visibility = View.GONE
                tvStatus.visibility = View.GONE
                showResults(result)
            } catch (e: BreachChecker.BreachCheckException) {
                progressBar.visibility = View.GONE
                tvStatus.visibility = View.VISIBLE
                tvStatus.text = "Error: ${e.message}"
                tvStatus.setTextColor(ContextCompat.getColor(this@BreachCheckerActivity, R.color.risk_high))
            } catch (e: Exception) {
                progressBar.visibility = View.GONE
                tvStatus.visibility = View.VISIBLE
                tvStatus.text = "Network error. Check your connection and API key."
                tvStatus.setTextColor(ContextCompat.getColor(this@BreachCheckerActivity, R.color.risk_high))
            } finally {
                btnCheck.isEnabled = true
            }
        }
    }

    private fun showResults(result: BreachChecker.BreachResult) {
        cardSummary.visibility = View.VISIBLE
        tvBreachCount.text = result.breachCount.toString()
        breachList.removeAllViews()

        if (result.isClean) {
            tvBreachCount.setTextColor(ContextCompat.getColor(this, R.color.risk_low))
            tvBreachLabel.text = "No breaches found — your email is clean!"
            tvBreachLabel.setTextColor(ContextCompat.getColor(this, R.color.risk_low))
            return
        }

        val countColor = when (result.severity) {
            "HIGH" -> R.color.risk_high
            "MEDIUM" -> R.color.risk_medium
            else -> R.color.risk_low
        }
        tvBreachCount.setTextColor(ContextCompat.getColor(this, countColor))
        tvBreachLabel.text = "breaches found for ${result.email}"
        tvBreachLabel.setTextColor(ContextCompat.getColor(this, R.color.text_secondary_dark))

        // Populate breach cards
        for (breach in result.breaches) {
            val itemView = layoutInflater.inflate(R.layout.item_breach, breachList, false)

            val tvName = itemView.findViewById<TextView>(R.id.tvBreachName)
            val tvSeverity = itemView.findViewById<TextView>(R.id.tvSeverity)
            val tvDate = itemView.findViewById<TextView>(R.id.tvBreachDate)
            val tvDesc = itemView.findViewById<TextView>(R.id.tvDescription)
            val tvData = itemView.findViewById<TextView>(R.id.tvDataExposed)
            val tvPwn = itemView.findViewById<TextView>(R.id.tvPwnCount)

            tvName.text = breach.title.ifBlank { breach.name }
            tvDate.text = "Breached: ${breach.breachDate}"
            tvDesc.text = breach.description
            tvData.text = "Exposed: ${breach.dataExposed.joinToString(", ")}"

            if (breach.pwnCount > 0) {
                tvPwn.text = "${NumberFormat.getNumberInstance().format(breach.pwnCount)} accounts affected"
                tvPwn.visibility = View.VISIBLE
            } else {
                tvPwn.visibility = View.GONE
            }

            // Severity badge
            when (breach.severity) {
                "HIGH" -> {
                    tvSeverity.text = "HIGH"
                    tvSeverity.setTextColor(ContextCompat.getColor(this, R.color.risk_high))
                    tvSeverity.setBackgroundResource(R.drawable.bg_badge_high)
                }
                "MEDIUM" -> {
                    tvSeverity.text = "MEDIUM"
                    tvSeverity.setTextColor(ContextCompat.getColor(this, R.color.risk_medium))
                    tvSeverity.setBackgroundResource(R.drawable.bg_badge_medium)
                }
                else -> {
                    tvSeverity.text = "LOW"
                    tvSeverity.setTextColor(ContextCompat.getColor(this, R.color.risk_low))
                    tvSeverity.setBackgroundResource(R.drawable.bg_badge_low)
                }
            }

            breachList.addView(itemView)
        }
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }
}
