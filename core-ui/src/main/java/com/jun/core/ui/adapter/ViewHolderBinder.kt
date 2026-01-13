package com.jun.core.ui.adapter

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding

/**
 * ViewHolder 绑定器
 * 处理 ViewHolder 的创建和绑定逻辑
 */
class ViewHolderBinder<T, VB : ViewBinding>(
    private val callback: AdapterCallback<T, VB>,
    private val clickDebounceManager: ClickDebounceManager
) {
    
    /**
     * 创建 ViewHolder
     */
    fun createViewHolder(
        parent: ViewGroup,
        viewType: Int,
        adapter: BaseAdapter<T, VB>
    ): BaseAdapter<T, VB>.BaseViewHolder {
        val binding = callback.createBindingForType(parent, viewType)
        val holder = adapter.BaseViewHolder(binding, viewType)
        callback.onViewHolderCreated(holder, viewType)
        return holder
    }
    
    /**
     * 绑定 ViewHolder（普通绑定）
     */
    fun bindViewHolder(
        holder: BaseAdapter<T, VB>.BaseViewHolder,
        item: T,
        position: Int,
        adapter: BaseAdapter<T, VB>
    ) {
        holder.bind(item, position, callback, clickDebounceManager, adapter)
        callback.onViewHolderBound(holder, position)
    }
    
    /**
     * 绑定 ViewHolder（带 Payload）
     */
    fun bindViewHolder(
        holder: BaseAdapter<T, VB>.BaseViewHolder,
        item: T,
        position: Int,
        payloads: List<Any>,
        adapter: BaseAdapter<T, VB>
    ) {
        if (payloads.isEmpty()) {
            bindViewHolder(holder, item, position, adapter)
        } else {
            holder.bindWithPayload(item, position, payloads, callback, clickDebounceManager, adapter)
            callback.onViewHolderBound(holder, position)
        }
    }
}

