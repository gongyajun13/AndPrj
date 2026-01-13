package com.jun.andprj.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import com.jun.andprj.databinding.FragmentHomeFeatureBinding
import com.jun.core.ui.base.BaseFragment

/**
 * 首页 Tab：功能
 */
class HomeFeatureFragment : BaseFragment<FragmentHomeFeatureBinding>() {

    override fun createBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentHomeFeatureBinding = FragmentHomeFeatureBinding.inflate(inflater, container, false)

    override fun setupViews() {
        binding.cardUser.setOnClickListener {
            showMessage("用户管理功能（待实现）")
        }
        binding.cardNetwork.setOnClickListener {
            showMessage("网络请求示例（待实现）")
        }
        binding.cardPaging.setOnClickListener {
            showMessage("分页加载示例（待实现）")
        }
        binding.cardSettings.setOnClickListener {
            showMessage("系统配置示例（待实现）")
        }
    }
}


