package com.jun.andprj.data.remote.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * 常用网站数据模型
 */
@JsonClass(generateAdapter = true)
data class Friend(
    @Json(name = "id")
    val id: Int,
    @Json(name = "name")
    val name: String?,
    @Json(name = "link")
    val link: String?,
    @Json(name = "visible")
    val visible: Int = 1,
    @Json(name = "order")
    val order: Int = 0,
    @Json(name = "icon")
    val icon: String?
)

