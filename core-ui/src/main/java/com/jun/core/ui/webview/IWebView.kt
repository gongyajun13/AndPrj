package com.jun.core.ui.webview

import android.webkit.WebChromeClient
import android.webkit.WebViewClient

/**
 * WebView 统一接口
 * 支持 X5 WebView 和系统 WebView
 */
interface IWebView {
    /**
     * 加载 URL
     */
    fun loadUrl(url: String)
    
    /**
     * 加载 HTML 内容
     */
    fun loadData(html: String, mimeType: String = "text/html", encoding: String = "utf-8")
    
    /**
     * 加载 HTML 内容（带 BaseUrl）
     */
    fun loadDataWithBaseURL(
        baseUrl: String?,
        data: String,
        mimeType: String = "text/html",
        encoding: String = "utf-8",
        historyUrl: String? = null
    )
    
    /**
     * 设置 WebViewClient
     */
    fun setWebViewClient(client: WebViewClient)
    
    /**
     * 设置 WebChromeClient
     */
    fun setWebChromeClient(client: WebChromeClient)
    
    /**
     * 重新加载
     */
    fun reload()
    
    /**
     * 返回上一页
     */
    fun goBack()
    
    /**
     * 前进到下一页
     */
    fun goForward()
    
    /**
     * 检查是否可以返回
     */
    fun canGoBack(): Boolean
    
    /**
     * 检查是否可以前进
     */
    fun canGoForward(): Boolean
    
    /**
     * 停止加载
     */
    fun stopLoading()
    
    /**
     * 清理历史记录
     */
    fun clearHistory()
    
    /**
     * 清理缓存
     */
    fun clearCache(includeDiskFiles: Boolean = true)
    
    /**
     * 销毁 WebView
     */
    fun destroy()
    
    /**
     * 获取实际的 WebView 实例（用于需要直接访问的场景）
     */
    fun getWebView(): Any
}



