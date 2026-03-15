package com.example.onboarder.chat

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.client.advisor.api.Advisor
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.stereotype.Service

/**
 * Service responsible for handling chat interactions with AI.
 * 
 * Uses RAG (Retrieval Augmented Generation) to answer questions by combining
 * company policy context from vector store with AI reasoning capabilities.
 * Supports Chain-of-Thought, ReAct prompting, self-reflection, and clarifying questions.
 * 
 * @property systemPrompt Loaded from external file for easy maintenance
 */
@Service
class ChatService(
    private val chatClientBuilder: ChatClient.Builder,
    private val ragAdvisor: Advisor,
    @Value("classpath:prompts/system-prompt.txt") private val systemPromptResource: Resource,
    private val toolsService: ToolsService,
    private val feedbackService: com.example.onboarder.feedback.FeedbackService
) {
    private val systemPrompt: String by lazy {
        systemPromptResource.inputStream.bufferedReader().use { it.readText() }
    }

    /**
     * Processes a user question and returns an AI-generated answer.
     * Handles both regular responses and tool approval flows.
     * 
     * @param question The user's question
     * @param toolRequestId Optional request ID for approved tool execution
     * @return ChatResponse with answer and/or tool request
     */
    suspend fun getAnswer(question: String, toolRequestId: String? = null): ChatResponse {
        return withContext(Dispatchers.IO) {
            if (toolRequestId != null) {
                executeApprovedTool(toolRequestId)
            } else {
                getRegularResponse(question)
            }
        }
    }

    /**
     * Executes an approved tool and returns the result.
     * 
     * @param toolRequestId The ID of the approved tool request
     * @return ChatResponse with tool execution result
     */
    private fun executeApprovedTool(toolRequestId: String): ChatResponse {
        val pending = toolsService.getPendingRequest(toolRequestId)
        if (pending == null) {
            return ChatResponse(response = "Tool request not found or expired.")
        }
        
        toolsService.removeRequest(toolRequestId)
        
        // Create a client with ONLY the tool function, no RAG advisor
        val toolClient = chatClientBuilder
            .defaultFunctions(pending.toolName)
            .build()
        
        // Get tool-specific prompt from ToolsService
        val toolPrompt = toolsService.getToolExecutionPrompt(pending.toolName)
        
        // Execute with the approved tool enabled
        val chatResponse = toolClient
            .prompt()
            .system("You MUST call the ${pending.toolName} function to retrieve the data. Do not answer without calling the function.")
            .user(toolPrompt)
            .call()
            .chatResponse()
        
        val result = chatResponse?.result?.output?.content ?: "Unable to execute tool."
        val responseId = java.util.UUID.randomUUID().toString()
        feedbackService.storeResponseContext(responseId, pending.userMessage, result)
        
        return ChatResponse(response = result, responseId = responseId)
    }

    /**
     * Generates a regular response without tool execution.
     * Detects if a tool should be offered after the response.
     * 
     * @param question The user's question
     * @return ChatResponse with answer and optional tool request
     */
    private fun getRegularResponse(question: String): ChatResponse {
        // Create chat client without functions to prevent premature tool calls
        val chatClientWithoutFunctions = chatClientBuilder
            .defaultAdvisors(ragAdvisor)
            .build()
        
        val chatResponse = chatClientWithoutFunctions
            .prompt()
            .system(systemPrompt)
            .user(question)
            .call()
            .chatResponse()
        
        val result = chatResponse?.result?.output?.content ?: "Unable to generate response."
        val responseId = java.util.UUID.randomUUID().toString()
        feedbackService.storeResponseContext(responseId, question, result)

        // Check if we should offer tool execution after the response
        val toolOffer = toolsService.detectToolRequirement(question)
        if (toolOffer != null) {
            val requestId = toolsService.createToolRequest(toolOffer.toolName, question)
            return ChatResponse(
                response = result,
                toolRequest = ToolRequest(
                    toolName = toolOffer.toolName,
                    description = toolOffer.description,
                    requestId = requestId
                ),
                responseId = responseId
            )
        }

        return ChatResponse(response = result, responseId = responseId)
    }
}