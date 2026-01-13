package com.jun.core.network.util

import android.content.Context
import okhttp3.Cache
import java.io.File

/**
 * OkHttp 缓存辅助工具
 */
object OkHttpCacheHelper {
    
    /**
     * 创建 OkHttp 缓存
     * @param context 上下文
     * @param cacheSize 缓存大小（字节），默认 10MB
     * @param cacheDirName 缓存目录名称
     */
    fun createCache(
        context: Context,
        cacheSize: Long = 10 * 1024 * 1024, // 10MB
        cacheDirName: String = "okhttp_cache"
    ): Cache {
        val cacheDir = File(context.cacheDir, cacheDirName)
        return Cache(cacheDir, cacheSize)
    }
}


