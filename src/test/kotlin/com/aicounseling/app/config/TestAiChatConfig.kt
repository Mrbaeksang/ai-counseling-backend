package com.aicounseling.app.config

import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.messages.AssistantMessage
import org.springframework.ai.chat.model.ChatModel
import org.springframework.ai.chat.model.ChatResponse
import org.springframework.ai.chat.model.Generation
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import reactor.core.publisher.Flux

@Configuration
@Profile("test")
class TestAiChatConfig {
    private val defaultResponse =
        """
        {
          "content": "테스트 환경용 기본 응답입니다. 실제 모델은 Spring AI로 대체됩니다.",
          "title": "테스트 세션"
        }
        """.trimIndent()

    @Bean
    @Primary
    fun testChatModel(): ChatModel =
        object : ChatModel {
            override fun call(prompt: Prompt): ChatResponse {
                val message = AssistantMessage(defaultResponse)
                val generation = Generation(message)
                return ChatResponse(listOf(generation))
            }

            override fun stream(prompt: Prompt): Flux<ChatResponse> = Flux.just(call(prompt))
        }

    @Bean
    @Primary
    fun testChatClient(chatModel: ChatModel): ChatClient = ChatClient.create(chatModel)
}
