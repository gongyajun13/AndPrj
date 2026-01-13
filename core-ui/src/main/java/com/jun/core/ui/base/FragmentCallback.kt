package com.jun.core.ui.base

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.viewbinding.ViewBinding

/**
 * Fragment 回调接口
 * 定义所有需要子类实现或重写的回调方法
 * 
 * 使用示例：
 * ```kotlin
 * class MyFragment : BaseFragment<FragmentMainBinding>(), FragmentCallback {
 *     override fun setupViews() {
 *         // 初始化视图
 *     }
 *     
 *     override fun loadData() {
 *         // 懒加载数据
 *     }
 * }
 * ```
 */
interface FragmentCallback {
    
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
     * Fragment 可见时回调（可选）
     */
    fun onVisible() {}
    
    /**
     * Fragment 不可见时回调（可选）
     */
    fun onInvisible() {}
    
    /**
     * 懒加载数据（可选）
     */
    fun loadData() {}
    
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

