package com.jun.core.ui.webview.client

import android.webkit.WebView
import android.webkit.WebViewClient
import com.jun.core.common.config.WebViewConfig
import com.jun.core.ui.webview.WebViewCallback
import timber.log.Timber

/**
 * 系统 WebViewClient 包装类
 * 封装系统 WebView 的 WebViewClient 逻辑
 */
class SystemWebViewClientWrapper(
    private val callback: WebViewCallback,
    private val config: WebViewConfig
) : WebViewClient() {
    
    override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
        url?.let {
            // 安全拦截：阻止 file:// 协议（除非配置允许）
            if (!config.allowFileScheme && it.startsWith("file://", ignoreCase = true)) {
                Timber.w("[SystemWebViewClientWrapper] 已阻止 file:// 协议访问")
                return true // 已处理，不继续加载
            }
            
            // 处理自定义协议
            view?.context?.let { context ->
                if (callback.handleCustomScheme(it, context)) {
                    return true
                }
            }
        }
        return super.shouldOverrideUrlLoading(view, url)
    }
    
    override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
        super.onPageStarted(view, url, favicon)
        url?.let { callback.onPageStarted(it) }
    }
    
    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        url?.let { callback.onPageFinished(it) }
    }
    
    override fun onReceivedError(
        view: WebView?,
        errorCode: Int,
        description: String?,
        failingUrl: String?
    ) {
        super.onReceivedError(view, errorCode, description, failingUrl)
        failingUrl?.let {
            callback.onReceivedError(errorCode, description ?: "未知错误", it)
        }
    }
    
    override fun onReceivedHttpError(
        view: WebView?,
        request: android.webkit.WebResourceRequest?,
        errorResponse: android.webkit.WebResourceResponse?
    ) {
        super.onReceivedHttpError(view, request, errorResponse)
        request?.url?.let { url ->
            val statusCode = errorResponse?.statusCode ?: 0
            callback.onReceivedHttpError(statusCode, url.toString())
        }
    }
    
    override fun onReceivedSslError(
        view: WebView?,
        handler: android.webkit.SslErrorHandler?,
        error: android.net.http.SslError?
    ) {
        // 默认拒绝 SSL 错误，防止中间人攻击
        Timber.e("[SystemWebViewClientWrapper] SSL 错误已拒绝（安全考虑）: ${error?.primaryError}")
        handler?.cancel() // 拒绝连接
        error?.url?.let { url ->
            callback.onReceivedSslError(url)
        }
    }
}

