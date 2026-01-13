package com.jun.andprj.data.remote.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * 体系数据模型
 */
@JsonClass(generateAdapter = true)
data class Tree(
    @Json(name = "id")
    val id: Int,
    @Json(name = "name")
    val name: String?,
    @Json(name = "courseId")
    val courseId: Int,
    @Json(name = "parentChapterId")
    val parentChapterId: Int,
    @Json(name = "order")
    val order: Int = 0,
    @Json(name = "visible")
    val visible: Int = 1,
    @Json(name = "children")
    val children: List<Tree> = emptyList()
)

