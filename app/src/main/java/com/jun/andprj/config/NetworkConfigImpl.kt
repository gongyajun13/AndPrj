package com.jun.andprj.config

import com.jun.core.network.config.NetworkConfig
import com.jun.andprj.util.constant.ApiConstants
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 网络配置实现
 */
@Singleton
class NetworkConfigImpl @Inject constructor() : NetworkConfig {
    override val baseUrl: String
        get() = ApiConstants.BASE_URL
    
    override val connectTimeoutSeconds: Long
        get() = 30
    
    override val readTimeoutSeconds: Long
        get() = 30
    
    override val writeTimeoutSeconds: Long
        get() = 30
    
    override val enableLogging: Boolean
        get() = com.jun.andprj.BuildConfig.DEBUG
    
    override val logLevel: NetworkConfig.LogLevel
        get() = if (com.jun.andprj.BuildConfig.DEBUG) {
            NetworkConfig.LogLevel.BODY
        } else {
            NetworkConfig.LogLevel.NONE
        }
    
    override val formatJsonResponse: Boolean
        get() = true
    
    override val maxResponseBodyLength: Int
        get() = 2000
    
    override val enableCache: Boolean
        get() = false
}

