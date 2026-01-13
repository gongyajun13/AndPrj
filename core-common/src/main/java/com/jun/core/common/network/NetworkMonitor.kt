package com.jun.core.common.network

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * 网络状态
 */
sealed class NetworkStatus {
    object Available : NetworkStatus()
    object Unavailable : NetworkStatus()
    object Lost : NetworkStatus()
}

/**
 * 网络监控接口
 */
interface NetworkMonitor {
    /**
     * 获取当前网络状态
     */
    val currentStatus: NetworkStatus
    
    /**
     * 网络状态 Flow
     */
    val networkStatus: Flow<NetworkStatus>
    
    /**
     * 检查网络是否可用
     */
    fun isNetworkAvailable(): Boolean
}

/**
 * 网络监控实现
 */
class NetworkMonitorImpl(
    private val context: Context
) : NetworkMonitor {
    
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    
    override val currentStatus: NetworkStatus
        get() = if (isNetworkAvailable()) {
            NetworkStatus.Available
        } else {
            NetworkStatus.Unavailable
        }
    
    @SuppressLint("MissingPermission")
    override val networkStatus: Flow<NetworkStatus> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(NetworkStatus.Available)
            }
            
            override fun onLost(network: Network) {
                trySend(NetworkStatus.Lost)
            }
            
            override fun onUnavailable() {
                trySend(NetworkStatus.Unavailable)
            }
        }
        
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        
        // 注意：使用此功能需要在 AndroidManifest.xml 中添加权限：
        // <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
        connectivityManager.registerNetworkCallback(request, callback)
        
        // 发送初始状态
        trySend(currentStatus)
        
        awaitClose {
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }.distinctUntilChanged()
    
    @SuppressLint("MissingPermission")
    override fun isNetworkAvailable(): Boolean {
        // 注意：使用此功能需要在 AndroidManifest.xml 中添加权限：
        // <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}

