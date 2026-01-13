package com.jun.core.ui.widget

import android.content.Context
import android.graphics.Color
import android.graphics.PorterDuff
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
import androidx.annotation.FontRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import com.jun.core.ui.R

/**
 * 一个可高度配置的居中标题 Toolbar：
 *
 * - 左侧：支持图标和文字，两者可单独显示或组合显示
 * - 中间：标题始终几何居中（不受左右菜单影响）
 * - 右侧：支持图标和文字
 *
 * 可配置内容：
 * - 左/中/右文字：文案、字号、颜色、字体
 * - 左/右图标：资源、尺寸、tint 颜色
 *
 * 建议作为 AppBarLayout 内部子 View 使用。
 *
 * 使用示例：
 * ```kotlin
 * // XML 中
 * <com.jun.core.ui.widget.CenterToolbar
 *     android:layout_width="match_parent"
 *     android:layout_height="?attr/actionBarSize"
 *     app:coreui_titleText="标题"
 *     app:coreui_leftIcon="@drawable/ic_back"
 *     app:coreui_rightIcon="@drawable/ic_more"
 *     app:coreui_backgroundColor="@color/primary" />
 *
 * // 代码中
 * toolbar.setupSimple(
 *     title = "标题",
 *     leftIcon = R.drawable.ic_back,
 *     onLeftClick = { finish() },
 *     rightIcon = R.drawable.ic_more,
 *     onRightClick = { showMenu() }
 * )
 * ```
 */
class CenterToolbar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val leftContainer: LinearLayout
    private val leftIconView: ImageView
    private val leftTextView: TextView

    private val rightContainer: LinearLayout
    private val rightTextView: TextView
    private val rightIconView: ImageView

    private val titleView: TextView

    // 保存原始高度，用于调整
    private var originalHeight: Int = -1
    // 缓存状态栏高度
    private var cachedStatusBarHeight: Int = -1

    init {
        LayoutInflater.from(context).inflate(R.layout.coreui_view_center_toolbar, this, true)

        leftContainer = findViewById(R.id.llLeftContainer)
        leftIconView = findViewById(R.id.ivLeftIcon)
        leftTextView = findViewById(R.id.tvLeftText)

        rightContainer = findViewById(R.id.llRightContainer)
        rightTextView = findViewById(R.id.tvRightText)
        rightIconView = findViewById(R.id.ivRightIcon)
        titleView = findViewById(R.id.tvTitle)
        setBackgroundColor(Color.WHITE)
        // 处理其他属性
        applyAttributes(attrs)

        // 设置 WindowInsets 监听，获取状态栏高度并调整 toolbar 高度
        ViewCompat.setOnApplyWindowInsetsListener(this) { _, insets ->
            handleWindowInsets(insets)
            insets
        }

    }
    
    /**
     * 设置背景色
     * @param color 背景颜色
     */
    fun setToolbarBackgroundColor(@ColorInt color: Int) {
        setBackgroundColor(color)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        
        // 保存原始高度
        saveOriginalHeight()
        
        // 请求应用 WindowInsets
        ViewCompat.requestApplyInsets(this)
        
        // 延迟检查，如果 WindowInsets 没有传递，尝试从资源获取状态栏高度
        post {
            if (cachedStatusBarHeight <= 0) {
                val statusBarHeight = getStatusBarHeightFromResource()
                if (statusBarHeight > 0) {
                    cachedStatusBarHeight = statusBarHeight
                    adjustToolbarHeight(statusBarHeight)
                }
            }
        }
    }
    
    /**
     * 保存原始高度
     */
    private fun saveOriginalHeight() {
        if (originalHeight < 0) {
            // 优先从 layoutParams.height 获取（XML 中设置的值）
            if (layoutParams.height > 0) {
                originalHeight = layoutParams.height
            } else {
                // 如果 layoutParams.height 为 0 或负数，尝试从实际高度获取
                originalHeight = height
            }
            
            // 如果还是 0 或负数，尝试获取 actionBarSize
            if (originalHeight <= 0) {
                val typedValue = TypedValue()
                if (context.theme.resolveAttribute(android.R.attr.actionBarSize, typedValue, true)) {
                    originalHeight = TypedValue.complexToDimensionPixelSize(
                        typedValue.data,
                        resources.displayMetrics
                    )
                }
            }
            
            // 如果还是 0，使用默认值 56dp
            if (originalHeight <= 0) {
                originalHeight = (56 * resources.displayMetrics.density).toInt()
            }
        }
    }
    
    /**
     * 处理 WindowInsets，获取状态栏高度并调整 toolbar 高度
     */
    private fun handleWindowInsets(insets: WindowInsetsCompat) {
        val systemBarsInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        var statusBarHeight = systemBarsInsets.top
        
        // 如果 WindowInsets 中的状态栏高度为 0，尝试从资源获取
        if (statusBarHeight == 0) {
            statusBarHeight = getStatusBarHeightFromResource()
        }
        
        // 缓存状态栏高度（使用较大的值，确保准确性）
        if (statusBarHeight > 0 && statusBarHeight > cachedStatusBarHeight) {
            cachedStatusBarHeight = statusBarHeight
        }
        
        // 如果状态栏高度大于 0，调整 toolbar 高度
        if (statusBarHeight > 0) {
            adjustToolbarHeight(statusBarHeight)
        }
    }
    
    /**
     * 调整 Toolbar paddingTop = 状态栏高度，确保内容区域保持在原位置
     * 这样总高度增加了（通过 paddingTop），但内容区域保持在原来的位置
     */
    private fun adjustToolbarHeight(statusBarHeight: Int) {
        if (statusBarHeight <= 0) return
        
        // 设置 paddingTop = 状态栏高度，让内容区域向下偏移，顶部留出状态栏空间
        if (paddingTop != statusBarHeight) {
            setPadding(paddingLeft, statusBarHeight, paddingRight, paddingBottom)
        }
    }
    
    /**
     * 从资源获取状态栏高度（备选方案，当 WindowInsets 未应用时使用）
     */
    private fun getStatusBarHeightFromResource(): Int {
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (resourceId > 0) {
            resources.getDimensionPixelSize(resourceId)
        } else {
            0
        }
    }

    private fun applyAttributes(attrs: AttributeSet?) {
        if (attrs == null) return

        val a = context.obtainStyledAttributes(attrs, R.styleable.CoreUiCenterToolbar)
        try {
            // 左侧文字
            a.getString(R.styleable.CoreUiCenterToolbar_coreui_leftText)?.let { setLeftText(it) }
            // 左侧图标
            a.getResourceId(R.styleable.CoreUiCenterToolbar_coreui_leftIcon, 0)
                .takeIf { it != 0 }?.let { setLeftIcon(it) }

            // 右侧文字
            a.getString(R.styleable.CoreUiCenterToolbar_coreui_rightText)?.let { setRightText(it) }
            // 右侧图标
            a.getResourceId(R.styleable.CoreUiCenterToolbar_coreui_rightIcon, 0)
                .takeIf { it != 0 }?.let { setRightIcon(it) }

            // 标题
            a.getString(R.styleable.CoreUiCenterToolbar_coreui_titleText)?.let { setTitleText(it) }

            // 文字大小
            if (a.hasValue(R.styleable.CoreUiCenterToolbar_coreui_leftTextSize)) {
                leftTextView.setTextSize(
                    TypedValue.COMPLEX_UNIT_PX,
                    a.getDimension(R.styleable.CoreUiCenterToolbar_coreui_leftTextSize, leftTextView.textSize)
                )
            }
            if (a.hasValue(R.styleable.CoreUiCenterToolbar_coreui_titleTextSize)) {
                titleView.setTextSize(
                    TypedValue.COMPLEX_UNIT_PX,
                    a.getDimension(R.styleable.CoreUiCenterToolbar_coreui_titleTextSize, titleView.textSize)
                )
            }
            if (a.hasValue(R.styleable.CoreUiCenterToolbar_coreui_rightTextSize)) {
                rightTextView.setTextSize(
                    TypedValue.COMPLEX_UNIT_PX,
                    a.getDimension(R.styleable.CoreUiCenterToolbar_coreui_rightTextSize, rightTextView.textSize)
                )
            }

            // 文字颜色
            if (a.hasValue(R.styleable.CoreUiCenterToolbar_coreui_leftTextColor)) {
                leftTextView.setTextColor(
                    a.getColor(
                        R.styleable.CoreUiCenterToolbar_coreui_leftTextColor,
                        leftTextView.currentTextColor
                    )
                )
            }
            if (a.hasValue(R.styleable.CoreUiCenterToolbar_coreui_titleTextColor)) {
                titleView.setTextColor(
                    a.getColor(
                        R.styleable.CoreUiCenterToolbar_coreui_titleTextColor,
                        titleView.currentTextColor
                    )
                )
            }
            if (a.hasValue(R.styleable.CoreUiCenterToolbar_coreui_rightTextColor)) {
                rightTextView.setTextColor(
                    a.getColor(
                        R.styleable.CoreUiCenterToolbar_coreui_rightTextColor,
                        rightTextView.currentTextColor
                    )
                )
            }

            // 字体
            a.getResourceId(R.styleable.CoreUiCenterToolbar_coreui_leftTextFont, 0)
                .takeIf { it != 0 }?.let { setLeftTextFont(it) }
            a.getResourceId(R.styleable.CoreUiCenterToolbar_coreui_titleTextFont, 0)
                .takeIf { it != 0 }?.let { setTitleTextFont(it) }
            a.getResourceId(R.styleable.CoreUiCenterToolbar_coreui_rightTextFont, 0)
                .takeIf { it != 0 }?.let { setRightTextFont(it) }

            // 图标尺寸
            if (a.hasValue(R.styleable.CoreUiCenterToolbar_coreui_leftIconSize)) {
                val size = a.getDimensionPixelSize(
                    R.styleable.CoreUiCenterToolbar_coreui_leftIconSize,
                    leftIconView.layoutParams.width
                )
                setLeftIconSize(size, size)
            }
            if (a.hasValue(R.styleable.CoreUiCenterToolbar_coreui_rightIconSize)) {
                val size = a.getDimensionPixelSize(
                    R.styleable.CoreUiCenterToolbar_coreui_rightIconSize,
                    rightIconView.layoutParams.width
                )
                setRightIconSize(size, size)
            }

            // 图标 tint
            if (a.hasValue(R.styleable.CoreUiCenterToolbar_coreui_leftIconTint)) {
                val tint = a.getColor(
                    R.styleable.CoreUiCenterToolbar_coreui_leftIconTint,
                    0
                )
                if (tint != 0) setLeftIconTint(tint)
            }
            if (a.hasValue(R.styleable.CoreUiCenterToolbar_coreui_rightIconTint)) {
                val tint = a.getColor(
                    R.styleable.CoreUiCenterToolbar_coreui_rightIconTint,
                    0
                )
                if (tint != 0) setRightIconTint(tint)
            }

            // 背景色
            if (a.hasValue(R.styleable.CoreUiCenterToolbar_coreui_backgroundColor)) {
                setBackgroundColor(a.getColor(R.styleable.CoreUiCenterToolbar_coreui_backgroundColor, Color.WHITE))
            }
        } finally {
            a.recycle()
        }
    }

    /* -------------------- 标题相关 -------------------- */

    fun setTitleText(text: CharSequence?) {
        titleView.text = text
    }

    fun setTitleTextColor(@ColorInt color: Int) {
        titleView.setTextColor(color)
    }

    fun setTitleTextSize(@Dimension(unit = Dimension.SP) sizeSp: Float) {
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp)
    }

    fun setTitleTextFont(@FontRes fontRes: Int) {
        val typeface = ResourcesCompat.getFont(context, fontRes)
        if (typeface != null) {
            titleView.typeface = typeface
        }
    }

    /* -------------------- 左侧区域 -------------------- */

    fun setLeftText(text: CharSequence?) {
        leftTextView.text = text
        leftTextView.isVisible = !text.isNullOrEmpty()
        updateLeftContainerVisibility()
    }

    fun setLeftTextColor(@ColorInt color: Int) {
        leftTextView.setTextColor(color)
    }

    fun setLeftTextSize(@Dimension(unit = Dimension.SP) sizeSp: Float) {
        leftTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp)
    }

    fun setLeftTextFont(@FontRes fontRes: Int) {
        val typeface = ResourcesCompat.getFont(context, fontRes)
        if (typeface != null) {
            leftTextView.typeface = typeface
        }
    }

    fun setLeftIcon(@DrawableRes resId: Int) {
        if (resId == 0) {
            leftIconView.setImageDrawable(null)
            leftIconView.visibility = View.GONE
        } else {
            leftIconView.setImageResource(resId)
            leftIconView.visibility = View.VISIBLE
        }
        updateLeftContainerVisibility()
    }

    fun setLeftIconSize(widthPx: Int, heightPx: Int) {
        leftIconView.updateLayoutParams {
            width = widthPx
            height = heightPx
        }
    }

    fun setLeftIconTint(@ColorInt color: Int) {
        leftIconView.setColorFilter(color, PorterDuff.Mode.SRC_IN)
    }

    fun setOnLeftClickListener(listener: OnClickListener?) {
        leftContainer.setOnClickListener(listener)
    }

    private fun updateLeftContainerVisibility() {
        leftContainer.isVisible = leftIconView.isVisible || leftTextView.isVisible
    }

    /* -------------------- 右侧区域 -------------------- */

    fun setRightText(text: CharSequence?) {
        rightTextView.text = text
        rightTextView.isVisible = !text.isNullOrEmpty()
        updateRightContainerVisibility()
    }

    fun setRightTextColor(@ColorInt color: Int) {
        rightTextView.setTextColor(color)
    }

    fun setRightTextSize(@Dimension(unit = Dimension.SP) sizeSp: Float) {
        rightTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp)
    }

    fun setRightTextFont(@FontRes fontRes: Int) {
        val typeface = ResourcesCompat.getFont(context, fontRes)
        if (typeface != null) {
            rightTextView.typeface = typeface
        }
    }

    fun setRightIcon(@DrawableRes resId: Int) {
        if (resId == 0) {
            rightIconView.setImageDrawable(null)
            rightIconView.visibility = View.GONE
        } else {
            rightIconView.setImageResource(resId)
            rightIconView.visibility = View.VISIBLE
        }
        updateRightContainerVisibility()
    }

    fun setRightIconSize(widthPx: Int, heightPx: Int) {
        rightIconView.updateLayoutParams {
            width = widthPx
            height = heightPx
        }
    }

    fun setRightIconTint(@ColorInt color: Int) {
        rightIconView.setColorFilter(color, PorterDuff.Mode.SRC_IN)
    }

    fun setOnRightClickListener(listener: OnClickListener?) {
        rightContainer.setOnClickListener(listener)
    }

    private fun updateRightContainerVisibility() {
        rightContainer.isVisible = rightIconView.isVisible || rightTextView.isVisible
    }

    /* -------------------- 简化配置 API -------------------- */

    /**
     * 常用场景的一站式完整配置：
     *
     * - 标题：文案 / 大小 / 颜色 / 字体
     * - 左侧：文字 / 文字大小 / 文字颜色 / 字体 / 图标 / 图标尺寸 / 图标 tint / 点击
     * - 右侧：文字 / 文字大小 / 文字颜色 / 字体 / 图标 / 图标尺寸 / 图标 tint / 点击
     * - 背景色：Toolbar 背景颜色
     *
     * 所有参数都是可选的，不传则不修改当前配置（可以沿用 XML 中的属性）。
     */
    fun setupSimple(
        // 标题
        title: CharSequence? = null,
        @Dimension(unit = Dimension.SP) titleTextSizeSp: Float? = null,
        @ColorInt titleTextColor: Int? = null,
        @FontRes titleTextFont: Int? = null,

        // 左侧
        leftText: CharSequence? = null,
        @Dimension(unit = Dimension.SP) leftTextSizeSp: Float? = null,
        @ColorInt leftTextColor: Int? = null,
        @FontRes leftTextFont: Int? = null,
        @DrawableRes leftIcon: Int? = null,
        leftIconWidthPx: Int? = null,
        leftIconHeightPx: Int? = null,
        @ColorInt leftIconTint: Int? = null,
        onLeftClick: (() -> Unit)? = null,

        // 右侧
        rightText: CharSequence? = null,
        @Dimension(unit = Dimension.SP) rightTextSizeSp: Float? = null,
        @ColorInt rightTextColor: Int? = null,
        @FontRes rightTextFont: Int? = null,
        @DrawableRes rightIcon: Int? = null,
        rightIconWidthPx: Int? = null,
        rightIconHeightPx: Int? = null,
        @ColorInt rightIconTint: Int? = null,
        onRightClick: (() -> Unit)? = null,

        // 背景色
        @ColorInt backgroundColor: Int? = null
    ) {
        // 标题
        title?.let { setTitleText(it) }
        titleTextSizeSp?.let { setTitleTextSize(it) }
        titleTextColor?.let { setTitleTextColor(it) }
        titleTextFont?.let { setTitleTextFont(it) }

        // 左侧文字
        leftText?.let { setLeftText(it) }
        leftTextSizeSp?.let { setLeftTextSize(it) }
        leftTextColor?.let { setLeftTextColor(it) }
        leftTextFont?.let { setLeftTextFont(it) }

        // 左侧图标
        leftIcon?.let { setLeftIcon(it) }
        if (leftIconWidthPx != null && leftIconHeightPx != null) {
            setLeftIconSize(leftIconWidthPx, leftIconHeightPx)
        }
        leftIconTint?.let { setLeftIconTint(it) }

        setOnLeftClickListener(
            if (onLeftClick != null) {
                OnClickListener { onLeftClick() }
            } else null
        )

        // 右侧文字
        rightText?.let { setRightText(it) }
        rightTextSizeSp?.let { setRightTextSize(it) }
        rightTextColor?.let { setRightTextColor(it) }
        rightTextFont?.let { setRightTextFont(it) }

        // 右侧图标
        rightIcon?.let { setRightIcon(it) }
        if (rightIconWidthPx != null && rightIconHeightPx != null) {
            setRightIconSize(rightIconWidthPx, rightIconHeightPx)
        }
        rightIconTint?.let { setRightIconTint(it) }

        setOnRightClickListener(
            if (onRightClick != null) {
                OnClickListener { onRightClick() }
            } else null
        )

        // 背景色
        backgroundColor?.let { setToolbarBackgroundColor(it) }
    }

    /* -------------------- 组件 View 获取 -------------------- */

    /**
     * 获取标题 TextView（用于自定义动画、样式等）
     */
    fun getTitleView(): TextView = titleView

    /**
     * 获取左侧整体容器（用于自定义布局、动画等）
     */
    fun getLeftContainer(): LinearLayout = leftContainer

    /**
     * 获取左侧图标 View
     */
    fun getLeftIconView(): ImageView = leftIconView

    /**
     * 获取左侧文字 View
     */
    fun getLeftTextView(): TextView = leftTextView

    /**
     * 获取右侧整体容器
     */
    fun getRightContainer(): LinearLayout = rightContainer

    /**
     * 获取右侧图标 View
     */
    fun getRightIconView(): ImageView = rightIconView

    /**
     * 获取右侧文字 View
     */
    fun getRightTextView(): TextView = rightTextView

    /* -------------------- 便捷方法 -------------------- */

    /**
     * 清除左侧内容（图标和文字）
     */
    fun clearLeft() {
        setLeftIcon(0)
        setLeftText(null)
    }

    /**
     * 清除右侧内容（图标和文字）
     */
    fun clearRight() {
        setRightIcon(0)
        setRightText(null)
    }

    /**
     * 清除所有内容（标题、左侧、右侧）
     */
    fun clearAll() {
        setTitleText(null)
        clearLeft()
        clearRight()
    }

    /**
     * 设置左侧为返回按钮（常用场景）
     * @param iconRes 返回图标资源，如果为 null 则不显示图标
     * @param text 返回文字，如果为 null 则不显示文字
     * @param onClick 点击回调
     */
    fun setLeftAsBack(
        @DrawableRes iconRes: Int? = null,
        text: CharSequence? = null,
        onClick: (() -> Unit)? = null
    ) {
        iconRes?.let { setLeftIcon(it) } ?: setLeftIcon(0)
        text?.let { setLeftText(it) } ?: setLeftText(null)
        setOnLeftClickListener(onClick?.let { OnClickListener { it() } })
    }

    /**
     * 设置右侧为更多按钮（常用场景）
     * @param iconRes 更多图标资源，如果为 null 则不显示图标
     * @param text 更多文字，如果为 null 则不显示文字
     * @param onClick 点击回调
     */
    fun setRightAsMore(
        @DrawableRes iconRes: Int? = null,
        text: CharSequence? = null,
        onClick: (() -> Unit)? = null
    ) {
        iconRes?.let { setRightIcon(it) } ?: setRightIcon(0)
        text?.let { setRightText(it) } ?: setRightText(null)
        setOnRightClickListener(onClick?.let { OnClickListener { it() } })
    }

    /**
     * 批量设置文字颜色
     * @param color 要设置的颜色
     * @param includeTitle 是否包含标题
     * @param includeLeft 是否包含左侧文字
     * @param includeRight 是否包含右侧文字
     */
    fun setAllTextColor(
        @ColorInt color: Int,
        includeTitle: Boolean = true,
        includeLeft: Boolean = true,
        includeRight: Boolean = true
    ) {
        if (includeTitle) setTitleTextColor(color)
        if (includeLeft) setLeftTextColor(color)
        if (includeRight) setRightTextColor(color)
    }

    /**
     * 批量设置文字大小
     * @param sizeSp 要设置的文字大小（SP）
     * @param includeTitle 是否包含标题
     * @param includeLeft 是否包含左侧文字
     * @param includeRight 是否包含右侧文字
     */
    fun setAllTextSize(
        @Dimension(unit = Dimension.SP) sizeSp: Float,
        includeTitle: Boolean = true,
        includeLeft: Boolean = true,
        includeRight: Boolean = true
    ) {
        if (includeTitle) setTitleTextSize(sizeSp)
        if (includeLeft) setLeftTextSize(sizeSp)
        if (includeRight) setRightTextSize(sizeSp)
    }

    /**
     * 批量设置图标 tint
     * @param color 要设置的 tint 颜色
     * @param includeLeft 是否包含左侧图标
     * @param includeRight 是否包含右侧图标
     */
    fun setAllIconTint(
        @ColorInt color: Int,
        includeLeft: Boolean = true,
        includeRight: Boolean = true
    ) {
        if (includeLeft) setLeftIconTint(color)
        if (includeRight) setRightIconTint(color)
    }
}


