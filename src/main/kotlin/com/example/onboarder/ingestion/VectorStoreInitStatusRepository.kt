package com.example.onboarder.ingestion

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface VectorStoreInitStatusRepository : JpaRepository<VectorStoreInitStatus, String>
