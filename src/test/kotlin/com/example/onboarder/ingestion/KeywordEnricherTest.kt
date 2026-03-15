package com.example.onboarder.ingestion

import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.ai.chat.model.ChatModel
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.ai.document.Document
import kotlin.test.assertEquals

class KeywordEnricherTest {

    @Test
    fun `should return original documents when enrichment fails`() {
        val mockChatModel = mock<ChatModel>()
        val enricher = DefaultKeywordEnricher(mockChatModel)
        
        val docs = listOf(Document("Test content"))
        
        // ChatModel will throw exception when called
        whenever(mockChatModel.call(any<Prompt>())).thenThrow(RuntimeException("API error"))
        
        val result = enricher.enrich(docs)
        
        assertEquals(docs, result)
    }

    @Test
    fun `should handle empty document list`() {
        val mockChatModel = mock<ChatModel>()
        val enricher = DefaultKeywordEnricher(mockChatModel)
        
        val result = enricher.enrich(emptyList())
        
        assertEquals(0, result.size)
    }

    @Test
    fun `should preserve document count after enrichment`() {
        val mockChatModel = mock<ChatModel>()
        val enricher = DefaultKeywordEnricher(mockChatModel)
        
        val docs = listOf(
            Document("Content 1"),
            Document("Content 2"),
            Document("Content 3")
        )
        
        // Will fail and return original docs
        whenever(mockChatModel.call(any<Prompt>())).thenThrow(RuntimeException("API error"))
        
        val result = enricher.enrich(docs)
        
        assertEquals(3, result.size)
    }
}
