package com.jun.andprj.ui.recycler

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.jun.andprj.databinding.ItemRecyclerLayoutBinding

/**
 * RecyclerView 布局示例适配器
 */
class RecyclerLayoutAdapter(
    private val items: List<RecyclerLayoutActivity.RecyclerItem>
) : RecyclerView.Adapter<RecyclerLayoutAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemRecyclerLayoutBinding.inflate(
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
        private val binding: ItemRecyclerLayoutBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: RecyclerLayoutActivity.RecyclerItem) {
            binding.tvTitle.text = item.title
            binding.tvContent.text = item.content
        }
    }
}









