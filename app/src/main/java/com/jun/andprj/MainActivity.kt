package com.jun.andprj

import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.viewpager2.widget.ViewPager2
import com.jun.andprj.databinding.ActivityMainBinding
import com.jun.andprj.ui.home.HomePagerAdapter
import com.jun.andprj.ui.user.UserViewModel
import com.jun.core.ui.base.BaseActivity
import com.jun.core.ui.widget.BottomTabBar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : BaseActivity<ActivityMainBinding>() {

    private val viewModel: UserViewModel by viewModels()
    private lateinit var pagerAdapter: HomePagerAdapter

    override fun createBinding(): ActivityMainBinding =
        ActivityMainBinding.inflate(layoutInflater)

    override fun setupViews() {
        // 状态栏白底黑字
        val white = ContextCompat.getColor(this, android.R.color.black)
        setStatusBarColor(white, lightIcons = true)

        setupViewPager()
        setupBottomTabs()
    }

    override fun setupObservers() {
        // 当前首页 ViewPager 各 Tab 暂无数据绑定，后续按需扩展
    }

    private fun setupViewPager() {
        pagerAdapter = HomePagerAdapter(this)

        binding.viewpager.adapter = pagerAdapter

        // 禁用 ViewPager2 的左右滑动
        binding.viewpager.isUserInputEnabled = false

        binding.viewpager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                binding.bottomTabBar.selectTab(position)
            }
        })
    }

    private fun setupBottomTabs() {
        val items = listOf(
            BottomTabBar.BottomTabItem(
                id = 0,
                iconRes = R.drawable.icon_tab_home,
                title = "视图"
            ),
            BottomTabBar.BottomTabItem(
                id = 1,
                iconRes = R.drawable.icon_tab_func,
                title = "功能"
            ),
            BottomTabBar.BottomTabItem(
                id = 2,
                iconRes = R.drawable.icon_tab_tool,
                title = "工具"
            ),
            BottomTabBar.BottomTabItem(
                id = 3,
                iconRes = R.drawable.icon_tab_mine,
                title = "我的",
                badgeCount = 100
            )
        )

        binding.bottomTabBar.apply {
            setItems(items)
            setOnTabSelectedListener { index, _ ->
                binding.viewpager.setCurrentItem(index, false)
            }
            selectTab(0)
        }
        // 默认选中第一个页面
        binding.viewpager.setCurrentItem(0, false)
    }
}