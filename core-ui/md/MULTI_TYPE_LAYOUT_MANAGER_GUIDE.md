# 多类型 RecyclerView LayoutManager 配置指南

## 📋 概述

当 RecyclerView 需要显示多类型数据时，LayoutManager 的配置非常重要。不同的 viewType 可能需要不同的布局策略：
- **Header/Footer** 通常需要占满整行
- **普通 Item** 可能需要占用部分列
- **特殊 Item** 可能需要不同的 span 大小

本指南介绍如何使用框架提供的多类型 LayoutManager 配置功能。

---

## 🎯 GridLayoutManager 多类型配置

### 1. 基础用法：使用 spanSizeLookup

```kotlin
recyclerView.setGridLayoutManagerWithSpan(
    spanCount = 4,  // 总共 4 列
    spanSizeLookup = { viewType ->
        when (viewType) {
            0 -> 4  // Header 占满整行（4 列）
            1 -> 2  // 普通 item 占 2 列
            2 -> 1  // 小 item 占 1 列
            3 -> 4  // Footer 占满整行（4 列）
            else -> 2  // 默认占 2 列
        }
    }
)
```

### 2. 使用 Map 配置（更简洁）

```kotlin
recyclerView.setGridLayoutManagerWithSpanConfig(
    spanCount = 4,
    spanConfig = mapOf(
        0 to 4,  // Header 占满整行
        1 to 2,  // 普通 item 占 2 列
        2 to 1,  // 小 item 占 1 列
        3 to 4   // Footer 占满整行
    ),
    defaultSpan = 2  // 未配置的 viewType 默认占 2 列
)
```

### 3. 使用配置类（推荐）

```kotlin
// 创建配置
val config = MultiTypeLayoutManagerConfig.createCommonGridConfig(
    spanCount = 4,
    headerSpan = 4,      // Header 占满整行
    itemSpan = 2,         // 普通 item 占 2 列
    footerSpan = 4,       // Footer 占满整行
    headerViewType = 0,   // Header 的 viewType
    footerViewType = 3    // Footer 的 viewType
)

// 应用配置
recyclerView.setGridLayoutManager(config)
```

---

## 🎯 StaggeredGridLayoutManager 多类型配置

### 1. 基础用法：设置全宽 viewType

```kotlin
recyclerView.setStaggeredGridLayoutManagerWithFullSpan(
    spanCount = 2,
    fullSpanViewTypes = setOf(0, 3)  // Header 和 Footer 占满整行
)
```

### 2. 使用配置类（推荐）

```kotlin
// 创建配置
val config = MultiTypeLayoutManagerConfig.createCommonStaggeredConfig(
    spanCount = 2,
    headerViewType = 0,
    footerViewType = 3
)

// 应用配置
recyclerView.setStaggeredGridLayoutManager(config)
```

---

## 📝 完整示例

### 示例 1：新闻列表（Header + 卡片列表 + Footer）

```kotlin
sealed class NewsItem {
    data class Header(val title: String) : NewsItem()
    data class NewsCard(val news: News) : NewsItem()
    data class Footer(val text: String) : NewsItem()
}

class NewsAdapter : BaseAdapter<NewsItem, ViewBinding>(diffCallback) {
    companion object {
        const val VIEW_TYPE_HEADER = 0
        const val VIEW_TYPE_NEWS = 1
        const val VIEW_TYPE_FOOTER = 2
    }
    
    override fun getItemViewType(item: NewsItem, position: Int): Int {
        return when (item) {
            is NewsItem.Header -> VIEW_TYPE_HEADER
            is NewsItem.NewsCard -> VIEW_TYPE_NEWS
            is NewsItem.Footer -> VIEW_TYPE_FOOTER
        }
    }
    
    // ... 其他实现
}

// 在 Activity/Fragment 中配置
class NewsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        val adapter = NewsAdapter()
        recyclerView.adapter = adapter
        
        // 配置 GridLayoutManager：Header/Footer 占满整行，News 占 2 列
        recyclerView.setGridLayoutManagerWithSpanConfig(
            spanCount = 4,
            spanConfig = mapOf(
                NewsAdapter.VIEW_TYPE_HEADER to 4,  // Header 占满整行
                NewsAdapter.VIEW_TYPE_NEWS to 2,    // News 占 2 列
                NewsAdapter.VIEW_TYPE_FOOTER to 4   // Footer 占满整行
            )
        )
    }
}
```

### 示例 2：商品列表（Banner + 商品网格）

```kotlin
sealed class ProductItem {
    data class Banner(val banners: List<Banner>) : ProductItem()
    data class Product(val product: Product) : ProductItem()
}

class ProductAdapter : BaseAdapter<ProductItem, ViewBinding>(diffCallback) {
    companion object {
        const val VIEW_TYPE_BANNER = 0
        const val VIEW_TYPE_PRODUCT = 1
    }
    
    override fun getItemViewType(item: ProductItem, position: Int): Int {
        return when (item) {
            is ProductItem.Banner -> VIEW_TYPE_BANNER
            is ProductItem.Product -> VIEW_TYPE_PRODUCT
        }
    }
    
    // ... 其他实现
}

// 配置：Banner 占满整行，Product 占 2 列（2x2 网格）
recyclerView.setGridLayoutManagerWithSpanConfig(
    spanCount = 4,
    spanConfig = mapOf(
        ProductAdapter.VIEW_TYPE_BANNER to 4,   // Banner 占满整行
        ProductAdapter.VIEW_TYPE_PRODUCT to 2   // Product 占 2 列
    )
)
```

### 示例 3：瀑布流（Header + 瀑布流内容 + Footer）

```kotlin
sealed class FeedItem {
    data class Header(val title: String) : FeedItem()
    data class FeedContent(val content: Content) : FeedItem()
    data class Footer(val text: String) : FeedItem()
}

class FeedAdapter : BaseAdapter<FeedItem, ViewBinding>(diffCallback) {
    companion object {
        const val VIEW_TYPE_HEADER = 0
        const val VIEW_TYPE_CONTENT = 1
        const val VIEW_TYPE_FOOTER = 2
    }
    
    // ... 实现
}

// 配置：Header 和 Footer 占满整行，Content 使用瀑布流
recyclerView.setStaggeredGridLayoutManagerWithFullSpan(
    spanCount = 2,
    fullSpanViewTypes = setOf(
        FeedAdapter.VIEW_TYPE_HEADER,
        FeedAdapter.VIEW_TYPE_FOOTER
    )
)
```

---

## 🎨 常见布局模式

### 模式 1：Header + 网格 + Footer

```
[========== Header ==========]  (span = 4)
[  Item1  ] [  Item2  ]        (span = 2, span = 2)
[  Item3  ] [  Item4  ]        (span = 2, span = 2)
[========== Footer ==========]  (span = 4)
```

配置：
```kotlin
recyclerView.setGridLayoutManagerWithSpanConfig(
    spanCount = 4,
    spanConfig = mapOf(
        VIEW_TYPE_HEADER to 4,
        VIEW_TYPE_ITEM to 2,
        VIEW_TYPE_FOOTER to 4
    )
)
```

### 模式 2：Banner + 3列网格

```
[========== Banner ==========]  (span = 3)
[ Item1 ] [ Item2 ] [ Item3 ]  (span = 1, span = 1, span = 1)
[ Item4 ] [ Item5 ] [ Item6 ]  (span = 1, span = 1, span = 1)
```

配置：
```kotlin
recyclerView.setGridLayoutManagerWithSpanConfig(
    spanCount = 3,
    spanConfig = mapOf(
        VIEW_TYPE_BANNER to 3,
        VIEW_TYPE_ITEM to 1
    )
)
```

### 模式 3：大图 + 小图混合

```
[========== Big Item ==========]  (span = 4)
[ Small1 ] [ Small2 ] [ Small3 ]  (span = 1, span = 1, span = 1)
[========== Big Item ==========]  (span = 4)
```

配置：
```kotlin
recyclerView.setGridLayoutManagerWithSpanConfig(
    spanCount = 4,
    spanConfig = mapOf(
        VIEW_TYPE_BIG to 4,
        VIEW_TYPE_SMALL to 1
    )
)
```

---

## ⚡ 性能优化建议

### 1. 合理设置 spanCount

- **spanCount 不要太大**：建议 2-6 列，过大会导致 item 过小
- **考虑屏幕尺寸**：可以根据屏幕宽度动态计算 spanCount

```kotlin
fun calculateSpanCount(context: Context, itemMinWidth: Int): Int {
    val screenWidth = context.resources.displayMetrics.widthPixels
    return (screenWidth / itemMinWidth).coerceAtLeast(1)
}

val spanCount = calculateSpanCount(context, 200)  // 每个 item 最小 200dp
recyclerView.setGridLayoutManagerWithSpanConfig(
    spanCount = spanCount,
    spanConfig = mapOf(...)
)
```

### 2. 避免频繁创建 LayoutManager

```kotlin
// ❌ 不好：每次数据变化都创建新的 LayoutManager
fun updateData() {
    recyclerView.setGridLayoutManagerWithSpanConfig(...)
    adapter.submitList(newData)
}

// ✅ 好：只创建一次
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    recyclerView.setGridLayoutManagerWithSpanConfig(...)
}

fun updateData() {
    adapter.submitList(newData)
}
```

### 3. 使用配置类管理

```kotlin
object LayoutManagerConfigs {
    val newsListConfig = MultiTypeLayoutManagerConfig.createCommonGridConfig(
        spanCount = 4,
        headerSpan = 4,
        itemSpan = 2,
        footerSpan = 4,
        headerViewType = NewsAdapter.VIEW_TYPE_HEADER,
        footerViewType = NewsAdapter.VIEW_TYPE_FOOTER
    )
    
    val productGridConfig = MultiTypeLayoutManagerConfig.createCommonGridConfig(
        spanCount = 3,
        headerSpan = 3,
        itemSpan = 1,
        footerSpan = 3,
        headerViewType = ProductAdapter.VIEW_TYPE_BANNER,
        footerViewType = ProductAdapter.VIEW_TYPE_FOOTER
    )
}

// 使用
recyclerView.setGridLayoutManager(LayoutManagerConfigs.newsListConfig)
```

---

## 🔧 与 BaseAdapter 集成

### 在 BaseAdapter 中添加 LayoutManager 配置建议

```kotlin
abstract class BaseAdapter<T, VB : ViewBinding>(
    diffCallback: DiffUtil.ItemCallback<T>
) : ListAdapter<T, BaseAdapter<T, VB>.BaseViewHolder>(diffCallback) {
    
    /**
     * 获取推荐的 LayoutManager 配置（可选）
     * 子类可以重写此方法来提供默认的 LayoutManager 配置建议
     */
    open fun getRecommendedLayoutManagerConfig(): MultiTypeLayoutManagerConfig.GridSpanConfig? {
        return null
    }
}
```

使用：
```kotlin
class NewsAdapter : BaseAdapter<NewsItem, ViewBinding>(diffCallback) {
    override fun getRecommendedLayoutManagerConfig(): MultiTypeLayoutManagerConfig.GridSpanConfig? {
        return MultiTypeLayoutManagerConfig.createCommonGridConfig(
            spanCount = 4,
            headerViewType = VIEW_TYPE_HEADER,
            footerViewType = VIEW_TYPE_FOOTER
        )
    }
}

// 在 Activity/Fragment 中
val config = adapter.getRecommendedLayoutManagerConfig()
if (config != null) {
    recyclerView.setGridLayoutManager(config)
}
```

---

## ✨ 总结

多类型 LayoutManager 配置的关键点：

1. **GridLayoutManager**：使用 `setGridLayoutManagerWithSpan` 或 `setGridLayoutManagerWithSpanConfig`
2. **StaggeredGridLayoutManager**：使用 `setStaggeredGridLayoutManagerWithFullSpan`
3. **配置类**：使用 `MultiTypeLayoutManagerConfig` 简化配置
4. **性能优化**：合理设置 spanCount，避免频繁创建 LayoutManager
5. **与 Adapter 集成**：可以在 Adapter 中提供默认配置建议

**建议：根据实际需求选择合适的配置方式！**


