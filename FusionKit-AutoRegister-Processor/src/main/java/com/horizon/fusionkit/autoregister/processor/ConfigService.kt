package com.horizon.fusionkit.autoregister.processor

import kotlin.reflect.KClass

/**
 * 配置化服务注解
 * 支持从外部配置文件加载服务配置
 */
@MustBeDocumented
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
annotation class ConfigService(
    /**
     * 配置文件路径（相对于项目根目录）
     */
    val configFile: String = "",
    
    /**
     * 配置键名
     */
    val configKey: String = "",
    
    /**
     * 配置格式（JSON/PROPERTIES/YAML）
     */
    val configFormat: ConfigFormat = ConfigFormat.JSON,
    
    /**
     * 热重载支持
     */
    val hotReload: Boolean = false
)

enum class ConfigFormat {
    JSON, PROPERTIES, YAML
}

/**
 * 配置服务管理器
 */
object ConfigServiceManager {
    
    private val configCache = mutableMapOf<String, Any>()
    private val configWatchers = mutableMapOf<String, () -> Unit>()
    
    /**
     * 获取配置值
     */
    fun <T> getConfig(configFile: String, configKey: String, defaultValue: T): T {
        val cacheKey = "$configFile:$configKey"
        return configCache[cacheKey] as? T ?: defaultValue
    }
    
    /**
     * 设置配置值
     */
    fun <T> setConfig(configFile: String, configKey: String, value: T) {
        val cacheKey = "$configFile:$configKey"
        configCache[cacheKey] = value as Any
        
        // 触发监听器
        configWatchers[cacheKey]?.invoke()
    }
    
    /**
     * 注册配置变更监听器
     */
    fun addConfigListener(configFile: String, configKey: String, listener: () -> Unit) {
        val cacheKey = "$configFile:$configKey"
        configWatchers[cacheKey] = listener
    }
    
    /**
     * 清除配置缓存
     */
    fun clearCache() {
        configCache.clear()
    }
}