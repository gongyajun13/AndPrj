package com.jun.andprj.data.remote.api

import com.jun.andprj.data.model.dto.UserDto
import com.jun.andprj.util.constant.ApiConstants
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface UserApi {
    @GET(ApiConstants.API_USERS)
    suspend fun getUsers(): Response<List<UserDto>>
    
    @GET(ApiConstants.API_USER_BY_ID)
    suspend fun getUserById(@Path("id") id: String): Response<UserDto>
}














