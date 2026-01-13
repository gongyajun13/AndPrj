package com.jun.core.ui.adapter

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 点击防抖管理器
 * 处理 Adapter 中的点击防抖逻辑
 */
class ClickDebounceManager(
    private var debounceTime: Long = 500L
) {
    private val clickJobs = mutableMapOf<Int, Job>()
    private var clickScope: CoroutineScope? = null
    private val lastClickTimes = mutableMapOf<Int, Long>()
    
    /**
     * 设置防抖时间
     */
    fun setDebounceTime(timeMillis: Long) {
        debounceTime = timeMillis
    }
    
    /**
     * 设置协程作用域
     */
    fun setScope(scope: CoroutineScope) {
        clickScope = scope
    }
    
    /**
     * 处理点击事件（带防抖）
     * @param position 位置
     * @param onClick 点击回调
     */
    fun handleClick(position: Int, onClick: () -> Unit) {
        val currentTime = System.currentTimeMillis()
        val lastTime = lastClickTimes[position] ?: 0L
        
        // 时间防抖：如果距离上次点击时间太短，忽略
        if (currentTime - lastTime < debounceTime) {
            return
        }
        lastClickTimes[position] = currentTime
        
        // 如果设置了协程作用域，使用协程防抖
        clickScope?.let { scope ->
            val job = clickJobs[position]
            job?.cancel()
            
            val newJob = scope.launch(Dispatchers.Main) {
                delay(debounceTime)
                onClick()
            }
            clickJobs[position] = newJob
        } ?: run {
            // 直接调用
            onClick()
        }
    }
    
    /**
     * 清空所有防抖任务
     */
    fun clear() {
        clickJobs.values.forEach { it.cancel() }
        clickJobs.clear()
        lastClickTimes.clear()
    }
}

