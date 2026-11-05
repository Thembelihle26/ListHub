package com.Group19.listhub

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.Group19.listhub.data.FirestoreRepository
import com.Group19.listhub.utils.GoogleAuthHelper
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private val repository = FirestoreRepository()

    // modern replacement for the old startActivityForResult() pattern
    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val idToken = account?.idToken
            if (idToken == null) {
                Toast.makeText(this, "Google sign-in failed: no ID token returned", Toast.LENGTH_SHORT).show()
                return@registerForActivityResult
            }
            lifecycleScope.launch {
                try {
                    repository.signInWithGoogleCredential(idToken)
                    Toast.makeText(this@LoginActivity, "Signed in with Google", Toast.LENGTH_SHORT).show()
                    goToDashboard()
                } catch (e: Exception) {
                    Toast.makeText(this@LoginActivity, "Firebase sign-in failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        } catch (e: ApiException) {
            // status code 10 (DEVELOPER_ERROR) almost always means the SHA-1 fingerprint
            // hasn't been registered in the Firebase console yet - see the README
            Toast.makeText(this, "Google sign-in failed (code ${e.statusCode}). See README for SHA-1 setup.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        if (repository.isLoggedIn()) {
            goToDashboard()
            return
        }

        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)

        val btnLogin: Button = findViewById(R.id.btnLogin)
        val btnGoogle: Button = findViewById(R.id.btnGoogleSignIn)
        val tvGoToRegister: TextView = findViewById(R.id.tvGoToRegister)

        btnLogin.setOnClickListener { attemptLogin() }

        tvGoToRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        btnGoogle.setOnClickListener {
            val signInClient = GoogleAuthHelper.getSignInClient(this)
            googleSignInLauncher.launch(signInClient.signInIntent)
        }
    }

    private fun attemptLogin() {
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill in both fields", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                val user = repository.login(email, password)
                Toast.makeText(this@LoginActivity, "Welcome back, ${user.displayName ?: user.email}!", Toast.LENGTH_SHORT).show()
                goToDashboard()
            } catch (e: Exception) {
                // Firebase throws things like FirebaseAuthInvalidCredentialsException /
                // FirebaseAuthInvalidUserException - we just surface the message for the prototype
                Toast.makeText(this@LoginActivity, "Login failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun goToDashboard() {
        val intent = Intent(this, DashboardActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
