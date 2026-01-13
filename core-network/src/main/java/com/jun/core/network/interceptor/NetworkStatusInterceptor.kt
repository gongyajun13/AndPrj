package com.jun.core.network.interceptor

import com.jun.core.common.network.NetworkMonitor
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

/**
 * 网络状态拦截器
 * 结合 NetworkMonitor，在网络不可用时直接返回错误，避免无效请求
 */
class NetworkStatusInterceptor(
    private val networkMonitor: NetworkMonitor
) : Interceptor {
    
    override fun intercept(chain: Interceptor.Chain): Response {
        // 检查网络是否可用
        if (!networkMonitor.isNetworkAvailable()) {
            throw IOException("网络不可用，请检查网络连接")
        }
        
        return chain.proceed(chain.request())
    }
}


