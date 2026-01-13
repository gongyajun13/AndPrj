package com.jun.core.common.util.notify

import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import com.google.android.material.snackbar.Snackbar
import com.jun.core.common.R

/**
 * 统一的 UI 提示入口。
 *
 * 设计目标：
 * - 业务层只依赖这一套接口，不关心底层是 Snackbar 还是自定义 Toast / Banner。
 * - 底层实现可以随时替换，而不影响调用方（比如后续从 Snackbar 切到全局自定义 Toast）。
 */
interface UiNotifier {

    /** 普通信息提示 */
    fun info(anchor: View, message: String)

    /** 成功提示 */
    fun success(anchor: View, message: String)

    /** 错误提示 */
    fun error(anchor: View, message: String)

    /** 警告提示 */
    fun warning(anchor: View, message: String)

    /** 带操作按钮的提示（如：撤销、重试等） */
    fun action(
        anchor: View,
        message: String,
        actionText: String,
        onAction: () -> Unit,
    )
}

/**
 * 全局唯一的 UiNotifier 访问入口。
 *
 * - 默认实现先用 Snackbar，后续可以在应用层替换为自定义 Toast / 顶部 Banner 等。
 * - 调用示例：
 *
 *   UiNotifierManager.success(view, "保存成功")
 *   UiNotifierManager.action(view, "删除 1 项", "撤销") { undo() }
 */
object UiNotifierManager : UiNotifier {

    /**
     * 当前使用的具体实现。
     * 应用层如需替换，可以在 Application 中：
     *
     * UiNotifierManager.delegate = MyCustomUiNotifier()
     */
    @Volatile
    var delegate: UiNotifier = SnackbarUiNotifier()

    override fun info(anchor: View, message: String) {
        delegate.info(anchor, message)
    }

    override fun success(anchor: View, message: String) {
        delegate.success(anchor, message)
    }

    override fun error(anchor: View, message: String) {
        delegate.error(anchor, message)
    }

    override fun warning(anchor: View, message: String) {
        delegate.warning(anchor, message)
    }

    override fun action(
        anchor: View,
        message: String,
        actionText: String,
        onAction: () -> Unit,
    ) {
        delegate.action(anchor, message, actionText, onAction)
    }
}

/**
 * 默认实现：基于 Snackbar 的 UiNotifier。
 *
 * 注意：这里是一个"当前版本"的默认策略。
 * 未来如果你希望全局改成自定义 Toast / 顶部 Banner，可以：
 * - 新建一个实现类 MyCustomUiNotifier
 * - 在 App 启动时：UiNotifierManager.delegate = MyCustomUiNotifier()
 */
class SnackbarUiNotifier : UiNotifier {

    override fun info(anchor: View, message: String) {
        show(anchor, message, Snackbar.LENGTH_SHORT, MessageType.INFO)
    }

    override fun success(anchor: View, message: String) {
        show(anchor, message, Snackbar.LENGTH_SHORT, MessageType.SUCCESS)
    }

    override fun error(anchor: View, message: String) {
        show(anchor, message, Snackbar.LENGTH_LONG, MessageType.ERROR)
    }

    override fun warning(anchor: View, message: String) {
        show(anchor, message, Snackbar.LENGTH_LONG, MessageType.WARNING)
    }

    override fun action(
        anchor: View,
        message: String,
        actionText: String,
        onAction: () -> Unit,
    ) {
        val snackbar = Snackbar.make(anchor, message, Snackbar.LENGTH_LONG)
            .setAction(actionText) { onAction() }
        applyMessageTypeStyle(snackbar, MessageType.INFO)
        snackbar.show()
    }

    private fun show(anchor: View, message: String, duration: Int, type: MessageType) {
        // 对于长文本，自动延长显示时长
        val actualDuration = if (message.length > 50) {
            Snackbar.LENGTH_LONG
        } else {
            duration
        }
        val snackbar = Snackbar.make(anchor, message, actualDuration)
        applyMessageTypeStyle(snackbar, type)
        snackbar.show()
    }

    private fun applyMessageTypeStyle(snackbar: Snackbar, type: MessageType) {
        val context = snackbar.context
        val backgroundColor = when (type) {
            MessageType.INFO -> ContextCompat.getColor(context, R.color.coreui_notify_info_bg)
            MessageType.SUCCESS -> ContextCompat.getColor(context, R.color.coreui_notify_success_bg)
            MessageType.ERROR -> ContextCompat.getColor(context, R.color.coreui_notify_error_bg)
            MessageType.WARNING -> ContextCompat.getColor(context, R.color.coreui_notify_warning_bg)
        }
        
        // 创建带圆角的背景 Drawable（椭圆效果）
        val drawable = GradientDrawable().apply {
            setColor(backgroundColor)
            // 使用较大的圆角半径实现椭圆效果
            cornerRadius = 24 * context.resources.displayMetrics.density // 24dp 转 px
        }
        
        // 设置背景（带圆角）
        ViewCompat.setBackground(snackbar.view, drawable)
        
        // 设置 padding（左右 14dp，上下 10dp）
        val paddingHorizontal = (14 * context.resources.displayMetrics.density).toInt() // 14dp 转 px
        val paddingVertical = (10 * context.resources.displayMetrics.density).toInt() // 10dp 转 px
        snackbar.view.setPadding(paddingHorizontal, paddingVertical, paddingHorizontal, paddingVertical)
        
        // 设置文本颜色为白色，确保在彩色背景上可读
        val textView = snackbar.view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
        textView?.apply {
            setTextColor(ContextCompat.getColor(context, R.color.coreui_notify_text))
            // 允许显示多行文本，最多显示 5 行
            maxLines = 5
            // 超出部分用省略号显示
            ellipsize = android.text.TextUtils.TruncateAt.END
            // 允许文本换行
            isSingleLine = false
        }
        
        // 设置操作按钮颜色
        val actionView = snackbar.view.findViewById<TextView>(com.google.android.material.R.id.snackbar_action)
        actionView?.setTextColor(ContextCompat.getColor(context, R.color.coreui_notify_text))
    }

    private enum class MessageType {
        INFO, SUCCESS, ERROR, WARNING
    }
}

/**
 * 备用实现：基于自定义 Toast 的 UiNotifier。
 *
 * - 使用自定义布局，避免各家 ROM 魔改系统 Toast 样式带来的不一致。
 * - 当前未作为默认实现，仅作为示例和备用方案。
 *
 * 使用方式（例如在 Application 中）：
 *
 *   UiNotifierManager.delegate = ToastUiNotifier()
 */
class ToastUiNotifier : UiNotifier {

    override fun info(anchor: View, message: String) {
        show(anchor, message, Toast.LENGTH_SHORT, MessageType.INFO)
    }

    override fun success(anchor: View, message: String) {
        show(anchor, message, Toast.LENGTH_SHORT, MessageType.SUCCESS)
    }

    override fun error(anchor: View, message: String) {
        show(anchor, message, Toast.LENGTH_LONG, MessageType.ERROR)
    }

    override fun warning(anchor: View, message: String) {
        show(anchor, message, Toast.LENGTH_LONG, MessageType.WARNING)
    }

    override fun action(
        anchor: View,
        message: String,
        actionText: String,
        onAction: () -> Unit,
    ) {
        // Toast 不适合复杂交互，这里简单拼接提示文案
        show(anchor, "$message（$actionText）", Toast.LENGTH_LONG, MessageType.INFO)
    }

    @Suppress("DEPRECATION")
    private fun show(anchor: View, message: String, length: Int, type: MessageType) {
        val context = anchor.context.applicationContext
        val toast = Toast(context)

        val view = LayoutInflater.from(context)
            .inflate(R.layout.coreui_view_toast, null)

        val textView = view.findViewById<TextView>(R.id.tvToastMessage)
        textView.text = message

        // 根据消息类型设置背景 drawable（带圆角和 padding）
        val backgroundDrawableRes = when (type) {
            MessageType.INFO -> R.drawable.coreui_bg_notify_info
            MessageType.SUCCESS -> R.drawable.coreui_bg_notify_success
            MessageType.ERROR -> R.drawable.coreui_bg_notify_error
            MessageType.WARNING -> R.drawable.coreui_bg_notify_warning
        }
        view.setBackgroundResource(backgroundDrawableRes)

        // 设置文本颜色为白色，确保在彩色背景上可读
        textView.setTextColor(ContextCompat.getColor(context, R.color.coreui_notify_text))

        toast.view = view
        toast.duration = length

        // 底部稍微上移一点，避免紧贴边缘
        val yOffset = (context.resources.displayMetrics.density * 80).toInt()
        toast.setGravity(Gravity.CENTER or Gravity.CENTER_HORIZONTAL, 0, yOffset)

        toast.show()
    }

    private enum class MessageType {
        INFO, SUCCESS, ERROR, WARNING
    }
}



