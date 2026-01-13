# BaseActivity 使用指南

## 📋 概述

`BaseActivity` 是一个功能完善的 Activity 基类，提供了常用的 Activity 功能，简化开发流程。

---

## ✨ 核心功能

### 1. ViewBinding 支持

```kotlin
class MainActivity : BaseActivity<ActivityMainBinding>() {
    override fun createBinding(): ActivityMainBinding {
        return ActivityMainBinding.inflate(layoutInflater)
    }
    
    override fun setupViews() {
        // 使用 binding 访问视图
        binding.textView.text = "Hello"
    }
}
```

### 2. 生命周期方法

```kotlin
class MainActivity : BaseActivity<ActivityMainBinding>() {
    override fun setupViews() {
        // 初始化视图
    }
    
    override fun setupObservers() {
        // 设置观察者（如 ViewModel 的 LiveData/Flow）
    }
    
    override fun setupListeners() {
        // 设置点击监听器等
    }
}
```

### 3. 消息提示

```kotlin
// 显示错误消息
showError("操作失败")

// 显示成功消息
showSuccess("操作成功")

// 显示警告消息
showWarning("请注意")

// 显示普通消息
showMessage("提示信息")
```

### 4. 软键盘管理

```kotlin
// 隐藏软键盘
hideKeyboard()

// 检查软键盘是否显示
if (isKeyboardVisible()) {
    hideKeyboard()
}
```

### 5. 返回键处理

```kotlin
class MainActivity : BaseActivity<ActivityMainBinding>() {
    // 禁用返回键处理
    override val enableBackPressHandling: Boolean = false
    
    // 自定义返回键处理
    override fun handleBackPress() {
        // 自定义逻辑
        if (canGoBack()) {
            super.handleBackPress()
        } else {
            // 显示确认对话框等
        }
    }
}
```

### 6. 状态栏和导航栏配置

#### 全屏模式

```kotlin
// 设置全屏
setFullScreen()

// 退出全屏
exitFullScreen()
```

#### 沉浸式状态栏

```kotlin
// 设置沉浸式状态栏（状态栏透明）
setImmersiveStatusBar(
    lightStatusBar = false,      // 状态栏图标是否浅色
    lightNavigationBar = false   // 导航栏图标是否浅色
)
```

#### 设置状态栏和导航栏颜色

```kotlin
// 设置状态栏颜色
setStatusBarColor(
    color = Color.BLACK,
    lightIcons = true  // 浅色图标（适合深色背景）
)

// 设置导航栏颜色
setNavigationBarColor(
    color = Color.WHITE,
    lightIcons = false  // 深色图标（适合浅色背景）
)

// 同时设置状态栏和导航栏
setSystemBarsColor(
    statusBarColor = Color.BLACK,
    navigationBarColor = Color.WHITE,
    lightStatusBar = true,
    lightNavigationBar = false
)
```

#### 显示/隐藏状态栏和导航栏

```kotlin
// 隐藏状态栏
hideStatusBar()

// 显示状态栏
showStatusBar()

// 隐藏导航栏
hideNavigationBar()

// 显示导航栏
showNavigationBar()
```

#### 保持屏幕常亮

```kotlin
// 保持屏幕常亮
setKeepScreenOn(true)

// 取消保持屏幕常亮
setKeepScreenOn(false)
```

### 7. Flow 收集（自动处理生命周期）

```kotlin
class MainActivity : BaseActivity<ActivityMainBinding>() {
    override fun setupObservers() {
        // 在 STARTED 状态收集（默认）
        viewModel.uiState.collectOnLifecycle { state ->
            // 处理状态
        }
        
        // 在 RESUMED 状态收集
        viewModel.uiState.collectOnResumed { state ->
            // 处理状态
        }
        
        // 在 STARTED 状态收集
        viewModel.uiState.collectOnStarted { state ->
            // 处理状态
        }
        
        // 在 CREATED 状态收集
        viewModel.uiState.collectOnCreated { state ->
            // 处理状态
        }
    }
}
```

### 8. 加载指示器

```kotlin
class MainActivity : BaseActivity<ActivityMainBinding>() {
    override fun showLoading() {
        // 显示加载指示器（如 ProgressDialog、ProgressBar）
        binding.progressBar.visibility = View.VISIBLE
    }
    
    override fun hideLoading() {
        // 隐藏加载指示器
        binding.progressBar.visibility = View.GONE
    }
}
```

### 9. 工具方法

```kotlin
// 检查是否首次创建（非配置变更导致的重建）
if (isFirstCreate()) {
    // 首次创建时的逻辑
}

// 检查是否由配置变更导致的重建
if (isConfigChange()) {
    // 配置变更时的逻辑
}

// 获取状态栏高度
val statusBarHeight = getStatusBarHeight()

// 获取导航栏高度
val navigationBarHeight = getNavigationBarHeight()

// 访问保存的实例状态
savedState?.let { bundle ->
    // 恢复状态
}
```

---

## 📝 完整示例

### 示例 1：基础使用

```kotlin
class UserListActivity : BaseActivity<ActivityUserListBinding>() {
    
    private val viewModel: UserViewModel by viewModels()
    
    override fun createBinding(): ActivityUserListBinding {
        return ActivityUserListBinding.inflate(layoutInflater)
    }
    
    override fun setupViews() {
        binding.recyclerView.setVerticalLayoutManager()
        binding.recyclerView.adapter = UserAdapter()
    }
    
    override fun setupObservers() {
        viewModel.uiState.collectOnLifecycle { state ->
            when (state) {
                is UiState.Loading -> showLoading()
                is UiState.Success -> {
                    hideLoading()
                    (binding.recyclerView.adapter as UserAdapter).submitList(state.data)
                }
                is UiState.Error -> {
                    hideLoading()
                    showError(state.message)
                }
                else -> {}
            }
        }
    }
    
    override fun setupListeners() {
        binding.fab.setOnClickListener {
            // 添加用户
        }
    }
    
    override fun showLoading() {
        binding.progressBar.visibility = View.VISIBLE
    }
    
    override fun hideLoading() {
        binding.progressBar.visibility = View.GONE
    }
}
```

### 示例 2：自定义返回键处理

```kotlin
class DetailActivity : BaseActivity<ActivityDetailBinding>() {
    private var hasUnsavedChanges = false
    
    override fun handleBackPress() {
        if (hasUnsavedChanges) {
            showConfirmDialog(
                title = "确认退出",
                message = "有未保存的更改，确定要退出吗？",
                onConfirm = {
                    finish()
                }
            )
        } else {
            super.handleBackPress()
        }
    }
}
```

### 示例 3：沉浸式状态栏

```kotlin
class ImageViewerActivity : BaseActivity<ActivityImageViewerBinding>() {
    
    override fun setupViews() {
        super.setupViews()
        
        // 设置沉浸式状态栏
        setImmersiveStatusBar(
            lightStatusBar = false,  // 深色图标（适合浅色图片）
            lightNavigationBar = false
        )
        
        // 或者设置全屏
        setFullScreen()
    }
}
```

### 示例 4：状态栏颜色配置

```kotlin
class MainActivity : BaseActivity<ActivityMainBinding>() {
    
    override fun setupViews() {
        super.setupViews()
        
        // 设置状态栏为白色，使用深色图标
        setStatusBarColor(
            color = Color.WHITE,
            lightIcons = false
        )
        
        // 设置导航栏为白色，使用深色图标
        setNavigationBarColor(
            color = Color.WHITE,
            lightIcons = false
        )
    }
}
```

---

## 🎯 最佳实践

### 1. 使用 Flow 收集替代 LiveData observe

```kotlin
// ✅ 推荐：使用 Flow 收集
viewModel.uiState.collectOnLifecycle { state ->
    // 处理状态
}

// ❌ 不推荐：使用 LiveData observe（需要手动处理生命周期）
viewModel.uiState.observe(this) { state ->
    // 处理状态
}
```

### 2. 合理使用生命周期方法

```kotlin
class MainActivity : BaseActivity<ActivityMainBinding>() {
    override fun setupViews() {
        // ✅ 只做视图初始化
        binding.recyclerView.setVerticalLayoutManager()
    }
    
    override fun setupObservers() {
        // ✅ 只做观察者设置
        viewModel.uiState.collectOnLifecycle { }
    }
    
    override fun setupListeners() {
        // ✅ 只做监听器设置
        binding.button.setOnClickListener { }
    }
}
```

### 3. 状态栏配置

```kotlin
// ✅ 推荐：根据内容背景选择图标颜色
setStatusBarColor(
    color = if (isDarkBackground) Color.BLACK else Color.WHITE,
    lightIcons = !isDarkBackground
)
```

### 4. 返回键处理

```kotlin
// ✅ 推荐：在需要时禁用自动返回键处理
override val enableBackPressHandling: Boolean = false

// 然后手动处理
override fun onBackPressed() {
    // 自定义逻辑
}
```

---

## ⚠️ 注意事项

1. **ViewBinding**：必须在 `createBinding()` 中创建 ViewBinding
2. **返回键处理**：默认启用，可以通过 `enableBackPressHandling` 禁用
3. **软键盘**：返回键默认会隐藏软键盘，可通过 `hideKeyboardOnBackPress` 禁用
4. **状态栏配置**：建议在 `setupViews()` 中配置
5. **Flow 收集**：自动处理生命周期，无需手动取消订阅

---

## ✨ 总结

BaseActivity 提供了：

- ✅ **ViewBinding 支持**：简化视图访问
- ✅ **消息提示**：统一的 Snackbar 提示
- ✅ **软键盘管理**：便捷的键盘控制
- ✅ **状态栏配置**：完整的系统栏配置
- ✅ **返回键处理**：灵活的返回键控制
- ✅ **Flow 收集**：自动处理生命周期
- ✅ **加载指示器**：统一的加载状态管理
- ✅ **工具方法**：实用的辅助方法

**建议：根据实际需求使用这些功能！**


