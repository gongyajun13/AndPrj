package com.jun.core.ui.base.utils

import android.content.DialogInterface
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.jun.core.ui.base.BaseDialog
import timber.log.Timber

/**
 * Dialog 队列管理工具类
 *
 * 设计目标：
 * - 同一时间只展示一个 Dialog
 * - 多个 Dialog 排队依次展示
 * - 支持优先级（priority）控制先后顺序
 * - 支持"插队"（高优先级、可选打断当前 Dialog）
 * - 支持队列监听和状态查询
 * - 支持去重和移除操作
 *
 * 使用方式（推荐在 Activity 中持有一个实例）：
 *
 * ```kotlin
 * class MainActivity : BaseActivity<ActivityMainBinding>() {
 *
 *     private val dialogQueue by lazy {
 *         DialogQueueManager(supportFragmentManager).apply {
 *             setOnQueueStateListener { hasCurrent, pendingCount ->
 *                 // 监听队列状态变化
 *             }
 *         }
 *     }
 *
 *     private fun showDialogs() {
 *         dialogQueue.enqueue(
 *             dialog = CustomDialog1(),
 *             priority = 0
 *         )
 *
 *         dialogQueue.enqueue(
 *             dialog = CustomDialog2(),
 *             priority = 10   // 更高优先级，会优先展示
 *         )
 *     }
 *
 *     private fun showInterruptDialog() {
 *         dialogQueue.enqueueAtFront(
 *             dialog = HighPriorityDialog(),
 *             priority = 100,
 *             interruptCurrent = true   // 打断当前 Dialog，优先展示
 *         )
 *     }
 *
 *     override fun onDestroy() {
 *         super.onDestroy()
 *         dialogQueue.release()
 *     }
 * }
 * ```
 */
class DialogQueueManager(
    private val fragmentManager: FragmentManager
) {

    /**
     * 队列项
     */
    private data class QueueItem(
        val dialog: AppCompatDialogFragment,
        val priority: Int,
        val tag: String?,
        val insertTime: Long = System.nanoTime(),
        val id: String = generateId()
    ) {
        companion object {
            private var idCounter = 0L
            private fun generateId(): String = "dialog_${++idCounter}_${System.nanoTime()}"
        }
    }

    /**
     * 队列状态监听器
     */
    interface OnQueueStateListener {
        /**
         * 队列状态变化时调用
         * @param hasCurrent 当前是否有 Dialog 正在显示
         * @param pendingCount 队列中等待的 Dialog 数量
         */
        fun onStateChanged(hasCurrent: Boolean, pendingCount: Int)
    }

    private val mainHandler = Handler(Looper.getMainLooper())

    /** 等待展示的 Dialog 队列 */
    private val queue = mutableListOf<QueueItem>()

    /** 当前正在展示的 Dialog（如果有） */
    private var current: QueueItem? = null

    /** 是否已注册生命周期回调 */
    private var lifecycleRegistered = false

    /** 队列状态监听器 */
    private var onQueueStateListener: OnQueueStateListener? = null

    /** 是否启用去重（相同 tag 的 Dialog 只保留一个） */
    private var enableDeduplication: Boolean = true

    /** 监听 Dialog 销毁和关闭，用于自动展示下一个 */
    private val fragmentLifecycleCallbacks =
        object : FragmentManager.FragmentLifecycleCallbacks() {
            override fun onFragmentDestroyed(fm: FragmentManager, f: Fragment) {
                super.onFragmentDestroyed(fm, f)
                synchronized(this@DialogQueueManager) {
                    val cur = current ?: return
                    if (f === cur.dialog) {
                        current = null
                        notifyStateChanged()
                        showNextLocked()
                    }
                }
            }
        }

    /** Dialog 关闭监听器 */
    private val dialogDismissListener = DialogInterface.OnDismissListener { dialog ->
        synchronized(this@DialogQueueManager) {
            val cur = current ?: return@OnDismissListener
            // 检查是否是当前 Dialog
            if (cur.dialog.dialog === dialog) {
                current = null
                notifyStateChanged()
                showNextLocked()
            }
        }
    }

    init {
        registerLifecycleCallbacksIfNeeded()
    }

    // ==================== 对外 API ====================

    /**
     * 设置队列状态监听器
     */
    fun setOnQueueStateListener(listener: OnQueueStateListener?) {
        synchronized(this) {
            onQueueStateListener = listener
        }
    }

    /**
     * 设置是否启用去重（默认 true）
     * 启用后，相同 tag 的 Dialog 只会保留队列中优先级最高的一个
     */
    fun setEnableDeduplication(enable: Boolean) {
        synchronized(this) {
            enableDeduplication = enable
        }
    }

    /**
     * 普通入队：按优先级排队展示
     *
     * @param dialog 需要展示的 DialogFragment（推荐继承 BaseDialog）
     * @param priority 优先级，数值越大优先级越高，默认 0
     * @param tag Dialog 的 tag，可为空，为空时使用类名
     * @return 是否成功入队（如果启用去重且已存在相同 tag，可能返回 false）
     */
    fun enqueue(
        dialog: AppCompatDialogFragment,
        priority: Int = 0,
        tag: String? = null
    ): Boolean {
        val finalTag = tag ?: dialog::class.java.simpleName
        val item = QueueItem(dialog, priority, finalTag)
        
        synchronized(this) {
            registerLifecycleCallbacksIfNeeded()
            
            // 去重处理
            if (enableDeduplication) {
                val existingIndex = queue.indexOfFirst { it.tag == finalTag }
                if (existingIndex >= 0) {
                    val existing = queue[existingIndex]
                    // 如果新 Dialog 优先级更高，替换旧的
                    if (priority > existing.priority) {
                        queue.removeAt(existingIndex)
                        Timber.d("DialogQueue: 替换相同 tag 的 Dialog: $finalTag (新优先级: $priority > 旧优先级: ${existing.priority})")
                    } else {
                        Timber.d("DialogQueue: 忽略低优先级的 Dialog: $finalTag (新优先级: $priority <= 旧优先级: ${existing.priority})")
                        return false
                    }
                }
            }
            
            queue.add(item)
            sortQueueLocked()
            notifyStateChanged()
            
            if (current == null) {
                showNextLocked()
            }
            
            return true
        }
    }

    /**
     * 插队入队：可选择打断当前 Dialog，优先展示
     *
     * @param dialog 需要展示的 DialogFragment
     * @param priority 优先级，默认一个很大的值（Int.MAX_VALUE / 2）
     * @param interruptCurrent 是否打断当前正在展示的 Dialog
     * @param tag Dialog 的 tag，可为空，为空时使用类名
     * @return 是否成功入队
     */
    fun enqueueAtFront(
        dialog: AppCompatDialogFragment,
        priority: Int = Int.MAX_VALUE / 2,
        interruptCurrent: Boolean = false,
        tag: String? = null
    ): Boolean {
        val finalTag = tag ?: dialog::class.java.simpleName
        val item = QueueItem(dialog, priority, finalTag)
        
        synchronized(this) {
            registerLifecycleCallbacksIfNeeded()
            
            // 去重处理
            if (enableDeduplication) {
                val existingIndex = queue.indexOfFirst { it.tag == finalTag }
                if (existingIndex >= 0) {
                    queue.removeAt(existingIndex)
                    Timber.d("DialogQueue: 插队时移除相同 tag 的 Dialog: $finalTag")
                }
            }
            
            queue.add(item)
            sortQueueLocked()
            notifyStateChanged()

            if (interruptCurrent && current != null) {
                // 打断当前 Dialog，优先展示新 Dialog
                Timber.d("DialogQueue: 打断当前 Dialog，优先展示: $finalTag")
                safelyDismissCurrentLocked()
            }

            if (current == null) {
                showNextLocked()
            }
            
            return true
        }
    }

    /**
     * 从队列中移除指定 tag 的 Dialog
     *
     * @param tag Dialog 的 tag
     * @return 是否成功移除
     */
    fun removeByTag(tag: String): Boolean {
        synchronized(this) {
            val removed = queue.removeAll { it.tag == tag }
            if (removed) {
                notifyStateChanged()
                Timber.d("DialogQueue: 从队列中移除 Dialog: $tag")
            }
            return removed
        }
    }

    /**
     * 从队列中移除指定 Dialog 实例
     *
     * @param dialog Dialog 实例
     * @return 是否成功移除
     */
    fun remove(dialog: AppCompatDialogFragment): Boolean {
        synchronized(this) {
            val removed = queue.removeAll { it.dialog === dialog }
            if (removed) {
                notifyStateChanged()
                Timber.d("DialogQueue: 从队列中移除 Dialog: ${dialog::class.java.simpleName}")
            }
            return removed
        }
    }

    /**
     * 清空队列并（可选）关闭当前 Dialog
     *
     * @param dismissCurrent 是否同时关闭当前正在展示的 Dialog
     */
    fun clear(dismissCurrent: Boolean = false) {
        synchronized(this) {
            val count = queue.size
            queue.clear()
            if (dismissCurrent) {
                safelyDismissCurrentLocked()
            }
            if (count > 0) {
                notifyStateChanged()
                Timber.d("DialogQueue: 清空队列，移除了 $count 个 Dialog")
            }
        }
    }

    /**
     * 队列中是否还有等待展示的 Dialog
     */
    fun hasPending(): Boolean = synchronized(this) {
        queue.isNotEmpty()
    }

    /**
     * 当前是否有 Dialog 正在展示
     */
    fun hasCurrent(): Boolean = synchronized(this) {
        current != null
    }

    /**
     * 获取队列中等待的 Dialog 数量
     */
    fun getPendingCount(): Int = synchronized(this) {
        queue.size
    }

    /**
     * 获取当前正在展示的 Dialog
     */
    fun getCurrentDialog(): AppCompatDialogFragment? = synchronized(this) {
        current?.dialog
    }

    /**
     * 获取当前正在展示的 Dialog 的 tag
     */
    fun getCurrentTag(): String? = synchronized(this) {
        current?.tag
    }

    /**
     * 检查队列中是否包含指定 tag 的 Dialog
     */
    fun containsTag(tag: String): Boolean = synchronized(this) {
        queue.any { it.tag == tag }
    }

    /**
     * 释放资源（如在 Activity.onDestroy 中调用）
     */
    fun release() {
        synchronized(this) {
            if (lifecycleRegistered) {
                try {
                    fragmentManager.unregisterFragmentLifecycleCallbacks(fragmentLifecycleCallbacks)
                } catch (e: Exception) {
                    Timber.w(e, "DialogQueue: 注销生命周期回调失败")
                }
                lifecycleRegistered = false
            }
            
            // 移除 Dialog 关闭监听器
            current?.dialog?.dialog?.setOnDismissListener(null)
            
            queue.clear()
            current = null
            onQueueStateListener = null
            notifyStateChanged()
            
            Timber.d("DialogQueue: 已释放资源")
        }
    }

    // ==================== 内部逻辑 ====================

    /** 注册 Fragment 生命周期回调（只注册一次） */
    private fun registerLifecycleCallbacksIfNeeded() {
        if (!lifecycleRegistered) {
            try {
                fragmentManager.registerFragmentLifecycleCallbacks(fragmentLifecycleCallbacks, false)
                lifecycleRegistered = true
                Timber.d("DialogQueue: 已注册生命周期回调")
            } catch (e: Exception) {
                Timber.w(e, "DialogQueue: 注册生命周期回调失败")
            }
        }
    }

    /** 根据优先级和插入时间对队列排序（优先级高的先出，优先级相同时先插入的先出） */
    private fun sortQueueLocked() {
        queue.sortWith(
            compareByDescending<QueueItem> { it.priority }
                .thenBy { it.insertTime }
        )
    }

    /**
     * 展示下一个 Dialog（需在 synchronized 块内调用）
     */
    private fun showNextLocked() {
        if (current != null) return
        if (queue.isEmpty()) return

        val next = queue.removeAt(0)
        current = next

        showDialogOnMainThread(next)
    }

    /**
     * 在主线程展示 Dialog
     */
    private fun showDialogOnMainThread(item: QueueItem) {
        val runnable = Runnable {
            if (fragmentManager.isDestroyed) {
                // 宿主已销毁，丢弃当前 Dialog
                synchronized(this) {
                    current = null
                    notifyStateChanged()
                    showNextLocked()
                }
                return@Runnable
            }

            val tag = item.tag ?: item.dialog::class.java.simpleName
            try {
                // 优先使用 BaseDialog 的 showSafely 方法
                if (item.dialog is BaseDialog<*>) {
                    item.dialog.showSafely(fragmentManager, tag)
                } else {
                    // 兼容其他 DialogFragment
                    if (!item.dialog.isAdded && !item.dialog.isVisible && !item.dialog.isRemoving) {
                        item.dialog.show(fragmentManager, tag)
                    }
                }
                
                // 设置关闭监听器
                item.dialog.dialog?.setOnDismissListener(dialogDismissListener)
                
                Timber.d("DialogQueue: 显示 Dialog: $tag (优先级: ${item.priority})")
            } catch (e: Exception) {
                Timber.w(e, "DialogQueue: 显示 Dialog 失败: $tag")
                // 显示失败时，继续下一个
                synchronized(this) {
                    current = null
                    notifyStateChanged()
                    showNextLocked()
                }
            }
        }

        if (Looper.myLooper() == Looper.getMainLooper()) {
            runnable.run()
        } else {
            mainHandler.post(runnable)
        }
    }

    /**
     * 安全关闭当前 Dialog（不触发 showNextLocked，供内部使用）
     */
    private fun safelyDismissCurrentLocked() {
        val cur = current ?: return
        try {
            // 移除关闭监听器，避免触发 showNextLocked
            cur.dialog.dialog?.setOnDismissListener(null)
            
            // 优先使用 BaseDialog 的 dismissSafely 方法
            if (cur.dialog is BaseDialog<*>) {
                cur.dialog.dismissSafely()
            } else {
                cur.dialog.dismissAllowingStateLoss()
            }
            
            Timber.d("DialogQueue: 关闭当前 Dialog: ${cur.tag}")
        } catch (e: Exception) {
            Timber.w(e, "DialogQueue: 关闭 Dialog 失败: ${cur.tag}")
        } finally {
            current = null
            notifyStateChanged()
        }
    }

    /**
     * 通知队列状态变化
     */
    private fun notifyStateChanged() {
        val listener = onQueueStateListener
        val hasCurrent = current != null
        val pendingCount = queue.size
        
        if (listener != null) {
            mainHandler.post {
                listener.onStateChanged(hasCurrent, pendingCount)
            }
        }
    }
}


