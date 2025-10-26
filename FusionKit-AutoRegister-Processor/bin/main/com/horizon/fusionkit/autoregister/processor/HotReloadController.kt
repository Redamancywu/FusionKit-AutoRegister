package com.horizon.fusionkit.autoregister.processor

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

/**
 * 热更新运行时控制器
 * 
 * 功能：
 * - 全局热更新开关控制
 * - 服务级别的热更新控制
 * - 运行时状态管理
 * - 安全检查和权限控制
 */
class HotReloadController private constructor() {
    
    companion object {
        @Volatile
        private var INSTANCE: HotReloadController? = null
        
        fun getInstance(): HotReloadController {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: HotReloadController().also { INSTANCE = it }
            }
        }
    }
    
    // 全局状态
    private val globalEnabled = AtomicBoolean(false)
    private val isInitialized = AtomicBoolean(false)
    private val lastStateChange = AtomicLong(0L)
    
    // 服务级别控制
    private val serviceStates = ConcurrentHashMap<String, ServiceHotReloadState>()
    private val serviceLock = ReentrantReadWriteLock()
    
    // 配置管理器
    private val configManager = HotReloadConfigManager.getInstance()
    
    // 状态监听器
    private val stateListeners = mutableListOf<HotReloadStateListener>()
    
    /**
     * 初始化控制器
     */
    fun initialize() {
        if (isInitialized.compareAndSet(false, true)) {
            configManager.initialize()
            
            // 从配置加载初始状态
            val shouldEnable = configManager.shouldEnableHotReload()
            setGlobalEnabled(shouldEnable, "initialization")
            
            // 添加配置变更监听
            configManager.addConfigChangeListener(object : ConfigChangeListener {
                override fun onConfigChanged(key: String, oldValue: String?, newValue: String) {
                    if (key == HotReloadConfigManager.Companion.Keys.ENABLED) {
                        val enabled = newValue.toBooleanStrictOrNull() ?: false
                        if (configManager.shouldEnableHotReload() != globalEnabled.get()) {
                            setGlobalEnabled(configManager.shouldEnableHotReload(), "config_change")
                        }
                    }
                }
            })
        }
    }
    
    /**
     * 设置全局热更新开关
     */
    fun setGlobalEnabled(enabled: Boolean, reason: String = "manual"): Boolean {
        // 安全检查
        if (!canChangeGlobalState()) {
            return false
        }
        
        val oldState = globalEnabled.get()
        if (oldState != enabled) {
            globalEnabled.set(enabled)
            lastStateChange.set(System.currentTimeMillis())
            
            // 通知状态变更
            notifyGlobalStateChanged(oldState, enabled, reason)
            
            // 如果禁用全局热更新，也禁用所有服务的热更新
            if (!enabled) {
                disableAllServices(reason)
            }
        }
        
        return true
    }
    
    /**
     * 获取全局热更新状态
     */
    fun isGlobalEnabled(): Boolean = globalEnabled.get()
    
    /**
     * 设置服务级别的热更新开关
     */
    fun setServiceEnabled(serviceId: String, enabled: Boolean, reason: String = "manual"): Boolean {
        // 检查全局状态
        if (!globalEnabled.get() && enabled) {
            return false
        }
        
        serviceLock.write {
            val currentState = serviceStates[serviceId] ?: ServiceHotReloadState(serviceId)
            val newState = currentState.copy(
                enabled = enabled,
                lastStateChange = System.currentTimeMillis(),
                stateChangeReason = reason
            )
            
            serviceStates[serviceId] = newState
            
            // 通知状态变更
            notifyServiceStateChanged(serviceId, currentState.enabled, enabled, reason)
        }
        
        return true
    }
    
    /**
     * 获取服务热更新状态
     */
    fun isServiceEnabled(serviceId: String): Boolean {
        if (!globalEnabled.get()) {
            return false
        }
        
        return serviceLock.read {
            serviceStates[serviceId]?.enabled ?: false
        }
    }
    
    /**
     * 获取服务状态信息
     */
    fun getServiceState(serviceId: String): ServiceHotReloadState? {
        return serviceLock.read {
            serviceStates[serviceId]?.copy()
        }
    }
    
    /**
     * 获取所有服务状态
     */
    fun getAllServiceStates(): Map<String, ServiceHotReloadState> {
        return serviceLock.read {
            serviceStates.mapValues { it.value.copy() }
        }
    }
    
    /**
     * 启用所有服务的热更新
     */
    fun enableAllServices(reason: String = "batch_enable"): Int {
        if (!globalEnabled.get()) {
            return 0
        }
        
        var count = 0
        serviceLock.write {
            serviceStates.keys.forEach { serviceId ->
                val currentState = serviceStates[serviceId]!!
                if (!currentState.enabled) {
                    serviceStates[serviceId] = currentState.copy(
                        enabled = true,
                        lastStateChange = System.currentTimeMillis(),
                        stateChangeReason = reason
                    )
                    count++
                    notifyServiceStateChanged(serviceId, false, true, reason)
                }
            }
        }
        
        return count
    }
    
    /**
     * 禁用所有服务的热更新
     */
    fun disableAllServices(reason: String = "batch_disable"): Int {
        var count = 0
        serviceLock.write {
            serviceStates.keys.forEach { serviceId ->
                val currentState = serviceStates[serviceId]!!
                if (currentState.enabled) {
                    serviceStates[serviceId] = currentState.copy(
                        enabled = false,
                        lastStateChange = System.currentTimeMillis(),
                        stateChangeReason = reason
                    )
                    count++
                    notifyServiceStateChanged(serviceId, true, false, reason)
                }
            }
        }
        
        return count
    }
    
    /**
     * 注册服务
     */
    fun registerService(serviceId: String, initialEnabled: Boolean = false): Boolean {
        if (!globalEnabled.get() && initialEnabled) {
            return false
        }
        
        serviceLock.write {
            if (!serviceStates.containsKey(serviceId)) {
                serviceStates[serviceId] = ServiceHotReloadState(
                    serviceId = serviceId,
                    enabled = initialEnabled,
                    registrationTime = System.currentTimeMillis(),
                    lastStateChange = System.currentTimeMillis(),
                    stateChangeReason = "registration"
                )
                return true
            }
        }
        
        return false
    }
    
    /**
     * 注销服务
     */
    fun unregisterService(serviceId: String): Boolean {
        serviceLock.write {
            val removed = serviceStates.remove(serviceId)
            if (removed != null) {
                notifyServiceUnregistered(serviceId)
                return true
            }
        }
        
        return false
    }
    
    /**
     * 检查是否可以执行热更新
     */
    fun canPerformHotReload(serviceId: String): HotReloadPermission {
        // 检查全局状态
        if (!globalEnabled.get()) {
            return HotReloadPermission.DENIED_GLOBAL_DISABLED
        }
        
        // 检查生产环境限制
        if (configManager.isProductionEnvironment() && 
            configManager.getBoolean(HotReloadConfigManager.Companion.Keys.DISABLE_IN_PRODUCTION, true)) {
            return HotReloadPermission.DENIED_PRODUCTION_ENV
        }
        
        // 检查服务状态
        val serviceState = serviceLock.read { serviceStates[serviceId] }
        if (serviceState == null) {
            return HotReloadPermission.DENIED_SERVICE_NOT_REGISTERED
        }
        
        if (!serviceState.enabled) {
            return HotReloadPermission.DENIED_SERVICE_DISABLED
        }
        
        // 检查冷却时间
        val cooldownPeriod = configManager.getLong("fusionkit.hotreload.cooldownPeriod", 1000L)
        if (System.currentTimeMillis() - serviceState.lastReloadTime < cooldownPeriod) {
            return HotReloadPermission.DENIED_COOLDOWN_PERIOD
        }
        
        return HotReloadPermission.ALLOWED
    }
    
    /**
     * 记录热更新执行
     */
    fun recordHotReload(serviceId: String, success: Boolean) {
        serviceLock.write {
            val currentState = serviceStates[serviceId]
            if (currentState != null) {
                serviceStates[serviceId] = currentState.copy(
                    lastReloadTime = System.currentTimeMillis(),
                    reloadCount = currentState.reloadCount + 1,
                    successCount = if (success) currentState.successCount + 1 else currentState.successCount,
                    failureCount = if (!success) currentState.failureCount + 1 else currentState.failureCount
                )
            }
        }
    }
    
    /**
     * 获取控制器状态摘要
     */
    fun getStatusSummary(): HotReloadControllerStatus {
        val serviceCount = serviceLock.read { serviceStates.size }
        val enabledServiceCount = serviceLock.read { 
            serviceStates.values.count { it.enabled } 
        }
        
        return HotReloadControllerStatus(
            globalEnabled = globalEnabled.get(),
            totalServices = serviceCount,
            enabledServices = enabledServiceCount,
            lastStateChange = lastStateChange.get(),
            isProductionEnvironment = configManager.isProductionEnvironment()
        )
    }
    
    /**
     * 添加状态监听器
     */
    fun addStateListener(listener: HotReloadStateListener) {
        stateListeners.add(listener)
    }
    
    /**
     * 移除状态监听器
     */
    fun removeStateListener(listener: HotReloadStateListener) {
        stateListeners.remove(listener)
    }
    
    /**
     * 重置所有状态
     */
    fun reset() {
        serviceLock.write {
            serviceStates.clear()
        }
        globalEnabled.set(false)
        lastStateChange.set(System.currentTimeMillis())
        notifyControllerReset()
    }
    
    // 私有方法
    
    private fun canChangeGlobalState(): Boolean {
        // 可以添加更多安全检查，比如权限验证等
        return true
    }
    
    private fun notifyGlobalStateChanged(oldState: Boolean, newState: Boolean, reason: String) {
        stateListeners.forEach { listener ->
            try {
                listener.onGlobalStateChanged(oldState, newState, reason)
            } catch (e: Exception) {
                println("[HotReloadController] State listener failed: ${e.message}")
            }
        }
    }
    
    private fun notifyServiceStateChanged(serviceId: String, oldState: Boolean, newState: Boolean, reason: String) {
        stateListeners.forEach { listener ->
            try {
                listener.onServiceStateChanged(serviceId, oldState, newState, reason)
            } catch (e: Exception) {
                println("[HotReloadController] State listener failed: ${e.message}")
            }
        }
    }
    
    private fun notifyServiceUnregistered(serviceId: String) {
        stateListeners.forEach { listener ->
            try {
                listener.onServiceUnregistered(serviceId)
            } catch (e: Exception) {
                println("[HotReloadController] State listener failed: ${e.message}")
            }
        }
    }
    
    private fun notifyControllerReset() {
        stateListeners.forEach { listener ->
            try {
                listener.onControllerReset()
            } catch (e: Exception) {
                println("[HotReloadController] State listener failed: ${e.message}")
            }
        }
    }
}

/**
 * 服务热更新状态
 */
data class ServiceHotReloadState(
    val serviceId: String,
    val enabled: Boolean = false,
    val registrationTime: Long = System.currentTimeMillis(),
    val lastStateChange: Long = System.currentTimeMillis(),
    val stateChangeReason: String = "",
    val lastReloadTime: Long = 0L,
    val reloadCount: Int = 0,
    val successCount: Int = 0,
    val failureCount: Int = 0
) {
    val successRate: Double
        get() = if (reloadCount > 0) successCount.toDouble() / reloadCount else 0.0
}

/**
 * 热更新权限检查结果
 */
enum class HotReloadPermission(val message: String) {
    ALLOWED("Hot reload is allowed"),
    DENIED_GLOBAL_DISABLED("Global hot reload is disabled"),
    DENIED_SERVICE_DISABLED("Service hot reload is disabled"),
    DENIED_SERVICE_NOT_REGISTERED("Service is not registered for hot reload"),
    DENIED_PRODUCTION_ENV("Hot reload is disabled in production environment"),
    DENIED_COOLDOWN_PERIOD("Service is in cooldown period")
}

/**
 * 控制器状态摘要
 */
data class HotReloadControllerStatus(
    val globalEnabled: Boolean,
    val totalServices: Int,
    val enabledServices: Int,
    val lastStateChange: Long,
    val isProductionEnvironment: Boolean
)

/**
 * 热更新状态监听器
 */
interface HotReloadStateListener {
    /**
     * 全局状态变更时调用
     */
    fun onGlobalStateChanged(oldState: Boolean, newState: Boolean, reason: String) {}
    
    /**
     * 服务状态变更时调用
     */
    fun onServiceStateChanged(serviceId: String, oldState: Boolean, newState: Boolean, reason: String) {}
    
    /**
     * 服务注销时调用
     */
    fun onServiceUnregistered(serviceId: String) {}
    
    /**
     * 控制器重置时调用
     */
    fun onControllerReset() {}
}

/**
 * 热更新控制器构建器
 */
class HotReloadControllerBuilder {
    private var globalEnabled = false
    private val services = mutableMapOf<String, Boolean>()
    
    fun globalEnabled(enabled: Boolean) = apply {
        this.globalEnabled = enabled
    }
    
    fun addService(serviceId: String, enabled: Boolean = false) = apply {
        services[serviceId] = enabled
    }
    
    fun build(): HotReloadController {
        val controller = HotReloadController.getInstance()
        controller.initialize()
        controller.setGlobalEnabled(globalEnabled, "builder")
        
        services.forEach { (serviceId, enabled) ->
            controller.registerService(serviceId, enabled)
        }
        
        return controller
    }
}