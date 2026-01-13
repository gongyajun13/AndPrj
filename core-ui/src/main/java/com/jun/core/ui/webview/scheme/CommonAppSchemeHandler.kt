package com.jun.core.ui.webview.scheme

import android.content.Context
import android.content.Intent
import android.widget.Toast
import timber.log.Timber

/**
 * 常见 App 协议处理器
 * 处理常见的第三方 App 协议，如 weixin://, alipays://, mqq:// 等
 * 
 * 注意：无论是否成功跳转，都会返回 true，避免 WebView 继续加载并显示错误页面
 */
class CommonAppSchemeHandler : CustomSchemeHandler.SchemeHandler {
    
    // 常见的 App 协议列表
    private val commonSchemes = listOf(
        "weixin://",
        "alipays://",
        "alipay://",
        "mqq://",
        "mqqapi://",
        "baiduboxapp://",
        "baiduboxlite://",
        "sinaweibo://",
        "taobao://",
        "tmall://",
        "jd://",
        "youku://",
        "iqiyi://",
        "tencentvideo://",
        "qqmusic://",
        "netease://",
        "dianping://",
        "meituan://",
        "eleme://",
        "kuaishou://",
        "douyin://",
        "tiktok://",
        "market://" // Google Play 商店协议
    )
    
    override fun canHandle(url: String): Boolean {
        return commonSchemes.any { scheme ->
            url.startsWith(scheme, ignoreCase = true)
        }
    }
    
    override fun handle(url: String, context: Context): Boolean {
        try {
            // 特殊处理 market:// 协议（应用商店）
            if (url.startsWith("market://", ignoreCase = true)) {
                return handleMarketScheme(url, context)
            }
            
            // 尝试解析 Intent URI
            val intent = try {
                Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
            } catch (e: Exception) {
                // 如果解析失败，尝试直接创建 Intent
                Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url))
            }
            
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            
            // 检查是否有 App 可以处理该 Intent
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                Timber.d("[CommonAppSchemeHandler] 已打开 App: $url")
                // 成功跳转，返回 true 表示已处理，WebView 不会继续加载
                return true
            } else {
                Timber.w("[CommonAppSchemeHandler] 未找到对应的 App: $url")
                // 未安装对应 App，显示 Toast 提示，但依然返回 true，避免 WebView 显示错误页面
                Toast.makeText(context, "未安装对应的应用，无法打开此链接", Toast.LENGTH_SHORT).show()
                return true
            }
        } catch (e: Exception) {
            Timber.e(e, "[CommonAppSchemeHandler] 处理失败: $url")
            // 即使处理失败，也返回 true，避免 WebView 显示错误页面
            Toast.makeText(context, "无法打开此链接", Toast.LENGTH_SHORT).show()
            return true
        }
    }
    
    /**
     * 处理 market:// 协议（应用商店）
     * 如果无法打开 Google Play，尝试降级到网页版链接
     */
    private fun handleMarketScheme(url: String, context: Context): Boolean {
        try {
            val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            
            // 检查是否有应用商店可以处理（Google Play、华为应用市场、小米应用商店等）
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                Timber.d("[CommonAppSchemeHandler] 已打开应用商店: $url")
                return true
            } else {
                // 如果没有应用商店，尝试转换为网页版链接
                Timber.w("[CommonAppSchemeHandler] 未找到应用商店，尝试转换为网页版链接: $url")
                
                // 将 market://details?id=包名 转换为 https://play.google.com/store/apps/details?id=包名
                val webUrl = url.replace("market://", "https://play.google.com/store/apps/", ignoreCase = true)
                
                try {
                    val webIntent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(webUrl)).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    
                    if (webIntent.resolveActivity(context.packageManager) != null) {
                        context.startActivity(webIntent)
                        Timber.d("[CommonAppSchemeHandler] 已打开网页版应用商店: $webUrl")
                        return true
                    }
                } catch (e: Exception) {
                    Timber.e(e, "[CommonAppSchemeHandler] 打开网页版应用商店失败")
                }
                
                // 如果都无法打开，显示提示
                Toast.makeText(context, "未找到应用商店，无法打开此链接", Toast.LENGTH_SHORT).show()

                return true
            }
        } catch (e: Exception) {
            Timber.e(e, "[CommonAppSchemeHandler] 处理 market:// 协议失败: $url")
            Toast.makeText(context, "无法打开应用商店链接", Toast.LENGTH_SHORT).show()
            return true
        }
    }
}

