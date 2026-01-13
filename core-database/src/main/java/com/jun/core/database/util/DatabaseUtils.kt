package com.jun.core.database.util

import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * 数据库工具类
 */
object DatabaseUtils {
    
    /**
     * 创建简单的数据库迁移
     * @param startVersion 起始版本
     * @param endVersion 目标版本
     * @param migrationSql SQL 迁移语句
     */
    fun createSimpleMigration(
        startVersion: Int,
        endVersion: Int,
        vararg migrationSql: String
    ): Migration {
        return object : Migration(startVersion, endVersion) {
            override fun migrate(database: SupportSQLiteDatabase) {
                migrationSql.forEach { sql ->
                    database.execSQL(sql)
                }
            }
        }
    }
    
    /**
     * 清空数据库中的所有表
     * 注意：此操作不可逆，请谨慎使用
     */
    fun clearAllTables(database: RoomDatabase) {
        database.clearAllTables()
    }
    
    /**
     * 检查数据库是否已打开
     */
    fun isOpen(database: RoomDatabase): Boolean {
        return database.isOpen
    }
    
    /**
     * 获取数据库版本
     */
    fun getVersion(database: RoomDatabase): Int {
        return database.openHelper.readableDatabase.version
    }
}


