package com.uow.guardify.ui.settings

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.uow.guardify.LoginActivity
import com.uow.guardify.R
import com.uow.guardify.util.PreferencesManager

class SettingsFragment : Fragment() {

    private lateinit var tvUserEmail: TextView
    private lateinit var btnAbout: LinearLayout
    private lateinit var btnPrivacy: LinearLayout
    private lateinit var btnLogout: Button

    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        auth = FirebaseAuth.getInstance()
        initViews(view)
        setupListeners()
        displayUserInfo()
    }

    private fun initViews(view: View) {
        tvUserEmail = view.findViewById(R.id.tvUserEmail)
        btnAbout = view.findViewById(R.id.btnAbout)
        btnPrivacy = view.findViewById(R.id.btnPrivacy)
        btnLogout = view.findViewById(R.id.btnLogout)
    }

    private fun setupListeners() {
        btnAbout.setOnClickListener {
            // Show about dialog or screen
        }

        btnPrivacy.setOnClickListener {
            // Open privacy policy
        }

        btnLogout.setOnClickListener {
            logout()
        }
    }

    private fun displayUserInfo() {
        auth.currentUser?.let { user ->
            tvUserEmail.text = user.email ?: user.displayName ?: "User"
        } ?: run {
            tvUserEmail.text = "Not signed in"
        }
    }

    private fun logout() {
        // Sign out from Firebase
        auth.signOut()

        // Sign out from Google
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        GoogleSignIn.getClient(requireContext(), gso).signOut()

        // Clear preferences
        PreferencesManager.clearAll(requireContext())

        // Navigate to login
        val intent = Intent(requireContext(), LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        requireActivity().finish()
    }
}
