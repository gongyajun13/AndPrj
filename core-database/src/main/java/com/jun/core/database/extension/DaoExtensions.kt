package com.jun.core.database.extension

import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Update
import com.jun.core.common.result.AppResult
import com.jun.core.database.dao.BaseDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * DAO 扩展函数集合
 */

/**
 * 安全插入实体，返回 AppResult
 */
suspend fun <T> BaseDao<T>.insertSafe(entity: T): AppResult<Long> {
    return try {
        withContext(Dispatchers.IO) {
            val result = insert(entity)
            AppResult.Success(result)
        }
    } catch (e: Exception) {
        AppResult.Error(
            exception = e,
            message = "插入失败: ${e.message}"
        )
    }
}

/**
 * 安全插入多个实体，返回 AppResult
 */
suspend fun <T> BaseDao<T>.insertAllSafe(entities: List<T>): AppResult<List<Long>> {
    return try {
        withContext(Dispatchers.IO) {
            val result = insertAll(entities)
            AppResult.Success(result)
        }
    } catch (e: Exception) {
        AppResult.Error(
            exception = e,
            message = "批量插入失败: ${e.message}"
        )
    }
}

/**
 * 安全更新实体，返回 AppResult
 */
suspend fun <T> BaseDao<T>.updateSafe(entity: T): AppResult<Int> {
    return try {
        withContext(Dispatchers.IO) {
            val result = update(entity)
            AppResult.Success(result)
        }
    } catch (e: Exception) {
        AppResult.Error(
            exception = e,
            message = "更新失败: ${e.message}"
        )
    }
}

/**
 * 安全更新多个实体，返回 AppResult
 */
suspend fun <T> BaseDao<T>.updateAllSafe(entities: List<T>): AppResult<Int> {
    return try {
        withContext(Dispatchers.IO) {
            val result = updateAll(entities)
            AppResult.Success(result)
        }
    } catch (e: Exception) {
        AppResult.Error(
            exception = e,
            message = "批量更新失败: ${e.message}"
        )
    }
}

/**
 * 安全删除实体，返回 AppResult
 */
suspend fun <T> BaseDao<T>.deleteSafe(entity: T): AppResult<Int> {
    return try {
        withContext(Dispatchers.IO) {
            val result = delete(entity)
            AppResult.Success(result)
        }
    } catch (e: Exception) {
        AppResult.Error(
            exception = e,
            message = "删除失败: ${e.message}"
        )
    }
}

/**
 * 安全删除多个实体，返回 AppResult
 */
suspend fun <T> BaseDao<T>.deleteAllSafe(entities: List<T>): AppResult<Int> {
    return try {
        withContext(Dispatchers.IO) {
            val result = deleteAll(entities)
            AppResult.Success(result)
        }
    } catch (e: Exception) {
        AppResult.Error(
            exception = e,
            message = "批量删除失败: ${e.message}"
        )
    }
}

/**
 * 插入或更新实体（如果存在则更新，不存在则插入）
 */
suspend fun <T> BaseDao<T>.insertOrUpdate(entity: T): AppResult<Long> {
    return try {
        withContext(Dispatchers.IO) {
            val insertResult = insert(entity)
            AppResult.Success(insertResult)
        }
    } catch (e: Exception) {
        // 如果插入失败，尝试更新
        try {
            withContext(Dispatchers.IO) {
                val updateResult = update(entity)
                AppResult.Success(updateResult.toLong())
            }
        } catch (updateException: Exception) {
            AppResult.Error(
                exception = updateException,
                message = "插入或更新失败: ${updateException.message}"
            )
        }
    }
}


