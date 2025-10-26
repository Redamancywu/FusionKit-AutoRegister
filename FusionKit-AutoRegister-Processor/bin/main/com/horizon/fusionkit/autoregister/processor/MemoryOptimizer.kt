package com.horizon.fusionkit.autoregister.processor

import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * 内存优化管理器 - 提供内存使用优化策略
 * 
 * 功能特性：
 * - 对象池管理：重用常用对象，减少GC压力
 * - 弱引用缓存：自动释放不再使用的对象
 * - 内存监控：实时监控内存使用情况
 * - 智能清理：根据内存压力自动清理缓存
 * - 字符串内化：减少重复字符串的内存占用
 */
class MemoryOptimizer {
    
    // 字符串内化池 - 减少重复字符串的内存占用
    private val stringPool = ConcurrentHashMap<String, WeakReference<String>>()
    
    // 对象池 - 重用ServiceEntry对象
    private val serviceEntryPool = mutableListOf<AutoRegisterSymbolProcessor.ServiceEntry>()
    private val maxPoolSize = 100
    
    // 内存使用统计
    private val allocatedObjects = AtomicLong(0)
    private val pooledObjects = AtomicLong(0)
    private val internedStrings = AtomicLong(0)
    
    // 内存监控
    private val runtime = Runtime.getRuntime()
    
    /**
     * 字符串内化 - 减少重复字符串的内存占用
     */
    fun internString(str: String): String {
        if (str.isEmpty()) return str
        
        // 检查是否已经内化
        stringPool[str]?.get()?.let { cached ->
            return cached
        }
        
        // 内化新字符串
        stringPool[str] = WeakReference(str)
        internedStrings.incrementAndGet()
        
        // 定期清理失效的弱引用
        if (stringPool.size > 1000) {
            cleanupStringPool()
        }
        
        return str
    }
    
    /**
     * 创建优化的ServiceEntry对象
     */
    fun createServiceEntry(
        className: String,
        name: String,
        type: String,
        priority: Int,
        isObject: Boolean,
        configFile: String = "",
        configKey: String = "",
        enableLifecycle: Boolean = false,
        enableMetrics: Boolean = false,
        pluginId: String = "",
        pluginVersion: String = ""
    ): AutoRegisterSymbolProcessor.ServiceEntry {
        
        // 尝试从对象池获取
        val pooledEntry = synchronized(serviceEntryPool) {
            if (serviceEntryPool.isNotEmpty()) {
                pooledObjects.incrementAndGet()
                serviceEntryPool.removeAt(serviceEntryPool.size - 1)
            } else {
                null
            }
        }
        
        return if (pooledEntry != null) {
            // 重用池中的对象，更新其属性
            pooledEntry.copy(
                className = internString(className),
                name = internString(name),
                type = internString(type),
                priority = priority,
                isObject = isObject,
                configFile = internString(configFile),
                configKey = internString(configKey),
                enableLifecycle = enableLifecycle,
                enableMetrics = enableMetrics,
                pluginId = internString(pluginId),
                pluginVersion = internString(pluginVersion)
            )
        } else {
            // 池为空时创建新对象
            allocatedObjects.incrementAndGet()
            AutoRegisterSymbolProcessor.ServiceEntry(
                className = internString(className),
                name = internString(name),
                type = internString(type),
                priority = priority,
                isObject = isObject,
                configFile = internString(configFile),
                configKey = internString(configKey),
                enableLifecycle = enableLifecycle,
                enableMetrics = enableMetrics,
                pluginId = internString(pluginId),
                pluginVersion = internString(pluginVersion)
            )
        }
    }
    
    /**
     * 回收ServiceEntry对象到对象池
     */
    fun recycleServiceEntry(entry: AutoRegisterSymbolProcessor.ServiceEntry) {
        synchronized(serviceEntryPool) {
            if (serviceEntryPool.size < maxPoolSize) {
                serviceEntryPool.add(entry)
            }
        }
    }
    
    /**
     * 清理字符串池中的失效引用
     */
    private fun cleanupStringPool() {
        val iterator = stringPool.entries.iterator()
        var cleanedCount = 0
        
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (entry.value.get() == null) {
                iterator.remove()
                cleanedCount++
            }
        }
        
        if (cleanedCount > 0) {
            // 更新统计
            internedStrings.addAndGet(-cleanedCount.toLong())
        }
    }
    
    /**
     * 检查内存压力并执行清理
     */
    fun checkMemoryPressure(): MemoryStatus {
        val totalMemory = runtime.totalMemory()
        val freeMemory = runtime.freeMemory()
        val usedMemory = totalMemory - freeMemory
        val maxMemory = runtime.maxMemory()
        
        val memoryUsageRatio = usedMemory.toDouble() / maxMemory
        
        return when {
            memoryUsageRatio > 0.9 -> {
                // 高内存压力，执行激进清理
                performAggressiveCleanup()
                MemoryStatus.HIGH_PRESSURE
            }
            memoryUsageRatio > 0.7 -> {
                // 中等内存压力，执行常规清理
                performRegularCleanup()
                MemoryStatus.MEDIUM_PRESSURE
            }
            else -> MemoryStatus.LOW_PRESSURE
        }
    }
    
    /**
     * 执行常规内存清理
     */
    private fun performRegularCleanup() {
        cleanupStringPool()
        
        // 清理对象池的一半
        synchronized(serviceEntryPool) {
            val halfSize = serviceEntryPool.size / 2
            repeat(halfSize) {
                if (serviceEntryPool.isNotEmpty()) {
                    serviceEntryPool.removeAt(serviceEntryPool.size - 1)
                }
            }
        }
    }
    
    /**
     * 执行激进内存清理
     */
    private fun performAggressiveCleanup() {
        // 清理所有缓存
        stringPool.clear()
        internedStrings.set(0)
        
        synchronized(serviceEntryPool) {
            serviceEntryPool.clear()
        }
        
        // 建议JVM执行垃圾回收
        System.gc()
    }
    
    /**
     * 获取内存使用统计
     */
    fun getMemoryStats(): MemoryStats {
        val totalMemory = runtime.totalMemory()
        val freeMemory = runtime.freeMemory()
        val usedMemory = totalMemory - freeMemory
        val maxMemory = runtime.maxMemory()
        
        return MemoryStats(
            usedMemory = usedMemory,
            freeMemory = freeMemory,
            totalMemory = totalMemory,
            maxMemory = maxMemory,
            memoryUsageRatio = usedMemory.toDouble() / maxMemory,
            allocatedObjects = allocatedObjects.get(),
            pooledObjects = pooledObjects.get(),
            internedStrings = internedStrings.get(),
            stringPoolSize = stringPool.size,
            serviceEntryPoolSize = serviceEntryPool.size
        )
    }
    
    /**
     * 重置统计信息
     */
    fun resetStats() {
        allocatedObjects.set(0)
        pooledObjects.set(0)
        internedStrings.set(0)
        synchronized(serviceEntryPool) {
            serviceEntryPool.clear()
        }
    }
    
    /**
     * 内存状态枚举
     */
    enum class MemoryStatus {
        LOW_PRESSURE,     // 内存使用率 < 70%
        MEDIUM_PRESSURE,  // 内存使用率 70% - 90%
        HIGH_PRESSURE     // 内存使用率 > 90%
    }
    
    /**
     * 内存统计信息
     */
    data class MemoryStats(
        val usedMemory: Long,
        val freeMemory: Long,
        val totalMemory: Long,
        val maxMemory: Long,
        val memoryUsageRatio: Double,
        val allocatedObjects: Long,
        val pooledObjects: Long,
        val internedStrings: Long,
        val stringPoolSize: Int,
        val serviceEntryPoolSize: Int
    ) {
        fun toReadableString(): String {
            val usedMB = usedMemory / (1024 * 1024)
            val totalMB = totalMemory / (1024 * 1024)
            val maxMB = maxMemory / (1024 * 1024)
            val usagePercent = (memoryUsageRatio * 100).toInt()
            
            return """
                Memory Usage: ${usedMB}MB / ${totalMB}MB / ${maxMB}MB (${usagePercent}%)
                Objects: Allocated=${allocatedObjects}, Pooled=${pooledObjects}
                Strings: Interned=${internedStrings}, Pool Size=${stringPoolSize}
                Service Entry Pool: ${serviceEntryPoolSize}
            """.trimIndent()
        }
    }
}