# 动态 ViewPager2 适配器使用指南

## 📋 概述

`DynamicViewPager2Adapter` 提供了完整的动态 Fragment 管理功能，支持：
- ✅ 动态添加/删除 Fragment
- ✅ 批量替换 Fragment（智能处理相同类型）
- ✅ Fragment 实例复用（通过 getItemId）
- ✅ 自动管理 Fragment 生命周期

---

## 🎯 FragmentConfig 数据类

### 基本结构

```kotlin
data class FragmentConfig(
    val id: String,        // Fragment 的唯一标识（用于 getItemId）
    val tag: String,        // Fragment 的类型标签（用于识别相同类型）
    val factory: () -> Fragment  // 创建 Fragment 的工厂方法
)
```

### 使用示例

```kotlin
// 创建 Fragment 配置
val homeConfig = FragmentConfig(
    id = "home_1",                    // 唯一 ID
    tag = "HomeFragment",             // Fragment 类型标签
    factory = { HomeFragment() }     // 工厂方法
)

// 或者使用扩展函数简化
fun FragmentConfig(
    id: String,
    fragmentClass: Class<out Fragment>,
    factory: () -> Fragment
): FragmentConfig {
    return FragmentConfig(id, fragmentClass.simpleName, factory)
}
```

---

## 🚀 基本使用

### 1. 创建适配器

```kotlin
class MainActivity : BaseActivity<ActivityMainBinding>() {
    private lateinit var adapter: DynamicViewPager2Adapter
    
    override fun setupViews() {
        adapter = DynamicViewPager2Adapter(this)
        binding.viewPager2.adapter = adapter
    }
}
```

### 2. 添加 Fragment

```kotlin
// 添加单个 Fragment
adapter.addFragment(
    FragmentConfig("home", "HomeFragment") { HomeFragment() }
)

// 在指定位置添加
adapter.addFragment(
    FragmentConfig("category", "CategoryFragment") { CategoryFragment() },
    position = 0  // 插入到第一个位置
)

// 批量添加
adapter.addFragments(
    listOf(
        FragmentConfig("home", "HomeFragment") { HomeFragment() },
        FragmentConfig("category", "CategoryFragment") { CategoryFragment() },
        FragmentConfig("profile", "ProfileFragment") { ProfileFragment() }
    )
)
```

### 3. 删除 Fragment

```kotlin
// 根据位置删除
adapter.removeFragment(0)

// 根据 ID 删除
adapter.removeFragmentById("home")

// 根据 Tag 删除（删除所有相同类型的 Fragment）
adapter.removeFragmentsByTag("HomeFragment")
```

### 4. 替换 Fragment

```kotlin
// 替换指定位置的 Fragment
adapter.replaceFragment(
    0,
    FragmentConfig("new_home", "HomeFragment") { HomeFragment() }
)
```

### 5. 批量替换（智能复用）

```kotlin
// 替换所有 Fragment，智能处理相同类型
adapter.replaceAll(
    listOf(
        FragmentConfig("home", "HomeFragment") { HomeFragment() },
        FragmentConfig("category", "CategoryFragment") { CategoryFragment() },
        FragmentConfig("profile", "ProfileFragment") { ProfileFragment() }
    ),
    reuseSameType = true  // 默认 true，复用相同类型的 Fragment
)
```

---

## 🔄 Fragment 复用机制

### 工作原理

`DynamicViewPager2Adapter` 通过重写 `getItemId()` 方法来实现 Fragment 复用：

1. **getItemId**：使用 `FragmentConfig.id` 的 hashCode 作为 itemId
2. **FragmentStateAdapter**：如果两个位置的 itemId 相同，会自动复用 Fragment 实例
3. **智能替换**：在 `replaceAll()` 时，如果 `reuseSameType = true`，会自动将相同 tag 的 Fragment 使用旧的 id

### 复用示例

```kotlin
// 初始状态
adapter.addFragment(FragmentConfig("home_1", "HomeFragment") { HomeFragment() })
adapter.addFragment(FragmentConfig("category_1", "CategoryFragment") { CategoryFragment() })

// 替换所有（reuseSameType = true）
adapter.replaceAll(
    listOf(
        FragmentConfig("home_2", "HomeFragment") { HomeFragment() },      // tag 相同，会复用
        FragmentConfig("profile_1", "ProfileFragment") { ProfileFragment() }  // 新类型，创建新实例
    ),
    reuseSameType = true
)

// 结果：
// - HomeFragment 会复用（因为 tag 相同，id 会被自动改为 "home_1"）
// - ProfileFragment 会创建新实例（因为 tag 不同）
```

### 不复用示例

```kotlin
// 替换所有（reuseSameType = false）
adapter.replaceAll(
    listOf(
        FragmentConfig("home_2", "HomeFragment") { HomeFragment() }
    ),
    reuseSameType = false  // 不复用，即使 tag 相同也会创建新实例
)
```

---

## 📝 完整示例

### 示例 1：动态 Tab 页面

```kotlin
class MainActivity : BaseActivity<ActivityMainBinding>() {
    private lateinit var adapter: DynamicViewPager2Adapter
    
    override fun setupViews() {
        adapter = DynamicViewPager2Adapter(this)
        binding.viewPager2.adapter = adapter
        
        // 初始添加 Fragment
        adapter.addFragments(
            listOf(
                FragmentConfig("home", "HomeFragment") { HomeFragment() },
                FragmentConfig("category", "CategoryFragment") { CategoryFragment() },
                FragmentConfig("profile", "ProfileFragment") { ProfileFragment() }
            )
        )
        
        // 设置 TabLayout
        setupTabLayout()
    }
    
    private fun setupTabLayout() {
        TabLayoutMediator(binding.tabLayout, binding.viewPager2) { tab, position ->
            val config = adapter.getFragmentConfig(position)
            tab.text = when (config?.tag) {
                "HomeFragment" -> "首页"
                "CategoryFragment" -> "分类"
                "ProfileFragment" -> "我的"
                else -> ""
            }
        }.attach()
    }
    
    // 动态添加新 Tab
    fun addNewTab() {
        adapter.addFragment(
            FragmentConfig("new_tab", "NewFragment") { NewFragment() }
        )
    }
    
    // 删除 Tab
    fun removeTab(position: Int) {
        adapter.removeFragment(position)
    }
}
```

### 示例 2：根据用户权限动态显示页面

```kotlin
class MainActivity : BaseActivity<ActivityMainBinding>() {
    private lateinit var adapter: DynamicViewPager2Adapter
    
    override fun setupViews() {
        adapter = DynamicViewPager2Adapter(this)
        binding.viewPager2.adapter = adapter
        
        // 根据用户权限加载页面
        loadPagesByPermission()
    }
    
    private fun loadPagesByPermission() {
        val pages = mutableListOf<FragmentConfig>()
        
        // 所有用户都有首页
        pages.add(FragmentConfig("home", "HomeFragment") { HomeFragment() })
        
        // 根据权限添加页面
        if (hasCategoryPermission()) {
            pages.add(FragmentConfig("category", "CategoryFragment") { CategoryFragment() })
        }
        
        if (hasProfilePermission()) {
            pages.add(FragmentConfig("profile", "ProfileFragment") { ProfileFragment() })
        }
        
        if (hasAdminPermission()) {
            pages.add(FragmentConfig("admin", "AdminFragment") { AdminFragment() })
        }
        
        adapter.replaceAll(pages, reuseSameType = true)
    }
    
    // 权限变化时更新页面
    fun onPermissionChanged() {
        loadPagesByPermission()
    }
}
```

### 示例 3：智能刷新（保持相同 Fragment）

```kotlin
class MainActivity : BaseActivity<ActivityMainBinding>() {
    private lateinit var adapter: DynamicViewPager2Adapter
    
    fun refreshPages(newPages: List<PageData>) {
        // 将新数据转换为 FragmentConfig
        val newConfigs = newPages.map { pageData ->
            FragmentConfig(
                id = pageData.id,
                tag = pageData.fragmentType,
                factory = { createFragment(pageData) }
            )
        }
        
        // 智能替换：相同类型的 Fragment 会复用
        adapter.replaceAll(newConfigs, reuseSameType = true)
    }
    
    private fun createFragment(pageData: PageData): Fragment {
        return when (pageData.fragmentType) {
            "HomeFragment" -> HomeFragment.newInstance(pageData.data)
            "CategoryFragment" -> CategoryFragment.newInstance(pageData.data)
            else -> throw IllegalArgumentException("Unknown fragment type: ${pageData.fragmentType}")
        }
    }
}
```

### 示例 4：Fragment 位置移动

```kotlin
// 将第一个 Fragment 移动到最后一个位置
adapter.moveFragment(0, adapter.fragments.size - 1)

// 将最后一个 Fragment 移动到第一个位置
adapter.moveFragment(adapter.fragments.size - 1, 0)
```

### 示例 5：查询和检查

```kotlin
// 获取指定位置的配置
val config = adapter.getFragmentConfig(0)

// 根据 ID 获取配置
val configById = adapter.getFragmentConfigById("home")

// 获取指定 ID 的位置
val position = adapter.getPositionById("home")

// 检查是否包含指定 ID
if (adapter.contains("home")) {
    // Fragment 存在
}

// 获取所有 Fragment 配置
val allConfigs = adapter.fragments
```

---

## ⚠️ 注意事项

### 1. Fragment ID 的重要性

```kotlin
// ✅ 正确：使用唯一且稳定的 ID
FragmentConfig("home_1", "HomeFragment") { HomeFragment() }

// ❌ 错误：使用随机 ID（会导致无法复用）
FragmentConfig(UUID.randomUUID().toString(), "HomeFragment") { HomeFragment() }
```

### 2. Fragment Tag 的作用

```kotlin
// Tag 用于识别 Fragment 类型，相同 tag 的 Fragment 在 replaceAll 时可以复用
// 但前提是 reuseSameType = true 且 id 相同（或自动匹配）

// ✅ 相同 tag，可以复用
FragmentConfig("home_1", "HomeFragment") { HomeFragment() }
FragmentConfig("home_1", "HomeFragment") { HomeFragment() }  // 复用

// ❌ 不同 tag，不会复用
FragmentConfig("home_1", "HomeFragment") { HomeFragment() }
FragmentConfig("home_2", "NewHomeFragment") { HomeFragment() }  // 不复用
```

### 3. replaceAll 的复用机制

```kotlin
// reuseSameType = true 时：
// 1. 查找新配置中每个 Fragment 的 tag
// 2. 在旧配置中查找相同 tag 的 Fragment
// 3. 如果找到，使用旧的 id（这样 getItemId 相同，会复用 Fragment）
// 4. 如果没找到，使用新的 id（创建新 Fragment）

// 因此，如果希望复用 Fragment，确保：
// - reuseSameType = true
// - 新旧 Fragment 的 tag 相同
```

### 4. Fragment 生命周期

```kotlin
// ViewPager2 会自动管理 Fragment 生命周期
// 不需要手动处理 Fragment 的创建和销毁
// 只需要管理 FragmentConfig 列表即可
```

### 5. 与 TabLayout 联动

```kotlin
// 当动态添加/删除 Fragment 时，需要重新设置 TabLayout
adapter.addFragment(config)
// TabLayout 需要重新 attach 或手动更新
TabLayoutMediator(tabLayout, viewPager2) { tab, position ->
    // 更新 tab
}.attach()
```

---

## 🎯 最佳实践

### 1. 使用稳定的 ID

```kotlin
// ✅ 推荐：使用有意义的稳定 ID
FragmentConfig("home", "HomeFragment") { HomeFragment() }
FragmentConfig("category", "CategoryFragment") { CategoryFragment() }

// ❌ 不推荐：使用随机或临时 ID
FragmentConfig(System.currentTimeMillis().toString(), "HomeFragment") { HomeFragment() }
```

### 2. 批量操作

```kotlin
// ✅ 推荐：批量添加/替换
adapter.addFragments(configs)
adapter.replaceAll(newConfigs)

// ❌ 不推荐：循环单个添加
configs.forEach { adapter.addFragment(it) }  // 会导致多次 notify
```

### 3. 智能复用

```kotlin
// ✅ 推荐：在需要保持 Fragment 状态时使用 reuseSameType = true
adapter.replaceAll(newConfigs, reuseSameType = true)

// ✅ 推荐：在需要完全刷新时使用 reuseSameType = false
adapter.replaceAll(newConfigs, reuseSameType = false)
```

### 4. 错误处理

```kotlin
// ✅ 推荐：检查操作结果
val removed = adapter.removeFragment(0)
if (removed != null) {
    // 删除成功
} else {
    // 位置无效
}

// ✅ 推荐：检查位置有效性
if (position in 0 until adapter.fragments.size) {
    adapter.replaceFragment(position, newConfig)
}
```

---

## 🔧 高级用法

### 1. 自定义 Fragment 创建

```kotlin
// 使用工厂方法创建带参数的 Fragment
val config = FragmentConfig(
    id = "user_detail",
    tag = "UserDetailFragment",
    factory = {
        UserDetailFragment.newInstance(userId, userName)
    }
)
```

### 2. 条件添加

```kotlin
// 根据条件动态添加 Fragment
val configs = mutableListOf<FragmentConfig>()

if (showHome) {
    configs.add(FragmentConfig("home", "HomeFragment") { HomeFragment() })
}

if (showCategory) {
    configs.add(FragmentConfig("category", "CategoryFragment") { CategoryFragment() })
}

adapter.replaceAll(configs)
```

### 3. 监听 Fragment 变化

```kotlin
// 可以结合 ViewPager2 的 OnPageChangeCallback 监听页面变化
binding.viewPager2.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
    override fun onPageSelected(position: Int) {
        val config = adapter.getFragmentConfig(position)
        // 处理页面选择
    }
})
```

---

## ✨ 总结

`DynamicViewPager2Adapter` 提供了：

- ✅ **动态管理**：添加、删除、替换 Fragment
- ✅ **智能复用**：通过 getItemId 自动复用相同类型的 Fragment
- ✅ **批量操作**：支持批量添加和替换
- ✅ **灵活查询**：根据 ID、Tag、位置查询 Fragment
- ✅ **位置移动**：支持 Fragment 位置调整
- ✅ **类型安全**：使用 FragmentConfig 确保类型安全

**建议：需要动态管理 Fragment 时使用 `DynamicViewPager2Adapter`！**


