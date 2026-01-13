package com.jun.core.ui.base

import androidx.activity.OnBackPressedCallback
import androidx.activity.OnBackPressedDispatcher
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import com.jun.core.ui.extension.hideKeyboard

/**
 * 返回键处理器
 * 封装返回键处理逻辑
 */
class BackPressHandler(
    private val onBackPress: () -> Unit,
    private val hideKeyboardOnBackPress: Boolean = true
) {
    
    private var callback: OnBackPressedCallback? = null
    
    /**
     * 为 Activity 设置返回键处理
     */
    fun setupForActivity(activity: AppCompatActivity, dispatcher: OnBackPressedDispatcher) {
        callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (hideKeyboardOnBackPress) {
                    activity.hideKeyboard()
                }
                onBackPress()
            }
        }
        dispatcher.addCallback(activity as LifecycleOwner, callback!!)
    }
    
    /**
     * 为 Fragment 设置返回键处理
     */
    fun setupForFragment(fragment: Fragment, lifecycleOwner: LifecycleOwner) {
        callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (hideKeyboardOnBackPress) {
                    fragment.activity?.hideKeyboard()
                }
                onBackPress()
            }
        }
        fragment.activity?.onBackPressedDispatcher?.addCallback(
            lifecycleOwner,
            callback!!
        )
    }
    
    /**
     * 移除回调
     */
    fun remove() {
        callback?.remove()
        callback = null
    }
}

