package com.jun.andprj.ui.tool

import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.jun.andprj.R
import com.jun.andprj.databinding.ActivityFileUtilsDemoBinding
import com.jun.core.common.extension.formatFileSize
import com.jun.core.common.util.FileUtils
import com.jun.core.ui.base.BaseActivity
import java.io.File

/**
 * FileUtils 工具类示例
 */
class FileUtilsDemoActivity : BaseActivity<ActivityFileUtilsDemoBinding>() {

    private lateinit var adapter: FileUtilsDemoAdapter
    private val demoItems = mutableListOf<FileUtilsDemoItem>()
    private val testDir: File by lazy { File(filesDir, "test_file_utils") }
    private val testFile: File by lazy { File(testDir, "test.txt") }

    override fun createBinding(): ActivityFileUtilsDemoBinding = ActivityFileUtilsDemoBinding.inflate(layoutInflater)

    override fun setupViews() {
        setupToolbar()
        setupRecyclerView()
        setupButtonListeners()
        refreshFileStatus()
    }

    private fun setupToolbar() {
        val white = ContextCompat.getColor(this, android.R.color.white)
        val blue = ContextCompat.getColor(this, R.color.gold)
        setStatusBarColor(white, lightIcons = false)
        binding.toolbar.setupSimple(
            leftIcon = R.drawable.icon_back,
            leftIconTint = white,
            title = "文件工具示例",
            titleTextColor = white,
            backgroundColor = blue,
            onLeftClick = { finish() }
        )
    }

    private fun setupRecyclerView() {
        adapter = FileUtilsDemoAdapter(demoItems)
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
    }

    private fun setupButtonListeners() {
        binding.btnCreateDir.setOnClickListener {
            val success = FileUtils.createDirectory(testDir.absolutePath)
            if (success) {
                showSuccess("目录创建成功：${testDir.absolutePath}")
            } else {
                showError("目录创建失败")
            }
            refreshFileStatus()
        }

        binding.btnCreateFile.setOnClickListener {
            val success = FileUtils.writeStringToFile(
                testFile.absolutePath,
                "Hello FileUtils!\n这是测试文件内容。"
            )
            if (success) {
                showSuccess("文件创建成功：${testFile.absolutePath}")
            } else {
                showError("文件创建失败")
            }
            refreshFileStatus()
        }

        binding.btnReadFile.setOnClickListener {
            val content = FileUtils.readFileAsString(testFile.absolutePath)
            if (content != null) {
                showSuccess("文件内容：$content")
            } else {
                showError("文件读取失败或文件不存在")
            }
            refreshFileStatus()
        }

        binding.btnGetFileInfo.setOnClickListener {
            refreshFileStatus()
        }

        binding.btnDeleteFile.setOnClickListener {
            val success = FileUtils.delete(testFile.absolutePath)
            if (success) {
                showSuccess("文件删除成功")
            } else {
                showError("文件删除失败或文件不存在")
            }
            refreshFileStatus()
        }

        binding.btnDeleteDir.setOnClickListener {
            val success = FileUtils.delete(testDir.absolutePath)
            if (success) {
                showSuccess("目录删除成功")
            } else {
                showError("目录删除失败或目录不存在")
            }
            refreshFileStatus()
        }
    }

    private fun refreshFileStatus() {
        val dirExists = FileUtils.exists(testDir.absolutePath)
        val fileExists = FileUtils.exists(testFile.absolutePath)
        val fileSize = if (fileExists) FileUtils.getFileSize(testFile.absolutePath) else 0L
        val fileExtension = if (fileExists) FileUtils.getFileExtension(testFile.absolutePath) else ""
        val fileNameWithoutExt = if (fileExists) FileUtils.getFileNameWithoutExtension(testFile.absolutePath) else ""
        val files = if (dirExists) FileUtils.listFiles(testDir.absolutePath) else emptyList()

        demoItems.clear()
        demoItems.addAll(
            listOf(
                FileUtilsDemoItem(
                    "目录是否存在",
                    if (dirExists) "是" else "否",
                    "FileUtils.exists(\"${testDir.absolutePath}\")"
                ),
                FileUtilsDemoItem(
                    "文件是否存在",
                    if (fileExists) "是" else "否",
                    "FileUtils.exists(\"${testFile.absolutePath}\")"
                ),
                FileUtilsDemoItem(
                    "文件大小",
                    if (fileExists) "${fileSize.formatFileSize()} ($fileSize 字节)" else "0",
                    "FileUtils.getFileSize(\"${testFile.absolutePath}\")"
                ),
                FileUtilsDemoItem(
                    "文件扩展名",
                    fileExtension.ifEmpty { "（无）" },
                    "FileUtils.getFileExtension(\"${testFile.absolutePath}\")"
                ),
                FileUtilsDemoItem(
                    "文件名（不含扩展名）",
                    fileNameWithoutExt.ifEmpty { "（无）" },
                    "FileUtils.getFileNameWithoutExtension(\"${testFile.absolutePath}\")"
                ),
                FileUtilsDemoItem(
                    "目录下文件数量",
                    "${files.size}",
                    "FileUtils.listFiles(\"${testDir.absolutePath}\").size"
                ),
                FileUtilsDemoItem(
                    "目录下文件列表",
                    if (files.isNotEmpty()) {
                        files.joinToString(", ") { it.name }
                    } else {
                        "（无文件）"
                    },
                    "FileUtils.listFiles(\"${testDir.absolutePath}\")"
                )
            )
        )
        adapter.notifyDataSetChanged()
    }
}

data class FileUtilsDemoItem(
    val title: String,
    val result: String,
    val code: String
)

