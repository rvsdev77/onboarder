package com.example.onboarder.ingestion

import org.junit.jupiter.api.Test
import org.springframework.ai.document.Document
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DefaultContentSplitterTest {

    @Test
    fun `should split document with default parameters`() {
        val splitter = DefaultContentSplitter(
            chunkSize = 500,
            minChunkSizeChars = 150,
            minChunkLengthToEmbed = 10,
            maxNumChunks = 10000,
            keepSeparator = true
        )
        val longText = "word ".repeat(1000)
        val docs = listOf(Document(longText))

        val result = splitter.split(docs)

        assertTrue(result.isNotEmpty())
        assertEquals(2, result.size)
    }

    @Test
    fun `should split document with custom chunk size`() {
        val splitter = DefaultContentSplitter(
            chunkSize = 100,
            minChunkSizeChars = 150,
            minChunkLengthToEmbed = 10,
            maxNumChunks = 10000,
            keepSeparator = true
        )
        val longText = "word ".repeat(500)
        val docs = listOf(Document(longText))

        val result = splitter.split(docs)

        assertEquals(5, result.size)
    }

    @Test
    fun `should handle empty document list`() {
        val splitter = DefaultContentSplitter(
            chunkSize = 500,
            minChunkSizeChars = 150,
            minChunkLengthToEmbed = 10,
            maxNumChunks = 10000,
            keepSeparator = true
        )
        val result = splitter.split(emptyList())

        assertEquals(0, result.size)
    }

    @Test
    fun `should handle single short document`() {
        val splitter = DefaultContentSplitter(
            chunkSize = 500,
            minChunkSizeChars = 10,  // Lower threshold for short documents
            minChunkLengthToEmbed = 5,
            maxNumChunks = 10000,
            keepSeparator = true
        )
        val docs = listOf(Document("Short text"))

        val result = splitter.split(docs)

        assertEquals(1, result.size)
    }
}
