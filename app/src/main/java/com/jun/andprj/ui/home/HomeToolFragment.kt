package com.jun.andprj.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import com.jun.andprj.databinding.FragmentHomeToolBinding
import com.jun.andprj.ui.permission.PermissionDemoActivity
import com.jun.andprj.ui.tool.CacheManagerDemoActivity
import com.jun.andprj.ui.tool.DateUtilsDemoActivity
import com.jun.andprj.ui.tool.FileUtilsDemoActivity
import com.jun.andprj.ui.tool.ImageUtilsDemoActivity
import com.jun.andprj.ui.tool.AppUpdateActivity
import com.jun.andprj.ui.tool.NetworkDemoActivity
import com.jun.andprj.ui.tool.ValidatorDemoActivity
import com.jun.core.ui.base.utils.ActivityManager
import com.jun.core.ui.base.BaseFragment

/**
 * 首页 Tab：工具
 */
class HomeToolFragment : BaseFragment<FragmentHomeToolBinding>() {

    override fun createBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentHomeToolBinding = FragmentHomeToolBinding.inflate(inflater, container, false)

    override fun setupViews() {
        binding.rowDateTool.setOnClickListener {
            ActivityManager.startActivity<DateUtilsDemoActivity>(requireContext())
        }
        binding.rowValidatorTool.setOnClickListener {
            ActivityManager.startActivity<ValidatorDemoActivity>(requireContext())
        }
        binding.rowCacheTool.setOnClickListener {
            ActivityManager.startActivity<CacheManagerDemoActivity>(requireContext())
        }
        binding.rowPermissionDemo.setOnClickListener {
            ActivityManager.startActivity<PermissionDemoActivity>(requireContext())
        }
        binding.rowFileUtilsDemo.setOnClickListener {
            ActivityManager.startActivity<FileUtilsDemoActivity>(requireContext())
        }
        binding.rowImageUtilsDemo.setOnClickListener {
            ActivityManager.startActivity<ImageUtilsDemoActivity>(requireContext())
        }
        binding.rowNetworkDemo.setOnClickListener {
            ActivityManager.startActivity<NetworkDemoActivity>(requireContext())
        }
        binding.rowAppUpdate.setOnClickListener {
            ActivityManager.startActivity<AppUpdateActivity>(requireContext())
        }
    }
}


