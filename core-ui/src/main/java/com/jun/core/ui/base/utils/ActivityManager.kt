package com.jun.core.ui.base.utils

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Process
import androidx.core.app.ActivityCompat
import timber.log.Timber
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Activity 管理器
 * 用于统一管理应用中的所有 Activity
 * 
 * 功能包括：
 * - Activity 栈管理
 * - 统一的 Activity 启动方法
 * - 退出应用功能
 * - 查找和关闭特定 Activity
 * - 获取当前 Activity
 * 
 * 使用方式：
 * 1. 在 Application.onCreate() 中初始化：ActivityManager.init(application)
 * 2. 使用 ActivityManager 启动 Activity：ActivityManager.startActivity<YourActivity>(context)
 * 3. 退出应用：ActivityManager.exitApp()
 * 
 * 示例：
 * ```kotlin
 * // 初始化（在 Application 中）
 * class MyApplication : Application() {
 *     override fun onCreate() {
 *         super.onCreate()
 *         ActivityManager.init(this)
 *     }
 * }
 * 
 * // 启动 Activity
 * ActivityManager.startActivity<DetailActivity>(this) {
 *     putExtra("key", "value")
 *     addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
 * }
 * 
 * // 退出应用
 * ActivityManager.exitApp()
 * 
 * // 关闭指定 Activity
 * ActivityManager.finishActivity(DetailActivity::class.java)
 * 
 * // 获取当前 Activity
 * val currentActivity = ActivityManager.getCurrentActivity()
 * ```
 */
object ActivityManager {
    
    private var application: Application? = null
    private val activityStack = CopyOnWriteArrayList<Activity>()
    
    /**
     * 初始化 ActivityManager
     * 需要在 Application.onCreate() 中调用
     */
    fun init(application: Application) {
        this.application = application
        application.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                if (!activityStack.contains(activity)) {
                    activityStack.add(activity)
                    Timber.d("[ActivityManager] Activity 已创建: ${activity.javaClass.simpleName}, 栈大小: ${activityStack.size}")
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
                Timber.d("[ActivityManager] Activity 已恢复: ${activity.javaClass.simpleName}, 栈大小: ${activityStack.size}")
            }
            
            override fun onActivityPaused(activity: Activity) {
                Timber.d("[ActivityManager] Activity 已暂停: ${activity.javaClass.simpleName}")
            }
            
            override fun onActivityStopped(activity: Activity) {
                Timber.d("[ActivityManager] Activity 已停止: ${activity.javaClass.simpleName}")
            }
            
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            
            override fun onActivityDestroyed(activity: Activity) {
                activityStack.remove(activity)
                Timber.d("[ActivityManager] Activity 已销毁: ${activity.javaClass.simpleName}, 栈大小: ${activityStack.size}")
            }
        })
    }
    
    /**
     * 获取当前 Activity（栈顶的 Activity）
     */
    fun getCurrentActivity(): Activity? {
        return activityStack.lastOrNull()
    }
    
    /**
     * 获取 Activity 栈
     */
    fun getActivityStack(): List<Activity> {
        return activityStack.toList()
    }
    
    /**
     * 获取 Activity 栈大小
     */
    fun getActivityStackSize(): Int {
        return activityStack.size
    }
    
    /**
     * 查找指定类型的 Activity
     * @param clazz Activity 的 Class
     * @return 找到的 Activity，如果不存在则返回 null
     */
    fun findActivity(clazz: Class<out Activity>): Activity? {
        return activityStack.find { it.javaClass == clazz }
    }
    
    /**
     * 检查指定类型的 Activity 是否存在
     * @param clazz Activity 的 Class
     * @return 如果存在则返回 true，否则返回 false
     */
    fun hasActivity(clazz: Class<out Activity>): Boolean {
        return findActivity(clazz) != null
    }
    
    /**
     * 关闭指定类型的 Activity
     * @param clazz Activity 的 Class
     * @return 如果找到并关闭了 Activity 则返回 true，否则返回 false
     */
    fun finishActivity(clazz: Class<out Activity>): Boolean {
        val activity = findActivity(clazz)
        return if (activity != null && !activity.isFinishing) {
            activity.finish()
            Timber.d("[ActivityManager] 已关闭 Activity: ${clazz.simpleName}")
            true
        } else {
            Timber.w("[ActivityManager] 未找到或已关闭的 Activity: ${clazz.simpleName}")
            false
        }
    }
    
    /**
     * 关闭除指定类型外的所有 Activity
     * @param clazz 要保留的 Activity 的 Class
     */
    fun finishAllActivitiesExcept(clazz: Class<out Activity>) {
        val activitiesToFinish = activityStack.filter { it.javaClass != clazz && !it.isFinishing }
        activitiesToFinish.forEach { activity ->
            activity.finish()
            Timber.d("[ActivityManager] 已关闭 Activity: ${activity.javaClass.simpleName}")
        }
        Timber.d("[ActivityManager] 已关闭除 ${clazz.simpleName} 外的所有 Activity，共 ${activitiesToFinish.size} 个")
    }
    
    /**
     * 关闭所有 Activity
     */
    fun finishAllActivities() {
        val activitiesToFinish = activityStack.filter { !it.isFinishing }
        activitiesToFinish.forEach { activity ->
            activity.finish()
            Timber.d("[ActivityManager] 已关闭 Activity: ${activity.javaClass.simpleName}")
        }
        Timber.d("[ActivityManager] 已关闭所有 Activity，共 ${activitiesToFinish.size} 个")
    }
    
    /**
     * 退出应用
     * 会关闭所有 Activity 并退出应用
     */
    fun exitApp() {
        Timber.d("[ActivityManager] 开始退出应用")
        finishAllActivities()
        // 退出进程
        Process.killProcess(Process.myPid())
    }
    
    /**
     * 启动 Activity（使用泛型）
     * @param context 上下文
     * @param block Intent 配置块
     */
    inline fun <reified T : Activity> startActivity(
        context: Context,
        block: Intent.() -> Unit = {}
    ) {
        val intent = Intent(context, T::class.java)
        intent.block()
        startActivity(context, intent)
    }
    
    /**
     * 启动 Activity（使用 Class）
     * @param context 上下文
     * @param clazz Activity 的 Class
     * @param block Intent 配置块
     */
    fun startActivity(
        context: Context,
        clazz: Class<out Activity>,
        block: Intent.() -> Unit = {}
    ) {
        val intent = Intent(context, clazz)
        intent.block()
        startActivity(context, intent)
    }
    
    /**
     * 启动 Activity（使用 Intent）
     * @param context 上下文
     * @param intent Intent
     */
    fun startActivity(context: Context, intent: Intent) {
        try {
            if (context !is Activity) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Timber.d("[ActivityManager] 启动 Activity: ${intent.component?.className}")
        } catch (e: Exception) {
            Timber.e(e, "[ActivityManager] 启动 Activity 失败: ${intent.component?.className}")
        }
    }
    
    /**
     * 启动 Activity 并关闭当前 Activity
     * @param context 上下文（必须是 Activity）
     * @param block Intent 配置块
     */
    inline fun <reified T : Activity> startActivityAndFinish(
        context: Context,
        block: Intent.() -> Unit = {}
    ) {
        if (context is Activity) {
            startActivity<T>(context, block)
            context.finish()
        } else {
            Timber.w("[ActivityManager] startActivityAndFinish 的 context 必须是 Activity")
            startActivity<T>(context, block)
        }
    }
    
    /**
     * 启动 Activity 并关闭当前 Activity（使用 Class）
     * @param context 上下文（必须是 Activity）
     * @param clazz Activity 的 Class
     * @param block Intent 配置块
     */
    fun startActivityAndFinish(
        context: Context,
        clazz: Class<out Activity>,
        block: Intent.() -> Unit = {}
    ) {
        if (context is Activity) {
            startActivity(context, clazz, block)
            context.finish()
        } else {
            Timber.w("[ActivityManager] startActivityAndFinish 的 context 必须是 Activity")
            startActivity(context, clazz, block)
        }
    }
    
    /**
     * 启动 Activity 并清空任务栈（用于登录等场景）
     * @param context 上下文
     * @param block Intent 配置块
     */
    inline fun <reified T : Activity> startActivityAndClearTask(
        context: Context,
        block: Intent.() -> Unit = {}
    ) {
        val intent = Intent(context, T::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            block()
        }
        startActivity(context, intent)
    }
    
    /**
     * 启动 Activity 并清空任务栈（使用 Class）
     * @param context 上下文
     * @param clazz Activity 的 Class
     * @param block Intent 配置块
     */
    fun startActivityAndClearTask(
        context: Context,
        clazz: Class<out Activity>,
        block: Intent.() -> Unit = {}
    ) {
        val intent = Intent(context, clazz).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            block()
        }
        startActivity(context, intent)
    }
    
    /**
     * 启动 Activity 并返回结果
     * @param activity 当前 Activity
     * @param requestCode 请求码
     * @param block Intent 配置块
     */
    inline fun <reified T : Activity> startActivityForResult(
        activity: Activity,
        requestCode: Int,
        block: Intent.() -> Unit = {}
    ) {
        val intent = Intent(activity, T::class.java)
        intent.block()
        ActivityCompat.startActivityForResult(activity, intent, requestCode, null)
        Timber.d("[ActivityManager] 启动 Activity 并等待结果: ${T::class.java.simpleName}, requestCode: $requestCode")
    }
    
    /**
     * 启动 Activity 并返回结果（使用 Class）
     * @param activity 当前 Activity
     * @param clazz Activity 的 Class
     * @param requestCode 请求码
     * @param block Intent 配置块
     */
    fun startActivityForResult(
        activity: Activity,
        clazz: Class<out Activity>,
        requestCode: Int,
        block: Intent.() -> Unit = {}
    ) {
        val intent = Intent(activity, clazz)
        intent.block()
        ActivityCompat.startActivityForResult(activity, intent, requestCode, null)
        Timber.d("[ActivityManager] 启动 Activity 并等待结果: ${clazz.simpleName}, requestCode: $requestCode")
    }
    
    /**
     * 获取指定 Activity 之上的所有 Activity（用于返回到指定 Activity）
     * @param activity 目标 Activity
     * @return 需要关闭的 Activity 列表
     */
    fun getActivitiesAbove(activity: Activity): List<Activity> {
        val currentIndex = activityStack.indexOf(activity)
        return if (currentIndex >= 0 && currentIndex < activityStack.size - 1) {
            activityStack.subList(currentIndex + 1, activityStack.size)
                .filter { !it.isFinishing }
        } else {
            emptyList()
        }
    }
    
    /**
     * 返回到指定类型的 Activity
     * 如果栈中存在该 Activity，则关闭其上的所有 Activity
     * 如果不存在，则启动该 Activity
     * @param context 上下文
     * @param block Intent 配置块
     */
    inline fun <reified T : Activity> backToActivity(
        context: Context,
        noinline block: Intent.() -> Unit = {}
    ) {
        val clazz = T::class.java
        val activity = findActivity(clazz)
        
        if (activity != null && !activity.isFinishing) {
            // 如果栈中存在该 Activity，关闭其上的所有 Activity
            val activitiesToFinish = getActivitiesAbove(activity)
            activitiesToFinish.forEach { it.finish() }
            Timber.d("[ActivityManager] 返回到 Activity: ${clazz.simpleName}, 关闭了 ${activitiesToFinish.size} 个 Activity")
        } else {
            // 如果不存在，启动该 Activity
            startActivity(context, clazz, block)
            Timber.d("[ActivityManager] Activity 不存在，启动新 Activity: ${clazz.simpleName}")
        }
    }
    
    /**
     * 返回到指定类型的 Activity（使用 Class）
     * @param context 上下文
     * @param clazz Activity 的 Class
     * @param block Intent 配置块
     */
    fun backToActivity(
        context: Context,
        clazz: Class<out Activity>,
        block: Intent.() -> Unit = {}
    ) {
        val activity = findActivity(clazz)
        
        if (activity != null && !activity.isFinishing) {
            // 如果栈中存在该 Activity，关闭其上的所有 Activity
            val activitiesToFinish = getActivitiesAbove(activity)
            activitiesToFinish.forEach { it.finish() }
            Timber.d("[ActivityManager] 返回到 Activity: ${clazz.simpleName}, 关闭了 ${activitiesToFinish.size} 个 Activity")
        } else {
            // 如果不存在，启动该 Activity
            startActivity(context, clazz, block)
            Timber.d("[ActivityManager] Activity 不存在，启动新 Activity: ${clazz.simpleName}")
        }
    }
    
    /**
     * 清理资源（在 Application 销毁时调用，通常不需要手动调用）
     */
    fun cleanup() {
        activityStack.clear()
        application = null
        Timber.d("[ActivityManager] 已清理资源")
    }
}

