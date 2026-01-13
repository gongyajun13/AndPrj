package com.jun.andprj.config

import com.jun.core.common.config.AppConfig

/**
 * App 配置实现
 */
class AppConfigImpl : AppConfig {
    override val isDebug: Boolean
        get() = com.jun.andprj.BuildConfig.DEBUG
    
    override val appName: String
        get() = "AndPrj"
    
    override val versionName: String
        get() = com.jun.andprj.BuildConfig.VERSION_NAME
    
    override val versionCode: Int
        get() = com.jun.andprj.BuildConfig.VERSION_CODE
}

