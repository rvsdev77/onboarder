package com.example.onboarder.ingestion

import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.ai.document.Document
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.core.io.Resource

class DocumentIndexerTest {

    @Test
    fun `should execute complete indexing pipeline`() {
        val mockReader = mock<DocumentReader>()
        val mockSplitter = mock<ContentSplitter>()
        val mockEnricher = mock<KeywordEnricher>()
        val mockVectorStore = mock<VectorStore>()
        
        val document = Document("Test content")
        val splitDocs = listOf(Document("Chunk 1"), Document("Chunk 2"))
        val enrichedDocs = listOf(Document("Enriched 1"), Document("Enriched 2"))
        
        whenever(mockReader.read(any<Resource>())).thenReturn(listOf(document))
        whenever(mockSplitter.split(any())).thenReturn(splitDocs)
        whenever(mockEnricher.enrich(any())).thenReturn(enrichedDocs)
        
        val indexer = DocumentIndexer(mockReader, mockSplitter, mockEnricher, mockVectorStore)
        
        indexer.index()
        
        verify(mockReader).read(any())
        verify(mockSplitter).split(listOf(document))
        verify(mockEnricher).enrich(splitDocs)
        verify(mockVectorStore).add(enrichedDocs)
    }

    @Test
    fun `should handle empty document list`() {
        val mockReader = mock<DocumentReader>()
        val mockSplitter = mock<ContentSplitter>()
        val mockEnricher = mock<KeywordEnricher>()
        val mockVectorStore = mock<VectorStore>()
        
        whenever(mockReader.read(any<Resource>())).thenReturn(emptyList())
        whenever(mockSplitter.split(any())).thenReturn(emptyList())
        whenever(mockEnricher.enrich(any())).thenReturn(emptyList())
        
        val indexer = DocumentIndexer(mockReader, mockSplitter, mockEnricher, mockVectorStore)
        
        indexer.index()
        
        verify(mockVectorStore).add(emptyList())
    }
}
