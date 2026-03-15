package com.example.onboarder.chat

import com.example.onboarder.configuration.ToolDefinition
import com.example.onboarder.configuration.ToolsConfiguration
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ToolsServiceTest {

    private lateinit var toolsService: ToolsService
    private lateinit var toolsConfiguration: ToolsConfiguration

    @BeforeEach
    fun setup() {
        toolsConfiguration = ToolsConfiguration(
            tools = listOf(
                ToolDefinition(
                    name = "getPtoBalance",
                    description = "Retrieve your current PTO balance",
                    executionPrompt = "Show me my PTO balance",
                    keywords = listOf("pto", "vacation", "sick", "time off")
                ),
                ToolDefinition(
                    name = "getSalaryInfo",
                    description = "Retrieve your salary information",
                    executionPrompt = "Show me my salary",
                    keywords = listOf("salary", "pay", "compensation")
                )
            )
        )
        toolsService = ToolsService(toolsConfiguration)
    }

    @Test
    fun `should detect PTO tool requirement from question with pto keyword`() {
        val result = toolsService.detectToolRequirement("How many PTO days do I have?")
        
        assertNotNull(result)
        assertEquals("getPtoBalance", result.toolName)
        assertEquals("Retrieve your current PTO balance", result.description)
    }

    @Test
    fun `should detect PTO tool requirement from question with vacation keyword`() {
        val result = toolsService.detectToolRequirement("What is my vacation balance?")
        
        assertNotNull(result)
        assertEquals("getPtoBalance", result.toolName)
    }

    @Test
    fun `should detect salary tool requirement from question with salary keyword`() {
        val result = toolsService.detectToolRequirement("What is my salary?")
        
        assertNotNull(result)
        assertEquals("getSalaryInfo", result.toolName)
        assertEquals("Retrieve your salary information", result.description)
    }

    @Test
    fun `should be case insensitive when detecting tool requirements`() {
        val result = toolsService.detectToolRequirement("WHAT IS MY PTO BALANCE?")
        
        assertNotNull(result)
        assertEquals("getPtoBalance", result.toolName)
    }

    @Test
    fun `should return null when no tool matches the question`() {
        val result = toolsService.detectToolRequirement("What is the company address?")
        
        assertNull(result)
    }

    @Test
    fun `should create tool request and return request ID`() {
        val requestId = toolsService.createToolRequest("getPtoBalance", "How many PTO days?")
        
        assertNotNull(requestId)
        assertTrue(requestId.isNotEmpty())
    }

    @Test
    fun `should retrieve pending request by ID`() {
        val requestId = toolsService.createToolRequest("getPtoBalance", "How many PTO days?")
        
        val pending = toolsService.getPendingRequest(requestId)
        
        assertNotNull(pending)
        assertEquals(requestId, pending.requestId)
        assertEquals("getPtoBalance", pending.toolName)
        assertEquals("How many PTO days?", pending.userMessage)
    }

    @Test
    fun `should return null for non-existent request ID`() {
        val pending = toolsService.getPendingRequest("non-existent-id")
        
        assertNull(pending)
    }

    @Test
    fun `should return true when request is approved (exists)`() {
        val requestId = toolsService.createToolRequest("getPtoBalance", "How many PTO days?")
        
        val isApproved = toolsService.isApproved(requestId)
        
        assertEquals(true, isApproved)
    }

    @Test
    fun `should return null when request does not exist`() {
        val isApproved = toolsService.isApproved("non-existent-id")
        
        assertNull(isApproved)
    }

    @Test
    fun `should remove request from pending queue`() {
        val requestId = toolsService.createToolRequest("getPtoBalance", "How many PTO days?")
        
        toolsService.removeRequest(requestId)
        
        val pending = toolsService.getPendingRequest(requestId)
        assertNull(pending)
    }

    @Test
    fun `should return configured execution prompt for tool`() {
        val prompt = toolsService.getToolExecutionPrompt("getPtoBalance")
        
        assertEquals("Show me my PTO balance", prompt)
    }

    @Test
    fun `should return generic prompt for unconfigured tool`() {
        val prompt = toolsService.getToolExecutionPrompt("unknownTool")
        
        assertEquals("Execute the unknownTool function and show me the results", prompt)
    }

    @Test
    fun `should return generic prompt when execution prompt is blank`() {
        val configWithBlankPrompt = ToolsConfiguration(
            tools = listOf(
                ToolDefinition(
                    name = "testTool",
                    description = "Test tool",
                    executionPrompt = "",
                    keywords = listOf("test")
                )
            )
        )
        val service = ToolsService(configWithBlankPrompt)
        
        val prompt = service.getToolExecutionPrompt("testTool")
        
        assertEquals("Execute the testTool function and show me the results", prompt)
    }

    @Test
    fun `should handle multiple keywords for same tool`() {
        val result1 = toolsService.detectToolRequirement("How much vacation do I have?")
        val result2 = toolsService.detectToolRequirement("What is my sick leave balance?")
        val result3 = toolsService.detectToolRequirement("Show me my time off")
        
        assertNotNull(result1)
        assertNotNull(result2)
        assertNotNull(result3)
        assertEquals("getPtoBalance", result1.toolName)
        assertEquals("getPtoBalance", result2.toolName)
        assertEquals("getPtoBalance", result3.toolName)
    }

    @Test
    fun `should return first matching tool when multiple tools match`() {
        val configWithOverlap = ToolsConfiguration(
            tools = listOf(
                ToolDefinition(
                    name = "tool1",
                    description = "First tool",
                    executionPrompt = "Execute tool 1",
                    keywords = listOf("balance")
                ),
                ToolDefinition(
                    name = "tool2",
                    description = "Second tool",
                    executionPrompt = "Execute tool 2",
                    keywords = listOf("balance")
                )
            )
        )
        val service = ToolsService(configWithOverlap)
        
        val result = service.detectToolRequirement("What is my balance?")
        
        assertNotNull(result)
        assertEquals("tool1", result.toolName)
    }
}
