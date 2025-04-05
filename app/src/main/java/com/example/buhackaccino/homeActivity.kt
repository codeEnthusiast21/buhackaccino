package com.example.buhackaccino
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.buhackaccino.databinding.ActivityHomeBinding

class homeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Make status bar transparent
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        window.statusBarColor = android.graphics.Color.TRANSPARENT


        binding.searchCard.setOnClickListener {
            Toast.makeText(this, "Ask Gemini something...", Toast.LENGTH_SHORT).show()
            // Here you would typically start a new activity or show a dialog for the search input
        }

        binding.btnVoice.setOnClickListener {
            Toast.makeText(this, "Voice search activated", Toast.LENGTH_SHORT).show()
            // Here you would typically start voice recognition
        }

        binding.btnAdd.setOnClickListener {
            Toast.makeText(this, "Add content", Toast.LENGTH_SHORT).show()
            // Here you would typically show options to add content
        }
    }
}