package com.horizon.fusionkit.autoregister.processor

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSNode

/**
 * 独立的日志功能
 * 支持通过 KSP 参数控制日志输出
 */
class Logger(private val kspLogger: KSPLogger, private val options: Map<String, String>) {
    
    private val debugMode = options["auto.register.debug"]?.toBoolean() ?: false
    private val logLevel = options["auto.register.log.level"]?.uppercase() ?: "INFO"
    
    /**
     * 输出调试信息（仅在调试模式下显示）
     */
    fun debug(message: String, symbol: KSNode? = null) {
        if (debugMode) {
            kspLogger.info("[DEBUG] $message", symbol)
        }
    }
    
    /**
     * 输出信息（始终显示）
     */
    fun info(message: String, symbol: KSNode? = null) {
        if (logLevel in listOf("INFO", "DEBUG", "WARN", "ERROR")) {
            kspLogger.info("[INFO] $message", symbol)
        }
    }
    
    /**
     * 输出警告信息
     */
    fun warn(message: String, symbol: KSNode? = null) {
        if (logLevel in listOf("WARN", "DEBUG", "ERROR")) {
            kspLogger.warn("[WARN] $message", symbol)
        }
    }
    
    /**
     * 输出错误信息
     */
    fun error(message: String, symbol: KSNode? = null) {
        if (logLevel in listOf("ERROR", "DEBUG")) {
            kspLogger.error("[ERROR] $message", symbol)
        }
    }
    
    /**
     * 输出编译时信息（给宿主层显示）
     */
    fun compileInfo(message: String) {
        if (debugMode) {
            kspLogger.info("\n" + "=".repeat(60))
            kspLogger.info("AutoRegister Processor - Compilation Info")
            kspLogger.info("=".repeat(60))
            kspLogger.info(message)
            kspLogger.info("=".repeat(60) + "\n")
        }
    }
    
    /**
     * 输出统计信息
     */
    fun stats(interfaceCount: Int, serviceCount: Int, generatedFiles: Int) {
        if (debugMode) {
            val statsMessage = """
                |AutoRegister Processor Statistics:
                |  - Interfaces processed: $interfaceCount
                |  - Services registered: $serviceCount
                |  - Files generated: $generatedFiles
                |  - Log level: $logLevel
                |  - Debug mode: $debugMode
                """.trimMargin()
            
            kspLogger.info("\n" + "=".repeat(60))
            kspLogger.info("AutoRegister Processor - Statistics")
            kspLogger.info("=".repeat(60))
            kspLogger.info(statsMessage)
            kspLogger.info("=".repeat(60) + "\n")
        }
    }
}