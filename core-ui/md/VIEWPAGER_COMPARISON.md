# ViewPager vs ViewPager2 对比分析

## 📊 结论：ViewPager2 更好

**本框架仅支持 ViewPager2**，原因如下：

---

## 🎯 核心对比

| 特性 | ViewPager | ViewPager2 | 优势方 |
|------|-----------|------------|--------|
| **状态** | ⚠️ 已弃用 | ✅ 官方推荐 | ViewPager2 |
| **实现方式** | 自定义 PagerAdapter | 基于 RecyclerView | ViewPager2 |
| **滑动方向** | 仅水平 | 水平 + 垂直 | ViewPager2 |
| **RTL 支持** | ❌ 不支持 | ✅ 原生支持 | ViewPager2 |
| **数据更新** | 有时不生效 | DiffUtil 支持 | ViewPager2 |
| **懒加载** | 默认预加载 | 默认关闭预加载 | ViewPager2 |
| **性能** | 一般 | 更优（基于 RecyclerView） | ViewPager2 |
| **维护状态** | 不再更新 | 持续维护 | ViewPager2 |

---

## 🚀 ViewPager2 的优势

### 1. **官方推荐，持续维护**

- ✅ ViewPager2 是 Google 官方推荐的新版本
- ✅ 持续更新和维护
- ⚠️ ViewPager 已被标记为弃用，不再更新

### 2. **基于 RecyclerView，性能更优**

```kotlin
// ViewPager2 基于 RecyclerView
// 继承了 RecyclerView 的所有优化：
// - 视图回收和复用
// - 高效的滚动性能
// - DiffUtil 支持
```

**性能对比：**
- ViewPager：自定义实现，性能一般
- ViewPager2：基于 RecyclerView，性能更优

### 3. **支持垂直滑动**

```kotlin
// ViewPager2 支持垂直滑动
viewPager2.orientation = ViewPager2.ORIENTATION_VERTICAL

// ViewPager 仅支持水平滑动
// 无法实现垂直滑动
```

### 4. **原生支持 RTL（从右到左）布局**

```kotlin
// ViewPager2 自动支持 RTL
// 适用于阿拉伯语、希伯来语等从右到左的语言

// ViewPager 不支持 RTL
// 需要手动实现，复杂且容易出错
```

### 5. **更好的数据更新机制**

```kotlin
// ViewPager2 使用 DiffUtil
adapter.submitList(newList)  // 自动计算差异，高效更新

// ViewPager 使用 notifyDataSetChanged()
adapter.notifyDataSetChanged()  // 有时不生效，需要手动处理
```

### 6. **默认关闭预加载，节省资源**

```kotlin
// ViewPager2 默认关闭预加载
// 只有在需要时才加载页面，节省内存和 CPU

// ViewPager 默认预加载前后页面
// 可能导致资源浪费
```

### 7. **更强大的页面转换器**

```kotlin
// ViewPager2 支持多个转换器组合
viewPager2.setPageTransformer(compositePageTransformer)

// ViewPager 转换器功能有限
```

### 8. **更好的 Fragment 生命周期管理**

```kotlin
// ViewPager2 使用 FragmentStateAdapter
// 自动管理 Fragment 生命周期，更可靠

// ViewPager 使用 FragmentPagerAdapter/FragmentStatePagerAdapter
// 生命周期管理相对复杂
```

---

## ⚠️ ViewPager 的问题

### 1. **已弃用**

```kotlin
// ViewPager 已被标记为弃用
@Deprecated
class ViewPager : ViewGroup
```

### 2. **功能受限**

- ❌ 仅支持水平滑动
- ❌ 不支持 RTL 布局
- ❌ 数据更新有时不生效
- ❌ 默认预加载，浪费资源

### 3. **不再维护**

- ⚠️ Google 不再更新 ViewPager
- ⚠️ 新功能不会添加到 ViewPager
- ⚠️ 只修复严重的安全问题

---

## 📝 使用建议

### ✅ 新项目：使用 ViewPager2

```kotlin
// 推荐：使用 ViewPager2
class MainActivity : BaseActivity<ActivityMainBinding>() {
    override fun setupViews() {
        val adapter = TabPagerAdapter(this)  // ViewPager2Adapter
        binding.viewPager2.adapter = adapter
    }
}
```

### ⚠️ 旧项目：逐步迁移到 ViewPager2

如果项目中使用 ViewPager，建议：

1. **新功能使用 ViewPager2**
2. **逐步迁移现有页面**
3. **保留 ViewPager 适配器以兼容旧代码**

---

## 🔄 迁移指南

### 从 ViewPager 迁移到 ViewPager2

#### 1. 依赖变更

```kotlin
// 旧版
implementation 'androidx.viewpager:viewpager:1.0.0'

// 新版
implementation 'androidx.viewpager2:viewpager2:1.1.0'
```

#### 2. 布局变更

```xml
<!-- 旧版 -->
<androidx.viewpager.widget.ViewPager
    android:id="@+id/viewPager"
    android:layout_width="match_parent"
    android:layout_height="match_parent" />

<!-- 新版 -->
<androidx.viewpager2.widget.ViewPager2
    android:id="@+id/viewPager2"
    android:layout_width="match_parent"
    android:layout_height="match_parent" />
```

#### 3. 适配器变更

```kotlin
// 旧版
class TabPagerAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm) {
    override fun getItem(position: Int): Fragment { ... }
}

// 新版
class TabPagerAdapter(activity: FragmentActivity) : ViewPager2Adapter(activity) {
    override fun createFragment(position: Int): Fragment { ... }
}
```

#### 4. TabLayout 联动变更

```kotlin
// 旧版
tabLayout.setupWithViewPager(viewPager)

// 新版
TabLayoutMediator(tabLayout, viewPager2) { tab, position ->
    tab.text = getPageTitle(position)
}.attach()
```

---

## 🎯 实际场景建议

### 场景 1：新项目

**✅ 使用 ViewPager2**

```kotlin
// 推荐
val adapter = ViewPager2Adapter(activity)
viewPager2.adapter = adapter
```

### 场景 2：需要垂直滑动

**✅ 必须使用 ViewPager2**

```kotlin
viewPager2.orientation = ViewPager2.ORIENTATION_VERTICAL
```

### 场景 3：需要 RTL 支持

**✅ 必须使用 ViewPager2**

```kotlin
// ViewPager2 自动支持 RTL
// ViewPager 不支持
```

### 场景 4：需要高性能

**✅ 使用 ViewPager2**

```kotlin
// ViewPager2 基于 RecyclerView，性能更优
```

### 场景 5：维护旧代码

**⚠️ 可以继续使用 ViewPager，但建议迁移**

```kotlin
// 框架提供了 ViewPager 适配器以兼容旧代码
// 但新功能建议使用 ViewPager2
```

---

## 📊 性能对比

### 内存使用

- **ViewPager**：默认预加载，内存占用较高
- **ViewPager2**：按需加载，内存占用较低

### 滚动性能

- **ViewPager**：自定义实现，性能一般
- **ViewPager2**：基于 RecyclerView，性能更优

### 数据更新

- **ViewPager**：`notifyDataSetChanged()` 有时不生效
- **ViewPager2**：支持 DiffUtil，高效更新

---

## ✨ 框架支持

**本框架仅支持 ViewPager2**，提供以下适配器：

### ViewPager2 适配器

```kotlin
// ViewPager2Adapter - Activity 中使用
class TabPagerAdapter(activity: FragmentActivity) : ViewPager2Adapter(activity) {
    override fun createFragment(position: Int): Fragment { ... }
}

// ViewPager2AdapterWithFragment - Fragment 中使用
class TabPagerAdapter(fragment: Fragment) : ViewPager2AdapterWithFragment(fragment) {
    override fun createFragment(position: Int): Fragment { ... }
}

// ViewPager2AdapterWithLifecycle - 自定义生命周期
class CustomPagerAdapter(
    fragmentManager: FragmentManager,
    lifecycle: Lifecycle
) : ViewPager2AdapterWithLifecycle(fragmentManager, lifecycle) {
    override fun createFragment(position: Int): Fragment { ... }
}

// SimpleViewPager2Adapter - 简化版本
val adapter = SimpleViewPager2Adapter(this, 3) { position ->
    when (position) {
        0 -> HomeFragment()
        1 -> CategoryFragment()
        2 -> ProfileFragment()
        else -> throw IllegalArgumentException("Invalid position: $position")
    }
}
```

---

## 🎯 最终建议

### ✅ 推荐：ViewPager2

**理由：**
1. ✅ 官方推荐，持续维护
2. ✅ 性能更优（基于 RecyclerView）
3. ✅ 功能更强大（垂直滑动、RTL 支持等）
4. ✅ 更好的数据更新机制
5. ✅ 默认关闭预加载，节省资源
6. ✅ 更好的 Fragment 生命周期管理

### ❌ 不支持：ViewPager

**本框架不支持 ViewPager（旧版）**，原因：
1. ✅ ViewPager2 是官方推荐的新版本
2. ✅ ViewPager2 功能更强大、性能更优
3. ✅ 简化框架，避免维护两套代码
4. ✅ 鼓励使用现代技术栈

---

## 📚 总结

| 项目 | ViewPager | ViewPager2 |
|------|-----------|-------------|
| **推荐度** | ❌ **不支持** | ✅ **框架支持** |
| **新项目** | ❌ 不支持 | ✅ **必须使用** |
| **旧项目** | ❌ 不支持 | ✅ **迁移到 ViewPager2** |
| **性能** | 一般 | **更优** |
| **功能** | 受限 | **更强大** |
| **维护** | 已弃用 | **持续维护** |

**结论：本框架仅支持 ViewPager2，这是更好的选择！**

