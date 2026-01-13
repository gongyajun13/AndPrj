package com.jun.andprj.ui.home

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.jun.core.ui.adapter.ViewPager2Adapter

/**
 * 首页 ViewPager2 适配器，对应 4 个 Tab：
 * 0: 视图, 1: 功能, 2: 工具, 3: 我的
 * 
 * 使用 core-ui 的 ViewPager2Adapter 基类，统一管理 ViewPager2 适配器
 */
class HomePagerAdapter(activity: FragmentActivity) : ViewPager2Adapter(activity) {

    override fun getItemCount(): Int = 4

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> HomeViewFragment()
            1 -> HomeFeatureFragment()
            2 -> HomeToolFragment()
            3 -> HomeMineFragment()
            else -> throw IllegalArgumentException("Invalid position: $position")
        }
    }
}


