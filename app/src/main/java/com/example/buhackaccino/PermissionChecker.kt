package com.example.buhackaccino

import android.content.Intent
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.view.View
import android.provider.Settings
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.buhackaccino.databinding.ActivityPermissionCheckerBinding
import java.util.Locale

class PermissionCheckerActivity : AppCompatActivity(), TextToSpeech.OnInitListener {
    private lateinit var binding: ActivityPermissionCheckerBinding
    private lateinit var textToSpeech: TextToSpeech
    private var selectedLanguage = "en"

    private val languageMap = mapOf(
        "English (US)" to LanguageInfo(Locale.US, "en"),
        "Hindi" to LanguageInfo(Locale("hi", "IN"), "hi"),
        "Tamil" to LanguageInfo(Locale("ta", "IN"), "ta"),
        "Bengali" to LanguageInfo(Locale("bn", "IN"), "bn"),
        "Telugu" to LanguageInfo(Locale("te", "IN"), "te"),
        "Marathi" to LanguageInfo(Locale("mr", "IN"), "mr"),
        "Gujarati" to LanguageInfo(Locale("gu", "IN"), "gu"),
        "Kannada" to LanguageInfo(Locale("kn", "IN"), "kn"),
        "Urdu" to LanguageInfo(Locale("ur", "IN"), "ur")
    )

    data class LanguageInfo(val locale: Locale, val code: String)

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
        val languages = languageMap.keys.toList()
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, languages)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        binding.languageSpinner.apply {
            this.adapter = adapter
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    val selectedLanguageName = languages[position]
                    val languageInfo = languageMap[selectedLanguageName]
                    if (languageInfo != null) {
                        selectedLanguage = languageInfo.code
                        textToSpeech.language = languageInfo.locale
                    }
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        }
    }

    private fun setupButtons() {
        binding.apply {
            settingsButton.setOnClickListener {

            }

            goToMainButton.setOnClickListener {
                val extras = Bundle().apply {
                    putString("selected_language", selectedLanguage)
                    putString(
                        "selected_locale",
                        languageMap[binding.languageSpinner.selectedItem.toString()]?.locale.toString()
                    )
                }

                TransitionActivity.start(this@PermissionCheckerActivity, MainActivity::class.java, extras)
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
            statusText.text = "Select your preferred language"
            languageSpinner.visibility = View.VISIBLE
            settingsButton.visibility = View.GONE
            goToMainButton.visibility = View.VISIBLE
            cancelButton.visibility = View.VISIBLE
        }
    }

    private fun showAccessibilityNeeded() {
        binding.apply {
            statusImage.setImageResource(R.drawable.ic_warning)
            statusText.text = "Please enable Voice Control Service in Accessibility Settings"
            languageSpinner.visibility = View.VISIBLE
            settingsButton.visibility = View.VISIBLE
            goToMainButton.visibility = View.VISIBLE
            cancelButton.visibility = View.GONE
        }
        speakText("Please enable voice control service in accessibility settings")
    }

//    private fun startMainActivity() {
//        val intent = Intent(this, MainActivity::class.java).apply {
//            putExtra("selected_language", selectedLanguage)
//            putExtra("selected_locale", languageMap[binding.languageSpinner.selectedItem.toString()]?.locale.toString())
//        }
//        startActivity(intent)
//        finish()
//    }

    private fun speakText(text: String) {
        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val locale = languageMap[binding.languageSpinner.selectedItem.toString()]?.locale ?: Locale.US
            textToSpeech.language = locale
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