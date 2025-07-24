package com.mobile.emoticon.fast.emoji

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.mobile.emoticon.fast.emoji.databinding.ItemSimpleEmojiBinding

class SimpleEmojiAdapter(
    private val onItemClick: (Int) -> Unit
) : RecyclerView.Adapter<SimpleEmojiAdapter.SimpleEmojiViewHolder>() {

    private var imageList: List<Int> = emptyList()

    fun updateData(newList: List<Int>) {
        imageList = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SimpleEmojiViewHolder {
        val binding = ItemSimpleEmojiBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SimpleEmojiViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SimpleEmojiViewHolder, position: Int) {
        holder.bind(imageList[position])
    }

    override fun getItemCount(): Int = imageList.size

    inner class SimpleEmojiViewHolder(
        private val binding: ItemSimpleEmojiBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(resourceId: Int) {
            binding.imageView.setImageResource(resourceId)

            binding.root.setOnClickListener {
                onItemClick(resourceId)
            }
        }
    }
}