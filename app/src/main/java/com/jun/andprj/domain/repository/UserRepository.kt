package com.jun.andprj.domain.repository

import com.jun.andprj.domain.model.User
import com.jun.core.common.result.AppResult

interface UserRepository {
    suspend fun getUsers(): AppResult<List<User>>
    suspend fun getUserById(id: String): AppResult<User>
}



