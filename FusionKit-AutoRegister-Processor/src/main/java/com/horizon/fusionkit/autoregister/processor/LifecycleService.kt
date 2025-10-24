package com.horizon.fusionkit.autoregister.processor

/**
 * 服务生命周期接口
 */
interface LifecycleService {
    /**
     * 服务初始化
     */
    fun onCreate()
    
    /**
     * 服务销毁
     */
    fun onDestroy()
    
    /**
     * 服务暂停
     */
    fun onPause() {}
    
    /**
     * 服务恢复
     */
    fun onResume() {}
}

/**
 * 生命周期管理器
 */
object ServiceLifecycleManager {
    
    private val lifecycleServices = mutableListOf<LifecycleService>()
    private var isInitialized = false
    
    /**
     * 注册生命周期服务
     */
    fun registerService(service: LifecycleService) {
        lifecycleServices.add(service)
        
        // 如果已经初始化，立即调用 onCreate
        if (isInitialized) {
            service.onCreate()
        }
    }
    
    /**
     * 初始化所有服务
     */
    fun initialize() {
        if (isInitialized) return
        
        lifecycleServices.forEach { service ->
            try {
                service.onCreate()
            } catch (e: Exception) {
                println("Error initializing service: ${e.message}")
            }
        }
        
        isInitialized = true
        println("ServiceLifecycleManager: ${lifecycleServices.size} services initialized")
    }
    
    /**
     * 销毁所有服务
     */
    fun destroy() {
        lifecycleServices.forEach { service ->
            try {
                service.onDestroy()
            } catch (e: Exception) {
                println("Error destroying service: ${e.message}")
            }
        }
        
        lifecycleServices.clear()
        isInitialized = false
        println("ServiceLifecycleManager: All services destroyed")
    }
    
    /**
     * 暂停所有服务
     */
    fun pause() {
        lifecycleServices.forEach { service ->
            try {
                service.onPause()
            } catch (e: Exception) {
                println("Error pausing service: ${e.message}")
            }
        }
        println("ServiceLifecycleManager: All services paused")
    }
    
    /**
     * 恢复所有服务
     */
    fun resume() {
        lifecycleServices.forEach { service ->
            try {
                service.onResume()
            } catch (e: Exception) {
                println("Error resuming service: ${e.message}")
            }
        }
        println("ServiceLifecycleManager: All services resumed")
    }
    
    /**
     * 获取服务数量
     */
    fun getServiceCount(): Int = lifecycleServices.size
    
    /**
     * 检查是否已初始化
     */
    fun isInitialized(): Boolean = isInitialized
}