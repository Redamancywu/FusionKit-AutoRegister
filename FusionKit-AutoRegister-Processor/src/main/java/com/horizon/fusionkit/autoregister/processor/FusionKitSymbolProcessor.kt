package com.horizon.fusionkit.autoregister.processor

import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*

/**
 * FusionKit 组合符号处理器
 * 
 * 集成了以下功能：
 * - AutoRegister 自动注册处理
 * - HotReload 热更新处理
 * - 性能优化（缓存、并行处理、内存优化）
 */
class FusionKitSymbolProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
    private val options: Map<String, String>
) : SymbolProcessor {
    
    // 子处理器
    private val autoRegisterProcessor = AutoRegisterSymbolProcessor(codeGenerator, logger, options)
    private val hotReloadProcessor = HotReloadSymbolProcessor(codeGenerator, logger, options)
    
    // 性能优化组件
    private val cacheManager = CacheManager()
    private val parallelProcessingManager = ParallelProcessingManager(Logger(logger, options))
    private val memoryOptimizer = MemoryOptimizer()
    
    private var isInitialized = false
    
    override fun process(resolver: Resolver): List<KSAnnotated> {
        if (!isInitialized) {
            initialize()
            isInitialized = true
        }
        
        val startTime = System.currentTimeMillis()
        val unableToProcess = mutableListOf<KSAnnotated>()
        
        try {
            // 顺序处理不同的注解类型（简化版本）
            val autoRegisterResult = autoRegisterProcessor.process(resolver)
            val hotReloadResult = hotReloadProcessor.process(resolver)
            
            // 合并结果
            unableToProcess.addAll(autoRegisterResult)
            unableToProcess.addAll(hotReloadResult)
            
            val processingTime = System.currentTimeMillis() - startTime
            logger.info("FusionKit processing completed in ${processingTime}ms")
            
            // 内存优化
            memoryOptimizer.checkMemoryPressure()
            
        } catch (e: Exception) {
            logger.error("Error during FusionKit processing: ${e.message}", null)
            throw e
        }
        
        return unableToProcess
    }
    
    override fun finish() {
        try {
            // 完成子处理器
            autoRegisterProcessor.finish()
            hotReloadProcessor.finish()
            
            // 生成集成配置
            generateIntegrationConfiguration()
            
            // 清理资源
            cleanup()
            
            logger.info("FusionKit processing finished successfully")
            
        } catch (e: Exception) {
            logger.error("Error during FusionKit finish: ${e.message}", null)
        }
    }
    
    private fun initialize() {
        logger.info("Initializing FusionKit Symbol Processor")
        
        // 初始化性能优化组件
        // 缓存管理器、并行处理管理器和内存优化器无需显式初始化
        
        // 检查配置选项
        val enableHotReload = options["fusionkit.hotreload.enabled"]?.toBoolean() ?: false
        val enableParallelProcessing = options["fusionkit.parallel.enabled"]?.toBoolean() ?: true
        val enableCaching = options["fusionkit.cache.enabled"]?.toBoolean() ?: true
        
        logger.info("FusionKit configuration:")
        logger.info("  - Hot Reload: $enableHotReload")
        logger.info("  - Parallel Processing: $enableParallelProcessing")
        logger.info("  - Caching: $enableCaching")
    }
    
    private fun generateIntegrationConfiguration() {
        val file = codeGenerator.createNewFile(
            dependencies = Dependencies(false),
            packageName = "com.horizon.fusionkit.autoregister.generated",
            fileName = "FusionKitConfiguration"
        )
        
        file.use { outputStream ->
            outputStream.write(generateIntegrationConfigurationCode().toByteArray())
        }
    }
    
    private fun generateIntegrationConfigurationCode(): String {
        return buildString {
            appendLine("package com.horizon.fusionkit.autoregister.generated")
            appendLine()
            appendLine("/**")
            appendLine(" * FusionKit 集成配置")
            appendLine(" * 此文件由 FusionKitSymbolProcessor 自动生成，请勿手动修改")
            appendLine(" */")
            appendLine("object FusionKitConfiguration {")
            appendLine()
            
            appendLine("    /**")
            appendLine("     * 初始化 FusionKit 系统")
            appendLine("     */")
            appendLine("    fun initialize() {")
            appendLine("        // 初始化自动注册系统")
            appendLine("        try {")
            appendLine("            val autoRegisterManager = Class.forName(\"com.horizon.fusionkit.autoregister.generated.AutoRegisterManager\")")
            appendLine("            val initMethod = autoRegisterManager.getDeclaredMethod(\"initialize\")")
            appendLine("            initMethod.invoke(null)")
            appendLine("        } catch (e: Exception) {")
            appendLine("            // AutoRegister 系统未启用或初始化失败")
            appendLine("        }")
            appendLine()
            appendLine("        // 初始化热更新系统")
            appendLine("        try {")
            appendLine("            val hotReloadManager = Class.forName(\"com.horizon.fusionkit.autoregister.generated.HotReloadManager\")")
            appendLine("            val initMethod = hotReloadManager.getDeclaredMethod(\"initialize\")")
            appendLine("            initMethod.invoke(null)")
            appendLine("        } catch (e: Exception) {")
            appendLine("            // HotReload 系统未启用或初始化失败")
            appendLine("        }")
            appendLine("    }")
            appendLine()
            
            appendLine("    /**")
            appendLine("     * 关闭 FusionKit 系统")
            appendLine("     */")
            appendLine("    fun shutdown() {")
            appendLine("        // 关闭热更新系统")
            appendLine("        try {")
            appendLine("            val hotReloadManager = Class.forName(\"com.horizon.fusionkit.autoregister.generated.HotReloadManager\")")
            appendLine("            val shutdownMethod = hotReloadManager.getDeclaredMethod(\"shutdown\")")
            appendLine("            shutdownMethod.invoke(null)")
            appendLine("        } catch (e: Exception) {")
            appendLine("            // HotReload 系统未启用或关闭失败")
            appendLine("        }")
            appendLine("    }")
            appendLine()
            
            appendLine("    /**")
            appendLine("     * 获取系统信息")
            appendLine("     */")
            appendLine("    fun getSystemInfo(): Map<String, Any> {")
            appendLine("        val info = mutableMapOf<String, Any>()")
            appendLine()
            appendLine("        // AutoRegister 信息")
            appendLine("        try {")
            appendLine("            val autoRegisterConfig = Class.forName(\"com.horizon.fusionkit.autoregister.generated.AutoRegisterConfiguration\")")
            appendLine("            val serviceCountField = autoRegisterConfig.getDeclaredField(\"SERVICE_COUNT\")")
            appendLine("            info[\"autoRegister.serviceCount\"] = serviceCountField.get(null)")
            appendLine("        } catch (e: Exception) {")
            appendLine("            info[\"autoRegister.serviceCount\"] = 0")
            appendLine("        }")
            appendLine()
            appendLine("        // HotReload 信息")
            appendLine("        try {")
            appendLine("            val hotReloadConfig = Class.forName(\"com.horizon.fusionkit.autoregister.generated.HotReloadConfiguration\")")
            appendLine("            val serviceCountField = hotReloadConfig.getDeclaredField(\"SERVICE_COUNT\")")
            appendLine("            val enabledField = hotReloadConfig.getDeclaredField(\"ENABLED\")")
            appendLine("            info[\"hotReload.serviceCount\"] = serviceCountField.get(null)")
            appendLine("            info[\"hotReload.enabled\"] = enabledField.get(null)")
            appendLine("        } catch (e: Exception) {")
            appendLine("            info[\"hotReload.serviceCount\"] = 0")
            appendLine("            info[\"hotReload.enabled\"] = false")
            appendLine("        }")
            appendLine()
            appendLine("        return info")
            appendLine("    }")
            
            appendLine("}")
        }
    }
    
    private fun cleanup() {
        // 清理缓存
        cacheManager.clearAll()
        
        // 关闭并行处理
        parallelProcessingManager.shutdown()
        
        // 内存优化
        memoryOptimizer.resetStats()
    }
}