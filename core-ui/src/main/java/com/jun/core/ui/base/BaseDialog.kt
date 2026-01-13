package com.jun.core.ui.base

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.FragmentManager
import androidx.viewbinding.ViewBinding
import com.jun.core.ui.extension.hideKeyboard
import com.jun.core.ui.extension.isKeyboardVisible
import com.jun.core.common.util.notify.UiNotifierManager

/**
 * BaseDialog 基类
 * 提供通用的 DialogFragment 功能
 * 
 * 功能包括：
 * - ViewBinding 支持（自动处理生命周期）
 * - 消息提示（Snackbar）
 * - 软键盘管理
 * - Dialog 配置（宽度、高度、动画、背景等）
 * - 返回键处理
 * - 生命周期方法
 * - 安全显示/隐藏
 * 
 * 使用示例：
 * ```kotlin
 * class CustomDialog : BaseDialog<DialogCustomBinding>() {
 *     override fun createBinding(inflater: LayoutInflater, container: ViewGroup?): DialogCustomBinding {
 *         return DialogCustomBinding.inflate(inflater, container, false)
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
 * // 显示 Dialog
 * CustomDialog().show(supportFragmentManager, "CustomDialog")
 * ```
 */
abstract class BaseDialog<VB : ViewBinding> : AppCompatDialogFragment() {
    
    private var _binding: VB? = null
    protected val binding: VB
        get() = _binding ?: throw IllegalStateException("Binding is not available. Dialog view has been destroyed.")
    
    /**
     * Dialog 宽度（默认 WRAP_CONTENT）
     * 可以设置为 MATCH_PARENT、WRAP_CONTENT 或具体数值（dp）
     */
    protected open val dialogWidth: Int = WindowManager.LayoutParams.WRAP_CONTENT
    
    /**
     * Dialog 高度（默认 WRAP_CONTENT）
     * 可以设置为 MATCH_PARENT、WRAP_CONTENT 或具体数值（dp）
     */
    protected open val dialogHeight: Int = WindowManager.LayoutParams.WRAP_CONTENT
    
    /**
     * 是否可取消（点击外部或返回键）
     */
    protected open val cancelable: Boolean = true
    
    /**
     * 是否在取消时隐藏键盘
     */
    protected open val hideKeyboardOnCancel: Boolean = true
    
    /**
     * Dialog 背景是否透明
     */
    protected open val isBackgroundTransparent: Boolean = false
    
    /**
     * Dialog 动画资源 ID（0 表示使用默认动画）
     */
    protected open val dialogAnimation: Int = 0
    
    /**
     * 是否全屏显示
     */
    protected open val isFullScreen: Boolean = false
    
    /**
     * 是否显示系统栏（状态栏和导航栏）
     */
    protected open val showSystemBars: Boolean = true
    
    /**
     * 创建 ViewBinding
     */
    protected abstract fun createBinding(inflater: LayoutInflater, container: ViewGroup?): VB
    
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        
        // 设置 Dialog 属性
        dialog.setCancelable(cancelable)
        dialog.setCanceledOnTouchOutside(cancelable)
        
        // 配置 Window
        dialog.window?.let { window ->
            configureWindow(window)
        }
        
        return dialog
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = createBinding(inflater, container)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupViews()
        setupObservers()
        setupListeners()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    
    // ==================== Window 配置 ====================
    
    /**
     * 配置 Window 属性
     */
    private fun configureWindow(window: Window) {
        // 设置布局参数
        val layoutParams = WindowManager.LayoutParams().apply {
            copyFrom(window.attributes)
            
            when {
                isFullScreen -> {
                    width = WindowManager.LayoutParams.MATCH_PARENT
                    height = WindowManager.LayoutParams.MATCH_PARENT
                }
                else -> {
                    width = dialogWidth
                    height = dialogHeight
                }
            }
        }
        
        window.attributes = layoutParams
        
        // 设置系统栏（使用现代 API）
        if (!showSystemBars || isFullScreen) {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            val insetsController = WindowInsetsControllerCompat(window, window.decorView)
            insetsController.hide(WindowInsetsCompat.Type.systemBars())
        }
        
        // 设置背景
        if (isBackgroundTransparent) {
            window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
        
        // 设置动画
        if (dialogAnimation != 0) {
            window.setWindowAnimations(dialogAnimation)
        }
    }
    
    /**
     * 设置 Dialog 宽度（dp）
     */
    protected fun setDialogWidth(widthDp: Int) {
        dialog?.window?.let { window ->
            val layoutParams = window.attributes
            layoutParams.width = (widthDp * resources.displayMetrics.density).toInt()
            window.attributes = layoutParams
        }
    }
    
    /**
     * 设置 Dialog 高度（dp）
     */
    protected fun setDialogHeight(heightDp: Int) {
        dialog?.window?.let { window ->
            val layoutParams = window.attributes
            layoutParams.height = (heightDp * resources.displayMetrics.density).toInt()
            window.attributes = layoutParams
        }
    }
    
    /**
     * 设置 Dialog 尺寸（dp）
     */
    protected fun setDialogSize(widthDp: Int, heightDp: Int) {
        dialog?.window?.let { window ->
            val layoutParams = window.attributes
            layoutParams.width = (widthDp * resources.displayMetrics.density).toInt()
            layoutParams.height = (heightDp * resources.displayMetrics.density).toInt()
            window.attributes = layoutParams
        }
    }
    
    /**
     * 设置 Dialog 位置
     */
    protected fun setDialogGravity(gravity: Int) {
        dialog?.window?.let { window ->
            val layoutParams = window.attributes
            layoutParams.gravity = gravity
            window.attributes = layoutParams
        }
    }
    
    // ==================== 生命周期方法 ====================
    
    protected open fun setupViews() {}
    protected open fun setupObservers() {}
    protected open fun setupListeners() {}
    
    // ==================== 消息提示 ====================
    
    /**
     * 显示错误消息（在 Dialog 内部）
     */
    protected fun showError(message: String) {
        val anchor = view ?: binding.root
        UiNotifierManager.error(anchor, message)
    }
    
    /**
     * 显示成功消息（在 Dialog 内部）
     */
    protected fun showSuccess(message: String) {
        val anchor = view ?: binding.root
        UiNotifierManager.success(anchor, message)
    }
    
    /**
     * 显示警告消息（在 Dialog 内部）
     */
    protected fun showWarning(message: String) {
        val anchor = view ?: binding.root
        UiNotifierManager.warning(anchor, message)
    }
    
    /**
     * 显示普通消息（在 Dialog 内部）
     */
    protected fun showMessage(message: String) {
        val anchor = view ?: binding.root
        UiNotifierManager.info(anchor, message)
    }
    
    // ==================== 软键盘管理 ====================
    
    /**
     * 隐藏软键盘
     */
    protected fun hideKeyboard() {
        dialog?.window?.let { window ->
            val view = window.currentFocus ?: window.decorView.rootView
            view.hideKeyboard()
        }
    }
    
    /**
     * 检查软键盘是否显示
     */
    protected fun isKeyboardVisible(): Boolean {
        return view?.isKeyboardVisible() ?: false
    }
    
    // ==================== Dialog 控制 ====================
    
    /**
     * 安全显示 Dialog
     * 避免重复显示和 FragmentManager 状态问题
     */
    open fun showSafely(fragmentManager: FragmentManager, tag: String? = null) {
        if (!isAdded && !isVisible && !isRemoving) {
            try {
                val dialogTag = tag ?: this::class.java.simpleName
                show(fragmentManager, dialogTag)
            } catch (e: Exception) {
                // 忽略显示异常
            }
        }
    }
    
    /**
     * 安全关闭 Dialog
     */
    open fun dismissSafely() {
        if (isAdded && isVisible) {
            try {
                dismiss()
            } catch (e: Exception) {
                dismissAllowingStateLoss()
            }
        }
    }
    
    /**
     * 关闭 Dialog（带回调）
     */
    protected fun dismissWithResult(result: Any? = null) {
        onDialogDismissed(result)
        dismissSafely()
    }
    
    /**
     * Dialog 关闭回调（子类可以重写）
     */
    protected open fun onDialogDismissed(result: Any?) {
        // 子类可以重写此方法来处理关闭结果
    }
    
    override fun onCancel(dialog: android.content.DialogInterface) {
        super.onCancel(dialog)
        if (hideKeyboardOnCancel) {
            hideKeyboard()
        }
        onDialogCancelled()
    }
    
    /**
     * Dialog 取消回调（子类可以重写）
     */
    protected open fun onDialogCancelled() {
        // 子类可以重写此方法
    }
    
    override fun onDismiss(dialog: android.content.DialogInterface) {
        super.onDismiss(dialog)
        if (hideKeyboardOnCancel) {
            hideKeyboard()
        }
    }
    
    // ==================== 工具方法 ====================
    
    /**
     * 获取 Context（类型安全）
     */
    protected fun requireContextSafe(): Context {
        return requireContext()
    }
    
    /**
     * 获取 Activity（类型安全）
     */
    protected fun requireActivitySafe() = requireActivity()
    
    /**
     * 检查 Dialog 是否已显示
     */
    protected fun isDialogShowing(): Boolean {
        return isAdded && isVisible && dialog?.isShowing == true
    }
}

/**
 * 简单的确认 Dialog
 * 使用 AlertDialog，无需自定义布局
 * 
 * 使用示例：
 * ```kotlin
 * SimpleConfirmDialog.newInstance(
 *     title = "确认",
 *     message = "确定要删除吗？",
 *     onConfirm = { /* 确认操作 */ }
 * ).show(supportFragmentManager, "ConfirmDialog")
 * ```
 */
class SimpleConfirmDialog : AppCompatDialogFragment() {
    
    private var title: String? = null
    private var message: String? = null
    private var positiveText: String = "确定"
    private var negativeText: String = "取消"
    private var onConfirm: (() -> Unit)? = null
    private var onCancel: (() -> Unit)? = null
    
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        title = arguments?.getString(ARG_TITLE)
        message = arguments?.getString(ARG_MESSAGE)
        positiveText = arguments?.getString(ARG_POSITIVE_TEXT) ?: "确定"
        negativeText = arguments?.getString(ARG_NEGATIVE_TEXT) ?: "取消"
        
        return androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(positiveText) { _, _ ->
                onConfirm?.invoke()
            }
            .setNegativeButton(negativeText) { _, _ ->
                onCancel?.invoke()
            }
            .create()
    }
    
    companion object {
        private const val ARG_TITLE = "title"
        private const val ARG_MESSAGE = "message"
        private const val ARG_POSITIVE_TEXT = "positive_text"
        private const val ARG_NEGATIVE_TEXT = "negative_text"
        
        fun newInstance(
            title: String? = null,
            message: String,
            positiveText: String = "确定",
            negativeText: String = "取消",
            onConfirm: (() -> Unit)? = null,
            onCancel: (() -> Unit)? = null
        ): SimpleConfirmDialog {
            return SimpleConfirmDialog().apply {
                arguments = Bundle().apply {
                    putString(ARG_TITLE, title)
                    putString(ARG_MESSAGE, message)
                    putString(ARG_POSITIVE_TEXT, positiveText)
                    putString(ARG_NEGATIVE_TEXT, negativeText)
                }
                this.onConfirm = onConfirm
                this.onCancel = onCancel
            }
        }
    }
}

/**
 * 简单的加载 Dialog
 * 显示加载指示器（使用 ProgressBar）和可选的提示消息
 * 支持通过 LoadingDialogConfig 自定义样式
 * 
 * 使用示例：
 * ```kotlin
 * // 使用默认配置
 * val loadingDialog = SimpleLoadingDialog()
 * 
 * // 使用自定义配置
 * val config = LoadingDialogConfig(
 *     backgroundColor = Color.BLUE,
 *     progressBarSize = 72
 * )
 * val loadingDialog = SimpleLoadingDialog(config)
 * 
 * loadingDialog.show(supportFragmentManager, "LoadingDialog")
 * 
 * // 更新消息
 * loadingDialog.updateMessage("正在处理...")
 * 
 * // 关闭
 * loadingDialog.dismissSafely()
 * ```
 */
class SimpleLoadingDialog(
    private val config: com.jun.core.common.ui.LoadingDialogConfig = com.jun.core.common.ui.LoadingDialogConfig.DEFAULT
) : AppCompatDialogFragment(), com.jun.core.common.ui.LoadingDialog {
    
    private var message: String = config.defaultMessage
    
    companion object {
        private const val ARG_MESSAGE = "message"
        private const val ARG_CONFIG = "config"
        
        /**
         * 创建实例（使用默认配置）
         */
        fun newInstance(message: String = "加载中..."): SimpleLoadingDialog {
            return SimpleLoadingDialog().apply {
                arguments = Bundle().apply {
                    putString(ARG_MESSAGE, message)
                }
            }
        }
        
        /**
         * 创建实例（使用自定义配置）
         */
        fun newInstance(
            config: com.jun.core.common.ui.LoadingDialogConfig,
            message: String = config.defaultMessage
        ): SimpleLoadingDialog {
            return SimpleLoadingDialog(config).apply {
                arguments = Bundle().apply {
                    putString(ARG_MESSAGE, message)
                    // 注意：Bundle 不能直接存储自定义对象，需要在工厂方法中传递
                }
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.getString(ARG_MESSAGE)?.let {
            message = it
        }
    }
    
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // 使用自定义布局或默认布局
        val layoutResId = if (config.customLayoutResId != 0) {
            config.customLayoutResId
        } else {
            com.jun.core.ui.R.layout.coreui_dialog_loading
        }
        
        val view = layoutInflater.inflate(layoutResId, null)
        val progressBar = view.findViewById<android.widget.ProgressBar>(com.jun.core.ui.R.id.progressBar)
        val tvMessage = view.findViewById<android.widget.TextView>(com.jun.core.ui.R.id.tvMessage)
        
        // 应用配置
        applyConfig(view, progressBar, tvMessage)
        
        val dialog = Dialog(requireContext(), android.R.style.Theme_Translucent_NoTitleBar)
        dialog.setContentView(view)
        dialog.setCancelable(false)
        dialog.setCanceledOnTouchOutside(false)
        
        // 设置窗口属性
        dialog.window?.apply {
            setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT
            )
            setBackgroundDrawableResource(android.R.color.transparent)
            
            // 添加动画
            if (config.windowAnimationStyle != 0) {
                setWindowAnimations(config.windowAnimationStyle)
            } else {
                setWindowAnimations(android.R.style.Animation_Dialog)
            }
        }
        
        return dialog
    }
    
    /**
     * 应用配置到视图
     */
    private fun applyConfig(
        rootView: android.view.View,
        progressBar: android.widget.ProgressBar?,
        tvMessage: android.widget.TextView?
    ) {
        val context = requireContext()
        val density = context.resources.displayMetrics.density
        
        // 设置遮罩层背景
        rootView.setBackgroundColor(config.overlayColor)
        
        // 查找内容容器（LinearLayout）
        val contentContainer = rootView.findViewById<android.view.ViewGroup>(
            com.jun.core.ui.R.id.contentContainer
        ) ?: (rootView as? android.view.ViewGroup)?.getChildAt(0) as? android.view.ViewGroup
        
        contentContainer?.apply {
            // 设置背景颜色和圆角
            background = createBackgroundDrawable()
            
            // 设置阴影
            elevation = config.elevation * density
            
            // 设置透明度
            alpha = config.alpha
            
            // 设置内边距
            val paddingPx = (config.padding * density).toInt()
            setPadding(paddingPx, paddingPx, paddingPx, paddingPx)
            
            // 设置最小宽度
            minimumWidth = (config.minWidth * density).toInt()
        }
        
        // 配置 ProgressBar
        progressBar?.apply {
            val sizePx = (config.progressBarSize * density).toInt()
            layoutParams = layoutParams?.apply {
                width = sizePx
                height = sizePx
            }
            
            val progressBarColor = config.progressBarColor
            if (progressBarColor != null) {
                indeterminateTintList = android.content.res.ColorStateList.valueOf(progressBarColor)
            }
        }
        
        // 配置消息文本
        tvMessage?.apply {
            if (config.showMessage) {
                visibility = android.view.View.VISIBLE
                text = message
                textSize = config.messageTextSize
                
                val messageTextColor = config.messageTextColor
                if (messageTextColor != null) setTextColor(messageTextColor)
                
                val spacingPx = (config.messageLineSpacing * density)
                setLineSpacing(spacingPx, 1.0f)
                letterSpacing = config.messageLetterSpacing
                
                // 设置间距
                val marginTopPx = (config.progressBarMessageSpacing * density).toInt()
                (layoutParams as? android.view.ViewGroup.MarginLayoutParams)?.topMargin = marginTopPx
            } else {
                visibility = android.view.View.GONE
            }
        }
    }
    
    /**
     * 创建背景 Drawable
     */
    private fun createBackgroundDrawable(): android.graphics.drawable.Drawable {
        val drawable = android.graphics.drawable.GradientDrawable()
        drawable.setColor(config.backgroundColor)
        drawable.cornerRadius = config.cornerRadius * requireContext().resources.displayMetrics.density
        return drawable
    }
    
    /**
     * 更新 Loading 消息
     * 注意：此方法可以被 LoadingManager 通过工厂模式调用
     */
    override fun updateMessage(message: String) {
        this.message = message
        view?.findViewById<android.widget.TextView>(com.jun.core.ui.R.id.tvMessage)?.text = message
    }
    
    /**
     * 安全显示 Dialog
     */
    override fun showSafely(fragmentManager: FragmentManager, tag: String?) {
        if (!isAdded && !isVisible && !isRemoving) {
            try {
                val dialogTag = tag ?: this::class.java.simpleName
                show(fragmentManager, dialogTag)
            } catch (e: Exception) {
                // 忽略显示异常
            }
        }
    }
    
    /**
     * 安全关闭 Dialog
     * 优化：即使不可见也尝试关闭，确保 dialog 能正确关闭
     */
    override fun dismissSafely() {
        if (isAdded) {
            try {
                if (isVisible) {
                    dismiss()
                } else {
                    // 即使不可见，也尝试关闭（可能正在关闭过程中）
                    dismissAllowingStateLoss()
                }
            } catch (e: Exception) {
                // 如果 dismiss() 失败，尝试使用 dismissAllowingStateLoss()
                try {
                    dismissAllowingStateLoss()
                } catch (e2: Exception) {
                    // 忽略所有异常，确保不会崩溃
                }
            }
        }
    }
}

