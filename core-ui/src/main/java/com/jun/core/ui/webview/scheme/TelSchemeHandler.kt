package com.jun.core.ui.webview.scheme

import android.content.Context
import android.content.Intent
import android.net.Uri
import timber.log.Timber

/**
 * 电话协议处理器
 * 处理 tel: 和 tel:// 协议
 */
class TelSchemeHandler : CustomSchemeHandler.SchemeHandler {
    
    override fun canHandle(url: String): Boolean {
        return url.startsWith("tel:", ignoreCase = true) || 
               url.startsWith("tel://", ignoreCase = true)
    }
    
    override fun handle(url: String, context: Context): Boolean {
        try {
            val phoneNumber = url.removePrefix("tel:").removePrefix("tel://")
            if (phoneNumber.isBlank()) {
                Timber.w("[TelSchemeHandler] 电话号码为空: $url")
                // 即使电话号码为空，也返回 true，避免 WebView 继续加载
                return true
            }
            
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:$phoneNumber")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            
            // 检查是否有应用可以处理拨号 Intent
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                Timber.d("[TelSchemeHandler] 已打开拨号界面: $phoneNumber")
                return true
            } else {
                Timber.w("[TelSchemeHandler] 未找到拨号应用: $url")
                // 未找到拨号应用，返回 true，避免 WebView 继续加载
                return true
            }
        } catch (e: Exception) {
            Timber.e(e, "[TelSchemeHandler] 处理失败: $url")
            // 即使处理失败，也返回 true，避免 WebView 显示错误页面
            return true
        }
    }
}

