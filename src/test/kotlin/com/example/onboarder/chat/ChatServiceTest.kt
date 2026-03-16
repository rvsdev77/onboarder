package com.example.onboarder.chat

import com.example.onboarder.configuration.ToolDefinition
import com.example.onboarder.configuration.ToolsConfiguration
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.client.advisor.api.Advisor
import org.springframework.ai.chat.model.Generation
import org.springframework.core.io.ByteArrayResource
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import org.springframework.ai.chat.model.ChatResponse as AiChatResponse

class ChatServiceTest {

    private lateinit var mockBuilder: ChatClient.Builder
    private lateinit var mockAdvisor: Advisor
    private lateinit var mockReflectionAdvisor: ReflectionAdvisor
    private lateinit var mockToolsService: ToolsService
    private lateinit var mockFeedbackService: com.example.onboarder.feedback.FeedbackService
    private lateinit var systemPromptResource: ByteArrayResource
    private lateinit var chatService: ChatService

    @BeforeEach
    fun setup() {
        mockBuilder = mock()
        mockAdvisor = mock()
        mockReflectionAdvisor = mock()
        mockToolsService = ToolsService(
            ToolsConfiguration(
                tools = listOf(
                    ToolDefinition(
                        name = "getPtoBalance",
                        description = "Retrieve PTO balance",
                        executionPrompt = "Show me PTO",
                        keywords = listOf("pto", "vacation")
                    )
                )
            )
        )
        mockFeedbackService = mock()
        systemPromptResource = ByteArrayResource("Test system prompt".toByteArray())

        whenever(mockBuilder.defaultAdvisors(any<Advisor>())).thenReturn(mockBuilder)
        whenever(mockBuilder.defaultAdvisors(any<Advisor>(), any<Advisor>())).thenReturn(mockBuilder)
        whenever(mockBuilder.defaultFunctions(any<String>())).thenReturn(mockBuilder)

        chatService = ChatService(
            mockBuilder,
            mockAdvisor,
            mockReflectionAdvisor,
            systemPromptResource,
            mockToolsService,
            mockFeedbackService
        )
    }

    private fun mockChatClientReturning(answer: String): ChatClient {
        val mockChatClient = mock<ChatClient>()
        val mockRequestSpec = mock<ChatClient.ChatClientRequestSpec>()
        val mockCallResponseSpec = mock<ChatClient.CallResponseSpec>()
        val mockAiChatResponse = mock<AiChatResponse>()
        val mockGeneration = mock<Generation>()

        whenever(mockBuilder.build()).thenReturn(mockChatClient)
        whenever(mockChatClient.prompt()).thenReturn(mockRequestSpec)
        whenever(mockRequestSpec.system(any<String>())).thenReturn(mockRequestSpec)
        whenever(mockRequestSpec.user(any<String>())).thenReturn(mockRequestSpec)
        whenever(mockRequestSpec.call()).thenReturn(mockCallResponseSpec)
        whenever(mockCallResponseSpec.chatResponse()).thenReturn(mockAiChatResponse)
        whenever(mockAiChatResponse.result).thenReturn(mockGeneration)
        whenever(mockGeneration.output).thenReturn(mock())
        whenever(mockGeneration.output.content).thenReturn(answer)

        return mockChatClient
    }

    @Test
    fun `should return chat response for non-PTO question`() = runTest {
        mockChatClientReturning("Company address is 123 Main St")

        val result = chatService.getAnswer("What is the company address?")

        assertNotNull(result)
        assertEquals("Company address is 123 Main St", result.response)
        assertNull(result.toolRequest)
        assertNotNull(result.responseId)
    }

    @Test
    fun `should return chat response with tool request for PTO question`() = runTest {
        mockChatClientReturning("You have 15 PTO days per year")

        val result = chatService.getAnswer("How many PTO days do I have?")

        assertNotNull(result)
        assertEquals("You have 15 PTO days per year", result.response)
        assertNotNull(result.toolRequest)
        assertEquals("getPtoBalance", result.toolRequest.toolName)
        assertEquals("Retrieve PTO balance", result.toolRequest.description)
        assertNotNull(result.responseId)
    }

    @Test
    fun `should handle null content from chat client`() = runTest {
        val mockChatClient = mock<ChatClient>()
        val mockRequestSpec = mock<ChatClient.ChatClientRequestSpec>()
        val mockCallResponseSpec = mock<ChatClient.CallResponseSpec>()

        whenever(mockBuilder.build()).thenReturn(mockChatClient)
        whenever(mockChatClient.prompt()).thenReturn(mockRequestSpec)
        whenever(mockRequestSpec.system(any<String>())).thenReturn(mockRequestSpec)
        whenever(mockRequestSpec.user(any<String>())).thenReturn(mockRequestSpec)
        whenever(mockRequestSpec.call()).thenReturn(mockCallResponseSpec)
        whenever(mockCallResponseSpec.chatResponse()).thenReturn(null)

        val result = chatService.getAnswer("Test question")

        assertNotNull(result)
        assertEquals("Unable to generate response.", result.response)
    }

    @Test
    fun `should skip reflection when tool request is detected`() = runTest {
        mockChatClientReturning("You have 15 PTO days per year")

        val result = chatService.getAnswer("How many vacation days do I have?")

        assertNotNull(result.toolRequest)
        verify(mockBuilder).defaultAdvisors(mockAdvisor)
        verify(mockBuilder, never()).defaultAdvisors(mockAdvisor, mockReflectionAdvisor)
    }
}
