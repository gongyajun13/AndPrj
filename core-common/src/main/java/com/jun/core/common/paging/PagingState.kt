package com.jun.core.common.paging

import com.jun.core.common.result.AppResult

/**
 * 分页状态
 */
sealed class PagingState<out T> {
    /**
     * 初始状态
     */
    object Initial : PagingState<Nothing>()
    
    /**
     * 加载中
     */
    object Loading : PagingState<Nothing>()
    
    /**
     * 加载成功
     */
    data class Success<T>(
        val items: List<T>,
        val hasMore: Boolean = true,
        val currentPage: Int = 1
    ) : PagingState<T>()
    
    /**
     * 加载失败
     */
    data class Error(
        val message: String,
        val throwable: Throwable? = null
    ) : PagingState<Nothing>()
    
    /**
     * 加载更多中
     */
    data class LoadingMore<T>(
        val items: List<T>,
        val currentPage: Int
    ) : PagingState<T>()
    
    /**
     * 没有更多数据
     */
    data class NoMoreData<T>(
        val items: List<T>
    ) : PagingState<T>()
}

/**
 * 分页数据模型
 */
data class PagingData<T>(
    val items: List<T>,
    val currentPage: Int,
    val pageSize: Int,
    val totalPages: Int? = null,
    val totalItems: Int? = null
) {
    val hasMore: Boolean
        get() = totalPages?.let { currentPage < it } ?: true
    
    val isEmpty: Boolean
        get() = items.isEmpty()
    
    val isFirstPage: Boolean
        get() = currentPage == 1
    
    fun nextPage(): Int = currentPage + 1
}

/**
 * 分页请求参数
 */
data class PagingParams(
    val page: Int = 1,
    val pageSize: Int = 20
) {
    fun next(): PagingParams = copy(page = page + 1)
    fun reset(): PagingParams = copy(page = 1)
}

/**
 * 将 AppResult 转换为 PagingState
 */
fun <T> AppResult<PagingData<T>>.toPagingState(): PagingState<T> {
    return when (this) {
        is AppResult.Loading -> {
            @Suppress("UNCHECKED_CAST")
            PagingState.Loading as PagingState<T>
        }
        is AppResult.Success -> {
            if (data.isEmpty) {
                PagingState.NoMoreData<T>(emptyList())
            } else {
                PagingState.Success<T>(
                    items = data.items,
                    hasMore = data.hasMore,
                    currentPage = data.currentPage
                )
            }
        }
        is AppResult.Error -> {
            @Suppress("UNCHECKED_CAST")
            PagingState.Error(
                message = errorMessage,
                throwable = exception
            ) as PagingState<T>
        }
    }
}

