package com.jun.core.ui.webview.client

import com.jun.core.ui.webview.WebViewCallback
import com.tencent.smtt.export.external.interfaces.GeolocationPermissionsCallback
import com.tencent.smtt.sdk.WebChromeClient
import com.tencent.smtt.sdk.WebView
import timber.log.Timber

/**
 * X5 WebChromeClient 包装类
 * 封装 X5 WebView 的 WebChromeClient 逻辑
 */
class X5WebChromeClientWrapper(
    private val callback: WebViewCallback
) : WebChromeClient() {
    
    override fun onReceivedTitle(view: WebView?, title: String?) {
        super.onReceivedTitle(view, title)
        title?.let { callback.onReceivedTitle(it) }
    }
    
    override fun onProgressChanged(view: WebView?, newProgress: Int) {
        super.onProgressChanged(view, newProgress)
        callback.onProgressChanged(newProgress)
    }
    
    /**
     * 安全处理：文件选择器
     * 默认拒绝文件选择请求，防止恶意网站访问本地文件
     */
    override fun onShowFileChooser(
        webView: WebView?,
        filePathCallback: com.tencent.smtt.sdk.ValueCallback<Array<android.net.Uri>>?,
        fileChooserParams: FileChooserParams?
    ): Boolean {
        Timber.w("[X5WebChromeClientWrapper] 已阻止文件选择请求（安全考虑）")
        return false
    }
    
    /**
     * 安全处理：地理位置请求
     * 默认拒绝地理位置请求，防止泄露用户位置
     */
    override fun onGeolocationPermissionsShowPrompt(
        origin: String,
        callback: GeolocationPermissionsCallback
    ) {
        Timber.w("[X5WebChromeClientWrapper] 已阻止地理位置请求（安全考虑）: $origin")
        callback.invoke(origin, false, false)
    }
}

