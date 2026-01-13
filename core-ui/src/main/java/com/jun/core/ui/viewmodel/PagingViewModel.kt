package com.jun.core.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jun.core.common.paging.PagingData
import com.jun.core.common.paging.PagingParams
import com.jun.core.common.paging.PagingState
import com.jun.core.common.result.AppResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 支持分页的 ViewModel 基类
 */
abstract class PagingViewModel<T> : ViewModel() {
    
    protected val _uiState = MutableStateFlow<PagingState<T>>(PagingState.Initial)
    val uiState: StateFlow<PagingState<T>> = _uiState.asStateFlow()
    
    protected val _pagingParams = MutableStateFlow(PagingParams())
    val pagingParams: StateFlow<PagingParams> = _pagingParams.asStateFlow()
    
    protected val _items = MutableStateFlow<List<T>>(emptyList())
    val items: StateFlow<List<T>> = _items.asStateFlow()
    
    /**
     * 加载第一页数据
     */
    fun loadFirstPage() {
        _pagingParams.value = PagingParams()
        _items.value = emptyList()
        executePagingLoad(_pagingParams.value)
    }
    
    /**
     * 加载下一页数据
     */
    fun loadNextPage() {
        val currentState = _uiState.value
        if (currentState is PagingState.Success && currentState.hasMore) {
            val nextParams = _pagingParams.value.next()
            _pagingParams.value = nextParams
            handleLoadingMore()
            executePagingLoad(nextParams)
        }
    }
    
    /**
     * 刷新数据
     */
    fun refresh() {
        _items.value = emptyList()
        loadFirstPage()
    }
    
    /**
     * 加载指定页的数据
     */
    protected abstract suspend fun loadPage(params: PagingParams): AppResult<PagingData<T>>
    
    /**
     * 执行分页加载
     */
    protected fun executePagingLoad(params: PagingParams) {
        viewModelScope.launch {
            _uiState.value = PagingState.Loading
            val result = loadPage(params)
            when (result) {
                is AppResult.Success -> {
                    handlePagingSuccess(result.data)
                }
                is AppResult.Error -> {
                    _uiState.value = PagingState.Error(
                        message = result.errorMessage,
                        throwable = result.exception
                    )
                }
                is AppResult.Loading -> {
                    // 已经在 Loading 状态
                }
            }
        }
    }
    
    /**
     * 处理分页成功结果
     */
    private fun handlePagingSuccess(pagingData: PagingData<T>) {
        val currentItems = _items.value
        val newItems = if (pagingData.isFirstPage) {
            pagingData.items
        } else {
            currentItems + pagingData.items
        }
        
        _items.value = newItems
        
        _uiState.value = if (pagingData.hasMore) {
            PagingState.Success(
                items = newItems,
                hasMore = true,
                currentPage = pagingData.currentPage
            )
        } else {
            PagingState.NoMoreData(newItems)
        }
    }
    
    /**
     * 处理分页加载更多
     */
    protected fun handleLoadingMore() {
        val currentItems = _items.value
        val currentPage = _pagingParams.value.page
        
        _uiState.value = PagingState.LoadingMore(
            items = currentItems,
            currentPage = currentPage
        )
    }
}

