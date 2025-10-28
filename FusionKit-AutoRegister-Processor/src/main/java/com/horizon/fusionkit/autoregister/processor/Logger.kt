package com.horizon.fusionkit.autoregister.processor

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSNode

/**
 * 独立的日志功能
 * 支持通过 KSP 参数控制日志输出
 */
class Logger(private val kspLogger: KSPLogger, private val options: Map<String, String>) {
    
    enum class LogLevel { OFF, ERROR, WARN, INFO, DEBUG }

    private val debugMode = options["auto.register.debug"]?.toBoolean() ?: false
    private val level: LogLevel = options["auto.register.log.level"]
        ?.uppercase()
        ?.let { name -> LogLevel.values().firstOrNull { it.name == name } }
        ?: LogLevel.INFO

    private fun allow(requestLevel: LogLevel): Boolean {
        // 阈值过滤：只有当请求级别不高于当前级别时，才输出
        return requestLevel.ordinal <= level.ordinal
    }
    
    /**
     * 输出调试信息（DEBUG 级别或显式调试模式）
     */
    fun debug(message: String, symbol: KSNode? = null) {
        if (allow(LogLevel.DEBUG) || debugMode) {
            kspLogger.info("[DEBUG] $message", symbol)
        }
    }
    
    /**
     * 输出信息
     */
    fun info(message: String, symbol: KSNode? = null) {
        if (allow(LogLevel.INFO)) {
            kspLogger.info("[INFO] $message", symbol)
        }
    }
    
    /**
     * 输出警告信息
     */
    fun warn(message: String, symbol: KSNode? = null) {
        if (allow(LogLevel.WARN)) {
            kspLogger.warn("[WARN] $message", symbol)
        }
    }
    
    /**
     * 输出错误信息（始终输出）
     */
    fun error(message: String, symbol: KSNode? = null) {
        kspLogger.error("[ERROR] $message", symbol)
    }
    
    /**
     * 输出编译时信息（仅在调试模式）
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
     * 输出统计信息（仅在调试模式）
     */
    fun stats(interfaceCount: Int, serviceCount: Int, generatedFiles: Int) {
        if (debugMode) {
            val statsMessage = """
                |AutoRegister Processor Statistics:
                |  - Interfaces processed: $interfaceCount
                |  - Services registered: $serviceCount
                |  - Files generated: $generatedFiles
                |  - Log level: ${level.name}
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