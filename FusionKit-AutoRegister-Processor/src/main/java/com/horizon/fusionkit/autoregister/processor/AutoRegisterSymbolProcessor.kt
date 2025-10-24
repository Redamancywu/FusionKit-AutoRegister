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
    private val logger: KSPLogger,
    private val options: Map<String, String>
) : SymbolProcessor {

    private val interfaceToEntries = mutableMapOf<String, MutableList<ServiceEntry>>()
    
    /**
     * 增量处理支持：记录已处理的文件，避免重复处理
     */
    private val processedFiles = mutableSetOf<String>()

    override fun process(resolver: Resolver): List<KSAnnotated> {
        interfaceToEntries.clear()

        val symbols = try {
            resolver.getSymbolsWithAnnotation(AutoRegister::class.qualifiedName!!)
                .filter { it.validate() && it is KSClassDeclaration }
                .filterIsInstance<KSClassDeclaration>()
                .toList()
        } catch (e: Exception) {
            logger.error("Failed to get symbols with annotation: ${e.message}")
            return emptyList()
        }

        val currentEnv = options["auto.register.env"] ?: "RELEASE"
        val debugMode = options["auto.register.debug"]?.toBoolean() ?: false

        if (debugMode) {
            logger.info("AutoRegister processor started with environment: $currentEnv")
            logger.info("Found ${symbols.size} annotated classes")
        }

        val invalidSymbols = mutableListOf<KSAnnotated>()

        for (classDecl in symbols) {
            try {
                // 支持多个 @AutoRegister 注解
                val autoRegisterAnnotations = classDecl.annotations.filter { it.shortName.asString() == "AutoRegister" }

                if (!classDecl.isPublic()) {
                    logger.warning("Class ${classDecl.qualifiedName?.asString()} must be public for @AutoRegister")
                    invalidSymbols.add(classDecl)
                    continue
                }

                val className = classDecl.qualifiedName?.asString() ?: {
                    logger.error("Class has no qualified name", classDecl)
                    invalidSymbols.add(classDecl)
                    continue
                }
                val simpleName = classDecl.simpleName.asString()

                for (annotation in autoRegisterAnnotations) {
                    val args = annotation.arguments.associate { it.name?.asString() to it.value }

                    // 必填：value
                    val interfaces = (args["value"] as? List<KSType>)
                        ?.mapNotNull { it.declaration.qualifiedName?.asString() }

                    if (interfaces == null || interfaces.isEmpty()) {
                        logger.error("'value' is required and cannot be empty in @AutoRegister", classDecl)
                        invalidSymbols.add(classDecl)
                        continue
                    }

                    // 可选参数
                    val priority = (args["priority"] as? Int) ?: 0
                    val enabledIn = (args["enabledIn"] as? List<KSType>)?.map { it.declaration.simpleName.asString() } ?: listOf("ALL")
                    val isObject = (args["isObject"] as? Boolean) ?: false

                    if (!enabledIn.any { it == "ALL" || it == currentEnv }) {
                        if (debugMode) {
                            logger.info("Skipping class $className - not enabled in current environment $currentEnv")
                        }
                        continue
                    }

                    for (iface in interfaces) {
                        val name = (args["name"] as? String)?.takeIf { it.isNotEmpty() } ?: simpleName
                        val type = (args["type"] as? String)?.takeIf { it.isNotEmpty() } ?: iface

                        // 检查重复的服务名称
                        val existingEntries = interfaceToEntries[iface]
                        val duplicateName = existingEntries?.find { it.name == name }
                        if (duplicateName != null) {
                            logger.error("Duplicate service name '$name' for interface $iface. Existing: ${duplicateName.className}, Current: $className")
                            invalidSymbols.add(classDecl)
                            continue
                        }

                        interfaceToEntries.getOrPut(iface) { mutableListOf() }.add(
                            ServiceEntry(className, name, type, priority, isObject)
                        )
                        
                        if (debugMode) {
                            logger.info("Registered service: $name (type: $type, priority: $priority) for interface $iface")
                        }
                    }
                }
            } catch (e: Exception) {
                logger.error("Error processing class ${classDecl.qualifiedName?.asString()}: ${e.message}", classDecl)
                invalidSymbols.add(classDecl)
            }
        }

        if (debugMode) {
            logger.info("Processing completed. Found ${interfaceToEntries.size} interfaces with services")
        }

        // 获取所有被引用的接口（确保即使无实现也生成聚合类）
        val allReferencedInterfaces = getAllReferencedInterfaces(resolver)
        allReferencedInterfaces.forEach { iface ->
            try {
                val entries = interfaceToEntries[iface] ?: emptyList()
                generateProvidersClass(iface, entries)
                if (debugMode && entries.isEmpty()) {
                    logger.info("Generated empty provider for interface $iface")
                }
            } catch (e: Exception) {
                logger.error("Failed to generate provider for interface $iface: ${e.message}")
            }
        }

        return invalidSymbols
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
                    val value = args["value"] as? List<KSType>
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
        private val LIST_CLASS = List::class.asClassName()
        private val MAP_CLASS = Map::class.asClassName()
        private val STRING_CLASS = String::class.asClassName()
        private val COLLECTIONS_CLASS = ClassName("java.util", "Collections")
    }

    data class ServiceEntry(
        val className: String,
        val name: String,
        val type: String,
        val priority: Int,
        val isObject: Boolean
    )
}