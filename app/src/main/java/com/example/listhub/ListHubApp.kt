package com.example.listhub

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

class ListHubApp : Application() {
    override fun onCreate() {
        super.onCreate()
        val prefs = getSharedPreferences("listhub_settings", MODE_PRIVATE)
        AppCompatDelegate.setDefaultNightMode(if (prefs.getBoolean("theme_dark", false)) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO)
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(prefs.getString("language", "en") ?: "en"))
    }
}
