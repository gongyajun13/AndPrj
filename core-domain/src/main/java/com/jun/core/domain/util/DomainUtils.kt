package com.jun.core.domain.util

import com.jun.core.common.result.AppResult

/**
 * 领域层工具类
 */
object DomainUtils {
    
    /**
     * 合并多个 AppResult
     * 如果所有结果都成功，返回成功；否则返回第一个错误
     */
    fun <T> combineResults(vararg results: AppResult<T>): AppResult<List<T>> {
        val successList = mutableListOf<T>()
        
        results.forEach { result ->
            when (result) {
                is AppResult.Success -> successList.add(result.data)
                is AppResult.Error -> return result
                is AppResult.Loading -> return AppResult.Loading
            }
        }
        
        return AppResult.Success(successList)
    }
    
    /**
     * 合并多个 AppResult（泛型版本）
     */
    fun <T1, T2> combineResults(
        result1: AppResult<T1>,
        result2: AppResult<T2>
    ): AppResult<Pair<T1, T2>> {
        return when {
            result1 is AppResult.Success && result2 is AppResult.Success -> {
                AppResult.Success(result1.data to result2.data)
            }
            result1 is AppResult.Error -> result1
            result2 is AppResult.Error -> result2
            else -> AppResult.Loading
        }
    }
    
    /**
     * 检查 AppResult 是否成功
     */
    fun <T> AppResult<T>.isSuccess(): Boolean {
        return this is AppResult.Success
    }
    
    /**
     * 检查 AppResult 是否失败
     */
    fun <T> AppResult<T>.isError(): Boolean {
        return this is AppResult.Error
    }
    
    /**
     * 检查 AppResult 是否加载中
     */
    fun <T> AppResult<T>.isLoading(): Boolean {
        return this is AppResult.Loading
    }
    
    /**
     * 获取成功的数据（如果成功）
     */
    fun <T> AppResult<T>.getDataOrNull(): T? {
        return if (this is AppResult.Success) data else null
    }
    
    /**
     * 获取错误消息（如果失败）
     */
    fun <T> AppResult<T>.getErrorMessageOrNull(): String? {
        return if (this is AppResult.Error) errorMessage else null
    }
}


