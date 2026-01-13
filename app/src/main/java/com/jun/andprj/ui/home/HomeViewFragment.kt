package com.jun.andprj.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.jun.andprj.databinding.FragmentHomeViewBinding
import com.jun.andprj.ui.notify.NotifyDemoActivity
import com.jun.andprj.ui.recycler.RecyclerLayoutActivity
import com.jun.andprj.ui.tool.WebViewDemoActivity
import com.jun.core.common.result.AppResult
import com.jun.core.ui.base.ActivityManager
import com.jun.core.ui.base.BaseFragment

/**
 * 首页 Tab：视图
 */
class HomeViewFragment : BaseFragment<FragmentHomeViewBinding>() {

    override fun createBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentHomeViewBinding = FragmentHomeViewBinding.inflate(inflater, container, false)

    private val demoItems = mutableListOf(
        HomeViewItem("布局示例", "展示线性布局、网格布局、瀑布流布局三种布局切换"),
        HomeViewItem("吸顶效果", "示例：标题吸顶 + 多类型布局"),
        HomeViewItem("Banner 轮播", "展示首页轮播图的封装用法"),
        HomeViewItem("消息提示示例", "展示 Snackbar 和 Toast 的 8 种提示方式"),
        HomeViewItem("WebView 示例", "演示统一封装的 WebView（优先 X5 内核，失败回退系统 WebView）")
    )

    private lateinit var adapter: HomeViewAdapter

    override fun setupViews() {
        // 初始化列表
        adapter = HomeViewAdapter(demoItems) { item ->
            when (item.title) {
                "布局示例" -> {
                    // 跳转到布局示例页面
                    ActivityManager.startActivity<RecyclerLayoutActivity>(requireContext())
                }
                "消息提示示例" -> {
                    // 跳转到消息提示示例页面
                    ActivityManager.startActivity<NotifyDemoActivity>(requireContext())
                }
                "WebView 示例" -> {
                    // 跳转到 WebView 示例页面
                    ActivityManager.startActivity<WebViewDemoActivity>(requireContext())
                }
                else -> {
                    showMessage("点击示例：「${item.title}」后续可跳转到对应 Demo 页面")
                }
            }
        }

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        // 初始展示内容
        binding.stateLayout.showContent()

        // 下拉刷新
        binding.refreshLayout.setOnRefreshListener {
            // 模拟刷新：这里可以接入真实网络请求，返回后更新数据并结束刷新
            binding.recyclerView.postDelayed({
                // 简单示例：在列表前面插入一条“最新示例”
                demoItems.add(
                    0,
                    HomeViewItem("新的示例", "这是一次刷新后新增的示例项")
                )
                adapter.notifyDataSetChanged()
                binding.refreshLayout.finishRefresh()
                showSuccess("刷新完成")
            }, 800)
        }

        // 上拉加载更多（示例：追加几条虚拟数据）
        binding.refreshLayout.setOnLoadMoreListener {
            binding.recyclerView.postDelayed({
                val start = demoItems.size
                repeat(3) { index ->
                    demoItems.add(
                        HomeViewItem(
                            title = "更多示例 ${start + index + 1}",
                            description = "用于演示加载更多的示例项"
                        )
                    )
                }
                adapter.notifyDataSetChanged()
                binding.refreshLayout.finishLoadMore()
            }, 800)
        }
    }

    data class HomeViewItem(
        val title: String,
        val description: String
    )
}


