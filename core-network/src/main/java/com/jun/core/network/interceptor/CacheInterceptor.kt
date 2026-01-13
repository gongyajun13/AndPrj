package com.jun.core.network.interceptor

import okhttp3.CacheControl
import okhttp3.Interceptor
import okhttp3.Response
import java.util.concurrent.TimeUnit

/**
 * 缓存拦截器
 * 提供 HTTP 缓存功能，减少网络请求
 */
class CacheInterceptor(
    private val maxAge: Int = 60, // 缓存最大存活时间（秒）
    private val maxStale: Int = 7 * 24 * 60 * 60 // 离线缓存最大存活时间（秒，默认7天）
) : Interceptor {
    
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)
        
        // 如果响应成功，添加缓存头
        if (response.isSuccessful) {
            val cacheControl = CacheControl.Builder()
                .maxAge(maxAge, TimeUnit.SECONDS)
                .maxStale(maxStale, TimeUnit.SECONDS)
                .build()
            
            return response.newBuilder()
                .header("Cache-Control", cacheControl.toString())
                .removeHeader("Pragma")
                .build()
        }
        
        return response
    }
}

/**
 * 离线缓存拦截器
 * 当网络不可用时，使用缓存数据
 */
class OfflineCacheInterceptor(
    private val maxStale: Int = 7 * 24 * 60 * 60, // 离线缓存最大存活时间（秒，默认7天）
    private val networkMonitor: com.jun.core.common.network.NetworkMonitor? = null
) : Interceptor {
    
    override fun intercept(chain: Interceptor.Chain): Response {
        var request = chain.request()
        
        // 如果网络不可用，强制使用缓存
        if (!isNetworkAvailable()) {
            val cacheControl = CacheControl.Builder()
                .onlyIfCached()
                .maxStale(maxStale, TimeUnit.SECONDS)
                .build()
            
            request = request.newBuilder()
                .cacheControl(cacheControl)
                .build()
        }
        
        return chain.proceed(request)
    }
    
    /**
     * 检查网络是否可用
     * 优先使用 NetworkMonitor，如果没有则使用简化版本
     */
    private fun isNetworkAvailable(): Boolean {
        return networkMonitor?.isNetworkAvailable() ?: true
    }
}

