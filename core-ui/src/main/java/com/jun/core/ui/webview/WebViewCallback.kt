package com.jun.core.ui.webview

import android.content.Context
import android.view.ViewGroup
import com.jun.core.common.config.WebViewConfig
import com.jun.core.ui.webview.download.WebViewDownloadInterceptor

/**
 * WebView 回调接口
 * 定义所有需要子类实现或重写的回调方法
 * 
 * 使用示例：
 * ```kotlin
 * class MyWebViewActivity : BaseWebViewActivity<ActivityWebViewBinding>(), WebViewCallback {
 *     override fun getWebViewContainer(): ViewGroup = binding.webViewContainer
 *     
 *     override fun onPageStarted(url: String) {
 *         // 页面开始加载
 *     }
 * }
 * ```
 */
interface WebViewCallback {
    
    /**
     * 获取 WebView 容器
     * 子类必须实现此方法
     */
    fun getWebViewContainer(): ViewGroup
    
    /**
     * 获取 WebView 配置
     * 子类可以重写此方法来自定义配置
     */
    fun getWebViewConfig(): WebViewConfig = WebViewConfig.default()
    
    /**
     * 获取初始 URL（可选）
     * 子类可以重写此方法来设置初始加载的 URL
     */
    fun getInitialUrl(): String? = null
    
    /**
     * 页面开始加载回调
     */
    fun onPageStarted(url: String) {}
    
    /**
     * 页面加载完成回调
     */
    fun onPageFinished(url: String) {}
    
    /**
     * 页面加载错误回调
     */
    fun onReceivedError(errorCode: Int, description: String, failingUrl: String) {}
    
    /**
     * HTTP 错误回调
     */
    fun onReceivedHttpError(statusCode: Int, url: String) {}
    
    /**
     * SSL 错误回调
     */
    fun onReceivedSslError(url: String) {}
    
    /**
     * 页面标题更新回调
     */
    fun onReceivedTitle(title: String) {}
    
    /**
     * 进度更新回调
     */
    fun onProgressChanged(newProgress: Int) {}
    
    /**
     * 处理自定义协议
     * 子类可以重写此方法来实现自定义的协议处理逻辑
     * @param url 要处理的 URL
     * @param context Context
     * @return true 表示已处理，false 表示未处理（将使用默认处理）
     */
    fun handleCustomScheme(url: String, context: Context): Boolean {
        return com.jun.core.ui.webview.scheme.CustomSchemeHandler.handle(url, context)
    }
    
    /**
     * 获取下载拦截器（可选）
     * 子类可以重写此方法来提供下载拦截器，启用下载拦截功能
     * 
     * @return 下载拦截器，如果返回 null 则不启用下载拦截
     */
    fun getDownloadInterceptor(): WebViewDownloadInterceptor? = null
}

