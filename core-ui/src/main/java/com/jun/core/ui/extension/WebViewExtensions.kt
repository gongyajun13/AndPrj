package com.jun.core.ui.extension

import android.webkit.WebView
import com.jun.core.common.config.WebViewConfig
import timber.log.Timber

/**
 * WebView 扩展函数
 */

/**
 * 应用 WebView 配置到系统 WebView
 * 包含安全设置，防止内容泄露
 */
fun WebView.applyConfig(config: WebViewConfig) {
    settings.apply {
        javaScriptEnabled = config.enableJavaScript
        domStorageEnabled = config.enableDomStorage
        databaseEnabled = config.enableDatabase
        allowFileAccess = config.enableFileAccess
        allowContentAccess = config.enableContentAccess
        setSupportZoom(config.enableZoom)
        builtInZoomControls = config.enableBuiltInZoomControls
        displayZoomControls = config.enableDisplayZoomControls
        
        // 安全设置：防止内容泄露
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
            // 禁止 file:// 页面访问其他来源的内容（防止 XSS）
            allowUniversalAccessFromFileURLs = config.allowUniversalAccessFromFile
            // 禁止 JavaScript 访问本地文件
            allowFileAccessFromFileURLs = config.allowFileAccessFromFileURLs
        }
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            mixedContentMode = if (config.enableMixedContent) {
                android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            } else {
                android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW
            }
        }
        
        // 安全浏览（Android 8.0+）
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            try {
                val safeBrowsingEnabledMethod = settings.javaClass.getMethod("setSafeBrowsingEnabled", Boolean::class.java)
                safeBrowsingEnabledMethod.invoke(settings, config.enableSafeBrowsing)
            } catch (e: Exception) {
                Timber.d("[WebViewExtensions] setSafeBrowsingEnabled 方法不可用")
            }
        }
        
        cacheMode = config.cacheMode
        config.userAgentString?.let { userAgentString = it }
        loadWithOverviewMode = true
        useWideViewPort = true
        setRenderPriority(android.webkit.WebSettings.RenderPriority.HIGH)
    }
}

/**
 * 安全地销毁 WebView
 */
fun WebView.destroySafely() {
    try {
        stopLoading()
        clearHistory()
        (parent as? android.view.ViewGroup)?.removeView(this)
        destroy()
        Timber.d("[WebViewExtensions] WebView 已安全销毁")
    } catch (e: Exception) {
        Timber.e(e, "[WebViewExtensions] 销毁 WebView 失败")
    }
}

/**
 * 安全地清理缓存
 */
fun WebView.clearCacheSafely(includeDiskFiles: Boolean = true) {
    try {
        clearCache(includeDiskFiles)
        Timber.d("[WebViewExtensions] WebView 缓存已清理")
    } catch (e: Exception) {
        Timber.e(e, "[WebViewExtensions] 清理缓存失败")
    }
}


