package com.example.onboarder.configuration

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "onboarder")
data class ToolsConfiguration(
    var tools: List<ToolDefinition> = emptyList()
)

data class ToolDefinition(
    var name: String = "",
    var description: String = "",
    var executionPrompt: String = "",
    var keywords: List<String> = emptyList()
)
