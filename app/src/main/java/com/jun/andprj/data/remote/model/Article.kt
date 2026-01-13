package com.jun.andprj.data.remote.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * 文章数据模型
 */
@JsonClass(generateAdapter = true)
data class Article(
    @Json(name = "id")
    val id: Int,
    @Json(name = "title")
    val title: String?,
    @Json(name = "link")
    val link: String?,
    @Json(name = "author")
    val author: String?,
    @Json(name = "shareUser")
    val shareUser: String?,
    @Json(name = "desc")
    val desc: String?,
    @Json(name = "envelopePic")
    val envelopePic: String?,
    @Json(name = "publishTime")
    val publishTime: Long,
    @Json(name = "niceDate")
    val niceDate: String?,
    @Json(name = "chapterName")
    val chapterName: String?,
    @Json(name = "superChapterName")
    val superChapterName: String?,
    @Json(name = "collect")
    val collect: Boolean = false,
    @Json(name = "zan")
    val zan: Int = 0
)

