package com.jun.andprj.ui.tool

import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.jun.andprj.R
import com.jun.andprj.data.remote.api.ArticleListResponse
import com.jun.andprj.data.remote.model.Article
import com.jun.andprj.data.remote.model.Banner
import com.jun.andprj.data.remote.model.Friend
import com.jun.andprj.data.remote.model.HotKey
import com.jun.andprj.data.remote.model.Tree
import com.jun.andprj.data.remote.model.WanAndroidResponse
import com.jun.andprj.databinding.ActivityNetworkDemoBinding
import com.jun.core.common.result.AppResult
import com.jun.core.network.cache.CachePolicy
import com.jun.core.network.cache.MemoryNetworkCache
import com.jun.core.network.cache.NetworkCache
import com.jun.core.common.ui.LoadingDialogConfig
import com.jun.core.network.client.NetworkClient
import com.jun.core.network.client.requestConfig
import com.jun.core.network.config.NetworkInterceptorManager
import com.jun.core.ui.base.BaseActivity
import android.graphics.Color
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 网络请求示例Activity
 * 演示core-network模块的网络请求功能，包括：
 * 1. 使用 NetworkClient 进行网络请求
 * 2. 缓存策略（NO_CACHE, CACHE_ONLY, CACHE_FIRST, NETWORK_FIRST, CACHE_AND_NETWORK）
 * 3. 错误处理（自动转换为 AppResult）
 * 4. 内存缓存（MemoryNetworkCache）
 * 5. 详细的错误信息展示（JsonDataException, JsonEncodingException, 超时, 网络不可达等）
 * 6. requestConfig DSL 配置请求
 */
@AndroidEntryPoint
class NetworkDemoActivity : BaseActivity<ActivityNetworkDemoBinding>() {

    @Inject
    lateinit var networkClient: NetworkClient
    
    @Inject
    lateinit var interceptorManager: NetworkInterceptorManager

    private lateinit var adapter: NetworkDemoAdapter
    
    // 内存缓存实例（使用 NetworkCache 类型以兼容 NetworkClient）
    @Suppress("UNCHECKED_CAST")
    private val articleListCache = MemoryNetworkCache<String, ArticleListResponse>(
        maxSize = 10,
        ttlMillis = 5 * 60 * 1000 // 5分钟过期
    ) as NetworkCache<String, Any>
    
    @Suppress("UNCHECKED_CAST")
    private val bannerCache = MemoryNetworkCache<String, List<Banner>>(
        maxSize = 10,
        ttlMillis = 5 * 60 * 1000
    ) as NetworkCache<String, Any>
    
    // 当前选择的缓存策略
    private var currentCachePolicy: CachePolicy = CachePolicy.NETWORK_FIRST
    
    // 当前选择的 Loading 样式配置
    private var currentLoadingConfig: LoadingDialogConfig? = null

    override fun createBinding(): ActivityNetworkDemoBinding = ActivityNetworkDemoBinding.inflate(layoutInflater)

    override fun setupViews() {
        setupToolbar()
        setupRecyclerView()
        setupButtonListeners()
        showInitialMessage()
    }

    private fun setupToolbar() {
        val white = ContextCompat.getColor(this, android.R.color.white)
        val blue = ContextCompat.getColor(this, R.color.blue)
        setStatusBarColor(white, lightIcons = false)
        binding.toolbar.setupSimple(
            leftIcon = R.drawable.icon_back,
            leftIconTint = white,
            title = "网络请求示例",
            titleTextColor = white,
            backgroundColor = blue,
            onLeftClick = { finish() }
        )
    }

    private fun setupRecyclerView() {
        adapter = NetworkDemoAdapter()
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
    }

    private fun setupButtonListeners() {
        // 缓存策略选择
        binding.btnCachePolicyNoCache.setOnClickListener {
            currentCachePolicy = CachePolicy.NO_CACHE
            showMessage("已选择缓存策略: NO_CACHE（不使用缓存，直接请求网络）")
            updateCachePolicyDisplay()
        }
        
        binding.btnCachePolicyCacheOnly.setOnClickListener {
            currentCachePolicy = CachePolicy.CACHE_ONLY
            showMessage("已选择缓存策略: CACHE_ONLY（只使用缓存，不发起网络请求）")
            updateCachePolicyDisplay()
        }
        
        binding.btnCachePolicyCacheFirst.setOnClickListener {
            currentCachePolicy = CachePolicy.CACHE_FIRST
            showMessage("已选择缓存策略: CACHE_FIRST（优先使用缓存，缓存不存在时请求网络）")
            updateCachePolicyDisplay()
        }
        
        binding.btnCachePolicyNetworkFirst.setOnClickListener {
            currentCachePolicy = CachePolicy.NETWORK_FIRST
            showMessage("已选择缓存策略: NETWORK_FIRST（优先请求网络，失败时使用缓存）")
            updateCachePolicyDisplay()
        }
        
        binding.btnCachePolicyCacheAndNetwork.setOnClickListener {
            currentCachePolicy = CachePolicy.CACHE_AND_NETWORK
            showMessage("已选择缓存策略: CACHE_AND_NETWORK（先返回缓存，后台更新网络数据）")
            updateCachePolicyDisplay()
        }
        
        // 网络请求操作
        binding.btnGetArticleList.setOnClickListener {
            getArticleList()
        }

        binding.btnGetBanner.setOnClickListener {
            getBanner()
        }

        binding.btnGetFriend.setOnClickListener {
            getFriend()
        }

        binding.btnGetHotKey.setOnClickListener {
            getHotKey()
        }

        binding.btnGetTopArticles.setOnClickListener {
            getTopArticles()
        }

        binding.btnGetTree.setOnClickListener {
            getTree()
        }
        
        // BaseUrl 切换
        binding.btnSwitchBaseUrlProduction.setOnClickListener {
            switchBaseUrl("https://www.wanandroid.com/", "生产环境")
        }
        
        binding.btnSwitchBaseUrlTest.setOnClickListener {
            switchBaseUrl("https://test.wanandroid.com/", "测试环境")
        }
        
        binding.btnSwitchBaseUrlDev.setOnClickListener {
            switchBaseUrl("https://dev.wanandroid.com/", "开发环境")
        }
        
        binding.btnShowCurrentBaseUrl.setOnClickListener {
            showCurrentBaseUrl()
        }
        
        // 缓存管理
        binding.btnClearCache.setOnClickListener {
//            clearCache()
            showCurrentLoadingStyle()
        }
        
        binding.btnShowCacheStatus.setOnClickListener {
            showCacheStatus()
        }
        
        // Loading 样式配置
        // 注意：如果需要在 UI 中切换 Loading 样式，请在布局文件中添加对应的按钮
        // 或者通过代码直接调用以下方法：
        // - setLoadingStyleDefault() - 设置为默认样式
        // - setLoadingStyleDark() - 设置为深色模式
        // - setLoadingStyleMinimal() - 设置为简约模式
        // - setLoadingStyleLarge() - 设置为大尺寸模式
        // - showCustomLoadingStyleDialog() - 显示自定义样式对话框
        // - showCurrentLoadingStyle() - 显示当前样式信息
    }

    private fun updateCachePolicyDisplay() {
        val policyName = when (currentCachePolicy) {
            CachePolicy.NO_CACHE -> "NO_CACHE"
            CachePolicy.CACHE_ONLY -> "CACHE_ONLY"
            CachePolicy.CACHE_FIRST -> "CACHE_FIRST"
            CachePolicy.NETWORK_FIRST -> "NETWORK_FIRST"
            CachePolicy.CACHE_AND_NETWORK -> "CACHE_AND_NETWORK"
        }
        val policyDesc = when (currentCachePolicy) {
            CachePolicy.NO_CACHE -> "不使用缓存，直接请求网络"
            CachePolicy.CACHE_ONLY -> "只使用缓存，不发起网络请求"
            CachePolicy.CACHE_FIRST -> "优先使用缓存，缓存不存在时请求网络"
            CachePolicy.NETWORK_FIRST -> "优先请求网络，失败时使用缓存"
            CachePolicy.CACHE_AND_NETWORK -> "先返回缓存，后台更新网络数据"
        }
        adapter.submitList(
            listOf(
                NetworkDemoItem(
                    title = "当前缓存策略",
                    result = "$policyName\n$policyDesc",
                    code = "cachePolicy = CachePolicy.$policyName"
                )
            )
        )
    }
    
    private fun updateLoadingStyleDisplay() {
        val styleName = when (currentLoadingConfig) {
            null -> "默认样式"
            LoadingDialogConfig.DARK -> "深色模式"
            LoadingDialogConfig.MINIMAL -> "简约模式"
            LoadingDialogConfig.LARGE -> "大尺寸模式"
            else -> "自定义样式"
        }
        val styleDesc = when (currentLoadingConfig) {
            null -> "使用框架默认的 Loading 样式"
            LoadingDialogConfig.DARK -> "深色背景，适合深色主题"
            LoadingDialogConfig.MINIMAL -> "小尺寸，无消息文本"
            LoadingDialogConfig.LARGE -> "大尺寸，适合重要操作"
            else -> "自定义配置的 Loading 样式"
        }
        val configCode = when (currentLoadingConfig) {
            null -> "null"
            LoadingDialogConfig.DARK -> "LoadingDialogConfig.DARK"
            LoadingDialogConfig.MINIMAL -> "LoadingDialogConfig.MINIMAL"
            LoadingDialogConfig.LARGE -> "LoadingDialogConfig.LARGE"
            else -> "LoadingDialogConfig(...)"
        }
        // 创建新的列表对象，确保 DiffUtil 能检测到变化
        val newList = listOf(
            NetworkDemoItem(
                title = "当前 Loading 样式",
                result = "$styleName\n$styleDesc",
                code = "loadingConfig = $configCode"
            )
        )
        adapter.submitList(newList)
    }

    private fun showInitialMessage() {
        adapter.submitList(
            listOf(
                NetworkDemoItem(
                    title = "网络请求示例",
                    result = "1. 选择缓存策略（默认: NETWORK_FIRST）\n2. 点击网络请求按钮发起请求\n3. 查看请求结果和缓存效果\n4. 使用缓存管理功能",
                    code = "使用 NetworkClient 和 requestConfig DSL 进行网络请求\n支持缓存策略、错误处理和请求配置"
                )
            )
        )
    }

    /**
     * 通用网络请求处理方法（带缓存）
     * 使用 NetworkClient 和 requestConfig DSL 实现缓存策略
     * 注意：使用 inline reified 来保留泛型类型信息，确保 Moshi 能正确解析泛型类型
     */
    private inline fun <reified T> executeRequestWithCache(
        loadingMessage: String,
        successMessage: String,
        url: String,
        cacheKey: String,
        cache: NetworkCache<String, Any>?,
        pathParams: Map<String, String> = emptyMap(),
        queryParams: Map<String, String> = emptyMap(),
        noinline onSuccess: (T, String) -> Unit
    ) {
        lifecycleScope.launch {
            // 构建请求配置
            val config = requestConfig {
                // 路径参数
                pathParams.forEach { (key, value) ->
                    pathParam(key, value)
                }
                // 查询参数
                queryParams.forEach { (key, value) ->
                    queryParam(key, value)
                }
                // 缓存配置
                if (cache != null) {
                    this.cache(cache)
                    cacheKey(cacheKey)
                    cachePolicy(currentCachePolicy)
                }
                // 显示 Loading 对话框（自动管理显示和隐藏）
                showLoading(loadingMessage, config = currentLoadingConfig)
            }
            
            // 使用 NetworkClient 发起请求
            // 注意：这里使用 WanAndroidResponse<T>，其中 T 是 reified 类型参数
            // 这样 NetworkClient 就能通过 typeOf<T>() 获取完整的类型信息（包括泛型参数）
            val result = networkClient.get<WanAndroidResponse<T>>(
                url = url,
                config = config
            )
            
            result.onSuccess { wanResponse ->
                if (wanResponse.errorCode == 0) {
                    val data = wanResponse.data
                    if (data != null) {
                        // 判断数据来源
                        val source = when {
                            cache == null -> "网络"
                            currentCachePolicy == CachePolicy.CACHE_ONLY -> "缓存"
                            currentCachePolicy == CachePolicy.CACHE_FIRST -> {
                                // 简化处理：如果缓存中有数据，可能是从缓存获取的
                                if (cache.get(cacheKey) != null) "缓存" else "网络"
                            }
                            currentCachePolicy == CachePolicy.CACHE_AND_NETWORK -> {
                                // CACHE_AND_NETWORK 先返回缓存，后台更新
                                if (cache.get(cacheKey) != null) "缓存（后台更新中）" else "网络"
                            }
                            else -> "网络"
                        }
                        onSuccess(data, source)
                    } else {
                        showError("数据为空")
                        clearResults()
                    }
                } else {
                    showError("请求失败: ${wanResponse.errorMsg}")
                    clearResults()
                }
            }.onError { error ->
                // 详细的错误处理
                val errorDetail = formatErrorDetail(error)
                showError("请求失败: $errorDetail")
                clearResults()
            }
        }
    }

    /**
     * 通用网络请求处理方法（不带缓存，用于演示错误处理）
     * 使用 NetworkClient 和 requestConfig DSL
     * 注意：使用 inline reified 来保留泛型类型信息，确保 Moshi 能正确解析泛型类型
     */
    private inline fun <reified T> executeRequest(
        loadingMessage: String,
        successMessage: String,
        url: String,
        pathParams: Map<String, String> = emptyMap(),
        queryParams: Map<String, String> = emptyMap(),
        noinline onSuccess: (T) -> Unit
    ) {
        lifecycleScope.launch {
            // 构建请求配置
            val config = requestConfig {
                // 路径参数
                pathParams.forEach { (key, value) ->
                    pathParam(key, value)
                }
                // 查询参数
                queryParams.forEach { (key, value) ->
                    queryParam(key, value)
                }
                // 显示 Loading 对话框（自动管理显示和隐藏）
                showLoading(loadingMessage, config = currentLoadingConfig)
            }
            
            // 使用 NetworkClient 发起请求
            // 注意：这里使用 WanAndroidResponse<T>，其中 T 是 reified 类型参数
            // 这样 NetworkClient 就能通过 typeOf<T>() 获取完整的类型信息（包括泛型参数）
            val result = networkClient.get<WanAndroidResponse<T>>(
                url = url,
                config = config
            )
            
            result.onSuccess { wanResponse ->
                if (wanResponse.errorCode == 0) {
                    val data = wanResponse.data
                    if (data != null) {
                        onSuccess(data)
                    } else {
                        showError("数据为空")
                        clearResults()
                    }
                } else {
                    showError("请求失败: ${wanResponse.errorMsg}")
                    clearResults()
                }
            }.onError { error ->
                val errorDetail = formatErrorDetail(error)
                showError("请求失败: $errorDetail")
                clearResults()
            }
        }
    }

    /**
     * 格式化错误详情
     * 根据文档中的错误处理指南，区分不同类型的错误
     */
    private fun formatErrorDetail(error: AppResult.Error): String {
        val exception = error.exception
        val errorMessage = error.errorMessage
        
        return when {
            // JSON 数据类型不匹配
            exception?.javaClass?.simpleName == "JsonDataException" ||
            errorMessage.contains("JsonDataException") ||
            errorMessage.contains("数据格式错误") ||
            errorMessage.contains("数据类型与预期不符") -> {
                "数据格式错误: 服务端返回的数据类型与预期不符\n详情: ${exception?.message ?: errorMessage}"
            }
            // JSON 编码错误
            exception?.javaClass?.simpleName == "JsonEncodingException" ||
            errorMessage.contains("JsonEncodingException") ||
            errorMessage.contains("JSON 编码错误") -> {
                "JSON 编码错误: 响应格式不正确\n详情: ${exception?.message ?: errorMessage}"
            }
            // 请求超时
            exception?.message?.contains("timeout", ignoreCase = true) == true ||
            errorMessage.contains("timeout", ignoreCase = true) -> {
                "请求超时，请检查网络连接\n详情: ${exception?.message ?: errorMessage}"
            }
            // 网络不可达
            exception?.message?.contains("Unable to resolve host", ignoreCase = true) == true ||
            exception?.message?.contains("Network is unreachable", ignoreCase = true) == true ||
            errorMessage.contains("网络不可达", ignoreCase = true) -> {
                "网络不可达，请检查网络连接\n详情: ${exception?.message ?: errorMessage}"
            }
            // 缓存不存在
            errorMessage.contains("缓存不存在") -> {
                "缓存不存在，请先使用其他策略获取数据\n提示: 可以尝试使用 CACHE_FIRST 或 NETWORK_FIRST 策略"
            }
            // 其他错误
            else -> {
                "${errorMessage}\n${exception?.message?.takeIf { it.isNotBlank() }?.let { "详情: $it" } ?: ""}"
            }
        }
    }

    private fun clearResults() {
        adapter.submitList(emptyList())
    }

    private fun getArticleList() {
        executeRequestWithCache(
            loadingMessage = "正在请求文章列表...",
            successMessage = "获取文章列表成功",
            url = "/article/list/{page}/json",
            cacheKey = "article_list_0",
            cache = articleListCache,
            pathParams = mapOf("page" to "0"),
            onSuccess = { articleList: ArticleListResponse, source ->
                val items = mutableListOf<NetworkDemoItem>().apply {
                    add(
                        NetworkDemoItem(
                            title = "📄 文章列表（第${articleList.curPage}页）",
                            result = "共 ${articleList.total} 条，当前页 ${articleList.datas.size} 条 | 来源: $source",
                            code = "networkClient.get<WanAndroidResponse<ArticleListResponse>>(\n    url = \"/article/list/{page}/json\",\n    config = requestConfig {\n        pathParam(\"page\", \"0\")\n        cache(cache)\n        cacheKey(\"article_list_0\")\n        cachePolicy(CachePolicy.${currentCachePolicy.name})\n    }\n)"
                        )
                    )
                    // 添加前5条文章标题
                    articleList.datas.take(5).forEachIndexed { index, article ->
                        add(
                            NetworkDemoItem(
                                title = "${index + 1}. ${article.title ?: "无标题"}",
                                result = "作者: ${article.author ?: article.shareUser ?: "未知"} | ${article.niceDate}",
                                code = article.link ?: ""
                            )
                        )
                    }
                    if (articleList.datas.size > 5) {
                        add(
                            NetworkDemoItem(
                                title = "...",
                                result = "还有 ${articleList.datas.size - 5} 条未显示",
                                code = ""
                            )
                        )
                    }
                }
                adapter.submitList(items)
            }
        )
    }

    private fun getBanner() {
        executeRequestWithCache(
            loadingMessage = "正在请求Banner...",
            successMessage = "获取Banner成功",
            url = "/banner/json",
            cacheKey = "banner_list",
            cache = bannerCache,
            onSuccess = { banners: List<Banner>, source ->
                val items = mutableListOf<NetworkDemoItem>().apply {
                    add(
                        NetworkDemoItem(
                            title = "🎨 Banner列表",
                            result = "共 ${banners.size} 个Banner | 来源: $source",
                            code = "networkClient.get<WanAndroidResponse<List<Banner>>>(\n    url = \"/banner/json\",\n    config = requestConfig {\n        cache(cache)\n        cacheKey(\"banner_list\")\n        cachePolicy(CachePolicy.${currentCachePolicy.name})\n    }\n)"
                        )
                    )
                    banners.forEachIndexed { index, banner ->
                        add(
                            NetworkDemoItem(
                                title = "${index + 1}. ${banner.title ?: "无标题"}",
                                result = banner.desc ?: "无描述",
                                code = banner.url ?: ""
                            )
                        )
                    }
                }
                adapter.submitList(items)
            }
        )
    }

    private fun getFriend() {
        executeRequest(
            loadingMessage = "正在请求常用网站...",
            successMessage = "获取常用网站成功",
            url = "/friend/json"
        ) { friends: List<Friend> ->
            val items = mutableListOf<NetworkDemoItem>().apply {
                add(
                    NetworkDemoItem(
                        title = "🔗 常用网站列表",
                        result = "共 ${friends.size} 个网站",
                        code = "networkClient.get<WanAndroidResponse<List<Friend>>>(\n    url = \"/friend/json\"\n)"
                    )
                )
                friends.take(10).forEachIndexed { index, friend ->
                    add(
                        NetworkDemoItem(
                            title = "${index + 1}. ${friend.name ?: "无名称"}",
                            result = friend.link ?: "无链接",
                            code = friend.link ?: ""
                        )
                    )
                }
                if (friends.size > 10) {
                    add(
                        NetworkDemoItem(
                            title = "...",
                            result = "还有 ${friends.size - 10} 个网站未显示",
                            code = ""
                        )
                    )
                }
            }
            adapter.submitList(items)
        }
    }

    private fun getHotKey() {
        executeRequest(
            loadingMessage = "正在请求搜索热词...",
            successMessage = "获取搜索热词成功",
            url = "/hotkey/json"
        ) { hotKeys: List<HotKey> ->
            val items = mutableListOf<NetworkDemoItem>().apply {
                add(
                    NetworkDemoItem(
                        title = "🔥 搜索热词列表",
                        result = "共 ${hotKeys.size} 个热词",
                        code = "networkClient.get<WanAndroidResponse<List<HotKey>>>(\n    url = \"/hotkey/json\"\n)"
                    )
                )
                hotKeys.take(10).forEachIndexed { index, hotKey ->
                    add(
                        NetworkDemoItem(
                            title = "${index + 1}. ${hotKey.name ?: "无名称"}",
                            result = "排序: ${hotKey.order}",
                            code = hotKey.link ?: ""
                        )
                    )
                }
                if (hotKeys.size > 10) {
                    add(
                        NetworkDemoItem(
                            title = "...",
                            result = "还有 ${hotKeys.size - 10} 个热词未显示",
                            code = ""
                        )
                    )
                }
            }
            adapter.submitList(items)
        }
    }

    private fun getTopArticles() {
        executeRequest(
            loadingMessage = "正在请求置顶文章...",
            successMessage = "获取置顶文章成功",
            url = "/article/top/json"
        ) { articles: List<Article> ->
            val items = mutableListOf<NetworkDemoItem>().apply {
                add(
                    NetworkDemoItem(
                        title = "⭐ 置顶文章列表",
                        result = "共 ${articles.size} 篇置顶文章",
                        code = "networkClient.get<WanAndroidResponse<List<Article>>>(\n    url = \"/article/top/json\"\n)"
                    )
                )
                articles.take(10).forEachIndexed { index, article ->
                    add(
                        NetworkDemoItem(
                            title = "${index + 1}. ${article.title ?: "无标题"}",
                            result = "作者: ${article.author ?: article.shareUser ?: "未知"} | ${article.niceDate}",
                            code = article.link ?: ""
                        )
                    )
                }
                if (articles.size > 10) {
                    add(
                        NetworkDemoItem(
                            title = "...",
                            result = "还有 ${articles.size - 10} 篇置顶文章未显示",
                            code = ""
                        )
                    )
                }
            }
            adapter.submitList(items)
        }
    }

    private fun getTree() {
        executeRequest(
            loadingMessage = "正在请求体系数据...",
            successMessage = "获取体系数据成功",
            url = "/tree/json"
        ) { trees: List<Tree> ->
            val items = mutableListOf<NetworkDemoItem>().apply {
                add(
                    NetworkDemoItem(
                        title = "🌳 体系数据",
                        result = "共 ${trees.size} 个一级分类",
                        code = "networkClient.get<WanAndroidResponse<List<Tree>>>(\n    url = \"/tree/json\"\n)"
                    )
                )
                trees.take(10).forEachIndexed { index, tree ->
                    val childrenCount = tree.children.size
                    add(
                        NetworkDemoItem(
                            title = "${index + 1}. ${tree.name ?: "无名称"}",
                            result = "子分类数: $childrenCount",
                            code = "ID: ${tree.id}"
                        )
                    )
                }
                if (trees.size > 10) {
                    add(
                        NetworkDemoItem(
                            title = "...",
                            result = "还有 ${trees.size - 10} 个一级分类未显示",
                            code = ""
                        )
                    )
                }
            }
            adapter.submitList(items)
        }
    }
    
    /**
     * 切换 BaseUrl
     */
    private fun switchBaseUrl(newBaseUrl: String, envName: String) {
        try {
            interceptorManager.switchBaseUrl(newBaseUrl)
            val currentBaseUrl = interceptorManager.getCurrentBaseUrl() ?: "未知"
            showSuccess("已切换到 $envName\n当前 BaseUrl: $currentBaseUrl")
            
            // 显示当前 BaseUrl 信息
            adapter.submitList(
                listOf(
                    NetworkDemoItem(
                        title = "🌐 BaseUrl 切换",
                        result = "环境: $envName\nBaseUrl: $currentBaseUrl\n\n提示：后续所有网络请求将使用新的 BaseUrl",
                        code = "interceptorManager.switchBaseUrl(\"$newBaseUrl\")"
                    )
                )
            )
        } catch (e: Exception) {
            showError("切换 BaseUrl 失败: ${e.message}")
        }
    }
    
    /**
     * 显示当前 BaseUrl
     */
    private fun showCurrentBaseUrl() {
        val currentBaseUrl = interceptorManager.getCurrentBaseUrl() ?: "未配置"
        val hasInterceptor = interceptorManager.hasBaseUrlInterceptor()
        
        adapter.submitList(
            listOf(
                NetworkDemoItem(
                    title = "🌐 当前 BaseUrl",
                    result = if (hasInterceptor) {
                        "BaseUrl: $currentBaseUrl\n状态: 已配置 BaseUrlInterceptor\n\n提示：可以通过上方按钮切换 BaseUrl"
                    } else {
                        "BaseUrl: $currentBaseUrl\n状态: 未配置 BaseUrlInterceptor\n\n提示：BaseUrlInterceptor 未注册，无法动态切换"
                    },
                    code = "interceptorManager.getCurrentBaseUrl() = \"$currentBaseUrl\""
                )
            )
        )
        showMessage("当前 BaseUrl: $currentBaseUrl")
    }
    
    /**
     * 清空缓存
     */
    private fun clearCache() {
        lifecycleScope.launch {
            articleListCache.clear()
            bannerCache.clear()
            showSuccess("缓存已清空")
            adapter.submitList(
                listOf(
                    NetworkDemoItem(
                        title = "缓存管理",
                        result = "所有缓存已清空\n文章列表缓存: 0 条\nBanner缓存: 0 条",
                        code = "cache.clear()"
                    )
                )
            )
        }
    }
    
    /**
     * 显示缓存状态
     */
    private fun showCacheStatus() {
        lifecycleScope.launch {
            val articleListSize = articleListCache.size()
            val bannerSize = bannerCache.size()
            
            val items = mutableListOf<NetworkDemoItem>().apply {
                add(
                    NetworkDemoItem(
                        title = "📊 缓存状态",
                        result = "文章列表缓存: $articleListSize 条\nBanner缓存: $bannerSize 条\n当前策略: ${currentCachePolicy.name}",
                        code = "cache.size()"
                    )
                )
                if (articleListSize > 0) {
                    add(
                        NetworkDemoItem(
                            title = "文章列表缓存",
                            result = "缓存键: article_list_0\n缓存数量: $articleListSize",
                            code = "articleListCache.size() = $articleListSize"
                        )
                    )
                }
                if (bannerSize > 0) {
                    add(
                        NetworkDemoItem(
                            title = "Banner缓存",
                            result = "缓存键: banner_list\n缓存数量: $bannerSize",
                            code = "bannerCache.size() = $bannerSize"
                        )
                    )
                }
                if (articleListSize == 0 && bannerSize == 0) {
                    add(
                        NetworkDemoItem(
                            title = "提示",
                            result = "当前没有缓存数据\n建议：\n1. 使用 CACHE_FIRST 或 NETWORK_FIRST 策略获取数据\n2. 数据会自动缓存\n3. 然后可以使用 CACHE_ONLY 策略查看缓存效果",
                            code = ""
                        )
                    )
                }
            }
            adapter.submitList(items)
        }
    }
    
    /**
     * 显示自定义 Loading 样式对话框
     */
    private fun showCustomLoadingStyleDialog() {
        // 创建自定义配置
        val customConfig = LoadingDialogConfig(
            overlayColor = Color.parseColor("#60000000"),
            backgroundColor = Color.parseColor("#FF6B9E"),
            cornerRadius = 20f,
            elevation = 16f,
            progressBarSize = 64,
            progressBarColor = Color.WHITE,
            messageTextSize = 16f,
            messageTextColor = Color.WHITE,
            padding = 48,
            progressBarMessageSpacing = 24,
            minWidth = 160,
            defaultMessage = "自定义加载中..."
        )
        
        currentLoadingConfig = customConfig
        showMessage("已选择 Loading 样式: 自定义样式（粉色主题）")
        updateLoadingStyleDisplay()
        
        // 显示配置详情
        adapter.submitList(
            listOf(
                NetworkDemoItem(
                    title = "🎨 自定义 Loading 样式",
                    result = "背景色: 粉色 (#FF6B9E)\n圆角: 20dp\nProgressBar: 64dp (白色)\n文字: 16sp (白色)\n内边距: 48dp",
                    code = """LoadingDialogConfig(
    overlayColor = Color.parseColor("#60000000"),
    backgroundColor = Color.parseColor("#FF6B9E"),
    cornerRadius = 20f,
    progressBarSize = 64,
    progressBarColor = Color.WHITE,
    messageTextColor = Color.WHITE,
    padding = 48
)"""
                )
            )
        )
    }
    
    /**
     * 显示当前 Loading 样式信息
     */
    private fun showCurrentLoadingStyle() {
        val config = currentLoadingConfig ?: LoadingDialogConfig.DEFAULT
        val styleName = when (currentLoadingConfig) {
            null -> "默认样式"
            LoadingDialogConfig.DARK -> "深色模式"
            LoadingDialogConfig.MINIMAL -> "简约模式"
            LoadingDialogConfig.LARGE -> "大尺寸模式"
            else -> "自定义样式"
        }
        
        val items = mutableListOf<NetworkDemoItem>().apply {
            add(
                NetworkDemoItem(
                    title = "🎨 当前 Loading 样式",
                    result = "样式名称: $styleName\n\n配置详情：\n遮罩颜色: #${Integer.toHexString(config.overlayColor).uppercase()}\n背景颜色: #${Integer.toHexString(config.backgroundColor).uppercase()}\n圆角半径: ${config.cornerRadius}dp\n阴影高度: ${config.elevation}dp\nProgressBar 尺寸: ${config.progressBarSize}dp\n文字大小: ${config.messageTextSize}sp\n内边距: ${config.padding}dp\n最小宽度: ${config.minWidth}dp\n显示消息: ${if (config.showMessage) "是" else "否"}",
                    code = "loadingConfig = ${if (currentLoadingConfig == null) "null" else "LoadingDialogConfig(...)"}"
                )
            )
            if (currentLoadingConfig != null && currentLoadingConfig != LoadingDialogConfig.DEFAULT && 
                currentLoadingConfig != LoadingDialogConfig.DARK && 
                currentLoadingConfig != LoadingDialogConfig.MINIMAL && 
                currentLoadingConfig != LoadingDialogConfig.LARGE) {
                add(
                    NetworkDemoItem(
                        title = "自定义配置代码",
                        result = "可以在代码中使用此配置",
                        code = """LoadingDialogConfig(
    overlayColor = Color.parseColor("#${Integer.toHexString(config.overlayColor).uppercase()}"),
    backgroundColor = Color.parseColor("#${Integer.toHexString(config.backgroundColor).uppercase()}"),
    cornerRadius = ${config.cornerRadius}f,
    elevation = ${config.elevation}f,
    progressBarSize = ${config.progressBarSize},
    messageTextSize = ${config.messageTextSize}f,
    padding = ${config.padding},
    minWidth = ${config.minWidth},
    showMessage = ${config.showMessage}
)"""
                    )
                )
            }
        }
        adapter.submitList(items)
        showMessage("当前 Loading 样式: $styleName")
    }

}

data class NetworkDemoItem(
    val title: String,
    val result: String,
    val code: String
)
