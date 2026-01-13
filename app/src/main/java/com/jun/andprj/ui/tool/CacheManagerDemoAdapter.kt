package com.jun.andprj.ui.tool

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.jun.andprj.databinding.ItemCacheManagerDemoBinding

class CacheManagerDemoAdapter(
    private val items: List<CacheManagerDemoItem>
) : RecyclerView.Adapter<CacheManagerDemoAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCacheManagerDemoBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class ViewHolder(
        private val binding: ItemCacheManagerDemoBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: CacheManagerDemoItem) {
            binding.tvKey.text = item.key
            binding.tvValue.text = item.value
            binding.tvCode.text = item.code
        }
    }
}

