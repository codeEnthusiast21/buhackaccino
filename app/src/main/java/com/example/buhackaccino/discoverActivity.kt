package com.example.buhackaccino

import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.buhackaccino.databinding.ActivityDiscoverBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import java.util.*

class discoverActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var binding: ActivityDiscoverBinding
    private lateinit var textToSpeech: TextToSpeech
    private var selectedLanguage: Locale = Locale.US
    private var translators = mutableMapOf<String, Translator>()
    private var isTranslating = false

    // Map language names to Locale objects and ML Kit language codes
    private val languageMap = mapOf(
        "English (US)" to LanguageInfo(Locale.US, TranslateLanguage.ENGLISH),
        "Hindi" to LanguageInfo(Locale("hi", "IN"), TranslateLanguage.HINDI),
        "Tamil" to LanguageInfo(Locale("ta", "IN"), TranslateLanguage.TAMIL),
        "Bengali" to LanguageInfo(Locale("bn", "IN"), TranslateLanguage.BENGALI),
        "Telugu" to LanguageInfo(Locale("te", "IN"), TranslateLanguage.TELUGU),
        "Marathi" to LanguageInfo(Locale("mr", "IN"), TranslateLanguage.MARATHI),
        "Gujarati" to LanguageInfo(Locale("gu", "IN"), TranslateLanguage.GUJARATI),
        "Kannada" to LanguageInfo(Locale("kn", "IN"), TranslateLanguage.KANNADA),
        "Urdu" to LanguageInfo(Locale("ur", "IN"), TranslateLanguage.URDU)
    )

    data class LanguageInfo(val locale: Locale, val mlKitCode: String)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDiscoverBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize TextToSpeech engine
        textToSpeech = TextToSpeech(this, this)

        // Set up language spinner
        setupLanguageSpinner()

        // Set up speak button click listener
        binding.speakButton.setOnClickListener {
            val selectedLanguageName = binding.languageSpinner.selectedItem.toString()
            translateAndSpeak(selectedLanguageName)
        }
    }

    private fun setupLanguageSpinner() {
        val adapter = ArrayAdapter.createFromResource(
            this,
            R.array.language_options,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.languageSpinner.adapter = adapter
        }

        binding.languageSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedLanguageName = parent.getItemAtPosition(position).toString()
                val languageInfo = languageMap[selectedLanguageName]

                if (languageInfo != null) {
                    selectedLanguage = languageInfo.locale

                    // Check if selected language is available for TTS
                    val languageAvailable = textToSpeech.isLanguageAvailable(selectedLanguage)
                    if (languageAvailable == TextToSpeech.LANG_MISSING_DATA ||
                        languageAvailable == TextToSpeech.LANG_NOT_SUPPORTED) {
                        binding.statusTextView.text = "TTS not supported for: $selectedLanguageName"
                    } else {
                        textToSpeech.language = selectedLanguage
                        binding.statusTextView.text = "Selected language: $selectedLanguageName"

                        // Prepare translator for the selected language
                        if (selectedLanguageName != "English (US)") {
                            prepareTranslator(selectedLanguageName)
                        }
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // Do nothing
            }
        }
    }

    private fun prepareTranslator(targetLanguageName: String) {
        if (translators.containsKey(targetLanguageName)) {
            // Translator already created
            return
        }

        val languageInfo = languageMap[targetLanguageName] ?: return

        // Create a translator from English to the target language
        val options = TranslatorOptions.Builder()
            .setSourceLanguage(TranslateLanguage.ENGLISH)
            .setTargetLanguage(languageInfo.mlKitCode)
            .build()

        val translator = Translation.getClient(options)
        translators[targetLanguageName] = translator

        // Download the translation model if needed
        val conditions = DownloadConditions.Builder()
            .requireWifi()
            .build()

        binding.statusTextView.text = "Downloading translation model..."
        binding.speakButton.isEnabled = false

        translator.downloadModelIfNeeded(conditions)
            .addOnSuccessListener {
                binding.statusTextView.text = "Translation model ready for $targetLanguageName"
                binding.speakButton.isEnabled = true
            }
            .addOnFailureListener { exception ->
                binding.statusTextView.text = "Failed to download model: ${exception.localizedMessage}"
                binding.speakButton.isEnabled = true
            }
    }

    private fun translateAndSpeak(targetLanguageName: String) {
        if (isTranslating) {
            Toast.makeText(this, "Translation in progress...", Toast.LENGTH_SHORT).show()
            return
        }

        val text = binding.inputEditText.text.toString().trim()
        if (text.isEmpty()) {
            Toast.makeText(this, "Please enter text to translate and speak", Toast.LENGTH_SHORT).show()
            return
        }

        // If selected language is English, just speak without translation
        if (targetLanguageName == "English (US)") {
            speakText(text)
            return
        }

        // Get the translator for the selected language
        val translator = translators[targetLanguageName]
        if (translator == null) {
            binding.statusTextView.text = "Translator not ready. Please wait or try again."
            prepareTranslator(targetLanguageName)
            return
        }

        isTranslating = true
        binding.statusTextView.text = "Translating..."
        binding.speakButton.isEnabled = false

        translator.translate(text)
            .addOnSuccessListener { translatedText ->
                binding.statusTextView.text = "Translation complete"
                binding.speakButton.isEnabled = true

                // Display the translated text
                MaterialAlertDialogBuilder(this)
                    .setTitle("Translation")
                    .setMessage("Original: $text\n\nTranslated: $translatedText")
                    .setPositiveButton("Speak") { _, _ -> speakText(translatedText) }
                    .setNegativeButton("Cancel", null)
                    .show()

                isTranslating = false
            }
            .addOnFailureListener { exception ->
                binding.statusTextView.text = "Translation failed: ${exception.localizedMessage}"
                binding.speakButton.isEnabled = true
                isTranslating = false

                // Offer to speak in original language
                MaterialAlertDialogBuilder(this)
                    .setTitle("Translation Failed")
                    .setMessage("Would you like to speak the text in its original language?")
                    .setPositiveButton("Yes") { _, _ -> speakText(text) }
                    .setNegativeButton("No", null)
                    .show()
            }
    }

    private fun speakText(text: String) {
        // Use QUEUE_FLUSH to stop any current speech and start new one
        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, "tts1")
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            // Set default language to US English
            val result = textToSpeech.setLanguage(Locale.US)

            if (result == TextToSpeech.LANG_MISSING_DATA ||
                result == TextToSpeech.LANG_NOT_SUPPORTED) {
                binding.statusTextView.text = "Default language not supported"
            } else {
                // Set speech rate and pitch to normal values
                textToSpeech.setSpeechRate(1.0f)
                textToSpeech.setPitch(1.0f)
                binding.statusTextView.text = "TTS engine initialized successfully"
            }
        } else {
            binding.statusTextView.text = "Failed to initialize TTS engine"
        }
    }

    override fun onDestroy() {
        // Shutdown TTS engine and translators to release resources
        if (::textToSpeech.isInitialized) {
            textToSpeech.stop()
            textToSpeech.shutdown()
        }

        // Close all translators
        for (translator in translators.values) {
            translator.close()
        }

        super.onDestroy()
    }
}