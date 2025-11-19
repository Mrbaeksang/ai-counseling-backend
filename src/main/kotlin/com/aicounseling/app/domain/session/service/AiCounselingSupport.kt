package com.aicounseling.app.domain.session.service

import com.aicounseling.app.global.constants.AppConstants
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.ai.chat.client.ChatClient
import org.springframework.stereotype.Component

@Component
class AiCounselingSupport(
    private val chatClient: ChatClient,
    private val objectMapper: ObjectMapper,
) {
    companion object {
        private val logger = LoggerFactory.getLogger(AiCounselingSupport::class.java)
        private const val RAW_RESPONSE_LOG_LIMIT = 1000
    }

    fun getAiResponse(
        systemPrompt: String,
        userMessage: String,
        sessionId: Long,
    ): String {
        return try {
            chatClient.prompt()
                .system(systemPrompt)
                .user(userMessage)
                .call()
                .content()
                ?.trim()
                .orEmpty()
        } catch (ex: RuntimeException) {
            logger.error("Spring AI 호출 실패 - sessionId: {}", sessionId, ex)
            throw ex
        }
    }

    fun buildSystemPrompt(
        basePrompt: String,
        conversationSummary: String,
        isFirstMessage: Boolean,
    ): String =
        buildString {
            appendLine("## 캐릭터 정보")
            appendLine(basePrompt.trim())
            appendLine()

            appendLine("## 안전 및 정책 안내")
            appendLine(AppConstants.Session.BASE_SYSTEM_PROMPT_NOTICE)
            appendLine()

            if (conversationSummary.isNotBlank()) {
                appendLine("## 최근 대화 요약")
                appendLine(conversationSummary)
                appendLine()
            }

            appendLine(AppConstants.Session.RESPONSE_JSON_FORMAT)
            if (isFirstMessage) {
                appendLine()
                appendLine(AppConstants.Session.FIRST_MESSAGE_JSON_FORMAT)
            }
        }

    fun parseAiResponse(
        rawResponse: String,
        expectTitle: Boolean = false,
    ): ParsedResponse {
        if (rawResponse.isBlank()) {
            logger.error("AI 응답이 비어있음")
            return parseFallbackResponse("")
        }

        val cleanedResponse = cleanMarkdownCodeBlock(rawResponse)

        if (cleanedResponse.startsWith("{") && cleanedResponse.endsWith("}")) {
            return parseJsonResponse(cleanedResponse, expectTitle, rawResponse)
        }

        logger.warn(
            "AI가 JSON 형식으로 응답하지 않음: {}",
            cleanedResponse.take(AppConstants.Session.LOG_PREVIEW_LENGTH),
        )
        return parseFallbackResponse(rawResponse)
    }

    private fun cleanMarkdownCodeBlock(rawResponse: String): String {
        return rawResponse.trim()
            .removePrefix("```json").removePrefix("```")
            .removeSuffix("```").trim()
    }

    private fun parseJsonResponse(
        cleanedResponse: String,
        expectTitle: Boolean,
        rawResponse: String,
    ): ParsedResponse {
        return try {
            val preprocessed = preprocessJsonContent(cleanedResponse)
            val jsonNode = objectMapper.readTree(preprocessed)

            val content =
                jsonNode.get("content")?.asText()?.takeIf { it.isNotBlank() }
                    ?: return parseFallbackResponse(rawResponse)

            val title =
                jsonNode.get("title")?.asText()?.take(AppConstants.Session.TITLE_MAX_LENGTH)
                    ?.takeIf { it.isNotBlank() || expectTitle }

            ParsedResponse(
                content = content,
                title = title,
            )
        } catch (e: JsonProcessingException) {
            logger.error("JSON 파싱 실패: {}", e.message)
            parseFallbackResponse(rawResponse)
        }
    }

    private fun preprocessJsonContent(json: String): String {
        // content 필드를 찾아서 내부의 줄바꿈 문자들을 이스케이프
        return json.replace(Regex(""""content"\s*:\s*"([^"]*(?:\\"[^"]*)*)"""")) { matchResult ->
            val content = matchResult.groupValues[1]
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t")
            """"content": "$content""""
        }
    }

    @Suppress("RegExpRedundantEscape")
    private fun parseFallbackResponse(rawResponse: String): ParsedResponse {
        val fallbackContent =
            rawResponse
                .replace(Regex("""\{.*?\}""", RegexOption.DOT_MATCHES_ALL), "")
                .replace(Regex("""\[.*?\]"""), "")
                .trim()
                .ifEmpty { "죄송합니다. 다시 한 번 말씀해 주시겠어요?" }
        return ParsedResponse(
            content = fallbackContent,
            title = null,
        )
    }

    data class ParsedResponse(
        val content: String,
        val title: String?,
    )
}
