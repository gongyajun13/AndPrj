package com.jun.andprj.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import com.jun.andprj.databinding.FragmentHomeMineBinding
import com.jun.core.ui.base.BaseFragment

/**
 * 首页 Tab：我的
 */
class HomeMineFragment : BaseFragment<FragmentHomeMineBinding>() {

    override fun createBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentHomeMineBinding = FragmentHomeMineBinding.inflate(inflater, container, false)

    override fun setupViews() {
        binding.layoutProfile.setOnClickListener {
            showMessage("跳转登录 / 个人信息（待实现）")
        }
        binding.rowProfile.setOnClickListener {
            showMessage("个人信息（待实现）")
        }
        binding.rowTheme.setOnClickListener {
            showMessage("主题设置（待实现）")
        }
        binding.rowAbout.setOnClickListener {
            showMessage("关于应用（待实现）")
        }
    }
}


