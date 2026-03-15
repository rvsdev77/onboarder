package com.example.onboarder.ingestion

import org.springframework.ai.document.Document
import org.springframework.ai.transformer.splitter.TokenTextSplitter
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

/**
 * Interface for splitting documents into smaller chunks.
 */
interface ContentSplitter {
    /**
     * Splits documents into smaller chunks suitable for embedding.
     * 
     * @param docs Documents to split
     * @return List of document chunks
     */
    fun split(docs: List<Document>): List<Document>
}

/**
 * Default content splitter using token-based splitting.
 * 
 * Splits documents into chunks based on token count to ensure chunks fit within
 * embedding model limits while maintaining semantic coherence.
 * 
 * Configuration properties (in application.yml):
 * - onboarder.splitter.chunk-size: Target size for each chunk in tokens (default: 500)
 * - onboarder.splitter.min-chunk-size-chars: Minimum chunk size in characters (default: 150)
 * - onboarder.splitter.min-chunk-length-to-embed: Minimum length required for embedding (default: 10)
 * - onboarder.splitter.max-num-chunks: Maximum number of chunks to create (default: 10000)
 * - onboarder.splitter.keep-separator: Whether to preserve separators in chunks (default: true)
 * 
 * @property chunkSize Target size for each chunk in tokens
 * @property minChunkSizeChars Minimum chunk size in characters
 * @property minChunkLengthToEmbed Minimum length required for embedding
 * @property maxNumChunks Maximum number of chunks to create
 * @property keepSeparator Whether to preserve separators in chunks
 */
@Component
class DefaultContentSplitter(
    @Value("\${onboarder.splitter.chunk-size:500}") private val chunkSize: Int,
    @Value("\${onboarder.splitter.min-chunk-size-chars:150}") private val minChunkSizeChars: Int,
    @Value("\${onboarder.splitter.min-chunk-length-to-embed:10}") private val minChunkLengthToEmbed: Int,
    @Value("\${onboarder.splitter.max-num-chunks:10000}") private val maxNumChunks: Int,
    @Value("\${onboarder.splitter.keep-separator:true}") private val keepSeparator: Boolean
) : ContentSplitter {

    private val splitter by lazy {
        TokenTextSplitter(
            chunkSize,
            minChunkSizeChars,
            minChunkLengthToEmbed,
            maxNumChunks,
            keepSeparator
        )
    }

    override fun split(docs: List<Document>): List<Document> {
        return splitter.split(docs)
    }
}