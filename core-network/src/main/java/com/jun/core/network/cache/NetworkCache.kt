package com.jun.core.network.cache

import com.jun.core.common.result.AppResult
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

/**
 * 网络请求缓存接口
 */
interface NetworkCache<K, V> {
    /**
     * 获取缓存
     */
    suspend fun get(key: K): V?
    
    /**
     * 设置缓存
     */
    suspend fun put(key: K, value: V)
    
    /**
     * 移除缓存
     */
    suspend fun remove(key: K)
    
    /**
     * 清空缓存
     */
    suspend fun clear()
    
    /**
     * 获取缓存大小
     */
    suspend fun size(): Int
}

/**
 * 内存缓存实现
 * 用于缓存网络请求结果
 */
class MemoryNetworkCache<K, V>(
    private val maxSize: Int = 50,
    private val ttlMillis: Long = 5 * 60 * 1000 // 默认5分钟过期
) : NetworkCache<K, V> {
    
    private data class CacheEntry<V>(
        val value: V,
        val timestamp: Long
    ) {
        fun isExpired(ttl: Long): Boolean {
            return System.currentTimeMillis() - timestamp > ttl
        }
    }
    
    private val cache = ConcurrentHashMap<K, CacheEntry<V>>()
    private val mutex = Mutex()
    
    override suspend fun get(key: K): V? {
        return mutex.withLock {
            val entry = cache[key] ?: return null
            
            // 检查是否过期
            if (entry.isExpired(ttlMillis)) {
                cache.remove(key)
                return null
            }
            
            entry.value
        }
    }
    
    override suspend fun put(key: K, value: V) {
        mutex.withLock {
            // 如果超过最大大小，移除最旧的条目
            if (cache.size >= maxSize && !cache.containsKey(key)) {
                val oldestKey = cache.keys.firstOrNull()
                oldestKey?.let { cache.remove(it) }
            }
            
            cache[key] = CacheEntry(value, System.currentTimeMillis())
        }
    }
    
    override suspend fun remove(key: K) {
        mutex.withLock {
            cache.remove(key)
        }
    }
    
    override suspend fun clear() {
        mutex.withLock {
            cache.clear()
        }
    }
    
    override suspend fun size(): Int {
        return mutex.withLock {
            cache.size
        }
    }
    
    /**
     * 清理过期的缓存
     */
    suspend fun cleanup() {
        mutex.withLock {
            val expiredKeys = cache.entries
                .filter { it.value.isExpired(ttlMillis) }
                .map { it.key }
            
            expiredKeys.forEach { cache.remove(it) }
        }
    }
}

/**
 * 缓存策略
 */
enum class CachePolicy {
    /**
     * 不使用缓存
     */
    NO_CACHE,
    
    /**
     * 只使用缓存，不发起网络请求
     */
    CACHE_ONLY,
    
    /**
     * 优先使用缓存，缓存不存在时请求网络
     */
    CACHE_FIRST,
    
    /**
     * 优先请求网络，失败时使用缓存
     */
    NETWORK_FIRST,
    
    /**
     * 同时请求网络和缓存，优先返回缓存，然后更新为网络数据
     */
    CACHE_AND_NETWORK
}


