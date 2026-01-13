package com.jun.core.ui.base

import android.view.View
import com.jun.core.common.util.notify.UiNotifierManager

/**
 * 消息通知器
 * 封装消息提示逻辑
 */
object MessageNotifier {
    
    /**
     * 显示错误消息
     */
    fun showError(view: View, message: String) {
        UiNotifierManager.error(view, message)
    }
    
    /**
     * 显示成功消息
     */
    fun showSuccess(view: View, message: String) {
        UiNotifierManager.success(view, message)
    }
    
    /**
     * 显示警告消息
     */
    fun showWarning(view: View, message: String) {
        UiNotifierManager.warning(view, message)
    }
    
    /**
     * 显示普通消息
     */
    fun showMessage(view: View, message: String) {
        UiNotifierManager.info(view, message)
    }
}

