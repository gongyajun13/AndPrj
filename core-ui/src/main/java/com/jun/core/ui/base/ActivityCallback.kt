package com.jun.core.ui.base

import androidx.viewbinding.ViewBinding

/**
 * Activity 回调接口
 * 定义所有需要子类实现或重写的回调方法
 * 
 * 使用示例：
 * ```kotlin
 * class MyActivity : BaseActivity<ActivityMainBinding>(), ActivityCallback {
 *     override fun setupViews() {
 *         // 初始化视图
 *     }
 * }
 * ```
 */
interface ActivityCallback {
    
    /**
     * 设置视图（可选）
     */
    fun setupViews() {}
    
    /**
     * 设置观察者（可选）
     */
    fun setupObservers() {}
    
    /**
     * 设置监听器（可选）
     */
    fun setupListeners() {}
    
    /**
     * 处理返回键（可选）
     */
    fun handleBackPress() {}
    
    /**
     * 显示加载指示器（可选）
     */
    fun showLoading() {}
    
    /**
     * 隐藏加载指示器（可选）
     */
    fun hideLoading() {}
}

