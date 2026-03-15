package com.example.onboarder.ingestion

import org.springframework.ai.document.Document
import org.springframework.ai.reader.pdf.PagePdfDocumentReader
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig
import org.springframework.core.io.Resource
import org.springframework.stereotype.Component

/**
 * Interface for reading documents from various sources.
 */
interface DocumentReader {
    /**
     * Reads a document from the given resource.
     * 
     * @param resource The resource to read from
     * @return List of parsed documents
     */
    fun read(resource: Resource): List<Document>
}

/**
 * PDF document reader implementation.
 * 
 * Reads PDF files page-by-page with minimal margins to maximize content extraction.
 * Each page is treated as a separate document for better chunking granularity.
 * 
 * Configuration:
 * - Pages per document: 1 (each page is a separate document)
 * - Top/bottom margins: 0 (preserve all content)
 */
@Component
class PdfDocumentReader : DocumentReader {

    override fun read(resource: Resource): List<Document> {
        val config = PdfDocumentReaderConfig.builder()
            .withPageTopMargin(0)
            .withPageBottomMargin(0)
            .withPagesPerDocument(1)
            .build()
            
        return PagePdfDocumentReader(resource, config).read()
    }
}