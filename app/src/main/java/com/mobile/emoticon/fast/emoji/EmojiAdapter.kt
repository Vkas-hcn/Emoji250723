package com.mobile.emoticon.fast.emoji

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView

sealed class EmojiItem {
    data class SingleEmoji(val resourceId: Int) : EmojiItem()
    data class CompositeEmoji(
        val faceId: Int,
        val eyeId: Int,
        val mouthId: Int,
        val handId: Int
    ) : EmojiItem()
}

class EmojiAdapter(
    private var emojiList: List<EmojiItem> = emptyList(),
    private val onItemClick: (EmojiItem) -> Unit = {}
) : RecyclerView.Adapter<EmojiAdapter.EmojiViewHolder>() {

    class EmojiViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val singleImage: ImageView = itemView.findViewById(R.id.img_single_emoji)
        val faceImage: ImageView = itemView.findViewById(R.id.img_face)
        val eyeImage: ImageView = itemView.findViewById(R.id.img_eye)
        val mouthImage: ImageView = itemView.findViewById(R.id.img_mouth)
        val handImage: ImageView = itemView.findViewById(R.id.img_hand)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EmojiViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_emoji, parent, false)
        return EmojiViewHolder(view)
    }

    override fun onBindViewHolder(holder: EmojiViewHolder, position: Int) {
        val item = emojiList[position]

        when (item) {
            is EmojiItem.SingleEmoji -> {
                holder.singleImage.visibility = View.VISIBLE
                holder.faceImage.visibility = View.GONE
                holder.eyeImage.visibility = View.GONE
                holder.mouthImage.visibility = View.GONE
                holder.handImage.visibility = View.GONE

                holder.singleImage.setImageResource(item.resourceId)
            }

            is EmojiItem.CompositeEmoji -> {
                holder.singleImage.visibility = View.GONE
                holder.faceImage.visibility = View.VISIBLE
                holder.eyeImage.visibility = View.VISIBLE
                holder.mouthImage.visibility = View.VISIBLE
                holder.handImage.visibility = View.VISIBLE

                holder.faceImage.setImageResource(item.faceId)
                holder.eyeImage.setImageResource(item.eyeId)
                holder.mouthImage.setImageResource(item.mouthId)
                holder.handImage.setImageResource(item.handId)
            }
        }

        holder.itemView.setOnClickListener {
            onItemClick(item)
        }
    }

    override fun getItemCount(): Int = emojiList.size

    fun updateData(newList: List<EmojiItem>) {
        emojiList = newList
        notifyDataSetChanged()
    }
}