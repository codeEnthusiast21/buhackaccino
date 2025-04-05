package com.example.buhackaccino

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.view.View
import android.view.accessibility.AccessibilityManager
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import com.example.buhackaccino.databinding.ActivityPermissionCheckerBinding

class PermissionChecker : AppCompatActivity() {
    private lateinit var binding: ActivityPermissionCheckerBinding
    private lateinit var tts: TextToSpeech
    private var isVoiceControlEnabled = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPermissionCheckerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                checkAccessibilitySettings()
            }
        }
    }

    private fun checkAccessibilitySettings() {
        val accessibilityManager = getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val isTalkBackEnabled = accessibilityManager.isEnabled &&
                accessibilityManager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_SPOKEN).isNotEmpty()

        if (isTalkBackEnabled) {
            isVoiceControlEnabled = true
            binding.statusImage.setImageResource(R.drawable.ic_check)
            binding.statusText.text = "Not visually impaired?"
            binding.cancelButton.visibility = View.VISIBLE
            speakText("Switching to voice controlled mode. Tap cancel if you are not visually impaired")

            binding.cancelButton.setOnClickListener {
                isVoiceControlEnabled = false
                tts.stop()
                binding.statusText.text = "Voice control cancelled"
                binding.cancelButton.visibility = View.GONE

                // Navigate to main activity after short delay
                Handler(Looper.getMainLooper()).postDelayed({
                    startActivity(Intent(this, MainActivity::class.java)
                        .putExtra("voice_control_enabled", false))
                    finish()
                }, 1500)
            }

            // Auto proceed with voice control after delay if not cancelled
            Handler(Looper.getMainLooper()).postDelayed({
                if (isVoiceControlEnabled) {
                    startActivity(Intent(this, MainActivity::class.java)
                        .putExtra("voice_control_enabled", true))
                    finish()
                }
            }, 5000)
        } else {
            binding.statusImage.setImageResource(R.drawable.ic_error)
            binding.statusText.text = getString(R.string.accessibility_disabled)
            binding.settingsButton.visibility = View.VISIBLE
            binding.settingsButton.setOnClickListener {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
        }
    }

    private fun speakText(text: String) {
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (tts.isSpeaking) {
            tts.stop()
        }
        tts.shutdown()
    }
}