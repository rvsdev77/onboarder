package com.example.onboarder.chat

import org.slf4j.LoggerFactory
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.client.advisor.api.AdvisedRequest
import org.springframework.ai.chat.client.advisor.api.AdvisedResponse
import org.springframework.ai.chat.client.advisor.api.CallAroundAdvisor
import org.springframework.ai.chat.client.advisor.api.CallAroundAdvisorChain
import org.springframework.ai.chat.messages.AssistantMessage
import org.springframework.ai.chat.model.ChatResponse
import org.springframework.ai.chat.model.Generation

/**
 * Advisor that implements a self-reflection loop over AI-generated responses.
 *
 * After the primary response is generated, a second "critic" LLM call evaluates
 * the answer for accuracy and completeness. If the critic identifies issues, a
 * third "rewrite" call produces an improved response.
 *
 * The reflection loop adds at most 2 extra LLM calls per request and can be
 * disabled entirely via [enabled].
 *
 * @property chatClient Client used for critic and rewrite calls.
 * @property enabled Whether reflection is active. When false, the advisor is a no-op.
 */
class ReflectionAdvisor(
    private val chatClient: ChatClient,
    private val enabled: Boolean
) : CallAroundAdvisor {

    private val log = LoggerFactory.getLogger(ReflectionAdvisor::class.java)

    /**
     * Intercepts the advisor chain, runs the primary call, then applies the
     * reflection loop before returning the final response to the caller.
     */
    override fun aroundCall(request: AdvisedRequest, chain: CallAroundAdvisorChain): AdvisedResponse {
        val advised = chain.nextAroundCall(request)

        if (!enabled) return advised

        val question = request.userText
        val originalAnswer = advised.response?.result?.output?.content ?: return advised

        val finalAnswer = reflect(question, originalAnswer)

        if (finalAnswer == originalAnswer) return advised

        val rewritten = AdvisedResponse(
            ChatResponse(listOf(Generation(AssistantMessage(finalAnswer)))),
            advised.adviseContext
        )
        return rewritten
    }

    /**
     * Runs the critic prompt against the given question/answer pair.
     *
     * Returns the original answer if the critic passes, or a rewritten answer
     * if the critic identifies issues.
     *
     * @param question The original user question.
     * @param answer The primary response to evaluate.
     * @return The original or improved answer.
     */
    private fun reflect(question: String, answer: String): String {
        val criticPrompt = """
            Question: "$question"
            Answer: "$answer"
            
            Evaluate the answer strictly. Reply ONLY with valid JSON, no markdown, no explanation.
            If the answer is accurate and complete: {"pass": true}
            If there are issues: {"pass": false, "rewrite": "<full improved answer>"}
        """.trimIndent()

        val criticResponse = chatClient.prompt()
            .user(criticPrompt)
            .call()
            .content() ?: return answer

        log.debug("Reflection critic response: {}", criticResponse)

        val cleaned = criticResponse.trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()

        return if (cleaned.contains("\"pass\":false") || cleaned.contains("\"pass\": false")) {
            val rewrite = extractRewrite(cleaned)
            if (rewrite.isNullOrBlank()) answer else rewrite
        } else {
            answer
        }
    }

    /**
     * Extracts the rewrite value from the critic's JSON response.
     *
     * Uses simple string parsing to avoid pulling in a JSON library dependency.
     *
     * @param json Raw JSON string from the critic.
     * @return The rewritten answer, or null if extraction fails.
     */
    private fun extractRewrite(json: String): String? {
        val match = Regex(""""rewrite"\s*:\s*"((?:[^"\\]|\\.)*)"""").find(json)
        return match?.groupValues?.get(1)?.replace("\\\"", "\"")?.replace("\\n", "\n")
    }

    /** Advisor order — runs after RAG (higher number = later in chain). */
    override fun getOrder(): Int = 200

    override fun getName(): String = "ReflectionAdvisor"
}
