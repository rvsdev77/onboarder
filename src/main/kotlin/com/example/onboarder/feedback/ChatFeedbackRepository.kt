package com.example.onboarder.feedback

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface ChatFeedbackRepository : JpaRepository<ChatFeedback, String> {
    
    @Query("SELECT COUNT(f) FROM ChatFeedback f WHERE f.rating = true")
    fun countPositive(): Long
    
    @Query("SELECT COUNT(f) FROM ChatFeedback f WHERE f.rating = false")
    fun countNegative(): Long
    
    fun findTop10ByOrderByTimestampDesc(): List<ChatFeedback>
}
