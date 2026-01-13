package com.jun.andprj.ui.tool

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * App 升级缓存数据
 */
data class AppUpdateCacheData(
    val downloadUrl: String? = null,
    val totalBytes: Long = 0L,
    val downloadedBytes: Long = 0L,
    val latestVersion: String? = null,
    val latestVersionCode: Int = 0,
    val updateInfo: String? = null,
    val isForceUpdate: Boolean = false
) {
    /**
     * 是否有缓存数据
     */
    fun hasCache(): Boolean {
        return downloadUrl != null && totalBytes > 0
    }
    
    /**
     * 文件是否完整
     */
    fun isFileComplete(): Boolean {
        if (totalBytes <= 0 || downloadedBytes < 0) {
            return false
        }
        // 文件大小应该在预期的 95%-105% 范围内（允许一些误差）
        return downloadedBytes >= totalBytes * 0.95 && downloadedBytes <= totalBytes * 1.05
    }
    
    /**
     * 文件是否不完整
     */
    fun isFileIncomplete(): Boolean {
        if (totalBytes <= 0) {
            return downloadedBytes > 0 && downloadedBytes < 1024 * 1024 // 小于1MB认为不完整
        }
        // 文件大小小于预期的 95%，认为不完整
        return downloadedBytes < totalBytes * 0.95
    }
}

/**
 * App 升级缓存管理器
 * 用于持久化保存下载进度、文件大小等信息
 */
@Singleton
class AppUpdateCacheManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_update_cache")
    
    private val dataStore = context.dataStore
    
    companion object {
        private val KEY_DOWNLOAD_URL = stringPreferencesKey("download_url")
        private val KEY_TOTAL_BYTES = longPreferencesKey("total_bytes")
        private val KEY_DOWNLOADED_BYTES = longPreferencesKey("downloaded_bytes")
        private val KEY_LATEST_VERSION = stringPreferencesKey("latest_version")
        private val KEY_LATEST_VERSION_CODE = longPreferencesKey("latest_version_code")
        private val KEY_UPDATE_INFO = stringPreferencesKey("update_info")
        private val KEY_IS_FORCE_UPDATE = stringPreferencesKey("is_force_update") // 使用字符串存储布尔值
    }
    
    /**
     * 获取缓存数据
     */
    val cacheData: Flow<AppUpdateCacheData> = dataStore.data.map { preferences ->
        AppUpdateCacheData(
            downloadUrl = preferences[KEY_DOWNLOAD_URL],
            totalBytes = preferences[KEY_TOTAL_BYTES] ?: 0L,
            downloadedBytes = preferences[KEY_DOWNLOADED_BYTES] ?: 0L,
            latestVersion = preferences[KEY_LATEST_VERSION],
            latestVersionCode = (preferences[KEY_LATEST_VERSION_CODE] ?: 0L).toInt(),
            updateInfo = preferences[KEY_UPDATE_INFO],
            isForceUpdate = preferences[KEY_IS_FORCE_UPDATE] == "true"
        )
    }
    
    /**
     * 保存缓存数据
     */
    suspend fun saveCacheData(data: AppUpdateCacheData) {
        dataStore.edit { preferences ->
            data.downloadUrl?.let { preferences[KEY_DOWNLOAD_URL] = it }
            if (data.totalBytes > 0) {
                preferences[KEY_TOTAL_BYTES] = data.totalBytes
            }
            if (data.downloadedBytes >= 0) {
                preferences[KEY_DOWNLOADED_BYTES] = data.downloadedBytes
            }
            data.latestVersion?.let { preferences[KEY_LATEST_VERSION] = it }
            if (data.latestVersionCode > 0) {
                preferences[KEY_LATEST_VERSION_CODE] = data.latestVersionCode.toLong()
            }
            data.updateInfo?.let { preferences[KEY_UPDATE_INFO] = it }
            preferences[KEY_IS_FORCE_UPDATE] = if (data.isForceUpdate) "true" else "false"
        }
    }
    
    /**
     * 更新下载进度
     */
    suspend fun updateDownloadProgress(downloadedBytes: Long, totalBytes: Long) {
        dataStore.edit { preferences ->
            preferences[KEY_DOWNLOADED_BYTES] = downloadedBytes
            if (totalBytes > 0) {
                preferences[KEY_TOTAL_BYTES] = totalBytes
            }
        }
    }
    
    /**
     * 更新文件大小
     */
    suspend fun updateFileSize(totalBytes: Long) {
        if (totalBytes > 0) {
            dataStore.edit { preferences ->
                preferences[KEY_TOTAL_BYTES] = totalBytes
            }
        }
    }
    
    /**
     * 清除缓存数据
     */
    suspend fun clearCache() {
        dataStore.edit { preferences ->
            preferences.remove(KEY_DOWNLOAD_URL)
            preferences.remove(KEY_TOTAL_BYTES)
            preferences.remove(KEY_DOWNLOADED_BYTES)
            preferences.remove(KEY_LATEST_VERSION)
            preferences.remove(KEY_LATEST_VERSION_CODE)
            preferences.remove(KEY_UPDATE_INFO)
            preferences.remove(KEY_IS_FORCE_UPDATE)
        }
    }
    
    /**
     * 获取当前缓存数据（同步方式，用于初始化）
     */
    suspend fun getCurrentCacheData(): AppUpdateCacheData {
        val preferences = dataStore.data.first()
        return AppUpdateCacheData(
            downloadUrl = preferences[KEY_DOWNLOAD_URL],
            totalBytes = preferences[KEY_TOTAL_BYTES] ?: 0L,
            downloadedBytes = preferences[KEY_DOWNLOADED_BYTES] ?: 0L,
            latestVersion = preferences[KEY_LATEST_VERSION],
            latestVersionCode = (preferences[KEY_LATEST_VERSION_CODE] ?: 0L).toInt(),
            updateInfo = preferences[KEY_UPDATE_INFO],
            isForceUpdate = preferences[KEY_IS_FORCE_UPDATE] == "true"
        )
    }
}

