package com.aicounseling.app.config

import org.springframework.ai.chat.client.ChatClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class AiChatConfig {
    @Bean
    fun chatClient(chatClientBuilder: ChatClient.Builder): ChatClient = chatClientBuilder.build()
}
