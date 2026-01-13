# ViewPager2 适配器使用指南

## 📋 概述

框架提供了完整的 ViewPager2 适配器支持，基于 `FragmentStateAdapter`。

所有适配器都与 `BaseFragment` 的懒加载和 ViewPager2 支持完美集成。

**新增功能**：
- ✅ **动态适配器**：`DynamicViewPager2Adapter` 支持动态添加/删除/替换 Fragment
- ✅ **智能复用**：自动处理相同类型 Fragment 的复用

详细说明请参考：[动态 ViewPager2 适配器指南](DYNAMIC_VIEWPAGER_GUIDE.md)

---

## 🎯 ViewPager2 适配器

### 1. ViewPager2Adapter（Activity 中使用）

```kotlin
class TabPagerAdapter(activity: FragmentActivity) : ViewPager2Adapter(activity) {
    override fun getItemCount(): Int = 3
    
    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> HomeFragment()
            1 -> CategoryFragment()
            2 -> ProfileFragment()
            else -> throw IllegalArgumentException("Invalid position: $position")
        }
    }
}

// 在 Activity 中使用
class MainActivity : BaseActivity<ActivityMainBinding>() {
    override fun setupViews() {
        val adapter = TabPagerAdapter(this)
        binding.viewPager2.adapter = adapter
        
        // 可选：与 TabLayout 联动
        TabLayoutMediator(binding.tabLayout, binding.viewPager2) { tab, position ->
            tab.text = when (position) {
                0 -> "首页"
                1 -> "分类"
                2 -> "我的"
                else -> ""
            }
        }.attach()
    }
}
```

### 2. ViewPager2AdapterWithFragment（Fragment 中使用）

```kotlin
class TabPagerAdapter(fragment: Fragment) : ViewPager2AdapterWithFragment(fragment) {
    override fun getItemCount(): Int = 3
    
    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> HomeFragment()
            1 -> CategoryFragment()
            2 -> ProfileFragment()
            else -> throw IllegalArgumentException("Invalid position: $position")
        }
    }
}

// 在 Fragment 中使用
class ContainerFragment : BaseFragment<FragmentContainerBinding>() {
    override fun setupViews() {
        val adapter = TabPagerAdapter(this)
        binding.viewPager2.adapter = adapter
    }
}
```

### 3. ViewPager2AdapterWithLifecycle（自定义生命周期）

```kotlin
class CustomPagerAdapter(
    fragmentManager: FragmentManager,
    lifecycle: Lifecycle
) : ViewPager2AdapterWithLifecycle(fragmentManager, lifecycle) {
    override fun getItemCount(): Int = 3
    
    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> HomeFragment()
            1 -> CategoryFragment()
            2 -> ProfileFragment()
            else -> throw IllegalArgumentException("Invalid position: $position")
        }
    }
}
```

### 4. SimpleViewPager2Adapter（简化版）

```kotlin
// 方式 1：使用 FragmentFactory
val fragments = listOf(
    HomeFragment(),
    CategoryFragment(),
    ProfileFragment()
)

val adapter = SimpleViewPager2Adapter(this, fragments.size) { position ->
    fragments[position]
}
binding.viewPager2.adapter = adapter

// 方式 2：动态创建
val adapter = SimpleViewPager2Adapter(this, 3) { position ->
    when (position) {
        0 -> HomeFragment()
        1 -> CategoryFragment()
        2 -> ProfileFragment()
        else -> throw IllegalArgumentException("Invalid position: $position")
    }
}
binding.viewPager2.adapter = adapter
```

---

## 🔄 与 BaseFragment 集成

### 懒加载支持

所有适配器都与 `BaseFragment` 的懒加载完美集成：

```kotlin
class HomeFragment : ViewPagerFragment<FragmentHomeBinding>() {
    override fun createBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentHomeBinding {
        return FragmentHomeBinding.inflate(inflater, container, false)
    }
    
    override fun loadData() {
        // 懒加载：只有在 Fragment 可见时才会调用
        viewModel.loadData()
    }
    
    override fun onPageVisible() {
        super.onPageVisible()
        // Fragment 在 ViewPager2 中变为可见时调用
    }
    
    override fun onPageInvisible() {
        super.onPageInvisible()
        // Fragment 在 ViewPager2 中变为不可见时调用
    }
}
```

### 可见性回调

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
}
```

---

## 📝 完整示例

### 示例 1：Tab 页面

```kotlin
// 1. 创建适配器
class TabPagerAdapter(activity: FragmentActivity) : ViewPager2Adapter(activity) {
    override fun getItemCount(): Int = 3
    
    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> HomeFragment()
            1 -> CategoryFragment()
            2 -> ProfileFragment()
            else -> throw IllegalArgumentException("Invalid position: $position")
        }
    }
}

// 2. 在 Activity 中使用
class MainActivity : BaseActivity<ActivityMainBinding>() {
    override fun createBinding(): ActivityMainBinding {
        return ActivityMainBinding.inflate(layoutInflater)
    }
    
    override fun setupViews() {
        // 设置适配器
        val adapter = TabPagerAdapter(this)
        binding.viewPager2.adapter = adapter
        
        // 与 TabLayout 联动
        TabLayoutMediator(binding.tabLayout, binding.viewPager2) { tab, position ->
            tab.text = when (position) {
                0 -> "首页"
                1 -> "分类"
                2 -> "我的"
                else -> ""
            }
        }.attach()
    }
}
```

### 示例 2：动态页面列表

```kotlin
class DynamicPagerAdapter(
    activity: FragmentActivity,
    private val pageConfigs: List<PageConfig>
) : ViewPager2Adapter(activity) {
    
    data class PageConfig(
        val title: String,
        val fragmentFactory: () -> Fragment
    )
    
    override fun getItemCount(): Int = pageConfigs.size
    
    override fun createFragment(position: Int): Fragment {
        return pageConfigs[position].fragmentFactory()
    }
    
    fun getPageTitle(position: Int): String {
        return pageConfigs[position].title
    }
}

// 使用
val pageConfigs = listOf(
    DynamicPagerAdapter.PageConfig("首页") { HomeFragment() },
    DynamicPagerAdapter.PageConfig("分类") { CategoryFragment() },
    DynamicPagerAdapter.PageConfig("我的") { ProfileFragment() }
)

val adapter = DynamicPagerAdapter(this, pageConfigs)
binding.viewPager2.adapter = adapter
```

### 示例 3：带参数的 Fragment

```kotlin
class TabPagerAdapter(
    activity: FragmentActivity,
    private val userId: String
) : ViewPager2Adapter(activity) {
    override fun getItemCount(): Int = 3
    
    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> HomeFragment.newInstance(userId)
            1 -> CategoryFragment.newInstance(userId)
            2 -> ProfileFragment.newInstance(userId)
            else -> throw IllegalArgumentException("Invalid position: $position")
        }
    }
}

// Fragment 中
class HomeFragment : ViewPagerFragment<FragmentHomeBinding>() {
    companion object {
        fun newInstance(userId: String): HomeFragment {
            return HomeFragment().apply {
                arguments = bundleOf("userId" to userId)
            }
        }
    }
    
    private val userId: String by lazy {
        arguments?.getString("userId") ?: ""
    }
}
```

### 示例 4：垂直滑动

```kotlin
class VerticalPagerAdapter(activity: FragmentActivity) : ViewPager2Adapter(activity) {
    override fun getItemCount(): Int = 5
    
    override fun createFragment(position: Int): Fragment {
        return PageFragment.newInstance(position)
    }
}

// 使用
val adapter = VerticalPagerAdapter(this)
binding.viewPager2.adapter = adapter
binding.viewPager2.orientation = ViewPager2.ORIENTATION_VERTICAL  // 垂直滑动
```

---

## 🎯 最佳实践

### 1. Fragment 创建

```kotlin
// ✅ 推荐：使用 when 表达式
override fun createFragment(position: Int): Fragment {
    return when (position) {
        0 -> HomeFragment()
        1 -> CategoryFragment()
        else -> throw IllegalArgumentException("Invalid position: $position")
    }
}

// ❌ 不推荐：使用 if-else
override fun createFragment(position: Int): Fragment {
    if (position == 0) return HomeFragment()
    if (position == 1) return CategoryFragment()
    throw IllegalArgumentException("Invalid position: $position")
}
```

### 2. 与 TabLayout 联动

```kotlin
// ✅ 推荐：使用 TabLayoutMediator
TabLayoutMediator(tabLayout, viewPager2) { tab, position ->
    tab.text = getPageTitle(position)
    tab.icon = getPageIcon(position)
}.attach()
```

### 3. 懒加载优化

```kotlin
// ✅ 推荐：使用 ViewPagerFragment 基类
class TabFragment : ViewPagerFragment<FragmentTabBinding>() {
    override fun loadData() {
        // 只有在可见时才加载数据
        viewModel.loadData()
    }
}

// ✅ 推荐：使用 collectOnVisible
override fun setupObservers() {
    viewModel.uiState.collectOnVisible { state ->
        // 只有在可见时才更新 UI
    }
}
```

### 4. 页面转换动画

```kotlin
// 设置页面转换动画
viewPager2.setPageTransformer { page, position ->
    // 自定义转换动画
    page.alpha = 1 - abs(position)
    page.scaleX = 1 - abs(position) * 0.3f
    page.scaleY = 1 - abs(position) * 0.3f
}
```

---

## ⚠️ 注意事项

1. **生命周期**：
   - ViewPager2 使用正常的 Fragment 生命周期
   - Fragment 可见时才会 RESUMED

2. **懒加载**：
   - 所有适配器都与 `BaseFragment` 的懒加载完美集成
   - 使用 `ViewPagerFragment` 基类可以自动启用懒加载

3. **内存管理**：
   - ViewPager2 自动管理 Fragment 生命周期
   - 不可见的 Fragment 会被销毁以节省内存

4. **性能优化**：
   - ViewPager2 基于 RecyclerView，性能优异
   - 支持 DiffUtil，可以高效处理数据集变化

5. **垂直滑动**：
   - ViewPager2 支持垂直滑动
   - 设置 `orientation = ViewPager2.ORIENTATION_VERTICAL`

---

## 🚀 动态适配器（新增）

### DynamicViewPager2Adapter

支持动态添加、删除、替换 Fragment 的适配器。

```kotlin
val adapter = DynamicViewPager2Adapter(this)

// 添加 Fragment
adapter.addFragment(FragmentConfig("home", "HomeFragment") { HomeFragment() })

// 删除 Fragment
adapter.removeFragment(0)

// 批量替换（智能复用相同类型）
adapter.replaceAll(
    listOf(
        FragmentConfig("home", "HomeFragment") { HomeFragment() },
        FragmentConfig("profile", "ProfileFragment") { ProfileFragment() }
    ),
    reuseSameType = true  // 智能复用
)
```

详细使用说明请参考：[动态 ViewPager2 适配器指南](DYNAMIC_VIEWPAGER_GUIDE.md)

---

## ✨ 总结

ViewPager2 适配器提供了：

- ✅ **ViewPager2Adapter**：Activity 中使用（静态）
- ✅ **ViewPager2AdapterWithFragment**：Fragment 中使用（静态）
- ✅ **ViewPager2AdapterWithLifecycle**：自定义生命周期管理（静态）
- ✅ **SimpleViewPager2Adapter**：简化版本（使用 FragmentFactory）
- ✅ **DynamicViewPager2Adapter**：动态管理 Fragment（新增）
- ✅ **DynamicViewPager2AdapterWithFragment**：动态管理 Fragment（Fragment 中使用）
- ✅ **懒加载集成**：与 `BaseFragment` 完美集成
- ✅ **类型安全**：抽象方法确保类型安全
- ✅ **灵活扩展**：支持自定义 Fragment 创建逻辑
- ✅ **智能复用**：自动处理相同类型 Fragment 的复用

**建议：所有新项目使用 ViewPager2！需要动态管理时使用 `DynamicViewPager2Adapter`！**
