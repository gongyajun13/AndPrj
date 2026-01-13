package com.jun.andprj.ui.tool

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import com.jun.andprj.databinding.ItemNetworkDemoBinding
import com.jun.core.ui.adapter.BaseAdapter

/**
 * 网络请求示例Adapter
 * 使用 BaseAdapter 实现，支持 DiffUtil 自动更新
 */
class NetworkDemoAdapter : BaseAdapter<NetworkDemoItem, ItemNetworkDemoBinding>(
    diffCallback = NetworkDemoItemDiffCallback()
) {

    override fun createBinding(parent: ViewGroup, viewType: Int): ItemNetworkDemoBinding {
        return ItemNetworkDemoBinding.inflate(
            android.view.LayoutInflater.from(parent.context),
            parent,
            false
        )
    }

    override fun bind(binding: ItemNetworkDemoBinding, item: NetworkDemoItem, position: Int) {
        binding.tvTitle.text = item.title
        binding.tvResult.text = item.result
        binding.tvCode.text = item.code
        
        // 如果 code 为空，隐藏代码 TextView
        binding.tvCode.visibility = if (item.code.isBlank()) {
            android.view.View.GONE
        } else {
            android.view.View.VISIBLE
        }
    }
    
    override fun bind(binding: ItemNetworkDemoBinding, item: NetworkDemoItem, position: Int, payloads: List<Any>) {
        // 如果有 payload，进行局部更新
        if (payloads.isNotEmpty()) {
            // 处理 payload：如果是 List，展开；否则转换为 String
            val payloadSet = payloads.flatMap { payload ->
                when (payload) {
                    is List<*> -> payload.mapNotNull { it as? String }
                    else -> listOf(payload.toString())
                }
            }.toSet()
            
            // 根据 payload 更新对应的视图
            if (payloadSet.contains("title")) {
                binding.tvTitle.text = item.title
            }
            if (payloadSet.contains("result")) {
                binding.tvResult.text = item.result
            }
            if (payloadSet.contains("code")) {
                binding.tvCode.text = item.code
                // 如果 code 为空，隐藏代码 TextView
                binding.tvCode.visibility = if (item.code.isBlank()) {
                    android.view.View.GONE
                } else {
                    android.view.View.VISIBLE
                }
            }
        } else {
            // 没有 payload，完整更新
            bind(binding, item, position)
        }
    }
}

/**
 * DiffUtil ItemCallback for NetworkDemoItem
 */
class NetworkDemoItemDiffCallback : DiffUtil.ItemCallback<NetworkDemoItem>() {
    override fun areItemsTheSame(oldItem: NetworkDemoItem, newItem: NetworkDemoItem): Boolean {
        // 使用 title 和 code 的组合作为唯一标识
        return oldItem.title == newItem.title && oldItem.code == newItem.code
    }

    override fun areContentsTheSame(oldItem: NetworkDemoItem, newItem: NetworkDemoItem): Boolean {
        // 比较所有字段，确保内容变化时能触发更新
        return oldItem.title == newItem.title &&
                oldItem.result == newItem.result &&
                oldItem.code == newItem.code
    }
    
    override fun getChangePayload(oldItem: NetworkDemoItem, newItem: NetworkDemoItem): Any? {
        // 如果只是 result 变化，返回 payload 用于局部更新
        val payloads = mutableListOf<String>()
        if (oldItem.result != newItem.result) {
            payloads.add("result")
        }
        if (oldItem.title != newItem.title) {
            payloads.add("title")
        }
        if (oldItem.code != newItem.code) {
            payloads.add("code")
        }
        return payloads.ifEmpty { null }
    }
}
