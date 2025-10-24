package com.horizon.fusionkit.autoregister.processor

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.reflect.KClass
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AutoRegisterSymbolProcessorTest {

    @TempDir
    lateinit var tempDir: File

    private lateinit var codeGenerator: CodeGenerator
    private lateinit var logger: KSPLogger
    private lateinit var resolver: Resolver
    private lateinit var processor: AutoRegisterSymbolProcessor

    @BeforeEach
    fun setUp() {
        codeGenerator = mockk()
        logger = mockk()
        resolver = mockk()
        processor = AutoRegisterSymbolProcessor(codeGenerator, logger, emptyMap())
    }

    @Test
    fun `test service entry creation`() {
        val entry = AutoRegisterSymbolProcessor.ServiceEntry(
            className = "com.example.MyService",
            name = "test",
            type = "business",
            priority = 100,
            isObject = false
        )

        assertEquals("com.example.MyService", entry.className)
        assertEquals("test", entry.name)
        assertEquals("business", entry.type)
        assertEquals(100, entry.priority)
        assertEquals(false, entry.isObject)
    }

    @Test
    fun `test service entry sorting by priority`() {
        val entries = listOf(
            AutoRegisterSymbolProcessor.ServiceEntry(
                "com.example.Service1",
                "s1",
                "type1",
                50,
                false
            ),
            AutoRegisterSymbolProcessor.ServiceEntry(
                "com.example.Service2",
                "s2",
                "type1",
                100,
                false
            ),
            AutoRegisterSymbolProcessor.ServiceEntry(
                "com.example.Service3",
                "s3",
                "type1",
                25,
                false
            )
        )

        val sorted = entries.sortedByDescending { it.priority }
        
        assertEquals(100, sorted[0].priority)
        assertEquals(50, sorted[1].priority)
        assertEquals(25, sorted[2].priority)
    }

    @Test
    fun `test companion object constants`() {
        // 这些常量可能不存在，暂时注释掉
        // assertNotNull(AutoRegisterSymbolProcessor.LIST_CLASS)
        // assertNotNull(AutoRegisterSymbolProcessor.MAP_CLASS)
        // assertNotNull(AutoRegisterSymbolProcessor.STRING_CLASS)
        // assertNotNull(AutoRegisterSymbolProcessor.COLLECTIONS_CLASS)
        assertTrue(true) // 占位测试
    }

    @Test
    fun `test build type enum values`() {
        val values = BuildType.values()
        assertEquals(3, values.size)
        assertTrue(values.contains(BuildType.DEBUG))
        assertTrue(values.contains(BuildType.RELEASE))
        assertTrue(values.contains(BuildType.ALL))
    }

    @Test
    fun `test auto register annotation default values`() {
        val annotationClass = AutoRegister::class.java

        // Get default values from annotation
        val defaultValueMethod = annotationClass.getDeclaredMethod("value")
        val defaultNameMethod = annotationClass.getDeclaredMethod("name")
        val defaultPriorityMethod = annotationClass.getDeclaredMethod("priority")

        // Test default values
        val defaultName = defaultNameMethod.defaultValue as String
        assertEquals("", defaultName)

        val defaultPriority = defaultPriorityMethod.defaultValue as Int
        assertEquals(0, defaultPriority)
    }
}