package com.example.onboarder.configuration

import com.knuddels.jtokkit.api.EncodingType
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.client.advisor.RetrievalAugmentationAdvisor
import org.springframework.ai.chat.client.advisor.api.Advisor
import org.springframework.ai.chat.prompt.PromptTemplate
import org.springframework.ai.embedding.BatchingStrategy
import org.springframework.ai.embedding.TokenCountBatchingStrategy
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter
import org.springframework.ai.rag.preretrieval.query.transformation.QueryTransformer
import org.springframework.ai.rag.preretrieval.query.transformation.RewriteQueryTransformer
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

private const val MAX_INPUT_TOKEN_COUNT = 8192
private const val RESERVE_PERCENTAGE = 0.15

private val EMPTY_CONTEXT_TEMPLATE = PromptTemplate(
    """
    Context: No specific company policy information found.
    
    Question: {query}
    """.trimIndent()
)

private val CONTEXT_PROMPT_TEMPLATE = PromptTemplate(
    """
    Context information is below.
    ---------------------
    {context}
    ---------------------
    Answer the question using the context if relevant, otherwise use your general knowledge.
    Never refuse to answer.
    
    Question: {query}
    """.trimIndent()
)

/**
 * Spring configuration for RAG (Retrieval Augmented Generation) components.
 * 
 * Configures the complete RAG pipeline including:
 * - Embedding batching strategy for efficient processing
 * - Query transformation for improved retrieval
 * - Document retrieval with similarity search
 * - Context augmentation for prompt engineering
 */
@Configuration
class OnboarderConfig {

    /**
     * Configures batching strategy for embedding operations.
     * 
     * Uses token counting to batch documents efficiently while staying within
     * model token limits (8192 tokens with 15% reserve).
     * 
     * @return Configured batching strategy
     */
    @Bean
    fun batchingStrategy(): BatchingStrategy = TokenCountBatchingStrategy(
        EncodingType.CL100K_BASE,
        MAX_INPUT_TOKEN_COUNT,
        RESERVE_PERCENTAGE
    )

    /**
     * Configures query transformer for improving retrieval quality.
     * 
     * Uses AI to rewrite user queries into more effective search queries,
     * improving the relevance of retrieved documents.
     * 
     * @param chatClientBuilder Builder for creating chat client
     * @return Configured query transformer
     */
    @Bean
    fun queryTransformer(chatClientBuilder: ChatClient.Builder): QueryTransformer {
        return RewriteQueryTransformer.builder()
            .chatClientBuilder(chatClientBuilder)
            .build()
    }

    /**
     * Configures the RAG advisor for chat interactions.
     * 
     * Combines query transformation, document retrieval, and context augmentation
     * to provide relevant company policy context to the AI.
     * 
     * Configuration:
     * - Similarity threshold: 0.15 (lower = more permissive)
     * - Top K: 5 documents retrieved per query
     * - Allows empty context for general knowledge questions
     * 
     * @param queryTransformer Transformer for improving queries
     * @param vectorStore Vector store containing document embeddings
     * @return Configured RAG advisor
     */
    @Bean
    fun ragAdvisor(
        queryTransformer: QueryTransformer,
        vectorStore: VectorStore,
    ): Advisor {
        val augmenter = ContextualQueryAugmenter.builder()
            .allowEmptyContext(true)
            .emptyContextPromptTemplate(EMPTY_CONTEXT_TEMPLATE)
            .promptTemplate(CONTEXT_PROMPT_TEMPLATE)
            .build()

        val documentRetriever = VectorStoreDocumentRetriever.builder()
            .vectorStore(vectorStore)
            .similarityThreshold(0.15)
            .topK(5)
            .build()

        return RetrievalAugmentationAdvisor.builder()
            .queryTransformers(queryTransformer)
            .queryAugmenter(augmenter)
            .documentRetriever(documentRetriever)
            .build()
    }

}