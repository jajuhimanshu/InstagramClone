package com.example.instagramclone.ui.auth

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.instagramclone.R
import com.google.firebase.auth.FirebaseAuth

class WelcomeActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)

        auth = FirebaseAuth.getInstance()

        val welcomeMessage = findViewById<TextView>(R.id.tvWelcomeMessage)
        val userEmail = findViewById<TextView>(R.id.tvUserEmail)
        val logoutButton = findViewById<Button>(R.id.btnLogout)

        val currentUser = auth.currentUser

        if (currentUser == null) {
            finish()
            return
        }

        welcomeMessage.text = "Welcome to Instagram!"
        userEmail.text = currentUser.email ?: ""

        logoutButton.setOnClickListener {
            auth.signOut()
            finish()
        }
    }
}
