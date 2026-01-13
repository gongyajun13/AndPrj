package com.jun.andprj

import android.app.Application
import com.jun.core.common.util.notify.ToastUiNotifier
import com.jun.core.common.util.notify.UiNotifierManager
import com.jun.core.network.loading.LoadingManager
import com.jun.core.ui.base.utils.ActivityManager
import com.jun.core.ui.base.SimpleLoadingDialog
import com.jun.core.ui.webview.X5WebViewHelper
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // 初始化 Timber（仅在 Debug 模式下）
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        UiNotifierManager.delegate = ToastUiNotifier()
        
        // 初始化 ActivityManager（用于统一管理 Activity）
        ActivityManager.init(this)
        
        // 初始化 LoadingManager（用于网络请求时自动显示 loading 对话框）
        LoadingManager.init(this)
        // 设置 LoadingDialog 工厂（使用 SimpleLoadingDialog，支持自定义配置）
        LoadingManager.dialogFactory = { config ->
            object : com.jun.core.common.ui.LoadingDialog {
                private val dialog = SimpleLoadingDialog(config ?: com.jun.core.common.ui.LoadingDialogConfig.DEFAULT)
                override fun showSafely(fragmentManager: androidx.fragment.app.FragmentManager, tag: String?) {
                    dialog.showSafely(fragmentManager, tag)
                }
                override fun dismissSafely() {
                    dialog.dismissSafely()
                }
                override fun updateMessage(message: String) {
                    dialog.updateMessage(message)
                }
            }
        }
        
        // 初始化 X5 WebView 内核
        X5WebViewHelper.init(this) { success ->
            if (success) {
                Timber.d("[MyApplication] X5 内核初始化成功，版本: ${X5WebViewHelper.getX5Version(this)}")
            } else {
                Timber.w("[MyApplication] X5 内核初始化失败，将使用系统 WebView")
            }
        }
        
        // 设置全局异常处理器，避免 MIUI 系统尝试写入异常日志时失败
        Thread.setDefaultUncaughtExceptionHandler { thread, exception ->
            // 记录异常到 Timber
            Timber.e(exception, "未捕获的异常 [线程: ${thread.name}]")
            
            // 调用系统默认的异常处理器（如果需要）
            // 注意：这会触发 MIUI 的异常日志写入，可能会产生 mi_exception_log 错误
            // 如果不想看到这个错误，可以注释掉下面这行
            val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
            defaultHandler?.uncaughtException(thread, exception)
        }
    }
}









