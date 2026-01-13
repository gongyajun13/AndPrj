package com.jun.andprj.ui.tool

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.jun.andprj.databinding.ItemDateUtilsDemoBinding

class DateUtilsDemoAdapter(
    private val items: List<DateUtilsDemoItem>
) : RecyclerView.Adapter<DateUtilsDemoAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemDateUtilsDemoBinding.inflate(
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
        private val binding: ItemDateUtilsDemoBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: DateUtilsDemoItem) {
            binding.tvTitle.text = item.title
            binding.tvResult.text = item.result
            binding.tvCode.text = item.code
        }
    }
}

