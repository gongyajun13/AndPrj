package com.jun.andprj.data.remote.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Banner数据模型
 */
@JsonClass(generateAdapter = true)
data class Banner(
    @Json(name = "id")
    val id: Int,
    @Json(name = "title")
    val title: String?,
    @Json(name = "desc")
    val desc: String?,
    @Json(name = "imagePath")
    val imagePath: String?,
    @Json(name = "url")
    val url: String?,
    @Json(name = "isVisible")
    val isVisible: Int = 1,
    @Json(name = "order")
    val order: Int = 0,
    @Json(name = "type")
    val type: Int = 0
)

