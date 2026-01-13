package com.jun.core.ui.extension

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.jun.core.ui.state.UiState
import com.jun.core.ui.widget.StateLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * StateLayout 相关扩展函数
 *
 * 目标：让页面层只需要一行代码就可以完成：
 * - 订阅 UiState
 * - 渲染 Loading/Empty/Error/Content
 * - 处理成功数据
 */

/**
 * 绑定 [StateLayout] 到 [StateFlow]<[UiState]>，适用于大部分场景
 */
fun <T> StateLayout.bindState(
    owner: LifecycleOwner,
    stateFlow: StateFlow<UiState<T>>,
    minActiveState: Lifecycle.State = Lifecycle.State.STARTED,
    onSuccess: (T) -> Unit = {},
    onError: ((String, Throwable?) -> Unit)? = null
) {
    bindStateInternal(
        scope = owner.lifecycleScope,
        lifecycle = owner.lifecycle,
        minActiveState = minActiveState,
        flow = stateFlow,
        onSuccess = onSuccess,
        onError = onError
    )
}

/**
 * 绑定 [StateLayout] 到任意 [Flow]<[UiState]>，更通用的版本
 */
fun <T> StateLayout.bindState(
    owner: LifecycleOwner,
    flow: Flow<UiState<T>>,
    minActiveState: Lifecycle.State = Lifecycle.State.STARTED,
    onSuccess: (T) -> Unit = {},
    onError: ((String, Throwable?) -> Unit)? = null
) {
    bindStateInternal(
        scope = owner.lifecycleScope,
        lifecycle = owner.lifecycle,
        minActiveState = minActiveState,
        flow = flow,
        onSuccess = onSuccess,
        onError = onError
    )
}

/**
 * 列表场景快捷绑定：自动根据列表是否为空切换 Empty/Content，并回调业务的 submitList
 */
fun <T> StateLayout.bindListState(
    owner: LifecycleOwner,
    stateFlow: StateFlow<UiState<List<T>>>,
    minActiveState: Lifecycle.State = Lifecycle.State.STARTED,
    submitList: (List<T>) -> Unit
) {
    bindState(
        owner = owner,
        stateFlow = stateFlow,
        minActiveState = minActiveState,
        onSuccess = { list ->
            submitList(list)
        }
    )
}

private fun <T> StateLayout.bindStateInternal(
    scope: CoroutineScope,
    lifecycle: Lifecycle,
    minActiveState: Lifecycle.State,
    flow: Flow<UiState<T>>,
    onSuccess: (T) -> Unit,
    onError: ((String, Throwable?) -> Unit)?
) {
    scope.launch {
        lifecycle.repeatOnLifecycle(minActiveState) {
            flow.collect { state ->
                // 交给 StateLayout 渲染 Loading / Empty / Error / Content
                renderState(state)

                when (state) {
                    is UiState.Success -> onSuccess(state.data)
                    is UiState.Error -> onError?.invoke(state.message, state.throwable)
                    else -> Unit
                }
            }
        }
    }
}


