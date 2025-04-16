package com.example.buhackaccino

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class PlacesAdapter(private val places: List<Place>) :
    RecyclerView.Adapter<PlacesAdapter.PlaceViewHolder>() {

    class PlaceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val placeImage: ImageView = itemView.findViewById(R.id.placeImage)
        val placeName: TextView = itemView.findViewById(R.id.placeName)
        val placeRating: RatingBar = itemView.findViewById(R.id.placeRating)
        val placeDistance: TextView = itemView.findViewById(R.id.placeDistance)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.place_item, parent, false)
        return PlaceViewHolder(view)
    }

    override fun onBindViewHolder(holder: PlaceViewHolder, position: Int) {
        val place = places[position]
        holder.placeName.text = place.name
        holder.placeImage.setImageResource(place.imageResId)
        holder.placeRating.rating = place.rating
        holder.placeDistance.text = place.distance
    }

    override fun getItemCount() = places.size
}