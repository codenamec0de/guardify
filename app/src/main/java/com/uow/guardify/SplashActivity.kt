package com.uow.guardify

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.uow.guardify.util.PreferencesManager

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    private val SPLASH_DELAY = 2000L // 2 seconds

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Animate logo
        val logo = findViewById<ImageView>(R.id.ivLogo)
        val fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in)
        logo.startAnimation(fadeIn)

        // Navigate after delay
        Handler(Looper.getMainLooper()).postDelayed({
            navigateToNextScreen()
        }, SPLASH_DELAY)
    }

    private fun navigateToNextScreen() {
        val auth = FirebaseAuth.getInstance()
        
        val intent = when {
            // Not logged in -> Login
            auth.currentUser == null -> Intent(this, LoginActivity::class.java)
            // Logged in but onboarding not complete -> Onboarding
            !PreferencesManager.isOnboardingComplete(this) -> Intent(this, OnboardingActivity::class.java)
            // Logged in and onboarding complete -> Main
            else -> Intent(this, MainActivity::class.java)
        }
        
        startActivity(intent)
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        finish()
    }
}
