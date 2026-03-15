package com.example.onboarder.ingestion

import org.slf4j.LoggerFactory
import org.springframework.ai.chat.model.ChatModel
import org.springframework.ai.document.Document
import org.springframework.ai.transformer.KeywordMetadataEnricher
import org.springframework.stereotype.Component

/**
 * Interface for enriching documents with metadata.
 */
interface KeywordEnricher {
    /**
     * Enriches documents with AI-generated keyword metadata.
     * 
     * @param docs Documents to enrich
     * @return Enriched documents with keyword metadata
     */
    fun enrich(docs: List<Document>): List<Document>
}

private const val DEFAULT_KEYWORD_COUNT = 5

/**
 * Default keyword enricher using AI to generate relevant keywords.
 * 
 * Uses OpenAI to analyze each document chunk and generate 5 relevant keywords
 * that are added to the document metadata. These keywords improve semantic search
 * by capturing alternative terminology and key concepts.
 * 
 * Features:
 * - Generates 5 keywords per document chunk
 * - Graceful error handling (returns original docs on failure)
 * - Detailed logging for monitoring
 * 
 * @property chatModel AI model for generating keywords
 */
@Component
class DefaultKeywordEnricher(private val chatModel: ChatModel) : KeywordEnricher {
    private val logger = LoggerFactory.getLogger(DefaultKeywordEnricher::class.java)

    override fun enrich(docs: List<Document>): List<Document> {
        return try {
            logger.info("Enriching {} documents with keywords", docs.size)
            val enriched = KeywordMetadataEnricher(chatModel, DEFAULT_KEYWORD_COUNT).apply(docs)
            logger.info("Successfully enriched {} documents", enriched.size)
            enriched
        } catch (e: Exception) {
            logger.error("Failed to enrich documents with keywords, returning original documents", e)
            docs
        }
    }

}