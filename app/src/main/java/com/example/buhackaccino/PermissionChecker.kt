package com.example.buhackaccino

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.view.View
import android.provider.Settings
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.buhackaccino.databinding.ActivityPermissionCheckerBinding
import java.util.Locale

class PermissionCheckerActivity : AppCompatActivity(), TextToSpeech.OnInitListener {
    private lateinit var binding: ActivityPermissionCheckerBinding
    private lateinit var textToSpeech: TextToSpeech
    private var selectedLanguage = "en"

    private var accessibilitySettingsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        checkAccessibilityPermission()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPermissionCheckerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        textToSpeech = TextToSpeech(this, this)

        setupLanguageSpinner()
        setupButtons()
        checkAccessibilityPermission()
    }

    private fun setupLanguageSpinner() {
        val languages = arrayOf("English", "Hindi")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, languages)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        binding.languageSpinner.apply {
            this.adapter = adapter
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    selectedLanguage = if (position == 0) "en" else "hi"
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        }
    }

    private fun setupButtons() {
        binding.apply {
            settingsButton.setOnClickListener {
                openAccessibilitySettings()
            }

            skipToMainButton.setOnClickListener {
                startMainActivity()
            }

            continueButton.setOnClickListener {
                startMainActivity()
            }

            cancelButton.setOnClickListener {
                finish()
            }
        }
    }

    private fun checkAccessibilityPermission() {
        if (isAccessibilityServiceEnabled()) {
            showVoiceControlDisabled()
        } else {
            showAccessibilityNeeded()
        }
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val expectedServiceName = "${packageName}/.services.VoiceControlService"
        try {
            val enabledServices = Settings.Secure.getString(
                contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
            return enabledServices?.contains(expectedServiceName) == true
        } catch (e: Exception) {
            return false
        }
    }

    private fun showVoiceControlDisabled() {
        binding.apply {
            statusImage.setImageResource(R.drawable.ic_info)
            statusText.text = "Voice Control will not start automatically"
            languageSpinner.visibility = View.VISIBLE
            settingsButton.visibility = View.GONE
            continueButton.visibility = View.VISIBLE
            skipToMainButton.visibility = View.VISIBLE
            cancelButton.visibility = View.VISIBLE
        }
    }

    private fun showAccessibilityNeeded() {
        binding.apply {
            statusImage.setImageResource(R.drawable.ic_warning)
            statusText.text = "Please enable Voice Control Service in Accessibility Settings"
            languageSpinner.visibility = View.GONE
            settingsButton.visibility = View.VISIBLE
            continueButton.visibility = View.GONE
            skipToMainButton.visibility = View.VISIBLE
            cancelButton.visibility = View.GONE
        }
        speakText("Please enable voice control service in accessibility settings")
    }

    private fun openAccessibilitySettings() {
        try {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            accessibilitySettingsLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Unable to open Accessibility Settings", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startMainActivity() {
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("selected_language", selectedLanguage)
        }
        startActivity(intent)
        finish()
    }

    private fun speakText(text: String) {
        val locale = if (selectedLanguage == "hi") Locale("hi", "IN") else Locale.US
        textToSpeech.language = locale
        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            checkAccessibilityPermission()
        }
    }

    override fun onDestroy() {
        if (::textToSpeech.isInitialized) {
            textToSpeech.stop()
            textToSpeech.shutdown()
        }
        super.onDestroy()
    }
}