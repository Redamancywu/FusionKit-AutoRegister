package com.horizon.fusionkit.autoregister.processor

import java.io.File
import java.net.URLClassLoader
import java.util.jar.JarFile

/**
 * 插件配置
 */
data class PluginConfig(
    val enabled: Boolean = true,
    val pluginDir: String = "plugins",
    val autoLoad: Boolean = true,
    val hotSwap: Boolean = false,
    val logLevel: PluginLogLevel = PluginLogLevel.INFO
)

enum class PluginLogLevel {
    DEBUG, INFO, WARN, ERROR
}

/**
 * 插件信息
 */
data class PluginInfo(
    val id: String,
    val name: String,
    val version: String,
    val description: String = "",
    val author: String = "",
    val className: String,
    val file: File? = null,
    val loaded: Boolean = false
)

/**
 * 插件接口
 */
interface Plugin {
    /**
     * 插件初始化
     */
    fun initialize()
    
    /**
     * 插件销毁
     */
    fun destroy()
    
    /**
     * 获取插件信息
     */
    fun getInfo(): PluginInfo
}

/**
 * 插件管理器
 */
object PluginManager {
    
    private var config: PluginConfig = PluginConfig()
    private val plugins = mutableMapOf<String, PluginInfo>()
    private val loadedPlugins = mutableMapOf<String, Plugin>()
    private val classLoaders = mutableMapOf<String, URLClassLoader>()
    
    /**
     * 初始化插件管理器
     */
    fun initialize(customConfig: PluginConfig? = null) {
        customConfig?.let { config = it }
        
        if (!config.enabled) {
            log("Plugin system is disabled", PluginLogLevel.INFO)
            return
        }
        
        log("PluginManager initialized with config: $config", PluginLogLevel.INFO)
        
        if (config.autoLoad) {
            loadPlugins()
        }
    }
    
    /**
     * 加载所有插件
     */
    fun loadPlugins() {
        if (!config.enabled) return
        
        val pluginDir = File(config.pluginDir)
        if (!pluginDir.exists() || !pluginDir.isDirectory) {
            log("Plugin directory not found: ${pluginDir.absolutePath}", PluginLogLevel.WARN)
            return
        }
        
        val jarFiles = pluginDir.listFiles { file ->
            file.isFile && file.extension.equals("jar", ignoreCase = true)
        } ?: emptyArray()
        
        log("Found ${jarFiles.size} plugin files", PluginLogLevel.INFO)
        
        jarFiles.forEach { jarFile ->
            try {
                loadPlugin(jarFile)
            } catch (e: Exception) {
                log("Failed to load plugin ${jarFile.name}: ${e.message}", PluginLogLevel.ERROR)
            }
        }
    }
    
    /**
     * 加载单个插件
     */
    fun loadPlugin(jarFile: File): PluginInfo? {
        if (!config.enabled) return null
        
        try {
            val jar = JarFile(jarFile)
            val manifest = jar.manifest
            val attributes = manifest.mainAttributes
            
            val pluginId = attributes.getValue("Plugin-Id") ?: throw IllegalArgumentException("Plugin-Id not found")
            val pluginName = attributes.getValue("Plugin-Name") ?: pluginId
            val pluginVersion = attributes.getValue("Plugin-Version") ?: "1.0.0"
            val pluginClass = attributes.getValue("Plugin-Class") ?: throw IllegalArgumentException("Plugin-Class not found")
            val pluginDescription = attributes.getValue("Plugin-Description") ?: ""
            val pluginAuthor = attributes.getValue("Plugin-Author") ?: ""
            
            val pluginInfo = PluginInfo(
                id = pluginId,
                name = pluginName,
                version = pluginVersion,
                description = pluginDescription,
                author = pluginAuthor,
                className = pluginClass,
                file = jarFile
            )
            
            // 创建类加载器
            val classLoader = URLClassLoader(arrayOf(jarFile.toURI().toURL()))
            classLoaders[pluginId] = classLoader
            
            // 加载插件类
            val pluginClassObj = classLoader.loadClass(pluginClass)
            val pluginInstance = pluginClassObj.getDeclaredConstructor().newInstance() as Plugin
            
            // 初始化插件
            pluginInstance.initialize()
            
            plugins[pluginId] = pluginInfo.copy(loaded = true)
            loadedPlugins[pluginId] = pluginInstance
            
            log("Plugin loaded successfully: $pluginName v$pluginVersion by $pluginAuthor", PluginLogLevel.INFO)
            
            return pluginInfo
            
        } catch (e: Exception) {
            log("Failed to load plugin ${jarFile.name}: ${e.message}", PluginLogLevel.ERROR)
            return null
        }
    }
    
    /**
     * 卸载插件
     */
    fun unloadPlugin(pluginId: String): Boolean {
        if (!config.enabled) return false
        
        val plugin = loadedPlugins[pluginId]
        if (plugin != null) {
            try {
                plugin.destroy()
                loadedPlugins.remove(pluginId)
                plugins[pluginId] = plugins[pluginId]?.copy(loaded = false) ?: return false
                
                classLoaders[pluginId]?.close()
                classLoaders.remove(pluginId)
                
                log("Plugin unloaded: $pluginId", PluginLogLevel.INFO)
                return true
            } catch (e: Exception) {
                log("Failed to unload plugin $pluginId: ${e.message}", PluginLogLevel.ERROR)
                return false
            }
        }
        return false
    }
    
    /**
     * 获取插件实例
     */
    fun getPlugin(pluginId: String): Plugin? = loadedPlugins[pluginId]
    
    /**
     * 获取所有插件信息
     */
    fun getAllPlugins(): List<PluginInfo> = plugins.values.toList()
    
    /**
     * 获取已加载的插件
     */
    fun getLoadedPlugins(): List<Plugin> = loadedPlugins.values.toList()
    
    /**
     * 检查插件是否已加载
     */
    fun isPluginLoaded(pluginId: String): Boolean = loadedPlugins.containsKey(pluginId)
    
    /**
     * 销毁所有插件
     */
    fun destroyAll() {
        if (!config.enabled) return
        
        loadedPlugins.keys.toList().forEach { pluginId ->
            unloadPlugin(pluginId)
        }
        
        plugins.clear()
        loadedPlugins.clear()
        classLoaders.values.forEach { it.close() }
        classLoaders.clear()
        
        log("All plugins destroyed", PluginLogLevel.INFO)
    }
    
    /**
     * 重新加载所有插件
     */
    fun reloadPlugins() {
        if (!config.enabled) return
        
        destroyAll()
        loadPlugins()
        
        log("All plugins reloaded", PluginLogLevel.INFO)
    }
    
    /**
     * 日志记录
     */
    private fun log(message: String, level: PluginLogLevel) {
        if (level >= config.logLevel) {
            println("[PluginManager] $level: $message")
        }
    }
}