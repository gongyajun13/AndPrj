package com.jun.core.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jun.core.common.result.AppResult
import com.jun.core.ui.state.UiState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * 基础 ViewModel
 * 提供统一的状态管理和错误处理
 */
abstract class BaseViewModel<UiStateType : UiState<*>> : ViewModel() {
    
    /**
     * UI 状态
     */
    protected val _uiState = MutableStateFlow<UiStateType>(createInitialState())
    val uiState: StateFlow<UiStateType> = _uiState.asStateFlow()
    
    /**
     * 创建初始状态
     */
    protected abstract fun createInitialState(): UiStateType
    
    /**
     * 更新 UI 状态
     */
    protected fun updateState(update: (UiStateType) -> UiStateType) {
        _uiState.update(update)
    }
    
    /**
     * 设置加载中状态
     */
    protected fun setLoading() {
        updateState { UiState.Loading as? UiStateType ?: it }
    }
    
    /**
     * 设置成功状态
     */
    protected fun <T> setSuccess(data: T) {
        updateState { UiState.Success(data) as? UiStateType ?: it }
    }
    
    /**
     * 设置错误状态
     */
    protected fun setError(message: String, throwable: Throwable? = null) {
        updateState { UiState.Error(message, throwable) as? UiStateType ?: it }
    }
    
    /**
     * 设置空状态
     */
    protected fun setEmpty() {
        updateState { UiState.Empty as? UiStateType ?: it }
    }
    
    /**
     * 处理 AppResult，自动更新 UI 状态
     */
    protected fun <T> handleResult(
        result: AppResult<T>,
        onSuccess: (T) -> Unit = {},
        onError: ((String, Throwable?) -> Unit)? = null
    ) {
        when (result) {
            is AppResult.Loading -> setLoading()
            is AppResult.Success -> {
                setSuccess(result.data)
                onSuccess(result.data)
            }
            is AppResult.Error -> {
                val errorMessage = result.errorMessage
                setError(errorMessage, result.exception)
                onError?.invoke(errorMessage, result.exception)
                Timber.e(result.exception, "Error: $errorMessage")
            }
        }
    }
    
    /**
     * 执行异步操作，自动处理结果
     */
    protected fun <T> executeAsync(
        block: suspend () -> AppResult<T>,
        onSuccess: (T) -> Unit = {},
        onError: ((String, Throwable?) -> Unit)? = null
    ) {
        viewModelScope.launch {
            setLoading()
            val result = block()
            handleResult(result, onSuccess, onError)
        }
    }
    
    /**
     * 执行异步操作（Flow 版本，支持响应式 UI 刷新）
     * 特别适用于 CACHE_AND_NETWORK 策略，可以接收缓存和网络结果
     * 
     * @param flow 要收集的 Flow<AppResult<T>>
     * @param onSuccess 成功回调
     * @param onError 错误回调
     */
    protected fun <T> executeAsyncFlow(
        flow: Flow<AppResult<T>>,
        onSuccess: (T) -> Unit = {},
        onError: ((String, Throwable?) -> Unit)? = null
    ) {
        viewModelScope.launch {
            setLoading()
            flow
                .catch { e ->
                    val errorMessage = "Flow 收集失败: ${e.message}"
                    setError(errorMessage, e)
                    onError?.invoke(errorMessage, e)
                    Timber.e(e, errorMessage)
                }
                .collect { result ->
                    handleResult(result, onSuccess, onError)
                }
        }
    }
    
    /**
     * 刷新数据
     */
    open fun refresh() {
        // 子类可以重写此方法来实现刷新逻辑
    }
}

