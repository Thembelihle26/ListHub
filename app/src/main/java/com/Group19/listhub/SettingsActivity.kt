package com.Group19.listhub

import android.content.Intent
import android.os.Bundle
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.lifecycleScope
import com.Group19.listhub.data.FirestoreRepository
import kotlinx.coroutines.launch

class SettingsActivity : AppCompatActivity() {

    private val repository = FirestoreRepository()

    private lateinit var switchDarkTheme: Switch
    private lateinit var switchNotifications: Switch
    private lateinit var spinnerLanguage: Spinner
    private lateinit var tvProfileName: TextView
    private lateinit var tvProfileEmail: TextView
    private lateinit var tvAvatarInitial: TextView

    private var isProgrammaticSelection = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val btnBack: ImageButton = findViewById(R.id.btnBack)
        val rowDarkTheme: LinearLayout = findViewById(R.id.rowDarkTheme)
        val rowNotifications: LinearLayout = findViewById(R.id.rowNotifications)
        switchDarkTheme = findViewById(R.id.switchDarkTheme)
        switchNotifications = findViewById(R.id.switchNotifications)
        spinnerLanguage = findViewById(R.id.spinnerLanguage)
        tvProfileName = findViewById(R.id.tvProfileName)
        tvProfileEmail = findViewById(R.id.tvProfileEmail)
        tvAvatarInitial = findViewById(R.id.tvAvatarInitial)
        val btnLogout: Button = findViewById(R.id.btnLogout)

        btnBack.setOnClickListener { finish() }
        rowDarkTheme.setOnClickListener { switchDarkTheme.toggle() }
        rowNotifications.setOnClickListener { switchNotifications.toggle() }

        val prefs = getSharedPreferences("listhub_settings", MODE_PRIVATE)
        val initialDark = prefs.getBoolean("theme_dark", false)
        switchDarkTheme.isChecked = initialDark

        val languageAdapter = ArrayAdapter.createFromResource(
            this, R.array.language_options, android.R.layout.simple_spinner_item
        )
        languageAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerLanguage.adapter = languageAdapter

        // Set initial spinner selection based on currently active app locale or saved pref
        val savedLang = prefs.getString("language", null)
        val activeLang = savedLang ?: getCurrentLanguageCode()
        val initialIndex = when (activeLang) {
            "af" -> 1
            "zu" -> 2
            else -> 0
        }
        isProgrammaticSelection = true
        spinnerLanguage.setSelection(initialIndex)

        loadUserSettings()

        switchDarkTheme.setOnCheckedChangeListener { _, isChecked ->
            getSharedPreferences("listhub_settings", MODE_PRIVATE).edit().putBoolean("theme_dark", isChecked).apply()
            saveThemePreference(isChecked)

            AppCompatDelegate.setDefaultNightMode(
                if (isChecked) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
            )
        }

        switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            saveNotificationsPreference(isChecked)
        }

        spinnerLanguage.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                if (isProgrammaticSelection) {
                    isProgrammaticSelection = false
                    return
                }

                val code = when (position) {
                    1 -> "af"
                    2 -> "zu"
                    else -> "en"
                }

                val currentCode = getCurrentLanguageCode()
                if (code == currentCode) return

                getSharedPreferences("listhub_settings", MODE_PRIVATE).edit().putString("language", code).apply()
                saveLanguagePreference(code)
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(code))
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        btnLogout.setOnClickListener {
            repository.logout()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun getCurrentLanguageCode(): String {
        val locales = AppCompatDelegate.getApplicationLocales()
        if (!locales.isEmpty) {
            val tag = locales.toLanguageTags()
            if (tag.startsWith("af", ignoreCase = true)) return "af"
            if (tag.startsWith("zu", ignoreCase = true)) return "zu"
        }
        return "en"
    }

    private fun loadUserSettings() {
        val uid = repository.getCurrentUser()?.uid ?: return
        lifecycleScope.launch {
            val profile = repository.getUserProfile(uid) ?: return@launch

            tvProfileName.text = profile.fullName
            tvProfileEmail.text = profile.email
            tvAvatarInitial.text = profile.fullName.firstOrNull()?.uppercase() ?: "?"

            getSharedPreferences("listhub_settings", MODE_PRIVATE).edit().putBoolean("theme_dark", profile.themeDark).putString("language", profile.languageCode).apply()

            if (switchDarkTheme.isChecked != profile.themeDark) {
                switchDarkTheme.isChecked = profile.themeDark
            }
            switchNotifications.isChecked = profile.notificationsEnabled

            val languageIndex = when (profile.languageCode) {
                "af" -> 1
                "zu" -> 2
                else -> 0
            }
            if (spinnerLanguage.selectedItemPosition != languageIndex) {
                isProgrammaticSelection = true
                spinnerLanguage.setSelection(languageIndex)
            }
        }
    }

    private fun saveThemePreference(isDark: Boolean) {
        val uid = repository.getCurrentUser()?.uid ?: return
        lifecycleScope.launch {
            val profile = repository.getUserProfile(uid) ?: return@launch
            profile.themeDark = isDark
            repository.updateUserProfile(profile)
        }
    }

    private fun saveNotificationsPreference(enabled: Boolean) {
        val uid = repository.getCurrentUser()?.uid ?: return
        lifecycleScope.launch {
            val profile = repository.getUserProfile(uid) ?: return@launch
            profile.notificationsEnabled = enabled
            repository.updateUserProfile(profile)
        }
    }

    private fun saveLanguagePreference(code: String) {
        val uid = repository.getCurrentUser()?.uid ?: return
        lifecycleScope.launch {
            val profile = repository.getUserProfile(uid) ?: return@launch
            profile.languageCode = code
            repository.updateUserProfile(profile)
        }
    }
}
