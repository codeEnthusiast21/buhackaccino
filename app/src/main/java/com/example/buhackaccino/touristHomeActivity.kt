package com.example.buhackaccino

import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.buhackaccino.databinding.ActivityTouristHomeBinding

class touristHomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTouristHomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTouristHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Card click listener
        binding.btnCamera.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            // Optional: send data through intent
            intent.putExtra("PLACE_NAME", "Tokyo Tower")
            startActivity(intent)
        }
    }
}
