package com.jun.core.ui.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter

/**
 * Fragment 配置数据类
 * 用于标识和管理 Fragment 实例
 */
data class FragmentConfig(
    /**
     * Fragment 的唯一标识
     */
    val id: String,
    
    /**
     * Fragment 的标签（用于识别 Fragment 类型）
     */
    val tag: String,
    
    /**
     * 创建 Fragment 的工厂方法
     */
    val factory: () -> Fragment
) {
    /**
     * 检查两个配置是否相同（基于 tag）
     */
    fun isSameType(other: FragmentConfig?): Boolean {
        return other != null && this.tag == other.tag
    }
}

/**
 * ViewPager2 适配器基类
 * 使用 FragmentStateAdapter，适合大量页面
 * 
 * 特性：
 * - 自动管理 Fragment 生命周期
 * - 支持懒加载（配合 BaseFragment）
 * - 类型安全的 Fragment 创建
 * - 基于 RecyclerView，性能优异
 * 
 * 使用示例：
 * ```kotlin
 * class TabPagerAdapter(activity: FragmentActivity) : ViewPager2Adapter(activity) {
 *     override fun getItemCount(): Int = 3
 *     
 *     override fun createFragment(position: Int): Fragment {
 *         return when (position) {
 *             0 -> HomeFragment()
 *             1 -> CategoryFragment()
 *             2 -> ProfileFragment()
 *             else -> throw IllegalArgumentException("Invalid position: $position")
 *         }
 *     }
 * }
 * 
 * // 在 Activity 中使用
 * val adapter = TabPagerAdapter(this)
 * viewPager2.adapter = adapter
 * ```
 */
abstract class ViewPager2Adapter(
    fragmentActivity: FragmentActivity
) : FragmentStateAdapter(fragmentActivity) {
    
    /**
     * 创建指定位置的 Fragment
     * 子类必须实现此方法
     */
    abstract override fun createFragment(position: Int): Fragment
}

/**
 * ViewPager2 适配器基类（使用 Fragment）
 * 适用于在 Fragment 中使用 ViewPager2
 * 
 * 使用示例：
 * ```kotlin
 * class TabPagerAdapter(fragment: Fragment) : ViewPager2AdapterWithFragment(fragment) {
 *     override fun getItemCount(): Int = 3
 *     
 *     override fun createFragment(position: Int): Fragment {
 *         return when (position) {
 *             0 -> HomeFragment()
 *             1 -> CategoryFragment()
 *             2 -> ProfileFragment()
 *             else -> throw IllegalArgumentException("Invalid position: $position")
 *         }
 *     }
 * }
 * 
 * // 在 Fragment 中使用
 * val adapter = TabPagerAdapter(this)
 * viewPager2.adapter = adapter
 * ```
 */
abstract class ViewPager2AdapterWithFragment(
    fragment: Fragment
) : FragmentStateAdapter(fragment) {
    
    /**
     * 创建指定位置的 Fragment
     * 子类必须实现此方法
     */
    abstract override fun createFragment(position: Int): Fragment
}

/**
 * ViewPager2 适配器基类（使用 Lifecycle）
 * 适用于需要自定义生命周期管理的场景
 * 
 * 使用示例：
 * ```kotlin
 * class CustomPagerAdapter(
 *     fragmentManager: FragmentManager,
 *     lifecycle: Lifecycle
 * ) : ViewPager2AdapterWithLifecycle(fragmentManager, lifecycle) {
 *     override fun getItemCount(): Int = 3
 *     
 *     override fun createFragment(position: Int): Fragment {
 *         return when (position) {
 *             0 -> HomeFragment()
 *             1 -> CategoryFragment()
 *             2 -> ProfileFragment()
 *             else -> throw IllegalArgumentException("Invalid position: $position")
 *         }
 *     }
 * }
 * ```
 */
abstract class ViewPager2AdapterWithLifecycle(
    fragmentManager: FragmentManager,
    lifecycle: Lifecycle
) : FragmentStateAdapter(fragmentManager, lifecycle) {
    
    /**
     * 创建指定位置的 Fragment
     * 子类必须实现此方法
     */
    abstract override fun createFragment(position: Int): Fragment
}

/**
 * 动态 ViewPager2 适配器（Activity 中使用）
 * 支持动态添加、删除、替换 Fragment
 * 
 * 特性：
 * - 动态添加/删除 Fragment
 * - 批量替换 Fragment（智能处理相同类型）
 * - Fragment 实例复用（相同类型时）
 * - 自动管理 Fragment 生命周期
 * 
 * 使用示例：
 * ```kotlin
 * val adapter = DynamicViewPager2Adapter(this)
 * 
 * // 添加 Fragment
 * adapter.addFragment(FragmentConfig("home", "HomeFragment") { HomeFragment() })
 * adapter.addFragment(FragmentConfig("category", "CategoryFragment") { CategoryFragment() })
 * 
 * // 删除 Fragment
 * adapter.removeFragment(0)
 * 
 * // 替换所有 Fragment
 * adapter.replaceAll(listOf(
 *     FragmentConfig("home", "HomeFragment") { HomeFragment() },
 *     FragmentConfig("profile", "ProfileFragment") { ProfileFragment() }
 * ))
 * ```
 */
class DynamicViewPager2Adapter(
    fragmentActivity: FragmentActivity
) : FragmentStateAdapter(fragmentActivity) {
    
    private val _fragments = mutableListOf<FragmentConfig>()
    val fragments: List<FragmentConfig> get() = _fragments.toList()
    
    override fun getItemCount(): Int = _fragments.size
    
    /**
     * 获取指定位置的 Item ID
     * 使用 FragmentConfig 的 id 作为 itemId
     * 如果两个位置的 itemId 相同，FragmentStateAdapter 会自动复用 Fragment
     */
    override fun getItemId(position: Int): Long {
        return if (position >= 0 && position < _fragments.size) {
            _fragments[position].id.hashCode().toLong()
        } else {
            super.getItemId(position)
        }
    }
    
    /**
     * 检查两个位置的内容是否相同
     * 用于 FragmentStateAdapter 的 DiffUtil
     */
    override fun containsItem(itemId: Long): Boolean {
        return _fragments.any { it.id.hashCode().toLong() == itemId }
    }
    
    override fun createFragment(position: Int): Fragment {
        val config = _fragments[position]
        return config.factory()
    }
    
    /**
     * 添加 Fragment
     * 
     * @param config Fragment 配置
     * @param position 插入位置（默认为末尾）
     */
    fun addFragment(config: FragmentConfig, position: Int = -1) {
        val insertPosition = if (position < 0 || position > _fragments.size) {
            _fragments.size
        } else {
            position
        }
        
        _fragments.add(insertPosition, config)
        notifyItemInserted(insertPosition)
    }
    
    /**
     * 添加多个 Fragment
     * 
     * @param configs Fragment 配置列表
     * @param position 插入位置（默认为末尾）
     */
    fun addFragments(configs: List<FragmentConfig>, position: Int = -1) {
        val insertPosition = if (position < 0 || position > _fragments.size) {
            _fragments.size
        } else {
            position
        }
        
        _fragments.addAll(insertPosition, configs)
        notifyItemRangeInserted(insertPosition, configs.size)
    }
    
    /**
     * 删除 Fragment
     * 
     * @param position Fragment 位置
     * @return 删除的 Fragment 配置，如果位置无效返回 null
     */
    fun removeFragment(position: Int): FragmentConfig? {
        if (position < 0 || position >= _fragments.size) {
            return null
        }
        
        val removed = _fragments.removeAt(position)
        notifyItemRemoved(position)
        return removed
    }
    
    /**
     * 删除 Fragment（根据 ID）
     * 
     * @param id Fragment ID
     * @return 删除的 Fragment 配置，如果未找到返回 null
     */
    fun removeFragmentById(id: String): FragmentConfig? {
        val position = _fragments.indexOfFirst { it.id == id }
        return if (position >= 0) {
            removeFragment(position)
        } else {
            null
        }
    }
    
    /**
     * 删除 Fragment（根据 Tag）
     * 
     * @param tag Fragment Tag
     * @return 删除的 Fragment 配置列表
     */
    fun removeFragmentsByTag(tag: String): List<FragmentConfig> {
        val removed = mutableListOf<FragmentConfig>()
        var position = _fragments.size - 1
        
        while (position >= 0) {
            if (_fragments[position].tag == tag) {
                removed.add(0, _fragments.removeAt(position))
                notifyItemRemoved(position)
            } else {
                position--
            }
        }
        
        return removed
    }
    
    /**
     * 替换 Fragment
     * 
     * @param position Fragment 位置
     * @param config 新的 Fragment 配置
     * @return 被替换的 Fragment 配置，如果位置无效返回 null
     */
    fun replaceFragment(position: Int, config: FragmentConfig): FragmentConfig? {
        if (position < 0 || position >= _fragments.size) {
            return null
        }
        
        val oldConfig = _fragments[position]
        val oldItemId = getItemId(position)
        
        _fragments[position] = config
        val newItemId = getItemId(position)
        
        // 如果 itemId 改变，需要通知移除和插入
        if (oldItemId != newItemId) {
            notifyItemRemoved(position)
            notifyItemInserted(position)
        } else {
            notifyItemChanged(position)
        }
        
        return oldConfig
    }
    
    /**
     * 替换所有 Fragment
     * 智能处理相同类型的 Fragment，通过 getItemId 复用实例
     * 
     * @param newConfigs 新的 Fragment 配置列表
     * @param reuseSameType 是否复用相同类型的 Fragment（默认 true）
     * 
     * 复用机制：
     * - 如果 reuseSameType = true：
     *   1. 对于每个新配置，查找旧配置中相同 tag 的 Fragment
     *   2. 如果找到，使用旧的 id（这样 getItemId 相同，会复用 Fragment 实例）
     *   3. 如果没找到，使用新的 id（创建新 Fragment 实例）
     * - 如果 reuseSameType = false：
     *   直接使用新配置的 id，不进行复用匹配
     * 
     * 注意：
     * - Fragment 复用基于 getItemId，如果 id 相同，FragmentStateAdapter 会自动复用
     * - 相同 tag 但不同 id 的 Fragment 不会复用（除非 reuseSameType = true 且自动匹配）
     */
    fun replaceAll(newConfigs: List<FragmentConfig>, reuseSameType: Boolean = true) {
        if (newConfigs.isEmpty()) {
            clear()
            return
        }
        
        val oldConfigs = _fragments.toList()
        val usedOldIds = mutableSetOf<String>()
        
        if (reuseSameType) {
            // 智能替换：尝试保持相同 tag 的 Fragment 复用
            val optimizedConfigs = newConfigs.map { newConfig ->
                // 查找旧配置中相同 tag 且未使用的 Fragment
                val oldConfig = oldConfigs.find { 
                    it.tag == newConfig.tag && it.id !in usedOldIds 
                }
                if (oldConfig != null) {
                    // 找到相同类型的 Fragment，使用旧的 id 以复用实例
                    usedOldIds.add(oldConfig.id)
                    newConfig.copy(id = oldConfig.id)
                } else {
                    // 没找到，使用新的 id
                    newConfig
                }
            }
            
            _fragments.clear()
            _fragments.addAll(optimizedConfigs)
        } else {
            // 不复用，直接替换
            _fragments.clear()
            _fragments.addAll(newConfigs)
        }
        
        // 使用 notifyDataSetChanged 来确保正确更新
        // FragmentStateAdapter 会根据 getItemId 来决定是否复用 Fragment
        notifyDataSetChanged()
    }
    
    /**
     * 清空所有 Fragment
     */
    fun clear() {
        val size = _fragments.size
        _fragments.clear()
        notifyItemRangeRemoved(0, size)
    }
    
    /**
     * 移动 Fragment 位置
     * 
     * @param fromPosition 原位置
     * @param toPosition 目标位置
     */
    fun moveFragment(fromPosition: Int, toPosition: Int): Boolean {
        if (fromPosition < 0 || fromPosition >= _fragments.size ||
            toPosition < 0 || toPosition >= _fragments.size ||
            fromPosition == toPosition) {
            return false
        }
        
        val config = _fragments.removeAt(fromPosition)
        _fragments.add(toPosition, config)
        notifyItemMoved(fromPosition, toPosition)
        return true
    }
    
    /**
     * 获取指定位置的 Fragment 配置
     */
    fun getFragmentConfig(position: Int): FragmentConfig? {
        return if (position >= 0 && position < _fragments.size) {
            _fragments[position]
        } else {
            null
        }
    }
    
    /**
     * 获取指定 ID 的 Fragment 配置
     */
    fun getFragmentConfigById(id: String): FragmentConfig? {
        return _fragments.find { it.id == id }
    }
    
    /**
     * 获取指定位置的索引
     */
    fun getPositionById(id: String): Int {
        return _fragments.indexOfFirst { it.id == id }
    }
    
    /**
     * 检查是否包含指定 ID 的 Fragment
     */
    fun contains(id: String): Boolean {
        return _fragments.any { it.id == id }
    }
}

/**
 * 动态 ViewPager2 适配器（Fragment 中使用）
 * 支持动态添加、删除、替换 Fragment
 */
class DynamicViewPager2AdapterWithFragment(
    fragment: Fragment
) : FragmentStateAdapter(fragment) {
    
    private val _fragments = mutableListOf<FragmentConfig>()
    val fragments: List<FragmentConfig> get() = _fragments.toList()
    
    override fun getItemCount(): Int = _fragments.size
    
    override fun getItemId(position: Int): Long {
        return if (position >= 0 && position < _fragments.size) {
            _fragments[position].id.hashCode().toLong()
        } else {
            super.getItemId(position)
        }
    }
    
    override fun containsItem(itemId: Long): Boolean {
        return _fragments.any { it.id.hashCode().toLong() == itemId }
    }
    
    override fun createFragment(position: Int): Fragment {
        val config = _fragments[position]
        return config.factory()
    }
    
    fun addFragment(config: FragmentConfig, position: Int = -1) {
        val insertPosition = if (position < 0 || position > _fragments.size) {
            _fragments.size
        } else {
            position
        }
        
        _fragments.add(insertPosition, config)
        notifyItemInserted(insertPosition)
    }
    
    fun addFragments(configs: List<FragmentConfig>, position: Int = -1) {
        val insertPosition = if (position < 0 || position > _fragments.size) {
            _fragments.size
        } else {
            position
        }
        
        _fragments.addAll(insertPosition, configs)
        notifyItemRangeInserted(insertPosition, configs.size)
    }
    
    fun removeFragment(position: Int): FragmentConfig? {
        if (position < 0 || position >= _fragments.size) {
            return null
        }
        
        val removed = _fragments.removeAt(position)
        notifyItemRemoved(position)
        return removed
    }
    
    fun removeFragmentById(id: String): FragmentConfig? {
        val position = _fragments.indexOfFirst { it.id == id }
        return if (position >= 0) {
            removeFragment(position)
        } else {
            null
        }
    }
    
    fun removeFragmentsByTag(tag: String): List<FragmentConfig> {
        val removed = mutableListOf<FragmentConfig>()
        var position = _fragments.size - 1
        
        while (position >= 0) {
            if (_fragments[position].tag == tag) {
                removed.add(0, _fragments.removeAt(position))
                notifyItemRemoved(position)
            } else {
                position--
            }
        }
        
        return removed
    }
    
    fun replaceFragment(position: Int, config: FragmentConfig): FragmentConfig? {
        if (position < 0 || position >= _fragments.size) {
            return null
        }
        
        val oldConfig = _fragments[position]
        val oldItemId = getItemId(position)
        
        _fragments[position] = config
        val newItemId = getItemId(position)
        
        if (oldItemId != newItemId) {
            notifyItemRemoved(position)
            notifyItemInserted(position)
        } else {
            notifyItemChanged(position)
        }
        
        return oldConfig
    }
    
    fun replaceAll(newConfigs: List<FragmentConfig>, reuseSameType: Boolean = true) {
        if (newConfigs.isEmpty()) {
            clear()
            return
        }
        
        val oldConfigs = _fragments.toList()
        val usedOldIds = mutableSetOf<String>()
        
        if (reuseSameType) {
            val optimizedConfigs = newConfigs.map { newConfig ->
                val oldConfig = oldConfigs.find { 
                    it.tag == newConfig.tag && it.id !in usedOldIds 
                }
                if (oldConfig != null) {
                    usedOldIds.add(oldConfig.id)
                    newConfig.copy(id = oldConfig.id)
                } else {
                    newConfig
                }
            }
            
            _fragments.clear()
            _fragments.addAll(optimizedConfigs)
        } else {
            _fragments.clear()
            _fragments.addAll(newConfigs)
        }
        
        notifyDataSetChanged()
    }
    
    fun clear() {
        val size = _fragments.size
        _fragments.clear()
        notifyItemRangeRemoved(0, size)
    }
    
    fun moveFragment(fromPosition: Int, toPosition: Int): Boolean {
        if (fromPosition < 0 || fromPosition >= _fragments.size ||
            toPosition < 0 || toPosition >= _fragments.size ||
            fromPosition == toPosition) {
            return false
        }
        
        val config = _fragments.removeAt(fromPosition)
        _fragments.add(toPosition, config)
        notifyItemMoved(fromPosition, toPosition)
        return true
    }
    
    fun getFragmentConfig(position: Int): FragmentConfig? {
        return if (position >= 0 && position < _fragments.size) {
            _fragments[position]
        } else {
            null
        }
    }
    
    fun getFragmentConfigById(id: String): FragmentConfig? {
        return _fragments.find { it.id == id }
    }
    
    fun getPositionById(id: String): Int {
        return _fragments.indexOfFirst { it.id == id }
    }
    
    fun contains(id: String): Boolean {
        return _fragments.any { it.id == id }
    }
}

/**
 * Fragment 工厂接口
 * 用于创建 Fragment 实例
 */
fun interface FragmentFactory {
    /**
     * 创建 Fragment
     * 
     * @param position Fragment 位置
     * @return Fragment 实例
     */
    fun create(position: Int): Fragment
}

/**
 * 基于 FragmentFactory 的 ViewPager2 适配器（Activity 中使用）
 * 适用于简单的 Fragment 创建场景
 * 
 * 使用示例：
 * ```kotlin
 * val fragments = listOf(
 *     HomeFragment(),
 *     CategoryFragment(),
 *     ProfileFragment()
 * )
 * 
 * val adapter = SimpleViewPager2Adapter(this, fragments.size) { position ->
 *     fragments[position]
 * }
 * viewPager2.adapter = adapter
 * ```
 */
class SimpleViewPager2Adapter(
    fragmentActivity: FragmentActivity,
    private val itemCount: Int,
    private val fragmentFactory: FragmentFactory
) : ViewPager2Adapter(fragmentActivity) {
    
    override fun getItemCount(): Int = itemCount
    
    override fun createFragment(position: Int): Fragment {
        return fragmentFactory.create(position)
    }
}

/**
 * 基于 FragmentFactory 的 ViewPager2 适配器（Fragment 中使用）
 * 适用于简单的 Fragment 创建场景
 * 
 * 使用示例：
 * ```kotlin
 * val adapter = SimpleViewPager2AdapterWithFragment(this, 3) { position ->
 *     when (position) {
 *         0 -> HomeFragment()
 *         1 -> CategoryFragment()
 *         2 -> ProfileFragment()
 *         else -> throw IllegalArgumentException("Invalid position: $position")
 *     }
 * }
 * viewPager2.adapter = adapter
 * ```
 */
class SimpleViewPager2AdapterWithFragment(
    fragment: Fragment,
    private val itemCount: Int,
    private val fragmentFactory: FragmentFactory
) : ViewPager2AdapterWithFragment(fragment) {
    
    override fun getItemCount(): Int = itemCount
    
    override fun createFragment(position: Int): Fragment {
        return fragmentFactory.create(position)
    }
}
