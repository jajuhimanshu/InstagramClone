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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()

        val email = findViewById<EditText>(R.id.etRegisterEmail)
        val password = findViewById<EditText>(R.id.etRegisterPassword)
        val confirmPassword = findViewById<EditText>(R.id.etConfirmPassword)
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

                    registerButton.isEnabled = true

                    if (task.isSuccessful) {

                        Toast.makeText(
                            this,
                            "Registration successful",
                            Toast.LENGTH_SHORT
                        ).show()

                        finish()

                    } else {

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
