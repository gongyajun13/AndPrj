package com.jun.core.ui.webview

import android.content.Context
import android.view.ViewGroup
import com.jun.core.common.config.WebViewConfig
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap

/**
 * WebView 管理器
 * 用于管理 WebView 实例的预加载、复用和生命周期
 * 
 * 功能：
 * - WebView 预加载（秒开优化）
 * - WebView 实例复用
 * - 生命周期管理
 * - 内存管理
 * - 自动选择 X5 或系统 WebView
 */
object WebViewManager {
    
    private val preloadedWebViews = ConcurrentHashMap<String, Any>()
    private val maxPreloadCount = 2 // 最多预加载 2 个 WebView
    
    /**
     * 预加载 WebView
     * @param context Context
     * @param config WebView 配置
     * @param tag 标签（用于标识不同的 WebView）
     */
    fun preloadWebView(
        context: Context,
        config: WebViewConfig = WebViewConfig.default(),
        tag: String = "default"
    ) {
        if (preloadedWebViews.size >= maxPreloadCount) {
            Timber.w("[WebViewManager] 预加载数量已达上限: $maxPreloadCount")
            return
        }
        
        if (preloadedWebViews.containsKey(tag)) {
            Timber.d("[WebViewManager] WebView 已预加载: $tag")
            return
        }
        
        try {
            val webView = if (X5WebViewHelper.checkX5Available(context)) {
                X5WebView(context).apply { applyConfig(config) }
            } else {
                SystemWebViewWrapper(context).apply { applyConfig(config) }
            }
            
            // 设置为不可见，用于预加载
            (webView as? android.view.View)?.apply {
                visibility = android.view.View.GONE
                layoutParams = ViewGroup.LayoutParams(1, 1)
            }
            
            preloadedWebViews[tag] = webView
            Timber.d("[WebViewManager] WebView 预加载成功: $tag")
        } catch (e: Exception) {
            Timber.e(e, "[WebViewManager] 预加载 WebView 失败: $tag")
        }
    }
    
    /**
     * 获取预加载的 WebView
     * @param tag 标签
     * @return WebView 实例，如果不存在则返回 null
     */
    fun getPreloadedWebView(tag: String = "default"): Any? {
        val webView = preloadedWebViews.remove(tag)
        if (webView != null) {
            Timber.d("[WebViewManager] 获取预加载的 WebView: $tag")
        }
        return webView
    }
    
    /**
     * 创建新的 WebView 实例
     * @param context Context
     * @param config WebView 配置
     * @return WebView 实例（X5WebView 或 SystemWebViewWrapper）
     */
    fun createWebView(
        context: Context,
        config: WebViewConfig = WebViewConfig.default()
    ): Any {
        // 尝试获取预加载的 WebView
        val preloaded = getPreloadedWebView()
        if (preloaded != null) {
            Timber.d("[WebViewManager] 使用预加载的 WebView")
            return preloaded
        }
        
        // 创建新的 WebView（优先使用 X5）
        val webView: Any = if (X5WebViewHelper.checkX5Available(context)) {
            Timber.d("[WebViewManager] 创建新的 X5 WebView")
            X5WebView(context).apply { applyConfig(config) }
        } else {
            Timber.d("[WebViewManager] 创建新的系统 WebView")
            SystemWebViewWrapper(context).apply { applyConfig(config) }
        }
        return webView
    }
    
    /**
     * 清理所有预加载的 WebView
     */
    fun clearPreloadedWebViews() {
        preloadedWebViews.values.forEach { webView ->
            try {
                when (webView) {
                    is SystemWebViewWrapper -> webView.destroy()
                    is X5WebView -> webView.destroy()
                }
            } catch (e: Exception) {
                Timber.e(e, "[WebViewManager] 清理预加载 WebView 失败")
            }
        }
        preloadedWebViews.clear()
        Timber.d("[WebViewManager] 所有预加载的 WebView 已清理")
    }
    
    /**
     * 获取预加载的 WebView 数量
     */
    fun getPreloadedCount(): Int = preloadedWebViews.size
}

