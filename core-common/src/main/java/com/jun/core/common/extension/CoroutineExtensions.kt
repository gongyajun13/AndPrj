package com.jun.core.common.extension

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch

/**
 * 防抖扩展函数
 * 在指定时间内只执行最后一次操作
 */
fun <T> Flow<T>.debounce(timeoutMillis: Long): Flow<T> = flow {
    var lastEmitTime = 0L
    collect { value ->
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastEmitTime >= timeoutMillis) {
            emit(value)
            lastEmitTime = currentTime
        }
    }
}

/**
 * 节流扩展函数
 * 在指定时间内只执行第一次操作
 */
fun <T> Flow<T>.throttle(timeoutMillis: Long): Flow<T> = flow {
    var lastEmitTime = 0L
    collect { value ->
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastEmitTime >= timeoutMillis) {
            emit(value)
            lastEmitTime = currentTime
        } else {
            delay(timeoutMillis - (currentTime - lastEmitTime))
            emit(value)
            lastEmitTime = System.currentTimeMillis()
        }
    }
}

/**
 * 添加加载状态
 */
fun <T> Flow<T>.withLoading(
    onStart: suspend () -> Unit = {},
    onComplete: suspend () -> Unit = {},
    onError: suspend (Throwable) -> Unit = {}
): Flow<T> = onStart { onStart() }
    .onCompletion { onComplete() }
    .catch { e ->
        onError(e)
        throw e
    }

/**
 * 安全启动协程，自动处理异常
 */
fun CoroutineScope.safeLaunch(
    onError: (Throwable) -> Unit = {},
    block: suspend CoroutineScope.() -> Unit
): Job {
    return launch {
        try {
            block()
        } catch (e: Throwable) {
            onError(e)
        }
    }
}

/**
 * 重试机制
 */
suspend fun <T> retry(
    times: Int = 3,
    initialDelay: Long = 100,
    maxDelay: Long = 1000,
    factor: Double = 2.0,
    block: suspend () -> T
): T {
    var currentDelay = initialDelay
    repeat(times - 1) {
        try {
            return block()
        } catch (e: Exception) {
            // 等待后重试
        }
        delay(currentDelay)
        currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelay)
    }
    return block() // 最后一次尝试
}

