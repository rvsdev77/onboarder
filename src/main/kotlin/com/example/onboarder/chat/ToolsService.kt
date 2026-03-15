package com.example.onboarder.chat

import com.example.onboarder.configuration.ToolsConfiguration
import org.springframework.stereotype.Service
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Represents a pending tool execution request awaiting user approval.
 *
 * @property requestId Unique identifier for the tool execution request
 * @property toolName Name of the tool to be executed (e.g., "getPtoBalance")
 * @property userMessage Original user message that triggered the tool requirement
 */
data class PendingToolExecution(
    val requestId: String,
    val toolName: String,
    val userMessage: String
)

/**
 * Represents a tool that can be offered to the user for execution.
 *
 * @property toolName Name of the tool function
 * @property description Human-readable description shown in the permission dialog
 */
data class ToolOffer(val toolName: String, val description: String)

/**
 * Service responsible for tool detection, permission management, and execution configuration.
 *
 * This service handles the complete lifecycle of tool usage:
 * - Detecting when a tool is needed based on user questions
 * - Managing permission requests and approvals
 * - Providing tool-specific execution prompts
 *
 * Tool configuration is externalized in application.yml, allowing environment-specific
 * keywords, descriptions, and execution prompts.
 *
 * @property toolsConfiguration Configuration containing tool definitions from application.yml
 */
@Service
class ToolsService(private val toolsConfiguration: ToolsConfiguration) {
    private val pendingRequests = ConcurrentHashMap<String, PendingToolExecution>()

    /**
     * Creates a new tool execution request and stores it for approval tracking.
     *
     * @param toolName Name of the tool to be executed
     * @param userMessage Original user message that triggered the tool
     * @return Unique request ID for tracking approval/denial
     */
    fun createToolRequest(toolName: String, userMessage: String): String {
        val requestId = UUID.randomUUID().toString()
        pendingRequests[requestId] = PendingToolExecution(requestId, toolName, userMessage)
        return requestId
    }

    /**
     * Checks if a tool request has been approved.
     *
     * @param requestId The unique identifier of the tool request
     * @return true if the request exists (approved), null if not found
     */
    fun isApproved(requestId: String): Boolean? {
        return pendingRequests[requestId]?.let { true }
    }

    /**
     * Retrieves a pending tool execution request by its ID.
     *
     * @param requestId The unique identifier of the tool request
     * @return The pending tool execution details, or null if not found
     */
    fun getPendingRequest(requestId: String): PendingToolExecution? {
        return pendingRequests[requestId]
    }

    /**
     * Removes a tool request from the pending queue.
     * Called after the tool has been executed or the request has been denied.
     *
     * @param requestId The unique identifier of the tool request to remove
     */
    fun removeRequest(requestId: String) {
        pendingRequests.remove(requestId)
    }

    /**
     * Detects if a user question requires a tool based on keyword matching.
     *
     * Performs case-insensitive keyword matching against configured tool definitions.
     * Returns the first tool whose keywords match the question.
     *
     * @param question The user's question to analyze
     * @return ToolOffer with tool name and description if a match is found, null otherwise
     */
    fun detectToolRequirement(question: String): ToolOffer? {
        val lowerQuestion = question.lowercase()
        
        return toolsConfiguration.tools.firstOrNull { toolDef ->
            toolDef.keywords.any { keyword -> lowerQuestion.contains(keyword.lowercase()) }
        }?.let { toolDef ->
            ToolOffer(toolDef.name, toolDef.description)
        }
    }

    /**
     * Retrieves the execution prompt for a specific tool.
     *
     * The execution prompt is sent to the LLM to instruct it on how to call the tool.
     * Prompts are configured per tool in application.yml for environment-specific customization.
     *
     * @param toolName Name of the tool
     * @return Tool-specific execution prompt, or a generic fallback if not configured
     */
    fun getToolExecutionPrompt(toolName: String): String {
        return toolsConfiguration.tools
            .firstOrNull { it.name == toolName }
            ?.executionPrompt
            ?.takeIf { it.isNotBlank() }
            ?: "Execute the $toolName function and show me the results"
    }
}
