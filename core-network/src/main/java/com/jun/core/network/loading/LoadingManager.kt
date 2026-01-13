package com.jun.core.network.loading

import com.jun.core.common.ui.LoadingDialog
import com.jun.core.common.ui.LoadingDialogConfig

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.*
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

/**
 * 全局 Loading 管理器
 * 用于在网络请求时自动显示和隐藏 loading 对话框
 * 
 * 使用方式：
 * 1. 在 Application 中初始化：LoadingManager.init(application)
 * 2. 在网络请求配置中启用 loading：requestConfig { showLoading("加载中...") }
 * 
 * 特性：
 * - 自动追踪最顶层的 Activity
 * - 支持多个并发请求（使用计数器）
 * - 自动管理 Dialog 的生命周期
 */
object LoadingManager {
    
    /**
     * LoadingDialog 工厂，由应用层提供实现
     * 支持传入配置来自定义样式
     * 
     * 使用示例：
     * ```kotlin
     * LoadingManager.dialogFactory = { config ->
     *     // 使用配置创建自定义 Dialog
     *     CustomLoadingDialog(config ?: LoadingDialogConfig.DEFAULT)
     * }
     * ```
     */
    @Volatile
    var dialogFactory: ((config: LoadingDialogConfig?) -> LoadingDialog)? = null
    
    private var application: Application? = null
    private val activityStack = CopyOnWriteArrayList<Activity>()
    private val loadingCounters = ConcurrentHashMap<String, AtomicInteger>()
    private val loadingDialogs = ConcurrentHashMap<String, LoadingDialog>()
    private val loadingMessages = ConcurrentHashMap<String, String>()
    private val loadingTimeouts = ConcurrentHashMap<String, Job>()
    private val loadingScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    /**
     * 初始化 LoadingManager
     * 需要在 Application.onCreate() 中调用
     */
    fun init(application: Application) {
        this.application = application
        application.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                if (!activityStack.contains(activity)) {
                    activityStack.add(activity)
                }
            }
            
            override fun onActivityStarted(activity: Activity) {
                // 确保 Activity 在栈顶
                activityStack.remove(activity)
                activityStack.add(activity)
            }
            
            override fun onActivityResumed(activity: Activity) {
                // 确保 Activity 在栈顶
                activityStack.remove(activity)
                activityStack.add(activity)
            }
            
            override fun onActivityPaused(activity: Activity) {}
            
            override fun onActivityStopped(activity: Activity) {}
            
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            
            override fun onActivityDestroyed(activity: Activity) {
                activityStack.remove(activity)
                // 清理该 Activity 的 loading 计数器
                val key = activity.javaClass.name
                loadingCounters.remove(key)
                loadingDialogs.remove(key)?.dismissSafely()
            }
        })
    }
    
    /**
     * 显示 Loading 对话框
     * @param message Loading 提示消息
     * @param tag 用于标识不同的 loading（可选，默认使用 Activity 类名）
     * @param timeoutMillis Loading 超时时间（毫秒），0 表示不超时，默认 0
     * @param config Loading 样式配置，如果为 null 则使用默认配置
     * @return 是否成功显示
     */
    fun showLoading(
        message: String = "加载中...",
        tag: String? = null,
        timeoutMillis: Long = 0,
        config: LoadingDialogConfig? = null
    ): Boolean {
        val topActivity = getTopActivity()
        if (topActivity == null) {
            Timber.w("[LoadingManager] 无法获取顶层 Activity，无法显示 Loading")
            return false
        }
        
        val activity = topActivity as? FragmentActivity
        if (activity == null) {
            Timber.w("[LoadingManager] 顶层 Activity 不是 FragmentActivity，无法显示 Loading: ${topActivity.javaClass.name}")
            return false
        }
        
        val key = tag ?: activity.javaClass.name
        
        // 检查 dialogFactory 是否已设置
        val factory = dialogFactory
        if (factory == null) {
            Timber.w("[LoadingManager] dialogFactory 未设置，无法显示 Loading。请在 Application.onCreate() 中设置 LoadingManager.dialogFactory")
            return false
        }
        
        // 保存消息
        loadingMessages[key] = message
        
        // 增加计数器
        val counter = loadingCounters.getOrPut(key) { AtomicInteger(0) }
        val count = counter.incrementAndGet()
        
        // 如果是第一个请求，显示 Dialog
        if (count == 1) {
            try {
                val dialog = factory(config)
                loadingDialogs[key] = dialog
                dialog.showSafely(activity.supportFragmentManager, "LoadingDialog_$key")
                Timber.d("[LoadingManager] 显示 Loading 对话框 [key: $key, message: $message]")
                
                // 设置超时（如果配置了）
                if (timeoutMillis > 0) {
                    val timeoutJob = loadingScope.launch {
                        delay(timeoutMillis)
                        Timber.w("[LoadingManager] Loading 超时，自动关闭 [key: $key, timeout: ${timeoutMillis}ms]")
                        hideLoading(tag = tag)
                    }
                    loadingTimeouts[key] = timeoutJob
                }
                
                return true
            } catch (e: Exception) {
                // 显示失败，减少计数器
                Timber.e(e, "[LoadingManager] 显示 Loading 对话框失败 [key: $key]")
                counter.decrementAndGet()
                loadingDialogs.remove(key)
                loadingMessages.remove(key)
                return false
            }
        }
        
        Timber.d("[LoadingManager] Loading 计数器增加 [key: $key, count: $count]")
        return true
    }
    
    /**
     * 更新 Loading 消息
     * @param message 新的提示消息
     * @param tag 用于标识不同的 loading（可选，默认使用 Activity 类名）
     * @return 是否成功更新
     */
    fun updateLoadingMessage(message: String, tag: String? = null): Boolean {
        val topActivity = getTopActivity()
        if (topActivity == null) {
            return false
        }
        
        val activity = topActivity as? FragmentActivity
        if (activity == null) {
            return false
        }
        
        val key = tag ?: activity.javaClass.name
        val dialog = loadingDialogs[key]
        
        if (dialog == null) {
            Timber.w("[LoadingManager] 未找到 Loading 对话框 [key: $key]，无法更新消息")
            return false
        }
        
        // 更新消息
        loadingMessages[key] = message
        dialog.updateMessage(message)
        Timber.d("[LoadingManager] 更新 Loading 消息 [key: $key, message: $message]")
        return true
    }
    
    /**
     * 隐藏 Loading 对话框
     * @param tag 用于标识不同的 loading（可选，默认使用 Activity 类名）
     * @return 是否成功隐藏
     */
    fun hideLoading(tag: String? = null): Boolean {
        val topActivity = getTopActivity()
        if (topActivity == null) {
            // 如果没有顶层 Activity，尝试清理所有 loading
            Timber.w("[LoadingManager] 无法获取顶层 Activity，强制清理所有 Loading")
            hideAllLoading()
            return false
        }
        
        val activity = topActivity as? FragmentActivity
        if (activity == null) {
            Timber.w("[LoadingManager] 顶层 Activity 不是 FragmentActivity: ${topActivity.javaClass.name}")
            return false
        }
        
        val key = tag ?: activity.javaClass.name
        val counter = loadingCounters[key]
        
        if (counter == null) {
            Timber.w("[LoadingManager] 未找到 Loading 计数器 [key: $key]，可能已经关闭")
            return false
        }
        
        val count = counter.decrementAndGet()
        Timber.d("[LoadingManager] Loading 计数器减少 [key: $key, count: $count]")
        
        // 如果计数器为 0，隐藏 Dialog
        if (count == 0) {
            // 取消超时任务
            loadingTimeouts.remove(key)?.cancel()
            
            val dialog = loadingDialogs.remove(key)
            loadingCounters.remove(key)
            loadingMessages.remove(key)
            
            if (dialog != null) {
                Timber.d("[LoadingManager] 准备隐藏 Loading 对话框 [key: $key]")
                dialog.dismissSafely()
                Timber.d("[LoadingManager] 已调用 dismissSafely [key: $key]")
            } else {
                Timber.w("[LoadingManager] Loading 对话框不存在 [key: $key]，可能已经关闭")
            }
            return true
        }
        
        // 如果计数器小于 0，说明有异常（可能被多次调用），强制关闭并重置
        if (count < 0) {
            Timber.w("[LoadingManager] Loading 计数器异常 [key: $key, count: $count]，强制关闭并重置")
            
            // 取消超时任务
            loadingTimeouts.remove(key)?.cancel()
            
            val dialog = loadingDialogs.remove(key)
            loadingCounters.remove(key)
            loadingMessages.remove(key)
            
            if (dialog != null) {
                dialog.dismissSafely()
                Timber.d("[LoadingManager] 已强制关闭 Loading 对话框 [key: $key]")
            }
            counter.set(0)
            return true
        }
        
        // 计数器大于 0，说明还有其他请求在进行，不关闭 dialog
        Timber.d("[LoadingManager] Loading 对话框保持显示 [key: $key, count: $count]，还有其他请求在进行")
        return true
    }
    
    /**
     * 强制隐藏所有 Loading 对话框（用于异常情况）
     */
    fun hideAllLoading() {
        // 取消所有超时任务
        loadingTimeouts.values.forEach { it.cancel() }
        loadingTimeouts.clear()
        
        loadingDialogs.values.forEach { it.dismissSafely() }
        loadingDialogs.clear()
        loadingCounters.clear()
        loadingMessages.clear()
    }
    
    /**
     * 获取当前 Loading 消息
     * @param tag 用于标识不同的 loading（可选，默认使用 Activity 类名）
     * @return Loading 消息，如果不存在则返回 null
     */
    fun getLoadingMessage(tag: String? = null): String? {
        val activity = getTopActivity() ?: return null
        val key = tag ?: activity.javaClass.name
        return loadingMessages[key]
    }
    
    /**
     * 清理资源（在 Application 销毁时调用）
     */
    fun cleanup() {
        hideAllLoading()
        loadingScope.cancel()
    }
    
    /**
     * 获取最顶层的 Activity
     */
    private fun getTopActivity(): Activity? {
        return activityStack.lastOrNull()
    }
    
    /**
     * 检查是否有正在显示的 Loading
     */
    fun hasLoading(tag: String? = null): Boolean {
        val activity = getTopActivity() ?: return false
        val key = tag ?: activity.javaClass.name
        return loadingCounters[key]?.get() ?: 0 > 0
    }
}

