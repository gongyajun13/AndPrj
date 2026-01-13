package com.jun.core.ui.webview.download

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.webkit.DownloadListener
import android.webkit.MimeTypeMap
import com.jun.core.common.extension.formatFileSize
import com.jun.core.ui.extension.showConfirmDialog
import com.tencent.smtt.sdk.DownloadListener as X5DownloadListener
import java.net.URLDecoder

/**
 * WebView 下载拦截器
 * 只负责拦截下载请求并显示确认弹窗，不直接执行下载
 * 用户确认后，将下载任务交给 DownloadManagerActivity 处理
 * 
 * 设计目的：
 * - 功能分离：WebView 只负责拦截和询问，DownloadManagerActivity 负责实际下载
 * - 避免多个地方管理下载状态，统一由 DownloadManagerActivity 管理
 * - 解决 WebView 和 DownloadManagerActivity 共用下载导致的状态同步问题
 */
class WebViewDownloadInterceptor(
    private val context: Context,
    private val onDownloadConfirmed: (DownloadRequest) -> Unit
) {
    
    /**
     * 下载请求数据类
     */
    data class DownloadRequest(
        val url: String,
        val userAgent: String?,
        val contentDisposition: String?,
        val mimeType: String?,
        val contentLength: Long
    )
    
    /**
     * 从 URL 中提取文件名
     */
    private fun extractFileName(url: String, contentDisposition: String?, mimeType: String?): String {
        // 尝试从 Content-Disposition 中提取文件名
        contentDisposition?.let {
            val fileNameRegex = Regex("filename[*]?=(?:UTF-8['\"]?)?([^;\\r\\n]+)", RegexOption.IGNORE_CASE)
            fileNameRegex.find(it)?.groupValues?.get(1)?.trim()?.let { name ->
                return URLDecoder.decode(name.trim('"', '\''), "UTF-8")
            }
        }
        
        // 从 URL 中提取文件名
        try {
            val path = Uri.parse(url).path
            if (path != null) {
                val fileName = path.substringAfterLast('/')
                if (fileName.isNotEmpty() && fileName.contains('.')) {
                    return fileName
                }
            }
        } catch (e: Exception) {
            // 从 URL 提取文件名失败，使用默认文件名
        }
        
        // 根据 MIME 类型生成默认文件名
        val extension = mimeType?.let { MimeTypeMap.getSingleton().getExtensionFromMimeType(it) } ?: "bin"
        return "download_${System.currentTimeMillis()}.$extension"
    }
    
    /**
     * 处理下载请求
     * 显示确认弹窗，用户确认后调用 onDownloadConfirmed 回调
     */
    private fun handleDownloadRequest(
        url: String,
        userAgent: String?,
        contentDisposition: String?,
        mimeType: String?,
        contentLength: Long
    ) {
        // 提取文件名
        val fileName = extractFileName(url, contentDisposition, mimeType)
        
        // 构建确认消息
        val sizeText = if (contentLength > 0) {
            contentLength.formatFileSize()
        } else {
            "未知大小"
        }
        val message = "是否下载文件？\n\n文件名：$fileName\n大小：$sizeText"
        
        // 显示确认弹窗
        context.showConfirmDialog(
            title = "下载确认",
            message = message,
            confirmText = "下载",
            cancelText = "取消",
            onConfirm = {
                // 用户确认后，将下载任务传递给回调
                onDownloadConfirmed(
                    DownloadRequest(
                        url = url,
                        userAgent = userAgent,
                        contentDisposition = contentDisposition,
                        mimeType = mimeType,
                        contentLength = contentLength
                    )
                )
            },
            onCancel = {
                // 用户取消下载
            }
        )
    }
    
    /**
     * 创建系统 WebView 的 DownloadListener
     */
    fun createSystemDownloadListener(): DownloadListener {
        return DownloadListener { url, userAgent, contentDisposition, mimeType, contentLength ->
            handleDownloadRequest(url, userAgent, contentDisposition, mimeType, contentLength)
        }
    }
    
    /**
     * 创建 X5 WebView 的 DownloadListener
     */
    fun createX5DownloadListener(): X5DownloadListener {
        return object : X5DownloadListener {
            override fun onDownloadStart(
                url: String,
                userAgent: String,
                contentDisposition: String,
                mimetype: String,
                contentLength: Long
            ) {
                handleDownloadRequest(url, userAgent, contentDisposition, mimetype, contentLength)
            }
        }
    }
}

