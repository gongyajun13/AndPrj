package com.jun.core.domain.repository

import com.jun.core.common.error.toAppError
import com.jun.core.common.error.toAppResultError
import com.jun.core.common.result.AppResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 基础 Repository 接口
 * 提供通用的错误处理和协程调度功能
 */
interface BaseRepository {
    /**
     * 执行网络请求，自动处理错误和线程切换
     * @param block 要执行的挂起函数
     * @return AppResult<T> 统一的结果封装
     */
    suspend fun <T> executeNetworkCall(
        block: suspend () -> T
    ): AppResult<T> {
        return try {
            withContext(Dispatchers.IO) {
                val result = block()
                AppResult.Success(result)
            }
        } catch (e: Exception) {
            val appError = e.toAppError()
            appError.toAppResultError()
        }
    }
    
    /**
     * 执行数据库操作，自动处理错误和线程切换
     * @param block 要执行的挂起函数
     * @return AppResult<T> 统一的结果封装
     */
    suspend fun <T> executeDatabaseCall(
        block: suspend () -> T
    ): AppResult<T> {
        return try {
            withContext(Dispatchers.IO) {
                val result = block()
                AppResult.Success(result)
            }
        } catch (e: Exception) {
            AppResult.Error(
                exception = e,
                message = "数据库操作失败: ${e.message}"
            )
        }
    }
    
    /**
     * 执行通用操作，自动处理错误
     * @param block 要执行的挂起函数
     * @return AppResult<T> 统一的结果封装
     */
    suspend fun <T> executeCall(
        block: suspend () -> T
    ): AppResult<T> {
        return try {
            val result = block()
            AppResult.Success(result)
        } catch (e: Exception) {
            val appError = e.toAppError()
            appError.toAppResultError()
        }
    }
}

