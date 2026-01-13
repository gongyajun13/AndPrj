package com.jun.core.ui.extension

import android.app.Activity
import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.fragment.app.Fragment

/**
 * 键盘相关扩展函数
 */

/**
 * 显示软键盘
 */
fun EditText.showKeyboard() {
    requestFocus()
    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
}

/**
 * 隐藏软键盘
 */
fun View.hideKeyboard() {
    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.hideSoftInputFromWindow(windowToken, 0)
}

/**
 * Activity 扩展：隐藏软键盘
 */
fun Activity.hideKeyboard() {
    val view = currentFocus ?: View(this)
    view.hideKeyboard()
}

/**
 * Fragment 扩展：隐藏软键盘
 */
fun Fragment.hideKeyboard() {
    activity?.hideKeyboard()
}

/**
 * 切换软键盘显示/隐藏
 */
fun EditText.toggleKeyboard() {
    if (hasFocus()) {
        hideKeyboard()
    } else {
        showKeyboard()
    }
}

/**
 * 检查软键盘是否显示
 */
fun View.isKeyboardVisible(): Boolean {
    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    return imm.isAcceptingText
}

/**
 * EditText 扩展：设置文本并移动光标到末尾
 */
fun EditText.setTextAndMoveCursor(text: String) {
    setText(text)
    setSelection(text.length)
}

/**
 * EditText 扩展：清空文本
 */
fun EditText.clear() {
    setText("")
}

/**
 * EditText 扩展：获取文本（去除首尾空格）
 */
fun EditText.getTextTrimmed(): String {
    return text.toString().trim()
}

/**
 * EditText 扩展：检查是否为空
 */
fun EditText.isEmpty(): Boolean {
    return text.isNullOrBlank()
}

/**
 * EditText 扩展：检查是否不为空
 */
fun EditText.isNotEmpty(): Boolean {
    return !isEmpty()
}


