package com.example.buhackaccino

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import java.text.SimpleDateFormat
import java.util.Locale

class ChatAdapter : RecyclerView.Adapter<ChatAdapter.MessageViewHolder>() {
    private val messages = mutableListOf<ChatMessage>()

    class MessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val messageText: TextView = view.findViewById(R.id.messageText)
        val messageImage: ImageView = view.findViewById(R.id.messageImage)
        val timestamp: TextView = view.findViewById(R.id.timestamp)
    }

    fun addMessage(message: ChatMessage) {
        messages.add(message)
        notifyItemInserted(messages.size - 1)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.chat_message_item, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val message = messages[position]
        holder.messageText.text = message.text

        message.imageUrl?.let { url ->
            holder.messageImage.visibility = View.VISIBLE
            Glide.with(holder.messageImage)
                .load(url)
                .into(holder.messageImage)
        } ?: run {
            holder.messageImage.visibility = View.GONE
        }

        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        holder.timestamp.text = sdf.format(message.timestamp)
    }

    override fun getItemCount() = messages.size
}