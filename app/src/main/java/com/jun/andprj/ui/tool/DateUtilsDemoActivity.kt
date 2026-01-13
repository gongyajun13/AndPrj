package com.jun.andprj.ui.tool

import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.jun.andprj.R
import com.jun.andprj.databinding.ActivityDateUtilsDemoBinding
import com.jun.core.common.util.DateUtils
import com.jun.core.ui.base.BaseActivity
import java.util.Date

/**
 * DateUtils 工具类示例
 */
class DateUtilsDemoActivity : BaseActivity<ActivityDateUtilsDemoBinding>() {

    private lateinit var adapter: DateUtilsDemoAdapter
    private val demoItems = mutableListOf<DateUtilsDemoItem>()

    override fun createBinding(): ActivityDateUtilsDemoBinding = ActivityDateUtilsDemoBinding.inflate(layoutInflater)

    override fun setupViews() {
        setupToolbar()
        setupRecyclerView()
        generateDemoItems()
    }

    private fun setupToolbar() {
        val white = ContextCompat.getColor(this, android.R.color.white)
        val blue = ContextCompat.getColor(this, R.color.blue)
        setStatusBarColor(white, lightIcons = false)
        binding.toolbar.setupSimple(
            leftIcon = R.drawable.icon_back,
            leftIconTint = white,
            title = "日期工具示例",
            titleTextColor = white,
            backgroundColor = blue,
            onLeftClick = { finish() }
        )
    }

    private fun setupRecyclerView() {
        adapter = DateUtilsDemoAdapter(demoItems)
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
    }

    private fun generateDemoItems() {
        val now = DateUtils.currentDate()
        val timestamp = DateUtils.currentTimestamp()
        val todayStart = DateUtils.todayStartTimestamp()
        val todayEnd = DateUtils.todayEndTimestamp()
        val yesterday = timestamp - 24 * 60 * 60 * 1000L
        val oneHourAgo = timestamp - 60 * 60 * 1000L
        val oneMinuteAgo = timestamp - 60 * 1000L

        demoItems.clear()
        demoItems.addAll(
            listOf(
                DateUtilsDemoItem(
                    "当前时间戳",
                    "${DateUtils.currentTimestamp()}",
                    "DateUtils.currentTimestamp()"
                ),
                DateUtilsDemoItem(
                    "格式化日期（标准格式）",
                    DateUtils.format(now, DateUtils.Format.DATE_TIME),
                    "DateUtils.format(date, DateUtils.Format.DATE_TIME)"
                ),
                DateUtilsDemoItem(
                    "格式化日期（中文格式）",
                    DateUtils.format(now, DateUtils.Format.DATE_TIME_CN),
                    "DateUtils.format(date, DateUtils.Format.DATE_TIME_CN)"
                ),
                DateUtilsDemoItem(
                    "格式化日期（仅日期）",
                    DateUtils.format(now, DateUtils.Format.DATE),
                    "DateUtils.format(date, DateUtils.Format.DATE)"
                ),
                DateUtilsDemoItem(
                    "格式化日期（仅时间）",
                    DateUtils.format(now, DateUtils.Format.TIME),
                    "DateUtils.format(date, DateUtils.Format.TIME)"
                ),
                DateUtilsDemoItem(
                    "格式化时间戳",
                    DateUtils.format(timestamp, DateUtils.Format.DATE_TIME),
                    "DateUtils.format(timestamp, DateUtils.Format.DATE_TIME)"
                ),
                DateUtilsDemoItem(
                    "今天开始时间戳",
                    "$todayStart",
                    "DateUtils.todayStartTimestamp()"
                ),
                DateUtilsDemoItem(
                    "今天结束时间戳",
                    "$todayEnd",
                    "DateUtils.todayEndTimestamp()"
                ),
                DateUtilsDemoItem(
                    "判断是否为今天",
                    if (DateUtils.isToday(timestamp)) "是" else "否",
                    "DateUtils.isToday(timestamp)"
                ),
                DateUtilsDemoItem(
                    "判断是否为昨天",
                    if (DateUtils.isYesterday(yesterday)) "是" else "否",
                    "DateUtils.isYesterday(timestamp)"
                ),
                DateUtilsDemoItem(
                    "相对时间（刚刚）",
                    DateUtils.getRelativeTime(oneMinuteAgo),
                    "DateUtils.getRelativeTime(oneMinuteAgo)"
                ),
                DateUtilsDemoItem(
                    "相对时间（1小时前）",
                    DateUtils.getRelativeTime(oneHourAgo),
                    "DateUtils.getRelativeTime(oneHourAgo)"
                ),
                DateUtilsDemoItem(
                    "相对时间（昨天）",
                    DateUtils.getRelativeTime(yesterday),
                    "DateUtils.getRelativeTime(yesterday)"
                ),
                DateUtilsDemoItem(
                    "解析日期字符串",
                    DateUtils.parse("2024-01-01 12:00:00", DateUtils.Format.DATE_TIME)?.let {
                        DateUtils.format(it, DateUtils.Format.DATE_TIME_CN)
                    } ?: "解析失败",
                    "DateUtils.parse(\"2024-01-01 12:00:00\", DateUtils.Format.DATE_TIME)"
                ),
                DateUtilsDemoItem(
                    "时间差（毫秒）",
                    "${DateUtils.getTimeDifference(timestamp, yesterday)}",
                    "DateUtils.getTimeDifference(timestamp1, timestamp2)"
                )
            )
        )
        adapter.notifyDataSetChanged()
    }
}

data class DateUtilsDemoItem(
    val title: String,
    val result: String,
    val code: String
)

