package com.example.onboarder.ingestion

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class VectorStoreStartupInitializer(
    private val repository: VectorStoreInitStatusRepository,
    private val documentIndexer: DocumentIndexer,
    @Value("\${onboarder.vectorstore.force-reinit:false}")
    private val forceReinit: Boolean
) : CommandLineRunner {

    private val logger = LoggerFactory.getLogger(VectorStoreStartupInitializer::class.java)

    override fun run(vararg args: String?) {
        val status = repository.findById("default").orElse(VectorStoreInitStatus())

        if (forceReinit || !status.isInitialized) {
            logger.info("Initializing vector store...")
            documentIndexer.index()
            repository.save(status.copy(isInitialized = true, lastInitializedAt = LocalDateTime.now()))
            logger.info("Vector store initialized successfully")
        } else {
            logger.info("Vector store already initialized (last: ${status.lastInitializedAt}), skipping")
        }
    }
}
