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

    // Key for SharedPreferences
    private val PREFS   = "kasisave_prefs"
    private val DARK_KEY = "dark_mode_enabled"

    override fun onCreate(savedInstanceState: Bundle?) {
        // 🔑 Apply the saved theme early, before super.onCreate inflates views
        applySavedDarkMode()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        notificationsSwitch = findViewById(R.id.notificationsSwitch)
        darkModeSwitch       = findViewById(R.id.darkModeSwitch)

        // ⏪ Restore switch position from prefs
        darkModeSwitch.isChecked = getSharedPrefs().getBoolean(DARK_KEY, false)

        notificationsSwitch.setOnCheckedChangeListener { _, isChecked ->
            Toast.makeText(
                this,
                "Notifications ${if (isChecked) "enabled" else "disabled"}",
                Toast.LENGTH_SHORT
            ).show()
        }

        darkModeSwitch.setOnCheckedChangeListener { _, isChecked ->
            toggleDarkMode(isChecked)
        }
    }

    /** Apply the stored night-mode choice before UI inflation */
    private fun applySavedDarkMode() {
        val enabled = getSharedPrefs().getBoolean(DARK_KEY, false)
        val mode    = if (enabled)
            AppCompatDelegate.MODE_NIGHT_YES
        else
            AppCompatDelegate.MODE_NIGHT_NO
        AppCompatDelegate.setDefaultNightMode(mode)
    }

    /** Change theme at runtime and store preference */
    private fun toggleDarkMode(enabled: Boolean) {
        val mode = if (enabled)
            AppCompatDelegate.MODE_NIGHT_YES
        else
            AppCompatDelegate.MODE_NIGHT_NO

        AppCompatDelegate.setDefaultNightMode(mode)
        getSharedPrefs().edit().putBoolean(DARK_KEY, enabled).apply()

        Toast.makeText(
            this,
            "Dark mode ${if (enabled) "enabled" else "disabled"}",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun getSharedPrefs() =
        getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
