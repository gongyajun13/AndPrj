package com.jun.core.ui.webview

import android.content.Context
import android.os.Build
import com.tencent.smtt.sdk.QbSdk
import com.tencent.smtt.sdk.TbsListener
import timber.log.Timber

/**
 * X5 WebView 辅助类
 * 用于初始化和管理 X5 内核
 */
object X5WebViewHelper {
    
    private var isInitialized = false
    private var isX5Available = false
    
    /**
     * 是否已初始化
     */
    fun isInitialized(): Boolean = isInitialized
    
    /**
     * X5 内核是否可用
     */
    fun isX5Available(): Boolean = isX5Available
    
    /**
     * 检查 X5 SDK 是否可用（仅检查类是否存在）
     */
    fun isX5SdkAvailable(): Boolean {
        return try {
            Class.forName("com.tencent.smtt.sdk.QbSdk")
            true
        } catch (e: ClassNotFoundException) {
            false
        }
    }
    
    /**
     * 初始化 X5 内核
     * @param context Application Context
     * @param onInitCallback 初始化完成回调（可选）
     */
    fun init(context: Context, onInitCallback: ((Boolean) -> Unit)? = null) {
        if (isInitialized) {
            Timber.d("[X5WebViewHelper] X5 内核已初始化")
            onInitCallback?.invoke(isX5Available)
            return
        }
        
        Timber.d("[X5WebViewHelper] 开始初始化 X5 内核...")
        
        try {
            // 设置 TBS 监听器
            QbSdk.setTbsListener(object : TbsListener {
                override fun onDownloadFinish(i: Int) {
                    Timber.d("[X5WebViewHelper] X5 内核下载完成: $i")
                }
                
                override fun onInstallFinish(i: Int) {
                    Timber.d("[X5WebViewHelper] X5 内核安装完成: $i")
                    isX5Available = (i == 0) // 0 表示安装成功
                    isInitialized = true
                    onInitCallback?.invoke(isX5Available)
                }
                
                override fun onDownloadProgress(i: Int) {
                    Timber.d("[X5WebViewHelper] X5 内核下载进度: $i%")
                }
            })
            
            // 预初始化 X5 内核（在后台线程进行，不阻塞主线程）
            QbSdk.initX5Environment(context, object : QbSdk.PreInitCallback {
                override fun onCoreInitFinished() {
                    // 核心初始化完成
                }
                
                override fun onViewInitFinished(p0: Boolean) {
                    isX5Available = p0
                    isInitialized = true
                    Timber.d("[X5WebViewHelper] X5 内核初始化完成: $p0")
                    onInitCallback?.invoke(p0)
                }
            })
            
            // 设置是否允许在不安全的上下文（HTTP）中运行
            // 注意：某些版本的 X5 SDK 可能没有此方法
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    val allowMethod = QbSdk::class.java.getMethod("allowThirdAppDownload", Boolean::class.java)
                    allowMethod.invoke(null, true)
                }
            } catch (e: Exception) {
                Timber.d("[X5WebViewHelper] allowThirdAppDownload 方法不存在，跳过")
            }
        } catch (e: Exception) {
            Timber.e(e, "[X5WebViewHelper] 初始化 X5 内核失败")
            isInitialized = true
            isX5Available = false
            onInitCallback?.invoke(false)
        }
    }
    
    /**
     * 获取 X5 内核版本信息
     */
    fun getX5Version(context: Context? = null): String {
        return try {
            val version = QbSdk.getTbsVersion(context)
            version.toString()
        } catch (e: Exception) {
            Timber.e(e, "[X5WebViewHelper] 获取 X5 版本失败")
            "未知"
        }
    }
    
    /**
     * 检查 X5 内核是否已加载
     */
    fun checkX5Available(context: Context? = null): Boolean {
        return try {
            QbSdk.canLoadX5(context)
        } catch (e: Exception) {
            Timber.e(e, "[X5WebViewHelper] 检查 X5 可用性失败")
            false
        }
    }
}
