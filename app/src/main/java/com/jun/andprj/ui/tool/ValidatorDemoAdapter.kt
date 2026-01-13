package com.jun.andprj.ui.tool

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.jun.andprj.databinding.ItemValidatorDemoBinding

class ValidatorDemoAdapter(
    private val items: List<ValidatorDemoItem>
) : RecyclerView.Adapter<ValidatorDemoAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemValidatorDemoBinding.inflate(
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
        private val binding: ItemValidatorDemoBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ValidatorDemoItem) {
            binding.tvTitle.text = item.title
            binding.tvInput.text = item.input
            binding.tvResult.text = item.result
            binding.tvCode.text = item.code
        }
    }
}

