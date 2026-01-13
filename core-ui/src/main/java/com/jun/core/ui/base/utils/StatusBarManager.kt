package com.jun.core.ui.base.utils

import android.view.Window
import android.view.WindowManager
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.WindowInsetsCompat

/**
 * 状态栏和导航栏管理器
 * 封装状态栏和导航栏的配置逻辑
 */
class StatusBarManager(private val window: Window) {
    
    private val insetsController: WindowInsetsControllerCompat by lazy {
        WindowInsetsControllerCompat(window, window.decorView)
    }
    
    /**
     * 设置全屏
     */
    fun setFullScreen() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        insetsController.apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
    
    /**
     * 退出全屏
     */
    fun exitFullScreen() {
        WindowCompat.setDecorFitsSystemWindows(window, true)
        insetsController.show(WindowInsetsCompat.Type.systemBars())
    }
    
    /**
     * 设置沉浸式状态栏
     */
    fun setImmersiveStatusBar(
        lightStatusBar: Boolean = false,
        lightNavigationBar: Boolean = false
    ) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        insetsController.apply {
            isAppearanceLightStatusBars = lightStatusBar
            isAppearanceLightNavigationBars = lightNavigationBar
        }
    }
    
    /**
     * 设置状态栏颜色
     */
    @Suppress("DEPRECATION")
    fun setStatusBarColor(color: Int, lightIcons: Boolean = false) {
        window.statusBarColor = color
        insetsController.isAppearanceLightStatusBars = lightIcons
    }
    
    /**
     * 设置导航栏颜色
     */
    @Suppress("DEPRECATION")
    fun setNavigationBarColor(color: Int, lightIcons: Boolean = false) {
        window.navigationBarColor = color
        insetsController.isAppearanceLightNavigationBars = lightIcons
    }
    
    /**
     * 设置系统栏颜色
     */
    fun setSystemBarsColor(
        statusBarColor: Int,
        navigationBarColor: Int,
        lightStatusBar: Boolean = false,
        lightNavigationBar: Boolean = false
    ) {
        setStatusBarColor(statusBarColor, lightStatusBar)
        setNavigationBarColor(navigationBarColor, lightNavigationBar)
    }
    
    /**
     * 隐藏状态栏
     */
    fun hideStatusBar() {
        insetsController.hide(WindowInsetsCompat.Type.statusBars())
    }
    
    /**
     * 显示状态栏
     */
    fun showStatusBar() {
        insetsController.show(WindowInsetsCompat.Type.statusBars())
    }
    
    /**
     * 隐藏导航栏
     */
    fun hideNavigationBar() {
        insetsController.hide(WindowInsetsCompat.Type.navigationBars())
    }
    
    /**
     * 显示导航栏
     */
    fun showNavigationBar() {
        insetsController.show(WindowInsetsCompat.Type.navigationBars())
    }
    
    /**
     * 设置保持屏幕常亮
     */
    fun setKeepScreenOn(keepOn: Boolean = true) {
        if (keepOn) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
}

