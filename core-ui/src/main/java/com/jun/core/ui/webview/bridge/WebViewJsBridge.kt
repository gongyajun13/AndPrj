package com.jun.core.ui.webview.bridge

import android.os.Build
import android.webkit.JavascriptInterface
import android.content.Context
import android.webkit.WebView
import com.jun.core.ui.webview.SystemWebViewWrapper
import com.jun.core.ui.webview.X5WebView
import timber.log.Timber

/**
 * 统一的 WebView JS Bridge 工具
 *
 * 目标：
 * - H5 -> Native：通过 addJavascriptInterface 暴露统一入口
 * - Native -> H5：通过 evaluateJavascript / loadUrl 调用 JS
 *
 * 使用方式（示例）：
 *
 * 1. 在 Activity / Fragment 中实现 JsBridgeReceiver：
 *    class WebViewDemoActivity : BaseWebViewActivity<...>(), JsBridgeReceiver {
 *        override fun onJsCall(handler: String, data: String?, callbackId: String?) {
 *            // 根据 handler 分发，比如 showToast / openPage 等
 *        }
 *    }
 *
 * 2. WebView 初始化完成后绑定：
 *    WebViewJsBridge.attach(webView, this)
 *
 * 3. H5 侧调用：
 *    window.AppBridge.postMessage('showToast', '{"msg":"Hello"}', 'cb_1')
 *
 * 4. Native 回调 H5（可选）：
 *    WebViewJsBridge.callJsFunction(webView, "window.onNativeCallback", "{\"callbackId\":\"cb_1\",\"result\":\"ok\"}")
 */
object WebViewJsBridge {

    /**
     * JS 调 Native 的统一接收接口
     */
    interface JsBridgeReceiver {
        /**
         * @param handler    JS 请求的 handler 名称（例如 showToast / openPage）
         * @param data       业务参数（JSON 字符串，H5 自行约定结构）
         * @param callbackId 回调 ID（H5 生成，用于 Native -> JS 回调对应这次请求）
         */
        fun onJsCall(handler: String, data: String?, callbackId: String?)
    }

    /**
     * JS Handler 接口
     * 建议按功能拆分多个 Handler 实现类，再通过 JsBridgeHandlerRegistry 进行注册
     */
    fun interface JsHandler {
        /**
         * @param data       业务参数（JSON 字符串）
         * @param callbackId 回调 ID（可为空）
         * @param sender     当前调用来源（包含 webView / context 等）
         */
        fun handle(data: String?, callbackId: String?, sender: JsCallSender)
    }

    /**
     * JS 调用的上下文信息
     */
    data class JsCallSender(
        val webView: Any?,
        val context: Context?
    )

    private const val DEFAULT_INTERFACE_NAME = "AppBridge"

    /**
     * 绑定 JS Bridge 到具体 WebView 实例（系统 WebView / X5 WebView / 包装类）
     */
    fun attach(webView: Any?, receiver: JsBridgeReceiver, interfaceName: String = DEFAULT_INTERFACE_NAME) {
        if (webView == null) return

        val bridge = NativeBridge(receiver)

        try {
            when (webView) {
                is SystemWebViewWrapper -> {
                    webView.addJavascriptInterface(bridge, interfaceName)
                }
                is X5WebView -> {
                    webView.addJavascriptInterface(bridge, interfaceName)
                }
                is WebView -> {
                    webView.addJavascriptInterface(bridge, interfaceName)
                }
                is com.tencent.smtt.sdk.WebView -> {
                    webView.addJavascriptInterface(bridge, interfaceName)
                }
                else -> {
                    Timber.w("[WebViewJsBridge] 不支持的 WebView 类型: ${webView::class.java.name}")
                }
            }
            Timber.d("[WebViewJsBridge] 已绑定 JS Bridge ($interfaceName) 到 ${webView::class.java.simpleName}")
        } catch (e: Exception) {
            Timber.e(e, "[WebViewJsBridge] 绑定 JS Bridge 失败")
        }
    }

    /**
     * 解除绑定（仅在需要严格控制生命周期时使用）
     */
    fun detach(webView: Any?, interfaceName: String = DEFAULT_INTERFACE_NAME) {
        if (webView == null) return

        try {
            when (webView) {
                is SystemWebViewWrapper -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                        webView.removeJavascriptInterface(interfaceName)
                    }
                }
                is X5WebView -> {
                    try {
                        webView.removeJavascriptInterface(interfaceName)
                    } catch (e: Exception) {
                        Timber.d("[WebViewJsBridge] X5 removeJavascriptInterface 不可用")
                    }
                }
                is WebView -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                        webView.removeJavascriptInterface(interfaceName)
                    }
                }
                is com.tencent.smtt.sdk.WebView -> {
                    try {
                        webView.removeJavascriptInterface(interfaceName)
                    } catch (e: Exception) {
                        Timber.d("[WebViewJsBridge] X5 removeJavascriptInterface 不可用")
                    }
                }
            }
            Timber.d("[WebViewJsBridge] 已解除 JS Bridge ($interfaceName) 绑定")
        } catch (e: Exception) {
            Timber.e(e, "[WebViewJsBridge] 解除 JS Bridge 失败")
        }
    }

    /**
     * Native 调用 JS 函数
     *
     * @param function JS 函数表达式，例如：
     *        "window.onNativeMessage" 或 "onNativeMessage"
     * @param jsonData 传入的 JSON 字符串参数（可为空）
     * @param callback 可选回调（仅在支持 evaluateJavascript 时能拿到返回值）
     */
    fun callJsFunction(
        webView: Any?,
        function: String,
        jsonData: String? = null,
        callback: ((String?) -> Unit)? = null
    ) {
        if (webView == null) return

        // 拼接 JS 调用语句
        // 如果有 jsonData，则作为一个参数传入；否则不传参
        val js = if (jsonData.isNullOrBlank()) {
            "$function()"
        } else {
            "$function($jsonData)"
        }

        val script = if (js.startsWith("javascript:", ignoreCase = true)) js else "javascript:$js"

        try {
            when (webView) {
                is SystemWebViewWrapper -> {
                    evaluateJavascriptCompat(webView, script, callback)
                }
                is X5WebView -> {
                    evaluateJavascriptCompat(webView, script, callback)
                }
                is WebView -> {
                    evaluateJavascriptCompat(webView, script, callback)
                }
                is com.tencent.smtt.sdk.WebView -> {
                    evaluateJavascriptCompat(webView, script, callback)
                }
                else -> Timber.w("[WebViewJsBridge] 不支持的 WebView 类型: ${webView::class.java.name}")
            }
        } catch (e: Exception) {
            Timber.e(e, "[WebViewJsBridge] 调用 JS 函数失败: $script")
        }
    }

    // region 内部实现

    /**
     * 暴露给 JS 的原生对象
     */
    private class NativeBridge(
        private val receiver: JsBridgeReceiver
    ) {
        /**
         * 统一入口：
         * window.AppBridge.postMessage(handler, dataJson, callbackId)
         */
        @JavascriptInterface
        fun postMessage(handler: String, data: String?, callbackId: String?) {
            try {
                Timber.d("[WebViewJsBridge] 接收到 JS 调用: handler=$handler, callbackId=$callbackId, data=$data")
                receiver.onJsCall(handler, data, callbackId)
            } catch (e: Exception) {
                Timber.e(e, "[WebViewJsBridge] 处理 JS 调用失败")
            }
        }
    }

    private fun evaluateJavascriptCompat(
        webView: WebView,
        script: String,
        callback: ((String?) -> Unit)?
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            webView.evaluateJavascript(script) { result -> callback?.invoke(result) }
        } else {
            webView.loadUrl(script)
            callback?.invoke(null)
        }
    }

    private fun evaluateJavascriptCompat(
        webView: com.tencent.smtt.sdk.WebView,
        script: String,
        callback: ((String?) -> Unit)?
    ) {
        try {
            // X5 WebView 也支持 evaluateJavascript
            webView.evaluateJavascript(script) { result -> callback?.invoke(result) }
        } catch (e: Throwable) {
            // 兜底：用 loadUrl
            Timber.d("[WebViewJsBridge] X5 evaluateJavascript 调用失败，使用 loadUrl 兜底")
            webView.loadUrl(script)
            callback?.invoke(null)
        }
    }

    // endregion
}

/**
 * 通用的 JS Handler 路由器 / 注册中心
 *
 * 作用：
 * - 提供 handler 注册 / 反注册能力
 * - 实现 JsBridgeReceiver 接口，对外充当统一入口
 */
class JsBridgeHandlerRegistry(
    private val webViewProvider: () -> Any?,
    private val contextProvider: () -> Context?
) : WebViewJsBridge.JsBridgeReceiver {

    private val handlers: MutableMap<String, WebViewJsBridge.JsHandler> = mutableMapOf()

    /**
     * 注册 Handler
     */
    fun registerHandler(name: String, handler: WebViewJsBridge.JsHandler) {
        handlers[name] = handler
        Timber.d("[JsBridgeHandlerRegistry] 注册 JS Handler: $name")
    }

    /**
     * 取消注册 Handler
     */
    fun unregisterHandler(name: String) {
        handlers.remove(name)
        Timber.d("[JsBridgeHandlerRegistry] 取消注册 JS Handler: $name")
    }

    /**
     * 清空所有 Handler
     */
    fun clearHandlers() {
        handlers.clear()
        Timber.d("[JsBridgeHandlerRegistry] 已清空所有 JS Handler")
    }

    override fun onJsCall(handler: String, data: String?, callbackId: String?) {
        val h = handlers[handler]
        if (h == null) {
            Timber.w("[JsBridgeHandlerRegistry] 未找到 JS Handler: $handler")
            return
        }

        val sender = WebViewJsBridge.JsCallSender(
            webView = webViewProvider.invoke(),
            context = contextProvider.invoke()
        )

        try {
            h.handle(data, callbackId, sender)
        } catch (e: Exception) {
            Timber.e(e, "[JsBridgeHandlerRegistry] 处理 JS 调用失败: $handler")
        }
    }
}



