package com.example.onboarder.feedback

import com.example.onboarder.chat.ChatRequest
import com.example.onboarder.chat.ChatRestController
import com.example.onboarder.chat.ChatService
import com.example.onboarder.chat.FeedbackRequest
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.client.advisor.api.Advisor
import org.springframework.ai.chat.model.Generation
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.core.io.ByteArrayResource
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import org.springframework.ai.chat.model.ChatResponse as AiChatResponse

class FeedbackIntegrationTest {

    @Test
    fun `should complete full feedback flow from chat to metrics`() = runTest {
        // Setup
        val feedbackRepository = mock<ChatFeedbackRepository>()
        val feedbackService = FeedbackService(feedbackRepository)
        
        val mockBuilder = mock<ChatClient.Builder>()
        val mockAdvisor = mock<Advisor>()
        val mockToolsService = mock<com.example.onboarder.chat.ToolsService>()
        val systemPromptResource = ByteArrayResource("Test prompt".toByteArray())
        
        val mockChatClient = mock<ChatClient>()
        val mockRequestSpec = mock<ChatClient.ChatClientRequestSpec>()
        val mockCallResponseSpec = mock<ChatClient.CallResponseSpec>()
        val mockAiChatResponse = mock<AiChatResponse>()
        val mockGeneration = mock<Generation>()
        
        whenever(mockBuilder.defaultAdvisors(any<Advisor>())).thenReturn(mockBuilder)
        whenever(mockBuilder.build()).thenReturn(mockChatClient)
        whenever(mockChatClient.prompt()).thenReturn(mockRequestSpec)
        whenever(mockRequestSpec.system(any<String>())).thenReturn(mockRequestSpec)
        whenever(mockRequestSpec.user(any<String>())).thenReturn(mockRequestSpec)
        whenever(mockRequestSpec.call()).thenReturn(mockCallResponseSpec)
        whenever(mockCallResponseSpec.chatResponse()).thenReturn(mockAiChatResponse)
        whenever(mockAiChatResponse.result).thenReturn(mockGeneration)
        whenever(mockGeneration.output).thenReturn(mock())
        whenever(mockGeneration.output.content).thenReturn("Test answer")
        whenever(mockToolsService.detectToolRequirement(any())).thenReturn(null)
        
        val chatService = ChatService(
            mockBuilder,
            mockAdvisor,
            systemPromptResource,
            mockToolsService,
            feedbackService
        )
        
        val mockVectorStore = mock<VectorStore>()
        val controller = ChatRestController(chatService, mockVectorStore, feedbackService)
        
        // Step 1: User asks a question
        val chatRequest = ChatRequest("What is the vacation policy?")
        val chatResponse = controller.chat(chatRequest)
        
        assertNotNull(chatResponse.response)
        assertNotNull(chatResponse.responseId)
        val responseId = chatResponse.responseId
        
        // Step 2: User submits positive feedback
        val savedFeedback = ChatFeedback(
            id = "feedback-1",
            responseId = responseId,
            question = "What is the vacation policy?",
            response = "Test answer",
            rating = true,
            comment = "Great answer!"
        )
        whenever(feedbackRepository.save(any<ChatFeedback>())).thenReturn(savedFeedback)
        
        val feedbackRequest = FeedbackRequest(responseId, true, "Great answer!")
        val feedbackResponse = controller.submitFeedback(feedbackRequest)
        
        assertEquals("success", feedbackResponse["status"])
        verify(feedbackRepository).save(any<ChatFeedback>())
        
        // Step 3: Check metrics
        whenever(feedbackRepository.count()).thenReturn(1)
        whenever(feedbackRepository.countPositive()).thenReturn(1)
        whenever(feedbackRepository.countNegative()).thenReturn(0)
        whenever(feedbackRepository.findTop10ByOrderByTimestampDesc()).thenReturn(listOf(savedFeedback))
        
        val metrics = controller.getMetrics()
        
        assertEquals(1, metrics.totalResponses)
        assertEquals(1, metrics.positiveCount)
        assertEquals(100.0, metrics.successRate)
        assertEquals(1, metrics.recentComments.size)
    }

    @Test
    fun `should handle negative feedback correctly`() = runTest {
        val feedbackRepository = mock<ChatFeedbackRepository>()
        val feedbackService = FeedbackService(feedbackRepository)
        
        val responseId = "test-response-id"
        feedbackService.storeResponseContext(responseId, "Test question", "Test answer")
        
        val savedFeedback = ChatFeedback(
            id = "feedback-2",
            responseId = responseId,
            question = "Test question",
            response = "Test answer",
            rating = false,
            comment = "Not helpful"
        )
        whenever(feedbackRepository.save(any<ChatFeedback>())).thenReturn(savedFeedback)
        
        val result = feedbackService.submitFeedback(responseId, false, "Not helpful")
        
        assertNotNull(result)
        assertEquals(false, result.rating)
        assertEquals("Not helpful", result.comment)
    }
}
