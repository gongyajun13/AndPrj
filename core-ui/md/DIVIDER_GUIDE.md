# RecyclerView 分割线配置指南

## 📋 概述

框架提供了完善的分割线配置功能，支持：
- ✅ 自定义颜色、大小、边距
- ✅ 自定义 Drawable
- ✅ 排除特定位置或 viewType
- ✅ GridLayoutManager 分割线支持
- ✅ 移除分割线

---

## 🎯 基础用法

### 1. 简单垂直分割线

```kotlin
// 使用默认配置（1dp 透明分割线）
recyclerView.addVerticalDivider()

// 自定义颜色和高度
recyclerView.addVerticalDivider(
    dividerHeight = 1,
    dividerColor = Color.GRAY
)
```

### 2. 简单水平分割线

```kotlin
recyclerView.addHorizontalDivider(
    dividerWidth = 1,
    dividerColor = Color.GRAY
)
```

---

## 🎨 完整配置用法

### 1. 垂直分割线（完整配置）

```kotlin
recyclerView.addVerticalDivider(
    DividerConfig(
        color = Color.GRAY,
        size = 1,              // 1dp
        startPadding = 16,      // 左边距 16dp
        endPadding = 16,       // 右边距 16dp
        excludeViewTypes = setOf(0, 2)  // Header 和 Footer 不显示分割线
    )
)
```

### 2. 水平分割线（完整配置）

```kotlin
recyclerView.addHorizontalDivider(
    DividerConfig(
        color = Color.GRAY,
        size = 1,
        startPadding = 16,     // 上边距 16dp
        endPadding = 16,       // 下边距 16dp
        excludePositions = setOf(0, 5)  // 第 0 和第 5 个位置不显示分割线
    )
)
```

### 3. 使用自定义 Drawable

```kotlin
// 从资源文件加载
val drawable = ContextCompat.getDrawable(context, R.drawable.divider)

recyclerView.addVerticalDivider(
    DividerConfig(
        drawable = drawable,
        size = 2,  // 2dp
        startPadding = 16,
        endPadding = 16
    )
)
```

---

## 📐 GridLayoutManager 分割线

### 基础用法

```kotlin
recyclerView.addGridDivider(
    DividerConfig(
        color = Color.GRAY,
        size = 1
    ),
    includeEdge = false  // 边缘不显示分割线
)
```

### 完整配置

```kotlin
recyclerView.addGridDivider(
    DividerConfig(
        color = Color.GRAY,
        size = 1,
        excludeViewTypes = setOf(0, 2)  // Header 和 Footer 不显示分割线
    ),
    includeEdge = true  // 边缘也显示分割线
)
```

---

## 🎯 DividerConfig 参数说明

| 参数 | 类型 | 说明 | 默认值 |
|------|------|------|--------|
| `color` | Int | 分割线颜色 | `Color.TRANSPARENT` |
| `size` | Int | 分割线大小（dp） | `1` |
| `startPadding` | Int | 起始边距（dp） | `0` |
| `endPadding` | Int | 结束边距（dp） | `0` |
| `drawable` | Drawable? | 自定义 Drawable（优先级高于 color） | `null` |
| `excludePositions` | Set<Int> | 排除的位置集合 | `emptySet()` |
| `excludeViewTypes` | Set<Int> | 排除的 viewType 集合 | `emptySet()` |

---

## 📝 完整示例

### 示例 1：新闻列表（Header + 列表 + Footer）

```kotlin
class NewsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        val adapter = NewsAdapter()
        recyclerView.adapter = adapter
        recyclerView.setVerticalLayoutManager()
        
        // 添加分割线：Header 和 Footer 不显示
        recyclerView.addVerticalDivider(
            DividerConfig(
                color = Color.parseColor("#E0E0E0"),
                size = 1,
                startPadding = 16,  // 左边距 16dp
                endPadding = 16,    // 右边距 16dp
                excludeViewTypes = setOf(
                    NewsAdapter.VIEW_TYPE_HEADER,
                    NewsAdapter.VIEW_TYPE_FOOTER
                )
            )
        )
    }
}
```

### 示例 2：商品网格（带分割线）

```kotlin
class ProductActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        val adapter = ProductAdapter()
        recyclerView.adapter = adapter
        recyclerView.setGridLayoutManager(spanCount = 2)
        
        // 添加网格分割线
        recyclerView.addGridDivider(
            DividerConfig(
                color = Color.parseColor("#E0E0E0"),
                size = 1
            ),
            includeEdge = false  // 边缘不显示分割线
        )
    }
}
```

### 示例 3：多类型列表（不同 viewType 不同处理）

```kotlin
sealed class ListItem {
    data class Header(val title: String) : ListItem()
    data class Content(val text: String) : ListItem()
    data class Footer(val text: String) : ListItem()
}

class MyAdapter : BaseAdapter<ListItem, ViewBinding>(diffCallback) {
    companion object {
        const val VIEW_TYPE_HEADER = 0
        const val VIEW_TYPE_CONTENT = 1
        const val VIEW_TYPE_FOOTER = 2
    }
    
    // ... 实现
}

// 在 Activity/Fragment 中
recyclerView.addVerticalDivider(
    DividerConfig(
        color = Color.GRAY,
        size = 1,
        startPadding = 16,
        endPadding = 16,
        // Header 和 Footer 不显示分割线，只有 Content 之间显示
        excludeViewTypes = setOf(
            MyAdapter.VIEW_TYPE_HEADER,
            MyAdapter.VIEW_TYPE_FOOTER
        )
    )
)
```

### 示例 4：使用自定义 Drawable

```kotlin
// 在 res/drawable/divider_gradient.xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android">
    <gradient
        android:startColor="#E0E0E0"
        android:endColor="#F5F5F5"
        android:angle="0" />
    <size android:height="1dp" />
</shape>

// 在代码中使用
val drawable = ContextCompat.getDrawable(context, R.drawable.divider_gradient)

recyclerView.addVerticalDivider(
    DividerConfig(
        drawable = drawable,
        size = 1,
        startPadding = 16,
        endPadding = 16
    )
)
```

---

## 🔧 移除分割线

### 移除所有分割线

```kotlin
recyclerView.removeAllDividers()
```

### 移除指定分割线

```kotlin
// 保存分割线引用
val divider = recyclerView.createVerticalDivider(
    DividerConfig(color = Color.GRAY, size = 1)
)
recyclerView.addVerticalDivider(divider)

// 后续移除
recyclerView.removeDivider(divider)
```

---

## 💡 最佳实践

### 1. 使用资源颜色

```kotlin
// ❌ 不推荐：硬编码颜色
recyclerView.addVerticalDivider(
    DividerConfig(color = Color.parseColor("#E0E0E0"), size = 1)
)

// ✅ 推荐：使用资源颜色
recyclerView.addVerticalDivider(
    DividerConfig(
        color = ContextCompat.getColor(context, R.color.divider),
        size = 1
    )
)
```

### 2. 统一配置管理

```kotlin
object DividerConfigs {
    val defaultVertical = DividerConfig(
        color = ContextCompat.getColor(context, R.color.divider),
        size = 1,
        startPadding = 16,
        endPadding = 16
    )
    
    val defaultGrid = DividerConfig(
        color = ContextCompat.getColor(context, R.color.divider),
        size = 1
    )
    
    fun forMultiType(headerViewType: Int, footerViewType: Int) = DividerConfig(
        color = ContextCompat.getColor(context, R.color.divider),
        size = 1,
        startPadding = 16,
        endPadding = 16,
        excludeViewTypes = setOf(headerViewType, footerViewType)
    )
}

// 使用
recyclerView.addVerticalDivider(DividerConfigs.defaultVertical)
recyclerView.addGridDivider(DividerConfigs.defaultGrid)
recyclerView.addVerticalDivider(
    DividerConfigs.forMultiType(
        NewsAdapter.VIEW_TYPE_HEADER,
        NewsAdapter.VIEW_TYPE_FOOTER
    )
)
```

### 3. 根据屏幕密度调整大小

```kotlin
fun createDividerConfig(
    context: Context,
    color: Int,
    sizeDp: Int = 1,
    paddingDp: Int = 16
): DividerConfig {
    return DividerConfig(
        color = color,
        size = sizeDp,
        startPadding = paddingDp,
        endPadding = paddingDp
    )
}

// 使用
recyclerView.addVerticalDivider(
    createDividerConfig(context, Color.GRAY, sizeDp = 1, paddingDp = 16)
)
```

### 4. 多类型场景

```kotlin
// 为不同的 viewType 设置不同的分割线样式
class MultiTypeDividerHelper {
    companion object {
        fun createConfigForContent(): DividerConfig {
            return DividerConfig(
                color = Color.GRAY,
                size = 1,
                startPadding = 16,
                endPadding = 16,
                excludeViewTypes = setOf(
                    MyAdapter.VIEW_TYPE_HEADER,
                    MyAdapter.VIEW_TYPE_FOOTER,
                    MyAdapter.VIEW_TYPE_BANNER
                )
            )
        }
        
        fun createConfigForSection(): DividerConfig {
            return DividerConfig(
                color = Color.LTGRAY,
                size = 8,  // 更大的分割线
                startPadding = 0,
                endPadding = 0,
                excludeViewTypes = setOf(
                    MyAdapter.VIEW_TYPE_HEADER,
                    MyAdapter.VIEW_TYPE_FOOTER
                )
            )
        }
    }
}

// 使用
recyclerView.addVerticalDivider(MultiTypeDividerHelper.createConfigForContent())
```

---

## ⚠️ 注意事项

1. **单位说明**：`size`、`startPadding`、`endPadding` 的单位是 **dp**，会自动转换为 px
2. **优先级**：如果同时设置了 `drawable` 和 `color`，`drawable` 优先级更高
3. **性能**：分割线会在每次绘制时计算，避免在滚动时频繁创建新的分割线
4. **GridLayoutManager**：`includeEdge` 参数控制是否在网格边缘也显示分割线
5. **排除规则**：`excludePositions` 和 `excludeViewTypes` 可以同时使用，满足任一条件即排除

---

## ✨ 总结

分割线配置功能提供了：

- ✅ **基础用法**：简单快速添加分割线
- ✅ **完整配置**：支持颜色、大小、边距、Drawable
- ✅ **排除功能**：支持排除特定位置或 viewType
- ✅ **Grid 支持**：专门支持 GridLayoutManager
- ✅ **移除功能**：支持移除分割线

**建议：根据实际需求选择合适的配置方式！**


