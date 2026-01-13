package com.jun.core.common.ui

import androidx.fragment.app.FragmentManager

/**
 * Loading Dialog 接口
 * 用于解耦 core-network 和 core-ui 模块
 */
interface LoadingDialog {
    fun showSafely(fragmentManager: FragmentManager, tag: String? = null)
    fun dismissSafely()
    /**
     * 更新 Loading 消息（可选实现）
     * 如果 Dialog 不支持消息更新，可以忽略此方法
     */
    fun updateMessage(message: String) {
        // 默认实现：不做任何操作
    }
}



