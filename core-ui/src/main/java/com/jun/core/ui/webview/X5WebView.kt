package com.jun.core.ui.webview

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import com.jun.core.common.config.WebViewConfig
import com.tencent.smtt.sdk.WebChromeClient
import com.tencent.smtt.sdk.WebSettings
import com.tencent.smtt.sdk.WebView as X5WebViewBase
import com.tencent.smtt.sdk.WebViewClient
import timber.log.Timber

/**
 * X5 WebView 封装
 * 基于腾讯 X5 内核的 WebView 组件
 * 
 * 特性：
 * - 自动使用 X5 内核
 * - 支持自定义配置
 * - 生命周期管理
 * - 错误处理
 */
class X5WebView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : X5WebViewBase(context, attrs) {
    
    private var webViewConfig: WebViewConfig? = null
    
    init {
        initWebView()
    }
    
    /**
     * 初始化 WebView
     */
    private fun initWebView() {
        val config = webViewConfig ?: WebViewConfig.default()
        applyConfig(config)
    }
    
    /**
     * 应用配置
     */
    fun applyConfig(config: WebViewConfig) {
        this.webViewConfig = config
        
        settings.apply {
            // JavaScript
            javaScriptEnabled = config.enableJavaScript
            
            // DOM 存储
            domStorageEnabled = config.enableDomStorage
            
            // 数据库存储
            databaseEnabled = config.enableDatabase
            
            // 文件访问
            allowFileAccess = config.enableFileAccess
            allowContentAccess = config.enableContentAccess
            
            // 安全设置：防止内容泄露
            // 注意：X5 WebView 的 API 可能不同，使用反射或 try-catch 处理
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
                try {
                    // 禁止 file:// 页面访问其他来源的内容（防止 XSS）
                    val allowUniversalMethod = settings.javaClass.getMethod("setAllowUniversalAccessFromFileURLs", Boolean::class.java)
                    allowUniversalMethod.invoke(settings, config.allowUniversalAccessFromFile)
                } catch (e: Exception) {
                    Timber.d("[X5WebView] setAllowUniversalAccessFromFileURLs 方法不可用")
                }
                
                try {
                    // 禁止 JavaScript 访问本地文件
                    val allowFileAccessMethod = settings.javaClass.getMethod("setAllowFileAccessFromFileURLs", Boolean::class.java)
                    allowFileAccessMethod.invoke(settings, config.allowFileAccessFromFileURLs)
                } catch (e: Exception) {
                    Timber.d("[X5WebView] setAllowFileAccessFromFileURLs 方法不可用")
                }
            }
            
            // 缩放
            setSupportZoom(config.enableZoom)
            builtInZoomControls = config.enableBuiltInZoomControls
            displayZoomControls = config.enableDisplayZoomControls
            
            // 混合内容（X5 WebView 的混合内容设置方式不同）
            // X5 WebView 默认支持混合内容，如果需要禁用，可以通过其他方式
            // 这里暂时不设置，因为 X5 SDK 的 API 可能不同
            
            // 安全浏览（Android 8.0+）
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                try {
                    val safeBrowsingEnabledMethod = settings.javaClass.getMethod("setSafeBrowsingEnabled", Boolean::class.java)
                    safeBrowsingEnabledMethod.invoke(settings, config.enableSafeBrowsing)
                } catch (e: Exception) {
                    Timber.d("[X5WebView] setSafeBrowsingEnabled 方法不可用")
                }
            }
            
            // 缓存模式
            cacheMode = config.cacheMode
            
            // 用户代理
            config.userAgentString?.let {
                userAgentString = it
            }
            
            // 其他优化设置
            loadWithOverviewMode = true
            useWideViewPort = true
            setRenderPriority(WebSettings.RenderPriority.HIGH)
            
            // 启用硬件加速（如果配置允许）
            if (config.enableHardwareAcceleration) {
                setRenderPriority(WebSettings.RenderPriority.HIGH)
            }
        }
        
        Timber.d("[X5WebView] 配置已应用: JavaScript=${config.enableJavaScript}, CacheMode=${config.cacheMode}")
    }
    
    /**
     * 设置 WebViewClient
     */
    override fun setWebViewClient(client: WebViewClient) {
        webViewClient = client
    }
    
    /**
     * 设置 WebChromeClient
     */
    override fun setWebChromeClient(client: WebChromeClient) {
        webChromeClient = client
    }
    
    /**
     * 加载 URL
     */
    override fun loadUrl(url: String) {
        Timber.d("[X5WebView] 加载 URL: $url")
        super.loadUrl(url)
    }
    
    /**
     * 加载 HTML 内容
     */
    override fun loadData(html: String, mimeType: String, encoding: String) {
        Timber.d("[X5WebView] 加载 HTML 内容")
        super.loadData(html, mimeType, encoding)
    }
    
    /**
     * 加载 HTML 内容（带 BaseUrl）
     */
    override fun loadDataWithBaseURL(
        baseUrl: String?,
        data: String,
        mimeType: String,
        encoding: String,
        historyUrl: String?
    ) {
        Timber.d("[X5WebView] 加载 HTML 内容（BaseUrl: $baseUrl）")
        super.loadDataWithBaseURL(baseUrl, data, mimeType, encoding, historyUrl)
    }
    
    /**
     * 清理资源
     * 在 Activity/Fragment 的 onDestroy 中调用
     */
    override fun destroy() {
        try {
            // 停止加载
            stopLoading()
            
            // 清理历史记录
            clearHistory()
            
            // 从父容器中移除
            (parent as? ViewGroup)?.removeView(this)
            
            // 销毁 WebView
            super.destroy()
            
            Timber.d("[X5WebView] WebView 已销毁")
        } catch (e: Exception) {
            Timber.e(e, "[X5WebView] 销毁 WebView 失败")
        }
    }
    
    /**
     * 清理缓存
     */
    override fun clearCache(includeDiskFiles: Boolean) {
        try {
            super.clearCache(includeDiskFiles)
            Timber.d("[X5WebView] 缓存已清理")
        } catch (e: Exception) {
            Timber.e(e, "[X5WebView] 清理缓存失败")
        }
    }
    
    /**
     * 检查是否可以返回
     */
    override fun canGoBack(): Boolean {
        return try {
            super.canGoBack()
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 返回上一页
     */
    override fun goBack() {
        if (canGoBack()) {
            super.goBack()
        }
    }
    
    /**
     * 检查是否可以前进
     */
    override fun canGoForward(): Boolean {
        return try {
            super.canGoForward()
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 前进到下一页
     */
    override fun goForward() {
        if (canGoForward()) {
            super.goForward()
        }
    }
}

