package com.jun.core.ui.webview

import android.view.ViewGroup
import androidx.viewbinding.ViewBinding
import com.jun.core.ui.base.BaseActivity
import com.jun.core.ui.webview.client.SystemWebChromeClientWrapper
import com.jun.core.ui.webview.client.SystemWebViewClientWrapper
import com.jun.core.ui.webview.client.X5WebChromeClientWrapper
import com.jun.core.ui.webview.client.X5WebViewClientWrapper

/**
 * WebView Activity 基类
 * 提供 WebView 的通用功能
 * 
 * 使用示例：
 * ```kotlin
 * class MyWebViewActivity : BaseWebViewActivity<ActivityWebViewBinding>(), WebViewCallback {
 *     override fun createBinding() = ActivityWebViewBinding.inflate(layoutInflater)
 *     
 *     override fun getWebViewContainer(): ViewGroup = binding.webViewContainer
 *     
 *     override fun onPageStarted(url: String) {
 *         // 页面开始加载
 *     }
 * }
 * ```
 */
abstract class BaseWebViewActivity<VB : ViewBinding> : BaseActivity<VB>(), WebViewCallback {
    
    protected var webView: Any? = null
        private set
    
    override fun setupViews() {
        super.setupViews()
        initWebView()
    }
    
    /**
     * 初始化 WebView
     */
    private fun initWebView() {
        val container = getWebViewContainer()
        val config = getWebViewConfig()
        
        // 创建或获取 WebView
        val webViewInstance = WebViewManager.createWebView(this, config)
        webView = webViewInstance
        
        // 设置布局参数
        (webViewInstance as? android.view.View)?.apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        
        // 设置 WebViewClient 和 WebChromeClient
        when (webViewInstance) {
            is SystemWebViewWrapper -> {
                webViewInstance.setWebViewClient(
                    SystemWebViewClientWrapper(this, config)
                )
                webViewInstance.setWebChromeClient(
                    SystemWebChromeClientWrapper(this)
                )
                
                // 设置下载监听器（使用下载拦截器）
                getDownloadInterceptor()?.let { interceptor ->
                    webViewInstance.setDownloadListener(interceptor.createSystemDownloadListener())
                }
            }
            is X5WebView -> {
                webViewInstance.setWebViewClient(
                    X5WebViewClientWrapper(this, config)
                )
                webViewInstance.webChromeClient = X5WebChromeClientWrapper(this)
                
                // 设置下载监听器（使用下载拦截器）
                getDownloadInterceptor()?.let { interceptor ->
                    webViewInstance.setDownloadListener(interceptor.createX5DownloadListener())
                }
            }
        }
        
        container.addView(webViewInstance as android.view.View)
        
        // 加载初始 URL
        getInitialUrl()?.let { url ->
            when (webViewInstance) {
                is SystemWebViewWrapper -> webViewInstance.loadUrl(url)
                is X5WebView -> webViewInstance.loadUrl(url)
            }
        }
    }
    
    override fun onBackPressed() {
        val canGoBack = when (val wv = webView) {
            is SystemWebViewWrapper -> wv.canGoBack()
            is X5WebView -> wv.canGoBack()
            else -> false
        }
        if (canGoBack) {
            when (val wv = webView) {
                is SystemWebViewWrapper -> wv.goBack()
                is X5WebView -> wv.goBack()
            }
        } else {
            super.onBackPressed()
        }
    }
    
    override fun onDestroy() {
        when (val wv = webView) {
            is SystemWebViewWrapper -> wv.destroy()
            is X5WebView -> wv.destroy()
        }
        webView = null
        super.onDestroy()
    }
    
    /**
     * 加载 URL
     */
    fun loadUrl(url: String) {
        when (val wv = webView) {
            is SystemWebViewWrapper -> wv.loadUrl(url)
            is X5WebView -> wv.loadUrl(url)
        }
    }
    
    /**
     * 加载 HTML 内容
     */
    fun loadData(html: String, mimeType: String = "text/html", encoding: String = "utf-8") {
        when (val wv = webView) {
            is SystemWebViewWrapper -> wv.loadData(html, mimeType, encoding)
            is X5WebView -> wv.loadData(html, mimeType, encoding)
        }
    }
    
    /**
     * 返回上一页
     */
    fun goBack() {
        when (val wv = webView) {
            is SystemWebViewWrapper -> wv.goBack()
            is X5WebView -> wv.goBack()
        }
    }
    
    /**
     * 前进到下一页
     */
    fun goForward() {
        when (val wv = webView) {
            is SystemWebViewWrapper -> wv.goForward()
            is X5WebView -> wv.goForward()
        }
    }
    
    /**
     * 重新加载
     */
    fun reload() {
        when (val wv = webView) {
            is SystemWebViewWrapper -> wv.reload()
            is X5WebView -> wv.reload()
        }
    }
}
