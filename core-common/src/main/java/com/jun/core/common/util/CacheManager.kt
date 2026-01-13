package com.jun.core.common.util

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

/**
 * 缓存管理器接口
 */
interface CacheManager {
    /**
     * 获取缓存
     */
    suspend fun <T> get(key: String): T?
    
    /**
     * 设置缓存
     */
    suspend fun <T> put(key: String, value: T, ttl: Long = 0)
    
    /**
     * 移除缓存
     */
    suspend fun remove(key: String)
    
    /**
     * 清空所有缓存
     */
    suspend fun clear()
    
    /**
     * 检查缓存是否存在
     */
    suspend fun contains(key: String): Boolean
}

/**
 * 内存缓存实现
 */
class MemoryCacheManager : CacheManager {
    
    private data class CacheItem<T>(
        val value: T,
        val expireTime: Long = 0
    ) {
        fun isExpired(): Boolean {
            return expireTime > 0 && System.currentTimeMillis() > expireTime
        }
    }
    
    private val cache = ConcurrentHashMap<String, CacheItem<*>>()
    private val mutex = Mutex()
    
    @Suppress("UNCHECKED_CAST")
    override suspend fun <T> get(key: String): T? {
        return mutex.withLock {
            val item = cache[key] as? CacheItem<T> ?: return null
            if (item.isExpired()) {
                cache.remove(key)
                return null
            }
            item.value
        }
    }
    
    override suspend fun <T> put(key: String, value: T, ttl: Long) {
        mutex.withLock {
            val expireTime = if (ttl > 0) {
                System.currentTimeMillis() + ttl
            } else {
                0
            }
            cache[key] = CacheItem(value, expireTime)
        }
    }
    
    override suspend fun remove(key: String) {
        mutex.withLock {
            cache.remove(key)
        }
    }
    
    override suspend fun clear() {
        mutex.withLock {
            cache.clear()
        }
    }
    
    override suspend fun contains(key: String): Boolean {
        return mutex.withLock {
            val item = cache[key]
            if (item?.isExpired() == true) {
                cache.remove(key)
                false
            } else {
                item != null
            }
        }
    }
    
    /**
     * 清理过期缓存
     */
    suspend fun cleanExpired() {
        mutex.withLock {
            val iterator = cache.iterator()
            while (iterator.hasNext()) {
                val entry = iterator.next()
                if ((entry.value as? CacheItem<*>)?.isExpired() == true) {
                    iterator.remove()
                }
            }
        }
    }
}

