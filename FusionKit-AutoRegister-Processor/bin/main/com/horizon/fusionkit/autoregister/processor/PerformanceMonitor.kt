package com.horizon.fusionkit.autoregister.processor

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import kotlin.time.Duration
import kotlin.time.measureTime

/**
 * 性能监控配置
 */
data class MonitorConfig(
    val enabled: Boolean = true,
    val logLevel: LogLevel = LogLevel.INFO,
    val maxHistorySize: Int = 1000,
    val enableMetrics: Boolean = true,
    val enableTracing: Boolean = false
)

enum class LogLevel {
    DEBUG, INFO, WARN, ERROR
}

/**
 * 服务使用统计
 */
data class ServiceMetrics(
    val serviceName: String,
    var callCount: AtomicLong = AtomicLong(0),
    var totalTime: AtomicLong = AtomicLong(0),
    var errorCount: AtomicLong = AtomicLong(0),
    var lastCallTime: Long = 0
) {
    val averageTime: Double
        get() = if (callCount.get() > 0) totalTime.get().toDouble() / callCount.get() else 0.0
    
    val errorRate: Double
        get() = if (callCount.get() > 0) errorCount.get().toDouble() / callCount.get() else 0.0
}

/**
 * 性能监控器
 */
object PerformanceMonitor {
    
    private var config: MonitorConfig = MonitorConfig()
    private val serviceMetrics = ConcurrentHashMap<String, ServiceMetrics>()
    private val callHistory = mutableListOf<CallRecord>()
    private val listeners = mutableListOf<(ServiceMetrics) -> Unit>()
    
    /**
     * 初始化监控器
     */
    fun initialize(customConfig: MonitorConfig? = null) {
        customConfig?.let { config = it }
        
        if (config.enabled) {
            log("PerformanceMonitor initialized with config: $config", LogLevel.INFO)
        }
    }
    
    /**
     * 记录服务调用
     */
    fun <T> trackCall(serviceName: String, block: () -> T): T {
        if (!config.enabled) return block()
        
        val startTime = System.currentTimeMillis()
        var error: Throwable? = null
        var result: T? = null
        
        val duration = measureTime {
            try {
                result = block()
            } catch (e: Throwable) {
                error = e
                // 不在这里重新抛出异常，让指标更新逻辑先执行
            }
        }
        
        val endTime = System.currentTimeMillis()
        
        // 更新指标
        val metrics = serviceMetrics.getOrPut(serviceName) {
            ServiceMetrics(serviceName)
        }
        
        metrics.callCount.incrementAndGet()
        metrics.totalTime.addAndGet(duration.inWholeMilliseconds)
        metrics.lastCallTime = endTime
        
        if (error != null) {
            metrics.errorCount.incrementAndGet()
            log("Service call failed: $serviceName - ${error?.message}", LogLevel.ERROR)
        }
        
        // 记录调用历史
        synchronized(callHistory) {
            callHistory.add(CallRecord(serviceName, startTime, endTime, error != null))
            if (callHistory.size > config.maxHistorySize) {
                callHistory.removeFirst()
            }
        }
        
        // 触发监听器
        if (config.enableMetrics) {
            listeners.forEach { it(metrics) }
        }
        
        // 记录调用日志
        if (config.logLevel <= LogLevel.INFO) {
            log("Service call: $serviceName - ${duration.inWholeMilliseconds}ms", LogLevel.INFO)
        }
        
        // 现在抛出异常（如果有的话）
        if (error != null) {
            throw error!!
        }
        
        return result!!
    }
    
    /**
     * 获取服务指标
     */
    fun getMetrics(serviceName: String): ServiceMetrics? = serviceMetrics[serviceName]
    
    /**
     * 获取所有服务指标
     */
    fun getAllMetrics(): Map<String, ServiceMetrics> = serviceMetrics.toMap()
    
    /**
     * 获取调用历史
     */
    fun getCallHistory(): List<CallRecord> = synchronized(callHistory) { callHistory.toList() }
    
    /**
     * 添加指标监听器
     */
    fun addMetricsListener(listener: (ServiceMetrics) -> Unit) {
        listeners.add(listener)
    }
    
    /**
     * 清理所有监听器
     */
    fun clearListeners() {
        listeners.clear()
    }
    
    /**
     * 生成性能报告
     */
    fun generateReport(): PerformanceReport {
        val totalCalls = serviceMetrics.values.sumOf { it.callCount.get() }
        val totalErrors = serviceMetrics.values.sumOf { it.errorCount.get() }
        val avgResponseTime = serviceMetrics.values.map { it.averageTime }.average()
        
        return PerformanceReport(
            totalCalls = totalCalls,
            totalErrors = totalErrors,
            averageResponseTime = avgResponseTime,
            serviceCount = serviceMetrics.size,
            topServices = serviceMetrics.values.sortedByDescending { it.callCount.get() }.take(10)
        )
    }
    
    /**
     * 重置所有指标
     */
    fun reset() {
        serviceMetrics.clear()
        synchronized(callHistory) { callHistory.clear() }
        listeners.clear()
        log("PerformanceMonitor reset", LogLevel.INFO)
    }
    
    /**
     * 日志记录
     */
    private fun log(message: String, level: LogLevel) {
        if (level >= config.logLevel) {
            println("[PerformanceMonitor] $level: $message")
        }
    }
}

/**
 * 调用记录
 */
data class CallRecord(
    val serviceName: String,
    val startTime: Long,
    val endTime: Long,
    val isError: Boolean
) {
    val duration: Long get() = endTime - startTime
}

/**
 * 性能报告
 */
data class PerformanceReport(
    val totalCalls: Long,
    val totalErrors: Long,
    val averageResponseTime: Double,
    val serviceCount: Int,
    val topServices: List<ServiceMetrics>
)