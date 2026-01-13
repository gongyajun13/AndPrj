package com.jun.andprj.ui.tool

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.ContextCompat
import com.jun.andprj.R
import com.jun.andprj.databinding.ActivityWebViewDemoBinding
import com.jun.core.common.config.WebViewConfig
import com.jun.core.ui.base.utils.ActivityManager
import com.jun.core.ui.webview.BaseWebViewActivity
import com.jun.core.ui.webview.WebViewCacheUtils
import com.jun.core.ui.webview.X5WebViewHelper
import com.jun.core.ui.webview.bridge.WebViewJsBridge
import com.jun.core.ui.webview.bridge.WebViewJsBridge.JsHandler
import com.jun.core.ui.webview.bridge.JsBridgeHandlerRegistry
import com.jun.core.ui.webview.download.WebViewDownloadInterceptor
import com.jun.core.ui.webview.scheme.CustomSchemeHandler
import com.jun.core.network.client.NetworkClient
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import timber.log.Timber

/**
 * WebView 示例 Activity
 * 演示统一封装的 WebView 使用方式（优先使用 X5 内核，失败则回退到系统 WebView）
 * 并演示 JS Bridge（Native <-> H5）和下载拦截功能
 */
@AndroidEntryPoint
class WebViewDemoActivity : BaseWebViewActivity<ActivityWebViewDemoBinding>() {
    
    @Inject
    lateinit var networkClient: NetworkClient
    
    /**
     * 下载拦截器
     * 只负责拦截下载请求并显示确认弹窗，不直接执行下载
     * 用户确认后，将下载任务交给 DownloadManagerActivity 处理
     */
    private val downloadInterceptorInstance by lazy {
        WebViewDownloadInterceptor(this) { request ->
            // 用户确认下载后，启动 DownloadManagerActivity 并传递下载任务
            DownloadManagerActivity.startWithDownload(this, request)
        }
    }

    /**
     * JS Bridge Handler 注册中心
     * - 负责管理各个 handler（showToast / openPage 等）
     * - 对外作为 JsBridgeReceiver，被 WebViewJsBridge 调用
     */
    private val jsBridgeRegistry by lazy {
        JsBridgeHandlerRegistry(
            webViewProvider = { webView },
            contextProvider = { this }
        )
    }
    
    override fun createBinding(): ActivityWebViewDemoBinding =
        ActivityWebViewDemoBinding.inflate(layoutInflater)
    
    override fun getWebViewContainer() = binding.webViewContainer
    
    override fun getWebViewConfig(): WebViewConfig = WebViewConfig.default()
    
    override fun getInitialUrl(): String? = "https://www.baidu.com"
    
    /**
     * 提供下载拦截器，启用下载拦截功能
     * 下载拦截器只负责显示确认弹窗，不直接执行下载
     * 用户确认后，将下载任务交给 DownloadManagerActivity 处理
     */
    override fun getDownloadInterceptor(): WebViewDownloadInterceptor? = downloadInterceptorInstance
    
    /**
     * 打开下载管理页面
     */
    private fun openDownloadManager() {
        ActivityManager.startActivity<DownloadManagerActivity>(this)
    }
    
    override fun setupViews() {
        super.setupViews()
        
        setupToolbar()
        setupCachePanel()
        setupDownloadManager()
        setupJsBridge()
        
        // 状态栏白底黑字
        val white = ContextCompat.getColor(this, android.R.color.white)
        setStatusBarColor(white, lightIcons = false)
        
        // 注册自定义协议处理器（示例：处理 baiduboxapp://）
        registerCustomSchemeHandlers()
    }
    
    private fun setupToolbar() {
        val white = ContextCompat.getColor(this, android.R.color.white)
        binding.toolbar.setupSimple(
            leftIcon = R.drawable.icon_back,
            leftIconTint = white,
            title = "WebView 示例",
            titleTextColor = white,
            backgroundColor = ContextCompat.getColor(this, R.color.blue),
            onLeftClick = { finish() }
        )
    }

    /**
     * WebView 缓存信息与清理按钮
     */
    private fun setupCachePanel() {
        updateCacheSizeText()

        binding.btnClearCache.setOnClickListener {
            // 清理应用级 WebView 缓存数据
            WebViewCacheUtils.clearAllWebViewData(this)
            // 清理当前 WebView 实例的缓存（如果已创建）
            WebViewCacheUtils.clearWebViewInstanceCache(webView)

            updateCacheSizeText()
            showMessage("WebView 缓存已清理")
        }
    }
    
    /**
     * 设置下载管理按钮
     */
    private fun setupDownloadManager() {
        binding.btnDownloadManager.setOnClickListener {
            openDownloadManager()
        }
    }

    private fun updateCacheSizeText() {
        val sizeText = WebViewCacheUtils.getWebViewCacheSizeFormatted(this)
        binding.tvCacheInfo.text = "当前 WebView 缓存：$sizeText"
    }

    /**
     * 初始化 JS Bridge：注册各业务 Handler
     */
    private fun setupJsBridge() {
        // 示例：注册 showToast handler
        jsBridgeRegistry.registerHandler("showToast", JsHandler { data, callbackId, sender ->
            Timber.d("[WebViewDemoActivity] showToast handler 调用: data=$data, callbackId=$callbackId")

            val msg = parseMessageFromJson(data) ?: "Hello from JS"
            showMessage(msg)

            // 如果有 callbackId，则回调给 H5
            if (!callbackId.isNullOrBlank()) {
                val callbackJson =
                    """{"callbackId":"$callbackId","status":"ok","message":"Toast 已显示"}"""
                WebViewJsBridge.callJsFunction(
                    webView = sender.webView,
                    function = "window.onNativeCallback",
                    jsonData = callbackJson
                )
            }
        })

        Timber.d("[WebViewDemoActivity] JS Bridge 已启用，H5 可通过 window.AppBridge.postMessage(handler, data, callbackId) 调用原生")
    }

    /**
     * 非严格 JSON 解析，只尝试从形如 {"msg":"xxx"} 的字符串中提取 msg 字段
     * 为了避免引入额外依赖，这里用最简单的方式解析
     */
    private fun parseMessageFromJson(json: String?): String? {
        if (json.isNullOrBlank()) return null
        return try {
            val regex = """"msg"\s*:\s*"([^"]*)"""".toRegex()
            val match = regex.find(json)
            match?.groups?.get(1)?.value
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * 注册自定义协议处理器
     * 演示如何为特定 Activity 注册专属的协议处理逻辑
     * 
     * 注意：无论是否成功跳转，都应该返回 true，避免 WebView 继续加载并显示错误页面
     */
    private fun registerCustomSchemeHandlers() {
        // 示例：专门处理 baiduboxapp:// 协议
        // 注意：CommonAppSchemeHandler 已经内置处理了 baiduboxapp://，这里只是演示如何注册自定义处理器
        CustomSchemeHandler.registerHandler(object : CustomSchemeHandler.SchemeHandler {
            override fun canHandle(url: String): Boolean {
                return url.startsWith("baiduboxapp://", ignoreCase = true)
            }
            
            override fun handle(url: String, context: Context): Boolean {
                try {
                    Timber.d("[WebViewDemoActivity] 处理百度 App 协议: $url")
                    
                    // 尝试解析 Intent URI
                    val intent = try {
                        Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
                    } catch (e: Exception) {
                        // 如果解析失败，尝试直接创建 Intent
                        Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    }
                    
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    
                    // 检查是否有 App 可以处理该 Intent
                    if (intent.resolveActivity(context.packageManager) != null) {
                        context.startActivity(intent)
                        Timber.d("[WebViewDemoActivity] 已打开百度 App")
                        // 成功跳转，返回 true 表示已处理，WebView 不会继续加载
                        return true
                    } else {
                        Timber.w("[WebViewDemoActivity] 未安装百度 App，无法打开: $url")
                        // 未安装 App，显示 Toast 提示，但依然返回 true，避免 WebView 显示错误页面
                        showMessage("未安装百度 App，无法打开此链接")
                        return true
                    }
                } catch (e: Exception) {
                    Timber.e(e, "[WebViewDemoActivity] 处理百度 App 协议失败: $url")
                    // 即使处理失败，也返回 true，避免 WebView 显示错误页面
                    showMessage("无法打开此链接")
                    return true
                }
            }
        })
        
        // 可以继续注册其他自定义协议处理器
        // CustomSchemeHandler.registerHandler(MyCustomSchemeHandler())
    }
    
    override fun onPageFinished(url: String) {
        super.onPageFinished(url)
        
        // 显示当前使用的 WebView 类型
        val webViewType = if (X5WebViewHelper.isX5Available()) {
            "X5 内核 (版本: ${X5WebViewHelper.getX5Version(this)})"
        } else {
            "系统 WebView"
        }
        
        Timber.d("[WebViewDemoActivity] 页面加载完成，使用: $webViewType")

        // 在页面加载完成后，为当前 WebView 绑定 JS Bridge
        WebViewJsBridge.attach(webView, jsBridgeRegistry)

        // 可选：注入一个简单的 JS callback 函数示例（H5 可选择性使用）
        // 这里不强依赖 H5 代码，仅当 H5 定义 window.onNativeCallback 时才会生效
    }
    
    override fun onReceivedError(errorCode: Int, description: String, failingUrl: String) {
        super.onReceivedError(errorCode, description, failingUrl)
        
        // 对于自定义协议的错误，不显示错误提示（因为已经在 handleCustomScheme 中处理了）
        if (failingUrl.startsWith("baiduboxapp://", ignoreCase = true) ||
            failingUrl.startsWith("weixin://", ignoreCase = true) ||
            failingUrl.startsWith("alipays://", ignoreCase = true)) {
            // 自定义协议的错误已经在 handleCustomScheme 中处理，这里不显示错误
            return
        }
        
        // 其他错误可以显示提示
        Timber.e("[WebViewDemoActivity] 页面加载错误: $errorCode, $description, $failingUrl")
    }
}
