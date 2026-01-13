# DialogQueueManager 使用指南

## 📋 概述

`DialogQueueManager` 是一个强大的 Dialog 队列管理工具类，用于管理多个 Dialog 的展示顺序，确保同一时间只展示一个 Dialog。

**核心功能**：
- ✅ **单实例展示**：同一时间只展示一个 Dialog
- ✅ **队列管理**：多个 Dialog 自动排队，依次展示
- ✅ **优先级控制**：支持优先级，高优先级优先展示
- ✅ **插队功能**：支持高优先级 Dialog 插队，可打断当前 Dialog
- ✅ **去重机制**：相同 tag 的 Dialog 只保留优先级最高的
- ✅ **状态监听**：支持监听队列状态变化
- ✅ **安全操作**：自动使用 BaseDialog 的安全显示/隐藏方法
- ✅ **资源管理**：自动处理生命周期，防止内存泄漏

---

## ✨ 核心功能

### 1. 基本使用

```kotlin
class MainActivity : BaseActivity<ActivityMainBinding>() {

    private val dialogQueue by lazy {
        DialogQueueManager(supportFragmentManager)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 显示多个 Dialog，自动排队
        showDialogs()
    }

    private fun showDialogs() {
        // 普通优先级
        dialogQueue.enqueue(
            dialog = CustomDialog1(),
            priority = 0
        )

        // 更高优先级，会优先展示
        dialogQueue.enqueue(
            dialog = CustomDialog2(),
            priority = 10
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        dialogQueue.release() // 释放资源
    }
}
```

### 2. 优先级控制

```kotlin
// 优先级越高，越先展示
dialogQueue.enqueue(CustomDialog1(), priority = 0)   // 最低优先级
dialogQueue.enqueue(CustomDialog2(), priority = 50)  // 中等优先级
dialogQueue.enqueue(CustomDialog3(), priority = 100)  // 最高优先级

// 展示顺序：CustomDialog3 -> CustomDialog2 -> CustomDialog1
```

**优先级规则**：
- 数值越大，优先级越高
- 优先级相同时，先入队的先展示（FIFO）

### 3. 插队功能

```kotlin
// 插队：高优先级 Dialog 可以插到队列前面
dialogQueue.enqueueAtFront(
    dialog = HighPriorityDialog(),
    priority = 100,
    interruptCurrent = false  // 不打断当前 Dialog，等当前关闭后优先展示
)

// 打断当前：立即关闭当前 Dialog，优先展示新 Dialog
dialogQueue.enqueueAtFront(
    dialog = UrgentDialog(),
    priority = 200,
    interruptCurrent = true  // 打断当前 Dialog，立即展示
)
```

### 4. 去重机制

```kotlin
// 默认启用去重，相同 tag 的 Dialog 只保留优先级最高的
dialogQueue.enqueue(CustomDialog(), priority = 10, tag = "custom")
dialogQueue.enqueue(CustomDialog(), priority = 50, tag = "custom")  // 替换上面的

// 禁用去重
dialogQueue.setEnableDeduplication(false)
```

### 5. 队列状态监听

```kotlin
dialogQueue.setOnQueueStateListener { hasCurrent, pendingCount ->
    if (hasCurrent) {
        // 当前有 Dialog 正在显示
    }
    if (pendingCount > 0) {
        // 队列中还有 $pendingCount 个 Dialog 等待展示
    }
}
```

### 6. 移除操作

```kotlin
// 从队列中移除指定 tag 的 Dialog
dialogQueue.removeByTag("custom")

// 从队列中移除指定 Dialog 实例
dialogQueue.remove(customDialog)

// 清空队列（可选：同时关闭当前 Dialog）
dialogQueue.clear(dismissCurrent = false)  // 只清空队列
dialogQueue.clear(dismissCurrent = true)   // 清空队列并关闭当前 Dialog
```

### 7. 状态查询

```kotlin
// 检查队列状态
if (dialogQueue.hasPending()) {
    val count = dialogQueue.getPendingCount()
    println("队列中还有 $count 个 Dialog")
}

if (dialogQueue.hasCurrent()) {
    val current = dialogQueue.getCurrentDialog()
    val tag = dialogQueue.getCurrentTag()
    println("当前正在显示: $tag")
}

// 检查队列中是否包含指定 tag
if (dialogQueue.containsTag("custom")) {
    println("队列中包含 custom Dialog")
}
```

---

## 📝 完整示例

### 示例 1：普通队列管理

```kotlin
class MainActivity : BaseActivity<ActivityMainBinding>() {

    private val dialogQueue by lazy {
        DialogQueueManager(supportFragmentManager).apply {
            setOnQueueStateListener { hasCurrent, pendingCount ->
                Timber.d("队列状态: 当前显示=$hasCurrent, 等待=$pendingCount")
            }
        }
    }

    private fun showUserDialogs() {
        // 显示用户信息 Dialog
        dialogQueue.enqueue(
            dialog = UserInfoDialog.newInstance(userId = "123"),
            priority = 5,
            tag = "user_info"
        )

        // 显示确认 Dialog
        dialogQueue.enqueue(
            dialog = ConfirmDialog.newInstance("确认操作"),
            priority = 10,
            tag = "confirm"
        )
    }
}
```

### 示例 2：高优先级插队

```kotlin
class MainActivity : BaseActivity<ActivityMainBinding>() {

    private val dialogQueue by lazy {
        DialogQueueManager(supportFragmentManager)
    }

    private fun handleTokenExpired() {
        // Token 过期，需要立即弹出登录 Dialog
        dialogQueue.enqueueAtFront(
            dialog = LoginDialog(),
            priority = 1000,  // 非常高的优先级
            interruptCurrent = true  // 打断当前 Dialog，立即显示
        )
    }

    private fun handleForceUpdate() {
        // 强制更新，需要立即弹出更新 Dialog
        dialogQueue.enqueueAtFront(
            dialog = UpdateDialog(),
            priority = 999,
            interruptCurrent = true
        )
    }
}
```

### 示例 3：去重和移除

```kotlin
class MainActivity : BaseActivity<ActivityMainBinding>() {

    private val dialogQueue by lazy {
        DialogQueueManager(supportFragmentManager)
    }

    private fun showNotification() {
        // 显示通知 Dialog（可能多次调用）
        dialogQueue.enqueue(
            dialog = NotificationDialog.newInstance(message = "新消息"),
            priority = 20,
            tag = "notification"  // 相同 tag 会去重
        )
    }

    private fun cancelNotification() {
        // 取消通知 Dialog
        dialogQueue.removeByTag("notification")
    }

    private fun clearAllDialogs() {
        // 清空所有 Dialog（包括当前显示的）
        dialogQueue.clear(dismissCurrent = true)
    }
}
```

### 示例 4：结合 BaseDialog 使用

```kotlin
class UserInfoDialog : BaseDialog<DialogUserInfoBinding>() {

    override val dialogWidth: Int = WindowManager.LayoutParams.MATCH_PARENT
    override val cancelable: Boolean = false

    override fun createBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): DialogUserInfoBinding {
        return DialogUserInfoBinding.inflate(inflater, container, false)
    }

    override fun setupViews() {
        binding.title.text = "用户信息"
    }

    override fun setupListeners() {
        binding.confirmButton.setOnClickListener {
            dismissWithResult("确认")
        }
    }

    companion object {
        fun newInstance(userId: String): UserInfoDialog {
            return UserInfoDialog().apply {
                arguments = Bundle().apply {
                    putString("userId", userId)
                }
            }
        }
    }
}

// 使用队列管理
dialogQueue.enqueue(
    dialog = UserInfoDialog.newInstance("123"),
    priority = 10
)
```

---

## 🎯 最佳实践

### 1. 在 Activity 中管理

```kotlin
// ✅ 推荐：在 Activity 中创建实例
class MainActivity : BaseActivity<ActivityMainBinding>() {
    private val dialogQueue by lazy {
        DialogQueueManager(supportFragmentManager)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        dialogQueue.release()
    }
}
```

### 2. 使用合适的优先级

```kotlin
// ✅ 推荐：定义优先级常量
object DialogPriority {
    const val LOW = 0
    const val NORMAL = 50
    const val HIGH = 100
    const val URGENT = 200
    const val CRITICAL = 1000
}

dialogQueue.enqueue(dialog, priority = DialogPriority.HIGH)
```

### 3. 使用有意义的 tag

```kotlin
// ✅ 推荐：使用有意义的 tag
dialogQueue.enqueue(
    dialog = CustomDialog(),
    tag = "user_profile_${userId}"  // 包含业务信息
)

// ❌ 不推荐：使用随机或空 tag
dialogQueue.enqueue(dialog = CustomDialog())  // tag 为空，无法精确控制
```

### 4. 监听队列状态

```kotlin
// ✅ 推荐：监听队列状态，用于 UI 反馈
dialogQueue.setOnQueueStateListener { hasCurrent, pendingCount ->
    if (pendingCount > 0) {
        // 显示"还有 X 个提示等待显示"
        showQueueIndicator(pendingCount)
    } else {
        hideQueueIndicator()
    }
}
```

### 5. 及时释放资源

```kotlin
// ✅ 推荐：在 onDestroy 中释放
override fun onDestroy() {
    super.onDestroy()
    dialogQueue.release()
}
```

---

## ⚠️ 注意事项

1. **FragmentManager 生命周期**：
   - 确保 `FragmentManager` 在 Dialog 显示期间有效
   - 在 `onDestroy` 中调用 `release()` 释放资源

2. **Dialog 生命周期**：
   - 推荐使用 `BaseDialog`，自动处理生命周期
   - 队列管理器会自动使用 `showSafely` 和 `dismissSafely`

3. **优先级设计**：
   - 建议定义优先级常量，避免硬编码
   - 普通 Dialog：0-50
   - 重要 Dialog：50-100
   - 紧急 Dialog：100-200
   - 关键 Dialog：200+

4. **去重机制**：
   - 默认启用，相同 tag 的 Dialog 只保留优先级最高的
   - 如果不需要去重，可以调用 `setEnableDeduplication(false)`

5. **线程安全**：
   - 所有操作都是线程安全的
   - 可以在任意线程调用，会自动切换到主线程显示

---

## 🔧 API 参考

### 核心方法

| 方法 | 说明 |
|------|------|
| `enqueue(dialog, priority, tag)` | 普通入队 |
| `enqueueAtFront(dialog, priority, interruptCurrent, tag)` | 插队入队 |
| `removeByTag(tag)` | 移除指定 tag 的 Dialog |
| `remove(dialog)` | 移除指定 Dialog 实例 |
| `clear(dismissCurrent)` | 清空队列 |
| `release()` | 释放资源 |

### 状态查询

| 方法 | 说明 |
|------|------|
| `hasPending()` | 是否有等待的 Dialog |
| `hasCurrent()` | 是否有正在显示的 Dialog |
| `getPendingCount()` | 获取等待的 Dialog 数量 |
| `getCurrentDialog()` | 获取当前显示的 Dialog |
| `getCurrentTag()` | 获取当前显示的 Dialog 的 tag |
| `containsTag(tag)` | 检查是否包含指定 tag |

### 配置方法

| 方法 | 说明 |
|------|------|
| `setOnQueueStateListener(listener)` | 设置状态监听器 |
| `setEnableDeduplication(enable)` | 设置是否启用去重 |

---

## ✨ 总结

DialogQueueManager 提供了：

- ✅ **单实例展示**：确保同一时间只显示一个 Dialog
- ✅ **队列管理**：自动排队，依次展示
- ✅ **优先级控制**：灵活的优先级机制
- ✅ **插队功能**：支持高优先级 Dialog 插队和打断
- ✅ **去重机制**：相同 tag 的 Dialog 自动去重
- ✅ **状态监听**：实时监听队列状态变化
- ✅ **安全操作**：自动使用安全显示/隐藏方法
- ✅ **资源管理**：自动处理生命周期，防止泄漏

**建议：所有需要管理多个 Dialog 的场景都使用 DialogQueueManager！**


