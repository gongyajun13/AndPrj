package com.jun.core.ui.adapter

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import kotlinx.coroutines.CoroutineScope

/**
 * 基础 RecyclerView Adapter
 * 使用 ViewBinding 和 DiffUtil
 * 
 * 支持单类型和多类型 ViewHolder：
 * - 单类型：默认模式，所有 item 使用相同的 ViewBinding 类型（向后兼容）
 * - 多类型：重写 getItemViewType()、createBindingForType() 和 bindToBinding() 即可支持多类型
 * 
 * 优化点：
 * 1. 点击事件防抖
 * 2. 避免重复设置监听器
 * 3. 支持 Payload 局部更新
 * 4. 支持多类型 ViewHolder（可选）
 * 5. 更好的性能优化
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
abstract class BaseAdapter<T, VB : ViewBinding>(
    diffCallback: DiffUtil.ItemCallback<T>? = null
) : ListAdapter<T, BaseAdapter<T, VB>.BaseViewHolder>(
    diffCallback ?: object : DiffUtil.ItemCallback<T>() {
        override fun areItemsTheSame(
            oldItem: T & Any,
            newItem: T & Any,
        ): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(
            oldItem: T & Any,
            newItem: T & Any,
        ): Boolean {
            return oldItem == newItem
        }
    }
), AdapterCallback<T, VB> {
    
    private val clickDebounceManager = ClickDebounceManager()
    private val viewHolderBinder = ViewHolderBinder(this, clickDebounceManager)
    
    /**
     * 设置点击防抖时间
     */
    fun setClickDebounceTime(timeMillis: Long) {
        clickDebounceManager.setDebounceTime(timeMillis)
    }
    
    /**
     * 设置点击防抖的协程作用域
     */
    fun setClickScope(scope: CoroutineScope) {
        clickDebounceManager.setScope(scope)
    }
    
    override fun getItemViewType(position: Int): Int {
        return getItemViewType(getItem(position), position)
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        return viewHolderBinder.createViewHolder(parent, viewType, this)
    }
    
    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        val item = getItem(position)
        viewHolderBinder.bindViewHolder(holder, item, position, this)
    }
    
    override fun onBindViewHolder(
        holder: BaseViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        val item = getItem(position)
        viewHolderBinder.bindViewHolder(holder, item, position, payloads, this)
    }
    
    /**
     * 获取指定位置的数据（安全）
     */
    fun getItemOrNull(position: Int): T? {
        return if (position in 0 until itemCount) {
            getItem(position)
        } else {
            null
        }
    }
    
    /**
     * 获取第一个数据
     */
    fun getFirstItemOrNull(): T? {
        return if (itemCount > 0) {
            getItem(0)
        } else {
            null
        }
    }
    
    /**
     * 获取最后一个数据
     */
    fun getLastItemOrNull(): T? {
        return if (itemCount > 0) {
            getItem(itemCount - 1)
        } else {
            null
        }
    }
    
    /**
     * 检查是否为空
     */
    fun isEmpty(): Boolean = itemCount == 0
    
    /**
     * 检查是否不为空
     */
    fun isNotEmpty(): Boolean = itemCount > 0
    
    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        clickDebounceManager.clear()
    }
    
    inner class BaseViewHolder(
        val binding: ViewBinding,
        val viewType: Int
    ) : RecyclerView.ViewHolder(binding.root) {
        private var isClickListenersSetup = false
        
        fun bind(
            item: T,
            position: Int,
            callback: AdapterCallback<T, VB>,
            clickDebounceManager: ClickDebounceManager,
            adapter: BaseAdapter<T, VB>
        ) {
            callback.bindToBinding(binding, item, position, viewType)
            
            if (!isClickListenersSetup) {
                setupClickListeners(callback, clickDebounceManager, adapter)
                isClickListenersSetup = true
            }
        }
        
        private fun setupClickListeners(
            callback: AdapterCallback<T, VB>,
            clickDebounceManager: ClickDebounceManager,
            adapter: BaseAdapter<T, VB>
        ) {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position == RecyclerView.NO_POSITION) return@setOnClickListener
                
                val item = adapter.getItemOrNull(position) ?: return@setOnClickListener
                
                clickDebounceManager.handleClick(position) {
                    callback.onItemClick(binding, item, position)
                }
            }
            
            binding.root.setOnLongClickListener {
                val position = bindingAdapterPosition
                if (position == RecyclerView.NO_POSITION) return@setOnLongClickListener false
                
                val item = adapter.getItemOrNull(position) ?: return@setOnLongClickListener false
                callback.onItemLongClick(binding, item, position)
            }
        }
        
        fun bindWithPayload(
            item: T,
            position: Int,
            payloads: List<Any>,
            callback: AdapterCallback<T, VB>,
            clickDebounceManager: ClickDebounceManager,
            adapter: BaseAdapter<T, VB>
        ) {
            callback.bindToBinding(binding, item, position, viewType, payloads)
            
            if (!isClickListenersSetup) {
                setupClickListeners(callback, clickDebounceManager, adapter)
                isClickListenersSetup = true
            }
        }
        
        /**
         * 获取安全的适配器位置
         */
        fun getAdapterPositionSafe(): Int {
            return try {
                bindingAdapterPosition
            } catch (e: Exception) {
                RecyclerView.NO_POSITION
            }
        }
        
        /**
         * 获取 ViewBinding（类型安全，仅单类型时使用）
         */
        @Suppress("UNCHECKED_CAST")
        fun getTypedBinding(): VB {
            return binding as VB
        }
    }
}
