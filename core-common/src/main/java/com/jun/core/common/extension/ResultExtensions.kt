package com.jun.core.common.extension

import com.jun.core.common.result.AppResult

/**
 * 将标准库的 Result<T> 转换为 AppResult<T> 的扩展函数
 */
fun <T> Result<T>.toAppResult(): AppResult<T> {
    return fold(
        onSuccess = { AppResult.Success(it) },
        onFailure = { AppResult.Error(exception = it) }
    )
}

