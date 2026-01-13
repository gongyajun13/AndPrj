package com.jun.core.ui.extension

import android.app.Dialog
import android.content.Context
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * Dialog 扩展函数集合
 */

/**
 * 显示简单的 AlertDialog
 */
fun Context.showAlertDialog(
    title: String? = null,
    message: String,
    positiveText: String = "确定",
    negativeText: String? = null,
    onPositive: (() -> Unit)? = null,
    onNegative: (() -> Unit)? = null,
    cancelable: Boolean = true
): AlertDialog {
    val builder = MaterialAlertDialogBuilder(this)
        .setMessage(message)
        .setCancelable(cancelable)
    
    title?.let { builder.setTitle(it) }
    
    builder.setPositiveButton(positiveText) { _, _ ->
        onPositive?.invoke()
    }
    
    negativeText?.let {
        builder.setNegativeButton(it) { _, _ ->
            onNegative?.invoke()
        }
    }
    
    return builder.show()
}

/**
 * 显示确认对话框
 */
fun Context.showConfirmDialog(
    title: String? = null,
    message: String,
    confirmText: String = "确认",
    cancelText: String = "取消",
    onConfirm: () -> Unit,
    onCancel: (() -> Unit)? = null
): AlertDialog {
    return showAlertDialog(
        title = title,
        message = message,
        positiveText = confirmText,
        negativeText = cancelText,
        onPositive = onConfirm,
        onNegative = onCancel
    )
}

/**
 * 显示列表对话框
 */
fun Context.showListDialog(
    title: String? = null,
    items: Array<String>,
    onItemClick: (Int, String) -> Unit
): AlertDialog {
    val builder = MaterialAlertDialogBuilder(this)
        .setItems(items) { _, which ->
            onItemClick(which, items[which])
        }
    
    title?.let { builder.setTitle(it) }
    
    return builder.show()
}

/**
 * 显示单选对话框
 */
fun Context.showSingleChoiceDialog(
    title: String? = null,
    items: Array<String>,
    selectedIndex: Int = 0,
    onItemSelected: (Int, String) -> Unit
): AlertDialog {
    val builder = MaterialAlertDialogBuilder(this)
        .setSingleChoiceItems(items, selectedIndex) { dialog, which ->
            onItemSelected(which, items[which])
            dialog.dismiss()
        }
    
    title?.let { builder.setTitle(it) }
    
    return builder.show()
}

/**
 * 显示多选对话框
 */
fun Context.showMultiChoiceDialog(
    title: String? = null,
    items: Array<String>,
    checkedItems: BooleanArray,
    onConfirm: (List<Int>) -> Unit
): AlertDialog {
    val builder = MaterialAlertDialogBuilder(this)
        .setMultiChoiceItems(items, checkedItems) { _, which, isChecked ->
            checkedItems[which] = isChecked
        }
        .setPositiveButton("确定") { _, _ ->
            val selectedIndices = checkedItems.mapIndexed { index: Int, checked: Boolean ->
                if (checked) index else null
            }.filterNotNull()
            onConfirm(selectedIndices)
        }
        .setNegativeButton("取消", null)
    
    title?.let { builder.setTitle(it) }
    
    return builder.show()
}

/**
 * DialogFragment 扩展：安全显示
 */
fun DialogFragment.showSafely(fragmentManager: androidx.fragment.app.FragmentManager, tag: String) {
    if (!isAdded && fragmentManager.findFragmentByTag(tag) == null) {
        show(fragmentManager, tag)
    }
}

/**
 * Dialog 扩展：安全关闭
 */
fun Dialog.dismissSafely() {
    if (isShowing) {
        dismiss()
    }
}

