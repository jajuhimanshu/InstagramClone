
package com.example.instagramclone

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestoreRepository: FirestoreRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()
        firestoreRepository = FirestoreRepository()

        val email = findViewById<EditText>(R.id.etRegisterEmail)
        val password = findViewById<EditText>(R.id.etRegisterPassword)
        val confirmPassword =
            findViewById<EditText>(R.id.etConfirmPassword)

        val registerButton = findViewById<Button>(R.id.btnRegister)
        val backToLogin = findViewById<TextView>(R.id.tvBackToLogin)

        registerButton.setOnClickListener {

            val emailText = email.text.toString().trim()
            val passwordText = password.text.toString()
            val confirmPasswordText = confirmPassword.text.toString()

            if (emailText.isEmpty() ||
                passwordText.isEmpty() ||
                confirmPasswordText.isEmpty()
            ) {
                Toast.makeText(
                    this,
                    "Please fill in all fields",
                    Toast.LENGTH_SHORT
                ).show()

            } else if (passwordText != confirmPasswordText) {
                Toast.makeText(
                    this,
                    "Passwords do not match",
                    Toast.LENGTH_SHORT
                ).show()

            } else if (passwordText.length < 6) {
                Toast.makeText(
                    this,
                    "Password must contain at least 6 characters",
                    Toast.LENGTH_SHORT
                ).show()

            } else {

                registerButton.isEnabled = false

                auth.createUserWithEmailAndPassword(
                    emailText,
                    passwordText
                ).addOnCompleteListener { task ->

                    if (task.isSuccessful) {

                        val firebaseUser = auth.currentUser

                        if (firebaseUser == null) {
                            registerButton.isEnabled = true

                            Toast.makeText(
                                this,
                                "Account created, but user details are unavailable",
                                Toast.LENGTH_LONG
                            ).show()

                            return@addOnCompleteListener
                        }

                        val username = emailText
                            .substringBefore("@")
                            .lowercase()

                        val user = User(
                            uid = firebaseUser.uid,
                            username = username,
                            usernameLowercase = username,
                            displayName = username,
                            bio = "",
                            profileImageUrl = "",
                            createdAt = System.currentTimeMillis()
                        )

                        firestoreRepository.saveUserProfile(
                            user = user,

                            onSuccess = {
                                Toast.makeText(
                                    this,
                                    "Registration successful",
                                    Toast.LENGTH_SHORT
                                ).show()

                                finish()
                            },

                            onFailure = { exception ->
                                registerButton.isEnabled = true

                                Toast.makeText(
                                    this,
                                    "Account created, but profile saving failed: " +
                                            (exception.localizedMessage
                                                ?: "Unknown error"),
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        )

                    } else {

                        registerButton.isEnabled = true

                        Toast.makeText(
                            this,
                            task.exception?.localizedMessage
                                ?: "Registration failed",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }

        backToLogin.setOnClickListener {
            finish()
        }
    }
}
