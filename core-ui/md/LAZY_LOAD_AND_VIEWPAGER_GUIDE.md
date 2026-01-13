# BaseFragment 懒加载和 ViewPager 支持指南

## 📋 概述

`BaseFragment` 现在支持懒加载和 ViewPager 优化，可以显著提升性能，特别是在 ViewPager 中使用时。

---

## 🎯 懒加载（Lazy Loading）

### 什么是懒加载？

懒加载是指只有在 Fragment 真正对用户可见时才加载数据和设置观察者，避免不必要的资源消耗。

### 启用懒加载

```kotlin
class UserListFragment : BaseFragment<FragmentUserListBinding>() {
    
    // 默认启用懒加载
    // override val enableLazyLoad: Boolean = true
    
    override fun createBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentUserListBinding {
        return FragmentUserListBinding.inflate(inflater, container, false)
    }
    
    override fun setupViews() {
        // 视图初始化（立即执行）
        binding.recyclerView.setVerticalLayoutManager()
    }
    
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
}
```

### 禁用懒加载

```kotlin
class UserListFragment : BaseFragment<FragmentUserListBinding>() {
    
    // 禁用懒加载
    override val enableLazyLoad: Boolean = false
    
    override fun setupObservers() {
        // 立即设置观察者（在 onViewCreated 时调用）
        viewModel.uiState.collectOnLifecycle { state ->
            // 处理状态
        }
    }
}
```

### 懒加载生命周期

```
onCreate()
  ↓
onCreateView()
  ↓
onViewCreated()
  ↓ (如果 enableLazyLoad = true，此时不调用 setupObservers 和 loadData)
onResume()
  ↓ (Fragment 可见)
onVisible() ← 首次可见时调用
  ↓
setupObservers() ← 懒加载时，首次可见时调用
setupListeners() ← 懒加载时，首次可见时调用
loadData() ← 懒加载时，首次可见时调用
```

---

## 📱 ViewPager 支持

### ViewPager2（推荐）

ViewPager2 使用 `FragmentStateAdapter`，Fragment 的生命周期是正常的，框架会自动检测并处理。

```kotlin
class TabFragment : BaseFragment<FragmentTabBinding>() {
    
    override fun createBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentTabBinding {
        return FragmentTabBinding.inflate(inflater, container, false)
    }
    
    override fun setupViews() {
        // 视图初始化
    }
    
    override fun loadData() {
        // 懒加载：只有在 Fragment 可见时才会调用
        viewModel.loadData()
    }
    
    override fun onVisible() {
        super.onVisible()
        // Fragment 在 ViewPager 中变为可见时调用
    }
    
    override fun onInvisible() {
        super.onInvisible()
        // Fragment 在 ViewPager 中变为不可见时调用
    }
}
```

### ViewPager（旧版）

对于旧版 ViewPager，框架也会自动检测并处理。

```kotlin
class TabFragment : BaseFragment<FragmentTabBinding>() {
    
    // 使用 ViewPagerFragment 基类（可选）
    // class TabFragment : ViewPagerFragment<FragmentTabBinding>() {
    
    override fun onPageVisible() {
        // 在 ViewPager 中变为可见时调用
    }
    
    override fun onPageInvisible() {
        // 在 ViewPager 中变为不可见时调用
    }
}
```

### 使用 ViewPagerFragment 基类

```kotlin
class TabFragment : ViewPagerFragment<FragmentTabBinding>() {
    
    override fun createBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentTabBinding {
        return FragmentTabBinding.inflate(inflater, container, false)
    }
    
    override fun onPageVisible() {
        // Fragment 在 ViewPager 中变为可见时调用
        // 适合刷新数据、恢复动画等
    }
    
    override fun onPageInvisible() {
        // Fragment 在 ViewPager 中变为不可见时调用
        // 适合暂停操作、保存状态等
    }
}
```

---

## 🔄 可见性回调

### onVisible() / onInvisible()

```kotlin
class UserListFragment : BaseFragment<FragmentUserListBinding>() {
    
    override fun onVisible() {
        super.onVisible()
        // Fragment 变为可见时调用
        // 适合：恢复动画、刷新数据、恢复播放等
    }
    
    override fun onInvisible() {
        super.onInvisible()
        // Fragment 变为不可见时调用
        // 适合：暂停动画、保存状态、暂停播放等
    }
}
```

### 检查可见性

```kotlin
// 检查 Fragment 是否对用户可见
if (isUserVisible()) {
    // Fragment 可见
}

// 安全执行（仅在可见时）
safeExecuteIfUserVisible {
    // 更新 UI
}
```

---

## 📊 Flow 收集优化

### collectOnVisible（仅在可见时收集）

```kotlin
class UserListFragment : BaseFragment<FragmentUserListBinding>() {
    
    override fun setupObservers() {
        // 仅在 Fragment 可见时收集（适用于 ViewPager）
        viewModel.uiState.collectOnVisible { state ->
            // 只有在 Fragment 可见时才会处理状态
            // 避免在不可见时更新 UI
        }
    }
}
```

### 对比

```kotlin
// 方式 1：普通收集（即使不可见也会收集）
viewModel.uiState.collectOnLifecycle { state ->
    // 可能在不必要时更新 UI
}

// 方式 2：仅在可见时收集（推荐用于 ViewPager）
viewModel.uiState.collectOnVisible { state ->
    // 只有在可见时才更新 UI
}
```

---

## 📝 完整示例

### 示例 1：普通 Fragment（懒加载）

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
        // 视图初始化（立即执行）
        binding.recyclerView.setVerticalLayoutManager()
        binding.recyclerView.adapter = UserAdapter()
    }
    
    override fun loadData() {
        // 懒加载：首次可见时调用
        viewModel.loadUsers()
    }
    
    override fun setupObservers() {
        // 懒加载：首次可见时调用
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
    
    override fun onVisible() {
        super.onVisible()
        // Fragment 可见时，可以刷新数据
        if (isDataLoaded()) {
            viewModel.refresh()
        }
    }
}
```

### 示例 2：ViewPager 中的 Fragment

```kotlin
class TabFragment : ViewPagerFragment<FragmentTabBinding>() {
    
    private val viewModel: TabViewModel by viewModels()
    
    override fun createBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentTabBinding {
        return FragmentTabBinding.inflate(inflater, container, false)
    }
    
    override fun setupViews() {
        binding.recyclerView.setVerticalLayoutManager()
    }
    
    override fun loadData() {
        // 懒加载：首次可见时调用
        viewModel.loadData()
    }
    
    override fun setupObservers() {
        // 仅在可见时收集（避免不必要的 UI 更新）
        viewModel.uiState.collectOnVisible { state ->
            when (state) {
                is UiState.Success -> {
                    (binding.recyclerView.adapter as TabAdapter).submitList(state.data)
                }
                else -> {}
            }
        }
    }
    
    override fun onPageVisible() {
        super.onPageVisible()
        // 在 ViewPager 中变为可见时
        // 可以刷新数据、恢复动画等
        if (isDataLoaded()) {
            viewModel.refresh()
        }
    }
    
    override fun onPageInvisible() {
        super.onPageInvisible()
        // 在 ViewPager 中变为不可见时
        // 可以暂停操作、保存状态等
    }
}
```

### 示例 3：刷新数据

```kotlin
class UserListFragment : BaseFragment<FragmentUserListBinding>() {
    
    override fun setupListeners() {
        binding.swipeRefresh.setOnRefreshListener {
            // 重置懒加载状态，重新加载数据
            resetLazyLoad()
        }
    }
    
    override fun loadData() {
        viewModel.loadUsers()
    }
}
```

### 示例 4：视频播放 Fragment（ViewPager 中）

```kotlin
class VideoFragment : ViewPagerFragment<FragmentVideoBinding>() {
    
    override fun onPageVisible() {
        super.onPageVisible()
        // 开始播放视频
        binding.videoView.start()
    }
    
    override fun onPageInvisible() {
        super.onPageInvisible()
        // 暂停播放视频
        binding.videoView.pause()
    }
    
    override fun onDestroyView() {
        binding.videoView.release()
        super.onDestroyView()
    }
}
```

---

## 🎯 最佳实践

### 1. 何时使用懒加载

```kotlin
// ✅ 推荐：数据加载成本高、网络请求、复杂计算
class UserListFragment : BaseFragment<FragmentUserListBinding>() {
    override val enableLazyLoad: Boolean = true
    
    override fun loadData() {
        viewModel.loadUsers()  // 网络请求
    }
}

// ❌ 不推荐：简单 UI 初始化
class SimpleFragment : BaseFragment<FragmentSimpleBinding>() {
    override val enableLazyLoad: Boolean = false  // 禁用懒加载
}
```

### 2. ViewPager 中的 Fragment

```kotlin
// ✅ 推荐：使用 ViewPagerFragment 基类
class TabFragment : ViewPagerFragment<FragmentTabBinding>() {
    override fun onPageVisible() {
        // 处理可见性
    }
}

// ✅ 推荐：使用 collectOnVisible
viewModel.uiState.collectOnVisible { state ->
    // 仅在可见时更新 UI
}
```

### 3. 数据刷新

```kotlin
// ✅ 推荐：使用 resetLazyLoad 刷新数据
binding.swipeRefresh.setOnRefreshListener {
    resetLazyLoad()  // 重置懒加载状态，重新加载
}

// ✅ 推荐：在 onVisible 中检查是否需要刷新
override fun onVisible() {
    super.onVisible()
    if (isDataLoaded() && needsRefresh()) {
        viewModel.refresh()
    }
}
```

### 4. 安全执行

```kotlin
// ✅ 推荐：使用安全执行方法
viewModel.data.collectOnLifecycle { data ->
    safeExecuteIfUserVisible {
        // 只有在可见时才更新 UI
        updateUI(data)
    }
}
```

---

## ⚠️ 注意事项

1. **懒加载时机**：
   - `setupViews()` 在 `onViewCreated()` 时立即调用
   - `setupObservers()` 和 `loadData()` 在首次可见时调用（如果启用懒加载）

2. **ViewPager 检测**：
   - 框架会自动检测 ViewPager/ViewPager2
   - 检测在 `onViewCreated()` 时进行

3. **可见性判断**：
   - 普通 Fragment：`isResumed && isVisible`
   - ViewPager 中的 Fragment：`isResumed`（ViewPager2 使用正常生命周期）

4. **数据加载**：
   - 使用 `isDataLoaded()` 检查数据是否已加载
   - 使用 `resetLazyLoad()` 重置懒加载状态

5. **Flow 收集**：
   - `collectOnLifecycle`：正常收集（即使不可见）
   - `collectOnVisible`：仅在可见时收集（推荐用于 ViewPager）

---

## 🔄 生命周期对比

### 普通 Fragment（懒加载）

```
onCreate()
  ↓
onCreateView()
  ↓
onViewCreated()
  ├─ setupViews() ← 立即执行
  └─ (setupObservers 和 loadData 不执行)
  ↓
onResume()
  ├─ onVisible() ← 首次可见
  ├─ setupObservers() ← 首次可见
  ├─ setupListeners() ← 首次可见
  └─ loadData() ← 首次可见
  ↓
onPause()
  └─ onInvisible() ← 不可见
```

### ViewPager 中的 Fragment（懒加载）

```
onCreate()
  ↓
onCreateView()
  ↓
onViewCreated()
  ├─ setupViews() ← 立即执行
  └─ (setupObservers 和 loadData 不执行)
  ↓
onResume() (Fragment 在 ViewPager 中可见)
  ├─ onVisible() / onPageVisible() ← 可见
  ├─ setupObservers() ← 首次可见
  ├─ setupListeners() ← 首次可见
  └─ loadData() ← 首次可见
  ↓
onPause() (Fragment 在 ViewPager 中不可见)
  └─ onInvisible() / onPageInvisible() ← 不可见
```

---

## ✨ 总结

懒加载和 ViewPager 支持提供了：

- ✅ **懒加载**：只有在 Fragment 可见时才加载数据
- ✅ **ViewPager 检测**：自动检测 ViewPager/ViewPager2
- ✅ **可见性回调**：`onVisible()` / `onInvisible()` / `onPageVisible()` / `onPageInvisible()`
- ✅ **Flow 收集优化**：`collectOnVisible()` 仅在可见时收集
- ✅ **数据加载控制**：`isDataLoaded()` / `resetLazyLoad()`
- ✅ **安全执行**：`safeExecuteIfUserVisible()`
- ✅ **ViewPagerFragment**：专门为 ViewPager 优化的基类

**建议：在 ViewPager 中使用时，启用懒加载并使用 `collectOnVisible`！**


