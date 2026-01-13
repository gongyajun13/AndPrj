package com.jun.core.network.interceptor

import okhttp3.Interceptor
import okhttp3.Response
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * 请求去重拦截器
 * 防止短时间内重复发送相同的请求
 */
class DeduplicationInterceptor(
    private val deduplicationWindowMillis: Long = 1000 // 去重时间窗口（毫秒）
) : Interceptor {
    
    private val pendingRequests = ConcurrentHashMap<String, PendingRequest>()
    private val lock = ReentrantLock()
    
    data class PendingRequest(
        val timestamp: Long,
        val response: Response
    )
    
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val requestKey = generateRequestKey(request)
        
        lock.withLock {
            val now = System.currentTimeMillis()
            val pending = pendingRequests[requestKey]
            
            // 如果在时间窗口内，返回缓存的响应
            if (pending != null && (now - pending.timestamp) < deduplicationWindowMillis) {
                // 克隆响应体，因为 ResponseBody 只能读取一次
                return pending.response.newBuilder()
                    .body(pending.response.peekBody(Long.MAX_VALUE))
                    .build()
            }
            
            // 执行请求
            val response = chain.proceed(request)
            
            // 如果请求成功，缓存响应
            if (response.isSuccessful) {
                pendingRequests[requestKey] = PendingRequest(now, response)
                
                // 清理过期的缓存
                cleanupExpiredRequests(now)
            }
            
            return response
        }
    }
    
    /**
     * 生成请求的唯一键
     */
    private fun generateRequestKey(request: okhttp3.Request): String {
        val url = request.url.toString()
        val method = request.method
        val bodyHash = request.body?.hashCode() ?: 0
        return "$method:$url:$bodyHash"
    }
    
    /**
     * 清理过期的请求缓存
     */
    private fun cleanupExpiredRequests(now: Long) {
        val expiredKeys = pendingRequests.entries
            .filter { (now - it.value.timestamp) >= deduplicationWindowMillis }
            .map { it.key }
        
        expiredKeys.forEach { pendingRequests.remove(it) }
    }
}


