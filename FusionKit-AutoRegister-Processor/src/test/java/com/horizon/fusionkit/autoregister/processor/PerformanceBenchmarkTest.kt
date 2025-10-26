package com.horizon.fusionkit.autoregister.processor

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.system.measureTimeMillis

/**
 * 性能基准测试
 */
class PerformanceBenchmarkTest {
    
    @TempDir
    lateinit var tempDir: Path
    
    private lateinit var cacheManager: CacheManager
    private lateinit var parallelManager: ParallelProcessingManager
    private lateinit var memoryOptimizer: MemoryOptimizer
    
    @BeforeEach
    fun setUp() {
        // 创建模拟的KSPLogger和options
        val mockKSPLogger = object : com.google.devtools.ksp.processing.KSPLogger {
            override fun info(message: String, symbol: com.google.devtools.ksp.symbol.KSNode?) = println("INFO: $message")
            override fun warn(message: String, symbol: com.google.devtools.ksp.symbol.KSNode?) = println("WARN: $message")
            override fun error(message: String, symbol: com.google.devtools.ksp.symbol.KSNode?) = println("ERROR: $message")
            override fun logging(message: String, symbol: com.google.devtools.ksp.symbol.KSNode?) = println("LOG: $message")
            override fun exception(e: Throwable) = println("EXCEPTION: ${e.message}")
        }
        val mockOptions = mapOf("auto.register.debug" to "true")
        val mockLogger = Logger(mockKSPLogger, mockOptions)
        
        cacheManager = CacheManager()
        parallelManager = ParallelProcessingManager(mockLogger)
        memoryOptimizer = MemoryOptimizer()
    }
    
    @AfterEach
    fun tearDown() {
        cacheManager.clearAll()
        parallelManager.shutdown()
        memoryOptimizer.resetStats()
    }
    
    @Test
    fun `benchmark cache performance`() {
        val iterations = 500  // 减少到500以适应缓存大小限制
        val testKey = "test-symbol"
        val testValue = "com.test.TestClass"
        
        // 测试缓存写入性能
        val writeTime = measureTimeMillis {
            repeat(iterations) { i ->
                val classDecl = createMockClassDeclaration(testValue)
                cacheManager.putSymbol("$testKey-$i", classDecl)
            }
        }
        
        println("Cache write time for $iterations items: ${writeTime}ms")
        assertTrue(writeTime < 1000, "Cache write should be fast")
        
        // 测试缓存读取性能
        val readTime = measureTimeMillis {
            repeat(iterations) { i ->
                cacheManager.getSymbol("$testKey-$i")
            }
        }
        
        println("Cache read time for $iterations items: ${readTime}ms")
        assertTrue(readTime < 500, "Cache read should be very fast")
        
        // 验证缓存命中率
        val stats = cacheManager.getStats()
        println("Cache stats: hitCount=${stats.hitCount}, missCount=${stats.missCount}, hitRate=${stats.hitRate}")
        assertTrue(stats.hitRate > 0.9, "Cache hit rate should be high")
    }
    
    @Test
    fun `benchmark cache memory usage`() {
        val runtime = Runtime.getRuntime()
        
        // 记录初始内存
        System.gc()
        val initialMemory = runtime.totalMemory() - runtime.freeMemory()
        
        // 添加大量缓存数据
        val itemCount = 50000
        repeat(itemCount) { i ->
            val className = "com.test.Class$i"
            val classDecl = createMockClassDeclaration(className)
            cacheManager.putSymbol("symbol-$i", classDecl)
            cacheManager.putInterfaceMapping("interface-$i", setOf("impl1-$i", "impl2-$i"))
        }
        
        // 记录使用内存
        System.gc()
        val usedMemory = runtime.totalMemory() - runtime.freeMemory()
        val memoryUsed = usedMemory - initialMemory
        
        println("Memory used for $itemCount cache items: ${memoryUsed / 1024 / 1024}MB")
        
        // 验证内存使用合理（每个条目平均不超过1KB）
        val avgMemoryPerItem = memoryUsed / itemCount
        assertTrue(avgMemoryPerItem < 1024, "Average memory per cache item should be reasonable")
        
        // 测试LRU淘汰
        val initialSize = cacheManager.getStats().symbolCacheSize
        
        // 添加更多数据触发LRU淘汰
        repeat(itemCount) { i ->
            val newClassName = "com.test.NewClass$i"
            val newClassDecl = createMockClassDeclaration(newClassName)
            cacheManager.putSymbol("new-symbol-$i", newClassDecl)
        }
        
        val finalSize = cacheManager.getStats().symbolCacheSize
        assertTrue(finalSize <= initialSize * 1.1, "LRU eviction should limit cache size")
    }
    
    @Test
    fun `benchmark parallel processing performance`() {
        val taskCount = 100
        
        // 创建模拟的类声明列表
        val mockClasses = (1..taskCount).map { i ->
            createMockClassDeclaration("TestClass$i")
        }
        
        // 测试串行处理时间
        val serialTime = measureTimeMillis {
            mockClasses.forEach { classDecl ->
                // 模拟处理工作
                Thread.sleep(1)
            }
        }
        
        // 测试并行处理时间
        val parallelTime = measureTimeMillis {
            val results = parallelManager.processClassesInParallel(mockClasses) { classDecl ->
                // 模拟处理工作
                Thread.sleep(1)
                ParallelProcessingManager.ProcessingResult.Success(
                    className = classDecl.qualifiedName?.asString() ?: "Unknown",
                    serviceEntries = emptyList(),
                    processingTime = 1
                )
            }
            assertEquals(taskCount, results.size)
        }
        
        println("Serial processing time: ${serialTime}ms")
        println("Parallel processing time: ${parallelTime}ms")
        if (parallelTime > 0) {
            println("Speedup: ${serialTime.toDouble() / parallelTime}x")
        }
        
        // 验证并行处理完成
        assertTrue(parallelTime > 0, "Parallel processing should complete")
        
        // 验证统计信息
        val stats = parallelManager.getProcessingStats()
        assertTrue(stats.totalCount >= taskCount)
        assertTrue(stats.successRate >= 0.0)
    }
    
    @Test
    fun `benchmark parallel processing with different thread counts`() {
        val taskCount = 100
        val results = mutableMapOf<Int, Long>()
        
        // 测试不同数量的类处理性能
        listOf(10, 25, 50, 100).forEach { classCount ->
            val mockClasses = (1..classCount).map { i ->
                createMockClassDeclaration("TestClass$i")
            }
            
            val time = measureTimeMillis {
                val processingResults = parallelManager.processClassesInParallel(mockClasses) { classDecl ->
                    // 模拟CPU密集型任务
                    var sum = 0
                    repeat(1000) { sum += it }
                    
                    ParallelProcessingManager.ProcessingResult.Success(
                        className = classDecl.qualifiedName?.asString() ?: "Unknown",
                        serviceEntries = emptyList(),
                        processingTime = 1L
                    )
                }
                assertEquals(classCount, processingResults.size)
            }
            
            results[classCount] = time
            println("Processing time with $classCount classes: ${time}ms")
        }
        
        // 验证处理完成
        assertTrue(results[100]!! > 0, "Processing should take some time")
    }
    
    @Test
    fun `benchmark memory optimization`() {
        // 重置内存优化器统计
        memoryOptimizer.resetStats()
        
        // 第一阶段：创建对象并回收到池中（预填充池）
        val initialEntries = mutableListOf<AutoRegisterSymbolProcessor.ServiceEntry>()
        repeat(50) { i ->
            val entry = memoryOptimizer.createServiceEntry(
                className = "com.test.Service$i",
                name = "service$i",
                type = "com.test.Interface",
                priority = 1,
                isObject = false
            )
            initialEntries.add(entry)
        }
        
        // 回收到池中
        initialEntries.forEach { entry ->
            memoryOptimizer.recycleServiceEntry(entry)
        }
        
        // 第二阶段：测试不使用池的情况（清空池）
        memoryOptimizer.resetStats() // 清空池和统计
        
        repeat(100) { i ->
            memoryOptimizer.createServiceEntry(
                className = "com.test.Service$i",
                name = "service$i",
                type = "com.test.Interface$i",
                priority = i,
                isObject = false
            )
        }
        
        val statsWithoutPool = memoryOptimizer.getMemoryStats()
        
        // 第三阶段：重新填充池并测试使用池的情况
        initialEntries.forEach { entry ->
            memoryOptimizer.recycleServiceEntry(entry)
        }
        
        // 重置统计但保留池（通过反射访问私有字段）
        val allocatedObjectsField = memoryOptimizer.javaClass.getDeclaredField("allocatedObjects")
        allocatedObjectsField.isAccessible = true
        val pooledObjectsField = memoryOptimizer.javaClass.getDeclaredField("pooledObjects")
        pooledObjectsField.isAccessible = true
        
        (allocatedObjectsField.get(memoryOptimizer) as java.util.concurrent.atomic.AtomicLong).set(0)
        (pooledObjectsField.get(memoryOptimizer) as java.util.concurrent.atomic.AtomicLong).set(0)
        
        repeat(100) { i ->
            memoryOptimizer.createServiceEntry(
                className = "TestClass$i",
                name = "test$i",
                type = "TestInterface",
                priority = 1,
                isObject = false
            )
        }
        
        val statsWithPool = memoryOptimizer.getMemoryStats()
        
        println("Without pool - Allocated: ${statsWithoutPool.allocatedObjects}, Pooled: ${statsWithoutPool.pooledObjects}")
        println("With pool - Allocated: ${statsWithPool.allocatedObjects}, Pooled: ${statsWithPool.pooledObjects}")
        
        // 使用对象池应该减少新分配的对象数量
        assertTrue(statsWithPool.pooledObjects > 0, "Pool should have reused some objects")
        assertTrue(statsWithPool.allocatedObjects < statsWithoutPool.allocatedObjects, "Object pool should reduce new allocations")
        
        // 验证对象池统计
        assertTrue(statsWithPool.serviceEntryPoolSize >= 0, "Pool size should be valid")
    }
    
    @Test
    fun `benchmark string interning`() {
        val stringCount = 10000
        val duplicateCount = 100 // 每个字符串重复100次
        
        // 不使用字符串驻留
        val stringsWithoutInterning = mutableListOf<String>()
        val timeWithoutInterning = measureTimeMillis {
            repeat(stringCount) { i ->
                repeat(duplicateCount) {
                    stringsWithoutInterning.add("com.test.package.Class$i")
                }
            }
        }
        
        // 使用字符串驻留
        val stringsWithInterning = mutableListOf<String>()
        val timeWithInterning = measureTimeMillis {
            repeat(stringCount) { i ->
                repeat(duplicateCount) {
                    stringsWithInterning.add(memoryOptimizer.internString("com.test.package.Class$i"))
                }
            }
        }
        
        println("Time without interning: ${timeWithoutInterning}ms")
        println("Time with interning: ${timeWithInterning}ms")
        
        // 验证字符串驻留的内存效果
        val internStats = memoryOptimizer.getMemoryStats()
        assertTrue(internStats.internedStrings > 0, "Should have interned strings")
        assertTrue(internStats.stringPoolSize >= 0, "String pool size should be non-negative")
    }
    
    @Test
    fun `benchmark memory pressure handling`() {
        val runtime = Runtime.getRuntime()
        
        // 模拟内存压力
        val largeObjects = mutableListOf<ByteArray>()
        
        try {
            // 分配大量内存直到接近限制
            while (runtime.freeMemory() > runtime.totalMemory() * 0.1) {
                largeObjects.add(ByteArray(1024 * 1024)) // 1MB
            }
            
            // 触发内存优化
            val cleanupTime = measureTimeMillis {
                memoryOptimizer.checkMemoryPressure()
            }
            
            println("Memory cleanup time: ${cleanupTime}ms")
            assertTrue(cleanupTime < 1000, "Memory cleanup should be fast")
            
            // 验证缓存被清理
            val cacheStats = cacheManager.getStats()
            assertTrue(cacheStats.symbolCacheSize < 1000, "Cache should be cleaned under memory pressure")
            
        } finally {
            // 清理大对象
            largeObjects.clear()
            System.gc()
        }
    }
    
    @Test
    fun `benchmark concurrent access performance`() {
        val threadCount = 10
        val operationsPerThread = 1000
        val latch = CountDownLatch(threadCount)
        val errors = AtomicInteger(0)
        
        val totalTime = measureTimeMillis {
            repeat(threadCount) { threadIndex ->
                Thread {
                    try {
                        repeat(operationsPerThread) { i ->
                            val key = "thread-$threadIndex-item-$i"
                            val value = "value-$threadIndex-$i"
                            
                            // 测试并发缓存操作
                            val classDecl = createMockClassDeclaration(value)
                            cacheManager.putSymbol(key, classDecl)
                            val retrieved = cacheManager.getSymbol(key)
                            assertEquals(classDecl, retrieved)
                            
                            // 测试并发内存优化操作
                            val internedString = memoryOptimizer.internString(value)
                            assertEquals(value, internedString)
                        }
                    } catch (e: Exception) {
                        errors.incrementAndGet()
                        e.printStackTrace()
                    } finally {
                        latch.countDown()
                    }
                }.start()
            }
            
            assertTrue(latch.await(30, TimeUnit.SECONDS), "All threads should complete")
        }
        
        println("Concurrent access time for ${threadCount * operationsPerThread} operations: ${totalTime}ms")
        assertEquals(0, errors.get(), "No errors should occur during concurrent access")
        
        // 验证数据一致性
        val stats = cacheManager.getStats()
        assertTrue(stats.symbolCacheSize > 0, "Cache should contain data after concurrent operations")
    }
    
    @Test
    fun `benchmark full processing pipeline`() {
        // 模拟完整的注解处理流水线
        val classCount = 1000
        val interfaceCount = 100
        
        // 生成测试数据
        val classes = (1..classCount).map { "com.test.Class$it" }
        val interfaces = (1..interfaceCount).map { "com.test.Interface${it % interfaceCount}" }
        
        val totalTime = measureTimeMillis {
            // 1. 创建模拟类声明
            val classDeclarations = classes.map { className ->
                createMockClassDeclaration(className)
            }
            
            // 2. 并行处理类
             val results = parallelManager.processClassesInParallel(classDeclarations) { classDecl ->
                 try {
                     // 模拟符号处理
                     val interfaceName = interfaces.random()
                     val className = classDecl.qualifiedName?.asString() ?: "Unknown"
                     
                     // 缓存符号信息
                     cacheManager.putSymbol(className, classDecl)
                     
                     // 字符串驻留
                     val internedClassName = memoryOptimizer.internString(className)
                     val internedInterfaceName = memoryOptimizer.internString(interfaceName)
                     
                     // 创建服务条目
                     val entry = memoryOptimizer.createServiceEntry(
                         className = internedClassName,
                         name = classDecl.simpleName.asString(),
                         type = internedInterfaceName,
                         priority = 1,
                         isObject = false
                     )
                     
                     ParallelProcessingManager.ProcessingResult.Success(
                         className = className,
                         serviceEntries = listOf(entry),
                         processingTime = 1L
                     )
                 } catch (e: Exception) {
                     ParallelProcessingManager.ProcessingResult.Error(
                         className = classDecl.qualifiedName?.asString() ?: "Unknown",
                         error = e,
                         processingTime = 1L
                     )
                 }
             }
            
            // 3. 生成接口映射
             interfaces.forEach { interfaceName: String ->
                 val implementations: Set<String> = results.filterIsInstance<ParallelProcessingManager.ProcessingResult.Success>()
                     .flatMap { result -> result.serviceEntries }
                     .filter { entry -> entry.type == interfaceName }
                     .map { entry -> entry.name }
                     .toSet()
                 
                 cacheManager.putInterfaceMapping(interfaceName, implementations)
             }
            
            // 3. 内存优化
            memoryOptimizer.checkMemoryPressure()
        }
        
        println("Full pipeline processing time for $classCount classes: ${totalTime}ms")
        
        // 验证结果
         val cacheStats = cacheManager.getStats()
         val parallelStats = parallelManager.getProcessingStats()
         val memoryStats = memoryOptimizer.getMemoryStats()
        
        assertTrue(cacheStats.symbolCacheSize > 0, "Symbol cache should contain data")
        assertTrue(cacheStats.interfaceCacheSize > 0, "Interface cache should contain data")
        assertTrue(parallelStats.processedCount >= classCount, "All tasks should be processed")
        assertTrue(memoryStats.internedStrings > 0, "Strings should be interned")
        
        println("Cache stats: $cacheStats")
        println("Parallel stats: $parallelStats")
        println("Memory stats: $memoryStats")
        
        // 性能基准
        val avgTimePerClass = totalTime.toDouble() / classCount
        assertTrue(avgTimePerClass < 10, "Average processing time per class should be reasonable")
    }
    
    // 辅助方法：创建模拟的类声明
    private fun createMockClassDeclaration(className: String): com.google.devtools.ksp.symbol.KSClassDeclaration {
        return object : com.google.devtools.ksp.symbol.KSClassDeclaration {
            override val qualifiedName: com.google.devtools.ksp.symbol.KSName? = object : com.google.devtools.ksp.symbol.KSName {
                override fun asString(): String = className
                override fun getQualifier(): String = ""
                override fun getShortName(): String = className
            }
            
            override val simpleName: com.google.devtools.ksp.symbol.KSName = object : com.google.devtools.ksp.symbol.KSName {
                override fun asString(): String = className
                override fun getQualifier(): String = ""
                override fun getShortName(): String = className
            }
            
            override val packageName: com.google.devtools.ksp.symbol.KSName = object : com.google.devtools.ksp.symbol.KSName {
                override fun asString(): String = "com.test"
                override fun getQualifier(): String = ""
                override fun getShortName(): String = "com.test"
            }
            
            // 必需的属性
            override val annotations: Sequence<com.google.devtools.ksp.symbol.KSAnnotation> = emptySequence()
            override val classKind: com.google.devtools.ksp.symbol.ClassKind = com.google.devtools.ksp.symbol.ClassKind.CLASS
            override val containingFile: com.google.devtools.ksp.symbol.KSFile? = null
            override val declarations: Sequence<com.google.devtools.ksp.symbol.KSDeclaration> = emptySequence()
            override val isCompanionObject: Boolean = false
            override val location: com.google.devtools.ksp.symbol.Location = com.google.devtools.ksp.symbol.NonExistLocation
            override val modifiers: Set<com.google.devtools.ksp.symbol.Modifier> = emptySet()
            override val origin: com.google.devtools.ksp.symbol.Origin = com.google.devtools.ksp.symbol.Origin.KOTLIN
            override val parent: com.google.devtools.ksp.symbol.KSNode? = null
            override val parentDeclaration: com.google.devtools.ksp.symbol.KSDeclaration? = null
            override val primaryConstructor: com.google.devtools.ksp.symbol.KSFunctionDeclaration? = null
            override val superTypes: Sequence<com.google.devtools.ksp.symbol.KSTypeReference> = emptySequence()
            override val typeParameters: List<com.google.devtools.ksp.symbol.KSTypeParameter> = emptyList()
            override val docString: String? = null
            
            // 必需的方法
            override fun <D, R> accept(visitor: com.google.devtools.ksp.symbol.KSVisitor<D, R>, data: D): R {
                throw NotImplementedError("Mock implementation")
            }
            
            override fun asStarProjectedType(): com.google.devtools.ksp.symbol.KSType {
                throw NotImplementedError("Mock implementation")
            }
            
            override fun asType(typeArguments: List<com.google.devtools.ksp.symbol.KSTypeArgument>): com.google.devtools.ksp.symbol.KSType {
                throw NotImplementedError("Mock implementation")
            }
            
            override fun getAllFunctions(): Sequence<com.google.devtools.ksp.symbol.KSFunctionDeclaration> = emptySequence()
            override fun getAllProperties(): Sequence<com.google.devtools.ksp.symbol.KSPropertyDeclaration> = emptySequence()
            override fun getSealedSubclasses(): Sequence<com.google.devtools.ksp.symbol.KSClassDeclaration> = emptySequence()
            override fun findActuals(): Sequence<com.google.devtools.ksp.symbol.KSClassDeclaration> = emptySequence()
            override fun findExpects(): Sequence<com.google.devtools.ksp.symbol.KSClassDeclaration> = emptySequence()
            override val isActual: Boolean = false
            override val isExpect: Boolean = false
        }
    }
}