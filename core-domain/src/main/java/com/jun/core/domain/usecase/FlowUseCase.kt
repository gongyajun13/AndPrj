package com.jun.core.domain.usecase

import com.jun.core.common.result.AppResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

/**
 * Flow 类型的 UseCase 接口
 * 用于返回 Flow 数据流
 */
interface FlowUseCase<in P, out T> {
    /**
     * 执行 UseCase，返回 Flow
     */
    suspend operator fun invoke(params: P): Flow<AppResult<T>>
}

/**
 * Flow 类型的 UseCase 实现类
 */
abstract class FlowUseCaseImpl<in P, out T>(
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : FlowUseCase<P, T> {
    
    override suspend operator fun invoke(params: P): Flow<AppResult<T>> {
        return flow {
            try {
                val result = execute(params)
                emit(AppResult.Success(result))
            } catch (e: Exception) {
                emit(AppResult.Error(exception = e))
            }
        }
            .catch { e ->
                emit(AppResult.Error(exception = e))
            }
            .flowOn(dispatcher)
    }
    
    protected abstract suspend fun execute(params: P): T
}

/**
 * 无参数的 Flow UseCase 接口
 */
interface FlowUseCaseNoParams<out T> {
    suspend operator fun invoke(): Flow<AppResult<T>>
}

/**
 * 无参数的 Flow UseCase 实现类
 */
abstract class FlowUseCaseNoParamsImpl<out T>(
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : FlowUseCaseNoParams<T> {
    
    override suspend operator fun invoke(): Flow<AppResult<T>> {
        return flow {
            try {
                val result = execute()
                emit(AppResult.Success(result))
            } catch (e: Exception) {
                emit(AppResult.Error(exception = e))
            }
        }
            .catch { e ->
                emit(AppResult.Error(exception = e))
            }
            .flowOn(dispatcher)
    }
    
    protected abstract suspend fun execute(): T
}


