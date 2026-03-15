package com.example.onboarder.chat

import com.example.onboarder.feedback.FeedbackMetrics
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.ai.document.Document
import org.springframework.ai.vectorstore.SearchRequest
import org.springframework.ai.vectorstore.VectorStore
import kotlin.test.assertEquals

class ChatRestControllerTest {

    @Test
    fun `should return chat response`() = runTest {
        val mockChatService = mock<ChatService>()
        val mockVectorStore = mock<VectorStore>()
        val mockFeedbackService = mock<com.example.onboarder.feedback.FeedbackService>()
        
        whenever(mockChatService.getAnswer("What is PTO?", null)).thenReturn(
            ChatResponse(response = "22 days per year", responseId = "test-id-123")
        )
        
        val controller = ChatRestController(mockChatService, mockVectorStore, mockFeedbackService)
        val request = ChatRequest("What is PTO?")
        
        val response = controller.chat(request)
        
        assertEquals("22 days per year", response.response)
        assertEquals("test-id-123", response.responseId)
    }

    @Test
    fun `should return chat response with null response field`() = runTest {
        val mockChatService = mock<ChatService>()
        val mockVectorStore = mock<VectorStore>()
        val mockFeedbackService = mock<com.example.onboarder.feedback.FeedbackService>()
        
        whenever(mockChatService.getAnswer("Unknown question", null)).thenReturn(ChatResponse(response = null))
        
        val controller = ChatRestController(mockChatService, mockVectorStore, mockFeedbackService)
        val request = ChatRequest("Unknown question")
        
        val response = controller.chat(request)
        
        assertEquals(null, response.response)
    }

    @Test
    fun `should return debug results with vector search`() = runTest {
        val mockChatService = mock<ChatService>()
        val mockVectorStore = mock<VectorStore>()
        val mockFeedbackService = mock<com.example.onboarder.feedback.FeedbackService>()
        val mockDocument = Document("Test content", mapOf("distance" to 0.15))
        
        whenever(mockVectorStore.similaritySearch(any<SearchRequest>())).thenReturn(listOf(mockDocument))
        
        val controller = ChatRestController(mockChatService, mockVectorStore, mockFeedbackService)
        
        val result = controller.debug("test query")
        
        assertEquals("test query", result["question"])
        assertEquals(1, result["resultsCount"])
    }

    @Test
    fun `should handle null vector search results`() = runTest {
        val mockChatService = mock<ChatService>()
        val mockVectorStore = mock<VectorStore>()
        val mockFeedbackService = mock<com.example.onboarder.feedback.FeedbackService>()
        
        whenever(mockVectorStore.similaritySearch(any<SearchRequest>())).thenReturn(null)
        
        val controller = ChatRestController(mockChatService, mockVectorStore, mockFeedbackService)
        
        val result = controller.debug("test query")
        
        assertEquals("test query", result["question"])
        assertEquals(0, result["resultsCount"])
    }

    @Test
    fun `should submit feedback successfully`() = runTest {
        val mockChatService = mock<ChatService>()
        val mockVectorStore = mock<VectorStore>()
        val mockFeedbackService = mock<com.example.onboarder.feedback.FeedbackService>()
        val mockFeedback = mock<com.example.onboarder.feedback.ChatFeedback>()
        
        whenever(mockFeedbackService.submitFeedback("response-123", true, "Helpful")).thenReturn(mockFeedback)
        
        val controller = ChatRestController(mockChatService, mockVectorStore, mockFeedbackService)
        val request = FeedbackRequest("response-123", true, "Helpful")
        
        val result = controller.submitFeedback(request)
        
        assertEquals("success", result["status"])
        assertEquals("Thank you for your feedback!", result["message"])
    }

    @Test
    fun `should handle feedback for unknown response`() = runTest {
        val mockChatService = mock<ChatService>()
        val mockVectorStore = mock<VectorStore>()
        val mockFeedbackService = mock<com.example.onboarder.feedback.FeedbackService>()
        
        whenever(mockFeedbackService.submitFeedback("unknown-id", true, null)).thenReturn(null)
        
        val controller = ChatRestController(mockChatService, mockVectorStore, mockFeedbackService)
        val request = FeedbackRequest("unknown-id", true, null)
        
        val result = controller.submitFeedback(request)
        
        assertEquals("error", result["status"])
        assertEquals("Response not found or expired", result["message"])
    }

    @Test
    fun `should return metrics`() = runTest {
        val mockChatService = mock<ChatService>()
        val mockVectorStore = mock<VectorStore>()
        val mockFeedbackService = mock<com.example.onboarder.feedback.FeedbackService>()
        val mockMetrics = FeedbackMetrics(
            totalResponses = 10,
            positiveCount = 8,
            negativeCount = 2,
            successRate = 80.0,
            recentComments = emptyList()
        )
        
        whenever(mockFeedbackService.getMetrics()).thenReturn(mockMetrics)
        
        val controller = ChatRestController(mockChatService, mockVectorStore, mockFeedbackService)
        
        val result = controller.getMetrics()
        
        assertEquals(10, result.totalResponses)
        assertEquals(80.0, result.successRate)
    }
}
