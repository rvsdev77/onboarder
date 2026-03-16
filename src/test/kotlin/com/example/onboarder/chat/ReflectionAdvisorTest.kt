package com.example.onboarder.chat

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.client.advisor.api.AdvisedRequest
import org.springframework.ai.chat.client.advisor.api.AdvisedResponse
import org.springframework.ai.chat.client.advisor.api.CallAroundAdvisorChain
import org.springframework.ai.chat.messages.AssistantMessage
import org.springframework.ai.chat.model.ChatResponse
import org.springframework.ai.chat.model.Generation
import kotlin.test.assertEquals

class ReflectionAdvisorTest {

    private lateinit var mockChatClient: ChatClient
    private lateinit var mockChain: CallAroundAdvisorChain
    private lateinit var mockRequest: AdvisedRequest
    private lateinit var mockPromptSpec: ChatClient.ChatClientRequestSpec
    private lateinit var mockCallSpec: ChatClient.CallResponseSpec

    @BeforeEach
    fun setup() {
        mockChatClient = mock()
        mockChain = mock()
        mockRequest = mock()
        mockPromptSpec = mock()
        mockCallSpec = mock()

        whenever(mockRequest.userText).thenReturn("What is the vacation policy?")
        whenever(mockChatClient.prompt()).thenReturn(mockPromptSpec)
        whenever(mockPromptSpec.user(any<String>())).thenReturn(mockPromptSpec)
        whenever(mockPromptSpec.call()).thenReturn(mockCallSpec)
    }

    private fun advisedResponseWith(content: String): AdvisedResponse {
        val generation = Generation(AssistantMessage(content))
        val chatResponse = ChatResponse(listOf(generation))
        return AdvisedResponse(chatResponse, emptyMap<String, Any>())
    }

    @Test
    fun `should return original response when critic passes`() {
        val advisor = ReflectionAdvisor(mockChatClient, enabled = true)
        val original = advisedResponseWith("You get 20 vacation days per year.")

        whenever(mockChain.nextAroundCall(mockRequest)).thenReturn(original)
        whenever(mockCallSpec.content()).thenReturn("""{"pass": true}""")

        val result = advisor.aroundCall(mockRequest, mockChain)

        assertEquals("You get 20 vacation days per year.", result.response?.result?.output?.content)
    }

    @Test
    fun `should return rewritten response when critic fails`() {
        val advisor = ReflectionAdvisor(mockChatClient, enabled = true)
        val original = advisedResponseWith("Vacation is good.")

        whenever(mockChain.nextAroundCall(mockRequest)).thenReturn(original)
        whenever(mockCallSpec.content()).thenReturn(
            """{"pass": false, "rewrite": "Employees receive 20 vacation days per year per company policy."}"""
        )

        val result = advisor.aroundCall(mockRequest, mockChain)

        assertEquals(
            "Employees receive 20 vacation days per year per company policy.",
            result.response?.result?.output?.content
        )
    }

    @Test
    fun `should handle escaped quotes in rewrite`() {
        val advisor = ReflectionAdvisor(mockChatClient, enabled = true)
        val original = advisedResponseWith("Short answer.")

        whenever(mockChain.nextAroundCall(mockRequest)).thenReturn(original)
        whenever(mockCallSpec.content()).thenReturn(
            """{"pass": false, "rewrite": "The policy states \"20 days\" annually."}"""
        )

        val result = advisor.aroundCall(mockRequest, mockChain)

        assertEquals("""The policy states "20 days" annually.""", result.response?.result?.output?.content)
    }

    @Test
    fun `should strip markdown code fences from critic response`() {
        val advisor = ReflectionAdvisor(mockChatClient, enabled = true)
        val original = advisedResponseWith("Original answer.")

        whenever(mockChain.nextAroundCall(mockRequest)).thenReturn(original)
        whenever(mockCallSpec.content()).thenReturn(
            "```json\n{\"pass\": false, \"rewrite\": \"Improved answer.\"}\n```"
        )

        val result = advisor.aroundCall(mockRequest, mockChain)

        assertEquals("Improved answer.", result.response?.result?.output?.content)
    }

    @Test
    fun `should return original response when rewrite field is blank`() {
        val advisor = ReflectionAdvisor(mockChatClient, enabled = true)
        val original = advisedResponseWith("Original answer.")

        whenever(mockChain.nextAroundCall(mockRequest)).thenReturn(original)
        whenever(mockCallSpec.content()).thenReturn("""{"pass": false, "rewrite": ""}""")

        val result = advisor.aroundCall(mockRequest, mockChain)

        assertEquals("Original answer.", result.response?.result?.output?.content)
    }

    @Test
    fun `should skip reflection and return chain response when disabled`() {
        val advisor = ReflectionAdvisor(mockChatClient, enabled = false)
        val original = advisedResponseWith("Original answer.")

        whenever(mockChain.nextAroundCall(mockRequest)).thenReturn(original)

        val result = advisor.aroundCall(mockRequest, mockChain)

        assertEquals("Original answer.", result.response?.result?.output?.content)
        verify(mockChatClient, never()).prompt()
    }

    @Test
    fun `should return original response when critic returns null`() {
        val advisor = ReflectionAdvisor(mockChatClient, enabled = true)
        val original = advisedResponseWith("Original answer.")

        whenever(mockChain.nextAroundCall(mockRequest)).thenReturn(original)
        whenever(mockCallSpec.content()).thenReturn(null)

        val result = advisor.aroundCall(mockRequest, mockChain)

        assertEquals("Original answer.", result.response?.result?.output?.content)
    }
}
