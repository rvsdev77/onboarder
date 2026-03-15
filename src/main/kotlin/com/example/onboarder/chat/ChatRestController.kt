package com.example.onboarder.chat

import org.springframework.ai.vectorstore.SearchRequest
import org.springframework.web.bind.annotation.*

/**
 * Request payload for chat endpoint.
 * @property message The user's question or message
 */
data class ChatRequest(val message: String)

/**
 * Response payload for chat endpoint.
 * @property response The AI-generated answer
 * @property responseId Unique identifier for tracking feedback
 */
data class ChatResponse(
    val response: String? = null,
    val toolRequest: ToolRequest? = null,
    val responseId: String? = null
)

data class ToolRequest(
    val toolName: String,
    val description: String,
    val requestId: String
)

data class ToolApprovalRequest(
    val requestId: String,
    val approved: Boolean
)

data class FeedbackRequest(
    val responseId: String,
    val rating: Boolean,
    val comment: String? = null
)

/**
 * REST controller providing chat and debugging endpoints.
 * 
 * Exposes two main endpoints:
 * - POST /api/chat: Main chat interface for user questions
 * - GET /api/chat/debug: Debug endpoint for testing vector similarity search
 * 
 * @property chatService Service handling AI chat interactions
 * @property vectorStore Vector store for similarity search debugging
 */
@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = ["*"])
class ChatRestController(
    private val chatService: ChatService,
    private val vectorStore: org.springframework.ai.vectorstore.VectorStore,
    private val feedbackService: com.example.onboarder.feedback.FeedbackService
) {

    /**
     * Main chat endpoint for processing user questions.
     * 
     * @param request Chat request containing the user's message
     * @return Chat response with AI-generated answer
     */
    @PostMapping
    suspend fun chat(@RequestBody request: ChatRequest): ChatResponse {
        return chatService.getAnswer(request.message)
    }

    @PostMapping("/approve")
    suspend fun approveToolExecution(@RequestBody approval: ToolApprovalRequest): ChatResponse {
        return if (approval.approved) {
            chatService.getAnswer("", approval.requestId)
        } else {
            ChatResponse(response = "Tool execution cancelled. How else can I help you?")
        }
    }

    @PostMapping("/feedback")
    fun submitFeedback(@RequestBody request: FeedbackRequest): Map<String, String> {
        val feedback = feedbackService.submitFeedback(request.responseId, request.rating, request.comment)
        return if (feedback != null) {
            mapOf("status" to "success", "message" to "Thank you for your feedback!")
        } else {
            mapOf("status" to "error", "message" to "Response not found or expired")
        }
    }

    @GetMapping("/metrics")
    fun getMetrics(): com.example.onboarder.feedback.FeedbackMetrics {
        return feedbackService.getMetrics()
    }

    /**
     * Debug endpoint for testing vector similarity search.
     * 
     * Returns the top 5 most similar document chunks for a given question,
     * along with similarity scores. Useful for:
     * - Testing vector search quality
     * - Debugging RAG context retrieval
     * - Verifying document embeddings
     * - Checking similarity thresholds
     * 
     * @param question The search query
     * @return Map containing question, result count, and matched chunks with scores
     */
    @GetMapping("/debug")
    suspend fun debug(@RequestParam question: String): Map<String, Any> {
        val searchRequest = SearchRequest.Builder()
            .query(question)
            .topK(5)
            .build()
        val results = vectorStore.similaritySearch(searchRequest)
        return if (results != null) {
            mapOf(
                "question" to question,
                "resultsCount" to results.size,
                "results" to results.map {
                    mapOf(
                        "content" to it?.text?.take(200),
                        "score" to it.metadata["distance"]
                    )
                }
            )
        } else {
            mapOf("question" to question, "resultsCount" to 0)
        }
    }
}