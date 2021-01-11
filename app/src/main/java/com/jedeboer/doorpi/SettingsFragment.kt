package com.jedeboer.doorpi

import android.os.Bundle
import android.text.InputType
import androidx.fragment.app.Fragment
import androidx.preference.EditTextPreference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceScreen
import androidx.preference.SwitchPreference

/**
 * A simple [Fragment] subclass.
 */
class SettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        val portPreference = preferenceManager.findPreference<EditTextPreference>("pi_port")!!
        portPreference.setOnBindEditTextListener { editText ->
            editText.inputType = InputType.TYPE_CLASS_NUMBER
        }

        val versionPreference = preferenceManager.findPreference<PreferenceScreen>("version")!!
        versionPreference.summary = BuildConfig.VERSION_NAME

        val useHttps = preferenceManager.findPreference<SwitchPreference>("ssl")!!
        val sslCheck = preferenceManager.findPreference<SwitchPreference>("sslCertificate")!!

        if (!useHttps.isChecked) {
            sslCheck.isEnabled = false
            sslCheck.isChecked = false
        }

        useHttps.setOnPreferenceChangeListener { _, newValue ->
            sslCheck.isEnabled = newValue != false

            if (newValue == false) {
                sslCheck.isChecked = false
            }
            true
        }
    }
}
