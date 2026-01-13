package com.jun.core.ui.base

import android.app.Activity
import android.view.View
import com.jun.core.ui.extension.hideKeyboard
import com.jun.core.ui.extension.isKeyboardVisible

/**
 * 软键盘管理器
 * 封装软键盘管理逻辑
 */
object KeyboardManager {
    
    /**
     * 隐藏软键盘（Activity）
     */
    fun hideKeyboard(activity: Activity) {
        val view = activity.currentFocus ?: activity.window.decorView.rootView
        view.hideKeyboard()
    }
    
    /**
     * 隐藏软键盘（Fragment）
     */
    fun hideKeyboard(fragment: androidx.fragment.app.Fragment) {
        fragment.activity?.let { hideKeyboard(it) }
    }
    
    /**
     * 检查软键盘是否可见
     */
    fun isKeyboardVisible(view: View): Boolean {
        return view.isKeyboardVisible()
    }
}

