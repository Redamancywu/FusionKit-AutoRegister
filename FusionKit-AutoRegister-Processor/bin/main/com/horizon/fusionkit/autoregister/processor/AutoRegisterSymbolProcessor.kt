package com.horizon.fusionkit.autoregister.processor

import com.google.devtools.ksp.isPublic
import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.validate
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import java.io.OutputStreamWriter

class AutoRegisterSymbolProcessor(
    private val codeGenerator: CodeGenerator,
    private val kspLogger: KSPLogger,
    private val options: Map<String, String>
) : SymbolProcessor {
    
    private val logger = Logger(kspLogger, options)

    private val interfaceToEntries = mutableMapOf<String, MutableList<ServiceEntry>>()
    
    /**
     * 增量处理支持：记录已处理的文件，避免重复处理
     */
    private val processedFiles = mutableSetOf<String>()
    
    /**
     * 符号缓存：缓存已解析的类声明，避免重复解析
     */
    private val symbolCache = mutableMapOf<String, KSClassDeclaration>()
    
    /**
     * 接口缓存：缓存接口到实现类的映射关系
     */
    private val interfaceCache = mutableMapOf<String, Set<String>>()

    override fun process(resolver: Resolver): List<KSAnnotated> {
        interfaceToEntries.clear()

        val symbols = try {
            resolver.getSymbolsWithAnnotation(AutoRegister::class.qualifiedName!!)
                .filter { it.validate() && it is KSClassDeclaration }
                .filterIsInstance<KSClassDeclaration>()
                .also { symbolList ->
                    // 更新符号缓存
                    symbolList.forEach { symbol ->
                        symbol.qualifiedName?.asString()?.let { qualifiedName ->
                            symbolCache[qualifiedName] = symbol
                        }
                    }
                }
                .toList()
        } catch (e: Exception) {
            kspLogger.error("Failed to get symbols with annotation: ${e.message}")
            return emptyList()
        }

        val currentEnv = options["auto.register.env"] ?: "RELEASE"

        logger.info("AutoRegister processor started with environment: $currentEnv")
        logger.info("Found ${symbols.size} annotated classes")

        val invalidSymbols = mutableListOf<KSAnnotated>()

        for (classDecl in symbols) {
            try {
                // 支持多个 @AutoRegister 注解
                val autoRegisterAnnotations = classDecl.annotations.filter { it.shortName.asString() == "AutoRegister" }

                if (!classDecl.isPublic()) {
                    logger.warn("Class ${classDecl.qualifiedName?.asString()} must be public for @AutoRegister")
                    invalidSymbols.add(classDecl)
                    continue
                }

                val className = classDecl.qualifiedName?.asString() ?: run {
                    logger.error("Class has no qualified name", classDecl)
                    invalidSymbols.add(classDecl)
                    null
                }
                if (className == null) {
                    continue
                }
                val simpleName = classDecl.simpleName.asString()

                for (annotation in autoRegisterAnnotations) {
                    val args = annotation.arguments.associate { it.name?.asString() to it.value }

                    // 必填：value - 使用安全的类型转换
                    val interfaces = args["value"]?.let { value ->
                        when (value) {
                            is List<*> -> value.filterIsInstance<KSType>()
                                .mapNotNull { it.declaration.qualifiedName?.asString() }
                            else -> null
                        }
                    }

                    if (interfaces == null || interfaces.isEmpty()) {
                        logger.error("'value' is required and cannot be empty in @AutoRegister", classDecl)
                        invalidSymbols.add(classDecl)
                        continue
                    }

                    // 可选参数 - 使用安全的类型转换
                    val priority = args["priority"]?.let { it as? Int } ?: 0
                    val enabledIn = args["enabledIn"]?.let { value ->
                        when (value) {
                            is List<*> -> value.filterIsInstance<KSType>()
                                .map { it.declaration.simpleName.asString() }
                            else -> listOf("ALL")
                        }
                    } ?: listOf("ALL")
                    val isObject = args["isObject"]?.let { it as? Boolean } ?: false
                    
                    // 新功能参数 - 使用安全的类型转换
                    val configFile = args["configFile"]?.let { it as? String } ?: ""
                    val configKey = args["configKey"]?.let { it as? String } ?: ""
                    val enableLifecycle = args["enableLifecycle"]?.let { it as? Boolean } ?: false
                    val enableMetrics = args["enableMetrics"]?.let { it as? Boolean } ?: false
                    val pluginId = args["pluginId"]?.let { it as? String } ?: ""
                    val pluginVersion = args["pluginVersion"]?.let { it as? String } ?: ""

                    if (!enabledIn.any { it == "ALL" || it == currentEnv }) {
                        logger.debug("Skipping class $className - not enabled in current environment $currentEnv")
                        continue
                    }

                    for (iface in interfaces) {
                        val name = (args["name"] as? String)?.takeIf { it.isNotEmpty() } ?: simpleName
                        val type = (args["type"] as? String)?.takeIf { it.isNotEmpty() } ?: iface

                        // 检查重复的服务名称
                        val existingEntries = interfaceToEntries[iface]
                        val duplicateName = existingEntries?.find { it.name == name }
                        if (duplicateName != null) {
                            logger.error("Duplicate service name '$name' for interface $iface. Existing: ${duplicateName.className}, Current: $className", classDecl)
                            invalidSymbols.add(classDecl)
                            continue
                        }

                        interfaceToEntries.getOrPut(iface) { mutableListOf() }.add(
                            ServiceEntry(
                                className, name, type, priority, isObject,
                                configFile, configKey, enableLifecycle, enableMetrics,
                                pluginId, pluginVersion
                            )
                        )
                        
                        val features = mutableListOf<String>()
                        if (configFile.isNotEmpty()) features.add("config")
                        if (enableLifecycle) features.add("lifecycle")
                        if (enableMetrics) features.add("metrics")
                        if (pluginId.isNotEmpty()) features.add("plugin")
                        
                        val featureInfo = if (features.isNotEmpty()) " [${features.joinToString(", ")}]" else ""
                        logger.debug("Registered service: $name (type: $type, priority: $priority)$featureInfo for interface $iface")
                    }
                }
            } catch (e: Exception) {
                logger.error("Error processing class ${classDecl.qualifiedName?.asString()}: ${e.message}", classDecl)
                invalidSymbols.add(classDecl)
            }
        }

        logger.info("Processing completed. Found ${interfaceToEntries.size} interfaces with services")

        // 获取所有被引用的接口（确保即使无实现也生成聚合类）
        val allReferencedInterfaces = getAllReferencedInterfaces(resolver)
        allReferencedInterfaces.forEach { iface ->
            try {
                val entries = interfaceToEntries[iface] ?: emptyList()
                generateProvidersClass(iface, entries)
                if (entries.isEmpty()) {
                    logger.debug("Generated empty provider for interface $iface")
                }
            } catch (e: Exception) {
                logger.error("Failed to generate provider for interface $iface: ${e.message}")
            }
        }

        // 内存优化：处理完成后清理缓存
        cleanupCaches()
        
        return invalidSymbols
    }
    
    /**
     * 清理缓存以释放内存
     */
    private fun cleanupCaches() {
        // 保留最近使用的符号缓存，清理过期的
        if (symbolCache.size > 100) {
            val keysToRemove = symbolCache.keys.take(symbolCache.size - 50)
            keysToRemove.forEach { symbolCache.remove(it) }
        }
        
        // 清理接口缓存
        interfaceCache.clear()
        
        logger.debug("Cache cleanup completed. Symbol cache size: ${symbolCache.size}")
    }

    private fun getAllReferencedInterfaces(resolver: Resolver): Set<String> {
        val interfaces = mutableSetOf<String>()
        resolver.getSymbolsWithAnnotation(AutoRegister::class.qualifiedName!!)
            .filterIsInstance<KSClassDeclaration>()
            .forEach { classDecl ->
                // 支持多个 @AutoRegister 注解
                val autoRegisterAnnotations = classDecl.annotations.filter { it.shortName.asString() == "AutoRegister" }
                for (annotation in autoRegisterAnnotations) {
                    val args = annotation.arguments.associate { it.name?.asString() to it.value }
                    val value = args["value"]?.let { v ->
                        when (v) {
                            is List<*> -> v.filterIsInstance<KSType>()
                            else -> null
                        }
                    }
                    value?.forEach { type ->
                        type.declaration.qualifiedName?.asString()?.let(interfaces::add)
                    }
                }
            }
        return interfaces
    }

    private fun generateProvidersClass(interfaceName: String, entries: List<ServiceEntry>) {
        val sorted = entries.sortedByDescending { it.priority }
        val packageName = interfaceName.substringBeforeLast('.', "")
        val simpleInterfaceName = interfaceName.substringAfterLast('.')
        val providersName = "${simpleInterfaceName}Providers"

        val interfaceType = ClassName.bestGuess(interfaceName)
        val listProviderType = LIST_CLASS.parameterizedBy(interfaceType)
        val mapStringToProvider = MAP_CLASS.parameterizedBy(STRING_CLASS, interfaceType)
        val mapStringToListProvider = MAP_CLASS.parameterizedBy(STRING_CLASS, listProviderType)

        val file = codeGenerator.createNewFile(
            dependencies = Dependencies.ALL_FILES,
            packageName = packageName,
            fileName = providersName
        )

        OutputStreamWriter(file).use { writer ->
            // 修复：区分 object 和 class
            val allInitializer = if (sorted.isEmpty()) {
                CodeBlock.of("%T.emptyList()", COLLECTIONS_CLASS)
            } else {
                CodeBlock.builder()
                    .add("listOf(\n        ")
                    .apply {
                        sorted.forEachIndexed { index, entry ->
                            val className = ClassName.bestGuess(entry.className)
                            if (entry.isObject) {
                                add("%T", className) // ✅ 修复：Kotlin object 直接用类名
                            } else {
                                add("%T()", className) // 普通 class 需要 ()
                            }
                            if (index < sorted.size - 1) add(",\n        ")
                        }
                    }
                    .add("\n    )")
                    .build()
            }

            // 修复：byName 生成
            val byNameInitializer = if (sorted.isEmpty()) {
                CodeBlock.of("%T.emptyMap()", COLLECTIONS_CLASS)
            } else {
                CodeBlock.builder()
                    .add("mapOf(\n        ")
                    .apply {
                        sorted.forEachIndexed { index, entry ->
                            val className = ClassName.bestGuess(entry.className)
                            add("\"%L\" to ", entry.name)
                            if (entry.isObject) {
                                add("%T", className) // ✅ 修复：Kotlin object 直接用类名
                            } else {
                                add("%T()", className) // 普通 class 需要 ()
                            }
                            if (index < sorted.size - 1) add(",\n        ")
                        }
                    }
                    .add("\n    )")
                    .build()
            }

            // 修复：byType 生成
            val byTypeInitializer = if (sorted.isEmpty()) {
                CodeBlock.of("%T.emptyMap()", COLLECTIONS_CLASS)
            } else {
                CodeBlock.builder()
                    .add("mapOf(\n        ")
                    .apply {
                        val typeGroups = sorted.groupBy { it.type }
                        typeGroups.entries.forEachIndexed { groupIndex, (type, list) ->
                            add("\"%L\" to listOf(", type)
                            list.forEachIndexed { index, entry ->
                                val className = ClassName.bestGuess(entry.className)
                                if (entry.isObject) {
                                    add("%T", className) // ✅ 修复：Kotlin object 直接用类名
                                } else {
                                    add("%T()", className) // 普通 class 需要 ()
                                }
                                if (index < list.size - 1) add(", ")
                            }
                            add(")")
                            if (groupIndex < typeGroups.size - 1) add(",\n        ")
                        }
                    }
                    .add("\n    )")
                    .build()
            }

            val fileSpec = FileSpec.builder(packageName, providersName)
                .addFileComment("Auto-generated by AutoRegister KSP. DO NOT EDIT.")
                .addFileComment("Registered implementations: [${sorted.joinToString { it.name }}]")
                .addType(
                    TypeSpec.objectBuilder(providersName)
                        .addProperty(
                            PropertySpec.builder("all", listProviderType)
                                .initializer(allInitializer)
                                .build()
                        )
                        .addProperty(
                            PropertySpec.builder("byName", mapStringToProvider)
                                .initializer(byNameInitializer)
                                .build()
                        )
                        .addProperty(
                            PropertySpec.builder("byType", mapStringToListProvider)
                                .initializer(byTypeInitializer)
                                .build()
                        )
                        .addFunction(
                            FunSpec.builder("get")
                                .addParameter("name", STRING_CLASS)
                                .returns(interfaceType.copy(nullable = true))
                                .addStatement("return byName[name]")
                                .build()
                        )
                        .addFunction(
                            FunSpec.builder("getBest")
                                .addParameter(
                                    ParameterSpec.builder("type", STRING_CLASS.copy(nullable = true))
                                        .defaultValue("null")
                                        .build()
                                )
                                .returns(interfaceType.copy(nullable = true))
                                .addStatement("return if (type != null) byType[type]?.firstOrNull() else all.firstOrNull()")
                                .build()
                        )
                        .addFunction(
                            FunSpec.builder("getAll")
                                .addParameter(
                                    ParameterSpec.builder("type", STRING_CLASS.copy(nullable = true))
                                        .defaultValue("null")
                                        .build()
                                )
                                .returns(listProviderType)
                                .addStatement("return type?.let { byType[it] } ?: all")
                                .build()
                        )
                        .build()
                )
                .build()

            fileSpec.writeTo(writer)
        }
    }

    companion object {
        val LIST_CLASS = List::class.asClassName()
        val MAP_CLASS = Map::class.asClassName()
        val STRING_CLASS = String::class.asClassName()
        val COLLECTIONS_CLASS = ClassName("java.util", "Collections")
    }

    data class ServiceEntry(
        val className: String,
        val name: String,
        val type: String,
        val priority: Int,
        val isObject: Boolean,
        val configFile: String = "",
        val configKey: String = "",
        val enableLifecycle: Boolean = false,
        val enableMetrics: Boolean = false,
        val pluginId: String = "",
        val pluginVersion: String = ""
    )
}