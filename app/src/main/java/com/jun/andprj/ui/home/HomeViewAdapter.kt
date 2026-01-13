package com.jun.andprj.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.jun.andprj.databinding.ItemHomeViewBinding

class HomeViewAdapter(
    private val items: List<HomeViewFragment.HomeViewItem>,
    private val onItemClick: (HomeViewFragment.HomeViewItem) -> Unit
) : RecyclerView.Adapter<HomeViewAdapter.HomeViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HomeViewHolder {
        val binding = ItemHomeViewBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return HomeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HomeViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item, onItemClick)
    }

    override fun getItemCount(): Int = items.size

    class HomeViewHolder(
        private val binding: ItemHomeViewBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(
            item: HomeViewFragment.HomeViewItem,
            onItemClick: (HomeViewFragment.HomeViewItem) -> Unit
        ) {
            binding.tvTitle.text = item.title
            binding.tvDescription.text = item.description
            binding.root.setOnClickListener { onItemClick(item) }
        }
    }
}


