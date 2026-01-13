package com.jun.andprj.ui.tool

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.jun.andprj.databinding.ItemImageUtilsDemoBinding

class ImageUtilsDemoAdapter(
    private val items: List<ImageUtilsDemoItem>
) : RecyclerView.Adapter<ImageUtilsDemoAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemImageUtilsDemoBinding.inflate(
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
        private val binding: ItemImageUtilsDemoBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ImageUtilsDemoItem) {
            binding.tvTitle.text = item.title
            binding.tvResult.text = item.result
            binding.tvCode.text = item.code
        }
    }
}

