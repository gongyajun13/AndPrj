package com.jun.andprj.ui.notify

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.jun.andprj.databinding.ItemNotifyDemoBinding

class NotifyDemoAdapter(
    private val items: List<NotifyDemoActivity.NotifyItem>,
    private val onItemClick: (NotifyDemoActivity.NotifyItem) -> Unit
) : RecyclerView.Adapter<NotifyDemoAdapter.NotifyViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotifyViewHolder {
        val binding = ItemNotifyDemoBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return NotifyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NotifyViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item, onItemClick)
    }

    override fun getItemCount(): Int = items.size

    class NotifyViewHolder(
        private val binding: ItemNotifyDemoBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(
            item: NotifyDemoActivity.NotifyItem,
            onItemClick: (NotifyDemoActivity.NotifyItem) -> Unit
        ) {
            binding.tvTitle.text = item.title
            binding.tvMessage.text = item.message
            binding.root.setOnClickListener { onItemClick(item) }
        }
    }
}

