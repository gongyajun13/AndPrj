package com.jun.andprj.ui.tool

import android.text.Editable
import android.text.TextWatcher
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.jun.andprj.R
import com.jun.andprj.databinding.ActivityValidatorDemoBinding
import com.jun.core.common.util.Validator
import com.jun.core.ui.base.BaseActivity

/**
 * Validator 工具类示例
 */
class ValidatorDemoActivity : BaseActivity<ActivityValidatorDemoBinding>() {

    private lateinit var adapter: ValidatorDemoAdapter
    private val demoItems = mutableListOf<ValidatorDemoItem>()

    override fun createBinding(): ActivityValidatorDemoBinding = ActivityValidatorDemoBinding.inflate(layoutInflater)

    override fun setupViews() {
        setupToolbar()
        setupRecyclerView()
        setupInputListeners()
    }

    private fun setupToolbar() {
        val white = ContextCompat.getColor(this, android.R.color.white)
        val blue = ContextCompat.getColor(this, R.color.blue)
        setStatusBarColor(white, lightIcons = false)
        binding.toolbar.setupSimple(
            leftIcon = R.drawable.icon_back,
            leftIconTint = white,
            title = "校验工具示例",
            titleTextColor = white,
            backgroundColor = blue,
            onLeftClick = { finish() }
        )
    }

    private fun setupRecyclerView() {
        adapter = ValidatorDemoAdapter(demoItems)
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
    }

    private fun setupInputListeners() {
        binding.etEmail.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                validateInputs()
            }
        })

        binding.etPhone.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                validateInputs()
            }
        })

        binding.etPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                validateInputs()
            }
        })

        binding.etUrl.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                validateInputs()
            }
        })
    }

    private fun validateInputs() {
        val email = binding.etEmail.text.toString()
        val phone = binding.etPhone.text.toString()
        val password = binding.etPassword.text.toString()
        val url = binding.etUrl.text.toString()

        demoItems.clear()
        demoItems.addAll(
            listOf(
                ValidatorDemoItem(
                    "邮箱验证",
                    email.ifEmpty { "（请输入）" },
                    when (val result = Validator.validateEmail(email)) {
                        is Validator.ValidationResult.Valid -> "✓ 验证通过"
                        is Validator.ValidationResult.Invalid -> "✗ ${result.message}"
                    },
                    "Validator.validateEmail(\"$email\")"
                ),
                ValidatorDemoItem(
                    "手机号验证",
                    phone.ifEmpty { "（请输入）" },
                    when (val result = Validator.validatePhone(phone)) {
                        is Validator.ValidationResult.Valid -> "✓ 验证通过"
                        is Validator.ValidationResult.Invalid -> "✗ ${result.message}"
                    },
                    "Validator.validatePhone(\"$phone\")"
                ),
                ValidatorDemoItem(
                    "密码验证（最少6位）",
                    password.ifEmpty { "（请输入）" },
                    when (val result = Validator.validatePassword(password, 6)) {
                        is Validator.ValidationResult.Valid -> "✓ 验证通过"
                        is Validator.ValidationResult.Invalid -> "✗ ${result.message}"
                    },
                    "Validator.validatePassword(\"$password\", 6)"
                ),
                ValidatorDemoItem(
                    "强密码验证",
                    password.ifEmpty { "（请输入）" },
                    when (val result = Validator.validateStrongPassword(password)) {
                        is Validator.ValidationResult.Valid -> "✓ 验证通过"
                        is Validator.ValidationResult.Invalid -> "✗ ${result.message}"
                    },
                    "Validator.validateStrongPassword(\"$password\")"
                ),
                ValidatorDemoItem(
                    "URL 验证",
                    url.ifEmpty { "（请输入）" },
                    when (val result = Validator.validateUrl(url)) {
                        is Validator.ValidationResult.Valid -> "✓ 验证通过"
                        is Validator.ValidationResult.Invalid -> "✗ ${result.message}"
                    },
                    "Validator.validateUrl(\"$url\")"
                ),
                ValidatorDemoItem(
                    "非空验证",
                    email.ifEmpty { "（请输入）" },
                    when (val result = Validator.validateNotEmpty(email, "邮箱")) {
                        is Validator.ValidationResult.Valid -> "✓ 验证通过"
                        is Validator.ValidationResult.Invalid -> "✗ ${result.message}"
                    },
                    "Validator.validateNotEmpty(\"$email\", \"邮箱\")"
                ),
                ValidatorDemoItem(
                    "长度验证（3-10位）",
                    email.ifEmpty { "（请输入）" },
                    when (val result = Validator.validateLength(email, 3, 10, "邮箱")) {
                        is Validator.ValidationResult.Valid -> "✓ 验证通过"
                        is Validator.ValidationResult.Invalid -> "✗ ${result.message}"
                    },
                    "Validator.validateLength(\"$email\", 3, 10, \"邮箱\")"
                )
            )
        )
        adapter.notifyDataSetChanged()
    }
}

data class ValidatorDemoItem(
    val title: String,
    val input: String,
    val result: String,
    val code: String
)

