package com.horizon.fusionkit.autoregister.processor

import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.validate
import java.io.OutputStream

/**
 * 热更新符号处理器
 * 
 * 功能：
 * - 处理@HotReload注解
 * - 生成热更新配置文件
 * - 生成热更新管理器代码
 * - 集成到现有的AutoRegister系统
 */
class HotReloadSymbolProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
    private val options: Map<String, String>
) : SymbolProcessor {
    
    private val hotReloadServices = mutableListOf<HotReloadServiceEntry>()
    private val hotReloadConfigs = mutableListOf<HotReloadConfigEntry>()
    
    override fun process(resolver: Resolver): List<KSAnnotated> {
        // 处理@HotReload注解
        val hotReloadSymbols = resolver.getSymbolsWithAnnotation("com.horizon.fusionkit.autoregister.processor.HotReload")
        val hotReloadConfigSymbols = resolver.getSymbolsWithAnnotation("com.horizon.fusionkit.autoregister.processor.HotReloadConfig")
        
        val unableToProcess = mutableListOf<KSAnnotated>()
        
        // 处理热更新配置
        hotReloadConfigSymbols.forEach { symbol ->
            if (!symbol.validate()) {
                unableToProcess.add(symbol)
                return@forEach
            }
            
            when (symbol) {
                is KSClassDeclaration -> processHotReloadConfig(symbol)
                else -> logger.warn("@HotReloadConfig can only be applied to classes", symbol)
            }
        }
        
        // 处理热更新服务
        hotReloadSymbols.forEach { symbol ->
            if (!symbol.validate()) {
                unableToProcess.add(symbol)
                return@forEach
            }
            
            when (symbol) {
                is KSClassDeclaration -> processHotReloadService(symbol)
                else -> logger.warn("@HotReload can only be applied to classes", symbol)
            }
        }
        
        // 生成热更新管理器
        if (hotReloadServices.isNotEmpty() || hotReloadConfigs.isNotEmpty()) {
            generateHotReloadManager()
            generateHotReloadConfiguration()
        }
        
        return unableToProcess
    }
    
    private fun processHotReloadConfig(classDeclaration: KSClassDeclaration) {
        val annotation = classDeclaration.annotations.find { 
            it.annotationType.resolve().declaration.qualifiedName?.asString() == 
            "com.horizon.fusionkit.autoregister.processor.HotReloadConfig" 
        } ?: return
        
        val configEntry = HotReloadConfigEntry(
            className = classDeclaration.qualifiedName?.asString() ?: return,
            enabled = getAnnotationValue(annotation, "enabled") ?: false,
            watchInterval = getAnnotationValue(annotation, "watchInterval") ?: 1000L,
            enableGlobalRollback = getAnnotationValue(annotation, "enableGlobalRollback") ?: true,
            globalReloadTimeout = getAnnotationValue(annotation, "globalReloadTimeout") ?: 10000L,
            maxConcurrentReloads = getAnnotationValue(annotation, "maxConcurrentReloads") ?: 5,
            enableEventNotification = getAnnotationValue(annotation, "enableEventNotification") ?: true,
            reloadHistorySize = getAnnotationValue(annotation, "reloadHistorySize") ?: 10,
            disableInProduction = getAnnotationValue(annotation, "disableInProduction") ?: true
        )
        
        hotReloadConfigs.add(configEntry)
        logger.info("Processed HotReloadConfig: ${configEntry.className}")
    }
    
    private fun processHotReloadService(classDeclaration: KSClassDeclaration) {
        val annotation = classDeclaration.annotations.find { 
            it.annotationType.resolve().declaration.qualifiedName?.asString() == 
            "com.horizon.fusionkit.autoregister.processor.HotReload" 
        } ?: return
        
        // 检查类是否为public
        if (!classDeclaration.isPublic()) {
            logger.warn("@HotReload can only be applied to public classes", classDeclaration)
            return
        }
        
        val className = classDeclaration.qualifiedName?.asString() ?: return
        val packageName = classDeclaration.packageName.asString()
        val simpleName = classDeclaration.simpleName.asString()
        
        // 获取注解参数
        val interfaces = getAnnotationValue<List<KSType>>(annotation, "value")?.map { 
            it.declaration.qualifiedName?.asString() ?: ""
        } ?: emptyList()
        
        val name = getAnnotationValue<String>(annotation, "name")?.takeIf { it.isNotEmpty() } ?: simpleName
        val watchPaths = getAnnotationValue<List<String>>(annotation, "watchPaths") ?: emptyList()
        val reloadStrategy = getAnnotationValue<KSType>(annotation, "reloadStrategy")?.let { 
            ReloadStrategy.valueOf(it.declaration.simpleName.asString())
        } ?: ReloadStrategy.IMMEDIATE
        
        val enableRollback = getAnnotationValue<Boolean>(annotation, "enableRollback") ?: true
        val maxRetries = getAnnotationValue<Int>(annotation, "maxRetries") ?: 3
        val reloadTimeout = getAnnotationValue<Long>(annotation, "reloadTimeout") ?: 5000L
        val enabledIn = getAnnotationValue<List<KSType>>(annotation, "enabledIn")?.map { 
            BuildType.valueOf(it.declaration.simpleName.asString())
        } ?: listOf(BuildType.DEBUG)
        
        val dependencies = getAnnotationValue<List<KSType>>(annotation, "dependencies")?.map { 
            it.declaration.qualifiedName?.asString() ?: ""
        } ?: emptyList()
        
        val beforeReload = getAnnotationValue<String>(annotation, "beforeReload") ?: ""
        val afterReload = getAnnotationValue<String>(annotation, "afterReload") ?: ""
        val onReloadFailed = getAnnotationValue<String>(annotation, "onReloadFailed") ?: ""
        val enableMetrics = getAnnotationValue<Boolean>(annotation, "enableMetrics") ?: false
        val enableLogging = getAnnotationValue<Boolean>(annotation, "enableLogging") ?: true
        
        // 自动推断监听路径
        val finalWatchPaths = if (watchPaths.isEmpty()) {
            // 根据类的位置推断源文件路径
            listOf("src/main/kotlin/${packageName.replace('.', '/')}/${simpleName}.kt")
        } else {
            watchPaths
        }
        
        val serviceEntry = HotReloadServiceEntry(
            className = className,
            packageName = packageName,
            simpleName = simpleName,
            name = name,
            interfaces = interfaces,
            watchPaths = finalWatchPaths,
            reloadStrategy = reloadStrategy,
            enableRollback = enableRollback,
            maxRetries = maxRetries,
            reloadTimeout = reloadTimeout,
            enabledIn = enabledIn,
            dependencies = dependencies,
            beforeReload = beforeReload,
            afterReload = afterReload,
            onReloadFailed = onReloadFailed,
            enableMetrics = enableMetrics,
            enableLogging = enableLogging
        )
        
        hotReloadServices.add(serviceEntry)
        logger.info("Processed HotReload service: $className")
    }
    
    private fun generateHotReloadManager() {
        val file = codeGenerator.createNewFile(
            dependencies = Dependencies(false),
            packageName = "com.horizon.fusionkit.autoregister.generated",
            fileName = "HotReloadManager"
        )
        
        file.use { outputStream ->
            outputStream.write(generateHotReloadManagerCode().toByteArray())
        }
    }
    
    private fun generateHotReloadManagerCode(): String {
        return buildString {
            appendLine("package com.horizon.fusionkit.autoregister.generated")
            appendLine()
            appendLine("import com.horizon.fusionkit.autoregister.processor.*")
            appendLine("import kotlin.reflect.KClass")
            appendLine()
            appendLine("/**")
            appendLine(" * 自动生成的热更新管理器")
            appendLine(" * 此文件由 HotReloadSymbolProcessor 自动生成，请勿手动修改")
            appendLine(" */")
            appendLine("object HotReloadManager {")
            appendLine()
            
            // 生成初始化方法
            appendLine("    /**")
            appendLine("     * 初始化热更新系统")
            appendLine("     */")
            appendLine("    fun initialize() {")
            appendLine("        val engine = HotReloadEngine.getInstance()")
            appendLine()
            
            // 应用配置
            if (hotReloadConfigs.isNotEmpty()) {
                val config = hotReloadConfigs.first() // 使用第一个配置
                appendLine("        // 应用热更新配置")
                appendLine("        val config = HotReloadConfiguration(")
                appendLine("            enabled = ${config.enabled},")
                appendLine("            watchInterval = ${config.watchInterval}L,")
                appendLine("            enableGlobalRollback = ${config.enableGlobalRollback},")
                appendLine("            globalReloadTimeout = ${config.globalReloadTimeout}L,")
                appendLine("            maxConcurrentReloads = ${config.maxConcurrentReloads},")
                appendLine("            enableEventNotification = ${config.enableEventNotification},")
                appendLine("            reloadHistorySize = ${config.reloadHistorySize},")
                appendLine("            disableInProduction = ${config.disableInProduction}")
                appendLine("        )")
                appendLine("        engine.initialize(config)")
            } else {
                appendLine("        // 使用默认配置")
                appendLine("        engine.initialize()")
            }
            
            appendLine()
            appendLine("        // 注册热更新服务")
            
            // 注册服务
            hotReloadServices.forEach { service ->
                appendLine("        registerService_${service.simpleName}(engine)")
            }
            
            appendLine("    }")
            appendLine()
            
            // 生成服务注册方法
            hotReloadServices.forEach { service ->
                appendLine("    private fun registerService_${service.simpleName}(engine: HotReloadEngine) {")
                appendLine("        val serviceInfo = HotReloadServiceInfo(")
                appendLine("            name = \"${service.name}\",")
                appendLine("            interfaces = listOf(${service.interfaces.joinToString(", ") { "${it}::class" }}),")
                appendLine("            watchPaths = listOf(${service.watchPaths.joinToString(", ") { "\"$it\"" }}),")
                appendLine("            reloadStrategy = ReloadStrategy.${service.reloadStrategy},")
                appendLine("            enableRollback = ${service.enableRollback},")
                appendLine("            maxRetries = ${service.maxRetries},")
                appendLine("            reloadTimeout = ${service.reloadTimeout}L,")
                appendLine("            dependencies = listOf(${service.dependencies.joinToString(", ") { "${it}::class" }}),")
                appendLine("            beforeReload = \"${service.beforeReload}\",")
                appendLine("            afterReload = \"${service.afterReload}\",")
                appendLine("            onReloadFailed = \"${service.onReloadFailed}\",")
                appendLine("            enableMetrics = ${service.enableMetrics},")
                appendLine("            enableLogging = ${service.enableLogging}")
                appendLine("        )")
                appendLine("        engine.registerService(serviceInfo)")
                appendLine("    }")
                appendLine()
            }
            
            // 生成便捷方法
            appendLine("    /**")
            appendLine("     * 手动触发指定服务的重载")
            appendLine("     */")
            appendLine("    fun reloadService(serviceName: String) {")
            appendLine("        HotReloadEngine.getInstance().triggerReload(serviceName)")
            appendLine("    }")
            appendLine()
            
            appendLine("    /**")
            appendLine("     * 获取重载统计信息")
            appendLine("     */")
            appendLine("    fun getStatistics(): ReloadStatistics {")
            appendLine("        return HotReloadEngine.getInstance().getReloadStatistics()")
            appendLine("    }")
            appendLine()
            
            appendLine("    /**")
            appendLine("     * 关闭热更新系统")
            appendLine("     */")
            appendLine("    fun shutdown() {")
            appendLine("        HotReloadEngine.getInstance().shutdown()")
            appendLine("    }")
            appendLine()
            
            // 生成服务列表
            appendLine("    /**")
            appendLine("     * 所有注册的热更新服务")
            appendLine("     */")
            appendLine("    val registeredServices = listOf(")
            hotReloadServices.forEach { service ->
                appendLine("        \"${service.name}\",")
            }
            appendLine("    )")
            
            appendLine("}")
        }
    }
    
    private fun generateHotReloadConfiguration() {
        val file = codeGenerator.createNewFile(
            dependencies = Dependencies(false),
            packageName = "com.horizon.fusionkit.autoregister.generated",
            fileName = "HotReloadConfiguration"
        )
        
        file.use { outputStream ->
            outputStream.write(generateHotReloadConfigurationCode().toByteArray())
        }
    }
    
    private fun generateHotReloadConfigurationCode(): String {
        return buildString {
            appendLine("package com.horizon.fusionkit.autoregister.generated")
            appendLine()
            appendLine("/**")
            appendLine(" * 热更新配置信息")
            appendLine(" * 此文件由 HotReloadSymbolProcessor 自动生成，请勿手动修改")
            appendLine(" */")
            appendLine("object HotReloadConfiguration {")
            appendLine()
            
            appendLine("    /**")
            appendLine("     * 热更新服务数量")
            appendLine("     */")
            appendLine("    const val SERVICE_COUNT = ${hotReloadServices.size}")
            appendLine()
            
            appendLine("    /**")
            appendLine("     * 是否启用热更新")
            appendLine("     */")
            appendLine("    const val ENABLED = ${hotReloadConfigs.firstOrNull()?.enabled ?: false}")
            appendLine()
            
            appendLine("    /**")
            appendLine("     * 所有热更新服务的类名")
            appendLine("     */")
            appendLine("    val SERVICE_CLASSES = arrayOf(")
            hotReloadServices.forEach { service ->
                appendLine("        \"${service.className}\",")
            }
            appendLine("    )")
            appendLine()
            
            appendLine("    /**")
            appendLine("     * 监听的文件路径")
            appendLine("     */")
            appendLine("    val WATCH_PATHS = arrayOf(")
            hotReloadServices.flatMap { it.watchPaths }.distinct().forEach { path ->
                appendLine("        \"$path\",")
            }
            appendLine("    )")
            
            appendLine("}")
        }
    }
    
    @Suppress("UNCHECKED_CAST")
    private fun <T> getAnnotationValue(annotation: KSAnnotation, name: String): T? {
        return annotation.arguments.find { it.name?.asString() == name }?.value as? T
    }
    
    private fun KSClassDeclaration.isPublic(): Boolean {
        return modifiers.contains(Modifier.PUBLIC) || 
               (!modifiers.contains(Modifier.PRIVATE) && !modifiers.contains(Modifier.INTERNAL))
    }
}

/**
 * 热更新服务条目
 */
data class HotReloadServiceEntry(
    val className: String,
    val packageName: String,
    val simpleName: String,
    val name: String,
    val interfaces: List<String>,
    val watchPaths: List<String>,
    val reloadStrategy: ReloadStrategy,
    val enableRollback: Boolean,
    val maxRetries: Int,
    val reloadTimeout: Long,
    val enabledIn: List<BuildType>,
    val dependencies: List<String>,
    val beforeReload: String,
    val afterReload: String,
    val onReloadFailed: String,
    val enableMetrics: Boolean,
    val enableLogging: Boolean
)

/**
 * 热更新配置条目
 */
data class HotReloadConfigEntry(
    val className: String,
    val enabled: Boolean,
    val watchInterval: Long,
    val enableGlobalRollback: Boolean,
    val globalReloadTimeout: Long,
    val maxConcurrentReloads: Int,
    val enableEventNotification: Boolean,
    val reloadHistorySize: Int,
    val disableInProduction: Boolean
)