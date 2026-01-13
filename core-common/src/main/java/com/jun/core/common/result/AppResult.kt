package com.jun.core.common.result

/**
 * 统一的应用程序结果封装
 * 用于替代标准库的 Result<T>，提供更丰富的错误信息和状态管理
 */
sealed class AppResult<out T> {
    /**
     * 成功状态，包含数据
     */
    data class Success<T>(val data: T) : AppResult<T>()
    
    /**
     * 错误状态，包含错误信息
     */
    data class Error(
        val exception: Throwable? = null,
        val message: String? = null,
        val code: Int? = null
    ) : AppResult<Nothing>() {
        val errorMessage: String
            get() = message ?: exception?.message ?: "未知错误"
    }
    
    /**
     * 加载中状态
     */
    object Loading : AppResult<Nothing>()
    
    /**
     * 检查是否为成功状态
     */
    val isSuccess: Boolean
        get() = this is Success
    
    /**
     * 检查是否为错误状态
     */
    val isError: Boolean
        get() = this is Error
    
    /**
     * 检查是否为加载中状态
     */
    val isLoading: Boolean
        get() = this is Loading
    
    /**
     * 获取数据，如果为成功状态则返回数据，否则返回 null
     */
    fun getOrNull(): T? = when (this) {
        is Success -> data
        else -> null
    }
    
    /**
     * 获取数据，如果为成功状态则返回数据，否则抛出异常
     */
    fun getOrThrow(): T = when (this) {
        is Success -> data
        is Error -> throw exception ?: IllegalStateException(errorMessage)
        is Loading -> throw IllegalStateException("数据正在加载中")
    }
    
    /**
     * 如果为成功状态则执行 action
     */
    inline fun onSuccess(action: (value: T) -> Unit): AppResult<T> {
        if (this is Success) {
            action(data)
        }
        return this
    }
    
    /**
     * 如果为错误状态则执行 action
     */
    inline fun onError(action: (error: Error) -> Unit): AppResult<T> {
        if (this is Error) {
            action(this)
        }
        return this
    }
    
    /**
     * 如果为加载中状态则执行 action
     */
    inline fun onLoading(action: () -> Unit): AppResult<T> {
        if (this is Loading) {
            action()
        }
        return this
    }
    
    /**
     * 映射数据
     */
    inline fun <R> map(transform: (value: T) -> R): AppResult<R> {
        return when (this) {
            is Success -> Success(transform(data))
            is Error -> this
            is Loading -> this
        }
    }
    
    /**
     * 扁平映射
     */
    inline fun <R> flatMap(transform: (value: T) -> AppResult<R>): AppResult<R> {
        return when (this) {
            is Success -> transform(data)
            is Error -> this
            is Loading -> this
        }
    }
}

/**
 * 将标准库的 Result<T> 转换为 AppResult<T>
 */
fun <T> Result<T>.toAppResult(): AppResult<T> {
    return fold(
        onSuccess = { AppResult.Success(it) },
        onFailure = { AppResult.Error(exception = it) }
    )
}

/**
 * 将 AppResult<T> 转换为标准库的 Result<T>
 */
fun <T> AppResult<T>.toResult(): Result<T> {
    return when (this) {
        is AppResult.Success -> Result.success(data)
        is AppResult.Error -> Result.failure(exception ?: Exception(errorMessage))
        is AppResult.Loading -> Result.failure(IllegalStateException("数据正在加载中"))
    }
}

