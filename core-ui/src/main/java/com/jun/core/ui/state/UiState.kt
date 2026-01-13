package com.jun.core.ui.state

/**
 * UI 状态封装
 * 用于统一管理 UI 的各种状态（加载中、成功、错误等）
 */
sealed class UiState<out T> {
    /**
     * 初始状态
     */
    object Initial : UiState<Nothing>()
    
    /**
     * 加载中状态
     */
    object Loading : UiState<Nothing>()
    
    /**
     * 成功状态，包含数据
     */
    data class Success<T>(val data: T) : UiState<T>()
    
    /**
     * 错误状态，包含错误信息
     */
    data class Error(
        val message: String,
        val throwable: Throwable? = null
    ) : UiState<Nothing>()
    
    /**
     * 空数据状态
     */
    object Empty : UiState<Nothing>()
    
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
     * 检查是否为空状态
     */
    val isEmpty: Boolean
        get() = this is Empty
    
    /**
     * 获取数据，如果为成功状态则返回数据，否则返回 null
     */
    fun getOrNull(): T? = when (this) {
        is Success -> data
        else -> null
    }
}

