package com.jun.andprj.ui.user

import com.jun.andprj.domain.model.User
import com.jun.andprj.domain.usecase.GetUsersUseCase
import com.jun.core.ui.state.UiState
import com.jun.core.ui.viewmodel.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class UserViewModel @Inject constructor(
    private val getUsersUseCase: GetUsersUseCase
) : BaseViewModel<UiState<List<User>>>() {
    
    init {
        loadUsers()
    }
    
    /**
     * 加载用户列表
     * 支持下拉刷新
     */
    fun loadUsers() {
        executeAsync(
            block = { getUsersUseCase() },
            onSuccess = { users ->
                // 可以在这里处理成功后的额外逻辑
            }
        )
    }
    
    /**
     * 刷新用户列表
     */
    override fun refresh() {
        loadUsers()
    }
    
    override fun createInitialState(): UiState<List<User>> {
        return UiState.Initial
    }
}



