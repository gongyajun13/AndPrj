package com.jun.andprj.ui.tool

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.jun.andprj.databinding.ItemFileUtilsDemoBinding

class FileUtilsDemoAdapter(
    private val items: List<FileUtilsDemoItem>
) : RecyclerView.Adapter<FileUtilsDemoAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemFileUtilsDemoBinding.inflate(
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
        private val binding: ItemFileUtilsDemoBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: FileUtilsDemoItem) {
            binding.tvTitle.text = item.title
            binding.tvResult.text = item.result
            binding.tvCode.text = item.code
        }
    }
}

