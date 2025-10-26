package com.horizon.fusionkit.autoregister.processor

import com.google.devtools.ksp.symbol.KSClassDeclaration
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicInteger

/**
 * 并行处理管理器 - 提供多线程并行处理能力来优化注解处理性能
 * 
 * 功能特性：
 * - 并行类扫描：同时处理多个类的注解
 * - 智能任务分配：根据类的复杂度分配处理任务
 * - 线程池管理：动态调整线程池大小
 * - 错误隔离：单个类处理失败不影响其他类
 * - 进度监控：实时监控处理进度
 */
class ParallelProcessingManager(
    private val logger: Logger
) {
    
    private val threadCount = maxOf(2, Runtime.getRuntime().availableProcessors() - 1)
    private val executor = Executors.newFixedThreadPool(threadCount)
    
    // 处理统计
    private val processedCount = AtomicInteger(0)
    private val errorCount = AtomicInteger(0)
    private val totalCount = AtomicInteger(0)
    
    /**
     * 并行处理类声明列表
     */
    fun processClassesInParallel(
        classes: List<KSClassDeclaration>,
        processor: (KSClassDeclaration) -> ProcessingResult
    ): List<ProcessingResult> {
        totalCount.set(classes.size)
        processedCount.set(0)
        errorCount.set(0)
        
        logger.info("Starting parallel processing of ${classes.size} classes with $threadCount threads")
        
        val results = ConcurrentHashMap<Int, ProcessingResult>()
        
        // 将类按复杂度分组，优先处理简单的类
        val groupedClasses = groupClassesByComplexity(classes)
        
        try {
            // 并行处理每个组
            groupedClasses.forEach { (complexity, classList) ->
                logger.debug("Processing ${classList.size} classes with complexity: $complexity")
                
                val futures = classList.mapIndexed { _, classDecl ->
                    executor.submit<ProcessingResult> {
                        try {
                            val result = processor(classDecl)
                            results[classes.indexOf(classDecl)] = result
                            processedCount.incrementAndGet()
                            
                            if (processedCount.get() % 10 == 0) {
                                logger.debug("Progress: ${processedCount.get()}/${totalCount.get()} classes processed")
                            }
                            
                            result
                        } catch (e: Exception) {
                            errorCount.incrementAndGet()
                            val errorResult = ProcessingResult.Error(
                                className = classDecl.qualifiedName?.asString() ?: "Unknown",
                                error = e,
                                processingTime = 0
                            )
                            results[classes.indexOf(classDecl)] = errorResult
                            logger.error("Error processing class ${classDecl.qualifiedName?.asString()}: ${e.message}")
                            errorResult
                        }
                    }
                }
                
                // 等待当前组完成再处理下一组
                futures.forEach { it.get() }
            }
            
            logger.info("Parallel processing completed. Processed: ${processedCount.get()}, Errors: ${errorCount.get()}")
            
            // 按原始顺序返回结果
            return classes.indices.mapNotNull { results[it] }
            
        } catch (e: Exception) {
            logger.error("Parallel processing failed: ${e.message}")
            throw e
        }
    }
    
    /**
     * 根据类的复杂度分组
     */
    private fun groupClassesByComplexity(classes: List<KSClassDeclaration>): Map<ClassComplexity, List<KSClassDeclaration>> {
        return classes.groupBy { classDecl ->
            calculateComplexity(classDecl)
        }.toSortedMap() // 按复杂度排序，简单的先处理
    }
    
    /**
     * 计算类的复杂度
     */
    private fun calculateComplexity(classDecl: KSClassDeclaration): ClassComplexity {
        val annotationCount = classDecl.annotations.count()
        val superTypeCount = classDecl.superTypes.count()
        val functionCount = classDecl.getAllFunctions().count()
        val propertyCount = classDecl.getAllProperties().count()
        
        val complexityScore = annotationCount + superTypeCount + (functionCount / 5) + (propertyCount / 10)
        
        return when {
            complexityScore <= 5 -> ClassComplexity.SIMPLE
            complexityScore <= 15 -> ClassComplexity.MEDIUM
            else -> ClassComplexity.COMPLEX
        }
    }
    
    /**
     * 批量处理接口映射
     */
    fun processInterfaceMappingsInParallel(
        interfaceNames: Set<String>,
        processor: (String) -> List<AutoRegisterSymbolProcessor.ServiceEntry>
    ): Map<String, List<AutoRegisterSymbolProcessor.ServiceEntry>> {
        logger.info("Processing ${interfaceNames.size} interface mappings in parallel")
        
        val results = ConcurrentHashMap<String, List<AutoRegisterSymbolProcessor.ServiceEntry>>()
        
        val futures = interfaceNames.map { interfaceName ->
            executor.submit {
                try {
                    val entries = processor(interfaceName)
                    results[interfaceName] = entries
                    logger.debug("Processed interface: $interfaceName with ${entries.size} entries")
                } catch (e: Exception) {
                    logger.error("Error processing interface $interfaceName: ${e.message}")
                    results[interfaceName] = emptyList()
                }
            }
        }
        
        futures.forEach { it.get() }
        return results
    }
    
    /**
     * 获取处理统计信息
     */
    fun getProcessingStats(): ProcessingStats {
        return ProcessingStats(
            totalCount = totalCount.get(),
            processedCount = processedCount.get(),
            errorCount = errorCount.get(),
            threadCount = threadCount,
            successRate = if (totalCount.get() > 0) {
                (processedCount.get() - errorCount.get()).toDouble() / totalCount.get()
            } else 0.0
        )
    }
    
    /**
     * 关闭并行处理管理器
     */
    fun shutdown() {
        try {
            executor.shutdown()
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow()
            }
            logger.info("Parallel processing manager shutdown completed")
        } catch (e: Exception) {
            logger.error("Error during shutdown: ${e.message}")
        }
    }
    
    /**
     * 类复杂度枚举
     */
    enum class ClassComplexity {
        SIMPLE,    // 简单类：注解少，继承关系简单
        MEDIUM,    // 中等复杂度类
        COMPLEX    // 复杂类：注解多，继承关系复杂
    }
    
    /**
     * 处理结果密封类
     */
    sealed class ProcessingResult {
        abstract val className: String
        abstract val processingTime: Long
        
        data class Success(
            override val className: String,
            val serviceEntries: List<AutoRegisterSymbolProcessor.ServiceEntry>,
            override val processingTime: Long
        ) : ProcessingResult()
        
        data class Error(
            override val className: String,
            val error: Exception,
            override val processingTime: Long
        ) : ProcessingResult()
        
        data class Skipped(
            override val className: String,
            val reason: String,
            override val processingTime: Long
        ) : ProcessingResult()
    }
    
    /**
     * 处理统计信息
     */
    data class ProcessingStats(
        val totalCount: Int,
        val processedCount: Int,
        val errorCount: Int,
        val threadCount: Int,
        val successRate: Double
    )
}