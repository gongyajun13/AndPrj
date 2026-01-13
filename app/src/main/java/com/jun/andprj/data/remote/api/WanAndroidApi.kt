package com.jun.andprj.data.remote.api

import com.jun.andprj.data.remote.model.WanAndroidResponse
import com.jun.andprj.data.remote.model.Article
import com.jun.andprj.data.remote.model.Banner
import com.jun.andprj.data.remote.model.Friend
import com.jun.andprj.data.remote.model.HotKey
import com.jun.andprj.data.remote.model.Tree
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * 玩Android开放API接口
 * 参考：https://www.wanandroid.com/blog/show/2
 */
interface WanAndroidApi {
    
    /**
     * 首页文章列表
     * @param page 页码，从0开始
     * @param pageSize 每页数量，可选，范围[1-40]
     */
    @GET("article/list/{page}/json")
    suspend fun getArticleList(
        @Path("page") page: Int,
        @Query("page_size") pageSize: Int? = null
    ): Response<WanAndroidResponse<ArticleListResponse>>
    
    /**
     * 首页banner
     */
    @GET("banner/json")
    suspend fun getBanner(): Response<WanAndroidResponse<List<Banner>>>
    
    /**
     * 常用网站
     */
    @GET("friend/json")
    suspend fun getFriend(): Response<WanAndroidResponse<List<Friend>>>
    
    /**
     * 搜索热词
     */
    @GET("hotkey/json")
    suspend fun getHotKey(): Response<WanAndroidResponse<List<HotKey>>>
    
    /**
     * 置顶文章
     */
    @GET("article/top/json")
    suspend fun getTopArticles(): Response<WanAndroidResponse<List<Article>>>
    
    /**
     * 体系数据
     */
    @GET("tree/json")
    suspend fun getTree(): Response<WanAndroidResponse<List<Tree>>>
    
    /**
     * 知识体系下的文章
     * @param page 页码，从0开始
     * @param cid 分类id
     * @param pageSize 每页数量，可选
     */
    @GET("article/list/{page}/json")
    suspend fun getTreeArticles(
        @Path("page") page: Int,
        @Query("cid") cid: Int,
        @Query("page_size") pageSize: Int? = null
    ): Response<WanAndroidResponse<ArticleListResponse>>
}

/**
 * 文章列表响应
 */
data class ArticleListResponse(
    val curPage: Int,
    val pageCount: Int,
    val size: Int,
    val total: Int,
    val datas: List<Article>
)

