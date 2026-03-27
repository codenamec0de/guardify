package com.uow.guardify

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.uow.guardify.model.AppInfo
import com.uow.guardify.util.AppScanner
import com.uow.guardify.util.PreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ScanActivity : AppCompatActivity() {

    private lateinit var btnBack: ImageButton
    private lateinit var progressCircle: ProgressBar
    private lateinit var tvPercentage: TextView
    private lateinit var tvScanStatus: TextView
    private lateinit var ivStep1Icon: ImageView
    private lateinit var ivStep2Icon: ImageView
    private lateinit var ivStep3Icon: ImageView
    private lateinit var tvStep1: TextView
    private lateinit var tvStep2: TextView
    private lateinit var tvStep3: TextView
    private lateinit var btnViewResults: Button

    private val handler = Handler(Looper.getMainLooper())
    private var scannedApps: List<AppInfo> = emptyList()

    companion object {
        const val SCAN_DURATION = 3000L // 3 seconds total
        const val STEP_DURATION = 1000L // 1 second per step
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan)

        initViews()
        setupListeners()
        startScan()
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        progressCircle = findViewById(R.id.progressCircle)
        tvPercentage = findViewById(R.id.tvPercentage)
        tvScanStatus = findViewById(R.id.tvScanStatus)
        ivStep1Icon = findViewById(R.id.ivStep1Icon)
        ivStep2Icon = findViewById(R.id.ivStep2Icon)
        ivStep3Icon = findViewById(R.id.ivStep3Icon)
        tvStep1 = findViewById(R.id.tvStep1)
        tvStep2 = findViewById(R.id.tvStep2)
        tvStep3 = findViewById(R.id.tvStep3)
        btnViewResults = findViewById(R.id.btnViewResults)
    }

    private fun setupListeners() {
        btnBack.setOnClickListener { 
            finish()
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }

        btnViewResults.setOnClickListener {
            // Return to main with results
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("SHOW_AUDIT", true)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            finish()
        }
    }

    private fun startScan() {
        // Start actual scanning in background
        lifecycleScope.launch {
            scannedApps = withContext(Dispatchers.IO) {
                AppScanner.scanInstalledApps(this@ScanActivity)
            }
        }

        // Animate progress over 3 seconds
        animateProgress()

        // Step 1: Scanning Apps (0-33%)
        handler.postDelayed({
            completeStep(1)
            tvScanStatus.text = getString(R.string.status_analyzing)
        }, STEP_DURATION)

        // Step 2: Analyzing Permissions (33-66%)
        handler.postDelayed({
            completeStep(2)
            tvScanStatus.text = getString(R.string.status_completing)
        }, STEP_DURATION * 2)

        // Step 3: Complete (66-100%)
        handler.postDelayed({
            completeStep(3)
            scanComplete()
        }, STEP_DURATION * 3)
    }

    private fun animateProgress() {
        // Animate percentage text
        val animator = ValueAnimator.ofInt(0, 100)
        animator.duration = SCAN_DURATION
        animator.interpolator = AccelerateDecelerateInterpolator()
        animator.addUpdateListener { animation ->
            val value = animation.animatedValue as Int
            tvPercentage.text = "$value%"
            progressCircle.progress = value
        }
        animator.start()
    }

    private fun completeStep(step: Int) {
        val greenColor = ContextCompat.getColor(this, R.color.risk_low)
        val whiteColor = ContextCompat.getColor(this, R.color.white)

        when (step) {
            1 -> {
                ivStep1Icon.setImageResource(R.drawable.ic_circle_complete)
                ivStep1Icon.setColorFilter(greenColor)
                tvStep1.setTextColor(whiteColor)
            }
            2 -> {
                ivStep2Icon.setImageResource(R.drawable.ic_circle_complete)
                ivStep2Icon.setColorFilter(greenColor)
                tvStep2.setTextColor(whiteColor)
            }
            3 -> {
                ivStep3Icon.setImageResource(R.drawable.ic_circle_complete)
                ivStep3Icon.setColorFilter(greenColor)
                tvStep3.setTextColor(whiteColor)
            }
        }
    }

    private fun scanComplete() {
        tvScanStatus.text = getString(R.string.status_completed)
        tvScanStatus.setTextColor(ContextCompat.getColor(this, R.color.risk_low))
        
        // Save scan time
        PreferencesManager.setLastScanTime(this, System.currentTimeMillis())
        
        // Show results button with animation
        btnViewResults.visibility = View.VISIBLE
        btnViewResults.alpha = 0f
        btnViewResults.animate()
            .alpha(1f)
            .setDuration(300)
            .start()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }
}
