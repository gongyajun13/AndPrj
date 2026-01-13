package com.jun.andprj.data.remote.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * 搜索热词数据模型
 */
@JsonClass(generateAdapter = true)
data class HotKey(
    @Json(name = "id")
    val id: Int,
    @Json(name = "name")
    val name: String?,
    @Json(name = "link")
    val link: String?,
    @Json(name = "order")
    val order: Int = 0,
    @Json(name = "visible")
    val visible: Int = 1
)

