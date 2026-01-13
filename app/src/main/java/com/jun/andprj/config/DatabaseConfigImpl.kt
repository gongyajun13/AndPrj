package com.jun.andprj.config

import com.jun.core.database.config.DatabaseConfig
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 数据库配置实现
 */
@Singleton
class DatabaseConfigImpl @Inject constructor() : DatabaseConfig {
    override val databaseName: String
        get() = "app_database"
    
    override val databaseVersion: Int
        get() = 1
    
    override val fallbackToDestructiveMigration: Boolean
        get() = com.jun.andprj.BuildConfig.DEBUG
    
    override val exportSchema: Boolean
        get() = false
}

