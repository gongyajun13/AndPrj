package com.jun.core.ui.webview

import android.content.Context
import android.os.Build
import android.webkit.CookieManager
import android.webkit.WebStorage
import android.webkit.WebView
import com.jun.core.ui.extension.clearCacheSafely
import timber.log.Timber
import java.io.File
import java.text.DecimalFormat
import kotlin.math.log10
import kotlin.math.pow

/**
 * WebView 缓存/数据 工具类
 *
 * 功能：
 * - 计算 WebView 相关缓存占用大小（应用级）
 * - 清理 WebView 缓存、数据库、LocalStorage 等数据
 * - 可选清理 Cookie、ServiceWorker 缓存
 *
 * 注意：
 * - 这里的“缓存大小”是基于目录遍历的估算值，仅供展示使用
 * - 实际占用会随系统实现略有差异
 */
object WebViewCacheUtils {

    /**
     * 获取 WebView 相关缓存大小（字节）
     *
     * 包含大部分常见目录：
     * - app_webview
     * - webview
     * - cacheDir 下的 WebView 缓存
     */
    fun getWebViewCacheSizeBytes(context: Context): Long {
        val appContext = context.applicationContext
        val dirs = mutableListOf<File>()

        // 应用缓存目录
        appContext.cacheDir?.let { dirs.add(it) }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            appContext.codeCacheDir?.let { dirs.add(it) }
        }

        // WebView 相关目录（大部分 ROM 通用）
        appContext.getDir("webview", Context.MODE_PRIVATE)?.let { dirs.add(it) }

        // /data/data/<pkg>/app_webview
        val appWebViewDir = File(appContext.filesDir.parentFile, "app_webview")
        dirs.add(appWebViewDir)

        // 去重
        val uniqueDirs = dirs.filter { it.exists() }.distinctBy { it.absolutePath }

        var total = 0L
        uniqueDirs.forEach { dir ->
            total += getDirSizeSafe(dir)
        }

        Timber.d("[WebViewCacheUtils] 计算到的 WebView 缓存大小: $total bytes")
        return total
    }

    /**
     * 获取格式化后的 WebView 缓存大小（如 12.3 MB）
     */
    fun getWebViewCacheSizeFormatted(context: Context): String {
        val bytes = getWebViewCacheSizeBytes(context)
        return formatSize(bytes)
    }

    /**
     * 清理应用内所有 WebView 缓存/数据
     *
     * @param context 上下文
     * @param includeDiskFiles 是否清理磁盘缓存（默认 true）
     * @param includeCookies 是否清理 Cookie（默认 true）
     * @param includeStorage 是否清理 WebStorage（LocalStorage / IndexedDB 等）（默认 true）
     */
    fun clearAllWebViewData(
        context: Context,
        includeDiskFiles: Boolean = true,
        includeCookies: Boolean = true,
        includeStorage: Boolean = true,
    ) {
        val appContext = context.applicationContext

        // 1. 清理磁盘缓存目录
        if (includeDiskFiles) {
            try {
                val dirs = mutableListOf<File>()
                appContext.cacheDir?.let { dirs.add(it) }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    appContext.codeCacheDir?.let { dirs.add(it) }
                }
                appContext.getDir("webview", Context.MODE_PRIVATE)?.let { dirs.add(it) }
                val appWebViewDir = File(appContext.filesDir.parentFile, "app_webview")
                dirs.add(appWebViewDir)

                dirs
                    .filter { it.exists() }
                    .distinctBy { it.absolutePath }
                    .forEach { dir ->
                        deleteDirSafe(dir)
                    }
            } catch (e: Exception) {
                Timber.e(e, "[WebViewCacheUtils] 清理 WebView 磁盘缓存失败")
            }
        }

        // 2. 清理 WebStorage（HTML5 存储）
        if (includeStorage) {
            try {
                WebStorage.getInstance().deleteAllData()
                Timber.d("[WebViewCacheUtils] WebStorage 数据已清理")
            } catch (e: Exception) {
                Timber.e(e, "[WebViewCacheUtils] 清理 WebStorage 失败")
            }
        }

        // 3. 清理 Cookie
        if (includeCookies) {
            try {
                val cookieManager = CookieManager.getInstance()
                cookieManager.removeAllCookies(null)
                cookieManager.flush()
                Timber.d("[WebViewCacheUtils] Cookie 已清理")
            } catch (e: Exception) {
                Timber.e(e, "[WebViewCacheUtils] 清理 Cookie 失败")
            }
        }

        // 4. 清理 ServiceWorker 缓存（如果 ROM/WebView 支持）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                val webView = WebView(appContext)
                webView.clearCacheSafely(includeDiskFiles)
                webView.destroy()
                Timber.d("[WebViewCacheUtils] 通过临时 WebView 触发缓存清理")
            } catch (e: Exception) {
                Timber.e(e, "[WebViewCacheUtils] 通过临时 WebView 清理缓存失败")
            }
        }
    }

    /**
     * 对单个 WebView 实例执行缓存清理（内存 + 磁盘）
     *
     * - 对系统 WebView：直接调用扩展函数
     * - 对 X5 WebView：调用 X5 的 clearCache(true)
     */
    fun clearWebViewInstanceCache(webView: Any?, includeDiskFiles: Boolean = true) {
        when (webView) {
            is WebView -> {
                webView.clearCacheSafely(includeDiskFiles)
            }
            is SystemWebViewWrapper -> {
                // SystemWebViewWrapper 继承自 WebView
                webView.clearCacheSafely(includeDiskFiles)
            }
            is X5WebView -> {
                try {
                    webView.clearCache(includeDiskFiles)
                } catch (e: Exception) {
                    Timber.e(e, "[WebViewCacheUtils] 清理 X5 WebView 缓存失败")
                }
            }
        }
    }

    // region 内部工具方法

    private fun getDirSizeSafe(file: File): Long {
        return try {
            getDirSize(file)
        } catch (e: Exception) {
            Timber.e(e, "[WebViewCacheUtils] 计算目录大小失败: ${file.absolutePath}")
            0L
        }
    }

    private fun getDirSize(file: File): Long {
        if (!file.exists()) return 0L
        if (file.isFile) return file.length()

        var total = 0L
        file.listFiles()?.forEach { child ->
            total += getDirSize(child)
        }
        return total
    }

    private fun deleteDirSafe(file: File) {
        try {
            if (!file.exists()) return
            if (file.isFile) {
                if (file.delete()) {
                    Timber.v("[WebViewCacheUtils] 删除文件: ${file.absolutePath}")
                }
            } else {
                file.listFiles()?.forEach { child ->
                    deleteDirSafe(child)
                }
                if (file.delete()) {
                    Timber.v("[WebViewCacheUtils] 删除目录: ${file.absolutePath}")
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "[WebViewCacheUtils] 删除目录失败: ${file.absolutePath}")
        }
    }

    /**
     * 格式化文件大小（B / KB / MB / GB）
     */
    fun formatSize(sizeBytes: Long): String {
        if (sizeBytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (log10(sizeBytes.toDouble()) / log10(1024.0)).toInt()
        val df = DecimalFormat("#,##0.##")
        return df.format(sizeBytes / 1024.0.pow(digitGroups.toDouble())) + " " + units[digitGroups]
    }

    // endregion
}




