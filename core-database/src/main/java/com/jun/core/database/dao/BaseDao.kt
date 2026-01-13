package com.jun.core.database.dao

import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Update

/**
 * 基础 DAO 接口
 * 提供通用的 CRUD 操作
 */
interface BaseDao<T> {
    
    /**
     * 插入单个实体
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: T): Long
    
    /**
     * 插入多个实体
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<T>): List<Long>
    
    /**
     * 更新实体
     */
    @Update
    suspend fun update(entity: T): Int
    
    /**
     * 更新多个实体
     */
    @Update
    suspend fun updateAll(entities: List<T>): Int
    
    /**
     * 删除实体
     */
    @Delete
    suspend fun delete(entity: T): Int
    
    /**
     * 删除多个实体
     */
    @Delete
    suspend fun deleteAll(entities: List<T>): Int
}

