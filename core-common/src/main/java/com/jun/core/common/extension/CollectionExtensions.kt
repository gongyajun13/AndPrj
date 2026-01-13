package com.jun.core.common.extension

/**
 * Collection 扩展函数集合
 */

/**
 * 安全获取列表元素，如果索引越界返回 null
 */
fun <T> List<T>.getOrNull(index: Int): T? {
    return if (index in 0 until size) {
        get(index)
    } else {
        null
    }
}

/**
 * 安全获取列表元素，如果索引越界返回默认值
 */
fun <T> List<T>.getOrDefault(index: Int, default: T): T {
    return getOrNull(index) ?: default
}

/**
 * 获取列表的第一个元素，如果为空返回 null
 */
fun <T> List<T>.firstOrNull(): T? {
    return if (isEmpty()) null else first()
}

/**
 * 获取列表的最后一个元素，如果为空返回 null
 */
fun <T> List<T>.lastOrNull(): T? {
    return if (isEmpty()) null else last()
}

/**
 * 检查列表是否不为空
 */
fun <T> Collection<T>?.isNotNullOrEmpty(): Boolean {
    return !isNullOrEmpty()
}

/**
 * 如果列表为空，返回默认列表
 */
fun <T> List<T>?.orEmpty(): List<T> {
    return this ?: emptyList()
}

/**
 * 将列表转换为带索引的 Pair 列表
 */
fun <T> List<T>.withIndexPairs(): List<Pair<Int, T>> {
    return mapIndexed { index, value -> index to value }
}

/**
 * 按指定大小分割列表
 */
fun <T> List<T>.chunked(size: Int): List<List<T>> {
    return if (size <= 0) {
        listOf(this)
    } else {
        (0 until size).map { index ->
            filterIndexed { i, _ -> i % size == index }
        }
    }
}

/**
 * 移除重复元素（保持顺序）
 */
fun <T> List<T>.distinct(): List<T> {
    val seen = mutableSetOf<T>()
    return filter { seen.add(it) }
}



/**
 * 安全地获取 Map 的值
 */
fun <K, V> Map<K, V>.getOrNull(key: K): V? {
    return get(key)
}

/**
 * 安全地获取 Map 的值，如果不存在返回默认值
 */
fun <K, V> Map<K, V>.getOrDefault(key: K, default: V): V {
    return get(key) ?: default
}

/**
 * 检查 Map 是否不为空
 */
fun <K, V> Map<K, V>?.isNotNullOrEmpty(): Boolean {
    return !isNullOrEmpty()
}

/**
 * 如果 Map 为空，返回空 Map
 */
fun <K, V> Map<K, V>?.orEmpty(): Map<K, V> {
    return this ?: emptyMap()
}

/**
 * 将两个列表合并为 Pair 列表
 */
fun <T, R> List<T>.zip(other: List<R>): List<Pair<T, R>> {
    val minSize = minOf(size, other.size)
    return (0 until minSize).map { index ->
        get(index) to other[index]
    }
}

/**
 * 将列表转换为 Map（使用索引作为键）
 */
fun <T> List<T>.toMapWithIndex(): Map<Int, T> {
    return mapIndexed { index, value -> index to value }.toMap()
}

/**
 * 将列表转换为 Map（使用指定函数生成键）
 */
fun <T, K> List<T>.toMap(keySelector: (T) -> K): Map<K, T> {
    return associateBy(keySelector)
}

/**
 * 将列表转换为 Map（使用指定函数生成键和值）
 */
fun <T, K, V> List<T>.toMap(
    keySelector: (T) -> K,
    valueTransform: (T) -> V
): Map<K, V> {
    return associateBy(keySelector, valueTransform)
}

