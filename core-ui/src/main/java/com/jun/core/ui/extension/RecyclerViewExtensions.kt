package com.jun.core.ui.extension

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager

/**
 * RecyclerView 扩展函数集合
 */

/**
 * 扩展函数：设置垂直 LinearLayoutManager
 */
fun RecyclerView.setVerticalLayoutManager(
    reverseLayout: Boolean = false
) {
    layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, reverseLayout)
}

/**
 * 扩展函数：设置水平 LinearLayoutManager
 */
fun RecyclerView.setHorizontalLayoutManager(
    reverseLayout: Boolean = false
) {
    layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, reverseLayout)
}

/**
 * 扩展函数：设置 GridLayoutManager
 */
fun RecyclerView.setGridLayoutManager(
    spanCount: Int,
    orientation: Int = RecyclerView.VERTICAL
) {
    layoutManager = GridLayoutManager(context, spanCount, orientation, false)
}

/**
 * 扩展函数：设置 GridLayoutManager（支持多类型 span 配置）
 * 
 * @param spanCount 总列数
 * @param spanSizeLookup 根据 viewType 返回该类型占用的 span 数量
 * @param orientation 方向
 * 
 * 示例：
 * ```kotlin
 * recyclerView.setGridLayoutManagerWithSpan(
 *     spanCount = 4,
 *     spanSizeLookup = { viewType ->
 *         when (viewType) {
 *             0 -> 4  // Header 占满整行
 *             1 -> 2  // 普通 item 占 2 列
 *             2 -> 1  // 小 item 占 1 列
 *             else -> 2
 *         }
 *     }
 * )
 * ```
 */
fun RecyclerView.setGridLayoutManagerWithSpan(
    spanCount: Int,
    spanSizeLookup: (viewType: Int) -> Int,
    orientation: Int = RecyclerView.VERTICAL
) {
    val gridLayoutManager = GridLayoutManager(context, spanCount, orientation, false)
    gridLayoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
        override fun getSpanSize(position: Int): Int {
            val adapter = adapter ?: return spanCount
            val viewType = adapter.getItemViewType(position)
            return spanSizeLookup(viewType)
        }
    }
    layoutManager = gridLayoutManager
}

/**
 * 扩展函数：设置 GridLayoutManager（支持多类型 span 配置，基于 Adapter）
 * 
 * 此方法会自动从 Adapter 获取 viewType，然后根据配置返回 span
 * 
 * @param spanCount 总列数
 * @param spanConfig Map<viewType, spanSize> 配置不同 viewType 的 span 大小
 * @param defaultSpan 默认 span（当 viewType 不在配置中时使用）
 * @param orientation 方向
 * 
 * 示例：
 * ```kotlin
 * recyclerView.setGridLayoutManagerWithSpanConfig(
 *     spanCount = 4,
 *     spanConfig = mapOf(
 *         0 to 4,  // Header 占满整行
 *         1 to 2,  // 普通 item 占 2 列
 *         2 to 1   // 小 item 占 1 列
 *     ),
 *     defaultSpan = 2
 * )
 * ```
 */
fun RecyclerView.setGridLayoutManagerWithSpanConfig(
    spanCount: Int,
    spanConfig: Map<Int, Int>,
    defaultSpan: Int = spanCount,
    orientation: Int = RecyclerView.VERTICAL
) {
    setGridLayoutManagerWithSpan(spanCount, { viewType ->
        spanConfig[viewType] ?: defaultSpan
    }, orientation)
}

/**
 * 扩展函数：设置 StaggeredGridLayoutManager
 */
fun RecyclerView.setStaggeredGridLayoutManager(
    spanCount: Int,
    orientation: Int = StaggeredGridLayoutManager.VERTICAL
) {
    layoutManager = StaggeredGridLayoutManager(spanCount, orientation)
}

/**
 * 扩展函数：设置 StaggeredGridLayoutManager（支持多类型全宽配置）
 * 
 * 某些 viewType（如 Header、Footer）可能需要占满整行
 * 
 * @param spanCount 总列数
 * @param fullSpanViewTypes 需要占满整行的 viewType 集合
 * @param orientation 方向
 * 
 * 示例：
 * ```kotlin
 * recyclerView.setStaggeredGridLayoutManagerWithFullSpan(
 *     spanCount = 2,
 *     fullSpanViewTypes = setOf(0, 2)  // Header 和 Footer 占满整行
 * )
 * ```
 */
fun RecyclerView.setStaggeredGridLayoutManagerWithFullSpan(
    spanCount: Int,
    fullSpanViewTypes: Set<Int> = emptySet(),
    orientation: Int = StaggeredGridLayoutManager.VERTICAL
) {
    val layoutManager = StaggeredGridLayoutManager(spanCount, orientation)
    this.layoutManager = layoutManager
    
    // 如果设置了全宽 viewType，需要监听 adapter 变化来设置
    if (fullSpanViewTypes.isNotEmpty()) {
        val observer = object : RecyclerView.AdapterDataObserver() {
            override fun onChanged() {
                updateFullSpanItems(fullSpanViewTypes)
            }
            
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                updateFullSpanItems(fullSpanViewTypes)
            }
            
            override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
                updateFullSpanItems(fullSpanViewTypes)
            }
            
            override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) {
                updateFullSpanItems(fullSpanViewTypes)
            }
        }
        
        // 如果 adapter 已设置，立即注册并处理
        adapter?.let {
            it.registerAdapterDataObserver(observer)
            post {
                updateFullSpanItems(fullSpanViewTypes)
            }
        } ?: run {
            // 如果 adapter 未设置，在设置 adapter 后处理
            addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
                override fun onViewAttachedToWindow(v: View) {
                    adapter?.let {
                        it.registerAdapterDataObserver(observer)
                        updateFullSpanItems(fullSpanViewTypes)
                    }
                }
                
                override fun onViewDetachedFromWindow(v: View) {}
            })
        }
    }
}

/**
 * 更新全宽 item
 * 通过自定义 LayoutManager 的 LayoutParams 来实现
 */
private fun RecyclerView.updateFullSpanItems(fullSpanViewTypes: Set<Int>) {
    val adapter = adapter ?: return
    val layoutManager = layoutManager as? StaggeredGridLayoutManager ?: return
    
    // 使用 post 确保在布局完成后设置
    post {
        for (i in 0 until adapter.itemCount) {
            val viewType = adapter.getItemViewType(i)
            if (fullSpanViewTypes.contains(viewType)) {
                val viewHolder = findViewHolderForAdapterPosition(i)
                viewHolder?.itemView?.let { view ->
                    val layoutParams = view.layoutParams as? StaggeredGridLayoutManager.LayoutParams
                    if (layoutParams != null && !layoutParams.isFullSpan) {
                        layoutParams.isFullSpan = true
                        view.layoutParams = layoutParams
                    }
                }
            }
        }
    }
}

/**
 * 分割线配置数据类
 */
data class DividerConfig(
    val color: Int = android.graphics.Color.TRANSPARENT,
    val size: Int = 1,  // 单位：dp
    val startPadding: Int = 0,  // 单位：dp
    val endPadding: Int = 0,  // 单位：dp
    val drawable: Drawable? = null,  // 自定义 Drawable（优先级高于 color）
    val excludePositions: Set<Int> = emptySet(),  // 排除的位置（不显示分割线）
    val excludeViewTypes: Set<Int> = emptySet()   // 排除的 viewType（不显示分割线）
)

/**
 * 扩展函数：添加垂直分割线（基础版本）
 * 
 * @param dividerHeight 分割线高度（dp）
 * @param dividerColor 分割线颜色
 */
fun RecyclerView.addVerticalDivider(
    dividerHeight: Int = 1,
    dividerColor: Int = android.graphics.Color.TRANSPARENT
) {
    addVerticalDivider(
        DividerConfig(
            color = dividerColor,
            size = dividerHeight
        )
    )
}

/**
 * 扩展函数：添加水平分割线（基础版本）
 * 
 * @param dividerWidth 分割线宽度（dp）
 * @param dividerColor 分割线颜色
 */
fun RecyclerView.addHorizontalDivider(
    dividerWidth: Int = 1,
    dividerColor: Int = android.graphics.Color.TRANSPARENT
) {
    addHorizontalDivider(
        DividerConfig(
            color = dividerColor,
            size = dividerWidth
        )
    )
}

/**
 * 扩展函数：添加垂直分割线（完整配置）
 * 
 * @param config 分割线配置
 * 
 * 示例：
 * ```kotlin
 * recyclerView.addVerticalDivider(
 *     DividerConfig(
 *         color = Color.GRAY,
 *         size = 1,  // 1dp
 *         startPadding = 16,  // 左边距 16dp
 *         endPadding = 16,    // 右边距 16dp
 *         excludeViewTypes = setOf(0, 2)  // Header 和 Footer 不显示分割线
 *     )
 * )
 * ```
 */
fun RecyclerView.addVerticalDivider(config: DividerConfig) {
    addItemDecoration(createVerticalDivider(config))
}

/**
 * 扩展函数：添加水平分割线（完整配置）
 */
fun RecyclerView.addHorizontalDivider(config: DividerConfig) {
    addItemDecoration(createHorizontalDivider(config))
}

/**
 * 创建垂直分割线
 */
private fun RecyclerView.createVerticalDivider(config: DividerConfig): RecyclerView.ItemDecoration {
    return object : RecyclerView.ItemDecoration() {
        private val paint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
            if (config.drawable != null) {
                // 如果使用 Drawable，不设置颜色
            } else {
                color = config.color
            }
        }
        
        private val dividerSize = (config.size * context.resources.displayMetrics.density).toInt()
        private val startPadding = (config.startPadding * context.resources.displayMetrics.density).toInt()
        private val endPadding = (config.endPadding * context.resources.displayMetrics.density).toInt()
        
        override fun getItemOffsets(
            outRect: Rect,
            view: View,
            parent: RecyclerView,
            state: RecyclerView.State
        ) {
            val position = parent.getChildAdapterPosition(view)
            if (position == RecyclerView.NO_POSITION) return
            
            // 检查是否需要排除
            if (shouldExclude(position, parent)) {
                return
            }
            
            // 只在 item 下方添加偏移（最后一个 item 不添加）
            if (position < state.itemCount - 1) {
                outRect.bottom = dividerSize
            }
        }
        
        override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
            if (config.drawable != null) {
                drawDividerWithDrawable(c, parent, state, true)
            } else {
                drawDividerWithPaint(c, parent, state, true)
            }
        }
        
        private fun drawDividerWithPaint(
            canvas: Canvas,
            parent: RecyclerView,
            state: RecyclerView.State,
            isVertical: Boolean
        ) {
            val childCount = parent.childCount
            for (i in 0 until childCount) {
                val child = parent.getChildAt(i)
                val position = parent.getChildAdapterPosition(child)
                
                if (position == RecyclerView.NO_POSITION || shouldExclude(position, parent)) {
                    continue
                }
                
                // 最后一个 item 不绘制分割线
                if (position >= state.itemCount - 1) {
                    continue
                }
                
                if (isVertical) {
                    val left = child.left + startPadding
                    val right = child.right - endPadding
                    val top = child.bottom
                    val bottom = top + dividerSize
                    canvas.drawRect(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat(), paint)
                } else {
                    val top = child.top + startPadding
                    val bottom = child.bottom - endPadding
                    val left = child.right
                    val right = left + dividerSize
                    canvas.drawRect(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat(), paint)
                }
            }
        }
        
        private fun drawDividerWithDrawable(
            canvas: Canvas,
            parent: RecyclerView,
            state: RecyclerView.State,
            isVertical: Boolean
        ) {
            val drawable = config.drawable ?: return
            val childCount = parent.childCount
            for (i in 0 until childCount) {
                val child = parent.getChildAt(i)
                val position = parent.getChildAdapterPosition(child)
                
                if (position == RecyclerView.NO_POSITION || shouldExclude(position, parent)) {
                    continue
                }
                
                // 最后一个 item 不绘制分割线
                if (position >= state.itemCount - 1) {
                    continue
                }
                
                if (isVertical) {
                    val left = child.left + startPadding
                    val right = child.right - endPadding
                    val top = child.bottom
                    val bottom = top + dividerSize
                    drawable.setBounds(left, top, right, bottom)
                    drawable.draw(canvas)
                } else {
                    val top = child.top + startPadding
                    val bottom = child.bottom - endPadding
                    val left = child.right
                    val right = left + dividerSize
                    drawable.setBounds(left, top, right, bottom)
                    drawable.draw(canvas)
                }
            }
        }
        
        private fun shouldExclude(position: Int, parent: RecyclerView): Boolean {
            // 检查位置
            if (config.excludePositions.contains(position)) {
                return true
            }
            
            // 检查 viewType
            val adapter = parent.adapter ?: return false
            if (position in 0 until adapter.itemCount) {
                val viewType = adapter.getItemViewType(position)
                if (config.excludeViewTypes.contains(viewType)) {
                    return true
                }
            }
            
            return false
        }
    }
}

/**
 * 创建水平分割线
 */
private fun RecyclerView.createHorizontalDivider(config: DividerConfig): RecyclerView.ItemDecoration {
    return object : RecyclerView.ItemDecoration() {
        private val paint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
            if (config.drawable != null) {
                // 如果使用 Drawable，不设置颜色
            } else {
                color = config.color
            }
        }
        
        private val dividerSize = (config.size * context.resources.displayMetrics.density).toInt()
        private val startPadding = (config.startPadding * context.resources.displayMetrics.density).toInt()
        private val endPadding = (config.endPadding * context.resources.displayMetrics.density).toInt()
        
        override fun getItemOffsets(
            outRect: Rect,
            view: View,
            parent: RecyclerView,
            state: RecyclerView.State
        ) {
            val position = parent.getChildAdapterPosition(view)
            if (position == RecyclerView.NO_POSITION) return
            
            // 检查是否需要排除
            if (shouldExclude(position, parent)) {
                return
            }
            
            // 只在 item 右侧添加偏移（最后一个 item 不添加）
            if (position < state.itemCount - 1) {
                outRect.right = dividerSize
            }
        }
        
        override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
            if (config.drawable != null) {
                drawDividerWithDrawable(c, parent, state, false)
            } else {
                drawDividerWithPaint(c, parent, state, false)
            }
        }
        
        private fun drawDividerWithPaint(
            canvas: Canvas,
            parent: RecyclerView,
            state: RecyclerView.State,
            isVertical: Boolean
        ) {
            val childCount = parent.childCount
            for (i in 0 until childCount) {
                val child = parent.getChildAt(i)
                val position = parent.getChildAdapterPosition(child)
                
                if (position == RecyclerView.NO_POSITION || shouldExclude(position, parent)) {
                    continue
                }
                
                // 最后一个 item 不绘制分割线
                if (position >= state.itemCount - 1) {
                    continue
                }
                
                if (isVertical) {
                    val left = child.left + startPadding
                    val right = child.right - endPadding
                    val top = child.bottom
                    val bottom = top + dividerSize
                    canvas.drawRect(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat(), paint)
                } else {
                    val top = child.top + startPadding
                    val bottom = child.bottom - endPadding
                    val left = child.right
                    val right = left + dividerSize
                    canvas.drawRect(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat(), paint)
                }
            }
        }
        
        private fun drawDividerWithDrawable(
            canvas: Canvas,
            parent: RecyclerView,
            state: RecyclerView.State,
            isVertical: Boolean
        ) {
            val drawable = config.drawable ?: return
            val childCount = parent.childCount
            for (i in 0 until childCount) {
                val child = parent.getChildAt(i)
                val position = parent.getChildAdapterPosition(child)
                
                if (position == RecyclerView.NO_POSITION || shouldExclude(position, parent)) {
                    continue
                }
                
                // 最后一个 item 不绘制分割线
                if (position >= state.itemCount - 1) {
                    continue
                }
                
                if (isVertical) {
                    val left = child.left + startPadding
                    val right = child.right - endPadding
                    val top = child.bottom
                    val bottom = top + dividerSize
                    drawable.setBounds(left, top, right, bottom)
                    drawable.draw(canvas)
                } else {
                    val top = child.top + startPadding
                    val bottom = child.bottom - endPadding
                    val left = child.right
                    val right = left + dividerSize
                    drawable.setBounds(left, top, right, bottom)
                    drawable.draw(canvas)
                }
            }
        }
        
        private fun shouldExclude(position: Int, parent: RecyclerView): Boolean {
            // 检查位置
            if (config.excludePositions.contains(position)) {
                return true
            }
            
            // 检查 viewType
            val adapter = parent.adapter ?: return false
            if (position in 0 until adapter.itemCount) {
                val viewType = adapter.getItemViewType(position)
                if (config.excludeViewTypes.contains(viewType)) {
                    return true
                }
            }
            
            return false
        }
    }
}

/**
 * 扩展函数：添加 GridLayoutManager 分割线
 * 
 * @param config 分割线配置
 * @param includeEdge 是否在边缘也显示分割线
 * 
 * 示例：
 * ```kotlin
 * recyclerView.addGridDivider(
 *     DividerConfig(
 *         color = Color.GRAY,
 *         size = 1
 *     ),
 *     includeEdge = true  // 边缘也显示分割线
 * )
 * ```
 */
fun RecyclerView.addGridDivider(
    config: DividerConfig,
    includeEdge: Boolean = false
) {
    addItemDecoration(createGridDivider(config, includeEdge))
}

/**
 * 创建 GridLayoutManager 分割线
 */
private fun RecyclerView.createGridDivider(
    config: DividerConfig,
    includeEdge: Boolean
): RecyclerView.ItemDecoration {
    return object : RecyclerView.ItemDecoration() {
        private val paint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
            if (config.drawable != null) {
                // 如果使用 Drawable，不设置颜色
            } else {
                color = config.color
            }
        }
        
        private val dividerSize = (config.size * context.resources.displayMetrics.density).toInt()
        
        override fun getItemOffsets(
            outRect: Rect,
            view: View,
            parent: RecyclerView,
            state: RecyclerView.State
        ) {
            val position = parent.getChildAdapterPosition(view)
            if (position == RecyclerView.NO_POSITION) return
            
            // 检查是否需要排除
            if (shouldExclude(position, parent)) {
                return
            }
            
            val layoutManager = parent.layoutManager as? GridLayoutManager ?: return
            val spanCount = layoutManager.spanCount
            val column = position % spanCount
            
            // 计算偏移
            val left = if (includeEdge || column > 0) dividerSize else 0
            val top = if (includeEdge || position >= spanCount) dividerSize else 0
            val right = if (includeEdge || column < spanCount - 1) dividerSize else 0
            val bottom = if (includeEdge) dividerSize else 0
            
            outRect.set(left, top, right, bottom)
        }
        
        override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
            if (config.drawable != null) {
                drawGridDividerWithDrawable(c, parent, state, includeEdge)
            } else {
                drawGridDividerWithPaint(c, parent, state, includeEdge)
            }
        }
        
        private fun drawGridDividerWithPaint(
            canvas: Canvas,
            parent: RecyclerView,
            state: RecyclerView.State,
            includeEdge: Boolean
        ) {
            val layoutManager = parent.layoutManager as? GridLayoutManager ?: return
            val spanCount = layoutManager.spanCount
            val childCount = parent.childCount
            
            for (i in 0 until childCount) {
                val child = parent.getChildAt(i)
                val position = parent.getChildAdapterPosition(child)
                
                if (position == RecyclerView.NO_POSITION || shouldExclude(position, parent)) {
                    continue
                }
                
                val column = position % spanCount
                val row = position / spanCount
                
                // 绘制垂直分割线
                if (includeEdge || column < spanCount - 1) {
                    val left = child.right.toFloat()
                    val top = child.top.toFloat()
                    val right = left + dividerSize
                    val bottom = child.bottom.toFloat()
                    canvas.drawRect(left, top, right, bottom, paint)
                }
                
                // 绘制水平分割线
                if (includeEdge || row < (state.itemCount - 1) / spanCount) {
                    val left = child.left.toFloat()
                    val top = child.bottom.toFloat()
                    val right = child.right.toFloat()
                    val bottom = top + dividerSize
                    canvas.drawRect(left, top, right, bottom, paint)
                }
            }
        }
        
        private fun drawGridDividerWithDrawable(
            canvas: Canvas,
            parent: RecyclerView,
            state: RecyclerView.State,
            includeEdge: Boolean
        ) {
            val drawable = config.drawable ?: return
            val layoutManager = parent.layoutManager as? GridLayoutManager ?: return
            val spanCount = layoutManager.spanCount
            val childCount = parent.childCount
            
            for (i in 0 until childCount) {
                val child = parent.getChildAt(i)
                val position = parent.getChildAdapterPosition(child)
                
                if (position == RecyclerView.NO_POSITION || shouldExclude(position, parent)) {
                    continue
                }
                
                val column = position % spanCount
                val row = position / spanCount
                
                // 绘制垂直分割线
                if (includeEdge || column < spanCount - 1) {
                    val left = child.right
                    val top = child.top
                    val right = left + dividerSize
                    val bottom = child.bottom
                    drawable.setBounds(left, top, right, bottom)
                    drawable.draw(canvas)
                }
                
                // 绘制水平分割线
                if (includeEdge || row < (state.itemCount - 1) / spanCount) {
                    val left = child.left
                    val top = child.bottom
                    val right = child.right
                    val bottom = top + dividerSize
                    drawable.setBounds(left, top, right, bottom)
                    drawable.draw(canvas)
                }
            }
        }
        
        private fun shouldExclude(position: Int, parent: RecyclerView): Boolean {
            // 检查位置
            if (config.excludePositions.contains(position)) {
                return true
            }
            
            // 检查 viewType
            val adapter = parent.adapter ?: return false
            if (position in 0 until adapter.itemCount) {
                val viewType = adapter.getItemViewType(position)
                if (config.excludeViewTypes.contains(viewType)) {
                    return true
                }
            }
            
            return false
        }
    }
}

/**
 * 扩展函数：移除所有分割线
 */
fun RecyclerView.removeAllDividers() {
    val decorations = mutableListOf<RecyclerView.ItemDecoration>()
    for (i in 0 until itemDecorationCount) {
        decorations.add(getItemDecorationAt(i))
    }
    decorations.forEach { removeItemDecoration(it) }
}

/**
 * 扩展函数：移除指定类型的分割线
 */
fun RecyclerView.removeDivider(decoration: RecyclerView.ItemDecoration) {
    removeItemDecoration(decoration)
}

/**
 * 扩展函数：滚动到顶部
 */
fun RecyclerView.scrollToTop(smooth: Boolean = true) {
    if (smooth) {
        smoothScrollToPosition(0)
    } else {
        scrollToPosition(0)
    }
}

/**
 * 扩展函数：滚动到底部
 */
fun RecyclerView.scrollToBottom(smooth: Boolean = true) {
    val adapter = adapter ?: return
    val position = adapter.itemCount - 1
    if (position >= 0) {
        if (smooth) {
            smoothScrollToPosition(position)
        } else {
            scrollToPosition(position)
        }
    }
}

/**
 * 扩展函数：检查是否滚动到底部
 */
fun RecyclerView.isScrolledToBottom(threshold: Int = 5): Boolean {
    val layoutManager = layoutManager as? LinearLayoutManager ?: return false
    val lastVisiblePosition = layoutManager.findLastVisibleItemPosition()
    val totalItemCount = adapter?.itemCount ?: 0
    return lastVisiblePosition >= totalItemCount - threshold
}

/**
 * 扩展函数：检查是否滚动到顶部
 */
fun RecyclerView.isScrolledToTop(threshold: Int = 5): Boolean {
    val layoutManager = layoutManager as? LinearLayoutManager ?: return false
    val firstVisiblePosition = layoutManager.findFirstVisibleItemPosition()
    return firstVisiblePosition <= threshold
}

/**
 * ViewHolder 扩展函数：获取适配器位置
 */
fun RecyclerView.ViewHolder.getAdapterPositionSafe(): Int {
    return try {
        bindingAdapterPosition
    } catch (e: Exception) {
        RecyclerView.NO_POSITION
    }
}

/**
 * LayoutInflater 扩展函数：从 ViewGroup 获取 LayoutInflater
 */
fun ViewGroup.inflater(): LayoutInflater {
    return LayoutInflater.from(context)
}

/**
 * ViewGroup 扩展函数：inflate layout
 */
fun ViewGroup.inflate(layoutRes: Int, attachToRoot: Boolean = false): View {
    return LayoutInflater.from(context).inflate(layoutRes, this, attachToRoot)
}

/**
 * DiffUtil 回调基类
 */
abstract class BaseDiffCallback<T>(
    private val oldList: List<T>,
    private val newList: List<T>
) : DiffUtil.Callback() {
    
    override fun getOldListSize(): Int = oldList.size
    
    override fun getNewListSize(): Int = newList.size
    
    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return areItemsTheSame(oldList[oldItemPosition], newList[newItemPosition])
    }
    
    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return areContentsTheSame(oldList[oldItemPosition], newList[newItemPosition])
    }
    
    abstract fun areItemsTheSame(oldItem: T, newItem: T): Boolean
    
    abstract fun areContentsTheSame(oldItem: T, newItem: T): Boolean
}

/**
 * 多类型 LayoutManager 配置辅助类
 * 
 * 用于简化多类型场景下的 LayoutManager 配置
 */
class MultiTypeLayoutManagerConfig {
    
    /**
     * GridLayoutManager 的 span 配置
     */
    data class GridSpanConfig(
        val spanCount: Int,
        val spanSizeLookup: (viewType: Int) -> Int,
        val orientation: Int = RecyclerView.VERTICAL
    )
    
    /**
     * StaggeredGridLayoutManager 的配置
     */
    data class StaggeredConfig(
        val spanCount: Int,
        val fullSpanViewTypes: Set<Int> = emptySet(),
        val orientation: Int = StaggeredGridLayoutManager.VERTICAL
    )
    
    companion object {
        /**
         * 创建常见的多类型 Grid 配置
         * 
         * @param spanCount 总列数
         * @param headerSpan Header 占用的 span（通常为 spanCount，即占满整行）
         * @param itemSpan 普通 item 占用的 span
         * @param footerSpan Footer 占用的 span（通常为 spanCount，即占满整行）
         * @param headerViewType Header 的 viewType
         * @param footerViewType Footer 的 viewType
         */
        fun createCommonGridConfig(
            spanCount: Int,
            headerSpan: Int = spanCount,
            itemSpan: Int = 1,
            footerSpan: Int = spanCount,
            headerViewType: Int = 0,
            footerViewType: Int = 2
        ): GridSpanConfig {
            return GridSpanConfig(
                spanCount = spanCount,
                spanSizeLookup = { viewType ->
                    when (viewType) {
                        headerViewType -> headerSpan
                        footerViewType -> footerSpan
                        else -> itemSpan
                    }
                }
            )
        }
        
        /**
         * 创建常见的多类型 StaggeredGrid 配置
         */
        fun createCommonStaggeredConfig(
            spanCount: Int,
            headerViewType: Int = 0,
            footerViewType: Int = 2
        ): StaggeredConfig {
            return StaggeredConfig(
                spanCount = spanCount,
                fullSpanViewTypes = setOf(headerViewType, footerViewType)
            )
        }
    }
}

/**
 * 扩展函数：使用 MultiTypeLayoutManagerConfig 配置 GridLayoutManager
 */
fun RecyclerView.setGridLayoutManager(config: MultiTypeLayoutManagerConfig.GridSpanConfig) {
    setGridLayoutManagerWithSpan(
        spanCount = config.spanCount,
        spanSizeLookup = config.spanSizeLookup,
        orientation = config.orientation
    )
}

/**
 * 扩展函数：使用 MultiTypeLayoutManagerConfig 配置 StaggeredGridLayoutManager
 */
fun RecyclerView.setStaggeredGridLayoutManager(config: MultiTypeLayoutManagerConfig.StaggeredConfig) {
    setStaggeredGridLayoutManagerWithFullSpan(
        spanCount = config.spanCount,
        fullSpanViewTypes = config.fullSpanViewTypes,
        orientation = config.orientation
    )
}
