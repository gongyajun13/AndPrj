package com.jun.andprj.ui.permission

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.jun.andprj.databinding.ItemPermissionDemoBinding

class PermissionDemoAdapter(
    private val items: List<PermissionDemoActivity.PermissionItem>,
    private val onItemClick: (PermissionDemoActivity.PermissionItem) -> Unit
) : RecyclerView.Adapter<PermissionDemoAdapter.PermissionViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PermissionViewHolder {
        val binding = ItemPermissionDemoBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PermissionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PermissionViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item, onItemClick)
    }

    override fun getItemCount(): Int = items.size

    class PermissionViewHolder(
        private val binding: ItemPermissionDemoBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(
            item: PermissionDemoActivity.PermissionItem,
            onItemClick: (PermissionDemoActivity.PermissionItem) -> Unit
        ) {
            binding.tvTitle.text = item.title
            binding.tvDescription.text = item.description
            binding.tvPermissionCount.text = "权限数量：${item.permissions.size}"
            binding.root.setOnClickListener { onItemClick(item) }
        }
    }
}
