package com.example.kasisave

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.switchmaterial.SwitchMaterial

class SettingsActivity : AppCompatActivity() {

    private lateinit var notificationsSwitch: SwitchMaterial
    private lateinit var darkModeSwitch: SwitchMaterial

    // SharedPreferences keys
    private val PREFS_NAME = "kasisave_prefs"
    private val DARK_MODE_KEY = "dark_mode_enabled"
    private val NOTIFICATIONS_KEY = "notifications_enabled"

    override fun onCreate(savedInstanceState: Bundle?) {
        applySavedDarkMode() // Apply theme before inflating layout
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // Initialize UI components
        notificationsSwitch = findViewById(R.id.notificationsSwitch)
        darkModeSwitch = findViewById(R.id.darkModeSwitch)

        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // Restore saved switch states
        darkModeSwitch.isChecked = prefs.getBoolean(DARK_MODE_KEY, false)
        notificationsSwitch.isChecked = prefs.getBoolean(NOTIFICATIONS_KEY, true)

        // Dark mode toggle handler
        darkModeSwitch.setOnCheckedChangeListener { _, isChecked ->
            toggleDarkMode(isChecked)
        }

        // Notifications toggle handler
        notificationsSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(NOTIFICATIONS_KEY, isChecked).apply()
            Toast.makeText(
                this,
                "Notifications ${if (isChecked) "enabled" else "disabled"}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun applySavedDarkMode() {
        val enabled = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(DARK_MODE_KEY, false)
        AppCompatDelegate.setDefaultNightMode(
            if (enabled) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )
    }

    private fun toggleDarkMode(enabled: Boolean) {
        AppCompatDelegate.setDefaultNightMode(
            if (enabled) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(DARK_MODE_KEY, enabled).apply()

        Toast.makeText(
            this,
            "Dark mode ${if (enabled) "enabled" else "disabled"}",
            Toast.LENGTH_SHORT
        ).show()
    }
}
