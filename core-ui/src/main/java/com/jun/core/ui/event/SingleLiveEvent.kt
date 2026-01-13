package com.jun.core.ui.event

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer

/**
 * 单次事件封装
 * 用于处理只需要触发一次的事件（如 Toast、导航等）
 * 使用组合而不是继承，因为 MutableLiveData 是 final 的
 */
class SingleLiveEvent<T> {
    
    private val _liveData = MutableLiveData<T?>()
    
    /**
     * 观察事件
     */
    fun observe(owner: LifecycleOwner, observer: Observer<in T>) {
        _liveData.observe(owner) { value ->
            if (value != null) {
                observer.onChanged(value)
                // 消费后立即清除，确保只触发一次
                _liveData.value = null
            }
        }
    }
    
    /**
     * 发送事件
     */
    fun call(value: T) {
        _liveData.value = value
    }
    
    /**
     * 获取内部的 LiveData（如果需要）
     */
    fun asLiveData(): LiveData<T?> = _liveData
}

/**
 * 无参数的单次事件
 */
class SingleLiveEventUnit {
    private val event = SingleLiveEvent<Unit>()
    
    fun observe(owner: LifecycleOwner, observer: Observer<in Unit>) {
        event.observe(owner, observer)
    }
    
    fun call() {
        event.call(Unit)
    }
    
    fun asLiveData(): LiveData<Unit?> = event.asLiveData()
}

