package com.aicounseling.app.domain.session.service

import com.aicounseling.app.domain.character.dto.RateSessionRequest
import com.aicounseling.app.domain.character.service.CharacterService
import com.aicounseling.app.domain.session.dto.CreateSessionResponse
import com.aicounseling.app.domain.session.dto.MessageItem
import com.aicounseling.app.domain.session.dto.SessionListResponse
import com.aicounseling.app.domain.session.entity.ChatSession
import com.aicounseling.app.domain.session.entity.Message
import com.aicounseling.app.domain.session.entity.SenderType
import com.aicounseling.app.domain.session.repository.ChatSessionRepository
import com.aicounseling.app.domain.session.repository.MessageRepository
import com.aicounseling.app.global.constants.AppConstants
import com.aicounseling.app.global.rsData.RsData
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.ai.chat.client.ChatClient
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Caching
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
@Transactional
class ChatSessionService(
    private val sessionRepository: ChatSessionRepository,
    private val characterService: CharacterService,
    private val messageRepository: MessageRepository,
    private val chatClient: ChatClient,
    private val objectMapper: ObjectMapper,
    private val chatSessionCacheService: ChatSessionCacheService,
) {
    companion object {
        private val logger = LoggerFactory.getLogger(ChatSessionService::class.java)
        private const val RAW_RESPONSE_LOG_LIMIT = 1000
    }

    /**
     * 사용자의 상담 세션 목록 조회 (N+1 문제 해결)
     * @param bookmarked 북마크 필터 (null이면 전체, true면 북마크만)
     * @param isClosed 종료 상태 필터 (null이면 전체, true면 종료된 세션, false면 진행중)
     * @param pageable 페이징 정보
     * @return Page<SessionListResponse> 페이징 정보를 포함한 세션 목록
     */
    @Transactional(readOnly = true)
    fun getUserSessions(
        userId: Long,
        bookmarked: Boolean?,
        isClosed: Boolean?,
        pageable: Pageable,
    ): Page<SessionListResponse> {
        // Custom Repository 메서드를 사용하여 N+1 문제 해결
        // 한 번의 쿼리로 Session과 Counselor 정보를 함께 조회
        return chatSessionCacheService.getUserSessions(userId, bookmarked, isClosed, pageable)
    }

    /**
     * 새로운 상담 세션 시작
     * @param characterId 캐릭터 ID
     * @return 생성된 세션 응답 DTO
     */
    @CacheEvict(cacheNames = ["user-sessions"], allEntries = true)
    fun startSession(
        userId: Long,
        characterId: Long,
    ): CreateSessionResponse {
        // 캐릭터 존재 여부 확인
        val character =
            characterService.findById(characterId)
                ?: throw IllegalArgumentException("캐릭터를 찾을 수 없습니다: $characterId")
        // 세션 생성
        val session =
            ChatSession(
                userId = userId,
                characterId = characterId,
            )
        val savedSession = sessionRepository.save(session)

        // DTO 변환 및 반환
        return CreateSessionResponse(
            sessionId = savedSession.id,
            characterId = characterId,
            counselorName = character.name,
            title = savedSession.title ?: AppConstants.Session.DEFAULT_SESSION_TITLE,
            avatarUrl = character.avatarUrl,
        )
    }

    /**
     * 상담 세션 종료
     * @param sessionId 종료할 세션 ID
     * @throws IllegalArgumentException 세션을 찾을 수 없는 경우
     * @throws IllegalStateException 이미 종료된 세션인 경우
     */
    @Transactional
    @Caching(
        evict = [
            CacheEvict(cacheNames = ["user-sessions"], allEntries = true),
            CacheEvict(cacheNames = ["session-messages"], allEntries = true),
        ],
    )
    fun closeSession(
        userId: Long,
        sessionId: Long,
    ) {
        val session = getSession(sessionId, userId)
        check(session.closedAt == null) {
            AppConstants.ErrorMessages.SESSION_ALREADY_CLOSED
        }
        session.closedAt = Instant.now()
        sessionRepository.save(session)
    }

    /**
     * 종료된 세션에 대한 캐릭터 평가
     * @param sessionId 평가할 세션 ID
     * @param request 평가 요청 (rating 1-10, feedback)
     * @return 평가 결과 (RsData)
     * @throws IllegalArgumentException 세션을 찾을 수 없는 경우
     * @throws IllegalStateException 진행 중인 세션인 경우
     */
    @Transactional
    fun rateSession(
        userId: Long,
        sessionId: Long,
        request: RateSessionRequest,
    ): RsData<String> {
        val session = getSession(sessionId, userId)
        check(session.closedAt != null) {
            AppConstants.ErrorMessages.SESSION_CANNOT_RATE_ACTIVE
        }
        // 중복 평가 체크
        if (characterService.isSessionRated(sessionId)) {
            return RsData.of(
                "F-400",
                "이미 평가가 완료된 세션입니다",
                null,
            )
        }
        return characterService.addRating(
            sessionId = sessionId,
            userId = userId,
            characterId = session.characterId,
            session = session,
            request = request,
        )
    }

    /**
     * 세션 북마크 토글 (추가/제거)
     * @param sessionId 북마크할 세션 ID
     * @return 토글 후 북마크 상태 (true: 북마크됨, false: 북마크 해제됨)
     * @throws IllegalArgumentException 세션을 찾을 수 없는 경우
     */
    @Transactional
    @CacheEvict(cacheNames = ["user-sessions"], allEntries = true)
    fun toggleBookmark(
        userId: Long,
        sessionId: Long,
    ): Boolean {
        val session = getSession(sessionId, userId)
        session.isBookmarked = !session.isBookmarked
        sessionRepository.save(session)
        return session.isBookmarked
    }

    /**
     * 특정 세션 조회 (사용자 권한 확인 포함)
     * @param sessionId 조회할 세션 ID
     * @param userId 사용자 ID (권한 확인용)
     * @return 조회된 세션
     * @throws IllegalArgumentException 세션을 찾을 수 없는 경우
     */
    private fun getSession(
        sessionId: Long,
        userId: Long,
    ): ChatSession {
        return sessionRepository.findByIdAndUserId(sessionId, userId)
            ?: throw IllegalArgumentException("${AppConstants.ErrorMessages.SESSION_NOT_FOUND}: $sessionId")
    }

    /**
     * 세션 제목 수정
     * @param sessionId 수정할 세션 ID
     * @param newTitle 새로운 제목
     * @return 수정된 세션
     * @throws IllegalArgumentException 세션을 찾을 수 없는 경우
     */
    @Transactional
    @Caching(
        evict = [
            CacheEvict(cacheNames = ["user-sessions"], allEntries = true),
            CacheEvict(cacheNames = ["session-messages"], allEntries = true),
        ],
    )
    fun updateSessionTitle(
        userId: Long,
        sessionId: Long,
        newTitle: String,
    ): ChatSession {
        val session = getSession(sessionId, userId)
        session.title = newTitle.trim()
        return sessionRepository.save(session)
    }

    /**
     * 세션의 메시지 목록 조회
     * @param sessionId 조회할 세션 ID
     * @param pageable 페이징 정보
     * @return Page<MessageItem> 페이징 정보를 포함한 메시지 목록
     * @throws IllegalArgumentException 세션을 찾을 수 없는 경우
     */
    @Transactional(readOnly = true)
    fun getSessionMessages(
        userId: Long,
        sessionId: Long,
        pageable: Pageable,
    ): Page<MessageItem> {
        getSession(sessionId, userId) // 권한 확인용
        return chatSessionCacheService.getSessionMessages(sessionId, pageable)
    }

    /**
     * 사용자 메시지 전송 및 AI 응답 생성
     * 5단계 상담 모델을 기반으로 AI가 적절한 상담 단계를 판단하여 응답
     * @param sessionId 메시지를 전송할 세션 ID
     * @param content 사용자 메시지 내용
     * @return 사용자 메시지, AI 응답 메시지, 업데이트된 세션
     * @throws IllegalArgumentException 세션을 찾을 수 없거나 메시지 내용이 비어있는 경우
     * @throws IllegalStateException 이미 종료된 세션인 경우
     */
    @Transactional
    @Caching(
        evict = [
            CacheEvict(cacheNames = ["user-sessions"], allEntries = true),
            CacheEvict(cacheNames = ["session-messages"], allEntries = true),
        ],
    )
    @Suppress("TooGenericExceptionCaught")
    fun sendMessage(
        userId: Long,
        sessionId: Long,
        content: String,
    ): Triple<Message, Message, ChatSession> {
        check(content.isNotBlank()) { AppConstants.ErrorMessages.MESSAGE_CONTENT_EMPTY }
        val session = getSession(sessionId, userId)
        check(session.closedAt == null) { AppConstants.ErrorMessages.SESSION_ALREADY_CLOSED }

        val isFirstMessage = messageRepository.countBySessionId(sessionId) == 0L
        val userMessage = saveUserMessage(session, content)

        val character =
            characterService.findById(session.characterId)
                ?: error("캐릭터를 찾을 수 없습니다: ${session.characterId}")

        val conversationSummary = summarizeConversation(sessionId)
        val systemPrompt = buildSystemPrompt(
            basePrompt = character.basePrompt,
            conversationSummary = conversationSummary,
            isFirstMessage = isFirstMessage,
        )

        val rawResponse =
            try {
                chatClient.prompt()
                    .system(systemPrompt)
                    .user(userMessage.content)
                    .call()
                    .content()
                    ?.trim()
                    .orEmpty()
            } catch (ex: RuntimeException) {
                logger.error("Spring AI 호출 실패 - sessionId: {}", sessionId, ex)
                val (fallbackMessage, fallbackSession) = handleAiError(session, userMessage)
                return Triple(userMessage, fallbackMessage, fallbackSession)
            }

        logger.info(
            "AI 원본 응답 - sessionId: {}, response: {}",
            sessionId,
            rawResponse.take(RAW_RESPONSE_LOG_LIMIT),
        )

        val parsedResponse = parseAiResponse(rawResponse, isFirstMessage)

        val aiMessage =
            messageRepository.save(
                Message(
                    session = session,
                    senderType = SenderType.AI,
                    content = parsedResponse.content,
                ),
            )

        applySessionUpdates(
            session = session,
            parsedResponse = parsedResponse,
            isFirstMessage = isFirstMessage,
        )

        val updatedSession = sessionRepository.save(session)

        return Triple(userMessage, aiMessage, updatedSession)
    }

    /**
     * 사용자 메시지 저장
     */
    private fun saveUserMessage(
        session: ChatSession,
        content: String,
    ): Message {
        val userMessage =
            Message(
                session = session,
                senderType = SenderType.USER,
                content = content,
            )
        return messageRepository.save(userMessage)
    }

    private fun summarizeConversation(sessionId: Long): String {
        val messages = messageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId)
        if (messages.isEmpty()) {
            return ""
        }

        return messages.joinToString(separator = "\n") { message ->
            val speaker = if (message.senderType == SenderType.USER) "사용자" else "AI 캐릭터"
            val cleaned = message.content.trim()
            "$speaker: $cleaned"
        }
    }

    private fun buildSystemPrompt(
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

    private fun applySessionUpdates(
        session: ChatSession,
        parsedResponse: ParsedResponse,
        isFirstMessage: Boolean,
    ) {
        parsedResponse.title?.let { candidateTitle ->
            if (candidateTitle.isNotBlank()) {
                val shouldUpdateTitle =
                    isFirstMessage || session.title.isNullOrBlank() ||
                        session.title == AppConstants.Session.DEFAULT_SESSION_TITLE
                if (shouldUpdateTitle) {
                    val normalizedTitle = candidateTitle.take(AppConstants.Session.TITLE_MAX_LENGTH).trim()
                    session.title =
                        normalizedTitle.ifEmpty { AppConstants.Session.DEFAULT_SESSION_TITLE }
                }
            }
        }
        session.lastMessageAt = Instant.now()
    }

    /**
     * AI 응답 에러 처리 통합 메서드
     * 기존 handleAiResponseError와 createErrorMessage를 통합
     */
    private fun handleAiError(
        session: ChatSession,
        userMessage: Message,
    ): Pair<Message, ChatSession> {
        // 세션 업데이트
        session.lastMessageAt = Instant.now()
        if (messageRepository.countBySessionId(session.id) == 1L) {
            // 첫 메시지 에러인 경우 제목 설정
            session.title =
                userMessage.content.take(AppConstants.Session.TITLE_MAX_LENGTH).trim()
                    .ifEmpty { AppConstants.Session.DEFAULT_SESSION_TITLE }
        }

        // 에러 메시지 생성
        val errorMessage =
            Message(
                session = session,
                senderType = SenderType.AI,
                content = AppConstants.ErrorMessages.AI_RESPONSE_ERROR,
            )

        val savedMessage = messageRepository.save(errorMessage)
        val updatedSession = sessionRepository.save(session)

        return Pair(savedMessage, updatedSession)
    }

    /**
     * 최근 AI 응답의 단계를 기반으로 현재 세션에서 허용 가능한 단계를 계산합니다.
     */

    private fun parseAiResponse(
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
            val jsonNode = objectMapper.readTree(cleanedResponse)

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
