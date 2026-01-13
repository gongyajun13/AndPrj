package com.jun.andprj.data.remote.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * 玩Android API统一响应格式
 * 注意：errorCode 和 errorMsg 可能在某些接口中不存在，使用默认值
 */
@JsonClass(generateAdapter = true)
data class WanAndroidResponse<T>(
    @Json(name = "data")
    val data: T?,
    @Json(name = "errorCode")
    val errorCode: Int = 0,  // 默认值为 0（成功）
    @Json(name = "errorMsg")
    val errorMsg: String = ""  // 默认值为空字符串
)

