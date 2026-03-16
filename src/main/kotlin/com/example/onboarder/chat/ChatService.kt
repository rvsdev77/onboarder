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
 * company policy context from the vector store with AI reasoning capabilities.
 * Responses are optionally improved by a [ReflectionAdvisor] that critiques
 * and rewrites answers before they are returned to the caller.
 *
 * @property chatClientBuilder Builder for creating chat clients.
 * @property ragAdvisor Advisor that retrieves and injects relevant document context.
 * @property reflectionAdvisor Advisor that critiques and optionally rewrites responses.
 * @property systemPromptResource External file containing the system prompt.
 * @property toolsService Manages tool detection and execution approval flow.
 * @property feedbackService Stores response context for later feedback correlation.
 */
@Service
class ChatService(
    private val chatClientBuilder: ChatClient.Builder,
    private val ragAdvisor: Advisor,
    private val reflectionAdvisor: ReflectionAdvisor,
    @Value("classpath:prompts/system-prompt.txt") private val systemPromptResource: Resource,
    private val toolsService: ToolsService,
    private val feedbackService: com.example.onboarder.feedback.FeedbackService
) {
    private val systemPrompt: String by lazy {
        systemPromptResource.inputStream.bufferedReader().use { it.readText() }
    }

    /**
     * Processes a user question and returns an AI-generated answer.
     *
     * Handles both regular responses and tool approval flows.
     *
     * @param question The user's question.
     * @param toolRequestId Optional request ID for an approved tool execution.
     * @return [ChatResponse] with the answer and/or a pending tool request.
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
     * Retrieves the pending tool request by ID, invokes the tool via a dedicated
     * chat client (no RAG advisor), and stores the result for feedback tracking.
     *
     * @param toolRequestId The ID of the approved tool request.
     * @return [ChatResponse] with the tool execution result.
     */
    private fun executeApprovedTool(toolRequestId: String): ChatResponse {
        val pending = toolsService.getPendingRequest(toolRequestId)
            ?: return ChatResponse(response = "Tool request not found or expired.")

        toolsService.removeRequest(toolRequestId)

        val toolClient = chatClientBuilder
            .defaultFunctions(pending.toolName)
            .build()

        val toolPrompt = toolsService.getToolExecutionPrompt(pending.toolName)

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
     * Generates a regular RAG-backed response with optional self-reflection.
     *
     * Tool detection runs first — if the question matches a tool, reflection is skipped
     * to avoid unnecessary latency. Otherwise the advisor chain is:
     * RAG retrieval → self-reflection critique/rewrite.
     *
     * @param question The user's question.
     * @return [ChatResponse] with the (possibly rewritten) answer and an optional tool request.
     */
    private fun getRegularResponse(question: String): ChatResponse {
        val toolOffer = toolsService.detectToolRequirement(question)

        val advisors = if (toolOffer != null) arrayOf(ragAdvisor) else arrayOf(ragAdvisor, reflectionAdvisor)
        val chatClient = chatClientBuilder
            .defaultAdvisors(*advisors)
            .build()

        val chatResponse = chatClient
            .prompt()
            .system(systemPrompt)
            .user(question)
            .call()
            .chatResponse()

        val result = chatResponse?.result?.output?.content ?: "Unable to generate response."
        val responseId = java.util.UUID.randomUUID().toString()
        feedbackService.storeResponseContext(responseId, question, result)

        if (toolOffer != null) {
            val requestId = toolsService.createToolRequest(toolOffer.toolName, question)
            return ChatResponse(
                response = result,
                toolRequest = ToolRequest(toolOffer.toolName, toolOffer.description, requestId),
                responseId = responseId
            )
        }

        return ChatResponse(response = result, responseId = responseId)
    }
}
