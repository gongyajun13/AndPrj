package com.jun.core.ui.webview.scheme

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import timber.log.Timber

/**
 * 邮件协议处理器
 * 处理 mailto: 协议
 * 
 * 注意：无论是否成功跳转，都会返回 true，避免 WebView 继续加载并显示错误页面
 */
class MailToSchemeHandler : CustomSchemeHandler.SchemeHandler {
    
    override fun canHandle(url: String): Boolean {
        return url.startsWith("mailto:", ignoreCase = true)
    }
    
    override fun handle(url: String, context: Context): Boolean {
        try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse(url)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                Timber.d("[MailToSchemeHandler] 已打开邮件客户端: $url")
                return true
            } else {
                Timber.w("[MailToSchemeHandler] 未找到邮件客户端: $url")
                // 未找到邮件客户端，显示 Toast 提示，但依然返回 true，避免 WebView 显示错误页面
                Toast.makeText(context, "未安装邮件应用，无法打开此链接", Toast.LENGTH_SHORT).show()
                return true
            }
        } catch (e: Exception) {
            Timber.e(e, "[MailToSchemeHandler] 处理失败: $url")
            // 即使处理失败，也返回 true，避免 WebView 显示错误页面
            Toast.makeText(context, "无法打开此链接", Toast.LENGTH_SHORT).show()
            return true
        }
    }
}

