package com.jun.core.ui.widget

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.Dimension
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.core.view.updateLayoutParams
import androidx.core.widget.ImageViewCompat
import com.jun.core.ui.R

/**
 * 通用底部 TabBar 组件
 *
 * 特点：
 * - 水平排列的多个 Tab（Icon + 文本，可单独显示）
 * - 支持选中/未选中文本颜色、图标 tint、文字大小、图标尺寸
 * - 提供回调：点击 Tab 时返回 index 和 id
 *
 * 使用方式概览：
 *
 * val items = listOf(
 *     BottomTabItem(id = 0, iconRes = R.drawable.ic_home, title = "首页", backgroundColor = Color.RED),
 *     BottomTabItem(id = 1, iconRes = R.drawable.ic_discover, title = "发现"),
 *     BottomTabItem(id = 2, iconRes = R.drawable.ic_mine, title = "我的")
 * )
 * bottomTabBar.setItems(items)
 * bottomTabBar.setOnTabSelectedListener { index, id -> ... }
 * bottomTabBar.selectTab(0)
 */
class BottomTabBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    data class BottomTabItem(
        val id: Int,
        @DrawableRes val iconRes: Int? = null,
        val title: String? = null,
        val badgeCount: Int = 0,      // 0 不显示，大于 0 显示数字，小于 0 显示小红点
        @ColorInt val backgroundColor: Int? = null  // Tab 背景色，null 表示使用默认背景
    )

    private val inflater = LayoutInflater.from(context)

    private var items: List<BottomTabItem> = emptyList()

    private var selectedIndex: Int = -1

    // 样式配置
    @ColorInt
    private var textColorDefault: Int =
        ContextCompat.getColor(context, R.color.coreui_tab_text_default)

    @ColorInt
    private var textColorSelected: Int =
        ContextCompat.getColor(context, R.color.coreui_tab_text_selected)

    @ColorInt
    private var iconTintDefault: Int =
        ContextCompat.getColor(context, R.color.coreui_tab_icon_default)

    @ColorInt
    private var iconTintSelected: Int =
        ContextCompat.getColor(context, R.color.coreui_tab_icon_selected)

    @Dimension(unit = Dimension.SP)
    private var textSizeSp: Float = 12f

    private var iconSizePx: Int? = null

    private var onTabSelectedListener: ((index: Int, id: Int) -> Unit)? = null

    init {
        orientation = HORIZONTAL
        applyAttributes(attrs)
    }

    private fun applyAttributes(attrs: AttributeSet?) {
        if (attrs == null) return

        val a = context.obtainStyledAttributes(attrs, R.styleable.CoreUiBottomTabBar)
        try {
            if (a.hasValue(R.styleable.CoreUiBottomTabBar_coreui_tabTextColor)) {
                textColorDefault = a.getColor(
                    R.styleable.CoreUiBottomTabBar_coreui_tabTextColor,
                    textColorDefault
                )
            }
            if (a.hasValue(R.styleable.CoreUiBottomTabBar_coreui_tabTextColorSelected)) {
                textColorSelected = a.getColor(
                    R.styleable.CoreUiBottomTabBar_coreui_tabTextColorSelected,
                    textColorSelected
                )
            }
            if (a.hasValue(R.styleable.CoreUiBottomTabBar_coreui_tabIconTint)) {
                iconTintDefault = a.getColor(
                    R.styleable.CoreUiBottomTabBar_coreui_tabIconTint,
                    iconTintDefault
                )
            }
            if (a.hasValue(R.styleable.CoreUiBottomTabBar_coreui_tabIconTintSelected)) {
                iconTintSelected = a.getColor(
                    R.styleable.CoreUiBottomTabBar_coreui_tabIconTintSelected,
                    iconTintSelected
                )
            }
            if (a.hasValue(R.styleable.CoreUiBottomTabBar_coreui_tabTextSize)) {
                textSizeSp = a.getDimension(
                    R.styleable.CoreUiBottomTabBar_coreui_tabTextSize,
                    TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_SP,
                        textSizeSp,
                        resources.displayMetrics
                    )
                ) / resources.displayMetrics.scaledDensity
            }
            if (a.hasValue(R.styleable.CoreUiBottomTabBar_coreui_tabIconSize)) {
                iconSizePx = a.getDimensionPixelSize(
                    R.styleable.CoreUiBottomTabBar_coreui_tabIconSize,
                    0
                ).takeIf { it > 0 }
            }
        } finally {
            a.recycle()
        }
    }

    /**
     * 设置 Tab 列表
     */
    fun setItems(items: List<BottomTabItem>) {
        this.items = items
        buildTabs()
    }

    /**
     * 设置 Tab 选择监听
     */
    fun setOnTabSelectedListener(listener: ((index: Int, id: Int) -> Unit)?) {
        onTabSelectedListener = listener
    }

    /**
     * 选中指定下标的 Tab
     */
    fun selectTab(index: Int) {
        if (index !in items.indices) return
        if (index == selectedIndex) return

        val previousIndex = selectedIndex
        selectedIndex = index
        updateSelection()

        // 添加选中动画效果
        animateTabSelection(index, previousIndex)

        val item = items[index]
        onTabSelectedListener?.invoke(index, item.id)
    }
    
    /**
     * 为选中的 Tab 添加动画效果：先放大1.2倍，再恢复
     * 使用弹性动画，让效果更自然
     */
    private fun animateTabSelection(selectedIndex: Int, previousIndex: Int) {
        val tabView = getChildAt(selectedIndex) ?: return
        
        // 确保初始状态正确
        tabView.scaleX = 1.0f
        tabView.scaleY = 1.0f
        
        // 创建缩放动画：先放大到1.2倍，再恢复
        val scaleX = ObjectAnimator.ofFloat(tabView, "scaleX", 1.0f, 1.2f, 1.0f)
        val scaleY = ObjectAnimator.ofFloat(tabView, "scaleY", 1.0f, 1.2f, 1.0f)
        
        val animatorSet = AnimatorSet().apply {
            duration = 300 // 动画时长300ms
            // 使用加速减速插值器，让动画更自然
            interpolator = android.view.animation.AccelerateDecelerateInterpolator()
            playTogether(scaleX, scaleY)
        }
        
        animatorSet.start()
    }

    private fun buildTabs() {
        removeAllViews()

        items.forEachIndexed { index, item ->
            val tabView = inflater.inflate(
                R.layout.coreui_view_bottom_tab_item,
                this,
                false
            )

            val iconView = tabView.findViewById<ImageView>(R.id.ivIcon)
            val titleView = tabView.findViewById<TextView>(R.id.tvTitle)
            val badgeView = tabView.findViewById<TextView>(R.id.tvBadge)

            // 文本
            if (item.title.isNullOrEmpty()) {
                titleView.visibility = View.GONE
            } else {
                titleView.text = item.title
                titleView.visibility = View.VISIBLE
                titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSp)
            }

            // 图标
            if (item.iconRes != null && item.iconRes != 0) {
                iconView.setImageResource(item.iconRes)
                iconView.visibility = View.VISIBLE
                iconSizePx?.let { size ->
                    val lp = iconView.layoutParams
                    lp.width = size
                    lp.height = size
                    iconView.layoutParams = lp
                }
            } else {
                iconView.visibility = View.GONE
            }

            // Badge
            updateBadgeView(badgeView, item.badgeCount)

            // 背景色
            item.backgroundColor?.let { color ->
                tabView.setBackgroundColor(color)
            }

            tabView.setOnClickListener {
                selectTab(index)
            }

            addView(tabView)
        }

        // 默认选中第一个（如果之前还没选中）
        if (selectedIndex == -1 && items.isNotEmpty()) {
            selectedIndex = 0
        }
        updateSelection()
    }

    private fun updateSelection() {
        for (i in 0 until childCount) {
            val tabView = getChildAt(i)
            val iconView = tabView.findViewById<ImageView>(R.id.ivIcon)
            val titleView = tabView.findViewById<TextView>(R.id.tvTitle)

            val selected = i == selectedIndex
            val textColor = if (selected) textColorSelected else textColorDefault
            val iconTint = if (selected) iconTintSelected else iconTintDefault

            titleView.setTextColor(textColor)

            if (iconView.visibility == View.VISIBLE) {
                ImageViewCompat.setImageTintList(
                    iconView,
                    android.content.res.ColorStateList.valueOf(iconTint)
                )
            }
        }
    }

    /**
     * 设置指定 id 的 badge 数量：
     * - count > 0：显示数字（99 以上可自行在外部控制为 "99+"）
     * - count == 0：隐藏
     * - count < 0：显示小红点（不显示数字）
     */
    fun setBadge(id: Int, count: Int) {
        val index = items.indexOfFirst { it.id == id }
        if (index == -1) return

        val item = items[index]
        items = items.toMutableList().apply {
            this[index] = item.copy(badgeCount = count)
        }

        // 局部更新对应 Tab 的 badge
        val tabView = getChildAt(index)  ?: return
        val badgeView = tabView.findViewById<TextView>(R.id.tvBadge)
        updateBadgeView(badgeView, count)
    }

    /**
     * 清除指定 id 的 badge
     */
    fun clearBadge(id: Int) {
        setBadge(id, 0)
    }

    private fun updateBadgeView(badgeView: TextView, count: Int) {
        when {
            count > 0 -> {
                badgeView.visibility = View.VISIBLE
                badgeView.text = if (count > 99) "99+" else count.toString()
                
                // 固定高度，确保所有 badge 高度一致
                // 注意：背景 drawable 有 padding，所以需要增加高度来容纳文本
                val fixedHeight = (18 * resources.displayMetrics.density).toInt()
                
                // 确保文本垂直居中
                badgeView.gravity = android.view.Gravity.CENTER
                
                if (count < 10) {
                    // 小于10：圆形（固定宽高）
                    badgeView.minWidth = 0
                    badgeView.minHeight = 0
                    // 设置较小的 padding，确保文本居中且不溢出
                    val padding = (3 * resources.displayMetrics.density).toInt()
                    badgeView.setPadding(padding, 0, padding, 0)
                    badgeView.updateLayoutParams {
                        width = fixedHeight
                        height = fixedHeight
                    }
                } else {
                    // 大于等于10：椭圆（宽度自适应，高度固定）
                    // 设置合适的 padding，确保文本有足够空间
                    val padding = (4 * resources.displayMetrics.density).toInt()
                    badgeView.setPadding(padding, 0, padding, 0)
                    badgeView.minWidth = fixedHeight
                    badgeView.minHeight = fixedHeight
                    badgeView.updateLayoutParams {
                        width = android.view.ViewGroup.LayoutParams.WRAP_CONTENT
                        height = fixedHeight
                    }
                }
            }
            count < 0 -> {
                badgeView.visibility = View.VISIBLE
                badgeView.text = ""   // 仅显示红点
                // 小红点模式：设置固定的小尺寸（圆形）
                val dotSize = (8 * resources.displayMetrics.density).toInt()
                badgeView.minWidth = 0
                badgeView.minHeight = 0
                badgeView.setPadding(0, 0, 0, 0)
                // 设置固定宽高，确保是圆形
                badgeView.updateLayoutParams {
                    width = dotSize
                    height = dotSize
                }
            }
            else -> {
                badgeView.visibility = View.GONE
            }
        }
    }
}


