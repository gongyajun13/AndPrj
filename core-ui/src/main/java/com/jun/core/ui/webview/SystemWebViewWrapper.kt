package com.jun.core.ui.webview

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import com.jun.core.common.config.WebViewConfig
import timber.log.Timber

/**
 * 系统 WebView 包装类
 * 使用系统 WebView，通过适配器实现 IWebView 接口
 */
class SystemWebViewWrapper @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : WebView(context, attrs) {
    
    init {
        initWebView()
    }
    
    private fun initWebView() {
        val config = WebViewConfig.default()
        applyConfig(config)
    }
    
    /**
     * 应用配置
     */
    fun applyConfig(config: WebViewConfig) {
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
                    WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                } else {
                    WebSettings.MIXED_CONTENT_NEVER_ALLOW
                }
            }
            
            // 安全浏览（Android 8.0+）
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                try {
                    val safeBrowsingEnabledMethod = settings.javaClass.getMethod("setSafeBrowsingEnabled", Boolean::class.java)
                    safeBrowsingEnabledMethod.invoke(settings, config.enableSafeBrowsing)
                } catch (e: Exception) {
                    Timber.d("[SystemWebViewWrapper] setSafeBrowsingEnabled 方法不可用")
                }
            }
            
            cacheMode = config.cacheMode
            config.userAgentString?.let { userAgentString = it }
            loadWithOverviewMode = true
            useWideViewPort = true
            setRenderPriority(WebSettings.RenderPriority.HIGH)
        }
        
        Timber.d("[SystemWebViewWrapper] 配置已应用（安全设置已启用）")
    }
    
    /**
     * 转换为 IWebView 接口
     */
    fun asIWebView(): IWebView = SystemWebViewAdapter(this)
    
    /**
     * 转换为 IWebView 接口的适配器
     */
    private class SystemWebViewAdapter(
        private val webView: SystemWebViewWrapper
    ) : IWebView {
        override fun loadUrl(url: String) = webView.loadUrl(url)
        override fun loadData(html: String, mimeType: String, encoding: String) = 
            webView.loadData(html, mimeType, encoding)
        override fun loadDataWithBaseURL(baseUrl: String?, data: String, mimeType: String, encoding: String, historyUrl: String?) =
            webView.loadDataWithBaseURL(baseUrl, data, mimeType, encoding, historyUrl)
        override fun setWebViewClient(client: WebViewClient) = webView.setWebViewClient(client)
        override fun setWebChromeClient(client: WebChromeClient) = webView.setWebChromeClient(client)
        override fun reload() = webView.reload()
        override fun goBack() = webView.goBack()
        override fun goForward() = webView.goForward()
        override fun canGoBack() = webView.canGoBack()
        override fun canGoForward() = webView.canGoForward()
        override fun stopLoading() = webView.stopLoading()
        override fun clearHistory() = webView.clearHistory()
        override fun clearCache(includeDiskFiles: Boolean) = webView.clearCache(includeDiskFiles)
        override fun destroy() = webView.destroy()
        override fun getWebView(): Any = webView
    }
    
    override fun destroy() {
        try {
            stopLoading()
            clearHistory()
            (parent as? ViewGroup)?.removeView(this)
            super.destroy()
            Timber.d("[SystemWebViewWrapper] WebView 已销毁")
        } catch (e: Exception) {
            Timber.e(e, "[SystemWebViewWrapper] 销毁 WebView 失败")
        }
    }
}

