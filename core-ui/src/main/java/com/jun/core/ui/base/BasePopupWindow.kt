package com.jun.core.ui.base

import android.content.Context
import android.graphics.Color
import android.graphics.Point
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.PopupWindow
import androidx.core.view.ViewCompat
import androidx.viewbinding.ViewBinding
import com.jun.core.ui.extension.hideKeyboard
import com.jun.core.ui.extension.isKeyboardVisible
import com.jun.core.common.util.notify.UiNotifierManager

/**
 * BasePopupWindow 基类
 * 提供通用的 PopupWindow 功能
 * 
 * 功能包括：
 * - ViewBinding 支持
 * - 消息提示（Snackbar）
 * - 软键盘管理
 * - 背景变暗
 * - 动画支持
 * - 尺寸配置
 * - 点击外部关闭
 * - 返回键处理
 * - 生命周期方法
 * - 安全显示/隐藏
 * - 锚点位置显示（上、下、左、右）
 * - 自动屏幕边界调整
 * - 链式调用配置
 * 
 * 使用示例：
 * ```kotlin
 * class CustomPopupWindow(context: Context) : BasePopupWindow<PopupCustomBinding>(context) {
 *     override fun createBinding(inflater: LayoutInflater): PopupCustomBinding {
 *         return PopupCustomBinding.inflate(inflater, null, false)
 *     }
 *     
 *     override fun setupViews() {
 *         binding.title.text = "标题"
 *         binding.message.text = "消息内容"
 *     }
 *     
 *     override fun setupListeners() {
 *         binding.confirmButton.setOnClickListener {
 *             onConfirm()
 *             dismiss()
 *         }
 *     }
 * }
 * 
 * val popup = CustomPopupWindow(context)
 * 
 * // 方式1：链式调用配置并显示（推荐）
 * popup.width(300)
 *     .height(400)
 *     .setDimBackground(true, 0.5f)
 *     .setOffset(10, 5)
 *     .showAbove(anchorView)
 * 
 * // 方式2：使用 configure DSL 配置
 * popup.configure {
 *     width = 300
 *     height = 400
 *     dimBackground = true
 *     dimAmount = 0.5f
 *     offsetX = 10
 *     offsetY = 5
 *     alignToAnchor = false
 * }.showBelow(anchorView)
 * 
 * // 方式3：传统方式（仍然支持）
 * popup.showAsDropDown(anchorView)
 * popup.showAtLocation(parentView, Gravity.CENTER, 0, 0)
 * popup.showAbove(anchorView)
 * popup.showBelow(anchorView)
 * popup.showToLeft(anchorView)
 * popup.showToRight(anchorView)
 * ```
 */
abstract class BasePopupWindow<VB : ViewBinding>(
    context: Context
) : PopupWindow(context) {
    
    protected lateinit var binding: VB
        private set
    
    protected val context: Context = context
    
    // ==================== 链式调用配置 ====================
    
    /**
     * 链式调用配置类
     * 用于在显示前动态配置 PopupWindow 属性
     */
    class PopupConfig {
        var width: Int? = null
        var height: Int? = null
        var outsideTouchable: Boolean? = null
        var focusable: Boolean? = null
        var dimBackground: Boolean? = null
        var dimAmount: Float? = null
        var backgroundColor: Int? = null
        var enableAnimation: Boolean? = null
        var enterAnimation: Int? = null
        var exitAnimation: Int? = null
        var hideKeyboardOnShow: Boolean? = null
        var showKeyboardOnDismiss: Boolean? = null
        
        // 显示相关配置
        var offsetX: Int = 0
        var offsetY: Int = 0
        var alignToAnchor: Boolean = true
        var adjustToScreen: Boolean = true
    }
    
    private val config = PopupConfig()
    
    // ==================== 配置属性 ====================
    
    /**
     * PopupWindow 宽度（默认 WRAP_CONTENT）
     */
    open val popupWidth: Int = ViewGroup.LayoutParams.WRAP_CONTENT
    
    /**
     * PopupWindow 高度（默认 WRAP_CONTENT）
     */
    open val popupHeight: Int = ViewGroup.LayoutParams.WRAP_CONTENT
    
    /**
     * 是否可点击外部关闭（默认 true）
     */
    open val outsideTouchable: Boolean = true
    
    /**
     * 是否可获取焦点（默认 true）
     */
    open val focusable: Boolean = true
    
    /**
     * 是否显示背景变暗效果（默认 false）
     */
    open val dimBackground: Boolean = false
    
    /**
     * 背景变暗的透明度（0.0 - 1.0，默认 0.5）
     */
    open val dimAmount: Float = 0.5f
    
    /**
     * 背景颜色（默认透明）
     */
    open val backgroundColor: Int = Color.TRANSPARENT
    
    /**
     * 是否显示动画（默认 false）
     */
    open val enableAnimation: Boolean = false
    
    /**
     * 进入动画资源 ID（0 表示使用默认）
     */
    open val enterAnimation: Int = 0
    
    /**
     * 退出动画资源 ID（0 表示使用默认）
     */
    open val exitAnimation: Int = 0
    
    /**
     * 是否在显示时隐藏软键盘（默认 false）
     */
    open val hideKeyboardOnShow: Boolean = false
    
    /**
     * 是否在关闭时显示软键盘（默认 false）
     */
    open val showKeyboardOnDismiss: Boolean = false
    
    // ==================== 初始化 ====================
    
    init {
        val inflater = LayoutInflater.from(context)
        binding = createBinding(inflater)
        
        // 设置内容视图
        contentView = binding.root
        
        // 初始配置（在链式调用配置之前应用默认值）
        applyConfiguration()
        
        // 设置关闭监听
        setOnDismissListener {
            onDismiss()
        }
        
        // 设置返回键监听
        contentView?.isFocusableInTouchMode = true
        contentView?.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                if (handleBackPress()) {
                    dismiss()
                    true
                } else {
                    false
                }
            } else {
                false
            }
        }
        
        // 调用生命周期方法
        setupViews()
        setupObservers()
        setupListeners()
    }
    
    /**
     * 创建 ViewBinding
     * 子类必须实现此方法
     */
    protected abstract fun createBinding(inflater: LayoutInflater): VB
    
    // ==================== 生命周期方法 ====================
    
    /**
     * 初始化视图
     * 子类可以重写此方法来设置视图内容
     */
    protected open fun setupViews() {}
    
    /**
     * 设置观察者
     * 子类可以重写此方法来设置数据观察
     */
    protected open fun setupObservers() {}
    
    /**
     * 设置监听器
     * 子类可以重写此方法来设置点击等监听
     */
    protected open fun setupListeners() {}
    
    // ==================== 显示/隐藏 ====================
    
    /**
     * 显示 PopupWindow（在指定位置下方）
     * 
     * @param anchor 锚点视图
     * @param xoff X 偏移量（默认 0）
     * @param yoff Y 偏移量（默认 0）
     * @param gravity 重力（默认 Gravity.NO_GRAVITY）
     */
    override fun showAsDropDown(
        anchor: View,
        xoff: Int,
        yoff: Int,
        gravity: Int
    ) {
        if (isShowing) return
        
        onBeforeShow()
        
        val shouldHideKeyboard = config.hideKeyboardOnShow ?: hideKeyboardOnShow
        if (shouldHideKeyboard) {
            anchor.hideKeyboard()
        }
        
        val shouldDim = config.dimBackground ?: dimBackground
        if (shouldDim) {
            dimBackground(anchor)
        }
        
        super.showAsDropDown(anchor, xoff, yoff, gravity)
        
        onAfterShow()
    }
    
    /**
     * 显示 PopupWindow（在指定位置下方）- 链式调用版本
     */
    fun showAsDropDownChain(
        anchor: View,
        xoff: Int = 0,
        yoff: Int = 0,
        gravity: Int = Gravity.NO_GRAVITY
    ): BasePopupWindow<VB> {
        showAsDropDown(anchor, xoff, yoff, gravity)
        return this
    }
    
    /**
     * 显示 PopupWindow（在指定位置）
     * 
     * @param parent 父视图
     * @param gravity 重力（默认 Gravity.CENTER）
     * @param x X 坐标（默认 0）
     * @param y Y 坐标（默认 0）
     */
    override fun showAtLocation(
        parent: View,
        gravity: Int,
        x: Int,
        y: Int
    ) {
        if (isShowing) return
        
        onBeforeShow()
        
        val shouldHideKeyboard = config.hideKeyboardOnShow ?: hideKeyboardOnShow
        if (shouldHideKeyboard) {
            parent.hideKeyboard()
        }
        
        val shouldDim = config.dimBackground ?: dimBackground
        if (shouldDim) {
            dimBackground(parent)
        }
        
        super.showAtLocation(parent, gravity, x, y)
        
        onAfterShow()
    }
    
    /**
     * 显示 PopupWindow（在指定位置）- 链式调用版本
     */
    fun showAtLocationChain(
        parent: View,
        gravity: Int = Gravity.CENTER,
        x: Int = 0,
        y: Int = 0
    ): BasePopupWindow<VB> {
        showAtLocation(parent, gravity, x, y)
        return this
    }
    
    /**
     * 安全显示 PopupWindow（在指定位置下方）
     * 检查视图是否已附加到窗口
     */
    fun showAsDropDownSafely(
        anchor: View,
        xoff: Int = 0,
        yoff: Int = 0,
        gravity: Int = Gravity.NO_GRAVITY
    ) {
        if (!ViewCompat.isAttachedToWindow(anchor)) {
            return
        }
        showAsDropDown(anchor, xoff, yoff, gravity)
    }
    
    /**
     * 安全显示 PopupWindow（在指定位置）
     * 检查视图是否已附加到窗口
     */
    fun showAtLocationSafely(
        parent: View,
        gravity: Int = Gravity.CENTER,
        x: Int = 0,
        y: Int = 0
    ) {
        if (!ViewCompat.isAttachedToWindow(parent)) {
            return
        }
        showAtLocation(parent, gravity, x, y)
    }
    
    // ==================== 锚点位置显示 ====================
    
    /**
     * PopupWindow 相对于锚点的位置
     */
    enum class PopupPosition {
        /** 在锚点上方 */
        TOP,
        /** 在锚点下方 */
        BOTTOM,
        /** 在锚点左侧 */
        LEFT,
        /** 在锚点右侧 */
        RIGHT
    }
    
    /**
     * 在锚点的指定位置显示 PopupWindow
     * 
     * @param anchor 锚点视图
     * @param position 位置（TOP/BOTTOM/LEFT/RIGHT）
     * @param offsetX X 轴偏移量（dp，默认 0）
     * @param offsetY Y 轴偏移量（dp，默认 0）
     * @param alignToAnchor 是否与锚点对齐（默认 true）
     *   - true: PopupWindow 的边缘与锚点对齐
     *   - false: PopupWindow 的中心与锚点对齐
     * @param adjustToScreen 是否自动调整位置以避免超出屏幕（默认 true）
     */
    fun showAtAnchor(
        anchor: View,
        position: PopupPosition,
        offsetX: Int = 0,
        offsetY: Int = 0,
        alignToAnchor: Boolean = true,
        adjustToScreen: Boolean = true
    ): BasePopupWindow<VB> {
        if (isShowing) return this
        if (!ViewCompat.isAttachedToWindow(anchor)) return this
        
        onBeforeShow()
        
        val shouldHideKeyboard = config.hideKeyboardOnShow ?: hideKeyboardOnShow
        if (shouldHideKeyboard) {
            anchor.hideKeyboard()
        }
        
        val shouldDim = config.dimBackground ?: dimBackground
        if (shouldDim) {
            dimBackground(anchor)
        }
        
        // 测量 PopupWindow 尺寸
        contentView?.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        val popupWidth = contentView?.measuredWidth ?: this.width
        val popupHeight = contentView?.measuredHeight ?: this.height
        
        // 获取锚点位置（相对于屏幕）
        val anchorLocation = IntArray(2)
        anchor.getLocationOnScreen(anchorLocation)
        val anchorX = anchorLocation[0]
        val anchorY = anchorLocation[1]
        val anchorWidth = anchor.width
        val anchorHeight = anchor.height
        
        // 获取父视图位置（用于计算相对坐标）
        val parentView = anchor.rootView ?: anchor
        val parentLocation = IntArray(2)
        parentView.getLocationOnScreen(parentLocation)
        
        // 获取屏幕尺寸
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display = windowManager.defaultDisplay
        val screenSize = Point()
        display.getSize(screenSize)
        val screenWidth = screenSize.x
        val screenHeight = screenSize.y
        
        // 转换偏移量（dp 转 px）
        val density = context.resources.displayMetrics.density
        val offsetXPx = (offsetX * density).toInt()
        val offsetYPx = (offsetY * density).toInt()
        
        // 计算位置（相对于屏幕的绝对坐标）
        val (screenX, screenY) = when (position) {
            PopupPosition.TOP -> {
                val x = if (alignToAnchor) {
                    anchorX + offsetXPx
                } else {
                    anchorX + anchorWidth / 2 - popupWidth / 2 + offsetXPx
                }
                val y = anchorY - popupHeight - offsetYPx
                Pair(x, y)
            }
            PopupPosition.BOTTOM -> {
                val x = if (alignToAnchor) {
                    anchorX + offsetXPx
                } else {
                    anchorX + anchorWidth / 2 - popupWidth / 2 + offsetXPx
                }
                val y = anchorY + anchorHeight + offsetYPx
                Pair(x, y)
            }
            PopupPosition.LEFT -> {
                val x = anchorX - popupWidth - offsetXPx
                val y = if (alignToAnchor) {
                    anchorY + offsetYPx
                } else {
                    anchorY + anchorHeight / 2 - popupHeight / 2 + offsetYPx
                }
                Pair(x, y)
            }
            PopupPosition.RIGHT -> {
                val x = anchorX + anchorWidth + offsetXPx
                val y = if (alignToAnchor) {
                    anchorY + offsetYPx
                } else {
                    anchorY + anchorHeight / 2 - popupHeight / 2 + offsetYPx
                }
                Pair(x, y)
            }
        }
        
        // 调整位置以避免超出屏幕
        val (finalScreenX, finalScreenY) = if (adjustToScreen) {
            adjustPositionToScreen(screenX, screenY, popupWidth, popupHeight, screenWidth, screenHeight)
        } else {
            Pair(screenX, screenY)
        }
        
        // 转换为相对于父视图的坐标
        val relativeX = finalScreenX - parentLocation[0]
        val relativeY = finalScreenY - parentLocation[1]
        
        // 显示 PopupWindow（使用相对坐标）
        showAtLocation(parentView, Gravity.NO_GRAVITY, relativeX, relativeY)
        
        onAfterShow()
        return this
    }
    
    /**
     * 调整位置以避免超出屏幕
     */
    private fun adjustPositionToScreen(
        x: Int,
        y: Int,
        popupWidth: Int,
        popupHeight: Int,
        screenWidth: Int,
        screenHeight: Int
    ): Pair<Int, Int> {
        var adjustedX = x
        var adjustedY = y
        
        // 调整 X 坐标
        if (adjustedX < 0) {
            adjustedX = 0
        } else if (adjustedX + popupWidth > screenWidth) {
            adjustedX = screenWidth - popupWidth
        }
        
        // 调整 Y 坐标
        if (adjustedY < 0) {
            adjustedY = 0
        } else if (adjustedY + popupHeight > screenHeight) {
            adjustedY = screenHeight - popupHeight
        }
        
        return Pair(adjustedX, adjustedY)
    }
    
    /**
     * 关闭 PopupWindow
     */
    override fun dismiss() {
        if (!isShowing) return
        
        onBeforeDismiss()
        
        if (showKeyboardOnDismiss && contentView != null) {
            contentView?.postDelayed({
                contentView?.requestFocus()
            }, 100)
        }
        
        super.dismiss()
        
        onAfterDismiss()
    }
    
    // ==================== 生命周期回调 ====================
    
    /**
     * 显示前回调
     */
    protected open fun onBeforeShow() {}
    
    /**
     * 显示后回调
     */
    protected open fun onAfterShow() {}
    
    /**
     * 关闭前回调
     */
    protected open fun onBeforeDismiss() {}
    
    /**
     * 关闭后回调
     */
    protected open fun onAfterDismiss() {
        // 恢复背景亮度
        val shouldDim = config.dimBackground ?: dimBackground
        if (shouldDim) {
            clearDimBackground()
        }
    }
    
    /**
     * 关闭回调（PopupWindow 的 OnDismissListener）
     */
    protected open fun onDismiss() {}
    
    // ==================== 返回键处理 ====================
    
    /**
     * 处理返回键
     * @return true 表示已处理，false 表示未处理
     */
    protected open fun handleBackPress(): Boolean {
        return true
    }
    
    // ==================== 背景变暗 ====================
    
    private var dimView: View? = null
    private var originalAlpha: Float = 1f
    
    /**
     * 背景变暗
     */
    private fun dimBackground(anchor: View) {
        val activity = anchor.context as? android.app.Activity ?: return
        val window = activity.window ?: return
        
        val params = window.attributes
        originalAlpha = params.alpha
        val dimAmountValue = config.dimAmount ?: dimAmount
        params.alpha = 1f - dimAmountValue
        window.attributes = params
    }
    
    /**
     * 清除背景变暗
     */
    private fun clearDimBackground() {
        val activity = context as? android.app.Activity ?: return
        val window = activity.window ?: return
        
        val params = window.attributes
        params.alpha = originalAlpha
        window.attributes = params
    }
    
    // ==================== 消息提示 ====================
    
    /**
     * 显示错误消息
     */
    protected fun showError(message: String) {
        contentView?.let { UiNotifierManager.error(it, message) }
    }
    
    /**
     * 显示成功消息
     */
    protected fun showSuccess(message: String) {
        contentView?.let { UiNotifierManager.success(it, message) }
    }
    
    /**
     * 显示警告消息
     */
    protected fun showWarning(message: String) {
        contentView?.let { UiNotifierManager.warning(it, message) }
    }
    
    /**
     * 显示普通消息
     */
    protected fun showMessage(message: String) {
        contentView?.let { UiNotifierManager.info(it, message) }
    }
    
    // ==================== 软键盘管理 ====================
    
    /**
     * 隐藏软键盘
     */
    protected fun hideKeyboard() {
        contentView?.hideKeyboard()
    }
    
    /**
     * 检查软键盘是否显示
     */
    protected fun isKeyboardVisible(): Boolean {
        return contentView?.isKeyboardVisible() ?: false
    }
    
    // ==================== 链式调用配置方法 ====================
    
    /**
     * 配置 PopupWindow（DSL 风格）
     * 
     * 使用示例：
     * ```kotlin
     * popup.configure {
     *     width = 300
     *     height = 400
     *     dimBackground = true
     *     offsetX = 10
     * }.showAbove(anchorView)
     * ```
     */
    fun configure(block: PopupConfig.() -> Unit): BasePopupWindow<VB> {
        config.block()
        applyConfiguration()
        return this
    }
    
    /**
     * 设置宽度（链式调用）
     */
    fun width(width: Int): BasePopupWindow<VB> {
        config.width = width
        this.width = width
        return this
    }
    
    /**
     * 设置高度（链式调用）
     */
    fun height(height: Int): BasePopupWindow<VB> {
        config.height = height
        this.height = height
        return this
    }
    
    /**
     * 设置尺寸
     */
    fun setSize(width: Int, height: Int): BasePopupWindow<VB> {
        config.width = width
        config.height = height
        this.width = width
        this.height = height
        return this
    }
    
    /**
     * 设置是否可点击外部关闭（链式调用）
     */
    fun outsideTouchable(touchable: Boolean): BasePopupWindow<VB> {
        config.outsideTouchable = touchable
        isOutsideTouchable = touchable
        return this
    }
    
    /**
     * 设置是否可获取焦点（链式调用）
     */
    fun focusable(focusable: Boolean): BasePopupWindow<VB> {
        config.focusable = focusable
        isFocusable = focusable
        return this
    }
    
    /**
     * 设置背景变暗
     */
    fun setDimBackground(dim: Boolean, amount: Float = 0.5f): BasePopupWindow<VB> {
        config.dimBackground = dim
        config.dimAmount = amount
        return this
    }
    
    /**
     * 设置背景颜色
     */
    fun setBackgroundColor(color: Int): BasePopupWindow<VB> {
        config.backgroundColor = color
        setBackgroundDrawable(ColorDrawable(color))
        return this
    }
    
    /**
     * 设置动画
     */
    fun setAnimation(animationResId: Int): BasePopupWindow<VB> {
        config.enableAnimation = true
        config.enterAnimation = animationResId
        animationStyle = animationResId
        return this
    }
    
    /**
     * 设置软键盘管理
     */
    fun setKeyboardBehavior(
        hideOnShow: Boolean = false,
        showOnDismiss: Boolean = false
    ): BasePopupWindow<VB> {
        config.hideKeyboardOnShow = hideOnShow
        config.showKeyboardOnDismiss = showOnDismiss
        return this
    }
    
    /**
     * 设置偏移量
     */
    fun setOffset(offsetX: Int = 0, offsetY: Int = 0): BasePopupWindow<VB> {
        config.offsetX = offsetX
        config.offsetY = offsetY
        return this
    }
    
    /**
     * 设置对齐方式
     */
    fun setAlignToAnchor(align: Boolean): BasePopupWindow<VB> {
        config.alignToAnchor = align
        return this
    }
    
    /**
     * 设置是否自动调整到屏幕边界
     */
    fun setAdjustToScreen(adjust: Boolean): BasePopupWindow<VB> {
        config.adjustToScreen = adjust
        return this
    }
    
    /**
     * 应用配置
     */
    private fun applyConfiguration() {
        // 应用宽度和高度
        width = config.width ?: popupWidth
        height = config.height ?: popupHeight
        
        // 应用外部点击和焦点
        isOutsideTouchable = config.outsideTouchable ?: outsideTouchable
        isFocusable = config.focusable ?: focusable
        
        // 应用背景
        val bgColor = config.backgroundColor ?: backgroundColor
        if (bgColor != Color.TRANSPARENT) {
            setBackgroundDrawable(ColorDrawable(bgColor))
        } else {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
        
        // 应用动画
        val shouldAnimate = config.enableAnimation ?: enableAnimation
        if (shouldAnimate) {
            val animRes = config.enterAnimation ?: enterAnimation
            if (animRes != 0) {
                animationStyle = animRes
            }
        }
    }
    
    // ==================== 链式调用显示方法 ====================
    
    /**
     * 在锚点上方显示（链式调用）
     */
    fun showAbove(
        anchor: View,
        offsetX: Int = config.offsetX,
        offsetY: Int = config.offsetY,
        alignToAnchor: Boolean = config.alignToAnchor
    ): BasePopupWindow<VB> {
        showAtAnchor(anchor, PopupPosition.TOP, offsetX, offsetY, alignToAnchor, config.adjustToScreen)
        return this
    }
    
    /**
     * 在锚点下方显示（链式调用）
     */
    fun showBelow(
        anchor: View,
        offsetX: Int = config.offsetX,
        offsetY: Int = config.offsetY,
        alignToAnchor: Boolean = config.alignToAnchor
    ): BasePopupWindow<VB> {
        showAtAnchor(anchor, PopupPosition.BOTTOM, offsetX, offsetY, alignToAnchor, config.adjustToScreen)
        return this
    }
    
    /**
     * 在锚点左侧显示（链式调用）
     */
    fun showToLeft(
        anchor: View,
        offsetX: Int = config.offsetX,
        offsetY: Int = config.offsetY,
        alignToAnchor: Boolean = config.alignToAnchor
    ): BasePopupWindow<VB> {
        showAtAnchor(anchor, PopupPosition.LEFT, offsetX, offsetY, alignToAnchor, config.adjustToScreen)
        return this
    }
    
    /**
     * 在锚点右侧显示（链式调用）
     */
    fun showToRight(
        anchor: View,
        offsetX: Int = config.offsetX,
        offsetY: Int = config.offsetY,
        alignToAnchor: Boolean = config.alignToAnchor
    ): BasePopupWindow<VB> {
        showAtAnchor(anchor, PopupPosition.RIGHT, offsetX, offsetY, alignToAnchor, config.adjustToScreen)
        return this
    }
    
    /**
     * 在指定位置显示（链式调用）
     */
    fun showAt(
        parent: View,
        gravity: Int = Gravity.CENTER,
        x: Int = 0,
        y: Int = 0
    ): BasePopupWindow<VB> {
        showAtLocationChain(parent, gravity, x, y)
        return this
    }
}

