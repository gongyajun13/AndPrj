package com.jun.core.ui.webview

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebViewClient
import com.jun.core.common.config.WebViewConfig
import timber.log.Timber

/**
 * 统一 WebView
 * 自动选择 X5 WebView 或系统 WebView
 * 
 * 使用方式：
 * ```kotlin
 * val webView = UnifiedWebView(context)
 * webView.applyConfig(WebViewConfig.default())
 * webView.loadUrl("https://example.com")
 * ```
 */
class UnifiedWebView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs) {
    
    private val webView: IWebView
    private val isX5Available: Boolean
    
    init {
        // 检查 X5 SDK 是否可用
        isX5Available = X5WebViewHelper.isX5SdkAvailable() && X5WebViewHelper.checkX5Available(context)
        
        webView = if (isX5Available) {
            Timber.d("[UnifiedWebView] 使用 X5 WebView")
            try {
                createX5WebView(context, attrs, defStyleAttr)
            } catch (e: Exception) {
                Timber.e(e, "[UnifiedWebView] 创建 X5 WebView 失败，回退到系统 WebView")
                SystemWebViewWrapper(context, attrs, defStyleAttr).asIWebView()
            }
        } else {
            Timber.d("[UnifiedWebView] 使用系统 WebView")
            SystemWebViewWrapper(context, attrs, defStyleAttr).asIWebView()
        }
        
        // 将 WebView 添加到当前 View
        if (webView is View) {
            val layoutParams = android.view.ViewGroup.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT
            )
            (webView as View).layoutParams = layoutParams
        }
    }
    
    /**
     * 创建 X5 WebView
     */
    private fun createX5WebView(context: Context, attrs: AttributeSet?, defStyleAttr: Int): IWebView {
        return try {
            val x5WebView = X5WebView(context, attrs, defStyleAttr)
            X5WebViewAdapter(x5WebView)
        } catch (e: Exception) {
            Timber.e(e, "[UnifiedWebView] 创建 X5 WebView 失败")
            throw e
        }
    }
    
    /**
     * 应用配置
     */
    fun applyConfig(config: WebViewConfig) {
        when (val wv = webView) {
            is X5WebViewAdapter -> wv.applyConfig(config)
            else -> {
                // 对于系统 WebView，需要通过 getWebView() 获取实际实例
                val actualWebView = wv.getWebView()
                when (actualWebView) {
                    is SystemWebViewWrapper -> actualWebView.applyConfig(config)
                    else -> Timber.w("[UnifiedWebView] 未知的 WebView 类型，无法应用配置")
                }
            }
        }
    }
    
    /**
     * 获取实际的 WebView 实例
     */
    fun getWebView(): IWebView = webView
    
    /**
     * 获取 View 实例（用于添加到布局）
     */
    fun getView(): View {
        return when (val wv = webView) {
            is View -> wv
            else -> throw IllegalStateException("WebView 不是 View 实例")
        }
    }
    
    /**
     * 是否使用 X5 内核
     */
    fun isUsingX5(): Boolean = isX5Available
    
    // 委托方法
    fun loadUrl(url: String) = webView.loadUrl(url)
    fun loadData(html: String, mimeType: String = "text/html", encoding: String = "utf-8") = 
        webView.loadData(html, mimeType, encoding)
    fun loadDataWithBaseURL(baseUrl: String?, data: String, mimeType: String = "text/html", encoding: String = "utf-8", historyUrl: String? = null) =
        webView.loadDataWithBaseURL(baseUrl, data, mimeType, encoding, historyUrl)
    fun setWebViewClient(client: WebViewClient) = webView.setWebViewClient(client)
    fun setWebChromeClient(client: WebChromeClient) = webView.setWebChromeClient(client)
    fun reload() = webView.reload()
    fun goBack() = webView.goBack()
    fun goForward() = webView.goForward()
    fun canGoBack() = webView.canGoBack()
    fun canGoForward() = webView.canGoForward()
    fun stopLoading() = webView.stopLoading()
    fun clearHistory() = webView.clearHistory()
    fun clearCache(includeDiskFiles: Boolean = true) = webView.clearCache(includeDiskFiles)
    fun destroy() = webView.destroy()
}

/**
 * X5 WebView 适配器
 * 将 X5 WebView 适配到 IWebView 接口
 */
private class X5WebViewAdapter(
    private val x5WebView: X5WebView
) : IWebView {
    
    fun applyConfig(config: WebViewConfig) {
        x5WebView.applyConfig(config)
    }
    
    override fun loadUrl(url: String) {
        x5WebView.loadUrl(url)
    }
    
    override fun loadData(html: String, mimeType: String, encoding: String) {
        x5WebView.loadData(html, mimeType, encoding)
    }
    
    override fun loadDataWithBaseURL(baseUrl: String?, data: String, mimeType: String, encoding: String, historyUrl: String?) {
        x5WebView.loadDataWithBaseURL(baseUrl, data, mimeType, encoding, historyUrl)
    }
    
    override fun setWebViewClient(client: WebViewClient) {
        // 将系统 WebViewClient 转换为 X5 WebViewClient
        val x5Client = object : com.tencent.smtt.sdk.WebViewClient() {
            override fun onPageStarted(view: com.tencent.smtt.sdk.WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                client.onPageStarted(null, url, favicon)
            }
            
            override fun onPageFinished(view: com.tencent.smtt.sdk.WebView?, url: String?) {
                client.onPageFinished(null, url)
            }
            
            override fun onReceivedError(
                view: com.tencent.smtt.sdk.WebView?,
                errorCode: Int,
                description: String?,
                failingUrl: String?
            ) {
                client.onReceivedError(null, errorCode, description, failingUrl)
            }
        }
        x5WebView.setWebViewClient(x5Client)
    }
    
    override fun setWebChromeClient(client: WebChromeClient) {
        // 将系统 WebChromeClient 转换为 X5 WebChromeClient
        val x5Client = object : com.tencent.smtt.sdk.WebChromeClient() {
            override fun onReceivedTitle(view: com.tencent.smtt.sdk.WebView?, title: String?) {
                client.onReceivedTitle(null, title)
            }
            
            override fun onProgressChanged(view: com.tencent.smtt.sdk.WebView?, newProgress: Int) {
                client.onProgressChanged(null, newProgress)
            }
        }
        x5WebView.setWebChromeClient(x5Client)
    }
    
    override fun reload() {
        x5WebView.reload()
    }
    
    override fun goBack() {
        x5WebView.goBack()
    }
    
    override fun goForward() {
        x5WebView.goForward()
    }
    
    override fun canGoBack(): Boolean {
        return x5WebView.canGoBack()
    }
    
    override fun canGoForward(): Boolean {
        return x5WebView.canGoForward()
    }
    
    override fun stopLoading() {
        x5WebView.stopLoading()
    }
    
    override fun clearHistory() {
        x5WebView.clearHistory()
    }
    
    override fun clearCache(includeDiskFiles: Boolean) {
        x5WebView.clearCache(includeDiskFiles)
    }
    
    override fun destroy() {
        x5WebView.destroy()
    }
    
    override fun getWebView(): Any = x5WebView
}


