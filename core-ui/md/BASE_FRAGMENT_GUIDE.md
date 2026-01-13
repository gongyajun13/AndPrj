# BaseFragment 使用指南

## 📋 概述

`BaseFragment` 是一个功能完善的 Fragment 基类，提供了常用的 Fragment 功能，简化开发流程。

**新增功能**：
- ✅ **懒加载支持**：只有在 Fragment 可见时才加载数据
- ✅ **ViewPager 支持**：自动检测并优化 ViewPager 中的 Fragment
- ✅ **可见性回调**：`onVisible()` / `onInvisible()` / `onPageVisible()` / `onPageInvisible()`

详细说明请参考：[懒加载和 ViewPager 支持指南](LAZY_LOAD_AND_VIEWPAGER_GUIDE.md)

---

## ✨ 核心功能

### 1. ViewBinding 支持（自动处理生命周期）

```kotlin
class UserListFragment : BaseFragment<FragmentUserListBinding>() {
    override fun createBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentUserListBinding {
        return FragmentUserListBinding.inflate(inflater, container, false)
    }
    
    override fun setupViews() {
        // 使用 binding 访问视图（自动处理生命周期）
        binding.recyclerView.setVerticalLayoutManager()
    }
}
```

**注意**：`binding` 在 `onDestroyView()` 后会自动置为 null，避免内存泄漏。

### 2. 生命周期方法

```kotlin
class UserListFragment : BaseFragment<FragmentUserListBinding>() {
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
class DetailFragment : BaseFragment<FragmentDetailBinding>() {
    private var hasUnsavedChanges = false
    
    // 禁用返回键处理
    override val enableBackPressHandling: Boolean = true
    
    // 自定义返回键处理
    override fun handleBackPress() {
        if (hasUnsavedChanges) {
            showConfirmDialog(
                title = "确认退出",
                message = "有未保存的更改，确定要退出吗？",
                onConfirm = {
                    parentFragmentManager.popBackStack()
                }
            )
        } else {
            // 默认不做处理，让 Activity 处理
        }
    }
}
```

### 6. Flow 收集（自动处理生命周期）

```kotlin
class UserListFragment : BaseFragment<FragmentUserListBinding>() {
    override fun setupObservers() {
        // 在 STARTED 状态收集（默认，使用 viewLifecycleOwner）
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
        
        // 仅在 Fragment 可见时收集（推荐用于 ViewPager）
        viewModel.uiState.collectOnVisible { state ->
            // 只有在可见时才处理状态
        }
    }
}
```

**重要**：默认使用 `viewLifecycleOwner`，确保在 `onDestroyView()` 时自动取消订阅。

### 7. 加载指示器

```kotlin
class UserListFragment : BaseFragment<FragmentUserListBinding>() {
    override fun showLoading() {
        // 显示加载指示器（如 ProgressBar）
        binding.progressBar.visibility = View.VISIBLE
    }
    
    override fun hideLoading() {
        // 隐藏加载指示器
        binding.progressBar.visibility = View.GONE
    }
}
```

### 7. 懒加载支持

```kotlin
class UserListFragment : BaseFragment<FragmentUserListBinding>() {
    
    // 默认启用懒加载
    // override val enableLazyLoad: Boolean = true
    
    override fun loadData() {
        // 懒加载：只有在 Fragment 首次可见时才会调用
        viewModel.loadUsers()
    }
    
    override fun setupObservers() {
        // 懒加载：只有在 Fragment 首次可见时才会调用
        viewModel.uiState.collectOnLifecycle { state ->
            // 处理状态
        }
    }
    
    override fun onVisible() {
        super.onVisible()
        // Fragment 变为可见时调用
    }
    
    override fun onInvisible() {
        super.onInvisible()
        // Fragment 变为不可见时调用
    }
}
```

### 8. ViewPager 支持

```kotlin
// 方式 1：使用 BaseFragment（自动检测 ViewPager）
class TabFragment : BaseFragment<FragmentTabBinding>() {
    override fun onVisible() {
        super.onVisible()
        // Fragment 在 ViewPager 中变为可见时调用
    }
}

// 方式 2：使用 ViewPagerFragment（推荐）
class TabFragment : ViewPagerFragment<FragmentTabBinding>() {
    override fun onPageVisible() {
        // Fragment 在 ViewPager 中变为可见时调用
    }
    
    override fun onPageInvisible() {
        // Fragment 在 ViewPager 中变为不可见时调用
    }
}
```

### 9. 工具方法

#### 状态检查

```kotlin
// 检查是否首次创建（非配置变更导致的重建）
if (isFirstCreate()) {
    // 首次创建时的逻辑
}

// 检查是否由配置变更导致的重建
if (isConfigChange()) {
    // 配置变更时的逻辑
}

// 访问保存的实例状态
savedState?.let { bundle ->
    // 恢复状态
}
```

#### Fragment 状态检查

```kotlin
// 检查 Fragment 是否已添加到 Activity
if (isFragmentAdded()) {
    // Fragment 已添加，可以安全访问 Activity
}

// 检查 Fragment 是否可见
if (isFragmentVisible()) {
    // Fragment 可见，可以更新 UI
}

// 检查 Fragment 是否对用户可见（包括 ViewPager 中的情况）
if (isUserVisible()) {
    // Fragment 对用户可见
}

// 检查是否在 ViewPager 中
if (isInViewPager) {
    // Fragment 在 ViewPager 中
}

// 检查数据是否已加载（懒加载）
if (isDataLoaded()) {
    // 数据已加载
}
```

#### 安全执行

```kotlin
// 仅在 Fragment 已添加时执行
safeExecute {
    // 安全操作
    activity?.let { /* ... */ }
}

// 仅在 Fragment 可见时执行
safeExecuteIfVisible {
    // 更新 UI
    binding.textView.text = "更新内容"
}

// 仅在 Fragment 对用户可见时执行（适用于 ViewPager）
safeExecuteIfUserVisible {
    // 更新 UI
    binding.textView.text = "更新内容"
}
```

#### 懒加载控制

```kotlin
// 检查数据是否已加载
if (isDataLoaded()) {
    // 数据已加载
}

// 重置懒加载状态（用于刷新数据）
resetLazyLoad()  // 下次可见时会重新加载数据
```

#### 父 Activity 访问

```kotlin
// 获取父 Activity（类型安全，必须是 AppCompatActivity）
val activity = requireAppCompatActivity()

// 使用 Activity 的功能
activity.setStatusBarColor(Color.BLACK)
```

#### 延迟执行

```kotlin
// 延迟执行（仅在 Fragment 可见时）
binding.button.postDelayedIfVisible(1000) {
    // 1 秒后执行（如果 Fragment 仍然可见）
}

// 延迟执行（仅在 Fragment 已添加时）
binding.button.postDelayedIfAdded(1000) {
    // 1 秒后执行（如果 Fragment 仍然已添加）
}

// 延迟执行（仅在 Fragment 对用户可见时，适用于 ViewPager）
binding.button.postDelayedIfUserVisible(1000) {
    // 1 秒后执行（如果 Fragment 仍然对用户可见）
}
```

#### 系统栏高度

```kotlin
// 获取状态栏高度
val statusBarHeight = getStatusBarHeight()

// 获取导航栏高度
val navigationBarHeight = getNavigationBarHeight()
```

---

## 📝 完整示例

### 示例 1：基础使用

```kotlin
class UserListFragment : BaseFragment<FragmentUserListBinding>() {
    
    private val viewModel: UserViewModel by viewModels()
    
    override fun createBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentUserListBinding {
        return FragmentUserListBinding.inflate(inflater, container, false)
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
class DetailFragment : BaseFragment<FragmentDetailBinding>() {
    private var hasUnsavedChanges = false
    
    override fun handleBackPress() {
        if (hasUnsavedChanges) {
            requireContext().showConfirmDialog(
                title = "确认退出",
                message = "有未保存的更改，确定要退出吗？",
                onConfirm = {
                    parentFragmentManager.popBackStack()
                }
            )
        }
        // 否则不做处理，让 Activity 处理返回键
    }
}
```

### 示例 3：安全执行操作

```kotlin
class UserListFragment : BaseFragment<FragmentUserListBinding>() {
    
    private fun loadData() {
        viewModel.loadUsers()
    }
    
    private fun updateUI(data: List<User>) {
        // 安全更新 UI（仅在 Fragment 可见时）
        safeExecuteIfVisible {
            (binding.recyclerView.adapter as UserAdapter).submitList(data)
        }
    }
    
    override fun setupObservers() {
        viewModel.users.collectOnLifecycle { users ->
            // 使用安全执行
            safeExecuteIfVisible {
                updateUI(users)
            }
        }
    }
}
```

### 示例 4：延迟执行

```kotlin
class SplashFragment : BaseFragment<FragmentSplashBinding>() {
    
    override fun setupViews() {
        super.setupViews()
        
        // 延迟 2 秒后跳转（仅在 Fragment 可见时）
        binding.root.postDelayedIfVisible(2000) {
            findNavController().navigate(R.id.action_splash_to_main)
        }
    }
}
```

### 示例 5：使用保存状态

```kotlin
class UserListFragment : BaseFragment<FragmentUserListBinding>() {
    private var scrollPosition = 0
    
    override fun setupViews() {
        super.setupViews()
        
        // 首次创建时初始化
        if (isFirstCreate()) {
            loadData()
        } else {
            // 配置变更时恢复状态
            binding.recyclerView.scrollToPosition(scrollPosition)
        }
    }
    
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("scroll_position", scrollPosition)
    }
    
    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        scrollPosition = savedInstanceState?.getInt("scroll_position", 0) ?: 0
    }
}
```

---

## 🎯 最佳实践

### 1. 使用 viewLifecycleOwner 收集 Flow

```kotlin
// ✅ 推荐：使用默认的 viewLifecycleOwner（自动处理）
viewModel.uiState.collectOnLifecycle { state ->
    // 处理状态
}

// ❌ 不推荐：使用 Fragment 的 lifecycleOwner（可能导致内存泄漏）
viewModel.uiState.collectOnLifecycle(lifecycleOwner = this) { state ->
    // 处理状态
}
```

### 2. 使用安全执行

```kotlin
// ✅ 推荐：使用安全执行
safeExecuteIfVisible {
    binding.textView.text = "更新内容"
}

// ❌ 不推荐：直接访问（可能 Fragment 已销毁）
binding.textView.text = "更新内容"
```

### 3. 合理使用生命周期方法

```kotlin
class UserListFragment : BaseFragment<FragmentUserListBinding>() {
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

### 4. 返回键处理

```kotlin
// ✅ 推荐：在需要时处理返回键
override fun handleBackPress() {
    if (needsCustomHandling()) {
        // 自定义处理
    }
    // 否则不做处理，让 Activity 处理
}

// ❌ 不推荐：总是调用 super（Fragment 没有 super）
override fun handleBackPress() {
    super.handleBackPress()  // 错误：Fragment 没有这个方法
}
```

### 5. ViewBinding 访问

```kotlin
// ✅ 推荐：在 setupViews、setupObservers、setupListeners 中使用
override fun setupViews() {
    binding.textView.text = "Hello"
}

// ⚠️ 注意：在 onDestroyView() 后不能访问 binding
override fun onDestroyView() {
    super.onDestroyView()
    // binding 已为 null，不能再访问
}
```

---

## ⚠️ 注意事项

1. **ViewBinding 生命周期**：
   - `binding` 在 `onCreateView()` 时创建
   - `binding` 在 `onDestroyView()` 时置为 null
   - 不要在 `onDestroyView()` 后访问 `binding`

2. **Flow 收集**：
   - 默认使用 `viewLifecycleOwner`，确保在 `onDestroyView()` 时自动取消订阅
   - 不要使用 Fragment 的 `lifecycleOwner`，可能导致内存泄漏

3. **返回键处理**：
   - 默认启用，可以通过 `enableBackPressHandling` 禁用
   - 返回键默认会隐藏软键盘，可通过 `hideKeyboardOnBackPress` 禁用
   - `handleBackPress()` 默认不做处理，让 Activity 处理返回键

4. **安全执行**：
   - 使用 `safeExecute` 和 `safeExecuteIfVisible` 确保操作安全
   - 特别是在异步操作的回调中使用

5. **Fragment 状态**：
   - 使用 `isFragmentAdded()` 检查 Fragment 是否已添加
   - 使用 `isFragmentVisible()` 检查 Fragment 是否可见
   - 在异步回调中总是检查状态

---

## 🔄 与 BaseActivity 的对比

| 功能 | BaseActivity | BaseFragment |
|------|-------------|--------------|
| ViewBinding | ✅ 支持 | ✅ 支持（自动处理生命周期） |
| 消息提示 | ✅ 支持 | ✅ 支持 |
| 软键盘管理 | ✅ 支持 | ✅ 支持 |
| 返回键处理 | ✅ 支持 | ✅ 支持 |
| Flow 收集 | ✅ 支持 | ✅ 支持（使用 viewLifecycleOwner） |
| 状态栏配置 | ✅ 支持 | ❌ 不支持（由 Activity 处理） |
| 工具方法 | ✅ 支持 | ✅ 支持（Fragment 特定） |

---

## ✨ 总结

BaseFragment 提供了：

- ✅ **ViewBinding 支持**：自动处理生命周期，避免内存泄漏
- ✅ **消息提示**：统一的 Snackbar 提示
- ✅ **软键盘管理**：便捷的键盘控制
- ✅ **返回键处理**：灵活的返回键控制
- ✅ **Flow 收集**：自动处理生命周期（使用 viewLifecycleOwner）
- ✅ **加载指示器**：统一的加载状态管理
- ✅ **懒加载支持**：只有在 Fragment 可见时才加载数据
- ✅ **ViewPager 支持**：自动检测并优化 ViewPager 中的 Fragment
- ✅ **可见性回调**：`onVisible()` / `onInvisible()` / `onPageVisible()` / `onPageInvisible()`
- ✅ **工具方法**：Fragment 特定的实用方法
- ✅ **安全执行**：确保操作在 Fragment 有效时执行

**建议：根据实际需求使用这些功能！**

**详细说明**：
- 懒加载和 ViewPager 支持：参考 [懒加载和 ViewPager 支持指南](LAZY_LOAD_AND_VIEWPAGER_GUIDE.md)

