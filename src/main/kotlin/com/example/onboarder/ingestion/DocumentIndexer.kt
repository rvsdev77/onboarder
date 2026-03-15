package com.example.onboarder.ingestion

import org.slf4j.LoggerFactory
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Component

/**
 * Orchestrates the document ingestion pipeline.
 * 
 * Coordinates the complete process of loading, processing, and indexing documents:
 * 1. Read PDF documents from resources
 * 2. Split documents into smaller chunks
 * 3. Enrich chunks with AI-generated keywords
 * 4. Store embeddings in vector store
 * 
 * @property documentReader Reads PDF documents
 * @property splitter Splits documents into chunks
 * @property keywordEnricher Adds keyword metadata to chunks
 * @property vectorStore Stores document embeddings
 */
@Component
class DocumentIndexer(
    private val documentReader: DocumentReader,
    private val splitter: ContentSplitter,
    private val keywordEnricher: KeywordEnricher,
    private val vectorStore: VectorStore
) {
    private val logger = LoggerFactory.getLogger(DocumentIndexer::class.java)

    /**
     * Executes the complete document indexing pipeline.
     * 
     * Loads company_policy.pdf from resources, processes it through the pipeline,
     * and stores the resulting embeddings in the vector store.
     * Logs progress at each stage for monitoring and debugging.
     */
    fun index() {
        val resource = ClassPathResource("rag/company_policy.pdf")
        val documents = documentReader.read(resource)
        logger.info("Read {} documents from PDF", documents.size)

        val splitDocuments = splitter.split(documents)
        logger.info("Split into {} chunks", splitDocuments.size)

        val enrichedDocuments = keywordEnricher.enrich(splitDocuments)
        logger.info("Enriched {} documents", enrichedDocuments.size)
        
        vectorStore.add(enrichedDocuments)
        logger.info("Added {} documents to vector store", enrichedDocuments.size)
    }
}