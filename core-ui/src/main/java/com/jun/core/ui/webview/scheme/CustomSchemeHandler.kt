package com.jun.core.ui.webview.scheme

import android.content.Context
import android.content.Intent
import android.net.Uri
import timber.log.Timber

/**
 * 自定义协议处理器
 * 用于处理 WebView 中的自定义 URL Scheme（如 tel:, mailto:, weixin:// 等）
 * 
 * 使用方式：
 * ```kotlin
 * // 注册自定义处理器
 * CustomSchemeHandler.registerHandler(object : SchemeHandler {
 *     override fun canHandle(url: String) = url.startsWith("myapp://")
 *     override fun handle(url: String, context: Context): Boolean {
 *         // 处理逻辑
 *         return true
 *     }
 * })
 * ```
 */
object CustomSchemeHandler {
    
    /**
     * 协议处理器接口
     */
    interface SchemeHandler {
        /**
         * 判断是否可以处理该 URL
         */
        fun canHandle(url: String): Boolean
        
        /**
         * 处理 URL
         * @return true 表示已处理，false 表示未处理
         */
        fun handle(url: String, context: Context): Boolean
    }
    
    private val handlers = mutableListOf<SchemeHandler>()
    
    init {
        // 注册内置的基础处理器
        registerHandler(TelSchemeHandler())
        registerHandler(MailToSchemeHandler())
        registerHandler(CommonAppSchemeHandler())
    }
    
    /**
     * 注册协议处理器
     * 后注册的处理器优先级更高（会先被调用）
     */
    fun registerHandler(handler: SchemeHandler) {
        handlers.add(0, handler) // 添加到列表开头，提高优先级
        Timber.d("[CustomSchemeHandler] 注册协议处理器: ${handler::class.simpleName}")
    }
    
    /**
     * 移除协议处理器
     */
    fun unregisterHandler(handler: SchemeHandler) {
        handlers.remove(handler)
        Timber.d("[CustomSchemeHandler] 移除协议处理器: ${handler::class.simpleName}")
    }
    
    /**
     * 处理自定义协议
     * @param url 要处理的 URL
     * @param context Context
     * @return true 表示已处理（无论成功与否），false 表示未找到处理器
     * 
     * 注意：所有处理器都应该返回 true，表示"已处理"（无论是否成功跳转），
     * 这样可以避免 WebView 继续加载并显示错误页面。
     * 如果无法跳转，处理器内部应该显示 Toast 提示用户。
     */
    fun handle(url: String, context: Context): Boolean {
        if (url.isBlank()) {
            return false
        }
        
        // 遍历所有处理器，找到第一个能处理的
        for (handler in handlers) {
            if (handler.canHandle(url)) {
                try {
                    val handled = handler.handle(url, context)
                    // 处理器应该总是返回 true（表示已处理，不继续加载）
                    // 如果返回 false，说明处理器实现有问题
                    if (handled) {
                        Timber.d("[CustomSchemeHandler] 协议已处理: $url (处理器: ${handler::class.simpleName})")
                        return true
                    } else {
                        Timber.w("[CustomSchemeHandler] 处理器返回 false，可能实现有误: $url (处理器: ${handler::class.simpleName})")
                        // 即使处理器返回 false，我们也返回 true，避免 WebView 显示错误页面
                        return true
                    }
                } catch (e: Exception) {
                    Timber.e(e, "[CustomSchemeHandler] 处理协议失败: $url (处理器: ${handler::class.simpleName})")
                    // 即使处理异常，也返回 true，避免 WebView 显示错误页面
                    return true
                }
            }
        }
        
        Timber.w("[CustomSchemeHandler] 未找到处理器: $url")
        return false
    }
    
    /**
     * 清空所有自定义处理器（保留内置处理器）
     */
    fun clearCustomHandlers() {
        val builtInHandlers = listOf(
            TelSchemeHandler::class,
            MailToSchemeHandler::class,
            CommonAppSchemeHandler::class
        )
        handlers.removeAll { handler ->
            !builtInHandlers.contains(handler::class)
        }
        Timber.d("[CustomSchemeHandler] 已清空自定义处理器")
    }
}

