package com.jun.core.common.error

import com.jun.core.common.result.AppResult

/**
 * 应用程序错误类型
 */
sealed class AppError(
    open val message: String,
    open val code: Int? = null,
    open val cause: Throwable? = null
) {
    /**
     * 网络错误
     */
    data class NetworkError(
        override val message: String = "网络连接失败",
        override val code: Int? = null,
        override val cause: Throwable? = null
    ) : AppError(message, code, cause)
    
    /**
     * HTTP 错误
     */
    data class HttpError(
        override val message: String,
        override val code: Int,
        override val cause: Throwable? = null
    ) : AppError(message, code, cause)
    
    /**
     * 数据库错误
     */
    data class DatabaseError(
        override val message: String = "数据库操作失败",
        override val code: Int? = null,
        override val cause: Throwable? = null
    ) : AppError(message, code, cause)
    
    /**
     * 业务逻辑错误
     */
    data class BusinessError(
        override val message: String,
        override val code: Int? = null,
        override val cause: Throwable? = null
    ) : AppError(message, code, cause)
    
    /**
     * 未知错误
     */
    data class UnknownError(
        override val message: String = "未知错误",
        override val code: Int? = null,
        override val cause: Throwable? = null
    ) : AppError(message, code, cause)
}

/**
 * 将 Throwable 转换为 AppError
 */
fun Throwable.toAppError(): AppError {
    return when (this) {
        is java.net.UnknownHostException,
        is java.net.ConnectException,
        is java.net.SocketTimeoutException -> {
            AppError.NetworkError(cause = this)
        }
        is java.io.IOException -> {
            AppError.NetworkError(message = "网络IO错误", cause = this)
        }
        else -> {
            AppError.UnknownError(cause = this)
        }
    }
}

/**
 * 将 AppError 转换为 AppResult.Error
 */
fun AppError.toAppResultError(): AppResult.Error {
    return AppResult.Error(
        exception = cause,
        message = message,
        code = code
    )
}

