# BaseAdapter 优化指南

## 📋 优化内容

`BaseAdapter.kt` 已进行多项优化，提升性能和易用性。

---

## ✨ 优化点详解

### 1. 点击事件防抖 ⭐

**问题**：快速连续点击可能导致重复触发事件。

**优化**：添加了点击防抖机制，防止短时间内重复点击。

```kotlin
class UserAdapter : BaseAdapter<User, ItemUserBinding>(diffCallback) {
    
    init {
        // 设置点击防抖时间（毫秒）
        setClickDebounceTime(500)
        
        // 设置协程作用域（可选，用于协程防抖）
        // setClickScope(viewModelScope)
    }
    
    // ... 其他代码
}
```

**功能：**
- ✅ 时间间隔防抖（默认 500ms）
- ✅ 协程防抖（可选，更精确）
- ✅ 自动清理防抖任务

### 2. 避免重复设置监听器 ⭐

**问题**：每次 `onBindViewHolder` 都会重新设置点击监听器，造成不必要的开销。

**优化**：监听器只设置一次，避免重复设置。

```kotlin
// 内部实现：使用 isClickListenersSetup 标志
// 只在第一次绑定时设置监听器
```

**效果：**
- ✅ 减少不必要的对象创建
- ✅ 提升绑定性能
- ✅ 避免内存泄漏风险

### 3. Payload 局部更新 ⭐

**问题**：数据变化时，整个 ViewHolder 都会重新绑定，即使只有部分内容变化。

**优化**：支持使用 Payload 进行局部更新。

```kotlin
class UserAdapter : BaseAdapter<User, ItemUserBinding>(diffCallback) {
    
    override fun bind(binding: ItemUserBinding, item: User, position: Int) {
        binding.tvUserName.text = item.name
        binding.tvUserEmail.text = item.email
    }
    
    // 重写此方法实现局部更新
    override fun bind(
        binding: ItemUserBinding,
        item: User,
        position: Int,
        payloads: List<Any>
    ) {
        if (payloads.isEmpty()) {
            // 没有 payload，完整绑定
            bind(binding, item, position)
        } else {
            // 根据 payload 进行局部更新
            payloads.forEach { payload ->
                when (payload) {
                    "name" -> binding.tvUserName.text = item.name
                    "email" -> binding.tvUserEmail.text = item.email
                }
            }
        }
    }
}
```

**使用场景：**
- 只更新部分 UI（如点赞数、评论数）
- 减少不必要的视图更新
- 提升列表滚动性能

### 4. 安全的数据访问 ⭐

**优化**：添加了安全的数据访问方法。

```kotlin
// 安全获取指定位置的数据
val item = adapter.getItemOrNull(5)

// 获取第一个数据
val first = adapter.getFirstItemOrNull()

// 获取最后一个数据
val last = adapter.getLastItemOrNull()

// 检查是否为空
if (adapter.isEmpty()) {
    // 显示空状态
}

// 检查是否不为空
if (adapter.isNotEmpty()) {
    // 显示数据
}
```

### 5. ViewHolder 生命周期回调 ⭐

**优化**：添加了 ViewHolder 创建和绑定的回调。

```kotlin
class UserAdapter : BaseAdapter<User, ItemUserBinding>(diffCallback) {
    
    // ViewHolder 创建时的回调
    override fun onViewHolderCreated(holder: BaseViewHolder, viewType: Int) {
        // 可以在这里进行一些初始化操作
        // 例如：设置动画、初始化状态等
    }
    
    // ViewHolder 绑定时的回调
    override fun onViewHolderBound(holder: BaseViewHolder, position: Int) {
        // 可以在这里进行一些绑定后的操作
        // 例如：启动动画、更新状态等
    }
}
```

### 6. 安全的适配器位置 ⭐

**优化**：ViewHolder 中提供了安全的适配器位置获取方法。

```kotlin
// 在 ViewHolder 中
val position = getAdapterPositionSafe()
// 如果位置无效，返回 RecyclerView.NO_POSITION
```

---

## 🎯 使用示例

### 基础使用

```kotlin
class UserAdapter : BaseAdapter<User, ItemUserBinding>(
    object : DiffUtil.ItemCallback<User>() {
        override fun areItemsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem == newItem
        }
    }
) {
    
    override fun createBinding(parent: ViewGroup, viewType: Int): ItemUserBinding {
        return ItemUserBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
    }
    
    override fun bind(binding: ItemUserBinding, item: User, position: Int) {
        binding.tvUserName.text = item.name
        binding.tvUserEmail.text = item.email
    }
    
    override fun onItemClick(binding: ItemUserBinding, item: User, position: Int) {
        // 处理点击事件
    }
}
```

### 带防抖的使用

```kotlin
class UserAdapter : BaseAdapter<User, ItemUserBinding>(diffCallback) {
    
    init {
        // 设置防抖时间
        setClickDebounceTime(300)
    }
    
    // ... 其他代码
}
```

### 带 Payload 局部更新

```kotlin
class UserAdapter : BaseAdapter<User, ItemUserBinding>(diffCallback) {
    
    override fun bind(binding: ItemUserBinding, item: User, position: Int) {
        binding.apply {
            tvUserName.text = item.name
            tvUserEmail.text = item.email
            tvLikeCount.text = "${item.likeCount}"
        }
    }
    
    override fun bind(
        binding: ItemUserBinding,
        item: User,
        position: Int,
        payloads: List<Any>
    ) {
        if (payloads.isEmpty()) {
            bind(binding, item, position)
        } else {
            // 只更新变化的部分
            payloads.forEach { payload ->
                when (payload) {
                    "like" -> binding.tvLikeCount.text = "${item.likeCount}"
                    "name" -> binding.tvUserName.text = item.name
                    else -> bind(binding, item, position)
                }
            }
        }
    }
}

// 使用 Payload 更新
adapter.notifyItemChanged(position, "like")
```

---

## 📊 性能优化效果

### 1. 点击防抖
- **减少无效点击**：防止快速连续点击导致的重复操作
- **提升用户体验**：避免因误触导致的问题

### 2. 避免重复设置监听器
- **减少对象创建**：每次绑定不再创建新的监听器对象
- **提升绑定性能**：减少不必要的操作

### 3. Payload 局部更新
- **减少视图更新**：只更新变化的部分
- **提升滚动性能**：减少不必要的重绘
- **节省资源**：减少 CPU 和内存使用

### 4. 安全的数据访问
- **避免崩溃**：防止索引越界导致的崩溃
- **更好的错误处理**：提供安全的访问方法

---

## 🔧 最佳实践

### 1. 使用 Payload 进行局部更新

```kotlin
// 在 DiffUtil.ItemCallback 中
override fun getChangePayload(oldItem: User, newItem: User): Any? {
    return when {
        oldItem.likeCount != newItem.likeCount -> "like"
        oldItem.name != newItem.name -> "name"
        else -> null
    }
}

// 在 Adapter 中处理 Payload
override fun bind(binding: ItemUserBinding, item: User, position: Int, payloads: List<Any>) {
    if (payloads.isEmpty()) {
        bind(binding, item, position)
    } else {
        payloads.forEach { payload ->
            when (payload) {
                "like" -> binding.tvLikeCount.text = "${item.likeCount}"
                "name" -> binding.tvUserName.text = item.name
            }
        }
    }
}
```

### 2. 合理设置防抖时间

```kotlin
// 列表项点击：300-500ms
setClickDebounceTime(300)

// 按钮点击：500-1000ms
setClickDebounceTime(500)
```

### 3. 使用安全的数据访问

```kotlin
// 推荐：使用安全方法
val item = adapter.getItemOrNull(position)
item?.let { /* 处理数据 */ }

// 不推荐：直接使用 getItem
try {
    val item = adapter.getItem(position)
} catch (e: Exception) {
    // 处理异常
}
```

---

## 🆕 多类型支持

### BaseAdapter 现在支持多类型 ViewHolder

`BaseAdapter` 现在内置支持多类型 ViewHolder，无需使用单独的 `MultiTypeAdapter`。

**单类型模式（默认，向后兼容）：**
```kotlin
class UserAdapter : BaseAdapter<User, ItemUserBinding>(diffCallback) {
    // 单类型，所有 item 使用相同的 ViewBinding
    override fun createBinding(parent: ViewGroup, viewType: Int): ItemUserBinding {
        return ItemUserBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    }
    
    override fun bind(binding: ItemUserBinding, item: User, position: Int) {
        binding.tvUserName.text = item.name
    }
}
```

**多类型模式：**
```kotlin
sealed class ListItem {
    data class Header(val title: String) : ListItem()
    data class User(val user: User) : ListItem()
    data class Footer(val text: String) : ListItem()
}

class MyMultiTypeAdapter : BaseAdapter<ListItem, ViewBinding>(
    object : DiffUtil.ItemCallback<ListItem>() {
        override fun areItemsTheSame(oldItem: ListItem, newItem: ListItem): Boolean {
            return when {
                oldItem is ListItem.Header && newItem is ListItem.Header -> true
                oldItem is ListItem.User && newItem is ListItem.User -> 
                    oldItem.user.id == newItem.user.id
                oldItem is ListItem.Footer && newItem is ListItem.Footer -> true
                else -> false
            }
        }
        
        override fun areContentsTheSame(oldItem: ListItem, newItem: ListItem): Boolean {
            return oldItem == newItem
        }
    }
) {
    // 1. 重写 getItemViewType 返回不同的 viewType
    override fun getItemViewType(item: ListItem, position: Int): Int {
        return when (item) {
            is ListItem.Header -> 0
            is ListItem.User -> 1
            is ListItem.Footer -> 2
        }
    }
    
    // 2. 重写 createBindingForType 创建不同类型的 ViewBinding
    override fun createBindingForType(parent: ViewGroup, viewType: Int): ViewBinding {
        return when (viewType) {
            0 -> ItemHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            1 -> ItemUserBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            2 -> ItemFooterBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            else -> throw IllegalArgumentException("Unknown viewType: $viewType")
        }
    }
    
    // 3. 必须实现 createBinding（即使不使用）
    override fun createBinding(parent: ViewGroup, viewType: Int): ViewBinding {
        return createBindingForType(parent, viewType)
    }
    
    // 4. 重写 bindToBinding 根据 viewType 进行不同的绑定
    override fun bindToBinding(binding: ViewBinding, item: ListItem, position: Int, viewType: Int) {
        when (viewType) {
            0 -> {
                val headerBinding = binding as ItemHeaderBinding
                val headerItem = item as ListItem.Header
                headerBinding.tvTitle.text = headerItem.title
            }
            1 -> {
                val userBinding = binding as ItemUserBinding
                val userItem = item as ListItem.User
                userBinding.tvUserName.text = userItem.user.name
            }
            2 -> {
                val footerBinding = binding as ItemFooterBinding
                val footerItem = item as ListItem.Footer
                footerBinding.tvText.text = footerItem.text
            }
        }
    }
    
    // 5. 必须实现 bind（即使不使用）
    override fun bind(binding: ViewBinding, item: ListItem, position: Int) {
        bindToBinding(binding, item, position, getItemViewType(item, position))
    }
}
```

---

## ✨ 总结

BaseAdapter 现在提供了：

- ✅ **点击防抖**：防止重复点击
- ✅ **性能优化**：避免重复设置监听器
- ✅ **Payload 支持**：局部更新，提升性能
- ✅ **安全访问**：防止索引越界
- ✅ **生命周期回调**：更好的扩展性
- ✅ **多类型支持**：内置支持多类型 ViewHolder（已合并 MultiTypeAdapter 功能）

**建议：根据实际需求使用这些优化功能！**

