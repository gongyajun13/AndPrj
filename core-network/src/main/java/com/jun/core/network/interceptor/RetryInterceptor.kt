package com.jun.core.network.interceptor

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import java.net.SocketTimeoutException

/**
 * 重试拦截器
 * 在网络请求失败时自动重试
 */
class RetryInterceptor(
    private val maxRetries: Int = 3,
    private val retryDelayMillis: Long = 1000,
    private val retryableExceptions: List<Class<out Throwable>> = listOf(
        IOException::class.java,
        SocketTimeoutException::class.java
    )
) : Interceptor {
    
    override fun intercept(chain: Interceptor.Chain): Response {
        var lastException: Exception? = null
        
        repeat(maxRetries) { attempt ->
            try {
                val response = chain.proceed(chain.request())
                
                // 如果响应成功，直接返回
                if (response.isSuccessful) {
                    return response
                }
                
                // 如果是最后一次尝试，返回响应
                if (attempt == maxRetries - 1) {
                    return response
                }
                
                // 关闭响应体
                response.close()
            } catch (e: Exception) {
                lastException = e as? Exception ?: Exception(e)
                
                // 检查是否是可重试的异常
                val isRetryable = retryableExceptions.any { it.isInstance(e) }
                
                if (!isRetryable || attempt == maxRetries - 1) {
                    throw e
                }
            }
            
            // 等待后重试
            if (attempt < maxRetries - 1) {
                Thread.sleep(retryDelayMillis * (attempt + 1))
            }
        }
        
        throw lastException ?: IOException("请求失败")
    }
}


