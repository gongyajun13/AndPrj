package com.jun.andprj.domain.usecase

import com.jun.andprj.domain.model.User
import com.jun.andprj.domain.repository.UserRepository
import com.jun.core.common.result.AppResult
import com.jun.core.domain.usecase.BaseUseCaseNoParamsImpl
import javax.inject.Inject

class GetUsersUseCase @Inject constructor(
    private val repository: UserRepository
) : BaseUseCaseNoParamsImpl<List<User>>() {
    
    override suspend fun execute(): List<User> {
        return repository.getUsers().getOrThrow()
    }
}



