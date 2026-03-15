package com.example.onboarder.feedback

import org.springframework.stereotype.Service
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

data class ResponseContext(
    val question: String,
    val response: String
)

data class FeedbackMetrics(
    val totalResponses: Long,
    val positiveCount: Long,
    val negativeCount: Long,
    val successRate: Double,
    val recentComments: List<FeedbackComment>
)

data class FeedbackComment(
    val rating: Boolean,
    val comment: String,
    val timestamp: Instant
)

@Service
class FeedbackService(
    private val feedbackRepository: ChatFeedbackRepository
) {
    private val responseCache = ConcurrentHashMap<String, ResponseContext>()
    
    fun storeResponseContext(responseId: String, question: String, response: String) {
        responseCache[responseId] = ResponseContext(question, response)
    }
    
    fun submitFeedback(responseId: String, rating: Boolean, comment: String?): ChatFeedback? {
        val context = responseCache.remove(responseId) ?: return null
        
        val feedback = ChatFeedback(
            responseId = responseId,
            question = context.question,
            response = context.response,
            rating = rating,
            comment = comment
        )
        
        return feedbackRepository.save(feedback)
    }
    
    fun getMetrics(): FeedbackMetrics {
        val total = feedbackRepository.count()
        val positive = feedbackRepository.countPositive()
        val negative = feedbackRepository.countNegative()
        val successRate = if (total > 0) (positive.toDouble() / total * 100) else 0.0
        val recentFeedback = feedbackRepository.findTop10ByOrderByTimestampDesc()
        
        return FeedbackMetrics(
            totalResponses = total,
            positiveCount = positive,
            negativeCount = negative,
            successRate = successRate,
            recentComments = recentFeedback
                .filter { !it.comment.isNullOrBlank() }
                .take(5)
                .map { FeedbackComment(it.rating, it.comment!!, it.timestamp) }
        )
    }
}
