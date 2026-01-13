package com.jun.andprj.data.repository

import com.jun.andprj.data.local.database.dao.UserDao
import com.jun.andprj.data.model.entity.UserEntity
import com.jun.andprj.data.remote.api.UserApi
import com.jun.andprj.domain.model.User
import com.jun.andprj.domain.repository.UserRepository
import com.jun.core.common.result.AppResult
import com.jun.core.domain.repository.BaseRepository
import kotlinx.coroutines.flow.first
import timber.log.Timber
import javax.inject.Inject

class UserRepositoryImpl @Inject constructor(
    private val userApi: UserApi,
    private val userDao: UserDao
) : UserRepository, BaseRepository {
    
    override suspend fun getUsers(): AppResult<List<User>> {
        // 先尝试从本地数据库读取当前已有数据
        val localUsersResult = executeDatabaseCall {
            userDao.getAllUsers().first().map { it.toDomain() }
        }
        val localUsers = localUsersResult.getOrNull() ?: emptyList()
        
        // 再尝试从网络获取最新数据并更新本地
        val networkResult = executeNetworkCall {
            val response = userApi.getUsers()
            if (response.isSuccessful) {
                val userDtos = response.body().orEmpty()
                val userEntities = userDtos.map { dto ->
                    UserEntity(
                        id = dto.id,
                        name = dto.name,
                        email = dto.email
                    )
                }
                // 保存到数据库
                userDao.insertUsers(userEntities)
                userEntities.map { it.toDomain() }
            } else {
                throw Exception("HTTP ${response.code()}")
            }
        }
        
        // 如果网络请求失败但本地有数据，返回本地数据
        return networkResult.onError { error ->
            if (localUsers.isNotEmpty()) {
                Timber.w("Network error, fallback to local cache")
            }
        }.let { result ->
            if (result.isError && localUsers.isNotEmpty()) {
                AppResult.Success(localUsers)
            } else {
                result
            }
        }
    }
    
    override suspend fun getUserById(id: String): AppResult<User> {
        return executeNetworkCall {
            val response = userApi.getUserById(id)
            if (response.isSuccessful) {
                val userDto = response.body()
                if (userDto != null) {
                    val userEntity = UserEntity(
                        id = userDto.id,
                        name = userDto.name,
                        email = userDto.email
                    )
                    userDao.insertUser(userEntity)
                    userEntity.toDomain()
                } else {
                    throw Exception("User not found")
                }
            } else {
                throw Exception("HTTP ${response.code()}")
            }
        }
    }
    
    private fun com.jun.andprj.data.model.entity.UserEntity.toDomain(): User {
        return User(
            id = id,
            name = name,
            email = email
        )
    }
}



