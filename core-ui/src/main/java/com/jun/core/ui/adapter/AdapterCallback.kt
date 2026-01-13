package com.jun.core.ui.adapter

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding

/**
 * Adapter 回调接口
 * 定义所有需要子类实现或重写的回调方法
 * 
 * 使用示例：
 * ```kotlin
 * class MyAdapter : BaseAdapter<Item, ItemBinding>(), AdapterCallback<Item, ItemBinding> {
 *     override fun createBinding(parent: ViewGroup, viewType: Int) = 
 *         ItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
 *     
 *     override fun bind(binding: ItemBinding, item: Item, position: Int) {
 *         binding.tvTitle.text = item.title
 *     }
 * }
 * ```
 */
interface AdapterCallback<T, VB : ViewBinding> {
    
    /**
     * 获取 item 的 viewType（可选，用于多类型支持）
     * 默认返回 0（单类型）
     */
    fun getItemViewType(item: T, position: Int): Int = 0
    
    /**
     * 创建 ViewBinding（单类型模式，必须实现）
     */
    fun createBinding(parent: ViewGroup, viewType: Int): VB
    
    /**
     * 创建指定 viewType 的 ViewBinding（多类型模式）
     * 默认调用 createBinding，单类型时不需要重写
     */
    fun createBindingForType(parent: ViewGroup, viewType: Int): ViewBinding {
        @Suppress("UNCHECKED_CAST")
        return createBinding(parent, viewType) as ViewBinding
    }
    
    /**
     * 绑定数据（单类型模式，必须实现）
     */
    fun bind(binding: VB, item: T, position: Int)
    
    /**
     * 绑定数据到 ViewBinding（多类型模式）
     * 默认调用 bind，单类型时不需要重写
     */
    fun bindToBinding(binding: ViewBinding, item: T, position: Int, viewType: Int) {
        @Suppress("UNCHECKED_CAST")
        bind(binding as VB, item, position)
    }
    
    /**
     * 使用 Payload 进行局部更新（可选）
     * 当 DiffUtil 检测到内容变化但 item 相同时，会调用此方法
     */
    fun bind(binding: VB, item: T, position: Int, payloads: List<Any>) {
        // 默认实现：如果有 payload，调用完整的 bind
        if (payloads.isNotEmpty()) {
            bind(binding, item, position)
        }
    }
    
    /**
     * 使用 Payload 进行局部更新（多类型模式）
     */
    fun bindToBinding(
        binding: ViewBinding,
        item: T,
        position: Int,
        viewType: Int,
        payloads: List<Any>
    ) {
        if (payloads.isEmpty()) {
            bindToBinding(binding, item, position, viewType)
        } else {
            @Suppress("UNCHECKED_CAST")
            bind(binding as VB, item, position, payloads)
        }
    }
    
    /**
     * 点击事件（可选）
     */
    fun onItemClick(binding: ViewBinding, item: T, position: Int) {}
    
    /**
     * 长按事件（可选）
     */
    fun onItemLongClick(binding: ViewBinding, item: T, position: Int): Boolean = false
    
    /**
     * ViewHolder 创建时的回调（可选）
     */
    fun onViewHolderCreated(holder: RecyclerView.ViewHolder, viewType: Int) {}
    
    /**
     * ViewHolder 绑定时的回调（可选）
     */
    fun onViewHolderBound(holder: RecyclerView.ViewHolder, position: Int) {}
}

