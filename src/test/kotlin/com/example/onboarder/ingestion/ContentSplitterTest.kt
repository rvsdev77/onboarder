package com.example.onboarder.ingestion

import org.junit.jupiter.api.Test
import org.springframework.ai.document.Document
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ContentSplitterTest {

    @Test
    fun `should split large document into multiple chunks`() {
        val splitter = DefaultContentSplitter(
            chunkSize = 200,
            minChunkSizeChars = 50,
            minChunkLengthToEmbed = 10,
            maxNumChunks = 10000,
            keepSeparator = true
        )
        val longText = "This is a test sentence. ".repeat(100)
        val docs = listOf(Document(longText))

        val result = splitter.split(docs)

        assertTrue(result.size > 1)
    }

    @Test
    fun `should handle multiple documents`() {
        val splitter = DefaultContentSplitter(
            chunkSize = 500,
            minChunkSizeChars = 50,
            minChunkLengthToEmbed = 10,
            maxNumChunks = 10000,
            keepSeparator = true
        )
        val docs = listOf(
            Document("word ".repeat(500)),
            Document("word ".repeat(500))
        )

        val result = splitter.split(docs)

        assertTrue(result.size >= 2)
    }

    @Test
    fun `should filter out documents below minimum length`() {
        val splitter = DefaultContentSplitter(
            chunkSize = 500,
            minChunkSizeChars = 200,
            minChunkLengthToEmbed = 50,
            maxNumChunks = 10000,
            keepSeparator = true
        )
        val docs = listOf(Document("tiny"))

        val result = splitter.split(docs)

        assertEquals(0, result.size)
    }
}
