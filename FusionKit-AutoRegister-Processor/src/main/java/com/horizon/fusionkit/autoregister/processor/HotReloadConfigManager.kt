package com.horizon.fusionkit.autoregister.processor

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 热更新配置管理器
 * 
 * 功能：
 * - 运行时配置管理
 * - 配置文件读写
 * - 配置变更监听
 * - 环境相关配置
 */
class HotReloadConfigManager private constructor() {
    
    companion object {
        @Volatile
        private var INSTANCE: HotReloadConfigManager? = null
        
        fun getInstance(): HotReloadConfigManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: HotReloadConfigManager().also { INSTANCE = it }
            }
        }
        
        // 默认配置文件路径
        private const val DEFAULT_CONFIG_FILE = "fusionkit-hotreload.properties"
        private const val USER_CONFIG_FILE = "fusionkit-hotreload-user.properties"
        
        // 配置键名
        object Keys {
            const val ENABLED = "fusionkit.hotreload.enabled"
            const val WATCH_INTERVAL = "fusionkit.hotreload.watchInterval"
            const val ENABLE_GLOBAL_ROLLBACK = "fusionkit.hotreload.enableGlobalRollback"
            const val GLOBAL_RELOAD_TIMEOUT = "fusionkit.hotreload.globalReloadTimeout"
            const val MAX_CONCURRENT_RELOADS = "fusionkit.hotreload.maxConcurrentReloads"
            const val ENABLE_EVENT_NOTIFICATION = "fusionkit.hotreload.enableEventNotification"
            const val RELOAD_HISTORY_SIZE = "fusionkit.hotreload.reloadHistorySize"
            const val DISABLE_IN_PRODUCTION = "fusionkit.hotreload.disableInProduction"
            const val ENABLE_LOGGING = "fusionkit.hotreload.enableLogging"
            const val LOG_LEVEL = "fusionkit.hotreload.logLevel"
            const val ENABLE_METRICS = "fusionkit.hotreload.enableMetrics"
            const val METRICS_INTERVAL = "fusionkit.hotreload.metricsInterval"
        }
    }
    
    // 配置存储
    private val config = ConcurrentHashMap<String, String>()
    private val defaultConfig = ConcurrentHashMap<String, String>()
    
    // 配置变更监听器
    private val configListeners = mutableListOf<ConfigChangeListener>()
    
    // 状态
    private val isInitialized = AtomicBoolean(false)
    
    /**
     * 初始化配置管理器
     */
    fun initialize() {
        if (isInitialized.compareAndSet(false, true)) {
            loadDefaultConfig()
            loadUserConfig()
            loadSystemProperties()
            loadEnvironmentVariables()
        }
    }
    
    /**
     * 获取配置值
     */
    fun getString(key: String, defaultValue: String = ""): String {
        return config[key] ?: defaultConfig[key] ?: defaultValue
    }
    
    fun getBoolean(key: String, defaultValue: Boolean = false): Boolean {
        return getString(key).toBooleanStrictOrNull() ?: defaultValue
    }
    
    fun getInt(key: String, defaultValue: Int = 0): Int {
        return getString(key).toIntOrNull() ?: defaultValue
    }
    
    fun getLong(key: String, defaultValue: Long = 0L): Long {
        return getString(key).toLongOrNull() ?: defaultValue
    }
    
    /**
     * 设置配置值
     */
    fun setString(key: String, value: String) {
        val oldValue = config[key]
        config[key] = value
        notifyConfigChange(key, oldValue, value)
    }
    
    fun setBoolean(key: String, value: Boolean) {
        setString(key, value.toString())
    }
    
    fun setInt(key: String, value: Int) {
        setString(key, value.toString())
    }
    
    fun setLong(key: String, value: Long) {
        setString(key, value.toString())
    }
    
    /**
     * 获取热更新配置对象
     */
    fun getHotReloadConfiguration(): HotReloadConfiguration {
        return HotReloadConfiguration(
            enabled = getBoolean(Keys.ENABLED, false),
            watchInterval = getLong(Keys.WATCH_INTERVAL, 1000L),
            enableGlobalRollback = getBoolean(Keys.ENABLE_GLOBAL_ROLLBACK, true),
            globalReloadTimeout = getLong(Keys.GLOBAL_RELOAD_TIMEOUT, 10000L),
            maxConcurrentReloads = getInt(Keys.MAX_CONCURRENT_RELOADS, 5),
            enableEventNotification = getBoolean(Keys.ENABLE_EVENT_NOTIFICATION, true),
            reloadHistorySize = getInt(Keys.RELOAD_HISTORY_SIZE, 10),
            disableInProduction = getBoolean(Keys.DISABLE_IN_PRODUCTION, true),
            enableLogging = getBoolean(Keys.ENABLE_LOGGING, true)
        )
    }
    
    /**
     * 更新热更新配置
     */
    fun updateHotReloadConfiguration(config: HotReloadConfiguration) {
        setBoolean(Keys.ENABLED, config.enabled)
        setLong(Keys.WATCH_INTERVAL, config.watchInterval)
        setBoolean(Keys.ENABLE_GLOBAL_ROLLBACK, config.enableGlobalRollback)
        setLong(Keys.GLOBAL_RELOAD_TIMEOUT, config.globalReloadTimeout)
        setInt(Keys.MAX_CONCURRENT_RELOADS, config.maxConcurrentReloads)
        setBoolean(Keys.ENABLE_EVENT_NOTIFICATION, config.enableEventNotification)
        setInt(Keys.RELOAD_HISTORY_SIZE, config.reloadHistorySize)
        setBoolean(Keys.DISABLE_IN_PRODUCTION, config.disableInProduction)
        setBoolean(Keys.ENABLE_LOGGING, config.enableLogging)
    }
    
    /**
     * 保存配置到文件
     */
    fun saveConfig(filePath: String = USER_CONFIG_FILE) {
        try {
            val properties = Properties()
            config.forEach { (key, value) ->
                properties.setProperty(key, value)
            }
            
            FileOutputStream(filePath).use { output ->
                properties.store(output, "FusionKit HotReload Configuration - Generated at ${Date()}")
            }
        } catch (e: Exception) {
            println("[HotReloadConfig] Failed to save config: ${e.message}")
        }
    }
    
    /**
     * 从文件加载配置
     */
    fun loadConfig(filePath: String) {
        try {
            if (File(filePath).exists()) {
                val properties = Properties()
                FileInputStream(filePath).use { input ->
                    properties.load(input)
                }
                
                properties.forEach { (key, value) ->
                    config[key.toString()] = value.toString()
                }
            }
        } catch (e: Exception) {
            println("[HotReloadConfig] Failed to load config from $filePath: ${e.message}")
        }
    }
    
    /**
     * 重置配置为默认值
     */
    fun resetToDefaults() {
        config.clear()
        config.putAll(defaultConfig)
        notifyConfigReset()
    }
    
    /**
     * 获取所有配置
     */
    fun getAllConfig(): Map<String, String> {
        return config.toMap()
    }
    
    /**
     * 添加配置变更监听器
     */
    fun addConfigChangeListener(listener: ConfigChangeListener) {
        configListeners.add(listener)
    }
    
    /**
     * 移除配置变更监听器
     */
    fun removeConfigChangeListener(listener: ConfigChangeListener) {
        configListeners.remove(listener)
    }
    
    /**
     * 检查是否在生产环境
     */
    fun isProductionEnvironment(): Boolean {
        val buildType = System.getProperty("fusionkit.build.type", "debug").lowercase()
        val environment = System.getProperty("fusionkit.environment", "development").lowercase()
        
        return buildType == "release" || 
               environment == "production" || 
               environment == "prod"
    }
    
    /**
     * 检查热更新是否应该启用
     */
    fun shouldEnableHotReload(): Boolean {
        val enabled = getBoolean(Keys.ENABLED, false)
        val disableInProduction = getBoolean(Keys.DISABLE_IN_PRODUCTION, true)
        
        return enabled && !(disableInProduction && isProductionEnvironment())
    }
    
    // 私有方法
    
    private fun loadDefaultConfig() {
        defaultConfig.apply {
            put(Keys.ENABLED, "false")
            put(Keys.WATCH_INTERVAL, "1000")
            put(Keys.ENABLE_GLOBAL_ROLLBACK, "true")
            put(Keys.GLOBAL_RELOAD_TIMEOUT, "10000")
            put(Keys.MAX_CONCURRENT_RELOADS, "5")
            put(Keys.ENABLE_EVENT_NOTIFICATION, "true")
            put(Keys.RELOAD_HISTORY_SIZE, "10")
            put(Keys.DISABLE_IN_PRODUCTION, "true")
            put(Keys.ENABLE_LOGGING, "true")
            put(Keys.LOG_LEVEL, "INFO")
            put(Keys.ENABLE_METRICS, "false")
            put(Keys.METRICS_INTERVAL, "60000")
        }
        
        // 复制默认配置到当前配置
        config.putAll(defaultConfig)
    }
    
    private fun loadUserConfig() {
        // 尝试从多个位置加载用户配置
        val configPaths = listOf(
            USER_CONFIG_FILE,
            "config/$USER_CONFIG_FILE",
            "src/main/resources/$USER_CONFIG_FILE",
            System.getProperty("user.home") + "/.fusionkit/$USER_CONFIG_FILE"
        )
        
        configPaths.forEach { path ->
            loadConfig(path)
        }
    }
    
    private fun loadSystemProperties() {
        // 从系统属性加载配置
        System.getProperties().forEach { (key, value) ->
            val keyStr = key.toString()
            if (keyStr.startsWith("fusionkit.hotreload.")) {
                config[keyStr] = value.toString()
            }
        }
    }
    
    private fun loadEnvironmentVariables() {
        // 从环境变量加载配置
        System.getenv().forEach { (key, value) ->
            if (key.startsWith("FUSIONKIT_HOTRELOAD_")) {
                // 转换环境变量名为配置键名
                val configKey = key.lowercase()
                    .replace("fusionkit_hotreload_", "fusionkit.hotreload.")
                    .replace("_", ".")
                config[configKey] = value
            }
        }
    }
    
    private fun notifyConfigChange(key: String, oldValue: String?, newValue: String) {
        configListeners.forEach { listener ->
            try {
                listener.onConfigChanged(key, oldValue, newValue)
            } catch (e: Exception) {
                println("[HotReloadConfig] Config listener failed: ${e.message}")
            }
        }
    }
    
    private fun notifyConfigReset() {
        configListeners.forEach { listener ->
            try {
                listener.onConfigReset()
            } catch (e: Exception) {
                println("[HotReloadConfig] Config listener failed: ${e.message}")
            }
        }
    }
}

/**
 * 配置变更监听器
 */
interface ConfigChangeListener {
    /**
     * 配置值变更时调用
     */
    fun onConfigChanged(key: String, oldValue: String?, newValue: String)
    
    /**
     * 配置重置时调用
     */
    fun onConfigReset() {}
}

/**
 * 配置构建器
 */
class HotReloadConfigBuilder {
    private val config = mutableMapOf<String, String>()
    
    fun enabled(enabled: Boolean) = apply {
        config[HotReloadConfigManager.Companion.Keys.ENABLED] = enabled.toString()
    }
    
    fun watchInterval(interval: Long) = apply {
        config[HotReloadConfigManager.Companion.Keys.WATCH_INTERVAL] = interval.toString()
    }
    
    fun enableGlobalRollback(enable: Boolean) = apply {
        config[HotReloadConfigManager.Companion.Keys.ENABLE_GLOBAL_ROLLBACK] = enable.toString()
    }
    
    fun globalReloadTimeout(timeout: Long) = apply {
        config[HotReloadConfigManager.Companion.Keys.GLOBAL_RELOAD_TIMEOUT] = timeout.toString()
    }
    
    fun maxConcurrentReloads(max: Int) = apply {
        config[HotReloadConfigManager.Companion.Keys.MAX_CONCURRENT_RELOADS] = max.toString()
    }
    
    fun enableEventNotification(enable: Boolean) = apply {
        config[HotReloadConfigManager.Companion.Keys.ENABLE_EVENT_NOTIFICATION] = enable.toString()
    }
    
    fun reloadHistorySize(size: Int) = apply {
        config[HotReloadConfigManager.Companion.Keys.RELOAD_HISTORY_SIZE] = size.toString()
    }
    
    fun disableInProduction(disable: Boolean) = apply {
        config[HotReloadConfigManager.Companion.Keys.DISABLE_IN_PRODUCTION] = disable.toString()
    }
    
    fun enableLogging(enable: Boolean) = apply {
        config[HotReloadConfigManager.Companion.Keys.ENABLE_LOGGING] = enable.toString()
    }
    
    fun enableMetrics(enable: Boolean) = apply {
        config[HotReloadConfigManager.Companion.Keys.ENABLE_METRICS] = enable.toString()
    }
    
    fun build(): Map<String, String> = config.toMap()
    
    fun applyTo(configManager: HotReloadConfigManager) {
        config.forEach { (key, value) ->
            configManager.setString(key, value)
        }
    }
}