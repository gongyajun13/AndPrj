package com.jun.core.ui.webview.client

import android.webkit.WebChromeClient
import android.webkit.WebView
import com.jun.core.ui.webview.WebViewCallback
import timber.log.Timber

/**
 * 系统 WebChromeClient 包装类
 * 封装系统 WebView 的 WebChromeClient 逻辑
 */
class SystemWebChromeClientWrapper(
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
}

