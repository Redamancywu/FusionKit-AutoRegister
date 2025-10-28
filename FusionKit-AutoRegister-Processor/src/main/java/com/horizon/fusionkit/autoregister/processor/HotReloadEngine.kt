package com.horizon.fusionkit.autoregister.processor

import java.io.File
import java.nio.file.*
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlin.reflect.KClass

/**
 * 热更新核心引擎
 * 
 * 功能特性：
 * - 文件系统监听：使用NIO WatchService监听文件变化
 * - 动态类重载：运行时重新编译和加载类
 * - 依赖管理：处理服务间的依赖关系和重载顺序
 * - 回滚机制：重载失败时自动回滚
 * - 事件通知：重载过程的事件回调
 * - 性能监控：重载性能统计和监控
 */
class HotReloadEngine private constructor() {
    
    companion object {
        @Volatile
        private var INSTANCE: HotReloadEngine? = null
        
        fun getInstance(): HotReloadEngine {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: HotReloadEngine().also { INSTANCE = it }
            }
        }
    }
    
    // 配置
    private var config: HotReloadConfiguration = HotReloadConfiguration()
    
    // 状态管理
    private val isRunning = AtomicBoolean(false)
    private val reloadCounter = AtomicLong(0)
    
    // 线程池
    private val watcherThreadId = java.util.concurrent.atomic.AtomicInteger(1)
    private val reloadThreadId = java.util.concurrent.atomic.AtomicInteger(1)
    private val watcherExecutor = Executors.newCachedThreadPool { r ->
        Thread(r, "HotReload-Watcher-${watcherThreadId.getAndIncrement()}").apply {
            isDaemon = true
        }
    }
    private var reloadExecutor: ExecutorService = Executors.newFixedThreadPool(
        config.maxConcurrentReloads
    ) { r ->
        Thread(r, "HotReload-Executor-${reloadThreadId.getAndIncrement()}").apply {
            isDaemon = true
        }
    }
    
    // 监听服务
    private var watchService: WatchService? = null
    private val watchKeys = mutableMapOf<WatchKey, Path>()
    
    // 注册的热更新服务
    private val hotReloadServices = ConcurrentHashMap<String, HotReloadServiceInfo>()
    
    // 重载历史
    private val reloadHistory = ConcurrentLinkedQueue<ReloadRecord>()
    
    // 事件监听器
    private val eventListeners = CopyOnWriteArrayList<HotReloadEventListener>()
    
    /**
     * 初始化热更新引擎
     */
    fun initialize(configuration: HotReloadConfiguration = HotReloadConfiguration()) {
        if (isRunning.get()) {
            throw IllegalStateException("HotReload engine is already running")
        }
        
        this.config = configuration
        
        if (!config.enabled) {
            log("HotReload is disabled")
            return
        }
        
        try {
            watchService = FileSystems.getDefault().newWatchService()
            // 使用最新配置重建重载执行器，确保并发数量生效
            reloadExecutor.shutdownNow()
            reloadExecutor = Executors.newFixedThreadPool(config.maxConcurrentReloads) { r ->
                Thread(r, "HotReload-Executor-${reloadThreadId.getAndIncrement()}").apply {
                    isDaemon = true
                }
            }
            isRunning.set(true)
            
            // 启动文件监听
            startFileWatcher()
            
            log("HotReload engine initialized successfully")
            notifyEvent(HotReloadEvent.EngineStarted(System.currentTimeMillis()))
            
        } catch (e: Exception) {
            log("Failed to initialize HotReload engine: ${e.message}")
            throw e
        }
    }
    
    /**
     * 注册热更新服务
     */
    fun registerService(serviceInfo: HotReloadServiceInfo) {
        if (!config.enabled) return
        
        val serviceName = serviceInfo.name
        hotReloadServices[serviceName] = serviceInfo
        
        // 注册文件监听
        serviceInfo.watchPaths.forEach { path ->
            registerFileWatch(Paths.get(path))
        }
        
        log("Registered hot reload service: $serviceName")
        notifyEvent(HotReloadEvent.ServiceRegistered(serviceName, System.currentTimeMillis()))
    }
    
    /**
     * 手动触发重载
     */
    fun triggerReload(serviceName: String): CompletableFuture<ReloadResult> {
        val serviceInfo = hotReloadServices[serviceName]
            ?: return CompletableFuture.completedFuture(
                ReloadResult.failure("Service not found: $serviceName")
            )
        
        return performReload(serviceInfo, ReloadTrigger.Manual)
    }
    
    /**
     * 批量重载
     */
    fun triggerBatchReload(serviceNames: List<String>): CompletableFuture<Map<String, ReloadResult>> {
        val futures = serviceNames.associateWith { serviceName ->
            triggerReload(serviceName)
        }
        
        return CompletableFuture.allOf(*futures.values.toTypedArray())
            .thenApply { 
                futures.mapValues { (_, future) -> future.get() }
            }
    }
    
    /**
     * 停止热更新引擎
     */
    fun shutdown() {
        if (!isRunning.compareAndSet(true, false)) {
            return
        }
        
        try {
            // 首先停止文件监听器线程
            watcherExecutor.shutdown()
            
            // 然后关闭WatchService
            watchService?.close()
            watchKeys.clear()
            
            // 关闭重载执行器
            reloadExecutor.shutdown()
            
            // 等待线程池关闭
            if (!watcherExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                watcherExecutor.shutdownNow()
            }
            if (!reloadExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                reloadExecutor.shutdownNow()
            }
            
            log("HotReload engine shutdown successfully")
            notifyEvent(HotReloadEvent.EngineStopped(System.currentTimeMillis()))
            
        } catch (e: Exception) {
            log("Error during HotReload engine shutdown: ${e.message}")
        }
    }
    
    /**
     * 添加事件监听器
     */
    fun addEventListener(listener: HotReloadEventListener) {
        eventListeners.add(listener)
    }
    
    /**
     * 移除事件监听器
     */
    fun removeEventListener(listener: HotReloadEventListener) {
        eventListeners.remove(listener)
    }
    
    /**
     * 获取重载统计信息
     */
    fun getReloadStatistics(): ReloadStatistics {
        val durations = reloadHistory.mapNotNull { it.duration }
        return ReloadStatistics(
            totalReloads = reloadCounter.get(),
            successfulReloads = reloadHistory.count { it.success },
            failedReloads = reloadHistory.count { !it.success },
            averageReloadTime = if (durations.isNotEmpty()) durations.average() else 0.0,
            lastReloadTime = reloadHistory.lastOrNull()?.timestamp
        )
    }
    
    /**
     * 获取重载历史
     */
    fun getReloadHistory(): List<ReloadRecord> {
        return reloadHistory.toList()
    }
    
    // 私有方法
    
    private fun startFileWatcher() {
        watcherExecutor.submit {
            while (isRunning.get()) {
                try {
                    val service = watchService
                    if (service == null) {
                        log("WatchService is null, stopping file watcher")
                        break
                    }
                    
                    val key = service.poll(config.watchInterval, TimeUnit.MILLISECONDS)
                    if (key != null) {
                        processWatchEvents(key)
                        if (!key.reset()) {
                            log("WatchKey is no longer valid, removing from watchKeys")
                            watchKeys.remove(key)
                        }
                    }
                } catch (e: InterruptedException) {
                    Thread.currentThread().interrupt()
                    break
                } catch (e: java.nio.file.ClosedWatchServiceException) {
                    // WatchService已关闭，正常退出
                    log("WatchService closed, stopping file watcher")
                    break
                } catch (e: Exception) {
                    log("Error in file watcher: ${e.javaClass.simpleName}: ${e.message}")
                    e.printStackTrace()
                }
            }
        }
    }
    
    private fun registerFileWatch(path: Path) {
        try {
            val parent = if (Files.exists(path) && Files.isDirectory(path)) {
                path
            } else {
                path.parent ?: return
            }
            
            // 确保父目录存在
            if (!Files.exists(parent)) {
                Files.createDirectories(parent)
            }
            
            val key = parent.register(
                watchService,
                StandardWatchEventKinds.ENTRY_MODIFY,
                StandardWatchEventKinds.ENTRY_CREATE,
                StandardWatchEventKinds.ENTRY_DELETE
            )
            watchKeys[key] = parent
        } catch (e: Exception) {
            log("Failed to register file watch for $path: ${e.message}")
        }
    }
    
    private fun processWatchEvents(key: WatchKey) {
        val dir = watchKeys[key] ?: return
        
        for (event in key.pollEvents()) {
            val kind = event.kind()
            
            if (kind == StandardWatchEventKinds.OVERFLOW) {
                continue
            }
            
            @Suppress("UNCHECKED_CAST")
            val filename = event.context() as Path
            val fullPath = dir.resolve(filename)
            
            // 查找受影响的服务
            val affectedServices = findAffectedServices(fullPath)
            
            affectedServices.forEach { serviceInfo ->
                when (serviceInfo.reloadStrategy) {
                    ReloadStrategy.IMMEDIATE -> {
                        scheduleReload(serviceInfo, ReloadTrigger.FileChange(fullPath.toString()))
                    }
                    ReloadStrategy.DELAYED -> {
                        scheduleDelayedReload(serviceInfo, ReloadTrigger.FileChange(fullPath.toString()))
                    }
                    ReloadStrategy.BATCH -> {
                        scheduleBatchReload(serviceInfo, ReloadTrigger.FileChange(fullPath.toString()))
                    }
                    ReloadStrategy.MANUAL -> {
                        // 手动模式不自动重载
                    }
                }
            }
        }
    }
    
    private fun findAffectedServices(changedPath: Path): List<HotReloadServiceInfo> {
        return hotReloadServices.values.filter { serviceInfo ->
            serviceInfo.watchPaths.any { watchPath ->
                try {
                    val watchPathObj = Paths.get(watchPath)
                    val changedPathStr = changedPath.toString()
                    val watchPathStr = watchPathObj.toString()
                    
                    // 检查是否是同一个文件或者文件在监听的目录下
                    changedPathStr == watchPathStr ||
                    changedPathStr.startsWith(watchPathStr) ||
                    watchPathStr.startsWith(changedPathStr)
                } catch (e: Exception) {
                    log("Error matching paths: $watchPath vs $changedPath - ${e.message}")
                    false
                }
            }
        }
    }
    
    private fun scheduleReload(serviceInfo: HotReloadServiceInfo, trigger: ReloadTrigger) {
        reloadExecutor.submit {
            performReload(serviceInfo, trigger)
        }
    }
    
    private fun scheduleDelayedReload(serviceInfo: HotReloadServiceInfo, trigger: ReloadTrigger) {
        // 延迟重载实现（可以使用ScheduledExecutorService）
        reloadExecutor.submit {
            Thread.sleep(config.delayedReloadInterval)
            performReload(serviceInfo, trigger)
        }
    }
    
    private fun scheduleBatchReload(serviceInfo: HotReloadServiceInfo, trigger: ReloadTrigger) {
        // 批量重载实现（收集一段时间内的变化）
        // 这里简化实现，实际可以使用更复杂的批量策略
        scheduleDelayedReload(serviceInfo, trigger)
    }
    
    private fun performReload(serviceInfo: HotReloadServiceInfo, trigger: ReloadTrigger): CompletableFuture<ReloadResult> {
        val reloadId = reloadCounter.incrementAndGet()
        val startTime = System.currentTimeMillis()
        
        return CompletableFuture.supplyAsync({
            try {
                log("Starting reload for service: ${serviceInfo.name} (ID: $reloadId)")
                notifyEvent(HotReloadEvent.ReloadStarted(serviceInfo.name, reloadId, startTime))
                
                // 执行重载前钩子
                if (!executeBeforeReloadHook(serviceInfo)) {
                    return@supplyAsync ReloadResult.cancelled("Before reload hook returned false")
                }
                
                // 保存当前实例（用于回滚）
                val oldInstance = getCurrentInstance(serviceInfo)
                
                // 执行重载
                val newInstance = reloadServiceInstance(serviceInfo)
                
                // 更新服务注册
                updateServiceRegistration(serviceInfo, newInstance)
                
                // 执行重载后钩子
                executeAfterReloadHook(serviceInfo, true, oldInstance, newInstance)
                
                val endTime = System.currentTimeMillis()
                val duration = endTime - startTime
                
                val result = ReloadResult.success("Service reloaded successfully", duration)
                recordReloadHistory(serviceInfo.name, reloadId, true, duration, trigger)
                
                log("Successfully reloaded service: ${serviceInfo.name} in ${duration}ms")
                notifyEvent(HotReloadEvent.ReloadCompleted(serviceInfo.name, reloadId, endTime, true))
                
                result
                
            } catch (e: Exception) {
                val endTime = System.currentTimeMillis()
                val duration = endTime - startTime
                
                log("Failed to reload service: ${serviceInfo.name} - ${e.message}")
                
                // 执行失败钩子
                val shouldRetry = executeReloadFailedHook(serviceInfo, e, 0)
                
                val result = ReloadResult.failure("Reload failed: ${e.message}", e)
                recordReloadHistory(serviceInfo.name, reloadId, false, duration, trigger)
                
                notifyEvent(HotReloadEvent.ReloadCompleted(serviceInfo.name, reloadId, endTime, false))
                
                // 如果启用回滚且失败钩子不要求重试，则执行回滚
                if (serviceInfo.enableRollback && !shouldRetry) {
                    performRollback(serviceInfo)
                }
                
                result
            }
        }, reloadExecutor)
    }
    
    private fun executeBeforeReloadHook(serviceInfo: HotReloadServiceInfo): Boolean {
        if (serviceInfo.beforeReload.isEmpty()) return true
        
        return try {
            // 这里需要通过反射调用钩子方法
            // 实际实现需要获取类实例并调用指定方法
            true
        } catch (e: Exception) {
            log("Before reload hook failed for ${serviceInfo.name}: ${e.message}")
            false
        }
    }
    
    private fun executeAfterReloadHook(serviceInfo: HotReloadServiceInfo, success: Boolean, oldInstance: Any?, newInstance: Any?) {
        if (serviceInfo.afterReload.isEmpty()) return
        
        try {
            // 通过反射调用钩子方法
            // 实际实现需要获取类实例并调用指定方法
        } catch (e: Exception) {
            log("After reload hook failed for ${serviceInfo.name}: ${e.message}")
        }
    }
    
    private fun executeReloadFailedHook(serviceInfo: HotReloadServiceInfo, error: Throwable, retryCount: Int): Boolean {
        if (serviceInfo.onReloadFailed.isEmpty()) return false
        
        return try {
            // 通过反射调用钩子方法
            // 实际实现需要获取类实例并调用指定方法
            false
        } catch (e: Exception) {
            log("Reload failed hook failed for ${serviceInfo.name}: ${e.message}")
            false
        }
    }
    
    private fun getCurrentInstance(serviceInfo: HotReloadServiceInfo): Any? {
        // 获取当前服务实例
        // 实际实现需要从服务注册表中获取
        return null
    }
    
    private fun reloadServiceInstance(serviceInfo: HotReloadServiceInfo): Any {
        // 重新编译和加载类
        // 这是热更新的核心逻辑，需要：
        // 1. 重新编译源文件
        // 2. 创建新的类加载器
        // 3. 加载新的类
        // 4. 创建新的实例
        throw NotImplementedError("Service instance reloading not implemented yet")
    }
    
    private fun updateServiceRegistration(serviceInfo: HotReloadServiceInfo, newInstance: Any) {
        // 更新服务注册表
        // 实际实现需要更新依赖注入容器中的服务实例
    }
    
    private fun performRollback(serviceInfo: HotReloadServiceInfo) {
        try {
            log("Performing rollback for service: ${serviceInfo.name}")
            // 回滚到上一个版本
            // 实际实现需要恢复之前保存的实例
        } catch (e: Exception) {
            log("Rollback failed for ${serviceInfo.name}: ${e.message}")
        }
    }
    
    private fun recordReloadHistory(serviceName: String, reloadId: Long, success: Boolean, duration: Long, trigger: ReloadTrigger) {
        val record = ReloadRecord(
            id = reloadId,
            serviceName = serviceName,
            timestamp = System.currentTimeMillis(),
            success = success,
            duration = duration,
            trigger = trigger
        )
        
        reloadHistory.offer(record)
        
        // 保持历史记录数量限制
        while (reloadHistory.size > config.reloadHistorySize) {
            reloadHistory.poll()
        }
    }
    
    private fun notifyEvent(event: HotReloadEvent) {
        eventListeners.forEach { listener ->
            try {
                listener.onEvent(event)
            } catch (e: Exception) {
                log("Event listener failed: ${e.message}")
            }
        }
    }
    
    private fun log(message: String) {
        if (config.enableLogging) {
            println("[HotReload] $message")
        }
    }
}

/**
 * 热更新配置
 */
data class HotReloadConfiguration(
    val enabled: Boolean = false,
    val watchInterval: Long = 1000L,
    val enableGlobalRollback: Boolean = true,
    val globalReloadTimeout: Long = 10000L,
    val maxConcurrentReloads: Int = 5,
    val enableEventNotification: Boolean = true,
    val reloadHistorySize: Int = 10,
    val disableInProduction: Boolean = true,
    val delayedReloadInterval: Long = 2000L,
    val enableLogging: Boolean = true
)

/**
 * 热更新服务信息
 */
data class HotReloadServiceInfo(
    val name: String,
    val interfaces: List<KClass<*>>,
    val watchPaths: List<String>,
    val reloadStrategy: ReloadStrategy,
    val enableRollback: Boolean,
    val maxRetries: Int,
    val reloadTimeout: Long,
    val dependencies: List<KClass<*>>,
    val beforeReload: String,
    val afterReload: String,
    val onReloadFailed: String,
    val enableMetrics: Boolean,
    val enableLogging: Boolean
)

/**
 * 重载结果
 */
sealed class ReloadResult {
    abstract val message: String
    abstract val timestamp: Long
    
    data class Success(
        override val message: String,
        override val timestamp: Long = System.currentTimeMillis(),
        val duration: Long
    ) : ReloadResult()
    
    data class Failure(
        override val message: String,
        override val timestamp: Long = System.currentTimeMillis(),
        val error: Throwable? = null
    ) : ReloadResult()
    
    data class Cancelled(
        override val message: String,
        override val timestamp: Long = System.currentTimeMillis()
    ) : ReloadResult()
    
    companion object {
        fun success(message: String, duration: Long) = Success(message, duration = duration)
        fun failure(message: String, error: Throwable? = null) = Failure(message, error = error)
        fun cancelled(message: String) = Cancelled(message)
    }
}

/**
 * 重载触发器
 */
sealed class ReloadTrigger {
    object Manual : ReloadTrigger()
    data class FileChange(val filePath: String) : ReloadTrigger()
    data class ApiCall(val caller: String) : ReloadTrigger()
    data class Scheduled(val scheduleName: String) : ReloadTrigger()
}

/**
 * 重载记录
 */
data class ReloadRecord(
    val id: Long,
    val serviceName: String,
    val timestamp: Long,
    val success: Boolean,
    val duration: Long,
    val trigger: ReloadTrigger
)

/**
 * 重载统计信息
 */
data class ReloadStatistics(
    val totalReloads: Long,
    val successfulReloads: Int,
    val failedReloads: Int,
    val averageReloadTime: Double,
    val lastReloadTime: Long?
)

/**
 * 热更新事件
 */
sealed class HotReloadEvent {
    data class EngineStarted(val timestamp: Long) : HotReloadEvent()
    data class EngineStopped(val timestamp: Long) : HotReloadEvent()
    data class ServiceRegistered(val serviceName: String, val timestamp: Long) : HotReloadEvent()
    data class ReloadStarted(val serviceName: String, val reloadId: Long, val timestamp: Long) : HotReloadEvent()
    data class ReloadCompleted(val serviceName: String, val reloadId: Long, val timestamp: Long, val success: Boolean) : HotReloadEvent()
}

/**
 * 热更新事件监听器
 */
interface HotReloadEventListener {
    fun onEvent(event: HotReloadEvent)
}