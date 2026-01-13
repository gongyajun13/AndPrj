package com.jun.core.ui.base.utils

import android.view.ViewGroup
import androidx.fragment.app.Fragment

/**
 * Fragment 可见性管理器
 * 处理 Fragment 的可见性检测和懒加载逻辑
 */
class FragmentVisibilityManager(
    private val fragment: Fragment,
    private val enableLazyLoad: Boolean
) {
    
    private var _isDataLoaded = false
    private var _isInViewPager = false
    private var _isUserVisible = false
    
    val isInViewPager: Boolean get() = _isInViewPager
    val isUserVisible: Boolean get() = _isUserVisible
    val isDataLoaded: Boolean get() = _isDataLoaded
    
    /**
     * 检查是否在 ViewPager2 中
     */
    fun checkViewPager() {
        fragment.view?.parent?.let { parent ->
            var currentParent: ViewGroup? = parent as? ViewGroup
            while (currentParent != null) {
                val className = currentParent.javaClass.name
                if (className.contains("ViewPager2") ||
                    className == "androidx.viewpager2.widget.ViewPager2") {
                    _isInViewPager = true
                    return
                }
                currentParent = currentParent.parent as? ViewGroup
            }
        }
    }
    
    /**
     * 处理可见性变化
     * @param onVisible Fragment 可见时回调
     * @param onInvisible Fragment 不可见时回调
     * @param onLoadData 懒加载数据回调
     * @param setupObservers 设置观察者回调
     * @param setupListeners 设置监听器回调
     */
    fun handleVisibilityChange(
        onVisible: () -> Unit,
        onInvisible: () -> Unit,
        onLoadData: () -> Unit,
        setupObservers: () -> Unit,
        setupListeners: () -> Unit
    ) {
        if (!_isInViewPager && fragment.view != null) {
            checkViewPager()
        }
        
        val shouldBeVisible = if (_isInViewPager) {
            fragment.isResumed
        } else {
            fragment.isResumed && fragment.isVisible
        }
        
        when {
            shouldBeVisible && !_isUserVisible -> {
                _isUserVisible = true
                onVisible()
                if (enableLazyLoad && !_isDataLoaded) {
                    _isDataLoaded = true
                    setupObservers()
                    setupListeners()
                    onLoadData()
                }
            }
            !shouldBeVisible && _isUserVisible -> {
                _isUserVisible = false
                onInvisible()
            }
        }
    }
    
    /**
     * 重置懒加载状态
     */
    fun resetLazyLoad(onLoadData: () -> Unit) {
        _isDataLoaded = false
        if (_isUserVisible) {
            onLoadData()
        }
    }
}

