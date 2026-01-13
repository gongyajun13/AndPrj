package com.jun.core.domain.usecase

import com.jun.core.common.result.AppResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 基础 UseCase 接口
 * 所有 UseCase 都应该实现此接口或继承 BaseUseCaseImpl
 */
interface BaseUseCase<in P, out T> {
    /**
     * 执行 UseCase
     * @param params 输入参数
     * @return AppResult<T> 统一的结果封装
     */
    suspend operator fun invoke(params: P): AppResult<T>
}

/**
 * 无参数的 UseCase 接口
 */
interface BaseUseCaseNoParams<out T> {
    /**
     * 执行 UseCase
     * @return AppResult<T> 统一的结果封装
     */
    suspend operator fun invoke(): AppResult<T>
}

/**
 * 基础 UseCase 实现类
 * 提供通用的执行逻辑和错误处理
 */
abstract class BaseUseCaseImpl<in P, out T>(
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : BaseUseCase<P, T> {
    
    override suspend operator fun invoke(params: P): AppResult<T> {
        return try {
            withContext(dispatcher) {
                val result = execute(params)
                AppResult.Success(result)
            }
        } catch (e: Exception) {
            AppResult.Error(exception = e)
        }
    }
    
    /**
     * 子类需要实现此方法来定义具体的业务逻辑
     */
    protected abstract suspend fun execute(params: P): T
}

/**
 * 无参数的基础 UseCase 实现类
 */
abstract class BaseUseCaseNoParamsImpl<out T>(
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : BaseUseCaseNoParams<T> {
    
    override suspend operator fun invoke(): AppResult<T> {
        return try {
            withContext(dispatcher) {
                val result = execute()
                AppResult.Success(result)
            }
        } catch (e: Exception) {
            AppResult.Error(exception = e)
        }
    }
    
    /**
     * 子类需要实现此方法来定义具体的业务逻辑
     */
    protected abstract suspend fun execute(): T
}

