package com.example.onboarder.feedback

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "chat_feedback")
class ChatFeedback(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: String? = null,
    
    @Column(nullable = false)
    var responseId: String = "",
    
    @Column(nullable = false, length = 2000)
    var question: String = "",
    
    @Column(nullable = false, length = 5000)
    var response: String = "",
    
    @Column(nullable = false)
    var rating: Boolean = false,
    
    @Column(length = 1000)
    var comment: String? = null,
    
    @Column(nullable = false)
    var timestamp: Instant = Instant.now()
)
