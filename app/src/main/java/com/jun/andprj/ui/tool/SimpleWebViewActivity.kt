package com.jun.andprj.ui.tool

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.jun.andprj.R
import com.jun.andprj.databinding.ActivitySimpleWebViewBinding
import com.jun.core.common.config.WebViewConfig
import com.jun.core.ui.webview.BaseWebViewActivity

/**
 * 超轻量通用 WebView 页面
 *
 * 特点：
 * - 仅加载指定 URL
 * - 简单标题栏 + 返回按钮
 * - 无 JS Bridge、无自定义协议扩展（仅使用基类默认行为）
 *
 * 适合用在：
 * - 隐私政策 / 用户协议 / 帮助中心 等简单展示页
 */
class SimpleWebViewActivity : BaseWebViewActivity<ActivitySimpleWebViewBinding>() {

    companion object {
        private const val EXTRA_URL = "extra_url"
        private const val EXTRA_TITLE = "extra_title"

        /**
         * 对外简易启动方法
         *
         * @param context 上下文
         * @param url 要加载的网页地址
         * @param title 页面标题（可选，默认“网页”）
         */
        fun start(context: Context, url: String, title: String? = null) {
            val intent = Intent(context, SimpleWebViewActivity::class.java).apply {
                putExtra(EXTRA_URL, url)
                putExtra(EXTRA_TITLE, title)
            }
            context.startActivity(intent)
        }
    }

    override fun createBinding(): ActivitySimpleWebViewBinding =
        ActivitySimpleWebViewBinding.inflate(layoutInflater)

    override fun getWebViewContainer() = binding.webViewContainer

    override fun getWebViewConfig(): WebViewConfig = WebViewConfig.default()

    override fun getInitialUrl(): String? = intent.getStringExtra(EXTRA_URL)

    override fun setupViews() {
        super.setupViews()
        setupToolbar()
        setupStatusBar()
    }

    private fun setupToolbar() {
        val white = ContextCompat.getColor(this, android.R.color.white)
        val title = intent.getStringExtra(EXTRA_TITLE).orEmpty().ifBlank { "网页" }

        binding.toolbar.setupSimple(
            leftIcon = R.drawable.icon_back,
            leftIconTint = white,
            title = title,
            titleTextColor = white,
            backgroundColor = ContextCompat.getColor(this, R.color.blue),
            onLeftClick = { finish() }
        )
    }

    private fun setupStatusBar() {
        // 白色状态栏，暗色图标
        val white = ContextCompat.getColor(this, android.R.color.white)
        setStatusBarColor(white, lightIcons = false)
    }
}




