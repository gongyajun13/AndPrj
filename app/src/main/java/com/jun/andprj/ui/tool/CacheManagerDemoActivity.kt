package com.jun.andprj.ui.tool

import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.jun.andprj.R
import com.jun.andprj.databinding.ActivityCacheManagerDemoBinding
import com.jun.core.common.util.CacheManager
import com.jun.core.common.util.MemoryCacheManager
import com.jun.core.ui.base.BaseActivity
import kotlinx.coroutines.launch

/**
 * CacheManager 工具类示例
 */
class CacheManagerDemoActivity : BaseActivity<ActivityCacheManagerDemoBinding>() {

    private lateinit var adapter: CacheManagerDemoAdapter
    private val demoItems = mutableListOf<CacheManagerDemoItem>()
    private val cacheManager: CacheManager = MemoryCacheManager()

    override fun createBinding(): ActivityCacheManagerDemoBinding = ActivityCacheManagerDemoBinding.inflate(layoutInflater)

    override fun setupViews() {
        setupToolbar()
        setupRecyclerView()
        setupButtonListeners()
        refreshCacheStatus()
    }

    private fun setupToolbar() {
        val white = ContextCompat.getColor(this, android.R.color.white)
        val blue = ContextCompat.getColor(this, R.color.blue)
        setStatusBarColor(white, lightIcons = false)
        binding.toolbar.setupSimple(
            leftIcon = R.drawable.icon_back,
            leftIconTint = white,
            title = "缓存工具示例",
            titleTextColor = white,
            backgroundColor = blue,
            onLeftClick = { finish() }
        )
    }

    private fun setupRecyclerView() {
        adapter = CacheManagerDemoAdapter(demoItems)
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
    }

    private fun setupButtonListeners() {
        binding.btnPutString.setOnClickListener {
            lifecycleScope.launch {
                cacheManager.put("test_string", "Hello Cache", 0)
                showSuccess("已缓存字符串：Hello Cache")
                refreshCacheStatus()
            }
        }

        binding.btnPutStringWithTtl.setOnClickListener {
            lifecycleScope.launch {
                cacheManager.put("test_string_ttl", "Hello Cache (TTL: 5s)", 5000)
                showSuccess("已缓存字符串（5秒过期）：Hello Cache (TTL: 5s)")
                refreshCacheStatus()
            }
        }

        binding.btnPutInt.setOnClickListener {
            lifecycleScope.launch {
                cacheManager.put("test_int", 12345, 0)
                showSuccess("已缓存整数：12345")
                refreshCacheStatus()
            }
        }

        binding.btnPutList.setOnClickListener {
            lifecycleScope.launch {
                cacheManager.put("test_list", listOf("A", "B", "C"), 0)
                showSuccess("已缓存列表：[A, B, C]")
                refreshCacheStatus()
            }
        }

        binding.btnGetString.setOnClickListener {
            lifecycleScope.launch {
                val value: String? = cacheManager.get("test_string")
                if (value != null) {
                    showSuccess("获取缓存：$value")
                } else {
                    showWarning("缓存不存在或已过期")
                }
                refreshCacheStatus()
            }
        }

        binding.btnGetInt.setOnClickListener {
            lifecycleScope.launch {
                val value: Int? = cacheManager.get("test_int")
                if (value != null) {
                    showSuccess("获取缓存：$value")
                } else {
                    showWarning("缓存不存在或已过期")
                }
                refreshCacheStatus()
            }
        }

        binding.btnContains.setOnClickListener {
            lifecycleScope.launch {
                val exists = cacheManager.contains("test_string")
                if (exists) {
                    showSuccess("缓存存在")
                } else {
                    showWarning("缓存不存在或已过期")
                }
                refreshCacheStatus()
            }
        }

        binding.btnRemove.setOnClickListener {
            lifecycleScope.launch {
                cacheManager.remove("test_string")
                showSuccess("已移除缓存：test_string")
                refreshCacheStatus()
            }
        }

        binding.btnClear.setOnClickListener {
            lifecycleScope.launch {
                cacheManager.clear()
                showSuccess("已清空所有缓存")
                refreshCacheStatus()
            }
        }
    }

    private fun refreshCacheStatus() {
        lifecycleScope.launch {
            val testString: String? = cacheManager.get("test_string")
            val testStringTtl: String? = cacheManager.get("test_string_ttl")
            val testInt: Int? = cacheManager.get("test_int")
            val testList: List<String>? = cacheManager.get("test_list")

            demoItems.clear()
            demoItems.addAll(
                listOf(
                    CacheManagerDemoItem(
                        "test_string",
                        testString ?: "（不存在）",
                        "cacheManager.get<String>(\"test_string\")"
                    ),
                    CacheManagerDemoItem(
                        "test_string_ttl",
                        testStringTtl ?: "（不存在或已过期）",
                        "cacheManager.get<String>(\"test_string_ttl\")"
                    ),
                    CacheManagerDemoItem(
                        "test_int",
                        testInt?.toString() ?: "（不存在）",
                        "cacheManager.get<Int>(\"test_int\")"
                    ),
                    CacheManagerDemoItem(
                        "test_list",
                        testList?.toString() ?: "（不存在）",
                        "cacheManager.get<List<String>>(\"test_list\")"
                    )
                )
            )
            adapter.notifyDataSetChanged()
        }
    }
}

data class CacheManagerDemoItem(
    val key: String,
    val value: String,
    val code: String
)

