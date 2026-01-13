# BaseDialog 使用指南

## 📋 概述

`BaseDialog` 是一个功能完善的 DialogFragment 基类，提供了常用的 Dialog 功能，简化开发流程。

**核心功能**：
- ✅ **ViewBinding 支持**：自动处理生命周期
- ✅ **消息提示**：Snackbar 支持
- ✅ **软键盘管理**：便捷的键盘控制
- ✅ **Dialog 配置**：宽度、高度、动画、背景等
- ✅ **安全显示/隐藏**：避免状态问题
- ✅ **生命周期方法**：setupViews、setupObservers、setupListeners

---

## ✨ 核心功能

### 1. ViewBinding 支持

```kotlin
class CustomDialog : BaseDialog<DialogCustomBinding>() {
    override fun createBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): DialogCustomBinding {
        return DialogCustomBinding.inflate(inflater, container, false)
    }
    
    override fun setupViews() {
        binding.title.text = "标题"
        binding.message.text = "消息内容"
    }
}
```

**注意**：`binding` 在 `onDestroyView()` 后会自动置为 null，避免内存泄漏。

### 2. Dialog 配置

```kotlin
class CustomDialog : BaseDialog<DialogCustomBinding>() {
    // 设置 Dialog 宽度（默认 WRAP_CONTENT）
    override val dialogWidth: Int = WindowManager.LayoutParams.MATCH_PARENT
    
    // 设置 Dialog 高度（默认 WRAP_CONTENT）
    override val dialogHeight: Int = WindowManager.LayoutParams.WRAP_CONTENT
    
    // 是否可取消（默认 true）
    override val cancelable: Boolean = true
    
    // 是否透明背景（默认 false）
    override val isBackgroundTransparent: Boolean = false
    
    // Dialog 动画（0 表示使用默认）
    override val dialogAnimation: Int = R.style.DialogAnimation
    
    // 是否全屏（默认 false）
    override val isFullScreen: Boolean = false
}
```

### 3. 动态设置 Dialog 尺寸

```kotlin
class CustomDialog : BaseDialog<DialogCustomBinding>() {
    override fun setupViews() {
        // 设置宽度为 300dp
        setDialogWidth(300)
        
        // 设置高度为 400dp
        setDialogHeight(400)
        
        // 同时设置宽度和高度
        setDialogSize(300, 400)
        
        // 设置 Dialog 位置（如居中、底部等）
        setDialogGravity(android.view.Gravity.CENTER)
    }
}
```

### 4. 生命周期方法

```kotlin
class CustomDialog : BaseDialog<DialogCustomBinding>() {
    override fun setupViews() {
        // 初始化视图
        binding.title.text = "标题"
    }
    
    override fun setupObservers() {
        // 设置观察者（如 ViewModel 的 LiveData/Flow）
    }
    
    override fun setupListeners() {
        // 设置点击监听器等
        binding.confirmButton.setOnClickListener {
            onConfirm()
            dismissSafely()
        }
    }
}
```

### 5. 消息提示

```kotlin
class CustomDialog : BaseDialog<DialogCustomBinding>() {
    fun showMessage() {
        // 显示错误消息
        showError("操作失败")
        
        // 显示成功消息
        showSuccess("操作成功")
        
        // 显示警告消息
        showWarning("请注意")
        
        // 显示普通消息
        showMessage("提示信息")
    }
}
```

### 6. 软键盘管理

```kotlin
class CustomDialog : BaseDialog<DialogCustomBinding>() {
    override fun setupListeners() {
        binding.editText.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                hideKeyboard()
            }
        }
    }
    
    fun checkKeyboard() {
        if (isKeyboardVisible()) {
            hideKeyboard()
        }
    }
}
```

### 7. 安全显示/隐藏

```kotlin
// 安全显示 Dialog
val dialog = CustomDialog()
dialog.showSafely(supportFragmentManager, "CustomDialog")

// 或者使用默认 tag（类名）
dialog.showSafely(supportFragmentManager)

// 安全关闭 Dialog
dialog.dismissSafely()
```

### 8. Dialog 关闭回调

```kotlin
class CustomDialog : BaseDialog<DialogCustomBinding>() {
    override fun setupListeners() {
        binding.confirmButton.setOnClickListener {
            // 关闭 Dialog 并传递结果
            dismissWithResult("确认")
        }
    }
    
    override fun onDialogDismissed(result: Any?) {
        // Dialog 关闭时的回调
        if (result == "确认") {
            // 处理确认操作
        }
    }
    
    override fun onDialogCancelled() {
        // Dialog 取消时的回调
    }
}
```

---

## 📝 完整示例

### 示例 1：自定义 Dialog

```kotlin
class UserInfoDialog : BaseDialog<DialogUserInfoBinding>() {
    
    private var userId: String? = null
    
    override val dialogWidth: Int = WindowManager.LayoutParams.MATCH_PARENT
    override val cancelable: Boolean = true
    
    override fun createBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): DialogUserInfoBinding {
        return DialogUserInfoBinding.inflate(inflater, container, false)
    }
    
    override fun setupViews() {
        userId = arguments?.getString("userId")
        
        // 设置 Dialog 宽度为屏幕的 80%
        val displayMetrics = resources.displayMetrics
        val width = (displayMetrics.widthPixels * 0.8).toInt()
        setDialogWidth(width)
        
        binding.title.text = "用户信息"
    }
    
    override fun setupListeners() {
        binding.confirmButton.setOnClickListener {
            val name = binding.nameEditText.text.toString()
            if (name.isNotEmpty()) {
                // 保存用户信息
                saveUserInfo(name)
                dismissWithResult(name)
            } else {
                showError("请输入用户名")
            }
        }
        
        binding.cancelButton.setOnClickListener {
            dismissSafely()
        }
    }
    
    private fun saveUserInfo(name: String) {
        // 保存逻辑
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

// 使用
val dialog = UserInfoDialog.newInstance("123")
dialog.showSafely(supportFragmentManager, "UserInfoDialog")
```

### 示例 2：全屏 Dialog

```kotlin
class FullScreenDialog : BaseDialog<DialogFullScreenBinding>() {
    
    override val isFullScreen: Boolean = true
    override val showSystemBars: Boolean = false
    
    override fun createBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): DialogFullScreenBinding {
        return DialogFullScreenBinding.inflate(inflater, container, false)
    }
    
    override fun setupViews() {
        binding.closeButton.setOnClickListener {
            dismissSafely()
        }
    }
}
```

### 示例 3：底部弹出 Dialog

```kotlin
class BottomSheetDialog : BaseDialog<DialogBottomSheetBinding>() {
    
    override val dialogWidth: Int = WindowManager.LayoutParams.MATCH_PARENT
    override val dialogAnimation: Int = R.style.BottomSheetAnimation
    override val isBackgroundTransparent: Boolean = true
    
    override fun createBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): DialogBottomSheetBinding {
        return DialogBottomSheetBinding.inflate(inflater, container, false)
    }
    
    override fun setupViews() {
        // 设置 Dialog 位置为底部
        setDialogGravity(android.view.Gravity.BOTTOM)
    }
}
```

### 示例 4：带加载状态的 Dialog

```kotlin
class LoadingDialog : BaseDialog<DialogLoadingBinding>() {
    
    override val cancelable: Boolean = false
    override val dialogWidth: Int = WindowManager.LayoutParams.WRAP_CONTENT
    
    override fun createBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): DialogLoadingBinding {
        return DialogLoadingBinding.inflate(inflater, container, false)
    }
    
    override fun setupViews() {
        binding.message.text = "加载中..."
    }
    
    fun updateMessage(message: String) {
        binding.message.text = message
    }
}

// 使用
val loadingDialog = LoadingDialog()
loadingDialog.showSafely(supportFragmentManager, "LoadingDialog")

// 更新消息
loadingDialog.updateMessage("处理中...")

// 关闭
loadingDialog.dismissSafely()
```

---

## 🎯 简单 Dialog

### SimpleConfirmDialog

```kotlin
// 显示确认 Dialog
SimpleConfirmDialog.newInstance(
    title = "确认",
    message = "确定要删除吗？",
    positiveText = "删除",
    negativeText = "取消",
    onConfirm = {
        // 确认操作
        deleteItem()
    },
    onCancel = {
        // 取消操作
    }
).showSafely(supportFragmentManager, "ConfirmDialog")
```

### SimpleLoadingDialog

```kotlin
// 显示加载 Dialog
val loadingDialog = SimpleLoadingDialog()
loadingDialog.showSafely(supportFragmentManager, "LoadingDialog")

// 关闭
loadingDialog.dismissSafely()
```

---

## 🎯 最佳实践

### 1. 使用 ViewBinding

```kotlin
// ✅ 推荐：使用 ViewBinding
class CustomDialog : BaseDialog<DialogCustomBinding>() {
    override fun createBinding(...): DialogCustomBinding {
        return DialogCustomBinding.inflate(inflater, container, false)
    }
}

// ❌ 不推荐：直接使用 findViewById
class CustomDialog : BaseDialog<...>() {
    override fun onCreateView(...): View {
        val view = inflate(...)
        val title = view.findViewById<TextView>(R.id.title)  // 不推荐
        return view
    }
}
```

### 2. 安全显示/隐藏

```kotlin
// ✅ 推荐：使用安全方法
dialog.showSafely(supportFragmentManager, "tag")
dialog.dismissSafely()

// ❌ 不推荐：直接调用
dialog.show(supportFragmentManager, "tag")  // 可能重复显示
dialog.dismiss()  // 可能状态异常
```

### 3. 参数传递

```kotlin
// ✅ 推荐：使用 companion object 创建实例
companion object {
    fun newInstance(userId: String): UserDialog {
        return UserDialog().apply {
            arguments = Bundle().apply {
                putString("userId", userId)
            }
        }
    }
}

// ❌ 不推荐：直接设置参数
val dialog = UserDialog()
dialog.userId = "123"  // 可能为空
```

### 4. Dialog 尺寸

```kotlin
// ✅ 推荐：使用 dp 单位
override val dialogWidth: Int = 300  // 300dp

// ✅ 推荐：动态设置
setDialogWidth(300)  // 300dp

// ❌ 不推荐：使用像素值
override val dialogWidth: Int = 900  // 硬编码像素值
```

---

## ⚠️ 注意事项

1. **ViewBinding 生命周期**：
   - `binding` 在 `onDestroyView()` 后会自动置为 null
   - 不要在 `onDestroyView()` 后访问 `binding`

2. **Dialog 显示**：
   - 使用 `showSafely()` 避免重复显示
   - 使用 `dismissSafely()` 避免状态异常

3. **参数传递**：
   - 使用 `arguments` Bundle 传递参数
   - 在 `setupViews()` 中读取参数

4. **全屏 Dialog**：
   - 设置 `isFullScreen = true`
   - 设置 `showSystemBars = false` 隐藏系统栏

5. **取消处理**：
   - 设置 `cancelable = false` 防止意外取消
   - 重写 `onDialogCancelled()` 处理取消事件

---

## ✨ 总结

BaseDialog 提供了：

- ✅ **ViewBinding 支持**：自动处理生命周期，避免内存泄漏
- ✅ **Dialog 配置**：宽度、高度、动画、背景等
- ✅ **消息提示**：统一的 Snackbar 提示
- ✅ **软键盘管理**：便捷的键盘控制
- ✅ **安全显示/隐藏**：避免状态问题
- ✅ **生命周期方法**：setupViews、setupObservers、setupListeners
- ✅ **关闭回调**：onDialogDismissed、onDialogCancelled
- ✅ **简单 Dialog**：SimpleConfirmDialog、SimpleLoadingDialog

**建议：所有自定义 Dialog 继承 BaseDialog！**


