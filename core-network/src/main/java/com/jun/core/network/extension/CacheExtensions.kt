package com.jun.core.network.extension

import com.jun.core.common.result.AppResult
import com.jun.core.network.api.safeApiCallEnhanced
import com.jun.core.network.cache.CachePolicy
import com.jun.core.network.cache.NetworkCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import retrofit2.Response

/**
 * 带缓存的网络请求扩展函数
 */

/**
 * 带缓存的网络请求
 * @param cache 缓存实例
 * @param cacheKey 缓存键
 * @param cachePolicy 缓存策略
 * @param apiCall 网络请求
 */
suspend fun <T> cachedApiCall(
    cache: NetworkCache<String, T>?,
    cacheKey: String,
    cachePolicy: CachePolicy = CachePolicy.NETWORK_FIRST,
    apiCall: suspend () -> Response<T>
): AppResult<T> {
    if (cache == null) {
        // 如果没有缓存，直接请求网络
        return safeApiCallEnhanced(apiCall)
    }
    
    return when (cachePolicy) {
        CachePolicy.NO_CACHE -> {
            // 不使用缓存，直接请求网络
            safeApiCallEnhanced(apiCall)
        }
        
        CachePolicy.CACHE_ONLY -> {
            // 只使用缓存
            val cached = cache.get(cacheKey)
            if (cached != null) {
                AppResult.Success(cached)
            } else {
                AppResult.Error(
                    message = "缓存不存在",
                    exception = null
                )
            }
        }
        
        CachePolicy.CACHE_FIRST -> {
            // 优先使用缓存
            val cached = cache.get(cacheKey)
            if (cached != null) {
                AppResult.Success(cached)
            } else {
                // 缓存不存在，请求网络
                val result = safeApiCallEnhanced(apiCall)
                if (result is AppResult.Success) {
                    cache.put(cacheKey, result.data)
                }
                result
            }
        }
        
        CachePolicy.NETWORK_FIRST -> {
            // 优先请求网络
            val result = safeApiCallEnhanced(apiCall)
            if (result is AppResult.Success) {
                // 网络请求成功，更新缓存
                cache.put(cacheKey, result.data)
                result
            } else {
                // 网络请求失败，尝试使用缓存
                val cached = cache.get(cacheKey)
                if (cached != null) {
                    AppResult.Success(cached)
                } else {
                    result
                }
            }
        }
        
        CachePolicy.CACHE_AND_NETWORK -> {
            // 同时使用缓存和网络
            val cached = cache.get(cacheKey)
            
            // 先返回缓存（如果存在）
            if (cached != null) {
                // 在后台更新网络数据
                CoroutineScope(Dispatchers.IO).launch {
                    val networkResult = safeApiCallEnhanced(apiCall)
                    if (networkResult is AppResult.Success) {
                        cache.put(cacheKey, networkResult.data)
                    }
                }
                AppResult.Success(cached)
            } else {
                // 缓存不存在，请求网络
                val result = safeApiCallEnhanced(apiCall)
                if (result is AppResult.Success) {
                    cache.put(cacheKey, result.data)
                }
                result
            }
        }
    }
}

/**
 * 带缓存的网络请求（Flow 版本）
 */
fun <T> cachedApiCallFlow(
    cache: NetworkCache<String, T>?,
    cacheKey: String,
    cachePolicy: CachePolicy = CachePolicy.NETWORK_FIRST,
    apiCall: suspend () -> Response<T>
): Flow<AppResult<T>> = flow {
    emit(cachedApiCall(cache, cacheKey, cachePolicy, apiCall))
}

