package com.example.onboarder.feedback

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class FeedbackServiceTest {

    private lateinit var mockRepository: ChatFeedbackRepository
    private lateinit var feedbackService: FeedbackService

    @BeforeEach
    fun setup() {
        mockRepository = mock<ChatFeedbackRepository>()
        feedbackService = FeedbackService(mockRepository)
    }

    @Test
    fun `should store and retrieve response context`() {
        val responseId = "test-id-123"
        val question = "What is PTO?"
        val response = "PTO is paid time off"
        
        feedbackService.storeResponseContext(responseId, question, response)
        
        val mockFeedback = ChatFeedback(
            id = "feedback-1",
            responseId = responseId,
            question = question,
            response = response,
            rating = true
        )
        whenever(mockRepository.save(any<ChatFeedback>())).thenReturn(mockFeedback)
        
        val result = feedbackService.submitFeedback(responseId, true, null)
        
        assertNotNull(result)
        assertEquals(responseId, result.responseId)
        verify(mockRepository).save(any<ChatFeedback>())
    }

    @Test
    fun `should return null when submitting feedback for unknown responseId`() {
        val result = feedbackService.submitFeedback("unknown-id", true, null)
        
        assertNull(result)
    }

    @Test
    fun `should submit feedback with comment`() {
        val responseId = "test-id-456"
        val question = "How many vacation days?"
        val response = "15 days per year"
        val comment = "Very helpful!"
        
        feedbackService.storeResponseContext(responseId, question, response)
        
        val mockFeedback = ChatFeedback(
            id = "feedback-2",
            responseId = responseId,
            question = question,
            response = response,
            rating = true,
            comment = comment
        )
        whenever(mockRepository.save(any<ChatFeedback>())).thenReturn(mockFeedback)
        
        val result = feedbackService.submitFeedback(responseId, true, comment)
        
        assertNotNull(result)
        assertEquals(comment, result.comment)
    }

    @Test
    fun `should calculate metrics correctly`() {
        whenever(mockRepository.count()).thenReturn(10)
        whenever(mockRepository.countPositive()).thenReturn(8)
        whenever(mockRepository.countNegative()).thenReturn(2)
        whenever(mockRepository.findTop10ByOrderByTimestampDesc()).thenReturn(
            listOf(
                ChatFeedback(
                    id = "1",
                    responseId = "r1",
                    question = "Q1",
                    response = "A1",
                    rating = true,
                    comment = "Great!",
                    timestamp = Instant.now()
                )
            )
        )
        
        val metrics = feedbackService.getMetrics()
        
        assertEquals(10, metrics.totalResponses)
        assertEquals(8, metrics.positiveCount)
        assertEquals(2, metrics.negativeCount)
        assertEquals(80.0, metrics.successRate)
        assertEquals(1, metrics.recentComments.size)
    }

    @Test
    fun `should handle zero feedback for metrics`() {
        whenever(mockRepository.count()).thenReturn(0)
        whenever(mockRepository.countPositive()).thenReturn(0)
        whenever(mockRepository.countNegative()).thenReturn(0)
        whenever(mockRepository.findTop10ByOrderByTimestampDesc()).thenReturn(emptyList())
        
        val metrics = feedbackService.getMetrics()
        
        assertEquals(0, metrics.totalResponses)
        assertEquals(0.0, metrics.successRate)
    }

    @Test
    fun `should filter out empty comments in metrics`() {
        whenever(mockRepository.count()).thenReturn(3)
        whenever(mockRepository.countPositive()).thenReturn(2)
        whenever(mockRepository.countNegative()).thenReturn(1)
        whenever(mockRepository.findTop10ByOrderByTimestampDesc()).thenReturn(
            listOf(
                ChatFeedback(
                    id = "1",
                    responseId = "r1",
                    question = "Q1",
                    response = "A1",
                    rating = true,
                    comment = "Good",
                    timestamp = Instant.now()
                ),
                ChatFeedback(
                    id = "2",
                    responseId = "r2",
                    question = "Q2",
                    response = "A2",
                    rating = true,
                    comment = null,
                    timestamp = Instant.now()
                ),
                ChatFeedback(
                    id = "3",
                    responseId = "r3",
                    question = "Q3",
                    response = "A3",
                    rating = false,
                    comment = "",
                    timestamp = Instant.now()
                )
            )
        )
        
        val metrics = feedbackService.getMetrics()
        
        assertEquals(1, metrics.recentComments.size)
        assertEquals("Good", metrics.recentComments[0].comment)
    }
}
