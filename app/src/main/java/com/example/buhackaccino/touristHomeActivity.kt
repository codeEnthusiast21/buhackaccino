package com.example.buhackaccino

import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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
        val places = listOf(
            Place("Tokyo Tower", R.drawable.tokyo_panorama, 4.5f, "0.5 km away"),
            Place("Sensoji Temple", R.drawable.sensoji, 4.8f, "1.2 km away"),
            Place("Meiji Shrine", R.drawable.meiji, 4.7f, "2.0 km away"),
            Place("Shibuya Crossing", R.drawable.shibuya, 4.6f, "0.8 km away"),
            Place("Ueno Park", R.drawable.ueno, 4.4f, "1.5 km away")
        )

        // Setup RecyclerView
        val recyclerView = findViewById<RecyclerView>(R.id.placesRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = PlacesAdapter(places)
    }

}

