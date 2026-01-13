package com.jun.core.ui.webview.client

import com.jun.core.common.config.WebViewConfig
import com.jun.core.ui.webview.WebViewCallback
import com.tencent.smtt.sdk.WebView
import com.tencent.smtt.sdk.WebViewClient
import timber.log.Timber

/**
 * X5 WebViewClient 包装类
 * 封装 X5 WebView 的 WebViewClient 逻辑
 */
class X5WebViewClientWrapper(
    private val callback: WebViewCallback,
    private val config: WebViewConfig
) : WebViewClient() {
    
    override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
        url?.let {
            // 安全拦截：阻止 file:// 协议（除非配置允许）
            if (!config.allowFileScheme && it.startsWith("file://", ignoreCase = true)) {
                Timber.w("[X5WebViewClientWrapper] 已阻止 file:// 协议访问")
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
        request: com.tencent.smtt.export.external.interfaces.WebResourceRequest?,
        errorResponse: com.tencent.smtt.export.external.interfaces.WebResourceResponse?
    ) {
        super.onReceivedHttpError(view, request, errorResponse)
        request?.url?.let { url ->
            val statusCode = errorResponse?.statusCode ?: 0
            callback.onReceivedHttpError(statusCode, url.toString())
        }
    }
    
    override fun onReceivedSslError(
        view: WebView?,
        handler: com.tencent.smtt.export.external.interfaces.SslErrorHandler?,
        error: com.tencent.smtt.export.external.interfaces.SslError?
    ) {
        // 默认拒绝 SSL 错误，防止中间人攻击
        Timber.e("[X5WebViewClientWrapper] SSL 错误已拒绝（安全考虑）: ${error?.getPrimaryError()}")
        handler?.cancel() // 拒绝连接
        error?.url?.let { url ->
            callback.onReceivedSslError(url)
        }
    }
}

