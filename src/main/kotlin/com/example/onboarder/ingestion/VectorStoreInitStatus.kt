package com.example.onboarder.ingestion

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "vector_store_init_status")
data class VectorStoreInitStatus(
    @Id val id: String = "default",
    val isInitialized: Boolean = false,
    val lastInitializedAt: LocalDateTime? = null
)
