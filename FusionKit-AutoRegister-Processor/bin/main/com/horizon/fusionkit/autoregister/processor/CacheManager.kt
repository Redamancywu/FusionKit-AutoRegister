package com.horizon.fusionkit.autoregister.processor

import com.google.devtools.ksp.symbol.KSClassDeclaration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

/**
 * 缓存管理器 - 提供多级缓存机制来优化注解处理性能
 * 
 * 功能特性：
 * - 符号缓存：缓存已解析的类声明
 * - 接口映射缓存：缓存接口到实现类的映射关系
 * - 注解处理结果缓存：缓存注解解析结果
 * - LRU淘汰策略：自动清理过期缓存
 * - 线程安全：支持并发访问
 */
class CacheManager {
    
    private val lock = ReentrantReadWriteLock()
    
    // 符号缓存 - 缓存已解析的类声明
    private val symbolCache = ConcurrentHashMap<String, CachedSymbol>()
    
    // 接口映射缓存 - 缓存接口到实现类的映射关系
    private val interfaceCache = ConcurrentHashMap<String, Set<String>>()
    
    // 注解处理结果缓存 - 缓存注解解析结果
    private val annotationCache = ConcurrentHashMap<String, List<AnnotationResult>>()
    
    // 文件修改时间缓存 - 用于增量处理
    private val fileTimestampCache = ConcurrentHashMap<String, Long>()
    
    // 统计信息
    private val hitCount = AtomicLong(0)
    private val missCount = AtomicLong(0)
    
    companion object {
        private const val MAX_CACHE_SIZE = 1000
        private const val CLEANUP_THRESHOLD = 1200
    }
    
    /**
     * 缓存的符号信息
     */
    data class CachedSymbol(
        val declaration: KSClassDeclaration,
        val qualifiedName: String,
        val lastAccessed: Long = System.currentTimeMillis()
    )
    
    /**
     * 注解处理结果
     */
    data class AnnotationResult(
        val interfaceName: String,
        val serviceEntry: AutoRegisterSymbolProcessor.ServiceEntry,
        val timestamp: Long = System.currentTimeMillis()
    )
    
    /**
     * 获取缓存的符号
     */
    fun getSymbol(qualifiedName: String): KSClassDeclaration? {
        return lock.read {
            symbolCache[qualifiedName]?.let { cached ->
                hitCount.incrementAndGet()
                // 更新访问时间
                symbolCache[qualifiedName] = cached.copy(lastAccessed = System.currentTimeMillis())
                cached.declaration
            } ?: run {
                missCount.incrementAndGet()
                null
            }
        }
    }
    
    /**
     * 缓存符号
     */
    fun putSymbol(qualifiedName: String, declaration: KSClassDeclaration) {
        lock.write {
            symbolCache[qualifiedName] = CachedSymbol(declaration, qualifiedName)
            
            // 检查是否需要清理缓存
            if (symbolCache.size > CLEANUP_THRESHOLD) {
                cleanupSymbolCache()
            }
        }
    }
    
    /**
     * 获取接口映射
     */
    fun getInterfaceMapping(interfaceName: String): Set<String>? {
        return lock.read {
            interfaceCache[interfaceName]?.also { hitCount.incrementAndGet() } ?: run {
                missCount.incrementAndGet()
                null
            }
        }
    }
    
    /**
     * 缓存接口映射
     */
    fun putInterfaceMapping(interfaceName: String, implementations: Set<String>) {
        lock.write {
            interfaceCache[interfaceName] = implementations
        }
    }
    
    /**
     * 获取注解处理结果
     */
    fun getAnnotationResults(className: String): List<AnnotationResult>? {
        return lock.read {
            annotationCache[className]?.also { 
                hitCount.incrementAndGet()
            } ?: run {
                missCount.incrementAndGet()
                null
            }
        }
    }
    
    /**
     * 缓存注解处理结果
     */
    fun putAnnotationResults(className: String, results: List<AnnotationResult>) {
        lock.write {
            annotationCache[className] = results
        }
    }
    
    /**
     * 检查文件是否已修改（用于增量处理）
     */
    fun isFileModified(filePath: String, currentTimestamp: Long): Boolean {
        return lock.read {
            val cachedTimestamp = fileTimestampCache[filePath]
            cachedTimestamp == null || cachedTimestamp < currentTimestamp
        }
    }
    
    /**
     * 更新文件时间戳
     */
    fun updateFileTimestamp(filePath: String, timestamp: Long) {
        lock.write {
            fileTimestampCache[filePath] = timestamp
        }
    }
    
    /**
     * 清理符号缓存（LRU策略）
     */
    private fun cleanupSymbolCache() {
        if (symbolCache.size <= MAX_CACHE_SIZE) return
        
        val sortedEntries = symbolCache.entries.sortedBy { it.value.lastAccessed }
        val toRemove = sortedEntries.take(symbolCache.size - MAX_CACHE_SIZE)
        
        toRemove.forEach { entry ->
            symbolCache.remove(entry.key)
        }
    }
    
    /**
     * 清理所有缓存
     */
    fun clearAll() {
        lock.write {
            symbolCache.clear()
            interfaceCache.clear()
            annotationCache.clear()
            fileTimestampCache.clear()
            hitCount.set(0)
            missCount.set(0)
        }
    }
    
    /**
     * 获取缓存统计信息
     */
    fun getStats(): CacheStats {
        return lock.read {
            val hitCountValue = hitCount.get()
            val missCountValue = missCount.get()
            val totalRequests = hitCountValue + missCountValue
            val hitRate = if (totalRequests > 0) hitCountValue.toDouble() / totalRequests else 0.0
            
            CacheStats(
                symbolCacheSize = symbolCache.size,
                interfaceCacheSize = interfaceCache.size,
                annotationCacheSize = annotationCache.size,
                fileTimestampCacheSize = fileTimestampCache.size,
                hitCount = hitCountValue,
                missCount = missCountValue,
                hitRate = hitRate
            )
        }
    }
    
    /**
     * 缓存统计信息
     */
    data class CacheStats(
        val symbolCacheSize: Int,
        val interfaceCacheSize: Int,
        val annotationCacheSize: Int,
        val fileTimestampCacheSize: Int,
        val hitCount: Long,
        val missCount: Long,
        val hitRate: Double
    )
}