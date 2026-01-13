package com.jun.core.domain.repository

import com.jun.core.common.result.AppResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Dispatchers

/**
 * Repository 扩展函数集合
 */

/**
 * 执行网络请求并返回 Flow
 */
suspend fun <T> BaseRepository.executeNetworkCallAsFlow(
    block: suspend () -> T
): Flow<AppResult<T>> {
    return flow {
        val result = executeNetworkCall(block)
        emit(result)
    }
        .catch { e ->
            emit(AppResult.Error(exception = e))
        }
        .flowOn(Dispatchers.IO)
}

/**
 * 执行数据库操作并返回 Flow
 */
suspend fun <T> BaseRepository.executeDatabaseCallAsFlow(
    block: suspend () -> T
): Flow<AppResult<T>> {
    return flow {
        val result = executeDatabaseCall(block)
        emit(result)
    }
        .catch { e ->
            emit(AppResult.Error(exception = e))
        }
        .flowOn(Dispatchers.IO)
}

/**
 * 执行通用操作并返回 Flow
 */
suspend fun <T> BaseRepository.executeCallAsFlow(
    block: suspend () -> T
): Flow<AppResult<T>> {
    return flow {
        val result = executeCall(block)
        emit(result)
    }
        .catch { e ->
            emit(AppResult.Error(exception = e))
        }
}


